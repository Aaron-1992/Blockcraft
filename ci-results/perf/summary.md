# Perf run 2026-10-05 00:16:54 UTC (llvmpipe, window 640x360, 60 frames per setting)

## forest (exit 0, 746 s)

```
GL: 4.5 (Compatibility Profile) Mesa 25.2.8-0ubuntu0.24.04.4 / llvmpipe (LLVM 20.1.2, 256 bits)
BENCH start: window 640x360, view distance 8, distant terrain 64, shaders, water 2, 60 frames per setting, GPU timers yes, renderer llvmpipe (LLVM 20.1.2, 256 bits)
BENCH waiting for the world: frame 200, chunks 357, mesh queue 0
BENCH waiting for the world: frame 400, chunks 357, mesh queue 0
BENCH world ready after 526 frames: 357 chunks, camera at 190,88,-25 (ground 85)
BENCH as configured        avg   748.0 ms (  1.3 fps)  95%  1628.1 ms  worst  1641.3 ms  draws 0  vertices 0k  sections 0  shadow 0 draws 0k
BENCH   cpu ms  update 2.6 sky 0.6 lod 71.6 cull 0.2 shadow 0.0 terrain 138.5 mobs 2.3 water 6.2 clouds 39.8 fx 0.0 hand 0.1 post 7.3 gui 0.2 swap 7.5   (worst frame in the window 277.6 ms, garbage 1386 KB/frame)
BENCH   gpu ms  update 0.1 sky 44.6 lod 127.4 cull 0.0 shadow 0.0 terrain 52.1 mobs 0.1 water 0.6 clouds 42.1 fx 0.0 hand 0.2 post 11.8 gui 0.0 swap 0.0
BENCH   worst ms  update 4.0 sky 0.6 lod 72.4 cull 0.2 shadow 0.0 terrain 139.6 mobs 3.4 water 7.2 clouds 39.8 fx 0.0 hand 0.1 post 8.0 gui 0.3 swap 7.6
BENCH exact phases         avg   785.1 ms (  1.3 fps)  95%  1681.8 ms  worst  1712.8 ms  draws 0  vertices 0k  sections 0  shadow 0 draws 0k
BENCH   cpu ms  update 2.0 sky 1.5 lod 72.8 cull 0.4 shadow 0.1 terrain 151.0 mobs 1.8 water 0.6 clouds 42.2 fx 0.1 hand 1.2 post 11.8 gui 0.5 swap 0.3   (worst frame in the window 291.8 ms, garbage 1627 KB/frame)
BENCH   gpu ms  update 0.2 sky 38.1 lod 41.7 cull 0.1 shadow 115.7 terrain 33.3 mobs 0.2 water 0.5 clouds 43.9 fx 1.2 hand 0.1 post 12.4 gui 0.1 swap 0.0
BENCH   worst ms  update 2.7 sky 1.7 lod 75.0 cull 0.4 shadow 0.1 terrain 152.6 mobs 2.0 water 0.6 clouds 42.6 fx 0.1 hand 1.3 post 12.0 gui 0.5 swap 0.3
BENCH no shadows           avg   360.8 ms (  2.8 fps)  95%   470.8 ms  worst   485.7 ms  draws 0  vertices 0k  sections 0  shadow 0 draws 0k
BENCH fast water           avg   761.6 ms (  1.3 fps)  95%  1627.7 ms  worst  1667.1 ms  draws 0  vertices 0k  sections 0  shadow 0 draws 0k
BENCH no clouds            avg   695.1 ms (  1.4 fps)  95%  1481.3 ms  worst  1539.9 ms  draws 0  vertices 0k  sections 0  shadow 0 draws 0k
BENCH no distant terrain   avg  1309.5 ms (  0.8 fps)  95%  3407.8 ms  worst  3422.6 ms  draws 0  vertices 0k  sections 0  shadow 0 draws 0k
BENCH no shaders           avg   267.4 ms (  3.7 fps)  95%   354.5 ms  worst   366.7 ms  draws 0  vertices 0k  sections 0  shadow 0 draws 0k
BENCH view distance 4      avg   379.4 ms (  2.6 fps)  95%  1084.3 ms  worst  1106.3 ms  draws 0  vertices 0k  sections 0  shadow 0 draws 0k
BENCH done
BENCH as configured        avg   748.0 ms (  1.3 fps)  95%  1628.1 ms  worst  1641.3 ms  draws 0  vertices 0k  sections 0  shadow 0 draws 0k
BENCH   cpu ms  update 2.6 sky 0.6 lod 71.6 cull 0.2 shadow 0.0 terrain 138.5 mobs 2.3 water 6.2 clouds 39.8 fx 0.0 hand 0.1 post 7.3 gui 0.2 swap 7.5   (worst frame in the window 277.6 ms, garbage 1386 KB/frame)
BENCH   gpu ms  update 0.1 sky 44.6 lod 127.4 cull 0.0 shadow 0.0 terrain 52.1 mobs 0.1 water 0.6 clouds 42.1 fx 0.0 hand 0.2 post 11.8 gui 0.0 swap 0.0
BENCH   worst ms  update 4.0 sky 0.6 lod 72.4 cull 0.2 shadow 0.0 terrain 139.6 mobs 3.4 water 7.2 clouds 39.8 fx 0.0 hand 0.1 post 8.0 gui 0.3 swap 7.6
BENCH exact phases         avg   785.1 ms (  1.3 fps)  95%  1681.8 ms  worst  1712.8 ms  draws 0  vertices 0k  sections 0  shadow 0 draws 0k
BENCH   cpu ms  update 2.0 sky 1.5 lod 72.8 cull 0.4 shadow 0.1 terrain 151.0 mobs 1.8 water 0.6 clouds 42.2 fx 0.1 hand 1.2 post 11.8 gui 0.5 swap 0.3   (worst frame in the window 291.8 ms, garbage 1627 KB/frame)
BENCH   gpu ms  update 0.2 sky 38.1 lod 41.7 cull 0.1 shadow 115.7 terrain 33.3 mobs 0.2 water 0.5 clouds 43.9 fx 1.2 hand 0.1 post 12.4 gui 0.1 swap 0.0
BENCH   worst ms  update 2.7 sky 1.7 lod 75.0 cull 0.4 shadow 0.1 terrain 152.6 mobs 2.0 water 0.6 clouds 42.6 fx 0.1 hand 1.3 post 12.0 gui 0.5 swap 0.3
BENCH no shadows           avg   360.8 ms (  2.8 fps)  95%   470.8 ms  worst   485.7 ms  draws 0  vertices 0k  sections 0  shadow 0 draws 0k
BENCH fast water           avg   761.6 ms (  1.3 fps)  95%  1627.7 ms  worst  1667.1 ms  draws 0  vertices 0k  sections 0  shadow 0 draws 0k
BENCH no clouds            avg   695.1 ms (  1.4 fps)  95%  1481.3 ms  worst  1539.9 ms  draws 0  vertices 0k  sections 0  shadow 0 draws 0k
BENCH no distant terrain   avg  1309.5 ms (  0.8 fps)  95%  3407.8 ms  worst  3422.6 ms  draws 0  vertices 0k  sections 0  shadow 0 draws 0k
BENCH no shaders           avg   267.4 ms (  3.7 fps)  95%   354.5 ms  worst   366.7 ms  draws 0  vertices 0k  sections 0  shadow 0 draws 0k
BENCH view distance 4      avg   379.4 ms (  2.6 fps)  95%  1084.3 ms  worst  1106.3 ms  draws 0  vertices 0k  sections 0  shadow 0 draws 0k
```
