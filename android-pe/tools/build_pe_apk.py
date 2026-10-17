#!/usr/bin/env python3
"""
Builds the standalone Android Pocket Edition APK for Blockcraft PE (com.blockcraft.pe),
built as a standalone Android app (API 26+), with responsive landscape menus and no model-specific runtime assumptions.

Outputs into build/release-pe/ (strictly separated from PC releases):
    Blockcraft-PE-v<ver>.apk
    Blockcraft-PE-v<ver>-android-bundle.zip
    release-notes.md
    VERSION
"""
import glob
import os
import shutil
import struct
import subprocess
import sys
import tempfile
import zlib
import zipfile

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
PE_DIR = os.path.join(ROOT, "android-pe")
OUT_DIR = os.path.join(ROOT, "build", "release-pe")


def write_launcher_icon_png(path, size=192):
    """Generates a crisp 192x192 RGBA PNG Blockcraft PE Grass Block icon."""
    os.makedirs(os.path.dirname(path), exist_ok=True)
    raw_rows = bytearray()
    for y in range(size):
        raw_rows.append(0)  # PNG filter type 0 (None)
        gy = (y * 16) // size
        for x in range(size):
            gx = (x * 16) // size
            border = gx == 0 or gx == 15 or gy == 0 or gy == 15
            fringe = 4 + ((gx * 7 + 3) & 3)
            noise = ((gx * 37 + gy * 91) & 15) - 8
            if border:
                r, g, b = 36, 48, 28
            elif gy < 3:
                r, g, b = max(0, min(255, 96 + noise)), max(0, min(255, 186 + noise)), max(0, min(255, 62 + noise))
            elif gy < fringe:
                r, g, b = max(0, min(255, 78 + noise)), max(0, min(255, 158 + noise)), max(0, min(255, 48 + noise))
            else:
                r, g, b = max(0, min(255, 134 + noise)), max(0, min(255, 96 + noise)), max(0, min(255, 67 + noise))
            raw_rows.extend((r, g, b, 255))

    def chunk(tag, data):
        crc = zlib.crc32(tag + data) & 0xFFFFFFFF
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", crc)

    png = bytearray(b"\x89PNG\r\n\x1a\n")
    ihdr = struct.pack(">IIBBBBB", size, size, 8, 6, 0, 0, 0)
    png.extend(chunk(b"IHDR", ihdr))
    png.extend(chunk(b"IDAT", zlib.compress(bytes(raw_rows), 9)))
    png.extend(chunk(b"IEND", b""))
    with open(path, "wb") as f:
        f.write(png)


def find_android_sdk():
    sdk = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT") or "/usr/local/lib/android/sdk"
    if not os.path.isdir(sdk):
        raise RuntimeError(f"Android SDK not found at {sdk}")

    platforms = sorted(glob.glob(os.path.join(sdk, "platforms", "android-*")),
                       key=lambda p: int(p.rsplit("-", 1)[-1]) if p.rsplit("-", 1)[-1].isdigit() else 0)
    build_tools = sorted(glob.glob(os.path.join(sdk, "build-tools", "*")),
                         key=lambda p: [int(x) if x.isdigit() else 0 for x in os.path.basename(p).split(".")])
    if not platforms or not build_tools:
        raise RuntimeError(f"Android platforms or build-tools missing in {sdk}")

    # Prefer android-34 if installed, else newest
    plat_dir = platforms[-1]
    for p in platforms:
        if p.endswith("android-34"):
            plat_dir = p
            break
    bt_dir = build_tools[-1]
    for b in build_tools:
        if os.path.basename(b).startswith("34."):
            bt_dir = b
            break
    return sdk, os.path.join(plat_dir, "android.jar"), bt_dir


def run_cmd(cmd, **kw):
    print("+", " ".join(cmd))
    res = subprocess.run(cmd, **kw)
    if res.returncode != 0:
        raise RuntimeError(f"Command failed ({res.returncode}): {' '.join(cmd)}")
    return res


def update_manifest_icon(manifest_src, manifest_dst):
    with open(manifest_src, "r", encoding="utf-8") as f:
        xml = f.read()
    if "android:icon=" not in xml:
        xml = xml.replace(
            'android:label="Blockcraft PE"',
            'android:label="Blockcraft PE"\n        android:icon="@drawable/ic_launcher"'
        )
    with open(manifest_dst, "w", encoding="utf-8") as f:
        f.write(xml)


