package OnLatticeExample;

import HAL.Rand;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/** Regression checks for calibration profiles, targets, freeze files, and ABC reproducibility. */
public final class CalibrationQualityControl {
    static void require(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }

    static int[][] run(ModelParameters p, long seed, int maxStep) throws Exception {
        ExampleGrid g = new ExampleGrid(100, 100);
        g.rng = new Rand(seed);
        return g.RunHeadless(p, maxStep);
    }

    public static void main(String[] args) throws Exception {
        long seed = args.length > 0 ? Long.parseLong(args[0]) : 9001L;
        int initPop = args.length > 1 ? Integer.parseInt(args[1]) : 25;
        int maxStep = args.length > 2 ? Integer.parseInt(args[2]) : 1440;

        require(Files.isRegularFile(Path.of("QuadratEndothelialOn.txt")), "missing QuadratEndothelialOn.txt");
        require(Files.isRegularFile(Path.of("QuadratStrOn.txt")), "missing QuadratStrOn.txt");

        CalibrationProfile core4 = CalibrationProfile.core4();
        CalibrationProfile core2 = CalibrationProfile.core2();
        CalibrationProfile legacy12 = CalibrationProfile.legacy12();
        require(core2.parameterNames().equals(Arrays.asList("divProbP", "pOffMax")),
                "core2 does not contain exactly divProbP and pOffMax");
        require(core4.parameterNames().equals(Arrays.asList("divProbP", "pOffMax", "divProbFP", "dieProbN")),
                "core4 does not match final Morris decision");
        require(new HashSet<>(core4.parameterNames()).size() == 4, "core4 parameters are not unique");
        require(legacy12.parameterNames().equals(Arrays.asList("netN", "dieProbN", "pOnMax", "pOffMax", "divProbP", "dieProbP",
                "cafDivBoost", "ecSurvival", "activProbF", "divProbFP", "activProbM", "activProbE")),
                "legacy12 order changed");
        for (CalibrationProfile profile : Arrays.asList(core2, core4, legacy12)) {
            profile.validate();
            for (CalibrationProfile.Parameter p : profile.parameters()) {
                require(ModelParameters.definition(p.name) != null, "profile parameter missing from ModelParameters: " + p.name);
                require(!CalibrationProfile.structurallyInactiveParameters().contains(p.name), "inactive parameter inferred: " + p.name);
                if ("core4".equals(profile.name())) {
                    require(!CalibrationProfile.unapprovedTierBParameters().contains(p.name), "unapproved Tier B inferred: " + p.name);
                }
            }
        }
        require(core2.parameters().get(0).samplingTransform == ModelParameters.Transform.LINEAR, "core2 divProbP must use linear sampling");
        require(core2.parameters().get(1).samplingTransform == ModelParameters.Transform.LINEAR, "core2 pOffMax must use linear sampling");
        require(core2.parameters().get(0).definition.lower == 0.005 && core2.parameters().get(0).definition.upper == 0.03,
                "core2 divProbP bounds changed");
        require(core2.parameters().get(1).definition.lower == 0.01 && core2.parameters().get(1).definition.upper == 0.20,
                "core2 pOffMax bounds changed");
        require(core4.parameters().get(0).samplingTransform == ModelParameters.Transform.LOG, "divProbP must use log sampling");
        require(core4.parameters().get(1).samplingTransform == ModelParameters.Transform.LOG, "pOffMax must use log sampling");
        require(core4.parameters().get(2).samplingTransform == ModelParameters.Transform.LINEAR, "divProbFP must use linear sampling");
        require(core4.parameters().get(3).samplingTransform == ModelParameters.Transform.LOG, "dieProbN must use log sampling");

        ModelParameters baseline = ModelParameters.currentBaseline(initPop);
        ModelParameters core2Lower = core2.applyValues(initPop, new double[]{0.005, 0.01});
        ModelParameters core2Mid = core2.applyValues(initPop, new double[]{0.0175, 0.105});
        ModelParameters core2Upper = core2.applyValues(initPop, new double[]{0.03, 0.20});
        require(core2Lower.divProbP == 0.005 && core2Lower.pOffMax == 0.01, "core2 lower values did not apply");
        require(core2Mid.divProbP == 0.0175 && core2Mid.pOffMax == 0.105, "core2 midpoint values did not apply");
        require(core2Upper.divProbP == 0.03 && core2Upper.pOffMax == 0.20, "core2 upper values did not apply");
        for (Map.Entry<String, Double> e : baseline.values().entrySet()) {
            if (core2.includes(e.getKey())) continue;
            require(Double.doubleToLongBits(core2Lower.get(e.getKey())) == Double.doubleToLongBits(e.getValue()),
                    "core2 lower changed frozen parameter " + e.getKey());
            require(Double.doubleToLongBits(core2Mid.get(e.getKey())) == Double.doubleToLongBits(e.getValue()),
                    "core2 midpoint changed frozen parameter " + e.getKey());
            require(Double.doubleToLongBits(core2Upper.get(e.getKey())) == Double.doubleToLongBits(e.getValue()),
                    "core2 upper changed frozen parameter " + e.getKey());
        }

        ExampleGrid legacyGrid = new ExampleGrid(100, 100);
        legacyGrid.rng = new Rand(seed);
        int[][] legacySnaps = legacyGrid.RunHeadless(baseline.legacyTheta(), initPop, maxStep);
        ExampleGrid namedGrid = new ExampleGrid(100, 100);
        namedGrid.rng = new Rand(seed);
        int[][] namedSnaps = namedGrid.RunHeadless(baseline, maxStep);
        require(Arrays.deepEquals(legacySnaps, namedSnaps), "legacy positional and named baseline snapshots differ");
        require(Arrays.deepEquals(legacyGrid.lastRimCore, namedGrid.lastRimCore), "rim/core diagnostics differ");
        require(Arrays.deepEquals(legacyGrid.lastEventCounts, namedGrid.lastEventCounts), "event counts differ");
        require(Double.doubleToLongBits(CalibrationTarget.distance(legacySnaps).distance) ==
                Double.doubleToLongBits(CalibrationTarget.distance(namedSnaps).distance), "legacy/named distance differs");

        ModelParameters coreAtBaseline = baseline;
        for (CalibrationProfile.Parameter cp : core4.parameters()) {
            coreAtBaseline = coreAtBaseline.with(cp.name, baseline.get(cp.name));
        }
        coreAtBaseline.validate();
        require(coreAtBaseline.csvRow().equals(baseline.csvRow()), "core4 baseline mutation changed the baseline");
        require(Arrays.deepEquals(run(coreAtBaseline, seed, maxStep), run(baseline, seed, maxStep)),
                "core4 baseline run differs from named baseline");

        ExampleGrid wiringGrid = new ExampleGrid(100, 100);
        wiringGrid.rng = new Rand(seed);
        wiringGrid.RunHeadless(core2Mid, maxStep);
        require(wiringGrid.divProbP == core2Mid.divProbP, "runtime divProbP does not match proposed core2 value");
        require(wiringGrid.pOffMax == core2Mid.pOffMax, "runtime pOffMax does not match proposed core2 value");
        require(wiringGrid.dieProbN == baseline.dieProbN && wiringGrid.pOnMax == baseline.pOnMax &&
                wiringGrid.divProbFP == baseline.divProbFP && wiringGrid.dieProbP == baseline.dieProbP,
                "runtime frozen values do not match baseline under core2");

        for (CalibrationProfile.Parameter cp : core4.parameters()) {
            ModelParameters changed = baseline.with(cp.name, cp.definition.upper);
            for (Map.Entry<String, Double> e : baseline.values().entrySet()) {
                if (e.getKey().equals(cp.name)) require(changed.get(e.getKey()) == cp.definition.upper, cp.name + " did not change");
                else require(Double.doubleToLongBits(changed.get(e.getKey())) == Double.doubleToLongBits(e.getValue()),
                        cp.name + " changed unrelated parameter " + e.getKey());
            }
            boolean failed = false;
            try { baseline.with(cp.name, cp.definition.upper + Math.max(1e-6, Math.abs(cp.definition.upper) * 0.1)).validate(); }
            catch (IllegalArgumentException ok) { failed = true; }
            require(failed, "validation did not catch out-of-range " + cp.name);
        }

        CalibrationTarget.validateTargets();
        require(CalibrationTarget.currentTargets().size() == ABCRejection.TT.length, "target array length mismatch");
        for (int i = 0; i < CalibrationTarget.currentTargets().size(); i++) {
            CalibrationTarget t = CalibrationTarget.currentTargets().get(i);
            require(t.statisticType.equals(ABCRejection.TT[i]), "target type drift at " + i);
            require(t.step == ABCRejection.TS[i], "target step drift at " + i);
            require(t.observedValue == ABCRejection.TV[i], "target value drift at " + i);
            require(t.weight == ABCRejection.TW[i], "target weight drift at " + i);
            require(t.scale == ABCRejection.TSC[i], "target scale drift at " + i);
        }
        require(Double.doubleToLongBits(ABCRejection.distance(namedSnaps)) ==
                Double.doubleToLongBits(CalibrationTarget.distance(namedSnaps).distance), "central target distance drift");

        ModelParameters a = core4.propose(ABCRejection.proposalSeed(12345L, 0), initPop);
        ModelParameters b = core4.propose(ABCRejection.proposalSeed(12345L, 0), initPop);
        ModelParameters c = core4.propose(ABCRejection.proposalSeed(12345L, 1), initPop);
        require(a.csvRow().equals(b.csvRow()), "identical proposal seed produced different proposal");
        require(!a.csvRow().equals(c.csvRow()), "different draw index produced identical proposal unexpectedly");
        require(Arrays.deepEquals(run(a, ABCRejection.simulationSeed(12345L, 0), maxStep),
                run(a, ABCRejection.simulationSeed(12345L, 0), maxStep)), "identical simulation seed/params not reproducible");

        ModelParameters ca = core2.propose(ABCRejection.proposalSeed(12345L, 0), initPop);
        ModelParameters cb = core2.propose(ABCRejection.proposalSeed(12345L, 0), initPop);
        require(ca.csvRow().equals(cb.csvRow()), "core2 identical proposal seed produced different proposal");
        for (CalibrationProfile.Parameter cp : core2.parameters()) {
            require(ca.get(cp.name) >= cp.definition.lower && ca.get(cp.name) <= cp.definition.upper,
                    "core2 proposal outside bounds: " + cp.name);
        }
        String fixedHash = ABCRejection.fixedSnapshotHash(core2, ca);
        for (int draw = 1; draw < 5; draw++) {
            ModelParameters p = core2.propose(ABCRejection.proposalSeed(12345L, draw), initPop);
            require(fixedHash.equals(ABCRejection.fixedSnapshotHash(core2, p)), "core2 fixed snapshot hash changed across proposals");
        }

        int lowDivisions = averageTumorDivisions(core2.applyValues(initPop, new double[]{0.005, baseline.pOffMax}), seed, maxStep);
        int highDivisions = averageTumorDivisions(core2.applyValues(initPop, new double[]{0.03, baseline.pOffMax}), seed, maxStep);
        require(highDivisions >= lowDivisions, "higher divProbP did not increase or preserve average tumour divisions");
        double lowOffJnk = averageJnkFraction(core2.applyValues(initPop, new double[]{baseline.divProbP, 0.01}), seed, maxStep);
        double highOffJnk = averageJnkFraction(core2.applyValues(initPop, new double[]{baseline.divProbP, 0.20}), seed, maxStep);
        require(highOffJnk <= lowOffJnk, "higher pOffMax did not decrease or preserve average final JNK+ fraction");

        CalibrationTarget.DistanceResult known = CalibrationTarget.distance(namedSnaps);
        double sum = 0.0;
        for (CalibrationTarget.TargetResult tr : known.targets) {
            double residual = tr.valid ? (tr.simulated - tr.target.observedValue) / tr.target.scale : 3.0;
            double independent = tr.target.weight * residual * residual;
            require(Math.abs(independent - tr.contribution) < 1e-12, "target contribution mismatch for " + tr.target.id);
            sum += tr.contribution;
        }
        require(Math.abs(Math.sqrt(sum) - known.distance) < 1e-12, "distance does not equal sqrt(sum contributions)");

        int[][] extinct = new int[][]{
                {20,5,10,10,10,10,10,10},
                {20,5,10,10,10,10,10,10},
                {20,5,10,10,10,10,10,10},
                {0,0,10,10,10,10,10,10}};
        ABCRejection.DrawResult extinctRun = classifiedSynthetic(extinct);
        require("TUMOR_EXTINCTION".equals(extinctRun.outcomeStatus), "tumor extinction not classified");
        require(!extinctRun.accepted && "invalid".equals(extinctRun.fitCategory), "tumor extinction accepted");
        int[][] stromaLoss = new int[][]{
                {20,5,10,10,10,10,10,10},
                {25,5,0,0,10,10,10,10},
                {30,5,0,0,10,10,10,10},
                {35,5,0,0,10,10,10,10}};
        ABCRejection.DrawResult stromaRun = classifiedSynthetic(stromaLoss);
        require("STROMAL_COMPARTMENT_LOSS".equals(stromaRun.outcomeStatus), "stroma loss not classified");
        require(!stromaRun.accepted && "invalid".equals(stromaRun.fitCategory), "stroma loss accepted");
        boolean malformedFailed = false;
        try { CalibrationTarget.distance(new int[][]{{1,1,1,1,1,1,1,1}}); }
        catch (IllegalArgumentException ok) { malformedFailed = true; }
        require(malformedFailed, "malformed snapshots were not rejected");
        ABCRejection.DrawResult exceptionRun = new ABCRejection.DrawResult();
        exceptionRun.outcomeStatus = "MODEL_EXCEPTION";
        exceptionRun.distance = new CalibrationTarget.DistanceResult(Double.POSITIVE_INFINITY, java.util.Collections.emptyList(), "MODEL_EXCEPTION");
        ABCRejection.Config qcConfig = ABCRejection.Config.parse(new String[]{"--profile","core2","--draws","1","--candidate-threshold","4.0","--output-dir","results/qc-unused"});
        ABCRejection.classifyFitAndAcceptance(qcConfig, exceptionRun, 4.0);
        require(!exceptionRun.accepted && "invalid".equals(exceptionRun.fitCategory), "exception run accepted");
        ABCRejection.DrawResult poorRun = new ABCRejection.DrawResult();
        poorRun.outcomeStatus = "VALID_FINITE";
        poorRun.distance = new CalibrationTarget.DistanceResult(4.1, java.util.Collections.emptyList(), "VALID_FINITE");
        ABCRejection.classifyFitAndAcceptance(qcConfig, poorRun, 4.0);
        require(!poorRun.accepted && "poor".equals(poorRun.fitCategory), "poor valid run accepted");
        ABCRejection.DrawResult goodRun = new ABCRejection.DrawResult();
        goodRun.outcomeStatus = "VALID_FINITE";
        goodRun.distance = new CalibrationTarget.DistanceResult(3.0, java.util.Collections.emptyList(), "VALID_FINITE");
        ABCRejection.classifyFitAndAcceptance(qcConfig, goodRun, 4.0);
        require(goodRun.accepted && "good".equals(goodRun.fitCategory), "good valid run rejected");

        Path temp = Files.createTempDirectory("core2-freeze-qc");
        ABCRejection.writeRuntimeFreeze(temp.resolve("freeze.json"), qcConfig, core2, 4.0);
        ABCRejection.writeFrozenParameterValues(temp.resolve("frozen.csv"), core2, initPop);
        String freezeText = Files.readString(temp.resolve("freeze.json"), StandardCharsets.UTF_8);
        require(freezeText.contains("\"profile_name\": \"core2\""), "runtime freeze profile missing");
        require(freezeText.contains("\"name\": \"divProbP\"") && freezeText.contains("\"name\": \"pOffMax\""),
                "runtime freeze variable parameters missing");
        require(Files.readString(temp.resolve("frozen.csv"), StandardCharsets.UTF_8).contains("dieProbN"),
                "runtime frozen parameter CSV missing frozen values");
        Files.deleteIfExists(temp.resolve("freeze.json"));
        Files.deleteIfExists(temp.resolve("frozen.csv"));
        Files.deleteIfExists(temp);

        String profileHash = CalibrationFreeze.sha256(core4.canonicalCsv().getBytes(StandardCharsets.UTF_8));
        String targetHash = CalibrationFreeze.sha256(CalibrationTarget.canonicalCsv().getBytes(StandardCharsets.UTF_8));
        require(!profileHash.equals(CalibrationFreeze.sha256((core4.canonicalCsv() + "x").getBytes(StandardCharsets.UTF_8))),
                "profile hash did not change after content change");
        require(!targetHash.equals(CalibrationFreeze.sha256((CalibrationTarget.canonicalCsv() + "x").getBytes(StandardCharsets.UTF_8))),
                "target hash did not change after content change");
        CalibrationFreeze.verifyFreeze(CalibrationFreeze.DEFAULT_DIR);

        System.out.println("PASS core2 profile membership, linear sampling, exact bounds, and frozen values");
        System.out.println("PASS profile integrity, core4 decision, and legacy12 order");
        System.out.println("PASS legacy positional == named baseline snapshots, diagnostics, events, and distance");
        System.out.println("PASS core2 runtime parameter wiring and representative boundary/midpoint behavior");
        System.out.println("PASS core4 baseline equivalence and named mutation validation");
        System.out.println("PASS target integrity and centralized distance equivalence");
        System.out.println("PASS independent distance contribution recomputation");
        System.out.println("PASS invalid-run and poor-fit classification/rejection");
        System.out.println("PASS runtime diagnostic freeze generation");
        System.out.println("PASS deterministic proposals and simulation reproducibility");
        System.out.println("PASS freeze hashes and stale-freeze guard");
        System.out.println("seed=" + seed + " initPop=" + initPop + " maxStep=" + maxStep +
                " core2=" + core2.parameterNames() + " core4=" + core4.parameterNames() +
                " targets=" + CalibrationTarget.currentTargets().size());
    }

