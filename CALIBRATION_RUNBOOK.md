# Calibration Runbook

Run commands from the repository root, which must contain `QuadratEndothelialOn.txt` and `QuadratStrOn.txt`.

## macOS/Linux

Compile:

```bash
javac -cp .:HAL-freq.jar:lwjgl.jar OnLatticeExample/*.java
```

Regenerate the freeze package:

```bash
java -cp .:HAL-freq.jar:lwjgl.jar OnLatticeExample.CalibrationFreeze calibration/freeze
```

Run existing Morris QC:

```bash
java -Xmx3g -cp .:HAL-freq.jar:lwjgl.jar OnLatticeExample.MorrisQualityControl 9001 25 1440
```

Run calibration QC:

```bash
java -cp .:HAL-freq.jar:lwjgl.jar OnLatticeExample.CalibrationQualityControl 9001 25 1440
```

Dry run the post-Morris profile:

```bash
java -cp .:HAL-freq.jar:lwjgl.jar OnLatticeExample.ABCRejection --profile core4 --draws 4 --epsilon -1 --quantile 0.5 --seed 12345 --init-pop 25 --max-step 1440 --output-dir results/abc-core4-dryrun --dry-run --force
```

Tiny `core4` smoke test:

```bash
java -cp .:HAL-freq.jar:lwjgl.jar OnLatticeExample.ABCRejection --profile core4 --draws 4 --epsilon -1 --quantile 0.5 --seed 12345 --init-pop 25 --max-step 1440 --output-dir results/abc-core4-smoke --force
```

Deterministic rerun check:

```bash
java -cp .:HAL-freq.jar:lwjgl.jar OnLatticeExample.ABCRejection --profile core4 --draws 4 --epsilon -1 --quantile 0.5 --seed 12345 --init-pop 25 --max-step 1440 --output-dir results/abc-core4-smoke-rerun --force
awk -F, 'BEGIN{OFS=","} {$(NF-1)=""; print}' results/abc-core4-smoke/abc_all_draws.csv > /tmp/abc_smoke_a.csv
awk -F, 'BEGIN{OFS=","} {$(NF-1)=""; print}' results/abc-core4-smoke-rerun/abc_all_draws.csv > /tmp/abc_smoke_b.csv
diff -u /tmp/abc_smoke_a.csv /tmp/abc_smoke_b.csv
diff -u results/abc-core4-smoke/distance_summary.csv results/abc-core4-smoke-rerun/distance_summary.csv
```

Resume check:

```bash
cp results/abc-core4-smoke/abc_all_draws.csv /tmp/abc_all_before_resume.csv
cp results/abc-core4-smoke/distance_summary.csv /tmp/abc_summary_before_resume.csv
java -cp .:HAL-freq.jar:lwjgl.jar OnLatticeExample.ABCRejection --profile core4 --draws 4 --epsilon -1 --quantile 0.5 --seed 12345 --init-pop 25 --max-step 1440 --output-dir results/abc-core4-smoke --resume
diff -u /tmp/abc_all_before_resume.csv results/abc-core4-smoke/abc_all_draws.csv
diff -u /tmp/abc_summary_before_resume.csv results/abc-core4-smoke/distance_summary.csv
```

Legacy compatibility smoke:

```bash
java -cp .:HAL-freq.jar:lwjgl.jar OnLatticeExample.ABCRejection --profile legacy12 --draws 2 --epsilon -1 --quantile 0.5 --seed 12345 --init-pop 25 --max-step 1440 --output-dir results/abc-legacy12-smoke --force
```

Old positional wrapper:

```bash
java -cp .:HAL-freq.jar:lwjgl.jar OnLatticeExample.ABCRejection 2 -1 0.5 12345 25
```

Inspect outputs:

```bash
cat results/abc-core4-smoke/distance_summary.csv
head -5 results/abc-core4-smoke/abc_all_draws.csv
cat results/abc-core4-smoke/best_run_target_breakdown.csv
cat results/abc-core4-smoke/run_manifest.json
```

