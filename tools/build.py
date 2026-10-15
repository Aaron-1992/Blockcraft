#!/usr/bin/env python3
"""
Builds Blockcraft from source.

    python3 tools/build.py            compile + build/blockcraft.jar + build/Blockcraft.exe
    python3 tools/build.py --patch    ...and releases/Blockcraft-UPDATE<ver>-patch.zip   (small, ~6 MB)
    python3 tools/build.py --full     ...and releases/Blockcraft-1.12-UPDATE<ver>-windows.zip (full, ~55 MB)

Layout it relies on:
    src/            game sources (package bc)
    res/            files that go into the jar root (font.bin)
    libs/           lwjgl-bundle.jar  = LWJGL classes + Windows/Linux natives (extracted from the 3.5 jar)
    launcher/       launch4j-stub.bin = the 69 KB Launch4j launcher. Blockcraft.exe is simply  stub + jar.
    package/        README.txt and Blockcraft-debug.bat that are shipped next to the exe
    Blockcraft-1.12-UPDATE3.5-windows.zip   the base kit (bundled Windows JRE) used for --full

The compiler is `javac` if a JDK is on PATH, otherwise the ECJ compiler set up by tools/bootstrap.py.
Either way the code is checked against the Java 21 API (the JRE bundled with the game).
"""
import argparse
import hashlib
import json
import os
import re
import shutil
import subprocess
import sys
import tempfile
import zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BUILD = os.path.join(ROOT, "build")
TC = os.environ.get("BC_TOOLCHAIN", os.path.join(tempfile.gettempdir(), "blockcraft-toolchain"))
BASE_ZIP = os.path.join(ROOT, "Blockcraft-1.12-UPDATE3.5-windows.zip")
FIXED_TIME = (2026, 1, 1, 0, 0, 0)  # deterministic archives


def sh(cmd, **kw):
    return subprocess.run(cmd, **kw)


def find_sources(*roots):
    out = []
    for r in roots:
        for dp, _, fs in os.walk(r):
            out += [os.path.join(dp, f) for f in fs if f.endswith(".java")]
    return sorted(out)


def toolchain():
    """Returns ('javac', None) or ('ecj', {java, ecj, winjre})."""
    if shutil.which("javac"):
        return "javac", None
    cfg = os.path.join(TC, "toolchain.json")
    if not os.path.exists(cfg):
        print("No JDK on PATH and no toolchain yet -> running tools/bootstrap.py")
        sh([sys.executable, os.path.join(ROOT, "tools", "bootstrap.py")], check=True)
    with open(cfg) as f:
        return "ecj", json.load(f)


def java_exe():
    kind, tc = toolchain()
    return shutil.which("java") if kind == "javac" else tc["java"]


def compile_java(out_dir, source_roots, classpath, debug=True):
    """Compiles all .java files under source_roots into out_dir. Returns True on success."""
    shutil.rmtree(out_dir, ignore_errors=True)
    os.makedirs(out_dir)
    sources = find_sources(*source_roots)
    argfile = os.path.join(BUILD, "sources.txt")
    os.makedirs(BUILD, exist_ok=True)
    with open(argfile, "w") as f:
        f.write("\n".join(sources))
    cp = os.pathsep.join(classpath)
    kind, tc = toolchain()
    if kind == "javac":
        cmd = ["javac", "--release", "21", "-encoding", "UTF-8", "-nowarn", "-proc:none"]
        if debug:
            cmd.append("-g")
        cmd += ["-cp", cp, "-d", out_dir, "@" + argfile]
    else:
        cmd = [tc["java"], "-Xmx1g", "-jar", tc["ecj"], "-source", "21", "-target", "21", "-encoding", "UTF-8",
               "-nowarn", "--system", tc["winjre"]]
        if debug:
            cmd.append("-g")
        cmd += ["-cp", cp, "-d", out_dir, "@" + argfile]
    print("[build] compiling %d source files with %s ..." % (len(sources), kind))
    r = sh(cmd)
    return r.returncode == 0


def game_version():
    """'4.0' etc, parsed from Game.VERSION_STR or Game.TITLE_STR (single source of truth)."""
    with open(os.path.join(ROOT, "src", "bc", "Game.java"), encoding="utf-8") as f:
        txt = f.read()
    m = re.search(r'VERSION_STR\s*=\s*"([0-9.]+)"', txt) or re.search(r'TITLE_STR\s*=\s*"[^"]*(?:Update\s+|V)([0-9.]+)"', txt)
    if not m:
        sys.exit("could not find the version in Game.java")
    return m.group(1)


