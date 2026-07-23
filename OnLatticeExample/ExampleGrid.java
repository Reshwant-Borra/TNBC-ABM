package OnLatticeExample;

import HAL.GridsAndAgents.AgentGrid2D;
import HAL.GridsAndAgents.AgentSQ2Dunstackable;
import HAL.Gui.GridWindow;
import HAL.Rand;
import HAL.Util;
import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.FileWriter;

import static HAL.Util.VonNeumannHood;

// ================================================================
// ExampleGrid — TNBC lung metastasis ABM
// Refactored for ABC parameterization
//
// CHANGES FROM ORIGINAL:
//   - UUID run IDs via args[0]  (enables parallel ABC)
//   - Single shared Random      (no per-cell new Random())
//   - Collapsed neighbourhood queries (countNeighbors15/35)
//   - DetailedCounts() output   (needed for JNK+/- ABC stats)
//   - Headless mode via --headless flag
//   - Clustered tumour seeding  (biologically realistic)
//   - Fixed parameters loaded from constants, not files
//   - Removed dead code (tmpStr, migrHood, getRandomNumberUsingInts)
//
// PARAMETER SOURCES:
//   [H22]  = Hongu et al. 2022, Nature Cancer
//   [P20]  = Pein et al. 2020, Nature Communications
//   [IR18] = Insua-Rodriguez et al. 2018, EMBO Mol Med
//   [LIT]  = General cell biology literature
// ================================================================

class ExampleCell extends AgentSQ2Dunstackable<ExampleGrid> {

    int    typeCell;
    double divProb;
    double dieProb;
    double migrProb;
    double activProb;
    double transformed;   // 1 = activated/JNK+,  0 = inactive/JNK-

    public void Init(int typeCell, double divProb, double dieProb,
                     double transformed, double migrProb, double activProb) {
        this.typeCell    = typeCell;
        this.transformed = transformed;
        this.divProb     = divProb;
        this.dieProb     = dieProb;
        this.migrProb    = migrProb;
        this.activProb   = activProb;
    }

    // ---- cell type codes ----
    //   0 = endothelial cells
    //   1 = macrophages
    //   2 = tumor cells
    //   3 = lung cells
    //   4 = fibroblasts

    // ----------------------------------------------------------------
    // Neighbourhood helpers — called once per cell per step
    // instead of the original 3-6 separate GetAgentsRad calls.
    // ----------------------------------------------------------------

    // Returns int[6] for radius 1.5:
    //   [0] tumJNKp   [1] tumJNKn   [2] lung
    //   [3] fibroInact [4] fibroAct  [5] macroInact
    private int[] countNeighbors15() {
        ArrayList<ExampleCell> neigh = new ArrayList<>();
        G.GetAgentsRad(neigh, this.Xsq(), this.Ysq(), 1.5);
        int tumP=0, tumN=0, lung=0, fibI=0, fibA=0, macI=0;
        for (ExampleCell c : neigh) {
            switch (c.typeCell) {
                case 2: if (c.transformed==1) tumP++; else tumN++; break;
                case 3: lung++;  break;
                case 4: if (c.transformed==1) fibA++; else fibI++; break;
                case 1: if (c.transformed==0) macI++; break;
            }
        }
        return new int[]{tumP, tumN, lung, fibI, fibA, macI};
    }

    // Returns int[3] for radius 3.5:
    //   [0] fibroAct35   [1] endoAct35   [2] tumJNKp35
    private int[] countNeighbors35() {
        ArrayList<ExampleCell> neigh = new ArrayList<>();
        G.GetAgentsRad(neigh, this.Xsq(), this.Ysq(), G.tumorEndothelialRadius);
        int fibA=0, endA=0, tumP=0;
        for (ExampleCell c : neigh) {
            if      (c.typeCell==4 && c.transformed==1) fibA++;
            else if (c.typeCell==0 && c.transformed==1) endA++;
            else if (c.typeCell==2 && c.transformed==1) tumP++;
        }
        return new int[]{fibA, endA, tumP};
    }

    // ----------------------------------------------------------------
    // TUMOUR CELLS — normal
    // ----------------------------------------------------------------
    // ----------------------------------------------------------------
    // Loop-1 niche signal:  S = exp(-d / lambdaCAF)
    //   d = Euclidean distance (sites) to the nearest ACTIVATED fibroblast.
    //   Searched within ~3*lambda (S negligible beyond); 0 if none found.
    //   S in [0,1]: ~1 touching a CAF, -> 0 in the CAF-free core.
    // ----------------------------------------------------------------
    private double nicheSignal() {
        double lam = G.lambdaCAF;
        ArrayList<ExampleCell> near = new ArrayList<>();
        G.GetAgentsRad(near, this.Xsq(), this.Ysq(), 3.0*lam + 1.0);
        double dmin = Double.MAX_VALUE;
        for (ExampleCell c : near) {
            if (c.typeCell == 4 && c.transformed == 1) {
                double dx = c.Xsq() - this.Xsq(), dy = c.Ysq() - this.Ysq();
                double d = Math.sqrt(dx*dx + dy*dy);
                if (d < dmin) dmin = d;
            }
        }
        return (dmin == Double.MAX_VALUE) ? 0.0 : Math.exp(-dmin / lam);
    }

    // ----------------------------------------------------------------
    // Foreign-contact stress source: adjacent (r1.5) RESTING stroma + lung.
    // Represents the hostile lung tissue a disseminated cell must survive in;
    // ACTIVATED stroma is the supportive niche (handled by nicheSignal / EC
    // survival), not counted here. Drives the JNK 'stress' signal (initiation)
    // - solves the CAF bootstrap and keeps small micromets uniformly JNK+.
    // ----------------------------------------------------------------
    // Hostile-environment stress as a distance-decayed FIELD (mirrors the CAF niche nicheSignal()):
    // stress = exp(-d / lambdaStress), d = distance to the nearest RESTING stroma / lung cell.
    // A small nodule sits entirely within the field (near-uniformly JNK+); a large nodule has an
    // interior beyond reach (JNK- core). Penetration depth = lambdaStress.
    private double stressSignal() {
        double lam = G.lambdaStress;
        ArrayList<ExampleCell> near = new ArrayList<>();
        G.GetAgentsRad(near, this.Xsq(), this.Ysq(), 3.0*lam + 1.0);
        double dmin = Double.MAX_VALUE;
        for (ExampleCell c : near) {
            boolean foreign = (c.typeCell == 3)                                                        // lung
                    || ((c.typeCell == 4 || c.typeCell == 1 || c.typeCell == 0) && c.transformed == 0); // resting fib/mac/EC
            if (foreign) {
                double dx = c.Xsq() - this.Xsq(), dy = c.Ysq() - this.Ysq();
                double d = Math.sqrt(dx*dx + dy*dy);
                if (d < dmin) dmin = d;
            }
        }
        return (dmin == Double.MAX_VALUE) ? 0.0 : Math.min(1.0, G.stressStrength * Math.exp(-dmin / lam));
    }

