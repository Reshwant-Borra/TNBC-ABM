package OnLatticeExample;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Immutable, named parameter set for headless TNBC model runs. */
public final class ModelParameters {
    public enum Transform { LINEAR, LOG, LOGIT, INTEGER, CATEGORICAL }

    public static final class Definition {
        public final String name, file, className, method, location, interpretation;
        public final double baseline, lower, upper;
        public final String status, boundSource, evidenceQuality, expectedOutputs;
        public final String mechanismEffect, activeAtBaseline, justification, group, rangeRisk;
        public final Transform transform;
        public final boolean screen;

        Definition(String name, String file, String className, String method, String location,
                   String interpretation, double baseline, String status, double lower, double upper,
                   Transform transform, String boundSource, String evidenceQuality,
                   String expectedOutputs, String mechanismEffect, String activeAtBaseline,
                   boolean screen, String justification, String group, String rangeRisk) {
            this.name = name;
            this.file = file;
            this.className = className;
            this.method = method;
            this.location = location;
            this.interpretation = interpretation;
            this.baseline = baseline;
            this.status = status;
            this.lower = lower;
            this.upper = upper;
            this.transform = transform;
            this.boundSource = boundSource;
            this.evidenceQuality = evidenceQuality;
            this.expectedOutputs = expectedOutputs;
            this.mechanismEffect = mechanismEffect;
            this.activeAtBaseline = activeAtBaseline;
            this.screen = screen;
            this.justification = justification;
            this.group = group;
            this.rangeRisk = rangeRisk;
        }

        public boolean isInteger() { return transform == Transform.INTEGER; }

        public double fromNormalized(double x) {
            if (!Double.isFinite(x) || x < -1e-12 || x > 1.0 + 1e-12) {
                throw new IllegalArgumentException(name + ": normalized value outside [0,1]: " + x);
            }
            x = Math.max(0.0, Math.min(1.0, x));
            switch (transform) {
                case LOG:
                    if (!(lower > 0.0)) throw new IllegalStateException(name + ": log lower bound must be positive");
                    return Math.exp(Math.log(lower) + x * (Math.log(upper) - Math.log(lower)));
                case LOGIT:
                    if (!(lower > 0.0 && upper < 1.0)) {
                        throw new IllegalStateException(name + ": logit bounds must lie strictly inside (0,1)");
                    }
                    double a = Math.log(lower / (1.0 - lower));
                    double b = Math.log(upper / (1.0 - upper));
                    double z = a + x * (b - a);
                    return 1.0 / (1.0 + Math.exp(-z));
                case INTEGER:
                    return Math.rint(lower + x * (upper - lower));
                case LINEAR:
                    return lower + x * (upper - lower);
                default:
                    throw new IllegalStateException(name + ": categorical parameters are not in this Morris design");
            }
        }
    }

    private static Definition d(String name, String method, String location, String interpretation,
                                double baseline, String status, double lower, double upper,
                                Transform transform, String boundSource, String evidence,
                                String outputs, String mechanismEffect, String active,
                                boolean screen, String justification, String group, String risk) {
        return new Definition(name, "OnLatticeExample/ExampleGrid.java", "ExampleGrid/ExampleCell",
                method, location, interpretation, baseline, status, lower, upper, transform,
                boundSource, evidence, outputs, mechanismEffect, active, screen, justification, group, risk);
    }

    private static final List<Definition> REGISTRY;
    private static final List<Definition> SCREENED;
    private static final Map<String, Definition> BY_NAME;

