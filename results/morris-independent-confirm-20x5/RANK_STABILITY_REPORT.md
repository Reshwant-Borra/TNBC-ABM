# Ranking Stability: primary20_1seed vs independent20_5seed

Comparison of Morris overall biological-priority rankings between `results/morris-primary-20` (primary20_1seed) and `results/morris-independent-confirm-20x5` (independent20_5seed). 45 screened parameters in common.

## Convergence is judged on multiple axes, not one cutoff

- **Overall Spearman** (priority score, primary20_1seed vs independent20_5seed): **0.823**
- **Top-5 overlap**: 0.60 (3/5)
- **Top-10 overlap**: 0.90 (9/10)
- **Top-15 overlap**: 0.80 (12/15)
- **Median |rank change|**: 4.0; **max |rank change|**: 19

## Per-family Spearman (family priority scores)

- tumor: 0.886
- jnk: 0.577
- fibroblast: 0.867
- macrophage: 0.759
- endothelial: 0.643
- failure: 0.075

## Parameters entering / leaving the top-10

- **Entered** top-10 in independent20_5seed: dieProbEN (rank 9)
- **Left** top-10 in independent20_5seed: migrProbP (was rank 10)
- Top-5 stable set: ['dieProbMP', 'divProbFN', 'divProbMP']; changed: in primary20_1seed only ['divProbMN', 'stressStrength'], in independent20_5seed only ['dieProbFN', 'pOffMax']

## Largest rank movers

| parameter | status | rank primary20_1seed | rank independent20_5seed | change |
|---|---|---:|---:|---:|
| activProbF | ABC-inferred | 34 | 15 | -19 |
| initialLungCount | hard-coded | 18 | 35 | +17 |
| fibroblastSignalBoost | hard-coded | 27 | 42 | +15 |
| initPop | fixed CLI | 16 | 29 | +13 |
| initialJnkPositiveTenths | hard-coded | 29 | 16 | -13 |
| divProbFP | ABC-inferred | 20 | 32 | +12 |
| dieProbL | fixed | 14 | 26 | +12 |
| dieProbFP | fixed | 26 | 14 | -12 |
| lambdaCAF | fixed | 32 | 21 | -11 |
| divProbEP | fixed | 24 | 13 | -11 |

## Failure / extinction driver ranking stability

- **TUMOR_EXTINCT**: Spearman(rank) = 0.079 over 45 shared params; top-5 overlap = 0.00. independent20_5seed top-5: pOffMax, stressStrength, migrProbP, netN, dieProbEN.
- **EC_POPULATION_ZERO**: Spearman(rank) = 0.094 over 45 shared params; top-5 overlap = 0.60. independent20_5seed top-5: dieProbEN, migrProbP, dieProbEP, ecSurvival, deactProbE.
- **MACROPHAGE_POPULATION_ZERO**: Spearman(rank) = 0.033 over 45 shared params; top-5 overlap = 0.40. independent20_5seed top-5: dieProbMP, dieProbMN, netN, migrProbP, initialJnkPositiveTenths.
- **GENERAL_INVALID**: Spearman(rank) = 0.112 over 45 shared params; top-5 overlap = 0.40. independent20_5seed top-5: dieProbEN, lambdaStress, divProbMN, dieProbEP, migrProbN.

## Fixed-vs-inferred conclusion check (top-10)

- Currently-fixed parameters in independent20_5seed top-10: ['dieProbMP', 'divProbMP', 'divProbFN', 'dieProbFN', 'divProbMN', 'stressStrength', 'dieProbEN', 'dieProbMN']
- ABC-inferred parameters in independent20_5seed top-10: ['pOffMax', 'divProbP']

