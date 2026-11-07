BLOCKCRAFT V6.30  -  an original Minecraft-style voxel sandbox   (EDITION V6.30)
==========================================================================
Run Blockcraft.exe (Windows 64-bit). Java is bundled in the "jre" folder - nothing to install.
Needs a GPU/driver with OpenGL 2.1+. The built-in shader pack needs OpenGL 3.0+ (falls back to
the plain renderer automatically if it can't start).
If it does not start, run Blockcraft-debug.bat and look at the messages, or open
%USERPROFILE%\.blockcraft112\crash.log  and  latest.log

This is NOT Minecraft and contains no Mojang code or assets: all textures, the world
generator and the physics are written from scratch.

WHAT'S NEW IN V6.30 - A ONE-MINUTE CINEMATIC TRAILER, IN GAME

  * WATCH TRAILER (1:00) on the title screen, or type /trailer while
    playing. This is NOT a video file: it is eight scripted camera
    moves through a real freshly generated world (fixed seed), with
    2.39:1 letterboxing, hard cuts, title cards, a dawn -> noon ->
    storm -> sunset -> night -> dawn time-lapse, a forced thunderstorm,
    a four-act soundtrack and foley stings from the real CC0 WAVs.
  * EXACTLY 60 SECONDS (8 shots x 7.5 s), then it ends by itself.
    ANY KEY or a mouse click skips it. The game restores your screen,
    camera position, view mode, daylight, weather, colour grading and
    options exactly as they were - in a live world too.
  * The shot table is deterministic and print-able without a window:
    java -jar blockcraft.jar --trailer-plan
    See docs/releases/v6.30-trailer-plan.txt, the storyboard
    docs/screenshots/v6.30-trailer.png (headless preview) and
    Trailer-guide.txt in this folder.
  * V6.29 walk-in cave entrances, real recorded sounds and all earlier
    work are unchanged. No Mojang code or assets are used.

WHAT'S NEW IN V6.29 - NORMAL CAVE ENTRANCES AND REAL RECORDED SOUNDS

  * ACTUAL WALK-IN CAVE ENTRANCES. V6.28's mouths were long diagonal
    cracks; the ones you see now are wide, irregular round openings in
    the ground with a stone lip and a SLOPED SOLID FLOOR you can walk
    down from the grass into a chamber. The deep end keeps a rock roof
    (a cave arch), and a horizontal gallery plus a straight tunnel
    always join a real carved cave - verified headlessly for every
    sampled entrance, 47/47 linked to the cave network.
  * NEW WORLDS ONLY (caveVer 4). The nine 64-block cells around spawn
    always get a mouth, so one is a short walk away; the old noise
    sinkholes, vertical shafts and most ravines are switched off so
    entrances are entrances rather than infinite cracks. V6.28 worlds
    keep caveVer 3 and are never rewritten. See the headless generated
    cross-section docs/screenshots/v6.29-caves.png (not a GPU shot).
  * REAL SOUNDS, NOT GENERATED ONES. All eight V6.28 procedural tones
    were removed. tree_fall, tree_crash, tree_creak, leaf_rustle,
    wind_gust, cave_drip, cave_wind, cave_rumble, geode_chime and
    stone_crumble are now REAL CC0 FIELD / FOLEY RECORDINGS, shipped as
    22050 Hz mono 16-bit WAVs in sounds/foley/ next to the exe and
    inside blockcraft.jar. Replace any WAV with your own recording and
    restart - loose files win. Type /sounds in game to see how many
    are active and /sounds tree_crash to audition one. Sources, authors
    and CC0 1.0 links are in sounds/foley/CREDITS.txt. No Mojang audio.
  * V6.28 caverns, V6.27 resource-pack fixes, V6.26 LOD fading and
    all earlier work are unchanged.

WHAT'S NEW IN V6.28 - 1.20-style CAVES, REAL ENTRANCES, MORE SOUNDS

  * CAVES REDONE TOWARD MINECRAFT 1.20 (caveVer 3, new worlds only).
    Cheese caverns now reach from y=8 to y=72, use two overlapping
    large-scale noise fields so they form multi-room chambers, and a
    third very long "spaghetti" tunnel scale connects the separate
    pockets into continuous systems. In a headless 6x6-chunk scan the
    carved volume went from 32,490 to 52,790 blocks (+62%) with more
    aquifer water and lava lakes.
  * ACTUAL CAVE ENTRANCES, not just those endless cracks (ravines) or
    tiny sinkhole pits. Every ~96-block hashed region can now grow a
    wide, irregular mouth in the surface with a stone lip, a SHALLOW
    SLOPED RAMP down through the rock, a rounded chamber at the bottom
    and a low gallery that joins the underground cave network. Trees
    and the world spawn no longer land inside the openings.
    See docs/screenshots/v6.28-caves.png (headless cross-section:
    old carver above, new entrance below). Old saves are untouched.
  * MORE SOUNDS (all original procedural synthesis, no Mojang audio):
      tree_fall   - wooden creak + rising canopy whoosh when a tree
                    starts toppling (audible from 48 blocks)
      tree_crash  - heavy thud, wood crack and settling leaves on landing
      leaf_rustle - extra foley when breaking leaves
      cave_drip / cave_wind / cave_rumble / geode_chime / stone_crumble
                  - positional ambience while you are under a rock
                    ceiling underground (drips, air swells, deep rumble,
                    crystal chimes, gravel slides)
      aliases: tree_creak, water_drip, cave_ambience
  * V6.27 texture-pack fixes, V6.26 LOD fading and all earlier work
    remain unchanged.

WHAT'S NEW IN V6.27 - the old Aseprite / resource-pack editing WORKS AGAIN

  * BUG FOUND AND FIXED. In V6.25 I added the single editable tilesheet.
    It compared each sheet cell against the CURRENT render atlas, so an
    UNTOUCHED tilesheet.png looked "different" wherever a resource pack
    had changed a texture - and quietly overwrote your edit with the
    default art. That is why Edit-textures.bat / tiles/0003_stone.png /
    Reload PNGs appeared to do nothing.
  * The game now ships a PRISTINE reference (tilesheet-default.png)
    next to tilesheet.png. Only cells that differ from THAT reference
    are applied. An untouched sheet is a true no-op, so any resource
    pack (per-tile PNGs, atlas.png, animation strips) works exactly as
    documented. A cell you actually edit in the tilesheet still wins.
  * PNG overrides now also apply to the built-in Vibrant / Retro grades
    and to classic-base packs, not just plain custom packs.
  * Diagnostics: "Reload PNGs" now says how many tile overrides were
    loaded, and /tilesheet reports edited cells + pack overrides +
    the active pack id, so a mis-selected pack is obvious.
  * V6.26 LOD fading, V6.25 trees / /treefall / tilesheet and all
    earlier features are unchanged.

WHAT'S NEW IN V6.26 - no more LOD pops and no more random crashes

  * LOD TILES NOW FADE IN. A tile used to appear the instant its mesh
    finished uploading, so walking or turning produced hard "pops" as
    each distant chunk snapped into view. New tiles now blend in over
    18 frames (~0.3 s) using per-tile constant-alpha blending, which
    works with AND without the LOD shader. At full opacity the result is
    byte-for-byte the old rendering.
  * THE OUTERMOST 10% OF THE LOD HORIZON also fades out, so CPU culling
    never cuts off a still-visible tile. Nearby tiles are never dimmed,
    even at a very short render distance.
  * SPLIT HYSTERESIS: a 6% band around each split distance stops a tile
    flipping between its own mesh and its four children on every tiny
    camera movement - another classic source of flicker/pops.
  * RANDOM CRASHES HARDENED:
      - LOD mesh uploads now derive the vertex count from the REAL byte
        length of the buffer, so a partial mesh can no longer overflow
        the compacted buffer and kill a worker.
      - A rejected worker task no longer leaks an inflight slot (which
        silently froze all further LOD requests).
      - Evicted / rebuilt tiles are marked retired, so a late worker can
        never queue a dead VBO for upload or draw.
      - Failed VBO uploads, failed per-tile draws and a failed LOD render
        are now contained: the bad tile is skipped/rebuilt instead of
        taking the whole game down. Normal blending state is restored.
      - Entity-shadow mob iteration uses a snapshot taken BEFORE glBegin,
        so a concurrent mob change (network/mods) cannot throw a
        ConcurrentModificationException or leave GL in a broken state.
      - Every uncaught exception from ANY thread is now written to
        %USERPROFILE%\.blockcraft112\crash.log with the thread name and
        full stack trace, so any remaining crash is diagnosable.
  * V6.25 trees, /treefall, the all-texture tilesheet and all earlier
    features are unchanged.

WHAT'S NEW IN V6.25 - trees rebuilt as real trees, they fall, and ONE editable
                       tilesheet holds every texture

  * TREES WERE REDONE AGAIN, FROM SCRATCH. No more geometric domes. Each
    tree now grows a connected skeleton of logs: a trunk that may lean,
    primary branches that reach out, and twigs, with separate irregular
    leaf bunches hanging on the twigs. Visible trunks, real branch gaps,
    lumpy asymmetric crowns, whorled conifers, flat acacia umbrellas and
    tall clean jungle boles. All 34 silhouettes, 3 size tiers, 8 species.
    See docs/screenshots/v6.25-trees.png (headless, not a GPU shot).
  * TREES REALLY FALL OVER WITH /treefall. The old planner refused most of
    the new trees: leaning trunks were diagonally disconnected, a whorl
    above the trunk top floated, 2x2 bases pinned the whole tree upright,
    and the 160-log / 700-leaf caps were far too small. Every species now
    topples at Normal AND Giant size in the headless chop tests; the caps
    are 480 logs / 1400 leaves with a 6-step leaf reach, and a real 2x2
    base is treated as part of the falling tree (a lone log on a stone
    still does NOT fall, so player-built cabins are safe).
  * ONE TILESHEET FOR ALL 1024 TEXTURES. tilesheet.png beside the exe is
    a 1024x1024 PNG: cell (row,col) = atlas tile row*32+col. Edit it in
    Aseprite keeping the canvas size and alpha, then type /tilesheet or
    press Options > Resource Packs > Tilesheet. Changes apply at
    full 32x32 immediately; an untouched sheet is a complete no-op.
    Animated cells are marked and left to the animation strips.
    Indexes: textures/TILESHEET.tsv, textures/tilesheet-index.html,
    textures/tilesheet-map.png. Full guide: TILESHEET.txt in this folder.
    Developer import: python3 tools/tilesheet.py import <your-sheet.png>.
  * Bug sweep: disconnected leaning trunks, floating top whorls, thick
    bases that never fell, oversized crowns left hanging in mid-air,
    oversized fall limits, and a reload path that ignored the new sheet.

WHAT'S NEW IN V6.24 - trees that look like trees, LOD that matches them,
                       and entity shadows that behave

  * TREES WERE REDONE. V6.23 crowns were one-block-thick flat discs with
    random holes; spruces were isolated rings with air between them. Each
    crown is now one closed, rounded 3D volume built from a vertical radius
    profile; conifers are continuous tapered cones; acacia, dark oak and
    jungle canopies have real thickness. All 34 silhouettes, all three size
    tiers and all eight species are kept.
  * NO MORE TRUNCATED GIANTS. Stacking whole domes used to exhaust the hard
    leaf budget, so the biggest jungle trees were cut off mid-crown. Crowns
    are hollow shells with solid caps, so even a Giant at Tree Size 1.5x
    stays well inside the budget (headless worst case 938 of 1200 leaves).
  * LOD NOW DEPICTS THE ACTUAL TREES. Distant tiles replay the exact chunk
    dice used by the world generator (same seed, 44 attempts, same tree
    plan, variant and size tier) and draw silhouettes MEASURED from the real
    geometry: trunk height, crown bottom/top and a six-band radius profile.
    Umbrellas stay flat, conifers taper, giants are taller. Distant forests
    are thinned by a hash instead of a fixed stride, so there are no empty
    stripes. See docs/screenshots/v6.24-trees-and-lod.png (headless render of
    the real geometry, NOT a GPU screenshot).
  * ENTITY SHADOWS LOOK RIGHT. Mobs used to cast five opaque box faces into
    the shadow map (big rectangular blobs), and they were drawn relative to
    the live camera while the shadow matrix is built at the last refresh, so
    shadows drifted up to 12 blocks as you walked. Each mob now casts one
    soft radial billboard facing the sun, anchored to the shadow-map camera
    and sun angle. Still re-stamped every frame; with "Live Entity Shadows"
    off they are stamped once per refresh instead of disappearing.
  * BUG SWEEP: fixed the lopsided disc loop, hole-riddled crown shells,
    detached jungle side lobes, clipped birch crowns, gaps at conifer tips,
    LOD's unrelated decorative forest and the missing static mob shadows.

WHAT'S NEW IN V6.23 - More tree variants and BIGGER trees for every species

  * EVERY tree species now has several different silhouettes and three
    size tiers (Normal / Large / Giant): 8 species, 34 shapes in total.
    Oak: lobed, squat, columnar, umbrella, forked twin-trunk, weeping.
    Birch: narrow, tall, three-stem clump, weeping, grand two-tier.
    Spruce: tiered, sparse pine, columnar, 2x2 giant, 2x2 grand, candle.
    Jungle: small, tall layered, 2x2 giant, huge buttressed canopy,
    very tall emergent. Acacia: bent, double-bend, wide plate, forked.
    Dark oak: classic, grand, spreading. Swamp: oak, cypress, willow.
    Bushes: round and tall. Giant mushrooms and ice spikes scale too.
  * The silhouette and size of each tree come from its seed-derived hash,
    so a tree crossing a chunk border is identical in both chunks. About
    58% Normal, 30% Large and 12% Giant; the biome's original plan still
    biases which variant you see, but all of them really appear.
  * BIGGER DOES NOT MEAN LAGGY. Giant conifers grow tall with spaced-out
    hollow tiers rather than solid crowns, the per-chunk tree cap remains,
    and a hard 900-leaf budget bounds every single tree. Headless 3x3
    taiga meshing is slightly FASTER than V6.22 (34.3 ms / 162548 verts
    vs 36.2 ms / 165648 verts) even with the bigger trees.
  * Two new settings in Options > Video > Debug Video > Advanced:
      Tree Size   0.70x .. 1.50x  (height and crown of every species)
      Giant Trees ON / OFF       (off removes the rare Giant tier)
    Both persist in options.txt, are saved with the world and appear in
    the Create New World preview. Old saves keep their old trees.
  * Headless renders of the REAL tree geometry:
    docs/screenshots/v6.23-tree-variants.png  (all 34 silhouettes)
    docs/screenshots/v6.23-tree-sizes.png     (Normal / Large / Giant)

WHAT'S NEW IN V6.22 - Caves & Cliffs carver, cheaper taiga, smooth borders,
                       live shadows and a movable minimap

  * CAVES WERE REDONE AGAIN for new worlds. An original Caves & Cliffs-style
    engine (CaveCliffs) replaces the V6.21 tunnels: thin noodle caves, long
    winding spaghetti tunnels, big irregular cheese caverns, rare giant halls
    and vertical wells. Local hashed AQUIFERS put a water table at y=22..49 in
    some regions, so a cavern can be dry above and flooded below. Warped cave
    biomes add lush vines / glow berries / azalea, dripstone, ice pockets,
    amethyst GEODES and deep-dark floors. The V6.21 underground sites and loot
    chests still generate on top of the new carver. Old saves stay untouched.
  * TAIGA IS NO LONGER THE GAME'S DEATH. Spruce crowns used to be nearly solid
    cones of ~1000 leaves. They are now tiered, hollow conifers with drooping
    tips; mega spruces use half the levels. Tree density in taiga / snowy
    taiga / mega taiga / dark forest / jungle is lower, a per-chunk cap stops
    mesh spikes, and bushy-leaf meshing no longer allocates per leaf block.
    A Tree Density slider (0.4x..1.4x) is in Debug Video > Advanced.
  * WORLD GENERATION has smoother borders. Climate coordinates are warped by
    two slow noises, biome edges blend by interleaving the neighbouring
    biome's surface / plants / trees and blending map colours, mountains get
    soft terraces, rivers get broad valleys, shorelines get wider beaches and
    the snow line is broken up. Biome Borders: Sharp / Smooth / Very Smooth.
  * ENTITY SHADOWS UPDATE EVERY FRAME. A terrain-only copy of the shadow depth
    is restored each frame with one depth blit, then this frame's mob
    silhouettes are stamped in. No terrain re-render, no 8..150 frame lag.
    Toggle with "Live Entity Shadows" in Debug Video > Advanced.
  * THE MINIMAP CAN MOVE. "Minimap Position" cycles Top Right / Top Left /
    Bottom Right / Bottom Left. "Move for Toasts" slides a top-right minimap
    down while an advancement toast is showing, so it never covers it.
  * Headless previews of the real generators: docs/screenshots/v6.22-caves.png
    (V6.21 vs V6.22 cross-sections) and v6.22-borders.png (biome maps).

WHAT'S NEW IN V6.21 - Rebuilt caves with underground structures, and weather to the horizon

  * The caves were COMPLETELY REDONE for new worlds. The V6.19 "swiss cheese"
    carving is replaced by a new layered cave engine: winding tunnels on
    several depth bands, sparse chambers and giant halls, lava provinces,
    sinkholes and rare cave mouths instead of the world being hollow.
  * Rock is now visibly stratified in horizontal undulating bands (granite,
    diorite, andesite and dark deepslate-like stone) rather than random
    speckles. Cave walls are lined, and caves contain stalactites,
    stalagmites, glowstone, ore pockets, glow-lichen and mushrooms.
  * There are STRUCTURES AND STUFF underground: six deterministic site
    types - ruined shrine, crystal geode, lava forge, mineshaft hall,
    mushroom grotto and flooded cistern - each with a loot chest, lights,
    pillars, beams, a pedestal and connecting corridors. Sites are placed
    under the real surface height, so they never float in an ocean, and they
    do not depend on the surface Structures switch.
  * Create New World has a new "V6.21 Caves" terrain choice plus Cave
    Structures, Cave Strata, Cave Lakes and Cave Richness settings. Old saves
    keep their exact old terrain; nothing is regenerated under your feet.
  * ALL WEATHER is prettier, including IN THE DISTANCE: rain and snow
    curtains now reach toward the horizon in a wind-leaning, distance-faded
    grid; storm cores brighten, lightning lights the nearby curtains, and a
    soft ground haze drifts under far-off rain. Three detail levels build
    1, 2 or 3 crossing planes per cell.
  * A seven-band RAINBOW appears opposite a low sun while rain clears, with
    the right wet-ground, clearing and sun-visibility conditions. Clouds get
    a stronger silver lining, a warm sunset rim and a cold ice halo.
  * Weather Quality, Weather Distance, Weather Wind, Ground Mist, Lightning
    Glow and Rainbows are all in Options > Video > Debug Video > Advanced
    (and persist in options.txt). All the earlier V6.20/6.19/6.18 settings,
    minimap, big map and fixed panorama remain.
  * Headless previews of the real generator and weather maths:
    docs/screenshots/v6.21-caves.png and v6.21-weather.png.

WHAT'S NEW IN V6.20 - Minimap, big world map and the fixed panorama

  * A live MINIMAP sits in the top-right of the HUD. It uses the real
    height/biome/top-block data of loaded chunks and fills unexplored land
    from the world generator, with relief shading, a player arrow, nearby
    mob dots, coordinates and a north marker.
  * Press M while playing for the BIG MAP: a pannable, zoomable overview
    (2..64 blocks per pixel). Pan with WASD or the arrow keys, zoom with
    the mouse wheel or +/- , click the picture to centre it there, press R
    to recentre on the player, G for the grid and B for mob dots. Buttons
    do the same. M or Esc (or Done) closes it.
  * Both maps share the same colours and are drawn as ordinary GUI pictures,
    so they work with or without the shader pack. The big map fills itself
    a thousand pixels per frame instead of in one frame-long burst.
  * Every map setting is in the game: Minimap, Minimap Size, Minimap
    Radius, Rotate Minimap, Map Refresh, Big Map Zoom, Mob Dots and Map
    Grid under Options > Video > Debug Video > Advanced (plus a quick
    Minimap switch on the Game tab). All persist in options.txt.
  * FIXED: the title-screen panorama often stayed black. Its fade-in waited
    for every section of 25 chunks to be meshed, but demand meshing
    deliberately leaves hidden cave sections dirty forever. It now uses the
    renderer's view-aware mesh readiness plus a six-second fallback.
  * The title now also draws a generated 2D landscape immediately, and
    cross-fades it into the real turning world. If the panorama worker or
    the 3D world fails, the landscape stays visible instead of a black
    screen. Preset switching and Cycle were also corrected.

WHAT'S NEW IN V6.19 - Moonlight, entity shadows, LOD control and bigger caves

  * The moon now casts real shadows. Once the sun is below the horizon the
    moon (the opposite direction) takes over the shadow map, with dim cool
    blue moonlight; terrain, water, clouds, LOD and entities all follow it.
    Moon Shadows and Moon Light are independent settings.
  * Mobs now cast shadows: a five-quad
    silhouette goes into the same depth-only shadow map, so the caster needs
    no extra textures, programs or framebuffers. Entity Shadows toggles it.
  * The first-person hand/item is lit by the shader now: sun/moon direction,
    cool moonlight, sunrise fog glow and the twelve dynamic lights all reach
    the viewmodel instead of a flat lightmap colour. Hand Lighting toggles it.
  * Distant terrain (LOD) is the big frame-spike control. New settings:
    LOD Falloff, LOD Fog Start/End (tiles beyond the fog are not drawn at
    all), LOD Detail, LOD Builds/Frame, LOD Uploads/Frame, LOD Draw Cap and
    LOD Workers. Per-frame root tiles, comparators and colour arrays are
    reused instead of allocated every frame.
  * New worlds get bigger connected cave systems, deep caverns, more cave
    mouths, taller sharper mountain ranges and wider ravines. Cave Size,
    Cave Density, Mountain Scale and Ravine Scale are configurable. Existing
    saves keep their exact old terrain; V6.19 Terrain switches it off.
  * All of the above are in Options > Video > Debug Video > Advanced, and
    persist in options.txt. F3 shows the live LOD tile count and moon state.

WHAT'S NEW IN V6.18 - New panoramas, the Advanced Video tab and smoother frames

  * Create New World shows every biome: the five aquatic ocean biomes that used
    to be black now have map colours, a colour key is drawn inside the preview,
    and the screen lists the biomes and features the world will contain. The
    "New Oceans" switch chooses the aquatic generator directly.
  * Three title-screen panoramas: Classic, Coral Coast and Snowy Peaks, plus a
    Cycle mode. Choose one under Options > Video (or the Advanced tab).
  * Turn on "Debug Video" in Options > Video to unlock a fifth Advanced tab.
    It exposes every render, shader and performance setting in game, including
    shadow-map size, cloud resolution/steps, bloom, shadow refresh, particle
    cap, worker counts, chunk cache, unload budget and autosave interval. All
    settings are saved to options.txt.
  * Redstone states are pruned when a chunk unloads, large circuits use bounded
    per-tick event budgets and allocation-free primitive tables, pending pulses
    and burnout now survive a save, and F3 shows the power, input, delay and
    burnout of the circuit you are looking at.
  * Frame spikes and retained memory: collision scratch buffers are reused,
    mob/tile tick allocations are cut, chunk files are written on a low
    priority worker, autosaves are spread over frames, unloads and saves are
    budgeted, and loaded chunks honour a configurable memory ceiling.

WHAT'S NEW IN V6.17 - Demand-driven terrain meshes and bounded mesh memory

  * Section air-connectivity is now computed from chunk block data on a
    background chunk-generation worker, before terrain reaches the renderer.
    Mesh jobs no longer have to run for every dirty section merely to learn
    whether a sealed section is hidden by solid ground.
  * Dirty non-empty sections are queued only when reached by the last camera
    visibility/cave-culling walk, with a two-chunk warm ring for nearby turns.
    Surface terrain is covered first; hidden cave/interior sections are built
    when they become visible. Render distance and graphics settings are unchanged.
  * The LOD yields when the real surface is covered, without pretending that
    deliberately deferred deep sections were meshed. Visible cave work is still
    required before that chunk is treated as fully covered. World-entry loading
    uses this same view-aware readiness rule and does not wait for hidden caves.
  * Mesh staging is bounded to at most two queued/running jobs per worker
    (four workers maximum). Large per-thread vertex scratch arrays are released
    after use; common AO, fluid, plant and crop meshing no longer allocates
    temporary coordinate arrays for every vertex or block.
  * The built-in benchmark now reports estimated uploaded VBO payload, deferred
    sections and Java heap use so RAM/VRAM work can be checked explicitly.
    No visual quality, render distance, or user setting has been lowered.

WHAT'S NEW IN V6.16 - PC chunk handling and performance test build

  * Chunk generation remains asynchronous; newly integrated chunks now receive
    lighting on a dedicated, low-priority worker instead of blocking the game
    update. The lighting backlog is bounded so busy loading requeues work rather
    than building an unbounded queue.
  * Block edits and lighting are coordinated safely. Chunks are not offered to
    the mesher while their lighting pass is in progress.
  * Chunk eviction and distant-terrain refresh are incremental: at most 128
    loaded chunks are scanned and two are evicted per update; at most 512 are
    checked and four LOD summaries refreshed per Overworld tick.
  * A per-section non-air count avoids rescanning empty block arrays during
    meshing and void-world checks. Counts are rebuilt after bulk generation or
    loading and maintained through ordinary block edits.
  * No graphics settings or render distance were lowered. This is a performance
    test release: no PC frame-time benchmark has been measured yet. Please try
    it on your normal settings/world and report CPU, memory, render distance,
    and whether the original chunk-load spikes improved.
  * All V6.15 gameplay, Create machinery, textures, saves, worlds and controls
    are retained; this release changes chunk scheduling and bookkeeping.

WHAT'S NEW IN V6.15 - Visible machines, smooth rotation, belts and water torque

  * New dedicated machine textures at consistent pixel density, not stretched
    block skins. Frame-interpolated models replace tick/chunk-based animation.
  * Working straight horizontal belts and depots with visible saved cargo.
    Presses stamp batches on the belt/depot two blocks below, without menus.
  * Drop ingredients into the millstone or insert by hand; collect with empty
    hand. Routine right-click motor/wrench status no longer fills chat.
  * Actual water-current torque drives water wheels: still water stops them;
    opposite current direction reverses them. No manual wheel toggle.
  * /kinetics is now a real belt/press/depot line plus a waterfall-powered wheel.
    See Create-foundation.txt for controls, upgrade safety and explicit limits.
  * Still NOT full Create. Straight segment-by-segment belts only for now;
    funnels, inclined belts, contraptions, trains and many systems are missing.

WHAT'S NEW IN V6.14 - Native Create FOUNDATION, NOT the full mod

  * Starts the native Create project inside Blockcraft. No Minecraft needed.
    This is explicitly an incomplete foundation milestone, not full Create.
  * Signed RPM networks, shafts/cogs/gearboxes, water wheel and Creative Motor,
    stress limits, clutch/gearshift, speed/stress gauges, press/mill prototypes.
  * Real item inputs/outputs and persistent processing; original animated
    models and five native 32px sprites. Existing artwork is preserved.
  * Search create in Creative. /kinetics builds a clear-air example factory.
    See Create-foundation.txt for controls, recipes and important differences.
  * Belts, contraptions, trains, logistics, fluids/heat, most processing and
    full multiplayer are NOT implemented. Do not expect full Create parity.
  * Create 6.0.10 code reference is MIT; upstream artwork is All Rights
    Reserved and is NOT bundled. See Create-code-license.txt.

WHAT'S NEW IN V6.13 - Listen to MIDI with note-block sounds, no track

  * Select a MIDI in the compiler menu and click Play full. No blocks are
    placed, no 256-block rail, and no flying after positional sounds.
  * Play section uses Start/Length; Stop cancels the notes and ringing tails.
    Sound stays centered on your own speakers/headphones. Keep the menu open;
    Close/Escape or editing the input stops playback. Local listening only.
  * Uses the actual 16 note-block samples, 25 pitches per instrument, octave
    folding and 100ms timing. No MIDI sustain, velocity or pitch bends.
  * Audio is not limited to 512 notes or six building lanes. Safety limits:
    1 MiB MIDI, 50,000 converted notes, 60 minutes and 32 notes per chord.
    Build circuit remains available separately with its physical limitations.
    See MIDI-guide.txt for the complete listening/building controls and limits.

WHAT'S NEW IN V6.12 - Direct MIDI selection and GUI building

  * Right-click the MIDI Build Compiler in Creative to browse directly to a
    local MIDI. No commands, copying or filename restrictions in the GUI.
  * Click a file to preview. Default: first FIVE seconds, not the whole song.
    Edit Start and Length, Preview, then Build circuit. Go to button takes
    you to the physical start; right-click the button to play.
  * Automatically finds clear space above terrain; never overwrites blocks.
    Shows specific errors and rechecks the captured compiler/world on Build.
    You no longer have to keep aiming at the compiler after preview.
  * Existing real redstone playback and all 16 note instruments retained.
    Limits remain: short sections, 256-block rail, 512 notes, six-note chords.
    See MIDI-guide.txt for full instructions, safety and conversion limits.

WHAT'S NEW IN V6.11 - MIDI-to-redstone compiler and note-block instruments

  * New MIDI Build Compiler block. Import a .mid/.midi file, preview its cost,
    then confirm a build of REAL repeaters, dust and tuned note blocks.
    Remove the compiler afterwards: the physical circuit still plays.
  * Find it in Creative > Decoration and Utility, or search MIDI. Right-click
    to enter /midi filename.mid; /midibuild confirms. Creative host only.
  * /mididemo creates an original Workshop.mid example without overwriting.
    User files go in %USERPROFILE%\.blockcraft112\midi\.
  * 16 synthesized note-block instruments selected by the block UNDERNEATH:
    wood bass, sand snare, glass hi-hat, stone bass drum, clay flute, gold bell,
    wool guitar, packed-ice chime, bone xylophone, iron xylophone, soul-sand
    cowbell, pumpkin didgeridoo, emerald bit, hay banjo, glowstone pling,
    otherwise harp. Right-click cycles 25 pitches. Keep the top clear.
  * Tuning now survives save/load and piston movement. Back up notes.properties
    with your worlds. Host note events carry instrument/pitch to clients.
  * Musical approximation: 100ms timing, six-note chords, octave folding;
    no sustain/velocity/pitch-bend reproduction. Maximum 256-block track.
    Import long files in sections: /midi MySong.mid 30 8. Nothing is silently
    truncated. Existing blocks/occupied air prevent the whole build.
  * MIDI-guide.txt has setup, limits and controls. Existing V6.10 machines,
    textures, flat sprite bow and held lighting are unchanged.

WHAT'S NEW IN V6.10 - Sound machines and original silhouette cinema

  * Block Drop sound cues are real TONE instructions in its editable ROM.
  * Reusable Raster Speaker and Media Cartridge blocks; distance/pan, mute,
    quiet volume, frame-locked audio, pause, seeking, looping and save/resume.
  * /shadow builds a 64x48 display and 32x24 redstone-lamp cinema machine.
    SHADOW SIGNAL is an ORIGINAL 32-second animation and synthesized score,
    not the Bad Apple video, song or character artwork.
  * /arcadesound safely upgrades the supplied V6.9 ROM (resets the game), or
    use the separate Block Drop - Sound Edition save in the new world bundle.
  * Space/P pauses cinema, arrows seek, Enter restarts, M mutes, Esc leaves.
    See Sound-machines.txt, MediaPlayer.asm, FallingBlocks-Sound.asm.
  * Original media is included in the game. Custom PNG/WAV media can be packed
    with Make-media.py and imported by the host. Matching versions required.
  * Monochrome panel geometry is merged without losing addressable pixels.
    Existing textures, flat bow, lighting and V6.8/V6.9 hardware are preserved.

WHAT'S NEW IN V6.9 - Modular pixel screens and a programmable lamp arcade

  * Display panels join edge-to-edge: 4x4 pixels per block, up to 64x64.
  * Separate processor, editable ROM, 4K-word RAM, GPU, keypad, data cable,
    lamp adapter and directional pixel drivers. No hidden Tetris controller.
  * /arcade builds a playable falling-block computer with a color panel wall
    and 448 ordinary redstone lamps. A matching native world is also supplied.
  * Empty-hand right-click keypad, ENTER to start. Arrows move/rotate, SPACE
    drops, P pauses, ESC leaves controls. See Raster-guide.txt for full details.
  * FallingBlocks.asm is the actual editable program, not pseudocode.
  * Existing 32px art, flat bow, held lighting and V6.8 circuit blocks preserved.
  * Use V6.9 on every multiplayer client. Back up saves before updating.

WHAT'S NEW IN V6.8 - Buildable redstone computer + furniture facing
  * In Creative, type /computer to build a working 4-bit stored-program
    computer above clear terrain. No existing blocks are replaced.
  * Physical, editable 16-word ROM, accumulator, program counter, ALU,
    visible isolated bus wiring, hex display and four actual redstone lamps.
    The example counts 1..F,0. Break wires or edit ROM to change what it does.
  * Orange clock: RIGHT-CLICK Run/Stop; empty-hand sneak/right-click Step.
    Red reset button resets registers without erasing the program.
    Aim at components for live values and control hints. Computer-guide.txt
    includes the board map, instruction set, wiring rules and example programs.
  * Sixteen reusable, craftable circuit parts plus a 3D nibble bus. This uses
    custom redstone-compatible blocks, not vanilla-only Minecraft circuitry.
    About two seconds per instruction; roughly six per counter increment.
  * Chests, furnaces and crafting tables now face the player on placement.
    Chest lids follow the correct hinge. Furnace cycling, bucket waterlogging,
    saves and multiplayer block edits preserve facing and inventory.
  * Flying to the board no longer leaves distant-terrain gaps where old,
    out-of-range chunk meshes incorrectly suppressed their LOD replacements.
  * Existing art, flat sprite bow, held lighting and Aseprite editing remain.
    V6.6 and V6.7 downloads remain unchanged. Back up saves; use V6.8 on all
    multiplayer peers. These new IDs/properties are not downgrade-compatible.
  * Limits: four-bit values, 16 instructions, no terminal/OS. Chunk unloading
    pauses computation; reload restarts clock phase after signals settle.
    A bus touching an unloaded chunk boundary pauses all computer registers
    and clocks in that world. Wider redstone still is not exact Java parity.

WHAT'S NEW IN V6.7 - Aquatic worlds and Java-style redstone
  * New Default/Large/Amplified worlds have temperature/depth-based oceans,
    deeper seabeds, kelp, seagrass, five coral families, reefs and icebergs.
    Old saves keep their generation setting; existing chunks are not replaced.
  * Water reaches seven blocks, routes toward nearby drops, falls, recedes,
    renews sources, washes away small components and carries swimmers.
    Soul sand and magma create upward/downward bubble-column forces.
  * Sprint underwater to swim with a low collision pose; look up/down to steer.
    Buckets waterlog slabs, stairs, fences, panes/bars, walls, trapdoors,
    ladders and chests. Break a wet block and its water stays behind.
  * Directional dust and weak/strong block power; timed torches, burnout,
    four repeater delays and side locking; one-redstone-tick observer pulses;
    delayed lamp shutoff; analog/inverted daylight sensors.
  * Craft comparators with three redstone torches, quartz and three stone.
    Right-click to toggle compare/subtract; container fullness is readable.
  * Pistons validate a 12-block assembly before moving it, carry slime-attached
    blocks, leave glazed terracotta unstuck, and support sticky short-pulse
    block dropping and quasi-connectivity sampled on updates.
  * Settings/waterlogging survive chunk saves and multiplayer block updates;
    hosts synchronize the generator setting before clients generate terrain.
  * Existing artwork, flat bow, held lighting and Aseprite editing are retained.
    The editor kit now adds 13 aquatic tiles (545 static tiles + 6 sheets).
    Your MyTextures folder is not overwritten. Missing new tiles use defaults.
  * BACK UP saves before upgrading. Use V6.7 on every multiplayer peer. New
    aquatic block IDs/settings are not intended for downgrade to older games.
  * Compatibility limits: this is an original implementation, not complete
    Minecraft Java 1.14 parity. Pistons move blocks instantly (no moving-block
    entities or slime entity launching); exact update-order/BUD contraptions
    are not guaranteed. Scheduled tick deadlines restart after world reload.
    Non-slab waterlogging uses simplified surface clipping. Coral death/growth
    and aquatic structure/mob catalogs are not a complete vanilla recreation.

WHAT'S NEW IN V6.6 - Art review updates + Aseprite texture editing
  * Includes the building, Colors/Nature and Utility/Tools review changes:
    revised ores/logs/wool/powders/glazes, utility faces, slimmer tools/armor,
    TNT lettering, scarier pumpkins, egg, cake, paper/book and Eye of Ender.
  * Hinged chest lids, raised portal eyes, six-facing observers, folded banner
    models and pick-block, torch components and localized emission.
  * Keeps the accepted planks/cobble/stone-brick references, shears and
    flint-and-steel; flat bow animation, gun poses and held lighting remain.
  * Edit-textures.bat exports the ACTUAL native 32px PNGs, six animation
    sheets, searchable index and instructions for editing in Aseprite.
  * Options > Resource Packs > Reload PNGs applies saved edits immediately.
    Native custom packs retain crisp 32px pixels, transparency, animated
    textures and held bow sprites. Old 16px packs remain supported.
  * Export never overwrites your pack. See Texture-editing.txt beside the EXE.

WHAT'S NEW IN V6.5 - Cohesive native-pixel art rework
  * Replaced all 456 non-protected populated default atlas cells from V6.4,
    including construction, vegetation, decorations, animation and item icons.
    This is a structural redraw, not a sharpening or recoloring pass.
  * Furniture and machinery share the approved timber and masonry. Cracked
    bricks now have sparse local fractures; Nether brick is charcoal-burgundy.
  * New tool heads/grips, armor panels, food, mineral and equipment silhouettes.
    Leaves, crops and flowers have native-pixel stems, veins and petal shapes.
  * Water, lava, fire and portal animations use five-pigment 32px frames.
    Glass, metal bars and neutral biome/dye masks retain their transparency/tints.
  * Kept all 61 protected V6.3 texture cells pixel-for-pixel, including stone
    bricks, planks, cobblestone and the flat animated bow. Corrected gun poses
    and held-item lighting are unchanged. Legacy/custom packs stay separate.
  * Select Natural Pixel 32 (Default) in Options > Resource Packs to see this art.
    Older packs intentionally retain their original appearance.

WHAT'S NEW IN V6.4 - Complete Natural Pixel 32 pack
  * Finished the remaining default block and item atlas artwork at native 32x32:
    plants, leaves, crops, workstations, containers, glass, colored materials,
    Nether/End blocks, redstone, decorations, tools, armor, food and item icons.
  * Water, lava, fire and portal animations now also use native 32px painters.
    Stepped palettes and hard pixel edges; no blur or generic smoothing filter.
  * Preserved the approved 6.3 terrain, wood, masonry, wool, ores and sprite bow
    exactly. Gun poses/recoil and the corrected held-item lighting are unchanged.
  * Held/dropped item extrusion follows the new native sprite silhouettes,
    avoiding old 16px outlines clipping the new artwork.
  * Default pack coverage is audited per atlas cell, including model-only faces
    and the almond-water icon. Older/custom resource packs remain separate.
  * Select Natural Pixel 32 (Default) in Options > Resource Packs.

WHAT'S NEW IN V6.3 - Natural Pixel 32, sprite bow and held-item lighting
  * Default textures are now 32x32, with crisp pixel clusters and natural stepped
    palettes. No bilinear reconstruction or blur. New stone, soil, grass, sand,
    timber, cobble, masonry and ore patterns; other authored pixels remain sharp.
  * The held bow is flat pixel art again, with idle/three pulling stages and a
    nocked arrow painted into the frame. No 3D rods or curved limb mesh.
  * Held items use the player's gamma-correct lightmap once, preserving torch
    colour without a second world-space sun/shadow pass. Nearby torch lights are
    sampled at the player, not from viewmodel face normals.
  * Fixed the All Blocks gallery startup crash caused by an extended block ID
    in the starter inventory, and missing biome tints that made gallery grass black.
  * Choose Natural Pixel 32 (Default) in Options > Resource Packs. Older packs
    and custom packs are still available and your saves/options are preserved.

WHAT'S NEW IN V6.2 - Smooth Realism & held weapons (included in this download)
  * Default block/item atlas now renders at 64x64 per tile (2048x2048 total), with
    new natural stone, soil, sand, timber, bark, cobble, masonry and ore materials;
    remaining block/item art has smooth colour reconstruction and fine detail.
    Removed heavy per-pixel embossing. Classic, older packs and custom packs stay selectable.
  * Guns point away from the camera, attach at the grip in third person, and use
    elbow-compensated aim instead of the generic melee swing. Recoil settles smoothly.
  * Held bows now use bending 3D limbs, an attached string and nocked arrow in both
    views, interpolated drawing, and a short spring-back after release.

HOW TO CONFIRM THIS UPDATE
  * Extract into a NEW folder and run that folder's Blockcraft.exe, not an old shortcut.
  * The title screen must say Blockcraft V6.3 and Textures: Natural Pixel 32px.
  * If a different pack is active, select Natural Pixel 32 (Default) under
    Options > Resource Packs. Do not delete your saves or options.
  * Check-build.bat reports the version and source fingerprint from inside the EXE.
  * The All Blocks buttons and the bed/turned-log fixes from V6.1 are retained.

WHAT'S NEW IN V6.0  (100% Brand-New Textures From Scratch for Every Single Block & Item + "All Blocks & Items Showcase" Gallery World)
  * 100% BRAND-NEW TEXTURES PAINTED FROM SCRATCH FOR EVERY SINGLE BLOCK & ITEM (TexBrandNewV60). Every single block tile, crop stage, flower, sapling, door, trapdoor, bed, banner, wool, concrete, terracotta, glazed terracotta, stained glass, prismarine, purpur, redstone mechanism, liquid/fire/portal animation frame, tool, weapon, armor piece, dye, food, and item sprite in the 512x512 atlas is now painted 100% new from scratch (no untouched legacy tiles anywhere). Previous texture versions (V5.9 Relief, V5.8 Fusion, V5.7 Pixel Art, V5.6 Painted, and OG Classic 3.5) remain selectable in the Resource Packs menu.
  * "ALL BLOCKS & ITEMS SHOWCASE" GALLERY WORLD (AllBlocksGen & WorldSettings.ALL_BLOCKS). Added the "All Blocks Gallery" world type (plus an automatic pre-seeded "All Blocks & Items Showcase" world in Singleplayer and `/gallery` / `/allblocks` / `/showcase` commands) that lays out every single block, crop stage, bed color, banner color, door, and redstone state in walkable museum aisles with floating 6-face inspection blocks above and entrance Showcase Chests containing every single item, tool, weapon, bow, armor piece, food, and dye.

WHAT'S NEW IN V5.9  (Realistic 5.6 x Minecraft HD-Feel Texture Engine, Fixed Recurve Bow, Complete Texture Coverage & All Texture Versions in Separate Resource Packs)
  * FIXED RECURVE BOW (2D SPRITE & 3D FIRST-PERSON DRAW ANIMATION). Fixed the backwards/disconnected Bow texture (id 268) with an authentic diagonal Recurve Bow featuring curved yew wood limbs, a wrapped dark-leather riser grip with gold bands, carved horn nock tips, and a taut braided bowstring directly connecting the upper nock (12, 2) to the lower nock (2, 12). Upgraded first-person Bow rendering in Renderer.renderHand so the wooden belly faces forward, the string faces the archer, and pulling the bow smoothly animates a nocked 3D arrow.
  * EVERY PREVIOUSLY UNTOUCHED BLOCK & ITEM TEXTURE UPDATED + REALISTIC 3D MICRO-RELIEF. Upgraded all previously untouched blocks and items across Updates 3.5–3.19 (Water & Lava animation frames, Monster Spawner, Wool, Concrete & Powder, Terracotta & Glazed Terracotta, Stained Glass, Granite/Diorite/Andesite, Podzol, Mycelium, Coarse Dirt, Jungle/Acacia/Dark Oak barks, Flowers, Double Plants, Lily Pad, Vines, Mushrooms, Iron Bars, Trapdoors, Beds, Banners, Redstone, Prismarine, Purpur, Stick, Coal, Ingots, Gems, Apple, Golden Apple, Bread, Buckets, Flint & Steel, Shears, Ender Pearl, Eye of Ender, Dyes, and all Food/Crop sprites) with PBR-inspired directional micro-normal relief shading and crevice ambient occlusion.
  * ALL TEXTURE VERSIONS IN SEPARATE RESOURCE PACKS (ResourcePacks). Added separate built-in Resource Packs for every texture era in the Resource Packs menu: Blockcraft V5.9 Realistic (Default), Blockcraft V5.8 (5.6 x MC Fusion), Blockcraft V5.7 (Bold Pixel Art), Blockcraft V5.6 / V4.0 (Painted), OG Classic 3.5, Vibrant Sunlit, and Retro 8-Bit.

WHAT'S NEW IN V5.8  (5.6 x Minecraft Fused Texture Engine for All Blocks, Tools, Weapons, Armor & Items)
  * 5.6 x MINECRAFT FUSED BLOCK TEXTURES (TexOverhaulV57). Fuses Blockcraft 5.6's organic FBM/Voronoi procedural block textures (TexPaint + TexPaint319) directly with classic 16x16 Minecraft pixel-art patterns and palettes: smooth FBM stone fused with soft horizontal Minecraft strata, 5.6 ore shading fused with Minecraft's iconic staggered ore vein layouts (Coal, Iron, Gold, Diamond, Redstone, Lapis, Emerald, Nether Quartz), 5.6 grass/dirt/leaves fused with Minecraft's grass dither, warm earthy pebble dirt, and jagged grass fringe, plus fused Oak/Spruce/Birch/Jungle/Acacia/Dark Oak planks & logs, Ashlar Stone Bricks, Clay Bricks, Crafting Tables, Furnaces, Chests, Bookshelves, Obsidian, Glowstone, Nether, and End blocks.
  * 5.6 x MINECRAFT FUSED TOOLS, WEAPONS, ARMOR & ITEMS (SELF-SHADED SILHOUETTES). Fuses Blockcraft 5.6's TexExtra + TexPaint319 item sprites with Minecraft's 16x16 stepped diagonal tool/weapon/armor silhouettes using self-colored material edge shading (top-left highlight and bottom-right darkened material edge, with zero heavy black border halos).

WHAT'S NEW IN V5.7  (Master Pixel-Art Texture Overhaul for Every Single Block, Tool, Weapon, Armor Piece & Item)
  * BRAND-NEW PIXEL-ART TEXTURES FOR EVERY BLOCK (TexOverhaulV57). Hand-crafted 5-tone pixel-art overhaul across every terrain block, wood species, leaf canopy, ore vein, mineral block, crop, work block, Nether/End block, and Redstone component: lush multi-blade grass tops & deep hanging grass skirts, stratified FBM stone & 9-cell Voronoi cobblestone, 5-tone faceted mineral ore clusters in recessed stone sockets, deep bark-fissured logs & 4-board wood planks with nail joints, and redesigned Crafting Tables, Furnaces, Chests, Bookshelves, TNT, Obsidian, Glowstone, Sea Lanterns, Pistons, Observers, and Daylight Sensors.
  * BRAND-NEW PIXEL-ART SPRITES FOR EVERY TOOL, WEAPON, ARMOR PIECE & ITEM. All 25 tools and weapons (Swords, Pickaxes, Axes, Shovels, and Hoes across Wood, Stone, Iron, Gold, and Diamond tiers), all 16 armor pieces (Leather, Iron, Gold, and Diamond Helmets, Chestplates, Leggings, and Boots), and every food, ingot, gem, bucket, bow, arrow, tactical CS2 weapon, Murder Mystery knife, C4 bomb, and Siege Cannon/Mortar sprite now feature custom 5-tone pixel art with crisp dark 1px outlines and top-left specular bevels.

WHAT'S NEW IN V5.1  (Clean Self-Contained Multi-Level Maps Without Cutoffs/Fusion, 3D Vertical AI Stair/Jump-Pad/Drop Pathfinding & Anti-Block-Spam, Multiplayer Player 2 Map Synchronization)
  * CLEAN, SELF-CONTAINED MULTI-LEVEL MAPS WITHOUT CUTOFFS OR FUSION. Rebuilt all 5 minigame map generators (Battle, Tumble, Bedwars, CS2 Strike, and Murder Mystery) across all 8 maps (Cove, Crucible, Cavern, Temple, Lair, Castle, Frontier, and Shrunk) so they no longer fuse old floating islands/walls with cut-off .mca perimeter rings. Every arena now features clean enclosing perimeter architecture, wide 2-to-3-block staircases with solid under-supports and open overhead stairwell cutouts (4+ blocks of headroom on every step), and Slime Block Jump Pads.
  * 3D VERTICAL AI PATHFINDING (UP & DOWN) & ANTI-BLOCK-SPAM. Added a 3D vertical connector resolver (SkyGen.resolveVerticalWaypoint3D) and upgraded the 25x25 3D BFS pathfinder (SkyGen.findLocalBfsStep & SkyGen.canBotWalkThrough3D) so AI bots naturally climb rising staircases, use Slime Block jump pads and ladders to reach upper floors, and navigate descending stairs and safe stairwell/balcony drop-offs (1..7 blocks down) to descend to lower floors. Disabled block-spamming in enclosed arenas (Battle, CS2 Strike, Murder Mystery, Tumble) and added column-floor detection (SkyGen.hasAnySolidColumnFloor) in Bedwars so bots only bridge over actual void gaps.
  * MULTIPLAYER PLAYER 2 MAP SYNCHRONIZATION. Fixed Player 2 map desyncs across all 8 maps (0..7) by passing hostMapId directly into Game.joinMultiplayerWorld on ASSIGN, replacing legacy (& 3) map masks with Math.floorMod(..., WorldSettings.MAP_COUNT), broadcasting MATCH START and MATCH RESET before stocking chests and spawning bots, and guarding asynchronous chunk generation against generator swaps.

WHAT'S NEW IN V5.0  (8 Ported Console Edition Maps & Multi-Level Architecture Across All 5 Gamemodes, Truly Randomized Chest Loot, Redesigned Tactical Crosshair & Animated Hitmarker)
  * 8 PORTED CONSOLE EDITION MAPS & MULTI-LEVEL ARCHITECTURE ACROSS ALL 5 GAMEMODES. Expanded the Arena Map roster to 8 iconic Console Edition maps (Cove, Crucible, Cavern, Temple, Lair, Castle, Frontier, and Shrunk) backed by a compressed 1.36-million-voxel Console Edition arena bank (res/maps/console_arenas.bin) and bespoke multi-story architecture in every mode: Battle features full multi-elevation Console Edition structures and 12–17 authentic Console Edition chest placements per map; Tumble features 8 distinct 3-layer destructible colosseums with raised terraces and outer towers; Bedwars features 2-story team fortresses, 3-tier center emerald citadels, and 2-story diamond outposts; CS2 Strike features 8 distinct tactical maps with 2-story buildings, sniper windows, and underground underpasses (Y_DECK - 4); and Murder Mystery features 3-story complexes (underground crypts/cellars at Y_DECK - 4, 1st floor halls, 2nd floor mezzanines at Y_DECK + 5, and 3rd floor attics/belltowers at Y_DECK + 9).
  * TRULY RANDOMIZED CHEST LOOT. Replaced static chest arrays with a tier-weighted, per-round randomized loot engine (SkyGen.fillRandomMinigameChest) that scatters randomized weapons, armor pieces, bows/arrows, tactical items, food, golden apples, and building blocks across random chest slots (0..26) for every chest in Battle, Tumble, Bedwars, CS2 Strike, and Murder Mystery.
  * REDESIGNED TACTICAL CROSSHAIR & ANIMATED HITMARKER ("HIT THINGIE"). Replaced the old 1px inverted crosshair with a crisp dark-outlined dynamic-gap crosshair (center precision dot, dynamic movement/recoil spread, enemy target-lock tinting, and ranged weapon coloring) surrounded by an animated 4-corner diagonal 'X' hitmarker ("hit thingie") that pops on every melee/ranged/gun hit and flashes golden-amber on critical hits and crimson-red on kills.

WHAT'S NEW IN V4.8 & V4.9  (3D Local BFS Grid Pathfinder & Wall-Slide Physics, Tactical Tracker Compass, Multi-Tab Bedwars Shop, Audio/Multiplayer Fixes, TNT/Fireball-Jumping & Polish)
  * 3D LOCAL BFS GRID PATHFINDER & WALL-SLIDE PHYSICS. Added a zero-allocation 3D Local Breadth-First Search (BFS) Grid Pathfinder (SkyGen.findLocalBfsStep) over a 13x13 neighborhood around each AI bot so bots route cleanly around wide walls, L-shaped corners, and enclosed doorways; upgraded Mobs.canStepUpOnCollision so bots only step-jump when headroom is clear and the obstacle is a true 1-block step-up (sliding smoothly on the ground along 2-block walls instead of bonk-jumping in place); and expanded tryBreakBlockingObstacle to 2-block reach.
  * TACTICAL TRACKER COMPASS & 3-TAB BEDWARS SHOP. Right-clicking a Compass now provides real-time tactical tracking in every mode (nearest enemy player/bot & intact enemy Bed in Bedwars, planted C4/Bomb Sites/enemy in CS2 Strike, dropped Sheriff Pistol/Gold/target in Murder Mystery, nearest fighter HP & distance in Battle/Tumble, and World Spawn + Home Bed + Biome in Survival). Added a 3-tab Bedwars Shop ([Items], [Gear & Upg], [Tools & Def]) with Iron Pickaxe, Iron Axe, Shears, End Stone, Obsidian, Fire Charges, Golden Apples, and permanent Iron/Diamond Armor persistence across respawns.
  * 3D AUDIO & REDSTONE MECHANICAL SOUND FIXES. Fixed 6 silent sound effect keys across CS2 Strike, Murder Mystery, and AI Golden Apple healing (player gunshots, TNT frag grenade & C4 fuse ignition, grenade detonations, and Golden Apple bites), added fallback sample aliases in SoundEngine, enabled Gareth Coker's Console Edition Minigame soundtrack in CS2 Strike & Murder Mystery, added 3D surface footsteps for walking mobs and remote multiplayer peers, and added 3D mechanical audio for Pistons, Levers, Buttons, Pressure Plates, and Redstone-powered Iron Doors.
  * MULTIPLAYER REMOTE PLAYER ANIMATION, ENDER PEARL & EXPLOSION FIXES. Fixed remote multiplayer peers initializing bow/eat pose values to 0.0 instead of -1.0 (which previously locked remote player arms and blocked arm swings, eating, and two-handed 3D gun aiming), fixed remote Ender Pearls so they never teleport or damage the local player on impact, and added 3D speed-of-sound explosion audio for remote multiplayer explosions.
  * MINIGAME PLAY-NOW RACE FIX, ARENA PROTECTION, SLIME JUMP PADS & TNT/FIREBALL-JUMPING. Fixed a first-tick world-reference race when clicking "Play Now" in the Minigames Hub that previously reset matchState and wiped Murder Mystery / CS2 loadouts on tick 1; fixed double-counted melee/bow kills on AI bots; anchored Bedwars Shopkeeper Villagers at their shop booths and made them invulnerable so they never flee into the void; added human player Slime Block Jump Pad launches across all minigame maps; added Bedwars TNT-Jumping and Fireball-Jumping with blast protection for permanent islands and beds; and made eliminated Bedwars AI bots drop their carried Iron, Gold, Diamonds, and Emeralds instead of Rotten Flesh/Bones.

WHAT'S NEW IN V4.2  (Minecraft Legacy Console Edition Soundtrack, Gareth Coker Battle Mini Game Themes for Skywars & Bedwars, Real-Life 3D Physical Acoustics, Realistic Foley Sound Effects, Backrooms & Stalker v2.3 & Full Gamemode Suite)
  * MINECRAFT LEGACY CONSOLE EDITION SOUNDTRACK (C418). Replaced the Animal Crossing music completely with the iconic Minecraft Legacy Console Edition soundtrack by C418: Moog City 2 on the Title Screen, Menus & Minigame Waiting Lobby, Subwoofer Lullaby (Morning), Minecraft (Midday Survival), Aria Math (Creative Mode), Wet Hands (Afternoon), Sweden (Sunset/Dusk), Living Mice (Night), Dead Voxel (The Nether), Boss (The End), and Alpha on the End Poem & Credits screen, plus authentic Legacy Console Edition UI press and item pickup sounds.
  * GARETH COKER BATTLE MINI GAME THEMES FOR SKYWARS & BEDWARS. Skywars and Bedwars matches now feature Gareth Coker's high-energy Minecraft Console Edition Battle Mini Game themes (Battle Theme 1: Double Time / Shrink, Battle Theme 2: Nock / Growth, and Battle Theme 3: Swift), automatically rotating through all three arena suites across matches.
  * REAL-LIFE 3D PHYSICAL ACOUSTICS & 66 REAL FOLEY SOUND EFFECTS. Binaural spatialization (ITD + ILD + head-shadowing), voxel ray-cast sound occlusion through solid walls, 14-ray 3D room/cave Schroeder reverb & canyon echo, speed-of-sound propagation delay (343 m/s), Doppler shift, underwater low-pass filtering, and 66 real recorded foley sound effects for footsteps, block digging/breaking/placing, doors, chests, fire, water, lava, TNT, and combat, with full attribution in the End Credits (/credits).

WHAT'S NEW IN V4.0  (New Edition V4.0, Dedicated Multiplayer Menu & Auto-Server Launcher, Survival PvP & Dropped Item Sync, Complete Redstone Expansion with Pistons/Repeaters/Observers & 3D Connected Wire, Full 28-Biome & Modern Structure LOD)
  * NEW EDITION V4.0 & DEDICATED MULTIPLAYER MENU. Blockcraft enters major Edition V4.0! The Title Screen is streamlined with Singleplayer and a dedicated Multiplayer menu (removing redundant Create New World from the title screen), where you can enter any server Host:Port, join LAN hosts automatically, host your world on LAN, or click "Start Server.bat" to automatically launch Blockcraft-server.bat / ServerMain in the background and connect immediately.
  * MULTIPLAYER SURVIVAL PVP & DROPPED ITEM SYNC. Players in Survival mode can now engage in melee PvP combat against other connected players with full weapon damage, attack cooldown, critical hits, armor reduction, knockback, hurt flash and death announcements. Dropped items (from Q throws or breaking blocks) and item pickups are synchronized live across all connected players.
  * REDSTONE OVERHAUL, FIXES & NEW COMPONENTS. Placing large grids of Redstone Dust no longer crashes or lags (replaced recursive neighbour updates and per-wire block relighting with an O(N) BFS network relaxation pass and batched chunk re-meshing). Redstone Dust now has a distinct inventory icon, opaque 3D connected wire lines that dynamically form crosses, straight lines or corners towards adjacent components (including 1-block step-up/down), and self-lit glowing red power states. Added Redstone Torch NOT-gate inversion, Redstone Repeaters (directional diode + 15-power booster), Pistons & Sticky Pistons (pushing up to 12 blocks and pulling blocks back), Note Blocks, Daylight Sensors, and Observers.
  * DISTANT TERRAIN (LOD) FOR ALL 28 BIOMES & NEW STRUCTURES. Distant LOD tiles now support all 28 biomes (Meadow, Flower Forest, Birch Forest, Dark Forest with giant mushrooms, Taiga/Mega Taiga/Snowy Taiga, Swamp, Savanna acacia, Jungle trees, Desert/Badlands cacti & terracotta bands, Mushroom Islands, Ice Spikes & Frozen Peaks) and render modern Update 3.14/3.19 village templates and biome landmarks (Desert Pyramids, Jungle Temples, Witch Huts, Igloos, Ruined Watchtowers) instead of legacy Update 3.5 village boxes.

WHAT'S NEW IN UPDATE 3.19  (discrete GPU PE exports, LOD speedup, Better Villages & biome landmarks, complete modern texture overhaul, raised player arms & fluid dragon animation, bushy leaves & colour looks, Redstone & 1.12 blocks, zero distant block seams, prototype P2P/LAN & standalone server multiplayer)
  * DISCRETE GPU SELECTION & PERFORMANCE. Both Blockcraft.exe and a dedicated JRE copy (jre\bin\blockcraft-gpu.exe) now carry native PE32/PE32+ export tables exporting NvOptimusEnablement = 1 and AmdPowerXpressRequestHighPerformance = 1 alongside DirectX UserGpuPreferences registry entries and NVIDIA/AMD OpenGL driver offload hints, and every start on an integrated GPU automatically relaunches onto blockcraft-gpu.exe. Distant LOD terrain now renders after near terrain (for hardware Early-Z rejection) with streamlined octree subdivision and far-tree geometry.
  * DISTANT BLOCK GAPS FIXED. Block atlas mipmaps (levels 1..3) are now downsampled per 16x16 tile on the CPU so adjacent tiles never bleed across borders, shader UVs are clamped inside each tile's half-texel safe zone, and chunk section origins are grid-snapped.
  * BETTER VILLAGES & BIOME LANDMARKS. Villages gain five new multi-story and civic templates (Inn/Tavern, Bakery, Windmill, Town Square Gazebo, Archery Range) with deeper terrain foundations, and the Overworld generates five biome-specific landmarks with loot chests: Desert Pyramids, mossy Jungle Temples, Swamp Witch Huts, Snowy Igloos and Ruined Watchtowers.
  * COMPLETE MODERN TEXTURE OVERHAUL. Every remaining block and item sprite in the 512x512 atlas has been repainted in a cohesive modern pixel-art style with directional shading and bevels.
  * ANIMATION OVERHAUL. Player arms sit two pixels higher so the shoulders are flush with the top of the torso, and the Ender Dragon features 3D figure-8 wing strokes, full-body heave/surge/bank, a travelling 5-segment serpentine neck wave, head yaw/pitch tracking on turns and perches, articulated leg flex and a 3-axis tail whip.
  * BUSHY LEAVES & GRAPHICS OVERHAUL. Leaf blocks sprout 3D diagonal crossed foliage fronds (Options > Graphics > Bushy Leaves, ON by default) with wind sway and warm subsurface sunlight scattering, alongside six colour-graded looks (Graphics > Look), bloom and ACES filmic tonemapping.
  * REDSTONE & MISSING 1.12 BLOCKS. Redstone Dust, Redstone Wire (15-step signal propagation), Block of Redstone, Redstone Torch, Redstone Lamp, Lever, Stone & Wooden Buttons, Stone & Wooden Pressure Plates (activating lamps, Iron Trapdoors, doors, fence gates and TNT), plus Slime Block (with bounce physics and zero fall damage), Sea Lantern, Polished Granite/Diorite/Andesite, Smooth & Chiseled Sandstone / Red Sandstone, and Chiseled, Mossy & Cracked Stone Bricks.
  * PROTOTYPE MULTIPLAYER (P2P / LAN & STANDALONE SERVER). Host your current world directly from the Pause Menu ("Host LAN") or with /host [port], and join a friend on LAN or localhost with "Join LAN" or /join [host:port]. Or run Blockcraft-server.bat to start a standalone headless server on your PC! Synchronizes world time, live block edits, chat messages and 20 Hz 3D animated remote players.

WHAT'S NEW IN UPDATE 3.18  (the real graphics card, less work per picture, a frame timer, the first repainted textures, a new villager, blaze, player body and dragon)
  * THE REAL GRAPHICS CARD. A laptop usually has two graphics chips: a weak one inside the processor (Intel UHD / Iris, AMD "Radeon Graphics") and a real graphics card (GeForce, Radeon RX). Windows hands a program
    the weak one unless the program is on its list, and a Java game never is - so the whole game ran on the weak chip. Blockcraft now puts its own Java (javaw.exe, java.exe) on that list - "Settings > System > Display >
    Graphics" - as "High performance" the first time it starts (it writes the same registry entry that page writes; nothing is installed). A program picks its chip when it starts, so if that very first start still lands on the
    weak chip the game starts itself once more (you see nothing of it, the window opens once) and the second start gets the real card. The video options have a switch "Fast GPU" (ON by default); F3 shows
    "GPU: <the chip that draws>" so you can see which one you got. If your Windows is older than the April 2018 update, or the registry is locked, nothing changes. If you want to do it by hand: Settings > System > Display >
    Graphics > "Add a desktop app" > Browse to  Blockcraft\jre\bin\javaw.exe  > Options > "High performance" > Save.
  * LESS WORK FOR EVERY PICTURE (the same picture, less drawing - nothing you will see changes, except the frame rate):
      - The SHADOW MAP no longer draws the whole neighbourhood of 19 x 19 chunks into a map that only holds 168 m x 168 m (and no sections that lie buried under the ground): 43 - 45 % fewer vertices go through the card when
        it is drawn in the test scenes, and it is drawn when it has to be - the player has walked 12 m, the sun has turned a third of a degree, terrain near you was built or changed, 150 frames have passed - not every 30 frames whatever happened.
      - HIDDEN SECTIONS ARE NOT DRAWN. Every 16 x 16 x 16 piece of the world knows which of its six faces are joined by open space (air, water, glass, leaves); each frame the game walks outward from you through those
        openings and draws only what that walk reaches. The caves, tunnels and the rock under a hill stay out of the picture: in a test forest and on a test mountain about 45 % fewer pieces (draw calls) and 10 - 26 % fewer
        vertices were drawn, with the same picture (32 000 test rays from cameras on the ground, in the air and in caves never ended in a piece that was left out). Spectator mode and a camera inside a block draw everything as before.
      - RENDER SCALE (video options, 50 - 100 %): the world is drawn smaller than the window and stretched to it - the quickest way to more frames on a weak chip, with a softer picture; the menus and the text stay sharp.
        Needs the shaders (Graphics > Shaders ON). MAX FPS (video options): stops the game from drawing more than 30 / 60 / 90 / 120 / 144 / 165 / 240 pictures a second so that a fast card rests; the menus never draw more than 60,
        a window in the background 30, a minimised one 10.
  * A FRAME TIMER. Shift+F3 shows where a frame goes: for every part of the picture (update, sky, distant terrain, hidden-section walk, shadow map, terrain, mobs, water, clouds, effects, hand, post effects, interface,
    swap) how many milliseconds the game thread and - read from the graphics card's own clock - the card needed, on average and in the worst frame of the last second, plus how many vertices and sections were drawn and how
    much garbage a frame leaves behind. Ctrl+F3 makes every part wait for the card (exact, slower). While it is open one line a second is written to  %USERPROFILE%\.blockcraft112\perf.log  (a CSV file you can open in Excel
    - send it to whoever is making the game faster for you).
  * FIRST REPAINTED TEXTURES. Stone, cobblestone, dirt, sand, gravel, grass, logs, leaves, planks, ores, snow, wool, sandstone, glowstone, obsidian, stone bricks, mossy cobblestone, the crafting table, the furnace, the
    chest and the spawner are painted again (same places in the atlas: nothing in your worlds changes), the water and the lava have new frames, and the ground stays sharp where you look along it (anisotropic filtering).
  * A NEW VILLAGER AND A NEW BLAZE (the two that looked worst). The villager has a robe that flares towards the ground, boots, folded arms with elbows and hands, ears, a long hooked nose, a heavy brow and hair, and an outfit for
    each profession - the farmer's straw hat, the librarian's glasses, the priest's purple cap, the blacksmith's apron, the butcher's apron - painted on a skin with twice the old resolution. The blaze has a crown of flames on its head,
    flares, a glowing ember core, rods made of segments that circle round it and embers that rise off it; its flames flicker.
  * A NEW BODY FOR YOU (F5 shows it). Your legs have hips and knees and your arms have shoulders and elbows, and the body leans, twists and rolls about the hips while the head keeps looking where you look. It walks with real steps and
    swinging arms, runs leaning forward, crouches with bent knees and a rounded back when you sneak, tucks the legs up in a jump and bends them on landing, swims (crawl: the arms go over the head), treads water, climbs a ladder hand over
    hand, flies with the legs trailing, swings the arm with the shoulder when you hit, raises the food to the mouth when you eat, draws the bow with one arm out and one pulled back, and flinches when it is hurt. One state fades into the next - nothing jumps.
  * A DRAGON THAT FLIES. Each wing has a shoulder, an elbow, a wrist and three fingers with the membrane between them. The beat is 60 % down and 40 % up, every joint a little behind the one before it, and the wing folds in
    on the upstroke; the dragon banks into its turns, points its nose up in a climb and down in a dive, its neck of five pieces and its tail of nine wave one after the other, the legs dangle, and on the perch it stretches its wings.
  * Honest notes. 1,654,000+ checks run without a screen (the rays above, the connectivity of every kind of tunnel, the registry commands against a pretend registry, the frame timer against a clock that is moved by hand, every joint
    angle of the new body, every clip of the new villager, blaze and dragon for finite, smooth numbers). The culling and the shadow map were compared picture against picture in the real game with a software renderer, the new bodies
    were drawn by a small software viewer and by the real game, and the start of a second process was tried in the real Blockcraft.exe on a Windows test machine. NOBODY has run any of it on a computer with two real graphics
    chips: whether Windows really hands the game to your graphics card, how many frames the changes bring there, whether the frame timer's numbers agree with your task manager, how the new walk feels in your hands - all a first
    guess. Press Shift+F3 and F3 and look at "GPU:". Still no sound. Not yet: redstone, brewing, enchanting, banner patterns, temples, igloos and shipwrecks, new textures for most blocks and items, new bodies for the other mobs
    (iron golem, ghast, pigman ...).

WHAT'S NEW IN UPDATE 3.17  (new villages, new block shapes, beds and banners in 16 colours, a new sky, mobs with jointed bodies)
  * NEW VILLAGES (worlds you create with any world type except CLASSIC - older worlds keep their villages, so nothing you built moves). A village is planned on a grid of streets now: a main street with
    side streets laid over the ground wherever it goes, a well in the square and, on both sides of the streets with their fronts to the road, more than a dozen kinds of buildings - huts, cottages in three sizes (with flat roofs
    in the desert), a library, a smithy, a butcher's, a long house, a barn, farms with crops, a market stall, an animal pen, a watchtower, a church, a shed and a garden - on a foundation, with timber
    frames, windows, doors, beds, tables, furnaces and chests inside, lamp posts along the streets. The wood and the stone follow the biome: oak and cobblestone in the plains, sandstone in the desert, acacia in the savanna,
    spruce in the taiga, spruce with snow on the roofs in the cold. No two houses of a village look alike (every building has variants of its roof and its colours), and a village is a function of the seed, so a chunk
    you walk into can always be drawn again.
  * NEW BLOCK SHAPES like Minecraft 1.12's: GLASS PANES (clear and in the 16 colours: 6 glass make 16 panes) and IRON BARS (6 iron ingots make 16) that grow arms towards their neighbours, WALLS (cobblestone, mossy cobblestone,
    stone brick, brick, sandstone, red sandstone, nether brick; 6 blocks make 6 walls) that join their neighbours, the FENCES of every wood (not only oak), FENCE GATES and TRAPDOORS of every wood and an IRON TRAPDOOR
    (no redstone yet: the iron one cannot be opened), and CARPETS in 16 colours (2 wool make 3). Gates and trapdoors open with a right click; trapdoors can sit in the top half of a block; the wooden ones are fuel in a furnace.
  * BEDS IN 16 COLOURS: three wool of one colour over three planks make a bed of that colour - a wooden frame, a blanket in the colour and a pillow at the head (the old red beds in your worlds are the red ones).
    BANNERS in 16 colours: six wool of one colour over a stick; they stand on the ground or hang on a wall (patterns are not in yet).
  * A NEW SKY. The colours of the sky are worked out from the height of the sun and the direction you look in: a deep blue zenith, a pale horizon, an orange belt on the sun's side and a pink arch with the blue shadow of the earth on the
    other side at dusk and dawn, and a glow round the sun that the fog of the terrain and of the distant terrain wears as well, so the far hills melt into the sky without a seam. At night 1,800 stars of different brightness and colour
    twinkle with a band of the Milky Way and the MOON goes through its eight PHASES and lights the sky a little. Under water the picture turns blue and murky.
  * THINGS THAT MAKE A PLACE FEEL ALIVE: leaves that come off the trees and drift down (in the colour of the biome), fireflies over the grass on warm nights, embers rising from the lava of the Nether, flame and smoke on torches,
    dust under a sprinting player on dirt, sand, gravel and snow, a splash when you jump into water and bubbles when you are under it. They fade and shrink (and some glow) instead of just disappearing.
  * MOBS WITH JOINTED BODIES. Every one of the 18 mobs has a new skeleton of bones (legs and arms with elbows and knees, necks, jaws, ears, tails, wings, tentacles) and a new skin with twice the old resolution, painted by
    the program so that patterns run on over the edges of the boxes: the patches of the cow, the wool of the sheep, the vines of the iron golem, the robes and hats of the villagers (farmer's straw hat,
    librarian's glasses, the priest's purple cap, the blacksmith's apron and the butcher's), the glowing eyes of the enderman and the blaze, the ghast that opens its face to shoot, the dragon with its jointed wings. They are animated by
    clips: walking with real steps (the legs bend), breathing and blinking when they stand, grazing, the attack of a zombie, a golem or a ghast, the way a hit throws them back and the way they fall over when they die.
  * Honest notes. 1,600,000+ checks run without a screen (every building template, that every village is a function of its seed, the shape and collision of every new block, every colour of bed and banner, the colours of the sky, every clip
    of every mob for finite, smooth numbers), and pictures of all of it were looked at in a software renderer and in the real Blockcraft.exe on a Windows test machine. NOTHING has been played on a real graphics card or by a
    person: how the villages feel to walk through, whether the stars twinkle nicely at your frame rate, how many mobs with jointed bodies your computer draws at once - all a first guess. Still no sound. Not yet: redstone, brewing,
    enchanting, banner patterns, temples, igloos and shipwrecks, a new player body with elbows and knees, new textures for the old blocks.

