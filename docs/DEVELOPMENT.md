# Blockcraft – developer guide

Blockcraft is a Minecraft-Java-1.12-style voxel sandbox written in **Java 21 + LWJGL 3 (GLFW + OpenGL)**.
Textures, models, terrain generation and physics are all generated in code; the only resource file is `font.bin`.

## Repository layout

| Path | What it is |
|---|---|
| `src/bc/*.java` | The game (package `bc`). `Game` = main loop, input, interaction; `World`/`Chunk`/`Gen`/`Structs` = world; `Blocks`/`Items`/`Recipes` = data tables; `Mobs`/`MobRender`; `Renderer`/`Mesher`/`Lod`/`Shaders`/`ShaderSrc`/`Tex`/`TexExtra`/`Gui` = rendering; `Weather`/`WeatherFx` = weather (3.8); `Skins`/`PlayerModel` = skins (3.9); `Trees` = falling trees, `Disasters`/`DisasterFx` = tornadoes, earthquakes, meteors (3.10). |
| `res/` | Files placed in the jar root (`font.bin`). |
| `libs/lwjgl-bundle.jar` | LWJGL classes **and** their Windows/Linux natives, extracted unchanged from the 3.5 jar. |
| `launcher/launch4j-stub.bin` | The 69 KB Launch4j launcher. **`Blockcraft.exe` = this stub + the jar appended** (Java opens the exe as a zip). |
| `package/` | `README.txt` and `Blockcraft-debug.bat` that ship next to the exe. |
| `Blockcraft-1.12-UPDATE3.5-windows.zip` | The last build produced before the source moved here. Used as the *base kit*: it holds the bundled Windows JRE (Temurin 21) that `--full` builds re-use. |
| `tools/` | `bootstrap.py` (get a compiler), `build.py`, `test.py`. |
| `test/bc/` | Headless tests in package `bc` (so they can see package-private members): `GameTests` (game logic), `Equivalence` (fingerprints vs the 3.5 jar), `TexSheet` (renders the procedural textures to a PNG for review). `test/equivalence-allowed.txt` lists the fingerprints that are *allowed* to differ from 3.5. |
| `.github/workflows/` | `ci.yml` (javac build + all tests on every push to `arena/**`), `visual.yml` (real game under Xvfb + Mesa software OpenGL, screenshots; trigger by editing `ci/visual-trigger.txt`, its first line selects scenes). |
| `releases/` | Output of `build.py --patch/--full` (the files the owner downloads). |

## Build

```
python3 tools/build.py             # -> build/blockcraft.jar, build/Blockcraft.exe
python3 tools/build.py --patch     # + releases/Blockcraft-UPDATE<ver>-patch.zip  (~6 MB, no JRE inside)
python3 tools/build.py --full      # + releases/Blockcraft-1.12-UPDATE<ver>-windows.zip (~55 MB, with JRE)
```

* The version comes from `Game.TITLE_STR` ("... Update 3.12"). Bump it, update `package/README.txt`, build.
* No JDK needed: `tools/bootstrap.py` fetches a Java runtime (PyPI `jdk4py`) and the Eclipse compiler ECJ (npm
  `@vscjava/java-language-server`) and compiles against the *Windows JRE's own class library*, so you cannot
  accidentally use an API newer than Java 21. If `javac` (JDK 21+) is on PATH it is used instead.
* **Gotcha:** `Blockcraft.exe` has the jar *embedded*. Replacing only `blockcraft.jar` next to an old exe changes
  nothing for people who start the exe. The build therefore always rebuilds the exe (stub + jar).
* Local test run of the real game needs a display + OpenGL 2.1; on a headless box use Xvfb + Mesa llvmpipe, e.g.
  `xvfb-run -a java -Dbc.auto=1 -Dbc.cam=0,90,0,0,20 -Dbc.shot=120 -jar build/blockcraft.jar`
  (`bc.*` system properties are the built-in screenshot/test hooks – see `Game.autoTest()`).

## Tests

```
python3 tools/runtests.py equivalence   # fingerprints terrain/lighting/physics/crafting/simulation/meshing vs the ORIGINAL 3.5 jar
python3 tools/runtests.py unit          # test/bc/GameTests.java + Tests37 .. 312 / TestsFall / TestsDisasters: ~880,000 checks of the game logic, no GPU needed
python3 tools/runtests.py               # both (this is what CI runs)
```