def source_fingerprint():
    """Hash source paths and bytes, not git HEAD: uncommitted fixes must be identified too."""
    digest = hashlib.sha256()
    for path in find_sources(os.path.join(ROOT, "src")):
        digest.update(os.path.relpath(path, ROOT).replace(os.sep, "/").encode("utf-8") + b"\0")
        with open(path, "rb") as f:
            digest.update(f.read())
        digest.update(b"\0")
    return digest.hexdigest()


def build_jar(classes_dir, jar_path):
    """LWJGL bundle + compiled classes + resources -> jar with Main-Class bc.Main."""
    entries = {}  # name -> (bytes, compress_type)
    with zipfile.ZipFile(os.path.join(ROOT, "libs", "lwjgl-bundle.jar")) as z:
        for i in z.infolist():
            if i.is_dir() or i.filename.upper() == "META-INF/MANIFEST.MF":
                continue
            entries[i.filename] = z.read(i)
    for base in (classes_dir, os.path.join(ROOT, "res")):
        for dp, _, fs in os.walk(base):
            for f in fs:
                p = os.path.join(dp, f)
                rel = os.path.relpath(p, base).replace(os.sep, "/")
                if rel == "sounds/soundbank.bin":
                    continue
                entries[rel] = open(p, "rb").read()
    receipt = "version=%s\nsource.sha256=%s\n" % (game_version(), source_fingerprint())
    for name in ("MidiCompiler", "NoteBlocks", "Redstone319", "Blocks", "Creative", "RasterAudio", "RasterClip", "RasterMedia", "ShadowMachine", "SoundEngine", "RasterVM", "RasterComputer", "RasterPrograms", "RasterArcade", "Circuit", "CircuitComputer", "Facing", "BlockState", "Shapes", "Mesher", "World", "Net319", "Aquatic", "RedstoneLogic", "WaterPhysics", "Game", "Renderer", "PlayerModel", "Tex", "TexPixel32", "TexRemaining32", "TexMaterials32", "TextureEditor", "ResourcePacks", "UtilityModels", "ChestLids", "TexUtility32", "TexTools32", "TexBuildingReview32", "TexColorNature32", "TexGlazedReview32", "TexItems32", "TexNature32", "PixelArt32", "WeaponPose", "BowSprite", "HandLighting", "ShaderSrc", "Shaders"):
        receipt += "class.%s.sha256=%s\n" % (name, hashlib.sha256(entries["bc/%s.class" % name]).hexdigest())
    receipt += "media.shadow-signal.sha256=%s\n" % hashlib.sha256(entries["media/shadow-signal.bcv"]).hexdigest()
    entries["bc-build.properties"] = receipt.encode("utf-8")
    os.makedirs(os.path.dirname(jar_path), exist_ok=True)
    with zipfile.ZipFile(jar_path, "w", zipfile.ZIP_DEFLATED, compresslevel=9) as z:
        zi = zipfile.ZipInfo("META-INF/MANIFEST.MF", FIXED_TIME)
        zi.compress_type = zipfile.ZIP_DEFLATED
        z.writestr(zi, "Manifest-Version: 1.0\r\nMain-Class: bc.Main\r\n\r\n")
        for name in sorted(entries):
            zi = zipfile.ZipInfo(name, FIXED_TIME)
            zi.compress_type = zipfile.ZIP_DEFLATED
            zi.external_attr = 0o644 << 16
            z.writestr(zi, entries[name])
    print("[build] %s  (%.2f MB, %d entries)" % (os.path.relpath(jar_path, ROOT), os.path.getsize(jar_path) / 1e6, len(entries) + 1))


def build_exe(jar_path, exe_path):
    """Launch4j 'wrapped' exe = launcher stub + the jar appended (Java opens the exe as a zip)."""
    with open(os.path.join(ROOT, "launcher", "launch4j-stub.bin"), "rb") as f:
        stub = f.read()
    with open(jar_path, "rb") as f:
        jar = f.read()
    with open(exe_path, "wb") as f:
        f.write(stub + jar)
    print("[build] %s  (%.2f MB)" % (os.path.relpath(exe_path, ROOT), os.path.getsize(exe_path) / 1e6))


def add(z, arcname, data, mode=0o644):
    zi = zipfile.ZipInfo(arcname, FIXED_TIME)
    zi.compress_type = zipfile.ZIP_DEFLATED
    zi.external_attr = mode << 16
    z.writestr(zi, data)


def package_files():
    """Files shipped next to the exe: arcname (inside Blockcraft/) -> bytes."""
    pkg = os.path.join(ROOT, "package")
    return {n: open(os.path.join(pkg, n), "rb").read() for n in sorted(os.listdir(pkg))}


