# Blockcraft

An original Minecraft-Java-1.12-style voxel sandbox for Windows (Java 21 + LWJGL/OpenGL).
In Creative, type `/computer` to build the physical 4-bit example machine. Right-click its orange clock to Run/Stop; empty-hand sneak/right-click to Step. [Computer guide](package/Computer-guide.txt).

Current source version: **V6.38 — classic Screens UI restored and polished, with four seasons, player air temperature / wind chill, and a spatial local-wind system**, ported onto V6.37 while preserving its deep-world rendering and cloud-height fixes. The Create New World live map preview and numeric settings remain. [V6.38 release notes](docs/releases/v6.38.md). This is an original voxel sandbox; no Mojang code or assets are included.

V6.38 routes the title, world list, world creation, options and pause flow through the classic `Screens.java` UI instead of the later rejected menu redraws. New worlds can enable seasons and select wind strength; in-world Gameplay settings control season length and wind, while the HUD reports season, ambient temperature, feels-like temperature and local wind. Wind varies with position and time, steers weather drift and gently affects exposed players; seasonal temperature, precipitation and crop growth use the saved world clock. V6.37's lifted clouds/weather and 24-section deep-world rendering are retained. No real-GPU/Windows playtest is claimed.

**V6.36 threw the V6.35 "Atlas" UI away.** Every menu was an ordinary centred panel with a plain title, and settings printed every value.

**V6.36 (superseded).** The user said *"Dont like ui at all also settings dont even show numbers and half the stuff does not make any sense"*. Every menu is now an ordinary centred panel with a plain title, a list of clearly labelled buttons and an obvious Back button — no nav rail, no numbered cards, no duplicated card titles, no clipped abbreviations, no decorative kickers and no nonsense footer. Settings now **print every value**: stepped settings show the number and its unit (`Render Distance: 8 chunks`, `Max FPS: 0 fps (0 = unlimited)`, `World Depth: 384 blocks`), sliders print the number in gold on the right *and* inside the bar (`70 degrees`), toggles say ON/OFF, and choices spell out the current option (`Minimap Corner: Top right`). Mini-game tabs are full words (Battle, Tumble, Bedwars, CS2, Murder, Ender, Siege) instead of "Brawl/Tumbl/Beds/Murdr". Debug Advanced still exposes all 61 video, shader, LOD and performance settings. The **live world-map preview on Create New World is kept** — same `WorldPreview` widget, plain frame, and it shows the biome count as a number. See [V6.36 notes](docs/releases/v6.36.md) and the headless board `docs/screenshots/v6.36-menus.png` (software render, not a GPU screenshot).

**V6.35 makes worlds so deep you can get completely lost.** The world is now **384 blocks tall** (24 sections) instead of 256, and new worlds lift the whole terrain and sea level by 128 blocks — sea level goes from 63 to **191**, so there are ~190 blocks of underground below it instead of ~62. Mountains reach above y=330, so the new upper half is really used. This required a taller chunk format (the old 0..255 part of every save is byte-for-byte compatible; y=256..383 lives in an ignored trailer), a 9-bit Y in the light queue, and 24 meshing sections. Old saves load with `depth = 0` and never move. See [V6.35 notes](docs/releases/v6.35.md) and the headless cross-section `docs/screenshots/v6.35-caves.png`.

**Caves get crazier the deeper you go.** `caveVer = 8` keeps V6.34's single continuous 3D cave volume (no apertures, funnels or drilled shafts) but adds a **depth gradient**: sheared worm/noodle tunnels widen sharply, cheese-cavern gates drop, cathedral-sized "mega" chambers open below ~y=100, a second cross-sheared maze appears in the abyss, giant halls get bigger and more frequent, and lava seas plus crystal/glowstone fields light the bottom. Headless cross-section: **47.2% carved in the abyss (y 6–40), 30.4% deep (40–110), 6.3% middle (110–170), 4.0% in the daylight layer** — the surface still stays intact. Twelve underground structures (the original six plus Crystal Cathedral, Fossil Hall, Lava Forge, Lost Library, Abyssal Maze and Sunken Vault) are carved by the *same* air function, get physically bigger with depth, and get extra abyss-only sites.

**ALL menus rebuilt from scratch: the Atlas UI.** `Menus635` replaces every screen the game draws — main menu, singleplayer/multiplayer hubs, Create New World, Create Multiplayer World, the world list, multiplayer connection, mini games, settings (seven new tabs plus Debug Advanced), pause, more, packs, mods, credits, loading and the lobby — with a dark navigation rail, numbered cards, a cyan rule and a common footer. The **live world-map preview on Create New World is kept** (the user asked to keep the map, not the old menu around it); it now previews the actual depth of the world being created. All 61 advanced video settings stay available behind Debug Video. The old `Screens.*` menus are no longer drawn but remain for regression tests. Headless board: `docs/screenshots/v6.35-menus.png`. Every new screen is validated by the headless layout suite at seven window sizes. No Mojang code or assets.