`GameTests` builds a real `Game` + flat `World` + `Player` without OpenGL (`Game.rend` is null; `Game.meshNow()` is the
null-safe seam) and drives the same methods the mouse and keyboard reach: `useSpecial`, `useBlock`, `placeBlock`,
`breakBlock`, `interact`, `useOnMob`, `World.tick`, `Player.tick`, `Mobs.tickMob`, `Mobs.blast`, save/load ...
Remember that every `newGame()` re-seeds its RNGs identically - reuse one game when a test needs many different rolls.

`Equivalence` proves that the code you did *not* touch still behaves exactly like the shipped 3.5 build: it prints
"label = value" fingerprints (4 seeds of terrain, the threaded world+lighting pipeline, spawn search, 6000 collision
trials, crafting lookups, a 600-tick fluid/sand/TNT simulation, chunk meshing of terrain and of every block id,
every procedural texture, every static table) for the old jar and for the freshly built classes and compares them
key by key. A value that changed on purpose must be listed in `test/equivalence-allowed.txt`; brand-new
fingerprints are always accepted; anything else is a regression.

`runtests.py equivalence` also runs a **strict pass**: with `-Dbc.noextras=true` (switches off the one worldgen addition since 3.5,
`Gen.plantShoreAndPatches` = shore sugar cane + pumpkin patches) the `gen`, `world`, `tick` and `mesh terrain` fingerprints must be
*identical* to the 3.5 jar - proof that the extras are purely additive and that nothing else in terrain generation changed. New
worldgen features must keep working that way: use their own `Random` stream (never the shared chunk RNG) and put the whole pass behind
`Gen.extras`. `Tests37.worldgen` additionally checks cell by cell that only cane / pumpkins were added, only into air.

**What is NOT covered headless:** everything that calls OpenGL (`Renderer`, `Shaders`, `Gui` drawing, `MobRender`
drawing, `Lod` upload). That is what the *Visual* workflow is for.

### Visual workflow (headless OpenGL on GitHub)

Edit `ci/visual-trigger.txt` (first line = comma separated scene names from `tools/shots.py`, or `all`; an optional second line
`jar=build` tests the freshly built jar instead of the newest committed `releases/*-patch.zip`) and push:
GitHub runs the real game under Xvfb with Mesa llvmpipe, the scenes use the game's own `-Dbc.*` hooks (below), and
side-by-side JPEGs + `report.md` (with timings) are committed to `ci-results/shots/` on the same branch.
Scenes marked *only new* run with the new jar only; the others also run the original 3.5 jar (`ref`) for an A/B picture.
Logs cannot be downloaded in the assistant sandbox, so workflows copy what matters into *annotations*
(`tools/ci_annotate.py`) which are readable through `gh api repos/<o>/<r>/check-runs/<job id>/annotations`.

### Windows smoke workflow

`windows-smoke.yml` (trigger: edit `ci/windows-trigger.txt`) takes the committed `releases/*-patch.zip`, installs it over the 3.5 base
zip like a player, adds Mesa's `opengl32.dll`/`libgallium_wgl.dll` next to the bundled `javaw.exe` (GitHub's Windows machines have no GPU)
and runs the real `Blockcraft.exe`; screenshots and logs land in `ci-results/windows/`. Gotcha: the Launch4j stub probes `java -version`
and **hangs on a long `JAVA_TOOL_OPTIONS`** (888 characters already did; `win_smoke.py` switches at 600), so scenes with long `-Dbc.*` values are started with `jre\bin\java.exe -jar Blockcraft.exe`
instead (the exe is a jar with a stub in front). Both visual workflows test the *committed* release zip, not a fresh build.

### Built-in test hooks (`Game.autoTest`, system properties)