    static {
        List<Definition> r = new ArrayList<>();
        String abc = "Current ABCRejection.java prior; newer than the supplied June 2026 reference PDF";
        String harness = "Project mechanism-harness range; assumption requiring mentor approval";
        String assumed = "Conservative variation around baseline; assumption requiring mentor approval";

        r.add(d("netN", "RunHeadless", "legacy lines 816-817", "JNK-negative tumour net growth per step; divProbN=dieProbN+netN", 0.0025997991807979125, "ABC-inferred", 0.0015, 0.005, Transform.LOG, abc, "project", "tumour burden; extinction; JNK fraction", "effect", "yes", true, "Current ABC prior retained exactly", "tumor", "Coupled to dieProbN; derived division must remain below one"));
        r.add(d("dieProbN", "tumorCell", "legacy lines 197-219", "JNK-negative tumour death probability per step", 0.018574516658068727, "ABC-inferred", 0.008, 0.025, Transform.LOG, abc, "project", "tumour burden; extinction; JNK fraction", "effect", "yes", true, "Current ABC prior retained exactly", "tumor", "Confounded with netN and EC survival"));
        r.add(d("pOnMax", "tumorCell", "legacy lines 197-198", "Ceiling for JNK-negative to JNK-positive switching", 0.034429916846495774, "ABC-inferred", 0.01, 0.10, Transform.LOG, abc, "low", "JNK fraction; tumour burden", "effect", "yes", true, "Current ABC prior retained exactly", "jnk", "Event ladder clamps the realized probability"));
        r.add(d("pOffMax", "tumorCell", "legacy lines 169-170", "Ceiling for JNK-positive to JNK-negative switching", 0.1188029560408925, "ABC-inferred", 0.01, 0.20, Transform.LOG, abc, "low", "JNK fraction; tumour burden", "effect", "yes", true, "Current ABC prior retained exactly", "jnk", "Event ladder clamps the realized probability"));
        r.add(d("divProbP", "tumorCell", "legacy lines 168-181", "JNK-positive tumour division probability per step", 0.006621397759285578, "ABC-inferred", 0.005, 0.03, Transform.LOG, abc, "project", "tumour burden; JNK fraction; extinction", "effect", "yes", true, "Current ABC prior retained exactly", "tumor", "CAF multiplier increases effective division"));
        r.add(d("dieProbP", "tumorCell", "legacy lines 165-173", "JNK-positive tumour death probability per step", 0.003061084528053752, "ABC-inferred", 0.001, 0.004, Transform.LOG, abc, "project", "tumour burden; JNK fraction; extinction", "effect", "yes", true, "Current ABC prior retained exactly", "tumor", "EC survival reduces effective death"));
        r.add(d("cafDivBoost", "tumorCell", "legacy line 168", "Multiplicative CAF support of JNK-positive tumour division", 0.4276094684821006, "ABC-inferred", 0.0, 1.0, Transform.LINEAR, abc, "low", "tumour burden; JNK rim", "effect", "yes", true, "Current ABC prior retained exactly", "fibroblast", "Zero lower bound requires linear sampling"));
        r.add(d("ecSurvival", "tumorCell", "legacy line 165", "Exponential tumour-death reduction per nearby activated EC", 0.05099982197121976, "ABC-inferred", 0.0, 0.3, Transform.LINEAR, abc, "low", "tumour burden; extinction", "effect", "yes", true, "Current ABC prior retained exactly", "endothelial", "Count-dependent exponential interaction"));
        r.add(d("activProbF", "Fibroblasts", "legacy lines 500-513", "Resting fibroblast activation probability near JNK-positive tumour", 0.023591782410718555, "ABC-inferred", 0.001, 0.05, Transform.LOG, abc, "medium target/low rate", "fibroblast burden; tumour burden", "effect", "yes", true, "Current ABC prior retained exactly", "fibroblast", "Daughter value also receives density boost"));
        r.add(d("divProbFP", "Fibroblasts", "legacy lines 477-488", "Activated CAF division probability per step", 0.029805746764728917, "ABC-inferred", 0.018, 0.038, Transform.LINEAR, abc + "; supplied PDF lists older 0.01-0.15", "medium target/low rate", "fibroblast burden; tumour burden", "effect", "yes", true, "Current ABC prior retained; older wider PDF range is documented, not silently substituted", "fibroblast", "Activated-cell value can receive initialization density boost"));
        r.add(d("activProbM", "Macrophages", "legacy lines 377-405", "Macrophage activation probability near JNK-positive tumour", 0.04182908819427277, "ABC-inferred", 0.02, 0.08, Transform.LINEAR, abc + "; supplied PDF lists older lower bound 0.005", "medium target/low rate", "macrophage activation; EC activation", "effect", "yes", true, "Current ABC prior retained", "macrophage", "Daughter activation probability receives a local tumour-count boost"));
        r.add(d("activProbE", "Endothelial", "legacy lines 448-456", "EC activation hazard per nearby activated macrophage", 0.05947751372653652, "ABC-inferred", 0.005, 0.08, Transform.LOG, abc, "medium target/low rate", "EC activation; tumour burden", "effect", "yes", true, "Current ABC prior retained exactly", "endothelial", "Compound hazard 1-(1-p)^k"));

        r.add(d("recruitBias", "Macrophages", "legacy lines 384-395", "Probability that an inactive macrophage migration step is biased toward tumour centroid", 0.04, "fixed", 0.0, 0.20, Transform.LINEAR, harness, "low", "macrophage and EC outputs", "effect", "yes", true, "Active fixed mechanism with project-tested range", "macrophage", "Conditional on migration and non-extinct tumour"));
        r.add(d("lambdaCAF", "nicheSignal", "legacy lines 110-123", "CAF paracrine exponential decay length in lattice sites", 2.0, "fixed", 0.5, 5.0, Transform.LOG, harness, "low", "JNK fraction; tumour burden; CAF distance", "effect", "yes", true, "Spatial fixed parameter was active in mechanism harness", "jnk", "Search truncation is separately recorded"));
        r.add(d("stressStrength", "stressSignal", "legacy lines 136-150", "Gain on hostile-lung JNK stress field", 1.5, "fixed", 0.0, 3.0, Transform.LINEAR, harness, "low", "JNK fraction; tumour burden", "effect", "yes", true, "Active fixed parameter with project-tested range", "jnk", "Signal saturates at one"));
        r.add(d("lambdaStress", "stressSignal", "legacy lines 136-150", "Hostile-lung stress exponential decay length in lattice sites", 2.0, "fixed", 0.5, 5.0, Transform.LOG, harness, "low", "JNK fraction; tumour burden", "effect", "yes", true, "Active fixed parameter with project-tested range", "jnk", "Search truncation is separately recorded"));
        r.add(d("migrProbP", "tumorCell", "legacy lines 183-193", "JNK-positive tumour migration probability per step", 0.10, "fixed", 0.0, 0.30, Transform.LINEAR, harness, "low", "tumour spread; burden; JNK fraction", "effect", "yes", true, "IR18 supports direction/ratio; absolute harness range requires approval", "tumor", "Competes in tumour event ladder"));
        r.add(d("migrProbN", "tumorCell", "legacy lines 211-221", "JNK-negative tumour migration probability per step", 0.01, "fixed", 0.0, 0.10, Transform.LINEAR, harness, "low", "tumour spread; burden; JNK fraction", "effect", "yes", true, "IR18 supports about ten-fold contrast; absolute range requires approval", "tumor", "Independent sampling does not preserve the proposed 10x ratio"));
        r.add(d("divProbFN", "Fibroblasts", "legacy lines 491-501", "Resting fibroblast division probability per step", 0.0, "fixed", 0.0, 0.01, Transform.LINEAR, harness, "low", "fibroblast burden", "effect", "yes", true, "Project-tested range; supplied PDF baseline 0.010 conflicts with current code 0", "fibroblast", "Zero is a biological structural assumption, but branch is executable"));
        r.add(d("dieProbFN", "Fibroblasts", "legacy lines 491-494", "Resting fibroblast death probability per step", 0.008, "fixed", 0.001, 0.008, Transform.LOG, harness, "low", "fibroblast burden", "effect", "yes", true, "Project-tested range; current upper endpoint retained", "fibroblast", "Time-step audit indicates implausibly short half-life"));
        r.add(d("dieProbFP", "Fibroblasts", "legacy lines 477-480", "Activated CAF death probability per step", 0.012, "fixed", 0.001, 0.012, Transform.LOG, harness, "low", "fibroblast burden; tumour burden", "effect", "yes", true, "Project-tested range", "fibroblast", "Strongly coupled to divProbFP"));
        r.add(d("divProbMN", "Macrophages", "legacy lines 369-380", "Resting macrophage division probability per step", 0.005, "fixed", 0.001, 0.01, Transform.LOG, harness, "low", "macrophage total; EC activation", "effect", "yes", true, "Project-tested range", "macrophage", "Homeostatic equality with dieProbMN is not enforced in global screen"));
        r.add(d("dieProbMN", "Macrophages", "legacy lines 369-372", "Resting macrophage death probability per step", 0.005, "fixed", 0.001, 0.01, Transform.LOG, harness, "low", "macrophage total; EC activation", "effect", "yes", true, "Project-tested range; supplied PDF baseline 0.008 is obsolete", "macrophage", "Independent sampling breaks baseline homeostatic equality"));
        r.add(d("divProbMP", "Macrophages", "legacy lines 319-329", "Activated macrophage division probability per step", 0.01575, "fixed", 0.001, 0.03, Transform.LOG, harness, "low", "macrophage total; EC activation", "effect", "yes", true, "Project-tested range; supplied PDF baseline 0.030 is obsolete", "macrophage", "Empty-space limitation makes realized division smaller"));
        r.add(d("dieProbMP", "Macrophages", "legacy lines 319-322", "Activated macrophage death probability per step", 0.015, "fixed", 0.001, 0.03, Transform.LOG, harness, "low", "macrophage total; EC activation", "effect", "yes", true, "Project-tested range", "macrophage", "Independent sampling breaks the baseline 1.05 division/death relation"));
        r.add(d("migrProbM", "Macrophages", "legacy lines 333-365 and 381-400", "Macrophage migration probability per step", 0.8, "fixed", 0.10, 0.85, Transform.LINEAR, "Mechanism harness upper bound 0.95 reduced explicitly to 0.85 to preserve all event ladders; mentor approval required", "low", "macrophage localization; EC activation", "effect", "yes", true, "Active fixed parameter; range constraint is explicit, not silent", "macrophage", "Upper range plus other probability maxima must remain below one"));
        r.add(d("dieProbEN", "Endothelial", "legacy lines 444-453", "Resting EC death probability per step", 0.005, "fixed", 0.001, 0.01, Transform.LOG, harness, "low", "EC population and activation fraction", "effect", "yes", true, "Project-tested range", "endothelial", "Inactive EC division is blocked, so this is uncompensated loss"));
        r.add(d("divProbEP", "Endothelial", "legacy lines 425-436", "Activated EC division probability per step", 0.0087, "fixed", 0.001, 0.02, Transform.LOG, harness, "medium conversion", "EC population and activation fraction; tumour survival", "effect", "yes", true, "Project-tested range around H22-derived baseline", "endothelial", "Daughter division receives macrophage-density boost"));
        r.add(d("dieProbEP", "Endothelial", "legacy lines 425-427", "Activated EC death probability per step", 0.008, "fixed", 0.001, 0.02, Transform.LOG, harness, "low", "EC population and activation fraction; tumour survival", "effect", "yes", true, "Project-tested range", "endothelial", "Coupled to activated EC division"));
        r.add(d("deactProbE", "Endothelial", "legacy lines 438-442", "Activated-to-resting EC reversion probability without local activated macrophages", 0.01, "fixed", 0.0, 0.05, Transform.LINEAR, harness, "low", "EC activation fraction; tumour survival", "effect", "yes", true, "Project-tested range", "endothelial", "Only active when local activated-macrophage count is zero"));
        r.add(d("dieProbL", "lungCells", "legacy lines 524-525", "Lung-cell death probability per step", 0.002, "fixed", 0.0005, 0.01, Transform.LOG, harness, "low", "stress field; tumour burden", "effect", "yes", true, "Project-tested range", "lung", "Lung division is declared but not implemented"));

        r.add(d("macrophageDaughterActivationBoost", "Macrophages", "legacy line 377: activProbM + 0.008*tumorCount", "Per-JNK-positive-neighbour boost to daughter macrophage activation probability", 0.008, "hard-coded", 0.002, 0.016, Transform.LOG, assumed, "low", "macrophage activation; EC activation", "not tested", "yes", true, "Hidden interaction strength promoted to named parameter", "macrophage", "Large local counts can make the raw daughter probability exceed one"));
        r.add(d("endothelialDaughterDivisionBoost", "Endothelial", "legacy line 434: divProbEP + 0.001*macroActCount", "Per-activated-macrophage boost to daughter EC division probability", 0.001, "hard-coded", 0.00025, 0.002, Transform.LOG, assumed, "low", "EC population; tumour survival", "not tested", "yes", true, "Hidden interaction strength promoted to named parameter", "endothelial", "Applies to daughters, creating inherited heterogeneity"));
        r.add(d("fibroblastSignalBoost", "Fibroblasts", "legacy lines 512-513: +0.02*fB", "Per-capped-JNK-positive-neighbour boost assigned on CAF activation", 0.02, "hard-coded", 0.005, 0.04, Transform.LOG, assumed, "low", "fibroblast burden; tumour burden", "not tested", "yes", true, "Hidden interaction strength promoted to named parameter", "fibroblast", "Must keep CAF death+division ladder below one"));
        r.add(d("fibroblastSignalCap", "Fibroblasts", "legacy line 509: min(tumorFCount,10)", "Cap on JNK-positive neighbours contributing to CAF signal strength", 10, "hard-coded", 5, 15, Transform.INTEGER, assumed, "low", "fibroblast burden; tumour burden", "not tested", "yes", true, "Hidden density coefficient promoted to integer parameter", "fibroblast", "Interacts multiplicatively with fibroblastSignalBoost"));
        r.add(d("tumorEndothelialRadius", "countNeighbors35/tumorCell", "legacy lines 87-99", "Radius for activated-EC tumour-survival support", 3.5, "hard-coded", 1.5, 5.5, Transform.LINEAR, assumed, "low", "tumour survival and burden", "not tested", "yes", true, "Biological interaction radius exposed without changing neighbour semantics", "endothelial", "Larger radii amplify count-dependent exponential survival"));
        r.add(d("macrophageInteractionRadius", "Macrophages", "legacy line 309", "Radius for macrophage sensing of JNK-positive tumour and ECs", 3.5, "hard-coded", 1.5, 5.5, Transform.LINEAR, assumed, "low", "macrophage activation/localization; EC activation", "not tested", "yes", true, "Biological neighbourhood radius exposed", "macrophage", "Changes two sensed cell sets simultaneously, matching current rule"));
        r.add(d("macrophageEndothelialBiasRadius", "Macrophages", "legacy lines 346-348", "Distance threshold defining EC-biased macrophage migration destinations", 3.5, "hard-coded", 1.5, 5.5, Transform.LINEAR, assumed, "low", "macrophage-EC colocalization; EC activation", "not tested", "yes", true, "Spatial bias radius exposed", "macrophage", "Conditional on ECs within macrophageInteractionRadius"));
        r.add(d("endothelialMacrophageRadius", "Endothelial", "legacy line 418", "Radius for activated macrophages to activate/support ECs", 1.5, "hard-coded", 1.0, 3.5, Transform.LINEAR, assumed, "low", "EC activation; tumour survival", "not tested", "yes", true, "Biological neighbourhood radius exposed", "endothelial", "Discrete lattice geometry makes response stepwise"));
        r.add(d("fibroblastTumorRadius", "Fibroblasts", "legacy line 470", "Radius over which JNK-positive tumour activates fibroblasts", 3.5, "hard-coded", 1.5, 5.5, Transform.LINEAR, assumed, "low", "fibroblast activation; tumour burden", "not tested", "yes", true, "Biological neighbourhood radius exposed", "fibroblast", "Discrete lattice geometry makes response stepwise"));
        r.add(d("clusterRadius", "RunHeadless initialization", "legacy lines 844-859", "Radius of initial tumour cluster in lattice sites", 4, "fixed", 2, 8, Transform.INTEGER, harness, "low", "establishment; spread; all niche outputs", "effect", "yes", true, "Project-tested integer range", "initialization", "Small radius may not fit high initPop without crowding"));
        r.add(d("initialJnkPositiveTenths", "RunHeadless initialization", "legacy lines 857-858: 9 of 10 draws", "Initial JNK-positive fraction represented in tenths", 9, "hard-coded", 5, 10, Transform.INTEGER, "IR18 supports majority JNK-positive; discrete range requires mentor approval", "low", "early JNK fraction; establishment", "not tested", "yes", true, "Uses the original integer draw to preserve baseline RNG behavior", "initialization", "Only 0.1 resolution; ten means all JNK-positive"));
        r.add(d("initialMacrophageCount", "RunHeadless initialization", "legacy line 841", "Initial randomly placed macrophage population", 925, "hard-coded", 463, 1388, Transform.INTEGER, assumed, "low", "macrophage and EC outputs; crowding", "not tested", "yes", true, "Counts conflict with older slide estimates and require global screening", "initialization", "High values increase occupancy and initialization runtime"));
        r.add(d("initialLungCount", "RunHeadless initialization", "legacy line 842", "Initial randomly placed lung-cell population", 1225, "hard-coded", 613, 1838, Transform.INTEGER, assumed, "low", "stress field; crowding; tumour outputs", "not tested", "yes", true, "Counts conflict with older slide estimates and require global screening", "initialization", "High values increase occupancy"));
        r.add(d("initPop", "RunHeadless initialization", "legacy lines 848-860", "Initial tumour-cell population", 25, "fixed CLI", 10, 50, Transform.INTEGER, harness, "low", "establishment; extinction; all tumour outputs", "effect", "yes", true, "Project-tested integer range; --init-pop changes its baseline before design generation", "initialization", "Must fit inside clusterRadius and available sites"));

        // Complete-audit exclusions. They remain in the registry and model, but are not assumed away.
        r.add(d("migrProbF", "Fibroblasts", "declared legacy line 630", "Fibroblast migration field", 0.0, "inactive", 0.0, 0.20, Transform.LINEAR, harness, "high code", "none in current implementation", "no effect", "no: no branch", false, "Structurally inactive: Fibroblasts has no migration branch", "fibroblast", "Do not interpret as biologically unnecessary"));
        r.add(d("activProbMP", "Macrophages", "declared legacy line 644", "Macrophage inactivation/activation reference field", 0.02, "inactive", 0.0, 0.05, Transform.LINEAR, harness, "high code", "none in current implementation", "no effect", "no: unused", false, "Structurally inactive and explicitly marked unused", "macrophage", "Retained permanently for auditability"));
        r.add(d("divProbEN", "Endothelial", "declared legacy line 649", "Resting EC division field", 0.0, "inactive", 0.0, 0.01, Transform.LINEAR, harness, "high code", "none in current implementation", "no effect", "no: inactive ECs do not divide", false, "Structurally blocked by current event logic", "endothelial", "Do not infer biological dispensability"));
        r.add(d("migrProbE", "Endothelial", "declared legacy line 657", "EC migration field", 0.0, "inactive", 0.0, 0.20, Transform.LINEAR, harness, "high code", "none in current implementation", "no effect", "no: no branch", false, "Structurally inactive: Endothelial has no migration branch", "endothelial", "Retained permanently"));
        r.add(d("divProbL", "lungCells", "declared legacy lines 661-663", "Lung-cell division field", 0.002, "inactive", 0.0, 0.01, Transform.LINEAR, harness, "high code", "none in current implementation", "no effect", "no: lungCells only dies", false, "Structurally inactive despite homeostasis comment", "lung", "Current death is uncompensated"));
        r.add(d("initialEndothelialCoordinates", "RunHeadless initialization", "QuadratEndothelialOn.txt and legacy lines 826-831", "Fixed EC coordinate-file realization (237 parsed pairs)", 237, "structural", 237, 237, Transform.INTEGER, "Fixed empirical/legacy spatial input; provenance unresolved", "low", "EC/macrophage/tumour outputs", "not tested", "yes", false, "No defensible resampling rule or range; retain as fixed spatial input pending image provenance", "initialization", "Count and geometry cannot be separated"));
        r.add(d("initialFibroblastCoordinates", "RunHeadless initialization", "QuadratStrOn.txt and legacy lines 832-836", "Fixed fibroblast coordinate-file realization (142 parsed pairs)", 142, "structural", 142, 142, Transform.INTEGER, "Fixed empirical/legacy spatial input; provenance unresolved", "low", "fibroblast/JNK/tumour outputs", "not tested", "yes", false, "No defensible resampling rule or range; retain pending image provenance", "initialization", "Count and geometry cannot be separated"));
        r.add(d("signalSearchMultiplier", "nicheSignal/stressSignal", "legacy lines 113 and 139", "Numerical cutoff in exponential-field neighbour search", 3.0, "computational", 3.0, 3.0, Transform.LINEAR, "Numerical truncation convention", "medium", "all spatial-signal outputs", "not tested", "yes", false, "Numerical accuracy control, not biological uncertainty; screen lambda values instead", "computational", "At cutoff the unscaled field is exp(-3)"));
        r.add(d("signalSearchPadding", "nicheSignal/stressSignal", "legacy lines 113 and 139", "One-site padding added to exponential-field search", 1.0, "computational", 1.0, 1.0, Transform.LINEAR, "Numerical/lattice convention", "medium", "all spatial-signal outputs", "not tested", "yes", false, "Fixed numerical guard", "computational", "Not a biological length"));
        r.add(d("chemoJnkPositiveDeathMultiplier", "tumorCellChemo", "legacy line 240", "Treatment-specific JNK-positive death multiplier", 2.0, "treatment-specific", 2.0, 2.0, Transform.LINEAR, "Supplied reference PDF Table 7; qualitative IR18 support", "low", "treated tumour outputs", "not tested", "no untreated", false, "Excluded from untreated screen; not active by step 1440", "treatment", "Must be screened in a separate treated design"));
        r.add(d("chemoJnkPositiveOffMultiplier", "tumorCellChemo", "legacy line 242", "Treatment-specific JNK-positive switch-off multiplier", 0.05, "treatment-specific", 0.05, 0.05, Transform.LINEAR, "Supplied reference PDF Table 7", "low", "treated JNK fraction", "not tested", "no untreated", false, "Excluded from untreated screen", "treatment", "Qualitative direction has stronger support than factor"));
        r.add(d("chemoJnkNegativeDeathMultiplier", "tumorCellChemo", "legacy line 270", "Treatment-specific JNK-negative death multiplier", 5.0, "treatment-specific", 5.0, 5.0, Transform.LINEAR, "Supplied reference PDF Table 7", "low", "treated tumour outputs", "not tested", "no untreated", false, "Excluded from untreated screen", "treatment", "Qualitative direction has stronger support than factor"));
        r.add(d("chemoJnkNegativeOnMultiplier", "tumorCellChemo", "legacy lines 271-272", "Treatment-specific JNK-negative switch-on multiplier", 5.0, "treatment-specific", 5.0, 5.0, Transform.LINEAR, "Supplied reference PDF Table 7", "low", "treated JNK fraction", "not tested", "no untreated", false, "Excluded from untreated screen", "treatment", "Capped at one"));
        r.add(d("chemoStartStep", "RunHeadless", "legacy line 865", "Step threshold for chemotherapy event logic", 2899, "treatment-specific", 2899, 2899, Transform.INTEGER, "Current code", "high code/low biology", "treated outputs", "not tested", "no untreated", false, "Excluded from untreated screen", "treatment", "Default screen ends at 1440"));
        r.add(d("gridWidth", "ExampleGrid constructor", "new ExampleGrid(100,100)", "Lattice width", 100, "structural", 100, 100, Transform.INTEGER, "Current implementation", "high code", "all counts and spatial outputs", "not tested", "yes", false, "Domain-size sensitivity requires coordinate remapping and a separate design", "computational", "Changing alone would invalidate fixed coordinates"));
        r.add(d("gridHeight", "ExampleGrid constructor", "new ExampleGrid(100,100)", "Lattice height", 100, "structural", 100, 100, Transform.INTEGER, "Current implementation", "high code", "all counts and spatial outputs", "not tested", "yes", false, "Domain-size sensitivity requires coordinate remapping", "computational", "Changing alone would invalidate fixed coordinates"));
        r.add(d("movementNeighborhood", "ExampleGrid field", "Util.VonNeumannHood(false)", "Four-neighbour movement/division topology", 4, "structural", 4, 4, Transform.CATEGORICAL, "Current implementation", "high code", "all spatial outputs", "not tested", "yes", false, "Categorical mechanism change prohibited in this screening", "computational", "Changing it would alter neighbourhood/event logic"));
        r.add(d("calibrationHorizon", "RunHeadless", "default maxStep=1440", "Untreated simulation horizon", 1440, "structural", 1440, 1440, Transform.INTEGER, "480 steps/week project convention", "medium", "all time-indexed outputs", "not tested", "yes", false, "Defines the scientific observation window rather than a biological parameter", "computational", "Supplied PDF contains an obsolete inconsistent time table"));
        r.add(d("snapshotSchedule", "RunHeadless", "{0,480,960,1440,2100}", "Requested biological snapshot schedule", 4, "structural", 4, 4, Transform.CATEGORICAL, "Current ABC target schedule", "medium", "all time-indexed outputs", "not tested", "yes", false, "Observation design, not model uncertainty", "computational", "Only snapshots at or before maxStep are returned"));
        r.add(d("tumorSeedCenterX", "RunHeadless initialization", "legacy line 846", "Nominal tumour cluster x coordinate", 35, "structural stochastic", 35, 35, Transform.INTEGER, "Current initialization", "low", "all outputs through spatial realization", "not tested", "yes", false, "Treat location variability through matched simulation seeds, not as a calibratable biological parameter", "initialization", "Strongly coupled to fixed coordinate maps"));
        r.add(d("tumorSeedCenterY", "RunHeadless initialization", "legacy line 847", "Nominal tumour cluster y coordinate", 35, "structural stochastic", 35, 35, Transform.INTEGER, "Current initialization", "low", "all outputs through spatial realization", "not tested", "yes", false, "Treat location variability through matched simulation seeds", "initialization", "Strongly coupled to fixed coordinate maps"));
        r.add(d("tumorSeedJitter", "RunHeadless initialization", "legacy lines 845-847", "Integer jitter span for tumour seed centre", 6, "structural stochastic", 6, 6, Transform.INTEGER, "Current initialization", "low", "all outputs through spatial realization", "not tested", "yes", false, "Stochastic-realization control held fixed for CRN", "initialization", "Changing changes seed-to-realization mapping"));
        r.add(d("placementAttemptLimit", "RunHeadless initialization", "legacy line 849", "Maximum tumour placement attempts", 10000, "computational", 10000, 10000, Transform.INTEGER, "Defensive implementation limit", "high code", "initial population completeness", "not tested", "yes", false, "Numerical guard, not biology", "computational", "Failures are recorded explicitly"));
        r.add(d("unusedNeighborCountRadius", "countNeighbors15", "legacy line 74", "Radius in the currently uncalled compact neighbour-count helper", 1.5, "inactive", 1.5, 1.5, Transform.LINEAR, "Current implementation", "high code", "none in current implementation", "not tested", "no: helper is uncalled", false, "Structurally inactive helper retained for audit", "computational", "Would matter only if countNeighbors15 were restored to event logic"));
        r.add(d("rimMetricRadius", "jnkpRimCore", "legacy line 755", "Radius used only to classify JNK-positive rim versus core output", 1.5, "output definition", 1.5, 1.5, Transform.LINEAR, "Eight-neighbour Moore output convention", "high code", "JNK-positive rim metric only", "not tested", "yes for output only", false, "Held fixed so the response definition does not change across the sensitivity design", "computational", "Not a biological event radius"));
        r.add(d("rimBuriedNeighborThreshold", "jnkpRimCore", "legacy line 759", "Tumour-neighbour count defining a buried JNK-positive cell", 8, "output definition", 8, 8, Transform.INTEGER, "Eight-neighbour Moore output convention", "high code", "JNK-positive rim metric only", "not tested", "yes for output only", false, "Held fixed as an output definition", "computational", "Changing it changes the statistic, not model behavior"));
        r.add(d("chemoSwitchCeiling", "tumorCellChemo", "legacy line 271: min(1,5*pOnMax)", "Upper cap on treatment-induced JNK switch-on ceiling", 1.0, "treatment-specific", 1.0, 1.0, Transform.LINEAR, "Probability bound", "high code", "treated JNK fraction", "not tested", "no untreated", false, "Excluded from untreated screen", "treatment", "Part of treatment probability guard"));
        r.add(d("tumorEventOrder", "tumorCell", "death then division then switch then migration", "Categorical ordering of tumour stochastic events", 1, "structural", 1, 1, Transform.CATEGORICAL, "Current biological event logic", "high code", "all tumour outputs", "not tested", "yes", false, "Event ordering is a prohibited biological-logic change in this task", "computational", "Competing risks make ordering consequential"));
        r.add(d("agentUpdateShuffle", "StepCellR", "ShuffleAgents(rng) before each step", "Randomized asynchronous agent update order", 1, "structural stochastic", 1, 1, Transform.CATEGORICAL, "Current HAL execution logic", "high code", "all outputs", "not tested", "yes", false, "Held fixed; stochastic variability is represented by matched simulation seeds", "computational", "Changing update semantics would change the ABM"));

        LinkedHashMap<String, Definition> map = new LinkedHashMap<>();
        List<Definition> screened = new ArrayList<>();
        for (Definition def : r) {
            if (map.put(def.name, def) != null) throw new IllegalStateException("duplicate parameter name: " + def.name);
            if (def.screen) screened.add(def);
        }
        REGISTRY = Collections.unmodifiableList(r);
        SCREENED = Collections.unmodifiableList(screened);
        BY_NAME = Collections.unmodifiableMap(map);
    }

