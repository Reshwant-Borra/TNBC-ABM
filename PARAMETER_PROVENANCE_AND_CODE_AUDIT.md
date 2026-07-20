# Parameter Provenance and Code Audit for TNBC Lung-Metastasis ABM

Audit date: 2026-07-13  
Repository: `/Users/80034573/Downloads/abc-run`  
Scope: `OnLatticeExample/ExampleGrid.java`, `OnLatticeExample/ABCRejection.java`, `README.md`, `posterior_java.csv`, `QuadratEndothelialOn.txt`, `QuadratStrOn.txt`, bundled PDF/DOCX/PPTX literature and strategy files.

Primary sources inspected in the repository:

- `OnLatticeExample/ExampleGrid.java`
- `OnLatticeExample/ABCRejection.java`
- `README.md`
- `posterior_java.csv`
- `QuadratEndothelialOn.txt` and `QuadratStrOn.txt`
- `Research Strategy_R21.docx`
- `MetsTNBC.pptx`
- `EMBO Mol Med - 2018 - Insua-Rodriguez - Stress signaling in breast cancer cells induces matrix components that promote (1).pdf`
- `s41467-020-15188-x (1).pdf`
- `s43018-022-00353-6.pdf`

No `ABC_TNBC_parameter_reference.md`, `ABC_TNBC_parameter_reference.pdf`, YAML config, plotting script, or posterior-analysis script was present in this repository. Several Java comments refer to `abc_config.yaml` and an SMC project, but no such file exists here.

External primary-source pages used only to verify paper identity and high-level figure context where the local PDFs were not text-extractable in this environment:

- Insua-Rodriguez et al. 2018, *EMBO Molecular Medicine*, "Stress signaling in breast cancer cells induces matrix components that promote chemoresistant metastasis." DOI page: https://doi.org/10.15252/emmm.201809003
- Pein et al. 2020, *Nature Communications*, "Metastasis-initiating cells induce and exploit a fibroblast niche to fuel malignant colonization of the lungs." Nature article: https://www.nature.com/articles/s41467-020-15188-x
- Hongu et al. 2022, *Nature Cancer*, "Perivascular tenascin C triggers sequential activation of macrophages and endothelial cells to generate a pro-metastatic vascular niche in the lungs." Nature article: https://www.nature.com/articles/s43018-022-00353-6

## 1. Executive Summary

This model currently uses 12 ABC-varied parameters, 26 fixed declared biological parameters, and many additional hard-coded constants for spatial scale, initialization, event ordering, chemotherapy, target construction, and ABC acceptance. Counting distinct simulation-controlling values and calibration constants, I found 72 parameters/constants that affect, or are intended to affect, model behavior.

The strongest conclusion is that most per-step probabilities are not directly measured from literature. The papers support qualitative mechanisms and several calibration targets: JNK/c-Jun state fractions, fibroblast niche expansion, EC niche emergence, macrophage/perivascular association, and chemotherapy-associated JNK activation. They generally do not directly provide the per-step division, death, migration, activation, switching, survival, and interaction-strength probabilities used in the Java code.

The most serious risk is the time-step inconsistency. Code comments say 480 steps is one week, so one step is about 21 minutes. Other comments say one step is 30-60 minutes. Turnover probabilities labelled "weeks" or "7 days" often convert to half-lives of about 1-5 simulated days under the 480-steps/week mapping. This makes several fixed death probabilities biologically questionable and likely confounded with inferred division and activation rates.

The second major risk is code/document inconsistency. `ABCRejection.java` samples `netN` as parameter 0, but `ExampleGrid.main(...)` reads `divProbN` directly from older parameter files. `ExampleGrid` comments still list older broad priors that disagree with current ABC priors. README says `muStress=3.0`, but the Java code has `stressStrength=1.5` and no `muStress`. `RunHeadless` comments say `lambdaStress=2.5`, but code sets `lambdaStress=2.0`.

The third major risk is that some declared fixed parameters do not actually influence the model: `divProbL`, `divProbEN`, `migrProbE`, `migrProbF`, `activProbMP`, and the tumor/EC per-cell `activProb` field are unused or effectively unused in current logic. These should not be treated as biologically fixed until their implementation role is corrected or removed.

## 2. Major Inconsistencies and Risks

1. **Time-step mismatch**
   - Code targets define week 1/2/3 as steps 480/960/1440 (`ABCRejection.java:83-88`, `ExampleGrid.java:901-907`), implying 480 steps/week and 68.57 steps/day.
   - Tumor migration comments say one step is approximately 30-60 minutes (`ExampleGrid.java:621`). If true, one week would be 168-336 steps, not 480.
   - All per-step conversions depend on this, including division, death, migration, and targets.

2. **Current ABC priors disagree with in-code parameter comments**
   - `ExampleGrid.java:553-585` lists old priors such as `divProbP U(0.02,0.18)` and `dieProbP U(0.005,0.06)`.
   - `ABCRejection.java:45-51` currently samples `divProbP U(0.005,0.03)` and `dieProbP U(0.001,0.004)`, and uses `netN` rather than directly sampling `divProbN`.

3. **Parameter order and semantics differ between `RunHeadless` and `main`**
   - `RunHeadless`: theta[0] is `netN`, and `divProbN = dieProbN + netN` (`ExampleGrid.java:816-820`).
   - `main`: `pT[0]` is read as `divProbN` directly (`ExampleGrid.java:916-922`).
   - Old file-driven runs and current ABC runs are not semantically equivalent.

4. **README documents obsolete or nonexistent fixed parameters**
   - README states fixed `lambdaCAF=2.0`, `muStress=3.0`, migration and stromal basal rates (`README.md:73-74`).
   - Java has `stressStrength=1.5` and `lambdaStress=2.0`; no `muStress` exists (`ExampleGrid.java:610-615`).

5. **Snapshot count mismatch**
   - README and `RunHeadless` comments say 5 snapshots (`README.md:6-7`, `ExampleGrid.java:793-794`).
   - `ABCRejection.distance(...)` requires exactly 4 snapshots (`ABCRejection.java:88`, `ABCRejection.java:114-116`).
   - `RunHeadless` defines `{0,480,960,1440,2100}` but default `maxStep=1440` returns only 4 snapshots because step 2100 is never reached (`ExampleGrid.java:808-823`, `ExampleGrid.java:863-869`).

6. **Chemotherapy branch is effectively dead during calibration**
   - `RunHeadless(th, initPop)` defaults to `maxStep=1440` (`ExampleGrid.java:808-810`).
   - Chemotherapy only starts after `i > 2898` (`ExampleGrid.java:865`, `ExampleGrid.java:1056-1057`).
   - Therefore chemo multipliers do not affect ABC calibration targets.

7. **Several fixed parameters are unused or blocked**
   - `divProbL` is declared and passed to lung cells, but `lungCells()` only implements death (`ExampleGrid.java:524-525`, `ExampleGrid.java:661-663`).
   - `divProbEN` is declared, but inactive ECs do not have a division branch (`ExampleGrid.java:444-457`, `ExampleGrid.java:649`).
   - `migrProbE` and `migrProbF` are declared but EC/fibroblast methods do not implement migration (`ExampleGrid.java:416-459`, `ExampleGrid.java:468-516`, `ExampleGrid.java:630`, `ExampleGrid.java:657`).
   - `activProbMP` is explicitly labelled unused (`ExampleGrid.java:644`).

8. **Several hard-coded interaction boosts are undocumented as parameters**
   - Macrophage daughter activation boost `+0.008 * tumorCount` (`ExampleGrid.java:377`).
   - Activated EC daughter division boost `+0.001 * macroActCount` (`ExampleGrid.java:434`).
   - CAF activation/division boost `+0.02 * min(tumorFCount,10)` (`ExampleGrid.java:509-513`).
   - These may be high-impact and should be considered parameters.

## 3. Master Parameter Table

Legend:

- Status: **inferred** = sampled in current `ABCRejection`; **fixed** = declared constant/default; **derived** = calculated from another parameter; **pinned** = fixed in current run but documented as candidate for inference or old file input.
- Match: whether code agrees with the current in-repository documentation/literature claim.
- Evidence categories are intentionally conservative. "Literature target" means the paper supports a calibration output, not necessarily a per-step probability.

| # | Parameter name in code | Biological/simulation meaning | Cell type/pathway | Current code value | Status | Prior/tested range | Exact code location | Source category | Literature source | Figure/table/section | Experimental system | Original units | Conversion to per-step ABM probability | Confidence | Code/lit/doc match | Recommendation | Confounding/output notes |
|---:|---|---|---|---:|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 1 | `netN` | JNK- tumor net growth per step; used to derive `divProbN` | Tumor JNK- | varied | inferred | U(0.0015,0.005) | `ABCRejection.java:45-51`; applied `ExampleGrid.java:816-817` | estimated from literature target | IR18 / MDA231-LM2 growth comments; target source unclear | code cites growth curve, no exact figure in repo | mouse xenograft/in vivo, lung metastasis implied | net growth/fold-change | `divProbN = dieProbN + netN`; expected net approx `div-die` | low | mismatch with `ExampleGrid.main`, which reads `divProbN` directly | keep inferred, narrow only after time-step fixed | Strongly confounded with `dieProbN`, EC survival, CAF boost, tumor carrying capacity |
| 2 | `dieProbN` | JNK- tumor death probability | Tumor JNK- | varied | inferred | U(0.008,0.025) | `ABCRejection.java:49-51`; `ExampleGrid.java:816` | estimated/model assumption | IR18 comments only | none verified | MDA231-LM2; in vivo/in vitro unclear | apoptosis/death rate | none documented; per-step probability sampled | low | old comment says U(0.005,0.05) at `ExampleGrid.java:565-566` | inferred with wide range after BrdU/caspase data | Drives tumor growth, extinction, JNK- dominance; confounded with `netN` |
| 3 | `divProbN` | JNK- tumor division probability | Tumor JNK- | derived in ABC; directly read in old main | derived/inferred | derived as `dieProbN+netN`; old comment U(0.01,0.12) | `ExampleGrid.java:563`; `ExampleGrid.java:816-817`; old file read `ExampleGrid.java:917` | derived from inferred values | IR18 growth comment | none verified | MDA231-LM2 | division probability | `div = death + net`; no direct conversion shown | low | major mismatch between ABC and file-driven mode | use derived in all entry points or document separate modes | Main determinant of tumor fold-change and grid saturation |
| 4 | `pOnMax` | JNK- to JNK+ switching ceiling near CAF/stress | JNK switching | varied | inferred | U(0.01,0.10) | `ABCRejection.java:45-51`; `ExampleGrid.java:818`; used `ExampleGrid.java:197-198` | model assumption constrained by JNK fractions | IR18 supports c-Jun/JNK dynamics qualitatively | IR18 Fig. 1E/F referenced in code | TNBC metastases, mouse/patient mixed | fraction JNK+ | `pOn=pOnMax*P`, clamped to event ladder | low | old comment says U(0.01,0.20) at `ExampleGrid.java:600`; default 0.05 | inferred; range should remain wide | Confounded with `pOffMax`, `lambdaCAF`, `lambdaStress`, `stressStrength`, initial JNK fraction |
| 5 | `pOffMax` | JNK+ to JNK- switching ceiling away from niche/stress | JNK switching | varied | inferred | U(0.01,0.20) | `ABCRejection.java:45-51`; `ExampleGrid.java:818`; used `ExampleGrid.java:169-170` | model assumption constrained by JNK fractions | IR18 qualitative | IR18 Fig. 1E/F referenced | TNBC metastases | fraction JNK+ over time | `pOff=pOffMax*(1-P)`, clamped to event ladder | low | current ABC matches comment for upper bound | inferred; keep broad | Confounded with stress and CAF fields; clamping can block effect when death/division high |
| 6 | `divProbP` | JNK+ tumor division probability | Tumor JNK+ | varied | inferred | U(0.005,0.03) | `ABCRejection.java:45-51`; `ExampleGrid.java:819`; used `ExampleGrid.java:168` | estimated from growth target/model assumption | IR18 comments cite doubling about 24 h | `ExampleGrid.java:553` cites IR18 Fig. 1H | MDA231-LM2 in vivo stated in code | doubling time | no documented formula; CAF boost multiplies by `1+cafDivBoost*S` | low | old comment says U(0.02,0.18), much higher | inferred; revisit after timestep fix | Confounded with `cafDivBoost`; division blocked by empty-neighbor availability |
| 7 | `dieProbP` | JNK+ tumor death probability | Tumor JNK+ | varied | inferred | U(0.001,0.004) | `ABCRejection.java:49-51`; `ExampleGrid.java:819` | model assumption/estimated | IR18 qualitative survival/stress | none verified | MDA231-LM2 | apoptosis/death | sampled per-step probability | low | old comment says U(0.005,0.06) | inferred; currently very narrow and low | Confounded with EC survival and chemo death multiplier |
| 8 | `cafDivBoost` | Multiplicative boost to JNK+ division near activated CAF | CAF-tumor loop | varied | inferred | U(0,1.0) | `ABCRejection.java:45-51`; `ExampleGrid.java:819`; used `ExampleGrid.java:168`, `ExampleGrid.java:241` | model assumption | P20/IR18 support fibroblast niche/JNK crosstalk qualitatively | P20 Fig. 1/6 comments; IR18 matrix components | mouse lung metastasis; MDA231-LM2 | qualitative effect | `divEff=divProb*(1+cafDivBoost*S)` | low | old comment says U(0,3) at `ExampleGrid.java:600-601` | inferred; consider wider sensitivity | Confounded with `divProbP`, CAF density, `lambdaCAF`; affects tumor growth and JNK+ rim |
| 9 | `ecSurvival` | Death reduction per nearby activated EC | EC-tumor survival niche | varied | inferred | U(0,0.3) | `ABCRejection.java:45-51`; `ExampleGrid.java:819`; used `ExampleGrid.java:165`, `ExampleGrid.java:240`, `ExampleGrid.java:270` | model assumption from qualitative biology | H22 supports vascular niche survival factors qualitatively | H22 Fig. 1/6/7 comments | mouse lung metastasis | qualitative survival support | `dieEff=dieProb*exp(-ecSurvival*activeEC_count)` | low | code and ABC agree | inferred; keep wide | Confounded with EC activation, EC count, tumor death probabilities |
| 10 | `activProbF` | Fibroblast activation probability per JNK+ tumor signal | Fibroblast/CAF | varied | inferred | U(0.001,0.05) | `ABCRejection.java:45-51`; `ExampleGrid.java:820`; used `ExampleGrid.java:500`, `ExampleGrid.java:512-513` | inferred from literature target | P20 supports fibroblast expansion and CXCL9/10 niche | P20 Fig. 1f and Fig. 6d cited in code | mouse lung metastasis; MDA231-LM2 | fibroblast fold-change, signaling | sampled per-step; boosted by `+0.02*fB` after activation | medium for target, low for probability | code priors match line comment | inferred; use broad range | Confounded with `divProbFP`, hard-coded `0.02`, initial fibroblast coordinates |
| 11 | `divProbFP` | Activated CAF division probability | Fibroblast/CAF | varied | inferred | U(0.018,0.038) | `ABCRejection.java:49-51`; `ExampleGrid.java:820`; used `ExampleGrid.java:487-488` | inferred from literature target | P20 fibroblast expansion | P20 Fig. 1f cited | mouse lung metastasis | fibroblast fold-change | no direct formula in code; target adjusted 3D to 2D | medium for target, low for per-cell value | old comment says U(0.01,0.15) | inferred; current range narrow | Confounded with `activProbF`, `dieProbFP`, hard-coded CAF boost, empty space |
| 12 | `activProbM` | Macrophage activation probability from JNK+ tumor contact | Macrophage/TNC/TLR4 | varied | inferred | U(0.02,0.08) | `ABCRejection.java:49-51`; `ExampleGrid.java:820`; used `ExampleGrid.java:377`, `ExampleGrid.java:401-405` | inferred from proxy target | H22 supports macrophage niche/perivascular role | H22 Fig. 6e/f cited | mouse lung metastasis | perivascular fraction / macrophage count | sampled per-step; daughter boost `+0.008*tumorCount` | medium for target, low for probability | old comment says U(0.005,0.08) | inferred; keep wide | Confounded with migration, EC layout, recruitment, activated macrophage proliferation |
| 13 | `activProbE` | EC activation hazard per activated macrophage nearby | Endothelial vascular niche | varied | inferred | U(0.005,0.08) | `ABCRejection.java:49-51`; `ExampleGrid.java:820`; used `ExampleGrid.java:448-449` | inferred from literature target | H22 EC niche data | H22 Fig. 1b/7 cited | mouse lung metastasis | EC+ nodule fraction/expression | `pAct=1-(1-activProbE)^k`, capped by survival of inactive EC | medium for target, low for probability | code priors match line comment | inferred; keep broad | Confounded with macrophage activation/migration and EC death/deactivation |
| 14 | `lambdaCAF` | CAF niche exponential decay length | CAF-tumor spatial loop | 2.0 sites | fixed/pinned | old file mode can read pT[8]; current ABC fixed | declaration `ExampleGrid.java:610`; old read `ExampleGrid.java:927`; used `ExampleGrid.java:110-123` | estimated from image/model assumption | code says "fixed from image"; source unspecified | no verifiable figure | likely histology image; species/cell line unclear | grid sites/cell diameters | `S=exp(-d/lambdaCAF)`; search radius `3*lambda+1` | low | README matches 2.0; RunHeadless fixed | reopen for sensitivity | Strongly controls rim width, JNK switching, CAF effect |
| 15 | `stressStrength` | Gain on hostile-environment stress field | JNK stress switching | 1.5 | fixed | none | `ExampleGrid.java:611-613`; used `ExampleGrid.java:150` | model assumption | no direct source | none | model design | dimensionless | `stress=min(1, stressStrength*exp(-d/lambdaStress))` | low | README says `muStress=3.0`; mismatch | wide sensitivity/investigate | Confounded with `lambdaStress`, `pOnMax`, `pOffMax`, initial JNK+ fraction |
| 16 | `lambdaStress` | Hostile-environment stress decay length | JNK stress switching | 2.0 sites | fixed/pinned | old file mode can read pT[9]; current ABC fixed | `ExampleGrid.java:614-616`; old read `ExampleGrid.java:928`; used `ExampleGrid.java:136-150` | model assumption | no direct source | none | model design | grid sites/cell diameters | `exp(-d/lambdaStress)`; search radius `3*lambda+1` | low | `RunHeadless` comment says 2.5 at `ExampleGrid.java:797` | wide sensitivity | Controls small-nodule JNK maintenance and early growth |
| 17 | `recruitBias` | Probability inactive macrophage step is biased toward tumor centroid | Macrophage recruitment/chemotaxis | 0.04 | fixed | none | `ExampleGrid.java:607-608`; used `ExampleGrid.java:384-395` | model assumption | H22 qualitative recruitment/perivascular localization only | H22 Fig. 6f cited in nearby comments | mouse lung metastasis | qualitative macrophage recruitment | per migration event probability | low | no doc range | sensitivity range needed | Confounded with `migrProbM`, tumor centroid, grid crowding |
| 18 | `migrProbP` | JNK+ tumor migration probability | Tumor migration/invasion | 0.10 | fixed | none | `ExampleGrid.java:622`; used in tumor init `ExampleGrid.java:857-858` and migration branches | estimated/transferred from in vitro literature | IR18 Fig. 3M; migration speed comment | IR18 invasion/Matrigel | in vitro invasion; MDA231-LM2 likely | invasion ratio; um/h comment | Assumes grid 20 um and step 21-60 min; no explicit formula | low | qualitative 10x ratio agrees with `migrProbN`; absolute value unverified | sensitivity, likely fixed ratio but varied scale | Affects spatial spread, access to niche, empty-space conflicts |
| 19 | `migrProbN` | JNK- tumor migration probability | Tumor migration/invasion | 0.01 | fixed | none | `ExampleGrid.java:623` | estimated/transferred | IR18 Fig. 3M | in vitro invasion | in vitro | relative invasion | set to one-tenth `migrProbP` | low | ratio matches comment | sensitivity | Confounded with proliferation and cluster geometry |
| 20 | `divProbFN` | Inactive fibroblast division | Fibroblast | 0.0 | fixed | none | `ExampleGrid.java:627`; used branch `ExampleGrid.java:494-500` | model assumption from P20 qualitative | P20 says activated fibroblast niche expands | P20 Fig. 1f cited nearby | mouse lung metastasis | none | zero by assumption | medium | code/doc agree | probably fixed at 0 but test | If nonzero, affects baseline fibroblast pool |
| 21 | `dieProbFN` | Inactive fibroblast death | Fibroblast | 0.008 | fixed | none | `ExampleGrid.java:628`; used `ExampleGrid.java:492-493` | transferred/general literature, unverified | `[LIT]` only | none | lung fibroblast turnover | half-life/turnover | If 480 steps/week, half-life = ln(2)/0.008 = 86.6 steps = 1.26 days, not 7 days | low | mismatch with "turnover ~7 days" | investigate; likely lower range around 0.0014 if 7-day half-life | Confounded with CAF expansion targets |
| 22 | `dieProbFP` | Activated CAF death | Fibroblast/CAF | 0.012 | fixed | none | `ExampleGrid.java:629`; used `ExampleGrid.java:487-488`, `ExampleGrid.java:507` | transferred/general literature, unverified | `[LIT]` only | none | CAF turnover unknown | death/half-life | half-life 57.8 steps = 0.84 days at 480 steps/week | low | no direct support | sensitivity/investigate | Confounds with `divProbFP` and fibroblast target |
| 23 | `migrProbF` | Fibroblast migration | Fibroblast | 0.0 | fixed but unused | none | `ExampleGrid.java:630`; passed `ExampleGrid.java:835`, `ExampleGrid.java:959` | model assumption/implementation convenience | none | none | model design | probability | no fibroblast migration branch exists | medium | code/doc say no migration | fixed/remove or implement if needed | Currently has no effect |
| 24 | `divProbMN` | Inactive macrophage division | Macrophage | 0.005 | fixed | none | `ExampleGrid.java:634`; used `ExampleGrid.java:372-379` | transferred/general literature, unverified | `[LIT]`; Research Strategy says macrophage death zero in future design | strategy SA1 text | primate/mouse literature proposed, not current value | half-life/turnover | no conversion shown; with 480 steps/week net balanced by death | low | literature claim "1-2 week half-life" does not support 0.005 | sensitivity/investigate | Confounded with resting pool and activation fraction |
| 25 | `dieProbMN` | Inactive macrophage death | Macrophage | 0.005 | fixed | none | `ExampleGrid.java:635-638`; used `ExampleGrid.java:370-371` | model assumption/homeostasis | `[LIT]`; Research Strategy says macrophage zero death based on turnover/no apoptosis | strategy SA1 | homeostatic macrophages | death/half-life | half-life 138.6 steps = 2.02 days at 480 steps/week | low | mismatch with "1-2 week half-life" and strategy zero-death statement | investigate; wide sensitivity | Strongly affects macrophage availability and EC activation |
| 26 | `divProbMP` | Activated macrophage division | Macrophage | 0.01575 | derived/fixed | none | `ExampleGrid.java:639-642`; used `ExampleGrid.java:322-329` | implementation assumption | H22 only supports increase 2-3x | H22 Fig. 6f cited | mouse lung metastasis | macrophage increase/fraction | `1.05*dieProbMP`; extra 5% for space-limited division | low | not directly supported | sensitivity/investigate | Crowding blocks division; diagnostic tracks `macDivFail` |
| 27 | `dieProbMP` | Activated macrophage death | Macrophage | 0.015 | fixed | none | `ExampleGrid.java:643`; used `ExampleGrid.java:320-321` | transferred/general literature, unverified | `[LIT]` only | none | tissue macrophage turnover | death/half-life | half-life 46.2 steps = 0.67 days at 480 steps/week | low | mismatch with 1-2 week comment | investigate | Confounds with `divProbMP`, activation, macrophage fraction |
| 28 | `activProbMP` | Former activated macrophage deactivation/activation reference | Macrophage | 0.020 | fixed unused | none | `ExampleGrid.java:644` | obsolete/implementation convenience | none | none | none | probability | not used; activation irreversible at `ExampleGrid.java:366-367` | high for unused status | code says unused | remove or document obsolete | No output effect |
| 29 | `migrProbM` | Macrophage migration probability | Macrophage | 0.8 | fixed | none | `ExampleGrid.java:645`; used `ExampleGrid.java:333`, `ExampleGrid.java:381` | transferred from related literature/assumption | Pixley 2012 cited in comment, not in repo | no figure | in vitro/in vivo macrophage motility, not TNBC lung metastasis | um/min | probability chosen near lattice maximum; with 20 um and 21 min gives about 0.76 um/min if every move succeeds | medium for order of magnitude, low context match | plausible if timestep is 21 min; not if 30-60 min | sensitivity but likely narrow | Strongly affects perivascular localization and activation encounter rates |
| 30 | `divProbEN` | Inactive EC division | Endothelial | 0.0 | fixed unused | none | `ExampleGrid.java:649`; inactive EC logic `ExampleGrid.java:444-457` | model assumption from H22 | H22 Fig. 1d cited | mouse lung metastasis | EC count correlation | zero assumption | no inactive division branch exists | medium | code/doc agree but unused | fixed or remove | No effect unless EC method is changed |
| 31 | `dieProbEN` | Inactive EC death | Endothelial | 0.005 | fixed | none | `ExampleGrid.java:650`; used `ExampleGrid.java:451-452` | transferred/general literature, unverified | `[LIT]` only | none | quiescent EC turnover | death/half-life | half-life 138.6 steps = 2.02 days at 480 steps/week | low | mismatch with "turnover ~weeks" | investigate | Confounds with EC activation target |
| 32 | `divProbEP` | Activated EC division | Endothelial | 0.0087 | estimated/fixed | none | `ExampleGrid.java:651-652`; used `ExampleGrid.java:428-436` | estimated from figure/fold-change | H22 Fig. 1d comment: EC ~2x week1 to week3 | H22 Fig. 1d cited | mouse lung metastasis | two-fold over 960 steps | net = ln(2)/960 = 0.000722; `div=dieProbEP+net = 0.00872` | medium for arithmetic, low for source extraction | formula agrees internally if source is correct | fixed/narrow sensitivity | Confounded with `dieProbEP`, hard-coded `+0.001*macActCount` |
| 33 | `dieProbEP` | Activated EC death | Endothelial | 0.008 | fixed | none | `ExampleGrid.java:653`; used `ExampleGrid.java:426-427` | transferred/general literature, unverified | `[LIT]` only | none | activated EC turnover | death/half-life | half-life 86.6 steps = 1.26 days at 480 steps/week | low | no direct support | investigate | Confounds with `divProbEP` and `deactProbE` |
| 34 | `deactProbE` | Activated EC to inactive probability when no active macrophage nearby | Endothelial | 0.01 | fixed | none | `ExampleGrid.java:654-656`; used `ExampleGrid.java:438-442` | model assumption | no direct source | none | model design | probability | signal-gated per-step deactivation | low | no documented range | sensitivity | Controls EC fraction; confounded with macrophage activation |
| 35 | `migrProbE` | EC migration | Endothelial | 0.0 | fixed unused | none | `ExampleGrid.java:657`; passed `ExampleGrid.java:830`, `ExampleGrid.java:948-949` | model assumption/implementation convenience | none | none | model design | probability | no EC migration branch | high for unused status | code/doc agree | fixed/remove | No output effect |
| 36 | `divProbL` | Lung cell division | Lung/background | 0.002 | fixed but unused | none | `ExampleGrid.java:661-662`; passed `ExampleGrid.java:842`, `ExampleGrid.java:979`; `lungCells()` line `ExampleGrid.java:524-525` | model assumption/bug risk | `[LIT]` only | none | alveolar turnover | division probability | not implemented; lung cells only die | high for unused status | comment says homeostatic net 0, but code has no division | fix later; not a current effective parameter | Intended to preserve stress field but ineffective |
| 37 | `dieProbL` | Lung cell death | Lung/background | 0.002 | fixed | none | `ExampleGrid.java:663`; used `ExampleGrid.java:524-525` | transferred/general literature, unverified | `[LIT]` only | none | alveolar epithelial turnover | death/half-life | half-life 346.6 steps = 5.05 days at 480 steps/week | low | mismatch with "turnover ~weeks" | investigate | Declining lung cells reduce stress field over time |
| 38 | `clusterRadius` | Tumor seeding radius | Initial tumor geometry | 4 sites | fixed | none | `ExampleGrid.java:667`; used `ExampleGrid.java:845`, `ExampleGrid.java:851-852`, `ExampleGrid.java:991`, `ExampleGrid.java:1004-1008` | model assumption | H22/P20 qualitative clustered/perivascular seeding | H22 Fig. 1a, P20 Fig. 1c cited in code | mouse lung metastasis images | cell diameters | circular rejection inside radius 4 | medium qualitative, low numeric | no direct numeric support | sensitivity | Controls early density, available space, initial stress |
| 39 | `InitPop` / `initPop` | Initial tumor cell count | Initialization | ABC default 25 | fixed input | CLI argument; no prior | `ABCRejection.java:189-193`; `ExampleGrid.java:808-815`; old file read `ExampleGrid.java:910-912` | model assumption | no direct source | none | model design; early metastatic seed | cells | direct count | low | README matches default | sensitivity/infer for initiation studies | Strongly affects establishment/extinction and early JNK fraction |
| 40 | initial JNK+ fraction | Initial tumor state mix | Initialization/JNK | 90% JNK+, 10% JNK- | fixed | none | `ExampleGrid.java:844`, `ExampleGrid.java:857-858`; old main `ExampleGrid.java:1019-1029` | estimated from literature/assumption | IR18 active c-Jun in micrometastases | IR18 Fig. 1F cited | mouse metastases | fraction positive | `rng.Int(10)>8` creates 10% JNK- | medium qualitative, low numeric | paper says >50%, code uses 90% | investigate/narrow range | Confounds with `pOnMax`, `pOffMax`, stress field |
| 41 | grid dimensions | Lattice size | Geometry | 100 x 100 | fixed | none | ABC grid `ABCRejection.java:226`; main `ExampleGrid.java:879`; random init uses `rng.Int(100)` | implementation convenience | none | none | model design | lattice sites | none | high | README/example agrees | fixed unless domain size matters | Carrying capacity and density-dependent crowding |
| 42 | `divHood` | Empty-neighbor movement/division neighborhood | Geometry | Von Neumann 4-neighbor | fixed | none | `ExampleGrid.java:671`; used all `MapEmptyHood(G.divHood)` branches | implementation assumption | none | none | model design | neighbor topology | HAL `VonNeumannHood(false)` | high | not documented in README | sensitivity if spatial dynamics matter | Controls growth anisotropy and crowding |
| 43 | `countNeighbors15` radius | Immediate local contact radius | Spatial interactions | 1.5 sites | fixed | none | `ExampleGrid.java:69-75`; EC activation `ExampleGrid.java:418`; rim/core `ExampleGrid.java:755` | model assumption | strategy says spatial ranges to be measured later | Research Strategy SA1 spatial analysis | future histology | cell diameters | radius query | low | PPT sometimes says 1 grid for tumor stress and 3 grid for EC/mac/fib | sensitivity/investigate | Affects EC activation, local counts, rim diagnostics |
| 44 | `countNeighbors35` radius | Medium-range paracrine/spatial interaction radius | Spatial interactions | 3.5 sites | fixed | none | `ExampleGrid.java:87-91`; tumor EC survival `ExampleGrid.java:160`; macrophage `ExampleGrid.java:309`; fibroblast `ExampleGrid.java:470` | model assumption | `MetsTNBC.pptx` says 3 grid/cell diameters for several interactions | slides 4,7,8,9 | conceptual model | cell diameters | radius query | medium qualitative, low numeric | code uses 3.5, slides say 3 | sensitivity | Controls nearly every cross-cell activation/survival interaction |
| 45 | niche search multiplier | CAF field search cutoff | Spatial/numerical | `3.0*lambdaCAF + 1.0` | fixed | none | `ExampleGrid.java:113` | implementation convenience | none | none | model design | sites | search cutoff for exponential | medium | undocumented | fixed/convenience | If too short, cuts off niche effects |
| 46 | stress search multiplier | Stress field search cutoff | Spatial/numerical | `3.0*lambdaStress + 1.0` | fixed | none | `ExampleGrid.java:139` | implementation convenience | none | none | model design | sites | search cutoff for exponential | medium | undocumented | fixed/convenience | If too short, cuts off stress effects |
| 47 | `P` combined JNK signal formula | Combines CAF and stress JNK-promoting signals | JNK switching | `1-(1-S)*(1-stress)` | fixed formula | none | `ExampleGrid.java:164`, `ExampleGrid.java:237` | model assumption | qualitative biology from IR18/P20 | none exact | model design | dimensionless | probabilistic OR-like combination | low | undocumented outside comments | investigate | Strongly shapes switching and confounds `pOn/pOff` |
| 48 | `pOff` clamp | Event-ladder clamp for JNK+ switch-off | Tumor event logic | `max(0,min(pOff,1-dieEff-divEff))` | fixed formula | none | `ExampleGrid.java:170`, chemo `ExampleGrid.java:243` | implementation convenience | none | none | model design | probability | clamps switch probability only | medium | documented in code | fixed, but audit in sensitivity | Can block switching when death/division are high |
| 49 | `pOn` clamp | Event-ladder clamp for JNK- switch-on | Tumor event logic | `max(0,min(pOn,1-dieEff-divProb))` | fixed formula | none | `ExampleGrid.java:198`, chemo `ExampleGrid.java:273` | implementation convenience | none | none | model design | probability | clamps switch probability only | medium | documented in code | fixed, but audit | Can block JNK activation under high death/division |
| 50 | chemotherapy JNK+ death multiplier | Chemo increases JNK+ death | Therapy | 2.0 | fixed but calibration-dead | none | `ExampleGrid.java:239-240` | model assumption from qualitative literature | IR18 Fig. 5D/E cited | chemo-treated lung metastasis | JNK+ fraction after chemo | multiplier on death | low | qualitative chemo JNK shift only, not death multiplier | sensitivity before therapy simulations | Not active in default ABC |
| 51 | chemotherapy JNK+ pOff multiplier | Chemo makes JNK+ harder to exit | Therapy/JNK | 0.05 | fixed but calibration-dead | none | `ExampleGrid.java:242` | model assumption | IR18 Fig. 5D/E cited | chemo response | fraction JNK+ | multiplier on `pOffMax` | low | no direct support | sensitivity | Not active in default ABC |
| 52 | chemotherapy JNK- death multiplier | Chemo increases JNK- death | Therapy | 5.0 | fixed but calibration-dead | none | `ExampleGrid.java:269-270` | model assumption | IR18 Fig. 5D/E cited | chemo response | fraction/JNK survival | multiplier on death | low | no direct support | sensitivity | Not active in default ABC |
| 53 | chemotherapy JNK- pOn multiplier | Chemo increases JNK- to JNK+ ceiling | Therapy/JNK | 5.0 capped at 1.0 | fixed but calibration-dead | none | `ExampleGrid.java:271-272` | model assumption | IR18 Fig. 5D/E cited | chemo response | JNK+ fraction | `onCeil=min(1,5*pOnMax)` | low | no direct support | sensitivity | Not active in default ABC |
| 54 | chemo start threshold | Time when chemo branch starts | Timing/therapy | after `i > 2898` | fixed | none | `ExampleGrid.java:865`, `ExampleGrid.java:1056-1057` | model assumption/implementation | none | none | model design | steps | chemo at final full-course step if `maxStep=2900` | low | comments say final chemo step | investigate | Therapy has no effect on ABC default |
| 55 | default calibration horizon | Simulation length for ABC | Timing | 1440 steps | fixed | optional `maxStep` overload; old `--steps` | `ExampleGrid.java:808-811`; main `ExampleGrid.java:879-880` | implementation/calibration | week 3 target convention | multiple paper timepoints | mouse lung metastasis | days/weeks | 1440 = day 21 if 480/week | medium for target, low timestep | code consistent with targets | fixed for current ABC | Blocks step 2100 and chemo |
| 56 | snapshot steps in `RunHeadless` | Model snapshots | Timing/calibration | `{0,480,960,1440,2100}` but returns 4 by default | fixed | none | `ExampleGrid.java:822`, `ExampleGrid.java:862-866` | calibration convention | IR18/P20/H22 week targets | multiple figures | mouse lung metastasis | weeks/days | 480 steps/week | medium | mismatch with ABC requiring 4 | fix documentation later | Determines all calibration summaries |
| 57 | ABC `SNAP` | Scored snapshot order | ABC calibration | `{0,480,960,1440}` | fixed | none | `ABCRejection.java:88` | calibration convention | paper timepoints | week 1/2/3 | mouse lung metastasis | steps | direct index mapping | high code confidence | mismatches comments saying 5 snapshots | fixed | Any returned step 2100 would break distance unless `SNAP` updated |
| 58 | ABC target types `TT` | Calibration outputs | ABC calibration | 10 targets | fixed | none | `ABCRejection.java:82` | literature targets/proxies | IR18/P20/H22 | comments lines `ABCRejection.java:57-63` | mixed systems | fractions/log10 fold | computed in `stat()` | medium | no external config present | fixed but review target validity | Determines posterior |
| 59 | ABC target steps `TS` | Target timepoints | ABC calibration | 480, 960, 1440 | fixed | none | `ABCRejection.java:83` | literature target timepoints | IR18/P20/H22 | week 1/2/3 | mouse lung metastasis | steps/weeks | 480 steps/week | medium | consistent with code, but timestep issue | fixed after timestep decision | Drives distance |
| 60 | ABC target values `TV` | Observed target values | ABC calibration | `{0.53,0.14,0.15,0.30,0.80,0.77,1.10,0.42,0.78,1.11}` | fixed | none | `ABCRejection.java:84` | direct/estimated literature targets | IR18/P20/H22 plus MDA231-LM2 biolum | comments lines `ABCRejection.java:57-71` | mouse/patient/in vitro mixed | fractions/log10 fold | fold targets 3D log10 multiplied by 2/3 | medium for values cited in code, low independently verified | no config to compare | investigate exact extraction | Main calibration constraints |
| 61 | ABC target weights `TW` | Target residual weights | ABC calibration | `{1,1,0.5,0.5,1,1,1,1,1,1}` | fixed | none | `ABCRejection.java:85` | model assumption | none | none | calibration design | dimensionless | weights in distance | high code, low biology | undocumented elsewhere | sensitivity | Changes posterior emphasis |
| 62 | ABC target scales `TSC` | Residual scales/tolerances | ABC calibration | fractions 0.2; folds 0.27 | fixed | none | `ABCRejection.java:86`; comments `ABCRejection.java:77-79` | model assumption with 2D adjustment | none direct | none | calibration design | residual scale | fold scale 0.4*2/3=0.27 | medium arithmetic, low biology | references absent config | sensitivity | Affects acceptance distance |
| 63 | NaN residual penalty | Penalty for missing stromal target due overgrowth | ABC calibration | 3.0 residual | fixed | none | `ABCRejection.java:123-131` | implementation convenience | none | none | calibration design | residual units | contributes `weight*9` | high code | undocumented | fixed/tune | Can dominate distance for saturated runs |
| 64 | extinction rule | Reject final tumor extinction | ABC calibration | infinite distance if final tumor count 0 | fixed | none | `ABCRejection.java:117-118` | calibration assumption | none | none | model design | cell count | hard reject | high code | documented in README broadly | fixed | Excludes extinction parameter sets |
| 65 | ABC defaults `N,epsilon,quantile,seed,initPop` | Rejection ABC run controls | ABC calibration | 1000, -1, 0.2, 12345, 25 | fixed defaults | CLI override | `ABCRejection.java:189-193`; README `README.md:40-47` | implementation convenience | none | none | computational | draws/tolerance/seed/cells | none | high | code/README match | fixed per experiment | Affects posterior sample size and accepted rows |
| 66 | run seed formula | Per-draw simulation RNG seed | ABC calibration | `seed + 1000 + n` | fixed | none | `ABCRejection.java:226-228` | implementation convenience | none | none | stochastic design | seed | deterministic per draw | high | undocumented | fixed | Affects stochastic reproducibility |
| 67 | macrophage count | Initial random macrophages | Initialization | 925 | fixed | none | `ExampleGrid.java:837-842`; old main `ExampleGrid.java:963-971` | model assumption; conflicts with old slides | `MetsTNBC.pptx` slide 23 estimated ~22 in 67x67 | slide 23 | lung histology proportions from Schupp/Weibel cited in slide | cells | direct count in 100x100 full grid | low | major mismatch with slide derivation | investigate/sensitivity | Strong effect on EC activation and crowding |
| 68 | lung cell count | Initial random lung cells | Initialization | 1225 | fixed | none | `ExampleGrid.java:837-842`; old main `ExampleGrid.java:974-981` | model assumption; conflicts with old slides | `MetsTNBC.pptx` slide 23 estimated ~107 in 67x67 | slide 23 | lung histology proportions | cells | direct count in 100x100 full grid | low | major mismatch with slide derivation | investigate/sensitivity | Controls stress field and occupancy |
| 69 | EC coordinate count | Initial endothelial positions from file | Initialization/geometry | 237 coordinate pairs | fixed file input | none | file read `ExampleGrid.java:826-831`, `ExampleGrid.java:941-950`; file `QuadratEndothelialOn.txt` | model assumption/histology-derived unclear | slide 23 estimated ~450 ECs | `MetsTNBC.pptx` slide 23 | histology/model representation | cells/coordinates | direct coordinates; no duplicates/out-of-bounds found | low | mismatch with slide estimate | investigate source image | Controls EC survival and macrophage localization |
| 70 | fibroblast coordinate count | Initial fibroblast positions from file | Initialization/geometry | 142 coordinate pairs | fixed file input | none | file read `ExampleGrid.java:832-836`, `ExampleGrid.java:952-961`; file `QuadratStrOn.txt` | model assumption/histology-derived unclear | slide 23 estimated ~180 fibroblasts | `MetsTNBC.pptx` slide 23 | histology/model representation | cells/coordinates | direct coordinates; no overlap with EC coordinates found | low | mismatch with slide estimate | investigate source image | Controls CAF activation and tumor JNK rim |
| 71 | tumor seed center/jitter | Initial tumor cluster location | Initialization/geometry | center near `(35,35)`, jitter 6, margin `clusterRadius+2` | fixed | none | `ExampleGrid.java:845-847`, old main `ExampleGrid.java:991-997` | model assumption | H22/P20 qualitative perivascular cluster | H22 Fig. 1a, P20 Fig. 1c cited | mouse lung metastasis images | grid sites | random integer 32-38 for x/y with current values | low | no direct numeric support | sensitivity | Location relative to coordinate files controls local niche |
| 72 | tumor placement attempt limit/boundary guard | Seeding algorithm cap | Initialization/implementation | 10000 attempts; boundary `1..98` | fixed | none | `ExampleGrid.java:849-855`, old main `ExampleGrid.java:999-1015` | implementation convenience | none | none | numerical | attempts/sites | hard cap and guard | high code | undocumented | fixed | Can silently place fewer cells if `InitPop` too high |

