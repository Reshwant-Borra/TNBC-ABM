# Global Morris Sensitivity Report

Generated: 2026-07-22T19:46:38.813203Z

## 1. Executive Summary

This run performs global Morris elementary-effects screening of the untreated TNBC lung-metastasis ABM. It is **screening, not final calibration**, and Morris indices are **not Sobol indices**. The run used 20 trajectories, 5 matched stochastic replicate(s), 45 parameters, and 4600 simulations. 1963 runs met the strict finite-output definition; 950 were tumour-extinct and 0 errored. With fewer than 10 trajectories, all rankings should be treated as pipeline/pilot evidence only.

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

Normalized [0,1]^k OAT trajectories use p=6 levels and delta=p/[2(p-1)]=0.600000000000. Starts, parameter order, and directions are randomized from design seed `-1836350308661608582`. Exactly one normalized and physical parameter changes at each step. Duplicate physical points: 0.

## 8. Simulation Count

Expected and recorded simulations: 4600. Each trajectory contains k+1=46 points.

## 9. Seed Strategy

Master seed `2026072201` deterministically generates a separate design seed and simulation seeds. For replicate r, every point in trajectory t uses the same simulation seed, providing common random numbers for adjacent effects. Different trajectories/replicates use different seeds.

## 10. Parallelization Strategy

A bounded Java executor uses 8 threads. Each task creates a fresh `ExampleGrid`, immutable copied parameters, and a fresh HAL `Rand`; no model or RNG instance is shared. Atomic per-run checkpoints permit deterministic resume.

## 11. Per-Output Sensitivity Results

The primary results are `morris_summary_by_output.csv`. The table below gives the leading valid parameter for every emitted output. Signed mu describes direction; mu-star describes magnitude; sigma reflects trajectory dependence/nonlinearity/interactions and stochastic variability.

