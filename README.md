# Blockcraft

An original Minecraft-Java-1.12-style voxel sandbox for Windows (Java 21 + LWJGL/OpenGL). Current version: **Update 3.7**
("the farm update": wet/dry farmland, trampling, carrots, potatoes, bone meal, hay bales, shears, animal breeding).

## Download and play (the easy way)

1. Open the **[Releases page](../../releases/latest)** of this repository (right side of the repo's front page: *Releases*).
2. Download **`Blockcraft-1.12-UPDATE<version>-windows.zip`** (about 54 MB, Java is inside - nothing to install).
3. Right-click the zip -> *Extract All...* into a **new folder**, open the `Blockcraft` folder, run **`Blockcraft.exe`**.

No git, no pulling, no branches. Every update gets its own release with the complete game zip (and a small ~6 MB
update pack). Your worlds and options are stored in `%USERPROFILE%\.blockcraft112`, so they survive updates, even when
you extract a new version into a new folder. The player manual with the full changelog is `package/README.txt`
(also inside the zip).

> Releases are created by the **Release** GitHub Action (Actions tab -> *Release* -> *Run workflow*), which packages the
> update pack committed in `releases/`. GitHub allows 2 GB per release file; plain git only allows 100 MB per file
> (25 MB for uploads through the website), so big files do not belong in the repository itself.

## Getting the source / update packs with git (optional)

* **GitHub Desktop:** *Fetch origin*, pick the branch `arena/01a0fdd0-blockcraft` under *Current branch*, *Pull origin*.
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
python3 tools/runtests.py          # headless tests: ~11,000 game-logic checks + "unchanged vs 3.5" fingerprints
python3 tools/make_release.py      # assemble the release files (full zip + patch + notes) into build/release/
```
