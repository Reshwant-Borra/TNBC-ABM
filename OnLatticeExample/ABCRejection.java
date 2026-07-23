package OnLatticeExample;

import HAL.Rand;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SplittableRandom;

/**
 * Rejection ABC driver for the untreated TNBC ABM.
 *
 * New flag-based runs use named calibration profiles and write all outputs under
 * a run directory. Old positional arguments are accepted as a legacy12 wrapper:
 *   ABCRejection [N] [epsilon] [quantile] [seed] [initPop]
 */
public class ABCRejection {
    static final String[] NAME = CalibrationProfile.legacy12().parameterNames().toArray(new String[0]);
    static final double[] LO = legacyBounds(true);
    static final double[] HI = legacyBounds(false);
    static final String[] TT = targetTypes();
    static final int[] TS = targetSteps();
    static final double[] TV = targetValues();
    static final double[] TW = targetWeights();
    static final double[] TSC = targetScales();
    static final int[] SNAP = CalibrationTarget.SNAP.clone();

    static int bestMacTry = 0, bestMacFail = 0;

    static double[] legacyBounds(boolean lower) {
        CalibrationProfile legacy = CalibrationProfile.legacy12();
        double[] out = new double[legacy.parameters().size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = lower ? legacy.parameters().get(i).definition.lower : legacy.parameters().get(i).definition.upper;
        }
        return out;
    }

    static String[] targetTypes() {
        List<CalibrationTarget> t = CalibrationTarget.currentTargets();
        String[] out = new String[t.size()];
        for (int i = 0; i < out.length; i++) out[i] = t.get(i).statisticType;
        return out;
    }
    static int[] targetSteps() {
        List<CalibrationTarget> t = CalibrationTarget.currentTargets();
        int[] out = new int[t.size()];
        for (int i = 0; i < out.length; i++) out[i] = t.get(i).step;
        return out;
    }
    static double[] targetValues() {
        List<CalibrationTarget> t = CalibrationTarget.currentTargets();
        double[] out = new double[t.size()];
        for (int i = 0; i < out.length; i++) out[i] = t.get(i).observedValue;
        return out;
    }
    static double[] targetWeights() {
        List<CalibrationTarget> t = CalibrationTarget.currentTargets();
        double[] out = new double[t.size()];
        for (int i = 0; i < out.length; i++) out[i] = t.get(i).weight;
        return out;
    }
    static double[] targetScales() {
        List<CalibrationTarget> t = CalibrationTarget.currentTargets();
        double[] out = new double[t.size()];
        for (int i = 0; i < out.length; i++) out[i] = t.get(i).scale;
        return out;
    }

    static int snapIdx(int step) { return CalibrationTarget.snapIdx(step); }
    static double stat(int[][] s, String type, int step) { return CalibrationTarget.stat(s, type, step); }
    static double distance(int[][] s) { return CalibrationTarget.distance(s).distance; }

    static final class Config {
        String profileName = "core4";
        int draws = 1000;
        double epsilon = -1.0;
        double quantile = 0.05;
        long seed = 12345L;
        int initPop = 25;
        int maxStep = 1440;
        Path outputDir = null;
        boolean dryRun = false, resume = false, force = false, legacyPositional = false;

        static Config parse(String[] args) {
            Config c = new Config();
            if (args.length > 0 && !args[0].startsWith("--")) {
                c.legacyPositional = true;
                c.profileName = "legacy12";
                c.draws = args.length > 0 ? Integer.parseInt(args[0]) : 1000;
                c.epsilon = args.length > 1 ? Double.parseDouble(args[1]) : -1.0;
                c.quantile = args.length > 2 ? Double.parseDouble(args[2]) : 0.2;
                c.seed = args.length > 3 ? Long.parseLong(args[3]) : 12345L;
                c.initPop = args.length > 4 ? Integer.parseInt(args[4]) : 25;
                if (args.length > 5) throw new IllegalArgumentException("legacy positional mode accepts at most five arguments");
                return c.validate();
            }
            for (int i = 0; i < args.length; i++) {
                String a = args[i];
                if ("--dry-run".equals(a)) { c.dryRun = true; continue; }
                if ("--resume".equals(a)) { c.resume = true; continue; }
                if ("--force".equals(a)) { c.force = true; continue; }
                if (i + 1 >= args.length) throw new IllegalArgumentException("missing value after " + a);
                String v = args[++i];
                switch (a) {
                    case "--profile": c.profileName = v; break;
                    case "--draws": c.draws = Integer.parseInt(v); break;
                    case "--epsilon": c.epsilon = Double.parseDouble(v); break;
                    case "--quantile": c.quantile = Double.parseDouble(v); break;
                    case "--seed": c.seed = Long.parseLong(v); break;
                    case "--init-pop": c.initPop = Integer.parseInt(v); break;
                    case "--max-step": c.maxStep = Integer.parseInt(v); break;
                    case "--output-dir": c.outputDir = Path.of(v); break;
                    default: throw new IllegalArgumentException("unknown option: " + a);
                }
            }
            return c.validate();
        }

