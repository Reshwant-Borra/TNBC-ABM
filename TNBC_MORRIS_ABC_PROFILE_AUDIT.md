# TNBC Morris Sensitivity Audit and First ABC Profile Recommendation

**Scope:** Evidence in `results.zip` only. No model code was changed and no simulations were run for this audit.

## 1. Executive conclusion

The independent 20-trajectory × 5-seed Morris run confirms the main biological structure of the earlier screen, but it does **not** justify carrying the old `core4` forward unchanged.

The strongest reproducible ABC-inferred parameters are:

1. `divProbP` — primary rank 7, independent rank 7; the dominant tumor-growth target driver at steps 480, 960, and 1440.
2. `pOffMax` — primary rank 8, independent rank 3; the dominant JNK target driver at steps 480 and 1440 and a major tumor driver.

`dieProbN` is stable but much weaker: rank 30 in both runs. In the independent run it ranks 12th for the late tumor target, 21st–43rd for most other ABC targets, and has a moderate tumor-extinction association (`rank-biserial = +0.270`). It is also structurally coupled to `netN` through the reported relation `divProbN = dieProbN + netN`. This makes it influential enough to retain for a later turnover/extinction test, but not well supported as an immediately identifiable parameter.

`divProbFP` should be removed from the first profile. It fell from rank 20 (moderate) to rank 32 (low), was only 12th for the independent fibroblast target, and is dominated on that target by fixed `divProbFN`, `dieProbFN`, `dieProbFP`, and by `activProbF`.

`activProbF` is the better fibroblast expansion candidate. It rose from rank 34 to 15 and was fourth for the independent fibroblast target with a consistent positive effect (`mu = mu* = 0.669`, SNR 2.49). It should not yet be included automatically because the old/new rank movement is large and the package omits the registry/provenance needed to approve its range.

### Final profile decision

For the **first diagnostic 100-draw ABC pilot**, use an evidence-driven **core2**:

- `divProbP`
- `pOffMax`

This is more defensible than `core3` for an identifiability diagnostic because the two parameters have strong, reproducible, and partly distinct observable signatures: `divProbP` controls tumor growth; `pOffMax` controls JNK and also tumor burden. Treat:

- `activProbF` as the first target-driven expansion if the fibroblast target remains systematically wrong after range review;
- `dieProbN` as a later tumor-turnover/extinction diagnostic after joint review with `netN`;
- all influential fixed macrophage, fibroblast, EC, stress, and migration parameters as review items, not automatic ABC additions.

### Blocking issue before that pilot

The current ABC acceptance behavior fails a necessary QC rule. In both `abc-core4-smoke` and its rerun, draw 0 has `outcome_status = STROMAL_COMPARTMENT_LOSS`, `failure_reason = EC_POPULATION_ZERO`, and an invalid EC target, yet appears in `abc_accepted.csv` with `accepted = true`. The legacy-12 smoke does the same. A posterior must not include a run missing a required target merely because a finite penalty happened to fall below the quantile threshold.

**Do not start the 100-draw pilot until `accepted => VALID_FINITE and every target valid` is verified.**

## 2. Files inspected

### Substantive run groups

| Folder | Role | Audit result |
|---|---|---|
| `morris-pilot-10/` | Ten-trajectory, one-seed pilot; range/failure review and pilot-to-primary context | QC passed; useful for convergence history, not the final parameter decision |
| `morris-primary-20/` | Earlier 20-trajectory, one-seed primary run | Supplies the old global ranks, classifications, target rankings, raw EEs, failures, and design |
| `morris-primary-20/confirmation/` | Targeted 18-parameter, three-seed confirmation on selected primary points | Supplies prior SNR evidence; it did not confirm all 45 parameters |
| `morris-independent-confirm-20x5/` | New 20-trajectory, all-45-parameter, five-seed run | Main confirmatory evidence; 4,600 simulations |
| `morris-smoke/`, `morris-independent-smoke-2x2/`, `dev-smoke-1/`, `checkpoint-v4/` | Pipeline, resume, and developmental checks | Not used for final ranking claims |
| `abc-core4-dryrun/` | Profile/config dry run | No simulations; manifest only |
| `abc-core4-smoke/` and `abc-core4-smoke-rerun/` | Four-draw reproducibility smoke | Simulated outputs reproduce exactly apart from runtime; exposes invalid-run acceptance issue |
| `abc-legacy12-smoke/` | Two-draw legacy profile smoke | Also accepts an EC-loss run; not evidence of calibration quality |

### Evidence files and what they establish

| File or file family | Role |
|---|---|
| `INDEPENDENT_CONFIRMATORY_MORRIS_REPORT.md` | Run design, commands, hashes, status counts, summary interpretation, and provisional recommendation |
| `COMBINED_CALIBRATION_RECOMMENDATION.csv` | Old/new rank-based recommendation table; a recommendation, not independent evidence |
| `INDEPENDENT_PARAMETER_CLASSIFICATION.csv`, `PRIMARY_PARAMETER_CLASSIFICATION.csv` | One row per screened parameter with status, default, bounds, six family scores, overall rank, validity, and tertile classification |
| `morris_global_rankings.csv` | Global priority ranking; score is the mean of six equally weighted family scores |
| `INDEPENDENT_TOP_PARAMETERS_BY_OUTPUT.csv`, `PRIMARY_TOP_PARAMETERS_BY_OUTPUT.csv` | Per-output top ten with signed `mu`, `mu*`, `sigma`, SNR, valid fraction, and effect character |
| `morris_summary_by_output.csv` | Full 45 × 127 parameter-output Morris summaries; basis for checking candidate effects beyond the top ten |
| `morris_elementary_effects.csv` and `.gz` | Raw matched-replicate and replicate-mean elementary effects |
| `morris_outputs.csv` and `.gz` | Long-form output table; 4,600 × 127 output rows in the independent run |
| `morris_raw_runs.csv` | One row per simulation with parameters, seeds, status, flags, and snapshots |
| `morris_design.csv` | All 920 design points, normalized coordinates, physical values, trajectory/step order, and changed parameter |
| `morris_failures.csv`, `failure_sensitivity.csv`, `INDEPENDENT_FAILURE_DRIVERS.csv` | Nonfinite runs and univariate failure/extinction associations |
| `RANK_STABILITY_*.csv`, `RANK_STABILITY_REPORT.md` | Old/new rank, score, family-score comparison, Spearman correlations, and top-set overlap |
| `MORRIS_QC_REPORT.md` | Eleven design/execution checks; all passed |
| `analysis_manifest.json`, `analysis_output_inventory.csv` | Output/figure counts, finite/invalid coverage, and analysis environment |
| `PARAMETER_INFLUENCE_CLASSIFICATION.csv` | Output-by-parameter A–F classification; also lists the 29 excluded quantities and the stated exclusion reason |
| `figures/` | Heatmap, failure map, 126 per-output `mu*`–`sigma` plots, EE distributions, top-`mu*` plots, scatterplots, and runtime/validity plots |
| ABC `run_manifest.json`, `resolved_config.json`, `abc_all_draws.csv`, `abc_accepted.csv`, `best_run_target_breakdown.csv`, `distance_summary.csv` | Profile, target hash, parameter values, residuals, outcome status, acceptance, and reproducibility smoke evidence |

