# Global Morris Sensitivity Report

Generated: 2026-07-17T18:46:58.129138Z

## 1. Executive Summary

This run performs global Morris elementary-effects screening of the untreated TNBC lung-metastasis ABM. It is **screening, not final calibration**, and Morris indices are **not Sobol indices**. The run used 20 trajectories, 1 matched stochastic replicate(s), 45 parameters, and 920 simulations. 357 runs met the strict finite-output definition; 240 were tumour-extinct and 0 errored. With fewer than 10 trajectories, all rankings should be treated as pipeline/pilot evidence only.

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

Expected and recorded simulations: 920. Each trajectory contains k+1=46 points.

## 9. Seed Strategy

Master seed `9001` deterministically generates a separate design seed and simulation seeds. For replicate r, every point in trajectory t uses the same simulation seed, providing common random numbers for adjacent effects. Different trajectories/replicates use different seeds.

## 10. Parallelization Strategy

A bounded Java executor uses 8 threads. Each task creates a fresh `ExampleGrid`, immutable copied parameters, and a fresh HAL `Rand`; no model or RNG instance is shared. Atomic per-run checkpoints permit deterministic resume.

## 11. Per-Output Sensitivity Results

The primary results are `morris_summary_by_output.csv`. The table below gives the leading valid parameter for every emitted output. Signed mu describes direction; mu-star describes magnitude; sigma reflects trajectory dependence/nonlinearity/interactions and stochastic variability.