def build_readme(version, apk_name):
    return f"""========================================================================
  BLOCKCRAFT PE (POCKET EDITION) v{version} — ANDROID
========================================================================

Standalone Android Pocket Edition. PC and PE releases remain separate; this
bundle contains only the Android app and its installation notes.

WHAT'S NEW IN BLOCKCRAFT PE v{version}
--------------------------------------
* World-generation performance: new and loaded worlds warm the inner 9x9
  chunk area, then stream the outer view ring incrementally. The title panorama
  uses a non-persistent 25-chunk preview instead of generating a playable save.
* Biome climate is sampled on globally aligned 4x4 cells and cached per chunk;
  a 16x16 heightmap is reused by terrain, vegetation, and structure placement.
* Added seed-deterministic, water-filled river valleys and one bounded landmark
  site per 6x6 chunk region. Desert, mountain, forest, snowy, and meadow regions
  select different structures, with flat-pad and headroom checks.
* Chunk section meshing now visits distance-ring perimeters directly while
  preserving the existing per-frame rebuild budget.
* Save format remains compatible with v1.10. Edited chunks and player progress
  persist; unmodified chunks use the updated deterministic terrain generator.

Existing features include the 16-color material families, survival dye mixing,
water-hardening concrete powder, a 27-slot backpack, touch controls, saved
infinite worlds, pixel-art textures, and optional high-refresh display mode.

INSTALL
-------
1. Install `{apk_name}` on a supported Android device (Android 8.0 / API 26+).
2. If prompted, allow installation from the source you used to download the APK.
3. Launch Blockcraft PE in landscape mode. Worlds are stored locally on-device.
"""


def build_release_notes(version, apk_name, bundle_name):
    return f"""## Blockcraft PE v{version} — Android

Standalone Pocket Edition release for Android 8.0/API 26 and newer. The PE
bundle is separate from Blockcraft's desktop release.

### Downloads
- **`{apk_name}`** — install directly on an Android device.
- **`{bundle_name}`** — APK plus README.

### What's new
- Reduced synchronous world warm-up to the inner 9x9 chunk area; remaining
  chunks stream incrementally. The title panorama is now a non-persistent,
  25-chunk preview and no longer creates a full playable world/save.
- Replaced temporary terrain arrays with a cached 16x16 heightmap and a compact
  4x4 biome cache. Biome climate samples are globally aligned in 4-block cells.
- Added deterministic river valleys and region-based, biome-aware structures:
  desert pyramids, mountain watchtowers, forest explorer cabins, snowy cabins,
  and meadow ruins. Structure sites reject unsafe slopes and insufficient headroom.
- Chunk loading and VBO section meshing traverse each distance-ring perimeter
  once, retaining the existing generation and GPU rebuild budgets.

The save format is unchanged from v1.10: edited chunks and player progress are
preserved. Unmodified chunks regenerate with the updated terrain and structure
rules when visited, so terrain next to previously edited chunks may transition.

### Existing PE features
World of Color materials and dyes, survival color crafting, concrete-powder
physics, local infinite-world saves, touch UI, pixel-art textures, and
high-refresh mode selection remain included. PE remains separate from the wider
desktop feature set.
"""


