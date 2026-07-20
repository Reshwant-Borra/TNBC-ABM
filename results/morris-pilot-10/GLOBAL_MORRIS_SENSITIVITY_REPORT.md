# Global Morris Sensitivity Report

Generated: 2026-07-17T18:21:45.998547Z

## 1. Executive Summary

This run performs global Morris elementary-effects screening of the untreated TNBC lung-metastasis ABM. It is **screening, not final calibration**, and Morris indices are **not Sobol indices**. The run used 10 trajectories, 1 matched stochastic replicate(s), 45 parameters, and 460 simulations. 171 runs met the strict finite-output definition; 158 were tumour-extinct and 0 errored. With fewer than 10 trajectories, all rankings should be treated as pipeline/pilot evidence only.

## 2. Scientific Question

Which uncertain biological rates, interaction strengths, spatial constants, and initialization quantities influence each model output across their documented ranges? Results are retained separately by output; total ABC distance is only one diagnostic output.

## 3. Files Audited

`ExampleGrid.java`, `ABCRejection.java`, `MechanismTestHarness.java`, `README.md`, the prior code/provenance audit, mechanism results, coordinate inputs, and `ABC_TNBC_parameter_reference.md.pdf`.

## 4. Complete Parameter Registry

See `GLOBAL_PARAMETER_REGISTRY.csv` and `GLOBAL_PARAMETER_REGISTRY.md`: 74 quantities audited, 45 screened.

## 5. Parameters Included and Excluded

All executable untreated biological parameters with stated ranges enter the design. Five declared but blocked/unused fields remain registered as structurally inactive. Fixed coordinate maps and computational structure remain fixed because varying them requires a separate spatial/domain design. Chemo multipliers remain treatment-specific and are excluded only because this screen ends before treatment.

## 6. Bound Justification

The hierarchy is literature/project-reference range, current ABC prior, prior mechanism-harness range, then explicit conservative variation requiring mentor approval. Log transforms are used for positive rates where ratios are meaningful; zero-inclusive quantities are linear; counts and radii requiring discreteness use integer transforms. Current executable baselines override obsolete fixed values in the supplied PDF, with every disagreement documented in the registry.

## 7. Morris Design

Normalized [0,1]^k OAT trajectories use p=6 levels and delta=p/[2(p-1)]=0.600000000000. Starts, parameter order, and directions are randomized from design seed `6949820704716978156`. Exactly one normalized and physical parameter changes at each step. Duplicate physical points: 0.

## 8. Simulation Count

Expected and recorded simulations: 460. Each trajectory contains k+1=46 points.

## 9. Seed Strategy

Master seed `9001` deterministically generates a separate design seed and simulation seeds. For replicate r, every point in trajectory t uses the same simulation seed, providing common random numbers for adjacent effects. Different trajectories/replicates use different seeds.

## 10. Parallelization Strategy

A bounded Java executor uses 8 threads. Each task creates a fresh `ExampleGrid`, immutable copied parameters, and a fresh HAL `Rand`; no model or RNG instance is shared. Atomic per-run checkpoints permit deterministic resume.

## 11. Per-Output Sensitivity Results

The primary results are `morris_summary_by_output.csv`. The table below gives the leading valid parameter for every emitted output. Signed mu describes direction; mu-star describes magnitude; sigma reflects trajectory dependence/nonlinearity/interactions and stochastic variability.

