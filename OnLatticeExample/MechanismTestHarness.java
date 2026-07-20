package OnLatticeExample;

import HAL.Rand;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Paired low/high mechanism checks for the TNBC lung-metastasis ABM.
 *
 * This harness intentionally does not modify ExampleGrid. Each tested parameter
 * is changed on a fresh grid, and the low/high pair is run with identical seeds.
 */
public class MechanismTestHarness {
    static final double STEPS_PER_WEEK = 480.0;
    static final double STEPS_PER_DAY = STEPS_PER_WEEK / 7.0;
    static final double HOURS_PER_STEP = 24.0 / STEPS_PER_DAY;

    static final double[] BASE_THETA = {
            0.0025997991807979125, 0.018574516658068727,
            0.034429916846495774, 0.1188029560408925,
            0.006621397759285578, 0.003061084528053752,
            0.4276094684821006, 0.05099982197121976,
            0.023591782410718555, 0.029805746764728917,
            0.04182908819427277, 0.05947751372653652
    };

    interface Setter {
        void apply(ExampleGrid g, double[] theta, RunConfig cfg, double value);
    }

    static class RunConfig {
        int initPop = 25;
        int maxStep = 1440;
    }

    static class ParamCase {
        final String name, group, expected;
        final double low, high;
        final Setter setter;

        ParamCase(String name, String group, double low, double high, String expected, Setter setter) {
            this.name = name;
            this.group = group;
            this.low = low;
            this.high = high;
            this.expected = expected;
            this.setter = setter;
        }
    }

    static class Metrics {
        double tumorLog, fibroLog, jnkFrac, ecFrac, macFrac;
        double tumorTotal, fibroTotal, ecTotal, macTotal;
        double jnkRimFrac;
        int macDivTry, macDivFail;
    }

    static class Summary {
        double tumorLog, fibroLog, jnkFrac, ecFrac, macFrac;
        double tumorTotal, fibroTotal, ecTotal, macTotal;
        double jnkRimFrac, macDivFailFrac;
    }

    static double[] thetaCopy() {
        double[] out = new double[BASE_THETA.length];
        System.arraycopy(BASE_THETA, 0, out, 0, BASE_THETA.length);
        return out;
    }

    static Metrics runOne(ParamCase pc, double value, long seed, int initPop, int maxStep) throws IOException {
        ExampleGrid g = new ExampleGrid(100, 100);
        g.rng = new Rand(seed);
        RunConfig cfg = new RunConfig();
        cfg.initPop = initPop;
        cfg.maxStep = maxStep;
        double[] theta = thetaCopy();
        pc.setter.apply(g, theta, cfg, value);

        int[][] snaps = g.RunHeadless(theta, cfg.initPop, cfg.maxStep);
        int[] start = snaps[0];
        int[] end = snaps[snaps.length - 1];

        Metrics m = new Metrics();
        int t0 = start[0] + start[1];
        int f0 = start[6] + start[7];
        int t = end[0] + end[1];
        int f = end[6] + end[7];
        int ec = end[2] + end[3];
        int mac = end[4] + end[5];
        m.tumorTotal = t;
        m.fibroTotal = f;
        m.ecTotal = ec;
        m.macTotal = mac;
        m.tumorLog = (t0 > 0 && t > 0) ? Math.log10((double) t / t0) : Double.NaN;
        m.fibroLog = (f0 > 0 && f > 0) ? Math.log10((double) f / f0) : Double.NaN;
        m.jnkFrac = t > 0 ? (double) end[0] / t : Double.NaN;
        m.ecFrac = ec > 0 ? (double) end[2] / ec : Double.NaN;
        m.macFrac = mac > 0 ? (double) end[4] / mac : Double.NaN;
        if (g.lastRimCore != null && g.lastRimCore.length > 0) {
            int[] rc = g.lastRimCore[g.lastRimCore.length - 1];
            int rcTot = rc[0] + rc[1];
            m.jnkRimFrac = rcTot > 0 ? (double) rc[0] / rcTot : Double.NaN;
        } else {
            m.jnkRimFrac = Double.NaN;
        }
        m.macDivTry = g.macDivTry;
        m.macDivFail = g.macDivFail;
        return m;
    }

