# 10-Trajectory Morris Pilot — Analysis Report

**Stage:** Pilot (screening, not calibration). **Model:** untreated TNBC lung-metastasis ABM.
**Generated for:** STEP 2–3 of the Morris study. All findings here are **pilot evidence**, superseded by
the 20-trajectory primary screen and matched-seed confirmation. Morris indices are **not** Sobol indices.

## 1. Design and QC

| Item | Value |
|---|---|
| Trajectories | 10 |
| Levels (p) | 6, delta = 0.6 |
| Screened parameters (k) | 45 |
| Points per trajectory | k+1 = 46 |
| **Design points / run records** | **460 / 460** |
| Replicates | 1 (single seed set per trajectory, CRN within pairs) |
| Master seed | 9001 |
| Physical duplicate points | 0 |
| Valid replicate-mean EEs | 51 742 |
| Lost replicate-mean EEs | 5 408 |
| **QC** | **PASS (11/11)** — baseline bit-identical, deterministic rerun, thread independence, registry/bounds, one-parameter-per-step, CRN seed match, no dup/drop, consistent CSV widths |

Command (macOS classpath, threads reduced 12→8 for host headroom; thread count does not affect results):
```bash
java -Xmx8g -cp ".:HAL-freq.jar:lwjgl.jar" OnLatticeExample.MorrisSensitivitySweep \
  --trajectories 10 --levels 6 --threads 8 --master-seed 9001 --replicates 1 --init-pop 25 \
  --output-dir results/morris-pilot-10
```

## 2. Run-outcome inventory

| Status | Count | Fraction |
|---|---:|---:|
| FINITE (strict) | 171 | 37.2% |
| EXTINCT (tumour) | 158 | 34.3% |
| INVALID (some derived output undefined) | 131 | 28.5% |

Failure-flag event rates (fraction of the 460 runs):

| Failure flag | Event rate |
|---|---:|
| EC population loss | **42.4%** |
| Tumour extinction | **34.3%** |
| Overall-invalid (≥1 undefined derived output) | 62.8% |
| Macrophage population loss | 3.0% |
| Fibroblast population loss | 3.5% |

These high rates are the **expected** behaviour of a wide 45-dimensional global screen: OAT trajectories
visit extreme corners where the tumour extinguishes or the EC pool zeroes out. Count-scale outputs
(populations, cumulative events, radii) remain FINITE and analysable even when ratio/fold outputs are
undefined — which is why the pipeline keeps every output separate with explicit valid/lost accounting.
See `RANGE_AND_FAILURE_REVIEW.md` for the invalidity provenance (no bound change recommended).

## 3. Deliverable tables

- `PILOT_TOP_PARAMETERS_BY_OUTPUT.csv` — top-10 parameters by mu\* for each of 127 outputs, with signed mu,
  sigma, sigma/mu\*, valid/lost EE counts and fractions, normalized mu\*, and an effect-character label.
- `PILOT_FAILURE_DRIVERS.csv` — ranked drivers per failure/extinction/invalid outcome (rank-biserial,
  logistic coef, permutation p, direction).
- `PILOT_PARAMETER_CLASSIFICATION.csv` — one row per screened parameter: family scores, rank, led-output
  fraction, sigma/mu\* interpretation, valid fraction, strongest failure association, and a rank-tertile tier.
- Figures in `figures/` (mu\*–sigma, top-mu\* bars, normalized-mu\* heatmap, failure heatmap, EE
  distributions, parameter–output scatters, runtime/validity).

## 4. Overall biological-priority ranking (top 15 of 45)

| Rank | Parameter | Status | Priority score |
|---:|---|---|---:|
| 1 | dieProbMP | fixed | 0.572 |
| 2 | divProbMP | fixed | 0.524 |
| 3 | stressStrength | fixed | 0.456 |
| 4 | divProbFN | fixed | 0.422 |
| 5 | divProbMN | fixed | 0.420 |
| 6 | lambdaStress | fixed | 0.420 |
| 7 | dieProbFN | fixed | 0.379 |
| 8 | dieProbMN | fixed | 0.370 |
| 9 | dieProbL | fixed | 0.364 |
| 10 | dieProbEN | fixed | 0.349 |
| 11 | migrProbP | fixed | 0.342 |
| 12 | initialLungCount | hard-coded | 0.339 |
| 13 | pOffMax | **ABC-inferred** | 0.324 |
| 14 | divProbFP | **ABC-inferred** | 0.312 |
| 15 | clusterRadius | fixed | 0.297 |

