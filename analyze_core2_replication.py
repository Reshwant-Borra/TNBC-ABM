#!/usr/bin/env python3
"""Analyze core2 candidate replication outputs and write figures/report."""

from __future__ import annotations

import json
import math
import sys
from pathlib import Path

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd


TARGETS = [
    "abc_target_01_jnkp_s480",
    "abc_target_02_jnkp_s1440",
    "abc_target_03_ec_s480",
    "abc_target_04_ec_s960",
    "abc_target_05_ec_s1440",
    "abc_target_06_mac_s1440",
    "abc_target_07_fibro_s1440",
    "abc_target_08_tumor_s480",
    "abc_target_09_tumor_s960",
    "abc_target_10_tumor_s1440",
]


def finite(x):
    return x[np.isfinite(x)]


def corr(a, b):
    x = pd.Series(a, dtype="float64")
    y = pd.Series(b, dtype="float64")
    m = x.notna() & y.notna() & np.isfinite(x) & np.isfinite(y)
    if m.sum() < 3 or x[m].nunique() < 2 or y[m].nunique() < 2:
        return float("nan")
    return float(np.corrcoef(x[m], y[m])[0, 1])


def point_biserial(x, flag):
    return corr(x, pd.Series(flag).astype(float))


def third_label(s):
    q1, q2 = s.quantile([1 / 3, 2 / 3])
    return pd.cut(s, [-np.inf, q1, q2, np.inf], labels=["low", "middle", "high"], include_lowest=True)


def savefig(path):
    path.parent.mkdir(parents=True, exist_ok=True)
    plt.tight_layout()
    plt.savefig(path, dpi=180)
    plt.close()


def markdown_table(df):
    if df.empty:
        return "_No rows._"
    tmp = df.reset_index()
    cols = [str(c) for c in tmp.columns]
    lines = ["| " + " | ".join(cols) + " |", "| " + " | ".join(["---"] * len(cols)) + " |"]
    for _, row in tmp.iterrows():
        vals = []
        for c in tmp.columns:
            v = row[c]
            if isinstance(v, float):
                vals.append("" if math.isnan(v) else f"{v:.4g}")
            else:
                vals.append(str(v))
        lines.append("| " + " | ".join(vals) + " |")
    return "\n".join(lines)


def bar_with_labels(x, y, title, ylabel, path):
    plt.figure(figsize=(8, 4))
    plt.bar([str(v) for v in x], y, color="#3f7f93")
    plt.title(title)
    plt.xlabel("candidate_id")
    plt.ylabel(ylabel)
    savefig(path)


