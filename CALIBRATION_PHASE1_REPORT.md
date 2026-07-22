# Calibration Phase 1 Report

## Executive Summary

Phase 1 froze the untreated TNBC ABM calibration setup without rerunning the completed Morris studies or performing a large ABC calibration. The executable setup now has a named post-Morris `core4` profile, a preserved `legacy12` profile, centralized targets, a machine-readable freeze package, run-directory ABC outputs with provenance, and regression QC.

The model is not calibrated. Morris screening supports a reduced pilot profile; it does not prove identifiability.

## Architecture

- `ExampleGrid` is the ABM and exposes `RunHeadless(double[], initPop, maxStep)` for legacy positional runs and `RunHeadless(ModelParameters, maxStep)` for named runs.
- `ModelParameters` is the named executable registry: 74 audited quantities, 45 screened parameters, immutable values, registry bounds/transforms, validation, and legacy-vector export.
- Morris source uses `ModelParameters`, fresh `ExampleGrid` instances, fresh `Rand` instances, checkpointed outputs, explicit failure flags, and common random numbers.
- ABC now uses `CalibrationProfile`, `CalibrationTarget`, and `CalibrationFreeze`.

## Morris Verification

Verified from repository artifacts:

- Pilot: 10 trajectories, 460 simulations, QC PASS 11/11.
- Primary: 20 trajectories, 920 simulations, QC PASS 11/11.
- Confirmation: 571 points x 3 seeds = 1,713 simulations for 18 selected parameters.
- Total reported new simulations: 3,093.
- Primary outcomes: 357 finite, 323 invalid, 240 extinct, 0 model exceptions.
- Confirmation stability: median per-output Spearman 0.903, median three-seed SNR 1.12.

## Calibration Decision

Final `core4` profile:

| Parameter | Transform | Bounds | Why Included |
|---|---|---|---|
| `divProbP` | log | [0.005, 0.03] | primary rank 7, confirmed rank 6, SNR 1.46 |
| `pOffMax` | log | [0.01, 0.20] | primary rank 8, confirmed rank 7, SNR 1.29 |
| `divProbFP` | linear | [0.018, 0.038] | primary rank 20, already ABC-inferred |
| `dieProbN` | log | [0.008, 0.025] | primary rank 30, already ABC-inferred, extinction-associated |

Excluded legacy ABC12 parameters are fixed at executable baselines in `core4`: `netN`, `pOnMax`, `dieProbP`, `cafDivBoost`, `ecSurvival`, `activProbF`, `activProbM`, and `activProbE`.

Tier B fixed influential parameters remain unresolved and are not inferred: `divProbMP`, `dieProbMP`, `divProbFN`, `stressStrength`, `divProbMN`, `dieProbMN`, `dieProbFN`, `migrProbP`, `dieProbEN`, `clusterRadius`, `lambdaStress`, `dieProbL`, and `dieProbEP`.

## Changes

- Added `OnLatticeExample/CalibrationTarget.java`: named target definitions and distance utilities.
- Added `OnLatticeExample/CalibrationProfile.java`: `core4` and `legacy12` profile definitions.
- Added `OnLatticeExample/CalibrationFreeze.java`: freeze package writer/verifier.
- Added `OnLatticeExample/CalibrationQualityControl.java`: profile, target, freeze, and reproducibility QC.
- Updated `OnLatticeExample/ABCRejection.java`: flag CLI, named profiles, run manifests, run-directory CSV outputs, deterministic draw seeds, resume/force/dry-run behavior, explicit outcome labels.
- Updated `OnLatticeExample/ModelParameters.java`: exposes immutable value map for QC/freeze validation.
- Added `calibration/freeze/`: machine-readable freeze files.
- Added `CALIBRATION_READINESS_AUDIT.md`, `CALIBRATION_RUNBOOK.md`, and `MENTOR_DECISIONS_REQUIRED.md`.
- Updated `README.md`.

## Freeze Package

