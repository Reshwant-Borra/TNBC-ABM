# STEP 4 — Range and Failure Review (pre-primary)

**Question:** before the 20-trajectory primary run, does any parameter *range* cause overwhelming
invalidity that would justify changing a bound? **Decision: NO bound changes.** Every finding below is
documented; nothing is silently modified, and no range is auto-narrowed.

## 1. Evidence that no single range is pathological

| Check | Result | Interpretation |
|---|---|---|
| Min per-parameter valid-EE fraction (biological outputs) | **0.847** (dieProbMP) | Even the worst-covered parameter keeps ~85% of its elementary effects |
| Parameters with valid fraction < 0.70 | **0** | No range collapses coverage |
| Parameters with valid fraction < 0.50 | **0** | No range produces majority invalidity attributable to that parameter |
| Failing points by the step's `changed_parameter` | ~8–9 each, near-uniform across all 45 | Failures come from the **joint** position in the wide 45-D box, not one pathological range |
| Screened probability bounds | all within [0,1] (e.g. divProbMP [0.001, 0.03], migrProbM [0.1, 0.85]) | No probability/validity constraint is violated |

Because invalidity is distributed near-uniformly over which parameter was last perturbed, the high global
failure rate (62.8% overall-invalid) is a property of screening a wide 45-dimensional space with OAT
trajectories — **not** of any individual parameter's range.

## 2. Provenance of the "INVALID" outputs

From `morris_outputs.csv` invalid-reason tallies, invalidity is overwhelmingly **downstream of legitimate
biological population-loss**, plus the composite ABC target requiring all endpoints finite:

| Invalid reason | ~count | Nature |
|---|---:|---|
| TARGET_STAT_UNDEFINED / NONFINITE_OR_UNDEFINED | ~3070 | Standardized-residual / ABC-distance undefined when a target denominator is 0 |
| ONE_OR_MORE_TARGETS_UNDEFINED | 578 | Composite ABC distance needs *all* targets finite |
| TUMOR_DENOMINATOR_ZERO / TUMOR_LOG_UNDEFINED | 566 | Tumour extinct → fold/log undefined (**real extinction**) |
| EC_POPULATION_ZERO | 272 | EC pool zeroed → activation-fraction denominator 0 (**real EC loss**) |
| FIBROBLAST_LOG_UNDEFINED / MACROPHAGE_POPULATION_ZERO | 44 | Fibroblast/macrophage loss (**real, rare**) |

None of these are numerical defects: each is a defined derived quantity becoming undefined because its
biological denominator legitimately reached zero, or the composite distance requiring every target at once.

## 3. Per-parameter failure associations (documented, no action)

Full table in `RANGE_REVIEW_FAILURE_ASSOCIATIONS.csv`. Direction: sign of rank-biserial (positive =
higher parameter value → more likely event). All permutation p-values ≤ 0.025.

| Parameter | Baseline | Lower | Upper | Failure mode | rank-biserial | Direction | Invalidity type |
|---|---:|---:|---:|---|---:|---|---|
| dieProbEP | 0.008 | 0.001 | 0.020 | EC_POPULATION_ZERO | +0.50 | higher → more EC loss | biologically-possible population loss |
| dieProbEN | 0.005 | 0.001 | 0.010 | EC_POPULATION_ZERO | +0.47 | higher → more EC loss | biologically-possible population loss |
| stressStrength | 1.5 | 0 | 3 | FIBROBLAST_POPULATION_ZERO | −0.89 | higher → less fibroblast loss | biologically-possible population loss |
| dieProbFN | 0.008 | 0.001 | 0.008 | FIBROBLAST_POPULATION_ZERO | +0.86 | higher → more fibroblast loss | biologically-possible population loss |
| dieProbMP | 0.015 | 0.001 | 0.030 | MACROPHAGE_POPULATION_ZERO | +0.69 | higher → more macrophage loss | biologically-possible population loss |
| divProbMP | 0.01575 | 0.001 | 0.030 | MACROPHAGE_POPULATION_ZERO | −0.51 | higher → less macrophage loss | biologically-possible population loss |
| dieProbP | 0.00306 | 0.001 | 0.004 | TUMOR_EXTINCT | +0.38 | higher → more extinction | biologically-possible extinction |
| cafDivBoost | 0.4276 | 0 | 1 | TUMOR_EXTINCT | +0.38 | higher → more extinction | biologically-possible extinction |
| clusterRadius | 4 | 2 | 8 | TUMOR_EXTINCT | −0.39 | larger → less extinction | biologically-possible extinction (establishment area) |
| dieProbEN | 0.005 | 0.001 | 0.010 | GENERAL_INVALID | +0.37 | higher → more invalid | downstream denominator (tracks EC loss) |

## 4. Classification of invalidity type for each dominant failure mode

- **Tumour extinction (34%)** → *biologically-possible extinction*. Death-rate and establishment-area
  parameters move it in the expected directions. Legitimate model outcome; derived tumour fold/log are
  correctly left undefined.
- **EC population loss (42%)** → *biologically-possible population loss*. EC inactive division is blocked
  in the model (registry: dieProbEN "uncompensated loss"), so high EC death rates deplete ECs; the
  activation-fraction denominator then legitimately goes to zero.
- **Macrophage / fibroblast loss (3–3.5%)** → *biologically-possible population loss*, rare, with strong
  turnover-balance associations.
- **General invalid** → *downstream denominator + composite-target problem*, entirely explained by the
  three biological losses above and by `total_abc_distance` requiring all targets finite simultaneously.
- **Code defect?** → No evidence. Zero model exceptions across 460 runs (QC PASS); associations are
  biologically coherent; no probability bound is violated.

## 5. Decision and justification

**No bound is changed.** The two allowable triggers are not met:

1. *Violation of a probability/validity constraint* — none: all screened probability ranges lie in [0,1],
   migration/probability ladders were explicitly constrained at registry-build time (e.g. migrProbM capped
   at 0.85), and QC confirms all physical design values are within registry bounds.
2. *Documentation specifying a corrected bound* — none in the repository.

Per the execution rules, ranges are **left unchanged** and the failure structure is documented here.
Reducing the EC-death or tumour-death ranges to suppress extinction would bias the screen by hiding a real
biological failure boundary; that is explicitly disallowed. The one item flagged for **biological review**
(not a bound change) is the registry's own note that **dieProbFN's upper bound implies an implausibly short
resting-fibroblast half-life** and that **inactive-EC division is blocked, making dieProbEN an uncompensated
loss** — both are pre-existing registry findings for mentor review, carried forward to the final report's
"range review" section, not altered here.

The primary 20-trajectory screen therefore proceeds on the **unchanged** registry ranges.
