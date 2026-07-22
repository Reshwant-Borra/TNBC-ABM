package OnLatticeExample;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Immutable named ABC target definitions and distance utilities. */
public final class CalibrationTarget {
    public static final int[] SNAP = {0, 480, 960, 1440};

    public final String id;
    public final String statisticType;
    public final int step;
    public final String interpretedTime;
    public final double observedValue;
    public final double weight;
    public final double scale;
    public final String sourceOrProxy;
    public final String transformation;
    public final String missingValueBehavior;
    public final String role;
    public final String provenanceConfidence;
    public final String unresolvedConcern;

    private static final List<CalibrationTarget> CURRENT;

    static {
        ArrayList<CalibrationTarget> t = new ArrayList<>();
        t.add(new CalibrationTarget("abc_target_01_jnkp_s480", "jnkp", 480, "week 1 (7 days)",
                0.53, 1.0, 0.2, "IR18 JNK-positive tumour fraction", "fraction",
                "NaN only if tumour denominator is zero; final tumour extinction gives infinite distance",
                "calibration", "medium in-code / unresolved extraction", "Exact source extraction not present"));
        t.add(new CalibrationTarget("abc_target_02_jnkp_s1440", "jnkp", 1440, "week 3 (21 days)",
                0.14, 1.0, 0.2, "IR18 JNK-positive tumour fraction", "fraction",
                "NaN only if tumour denominator is zero; final tumour extinction gives infinite distance",
                "calibration", "medium in-code / unresolved extraction", "Exact source extraction not present"));
        t.add(new CalibrationTarget("abc_target_03_ec_s480", "ec", 480, "week 1 (7 days)",
                0.15, 0.5, 0.2, "H22 activated EC fraction", "fraction",
                "Missing EC denominator is penalized as residual 3.0", "calibration",
                "medium in-code / unresolved extraction", "Confirm measurement matches activated EC fraction"));
        t.add(new CalibrationTarget("abc_target_04_ec_s960", "ec", 960, "week 2 (14 days)",
                0.30, 0.5, 0.2, "H22 activated EC fraction", "fraction",
                "Missing EC denominator is penalized as residual 3.0", "calibration",
                "medium in-code / unresolved extraction", "Confirm measurement matches activated EC fraction"));
        t.add(new CalibrationTarget("abc_target_05_ec_s1440", "ec", 1440, "week 3 (21 days)",
                0.80, 1.0, 0.2, "H22 activated EC fraction", "fraction",
                "Missing EC denominator is penalized as residual 3.0", "calibration",
                "medium in-code / unresolved extraction", "Confirm measurement matches activated EC fraction"));
        t.add(new CalibrationTarget("abc_target_06_mac_s1440", "mac", 1440, "week 3 (21 days)",
                0.77, 1.0, 0.2, "H22 perivascular macrophage fraction proxy", "fraction",
                "Missing macrophage denominator is penalized as residual 3.0", "calibration",
                "proxy / unresolved", "Activated macrophage fraction may not equal perivascular macrophage fraction"));
        t.add(new CalibrationTarget("abc_target_07_fibro_s1440", "fibro", 1440, "week 3 (21 days)",
                1.10, 1.0, 0.27, "P20 fibroblast fold-change, 3D-to-2D adjusted", "log10 fold vs step 0",
                "Missing fibroblast denominator/log fold is penalized as residual 3.0", "calibration",
                "medium arithmetic / unresolved extraction", "Confirm original 3D value and 2D conversion"));
        t.add(new CalibrationTarget("abc_target_08_tumor_s480", "tumor", 480, "week 1 (7 days)",
                0.42, 1.0, 0.27, "MDA231-LM2 tumour fold-change, 3D-to-2D adjusted", "log10 fold vs step 0",
                "Final tumour extinction gives infinite distance", "calibration",
                "medium arithmetic / unresolved extraction", "Confirm original growth curve and 2D conversion"));
        t.add(new CalibrationTarget("abc_target_09_tumor_s960", "tumor", 960, "week 2 (14 days)",
                0.78, 1.0, 0.27, "MDA231-LM2 tumour fold-change, 3D-to-2D adjusted", "log10 fold vs step 0",
                "Final tumour extinction gives infinite distance", "calibration",
                "medium arithmetic / unresolved extraction", "Confirm original growth curve and 2D conversion"));
        t.add(new CalibrationTarget("abc_target_10_tumor_s1440", "tumor", 1440, "week 3 (21 days)",
                1.11, 1.0, 0.27, "MDA231-LM2 tumour fold-change, 3D-to-2D adjusted", "log10 fold vs step 0",
                "Final tumour extinction gives infinite distance", "calibration",
                "medium arithmetic / unresolved extraction", "Confirm original growth curve and 2D conversion"));
        CURRENT = Collections.unmodifiableList(t);
    }