    // ----------------------------------------------------------------
    // TUMOUR CELLS - normal.  Two niche loops:
    //   Loop 1 (CAF, rim):  S maintains JNK+ and boosts JNK+ division.
    //   Loop 2 (EC, all):   active ECs lower the death of ANY tumour cell.
    // All probabilities bounded in [0,1] by construction (S in [0,1]).
    // ----------------------------------------------------------------
    public void tumorCell() {
        int[]  n35    = countNeighbors35();              // [1] = active ECs within r3.5
        double r      = G.rng.Double();
        double S      = nicheSignal();                   // Loop 1: CAF paracrine (maintenance)
        double stress = stressSignal();                  // hostile-environment stress FIELD exp(-d/lambdaStress), [0,1]
        double P      = 1.0 - (1.0 - S) * (1.0 - stress);// combined JNK-promoting signal, [0,1)
        double dieEff = this.dieProb * Math.exp(-G.ecSurvival * n35[1]);  // Loop 2: EC survival

        if (this.transformed == 1) {          // JNK+
            double divEff = this.divProb * (1.0 + G.cafDivBoost * S);     // Loop 1: CAF boosts division
            double pOff   = G.pOffMax * (1.0 - P);                        // switch OFF where no niche/stress
            pOff = Math.max(0.0, Math.min(pOff, 1.0 - dieEff - divEff));  // keep ladder proper

            if (r <= dieEff) {
                Dispose(); ++G.numDie;
            } else if (r < dieEff + divEff) {
                int opts = MapEmptyHood(G.divHood);
                if (opts > 0) {
                    ++G.numDiv;
                    int ind = G.rng.Int(opts);
                    G.NewAgentSQ(G.divHood[ind]).Init(
                            2, G.divProbP, this.dieProb, 1, this.migrProb, this.activProb);
                }
            } else if (r < dieEff + divEff + pOff) {
                this.transformed = 0;                    // JNK+ -> JNK-
                this.divProb = G.divProbN;
                this.dieProb = G.dieProbN;
            } else if (r < dieEff + divEff + pOff + this.migrProb) {
                int opts = MapEmptyHood(G.divHood);
                if (opts > 0) {
                    int ind = G.rng.Int(opts);
                    G.NewAgentSQ(G.divHood[ind]).Init(
                            2, this.divProb, this.dieProb, 1, this.migrProb, this.activProb);
                    Dispose();
                }
            }

        } else {                               // JNK-
            double pOn = G.pOnMax * P;                                    // switch ON near CAFs or foreign stress
            pOn = Math.max(0.0, Math.min(pOn, 1.0 - dieEff - this.divProb));

            if (r <= dieEff) {
                Dispose(); ++G.numDie;
            } else if (r < dieEff + this.divProb) {
                int opts = MapEmptyHood(G.divHood);
                if (opts > 0) {
                    ++G.numDiv;
                    int ind = G.rng.Int(opts);
                    G.NewAgentSQ(G.divHood[ind]).Init(
                            2, G.divProbN, this.dieProb, 0, this.migrProb, this.activProb);
                }
            } else if (r < dieEff + this.divProb + pOn) {
                this.transformed = 1;                    // JNK- -> JNK+
                this.divProb = G.divProbP;
                this.dieProb = G.dieProbP;
            } else if (r < dieEff + this.divProb + pOn + this.migrProb) {
                int opts = MapEmptyHood(G.divHood);
                if (opts > 0) {
                    int ind = G.rng.Int(opts);
                    G.NewAgentSQ(G.divHood[ind]).Init(
                            2, this.divProb, this.dieProb, 0, this.migrProb, this.activProb);
                    Dispose();
                }
            }
        }
    }

    // ----------------------------------------------------------------
    // TUMOUR CELLS — under chemotherapy
    // [IR18 Fig.5D/E]: chemo drives JNK+ proportion from <20% to >80%
    //   -> 2x death for JNK+, switch-off (pOffMax) scaled to 5% (harder to escape JNK+)
    //   → 5x death for JNK-, 5x switch to JNK+ (stress drives JNK activation)
    // ----------------------------------------------------------------
    public void tumorCellChemo() {
        int[]  n35 = countNeighbors35();
        double r   = G.rng.Double();
        double S   = nicheSignal();
        double stress = stressSignal();
        double P      = 1.0 - (1.0 - S) * (1.0 - stress);

        if (this.transformed == 1) {          // JNK+  (chemo: 2x death; harder to leave JNK+)
            double dieEff = 2.0 * this.dieProb * Math.exp(-G.ecSurvival * n35[1]);
            double divEff = this.divProb * (1.0 + G.cafDivBoost * S);
            double pOff   = 0.05 * G.pOffMax * (1.0 - P);
            pOff = Math.max(0.0, Math.min(pOff, 1.0 - dieEff - divEff));

            if (r <= dieEff) {
                Dispose(); ++G.numDieT;
            } else if (r < dieEff + divEff) {
                int opts = MapEmptyHood(G.divHood);
                if (opts > 0) {
                    ++G.numDivT;
                    int ind = G.rng.Int(opts);
                    G.NewAgentSQ(G.divHood[ind]).Init(
                            2, G.divProbP, this.dieProb, 1, this.migrProb, this.activProb);
                }
            } else if (r < dieEff + divEff + pOff) {
                this.transformed = 0;
                this.divProb = G.divProbN;
                this.dieProb = G.dieProbN;
            } else if (r < dieEff + divEff + pOff + this.migrProb) {
                int opts = MapEmptyHood(G.divHood);
                if (opts > 0) {
                    int ind = G.rng.Int(opts);
                    G.NewAgentSQ(G.divHood[ind]).Init(
                            2, this.divProb, this.dieProb, 1, this.migrProb, this.activProb);
                    Dispose();
                }
            }

        } else {                               // JNK-  (chemo: 5x death; 5x switch-on CEILING, capped <=1)
            double dieEff = 5.0 * this.dieProb * Math.exp(-G.ecSurvival * n35[1]);
            double onCeil = Math.min(1.0, 5.0 * G.pOnMax);   // ceiling capped -> cannot exceed 1
            double pOn    = onCeil * P;
            pOn = Math.max(0.0, Math.min(pOn, 1.0 - dieEff - this.divProb));

            if (r <= dieEff) {
                Dispose(); ++G.numDieT;
            } else if (r < dieEff + this.divProb) {
                int opts = MapEmptyHood(G.divHood);
                if (opts > 0) {
                    ++G.numDivT;
                    int ind = G.rng.Int(opts);
                    G.NewAgentSQ(G.divHood[ind]).Init(
                            2, G.divProbN, this.dieProb, 0, this.migrProb, this.activProb);
                }
            } else if (r < dieEff + this.divProb + pOn) {
                this.transformed = 1;
                this.divProb = G.divProbP;
                this.dieProb = G.dieProbP;
            } else if (r < dieEff + this.divProb + pOn + this.migrProb) {
                int opts = MapEmptyHood(G.divHood);
                if (opts > 0) {
                    int ind = G.rng.Int(opts);
                    G.NewAgentSQ(G.divHood[ind]).Init(
                            2, this.divProb, this.dieProb, 0, this.migrProb, this.activProb);
                    Dispose();
                }
            }
        }
    }

    // ----------------------------------------------------------------
    // MACROPHAGES
    // [H22 Fig.6e]: ~77% of nodule macrophages are perivascular
    // [H22 Fig.6f]: interstitial (recruited) macrophages increase ~2-3x
    //               during metastatic progression
    // ----------------------------------------------------------------
    public void Macrophages() {
        ArrayList<ExampleCell> neigh35 = new ArrayList<>();
        G.GetAgentsRad(neigh35, this.Xsq(), this.Ysq(), G.macrophageInteractionRadius);
        int tumorCount = 0;
        ArrayList<ExampleCell> endoList = new ArrayList<>();
        for (ExampleCell c : neigh35) {
            if (c.typeCell==2 && c.transformed==1) tumorCount++;
            if (c.typeCell==0) endoList.add(c);
        }
        int endoCount = endoList.size();
        double r = G.rng.Double();

        if (this.transformed == 1) {          // activated macrophage
            if (r <= this.dieProb) {
                Dispose(); ++G.numDieM;
            } else if (r < this.dieProb + this.divProb) {
                int opts = MapEmptyHood(G.divHood);
                G.macDivTry++;
                if (opts > 0) {
                    ++G.numDivM;
                    int ind = G.rng.Int(opts);
                    G.NewAgentSQ(G.divHood[ind]).Init(
                            1, this.divProb, this.dieProb, 1, this.migrProb, this.activProb);
                } else {
                    G.macDivFail++;   // crowding: activated mac wanted to divide but had no empty neighbour
                }
            } else if (r < this.dieProb + this.divProb + this.migrProb) {
                int opts = MapEmptyHood(G.divHood);
                if (opts > 0) {
                    if (endoCount == 0) {
                        int ind = G.rng.Int(opts);
                        G.NewAgentSQ(G.divHood[ind]).Init(
                                1, this.divProb, this.dieProb, 1, this.migrProb, this.activProb);
                        Dispose();
                    } else {
                        // bias migration toward endothelial cells [H22: perivascular localisation]
                        ArrayList<Integer> biased = new ArrayList<>();
                        for (ExampleCell e : endoList)
                            for (int j = 0; j < opts; j++)
                                if (Util.Dist(e.Xsq(), e.Ysq(),
                                        G.ItoX(G.divHood[j]), G.ItoY(G.divHood[j])) <= G.macrophageEndothelialBiasRadius)
                                    if (!biased.contains(G.divHood[j]))
                                        biased.add(G.divHood[j]);

                        // biased can be empty if no empty slot is close enough to
                        // any endothelial cell — fall back to random migration
                        int targetSlot;
                        if (biased.size() > 0) {
                            int idx = (int)(G.rng.Double() * biased.size());
                            targetSlot = biased.get(idx);
                        } else {
                            targetSlot = G.divHood[G.rng.Int(opts)];
                        }
                        G.NewAgentSQ(targetSlot).Init(
                                1, this.divProb, this.dieProb, 1, this.migrProb, this.activProb);
                        Dispose();
                    }
                }
            }
            // activated macrophages do NOT deactivate -- activation is irreversible
            // (tumour-associated macrophages stay polarized in the niche)

        } else {                               // inactive macrophage
            if (r <= this.dieProb) {
                Dispose(); ++G.numDieM;
            } else if (r < this.dieProb + this.divProb) {
                int opts = MapEmptyHood(G.divHood);
                if (opts > 0) {
                    ++G.numDivM;
                    int ind = G.rng.Int(opts);
                    double newAct = G.activProbM + G.macrophageDaughterActivationBoost * tumorCount;
                    G.NewAgentSQ(G.divHood[ind]).Init(
                            1, G.divProbMN, this.dieProb, 0, this.migrProb, newAct);
                }
            } else if (r < this.dieProb + this.divProb + this.migrProb) {
                int opts = MapEmptyHood(G.divHood);
                if (opts > 0) {
                    // RECRUITMENT: with prob recruitBias, step to the empty neighbour nearest the tumour
                    // centroid (chemotaxis toward the tumour); otherwise random walk.
                    int slot;
                    if (G.tumorCX >= 0 && G.rng.Double() < G.recruitBias) {
                        slot = G.divHood[0]; double best = Double.MAX_VALUE;
                        for (int j = 0; j < opts; j++) {
                            double dx = G.ItoX(G.divHood[j]) - G.tumorCX, dy = G.ItoY(G.divHood[j]) - G.tumorCY;
                            double d = dx*dx + dy*dy;
                            if (d < best) { best = d; slot = G.divHood[j]; }
                        }
                    } else {
                        slot = G.divHood[G.rng.Int(opts)];
                    }
                    G.NewAgentSQ(slot).Init(
                            1, this.divProb, this.dieProb, 0, this.migrProb, this.activProb);
                    Dispose();
                }
            } else if (r < this.dieProb + this.divProb + this.migrProb + this.activProb) {
                if (tumorCount > 0) {
                    this.transformed = 1;    // irreversible: no path back to inactive
                    this.divProb   = G.divProbMP;
                    this.dieProb   = G.dieProbMP;
                }
            }
        }
    }