`-Dbc.auto=1` starts a world and exits after one screenshot (`~/.blockcraft112/screenshots`). Useful companions:
`bc.seed`, `bc.mode`, `bc.rd` (render distance), `bc.lod`, `bc.shaders=false`, `bc.cam=x,y,z,yaw,pitch`, `bc.time`,
`bc.shot=<frame>`, `bc.set=x,y,z,id;...` (place blocks at frame 70), `bc.summon=pig,baby:cow,sheared:sheep,...` (with `bc.summonDist`, `bc.summonDy` for close-ups),
`bc.inv=creative|craft|player|furnace|chest` with `bc.page`, `bc.tnt=x,y,z`, `bc.kit`, `bc.hold=<id>`, `bc.title=<screen>`,
`bc.fire=off` (freezes fires: they neither spread nor burn out - for screenshots).
Weather hooks (3.8): `bc.weather=calm|clear|rain|thunder|cell:dx,dz,radius` (a storm cell placed relative to the player; the sky is otherwise calm),
`bc.wtime=<weather ticks>`, `bc.lightning=off` (no random lightning), `bc.bolt=dx,dz` (a fresh bolt every frame near the shot, so the screenshot
catches the first stroke), `bc.biome=<id>` (all loaded chunks get this biome, e.g. 5 = ice plains = snow).

Skin hooks (3.9): `bc.view=0|1|2` (first person / behind / in front), `bc.skin=demo|<file in the skins folder>` (`demo` = a built-in
slim-armed test skin), `bc.emptyhand=1` (first-person arm), `bc.hold=<id>` also feeds the third-person hand, `bc.bodyyaw=<deg>` (turn the body
away from the camera to see the model from the side), `bc.mouse=x,y` (framebuffer pixels; the inventory model looks at it), `bc.pause=1`
(open the pause menu, then screenshot), `bc.title=SKINS` (the Skins screen), `bc.skinuser=<name>` (import a real account's skin over the
network before the shot - the Windows smoke machine has internet), `bc.skinbrowse=1` (open the file dialog).

Update 3.10 hooks: `bc.fill=x0,y0,z0,x1,y1,z1,id;...` (fill boxes at frame 68, before `bc.set`), `bc.fell=x,y,z,dx,dz` (frame 80: cut the log at x,y,z - the tree above falls
towards dx,dz) with `bc.fellangle=<degrees>` (freeze every falling tree at that angle: a picture of the fall half way), `bc.disaster=tornado:dx,dz[,size[,speed]]`
`| quake:dx,dz[,magnitude[,life ticks]] | meteor:dx,dz[,flight ticks]` (several separated by `;`; frame 75; positions relative to the camera; a tornado is fully grown
and stands still unless a speed in blocks per tick is given), `bc.dfreeze=<ticks>` (everything the disasters do stops that many ticks after it was started).

## How the source was recovered (2026-10-02)

The 3.5 source did not survive the move away from the first chat, so it was decompiled from `blockcraft.jar`
(57 classes in package `bc`, Java 11 bytecode; everything else in the jar is LWJGL) with Vineflower
(npm `@run-slicer/vf`), then made to compile again:

* 22 generics errors fixed by hand (raw `ArrayList` locals), `Recipes.find` rewritten (decompiler mangled its loops).
* **38 `byte`/`short` locals changed to `int`.** The decompiler typed loop counters like `for (byte i = 0; i < n; i += 6)`
  from the constant 0; with a byte that overflows at 127 (infinite loop in `Gen.findSpawn`, wrong results in `Phys.move`).
  If you ever decompile again, check for this first.
* **Local variable names are lost** (`var12`, ...): the jar had no debug info. Fields, methods and classes kept their names.
  Rename locals when you touch a method; builds now include `-g` so future decompiles keep names.

## How to add content (what the code expects)

**A block** (ids 226..255 are free - 3.7 used 159-194, 3.9 195-219, 3.10 220-225; keep `Blocks.COUNT` = last id + 1, a block is ONE BYTE and has no metadata, so
rotations/stages use consecutive ids):
1. `Blocks`: constant + `def(id, name, hardness, opaque, tile)` in the static initialiser; then `kind[id]`
   (0 cube, 1 cross, 2 box, 3 fluid, 4 stair, 5 wall torch, 6 fence, 7 crop), `box`/`coll`, `drop`, `mineClass`/`needTool`,
   `faces(id, top, bottom, side)` for different tiles. Add it to `Blocks.creativeList()` if players should get it.
2. Art: a function returning `int[256]` (ARGB, 16x16) in `TexExtra` and `Tex.blit(tile, pixels)` in `TexExtra.blocks()`.
   The atlas is 16x16 tiles; tiles 0..245 are taken (3.6 uses 203 wet farmland, 204-207 carrots, 208-211 potatoes,
   212-213 hay, 214-218 item icons 336-340; 3.7 uses 219-245, all drawn in `TexArt37`: cane, bookshelf, pumpkin/melon faces,
   4 stem groups, cake, the animated fire tile 235, item icons 236-245). **Next free tile: 249** (3.10 uses 246-248 for the turned bark of logs on their side, `TexArt310`). Preview art with
   `bc.TexSheet out.png` (add `37` as 2nd argument for only the 3.7 art).