**V6.34 removes the drilled holes.** The V6.32 carver kept a solid crust and then bored an aperture shaft straight down until it hit a cave. `caveVer = 7` (new worlds only) is a completely new, original implementation of Minecraft 1.18/1.20-style generation: **one continuous 3D cave volume** (ridged spaghetti/noodle worm tunnels sheared in Y, clustered cheese caverns, rare giant halls, local aquifers and lava) carved in **absolute world Y** from bedrock to the grass. There is no entrance object, no aperture, no funnel and no shaft: a mouth is simply where that volume intersects the terrain, so the walls are sloped and ragged and every opening is different. Headless 96×96 land sample: **9.0% carved overall, 4.4% in the top 20 blocks, 121 open columns (1.3% of the sample), 1 single-block vertical shaft, 84 openings continuing into the network** — the ground stays intact. Ravine cracks are dialled almost completely out. No Mojang code or assets. See [V6.34 notes](docs/releases/v6.34.md) and the headless before/after preview `docs/screenshots/v6.34-caves.png`. ⚠️ Create a **new world** — old saves keep their cave version.

**Every underground structure is now part of the cave system.** `CaveStructures7` returns its mineshaft corridors, dungeon rooms, stronghold halls, geode interiors, ancient-city plazas and ruined-portal areas from the **same `air()` function** the carver calls for each block, so cheese caverns and worm tunnels cut straight through them instead of being attached by a short link tunnel. Floors, walls, supports and loot are stamped only where the neighbouring rock is solid, so a cave that passes through a wall stays open. The legacy sealed dungeon/mineshaft/stronghold generators are disabled for new worlds, and village sites now reject ground that contains a cave mouth. All six structure types were verified to build blocks and to have natural cave air inside their own carved space.

**V6.32 fixes the "world basically non-existent" problem.** V6.31's carver hollowed out 19% of the top 20 blocks and opened 10% of all surface columns. `caveVer = 6` (new worlds only) is a completely new original carver matching Minecraft 1.18/1.20 density: an intact 12-block surface crust, thin spaghetti/noodle worm tunnels, sparse clustered cheese chambers, local aquifers, and only rare noise-shaped entrances that always connect to a real cave. Headless sample: **5.0% carved overall, 1.2% near the surface, 0.4% surface openings, 40/40 entrances connected** (V6.31: 16.1% / 19.3% / 10%). The structures are completely new too: mineshafts, dungeons, strongholds, amethyst geodes, ancient cities and ruined portals — small, deterministic and linked into the caves. No Mojang code or assets. See [V6.32 notes](docs/releases/v6.32.md) and the headless preview `docs/screenshots/v6.32-caves.png`. ⚠️ Create a **new world** — old saves keep their cave version.

**V6.33 redoes every menu.** The main screen is exactly three choices: **Singleplayer**, **Multiplayer** and **Settings** (Trailer and Quit are small footer buttons). Singleplayer opens a hub where you choose **Open World** or **Mini Games**; Multiplayer opens the same hub plus **Create & Host World**, which is a full **Create New World**-style screen with a real world name, seed, player name, game mode, world type, structures and a live biome preview. The **Mini Games** hub has a proper card look with a highlighted selected arena. **Settings** now has seven focused tabs (Video, Graphics, Gameplay, Controls, Sound, World, Content) drawn in two readable rows, plus the Debug Advanced tab — so world-gen, audio and content settings no longer have to be hunted for. See [V6.33 notes](docs/releases/v6.33.md) and the headless preview `docs/screenshots/v6.33-menus.png`.

V6.31 fixes the remaining LOD popping at its root: tile uploads, rebuilds and LOD split transitions now **cross-fade** (the old mesh stays fully opaque while the new one blends in) with a continuous 28% split band instead of a one-frame switch. The trailer is now a **24-shot montage** with eight camera moves and eight locations, not one long flythrough. **Borderless fullscreen** (`/borderless`, Options > Video) and a cleaner five-button title screen with a **More...** screen are in. **Caves are completely redone** for new worlds (`caveVer = 5`): there are no built cave mouths any more — the cave noise itself breaks through the ground in irregular, differently shaped openings — and the underground structures are eight new builds linked into the cave network. Old saves keep their old cave version. See [V6.31 notes](docs/releases/v6.31.md), the [montage plan](docs/releases/v6.31-trailer-plan.txt) and the headless previews `docs/screenshots/v6.31-caves.png` / `v6.31-trailer.png`.

