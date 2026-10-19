#!/usr/bin/env python3
"""Repeated world-only screenshot comparison of the float and packed-color VBO layouts."""
import argparse
import itertools
import os
import shutil
import statistics
import sys
import time

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import cullcheck  # noqa: E402
import shots  # noqa: E402

BASE = [
    "-Dbc.auto=1", "-Dbc.seed=12345", "-Dbc.gen=1", "-Dbc.mode=1", "-Dbc.rd=5", "-Dbc.lod=48",
    "-Dbc.time=6000", "-Dbc.weather=calm", "-Dbc.gamma=1", "-Dbc.ambient=0.85", "-Dbc.frozen=1",
    "-Dbc.nomobs=1", "-Dbc.worldonly=true", "-Dbc.shot=260",
]
SCENES = {
    "forest": ["-Dbc.cam=190.5,96,-25.5,90,16"],
    "cave1": ["-Dbc.cam=81.5,22.5,-76.5,0,0"],
}
REFERENCE_RUNS = 3
CANDIDATE_RUNS = 2
BLOB_LIMIT = 0.05


def compare(a, b):
    return shots.diff_pct(a, b), cullcheck.blob_pct(a, b)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--reference", required=True, help="the pre-packed V6.17 jar")
    ap.add_argument("--candidate", required=True, help="the packed-VBO candidate jar")
    ap.add_argument("--scenes", default="forest")
    ap.add_argument("--win", default="640x360")
    ap.add_argument("--timeout", type=int, default=300)
    args = ap.parse_args()
    names = [name.strip() for name in args.scenes.split(",") if name.strip()]
    unknown = [name for name in names if name not in SCENES]
    if unknown:
        ap.error("unknown visual scene(s): " + ",".join(unknown))
    if not names:
        ap.error("at least one visual scene is required")

    out = os.path.join(shots.ROOT, "build", "paired", "visual")
    shutil.rmtree(out, ignore_errors=True)
    os.makedirs(out, exist_ok=True)
    shots.OUT = out
    report_path = os.path.join(shots.ROOT, "ci-results", "perf", "packed-visual.md")
    os.makedirs(os.path.dirname(report_path), exist_ok=True)
    java = shutil.which("java") or "java"
    rows = [
        "# Packed-VBO visual comparison %s (world only, %s)" % (time.strftime("%Y-%m-%d %H:%M:%S UTC", time.gmtime()), args.win),
        "", "Each scene has %d pre-pack and %d packed screenshots. Repeat noise is the largest within-version difference; packed difference is the median of every cross-version pair." % (REFERENCE_RUNS, CANDIDATE_RUNS),
        "", "The camera, seed, frozen sky, and hidden hand/HUD are identical across runs.",
        "", "| Scene | Maximum repeat noise (pixels / blobs) | Median packed difference (pixels / blobs) | Verdict |", "|---|---:|---:|---|",
    ]
    failed = 0
    for name in names:
        props = BASE + ["-Dbc.win=" + args.win] + SCENES[name]
        references = [
            shots.run_scene(java, "prepack%d" % (i + 1), args.reference, name, props, args.timeout)
            for i in range(REFERENCE_RUNS)
        ]
        candidates = [
            shots.run_scene(java, "packed%d" % (i + 1), args.candidate, name, props, args.timeout)
            for i in range(CANDIDATE_RUNS)
        ]
        verdict = "NO PICTURE"
        noise = noise_blob = difference = difference_blob = None
        if all(references) and all(candidates):
            repeat_pairs = list(itertools.combinations(references, 2)) + list(itertools.combinations(candidates, 2))
            cross_pairs = list(itertools.product(references, candidates))
            repeat_metrics = [compare(a, b) for a, b in repeat_pairs]
            cross_metrics = [compare(a, b) for a, b in cross_pairs]
            if all(p is not None and b is not None for p, b in repeat_metrics + cross_metrics):
                noise = max(p for p, _ in repeat_metrics)
                noise_blob = max(b for _, b in repeat_metrics)
                difference = statistics.median(p for p, _ in cross_metrics)
                difference_blob = statistics.median(b for _, b in cross_metrics)
                if difference_blob < BLOB_LIMIT:
                    verdict = "same (no persistent blobs)"
                elif difference_blob - noise_blob < BLOB_LIMIT:
                    verdict = "same (within repeat noise)"
                else:
                    verdict = "DIFFERENT"

            shots.composite(
                [references[0], candidates[0]], ["pre-pack", "packed"],
                os.path.join(out, name + "-comparison.jpg")
            )
            if verdict == "DIFFERENT":
                grid = cullcheck.blob_grid(references[0], candidates[0])
                preview = cullcheck.preview_base64(references[0], candidates[0])
                if grid:
                    rows.append("\n#### %s residual mask (`#` = persistent difference)\n\n```text\n%s\n```\n" % (name, grid))
                if preview:
                    rows.append("\nDIFF_PREVIEW_JPEG_BASE64:%s\n" % preview)
        fmt = lambda value: "-" if value is None else "%.3f%%" % value
        rows.append("| %s | %s / %s | %s / %s | %s |" % (
            name, fmt(noise), fmt(noise_blob), fmt(difference), fmt(difference_blob), verdict
        ))
        if not verdict.startswith("same"):
            failed += 1
        for image in references + candidates:
            if image and os.path.exists(image):
                os.remove(image)
        with open(report_path, "w", encoding="utf-8") as f:
            f.write("\n".join(rows) + "\n")
    rows.append("\n%d of %d scenes differ or produced no picture.\n" % (failed, len(names)))
    with open(report_path, "w", encoding="utf-8") as f:
        f.write("\n".join(rows) + "\n")
    print("\n".join(rows))
    return 0 if failed == 0 else 1


if __name__ == "__main__":
    raise SystemExit(main())
