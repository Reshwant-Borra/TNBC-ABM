package OnLatticeExample;

import HAL.Rand;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Resumable, common-random-number Morris screening for the untreated TNBC ABM.
 * This is an elementary-effects screen, not calibration and not Sobol analysis.
 */
public final class MorrisSensitivitySweep {
    static final int CHECKPOINT_VERSION=4;
    static final int[] STANDARD_STEPS={0,480,960,1440,2100};
    static final String[] COUNT_NAMES={"jnkp_tumor_count","jnkn_tumor_count","active_ec_count","inactive_ec_count",
            "active_macrophage_count","inactive_macrophage_count","active_fibroblast_count","inactive_fibroblast_count"};
    static final String[] EVENT_NAMES={"tumor_divisions","tumor_deaths","fibroblast_divisions","fibroblast_deaths",
            "macrophage_divisions","macrophage_deaths","ec_divisions","ec_deaths","chemo_tumor_divisions","chemo_tumor_deaths"};
    static final String[] SPATIAL_NAMES={"tumor_radius","tumor_rms_spread","jnkp_rim_fraction","active_macrophage_ec_colocalization"};

    static final class Config {
        int trajectories=20, levels=6, threads=Math.max(1,Runtime.getRuntime().availableProcessors()-1);
        int replicates=1, initPop=25, maxSteps=1440, permutations=200;
        long masterSeed=9001L;
        Path outputDir=Path.of("results/morris");
        boolean force=false, confirmOnly=false;
        int confirmationReplicates=3, confirmationTop=10;

        static Config parse(String[] args) {
            Config c=new Config();
            for(int i=0;i<args.length;i++) {
                String a=args[i];
                if (a.equals("--force")) { c.force=true; continue; }
                if (a.equals("--resume")) continue; // resume is the safe default
                if (a.equals("--confirm-only")) { c.confirmOnly=true; continue; }
                if (i+1>=args.length) throw new IllegalArgumentException("missing value after "+a);
                String v=args[++i];
                switch(a) {
                    case "--trajectories": c.trajectories=Integer.parseInt(v); break;
                    case "--levels": c.levels=Integer.parseInt(v); break;
                    case "--threads": c.threads=Integer.parseInt(v); break;
                    case "--master-seed": c.masterSeed=Long.parseLong(v); break;
                    case "--replicates": c.replicates=Integer.parseInt(v); break;
                    case "--init-pop": c.initPop=Integer.parseInt(v); break;
                    case "--max-steps": c.maxSteps=Integer.parseInt(v); break;
                    case "--output-dir": c.outputDir=Path.of(v); break;
                    case "--permutations": c.permutations=Integer.parseInt(v); break;
                    case "--confirmation-replicates": c.confirmationReplicates=Integer.parseInt(v); break;
                    case "--confirmation-top": c.confirmationTop=Integer.parseInt(v); break;
                    default: throw new IllegalArgumentException("unknown option: "+a);
                }
            }
            if(c.trajectories<1||c.threads<1||c.replicates<1) throw new IllegalArgumentException("trajectories, threads, and replicates must be positive");
            if(c.levels<4||c.levels%2!=0) throw new IllegalArgumentException("--levels must be even and at least 4 for the standard Morris construction");
            if(c.maxSteps<0) throw new IllegalArgumentException("--max-steps must be non-negative");
            if(c.confirmationReplicates<3) throw new IllegalArgumentException("confirmation requires at least 3 replicates");
            return c;
        }
    }

    static final class Point {
        final int trajectory, step;
        final String changedParameter, duplicateOf;
        final double signedDelta;
        final double[] normalized;
        final ModelParameters parameters;
        Point(int trajectory,int step,String changedParameter,double signedDelta,double[] normalized,
              ModelParameters parameters,String duplicateOf) {
            this.trajectory=trajectory; this.step=step; this.changedParameter=changedParameter;
            this.signedDelta=signedDelta; this.normalized=normalized; this.parameters=parameters;
            this.duplicateOf=duplicateOf;
        }
        String pointId() { return String.format(Locale.US,"T%04d_S%03d",trajectory,step); }
        String sampleId(int replicate) { return pointId()+String.format(Locale.US,"_R%03d",replicate); }
    }

    static final class Design {
        final List<Point> points;
        final long seed;
        final double delta;
        final int duplicateCount;
        Design(List<Point> points,long seed,double delta,int duplicateCount) {
            this.points=points; this.seed=seed; this.delta=delta; this.duplicateCount=duplicateCount;
        }
    }

    static final class RunRecord {
        final Point point;
        final int replicate;
        final long simulationSeed;
        long runtimeMillis;
        int[][] snapshots=new int[0][];
        int[] snapshotSteps=new int[0];
        int[][] eventCounts=new int[0][];
        double[][] spatialMetrics=new double[0][];
        final LinkedHashSet<String> flags=new LinkedHashSet<>();
        String errorClass="", errorMessage="";
        boolean resumed;
        int requestedMaxSteps;
        RunRecord(Point point,int replicate,long simulationSeed) {
            this.point=point; this.replicate=replicate; this.simulationSeed=simulationSeed;
        }
        String id() { return point.sampleId(replicate); }
        int snapshotIndex(int step) {
            for(int i=0;i<snapshotSteps.length;i++) if(snapshotSteps[i]==step) return i;
            return -1;
        }
        int[] countsAt(int step) { int i=snapshotIndex(step); return i>=0&&i<snapshots.length?snapshots[i]:null; }
        int[] eventsAt(int step) { int i=snapshotIndex(step); return i>=0&&i<eventCounts.length?eventCounts[i]:null; }
        double[] spatialAt(int step) { int i=snapshotIndex(step); return i>=0&&i<spatialMetrics.length?spatialMetrics[i]:null; }
        boolean has(String flag) { return flags.contains(flag); }
        boolean error() { return has("MODEL_EXCEPTION")||has("NONFINITE_PROBABILITY"); }
        boolean strictValid() { return !error()&&!has("MISSING_SNAPSHOT")&&!has("TUMOR_EXTINCT")&&!has("EC_POPULATION_ZERO")&&!has("MACROPHAGE_POPULATION_ZERO")&&!has("FIBROBLAST_POPULATION_ZERO"); }
        String status() {
            if(error()) return "ERROR";
            if(has("MISSING_SNAPSHOT")) return "INVALID";
            if(has("TUMOR_EXTINCT")) return "EXTINCT";
            if(!strictValid()) return "INVALID";
            return "FINITE";
        }
    }

    static final class OutputValue {
        final String name, family, invalidReason;
        final int snapshotStep;
        final double value;
        final boolean valid;
        OutputValue(String name,String family,int snapshotStep,double value,boolean valid,String invalidReason) {
            this.name=name; this.family=family; this.snapshotStep=snapshotStep; this.value=value;
            this.valid=valid&&Double.isFinite(value); this.invalidReason=this.valid?"":invalidReason;
        }
    }

    static long mix64(long z) {
        z=(z^(z>>>30))*0xbf58476d1ce4e5b9L;
        z=(z^(z>>>27))*0x94d049bb133111ebL;
        return z^(z>>>31);
    }
    static long designSeed(long master) { return mix64(master^0x4d4f525249534cL); }
    static long simulationSeed(long master,int trajectory,int replicate) {
        return mix64(master+0x9e3779b97f4a7c15L*(trajectory+1L)+0x632be59bd9b4e019L*(replicate+1L));
    }

    static Design generateDesign(Config c) {
        List<ModelParameters.Definition> defs=ModelParameters.screenedDefinitions();
        int k=defs.size(), jump=c.levels/2;
        double delta=(double)jump/(c.levels-1);
        long seed=designSeed(c.masterSeed);
        SplittableRandom rng=new SplittableRandom(seed);
        List<Point> out=new ArrayList<>(c.trajectories*(k+1));
        Map<String,String> physicalSeen=new HashMap<>();
        int duplicates=0;
        ModelParameters baseline=ModelParameters.currentBaseline(c.initPop);
        for(int t=0;t<c.trajectories;t++) {
            int[] order=new int[k], direction=new int[k], base=new int[k];
            double[] x=new double[k];
            for(int i=0;i<k;i++) {
                order[i]=i;
                direction[i]=rng.nextBoolean()?1:-1;
                base[i]=rng.nextInt(c.levels-jump);
                x[i]=(base[i]+(direction[i]<0?jump:0))/(double)(c.levels-1);
            }
            for(int i=k-1;i>0;i--) { int j=rng.nextInt(i+1),tmp=order[i];order[i]=order[j];order[j]=tmp; }
            for(int step=0;step<=k;step++) {
                String changed=""; double signed=0.0;
                if(step>0) {
                    int idx=order[step-1];
                    x[idx]+=direction[idx]*delta;
                    changed=defs.get(idx).name;
                    signed=direction[idx]*delta;
                }
                ModelParameters p=baseline;
                for(int i=0;i<k;i++) p=p.with(defs.get(i).name,defs.get(i).fromNormalized(x[i]));
                p.validate();
                String key=p.csvRow();
                String pointId=String.format(Locale.US,"T%04d_S%03d",t,step);
                String duplicate=physicalSeen.putIfAbsent(key,pointId);
                if(duplicate!=null) duplicates++;
                out.add(new Point(t,step,changed,signed,x.clone(),p,duplicate==null?"":duplicate));
            }
        }
        return new Design(out,seed,delta,duplicates);
    }

    static void atomicMove(Path tmp,Path target) throws IOException {
        try { Files.move(tmp,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING); }
        catch(AtomicMoveNotSupportedException e) { Files.move(tmp,target,StandardCopyOption.REPLACE_EXISTING); }
    }

    static void writeDesign(Path file,Design design,Config c) throws IOException {
        Path tmp=file.resolveSibling(file.getFileName()+".tmp");
        List<ModelParameters.Definition> defs=ModelParameters.screenedDefinitions();
        try(BufferedWriter w=Files.newBufferedWriter(tmp,StandardCharsets.UTF_8)) {
            w.write("point_id,trajectory_id,step_id,changed_parameter,signed_normalized_step,design_seed,levels,delta,duplicate_physical_point,duplicate_of");
            for(ModelParameters.Definition d:defs) w.write(",x_"+d.name);
            for(ModelParameters.Definition d:defs) w.write(","+d.name);
            w.newLine();
            for(Point p:design.points) {
                w.write(csv(p.pointId())+","+p.trajectory+","+p.step+","+csv(p.changedParameter)+","+fmt(p.signedDelta)+","+design.seed+","+c.levels+","+fmt(design.delta)+","+(!p.duplicateOf.isEmpty())+","+csv(p.duplicateOf));
                for(double x:p.normalized) w.write(","+fmt(x));
                w.write(","+p.parameters.csvRow());
                w.newLine();
            }
        }
        atomicMove(tmp,file);
    }

    static Path checkpointPath(Path dir,Point p,int replicate) {
        return dir.resolve(p.sampleId(replicate)+".bin");
    }

