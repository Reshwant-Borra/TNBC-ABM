package OnLatticeExample;

import HAL.Rand;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

/**
 * ABCRejection — a standalone, pure-Java Approximate Bayesian Computation
 * (rejection sampler) for the TNBC lung-metastasis ABM (ExampleGrid).
 *
 * No Python wrapper, no parameter files: it samples parameter sets from the
 * priors, runs the model in-process (ExampleGrid.RunHeadless), reduces the
 * output to summary statistics, computes a distance to the calibration targets,
 * and keeps the sets whose distance is below a tolerance epsilon.
 *
 * The four steps of rejection ABC are all visible below:
 *   1. PROPOSE  - draw theta ~ Uniform(LO, HI)                 [main loop]
 *   2. SIMULATE - snaps = model.RunHeadless(theta)             [main loop]
 *   3. COMPARE  - d = distance(snaps) vs the targets           [distance()]
 *   4. ACCEPT   - keep theta if d <= epsilon                   [end of main]
 *
 * Build:  javac -cp HAL-freq.jar ExampleGrid.java ABCRejection.java
 * Run:    java  -cp .:HAL-freq.jar ABCRejection [N] [epsilon] [quantile] [seed] [initPop]
 *           N        : number of prior draws          (default 1000)
 *           epsilon  : fixed tolerance; <0 => use the quantile instead (default -1)
 *           quantile : if epsilon<0, keep the closest fraction         (default 0.2)
 *           seed     : RNG seed for reproducibility                    (default 12345)
 *           initPop  : seeded tumour cells                             (default 25)
 *         (QuadratEndothelialOn.txt and QuadratStrOn.txt must be in the working dir.)
 *
 * NOTE on runtime: each draw is a full 2900-step simulation; large tumours are
 * much slower than ones that go extinct, so a 1000-draw pilot can take a while.
 */
public class ABCRejection {

    // ================================================================
    // 1) PRIORS — the 12 inferred parameters, Uniform(LO[i], HI[i]).
    //    Order MUST match ExampleGrid.RunHeadless(theta).
    // ================================================================
    // Tumour rate STRUCTURE (biology):  JNK+ = LOW division + VERY LOW death (near-quiescent; grows only via CAF boost);
    //                                   JNK- = HIGH division + HIGH death (grows fast; survives via EC death-reduction).
    // NOTE: param[0] is netN (JNK- NET growth); divProbN = dieProbN + netN is computed in RunHeadless,
    //       so the growth-curve-constrained net is inferred directly (identifiable, always net-positive).
    static final String[] NAME = {
        "netN","dieProbN","pOnMax","pOffMax","divProbP","dieProbP",
        "cafDivBoost","ecSurvival","activProbF","divProbFP","activProbM","activProbE"
    };
    static final double[] LO = {0.0015, 0.008, 0.01, 0.01, 0.005, 0.001, 0.0, 0.0, 0.001, 0.018, 0.02,  0.005};
    static int bestMacTry = 0, bestMacFail = 0;  // crowding read for the best run
    static final double[] HI = {0.005, 0.025, 0.10, 0.20, 0.03, 0.004, 1.0, 0.3, 0.05,  0.038, 0.08,  0.08};

