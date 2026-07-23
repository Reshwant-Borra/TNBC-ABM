# Morris Sensitivity Execution Log

Repository: `/Users/80034573/Downloads/abc-run`  
Execution host: macOS (`Darwin`), Apple Silicon (`arm64`)  
Started: 2026-07-17 11:33:28 EDT

This file records commands executed for the requested 10- and 20-trajectory Morris study. Commands are shown exactly as issued from the repository root unless a different working directory is stated.

## Audit and discovery

```bash
pwd && rg --files -g 'AGENTS.md' -g 'MORRIS_PIPELINE.md' -g 'GLOBAL_PARAMETER_REGISTRY.csv' -g 'MORRIS_QC_REPORT.md' -g 'GLOBAL_MORRIS_SENSITIVITY_REPORT.md' -g 'MorrisSensitivitySweep.java' -g 'analyze_morris.py' -g '!results/morris-pilot-10/**' -g '!results/morris-primary-20/**' | sort
```

Outcome: repository root confirmed; required pipeline, registry, source, analysis script, and smoke-report files found. No `AGENTS.md` was returned within the repository search.

```bash
wc -l MORRIS_PIPELINE.md GLOBAL_PARAMETER_REGISTRY.csv results/morris-smoke/MORRIS_QC_REPORT.md results/morris-smoke/GLOBAL_MORRIS_SENSITIVITY_REPORT.md OnLatticeExample/MorrisSensitivitySweep.java analyze_morris.py; find .. -name AGENTS.md -print; rg -n "compile|javac|java |trajectory|resume|analy|confirm|classpath|coordinate|input|thread|seed|output" MORRIS_PIPELINE.md; git status --short
```

Outcome: documentation command locations identified; no parent `AGENTS.md` found; the directory is not a Git worktree.

```bash
date '+%Y-%m-%d %H:%M:%S %Z'; uname -s; uname -m
```

Outcome: `2026-07-17 11:33:28 EDT`, `Darwin`, `arm64`.

## Environment and audit summary (STEP 1)

- Host: macOS `Darwin` 25.5.0, Apple Silicon `arm64`, `10` logical CPUs, `17.2 GB` RAM, `373 GB` free disk.
- JDK: OpenJDK `26.0.1`. `javac`/`java` on `PATH`.
- Registry: `GLOBAL_PARAMETER_REGISTRY.csv` — 74 audited quantities; 45 with `enter_morris_screen=true`
  (20 fixed, 12 ABC-inferred, 12 hard-coded, 1 fixed-CLI = `initPop`).
- ABC-12 inferred set: `netN, dieProbN, pOnMax, pOffMax, divProbP, dieProbP, cafDivBoost, ecSurvival, activProbF, divProbFP, activProbM, activProbE`.
- Documented commands (`MORRIS_PIPELINE.md`) validated against `MorrisSensitivitySweep.java` arg parser
  (`--trajectories --levels --threads --master-seed --replicates --init-pop --output-dir --confirm-only
  --confirmation-replicates --confirmation-top --resume --force`). No command invention required.

### Documented deviations (safe, justified, non-silent)

1. **Threads 12 → 8.** Documented commands use `--threads 12`; this host has 10 logical CPUs. I use
   `--threads 8` to leave OS/analysis headroom. Thread count does **not** affect scientific output:
   `MorrisQualityControl` re-passes "deterministic rerun and two-thread fresh-grid independence", and seeds
   depend only on (master seed, trajectory, replicate). QC in this session confirms this.
2. **Output directory names.** Task requests `results/morris-pilot-10` and `results/morris-primary-20`
   (docs used `results/morris-pilot`, `results/morris`). `--output-dir` is the only thing affected; the
   directory name does not enter the design or seeds. Smoke/dev results are preserved untouched.

### Verification commands executed this session

```bash
# Recompile (macOS classpath: ';'->':', '\'->'/')
javac -cp ".:HAL-freq.jar:lwjgl.jar" OnLatticeExample/ModelParameters.java OnLatticeExample/ExampleGrid.java \
  OnLatticeExample/ABCRejection.java OnLatticeExample/MorrisQualityControl.java OnLatticeExample/MorrisSensitivitySweep.java

# QC gate (baseline regression, deterministic rerun, thread independence, registry checks)
java -Xmx3g -cp ".:HAL-freq.jar:lwjgl.jar" OnLatticeExample.MorrisQualityControl 9001 25 1440
```

Outcome: compile clean; QC printed `PASS baseline regression`, `PASS deterministic rerun and two-thread
fresh-grid independence`, `PASS registry uniqueness/transforms/bounds and snapshot shape`
(`seed=9001 initPop=25 maxStep=1440 snapshots=4 screened=45 audited=74`).

## STEP 2 — 10-trajectory pilot

