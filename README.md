# Blockcraft

An original Minecraft-Java-1.12-style voxel sandbox for Windows (Java 21 + LWJGL/OpenGL).
Current source version: **V6.7 — aquatic worlds and Java-style redstone**.

Adds temperature/depth-based oceans, reefs/kelp/icebergs, water flow, swimming
and waterlogging; directional/timed circuits, comparators and slime-piston
assemblies. See [controls, compatibility limits and tests](docs/gameplay/aquatic-redstone.md).
Back up worlds before upgrading; use V6.7 on all multiplayer peers.

Retains the Building, Colors/Nature and Utility/Tools review batches, chest and
utility model fixes, and the native 32×32 PNG editing workflow. Accepted stone
bricks, planks and cobblestone remain; the bow stays a flat animated sprite, and
corrected gun poses and held-item lighting are retained. See the
[review record](docs/art-review/utility-tools-review.md) and `package/README.txt`.

## Edit textures in Aseprite

1. Run **Edit-textures.bat** in the extracted game folder. It creates
   `resourcepacks/MyTextures/` and opens a searchable picture index.
2. Edit a **32×32 PNG** in Aseprite and save/export to the same PNG filename.
3. In-game, open **Options → Resource Packs → Reload PNGs**, then select
   **My Aseprite Textures**. After later saves, just press **Reload PNGs**.

No Java/source editing or extra tools needed. The exporter **never overwrites an
existing pack**. Includes 545 individual tiles and six animation sheets, with
filenames, face/component mappings, and Aseprite import/export instructions.
A separate ready-made texture kit is also available with the release.
Read the [complete editing guide](package/Texture-editing.txt), including backups
and the difference between editable PNGs and the reference atlas.

## Download and play (the easy way)

1. Open the **[Releases page](../../releases/latest)** of this repository (right side of the repo's front page: *Releases*).
2. Download **`Blockcraft-V<version>-windows.zip`** (about 82 MB, Java is inside - nothing to install).
3. Right-click the zip -> *Extract All...* into a **new folder**, open the `Blockcraft` folder, run **`Blockcraft.exe`**.

No git, no pulling, no branches. Every update gets its own release with the complete game zip (and a smaller ~33 MB
patch). Your worlds and options are stored in `%USERPROFILE%\.blockcraft112`, so they survive updates, even when
you extract a new version into a new folder. The player manual with the full changelog is `package/README.txt`
(also inside the zip).

**Seeing older art?** Check the title says V6.7 and select **Natural Pixel 32 (Default)**
in Options > Resource Packs. Older/custom packs deliberately keep their own textures.

> Releases are created by the **Release** GitHub Action (Actions tab -> *Release* -> *Run workflow*), which packages the
> update pack committed in `releases/`. GitHub allows 2 GB per release file; plain git only allows 100 MB per file
> (25 MB for uploads through the website), so big files do not belong in the repository itself.

## Getting the source / update packs with git (optional)

* **GitHub Desktop:** *Fetch origin*, pick the branch `arena/5e64794c-blockcraft` under *Current branch*, *Pull origin*.
  The small update pack is in the `releases` folder.
* **Browser only:** open the branch on github.com, click *Code -> Download ZIP*.
* Merging the pull request into `main` puts the same files on `main`.
* `Blockcraft-1.12-UPDATE3.5-windows.zip` in the repo root is the original 3.5 build; the build uses it as the *base kit*
  (it contains the bundled Windows Java), so please leave it where it is.

## Develop

See [`docs/DEVELOPMENT.md`](docs/DEVELOPMENT.md) (build, tests, layout, how to add blocks/items/mobs, gotchas) and
[`docs/PROJECT_STATUS.md`](docs/PROJECT_STATUS.md) (features, what is verified and how, what isn't).

```
python3 tools/build.py --patch     # build the game and the small update pack  (finds/downloads a compiler by itself)
python3 tools/runtests.py          # headless tests: over 4,000,000 assertions + "unchanged vs 3.5" fingerprints
python3 tools/make_release.py      # assemble the release files (full zip + patch + notes) into build/release/
```