The raw independent inventory is internally consistent:

- 920 design points = 20 trajectories × 46 points.
- 4,600 run rows = 920 points × 5 seeds.
- 584,200 output rows = 4,600 × 127.
- 114,300 replicate-mean parameter-output effects = 45 × 20 × 127.
- 108,260 valid and 6,040 lost replicate-mean effects.
- Status: 1,963 finite, 1,687 invalid, 950 tumor-extinct, 0 exceptions.

### Missing evidence

The reports cite files that are **not in the ZIP**:

- `GLOBAL_PARAMETER_REGISTRY.csv`
- `GLOBAL_PARAMETER_REGISTRY.md`
- `ModelParameters.java`
- `ExampleGrid.java`
- `analyze_morris.py`
- `morris_stage_analysis.py`
- `morris_rank_stability.py`
- detailed provenance/mechanism audit documents

Consequences:

- Defaults and tested bounds can be verified from the result CSVs.
- The global-score formula can be recovered from the `weighting_rule` column.
- Exact units, per-step physical duration, literature citations, transform choice per parameter, and code-branch reachability cannot be independently re-audited from this package.
- Statements that a parameter is blocked/unused are reported results, not source-code verification in this audit.

The ZIP also contains macOS resource-fork files (`__MACOSX/._*`) and binary checkpoint files. These are execution artifacts, not additional scientific evidence.

### Figure audit warnings

Representative heatmaps, failure maps, `mu*`–`sigma` plots, EE distributions, and scatterplots agree with the CSVs. However:

- `figures/one_vs_multi_seed_rank_comparison.png` is a placeholder saying “Multi-seed confirmation not yet available,” even in the five-seed independent folder. It is stale/misgenerated and must not be used.
- `analysis_manifest.json` still counts that placeholder as a rank-comparison figure.
- `analysis_manifest.json` reports `scipy_available = false`; the absent analysis scripts prevent verification of the exact fallback implementation.
- `resolved_config.json` remains a pre-run `PENDING` snapshot; `run_manifest.json` is the completed record.

## 3. Morris design explained

### What one trajectory is

A Morris trajectory is a path through parameter space. It starts from one randomly selected combination of values and then changes **one parameter at a time**. With 45 parameters, the path contains:

- one starting model evaluation; and
- 45 additional evaluations, one after each parameter is changed.

That is why each trajectory needs `45 + 1 = 46` model evaluations. Several parameters may differ between the beginning and end of the path, but each adjacent pair differs in exactly one parameter. The elementary effect for a parameter is computed only from its adjacent pair, so the effect is not attributed to simultaneous changes.

### Why 20 trajectories and five seeds

One trajectory tests each parameter in only one background combination. Twenty trajectories place each one-at-a-time change into 20 different parameter backgrounds. That reveals whether an effect is stable or context-dependent.

The ABM is stochastic: the same parameters can produce different outcomes because events and placements are random. For each trajectory point, the independent run used five simulation seeds. The same seed was used on the two sides of an elementary-effect pair (“common random numbers”), reducing noise in their difference.

The total is:

`20 trajectories × 46 points × 5 seeds = 4,600 simulations`.

### Elementary effects and summary statistics

For a parameter \(x_i\) and output \(y\), an elementary effect is approximately:

\[
EE_i = \frac{y(x_i+\Delta)-y(x_i)}{\Delta}
\]

with all other normalized coordinates unchanged for that adjacent step. It is a local one-at-a-time slope measured in one background.

- `mu`: the signed mean EE. Positive means higher parameter values usually increase the output; negative means they usually decrease it. Opposite-signed effects can cancel.
- `mu_star`: the mean absolute EE. This measures effect magnitude without cancellation.
- `sigma`: how much EEs differ across trajectories. High values can reflect nonlinearity, interaction with other parameters, thresholds/discreteness, or remaining stochastic noise.

High `mu_star` and low/moderate `sigma` suggest a strong, fairly consistent effect. High `mu_star` and high `sigma` suggest a strong effect whose size or direction depends on context. In the five-seed run, direction fractions and SNR help separate reproducible context dependence from noise.

A low sensitivity score means only that the parameter changed the measured outputs relatively little **over the tested range, time horizon, model state, and target definitions**. It does not prove that the mechanism is biologically unimportant.

### What Morris does and does not cover

Morris samples 20 one-at-a-time paths through a 45-dimensional box. It does not enumerate all parameter combinations. Even a two-level exhaustive design would require \(2^{45}\) combinations, which is infeasible.

Morris supports:

- screening influential versus lower-influence parameters;
- target-specific direction and magnitude comparisons;
- detection of context-dependent effects;
- identification of failure/extinction boundaries;
- prioritization of parameters for biological and range review.

Morris does **not** support:

- posterior estimates or calibration;
- proof of identifiability;
- a complete variance decomposition such as Sobol indices;
- unique causal attribution for failure associations;
- claims outside the tested ranges;
- deletion of low-influence mechanisms.

## 4. Previous versus independent run comparison

The old primary run used 20 trajectories and one seed per point. The independent run used a new design seed, 20 new trajectories, and five seeds per point. The new design had zero identical normalized rows at matching positions. Model logic and target hashes were reported unchanged.

### Stability statistics

- Overall Spearman rank correlation: **0.823**
- Top-5 overlap: **3/5**
- Top-10 overlap: **9/10**
- Top-15 overlap: **12/15**
- Median absolute rank movement: **4**
- Maximum movement: **19**

Family Spearman correlations were tumor 0.886, fibroblast 0.867, macrophage 0.759, endothelial 0.643, JNK 0.577, and failure only 0.075.

Spearman 0.823 means the overall ordering is strongly similar, not identical. The top ten agree well because the same macrophage/fibroblast turnover, tumor/JNK, stress, EC-death, and migration mechanisms dominate. The top five agree less well because several high scores are close and the five-seed averaging plus new backgrounds reorder the upper pool. In particular, `pOffMax` and `dieProbFN` move into the top five while `stressStrength` and `divProbMN` move out; neither leaving parameter becomes unimportant.

The biological conclusion is stable: fixed macrophage/fibroblast turnover dominates many outputs, `divProbP` dominates tumor growth, and `pOffMax` dominates JNK. The exact ordering inside that structure is less stable.

Failure-driver order is not stable. The independent run has 950 tumor-extinct runs, 1,482 EC-zero runs, 603 macrophage-zero runs, and 257 fibroblast-zero runs, but univariate failure ranks change sharply between designs. These associations are screening signals, not causal coefficients.

### Exact top-set changes

- Primary top five: `divProbMP`, `dieProbMP`, `divProbFN`, `stressStrength`, `divProbMN`
- Independent top five: `dieProbMP`, `divProbMP`, `pOffMax`, `divProbFN`, `dieProbFN`
- Entered top five: `pOffMax`, `dieProbFN`
- Left top five: `stressStrength`, `divProbMN`
- Entered top ten: `dieProbEN`
- Left top ten: `migrProbP` (to rank 11)

