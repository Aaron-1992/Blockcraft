# Third to-do list (`To do.txt`, commit 13db49b) - work board

Two runs of the assistant push to the branch `arena/01a0fdd0-blockcraft`. **Read this before you start an item, `git fetch` before every
commit, rebase on `origin`, keep commits small, and do not reformat files you do not own.** Put a line in the table when you take an item.

| # | Item | Taken by | State |
|---|---|---|---|
| 1 | Massive world gen overhaul | run 1 (`TerraGen`, `Biomes`, `TerraTrees`, `FlatGen`, `WorldSettings`) | WIP since 962f163 |
| 2 | Massive structure overhaul | run 4 (took it 2026-10-04 13:10 UTC: run 3 has not pushed since 12:14 UTC and its last CI run, 575c67e, was red) | in progress (3.14: biome villages, temples, igloos, shipwrecks, ...) |
| 3 | Massive models overhaul (mobs, held items, block shapes) | run 5 (picked up from run 3, 2026-10-04 14:30 UTC): new block shapes first (`Shapes`: panes, bars, walls, gates, trapdoors, carpets), then the mobs | in progress |
| 4 | Massive texture overhaul | run 5 (`TexArt313` stays run 1's work; mine will be `TexArt315`) | open (after the shapes / redstone) |
| 5 | Massive animation overhaul | run 5 | open |
| 6 | Massive graphics overhaul (shaders, lighting, particles, sky) | run 5 (`ShaderSrc`, `Shaders`) | open |
| 7 | New title screen (spinning background) | run 2 (`Panorama`, new `Screens`) | in progress |
| 8 | Multiple world gen settings | the data is run 1's (`WorldSettings`), the Create New World screen is run 2's (`Screens`) | in progress |
| 9 | Make the inventory more understandable | run 2 (`Creative`, `Menu`) | tabs, search, tooltips, number keys, Q: done |
| 10 | Clean up the settings screen | run 2 | in progress |
| 11 | Add the missing features of 3.12 (coloured beds, banners, brewing, enchanting, redstone, End cities, endermites) | run 5. Order: shapes + coloured beds + banners + redstone (`Shapes`, `Redstone`), then XP + enchanting + brewing, then the End (outer islands, cities, shulkers, elytra, endermites) | in progress |
| - | Release 3.13 (terrain + menus + inventory), then 3.14 ... | run 4 (CI fixed: the parrot test used an unseeded generator) | releasing now |

Where the screens live: the new screens are drawn by the new class `Screens` (title, singleplayer list, create world, options, pause); `Game.render()` only calls it.
`WorldSettings` (type, structures, bonus chest, flat preset) stays the data model of the Create New World screen.

## Ranges that run 5 uses (please stay out of them)

| What | Range |
|---|---|
| Block ids | 1500-2999: `Shapes` 1500-1899 (panes 1500, bars 1517, walls 1520, fences 1530, gates 1540, trapdoors 1600, carpets 1720), `Redstone` 1900-2199, beds 2200-2327 (`Decor315`), banners 2330-2463 (`Decor315`), brewing / enchanting 2500-2599 |
| Item ids | 400-999 (beds 400-415, banners 420-435) |
| Atlas tiles | 600-999 (`TexArt315`); free below that: 296-399, 489-519, 588-599 |
| Tests / tools | `Tests315`, `MeshView` (software view of the real chunk meshes: `bc.MeshView out.png scene`), `IconSheet`, `Scenes315` |
