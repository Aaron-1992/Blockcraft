#!/usr/bin/env python3
"""
Builds the standalone Android Pocket Edition APK for Blockcraft PE (com.blockcraft.pe),
optimized for the Samsung Galaxy A55 5G (Exynos 1480 / Xclipse 530 GPU / 120Hz 1080x2340 / Android 14 API 34).

Outputs into build/release-pe/ (strictly separated from PC releases):
    Blockcraft-PE-v<ver>-Samsung-A55-5G.apk
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
  BLOCKCRAFT PE (POCKET EDITION) v{version} — ANDROID (SAMSUNG GALAXY A55 5G)
========================================================================

Note: This is the standalone Android Pocket Edition release of Blockcraft.
PC and PE releases are strictly separated — this release contains ONLY the
Android Pocket Edition build and no PC files.

TARGET DEVICE & HARDWARE OPTIMIZATIONS
--------------------------------------
* Samsung Galaxy A55 5G (SM-A556B / SM-A556E, Android 14 / One UI 6, API 26-34)
* Exynos 1480 Octa-Core + Xclipse 530 GPU (RDNA2 mobile architecture)
* 6.6" 1080x2340 (19.5:9) Super AMOLED @ 120Hz High Refresh Rate
* Native OpenGL ES 3.0 / 2.0 Hardware VBO Chunk Renderer:
  - 16x16x16 chunk section Vertex Buffer Objects (GL_STATIC_DRAW) with
    hidden-face culling, directional sunlight + torch lighting, and view-cone
    frustum culling for locked 120 FPS gameplay on mobile GPUs.
* Multi-Touch Pocket Edition Controls:
  - Left Classic PE 5-Button D-Pad OR Virtual Analog Thumbstick (toggleable in
    the top bar via [CTRL: D-PAD / STICK]).
  - Right-side Touch-Look + Quick-Tap Attack/Place + Hold-to-Mine progress circle,
    plus dedicated [JUMP/FLY], [ATK], and [USE] thumb buttons.
  - Bottom 9-slot Touch Hotbar + [...] 28-item Block & Weapon Palette Drawer.
  - Hole-punch camera cutout safe-area margins for landscape play.

8 INCLUDED GAMEMODES & 24 ARENA MAPS
------------------------------------
1. Survival World   — Procedural Overworld biomes, trees, ores, village cabin & mobs
2. Creative World   — Infinite blocks, double-tap JUMP to toggle 3D Creative Flight
3. Battle Brawl     — Authentic 160x160x96 Console Edition .mca arenas (Cove, Crucible,
                      Cavern, Temple, Lair, Castle, Frontier, Shrunk) with outer-map
                      AI waypoint dispersion & randomized loot
4. Tumble Spleef    — 3-layer multi-material spleef arena with Snowballs & Tumble Shovel
5. Bedwars 4-Team   — Spawn islands, Diamond/Emerald generators, beds & dynamic AI teams
6. CS2 Tactical     — 8 dedicated 3-lane maps (Dust II, Mirage, Inferno, Nuke, Overpass,
                      Train, Vertigo, Anubis) with Bomb Sites A & B, SMGs, Pistols & AWPs
7. End Bed Fight    — Obsidian pillars, Ender Pearls & zero-self-damage Bed-Bombing
8. Castle Siege     — 8 dedicated fortresses (Ironkeep, Helm's Deep, Dragonstone,
                      Frosthold, Sandstorm, Blackrock, Skyhaven, Dreadfort) with
                      Siege Cannons, Mortars, destructible gates & Control Zones A/B

HOW TO INSTALL ON SAMSUNG GALAXY A55 5G
---------------------------------------
1. Download `{apk_name}` directly to your Samsung Galaxy A55 5G
   (or copy it over USB from this zip).
2. Tap `{apk_name}` in Samsung "My Files" or your browser Downloads.
3. If prompted by Android 14 ("Install unknown apps"), tap Settings -> Allow
   from this source, then tap Install.
4. If Google Play Protect shows "Unrecognized app developer" (because this is
   an indie self-signed APK), tap "More details" -> "Install anyway".
5. Launch "Blockcraft PE" in landscape mode!
"""