    static Summary mean(List<Metrics> xs) {
        Summary s = new Summary();
        for (Metrics m : xs) {
            s.tumorLog += finite(m.tumorLog);
            s.fibroLog += finite(m.fibroLog);
            s.jnkFrac += finite(m.jnkFrac);
            s.ecFrac += finite(m.ecFrac);
            s.macFrac += finite(m.macFrac);
            s.tumorTotal += m.tumorTotal;
            s.fibroTotal += m.fibroTotal;
            s.ecTotal += m.ecTotal;
            s.macTotal += m.macTotal;
            s.jnkRimFrac += finite(m.jnkRimFrac);
            s.macDivFailFrac += m.macDivTry > 0 ? (double) m.macDivFail / m.macDivTry : 0.0;
        }
        int n = xs.size();
        s.tumorLog /= n;
        s.fibroLog /= n;
        s.jnkFrac /= n;
        s.ecFrac /= n;
        s.macFrac /= n;
        s.tumorTotal /= n;
        s.fibroTotal /= n;
        s.ecTotal /= n;
        s.macTotal /= n;
        s.jnkRimFrac /= n;
        s.macDivFailFrac /= n;
        return s;
    }

    static double finite(double x) {
        return Double.isFinite(x) ? x : 0.0;
    }

    static boolean changed(Summary lo, Summary hi) {
        return Math.abs(hi.tumorLog - lo.tumorLog) >= 0.10
                || Math.abs(hi.fibroLog - lo.fibroLog) >= 0.10
                || Math.abs(hi.jnkFrac - lo.jnkFrac) >= 0.05
                || Math.abs(hi.ecFrac - lo.ecFrac) >= 0.05
                || Math.abs(hi.macFrac - lo.macFrac) >= 0.05
                || rel(hi.tumorTotal, lo.tumorTotal) >= 0.20
                || rel(hi.fibroTotal, lo.fibroTotal) >= 0.20
                || rel(hi.ecTotal, lo.ecTotal) >= 0.20
                || rel(hi.macTotal, lo.macTotal) >= 0.20;
    }

    static double rel(double a, double b) {
        double den = Math.max(1.0, Math.abs(b));
        return Math.abs(a - b) / den;
    }

    static String fmt(double x) {
        if (!Double.isFinite(x)) return "NaN";
        return String.format(java.util.Locale.US, "%.6g", x);
    }