    static int averageTumorDivisions(ModelParameters p, long seed, int maxStep) throws Exception {
        long sum = 0;
        int n = 5;
        for (int i = 0; i < n; i++) {
            ExampleGrid g = new ExampleGrid(100, 100);
            g.rng = new Rand(seed + 1000 + i);
            g.RunHeadless(p, maxStep);
            int[] last = g.lastEventCounts[g.lastEventCounts.length - 1];
            sum += last[0];
        }
        return (int)Math.round((double)sum / n);
    }

    static double averageJnkFraction(ModelParameters p, long seed, int maxStep) throws Exception {
        double sum = 0.0;
        int n = 5;
        for (int i = 0; i < n; i++) {
            int[][] snaps = run(p, seed + 2000 + i, maxStep);
            sum += CalibrationTarget.stat(snaps, "jnkp", 1440);
        }
        return sum / n;
    }

    static ABCRejection.DrawResult classifiedSynthetic(int[][] snapshots) {
        ABCRejection.DrawResult r = new ABCRejection.DrawResult();
        r.snapshots = snapshots;
        r.distance = CalibrationTarget.distance(snapshots);
        ABCRejection.classify(r);
        ABCRejection.Config c = ABCRejection.Config.parse(new String[]{"--profile","core2","--draws","1","--candidate-threshold","4.0","--output-dir","results/qc-unused"});
        ABCRejection.classifyFitAndAcceptance(c, r, 4.0);
        return r;
    }
}