def build_release_notes(version, apk_name, bundle_name):
    return f"""## Blockcraft PE (Pocket Edition) v{version} — Android (Samsung Galaxy A55 5G)

> **Separate Pocket Edition Release**: This release contains **only** the Android Pocket Edition (`com.blockcraft.pe`) and does **not** include any PC files. PC (`v*`) and PE (`pe-v*`) releases are kept strictly separated.

### Download & Install on Samsung Galaxy A55 5G (Android 14 / One UI 6)
1. Download **`{apk_name}`** below under **Assets** directly on your phone (or download `{bundle_name}` which includes the APK + installation guide).
2. Open **`{apk_name}`** from your browser downloads or Samsung **My Files** and tap **Install**.
3. If prompted to allow installation from your browser/My Files, toggle **Allow from this source**; if Play Protect prompts about an indie self-signed APK, tap **More details -> Install anyway**.

### Engineered for Samsung Galaxy A55 5G (`1080x2340` Super AMOLED @ `120Hz`)
* **Rebuilt Mobile OpenGL ES 3.0 / 2.0 Hardware VBO Renderer**:
  * Replaces desktop immediate-mode rendering with `16x16x16` hardware **Vertex Buffer Objects (`GL_STATIC_DRAW`)**, 6-face hidden-surface culling, per-face sunlight/torch shading, distance fog shaders (`PeShaders`), and 3D view-cone frustum culling for smooth **120Hz** rendering on the **Xclipse 530 GPU** (Exynos 1480).
  * Native `120Hz` display mode negotiation (`preferredDisplayModeId` / `preferredRefreshRate = 120.0f`) and edge-to-edge landscape safe-area margins around the Galaxy A55 5G punch-hole camera.
* **Multi-Touch Pocket Edition Controls (`PeTouchController`)**:
  * **Left Movement Pad**: Switch anytime between the classic **5-Button PE D-Pad** (with center sneak/descend) and a **Virtual Analog Thumbstick** via `[CTRL: D-PAD / STICK]`.
  * **Right Touch-Look & Combat Buttons**: Simultaneous multi-finger camera look, hold-to-mine touch progress circle, tactical crosshair with animated hitmarkers, and dedicated **`[JUMP/FLY]`**, **`[ATK]`**, and **`[USE]`** buttons.
  * **9-Slot Touch Hotbar + `[...]` Palette Drawer**: Instant tap-to-equip for 28 blocks, tools, swords, bows, CS2 firearms, and siege weapons.
* **All 8 Gamemodes & Dynamic AI Bots Included**:
  * **Survival World** & **Creative World** (double-tap `JUMP` to fly)
  * **Battle Brawl** (loads all 8 authentic `160x160x96` Console Edition `.mca` arenas from `console_arenas.bin` with outer-map AI roaming & dynamic combat)
  * **Tumble Spleef**, **Bedwars 4-Team**, **CS2 Tactical** (8 maps), **End Bed Fight** (zero-self-damage bed-bombing), and **Castle Siege** (8 fortresses with cannons & destructible gates)
"""


def main():
    version = sys.argv[1] if len(sys.argv) > 1 else "1.0"
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
        icon_path = os.path.join(res_dir, "drawable-nodpi", "ic_launcher.png")
        write_launcher_icon_png(icon_path, 192)

        manifest_path = os.path.join(work, "AndroidManifest.xml")
        update_manifest_icon(os.path.join(PE_DIR, "AndroidManifest.xml"), manifest_path)

        compiled_res_zip = os.path.join(work, "compiled_res.zip")
        run_cmd([aapt2, "compile", "--dir", res_dir, "-o", compiled_res_zip])

        unaligned_apk = os.path.join(work, "unaligned.apk")
        gen_r_dir = os.path.join(work, "gen")
        os.makedirs(gen_r_dir, exist_ok=True)
        run_cmd([
            aapt2, "link",
            "-I", android_jar,
            "--manifest", manifest_path,
            "--java", gen_r_dir,
            "--min-sdk-version", "26",
            "--target-sdk-version", "34",
            "--version-code", "100",
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

        # Add classes.dex and game assets (font.bin + maps/console_arenas.bin) into unaligned_apk
        with zipfile.ZipFile(unaligned_apk, "a") as zf:
            zf.write(os.path.join(dex_dir, "classes.dex"), "classes.dex", compress_type=zipfile.ZIP_DEFLATED)
            font_bin = os.path.join(ROOT, "res", "font.bin")
            if os.path.exists(font_bin):
                zf.write(font_bin, "assets/font.bin", compress_type=zipfile.ZIP_STORED)
            arenas_bin = os.path.join(ROOT, "res", "maps", "console_arenas.bin")
            if os.path.exists(arenas_bin):
                zf.write(arenas_bin, "assets/maps/console_arenas.bin", compress_type=zipfile.ZIP_STORED)

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

        apk_name = f"Blockcraft-PE-v{version}-Samsung-A55-5G.apk"
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
