package OnLatticeExample;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Writes and verifies the machine-readable calibration freeze package. */
public final class CalibrationFreeze {
    static final String FREEZE_SCHEMA_VERSION = "tnbc-abm-calibration-freeze-v1";
    static final Path DEFAULT_DIR = Path.of("calibration", "freeze");

    public static void main(String[] args) throws Exception {
        Path dir = args.length > 0 ? Path.of(args[0]) : DEFAULT_DIR;
        writeFreeze(dir);
        System.out.println("wrote calibration freeze package: " + dir.toAbsolutePath());
    }

    public static void writeFreeze(Path dir) throws Exception {
        Files.createDirectories(dir);
        writeModelFreeze(dir.resolve("model_freeze.json"), "core4");
        writeCalibrationParameters(dir.resolve("calibration_parameters.csv"));
        writeCalibrationTargets(dir.resolve("calibration_targets.csv"));
        writeFixedParameterSnapshot(dir.resolve("fixed_parameter_snapshot.csv"), CalibrationProfile.core4(), 25);
        writeReadme(dir.resolve("README.md"));
    }

    public static void verifyFreeze(Path dir) throws Exception {
        if (!Files.isDirectory(dir)) throw new IllegalStateException("freeze directory missing: " + dir);
        String currentTargetsHash = sha256(CalibrationTarget.canonicalCsv().getBytes(StandardCharsets.UTF_8));
        String currentCoreHash = sha256(CalibrationProfile.core4().canonicalCsv().getBytes(StandardCharsets.UTF_8));
        String json = Files.readString(dir.resolve("model_freeze.json"), StandardCharsets.UTF_8);
        require(json.contains("\"target_profile_hash\": \"" + currentTargetsHash + "\""),
                "stale target profile hash in model_freeze.json");
        require(json.contains("\"parameter_profile_hash\": \"" + currentCoreHash + "\""),
                "stale core4 parameter profile hash in model_freeze.json");
        require(json.contains("\"QuadratEndothelialOn.txt\": \"" + fileSha(Path.of("QuadratEndothelialOn.txt")) + "\""),
                "stale endothelial coordinate hash in model_freeze.json");
        require(json.contains("\"QuadratStrOn.txt\": \"" + fileSha(Path.of("QuadratStrOn.txt")) + "\""),
                "stale fibroblast coordinate hash in model_freeze.json");
    }

