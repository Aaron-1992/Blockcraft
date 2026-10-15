# V6.7: aquatic worlds and Java-style circuits

This is an original implementation targeting familiar Java Edition 1.14-era
behavior, **not a claim of full vanilla parity**. Existing art and custom PNG
packs are retained. V6.6 downloads are immutable.

## Water and world generation

Create a **Default, Large Biomes or Amplified** world for the new generation.
Oceans vary by temperature and depth, with cold/lukewarm/deep-frozen variants,
underwater trenches, sandy/gravelly floors, kelp, seagrass, five coral families,
clustered reefs and icebergs. Rivers also receive seagrass. Generation is
coordinate/seed driven, including features spanning chunk boundaries.

Old saves without the new `aquatic` generation property load with it **off**,
including newly explored chunks. Existing chunks are never regenerated. Hosts
send the property before clients construct their generator. Classic and
minigame terrain retain their existing generators.

- Water transmits skylight with one level of attenuation per block; warm/cold
  seas have distinct underwater haze and visibility rather than uniformly dark blue.
- Water advances every five game ticks, reaches seven blocks horizontally,
  prefers nearby drops, falls, recedes when its supply is removed, and renews
  source blocks between two sources over a supporting block/source.
- Flow washes away dust, torches and other small replaceable components,
  dropping their items. Water/lava contact still produces the appropriate rock.
- Buckets fill/drain waterloggable slabs, stairs, fences, panes/bars, walls,
  trapdoors, ladders and chests. Breaking a wet shape leaves its water behind.
  Waterlogged wood does not burn. Ordinary flowing water cannot be bucketed.
- Sprint forward with your eyes underwater to swim. Looking up/down steers
  vertically. The collision pose is 0.6 blocks high; standing is restored only
  with enough clearance. Surface breathing respects water height. Flow carries
  the swimmer; soul sand/magma source-water columns lift/pull them respectively.

**Scope limits:** this does not recreate all vanilla aquatic mobs, structures,
loot, coral growth/death timing or block variants. Slab fluid geometry is clipped
to the empty half; other waterlogged shapes use simplified fluid surfaces.
Bubble-column forces are implemented, not a complete vanilla particle system.

## Redstone contracts

One redstone tick is **two game ticks** (0.1 seconds at 20 TPS).

| Component | Implemented behavior |
|---|---|
| Dust | 0–15 strength, one lost per edge; directional connections and output; step-up/down checks; connected-network traversal rather than a fixed-radius charge region |
| Solid blocks | Distinct direct/strong conduction; glass is not a conductor |
| Repeater | Rear input/front output; right-click selects 1–4 redstone ticks; delay survives save/power transitions and moves the model's adjustable torch; powered side diodes lock it |
| Comparator | Two-game-tick update; compare/subtract toggle with front indicator; analog container fullness, including through one solid block; recipe and creative item |
| Torch | Two-game-tick inversion; eight quick off transitions in 60 game ticks cause burnout, recovering after 160 ticks |
| Observer | Six orientations; watched-state change schedules a two-game-tick delay and two-game-tick rear-only pulse |
| Lamp | Immediate on; four-game-tick off delay |
| Buttons | Stone 20 game ticks; wood 30 |
| Daylight sensor | Analog skylight/time output, weather attenuation and saved inverted mode |
| Pistons | Directional activation and sampled quasi-connectivity; validate entire 12-block push/slime assembly before mutation; sticky pull; very short pulses leave the block out; glazed terracotta does not stick |
| Consumers | Lamps, doors/trapdoors/gates, note blocks, TNT and pistons respond to circuit power; distant consumers reached through repeater networks update on both edges |

All scheduler, burnout and transient circuit maps belong to their **World**,
not a process-global circuit. Component events are rebuilt on chunk load:
pressed buttons and lit observers/lamps cannot remain stuck indefinitely.
Loading a neighbor alone is not an observer block-change event.

**Compatibility limits:** piston movement is instantaneous, without moving-block
entities, player/mob pushing or slime launches. Exact Java update priorities,
BUD/update-order contraptions and zero-tick tricks are not guaranteed. Pending
event deadlines/burnout histories are not serialized; recovery starts fresh on
reload. This is not the complete vanilla redstone block catalog (for example,
it does not add a hopper/dispenser/dropper system). Note-block tuning remains
session state. Daylight output is an approximation, not a bit-exact Java formula.

## Save, network and texture safety

The existing chunk extension stores a 12-bit block ID plus four property bits.
Block-ID readers mask properties; chunk save/load and authoritative network
updates preserve all 16 unsigned bits. Saved properties include waterlogging,
falling/bubble water, repeater delay, comparator mode and sensor inversion.

**Back up worlds before upgrading and run V6.7 on all multiplayer peers.**
New block IDs/properties are not designed for downgrading. New clients accept
an older handshake by selecting legacy terrain, but mixed-version gameplay is
not a supported configuration.

The Aseprite workflow is unchanged. Thirteen new aquatic cells occupy **781–793**;
existing cells are not repainted. Export now contains **545 individual PNGs plus
six animation sheets**. Keep your existing `MyTextures` folder. Missing new
PNGs fall back to the built-in art; to edit them, copy just the new cells from
a fresh kit into your backed-up pack, rather than overwriting old edits.

## Verification and reproduction

`python3 tools/runtests.py` runs historical equivalence and the full suite.
The equivalence process explicitly selects legacy fluid behavior; the new
behavior is exercised separately by `test/bc/TestsAquaticRedstone.java`.
The mesh scheduler also visits all sixteen vertical sections, fixing a loading
stall when terrain near spawn extends above Y=127.
That suite tests flow/recession/routing, waterlogging/buckets/fire, swimming,
bubbles, biome/decorating determinism, unsigned chunk/network states, generator
handshake, circuit timing/locking/burnout, analog comparators, long networks,
piston/slime atomicity and load recovery. Historical art guards exclude only
the 13 new cells; native coverage and export/import tests still cover them.

Actual-game visual scenes in `tools/shots.py`:

- `pixel-aquatic-reef`: generated warm-ocean reef, seed 12345, near -2968/-3000.
- `pixel-aquatic-ice`: generated iceberg field, same seed, near -216/-3000.
- `pixel-aquatic-circuits`: circuit and waterlogged-shape bench.
- `pixel-editor-bow`: existing PNG reload and flat-bow regression fixture.

The release receipt records the final executable, screenshots, checksums and
GitHub checks once validated. Test success is not blanket artistic approval or
proof of full Minecraft compatibility.
