# Confirmation Stability: one-seed vs three matched seeds

Targeted 3-seed confirmation reused the primary 20-trajectory design and re-ran only the neighbouring points needed for **18 selected parameters**. Comparison uses the pipeline's `morris_confirmed_rankings.csv` (restricted one-seed vs three-seed) joined to the primary one-seed summary.

## Rank agreement (one-seed vs three-seed)

- **Median per-output Spearman rank correlation**: 0.903 (over 126 outputs with >=2 shared parameters)
- Per-output Spearman quartiles: Q1=0.810, median=0.903, Q3=0.998
- **|rank change|** across all restricted rows: median=1.0, mean=1.48, 90th pct=4.0, max=13
- Rows with unchanged restricted rank: 45.7%

## Effect-size stability (mu*)

- **Median mu\* ratio (3-seed / 1-seed)**: 0.834 (1.0 = unchanged); IQR [0.656, 0.998]
- Fraction of rows where 3-seed mu\* within +/-25% of 1-seed: 56.5%

## Noise and reproducibility

- **Three-seed mean SNR** (mu\* vs within-point stochastic noise): median=1.12, Q1=0.77, Q3=2.15
- Rows with SNR >= 1 (signal exceeds stochastic noise): 56.2%
- **Sigma ratio (3-seed / 1-seed)**: median=0.835 (values <1 indicate 1-seed sigma partly reflected stochastic noise now averaged out)
- **Valid multi-seed pairs** per row: median=20, lost: median=0

## Per-parameter reproducibility (top by three-seed mu*)

| parameter | ABC12 | 3-seed mu* (med) | mu* ratio | 3-seed SNR (med) | |rank chg| med | CI rel |
|---|:--:|---:|---:|---:|---:|---:|
| dieProbMP |  | 0.7286 | 1.03 | 3.21 | 0.0 | 1.10 |
| stressStrength |  | 0.6059 | 0.86 | 1.64 | 1.0 | 1.29 |
| divProbMP |  | 0.5809 | 0.90 | 3.31 | 0.0 | 1.18 |
| divProbMN |  | 0.5029 | 0.88 | 1.57 | 1.0 | 1.20 |
| dieProbMN |  | 0.4922 | 0.91 | 1.48 | 1.0 | 1.19 |
| divProbP | Y | 0.4438 | 0.90 | 1.46 | 0.0 | 1.37 |
| pOffMax | Y | 0.4144 | 0.84 | 1.29 | 1.0 | 1.29 |
| migrProbP |  | 0.3787 | 0.95 | 1.58 | 1.0 | 1.19 |
| divProbFN |  | 0.3676 | 0.85 | 1.17 | 0.0 | 1.17 |
| clusterRadius |  | 0.364 | 0.67 | 1.19 | 2.0 | 1.21 |
| dieProbFN |  | 0.334 | 0.96 | 1.15 | 1.0 | 1.17 |
| dieProbEN |  | 0.3244 | 0.66 | 0.75 | 1.0 | 1.12 |
| initialMacrophageCount |  | 0.3137 | 0.84 | 1.17 | 1.0 | 1.24 |
| activProbM | Y | 0.2907 | 0.79 | 0.92 | 1.0 | 1.28 |
| dieProbFP |  | 0.2565 | 0.81 | 0.91 | 1.0 | 1.21 |
| initPop |  | 0.2531 | 0.60 | 0.86 | 1.0 | 1.18 |
| initialJnkPositiveTenths |  | 0.2222 | 0.70 | 0.79 | 1.0 | 1.41 |
| ecSurvival | Y | 0.2068 | 0.69 | 0.56 | 0.0 | 1.38 |

## Interpretation

- High per-output Spearman and small |rank change| mean the one-seed 20-trajectory ranks are reproducible under matched-seed replication: stochasticity is not driving the ordering.
- mu\* ratios near 1 confirm the single-seed magnitudes are not seed artefacts. Ratios far from 1 with low SNR flag parameters whose apparent effect was partly stochastic.
- SNR < 1 with high sigma is the signature of stochasticity/interaction rather than a stable monotone effect; treat those parameters' magnitudes cautiously.