## 4. Parameters Directly Supported by Literature

Direct support is limited to qualitative mechanisms and calibration outputs, not most per-step probabilities.

- **JNK/c-Jun fraction targets and chemo-induced JNK activation:** IR18 is cited for JNK+ tumor fractions and chemotherapy increasing JNK/c-Jun activation. In code, this supports target type `jnkp` and chemo qualitative direction (`ABCRejection.java:57`, `ExampleGrid.java:227-230`, `ExampleGrid.java:727`), but not exact switch probabilities or chemo multipliers.
- **Fibroblast niche expansion and JNK/IL-1/CXCL9/10 fibroblast loop:** P20 supports fibroblast niche expansion and crosstalk. In code, this supports the existence of `activProbF`, `divProbFP`, and the fibroblast fold target (`ExampleGrid.java:463-466`, `ABCRejection.java:62`, `ABCRejection.java:70`), but not the exact per-step activation/division values.
- **Macrophage and EC vascular niche axis:** H22 supports macrophage-mediated EC niche formation and perivascular macrophage association. This supports the `mac` and `ec` calibration targets and topology of the macrophage -> EC activation rule (`ABCRejection.java:58-61`, `ExampleGrid.java:303-305`, `ExampleGrid.java:412-414`, `ExampleGrid.java:445-447`), but not exact probabilities.
- **Tumor and fibroblast fold-change targets:** The log10 targets in `TV` are presented as derived from literature curves and adjusted for 2D. The exact data extraction cannot be verified from the local PDFs because text extraction failed; therefore the values should be treated as documented estimates pending mentor/source confirmation.

