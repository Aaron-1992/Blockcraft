# Fourth to-do list (`To do.txt`, commit c214df8, 2026-10-05 00:50 +0200) - work board

Same rules as below: **`git fetch` before every commit, rebase on `origin` (never force-push), small commits, put a line here when you take an item.**
The owner wants **item 1 first**.

| # | Item (the owner's words in short) | Taken by | State |
|---|---|---|---|
| 1 | **PERFORMANCE - do this one first.** The game runs entirely on the integrated GPU when there is one; it must use the dedicated GPU when there is one. Then general speed. | **run 10**: **done, goes out with 3.18** - `GpuPref` (Windows graphics preference of `javaw.exe` = high performance, one restart when the integrated GPU was in use, options switch "Fast GPU", F3 line), `Perf` + `PerfBench` (frame timer, Shift+F3, `perf.log`, benchmark, workflow `Perf`), `CaveCull` (hidden sections not drawn), the shadow pass cut to the map's window and redrawn only when needed, render scale, Max FPS. Still open, measured but not done: the distant terrain (`Lod`) costs as much as the real terrain (about 2 M vertices a frame at LOD 64), vertex format 44 -> 24 bytes, region buffers. Files I touched: `Renderer.renderWorld / renderShadowPass / upload`, `Shaders.endScene`, `Game.loop / render / drawDebug`, `Options`, `Screens` (video tab), `Mesher` (the connectivity), `Chunk.Section.conn`, `Lod.render` (counters). | done (release 3.18 pending) |
| 2 | Massive structure overhaul (the 3.14 villages are in; open: temples, igloos, shipwrecks, ocean ruins, ruined portals, mineshafts, ...) | open (run 7's area) | open |
| 3 | Massive models overhaul - **villagers and blazes are very bad** | open (run 8's area: `Rig`, `Rigs317`, `Folk317`, `Nether317`) | open |
| 4 | Massive texture overhaul - ALL block and item textures (the mobs' skins are done) | run 9 (`TexPaint`, 3.18 textures 1 = tiles 0-399 blocks) | in progress |
| 5 | Massive animation overhaul - **the player's animation is bad, the dragon is stiff** | open (run 8's area: `PlayerModel`, `PlayerAnim317`, `Anim317`, `AnimE317`) | open |
| 6 | Massive graphics overhaul - the owner sees "nothing changed" (needs changes that are visible: light, shadows, water, shaders, post effects) | open (run 6's area, no pushes since 16:05 UTC) | open |
| 7 | Add the missing features of the last update (redstone, XP + enchanting, brewing, End cities, endermites ...) | run 9 (took the rest of item 11 on 2026-10-04 21:50 UTC) | open |

# Third to-do list (`To do.txt`, commit 13db49b) - work board

Two runs of the assistant push to the branch `arena/01a0fdd0-blockcraft`. **Read this before you start an item, `git fetch` before every
commit, rebase on `origin`, keep commits small, and do not reformat files you do not own.** Put a line in the table when you take an item.

| # | Item | Taken by | State |
|---|---|---|---|
| 1 | Massive world gen overhaul | run 1 (`TerraGen`, `Biomes`, `TerraTrees`, `FlatGen`, `WorldSettings`) | WIP since 962f163 |
| 2 | Massive structure overhaul | run 7 (took it 2026-10-04 17:40 UTC from run 4, who has not pushed since 13:03 UTC): text-art building templates with a palette per biome (`Tpl`, `Style`), new villages (`Village314`: streets, plots, 15+ building kinds, interiors), temples, igloos, huts, shipwrecks, ruins, ruined portals ... (`Struct314`); only for the new world types (`WorldSettings.structVer` 2), classic worlds and 3.13 worlds keep their villages | in progress (3.14) |
| 3 | Massive models overhaul (mobs, held items, block shapes) | block shapes and held items: run 5 (`Shapes`: panes, bars, walls, gates, trapdoors, carpets). **The mobs: run 8** (took them 2026-10-04 19:05 UTC; run 5 has not touched `MobRender` / `MobModels312` since 3.12): a new skeleton system (`Rig`: bones, cuboids, procedural skins at twice the resolution), new models and skins for all 18 mobs (and the new mobs of item 11) | in progress (3.17) |
| 4 | Massive texture overhaul | run 6 (took it 2026-10-04 16:00 UTC from run 5, who has the shapes / redstone / mobs to do): the EXISTING tiles 0-399 are redrawn in place (same tile ids, nothing moves) plus texture filtering; run 5 keeps `TexArt315` (tiles 600-999) for the tiles of its new blocks, run 1's `TexArt313` stays as it is | in progress |
| 5 | Massive animation overhaul | **run 8** (took it 2026-10-04 19:05 UTC; it was open): jointed legs / arms, idle and attack clips, blinking, ears and tails, death and hurt reactions for the mobs, then the player (swimming, climbing, bow, sleeping, elytra), then moving blocks (doors, gates, trapdoors, chest lids - `Mesher` hook, I will tell run 5 / 6 here before I touch it) | in progress (3.17) |
| 6 | Massive graphics overhaul (shaders, lighting, particles, sky) | run 6 (took it 2026-10-04 16:00 UTC from run 5): the sky and the light of the day (`Sky`, `Renderer.drawSky / computeSky`), `ShaderSrc` / `Shaders` (fog, post effects, ambient occlusion), particles; run 5 please do not edit those files without a note here | in progress |
| 7 | New title screen (spinning background) | run 2 (`Panorama`, new `Screens`) | in progress |
| 8 | Multiple world gen settings | the data is run 1's (`WorldSettings`), the Create New World screen is run 2's (`Screens`) | in progress |
| 9 | Make the inventory more understandable | run 2 (`Creative`, `Menu`) | tabs, search, tooltips, number keys, Q: done |
| 10 | Clean up the settings screen | run 2 | in progress |
| 11 | Add the missing features of 3.12 (coloured beds, banners, brewing, enchanting, redstone, End cities, endermites) | run 5. Order: shapes + coloured beds + banners + redstone (`Shapes`, `Redstone`), then XP + enchanting + brewing, then the End (outer islands, cities, shulkers, elytra, endermites) | in progress |
| - | Release 3.13 (terrain + menus + inventory), then 3.14 ... | run 4 (CI fixed: the parrot test used an unseeded generator) | 3.13 released |
| - | **Release 3.17** = the tip at 22:00 UTC: 3.14 structures 1, 3.15 shapes / beds / banners, 3.16 sky + particles, 3.17 mobs (docs for all four, version, update pack, Windows smoke, publish) | **run 9** | **released 2026-10-04 22:10 UTC** (v3.17, Latest). The next release is 3.18: whoever pushes next bumps `Game.TITLE_STR`, adds "WHAT'S NEW IN UPDATE 3.18" to `package/README.txt` and builds the pack with `python3 tools/build.py --patch` (see `docs/DEVELOPMENT.md`) |
| 4 | (texture overhaul, continued) | **run 9 takes it over from run 6** (2026-10-04 21:50 UTC; run 6 has pushed no texture work since claiming it at 16:00 and has not touched `Tex` / `TexArt37 / 310 / 312` / `Color312`): a new painter (`TexPaint`: material palettes, multi-scale noise, outlines, bevels) redraws the tiles 0-399 in place - same ids, same atlas, 16 x 16 - checked with `bc.AtlasSheet`; `TexArt313` / `TexArt315` stay as they are | in progress |
| 11 | (missing features, continued) | **run 9 takes the rest of item 11 from run 5** (2026-10-04 21:50 UTC; run 5 has not pushed since 14:45 UTC): redstone first (`Redstone`: wire, torch, lever, buttons, plates, lamp, repeater, pistons, doors / gates / trapdoors / TNT; the block ids 1900-2199 run 5 reserved), then XP + enchanting + brewing, then the End; run 5: if you are still working on any of this, say so here and I will stop | in progress |

Where the screens live: the new screens are drawn by the new class `Screens` (title, singleplayer list, create world, options, pause); `Game.render()` only calls it.
`WorldSettings` (type, structures, bonus chest, flat preset) stays the data model of the Create New World screen.

## Ranges that run 5 uses (please stay out of them)

| What | Range |
|---|---|
| Block ids | 1500-2999: `Shapes` 1500-1899 (panes 1500, bars 1517, walls 1520, fences 1530, gates 1540, trapdoors 1600, carpets 1720), `Redstone` 1900-2199, beds 2200-2327 (`Decor315`), banners 2330-2463 (`Decor315`), brewing / enchanting 2500-2599 |
| Item ids | 400-999 (beds 400-415, banners 420-435) |
| Atlas tiles | 600-999 (`TexArt315`); free below that: 296-399, 489-519, 588-599 |
| Tests / tools | `Tests315`, `MeshView` (software view of the real chunk meshes: `bc.MeshView out.png scene`), `IconSheet`, `Scenes315` |

## Ranges that run 8 uses (mobs and animation)

| What | Range |
|---|---|
| Files | `Rig`, `RigPaint`, `RigDraw`, `Rigs317` (the models), `Anim317` (the clips), `Tests317`, `test/bc/MobView` (software view of the mobs: `bc.MobView out.png type [pose ...]`); hooks: `MobRender.render` (calls `RigDraw` for every type that has a rig), `Anim` (kept, extended) |
| Mob ids | 18-39 (item 11's shulker, endermite, ...; run 5: tell me here if you need a mob and take 40+) |
| Block ids / items / atlas tiles | none (mob skins are their own textures, not atlas tiles) |
| Player animation (started 2026-10-04 20:10 UTC) | `PlayerModel.draw` + `PlayerModel.Pose` are being rewritten (two-part arms and legs with elbows / knees, swimming, climbing, bow, eating, flying, death), new `PlayerAnim317` (the joint angles; plain Java, tested), `Game.pose / tickAnim`, `Renderer.drawSelf`. The first-person hand (`drawArmFirstPerson`, `Renderer.renderHand`) is not touched. **Run 5 (elytra):** set `Pose.glide` / `Pose.elytra` from your code and the body flies; if you need the wings drawn on the back, tell me here, I will leave a hook `PlayerModel.drawBack` |

## Ranges that run 7 uses (structures)

| What | Range |
|---|---|
| Files | `Tpl`, `Style`, `Village314`, `Struct314`, `Tests314`, `test/bc/StructView` (look at villages without a GPU); hooks: `Structs.apply` (new villages only when `settings.structVer >= 2`), `WorldSettings.structVer` |
| Block ids | 3000-3499 (nothing yet; typed loot chests if needed) |
| Items / atlas tiles | none |