        Config validate() {
            CalibrationProfile.byName(profileName).validate();
            if (draws < 1) throw new IllegalArgumentException("--draws must be positive");
            if (!Double.isFinite(epsilon)) throw new IllegalArgumentException("--epsilon must be finite; use -1 for quantile mode");
            if (!(quantile > 0.0 && quantile <= 1.0)) throw new IllegalArgumentException("--quantile must be in (0,1]");
            if (initPop < 1) throw new IllegalArgumentException("--init-pop must be positive");
            if (maxStep < 0) throw new IllegalArgumentException("--max-step must be nonnegative");
            if (maxStep < 1440) throw new IllegalArgumentException("--max-step must reach all current targets (1440)");
            if (outputDir == null) outputDir = Path.of("results", "abc-" + profileName + "-" + timestamp());
            return this;
        }
    }

    static final class DrawResult {
        int drawId;
        long proposalSeed, simulationSeed, runtimeMillis;
        ModelParameters parameters;
        int[][] snapshots = new int[0][];
        int[][] rimCore = new int[0][];
        int[][] eventCounts = new int[0][];
        double[][] spatialMetrics = new double[0][];
        CalibrationTarget.DistanceResult distance;
        String outcomeStatus = "";
        String failureReason = "";
        String errorClass = "";
        String errorMessage = "";
        boolean accepted;
        String existingCsvRow;
    }

    public static void main(String[] args) throws Exception {
        Config c = Config.parse(args);
        for (String req : new String[]{"QuadratEndothelialOn.txt", "QuadratStrOn.txt"}) {
            if (!Files.isRegularFile(Path.of(req))) {
                throw new IOException("required input not found in working dir: " + req);
            }
        }
        CalibrationTarget.validateTargets();
        CalibrationFreeze.verifyFreeze(CalibrationFreeze.DEFAULT_DIR);
        CalibrationProfile profile = CalibrationProfile.byName(c.profileName);
        prepareOutputDir(c, profile);

        System.out.printf(Locale.US, "ABC rejection | profile=%s | draws=%d | targets=%d | seed=%d | output=%s%n",
                profile.name(), c.draws, CalibrationTarget.currentTargets().size(), c.seed, c.outputDir);
        if (c.dryRun) {
            writeManifest(c, profile, "DRY_RUN", 0, 0, Double.NaN, 0);
            System.out.println("dry run complete: configuration validated and manifest written");
            return;
        }

        Map<Integer, String> resumedRows = c.resume ? readCompletedRows(c.outputDir.resolve("abc_all_draws.csv")) : Collections.emptyMap();
        List<DrawResult> results = new ArrayList<>();
        long start = System.currentTimeMillis();
        for (int n = 0; n < c.draws; n++) {
            if (resumedRows.containsKey(n)) {
                DrawResult r = new DrawResult();
                r.drawId = n;
                r.existingCsvRow = resumedRows.get(n);
                hydrateExisting(r);
                results.add(r);
                continue;
            }
            DrawResult r = runDraw(c, profile, n);
            results.add(r);
            if ((n + 1) % 25 == 0) {
                double secs = (System.currentTimeMillis() - start) / 1000.0;
                System.out.printf(Locale.US, "  %d/%d done (%.0f s, %.2f s/draw)%n", n + 1, c.draws, secs, secs / (n + 1));
            }
        }

        double eps = selectEpsilon(c, results);
        for (DrawResult r : results) {
            if (r.existingCsvRow == null && r.distance != null) r.accepted = Double.isFinite(r.distance.distance) && r.distance.distance <= eps;
        }
        writeAllOutputs(c, profile, results, eps);
        Summary s = summarize(results, eps);
        writeManifest(c, profile, "COMPLETE", s.validRuns, s.acceptanceCount, eps, System.currentTimeMillis() - start);
        System.out.printf(Locale.US, "accepted %d/%d draws (epsilon %.6g) -> %s%n", s.acceptanceCount, c.draws, eps, c.outputDir.resolve("abc_accepted.csv"));
    }

