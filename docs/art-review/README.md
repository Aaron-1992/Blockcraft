# V6.5 native-pixel art rework

## Scope and protected references

This finishes the replacement of the rejected V6.4 default block/item atlas.
The earlier 15-sample proof and 148-face construction preview were **not**
artistically approved; this pass is not presented as user-approved either.
The user asked to proceed with the complete rework rather than more small proofs.

All 456 non-protected populated cells are now different from V6.4. The 61 V6.3
reference cells remain pixel-exact (including normal stone bricks, all six planks,
cobble and the flat sprite bow). The white renderer utility cell is unchanged.
`docs/texture-coverage.tsv` identifies every populated cell, its current artist,
changed texels against **V6.4**, and its color count. Differences and passing tests
establish implementation coverage, not a numerical measure of artistic quality.

## Shared art direction

- Native 32×32 canvases and hard pixel silhouettes; no resizing of old artwork,
  antialiasing, blur, sharpening, relief filter or generic finishing overlay.
- `TexMaterials32`: furniture and machine bodies built from the exact protected
  timber/masonry arrays, 1px joints/edge accents, restrained grain, localized
  cracked-brick fractures and charcoal-burgundy Nether brick.
- `PixelArt32`: common material palettes, native polygon/ellipse rasterization,
  small tonal steps, wood sampling and pointed leaf construction.
- `TexItems32`: new silhouettes/components for tools, armor, foods, minerals,
  doors, utilities, equipment and mod icons. Different tool heads, grips, seams,
  metal edges, gemstone facets, food cuts and natural contours replace the old
  broad geometric stamps. Protected bow art and weapon pose code are unchanged.
- `TexNature32`: leaf clusters/veins, grass blades, growth-stage crops, flowers,
  continuous double-height plants, textiles, transparent surfaces, decorations,
  and five-pigment periodic water/lava/fire/portal frames. Biome/dye masks remain
  neutral so the renderer, not an accidentally baked color, applies their tint.

`TexRemaining32.paint` dispatches only to these new artists. Its `previous`
method retains the V6.4 reference solely for comparison/audit, never as a fallback.
Legacy/custom resource packs intentionally retain their own art and resolution.
Choose **Natural Pixel 32 (Default)** in game to see the new artwork.

## Review and verification

Reviewed enlarged, nearest-neighbor exports of the new item and nature canvases.
The second iteration refined axe heads, irregular flowering-bush placement and
instrument cases. Export sheets are actual painter output, not generated concept
art or a claim that the user has approved the visuals.

Local full tests: **4,175,541 passed, zero failed**. Strict equivalence: 397 unchanged
fingerprints, 64 intentionally changed, 145 new. Coverage: 61 protected, 456
redrawn, one utility white. Tests include every cell differing from V6.4,
new item alpha silhouettes, periodic bounded-palette animations, double-plant
stem continuity, protected golden hashes, neutral tints, transparent fringes,
native/legacy extrusion, flat bow animation and corrected hand lighting.

Actual-game fixtures in `tools/shots.py`:
- `pixel-items`, `pixel-food`, `pixel-resources`: inventory icon rendering.
- `pixel-garden-plain/lit`: close flowers/crops/foliage with shader lighting off/on.
- `pixel-cohesion-plain/lit`: neighboring construction faces and a mixed floor.
- `pixel-nether`, `pixel-bow-full`, `pixel-hand-torch`: animation/material and
  preserved weapon/lighting integration.

Remote visual/CI and Windows package checks are pending at this source commit.
Release receipts now require all four new/shared artists as well as the existing
renderer, atlas, bow and lighting classes, and compare EXE-embedded game bytes
against the JAR. V6.5 will be published only after the actual-game/package checks.

## Binary retention

The obsolete, rejected V6.2 generated patch was removed from the working checkout
so the new patch does not accumulate another large binary in session snapshots.
Its published GitHub release remains available and was not modified or deleted:
https://github.com/Aaron-1992/Blockcraft/releases/tag/v6.2
V6.3 and V6.4 archives are retained locally; all previous release downloads remain.
