# Global Morris Sensitivity Report

Generated: 2026-07-22T18:06:52.061941Z

## 1. Executive Summary

This run performs global Morris elementary-effects screening of the untreated TNBC lung-metastasis ABM. It is **screening, not final calibration**, and Morris indices are **not Sobol indices**. The run used 2 trajectories, 2 matched stochastic replicate(s), 45 parameters, and 184 simulations. 118 runs met the strict finite-output definition; 60 were tumour-extinct and 0 errored. With fewer than 10 trajectories, all rankings should be treated as pipeline/pilot evidence only.

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

Expected and recorded simulations: 184. Each trajectory contains k+1=46 points.

## 9. Seed Strategy

Master seed `2026072201` deterministically generates a separate design seed and simulation seeds. For replicate r, every point in trajectory t uses the same simulation seed, providing common random numbers for adjacent effects. Different trajectories/replicates use different seeds.

## 10. Parallelization Strategy

A bounded Java executor uses 4 threads. Each task creates a fresh `ExampleGrid`, immutable copied parameters, and a fresh HAL `Rand`; no model or RNG instance is shared. Atomic per-run checkpoints permit deterministic resume.

## 11. Per-Output Sensitivity Results

The primary results are `morris_summary_by_output.csv`. The table below gives the leading valid parameter for every emitted output. Signed mu describes direction; mu-star describes magnitude; sigma reflects trajectory dependence/nonlinearity/interactions and stochastic variability.