### Full old/new comparison

Negative rank change means a stronger new rank.

| Parameter | Old rank/score/class | New rank/score/class | Δ rank | Strongest new evidence | Prior status | Interpretation |
|---|---|---|---:|---|---|---|
| `dieProbMP` | 2 / .497 / high | 1 / .472 / high | -1 | macrophage, EC, failure | fixed | stable |
| `divProbMP` | 1 / .517 / high | 2 / .414 / high | +1 | macrophage, EC, tumor | fixed | stable |
| `pOffMax` | 8 / .362 / high | 3 / .414 / high | -5 | JNK, tumor, extinction | inferred | strengthened |
| `divProbFN` | 3 / .424 / high | 4 / .350 / high | +1 | fibroblast | fixed | stable |
| `dieProbFN` | 9 / .351 / high | 5 / .347 / high | -4 | fibroblast, fibroblast loss | fixed | stable/stronger |
| `divProbMN` | 5 / .403 / high | 6 / .341 / high | +1 | macrophage | fixed | stable |
| `divProbP` | 7 / .371 / high | 7 / .339 / high | 0 | tumor | inferred | stable |
| `stressStrength` | 4 / .423 / high | 8 / .332 / high | +4 | JNK, tumor | fixed | stable but lower |
| `dieProbEN` | 11 / .345 / high | 9 / .328 / high | -2 | EC loss | fixed | stable |
| `dieProbMN` | 6 / .387 / high | 10 / .320 / high | +4 | macrophage, loss | fixed | stable but lower |
| `migrProbP` | 10 / .349 / high | 11 / .263 / high | +1 | tumor radius/spread | fixed | stable |
| `lambdaStress` | 13 / .325 / high | 12 / .243 / high | -1 | JNK | fixed | stable |
| `divProbEP` | 24 / .255 / moderate | 13 / .222 / high | -11 | EC division | fixed | strengthened |
| `dieProbFP` | 26 / .245 / moderate | 14 / .221 / high | -12 | fibroblast | fixed | strengthened |
| `activProbF` | 34 / .211 / low | 15 / .216 / high | -19 | fibroblast target | inferred | contradictory across runs; new signal strong |
| `initialJnkPositiveTenths` | 29 / .232 / moderate | 16 / .215 / moderate | -13 | failure/initialization | hard-coded | strengthened but assumption-driven |
| `pOnMax` | 25 / .254 / moderate | 17 / .214 / moderate | -8 | JNK, late tumor | inferred | strengthened |
| `migrProbM` | 19 / .284 / moderate | 18 / .213 / moderate | -1 | macrophage/EC | fixed | stable |
| `macrophageInteractionRadius` | 22 / .272 / moderate | 19 / .210 / moderate | -3 | EC/spatial | hard-coded | stable |
| `clusterRadius` | 12 / .338 / high | 20 / .210 / moderate | +8 | tumor establishment/spread | fixed | weakened |
| `lambdaCAF` | 32 / .223 / low | 21 / .208 / moderate | -11 | JNK | fixed | strengthened |
| `dieProbP` | 21 / .272 / moderate | 22 / .197 / moderate | +1 | EC-failure association, tumor | inferred | rank stable, magnitude lower |
| `endothelialMacrophageRadius` | 31 / .225 / low | 23 / .194 / moderate | -8 | early EC | hard-coded | strengthened |
| `dieProbEP` | 15 / .320 / high | 24 / .193 / moderate | +9 | EC | fixed | weakened |
| `initialMacrophageCount` | 23 / .262 / moderate | 25 / .192 / moderate | +2 | initialization/macrophage | hard-coded | stable |
| `dieProbL` | 14 / .321 / high | 26 / .189 / moderate | +12 | late JNK | fixed | globally weakened; target-specific signal remains |
| `recruitBias` | 17 / .292 / moderate | 27 / .184 / moderate | +10 | tumor/macrophage | fixed | weakened |
| `netN` | 33 / .215 / low | 28 / .164 / moderate | -5 | late tumor/failure | inferred | slightly strengthened; coupled |
| `initPop` | 16 / .300 / moderate | 29 / .161 / moderate | +13 | establishment/failure | fixed CLI | weakened |
| `dieProbN` | 30 / .226 / moderate | 30 / .161 / moderate | 0 | late tumor/extinction | inferred | rank stable, magnitude lower |
| `activProbM` | 39 / .189 / low | 31 / .156 / low | -8 | weak EC/macrophage | inferred | still low/noisy |
| `divProbFP` | 20 / .283 / moderate | 32 / .156 / low | +12 | fibroblast, but only rank 12 target | inferred | materially weakened |
| `deactProbE` | 42 / .167 / low | 33 / .155 / low | -9 | EC/failure | fixed | still low continuous influence |
| `fibroblastTumorRadius` | 40 / .189 / low | 34 / .154 / low | -6 | conditional fibro/spatial | hard-coded | still low |
| `initialLungCount` | 18 / .286 / moderate | 35 / .152 / low | +17 | initialization | hard-coded | materially weakened |
| `activProbE` | 36 / .209 / low | 36 / .142 / low | 0 | EC | inferred | stable low |
| `macrophageEndothelialBiasRadius` | 35 / .209 / low | 37 / .130 / low | +2 | conditional spatial | hard-coded | stable low |
| `migrProbN` | 28 / .236 / moderate | 38 / .119 / low | +10 | tumor migration | fixed | weakened |
| `tumorEndothelialRadius` | 37 / .201 / low | 39 / .117 / low | +2 | conditional EC/spatial | hard-coded | stable low |
| `ecSurvival` | 41 / .184 / low | 40 / .116 / low | -1 | failure association, weak EC | inferred | stable low; prior SNR .56 |
| `cafDivBoost` | 38 / .194 / low | 41 / .109 / low | +3 | weak fibro/JNK | inferred | stable low |
| `fibroblastSignalBoost` | 27 / .239 / moderate | 42 / .099 / low | +15 | weak fibro signaling | hard-coded | materially weakened |
| `fibroblastSignalCap` | 45 / .126 / low | 43 / .081 / low | -2 | weak/conditional fibro signaling | hard-coded | stable low |
| `macrophageDaughterActivationBoost` | 43 / .163 / low | 44 / .073 / low | +1 | daughter-state mechanism | hard-coded | stable low |
| `endothelialDaughterDivisionBoost` | 44 / .133 / low | 45 / .062 / low | +1 | daughter-state mechanism | hard-coded | stable low |

The changes that materially affect the ABC profile are `pOffMax` strengthening, `divProbFP` weakening, and `activProbF` strengthening. Movement among fixed turnover parameters changes biological-review priority but does not by itself justify immediate inference.

## 5. Exact parameters that changed

### Calibration-relevant changes

- `pOffMax`: 8 → 3; its JNK effects became the clearest consistent signal.
- `divProbP`: 7 → 7; unchanged conclusion.
- `dieProbN`: 30 → 30; stable rank but lower score (.226 → .161), so “calibrate with caution” is stronger wording than the direct target evidence supports.
- `divProbFP`: 20 → 32; drops from moderate to low and loses its place as the fibroblast degree of freedom.
- `activProbF`: 34 → 15; becomes fourth on the fibroblast target with a consistent positive effect.
- `pOnMax`: 25 → 17; remains secondary to `pOffMax` and overlaps its JNK signature.
- `netN`: 33 → 28; still coupled to `dieProbN`.

