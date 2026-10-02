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
| `test/bc/` | Headless tests (package `bc`, so they can see package-private members). |
| `releases/` | Output of `build.py --patch/--full` (the files the owner downloads). |

## Build

```
python3 tools/build.py             # -> build/blockcraft.jar, build/Blockcraft.exe
python3 tools/build.py --patch     # + releases/Blockcraft-UPDATE<ver>-patch.zip  (~6 MB, no JRE inside)
python3 tools/build.py --full      # + releases/Blockcraft-1.12-UPDATE<ver>-windows.zip (~55 MB, with JRE)
```

* The version comes from `Game.TITLE_STR` ("... Update 3.6"). Bump it, update `package/README.txt`, build.
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
python3 tools/runtests.py equivalence   # fingerprints terrain/lighting/physics/crafting/simulation vs the ORIGINAL 3.5 jar
python3 tools/runtests.py unit          # game-logic tests (test/bc/GameTests.java)
```

`Equivalence` was how the recovered source was proven faithful: 224 fingerprint lines (4 seeds of terrain,
the threaded world+lighting pipeline, spawn search, 6000 collision trials, crafting lookups, a 600-tick
fluid/sand/TNT simulation, every static table) are identical between the original jar and the rebuilt classes.
Features that change behaviour on purpose will (correctly) change some fingerprints; refresh the expectation
consciously, don't just ignore a diff.

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

## Conventions / gotchas

* Blocks are one **byte** per cell (IDs 0..255, `Blocks.COUNT` = 141 so far) and there is **no metadata**: rotated/staged
  blocks use consecutive IDs (beds 133–140, wheat stages 124–131, ladders 119–122, doors 73–81, stairs 93–112 ...).
  Items live in the same id space as blocks below 256; non-block items start at 256.
* World ticks run at 20 per second from `Game.update`; `World.tick()` does fluids, block updates, falling blocks/TNT,
  crops (only within 32 blocks of the player), items, furnaces.
* Save data lives in `%USERPROFILE%\.blockcraft112` (worlds under `saves\`, options, `latest.log`, `crash.log`).
* `World.game` may be `null` in tests; code that reaches the player/mobs must tolerate that or the test must provide a `Game`.

## Environment notes (assistant sandbox, 2026-10-02)

Reachable: github.com, api.github.com, npm, PyPI. Not reachable: Maven Central, Adoptium, apt mirrors, GitHub Releases
asset hosts, file-sharing sites. No JDK, no display, no GPU, no Wine – so the game itself cannot be started in the
sandbox; GitHub Actions (see `.github/workflows`) is used for real-JDK builds and headless OpenGL screenshot runs.
