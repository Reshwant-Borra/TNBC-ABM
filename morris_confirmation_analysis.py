#!/usr/bin/env python3
"""One-seed vs three-seed confirmation analysis.

Reads morris_confirmed_rankings.csv (written by --confirm-only) and the primary
one-seed summary, and emits:
  CONFIRMED_PARAMETER_EFFECTS.csv     per-parameter reproducibility summary
  CONFIRMATION_STABILITY_REPORT.md    one-vs-three-seed stability narrative

No simulation is run here.
"""
from __future__ import annotations

import argparse
from pathlib import Path

import numpy as np
import pandas as pd

ABC12 = {
    "netN", "dieProbN", "pOnMax", "pOffMax", "divProbP", "dieProbP",
    "cafDivBoost", "ecSurvival", "activProbF", "divProbFP", "activProbM", "activProbE",
}


def num(s):
    return pd.to_numeric(s, errors="coerce")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--primary-dir", type=Path, required=True)
    ap.add_argument("--out-dir", type=Path, required=True)
    args = ap.parse_args()

    conf = pd.read_csv(args.primary_dir / "morris_confirmed_rankings.csv")
    prim = pd.read_csv(args.primary_dir / "morris_summary_by_output.csv")
    for c in ["one_seed_mu_star", "multi_seed_mu_star", "multi_seed_mu_star_ci_low",
              "multi_seed_mu_star_ci_high", "one_seed_restricted_rank", "multi_seed_rank",
              "rank_change", "multi_seed_sigma", "multi_seed_mean_snr",
              "spearman_rank_correlation", "n_valid_multi_seed", "n_lost_multi_seed"]:
        conf[c] = num(conf[c])

    # join one-seed sigma / snr from primary summary
    prim_small = prim[["output", "parameter", "sigma", "mean_signal_to_noise_ratio"]].copy()
    prim_small = prim_small.rename(columns={"sigma": "one_seed_sigma",
                                            "mean_signal_to_noise_ratio": "one_seed_snr"})
    prim_small["one_seed_sigma"] = num(prim_small["one_seed_sigma"])
    prim_small["one_seed_snr"] = num(prim_small["one_seed_snr"])
    m = conf.merge(prim_small, on=["output", "parameter"], how="left")
    m["mu_star_ratio"] = m["multi_seed_mu_star"] / m["one_seed_mu_star"].replace(0, np.nan)
    m["ci_width"] = m["multi_seed_mu_star_ci_high"] - m["multi_seed_mu_star_ci_low"]
    m["ci_rel"] = m["ci_width"] / m["multi_seed_mu_star"].replace(0, np.nan)
    m["sigma_ratio"] = m["multi_seed_sigma"] / m["one_seed_sigma"].replace(0, np.nan)

    # per-parameter aggregation
    rows = []
    for p, g in m.groupby("parameter"):
        rows.append({
            "parameter": p,
            "in_abc12": p in ABC12,
            "n_outputs": len(g),
            "median_one_seed_mu_star": g["one_seed_mu_star"].median(),
            "median_multi_seed_mu_star": g["multi_seed_mu_star"].median(),
            "median_mu_star_ratio_3seed_over_1seed": g["mu_star_ratio"].median(),
            "median_multi_seed_sigma": g["multi_seed_sigma"].median(),
            "median_sigma_ratio_3seed_over_1seed": g["sigma_ratio"].median(),
            "median_multi_seed_snr": g["multi_seed_mean_snr"].median(),
            "median_abs_rank_change": g["rank_change"].abs().median(),
            "mean_rank_change": g["rank_change"].mean(),
            "median_ci_rel_width": g["ci_rel"].median(),
            "median_valid_pairs": g["n_valid_multi_seed"].median(),
            "median_lost_pairs": g["n_lost_multi_seed"].median(),
        })
    eff = pd.DataFrame(rows).sort_values("median_multi_seed_mu_star", ascending=False)
    eff.to_csv(args.out_dir / "CONFIRMED_PARAMETER_EFFECTS.csv", index=False)

    # per-output Spearman (one row per output; take first)
    per_out = conf.groupby("output")["spearman_rank_correlation"].first().dropna()
    med_rho = per_out.median() if len(per_out) else np.nan

    params = sorted(m["parameter"].unique())
    lines = []
    lines.append("# Confirmation Stability: one-seed vs three matched seeds\n")
    lines.append(f"Targeted 3-seed confirmation reused the primary 20-trajectory design and re-ran only the "
                 f"neighbouring points needed for **{len(params)} selected parameters**. Comparison uses the "
                 f"pipeline's `morris_confirmed_rankings.csv` (restricted one-seed vs three-seed) joined to the "
                 f"primary one-seed summary.\n")
    lines.append("## Rank agreement (one-seed vs three-seed)\n")
    lines.append(f"- **Median per-output Spearman rank correlation**: {med_rho:.3f} "
                 f"(over {len(per_out)} outputs with >=2 shared parameters)")
    lines.append(f"- Per-output Spearman quartiles: "
                 f"Q1={per_out.quantile(.25):.3f}, median={per_out.median():.3f}, Q3={per_out.quantile(.75):.3f}")
    absrc = m["rank_change"].abs()
    lines.append(f"- **|rank change|** across all restricted rows: median={absrc.median():.1f}, "
                 f"mean={absrc.mean():.2f}, 90th pct={absrc.quantile(.9):.1f}, max={int(absrc.max())}")
    lines.append(f"- Rows with unchanged restricted rank: {(m['rank_change']==0).mean()*100:.1f}%\n")
    lines.append("## Effect-size stability (mu*)\n")
    mr = m["mu_star_ratio"].replace([np.inf, -np.inf], np.nan).dropna()
    lines.append(f"- **Median mu\\* ratio (3-seed / 1-seed)**: {mr.median():.3f} "
                 f"(1.0 = unchanged); IQR [{mr.quantile(.25):.3f}, {mr.quantile(.75):.3f}]")
    lines.append(f"- Fraction of rows where 3-seed mu\\* within +/-25% of 1-seed: "
                 f"{((mr>=0.75)&(mr<=1.25)).mean()*100:.1f}%\n")
    lines.append("## Noise and reproducibility\n")
    snr = m["multi_seed_mean_snr"].replace([np.inf, -np.inf], np.nan).dropna()
    lines.append(f"- **Three-seed mean SNR** (mu\\* vs within-point stochastic noise): "
                 f"median={snr.median():.2f}, Q1={snr.quantile(.25):.2f}, Q3={snr.quantile(.75):.2f}")
    lines.append(f"- Rows with SNR >= 1 (signal exceeds stochastic noise): {(snr>=1).mean()*100:.1f}%")
    sr = m["sigma_ratio"].replace([np.inf, -np.inf], np.nan).dropna()
    if len(sr):
        lines.append(f"- **Sigma ratio (3-seed / 1-seed)**: median={sr.median():.3f} "
                     f"(values <1 indicate 1-seed sigma partly reflected stochastic noise now averaged out)")
    lines.append(f"- **Valid multi-seed pairs** per row: median={m['n_valid_multi_seed'].median():.0f}, "
                 f"lost: median={m['n_lost_multi_seed'].median():.0f}\n")
    lines.append("## Per-parameter reproducibility (top by three-seed mu*)\n")
    lines.append("| parameter | ABC12 | 3-seed mu* (med) | mu* ratio | 3-seed SNR (med) | |rank chg| med | CI rel |")
    lines.append("|---|:--:|---:|---:|---:|---:|---:|")
    for _, r in eff.head(20).iterrows():
        lines.append(f"| {r['parameter']} | {'Y' if r['in_abc12'] else ''} | "
                     f"{r['median_multi_seed_mu_star']:.4g} | {r['median_mu_star_ratio_3seed_over_1seed']:.2f} | "
                     f"{r['median_multi_seed_snr']:.2f} | {r['median_abs_rank_change']:.1f} | "
                     f"{r['median_ci_rel_width']:.2f} |")
    lines.append("")
    lines.append("## Interpretation\n")
    lines.append("- High per-output Spearman and small |rank change| mean the one-seed 20-trajectory ranks are "
                 "reproducible under matched-seed replication: stochasticity is not driving the ordering.")
    lines.append("- mu\\* ratios near 1 confirm the single-seed magnitudes are not seed artefacts. Ratios far "
                 "from 1 with low SNR flag parameters whose apparent effect was partly stochastic.")
    lines.append("- SNR < 1 with high sigma is the signature of stochasticity/interaction rather than a stable "
                 "monotone effect; treat those parameters' magnitudes cautiously.")
    (args.out_dir / "CONFIRMATION_STABILITY_REPORT.md").write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"median per-output Spearman(one vs three seed) = {med_rho:.3f}")
    print(f"wrote CONFIRMED_PARAMETER_EFFECTS.csv ({len(eff)} params) and CONFIRMATION_STABILITY_REPORT.md")


if __name__ == "__main__":
    main()
