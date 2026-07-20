# Morris QC Report

Generated: 2026-07-17T14:55:55.940277Z

## Result

**PASS** (11 checks passed, 0 failed)

## Passed Checks

- PASS: Every audited parameter name is unique
- PASS: All screened transformations have finite endpoints
- PASS: All physical design values are finite and within registry bounds
- PASS: Every Morris step changes exactly one normalized coordinate
- PASS: Every Morris step changes exactly one physical parameter after mapping
- PASS: All neighboring points use matching trajectory/replicate simulation seeds (CRN)
- PASS: No simulation record was silently duplicated or dropped
- PASS: Deterministic fresh-grid rerun reproduces snapshots and event counts exactly
- PASS: Legacy 12-vector and named baseline interfaces are bit-identical at the requested horizon
- PASS: Parallel task code constructs a new ExampleGrid and a new HAL Rand for every run; no mutable model state is static
- PASS: All generated CSV files have consistent row widths

## Failed Checks

- None.

## Design/Run Inventory

- Screened parameters: 45
- Audited quantities: 68
- Design points: 46
- Run records: 46
- Physical duplicate points: 0
- Valid replicate-mean EEs: 1395
- Lost replicate-mean EEs: 1080

The HAL runtime may emit a Java final-field-reflection compatibility warning. That warning originates in the bundled HAL JAR; model instances and RNGs remain per task.