    static List<ParamCase> cases() {
        List<ParamCase> out = new ArrayList<>();
        out.add(new ParamCase("netN", "tumor", 0.0015, 0.0050, "tumorLog", (g, th, cfg, v) -> th[0] = v));
        out.add(new ParamCase("dieProbN", "tumor", 0.008, 0.025, "tumorLog/jnkFrac", (g, th, cfg, v) -> th[1] = v));
        out.add(new ParamCase("pOnMax", "jnkSwitch", 0.01, 0.10, "jnkFrac", (g, th, cfg, v) -> th[2] = v));
        out.add(new ParamCase("pOffMax", "jnkSwitch", 0.01, 0.20, "jnkFrac", (g, th, cfg, v) -> th[3] = v));
        out.add(new ParamCase("divProbP", "tumor", 0.005, 0.03, "tumorLog/jnkFrac", (g, th, cfg, v) -> th[4] = v));
        out.add(new ParamCase("dieProbP", "tumor", 0.001, 0.004, "tumorLog/jnkFrac", (g, th, cfg, v) -> th[5] = v));
        out.add(new ParamCase("cafDivBoost", "cafTumor", 0.0, 1.0, "tumorLog/jnkRim", (g, th, cfg, v) -> th[6] = v));
        out.add(new ParamCase("ecSurvival", "ecTumor", 0.0, 0.3, "tumorLog", (g, th, cfg, v) -> th[7] = v));
        out.add(new ParamCase("activProbF", "fibroblast", 0.001, 0.05, "fibroLog/tumorLog", (g, th, cfg, v) -> th[8] = v));
        out.add(new ParamCase("divProbFP", "fibroblast", 0.018, 0.038, "fibroLog", (g, th, cfg, v) -> th[9] = v));
        out.add(new ParamCase("activProbM", "macrophage", 0.02, 0.08, "macFrac/ecFrac", (g, th, cfg, v) -> th[10] = v));
        out.add(new ParamCase("activProbE", "endothelial", 0.005, 0.08, "ecFrac/tumorLog", (g, th, cfg, v) -> th[11] = v));

        out.add(new ParamCase("recruitBias", "macrophage", 0.0, 0.20, "macFrac/ecFrac", (g, th, cfg, v) -> g.recruitBias = v));
        out.add(new ParamCase("lambdaCAF", "cafTumor", 0.5, 5.0, "jnkFrac/tumorLog", (g, th, cfg, v) -> g.lambdaCAF = v));
        out.add(new ParamCase("stressStrength", "jnkSwitch", 0.0, 3.0, "jnkFrac", (g, th, cfg, v) -> g.stressStrength = v));
        out.add(new ParamCase("lambdaStress", "jnkSwitch", 0.5, 5.0, "jnkFrac", (g, th, cfg, v) -> g.lambdaStress = v));
        out.add(new ParamCase("migrProbP", "tumorMigration", 0.0, 0.30, "tumorLog/jnkFrac", (g, th, cfg, v) -> g.migrProbP = v));
        out.add(new ParamCase("migrProbN", "tumorMigration", 0.0, 0.10, "tumorLog/jnkFrac", (g, th, cfg, v) -> g.migrProbN = v));
        out.add(new ParamCase("divProbFN", "fibroblast", 0.0, 0.01, "fibroLog", (g, th, cfg, v) -> g.divProbFN = v));
        out.add(new ParamCase("dieProbFN", "fibroblast", 0.001, 0.008, "fibroLog", (g, th, cfg, v) -> g.dieProbFN = v));
        out.add(new ParamCase("dieProbFP", "fibroblast", 0.001, 0.012, "fibroLog", (g, th, cfg, v) -> g.dieProbFP = v));
        out.add(new ParamCase("migrProbF", "fibroblast", 0.0, 0.20, "noneCurrently", (g, th, cfg, v) -> g.migrProbF = v));
        out.add(new ParamCase("divProbMN", "macrophage", 0.001, 0.01, "macTotal/macFrac", (g, th, cfg, v) -> g.divProbMN = v));
        out.add(new ParamCase("dieProbMN", "macrophage", 0.001, 0.01, "macTotal/macFrac", (g, th, cfg, v) -> g.dieProbMN = v));
        out.add(new ParamCase("divProbMP", "macrophage", 0.001, 0.03, "macTotal/macFrac", (g, th, cfg, v) -> g.divProbMP = v));
        out.add(new ParamCase("dieProbMP", "macrophage", 0.001, 0.03, "macTotal/macFrac", (g, th, cfg, v) -> g.dieProbMP = v));
        out.add(new ParamCase("activProbMP", "macrophage", 0.0, 0.05, "noneCurrently", (g, th, cfg, v) -> g.activProbMP = v));
        out.add(new ParamCase("migrProbM", "macrophage", 0.1, 0.95, "macFrac/ecFrac", (g, th, cfg, v) -> g.migrProbM = v));
        out.add(new ParamCase("divProbEN", "endothelial", 0.0, 0.01, "noneCurrently", (g, th, cfg, v) -> g.divProbEN = v));
        out.add(new ParamCase("dieProbEN", "endothelial", 0.001, 0.01, "ecTotal/ecFrac", (g, th, cfg, v) -> g.dieProbEN = v));
        out.add(new ParamCase("divProbEP", "endothelial", 0.001, 0.02, "ecTotal/ecFrac", (g, th, cfg, v) -> g.divProbEP = v));
        out.add(new ParamCase("dieProbEP", "endothelial", 0.001, 0.02, "ecTotal/ecFrac", (g, th, cfg, v) -> g.dieProbEP = v));
        out.add(new ParamCase("deactProbE", "endothelial", 0.0, 0.05, "ecFrac", (g, th, cfg, v) -> g.deactProbE = v));
        out.add(new ParamCase("migrProbE", "endothelial", 0.0, 0.20, "noneCurrently", (g, th, cfg, v) -> g.migrProbE = v));
        out.add(new ParamCase("divProbL", "lung", 0.0, 0.01, "noneCurrently", (g, th, cfg, v) -> g.divProbL = v));
        out.add(new ParamCase("dieProbL", "lung", 0.0005, 0.01, "jnkFrac/tumorLog", (g, th, cfg, v) -> g.dieProbL = v));
        out.add(new ParamCase("clusterRadius", "initialGeometry", 2.0, 8.0, "tumorLog/jnkFrac", (g, th, cfg, v) -> g.clusterRadius = (int) Math.round(v)));
        out.add(new ParamCase("initPop", "initialGeometry", 10.0, 50.0, "tumorLog/establishment", (g, th, cfg, v) -> cfg.initPop = (int) Math.round(v)));
        return out;
    }