WHAT'S NEW IN UPDATE 3.13  (a new world, new menus, a new inventory)
  * A NEW WORLD GENERATOR. Five climate maps (warmth, wetness, how far inland, how rugged, how weird) decide among 28 BIOMES: plains, meadows, forests (oak, birch, dark oak with giant mushrooms, flower forests),
    taigas (spruce, snowy, giant two-by-two spruces on podzol), swamps (lily pads, vines, dark water), savannas (acacia), jungles (giant trees, vines, bushes), deserts, BADLANDS (bands of red, orange and yellow
    terracotta on red sand), snowy plains, ICE SPIKES, frozen peaks, mountains, MUSHROOM FIELDS (giant mushrooms, mycelium), beaches (sand, snow, stone shores), oceans (cold, warm, frozen, DEEP with a sea floor of
    gravel and clay) and RIVERS that cut through the land. Mountains are real mountains with snow on the top, the land has hills and valleys, caves open to the surface, and CANYONS (ravines) cut down
    to the lava. The tint of the grass, the leaves and the water follows the biome, rain falls only where it rains (deserts and badlands are dry, snow falls in the cold), and the distant terrain (LOD) shows it all.
  * WORLD TYPES (choose them when you create a world): DEFAULT (the new terrain), LARGE BIOMES (the same, four times as big), AMPLIFIED (towering mountains, deep valleys), SUPERFLAT (four presets: grassland,
    desert, snow, sandstone) and CLASSIC (the terrain of Update 3.5 - 3.12). Every world you made before 3.13 stays CLASSIC, so nothing you built moves. The type is saved with the world.
  * 70 NEW BLOCKS the new world needs: granite, diorite, andesite (also craftable), red sand and red sandstone, podzol, mycelium, coarse dirt, ice and packed ice, a thin cover of snow, planks of every wood, logs and
    leaves of jungle, acacia and dark oak, ferns, dead bushes, ten more flowers, brown and red mushrooms, tall grass, large ferns, sunflowers, lilacs, rose bushes and peonies (two blocks high), lily pads (they float on
    water), vines (they cling to walls and trees; shears) and the blocks of the giant mushrooms. Every recipe takes every kind of planks; a log gives the planks of its own wood.
  * THE TITLE SCREEN has a live background: a camera turns slowly over a landscape (a real piece of the world generator, always the same place), the logo, a splash text, Singleplayer, Options, Quit.
  * SINGLEPLAYER lists your worlds as in Minecraft: a little map of each world's spawn, the name, the mode, the world type, the seed and how long ago you played. CREATE NEW WORLD has the name, the seed
    (empty = random), the game mode, the world type with a one-line description, the superflat preset, a switch for STRUCTURES (villages, strongholds, dungeons, mineshafts) and one for a BONUS CHEST (a chest
    next to your first steps with an axe, a pickaxe, wood, bread, apples and torches), and a MAP PREVIEW of the terrain the seed makes (it follows the type: try a few seeds before you start).
  * OPTIONS are sorted into four tabs - VIDEO (render distance, FOV, distant terrain, brightness, GUI scale, VSync, fullscreen), GRAPHICS (shaders, smooth lighting, clouds, leaves, weather), GAMEPLAY (mouse
    sensitivity, invert mouse, view bobbing, disasters) and CONTROLS (the keys, for reading) - each with a line of help for the button you point at. The pause menu shows the world and its mode, and the loading screen
    shows what it is doing.
  * THE CREATIVE INVENTORY has TABS with pictures (Building Blocks, Colors, Nature, Decoration and Utility, Tools and Combat, Food and Farming, Materials) and a SEARCH tab (type, it filters as you type); new blocks
    land in the right tab by themselves. TOOLTIPS tell more: the name, durability, attack damage, armour points, food value, fuel time and what an item smelts into. Number keys put a picked item in the hotbar; Q drops.
    The menus are laid out for every window size (they were checked at seven sizes) and the GUI scale never goes above what fits.
  * Spawn: the first step in a snowy or grassy place no longer puts you ten blocks away from the spawn point (snow and tall grass were taken for a wall); in a world with Large Biomes the search for dry land goes on
    for up to 4000 blocks (it used to give up after 900 and drop you into the sea).
  * Honest notes. 1,240,000+ checks run without a screen (the terrain of every type for many seeds, that every column of the distant terrain is solid ground, that you always spawn on land, every menu at seven window sizes,
    the inventory, the bonus chest), and pictures of the terrain and every new menu were looked at in a software renderer. NOTHING has been played on a real graphics card or by a person: how the new terrain feels,
    whether the panorama is smooth, how fast big oceans and jungles draw - all of that is a first guess. The keys cannot be changed yet. Still no sound. Villages stand in plains, deserts, savannas and taigas but still
    look the same everywhere - that, temples, shipwrecks and more are the next update.