### Fixed-parameter changes that matter

- Strengthened: `divProbEP` (24 → 13), `dieProbFP` (26 → 14), `lambdaCAF` (32 → 21), and `endothelialMacrophageRadius` (31 → 23).
- Weakened: `clusterRadius` (12 → 20), `dieProbEP` (15 → 24), `dieProbL` (14 → 26), `initPop` (16 → 29), `initialLungCount` (18 → 35), `migrProbN` (28 → 38), and `fibroblastSignalBoost` (27 → 42).

These shifts do not overturn the family-level conclusion: turnover parameters remain dominant for macrophage, fibroblast, and EC targets.

## 6. Target-specific biological interpretation

The current results contain **10 ABC targets**, not 11. Their values, weights, and scales are recoverable from `abc-core4-smoke-rerun/best_run_target_breakdown.csv`.

| Target | Target value | Strongest independent parameters | Direction/pattern | Stability | Interpretation |
|---|---:|---|---|---|---|
| JNK+ fraction, step 480 | .53 | `pOffMax`, `stressStrength`, `lambdaStress`, `lambdaCAF`, `pOnMax` | `pOffMax` negative and fully consistent; stress fields positive/contextual | `pOffMax` rises from old #3 to new #1; stress/lambda remain leading | A JNK-off rate and stress/CAF switching axis control early JNK |
| JNK+ fraction, step 1440 | .14 | `pOffMax`, `dieProbL`, `stressStrength`, `pOnMax`, `lambdaCAF` | `pOffMax` negative; others mostly positive/contextual | `pOffMax` #1 both; supporting order moves | JNK target uniquely supports `pOffMax`; `pOnMax` is partly redundant |
| EC activated fraction, step 480 | .15 | `divProbMP`, `endothelialMacrophageRadius`, `dieProbMP`, `dieProbEP`, `dieProbEN` | mixed, interaction-dependent | turnover appears in both runs; exact leader unstable | Early EC state depends strongly on macrophage–EC context, not an inferred EC parameter alone |
| EC activated fraction, step 960 | .30 | `dieProbMP`, `divProbMP`, `dieProbEN`, `divProbMN`, `migrProbM` | mostly interaction-dependent | macrophage turnover stable; supporting ranks move | EC target is strongly confounded with macrophage population dynamics |
| EC activated fraction, step 1440 | .80 | `dieProbMP`, `divProbMP`, `dieProbEN`, `ecSurvival`, `deactProbE` | `ecSurvival` sign-varying/SNR .57; turnover reproducible | top three turnover parameters stable | EC target cannot identify `ecSurvival` reliably over its current range |
| Macrophage activated fraction, step 1440 | .77 | `dieProbMP`, `divProbMN`, `divProbMP`, `dieProbMN`, `migrProbM` | turnover effects large; several contextual | same top-four set in both runs, reordered | Resting/activated macrophage turnover is the governing axis |
| Fibroblast log-fold, step 1440 | 1.10 | `divProbFN`, `dieProbFN`, `dieProbFP`, `activProbF`, `divProbMN` | first four directions coherent: +, -, -, + | first three fixed rates #1–3 in both; `activProbF` newly #4 | `activProbF` offers a distinct activation lever but can compensate for uncertain fixed turnover |
| Tumor log-fold, step 480 | .42 | `divProbP`, `pOffMax`, `stressStrength`, `dieProbMP`, `migrProbP` | `divProbP` positive/consistent; `pOffMax` negative | `divProbP` #1 both; `pOffMax` strengthens | early tumor target separates direct proliferation from JNK switching |
| Tumor log-fold, step 960 | .78 | `divProbP`, `pOffMax`, `divProbMP`, `dieProbMP`, `stressStrength` | `divProbP` positive; `pOffMax` negative; turnover contextual | `divProbP` #1 both | macrophage turnover can compensate for tumor parameters |
| Tumor log-fold, step 1440 | 1.11 | `divProbP`, `divProbMP`, `dieProbMP`, `pOffMax`, `pOnMax` | `divProbP` positive; `pOffMax` negative | same tumor/macrophage structure in both runs | late burden alone cannot separate tumor-intrinsic from macrophage-mediated effects |

### Cross-target redundancy and confounding

- `divProbP` and `pOffMax` both affect tumor burden, but JNK targets distinguish them because `pOffMax` is JNK #1 while `divProbP` is only rank 10/14 on those targets.
- `pOffMax`, `pOnMax`, `stressStrength`, `lambdaStress`, and `lambdaCAF` overlap on JNK. Inferring several at once would be underdetermined without additional JNK observables.
- `activProbF`, `divProbFN`, `dieProbFN`, and `dieProbFP` overlap on the single fibroblast target. Only one should be varied initially, and fixed turnover must be biologically defensible.
- Macrophage and EC targets are coupled: `dieProbMP` and `divProbMP` dominate both.
- `dieProbN` and `netN` are mechanistically coupled; available targets do not provide a clear unique turnover signature.

## 7. Old `core4` evaluation

| Parameter | Biological role reported by results | Old → new rank | Main independent evidence | Range | First-profile decision |
|---|---|---:|---|---|---|
| `divProbP` | tumor division probability | 7 → 7 | tumor targets #1 at 480/960/1440; signed effects positive and consistent; prior 3-seed SNR 1.46 | default .006621; .005–.03 | retain |
| `pOffMax` | maximum JNK off-switch probability | 8 → 3 | JNK targets #1; negative consistent effect; tumor #2/#2/#4; prior SNR 1.29 | default .118803; .01–.20 | retain |
| `divProbFP` | activated fibroblast division probability | 20 → 32 | fibroblast target rank 12 (`mu* .366`); weaker than `activProbF` and fixed turnover | default .029806; .018–.038 | remove from first profile |
| `dieProbN` | JNK-negative tumor death/turnover probability | 30 → 30 | late tumor rank 12; most targets rank 14–43; extinction association +.270; coupled to `netN` | default .018575; .008–.025 | hold for staged turnover test |

`divProbFP` is not biologically unimportant. Its independent fibroblast-target effect is positive and has SNR 2.64. It is unsuitable for the first profile because:

- its global result is not reproducible across designs;
- it is not a leading fibroblast-target driver;
- fixed `divProbFN`, `dieProbFN`, and `dieProbFP` dominate the same target;
- its tested range is relatively narrow and its provenance file is absent;
- it offers little unique signature outside the fibroblast axis.

## 8. `activProbF` evaluation

`activProbF` changes fibroblast activation, whereas `divProbFP` changes division after activation. In the independent target-specific table:

- `divProbFN`: fibroblast target #1, `mu = +1.525`, SNR 5.40
- `dieProbFN`: #2, `mu = -1.226`, SNR 7.75
- `dieProbFP`: #3, `mu = -0.712`, SNR 3.66
- `activProbF`: #4, `mu = +0.669`, SNR 2.49
- `divProbFP`: #12, `mu = +0.364`, `mu* = .366`, SNR 2.64