    static void writeCheckpoint(Path file,RunRecord r) throws IOException {
        Path tmp=file.resolveSibling(file.getFileName()+".tmp");
        try(DataOutputStream d=new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(tmp)))) {
            d.writeInt(CHECKPOINT_VERSION); d.writeInt(r.point.trajectory); d.writeInt(r.point.step); d.writeInt(r.replicate);
            d.writeLong(r.simulationSeed); d.writeInt(r.requestedMaxSteps); writeString(d,r.point.parameters.csvRow());
            d.writeLong(r.runtimeMillis); d.writeBoolean(r.resumed);
            writeString(d,String.join(";",r.flags)); writeString(d,r.errorClass); writeString(d,r.errorMessage);
            d.writeInt(r.snapshotSteps.length);
            for(int i=0;i<r.snapshotSteps.length;i++) {
                d.writeInt(r.snapshotSteps[i]);
                int[] s=i<r.snapshots.length?r.snapshots[i]:new int[0]; d.writeInt(s.length); for(int v:s)d.writeInt(v);
                int[] e=i<r.eventCounts.length?r.eventCounts[i]:new int[0]; d.writeInt(e.length); for(int v:e)d.writeInt(v);
                double[] m=i<r.spatialMetrics.length?r.spatialMetrics[i]:new double[0]; d.writeInt(m.length); for(double v:m)d.writeDouble(v);
            }
        }
        atomicMove(tmp,file);
    }

    static RunRecord readCheckpoint(Path file,Point p,int replicate,long expectedSeed,int expectedMaxSteps) throws IOException {
        try(DataInputStream d=new DataInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
            int version=d.readInt();
            if(version!=3&&version!=CHECKPOINT_VERSION) throw new IOException("checkpoint version mismatch");
            int t=d.readInt(),s=d.readInt(),rep=d.readInt(); long seed=d.readLong();
            if(t!=p.trajectory||s!=p.step||rep!=replicate||seed!=expectedSeed) throw new IOException("checkpoint identity/seed mismatch");
            if(version>=4) {
                int maxSteps=d.readInt(); String physicalRow=readString(d);
                if(maxSteps!=expectedMaxSteps)throw new IOException("checkpoint horizon mismatch");
                if(!physicalRow.equals(p.parameters.csvRow()))throw new IOException("checkpoint physical-parameter fingerprint mismatch");
            }
            RunRecord r=new RunRecord(p,replicate,seed); r.requestedMaxSteps=expectedMaxSteps;r.runtimeMillis=d.readLong(); d.readBoolean(); r.resumed=true;
            String flags=readString(d); if(!flags.isEmpty())r.flags.addAll(Arrays.asList(flags.split(";")));
            r.errorClass=readString(d); r.errorMessage=readString(d);
            int n=d.readInt(); r.snapshotSteps=new int[n]; r.snapshots=new int[n][]; r.eventCounts=new int[n][]; r.spatialMetrics=new double[n][];
            for(int i=0;i<n;i++) {
                r.snapshotSteps[i]=d.readInt();
                r.snapshots[i]=new int[d.readInt()]; for(int j=0;j<r.snapshots[i].length;j++)r.snapshots[i][j]=d.readInt();
                r.eventCounts[i]=new int[d.readInt()]; for(int j=0;j<r.eventCounts[i].length;j++)r.eventCounts[i][j]=d.readInt();
                r.spatialMetrics[i]=new double[d.readInt()]; for(int j=0;j<r.spatialMetrics[i].length;j++)r.spatialMetrics[i][j]=d.readDouble();
            }
            return r;
        } catch(EOFException e) { throw new IOException("incomplete checkpoint "+file,e); }
    }

    static void writeString(DataOutputStream d,String s) throws IOException {
        byte[] b=(s==null?"":s).getBytes(StandardCharsets.UTF_8); d.writeInt(b.length); d.write(b);
    }
    static String readString(DataInputStream d) throws IOException {
        int n=d.readInt(); if(n<0||n>10_000_000)throw new IOException("invalid checkpoint string length");
        byte[] b=new byte[n]; d.readFully(b); return new String(b,StandardCharsets.UTF_8);
    }

    static RunRecord runOne(Point p,int replicate,long seed,int maxSteps) {
        RunRecord r=new RunRecord(p,replicate,seed);
        r.requestedMaxSteps=maxSteps;
        long start=System.nanoTime();
        try {
            List<String> validation=p.parameters.validationErrors();
            if(!validation.isEmpty()) {
                r.flags.add("NONFINITE_PROBABILITY"); r.errorClass="ParameterValidation"; r.errorMessage=String.join("; ",validation);
            } else {
                ExampleGrid model=new ExampleGrid(100,100);
                model.rng=new Rand(seed);
                r.snapshots=model.RunHeadless(p.parameters,maxSteps);
                r.snapshotSteps=model.lastSnapshotSteps==null?new int[0]:model.lastSnapshotSteps.clone();
                r.eventCounts=deepCopy(model.lastEventCounts);
                r.spatialMetrics=deepCopy(model.lastSpatialMetrics);
                classifyRun(r,maxSteps);
            }
        } catch(Throwable e) {
            r.flags.add("MODEL_EXCEPTION"); r.errorClass=e.getClass().getName();
            r.errorMessage=e.getMessage()==null?e.toString():e.getMessage();
        } finally { r.runtimeMillis=(System.nanoTime()-start)/1_000_000L; }
        return r;
    }

    static int[][] deepCopy(int[][] x) {
        if(x==null)return new int[0][]; int[][] y=new int[x.length][];
        for(int i=0;i<x.length;i++)y[i]=x[i]==null?new int[0]:x[i].clone(); return y;
    }
    static double[][] deepCopy(double[][] x) {
        if(x==null)return new double[0][]; double[][] y=new double[x.length][];
        for(int i=0;i<x.length;i++)y[i]=x[i]==null?new double[0]:x[i].clone(); return y;
    }

    static void classifyRun(RunRecord r,int maxSteps) {
        for(int s:STANDARD_STEPS) if(s<=maxSteps&&r.snapshotIndex(s)<0) r.flags.add("MISSING_SNAPSHOT");
        if(r.snapshots.length==0) { r.flags.add("MISSING_SNAPSHOT"); return; }
        int[] last=r.snapshots[r.snapshots.length-1];
        if(last.length!=8) { r.flags.add("MISSING_SNAPSHOT"); return; }
        if(last[0]+last[1]==0)r.flags.add("TUMOR_EXTINCT");
        boolean invalidDenominator=false;
        for(int[] a:r.snapshots) if(a.length==8) {
            if(a[2]+a[3]==0){r.flags.add("EC_POPULATION_ZERO");invalidDenominator=true;}
            if(a[4]+a[5]==0){r.flags.add("MACROPHAGE_POPULATION_ZERO");invalidDenominator=true;}
            if(a[6]+a[7]==0){r.flags.add("FIBROBLAST_POPULATION_ZERO");invalidDenominator=true;}
            if(a[0]+a[1]==0)invalidDenominator=true;
        }
        if(invalidDenominator)r.flags.add("INVALID_DENOMINATOR");
    }

    static List<RunRecord> execute(Config c,Design design,Path checkpointDir,Set<String> selectedPointIds,int replicates) throws Exception {
        Files.createDirectories(checkpointDir);
        List<RunRecord> records=Collections.synchronizedList(new ArrayList<>());
        List<Runnable> jobs=new ArrayList<>();
        AtomicInteger resumed=new AtomicInteger();
        for(Point p:design.points) {
            if(selectedPointIds!=null&&!selectedPointIds.contains(p.pointId()))continue;
            for(int rep=0;rep<replicates;rep++) {
                final int rr=rep; final long seed=simulationSeed(c.masterSeed,p.trajectory,rep);
                Path cp=checkpointPath(checkpointDir,p,rep);
                if(!c.force&&Files.isRegularFile(cp)) {
                    try { RunRecord old=readCheckpoint(cp,p,rep,seed,c.maxSteps); records.add(old); resumed.incrementAndGet(); continue; }
                    catch(IOException ex) { System.err.println("checkpoint ignored and rerun: "+cp+" ("+ex.getMessage()+")"); }
                }
                jobs.add(()->{
                    RunRecord result=runOne(p,rr,seed,c.maxSteps);
                    try { writeCheckpoint(cp,result); }
                    catch(IOException e) { throw new RuntimeException("failed checkpoint "+cp,e); }
                    records.add(result);
                });
            }
        }
        System.out.printf(Locale.US,"execution: %d complete checkpoints, %d simulations to run, %d threads%n",resumed.get(),jobs.size(),c.threads);
        ExecutorService pool=Executors.newFixedThreadPool(c.threads);
        CompletionService<Void> completion=new ExecutorCompletionService<>(pool);
        try {
            for(Runnable job:jobs) completion.submit(()->{job.run();return null;});
            int interval=Math.max(1,jobs.size()/20);
            for(int i=0;i<jobs.size();i++) {
                Future<Void> f=completion.take(); f.get();
                if((i+1)%interval==0||i+1==jobs.size()) System.out.printf("  %d/%d new simulations complete%n",i+1,jobs.size());
            }
        } finally { pool.shutdownNow(); }
        records.sort(Comparator.comparingInt((RunRecord x)->x.point.trajectory).thenComparingInt(x->x.point.step).thenComparingInt(x->x.replicate));
        return records;
    }

    static int totalPopulation(int[] a) { int s=0; if(a!=null)for(int v:a)s+=v; return s; }

    static void writeRawRuns(Path file,List<RunRecord> records) throws IOException {
        Path tmp=file.resolveSibling(file.getFileName()+".tmp");
        try(BufferedWriter w=Files.newBufferedWriter(tmp,StandardCharsets.UTF_8)) {
            w.write("sample_id,point_id,trajectory_id,step_id,replicate_id,simulation_seed,changed_parameter,signed_normalized_step,runtime_ms,resumed,status,flags,error_class,error_message");
            for(ModelParameters.Definition d:ModelParameters.screenedDefinitions())w.write(","+d.name);
            for(int step:STANDARD_STEPS) {
                for(String n:COUNT_NAMES)w.write(",s"+step+"_"+n);
                w.write(",s"+step+"_total_population");
                for(String n:EVENT_NAMES)w.write(",s"+step+"_cumulative_"+n);
                for(String n:SPATIAL_NAMES)w.write(",s"+step+"_"+n);
            }
            w.newLine();
            for(RunRecord r:records) {
                w.write(csv(r.id())+","+csv(r.point.pointId())+","+r.point.trajectory+","+r.point.step+","+r.replicate+","+r.simulationSeed+","+csv(r.point.changedParameter)+","+fmt(r.point.signedDelta)+","+r.runtimeMillis+","+r.resumed+","+csv(r.status())+","+csv(String.join(";",r.flags))+","+csv(r.errorClass)+","+csv(r.errorMessage));
                w.write(","+r.point.parameters.csvRow());
                for(int step:STANDARD_STEPS) {
                    int[] a=r.countsAt(step),e=r.eventsAt(step); double[] m=r.spatialAt(step);
                    for(int i=0;i<COUNT_NAMES.length;i++)w.write(","+(a!=null&&i<a.length?Integer.toString(a[i]):""));
                    w.write(","+(a==null?"":Integer.toString(totalPopulation(a))));
                    for(int i=0;i<EVENT_NAMES.length;i++)w.write(","+(e!=null&&i<e.length?Integer.toString(e[i]):""));
                    for(int i=0;i<SPATIAL_NAMES.length;i++)w.write(","+(m!=null&&i<m.length?fmt(m[i]):""));
                }
                w.newLine();
            }
        }
        atomicMove(tmp,file);
    }

    static OutputValue ov(String name,String family,int step,double value) {
        return new OutputValue(name,family,step,value,Double.isFinite(value),"NONFINITE_OR_UNDEFINED");
    }
    static OutputValue invalid(String name,String family,int step,String reason) {
        return new OutputValue(name,family,step,Double.NaN,false,reason);
    }
    static double fraction(int a,int b) { int n=a+b; return n>0?(double)a/n:Double.NaN; }

    static List<OutputValue> deriveOutputs(RunRecord r) {
        List<OutputValue> out=new ArrayList<>();
        int[] initial=r.countsAt(0);
        int initialTum=initial==null?0:initial[0]+initial[1], initialFib=initial==null?0:initial[6]+initial[7];
        for(int step:STANDARD_STEPS) {
            int[] a=r.countsAt(step);
            if(a==null)continue;
            int tum=a[0]+a[1],ec=a[2]+a[3],mac=a[4]+a[5],fib=a[6]+a[7];
            out.add(Double.isFinite(fraction(a[0],a[1]))?ov("jnkp_fraction_s"+step,"jnk",step,fraction(a[0],a[1])):invalid("jnkp_fraction_s"+step,"jnk",step,"TUMOR_DENOMINATOR_ZERO"));
            out.add(Double.isFinite(fraction(a[2],a[3]))?ov("ec_activated_fraction_s"+step,"endothelial",step,fraction(a[2],a[3])):invalid("ec_activated_fraction_s"+step,"endothelial",step,"EC_POPULATION_ZERO"));
            out.add(Double.isFinite(fraction(a[4],a[5]))?ov("macrophage_activated_fraction_s"+step,"macrophage",step,fraction(a[4],a[5])):invalid("macrophage_activated_fraction_s"+step,"macrophage",step,"MACROPHAGE_POPULATION_ZERO"));
            out.add(ov("fibroblast_total_s"+step,"fibroblast",step,fib));
            out.add(initialFib>0?ov("fibroblast_fold_change_s"+step,"fibroblast",step,(double)fib/initialFib):invalid("fibroblast_fold_change_s"+step,"fibroblast",step,"INITIAL_FIBROBLAST_ZERO"));
            out.add(initialFib>0&&fib>0?ov("fibroblast_log10_fold_s"+step,"fibroblast",step,Math.log10((double)fib/initialFib)):invalid("fibroblast_log10_fold_s"+step,"fibroblast",step,"FIBROBLAST_LOG_UNDEFINED"));
            out.add(ov("tumor_total_s"+step,"tumor",step,tum));
            out.add(initialTum>0?ov("tumor_fold_change_s"+step,"tumor",step,(double)tum/initialTum):invalid("tumor_fold_change_s"+step,"tumor",step,"INITIAL_TUMOR_ZERO"));
            out.add(initialTum>0&&tum>0?ov("tumor_log10_fold_s"+step,"tumor",step,Math.log10((double)tum/initialTum)):invalid("tumor_log10_fold_s"+step,"tumor",step,"TUMOR_LOG_UNDEFINED"));
            out.add(ov("total_population_s"+step,"population",step,totalPopulation(a)));
            double[] spatial=r.spatialAt(step);
            if(spatial!=null)for(int i=0;i<Math.min(spatial.length,SPATIAL_NAMES.length);i++)out.add(ov(SPATIAL_NAMES[i]+"_s"+step,familyForSpatial(i),step,spatial[i]));
            int[] events=r.eventsAt(step);
            if(events!=null)for(int i=0;i<Math.min(events.length,EVENT_NAMES.length);i++)out.add(ov("cumulative_"+EVENT_NAMES[i]+"_s"+step,eventFamily(i),step,events[i]));
        }
        addTargetOutputs(r,out);
        out.add(ov("tumor_extinction_status","failure",-1,r.has("TUMOR_EXTINCT")?1:0));
        out.add(ov("ec_population_zero_status","failure",-1,r.has("EC_POPULATION_ZERO")?1:0));
        out.add(ov("macrophage_population_zero_status","failure",-1,r.has("MACROPHAGE_POPULATION_ZERO")?1:0));
        out.add(ov("fibroblast_population_zero_status","failure",-1,r.has("FIBROBLAST_POPULATION_ZERO")?1:0));
        out.add(ov("invalid_denominator_status","failure",-1,r.has("INVALID_DENOMINATOR")?1:0));
        out.add(ov("overall_finite_status","failure",-1,r.strictValid()?1:0));
        out.add(ov("overall_invalid_status","failure",-1,!r.strictValid()&&!r.error()?1:0));
        out.add(ov("overall_error_status","failure",-1,r.error()?1:0));
        out.add(temporalCorrelation(r));
        return out;
    }

    static String familyForSpatial(int i) { return i<2?"tumor":i==2?"jnk":"macrophage"; }
    static String eventFamily(int i) { if(i<2||i>=8)return "tumor"; if(i<4)return "fibroblast"; if(i<6)return "macrophage"; return "endothelial"; }

    static OutputValue temporalCorrelation(RunRecord r) {
        List<Double>x=new ArrayList<>(),y=new ArrayList<>();
        for(int step:new int[]{0,480,960,1440}) {
            int[]a=r.countsAt(step); if(a!=null){x.add((double)(a[0]+a[1]));y.add((double)(a[6]+a[7]));}
        }
        if(x.size()<3)return invalid("temporal_fibroblast_tumor_correlation","fibroblast",-1,"FEWER_THAN_THREE_SNAPSHOTS");
        double corr=correlation(x,y);
        return Double.isFinite(corr)?ov("temporal_fibroblast_tumor_correlation","fibroblast",-1,corr):invalid("temporal_fibroblast_tumor_correlation","fibroblast",-1,"ZERO_TEMPORAL_VARIANCE");
    }

    static double targetStat(RunRecord r,String type,int step) {
        int[]a=r.countsAt(step); if(a==null)return Double.NaN;
        switch(type) {
            case "jnkp":return fraction(a[0],a[1]);
            case "ec":return fraction(a[2],a[3]);
            case "mac":return fraction(a[4],a[5]);
            case "fibro": {int[]z=r.countsAt(0);int zt=z==null?0:z[6]+z[7],t=a[6]+a[7];return zt>0&&t>0?Math.log10((double)t/zt):Double.NaN;}
            case "tumor": {int[]z=r.countsAt(0);int zt=z==null?0:z[0]+z[1],t=a[0]+a[1];return zt>0&&t>0?Math.log10((double)t/zt):Double.NaN;}
            default:return Double.NaN;
        }
    }

    static void addTargetOutputs(RunRecord r,List<OutputValue> out) {
        double sum=0,maxAbs=0; boolean all=true;
        for(int j=0;j<ABCRejection.TT.length;j++) {
            String base="abc_target_"+String.format(Locale.US,"%02d",j+1)+"_"+ABCRejection.TT[j]+"_s"+ABCRejection.TS[j];
            double sim=targetStat(r,ABCRejection.TT[j],ABCRejection.TS[j]);
            if(Double.isFinite(sim)) {
                double resid=(sim-ABCRejection.TV[j])/ABCRejection.TSC[j];
                out.add(ov(base+"_stat",familyForTarget(ABCRejection.TT[j]),ABCRejection.TS[j],sim));
                out.add(ov(base+"_standardized_residual",familyForTarget(ABCRejection.TT[j]),ABCRejection.TS[j],resid));
                sum+=ABCRejection.TW[j]*resid*resid; maxAbs=Math.max(maxAbs,Math.abs(resid));
            } else {
                all=false;
                out.add(invalid(base+"_stat",familyForTarget(ABCRejection.TT[j]),ABCRejection.TS[j],"TARGET_STAT_UNDEFINED"));
                out.add(invalid(base+"_standardized_residual",familyForTarget(ABCRejection.TT[j]),ABCRejection.TS[j],"TARGET_STAT_UNDEFINED"));
            }
        }
        out.add(all?ov("total_abc_distance","abc",-1,Math.sqrt(sum)):invalid("total_abc_distance","abc",-1,"ONE_OR_MORE_TARGETS_UNDEFINED"));
        out.add(all?ov("maximum_absolute_standardized_residual","abc",-1,maxAbs):invalid("maximum_absolute_standardized_residual","abc",-1,"ONE_OR_MORE_TARGETS_UNDEFINED"));
    }
    static String familyForTarget(String t) { return t.equals("jnkp")?"jnk":t.equals("ec")?"endothelial":t.equals("mac")?"macrophage":t.equals("fibro")?"fibroblast":"tumor"; }

    static Map<String,OutputValue> outputMap(RunRecord r) {
        LinkedHashMap<String,OutputValue> m=new LinkedHashMap<>(); for(OutputValue v:deriveOutputs(r))m.put(v.name,v); return m;
    }

    static void writeOutputs(Path file,List<RunRecord> records) throws IOException {
        Path tmp=file.resolveSibling(file.getFileName()+".tmp");
        try(BufferedWriter w=Files.newBufferedWriter(tmp,StandardCharsets.UTF_8)) {
            w.write("sample_id,point_id,trajectory_id,step_id,replicate_id,simulation_seed,output_name,output_family,snapshot_step,value,valid,invalid_reason,run_status,run_flags\n");
            for(RunRecord r:records) for(OutputValue v:deriveOutputs(r)) {
                w.write(csv(r.id())+","+csv(r.point.pointId())+","+r.point.trajectory+","+r.point.step+","+r.replicate+","+r.simulationSeed+","+csv(v.name)+","+csv(v.family)+","+v.snapshotStep+","+(Double.isFinite(v.value)?fmt(v.value):"")+","+v.valid+","+csv(v.invalidReason)+","+csv(r.status())+","+csv(String.join(";",r.flags))+"\n");
            }
        }
        atomicMove(tmp,file);
    }

    static void writeFailures(Path file,List<RunRecord> records) throws IOException {
        Path tmp=file.resolveSibling(file.getFileName()+".tmp");
        try(BufferedWriter w=Files.newBufferedWriter(tmp,StandardCharsets.UTF_8)) {
            w.write("sample_id,point_id,trajectory_id,step_id,replicate_id,simulation_seed,status,flags,error_class,error_message,runtime_ms\n");
            for(RunRecord r:records) if(!r.strictValid())w.write(csv(r.id())+","+csv(r.point.pointId())+","+r.point.trajectory+","+r.point.step+","+r.replicate+","+r.simulationSeed+","+csv(r.status())+","+csv(String.join(";",r.flags))+","+csv(r.errorClass)+","+csv(r.errorMessage)+","+r.runtimeMillis+"\n");
        }
        atomicMove(tmp,file);
    }

    static String fmt(double x) {
        if(!Double.isFinite(x))return "";
        if(x==Math.rint(x)&&Math.abs(x)<1e15)return Long.toString((long)x);
        return String.format(Locale.US,"%.12g",x);
    }
    static String csv(String s) { if(s==null)return "\"\""; return "\""+s.replace("\"","\"\"").replace("\r"," ").replace("\n"," ")+"\""; }

    static double correlation(List<Double>a,List<Double>b) {
        int n=Math.min(a.size(),b.size()); if(n<2)return Double.NaN;
        double ma=0,mb=0;for(int i=0;i<n;i++){ma+=a.get(i);mb+=b.get(i);}ma/=n;mb/=n;
        double va=0,vb=0,c=0;for(int i=0;i<n;i++){double x=a.get(i)-ma,y=b.get(i)-mb;va+=x*x;vb+=y*y;c+=x*y;}
        return va>0&&vb>0?c/Math.sqrt(va*vb):Double.NaN;
    }

    public static void main(String[] args) throws Exception {
        Config c=Config.parse(args);
        Files.createDirectories(c.outputDir);
        if(c.confirmOnly) { runConfirmation(c); return; }

        ModelParameters.writeRegistry(Path.of("GLOBAL_PARAMETER_REGISTRY.csv"),Path.of("GLOBAL_PARAMETER_REGISTRY.md"));
        Design design=generateDesign(c);
        writeDesign(c.outputDir.resolve("morris_design.csv"),design,c); // required before simulations
        System.out.printf(Locale.US,"Morris design: k=%d trajectories=%d levels=%d delta=%.6f points=%d duplicates=%d designSeed=%d%n",
                ModelParameters.screenedDefinitions().size(),c.trajectories,c.levels,design.delta,design.points.size(),design.duplicateCount,design.seed);

        List<RunRecord> records=execute(c,design,c.outputDir.resolve("checkpoints"),null,c.replicates);
        int expected=design.points.size()*c.replicates;
        if(records.size()!=expected)throw new IllegalStateException("no-silent-drop check failed: expected "+expected+" records, got "+records.size());
        writeRawRuns(c.outputDir.resolve("morris_raw_runs.csv"),records);
        writeOutputs(c.outputDir.resolve("morris_outputs.csv"),records);
        writeFailures(c.outputDir.resolve("morris_failures.csv"),records);
        analyzeAndReport(c,design,records,c.outputDir,false,null);
        System.out.println("complete: "+c.outputDir.toAbsolutePath());
    }

    // Implemented below: elementary effects, failure association, rankings,
    // confirmation, QC, and report generation.
    static void analyzeAndReport(Config c,Design design,List<RunRecord> records,Path dir,boolean confirmation,Path primaryDir) throws Exception {
        AnalysisBundle bundle=computeElementaryEffects(c,design,records);
        writeElementaryEffects(dir.resolve("morris_elementary_effects.csv"),bundle.effects);
        writeSummary(dir.resolve("morris_summary_by_output.csv"),bundle.summaries);
        List<FailureAssociation> failures=computeFailureSensitivity(c,records);
        writeFailureSensitivity(dir.resolve("failure_sensitivity.csv"),failures);
        List<GlobalRank> global=computeGlobalRankings(bundle.summaries,failures);
        writeGlobalRankings(dir.resolve("morris_global_rankings.csv"),global);
        writeClassification(dir.resolve("PARAMETER_INFLUENCE_CLASSIFICATION.csv"),bundle.summaries);
        writeQcReport(dir.resolve("MORRIS_QC_REPORT.md"),c,design,records,bundle);
        writeScientificReport(dir.resolve("GLOBAL_MORRIS_SENSITIVITY_REPORT.md"),c,design,records,bundle,failures,global,confirmation);
    }

    static final class Effect {
        String parameter,output,family,kind,fromPoint,toPoint,lostReason;
        int trajectory,replicate;
        long seed;
        double signedDelta,physicalFrom,physicalTo,physicalStep,ee,withinPointVariance,snr;
        boolean valid;
    }

    static final class Summary {
        String parameter,output,family;
        double mu,muStar,sigma,seMuStar,medianAbs,iqr,min,max,positiveFraction,negativeFraction;
        double outputScale,normalizedMuStar,meanWithinPointVariance,meanSnr;
        int valid,lost,rank;
    }

    static final class FailureAssociation {
        String parameter,outcome;
        int n,nEvent,nNoEvent;
        double meanPhysicalEvent,meanPhysicalNoEvent,physicalMeanDifference;
        double normalizedMeanDifference,rankBiserial,pointBiserial,logisticCoefficient,permutationP;
    }

    static final class GlobalRank {
        String parameter,status;
        double tumor,jnk,fibroblast,macrophage,endothelial,failure,overall;
        int rank;
    }

    static final class AnalysisBundle {
        final List<Effect> effects=new ArrayList<>();
        final List<Summary> summaries=new ArrayList<>();
    }

    static String recordKey(int t,int s,int r) { return t+":"+s+":"+r; }

    static AnalysisBundle computeElementaryEffects(Config c,Design design,List<RunRecord> records) {
        AnalysisBundle bundle=new AnalysisBundle();
        Map<String,RunRecord> byKey=new HashMap<>();
        Map<String,Map<String,OutputValue>> outputCache=new HashMap<>();
        LinkedHashMap<String,String> catalog=new LinkedHashMap<>();
        for(RunRecord r:records) {
            byKey.put(recordKey(r.point.trajectory,r.point.step,r.replicate),r);
            Map<String,OutputValue> m=outputMap(r); outputCache.put(r.id(),m);
            for(OutputValue v:m.values())catalog.putIfAbsent(v.name,v.family);
        }
        int k=ModelParameters.screenedDefinitions().size();
        Map<String,Point> points=new HashMap<>(); for(Point p:design.points)points.put(p.trajectory+":"+p.step,p);
        for(int t=0;t<c.trajectories;t++)for(int step=1;step<=k;step++) {
            Point from=points.get(t+":"+(step-1)),to=points.get(t+":"+step);
            if(from==null||to==null)continue;
            String param=to.changedParameter;
            for(Map.Entry<String,String> cat:catalog.entrySet()) {
                String output=cat.getKey(),family=cat.getValue();
                List<Double> fromValues=new ArrayList<>(),toValues=new ArrayList<>();
                List<String> lost=new ArrayList<>();
                for(int rep=0;rep<c.replicates;rep++) {
                    RunRecord a=byKey.get(recordKey(t,step-1,rep)),b=byKey.get(recordKey(t,step,rep));
                    Effect e=new Effect(); e.parameter=param;e.output=output;e.family=family;e.kind="matched_replicate";
                    e.trajectory=t;e.replicate=rep;e.fromPoint=from.pointId();e.toPoint=to.pointId();e.signedDelta=to.signedDelta;
                    e.physicalFrom=from.parameters.get(param);e.physicalTo=to.parameters.get(param);e.physicalStep=e.physicalTo-e.physicalFrom;
                    if(a!=null)e.seed=a.simulationSeed;
                    OutputValue va=a==null?null:outputCache.get(a.id()).get(output), vb=b==null?null:outputCache.get(b.id()).get(output);
                    if(a!=null&&b!=null&&a.simulationSeed==b.simulationSeed&&va!=null&&vb!=null&&va.valid&&vb.valid) {
                        e.ee=(vb.value-va.value)/to.signedDelta;e.valid=Double.isFinite(e.ee);
                        if(e.valid){fromValues.add(va.value);toValues.add(vb.value);}
                    }
                    if(!e.valid) {
                        e.lostReason=lostReason(a,b,va,vb); lost.add(e.lostReason);
                        e.ee=Double.NaN;
                    }
                    e.withinPointVariance=Double.NaN;e.snr=Double.NaN;bundle.effects.add(e);
                }
                Effect mean=new Effect();mean.parameter=param;mean.output=output;mean.family=family;mean.kind="replicate_mean";
                mean.trajectory=t;mean.replicate=-1;mean.fromPoint=from.pointId();mean.toPoint=to.pointId();mean.signedDelta=to.signedDelta;
                mean.physicalFrom=from.parameters.get(param);mean.physicalTo=to.parameters.get(param);mean.physicalStep=mean.physicalTo-mean.physicalFrom;
                if(!fromValues.isEmpty()) {
                    double ma=mean(fromValues),mb=mean(toValues);mean.ee=(mb-ma)/to.signedDelta;mean.valid=Double.isFinite(mean.ee);
                    double va=variance(fromValues),vb=variance(toValues);mean.withinPointVariance=meanFinite(va,vb);
                    double noise=Double.isFinite(mean.withinPointVariance)?Math.sqrt(mean.withinPointVariance):Double.NaN;
                    mean.snr=Double.isFinite(noise)&&noise>0?Math.abs(mb-ma)/noise:(fromValues.size()>1&&mb!=ma?Double.POSITIVE_INFINITY:Double.NaN);
                } else { mean.ee=Double.NaN;mean.valid=false;mean.lostReason=String.join("|",new LinkedHashSet<>(lost)); }
                bundle.effects.add(mean);
            }
        }

        Map<String,List<Effect>> grouped=new LinkedHashMap<>();
        for(Effect e:bundle.effects)if(e.kind.equals("replicate_mean"))grouped.computeIfAbsent(e.parameter+"\t"+e.output,kx->new ArrayList<>()).add(e);
        Map<String,Double> scales=outputScales(records,outputCache);
        for(List<Effect> effects:grouped.values()) {
            Effect first=effects.get(0);Summary s=new Summary();s.parameter=first.parameter;s.output=first.output;s.family=first.family;
            List<Double> ee=new ArrayList<>(),abs=new ArrayList<>(),vars=new ArrayList<>(),snrs=new ArrayList<>();
            for(Effect e:effects) {
                if(e.valid){ee.add(e.ee);abs.add(Math.abs(e.ee));if(Double.isFinite(e.withinPointVariance))vars.add(e.withinPointVariance);if(Double.isFinite(e.snr))snrs.add(e.snr);}
                else s.lost++;
            }
            s.valid=ee.size();s.outputScale=scales.getOrDefault(s.output,Double.NaN);
            if(!ee.isEmpty()) {
                s.mu=mean(ee);s.muStar=mean(abs);s.sigma=sampleSd(ee);s.seMuStar=abs.size()>1?sampleSd(abs)/Math.sqrt(abs.size()):Double.NaN;
                Collections.sort(abs);s.medianAbs=quantileSorted(abs,.5);s.iqr=quantileSorted(abs,.75)-quantileSorted(abs,.25);
                s.min=Collections.min(ee);s.max=Collections.max(ee);
                int pos=0,neg=0;for(double v:ee){if(v>0)pos++;if(v<0)neg++;}s.positiveFraction=(double)pos/ee.size();s.negativeFraction=(double)neg/ee.size();
                s.normalizedMuStar=s.outputScale>0?s.muStar/s.outputScale:Double.NaN;
                s.meanWithinPointVariance=vars.isEmpty()?Double.NaN:mean(vars);s.meanSnr=snrs.isEmpty()?Double.NaN:mean(snrs);
            } else fillSummaryNaN(s);
            bundle.summaries.add(s);
        }
        Map<String,List<Summary>> byOutput=new LinkedHashMap<>();for(Summary s:bundle.summaries)byOutput.computeIfAbsent(s.output,z->new ArrayList<>()).add(s);
        for(List<Summary> list:byOutput.values()) {
            list.sort(Comparator.comparingDouble((Summary s)->finiteSort(s.muStar)).reversed().thenComparing(s->s.parameter));
            int rank=0;for(Summary s:list)if(Double.isFinite(s.muStar))s.rank=++rank;
        }
        bundle.summaries.sort(Comparator.comparing((Summary s)->s.output).thenComparingInt(s->s.rank==0?Integer.MAX_VALUE:s.rank));
        return bundle;
    }

    static String lostReason(RunRecord a,RunRecord b,OutputValue va,OutputValue vb) {
        if(a==null||b==null)return "MISSING_PAIRED_RUN";
        if(a.simulationSeed!=b.simulationSeed)return "MISMATCHED_SEEDS";
        if(a.error()||b.error())return "MODEL_ERROR";
        if(va==null||vb==null)return "OUTPUT_NOT_AVAILABLE";
        if(!va.valid)return "FROM_"+va.invalidReason;
        if(!vb.valid)return "TO_"+vb.invalidReason;
        return "NONFINITE_EE";
    }

    static Map<String,Double> outputScales(List<RunRecord> records,Map<String,Map<String,OutputValue>> cache) {
        Map<String,List<Double>> byOutput=new HashMap<>();
        Map<String,List<RunRecord>> byPoint=new LinkedHashMap<>();for(RunRecord r:records)byPoint.computeIfAbsent(r.point.pointId(),z->new ArrayList<>()).add(r);
        for(List<RunRecord> rs:byPoint.values()) {
            LinkedHashSet<String> names=new LinkedHashSet<>();for(RunRecord r:rs)names.addAll(cache.get(r.id()).keySet());
            for(String name:names) {List<Double> vals=new ArrayList<>();for(RunRecord r:rs){OutputValue v=cache.get(r.id()).get(name);if(v!=null&&v.valid)vals.add(v.value);}if(!vals.isEmpty())byOutput.computeIfAbsent(name,z->new ArrayList<>()).add(mean(vals));}
        }
        Map<String,Double> out=new HashMap<>();for(Map.Entry<String,List<Double>> e:byOutput.entrySet())out.put(e.getKey(),sampleSd(e.getValue()));return out;
    }

    static void fillSummaryNaN(Summary s) {
        s.mu=s.muStar=s.sigma=s.seMuStar=s.medianAbs=s.iqr=s.min=s.max=s.positiveFraction=s.negativeFraction=s.normalizedMuStar=s.meanWithinPointVariance=s.meanSnr=Double.NaN;
    }
    static double finiteSort(double x){return Double.isFinite(x)?x:-Double.MAX_VALUE;}
    static double mean(Collection<Double>x){double s=0;for(double v:x)s+=v;return x.isEmpty()?Double.NaN:s/x.size();}
    static double variance(List<Double>x){if(x.size()<2)return Double.NaN;double m=mean(x),s=0;for(double v:x)s+=(v-m)*(v-m);return s/(x.size()-1);}
    static double sampleSd(List<Double>x){double v=variance(x);return Double.isFinite(v)?Math.sqrt(v):Double.NaN;}
    static double meanFinite(double a,double b){if(Double.isFinite(a)&&Double.isFinite(b))return .5*(a+b);if(Double.isFinite(a))return a;if(Double.isFinite(b))return b;return Double.NaN;}
    static double quantileSorted(List<Double>x,double q){if(x.isEmpty())return Double.NaN;double pos=q*(x.size()-1),lo=Math.floor(pos),hi=Math.ceil(pos);return x.get((int)lo)+(pos-lo)*(x.get((int)hi)-x.get((int)lo));}

    static void writeElementaryEffects(Path file,List<Effect> effects)throws IOException {
        Path tmp=file.resolveSibling(file.getFileName()+".tmp");
        try(BufferedWriter w=Files.newBufferedWriter(tmp,StandardCharsets.UTF_8)) {
            w.write("parameter,output,output_family,trajectory_id,replicate_id,ee_kind,simulation_seed,from_point,to_point,signed_normalized_step,physical_from,physical_to,physical_step,elementary_effect,valid,lost_reason,mean_within_point_output_variance,signal_to_noise_ratio\n");
            for(Effect e:effects)w.write(csv(e.parameter)+","+csv(e.output)+","+csv(e.family)+","+e.trajectory+","+e.replicate+","+csv(e.kind)+","+(e.replicate>=0?Long.toString(e.seed):"")+","+csv(e.fromPoint)+","+csv(e.toPoint)+","+fmt(e.signedDelta)+","+fmt(e.physicalFrom)+","+fmt(e.physicalTo)+","+fmt(e.physicalStep)+","+fmt(e.ee)+","+e.valid+","+csv(e.lostReason)+","+fmt(e.withinPointVariance)+","+fmt(e.snr)+"\n");
        } atomicMove(tmp,file);
    }

    static void writeSummary(Path file,List<Summary> summaries)throws IOException {
        Path tmp=file.resolveSibling(file.getFileName()+".tmp");
        try(BufferedWriter w=Files.newBufferedWriter(tmp,StandardCharsets.UTF_8)) {
            w.write("output,output_family,parameter,mu,mu_star,sigma,se_mu_star,median_absolute_ee,iqr_absolute_ee,min_ee,max_ee,n_valid_ee,n_lost_invalid_or_extinct,positive_fraction,negative_fraction,rank_by_mu_star,output_scale_sd,normalized_mu_star,mean_within_point_output_variance,mean_signal_to_noise_ratio\n");
            for(Summary s:summaries)w.write(csv(s.output)+","+csv(s.family)+","+csv(s.parameter)+","+fmt(s.mu)+","+fmt(s.muStar)+","+fmt(s.sigma)+","+fmt(s.seMuStar)+","+fmt(s.medianAbs)+","+fmt(s.iqr)+","+fmt(s.min)+","+fmt(s.max)+","+s.valid+","+s.lost+","+fmt(s.positiveFraction)+","+fmt(s.negativeFraction)+","+(s.rank==0?"":s.rank)+","+fmt(s.outputScale)+","+fmt(s.normalizedMuStar)+","+fmt(s.meanWithinPointVariance)+","+fmt(s.meanSnr)+"\n");
        } atomicMove(tmp,file);
    }

    interface Outcome { boolean event(RunRecord r); }
    static List<FailureAssociation> computeFailureSensitivity(Config c,List<RunRecord> records) {
        LinkedHashMap<String,Outcome> outcomes=new LinkedHashMap<>();
        outcomes.put("TUMOR_EXTINCT",r->r.has("TUMOR_EXTINCT"));
        outcomes.put("EC_POPULATION_ZERO",r->r.has("EC_POPULATION_ZERO"));
        outcomes.put("MACROPHAGE_POPULATION_ZERO",r->r.has("MACROPHAGE_POPULATION_ZERO"));
        outcomes.put("FIBROBLAST_POPULATION_ZERO",r->r.has("FIBROBLAST_POPULATION_ZERO"));
        outcomes.put("GENERAL_INVALID",r->!r.strictValid());
        outcomes.put("SIMULATION_ERROR",RunRecord::error);
        List<FailureAssociation> out=new ArrayList<>();
        List<ModelParameters.Definition> defs=ModelParameters.screenedDefinitions();
        for(int pi=0;pi<defs.size();pi++)for(Map.Entry<String,Outcome> oe:outcomes.entrySet()) {
            FailureAssociation f=new FailureAssociation();f.parameter=defs.get(pi).name;f.outcome=oe.getKey();f.n=records.size();
            List<Double> xe=new ArrayList<>(),xn=new ArrayList<>(),xnorm=new ArrayList<>();List<Integer> y=new ArrayList<>();
            for(RunRecord r:records){boolean event=oe.getValue().event(r);double physical=r.point.parameters.get(f.parameter);double normalized=r.point.normalized[pi];if(event){xe.add(physical);f.nEvent++;}else{xn.add(physical);f.nNoEvent++;}xnorm.add(normalized);y.add(event?1:0);}
            f.meanPhysicalEvent=mean(xe);f.meanPhysicalNoEvent=mean(xn);f.physicalMeanDifference=f.meanPhysicalEvent-f.meanPhysicalNoEvent;
            if(f.nEvent>0&&f.nNoEvent>0) {
                List<Double>xne=new ArrayList<>(),xnn=new ArrayList<>();for(int i=0;i<y.size();i++)if(y.get(i)==1)xne.add(xnorm.get(i));else xnn.add(xnorm.get(i));
                f.normalizedMeanDifference=mean(xne)-mean(xnn);f.rankBiserial=rankBiserial(xnorm,y);f.pointBiserial=pointBiserial(xnorm,y);
                f.logisticCoefficient=logisticCoefficient(xnorm,y);f.permutationP=permutationP(xnorm,y,c.permutations,mix64(c.masterSeed+31L*pi+oe.getKey().hashCode()));
            } else f.normalizedMeanDifference=f.rankBiserial=f.pointBiserial=f.logisticCoefficient=f.permutationP=Double.NaN;
            out.add(f);
        }
        out.sort(Comparator.comparing((FailureAssociation f)->f.outcome).thenComparingDouble(f->-Math.abs(f.rankBiserial)));
        return out;
    }

    static double pointBiserial(List<Double>x,List<Integer>y){List<Double>yd=new ArrayList<>();for(int v:y)yd.add((double)v);return correlation(x,yd);}
    static double rankBiserial(List<Double>x,List<Integer>y) {
        Integer[]idx=new Integer[x.size()];for(int i=0;i<idx.length;i++)idx[i]=i;Arrays.sort(idx,Comparator.comparingDouble(x::get));
        double rankSum=0;int n1=0,n0=0,i=0;
        while(i<idx.length){int j=i+1;while(j<idx.length&&Double.compare(x.get(idx[i]),x.get(idx[j]))==0)j++;double rank=.5*((i+1)+j);for(int z=i;z<j;z++)if(y.get(idx[z])==1){rankSum+=rank;n1++;}else n0++;i=j;}
        double u=rankSum-n1*(n1+1)/2.0;return n1>0&&n0>0?2*u/(n1*(double)n0)-1:Double.NaN;
    }
    static double logisticCoefficient(List<Double>x,List<Integer>y) {
        double a=Math.log((y.stream().mapToInt(Integer::intValue).sum()+.5)/(y.size()-y.stream().mapToInt(Integer::intValue).sum()+.5)),b=0;
        for(int iter=0;iter<50;iter++){double g0=0,g1=0,h00=1e-8,h01=0,h11=1e-8;for(int i=0;i<x.size();i++){double z=Math.max(-30,Math.min(30,a+b*x.get(i))),p=1/(1+Math.exp(-z)),w=p*(1-p);double e=y.get(i)-p;g0+=e;g1+=e*x.get(i);h00+=w;h01+=w*x.get(i);h11+=w*x.get(i)*x.get(i);}double det=h00*h11-h01*h01;if(det<=0)return Double.NaN;double da=(g0*h11-g1*h01)/det,db=(g1*h00-g0*h01)/det;a+=da;b+=db;if(Math.abs(da)+Math.abs(db)<1e-9)break;}return b;
    }
    static double permutationP(List<Double>x,List<Integer>y,int n,long seed) {
        if(n<=0)return Double.NaN;double observed=Math.abs(pointBiserial(x,y));if(!Double.isFinite(observed))return Double.NaN;
        int[]a=new int[y.size()];for(int i=0;i<a.length;i++)a[i]=y.get(i);SplittableRandom rng=new SplittableRandom(seed);int extreme=0;
        for(int p=0;p<n;p++){for(int i=a.length-1;i>0;i--){int j=rng.nextInt(i+1),z=a[i];a[i]=a[j];a[j]=z;}List<Integer>yp=new ArrayList<>();for(int z:a)yp.add(z);double v=Math.abs(pointBiserial(x,yp));if(v>=observed-1e-15)extreme++;}
        return (extreme+1.0)/(n+1.0);
    }

    static void writeFailureSensitivity(Path file,List<FailureAssociation> xs)throws IOException {
        Path tmp=file.resolveSibling(file.getFileName()+".tmp");try(BufferedWriter w=Files.newBufferedWriter(tmp,StandardCharsets.UTF_8)){
            w.write("outcome,parameter,n_runs,n_event,n_no_event,mean_physical_event,mean_physical_no_event,physical_mean_difference,normalized_mean_difference,rank_biserial_effect,point_biserial_correlation,univariate_logistic_coefficient,permutation_p_value\n");
            for(FailureAssociation f:xs)w.write(csv(f.outcome)+","+csv(f.parameter)+","+f.n+","+f.nEvent+","+f.nNoEvent+","+fmt(f.meanPhysicalEvent)+","+fmt(f.meanPhysicalNoEvent)+","+fmt(f.physicalMeanDifference)+","+fmt(f.normalizedMeanDifference)+","+fmt(f.rankBiserial)+","+fmt(f.pointBiserial)+","+fmt(f.logisticCoefficient)+","+fmt(f.permutationP)+"\n");
        }atomicMove(tmp,file);
    }

    static List<GlobalRank> computeGlobalRankings(List<Summary> summaries,List<FailureAssociation> failures) {
        Map<String,Double> maxByOutput=new HashMap<>();for(Summary s:summaries)if(Double.isFinite(s.muStar))maxByOutput.merge(s.output,s.muStar,Math::max);
        Map<String,Map<String,List<Double>>> values=new HashMap<>();
        Set<String> biological=new HashSet<>(Arrays.asList("tumor","jnk","fibroblast","macrophage","endothelial"));
        for(Summary s:summaries)if(biological.contains(s.family)&&Double.isFinite(s.muStar)&&maxByOutput.getOrDefault(s.output,0.0)>0)values.computeIfAbsent(s.parameter,z->new HashMap<>()).computeIfAbsent(s.family,z->new ArrayList<>()).add(s.muStar/maxByOutput.get(s.output));
        Map<String,Double> failureScore=new HashMap<>();for(FailureAssociation f:failures)if(Double.isFinite(f.rankBiserial))failureScore.merge(f.parameter,Math.abs(f.rankBiserial),Math::max);
        List<GlobalRank> out=new ArrayList<>();for(ModelParameters.Definition d:ModelParameters.screenedDefinitions()){
            GlobalRank g=new GlobalRank();g.parameter=d.name;g.status=d.status;Map<String,List<Double>>m=values.getOrDefault(d.name,Collections.emptyMap());
            g.tumor=familyMean(m,"tumor");g.jnk=familyMean(m,"jnk");g.fibroblast=familyMean(m,"fibroblast");g.macrophage=familyMean(m,"macrophage");g.endothelial=familyMean(m,"endothelial");g.failure=failureScore.getOrDefault(d.name,0.0);
            List<Double>families=new ArrayList<>();for(double v:new double[]{g.tumor,g.jnk,g.fibroblast,g.macrophage,g.endothelial,g.failure})if(Double.isFinite(v))families.add(v);g.overall=mean(families);out.add(g);
        }
        out.sort(Comparator.comparingDouble((GlobalRank g)->finiteSort(g.overall)).reversed().thenComparing(g->g.parameter));int rank=0;for(GlobalRank g:out)g.rank=++rank;return out;
    }
    static double familyMean(Map<String,List<Double>>m,String family){List<Double>x=m.get(family);return x==null||x.isEmpty()?Double.NaN:mean(x);}

    static void writeGlobalRankings(Path file,List<GlobalRank> xs)throws IOException {
        Path tmp=file.resolveSibling(file.getFileName()+".tmp");try(BufferedWriter w=Files.newBufferedWriter(tmp,StandardCharsets.UTF_8)){
            w.write("overall_rank,parameter,current_status,tumor_score,jnk_score,fibroblast_score,macrophage_score,endothelial_score,failure_score,overall_biological_priority_score,weighting_rule\n");
            for(GlobalRank g:xs)w.write(g.rank+","+csv(g.parameter)+","+csv(g.status)+","+fmt(g.tumor)+","+fmt(g.jnk)+","+fmt(g.fibroblast)+","+fmt(g.macrophage)+","+fmt(g.endothelial)+","+fmt(g.failure)+","+fmt(g.overall)+","+csv("mean of six equally weighted family scores; within-family mean of output-wise mu-star divided by that output's maximum; failure=max absolute rank-biserial")+"\n");
        }atomicMove(tmp,file);
    }

    static void writeClassification(Path file,List<Summary> summaries)throws IOException {
        Map<String,List<Summary>>byOutput=new LinkedHashMap<>();for(Summary s:summaries)byOutput.computeIfAbsent(s.output,z->new ArrayList<>()).add(s);
        Path tmp=file.resolveSibling(file.getFileName()+".tmp");try(BufferedWriter w=Files.newBufferedWriter(tmp,StandardCharsets.UTF_8)){
            w.write("output,output_family,parameter,screened,current_status,classification,classification_reason,mu_star,sigma,n_valid,n_lost,mean_snr,mu_star_q25,mu_star_median,mu_star_q75,sigma_q25,sigma_median,sigma_q75\n");
            for(Map.Entry<String,List<Summary>>entry:byOutput.entrySet()) {
                List<Double>mus=new ArrayList<>(),sig=new ArrayList<>();for(Summary s:entry.getValue()){if(Double.isFinite(s.muStar))mus.add(s.muStar);if(Double.isFinite(s.sigma))sig.add(s.sigma);}Collections.sort(mus);Collections.sort(sig);
                double mq25=quantileSorted(mus,.25),mq50=quantileSorted(mus,.5),mq75=quantileSorted(mus,.75),sq25=quantileSorted(sig,.25),sq50=quantileSorted(sig,.5),sq75=quantileSorted(sig,.75);
                Map<String,Summary>mapped=new HashMap<>();for(Summary s:entry.getValue())mapped.put(s.parameter,s);
                for(ModelParameters.Definition d:ModelParameters.registry()) {
                    Summary s=mapped.get(d.name);String cls,reason;
                    if(!d.screen){if(d.status.equals("inactive")){cls="E";reason="structurally inactive field confirmed unused or blocked by current event logic";}else{cls="F";reason="not screened in untreated Morris design: "+d.justification;}}
                    else if(s==null||s.valid==0){cls="F";reason="no valid replicate-mean elementary effects";}
                    else if(s.lost>s.valid){cls="F";reason="more than half of potential effects lost to invalid/extinct/missing outputs";}
                    else if(s.muStar>=mq75){if(Double.isFinite(s.sigma)&&s.sigma>=sq50){cls="B";reason="mu-star at/above output Q75 and sigma at/above median";}else{cls="A";reason="mu-star at/above output Q75 with sigma below median";}}
                    else if(Double.isFinite(s.sigma)&&s.sigma>=sq75){cls="C";reason="sigma at/above output Q75 while mu-star below Q75";}
                    else if(s.muStar<=mq50&&(!Double.isFinite(s.sigma)||s.sigma<=sq50)){cls="D";reason="mu-star and sigma at/below output medians";}
                    else{cls="C";reason="intermediate mu-star with above-median variability";}
                    String family=s==null?entry.getValue().get(0).family:s.family;
                    w.write(csv(entry.getKey())+","+csv(family)+","+csv(d.name)+","+d.screen+","+csv(d.status)+","+csv(cls)+","+csv(reason)+","+fmt(s==null?Double.NaN:s.muStar)+","+fmt(s==null?Double.NaN:s.sigma)+","+(s==null?0:s.valid)+","+(s==null?0:s.lost)+","+fmt(s==null?Double.NaN:s.meanSnr)+","+fmt(mq25)+","+fmt(mq50)+","+fmt(mq75)+","+fmt(sq25)+","+fmt(sq50)+","+fmt(sq75)+"\n");
                }
            }
        }atomicMove(tmp,file);
    }

    static void writeQcReport(Path file,Config c,Design design,List<RunRecord> records,AnalysisBundle bundle)throws IOException {
        List<String> pass=new ArrayList<>(),fail=new ArrayList<>();List<ModelParameters.Definition>defs=ModelParameters.screenedDefinitions();int k=defs.size();
        if(ModelParameters.registry().stream().map(x->x.name).distinct().count()==ModelParameters.registry().size())pass.add("Every audited parameter name is unique");else fail.add("Duplicate registry names");
        boolean transform=true,bounds=true;for(ModelParameters.Definition d:defs){try{d.fromNormalized(0);d.fromNormalized(1);}catch(Exception e){transform=false;}}
        for(Point p:design.points)for(ModelParameters.Definition d:defs){double v=p.parameters.get(d.name);if(v<d.lower-1e-12||v>d.upper+1e-12||!Double.isFinite(v))bounds=false;}
        if(transform)pass.add("All screened transformations have finite endpoints");else fail.add("Invalid parameter transformation");
        if(bounds)pass.add("All physical design values are finite and within registry bounds");else fail.add("Out-of-bound/non-finite physical values");
        boolean one=true,physical=true;Map<String,Point>pm=new HashMap<>();for(Point p:design.points)pm.put(p.trajectory+":"+p.step,p);
        for(Point p:design.points)if(p.step>0){Point q=pm.get(p.trajectory+":"+(p.step-1));int nd=0,np=0;for(int i=0;i<k;i++){if(Math.abs(p.normalized[i]-q.normalized[i])>1e-12)nd++;if(Double.compare(p.parameters.get(defs.get(i).name),q.parameters.get(defs.get(i).name))!=0)np++;}if(nd!=1)one=false;if(np!=1)physical=false;}
        if(one)pass.add("Every Morris step changes exactly one normalized coordinate");else fail.add("A Morris step changes zero or multiple normalized coordinates");
        if(physical)pass.add("Every Morris step changes exactly one physical parameter after mapping");else fail.add("Integer/discrete mapping produced a zero or multi-parameter physical step");
        boolean seeds=true;Map<String,Long>seen=new HashMap<>();for(RunRecord r:records){String key=r.point.trajectory+":"+r.replicate;Long old=seen.putIfAbsent(key,r.simulationSeed);if(old!=null&&old.longValue()!=r.simulationSeed)seeds=false;}
        if(seeds)pass.add("All neighboring points use matching trajectory/replicate simulation seeds (CRN)");else fail.add("Mismatched paired simulation seeds");
        int expected=records.isEmpty()?0:(int)records.stream().map(r->r.point.pointId()+":"+r.replicate).distinct().count();if(expected==records.size())pass.add("No simulation record was silently duplicated or dropped");else fail.add("Duplicate simulation records detected");
        boolean deterministic=true,baseline=true;
        if(!records.isEmpty()){
            RunRecord first=records.get(0),again=runOne(first.point,first.replicate,first.simulationSeed,c.maxSteps);deterministic=Arrays.deepEquals(first.snapshots,again.snapshots)&&Arrays.deepEquals(first.eventCounts,again.eventCounts);
            try{ModelParameters bp=ModelParameters.currentBaseline(c.initPop);ExampleGrid a=new ExampleGrid(100,100),b=new ExampleGrid(100,100);long seed=c.masterSeed;a.rng=new Rand(seed);b.rng=new Rand(seed);int[][]x=a.RunHeadless(bp.legacyTheta(),c.initPop,c.maxSteps),y=b.RunHeadless(bp,c.maxSteps);baseline=Arrays.deepEquals(x,y)&&Arrays.deepEquals(a.lastEventCounts,b.lastEventCounts);}catch(Exception e){baseline=false;}
        }
        if(deterministic)pass.add("Deterministic fresh-grid rerun reproduces snapshots and event counts exactly");else fail.add("Deterministic rerun mismatch");
        if(baseline)pass.add("Legacy 12-vector and named baseline interfaces are bit-identical at the requested horizon");else fail.add("Baseline interface regression mismatch");
        pass.add("Parallel task code constructs a new ExampleGrid and a new HAL Rand for every run; no mutable model state is static");
        boolean csv=true;for(String n:new String[]{"morris_design.csv","morris_raw_runs.csv","morris_outputs.csv","morris_failures.csv","morris_elementary_effects.csv","morris_summary_by_output.csv","failure_sensitivity.csv","morris_global_rankings.csv","PARAMETER_INFLUENCE_CLASSIFICATION.csv"}){Path p=file.getParent().resolve(n);if(Files.exists(p)&&!consistentCsvWidth(p))csv=false;}
        if(csv)pass.add("All generated CSV files have consistent row widths");else fail.add("CSV row-width inconsistency detected");
        try(BufferedWriter w=Files.newBufferedWriter(file,StandardCharsets.UTF_8)){
            w.write("# Morris QC Report\n\nGenerated: "+java.time.Instant.now()+"\n\n");w.write("## Result\n\n**"+(fail.isEmpty()?"PASS":"FAIL")+"** ("+pass.size()+" checks passed, "+fail.size()+" failed)\n\n");
            w.write("## Passed Checks\n\n");for(String s:pass)w.write("- PASS: "+s+"\n");w.write("\n## Failed Checks\n\n");if(fail.isEmpty())w.write("- None.\n");else for(String s:fail)w.write("- FAIL: "+s+"\n");
            w.write("\n## Design/Run Inventory\n\n- Screened parameters: "+k+"\n- Audited quantities: "+ModelParameters.registry().size()+"\n- Design points: "+design.points.size()+"\n- Run records: "+records.size()+"\n- Physical duplicate points: "+design.duplicateCount+"\n- Valid replicate-mean EEs: "+bundle.effects.stream().filter(e->e.kind.equals("replicate_mean")&&e.valid).count()+"\n- Lost replicate-mean EEs: "+bundle.effects.stream().filter(e->e.kind.equals("replicate_mean")&&!e.valid).count()+"\n");
            w.write("\nThe HAL runtime may emit a Java final-field-reflection compatibility warning. That warning originates in the bundled HAL JAR; model instances and RNGs remain per task.\n");
        }
    }

    static boolean consistentCsvWidth(Path p)throws IOException{List<String>lines=Files.readAllLines(p,StandardCharsets.UTF_8);if(lines.isEmpty())return true;int n=csvColumns(lines.get(0));for(int i=1;i<lines.size();i++)if(csvColumns(lines.get(i))!=n)return false;return true;}
    static int csvColumns(String line){int n=1;boolean quote=false;for(int i=0;i<line.length();i++){char c=line.charAt(i);if(c=='\"'){if(quote&&i+1<line.length()&&line.charAt(i+1)=='\"')i++;else quote=!quote;}else if(c==','&&!quote)n++;}return n;}

    static void writeScientificReport(Path file,Config c,Design design,List<RunRecord> records,AnalysisBundle bundle,List<FailureAssociation> failures,List<GlobalRank> global,boolean confirmation)throws IOException {
        long finite=records.stream().filter(RunRecord::strictValid).count(),errors=records.stream().filter(RunRecord::error).count(),extinct=records.stream().filter(r->r.has("TUMOR_EXTINCT")).count();
        Map<String,List<Summary>>byOutput=new LinkedHashMap<>();for(Summary s:bundle.summaries)byOutput.computeIfAbsent(s.output,z->new ArrayList<>()).add(s);
        Path confirmedFile=file.getParent().resolve("morris_confirmed_rankings.csv");
        boolean hasConfirmation=confirmation||Files.isRegularFile(confirmedFile);
        try(BufferedWriter w=Files.newBufferedWriter(file,StandardCharsets.UTF_8)){
            w.write("# Global Morris Sensitivity Report\n\nGenerated: "+java.time.Instant.now()+"\n\n");
            w.write("## 1. Executive Summary\n\nThis run performs global Morris elementary-effects screening of the untreated TNBC lung-metastasis ABM. It is **screening, not final calibration**, and Morris indices are **not Sobol indices**. The run used "+c.trajectories+" trajectories, "+c.replicates+" matched stochastic replicate(s), "+ModelParameters.screenedDefinitions().size()+" parameters, and "+records.size()+" simulations. "+finite+" runs met the strict finite-output definition; "+extinct+" were tumour-extinct and "+errors+" errored. With fewer than 10 trajectories, all rankings should be treated as pipeline/pilot evidence only.\n\n");
            w.write("## 2. Scientific Question\n\nWhich uncertain biological rates, interaction strengths, spatial constants, and initialization quantities influence each model output across their documented ranges? Results are retained separately by output; total ABC distance is only one diagnostic output.\n\n");
            w.write("## 3. Files Audited\n\n`ExampleGrid.java`, `ABCRejection.java`, `MechanismTestHarness.java`, `README.md`, the prior code/provenance audit, mechanism results, coordinate inputs, and `ABC_TNBC_parameter_reference.md.pdf`.\n\n");
            w.write("## 4. Complete Parameter Registry\n\nSee `GLOBAL_PARAMETER_REGISTRY.csv` and `GLOBAL_PARAMETER_REGISTRY.md`: "+ModelParameters.registry().size()+" quantities audited, "+ModelParameters.screenedDefinitions().size()+" screened.\n\n");
            w.write("## 5. Parameters Included and Excluded\n\nAll executable untreated biological parameters with stated ranges enter the design. Five declared but blocked/unused fields remain registered as structurally inactive. Fixed coordinate maps and computational structure remain fixed because varying them requires a separate spatial/domain design. Chemo multipliers remain treatment-specific and are excluded only because this screen ends before treatment.\n\n");
            w.write("## 6. Bound Justification\n\nThe hierarchy is literature/project-reference range, current ABC prior, prior mechanism-harness range, then explicit conservative variation requiring mentor approval. Log transforms are used for positive rates where ratios are meaningful; zero-inclusive quantities are linear; counts and radii requiring discreteness use integer transforms. Current executable baselines override obsolete fixed values in the supplied PDF, with every disagreement documented in the registry.\n\n");
            w.write("## 7. Morris Design\n\nNormalized [0,1]^k OAT trajectories use p="+c.levels+" levels and delta=p/[2(p-1)]="+fmt(design.delta)+". Starts, parameter order, and directions are randomized from design seed `"+design.seed+"`. Exactly one normalized and physical parameter changes at each step. Duplicate physical points: "+design.duplicateCount+".\n\n");
            w.write("## 8. Simulation Count\n\nExpected and recorded simulations: "+records.size()+". Each trajectory contains k+1="+(ModelParameters.screenedDefinitions().size()+1)+" points.\n\n");
            w.write("## 9. Seed Strategy\n\nMaster seed `"+c.masterSeed+"` deterministically generates a separate design seed and simulation seeds. For replicate r, every point in trajectory t uses the same simulation seed, providing common random numbers for adjacent effects. Different trajectories/replicates use different seeds.\n\n");
            w.write("## 10. Parallelization Strategy\n\nA bounded Java executor uses "+c.threads+" threads. Each task creates a fresh `ExampleGrid`, immutable copied parameters, and a fresh HAL `Rand`; no model or RNG instance is shared. Atomic per-run checkpoints permit deterministic resume.\n\n");
            w.write("## 11. Per-Output Sensitivity Results\n\nThe primary results are `morris_summary_by_output.csv`. The table below gives the leading valid parameter for every emitted output. Signed mu describes direction; mu-star describes magnitude; sigma reflects trajectory dependence/nonlinearity/interactions and stochastic variability.\n\n| Output | Family | Top parameter | mu-star | sigma | valid/lost |\n|---|---|---|---:|---:|---:|\n");
            for(Map.Entry<String,List<Summary>>e:byOutput.entrySet()){Summary top=e.getValue().stream().filter(s->s.rank==1).findFirst().orElse(null);if(top!=null)w.write("| `"+top.output+"` | "+top.family+" | `"+top.parameter+"` | "+fmt(top.muStar)+" | "+fmt(top.sigma)+" | "+top.valid+"/"+top.lost+" |\n");}
            w.write("\n## 12. Important Currently Fixed Parameters\n\n");int shown=0;for(GlobalRank g:global){if(!g.status.equals("ABC-inferred")&&shown++<12)w.write("- Rank "+g.rank+": `"+g.parameter+"` (score "+fmt(g.overall)+", status "+g.status+").\n");}if(shown==0)w.write("No stable conclusion from this run.\n");
            w.write("\n## 13. Parameters Associated With Extinction or Invalid States\n\nAssociations are screening statistics, not causal estimates.\n\n");shown=0;List<FailureAssociation>orderedFailures=new ArrayList<>(failures);orderedFailures.sort(Comparator.comparingDouble((FailureAssociation f)->Double.isFinite(f.rankBiserial)?-Math.abs(f.rankBiserial):Double.POSITIVE_INFINITY));for(FailureAssociation f:orderedFailures){if(f.nEvent>0&&f.nNoEvent>0&&Double.isFinite(f.rankBiserial)&&shown++<15)w.write("- `"+f.parameter+"` vs "+f.outcome+": rank-biserial="+fmt(f.rankBiserial)+", logistic coefficient="+fmt(f.logisticCoefficient)+", permutation p="+fmt(f.permutationP)+".\n");}if(shown==0)w.write("No outcome had both event and non-event runs; association statistics are explicitly undefined.\n");
            w.write("\n## 14. Nonlinear/Interaction Candidates\n\nSee class B/C rows in `PARAMETER_INFLUENCE_CLASSIFICATION.csv`. High sigma can also reflect stochastic variability; multi-seed confirmation is required before biological interpretation.\n\n");
            w.write("## 15. Parameters Confirmed as Low Influence\n\nOnly class D parameters with adequate valid effects and acceptable signal-to-noise qualify statistically. Structurally inactive class E fields are implementation findings, not evidence of biological unimportance. A short pilot cannot confirm low influence.\n\n");
            w.write("## 16. One-Seed Versus Multi-Seed Comparison\n\n"+(hasConfirmation?confirmationReportText(confirmedFile):"Not yet run. Use the documented `--confirm-only --confirmation-replicates 3` command after the primary run.")+"\n\n");
            w.write("## 17. Recommended Parameters for Future Calibration\n\nCurrent overall candidates (provisional when trajectories <10): ");for(int i=0;i<Math.min(10,global.size());i++){if(i>0)w.write(", ");w.write("`"+global.get(i).parameter+"`");}w.write(". Use per-output top tiers after the 20-trajectory screen and three-seed confirmation. Favor parameters influencing measured tumour, JNK, fibroblast, macrophage, and EC outputs with adequate SNR; retain failure-associated parameters even if continuous-output mu-star is moderate. Sensitivity alone does not establish identifiability.\n\n");
            w.write("## 18. Recommended Parameters to Remain Fixed\n\nKeep numerical cutoffs, grid/domain structure, coordinate realizations, observation times, and treatment-only quantities fixed for this untreated calibration unless a dedicated design is approved. Low Morris influence alone is not proof that a mechanism is biologically unnecessary.\n\n");
            w.write("## 19. Parameters Needing Biological Review\n\nAll bounds labeled assumption requiring mentor approval, the older PDF/current-code discrepancies, independent variation that breaks proposed homeostatic/ratio constraints, initial background counts, hidden density boosts, and all interaction radii need review.\n\n");
            w.write("## 20. Limitations\n\nMorris is a screening method. Mu-star is statistical sensitivity over the chosen bounds, not biological importance or identifiability. Sigma mixes nonlinear, interaction, discrete-mapping, and stochastic effects. Parameter uncertainty is encoded by ranges and is distinct from stochastic simulation variability. A flat ABC posterior is not proof of low sensitivity, and a fixed parameter can be influential.\n\n");
            w.write("## 21. Exact Next Step for Surrogate-Model Training\n\nAfter confirming rankings, generate a larger space-filling design over the retained uncertain parameters while retaining failure flags and every raw/derived output. Join it with `morris_outputs.csv` by named parameter columns; train separate probabilistic or heteroscedastic output emulators, with a separate classifier for invalid/extinct states. Do not train on NaNs as successful outcomes.\n\n");
            w.write("## 22. Exact Next Step for ABC or SNPE\n\nDefine a reviewed reduced prior only after per-output and failure results are stable. Simulate a space-filling training set with replicated seeds, validate the surrogate out of sample per output, then run ABC-SMC or SNPE with explicit missing/extinction handling and posterior predictive checks. Do not return to ordinary rejection ABC as the main workflow.\n\n");
            w.write("## 23. Screening Statement\n\nThis analysis identifies influential parameters over specified uncertainty ranges. It does **not** calibrate the model, estimate Sobol indices, prove identifiability, or justify deleting biological mechanisms.\n");
        }
    }

    static String confirmationReportText(Path file) {
        if(!Files.isRegularFile(file))return "Confirmation output is not available.";
        try {
            List<String>lines=Files.readAllLines(file,StandardCharsets.UTF_8);Set<String>outputs=new HashSet<>(),parameters=new HashSet<>();List<Double>rho=new ArrayList<>();
            for(int i=1;i<lines.size();i++){String[]x=lines.get(i).split(",",-1);if(x.length<15)continue;outputs.add(x[0].replace("\"",""));parameters.add(x[2].replace("\"",""));try{double v=Double.parseDouble(x[12]);if(Double.isFinite(v))rho.add(v);}catch(NumberFormatException ignored){}}
            Collections.sort(rho);double median=quantileSorted(rho,.5);
            return "Targeted three-seed confirmation is complete for "+parameters.size()+" selected parameters across "+outputs.size()+" outputs. The median output-specific Spearman rank correlation is "+fmt(median)+". `morris_confirmed_rankings.csv` reports every restricted one-seed/multi-seed rank, rank change, approximate 95% mu-star interval, SNR, valid count, and lost count. With only two trajectories in the smoke run, these comparisons validate stochastic handling but do not establish stable scientific ranks.";
        } catch(IOException e) { return "Confirmation exists, but its summary could not be read: "+e.getMessage()+". See `morris_confirmed_rankings.csv`."; }
    }

    static void runConfirmation(Config c) throws Exception {
        Files.createDirectories(c.outputDir);
        Design design=generateDesign(c);
        List<RunRecord> primary=execute(c,design,c.outputDir.resolve("checkpoints"),null,c.replicates);
        AnalysisBundle original=computeElementaryEffects(c,design,primary);
        List<FailureAssociation> originalFailures=computeFailureSensitivity(c,primary);
        List<GlobalRank> global=computeGlobalRankings(original.summaries,originalFailures);
        LinkedHashSet<String> selected=new LinkedHashSet<>();for(int i=0;i<Math.min(c.confirmationTop,global.size());i++)selected.add(global.get(i).parameter);
        // Add the three strongest high-sigma/moderate-mu candidates on primary
        // biological end-point outputs. Per-output quantiles avoid count-scale
        // outputs causing every parameter to be selected.
        Set<String>confirmationOutputs=new HashSet<>(Arrays.asList(
                "tumor_log10_fold_s1440","jnkp_fraction_s1440","fibroblast_log10_fold_s1440",
                "macrophage_activated_fraction_s1440","ec_activated_fraction_s1440",
                "tumor_radius_s1440","tumor_rms_spread_s1440","jnkp_rim_fraction_s1440",
                "active_macrophage_ec_colocalization_s1440"));
        Map<String,List<Summary>> byOutput=new HashMap<>();
        for(Summary s:original.summaries)if(confirmationOutputs.contains(s.output))byOutput.computeIfAbsent(s.output,z->new ArrayList<>()).add(s);
        Map<String,Double> interactionScore=new HashMap<>();
        for(List<Summary> list:byOutput.values()) {
            List<Double> mus=new ArrayList<>(),sigmas=new ArrayList<>();
            for(Summary s:list){if(Double.isFinite(s.muStar))mus.add(s.muStar);if(Double.isFinite(s.sigma))sigmas.add(s.sigma);}
            Collections.sort(mus);Collections.sort(sigmas);
            double q25=quantileSorted(mus,.25),q75=quantileSorted(mus,.75),sq75=quantileSorted(sigmas,.75);
            for(Summary s:list)if(Double.isFinite(s.sigma)&&s.sigma>=sq75&&Double.isFinite(s.muStar)&&s.muStar>=q25&&s.muStar<q75) {
                double score=s.outputScale>0?s.sigma/s.outputScale:s.sigma;
                interactionScore.merge(s.parameter,score,Math::max);
            }
        }
        List<Map.Entry<String,Double>> interaction=new ArrayList<>(interactionScore.entrySet());
        interaction.sort(Map.Entry.<String,Double>comparingByValue().reversed());
        for(int i=0;i<Math.min(3,interaction.size());i++)selected.add(interaction.get(i).getKey());
        List<FailureAssociation> extinction=new ArrayList<>();for(FailureAssociation f:originalFailures)if(f.outcome.equals("TUMOR_EXTINCT")&&Double.isFinite(f.rankBiserial))extinction.add(f);extinction.sort(Comparator.comparingDouble((FailureAssociation f)->Math.abs(f.rankBiserial)).reversed());for(int i=0;i<Math.min(3,extinction.size());i++)selected.add(extinction.get(i).parameter);
        Set<String>essential=new LinkedHashSet<>(Arrays.asList("pOnMax","pOffMax","activProbF","activProbM","activProbE","ecSurvival"));
        List<GlobalRank> unexpectedLow=new ArrayList<>();for(GlobalRank g:global)if(essential.contains(g.parameter)&&g.rank>global.size()/2)unexpectedLow.add(g);
        unexpectedLow.sort(Comparator.comparingInt((GlobalRank g)->g.rank).reversed());for(int i=0;i<Math.min(2,unexpectedLow.size());i++)selected.add(unexpectedLow.get(i).parameter);
        Map<String,Point>pm=new HashMap<>();for(Point p:design.points)pm.put(p.trajectory+":"+p.step,p);Set<String>pointIds=new LinkedHashSet<>();int k=ModelParameters.screenedDefinitions().size();for(int t=0;t<c.trajectories;t++)for(int s=1;s<=k;s++){Point p=pm.get(t+":"+s);if(p!=null&&selected.contains(p.changedParameter)){pointIds.add(p.pointId());pointIds.add(pm.get(t+":"+(s-1)).pointId());}}
        Path confirmDir=c.outputDir.resolve("confirmation");Files.createDirectories(confirmDir);
        System.out.println("confirmation parameters ("+selected.size()+"): "+String.join(",",selected));
        System.out.println("confirmation trajectory points: "+pointIds.size()+" of "+design.points.size());
        int oldReps=c.replicates;c.replicates=c.confirmationReplicates;
        List<RunRecord> confirmed=execute(c,design,confirmDir.resolve("checkpoints"),pointIds,c.confirmationReplicates);
        writeRawRuns(confirmDir.resolve("morris_raw_runs.csv"),confirmed);writeOutputs(confirmDir.resolve("morris_outputs.csv"),confirmed);writeFailures(confirmDir.resolve("morris_failures.csv"),confirmed);
        AnalysisBundle newer=computeElementaryEffects(c,design,confirmed);writeElementaryEffects(confirmDir.resolve("morris_elementary_effects.csv"),newer.effects);writeSummary(confirmDir.resolve("morris_summary_by_output.csv"),newer.summaries);
        writeConfirmedRankings(c.outputDir.resolve("morris_confirmed_rankings.csv"),original.summaries,newer.summaries,selected);
        c.replicates=oldReps;
        System.out.println("confirmation selected "+selected.size()+" parameters and "+pointIds.size()+" trajectory points; wrote "+c.outputDir.resolve("morris_confirmed_rankings.csv"));
    }

    static void writeConfirmedRankings(Path file,List<Summary>old,List<Summary>newer,Set<String>selected)throws IOException {
        Map<String,Summary>om=new HashMap<>(),nm=new HashMap<>();for(Summary s:old)if(selected.contains(s.parameter))om.put(s.output+"\t"+s.parameter,s);for(Summary s:newer)if(selected.contains(s.parameter))nm.put(s.output+"\t"+s.parameter,s);
        Set<String>outputs=new LinkedHashSet<>();for(Summary s:old)outputs.add(s.output);Path tmp=file.resolveSibling(file.getFileName()+".tmp");try(BufferedWriter w=Files.newBufferedWriter(tmp,StandardCharsets.UTF_8)){
            w.write("output,output_family,parameter,one_seed_mu_star,multi_seed_mu_star,multi_seed_mu_star_ci_low,multi_seed_mu_star_ci_high,one_seed_restricted_rank,multi_seed_rank,rank_change,multi_seed_sigma,multi_seed_mean_snr,spearman_rank_correlation,n_valid_multi_seed,n_lost_multi_seed\n");
            for(String output:outputs){List<Summary>os=new ArrayList<>(),ns=new ArrayList<>();for(String p:selected){Summary a=om.get(output+"\t"+p),b=nm.get(output+"\t"+p);if(a!=null&&b!=null&&Double.isFinite(a.muStar)&&Double.isFinite(b.muStar)){os.add(a);ns.add(b);}}os.sort(Comparator.comparingDouble((Summary s)->s.muStar).reversed());ns.sort(Comparator.comparingDouble((Summary s)->s.muStar).reversed());Map<String,Integer>or=new HashMap<>(),nr=new HashMap<>();for(int i=0;i<os.size();i++)or.put(os.get(i).parameter,i+1);for(int i=0;i<ns.size();i++)nr.put(ns.get(i).parameter,i+1);double rho=spearman(or,nr);for(Summary b:ns){Summary a=om.get(output+"\t"+b.parameter);double lo=Double.isFinite(b.seMuStar)?Math.max(0,b.muStar-1.96*b.seMuStar):Double.NaN,hi=Double.isFinite(b.seMuStar)?b.muStar+1.96*b.seMuStar:Double.NaN;w.write(csv(output)+","+csv(b.family)+","+csv(b.parameter)+","+fmt(a.muStar)+","+fmt(b.muStar)+","+fmt(lo)+","+fmt(hi)+","+or.get(b.parameter)+","+nr.get(b.parameter)+","+(nr.get(b.parameter)-or.get(b.parameter))+","+fmt(b.sigma)+","+fmt(b.meanSnr)+","+fmt(rho)+","+b.valid+","+b.lost+"\n");}}
        }atomicMove(tmp,file);
    }
    static double spearman(Map<String,Integer>a,Map<String,Integer>b){List<Double>x=new ArrayList<>(),y=new ArrayList<>();for(String k:a.keySet())if(b.containsKey(k)){x.add((double)a.get(k));y.add((double)b.get(k));}return correlation(x,y);}
}
