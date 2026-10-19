#!/usr/bin/env python3
"""World-only screenshot comparison of the old float VBO and packed-color VBO layouts."""
import argparse
import os
import shutil
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
BLOB_LIMIT = 0.05


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
        "", "Reference and candidate use the same camera, seed, frozen sky, and hidden hand/HUD. A repeat reference run measures renderer noise.",
        "", "| Scene | Pre-pack repeat noise (pixels / blobs) | Packed difference (pixels / blobs) | Verdict |", "|---|---:|---:|---|",
    ]
    failed = 0
    for name in names:
        props = BASE + ["-Dbc.win=" + args.win] + SCENES[name]
        ref1 = shots.run_scene(java, "prepack1", args.reference, name, props, args.timeout)
        ref2 = shots.run_scene(java, "prepack2", args.reference, name, props, args.timeout)
        candidate = shots.run_scene(java, "packed", args.candidate, name, props, args.timeout)
        verdict = "NO PICTURE"
        noise = noise_blob = difference = difference_blob = None
        if ref1 and ref2 and candidate:
            noise = shots.diff_pct(ref1, ref2)
            noise_blob = cullcheck.blob_pct(ref1, ref2)
            difference = shots.diff_pct(ref1, candidate)
            difference_blob = cullcheck.blob_pct(ref1, candidate)
            if difference_blob is not None and difference_blob < BLOB_LIMIT:
                verdict = "same (no persistent blobs)"
            elif difference_blob is not None and noise_blob is not None and difference_blob - noise_blob < BLOB_LIMIT:
                verdict = "same (within repeat noise)"
            else:
                verdict = "DIFFERENT"
            shots.composite([ref1, candidate], ["pre-pack", "packed"], os.path.join(out, name + "-comparison.jpg"))
            if verdict == "DIFFERENT":
                grid = cullcheck.blob_grid(ref1, candidate)
                preview = cullcheck.preview_base64(ref1, candidate)
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
        for image in (ref1, ref2, candidate):
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