| Output | Family | Top parameter | mu-star | sigma | valid/lost |
|---|---|---|---:|---:|---:|
| `abc_target_01_jnkp_s480_standardized_residual` | jnk | `lambdaStress` | 3.51018022637 | 2.57729216747 | 8/2 |
| `abc_target_01_jnkp_s480_stat` | jnk | `lambdaStress` | 0.702036045274 | 0.515458433495 | 8/2 |
| `abc_target_02_jnkp_s1440_standardized_residual` | jnk | `dieProbL` | 2.66663352327 | 3.08277552233 | 6/4 |
| `abc_target_02_jnkp_s1440_stat` | jnk | `dieProbL` | 0.533326704653 | 0.616555104465 | 6/4 |
| `abc_target_03_ec_s480_standardized_residual` | endothelial | `dieProbEP` | 1.49167031592 | 1.40505300596 | 10/0 |
| `abc_target_03_ec_s480_stat` | endothelial | `dieProbEP` | 0.298334063183 | 0.281010601193 | 10/0 |
| `abc_target_04_ec_s960_standardized_residual` | endothelial | `recruitBias` | 3.07539682540 | 4.67324036243 | 7/3 |
| `abc_target_04_ec_s960_stat` | endothelial | `recruitBias` | 0.615079365079 | 0.934648072485 | 7/3 |
| `abc_target_05_ec_s1440_standardized_residual` | endothelial | `divProbMP` | 3.30245660975 | 3.39404598689 | 6/4 |
| `abc_target_05_ec_s1440_stat` | endothelial | `divProbMP` | 0.660491321950 | 0.678809197378 | 6/4 |
| `abc_target_06_mac_s1440_standardized_residual` | macrophage | `divProbMP` | 2.93340894535 | 2.72295891632 | 8/2 |
| `abc_target_06_mac_s1440_stat` | macrophage | `divProbMP` | 0.586681789070 | 0.544591783265 | 8/2 |
| `abc_target_07_fibro_s1440_standardized_residual` | fibroblast | `divProbFN` | 4.26676910718 | 3.64484070913 | 9/1 |
| `abc_target_07_fibro_s1440_stat` | fibroblast | `divProbFN` | 1.15202765894 | 0.984106991466 | 9/1 |
| `abc_target_08_tumor_s480_standardized_residual` | tumor | `divProbP` | 4.36331847025 | 3.60111644519 | 8/2 |
| `abc_target_08_tumor_s480_stat` | tumor | `divProbP` | 1.17809598697 | 0.972301440201 | 8/2 |
| `abc_target_09_tumor_s960_standardized_residual` | tumor | `divProbP` | 5.36426338801 | 2.68324107022 | 6/4 |
| `abc_target_09_tumor_s960_stat` | tumor | `divProbP` | 1.44835111476 | 0.724475088959 | 6/4 |
| `abc_target_10_tumor_s1440_standardized_residual` | tumor | `dieProbMP` | 6.26450667506 | 9.13485845160 | 4/6 |
| `abc_target_10_tumor_s1440_stat` | tumor | `dieProbMP` | 1.69141680227 | 2.46641178193 | 4/6 |
| `active_macrophage_ec_colocalization_s1440` | macrophage | `dieProbEP` | 0.329042455402 | 0.402547546784 | 8/2 |
| `active_macrophage_ec_colocalization_s480` | macrophage | `dieProbEP` | 0.326199385598 | 0.276445286886 | 10/0 |
| `active_macrophage_ec_colocalization_s960` | macrophage | `dieProbEP` | 0.432955221948 | 0.534323983593 | 10/0 |
| `cumulative_chemo_tumor_deaths_s0` | tumor | `activProbE` | 0 | 0 | 10/0 |
| `cumulative_chemo_tumor_deaths_s1440` | tumor | `activProbE` | 0 | 0 | 10/0 |
| `cumulative_chemo_tumor_deaths_s480` | tumor | `activProbE` | 0 | 0 | 10/0 |
| `cumulative_chemo_tumor_deaths_s960` | tumor | `activProbE` | 0 | 0 | 10/0 |
| `cumulative_chemo_tumor_divisions_s0` | tumor | `activProbE` | 0 | 0 | 10/0 |
| `cumulative_chemo_tumor_divisions_s1440` | tumor | `activProbE` | 0 | 0 | 10/0 |
| `cumulative_chemo_tumor_divisions_s480` | tumor | `activProbE` | 0 | 0 | 10/0 |
| `cumulative_chemo_tumor_divisions_s960` | tumor | `activProbE` | 0 | 0 | 10/0 |
| `cumulative_ec_deaths_s0` | endothelial | `activProbE` | 0 | 0 | 10/0 |
| `cumulative_ec_deaths_s1440` | endothelial | `divProbEP` | 255.833333333 | 314.955660215 | 10/0 |
| `cumulative_ec_deaths_s480` | endothelial | `dieProbEN` | 122 | 57.3003781256 | 10/0 |
| `cumulative_ec_deaths_s960` | endothelial | `divProbEP` | 198.000000000 | 245.175422207 | 10/0 |
| `cumulative_ec_divisions_s0` | endothelial | `activProbE` | 0 | 0 | 10/0 |
| `cumulative_ec_divisions_s1440` | endothelial | `dieProbEP` | 309 | 425.905716107 | 10/0 |
| `cumulative_ec_divisions_s480` | endothelial | `divProbEP` | 149.000000000 | 193.325670346 | 10/0 |
| `cumulative_ec_divisions_s960` | endothelial | `divProbEP` | 243.666666667 | 290.228517883 | 10/0 |
| `cumulative_fibroblast_deaths_s0` | fibroblast | `activProbE` | 0 | 0 | 10/0 |
| `cumulative_fibroblast_deaths_s1440` | fibroblast | `divProbFP` | 5666.16666667 | 6910.79144714 | 10/0 |
| `cumulative_fibroblast_deaths_s480` | fibroblast | `divProbFN` | 558.666666667 | 263.988636119 | 10/0 |
| `cumulative_fibroblast_deaths_s960` | fibroblast | `divProbFN` | 2193.50000000 | 1591.50318144 | 10/0 |
| `cumulative_fibroblast_divisions_s0` | fibroblast | `activProbE` | 0 | 0 | 10/0 |
| `cumulative_fibroblast_divisions_s1440` | fibroblast | `divProbFP` | 7951.66666667 | 8677.02980843 | 10/0 |
| `cumulative_fibroblast_divisions_s480` | fibroblast | `divProbFN` | 1504.83333333 | 613.677163086 | 10/0 |
| `cumulative_fibroblast_divisions_s960` | fibroblast | `divProbFN` | 4060.66666667 | 2657.91622178 | 10/0 |
| `cumulative_macrophage_deaths_s0` | macrophage | `activProbE` | 0 | 0 | 10/0 |
| `cumulative_macrophage_deaths_s1440` | macrophage | `dieProbMP` | 35572.6666667 | 58889.6254052 | 10/0 |
| `cumulative_macrophage_deaths_s480` | macrophage | `divProbMP` | 2187.83333333 | 3890.57593962 | 10/0 |
| `cumulative_macrophage_deaths_s960` | macrophage | `dieProbMP` | 15033.3333333 | 25813.5386384 | 10/0 |
| `cumulative_macrophage_divisions_s0` | macrophage | `activProbE` | 0 | 0 | 10/0 |
| `cumulative_macrophage_divisions_s1440` | macrophage | `dieProbMP` | 34824.0000000 | 58879.3505682 | 10/0 |
| `cumulative_macrophage_divisions_s480` | macrophage | `divProbMN` | 6351.33333333 | 6292.23904681 | 10/0 |
| `cumulative_macrophage_divisions_s960` | macrophage | `dieProbMP` | 14654.3333333 | 25787.7635855 | 10/0 |
| `cumulative_tumor_deaths_s0` | tumor | `activProbE` | 0 | 0 | 10/0 |
| `cumulative_tumor_deaths_s1440` | tumor | `divProbP` | 5449.33333333 | 9859.09247570 | 10/0 |
| `cumulative_tumor_deaths_s480` | tumor | `endothelialMacrophageRadius` | 313.333333333 | 719.094852858 | 10/0 |
| `cumulative_tumor_deaths_s960` | tumor | `divProbP` | 1988.33333333 | 3340.57103125 | 10/0 |
| `cumulative_tumor_divisions_s0` | tumor | `activProbE` | 0 | 0 | 10/0 |
| `cumulative_tumor_divisions_s1440` | tumor | `divProbMP` | 6891.16666667 | 13928.4732143 | 10/0 |
| `cumulative_tumor_divisions_s480` | tumor | `divProbP` | 800.166666667 | 1340.95945244 | 10/0 |
| `cumulative_tumor_divisions_s960` | tumor | `divProbP` | 3535.33333333 | 6601.10974583 | 10/0 |
| `ec_activated_fraction_s0` | endothelial | `activProbE` | 0 | 0 | 10/0 |
| `ec_activated_fraction_s1440` | endothelial | `divProbMP` | 0.660491321950 | 0.678809197378 | 6/4 |
| `ec_activated_fraction_s480` | endothelial | `dieProbEP` | 0.298334063183 | 0.281010601193 | 10/0 |
| `ec_activated_fraction_s960` | endothelial | `recruitBias` | 0.615079365079 | 0.934648072485 | 7/3 |
| `ec_population_zero_status` | failure | `dieProbEP` | 1.16666666667 | 1.17851130198 | 10/0 |
| `fibroblast_fold_change_s0` | fibroblast | `activProbE` | 0 | 0 | 10/0 |
| `fibroblast_fold_change_s1440` | fibroblast | `dieProbMP` | 17.2417840376 | 18.5024695899 | 10/0 |
| `fibroblast_fold_change_s480` | fibroblast | `divProbFN` | 6.66314553991 | 4.01889029051 | 10/0 |
| `fibroblast_fold_change_s960` | fibroblast | `divProbFN` | 13.1490610329 | 11.0909787109 | 10/0 |
| `fibroblast_log10_fold_s0` | fibroblast | `activProbE` | 0 | 0 | 10/0 |
| `fibroblast_log10_fold_s1440` | fibroblast | `divProbFN` | 1.15202765894 | 0.984106991466 | 9/1 |
| `fibroblast_log10_fold_s480` | fibroblast | `divProbFN` | 1.26241465538 | 0.645628671473 | 10/0 |
| `fibroblast_log10_fold_s960` | fibroblast | `divProbFN` | 1.11339755045 | 0.693075639190 | 9/1 |
| `fibroblast_population_zero_status` | failure | `dieProbL` | 0.166666666667 | 0.527046276695 | 10/0 |
| `fibroblast_total_s0` | fibroblast | `activProbE` | 0 | 0 | 10/0 |
| `fibroblast_total_s1440` | fibroblast | `dieProbMP` | 2448.33333333 | 2627.35068176 | 10/0 |
| `fibroblast_total_s480` | fibroblast | `divProbFN` | 946.166666667 | 570.682421252 | 10/0 |
| `fibroblast_total_s960` | fibroblast | `divProbFN` | 1867.16666667 | 1574.91897695 | 10/0 |
| `invalid_denominator_status` | failure | `divProbEP` | 0.833333333333 | 1.12491426285 | 10/0 |
| `jnkp_fraction_s0` | jnk | `initialJnkPositiveTenths` | 0.383734220691 | 0.157789427121 | 10/0 |
| `jnkp_fraction_s1440` | jnk | `dieProbL` | 0.533326704653 | 0.616555104465 | 6/4 |
| `jnkp_fraction_s480` | jnk | `lambdaStress` | 0.702036045274 | 0.515458433495 | 8/2 |
| `jnkp_fraction_s960` | jnk | `lambdaStress` | 0.583541667302 | 0.411962435900 | 5/5 |
| `jnkp_rim_fraction_s0` | jnk | `activProbE` | 0 | 0 | 10/0 |
| `jnkp_rim_fraction_s1440` | jnk | `activProbE` | 0 | 0 | 6/4 |
| `jnkp_rim_fraction_s480` | jnk | `activProbE` | 0 | 0 | 7/3 |
| `jnkp_rim_fraction_s960` | jnk | `activProbE` | 0 | 0 | 7/3 |
| `macrophage_activated_fraction_s0` | macrophage | `activProbE` | 0 | 0 | 10/0 |
| `macrophage_activated_fraction_s1440` | macrophage | `divProbMP` | 0.586681789070 | 0.544591783265 | 8/2 |
| `macrophage_activated_fraction_s480` | macrophage | `divProbMP` | 0.426491417069 | 0.371559006161 | 10/0 |
| `macrophage_activated_fraction_s960` | macrophage | `divProbMP` | 0.475899978016 | 0.390265936697 | 10/0 |
| `macrophage_population_zero_status` | failure | `dieProbMP` | 0.333333333333 | 0.702728368926 | 10/0 |
| `maximum_absolute_standardized_residual` | abc | `divProbMN` | 9.11017048008 |  | 1/9 |
| `overall_error_status` | failure | `activProbE` | 0 | 0 | 10/0 |
| `overall_finite_status` | failure | `divProbEP` | 0.833333333333 | 1.12491426285 | 10/0 |
| `overall_invalid_status` | failure | `divProbEP` | 0.833333333333 | 1.12491426285 | 10/0 |
| `temporal_fibroblast_tumor_correlation` | fibroblast | `divProbP` | 1.15261571013 | 1.53752670796 | 10/0 |
| `total_abc_distance` | abc | `divProbMN` | 13.8777010392 |  | 1/9 |
| `total_population_s0` | population | `initialMacrophageCount` | 924.500000000 | 2.94496850682 | 10/0 |
| `total_population_s1440` | population | `dieProbMN` | 3594.66666667 | 4224.51003807 | 10/0 |
| `total_population_s480` | population | `divProbMN` | 4081.83333333 | 4556.87962127 | 10/0 |
| `total_population_s960` | population | `dieProbMN` | 3415.66666667 | 3902.64177699 | 10/0 |
| `tumor_extinction_status` | failure | `dieProbMP` | 0.500000000000 | 0.805076485899 | 10/0 |
| `tumor_fold_change_s0` | tumor | `activProbE` | 0 | 0 | 10/0 |
| `tumor_fold_change_s1440` | tumor | `initPop` | 125.834136361 | 346.927901230 | 10/0 |
| `tumor_fold_change_s480` | tumor | `divProbP` | 44.0357619293 | 106.002773906 | 10/0 |
| `tumor_fold_change_s960` | tumor | `divProbP` | 129.334873575 | 335.080199633 | 10/0 |
| `tumor_log10_fold_s0` | tumor | `activProbE` | 0 | 0 | 10/0 |
| `tumor_log10_fold_s1440` | tumor | `dieProbMP` | 1.69141680227 | 2.46641178193 | 4/6 |
| `tumor_log10_fold_s480` | tumor | `divProbP` | 1.17809598697 | 0.972301440201 | 8/2 |
| `tumor_log10_fold_s960` | tumor | `divProbP` | 1.44835111476 | 0.724475088959 | 6/4 |
| `tumor_radius_s0` | tumor | `clusterRadius` | 6.63172881741 | 0.996476843760 | 10/0 |
| `tumor_radius_s1440` | tumor | `divProbP` | 26.6599023065 | 23.6028184755 | 6/4 |
| `tumor_radius_s480` | tumor | `dieProbMP` | 17.4025883449 | 16.3526727516 | 7/3 |
| `tumor_radius_s960` | tumor | `migrProbP` | 23.0819115474 | 19.0117759545 | 6/4 |
| `tumor_rms_spread_s0` | tumor | `clusterRadius` | 4.65560590623 | 0.450915444153 | 10/0 |
| `tumor_rms_spread_s1440` | tumor | `divProbMP` | 14.2756721554 | 17.6481825483 | 5/5 |
| `tumor_rms_spread_s480` | tumor | `divProbP` | 8.16881896995 | 9.12371365470 | 8/2 |
| `tumor_rms_spread_s960` | tumor | `divProbP` | 13.2330809902 | 10.4007864185 | 6/4 |
| `tumor_total_s0` | tumor | `initPop` | 22.3333333333 | 14.1464998070 | 10/0 |
| `tumor_total_s1440` | tumor | `divProbMP` | 1486.16666667 | 3119.46373300 | 10/0 |
| `tumor_total_s480` | tumor | `divProbP` | 553.333333333 | 1057.15574614 | 10/0 |
| `tumor_total_s960` | tumor | `divProbP` | 1547 | 3321.52480602 | 10/0 |