    private CalibrationTarget(String id, String statisticType, int step, String interpretedTime,
                              double observedValue, double weight, double scale, String sourceOrProxy,
                              String transformation, String missingValueBehavior, String role,
                              String provenanceConfidence, String unresolvedConcern) {
        this.id = id;
        this.statisticType = statisticType;
        this.step = step;
        this.interpretedTime = interpretedTime;
        this.observedValue = observedValue;
        this.weight = weight;
        this.scale = scale;
        this.sourceOrProxy = sourceOrProxy;
        this.transformation = transformation;
        this.missingValueBehavior = missingValueBehavior;
        this.role = role;
        this.provenanceConfidence = provenanceConfidence;
        this.unresolvedConcern = unresolvedConcern;
    }

    public static List<CalibrationTarget> currentTargets() {
        return CURRENT;
    }

    public static int snapIdx(int step) {
        for (int i = 0; i < SNAP.length; i++) if (SNAP[i] == step) return i;
        return -1;
    }

    public static double stat(int[][] s, String type, int step) {
        int idx = snapIdx(step);
        if (idx < 0 || s == null || idx >= s.length || s[idx].length < 8) return Double.NaN;
        int[] c = s[idx];
        int tumP = c[0], tumN = c[1], ecA = c[2], ecI = c[3], macA = c[4], macI = c[5], fibA = c[6], fibI = c[7];
        switch (type) {
            case "jnkp":  { int total = tumP + tumN; return total > 0 ? (double) tumP / total : Double.NaN; }
            case "ec":    { int total = ecA + ecI;   return total > 0 ? (double) ecA / total : Double.NaN; }
            case "mac":   { int total = macA + macI; return total > 0 ? (double) macA / total : Double.NaN; }
            case "fibro": { int t0 = s[0][6] + s[0][7], t = fibA + fibI; return (t0 > 0 && t > 0) ? Math.log10((double) t / t0) : Double.NaN; }
            case "tumor": { int t0 = s[0][0] + s[0][1], t = tumP + tumN; return (t0 > 0 && t > 0) ? Math.log10((double) t / t0) : Double.NaN; }
            default: return Double.NaN;
        }
    }

    public static DistanceResult distance(int[][] snapshots) {
        validateTargets();
        if (snapshots == null || snapshots.length != SNAP.length) {
            throw new IllegalArgumentException("expected " + SNAP.length + " snapshots, got " + (snapshots == null ? 0 : snapshots.length));
        }
        if (snapshots[SNAP.length - 1][0] + snapshots[SNAP.length - 1][1] == 0) {
            return new DistanceResult(Double.POSITIVE_INFINITY, perTarget(snapshots), "TUMOR_EXTINCTION");
        }
        List<TargetResult> parts = perTarget(snapshots);
        double sum = 0.0;
        boolean invalid = false;
        for (TargetResult p : parts) {
            sum += p.contribution;
            invalid |= !p.valid;
        }
        return new DistanceResult(Math.sqrt(sum), parts, invalid ? "INVALID_TARGET_STATISTIC" : "VALID_FINITE");
    }