3. Behaviour: `World.blockUpdate` (reacts to neighbour changes), `World.tickCrops` (random ticks within 32 blocks of the
   player), `Game.useBlock` (right-click), `Game.placeBlock`, `Game.breakBlock` / `Game.blockDrops`.

**An item** (ids 351..399 free - 3.7 used 341-350; the arrays in `Items` are sized 400): `Items.def(id, name, tileOffset)` (the atlas tile is
`65 + tileOffset`), `maxStack[]`, `maxDmg[]`, `food(id, hunger, saturation)`; icon in `TexExtra.newSprite` (range hook in
`TexExtra.sprite`) - the registration loop in `TexExtra.blocks()` covers ids 256..399; recipes in `Recipes`
(`shaped`, `shapeless`, `smelt`); right-click behaviour in `Game.useSpecial` (runs *before* eating and placing).

**A mob** type: parallel arrays in `Mobs` (`NAME`, `HP`, `W`, `H`, `SPD`), AI in `Mobs.tickMob`, model in
`MobRender.build`/`drawMob`, drops in `Mobs.kill`, persistence in `mobs.dat` (space separated; append fields at the end
so old lines still load).

## Weather (Update 3.8)

`Weather.java` is the model, `WeatherFx.java` everything that draws it, `Shaders`/`ShaderSrc.CLOUD_FS` the raymarched clouds.

* **The weather is a field over the map, not a global state**: `Weather.Snap.s(x, z)` = `0.62 * mapA + 0.38 * mapB + climate (+ forcing, storm cells)`,
  where the two tileable noise maps (`Weather.MAP_A` period 9000 blocks, `MAP_B` 4500) drift with the wind at slightly different speeds. Thresholds
  (`C0..T1`, quantiles of the field, so ~30 % clear, ~16 % rain, ~6 % thunder) turn `s` into cloud cover, rain and a thunderstorm; `core` is the height
  of the thunderhead. A `Snap` is immutable (thread-safe); the mesher thread and the GPU use the same numbers as the game rules.
* **CPU == GPU**: the maps are uploaded as a texture (`Shaders.makeWeatherNoise`), the CPU samples the same bytes with the same bilinear/repeat maths,
  and every noise period divides 9000, so the shader can work on coordinates reduced modulo 9000 (float precision). `Tests38.gpuParity` proves both
  claims. If you change the formula in `Weather.Snap.s`, change `ShaderSrc.CLOUD_FS` (`wfield`, `weather`, `thickOf`, `dens`) and `WeatherFx.density`
  /`thickness` together - the tests will tell you if they drift apart.
* **No-shader path** = blocky clouds: `WeatherFx.build` (worker thread, every ~2.5 s or when the camera moved 40 blocks) makes a fine level (16-block
  columns out to 1120 blocks) and a coarse level (48-block columns out to 3.6 km) in the drifting frame of weather layer A (so the mesh only needs a
  translation per frame), plus the rain curtains (3 crossing planes per grid point, a veil texture so they still show from afar). Drawn with a depth
  pre-pass so overlapping translucent faces blend once. **Shader path** = the raymarch; only the curtains, the rain/snow around the player and the bolts
  are geometry (drawn after the cloud composite).
* The cloud shader marches only as tall a slab as the storms in range need (`Weather.scanSky` -> `WeatherFx.slabLo/slabHi`); fair weather uses the thin
  slab, so the fair-weather look equals 3.5's.
* **Game rules** live in `Weather` (`rainingAt`, `thunderingAt`, `strike`, `precipKind`) and are called from `Fire.update` (rain puts fires out),
  `World.hydrated` (rain waters farmland), `Mobs.tickMob` (no sun-burn in rain), `Game.sleepAt` (thunder allows sleeping; sleeping clears the sky).
  Biomes decide rain / snow (ice plains and mountains above y=112) / dry (desert).
* Weather is saved in `level.properties` (`wclock`, `wenabled`, `wforce`, `wcells`, `wcalm`). New worlds and worlds from before 3.8 start *calm*
  (fair-weather clouds only) for 5 minutes (`Weather.startFair`).