```bash
java -Xmx8g -cp ".:HAL-freq.jar:lwjgl.jar" OnLatticeExample.MorrisSensitivitySweep \
  --trajectories 10 --levels 6 --threads 8 --master-seed 9001 --replicates 1 --init-pop 25 \
  --output-dir results/morris-pilot-10
```

Expected design points: 10 × (45+1) = 460. Launched in background; resume-on-interrupt available by
re-issuing the same command (resume is the default; complete checkpoints are skipped).

**Result:** exit 0. 460/460 simulations, 460 run records, 0 duplicate physical points, 0 model exceptions.
QC **PASS 11/11**. Outcomes: 171 FINITE / 158 EXTINCT / 131 INVALID. Valid replicate-mean EEs 51 742,
lost 5 408.

### STEP 3 — pilot analysis commands

```bash
python analyze_morris.py --input-dir results/morris-pilot-10 --output-dir results/morris-pilot-10/figures --top-n 10
python morris_stage_analysis.py --input-dir results/morris-pilot-10 --prefix PILOT_ --top-n 10
```

Wrote `PILOT_TOP_PARAMETERS_BY_OUTPUT.csv`, `PILOT_FAILURE_DRIVERS.csv`,
`PILOT_PARAMETER_CLASSIFICATION.csv`, 383 figures, and `PILOT_REPORT.md`. Provisional smoke drivers
re-checked: dieProbMP/divProbMP/stressStrength hold (ranks 1/2/3); dieProbFP→25 and
macrophageInteractionRadius→19 were 2-trajectory artefacts.

### STEP 4 — range/failure review

Command basis: `morris_stage_analysis.py` per-parameter valid fractions + `morris_outputs.csv` invalid
reasons. Finding: min per-parameter valid-EE fraction 0.847; 0 parameters below 0.70; failing points
near-uniform across changed parameters; all probability bounds within [0,1]. **Decision: no bound
changes.** Written to `results/morris-pilot-10/RANGE_AND_FAILURE_REVIEW.md` +
`RANGE_REVIEW_FAILURE_ASSOCIATIONS.csv`.

## STEP 5 — 20-trajectory primary screen

```bash
java -Xmx8g -cp ".:HAL-freq.jar:lwjgl.jar" OnLatticeExample.MorrisSensitivitySweep \
  --trajectories 20 --levels 6 --threads 8 --master-seed 9001 --replicates 1 --init-pop 25 \
  --output-dir results/morris-primary-20
```

Expected design points: 20 × (45+1) = 920. Launched in background on unchanged ranges.

**Result:** exit 0. 920/920 simulations, 920 run records, 0 duplicate points, 0 exceptions. QC **PASS
11/11**. Outcomes: 357 FINITE / 323 INVALID / 240 EXTINCT. Valid replicate-mean EEs 104 497, lost 9 803.

### STEP 5–6 — primary analysis and rank stability

```bash
python analyze_morris.py --input-dir results/morris-primary-20 --output-dir results/morris-primary-20/figures --top-n 10
python morris_stage_analysis.py --input-dir results/morris-primary-20 --prefix PRIMARY_ --top-n 10
python morris_rank_stability.py --dir-a results/morris-pilot-10 --dir-b results/morris-primary-20 \
  --label-a 10traj --label-b 20traj --out-dir results/morris-primary-20
```

Stability: overall Spearman **0.844**; top-5/10/15 overlap 1.00/0.70/0.87; per-family Spearman 0.79–0.86;
failure-driver ranks noisier (Spearman 0.14–0.65). Outputs: `RANK_STABILITY_10traj_VS_20traj.csv`,
`RANK_STABILITY_REPORT.md`, `PRIMARY_*` tables.

## STEP 7–8 — confirmation selection and 3-seed confirmation

```bash
java -Xmx8g -cp ".:HAL-freq.jar:lwjgl.jar" OnLatticeExample.MorrisSensitivitySweep \
  --trajectories 20 --levels 6 --threads 8 --master-seed 9001 --replicates 1 --init-pop 25 \
  --output-dir results/morris-primary-20 --confirm-only --confirmation-replicates 3 --confirmation-top 12
```

Resumed all 920 primary checkpoints (0 re-run — primary CSVs preserved), selected **18 parameters**, and
ran **571 points × 3 replicates = 1713 simulations**. Selection documented in
`CONFIRMATION_PARAMETER_SELECTION.md`.

**Result:** exit 0. `morris_confirmed_rankings.csv` (2268 rows) + `confirmation/` subdir written; primary
CSVs intact (45-row global rankings). Confirmation analysis:

```bash
python morris_confirmation_analysis.py --primary-dir results/morris-primary-20 --out-dir results/morris-primary-20
```

Median one-vs-three-seed Spearman **0.903**; mu\* ratio 0.834; median 3-seed SNR 1.12; 0 lost pairs (median).
Wrote `CONFIRMED_PARAMETER_EFFECTS.csv`, `CONFIRMATION_STABILITY_REPORT.md`.

## STEP 9–11 — decision table, final report, surrogate plan