    // ================================================================
    // 2) TARGETS — the calibration data (see the project's abc_config.yaml).
    //    Each target: a summary-statistic TYPE at a snapshot STEP, its observed
    //    VALUE, a WEIGHT, and a SCALE that normalises the residual.
    //      jnkp  = JNK+ tumour fraction        [flow: IR18]        (intensive)
    //      ec    = activated-EC fraction        [H22 Fig.1b]        (intensive)
    //      mac   = ACTIVATED macrophage fraction  [H22 Fig.6e] -- PROXY for the
    //              paper's perivascular fraction; the model biases activated-mac
    //              migration toward ECs, so activated ~ perivascular (see note in README).
    //      fibro = log10 fibroblast fold vs t0  [P20 Fig.1f]  (2D-adjusted)
    //      tumor = log10 tumour fold vs t0      [MDA231-LM2 biolum] (2D-adjusted)
    //
    //    2D DIMENSIONAL ADJUSTMENT (fold-change targets only):
    //      The ABM is a 2D areal lattice. tumor (bioluminescence ~ volume) and
    //      fibro (whole-lung count ~ tumour burden) are 3D/volumetric measures.
    //      For isotropic compact growth  fold_2D = fold_3D^(2/3), i.e. in log10
    //      value_2D = value_3D * 2/3:
    //        fibro 1440: 1.65 -> 1.10 ;  tumor 480/960/1440:
    //        0.63->0.42, 1.17->0.78, 1.66->1.11.
    //      Day-30 (step 2100) tumour target DROPPED: it lay beyond the growth-
    //      curve data window (all targets end at day 21 = step 1440). The model
    //      still runs to 2900 for chemo, so SNAP keeps step 2100; it is simply
    //      no longer a target (its snapshot is produced but not scored).
    //      Fractions (jnkp, ec, mac) are intensive ratios -> NOT converted.
    //      Fold scales rescaled 0.4 -> 0.27 (x2/3) to preserve relative tolerance
    //      (revert to 0.4 for the original absolute tolerance). Fibroblast exponent
    //      2/3 assumes volume-scaling; for a fixed-thickness shell use 1/2 -> 0.83.
    //      (Keep TV/TSC identical to config/abc_config.yaml in the SMC project.)
    // ================================================================
    static final String[] TT  = {"jnkp","jnkp","ec",  "ec",  "ec",  "mac", "fibro","tumor","tumor","tumor"};
    static final int[]    TS  = { 480,   1440,  480,   960,   1440,  1440,  1440,   480,    960,    1440 };
    static final double[] TV  = { 0.53,  0.14,  0.15,  0.30,  0.80,  0.77,  1.10,   0.42,   0.78,   1.11 };
    static final double[] TW  = { 1.0,   1.0,   0.5,   0.5,   1.0,   1.0,   1.0,    1.0,    1.0,    1.0  };
    static final double[] TSC = { 0.2,   0.2,   0.2,   0.2,   0.2,   0.2,   0.27,   0.27,   0.27,   0.27 };

    static final int[] SNAP = {0, 480, 960, 1440};   // snapshot step order (calibration horizon = day 21)
    static int snapIdx(int step) { for (int i=0;i<SNAP.length;i++) if (SNAP[i]==step) return i; return -1; }

    // ----------------------------------------------------------------
    // One summary statistic from the snapshots (int[5][8]).
    // snapshot row = {tumJNKp,tumJNKn,ecAct,ecInact,macAct,macInact,fibAct,fibInact}
    // Returns NaN if undefined (e.g. no cells of that type -> extinction).
    // ----------------------------------------------------------------
    static double stat(int[][] s, String type, int step) {
        int[] c = s[snapIdx(step)];
        int tumP=c[0], tumN=c[1], ecA=c[2], ecI=c[3], macA=c[4], macI=c[5], fibA=c[6], fibI=c[7];
        switch (type) {
            case "jnkp":  { int t=tumP+tumN; return t>0 ? (double)tumP/t : Double.NaN; }
            case "ec":    { int t=ecA+ecI;   return t>0 ? (double)ecA /t : Double.NaN; }
            case "mac":   { int t=macA+macI; return t>0 ? (double)macA/t : Double.NaN; }
            case "fibro": { int t0=s[0][6]+s[0][7], t=fibA+fibI; return (t0>0&&t>0)? Math.log10((double)t/t0):Double.NaN; }
            case "tumor": { int t0=s[0][0]+s[0][1], t=tumP+tumN; return (t0>0&&t>0)? Math.log10((double)t/t0):Double.NaN; }
            default: return Double.NaN;
        }
    }