| Output | Family | Top parameter | mu-star | sigma | valid/lost |
|---|---|---|---:|---:|---:|
| `abc_target_01_jnkp_s480_standardized_residual` | jnk | `stressStrength` | 3.18638206492 | 2.68901808209 | 19/1 |
| `abc_target_01_jnkp_s480_stat` | jnk | `stressStrength` | 0.637276412983 | 0.537803616418 | 19/1 |
| `abc_target_02_jnkp_s1440_standardized_residual` | jnk | `pOffMax` | 2.22508458535 | 1.01685185674 | 13/7 |
| `abc_target_02_jnkp_s1440_stat` | jnk | `pOffMax` | 0.445016917071 | 0.203370371348 | 13/7 |
| `abc_target_03_ec_s480_standardized_residual` | endothelial | `dieProbEP` | 1.69589895403 | 1.50432932840 | 20/0 |
| `abc_target_03_ec_s480_stat` | endothelial | `dieProbEP` | 0.339179790806 | 0.300865865680 | 20/0 |
| `abc_target_04_ec_s960_standardized_residual` | endothelial | `divProbMP` | 2.41336967125 | 2.32788370694 | 15/5 |
| `abc_target_04_ec_s960_stat` | endothelial | `divProbMP` | 0.482673934250 | 0.465576741388 | 15/5 |
| `abc_target_05_ec_s1440_standardized_residual` | endothelial | `divProbMP` | 3.07027160768 | 3.14039264160 | 12/8 |
| `abc_target_05_ec_s1440_stat` | endothelial | `divProbMP` | 0.614054321537 | 0.628078528321 | 12/8 |
| `abc_target_06_mac_s1440_standardized_residual` | macrophage | `divProbMN` | 2.60754899115 | 2.67923000362 | 18/2 |
| `abc_target_06_mac_s1440_stat` | macrophage | `divProbMN` | 0.521509798230 | 0.535846000724 | 18/2 |
| `abc_target_07_fibro_s1440_standardized_residual` | fibroblast | `divProbFN` | 5.14160655794 | 3.60775735922 | 18/2 |
| `abc_target_07_fibro_s1440_stat` | fibroblast | `divProbFN` | 1.38823377064 | 0.974094486989 | 18/2 |
| `abc_target_08_tumor_s480_standardized_residual` | tumor | `divProbP` | 4.12249374828 | 2.99335226243 | 18/2 |
| `abc_target_08_tumor_s480_stat` | tumor | `divProbP` | 1.11307331204 | 0.808205110856 | 18/2 |
| `abc_target_09_tumor_s960_standardized_residual` | tumor | `divProbP` | 5.97555992324 | 3.23515481953 | 15/5 |
| `abc_target_09_tumor_s960_stat` | tumor | `divProbP` | 1.61340117927 | 0.873491801274 | 15/5 |
| `abc_target_10_tumor_s1440_standardized_residual` | tumor | `divProbP` | 7.31749544665 | 5.83150019699 | 14/6 |
| `abc_target_10_tumor_s1440_stat` | tumor | `divProbP` | 1.97572377060 | 1.57450505319 | 14/6 |
| `active_macrophage_ec_colocalization_s1440` | macrophage | `dieProbEP` | 0.246257446570 | 0.326126020631 | 16/4 |
| `active_macrophage_ec_colocalization_s480` | macrophage | `dieProbEP` | 0.313455483701 | 0.263955661430 | 19/1 |
| `active_macrophage_ec_colocalization_s960` | macrophage | `dieProbEP` | 0.369870386334 | 0.426876896778 | 18/2 |
| `cumulative_chemo_tumor_deaths_s0` | tumor | `activProbE` | 0 | 0 | 20/0 |
| `cumulative_chemo_tumor_deaths_s1440` | tumor | `activProbE` | 0 | 0 | 20/0 |
| `cumulative_chemo_tumor_deaths_s480` | tumor | `activProbE` | 0 | 0 | 20/0 |
| `cumulative_chemo_tumor_deaths_s960` | tumor | `activProbE` | 0 | 0 | 20/0 |
| `cumulative_chemo_tumor_divisions_s0` | tumor | `activProbE` | 0 | 0 | 20/0 |
| `cumulative_chemo_tumor_divisions_s1440` | tumor | `activProbE` | 0 | 0 | 20/0 |
| `cumulative_chemo_tumor_divisions_s480` | tumor | `activProbE` | 0 | 0 | 20/0 |
| `cumulative_chemo_tumor_divisions_s960` | tumor | `activProbE` | 0 | 0 | 20/0 |
| `cumulative_ec_deaths_s0` | endothelial | `activProbE` | 0 | 0 | 20/0 |
| `cumulative_ec_deaths_s1440` | endothelial | `dieProbEP` | 260.083333333 | 614.305168986 | 20/0 |
| `cumulative_ec_deaths_s480` | endothelial | `dieProbEN` | 127.416666667 | 43.7405796040 | 20/0 |
| `cumulative_ec_deaths_s960` | endothelial | `divProbEP` | 174.666666667 | 270.885811380 | 20/0 |
| `cumulative_ec_divisions_s0` | endothelial | `activProbE` | 0 | 0 | 20/0 |
| `cumulative_ec_divisions_s1440` | endothelial | `dieProbEP` | 561.666666667 | 1363.96755291 | 20/0 |
| `cumulative_ec_divisions_s480` | endothelial | `divProbEP` | 125.083333333 | 182.639416427 | 20/0 |
| `cumulative_ec_divisions_s960` | endothelial | `dieProbEP` | 237.166666667 | 465.948695818 | 20/0 |
| `cumulative_fibroblast_deaths_s0` | fibroblast | `activProbE` | 0 | 0 | 20/0 |
| `cumulative_fibroblast_deaths_s1440` | fibroblast | `divProbFP` | 6286.33333333 | 8119.49032444 | 20/0 |
| `cumulative_fibroblast_deaths_s480` | fibroblast | `divProbFN` | 552.166666667 | 304.132406145 | 20/0 |
| `cumulative_fibroblast_deaths_s960` | fibroblast | `divProbFN` | 2241.16666667 | 1525.22207184 | 20/0 |
| `cumulative_fibroblast_divisions_s0` | fibroblast | `activProbE` | 0 | 0 | 20/0 |
| `cumulative_fibroblast_divisions_s1440` | fibroblast | `divProbFP` | 8180.66666667 | 9906.98264295 | 20/0 |
| `cumulative_fibroblast_divisions_s480` | fibroblast | `divProbFN` | 1484.50000000 | 555.364350137 | 20/0 |
| `cumulative_fibroblast_divisions_s960` | fibroblast | `divProbFN` | 4010.50000000 | 2346.55203692 | 20/0 |
| `cumulative_macrophage_deaths_s0` | macrophage | `activProbE` | 0 | 0 | 20/0 |
| `cumulative_macrophage_deaths_s1440` | macrophage | `dieProbMP` | 24131.0000000 | 45533.5437837 | 20/0 |
| `cumulative_macrophage_deaths_s480` | macrophage | `divProbMN` | 2059.16666667 | 1836.26006420 | 20/0 |
| `cumulative_macrophage_deaths_s960` | macrophage | `dieProbMP` | 9740.91666667 | 19569.5664135 | 20/0 |
| `cumulative_macrophage_divisions_s0` | macrophage | `activProbE` | 0 | 0 | 20/0 |
| `cumulative_macrophage_divisions_s1440` | macrophage | `dieProbMP` | 24930.9166667 | 46094.7157520 | 20/0 |
| `cumulative_macrophage_divisions_s480` | macrophage | `divProbMN` | 5423.83333333 | 4838.85112748 | 20/0 |
| `cumulative_macrophage_divisions_s960` | macrophage | `divProbMP` | 10989.3333333 | 21492.8626267 | 20/0 |
| `cumulative_tumor_deaths_s0` | tumor | `activProbE` | 0 | 0 | 20/0 |
| `cumulative_tumor_deaths_s1440` | tumor | `divProbP` | 8052.50000000 | 13347.8318376 | 20/0 |
| `cumulative_tumor_deaths_s480` | tumor | `divProbP` | 401.333333333 | 410.681077441 | 20/0 |
| `cumulative_tumor_deaths_s960` | tumor | `divProbP` | 2859.25000000 | 4078.12272182 | 20/0 |
| `cumulative_tumor_divisions_s0` | tumor | `activProbE` | 0 | 0 | 20/0 |
| `cumulative_tumor_divisions_s1440` | tumor | `divProbP` | 9895.33333333 | 16401.5180851 | 20/0 |
| `cumulative_tumor_divisions_s480` | tumor | `divProbP` | 978.916666667 | 1317.86393211 | 20/0 |
| `cumulative_tumor_divisions_s960` | tumor | `divProbP` | 4506.66666667 | 7047.28023935 | 20/0 |
| `ec_activated_fraction_s0` | endothelial | `activProbE` | 0 | 0 | 20/0 |
| `ec_activated_fraction_s1440` | endothelial | `divProbMP` | 0.614054321537 | 0.628078528321 | 12/8 |
| `ec_activated_fraction_s480` | endothelial | `dieProbEP` | 0.339179790806 | 0.300865865680 | 20/0 |
| `ec_activated_fraction_s960` | endothelial | `divProbMP` | 0.482673934250 | 0.465576741388 | 15/5 |
| `ec_population_zero_status` | failure | `dieProbEN` | 0.916666666667 | 0.850696309223 | 20/0 |
| `fibroblast_fold_change_s0` | fibroblast | `activProbE` | 0 | 0 | 20/0 |
| `fibroblast_fold_change_s1440` | fibroblast | `divProbFN` | 15.0633802817 | 13.9041941004 | 20/0 |
| `fibroblast_fold_change_s480` | fibroblast | `divProbFN` | 6.56572769953 | 3.64039701097 | 20/0 |
| `fibroblast_fold_change_s960` | fibroblast | `divProbFN` | 12.4600938967 | 9.88898451991 | 20/0 |
| `fibroblast_log10_fold_s0` | fibroblast | `activProbE` | 0 | 0 | 20/0 |
| `fibroblast_log10_fold_s1440` | fibroblast | `divProbFN` | 1.38823377064 | 0.974094486989 | 18/2 |
| `fibroblast_log10_fold_s480` | fibroblast | `divProbFN` | 1.16144440069 | 0.557489061562 | 20/0 |
| `fibroblast_log10_fold_s960` | fibroblast | `divProbFN` | 1.22881947642 | 0.734325953466 | 18/2 |
| `fibroblast_population_zero_status` | failure | `dieProbFN` | 0.166666666667 | 0.512989176043 | 20/0 |
| `fibroblast_total_s0` | fibroblast | `activProbE` | 0 | 0 | 20/0 |
| `fibroblast_total_s1440` | fibroblast | `divProbFN` | 2139.00000000 | 1974.39556226 | 20/0 |
| `fibroblast_total_s480` | fibroblast | `divProbFN` | 932.333333333 | 516.936375557 | 20/0 |
| `fibroblast_total_s960` | fibroblast | `divProbFN` | 1769.33333333 | 1404.23580183 | 20/0 |
| `invalid_denominator_status` | failure | `dieProbEN` | 0.833333333333 | 0.854981960071 | 20/0 |
| `jnkp_fraction_s0` | jnk | `initialJnkPositiveTenths` | 0.435420672796 | 0.163735874583 | 20/0 |
| `jnkp_fraction_s1440` | jnk | `pOffMax` | 0.445016917071 | 0.203370371348 | 13/7 |
| `jnkp_fraction_s480` | jnk | `stressStrength` | 0.637276412983 | 0.537803616418 | 19/1 |
| `jnkp_fraction_s960` | jnk | `pOffMax` | 0.504183606167 | 0.443287374250 | 17/3 |
| `jnkp_rim_fraction_s0` | jnk | `activProbE` | 0 | 0 | 20/0 |
| `jnkp_rim_fraction_s1440` | jnk | `activProbE` | 0 | 0 | 14/6 |
| `jnkp_rim_fraction_s480` | jnk | `activProbE` | 0 | 0 | 17/3 |
| `jnkp_rim_fraction_s960` | jnk | `activProbE` | 0 | 0 | 16/4 |
| `macrophage_activated_fraction_s0` | macrophage | `activProbE` | 0 | 0 | 20/0 |
| `macrophage_activated_fraction_s1440` | macrophage | `divProbMN` | 0.521509798230 | 0.535846000724 | 18/2 |
| `macrophage_activated_fraction_s480` | macrophage | `divProbMP` | 0.424563459971 | 0.369062780458 | 20/0 |
| `macrophage_activated_fraction_s960` | macrophage | `dieProbMP` | 0.499941450162 | 0.486447556429 | 20/0 |
| `macrophage_population_zero_status` | failure | `divProbMP` | 0.416666666667 | 0.740436097199 | 20/0 |
| `maximum_absolute_standardized_residual` | abc | `dieProbMP` | 6.78192901697 | 6.36336395457 | 4/16 |
| `overall_error_status` | failure | `activProbE` | 0 | 0 | 20/0 |
| `overall_finite_status` | failure | `dieProbEN` | 0.833333333333 | 0.854981960071 | 20/0 |
| `overall_invalid_status` | failure | `dieProbEN` | 0.833333333333 | 0.854981960071 | 20/0 |
| `temporal_fibroblast_tumor_correlation` | fibroblast | `divProbP` | 1.34076016802 | 1.70837342757 | 20/0 |
| `total_abc_distance` | abc | `dieProbMP` | 6.55478787987 | 7.22224967362 | 4/16 |
| `total_population_s0` | population | `initialMacrophageCount` | 924 | 2.83049393330 | 20/0 |
| `total_population_s1440` | population | `dieProbMN` | 2578.41666667 | 3443.27479422 | 20/0 |
| `total_population_s480` | population | `divProbMN` | 3085.75000000 | 3571.55331234 | 20/0 |
| `total_population_s960` | population | `dieProbMN` | 2884.83333333 | 3448.29970815 | 20/0 |
| `tumor_extinction_status` | failure | `divProbMN` | 0.500000000000 | 0.871913939634 | 20/0 |
| `tumor_fold_change_s0` | tumor | `activProbE` | 0 | 0 | 20/0 |
| `tumor_fold_change_s1440` | tumor | `pOnMax` | 103.709685631 | 300.291088810 | 20/0 |
| `tumor_fold_change_s480` | tumor | `divProbP` | 31.8923112838 | 75.4670468826 | 20/0 |
| `tumor_fold_change_s960` | tumor | `divProbP` | 94.8077134283 | 239.412300909 | 20/0 |
| `tumor_log10_fold_s0` | tumor | `activProbE` | 0 | 0 | 20/0 |
| `tumor_log10_fold_s1440` | tumor | `divProbP` | 1.97572377060 | 1.57450505319 | 14/6 |
| `tumor_log10_fold_s480` | tumor | `divProbP` | 1.11307331204 | 0.808205110856 | 18/2 |
| `tumor_log10_fold_s960` | tumor | `divProbP` | 1.61340117927 | 0.873491801274 | 15/5 |
| `tumor_radius_s0` | tumor | `clusterRadius` | 6.58899771374 | 0.787752096113 | 20/0 |
| `tumor_radius_s1440` | tumor | `pOnMax` | 31.2504913625 | 36.9449253311 | 14/6 |
| `tumor_radius_s480` | tumor | `clusterRadius` | 14.7773718796 | 16.1382364623 | 18/2 |
| `tumor_radius_s960` | tumor | `migrProbP` | 24.3724273889 | 21.7312233458 | 16/4 |
| `tumor_rms_spread_s0` | tumor | `clusterRadius` | 4.51029682380 | 0.433904133734 | 20/0 |
| `tumor_rms_spread_s1440` | tumor | `divProbMP` | 17.4554363489 | 24.1072308235 | 15/5 |
| `tumor_rms_spread_s480` | tumor | `migrProbP` | 7.28657865762 | 6.41852663787 | 18/2 |
| `tumor_rms_spread_s960` | tumor | `migrProbP` | 11.9164569399 | 11.2737161884 | 16/4 |
| `tumor_total_s0` | tumor | `initPop` | 25.2500000000 | 14.8803709160 | 20/0 |
| `tumor_total_s1440` | tumor | `divProbP` | 1852 | 3160.93632493 | 20/0 |
| `tumor_total_s480` | tumor | `divProbP` | 579.416666667 | 997.661721637 | 20/0 |
| `tumor_total_s960` | tumor | `divProbP` | 1649.75000000 | 3112.52504041 | 20/0 |

