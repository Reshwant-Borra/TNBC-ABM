#!/usr/bin/env python3
"""Analyze and visualize Java Morris outputs without seaborn."""

from __future__ import annotations

import argparse
import json
import math
import re
from pathlib import Path

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

try:
    from scipy.stats import spearmanr
except Exception:  # optional dependency
    spearmanr = None


FAMILIES = {"tumor", "jnk", "fibroblast", "macrophage", "endothelial", "failure"}
COLORS = {
    "tumor": "#b23a48", "jnk": "#2a6f97", "fibroblast": "#d17a22",
    "macrophage": "#4f772d", "endothelial": "#6a4c93", "failure": "#343a40",
    "population": "#64748b", "abc": "#7f5539",
}


def safe_name(value: str) -> str:
    value = re.sub(r"[^A-Za-z0-9_.-]+", "_", value).strip("_")
    return value[:180] or "unnamed"


def finite_frame(frame: pd.DataFrame, columns: list[str]) -> pd.DataFrame:
    out = frame.copy()
    for column in columns:
        out[column] = pd.to_numeric(out[column], errors="coerce")
    return out.replace([np.inf, -np.inf], np.nan).dropna(subset=columns)


def save(fig: plt.Figure, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    fig.savefig(path, dpi=160, bbox_inches="tight", facecolor="white")
    plt.close(fig)


def mu_sigma_figures(summary: pd.DataFrame, root: Path) -> int:
    count = 0
    for output, data in summary.groupby("output", sort=True):
        data = finite_frame(data, ["mu_star", "sigma"])
        if data.empty:
            continue
        family = str(data["output_family"].iloc[0])
        fig, ax = plt.subplots(figsize=(8.5, 6.2))
        ax.scatter(data["mu_star"], data["sigma"], s=34, color=COLORS.get(family, "#334155"), alpha=.82)
        labels = data.nlargest(min(12, len(data)), "mu_star")
        for _, row in labels.iterrows():
            ax.annotate(row["parameter"], (row["mu_star"], row["sigma"]), xytext=(4, 3),
                        textcoords="offset points", fontsize=7)
        ax.set(title=f"Morris effects: {output}", xlabel="mu* (mean absolute elementary effect)", ylabel="sigma")
        ax.grid(alpha=.22)
        save(fig, root / "mu_star_sigma" / f"{safe_name(output)}.png")
        count += 1
    return count


def top_bar_figures(summary: pd.DataFrame, root: Path, top_n: int) -> int:
    count = 0
    for output, data in summary.groupby("output", sort=True):
        data = finite_frame(data, ["mu_star"]).nlargest(top_n, "mu_star").sort_values("mu_star")
        if data.empty:
            continue
        family = str(data["output_family"].iloc[0])
        height = max(4.0, .31 * len(data) + 1.7)
        fig, ax = plt.subplots(figsize=(9.4, height))
        ax.barh(data["parameter"], data["mu_star"], color=COLORS.get(family, "#334155"), alpha=.9)
        ax.set(title=f"Top Morris drivers: {output}", xlabel="mu*", ylabel="")
        ax.grid(axis="x", alpha=.2)
        save(fig, root / "top_mu_star" / f"{safe_name(output)}.png")
        count += 1
    return count


def heatmap(summary: pd.DataFrame, root: Path) -> int:
    data = finite_frame(summary, ["normalized_mu_star"])
    data = data[data["output_family"].isin(FAMILIES - {"failure"})]
    if data.empty:
        return 0
    pivot = data.pivot_table(index="parameter", columns="output", values="normalized_mu_star", aggfunc="mean")
    importance = pivot.max(axis=1).sort_values(ascending=False)
    pivot = pivot.loc[importance.index]
    fig_w = max(13, .20 * pivot.shape[1] + 5)
    fig_h = max(10, .24 * pivot.shape[0] + 3)
    fig, ax = plt.subplots(figsize=(fig_w, fig_h))
    image = ax.imshow(pivot.fillna(0).to_numpy(), aspect="auto", interpolation="nearest", cmap="viridis")
    ax.set_xticks(np.arange(pivot.shape[1]), labels=pivot.columns, rotation=90, fontsize=6)
    ax.set_yticks(np.arange(pivot.shape[0]), labels=pivot.index, fontsize=7)
    ax.set(title="Normalized Morris mu* across parameters and outputs", xlabel="Output", ylabel="Parameter")
    fig.colorbar(image, ax=ax, label="mu* / output SD")
    save(fig, root / "normalized_mu_star_heatmap.png")
    return 1


def failure_figure(failure: pd.DataFrame, root: Path) -> int:
    data = finite_frame(failure, ["rank_biserial_effect"])
    if data.empty:
        fig, ax = plt.subplots(figsize=(8, 4))
        ax.text(.5, .5, "No failure outcome had both event and non-event runs", ha="center", va="center")
        ax.axis("off")
        save(fig, root / "failure_extinction_sensitivity.png")
        return 1
    pivot = data.pivot_table(index="parameter", columns="outcome", values="rank_biserial_effect", aggfunc="mean")
    pivot = pivot.loc[pivot.abs().max(axis=1).sort_values(ascending=False).index]
    fig, ax = plt.subplots(figsize=(10, max(7, .27 * len(pivot) + 2)))
    image = ax.imshow(pivot.fillna(0).to_numpy(), aspect="auto", cmap="coolwarm", vmin=-1, vmax=1)
    ax.set_xticks(np.arange(pivot.shape[1]), labels=pivot.columns, rotation=35, ha="right")
    ax.set_yticks(np.arange(pivot.shape[0]), labels=pivot.index, fontsize=7)
    ax.set(title="Failure and extinction sensitivity", xlabel="Outcome", ylabel="Parameter")
    fig.colorbar(image, ax=ax, label="Rank-biserial effect")
    save(fig, root / "failure_extinction_sensitivity.png")
    return 1


def rank_comparison(input_dir: Path, root: Path) -> int:
    path = input_dir / "morris_confirmed_rankings.csv"
    fig, ax = plt.subplots(figsize=(7.5, 6.5))
    if not path.exists():
        ax.text(.5, .5, "Multi-seed confirmation not yet available", ha="center", va="center")
        ax.axis("off")
    else:
        data = finite_frame(pd.read_csv(path), ["one_seed_restricted_rank", "multi_seed_rank"])
        ax.scatter(data["one_seed_restricted_rank"], data["multi_seed_rank"], s=24, alpha=.65, color="#2a6f97")
        limit = max(data[["one_seed_restricted_rank", "multi_seed_rank"]].max().max(), 1)
        ax.plot([1, limit], [1, limit], color="#b23a48", lw=1.2)
        rho = np.nan
        if len(data) > 1:
            rho = spearmanr(data["one_seed_restricted_rank"], data["multi_seed_rank"]).statistic if spearmanr else data[["one_seed_restricted_rank", "multi_seed_rank"]].corr(method="spearman").iloc[0, 1]
        ax.set(title=f"One-seed vs multi-seed ranks (Spearman={rho:.3g})", xlabel="One-seed restricted rank", ylabel="Multi-seed rank")
        ax.grid(alpha=.2)
    save(fig, root / "one_vs_multi_seed_rank_comparison.png")
    return 1


def ee_distributions(summary: pd.DataFrame, effects: pd.DataFrame, root: Path, top_n: int) -> int:
    count = 0
    mean_effects = effects[(effects["ee_kind"] == "replicate_mean") & effects["valid"].astype(str).str.lower().eq("true")].copy()
    mean_effects["elementary_effect"] = pd.to_numeric(mean_effects["elementary_effect"], errors="coerce")
    for output, ranked in summary.groupby("output", sort=True):
        top = finite_frame(ranked, ["mu_star"]).nlargest(min(top_n, len(ranked)), "mu_star")["parameter"].tolist()
        data = mean_effects[(mean_effects["output"] == output) & mean_effects["parameter"].isin(top)].dropna(subset=["elementary_effect"])
        if data.empty:
            continue
        arrays = [data.loc[data["parameter"] == name, "elementary_effect"].to_numpy() for name in top]
        keep = [(name, values) for name, values in zip(top, arrays) if len(values)]
        if not keep:
            continue
        fig, ax = plt.subplots(figsize=(9.5, max(4.5, .42 * len(keep) + 2)))
        ax.boxplot([x[1] for x in keep], tick_labels=[x[0] for x in keep], vert=False, showfliers=True)
        ax.axvline(0, color="#64748b", lw=.8)
        ax.set(title=f"Elementary-effect distributions: {output}", xlabel="Elementary effect", ylabel="")
        ax.grid(axis="x", alpha=.2)
        save(fig, root / "elementary_effect_distributions" / f"{safe_name(output)}.png")
        count += 1
    return count


def parameter_output_scatter(summary: pd.DataFrame, outputs: pd.DataFrame, raw: pd.DataFrame, root: Path) -> int:
    count = 0
    points = raw.drop_duplicates("sample_id").set_index("sample_id")
    valid = outputs[outputs["valid"].astype(str).str.lower().eq("true")].copy()
    valid["value"] = pd.to_numeric(valid["value"], errors="coerce")
    for output, ranked in summary.groupby("output", sort=True):
        ranked = finite_frame(ranked, ["mu_star"]).nlargest(1, "mu_star")
        if ranked.empty:
            continue
        parameter = str(ranked.iloc[0]["parameter"])
        if parameter not in points.columns:
            continue
        data = valid[valid["output_name"] == output][["sample_id", "value"]].dropna()
        data = data.join(points[[parameter]], on="sample_id")
        data[parameter] = pd.to_numeric(data[parameter], errors="coerce")
        data = data.dropna()
        if data.empty:
            continue
        fig, ax = plt.subplots(figsize=(7.8, 5.5))
        ax.scatter(data[parameter], data["value"], s=24, alpha=.62, color="#2a6f97")
        ax.set(title=f"{parameter} vs {output}", xlabel=parameter, ylabel=output)
        ax.grid(alpha=.2)
        save(fig, root / "parameter_output_scatter" / f"{safe_name(output)}__{safe_name(parameter)}.png")
        count += 1
    return count


def runtime_and_validity(raw: pd.DataFrame, root: Path) -> int:
    runtime = pd.to_numeric(raw["runtime_ms"], errors="coerce").dropna() / 1000.0
    fig, ax = plt.subplots(figsize=(8, 5))
    ax.hist(runtime, bins=min(40, max(5, int(math.sqrt(max(len(runtime), 1))))), color="#2a6f97", edgecolor="white")
    ax.set(title="Simulation runtime distribution", xlabel="Runtime (seconds)", ylabel="Runs")
    ax.grid(axis="y", alpha=.2)
    save(fig, root / "runtime_distribution.png")

    counts = raw["status"].fillna("MISSING").value_counts().sort_index()
    fig, ax = plt.subplots(figsize=(8, 5))
    ax.bar(counts.index, counts.values, color=[COLORS.get("failure", "#343a40") if x != "FINITE" else COLORS["endothelial"] for x in counts.index])
    ax.set(title="Valid versus invalid outcomes", xlabel="Run status", ylabel="Runs")
    ax.tick_params(axis="x", rotation=25)
    ax.grid(axis="y", alpha=.2)
    save(fig, root / "valid_invalid_outcome_counts.png")
    return 2


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--input-dir", type=Path, default=Path("results/morris"))
    parser.add_argument("--output-dir", type=Path)
    parser.add_argument("--top-n", type=int, default=10)
    parser.add_argument("--analysis-only", action="store_true")
    parser.add_argument("--figures-only", action="store_true")
    args = parser.parse_args()
    input_dir = args.input_dir
    figure_dir = args.output_dir or input_dir / "figures"

    required = ["morris_summary_by_output.csv", "morris_elementary_effects.csv", "morris_outputs.csv",
                "morris_raw_runs.csv", "failure_sensitivity.csv"]
    missing = [name for name in required if not (input_dir / name).exists()]
    if missing:
        raise SystemExit(f"missing required files in {input_dir}: {', '.join(missing)}")

    summary = pd.read_csv(input_dir / required[0])
    effects = pd.read_csv(input_dir / required[1])
    outputs = pd.read_csv(input_dir / required[2])
    raw = pd.read_csv(input_dir / required[3])
    failure = pd.read_csv(input_dir / required[4])

    inventory = (outputs.groupby(["output_family", "output_name"], dropna=False)
                 .agg(rows=("sample_id", "size"), valid=("valid", lambda x: x.astype(str).str.lower().eq("true").sum()))
                 .reset_index())
    inventory["invalid"] = inventory["rows"] - inventory["valid"]
    inventory.to_csv(input_dir / "analysis_output_inventory.csv", index=False)

    manifest = {
        "input_dir": str(input_dir.resolve()), "runs": int(len(raw)),
        "finite_runs": int((raw["status"] == "FINITE").sum()), "outputs": int(outputs["output_name"].nunique()),
        "parameters": int(summary["parameter"].nunique()), "figures": {},
        "scipy_available": spearmanr is not None,
    }
    if not args.analysis_only:
        manifest["figures"]["mu_star_sigma"] = mu_sigma_figures(summary, figure_dir)
        manifest["figures"]["top_mu_star"] = top_bar_figures(summary, figure_dir, args.top_n)
        manifest["figures"]["heatmap"] = heatmap(summary, figure_dir)
        manifest["figures"]["failure"] = failure_figure(failure, figure_dir)
        manifest["figures"]["rank_comparison"] = rank_comparison(input_dir, figure_dir)
        manifest["figures"]["ee_distributions"] = ee_distributions(summary, effects, figure_dir, min(args.top_n, 8))
        manifest["figures"]["parameter_output_scatter"] = parameter_output_scatter(summary, outputs, raw, figure_dir)
        manifest["figures"]["runtime_validity"] = runtime_and_validity(raw, figure_dir)
    (input_dir / "analysis_manifest.json").write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(manifest, indent=2))


if __name__ == "__main__":
    main()