WHAT'S NEW IN UPDATE 3.12  (the Nether, the End, villages, a world of colour)
  * THE NETHER. Build a frame of obsidian (at least 4 wide and 5 high, up to 23 x 23, the four corners may be left out) and light it from the
    inside with Flint and Steel: the inside turns into a purple portal. Stand in it for four seconds (in creative you go at once) and you
    arrive in the Nether, where one block is eight blocks of the Overworld; a portal that is not there yet is built for you, and the way
    back works the same. The Nether is a cave world between a bedrock floor and a bedrock ceiling: a sea of lava, netherrack islands
    and cliffs, glowstone hanging from the ceiling, soul sand and gravel on the shores, nether quartz ore in the walls, magma blocks
    where the rock meets the lava. NETHER FORTRESSES (seven out of ten regions of 256 x 256 blocks have one) are dark castles of nether brick over the lava: bridges
    with railings, covered corridors with windows, pillared halls, closets with chests and a throne room with a blaze spawner. Mobs: ZOMBIE PIGMEN
    (calm until you hit one - then the whole group comes), GHASTS (float about and throw big fireballs that blow holes in the ground and start fires),
    BLAZES (hover and shoot bursts of small fireballs; they drop blaze rods) and MAGMA CUBES (hop at you and split into smaller ones when killed).
    Small rules like Minecraft's: fire on netherrack never goes out, soul sand slows you down, a magma block burns your feet unless you sneak. New items: nether quartz (and a block of it), nether brick (and the fence), blaze rod / powder, ghast tear, gold nugget
    (9 to an ingot), ender pearl, eye of ender, emerald. The command /dim nether|end|overworld takes you there at once.
  * THE END. Strongholds - buried castles of stone brick with libraries, closets with chests and a portal room - wait underground in
    three rings round the middle of the map (3 at 640 to 960 blocks, 6 at 1500 to 2100, 10 at 2800 to 3400). Craft EYES OF ENDER (ender pearl +
    blaze powder; pearls come from endermen), throw one (right click): it flies towards the nearest stronghold for a dozen blocks, hovers and drops
    (now and then it breaks). In the portal room twelve End portal frames stand round a pit of lava - put an eye into every frame (some hold one
    already) and the pit fills with the END PORTAL. The End is one island of end stone, about 110 blocks across, floating in the void, with TEN OBSIDIAN
    PILLARS (an End crystal on top of each: it heals the dragon, hit it and it blows up) and THE ENDER DRAGON (200 health, boss bar at the top of the
    screen): it circles over the island, swoops at you and breathes purple fireballs that leave a cloud that hurts, and now and then lands on the
    bedrock fountain in the middle where you can hit it. Kill it and an exit portal opens (back to the Overworld) and a dragon egg is laid. ENDERMEN
    teleport, get angry when you look at them and are hurt by water. Falling into the void kills you. ENDER PEARLS can be thrown (right click): they fly in
    an arc and you appear where they land - for 5 half hearts of damage that no armour stops.
  * VILLAGES: besides the houses, wells and churches there are now smithies (two furnaces and a chest), FARMS (fenced, with a water channel and four rows of
    wheat, carrots and potatoes) and lamp posts along the paths. VILLAGERS (farmer, librarian, priest, blacksmith, butcher, and the green nitwit; each in a robe of his
    colour) wander around the village and run from monsters, IRON GOLEMS keep watch and beat up monsters. Right click a villager to TRADE: emeralds are the money.
    The farmer buys wheat, potatoes and carrots and sells bread, apples and cake; the librarian trades paper, books, bookshelves and glass; the priest sells bone meal, glowstone, ender pearls and
    blaze powder; the blacksmith buys coal and iron ingots and sells an iron axe, sword and chestplate and a diamond pickaxe; the butcher trades raw and cooked meat. Trades can be repeated as often as you like.
  * THE WORLD OF COLOUR (Minecraft 1.12). 15 DYES: red from roses, yellow from dandelions, black from coal, green from smelted cactus, blue from lapis lazuli ore (4 to 8 per ore),
    bone meal is the white one; mixing two dyes makes two of the next colour (orange, magenta, light blue, lime, pink, grey, light grey, cyan, purple, brown). Dye wool (one dye + one wool), SHEEP (right click with a dye: the
    wool that grows back has that colour), CONCRETE POWDER (4 sand + 4 gravel + a dye = 8; it falls like sand and turns into CONCRETE the moment water touches it), STAINED TERRACOTTA (8 terracotta
    around a dye; terracotta is smelted clay), GLAZED TERRACOTTA (smelt stained terracotta: four patterns, each in the colour of its dye) and STAINED GLASS (8 glass around a dye - it is translucent).
    Also: clay (in the beds of lakes and the sea), blocks of iron, gold, diamond, emerald, coal and lapis lazuli (9 ingots, and back), emerald ore (deep under mountains).
  * ADVANCEMENTS: press L. 34 advancements in five tabs (Minecraft, Nether, The End, Adventure, Husbandry) as a tree with the Minecraft 1.12 titles ("Stone Age", "Isn't It Iron Pick", "We Need to Go Deeper",
    "Free the End" ...), a toast in the corner when you make one, saved with the world. /advancement shows how many you have, /advancement grant <id|all> and revoke all are for testing.
  * PARROTS: five colours (red, blue, green, cyan, grey) that flutter from tree to tree in forests and sit down now and then. Right click with SEEDS (wheat seeds, pumpkin or melon seeds; one in three works) to tame
    a parrot: it follows you, and a right click with an empty hand puts it on your SHOULDER (two at most; sneak + right click with an empty hand sends it off).
  * STORM CLOUDS without shaders now rise as domes with cauliflower tops instead of flat slabs.
  * TO TRY IT ALL QUICKLY (press T, type the command, Enter): /gamemode creative, then /dim nether (the same command with overworld takes you back), /dim end (the dragon
    is waiting; from the End the command takes you home), /give ender pearl 16 (right click throws one), /summon villager, /summon parrot, /summon enderman, /give red dye (and
    /give concrete, /give glazed ...: the creative inventory, key E, has every new block on its last pages), L opens the advancements (/advancement grant all gives you all of them).
    Villages stand on flat plains and deserts (at most one in every 320 x 320 block region of the map), nether fortresses are in about seven of ten regions of 256 x 256 blocks:
    in creative fly about (double-tap Space) and look for them.
  * Under the hood: block numbers are 12 bits wide (4096 blocks instead of 226), the texture atlas is 32 x 32 tiles, every dimension has its own folder in the world (DIM-1, DIM1), strongholds and fortresses are
    planned from the seed alone. Worlds from 3.5 - 3.11 open as before. Chunk files only get the new format when they contain a new block, but an older game cannot show the new blocks, so
    keep a copy of a world you want to open in an older version.
  * Honest notes. Everything that can be tested without a screen is: 879,000+ checks (every nether / End / village / colour rule, saving and loading, that every room of every fortress and stronghold can be reached on foot,
    the dragon fight from first hit to the portal), and the pictures of the Nether, the End, the strongholds, the villages, the new blocks and the weather were looked at in a software renderer. NOTHING has been played on
    a real graphics card or by a person: how hard the dragon is, whether the Nether is too dark or the fortresses too easy to find, how the villagers' trades feel, how fast the game runs with a big fortress in view - all of that
    is a first guess. The game still has no sound at all (no ghast cries, no dragon roar, no portal hum). Not in the game (yet): brewing, enchanting, redstone wiring, beds in colours, banners, End cities, endermites.