`activProbF` therefore has a clearer direct target signature than `divProbFP`, and its activation role is more mechanistically distinct from the three fixed division/death rates. However:

- global rank moved 34 → 15;
- no old multi-seed confirmation exists for it;
- default/range provenance is missing from the package;
- one fibroblast target may not distinguish activation from turnover compensation;
- fixed turnover rates have stronger effects and unresolved ranges.

**Decision:** first expansion candidate after mentor/range review. Do not include it in the initial core2 pilot. If the fibroblast residual is persistently poor while tumor/JNK improve, run a separate staged `core2 + activProbF` pilot before changing any fixed turnover rate.

## 9. Influential fixed-parameter review

“Fixed” means ABC does not currently vary the parameter. It does not mean the parameter is unimportant or permanently fixed.

| Parameter | Old → new rank | Default; Morris range | Main targets/processes | Stability and range concern | Decision |
|---|---:|---|---|---|---|
| `dieProbMP` | 2 → 1 | .015; .001–.03 | macrophage, EC, tumor, macrophage loss | dominant in both; broad turnover range needs source | review value/range; keep fixed for first pilot |
| `divProbMP` | 1 → 2 | .01575; .001–.03 | macrophage, EC, tumor | dominant in both; paired balance with death unresolved | review jointly with `dieProbMP` |
| `divProbFN` | 3 → 4 | 0; 0–.01 | fibroblast target #1 | highly stable; zero default requires biological approval | review; do not infer with `activProbF` initially |
| `dieProbFN` | 9 → 5 | .008; .001–.008 | fibroblast target/loss | stable; report flags upper bound as implausibly fast turnover | biological/range review required |
| `divProbMN` | 5 → 6 | .005; .001–.01 | macrophage, EC, fibroblast | dominant in both | review resting/activated homeostatic constraints |
| `dieProbMN` | 6 → 10 | .005; .001–.01 | macrophage activation/loss | stable top ten | review jointly with `divProbMN` |
| `stressStrength` | 4 → 8 | 1.5; 0–3 | JNK and tumor | stable high; range reported as requiring approval | keep fixed pending source/range review |
| `dieProbEN` | 11 → 9 | .005; .001–.01 | EC population zero, EC targets | stable; EC death is uncompensated because `divProbEN` is inactive | mechanism and range review required |
| `migrProbP` | 10 → 11 | .1; 0–.3 | tumor radius/spread, failure | stable high; relation to `migrProbN` needs review | keep fixed for pilot; consider spatial validation |
| `lambdaStress` | 13 → 12 | 2; .5–5 | JNK/stress | stable high | range/provenance review |
| `divProbEP` | 24 → 13 | .0087; .001–.02 | EC division/late EC state | strengthens substantially | review EC turnover; not immediate inference |
| `dieProbFP` | 26 → 14 | .012; .001–.012 | fibroblast target #3 | strengthens substantially; default at upper bound | range review; keep fixed initially |
| `dieProbEP` | 15 → 24 | .008; .001–.02 | EC targets/loss | weakens but remains direct EC mechanism | keep fixed pending EC review |
| `clusterRadius` | 12 → 20 | 4; 2–8 | tumor establishment/radius | weakens; may be measured initial condition rather than biological rate | fix to measured condition if available |
| `dieProbL` | 14 → 26 | .002; .0005–.01 | late JNK target #2 | globally weaker but target-specific effect remains | review lung-cell role/range |
| `migrProbM` | 19 → 18 | .8; .1–.85 | macrophage and EC targets | stable moderate; broad high range | keep fixed pending migration data |
| `macrophageInteractionRadius` | 22 → 19 | 3.5; 1.5–5.5 | macrophage–EC/spatial | stable moderate; hard-coded assumption | spatial/mechanism review |
| `lambdaCAF` | 32 → 21 | 2; .5–5 | JNK | strengthens; overlaps stress and JNK on/off rates | keep fixed unless extra JNK data |
| `endothelialMacrophageRadius` | 31 → 23 | 1.5; 1–3.5 | early EC target #2 | strengthened; hard-coded radius | spatial provenance review |
| `recruitBias` | 17 → 27 | .04; 0–.2 | macrophage/tumor | weakened | keep fixed |
| `initialMacrophageCount` | 23 → 25 | 925; 463–1388 | initialization/macrophage | stable moderate; assumption requiring approval | use measured initial condition if possible |
| `initialJnkPositiveTenths` | 29 → 16 | 9; 5–10 | initialization/JNK/failure | rank strengthens, but it is an initial condition | measure/fix rather than infer if possible |
| `initPop` | 16 → 29 | 25; 10–50 | tumor establishment/extinction | materially weaker; boundary control | fix to experimental seeding condition |

The most important fixed-parameter implication is not “add all of these to ABC.” It is that a core2 posterior will be **conditional on their defaults**. If those defaults are biologically wrong, the inferred tumor/JNK parameters may compensate.

## 10. Low-influence mechanism review

The independent ranking labels ranks 31–45 as low **relative influence within range**. Nonzero EEs show that the 15 screened parameters are active somewhere in the untreated simulation, but the ZIP lacks source code, so exact branch reachability cannot be independently verified.

| Parameter | New rank | Evidence-based conceptual class | Why |
|---|---:|---|---|
| `activProbM` | 31 | active but weak/noisy | nonzero effects; prior 3-seed SNR .92; macrophage proxy concern |
| `divProbFP` | 32 | active but weak over tested range | positive fibroblast-target EE but only rank 12 on that target |
| `deactProbE` | 33 | active but weak/conditional | EC effects and EC-zero association; mechanism requires activated ECs |
| `fibroblastTumorRadius` | 34 | active only under spatial proximity conditions | nonzero fibroblast/spatial effects; hard-coded radius |
| `initialLungCount` | 35 | genuinely low influence within tested initialization range | direct initial condition, substantially weaker in independent run |
| `activProbE` | 36 | active but weak/conditional | nonzero EC effects; activation requires appropriate state/context |
| `macrophageEndothelialBiasRadius` | 37 | active only under spatial proximity conditions | nonzero spatial/failure effects; hard-coded radius |
| `migrProbN` | 38 | active but weak | nonzero tumor/spatial effects; weakened 28 → 38 |
| `tumorEndothelialRadius` | 39 | active only under spatial proximity conditions | nonzero EC/spatial effects; hard-coded radius |
| `ecSurvival` | 40 | active but weak/noisy | EC target rank 4 at step 1440 but sign-varying and SNR .57 |
| `cafDivBoost` | 41 | active but weak over tested range | nonzero fibro/JNK effects; low in both designs |
| `fibroblastSignalBoost` | 42 | active but weak/possibly context-limited | falls 27 → 42; signaling branch may be triggered without dominating measured outputs |
| `fibroblastSignalCap` | 43 | active only when the cap binds, or range not informative | nonzero weak effects; saturation cap is conditional by definition |
| `macrophageDaughterActivationBoost` | 44 | active only after macrophage division | daughter-specific mechanism; weak on current endpoints |
| `endothelialDaughterDivisionBoost` | 45 | active only after EC division | daughter-specific mechanism; weakest screened parameter |

