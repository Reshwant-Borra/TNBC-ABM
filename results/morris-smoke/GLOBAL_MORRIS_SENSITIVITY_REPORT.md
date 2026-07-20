# Global Morris Sensitivity Report

Generated: 2026-07-17T15:14:37.208149Z

## 1. Executive Summary

This run performs global Morris elementary-effects screening of the untreated TNBC lung-metastasis ABM. It is **screening, not final calibration**, and Morris indices are **not Sobol indices**. The run used 2 trajectories, 1 matched stochastic replicate(s), 45 parameters, and 92 simulations. 39 runs met the strict finite-output definition; 10 were tumour-extinct and 0 errored. With fewer than 10 trajectories, all rankings should be treated as pipeline/pilot evidence only.

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

Expected and recorded simulations: 92. Each trajectory contains k+1=46 points.

## 9. Seed Strategy

Master seed `9001` deterministically generates a separate design seed and simulation seeds. For replicate r, every point in trajectory t uses the same simulation seed, providing common random numbers for adjacent effects. Different trajectories/replicates use different seeds.

## 10. Parallelization Strategy

A bounded Java executor uses 12 threads. Each task creates a fresh `ExampleGrid`, immutable copied parameters, and a fresh HAL `Rand`; no model or RNG instance is shared. Atomic per-run checkpoints permit deterministic resume.

## 11. Per-Output Sensitivity Results

The primary results are `morris_summary_by_output.csv`. The table below gives the leading valid parameter for every emitted output. Signed mu describes direction; mu-star describes magnitude; sigma reflects trajectory dependence/nonlinearity/interactions and stochastic variability.

