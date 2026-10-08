# Visual run 2026-10-08 19:24:51 UTC

| scene | rendered by | differing pixels (ref vs new) |
|---|---|---|
| gap-far | new | n/a |
| gap-far-aniso1 | new | n/a |
| gap-far-plain | new | n/a |
| gap-detect | new | n/a |
| gap-detect-aniso1 | new | n/a |
| gap-detect-plain | new | n/a |
| gap-detect-lod | new | n/a |

## cracks (background pixels with terrain on both sides)

- gap-detect: 3 runs of 1-3 background pixels inside the terrain, 3 of them in the lower half of the picture
- gap-detect-aniso1: 0 runs of 1-3 background pixels inside the terrain, 0 of them in the lower half of the picture
- gap-detect-plain: 0 runs of 1-3 background pixels inside the terrain, 0 of them in the lower half of the picture
- gap-detect-lod: 192 runs of 1-3 background pixels inside the terrain, 94 of them in the lower half of the picture

## timings

- gap-far/new: exit 0 in 35s
- gap-far-aniso1/new: exit 0 in 28s
- gap-far-plain/new: exit 0 in 14s
- gap-detect/new: exit 0 in 18s
- gap-detect-aniso1/new: exit 0 in 17s
- gap-detect-plain/new: exit 0 in 6s
- gap-detect-lod/new: exit 0 in 29s
