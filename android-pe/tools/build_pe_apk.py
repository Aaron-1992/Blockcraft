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

WHAT'S NEW IN BLOCKCRAFT PE v{version}
--------------------------------------
* True Infinite Worlds (`PeWorld` & `PeChunkMesher`):
  - Explore endlessly in every direction! Deterministic 16x128x16 chunks stream
    seamlessly around the player at any coordinate using a lock-free O(1) 16x16
    active chunk ring buffer (`256` active chunk slots).
  - Persistent Infinite Chunk Saving (`world_0..2.bcpe` v2): every modified
    chunk across the infinite world is saved and restored at any coordinate.
  - Multi-Biome Overworld: Emerald Forests, Flower Meadows (Poppies, Dandelions,
    Tall Grass), Golden Deserts with Cacti, Snowy Taiga with Spruce Pines & Ice
    Lakes, Mountain Peaks, 3D Caves, Ore Veins & Explorer Cabins.
* Hand-Crafted Pixel-Art Texture Overhaul (`PeTextures`):
  - Ported the PC game's Update 3.18 / 3.19 `TexPaint` & `TexPaint319` painters:
    * Crisp pixel-art Oak & Spruce Leaves with 16 overlapping 4x3 / 3x4 leaf
      blade clusters, cast shadows, 4-tone sunlit leaf tips, and organic sky
      cutouts (plus interior leaf-to-leaf face culling!).
    * Multi-tone Grass Top & hanging Grass Side skirt with drop shadow over
      pebble-strewn Dirt.
    * Voronoi Cobblestone & Mossy Cobble, cracked FBM Stone, 3D-shaded Ore
      veins, grooved Bark Logs with concentric growth rings, 4-board Planks,
      Stone Bricks, Crafting Table, Furnace, Chest, Bookshelf, and bevelled
      Tool/Item sprites.
* High-Performance 120Hz VBO Engine (`PeChunkMesher`):
  - O(1) empty sky section skipping via per-section non-air block counters.
  - Direct column bit-shift indexing `((lz << 11) | (lx << 7) | wy)` with
    pre-fetched cardinal neighbor chunks.
  - Interior leaf-to-leaf face culling and configurable Render Distance
    (`9x9`, `11x11`, or `13x13` Chunks) in Settings.
  - Complete removal of AI bots and mobs for lag-free single-player performance.
* Redesigned Mobile-Friendly UI (`PeGLSurfaceView` & `PeTouchController`):
  - Dark-glass cards with double borders, top specular highlights & recessed
    item slots.
  - Circular Virtual Analog Joystick ring + glowing thumb puck (or Classic
    5-Button D-Pad), circular right-thumb action arc (`[MINE]`, `[PLACE]`,
    `[JUMP]`) with mini item icons, top quick-action pills (`[|| PAUSE]`,
    `[BAG / CRAFT]`, `[FLY]`), and target block tooltip pill under the crosshair.

HOW TO INSTALL ON SAMSUNG GALAXY A55 5G
---------------------------------------
1. Download `{apk_name}` directly to your Samsung Galaxy A55 5G.
2. Tap `{apk_name}` in Samsung "My Files" or your browser Downloads and tap
   Install / Update.
3. Launch "Blockcraft PE" in landscape mode!
"""


def build_release_notes(version, apk_name, bundle_name):
    return f"""## Blockcraft PE (Pocket Edition) v{version} — Fixed Face Culling & Orientation, Real Animated Falling Trees, 3D Tools, 36-Slot Backpack & Polish Overhaul (Samsung Galaxy A55 5G)

> **Separate Pocket Edition Release**: This release contains **only** the Android Pocket Edition (`com.blockcraft.pe`) and does **not** include any PC files. PC (`v*`) and PE (`pe-v*`) releases are kept strictly separated.

### Download & Install on Samsung Galaxy A55 5G (Android 14 / One UI 6)
1. Download **`{apk_name}`** below under **Assets** directly on your phone (or download `{bundle_name}`).
2. Open **`{apk_name}`** from your browser downloads or Samsung **My Files** and tap **Install / Update**.

### What's New in Blockcraft PE `pe-v{version}`
* **Fixed Face Culling & Side-Face Texture Orientation (`PeChunkMesher` & `PeWorld`)**:
  * **Fixed Counter-Clockwise (`GL_CCW`) Winding & Upright UV Mapping on All 6 Faces**: Re-engineered the North (`-Z`), South (`+Z`), East (`+X`), and West (`-X`) quad vertex order across `meshSection`, `emitCustomBox`, and `addDynamicBox` to `(Top-Left, Bottom-Left, Bottom-Right, Top-Right)` when viewed from outside each face. Every outer face of terrain blocks, slabs, stairs, fences, doors, torches, and chests now winds strictly `GL_CCW` (zero missing/inside-out faces under `glCullFace(GL_BACK)`) AND renders every side texture (`Grass Side`, `Furnace Front`, `Crafting Table Side`, `Chest Front`, `Oak/Spruce/Birch Bark`, `Pumpkin/Jack o'Lantern Face`, `Bookshelf`) right-side-up (`+Y` at top, `-Y` at bottom) without any 90° rotation or mirroring!
  * **Fixed `shouldRenderFace` & `isOpaque` Culling Rules**:
    * Fixed `B_FARMLAND` (`0.9375` height) being marked `isOpaque`, which previously caused adjacent dirt/grass blocks to cull their lower face strip and leave a visible seam.
    * Fixed leaf-to-leaf internal face culling (`face == 1, 3, 5`) making tree canopies look hollow when viewed from `-Y`, `+Z`, or `-X` with `GL_CULL_FACE` enabled.
    * Fixed `shouldRenderFace` so top faces of `0.5`-high Slabs, Beds, `0.9375`-high Farmland, and `0.75`-high Enchanting Tables are never culled by solid blocks above them, and subsurface water/lava uses `topY = 1.0F` when covered by water/lava above so underwater pockets never show air gaps.
    * Widened horizontal chunk frustum culling (`distSq > 1024.0F`, `dot < -0.68F`) so tall trees, watchtowers, and hills at the top/bottom edges of wide-FOV screens never pop out when pitching the camera up or down.