The package separately classifies six unscreened quantities as structurally inactive:

| Parameter | Reported status | Decision |
|---|---|---|
| `migrProbF` | declared field with no executable fibroblast migration branch | exclude from untreated calibration; code audit before reuse |
| `activProbMP` | unused/blocked current event logic | exclude; code audit |
| `divProbEN` | inactive ECs do not divide | exclude; review alongside uncompensated `dieProbEN` |
| `migrProbE` | no executable EC migration branch | exclude; code audit |
| `divProbL` | unused/blocked current event logic | exclude; code audit |
| `unusedNeighborCountRadius` | uncalled helper radius | exclude |

Other unscreened quantities are appropriately excluded by design: treatment-specific parameters, coordinate maps, grid/domain choices, observation timing, numerical guards, output definitions, and stochastic controls. They are not evidence of low biological influence because they were not tested.

## 11. Range assessment

The result files verify mathematical/code validity: all screened probability bounds are in [0,1], every physical design value is within its registered bounds, and no single range caused majority EE loss. They do **not** include the source-by-source range provenance needed to claim biological validity.

The global report states the intended hierarchy: literature/project reference, then existing ABC prior, then mechanism-harness range, then conservative assumed variation requiring mentor approval. Because the registry is missing, the hierarchy cannot be assigned parameter-by-parameter here.

The result names and reports treat `*Prob*` quantities as dimensionless per-model-step event probabilities. The physical duration of a step is absent from the ZIP, so half-lives, doubling times, or per-day conversions cannot be verified.

### Serious ABC candidates

| Parameter | Default | Morris min–max | Type/interpretation | Documentary status available here | Range decision |
|---|---:|---:|---|---|---|
| `divProbP` | .006621 | .005–.03 | tumor division probability per model step | existing ABC-inferred range; exact source absent | retain provisionally for diagnostic pilot |
| `pOffMax` | .118803 | .01–.20 | maximum JNK off-switch probability per step | existing ABC-inferred range; exact source absent | retain provisionally |
| `dieProbN` | .018575 | .008–.025 | JNK-negative tumor death/turnover probability per step | existing ABC range; coupled to `netN`; source absent | do not infer until joint parameterization/range review |
| `activProbF` | .023592 | .001–.05 | fibroblast activation probability per step | existing ABC range; exact source absent | mentor/source review before expansion |
| `divProbFP` | .029806 | .018–.038 | activated fibroblast division probability per step | existing ABC range; relatively narrow; source absent | do not infer now; do not widen without evidence |
| `pOnMax` | .034430 | .01–.10 | maximum JNK on-switch probability per step | existing ABC range; overlaps `pOffMax` | keep fixed |
| `dieProbP` | .003061 | .001–.004 | JNK-positive tumor death probability per step | existing ABC range; moderate/failure-associated | keep fixed pending tumor-death review |
| `netN` | .002600 | .0015–.005 | net-growth term coupled to `dieProbN` | existing ABC range | keep fixed for core2; joint reparameterization review |

### Fixed-range flags

- `dieProbFN`: upper bound .008 is explicitly flagged as implying an implausibly short resting-fibroblast half-life.
- `dieProbEN`: .001–.01 is an uncompensated loss because `divProbEN` is inactive.
- `divProbFN`: default 0 with 0–.01 range requires explicit biological approval.
- `dieProbFP`: default .012 equals the upper bound.
- macrophage turnover pairs require joint homeostatic review rather than independent arbitrary ranges.
- radii, hidden boosts, and initial counts are reported as assumptions requiring mentor approval.

No result supports narrowing a range merely to suppress extinction. No result supports widening a low-influence range without a biological source. Missing provenance does not invalidate the Morris computation, but it blocks claiming that the screen covered a biologically defensible uncertainty range.

## 12. Identifiability and redundancy assessment

Four distinct concepts must remain separate:

- **Influential:** changing the parameter changes outputs over the tested range.
- **Calibratable:** there is a defensible range and a measured target related to the mechanism.
- **Identifiable:** the targets can distinguish that parameter from other parameters.
- **Biologically uncertain:** the true value is not known well enough to justify fixing it.

Morris directly addresses only the first.

### Candidate-specific identifiability

- `divProbP`: strongest identifiable candidate. It is #1 on all three tumor targets and has a consistent positive effect.
- `pOffMax`: strong identifiable candidate because it is #1 on both JNK targets with a consistent negative direction. Its tumor effects add information rather than erase the JNK signature.
- `dieProbN`: influential only modestly and lacks a unique measured signature. Coupling to `netN` and mostly weak target ranks make it likely to have a broad/flat conditional posterior.
- `activProbF`: has a useful fibroblast signature, but one fibroblast target cannot distinguish activation from errors in `divProbFN`, `dieProbFN`, and `dieProbFP`.
- `divProbFP`: overlaps the same fibroblast target and is weaker than both `activProbF` and fixed turnover.

### Fixed-parameter compensation

- Wrong macrophage turnover can be absorbed by `divProbP` or `pOffMax` because macrophage rates strongly affect tumor targets.
- Wrong stress-field defaults can be absorbed by `pOffMax` on JNK targets.
- Wrong fibroblast turnover can be absorbed by `activProbF`.
- Wrong EC turnover cannot be corrected legitimately by a tumor/JNK-only profile, even if global distance rewards an indirect compensation.

This is why the first pilot should be deliberately small and diagnostic rather than an attempt to fit every target at once.

## 13. ABC profile comparison

| Profile | Targets directly controlled | Evidence/reproducibility | Expected identifiability | Main risk | 100-draw suitability |
|---|---|---|---|---|---|
| A — `core3`: `divProbP`, `pOffMax`, `dieProbN` | tumor, JNK, weak turnover/extinction | strong for first two; rank-30 stable but weak for `dieProbN` | moderate; `dieProbN` likely weak/conditional | extra parameter may stay flat and is coupled to fixed `netN` | acceptable only as staged turnover diagnostic |
| B — `core3 + activProbF` | tumor, JNK, fibroblast, extinction | adds a strong new five-seed fibro signal, but old/new instability | lower; fibro activation confounded with three fixed turnover rates | compensation and overparameterization before range review | premature |
| C — old `core4` with `divProbFP` | tumor, JNK, weak fibroblast, extinction | `divProbFP` weakens 20 → 32 | lower than A; weak unique fibro signal | carries an unsupported parameter and misses stronger `activProbF` | reject |
| D — diagnostic `core2`: `divProbP`, `pOffMax` | tumor and JNK | both high and reproduced; distinct target signatures | highest of the four | too limited to repair fibro/macrophage/EC targets | **best first diagnostic pilot** |

Profile D is strongly supported because only `divProbP` and `pOffMax` are both top-tier, reproducible, already inferred, and directly matched to distinguishable measured targets. It is intentionally limited. Failure of EC, macrophage, or fibroblast targets under core2 is evidence to review those axes, not evidence to distort tumor/JNK parameters.

