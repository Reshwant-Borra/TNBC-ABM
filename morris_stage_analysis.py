#!/usr/bin/env python3
"""Stage-level scientific analysis of Java Morris outputs.

Consumes the CSVs written by OnLatticeExample.MorrisSensitivitySweep in an
input directory and emits decision-ready tables:

  <PREFIX>TOP_PARAMETERS_BY_OUTPUT.csv   top-N params per output with EE stats
  <PREFIX>FAILURE_DRIVERS.csv            ranked failure/extinction/invalid drivers
  <PREFIX>PARAMETER_CLASSIFICATION.csv   one row per screened parameter

This performs no simulation and changes no model logic. It only reads and
summarises existing Morris artefacts. Registry metadata (status, bounds,
biology) is joined from GLOBAL_PARAMETER_REGISTRY.csv.
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

BIO_FAMILIES = ["tumor", "jnk", "fibroblast", "macrophage", "endothelial"]

# Failure outcomes that carry scientific meaning for this untreated screen.
FAILURE_OUTCOMES = [
    "TUMOR_EXTINCT", "EC_POPULATION_ZERO", "MACROPHAGE_POPULATION_ZERO",
    "FIBROBLAST_POPULATION_ZERO", "GENERAL_INVALID", "SIMULATION_ERROR",
]

CLASS_MEANING = {
    "A": "high mu*, low sigma: strong near-linear/additive effect",
    "B": "high mu*, high sigma: strong effect with nonlinearity/interaction/stochasticity",
    "C": "intermediate mu* with above-median variability: possible interaction",
    "D": "low mu* and low sigma: low influence within range",
    "E": "structurally inactive field (unused/blocked)",
    "F": "not screened or insufficient valid effects",
}


def load_registry(repo: Path) -> pd.DataFrame:
    reg = pd.read_csv(repo / "GLOBAL_PARAMETER_REGISTRY.csv")
    reg = reg.rename(columns={"canonical_name": "parameter"})
    reg["fixed_or_inferred"] = np.where(
        reg["current_status"].eq("ABC-inferred"), "inferred", "fixed")
    reg["in_abc12"] = reg["parameter"].isin(ABC12)
    return reg[[
        "parameter", "current_status", "fixed_or_inferred", "in_abc12",
        "baseline_value", "lower_bound", "upper_bound", "group",
        "biological_interpretation", "enter_morris_screen",
    ]]


def num(s):
    return pd.to_numeric(s, errors="coerce")


def effect_character(mu, mu_star, sigma, pos_frac, neg_frac):
    if not np.isfinite(mu_star) or mu_star == 0:
        return "inactive/zero-effect"
    ratio = sigma / mu_star if np.isfinite(sigma) else np.nan
    sign_varying = (min(pos_frac, neg_frac) >= 0.25) if (
        np.isfinite(pos_frac) and np.isfinite(neg_frac)) else False
    if not np.isfinite(ratio):
        base = "single-effect (sigma undefined)"
    elif ratio < 0.5:
        base = "consistent (near-linear/additive)"
    elif ratio < 1.0:
        base = "moderate nonlinearity/interaction"
    else:
        base = "strong nonlinearity/interaction-dependent"
    if sign_varying:
        base += "; sign-varying"
    return base


def top_by_output(summary: pd.DataFrame, reg: pd.DataFrame, top_n: int) -> pd.DataFrame:
    s = summary.copy()
    for c in ["mu", "mu_star", "sigma", "n_valid_ee", "n_lost_invalid_or_extinct",
              "positive_fraction", "negative_fraction", "mean_signal_to_noise_ratio",
              "normalized_mu_star"]:
        s[c] = num(s[c])
    s["valid_fraction"] = s["n_valid_ee"] / (
        s["n_valid_ee"] + s["n_lost_invalid_or_extinct"]).replace(0, np.nan)
    s["lost_fraction"] = 1 - s["valid_fraction"]
    s["sigma_to_mustar"] = s["sigma"] / s["mu_star"].replace(0, np.nan)
    rows = []
    for output, grp in s.groupby("output", sort=True):
        grp = grp[np.isfinite(grp["mu_star"])].nlargest(top_n, "mu_star")
        for rank, (_, r) in enumerate(grp.iterrows(), start=1):
            rows.append({
                "output": output,
                "output_family": r["output_family"],
                "rank_in_output": rank,
                "parameter": r["parameter"],
                "mu_signed": r["mu"],
                "mu_star": r["mu_star"],
                "sigma": r["sigma"],
                "sigma_to_mustar": r["sigma_to_mustar"],
                "n_valid_ee": r["n_valid_ee"],
                "valid_fraction": r["valid_fraction"],
                "n_lost": r["n_lost_invalid_or_extinct"],
                "lost_fraction": r["lost_fraction"],
                "mean_snr": r["mean_signal_to_noise_ratio"],
                "normalized_mu_star": r["normalized_mu_star"],
                "positive_fraction": r["positive_fraction"],
                "negative_fraction": r["negative_fraction"],
                "effect_character": effect_character(
                    r["mu"], r["mu_star"], r["sigma"],
                    r["positive_fraction"], r["negative_fraction"]),
            })
    out = pd.DataFrame(rows)
    out = out.merge(reg[["parameter", "current_status", "fixed_or_inferred", "in_abc12"]],
                    on="parameter", how="left")
    return out


def failure_drivers(failure: pd.DataFrame, reg: pd.DataFrame, top_n: int) -> pd.DataFrame:
    f = failure.copy()
    for c in ["rank_biserial_effect", "point_biserial_correlation",
              "univariate_logistic_coefficient", "permutation_p_value",
              "n_event", "n_no_event", "normalized_mean_difference"]:
        f[c] = num(f[c])
    f = f[f["outcome"].isin(FAILURE_OUTCOMES)]
    f = f[np.isfinite(f["rank_biserial_effect"])]
    f["abs_rb"] = f["rank_biserial_effect"].abs()
    f["direction"] = np.where(
        f["rank_biserial_effect"] > 0,
        "higher-value -> more likely event",
        "higher-value -> less likely event")
    rows = []
    for outcome, grp in f.groupby("outcome"):
        grp = grp.nlargest(top_n, "abs_rb")
        for rank, (_, r) in enumerate(grp.iterrows(), start=1):
            rows.append({
                "outcome": outcome,
                "rank_in_outcome": rank,
                "parameter": r["parameter"],
                "n_event": r["n_event"],
                "n_no_event": r["n_no_event"],
                "rank_biserial_effect": r["rank_biserial_effect"],
                "direction": r["direction"],
                "point_biserial_correlation": r["point_biserial_correlation"],
                "normalized_mean_difference": r["normalized_mean_difference"],
                "univariate_logistic_coefficient": r["univariate_logistic_coefficient"],
                "permutation_p_value": r["permutation_p_value"],
            })
    out = pd.DataFrame(rows)
    out = out.merge(reg[["parameter", "current_status", "fixed_or_inferred",
                         "in_abc12", "baseline_value", "lower_bound", "upper_bound"]],
                    on="parameter", how="left")
    return out


def parameter_classification(summary, classification, glob, failure, reg, repo) -> pd.DataFrame:
    s = summary.copy()
    for c in ["mu_star", "sigma", "normalized_mu_star", "n_valid_ee",
              "n_lost_invalid_or_extinct", "rank_by_mu_star"]:
        s[c] = num(s[c])
    s["valid_fraction"] = s["n_valid_ee"] / (
        s["n_valid_ee"] + s["n_lost_invalid_or_extinct"]).replace(0, np.nan)
    s["sigma_ratio"] = s["sigma"] / s["mu_star"].replace(0, np.nan)
    bio = s[s["output_family"].isin(BIO_FAMILIES)]

    cls = classification.copy()
    cls_bio = cls[cls["output_family"].isin(BIO_FAMILIES)]
    class_rank = {"A": 4, "B": 3, "C": 2, "D": 1, "E": 0, "F": 0}

    fail = failure.copy()
    fail["rank_biserial_effect"] = num(fail["rank_biserial_effect"])
    fail = fail[fail["outcome"].isin(FAILURE_OUTCOMES) & np.isfinite(fail["rank_biserial_effect"])]

    g = glob.set_index("parameter")
    reg_screen = reg[reg["enter_morris_screen"].astype(str).str.lower().eq("true")]

    n_params = len(reg_screen)
    rows = []
    for _, rr in reg_screen.iterrows():
        p = rr["parameter"]
        pb = bio[bio["parameter"] == p]
        pcls = cls_bio[cls_bio["parameter"] == p]
        classes = list(pcls["classification"].dropna())
        peak = max(classes, key=lambda c: class_rank.get(c, 0)) if classes else "F"
        n_A = sum(c == "A" for c in classes)
        n_B = sum(c == "B" for c in classes)
        n_bio = len(pb)
        led = pb[pb["rank_by_mu_star"] <= 10]  # outputs this parameter leads (top-10)
        n_top10 = int((pb["rank_by_mu_star"] <= 10).sum()) if n_bio else 0
        n_top5 = int((pb["rank_by_mu_star"] <= 5).sum()) if n_bio else 0
        frac_top10 = n_top10 / n_bio if n_bio else 0.0
        frac_classAB = (n_A + n_B) / n_bio if n_bio else 0.0
        max_norm = float(np.nanmax(pb["normalized_mu_star"])) if n_bio else np.nan
        # sigma/mu* ratio only over the outputs the parameter actually leads (meaningful)
        med_ratio = float(np.nanmedian(led["sigma_ratio"])) if len(led) else (
            float(np.nanmedian(pb["sigma_ratio"])) if n_bio else np.nan)
        mean_valid = float(np.nanmean(pb["valid_fraction"])) if n_bio else np.nan
        rank_overall = int(g.loc[p, "overall_rank"]) if p in g.index else np.nan
        pf = fail[fail["parameter"] == p]
        if len(pf):
            best = pf.iloc[pf["rank_biserial_effect"].abs().argmax()]
            fail_out, fail_rb = best["outcome"], float(best["rank_biserial_effect"])
        else:
            fail_out, fail_rb = "", np.nan
        # top affected biological outputs (by normalized mu*)
        if n_bio:
            top_outs = pb.sort_values("normalized_mu_star", ascending=False)["output"].head(3).tolist()
        else:
            top_outs = []
        # relative influence tier by overall biological-priority rank tertile (over screened set)
        if not np.isfinite(rank_overall) or peak in ("E", "F") or (np.isfinite(mean_valid) and mean_valid < 0.5):
            label = "INSUFFICIENT_OR_INACTIVE"
        elif rank_overall <= n_params / 3:
            label = "HIGH_INFLUENCE"
        elif rank_overall <= 2 * n_params / 3:
            label = "MODERATE_INFLUENCE"
        else:
            label = "LOW_INFLUENCE_WITHIN_RANGE"
        rows.append({
            "parameter": p,
            "biological_category": rr["group"],
            "current_status": rr["current_status"],
            "fixed_or_inferred": rr["fixed_or_inferred"],
            "in_abc12": rr["in_abc12"],
            "baseline": rr["baseline_value"],
            "lower_bound": rr["lower_bound"],
            "upper_bound": rr["upper_bound"],
            "overall_rank": int(g.loc[p, "overall_rank"]) if p in g.index else np.nan,
            "overall_priority_score": float(g.loc[p, "overall_biological_priority_score"]) if p in g.index else np.nan,
            "tumor_score": float(g.loc[p, "tumor_score"]) if p in g.index else np.nan,
            "jnk_score": float(g.loc[p, "jnk_score"]) if p in g.index else np.nan,
            "fibroblast_score": float(g.loc[p, "fibroblast_score"]) if p in g.index else np.nan,
            "macrophage_score": float(g.loc[p, "macrophage_score"]) if p in g.index else np.nan,
            "endothelial_score": float(g.loc[p, "endothelial_score"]) if p in g.index else np.nan,
            "failure_score": float(g.loc[p, "failure_score"]) if p in g.index else np.nan,
            "peak_class": peak,
            "peak_class_meaning": CLASS_MEANING.get(peak, ""),
            "n_bio_outputs": n_bio,
            "n_outputs_class_A": n_A,
            "n_outputs_class_B": n_B,
            "n_bio_outputs_top5": n_top5,
            "n_bio_outputs_top10": n_top10,
            "frac_bio_outputs_top10": frac_top10,
            "frac_bio_outputs_classAB": frac_classAB,
            "max_normalized_mu_star": max_norm,
            "median_sigma_to_mustar": med_ratio,
            "mean_valid_fraction": mean_valid,
            "strongest_failure_outcome": fail_out,
            "strongest_failure_rank_biserial": fail_rb,
            "top_affected_bio_outputs": ";".join(top_outs),
            "classification": label,
        })
    out = pd.DataFrame(rows).sort_values("overall_rank")
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--input-dir", type=Path, required=True)
    ap.add_argument("--prefix", default="")
    ap.add_argument("--top-n", type=int, default=10)
    ap.add_argument("--repo", type=Path, default=Path("."))
    args = ap.parse_args()

    d = args.input_dir
    reg = load_registry(args.repo)
    summary = pd.read_csv(d / "morris_summary_by_output.csv")
    classification = pd.read_csv(d / "PARAMETER_INFLUENCE_CLASSIFICATION.csv")
    glob = pd.read_csv(d / "morris_global_rankings.csv")
    failure = pd.read_csv(d / "failure_sensitivity.csv")

    top = top_by_output(summary, reg, args.top_n)
    top.to_csv(d / f"{args.prefix}TOP_PARAMETERS_BY_OUTPUT.csv", index=False)

    fail = failure_drivers(failure, reg, args.top_n + 5)
    fail.to_csv(d / f"{args.prefix}FAILURE_DRIVERS.csv", index=False)

    pc = parameter_classification(summary, classification, glob, failure, reg, args.repo)
    pc.to_csv(d / f"{args.prefix}PARAMETER_CLASSIFICATION.csv", index=False)

    print(f"wrote {args.prefix}TOP_PARAMETERS_BY_OUTPUT.csv ({len(top)} rows)")
    print(f"wrote {args.prefix}FAILURE_DRIVERS.csv ({len(fail)} rows)")
    print(f"wrote {args.prefix}PARAMETER_CLASSIFICATION.csv ({len(pc)} rows)")
    # quick provenance echo
    print("\nGlobal top-12 by overall_biological_priority_score:")
    print(glob.sort_values("overall_rank").head(12)[
        ["overall_rank", "parameter", "current_status", "overall_biological_priority_score"]
    ].to_string(index=False))


if __name__ == "__main__":
    main()
