#!/usr/bin/env python3
"""
A/B check of the culling of hidden sections (CaveCull): every scene is photographed twice by the REAL game under Xvfb + Mesa llvmpipe,
once as it is and once with -Dbc.cavecull=false. The culling only leaves out what cannot be seen, so the two pictures have to be the
same (up to the little that moves between two runs: clouds, a fish); a hole where a hill or a cave wall should be shows up as a big difference.

    python3 tools/cullcheck.py [--scenes surface,cave1] [--jar build/blockcraft.jar] [--win 640x360]

Results: ci-results/perf/cull/<scene>.jpg (side by side: culling on | off) and ci-results/perf/cullcheck.md.
"""
import argparse
import os
import shutil
import sys
import time

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import shots  # noqa: E402

ROOT = shots.ROOT
OUT = os.path.join(ROOT, "ci-results", "perf")

# TerraGen world, seed 12345, noon, calm, "Bright" (caves can be seen); view distance 5
BASE = ["-Dbc.auto=1", "-Dbc.seed=12345", "-Dbc.gen=1", "-Dbc.mode=1", "-Dbc.rd=5", "-Dbc.lod=48", "-Dbc.time=6000", "-Dbc.weather=calm", "-Dbc.gamma=1", "-Dbc.ambient=0.85", "-Dbc.frozen=1", "-Dbc.shot=260"]
CULL = "-Dbc.cavecull=false"
SHADOW = "-Dbc.shadowuncut=1"
# name -> (properties, the switch of the second picture: the culling of hidden sections off, or the shadow map drawn without its window test)
SCENES = {
    "surface": (["-Dbc.cam=133.5,100,71.5,45,10"], CULL),
    "hill":    (["-Dbc.cam=392.5,150,-303.5,200,8"], CULL),
    "forest":  (["-Dbc.cam=190.5,96,-25.5,90,16"], CULL),
    "cave1":   (["-Dbc.cam=81.5,22.5,-76.5,0,0"], CULL),
    "cave2":   (["-Dbc.cam=126.5,24.5,-120.5,90,5"], CULL),
    "cave3":   (["-Dbc.cam=-42.5,41.5,126.5,180,0"], CULL),
    # the sun low in the sky: long shadows from far away must still be in the map
    "shadow-forest": (["-Dbc.cam=190.5,96,-25.5,90,16", "-Dbc.time=2500", "-Dbc.shot=330"], SHADOW),
    "shadow-hill":   (["-Dbc.cam=392.5,150,-303.5,200,8", "-Dbc.time=9800", "-Dbc.shot=330"], SHADOW),
}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--scenes", default="all")
    ap.add_argument("--jar", default=os.path.join(ROOT, "build", "blockcraft.jar"))
    ap.add_argument("--win", default="640x360")
    ap.add_argument("--timeout", type=int, default=420)
    a = ap.parse_args()
    names = list(SCENES) if a.scenes == "all" else [x.strip() for x in a.scenes.split(",") if x.strip()]
    shots.OUT = os.path.join(OUT, "cull")
    shutil.rmtree(shots.OUT, ignore_errors=True)
    os.makedirs(shots.OUT, exist_ok=True)
    java = shutil.which("java") or "java"
    rows = ["# Culling check %s (window %s)\n\n| scene | differing pixels | verdict |\n|---|---|---|\n" % (time.strftime("%Y-%m-%d %H:%M:%S UTC", time.gmtime()), a.win)]
    bad = 0
    for n in names:
        extra, switch = SCENES[n]
        props = BASE + ["-Dbc.win=" + a.win] + extra
        on = shots.run_scene(java, "on", a.jar, n, props, a.timeout)
        off = shots.run_scene(java, "off", a.jar, n, props + [switch], a.timeout)
        pct = None
        if on and off:
            pct = shots.diff_pct(on, off)
            shots.composite([on, off], ["as it is", "without " + ("the shadow map's window" if switch == SHADOW else "culling")], os.path.join(shots.OUT, n + ".jpg"))
        verdict = "NO PICTURE" if pct is None else ("same" if pct < 0.3 else "DIFFERENT")
        if verdict != "same":
            bad += 1
        rows.append("| %s | %s | %s |\n" % (n, "-" if pct is None else "%.3f %%" % pct, verdict))
        for p in (on, off):
            if p and os.path.exists(p):
                os.remove(p)
        with open(os.path.join(OUT, "cullcheck.md"), "w") as f:
            f.write("".join(rows))
    rows.append("\n%d of %d scenes differ or have no picture.\n" % (bad, len(names)))
    with open(os.path.join(OUT, "cullcheck.md"), "w") as f:
        f.write("".join(rows))
    print("".join(rows))


if __name__ == "__main__":
    main()