    static void writeModelFreeze(Path file, String profileName) throws Exception {
        CalibrationProfile profile = CalibrationProfile.byName(profileName);
        LinkedHashMap<String, String> sourceHashes = new LinkedHashMap<>();
        for (String f : new String[]{
                "OnLatticeExample/ExampleGrid.java",
                "OnLatticeExample/ModelParameters.java",
                "OnLatticeExample/ABCRejection.java",
                "OnLatticeExample/CalibrationProfile.java",
                "OnLatticeExample/CalibrationTarget.java"}) {
            sourceHashes.put(f, fileSha(Path.of(f)));
        }
        LinkedHashMap<String, String> coordHashes = new LinkedHashMap<>();
        coordHashes.put("QuadratEndothelialOn.txt", fileSha(Path.of("QuadratEndothelialOn.txt")));
        coordHashes.put("QuadratStrOn.txt", fileSha(Path.of("QuadratStrOn.txt")));
        String commit = git("rev-parse", "HEAD");
        String branch = git("rev-parse", "--abbrev-ref", "HEAD");
        boolean dirty = !git("status", "--porcelain").isEmpty();
        try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            w.write("{\n");
            field(w, "schema_version", FREEZE_SCHEMA_VERSION, true);
            field(w, "created_at_utc", Instant.now().toString(), true);
            field(w, "repository_commit_sha", commit, true);
            field(w, "branch", branch, true);
            w.write("  \"working_tree_dirty\": " + dirty + ",\n");
            mapField(w, "source_file_hashes", sourceHashes, true);
            mapField(w, "coordinate_file_hashes", coordHashes, true);
            field(w, "model_class", "OnLatticeExample.ExampleGrid", true);
            field(w, "calibration_entry_point", "OnLatticeExample.ABCRejection", true);
            w.write("  \"grid_dimensions\": {\"width\": 100, \"height\": 100},\n");
            w.write("  \"simulation_horizon_steps\": 1440,\n");
            w.write("  \"snapshot_steps\": [0, 480, 960, 1440],\n");
            w.write("  \"initial_tumor_count_default\": 25,\n");
            w.write("  \"initial_endothelial_coordinates\": 237,\n");
            w.write("  \"initial_fibroblast_coordinates\": 142,\n");
            field(w, "baseline_seed_policy", "ABC run seeds are deterministic functions of master seed and draw index; each simulation gets a fresh ExampleGrid and HAL Rand", true);
            field(w, "update_semantics", "ShuffleAgents(rng), randomized asynchronous per-step update, Von Neumann movement/division hood", true);
            field(w, "treatment_status", "untreated through maxStep 1440; chemotherapy branch starts after step 2898", true);
            field(w, "parameter_profile_name", profile.name(), true);
            field(w, "parameter_profile_hash", sha256(profile.canonicalCsv().getBytes(StandardCharsets.UTF_8)), true);
            field(w, "target_profile_name", "current_abc_targets", true);
            field(w, "target_profile_hash", sha256(CalibrationTarget.canonicalCsv().getBytes(StandardCharsets.UTF_8)), true);
            field(w, "distance_function_name", "weighted_scaled_euclidean_with_infinite_final_tumor_extinction_and_residual3_missing_stroma", true);
            field(w, "result_provenance", "Morris evidence from results/morris-pilot-10 and results/morris-primary-20; freeze generated from Java definitions", false);
            w.write("}\n");
        }
    }

    static void writeCalibrationParameters(Path file) throws IOException {
        Map<String, Evidence> evidence = readEvidence();
        try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            w.write("profile,parameter,biological category,role,inferred or fixed,baseline,lower bound,upper bound,transformation,prior distribution,evidence source,Morris primary rank,confirmed rank,confirmed SNR,decision,justification,confounding note,mentor approval status\n");
            writeProfileRows(w, CalibrationProfile.core4(), evidence);
            writeProfileRows(w, CalibrationProfile.legacy12(), evidence);
        }
    }

    static void writeProfileRows(BufferedWriter w, CalibrationProfile profile, Map<String, Evidence> evidence) throws IOException {
        for (CalibrationProfile.Parameter p : profile.parameters()) {
            Evidence e = evidence.getOrDefault(p.name, Evidence.empty());
            ModelParameters.Definition d = p.definition;
            w.write(csv(profile.name()) + "," + csv(p.name) + "," + csv(d.group) + "," + csv(p.role) + "," +
                    csv(p.role.contains("fixed") ? "fixed" : "inferred") + "," + fmt(d.baseline) + "," +
                    fmt(d.lower) + "," + fmt(d.upper) + "," + csv(p.samplingTransform.name().toLowerCase(Locale.ROOT)) + "," +
                    csv(p.priorDistribution) + "," + csv("GLOBAL_PARAMETER_REGISTRY.csv; FINAL_PARAMETER_DECISION_TABLE.csv") + "," +
                    csv(e.primaryRank) + "," + csv(e.confirmedRank) + "," + csv(e.confirmedSnr) + "," +
                    csv(p.decision) + "," + csv(p.justification) + "," + csv(d.rangeRisk) + "," +
                    csv(p.mentorApprovalStatus) + "\n");
        }
    }

    static void writeCalibrationTargets(Path file) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            w.write("target ID,statistic type,step,interpreted biological time,observed value,weight,scale,source or proxy,transformation,missing-value behavior,calibration or validation role,provenance confidence,unresolved concern\n");
            for (CalibrationTarget t : CalibrationTarget.currentTargets()) {
                w.write(csv(t.id) + "," + csv(t.statisticType) + "," + t.step + "," + csv(t.interpretedTime) + "," +
                        fmt(t.observedValue) + "," + fmt(t.weight) + "," + fmt(t.scale) + "," + csv(t.sourceOrProxy) + "," +
                        csv(t.transformation) + "," + csv(t.missingValueBehavior) + "," + csv(t.role) + "," +
                        csv(t.provenanceConfidence) + "," + csv(t.unresolvedConcern) + "\n");
            }
        }
    }

    static void writeFixedParameterSnapshot(Path file, CalibrationProfile profile, int initPop) throws IOException {
        ModelParameters p = ModelParameters.currentBaseline(initPop);
        try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            w.write("profile,parameter,baseline_value,lower_bound,upper_bound,transformation,current_status,active_under_baseline,expected_affected_outputs,fixed_reason\n");
            for (ModelParameters.Definition d : ModelParameters.screenedDefinitions()) {
                if (profile.includes(d.name)) continue;
                w.write(csv(profile.name()) + "," + csv(d.name) + "," + fmt(p.get(d.name)) + "," +
                        fmt(d.lower) + "," + fmt(d.upper) + "," + csv(d.transform.name().toLowerCase(Locale.ROOT)) + "," +
                        csv(d.status) + "," + csv(d.activeAtBaseline) + "," + csv(d.expectedOutputs) + "," +
                        csv("Fixed at current executable baseline in reduced profile; sensitivity does not imply identifiability") + "\n");
            }
        }
    }

    static void writeReadme(Path file) throws IOException {
        String text = "# Calibration Freeze Package\n\n" +
                "This directory freezes the untreated TNBC ABM calibration definitions for the post-Morris Phase 1 workflow.\n\n" +
                "- `model_freeze.json` records source and coordinate hashes, commit, branch, model entry point, horizon, snapshots, and profile/target hashes.\n" +
                "- `calibration_parameters.csv` records both `core4` and `legacy12` profile definitions.\n" +
                "- `calibration_targets.csv` records the named target definitions used by the distance function.\n" +
                "- `fixed_parameter_snapshot.csv` records screened executable parameters fixed at baseline under `core4`.\n\n" +
                "Regenerate after intentional definition changes with:\n\n" +
                "```bash\njavac -cp .:HAL-freq.jar:lwjgl.jar OnLatticeExample/*.java\njava -cp .:HAL-freq.jar:lwjgl.jar OnLatticeExample.CalibrationFreeze calibration/freeze\n```\n\n" +
                "Verify with:\n\n" +
                "```bash\njava -cp .:HAL-freq.jar:lwjgl.jar OnLatticeExample.CalibrationQualityControl\n```\n";
        Files.writeString(file, text, StandardCharsets.UTF_8);
    }

    static Map<String, Evidence> readEvidence() {
        LinkedHashMap<String, Evidence> out = new LinkedHashMap<>();
        Path file = Path.of("results", "morris-primary-20", "FINAL_PARAMETER_DECISION_TABLE.csv");
        if (!Files.isRegularFile(file)) return out;
        try (BufferedReader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String header = r.readLine();
            if (header == null) return out;
            List<String> cols = parseCsv(header);
            int p = cols.indexOf("parameter"), pr = cols.indexOf("primary_mu_star_rank"),
                    cr = cols.indexOf("confirmed_mu_star_rank"), snr = cols.indexOf("stochastic_signal_to_noise");
            String line;
            while ((line = r.readLine()) != null) {
                List<String> row = parseCsv(line);
                if (row.size() <= p) continue;
                out.put(row.get(p), new Evidence(get(row, pr), get(row, cr), get(row, snr)));
            }
        } catch (IOException ignored) {
        }
        return out;
    }

    static List<String> parseCsv(String line) {
        ArrayList<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean q = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (q) {
                if (c == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') { cur.append('"'); i++; }
                else if (c == '"') q = false;
                else cur.append(c);
            } else if (c == '"') q = true;
            else if (c == ',') { out.add(cur.toString()); cur.setLength(0); }
            else cur.append(c);
        }
        out.add(cur.toString());
        return out;
    }

    static String get(List<String> row, int idx) { return idx >= 0 && idx < row.size() ? row.get(idx) : ""; }

    static final class Evidence {
        final String primaryRank, confirmedRank, confirmedSnr;
        Evidence(String primaryRank, String confirmedRank, String confirmedSnr) {
            this.primaryRank = primaryRank; this.confirmedRank = confirmedRank; this.confirmedSnr = confirmedSnr;
        }
        static Evidence empty() { return new Evidence("", "", ""); }
    }

    static void field(BufferedWriter w, String key, String value, boolean comma) throws IOException {
        w.write("  \"" + key + "\": " + json(value) + (comma ? "," : "") + "\n");
    }

    static void mapField(BufferedWriter w, String key, LinkedHashMap<String, String> map, boolean comma) throws IOException {
        w.write("  \"" + key + "\": {\n");
        int i = 0;
        for (Map.Entry<String, String> e : map.entrySet()) {
            w.write("    " + json(e.getKey()) + ": " + json(e.getValue()) + (++i < map.size() ? "," : "") + "\n");
        }
        w.write("  }" + (comma ? "," : "") + "\n");
    }

    static String git(String... args) {
        try {
            ArrayList<String> cmd = new ArrayList<>();
            cmd.add("git");
            for (String a : args) cmd.add(a);
            Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            byte[] b = p.getInputStream().readAllBytes();
            p.waitFor();
            return new String(b, StandardCharsets.UTF_8).trim();
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    static String fileSha(Path file) throws Exception {
        return sha256(Files.readAllBytes(file));
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
        StringBuilder b = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\\') b.append('\\').append(c);
            else if (c == '\n') b.append("\\n");
            else if (c == '\r') b.append("\\r");
            else if (c == '\t') b.append("\\t");
            else b.append(c);
        }
        return b.append('"').toString();
    }
}
