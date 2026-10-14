# Blockcraft

An original Minecraft-Java-1.12-style voxel sandbox for Windows (Java 21 + LWJGL/OpenGL).
Current source version: **V6.5 — cohesive native-pixel art rework**.

The default pack uses crisp 32×32 artwork across blocks, plants, animations and item
icons. V6.5 replaces the 456 non-protected atlas cells from V6.4 while preserving the
61 reference cells, including stone bricks, planks, cobblestone and the flat animated
bow. Corrected gun poses and held-item lighting are retained. See the
[art review and verification record](docs/art-review/README.md) and the full player
manual/changelog in `package/README.txt`.

## Download and play (the easy way)

1. Open the **[Releases page](../../releases/latest)** of this repository (right side of the repo's front page: *Releases*).
2. Download **`Blockcraft-V<version>-windows.zip`** (about 82 MB, Java is inside - nothing to install).
3. Right-click the zip -> *Extract All...* into a **new folder**, open the `Blockcraft` folder, run **`Blockcraft.exe`**.

No git, no pulling, no branches. Every update gets its own release with the complete game zip (and a smaller ~33 MB
patch). Your worlds and options are stored in `%USERPROFILE%\.blockcraft112`, so they survive updates, even when
you extract a new version into a new folder. The player manual with the full changelog is `package/README.txt`
(also inside the zip).

**Seeing older art?** Check the title says V6.5 and select **Natural Pixel 32 (Default)**
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
