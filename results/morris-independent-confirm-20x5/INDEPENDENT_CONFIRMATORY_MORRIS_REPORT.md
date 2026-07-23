# Independent Confirmatory Morris Analysis

Generated: 2026-07-22

## 1. Previous Morris Work

The repository already contained a completed untreated Morris workflow:

- `results/morris-pilot-10`: 10 trajectories, 1 replicate, 460 simulations, QC PASS.
- `results/morris-primary-20`: 20 trajectories, 1 replicate, 920 simulations, QC PASS.
- `results/morris-primary-20/confirmation`: targeted multi-seed confirmation on the primary design, 571 selected trajectory points x 3 replicates = 1,713 simulations.

The prior final recommendation was `core4`: `divProbP`, `pOffMax`, `divProbFP`, and `dieProbN`.

## 2. Model Logic Protection

No biological model logic was changed for this independent confirmation. `git diff -- OnLatticeExample/ExampleGrid.java` was empty before the run, and the current `ExampleGrid.java` SHA-256 was:

`0821b7fa4ab8dd3d8e213161d70ae3933ff706bfcf76d87a6b37020a831f0039`

Recent calibration work in the dirty tree changed `ABCRejection.java`, `ModelParameters.java`, `README.md`, and added calibration profile/target/freeze/QC files. It did not modify `ExampleGrid.java`. The run used the existing model behavior, target values, ranges, transformations, horizon, coordinate files, and outputs.

## 3. Design Choice

I used a full all-active-parameter confirmation rather than a focused subset. A focused subset would be cheaper, but it could not answer whether previously weak or borderline parameters become important under a new trajectory design.

Design:

- 45 screened parameters from `ModelParameters.screenedDefinitions()`
- 20 newly generated Morris trajectories
- 6 levels, delta 0.6
- 5 matched stochastic replicates per trajectory point
- master seed `2026072201`
- design seed `-1836350308661608582`
- output directory `results/morris-independent-confirm-20x5`
- expected simulations: 20 x 46 x 5 = 4,600

This is independent from the primary run, whose master seed was `9001` and design seed was `6949820704716978156`. The new and old design CSVs had zero identical normalized rows at the same row positions.

## 4. Commands Run

Compile from source:

```bash
javac -cp .:HAL-freq.jar:lwjgl.jar OnLatticeExample/*.java
```

Existing QC:

```bash
java -Xmx3g -cp .:HAL-freq.jar:lwjgl.jar OnLatticeExample.MorrisQualityControl 9001 25 1440
java -cp .:HAL-freq.jar:lwjgl.jar OnLatticeExample.CalibrationQualityControl 9001 25 1440
```

Independent smoke and resume check:

```bash
java -cp .:HAL-freq.jar:lwjgl.jar OnLatticeExample.MorrisSensitivitySweep --trajectories 2 --levels 6 --threads 4 --master-seed 2026072201 --replicates 2 --init-pop 25 --max-steps 1440 --output-dir results/morris-independent-smoke-2x2
java -cp .:HAL-freq.jar:lwjgl.jar OnLatticeExample.MorrisSensitivitySweep --trajectories 2 --levels 6 --threads 4 --master-seed 2026072201 --replicates 2 --init-pop 25 --max-steps 1440 --output-dir results/morris-independent-smoke-2x2 --resume
```

Full run:

```bash
java -Xmx10g -cp .:HAL-freq.jar:lwjgl.jar OnLatticeExample.MorrisSensitivitySweep --trajectories 20 --levels 6 --threads 8 --master-seed 2026072201 --replicates 5 --init-pop 25 --max-steps 1440 --output-dir results/morris-independent-confirm-20x5
```

Analysis:

```bash
python3 analyze_morris.py --input-dir results/morris-independent-confirm-20x5 --output-dir results/morris-independent-confirm-20x5/figures --top-n 10
python3 morris_stage_analysis.py --input-dir results/morris-independent-confirm-20x5 --prefix INDEPENDENT_ --top-n 10
python3 morris_rank_stability.py --dir-a results/morris-primary-20 --dir-b results/morris-independent-confirm-20x5 --label-a primary20_1seed --label-b independent20_5seed --out-dir results/morris-independent-confirm-20x5
```

This host has `python3`, not `python`.

## 5. QC and Reproducibility

Pre-run QC:

- Java compilation succeeded.
- `MorrisQualityControl` passed baseline legacy/named equivalence, deterministic rerun, two-thread fresh-grid independence, registry transform/bounds checks, and snapshot shape checks.
- `CalibrationQualityControl` passed profile integrity, centralized target/distance equivalence, deterministic proposal/simulation checks, and freeze hash validation.
- Recompilation made `.class` files newer than Java sources, so stale compiled Morris classes were not used.

New Morris QC:

- `MORRIS_QC_REPORT.md`: PASS, 11/11 checks.
- Every Morris step changed exactly one normalized coordinate and exactly one physical parameter.
- Neighboring points used matched trajectory/replicate seeds.
- Every simulation used a fresh `ExampleGrid` and fresh HAL `Rand`.
- No duplicate or silently dropped records.
- Physical duplicate design points: 0.
- Generated CSV widths were consistent.

Smoke/resume:

- Smoke design used the same independent master seed and generated a design different from the primary design.
- Resume check reused 184/184 checkpoints and ran 0 new simulations.

Full run inventory:

- Run records: 4,600
- FINITE: 1,963
- INVALID: 1,687
- EXTINCT: 950
- Model errors: 0 by status counts
- Runtime sum across simulations: 13.245 CPU-hours
- Median runtime: 5.59 s; p90: 26.83 s; p99: 64.01 s
- Valid replicate-mean elementary effects: 108,260
- Lost replicate-mean elementary effects: 6,040

Hashes:

- `morris_design.csv`: `a7721132352723da7e4c4fd244e157083a02a6a132f69af98d6a5f51751148ca`
- `morris_raw_runs.csv`: `e3c064bda9783162e1824a40ceee542757dd890bc71af8c0b2abc533daeb30e9`

GitHub storage note:

- The local plaintext files `morris_elementary_effects.csv` (135 MB) and `morris_outputs.csv` (88 MB)
  exceed GitHub's normal file-size policy. Their compressed equivalents are committed as
  `morris_elementary_effects.csv.gz` and `morris_outputs.csv.gz`.
- Restore with:

```bash
gunzip -k results/morris-independent-confirm-20x5/morris_elementary_effects.csv.gz
gunzip -k results/morris-independent-confirm-20x5/morris_outputs.csv.gz
```

## 6. Old vs New Ranking Stability

Primary 20-trajectory one-seed vs independent 20-trajectory five-seed:

- Overall Spearman: 0.823
- Top-5 overlap: 3/5
- Top-10 overlap: 9/10
- Top-15 overlap: 12/15

Top old drivers mostly reproduced. The two macrophage activated-turnover rates remained ranks 1-2, `divProbFN`, `dieProbFN`, `divProbMN`, `dieProbMN`, `dieProbEN`, `stressStrength`, and `migrProbP` remained high. `clusterRadius`, `dieProbL`, and `dieProbEP` weakened in the independent run.

ABC-inferred ranks:

| Parameter | Pilot Rank | Primary Rank | Independent Rank | Interpretation |
|---|---:|---:|---:|---|
| `pOffMax` | 13 | 8 | 3 | strong and reproduced |
| `divProbP` | 16 | 7 | 7 | strong and reproduced |
| `activProbF` | 22 | 34 | 15 | newly important, not stable enough for immediate first-pass inference |
| `pOnMax` | 25 | 25 | 17 | moderate, JNK-specific |
| `dieProbP` | 21 | 21 | 22 | moderate, failure-associated |
| `dieProbN` | 32 | 30 | 30 | stable moderate/low, extinction-associated |
| `divProbFP` | 14 | 20 | 32 | weakened materially in the independent run |
| `activProbE` | 36 | 36 | 36 | weak |
| `ecSurvival` | 41 | 41 | 40 | weak and prior 3-seed SNR < 1 |
| `cafDivBoost` | 38 | 38 | 41 | weak |
| `activProbM` | 39 | 39 | 31 | weak/moderate but prior 3-seed SNR < 1 |
| `netN` | 33 | 33 | 28 | low/moderate and confounded with `dieProbN` |

## 7. Output-Family Findings

Tumour outputs:

- `divProbP` remained the dominant tumour log-fold driver at 480, 960, and 1440.
- `pOffMax`, `divProbMP`, `dieProbMP`, `stressStrength`, and `migrProbP` also influenced tumour burden/spread.
- `dieProbN` remained stable but was not a top continuous tumour-output driver.

JNK outputs:

- `pOffMax` became the top JNK target driver at both 480 and 1440.
- `stressStrength`, `lambdaStress`, `lambdaCAF`, and `pOnMax` remained biologically important fixed or secondary JNK drivers.
- Direction was coherent: higher `pOffMax` reduced JNK-positive fraction.

Fibroblast outputs:

- Fixed turnover parameters dominated: `divProbFN`, `dieProbFN`, and `dieProbFP`.
- `activProbF` became a top fibroblast target driver in the independent run.
- `divProbFP`, one of the prior `core4` parameters, dropped to low global influence and was not a leading fibroblast target driver in the independent run.

Macrophage outputs:

- `dieProbMP`, `divProbMN`, `divProbMP`, and `dieProbMN` dominated macrophage activation/fraction outputs.
- These are fixed Tier B parameters and remain high-priority biological-review items, not automatic calibration parameters.

Endothelial outputs:

- `dieProbMP`, `divProbMP`, `dieProbEN`, `divProbEP`, and interaction radii were important.
- `dieProbEN` remained a major EC-population-zero driver, supporting the existing concern that inactive EC death without inactive EC division may be a boundary/provenance issue.