**V6.30 added the in-game trailer (since reworked into a montage).** Press **Watch Trailer (1:00)** on the title screen or type **`/trailer`** while playing: eight scripted camera moves fly through a freshly generated world with 2.39:1 letterboxing, hard cuts, title cards, a dawn-to-night-to-dawn time-lapse, a forced thunderstorm, a four-act soundtrack and foley stings from the real CC0 WAVs. It runs for exactly 60 seconds, is skippable with any key or click, and restores the exact screen, camera, weather, daylight and options afterwards. No Mojang assets; `Trailer.java` is original and the shot table is deterministic (`java -jar blockcraft.jar --trailer-plan`). See [V6.30 notes](docs/releases/v6.30.md), the [shot plan](docs/releases/v6.30-trailer-plan.txt) and the headless storyboard `docs/screenshots/v6.30-trailer.png` (not a GPU screenshot).

V6.29 replaces V6.28's long diagonal entrance cracks with wide, irregular cave mouths you can actually walk into: a sloped stone floor from the grass down into a chamber, a rock arch over the deep end, and a tunnel that joins a real carved cave. The eight V6.28 procedural tones are gone; tree falls/crashes/creaks, leaf rustle, wind gusts, cave drips, cavern air, deep rumble, crystal chimes and rockfall are now **real CC0 field/foley recordings** shipped as editable WAVs in `sounds/foley/` (full provenance in `res/sounds/foley/CREDITS.txt`). See [V6.29 notes](docs/releases/v6.29.md) and the headless generated cross-section `docs/screenshots/v6.29-caves.png`. V6.28 worlds keep their old cave version; nothing in an existing save is rewritten.

Chunk lighting runs on a bounded, low-priority worker; unload and distant-terrain scans are incremental, and per-section occupancy is indexed. V6.18 continues that work with background chunk saves, incremental autosaves, bounded unloads, a configurable chunk cache and reused collision scratch buffers. No measured frame-time gain on real GPU hardware is claimed yet; please report machine specs, render distance and whether chunk-load spikes improved.

Native mechanical parts now include shafts/cogs, a gearbox, motor/water-wheel
sources, redstone clutch/gearshift, gauges, and visible belt/depot pressing and hand-fed millstone processing.
Dedicated component art replaces stretched block skins, and mechanisms animate
every render frame. Water-wheel direction follows real currents, not a toggle.
Search **create** in Creative, or use **/kinetics** for an inspectable example factory.
**This is not full Create:** inclined belts, funnels, contraptions, trains, logistics and other major
systems are unfinished. [Exact status and roadmap](docs/create/STATUS.md) ·
[Foundation guide](package/Create-foundation.txt).

