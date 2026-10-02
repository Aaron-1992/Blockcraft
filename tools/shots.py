#!/usr/bin/env python3
"""
Headless OpenGL screenshot runs of the REAL game (needs Linux + Xvfb + Mesa llvmpipe + ImageMagick;
this is what the 'Visual' GitHub workflow uses - see .github/workflows/visual.yml).

For every scene the game is started with the built-in test hooks (-Dbc.auto=1 ... see Game.autoTest()),
takes one screenshot and exits. Scenes run for each jar given with --jar label=path (default: the original
Update 3.5 jar 'ref' and the freshly built 'new'), then a side-by-side composite and a difference number are
written to ci-results/shots/.

    python3 tools/shots.py                       # all scenes, ref + new
    python3 tools/shots.py --scenes day,title    # some scenes
    python3 tools/shots.py --jar new=build/blockcraft.jar   # only the new jar
"""
import argparse
import glob
import os
import re
import shutil
import subprocess
import sys
import tempfile
import time

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "ci-results", "shots")

# Spawn of seed 12345 is around x=133,z=71 (see Equivalence output) with terrain ~y=70.
COMMON = ["-Dbc.auto=1", "-Dbc.seed=12345", "-Dbc.mode=1", "-Dbc.rd=4", "-Dbc.lod=48"]
CAM = "-Dbc.cam=133,92,71,45,22"

# name -> (props, only_new)
SCENES = {
    "title":      (["-Dbc.title=TITLE"], False),
    "day":        (COMMON + [CAM, "-Dbc.time=6000", "-Dbc.shot=260"], False),
    "day-plain":  (COMMON + [CAM, "-Dbc.time=6000", "-Dbc.shaders=false", "-Dbc.shot=260"], False),
    "sunset":     (COMMON + ["-Dbc.cam=133,80,71,200,6", "-Dbc.time=12700", "-Dbc.shot=260"], False),
    "night":      (COMMON + ["-Dbc.cam=133,80,71,45,15", "-Dbc.time=18000", "-Dbc.shaders=false", "-Dbc.shot=260"], False),
    "mobs":       (COMMON + [CAM, "-Dbc.time=6000", "-Dbc.shaders=false", "-Dbc.summon=pig,cow,sheep,chicken,zombie,skeleton,creeper,spider", "-Dbc.shot=300"], False),
    "inv-creative-1": (COMMON + [CAM, "-Dbc.shaders=false", "-Dbc.inv=creative", "-Dbc.page=0", "-Dbc.shot=220"], False),
    "inv-creative-2": (COMMON + [CAM, "-Dbc.shaders=false", "-Dbc.inv=creative", "-Dbc.page=1", "-Dbc.shot=220"], False),
    "inv-craft":  (COMMON + [CAM, "-Dbc.shaders=false", "-Dbc.inv=craft", "-Dbc.shot=220"], False),
}


def run_scene(java, label, jar, name, props, timeout):
    home = tempfile.mkdtemp(prefix="bc-home-")
    env = dict(os.environ, HOME=home, LIBGL_ALWAYS_SOFTWARE="1", GALLIUM_DRIVER="llvmpipe", LP_NUM_THREADS="2")
    cmd = ["xvfb-run", "-a", "-s", "-screen 0 1280x800x24 +extension GLX +render -noreset",
           java, "-Xmx1g", "-Duser.home=" + home] + props + ["-jar", jar]
    t0 = time.time()
    try:
        r = subprocess.run(cmd, env=env, capture_output=True, text=True, timeout=timeout)
        status = "exit %d" % r.returncode
        err = (r.stderr or "")[-1500:]
    except subprocess.TimeoutExpired:
        status = "TIMEOUT"
        err = ""
    dt = time.time() - t0
    shots = sorted(glob.glob(os.path.join(home, ".blockcraft112", "screenshots", "*.png")))
    log = os.path.join(home, ".blockcraft112", "latest.log")
    os.makedirs(os.path.join(OUT, "logs"), exist_ok=True)
    if os.path.exists(log):
        shutil.copy(log, os.path.join(OUT, "logs", "%s-%s.log" % (name, label)))
    elif err:
        with open(os.path.join(OUT, "logs", "%s-%s.log" % (name, label)), "w") as f:
            f.write(err)
    png = None
    if shots:
        png = os.path.join(OUT, "%s-%s.png" % (name, label))
        shutil.copy(shots[-1], png)
    shutil.rmtree(home, ignore_errors=True)
    print("  [%-4s] %-16s %s in %.0fs -> %s" % (label, name, status, dt, "ok" if png else "NO SCREENSHOT"), flush=True)
    return png