Failure and invalid-state associations:

- `dieProbEN` remained the strongest EC-population-zero and general-invalid driver.
- `dieProbFN` remained the strongest fibroblast-population-zero driver.
- `dieProbMP` became the strongest macrophage-population-zero driver in the independent run.
- `pOffMax` became the strongest tumour-extinction driver in the independent run, while `dieProbN` remained a weaker but stable extinction-associated parameter.

The apparent effects are not caused by Java exceptions or stale compiled files. They are mostly real model boundary behavior: extinction, stromal compartment loss, and invalid denominators under broad biological ranges.

## 8. Calibration Recommendation

The previous `core4` is only partly supported. `divProbP` and `pOffMax` are strongly confirmed. `dieProbN` remains stable but modest and should be treated as an extinction/boundary control with `netN` fixed. `divProbFP` is no longer strongly supported for immediate calibration because its influence weakened materially in the independent five-seed design.

Recommended immediate calibration set:

- `divProbP`
- `pOffMax`
- `dieProbN`

Do not run the existing `core4` profile unchanged without an explicit decision to keep `divProbFP` for continuity. If a fibroblast degree of freedom must be included in the first ABC profile, the new evidence favors `activProbF` over `divProbFP`, but the old/new rank movement means this should be a mentor-reviewed addition rather than an automatic change.

## 9. Category Summary

Recommended for calibration now:

- `divProbP`: robust tumour burden driver; primary rank 7, independent rank 7, prior confirmation SNR 1.46.
- `pOffMax`: robust JNK and tumour driver; primary rank 8, independent rank 3, prior confirmation SNR 1.29.
- `dieProbN`: stable moderate/extinction-associated ABC-inferred death rate; primary rank 30, independent rank 30. Keep `netN` fixed unless a joint net-growth parameterization is reviewed.

Influential but should remain fixed pending biological review:

- `dieProbMP`, `divProbMP`, `divProbMN`, `dieProbMN`, `divProbFN`, `dieProbFN`, `dieProbEN`, `stressStrength`, `migrProbP`, `lambdaStress`, `divProbEP`, `dieProbFP`, `dieProbEP`, `clusterRadius`.

Potential later expanded calibration:

- `activProbF`: independent fibroblast-target signal, but old/new ranks are unstable.
- `divProbFP`: prior core4 member, but weakened in the independent run.
- `pOnMax`, `dieProbP`, `netN`: biologically meaningful but confounded or secondary.
- `lambdaCAF`, `migrProbM`, interaction radii and hidden boosts only after mentor-approved ranges.

Negligible or weak over tested range:

- `ecSurvival`, `cafDivBoost`, `activProbE`, `activProbM`, `deactProbE`, `macrophageDaughterActivationBoost`, `endothelialDaughterDivisionBoost`, `fibroblastSignalCap`, `fibroblastTumorRadius`, `tumorEndothelialRadius`, and other lower-third fixed quantities, subject to target/range caveats.

Inactive in current implementation:

- `migrProbF`, `activProbMP`, `divProbEN`, `migrProbE`, `divProbL`, `unusedNeighborCountRadius`.

Uncertain because of stochasticity, range assumptions, or insufficient evidence:

- Fibroblast axis: `activProbF` vs `divProbFP` vs fixed `divProbFN/dieProbFN/dieProbFP`.
- EC axis: `dieProbEN`, `divProbEP`, `dieProbEP`, and EC activation target interpretation.
- Macrophage axis: `divProbMP/dieProbMP/divProbMN/dieProbMN` and whether turnover constraints should be enforced.
- Extinction behavior: `pOffMax`, `dieProbN`, `netN`, `stressStrength`, and migration interactions.

## 10. Limitations and Mentor Decisions

Morris remains a screening method. The five-seed independent run improves reproducibility evidence, but it does not prove identifiability or estimate posterior uncertainty.

Required mentor decisions remain:

- Whether fixed macrophage turnover parameters should remain fixed or be constrained/inferred.
- Whether resting fibroblast division/death and activated fibroblast death ranges are biologically plausible.
- Whether inactive EC death without inactive EC division is intended.
- Whether `activProbF` or `divProbFP` should represent the fibroblast calibration degree of freedom.
- Whether the macrophage target proxy and EC target measurement are valid.
- Whether target extraction and 3D-to-2D conversions are sufficiently documented.

## 11. Recommended Next Step

Do not run ABC or build a neural-network surrogate yet. Update the calibration profile/freeze only after choosing between:

1. `core3`: `divProbP`, `pOffMax`, `dieProbN`
2. `core3 + activProbF`: if mentor review requires a fibroblast-target degree of freedom immediately

Then rerun calibration QC and only then start a small ABC pilot. The existing `core4` should be treated as superseded by this independent confirmation unless a deliberate continuity decision keeps `divProbFP`.