| Output | Family | Top parameter | mu-star | sigma | valid/lost |
|---|---|---|---:|---:|---:|
| `abc_target_01_jnkp_s480_standardized_residual` | jnk | `pOffMax` | 2.29290780142 | 1.93320430784 | 2/0 |
| `abc_target_01_jnkp_s480_stat` | jnk | `pOffMax` | 0.458581560284 | 0.386640861568 | 2/0 |
| `abc_target_02_jnkp_s1440_standardized_residual` | jnk | `dieProbEP` | 8.33333333333 |  | 1/1 |
| `abc_target_02_jnkp_s1440_stat` | jnk | `dieProbEP` | 1.66666666667 |  | 1/1 |
| `abc_target_03_ec_s480_standardized_residual` | endothelial | `divProbMP` | 4.12157557645 | 2.21572053434 | 2/0 |
| `abc_target_03_ec_s480_stat` | endothelial | `divProbMP` | 0.824315115290 | 0.443144106868 | 2/0 |
| `abc_target_04_ec_s960_standardized_residual` | endothelial | `stressStrength` | 4.54130116959 | 6.42236970486 | 2/0 |
| `abc_target_04_ec_s960_stat` | endothelial | `stressStrength` | 0.908260233918 | 1.28447394097 | 2/0 |
| `abc_target_05_ec_s1440_standardized_residual` | endothelial | `dieProbEN` | 2.64858125398 | 1.24259845885 | 2/0 |
| `abc_target_05_ec_s1440_stat` | endothelial | `dieProbEN` | 0.529716250795 | 0.248519691770 | 2/0 |
| `abc_target_06_mac_s1440_standardized_residual` | macrophage | `divProbMP` | 4.76064342260 | 4.50089012343 | 2/0 |
| `abc_target_06_mac_s1440_stat` | macrophage | `divProbMP` | 0.952128684521 | 0.900178024685 | 2/0 |
| `abc_target_07_fibro_s1440_standardized_residual` | fibroblast | `divProbFN` | 4.85173129303 | 5.54551669043 | 2/0 |
| `abc_target_07_fibro_s1440_stat` | fibroblast | `divProbFN` | 1.30996744912 | 1.49728950642 | 2/0 |
| `abc_target_08_tumor_s480_standardized_residual` | tumor | `divProbMP` | 4.93046730872 | 3.93090506856 | 2/0 |
| `abc_target_08_tumor_s480_stat` | tumor | `divProbMP` | 1.33122617335 | 1.06134436851 | 2/0 |
| `abc_target_09_tumor_s960_standardized_residual` | tumor | `pOffMax` | 8.53729076536 |  | 1/1 |
| `abc_target_09_tumor_s960_stat` | tumor | `pOffMax` | 2.30506850665 |  | 1/1 |
| `abc_target_10_tumor_s1440_standardized_residual` | tumor | `pOffMax` | 14.5581925758 |  | 1/1 |
| `abc_target_10_tumor_s1440_stat` | tumor | `pOffMax` | 3.93071199546 |  | 1/1 |
| `active_macrophage_ec_colocalization_s1440` | macrophage | `endothelialMacrophageRadius` | 0.778081127774 |  | 1/1 |
| `active_macrophage_ec_colocalization_s480` | macrophage | `macrophageEndothelialBiasRadius` | 0.672984085197 | 0.0684014206009 | 2/0 |
| `active_macrophage_ec_colocalization_s960` | macrophage | `initPop` | 0.802877663164 | 1.13544048017 | 2/0 |
| `cumulative_chemo_tumor_deaths_s0` | tumor | `activProbE` | 0 | 0 | 2/0 |
| `cumulative_chemo_tumor_deaths_s1440` | tumor | `activProbE` | 0 | 0 | 2/0 |
| `cumulative_chemo_tumor_deaths_s480` | tumor | `activProbE` | 0 | 0 | 2/0 |
| `cumulative_chemo_tumor_deaths_s960` | tumor | `activProbE` | 0 | 0 | 2/0 |
| `cumulative_chemo_tumor_divisions_s0` | tumor | `activProbE` | 0 | 0 | 2/0 |
| `cumulative_chemo_tumor_divisions_s1440` | tumor | `activProbE` | 0 | 0 | 2/0 |
| `cumulative_chemo_tumor_divisions_s480` | tumor | `activProbE` | 0 | 0 | 2/0 |
| `cumulative_chemo_tumor_divisions_s960` | tumor | `activProbE` | 0 | 0 | 2/0 |
| `cumulative_ec_deaths_s0` | endothelial | `activProbE` | 0 | 0 | 2/0 |
| `cumulative_ec_deaths_s1440` | endothelial | `endothelialMacrophageRadius` | 1905.41666667 | 2689.95204676 | 2/0 |
| `cumulative_ec_deaths_s480` | endothelial | `dieProbEN` | 149.583333333 | 13.5528799727 | 2/0 |
| `cumulative_ec_deaths_s960` | endothelial | `endothelialMacrophageRadius` | 737.083333333 | 1038.85771269 | 2/0 |
| `cumulative_ec_divisions_s0` | endothelial | `activProbE` | 0 | 0 | 2/0 |
| `cumulative_ec_divisions_s1440` | endothelial | `endothelialMacrophageRadius` | 2769.58333333 | 3910.88975561 | 2/0 |
| `cumulative_ec_divisions_s480` | endothelial | `endothelialMacrophageRadius` | 295.416666667 | 413.068211343 | 2/0 |
| `cumulative_ec_divisions_s960` | endothelial | `endothelialMacrophageRadius` | 1375.41666667 | 1939.24034740 | 2/0 |
| `cumulative_fibroblast_deaths_s0` | fibroblast | `activProbE` | 0 | 0 | 2/0 |
| `cumulative_fibroblast_deaths_s1440` | fibroblast | `divProbFN` | 3557.08333333 | 2533.21004360 | 2/0 |
| `cumulative_fibroblast_deaths_s480` | fibroblast | `divProbFN` | 451.666666667 | 98.9949493661 | 2/0 |
| `cumulative_fibroblast_deaths_s960` | fibroblast | `divProbFN` | 1797.08333333 | 988.181726708 | 2/0 |
| `cumulative_fibroblast_divisions_s0` | fibroblast | `activProbE` | 0 | 0 | 2/0 |
| `cumulative_fibroblast_divisions_s1440` | fibroblast | `divProbMN` | 6612.50000000 | 9351.48718119 | 2/0 |
| `cumulative_fibroblast_divisions_s480` | fibroblast | `divProbFN` | 1759.16666667 | 745.997654152 | 2/0 |
| `cumulative_fibroblast_divisions_s960` | fibroblast | `divProbFN` | 4069.16666667 | 2975.74103749 | 2/0 |
| `cumulative_macrophage_deaths_s0` | macrophage | `activProbE` | 0 | 0 | 2/0 |
| `cumulative_macrophage_deaths_s1440` | macrophage | `dieProbMP` | 46075.4166667 | 64983.7024467 | 2/0 |
| `cumulative_macrophage_deaths_s480` | macrophage | `divProbMN` | 5583.33333333 | 7896.02572325 | 2/0 |
| `cumulative_macrophage_deaths_s960` | macrophage | `dieProbMP` | 24603.7500000 | 34364.8003100 | 2/0 |
| `cumulative_macrophage_divisions_s0` | macrophage | `activProbE` | 0 | 0 | 2/0 |
| `cumulative_macrophage_divisions_s1440` | macrophage | `divProbMP` | 49493.7500000 | 69994.7325027 | 2/0 |
| `cumulative_macrophage_divisions_s480` | macrophage | `divProbMN` | 10645.0000000 | 15054.3033715 | 2/0 |
| `cumulative_macrophage_divisions_s960` | macrophage | `divProbMP` | 27428.3333333 | 38789.5209933 | 2/0 |
| `cumulative_tumor_deaths_s0` | tumor | `activProbE` | 0 | 0 | 2/0 |
| `cumulative_tumor_deaths_s1440` | tumor | `dieProbMN` | 2732.08333333 | 3790.68160281 | 2/0 |
| `cumulative_tumor_deaths_s480` | tumor | `initPop` | 465.416666667 | 286.967502032 | 2/0 |
| `cumulative_tumor_deaths_s960` | tumor | `initPop` | 1177.08333333 | 770.157135842 | 2/0 |
| `cumulative_tumor_divisions_s0` | tumor | `activProbE` | 0 | 0 | 2/0 |
| `cumulative_tumor_divisions_s1440` | tumor | `pOffMax` | 3232.50000000 | 4571.44534037 | 2/0 |
| `cumulative_tumor_divisions_s480` | tumor | `initPop` | 584.166666667 | 308.769961118 | 2/0 |
| `cumulative_tumor_divisions_s960` | tumor | `pOffMax` | 1485.83333333 | 2101.28565143 | 2/0 |
| `ec_activated_fraction_s0` | endothelial | `activProbE` | 0 | 0 | 2/0 |
| `ec_activated_fraction_s1440` | endothelial | `dieProbEN` | 0.529716250795 | 0.248519691770 | 2/0 |
| `ec_activated_fraction_s480` | endothelial | `divProbMP` | 0.824315115290 | 0.443144106868 | 2/0 |
| `ec_activated_fraction_s960` | endothelial | `stressStrength` | 0.908260233918 | 1.28447394097 | 2/0 |
| `ec_population_zero_status` | failure | `deactProbE` | 0.833333333333 | 1.17851130198 | 2/0 |
| `fibroblast_fold_change_s0` | fibroblast | `activProbE` | 0 | 0 | 2/0 |
| `fibroblast_fold_change_s1440` | fibroblast | `divProbMN` | 26.8661971831 | 37.9945404257 | 2/0 |
| `fibroblast_fold_change_s480` | fibroblast | `divProbFN` | 9.20774647887 | 4.55635707596 | 2/0 |
| `fibroblast_fold_change_s960` | fibroblast | `divProbFN` | 16.0005868545 | 13.9968965548 | 2/0 |
| `fibroblast_log10_fold_s0` | fibroblast | `activProbE` | 0 | 0 | 2/0 |
| `fibroblast_log10_fold_s1440` | fibroblast | `divProbFN` | 1.30996744912 | 1.49728950642 | 2/0 |
| `fibroblast_log10_fold_s480` | fibroblast | `divProbFN` | 1.19302429794 | 0.360752263528 | 2/0 |
| `fibroblast_log10_fold_s960` | fibroblast | `divProbFN` | 1.27669120644 | 1.01019170898 | 2/0 |
| `fibroblast_population_zero_status` | failure | `activProbE` | 0 | 0 | 2/0 |
| `fibroblast_total_s0` | fibroblast | `activProbE` | 0 | 0 | 2/0 |
| `fibroblast_total_s1440` | fibroblast | `divProbMN` | 3815 | 5395.22474045 | 2/0 |
| `fibroblast_total_s480` | fibroblast | `divProbFN` | 1307.50000000 | 647.002704786 | 2/0 |
| `fibroblast_total_s960` | fibroblast | `divProbFN` | 2272.08333333 | 1987.55931079 | 2/0 |
| `invalid_denominator_status` | failure | `clusterRadius` | 0.833333333333 | 1.17851130198 | 2/0 |
| `jnkp_fraction_s0` | jnk | `initialJnkPositiveTenths` | 0.441857298475 | 0.0683613582029 | 2/0 |
| `jnkp_fraction_s1440` | jnk | `dieProbEP` | 1.66666666667 |  | 1/1 |
| `jnkp_fraction_s480` | jnk | `pOffMax` | 0.458581560284 | 0.386640861568 | 2/0 |
| `jnkp_fraction_s960` | jnk | `pOffMax` | 0.516403890263 |  | 1/1 |
| `jnkp_rim_fraction_s0` | jnk | `activProbE` | 0 | 0 | 2/0 |
| `jnkp_rim_fraction_s1440` | jnk | `activProbE` | 0 |  | 1/1 |
| `jnkp_rim_fraction_s480` | jnk | `activProbE` | 0 | 0 | 2/0 |
| `jnkp_rim_fraction_s960` | jnk | `activProbE` | 0 |  | 1/1 |
| `macrophage_activated_fraction_s0` | macrophage | `activProbE` | 0 | 0 | 2/0 |
| `macrophage_activated_fraction_s1440` | macrophage | `divProbMP` | 0.952128684521 | 0.900178024685 | 2/0 |
| `macrophage_activated_fraction_s480` | macrophage | `divProbMP` | 0.628580124874 | 0.788230221580 | 2/0 |
| `macrophage_activated_fraction_s960` | macrophage | `divProbMP` | 0.829858672148 | 0.941290147039 | 2/0 |
| `macrophage_population_zero_status` | failure | `activProbE` | 0 | 0 | 2/0 |
| `maximum_absolute_standardized_residual` | abc | `stressStrength` | 10.1742543912 |  | 1/1 |
| `overall_error_status` | failure | `activProbE` | 0 | 0 | 2/0 |
| `overall_finite_status` | failure | `clusterRadius` | 0.833333333333 | 1.17851130198 | 2/0 |
| `overall_invalid_status` | failure | `clusterRadius` | 0.833333333333 | 1.17851130198 | 2/0 |
| `temporal_fibroblast_tumor_correlation` | fibroblast | `stressStrength` | 1.84764080154 | 2.61295867994 | 2/0 |
| `total_abc_distance` | abc | `stressStrength` | 10.0936836123 |  | 1/1 |
| `total_population_s0` | population | `initialMacrophageCount` | 925 | 0 | 2/0 |
| `total_population_s1440` | population | `divProbMP` | 2859.16666667 | 3464.82322781 | 2/0 |
| `total_population_s480` | population | `divProbMN` | 4447.50000000 | 6289.71481865 | 2/0 |
| `total_population_s960` | population | `divProbMP` | 3915.00000000 | 5371.65451441 | 2/0 |
| `tumor_extinction_status` | failure | `clusterRadius` | 0.833333333333 | 1.17851130198 | 2/0 |
| `tumor_fold_change_s0` | tumor | `activProbE` | 0 | 0 | 2/0 |
| `tumor_fold_change_s1440` | tumor | `initPop` | 57.7328431373 | 79.1994257026 | 2/0 |
| `tumor_fold_change_s480` | tumor | `initPop` | 11.2132352941 | 10.8388377685 | 2/0 |
| `tumor_fold_change_s960` | tumor | `initPop` | 46.9411764706 | 64.2843253132 | 2/0 |
| `tumor_log10_fold_s0` | tumor | `activProbE` | 0 | 0 | 2/0 |
| `tumor_log10_fold_s1440` | tumor | `pOffMax` | 3.93071199546 |  | 1/1 |
| `tumor_log10_fold_s480` | tumor | `divProbMP` | 1.33122617335 | 1.06134436851 | 2/0 |
| `tumor_log10_fold_s960` | tumor | `pOffMax` | 2.30506850665 |  | 1/1 |
| `tumor_radius_s0` | tumor | `clusterRadius` | 7.11237599691 | 0.176469003653 | 2/0 |
| `tumor_radius_s1440` | tumor | `pOffMax` | 63.3725862342 |  | 1/1 |
| `tumor_radius_s480` | tumor | `initialMacrophageCount` | 12.8144384226 | 7.03414110557 | 2/0 |
| `tumor_radius_s960` | tumor | `divProbP` | 36.8063657841 |  | 1/1 |
| `tumor_rms_spread_s0` | tumor | `clusterRadius` | 5.12352882005 | 0.176563893939 | 2/0 |
| `tumor_rms_spread_s1440` | tumor | `pOffMax` | 36.5989728894 |  | 1/1 |
| `tumor_rms_spread_s480` | tumor | `dieProbMN` | 5.49145467781 | 7.76608968251 | 2/0 |
| `tumor_rms_spread_s960` | tumor | `divProbP` | 17.3665502864 |  | 1/1 |
| `tumor_total_s0` | tumor | `initPop` | 40 | 0 | 2/0 |
| `tumor_total_s1440` | tumor | `pOffMax` | 710 | 1004.09162928 | 2/0 |
| `tumor_total_s480` | tumor | `pOffMax` | 218.333333333 | 308.769961118 | 2/0 |
| `tumor_total_s960` | tumor | `pOffMax` | 418.333333333 | 591.612673593 | 2/0 |