    // Named immutable fields used by ExampleGrid. No positional vector is exposed to new callers.
    public final double netN, dieProbN, pOnMax, pOffMax, divProbP, dieProbP;
    public final double cafDivBoost, ecSurvival, activProbF, divProbFP, activProbM, activProbE;
    public final double recruitBias, lambdaCAF, stressStrength, lambdaStress, migrProbP, migrProbN;
    public final double divProbFN, dieProbFN, dieProbFP, divProbMN, dieProbMN, divProbMP, dieProbMP;
    public final double migrProbM, dieProbEN, divProbEP, dieProbEP, deactProbE, dieProbL;
    public final double macrophageDaughterActivationBoost, endothelialDaughterDivisionBoost;
    public final double fibroblastSignalBoost, tumorEndothelialRadius, macrophageInteractionRadius;
    public final double macrophageEndothelialBiasRadius, endothelialMacrophageRadius, fibroblastTumorRadius;
    public final int fibroblastSignalCap, clusterRadius, initialJnkPositiveTenths;
    public final int initialMacrophageCount, initialLungCount, initPop;
    private final Map<String, Double> values;

    private ModelParameters(Map<String, Double> source) {
        LinkedHashMap<String, Double> v = new LinkedHashMap<>();
        for (Definition def : SCREENED) v.put(def.name, source.getOrDefault(def.name, def.baseline));
        values = Collections.unmodifiableMap(v);
        netN=g(v,"netN"); dieProbN=g(v,"dieProbN"); pOnMax=g(v,"pOnMax"); pOffMax=g(v,"pOffMax");
        divProbP=g(v,"divProbP"); dieProbP=g(v,"dieProbP"); cafDivBoost=g(v,"cafDivBoost"); ecSurvival=g(v,"ecSurvival");
        activProbF=g(v,"activProbF"); divProbFP=g(v,"divProbFP"); activProbM=g(v,"activProbM"); activProbE=g(v,"activProbE");
        recruitBias=g(v,"recruitBias"); lambdaCAF=g(v,"lambdaCAF"); stressStrength=g(v,"stressStrength"); lambdaStress=g(v,"lambdaStress");
        migrProbP=g(v,"migrProbP"); migrProbN=g(v,"migrProbN"); divProbFN=g(v,"divProbFN"); dieProbFN=g(v,"dieProbFN"); dieProbFP=g(v,"dieProbFP");
        divProbMN=g(v,"divProbMN"); dieProbMN=g(v,"dieProbMN"); divProbMP=g(v,"divProbMP"); dieProbMP=g(v,"dieProbMP"); migrProbM=g(v,"migrProbM");
        dieProbEN=g(v,"dieProbEN"); divProbEP=g(v,"divProbEP"); dieProbEP=g(v,"dieProbEP"); deactProbE=g(v,"deactProbE"); dieProbL=g(v,"dieProbL");
        macrophageDaughterActivationBoost=g(v,"macrophageDaughterActivationBoost"); endothelialDaughterDivisionBoost=g(v,"endothelialDaughterDivisionBoost");
        fibroblastSignalBoost=g(v,"fibroblastSignalBoost"); fibroblastSignalCap=gi(v,"fibroblastSignalCap");
        tumorEndothelialRadius=g(v,"tumorEndothelialRadius"); macrophageInteractionRadius=g(v,"macrophageInteractionRadius");
        macrophageEndothelialBiasRadius=g(v,"macrophageEndothelialBiasRadius"); endothelialMacrophageRadius=g(v,"endothelialMacrophageRadius");
        fibroblastTumorRadius=g(v,"fibroblastTumorRadius"); clusterRadius=gi(v,"clusterRadius"); initialJnkPositiveTenths=gi(v,"initialJnkPositiveTenths");
        initialMacrophageCount=gi(v,"initialMacrophageCount"); initialLungCount=gi(v,"initialLungCount"); initPop=gi(v,"initPop");
    }