Directly supported row count in this audit: **6** rows have direct support for output targets or qualitative mechanisms (`TT`, `TS`, `TV`, JNK fractions, fibroblast expansion, EC/macrophage niche). Direct support for actual per-step probabilities is effectively **0**.

## 5. Parameters Estimated or Converted from Literature

The following values are explicitly estimated or converted:

1. **2D fold-change conversion** (`ABCRejection.java:65-79`):
   - Formula: for compact isotropic growth, `fold_2D = fold_3D^(2/3)`.
   - In log10 space: `log10(fold_2D) = (2/3) * log10(fold_3D)`.
   - Recalculations:
     - Fibroblast: `1.65 * 2/3 = 1.10`; fold = `10^1.10 = 12.59`.
     - Tumor week 1: `0.63 * 2/3 = 0.42`; fold = `10^0.42 = 2.63`.
     - Tumor week 2: `1.17 * 2/3 = 0.78`; fold = `10^0.78 = 6.03`.
     - Tumor week 3: `1.66 * 2/3 = 1.1067`, rounded in code to `1.11`; fold = `10^1.11 = 12.88`.
   - Confidence: medium for arithmetic, low for the original extracted 3D values until the figure extraction is documented.

2. **Activated EC division from H22 Fig. 1d** (`ExampleGrid.java:651-652`):
   - Code comment: EC approximately doubles from week 1 to week 3, over 960 steps.
   - Formula: net per-step growth `r = ln(2)/960 = 0.000722`.
   - Code: `divProbEP = dieProbEP + r = 0.008 + 0.000722 = 0.008722`, rounded to `0.0087`.
   - Confidence: medium for arithmetic, low until the figure value is confirmed.

