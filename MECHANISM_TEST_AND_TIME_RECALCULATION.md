# Time Recalculation and Mechanism-Test Report

Audit date: 2026-07-13  
Confirmed mapping: **480 simulation steps per biological week**

This report adds two artifacts without changing simulation logic:

- Harness: `OnLatticeExample/MechanismTestHarness.java`
- Results: `mechanism_test_results.csv`

The harness compares low versus high values by running paired simulations with identical random seeds. The current CSV was generated with:

```bash
javac -cp .:HAL-freq.jar:lwjgl.jar OnLatticeExample/MechanismTestHarness.java
java -cp .:HAL-freq.jar:lwjgl.jar OnLatticeExample.MechanismTestHarness 1 25 1440 9001 mechanism_test_results.csv
```

The run used one paired seed as a smoke-test screen because a larger 3-seed sweep was slow for high-growth cases. The harness is configurable: `MechanismTestHarness [seedCount] [initPop] [maxStep] [baseSeed] [outCsv]`.

## Time Mapping

- `480 steps = 7 days`
- `1 step = 7 / 480 days = 0.0145833 days = 0.35 hours = 21 minutes`
- `1 day = 68.5714 steps`
- `1440 steps = 21 days`

For an event probability `p` per step:

- Event probability per day: `1 - (1 - p)^(480/7)`
- Event probability per week: `1 - (1 - p)^480`
- Expected waiting time: `1 / (p * 480/7)` days
- Death/deactivation half-life: `ln(0.5) / ln(1 - p) / (480/7)` days
- Pure-division doubling time: `ln(2) / ln(1 + p) / (480/7)` days

## Recalculated Time-Dependent Parameters

For ABC-varied parameters, the "p per step" column uses the best accepted posterior row in the existing `posterior_java.csv`, which is also the harness baseline. For fixed parameters, it uses the current Java default.

| Parameter | Kind | p per step | Event probability/day | Event probability/week | Expected wait days | Half/doubling days | Notes |
|---|---:|---:|---:|---:|---:|---:|---|
| `netN` | net growth | 0.0025998 | 0.163 | 0.713 | 5.61 | 3.89 | net-growth doubling |
| `divProbN` | division | 0.0211743 | 0.770 | ~1.000 | 0.689 | 0.482 | derived as `dieProbN + netN` |
| `dieProbN` | death | 0.0185745 | 0.724 | ~1.000 | 0.785 | 0.539 | half-life |
| `pOnMax` | switch/activation | 0.0344299 | 0.910 | ~1.000 | 0.424 | n/a | ceiling before spatial signal/clamp |
| `pOffMax` | switch/inactivation | 0.118803 | ~1.000 | ~1.000 | 0.123 | 0.0799 | half-life if fully exposed to off signal |
| `divProbP` | division | 0.0066214 | 0.366 | 0.959 | 2.20 | 1.53 | pure-division doubling |
| `dieProbP` | death | 0.00306108 | 0.190 | 0.770 | 4.76 | 3.30 | half-life |
| `activProbF` | activation | 0.0235918 | 0.805 | ~1.000 | 0.618 | n/a | baseline before hard-coded local boost |
| `divProbFP` | division | 0.0298057 | 0.874 | ~1.000 | 0.489 | 0.344 | pure-division doubling |
| `activProbM` | activation | 0.0418291 | 0.947 | ~1.000 | 0.349 | n/a | baseline before hard-coded daughter boost |
| `activProbE` | activation | 0.0594775 | 0.985 | ~1.000 | 0.245 | n/a | per active macrophage hazard component |
| `recruitBias` | biased migration choice | 0.0400000 | 0.939 | ~1.000 | 0.365 | n/a | conditional on inactive macrophage migration branch |
| `migrProbP` | migration | 0.100000 | 0.999 | ~1.000 | 0.146 | n/a | expected speed 5.7 um/h if 20 um/site |
| `migrProbN` | migration | 0.0100000 | 0.498 | 0.992 | 1.46 | n/a | expected speed 0.57 um/h if 20 um/site |
| `divProbFN` | division | 0.000000 | 0.000 | 0.000 | inf | inf | inactive fibroblast division currently zero |
| `dieProbFN` | death | 0.008000 | 0.423 | 0.979 | 1.82 | 1.26 | much faster than a 7-day turnover claim |
| `dieProbFP` | death | 0.012000 | 0.563 | 0.997 | 1.22 | 0.837 | activated CAF half-life |
| `migrProbF` | migration | 0.000000 | 0.000 | 0.000 | inf | n/a | no fibroblast migration branch; no effect in harness |
| `divProbMN` | division | 0.005000 | 0.291 | 0.910 | 2.92 | 2.03 | inactive macrophage pure-division doubling |
| `dieProbMN` | death | 0.005000 | 0.291 | 0.910 | 2.92 | 2.02 | inactive macrophage half-life |
| `divProbMP` | division | 0.015750 | 0.663 | ~1.000 | 0.926 | 0.647 | activated macrophage pure-division doubling |
| `dieProbMP` | death | 0.015000 | 0.645 | 0.999 | 0.972 | 0.669 | activated macrophage half-life |
| `activProbMP` | unused activation | 0.020000 | 0.750 | ~1.000 | 0.729 | n/a | declared but unused; no effect in harness |
| `migrProbM` | migration | 0.800000 | ~1.000 | ~1.000 | 0.0182 | n/a | expected speed 46 um/h if 20 um/site |
| `divProbEN` | division | 0.000000 | 0.000 | 0.000 | inf | inf | inactive EC division has no current branch; no effect |
| `dieProbEN` | death | 0.005000 | 0.291 | 0.910 | 2.92 | 2.02 | inactive EC half-life |
| `divProbEP` | division | 0.008700 | 0.451 | 0.985 | 1.68 | 1.17 | pure-division doubling |
| `dieProbEP` | death | 0.008000 | 0.423 | 0.979 | 1.82 | 1.26 | activated EC half-life |
| `deactProbE` | deactivation | 0.010000 | 0.498 | 0.992 | 1.46 | 1.01 | only fires when no active macrophage is nearby |
| `migrProbE` | migration | 0.000000 | 0.000 | 0.000 | inf | n/a | no EC migration branch; no effect in harness |
| `divProbL` | division | 0.002000 | 0.128 | 0.617 | 7.29 | 5.06 | lung division has no current branch; no effect |
| `dieProbL` | death | 0.002000 | 0.128 | 0.617 | 7.29 | 5.05 | lung death half-life |