    private static double g(Map<String, Double> v, String n) { return v.get(n); }
    private static int gi(Map<String, Double> v, String n) { return (int)Math.rint(v.get(n)); }

    public static ModelParameters currentBaseline() { return currentBaseline(25); }
    public static ModelParameters currentBaseline(int initPop) {
        LinkedHashMap<String, Double> v = new LinkedHashMap<>();
        for (Definition def : SCREENED) v.put(def.name, def.baseline);
        v.put("initPop", (double)initPop);
        ModelParameters out = new ModelParameters(v);
        out.validate();
        return out;
    }

    public ModelParameters copy() { return new ModelParameters(values); }

    public ModelParameters with(String name, double value) {
        Definition def = BY_NAME.get(name);
        if (def == null) throw new IllegalArgumentException("unknown parameter: " + name);
        if (!def.screen) throw new IllegalArgumentException("parameter is not in the untreated Morris screen: " + name);
        LinkedHashMap<String, Double> v = new LinkedHashMap<>(values);
        v.put(name, def.isInteger() ? Math.rint(value) : value);
        return new ModelParameters(v);
    }

    public double get(String name) {
        Double value = values.get(name);
        if (value == null) throw new IllegalArgumentException("parameter is not screened: " + name);
        return value;
    }

    public Map<String, Double> values() {
        return values;
    }