`calibration/freeze/model_freeze.json` records commit `ae77dc5db82fa78075107dac570986d67dcae103`, branch `main`, dirty state, source-file hashes, coordinate-file hashes, grid 100x100, horizon 1440, snapshots `[0,480,960,1440]`, untreated status, profile hash, target hash, and distance function name.

CSV freeze files:

- `calibration_parameters.csv`: `core4` plus preserved `legacy12`.
- `calibration_targets.csv`: 10 centralized targets.
- `fixed_parameter_snapshot.csv`: screened executable parameters fixed under `core4`.

## Verification Results

Compilation:

- `javac -cp .:HAL-freq.jar:lwjgl.jar OnLatticeExample/*.java` succeeded.

Existing QC:

- `MorrisQualityControl 9001 25 1440` passed baseline regression, deterministic rerun/two-thread independence, registry bounds/transforms, and snapshot shape.

New QC:

- `CalibrationQualityControl 9001 25 1440` passed profile integrity, legacy/named equivalence, core baseline equivalence, named mutation validation, target integrity, deterministic proposals/simulations, and freeze hash validation.

Smoke outputs:

- Dry run: `results/abc-core4-dryrun/run_manifest.json`.
- Core4 smoke: `results/abc-core4-smoke/`.
- Deterministic rerun: `results/abc-core4-smoke-rerun/`.
- Legacy smoke: `results/abc-legacy12-smoke/`.

Core4 smoke result:

- 4 draws, 2 valid finite, 2 stromal-compartment-loss, 0 extinction, 0 exceptions.
- Selected epsilon: 5.98849878777.
- Accepted: 2/4.
- Deterministic scientific outputs matched between smoke and rerun after removing runtime.
- Resume preserved `abc_all_draws.csv` and `distance_summary.csv`.

Legacy12 smoke result:

- 2 draws, 1 valid finite, 1 invalid, 0 exceptions.
- Accepted: 1/2.

## Deliberately Not Done

- No full ABC calibration run.
- No rerun of 10-trajectory pilot, 20-trajectory primary, or confirmation Morris studies.
- No biological event logic, event ordering, update semantics, coordinate files, grid dimensions, target values, target weights/scales, bounds, transformations, or baselines changed.
- No Tier B parameter was added to calibration.
- No low-influence or structurally inactive mechanism was deleted.
- No surrogate or neural-network workflow was implemented.

## Remaining Risks

Scientific risks:

- Target extraction provenance and proxy validity remain unresolved.
- The 480 steps/week convention conflicts with older timing comments.
- Several fixed turnover rates imply short biological half-lives.
- Fixed Tier B parameters dominate model behavior but are not yet approved for inference.

Engineering risks:

- The repository tracks compiled `.class` files, so local compilation dirties binary outputs.
- `ExampleGrid.main(...)` remains an older file-driven mode with direct `divProbN` semantics; Phase 1 preserves it but does not modernize it.
- ABC is serial; no thread-count ABC seed test is needed yet.

## Recommended First ABC Pilot

Do not run a large pilot until mentor target/range questions are reviewed.

**2026-07-22 addendum:** a later independent 20-trajectory, 5-replicate Morris confirmation in
`results/morris-independent-confirm-20x5/` supersedes the exact `core4` recommendation below. It strongly
confirms `divProbP` and `pOffMax`, keeps `dieProbN` as a stable but cautious extinction/boundary parameter,
and weakens support for `divProbFP`. The existing `core4` profile should not be run unchanged unless a
deliberate continuity decision keeps `divProbFP`. If a fibroblast degree of freedom is required immediately,
review `activProbF` versus `divProbFP` first.

The earlier first technical pilot command was:

```bash
java -cp .:HAL-freq.jar:lwjgl.jar OnLatticeExample.ABCRejection --profile core4 --draws 100 --epsilon -1 --quantile 0.05 --seed 12345 --init-pop 25 --max-step 1440 --output-dir results/abc-core4-pilot-100
```

This should now be treated as historical Phase 1 guidance, not the current final recommendation. Update the
calibration profile/freeze after mentor review before running ABC.
