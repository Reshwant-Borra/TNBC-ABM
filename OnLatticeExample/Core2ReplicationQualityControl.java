package OnLatticeExample;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Regression checks for Core2CandidateReplication. */
public final class Core2ReplicationQualityControl {
    static void require(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        Path accepted = Path.of("results", "abc-core2-diagnostic-100", "abc_accepted.csv");
        List<Core2CandidateReplication.Candidate> candidates = Core2CandidateReplication.loadCandidates(accepted);
        require(candidates.size() == 13, "expected exactly 13 core2 candidates");
        require(new HashSet<Integer>() {{ for (Core2CandidateReplication.Candidate c : candidates) add(c.candidateId); }}.size() == 13,
                "candidate IDs are not unique");
        require(CalibrationProfile.core2().parameterNames().equals(Arrays.asList("divProbP", "pOffMax")),
                "unexpected core2 variable parameters");

        ModelParameters baseline = ModelParameters.currentBaseline(25);
        Core2CandidateReplication.Candidate first = candidates.get(0);
        ModelParameters applied = CalibrationProfile.core2().applyValues(25, new double[]{first.divProbP, first.pOffMax});
        for (Map.Entry<String, Double> e : baseline.values().entrySet()) {
            if (CalibrationProfile.core2().includes(e.getKey())) continue;
            require(Double.doubleToLongBits(applied.get(e.getKey())) == Double.doubleToLongBits(e.getValue()),
                    "frozen parameter changed: " + e.getKey());
        }

        long[] seedsA = Core2CandidateReplication.replicateSeeds(Core2CandidateReplication.DEFAULT_MASTER_SEED, 10);
        long[] seedsB = Core2CandidateReplication.replicateSeeds(Core2CandidateReplication.DEFAULT_MASTER_SEED, 10);
        require(Arrays.equals(seedsA, seedsB), "seed derivation is not deterministic");
        require(new HashSet<Long>() {{ for (long s : seedsA) add(s); }}.size() == 10, "replicate seeds are not unique");
        require(Core2CandidateReplication.taskCount(candidates, 10) == 130, "default task count is not 130");

        ArrayList<Core2CandidateReplication.Candidate> tiny = new ArrayList<>();
        tiny.add(candidates.get(0));
        tiny.add(candidates.get(1));
        List<Core2CandidateReplication.SimResult> one = runSubset(tiny, Arrays.copyOf(seedsA, 2), 1);
        List<Core2CandidateReplication.SimResult> two = runSubset(tiny, Arrays.copyOf(seedsA, 2), 2);
        List<Core2CandidateReplication.SimResult> eight = runSubset(tiny, Arrays.copyOf(seedsA, 2), 8);
        require(one.size() == 4 && two.size() == 4 && eight.size() == 4, "threaded subset task count mismatch");
        for (int i = 0; i < one.size(); i++) {
            String a = stableRow(one.get(i)), b = stableRow(two.get(i));
            require(a.equals(b), "one-thread and two-thread outputs differ at row " + i);
            require(a.equals(stableRow(eight.get(i))), "one-thread and eight-thread outputs differ at row " + i);
        }
        List<Core2CandidateReplication.SimResult> repeat = runSubset(tiny, Arrays.copyOf(seedsA, 2), 2);
        for (int i = 0; i < two.size(); i++) require(stableRow(two.get(i)).equals(stableRow(repeat.get(i))),
                "repeated execution changed row " + i);

        for (Core2CandidateReplication.SimResult r : one) {
            if (r.biologicallyValid) {
                double sum = 0.0;
                for (CalibrationTarget.TargetResult tr : r.distanceResult.targets) sum += tr.contribution;
                require(Math.abs(sum - r.distance * r.distance) < 1e-9, "distance contributions do not sum to d^2");
            } else {
                require(Double.isInfinite(r.distance), "invalid run distance is not infinite");
                require("INVALID".equals(r.acceptanceClass), "invalid run has non-invalid acceptance class");
            }
        }

        Core2CandidateReplication.SimResult bad = Core2CandidateReplication.runOne(
                CalibrationProfile.core2(), new Core2CandidateReplication.Candidate(999, -1.0, 0.1, ""), 0, seedsA[0]);
        require(!bad.biologicallyValid && Double.isInfinite(bad.distance), "worker exception was not captured as invalid");

        Core2CandidateReplication.SimResult synthetic = syntheticLoss();
        Core2CandidateReplication.detectLosses(synthetic, new Core2CandidateReplication.Candidate(1, 0.01, 0.1, ""));
        require(synthetic.ecLossStep == 2 && synthetic.fibroblastLossStep == 3, "loss-step detection failed");
        require("EC".equals(synthetic.firstLostCompartment), "first lost compartment detection failed");

        ArrayList<Core2CandidateReplication.SimResult> classified = new ArrayList<>();
        classified.add(syntheticSummaryRun(1, true, 2.0, false, false));
        classified.add(syntheticSummaryRun(1, true, 2.5, false, false));
        classified.add(syntheticSummaryRun(1, true, 3.5, false, false));
        classified.add(syntheticSummaryRun(1, true, 4.2, false, false));
        classified.add(syntheticSummaryRun(1, true, 4.0, false, false));
        classified.add(syntheticSummaryRun(1, true, 2.8, false, false));
        classified.add(syntheticSummaryRun(1, true, 3.8, false, false));
        classified.add(syntheticSummaryRun(1, true, 4.1, false, false));
        classified.add(syntheticSummaryRun(1, true, 2.9, false, false));
        classified.add(syntheticSummaryRun(1, true, 3.1, false, false));
        List<Core2CandidateReplication.CandidateSummary> robust = Core2CandidateReplication.summarizeCandidates(
                Arrays.asList(new Core2CandidateReplication.Candidate(1, 0.01, 0.1, "")), classified);
        require("ROBUST".equals(robust.get(0).classification), "ROBUST classification rule failed");

        ArrayList<Core2CandidateReplication.SimResult> rejectedRows = new ArrayList<>();
        for (int i = 0; i < 10; i++) rejectedRows.add(syntheticSummaryRun(2, i < 4, i < 4 ? 2.0 : Double.POSITIVE_INFINITY, true, false));
        List<Core2CandidateReplication.CandidateSummary> rejected = Core2CandidateReplication.summarizeCandidates(
                Arrays.asList(new Core2CandidateReplication.Candidate(2, 0.02, 0.1, "")), rejectedRows);
        require("REJECTED".equals(rejected.get(0).classification), "REJECTED classification rule failed");

        ArrayList<Core2CandidateReplication.SimResult> rankingRows = new ArrayList<>();
        for (int i = 0; i < 10; i++) rankingRows.add(syntheticSummaryRun(3, true, i == 0 ? 1.0 : 3.6, false, false));
        for (int i = 0; i < 10; i++) rankingRows.add(syntheticSummaryRun(4, true, 2.7, false, false));
        List<Core2CandidateReplication.CandidateSummary> ranked = Core2CandidateReplication.summarizeCandidates(
                Arrays.asList(new Core2CandidateReplication.Candidate(3, 0.01, 0.05, ""),
                        new Core2CandidateReplication.Candidate(4, 0.011, 0.06, "")), rankingRows);
        require(ranked.get(0).candidateId == 4, "robust ranking prioritized minimum single-run distance");

        Path tmp = Files.createTempDirectory("core2-repl-qc");
        String profileHash = CalibrationFreeze.sha256(CalibrationProfile.core2().canonicalCsv().getBytes(StandardCharsets.UTF_8));
        Core2CandidateReplication.writeReplicationCsv(tmp.resolve("core2_candidate_replications.csv"), profileHash, one.subList(0, 1));
        Map<String, Core2CandidateReplication.SimResult> existing =
                Core2CandidateReplication.readExistingResults(tmp.resolve("core2_candidate_replications.csv"), profileHash);
        require(existing.size() == 1, "resume did not read exactly one completed row");
        Core2CandidateReplication.writeReplicationCsv(tmp.resolve("core2_candidate_replications.csv"), profileHash, one.subList(0, 1));
        existing = Core2CandidateReplication.readExistingResults(tmp.resolve("core2_candidate_replications.csv"), profileHash);
        require(existing.size() == 1, "resume duplicated completed rows");
        Files.deleteIfExists(tmp.resolve("core2_candidate_replications.csv"));
        Files.deleteIfExists(tmp);

        require(noSharedMutableInstances(one), "simultaneous tasks appear to share result/frame buffers");

        System.out.println("PASS core2 candidate parsing and exact 13-row posterior source");
        System.out.println("PASS core2 variable/frozen parameter safeguards");
        System.out.println("PASS deterministic seed derivation and default 130 task count");
        System.out.println("PASS one-thread/two-thread/eight-thread deterministic equivalence and repeated execution");
        System.out.println("PASS target contribution sum, invalid infinity, and invalid exclusion class");
        System.out.println("PASS worker exception capture");
        System.out.println("PASS resume row de-duplication");
        System.out.println("PASS loss-step detection");
        System.out.println("PASS robustness classification and ranking rules");
        System.out.println("PASS result buffers are per-task objects");
    }