    public void validate() {
        List<String> errors = validationErrors();
        if (!errors.isEmpty()) throw new IllegalArgumentException(String.join("; ", errors));
    }

    public List<String> validationErrors() {
        List<String> errors = new ArrayList<>();
        for (Definition def : SCREENED) {
            double value = values.get(def.name);
            if (!Double.isFinite(value)) errors.add(def.name + " is non-finite");
            if (value < def.lower - 1e-12 || value > def.upper + 1e-12) errors.add(def.name + " outside bounds");
            if (def.isInteger() && value != Math.rint(value)) errors.add(def.name + " must be integer");
        }
        if (dieProbN + netN >= 1.0) errors.add("derived divProbN must be below one");
        if (dieProbP + divProbP * (1.0 + cafDivBoost) >= 1.0) errors.add("JNK-positive death/division ladder reaches one");
        if (dieProbFP + divProbFP + fibroblastSignalBoost * fibroblastSignalCap >= 1.0) errors.add("CAF death/division ladder reaches one");
        if (dieProbMN + divProbMN + migrProbM + activProbM >= 1.0) errors.add("resting macrophage event ladder reaches one");
        if (dieProbMP + divProbMP + migrProbM >= 1.0) errors.add("activated macrophage event ladder reaches one");
        if (initialJnkPositiveTenths < 0 || initialJnkPositiveTenths > 10) errors.add("initial JNK-positive tenths outside 0..10");
        return errors;
    }