def main():
    version = sys.argv[1] if len(sys.argv) > 1 else "1.11"
    version = version.lstrip("vV")
    if version.startswith("pe-"):
        version = version[3:].lstrip("vV")

    print(f"=== Running headless verification (PeCoreTest) for Blockcraft PE v{version} ===")
    run_cmd([sys.executable, os.path.join(PE_DIR, "tools", "verify_pe_local.py")], cwd=ROOT)

    sdk, android_jar, bt_dir = find_android_sdk()
    aapt2 = os.path.join(bt_dir, "aapt2")
    d8 = os.path.join(bt_dir, "d8")
    zipalign = os.path.join(bt_dir, "zipalign")
    apksigner = os.path.join(bt_dir, "apksigner")

    shutil.rmtree(OUT_DIR, ignore_errors=True)
    os.makedirs(OUT_DIR, exist_ok=True)

    work = tempfile.mkdtemp(prefix="blockcraft-pe-apk-")
    try:
        res_dir = os.path.join(work, "res")
        write_launcher_icon_png(os.path.join(res_dir, "mipmap-nodpi", "ic_launcher.png"), 192)
        write_launcher_icon_png(os.path.join(res_dir, "drawable-nodpi", "ic_launcher.png"), 192)

        manifest_path = os.path.join(work, "AndroidManifest.xml")
        shutil.copyfile(os.path.join(PE_DIR, "AndroidManifest.xml"), manifest_path)

        compiled_res_zip = os.path.join(work, "compiled_res.zip")
        run_cmd([aapt2, "compile", "--dir", res_dir, "-o", compiled_res_zip])

        unaligned_apk = os.path.join(work, "unaligned.apk")
        gen_r_dir = os.path.join(work, "gen")
        os.makedirs(gen_r_dir, exist_ok=True)
        ver_parts = [int(p) for p in version.split(".") if p.isdigit()]
        ver_code = str((ver_parts[0] * 100 + (ver_parts[1] * 10 if len(ver_parts) > 1 else 0)) if ver_parts else 110)
        run_cmd([
            aapt2, "link",
            "-I", android_jar,
            "--manifest", manifest_path,
            "--java", gen_r_dir,
            "--min-sdk-version", "26",
            "--target-sdk-version", "34",
            "--version-code", ver_code,
            "--version-name", version,
            "-o", unaligned_apk,
            compiled_res_zip,
        ])

        classes_dir = os.path.join(work, "classes")
        os.makedirs(classes_dir, exist_ok=True)
        java_sources = []
        for root_dp, _, files in os.walk(os.path.join(PE_DIR, "src")):
            for fn in sorted(files):
                if fn.endswith(".java"):
                    java_sources.append(os.path.join(root_dp, fn))
        for root_dp, _, files in os.walk(gen_r_dir):
            for fn in sorted(files):
                if fn.endswith(".java"):
                    java_sources.append(os.path.join(root_dp, fn))

        run_cmd([
            "javac", "--release", "8", "-encoding", "UTF-8",
            "-cp", android_jar,
            "-d", classes_dir,
        ] + java_sources)

        class_files = []
        for root_dp, _, files in os.walk(classes_dir):
            for fn in sorted(files):
                if fn.endswith(".class"):
                    class_files.append(os.path.join(root_dp, fn))

        dex_dir = os.path.join(work, "dex")
        os.makedirs(dex_dir, exist_ok=True)
        run_cmd([
            d8, "--release", "--min-api", "26",
            "--lib", android_jar,
            "--output", dex_dir,
        ] + class_files)

        # Add classes.dex and font.bin asset into unaligned_apk
        with zipfile.ZipFile(unaligned_apk, "a") as zf:
            zf.write(os.path.join(dex_dir, "classes.dex"), "classes.dex", compress_type=zipfile.ZIP_DEFLATED)
            font_bin = os.path.join(ROOT, "res", "font.bin")
            if os.path.exists(font_bin):
                zf.write(font_bin, "assets/font.bin", compress_type=zipfile.ZIP_STORED)

        aligned_apk = os.path.join(work, "aligned.apk")
        run_cmd([zipalign, "-f", "-p", "4", unaligned_apk, aligned_apk])

        keystore = os.path.join(work, "pe-release.jks")
        run_cmd([
            "keytool", "-genkeypair", "-v",
            "-keystore", keystore,
            "-storepass", "blockcraftpe",
            "-keypass", "blockcraftpe",
            "-alias", "pekey",
            "-keyalg", "RSA",
            "-keysize", "2048",
            "-validity", "10000",
            "-dname", "CN=Blockcraft PE, OU=Mobile, O=Blockcraft, L=Vienna, S=AT, C=AT",
        ])

        apk_name = f"Blockcraft-PE-v{version}.apk"
        final_apk = os.path.join(OUT_DIR, apk_name)
        run_cmd([
            apksigner, "sign",
            "--ks", keystore,
            "--ks-key-alias", "pekey",
            "--ks-pass", "pass:blockcraftpe",
            "--key-pass", "pass:blockcraftpe",
            "--v1-signing-enabled", "true",
            "--v2-signing-enabled", "true",
            "--v3-signing-enabled", "true",
            "--out", final_apk,
            aligned_apk,
        ])
        run_cmd([apksigner, "verify", "--verbose", final_apk])

        readme_txt = build_readme(version, apk_name)
        bundle_name = f"Blockcraft-PE-v{version}-android-bundle.zip"
        bundle_path = os.path.join(OUT_DIR, bundle_name)
        with zipfile.ZipFile(bundle_path, "w", compression=zipfile.ZIP_DEFLATED) as bz:
            bz.write(final_apk, apk_name)
            bz.writestr("README-PE.txt", readme_txt)

        with open(os.path.join(OUT_DIR, "release-notes.md"), "w", encoding="utf-8") as f:
            f.write(build_release_notes(version, apk_name, bundle_name))
        with open(os.path.join(OUT_DIR, "VERSION"), "w", encoding="utf-8") as f:
            f.write(version + "\n")

        print(f"SUCCESS: Built {final_apk} ({os.path.getsize(final_apk)} bytes) and {bundle_path} ({os.path.getsize(bundle_path)} bytes)")
    finally:
        shutil.rmtree(work, ignore_errors=True)


if __name__ == "__main__":
    main()