| Output | Family | Top parameter | mu-star | sigma | valid/lost |
|---|---|---|---:|---:|---:|
| `abc_target_01_jnkp_s480_standardized_residual` | jnk | `macrophageInteractionRadius` | 2.07665598291 | 1.97362870283 | 2/0 |
| `abc_target_01_jnkp_s480_stat` | jnk | `macrophageInteractionRadius` | 0.415331196581 | 0.394725740566 | 2/0 |
| `abc_target_02_jnkp_s1440_standardized_residual` | jnk | `dieProbFP` | 2.40379700668 | 3.14780268295 | 2/0 |
| `abc_target_02_jnkp_s1440_stat` | jnk | `dieProbFP` | 0.480759401336 | 0.629560536589 | 2/0 |
| `abc_target_03_ec_s480_standardized_residual` | endothelial | `divProbMP` | 1.86652019735 | 2.63965817753 | 2/0 |
| `abc_target_03_ec_s480_stat` | endothelial | `divProbMP` | 0.373304039470 | 0.527931635507 | 2/0 |
| `abc_target_04_ec_s960_standardized_residual` | endothelial | `cafDivBoost` | 4.16666666667 | 5.89255650989 | 2/0 |
| `abc_target_04_ec_s960_stat` | endothelial | `cafDivBoost` | 0.833333333333 | 1.17851130198 | 2/0 |
| `abc_target_05_ec_s1440_standardized_residual` | endothelial | `deactProbE` | 8.33333333333 |  | 1/1 |
| `abc_target_05_ec_s1440_stat` | endothelial | `deactProbE` | 1.66666666667 |  | 1/1 |
| `abc_target_06_mac_s1440_standardized_residual` | macrophage | `divProbMP` | 4.08445621718 | 3.01927769714 | 2/0 |
| `abc_target_06_mac_s1440_stat` | macrophage | `divProbMP` | 0.816891243436 | 0.603855539428 | 2/0 |
| `abc_target_07_fibro_s1440_standardized_residual` | fibroblast | `dieProbMP` | 5.20533914868 | 3.22739058583 | 2/0 |
| `abc_target_07_fibro_s1440_stat` | fibroblast | `dieProbMP` | 1.40544157014 | 0.871395458174 | 2/0 |
| `abc_target_08_tumor_s480_standardized_residual` | tumor | `dieProbMP` | 7.21592025827 | 0.480446142720 | 2/0 |
| `abc_target_08_tumor_s480_stat` | tumor | `dieProbMP` | 1.94829846973 | 0.129720458534 | 2/0 |
| `abc_target_09_tumor_s960_standardized_residual` | tumor | `dieProbMP` | 11.8136657555 |  | 1/1 |
| `abc_target_09_tumor_s960_stat` | tumor | `dieProbMP` | 3.18968975397 |  | 1/1 |
| `abc_target_10_tumor_s1440_standardized_residual` | tumor | `initialMacrophageCount` | 5.01959427488 |  | 1/1 |
| `abc_target_10_tumor_s1440_stat` | tumor | `initialMacrophageCount` | 1.35529045422 |  | 1/1 |
| `active_macrophage_ec_colocalization_s1440` | macrophage | `dieProbEP` | 0.557719659967 |  | 1/1 |
| `active_macrophage_ec_colocalization_s480` | macrophage | `netN` | 0.531593972144 | 0.712423606466 | 2/0 |
| `active_macrophage_ec_colocalization_s960` | macrophage | `dieProbEP` | 0.293397171274 | 0.414926258777 | 2/0 |
| `cumulative_chemo_tumor_deaths_s0` | tumor | `activProbE` | 0 | 0 | 2/0 |
| `cumulative_chemo_tumor_deaths_s1440` | tumor | `activProbE` | 0 | 0 | 2/0 |
| `cumulative_chemo_tumor_deaths_s480` | tumor | `activProbE` | 0 | 0 | 2/0 |
| `cumulative_chemo_tumor_deaths_s960` | tumor | `activProbE` | 0 | 0 | 2/0 |
| `cumulative_chemo_tumor_divisions_s0` | tumor | `activProbE` | 0 | 0 | 2/0 |
| `cumulative_chemo_tumor_divisions_s1440` | tumor | `activProbE` | 0 | 0 | 2/0 |
| `cumulative_chemo_tumor_divisions_s480` | tumor | `activProbE` | 0 | 0 | 2/0 |
| `cumulative_chemo_tumor_divisions_s960` | tumor | `activProbE` | 0 | 0 | 2/0 |
| `cumulative_ec_deaths_s0` | endothelial | `activProbE` | 0 | 0 | 2/0 |
| `cumulative_ec_deaths_s1440` | endothelial | `dieProbMP` | 626.666666667 | 886.240499087 | 2/0 |
| `cumulative_ec_deaths_s480` | endothelial | `divProbEP` | 213.333333333 | 245.130350811 | 2/0 |
| `cumulative_ec_deaths_s960` | endothelial | `divProbEP` | 374.166666667 | 453.726851261 | 2/0 |
| `cumulative_ec_divisions_s0` | endothelial | `activProbE` | 0 | 0 | 2/0 |
| `cumulative_ec_divisions_s1440` | endothelial | `dieProbMP` | 1078.33333333 | 1524.99362476 | 2/0 |
| `cumulative_ec_divisions_s480` | endothelial | `divProbEP` | 330.000000000 | 412.478955692 | 2/0 |
| `cumulative_ec_divisions_s960` | endothelial | `dieProbMP` | 400.833333333 | 566.863936251 | 2/0 |
| `cumulative_fibroblast_deaths_s0` | fibroblast | `activProbE` | 0 | 0 | 2/0 |
| `cumulative_fibroblast_deaths_s1440` | fibroblast | `dieProbMP` | 12630.8333333 | 14296.5206043 | 2/0 |
| `cumulative_fibroblast_deaths_s480` | fibroblast | `divProbFN` | 926.666666667 | 172.062650089 | 2/0 |
| `cumulative_fibroblast_deaths_s960` | fibroblast | `divProbFN` | 4796.66666667 | 832.028979196 | 2/0 |
| `cumulative_fibroblast_divisions_s0` | fibroblast | `activProbE` | 0 | 0 | 2/0 |
| `cumulative_fibroblast_divisions_s1440` | fibroblast | `dieProbMP` | 18060 | 15506.8517114 | 2/0 |
| `cumulative_fibroblast_divisions_s480` | fibroblast | `divProbFN` | 1960 | 28.2842712475 | 2/0 |
| `cumulative_fibroblast_divisions_s960` | fibroblast | `divProbFN` | 6981.66666667 | 400.693842672 | 2/0 |
| `cumulative_macrophage_deaths_s0` | macrophage | `activProbE` | 0 | 0 | 2/0 |
| `cumulative_macrophage_deaths_s1440` | macrophage | `dieProbMP` | 25449.1666667 | 35990.5566511 | 2/0 |
| `cumulative_macrophage_deaths_s480` | macrophage | `divProbMP` | 1566.66666667 | 2215.60124772 | 2/0 |
| `cumulative_macrophage_deaths_s960` | macrophage | `dieProbMP` | 9538.33333333 | 13489.2403624 | 2/0 |
| `cumulative_macrophage_divisions_s0` | macrophage | `activProbE` | 0 | 0 | 2/0 |
| `cumulative_macrophage_divisions_s1440` | macrophage | `dieProbMP` | 27571.6666667 | 38992.2249372 | 2/0 |
| `cumulative_macrophage_divisions_s480` | macrophage | `divProbMP` | 4685 | 6625.59053972 | 2/0 |
| `cumulative_macrophage_divisions_s960` | macrophage | `dieProbMP` | 9606.66666667 | 13585.8782892 | 2/0 |
| `cumulative_tumor_deaths_s0` | tumor | `activProbE` | 0 | 0 | 2/0 |
| `cumulative_tumor_deaths_s1440` | tumor | `divProbP` | 4714.16666667 | 5896.09204379 | 2/0 |
| `cumulative_tumor_deaths_s480` | tumor | `dieProbMP` | 366.666666667 | 37.7123616633 | 2/0 |
| `cumulative_tumor_deaths_s960` | tumor | `dieProbMP` | 1786.66666667 | 1192.65343760 | 2/0 |
| `cumulative_tumor_divisions_s0` | tumor | `activProbE` | 0 | 0 | 2/0 |
| `cumulative_tumor_divisions_s1440` | tumor | `divProbP` | 5648.33333333 | 7113.49421874 | 2/0 |
| `cumulative_tumor_divisions_s480` | tumor | `dieProbMP` | 601.666666667 | 73.0677007226 | 2/0 |
| `cumulative_tumor_divisions_s960` | tumor | `dieProbMP` | 2266.66666667 | 1659.34391318 | 2/0 |
| `ec_activated_fraction_s0` | endothelial | `activProbE` | 0 | 0 | 2/0 |
| `ec_activated_fraction_s1440` | endothelial | `deactProbE` | 1.66666666667 |  | 1/1 |
| `ec_activated_fraction_s480` | endothelial | `divProbMP` | 0.373304039470 | 0.527931635507 | 2/0 |
| `ec_activated_fraction_s960` | endothelial | `cafDivBoost` | 0.833333333333 | 1.17851130198 | 2/0 |
| `ec_population_zero_status` | failure | `divProbEP` | 1.66666666667 | 0 | 2/0 |
| `fibroblast_fold_change_s0` | fibroblast | `activProbE` | 0 | 0 | 2/0 |
| `fibroblast_fold_change_s1440` | fibroblast | `dieProbMP` | 38.2335680751 | 8.52345850092 | 2/0 |
| `fibroblast_fold_change_s480` | fibroblast | `divProbFN` | 7.27699530516 | 1.41089381223 | 2/0 |
| `fibroblast_fold_change_s960` | fibroblast | `dieProbMP` | 19.4072769953 | 2.84668575055 | 2/0 |
| `fibroblast_log10_fold_s0` | fibroblast | `activProbE` | 0 | 0 | 2/0 |
| `fibroblast_log10_fold_s1440` | fibroblast | `dieProbMP` | 1.40544157014 | 0.871395458174 | 2/0 |
| `fibroblast_log10_fold_s480` | fibroblast | `divProbFN` | 1.18442535488 | 1.00708813529 | 2/0 |
| `fibroblast_log10_fold_s960` | fibroblast | `divProbMP` | 0.894649175065 | 1.21342767902 | 2/0 |
| `fibroblast_population_zero_status` | failure | `activProbE` | 0 | 0 | 2/0 |
| `fibroblast_total_s0` | fibroblast | `activProbE` | 0 | 0 | 2/0 |
| `fibroblast_total_s1440` | fibroblast | `dieProbMP` | 5429.16666667 | 1210.33110713 | 2/0 |
| `fibroblast_total_s480` | fibroblast | `divProbFN` | 1033.33333333 | 200.346921336 | 2/0 |
| `fibroblast_total_s960` | fibroblast | `dieProbMP` | 2755.83333333 | 404.229376578 | 2/0 |
| `invalid_denominator_status` | failure | `divProbEP` | 1.66666666667 | 0 | 2/0 |
| `jnkp_fraction_s0` | jnk | `initialJnkPositiveTenths` | 0.339743589744 | 0.244767731949 | 2/0 |
| `jnkp_fraction_s1440` | jnk | `dieProbFP` | 0.480759401336 | 0.629560536589 | 2/0 |
| `jnkp_fraction_s480` | jnk | `macrophageInteractionRadius` | 0.415331196581 | 0.394725740566 | 2/0 |
| `jnkp_fraction_s960` | jnk | `migrProbP` | 0.623912885100 | 0.689002338788 | 2/0 |
| `jnkp_rim_fraction_s0` | jnk | `activProbE` | 0 | 0 | 2/0 |
| `jnkp_rim_fraction_s1440` | jnk | `activProbE` | 0 | 0 | 2/0 |
| `jnkp_rim_fraction_s480` | jnk | `activProbE` | 0 | 0 | 2/0 |
| `jnkp_rim_fraction_s960` | jnk | `activProbE` | 0 | 0 | 2/0 |
| `macrophage_activated_fraction_s0` | macrophage | `activProbE` | 0 | 0 | 2/0 |
| `macrophage_activated_fraction_s1440` | macrophage | `divProbMP` | 0.816891243436 | 0.603855539428 | 2/0 |
| `macrophage_activated_fraction_s480` | macrophage | `dieProbMP` | 0.602296208625 | 0.282234757366 | 2/0 |
| `macrophage_activated_fraction_s960` | macrophage | `divProbMP` | 0.694881267440 | 0.583709989275 | 2/0 |
| `macrophage_population_zero_status` | failure | `dieProbEP` | 0.833333333333 | 1.17851130198 | 2/0 |
| `maximum_absolute_standardized_residual` | abc | `divProbMP` | 4.67283165393 |  | 1/1 |
| `overall_error_status` | failure | `activProbE` | 0 | 0 | 2/0 |
| `overall_finite_status` | failure | `divProbEP` | 1.66666666667 | 0 | 2/0 |
| `overall_invalid_status` | failure | `divProbEP` | 1.66666666667 | 0 | 2/0 |
| `temporal_fibroblast_tumor_correlation` | fibroblast | `dieProbMP` | 1.74501225764 | 0.549449842747 | 2/0 |
| `total_abc_distance` | abc | `divProbMP` | 5.32616038866 |  | 1/1 |
| `total_population_s0` | population | `initialMacrophageCount` | 920.833333333 | 5.89255650989 | 2/0 |
| `total_population_s1440` | population | `dieProbMP` | 2450.83333333 | 1693.52074094 | 2/0 |
| `total_population_s480` | population | `dieProbMN` | 2733.33333333 | 2802.49987610 | 2/0 |
| `total_population_s960` | population | `dieProbMN` | 3101.66666667 | 3738.23784987 | 2/0 |
| `tumor_extinction_status` | failure | `dieProbMP` | 1.66666666667 | 0 | 2/0 |
| `tumor_fold_change_s0` | tumor | `activProbE` | 0 | 0 | 2/0 |
| `tumor_fold_change_s1440` | tumor | `dieProbMP` | 60.5064102564 | 78.9511917609 | 2/0 |
| `tumor_fold_change_s480` | tumor | `initialMacrophageCount` | 18.5468750000 | 22.3254234768 | 2/0 |
| `tumor_fold_change_s960` | tumor | `dieProbMP` | 43.3846153846 | 53.1961870770 | 2/0 |
| `tumor_log10_fold_s0` | tumor | `activProbE` | 0 | 0 | 2/0 |
| `tumor_log10_fold_s1440` | tumor | `initialMacrophageCount` | 1.35529045422 |  | 1/1 |
| `tumor_log10_fold_s480` | tumor | `dieProbMP` | 1.94829846973 | 0.129720458534 | 2/0 |
| `tumor_log10_fold_s960` | tumor | `dieProbMP` | 3.18968975397 |  | 1/1 |
| `tumor_radius_s0` | tumor | `clusterRadius` | 5.73897809945 | 0.967785507609 | 2/0 |
| `tumor_radius_s1440` | tumor | `divProbFN` | 29.6019983445 | 6.65123324630 | 2/0 |
| `tumor_radius_s480` | tumor | `dieProbMP` | 30.6380015233 | 14.5660511404 | 2/0 |
| `tumor_radius_s960` | tumor | `dieProbMP` | 59.5767727400 |  | 1/1 |
| `tumor_rms_spread_s0` | tumor | `clusterRadius` | 4.45221021182 | 0.690201068304 | 2/0 |
| `tumor_rms_spread_s1440` | tumor | `divProbFN` | 18.1260997769 | 3.38541985256 | 2/0 |
| `tumor_rms_spread_s480` | tumor | `dieProbMP` | 8.77435925309 | 7.54868443399 | 2/0 |
| `tumor_rms_spread_s960` | tumor | `dieProbMP` | 32.2874954852 |  | 1/1 |
| `tumor_total_s0` | tumor | `initPop` | 17.5000000000 | 1.17851130198 | 2/0 |
| `tumor_total_s1440` | tumor | `divProbP` | 934.166666667 | 1217.40217494 | 2/0 |
| `tumor_total_s480` | tumor | `initialMacrophageCount` | 236.666666667 | 150.849446653 | 2/0 |
| `tumor_total_s960` | tumor | `divProbP` | 623.333333333 | 549.186266722 | 2/0 |

