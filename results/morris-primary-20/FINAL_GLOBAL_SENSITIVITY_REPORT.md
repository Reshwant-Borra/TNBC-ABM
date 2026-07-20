# Final Global Morris Sensitivity Report — Untreated TNBC Lung-Metastasis ABM

**Scope:** global Morris elementary-effects screening (10-trajectory pilot → 20-trajectory primary →
3-seed matched confirmation). **This is screening, not calibration.** Morris mu\* is statistical
sensitivity over the chosen ranges; it is **not** a Sobol index, **not** identifiability, and **not** a
statement of biological importance. **The model is not calibrated.** No ABC/SNPE and no surrogate training
were run.

---

## 1. Executive summary

Across 45 screened parameters and 3,093 new simulations, the untreated model's behaviour is dominated by
currently **fixed** parameters — the myeloid (macrophage) and fibroblast turnover rates and the lung-stress
field — not by the 12 parameters the ABC currently infers. The primary 20-trajectory ranking is stable
(overall Spearman vs pilot = 0.844; top-5 overlap 5/5) and reproducible under matched seeds (median
one-vs-three-seed Spearman = 0.903). Of the 12 currently ABC-inferred parameters, **only 2 (divProbP,
pOffMax) are top-tier influential; 6 are bottom-tier / weakly identifiable.** The dominant biological
failure modes are EC population loss (42%) and tumour extinction (34%), both driven by the same fixed death
rates. **Recommendation headline:** review the fixed top drivers for biological support before calibration,
retain 4 inferred parameters as calibration targets, and stop inferring 6 low-influence inferred parameters
(fix or gather data). No parameter range required changing.

## 2. Scientific motivation

Rejection ABC over 12 tumour-centric parameters produced flat posteriors and poor target fit (repo history).
Before any renewed calibration, we must know *which* uncertain biological rates, interaction strengths,
spatial constants, and initialisation counts actually move each measured output over its documented range —
and which currently-fixed quantities silently govern the model. Morris screening answers this cheaply and
per-output, separating influential from non-influential parameters and flagging failure/extinction drivers,
so the calibration prior can be re-scoped on evidence rather than habit.

## 3. Software and QC verification

- Recompiled on macOS (`javac -cp ".:HAL-freq.jar:lwjgl.jar" …`), classes fresh.
- QC gate (`MorrisQualityControl 9001 25 1440`): **PASS** — baseline legacy-vs-named bit-identical,
  deterministic rerun, two-thread fresh-grid independence, registry/transform/bounds checks.
- Pilot and primary in-run QC: **PASS 11/11** each (unique names; finite transforms; design values within
  bounds; exactly one parameter per step; CRN seed match across pairs; no dup/drop; deterministic fresh-grid
  rerun; legacy/named bit-identical; per-run fresh `ExampleGrid`+`Rand`; consistent CSV widths).
- 0 model exceptions across all runs. Threads reduced 12→8 for host headroom; thread count does not affect
  results (seeds depend only on master seed, trajectory, replicate — QC-verified).

## 4. 10-trajectory pilot design and results

10 trajectories × (45+1) = **460 points / 460 runs**, seed 9001, 1 replicate, delta 0.6, p=6. Outcomes:
171 FINITE / 158 EXTINCT / 131 INVALID. Overall top drivers: dieProbMP, divProbMP, stressStrength,
divProbFN, divProbMN. Three of five provisional smoke drivers survived (dieProbMP/divProbMP/stressStrength
at ranks 1/2/3); dieProbFP (→25) and macrophageInteractionRadius (→19) were 2-trajectory artefacts. Full
detail in `../morris-pilot-10/PILOT_REPORT.md`.

## 5. 20-trajectory primary design and results

20 × 46 = **920 points / 920 runs**, same seed/config. Outcomes: 357 FINITE / 323 INVALID / 240 EXTINCT.
Overall top-10 by biological-priority score:

| Rank | Parameter | Status | | Rank | Parameter | Status |
|--:|---|---|---|--:|---|---|
| 1 | divProbMP | fixed | | 6 | dieProbMN | fixed |
| 2 | dieProbMP | fixed | | 7 | divProbP | **ABC-inferred** |
| 3 | divProbFN | fixed | | 8 | pOffMax | **ABC-inferred** |
| 4 | stressStrength | fixed | | 9 | dieProbFN | fixed |
| 5 | divProbMN | fixed | | 10 | migrProbP | fixed |

Eight of the top-10 are currently fixed. Detail in `PRIMARY_*` tables and
`GLOBAL_MORRIS_SENSITIVITY_REPORT.md` (Java) in this directory.

## 6. Ranking stability (10 vs 20 trajectories)

Judged on multiple axes (`RANK_STABILITY_REPORT.md`):

- Overall Spearman (priority score): **0.844**; top-5/10/15 overlap **1.00 / 0.70 / 0.87**.
- Per-family Spearman: tumour 0.79, JNK 0.81, fibroblast 0.84, macrophage 0.86, endothelial 0.80.
- Median |rank change| 5, max 18. Largest movers dieProbP (39→21) and pOnMax (41→25), both ABC-inferred.
- **Failure-driver ranks are noisier** (Spearman 0.14–0.65): the rare macrophage/fibroblast-loss modes (3%)
  give unstable driver orderings; the common EC-loss/extinction modes are more stable.

Conclusion: the continuous-output biological ranking has converged at the top; the middle ranks and the
rare-failure driver ranks have not, and are treated cautiously below.

## 7. Matched-seed confirmation (3 seeds, 18 parameters)

571 points × 3 replicates = **1,713 simulations** (`CONFIRMATION_STABILITY_REPORT.md`,
`morris_confirmed_rankings.csv`, `CONFIRMED_PARAMETER_EFFECTS.csv`):

- Median per-output one-vs-three-seed Spearman **0.903** (Q1 0.81, Q3 0.998); 45.7% of restricted ranks
  unchanged; max |rank change| 13. **Stochasticity is not driving the ordering.**
- Median mu\* ratio (3-seed/1-seed) **0.834** and sigma ratio 0.835 → single-replicate mu\*/sigma were
  modestly inflated by stochastic variance, now averaged out; magnitudes are otherwise stable.
- Median three-seed SNR **1.12**; SNR ≥ 1 for 56% of rows. This is the **first stage where SNR is defined**
  (single-replicate screens give NaN SNR), so it is the reproducibility basis for the decision table.

## 8. Top drivers by biological output (primary, s1440 endpoints)

| Family | 1st | 2nd | 3rd |
|---|---|---|---|
| Tumour (log10 fold) | divProbP (inf) | dieProbMP (fx) | divProbMP (fx) |
| JNK (JNK+ fraction) | pOffMax (inf) | stressStrength (fx) | divProbMN (fx) |
| Fibroblast (log10 fold) | divProbFN (fx) | dieProbFN (fx) | dieProbFP (fx) |
| Macrophage (activated frac) | divProbMN (fx) | dieProbMN (fx) | dieProbMP (fx) |
| Endothelial (activated frac) | divProbMP (fx) | dieProbEN (fx) | dieProbEP (fx) |

Only the tumour and JNK endpoints are led by an inferred parameter (divProbP, pOffMax); macrophage,
fibroblast and endothelial endpoints are led entirely by fixed turnover/death rates.

## 9. Influential currently-fixed parameters (top tertile, all fixed/hard-coded)

divProbMP, dieProbMP, divProbFN, stressStrength, divProbMN, dieProbMN, dieProbFN, migrProbP, dieProbEN,
clusterRadius, lambdaStress, dieProbL, dieProbEP. **These are the highest-value review targets.** A fixed
but highly sensitive parameter is not automatically a calibration target — each needs literature/mentor
review of its value and range first (recommendation REVIEW_FIXED_VALUE). Several are confirmed reproducible
(SNR: dieProbMP 3.21, divProbMP 3.31, stressStrength 1.64, divProbMN 1.57).

## 10. Influential currently-inferred parameters