WHAT'S NEW IN UPDATE 3.11  (hands, water, distant terrain, animation)
  * FACES ARE SINGLE-SIDED, like in Minecraft. Go under the map in spectator mode (/gamemode spectator, F to fly): the ground is see-through
    from below, you see the sky, clouds, the undersides of trees and houses and the caves - not a textured ceiling. (Plants, crops, flames and
    the surface of water are still seen from both sides.) The sky no longer goes black when a spectator is underground.
  * DOORS HAVE BOTH SIDES. A door lost its visible big face whenever a solid block stood right against that side (a bug that was also in 3.5:
    the test for "is this face pressed against a neighbour" looked at the wrong side for two of the six faces). The same bug hid the riser of some
    stairs. Fixed for every door, stair, slab, fence and torch; checked for every door state with every combination of neighbours.
  * THE DISTANT TERRAIN (LOD) SHOWS WHAT IS REALLY THERE. It used to be the world as the generator made it, so a tower you built, a pit you
    dug or a forest you cut down was not there when you looked at it from far away (and the old forest came back). Now every chunk that was
    changed is summarised (the height and colour of what is on top, water depth, which trees still stand) when it is saved and two seconds
    after the last change; the distant terrain is rebuilt from that (a few seconds later, without a gap). Worlds from before 3.11 are
    scanned once in the background when you open them (a file lod.dat appears in the world's folder).
  * THE DISTANT TERRAIN NO LONGER FLASHES UP INSIDE CHUNKS THAT ARE DRAWN, for instance while trees burn: a chunk counts as covered
    once it has been drawn, and keeps counting while a part of it is being rebuilt. Where the real chunks stop, the distant tiles
    are always the small ones, so nothing big overlaps the near terrain.
  * HANDS LIKE MINECRAFT 1.12. The empty hand is your arm in your skin, reaching forward from the bottom right with the hand end
    towards the middle of the screen. Tools, food, torches and flowers are held as flat items with Minecraft's own position and angle,
    blocks as the little cube at the bottom right. The swing, the swap animation when you change the slot, eating (the food rises to
    your mouth and bobs) and the sway when you turn the camera use Minecraft's formulas; the hand also bobs with every step
    (switch: /bobbing on|off). In F5 view the item sits in the hand the way it does for other players in Minecraft (a sword points
    forward and up, flat items hang from the fist).
  * SHADERS: ANIMALS, MONSTERS AND YOU ARE LIT LIKE THE TERRAIN. With shaders on, mobs, dropped items, falling blocks, flying debris,
    your own body and your hand get the warm sun, the cool blue shade (also under trees: they receive the terrain's shadows), the glow of
    torches and other lights, and the same fog. Before, they were lit by one flat number.
  * SHADERS: BETTER WATER. The colour now depends on depth (clear turquoise and the sand visible through shallow water, deep blue
    further out), the bed is seen through the rippling surface (refraction), there is white foam along the shore, the ripples are
    four layers of noise drifting in different directions, the sun leaves a glitter path, and with "FANCY" the shore, trees and hills
    are mirrored in the water. Options -> "Shaders: OFF / ON / FANCY" (ON = everything except the mirrored terrain, which costs the most).
    If a graphics driver cannot run the new water program the game uses the old water by itself.
  * CLOUDS (shader sky): the cloud shapes bulge and lean with height instead of standing in columns, edges are eroded and wispy,
    thin edges glow towards the sun (silver lining), the bottoms are a bit greyer than the tops, a thin high layer of ice-cloud
    streaks shows in fair weather, and the walls of thunderhead towers are steeper.
  * ANIMATION (like Minecraft packs such as "Fresh Animations"): animals and monsters bounce and sway with every step, breathe when
    they stand, are thrown back when hit, nod while walking; pigs, cows and sheep now and then put their heads down and chew; chickens
    peck the air with every step; baby animals have bigger heads; legs spread when an animal jumps or falls. You: the shoulders turn against the hips
    when you walk, you lean forward when you sprint, and your arms go out in the air.
  * Honest notes. Everything above that can be tested without a screen is (the face winding of every block, all door
    states, the distant terrain's summaries, the saving and the rebuilding, the animation maths: 480,000+ checks), and the pictures
    were looked at from a software renderer and through the real Blockcraft.exe on a Windows test machine. What has NOT been tried
    is how it feels and runs on a real graphics card: the new water program in particular is the heaviest shader in the game.
    If it is slow, set Options -> Shaders to ON or OFF. Villages, the Nether and more 1.12 features are NOT in this update.

WHAT'S NEW IN UPDATE 3.10  (natural disasters and falling trees)
  * TREES FALL OVER. Chop through a tree's trunk (usually the bottom log) and the whole tree above the cut tips over like a felled
    tree: trunk, branches and crown fall away from you as one piece (another way if something is in the way - never onto you -
    and not at all if the tree is walled in), and lie on the ground. The log you cut drops as usual; the rest of the trunk stays in the world as
    LOGS ON THEIR SIDE that you harvest piece by piece (each gives a normal log, an axe is faster, they burn like logs). Cut
    higher up and the stump stays while the top falls and drops onto the ground. The crown comes down with the tree: leaves
    that land are still leaves (the ones that get squashed drop apples and sticks like broken leaves). Mobs - and you - under
    a landing trunk are hurt (3.5 hearts) and put on top of it. Only trees count: a log pillar, a cabin or anything without
    leaves does not fall. Switch: /treefall on|off (saved with the world).
  * LOGS PLACED AGAINST THE SIDE OF A BLOCK NOW LIE DOWN along that direction, like in Minecraft (on top of a block, or in tall
    grass, they still stand upright). The bark runs along a log on its side, the rings are on the ends.
  * NATURAL DISASTERS (they only start some minutes into a world and are rare; Options -> "Disasters: ON/OFF" switches them off
    in every world, /disaster off|on in one world - saved with it; the /disaster commands below still work when they are off).
    You are told in the chat when one starts by itself.
      TORNADOES form in thunderstorms 90-180 blocks away and head for your neighbourhood at about 2 blocks a second (you can walk
      away from one). A dark wall cloud with a grey funnel hanging from it, a dust skirt on the ground. Close to the funnel
      you are swirled around, drawn in and LIFTED (up to about 7-14 blocks), thrown out again, and hurt a little; animals and
      dropped items are carried too. It tears leaves, plants, torches, ladders, glass, wool, pumpkins, melons, cakes, hay bales and
      wooden furniture (planks, fences, doors, beds, crafting tables, bookshelves, wooden stairs and slabs) out of the ground and spins them around as flying
      debris that crashes down again further on (a lot of it as blocks again, some as splinters, and a flying plank hurts).
      TREES close to the funnel are uprooted and topple away from it. It never touches terrain, stone, bricks, ores, chests,
      furnaces or logs lying on the ground. It lives 2-4 minutes and ropes out.
      EARTHQUAKES shake the screen (stronger the nearer the middle), crumble loose terrain off cliffs and overhangs as falling
      blocks (dirt, grass, stone, sand, gravel, ores - never what you built), shake plants, torches, ladders and glass loose
      and topple trees. Radius 20-35 blocks, about 10-15 seconds; flat ground stays flat.
      METEORS streak across the night sky (some nights a whole shower) and hit with a blast the size of TNT or bigger, set fires around
      the crater and leave a few blocks of OBSIDIAN (mine it with a diamond pickaxe).
    COMMANDS: /disaster tornado [distance]  (a tornado forms that many blocks ahead of you, default 80, and heads your way, under
    a grey rainy sky),  /disaster quake [1-3],  /disaster meteor [count],  /disaster stop,  /disaster on|off,  /disaster (status).
  * The camera shakes when the ground does: earthquakes, a tree hitting the ground close to you, a meteor striking.
  * Honest note: everything is covered by automated tests (41,000+ checks: the rules, the numbers, save / load) and was looked
    at as pictures from a software renderer and through the real Blockcraft.exe on a Windows test machine. How it FEELS - the
    tornado's pull, the shaking, the frame rate with a big forest being shredded - has not been tried on real hardware, and
    the game has no sound at all (no thunder, no rumble, no crash of a falling tree). If a disaster is too much for your taste,
    Options -> Disasters: OFF.

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
  * SLABS AND STAIRS ON THE TOP HALF / UPSIDE DOWN: aim at the upper half of a block's side face, or at the underside of a block,
    and the slab or stair you place goes into the upper half of the cell (stairs upside down, like in Minecraft); the lower half
    of a side face or the top of a block gives the usual bottom slab / upright stair. Two slabs of the same kind in one cell make
    the full block - whichever halves they are: a slab on top of a bottom slab, a slab under a top slab, or one placed into the
    cell that already holds the other half (not while you stand where the block would be). Breaking them gives the normal item;
    middle-click in creative picks the normal item. (Wooden ones burn, like the upright ones.)
  * SMOOTHER MOVEMENT: water, lava and fire used to jump to their next picture ten times a second; the pictures are now blended
    so they flow at any frame rate. Falling sand, gravel and primed TNT move smoothly instead of stepping twenty times a second
    (a primed TNT also swells around its own middle now). The item in your hand lowers and the new one rises when you switch
    slots or when it is used up or replaced, like Minecraft's equip animation.
  * LESS HIDDEN GEOMETRY (performance): about a third of everything drawn in a forest were faces between two leaf blocks, mostly
    inside the foliage where nobody can see them. Options -> "Leaves": SMART (default; hides the faces between leaves that are
    completely enclosed by other leaves or solid blocks - about a tenth of all faces in a forest, and the picture stays the same),
    FAST (hides every leaf-to-leaf face: about a third fewer faces, the foliage looks a little thinner where the leaf texture has
    holes) or FANCY (everything, like before). Slabs and stairs next to other blocks are no longer black on their inner faces.
  * Smaller things: fog in the shader pack is measured around you instead of along the view direction (the far edge of the
    world is hidden from every angle); the game now really ends when you close it (a lingering helper could keep a
    "Blockcraft" process alive); the "Inventory" caption is gone from the crafting screen (the player stands there).
  * Honest note: skin handling, the model, the file dialog and the username download are covered by automated tests, and
    the real Blockcraft.exe was started on a Windows test machine to import a real account's skin over the internet and to
    open the file dialog; the building / lighting / fire / animation changes were checked as pictures from a software renderer
    and by automated tests (40,000+ checks). Dragging a file onto the window, the frame rate and how it all looks on a real
    graphics card have still not been tried on real hardware (so the performance gain of the Leaves option is measured in
    faces drawn, not in frames per second).

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
  Q             drop item       T or /      chat / commands           Esc  pause              L  advancements
  F1 hide HUD   F2 screenshot   F3 debug    F5 camera: first person / behind you / in front of you    F11 fullscreen
COMMANDS: /gamemode <survival|creative|adventure|spectator>  /time  /tp  /seed  /give <item> [n]
          /kill  /heal  /fly  /summon <mob>  /weather  /firespread  /treefall  /disaster  /bobbing
          /dim <overworld|nether|end>  /advancement  /help

Worlds, options and screenshots are stored in  %USERPROFILE%\.blockcraft112