* There is no sound in the game, so thunder and rain are visual only.

## Skins (Update 3.9)

* `Skins` has no OpenGL in it (so `Tests39` runs it headless): listing / loading / importing files, legacy and HD conversion, slim detection,
  Mojang JSON extraction (`profileId`, `textureInfo`, `profileName`, `uuidOf` - plain regexes, no JSON library), the code-drawn default skin.
  `PlayerModel` is the OpenGL side: the UV layout of a 64x64 skin (`faceRects`), the body with its second layer (inflated boxes), poses.
* The skin texture is uploaded again whenever the pixel array changes identity (`Skins.pixels` caches per file stamp).
* Windows specifics that CI cannot click through: dropping a file on the window, the dialog being visible. Keep `browseSkin` leaving
  fullscreen and disposing the `FileDialog` - an undisposed AWT dialog keeps the JVM alive after the window closes (hence `System.exit(0)`
  in `Main`). `java.awt.headless` is set to `false` on Windows only.
* `Blocks.COUNT` is 226; ids 226..255 are free. 3.9 added 195..214 upside-down stairs (`Blocks.STAIR_TOP0`, material * 4 + facing; use `Blocks.stairFacing`) and 215..219 top slabs
  (`SLAB_TOP0`); 3.10 added 220..225 logs on their side (`LOG_X0` 220..222 along x, `LOG_Z0` 223..225 along z; oak / birch / spruce).

## Falling trees (Update 3.10)

`Trees.java`, one instance per world (`World.trees`, ticked from `World.tick`, landed by `World.saveAll`). `Game.breakBlock` calls `Trees.fell` after a log
(9..11) was broken; `Disasters.uproot` (tornado, earthquake) uses `Trees.collect` / `start` directly.

* **What falls** (`collect`): the logs connected (6 neighbours) to the cut cell, going up, that are not held up by anything else (a solid non-leaf block under one of them
  = nothing falls) - the *trunk* is the part in the cut's own column, the rest are *branches* - plus the leaves within 4 steps (26 neighbours: the generated crowns have
  leaves that only touch at an edge) of them that no other tree's log is closer to. Fewer than 4 leaves = not a tree (a log pillar or a cabin). Caps: 160 logs, 700 leaves.
* **Where** (`checkDirection`): the directions in the order of how well they point away from the player (`directionOrder`); a direction is free when the cells the trunk's
  log centres pass at 15 / 30 ... 75 degrees and the cells it lies in are free (air, plants, leaves; not solid blocks, not water) - only the trunk is checked, branches and
  leaves are not rigid: what does not fit when it lands is pushed up (at most 2) or snaps off (a branch drops as a log item, a leaf like a broken leaf). The trunk then drops
  until it rests (cut higher up). `Trees.rotated` is the exact 90 degree turn that puts every block in a cell (`TestsFall.geometry` proves it is a bijection).
* **How it looks**: the blocks are taken out of the world when the fall starts and live in `Trees.Fall`; `Renderer.drawFallingTrees` draws them as one rigid piece
  (`glRotatef(angle, dz, 0, -dx)` about the cut cell, the angle follows `90 * (t / T)^2`, T = 20..44 ticks by height) and `emitCube` draws a cube in immediate mode.
  Landing: side logs (`Blocks.sideLog(id, axis)`), `squash` hurts and lifts mobs / the player (7 half hearts), particles, camera shake (`Game.addShake`).
* Logs on their side are ids 220..225 (tiles: rings on the two ends, `TexArt310` bark transposed on the other faces so the grain runs along the log). `Game.placeBlock`
  turns a log put against a side face into one (clicked axis); `Blocks.drop` / `pickItem` give the upright log; `Fire` treats them like logs.
* Tests: `TestsFall` (detection, geometry, falls in all directions, blocked ways, cutting higher, neighbours, 36 generated trees, landing, harvesting, placing, meshing, saving).

## Natural disasters (Update 3.10)

`Disasters.java` (rules, one instance per world: `World.disasters`, ticked from `World.tick`), `DisasterFx.java` (drawing), `Options.disasters` + `Disasters.enabled`
(`/disaster on|off`, saved as `disasters`, `dclock`, `dquake`, `dtornado`, `dshower` in level.properties). Nothing here uses OpenGL except `DisasterFx`.

