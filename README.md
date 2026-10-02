# Blockcraft

An original Minecraft-Java-1.12-style voxel sandbox for Windows (Java 21 + LWJGL/OpenGL). Current version: **Update 3.6**
("the farm update": wet/dry farmland, trampling, carrots, potatoes, bone meal, hay bales, shears, animal breeding).

## Play it

| You want | Download | How |
|---|---|---|
| **Update 3.6** on top of an existing install | [`releases/Blockcraft-UPDATE3.6-patch.zip`](releases) (~6 MB) | extract, copy the `Blockcraft` folder over your old one ("Replace files"), run `Blockcraft.exe` |
| A fresh install | [`Blockcraft-1.12-UPDATE3.5-windows.zip`](Blockcraft-1.12-UPDATE3.5-windows.zip) (54 MB, includes Java), then the 3.6 patch above | extract to a new folder, then apply the patch |

Your worlds and options are stored in `%USERPROFILE%\.blockcraft112` and survive updates.
The update pack contains `HOW-TO-INSTALL-THIS-UPDATE.txt`; the player manual with the full changelog is `package/README.txt`.

## Getting new versions from GitHub (no git knowledge needed)

* **GitHub Desktop:** *Fetch origin*, pick the branch `arena/01a0fdd0-blockcraft` under *Current branch*, *Pull origin*. The new zip is in the `releases` folder.
* **Browser only:** open the branch on github.com, click *Code -> Download ZIP*, or open `releases/` and click the `.zip` file -> *Download*.
* Merging the pull request into `main` puts the same files on `main`.

## Develop

See [`docs/DEVELOPMENT.md`](docs/DEVELOPMENT.md) (build, tests, layout, how to add blocks/items/mobs, gotchas) and
[`docs/PROJECT_STATUS.md`](docs/PROJECT_STATUS.md) (features, what is verified and how, what isn't).

```
python3 tools/build.py --patch     # build the game and the small update pack  (finds/downloads a compiler by itself)
python3 tools/runtests.py          # headless tests: ~3,200 game-logic checks + "unchanged vs 3.5" fingerprints
```