## 12. Important Currently Fixed Parameters

- Rank 1: `dieProbMP` (score 0.647189113374, status fixed).
- Rank 2: `divProbMP` (score 0.495277116820, status fixed).
- Rank 4: `stressStrength` (score 0.316949821176, status fixed).
- Rank 5: `dieProbFP` (score 0.298579532647, status fixed).
- Rank 6: `macrophageInteractionRadius` (score 0.298574861878, status hard-coded).
- Rank 7: `divProbFN` (score 0.288199850138, status fixed).
- Rank 9: `dieProbFN` (score 0.280778176123, status fixed).
- Rank 10: `endothelialMacrophageRadius` (score 0.279258638480, status hard-coded).
- Rank 11: `dieProbMN` (score 0.279010682566, status fixed).
- Rank 15: `migrProbP` (score 0.249772838135, status fixed).
- Rank 16: `initialLungCount` (score 0.248994488065, status hard-coded).
- Rank 18: `divProbEP` (score 0.244803471440, status fixed).

## 13. Parameters Associated With Extinction or Invalid States

Associations are screening statistics, not causal estimates.

- `activProbM` vs MACROPHAGE_POPULATION_ZERO: rank-biserial=0.866666666667, logistic coefficient=98.8995764571, permutation p=0.0398009950249.
- `stressStrength` vs TUMOR_EXTINCT: rank-biserial=-0.858536585366, logistic coefficient=-10.4396125404, permutation p=0.00497512437811.
- `endothelialDaughterDivisionBoost` vs MACROPHAGE_POPULATION_ZERO: rank-biserial=0.811111111111, logistic coefficient=96.2777079490, permutation p=0.0696517412935.
- `cafDivBoost` vs TUMOR_EXTINCT: rank-biserial=0.804878048780, logistic coefficient=40.0152663966, permutation p=0.00497512437811.
- `netN` vs MACROPHAGE_POPULATION_ZERO: rank-biserial=-0.800000000000, logistic coefficient=-91.0856735050, permutation p=0.0646766169154.
- `fibroblastSignalCap` vs MACROPHAGE_POPULATION_ZERO: rank-biserial=-0.788888888889, logistic coefficient=-52.0867166748, permutation p=0.0298507462687.
- `macrophageInteractionRadius` vs MACROPHAGE_POPULATION_ZERO: rank-biserial=-0.788888888889, logistic coefficient=-37.7977495792, permutation p=0.0398009950249.
- `dieProbFN` vs TUMOR_EXTINCT: rank-biserial=-0.782926829268, logistic coefficient=-7.06364657578, permutation p=0.00497512437811.
- `divProbMP` vs MACROPHAGE_POPULATION_ZERO: rank-biserial=-0.733333333333, logistic coefficient=-87.4444314233, permutation p=0.104477611940.
- `tumorEndothelialRadius` vs MACROPHAGE_POPULATION_ZERO: rank-biserial=-0.722222222222, logistic coefficient=-98.9924545098, permutation p=0.169154228856.
- `fibroblastSignalCap` vs TUMOR_EXTINCT: rank-biserial=0.709756097561, logistic coefficient=5.61427140266, permutation p=0.00497512437811.
- `dieProbL` vs MACROPHAGE_POPULATION_ZERO: rank-biserial=0.700000000000, logistic coefficient=36.7626608353, permutation p=0.0995024875622.
- `dieProbP` vs TUMOR_EXTINCT: rank-biserial=0.695121951220, logistic coefficient=3.35158473195, permutation p=0.00995024875622.
- `divProbP` vs MACROPHAGE_POPULATION_ZERO: rank-biserial=-0.677777777778, logistic coefficient=-36.6212120701, permutation p=0.199004975124.
- `activProbE` vs TUMOR_EXTINCT: rank-biserial=0.670731707317, logistic coefficient=39.1510652962, permutation p=0.00497512437811.