def build_infdev_mod_jar(classes_dir):
    """Packages the external Minecraft Infdev (2010) total-conversion mod into mods/infdev-2010.jar."""
    mod_jar = os.path.join(ROOT, "mods", "infdev-2010.jar")
    os.makedirs(os.path.dirname(mod_jar), exist_ok=True)
    with zipfile.ZipFile(mod_jar, "w", zipfile.ZIP_DEFLATED, compresslevel=9) as z:
        add(z, "META-INF/MANIFEST.MF", "Manifest-Version: 1.0\r\nMod-Class: bc.InfdevMod\r\n\r\n")
        add(z, "META-INF/services/bc.BlockcraftMod", "bc.InfdevMod\n")
        bc_dir = os.path.join(classes_dir, "bc")
        if os.path.isdir(bc_dir):
            for fn in sorted(os.listdir(bc_dir)):
                if fn.startswith("Infdev") and fn.endswith(".class"):
                    add(z, "bc/" + fn, open(os.path.join(bc_dir, fn), "rb").read())
    print("[build] %s  (%.1f KB)" % (os.path.relpath(mod_jar, ROOT), os.path.getsize(mod_jar) / 1e3))
    return mod_jar


def build_backrooms_mod_jar(classes_dir):
    """Packages the Backrooms & Stalker horror mod into mods/backrooms-stalker.jar."""
    mod_jar = os.path.join(ROOT, "mods", "backrooms-stalker.jar")
    os.makedirs(os.path.dirname(mod_jar), exist_ok=True)
    with zipfile.ZipFile(mod_jar, "w", zipfile.ZIP_DEFLATED, compresslevel=9) as z:
        add(z, "META-INF/MANIFEST.MF", "Manifest-Version: 1.0\r\nMod-Class: bc.BackroomsStalkerMod\r\n\r\n")
        add(z, "META-INF/services/bc.BlockcraftMod", "bc.BackroomsStalkerMod\n")
        bc_dir = os.path.join(classes_dir, "bc")
        if os.path.isdir(bc_dir):
            for fn in sorted(os.listdir(bc_dir)):
                if fn.startswith("Backrooms") and fn.endswith(".class"):
                    add(z, "bc/" + fn, open(os.path.join(bc_dir, fn), "rb").read())
    print("[build] %s  (%.1f KB)" % (os.path.relpath(mod_jar, ROOT), os.path.getsize(mod_jar) / 1e3))
    return mod_jar


def build_patch_zip(ver, exe_path, jar_path, mod_jar_path=None):
    out = os.path.join(ROOT, "releases", "Blockcraft-UPDATE%s-patch.zip" % ver)
    os.makedirs(os.path.dirname(out), exist_ok=True)
    howto = (
        "BLOCKCRAFT UPDATE %s - HOW TO INSTALL\r\n"
        "=====================================\r\n"
        "This is a small update pack. It does NOT contain Java - it re-uses the one from your\r\n"
        "existing Blockcraft folder (the one you got from the Update 3.5 zip).\r\n\r\n"
        "1. Extract this zip somewhere (e.g. your Desktop). You get a folder called 'Blockcraft'.\r\n"
        "2. Copy that 'Blockcraft' folder INTO the folder that already contains your old\r\n"
        "   'Blockcraft' folder, and choose 'Replace the files in the destination'.\r\n"
        "   (Or: copy Blockcraft.exe, blockcraft.jar and README.txt over the old ones.)\r\n"
        "3. Start Blockcraft.exe. Your worlds and options are kept (they live in\r\n"
        "   %%USERPROFILE%%\\.blockcraft112).\r\n\r\n"
        "Starting from scratch? Extract Blockcraft-1.12-UPDATE3.5-windows.zip first, then do the above.\r\n"
        % ver
    )
    with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED, compresslevel=9) as z:
        add(z, "HOW-TO-INSTALL-THIS-UPDATE.txt", howto)
        add(z, "Blockcraft/Blockcraft.exe", open(exe_path, "rb").read(), 0o755)
        add(z, "Blockcraft/blockcraft.jar", open(jar_path, "rb").read())
        for n, data in package_files().items():
            add(z, "Blockcraft/" + n, data)
        if mod_jar_path and os.path.isfile(mod_jar_path):
            add(z, "Blockcraft/mods/infdev-2010.jar", open(mod_jar_path, "rb").read())
        br_jar = os.path.join(ROOT, "mods", "backrooms-stalker.jar")
        if os.path.isfile(br_jar):
            add(z, "Blockcraft/mods/backrooms-stalker.jar", open(br_jar, "rb").read())
        mod_readme = os.path.join(ROOT, "mods", "README.txt")
        if os.path.isfile(mod_readme):
            add(z, "Blockcraft/mods/README.txt", open(mod_readme, "rb").read())
        mod_doc = os.path.join(ROOT, "docs", "MODDING.md")
        if os.path.isfile(mod_doc):
            add(z, "Blockcraft/MODDING.md", open(mod_doc, "rb").read())
        sb_bin = os.path.join(ROOT, "res", "sounds", "soundbank.bin")
        if os.path.isfile(sb_bin):
            add(z, "Blockcraft/sounds/soundbank.bin", open(sb_bin, "rb").read())
    print("[build] %s  (%.2f MB)" % (os.path.relpath(out, ROOT), os.path.getsize(out) / 1e6))
    return out