* **Tornado** (`Tornado`, up to 2): the funnel forms over 12 s (`strength` 0..1), moves 0.075-0.12 blocks per tick with a wandering heading and ropes out after 2-4 minutes.
  `pull` is the force field (swirl, inward pull, lift that fades 14 blocks above the ground; `reach` grows with height); `tornadoEntities` applies it to the player (not when
  flying), mobs and items and hurts what is in the core; `tornadoTrees` uproots trees within 5.5 blocks (every 4 ticks, one tree per look, before the leaves are gone);
  `tornadoBlocks` picks random columns in a 7-block radius and lifts what `fragility(id)` allows (terrain, stone, chests, furnaces, TNT, fire, hard blocks never) as `Debris`.
  `Debris` circles the funnel (`tickDebris`), is flung off along the tangent, hits entities (3 damage) and lands as a `World.Falling` block (60 %) or splinters.
* **Earthquake** (`Quake`, one at a time): `tickQuake` shakes the camera by distance (`Game.addShake`), and every tick picks columns in the radius (14 + 7 * magnitude) for
  `shakeColumn`: topple a tree (`uproot`), shake loose plants / torches / ladders / glass, or let an unstable block of `crumbles(id)` terrain fall (a block with air under it falls
  where it is, one at a cliff edge slides off into the open column next to it as a `World.Falling`).
* **Meteor** (`Meteor`): a straight flight in steps of < 1 block (a thin roof cannot be flown through), `impact` = `Mobs.blast` (power 4-6) + fires (`Fire.ignite`) + 1-3 obsidian blocks
  + shake. A shower is `showerLeft` meteors launched 3-10 s apart at loaded spots 40-130 blocks from the player.
* **Natural starts** (`tickNatural`, every 2 s, only while `active()` = world switch and Options): quake 20-45 minutes apart (the first one 10-30 minutes in), a tornado when a point 90-180
  blocks away has `Snap.storm > 0.3` (needs weather on; 17-33 minutes apart, the first one after 7.5-15 minutes), a meteor shower at night 30-70 minutes apart. All use the world's own
  `Random` (seeded from the world seed), so tests are deterministic. `GameTests.newGame` switches disasters off; `TestsDisasters.game` switches them on.
* **Drawing** (`DisasterFx`): `drawDebris` with the entities; `drawSky` after the clouds (before the rain curtains): two translucent shells of rings (inner dense, outer a soft veil turning
  the other way), a dust skirt, a wall cloud of stacked soft discs, tinted by `WeatherFx.cloudTint` (night / sunset / lightning flashes, never darker than 0.27); meteors are additive
  ribbons with three soft glow discs. Camera shake is in `Renderer.renderWorld` (position + a little rotation, `Game.shake` fades by itself).
* Tests: `TestsDisasters` (rule tables, ground height, the life of a tornado, what it tears and what it leaves, uprooting, the force field with a real `Player` and a cow, debris flight and landing,
  quakes, meteors, natural starts and their switches, commands, saving).

## Update 3.11 notes

