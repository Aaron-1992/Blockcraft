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
TIMES = []

# Seed 12345: spawn is around x=133,z=71; there is a flat grass strip (surface y=66) around x=138..150, z=73..76.
COMMON = ["-Dbc.auto=1", "-Dbc.seed=12345", "-Dbc.mode=1", "-Dbc.rd=3", "-Dbc.lod=32"]
CAM = "-Dbc.cam=133,92,71,45,22"
PLAIN = ["-Dbc.shaders=false"]


def sets(blocks):
    """bc.set hook: 'x,y,z,id;...' - blocks are placed at frame 70 of the run."""
    return "-Dbc.set=" + ";".join("%d,%d,%d,%d" % b for b in blocks)


def farm_blocks():
    """3 rows of crops in all 8 growth stages on wet farmland, a dry plot, an irrigation ditch and hay bales."""
    out = []
    gy = 66  # ground (grass) level of the strip
    for z in range(72, 80):
        out.append((137, gy, z, 50))          # water column west of the field: x=137
        for x in range(138, 150):
            out.append((x, gy + 1, z, 0))     # clear anything above
            out.append((x, gy + 2, z, 0))
    for i in range(8):
        x = 138 + i
        for z, base in ((73, 124), (74, 142), (75, 150)):
            out.append((x, gy, z, 141))       # wet farmland
            out.append((x, gy + 1, z, base + i))
        out.append((x, gy, 76, 123))          # dry, empty farmland row
        out.append((x, gy, 77, 123))
        out.append((x, gy + 1, 77, 124))      # dry farmland with a young wheat plant
    for x, z in ((147, 73), (148, 73), (147, 74), (148, 74)):
        out.append((x, gy + 1, z, 158))
    out.append((147, gy + 2, 73, 158))
    return out


# name -> (props, only_new)
# Software OpenGL is slow, so scenes use the plain renderer, few frames and a short render distance.
SCENES = {
    "title":          (["-Dbc.auto=1", "-Dbc.title=TITLE"], False),
    "day-plain":      (COMMON + [CAM, "-Dbc.time=6000"] + PLAIN + ["-Dbc.shot=100"], False),
    "mobs":           (COMMON + ["-Dbc.cam=140,70,60,0,48", "-Dbc.time=6000"] + PLAIN + ["-Dbc.summon=pig,cow,sheep,chicken,creeper", "-Dbc.shot=110"], False),
    "farm":           (COMMON + ["-Dbc.cam=143.5,71,68.5,0,30", "-Dbc.time=6000"] + PLAIN + [sets(farm_blocks()), "-Dbc.shot=170"], True),
    "farm-close":     (COMMON + ["-Dbc.cam=141.5,69.3,70.8,0,38", "-Dbc.time=6000"] + PLAIN + [sets(farm_blocks()), "-Dbc.shot=170"], True),
    # close-ups: camera at y=65.5 over the beach (surface top y=64), mobs appear 3.2 ahead and 1.3 below the camera
    "sheep36":        (COMMON + ["-Dbc.cam=140,65.5,60,0,22", "-Dbc.time=6000"] + PLAIN + ["-Dbc.summon=sheep,sheared:sheep,baby:sheep", "-Dbc.summonDist=3.2", "-Dbc.summonDy=-1.3", "-Dbc.shot=100"], True),
    "pigs36":         (COMMON + ["-Dbc.cam=140,65.5,60,0,22", "-Dbc.time=6000"] + PLAIN + ["-Dbc.summon=baby:pig,pig,baby:cow,cow", "-Dbc.summonDist=3.2", "-Dbc.summonDy=-1.3", "-Dbc.shot=100"], True),
    "inv-creative-2": (COMMON + [CAM] + PLAIN + ["-Dbc.inv=creative", "-Dbc.page=1", "-Dbc.shot=110"], True),
    "day-shaders":    (["-Dbc.auto=1", "-Dbc.seed=12345", "-Dbc.mode=1", "-Dbc.rd=2", "-Dbc.lod=16", CAM, "-Dbc.time=6000", "-Dbc.shot=20"], True),
}
DEFAULT_SCENES = ["title", "day-plain", "mobs", "farm", "farm-close", "sheep36", "pigs36", "inv-creative-2"]


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
    TIMES.append("%s/%s: %s in %.0fs" % (name, label, status, dt))
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


def release_jar():
    """The blockcraft.jar inside the newest committed update pack (the exact artifact players get), if there is one."""
    packs = sorted(glob.glob(os.path.join(ROOT, "releases", "Blockcraft-UPDATE*-patch.zip")))
    if not packs:
        return None
    import zipfile
    out = os.path.join(ROOT, "build", "release-blockcraft.jar")
    os.makedirs(os.path.dirname(out), exist_ok=True)
    with zipfile.ZipFile(packs[-1]) as z, open(out, "wb") as f:
        f.write(z.read("Blockcraft/blockcraft.jar"))
    print("new jar = Blockcraft/blockcraft.jar from " + os.path.basename(packs[-1]))
    return out


def write_report(rows):
    with open(os.path.join(OUT, "report.md"), "w") as f:
        f.write("# Visual run %s\n\n| scene | rendered by | differing pixels (ref vs new) |\n|---|---|---|\n" % time.strftime("%Y-%m-%d %H:%M:%S UTC", time.gmtime()))
        for r in rows:
            f.write("| %s | %s | %s |\n" % (r["scene"], ", ".join(r["have"]) or "NOTHING", "n/a" if r["diff"] is None else "%.2f%%" % r["diff"]))
        f.write("\n## timings\n\n" + "\n".join("- " + t for t in TIMES) + "\n")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--scenes", default="all")
    ap.add_argument("--jar", action="append", help="label=path (repeatable)")
    ap.add_argument("--timeout", type=int, default=240)
    a = ap.parse_args()
    jars = {}
    if a.jar:
        for j in a.jar:
            k, v = j.split("=", 1)
            jars[k] = os.path.abspath(v)
    else:
        sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
        import runtests as T  # reuses reference_jar()
        jars = {"ref": T.reference_jar(), "new": release_jar() or os.path.join(ROOT, "build", "blockcraft.jar")}
    java = shutil.which("java") or "java"
    names = DEFAULT_SCENES if a.scenes == "all" else [x.strip() for x in a.scenes.split(",") if x.strip()]
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
        write_report(rows)
        # keep the repo small: only the side-by-side JPEG is kept, full PNGs are deleted
        for l in got:
            if got[l] and os.path.exists(got[l]):
                os.remove(got[l])
    write_report(rows)
    print(open(os.path.join(OUT, "report.md")).read())


if __name__ == "__main__":
    main()