3. **JNK- division from net growth and death** (`ExampleGrid.java:816-817`):
   - Formula: `divProbN = dieProbN + netN`.
   - This enforces net growth `divProbN - dieProbN = netN`.
   - Confidence: high code confidence, low biological confidence.

4. **Activated macrophage division from death** (`ExampleGrid.java:639-642`):
   - Formula: `divProbMP = 1.05 * dieProbMP = 1.05 * 0.015 = 0.01575`.
   - Biological rationale: 5% margin offsets empty-neighbor blocking.
   - Confidence: medium code rationale, low literature support.

5. **Migration probability from speed** (`ExampleGrid.java:619-623`, `ExampleGrid.java:645`):
   - Tumor comment: TNBC migration about 20 um/h, grid spacing about 20 um, step about 30-60 min.
   - If current target timing is used, one step is 21 min; `migrProbP=0.10` gives expected speed about `0.10*20 um / 0.35 h = 5.7 um/h`, below the comment.
   - Macrophage `migrProbM=0.8` gives `0.8*20 um / 0.35 h = 45.7 um/h = 0.76 um/min`, close to the cited about 1 um/min.
   - Confidence depends entirely on resolving the step duration.

## 6. Model Assumptions and Convenience Constants

Model assumptions dominate the current implementation. The highest-impact assumptions are:

- `lambdaCAF=2.0`, `lambdaStress=2.0`, `stressStrength=1.5`, and combined signal `P=1-(1-S)(1-stress)`.
- Event ladder ordering: death -> division -> switch -> migration for tumor cells; death -> division -> migration -> activation for inactive macrophages.
- Hard-coded interaction boosts: macrophage daughter activation `0.008`, activated EC daughter division `0.001`, CAF boost `0.02` with cap 10.
- Initial geometry: grid `100x100`, tumor seed near `(35,35)`, cluster radius 4, 25 tumor cells by ABC default, 90% initial JNK+.
- Initial background populations: 237 EC coordinates, 142 fibroblast coordinates, 925 random macrophages, 1225 random lung cells.
- ABC weights/scales and NaN penalty.

These should be documented as assumptions unless mentor-provided image quantification or figure digitization can be attached.

## 7. Code-Versus-Document Mismatches

1. **Missing reference/config files**
   - No `ABC_TNBC_parameter_reference.md`, PDF equivalent, YAML, or `abc_config.yaml` exists in this repository.
   - `ABCRejection.java:54` and `ABCRejection.java:80` refer to an external config that is absent.

2. **12-parameter order**
   - `ABCRejection.NAME`: `netN,dieProbN,pOnMax,pOffMax,divProbP,dieProbP,cafDivBoost,ecSurvival,activProbF,divProbFP,activProbM,activProbE` (`ABCRejection.java:45-48`).
   - `RunHeadless` applies the same order with `divProbN=dieProbN+netN` (`ExampleGrid.java:816-820`).
   - `ExampleGrid.main` old file path expects direct `divProbN` at position 0 (`ExampleGrid.java:916-922`), not `netN`.