    // ----------------------------------------------------------------
    // ENDOTHELIAL CELLS
    // [H22 Fig.1b]: EC+ nodule fraction: ~20% wk1 → ~45% wk2 → ~80% wk3
    // [H22 Fig.1d]: EC count correlates with cancer cell count (R ~ 0.9)
    // ----------------------------------------------------------------
    public void Endothelial() {
        ArrayList<ExampleCell> neigh = new ArrayList<>();
        G.GetAgentsRad(neigh, this.Xsq(), this.Ysq(), G.endothelialMacrophageRadius);
        int macroActCount = 0;
        for (ExampleCell c : neigh)
            if (c.typeCell==1 && c.transformed==1) macroActCount++;

        double r = G.rng.Double();

        if (this.transformed == 1) {          // activated EC
            if (r <= this.dieProb) {
                Dispose(); ++G.numDieE;
            } else if (r < this.dieProb + this.divProb) {
                int opts = MapEmptyHood(G.divHood);
                if (opts > 0) {
                    ++G.numDivE;
                    int ind = G.rng.Int(opts);
                    // activated macrophages drive EC proliferation [H22 Fig.1d, Fig.6]
                    double newDiv = G.divProbEP + G.endothelialDaughterDivisionBoost * macroActCount;
                    G.NewAgentSQ(G.divHood[ind]).Init(
                            0, newDiv, this.dieProb, 1, this.migrProb, this.activProb);
                }
            } else if (macroActCount == 0 && r < this.dieProb + this.divProb + G.deactProbE) {
                // no activated-mac signal nearby -> gently revert to inactive
                this.transformed = 0;
                this.divProb = G.divProbEN;
                this.dieProb = G.dieProbEN;
            }
        } else {                               // inactive EC -- does NOT proliferate (only activated ECs divide)
            // Only two fates: die, or activate. Activation requires activated
            // macrophages within 1 grid distance: hazard = 1-(1-activProbE)^k for
            // k such macs (k==0 -> pAct==0 -> no activation) [H22 Fig.7, NO/TNF].
            double pAct = (macroActCount > 0)
                    ? Math.min(1.0 - Math.pow(1.0 - G.activProbE, macroActCount), 1.0 - this.dieProb)
                    : 0.0;
            if (r <= this.dieProb) {
                Dispose(); ++G.numDieE;
            } else if (r < this.dieProb + pAct) {
                this.transformed = 1;
                this.divProb   = G.divProbEP;
                this.dieProb   = G.dieProbEP;
            }
        }
    }

    // ----------------------------------------------------------------
    // FIBROBLASTS
    // [P20 Fig.1f]: fibroblast count increases ~45-fold (healthy -> wk3 macromet)
    // [P20 Fig.1d/e]: fibroblast number correlates with metastatic burden R~0.97
    // [P20 Fig.6d]: IL-1 knockdown → ~50% reduction in colonization
    //               → activProbF and divProbFP are tightly constrained
    // ----------------------------------------------------------------
    public void Fibroblasts() {
        ArrayList<ExampleCell> neigh = new ArrayList<>();
        G.GetAgentsRad(neigh, this.Xsq(), this.Ysq(), G.fibroblastTumorRadius);
        int tumorFCount = 0;
        for (ExampleCell c : neigh)
            if (c.typeCell==2 && c.transformed==1) tumorFCount++;

        double r = G.rng.Double();

        if (this.transformed == 1) {          // activated CAF
            if (r <= this.dieProb) {
                Dispose(); ++G.numDieF;
            } else if (r < this.dieProb + this.divProb) {
                int opts = MapEmptyHood(G.divHood);
                if (opts > 0) {
                    ++G.numDivF;
                    int ind = G.rng.Int(opts);
                    // CAF division into empty space; expansion is bounded by available
                    // space + the ~45x fibroblast target (P20 Fig.1f), which ABC calibrates.
                    G.NewAgentSQ(G.divHood[ind]).Init(
                            4, G.divProbFP, G.dieProbFP, 1, this.migrProb, this.activProb);
                }
            }
        } else {                               // inactive fibroblast
            if (r <= this.dieProb) {
                Dispose(); ++G.numDieF;
            } else if (r < this.dieProb + this.divProb) {
                int opts = MapEmptyHood(G.divHood);
                if (opts > 0) {
                    ++G.numDivF;
                    int ind = G.rng.Int(opts);
                    G.NewAgentSQ(G.divHood[ind]).Init(
                            4, G.divProbFN, this.dieProb, 0, this.migrProb, G.activProbF);
                }
            } else if (r >= this.dieProb + this.divProb &&
                    r  < this.dieProb + this.divProb + this.activProb) {
                if (tumorFCount > 0) {
                    // IL-1α/β from JNK+ tumour cells activates fibroblasts [P20, IR18]
                    this.transformed = 1;
                    this.dieProb   = G.dieProbFP;
                    // bound the JNK+ density boost so the CAF event ladder stays < 1
                    int fB = Math.min(tumorFCount, G.fibroblastSignalCap);
                    // JNK+ -> fibroblast SIGNAL STRENGTH (not range): how hard JNK+ density drives CAF
                    // proliferation / further activation. Raised 0.01 -> 0.02 to strengthen the communication. [tunable]
                    this.divProb   = G.divProbFP  + G.fibroblastSignalBoost * fB;
                    this.activProb = G.activProbF + G.fibroblastSignalBoost * fB;
                }
            }
        }
    }

    // ----------------------------------------------------------------
    // LUNG CELLS
    // [LIT]: slow baseline turnover; die faster near tumour (not modelled
    //         explicitly here — dieProb is a fixed parameter)
    // ----------------------------------------------------------------
    public void lungCells() {
        if (G.rng.Double() <= this.dieProb) Dispose();
    }
}


// ==================================================================
public class ExampleGrid extends AgentGrid2D<ExampleCell> {

    // ---- step counters ----
    public int numDiv, numDie;
    public int numDivF, numDieF;
    public int numDivM, numDieM;
    public int macDivTry, macDivFail;   // crowding read: activated-mac division attempts / (blocked for no space) over the run
    public int numDivT, numDieT;
    public int numDivE, numDieE;

