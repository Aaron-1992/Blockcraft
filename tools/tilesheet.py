#!/usr/bin/env python3
"""Blockcraft V6.25 - the ONE editable tilesheet.

    python3 tools/tilesheet.py export            regenerate textures/ from the game's own art
    python3 tools/tilesheet.py import sheet.png  install an edited sheet into the game/release
    python3 tools/tilesheet.py check  sheet.png  validate a sheet (1024x1024 PNG)

The sheet is a 1024x1024 RGBA PNG = 32x32 cells of 32x32 pixels. Cell row*32+col is
atlas tile row*32+col. Edit it in Aseprite (keep the canvas size + alpha), save as PNG,
then either:
  * drop it next to Blockcraft.exe as tilesheet.png and type /tilesheet in game, or
  * run `import` here and rebuild the release so it ships with the game.
"""
import os
import shutil
import struct
import subprocess
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import build as B  # noqa: E402

ROOT = B.ROOT
JAR = os.path.join(B.BUILD, "blockcraft.jar")
MASTER = os.path.join(ROOT, "textures", "Blockcraft-tilesheet.png")
SHIPPED = os.path.join(ROOT, "package", "tilesheet.png")
MASTER_DEFAULT = os.path.join(ROOT, "textures", "Blockcraft-tilesheet-default.png")
SHIPPED_DEFAULT = os.path.join(ROOT, "package", "tilesheet-default.png")


def check(path):
    with open(path, "rb") as f:
        hdr = f.read(24)
    if len(hdr) < 24 or hdr[:8] != b"\x89PNG\r\n\x1a\n":
        raise ValueError("Not a PNG: " + path)
    w, h = struct.unpack(">II", hdr[16:24])
    if (w, h) != (1024, 1024):
        raise ValueError("A Blockcraft tilesheet must be exactly 1024x1024 (got %dx%d)" % (w, h))
    return w, h


def export(out="textures"):
    if not os.path.isfile(JAR):
        raise SystemExit("Run `python3 tools/build.py` first (missing %s)" % JAR)
    subprocess.run([B.java_exe(), "-Djava.awt.headless=true", "-jar", JAR,
                    "--export-tilesheet", out], check=True, cwd=ROOT)
    src = os.path.join(ROOT, out, "Blockcraft-tilesheet.png")
    check(src)
    ref = os.path.join(ROOT, out, "Blockcraft-tilesheet-default.png")
    check(ref)
    shutil.copyfile(src, SHIPPED)
    shutil.copyfile(ref, SHIPPED_DEFAULT)
    print("[tilesheet] %s -> %s" % (src, SHIPPED))
    print("[tilesheet] %s -> %s (pristine reference, never overwritten by import)" % (ref, SHIPPED_DEFAULT))


def install(path):
    path = os.path.abspath(path)
    check(path)
    os.makedirs(os.path.dirname(MASTER), exist_ok=True)
    shutil.copyfile(path, MASTER)
    shutil.copyfile(path, SHIPPED)
    # NOTE: the pristine reference is deliberately NOT touched by import.
    print("[tilesheet] installed %s" % path)
    print("[tilesheet]   master : %s" % MASTER)
    print("[tilesheet]   shipped: %s  (run build.py --patch + make_release.py to publish)" % SHIPPED)


def main(argv):
    cmd = argv[1] if len(argv) > 1 else "export"
    if cmd == "export":
        export(argv[2] if len(argv) > 2 else "textures")
    elif cmd == "import":
        if len(argv) < 3:
            raise SystemExit("Usage: python3 tools/tilesheet.py import <edited.png>")
        install(argv[2])
    elif cmd == "check":
        if len(argv) < 3:
            raise SystemExit("Usage: python3 tools/tilesheet.py check <png>")
        w, h = check(argv[2])
        print("[tilesheet] OK %s %dx%d" % (argv[2], w, h))
    else:
        raise SystemExit(__doc__)


if __name__ == "__main__":
    main(sys.argv)
