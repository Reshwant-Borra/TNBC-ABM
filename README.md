# TNBC ABM Calibration and Sensitivity Workflow

This repository contains a Java/HAL agent-based model of untreated TNBC lung metastasis and the supporting calibration/sensitivity infrastructure.

The current scientific state is:

- The model is **not calibrated**.
- The untreated Morris global sensitivity workflow is complete and preserved.
- Phase 1 post-Morris calibration infrastructure is implemented.
- The recommended first ABC pilot profile is `core4`, not the old 12-parameter workflow.
- `legacy12` remains available for reproducibility and regression testing.

## Model Entry Points

- `OnLatticeExample.ExampleGrid`: TNBC ABM.
- `ExampleGrid.RunHeadless(double[], initPop, maxStep)`: legacy 12-position parameter vector.
- `ExampleGrid.RunHeadless(ModelParameters, maxStep)`: named parameter interface used by Morris and new calibration code.
- `OnLatticeExample.ABCRejection`: rejection ABC driver.
- `OnLatticeExample.MorrisSensitivitySweep`: resumable untreated Morris screen.
- `OnLatticeExample.MorrisQualityControl`: existing Morris regression QC.
- `OnLatticeExample.CalibrationQualityControl`: calibration profile/target/freeze QC.
- `OnLatticeExample.CalibrationFreeze`: machine-readable freeze package generator.

The model requires `QuadratEndothelialOn.txt` and `QuadratStrOn.txt` in the working directory.

## Calibration Profiles

### `core4`

Initial post-Morris reduced profile:

- `divProbP` sampled on log scale over `[0.005, 0.03]`
- `pOffMax` sampled on log scale over `[0.01, 0.20]`
- `divProbFP` sampled linearly over `[0.018, 0.038]`
- `dieProbN` sampled on log scale over `[0.008, 0.025]`

All other executable screened parameters are fixed at current `ModelParameters.currentBaseline(initPop)` values. Tier B fixed influential parameters are not inferred until mentor/literature review approves their ranges and parameterization.

### `legacy12`

Preserves the original ABC parameter names and order:

```text
netN, dieProbN, pOnMax, pOffMax, divProbP, dieProbP,
cafDivBoost, ecSurvival, activProbF, divProbFP, activProbM, activProbE
```

This profile keeps the old physical-space uniform sampling behavior for compatibility. It is not the recommended scientific default.

## Build

macOS/Linux:

```bash
javac -cp .:HAL-freq.jar:lwjgl.jar OnLatticeExample/*.java
```

Windows PowerShell:

```powershell
javac -cp ".;HAL-freq.jar;lwjgl.jar" OnLatticeExample\*.java
```

## Calibration QC

macOS/Linux:

```bash
java -cp .:HAL-freq.jar:lwjgl.jar OnLatticeExample.CalibrationQualityControl 9001 25 1440
```

Windows PowerShell:

```powershell
java -cp ".;HAL-freq.jar;lwjgl.jar" OnLatticeExample.CalibrationQualityControl 9001 25 1440
```

## Rejection ABC

Flag-based CLI:

```bash
java -cp .:HAL-freq.jar:lwjgl.jar OnLatticeExample.ABCRejection \
  --profile core4 \
  --draws 100 \
  --epsilon -1 \
  --quantile 0.05 \
  --seed 12345 \
  --init-pop 25 \
  --max-step 1440 \
  --output-dir results/abc-core4-pilot-100
```

Supported flags:

- `--profile core4|legacy12`
- `--draws N`
- `--epsilon X` (`-1` selects quantile mode)
- `--quantile Q`
- `--seed S`
- `--init-pop N`
- `--max-step N`
- `--output-dir DIR`
- `--dry-run`
- `--resume`
- `--force`

Old positional syntax still works as a `legacy12` compatibility wrapper:

```bash
java -cp .:HAL-freq.jar:lwjgl.jar OnLatticeExample.ABCRejection 1000 -1 0.2 12345 25
```

ABC outputs are written under the selected output directory:

- `run_manifest.json`
- `resolved_config.json`
- `abc_all_draws.csv`
- `abc_accepted.csv`
- `distance_summary.csv`
- `best_run_target_breakdown.csv`

The historical root-level `posterior_java.csv` is preserved but is no longer the default output target.

## Freeze Package

The Phase 1 freeze package is in `calibration/freeze/`:

- `model_freeze.json`
- `calibration_parameters.csv`
- `calibration_targets.csv`
- `fixed_parameter_snapshot.csv`
- `README.md`

Regenerate after intentional source/profile/target changes:

```bash
java -cp .:HAL-freq.jar:lwjgl.jar OnLatticeExample.CalibrationFreeze calibration/freeze
```

`ABCRejection` and `CalibrationQualityControl` verify the freeze hashes before execution.

## Morris Screening

The completed untreated Morris workflow is documented in:

- `MORRIS_PIPELINE.md`
- `MORRIS_EXECUTION_LOG.md`
- `results/morris-pilot-10/`
- `results/morris-primary-20/`
- `results/morris-primary-20/FINAL_GLOBAL_SENSITIVITY_REPORT.md`
- `results/morris-primary-20/FINAL_PARAMETER_DECISION_TABLE.csv`
- `results/morris-primary-20/CONFIRMED_PARAMETER_EFFECTS.csv`

Existing Morris QC:

```bash
java -Xmx3g -cp .:HAL-freq.jar:lwjgl.jar OnLatticeExample.MorrisQualityControl 9001 25 1440
```

Morris screening is not calibration. It supports the reduced pilot profile and identifies fixed parameters requiring review; it does not establish identifiability.

## Phase 1 Documentation

- `CALIBRATION_READINESS_AUDIT.md`: repository, ABC, target, Morris-to-calibration, and provenance audit.
- `CALIBRATION_PHASE1_REPORT.md`: implemented changes, QC, smoke-test results, risks, and recommended first pilot.
- `CALIBRATION_RUNBOOK.md`: verified macOS/Linux and Windows PowerShell commands.
- `MENTOR_DECISIONS_REQUIRED.md`: biological/provenance questions that remain unresolved.

Future work should proceed through a small `core4` ABC pilot, mentor review of Tier B fixed drivers, posterior predictive checks, and only then any larger ABC-SMC/SNPE or surrogate workflow.