    public int    InitPop;
    public double kill;
    public String runId;
    public boolean headless = false;

    // =================================================================
    // PARAMETERS TO INFER WITH ABC
    // These are set from parameter files (written by Python ABC wrapper)
    // Bounds come from our three-paper analysis.
    // =================================================================

    // -- Tumour JNK+ --
    // [IR18 Fig.1H]: MDA231-LM2 doubling ~24h in vivo, 1 step ≈ 30-60 min
    // Prior: Uniform(0.02, 0.18)
    public double divProbP;   // JNK+ division probability   [ABC: infer]

    // Prior: Uniform(0.005, 0.06)
    public double dieProbP;   // JNK+ death probability      [ABC: infer]

    // -- Tumour JNK- --
    // JNK- cells grow slower and die at similar rate [IR18 Fig.1]
    // Prior: Uniform(0.01, 0.12)
    public double divProbN;   // JNK- division probability   [ABC: infer]

    // Prior: Uniform(0.005, 0.05)
    public double dieProbN;   // JNK- death probability      [ABC: infer]

    // -- Fibroblast activation --
    // [P20 Fig.1f]: fibroblast expansion over 3 weeks constrains these tightly
    //   (~45x in 3D; ~12.6x = log10 1.10 after 2D adjustment -- see abc_config.yaml)
    // Prior activProbF: Uniform(0.001, 0.05)
    public double activProbF; // fibroblast activation per JNK+ tumour cell [ABC: infer]

    // Prior divProbFP: Uniform(0.01, 0.15)
    public double divProbFP;  // activated CAF division probability          [ABC: infer]

    // -- Macrophage activation --
    // [H22 Fig.6e]: ~77% of macrophages perivascular in nodules
    // Prior: Uniform(0.005, 0.08)
    public double activProbM; // macrophage activation per JNK+ tumour cell  [ABC: infer]

    // -- Endothelial activation --
    // [H22 Fig.1b]: EC+ nodule % goes 20 → 45 → 80 over weeks 1-3
    // Prior: Uniform(0.005, 0.08)
    public double activProbE; // EC activation hazard per activated mac within 1.5 grid distance [ABC: infer]

    // =================================================================
    // PARAMETERS FIXED FROM LITERATURE / PAPER DATA
    // These do NOT vary across ABC runs.
    // =================================================================

    // -- JNK switching: see TUMOUR-CELL NICHE PARAMETERS below (pOnMax / pOffMax) --
    // ================================================================
    // TUMOUR-CELL NICHE PARAMETERS (two feedback loops) [ABC-inferrable]
    //   Loop 1 (CAF, rim):  S = exp(-d/lambdaCAF), d = dist to nearest activated
    //     fibroblast.  JNK- -> JNK+ = pOnMax*S ; JNK+ -> JNK- = pOffMax*(1-S) ;
    //     JNK+ division *= (1 + cafDivBoost*S).  -> ~2-cell JNK+ rim, JNK- core.
    //   Loop 2 (EC, all):   dieEff = dieProb * exp(-ecSurvival * activeEC_count).
    //   Bounded in [0,1] by construction (S in [0,1]); no clamp / no overflow.
    //   ABC priors: pOnMax U(0.01,0.20)[p2] pOffMax U(0.01,0.20)[p3]
    //     cafDivBoost U(0,3)[p6] ecSurvival U(0,0.3)[p7] lambdaCAF fixed 2.0[p8]
    // ================================================================
    public double pOnMax      = 0.05; // JNK- -> JNK+ ceiling at a CAF
    public double pOffMax     = 0.05; // JNK+ -> JNK- ceiling far from CAFs
    public double cafDivBoost = 1.0;  // JNK+ division boost at a CAF
    public double ecSurvival  = 0.10; // death reduction per nearby active EC
    public double recruitBias = 0.04;  // inactive-macrophage recruitment: prob. of biasing a migration step toward the
                                      // tumour centroid (chemotaxis) vs random walk. Gentle so the fraction rises from below. [FIXED, tunable]
    public double tumorCX = -1, tumorCY = -1;  // tumour centroid, updated each step (-1 => no tumour)
    public double lambdaCAF   = 2.0;  // niche decay length (sites); FIXED from image
    public double stressStrength = 1.5; // stress SIGNAL GAIN (not reach): multiplies the stress field, saturating at 1, so a
                                        // given amount of hostile contact drives JNK+ switch-on / maintenance harder. Lifts the
                                        // rim toward 1 (pOff->0, cells stay JNK+) but leaves the deep core ~0 (gain*0=0). [FIXED, tunable]
    public double lambdaStress = 2.0; // hostile-environment stress penetration depth (sites): stress = exp(-d/lambdaStress)
                                      // to nearest resting stroma/lung. Was a ~1-cell reach; a deeper field keeps a SMALL
                                      // nodule near-uniformly JNK+ and slows its (quiescent) early growth. [FIXED, tunable]

    // -- Tumour migration --
    // [IR18 Fig.3M]: JNK inhibition reduces Matrigel invasion ~10x
    //   → migrProbP ≈ 10 × migrProbN
    // TNBC migration ~20 µm/h; grid spacing ~20 µm; step ~30-60 min
    public double migrProbP = 0.10;  // JNK+ migration probability  [IR18, fixed]
    public double migrProbN = 0.01;  // JNK- migration probability  [IR18, fixed]

    // -- Fibroblast basal rates --
    // [LIT]: lung fibroblast turnover ~7 days
    public double divProbFN  = 0.0;    // inactive fibroblast division: 0 - only ACTIVATED fibroblasts proliferate [Pein]
    public double dieProbFN  = 0.008;  // inactive fibroblast death     [LIT, fixed]
    public double dieProbFP  = 0.012;  // activated CAF death           [LIT, fixed]
    public double migrProbF  = 0.000;  // fibroblasts don't migrate     [fixed]

    // -- Macrophage basal rates --
    // [LIT]: tissue macrophage turnover; interstitial macs ~1-2 week half-life
    public double divProbMN  = 0.005;  // inactive macrophage division  [LIT, fixed]
    public double dieProbMN  = 0.005;  // inactive macrophage death: = divProbMN so the RESTING pool is
                                       // net-zero (homeostatic) and does not collapse. Was 0.008 (net -0.003/step),
                                       // which crashed macs to ~12% by day 21. The nodule increase [H22 Fig.6f, ~2-3x]
                                       // now comes from activated-mac proliferation; a clean 2-3x needs recruitment.
    public double divProbMP  = 0.01575; // activated macrophage division = 1.05 * dieProbMP.
                                       // ~5% above death because division needs an empty neighbour but death does
                                       // not, so a small margin offsets the space-limited throttling of division.
                                       // (activation is irreversible, so the fraction rises mainly by conversion.)
    public double dieProbMP  = 0.015;  // activated macrophage death    [LIT, fixed]
    public double activProbMP = 0.020; // (UNUSED) macrophage activation is now irreversible; kept for reference only
    public double migrProbM  = 0.8;    // macrophage migration: macs move ~every step (fastest the lattice allows) [Pixley 2012 IJCB: ~1 um/min in vitro, >10 in vivo; fibroblasts only 0.1-0.5]

    // -- Endothelial basal rates --
    // [LIT]: quiescent EC turnover ~weeks; activated EC proliferates faster
    public double divProbEN  = 0.0;    // inactive EC division: 0 - only ACTIVATED ECs proliferate [Hongu Fig.1d]
    public double dieProbEN  = 0.005;  // inactive EC death             [LIT, fixed]
    public double divProbEP  = 0.0087;  // activated EC division: from H22 Fig.1d (EC ~2x wk1->wk3, 960 steps)
                                       // -> net = ln(2)/960 = 0.00072/step above dieProbEP. WAS 0.040 (net +0.032, ~44x too fast).
    public double dieProbEP  = 0.008;  // activated EC death            [LIT, fixed]
    public double deactProbE = 0.01;   // activated EC -> inactive, GENTLE and signal-gated: only fires when there is
                                       // NO activated macrophage within 1.5 (macroActCount==0), so the EC fraction
                                       // tracks the (rising) mac signal up AND down instead of latching at 100% [fixed]
    public double migrProbE  = 0.000;  // ECs don't migrate (fixed tubes) [fixed]

    // -- Lung cell rates --
    // [LIT]: alveolar epithelial turnover ~weeks
    public double divProbL   = 0.002;  // lung division  [DIAGNOSTIC REVERT: homeostatic, net 0 -- keeps the resting
                                       // lung present so the foreign-contact stress that maintains JNK+ early is not lost]
    public double dieProbL   = 0.002;  // lung death

