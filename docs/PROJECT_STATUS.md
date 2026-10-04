# Blockcraft – project status

_Last updated: 2026-10-04 (Update 3.13)._

## Roadmap: the owner's "To do.txt" (worked through step by step, in this order)

| # | Item | State |
|---|---|---|
| 1 | Weather system with volumetric clouds, storms and rain visible in the distance, working without shaders | **done in Update 3.8** (weather field, clouds in both render paths, rain / snow / lightning, `/weather`) |
| 2 | Fix LODs and adapt them for the weather system | weather part **done in 3.8** (the LOD follows the weather's fog / sun / brightness); 3.9 makes the fog radial so the world's edge is hidden from every angle; inspected in day / night / shader / top-down scenes (`lod-*`) - no further defect found without a concrete report |
| 3 | Skins (easy import of Minecraft skins) | **done in Update 3.9** (drag & drop, file dialog, username download, clipboard; legacy / HD skins; slim arms; F5 camera; inventory preview) |
| 4 | Smooth animations | **done in Update 3.9**: water / lava / fire frames are blended (`Tex.animate(tick, partial)`), falling blocks and TNT interpolate (`World.Falling.px/py/pz`), the hand has an equip animation (`Game.tickEquip`); the sneak height and the mobs / player / items / particles were already smooth |
| 5 | Natural disasters | **done in Update 3.10**: tornadoes (born in thunderstorms), earthquakes, meteors (`Disasters`, `DisasterFx`; `/disaster`, Options > Disasters) |
| 6 | Trees fall over when chopped; the fallen trunk can be harvested further | **done in Update 3.10** (`Trees`; logs on their side = ids 220-225; the felled trunk is harvested piece by piece) |
| 7 | Slabs, stairs etc. on the top half of a block / upside down | **done in Update 3.9** (`Hit.fy`, ids 195-219, `Game.placeBlock`, slab merging) |
| 8 | Make creepers ignitable | **done in Update 3.9** (flint and steel) |
| 9 | Fire drawn like in Minecraft when attached to a block | **done in Update 3.9** (`Mesher.fire`: wall / ceiling / corner flames) |
| 10 | Hide interior surfaces (performance) | **done in Update 3.9**: `Mesher.leafMode` (Options > Leaves: Fancy / Smart / Fast) - leaf-to-leaf faces were ~30 % of all faces in a forest |
| 11 | Fix mobs not being able to jump up a block | **done in Update 3.9** (`Mobs.JUMP`) |

## Second to-do list (the owner's "To do.txt" of 2026-10-03, commit c52da00)

| # | Item | State |
|---|---|---|
| 1 | Fix hand models | **done in 3.11**: Minecraft 1.12's own transforms for the empty arm, flat items, tools, blocks, flat blocks (torch, flowers), swing, swap, eating, sway; third-person held items (`Renderer.renderHand`, `PlayerModel.drawArmFirstPerson` / `drawHeld`) |
| 2 | Make entities react to shader lighting | **done in 3.11**: `ShaderSrc.ENTITY_FS` (sun tint, shade + terrain shadows, placed lights, fog) for mobs, items, falling blocks, debris, your body and your hand |
| 3 | Make clouds and weather more realistic | **done in 3.11 (shader sky) and 3.12 (no-shader storm towers are domes with cauliflower tops, `WeatherFx.thickness`)**: the shader sky (billows that lean with height, eroded edges, silver lining, height-dependent ambient, a cirrus layer, steeper storm towers). The blocky no-shader clouds only got the same tower walls; rain, snow and lightning are unchanged |
| 4 | Make water look better with shader | **done in 3.11**: `ShaderSrc.WATER_FS` (depth colour, refraction, foam, layered ripples, glitter, mirrored terrain with "FANCY") |
| 5 | Add more 1.12 features | **done in 3.12**: the World of Colour (15 dyes, dyed wool and sheep, concrete + powder, terracotta, stained / glazed terracotta, stained glass, clay, storage blocks, emerald ore: `Color312`), 34 advancements (`Adv312`, L), parrots, ender pearls you can throw, soul sand / magma / netherrack fire rules, and everything of the two dimensions |
| 6 | Upgrade the villages | **done in 3.12**: farms, smithies, lamp posts (`Structs.upgrade / farm / lamp`), villagers with six professions who trade for emeralds (`Trades`, `Menu` kind 5), iron golems, villagers flee from monsters |
| 7 | Dimensions | **done in 3.12**: the Nether (`Dim`, `Portals`, `NetherGen`, `NetherFortress`, five new mobs) and the End (`Stronghold`, `EndPortals`, `EndGen`, `EndFight`, `Dragon`, endermen); one `World` per dimension, 12-bit block ids |
| 8 | Fix door culling | **done in 3.11**: `Mesher.boxBB` tested the wrong side for the two z faces (a bug since 3.5) |
| 9 | Make textures only render from one side | **done in 3.11**: GL back-face culling for terrain and LOD, back quads for plants / crops / flames / water surfaces |
| 10 | Better animations (Fresh Animations style) | **done in 3.11** for mobs, the player body and the first-person hand (`Anim`); no new models, no sound |
| 11 | Make LODs resemble what is in the chunk when something is broken or built | **done in 3.11**: `LodMap` summaries of changed chunks (saved in `lod.dat`, old saves scanned in the background), tiles rebuilt and swapped without a gap |
| 12 | Fix LODs popping up in fully rendered chunks when trees burn | **done in 3.11**: `Chunk.everMeshed` (a chunk stays covered while a section is re-meshed), tiles that may reach the real chunks are always split to 32 |

## Third to-do list (the owner's "To do.txt" of 2026-10-03 22:17 UTC, commit 13db49b)

| # | Item | State |
|---|---|---|
| 1 | Massive world gen overhaul | **done in 3.13**: `TerraGen` (28 biomes from five climate fields, oceans, rivers, ravines, caves, mountains with a snow line, 17 kinds of plants and trees, biome tints, structures on top), `Biomes`, `TerraTrees`, 70 new blocks (`Blocks313`, `TexArt313`) |
| 2 | Massive structure overhaul | open (planned for 3.14: biome villages, temples, igloos, shipwrecks, ocean ruins, ruined portals, better mineshafts) |
| 3 | Massive models overhaul | open |
| 4 | Massive texture overhaul | open (the textures of the new blocks are done: `TexArt313`) |
| 5 | Massive animation overhaul | open |
| 6 | Massive graphics overhaul | open |
| 7 | New title screen (spinning background) | **done in 3.13**: `Panorama` (a camera turning over real terrain in a throw-away world), `Logo`, `Screens.title` |
| 8 | Multiple world gen settings | **done in 3.13**: `WorldSettings` (Default / Large Biomes / Amplified / Superflat with 4 presets / Classic, structures on / off, bonus chest; saved in `level.properties`), `Screens.newWorld` with a map preview (`WorldPreview`) |
| 9 | Make the inventory more understandable | **done in 3.13**: `Creative` (tabs, search), `Menu` tooltips (durability, damage, armour, food, fuel, smelts into), number keys, Q |
| 10 | Clean up the Settings screen | **done in 3.13**: `Screens.options` with four tabs (Video / Graphics / Gameplay / Controls) and a help line (`Ui.hint`); the keys cannot be changed yet |
| 11 | Add the missing features of the last update | open (coloured beds, banners, brewing, enchanting + XP, redstone, End cities with shulkers and elytra, endermites) |

## What it is

Blockcraft is an original Minecraft-Java-1.12-style block sandbox for Windows ("an original clone": no Mojang code or
assets, everything - textures, world generator, physics - is written from scratch). Java 21 + LWJGL 3 (OpenGL);
shipped as a zip with a bundled Windows JRE and `Blockcraft.exe` (a Launch4j wrapper around the jar).
Details for developers: [`DEVELOPMENT.md`](DEVELOPMENT.md).

## Current state: Update 3.13 - "a new world, new menus, a new inventory"

- **World generator** (`TerraGen`, `Biomes`, `TerraTrees`, `FlatGen`, `WorldSettings`, `Dim.makeGen(dim, seed, settings)`): the type is chosen on the Create New World screen and stored in `level.properties`
  (`gentype`, `structures`, `bonus`, `flat`); a world without `gentype` is CLASSIC (`Gen`, unchanged - the strict `Equivalence` test still compares it block by block). `TerraGen.sample` computes one `Cl` per column
  (continentalness, erosion, peaks, temperature, humidity -> height, biome, river factor); `generate` builds stone, the surface (`surface()` rules per biome), water, ice, caves (`Gen.carve`), ravines, ores, then
  `decorate` (grass, ferns, flowers, mushrooms, cacti, snow layers, lily pads), `treesFor` (`TerraTrees`: plants are drawn into a buffer round their root and clipped to the chunk, so trees that cross a chunk
  border come out the same in both chunks), `Structs.apply` (villages, dungeons, mineshafts) and `Stronghold.apply` when the world has structures. `findSpawn` rings out to 4000 blocks; `Game.findStand` looks through
  snow and plants for the ground. `Bonus` puts the bonus chest next to the first step.
- **Menus** (`Ui`, `Screens`, `Panorama`, `Logo`, `WorldPreview`, `Creative`, `Menu`): immediate-mode widgets drawn through `Gui` only (so `GuiRaster` can draw every screen without OpenGL), help line, tabs, scrolling
  lists, world thumbnails drawn from the generator, a live panorama. Layout of every screen is tested at seven window sizes (`TestsUi`).
- **Tests**: `Tests313` (terrain: determinism, valid ids, `column()` agrees with the real ground, no air pockets under the sea, 28 biomes and a believable land share for six seeds, spawn on land for 25 seeds x 4 types,
  Superflat / Large / Amplified, saved settings, every block has a picture, structures only when asked; a soak test that walks a player through 16 biomes day and night; the bonus chest; the stand position), `TestsUi`, `TestsInv`.
  Dev tools in `test/bc/`: `VoxView` (software ray caster: `bc.VoxView out.png seed type x y z yaw pitch [dist] [w] [h] [fov]`), `MapDump`, `AtlasSheet`, `GuiRaster`, `UiShots`, `PanoView`.

## Update 3.12 (still in the game) - "the Nether, the End, villages, a world of colour"

- **Foundation**: block ids are 12 bits (`Chunk.blocks` is a `short[]`; ids 0..255 and 1024..4095 are blocks, 256..1023 items, new items from 352), chunk files keep the old byte array and add the high
  nibbles behind the `BCX1` magic only when a chunk holds a new block (old worlds load unchanged), the atlas is 32 x 32 tiles (`TexArt312`, `Color312` tiles from 400).
- **Dimensions** (`Dim`, `World(seed, dir, dim)`, `Game.switchDimension`, `/dim nether|end|overworld`): one world per dimension in `DIM-1` / `DIM1`, no sky / weather / LOD / disasters outside the Overworld.
  - *Nether*: `Portals` (obsidian frames 2..21 x 3..21 inside, flint and steel, 80 ticks, coordinates 1:8, portal film = mesher kind 8, exit portal found or built), `NetherGen` (bedrock floor and ceiling, lava sea to
    y 31, netherrack, glowstone, soul sand, gravel, quartz ore, magma), `NetherFortress` (`BoxPlan`: bridges, corridors, halls, throne room with a blaze spawner, chests; 70 % of the 256 x 256 regions),
    mobs zombie pigman / ghast / blaze / magma cube (`Mobs312`), items quartz, nether brick, blaze rod / powder, ghast tear, gold nugget, emerald. Rules: fire on netherrack never burns out (`Fire`), soul sand drags
    (`Player.travel`), magma burns the feet unless sneaking (`Player.tick`).
  - *Strongholds* (`Stronghold`, `BoxPlan`): 3 / 6 / 10 in rings at 640-960 / 1500-2100 / 2800-3400 blocks, portal room with 12 frames round a lava pit, libraries, closets, a spawner; `EndPortals`: eyes of ender
    (craft: pearl + blaze powder) fly towards the nearest stronghold, 1 in 5 breaks, frames take eyes, the pit fills with portal.
  - *The End* (`EndGen`, `EndFight`, `Dragon`): one island, ten obsidian pillars with crystals, the dragon (200 HP; circle, swoop with breath, perch on the fountain, 200 ticks of dying), exit portal + egg, boss bar,
    endermen (teleport, water hurts, look-aggro, drop pearls), void kills below y -12, a neutral light (`Dim.ambientTint`). **Thrown ender pearls** (`Mobs312.throwPearl`: a `Fireball` with `pearl`, gravity 0.03,
    drag 0.99; you land at the last free spot, 5 damage that armour does not stop).
- **Villages** (`Structs`, `Mobs312`, `Trades`): farms (9 x 7, fenced, water channel, four rows of crops), smithy, lamp posts; villagers (farmer, librarian, priest, blacksmith, butcher, nitwit; `Mob.color` = profession)
  and iron golems (`keepsForever`), trading screen (`Menu` kind 5, emeralds).
- **World of Colour, advancements, parrots** (`Color312`, `Adv312`, `Mobs312`, written by a second run of the project): see the roadmap row 5.
- **Weather**: storm towers without shaders rise as domes (`WeatherFx.thickness`).
- Under the hood: tests grew to ~879,000 checks (`Tests312`: foundation, nether, netherMobs, fortress, stronghold, end, villages, colors, parrots, advancements, pearls, netherRules), `tools/ci_annotate.py` (reads the
  results of a CI run without log access), screenshot hooks `bc.dim`, `bc.dragon`, `bc.inv=trade`, `bc.summon`, `bc.set`, `bc.adv`, `bc.parrot`.

## Update 3.11 (still in the game) – "hands, water, distant terrain, animation"

- **Single-sided faces** (`Renderer.cullFaces`, `Mesher.emitBack`): terrain and LOD are drawn with back-face culling (every block's mesh and every LOD quad winds counter-clockwise
  seen from outside - `Tests311` checks all of them); plants, crops, flames and the top of water / lava get a second quad facing the other way. `-Dbc.legacymesh=true` gives the
  3.5 meshes back (the strict equivalence pass uses it), `-Dbc.nocull=true` draws double-sided again. A spectator under the map sees the sky (no cave darkening) and no LOD walls.
- **Doors** (`Mesher.boxBB`): the face-against-a-neighbour test used the even / odd face number to pick the min / max side, but the face order is +y -y -z +z +x -x, so the z faces were swapped.
- **LOD from the real world** (`LodMap`, `Lod`, `Chunk.lodDirty`, `World.lodRefresh`): summaries of changed chunks, `lod.dat`, background scan, `Lod.invalidate` / `refresh`, trees drawn only where a
  trunk of >= 3 logs still stands, no generator houses over changed chunks; `Chunk.everMeshed` for the coverage test; hooks `-Dbc.tp=frame:x,y,z`, `-Dbc.lodage`.
- **Hands** (`Renderer.renderHand`, `PlayerModel`): Minecraft 1.12's `ItemRenderer` numbers; `Game.tickArm` (sway), `bobDist` / `bobAmp` (hand bob, `/bobbing`), hooks `bc.handswing`, `bc.handeat`.
- **Shaders**: `ENTITY_FS` (`Shaders.useEntity`, `Renderer.brightnessAt` forwards each entity's light level), `WATER_FS` (derived from `WORLD_FS` at class load by `ShaderSrc.deriveWater`; needs
  a copy of the scene's colour and depth: `Shaders.copyScene`), `Options.waterQ` (1 = no mirrored terrain, 2 = FANCY), the clouds in `CLOUD_FS`. `tools/glsl_check.py` parses every shader
  without a GPU (CI runs it).
- **Animation** (`Anim`, `MobRender.drawMob`, `PlayerModel.draw`): pure functions, unit-tested for range and smoothness; hooks `bc.walkphase`, `bc.graze`, `bc.posewalk`.
- Under the hood: tests grew to ~487,000 checks (`Tests311`); `Tex.initPixels` (the atlas without OpenGL).

## Update 3.10 (still in the game) – "natural disasters and falling trees"

- **Falling trees** (`Trees.java`): breaking a log of a tree with leaves makes everything above the cut tip over as one rigid piece (90 degrees about the cut cell,
  away from the player, else the best free direction; none when walled in). The trunk lands as **logs on their side** (`Blocks.LOG_X0` 220-222 / `LOG_Z0` 223-225, bark turned
  by `TexArt310`), branches and leaves land around the tip (pushed up or crumbling), a cut higher up leaves a stump and the top drops until it rests. Mobs / the player under a
  trunk take 7 half hearts and are lifted. Log pillars and cabins (no leaves) do not fall. Logs placed against a side face now lie along that axis. `/treefall on|off`.
- **Natural disasters** (`Disasters.java`, `DisasterFx.java`): *tornadoes* (wall cloud, two-shell funnel, dust skirt; a force field that swirls, draws in and lifts the player, animals and
  items, hurts in the core; tears leaves / plants / glass / wool / wooden furniture out as flying debris, uproots trees; never touches terrain, stone, ores, chests, furnaces),
  *earthquakes* (camera shake by distance, crumbling cliffs and overhangs, plants / torches / glass shaken loose, toppling trees) and *meteors* (additive streaks, a blast, fires,
  obsidian; showers at night). They start by themselves (rare, after a grace period of 7.5-30 minutes; tornadoes only under thunderstorms), `Options > Disasters`, `/disaster on|off|stop|tornado|quake|meteor`.
- Camera shake (`Game.shake`, `Renderer.renderWorld`) for quakes, landing trees and impacts.
- Under the hood: `Blocks.COUNT` 226, tiles 246-248, ~1,800 lines of new rules (`Trees`, `Disasters`) + ~300 of drawing (`DisasterFx`), `TestsFall` (~1,000 checks) and `TestsDisasters` (~300 checks), screenshot hooks `bc.fill`,
  `bc.fell`, `bc.fellangle`, `bc.disaster`, `bc.dfreeze`.

## Update 3.9 (still in the game) – "the skins update"

- **Skins** (`Skins.java`, `PlayerModel.java`): any Minecraft skin PNG - 64x64, the old 64x32 (left limbs mirrored from the right ones), HD (a
  multiple of 64, scaled down) - is copied into `%USERPROFILE%\.blockcraft112\skins`. Ways in: drag & drop onto the window (GLFW drop callback,
  works in menus and in the game), "Import skin file..." (AWT `FileDialog` on a helper thread; the game leaves fullscreen while it is open
  because a fullscreen GLFW window is topmost and would hide it; the dialog is disposed so no AWT thread outlives the game), a Minecraft
  username or account id (Mojang's profile + session servers over `java.net.http`, then the texture URL; also takes a link to a PNG), and
  Ctrl+V (path / link / name). Classic vs slim arms are detected from the unused pixel column and can be switched. The default skin is drawn in
  code. `Game.skins`, `Options.skin`, screen `S.SKINS` (title screen + pause menu buttons).
- **You on screen**: F5 = first person / behind / in front (`Renderer.thirdPerson`: 4 blocks along the line of sight, stopped by blocks);
  `Renderer.drawSelf` draws `PlayerModel` with the walking / sneaking / swinging pose from `Game.tickPose` and the held item in the fist
  (`PlayerModel.drawHeld`); the first-person arm is drawn when the hand is empty; the survival inventory shows the model looking at the mouse.
- **Mobs jump up blocks**: the mob loop applies gravity *before* moving, so the old jump speed 0.42 peaked at 0.83 blocks; `Mobs.JUMP` =
  0.42 / 0.98 + 0.08 gives Minecraft's 1.25 (one block yes, two no). `Tests39.steps` drives all animals, babies and zombies at walls.
- **Creepers can be lit** (`Mobs.ignite`, flint and steel in `Game.useOnMob`): the fuse runs by itself, the creeper stands still.
- **Fire models** (`Mesher.fire`): X on a supporting block (solid top or flammable below), otherwise a flame plane on each flammable
  side neighbour and crossed planes under a flammable ceiling.
- **Slabs / stairs upside down** (`Blocks.STAIR_TOP0` 195..214, `SLAB_TOP0` 215..219; `Blocks.COUNT` = 220): chosen from where you click (`World.Hit.fy`,
  `Game.placeBlock`), mirrored mesh in `Mesher.stair`, merging of two slabs (`Game.mergeSlabs`), plain item drops. Stairs and slabs light themselves
  from the best light around them (`Mesher.neighbourLight`) - they block light, so their own cell and their neighbours' cells are dark.
- **Interior surfaces**: `Mesher.hiddenLeafFace` / `leafMode` (0 fancy, 1 smart = default, 2 fast); `Options.leaves`; the strict equivalence pass runs with `-Dbc.leaves=0`.
- **Smooth animations**: see the roadmap table; `Tests310.smooth`.
- Fog distance in both vertex shaders is radial; `Main` ends the process with `System.exit(0)` after a normal exit.

## Update 3.8 (still in the game) – "the weather update"

- **A weather field that drifts with the wind** (`Weather.java`): storm systems you can see coming - a dark thundercloud with a rain curtain under it on the
  horizon is the storm that will reach you. Cloud cover -> rain -> thunderstorm by thresholds on one field; climate cycle (dry and stormy stretches);
  `/weather` commands (`clear|rain|thunder [s]`, `storm [distance]`, `natural`, `on|off`, status); saved with the world; new worlds start calm for 5 minutes.
- **Clouds in both render paths**: with shaders the raymarched clouds follow the field (fair cumulus, grey rain sheets, tall dark thunderheads with an
  anvil and a lowered base, lit from inside by lightning); without shaders blocky 3D clouds (two levels, out to 3.6 km) plus rain curtains. Rain, snow
  (ice plains, high mountains), splashes, grey sky / closer fog, hidden sun and moon, lightning bolts and flashes.
- **Game rules**: rain puts fires out and waters farmland, no sun-burn for zombies / skeletons in rain, thunder lets you sleep by day, lightning ignites,
  hurts and charges creepers (blue glow, double blast). Options: Weather ON/OFF.
- The no-shader LOD now has the same brightness as the real terrain (it follows the weather-dimmed sky light).
- Under the hood: `Weather`, `WeatherFx`, new cloud shader, `Tests38` (160+ checks incl. CPU/GPU parity), screenshot hooks `bc.weather` & co.

## Update 3.7 (still in the game) – "the kitchen & fire update"

- **Fire** (`Fire.java`, Minecraft 1.12 rules): flint & steel lights a fire on the clicked face; it ages, burns out on bare ground after
  ~10-30 s, spreads to planks / logs / leaves / wool / bookshelves / hay / tall grass / flowers and consumes them, sets TNT off, and
  lava lights nearby flammables. Player, animals and monsters burn (1 damage/s, water puts it out, cooked meat drops from burning
  animals, flame overlay on screen, sparks and smoke particles). `/firespread on|off` is the safety switch (like gamerule doFireTick).
- **Sugar cane** (shore worldgen, up to 3 high, grows by random ticks, needs water next to its ground), **sugar, paper, book,
  bookshelf** (drops 3 books).
- **Pumpkins and melons**: pumpkin patches in worldgen, seeds in dungeon chests, 8-stage stems that grow a fruit beside them (one at a
  time, bone meal works), melon slices, **shears carve pumpkins**, **jack o'lantern** (light 15), pumpkin pie.
- **Cake** (7 bites, 2 hunger per slice), **milk** (bucket on a cow; crafting returns the empty buckets), **eggs** (chickens lay them).
- **Animals follow** a player holding their food.
- Under the hood: 36 new block ids (159-194), items 341-350, atlas tiles 219-245, `TexArt37`, `Fire`, `Gen.plantShoreAndPatches`
  (separate random stream - provably additive), 100+ new tests (`test/bc/Tests37.java`).

## Update 3.6 (still in the game) – "the farm update"

- **Farmland needs water.** Farmland with water within 4 blocks (same level or one higher) becomes *wet* (new darker block).
  Crops grow ~40 % faster on wet farmland than in 3.5 and ~40 % slower on dry farmland. Dry farmland with nothing planted
  turns back into dirt after a few minutes; wet farmland and planted farmland never do.
- **Trampling.** Landing on farmland after a fall tramples it with probability (fall distance − 0.5), crops pop off. Walking is safe.
  (Only the player tramples; mobs don't.)
- **Carrots and potatoes** (8 growth stages, ripe drop 1–4, young drop 1; zombies rarely drop them, ~2.5 %), **baked potato**
  (furnace). Right-click plants them on farmland, anywhere else it eats them.
- **Bone meal** (1 bone → 3): +2–5 crop stages; on grass it sprinkles tall grass and a few flowers.
- **Hay bale** (9 wheat ⇄ 1 bale): 80 % less fall damage when landing on it.
- **Hoes** also work on the side faces of grass/dirt (3.5 required the top face).
- **Shears** (2 iron ingots diagonal): shear sheep (1–3 wool, sheep turns pink, wool regrows after 4–8 min); leaves and tall grass
  drop themselves.
- **Breeding.** Feed grown-up animals (pigs carrot/potato, cows & sheep wheat, chickens seeds); two animals in love walk to each
  other and make a half-size baby that grows up in 20 min (feeding speeds it up by 2 min each), drops nothing and cannot be
  sheared; parents rest 5 min. State is saved in `mobs.dat`.

## Update 3.5 (still in the game)

Farming (5 hoe tiers, farmland, seeds from tall grass ≈ 1 in 8, 8-stage wheat that only grows within 32 blocks of the player,
bread), ladders (3 from 7 sticks, climb by walking into them or holding jump, sneak to hold, no fall damage on a ladder),
buckets (pick up / pour water and lava sources), TNT (5 gunpowder + 4 sand, flint & steel, 4 s fuse, radius-4 blast, chain
reactions, creeper blasts ignite it), beds (3 wool over 3 planks, sleep at night unless monsters within 8 blocks, respawn point).

## How well is it verified?

| Area | How | Result |
|---|---|---|
| The recovered source equals the shipped 3.5 jar | `tools/runtests.py equivalence`: 437 fingerprints (terrain of 4 seeds, threaded world + lighting, spawn search, 6000 collision trials, crafting, a 600-tick fluid/sand/TNT simulation, chunk meshing of terrain and of *every* block id, every old texture, every static table) | identical |
| …including the OpenGL classes that cannot run headless | `tools/classcompare.py`: per-method comparison of what the original bytecode calls/reads/writes/loads vs the recompiled classes (718 methods) | 21 differences, all compiler codegen / dead code / a shared lambda |
| Game logic of 3.5 - 3.10 | `test/bc/GameTests.java` + `Tests37` / `38` / `39` / `310` / `TestsFall` / `TestsDisasters`: 41,000+ checks (3.10: see the two rows below; the rest as before): 11,458 checks on a real `Game`/`World`/`Player` without OpenGL: every recipe, hoe/seeds/growth/harvest rates, buckets, flint & steel + TNT blasts/chains/damage, ladders (placement, climbing, falling), beds (placement, sleeping rules, respawn), hydration, drying, trampling (also through real player physics), hay landing, carrots/potatoes/bone meal, shears, breeding, growing up, save/load, atlas tiles; 3.7: cane rules/growth, stems/fruit, carving, cake, milk, eggs, following food, fire (ignite, age, spread, burn out, TNT, lava, water, `/firespread`, burning player and animals), shore/pumpkin worldgen, meshing of every new block | all pass |
| Builds with a real JDK | GitHub Actions `CI` (javac, JDK 21) on every push | green |
| Rendering (Linux, headless OpenGL) | GitHub Actions `Visual`: the real game under Xvfb + Mesa llvmpipe; the ORIGINAL 3.5 jar and the new jar render the same scenes (`ci-results/shots/*.jpg`, left = 3.5, right = new) | 7 scenes (world by day/night, mobs, inventory pages, ...) differ by 0.00 - 0.21 % of the pixels; the shader pack looks identical (its animated water/clouds make 9.6 % of the pixels differ) |
| Terrain generation is unchanged by 3.7 | `runtests.py equivalence` strict pass: with the worldgen extras off (`-Dbc.noextras=true`) `gen`/`world`/`tick`/`mesh terrain` fingerprints are identical to the 3.5 jar | identical |
| 3.7 content on screen | same workflow, committed 3.7 jar: stems in 8 stages + fruit on wet farmland, cake with 0-6 bites, carved pumpkins / jack o'lanterns in 4 facings (lit at night), melons, bookshelves, sugar cane beside water, animated fires on wood / wool / grass, natural shore cane in the normal world, creative inventory page with all new icons | renders correctly (`ci-results/shots/kitchen37.jpg`, `night37.jpg`, `inv-creative-2.jpg`, `day-plain.jpg`) |
| 3.6 content on screen | same workflow: farm field with all growth stages on wet and dry farmland, irrigation ditch, hay bales, baby animals, sheared sheep, creative inventory page with the new icons | renders correctly (`ci-results/shots/farm*.jpg`, `mobs36*.jpg`, `inv-creative-2.jpg`) |
| **The real Windows build** | GitHub Actions `Windows smoke test`: a Windows Server machine installs the 3.5 base zip + the committed update pack exactly like a player (`tools/win_smoke.py`), Mesa software OpenGL next to `javaw.exe`, then starts the real `Blockcraft.exe` (Launch4j stub -> bundled JRE -> jar -> LWJGL Windows natives -> OpenGL) | title screen, world, farm scene, baby animals, inventory and the 3.7 kitchen / fire scene all start, render and exit cleanly with the 3.7 update pack (`ci-results/windows/`) |
| 3.9 skins on screen | `Visual` scenes `skin-front/back/first/hold-a/b/hold-block`, `skin-screen(-def)`, `inv-player(-def)`, `pause-menu`, `title`, `firewall(-night)` | render correctly (`ci-results/shots/`) |
| 3.9 skins on the real Windows build | `Windows smoke test` scenes `skin-net` (the real `Blockcraft.exe` downloads a real account's skin from Mojang over the internet and shows it) and `skin-dialog` (the AWT file dialog opens, the game still exits cleanly with it open) | see `ci-results/windows/` |
| 3.10 falling trees | `TestsFall`: what hangs on a cut log (trunk, branches, leaves, other trees' leaves, held-up logs, bare pillars), the exact 90 degree turn (a bijection that keeps distances), falls in all four directions, blocked ways, cutting higher up, 36 generated trees (oak with branches, birch, spruce, big oaks: every log and leaf accounted for, nothing left hanging), landing on mobs / the player, harvesting, placing logs sideways, meshing of every face, saving mid-fall | all pass |
| 3.10 disasters | `TestsDisasters`: what a tornado takes and what never (every block class), a tornado's life cycle and speed, 900 ticks over a yard of every block class, uprooting, the force field on a real `Player` + a cow, debris flight and landing, quakes (shake by distance, plants, plateau crumbling, flat ground stays flat, trees topple), meteors (straight flight, crater, fires, obsidian, unloaded ground), natural starts and their switches, `/disaster`, saving | all pass |
| 3.10 on screen | `Visual` scenes `treefall-30`, `treefall-65`, `treefall-down`, `logs310`, `tornado-mid`, `tornado-close`, `tornado-night`, `quake`, `meteors`, `meteor-crater`, `options310` | render correctly (`ci-results/shots/`) |
| 3.10 on the real Windows build | `Windows smoke test` (the committed 3.10 update pack installed over the 3.5 base zip, the real `Blockcraft.exe` under Mesa software OpenGL): `title`, `options310` (the new Disasters button), `tornado-close`, `meteors`, `quake` in the first run; `treefall-65`, `treefall-down` (the tree half way down and landed) and `logs310` in the second (the first run's `treefall-65` hung because the Launch4j stub cannot take 888 characters of `JAVA_TOOL_OPTIONS` - a harness limit, `win_smoke.py` now starts such scenes with java.exe directly) | all start, render and exit cleanly (`ci-results/windows/`; the first run's pictures are in the history of that folder) |
| 3.11 faces, doors, LOD, animation maths | `Tests311`: the winding of every block's mesh, a back quad for every plant / crop / flame / water surface, all 16 door blocks x 64 combinations of solid neighbours, the LOD summaries (flat ground, tower, pit, water, trees, cabin pillars), `lod.dat` round trip, the scan of old chunk files, the LOD tile built from a summary, trees validated against trunks, every LOD quad's winding against its face code, tile invalidation and the swap, `Chunk.everMeshed`, the animation functions | all pass |
| 3.11 on screen | `Visual` scenes `under-map(-sh)`, `doors311`, `lod-changes` (+ `-ref`), `hand-*`, `skin-hold-*`, `skin-run`, `mobs-sh`, `mobs-shade-*`, `mobs-torch-*`, `water-*`, `wx-*` | render correctly (`ci-results/shots/`); the cloud changes are subtle in fair weather |
| 3.12 game logic | `Tests312` (and the older suites): 12-bit ids and the chunk file format, portals and the Nether, fortress and stronghold plans (a breadth-first search proves that every room can be reached on foot), the End fight from the first hit to the exit portal, villages and trades, dye / concrete / terracotta recipes, advancements, parrots, ender pearls, soul sand / magma / netherrack fire | 879,000+ checks, 0 failures; the equivalence pass still finds the 3.5 terrain, world and tick fingerprints identical (400 identical, 61 changed on purpose, 127 new) |
| 3.12 on screen | `Visual` scenes `nether-*` (vista, portal, mobs, ghast, three fortress views), `end-*` (island, crystal, dragon, enderman), `stronghold*`, `village-*`, `colors-*`, `adv-*`, `parrot*`, `wx-storm*` | render correctly (`ci-results/shots/`, every picture looked at); the Nether and the End are dark by design |
| 3.12 on the real Windows build | `Windows smoke test` (the committed 3.12 update pack installed over the 3.5 base zip, the real `Blockcraft.exe` under Mesa software OpenGL): `title`, `nether-portal`, `nether-fortress`, `end-dragon-side`, `end-island`, `village-trade`, `adv-screen`, `adv-toast-long` | every scene exits with code 0 and a picture (`ci-results/windows/`; looked at: the fortress corridor with a ghast at its end, the blacksmith's trades, the dragon on the fountain, the toast with the long title) |
| A physical GPU / real hardware | - | never run. Software OpenGL is slow but behaves like the real thing for these features; GPU-driver quirks cannot be ruled out. |

The 3.5 features that the first chat could not test (hoe, seeds, bucket, flint & steel right-clicks; sleeping; ladder climbing) were
exercised through the same code paths the mouse/keyboard use and **work as designed**; no bugs were found in them.

## Known gaps / ideas

- Not implemented: disasters that are not weather / ground / sky (floods, volcanoes, sandstorms, blizzards with damage, wildfires beyond lightning), sound of any kind (so thunder, rain, rumbling and crashing trees are silent), the disasters' numbers (pull, lift, damage, frequency) were set from tests and pictures, not by playing; egg throwing, mobs trampling crops, animals following their parents, boats/minecarts, signs, fishing, enchanting, naturally generated melons (seeds come from dungeon chests), sound of any kind (thunder and rain are silent)…
- 3.12 gaps: no brewing, enchanting, redstone wiring (redstone ore exists), banners, coloured beds (the bed is still red), End cities / gateways / elytra, endermites, shulkers, witches, the Nether's other biomes
  (one cave world), blaze / ghast / dragon sounds (the game has no sound at all). Villagers do not breed, have no beds / jobs / night routine and their trade lists are fixed (no restocking, no levels). The
  dragon, the ghasts and the fortresses were balanced by tests and pictures, never by playing. Eyes of ender need the stronghold ring (3 / 6 / 10 strongholds); `/dim` is the shortcut.
- The sheep model has no separate "sheared" geometry: a sheared sheep is the same box tinted skin-pink with the head tuft hidden.
- Hearts for breeding are plain red particles.

## Delivery

Update packs live in `releases/` (≈ 6 MB: new `Blockcraft.exe`, `blockcraft.jar`, `README.txt`, debug `.bat`; they re-use the
JRE of an existing install). The 54 MB base install is `Blockcraft-1.12-UPDATE3.5-windows.zip` in the repo root. `tools/build.py --full`
produces a complete zip if one is ever wanted. Limits that matter: GitHub accepts files up to 100 MB (25 MB through the web uploader);
the assistant's sandbox can only talk to github.com, npm and PyPI (see `DEVELOPMENT.md`).