def im(*args):
    return subprocess.run(list(args), capture_output=True, text=True)


def diff_pct(a, b):
    """Percentage of pixels that differ by more than a small tolerance."""
    r = im("compare", "-metric", "AE", "-fuzz", "6%", a, b, "null:")
    txt = (r.stderr or r.stdout).strip().split()
    try:
        n = float(txt[0])
    except Exception:
        return None
    w, h = [int(x) for x in im("identify", "-format", "%w %h", a).stdout.split()]
    return 100.0 * n / (w * h)


def composite(paths, labels, out):
    args = ["convert"]
    for p, l in zip(paths, labels):
        args += ["(", p, "-resize", "800x", "-gravity", "NorthWest", "-fill", "yellow", "-stroke", "black",
                 "-strokewidth", "1", "-pointsize", "20", "-annotate", "+8+6", l, ")"]
    args += ["+append", "-quality", "78", out]
    im(*args)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--scenes", default="all")
    ap.add_argument("--jar", action="append", help="label=path (repeatable)")
    ap.add_argument("--timeout", type=int, default=420)
    a = ap.parse_args()
    jars = {}
    if a.jar:
        for j in a.jar:
            k, v = j.split("=", 1)
            jars[k] = os.path.abspath(v)
    else:
        sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
        import runtests as T  # reuses reference_jar()
        jars = {"ref": T.reference_jar(), "new": os.path.join(ROOT, "build", "blockcraft.jar")}
    java = shutil.which("java") or "java"
    names = list(SCENES) if a.scenes == "all" else [s.strip() for s in a.scenes.split(",")]
    shutil.rmtree(OUT, ignore_errors=True)
    os.makedirs(OUT, exist_ok=True)
    rows = []
    for name in names:
        props, only_new = SCENES[name]
        got = {}
        for label, jar in jars.items():
            if only_new and label != "new":
                continue
            got[label] = run_scene(java, label, jar, name, props, a.timeout)
        row = {"scene": name, "diff": None}
        have = [l for l in got if got[l]]
        if len(have) >= 1:
            composite([got[l] for l in have], have, os.path.join(OUT, "%s.jpg" % name))
        if "ref" in got and "new" in got and got["ref"] and got["new"]:
            row["diff"] = diff_pct(got["ref"], got["new"])
        row["have"] = have
        rows.append(row)
        # keep the repo small: only the side-by-side JPEG is kept, full PNGs are deleted
        for l in got:
            if got[l] and os.path.exists(got[l]):
                os.remove(got[l])
    with open(os.path.join(OUT, "report.md"), "w") as f:
        f.write("# Visual run %s\n\n| scene | rendered by | differing pixels (ref vs new) |\n|---|---|---|\n" % time.strftime("%Y-%m-%d %H:%M:%S UTC", time.gmtime()))
        for r in rows:
            f.write("| %s | %s | %s |\n" % (r["scene"], ", ".join(r["have"]) or "NOTHING", "n/a" if r["diff"] is None else "%.2f%%" % r["diff"]))
    print(open(os.path.join(OUT, "report.md")).read())


if __name__ == "__main__":
    main()