Only **divProbP** (rank 7, confirmed SNR 1.46) and **pOffMax** (rank 8, SNR 1.29) of the 12 ABC-inferred
parameters reach the top tertile → **CALIBRATE**. divProbFP (rank 20) and dieProbN (rank 30) are middle-tier
inferred and retained as calibration targets. The remaining 6 inferred parameters are low-influence (§13).

## 11. Failure and extinction drivers (primary; screening associations, not causal)

Event rates: EC loss **42%**, tumour extinction **34%**, general-invalid 63% (downstream of the biological
losses + composite ABC target), macrophage loss 3%, fibroblast loss 3.5%.

| Failure mode | Top drivers (rank-biserial) |
|---|---|
| Tumour extinction | initPop (−0.38), dieProbMP (−0.36), divProbMP (+0.36) |
| EC population loss | dieProbEN (+0.59), dieProbEP (+0.42), dieProbL (+0.30) |
| Macrophage loss | dieProbMN (+0.54), divProbFN (−0.45), divProbMP (−0.40) |
| Fibroblast loss | dieProbFN (+0.72), dieProbL (+0.68), divProbFN (−0.62) |
| General invalid | dieProbEN (+0.48), migrProbP (+0.30), clusterRadius (−0.23) |

**initPop** is the one parameter whose primary role in the screen is boundary/establishment (small seed →
extinction) rather than continuous influence → flagged **FAILURE_DRIVER**. All associations are moderate
and (for the rare modes) unstable pilot-to-primary; treat as screening signals, not causal estimates. Full
detail: `PRIMARY_FAILURE_DRIVERS.csv`, `../morris-pilot-10/RANGE_AND_FAILURE_REVIEW.md`.

## 12. Parameters with high interaction / nonlinearity signatures

At one replicate, sigma/mu\* > 1 for essentially all parameters — this conflates interaction, nonlinearity,
discrete mapping, and stochasticity. Confirmation resolves it: parameters with high sigma **and** high
three-seed SNR carry *reproducible* interaction/nonlinearity (not noise): **divProbMP (SNR 3.31), dieProbMP
(3.21), stressStrength (1.64), migrProbP (1.58), divProbMN (1.57)**. Parameters with high sigma but SNR < 1
(ecSurvival 0.56, dieProbEN 0.75, initialJnkPositiveTenths 0.79, initPop 0.86, dieProbFP 0.91, activProbM
0.92) have magnitudes partly driven by stochasticity and must be interpreted cautiously.

## 13. Low-influence parameters (bottom tertile — within the chosen ranges)

Fixed: endothelialMacrophageRadius, lambdaCAF, macrophageEndothelialBiasRadius, tumorEndothelialRadius,
fibroblastTumorRadius, deactProbE, macrophageDaughterActivationBoost, endothelialDaughterDivisionBoost,
fibroblastSignalCap (→ LOW_INFLUENCE_WITHIN_RANGE, keep fixed). Inferred: netN, activProbF, activProbE,
cafDivBoost, activProbM, ecSurvival (→ NEEDS_ADDITIONAL_DATA or FIX_AT_SUPPORTED_VALUE). **Low Morris
influence over these ranges is not evidence a mechanism is biologically unnecessary; no mechanism is
deleted.**

## 14. Structurally inactive parameters

migrProbF, activProbMP, divProbEN, migrProbE, divProbL, unusedNeighborCountRadius (registry status
`inactive`) — declared fields with no executable branch under current event logic (e.g. fibroblasts/ECs
have no migration branch; inactive ECs do not divide). Retained for auditability; **not** calibration
targets and **not** to be deleted here. Additional treatment-only, grid/domain, coordinate, observation-time
and numerical-guard quantities remain fixed by design (registry `enter_morris_screen=false`) and are out of
scope for this untreated screen.

## 15. Parameters requiring range review (biological, not numerical)

No bound was changed (§ range review). Flagged for biological/mentor review (documented, not altered):
- **dieProbFN** upper bound implies an implausibly short resting-fibroblast half-life (registry note); it is
  also the top fibroblast-loss driver — review the biological range.