## 14. Final recommended profile

### Selected profile

Use `divProbP` and `pOffMax` only for the first diagnostic pilot.

Freeze all other parameters at an explicitly recorded snapshot. Preserve the existing Morris bounds only as provisional diagnostic priors until the missing provenance is reviewed:

- `divProbP`: .005–.03
- `pOffMax`: .01–.20

Do not infer `dieProbN` in the first pass. Add it only if:

1. accepted finite core2 draws systematically miss the late tumor target or tumor-extinction behavior;
2. the `dieProbN`/`netN` relation and ranges are reviewed;
3. the goal is explicitly a turnover/boundary diagnostic rather than assuming identifiability.

Add `activProbF` before `divProbFP` if a fibroblast parameter is needed and its range is approved.

### Required 100-draw diagnostic pilot

The pilot should test pipeline behavior and parameter-to-target information, not establish a calibrated posterior.

**Pre-run QC gates**

1. `accepted` implies `outcome_status = VALID_FINITE`.
2. Every accepted draw has all ten target-valid flags true and finite target values.
3. Epsilon is computed from valid finite draws only; invalid/extinct runs are reported separately, not ranked into the posterior.
4. Profile contains exactly `divProbP` and `pOffMax`; freeze hash, target hash, code hashes, ranges, transformations, horizon, and seed policy are saved.
5. Proposal samples reproduce the intended priors and remain in bounds.
6. A rerun with the same seeds is identical.
7. Clarify why the expected 11th target is absent; freeze the intended ten-target definition.

**Design**

- 100 parameter draws.
- Prefer three simulation seeds per draw (300 simulations) and aggregate target statistics while retaining every replicate. If the current runner cannot do replicated ABC, the one-seed pilot may test plumbing only, not posterior behavior.
- Keep the same target values, weights, and scales only after target provenance/units are confirmed.

**Save for every draw and replicate**

- proposal and simulation seeds;
- physical and normalized parameter values;
- all raw snapshots and derived outputs;
- each target value, validity flag, standardized residual, contribution, and total distance;
- `FINITE`, `INVALID`, `EXTINCT`, and specific population-zero flags;
- runtime, profile/freeze/target hashes, and complete config;
- accepted finite rows in a separate file.

**Pilot evaluation**

- Plot all valid finite distances; report min, median, quartiles, and chosen epsilon.
- Compare best accepted target residuals with the prior-predictive distribution, not merely with the worst runs.
- Check whether accepted values move away from their priors and whether the accepted cloud shows a coherent `divProbP`–`pOffMax` tradeoff.
- Require improvement in both JNK targets and all three tumor targets. A low total distance caused by one excellent family and several poor families is not sufficient.
- Compare accepted draws against a fixed-default baseline using the same seeds.
- Report valid/extinct/invalid fractions by parameter bins.
- Do not interpret posterior shape from only a handful of accepted points; this is a diagnostic.

**Expansion triggers**

- Fibroblast target stays systematically wrong while tumor/JNK improve → review fixed fibroblast turnover, then test `activProbF`.
- Macrophage target stays wrong → review `divProbMP`, `dieProbMP`, `divProbMN`, `dieProbMN`, and target validity.
- EC targets stay wrong or EC-zero remains common → review `dieProbEN`, inactive `divProbEN`, `divProbEP`, `dieProbEP`, and macrophage–EC coupling.
- JNK targets remain wrong across the core2 box → review `stressStrength`, `lambdaStress`, `lambdaCAF`, and possibly `pOnMax`; do not add all simultaneously.
- Tumor radius/spread remains wrong despite tumor-fold improvement → review `migrProbP` and `clusterRadius`.
- Late tumor/extinction remains wrong while early tumor/JNK improve → review `dieProbN` jointly with `netN`.

### Limitations and whether they block the pilot

| Limitation | Effect | Blocks pilot? |
|---|---|---|
| Only 20 paths through 45-D space | incomplete combination coverage | no for screening-based core2 diagnostic |
| Stochastic ABM | rank and EE uncertainty | no if replicated; one-seed posterior claims would be blocked |
| Tested-range dependence | low/high labels may change with ranges | biological calibration claims blocked; diagnostic pilot can proceed after explicit provisional approval |
| Top-rank instability | exact top-five order changes | no; core2 is stable on target-specific evidence |
| Nonlinearity/interactions | high `sigma` complicates signed effects | no; motivates small profile and replication |
| Failure/extinction bias | 57.3% of independent runs are nonfinite; EEs lost | no for Morris conclusion, but requires explicit ABC handling |
| Global score aggregation | equal family weighting can hide target-specific structure | no; recommendation uses target tables |
| No direct identifiability test | Morris cannot prove contraction | no; this is the purpose of the diagnostic pilot |
| Missing registry/scripts/source | exact provenance, units, transforms, and code branches unverified | blocks final prior freeze; provisional core2 pilot requires mentor sign-off |
| Accepted invalid runs in ABC smoke | contaminates accepted sample | **yes—must be corrected/verified before pilot** |
| Ten targets present although prior context expected eleven | possible target-definition mismatch | **yes until intended target set is confirmed** |
| All-NaN/missing ratios | primary one-seed SNR is NaN by design; independent `active_macrophage_ec_colocalization_s0` has all-NaN `mu*`/`sigma`; 29 outputs have all-NaN SNR, mostly baseline/zero/treatment/status outputs | no for core2 ABC targets, but warnings must be documented |
| Placeholder rank-comparison figure | misleading figure | no; use CSV/report values |

## 15. Parameter-by-parameter decision table

“Range quality: provisional” means code-valid bounds are present but biological provenance is missing from the ZIP.

