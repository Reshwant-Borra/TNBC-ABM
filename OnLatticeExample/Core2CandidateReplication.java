package OnLatticeExample;

import HAL.Rand;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Permanent replication/diagnostic harness for the frozen core2 ABC candidates. */
public final class Core2CandidateReplication {
    static final String PROFILE_NAME = "core2";
    static final int DEFAULT_REPLICATES = 10;
    static final long DEFAULT_MASTER_SEED = 2026072302L;
    static final int MAX_STEP = 1440;
    static final int INIT_POP = 25;
    static final double GOOD = 3.0;
    static final double CANDIDATE = 4.0;
    static final double STEPS_PER_DAY = 480.0 / 7.0;

    static final class Config {
        Path input = Path.of("results", "abc-core2-diagnostic-100", "abc_accepted.csv");
        Path output = Path.of("outputs", "core2_replication");
        int replicates = DEFAULT_REPLICATES;
        long masterSeed = DEFAULT_MASTER_SEED;
        int threads = Math.min(Runtime.getRuntime().availableProcessors(), 8);
        boolean resume = false;

        static Config parse(String[] args) {
            Config c = new Config();
            for (int i = 0; i < args.length; i++) {
                String a = args[i];
                if ("--resume".equals(a)) { c.resume = true; continue; }
                if (i + 1 >= args.length) throw new IllegalArgumentException("missing value after " + a);
                String v = args[++i];
                switch (a) {
                    case "--input": c.input = Path.of(v); break;
                    case "--output": c.output = Path.of(v); break;
                    case "--replicates": c.replicates = Integer.parseInt(v); break;
                    case "--master-seed": c.masterSeed = Long.parseLong(v); break;
                    case "--threads": c.threads = Integer.parseInt(v); break;
                    default: throw new IllegalArgumentException("unknown option: " + a);
                }
            }
            if (c.replicates < 1) throw new IllegalArgumentException("--replicates must be positive");
            if (c.threads < 1) throw new IllegalArgumentException("--threads must be positive");
            c.threads = Math.min(c.threads, c.replicates * 13);
            return c;
        }
    }

    static final class Candidate {
        final int candidateId;
        final double divProbP;
        final double pOffMax;
        final String sourceRow;
        Candidate(int candidateId, double divProbP, double pOffMax, String sourceRow) {
            this.candidateId = candidateId;
            this.divProbP = divProbP;
            this.pOffMax = pOffMax;
            this.sourceRow = sourceRow;
        }
    }

    static final class Frame {
        final int candidateId, replicateIndex, step;
        final long seed;
        final int[] counts, events;
        Frame(int candidateId, int replicateIndex, long seed, int step, int[] counts, int[] events) {
            this.candidateId = candidateId;
            this.replicateIndex = replicateIndex;
            this.seed = seed;
            this.step = step;
            this.counts = counts.clone();
            this.events = events.clone();
        }
    }

    static final class LossEvent {
        int candidateId, replicateIndex, lossStep;
        long seed;
        String compartment;
        double biologicalTime;
        int priorPopulation, divisionsBeforeLoss, deathsBeforeLoss, displacementBeforeLoss;
        int tumorAtLoss, ecAtLoss, fibroAtLoss, macrophageAtLoss;
        double divProbP, pOffMax;
    }

    static final class SimResult {
        int candidateId, replicateIndex, simulationStartStep = 0, simulationEndStep = -1;
        long seed;
        double divProbP, pOffMax, distance = Double.POSITIVE_INFINITY, runtimeSeconds;
        String runStatus = "MODEL_EXCEPTION", invalidReason = "", exceptionClass = "", exceptionMessage = "";
        String acceptanceClass = "INVALID", firstLostCompartment = "";
        boolean biologicallyValid;
        int firstRequiredCompartmentLossStep = -1, tumorExtinctionStep = -1;
        int ecLossStep = -1, fibroblastLossStep = -1, macrophageLossStep = -1;
        double firstRequiredCompartmentLossTime = Double.NaN;
        CalibrationTarget.DistanceResult distanceResult;
        ArrayList<Frame> frames = new ArrayList<>();
        ArrayList<LossEvent> lossEvents = new ArrayList<>();
        String existingRow;

        String key() { return key(candidateId, replicateIndex, seed); }
        static String key(int candidateId, int replicateIndex, long seed) {
            return candidateId + ":" + replicateIndex + ":" + seed;
        }
    }

