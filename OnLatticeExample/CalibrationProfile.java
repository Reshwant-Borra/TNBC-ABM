package OnLatticeExample;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.SplittableRandom;

/** Named ABC calibration profiles. New profiles mutate ModelParameters by canonical name. */
public final class CalibrationProfile {
    public static final class Parameter {
        public final String name;
        public final ModelParameters.Definition definition;
        public final ModelParameters.Transform samplingTransform;
        public final String role;
        public final String priorDistribution;
        public final String decision;
        public final String justification;
        public final String mentorApprovalStatus;

        Parameter(String name, ModelParameters.Transform samplingTransform, String role,
                  String priorDistribution, String decision, String justification,
                  String mentorApprovalStatus) {
            this.name = name;
            this.definition = ModelParameters.definition(name);
            if (this.definition == null) throw new IllegalArgumentException("unknown parameter in calibration profile: " + name);
            this.samplingTransform = samplingTransform;
            this.role = role;
            this.priorDistribution = priorDistribution;
            this.decision = decision;
            this.justification = justification;
            this.mentorApprovalStatus = mentorApprovalStatus;
        }

        double sample(SplittableRandom rng) {
            double x = rng.nextDouble();
            switch (samplingTransform) {
                case LOG:
                    if (!(definition.lower > 0.0)) throw new IllegalStateException(name + ": log lower bound must be positive");
                    return Math.exp(Math.log(definition.lower) + x * (Math.log(definition.upper) - Math.log(definition.lower)));
                case INTEGER:
                    return Math.rint(definition.lower + x * (definition.upper - definition.lower));
                case LINEAR:
                    return definition.lower + x * (definition.upper - definition.lower);
                default:
                    throw new IllegalStateException(name + ": unsupported sampling transform " + samplingTransform);
            }
        }
    }

    private static final List<String> STRUCTURALLY_INACTIVE = Collections.unmodifiableList(Arrays.asList(
            "migrProbF", "activProbMP", "divProbEN", "migrProbE", "divProbL", "unusedNeighborCountRadius"));
    private static final List<String> UNAPPROVED_TIER_B = Collections.unmodifiableList(Arrays.asList(
            "divProbMP", "dieProbMP", "divProbFN", "stressStrength", "divProbMN", "dieProbMN",
            "dieProbFN", "migrProbP", "dieProbEN", "clusterRadius", "lambdaStress", "dieProbL", "dieProbEP"));

    private final String name;
    private final String description;
    private final List<Parameter> parameters;

    private CalibrationProfile(String name, String description, List<Parameter> parameters) {
        this.name = name;
        this.description = description;
        this.parameters = Collections.unmodifiableList(new ArrayList<>(parameters));
        validate();
    }

    public static CalibrationProfile byName(String name) {
        if ("core4".equals(name)) return core4();
        if ("legacy12".equals(name)) return legacy12();
        throw new IllegalArgumentException("unknown calibration profile: " + name + " (expected core4 or legacy12)");
    }

    public static CalibrationProfile core4() {
        ArrayList<Parameter> p = new ArrayList<>();
        addRegistry(p, "divProbP", "inferred",
                "CALIBRATE", "Tier A: top-third Morris rank, confirmed SNR 1.46, already ABC-inferred", "not required; already inferred");
        addRegistry(p, "pOffMax", "inferred",
                "CALIBRATE", "Tier A: top-third Morris rank, confirmed SNR 1.29, already ABC-inferred", "not required; already inferred");
        addRegistry(p, "divProbFP", "inferred",
                "CALIBRATE", "Tier A: middle-third Morris rank and already ABC-inferred", "not required; already inferred");
        addRegistry(p, "dieProbN", "inferred",
                "CALIBRATE", "Tier A: middle-third Morris rank, failure-associated, already ABC-inferred", "not required; already inferred");
        return new CalibrationProfile("core4", "Post-Morris reduced untreated ABC profile", p);
    }

    public static CalibrationProfile legacy12() {
        String[] names = {
                "netN", "dieProbN", "pOnMax", "pOffMax", "divProbP", "dieProbP",
                "cafDivBoost", "ecSurvival", "activProbF", "divProbFP", "activProbM", "activProbE"
        };
        ArrayList<Parameter> p = new ArrayList<>();
        for (String name : names) {
            p.add(new Parameter(name, ModelParameters.Transform.LINEAR, "legacy inferred",
                    "Uniform(lower_bound, upper_bound) in physical space",
                    "LEGACY_COMPATIBILITY",
                    "Preserves the old ABCRejection positional prior behavior; registry transforms are not applied in this profile",
                    "legacy behavior, not a new scientific recommendation"));
        }
        return new CalibrationProfile("legacy12", "Original 12-parameter rejection ABC profile", p);
    }