```bash
python morris_decision_table.py --primary-dir results/morris-primary-20 --pilot-dir results/morris-pilot-10 \
  --out results/morris-primary-20/FINAL_PARAMETER_DECISION_TABLE.csv
```

Decision table (51 rows = 45 screened + 6 inactive). Recommendations: REVIEW_FIXED_VALUE 13, INCONCLUSIVE 12,
LOW_INFLUENCE_WITHIN_RANGE 9, STRUCTURALLY_INACTIVE 6, CALIBRATE 4, NEEDS_ADDITIONAL_DATA 4,
FIX_AT_SUPPORTED_VALUE 2, FAILURE_DRIVER 1. Wrote `FINAL_GLOBAL_SENSITIVITY_REPORT.md` (22 sections) and
`SURROGATE_DATA_PLAN.md` (plan only; no training).

## Totals

New simulations this study: 460 (pilot) + 920 (primary) + 1713 (confirmation) = **3093**, plus QC
regressions. All QC **PASS**; 0 model exceptions. Prior smoke/dev results preserved untouched. Analysis
helper scripts added: `morris_stage_analysis.py`, `morris_rank_stability.py`, `morris_confirmation_analysis.py`,
`morris_decision_table.py`.

## Independent confirmatory Morris run (2026-07-22)

Purpose: independent reproducibility check after calibration-readiness work. This run used new Morris
trajectories and a new master seed; it did not rerun selected primary points.

Model/source protection:

- `git diff -- OnLatticeExample/ExampleGrid.java` was empty.
- Current `ExampleGrid.java` SHA-256:
  `0821b7fa4ab8dd3d8e213161d70ae3933ff706bfcf76d87a6b37020a831f0039`.
- Recent calibration work changed `ABCRejection.java`, `ModelParameters.java`, `README.md`, and added
  calibration profile/target/freeze/QC files; biological model logic was not changed for this run.

Pre-run commands:

```bash
javac -cp .:HAL-freq.jar:lwjgl.jar OnLatticeExample/*.java
java -Xmx3g -cp .:HAL-freq.jar:lwjgl.jar OnLatticeExample.MorrisQualityControl 9001 25 1440
java -cp .:HAL-freq.jar:lwjgl.jar OnLatticeExample.CalibrationQualityControl 9001 25 1440
```

Outcomes: compile succeeded; both QC commands passed. Recompilation made Morris `.class` files newer than
source, so stale compiled classes were not used.

Smoke/resume:

```bash
java -cp .:HAL-freq.jar:lwjgl.jar OnLatticeExample.MorrisSensitivitySweep --trajectories 2 --levels 6 --threads 4 --master-seed 2026072201 --replicates 2 --init-pop 25 --max-steps 1440 --output-dir results/morris-independent-smoke-2x2
java -cp .:HAL-freq.jar:lwjgl.jar OnLatticeExample.MorrisSensitivitySweep --trajectories 2 --levels 6 --threads 4 --master-seed 2026072201 --replicates 2 --init-pop 25 --max-steps 1440 --output-dir results/morris-independent-smoke-2x2 --resume
```

Outcome: smoke completed 184 simulations; resume reused 184 complete checkpoints and ran 0 simulations.
New design seed was `-1836350308661608582`, different from primary design seed `6949820704716978156`.

Full run:

```bash
java -Xmx10g -cp .:HAL-freq.jar:lwjgl.jar OnLatticeExample.MorrisSensitivitySweep --trajectories 20 --levels 6 --threads 8 --master-seed 2026072201 --replicates 5 --init-pop 25 --max-steps 1440 --output-dir results/morris-independent-confirm-20x5
```

Outcome: exit 0. Expected and recorded simulations: 4,600. Status counts: 1,963 FINITE, 1,687 INVALID,
950 EXTINCT. QC PASS 11/11; 0 model-error status count. Design points: 920. Valid replicate-mean EEs:
108,260; lost replicate-mean EEs: 6,040. Runtime sum across simulations: 13.245 CPU-hours.

Analysis:

```bash
python3 analyze_morris.py --input-dir results/morris-independent-confirm-20x5 --output-dir results/morris-independent-confirm-20x5/figures --top-n 10
python3 morris_stage_analysis.py --input-dir results/morris-independent-confirm-20x5 --prefix INDEPENDENT_ --top-n 10
python3 morris_rank_stability.py --dir-a results/morris-primary-20 --dir-b results/morris-independent-confirm-20x5 --label-a primary20_1seed --label-b independent20_5seed --out-dir results/morris-independent-confirm-20x5
```

Old/new comparison: overall Spearman 0.823; top-5/10/15 overlap 0.60/0.90/0.80. Updated final
recommendation written to `results/morris-independent-confirm-20x5/INDEPENDENT_CONFIRMATORY_MORRIS_REPORT.md`
and `results/morris-independent-confirm-20x5/COMBINED_CALIBRATION_RECOMMENDATION.csv`.


