# Calibration Readiness Audit

Audit date: 2026-07-22  
Repository branch: `main`  
Repository commit at freeze: `ae77dc5db82fa78075107dac570986d67dcae103`  
Working tree: dirty after Phase 1 source, freeze, docs, and smoke-output additions.

## Repository State

Relevant source files inspected:

- `OnLatticeExample/ExampleGrid.java`: TNBC ABM, headless simulation, initialization, stochastic updates, snapshots.
- `OnLatticeExample/ModelParameters.java`: immutable named parameter registry with 74 audited quantities and 45 screened parameters.
- `OnLatticeExample/ABCRejection.java`: rejection ABC driver.
- `OnLatticeExample/MorrisSensitivitySweep.java`: untreated global Morris design, execution, checkpointing, output derivation, reporting.
- `OnLatticeExample/MorrisQualityControl.java`: existing Morris regression QC.
- `OnLatticeExample/MechanismTestHarness.java`: paired low/high mechanism smoke harness.

Relevant analysis and provenance files inspected:

- `README.md`, `MORRIS_PIPELINE.md`, `MORRIS_EXECUTION_LOG.md`
- `GLOBAL_PARAMETER_REGISTRY.csv`, `GLOBAL_PARAMETER_REGISTRY.md`
- `PARAMETER_PROVENANCE_AND_CODE_AUDIT.md`
- `MECHANISM_TEST_AND_TIME_RECALCULATION.md`, `mechanism_test_results.csv`
- `analyze_morris.py`, `morris_stage_analysis.py`, `morris_rank_stability.py`, `morris_confirmation_analysis.py`, `morris_decision_table.py`
- `SURROGATE_DATA_PLAN.md`

Relevant result directories inspected:

- `results/morris-pilot-10/`
- `results/morris-primary-20/`
- `results/morris-primary-20/confirmation/`
- `results/checkpoint-v4/`, `results/morris-smoke/`, `results/dev-smoke-1/` as prior development/checkpoint evidence

Build/runtime:

- Java build uses the bundled HAL jar and, on this host, `lwjgl.jar`:  
  `javac -cp .:HAL-freq.jar:lwjgl.jar OnLatticeExample/*.java`
- Runtime requires Java 26.0.1 on this host. HAL emits a final-field-mutation warning from the bundled jar; existing Morris QC says this does not alter per-run model/RNG isolation.
- Required coordinate files in repository root:
  `QuadratEndothelialOn.txt` (237 parsed coordinate pairs, SHA-256 `169618f369bd6f880fb2f197a146308f3f84b047565b908b27eb52188abaceaa`)
  and `QuadratStrOn.txt` (142 parsed coordinate pairs, SHA-256 `fca3eba27df2ac18dec2bd13800b26f1254c0fb939899be196273c34cfa5cef6`).

No `AGENTS.md` was found in or above the repository. An untracked empty file named `Audit` existed before Phase 1 edits.

## Existing Calibration Implementation

Current ABC entry point:

- `OnLatticeExample.ABCRejection`
- New flag CLI supports `--profile`, `--draws`, `--epsilon`, `--quantile`, `--seed`, `--init-pop`, `--max-step`, `--output-dir`, `--dry-run`, `--resume`, and `--force`.
- Old positional CLI is still accepted as a `legacy12` wrapper: `[N] [epsilon] [quantile] [seed] [initPop]`.

Legacy 12-parameter order:

1. `netN`
2. `dieProbN`
3. `pOnMax`
4. `pOffMax`
5. `divProbP`
6. `dieProbP`
7. `cafDivBoost`
8. `ecSurvival`
9. `activProbF`
10. `divProbFP`
11. `activProbM`
12. `activProbE`

Legacy bounds and old sampling behavior:

- Bounds are the registry/current ABC bounds.
- `legacy12` samples `Uniform(lower, upper)` in physical space for all 12 parameters, matching the previous executable `ABCRejection` behavior.
- `RunHeadless(double[], initPop, maxStep)` still applies the positional vector and derives `divProbN = dieProbN + netN`.

Current targets and distance:

- Targets are centralized in `CalibrationTarget`.
- Snapshot steps for calibration are `{0, 480, 960, 1440}`.
- Target statistics are JNK-positive tumour fraction at 480 and 1440; activated EC fraction at 480, 960, 1440; activated macrophage fraction at 1440; fibroblast log10 fold change at 1440; tumour log10 fold change at 480, 960, 1440.
- Distance is weighted, scaled Euclidean over the 10 targets.
- Final tumour extinction returns infinite distance.
- Missing non-tumour target statistics retain the old finite penalty: residual `3.0`, contribution `weight * 9`.
- Exceptions are labeled `MODEL_EXCEPTION`, not biological extinction.

Seed policy:

- Morris uses common random numbers within each trajectory via `simulationSeed(masterSeed, trajectory, replicate)`.
- ABC now uses deterministic draw-index seeds: one proposal seed and one simulation seed per draw, derived only from master seed and draw ID.
- Each simulation constructs a fresh `ExampleGrid` and a fresh HAL `Rand`.

Outputs:

- Old root overwrite of `posterior_java.csv` is replaced by run-directory outputs:
  `run_manifest.json`, `resolved_config.json`, `abc_all_draws.csv`, `abc_accepted.csv`, `distance_summary.csv`, and `best_run_target_breakdown.csv`.
- Existing historical `posterior_java.csv` remains preserved in the repository root.

## Morris Completion Verification

Repository artifacts verify the completed untreated Morris workflow:

- Registry: 74 audited quantities, 45 screened parameters.
- Pilot: `results/morris-pilot-10/run.log` records 10 trajectories x 46 points = 460 simulations.
- Primary: `results/morris-primary-20/run.log` records 20 trajectories x 46 points = 920 simulations.
- Confirmation: `FINAL_GLOBAL_SENSITIVITY_REPORT.md` records 571 points x 3 replicates = 1,713 simulations for 18 selected parameters.
- Total new simulations reported: 3,093.
- Pilot and primary QC reports both passed 11/11 checks: unique names, finite transforms, bounds, one-coordinate Morris steps, seed matching/CRN, no dropped records, deterministic rerun, legacy/named equivalence, fresh grid/Rand, and CSV width checks.
- Confirmation stability report records median one-vs-three-seed Spearman 0.903 and median three-seed SNR 1.12.

## Morris-to-Calibration Decision for ABC12

| Parameter | Primary Rank | Confirmed Rank | Classification | Recommendation | Phase 1 Decision | Included in `core4` | Fixed Value if Excluded | Bounds | Transform in `core4`/registry | Principal Outputs | Identifiability Concern |
|---|---:|---:|---|---|---|---|---:|---|---|---|---|
| `netN` | 33 | n/a | LOW_INFLUENCE_WITHIN_RANGE | NEEDS_ADDITIONAL_DATA | fixed at baseline | no | 0.00259979918080 | [0.0015, 0.005] | log | tumour burden, extinction, JNK fraction | Coupled to `dieProbN`; `divProbN=dieProbN+netN` |
| `dieProbN` | 30 | n/a | MODERATE_INFLUENCE | CALIBRATE | infer | yes | n/a | [0.008, 0.025] | log | tumour spread/burden/extinction | Confounded with `netN` and EC survival |
| `pOnMax` | 25 | n/a | MODERATE_INFLUENCE | INCONCLUSIVE | fixed at baseline | no | 0.0344299168465 | [0.01, 0.10] | log | JNK fraction, tumour burden | Confounded with `pOffMax`, stress/CAF fields |
| `pOffMax` | 8 | 7 | HIGH_INFLUENCE | CALIBRATE | infer | yes | n/a | [0.01, 0.20] | log | JNK fractions | Event-ladder clamp can mask switching |
| `divProbP` | 7 | 6 | HIGH_INFLUENCE | CALIBRATE | infer | yes | n/a | [0.005, 0.03] | log | tumour burden, JNK fraction, extinction | Confounded with CAF support and empty-space limits |
| `dieProbP` | 21 | n/a | MODERATE_INFLUENCE | INCONCLUSIVE | fixed at baseline | no | 0.00306108452805 | [0.001, 0.004] | log | tumour burden/JNK | Confounded with EC survival and division |
| `cafDivBoost` | 38 | n/a | LOW_INFLUENCE_WITHIN_RANGE | NEEDS_ADDITIONAL_DATA | fixed at baseline | no | 0.427609468482 | [0, 1] | linear | tumour burden, JNK rim | Confounded with `divProbP` and CAF density |
| `ecSurvival` | 41 | 18 | LOW_INFLUENCE_WITHIN_RANGE | FIX_AT_SUPPORTED_VALUE | fixed at baseline | no | 0.0509998219712 | [0, 0.3] | linear | tumour burden/extinction | Confirmed SNR 0.56; weak signal relative to stochasticity |
| `activProbF` | 34 | n/a | LOW_INFLUENCE_WITHIN_RANGE | NEEDS_ADDITIONAL_DATA | fixed at baseline | no | 0.0235917824107 | [0.001, 0.05] | log | fibroblast burden, tumour burden | Confounded with `divProbFP` and hard-coded fibroblast boost |
| `divProbFP` | 20 | n/a | MODERATE_INFLUENCE | CALIBRATE | infer | yes | n/a | [0.018, 0.038] | linear | fibroblast burden, tumour burden | Coupled to `dieProbFP`, activation, and crowding |
| `activProbM` | 39 | 14 | LOW_INFLUENCE_WITHIN_RANGE | FIX_AT_SUPPORTED_VALUE | fixed at baseline | no | 0.0418290881943 | [0.02, 0.08] | linear | macrophage activation, EC activation | Confirmed SNR 0.92; proxy target concern |
| `activProbE` | 36 | n/a | LOW_INFLUENCE_WITHIN_RANGE | NEEDS_ADDITIONAL_DATA | fixed at baseline | no | 0.0594775137265 | [0.005, 0.08] | log | EC activation, tumour burden | Confounded with macrophage state and EC death |

