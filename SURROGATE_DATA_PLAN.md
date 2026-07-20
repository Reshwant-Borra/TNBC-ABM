# Surrogate-Model Data Plan (PLAN ONLY — do not train yet)

Prepared from the Morris global sensitivity study (10-traj pilot → 20-traj primary → 3-seed confirmation).
**No surrogate model is trained in this task.** This document specifies the dataset to generate next. The
authoritative per-parameter basis is `results/morris-primary-20/FINAL_PARAMETER_DECISION_TABLE.csv`.

> **Morris points are supplemental only.** The 460 + 920 + 1,713 existing Morris/confirmation runs may be
> *added* to the training set, but they are one-at-a-time trajectory points concentrated on axes and
> corners — **not** space-filling. A reliable surrogate requires a **separate Latin-hypercube or Sobol
> design** over the retained uncertain parameters. Do not build the surrogate from Morris points alone.

## 1. Input-parameter partition

### 1a. Varied (uncertain) inputs — the surrogate's input space
**Tier A — retained calibration targets (currently inferred, top/mid influence, reproducible):**
`divProbP, pOffMax, divProbFP, dieProbN` (+ `netN`, coupled to dieProbN via divProbN = dieProbN + netN —
vary as a pair).

**Tier B — fixed but top-tier influential; include only after biological/mentor review (REVIEW_FIXED_VALUE):**
`divProbMP, dieProbMP, divProbFN, stressStrength, divProbMN, dieProbMN, dieProbFN, migrProbP, dieProbEN,
clusterRadius, lambdaStress, dieProbL, dieProbEP`.

Recommended surrogate input dimension: **Tier A (~5) for the first surrogate**, expanding to **Tier A + approved
Tier B (up to ~18)** once ranges are reviewed. Use each parameter's registry transform (log for positive
rates, linear for zero-inclusive, integer for counts/radii) as the sampling scale.

### 1b. Held fixed at baseline (not surrogate inputs)
- 9 low-influence fixed parameters: `endothelialMacrophageRadius, lambdaCAF, macrophageEndothelialBiasRadius,
  tumorEndothelialRadius, fibroblastTumorRadius, deactProbE, macrophageDaughterActivationBoost,
  endothelialDaughterDivisionBoost, fibroblastSignalCap`.
- Weakly-identifiable inferred parameters fixed at literature-supported values: `ecSurvival, activProbM`
  (confirmed SNR < 1).
- All structural / domain / coordinate / observation-time / treatment / numerical-guard quantities
  (registry `enter_morris_screen=false`) and structurally-inactive fields.

### 1c. Requiring biological review before any inclusion
`dieProbFN` (implausible half-life note), `dieProbEN` (uncompensated EC loss), interaction radii and hidden
density boosts, initial background counts — plus `NEEDS_ADDITIONAL_DATA` inferred rates
`netN, activProbF, activProbE, cafDivBoost`. Review the value/range before adding to the input space.

## 2. Space-filling design
- **Method:** scrambled Sobol' (preferred for low-discrepancy and extensibility) or maximin Latin hypercube,
  sampled in each parameter's registry transform, then mapped to physical values and validated within bounds.
- **Initial count:** ≈ **3,000–5,000** design points for a ~5–18-D input space (scale up ~200–300 points per
  added dimension). Reserve an extensible Sobol sequence so the set can grow without re-sampling.
- **Ranges:** the unchanged registry bounds for varied parameters (this study changed none).

## 3. Stochastic replicates
- **3 matched seeds per design point** (same CRN scheme as the confirmation stage), reusing the master-seed →
  simulation-seed mapping so replicates are reproducible. This mirrors the confirmed reproducibility basis
  (median 3-seed SNR ≈ 1.1) and lets the surrogate learn the **mean** and **stochastic variance** per point.

## 4. Targets
- **Classifier target (train first):** run validity/outcome label per point — `FINITE / EXTINCT / INVALID`
  (and the specific failure flags: tumour-extinction, EC/macrophage/fibroblast population-zero). ~55–65% of a
  wide design is non-FINITE, so a **separate valid/extinct classifier is mandatory** before regression.
- **Regression targets (FINITE points only):** the per-output biological endpoints kept separate by family —
  tumour (log10 fold, radius, rms spread), JNK+ fraction (and rim), fibroblast log10 fold, macrophage
  activated fraction, EC activated fraction, and the macrophage–EC colocalisation — at each snapshot
  (s0/480/960/1440). Model the mean and a heteroscedastic variance term per output. **Never impute NaN as a
  successful value**; masked/undefined outputs are excluded from the regression loss, not zero-filled.

## 5. Splits and validation
- **Random split:** 70% train / 15% validation / 15% test at the **design-point** level (all 3 replicates of
  a point stay in the same fold — no replicate leakage across folds).
- **Held-out parameter-space validation:** additionally hold out one or more contiguous sub-regions (e.g. a
  Sobol subcube or a high-`dieProbMP` slab) to test extrapolation, not just interpolation.
- **Metrics:** per-output R²/CRPS and calibration of predictive intervals on validation; classifier
  AUC/PR and calibration; report per-family, not just pooled.

## 6. Candidate-search and real-ABM verification
- **Candidate search:** once validated out-of-sample, use the surrogate to screen large candidate pools
  (LHS/Sobol or optimisation/active-learning) cheaply for regions matching ABC targets and low failure
  probability.
- **Real-ABM verification (required):** every surrogate-selected candidate region must be re-simulated in the
  **actual ABM** with fresh matched seeds before any scientific claim; the surrogate is a filter, never the
  final evidence. Add active-learning rounds where surrogate uncertainty is high or classifier confidence is
  low, re-simulate, and refit.

## 7. Reuse of existing Morris data
Add the existing Morris/confirmation runs (with their exact named parameter columns and outputs) as
**supplemental** rows — they enrich coverage near axes/corners and the failure boundary — but the primary
training signal must come from the new space-filling design. Join by named parameter columns; keep the
`run_status`/failure flags so the classifier sees real invalid/extinct labels.

## 8. Explicit non-goals for this task
Do **not** train the surrogate, do **not** run ABC/SNPE, and do **not** treat any ranking here as a calibrated
result. This plan is the input to those separate, later stages.
