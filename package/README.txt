BLOCKCRAFT 1.12  -  an original Minecraft-style voxel sandbox   (UPDATE 3.9)
==========================================================================
Run Blockcraft.exe (Windows 64-bit). Java is bundled in the "jre" folder - nothing to install.
Needs a GPU/driver with OpenGL 2.1+. The built-in shader pack needs OpenGL 3.0+ (falls back to
the plain renderer automatically if it can't start).
If it does not start, run Blockcraft-debug.bat and look at the messages, or open
%USERPROFILE%\.blockcraft112\crash.log  and  latest.log

This is NOT Minecraft and contains no Mojang code or assets: all textures, the world
generator and the physics are written from scratch to feel like Java Edition 1.12.

WHAT'S NEW IN UPDATE 3.9  (the skins update)
  * SKINS: wear your own Minecraft skin. Title screen or pause menu -> "Skins...". Four easy ways to get a skin into the game:
      1. DRAG a skin .png from a folder onto the game window - anywhere, in a menu or while you play - and you wear it at once.
      2. "Import skin file..." opens the normal Windows file dialog.
      3. Type your Minecraft USERNAME (or paste an account id) and press "Import": the game downloads your skin from
         Mojang's servers (needs internet; an account without a custom skin has nothing to download).
      4. Copy a file path, a link to a skin .png or a username and press Ctrl+V on the Skins screen.
    Normal 64x64 skins, old 64x32 skins and HD skins (128x128 ...) all work. Classic (4 px) and slim "Alex" (3 px) arms are
    detected by themselves and can be switched by hand ("Arms: ..."). Imported skins are copied into
    %USERPROFILE%\.blockcraft112\skins  ("Open skins folder" shows it), so you can also just drop .png files there;
    the skin you picked is remembered. Without a skin you wear the game's own default skin (drawn in code - no Mojang files).
  * YOU CAN SEE YOURSELF: F5 switches the camera between first person, behind you and in front of you. The body walks,
    sneaks, swings when you hit and holds the item from your hand; the camera stops at walls. In first person your own arm
    (in your skin) shows when your hand is empty. The survival inventory (E) now shows your character, looking at your mouse.
  * MOBS JUMP UP BLOCKS: pigs, cows, sheep, chickens, babies, zombies and creepers used to hop against a one-block ledge
    forever (their jump only reached 0.8 blocks). They now get up one block, but still not two - like in Minecraft.
  * CREEPERS CAN BE LIT: right-click a creeper with Flint and Steel - it stops, swells and explodes after 1.5 seconds, even if
    it has not noticed you (a charged creeper still blasts twice as hard). Costs one point of durability.
  * FIRE LOOKS LIKE MINECRAFT'S: on a block it is still the X of flames, but fire that hangs on something flammable (nothing
    under it) now covers the wall it clings to, fills an inside corner, or hangs from a wooden / leafy ceiling.
  * Smaller things: fog in the shader pack is measured around you instead of along the view direction (the far edge of the
    world is hidden from every angle); the game now really ends when you close it (a lingering helper could keep a
    "Blockcraft" process alive); the "Inventory" caption is gone from the crafting screen (the player stands there).
  * Honest note: skin handling, the model, the file dialog and the username download are covered by automated tests, and
    the real Blockcraft.exe was started on a Windows test machine to import a real account's skin over the internet and to
    open the file dialog; everything was checked as pictures from a software renderer. Dragging a file onto the window and
    how it all looks on a real graphics card have still not been tried on real hardware.