    // ----------------------------------------------------------------
    // 3) DISTANCE — weighted, scaled Euclidean over all targets.
    //    Returns +infinity if any target is undefined (degenerate run),
    //    which guarantees such a run is rejected.
    // ----------------------------------------------------------------
    static double distance(int[][] s) {
        if (s.length != SNAP.length)
            throw new IllegalArgumentException("expected " + SNAP.length + " snapshots, got " + s.length);
        // Genuine tumour extinction (no tumour cells at the final snapshot) -> reject.
        if (s[SNAP.length-1][0] + s[SNAP.length-1][1] == 0) return Double.POSITIVE_INFINITY;
        double sum = 0.0;
        for (int j=0; j<TT.length; j++) {
            double sim = stat(s, TT[j], TS[j]);
            double resid;
            if (Double.isNaN(sim)) {
                // A stromal compartment was squeezed to zero by an OVER-GROWN tumour
                // (grid saturation), NOT extinction. Apply a large but finite penalty so
                // the run is rejected and visible, instead of mislabelled as extinct.
                resid = 3.0;
            } else {
                resid = (sim - TV[j]) / TSC[j];
            }
            sum += TW[j] * resid * resid;
        }
        return Math.sqrt(sum);
    }

    // ----------------------------------------------------------------
    // Per-target residual breakdown for one run's snapshots.
    // Prints each target's sim vs target and its contribution to d^2
    // ( contrib_j = weight_j * ((sim_j - target_j)/scale_j)^2 ), sorted
    // worst-first, plus each target's share of d^2. Diffuse (many small
    // shares) => target tension; one dominant share => a broken mechanism.
    // ----------------------------------------------------------------
    static void printBreakdown(int[][] s, int[][] rimCore, double dist) {
        int m = TT.length;
        double[]  sims    = new double[m];
        double[]  contrib = new double[m];
        Integer[] order   = new Integer[m];
        for (int j = 0; j < m; j++) {
            double sim = stat(s, TT[j], TS[j]);
            sims[j] = sim;
            double resid = (sim - TV[j]) / TSC[j];
            contrib[j] = TW[j] * resid * resid;
            order[j] = j;
        }
        Arrays.sort(order, (a, b) -> Double.compare(contrib[b], contrib[a]));  // worst first
        double d2 = dist * dist;
        System.out.printf("%n--- best run: per-target breakdown  (d = %.3f, d^2 = %.3f) ---%n", dist, d2);
        System.out.printf("  %-16s %8s %8s %6s %6s %10s  %6s%n",
                "target@step", "sim", "target", "wt", "scale", "contrib", "share");
        for (int k = 0; k < m; k++) {
            int j = order[k];
            double share = (d2 > 0) ? 100.0 * contrib[j] / d2 : 0.0;
            System.out.printf("  %-16s %8.3f %8.3f %6.2f %6.2f %10.3f  %5.1f%%%n",
                    TT[j] + "@" + TS[j], sims[j], TV[j], TW[j], TSC[j], contrib[j], share);
        }
        System.out.println("  (contrib = weight * ((sim-target)/scale)^2 ;  sum of contrib = d^2)");

        // spatial / cell-count diagnostics for the same best run
        System.out.printf("%n--- best run: JNK+ location & macrophage counts (per snapshot) ---%n");
        System.out.printf("  %-6s %8s %8s %9s %9s %8s %8s%n",
                "step", "tumP", "tumN", "jnk+rim", "jnk+core", "macAct", "macIn");
        for (int k = 0; k < SNAP.length && k < s.length; k++) {
            int[] c = s[k];
            int rimc  = (rimCore != null && k < rimCore.length) ? rimCore[k][0] : -1;
            int corec = (rimCore != null && k < rimCore.length) ? rimCore[k][1] : -1;
            System.out.printf("  %-6d %8d %8d %9d %9d %8d %8d%n",
                    SNAP[k], c[0], c[1], rimc, corec, c[4], c[5]);
        }
        System.out.println("  jnk+rim = JNK+ cells with an exposed (empty/non-tumour) neighbour; jnk+core = buried.");
        System.out.println("  macAct  = activated macrophages (the mac-stat numerator). ~0 across all steps => macs never activate.");
        System.out.printf("  activated-mac division crowding: %d/%d attempts blocked for no space (%.0f%%)%n",
                bestMacFail, bestMacTry, bestMacTry > 0 ? 100.0*bestMacFail/bestMacTry : 0.0);
    }