    static DrawResult runDraw(Config c, CalibrationProfile profile, int drawId) {
        DrawResult r = new DrawResult();
        r.drawId = drawId;
        r.proposalSeed = proposalSeed(c.seed, drawId);
        r.simulationSeed = simulationSeed(c.seed, drawId);
        long t0 = System.nanoTime();
        try {
            r.parameters = profile.propose(r.proposalSeed, c.initPop);
            ExampleGrid model = new ExampleGrid(100, 100);
            model.rng = new Rand(r.simulationSeed);
            r.snapshots = model.RunHeadless(r.parameters, c.maxStep);
            r.rimCore = deepCopy(model.lastRimCore);
            r.eventCounts = deepCopy(model.lastEventCounts);
            r.spatialMetrics = deepCopy(model.lastSpatialMetrics);
            if (r.snapshots.length == 0 || r.snapshots[0].length < 2 || r.snapshots[0][0] + r.snapshots[0][1] != c.initPop) {
                r.outcomeStatus = "INITIALIZATION_FAILURE";
                r.failureReason = "initial tumour count did not equal initPop";
                r.distance = new CalibrationTarget.DistanceResult(Double.POSITIVE_INFINITY,
                        CalibrationTarget.perTarget(safeSnapshots(r.snapshots)), "INITIALIZATION_FAILURE");
            } else {
                r.distance = CalibrationTarget.distance(r.snapshots);
                classify(r);
            }
        } catch (IllegalArgumentException e) {
            r.outcomeStatus = "PARAMETER_VALIDATION_FAILURE";
            r.failureReason = e.getMessage();
            r.errorClass = e.getClass().getName();
            r.errorMessage = e.getMessage();
            r.distance = new CalibrationTarget.DistanceResult(Double.POSITIVE_INFINITY, Collections.emptyList(), r.outcomeStatus);
        } catch (Throwable e) {
            r.outcomeStatus = "MODEL_EXCEPTION";
            r.failureReason = e.getMessage() == null ? e.toString() : e.getMessage();
            r.errorClass = e.getClass().getName();
            r.errorMessage = e.getMessage() == null ? e.toString() : e.getMessage();
            r.distance = new CalibrationTarget.DistanceResult(Double.POSITIVE_INFINITY, Collections.emptyList(), r.outcomeStatus);
        } finally {
            r.runtimeMillis = (System.nanoTime() - t0) / 1_000_000L;
        }
        return r;
    }

    static void classify(DrawResult r) {
        if (!Double.isFinite(r.distance.distance) && "TUMOR_EXTINCTION".equals(r.distance.statusHint)) {
            r.outcomeStatus = "TUMOR_EXTINCTION";
            r.failureReason = "final tumour denominator is zero";
            return;
        }
        boolean ec = false, mac = false, fib = false, other = false;
        for (CalibrationTarget.TargetResult tr : r.distance.targets) if (!tr.valid) {
            if ("EC_POPULATION_ZERO".equals(tr.invalidReason)) ec = true;
            else if ("MACROPHAGE_POPULATION_ZERO".equals(tr.invalidReason)) mac = true;
            else if ("FIBROBLAST_POPULATION_ZERO".equals(tr.invalidReason)) fib = true;
            else other = true;
        }
        if (ec || mac || fib) {
            r.outcomeStatus = "STROMAL_COMPARTMENT_LOSS";
            ArrayList<String> reasons = new ArrayList<>();
            if (ec) reasons.add("EC_POPULATION_ZERO");
            if (mac) reasons.add("MACROPHAGE_POPULATION_ZERO");
            if (fib) reasons.add("FIBROBLAST_POPULATION_ZERO");
            r.failureReason = String.join(";", reasons);
        } else if (other) {
            r.outcomeStatus = "INVALID_TARGET_STATISTIC";
            r.failureReason = "one or more target statistics undefined";
        } else {
            r.outcomeStatus = "VALID_FINITE";
            r.failureReason = "";
        }
    }