    static List<Core2CandidateReplication.SimResult> runSubset(List<Core2CandidateReplication.Candidate> candidates, long[] seeds, int threads) throws Exception {
        ExecutorService exec = Executors.newFixedThreadPool(threads);
        ArrayList<Future<Core2CandidateReplication.SimResult>> futures = new ArrayList<>();
        try {
            for (Core2CandidateReplication.Candidate c : candidates) {
                for (int i = 0; i < seeds.length; i++) {
                    final int rep = i;
                    final long seed = seeds[i];
                    Callable<Core2CandidateReplication.SimResult> task =
                            () -> Core2CandidateReplication.runOne(CalibrationProfile.core2(), c, rep, seed);
                    futures.add(exec.submit(task));
                }
            }
            ArrayList<Core2CandidateReplication.SimResult> out = new ArrayList<>();
            for (Future<Core2CandidateReplication.SimResult> f : futures) out.add(f.get());
            out.sort(Core2CandidateReplication.resultComparator());
            return out;
        } finally {
            exec.shutdownNow();
        }
    }

    static String stableRow(Core2CandidateReplication.SimResult r) {
        String row = Core2CandidateReplication.replicationRow(r, "qc_hash");
        List<String> cols = CalibrationFreeze.parseCsv(row);
        int runtime = CalibrationFreeze.parseCsv(Core2CandidateReplication.replicationHeader()).indexOf("runtime_seconds");
        cols.set(runtime, "RUNTIME");
        return String.join(",", cols);
    }

