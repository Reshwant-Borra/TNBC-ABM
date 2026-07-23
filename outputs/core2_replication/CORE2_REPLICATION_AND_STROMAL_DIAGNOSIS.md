# CORE2 Replication And Stromal Diagnosis
## Architecture and Safeguards
- Authoritative calibration remains `OnLatticeExample.ABCRejection`; targets, weights, scales, thresholds, and core2 parameter ranges were not changed.
- Simulation entry point used here is `ExampleGrid.RunHeadlessDiagnostic(ModelParameters, maxStep, interval)`, which wraps the same headless update path and records counts/events after each step.
- Threading uses fixed Java worker pools; every task owns a fresh `ExampleGrid`, immutable `ModelParameters`, HAL `Rand`, diagnostic frame list, target result, and temporary buffers. Workers return objects; CSV writing is coordinator-only and sorted by candidate, replicate, seed.
- Repository search found no static grid, static agent collection, or static RNG used by the simulation. Static fields in calibration classes are immutable target/profile arrays except `ABCRejection.bestMacTry/bestMacFail`, which this harness does not mutate.
- Mutable model state is instance-scoped in `ExampleGrid`; `ModelParameters` and target/profile definitions are immutable for this workflow.

## ExampleGrid.java impact
- Modified: yes, minimally.
- Changed methods: diagnostic recorder calls inside `runHeadlessConfigured(...)`, plus new private helper `diagnosticEvents(int[])`.
- Reason: `macDivTry` and `macDivFail` were directly available instance counters but were not included in per-step diagnostic frames; exposing them is required to report failed division/crowding diagnostics.
- Alternatives considered: ignore failed-division diagnostics or duplicate initialization/update logic outside `ExampleGrid`; both were worse because they would either omit requested evidence or risk diverging from the simulation.
- Biological behavior: unchanged. The helper only copies cumulative event counters and appends diagnostic `macDivTry/macDivFail` values when diagnostics are enabled; agent rules, update order, random-number consumption, target calculations, and default `RunHeadless` outputs are unchanged.
- Regression evidence: `CalibrationQualityControl` compares legacy and named seeded outputs including snapshots/event counts; `Core2ReplicationQualityControl` compares one-thread and two-thread replicated outputs.

## Replication Results
- Total simulations: 130; valid: 79; invalid: 51.
- Good: 22; borderline: 29; poor valid: 28.
- Candidate classifications: ROBUST 1, UNCERTAIN 9, REJECTED 3.
- Best robust candidate by composite ranking: 16, divProbP=0.011219865151, pOffMax=0.158809491956, median distance=2.96, valid fraction=0.80, <=3 fraction=0.40, <=4 fraction=0.70, sd=0.907.

## Candidate Interpretation
- Low `divProbP` support: yes; robust median divProbP is 0.01122.
- `pOffMax` constraint: only one robust candidate was found, so the robust range cannot constrain `pOffMax`; the broader candidate set still spans most of the original interval.
- Apparent divProbP-pOffMax tradeoff correlation across candidate summaries: 0.707; this is descriptive, not causal.
- Original one-seed lucky/fragile candidates: 3 candidates were rejected or had no reproduced passing replicate; 12 had at least one pass but did not meet robust criteria.
- Candidates to proceed to 30-50 seed validation: [16].

## Compartment-Loss Diagnosis
- Dominant invalid reason: `EC_POPULATION_ZERO`.
- Median loss times by compartment, days: {'EC': 17.83541666665, 'tumor': 9.43541666667}.
- Fibroblast loss was not observed in the replicated loss events, so EC/fibroblast ordering could not be estimated.
- Median tumour count immediately before stromal loss: {'EC': 351.5, 'tumor': 1.0}.
- Median EC birth/death ratio before EC loss: 0.136; median fibroblast birth/death ratio before fibroblast loss: not observed.
- Parameter-validity/loss associations: {'divProbP_validity_point_biserial': 0.043, 'pOffMax_validity_point_biserial': -0.083, 'divProbP_EC_loss_point_biserial': -0.022, 'pOffMax_EC_loss_point_biserial': 0.069, 'divProbP_fibro_loss_point_biserial': None, 'pOffMax_fibro_loss_point_biserial': None, 'divProbP_loss_time_corr': 0.037, 'pOffMax_loss_time_corr': -0.257, 'tumor_final_invalid_corr': -0.163}.
- Loss rates by parameter thirds:

### divProbP thirds

| divProbP_third | invalid_fraction | EC_loss_fraction | fibroblast_loss_fraction | median_distance |
| --- | --- | --- | --- | --- |
| low | 0.4 | 0.38 | 0 | 4.048 |
| middle | 0.4 | 0.4 | 0 | 3.246 |
| high | 0.375 | 0.375 | 0 | 3.419 |

### pOffMax thirds

| pOffMax_third | invalid_fraction | EC_loss_fraction | fibroblast_loss_fraction | median_distance |
| --- | --- | --- | --- | --- |
| low | 0.34 | 0.34 | 0 | 4.064 |
| middle | 0.4 | 0.4 | 0 | 2.91 |
| high | 0.45 | 0.425 | 0 | 3.449 |

- Interpretation: compartment-loss evidence is based on replicated stochastic runs and correlations/event ratios; it does not establish causation by itself.

## Step 4 Decision
**Outcome B: Core2 fits targets but has stromal fragility.**
Core2 has reproducible target fits but stromal collapse remains frequent enough to justify exactly one stromal parameter in the next phase.
- Recommended parameter: `dieProbEN`.
- Biological meaning: Resting EC death probability per step.
- Current frozen value: 0.005.
- Code use: OnLatticeExample/ExampleGrid.java, inactive EC branch in Endothelial(); registry location `legacy lines 444-453`.
- Diagnostic evidence: EC birth/death ratio 0.136; fibroblast birth/death ratio not observed; dominant invalid reason `EC_POPULATION_ZERO`.
- Previous Morris support: see `results/morris-primary-20/FINAL_PARAMETER_DECISION_TABLE.csv` and registry evidence; sensitivity supports inclusion only when the observed failure mechanism matches the parameter.
- Proposed conservative range: 0.001 to 0.005.
- Range provenance: Conservative narrowing within the existing registry/Morris range 0.001 to 0.01, capped at the current frozen value to test whether uncompensated resting-EC loss is driving collapse.
- Risks: Lowering EC death can inflate EC activation-fraction denominators and indirectly increase tumour survival.
- Expected effect: Should preserve the EC compartment before activation without directly changing tumour JNK switching.

## Figure Inventory
- `figures/01_candidate_median_distance.png`
- `figures/02_valid_fraction.png`
- `figures/03_pass_fractions.png`
- `figures/04_parameter_scatter_robustness.png`
- `figures/05_parameter_vs_median_distance.png`
- `figures/06_parameter_vs_invalid_fraction.png`
- `figures/07_ec_loss_frequency.png`
- `figures/08_fibroblast_loss_frequency.png`
- `figures/09_loss_time_distribution.png`
- `figures/10_representative_population_trajectories.png`
- `figures/11_median_target_residuals.png`
- `figures/12_median_target_contributions.png`
- `figures/13_valid_invalid_population_comparison.png`
- `figures/14_tradeoff_ridge.png`
