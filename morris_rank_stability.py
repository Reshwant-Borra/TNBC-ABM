#!/usr/bin/env python3
"""Compare Morris rankings between two stages (e.g. 10-traj pilot vs 20-traj).

Emits:
  <out>/RANK_STABILITY_<A>_VS_<B>.csv   per-parameter rank movement + per-family
  <out>/RANK_STABILITY_REPORT.md        narrative: Spearman, overlaps, movers

Reads only existing Morris CSVs; runs no simulation.
"""
from __future__ import annotations

import argparse
from pathlib import Path

import numpy as np
import pandas as pd

try:
    from scipy.stats import spearmanr
    def spearman(x, y):
        if len(x) < 2:
            return np.nan
        return float(spearmanr(x, y).statistic)
except Exception:
    def spearman(x, y):
        if len(x) < 2:
            return np.nan
        a = pd.Series(x).rank()
        b = pd.Series(y).rank()
        return float(a.corr(b))

ABC12 = {
    "netN", "dieProbN", "pOnMax", "pOffMax", "divProbP", "dieProbP",
    "cafDivBoost", "ecSurvival", "activProbF", "divProbFP", "activProbM", "activProbE",
}
FAMILIES = ["tumor_score", "jnk_score", "fibroblast_score", "macrophage_score",
            "endothelial_score", "failure_score"]
FAIL_OUTCOMES = ["TUMOR_EXTINCT", "EC_POPULATION_ZERO", "MACROPHAGE_POPULATION_ZERO",
                 "GENERAL_INVALID"]


def overlap(a_rank: pd.Series, b_rank: pd.Series, k: int) -> float:
    ta = set(a_rank.nsmallest(k).index)
    tb = set(b_rank.nsmallest(k).index)
    return len(ta & tb) / k


def load_glob(d: Path) -> pd.DataFrame:
    g = pd.read_csv(d / "morris_global_rankings.csv").set_index("parameter")
    return g