    static Core2CandidateReplication.SimResult syntheticLoss() {
        Core2CandidateReplication.SimResult r = new Core2CandidateReplication.SimResult();
        r.candidateId = 1; r.replicateIndex = 0; r.seed = 1L;
        r.frames.add(new Core2CandidateReplication.Frame(1,0,1L,0,new int[]{10,10,5,5,5,5,5,5},new int[12]));
        r.frames.add(new Core2CandidateReplication.Frame(1,0,1L,1,new int[]{10,10,1,0,5,5,5,5},new int[]{1,1,2,2,3,3,4,4,0,0,0,0}));
        r.frames.add(new Core2CandidateReplication.Frame(1,0,1L,2,new int[]{10,10,0,0,5,5,2,0},new int[]{1,1,2,2,3,3,4,5,0,0,0,0}));
        r.frames.add(new Core2CandidateReplication.Frame(1,0,1L,3,new int[]{10,10,0,0,5,5,0,0},new int[]{1,1,2,4,3,3,4,5,0,0,0,0}));
        return r;
    }

    static Core2CandidateReplication.SimResult syntheticSummaryRun(int candidateId, boolean valid, double distance, boolean ecLoss, boolean fibLoss) {
        Core2CandidateReplication.SimResult r = new Core2CandidateReplication.SimResult();
        r.candidateId = candidateId;
        r.biologicallyValid = valid;
        r.distance = valid ? distance : Double.POSITIVE_INFINITY;
        r.runStatus = valid ? "VALID_FINITE" : "STROMAL_COMPARTMENT_LOSS";
        r.invalidReason = valid ? "" : "EC_POPULATION_ZERO";
        r.acceptanceClass = valid ? (distance <= 3.0 ? "GOOD" : (distance <= 4.0 ? "BORDERLINE" : "POOR_VALID")) : "INVALID";
        r.ecLossStep = ecLoss ? 100 : -1;
        r.fibroblastLossStep = fibLoss ? 100 : -1;
        ArrayList<CalibrationTarget.TargetResult> targets = new ArrayList<>();
        for (CalibrationTarget t : CalibrationTarget.currentTargets()) {
            targets.add(new CalibrationTarget.TargetResult(t, t.observedValue, 0.0, 0.0, true, ""));
        }
        r.distanceResult = new CalibrationTarget.DistanceResult(r.distance, targets, r.runStatus);
        return r;
    }

    static boolean noSharedMutableInstances(List<Core2CandidateReplication.SimResult> xs) {
        HashSet<Integer> resultIds = new HashSet<>();
        HashSet<Integer> frameListIds = new HashSet<>();
        for (Core2CandidateReplication.SimResult r : xs) {
            if (!resultIds.add(System.identityHashCode(r))) return false;
            if (!frameListIds.add(System.identityHashCode(r.frames))) return false;
        }
        return true;
    }
}