| Parameter | Prior status | Morris evidence | Main targets | Range quality | Biological uncertainty | Identifiability concern | Decision | Reason |
|---|---|---|---|---|---|---|---|---|
| `divProbP` | inferred | 7 → 7, high | tumor | provisional ABC prior | moderate | low/moderate | calibrate now | strongest consistent tumor signature |
| `pOffMax` | inferred | 8 → 3, high | JNK, tumor | provisional ABC prior | moderate | moderate vs stress/on-rate | calibrate now | strongest JNK signature |
| `dieProbN` | inferred | 30 → 30, moderate | late tumor, extinction | provisional; coupled | high | high vs `netN` | staged expansion/reparameterization review | weak unique signature |
| `activProbF` | inferred | 34 → 15, high new | fibroblast | provisional; source absent | high | high vs fixed turnover | first expansion candidate | direct fibro target #4; reproducibility across designs uncertain |
| `divProbFP` | inferred | 20 → 32, low new | fibroblast | provisional/narrow | moderate | high vs fibro turnover | do not calibrate now | materially weakened |
| `pOnMax` | inferred | 25 → 17, moderate | JNK, late tumor | provisional | moderate | high vs `pOffMax`/stress | keep fixed; later JNK review | secondary overlapping signature |
| `dieProbP` | inferred | 21 → 22, moderate | tumor/failure | provisional/narrow | moderate | overlaps tumor growth/death | keep fixed; tumor-death review | not a robust leading target driver |
| `netN` | inferred | 33 → 28, moderate | late tumor/failure | provisional; coupled | high | high vs `dieProbN` | keep fixed; joint review | coupled parameterization |
| `activProbM` | inferred | 39 → 31, low | weak macrophage/EC | provisional | high | high/noisy | fix at supported value | prior SNR below 1 |
| `activProbE` | inferred | 36 → 36, low | EC | provisional | high | high | keep fixed/needs data | weak across both |
| `ecSurvival` | inferred | 41 → 40, low | EC/failure | provisional broad | high | very high/sign-varying | fix at supported value | prior SNR .56 |
| `cafDivBoost` | inferred | 38 → 41, low | fibro/JNK | provisional broad | high | high | keep fixed/needs data | weak across both |
| `dieProbMP` | fixed | 2 → 1, high | macrophage, EC, tumor, failure | review required | high | confounded across families | biology/range review | dominant fixed driver |
| `divProbMP` | fixed | 1 → 2, high | macrophage, EC, tumor | review required | high | paired turnover | biology/range review | dominant fixed driver |
| `divProbFN` | fixed | 3 → 4, high | fibroblast | default 0 concern | high | overlaps activation/death | biology/range review | target #1 |
| `dieProbFN` | fixed | 9 → 5, high | fibroblast/loss | flagged questionable | high | overlaps other turnover | biology/range review | target #2 and loss driver |
| `divProbMN` | fixed | 5 → 6, high | macrophage/EC | review required | high | paired turnover | biology/range review | dominant macrophage driver |
| `dieProbMN` | fixed | 6 → 10, high | macrophage/loss | review required | high | paired turnover | biology/range review | stable top ten |
| `stressStrength` | fixed | 4 → 8, high | JNK/tumor | assumed range approval | high | overlaps JNK switching | keep fixed pending review | strong but not identifiable with `pOffMax` |
| `dieProbEN` | fixed | 11 → 9, high | EC/loss | mechanism/range concern | high | EC–macrophage confounding | mechanism/range review | uncompensated EC death |
| `migrProbP` | fixed | 10 → 11, high | tumor spread | review required | moderate | tumor growth/spread overlap | keep fixed pending spatial review | stable spread driver |
| `lambdaStress` | fixed | 13 → 12, high | JNK | source absent | high | overlaps stress strength | keep fixed pending review | stable JNK driver |
| `divProbEP` | fixed | 24 → 13, high new | EC | review required | high | paired EC turnover | biology/range review | strengthened |
| `dieProbFP` | fixed | 26 → 14, high new | fibroblast | default at upper bound | high | overlaps fibro parameters | biology/range review | target #3 |
| `migrProbM` | fixed | 19 → 18, moderate | macrophage/EC | broad/source absent | moderate | spatial coupling | keep fixed pending data | stable moderate |
| `macrophageInteractionRadius` | hard-coded | 22 → 19, moderate | spatial/EC | assumed radius | high | spatial confounding | spatial review | hard-coded assumption |
| `clusterRadius` | fixed | 12 → 20, moderate | tumor establishment | initial-condition range | moderate | confounds growth/extinction | fix to measured condition | not ideal calibration parameter |
| `lambdaCAF` | fixed | 32 → 21, moderate | JNK | source absent | high | overlaps stress/JNK rates | keep fixed | strengthened but redundant |
| `endothelialMacrophageRadius` | hard-coded | 31 → 23, moderate | early EC | assumed radius | high | spatial confounding | spatial review | target-specific signal |
| `dieProbEP` | fixed | 15 → 24, moderate | EC/loss | review required | high | EC turnover confounding | biology/range review | weakened but direct |
| `initialMacrophageCount` | hard-coded | 23 → 25, moderate | initialization/macrophage | assumption | high | compensates turnover | measure/fix | initial condition |
| `dieProbL` | fixed | 14 → 26, moderate | late JNK | source absent | high | overlaps JNK axis | biology/range review | globally weaker, target #2 late JNK |
| `recruitBias` | fixed | 17 → 27, moderate | macrophage/tumor | source absent | moderate | spatial/migration overlap | keep fixed | weakened |
| `initPop` | fixed CLI | 16 → 29, moderate | establishment/extinction | experimental condition | low if known | confounds growth | fix to protocol | not a biological rate |
| `initialJnkPositiveTenths` | hard-coded | 29 → 16, moderate | JNK/failure | assumption | high | initial-state confounding | measure/fix | initial condition |
| `deactProbE` | fixed | 42 → 33, low | EC/failure | source absent | high | conditional/noisy | keep fixed | low continuous influence |
| `fibroblastTumorRadius` | hard-coded | 40 → 34, low | fibro/spatial | assumed radius | high | conditional | keep fixed; spatial review | low within range |
| `initialLungCount` | hard-coded | 18 → 35, low | initialization | assumption | high | initial-state compensation | measure/fix | materially weakened |
| `macrophageEndothelialBiasRadius` | hard-coded | 35 → 37, low | spatial | assumed radius | high | conditional | keep fixed | low within range |
| `migrProbN` | fixed | 28 → 38, low | tumor migration | source absent | moderate | overlaps `migrProbP` | keep fixed | weakened |
| `tumorEndothelialRadius` | hard-coded | 37 → 39, low | EC/spatial | assumed radius | high | conditional | keep fixed | low within range |
| `fibroblastSignalBoost` | hard-coded | 27 → 42, low | fibro signaling | assumption | high | conditional | keep fixed; mechanism audit if expected | materially weakened |
| `fibroblastSignalCap` | hard-coded | 45 → 43, low | fibro signaling | assumption | high | cap may rarely bind | keep fixed | weak/conditional |
| `macrophageDaughterActivationBoost` | hard-coded | 43 → 44, low | macrophage daughter state | assumption | high | rare-event conditional | keep fixed | low within range |
| `endothelialDaughterDivisionBoost` | hard-coded | 44 → 45, low | EC daughter state | assumption | high | rare-event conditional | keep fixed | lowest screened influence |
| `migrProbF` | inactive | not screened | none in untreated logic | no active range | mechanism uncertain | not identifiable | exclude; code audit | reported no executable branch |
| `activProbMP` | inactive | not screened | none | no active range | mechanism uncertain | not identifiable | exclude; code audit | reported blocked/unused |
| `divProbEN` | inactive | not screened | none | no active range | high | not identifiable | exclude; mechanism review | inactive division makes EC death uncompensated |
| `migrProbE` | inactive | not screened | none | no active range | mechanism uncertain | not identifiable | exclude; code audit | reported no migration branch |
| `divProbL` | inactive | not screened | none | no active range | mechanism uncertain | not identifiable | exclude; code audit | reported blocked/unused |
| `unusedNeighborCountRadius` | inactive | not screened | none | none | low | not identifiable | exclude | uncalled helper |

### Bottom line

The old `core4` is superseded. The evidence supports a two-parameter diagnostic pilot first, followed by target-triggered expansion. The scientifically correct next action is **not yet to run that pilot**: first fix or verify finite-only acceptance and confirm the intended ten-versus-eleven target definition, then freeze the core2 profile and its provisional ranges.
