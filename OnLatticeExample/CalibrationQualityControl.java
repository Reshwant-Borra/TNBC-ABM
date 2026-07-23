package OnLatticeExample;

import HAL.Rand;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
        CalibrationProfile legacy12 = CalibrationProfile.legacy12();
        require(core4.parameterNames().equals(Arrays.asList("divProbP", "pOffMax", "divProbFP", "dieProbN")),
                "core4 does not match final Morris decision");
        require(new HashSet<>(core4.parameterNames()).size() == 4, "core4 parameters are not unique");
        require(legacy12.parameterNames().equals(Arrays.asList("netN", "dieProbN", "pOnMax", "pOffMax", "divProbP", "dieProbP",
                "cafDivBoost", "ecSurvival", "activProbF", "divProbFP", "activProbM", "activProbE")),
                "legacy12 order changed");
        for (CalibrationProfile profile : Arrays.asList(core4, legacy12)) {
            profile.validate();
            for (CalibrationProfile.Parameter p : profile.parameters()) {
                require(ModelParameters.definition(p.name) != null, "profile parameter missing from ModelParameters: " + p.name);
                require(!CalibrationProfile.structurallyInactiveParameters().contains(p.name), "inactive parameter inferred: " + p.name);
                if ("core4".equals(profile.name())) {
                    require(!CalibrationProfile.unapprovedTierBParameters().contains(p.name), "unapproved Tier B inferred: " + p.name);
                }
            }
        }
        require(core4.parameters().get(0).samplingTransform == ModelParameters.Transform.LOG, "divProbP must use log sampling");
        require(core4.parameters().get(1).samplingTransform == ModelParameters.Transform.LOG, "pOffMax must use log sampling");
        require(core4.parameters().get(2).samplingTransform == ModelParameters.Transform.LINEAR, "divProbFP must use linear sampling");
        require(core4.parameters().get(3).samplingTransform == ModelParameters.Transform.LOG, "dieProbN must use log sampling");

        ModelParameters baseline = ModelParameters.currentBaseline(initPop);
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

        String profileHash = CalibrationFreeze.sha256(core4.canonicalCsv().getBytes(StandardCharsets.UTF_8));
        String targetHash = CalibrationFreeze.sha256(CalibrationTarget.canonicalCsv().getBytes(StandardCharsets.UTF_8));
        require(!profileHash.equals(CalibrationFreeze.sha256((core4.canonicalCsv() + "x").getBytes(StandardCharsets.UTF_8))),
                "profile hash did not change after content change");
        require(!targetHash.equals(CalibrationFreeze.sha256((CalibrationTarget.canonicalCsv() + "x").getBytes(StandardCharsets.UTF_8))),
                "target hash did not change after content change");
        CalibrationFreeze.verifyFreeze(CalibrationFreeze.DEFAULT_DIR);

        System.out.println("PASS profile integrity, core4 decision, and legacy12 order");
        System.out.println("PASS legacy positional == named baseline snapshots, diagnostics, events, and distance");
        System.out.println("PASS core4 baseline equivalence and named mutation validation");
        System.out.println("PASS target integrity and centralized distance equivalence");
        System.out.println("PASS deterministic proposals and simulation reproducibility");
        System.out.println("PASS freeze hashes and stale-freeze guard");
        System.out.println("seed=" + seed + " initPop=" + initPop + " maxStep=" + maxStep +
                " core4=" + core4.parameterNames() + " targets=" + CalibrationTarget.currentTargets().size());
    }
}
