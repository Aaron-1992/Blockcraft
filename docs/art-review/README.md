# Cohesive material revision — in progress, not a release

## Accepted direction, not an approved screenshot

The user wants believable material structure, no mixels, and a consistent art
direction when different blocks touch. Their references are the existing stone
bricks, oak/other planks and cobblestone. They rejected the previous 15-tile proof;
it is removed, not silently shipped. Its icon changes are also not active.

## Current construction pass: 148 populated atlas cells

`TexMaterials32` paints construction/material families from one shared vocabulary:

- Native 32×32 texel grid; no resizing or per-face resolution differences.
- 1px joints and bevels; 2–3px grain clusters; restrained stepped contrast.
- Actual approved timber arrays for furniture, workstations and mechanisms.
- Actual approved stone/masonry for housings and variants, not flat grey panels.
- Related stone families share the same brick dimensions and mortar treatment.
- Bounded surface variation; no generic sharpening, blur, bevel or noise overlay
  applied to a finished legacy image.
- Short cracks confined to individual bricks. Dark charcoal-burgundy Nether brick.

This covers workstations/containers, doors/trapdoors/ladders, mineral and colored
building materials, stone variants, Nether/End construction materials, prismarine,
purpur and redstone machine faces. Approval of the *direction* is not approval of
every newly painted asset. The 61 previous golden hashes still protect the original
accepted textures and bow pixel-for-pixel. Tools, bows, guns, poses and lighting
have not been altered by this pass.

**Not complete:** the remaining vegetation, animated textures, decorations and
item icons still need the same art-direction revision. No new version/release has
been published. The 6.4 release assets and release trigger remain unchanged. Do
not describe the entire pack as finished based on this pass or passing tests.

## Check adjacency, not just isolated samples

`adjacent-materials.png` is an offline rendering of actual painter output, with
contiguous tiles at identical nearest-neighbour scale, before/after. It is not a
generated illustration and not an in-game screenshot. Regenerate after compiling:

```
java -Djava.awt.headless=true -cp build/classes:build/test-classes:libs/lwjgl-bundle.jar bc.MaterialCohesionPreview docs/art-review/adjacent-materials.png
```

`pixel-cohesion-plain` / `pixel-cohesion-lit` in `tools/shots.py` build a touching
wall and mixed-material floor in the actual game, with identical camera and
lighting and shaders off/on. Unlike the old gallery, there is no floating sample
ceiling obscuring the comparison.

Local validation: compilation plus 4,004,055 unit assertions passed, zero failed.
Includes shared-timber identity, reference cache immutability, masonry joint
preservation, sparse cracks, native dimensions/alpha, determinism, extrusion UVs,
existing approved golden hashes and the previous gameplay regressions. Numerical
tests protect implementation constraints; visual/style review remains necessary.
