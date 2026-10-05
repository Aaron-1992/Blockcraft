#!/usr/bin/env python3
"""
The benchmark of the REAL game (-Dbc.bench, see PerfBench.java) under Xvfb + Mesa llvmpipe - what the 'Perf' GitHub workflow runs.

llvmpipe draws with the processor, so the numbers are no frame rates of a graphics card. They show which parts of a frame are
expensive (the 'exact phases' lines), what a setting costs (the lines of the other settings) and how many vertices / draw calls a
change saves. Compare two runs of this script, never one with a real graphics card.

    python3 tools/perf.py                       # every scenario
    python3 tools/perf.py --scenes forest       # some
    python3 tools/perf.py --win 960x540 --frames 90

Results: ci-results/perf/<scenario>.txt (the BENCH lines of the game) and ci-results/perf/summary.md.
"""
import argparse
import glob
import os
import shutil
import subprocess
import sys
import tempfile
import time

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "ci-results", "perf")

# TerraGen world, seed 12345, noon, calm weather; the camera stands above the ground at the given x / z and turns once round
BASE = ["-Dbc.auto=1", "-Dbc.seed=12345", "-Dbc.gen=1", "-Dbc.mode=1", "-Dbc.rd=8", "-Dbc.lod=64", "-Dbc.time=6000", "-Dbc.weather=calm"]
SCENARIOS = {
    # a dark forest (the most leaves per chunk), a mountain range (the most cliffs), the old generator's flat land near the first village
    "forest":   BASE + ["-Dbc.cam=190.5,96,-25.5,90,16"],
    "mountain": BASE + ["-Dbc.cam=392.5,150,-303.5,200,8"],
    "classic":  ["-Dbc.auto=1", "-Dbc.seed=12345", "-Dbc.gen=0", "-Dbc.mode=1", "-Dbc.rd=8", "-Dbc.lod=64", "-Dbc.time=6000", "-Dbc.weather=calm", "-Dbc.cam=133,92,71,45,22"],
}


def run(name, props, win, frames, jar, timeout):
    home = tempfile.mkdtemp(prefix="bc-perf-")
    env = dict(os.environ, HOME=home, LIBGL_ALWAYS_SOFTWARE="1", GALLIUM_DRIVER="llvmpipe", LP_NUM_THREADS="4")
    cmd = ["xvfb-run", "-a", "-s", "-screen 0 1280x800x24 +extension GLX +render -noreset",
           shutil.which("java") or "java", "-Xmx2g", "-Duser.home=" + home, "-Dbc.win=" + win, "-Dbc.bench=%d" % frames] + props + ["-jar", jar]
    t0 = time.time()
    try:
        r = subprocess.run(cmd, env=env, capture_output=True, text=True, timeout=timeout)
        status = "exit %d" % r.returncode
        text = (r.stdout or "") + (r.stderr or "")
    except subprocess.TimeoutExpired as e:
        status = "TIMEOUT"
        text = ((e.stdout or b"").decode("utf-8", "replace") if isinstance(e.stdout, bytes) else (e.stdout or "")) + \
               ((e.stderr or b"").decode("utf-8", "replace") if isinstance(e.stderr, bytes) else (e.stderr or ""))
    dt = time.time() - t0
    log = os.path.join(home, ".blockcraft112", "latest.log")
    if os.path.exists(log):
        with open(log, errors="replace") as f:
            text += "\n" + f.read()
    lines = [l for l in text.splitlines() if l.startswith("BENCH") or l.startswith("GL:") or l.startswith("GPU")]
    shutil.rmtree(home, ignore_errors=True)
    os.makedirs(OUT, exist_ok=True)
    with open(os.path.join(OUT, name + ".txt"), "w") as f:
        f.write("# %s: %s in %.0f s, window %s, %d frames per setting\n" % (name, status, dt, win, frames))
        f.write("\n".join(lines) + "\n")
        if not any(l.startswith("BENCH done") for l in lines):
            f.write("\n--- the end of the output (the benchmark did not finish) ---\n" + text[-3000:] + "\n")
    print("  %-10s %s in %.0f s, %d BENCH lines" % (name, status, dt, len([l for l in lines if l.startswith("BENCH")])), flush=True)
    return status, dt, lines


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--scenes", default="all")
    ap.add_argument("--win", default="640x360")
    ap.add_argument("--frames", type=int, default=60)
    ap.add_argument("--jar", default=os.path.join(ROOT, "build", "blockcraft.jar"))
    ap.add_argument("--timeout", type=int, default=1500)
    a = ap.parse_args()
    names = list(SCENARIOS) if a.scenes == "all" else [x.strip() for x in a.scenes.split(",") if x.strip()]
    # (the culling check writes its own files into this folder: only the benchmark's files are replaced)
    os.makedirs(OUT, exist_ok=True)
    for old_file in glob.glob(os.path.join(OUT, "*.txt")) + glob.glob(os.path.join(OUT, "summary.md")):
        os.remove(old_file)
    rows = ["# Perf run %s (llvmpipe, window %s, %d frames per setting)\n" % (time.strftime("%Y-%m-%d %H:%M:%S UTC", time.gmtime()), a.win, a.frames)]
    for n in names:
        status, dt, lines = run(n, SCENARIOS[n], a.win, a.frames, a.jar, a.timeout)
        rows.append("\n## %s (%s, %.0f s)\n\n```\n%s\n```\n" % (n, status, dt, "\n".join(l for l in lines if l.startswith("BENCH") or l.startswith("GL:"))))
        with open(os.path.join(OUT, "summary.md"), "w") as f:
            f.write("".join(rows))
    print(open(os.path.join(OUT, "summary.md")).read())


if __name__ == "__main__":
    main()