    public static List<TargetResult> perTarget(int[][] snapshots) {
        ArrayList<TargetResult> out = new ArrayList<>();
        for (CalibrationTarget t : CURRENT) {
            double sim = stat(snapshots, t.statisticType, t.step);
            boolean valid = Double.isFinite(sim);
            double residual = valid ? (sim - t.observedValue) / t.scale : 3.0;
            double contribution = t.weight * residual * residual;
            out.add(new TargetResult(t, sim, residual, contribution, valid,
                    valid ? "" : missingReason(snapshots, t)));
        }
        return out;
    }

    static String missingReason(int[][] snapshots, CalibrationTarget t) {
        int idx = snapIdx(t.step);
        if (idx < 0 || snapshots == null || idx >= snapshots.length || snapshots[idx].length < 8) return "MISSING_SNAPSHOT";
        int[] c = snapshots[idx];
        switch (t.statisticType) {
            case "jnkp":
            case "tumor": return c[0] + c[1] == 0 ? "TUMOR_DENOMINATOR_ZERO" : "TARGET_STAT_UNDEFINED";
            case "ec": return c[2] + c[3] == 0 ? "EC_POPULATION_ZERO" : "TARGET_STAT_UNDEFINED";
            case "mac": return c[4] + c[5] == 0 ? "MACROPHAGE_POPULATION_ZERO" : "TARGET_STAT_UNDEFINED";
            case "fibro": return c[6] + c[7] == 0 ? "FIBROBLAST_POPULATION_ZERO" : "TARGET_STAT_UNDEFINED";
            default: return "TARGET_STAT_UNDEFINED";
        }
    }

    public static void validateTargets() {
        java.util.HashSet<String> ids = new java.util.HashSet<>();
        for (CalibrationTarget t : CURRENT) {
            if (!ids.add(t.id)) throw new IllegalStateException("duplicate target ID: " + t.id);
            if (snapIdx(t.step) < 0) throw new IllegalStateException("target step is not in SNAP: " + t.id);
            if (!Double.isFinite(t.observedValue) || !Double.isFinite(t.weight) || !Double.isFinite(t.scale)) {
                throw new IllegalStateException("non-finite target definition: " + t.id);
            }
            if (t.scale <= 0.0) throw new IllegalStateException("target scale must be positive: " + t.id);
            if (t.weight < 0.0) throw new IllegalStateException("target weight must be nonnegative: " + t.id);
        }
    }

    public static String canonicalCsv() {
        StringBuilder b = new StringBuilder("target_id,statistic_type,step,observed_value,weight,scale\n");
        for (CalibrationTarget t : CURRENT) {
            b.append(t.id).append(',').append(t.statisticType).append(',').append(t.step).append(',')
                    .append(fmt(t.observedValue)).append(',').append(fmt(t.weight)).append(',').append(fmt(t.scale)).append('\n');
        }
        return b.toString();
    }

    static String fmt(double x) {
        if (!Double.isFinite(x)) return Double.toString(x);
        if (x == Math.rint(x) && Math.abs(x) < 1e15) return Long.toString((long) x);
        return String.format(Locale.US, "%.12g", x);
    }

    public static final class TargetResult {
        public final CalibrationTarget target;
        public final double simulated;
        public final double standardizedResidual;
        public final double contribution;
        public final boolean valid;
        public final String invalidReason;

        TargetResult(CalibrationTarget target, double simulated, double standardizedResidual,
                     double contribution, boolean valid, String invalidReason) {
            this.target = target;
            this.simulated = simulated;
            this.standardizedResidual = standardizedResidual;
            this.contribution = contribution;
            this.valid = valid;
            this.invalidReason = invalidReason;
        }
    }

    public static final class DistanceResult {
        public final double distance;
        public final List<TargetResult> targets;
        public final String statusHint;

        DistanceResult(double distance, List<TargetResult> targets, String statusHint) {
            this.distance = distance;
            this.targets = Collections.unmodifiableList(targets);
            this.statusHint = statusHint;
        }
    }
}
