# Ranking Stability: 10traj vs 20traj

Comparison of Morris overall biological-priority rankings between `results/morris-pilot-10` (10traj) and `results/morris-primary-20` (20traj). 45 screened parameters in common.

## Convergence is judged on multiple axes, not one cutoff

- **Overall Spearman** (priority score, 10traj vs 20traj): **0.844**
- **Top-5 overlap**: 1.00 (5/5)
- **Top-10 overlap**: 0.70 (7/10)
- **Top-15 overlap**: 0.87 (13/15)
- **Median |rank change|**: 5.0; **max |rank change|**: 18

## Per-family Spearman (family priority scores)

- tumor: 0.792
- jnk: 0.806
- fibroblast: 0.840
- macrophage: 0.864
- endothelial: 0.799
- failure: 0.588

## Parameters entering / leaving the top-10

- **Entered** top-10 in 20traj: divProbP (rank 7), pOffMax (rank 8), migrProbP (rank 10)
- **Left** top-10 in 20traj: lambdaStress (was rank 6), dieProbL (was rank 9), dieProbEN (was rank 10)
- Top-5 stable set: ['dieProbMP', 'divProbFN', 'divProbMN', 'divProbMP', 'stressStrength']; changed: in 10traj only [], in 20traj only []

## Largest rank movers

| parameter | status | rank 10traj | rank 20traj | change |
|---|---|---:|---:|---:|
| dieProbP | ABC-inferred | 39 | 21 | -18 |
| pOnMax | ABC-inferred | 41 | 25 | -16 |
| deactProbE | fixed | 26 | 42 | +16 |
| lambdaCAF | fixed | 44 | 32 | -12 |
| activProbF | ABC-inferred | 22 | 34 | +12 |
| activProbE | ABC-inferred | 24 | 36 | +12 |
| fibroblastTumorRadius | hard-coded | 28 | 40 | +12 |
| ecSurvival | ABC-inferred | 29 | 41 | +12 |
| recruitBias | fixed | 27 | 17 | -10 |
| migrProbN | fixed | 38 | 28 | -10 |

## Failure / extinction driver ranking stability

- **TUMOR_EXTINCT**: Spearman(rank) = 0.649 over 45 shared params; top-5 overlap = 0.20. 20traj top-5: initPop, dieProbMP, divProbMP, endothelialDaughterDivisionBoost, lambdaStress.
- **EC_POPULATION_ZERO**: Spearman(rank) = 0.279 over 45 shared params; top-5 overlap = 0.40. 20traj top-5: dieProbEN, dieProbEP, dieProbL, migrProbP, divProbEP.
- **MACROPHAGE_POPULATION_ZERO**: Spearman(rank) = 0.140 over 45 shared params; top-5 overlap = 0.40. 20traj top-5: dieProbMN, divProbFN, divProbMP, dieProbMP, endothelialMacrophageRadius.
- **GENERAL_INVALID**: Spearman(rank) = 0.493 over 45 shared params; top-5 overlap = 0.60. 20traj top-5: dieProbEN, migrProbP, clusterRadius, dieProbEP, dieProbL.

## Fixed-vs-inferred conclusion check (top-10)

- Currently-fixed parameters in 20traj top-10: ['divProbMP', 'dieProbMP', 'divProbFN', 'stressStrength', 'divProbMN', 'dieProbMN', 'dieProbFN', 'migrProbP']
- ABC-inferred parameters in 20traj top-10: ['divProbP', 'pOffMax']