WHAT'S NEW IN UPDATE 3.8  (the weather update)
  * WEATHER YOU CAN SEE COMING. Weather is no longer one switch for the whole world: there are weather systems that drift
    across the map with the wind. A dark thundercloud on the horizon with a grey curtain of rain hanging under it IS the storm
    that will reach you in a few minutes (and you may see lightning in it). Cloud cover thickens before it rains, it rains under
    the dark cloud, and under the tallest part there is thunder. Some stretches are dry, some stormy. A new world starts under a
    fair sky (no rain for the first 5 minutes); sleeping through the night clears the sky as well.
  * CLOUDS IN BOTH MODES. With the shader pack the volumetric clouds now follow the weather: thin scattered cloud on fair days,
    grey sheets in the rain, tall dark thunderheads with a lowered base in storms. Without shaders you now get blocky 3D clouds
    (flat-bottomed fair-weather cumulus, storm towers that hang low and reach very high) that drift with the wind, instead of one
    flat sheet, and you can see storms and their rain curtains up to about 3 km away in both modes.
  * RAIN AND SNOW: streaks around you, but only where nothing is above you (not under a roof or a tree), splashes on the ground
    and on water; sky and fog turn grey, the haze moves closer and thick cloud hides the sun and moon. Cold biomes (ice plains,
    high mountains) get snow instead of rain, deserts stay dry.
  * THUNDERSTORMS: lightning flashes light up the sky and the clouds. A bolt that strikes within about 140 blocks of you sets
    fires, hurts you and animals (5 damage and burning; creative players are safe) and turns a creeper into a CHARGED CREEPER
    (glows blue, explodes with twice the power).
  * RAIN AND THE GAME: rain puts fires out, waters farmland even far from water, zombies and skeletons do not burn in it, and you
    can sleep through a thunderstorm even in the daytime.
  * COMMANDS: /weather  (what it is like here and where the nearest thunderstorm is),  /weather clear|rain|thunder [seconds],
    /weather storm [distance]  (a thunderstorm forms upwind and drifts over you - a good way to watch one arrive),
    /weather natural  (back to normal),  /weather off|on.  Options screen: "Weather: ON/OFF" (off = fair-weather clouds only).
    F3 shows the weather at your position. Weather is saved with the world.
  * Under the hood: one weather field that the game rules, the shader clouds and the blocky clouds all read (the same noise maps
    on the CPU and the GPU), so what you see in the distance is exactly what will happen.
  * Honest note: the weather rules and the cloud / rain geometry are covered by automated tests (33,000+ checks, including a proof
    that the shader's formula and the game's formula give the same weather), and the pictures were checked in automated
    screenshots on a software renderer in both render paths. The game has still never been played on real Windows hardware, so
    frame rate with heavy rain and clouds on a real graphics card is unknown. No sound yet (the game has no sound at all).

WHAT'S NEW IN UPDATE 3.7  (the kitchen & fire update)
  * FIRE: right-click with Flint and Steel on the face you are pointing at to light a fire there (it needs a solid block
    under it or something flammable next to it). It works like Minecraft 1.12's fire: it ages, burns out on its own on bare
    ground after roughly 10-30 seconds, and spreads to planks, logs, leaves, wool, bookshelves, hay bales, tall grass and
    flowers - what burns is gone for good (no drops). A fire next to TNT sets it off. Lava lights flammable blocks next to it
    now and then. Fire gives light (level 15). Punch it or flood it with water to put it out; blocks can be placed into it.
    You, animals and monsters catch fire in a fire or in lava: 1 damage per second until it burns out or you jump into
    water (flames show on your screen). Animals that die while burning drop cooked meat.
    A house fire can ruin a base, so there is a switch:  /firespread off  freezes every fire (it neither spreads nor burns
    out),  /firespread on  turns it back on (saved with the world). Creative players are immune.
  * SUGAR CANE grows by itself along shores: sand, grass or dirt with water next to it (new chunks only), up to 3 blocks
    high. Plant it yourself on such ground or on top of other cane; it pops off when its ground or water is gone and
    slowly grows while you are near. 1 cane -> 1 sugar; 3 canes in a row -> 3 PAPER; 3 paper + 1 leather -> BOOK;
    6 planks + 3 books -> BOOKSHELF (drops 3 books, burns).
  * PUMPKINS and MELONS: pumpkin patches now appear on grassland (new chunks only); dungeon chests can hold pumpkin and melon
    seeds. 1 pumpkin -> 4 pumpkin seeds, 1 melon slice -> 1 melon seed. Plant seeds on farmland: the stem grows through 8
    stages and then puts a fruit on a free grass/dirt/farmland spot beside it - one at a time, harvest it and it makes
    another. Bone meal works on stems. A melon drops 3-7 slices (1 hunger each; 9 slices -> melon block).
    Right-click a pumpkin with SHEARS to carve a face into it (4 seeds drop). Carved pumpkin with a torch under it
    -> JACK O'LANTERN (light 15). Carved pumpkins and lanterns are placed facing you.
    PUMPKIN PIE = pumpkin + sugar + egg (4 hunger).
  * CAKE: 3 milk buckets, 2 sugar, 1 egg, 3 wheat (MMM / SES / WWW). Place it on solid ground; right-click it to eat a
    slice (1 hunger, and only when you are hungry); 7 slices, then it is gone.
  * MILK: right-click a grown-up cow with an empty bucket. Milk is only an ingredient (the cake gives you the empty
    buckets back). EGGS: every grown-up chicken lays one about every 5-10 minutes.
  * ANIMALS FOLLOW FOOD: hold wheat (cows, sheep), carrots or potatoes (pigs) or seeds (chickens) and the animals that
    like it within 9 blocks walk over to you.
  * All new blocks and items are in the creative inventory (page 2); /give pumpkin, /give cake ... also work.
  * Honest note: all the rules above are covered by automated tests (11,000+ checks) and the terrain / lighting / physics /
    crafting code that was not meant to change is proven identical to Update 3.5 (with the new shore cane and pumpkin patches
    switched off, a generated world is bit-for-bit the 3.5 world). The textures, the fire animation and the flame screen
    effect were only checked as pictures and in automated screenshots on a software renderer - the game itself has still
    not been played on real Windows hardware.
    Not in this update: rain/weather, boats, fishing, throwing eggs, melons and pumpkins generating naturally in jungles.