## Mechanism-Test Results

Changed means at least one primary output crossed the harness threshold:

- `abs(delta tumorLog or fibroLog) >= 0.10`
- `abs(delta JNK/EC/mac fraction) >= 0.05`
- or at least 20% relative change in tumor, fibroblast, EC, or macrophage count

Because this is a one-seed screen, "changed" means "detectable in this paired smoke test", not a statistical sensitivity conclusion.

| Parameter | Changed? | Main observed deltas, high-low | Expected biological output |
|---|---|---|---|
| `netN` | true | fibro cells +129; tumor cells +64; mac cells +53 | tumorLog |
| `dieProbN` | true | fibro cells +651; mac cells +388; tumor cells -268 | tumorLog/JNK fraction |
| `pOnMax` | true | tumor cells +225; fibro cells -81; mac cells -55 | JNK fraction |
| `pOffMax` | true | tumor cells -563; fibro cells +106; mac cells -28 | JNK fraction |
| `divProbP` | true | tumor cells +4418; fibro cells -1568; mac cells -278 | tumorLog/JNK fraction |
| `dieProbP` | true | tumor cells -568; mac cells +81; fibro cells -46 | tumorLog/JNK fraction |
| `cafDivBoost` | true | mac cells -219; fibro cells +124; tumor cells +85 | tumorLog/JNK rim |
| `ecSurvival` | true | mac cells -88; tumor cells -7; fibro cells -1 | tumorLog |
| `activProbF` | true | fibro cells +2344; tumor cells -456; mac cells -189 | fibroLog/tumorLog |
| `divProbFP` | true | fibro cells +3931; mac cells -265; tumor cells -232 | fibroLog |
| `activProbM` | true | fibro cells +109; tumor cells -93; mac cells -91 | macrophage/EC fractions |
| `activProbE` | true | fibro cells -421; tumor cells +340; mac cells +140 | EC fraction/tumorLog |
| `recruitBias` | true | fibro cells -701; tumor cells -237; mac cells -224 | macrophage/EC fractions |
| `lambdaCAF` | true | fibro cells +274; mac cells -203; tumor cells +123 | JNK fraction/tumorLog |
| `stressStrength` | true | fibro cells +1521; tumor cells +255; mac cells +129 | JNK fraction |
| `lambdaStress` | true | fibro cells -516; tumor cells +371; mac cells +370 | JNK fraction |
| `migrProbP` | true | tumor cells +813; fibro cells +240; mac cells -143 | tumorLog/JNK fraction |
| `migrProbN` | true | fibro cells +538; mac cells +338; tumor cells -206 | tumorLog/JNK fraction |
| `divProbFN` | true | fibro cells +1014; mac cells -415; tumor cells -182 | fibroLog |
| `dieProbFN` | true | fibro cells -1369; mac cells +519; tumor cells +95 | fibroLog |
| `dieProbFP` | true | fibro cells -3908; mac cells +604; tumor cells +186 | fibroLog |
| `migrProbF` | false | none | none currently |
| `divProbMN` | true | mac cells +7850; fibro cells -2534; tumor cells -68 | macrophage total/fraction |
| `dieProbMN` | true | mac cells -9023; fibro cells +2229; tumor cells +96 | macrophage total/fraction |
| `divProbMP` | true | mac cells +7771; fibro cells -2232; tumor cells -140 | macrophage total/fraction |
| `dieProbMP` | true | mac cells -9194; fibro cells +2737; tumor cells +52 | macrophage total/fraction |
| `activProbMP` | false | none | none currently |
| `migrProbM` | true | fibro cells -296; mac cells +109; tumor cells +37 | macrophage/EC fractions |
| `divProbEN` | false | none | none currently |
| `dieProbEN` | true | tumor cells +233; EC cells -61; mac cells +57 | EC total/fraction |
| `divProbEP` | true | fibro cells -914; tumor cells +438; EC cells +271 | EC total/fraction |
| `dieProbEP` | true | mac cells +332; tumor cells +149; EC cells -82 | EC total/fraction |
| `deactProbE` | true | mac cells -144; tumor cells -95; fibro cells -38 | EC fraction |
| `migrProbE` | false | none | none currently |
| `divProbL` | false | none | none currently |
| `dieProbL` | true | mac cells +161; fibro cells +73; tumor cells +21 | JNK fraction/tumorLog |
| `clusterRadius` | true | fibro cells +297; tumor cells +68; mac cells +54 | tumorLog/JNK fraction |
| `initPop` | true | fibro cells +256; tumor cells -24; mac cells +11 | tumorLog/establishment |