- **dieProbEN**: inactive-EC division is blocked, so EC death is uncompensated; it is the top EC-loss and
  general-invalid driver — review whether the range/mechanism is biologically intended.
- Interaction radii and hidden density boosts (macrophageInteractionRadius, the *DaughterBoost / *Radius
  hard-coded fields) and initial background counts (initialMacrophageCount, initialLungCount,
  initialJnkPositiveTenths) — all "assumption requiring mentor approval" in the registry.

## 16. Recommended parameter set for future calibration

**Tier A — calibrate (already inferred, top/mid influence, reproducible):** divProbP, pOffMax, divProbFP,
dieProbN. **Tier B — review then likely add (fixed but top-tier influential):** divProbMP, dieProbMP,
divProbFN, stressStrength, divProbMN, dieProbMN, dieProbFN, migrProbP, dieProbEN, clusterRadius, lambdaStress,
dieProbL, dieProbEP — each pending literature/mentor review of value and range (sensitivity ≠ identifiability).
The calibration prior should be re-scoped from the current 12 to a reviewed set emphasising Tier A + approved
Tier B, with failure-associated parameters retained even at moderate continuous mu\*.

## 17. Parameters that should stay fixed

The 9 low-influence fixed parameters (§13), plus all structural/domain/treatment/observation/numerical
quantities. Keeping them fixed for this untreated calibration is justified by low relative Morris influence
over their ranges **and** by their being spatial/observation/treatment choices, not biological uncertainties.

## 18. Parameters requiring additional biological data

NEEDS_ADDITIONAL_DATA: netN, activProbF, activProbE, cafDivBoost — biologically essential inferred rates
with bottom-tertile influence and no matched-seed confirmation; screening cannot resolve them.
FIX_AT_SUPPORTED_VALUE: ecSurvival (confirmed SNR 0.56) and activProbM (0.92) — demonstrably weakly
identifiable (signal below stochastic noise), so fix at a literature-supported value rather than infer.
(netN is coupled to dieProbN via divProbN = dieProbN + netN; treat the pair jointly.)

## 19. Limitations

Morris is screening, not variance decomposition. mu\* is sensitivity over the chosen bounds, not biological
importance; sigma mixes nonlinearity, interaction, discretisation and (at 1 replicate) stochasticity —
disentangled only for the 18 confirmed parameters. Parameter uncertainty (ranges) is distinct from
stochastic simulation variance. Failure-driver rankings for the rare modes are unstable. 20 trajectories
converge the top continuous ranks but not the middle ranks or rare-failure drivers. A flat ABC posterior is
not proof of low sensitivity; a fixed parameter can be highly influential. These results justify neither
deleting mechanisms nor claiming identifiability.

## 20. Exact next step — neural-network surrogate dataset

Do **not** reuse only Morris points. Build a fresh space-filling design (Latin-hypercube or Sobol) over the
retained **uncertain** parameters (Tier A + approved Tier B), holding the fixed/low-influence set at
baseline; keep every raw and derived output plus the failure flags. Recommended initial size ≈ 3,000–5,000
LHS points × 3 matched replicates. Train separate probabilistic/heteroscedastic regressors per output plus a
**separate valid/extinct classifier**; never train on NaN as a successful value. Morris trajectory points
may be added as *supplemental* training data only. Full spec: `../../SURROGATE_DATA_PLAN.md`.

## 21. Exact next step — renewed ABC / SNPE

Only after the surrogate validates out-of-sample per output: define a reviewed reduced prior over the Tier A
(and approved Tier B) parameters, simulate a replicated training set, then run ABC-SMC or SNPE with explicit
missing/extinction handling and posterior predictive checks. Do **not** return to ordinary rejection ABC
over the current 12 parameters as the main workflow.

## 22. Calibration status

**The model is NOT calibrated.** This report screens influence over specified ranges; it does not estimate
posteriors, prove identifiability, estimate Sobol indices, or justify deleting mechanisms. Decision detail:
`FINAL_PARAMETER_DECISION_TABLE.csv`.
