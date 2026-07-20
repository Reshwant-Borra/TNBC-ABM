#!/usr/bin/env python3
"""Build the FINAL_PARAMETER_DECISION_TABLE.csv from primary + confirmation + pilot.

Transparent, deterministic recommendation logic. Sensitivity != identifiability:
a fixed-but-sensitive parameter is flagged for review, not auto-calibrated.
No simulation is run.
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
FAIL_BIO = {"TUMOR_EXTINCT", "EC_POPULATION_ZERO", "MACROPHAGE_POPULATION_ZERO",
            "FIBROBLAST_POPULATION_ZERO"}
# Core mechanism rates whose biology is essential even if screening rank is modest.
ESSENTIAL = ABC12 | {"pOnMax", "pOffMax", "activProbM", "activProbE", "activProbF", "ecSurvival"}


def num(s):
    return pd.to_numeric(s, errors="coerce")


def sigma_interp(ratio, snr_conf=np.nan):
    if not np.isfinite(ratio):
        base = "sigma undetermined"
    elif ratio < 0.5:
        base = "low sigma/mu*: near-linear, consistent effect"
    elif ratio < 1.0:
        base = "moderate sigma/mu*: nonlinearity or interaction likely"
    else:
        base = "high sigma/mu*: strong interaction/nonlinearity or stochastic"
    if np.isfinite(snr_conf):
        if snr_conf >= 1.5:
            base += f"; 3-seed SNR {snr_conf:.2f}: reproducible -> genuine interaction/nonlinearity, not noise"
        elif snr_conf >= 1.0:
            base += f"; 3-seed SNR {snr_conf:.2f}: signal exceeds stochastic noise"
        else:
            base += f"; 3-seed SNR {snr_conf:.2f} < 1: effect partly stochastic, treat magnitude cautiously"
    return base


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--primary-dir", type=Path, required=True)
    ap.add_argument("--pilot-dir", type=Path, required=True)
    ap.add_argument("--out", type=Path, required=True)
    ap.add_argument("--repo", type=Path, default=Path("."))
    args = ap.parse_args()

    reg = pd.read_csv(args.repo / "GLOBAL_PARAMETER_REGISTRY.csv").rename(
        columns={"canonical_name": "parameter"})
    glob = pd.read_csv(args.primary_dir / "morris_global_rankings.csv").set_index("parameter")
    pc_path = args.primary_dir / "PRIMARY_PARAMETER_CLASSIFICATION.csv"
    if not pc_path.exists():
        pc_path = args.primary_dir / "PARAMETER_CLASSIFICATION.csv"
    pc = pd.read_csv(pc_path).set_index("parameter")
    summ = pd.read_csv(args.primary_dir / "morris_summary_by_output.csv")
    fail = pd.read_csv(args.primary_dir / "failure_sensitivity.csv")
    fail["rank_biserial_effect"] = num(fail["rank_biserial_effect"])
    fail["permutation_p_value"] = num(fail["permutation_p_value"])
    pilot_glob = pd.read_csv(args.pilot_dir / "morris_global_rankings.csv").set_index("parameter")

    # confirmation (optional)
    conf_path = args.primary_dir / "CONFIRMED_PARAMETER_EFFECTS.csv"
    conf = pd.read_csv(conf_path).set_index("parameter") if conf_path.exists() else None
    conf_rank = None
    if conf is not None:
        cc = conf.sort_values("median_multi_seed_mu_star", ascending=False)
        conf_rank = {p: i + 1 for i, p in enumerate(cc.index)}

    # per-parameter primary snr over bio outputs
    sbio = summ[summ["output_family"].isin(BIO_FAMILIES)].copy()
    sbio["mean_signal_to_noise_ratio"] = num(sbio["mean_signal_to_noise_ratio"])
    snr_prim = sbio.groupby("parameter")["mean_signal_to_noise_ratio"].median()

    rows = []
    for _, rr in reg.iterrows():
        p = rr["parameter"]
        screened = str(rr["enter_morris_screen"]).lower() == "true"
        status = rr["current_status"]
        inferred = status == "ABC-inferred"
        # include screened params + explicitly inactive declared fields
        if not screened and status != "inactive":
            continue
        foi = "ABC-inferred" if inferred else "fixed"
        base = rr["baseline_value"]; lo = rr["lower_bound"]; hi = rr["upper_bound"]
        cat = rr["group"]
        rank_p = int(glob.loc[p, "overall_rank"]) if p in glob.index else np.nan
        rank_c = conf_rank.get(p, np.nan) if conf_rank else np.nan
        peak = pc.loc[p, "peak_class"] if p in pc.index else "F"
        classification = pc.loc[p, "classification"] if p in pc.index else "INSUFFICIENT_OR_INACTIVE"
        med_ratio = pc.loc[p, "median_sigma_to_mustar"] if p in pc.index else np.nan
        mean_valid = pc.loc[p, "mean_valid_fraction"] if p in pc.index else np.nan
        frac_top10 = pc.loc[p, "frac_bio_outputs_top10"] if p in pc.index else np.nan
        top_outs = pc.loc[p, "top_affected_bio_outputs"] if p in pc.index else ""
        snr = snr_prim.get(p, np.nan)
        snr_c = conf.loc[p, "median_multi_seed_snr"] if (conf is not None and p in conf.index) else np.nan
        snr_use = snr_c if np.isfinite(snr_c) else snr
        # strongest biological failure association
        pf = fail[(fail["parameter"] == p) & fail["outcome"].isin(FAIL_BIO) &
                  np.isfinite(fail["rank_biserial_effect"])]
        if len(pf):
            b = pf.iloc[pf["rank_biserial_effect"].abs().argmax()]
            fout, frb, fp = b["outcome"], float(b["rank_biserial_effect"]), float(b["permutation_p_value"])
        else:
            fout, frb, fp = "", np.nan, np.nan
        strong_failure = np.isfinite(frb) and abs(frb) >= 0.6 and np.isfinite(fp) and fp < 0.05
        # pilot->primary stability
        rank_pilot = int(pilot_glob.loc[p, "overall_rank"]) if p in pilot_glob.index else np.nan
        drank = abs(rank_p - rank_pilot) if (np.isfinite(rank_p) and np.isfinite(rank_pilot)) else np.nan
        unstable = np.isfinite(drank) and drank >= 10

        # Relative influence tiering over the 45 screened parameters (rank tertiles).
        n_screened = 45
        top_tier = np.isfinite(rank_p) and rank_p <= n_screened / 3          # rank 1-15
        middle_tier = np.isfinite(rank_p) and (n_screened / 3 < rank_p <= 2 * n_screened / 3)  # 16-30
        bottom_tier = np.isfinite(rank_p) and rank_p > 2 * n_screened / 3    # rank 31-45
        influential = top_tier
        fail_txt = (f"association with {fout} (rank-biserial={frb:+.2f}, perm p={fp:.3g}; "
                    f"{'higher' if frb>0 else 'lower'} values -> more likely)")

        # ---- recommendation precedence: coverage -> tier -> status/failure ----
        if status == "inactive":
            rec = "STRUCTURALLY_INACTIVE"
            just = ("Declared field with no executable branch under current event logic; retained for "
                    "auditability. Not a calibration target and must not be deleted here.")
        elif np.isfinite(mean_valid) and mean_valid < 0.5:
            rec = "REVIEW_RANGE"
            just = (f"Majority of elementary effects lost to invalid/extinct pairs "
                    f"(mean valid fraction {mean_valid:.2f}); inspect whether this reflects biological "
                    f"failure, boundary behaviour, or a denominator artefact. Do not auto-narrow.")
        elif top_tier:
            if unstable:
                rec = "INCONCLUSIVE"
                just = (f"Top-third rank (primary {rank_p}) but pilot->primary rank moved by "
                        f"{int(drank)}; needs matched-seed confirmation before a calibration decision.")
            elif inferred:
                rec = "CALIBRATE"
                just = (f"Top-third biological-priority rank (primary {rank_p}) and already ABC-inferred; "
                        f"retain as a calibration target. {sigma_interp(med_ratio, snr_c)}."
                        + (f" Also has {fail_txt}." if strong_failure else ""))
            else:
                rec = "REVIEW_FIXED_VALUE"
                just = (f"Top-third biological-priority rank (primary {rank_p}) but currently {status}. "
                        f"A fixed yet highly sensitive parameter needs literature/mentor review before "
                        f"deciding whether to calibrate; sensitivity is not identifiability."
                        + (f" Also has {fail_txt}." if strong_failure else ""))
        elif middle_tier:
            if inferred and not unstable:
                rec = "CALIBRATE"
                just = (f"Middle-third rank (primary {rank_p}) and already ABC-inferred; keep in the "
                        f"calibration set. {sigma_interp(med_ratio, snr_c)}.")
            elif strong_failure:
                rec = "FAILURE_DRIVER"
                just = (f"Secondary continuous influence (primary rank {rank_p}) but strong {fail_txt}. "
                        f"Treat as a failure/boundary driver; review range/biology before a prior.")
            else:
                rec = "INCONCLUSIVE"
                just = (f"Middle-third rank (primary {rank_p}), currently {status}, no strong failure or "
                        f"reproducibility signal; secondary priority pending confirmation/surrogate.")
        else:  # bottom third
            if strong_failure:
                rec = "FAILURE_DRIVER"
                just = (f"Low continuous rank (primary {rank_p}) but strong {fail_txt}. "
                        f"Its role is pushing the model into this failure mode; review range/biology.")
            elif inferred and np.isfinite(snr_c) and snr_c < 1.0:
                rec = "FIX_AT_SUPPORTED_VALUE"
                just = (f"Lower-third rank (primary {rank_p}) and 3-seed confirmation gives SNR {snr_c:.2f} < 1 "
                        f"— demonstrably weakly identifiable (signal below stochastic noise). Fix at a "
                        f"literature-supported value rather than inferring. Mechanism stays in the model.")
            elif inferred:
                rec = "NEEDS_ADDITIONAL_DATA"
                just = (f"Biologically essential ABC-inferred rate but lower-third screening rank "
                        f"(primary {rank_p}); screening alone cannot resolve it and it was not matched-seed "
                        f"confirmed. Needs targeted data / narrower biological prior before calibration. "
                        f"Do not delete the mechanism.")
            else:
                rec = "LOW_INFLUENCE_WITHIN_RANGE"
                just = (f"Lower-third rank (primary {rank_p}) within the current range; keep fixed. "
                        f"Low relative influence is not evidence the mechanism is unnecessary.")

        rows.append({
            "parameter": p,
            "biological_category": cat,
            "current_status": status,
            "fixed_or_inferred": foi,
            "in_abc12": p in ABC12,
            "baseline": base,
            "lower_bound": lo,
            "upper_bound": hi,
            "top_affected_outputs": top_outs,
            "primary_mu_star_rank": rank_p,
            "confirmed_mu_star_rank": rank_c,
            "peak_class": peak,
            "sigma_interpretation": sigma_interp(med_ratio, snr_c),
            "failure_association": (f"{fout} (rb={frb:+.2f}, p={fp:.3g})" if fout else "none significant"),
            "stochastic_signal_to_noise": (f"{snr_use:.2f}" if np.isfinite(snr_use) else "n/a"),
            "pilot_to_primary_abs_rank_change": drank,
            "classification": classification,
            "recommendation": rec,
            "justification": just,
        })
    out = pd.DataFrame(rows)
    # order: screened by primary rank first, inactive last
    out["__sort"] = out["primary_mu_star_rank"].fillna(9999)
    out = out.sort_values(["__sort", "parameter"]).drop(columns="__sort")
    out.to_csv(args.out, index=False)
    print(f"wrote {args.out} ({len(out)} rows)")
    print(out["recommendation"].value_counts().to_string())


if __name__ == "__main__":
    main()
