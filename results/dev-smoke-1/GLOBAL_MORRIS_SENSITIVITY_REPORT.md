# Global Morris Sensitivity Report

Generated: 2026-07-17T14:55:55.945309Z

## 1. Executive Summary

This run performs global Morris elementary-effects screening of the untreated TNBC lung-metastasis ABM. It is **screening, not final calibration**, and Morris indices are **not Sobol indices**. The run used 1 trajectories, 1 matched stochastic replicate(s), 45 parameters, and 46 simulations. 46 runs met the strict finite-output definition; 0 were tumour-extinct and 0 errored. With fewer than 10 trajectories, all rankings should be treated as pipeline/pilot evidence only.

## 2. Scientific Question

Which uncertain biological rates, interaction strengths, spatial constants, and initialization quantities influence each model output across their documented ranges? Results are retained separately by output; total ABC distance is only one diagnostic output.

## 3. Files Audited

`ExampleGrid.java`, `ABCRejection.java`, `MechanismTestHarness.java`, `README.md`, the prior code/provenance audit, mechanism results, coordinate inputs, and `ABC_TNBC_parameter_reference.md.pdf`.

## 4. Complete Parameter Registry

See `GLOBAL_PARAMETER_REGISTRY.csv` and `GLOBAL_PARAMETER_REGISTRY.md`: 68 quantities audited, 45 screened.

## 5. Parameters Included and Excluded

All executable untreated biological parameters with stated ranges enter the design. Five declared but blocked/unused fields remain registered as structurally inactive. Fixed coordinate maps and computational structure remain fixed because varying them requires a separate spatial/domain design. Chemo multipliers remain treatment-specific and are excluded only because this screen ends before treatment.

## 6. Bound Justification

The hierarchy is literature/project-reference range, current ABC prior, prior mechanism-harness range, then explicit conservative variation requiring mentor approval. Log transforms are used for positive rates where ratios are meaningful; zero-inclusive quantities are linear; counts and radii requiring discreteness use integer transforms. Current executable baselines override obsolete fixed values in the supplied PDF, with every disagreement documented in the registry.

## 7. Morris Design

Normalized [0,1]^k OAT trajectories use p=6 levels and delta=p/[2(p-1)]=0.600000000000. Starts, parameter order, and directions are randomized from design seed `6949820704716978156`. Exactly one normalized and physical parameter changes at each step. Duplicate physical points: 0.

## 8. Simulation Count

Expected and recorded simulations: 46. Each trajectory contains k+1=46 points.

## 9. Seed Strategy

Master seed `9001` deterministically generates a separate design seed and simulation seeds. For replicate r, every point in trajectory t uses the same simulation seed, providing common random numbers for adjacent effects. Different trajectories/replicates use different seeds.

## 10. Parallelization Strategy

A bounded Java executor uses 4 threads. Each task creates a fresh `ExampleGrid`, immutable copied parameters, and a fresh HAL `Rand`; no model or RNG instance is shared. Atomic per-run checkpoints permit deterministic resume.

## 11. Per-Output Sensitivity Results

The primary results are `morris_summary_by_output.csv`. The table below gives the leading valid parameter for every emitted output. Signed mu describes direction; mu-star describes magnitude; sigma reflects trajectory dependence/nonlinearity/interactions and stochastic variability.

| Output | Family | Top parameter | mu-star | sigma | valid/lost |
|---|---|---|---:|---:|---:|
| `cumulative_chemo_tumor_deaths_s0` | tumor | `activProbE` | 0 |  | 1/0 |
| `cumulative_chemo_tumor_divisions_s0` | tumor | `activProbE` | 0 |  | 1/0 |
| `cumulative_ec_deaths_s0` | endothelial | `activProbE` | 0 |  | 1/0 |
| `cumulative_ec_divisions_s0` | endothelial | `activProbE` | 0 |  | 1/0 |
| `cumulative_fibroblast_deaths_s0` | fibroblast | `activProbE` | 0 |  | 1/0 |
| `cumulative_fibroblast_divisions_s0` | fibroblast | `activProbE` | 0 |  | 1/0 |
| `cumulative_macrophage_deaths_s0` | macrophage | `activProbE` | 0 |  | 1/0 |
| `cumulative_macrophage_divisions_s0` | macrophage | `activProbE` | 0 |  | 1/0 |
| `cumulative_tumor_deaths_s0` | tumor | `activProbE` | 0 |  | 1/0 |
| `cumulative_tumor_divisions_s0` | tumor | `activProbE` | 0 |  | 1/0 |
| `ec_activated_fraction_s0` | endothelial | `activProbE` | 0 |  | 1/0 |
| `ec_population_zero_status` | failure | `activProbE` | 0 |  | 1/0 |
| `fibroblast_fold_change_s0` | fibroblast | `activProbE` | 0 |  | 1/0 |
| `fibroblast_log10_fold_s0` | fibroblast | `activProbE` | 0 |  | 1/0 |
| `fibroblast_population_zero_status` | failure | `activProbE` | 0 |  | 1/0 |
| `fibroblast_total_s0` | fibroblast | `activProbE` | 0 |  | 1/0 |
| `invalid_denominator_status` | failure | `activProbE` | 0 |  | 1/0 |
| `jnkp_fraction_s0` | jnk | `initialJnkPositiveTenths` | 0.512820512821 |  | 1/0 |
| `jnkp_rim_fraction_s0` | jnk | `activProbE` | 0 |  | 1/0 |
| `macrophage_activated_fraction_s0` | macrophage | `activProbE` | 0 |  | 1/0 |
| `macrophage_population_zero_status` | failure | `activProbE` | 0 |  | 1/0 |
| `overall_error_status` | failure | `activProbE` | 0 |  | 1/0 |
| `overall_finite_status` | failure | `activProbE` | 0 |  | 1/0 |
| `overall_invalid_status` | failure | `activProbE` | 0 |  | 1/0 |
| `total_population_s0` | population | `initialMacrophageCount` | 916.666666667 |  | 1/0 |
| `tumor_extinction_status` | failure | `activProbE` | 0 |  | 1/0 |
| `tumor_fold_change_s0` | tumor | `activProbE` | 0 |  | 1/0 |
| `tumor_log10_fold_s0` | tumor | `activProbE` | 0 |  | 1/0 |
| `tumor_radius_s0` | tumor | `clusterRadius` | 5.05465040429 |  | 1/0 |
| `tumor_rms_spread_s0` | tumor | `clusterRadius` | 3.96416435604 |  | 1/0 |
| `tumor_total_s0` | tumor | `initPop` | 18.3333333333 |  | 1/0 |