    // -- Cluster seeding --
    // Biologically: metastatic cells arrive as a small perivascular cluster
    public int    clusterRadius = 4;   // cells placed within radius 4 of seed point

    // Named sensitivity controls. Their defaults reproduce the literals that
    // were previously embedded in the biological and initialization rules.
    public double macrophageDaughterActivationBoost = 0.008;
    public double endothelialDaughterDivisionBoost = 0.001;
    public double fibroblastSignalBoost = 0.02;
    public int fibroblastSignalCap = 10;
    public double tumorEndothelialRadius = 3.5;
    public double macrophageInteractionRadius = 3.5;
    public double macrophageEndothelialBiasRadius = 3.5;
    public double endothelialMacrophageRadius = 1.5;
    public double fibroblastTumorRadius = 3.5;
    public int initialJnkPositiveTenths = 9;
    public int initialMacrophageCount = 925;
    public int initialLungCount = 1225;

    // ---- HAL utilities ----
    Rand rng = new Rand();
    int[] divHood = Util.VonNeumannHood(false);
    public int[][] lastRimCore;   // diagnostic: {rim,core} JNK+ counts per snapshot of the last RunHeadless
    public int[] lastSnapshotSteps;
    // cumulative {tumDiv,tumDeath,fibDiv,fibDeath,macDiv,macDeath,ecDiv,ecDeath,chemoTumDiv,chemoTumDeath}
    public int[][] lastEventCounts;
    // {tumorRadius,tumorRmsSpread,jnkpRimFraction,activeMacrophageEcColocalization}
    public double[][] lastSpatialMetrics;

    public static final class DiagnosticFrame {
        public final int step;
        public final int[] counts;
        public final int[] cumulativeEvents;
        DiagnosticFrame(int step, int[] counts, int[] cumulativeEvents) {
            this.step = step;
            this.counts = counts.clone();
            this.cumulativeEvents = cumulativeEvents.clone();
        }
    }

    public static final class DiagnosticRun {
        public final int[][] snapshots;
        public final ArrayList<DiagnosticFrame> frames;
        DiagnosticRun(int[][] snapshots, ArrayList<DiagnosticFrame> frames) {
            this.snapshots = snapshots;
            this.frames = frames;
        }
    }


    public ExampleGrid(int x, int y) throws IOException {
        super(x, y, ExampleCell.class);
    }

    // ----------------------------------------------------------------
    // Step methods
    // ----------------------------------------------------------------
    public void StepCellR() {
        numDiv=numDie=numDivM=numDieM=numDivE=numDieE=numDivF=numDieF=numDivT=numDieT=0;
        ShuffleAgents(rng);   // randomize update order each step (avoid fixed-order growth bias)
        // tumour centroid for inactive-macrophage recruitment (computed before any agent moves)
        double _sx=0,_sy=0; int _nT=0;
        for (ExampleCell c : this) if (c.typeCell==2) { _sx+=c.Xsq(); _sy+=c.Ysq(); _nT++; }
        if (_nT>0) { tumorCX=_sx/_nT; tumorCY=_sy/_nT; } else { tumorCX=-1; tumorCY=-1; }
        for (ExampleCell c : this) {
            switch (c.typeCell) {
                case 0: c.Endothelial();    break;
                case 1: c.Macrophages();    break;
                case 2: c.tumorCell();      break;
                case 3: c.lungCells();      break;
                case 4: c.Fibroblasts();    break;
            }
        }
    }

    public void StepCellRChemo() {
        numDiv=numDie=numDivM=numDieM=numDivE=numDieE=numDivF=numDieF=numDivT=numDieT=0;
        ShuffleAgents(rng);   // randomize update order each step (avoid fixed-order growth bias)
        for (ExampleCell c : this) {
            switch (c.typeCell) {
                case 0: c.Endothelial();      break;
                case 1: c.Macrophages();      break;
                case 2: c.tumorCellChemo();   break;
                case 3: c.lungCells();        break;
                case 4: c.Fibroblasts();      break;
            }
        }
    }

    // ----------------------------------------------------------------
    // Output helpers
    // ----------------------------------------------------------------

    // Basic counter: [numDiv, numDie, totalPop]
    public ArrayList MyCounter(ArrayList<Integer> myCount) {
        myCount.add(numDiv);
        myCount.add(numDie);
        myCount.add(this.Pop());
        return myCount;
    }

    // Detailed per-cell-type counts needed for ABC summary statistics:
    //   S[6]: JNK+ fraction trajectory       [IR18 Fig.1E/F]
    //   S[2]: EC activation fraction          [H22  Fig.1b]
    //   S[3]: macrophage activation fraction  [H22  Fig.6e]
    //   S[1]: fibroblast count (fold change)  [P20  Fig.1f]
    // Returns: [tumJNKp, tumJNKn, ecActive, ecInact,
    //           macActive, macInact, fibroActive, fibroInact]
    public int[] DetailedCounts() {
        int tP=0,tN=0,eA=0,eI=0,mA=0,mI=0,fA=0,fI=0;
        for (ExampleCell c : this) {
            switch (c.typeCell) {
                case 2: if(c.transformed==1) tP++; else tN++; break;
                case 0: if(c.transformed==1) eA++; else eI++; break;
                case 1: if(c.transformed==1) mA++; else mI++; break;
                case 4: if(c.transformed==1) fA++; else fI++; break;
            }
        }
        return new int[]{tP,tN,eA,eI,mA,mI,fA,fI};
    }

    // Diagnostic: split current JNK+ tumour cells into rim vs core.
    // rim  = has an empty or non-tumour site among its 8 Moore neighbours (exposed);
    // core = fully surrounded by tumour cells. Returns {rim, core}.
    public int[] jnkpRimCore() {
        int rim = 0, core = 0;
        ArrayList<ExampleCell> near = new ArrayList<>();
        for (ExampleCell c : this) {
            if (c.typeCell != 2 || c.transformed != 1) continue;   // JNK+ tumour only
            near.clear();
            GetAgentsRad(near, c.Xsq(), c.Ysq(), 1.5);             // 8 Moore neighbours (+ self)
            int nTum = 0;
            for (ExampleCell m : near)
                if (m != c && m.typeCell == 2) nTum++;
            if (nTum >= 8) core++; else rim++;                     // all 8 tumour => buried => core
        }
        return new int[]{rim, core};
    }

    /** Snapshot-only spatial diagnostics; does not consume random numbers. */
    public double[] SpatialMetrics() {
        double sx=0.0, sy=0.0;
        int nTum=0;
        for (ExampleCell c : this) {
            if (c.typeCell==2) { sx+=c.Xsq(); sy+=c.Ysq(); nTum++; }
        }
        double radius=Double.NaN, spread=Double.NaN;
        if (nTum>0) {
            double cx=sx/nTum, cy=sy/nTum, sumSq=0.0, maxSq=0.0;
            for (ExampleCell c : this) if (c.typeCell==2) {
                double dx=c.Xsq()-cx, dy=c.Ysq()-cy, ds=dx*dx+dy*dy;
                sumSq+=ds;
                if (ds>maxSq) maxSq=ds;
            }
            radius=Math.sqrt(maxSq);
            spread=Math.sqrt(sumSq/nTum);
        }

        int[] rc=jnkpRimCore();
        int nJnk=rc[0]+rc[1];
        double rimFrac=nJnk>0 ? (double)rc[0]/nJnk : Double.NaN;

        int activeMac=0, nearEc=0;
        ArrayList<ExampleCell> near = new ArrayList<>();
        for (ExampleCell c : this) {
            if (c.typeCell!=1 || c.transformed!=1) continue;
            activeMac++;
            near.clear();
            GetAgentsRad(near,c.Xsq(),c.Ysq(),macrophageEndothelialBiasRadius);
            boolean found=false;
            for (ExampleCell n : near) if (n.typeCell==0) { found=true; break; }
            if (found) nearEc++;
        }
        double coloc=activeMac>0 ? (double)nearEc/activeMac : Double.NaN;
        return new double[]{radius,spread,rimFrac,coloc};
    }

    public ArrayList MyCoords(ArrayList<Integer> myCoord) {
        for (ExampleCell c : this) myCoord.add(c.typeCell);
        return myCoord;
    }

    public String ReadMyFile(String name) throws IOException {
        return new String(Files.readAllBytes(Paths.get(name)));
    }

