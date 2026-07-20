# STEP 7 — Confirmation Parameter Selection

**18 parameters** were selected for 3-seed matched confirmation, reusing the primary 20-trajectory design
(common random numbers within Morris pairs). Selection was performed by the validated pipeline
(`--confirm-only --confirmation-top 12 --confirmation-replicates 3`), whose built-in criteria correspond
directly to the study's selection rules. `--confirmation-top` was set to **12** (vs the documented default
10) so the fixed-dominated primary top tier — including the two ABC-inferred parameters that rose into it
(divProbP, pOffMax) — is fully covered. This is a parameter of the documented command, not a new mechanism.

Confirmation ran **571 of 920 trajectory points × 3 replicates = 1713 simulations** (only the neighbouring
points needed for the selected parameters, not the whole design × 3).

## Selection criteria and the parameters each contributes

Ranks are primary 20-trajectory `overall_biological_priority_score` ranks (of 45).

### C1 — Top-12 overall biological-priority (criteria 1 + 4: top drivers, incl. fixed top tier)
divProbMP (1), dieProbMP (2), divProbFN (3), stressStrength (4), divProbMN (5), dieProbMN (6),
divProbP (7), pOffMax (8), dieProbFN (9), migrProbP (10), dieProbEN (11), clusterRadius (12).
Ten of these twelve are currently **fixed/hard-coded** (only divProbP and pOffMax are ABC-inferred),
satisfying "any currently fixed parameter in the primary top tier".

### C2 — High sigma, moderate mu\* on biological endpoint outputs (criterion 2: interaction candidates)
initialJnkPositiveTenths (29), dieProbFP (26), initialMacrophageCount (23).
These have sigma at/above the per-output Q75 while mu\* sits in the interquartile band on s1440 endpoint
outputs — the signature of interaction/nonlinearity worth separating from stochastic noise via replication.

### C3 — Tumour-extinction drivers (criterion 3: failure/extinction association)
initPop (16) — top-3 |rank-biserial| for TUMOR_EXTINCT. The other two extinction leaders, divProbMP and
dieProbMP, are already in C1.

### C4 — Biologically essential rates with unexpectedly low rank (criterion 5)
ecSurvival (41), activProbM (39) — both in the essential set {pOnMax, pOffMax, activProbF, activProbM,
activProbE, ecSurvival} and ranked in the bottom half, so their low screening influence is checked under
matched seeds rather than assumed.

## Coverage of the other failure modes (criterion 3, EC / macrophage / invalid)

These are covered through the top-12 rather than as separate additions:

| Failure mode | Primary top driver | In confirmation set? |
|---|---|---|
| EC population loss | dieProbEN (rank 11), dieProbEP | dieProbEN ✔ (C1); dieProbEP not (rank 13, just outside) |
| Macrophage population loss | dieProbMN (6), divProbMP (1), dieProbMP (2) | ✔ (C1) |
| Tumour extinction | dieProbMP, divProbMP, initPop | ✔ (C1 + C3) |
| General invalid | dieProbEN, migrProbP, clusterRadius | ✔ (C1) |

## Honest gap (criterion 6 — largest pilot→primary movers)

The two largest movers between the 10- and 20-trajectory screens were **dieProbP** (pilot 39 → primary 21,
−18) and **pOnMax** (pilot 41 → primary 25, −16), both ABC-inferred. Neither is in the top-12, neither is a
top-3 extinction/interaction driver, and they were edged out of the essential-low slot by ecSurvival(41) and
activProbM(39). They are therefore **not** re-simulated in this confirmation pass. Because their ranks
themselves are unstable, their moderate primary ranks should be treated as provisional; they are flagged in
the final decision table (INCONCLUSIVE / NEEDS_ADDITIONAL_DATA) and recommended for the next targeted pass
or the space-filling surrogate design. This gap is documented rather than papered over.

## Selected set (18)

divProbMP, dieProbMP, divProbFN, stressStrength, divProbMN, dieProbMN, divProbP, pOffMax, dieProbFN,
migrProbP, dieProbEN, clusterRadius, initialJnkPositiveTenths, dieProbFP, initialMacrophageCount, initPop,
ecSurvival, activProbM.
