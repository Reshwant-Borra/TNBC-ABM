# Global Morris Screening Pipeline

This pipeline performs untreated global Morris elementary-effects screening. It does not run rejection ABC, estimate Sobol indices, calibrate the model, or train a surrogate.

## Build

From the repository root in Windows PowerShell:

```powershell
javac -cp ".;HAL-freq.jar;lwjgl.jar" OnLatticeExample\ModelParameters.java OnLatticeExample\ExampleGrid.java OnLatticeExample\ABCRejection.java OnLatticeExample\MorrisQualityControl.java OnLatticeExample\MorrisSensitivitySweep.java
```

On macOS/Linux, replace semicolons in the classpath with colons and path backslashes with slashes.

## Required Commands

1. Baseline regression and thread-independence QC:

```powershell
java -Xmx3g -cp ".;HAL-freq.jar;lwjgl.jar" OnLatticeExample.MorrisQualityControl 9001 25 1440
```

2. Two-trajectory smoke test (92 simulations for 45 screened parameters):

```powershell
java -Xmx6g -cp ".;HAL-freq.jar;lwjgl.jar" OnLatticeExample.MorrisSensitivitySweep --trajectories 2 --levels 6 --threads 12 --master-seed 9001 --replicates 1 --init-pop 25 --output-dir results/morris-smoke
```

3. Ten-trajectory pilot (460 simulations):

```powershell
java -Xmx8g -cp ".;HAL-freq.jar;lwjgl.jar" OnLatticeExample.MorrisSensitivitySweep --trajectories 10 --levels 6 --threads 12 --master-seed 9001 --replicates 1 --init-pop 25 --output-dir results/morris-pilot
```

4. Twenty-trajectory primary run (920 simulations):

```powershell
java -Xmx8g -cp ".;HAL-freq.jar;lwjgl.jar" OnLatticeExample.MorrisSensitivitySweep --trajectories 20 --levels 6 --threads 12 --master-seed 9001 --replicates 1 --init-pop 25 --output-dir results/morris
```

5. Twenty trajectories with three matched replicates (2,760 simulations):

```powershell
java -Xmx10g -cp ".;HAL-freq.jar;lwjgl.jar" OnLatticeExample.MorrisSensitivitySweep --trajectories 20 --levels 6 --threads 12 --master-seed 9001 --replicates 3 --init-pop 25 --output-dir results/morris-3rep
```

6. Resume an interrupted primary run. The design and sample-to-seed mapping must use the original arguments:

```powershell
java -Xmx8g -cp ".;HAL-freq.jar;lwjgl.jar" OnLatticeExample.MorrisSensitivitySweep --trajectories 20 --levels 6 --threads 12 --master-seed 9001 --replicates 1 --init-pop 25 --output-dir results/morris --resume
```

7. Generate/recheck the tabular analysis inventory without figures:

```powershell
python analyze_morris.py --input-dir results/morris --analysis-only
```

8. Generate all figures:

```powershell
python analyze_morris.py --input-dir results/morris --output-dir results/morris/figures --figures-only --top-n 10
```

9. Run targeted multi-seed confirmation. This reuses the primary design, selects the top overall drivers, high-sigma candidates, extinction drivers, influential fixed parameters, and unexpectedly low essential parameters, then runs only required neighbouring points with three matched seeds:

```powershell
java -Xmx8g -cp ".;HAL-freq.jar;lwjgl.jar" OnLatticeExample.MorrisSensitivitySweep --trajectories 20 --levels 6 --threads 12 --master-seed 9001 --replicates 1 --init-pop 25 --output-dir results/morris --confirm-only --confirmation-replicates 3 --confirmation-top 10
```

10. Reproduce all Java summaries, QC, classifications, and the final report from checkpoints without rerunning completed simulations:

```powershell
java -Xmx8g -cp ".;HAL-freq.jar;lwjgl.jar" OnLatticeExample.MorrisSensitivitySweep --trajectories 20 --levels 6 --threads 12 --master-seed 9001 --replicates 1 --init-pop 25 --output-dir results/morris --resume
```

## Reproducibility and Resume Contract

- `master-seed`, `trajectories`, `levels`, and the parameter registry determine the design.
- Simulation seeds depend only on master seed, trajectory ID, and replicate ID. Every point in a trajectory uses the same replicate-seed set (common random numbers).
- Each simulation creates a fresh `ExampleGrid` and fresh HAL `Rand`.
- Complete checkpoints are skipped. Incomplete, incompatible, or seed-mismatched checkpoints are surfaced and rerun.
- `--force` intentionally ignores existing checkpoints; do not use it for routine resume.
- `initPop` is itself screened over the documented integer range. `--init-pop` specifies the current baseline used by regression/provenance, not a silent removal of initial tumour count from the global design.

## Outputs

The Java driver writes the full design, raw snapshots/events/spatial diagnostics, long-form derived outputs, failures, matched and replicate-mean elementary effects, per-output summaries, global rankings, failure associations, classifications, QC, and the scientific report. Invalid denominators remain blank with explicit invalid reasons; extinct and errored simulations remain present.
