# Mentor Decisions Required

These questions remain unresolved after Phase 1. They should be answered before expanding beyond the `core4` ABC pilot.

1. Should Tier B macrophage turnover parameters `divProbMP`, `dieProbMP`, `divProbMN`, and `dieProbMN` remain fixed, or should any be inferred after review?
2. If macrophage turnover is calibrated, should division/death be independent rates, or should a constrained net-turnover parameterization be used?
3. Should resting macrophage division and death remain homeostatically linked at baseline?
4. Should activated macrophage division and death preserve the current `divProbMP = 1.05 * dieProbMP` relationship?
5. Is `divProbFN = 0` an intended biological assumption for resting fibroblasts?
6. Is the current resting fibroblast death range (`dieProbFN` up to 0.008 per step) biologically plausible under 480 steps/week?
7. Is inactive endothelial death without inactive endothelial division intended, or is the EC loss mode an implementation/provenance problem?
8. Do `stressStrength` and `lambdaStress` have defensible biological ranges?
9. Should `migrProbP` be calibrated independently from `migrProbN`, or should the IR18-inspired ratio be constrained?
10. Are `clusterRadius` and `initPop` known experimental conditions, or uncertain parameters?
11. What is the exact provenance of every calibration target value, including figure, extraction method, and unit conversion?
12. Is activated macrophage fraction an acceptable proxy for perivascular macrophage fraction?
13. Does activated EC fraction in the model match the experimental measurement used for the EC target?
14. Is the 3D-to-2D fold-change conversion for tumour and fibroblast targets justified for this model geometry?
15. What biological duration should 480 simulation steps represent, and should all rate conversions use 480 steps/week?
16. Should `dieProbL` remain active without a lung-cell division branch despite the homeostasis comment?
17. Should fixed initial background counts (`initialMacrophageCount`, `initialLungCount`) and coordinate-file counts be treated as measured geometry or uncertainty?