## 12. Important Currently Fixed Parameters

- Rank 1: `divProbMP` (score 0.484135635419, status fixed).
- Rank 2: `endothelialMacrophageRadius` (score 0.351318984118, status hard-coded).
- Rank 4: `divProbMN` (score 0.335161387898, status fixed).
- Rank 5: `divProbFN` (score 0.328755727882, status fixed).
- Rank 6: `stressStrength` (score 0.312321471028, status fixed).
- Rank 8: `initialMacrophageCount` (score 0.255429029230, status hard-coded).
- Rank 9: `dieProbEN` (score 0.254019716034, status fixed).
- Rank 11: `initialJnkPositiveTenths` (score 0.236637930862, status hard-coded).
- Rank 13: `lambdaCAF` (score 0.230594235328, status fixed).
- Rank 14: `dieProbMN` (score 0.219540210723, status fixed).
- Rank 15: `initPop` (score 0.218754600519, status fixed CLI).
- Rank 16: `dieProbMP` (score 0.218653585824, status fixed).

## 13. Parameters Associated With Extinction or Invalid States

Associations are screening statistics, not causal estimates.

- `pOnMax` vs EC_POPULATION_ZERO: rank-biserial=0.705592105263, logistic coefficient=5.08930119542, permutation p=0.00497512437811.
- `initialJnkPositiveTenths` vs EC_POPULATION_ZERO: rank-biserial=0.651315789474, logistic coefficient=5.67372908290, permutation p=0.00497512437811.
- `deactProbE` vs EC_POPULATION_ZERO: rank-biserial=0.634868421053, logistic coefficient=6.13138186377, permutation p=0.00497512437811.
- `clusterRadius` vs EC_POPULATION_ZERO: rank-biserial=-0.631578947368, logistic coefficient=-3.98416860945, permutation p=0.00497512437811.
- `dieProbEN` vs EC_POPULATION_ZERO: rank-biserial=0.601973684211, logistic coefficient=5.13963809620, permutation p=0.00497512437811.
- `divProbMP` vs TUMOR_EXTINCT: rank-biserial=0.583602150538, logistic coefficient=2.99162526993, permutation p=0.00497512437811.
- `ecSurvival` vs EC_POPULATION_ZERO: rank-biserial=0.582236842105, logistic coefficient=3.41494032718, permutation p=0.00497512437811.
- `dieProbFP` vs TUMOR_EXTINCT: rank-biserial=0.563978494624, logistic coefficient=2.67321635294, permutation p=0.00497512437811.
- `initialMacrophageCount` vs TUMOR_EXTINCT: rank-biserial=-0.561559139785, logistic coefficient=-9.73166811773, permutation p=0.00497512437811.
- `lambdaCAF` vs TUMOR_EXTINCT: rank-biserial=-0.554569892473, logistic coefficient=-2.89441164837, permutation p=0.00497512437811.
- `cafDivBoost` vs EC_POPULATION_ZERO: rank-biserial=-0.542763157895, logistic coefficient=-4.27491559577, permutation p=0.00497512437811.
- `clusterRadius` vs TUMOR_EXTINCT: rank-biserial=-0.542473118280, logistic coefficient=-2.62435906053, permutation p=0.00497512437811.
- `divProbFN` vs TUMOR_EXTINCT: rank-biserial=-0.539516129032, logistic coefficient=-2.76686738838, permutation p=0.00497512437811.
- `divProbMN` vs TUMOR_EXTINCT: rank-biserial=-0.533602150538, logistic coefficient=-3.35614096694, permutation p=0.00497512437811.
- `initialMacrophageCount` vs GENERAL_INVALID: rank-biserial=-0.509758602979, logistic coefficient=-8.41892869987, permutation p=0.00497512437811.

## 14. Nonlinear/Interaction Candidates

See class B/C rows in `PARAMETER_INFLUENCE_CLASSIFICATION.csv`. High sigma can also reflect stochastic variability; multi-seed confirmation is required before biological interpretation.

## 15. Parameters Confirmed as Low Influence

Only class D parameters with adequate valid effects and acceptable signal-to-noise qualify statistically. Structurally inactive class E fields are implementation findings, not evidence of biological unimportance. A short pilot cannot confirm low influence.

## 16. One-Seed Versus Multi-Seed Comparison

Not yet run. Use the documented `--confirm-only --confirmation-replicates 3` command after the primary run.

## 17. Recommended Parameters for Future Calibration

Current overall candidates (provisional when trajectories <10): `divProbMP`, `endothelialMacrophageRadius`, `pOffMax`, `divProbMN`, `divProbFN`, `stressStrength`, `pOnMax`, `initialMacrophageCount`, `dieProbEN`, `divProbP`. Use per-output top tiers after the 20-trajectory screen and three-seed confirmation. Favor parameters influencing measured tumour, JNK, fibroblast, macrophage, and EC outputs with adequate SNR; retain failure-associated parameters even if continuous-output mu-star is moderate. Sensitivity alone does not establish identifiability.

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