    static int[][] safeSnapshots(int[][] s) {
        return (s != null && s.length == CalibrationTarget.SNAP.length) ? s : new int[][]{{0,0,0,0,0,0,0,0},{0,0,0,0,0,0,0,0},{0,0,0,0,0,0,0,0},{0,0,0,0,0,0,0,0}};
    }

    static double selectEpsilon(Config c, List<DrawResult> results) {
        if (c.epsilon > 0.0) return c.epsilon;
        ArrayList<Double> finite = new ArrayList<>();
        for (DrawResult r : results) if (r.distance != null && Double.isFinite(r.distance.distance)) finite.add(r.distance.distance);
        if (finite.isEmpty()) return Double.POSITIVE_INFINITY;
        finite.sort(Double::compare);
        int keep = Math.max(1, (int) Math.round(c.quantile * finite.size()));
        return finite.get(Math.min(finite.size() - 1, keep - 1));
    }

    static void prepareOutputDir(Config c, CalibrationProfile profile) throws Exception {
        if (Files.exists(c.outputDir)) {
            boolean empty = isEmpty(c.outputDir);
            if (!empty && !c.resume && !c.force) {
                throw new IOException("refusing to write into nonempty output directory without --resume or --force: " + c.outputDir);
            }
            if (c.resume) {
                Path manifest = c.outputDir.resolve("run_manifest.json");
                if (!Files.isRegularFile(manifest)) throw new IOException("--resume requires existing run_manifest.json");
                String text = Files.readString(manifest, StandardCharsets.UTF_8);
                require(text.contains("\"profile\": \"" + profile.name() + "\""), "resume profile mismatch");
                require(text.contains("\"master_seed\": " + c.seed), "resume seed mismatch");
                require(text.contains("\"draws\": " + c.draws), "resume draws mismatch");
                require(text.contains("\"init_population\": " + c.initPop), "resume initPop mismatch");
                require(text.contains("\"max_step\": " + c.maxStep), "resume maxStep mismatch");
            }
        }
        Files.createDirectories(c.outputDir);
    }