* **Trees Actually Fall Over in 3D (`PeWorld.FallingTree` & `PeChunkMesher.addFallingTrees3D`)**:
  * Ported PC Update 3.10's (`src/bc/Trees.java`) animated 3D tree-felling system! Chopping a natural tree trunk (`B_LOG_OAK`, `B_LOG_SPRUCE`, `B_LOG_BIRCH`, `B_LOG_JUNGLE`) detaches the connected trunk and leaf canopy (`up to 160 blocks`), hinges on the cut base block, and smoothly tips over **0° → 90°** (`cubic ease-in` gravity acceleration at 120Hz) away from the player before crashing onto the ground, placing fallen trunk logs & leaves along the terrain and dropping harvested wood!
* **Real 3D First-Person Tools, Weapons, Torches & Isometric 3D Inventory Block Icons (`PeChunkMesher.addFirstPersonHeld3D` & `addItemOrBlockIcon2D`)**:
  * **First-Person 3D Tools & Weapons**: Replaced the flat cube placeholder in the bottom-right hand with multi-part 3D voxel-extruded models for **Pickaxes** (3D diagonal oak handle + metallic collar + curved twin-tip pick head), **Swords** (pommel + grip + wide crossguard + stepped 3D blade), **Axes** (handle + bearded 3D axe bit + poll), **Shovels** (handle + wide 3D scoop blade), **Bows** (curved limbs + taut bowstring), **Torches** (3D stick + glowing ember head), **6-Face 3D Blocks / Slabs / Stairs**, and a **3D First-Person Player Arm** when empty-handed, complete with 120Hz walk bobbing and forward-and-down mining/chopping swing arcs!
  * **3-Face Isometric 3D Block Icons in Hotbar & Inventory**: All 3D blocks in the Hotbar and Inventory now render as 3-face isometric 3D cubes/slabs (`Top + Front-Left + Right` shaded faces with true slab heights), while tools, weapons, food, ingots, doors, ladders, and plants render as crisp pixel-art sprites.
* **Upgraded 4-Tab Inventory, 27-Slot Player Backpack (`36` Total Slots) & Fixed Gameplay Interactions (`PeWorld`, `PeGLSurfaceView` & `PeTouchController`)**:
  * **36-Slot Player Inventory (`9` Hotbar + `27` Backpack Slots, persisted in `world_0..2.bcpe` `"BAG1"`)**: Fixed `giveItem` overwriting the player's currently held tool when all 9 hotbar slots were occupied! Picked-up blocks and crafted items now stack across all 36 slots and overflow cleanly into the **27-slot Player Backpack** (`Tab 0: Backpack & Craft`).
  * **Interactive Slot Swapping & Item Management**: Tap any slot in your `9x3` Backpack grid and tap any Hotbar slot (or tap `[TO HOTBAR]` / double-tap the slot) to swap or stack items between your Backpack and Hotbar, or tap `[DROP / CLEAR]` to discard unwanted items. In Survival mode, tapping any owned item in the Building/Nature/Tools tabs automatically equips it from your Backpack into your active Hotbar slot!
  * **Fixed All Wood Types in Crafting & Fixed World Interactions**:
    * Crafting recipes requiring Wood Logs (`Oak Planks x4`, `Charcoal x2`) now accept **Oak, Spruce, Birch, or Jungle Logs**, and recipes requiring Planks (`Crafting Table`, `Sticks`, `Chest`, `Door`, `Bed`, `Fences`, `Slabs`, `Stairs`, `Wooden Tools`) accept **any Plank type** across all 36 Hotbar + Backpack slots.
    * Breaking a block underneath a **Sand** or **Gravel** column now immediately settles the entire column above it downward.
    * Tapping the upper or lower half of an **Oak Door** toggles or mines the door cleanly.
    * Tapping `[PLACE]` on a placed **Crafting Table**, **Furnace**, or **Chest** opens the Inventory & Crafting screen; tapping `[PLACE]` with an **Ender Pearl** teleports you forward along your look ray; and tapping `[PLACE]` with a **Bow** fires a ranged arrow strike!
"""


def main():
    version = sys.argv[1] if len(sys.argv) > 1 else "1.4"
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
