# V6.5 native-pixel art rework

**Unreleased follow-ups (not yet user-approved):**
[Building category](building-block-review.md) · [Colors / Nature categories](colors-nature-review.md).
The release record below describes the published V6.5 assets, not those working revisions.

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

Remote verification completed:
- CI **38488288667** and **38488964803**: 4,175,541 assertions, zero failures;
  strict equivalence, shader syntax and native coverage checks passed.
- Visual **38488288632**: all ten world/inventory/weapon scenes exited 0.
  Actual shader-off/on plant and material captures, bow, torch-lit hand and lava
  were inspected; no atlas/alpha errors were observed.
- Visual **38488964794**: title and three unobscured inventory captures exited 0.
  The cursor is outside the inventory so tooltips cannot conceal the new icons.
- Windows **38488964860**: all seven scenes captured, exit 0, and GPU preference
  registry checks passed. Title/inventories/bow/hand launched through Launch4j;
  the two long-property fixtures launched the EXE payload through bundled Java.
  The initial run's missing legacy title capture was not counted as a pass: the
  final run uses the production-style panorama title, and the smoke harness now
  fails if *any* requested capture is missing instead of accepting one success.
  Windows and Linux title images visibly identify V6.5 / Natural Pixel 32.

Release receipts now require all four new/shared artists as well as the existing
renderer, atlas, bow and lighting classes, and compare EXE-embedded game bytes
against the JAR. Both packaged `--build-info` entry points report V6.5, native
32px atlas, flat sprite bow and single-lightmap hand lighting. Full archive
assembly preserved all 327 bundled files and the exact smoke-tested patch bytes.

Release source SHA-256:
`7b8788e898a2fa2b700dea9ec566b3989b62b5999e13cbd4f8764f0db8428cf0`

Published as [V6.5](https://github.com/Aaron-1992/Blockcraft/releases/tag/v6.5)
at 2026-10-14 00:20:44 UTC, target `23a29db449ae465235d23ab36ff97b921627607e`.
Release **38489577013** and final CI **38489577028** succeeded. Both assets are
uploaded, non-draft and non-prerelease. GitHub's asset API SHA-256 digests and
sizes match the locally verified full/patch archives exactly (82,139,419 and
33,290,380 bytes). Direct asset re-download in this sandbox was blocked by its
release-assets host restriction; publication verification therefore uses those
GitHub-provided digests, not a claim of a successful local re-download.

Published assets:
- Full ZIP SHA-256: `84269d8a9f39e2bf4168e746cacbc6433785ff6ec0796a87e716d52ccdc53d42`
- Patch SHA-256: `8f06436c37db3e2dd7b179f6cef1c446860369d6a13b529909b4b39afdd95db7`

## Binary retention

The obsolete, rejected V6.2 generated patch was removed from the working checkout
so the new patch does not accumulate another large binary in session snapshots.
Its published GitHub release remains available and was not modified or deleted:
https://github.com/Aaron-1992/Blockcraft/releases/tag/v6.2
V6.3 and V6.4 archives are retained locally; all previous release downloads remain.