**Headline:** the top-12 biological-priority parameters are all currently **fixed / hard-coded**. The
macrophage turnover rates (dieProbMP, divProbMP, dieProbMN, divProbMN), the fibroblast turnover rates
(divProbFN, dieProbFN), and the lung-stress field (stressStrength, lambdaStress, dieProbL) dominate.
The 12 currently ABC-inferred parameters rank 13–43 — the current calibration set is not aligned with the
model's dominant sensitivities. This is the central finding motivating the fixed-vs-inferred review.

## 5. Per-family endpoint drivers (top 3 by mu\* at s1440)

| Family / endpoint output | 1st | 2nd | 3rd |
|---|---|---|---|
| Tumour `tumor_log10_fold_s1440` | dieProbMP (fx) | divProbP (inf) | divProbMP (fx) |
| JNK `jnkp_fraction_s1440` | dieProbL (fx) | dieProbMP (fx) | pOffMax (inf) |
| Fibroblast `fibroblast_log10_fold_s1440` | divProbFN (fx) | dieProbFN (fx) | dieProbFP (fx) |
| Macrophage `macrophage_activated_fraction_s1440` | divProbMP (fx) | dieProbMN (fx) | divProbMN (fx) |
| Endothelial `ec_activated_fraction_s1440` | divProbMP (fx) | deactProbE (fx) | activProbF (inf) |
| ABC `total_abc_distance` | divProbMN (fx) | migrProbM (fx) | dieProbN (inf) |

Note `total_abc_distance` has poor coverage (many single-EE estimates) because it requires **all** ABC
targets finite simultaneously; it is one diagnostic output, not the ranking basis.

## 6. Provisional smoke-driver check (do NOT call confirmed)

| Smoke driver | Smoke rank | Pilot rank | Verdict |
|---|---:|---:|---|
| dieProbMP | 1 | **1** | holds — top driver |
| divProbMP | 2 | **2** | holds |
| stressStrength | 4 | **3** | holds |
| dieProbFP | 5 | **25** | **dropped** — smoke artefact |
| macrophageInteractionRadius | 6 | **19** | **dropped** — smoke artefact |

Three of five provisional drivers survive to the 10-trajectory pilot; two were 2-trajectory artefacts.
This is exactly the instability the smoke report warned about and justifies the pilot→primary escalation.

## 7. Failure and extinction drivers (pilot)

- **Tumour extinction** (34%): ↑ with higher dieProbP (+0.38), cafDivBoost (+0.38), divProbMN (+0.34);
  ↓ with larger clusterRadius (−0.39) and fibroblastTumorRadius (−0.39). Biologically coherent (more
  tumour death / less establishment area → extinction).
- **EC population loss** (42%): ↑ with higher dieProbEP (+0.50), dieProbEN (+0.47); ↓ with higher
  activProbE (−0.27). Direct EC death rates; activation rescues ECs via division.
- **Macrophage loss** (3%): ↑ dieProbMP (+0.69); ↓ divProbMP (−0.51), activProbF (−0.59). Turnover balance.
- **Fibroblast loss** (3.5%): ↓ stressStrength (−0.89), fibroblastTumorRadius (−0.80), fibroblastSignalCap
  (−0.79); ↑ dieProbFN (+0.86), dieProbEN (+0.80), migrProbP (+0.78). Stress/tumour signalling sustains CAFs.
- **General invalid**: dieProbEN (+0.37) and cafDivBoost (+0.28) lead — invalidity tracks EC loss and tumour
  extinction, i.e. it is downstream of biological population-loss, not a numerical defect.

## 8. Methodological notes (carried into primary)

1. **SNR is undefined at 1 replicate.** `mean_signal_to_noise_ratio` needs within-point variance across
   replicates; with `--replicates 1` it is NaN for every output. Signal-to-noise is only assessable in the
   3-seed confirmation. Do not read the pilot/primary SNR columns as reproducibility.
2. **sigma conflates interaction, nonlinearity, discretisation, and stochasticity.** All 45 parameters show
   sigma/mu\* > 1 on their led outputs. This is *not* proof of biological interaction — a single replicate
   folds stochastic variance into sigma. Confirmation is required before interpreting high sigma.
3. **Per-output separation matters.** Aggregate composites (total_abc_distance, maximum residual) lose
   coverage under wide ranges; the family-resolved rankings are the trustworthy basis.

## 9. Classification tiers (rank tertiles over 45 screened)

15 HIGH_INFLUENCE (rank 1–15), 15 MODERATE_INFLUENCE (16–30), 15 LOW_INFLUENCE_WITHIN_RANGE (31–45).
"Low within range" is a **relative** screening statement over the chosen bounds, **not** evidence that a
mechanism is biologically unnecessary. No mechanism is deleted.

## 10. Status

Pilot complete and QC-clean. Rankings are pilot-grade and are compared against the 20-trajectory primary in
`../morris-primary-20/RANK_STABILITY_REPORT.md`. **The model is not calibrated.**
