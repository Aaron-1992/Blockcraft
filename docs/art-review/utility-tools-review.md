# Decorations, utility models, tools and materials — unreleased review

**Not a release and not artistically approved.** Published V6.5 assets and the
release trigger are unchanged. Earlier Building and Colors/Nature work also
remains unapproved; this batch preserves it except for the explicitly reopened
carved-pumpkin and jack-o-lantern faces (223/224).

## Implemented in the working branch

- Original native-32 utility artwork: luminous amber glowstone, separate torch
  flame/cap/wood materials, chest timber, distinct furnace faces and animated
  firebox, cage bars/rivets, legible **TNT**, hay stalks/cut ends, detailed book
  spines, jagged pumpkin faces, layered cake, portal sockets, a separate purple
  egg shell, cloth, sea lanterns, lamps without diagonal crosses, and mechanisms.
- Hollow chest with a hinged lid and moving latch. Menu-driven interpolation
  invalidates the owning mesh section at 20 Hz; closing/world changes clear the
  transient state. Inventory contents and existing chest save IDs are unchanged.
- Stepped egg geometry; inserted portal eye rises above the socket. Levers tilt,
  repeaters have small component heads/shafts, and buttons use component materials
  instead of stretching full-block pictures. GUI and held model paths share
  world UV/hinge math. Wall torches use the proper shaft/head/cap geometry.
- Folded banner strips use consecutive cloth UV regions. Pick-block returns the
  correctly coloured banner **item**, not a wool block.
- Observers watch/output in all six directions. Existing horizontal IDs are
  retained; 2030–2033 hold vertical off/on variants, avoiding Backrooms IDs.
  Timer resets tolerate chained observers. Sensor states use different panels.
- Explicit emissive-material list replaces inference from shared block textures;
  fragment masks restrict furnace/jack-o-lantern/torch/observer emission. Wood,
  casings, unpowered parts and daylight sensors are not emissive materials.
- 56 slimmer item canvases: 25 tools, 16 armour pieces, arrow, four buckets,
  compass, clock, fire charge, the flat bow, and six combat/mod icons. Shears and
  flint-and-steel are pixel-exact. The bow has slimmer two-pixel limbs while
  retaining its flat draw animation. Held lighting, gun poses and cannon/mortar
  artwork are unchanged.
- Paper, book and Eye of Ender are redrawn. Cake icing is authored at its actual
  half-height side UV range, rather than disappearing above the placed model.

## Scope and local verification

The immutable `test/pixel32-color-nature.properties` baseline was exported from
**dfee07f**, before this batch. An independent allowlist verifies exactly **129
changed cells**: 108 existing canvases and 21 new component cells (760–780).
Every other atlas cell, including the 16 Building revisions and 100 retained
Colors/Nature revisions, is unchanged. Previous review scope checks remain in
place; only the two explicitly reopened pumpkin faces are delegated to this guard.

Local full run: **4,422,166 assertions passed, 0 failed**. Strict terrain/world/tick
comparison still matches the 3.5 reference with legacy features selected:
397 equal fingerprints, 64 intentionally changed, 145 new. The old mesh flag
retains historical torch/chest geometry rather than weakening that comparison.
GLSL syntax checks pass; these are not a substitute for a GPU/driver run.

Coverage: **539 populated cells = 522 redrawn + 16 protected + 1 white utility**.
New components have CPU16 fallbacks before resource-pack overrides; default
native32 dispatch owns their full-resolution art. The furnace animation upload
is limited to the default native32 pack.

New tests cover directional pulse/reset and chained timers, pick/drop identity,
placement pitch, lid interpolation/mesh invalidation/world reset, banner colour
and UV strips, egg/eye bounds, component winding, TNT's N, slimmer silhouettes,
actual cake-side icing, emissive exclusions, and fallback atlas population.

## Review evidence and remaining validation

The following nearest-neighbour sheets come from real native atlas dispatch,
not generated concepts. Left is the previous review; right is this working batch.
The icons sheet uses the real GUI model path with a native32 software atlas.

- [Utility/material canvases](utility-review-utility.png)
- [Tools/combat canvases](utility-review-tools.png)
- [GUI models and banner items](utility-review-icons.png)

## Actual-game verification

All **11 real-game scenes** passed and were inspected (Mesa llvmpipe, source
`a6e14de`, [Visual run 38620690785](https://github.com/Aaron-1992/Blockcraft/actions/runs/38620690785)).
The final cloth-join correction at `2c1cea9` also passed its focused plain/lit
recheck: [Visual run 38622022895](https://github.com/Aaron-1992/Blockcraft/actions/runs/38622022895).
Final [CI run 38622022970](https://github.com/Aaron-1992/Blockcraft/actions/runs/38622022970)
confirms **4,422,166 passed / 0 failed**, strict equivalence, GLSL syntax and coverage.

| Actual-game view | Captures |
|---|---|
| Utility models | [Plain](../../ci-results/shots/pixel-utility-review-plain-new.png) · [Shaders](../../ci-results/shots/pixel-utility-review-lit-new.png) |
| Selective emission | [Night lights and unlit control](../../ci-results/shots/pixel-utility-review-night-new.png) |
| Real chest menu/lid path | [Open](../../ci-results/shots/pixel-utility-chest-open-new.png) · [Closed](../../ci-results/shots/pixel-utility-chest-closed-new.png) |
| Tools/Combat inventory | [Page 1](../../ci-results/shots/pixel-utility-tools-new.png) · [Page 2](../../ci-results/shots/pixel-utility-tools-page2-new.png) |
| Standing/wall cloth and torch caps | [Plain](../../ci-results/shots/pixel-utility-banners-plain-new.png) · [Shaders](../../ci-results/shots/pixel-utility-banners-lit-new.png) |
| Flat bow preserved | [Idle](../../ci-results/shots/pixel-bow-idle-new.png) · [Full draw](../../ci-results/shots/pixel-bow-full-new.png) |

Image inspection caught and led to fixes for the egg/cage hiding their supporting
floor, duplicated latch art inside the open chest, and light leaking between thin
banner folds. These now have mesh/model regressions, not just screenshot evidence.

### Remaining before a public release

- User art review: this batch and the earlier two batches are still unapproved.
- Windows/manual play checks, especially lid smoothness/performance and custom-pack
  appearance. Current lid animation is driven by the local chest menu, not a new
  multiplayer open-container broadcast.
- Fresh versioned packaging and release receipts after the review is settled.

Test totals and coverage establish scope/correctness, not artistic acceptance.
No published archive has been rebuilt or replaced for this review. V6.5 still
points to `23a29db449ae465235d23ab36ff97b921627607e` (published 2026-10-14 00:20:44Z).