## 14. Nonlinear/Interaction Candidates

See class B/C rows in `PARAMETER_INFLUENCE_CLASSIFICATION.csv`. High sigma can also reflect stochastic variability; multi-seed confirmation is required before biological interpretation.

## 15. Parameters Confirmed as Low Influence

Only class D parameters with adequate valid effects and acceptable signal-to-noise qualify statistically. Structurally inactive class E fields are implementation findings, not evidence of biological unimportance. A short pilot cannot confirm low influence.

## 16. One-Seed Versus Multi-Seed Comparison

Targeted three-seed confirmation is complete for 15 selected parameters across 126 outputs. The median output-specific Spearman rank correlation is 0.828571428571. `morris_confirmed_rankings.csv` reports every restricted one-seed/multi-seed rank, rank change, approximate 95% mu-star interval, SNR, valid count, and lost count. With only two trajectories in the smoke run, these comparisons validate stochastic handling but do not establish stable scientific ranks.

## 17. Recommended Parameters for Future Calibration

Current overall candidates (provisional when trajectories <10): `dieProbMP`, `divProbMP`, `divProbFP`, `stressStrength`, `dieProbFP`, `macrophageInteractionRadius`, `divProbFN`, `divProbP`, `dieProbFN`, `endothelialMacrophageRadius`. Use per-output top tiers after the 20-trajectory screen and three-seed confirmation. Favor parameters influencing measured tumour, JNK, fibroblast, macrophage, and EC outputs with adequate SNR; retain failure-associated parameters even if continuous-output mu-star is moderate. Sensitivity alone does not establish identifiability.

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
