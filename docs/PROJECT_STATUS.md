# Blockcraft – project status

_Last updated: 2026-10-03 (Update 3.9)._

## Roadmap: the owner's "To do.txt" (worked through step by step, in this order)

| # | Item | State |
|---|---|---|
| 1 | Weather system with volumetric clouds, storms and rain visible in the distance, working without shaders | **done in Update 3.8** (weather field, clouds in both render paths, rain / snow / lightning, `/weather`) |
| 2 | Fix LODs and adapt them for the weather system | weather part **done in 3.8** (the LOD follows the weather's fog / sun / brightness); 3.9 makes the fog radial so the world's edge is hidden from every angle; inspected in day / night / shader / top-down scenes (`lod-*`) - no further defect found without a concrete report |
| 3 | Skins (easy import of Minecraft skins) | **done in Update 3.9** (drag & drop, file dialog, username download, clipboard; legacy / HD skins; slim arms; F5 camera; inventory preview) |
| 4 | Smooth animations | planned |
| 5 | Natural disasters | planned (builds on the weather system: lightning fires, storms) |
| 6 | Trees fall over when chopped; the fallen trunk can be harvested further | planned (needs sideways log blocks) |
| 7 | Slabs, stairs etc. on the top half of a block / upside down | planned (needs `Hit` fractions + 25 new block ids) |
| 8 | Make creepers ignitable | **done in Update 3.9** (flint and steel) |
| 9 | Fire drawn like in Minecraft when attached to a block | **done in Update 3.9** (`Mesher.fire`: wall / ceiling / corner flames) |
| 10 | Hide interior surfaces (performance) | planned |
| 11 | Fix mobs not being able to jump up a block | **done in Update 3.9** (`Mobs.JUMP`) |

## What it is

Blockcraft is an original Minecraft-Java-1.12-style block sandbox for Windows ("an original clone": no Mojang code or
assets, everything - textures, world generator, physics - is written from scratch). Java 21 + LWJGL 3 (OpenGL);
shipped as a zip with a bundled Windows JRE and `Blockcraft.exe` (a Launch4j wrapper around the jar).
Details for developers: [`DEVELOPMENT.md`](DEVELOPMENT.md).

## Current state: Update 3.9 – "the skins update"

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
| Game logic of 3.5 - 3.9 | `test/bc/GameTests.java` + `Tests37.java`: 11,458 checks on a real `Game`/`World`/`Player` without OpenGL: every recipe, hoe/seeds/growth/harvest rates, buckets, flint & steel + TNT blasts/chains/damage, ladders (placement, climbing, falling), beds (placement, sleeping rules, respawn), hydration, drying, trampling (also through real player physics), hay landing, carrots/potatoes/bone meal, shears, breeding, growing up, save/load, atlas tiles; 3.7: cane rules/growth, stems/fruit, carving, cake, milk, eggs, following food, fire (ignite, age, spread, burn out, TNT, lava, water, `/firespread`, burning player and animals), shore/pumpkin worldgen, meshing of every new block | all pass |
| Builds with a real JDK | GitHub Actions `CI` (javac, JDK 21) on every push | green |
| Rendering (Linux, headless OpenGL) | GitHub Actions `Visual`: the real game under Xvfb + Mesa llvmpipe; the ORIGINAL 3.5 jar and the new jar render the same scenes (`ci-results/shots/*.jpg`, left = 3.5, right = new) | 7 scenes (world by day/night, mobs, inventory pages, ...) differ by 0.00 - 0.21 % of the pixels; the shader pack looks identical (its animated water/clouds make 9.6 % of the pixels differ) |
| Terrain generation is unchanged by 3.7 | `runtests.py equivalence` strict pass: with the worldgen extras off (`-Dbc.noextras=true`) `gen`/`world`/`tick`/`mesh terrain` fingerprints are identical to the 3.5 jar | identical |
| 3.7 content on screen | same workflow, committed 3.7 jar: stems in 8 stages + fruit on wet farmland, cake with 0-6 bites, carved pumpkins / jack o'lanterns in 4 facings (lit at night), melons, bookshelves, sugar cane beside water, animated fires on wood / wool / grass, natural shore cane in the normal world, creative inventory page with all new icons | renders correctly (`ci-results/shots/kitchen37.jpg`, `night37.jpg`, `inv-creative-2.jpg`, `day-plain.jpg`) |
| 3.6 content on screen | same workflow: farm field with all growth stages on wet and dry farmland, irrigation ditch, hay bales, baby animals, sheared sheep, creative inventory page with the new icons | renders correctly (`ci-results/shots/farm*.jpg`, `mobs36*.jpg`, `inv-creative-2.jpg`) |
| **The real Windows build** | GitHub Actions `Windows smoke test`: a Windows Server machine installs the 3.5 base zip + the committed update pack exactly like a player (`tools/win_smoke.py`), Mesa software OpenGL next to `javaw.exe`, then starts the real `Blockcraft.exe` (Launch4j stub -> bundled JRE -> jar -> LWJGL Windows natives -> OpenGL) | title screen, world, farm scene, baby animals, inventory and the 3.7 kitchen / fire scene all start, render and exit cleanly with the 3.7 update pack (`ci-results/windows/`) |
| 3.9 skins on screen | `Visual` scenes `skin-front/back/first/hold-a/b/hold-block`, `skin-screen(-def)`, `inv-player(-def)`, `pause-menu`, `title`, `firewall(-night)` | render correctly (`ci-results/shots/`) |
| 3.9 skins on the real Windows build | `Windows smoke test` scenes `skin-net` (the real `Blockcraft.exe` downloads a real account's skin from Mojang over the internet and shows it) and `skin-dialog` (the AWT file dialog opens, the game still exits cleanly with it open) | see `ci-results/windows/` |
| A physical GPU / real hardware | - | never run. Software OpenGL is slow but behaves like the real thing for these features; GPU-driver quirks cannot be ruled out. |

The 3.5 features that the first chat could not test (hoe, seeds, bucket, flint & steel right-clicks; sleeping; ladder climbing) were
exercised through the same code paths the mouse/keyboard use and **work as designed**; no bugs were found in them.

## Known gaps / ideas

- Not implemented: egg throwing, mobs trampling crops, animals following their parents, boats/minecarts, signs, fishing, enchanting, naturally generated melons (seeds come from dungeon chests), sound of any kind (thunder and rain are silent)…
- The sheep model has no separate "sheared" geometry: a sheared sheep is the same box tinted skin-pink with the head tuft hidden.
- Hearts for breeding are plain red particles.

## Delivery

Update packs live in `releases/` (≈ 6 MB: new `Blockcraft.exe`, `blockcraft.jar`, `README.txt`, debug `.bat`; they re-use the
JRE of an existing install). The 54 MB base install is `Blockcraft-1.12-UPDATE3.5-windows.zip` in the repo root. `tools/build.py --full`
produces a complete zip if one is ever wanted. Limits that matter: GitHub accepts files up to 100 MB (25 MB through the web uploader);
the assistant's sandbox can only talk to github.com, npm and PyPI (see `DEVELOPMENT.md`).