    public static void main(String[] args) throws Exception {
        Config c = Config.parse(args);
        Files.createDirectories(c.output);
        CalibrationTarget.validateTargets();
        CalibrationFreeze.verifyFreeze(CalibrationFreeze.DEFAULT_DIR);
        CalibrationProfile profile = CalibrationProfile.core2();
        profile.validate();
        List<Candidate> candidates = loadCandidates(c.input);
        if (candidates.size() != 13) throw new IllegalStateException("expected exactly 13 accepted candidates, got " + candidates.size());
        long[] seeds = replicateSeeds(c.masterSeed, c.replicates);
        String profileHash = CalibrationFreeze.sha256(profile.canonicalCsv().getBytes(StandardCharsets.UTF_8));
        String inputHash = CalibrationFreeze.fileSha(c.input);

        Map<String, SimResult> existing = c.resume ? readExistingResults(c.output.resolve("core2_candidate_replications.csv"), profileHash) : Collections.emptyMap();
        List<Callable<SimResult>> tasks = new ArrayList<>();
        for (Candidate cand : candidates) {
            for (int r = 0; r < c.replicates; r++) {
                long seed = seeds[r];
                if (existing.containsKey(SimResult.key(cand.candidateId, r, seed))) continue;
                final int rep = r;
                tasks.add(() -> runOne(profile, cand, rep, seed));
            }
        }

        long t0 = System.nanoTime();
        ArrayList<SimResult> results = new ArrayList<>(existing.values());
        ExecutorService exec = Executors.newFixedThreadPool(c.threads);
        try {
            ArrayList<Future<SimResult>> futures = new ArrayList<>();
            for (Callable<SimResult> task : tasks) futures.add(exec.submit(task));
            int done = 0;
            for (Future<SimResult> f : futures) {
                results.add(f.get());
                done++;
                if (done % 10 == 0 || done == futures.size()) {
                    System.out.printf(Locale.US, "completed %d/%d submitted simulations%n", done, futures.size());
                }
            }
        } finally {
            exec.shutdownNow();
        }
        results.sort(resultComparator());
        double runtimeSeconds = (System.nanoTime() - t0) / 1.0e9;
        writeOutputs(c, profileHash, inputHash, seeds, candidates, results, runtimeSeconds);
        System.out.printf(Locale.US, "core2 replication complete: %d simulations, %.1f s, output=%s%n",
                results.size(), runtimeSeconds, c.output);
    }

    static SimResult runOne(CalibrationProfile profile, Candidate cand, int replicateIndex, long seed) {
        SimResult out = new SimResult();
        out.candidateId = cand.candidateId;
        out.replicateIndex = replicateIndex;
        out.seed = seed;
        out.divProbP = cand.divProbP;
        out.pOffMax = cand.pOffMax;
        long t0 = System.nanoTime();
        try {
            ModelParameters p = profile.applyValues(INIT_POP, new double[]{cand.divProbP, cand.pOffMax});
            ExampleGrid model = new ExampleGrid(100, 100);
            model.rng = new Rand(seed);
            ExampleGrid.DiagnosticRun run = model.RunHeadlessDiagnostic(p, MAX_STEP, 1);
            out.simulationEndStep = lastStep(run.frames);
            for (ExampleGrid.DiagnosticFrame f : run.frames) {
                out.frames.add(new Frame(cand.candidateId, replicateIndex, seed, f.step, f.counts, f.cumulativeEvents));
            }
            if (run.snapshots.length == 0 || run.snapshots[0].length < 8 ||
                    run.snapshots[0][0] + run.snapshots[0][1] != INIT_POP) {
                out.runStatus = "INITIALIZATION_FAILURE";
                out.invalidReason = "initial tumour count did not equal initPop";
                out.distanceResult = new CalibrationTarget.DistanceResult(Double.POSITIVE_INFINITY,
                        CalibrationTarget.perTarget(ABCRejection.safeSnapshots(run.snapshots)), "INITIALIZATION_FAILURE");
            } else {
                out.distanceResult = CalibrationTarget.distance(run.snapshots);
                classify(out);
            }
            detectLosses(out, cand);
            if (out.biologicallyValid) out.distance = out.distanceResult.distance;
            classifyAcceptance(out);
        } catch (Throwable e) {
            out.runStatus = "MODEL_EXCEPTION";
            out.invalidReason = e.getMessage() == null ? e.toString() : e.getMessage();
            out.exceptionClass = e.getClass().getName();
            out.exceptionMessage = out.invalidReason;
            out.distanceResult = new CalibrationTarget.DistanceResult(Double.POSITIVE_INFINITY, Collections.emptyList(), out.runStatus);
            out.distance = Double.POSITIVE_INFINITY;
            out.acceptanceClass = "INVALID";
        } finally {
            out.runtimeSeconds = (System.nanoTime() - t0) / 1.0e9;
        }
        return out;
    }

    static void classify(SimResult r) {
        ABCRejection.DrawResult d = new ABCRejection.DrawResult();
        d.distance = r.distanceResult;
        ABCRejection.classify(d);
        r.runStatus = d.outcomeStatus;
        r.invalidReason = d.failureReason;
        r.biologicallyValid = "VALID_FINITE".equals(r.runStatus) && r.distanceResult != null && Double.isFinite(r.distanceResult.distance);
    }

    static void classifyAcceptance(SimResult r) {
        if (!r.biologicallyValid || !Double.isFinite(r.distance)) {
            r.acceptanceClass = "INVALID";
        } else if (r.distance <= GOOD) {
            r.acceptanceClass = "GOOD";
        } else if (r.distance <= CANDIDATE) {
            r.acceptanceClass = "BORDERLINE";
        } else {
            r.acceptanceClass = "POOR_VALID";
        }
    }

