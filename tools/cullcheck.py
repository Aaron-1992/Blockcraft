#!/usr/bin/env python3
"""
A/B check of the culling of hidden sections (CaveCull): every scene is photographed twice by the REAL game under Xvfb + Mesa llvmpipe,
once as it is and once with -Dbc.cavecull=false. The culling only leaves out what cannot be seen, so the two pictures have to be the
same - up to the speckles along the edges of blocks that two runs of one scene differ by anyway (the software renderer is not exact: the
control run, the same scene a second time, shows 1 - 8 % of such pixels). A hole where a hill or a cave wall should be is a BLOB: the
verdict looks at the part of the picture that differs in blobs of at least 5 x 5 pixels (a morphological opening of the difference mask).

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
BASE = ["-Dbc.auto=1", "-Dbc.seed=12345", "-Dbc.gen=1", "-Dbc.mode=1", "-Dbc.rd=5", "-Dbc.lod=48", "-Dbc.time=6000", "-Dbc.weather=calm", "-Dbc.gamma=1", "-Dbc.ambient=0.85", "-Dbc.frozen=1", "-Dbc.nomobs=1", "-Dbc.shot=260"]
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


def blob_pct(a, b):
    """The part of the picture (percent) where two pictures differ in blobs of at least 5 x 5 pixels: edge noise vanishes, a hole does not."""
    mask = a + ".mask.png"
    shots.im("compare", "-fuzz", "6%", "-compose", "src", "-highlight-color", "white", "-lowlight-color", "black", a, b, mask)
    r = shots.im("convert", mask, "-colorspace", "Gray", "-morphology", "Open", "Square:5", "-format", "%[fx:mean*100]", "info:")
    if os.path.exists(mask):
        os.remove(mask)
    try:
        return float(r.stdout.strip())
    except ValueError:
        return None


BLOB_LIMIT = 0.05  # percent of the picture: 115 solid pixels in a 640 x 360 frame; a missing hill or cave wall is thousands


def diff_image(a, b, out):
    """A picture of where two pictures differ: red on a faded copy of the first one."""
    tmp = out + ".png"
    shots.im("compare", "-fuzz", "6%", a, b, tmp)
    shots.im("convert", tmp, "-resize", "640x", "-quality", "70", out)
    if os.path.exists(tmp):
        os.remove(tmp)


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
    rows = ["# Culling check %s (window %s)\n\n| scene | differing pixels | of them in blobs (>= 5 x 5) | same scene twice: pixels / in blobs | verdict |\n|---|---|---|---|---|\n" % (time.strftime("%Y-%m-%d %H:%M:%S UTC", time.gmtime()), a.win)]
    bad = 0
    for n in names:
        extra, switch = SCENES[n]
        props = BASE + ["-Dbc.win=" + a.win] + extra
        on = shots.run_scene(java, "on", a.jar, n, props, a.timeout)
        off = shots.run_scene(java, "off", a.jar, n, props + [switch], a.timeout)
        pct = None
        noise = None
        blob = None
        noise_blob = None
        if on and off:
            pct = shots.diff_pct(on, off)
            blob = blob_pct(on, off)
            shots.composite([on, off], ["as it is", "without " + ("the shadow map's window" if switch == SHADOW else "culling")], os.path.join(shots.OUT, n + ".jpg"))
            if pct is not None and pct >= 0.02:
                # where do the pictures differ? (red = differs): blocks of red would be a hole in the world, scattered dots are noise
                diff = os.path.join(shots.OUT, n + "-diff.jpg")
                diff_image(on, off, diff)
            if pct is not None and (pct >= 0.3 or (blob is not None and blob >= BLOB_LIMIT)):
                # the control: the SAME scene a second time, as it is. What two identical runs differ by is not the culling's fault.
                on2 = shots.run_scene(java, "on2", a.jar, n, props, a.timeout)
                if on2:
                    noise = shots.diff_pct(on, on2)
                    noise_blob = blob_pct(on, on2)
                    if noise is not None and noise >= 0.02:
                        diff_image(on, on2, os.path.join(shots.OUT, n + "-noise.jpg"))
                    if os.path.exists(on2):
                        os.remove(on2)
        if pct is None:
            verdict = "NO PICTURE"
        elif blob is not None and blob < BLOB_LIMIT:
            verdict = "same (no blobs of difference)"
        elif blob is None and pct < 0.3:
            verdict = "same"
        elif blob is not None and noise_blob is not None and blob - noise_blob < BLOB_LIMIT:
            verdict = "same (no more blobs than in two identical runs)"
        else:
            verdict = "DIFFERENT"
        if not verdict.startswith("same"):
            bad += 1
        fmt = lambda v: "-" if v is None else "%.3f %%" % v
        rows.append("| %s | %s | %s | %s / %s | %s |\n" % (n, fmt(pct), fmt(blob), fmt(noise), fmt(noise_blob), verdict))
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
