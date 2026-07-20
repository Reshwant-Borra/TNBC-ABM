package OnLatticeExample;

import HAL.Rand;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Baseline, deterministic-rerun, registry, and independent-grid checks. */
public final class MorrisQualityControl {
    private static int[][] runNamed(ModelParameters p, long seed, int maxStep) throws Exception {
        ExampleGrid g = new ExampleGrid(100,100);
        g.rng = new Rand(seed);
        return g.RunHeadless(p,maxStep);
    }

    private static void require(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        long seed=args.length>0 ? Long.parseLong(args[0]) : 9001L;
        int initPop=args.length>1 ? Integer.parseInt(args[1]) : 25;
        int maxStep=args.length>2 ? Integer.parseInt(args[2]) : 1440;
        ModelParameters p=ModelParameters.currentBaseline(initPop);

        require(Files.isRegularFile(Path.of("QuadratEndothelialOn.txt")), "missing QuadratEndothelialOn.txt");
        require(Files.isRegularFile(Path.of("QuadratStrOn.txt")), "missing QuadratStrOn.txt");
        require(ModelParameters.registry().stream().map(x->x.name).distinct().count()==ModelParameters.registry().size(),
                "registry contains duplicate names");
        require(ModelParameters.screenedDefinitions().size()>0,"screened registry is empty");
        for (ModelParameters.Definition d : ModelParameters.screenedDefinitions()) {
            require(d.lower<=d.baseline && d.baseline<=d.upper,d.name+" baseline outside bounds");
            require(Double.isFinite(d.fromNormalized(0.0)) && Double.isFinite(d.fromNormalized(1.0)),d.name+" invalid transform endpoints");
        }

        ExampleGrid legacy=new ExampleGrid(100,100);
        legacy.rng=new Rand(seed);
        int[][] oldSnaps=legacy.RunHeadless(p.legacyTheta(),initPop,maxStep);

        ExampleGrid named=new ExampleGrid(100,100);
        named.rng=new Rand(seed);
        int[][] newSnaps=named.RunHeadless(p,maxStep);
        require(Arrays.deepEquals(oldSnaps,newSnaps),"baseline mismatch: legacy and named snapshots differ");
        require(Arrays.deepEquals(legacy.lastRimCore,named.lastRimCore),"baseline mismatch: rim/core diagnostics differ");
        require(Arrays.deepEquals(legacy.lastEventCounts,named.lastEventCounts),"baseline mismatch: event counts differ");
        require(Arrays.equals(legacy.lastSnapshotSteps,named.lastSnapshotSteps),"baseline mismatch: snapshot steps differ");
        int expected=1;
        for (int s : new int[]{480,960,1440,2100}) if (s<=maxStep) expected++;
        require(newSnaps.length==expected,"snapshot shape mismatch: expected "+expected+" got "+newSnaps.length);
        for (int[] row:newSnaps) require(row.length==8,"snapshot row width is not eight");

        ExecutorService pool=Executors.newFixedThreadPool(2);
        try {
            Callable<int[][]> job=()->runNamed(p.copy(),seed,maxStep);
            List<Future<int[][]>> f=pool.invokeAll(Arrays.asList(job,job));
            int[][] a=f.get(0).get(), b=f.get(1).get();
            require(Arrays.deepEquals(a,b),"parallel fresh-grid deterministic reruns differ");
            require(Arrays.deepEquals(a,newSnaps),"parallel result differs from sequential result; shared mutable state suspected");
        } finally {
            pool.shutdownNow();
        }

        System.out.println("PASS baseline regression: legacy vector == named parameters");
        System.out.println("PASS deterministic rerun and two-thread fresh-grid independence");
        System.out.println("PASS registry uniqueness/transforms/bounds and snapshot shape");
        System.out.println("seed="+seed+" initPop="+initPop+" maxStep="+maxStep+" snapshots="+newSnaps.length+
                " screened="+ModelParameters.screenedDefinitions().size()+" audited="+ModelParameters.registry().size());
    }
}