    static void detectLosses(SimResult r, Candidate cand) {
        HashMap<String, Frame> previous = new HashMap<>();
        for (Frame f : r.frames) {
            setLoss(r, cand, f, previous.get("tumor"), "tumor", totalTumor(f), r.tumorExtinctionStep);
            setLoss(r, cand, f, previous.get("EC"), "EC", totalEc(f), r.ecLossStep);
            setLoss(r, cand, f, previous.get("fibroblast"), "fibroblast", totalFibro(f), r.fibroblastLossStep);
            setLoss(r, cand, f, previous.get("macrophage"), "macrophage", totalMac(f), r.macrophageLossStep);
            previous.put("tumor", f);
            previous.put("EC", f);
            previous.put("fibroblast", f);
            previous.put("macrophage", f);
        }
        ArrayList<LossEvent> losses = new ArrayList<>(r.lossEvents);
        losses.sort(Comparator.comparingInt((LossEvent e) -> e.lossStep).thenComparing(e -> e.compartment));
        if (!losses.isEmpty()) {
            LossEvent first = losses.get(0);
            r.firstRequiredCompartmentLossStep = first.lossStep;
            r.firstRequiredCompartmentLossTime = first.biologicalTime;
            r.firstLostCompartment = first.compartment;
        }
    }

    static void setLoss(SimResult r, Candidate cand, Frame f, Frame prev, String compartment, int total, int currentLossStep) {
        if (total != 0 || currentLossStep >= 0) return;
        if ("tumor".equals(compartment)) r.tumorExtinctionStep = f.step;
        else if ("EC".equals(compartment)) r.ecLossStep = f.step;
        else if ("fibroblast".equals(compartment)) r.fibroblastLossStep = f.step;
        else if ("macrophage".equals(compartment)) r.macrophageLossStep = f.step;
        LossEvent e = new LossEvent();
        e.candidateId = r.candidateId;
        e.replicateIndex = r.replicateIndex;
        e.seed = r.seed;
        e.compartment = compartment;
        e.lossStep = f.step;
        e.biologicalTime = days(f.step);
        e.priorPopulation = prev == null ? -1 : compartmentTotal(prev, compartment);
        e.divisionsBeforeLoss = prev == null ? 0 : compartmentDivisions(prev, compartment);
        e.deathsBeforeLoss = prev == null ? 0 : compartmentDeaths(prev, compartment);
        e.displacementBeforeLoss = 0;
        e.tumorAtLoss = totalTumor(f);
        e.ecAtLoss = totalEc(f);
        e.fibroAtLoss = totalFibro(f);
        e.macrophageAtLoss = totalMac(f);
        e.divProbP = cand.divProbP;
        e.pOffMax = cand.pOffMax;
        r.lossEvents.add(e);
    }

    static List<Candidate> loadCandidates(Path input) throws IOException {
        ArrayList<Candidate> out = new ArrayList<>();
        try (BufferedReader r = Files.newBufferedReader(input, StandardCharsets.UTF_8)) {
            String header = r.readLine();
            if (header == null) throw new IOException("empty candidate CSV: " + input);
            List<String> h = CalibrationFreeze.parseCsv(header);
            int draw = h.indexOf("draw_id"), profile = h.indexOf("profile"), div = h.indexOf("divProbP"),
                    off = h.indexOf("pOffMax"), accepted = h.indexOf("accepted");
            require(draw >= 0 && profile >= 0 && div >= 0 && off >= 0 && accepted >= 0, "candidate CSV missing required columns");
            String line;
            while ((line = r.readLine()) != null) {
                List<String> row = CalibrationFreeze.parseCsv(line);
                if (!"true".equalsIgnoreCase(row.get(accepted))) continue;
                if (!PROFILE_NAME.equals(row.get(profile))) throw new IllegalStateException("non-core2 accepted row: " + line);
                out.add(new Candidate(Integer.parseInt(row.get(draw)), Double.parseDouble(row.get(div)), Double.parseDouble(row.get(off)), line));
            }
        }
        out.sort(Comparator.comparingInt(c -> c.candidateId));
        return out;
    }

    static long[] replicateSeeds(long master, int n) {
        long[] out = new long[n];
        for (int i = 0; i < n; i++) {
            out[i] = MorrisSensitivitySweep.mix64(master ^ 0x434f524532524550L ^ (0x9e3779b97f4a7c15L * (i + 1L)));
        }
        return out;
    }

    static int taskCount(List<Candidate> candidates, int replicates) {
        return candidates.size() * replicates;
    }

    static Comparator<SimResult> resultComparator() {
        return Comparator.comparingInt((SimResult r) -> r.candidateId)
                .thenComparingInt(r -> r.replicateIndex)
                .thenComparingLong(r -> r.seed);
    }

    static void writeOutputs(Config c, String profileHash, String inputHash, long[] seeds, List<Candidate> candidates,
                             List<SimResult> results, double runtimeSeconds) throws Exception {
        writeReplicationCsv(c.output.resolve("core2_candidate_replications.csv"), profileHash, results);
        writeTimeseries(c.output.resolve("core2_compartment_timeseries.csv"), results);
        writeLossEvents(c.output.resolve("core2_compartment_loss_events.csv"), results);
        List<CandidateSummary> summaries = summarizeCandidates(candidates, results);
        writeSummary(c.output.resolve("core2_candidate_robustness_summary.csv"), summaries);
        writeRobust(c.output.resolve("core2_robust_candidates.csv"), summaries);
        writeManifest(c.output.resolve("core2_replication_runtime_manifest.json"), c, profileHash, inputHash, seeds, runtimeSeconds);
    }