## 12. Important Currently Fixed Parameters

- Rank 1: `clusterRadius` (score 0.347222222222, status fixed).
- Rank 2: `initialJnkPositiveTenths` (score 0.333333333333, status hard-coded).
- Rank 3: `initPop` (score 0.175786974201, status fixed CLI).
- Rank 4: `initialLungCount` (score 0.0800282695891, status hard-coded).
- Rank 5: `initialMacrophageCount` (score 0.0709908908727, status hard-coded).
- Rank 10: `deactProbE` (score 0, status fixed).
- Rank 11: `dieProbEN` (score 0, status fixed).
- Rank 12: `dieProbEP` (score 0, status fixed).
- Rank 13: `dieProbFN` (score 0, status fixed).
- Rank 14: `dieProbFP` (score 0, status fixed).
- Rank 15: `dieProbL` (score 0, status fixed).
- Rank 16: `dieProbMN` (score 0, status fixed).

## 13. Parameters Associated With Extinction or Invalid States

Associations are screening statistics, not causal estimates.

No outcome had both event and non-event runs; association statistics are explicitly undefined.

## 14. Nonlinear/Interaction Candidates

See class B/C rows in `PARAMETER_INFLUENCE_CLASSIFICATION.csv`. High sigma can also reflect stochastic variability; multi-seed confirmation is required before biological interpretation.

## 15. Parameters Confirmed as Low Influence

Only class D parameters with adequate valid effects and acceptable signal-to-noise qualify statistically. Structurally inactive class E fields are implementation findings, not evidence of biological unimportance. A short pilot cannot confirm low influence.

## 16. One-Seed Versus Multi-Seed Comparison

Not yet run. Use the documented `--confirm-only --confirmation-replicates 3` command after the primary run.

## 17. Recommended Parameters for Future Calibration

Use the per-output top tiers after the 20-trajectory screen and three-seed confirmation. Favor parameters influencing measured tumour, JNK, fibroblast, macrophage, and EC outputs with adequate SNR; retain failure-associated parameters even if continuous-output mu-star is moderate. Sensitivity alone does not establish identifiability.

## 18. Recommended Parameters to Remain Fixed

Keep numerical cutoffs, grid/domain structure, coordinate realizations, observation times, and treatment-only quantities fixed for this untreated calibration unless a dedicated design is approved. Low Morris influence alone is not proof that a mechanism is biologically unnecessary.

## 19. Parameters Needing Biological Review

All bounds labeled assumption requiring mentor approval, the older PDF/current-code discrepancies, independent variation that breaks proposed homeostatic/ratio constraints, initial background counts, hidden density boosts, and all interaction radii need review.

## 20. Limitations

Morris is a screening method. Mu-star is statistical sensitivity over the chosen bounds, not biological importance or identifiability. Sigma mixes nonlinear, interaction, discrete-mapping, and stochastic effects. Parameter uncertainty is encoded by ranges and is distinct from stochastic simulation variability. A flat ABC posterior is not proof of low sensitivity, and a fixed parameter can be influential.

## 21. Exact Next Step for Surrogate-Model Training

After confirming rankings, generate a larger space-filling design over the retained uncertain parameters while retaining failure flags and every raw/derived output. Join it with `morris_outputs.csv` by named parameter columns; train separate probabilistic or heteroscedastic output emulators, with a separate classifier for invalid/extinct states. Do not train on NaNs as successful outcomes.

## 22. Exact Next Step for ABC or SNPE

Define a reviewed reduced prior only after per-output and failure results are stable. Simulate a space-filling training set with replicated seeds, validate the surrogate out of sample per output, then run ABC-SMC or SNPE with explicit missing/extinction handling and posterior predictive checks. Do not return to ordinary rejection ABC as the main workflow.

## 23. Screening Statement

This analysis identifies influential parameters over specified uncertainty ranges. It does **not** calibrate the model, estimate Sobol indices, prove identifiability, or justify deleting biological mechanisms.