## Windows PowerShell

Compile:

```powershell
javac -cp ".;HAL-freq.jar;lwjgl.jar" OnLatticeExample\*.java
```

Regenerate the freeze package:

```powershell
java -cp ".;HAL-freq.jar;lwjgl.jar" OnLatticeExample.CalibrationFreeze calibration\freeze
```

Run existing Morris QC:

```powershell
java -Xmx3g -cp ".;HAL-freq.jar;lwjgl.jar" OnLatticeExample.MorrisQualityControl 9001 25 1440
```

Run calibration QC:

```powershell
java -cp ".;HAL-freq.jar;lwjgl.jar" OnLatticeExample.CalibrationQualityControl 9001 25 1440
```

Dry run:

```powershell
java -cp ".;HAL-freq.jar;lwjgl.jar" OnLatticeExample.ABCRejection --profile core4 --draws 4 --epsilon -1 --quantile 0.5 --seed 12345 --init-pop 25 --max-step 1440 --output-dir results\abc-core4-dryrun --dry-run --force
```

Tiny `core4` smoke test:

```powershell
java -cp ".;HAL-freq.jar;lwjgl.jar" OnLatticeExample.ABCRejection --profile core4 --draws 4 --epsilon -1 --quantile 0.5 --seed 12345 --init-pop 25 --max-step 1440 --output-dir results\abc-core4-smoke --force
```

Deterministic rerun check:

```powershell
java -cp ".;HAL-freq.jar;lwjgl.jar" OnLatticeExample.ABCRejection --profile core4 --draws 4 --epsilon -1 --quantile 0.5 --seed 12345 --init-pop 25 --max-step 1440 --output-dir results\abc-core4-smoke-rerun --force
Compare-Object (Import-Csv results\abc-core4-smoke\abc_all_draws.csv | Select-Object * -ExcludeProperty runtime_ms) (Import-Csv results\abc-core4-smoke-rerun\abc_all_draws.csv | Select-Object * -ExcludeProperty runtime_ms)
Compare-Object (Get-Content results\abc-core4-smoke\distance_summary.csv) (Get-Content results\abc-core4-smoke-rerun\distance_summary.csv)
```

Resume check:

```powershell
Copy-Item results\abc-core4-smoke\abc_all_draws.csv $env:TEMP\abc_all_before_resume.csv -Force
Copy-Item results\abc-core4-smoke\distance_summary.csv $env:TEMP\abc_summary_before_resume.csv -Force
java -cp ".;HAL-freq.jar;lwjgl.jar" OnLatticeExample.ABCRejection --profile core4 --draws 4 --epsilon -1 --quantile 0.5 --seed 12345 --init-pop 25 --max-step 1440 --output-dir results\abc-core4-smoke --resume
Compare-Object (Get-Content $env:TEMP\abc_all_before_resume.csv) (Get-Content results\abc-core4-smoke\abc_all_draws.csv)
Compare-Object (Get-Content $env:TEMP\abc_summary_before_resume.csv) (Get-Content results\abc-core4-smoke\distance_summary.csv)
```

Legacy compatibility smoke:

```powershell
java -cp ".;HAL-freq.jar;lwjgl.jar" OnLatticeExample.ABCRejection --profile legacy12 --draws 2 --epsilon -1 --quantile 0.5 --seed 12345 --init-pop 25 --max-step 1440 --output-dir results\abc-legacy12-smoke --force
```

Inspect outputs:

```powershell
Get-Content results\abc-core4-smoke\distance_summary.csv
Import-Csv results\abc-core4-smoke\abc_all_draws.csv | Select-Object -First 4
Get-Content results\abc-core4-smoke\best_run_target_breakdown.csv
Get-Content results\abc-core4-smoke\run_manifest.json
```
