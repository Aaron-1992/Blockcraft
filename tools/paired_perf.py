#!/usr/bin/env python3
"""Run the same headless game benchmark against V6.16, the pre-pack renderer, and the packed-VBO candidate.

This keeps the scene, machine, Mesa software renderer, frame count and settings identical, so the output can distinguish the
historical release baseline from the effect of the 32-byte VBO change.  The underlying benchmark is tools/perf.py's in-game run.
"""
import argparse
import glob
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import perf  # noqa: E402


def bench_rows(lines):
    rows = {}
    for line in lines:
        if not line.startswith("BENCH ") or line.startswith("BENCH   "):
            # The indented detail lines (CPU phases, GPU phases, memory by tile) can contain their own 'avg'; only
            # the top-level BENCH <preset> avg rows belong in the comparison table.
            continue
        match = re.match(r"BENCH\s+(.+?)\s+avg\s+([0-9]+(?:\.[0-9]+)?)\s+ms", line)
        if match:
            rows[match.group(1).strip()] = float(match.group(2))
    return rows


def run_variant(label, jar, scenes, win, frames, timeout):
    result = {}
    ok = True
    for scene in scenes:
        status, seconds, lines = perf.run(
            label + "-" + scene, perf.SCENARIOS[scene], win, frames, jar, timeout
        )
        rows = bench_rows(lines)
        memory = [line for line in lines if line.startswith("BENCH mesh memory:")]
        done = any(line.startswith("BENCH done") for line in lines)
        result[scene] = {"status": status, "seconds": seconds, "rows": rows, "memory": memory, "done": done}
        if status != "exit 0" or not done or not rows:
            ok = False
    return result, ok


def format_delta(candidate, reference):
    if reference is None or candidate is None or reference <= 0:
        return "n/a"
    delta = candidate - reference
    pct = delta * 100.0 / reference
    return "%+.1f ms (%+.1f%%)" % (delta, pct)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--v616", required=True, help="the exact V6.16 release jar")
    ap.add_argument("--v617", required=True, help="jar built from the pre-packed renderer")
    ap.add_argument("--packed", required=True, help="jar built from the packed-VBO candidate")
    ap.add_argument("--scenes", default="forest")
    ap.add_argument("--win", default="640x360")
    ap.add_argument("--frames", type=int, default=20)
    ap.add_argument("--timeout", type=int, default=900)
    args = ap.parse_args()
    scenes = list(perf.SCENARIOS) if args.scenes == "all" else [name.strip() for name in args.scenes.split(",") if name.strip()]
    unknown = [name for name in scenes if name not in perf.SCENARIOS]
    if unknown:
        ap.error("unknown benchmark scene(s): " + ",".join(unknown))

    os.makedirs(perf.OUT, exist_ok=True)
    for old_file in glob.glob(os.path.join(perf.OUT, "*.txt")) + [os.path.join(perf.OUT, "summary.md")]:
        if os.path.isfile(old_file):
            os.remove(old_file)

    variants = [
        ("V6.16 release", "v616", args.v616),
        ("Pre-pack V6.17", "v617", args.v617),
        ("Packed candidate", "packed", args.packed),
    ]
    results = {}
    ok = True
    for title, label, jar in variants:
        print("\n=== %s (%s) ===" % (title, jar), flush=True)
        results[label], passed = run_variant(label, jar, scenes, args.win, args.frames, args.timeout)
        ok = ok and passed

    report = [
        "# Paired PC performance comparison",
        "",
        "All three jars were attempted on the same GitHub runner with llvmpipe, identical scene/settings, %s, %d frames per setting. See run status below for incomplete attempts." % (args.win, args.frames),
        "Deltas are candidate minus the named reference (negative is faster). llvmpipe is a CPU software renderer, not a prediction of a graphics card's FPS.",
        "",
    ]
    for scene in scenes:
        report.extend(["## %s" % scene, "", "| Setting | V6.16 | Pre-pack V6.17 | Packed candidate | Packed vs pre-pack | Packed vs V6.16 |", "|---|---:|---:|---:|---:|---:|"])
        names = set()
        for label in ("v616", "v617", "packed"):
            names.update(results[label].get(scene, {}).get("rows", {}).keys())
        for setting in sorted(names):
            base = results["v616"].get(scene, {}).get("rows", {}).get(setting)
            pre = results["v617"].get(scene, {}).get("rows", {}).get(setting)
            candidate = results["packed"].get(scene, {}).get("rows", {}).get(setting)
            fmt = lambda value: "n/a" if value is None else "%.1f ms" % value
            report.append("| %s | %s | %s | %s | %s | %s |" % (
                setting, fmt(base), fmt(pre), fmt(candidate), format_delta(candidate, pre), format_delta(candidate, base)
            ))
        report.append("")
        for label, title in (("v616", "V6.16"), ("v617", "Pre-pack V6.17"), ("packed", "Packed candidate")):
            for line in results[label].get(scene, {}).get("memory", []):
                report.append("- %s: %s" % (title, line.removeprefix("BENCH ")))
        report.append("")

    report.extend(["## Run status", ""])
    for title, label, _ in variants:
        for scene in scenes:
            item = results[label].get(scene, {})
            report.append("- %s / %s: %s, %.0f s, benchmark %s" % (
                title, scene, item.get("status", "not run"), item.get("seconds", 0.0),
                "complete" if item.get("done") else "incomplete"
            ))
    report.append("")
    os.makedirs(perf.OUT, exist_ok=True)
    with open(os.path.join(perf.OUT, "summary.md"), "w", encoding="utf-8") as f:
        f.write("\n".join(report))
    print("\n".join(report))
    return 0 if ok else 1


if __name__ == "__main__":
    raise SystemExit(main())