    public void DrawModel(GridWindow win) {
        for (int i=0; i<length; i++) {
            int shade = Util.WHITE;
            ExampleCell c = GetAgent(i);
            if (c != null) {
                switch (c.typeCell) {
                    case 0: shade = Util.RED; break;
                    case 1: shade = (c.transformed==1) ? Util.YELLOW : Util.BLUE; break;
                    case 2: shade = (c.transformed==1) ? Util.GREEN : Util.RGB256(0,128,128); break;
                    case 3: shade = Util.WHITE; break;
                    case 4: shade = (c.transformed==1) ? Util.BLACK : Util.RGB256(119,136,153); break;
                }
            }
            win.SetPix(i, shade);
        }
    }

    // ================================================================
    // In-process single run for the pure-Java ABC driver (ABCRejection.java).
    // Creates NO files. The legacy overload sets the original 12 inferred
    // parameters; the named overload supplies the full untreated parameter set.
    // Both seed the tissue and return reached snapshots as int[][8]
    // (each row = {tumJNKp,tumJNKn,ecAct,ecInact,macAct,macInact,fibAct,fibInact}).
    //   theta = {divProbN,dieProbN,pOnMax,pOffMax,divProbP,dieProbP,
    //            cafDivBoost,ecSurvival,activProbF,divProbFP,activProbM,activProbE}
    // lambdaCAF (2.0) and lambdaStress (2.5) keep their fixed class defaults.
    // Call on a FRESH grid whose rng has been seeded, e.g.:
    //   ExampleGrid g = new ExampleGrid(100,100); g.rng = new Rand(seed);
    //   int[][] snaps = g.RunHeadless(theta, initPop);
    // Requires QuadratEndothelialOn.txt and QuadratStrOn.txt in the working dir.
    // ================================================================
    // Calibration entry point. The last target is day 21 = step 1440, so the
    // default horizon stops there (running to 2900 wastes ~1460 steps/run that
    // are never scored). Chemo only fires at step 2899, AFTER the last snapshot,
    // so stopping at 1440 gives bit-identical snapshots and identical distances.
    // Pass maxStep=2900 for the full course incl. the final chemo step.
    public int[][] RunHeadless(double[] th, int initPop) throws IOException {
        return RunHeadless(th, initPop, 1440);
    }
    public int[][] RunHeadless(double[] th, int initPop, int maxStep) throws IOException {
        if (th.length != 12)
            throw new IllegalArgumentException("RunHeadless expected 12 parameters, got " + th.length);
        this.dieProbN  = th[1];  this.divProbN = th[1] + th[0];  // netN reparametrization: th[0]=netN (JNK- net growth),
                                                                 // divProbN = dieProbN + netN (guaranteed net-positive, net = curve-constrained)
        this.pOnMax     = th[2];  this.pOffMax   = th[3];
        this.divProbP = th[4];  this.dieProbP  = th[5];  this.cafDivBoost= th[6];  this.ecSurvival= th[7];
        this.activProbF=th[8];  this.divProbFP = th[9];  this.activProbM = th[10]; this.activProbE= th[11];
        return runHeadlessConfigured(initPop, maxStep);
    }

    public int[][] RunHeadless(ModelParameters p) throws IOException {
        return RunHeadless(p, 1440);
    }

    public int[][] RunHeadless(ModelParameters p, int maxStep) throws IOException {
        p.validate();
        this.dieProbN=p.dieProbN; this.divProbN=p.dieProbN+p.netN;
        this.pOnMax=p.pOnMax; this.pOffMax=p.pOffMax; this.divProbP=p.divProbP; this.dieProbP=p.dieProbP;
        this.cafDivBoost=p.cafDivBoost; this.ecSurvival=p.ecSurvival; this.activProbF=p.activProbF;
        this.divProbFP=p.divProbFP; this.activProbM=p.activProbM; this.activProbE=p.activProbE;
        this.recruitBias=p.recruitBias; this.lambdaCAF=p.lambdaCAF; this.stressStrength=p.stressStrength;
        this.lambdaStress=p.lambdaStress; this.migrProbP=p.migrProbP; this.migrProbN=p.migrProbN;
        this.divProbFN=p.divProbFN; this.dieProbFN=p.dieProbFN; this.dieProbFP=p.dieProbFP;
        this.divProbMN=p.divProbMN; this.dieProbMN=p.dieProbMN; this.divProbMP=p.divProbMP;
        this.dieProbMP=p.dieProbMP; this.migrProbM=p.migrProbM; this.dieProbEN=p.dieProbEN;
        this.divProbEP=p.divProbEP; this.dieProbEP=p.dieProbEP; this.deactProbE=p.deactProbE;
        this.dieProbL=p.dieProbL; this.macrophageDaughterActivationBoost=p.macrophageDaughterActivationBoost;
        this.endothelialDaughterDivisionBoost=p.endothelialDaughterDivisionBoost;
        this.fibroblastSignalBoost=p.fibroblastSignalBoost; this.fibroblastSignalCap=p.fibroblastSignalCap;
        this.tumorEndothelialRadius=p.tumorEndothelialRadius;
        this.macrophageInteractionRadius=p.macrophageInteractionRadius;
        this.macrophageEndothelialBiasRadius=p.macrophageEndothelialBiasRadius;
        this.endothelialMacrophageRadius=p.endothelialMacrophageRadius;
        this.fibroblastTumorRadius=p.fibroblastTumorRadius; this.clusterRadius=p.clusterRadius;
        this.initialJnkPositiveTenths=p.initialJnkPositiveTenths;
        this.initialMacrophageCount=p.initialMacrophageCount; this.initialLungCount=p.initialLungCount;
        return runHeadlessConfigured(p.initPop, maxStep);
    }

    private int[][] runHeadlessConfigured(int initPop, int maxStep) throws IOException {
        return runHeadlessConfigured(initPop, maxStep, null);
    }

    public DiagnosticRun RunHeadlessDiagnostic(ModelParameters p, int maxStep, int diagnosticInterval) throws IOException {
        p.validate();
        this.dieProbN=p.dieProbN; this.divProbN=p.dieProbN+p.netN;
        this.pOnMax=p.pOnMax; this.pOffMax=p.pOffMax; this.divProbP=p.divProbP; this.dieProbP=p.dieProbP;
        this.cafDivBoost=p.cafDivBoost; this.ecSurvival=p.ecSurvival; this.activProbF=p.activProbF;
        this.divProbFP=p.divProbFP; this.activProbM=p.activProbM; this.activProbE=p.activProbE;
        this.recruitBias=p.recruitBias; this.lambdaCAF=p.lambdaCAF; this.stressStrength=p.stressStrength;
        this.lambdaStress=p.lambdaStress; this.migrProbP=p.migrProbP; this.migrProbN=p.migrProbN;
        this.divProbFN=p.divProbFN; this.dieProbFN=p.dieProbFN; this.dieProbFP=p.dieProbFP;
        this.divProbMN=p.divProbMN; this.dieProbMN=p.dieProbMN; this.divProbMP=p.divProbMP;
        this.dieProbMP=p.dieProbMP; this.migrProbM=p.migrProbM; this.dieProbEN=p.dieProbEN;
        this.divProbEP=p.divProbEP; this.dieProbEP=p.dieProbEP; this.deactProbE=p.deactProbE;
        this.dieProbL=p.dieProbL; this.macrophageDaughterActivationBoost=p.macrophageDaughterActivationBoost;
        this.endothelialDaughterDivisionBoost=p.endothelialDaughterDivisionBoost;
        this.fibroblastSignalBoost=p.fibroblastSignalBoost; this.fibroblastSignalCap=p.fibroblastSignalCap;
        this.tumorEndothelialRadius=p.tumorEndothelialRadius;
        this.macrophageInteractionRadius=p.macrophageInteractionRadius;
        this.macrophageEndothelialBiasRadius=p.macrophageEndothelialBiasRadius;
        this.endothelialMacrophageRadius=p.endothelialMacrophageRadius;
        this.fibroblastTumorRadius=p.fibroblastTumorRadius; this.clusterRadius=p.clusterRadius;
        this.initialJnkPositiveTenths=p.initialJnkPositiveTenths;
        this.initialMacrophageCount=p.initialMacrophageCount; this.initialLungCount=p.initialLungCount;
        ArrayList<DiagnosticFrame> frames = new ArrayList<>();
        int interval = Math.max(1, diagnosticInterval);
        int[][] snapshots = runHeadlessConfigured(p.initPop, maxStep, (step, counts, events) -> {
            if (step == 0 || step == maxStep || step % interval == 0 || requiredCompartmentLost(counts)) {
                frames.add(new DiagnosticFrame(step, counts, events));
            }
        });
        return new DiagnosticRun(snapshots, frames);
    }