    // ================================================================
    // MAIN — the rejection loop.
    // ================================================================
    public static void main(String[] args) throws IOException {
        int    N        = args.length > 0 ? Integer.parseInt(args[0]) : 1000;
        double epsilon  = args.length > 1 ? Double.parseDouble(args[1]) : -1.0;   // <0 => quantile
        double quantile = args.length > 2 ? Double.parseDouble(args[2]) : 0.2;
        long   seed     = args.length > 3 ? Long.parseLong(args[3]) : 12345L;
        int    initPop  = args.length > 4 ? Integer.parseInt(args[4]) : 25;

        // pre-flight: the model reads these every run; fail fast with a clear message
        for (String req : new String[]{"QuadratEndothelialOn.txt", "QuadratStrOn.txt"}) {
            if (!new java.io.File(req).exists()) {
                System.err.println("ERROR: required input not found in working dir: " + req
                        + "\n  Run from the directory that contains the Quadrat*.txt coordinate files.");
                return;
            }
        }

        Rand rng = new Rand(seed);
        double[][] thetas = new double[N][NAME.length];
        double[]   dists  = new double[N];
        double[][] simOut = new double[N][TT.length];   // simulated value of each of the 10 target statistics per run
        int        nErr   = 0;   // runs that threw (bug/setup), distinct from extinction
        double     bestDist  = Double.POSITIVE_INFINITY;
        int[][]    bestSnaps = null;   // snapshots of the lowest-distance finite run (for the breakdown)
        int[][]    bestRimCore = null; // {rim,core} JNK+ counts of that same run
        java.util.ArrayList<int[]> extinctTraj = new java.util.ArrayList<>(); // tumour totals/snapshot for extinct runs

        System.out.println("ABC rejection | " + N + " draws | " + NAME.length
                + " params | " + TT.length + " targets | seed " + seed);

        long t0 = System.currentTimeMillis();
        for (int n = 0; n < N; n++) {
            // 1) PROPOSE: theta ~ Uniform(LO, HI)
            double[] th = new double[NAME.length];
            for (int p = 0; p < NAME.length; p++) th[p] = LO[p] + rng.Double() * (HI[p] - LO[p]);
            thetas[n] = th;

            // 2) SIMULATE: fresh grid, per-run seed (so the whole sweep is reproducible)
            try {
                ExampleGrid model = new ExampleGrid(100, 100);
                model.rng = new Rand(seed + 1_000L + n);
                int[][] snaps = model.RunHeadless(th, initPop);
                // 3) COMPARE
                dists[n] = distance(snaps);
                for (int j = 0; j < TT.length; j++) simOut[n][j] = stat(snaps, TT[j], TS[j]);
                if (Double.isFinite(dists[n]) && dists[n] < bestDist) {  // remember best run's snapshots
                    bestDist    = dists[n];
                    bestSnaps   = snaps;
                    bestRimCore = model.lastRimCore;
                    bestMacTry  = model.macDivTry;
                    bestMacFail = model.macDivFail;
                }
                if (!Double.isFinite(dists[n])) {   // extinction: record tumour trajectory (JNK+ + JNK-)
                    int[] tr = new int[SNAP.length];
                    for (int i = 0; i < SNAP.length; i++) tr[i] = snaps[i][0] + snaps[i][1];
                    extinctTraj.add(tr);
                }
            } catch (Exception e) {
                dists[n] = Double.POSITIVE_INFINITY;   // an ERROR (not extinction) -> rejected
                nErr++;
                if (nErr <= 5) {                       // surface the first few for debugging
                    System.err.println("run " + n + " threw "
                            + e.getClass().getSimpleName() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }

            if ((n + 1) % 25 == 0) {
                double secs = (System.currentTimeMillis() - t0) / 1000.0;
                System.out.printf("  %d/%d done  (%.0f s, %.2f s/run)%n", n+1, N, secs, secs/(n+1));
            }
        }

        // ---- distance distribution + choose epsilon ----
        double[] finite = Arrays.stream(dists).filter(Double::isFinite).sorted().toArray();
        int nFin = finite.length;
        System.out.println("\nfinite runs: " + nFin + "/" + N
                + "   (rejected: " + (N - nFin) + " total; of those " + nErr + " ERRORED, "
                + ((N - nFin) - nErr) + " went extinct)");
        if (nErr > 0) {
            System.out.println("WARNING: " + nErr + " run(s) threw an exception - a bug or "
                    + "missing/short input, NOT biological extinction. See the traces above.");
        }
        if (!extinctTraj.isEmpty()) {
            int neverEst = 0, grewCrashed = 0;
            for (int[] tr : extinctTraj) { int pk=0; for (int v: tr) pk=Math.max(pk,v);
                if (pk < 40) neverEst++; else grewCrashed++; }
            System.out.printf("%n--- extinction diagnostic (%d extinct runs) ---%n", extinctTraj.size());
            System.out.printf("  never established (peak tumour < 40 cells): %d%n", neverEst);
            System.out.printf("  grew then crashed (peak >= 40 cells):       %d%n", grewCrashed);
            System.out.println("  first extinct runs, tumour total per snapshot {0,480,960,1440}:");
            for (int k = 0; k < Math.min(12, extinctTraj.size()); k++) {
                int[] tr = extinctTraj.get(k);
                System.out.printf("    %2d:  %5d -> %5d -> %5d -> %5d%n", k, tr[0], tr[1], tr[2], tr[3]);
            }
        }
        if (nFin == 0) {
            if (nErr == N) System.out.println("Every run ERRORED (see traces) - fix the setup, not the priors.");
            else           System.out.println("Every run went extinct - the prior region kills the tumour; widen/shift priors.");
            return;
        }
        double dmin = finite[0], dmed = finite[nFin/2];
        // keep the closest ~quantile fraction: choose the COUNT, then take the
        // count-th smallest distance as epsilon (inclusive <= keeps exactly `keep`).
        int    keep = Math.max(1, (int)Math.round(quantile * nFin));   // at least 1
        double eps  = (epsilon > 0) ? epsilon : finite[Math.min(nFin - 1, keep - 1)];
        System.out.printf("distance:  min=%.3f  median=%.3f%n", dmin, dmed);
        System.out.printf("epsilon:   %.3f   (%s)%n", eps,
                (epsilon > 0) ? "fixed" : ("quantile q=" + quantile));

        // per-target breakdown of the best-scoring run: which targets dominate the distance?
        if (bestSnaps != null) printBreakdown(bestSnaps, bestRimCore, bestDist);

        // ---- 4) ACCEPT: keep theta with distance <= epsilon, write posterior ----
        FileWriter w = new FileWriter("posterior_java.csv");
        StringBuilder hdr = new StringBuilder(String.join(",", NAME));
        for (int j = 0; j < TT.length; j++) hdr.append(",out_").append(TT[j]).append("_").append(TS[j]);
        hdr.append(",distance");
        w.write(hdr + "\n");
        int nAcc = 0;
        for (int n = 0; n < N; n++) {
            if (Double.isFinite(dists[n]) && dists[n] <= eps) {
                StringBuilder sb = new StringBuilder();
                for (double v : thetas[n]) sb.append(v).append(",");
                for (double v : simOut[n]) sb.append(v).append(",");
                sb.append(dists[n]);
                w.write(sb.toString() + "\n");
                nAcc++;
            }
        }
        w.close();
        System.out.println("accepted " + nAcc + " parameter set(s)  ->  posterior_java.csv");
        System.out.println("(each accepted row is a sample from the approximate posterior)");
    }
}