3. **Old priors in `ExampleGrid` comments**
   - `divProbP`: old U(0.02,0.18), current U(0.005,0.03).
   - `dieProbP`: old U(0.005,0.06), current U(0.001,0.004).
   - `divProbN`: old U(0.01,0.12), current derived from `dieProbN+netN`.
   - `dieProbN`: old U(0.005,0.05), current U(0.008,0.025).
   - `divProbFP`: old U(0.01,0.15), current U(0.018,0.038).
   - `activProbM`: old U(0.005,0.08), current U(0.02,0.08).
   - `pOnMax`: old U(0.01,0.20), current U(0.01,0.10).
   - `cafDivBoost`: old U(0,3), current U(0,1).

4. **README fixed parameter mismatch**
   - README: `lambdaCAF=2.0`, `muStress=3.0` (`README.md:73-74`).
   - Code: `lambdaCAF=2.0`, `stressStrength=1.5`, `lambdaStress=2.0` (`ExampleGrid.java:610-615`).

5. **Snapshot mismatch**
   - README/RunHeadless comments: 5 snapshots.
   - ABC: 4 snapshots required.
   - `RunHeadless` includes step 2100 in `snapSteps` but never reaches it with default maxStep 1440.

6. **Chemotherapy mismatch**
   - Comments say model still runs to 2900 for chemo in ABC comment block (`ABCRejection.java:72-75`), but current `RunHeadless` default stops at 1440.

7. **Lung homeostasis mismatch**
   - Comment says `divProbL=dieProbL` keeps lung present (`ExampleGrid.java:661-662`).
   - `lungCells()` only dies (`ExampleGrid.java:524-525`).

8. **Macrophage death mismatch with strategy**
   - Research Strategy says macrophages will have zero death based on homeostatic turnover/lack of apoptosis.
   - Current code uses nonzero `dieProbMN=0.005` and `dieProbMP=0.015`.

9. **Initial counts mismatch with slide deck**
   - `MetsTNBC.pptx` slide 23 estimates about 450 ECs, 180 fibroblasts, 22 macrophages, 107 lung cells on a 67x67 derivation.
   - Current code uses 237 EC coordinates, 142 fibroblast coordinates, 925 macrophages, 1225 lung cells on a 100x100 grid.

## 8. Parameters That May Not Actually Affect Model Outputs

- `activProbMP`: explicitly unused (`ExampleGrid.java:644`).
- `divProbL`: passed to lung cells but `lungCells()` ignores division (`ExampleGrid.java:524-525`).
- `divProbEN`: inactive ECs do not divide; deactivated EC receives `divProbEN` but inactive EC logic ignores division (`ExampleGrid.java:438-457`).
- `migrProbE`: passed to ECs but EC method has no migration branch.
- `migrProbF`: passed to fibroblasts but fibroblast method has no migration branch.
- Tumor per-cell `activProb`: seed passes `pOnMax` or `pOffMax`, but tumor switching uses grid-level `G.pOnMax/G.pOffMax`, not `this.activProb`.
- EC per-cell `activProb`: EC activation uses grid-level `G.activProbE`, not `this.activProb`.
- Chemo multipliers: do not affect default ABC because maxStep is 1440 and chemo starts only after 2898.
- Step 2100 snapshot: defined in `RunHeadless`, but not returned during default ABC; not scored by `ABCRejection`.

## 9. Recommended Sensitivity Ranges for All Parameters

These ranges are recommendations for a sensitivity-analysis pass, not corrections to the current code.

| Parameter/group | Recommended range/class | Rationale |
|---|---|---|
| `netN`, `dieProbN`, `divProbP`, `dieProbP` | keep inferred; repeat after timestep decision | Directly controls tumor growth/extinction; current priors conflict with old comments |
| `pOnMax`, `pOffMax` | inferred, broad; at least current ABC ranges, consider old upper 0.20 for `pOnMax` | JNK switching is mechanistic core and weakly supported numerically |
| `cafDivBoost`, `ecSurvival` | inferred; test wider `cafDivBoost` 0-3 and current 0-1 | Prior mismatch and strong confounding |
| `activProbF`, `divProbFP`, `activProbM`, `activProbE` | inferred; keep broad until figure extraction is documented | Calibrated from output targets, not direct per-cell measurements |
| `lambdaCAF`, `lambdaStress` | wide sensitivity: 0.5-5 cell diameters | Spatial reach is image-derived/assumed and high leverage |
| `stressStrength` | wide sensitivity: 0-3 or 0-5 | README/code mismatch and strong JNK effect |
| `recruitBias` | sensitivity: 0-0.2 | No direct source; affects macrophage spatial pattern |
| `migrProbP`, `migrProbN` | vary common scale and maintain 10x ratio initially | Ratio has qualitative IR18 support; absolute speed depends on timestep |
| `dieProbFN`, `dieProbFP`, `dieProbMN`, `dieProbMP`, `dieProbEN`, `dieProbEP`, `dieProbL` | investigate; include at least one order-of-magnitude range after timestep fix | Current values imply short half-lives despite "weeks" comments |
| `divProbMN`, `divProbMP`, `divProbEP` | sensitivity around current values; couple to death rates where derived | Derived/homeostatic logic should be explicit |
| `deactProbE` | sensitivity 0-0.05 | No source; controls EC activation fraction |
| hard-coded `0.008`, `0.001`, `0.02`, cap 10 | promote to sensitivity parameters | Currently hidden high-impact parameters |
| initial counts and coordinate-derived populations | sensitivity or rederive from histology | Current counts mismatch slide deck |
| `InitPop`, initial JNK+ fraction, `clusterRadius`, seed center/jitter | sensitivity | Strong early establishment effects |
| ABC `TV`, `TW`, `TSC`, NaN penalty | sensitivity/calibration audit | Posterior depends heavily on target weights/scales |
| chemo multipliers and start time | investigate separately for therapy model | Not calibrated by current ABC |