    interface DiagnosticRecorder {
        void record(int step, int[] counts, int[] cumulativeEvents);
    }

    static boolean requiredCompartmentLost(int[] c) {
        if (c == null || c.length < 8) return true;
        return c[0] + c[1] == 0 || c[2] + c[3] == 0 || c[4] + c[5] == 0 || c[6] + c[7] == 0;
    }

    private int[][] runHeadlessConfigured(int initPop, int maxStep, DiagnosticRecorder diagnosticRecorder) throws IOException {
        if (maxStep < 0) throw new IllegalArgumentException("maxStep must be non-negative");
        this.headless = true;
        this.InitPop = initPop;

        int[] snapSteps = {0, 480, 960, 1440, 2100};
        java.util.List<int[]> snaps = new ArrayList<>();
        java.util.List<int[]> rimCore = new ArrayList<>();   // diagnostic, lockstep with snaps
        java.util.List<Integer> reachedSteps = new ArrayList<>();
        java.util.List<int[]> eventCounts = new ArrayList<>();
        java.util.List<double[]> spatialMetrics = new ArrayList<>();
        int[] cumulativeEvents = new int[10];

        // endothelial cells + fibroblasts from the static coordinate files
        String[] vE = ReadMyFile("QuadratEndothelialOn.txt").split(", ");
        for (int i=0; i<vE.length-1; i+=2) {
            int dx=Integer.parseInt(vE[i].trim()), dy=Integer.parseInt(vE[i+1].trim());
            if (GetAgent(dx,dy)==null) NewAgentSQ(dx,dy).Init(0, divProbEN, dieProbEN, 0, migrProbE, activProbE);
        }
        String[] vF = ReadMyFile("QuadratStrOn.txt").split(", ");
        for (int i=0; i<vF.length-1; i+=2) {
            int dx=Integer.parseInt(vF[i].trim()), dy=Integer.parseInt(vF[i+1].trim());
            if (GetAgent(dx,dy)==null) NewAgentSQ(dx,dy).Init(4, divProbFN, dieProbFN, 0, migrProbF, activProbF);
        }
        // macrophages and lung cells at random positions across the full grid.
        // (Was a 66x66 corner, which packed the tumour region to ~54% occupancy and boxed the
        //  macrophages in; spreading over 100x100 drops local density to ~25%, matching the full-grid
        //  EC/fibroblast layout, so macrophages can migrate and the tumour has room to grow.)
        int k=0;   while (k<initialMacrophageCount)  { int dx=rng.Int(100), dy=rng.Int(100); if (GetAgent(dx,dy)==null){ NewAgentSQ(dx,dy).Init(1, divProbMN, dieProbMN, 0, migrProbM, activProbM); k++; } }
        int mm=0;  while (mm<initialLungCount){ int dx=rng.Int(100), dy=rng.Int(100); if (GetAgent(dx,dy)==null){ NewAgentSQ(dx,dy).Init(3, divProbL,  dieProbL,  0, 0, 0);            mm++; } }

        // clustered tumour seeding (same logic as main), 90% JNK+
        int margin=clusterRadius+2, jitter=6;
        int seedX=Math.max(margin, Math.min(99-margin, 35 + rng.Int(jitter+1) - jitter/2));
        int seedY=Math.max(margin, Math.min(99-margin, 35 + rng.Int(jitter+1) - jitter/2));
        int h=0, att=0;
        while (h<InitPop && att<10000) {
            att++;
            int ox=rng.Int(2*clusterRadius+1)-clusterRadius, oy=rng.Int(2*clusterRadius+1)-clusterRadius;
            if (ox*ox+oy*oy > clusterRadius*clusterRadius) continue;
            int dx=seedX+ox, dy=seedY+oy;
            if (dx<1||dx>=99||dy<1||dy>=99) continue;
            if (GetAgent(dx,dy)==null) {
                h++;
                if (rng.Int(10)>=initialJnkPositiveTenths) NewAgentSQ(dx,dy).Init(2, divProbN, dieProbN, 0, migrProbN, pOnMax);
                else                                      NewAgentSQ(dx,dy).Init(2, divProbP, dieProbP, 1, migrProbP, pOffMax);
            }
        }

        int[] currentCounts = DetailedCounts();
        snaps.add(currentCounts);
        rimCore.add(jnkpRimCore());
        reachedSteps.add(0);
        eventCounts.add(cumulativeEvents.clone());
        spatialMetrics.add(SpatialMetrics());
        if (diagnosticRecorder != null) diagnosticRecorder.record(0, currentCounts, diagnosticEvents(cumulativeEvents));
        for (int i=0; i<maxStep; i++) {                    // calibration horizon (default 1440 = day 21)
            IncTick();
            if (i<=2898) StepCellR(); else StepCellRChemo();  // chemo unused (maxStep=1440); dead branch
            cumulativeEvents[0]+=numDiv; cumulativeEvents[1]+=numDie;
            cumulativeEvents[2]+=numDivF; cumulativeEvents[3]+=numDieF;
            cumulativeEvents[4]+=numDivM; cumulativeEvents[5]+=numDieM;
            cumulativeEvents[6]+=numDivE; cumulativeEvents[7]+=numDieE;
            cumulativeEvents[8]+=numDivT; cumulativeEvents[9]+=numDieT;
            currentCounts = DetailedCounts();
            if (diagnosticRecorder != null) diagnosticRecorder.record(i+1, currentCounts, diagnosticEvents(cumulativeEvents));
            for (int s : snapSteps) if (i+1==s) {
                snaps.add(currentCounts);
                rimCore.add(jnkpRimCore());
                reachedSteps.add(s);
                eventCounts.add(cumulativeEvents.clone());
                spatialMetrics.add(SpatialMetrics());
            }
        }
        this.lastRimCore = rimCore.toArray(new int[0][]);
        this.lastSnapshotSteps = new int[reachedSteps.size()];
        for (int i=0;i<reachedSteps.size();i++) this.lastSnapshotSteps[i]=reachedSteps.get(i);
        this.lastEventCounts = eventCounts.toArray(new int[0][]);
        this.lastSpatialMetrics = spatialMetrics.toArray(new double[0][]);
        return snaps.toArray(new int[0][]);
    }

    private int[] diagnosticEvents(int[] cumulativeEvents) {
        int[] out = new int[12];
        System.arraycopy(cumulativeEvents, 0, out, 0, Math.min(cumulativeEvents.length, 10));
        out[10] = macDivTry;
        out[11] = macDivFail;
        return out;
    }