def main(out_dir: str):
    root = Path(out_dir)
    fig_dir = root / "figures"
    reps = pd.read_csv(root / "core2_candidate_replications.csv")
    summary = pd.read_csv(root / "core2_candidate_robustness_summary.csv")
    loss = pd.read_csv(root / "core2_compartment_loss_events.csv")
    ts = pd.read_csv(root / "core2_compartment_timeseries.csv")
    manifest = json.loads((root / "core2_replication_runtime_manifest.json").read_text())

    reps["finite_distance"] = pd.to_numeric(reps["distance"], errors="coerce").replace([np.inf, -np.inf], np.nan)
    reps["pass_le4"] = reps["acceptance_class"].isin(["GOOD", "BORDERLINE"])
    reps["good_le3"] = reps["acceptance_class"].eq("GOOD")
    reps["has_EC_loss"] = reps["EC_loss_step"].fillna(-1).astype(int) >= 0
    reps["has_fibro_loss"] = reps["fibroblast_loss_step"].fillna(-1).astype(int) >= 0

    # 1. Candidate median distance with variability.
    plt.figure(figsize=(9, 4))
    plt.errorbar(summary["candidate_id"].astype(str), summary["median_distance"],
                 yerr=summary["interquartile_range"].fillna(0) / 2, fmt="o", color="#234b63")
    plt.axhline(3, color="#547a3a", linestyle="--", linewidth=1)
    plt.axhline(4, color="#b07a2c", linestyle="--", linewidth=1)
    plt.title("Candidate median valid distance with IQR/2")
    plt.xlabel("candidate_id")
    plt.ylabel("median valid distance")
    savefig(fig_dir / "01_candidate_median_distance.png")

    # 2-3. Fractions.
    bar_with_labels(summary["candidate_id"], summary["valid_fraction"], "Valid fraction by candidate", "valid fraction", fig_dir / "02_valid_fraction.png")
    plt.figure(figsize=(9, 4))
    x = np.arange(len(summary))
    plt.bar(x - 0.18, summary["fraction_distance_le_3"], width=0.36, label="distance <= 3", color="#48795a")
    plt.bar(x + 0.18, summary["fraction_distance_le_4"], width=0.36, label="distance <= 4", color="#bc8d3f")
    plt.xticks(x, summary["candidate_id"].astype(str))
    plt.xlabel("candidate_id")
    plt.ylabel("fraction of all replicates")
    plt.legend()
    plt.title("Passing fractions by candidate")
    savefig(fig_dir / "03_pass_fractions.png")

    # 4. Parameter scatter by robustness.
    colors = {"ROBUST": "#277c62", "UNCERTAIN": "#c58a2f", "REJECTED": "#9a3a35"}
    plt.figure(figsize=(6, 5))
    for cls, g in summary.groupby("robustness_classification"):
        plt.scatter(g["divProbP"], g["pOffMax"], label=cls, s=70, color=colors.get(cls, "#555"))
        for _, row in g.iterrows():
            plt.text(row["divProbP"], row["pOffMax"], str(int(row["candidate_id"])), fontsize=8)
    plt.xlabel("divProbP")
    plt.ylabel("pOffMax")
    plt.title("core2 parameter candidates by robustness")
    plt.legend()
    savefig(fig_dir / "04_parameter_scatter_robustness.png")

    # 5-6. Parameters vs distance/invalid fraction.
    for metric, label, name in [
        ("median_distance", "median valid distance", "05_parameter_vs_median_distance.png"),
        ("invalid_fraction", "invalid fraction", "06_parameter_vs_invalid_fraction.png"),
    ]:
        fig, axes = plt.subplots(1, 2, figsize=(10, 4))
        axes[0].scatter(summary["divProbP"], summary[metric], color="#315f72")
        axes[0].set_xlabel("divProbP"); axes[0].set_ylabel(label)
        axes[1].scatter(summary["pOffMax"], summary[metric], color="#315f72")
        axes[1].set_xlabel("pOffMax"); axes[1].set_ylabel(label)
        fig.suptitle(f"Parameter values versus {label}")
        savefig(fig_dir / name)

    # 7-9. Compartment loss.
    bar_with_labels(summary["candidate_id"], summary["EC_loss_count"] / summary["total_replicates"], "EC-loss frequency by candidate", "frequency", fig_dir / "07_ec_loss_frequency.png")
    bar_with_labels(summary["candidate_id"], summary["fibroblast_loss_count"] / summary["total_replicates"], "Fibroblast-loss frequency by candidate", "frequency", fig_dir / "08_fibroblast_loss_frequency.png")
    if not loss.empty:
        plt.figure(figsize=(8, 4))
        for comp, g in loss.groupby("compartment"):
            plt.hist(g["biological_time"], bins=20, alpha=0.55, label=comp)
        plt.xlabel("biological time (days)")
        plt.ylabel("loss events")
        plt.title("Distribution of compartment-loss times")
        plt.legend()
        savefig(fig_dir / "09_loss_time_distribution.png")

    # 10. Representative trajectories.
    fig, axes = plt.subplots(2, 2, figsize=(11, 8), sharex=True)
    reps_sorted = reps.sort_values(["candidate_id", "replicate_index"])
    robust_ids = set(summary.loc[summary["robustness_classification"].eq("ROBUST"), "candidate_id"])
    choices = []
    rv = reps_sorted[reps_sorted["candidate_id"].isin(robust_ids) & reps_sorted["biologically_valid"].astype(bool)]
    choices.append(("robust valid", rv.iloc[0] if not rv.empty else reps_sorted[reps_sorted["biologically_valid"].astype(bool)].iloc[0]))
    frag = reps_sorted[reps_sorted["pass_le4"] & ~reps_sorted["candidate_id"].isin(robust_ids)]
    choices.append(("fragile passing", frag.iloc[0] if not frag.empty else reps_sorted[reps_sorted["pass_le4"]].iloc[0]))
    ec_loss = reps_sorted[reps_sorted["has_EC_loss"]]
    choices.append(("EC-loss", ec_loss.iloc[0] if not ec_loss.empty else reps_sorted.iloc[0]))
    fib_loss = reps_sorted[reps_sorted["has_fibro_loss"]]
    choices.append(("fibroblast-loss", fib_loss.iloc[0] if not fib_loss.empty else reps_sorted.iloc[0]))
    for ax, (label, row) in zip(axes.ravel(), choices):
        g = ts[(ts["candidate_id"] == row["candidate_id"]) & (ts["replicate_index"] == row["replicate_index"]) & (ts["seed"] == row["seed"])]
        ax.plot(g["biological_time"], g["tumor_total"], label="tumor")
        ax.plot(g["biological_time"], g["EC_total"], label="EC")
        ax.plot(g["biological_time"], g["fibroblast_total"], label="fibroblast")
        ax.plot(g["biological_time"], g["macrophage_total"], label="macrophage")
        ax.set_title(f"{label}: cand {int(row['candidate_id'])}, rep {int(row['replicate_index'])}")
        ax.set_yscale("symlog")
        ax.set_xlabel("days")
    axes[0, 0].legend(fontsize=8)
    savefig(fig_dir / "10_representative_population_trajectories.png")

    # 11-12. Target residual/contribution heatmaps.
    residual_cols = [f"{t}_median_normalized_residual" for t in TARGETS]
    contrib_cols = [f"{t}_median_distance_contribution" for t in TARGETS]
    for cols, name, title in [
        (residual_cols, "11_median_target_residuals.png", "Median normalized residuals by candidate"),
        (contrib_cols, "12_median_target_contributions.png", "Median distance contributions by candidate"),
    ]:
        mat = summary.set_index("candidate_id")[cols].to_numpy(dtype=float)
        plt.figure(figsize=(12, 5))
        plt.imshow(mat, aspect="auto", cmap="coolwarm" if "residual" in name else "viridis")
        plt.yticks(range(len(summary)), summary["candidate_id"].astype(str))
        plt.xticks(range(len(TARGETS)), [t.replace("abc_target_", "") for t in TARGETS], rotation=45, ha="right", fontsize=8)
        plt.colorbar()
        plt.title(title)
        savefig(fig_dir / name)

    # 13. Valid/invalid comparison of burden/populations near final time.
    final_ts = ts.sort_values("step").groupby(["candidate_id", "replicate_index", "seed"]).tail(1)
    final_ts = final_ts.merge(reps[["candidate_id", "replicate_index", "seed", "biologically_valid"]], on=["candidate_id", "replicate_index", "seed"], how="left")
    metrics = ["tumor_total", "EC_total", "fibroblast_total", "macrophage_total"]
    plt.figure(figsize=(9, 5))
    data = [final_ts.loc[final_ts["biologically_valid"].astype(bool), m] for m in metrics] + [
        final_ts.loc[~final_ts["biologically_valid"].astype(bool), m] for m in metrics]
    positions = list(range(1, len(metrics) + 1)) + list(range(len(metrics) + 2, 2 * len(metrics) + 2))
    plt.boxplot(data, positions=positions)
    plt.xticks(positions, [f"valid\n{m}" for m in metrics] + [f"invalid\n{m}" for m in metrics], rotation=30, ha="right")
    plt.yscale("symlog")
    plt.title("Final burden and stromal populations: valid versus invalid")
    savefig(fig_dir / "13_valid_invalid_population_comparison.png")

    # 14. Tradeoff ridge if enough passing points exist.
    pass_summary = summary[summary["fraction_distance_le_4"] > 0]
    if len(pass_summary) >= 4:
        plt.figure(figsize=(6, 5))
        plt.scatter(pass_summary["divProbP"], pass_summary["pOffMax"], c=pass_summary["median_distance"], cmap="viridis", s=70)
        plt.colorbar(label="median distance")
        plt.xlabel("divProbP")
        plt.ylabel("pOffMax")
        plt.title("Apparent divProbP-pOffMax tradeoff among passing candidates")
        savefig(fig_dir / "14_tradeoff_ridge.png")

    # Quantitative diagnosis.
    total = len(reps)
    valid_count = int(reps["biologically_valid"].astype(bool).sum())
    invalid_count = total - valid_count
    good = int((reps["acceptance_class"] == "GOOD").sum())
    borderline = int((reps["acceptance_class"] == "BORDERLINE").sum())
    poor = int((reps["acceptance_class"] == "POOR_VALID").sum())
    cls_counts = summary["robustness_classification"].value_counts().to_dict()
    dominant_invalid = reps.loc[~reps["biologically_valid"].astype(bool), "invalid_reason"].mode()
    dominant_invalid = dominant_invalid.iloc[0] if not dominant_invalid.empty else ""

    loss_rates = summary[["candidate_id", "EC_loss_count", "fibroblast_loss_count", "total_replicates"]].copy()
    loss_rates["EC_loss_rate"] = loss_rates["EC_loss_count"] / loss_rates["total_replicates"]
    loss_rates["fibroblast_loss_rate"] = loss_rates["fibroblast_loss_count"] / loss_rates["total_replicates"]

    thirds = {}
    for p in ["divProbP", "pOffMax"]:
        reps[f"{p}_third"] = third_label(reps[p])
        thirds[p] = reps.groupby(f"{p}_third", observed=False).agg(
            invalid_fraction=("biologically_valid", lambda x: 1 - x.astype(bool).mean()),
            EC_loss_fraction=("has_EC_loss", "mean"),
            fibroblast_loss_fraction=("has_fibro_loss", "mean"),
            median_distance=("finite_distance", "median"),
        )

    loss_times = loss.groupby("compartment")["biological_time"].median().to_dict() if not loss.empty else {}
    ec_loss_rows = reps[reps["has_EC_loss"]][["candidate_id", "replicate_index", "seed", "EC_loss_step"]]
    fib_loss_rows = reps[reps["has_fibro_loss"]][["candidate_id", "replicate_index", "seed", "fibroblast_loss_step"]]
    paired = ec_loss_rows.merge(fib_loss_rows, on=["candidate_id", "replicate_index", "seed"], how="inner")
    fibro_after_ec = float((paired["fibroblast_loss_step"] > paired["EC_loss_step"]).mean()) if not paired.empty else float("nan")

    before_loss = []
    for _, ev in loss.iterrows():
        g = ts[(ts["candidate_id"] == ev["candidate_id"]) & (ts["replicate_index"] == ev["replicate_index"]) & (ts["seed"] == ev["seed"]) & (ts["step"] < ev["loss_step"])]
        if not g.empty:
            row = g.tail(1).iloc[0].to_dict()
            row["compartment"] = ev["compartment"]
            before_loss.append(row)
    before_loss = pd.DataFrame(before_loss)
    tumor_before_loss_median = before_loss.groupby("compartment")["tumor_total"].median().to_dict() if not before_loss.empty else {}

    ec_events = loss[loss["compartment"].eq("EC")]
    fib_events = loss[loss["compartment"].eq("fibroblast")]
    ec_birth_death = float((ec_events["divisions_before_loss"] / ec_events["deaths_before_loss"].replace(0, np.nan)).median()) if not ec_events.empty else float("nan")
    fib_birth_death = float((fib_events["divisions_before_loss"] / fib_events["deaths_before_loss"].replace(0, np.nan)).median()) if not fib_events.empty else float("nan")

    associations = {
        "divProbP_validity_point_biserial": point_biserial(reps["divProbP"], reps["biologically_valid"].astype(bool)),
        "pOffMax_validity_point_biserial": point_biserial(reps["pOffMax"], reps["biologically_valid"].astype(bool)),
        "divProbP_EC_loss_point_biserial": point_biserial(reps["divProbP"], reps["has_EC_loss"]),
        "pOffMax_EC_loss_point_biserial": point_biserial(reps["pOffMax"], reps["has_EC_loss"]),
        "divProbP_fibro_loss_point_biserial": point_biserial(reps["divProbP"], reps["has_fibro_loss"]),
        "pOffMax_fibro_loss_point_biserial": point_biserial(reps["pOffMax"], reps["has_fibro_loss"]),
    }
    if not loss.empty:
        associations["divProbP_loss_time_corr"] = corr(loss["divProbP"], loss["loss_step"])
        associations["pOffMax_loss_time_corr"] = corr(loss["pOffMax"], loss["loss_step"])
    associations["tumor_final_invalid_corr"] = point_biserial(final_ts["tumor_total"], ~final_ts["biologically_valid"].astype(bool))

    robust = summary[summary["robustness_classification"].eq("ROBUST")]
    best = robust.sort_values("robust_ranking_score", ascending=False).head(1)
    best_dict = best.iloc[0].to_dict() if not best.empty else {}
    fragile_fit = summary[(summary["fraction_distance_le_4"] > 0) & (summary["robustness_classification"].ne("ROBUST"))]
    lucky = summary[(summary["fraction_distance_le_4"] <= 0) | (summary["valid_fraction"] < 0.5)]

    if len(robust) >= 3 and summary["invalid_fraction"].median() <= 0.2:
        outcome = "Outcome A: Core2 is sufficient"
    elif len(robust) > 0 and (summary["EC_loss_count"].sum() + summary["fibroblast_loss_count"].sum()) > 0:
        outcome = "Outcome B: Core2 fits targets but has stromal fragility"
    else:
        outcome = "Outcome C: Core2 pilot was not reproducible"

    recommended_param = None
    if outcome.startswith("Outcome B"):
        if math.isfinite(ec_birth_death) and (not math.isfinite(fib_birth_death) or ec_birth_death < fib_birth_death):
            recommended_param = {
                "name": "dieProbEN",
                "meaning": "Resting EC death probability per step",
                "current": manifest["frozen_parameter_values"].get("dieProbEN"),
                "code": "OnLatticeExample/ExampleGrid.java, inactive EC branch in Endothelial(); registry location `legacy lines 444-453`",
                "range": "0.001 to 0.005",
                "provenance": "Conservative narrowing within the existing registry/Morris range 0.001 to 0.01, capped at the current frozen value to test whether uncompensated resting-EC loss is driving collapse.",
                "risk": "Lowering EC death can inflate EC activation-fraction denominators and indirectly increase tumour survival.",
                "effect": "Should preserve the EC compartment before activation without directly changing tumour JNK switching.",
            }
        else:
            recommended_param = {
                "name": "divProbFP",
                "meaning": "Activated CAF division probability per step",
                "current": manifest["frozen_parameter_values"].get("divProbFP"),
                "code": "OnLatticeExample/ExampleGrid.java, activated fibroblast branch in Fibroblasts(); registry location `legacy lines 477-488`",
                "range": "0.018 to 0.038",
                "provenance": "Existing registry range retained; already ABC-inferred and supported as fibroblast target-associated in previous Morris outputs.",
                "risk": "Can raise fibroblast burden and CAF support of JNK-positive tumour growth, worsening tumour-count fit if too high.",
                "effect": "Should increase activated fibroblast replenishment when fibroblast death/space limitation exceeds division.",
            }

    report = []
    report.append("# CORE2 Replication And Stromal Diagnosis\n")
    report.append("## Architecture and Safeguards\n")
    report.append("- Authoritative calibration remains `OnLatticeExample.ABCRejection`; targets, weights, scales, thresholds, and core2 parameter ranges were not changed.\n")
    report.append("- Simulation entry point used here is `ExampleGrid.RunHeadlessDiagnostic(ModelParameters, maxStep, interval)`, which wraps the same headless update path and records counts/events after each step.\n")
    report.append("- Threading uses fixed Java worker pools; every task owns a fresh `ExampleGrid`, immutable `ModelParameters`, HAL `Rand`, diagnostic frame list, target result, and temporary buffers. Workers return objects; CSV writing is coordinator-only and sorted by candidate, replicate, seed.\n")
    report.append("- Repository search found no static grid, static agent collection, or static RNG used by the simulation. Static fields in calibration classes are immutable target/profile arrays except `ABCRejection.bestMacTry/bestMacFail`, which this harness does not mutate.\n")
    report.append("- Mutable model state is instance-scoped in `ExampleGrid`; `ModelParameters` and target/profile definitions are immutable for this workflow.\n")
    report.append("\n## ExampleGrid.java impact\n")
    report.append("- Modified: yes, minimally.\n")
    report.append("- Changed methods: diagnostic recorder calls inside `runHeadlessConfigured(...)`, plus new private helper `diagnosticEvents(int[])`.\n")
    report.append("- Reason: `macDivTry` and `macDivFail` were directly available instance counters but were not included in per-step diagnostic frames; exposing them is required to report failed division/crowding diagnostics.\n")
    report.append("- Alternatives considered: ignore failed-division diagnostics or duplicate initialization/update logic outside `ExampleGrid`; both were worse because they would either omit requested evidence or risk diverging from the simulation.\n")
    report.append("- Biological behavior: unchanged. The helper only copies cumulative event counters and appends diagnostic `macDivTry/macDivFail` values when diagnostics are enabled; agent rules, update order, random-number consumption, target calculations, and default `RunHeadless` outputs are unchanged.\n")
    report.append("- Regression evidence: `CalibrationQualityControl` compares legacy and named seeded outputs including snapshots/event counts; `Core2ReplicationQualityControl` compares one-thread and two-thread replicated outputs.\n")

    report.append("\n## Replication Results\n")
    report.append(f"- Total simulations: {total}; valid: {valid_count}; invalid: {invalid_count}.\n")
    report.append(f"- Good: {good}; borderline: {borderline}; poor valid: {poor}.\n")
    report.append(f"- Candidate classifications: ROBUST {cls_counts.get('ROBUST', 0)}, UNCERTAIN {cls_counts.get('UNCERTAIN', 0)}, REJECTED {cls_counts.get('REJECTED', 0)}.\n")
    if best_dict:
        report.append(f"- Best robust candidate by composite ranking: {int(best_dict['candidate_id'])}, divProbP={best_dict['divProbP']:.12g}, pOffMax={best_dict['pOffMax']:.12g}, median distance={best_dict['median_distance']:.3g}, valid fraction={best_dict['valid_fraction']:.2f}, <=3 fraction={best_dict['fraction_distance_le_3']:.2f}, <=4 fraction={best_dict['fraction_distance_le_4']:.2f}, sd={best_dict['sd_distance']:.3g}.\n")
    else:
        report.append("- Best robust candidate: none; zero robust candidates.\n")

    report.append("\n## Candidate Interpretation\n")
    low_div_supported = robust["divProbP"].median() <= 0.0141 if not robust.empty else False
    p_range = robust["pOffMax"].max() - robust["pOffMax"].min() if len(robust) > 1 else float("nan")
    trade_corr = corr(summary["divProbP"], summary["pOffMax"])
    report.append(f"- Low `divProbP` support: {'yes' if low_div_supported else 'not clearly'}; robust median divProbP is {robust['divProbP'].median() if not robust.empty else float('nan'):.4g}.\n")
    if len(robust) > 1:
        report.append(f"- `pOffMax` constraint: robust range is {p_range:.4g}; broad ranges indicate weak constraint.\n")
    elif len(robust) == 1:
        report.append("- `pOffMax` constraint: only one robust candidate was found, so the robust range cannot constrain `pOffMax`; the broader candidate set still spans most of the original interval.\n")
    else:
        report.append("- `pOffMax` constraint: no robust candidates were found, so no robust range can be inferred.\n")
    report.append(f"- Apparent divProbP-pOffMax tradeoff correlation across candidate summaries: {trade_corr:.3g}; this is descriptive, not causal.\n")
    report.append(f"- Original one-seed lucky/fragile candidates: {len(lucky)} candidates were rejected or had no reproduced passing replicate; {len(fragile_fit)} had at least one pass but did not meet robust criteria.\n")
    proceed = robust.sort_values("robust_ranking_score", ascending=False)["candidate_id"].astype(int).tolist()[:5]
    report.append(f"- Candidates to proceed to 30-50 seed validation: {proceed if proceed else 'none'}.\n")

    report.append("\n## Compartment-Loss Diagnosis\n")
    report.append(f"- Dominant invalid reason: `{dominant_invalid}`.\n")
    report.append(f"- Median loss times by compartment, days: {loss_times}.\n")
    if math.isfinite(fibro_after_ec):
        report.append(f"- Fibroblast loss followed EC loss in {fibro_after_ec:.2f} of paired EC+fibroblast collapse runs.\n")
    else:
        report.append("- Fibroblast loss was not observed in the replicated loss events, so EC/fibroblast ordering could not be estimated.\n")
    report.append(f"- Median tumour count immediately before stromal loss: {tumor_before_loss_median}.\n")
    fib_ratio_text = f"{fib_birth_death:.3g}" if math.isfinite(fib_birth_death) else "not observed"
    report.append(f"- Median EC birth/death ratio before EC loss: {ec_birth_death:.3g}; median fibroblast birth/death ratio before fibroblast loss: {fib_ratio_text}.\n")
    report.append(f"- Parameter-validity/loss associations: { {k: round(v, 3) if math.isfinite(v) else None for k, v in associations.items()} }.\n")
    report.append("- Loss rates by parameter thirds:\n\n")
    for p, table in thirds.items():
        report.append(f"### {p} thirds\n\n")
        report.append(markdown_table(table))
        report.append("\n\n")
    report.append("- Interpretation: compartment-loss evidence is based on replicated stochastic runs and correlations/event ratios; it does not establish causation by itself.\n")

    report.append("\n## Step 4 Decision\n")
    report.append(f"**{outcome}.**\n")
    if outcome.startswith("Outcome A"):
        report.append("Retain only robust candidates and run the best few with 30 to 50 seeds each. Do not add another calibration parameter yet.\n")
    elif outcome.startswith("Outcome B"):
        report.append("Core2 has reproducible target fits but stromal collapse remains frequent enough to justify exactly one stromal parameter in the next phase.\n")
        report.append(f"- Recommended parameter: `{recommended_param['name']}`.\n")
        report.append(f"- Biological meaning: {recommended_param['meaning']}.\n")
        report.append(f"- Current frozen value: {recommended_param['current']}.\n")
        report.append(f"- Code use: {recommended_param['code']}.\n")
        report.append(f"- Diagnostic evidence: EC birth/death ratio {ec_birth_death:.3g}; fibroblast birth/death ratio {fib_ratio_text}; dominant invalid reason `{dominant_invalid}`.\n")
        report.append("- Previous Morris support: see `results/morris-primary-20/FINAL_PARAMETER_DECISION_TABLE.csv` and registry evidence; sensitivity supports inclusion only when the observed failure mechanism matches the parameter.\n")
        report.append(f"- Proposed conservative range: {recommended_param['range']}.\n")
        report.append(f"- Range provenance: {recommended_param['provenance'].rstrip('.')}.\n")
        report.append(f"- Risks: {recommended_param['risk'].rstrip('.')}.\n")
        report.append(f"- Expected effect: {recommended_param['effect'].rstrip('.')}.\n")
    else:
        report.append("Do not narrow parameter bounds or add a third parameter yet. Replace one-seed random ABC with a structured two-dimensional design over `divProbP` and `pOffMax` using several stochastic replicates per design point.\n")

    report.append("\n## Figure Inventory\n")
    for p in sorted(fig_dir.glob("*.png")):
        report.append(f"- `{p.relative_to(root)}`\n")

    (root / "CORE2_REPLICATION_AND_STROMAL_DIAGNOSIS.md").write_text("".join(report), encoding="utf-8")
    diagnostics = {
        "total": total,
        "valid": valid_count,
        "invalid": invalid_count,
        "good": good,
        "borderline": borderline,
        "poor_valid": poor,
        "classification_counts": cls_counts,
        "dominant_invalid_reason": dominant_invalid,
        "best_robust_candidate": best_dict,
        "outcome": outcome,
        "associations": associations,
        "loss_times_days": loss_times,
        "fibro_after_ec_fraction": fibro_after_ec,
        "tumor_before_loss_median": tumor_before_loss_median,
        "ec_birth_death_before_loss": ec_birth_death,
        "fibro_birth_death_before_loss": fib_birth_death,
        "proceed_candidates": proceed,
    }
    (root / "core2_diagnostic_analysis_summary.json").write_text(json.dumps(diagnostics, indent=2, allow_nan=True), encoding="utf-8")


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else "outputs/core2_replication")