## 10. Recommended Fixed Versus Inferred Classification

Recommended **inferred now**:

- `netN`, `dieProbN`, `divProbP`, `dieProbP`
- `pOnMax`, `pOffMax`
- `cafDivBoost`, `ecSurvival`
- `activProbF`, `divProbFP`, `activProbM`, `activProbE`

Recommended **reopen for sensitivity before surrogate training**:

- `lambdaCAF`, `lambdaStress`, `stressStrength`
- `recruitBias`
- `migrProbP`, `migrProbN`, `migrProbM`
- all fixed death/turnover parameters
- initial counts, `InitPop`, initial JNK+ fraction, `clusterRadius`
- hard-coded boosts `0.008`, `0.001`, `0.02`, cap 10
- ABC target weights/scales and dimensional adjustment exponent

Recommended **fixed only as implementation constants**:

- grid size only if domain-size sensitivity is out of scope
- Von Neumann movement neighborhood, but document it clearly
- numerical search cutoffs `3*lambda+1`, after confirming they do not truncate meaningful signal
- seed formula and progress interval

Recommended **remove or implement before biological interpretation**:

- `activProbMP`
- `divProbL`
- `divProbEN`
- `migrProbE`
- `migrProbF`
- tumor and EC per-cell `activProb` uses

## 11. Missing Evidence and Questions for the Mentor

1. What is the intended biological duration of one ABM step: 21 minutes, 30 minutes, 60 minutes, or something else?
2. Where is the missing `ABC_TNBC_parameter_reference` file or `abc_config.yaml` referenced by the code?
3. Which exact figures and digitized values produced tumor log10 fold targets `0.63`, `1.17`, `1.66` before 2D adjustment?
4. Which exact P20 panel/data produced fibroblast log10 fold `1.65`? The strategy text says 100-500 fold in one place and over 500% in another, while code says about 45x.
5. Should macrophage calibration target `0.77` represent activated macrophage fraction, perivascular fraction, or both? The code uses activated fraction as a proxy for perivascular fraction.
6. Should EC target values `0.15`, `0.30`, `0.80` represent EC+ nodule fraction, activated EC fraction among ECs, or another measurement?
7. Are the static coordinate files derived from a histology image? If yes, which image, pixel scale, and crop?
8. Why did initial macrophages/lung cells change from slide-derived counts (~22/~107) to 925/1225?
9. Should `lambdaCAF` be inferred or fixed? The old `main` path can read it from files, while current ABC fixes it.
10. Are chemo multipliers intended to be calibrated against IR18 Fig. 5D/E, or are they purely scenario assumptions?
11. Should lung cells divide? Current code says homeostatic but implements only death.
12. Should inactive ECs divide? Current code declares `divProbEN` but inactive ECs never divide.

## 12. Exact Next Steps

1. Resolve and document the simulation time step. Recompute every per-step probability afterward.
2. Recover or recreate the missing parameter reference/config document with traceable citations and figure digitization notes.
3. Decide whether `RunHeadless`/ABC or file-driven `ExampleGrid.main` is the authoritative parameter interface; make parameter 0 semantics consistent before future runs.
4. Promote hidden hard-coded interaction strengths (`0.008`, `0.001`, `0.02`, cap 10) into named parameters or explicitly justify them as fixed assumptions.
5. Audit/fix unused parameters before interpreting them biologically.
6. Re-evaluate fixed turnover/death values after timestep resolution; current values are likely too high for "weeks" turnover.
7. Build a small validation script that prints effective half-lives, doubling times, and expected migration speeds from the current parameter set.
8. Before ABC or neural-network surrogate training, run global sensitivity on all fixed-but-uncertain parameters listed in Section 9.

## Derived-Value and Unit-Conversion Checks

Using the current calibration mapping `480 steps = 7 days`:

- Step duration: `7 days / 480 = 0.014583 days = 0.35 hours = 21 minutes`.
- One day: `68.57 steps`.
- Three weeks: `1440 steps`.

Half-life from per-step death probability:

Formula: `t_half_steps = ln(2)/p` for small per-step probability `p`; exact discrete formula is `ln(0.5)/ln(1-p)`, nearly identical here.

| Parameter | p | Approx half-life steps | Approx half-life days at 480 steps/week | Comment |
|---|---:|---:|---:|---|
| `dieProbFN` | 0.008 | 86.6 | 1.26 | Comment says fibroblast turnover about 7 days |
| `dieProbFP` | 0.012 | 57.8 | 0.84 | No direct support |
| `dieProbMN` | 0.005 | 138.6 | 2.02 | Comment says 1-2 week half-life |
| `dieProbMP` | 0.015 | 46.2 | 0.67 | Comment says tissue macrophage turnover |
| `dieProbEN` | 0.005 | 138.6 | 2.02 | Comment says EC turnover weeks |
| `dieProbEP` | 0.008 | 86.6 | 1.26 | Activated EC death assumption |
| `dieProbL` | 0.002 | 346.6 | 5.05 | Comment says alveolar turnover weeks |

Doubling/net-growth conversion:

- Exact net per-step for a doubling over `T` steps is `2^(1/T)-1`.
- Continuous approximation is `ln(2)/T`.
- For EC doubling over 960 steps: exact `2^(1/960)-1 = 0.000722`; continuous `ln(2)/960 = 0.000722`.
- Code sets `divProbEP=0.0087` and `dieProbEP=0.008`, net about `0.0007`, which matches the arithmetic.

Migration speed check:

- If lattice spacing is 20 um and current step is 21 min:
  - `migrProbP=0.10`: expected speed `0.10*20/0.35 = 5.7 um/h`.
  - `migrProbN=0.01`: expected speed `0.57 um/h`.
  - `migrProbM=0.8`: expected speed `45.7 um/h = 0.76 um/min`.
- If one step is 30 min or 60 min as comments state, all speeds change by factors of 0.7 or 0.35 relative to the 21-minute interpretation.

## Final Audit Counts

- Total parameters/constants found: **72**
- Directly supported by literature as mechanisms or calibration outputs: **6**
- Directly supported as per-step ABM probabilities: **0**
- Estimated or converted from literature: **11**
- Model assumptions or implementation conveniences: **55**
- Code/document/literature mismatches or obsolete definitions: **18**
- Parameters/constants that should likely be varied in sensitivity analysis: **33**

Five highest-priority issues before further ABC or neural-network surrogate training:

1. Resolve the time-step definition and recompute all per-step probabilities.
2. Restore or recreate the missing parameter reference/config with exact figure extraction provenance.
3. Fix/document the `netN` versus `divProbN` interface mismatch between ABC and file-driven runs.
4. Investigate unused/blocked fixed parameters, especially lung division and inactive EC division.
5. Promote hidden hard-coded interaction strengths and uncertain fixed spatial/turnover constants into sensitivity parameters.