    static void writeReplicationCsv(Path file, String profileHash, List<SimResult> results) throws Exception {
        try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            w.write(replicationHeader());
            w.newLine();
            for (SimResult r : results) {
                if (r.existingRow != null) {
                    w.write(r.existingRow); w.newLine();
                } else {
                    w.write(replicationRow(r, profileHash)); w.newLine();
                }
            }
        }
    }

    static String replicationHeader() {
        StringBuilder h = new StringBuilder("candidate_id,replicate_index,seed,profile_hash,divProbP,pOffMax,run_status,biologically_valid,distance,acceptance_class,invalid_reason,exception_class,exception_message,simulation_start_step,simulation_end_step,first_required_compartment_loss_step,first_required_compartment_loss_time,first_lost_compartment,tumor_extinction_step,EC_loss_step,fibroblast_loss_step,macrophage_loss_step,runtime_seconds");
        for (CalibrationTarget t : CalibrationTarget.currentTargets()) {
            h.append(',').append(t.id).append("_sim")
                    .append(',').append(t.id).append("_target")
                    .append(',').append(t.id).append("_scale")
                    .append(',').append(t.id).append("_weight")
                    .append(',').append(t.id).append("_normalized_residual")
                    .append(',').append(t.id).append("_distance_contribution");
        }
        return h.toString();
    }

    static String replicationRow(SimResult r, String profileHash) {
        StringBuilder b = new StringBuilder();
        b.append(r.candidateId).append(',').append(r.replicateIndex).append(',').append(r.seed).append(',').append(csv(profileHash))
                .append(',').append(fmt(r.divProbP)).append(',').append(fmt(r.pOffMax)).append(',')
                .append(csv(r.runStatus)).append(',').append(r.biologicallyValid).append(',').append(fmt(r.distance)).append(',')
                .append(csv(r.acceptanceClass)).append(',').append(csv(r.invalidReason)).append(',').append(csv(r.exceptionClass)).append(',')
                .append(csv(r.exceptionMessage)).append(',').append(r.simulationStartStep).append(',').append(r.simulationEndStep).append(',')
                .append(r.firstRequiredCompartmentLossStep).append(',').append(fmt(r.firstRequiredCompartmentLossTime)).append(',')
                .append(csv(r.firstLostCompartment)).append(',').append(r.tumorExtinctionStep).append(',').append(r.ecLossStep).append(',')
                .append(r.fibroblastLossStep).append(',').append(r.macrophageLossStep).append(',').append(fmt(r.runtimeSeconds));
        Map<String, CalibrationTarget.TargetResult> byId = new HashMap<>();
        if (r.distanceResult != null) for (CalibrationTarget.TargetResult tr : r.distanceResult.targets) byId.put(tr.target.id, tr);
        for (CalibrationTarget t : CalibrationTarget.currentTargets()) {
            CalibrationTarget.TargetResult tr = byId.get(t.id);
            b.append(',').append(fmt(tr == null ? Double.NaN : tr.simulated))
                    .append(',').append(fmt(t.observedValue))
                    .append(',').append(fmt(t.scale))
                    .append(',').append(fmt(t.weight))
                    .append(',').append(fmt(tr == null ? Double.NaN : tr.standardizedResidual))
                    .append(',').append(fmt(tr == null ? Double.NaN : tr.contribution));
        }
        return b.toString();
    }

    static void writeTimeseries(Path file, List<SimResult> results) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            w.write("candidate_id,replicate_index,seed,step,biological_time,tumor_total,tumor_jnk_positive,tumor_jnk_negative,EC_total,EC_activated,EC_non_activated,fibroblast_total,fibroblast_activated,fibroblast_non_activated,macrophage_total,macrophage_activated,macrophage_non_activated,cumulative_EC_divisions,cumulative_EC_deaths,cumulative_fibroblast_divisions,cumulative_fibroblast_deaths,cumulative_tumor_divisions,cumulative_tumor_deaths,cumulative_macrophage_divisions,cumulative_macrophage_deaths,cumulative_displacement_events,cumulative_failed_divisions,compartment_loss_flag\n");
            ArrayList<Frame> frames = new ArrayList<>();
            for (SimResult r : results) frames.addAll(r.frames);
            frames.sort(Comparator.comparingInt((Frame f) -> f.candidateId).thenComparingInt(f -> f.replicateIndex).thenComparingLong(f -> f.seed).thenComparingInt(f -> f.step));
            for (Frame f : frames) {
                int[] c = f.counts, e = f.events;
                boolean loss = totalTumor(f) == 0 || totalEc(f) == 0 || totalFibro(f) == 0 || totalMac(f) == 0;
                w.write(f.candidateId + "," + f.replicateIndex + "," + f.seed + "," + f.step + "," + fmt(days(f.step)) + "," +
                        totalTumor(f) + "," + c[0] + "," + c[1] + "," + totalEc(f) + "," + c[2] + "," + c[3] + "," +
                        totalFibro(f) + "," + c[6] + "," + c[7] + "," + totalMac(f) + "," + c[4] + "," + c[5] + "," +
                        event(e,6) + "," + event(e,7) + "," + event(e,2) + "," + event(e,3) + "," +
                        event(e,0) + "," + event(e,1) + "," + event(e,4) + "," + event(e,5) + ",0," + event(e,11) + "," + loss + "\n");
            }
        }
    }

    static void writeLossEvents(Path file, List<SimResult> results) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            w.write("candidate_id,replicate_index,seed,compartment,loss_step,biological_time,population_1_interval_before_loss,divisions_before_loss,deaths_before_loss,displacement_removal_events_before_loss,tumor_count_at_loss,EC_count_at_loss,fibroblast_count_at_loss,macrophage_count_at_loss,divProbP,pOffMax\n");
            ArrayList<LossEvent> events = new ArrayList<>();
            for (SimResult r : results) events.addAll(r.lossEvents);
            events.sort(Comparator.comparingInt((LossEvent e) -> e.candidateId).thenComparingInt(e -> e.replicateIndex).thenComparingLong(e -> e.seed).thenComparingInt(e -> e.lossStep).thenComparing(e -> e.compartment));
            for (LossEvent e : events) {
                w.write(e.candidateId + "," + e.replicateIndex + "," + e.seed + "," + csv(e.compartment) + "," + e.lossStep + "," +
                        fmt(e.biologicalTime) + "," + e.priorPopulation + "," + e.divisionsBeforeLoss + "," + e.deathsBeforeLoss + "," +
                        e.displacementBeforeLoss + "," + e.tumorAtLoss + "," + e.ecAtLoss + "," + e.fibroAtLoss + "," +
                        e.macrophageAtLoss + "," + fmt(e.divProbP) + "," + fmt(e.pOffMax) + "\n");
            }
        }
    }

    static final class CandidateSummary {
        int candidateId, total, valid, invalid, good, borderline, pass, poor, ecLoss, fibroLoss, combinedLoss, initFail;
        double divProbP, pOffMax, validFraction, invalidFraction, fracLe3, fracLe4, meanDistance, medianDistance;
        double sdDistance, minDistance, maxDistance, iqr, mad, meanRuntime, robustScore;
        String mostCommonInvalidReason, classification;
        LinkedHashMap<String, double[]> targetStats = new LinkedHashMap<>();
    }

    static List<CandidateSummary> summarizeCandidates(List<Candidate> candidates, List<SimResult> results) {
        ArrayList<CandidateSummary> out = new ArrayList<>();
        for (Candidate c : candidates) {
            ArrayList<SimResult> rs = new ArrayList<>();
            for (SimResult r : results) if (r.candidateId == c.candidateId) rs.add(r);
            CandidateSummary s = new CandidateSummary();
            s.candidateId = c.candidateId; s.divProbP = c.divProbP; s.pOffMax = c.pOffMax; s.total = rs.size();
            ArrayList<Double> distances = new ArrayList<>(), runtimes = new ArrayList<>();
            HashMap<String, Integer> invalidReasons = new HashMap<>();
            for (SimResult r : rs) {
                runtimes.add(r.runtimeSeconds);
                if (r.biologicallyValid && Double.isFinite(r.distance)) {
                    s.valid++; distances.add(r.distance);
                    if (r.distance <= GOOD) s.good++;
                    else if (r.distance <= CANDIDATE) s.borderline++;
                    else s.poor++;
                    if (r.distance <= CANDIDATE) s.pass++;
                } else {
                    s.invalid++;
                    invalidReasons.put(r.invalidReason, invalidReasons.getOrDefault(r.invalidReason, 0) + 1);
                }
                boolean ec = r.ecLossStep >= 0, fib = r.fibroblastLossStep >= 0;
                if (ec) s.ecLoss++;
                if (fib) s.fibroLoss++;
                if (ec && fib) s.combinedLoss++;
                if ("INITIALIZATION_FAILURE".equals(r.runStatus)) s.initFail++;
            }
            s.validFraction = frac(s.valid, s.total); s.invalidFraction = frac(s.invalid, s.total);
            s.fracLe3 = frac(s.good, s.total); s.fracLe4 = frac(s.pass, s.total);
            Collections.sort(distances);
            s.meanDistance = mean(distances); s.medianDistance = medianSorted(distances); s.sdDistance = sd(distances);
            s.minDistance = distances.isEmpty() ? Double.NaN : distances.get(0);
            s.maxDistance = distances.isEmpty() ? Double.NaN : distances.get(distances.size() - 1);
            s.iqr = quantile(distances, 0.75) - quantile(distances, 0.25);
            s.mad = mad(distances);
            s.meanRuntime = mean(runtimes);
            s.mostCommonInvalidReason = mode(invalidReasons);
            for (CalibrationTarget t : CalibrationTarget.currentTargets()) {
                ArrayList<Double> sim = new ArrayList<>(), resid = new ArrayList<>(), contrib = new ArrayList<>();
                for (SimResult r : rs) if (r.biologicallyValid && r.distanceResult != null) {
                    for (CalibrationTarget.TargetResult tr : r.distanceResult.targets) if (tr.target.id.equals(t.id)) {
                        sim.add(tr.simulated); resid.add(tr.standardizedResidual); contrib.add(tr.contribution);
                    }
                }
                s.targetStats.put(t.id, new double[]{mean(sim), median(sim), sd(sim), median(resid), median(contrib)});
            }
            int maxCollapse = Math.max(s.ecLoss, Math.max(s.fibroLoss, 0));
            boolean robust = s.validFraction >= 0.80 && s.medianDistance <= 4.0 && s.good >= 2 && frac(maxCollapse, s.total) <= 0.20;
            boolean rejected = s.validFraction < 0.50 || s.medianDistance > 5.0 || s.pass == 0;
            s.classification = robust ? "ROBUST" : (rejected ? "REJECTED" : "UNCERTAIN");
            double medianPenalty = Double.isFinite(s.medianDistance) ? Math.min(1.0, s.medianDistance / 6.0) : 1.0;
            double variancePenalty = Double.isFinite(s.sdDistance) ? Math.min(1.0, s.sdDistance / 3.0) : 1.0;
            double lossFreq = frac(Math.max(Math.max(s.ecLoss, s.fibroLoss), s.combinedLoss), s.total);
            s.robustScore = 0.40 * s.validFraction + 0.25 * (1.0 - medianPenalty) + 0.20 * s.fracLe4 +
                    0.10 * (1.0 - variancePenalty) + 0.05 * (1.0 - lossFreq);
            out.add(s);
        }
        out.sort(Comparator.comparing((CandidateSummary s) -> !"ROBUST".equals(s.classification))
                .thenComparing((CandidateSummary s) -> -s.robustScore)
                .thenComparingDouble(s -> s.medianDistance)
                .thenComparingInt(s -> s.candidateId));
        return out;
    }

    static void writeSummary(Path file, List<CandidateSummary> xs) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            StringBuilder h = new StringBuilder("robust_rank,candidate_id,divProbP,pOffMax,total_replicates,valid_count,invalid_count,valid_fraction,invalid_fraction,good_count,borderline_count,candidate_pass_count,poor_valid_count,fraction_distance_le_3,fraction_distance_le_4,mean_distance,median_distance,sd_distance,min_distance,max_distance,interquartile_range,median_absolute_deviation,mean_runtime,most_common_invalid_reason,EC_loss_count,fibroblast_loss_count,combined_EC_and_fibroblast_loss_count,initialization_failure_count,robustness_classification,robust_ranking_score,ranking_formula");
            for (CalibrationTarget t : CalibrationTarget.currentTargets()) {
                h.append(',').append(t.id).append("_mean_sim")
                        .append(',').append(t.id).append("_median_sim")
                        .append(',').append(t.id).append("_sd_sim")
                        .append(',').append(t.id).append("_median_normalized_residual")
                        .append(',').append(t.id).append("_median_distance_contribution");
            }
            w.write(h.toString()); w.newLine();
            int rank = 0;
            for (CandidateSummary s : xs) {
                if ("ROBUST".equals(s.classification)) rank++;
                StringBuilder b = new StringBuilder();
                b.append("ROBUST".equals(s.classification) ? rank : "").append(',').append(s.candidateId).append(',')
                        .append(fmt(s.divProbP)).append(',').append(fmt(s.pOffMax)).append(',').append(s.total).append(',')
                        .append(s.valid).append(',').append(s.invalid).append(',').append(fmt(s.validFraction)).append(',')
                        .append(fmt(s.invalidFraction)).append(',').append(s.good).append(',').append(s.borderline).append(',')
                        .append(s.pass).append(',').append(s.poor).append(',').append(fmt(s.fracLe3)).append(',')
                        .append(fmt(s.fracLe4)).append(',').append(fmt(s.meanDistance)).append(',').append(fmt(s.medianDistance)).append(',')
                        .append(fmt(s.sdDistance)).append(',').append(fmt(s.minDistance)).append(',').append(fmt(s.maxDistance)).append(',')
                        .append(fmt(s.iqr)).append(',').append(fmt(s.mad)).append(',').append(fmt(s.meanRuntime)).append(',')
                        .append(csv(s.mostCommonInvalidReason)).append(',').append(s.ecLoss).append(',').append(s.fibroLoss).append(',')
                        .append(s.combinedLoss).append(',').append(s.initFail).append(',').append(csv(s.classification)).append(',')
                        .append(fmt(s.robustScore)).append(',').append(csv("0.40*valid_fraction + 0.25*(1-min(median_distance/6,1)) + 0.20*fraction_distance_le_4 + 0.10*(1-min(sd_distance/3,1)) + 0.05*(1-max_compartment_loss_frequency)"));
                for (double[] a : s.targetStats.values()) for (double v : a) b.append(',').append(fmt(v));
                w.write(b.toString()); w.newLine();
            }
        }
    }

    static void writeRobust(Path file, List<CandidateSummary> xs) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            w.write("robust_rank,candidate_id,divProbP,pOffMax,valid_fraction,fraction_distance_le_3,fraction_distance_le_4,median_distance,sd_distance,invalid_fraction,EC_loss_count,fibroblast_loss_count,robust_ranking_score\n");
            int rank = 0;
            for (CandidateSummary s : xs) {
                if (!"ROBUST".equals(s.classification)) continue;
                rank++;
                w.write(rank + "," + s.candidateId + "," + fmt(s.divProbP) + "," + fmt(s.pOffMax) + "," +
                        fmt(s.validFraction) + "," + fmt(s.fracLe3) + "," + fmt(s.fracLe4) + "," +
                        fmt(s.medianDistance) + "," + fmt(s.sdDistance) + "," + fmt(s.invalidFraction) + "," +
                        s.ecLoss + "," + s.fibroLoss + "," + fmt(s.robustScore) + "\n");
            }
        }
    }

    static void writeManifest(Path file, Config c, String profileHash, String inputHash, long[] seeds, double runtimeSeconds) throws Exception {
        ModelParameters baseline = ModelParameters.currentBaseline(INIT_POP);
        StringBuilder b = new StringBuilder();
        b.append("{\n");
        b.append("  \"profile_name\": \"core2\",\n");
        b.append("  \"candidate_input_file\": ").append(json(c.input.toString())).append(",\n");
        b.append("  \"candidate_input_file_hash\": ").append(json(inputHash)).append(",\n");
        b.append("  \"repository_commit\": ").append(json(CalibrationFreeze.git("rev-parse", "HEAD"))).append(",\n");
        b.append("  \"working_tree_dirty\": ").append(!CalibrationFreeze.git("status", "--porcelain").isEmpty()).append(",\n");
        b.append("  \"master_seed\": ").append(c.masterSeed).append(",\n");
        b.append("  \"replicate_count\": ").append(c.replicates).append(",\n");
        b.append("  \"replicate_seeds\": ").append(Arrays.toString(seeds)).append(",\n");
        b.append("  \"thread_count\": ").append(c.threads).append(",\n");
        b.append("  \"runtime_seconds\": ").append(fmt(runtimeSeconds)).append(",\n");
        b.append("  \"created_at_utc\": ").append(json(Instant.now().toString())).append(",\n");
        b.append("  \"java_version\": ").append(json(System.getProperty("java.version"))).append(",\n");
        b.append("  \"operating_system\": ").append(json(System.getProperty("os.name") + " " + System.getProperty("os.version"))).append(",\n");
        b.append("  \"available_processor_count\": ").append(Runtime.getRuntime().availableProcessors()).append(",\n");
        b.append("  \"profile_hash\": ").append(json(profileHash)).append(",\n");
        b.append("  \"variable_parameter_ranges\": [{\"name\":\"divProbP\",\"lower\":0.005,\"upper\":0.03,\"sampling\":\"uniform_linear\"},{\"name\":\"pOffMax\",\"lower\":0.01,\"upper\":0.20,\"sampling\":\"uniform_linear\"}],\n");
        b.append("  \"frozen_parameter_values\": {\n");
        int n = 0, total = 0;
        for (ModelParameters.Definition d : ModelParameters.screenedDefinitions()) if (!CalibrationProfile.core2().includes(d.name)) total++;
        for (ModelParameters.Definition d : ModelParameters.screenedDefinitions()) {
            if (CalibrationProfile.core2().includes(d.name)) continue;
            b.append("    ").append(json(d.name)).append(": ").append(fmt(baseline.get(d.name))).append(++n < total ? "," : "").append("\n");
        }
        b.append("  },\n");
        b.append("  \"targets\": [\n");
        for (int i = 0; i < CalibrationTarget.currentTargets().size(); i++) {
            CalibrationTarget t = CalibrationTarget.currentTargets().get(i);
            b.append("    {\"name\": ").append(json(t.id)).append(", \"target\": ").append(fmt(t.observedValue))
                    .append(", \"scale\": ").append(fmt(t.scale)).append(", \"weight\": ").append(fmt(t.weight)).append("}")
                    .append(i + 1 < CalibrationTarget.currentTargets().size() ? "," : "").append("\n");
        }
        b.append("  ],\n");
        b.append("  \"acceptance_rules\": \"GOOD distance <= 3; BORDERLINE 3 < distance <= 4; candidate pass distance <= 4 and biologically valid; invalid distance Infinity and never posterior\",\n");
        b.append("  \"required_compartment_definitions\": \"tumor, EC, fibroblast, and macrophage totals must remain nonzero through required target observations\",\n");
        b.append("  \"thread_safety_policy\": \"fixed ExecutorService; one callable per candidate-seed; each callable constructs a fresh ExampleGrid, immutable ModelParameters, HAL Rand, diagnostic result buffers, and returns data to the coordinator; workers do not write shared CSVs\"\n");
        b.append("}\n");
        Files.writeString(file, b.toString(), StandardCharsets.UTF_8);
    }

    static Map<String, SimResult> readExistingResults(Path file, String profileHash) throws IOException {
        LinkedHashMap<String, SimResult> out = new LinkedHashMap<>();
        if (!Files.isRegularFile(file)) return out;
        try (BufferedReader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String header = r.readLine();
            if (header == null) return out;
            List<String> h = CalibrationFreeze.parseCsv(header);
            int cid = h.indexOf("candidate_id"), rep = h.indexOf("replicate_index"), seed = h.indexOf("seed"),
                    hash = h.indexOf("profile_hash"), div = h.indexOf("divProbP"), off = h.indexOf("pOffMax"),
                    status = h.indexOf("run_status"), valid = h.indexOf("biologically_valid"), dist = h.indexOf("distance"),
                    acc = h.indexOf("acceptance_class"), invalid = h.indexOf("invalid_reason"), runtime = h.indexOf("runtime_seconds"),
                    ec = h.indexOf("EC_loss_step"), fib = h.indexOf("fibroblast_loss_step");
            String line;
            while ((line = r.readLine()) != null) {
                List<String> row = CalibrationFreeze.parseCsv(line);
                if (!profileHash.equals(row.get(hash))) continue;
                SimResult sr = new SimResult();
                sr.candidateId = Integer.parseInt(row.get(cid));
                sr.replicateIndex = Integer.parseInt(row.get(rep));
                sr.seed = Long.parseLong(row.get(seed));
                sr.divProbP = Double.parseDouble(row.get(div));
                sr.pOffMax = Double.parseDouble(row.get(off));
                sr.runStatus = row.get(status);
                sr.biologicallyValid = Boolean.parseBoolean(row.get(valid));
                sr.distance = Double.parseDouble(row.get(dist));
                sr.acceptanceClass = row.get(acc);
                sr.invalidReason = row.get(invalid);
                sr.runtimeSeconds = Double.parseDouble(row.get(runtime));
                sr.ecLossStep = Integer.parseInt(row.get(ec));
                sr.fibroblastLossStep = Integer.parseInt(row.get(fib));
                sr.existingRow = line;
                out.put(sr.key(), sr);
            }
        }
        return out;
    }

    static int lastStep(List<ExampleGrid.DiagnosticFrame> frames) { return frames.isEmpty() ? -1 : frames.get(frames.size() - 1).step; }
    static int event(int[] e, int i) { return e != null && i >= 0 && i < e.length ? e[i] : 0; }
    static int totalTumor(Frame f) { return f.counts[0] + f.counts[1]; }
    static int totalEc(Frame f) { return f.counts[2] + f.counts[3]; }
    static int totalMac(Frame f) { return f.counts[4] + f.counts[5]; }
    static int totalFibro(Frame f) { return f.counts[6] + f.counts[7]; }
    static int compartmentTotal(Frame f, String c) {
        if ("tumor".equals(c)) return totalTumor(f);
        if ("EC".equals(c)) return totalEc(f);
        if ("fibroblast".equals(c)) return totalFibro(f);
        if ("macrophage".equals(c)) return totalMac(f);
        return -1;
    }
    static int compartmentDivisions(Frame f, String c) {
        if ("tumor".equals(c)) return event(f.events, 0);
        if ("EC".equals(c)) return event(f.events, 6);
        if ("fibroblast".equals(c)) return event(f.events, 2);
        if ("macrophage".equals(c)) return event(f.events, 4);
        return 0;
    }
    static int compartmentDeaths(Frame f, String c) {
        if ("tumor".equals(c)) return event(f.events, 1);
        if ("EC".equals(c)) return event(f.events, 7);
        if ("fibroblast".equals(c)) return event(f.events, 3);
        if ("macrophage".equals(c)) return event(f.events, 5);
        return 0;
    }
    static double days(int step) { return step / STEPS_PER_DAY; }
    static double frac(int n, int d) { return d > 0 ? (double)n / d : Double.NaN; }
    static double mean(List<Double> x) { double s = 0; int n = 0; for (double v : x) if (Double.isFinite(v)) { s += v; n++; } return n == 0 ? Double.NaN : s / n; }
    static double sd(List<Double> x) { ArrayList<Double> f = finite(x); if (f.size() < 2) return Double.NaN; double m = mean(f), s = 0; for (double v : f) s += (v - m) * (v - m); return Math.sqrt(s / (f.size() - 1)); }
    static double median(List<Double> x) { ArrayList<Double> f = finite(x); Collections.sort(f); return medianSorted(f); }
    static double medianSorted(List<Double> x) { return quantile(x, 0.5); }
    static double quantile(List<Double> sorted, double q) {
        if (sorted.isEmpty()) return Double.NaN;
        double pos = q * (sorted.size() - 1), lo = Math.floor(pos), hi = Math.ceil(pos);
        return sorted.get((int)lo) + (pos - lo) * (sorted.get((int)hi) - sorted.get((int)lo));
    }
    static double mad(List<Double> x) {
        ArrayList<Double> f = finite(x);
        if (f.isEmpty()) return Double.NaN;
        double med = median(f);
        ArrayList<Double> dev = new ArrayList<>();
        for (double v : f) dev.add(Math.abs(v - med));
        return median(dev);
    }
    static ArrayList<Double> finite(List<Double> x) { ArrayList<Double> y = new ArrayList<>(); for (double v : x) if (Double.isFinite(v)) y.add(v); return y; }
    static String mode(Map<String, Integer> m) {
        String best = ""; int count = 0;
        for (Map.Entry<String, Integer> e : m.entrySet()) if (e.getValue() > count) { best = e.getKey(); count = e.getValue(); }
        return best;
    }
    static void require(boolean ok, String message) { if (!ok) throw new IllegalStateException(message); }
    static String fmt(double x) {
        if (!Double.isFinite(x)) return Double.toString(x);
        if (x == Math.rint(x) && Math.abs(x) < 1e15) return Long.toString((long)x);
        return String.format(Locale.US, "%.12g", x);
    }
    static String csv(String s) { return "\"" + (s == null ? "" : s).replace("\"", "\"\"").replace("\n", " ").replace("\r", " ") + "\""; }
    static String json(String s) { return "\"" + (s == null ? "" : s).replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + "\""; }
}