## Fixed Influential Parameters Requiring Review

| Parameter | Baseline | Primary Rank | Confirmed SNR | Main Affected Outputs | Failure Association | Evidence Quality | Range Source | Why Not Inferred in Phase 1 | Decision Required |
|---|---:|---:|---:|---|---|---|---|---|---|
| `divProbMP` | 0.01575 | 1 | 3.31 | EC activation, tumour spread | macrophage loss | low | mechanism-harness range | fixed Tier B; sensitivity is not identifiability | Approve value/range and independent inference? |
| `dieProbMP` | 0.015 | 2 | 3.21 | tumour fold, extinction | tumour extinction | low | mechanism-harness range | fixed Tier B | Review activated macrophage turnover relationship |
| `divProbFN` | 0 | 3 | 1.17 | fibroblast fold | fibroblast loss | low | mechanism-harness range | fixed Tier B | Is zero resting fibroblast division intended? |
| `stressStrength` | 1.5 | 4 | 1.64 | early JNK fraction | fibroblast loss | low | mechanism-harness range | fixed Tier B | Defensible stress-field gain range |
| `divProbMN` | 0.005 | 5 | 1.57 | macrophage totals/fraction | macrophage loss | low | mechanism-harness range | fixed Tier B | Preserve homeostatic link to `dieProbMN`? |
| `dieProbMN` | 0.005 | 6 | 1.48 | macrophage fraction | macrophage loss | low | mechanism-harness range | fixed Tier B | Review resting macrophage half-life |
| `dieProbFN` | 0.008 | 9 | 1.15 | fibroblast loss/fold | fibroblast loss | low | mechanism-harness range | fixed Tier B | Resting fibroblast death plausibility |
| `migrProbP` | 0.1 | 10 | 1.58 | tumour spread/radius | fibroblast loss | low | mechanism-harness range | fixed Tier B | Calibrate independently or constrain to `migrProbN`? |
| `dieProbEN` | 0.005 | 11 | 0.75 | EC loss/fraction | EC loss | low | mechanism-harness range | fixed Tier B | Inactive EC death without division intended? |
| `clusterRadius` | 4 | 12 | 1.19 | establishment/spread | fibroblast loss | low | mechanism-harness range | fixed Tier B | Known experimental condition or uncertain? |
| `lambdaStress` | 2 | 13 | n/a | JNK fraction | fibroblast loss | low | mechanism-harness range | fixed Tier B | Defensible spatial range |
| `dieProbL` | 0.002 | 14 | n/a | JNK fraction/tumour | fibroblast loss | low | mechanism-harness range | fixed Tier B | Lung turnover mechanism/range |
| `dieProbEP` | 0.008 | 15 | n/a | EC colocalization/fraction | EC loss | low | mechanism-harness range | fixed Tier B | Coupling to activated EC division |

## Contradictions and Missing Provenance

- Java comments refer to `abc_config.yaml` and an SMC project; no such file exists in the repository.
- README described only the original 12-parameter ABC workflow and root `posterior_java.csv`; it is obsolete for Phase 1.
- `ExampleGrid.main(...)` file-driven mode reads direct `divProbN`, while `RunHeadless` and ABC use `netN` and derive `divProbN = dieProbN + netN`.
- Older comments in `ExampleGrid` list broad priors that disagree with the current registry and ABC bounds.
- Several comments mention five snapshots, but current calibration stops at 1440 and scores four snapshots.
- `RunHeadless` includes 2100 in its possible snapshot schedule only when `maxStep >= 2100`; ABC targets do not score 2100.
- Current untreated calibration horizon is 1440 steps. Time recalculation documents 480 steps/week, but other old comments mention 30-60 minutes/step.
- Target extraction provenance remains unresolved; fold targets include documented 3D-to-2D conversion but not recoverable raw extraction files.
- `mac` target is a proxy: activated macrophage fraction is not proven equivalent to perivascular macrophage fraction.
- Structurally inactive fields are documented and retained: `migrProbF`, `activProbMP`, `divProbEN`, `migrProbE`, `divProbL`, `unusedNeighborCountRadius`.

No biological event logic, timestep, grid dimension, coordinate file, target value, target weight, target scale, bound, transformation, or baseline value was changed in Phase 1.
