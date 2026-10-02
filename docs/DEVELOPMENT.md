# Blockcraft – developer guide

Blockcraft is a Minecraft-Java-1.12-style voxel sandbox written in **Java 21 + LWJGL 3 (GLFW + OpenGL)**.
Textures, models, terrain generation and physics are all generated in code; the only resource file is `font.bin`.

## Repository layout

| Path | What it is |
|---|---|
| `src/bc/*.java` | The game (package `bc`). `Game` = main loop, input, interaction; `World`/`Chunk`/`Gen`/`Structs` = world; `Blocks`/`Items`/`Recipes` = data tables; `Mobs`/`MobRender`; `Renderer`/`Mesher`/`Lod`/`Shaders`/`ShaderSrc`/`Tex`/`TexExtra`/`Gui` = rendering. |
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

* The version comes from `Game.TITLE_STR` ("... Update 3.7"). Bump it, update `package/README.txt`, build.
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
python3 tools/runtests.py unit          # test/bc/GameTests.java + Tests37.java: ~11,000 checks of the game logic, no GPU needed
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
and **hangs on a huge `JAVA_TOOL_OPTIONS`** (> ~1 KB), so scenes with long `-Dbc.*` values are started with `jre\bin\java.exe -jar Blockcraft.exe`
instead (the exe is a jar with a stub in front). Both visual workflows test the *committed* release zip, not a fresh build.

### Built-in test hooks (`Game.autoTest`, system properties)

`-Dbc.auto=1` starts a world and exits after one screenshot (`~/.blockcraft112/screenshots`). Useful companions:
`bc.seed`, `bc.mode`, `bc.rd` (render distance), `bc.lod`, `bc.shaders=false`, `bc.cam=x,y,z,yaw,pitch`, `bc.time`,
`bc.shot=<frame>`, `bc.set=x,y,z,id;...` (place blocks at frame 70), `bc.summon=pig,baby:cow,sheared:sheep,...` (with `bc.summonDist`, `bc.summonDy` for close-ups),
`bc.inv=creative|craft|player|furnace|chest` with `bc.page`, `bc.tnt=x,y,z`, `bc.kit`, `bc.hold=<id>`, `bc.title=<screen>`,
`bc.fire=off` (freezes fires: they neither spread nor burn out - for screenshots).

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

**A block** (ids 195..255 are free - 3.7 used 159-194; keep `Blocks.COUNT` = last id + 1, a block is ONE BYTE and has no metadata, so
rotations/stages use consecutive ids):
1. `Blocks`: constant + `def(id, name, hardness, opaque, tile)` in the static initialiser; then `kind[id]`
   (0 cube, 1 cross, 2 box, 3 fluid, 4 stair, 5 wall torch, 6 fence, 7 crop), `box`/`coll`, `drop`, `mineClass`/`needTool`,
   `faces(id, top, bottom, side)` for different tiles. Add it to `Blocks.creativeList()` if players should get it.
2. Art: a function returning `int[256]` (ARGB, 16x16) in `TexExtra` and `Tex.blit(tile, pixels)` in `TexExtra.blocks()`.
   The atlas is 16x16 tiles; tiles 0..245 are taken (3.6 uses 203 wet farmland, 204-207 carrots, 208-211 potatoes,
   212-213 hay, 214-218 item icons 336-340; 3.7 uses 219-245, all drawn in `TexArt37`: cane, bookshelf, pumpkin/melon faces,
   4 stem groups, cake, the animated fire tile 235, item icons 236-245). **Next free tile: 246.** Preview art with
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

## Conventions / gotchas

* Blocks are one **byte** per cell (IDs 0..255, `Blocks.COUNT` = 195 so far) and there is **no metadata**: rotated/staged
  blocks use consecutive IDs (beds 133–140, wheat stages 124–131, ladders 119–122, doors 73–81, stairs 93–112 ...).
  Items live in the same id space as blocks below 256; non-block items start at 256.
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