## 12. Important Currently Fixed Parameters

- Rank 1: `dieProbMP` (score 0.572391476180, status fixed).
- Rank 2: `divProbMP` (score 0.524212917911, status fixed).
- Rank 3: `stressStrength` (score 0.455854142255, status fixed).
- Rank 4: `divProbFN` (score 0.421968685380, status fixed).
- Rank 5: `divProbMN` (score 0.420296208183, status fixed).
- Rank 6: `lambdaStress` (score 0.419700835185, status fixed).
- Rank 7: `dieProbFN` (score 0.379462005581, status fixed).
- Rank 8: `dieProbMN` (score 0.370441847487, status fixed).
- Rank 9: `dieProbL` (score 0.364384416297, status fixed).
- Rank 10: `dieProbEN` (score 0.349050715232, status fixed).
- Rank 11: `migrProbP` (score 0.341620384606, status fixed).
- Rank 12: `initialLungCount` (score 0.338658105214, status hard-coded).

## 13. Parameters Associated With Extinction or Invalid States

Associations are screening statistics, not causal estimates.

- `stressStrength` vs FIBROBLAST_POPULATION_ZERO: rank-biserial=-0.891891891892, logistic coefficient=-100.784680004, permutation p=0.00497512437811.
- `dieProbFN` vs FIBROBLAST_POPULATION_ZERO: rank-biserial=0.856981981982, logistic coefficient=11.8719520640, permutation p=0.00497512437811.
- `dieProbEN` vs FIBROBLAST_POPULATION_ZERO: rank-biserial=0.804898648649, logistic coefficient=11.3959158300, permutation p=0.00497512437811.
- `fibroblastTumorRadius` vs FIBROBLAST_POPULATION_ZERO: rank-biserial=-0.800394144144, logistic coefficient=-10.0215601759, permutation p=0.00497512437811.
- `fibroblastSignalCap` vs FIBROBLAST_POPULATION_ZERO: rank-biserial=-0.789132882883, logistic coefficient=-9.95157927166, permutation p=0.00497512437811.
- `migrProbP` vs FIBROBLAST_POPULATION_ZERO: rank-biserial=0.783783783784, logistic coefficient=103.628515088, permutation p=0.00497512437811.
- `dieProbL` vs FIBROBLAST_POPULATION_ZERO: rank-biserial=0.759009009009, logistic coefficient=6.70554981121, permutation p=0.00497512437811.
- `initialLungCount` vs FIBROBLAST_POPULATION_ZERO: rank-biserial=0.759009009009, logistic coefficient=10.5445377816, permutation p=0.00497512437811.
- `divProbFN` vs FIBROBLAST_POPULATION_ZERO: rank-biserial=-0.752252252252, logistic coefficient=-5.11705650776, permutation p=0.00497512437811.
- `fibroblastSignalBoost` vs FIBROBLAST_POPULATION_ZERO: rank-biserial=-0.739864864865, logistic coefficient=-6.90358491562, permutation p=0.00497512437811.
- `ecSurvival` vs FIBROBLAST_POPULATION_ZERO: rank-biserial=0.737612612613, logistic coefficient=6.31883484049, permutation p=0.00497512437811.
- `divProbMN` vs FIBROBLAST_POPULATION_ZERO: rank-biserial=0.716779279279, logistic coefficient=5.22349491641, permutation p=0.00497512437811.
- `dieProbMP` vs MACROPHAGE_POPULATION_ZERO: rank-biserial=0.685938500961, logistic coefficient=4.95745006616, permutation p=0.00497512437811.
- `deactProbE` vs FIBROBLAST_POPULATION_ZERO: rank-biserial=-0.679617117117, logistic coefficient=-5.93279595343, permutation p=0.00497512437811.
- `activProbE` vs FIBROBLAST_POPULATION_ZERO: rank-biserial=-0.661317567568, logistic coefficient=-5.61683072153, permutation p=0.00497512437811.

## 14. Nonlinear/Interaction Candidates

See class B/C rows in `PARAMETER_INFLUENCE_CLASSIFICATION.csv`. High sigma can also reflect stochastic variability; multi-seed confirmation is required before biological interpretation.

## 15. Parameters Confirmed as Low Influence

Only class D parameters with adequate valid effects and acceptable signal-to-noise qualify statistically. Structurally inactive class E fields are implementation findings, not evidence of biological unimportance. A short pilot cannot confirm low influence.

## 16. One-Seed Versus Multi-Seed Comparison

Not yet run. Use the documented `--confirm-only --confirmation-replicates 3` command after the primary run.

## 17. Recommended Parameters for Future Calibration

Current overall candidates (provisional when trajectories <10): `dieProbMP`, `divProbMP`, `stressStrength`, `divProbFN`, `divProbMN`, `lambdaStress`, `dieProbFN`, `dieProbMN`, `dieProbL`, `dieProbEN`. Use per-output top tiers after the 20-trajectory screen and three-seed confirmation. Favor parameters influencing measured tumour, JNK, fibroblast, macrophage, and EC outputs with adequate SNR; retain failure-associated parameters even if continuous-output mu-star is moderate. Sensitivity alone does not establish identifiability.

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