Right-click a **MIDI Build Compiler** in Creative, select a MIDI and click
**Play full** to hear the whole file with note-block sounds, without building a
track or flying alongside it. **Play section** uses Start/Length; **Stop** ends
listening. Audio is local and stops when closing the menu. **Build circuit**
still makes independent physical redstone; its default section is five seconds.
[Download V6.16](https://github.com/Aaron-1992/Blockcraft/releases/tag/v6.16) · [MIDI guide](package/MIDI-guide.txt).

Adds temperature/depth-based oceans, reefs/kelp/icebergs, water flow, swimming
and waterlogging; directional/timed circuits, comparators and slime-piston
assemblies. See [controls, compatibility limits and tests](docs/gameplay/aquatic-redstone.md).
Back up worlds before upgrading; use V6.16 on all multiplayer peers.

Retains the Building, Colors/Nature and Utility/Tools review batches, chest and
utility model fixes, and the native 32×32 PNG editing workflow. Accepted stone
bricks, planks and cobblestone remain; the bow stays a flat animated sprite, and
corrected gun poses and held-item lighting are retained. See the
[review record](docs/art-review/utility-tools-review.md) and `package/README.txt`.

Update 6.23 gives **every tree species** several silhouettes and three size tiers: 8 species and 34 shapes (lobed, columnar, umbrella, forked and weeping oaks; clump, weeping and grand birches; tiered, pine, columnar, 2x2 giant, grand and candle spruces; layered, buttressed and emergent jungle trees; bent, double-bend, wide and forked acacias; grand and spreading dark oaks; cypress and willow swamps; two bushes). Giant mushrooms and ice spikes scale too. Variants and Normal/Large/Giant tiers come from the seed-derived tree hash, so chunk seams still agree; a hard 900-leaf budget and taller-but-hollow giant conifers keep the V6.22 taiga fix intact. **Tree Size** (0.7x-1.5x) and **Giant Trees** are in Debug Video > Advanced, persist in options.txt and are saved with the world. See [V6.23 notes](docs/releases/v6.23.md) and the headless geometry previews `docs/screenshots/v6.23-tree-variants.png`, `docs/screenshots/v6.23-tree-sizes.png`.

Update 6.22 replaces the V6.21 carver again for new worlds with an original Caves & Cliffs-style engine (`CaveCliffs`): noodle/spaghetti/cheese caves, giant halls and wells, hashed local aquifers, warped lush/dripstone/ice/amethyst/deep-dark cave biomes, amethyst geodes and underground flora. World generation gains climate-warped, blended biome borders, mountain terraces, broader river valleys, gentler beaches and a broken snow line. Spruce crowns are rebuilt as sparse tiered conifers with a per-chunk tree cap and a Tree Density slider, so taiga is no longer the heaviest biome to mesh. Mob shadows now re-stamp every frame from a terrain-only depth copy instead of lagging behind the shadow-map refresh. The HUD minimap has four corners and slides down when an advancement toast appears. All new knobs are in Debug Video > Advanced and persist; old saves keep their exact old terrain. See [V6.22 notes](docs/releases/v6.22.md) and the headless generator previews `docs/screenshots/v6.22-caves.png`, `docs/screenshots/v6.22-borders.png`.

Update 6.21 completely redoes the caves for new worlds and makes all weather prettier, in the distance too. The old swiss-cheese carving is replaced by a layered cave engine (`CaveWorks`): winding tunnels, sparse chambers, lava provinces, real horizontal rock strata, sinkholes and cave decorations. Six deterministic underground site types add loot chests, lights, corridors, pillars and bridges; sites are placed below the true surface so they never float in an ocean. Rain and snow curtains now reach toward the horizon, lean with the wind, brighten near storms and lightning, and get soft drifting ground mist; clearing rain under a low sun draws a seven-band rainbow, and the cloud shader gains a stronger silver lining, warm sunset rim and ice halo. All cave/weather controls are in Debug Video > Advanced. See [V6.21 notes](docs/releases/v6.21.md) and the headless generator/weather previews `docs/screenshots/v6.21-caves.png`, `docs/screenshots/v6.21-weather.png`.

Update 6.20 adds a live **minimap** in the HUD corner and a **big world map** on the **M** key: pan with WASD/arrows or a click, zoom with the wheel or +/-, and see mobs, coordinates and biomes. Size, radius, rotation, refresh budget, zoom and grid are configurable under Options (and in Debug Video > Advanced). The title panorama was fixed: its fade-in no longer waits forever for hidden cave meshes, and a generated 2D landscape is always drawn while the 3D world loads. See [V6.20 notes](docs/releases/v6.20.md).

Update 6.19 makes the moon cast real shadows, gives mobs shadow silhouettes, lights the player's
hand with the shader, and adds the LOD falloff / fog / detail / build-upload-draw budget controls
that were missing from the Advanced tab. New worlds also get bigger cave systems and taller
mountains; old saves keep their terrain. See [V6.19 notes](docs/releases/v6.19.md).

Update 6.18 adds two new title-screen panoramas (Coral Coast and Snowy Peaks, plus a Cycle
mode), a fifth **Advanced** options tab unlocked by the **Debug Video** switch, and a colour key
with a content list on Create New World so the aquatic biomes are visible before you create a
world. It also cuts frame spikes and retained memory: reused collision buffers, background
chunk saves, incremental autosaves, bounded unloads, a configurable chunk cache, and
allocation-free redstone propagation. See [V6.18 notes](docs/releases/v6.18.md).

## Edit textures in Aseprite

1. Run **Edit-textures.bat** in the extracted game folder. It creates
   `resourcepacks/MyTextures/` and opens a searchable picture index.
2. Edit a **32×32 PNG** in Aseprite and save/export to the same PNG filename.
3. In-game, open **Options → Resource Packs → Reload PNGs**, then select
   **My Aseprite Textures**. After later saves, just press **Reload PNGs**.

No Java/source editing or extra tools needed. The exporter **never overwrites an
existing pack**. Includes 558 individual tiles and six animation sheets, with
filenames, face/component mappings, and Aseprite import/export instructions.
A separate ready-made texture kit is also available with the release.
Read the [complete editing guide](package/Texture-editing.txt), including backups
and the difference between editable PNGs and the reference atlas.

## Download and play (the easy way)

1. Open the **[Releases page](../../releases/latest)** of this repository (right side of the repo's front page: *Releases*).
2. Download **`Blockcraft-V<version>-windows.zip`** (about 88 MB, Java is inside - nothing to install).
3. Right-click the zip -> *Extract All...* into a **new folder**, open the `Blockcraft` folder, run **`Blockcraft.exe`**.

No git, no pulling, no branches. Every update gets its own release with the complete game zip (and a smaller ~39 MB
patch). Your worlds and options are stored in `%USERPROFILE%\.blockcraft112`, so they survive updates, even when
you extract a new version into a new folder. The player manual with the full changelog is `package/README.txt`
(also inside the zip).

**Seeing older art?** Check the title says V6.16 and select **Natural Pixel 32 (Default)**
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