    static boolean isEmpty(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) return true;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) {
            return !ds.iterator().hasNext();
        }
    }

    static Map<Integer, String> readCompletedRows(Path csv) throws IOException {
        HashMap<Integer, String> out = new HashMap<>();
        if (!Files.isRegularFile(csv)) return out;
        try (BufferedReader r = Files.newBufferedReader(csv, StandardCharsets.UTF_8)) {
            r.readLine();
            String line;
            while ((line = r.readLine()) != null) {
                int comma = line.indexOf(',');
                if (comma > 0) out.put(Integer.parseInt(line.substring(0, comma)), line);
            }
        }
        return out;
    }

    static void hydrateExisting(DrawResult r) {
        List<String> row = CalibrationFreeze.parseCsv(r.existingCsvRow);
        int n = row.size();
        if (n < 5) throw new IllegalArgumentException("malformed resumed ABC row for draw " + r.drawId);
        double distance = parseDouble(row.get(n - 5));
        r.outcomeStatus = row.get(n - 4);
        r.failureReason = row.get(n - 3);
        r.runtimeMillis = (long) parseDouble(row.get(n - 2));
        r.accepted = Boolean.parseBoolean(row.get(n - 1));
        r.distance = new CalibrationTarget.DistanceResult(distance, Collections.emptyList(), r.outcomeStatus);
    }

    static double parseDouble(String s) {
        if (s == null || s.isEmpty()) return Double.NaN;
        return Double.parseDouble(s);
    }

    static void writeAllOutputs(Config c, CalibrationProfile profile, List<DrawResult> results, double eps) throws Exception {
        results.sort(Comparator.comparingInt(x -> x.drawId));
        String header = allDrawsHeader(profile);
        try (BufferedWriter all = Files.newBufferedWriter(c.outputDir.resolve("abc_all_draws.csv"), StandardCharsets.UTF_8);
             BufferedWriter acc = Files.newBufferedWriter(c.outputDir.resolve("abc_accepted.csv"), StandardCharsets.UTF_8)) {
            all.write(header); all.newLine();
            acc.write(header); acc.newLine();
            for (DrawResult r : results) {
                String row = r.existingCsvRow == null ? drawCsvRow(profile, r, eps) : r.existingCsvRow;
                all.write(row); all.newLine();
                if (row.endsWith(",true")) { acc.write(row); acc.newLine(); }
            }
        }
        writeDistanceSummary(c.outputDir.resolve("distance_summary.csv"), summarize(results, eps));
        writeBestBreakdown(c.outputDir.resolve("best_run_target_breakdown.csv"), best(results));
        Files.writeString(c.outputDir.resolve("resolved_config.json"), resolvedConfigJson(c, profile, "PENDING", 0, 0, eps, 0), StandardCharsets.UTF_8);
    }

    static String allDrawsHeader(CalibrationProfile profile) {
        StringBuilder h = new StringBuilder("draw_id,proposal_seed,simulation_seed,profile,fixed_snapshot_hash");
        for (CalibrationProfile.Parameter p : profile.parameters()) h.append(',').append(p.name);
        for (CalibrationTarget t : CalibrationTarget.currentTargets()) {
            h.append(",sim_").append(t.id).append(",resid_").append(t.id).append(",contrib_").append(t.id).append(",valid_").append(t.id).append(",invalid_reason_").append(t.id);
        }
        h.append(",total_distance,outcome_status,failure_reason,runtime_ms,accepted");
        return h.toString();
    }

    static String drawCsvRow(CalibrationProfile profile, DrawResult r, double eps) throws Exception {
        StringBuilder b = new StringBuilder();
        b.append(r.drawId).append(',').append(r.proposalSeed).append(',').append(r.simulationSeed).append(',').append(csv(profile.name()))
                .append(',').append(csv(fixedSnapshotHash(profile, r.parameters)));
        for (CalibrationProfile.Parameter p : profile.parameters()) b.append(',').append(fmt(r.parameters == null ? Double.NaN : r.parameters.get(p.name)));
        Map<String, CalibrationTarget.TargetResult> byId = new HashMap<>();
        if (r.distance != null) for (CalibrationTarget.TargetResult tr : r.distance.targets) byId.put(tr.target.id, tr);
        for (CalibrationTarget t : CalibrationTarget.currentTargets()) {
            CalibrationTarget.TargetResult tr = byId.get(t.id);
            b.append(',').append(fmt(tr == null ? Double.NaN : tr.simulated))
                    .append(',').append(fmt(tr == null ? Double.NaN : tr.standardizedResidual))
                    .append(',').append(fmt(tr == null ? Double.NaN : tr.contribution))
                    .append(',').append(tr != null && tr.valid)
                    .append(',').append(csv(tr == null ? "NO_TARGET_RESULT" : tr.invalidReason));
        }
        b.append(',').append(fmt(r.distance == null ? Double.POSITIVE_INFINITY : r.distance.distance))
                .append(',').append(csv(r.outcomeStatus)).append(',').append(csv(r.failureReason))
                .append(',').append(r.runtimeMillis).append(',').append(r.accepted);
        return b.toString();
    }

    static DrawResult best(List<DrawResult> results) {
        DrawResult best = null;
        for (DrawResult r : results) {
            if (r.distance == null || !Double.isFinite(r.distance.distance)) continue;
            if (best == null || r.distance.distance < best.distance.distance) best = r;
        }
        if (best != null) {
            bestMacTry = best.eventCounts.length == 0 ? 0 : bestMacTry;
        }
        return best;
    }

    static void writeBestBreakdown(Path file, DrawResult best) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            w.write("target_id,statistic_type,step,simulated,target,weight,scale,standardized_residual,distance_contribution,share_of_d2,valid,invalid_reason\n");
            if (best == null || best.distance == null) return;
            double d2 = best.distance.distance * best.distance.distance;
            for (CalibrationTarget.TargetResult tr : best.distance.targets) {
                double share = d2 > 0.0 ? tr.contribution / d2 : 0.0;
                w.write(csv(tr.target.id) + "," + csv(tr.target.statisticType) + "," + tr.target.step + "," +
                        fmt(tr.simulated) + "," + fmt(tr.target.observedValue) + "," + fmt(tr.target.weight) + "," +
                        fmt(tr.target.scale) + "," + fmt(tr.standardizedResidual) + "," + fmt(tr.contribution) + "," +
                        fmt(share) + "," + tr.valid + "," + csv(tr.invalidReason) + "\n");
            }
        }
    }

    static final class Summary {
        int totalDraws, validRuns, extinctionCount, invalidCount, exceptionCount, acceptanceCount;
        double minDistance = Double.POSITIVE_INFINITY, medianDistance = Double.NaN, epsilon, acceptanceFraction;
    }

    static Summary summarize(List<DrawResult> results, double eps) {
        Summary s = new Summary();
        s.totalDraws = results.size();
        s.epsilon = eps;
        ArrayList<Double> finite = new ArrayList<>();
        for (DrawResult r : results) {
            if ("VALID_FINITE".equals(r.outcomeStatus)) s.validRuns++;
            else if ("TUMOR_EXTINCTION".equals(r.outcomeStatus)) s.extinctionCount++;
            else if ("MODEL_EXCEPTION".equals(r.outcomeStatus)) s.exceptionCount++;
            else s.invalidCount++;
            if (r.distance != null && Double.isFinite(r.distance.distance)) finite.add(r.distance.distance);
            if (r.accepted) s.acceptanceCount++;
        }
        finite.sort(Double::compare);
        if (!finite.isEmpty()) {
            s.minDistance = finite.get(0);
            s.medianDistance = finite.get(finite.size() / 2);
        }
        s.acceptanceFraction = s.totalDraws > 0 ? (double) s.acceptanceCount / s.totalDraws : 0.0;
        return s;
    }

    static void writeDistanceSummary(Path file, Summary s) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            w.write("total_draws,valid_runs,extinction_count,invalid_count,exception_count,minimum_distance,median_distance,selected_epsilon,acceptance_count,acceptance_fraction\n");
            w.write(s.totalDraws + "," + s.validRuns + "," + s.extinctionCount + "," + s.invalidCount + "," +
                    s.exceptionCount + "," + fmt(s.minDistance) + "," + fmt(s.medianDistance) + "," +
                    fmt(s.epsilon) + "," + s.acceptanceCount + "," + fmt(s.acceptanceFraction) + "\n");
        }
    }

    static void writeManifest(Config c, CalibrationProfile profile, String status, int validRuns, int accepted, double eps, long runtimeMillis) throws Exception {
        Files.writeString(c.outputDir.resolve("run_manifest.json"),
                resolvedConfigJson(c, profile, status, validRuns, accepted, eps, runtimeMillis), StandardCharsets.UTF_8);
    }

    static String resolvedConfigJson(Config c, CalibrationProfile profile, String status, int validRuns, int accepted, double eps, long runtimeMillis) throws Exception {
        String targetHash = CalibrationFreeze.sha256(CalibrationTarget.canonicalCsv().getBytes(StandardCharsets.UTF_8));
        String profileHash = CalibrationFreeze.sha256(profile.canonicalCsv().getBytes(StandardCharsets.UTF_8));
        StringBuilder b = new StringBuilder();
        b.append("{\n");
        b.append("  \"repository_commit\": ").append(json(CalibrationFreeze.git("rev-parse", "HEAD"))).append(",\n");
        b.append("  \"working_tree_dirty\": ").append(!CalibrationFreeze.git("status", "--porcelain").isEmpty()).append(",\n");
        b.append("  \"profile\": ").append(json(profile.name())).append(",\n");
        b.append("  \"target_profile\": \"current_abc_targets\",\n");
        b.append("  \"master_seed\": ").append(c.seed).append(",\n");
        b.append("  \"draws\": ").append(c.draws).append(",\n");
        b.append("  \"epsilon_mode\": ").append(json(c.epsilon > 0.0 ? "fixed" : "quantile")).append(",\n");
        b.append("  \"epsilon\": ").append(fmt(c.epsilon)).append(",\n");
        b.append("  \"selected_epsilon\": ").append(fmt(eps)).append(",\n");
        b.append("  \"quantile\": ").append(fmt(c.quantile)).append(",\n");
        b.append("  \"init_population\": ").append(c.initPop).append(",\n");
        b.append("  \"max_step\": ").append(c.maxStep).append(",\n");
        b.append("  \"start_time\": ").append(json(Instant.now().toString())).append(",\n");
        b.append("  \"completion_status\": ").append(json(status)).append(",\n");
        b.append("  \"valid_runs\": ").append(validRuns).append(",\n");
        b.append("  \"accepted_runs\": ").append(accepted).append(",\n");
        b.append("  \"runtime_ms\": ").append(runtimeMillis).append(",\n");
        b.append("  \"java_version\": ").append(json(System.getProperty("java.version"))).append(",\n");
        b.append("  \"operating_system\": ").append(json(System.getProperty("os.name") + " " + System.getProperty("os.version"))).append(",\n");
        b.append("  \"target_hash\": ").append(json(targetHash)).append(",\n");
        b.append("  \"parameter_freeze_hash\": ").append(json(profileHash)).append(",\n");
        b.append("  \"source_hashes\": {\n");
        String[] files = {"OnLatticeExample/ExampleGrid.java", "OnLatticeExample/ModelParameters.java", "OnLatticeExample/ABCRejection.java", "OnLatticeExample/CalibrationProfile.java", "OnLatticeExample/CalibrationTarget.java"};
        for (int i = 0; i < files.length; i++) {
            b.append("    ").append(json(files[i])).append(": ").append(json(CalibrationFreeze.fileSha(Path.of(files[i])))).append(i + 1 < files.length ? "," : "").append("\n");
        }
        b.append("  }\n");
        b.append("}\n");
        return b.toString();
    }

    static long proposalSeed(long master, int drawId) {
        return MorrisSensitivitySweep.mix64(master ^ 0x41424350524f504fL ^ (0x9e3779b97f4a7c15L * (drawId + 1L)));
    }

    static long simulationSeed(long master, int drawId) {
        return MorrisSensitivitySweep.mix64(master ^ 0x41424353494d5345L ^ (0xbf58476d1ce4e5b9L * (drawId + 1L)));
    }

    static String timestamp() {
        return java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(java.time.ZoneOffset.UTC).format(Instant.now());
    }

    static int[][] deepCopy(int[][] x) {
        if (x == null) return new int[0][];
        int[][] y = new int[x.length][];
        for (int i = 0; i < x.length; i++) y[i] = x[i] == null ? new int[0] : x[i].clone();
        return y;
    }
    static double[][] deepCopy(double[][] x) {
        if (x == null) return new double[0][];
        double[][] y = new double[x.length][];
        for (int i = 0; i < x.length; i++) y[i] = x[i] == null ? new double[0] : x[i].clone();
        return y;
    }

    static String fixedSnapshotHash(CalibrationProfile profile, ModelParameters p) throws Exception {
        if (p == null) return "";
        StringBuilder b = new StringBuilder();
        for (ModelParameters.Definition d : ModelParameters.screenedDefinitions()) {
            if (!profile.includes(d.name)) b.append(d.name).append('=').append(fmt(p.get(d.name))).append('\n');
        }
        return sha256(b.toString().getBytes(StandardCharsets.UTF_8));
    }

    static String sha256(byte[] data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(data);
        StringBuilder b = new StringBuilder();
        for (byte x : hash) b.append(String.format(Locale.US, "%02x", x & 0xff));
        return b.toString();
    }

    static void require(boolean ok, String message) {
        if (!ok) throw new IllegalStateException(message);
    }

    static String fmt(double x) {
        if (!Double.isFinite(x)) return Double.toString(x);
        if (x == Math.rint(x) && Math.abs(x) < 1e15) return Long.toString((long) x);
        return String.format(Locale.US, "%.12g", x);
    }

    static String csv(String s) {
        if (s == null) return "\"\"";
        return "\"" + s.replace("\"", "\"\"").replace("\r", " ").replace("\n", " ") + "\"";
    }

    static String json(String s) {
        if (s == null) return "null";
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "\"";
    }
}
