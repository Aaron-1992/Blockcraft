# Third to-do list (`To do.txt`, commit 13db49b) - work board

Two runs of the assistant push to the branch `arena/01a0fdd0-blockcraft`. **Read this before you start an item, `git fetch` before every
commit, rebase on `origin`, keep commits small, and do not reformat files you do not own.** Put a line in the table when you take an item.

| # | Item | Taken by | State |
|---|---|---|---|
| 1 | Massive world gen overhaul | run 1 (`TerraGen`, `Biomes`, `TerraTrees`, `FlatGen`, `WorldSettings`) | WIP since 962f163 |
| 2 | Massive structure overhaul | run 1 (after #1: structures follow the new biomes) | open |
| 3 | Massive models overhaul (mobs, held items) | run 2 | open |
| 4 | Massive texture overhaul | run 1 for the textures of new blocks (`TexArt313`); run 2 for the existing block / item textures | open |
| 5 | Massive animation overhaul | run 2 | open |
| 6 | Massive graphics overhaul (shaders, lighting, particles, sky) | run 2 | open |
| 7 | New title screen (spinning background) | run 2 (`Panorama`, new `Screens`) | in progress |
| 8 | Multiple world gen settings | the data is run 1's (`WorldSettings`), the Create New World screen is run 2's (`Screens`) | in progress |
| 9 | Make the inventory more understandable | run 2 (`Creative`, `Menu`) | tabs, search, tooltips, number keys, Q: done |
| 10 | Clean up the settings screen | run 2 | in progress |
| 11 | Add the missing features of 3.12 (coloured beds, banners, ...) | run 2 | open |

Where the screens live: the new screens are drawn by the new class `Screens` (title, singleplayer list, create world, options, pause); `Game.render()` only calls it.
`WorldSettings` (type, structures, bonus chest, flat preset) stays the data model of the Create New World screen.
