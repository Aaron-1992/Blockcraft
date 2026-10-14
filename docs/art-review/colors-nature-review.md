# Colors / Nature feedback — working revision, not a release

The user has **not reviewed or approved the earlier building-block revision**.
They supplied this second batch after inspecting Colors and Nature in published
V6.5 and intend to continue their review later. Both batches remain queued for a
combined update. No V6.5 download or release trigger is changed here.

## Implemented scope

| Requested material | Change | Native cells |
|---|---|---|
| All wool | Staggered, rotated short curled fibres, replacing the two-tone checkerboard | 48–63 |
| Concrete powder | Fine independent grains and small aggregate; slightly dusty pigment rather than solid-concrete shading | 416–431 |
| Plain/stained terracotta | Quiet kiln mottling and curved compression marks, distinct from loose granular powder | 432–448 |
| All glazed terracotta | 16 individually authored, flat-pigment ceramic designs: flowers, directional arrows, fans, scrolls, stars, vines and interleaved ribbons | 449–464 |
| Dirt | Earth-colored fine aggregate without the oversized grey pebble stamps | 1 |
| Sand / red sand | Native one-texel grains, restrained pigment steps and faint wind ripples | 5, 523 |
| Podzol | Dark organic soil with rusty leaf fragments and fine needles; a litter cap on the sides | 524–525 |
| Coarse dirt | Smaller angular aggregate in the soil palette, not large pale squares | 528 |
| All logs | Narrow broken furrows, single-texel accents, irregular growth rings, exact transposes for horizontal bark | 7–8, 31–33, 246–248, 540–548 |
| Birch bark | Tapered lenticels, peeling-paper highlights and darker torn bark details rather than a few rectangular spots | 33, 247 (included above) |
| All ores | Branched angular seams embedded in the existing rock; no repeated raised diamond/round mineral stamps | 12–17, 258, 488 |
| Pumpkin / melon | Orange curved ribs, stem/radial cap and carved faces; green broken wavy rind markings on melon | 221–226 |

Exactly **102 atlas cells** change in this batch. Shared cells also naturally
update the blocks that use them (for example carpets use wool, and dirt is also
the substrate of grass sides). Concrete, snow-block artwork, planks, masonry,
items, plants, animations and the earlier 16 building-review faces remain exact.
The glazed art is original, using the bold polychrome/decorative vocabulary of
Minecraft's color category rather than copying Mojang texture assets. It does
not add new block-rotation mechanics.

## Snow-layer bug

`Blocks313.SNOW_LAYER` was mapped to `TexArt313.SNOW_SIDE` on **all faces**. That
cell depicts snow-capped dirt, including brown soil. The thin loose-snow block
now uses the existing `Tex.SNOW` on every face. Its 1/8-block height, collision,
replaceability and drops are unchanged. **The snow-block texture is not redrawn.**
This fixes the material assignment for inventory and world rendering, including
older/custom packs using their own snow texture.

## Preservation and checks

- `test/pixel32-building-review.properties` was captured from the first revision
  at `a6c0f73` before any second-batch edits. A full 1,024-cell comparison proves
  that precisely these 102 cells change; all earlier building-review pixels stay
  intact. That baseline records work in progress, not user approval.
- `test/pixel32-v65.properties` remains the untouched published-release baseline.
  The cumulative difference is now 118 cells (16 building + 102 colors/nature).
- 42 more previously protected cells were explicitly reopened by this request.
  The remaining **17** golden cells stay exact. Historical reference painters
  for all reopened cells are tested against V6.5 hashes, not silently relabeled.
- Tests check default dispatch, opacity, determinism, exact rotated log pixels,
  100–350 changed pixels per ore face with the original host rock retained,
  all 16 glazed designs, fibrous wool, powder vs concrete and snow-layer mapping.
- Local full test result: **4,412,628 assertions passed, zero failed**. Strict
  equivalence passed (397 identical, 64 intentionally changed, 145 new).
- The coverage ledger contains 17 protected, 500 redrawn and one utility-white
  populated cell. No new resolution, smoothing pass or enlarged legacy mask was
  introduced. Material grains and bark accents are authored on the 32px grid.

Reviewed actual nearest-neighbour comparisons (not generated concept art):
[colors](colors-nature-colors.png), [glazed](colors-nature-glazed.png),
[earth/gourds](colors-nature-earth.png), [logs/ores](colors-nature-logs-ores.png).
An inspection iteration reduced the pumpkin cap's mechanical spoke appearance,
strengthened the birch tears, and increased ore seam visibility without adding
raised outlines around the mineral deposits. The first in-game tiled wall revealed
that the short ore strokes repeated as small checkmarks. These were replaced by
individually laid-out branched fracture networks, widened at angular mineral
pockets, so the deposits are not isolated glyphs or only hairline cracks.

Initial remote CI **38495229683** passed with 4,412,628 assertions, zero failures.
Visual **38495229743** rendered all four scenes (exit 0). Reviewed the colors and
all 16 glaze designs in the actual game; powder/wool/clay show distinct surface
structure beside unchanged concrete. The nature shots confirm snow is white on
top and sides while remaining a shallow layer over dirt. The final ore iteration
and elevated log-end/snow view were verified in follow-up Visual **38496066332**:
plain/lit/overview exited 0 in 16s / 59s / 11s. Inspected all three final PNGs,
including the six log-end samples, thin white snow over dirt, and the unchanged
full snow-block control. Mineral deposits now use branched networks rather than
the first draft's repeated isolated strokes. Final CI **38496066379** passed with
**4,412,628 assertions, zero failures**, strict equivalence, shader syntax and
native coverage checks. Source commit: `71d789f`.

These checks establish rendering and scope protection, not artistic approval.
The current Colors/Nature and earlier Building revisions remain unapproved and
unreleased. V6.5 download archives are unchanged.

Actual-game fixtures: `pixel-color-review`, `pixel-glazed-review`,
`pixel-nature-review-plain/lit` use `jar=build`, with unchanged concrete controls,
all 16 glaze patterns, log/ore walls, visible log ends, soil/rind floors, thin
snow over dirt and an unchanged snow block alongside it.
