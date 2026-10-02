BLOCKCRAFT 1.12  -  an original Minecraft-style voxel sandbox   (UPDATE 3.5)
==========================================================================
Run Blockcraft.exe (Windows 64-bit). Java is bundled in the "jre" folder - nothing to install.
Needs a GPU/driver with OpenGL 2.1+. The built-in shader pack needs OpenGL 3.0+ (falls back to
the plain renderer automatically if it can't start).
If it does not start, run Blockcraft-debug.bat and look at the messages, or open
%USERPROFILE%\.blockcraft112\crash.log  and  latest.log

This is NOT Minecraft and contains no Mojang code or assets: all textures, the world
generator and the physics are written from scratch to feel like Java Edition 1.12.

WHAT'S NEW IN UPDATE 3.5  (more 1.12 features)
  * FARMING: craft a hoe (wood/stone/iron/gold/diamond: MM / _S / _S) and right-click grass or dirt to make farmland.
    Plant seeds (get them by breaking tall grass, ~1 in 8) on farmland. Wheat grows through 8 stages over a few minutes
    (only near you - crops far away don't grow while unloaded). Harvest ripe wheat for wheat + 0-3 seeds; craft bread
    from 3 wheat (restores 5 hunger). Farmland turns back to dirt if you build over it; crops pop off without farmland.
    Not implemented: farmland hydration/water and trampling.
  * LADDERS: 3 per 7 sticks (S_S / SSS / S_S). Place on the side of a block; walk into it or hold jump to climb,
    sneak to hold still, and you take no fall damage while on a ladder.
  * BUCKETS: 3 iron ingots (I_I / _I_). Right-click a water/lava source to fill, right-click to pour a source back out.
  * TNT: 5 gunpowder + 4 sand. Light it with Flint and Steel (iron ingot + flint, 64 uses). 4 seconds fuse, falls and flashes,
    radius-4 blast that damages you and mobs and chain-ignites other TNT. Creepers' explosions also set off TNT.
  * BEDS: 3 any-colour wool over 3 planks. Placed 2 blocks long in the direction you face. Right-click at night to sleep
    through to morning (not if monsters are within 8 blocks); right-click any time to set your respawn point.
    If the bed is destroyed you respawn at world spawn. Both halves break together.
  * All new items are in the creative inventory (page 2).
  * Honest note: Blockcraft is an original clone and hasn't been tested on real Windows hardware (developed/tested headless on Linux).

WHAT'S NEW IN UPDATE 3.4
  * SUN RAYS (shader mode): screen-space light shafts streaming from the sun through gaps in trees/clouds/terrain.
    Strongest at sunrise/sunset, absent at night, in caves and when the sun is off-screen.
  * Shader lighting is back to the 3.2 look: the tone curve/colour grading, sharpening and soft-shadow filter added in 3.3 were removed.
    (Everything else from 3.3 - LOD lighting, coloured placed lights, trees, villages - is unchanged.)

WHAT'S NEW IN UPDATE 3.3
  * MULTIPLE WORLDS: Singleplayer now opens a world list (play / delete / create new). Each world has its own name and seed
    and its own save folder under %USERPROFILE%\.blockcraft112\saves\. Your old world shows up as a normal entry.
  * STAIRS, SLABS, FENCES (creative inventory, or craft: stairs 6 material -> 4, slabs 3 -> 6, fence 6 planks+sticks -> 3).
    Materials: oak planks, cobblestone, stone brick, brick, sandstone. Stairs face the way you look when placing;
    click the top of a slab with the same slab to make a full block. Fences connect to each other and to solid blocks.
  * WALL TORCHES: right-click the side of a block with a torch. (Top of a block = standing torch as before.)
  * Villages: real stair roofs (hollow attic, ridge caps, some brick roofs), fence posts on wells and lamp posts,
    wall torches by the doors and inside, small fenced flower gardens.
  * Trees: rounder lumpy oaks with branches (some big ones), tall narrow birches, layered spruces.
    Far-away (LOD) trees now match: rounded/tiered canopies with a light-to-dark gradient.
  * Placed light sources now actually light up the world with shaders on (up to 12 nearby lights, each with its own colour:
    torches warm orange, glowstone yellow, lava red, lit furnaces orange). Note: this is per-light coloured tinting on top of
    the normal single-colour block light, not full coloured light propagation through the world.
  * Shader: soft shadow edges, filmic tone curve + split toning, mild sharpening.
  * LOD lighting reworked separately from near terrain: hemispheric sky/ground ambient, sun-facing shading,
    procedural surface variation instead of flat colour.
  * Doors: edge faces no longer stretch/glitch the texture while opening (best-effort fix).

NEW IN 3.2: ZOOM - hold C to zoom in (smooth, slower mouse); scroll the mouse wheel while holding C to change the zoom level.

WHAT'S NEW IN UPDATE 3 (visuals)
  - Volumetric clouds (raymarched, lit by the sun, soft shaded undersides) - shader mode only
  - Dynamic lights: a held torch / glowstone / lava bucket-style emitter lights the world around you; dropped torches too
  - Shadows no longer shimmer or "move": swaying leaves/grass use their rest position for shadows, and the shadow map refreshes much more precisely with the sun
  - LOD terrain, trees and buildings are now lit with the real lightmap (day/night), sun tint and face shading, and get sun-coloured fog
  - LOD water uses the same animated waves / sky reflection / sun glint as real water, so the transition is seamless;
    far-away waves fade out (no more moire rings)
  - Better look: warm sunlight / cool shadows, FXAA, mild contrast curve, LOD no longer z-fights with real chunks
  - 3.1: removed thin dark lines across distant LOD water
  - Sheep face fixed (two eyes, no more cyclops)

FIXES IN 2.2 / 2.1
  * Chunks near you load/mesh many times faster (time-sliced, more threads, faster lighting)
  * LOD no longer leaves holes at the edge of loaded chunks (it stays until the real chunk is meshed)
  * Shader fixes: striped log/trunk textures, shadow acne on roofs, fog now matches the LOD fog,
    shadow map is cached (big FPS gain)

WHAT'S NEW IN UPDATE 2
  * Survival: health + hunger/saturation, sprint needs food, fall damage, drowning, lava, respawn
  * Game modes: survival, creative, ADVENTURE (can't break/place) and SPECTATOR (fly through blocks)
  * Inventory, 2x2 and 3x3 CRAFTING (shaped + shapeless), FURNACE smelting, CHESTS (+ loot chests)
  * TOOLS (wood/stone/iron/gold/diamond) with proper mining speed + durability; ARMOR with defense
  * MOBS: pig, cow, sheep, chicken, zombie, skeleton (bow), creeper (explodes), spider; spawners
  * STRUCTURES: villages, dungeons (spawner + chests), mineshafts
  * Sprinting runs on the 20 tps tick like 1.12, sprint FOV bump fixed (no more stuck zoom)
  * DOORS: craft with 6 planks (3 doors); right-click to open/close; villages now have real doors
  * SHADERS (Options > Shaders): sun shadows, reflective waves on water, bloom, wavy leaves/grass
  * LOD: distant terrain now includes trees and village buildings, finer detail near you;
    distant terrain simplified so you can see up to 256 chunks (Options > LOD Distance);
    real render distance goes 2-32 chunks. Lower LOD or turn Shaders off if your GPU is slow.

CONTROLS
  W A S D       move            Space       jump (double-tap = toggle flying in creative/spectator)
  Ctrl / W x2   sprint          Shift       sneak (won't fall off edges) / fly down
  Mouse         look            Left click  break (hold) / attack     Right click  place / use / eat / bow
  Middle click  pick block (creative)       1-9 / wheel hotbar        E  inventory
  Q             drop item       T or /      chat / commands           Esc  pause
  F1 hide HUD   F2 screenshot   F3 debug    F11 fullscreen
COMMANDS: /gamemode <survival|creative|adventure|spectator>  /time  /tp  /seed  /give <item> [n]
          /kill  /heal  /fly  /summon <mob>  /help

Worlds, options and screenshots are stored in  %USERPROFILE%\.blockcraft112