* **Culling.** `Renderer.renderWorld` and `Lod.render` enable `GL_CULL_FACE` only around their chunk / tile loops (everything else keeps the old state: weather, sky, entities
  draw double-sided). Every mesh must wind counter-clockwise seen from outside: cubes / boxes / stairs / fences use `Mesher.FC`, the LOD uses `Lod.quadN` (it orders the corners from the
  face's intended normal). New geometry that is seen from both sides needs a back quad (`Mesher.emitBack`). `Tests311.culling` fails if a block's face points into the block.
* **LOD summaries** (`LodMap.Sum`, 256 columns per chunk): `top` (plane of the surface = y of the top block + 1), `depth` (water over the bed), `trunk` (upright logs with leaves above), `rgb` and `side`
  colours. Made by `World.saveChunk`, `World.lodRefresh` (2 s after the last `setBlock`) and the background scan of chunk files without a summary. `Lod.build` reads a summary where it exists.
  A summary is NOT made for unchanged chunks (they equal the generator). Changing how the generator draws the LOD does not change summaries; changing `LodMap.summarize` needs `MAGIC` bumped
  (old files would be read with the wrong meaning).
* **Shader programs**: `pWorld` (terrain), `pWater` (the same source with the water branch replaced: `ShaderSrc.WATER_DECL` + `WATER_MAIN` spliced in by `deriveWater`), `pEntity`, `pLod`, `pCloud` ...
  A program that fails to compile only costs its own effect (`waterOk`, `entityOk`, `lodOk`, `cloudsOk`). Run `python3 tools/glsl_check.py` after touching any shader string.
* **Test hooks** (3.11): `bc.tp=frame:x,y,z`, `bc.lodage`, `bc.handswing`, `bc.handeat`, `bc.graze`, `bc.walkphase=phase,amp`, `bc.posewalk=phase,amp,sprint`, `bc.waterq`, `bc.nocull`, `bc.legacymesh`.

## Update 3.12 notes (dimensions, villages, colour)

* **Block ids are 12 bits wide.** `Chunk.blocks` is a `short[]`. Block ids are 0..255 (the old ones) and 1024..4095 (new: Nether 1024-1032, End 1040-1044, colour blocks from `Blocks.CONCRETE0` on);
  256..1023 are items (new items start at 352: `Items.defTile(id, name, tile)`, icon tile `TexArt312.ITEM0 + id - 352`). `Blocks.isBlockId` tells them apart. **A leftover `& 255` silently
  truncates a new id** - read blocks with `Chunk.get` / `World.getBlock` (the equivalence test had to be fixed for this). Chunk files keep the old byte array (the low 8 bits of every id) and
  append the high 4 bits behind the magic `BCX1` only when some id is above 255, so old chunk files read as before and an old game still reads the blocks it knows.
  The atlas is 32 x 32 tiles (`Tex.PX`): 256-269 blocks, 272+ items, colour tiles from 400; animated: water 23, lava 24, fire 235, portal 263; tile 64 is the white particle.
* **Dimensions.** `Dim` has the ids (Overworld 0, Nether -1, End 1), the ambient light (`Dim.ambient`: Nether 0.27, End 0.46) and its tint (`Dim.ambientTint`, used by `Renderer.updateLightmap`: the End is neutral,
  the rest warm). One `World(seed, dir, dim)` per dimension (the Overworld lives in the world folder, the others in `DIM-1` / `DIM1` inside it), `Game.switchDimension(dim, x, [y], z, kind)` and
  `Game.finishArrival` (kind 0 = come out of a portal, 1 = spawn, 2 = exact position). Dimensions other than the Overworld have no sky, weather, LOD or disasters. Villagers and golems are kept
  forever (`Mobs312.keepsForever`), everything else far away goes as before.
* **Nether**: `NetherGen` (a function of seed and position only: a chunk can always be generated again and still matches saved neighbours), `Portals` (frame detection, the 80-tick wait, 1:8 coordinates,
  `Portals.arrive` finds or builds the exit portal; portal blocks 1030 / 1031 are drawn as mesher kind 8 = a film in the blended buffer), fortresses (`NetherFortress`, `REG` 256, `CHANCE` 70 %).
* **Plans** (`BoxPlan`): a structure is a list of boxes in three layers - shell, then the air cut out of it (so crossing pieces always leave their passages open), then decor (`soft` decor never fills a passage
  another piece cut) - plus pillars that grow down to the rock. `NetherFortress` and `Stronghold` extend it. Rules that took effort to find: rooms never overlap (`roomFree`), a plan without a throne room
  / a chest is thrown away and drawn again (up to 12 attempts), and `Tests312` walks every plan with a breadth-first search (`Reach`) to prove that every room can be reached - keep those tests.
* **Everything new in worldgen is behind `Gen.extras`** (a static: restore it in `finally` in tests). With `-Dbc.noextras=true` the terrain is still bit-identical to 3.5 (the strict equivalence pass).
  Strongholds (3 / 6 / 10 in rings), emerald ore, clay and the village upgrades only exist with extras.
* **The End**: `EndGen` is the same island for every seed (top y 64, platform at (62, 63, 0), ten pillars on a ring of radius 42), `EndFight` holds the state of the fight (`fight.properties` in the End's folder),
  `Dragon` is a state machine (CIRCLE, STRAFE, LAND, PERCH, TAKEOFF; 200 ticks of dying) that simply sets its position, `EndPortals` has the frames, the eyes and the portal. Below y -12 the void kills.
* **Mobs 8..17**: pigman, ghast, blaze, magma cube, enderman, dragon, end crystal (`persistent`), villager, iron golem, parrot. Their minds are in `Mobs312.think`, their boxes in `MobModels312`, their drops in
  `Mobs312.kill`. `Mobs312.Fireball` is the one projectile: flags `big` (ghast), `dragon` (breath) and `pearl` (a thrown ender pearl: gravity 0.03, drag 0.99, lands the player at the last free position).
* **Colour** (`Color312`, by another run of the project): 16 colours x (wool, concrete, concrete powder, stained and glazed terracotta, stained glass) as consecutive ids; `dyeRecipe` is hooked into `Recipes.find`
  (dye + wool, 8 around a dye, 4 sand + 4 gravel + a dye), `wet` hardens powder next to water. **Advancements** (`Adv312`, 34 of them): polled twice a second plus `Game.adv.award` calls; screen `Game.S.ADV` (key L).
* **GL traps**: the blended pass must switch `GL_TEXTURE_2D` back on (`drawSelection` turns it off) or the portal film is flat. The End's ambient light is low, so crystals and the dragon are drawn bright / full-bright.
  The lightmap's ambient colour is warm (`Tex.ambG` / `Tex.ambB`, 0.80 / 0.78 of red) - in the End that made end stone brown, hence the neutral tint there.
* **Test traps**: `World.updateChunks` only loads chunks within a radius and unloads the ones outside the last centre (use a radius <= 3 and reload the centre before reading blocks); the flat test world covers chunks
  -2..2; an unseeded `Mobs.rnd` makes tests flaky (`g.mobs.rnd.setSeed(..)`; `World.rnd` is private); in End tests the world folder is the test folder itself (the game uses `<root>/DIM1`); water must hurt an enderman
  before it teleports; the dragon's circle can reach radius ~62 after a swoop.
* **Test hooks** (3.12): `bc.dim=-1|1` (`bc.cam` is then a position in that dimension), `bc.dragon=perch`, `bc.inv=trade` with `bc.prof`, `bc.summon=type,type..` (`bc.summonDist`, `bc.summonDy`), `bc.set=x,y,z,id;..`,
  `bc.adv=screen|toast` (`bc.advdone`, `bc.advtab`), `bc.parrot=land|shoulder`. All scenes are in `tools/shots.py`; yaw 0 looks towards +z, -90 east, 180 -z, a positive pitch looks down.
* Tests: `Tests312` (foundation, nether, netherMobs, fortress, stronghold, end, villages, colors, parrots, advancements, pearls, netherRules).

## Conventions / gotchas

* Blocks are 12 bits per cell since 3.12 (`short[]`; IDs 0..255 and 1024..4095, see the 3.12 notes) and there is **no metadata**: rotated/staged
  blocks use consecutive IDs (beds 133–140, wheat stages 124–131, ladders 119–122, doors 73–81, stairs 93–112 ...).
  Items live in the same id space as blocks below 256; non-block items start at 256 (new ones at 352).
* **Fire** (`Fire.java`, one block id 194): blocks have no metadata, so the fire's age lives in `World.fire.cells`
  (position -> {age, next update tick}); `World.setBlock` keeps it in sync and `World.updateChunks` refills it from chunk data
  (`Fire.scanChunk`), so ages restart after a reload. The rules are Minecraft 1.12's `BlockFire` (see the class comment);
  `World.fire.spread = false` (`/firespread off`) freezes everything. Other state that cannot live in a block (stem fruit
  "attachment", cane age) is deliberately not modelled: stems always count as fully grown at stage 7, cane grows by random ticks.
* World ticks run at 20 per second from `Game.update`; `World.tick()` does fluids, block updates, falling blocks/TNT,
  crops (only within 32 blocks of the player), items, furnaces.
* Save data lives in `%USERPROFILE%\.blockcraft112` (worlds under `saves\`, options, `latest.log`, `crash.log`).
* `World.game` may be `null` in tests; code that reaches the player/mobs must tolerate that or the test must provide a `Game`.

## Environment notes (assistant sandbox, 2026-10-02)

Reachable: github.com, api.github.com, npm, PyPI. Not reachable: Maven Central, Adoptium, apt mirrors, GitHub Releases
asset hosts, file-sharing sites. No JDK, no display, no GPU, no Wine – so the game itself cannot be started in the
sandbox; GitHub Actions (see `.github/workflows`) is used for real-JDK builds and headless OpenGL screenshot runs.