| Output | Family | Top parameter | mu-star | sigma | valid/lost |
|---|---|---|---:|---:|---:|
| `abc_target_01_jnkp_s480_standardized_residual` | jnk | `pOffMax` | 2.73747980421 | 1.20410559569 | 20/0 |
| `abc_target_01_jnkp_s480_stat` | jnk | `pOffMax` | 0.547495960843 | 0.240821119138 | 20/0 |
| `abc_target_02_jnkp_s1440_standardized_residual` | jnk | `pOffMax` | 2.39848935598 | 1.45545811751 | 18/2 |
| `abc_target_02_jnkp_s1440_stat` | jnk | `pOffMax` | 0.479697871197 | 0.291091623503 | 18/2 |
| `abc_target_03_ec_s480_standardized_residual` | endothelial | `divProbMP` | 1.79311236570 | 1.76537529318 | 20/0 |
| `abc_target_03_ec_s480_stat` | endothelial | `divProbMP` | 0.358622473140 | 0.353075058637 | 20/0 |
| `abc_target_04_ec_s960_standardized_residual` | endothelial | `dieProbMP` | 3.02748283505 | 2.91828224621 | 17/3 |
| `abc_target_04_ec_s960_stat` | endothelial | `dieProbMP` | 0.605496567009 | 0.583656449242 | 17/3 |
| `abc_target_05_ec_s1440_standardized_residual` | endothelial | `dieProbMP` | 3.40351151977 | 3.04852006524 | 14/6 |
| `abc_target_05_ec_s1440_stat` | endothelial | `dieProbMP` | 0.680702303954 | 0.609704013048 | 14/6 |
| `abc_target_06_mac_s1440_standardized_residual` | macrophage | `dieProbMP` | 3.09676382836 | 2.75882123367 | 17/3 |
| `abc_target_06_mac_s1440_stat` | macrophage | `dieProbMP` | 0.619352765672 | 0.551764246734 | 17/3 |
| `abc_target_07_fibro_s1440_standardized_residual` | fibroblast | `divProbFN` | 5.64833143042 | 3.49788246906 | 19/1 |
| `abc_target_07_fibro_s1440_stat` | fibroblast | `divProbFN` | 1.52504948621 | 0.944428266645 | 19/1 |
| `abc_target_08_tumor_s480_standardized_residual` | tumor | `divProbP` | 4.10215536972 | 2.15878338446 | 20/0 |
| `abc_target_08_tumor_s480_stat` | tumor | `divProbP` | 1.10758194983 | 0.582871513803 | 20/0 |
| `abc_target_09_tumor_s960_standardized_residual` | tumor | `divProbP` | 6.04970626297 | 3.37094808207 | 18/2 |
| `abc_target_09_tumor_s960_stat` | tumor | `divProbP` | 1.63342069100 | 0.910155982158 | 18/2 |
| `abc_target_10_tumor_s1440_standardized_residual` | tumor | `divProbP` | 7.59943122699 | 4.48919616761 | 17/3 |
| `abc_target_10_tumor_s1440_stat` | tumor | `divProbP` | 2.05184643129 | 1.21208296526 | 17/3 |
| `active_macrophage_ec_colocalization_s1440` | macrophage | `dieProbEN` | 0.283563644775 | 0.191325688223 | 15/5 |
| `active_macrophage_ec_colocalization_s480` | macrophage | `dieProbEN` | 0.428437032182 | 0.312546667471 | 19/1 |
| `active_macrophage_ec_colocalization_s960` | macrophage | `dieProbEN` | 0.430251440255 | 0.388229290859 | 17/3 |
| `cumulative_chemo_tumor_deaths_s0` | tumor | `activProbE` | 0 | 0 | 20/0 |
| `cumulative_chemo_tumor_deaths_s1440` | tumor | `activProbE` | 0 | 0 | 20/0 |
| `cumulative_chemo_tumor_deaths_s480` | tumor | `activProbE` | 0 | 0 | 20/0 |
| `cumulative_chemo_tumor_deaths_s960` | tumor | `activProbE` | 0 | 0 | 20/0 |
| `cumulative_chemo_tumor_divisions_s0` | tumor | `activProbE` | 0 | 0 | 20/0 |
| `cumulative_chemo_tumor_divisions_s1440` | tumor | `activProbE` | 0 | 0 | 20/0 |
| `cumulative_chemo_tumor_divisions_s480` | tumor | `activProbE` | 0 | 0 | 20/0 |
| `cumulative_chemo_tumor_divisions_s960` | tumor | `activProbE` | 0 | 0 | 20/0 |
| `cumulative_ec_deaths_s0` | endothelial | `activProbE` | 0 | 0 | 20/0 |
| `cumulative_ec_deaths_s1440` | endothelial | `divProbEP` | 307.333333333 | 441.535068114 | 20/0 |
| `cumulative_ec_deaths_s480` | endothelial | `dieProbEN` | 149.100000000 | 32.0980041645 | 20/0 |
| `cumulative_ec_deaths_s960` | endothelial | `divProbEP` | 155.066666667 | 171.254251665 | 20/0 |
| `cumulative_ec_divisions_s0` | endothelial | `activProbE` | 0 | 0 | 20/0 |
| `cumulative_ec_divisions_s1440` | endothelial | `divProbEP` | 596.916666667 | 1152.06445196 | 20/0 |
| `cumulative_ec_divisions_s480` | endothelial | `divProbEP` | 109.300000000 | 153.901308849 | 20/0 |
| `cumulative_ec_divisions_s960` | endothelial | `divProbEP` | 338.000000000 | 569.428768861 | 20/0 |
| `cumulative_fibroblast_deaths_s0` | fibroblast | `activProbE` | 0 | 0 | 20/0 |
| `cumulative_fibroblast_deaths_s1440` | fibroblast | `divProbFN` | 5562.61666667 | 3056.66840436 | 20/0 |
| `cumulative_fibroblast_deaths_s480` | fibroblast | `divProbFN` | 679 | 263.199699603 | 20/0 |
| `cumulative_fibroblast_deaths_s960` | fibroblast | `divProbFN` | 2708.71666667 | 1283.13154611 | 20/0 |
| `cumulative_fibroblast_divisions_s0` | fibroblast | `activProbE` | 0 | 0 | 20/0 |
| `cumulative_fibroblast_divisions_s1440` | fibroblast | `divProbFN` | 7477.08333333 | 4192.28154867 | 20/0 |
| `cumulative_fibroblast_divisions_s480` | fibroblast | `divProbFN` | 1623.55000000 | 614.252869763 | 20/0 |
| `cumulative_fibroblast_divisions_s960` | fibroblast | `divProbFN` | 4433.83333333 | 2154.66903742 | 20/0 |
| `cumulative_macrophage_deaths_s0` | macrophage | `activProbE` | 0 | 0 | 20/0 |
| `cumulative_macrophage_deaths_s1440` | macrophage | `divProbMN` | 21773.8166667 | 34705.2744877 | 20/0 |
| `cumulative_macrophage_deaths_s480` | macrophage | `divProbMN` | 3369.46666667 | 3186.27007809 | 20/0 |
| `cumulative_macrophage_deaths_s960` | macrophage | `divProbMN` | 12100.7833333 | 15763.5005464 | 20/0 |
| `cumulative_macrophage_divisions_s0` | macrophage | `activProbE` | 0 | 0 | 20/0 |
| `cumulative_macrophage_divisions_s1440` | macrophage | `divProbMN` | 25849.5500000 | 36264.9083365 | 20/0 |
| `cumulative_macrophage_divisions_s480` | macrophage | `divProbMN` | 7444.63333333 | 6127.73207916 | 20/0 |
| `cumulative_macrophage_divisions_s960` | macrophage | `divProbMN` | 16728.3500000 | 17879.8886569 | 20/0 |
| `cumulative_tumor_deaths_s0` | tumor | `activProbE` | 0 | 0 | 20/0 |
| `cumulative_tumor_deaths_s1440` | tumor | `divProbP` | 15582.7500000 | 26242.2362355 | 20/0 |
| `cumulative_tumor_deaths_s480` | tumor | `divProbP` | 1184.31666667 | 1832.04718674 | 20/0 |
| `cumulative_tumor_deaths_s960` | tumor | `divProbP` | 7614.86666667 | 13027.3274556 | 20/0 |
| `cumulative_tumor_divisions_s0` | tumor | `activProbE` | 0 | 0 | 20/0 |
| `cumulative_tumor_divisions_s1440` | tumor | `divProbP` | 17522.5166667 | 28648.0930085 | 20/0 |
| `cumulative_tumor_divisions_s480` | tumor | `divProbP` | 2563.20000000 | 3908.75273387 | 20/0 |
| `cumulative_tumor_divisions_s960` | tumor | `divProbP` | 10030.2333333 | 16417.0880132 | 20/0 |
| `ec_activated_fraction_s0` | endothelial | `activProbE` | 0 | 0 | 20/0 |
| `ec_activated_fraction_s1440` | endothelial | `dieProbMP` | 0.680702303954 | 0.609704013048 | 14/6 |
| `ec_activated_fraction_s480` | endothelial | `divProbMP` | 0.358622473140 | 0.353075058637 | 20/0 |
| `ec_activated_fraction_s960` | endothelial | `dieProbMP` | 0.605496567009 | 0.583656449242 | 17/3 |
| `ec_population_zero_status` | failure | `dieProbEN` | 0.650000000000 | 0.713077507823 | 20/0 |
| `fibroblast_fold_change_s0` | fibroblast | `activProbE` | 0 | 0 | 20/0 |
| `fibroblast_fold_change_s1440` | fibroblast | `dieProbFN` | 18.0607981221 | 14.6579122323 | 20/0 |
| `fibroblast_fold_change_s480` | fibroblast | `divProbFN` | 6.65176056338 | 3.87069966937 | 20/0 |
| `fibroblast_fold_change_s960` | fibroblast | `dieProbFN` | 13.6637323944 | 11.2286950635 | 20/0 |
| `fibroblast_log10_fold_s0` | fibroblast | `activProbE` | 0 | 0 | 20/0 |
| `fibroblast_log10_fold_s1440` | fibroblast | `divProbFN` | 1.52504948621 | 0.944428266645 | 19/1 |
| `fibroblast_log10_fold_s480` | fibroblast | `divProbFN` | 1.24629548881 | 0.396793247219 | 20/0 |
| `fibroblast_log10_fold_s960` | fibroblast | `divProbFN` | 1.62212518562 | 0.756428675219 | 20/0 |
| `fibroblast_population_zero_status` | failure | `divProbFN` | 0.150000000000 | 0.396991610971 | 20/0 |
| `fibroblast_total_s0` | fibroblast | `activProbE` | 0 | 0 | 20/0 |
| `fibroblast_total_s1440` | fibroblast | `dieProbFN` | 2564.63333333 | 2081.42353699 | 20/0 |
| `fibroblast_total_s480` | fibroblast | `divProbFN` | 944.550000000 | 549.639353050 | 20/0 |
| `fibroblast_total_s960` | fibroblast | `dieProbFN` | 1940.25000000 | 1594.47469901 | 20/0 |
| `invalid_denominator_status` | failure | `dieProbMP` | 0.450000000000 | 0.677758603889 | 20/0 |
| `jnkp_fraction_s0` | jnk | `initialJnkPositiveTenths` | 0.511742180730 | 0.0911564183108 | 20/0 |
| `jnkp_fraction_s1440` | jnk | `pOffMax` | 0.479697871197 | 0.291091623503 | 18/2 |
| `jnkp_fraction_s480` | jnk | `pOffMax` | 0.547495960843 | 0.240821119138 | 20/0 |
| `jnkp_fraction_s960` | jnk | `pOffMax` | 0.495797498545 | 0.205518567727 | 18/2 |
| `jnkp_rim_fraction_s0` | jnk | `activProbE` | 0 | 0 | 20/0 |
| `jnkp_rim_fraction_s1440` | jnk | `activProbE` | 0 | 0 | 15/5 |
| `jnkp_rim_fraction_s480` | jnk | `activProbE` | 0 | 0 | 17/3 |
| `jnkp_rim_fraction_s960` | jnk | `activProbE` | 0 | 0 | 16/4 |
| `macrophage_activated_fraction_s0` | macrophage | `activProbE` | 0 | 0 | 20/0 |
| `macrophage_activated_fraction_s1440` | macrophage | `dieProbMP` | 0.619352765672 | 0.551764246734 | 17/3 |
| `macrophage_activated_fraction_s480` | macrophage | `divProbMP` | 0.405648150499 | 0.360402645916 | 20/0 |
| `macrophage_activated_fraction_s960` | macrophage | `dieProbMP` | 0.594265846895 | 0.476424498210 | 20/0 |
| `macrophage_population_zero_status` | failure | `dieProbMN` | 0.300000000000 | 0.620318725848 | 20/0 |
| `maximum_absolute_standardized_residual` | abc | `divProbMP` | 3.15040744018 | 2.71767850184 | 9/11 |
| `overall_error_status` | failure | `activProbE` | 0 | 0 | 20/0 |
| `overall_finite_status` | failure | `dieProbMP` | 0.450000000000 | 0.677758603889 | 20/0 |
| `overall_invalid_status` | failure | `dieProbMP` | 0.450000000000 | 0.677758603889 | 20/0 |
| `temporal_fibroblast_tumor_correlation` | fibroblast | `divProbFN` | 1.11599573383 | 1.38465650635 | 20/0 |
| `total_abc_distance` | abc | `divProbP` | 3.71657482009 | 3.97362893149 | 10/10 |
| `total_population_s0` | population | `initialMacrophageCount` | 924.850000000 | 1.29540347978 | 20/0 |
| `total_population_s1440` | population | `divProbMN` | 2232.58333333 | 3292.78694856 | 20/0 |
| `total_population_s480` | population | `divProbMN` | 3883 | 3616.21673974 | 20/0 |
| `total_population_s960` | population | `divProbMN` | 3373 | 3680.14582663 | 20/0 |
| `tumor_extinction_status` | failure | `dieProbMP` | 0.350000000000 | 0.587142948612 | 20/0 |
| `tumor_fold_change_s0` | tumor | `activProbE` | 0 | 0 | 20/0 |
| `tumor_fold_change_s1440` | tumor | `pOffMax` | 167.018562253 | 234.208362270 | 20/0 |
| `tumor_fold_change_s480` | tumor | `divProbP` | 55.0174157964 | 91.4938560243 | 20/0 |
| `tumor_fold_change_s960` | tumor | `divProbP` | 102.191755074 | 165.270719773 | 20/0 |
| `tumor_log10_fold_s0` | tumor | `activProbE` | 0 | 0 | 20/0 |
| `tumor_log10_fold_s1440` | tumor | `divProbP` | 2.05184643129 | 1.21208296526 | 17/3 |
| `tumor_log10_fold_s480` | tumor | `divProbP` | 1.10758194983 | 0.582871513803 | 20/0 |
| `tumor_log10_fold_s960` | tumor | `divProbP` | 1.63342069100 | 0.910155982158 | 18/2 |
| `tumor_radius_s0` | tumor | `clusterRadius` | 6.86338310274 | 0.255721778044 | 20/0 |
| `tumor_radius_s1440` | tumor | `divProbP` | 30.9135608836 | 25.0020376728 | 17/3 |
| `tumor_radius_s480` | tumor | `migrProbP` | 15.1922383079 | 8.80212046092 | 20/0 |
| `tumor_radius_s960` | tumor | `migrProbP` | 25.0195461894 | 12.8117329924 | 17/3 |
| `tumor_rms_spread_s0` | tumor | `clusterRadius` | 4.58496217789 | 0.284125082625 | 20/0 |
| `tumor_rms_spread_s1440` | tumor | `divProbP` | 16.3804150331 | 12.7718509728 | 17/3 |
| `tumor_rms_spread_s480` | tumor | `divProbP` | 7.75014008883 | 8.35672159353 | 20/0 |
| `tumor_rms_spread_s960` | tumor | `migrProbP` | 13.0772492908 | 7.49234112409 | 17/3 |
| `tumor_total_s0` | tumor | `initPop` | 30.6666666667 | 16.7555524532 | 20/0 |
| `tumor_total_s1440` | tumor | `pOffMax` | 2216.50000000 | 2133.51805423 | 20/0 |
| `tumor_total_s480` | tumor | `divProbP` | 1378.88333333 | 2188.45009449 | 20/0 |
| `tumor_total_s960` | tumor | `divProbP` | 2415.60000000 | 3769.30729346 | 20/0 |