    public static void main(String[] args) throws IOException {
        int seedCount = args.length > 0 ? Integer.parseInt(args[0]) : 5;
        int initPop = args.length > 1 ? Integer.parseInt(args[1]) : 25;
        int maxStep = args.length > 2 ? Integer.parseInt(args[2]) : 1440;
        long baseSeed = args.length > 3 ? Long.parseLong(args[3]) : 9001L;
        String outPath = args.length > 4 ? args[4] : "mechanism_test_results.csv";

        try (FileWriter w = new FileWriter(outPath)) {
            w.write("parameter,group,expected_output,low,high,seeds,changed,"
                    + "low_tumorLog,high_tumorLog,delta_tumorLog,"
                    + "low_fibroLog,high_fibroLog,delta_fibroLog,"
                    + "low_jnkFrac,high_jnkFrac,delta_jnkFrac,"
                    + "low_ecFrac,high_ecFrac,delta_ecFrac,"
                    + "low_macFrac,high_macFrac,delta_macFrac,"
                    + "low_tumorTotal,high_tumorTotal,delta_tumorTotal,"
                    + "low_fibroTotal,high_fibroTotal,delta_fibroTotal,"
                    + "low_ecTotal,high_ecTotal,delta_ecTotal,"
                    + "low_macTotal,high_macTotal,delta_macTotal,"
                    + "low_jnkRimFrac,high_jnkRimFrac,delta_jnkRimFrac,"
                    + "low_macDivFailFrac,high_macDivFailFrac,delta_macDivFailFrac\n");

            for (ParamCase pc : cases()) {
                List<Metrics> lows = new ArrayList<>();
                List<Metrics> highs = new ArrayList<>();
                for (int i = 0; i < seedCount; i++) {
                    long seed = baseSeed + i;
                    lows.add(runOne(pc, pc.low, seed, initPop, maxStep));
                    highs.add(runOne(pc, pc.high, seed, initPop, maxStep));
                }
                Summary lo = mean(lows);
                Summary hi = mean(highs);
                boolean ch = changed(lo, hi);
                w.write(pc.name + "," + pc.group + "," + pc.expected + ","
                        + fmt(pc.low) + "," + fmt(pc.high) + "," + seedCount + "," + ch + ","
                        + row(lo.tumorLog, hi.tumorLog)
                        + row(lo.fibroLog, hi.fibroLog)
                        + row(lo.jnkFrac, hi.jnkFrac)
                        + row(lo.ecFrac, hi.ecFrac)
                        + row(lo.macFrac, hi.macFrac)
                        + row(lo.tumorTotal, hi.tumorTotal)
                        + row(lo.fibroTotal, hi.fibroTotal)
                        + row(lo.ecTotal, hi.ecTotal)
                        + row(lo.macTotal, hi.macTotal)
                        + row(lo.jnkRimFrac, hi.jnkRimFrac)
                        + rowLast(lo.macDivFailFrac, hi.macDivFailFrac));
            }
        }

        System.out.println("wrote " + outPath + " using " + seedCount
                + " paired seed(s), initPop=" + initPop + ", maxStep=" + maxStep
                + ", steps/week=" + STEPS_PER_WEEK + ", hours/step=" + HOURS_PER_STEP);
    }

    static String row(double lo, double hi) {
        return fmt(lo) + "," + fmt(hi) + "," + fmt(hi - lo) + ",";
    }

    static String rowLast(double lo, double hi) {
        return fmt(lo) + "," + fmt(hi) + "," + fmt(hi - lo) + "\n";
    }
}