    public double[] legacyTheta() {
        return new double[]{netN,dieProbN,pOnMax,pOffMax,divProbP,dieProbP,cafDivBoost,
                ecSurvival,activProbF,divProbFP,activProbM,activProbE};
    }

    public static List<Definition> registry() { return REGISTRY; }
    public static List<Definition> screenedDefinitions() { return SCREENED; }
    public static Definition definition(String name) { return BY_NAME.get(name); }

    public static String csvHeader() {
        StringBuilder b = new StringBuilder();
        for (Definition def : SCREENED) { if (b.length()>0) b.append(','); b.append(def.name); }
        return b.toString();
    }

    public String csvRow() {
        StringBuilder b = new StringBuilder();
        for (Definition def : SCREENED) { if (b.length()>0) b.append(','); b.append(format(get(def.name))); }
        return b.toString();
    }

    public static void writeRegistry(Path csv, Path markdown) throws IOException {
        String[] columns = {"canonical_name","java_file","class","method","code_location_or_expression","biological_interpretation",
                "baseline_value","current_status","lower_bound","upper_bound","transformation","bounds_source","evidence_quality",
                "expected_affected_outputs","prior_mechanism_harness_effect","active_under_baseline","enter_morris_screen","inclusion_exclusion_justification","group","range_or_ladder_risk"};
        try (BufferedWriter w = Files.newBufferedWriter(csv, StandardCharsets.UTF_8)) {
            w.write(String.join(",", columns)); w.newLine();
            for (Definition x : REGISTRY) {
                String[] row = {x.name,x.file,x.className,x.method,x.location,x.interpretation,format(x.baseline),x.status,
                        format(x.lower),format(x.upper),x.transform.name().toLowerCase(Locale.ROOT),x.boundSource,x.evidenceQuality,
                        x.expectedOutputs,x.mechanismEffect,x.activeAtBaseline,Boolean.toString(x.screen),x.justification,x.group,x.rangeRisk};
                for (int i=0;i<row.length;i++) { if(i>0) w.write(','); w.write(csv(row[i])); } w.newLine();
            }
        }
        try (BufferedWriter w = Files.newBufferedWriter(markdown, StandardCharsets.UTF_8)) {
            w.write("# Global Parameter Registry\n\n");
            w.write("Generated from the executable Java call sites, the supplied `ABC_TNBC_parameter_reference.md.pdf`, current ABC priors, and the prior paired mechanism harness. The PDF is treated as a project reference, not as proof that its per-step ranges were directly measured. Current-code/PDF disagreements are retained explicitly.\n\n");
            w.write("Screened parameters: **"+SCREENED.size()+"**. Complete audited quantities: **"+REGISTRY.size()+"**. Treatment parameters are excluded only from this untreated screen. Structurally inactive fields remain registered.\n\n");
            w.write("| Name | Baseline | Bounds | Transform | Status | Active | Morris | Bound source / decision | Outputs | Risk |\n");
            w.write("|---|---:|---:|---|---|---|---|---|---|---|\n");
            for (Definition x : REGISTRY) {
                w.write("| `"+x.name+"` | "+format(x.baseline)+" | ["+format(x.lower)+", "+format(x.upper)+"] | "+x.transform.name().toLowerCase(Locale.ROOT)+" | "+md(x.status)+" | "+md(x.activeAtBaseline)+" | "+x.screen+" | "+md(x.boundSource+"; "+x.justification)+" | "+md(x.expectedOutputs)+" | "+md(x.rangeRisk)+" |\n");
            }
            w.write("\n## Scientific Caveats\n\n");
            w.write("- The supplied PDF's fixed values are older than several current Java defaults. The registry uses current executable baselines.\n");
            w.write("- Literature figures support mechanisms and output constraints more strongly than exact per-step probabilities. Low evidence is not upgraded silently.\n");
            w.write("- Log sampling is used for positive rates spanning meaningful ratios; zero-inclusive ranges remain linear. Integer geometry/count parameters are rounded to valid discrete values.\n");
            w.write("- Independent Morris perturbations deliberately do not enforce proposed biological equalities such as `divProbMN=dieProbMN` or `migrProbP=10*migrProbN`; those dependencies are uncertain assumptions, and their violation is recorded as an interpretation risk.\n");
            w.write("- Event-ladder constraints are validated for every physical point. The macrophage migration upper bound is explicitly 0.85, rather than the harness's 0.95, because the latter can make event probabilities sum above one when combined with other allowed maxima.\n");
        }
    }

    private static String format(double x) {
        if (!Double.isFinite(x)) return Double.toString(x);
        if (x == Math.rint(x) && Math.abs(x) < 1e15) return Long.toString((long)x);
        return String.format(Locale.US, "%.12g", x);
    }
    private static String csv(String s) { return "\"" + s.replace("\"", "\"\"").replace("\r", " ").replace("\n", " ") + "\""; }
    private static String md(String s) { return s.replace("|", "\\|").replace("\n", " "); }

    public static void main(String[] args) throws IOException {
        Path csv = Path.of(args.length > 0 ? args[0] : "GLOBAL_PARAMETER_REGISTRY.csv");
        Path md = Path.of(args.length > 1 ? args[1] : "GLOBAL_PARAMETER_REGISTRY.md");
        writeRegistry(csv, md);
        System.out.println("wrote " + csv + " and " + md + " (" + SCREENED.size() + " screened / " + REGISTRY.size() + " audited)");
    }
}