## 12. Important Currently Fixed Parameters

- Rank 1: `dieProbMP` (score 0.471532288385, status fixed).
- Rank 2: `divProbMP` (score 0.414273972171, status fixed).
- Rank 4: `divProbFN` (score 0.350252259044, status fixed).
- Rank 5: `dieProbFN` (score 0.347145495280, status fixed).
- Rank 6: `divProbMN` (score 0.341289323986, status fixed).
- Rank 8: `stressStrength` (score 0.331587858632, status fixed).
- Rank 9: `dieProbEN` (score 0.328213830132, status fixed).
- Rank 10: `dieProbMN` (score 0.319968982348, status fixed).
- Rank 11: `migrProbP` (score 0.263348610365, status fixed).
- Rank 12: `lambdaStress` (score 0.243176113913, status fixed).
- Rank 13: `divProbEP` (score 0.221816309835, status fixed).
- Rank 14: `dieProbFP` (score 0.221221313566, status fixed).

## 13. Parameters Associated With Extinction or Invalid States

Associations are screening statistics, not causal estimates.

- `dieProbEN` vs EC_POPULATION_ZERO: rank-biserial=0.763881134226, logistic coefficient=7.21771186045, permutation p=0.00497512437811.
- `dieProbMP` vs MACROPHAGE_POPULATION_ZERO: rank-biserial=0.759020758106, logistic coefficient=7.03538648216, permutation p=0.00497512437811.
- `dieProbFN` vs FIBROBLAST_POPULATION_ZERO: rank-biserial=0.717313338428, logistic coefficient=7.14811490307, permutation p=0.00497512437811.
- `pOffMax` vs FIBROBLAST_POPULATION_ZERO: rank-biserial=0.615673864916, logistic coefficient=4.07982729993, permutation p=0.00497512437811.
- `initialJnkPositiveTenths` vs FIBROBLAST_POPULATION_ZERO: rank-biserial=0.566007645919, logistic coefficient=4.00457155884, permutation p=0.00497512437811.
- `dieProbMN` vs MACROPHAGE_POPULATION_ZERO: rank-biserial=0.539882523833, logistic coefficient=3.40774273844, permutation p=0.00497512437811.
- `pOffMax` vs TUMOR_EXTINCT: rank-biserial=0.523894736842, logistic coefficient=2.95368133580, permutation p=0.00497512437811.
- `dieProbP` vs FIBROBLAST_POPULATION_ZERO: rank-biserial=0.519871415248, logistic coefficient=3.62069416611, permutation p=0.00497512437811.
- `dieProbFP` vs FIBROBLAST_POPULATION_ZERO: rank-biserial=0.488298626261, logistic coefficient=2.71598570739, permutation p=0.00497512437811.
- `netN` vs MACROPHAGE_POPULATION_ZERO: rank-biserial=0.464896765443, logistic coefficient=3.22825586231, permutation p=0.00497512437811.
- `stressStrength` vs TUMOR_EXTINCT: rank-biserial=-0.457418889690, logistic coefficient=-2.81149495997, permutation p=0.00497512437811.
- `divProbEP` vs FIBROBLAST_POPULATION_ZERO: rank-biserial=0.435008345645, logistic coefficient=2.57163155381, permutation p=0.00497512437811.
- `divProbFN` vs FIBROBLAST_POPULATION_ZERO: rank-biserial=-0.434555897903, logistic coefficient=-2.76924007553, permutation p=0.00497512437811.
- `migrProbP` vs MACROPHAGE_POPULATION_ZERO: rank-biserial=0.427907995673, logistic coefficient=2.31499101165, permutation p=0.00497512437811.
- `migrProbP` vs TUMOR_EXTINCT: rank-biserial=-0.421290555155, logistic coefficient=-2.26061668582, permutation p=0.00497512437811.

## 14. Nonlinear/Interaction Candidates

See class B/C rows in `PARAMETER_INFLUENCE_CLASSIFICATION.csv`. High sigma can also reflect stochastic variability; multi-seed confirmation is required before biological interpretation.

## 15. Parameters Confirmed as Low Influence

Only class D parameters with adequate valid effects and acceptable signal-to-noise qualify statistically. Structurally inactive class E fields are implementation findings, not evidence of biological unimportance. A short pilot cannot confirm low influence.

## 16. One-Seed Versus Multi-Seed Comparison

Not yet run. Use the documented `--confirm-only --confirmation-replicates 3` command after the primary run.

## 17. Recommended Parameters for Future Calibration

Current overall candidates (provisional when trajectories <10): `dieProbMP`, `divProbMP`, `pOffMax`, `divProbFN`, `dieProbFN`, `divProbMN`, `divProbP`, `stressStrength`, `dieProbEN`, `dieProbMN`. Use per-output top tiers after the 20-trajectory screen and three-seed confirmation. Favor parameters influencing measured tumour, JNK, fibroblast, macrophage, and EC outputs with adequate SNR; retain failure-associated parameters even if continuous-output mu-star is moderate. Sensitivity alone does not establish identifiability.

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