## Interpretation

The 480-steps/week conversion makes many current probabilities biologically aggressive. Several "turnover" rates described as weekly-scale in comments now imply half-lives of about 0.7-2.0 days. This applies especially to fixed fibroblast, macrophage, and endothelial death parameters.

The mechanism test confirmed that almost all active parameters can change expected model outputs. The only low/high cases with exactly no output change in the current implementation were:

- `migrProbF`: fibroblast migration is declared but there is no fibroblast migration branch.
- `activProbMP`: declared obsolete and not used by macrophage logic.
- `divProbEN`: inactive EC division is declared but inactive ECs do not divide.
- `migrProbE`: EC migration is declared but there is no EC migration branch.
- `divProbL`: lung division is declared but lung cells only die in `lungCells()`.

The highest-leverage output movers in this one-seed screen were:

- Tumor burden: `divProbP`, `pOffMax`, `migrProbP`, `activProbF`, `dieProbP`, `dieProbN`.
- Fibroblast burden: `divProbFP`, `dieProbFP`, `dieProbMP`, `divProbMN`, `dieProbMN`, `activProbF`.
- Macrophage burden: `dieProbMP`, `dieProbMN`, `divProbMN`, `divProbMP`.
- JNK fraction: `lambdaCAF`, `migrProbP`, `pOffMax`, `stressStrength`, `dieProbMP`, `lambdaStress`.
- EC activation/fraction: `pOffMax`, `activProbE`, `lambdaCAF`, `divProbMP`, `migrProbM`, `deactProbE`.

Next recommended run for a more stable result:

```bash
java -cp .:HAL-freq.jar:lwjgl.jar OnLatticeExample.MechanismTestHarness 5 25 1440 9001 mechanism_test_results_5seeds.csv
```

That should be treated as a sensitivity smoke test, not a replacement for formal global sensitivity analysis.