def failure_rank(d: Path) -> pd.DataFrame:
    f = pd.read_csv(d / "failure_sensitivity.csv")
    f["rank_biserial_effect"] = pd.to_numeric(f["rank_biserial_effect"], errors="coerce")
    f = f[f["outcome"].isin(FAIL_OUTCOMES) & np.isfinite(f["rank_biserial_effect"])].copy()
    f["abs_rb"] = f["rank_biserial_effect"].abs()
    f["rank"] = f.groupby("outcome")["abs_rb"].rank(ascending=False, method="min")
    return f


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--dir-a", type=Path, required=True, help="baseline stage (e.g. pilot 10)")
    ap.add_argument("--dir-b", type=Path, required=True, help="comparison stage (e.g. primary 20)")
    ap.add_argument("--label-a", default="10traj")
    ap.add_argument("--label-b", default="20traj")
    ap.add_argument("--out-dir", type=Path, required=True)
    args = ap.parse_args()

    A, B = load_glob(args.dir_a), load_glob(args.dir_b)
    params = sorted(set(A.index) & set(B.index))
    A, B = A.loc[params], B.loc[params]

    df = pd.DataFrame(index=params)
    df["current_status_a"] = A["current_status"]
    df["current_status_b"] = B["current_status"]
    df["in_abc12"] = [p in ABC12 for p in params]
    df["rank_a"] = A["overall_rank"]
    df["rank_b"] = B["overall_rank"]
    df["rank_change_b_minus_a"] = df["rank_b"] - df["rank_a"]
    df["abs_rank_change"] = df["rank_change_b_minus_a"].abs()
    df["score_a"] = A["overall_biological_priority_score"]
    df["score_b"] = B["overall_biological_priority_score"]
    df["score_change"] = df["score_b"] - df["score_a"]
    for fam in FAMILIES:
        df[f"{fam}_a"] = A[fam]
        df[f"{fam}_b"] = B[fam]
    df = df.sort_values("rank_b")
    df.to_csv(args.out_dir / f"RANK_STABILITY_{args.label_a}_VS_{args.label_b}.csv")

    # overall Spearman on priority score
    rho_overall = spearman(df["score_a"].values, df["score_b"].values)
    # per-family Spearman
    fam_rho = {fam: spearman(df[f"{fam}_a"].values, df[f"{fam}_b"].values) for fam in FAMILIES}
    # overlaps on overall rank (rank 1 = best -> nsmallest)
    ra = df["rank_a"]
    rb = df["rank_b"]
    ov = {k: overlap(ra, rb, k) for k in (5, 10, 15)}

    # movers
    movers = df.sort_values("abs_rank_change", ascending=False)
    top_movers = movers.head(10)
    # entering/leaving top-10
    top10_a = set(df.index[df["rank_a"] <= 10])
    top10_b = set(df.index[df["rank_b"] <= 10])
    entered = sorted(top10_b - top10_a, key=lambda p: df.loc[p, "rank_b"])
    left = sorted(top10_a - top10_b, key=lambda p: df.loc[p, "rank_a"])
    top5_a = set(df.index[df["rank_a"] <= 5])
    top5_b = set(df.index[df["rank_b"] <= 5])

    # failure driver ranking changes
    fa = failure_rank(args.dir_a)
    fb = failure_rank(args.dir_b)
    fail_lines = []
    for oc in FAIL_OUTCOMES:
        sa = fa[fa["outcome"] == oc].set_index("parameter")["rank"]
        sb = fb[fb["outcome"] == oc].set_index("parameter")["rank"]
        common = sorted(set(sa.index) & set(sb.index))
        if len(common) >= 2:
            rho = spearman(sa.loc[common].values, sb.loc[common].values)
        else:
            rho = np.nan
        ta = set(sa.nsmallest(5).index) if len(sa) else set()
        tb = set(sb.nsmallest(5).index) if len(sb) else set()
        ov5 = len(ta & tb) / 5 if tb else np.nan
        top_b = ", ".join(f"{p}" for p in sb.nsmallest(5).index) if len(sb) else "(none)"
        fail_lines.append(
            f"- **{oc}**: Spearman(rank) = {rho:.3f} over {len(common)} shared params; "
            f"top-5 overlap = {ov5:.2f}. {args.label_b} top-5: {top_b}.")

    # fixed vs inferred conclusion changes among top-10
    def infl_set(d, thr=10):
        return {p for p in d.index if d.loc[p, "rank_b" if d is df else "rank_b"] <= thr}

    lines = []
    lines.append(f"# Ranking Stability: {args.label_a} vs {args.label_b}\n")
    lines.append(f"Comparison of Morris overall biological-priority rankings between "
                 f"`{args.dir_a}` ({args.label_a}) and `{args.dir_b}` ({args.label_b}). "
                 f"{len(params)} screened parameters in common.\n")
    lines.append("## Convergence is judged on multiple axes, not one cutoff\n")
    lines.append(f"- **Overall Spearman** (priority score, {args.label_a} vs {args.label_b}): "
                 f"**{rho_overall:.3f}**")
    lines.append(f"- **Top-5 overlap**: {ov[5]:.2f} ({int(ov[5]*5)}/5)")
    lines.append(f"- **Top-10 overlap**: {ov[10]:.2f} ({int(ov[10]*10)}/10)")
    lines.append(f"- **Top-15 overlap**: {ov[15]:.2f} ({int(ov[15]*15)}/15)")
    lines.append(f"- **Median |rank change|**: {df['abs_rank_change'].median():.1f}; "
                 f"**max |rank change|**: {int(df['abs_rank_change'].max())}\n")
    lines.append("## Per-family Spearman (family priority scores)\n")
    for fam, rho in fam_rho.items():
        lines.append(f"- {fam.replace('_score','')}: {rho:.3f}")
    lines.append("")
    lines.append("## Parameters entering / leaving the top-10\n")
    lines.append(f"- **Entered** top-10 in {args.label_b}: "
                 + (", ".join(f"{p} (rank {int(df.loc[p,'rank_b'])})" for p in entered) if entered else "none"))
    lines.append(f"- **Left** top-10 in {args.label_b}: "
                 + (", ".join(f"{p} (was rank {int(df.loc[p,'rank_a'])})" for p in left) if left else "none"))
    lines.append(f"- Top-5 stable set: {sorted(top5_a & top5_b)}; changed: "
                 f"in {args.label_a} only {sorted(top5_a-top5_b)}, in {args.label_b} only {sorted(top5_b-top5_a)}\n")
    lines.append("## Largest rank movers\n")
    lines.append("| parameter | status | rank " + args.label_a + " | rank " + args.label_b + " | change |")
    lines.append("|---|---|---:|---:|---:|")
    for p, r in top_movers.iterrows():
        lines.append(f"| {p} | {r['current_status_b']} | {int(r['rank_a'])} | "
                     f"{int(r['rank_b'])} | {int(r['rank_change_b_minus_a']):+d} |")
    lines.append("")
    lines.append("## Failure / extinction driver ranking stability\n")
    lines.extend(fail_lines)
    lines.append("")
    lines.append("## Fixed-vs-inferred conclusion check (top-10)\n")
    fixed_top_b = [p for p in df.index if df.loc[p, "rank_b"] <= 10 and p not in ABC12]
    inf_top_b = [p for p in df.index if df.loc[p, "rank_b"] <= 10 and p in ABC12]
    lines.append(f"- Currently-fixed parameters in {args.label_b} top-10: {fixed_top_b}")
    lines.append(f"- ABC-inferred parameters in {args.label_b} top-10: {inf_top_b}")
    lines.append("")

    (args.out_dir / "RANK_STABILITY_REPORT.md").write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"Overall Spearman = {rho_overall:.3f}; top5/10/15 overlap = "
          f"{ov[5]:.2f}/{ov[10]:.2f}/{ov[15]:.2f}")
    print(f"wrote RANK_STABILITY_{args.label_a}_VS_{args.label_b}.csv and RANK_STABILITY_REPORT.md")


if __name__ == "__main__":
    main()
