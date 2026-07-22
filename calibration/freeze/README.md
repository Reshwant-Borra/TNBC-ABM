# Calibration Freeze Package

This directory freezes the untreated TNBC ABM calibration definitions for the post-Morris Phase 1 workflow.

- `model_freeze.json` records source and coordinate hashes, commit, branch, model entry point, horizon, snapshots, and profile/target hashes.
- `calibration_parameters.csv` records both `core4` and `legacy12` profile definitions.
- `calibration_targets.csv` records the named target definitions used by the distance function.
- `fixed_parameter_snapshot.csv` records screened executable parameters fixed at baseline under `core4`.

Regenerate after intentional definition changes with:

```bash
javac -cp .:HAL-freq.jar:lwjgl.jar OnLatticeExample/*.java
java -cp .:HAL-freq.jar:lwjgl.jar OnLatticeExample.CalibrationFreeze calibration/freeze
```

Verify with:

```bash
java -cp .:HAL-freq.jar:lwjgl.jar OnLatticeExample.CalibrationQualityControl
```