def compose_full_zip(out, replacements):
    """Complete game zip = the 3.5 base kit (bundled JRE etc.) with some files replaced.

    replacements: archive name inside the zip (e.g. 'Blockcraft/Blockcraft.exe') -> (bytes, unix mode)
    Everything else is copied byte-for-byte from the base zip (deterministic timestamps).
    """
    os.makedirs(os.path.dirname(out), exist_ok=True)
    with zipfile.ZipFile(BASE_ZIP) as base, zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED, compresslevel=6) as z:
        for i in base.infolist():
            if i.filename in replacements:
                continue
            zi = zipfile.ZipInfo(i.filename, FIXED_TIME)
            zi.compress_type = zipfile.ZIP_DEFLATED
            zi.external_attr = i.external_attr
            z.writestr(zi, base.read(i))
        for name in sorted(replacements):
            data, mode = replacements[name]
            add(z, name, data, mode)
    print("[build] %s  (%.2f MB)" % (os.path.relpath(out, ROOT), os.path.getsize(out) / 1e6))
    return out


def build_full_zip(ver, exe_path, jar_path, mod_jar_path=None):
    out = os.path.join(ROOT, "releases", "Blockcraft-1.12-UPDATE%s-windows.zip" % ver)
    rep = {
        "Blockcraft/Blockcraft.exe": (open(exe_path, "rb").read(), 0o755),
        "Blockcraft/blockcraft.jar": (open(jar_path, "rb").read(), 0o644),
    }
    for n, data in package_files().items():
        rep["Blockcraft/" + n] = (data, 0o644)
    if mod_jar_path and os.path.isfile(mod_jar_path):
        rep["Blockcraft/mods/infdev-2010.jar"] = (open(mod_jar_path, "rb").read(), 0o644)
    br_jar = os.path.join(ROOT, "mods", "backrooms-stalker.jar")
    if os.path.isfile(br_jar):
        rep["Blockcraft/mods/backrooms-stalker.jar"] = (open(br_jar, "rb").read(), 0o644)
    mod_readme = os.path.join(ROOT, "mods", "README.txt")
    if os.path.isfile(mod_readme):
        rep["Blockcraft/mods/README.txt"] = (open(mod_readme, "rb").read(), 0o644)
    mod_doc = os.path.join(ROOT, "docs", "MODDING.md")
    if os.path.isfile(mod_doc):
        rep["Blockcraft/MODDING.md"] = (open(mod_doc, "rb").read(), 0o644)
    sb_bin = os.path.join(ROOT, "res", "sounds", "soundbank.bin")
    if os.path.isfile(sb_bin):
        rep["Blockcraft/sounds/soundbank.bin"] = (open(sb_bin, "rb").read(), 0o644)
    return compose_full_zip(out, rep)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--patch", action="store_true", help="also write the small update-pack zip into releases/")
    ap.add_argument("--full", action="store_true", help="also write the complete zip (with the bundled JRE) into releases/")
    ap.add_argument("--no-debug", action="store_true", help="omit local-variable debug info")
    a = ap.parse_args()

    lwjgl = os.path.join(ROOT, "libs", "lwjgl-bundle.jar")
    classes = os.path.join(BUILD, "classes")
    if not compile_java(classes, [os.path.join(ROOT, "src")], [lwjgl], debug=not a.no_debug):
        sys.exit("COMPILE FAILED")
    ver = game_version()
    jar = os.path.join(BUILD, "blockcraft.jar")
    exe = os.path.join(BUILD, "Blockcraft.exe")
    build_jar(classes, jar)
    build_exe(jar, exe)
    mod_jar = build_infdev_mod_jar(classes)
    build_backrooms_mod_jar(classes)
    if a.patch:
        build_patch_zip(ver, exe, jar, mod_jar)
    if a.full:
        build_full_zip(ver, exe, jar, mod_jar)
    print("[build] OK  (Update %s)" % ver)


if __name__ == "__main__":
    main()