    private static void addRegistry(ArrayList<Parameter> p, String name, String role, String decision,
                                    String justification, String approval) {
        ModelParameters.Definition d = ModelParameters.definition(name);
        p.add(new Parameter(name, d.transform, role,
                "Uniform on " + d.transform.name().toLowerCase(Locale.ROOT) + " scale over registry bounds",
                decision, justification, approval));
    }

    public String name() { return name; }
    public String description() { return description; }
    public List<Parameter> parameters() { return parameters; }

    public List<String> parameterNames() {
        ArrayList<String> out = new ArrayList<>();
        for (Parameter p : parameters) out.add(p.name);
        return Collections.unmodifiableList(out);
    }

    public boolean includes(String parameterName) {
        for (Parameter p : parameters) if (p.name.equals(parameterName)) return true;
        return false;
    }

    public ModelParameters propose(long proposalSeed, int initPop) {
        SplittableRandom rng = new SplittableRandom(proposalSeed);
        ModelParameters p = ModelParameters.currentBaseline(initPop);
        for (Parameter cp : parameters) p = p.with(cp.name, cp.sample(rng));
        p.validate();
        return p;
    }

    public ModelParameters applyValues(int initPop, double[] values) {
        if (values.length != parameters.size()) {
            throw new IllegalArgumentException("profile " + name + " expected " + parameters.size() + " values, got " + values.length);
        }
        ModelParameters p = ModelParameters.currentBaseline(initPop);
        for (int i = 0; i < values.length; i++) p = p.with(parameters.get(i).name, values[i]);
        p.validate();
        return p;
    }

    public String canonicalCsv() {
        StringBuilder b = new StringBuilder("profile,parameter,lower,upper,sampling_transform,prior_distribution,decision\n");
        for (Parameter p : parameters) {
            b.append(name).append(',').append(p.name).append(',')
                    .append(fmt(p.definition.lower)).append(',').append(fmt(p.definition.upper)).append(',')
                    .append(p.samplingTransform.name().toLowerCase(Locale.ROOT)).append(',')
                    .append(csv(p.priorDistribution)).append(',').append(csv(p.decision)).append('\n');
        }
        return b.toString();
    }

    public void validate() {
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (Parameter p : parameters) {
            if (!seen.add(p.name)) throw new IllegalStateException(name + ": duplicate parameter " + p.name);
            if (!Double.isFinite(p.definition.lower) || !Double.isFinite(p.definition.upper) || !(p.definition.lower < p.definition.upper)) {
                throw new IllegalStateException(name + ": invalid bounds for " + p.name);
            }
            double baseline = p.definition.baseline;
            if (baseline < p.definition.lower - 1e-12 || baseline > p.definition.upper + 1e-12) {
                throw new IllegalStateException(name + ": baseline outside bounds for " + p.name);
            }
            if (STRUCTURALLY_INACTIVE.contains(p.name)) throw new IllegalStateException(name + ": structurally inactive parameter inferred: " + p.name);
            if ("core4".equals(name) && UNAPPROVED_TIER_B.contains(p.name)) {
                throw new IllegalStateException("core4 contains unapproved Tier B parameter: " + p.name);
            }
        }
        if ("core4".equals(name) && !parameterNames().equals(Arrays.asList("divProbP", "pOffMax", "divProbFP", "dieProbN"))) {
            throw new IllegalStateException("core4 parameter list does not match final Morris decision");
        }
    }

    public static List<String> structurallyInactiveParameters() { return STRUCTURALLY_INACTIVE; }
    public static List<String> unapprovedTierBParameters() { return UNAPPROVED_TIER_B; }

    static String fmt(double x) {
        if (!Double.isFinite(x)) return Double.toString(x);
        if (x == Math.rint(x) && Math.abs(x) < 1e15) return Long.toString((long) x);
        return String.format(Locale.US, "%.12g", x);
    }

    static String csv(String s) {
        if (s == null) return "\"\"";
        return "\"" + s.replace("\"", "\"\"").replace("\r", " ").replace("\n", " ") + "\"";
    }
}