    // ----------------------------------------------------------------
    // MAIN
    // Usage (interactive): java ExampleGrid
    // Usage (ABC):         java ExampleGrid <runId> --headless
    // ----------------------------------------------------------------
    public static void main(String[] args) throws IOException {

        int x=100, y=100, timeStep=1440;   // calibration horizon (day 21); full chemo course = 2900
        for (String __a : args) if (__a.startsWith("--steps=")) timeStep = Integer.parseInt(__a.substring(8));
        ExampleGrid model = new ExampleGrid(x, y);

        // FIX 1: unique run ID from args, avoids file collisions in parallel ABC
        if (args.length > 0) {
            model.runId = args[0];
        } else {
            model.runId = model.ReadMyFile("NumRun.txt").trim();
        }
        // flags: --headless, and optional --seed <N> for a reproducible single RNG
        for (int a = 1; a < args.length; a++) {
            if (args[a].equals("--headless")) {
                model.headless = true;
            } else if (args[a].equals("--seed") && a + 1 < args.length) {
                model.rng = new Rand(Long.parseLong(args[a + 1]));
            }
        }

        GridWindow win = model.headless ? null : new GridWindow(x, y, 8);
        String StringPath = Util.PWD();

        // ---- snapshot steps for ABC summary statistics ----
        // Step 0   = initial state
        // Step 480  ≈ week 1  (constrains JNK+ fraction S[6][0] [IR18])
        // Step 960  ≈ week 2  (EC activation midpoint S[2][1]   [H22])
        // Step 1440 ≈ week 3  (primary calibration target)
        // Step 2100 ≈ day 30  (beyond papers' 3-week window; tumor growth only)
        int[] snapshotSteps = {0, 480, 960, 1440, 2100};
        List<int[]> snapshots = new ArrayList<>();

        // ---- read InitPop ----
        model.InitPop = Integer.parseInt(
                model.ReadMyFile("InitPopulation" + model.runId + ".txt").trim().split(",")[0]);

        // ---- read ABC-inferred parameters from files ----
        // (Python ABC wrapper writes these before invoking java)
        String[] pT = model.ReadMyFile("ABMparamsTumor" + model.runId + ".txt").split(",");
        model.divProbN    = Double.parseDouble(pT[0]);
        model.dieProbN    = Double.parseDouble(pT[1]);
        model.pOnMax  = Double.parseDouble(pT[2]);
        model.pOffMax = Double.parseDouble(pT[3]);
        model.divProbP    = Double.parseDouble(pT[4]);
        model.dieProbP    = Double.parseDouble(pT[5]);
        // Spatial-switching coefficients (optional positions 6-8; ABC-inferrable).
        // 6-value files keep the defaults declared in the class.
        if (pT.length > 6) model.cafDivBoost = Double.parseDouble(pT[6].trim());
        if (pT.length > 7) model.ecSurvival  = Double.parseDouble(pT[7].trim());
        if (pT.length > 8) model.lambdaCAF   = Double.parseDouble(pT[8].trim());
        if (pT.length > 9) model.lambdaStress = Double.parseDouble(pT[9].trim());
        // migrProbP and migrProbN are fixed constants above, not read from file

        String[] pF = model.ReadMyFile("ABMparamsFibroblasts" + model.runId + ".txt").split(",");
        model.activProbF = Double.parseDouble(pF[0]); // [P20 Fig.1f constraint]
        model.divProbFP  = Double.parseDouble(pF[1]); // [P20 Fig.1f constraint]

        String[] pM = model.ReadMyFile("ABMparamsMacrophages" + model.runId + ".txt").split(",");
        model.activProbM = Double.parseDouble(pM[0]); // [H22 Fig.6e constraint]

        String[] pE = model.ReadMyFile("ABMparamsEndothelial" + model.runId + ".txt").split(",");
        model.activProbE = Double.parseDouble(pE[0]); // [H22 Fig.1b constraint]

        // ---- initialise endothelial cells (from coordinate file) ----
        String myFileE = model.ReadMyFile("QuadratEndothelialOn.txt");
        String[] valuesE = myFileE.split(", ");
        for (int i=0; i<valuesE.length-1; i+=2) {
            int dx = Integer.parseInt(valuesE[i].trim());
            int dy = Integer.parseInt(valuesE[i+1].trim());
            if (model.GetAgent(dx,dy)==null)
                model.NewAgentSQ(dx,dy).Init(0, model.divProbEN, model.dieProbEN,
                        0, model.migrProbE, model.activProbE);
        }

        // ---- initialise fibroblasts (from coordinate file) ----
        String myFile = model.ReadMyFile("QuadratStrOn.txt");
        String[] values = myFile.split(", ");
        for (int i=0; i<values.length-1; i+=2) {
            int dx = Integer.parseInt(values[i].trim());
            int dy = Integer.parseInt(values[i+1].trim());
            if (model.GetAgent(dx,dy)==null)
                model.NewAgentSQ(dx,dy).Init(4, model.divProbFN, model.dieProbFN,
                        0, model.migrProbF, model.activProbF);
        }

        // ---- initialise macrophages (random positions) ----
        int k=0;
        while (k < 925) {
            int dx = model.rng.Int(100), dy = model.rng.Int(100);
            if (model.GetAgent(dx,dy)==null) {
                model.NewAgentSQ(dx,dy).Init(1, model.divProbMN, model.dieProbMN,
                        0, model.migrProbM, model.activProbM);
                k++;
            }
        }

        // ---- initialise lung cells (random positions) ----
        int m=0;
        while (m < 1225) {
            int dx = model.rng.Int(100), dy = model.rng.Int(100);
            if (model.GetAgent(dx,dy)==null) {
                model.NewAgentSQ(dx,dy).Init(3, model.divProbL, model.dieProbL, 0, 0, 0);
                m++;
            }
        }

        // ---- initialise tumour cells — CLUSTERED SEEDING ----
        // Biologically: CTCs arrive as a small perivascular cluster,
        // not scattered randomly across the lung [H22 Fig.1a, P20 Fig.1c]
        //
        // Seed point: centre of grid ± small jitter so ABC runs aren't
        // all identical, but kept far enough from edges that the full
        // cluster fits.  Safe margin = clusterRadius + 2 from each edge.
        int margin = model.clusterRadius + 2;
        int jitterRange = 6;   // seed point wanders ±3 cells from centre
        // clamp so seed can never be within 'margin' of any edge
        int seedX = Math.max(margin, Math.min(x - 1 - margin,
                35 + model.rng.Int(jitterRange + 1) - jitterRange/2));
        int seedY = Math.max(margin, Math.min(y - 1 - margin,
                35 + model.rng.Int(jitterRange + 1) - jitterRange/2));

        int h=0, attempts=0;
        while (h < model.InitPop && attempts < 10000) {
            attempts++;

            // random offset within a square of side 2*radius+1
            int offX = model.rng.Int(2*model.clusterRadius + 1) - model.clusterRadius;
            int offY = model.rng.Int(2*model.clusterRadius + 1) - model.clusterRadius;

            // keep circular shape — reject corners of the bounding square
            if (offX*offX + offY*offY > model.clusterRadius*model.clusterRadius) continue;

            int dx = seedX + offX;
            int dy = seedY + offY;

            // boundary guard (should never fire given the margin above,
            // but defensive programming is good practice)
            if (dx < 1 || dx >= x-1 || dy < 1 || dy >= y-1) continue;

            if (model.GetAgent(dx, dy) == null) {
                h++;
                // 90% JNK+, 10% JNK-
                // [IR18 Fig.1F]: >50% active c-Jun in micrometastases at day 7,
                // consistent with majority of seeded cells being JNK+
                if (model.rng.Int(10) > 8) {
                    // JNK-
                    model.NewAgentSQ(dx, dy).Init(2, model.divProbN, model.dieProbN,
                            0, model.migrProbN, model.pOnMax);
                } else {
                    // JNK+
                    model.NewAgentSQ(dx, dy).Init(2, model.divProbP, model.dieProbP,
                            1, model.migrProbP, model.pOffMax);
                }
            }
        }

        if (h < model.InitPop) {
            System.err.println("WARNING [" + model.runId + "]: only placed "
                    + h + "/" + model.InitPop + " tumour cells. "
                    + "Increase clusterRadius (currently " + model.clusterRadius
                    + ") or reduce InitPop (currently " + model.InitPop + ").");
        }

        System.out.println("INFO [" + model.runId + "]: tumour cluster seeded at ("
                + seedX + "," + seedY + "), radius=" + model.clusterRadius
                + ", cells placed=" + h);

        // capture step-0 snapshot
        snapshots.add(model.DetailedCounts());

        // ---- main loop ----
        List<ArrayList> data = new ArrayList<>();

        for (int i=0; i<timeStep; i++) {
            model.IncTick();

            if (!model.headless) win.TickPause(100);

            if (i <= 2898) model.StepCellR();
            else           model.StepCellRChemo();

            // capture snapshots at key steps
            for (int s : snapshotSteps)
                if (i+1 == s) snapshots.add(model.DetailedCounts());

            ArrayList<Integer> tempCount = new ArrayList<>();
            data.add(model.MyCounter(tempCount));

            if (!model.headless) model.DrawModel(win);
        }

        // ---- write outputs ----

        // Basic count data (numDiv, numDie, totalPop per step)
        FileWriter w1 = new FileWriter("ContDeath" + model.runId + ".txt");
        w1.write(data.toString()); w1.close();

        // Detailed snapshots for ABC summary statistics
        // Format per line: step,tumJNKp,tumJNKn,ecActive,ecInact,
        //                        macActive,macInact,fibroActive,fibroInact
        FileWriter w2 = new FileWriter("DetailedCounts" + model.runId + ".txt");
        for (int s=0; s<snapshots.size(); s++) {
            int step = (s < snapshotSteps.length) ? snapshotSteps[s] : -1;
            int[] c  = snapshots.get(s);
            w2.write(step+","+c[0]+","+c[1]+","+c[2]+","
                    +c[3]+","+c[4]+","+c[5]+","
                    +c[6]+","+c[7]+"\n");
        }
        w2.close();

        if (!model.headless && win!=null) win.Close();
    }
}
