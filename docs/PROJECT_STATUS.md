# Blockcraft – project status

_Handoff notes, 2026-10-02. The project was started in an earlier chat and moved to this repo. The feature list below is copied from that chat's "Update 3.5" release message and matches the `README.txt` inside the 3.5 zip. The original source did not survive, so it was **recovered from the shipped jar** (see [`DEVELOPMENT.md`](DEVELOPMENT.md)) and now lives in `src/`; the recovered code was verified to behave identically to the 3.5 jar for all non-graphics logic._

## What it is

Blockcraft is a Minecraft-Java-1.12-style block sandbox for Windows, described as "an original clone". It was shipped as `Blockcraft-1.12-UPDATE3.5-windows.zip`: extract it to a fresh folder and run `Blockcraft.exe`. The title screen shows "Update 3.5".

## Update 3.5 – what it contains

### Farming
- Hoes come in 5 tiers. Right-click grass or dirt to make farmland.
- Seeds drop from tall grass, about 1 in 8. Plant them on farmland.
- Wheat has 8 growth stages and only grows near the player. Ripe wheat drops wheat and 0–3 seeds. 3 wheat make bread.

### Ladders
- 3 ladders from 7 sticks.
- Place them on the side of a block. Walk into one or hold jump to climb. Sneak to hold still.
- No fall damage while on a ladder.

### Buckets
- 3 iron ingots make one bucket.
- It picks up water or lava sources and pours them back out.

### TNT
- Recipe: 5 gunpowder + 4 sand. Light it with Flint and Steel (iron ingot + flint).
- 4-second fuse, flashes, falls if unsupported.
- Blast radius 4. Hurts the player and mobs. Chain-ignites other TNT. Creeper blasts also ignite TNT.

### Beds
- 3 wool over 3 planks.
- Right-click at night to sleep until morning, unless monsters are within 8 blocks.
- Right-click at any time to set the respawn point. If the bed is destroyed, the player respawns at the world spawn.

All the new items are on page 2 of the creative inventory.

## Verification status (as reported by the previous chat)

Checked with screenshots: wheat, farmland, TNT and bed textures/models; the new item icons in the inventory; a chain explosion.

**Never run in the game** (controls untested):
- right-clicks with the hoe, bucket, seeds and flint & steel
- sleeping in a bed
- climbing a ladder

The crafting recipes were not checked in-game either.

## Known gaps
- Farmland hydration is not implemented.
- Crop trampling is not implemented.

## Previous build/test setup
Built and tested on Linux with software rendering. **Not** tested on real Windows hardware.

## Open items
- [x] Upload the 3.5 zip to this repo (done by the owner).
- [x] Identify the tech stack: Java 21 + LWJGL 3; `Blockcraft.exe` is a Launch4j wrapper around the jar.
- [x] Recover the source from `blockcraft.jar` and make it build again (`src/`, `tools/build.py`).
- [x] Prove the recovered logic code equals the 3.5 jar (`python3 tools/test.py equivalence`).
- [ ] Decide how Windows builds are delivered to the owner (update packs in `releases/`; see README).
- [ ] Play-test the three untested features above and the crafting recipes (headless unit tests cover the logic).

## Moving files around (limits observed 2026-10-02)
- GitHub website upload: max 25 MiB per file, 100 files at a time. Git / GitHub Desktop: max 100 MiB per file (warning above 50 MiB). Anything larger needs Git LFS.
- The assistant's sandbox in that session could reach github.com, api.github.com, npm and PyPI. It could **not** reach GitHub Releases downloads/uploads or common file-sharing sites, had no Windows cross-compiler, and had no display server or browser for running the game. Large binaries therefore can't be routed through the assistant. Options to evaluate: build the Windows zip on GitHub (GitHub Actions, Windows runner) or on the owner's PC.
