# Standalone Java ABC (no Python wrapper)

A pure-Java rejection ABC for the TNBC lung-metastasis model, meant to be
read and checked end to end. Two files:

- **`ExampleGrid.java`** — the agent-based model. New method **`RunHeadless(theta, initPop)`**
  runs one simulation *in process* (no files) and returns the 5 snapshots.
- **`ABCRejection.java`** — the ABC driver: priors, targets, distance, accept/reject.

## What to check (the ABC logic lives entirely in `ABCRejection.java`)

1. **Priors** (`NAME`, `LO`, `HI`): 12 parameters, sampled `Uniform(LO,HI)`.
   The order must match `ExampleGrid.RunHeadless`.
2. **Targets** (`TT`,`TS`,`TV`,`TW`,`TSC`): the calibration data — a statistic
   *type* at a snapshot *step*, its observed *value*, a *weight*, and a *scale*.
3. **`stat(...)`**: how each summary statistic is computed from the snapshot
   counts (fractions, or log10 fold-change vs the step-0 count).
4. **`distance(...)`**: weighted, scaled Euclidean over the targets; `+infinity`
   if any statistic is undefined (e.g. the tumour went extinct → auto-reject).
5. **`main(...)`**: the four ABC steps — propose → simulate → compare → accept.

## Build

Both files declare `package OnLatticeExample;`, so keep them in an
`OnLatticeExample/` folder and compile/run with the qualified name:


```
javac -cp HAL-freq.jar OnLatticeExample/ExampleGrid.java OnLatticeExample/ABCRejection.java
```
(If it can't find an `lwjgl` class at compile time, add it:
`javac -cp "HAL-freq.jar:lwjgl.jar" OnLatticeExample/ExampleGrid.java OnLatticeExample/ABCRejection.java`.)

## Run

Needs the two static coordinate files in the working directory:
`QuadratEndothelialOn.txt` and `QuadratStrOn.txt`.

```
java -cp .:HAL-freq.jar OnLatticeExample.ABCRejection [N] [epsilon] [quantile] [seed] [initPop]
```

- `N` — number of prior draws (default 1000)
- `epsilon` — fixed tolerance; use `-1` to instead keep the closest `quantile` (default -1)
- `quantile` — if epsilon<0, fraction of finite runs to keep (default 0.2)
- `seed` — RNG seed; the whole sweep is reproducible from it (default 12345)
- `initPop` — seeded tumour cells (default 25)

Examples:
```
# pilot: 1000 draws, keep best 20%, learn the distance scale
java -cp .:HAL-freq.jar OnLatticeExample.ABCRejection 1000 -1 0.2 12345 25

# fixed tolerance once you know the scale
java -cp .:HAL-freq.jar OnLatticeExample.ABCRejection 5000 1.0 0.2 42 25
```

## Output

- prints, per 25 runs, progress and seconds/run;
- prints the finite-run count and the distance min / median / chosen epsilon;
- writes **`posterior_java.csv`** — one row per accepted parameter set (the 12
  parameters + its distance). Each row is a sample from the approximate posterior.

## Notes / things a careful reader should question

- **Runtime is uneven.** A run whose tumour goes extinct finishes in seconds; one
  that fills the grid is much slower. So wall-time is dominated by the big-tumour
  draws — a 1000-run pilot can take a while.
- **`epsilon` is not set a priori.** With `epsilon=-1` the code learns it as the
  `quantile`-th distance of the finite runs. Look at `min`/`median` first, then
  decide whether to pin a fixed epsilon.
- **Fixed parameters** (not inferred here): `lambdaCAF=2.0`, `muStress=3.0`, all
  migration and stromal basal rates. They keep the model's class defaults.
- **Coordinate files** are re-read on every run inside `RunHeadless`; for speed you
  could read them once and cache. Left simple on purpose.
- This is deliberately the *simplest* ABC (rejection). SMC is more efficient but
  harder to read; rejection is the right thing to hand someone to audit.