## 12. Important Currently Fixed Parameters

- Rank 1: `divProbMP` (score 0.516678761519, status fixed).
- Rank 2: `dieProbMP` (score 0.497298823778, status fixed).
- Rank 3: `divProbFN` (score 0.423818575876, status fixed).
- Rank 4: `stressStrength` (score 0.423152887154, status fixed).
- Rank 5: `divProbMN` (score 0.403417674000, status fixed).
- Rank 6: `dieProbMN` (score 0.386751104145, status fixed).
- Rank 9: `dieProbFN` (score 0.350511632722, status fixed).
- Rank 10: `migrProbP` (score 0.348521796276, status fixed).
- Rank 11: `dieProbEN` (score 0.344587896255, status fixed).
- Rank 12: `clusterRadius` (score 0.338084882561, status fixed).
- Rank 13: `lambdaStress` (score 0.325208962171, status fixed).
- Rank 14: `dieProbL` (score 0.320584325634, status fixed).

## 13. Parameters Associated With Extinction or Invalid States

Associations are screening statistics, not causal estimates.

- `dieProbFN` vs FIBROBLAST_POPULATION_ZERO: rank-biserial=0.717713533805, logistic coefficient=5.74064659014, permutation p=0.00497512437811.
- `dieProbL` vs FIBROBLAST_POPULATION_ZERO: rank-biserial=0.675567939936, logistic coefficient=5.23080097507, permutation p=0.00497512437811.
- `divProbFN` vs FIBROBLAST_POPULATION_ZERO: rank-biserial=-0.622818220519, logistic coefficient=-3.96835980519, permutation p=0.00497512437811.
- `initPop` vs FIBROBLAST_POPULATION_ZERO: rank-biserial=-0.604822168041, logistic coefficient=-3.98117836700, permutation p=0.00497512437811.
- `dieProbEN` vs EC_POPULATION_ZERO: rank-biserial=0.587701147785, logistic coefficient=3.86780472763, permutation p=0.00497512437811.
- `dieProbP` vs FIBROBLAST_POPULATION_ZERO: rank-biserial=0.567475521499, logistic coefficient=3.81459658416, permutation p=0.00497512437811.
- `fibroblastSignalBoost` vs FIBROBLAST_POPULATION_ZERO: rank-biserial=-0.546925190603, logistic coefficient=-3.74111446423, permutation p=0.00497512437811.
- `dieProbMN` vs MACROPHAGE_POPULATION_ZERO: rank-biserial=0.543335129689, logistic coefficient=3.11005678332, permutation p=0.00497512437811.
- `migrProbP` vs FIBROBLAST_POPULATION_ZERO: rank-biserial=0.506482449011, logistic coefficient=3.56358674282, permutation p=0.00497512437811.
- `endothelialDaughterDivisionBoost` vs FIBROBLAST_POPULATION_ZERO: rank-biserial=-0.500522466040, logistic coefficient=-2.68906896770, permutation p=0.00497512437811.
- `initialLungCount` vs FIBROBLAST_POPULATION_ZERO: rank-biserial=0.481365377917, logistic coefficient=2.82839663575, permutation p=0.00497512437811.
- `dieProbEN` vs GENERAL_INVALID: rank-biserial=0.478822434835, logistic coefficient=2.74338365464, permutation p=0.00497512437811.
- `divProbFN` vs MACROPHAGE_POPULATION_ZERO: rank-biserial=-0.450830619274, logistic coefficient=-2.65295902320, permutation p=0.00497512437811.
- `migrProbN` vs FIBROBLAST_POPULATION_ZERO: rank-biserial=0.433337203452, logistic coefficient=3.07468588383, permutation p=0.00497512437811.
- `fibroblastSignalCap` vs FIBROBLAST_POPULATION_ZERO: rank-biserial=-0.423584504044, logistic coefficient=-2.53313974316, permutation p=0.00497512437811.

## 14. Nonlinear/Interaction Candidates

See class B/C rows in `PARAMETER_INFLUENCE_CLASSIFICATION.csv`. High sigma can also reflect stochastic variability; multi-seed confirmation is required before biological interpretation.

## 15. Parameters Confirmed as Low Influence

Only class D parameters with adequate valid effects and acceptable signal-to-noise qualify statistically. Structurally inactive class E fields are implementation findings, not evidence of biological unimportance. A short pilot cannot confirm low influence.

## 16. One-Seed Versus Multi-Seed Comparison

Not yet run. Use the documented `--confirm-only --confirmation-replicates 3` command after the primary run.

## 17. Recommended Parameters for Future Calibration

Current overall candidates (provisional when trajectories <10): `divProbMP`, `dieProbMP`, `divProbFN`, `stressStrength`, `divProbMN`, `dieProbMN`, `divProbP`, `pOffMax`, `dieProbFN`, `migrProbP`. Use per-output top tiers after the 20-trajectory screen and three-seed confirmation. Favor parameters influencing measured tumour, JNK, fibroblast, macrophage, and EC outputs with adequate SNR; retain failure-associated parameters even if continuous-output mu-star is moderate. Sensitivity alone does not establish identifiability.

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