WHAT'S NEW IN UPDATE 3.6  (the farm update)
  * FARMLAND NEEDS WATER: farmland with water within 4 blocks (on its level or one higher) turns dark and "wet".
    Crops grow about 40% faster on wet farmland than they did in 3.5 and about 40% slower on dry farmland (wet is
    roughly 2.4x as fast as dry). Dry farmland with nothing planted on it slowly turns back into dirt - after a few
    minutes - wet farmland never does. Farmland and crops only update within 32 blocks of you, as before.
  * TRAMPLING: jumping or falling onto farmland can turn it into dirt - the higher the fall the likelier (a short hop is
    often fine, a 2 block drop always ruins it). Crops on it pop off. Walking is always safe. Only you trample (mobs don't).
  * CARROTS and POTATOES: plant them on farmland with right-click (8 growth stages, ripe plants give 1-4 back, young
    ones give 1). Zombies rarely drop them (about 1 in 40). Carrot = 3 hunger, raw potato = 1, and a potato bakes in
    a furnace into a BAKED POTATO (5 hunger). On farmland right-click plants them; anywhere else it eats them.
  * BONE MEAL: 1 bone -> 3 bone meal. Right-click a growing crop (or the soil under it) to grow it 2-5 stages at once;
    right-click a grass block to sprinkle tall grass and a few flowers around it.
  * HAY BALE: 9 wheat -> 1 hay bale, and 1 hay bale -> 9 wheat. Landing on a hay bale takes 80% less fall damage.
  * SHEARS: 2 iron ingots placed diagonally. Right-click a sheep: 1-3 wool of its colour, the sheep turns pink and its
    wool grows back after 4-8 minutes. Shears also make leaves and tall grass drop themselves when you break them.
  * BREEDING: right-click two grown-up animals of the same kind with their food and they walk to each other and make
    a baby. Pigs like carrots or potatoes, cows and sheep like wheat, chickens like seeds. Love lasts 30 seconds, the
    parents then need 5 minutes of rest. Babies are half size, take 20 minutes to grow up (every feeding makes it
    2 minutes faster), cannot be sheared and drop nothing. A baby sheep gets one of its parents' colours.
  * HOES now also work on the side of a grass or dirt block (not only the top), like in Minecraft.
  * New items and the hay bale are in the creative inventory (page 2).
  * Honest note: every rule above is covered by automated tests (3,200+ checks), and the old terrain / lighting /
    physics / crafting code was verified to be unchanged against the 3.5 build, but the game itself has still not
    been played on real Windows hardware; the new textures were only checked as pictures.
    Not in this update: milk, eggs, mobs trampling crops, hoes/shears on more blocks.

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
  Mouse         look            Left click  break (hold) / attack     Right click  place / use / eat / bow / feed or shear animals
  Middle click  pick block (creative)       1-9 / wheel hotbar        E  inventory
  Q             drop item       T or /      chat / commands           Esc  pause
  F1 hide HUD   F2 screenshot   F3 debug    F5 camera: first person / behind you / in front of you    F11 fullscreen
COMMANDS: /gamemode <survival|creative|adventure|spectator>  /time  /tp  /seed  /give <item> [n]
          /kill  /heal  /fly  /summon <mob>  /help

Worlds, options and screenshots are stored in  %USERPROFILE%\.blockcraft112
