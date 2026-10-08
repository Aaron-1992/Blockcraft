# Perf run 2026-10-05 00:54:09 UTC (llvmpipe, window 640x360, 30 frames per setting)

## forest (exit 0, 572 s)

```
GL: 4.5 (Compatibility Profile) Mesa 25.2.8-0ubuntu0.24.04.4 / llvmpipe (LLVM 20.1.2, 256 bits)
BENCH start: window 640x360, view distance 8, distant terrain 64, shaders, water 2, 30 frames per setting, GPU timers yes, renderer llvmpipe (LLVM 20.1.2, 256 bits)
BENCH waiting for the world: frame 200, chunks 353, mesh queue 12
BENCH waiting for the world: frame 400, chunks 353, mesh queue 0
BENCH world ready after 526 frames: 353 chunks, camera at 190,88,-25 (ground 85)
BENCH as configured        avg   423.9 ms (  2.4 fps)  95%  1521.2 ms  worst  1640.9 ms  draws 0  vertices 2054k  sections 245  lod 1659k  shadow 32 draws 269k
BENCH   cpu ms  update 2.4 sky 0.6 lod 84.6 cull 0.3 shadow 23.5 terrain 250.3 mobs 2.1 water 2.4 clouds 40.4 fx 0.0 hand 0.1 post 8.3 gui 0.3 swap 8.7   (the frames of the Perf timer: avg 423.9 ms, worst 1641.1 ms; garbage 1816 KB/frame)
BENCH   gpu ms  update 1.3 sky 52.6 lod 135.8 cull 0.0 shadow 76.8 terrain 104.0 mobs 0.0 water 0.2 clouds 35.8 fx 0.0 hand 0.0 post 13.8 gui 0.0 swap 0.0   (30 frames)
BENCH   garbage KB/frame  update 1531 sky 0 lod 26 cull 18 shadow 4 terrain 2 mobs 148 water 2 clouds 1 fx 0 hand 1 post 1 gui 5 swap 0
BENCH   worst ms  update 7.4 sky 0.8 lod 187.4 cull 0.5 shadow 367.8 terrain 1155.6 mobs 4.2 water 11.2 clouds 68.1 fx 0.2 hand 0.1 post 16.0 gui 0.5 swap 14.7
BENCH exact phases         avg   359.4 ms (  2.8 fps)  95%   495.1 ms  worst   498.6 ms  draws 0  vertices 2266k  sections 270  lod 2092k  shadow 0 draws 0k
BENCH   cpu ms  update 2.3 sky 1.8 lod 100.2 cull 0.4 shadow 0.1 terrain 202.3 mobs 2.0 water 0.4 clouds 34.3 fx 0.2 hand 1.3 post 13.3 gui 0.6 swap 0.3   (the frames of the Perf timer: avg 359.4 ms, worst 499.0 ms; garbage 1837 KB/frame)
BENCH   gpu ms  update 0.6 sky 40.9 lod 61.4 cull 0.1 shadow 117.0 terrain 86.3 mobs 0.2 water 0.4 clouds 34.0 fx 1.3 hand 0.1 post 13.6 gui 0.1 swap 0.0   (30 frames)
BENCH   garbage KB/frame  update 1581 sky 0 lod 28 cull 18 shadow 0 terrain 2 mobs 154 water 2 clouds 1 fx 0 hand 1 post 1 gui 4 swap 0
BENCH   worst ms  update 4.5 sky 3.1 lod 161.8 cull 0.6 shadow 0.3 terrain 293.7 mobs 3.0 water 1.8 clouds 56.5 fx 2.0 hand 2.2 post 24.4 gui 0.9 swap 0.4
BENCH no shadows           avg   346.9 ms (  2.9 fps)  95%   525.0 ms  worst   558.9 ms  draws 0  vertices 2374k  sections 286  lod 2121k  shadow 0 draws 0k
BENCH shadow every frame   avg  1513.0 ms (  0.7 fps)  95%  1618.9 ms  worst  1801.8 ms  draws 0  vertices 2374k  sections 286  lod 2121k  shadow 507 draws 4274k
BENCH shadow map uncut     avg  1574.2 ms (  0.6 fps)  95%  1670.1 ms  worst  1673.5 ms  draws 0  vertices 2374k  sections 286  lod 2121k  shadow 1577 draws 7459k
BENCH no section culling   avg   366.4 ms (  2.7 fps)  95%   462.4 ms  worst   473.2 ms  draws 0  vertices 2644k  sections 528  lod 2121k  shadow 0 draws 0k
BENCH fast water           avg   354.6 ms (  2.8 fps)  95%   450.9 ms  worst   460.8 ms  draws 0  vertices 2374k  sections 286  lod 2121k  shadow 0 draws 0k
BENCH no clouds            avg   369.2 ms (  2.7 fps)  95%   563.5 ms  worst  1547.4 ms  draws 0  vertices 2374k  sections 286  lod 2121k  shadow 17 draws 142k
BENCH no distant terrain   avg   274.2 ms (  3.6 fps)  95%   424.8 ms  worst   452.7 ms  draws 0  vertices 2374k  sections 286  lod 0k  shadow 0 draws 0k
BENCH no shaders           avg   270.9 ms (  3.7 fps)  95%   344.7 ms  worst   420.2 ms  draws 0  vertices 2374k  sections 286  lod 2121k  shadow 0 draws 0k
BENCH render scale 50%     avg   253.7 ms (  3.9 fps)  95%   328.4 ms  worst   328.7 ms  draws 0  vertices 2374k  sections 286  lod 2121k  shadow 0 draws 0k
BENCH view distance 4      avg   311.2 ms (  3.2 fps)  95%   342.7 ms  worst  1408.8 ms  draws 0  vertices 709k  sections 89  lod 2420k  shadow 18 draws 143k
BENCH done
BENCH as configured        avg   423.9 ms (  2.4 fps)  95%  1521.2 ms  worst  1640.9 ms  draws 0  vertices 2054k  sections 245  lod 1659k  shadow 32 draws 269k
BENCH   cpu ms  update 2.4 sky 0.6 lod 84.6 cull 0.3 shadow 23.5 terrain 250.3 mobs 2.1 water 2.4 clouds 40.4 fx 0.0 hand 0.1 post 8.3 gui 0.3 swap 8.7   (the frames of the Perf timer: avg 423.9 ms, worst 1641.1 ms; garbage 1816 KB/frame)
BENCH   gpu ms  update 1.3 sky 52.6 lod 135.8 cull 0.0 shadow 76.8 terrain 104.0 mobs 0.0 water 0.2 clouds 35.8 fx 0.0 hand 0.0 post 13.8 gui 0.0 swap 0.0   (30 frames)
BENCH   garbage KB/frame  update 1531 sky 0 lod 26 cull 18 shadow 4 terrain 2 mobs 148 water 2 clouds 1 fx 0 hand 1 post 1 gui 5 swap 0
BENCH   worst ms  update 7.4 sky 0.8 lod 187.4 cull 0.5 shadow 367.8 terrain 1155.6 mobs 4.2 water 11.2 clouds 68.1 fx 0.2 hand 0.1 post 16.0 gui 0.5 swap 14.7
BENCH exact phases         avg   359.4 ms (  2.8 fps)  95%   495.1 ms  worst   498.6 ms  draws 0  vertices 2266k  sections 270  lod 2092k  shadow 0 draws 0k
BENCH   cpu ms  update 2.3 sky 1.8 lod 100.2 cull 0.4 shadow 0.1 terrain 202.3 mobs 2.0 water 0.4 clouds 34.3 fx 0.2 hand 1.3 post 13.3 gui 0.6 swap 0.3   (the frames of the Perf timer: avg 359.4 ms, worst 499.0 ms; garbage 1837 KB/frame)
BENCH   gpu ms  update 0.6 sky 40.9 lod 61.4 cull 0.1 shadow 117.0 terrain 86.3 mobs 0.2 water 0.4 clouds 34.0 fx 1.3 hand 0.1 post 13.6 gui 0.1 swap 0.0   (30 frames)
BENCH   garbage KB/frame  update 1581 sky 0 lod 28 cull 18 shadow 0 terrain 2 mobs 154 water 2 clouds 1 fx 0 hand 1 post 1 gui 4 swap 0
BENCH   worst ms  update 4.5 sky 3.1 lod 161.8 cull 0.6 shadow 0.3 terrain 293.7 mobs 3.0 water 1.8 clouds 56.5 fx 2.0 hand 2.2 post 24.4 gui 0.9 swap 0.4
BENCH no shadows           avg   346.9 ms (  2.9 fps)  95%   525.0 ms  worst   558.9 ms  draws 0  vertices 2374k  sections 286  lod 2121k  shadow 0 draws 0k
BENCH shadow every frame   avg  1513.0 ms (  0.7 fps)  95%  1618.9 ms  worst  1801.8 ms  draws 0  vertices 2374k  sections 286  lod 2121k  shadow 507 draws 4274k
BENCH shadow map uncut     avg  1574.2 ms (  0.6 fps)  95%  1670.1 ms  worst  1673.5 ms  draws 0  vertices 2374k  sections 286  lod 2121k  shadow 1577 draws 7459k
BENCH no section culling   avg   366.4 ms (  2.7 fps)  95%   462.4 ms  worst   473.2 ms  draws 0  vertices 2644k  sections 528  lod 2121k  shadow 0 draws 0k
BENCH fast water           avg   354.6 ms (  2.8 fps)  95%   450.9 ms  worst   460.8 ms  draws 0  vertices 2374k  sections 286  lod 2121k  shadow 0 draws 0k
BENCH no clouds            avg   369.2 ms (  2.7 fps)  95%   563.5 ms  worst  1547.4 ms  draws 0  vertices 2374k  sections 286  lod 2121k  shadow 17 draws 142k
BENCH no distant terrain   avg   274.2 ms (  3.6 fps)  95%   424.8 ms  worst   452.7 ms  draws 0  vertices 2374k  sections 286  lod 0k  shadow 0 draws 0k
BENCH no shaders           avg   270.9 ms (  3.7 fps)  95%   344.7 ms  worst   420.2 ms  draws 0  vertices 2374k  sections 286  lod 2121k  shadow 0 draws 0k
BENCH render scale 50%     avg   253.7 ms (  3.9 fps)  95%   328.4 ms  worst   328.7 ms  draws 0  vertices 2374k  sections 286  lod 2121k  shadow 0 draws 0k
BENCH view distance 4      avg   311.2 ms (  3.2 fps)  95%   342.7 ms  worst  1408.8 ms  draws 0  vertices 709k  sections 89  lod 2420k  shadow 18 draws 143k
```

## mountain (exit 0, 365 s)

```
GL: 4.5 (Compatibility Profile) Mesa 25.2.8-0ubuntu0.24.04.4 / llvmpipe (LLVM 20.1.2, 256 bits)
BENCH start: window 640x360, view distance 8, distant terrain 64, shaders, water 2, 30 frames per setting, GPU timers yes, renderer llvmpipe (LLVM 20.1.2, 256 bits)
BENCH waiting for the world: frame 200, chunks 325, mesh queue 0
BENCH waiting for the world: frame 400, chunks 325, mesh queue 0
BENCH world ready after 524 frames: 325 chunks, camera at 392,124,-303 (ground 121)
BENCH as configured        avg   246.7 ms (  4.1 fps)  95%   695.3 ms  worst   724.6 ms  draws 0  vertices 837k  sections 243  lod 1598k  shadow 53 draws 146k
BENCH   cpu ms  update 2.3 sky 0.6 lod 94.2 cull 0.3 shadow 9.1 terrain 95.3 mobs 4.4 water 14.6 clouds 9.2 fx 0.0 hand 0.1 post 7.7 gui 0.2 swap 8.7   (the frames of the Perf timer: avg 246.7 ms, worst 724.3 ms; garbage 1464 KB/frame)
BENCH   gpu ms  update 1.1 sky 52.8 lod 56.2 cull 0.0 shadow 34.8 terrain 70.3 mobs 0.0 water 0.6 clouds 10.6 fx 0.0 hand 0.0 post 13.9 gui 0.0 swap 0.0   (30 frames)
BENCH   garbage KB/frame  update 1209 sky 0 lod 27 cull 16 shadow 3 terrain 2 mobs 140 water 2 clouds 1 fx 0 hand 1 post 1 gui 5 swap 0
BENCH   worst ms  update 8.9 sky 1.0 lod 191.5 cull 0.4 shadow 138.9 terrain 515.2 mobs 50.9 water 28.9 clouds 33.0 fx 0.2 hand 0.1 post 12.8 gui 0.4 swap 16.3
BENCH exact phases         avg   242.9 ms (  4.1 fps)  95%   260.2 ms  worst   795.1 ms  draws 0  vertices 938k  sections 267  lod 2180k  shadow 28 draws 77k
BENCH   cpu ms  update 1.8 sky 1.7 lod 116.1 cull 0.4 shadow 19.2 terrain 76.4 mobs 1.8 water 0.6 clouds 9.3 fx 0.1 hand 1.4 post 13.3 gui 0.6 swap 0.3   (the frames of the Perf timer: avg 242.9 ms, worst 795.3 ms; garbage 1705 KB/frame)
BENCH   gpu ms  update 0.6 sky 42.8 lod 75.8 cull 0.1 shadow 42.8 terrain 55.3 mobs 0.1 water 0.6 clouds 8.7 fx 1.3 hand 0.2 post 13.3 gui 0.1 swap 0.0   (30 frames)
BENCH   garbage KB/frame  update 1458 sky 0 lod 28 cull 16 shadow 1 terrain 2 mobs 154 water 2 clouds 1 fx 0 hand 1 post 1 gui 4 swap 0
BENCH   worst ms  update 3.4 sky 4.6 lod 170.4 cull 0.7 shadow 573.9 terrain 92.1 mobs 3.1 water 0.8 clouds 20.0 fx 0.4 hand 3.9 post 25.4 gui 0.7 swap 0.3
BENCH no shadows           avg   227.6 ms (  4.4 fps)  95%   276.3 ms  worst   302.5 ms  draws 0  vertices 1008k  sections 280  lod 2182k  shadow 0 draws 0k
BENCH shadow every frame   avg   815.8 ms (  1.2 fps)  95%   845.7 ms  worst  1010.2 ms  draws 0  vertices 1008k  sections 280  lod 2182k  shadow 840 draws 2351k
BENCH shadow map uncut     avg   853.4 ms (  1.2 fps)  95%   886.4 ms  worst   932.6 ms  draws 0  vertices 1008k  sections 280  lod 2182k  shadow 1800 draws 4244k
BENCH no section culling   avg   244.7 ms (  4.1 fps)  95%   268.9 ms  worst   273.1 ms  draws 0  vertices 1371k  sections 509  lod 2182k  shadow 0 draws 0k
BENCH fast water           avg   223.2 ms (  4.5 fps)  95%   252.8 ms  worst   257.7 ms  draws 0  vertices 1008k  sections 280  lod 2182k  shadow 0 draws 0k
BENCH no clouds            avg   245.6 ms (  4.1 fps)  95%   344.1 ms  worst   941.6 ms  draws 0  vertices 1008k  sections 280  lod 2182k  shadow 28 draws 78k
BENCH no distant terrain   avg   108.6 ms (  9.2 fps)  95%   122.6 ms  worst   123.2 ms  draws 0  vertices 1008k  sections 280  lod 0k  shadow 0 draws 0k
BENCH no shaders           avg   193.4 ms (  5.2 fps)  95%   267.2 ms  worst   293.8 ms  draws 0  vertices 1008k  sections 280  lod 2182k  shadow 0 draws 0k
BENCH render scale 50%     avg   167.1 ms (  6.0 fps)  95%   190.3 ms  worst   198.9 ms  draws 0  vertices 1008k  sections 280  lod 2182k  shadow 0 draws 0k
BENCH view distance 4      avg   208.6 ms (  4.8 fps)  95%   222.6 ms  worst   755.5 ms  draws 0  vertices 244k  sections 107  lod 2446k  shadow 28 draws 78k
BENCH done
BENCH as configured        avg   246.7 ms (  4.1 fps)  95%   695.3 ms  worst   724.6 ms  draws 0  vertices 837k  sections 243  lod 1598k  shadow 53 draws 146k
BENCH   cpu ms  update 2.3 sky 0.6 lod 94.2 cull 0.3 shadow 9.1 terrain 95.3 mobs 4.4 water 14.6 clouds 9.2 fx 0.0 hand 0.1 post 7.7 gui 0.2 swap 8.7   (the frames of the Perf timer: avg 246.7 ms, worst 724.3 ms; garbage 1464 KB/frame)
BENCH   gpu ms  update 1.1 sky 52.8 lod 56.2 cull 0.0 shadow 34.8 terrain 70.3 mobs 0.0 water 0.6 clouds 10.6 fx 0.0 hand 0.0 post 13.9 gui 0.0 swap 0.0   (30 frames)
BENCH   garbage KB/frame  update 1209 sky 0 lod 27 cull 16 shadow 3 terrain 2 mobs 140 water 2 clouds 1 fx 0 hand 1 post 1 gui 5 swap 0
BENCH   worst ms  update 8.9 sky 1.0 lod 191.5 cull 0.4 shadow 138.9 terrain 515.2 mobs 50.9 water 28.9 clouds 33.0 fx 0.2 hand 0.1 post 12.8 gui 0.4 swap 16.3
BENCH exact phases         avg   242.9 ms (  4.1 fps)  95%   260.2 ms  worst   795.1 ms  draws 0  vertices 938k  sections 267  lod 2180k  shadow 28 draws 77k
BENCH   cpu ms  update 1.8 sky 1.7 lod 116.1 cull 0.4 shadow 19.2 terrain 76.4 mobs 1.8 water 0.6 clouds 9.3 fx 0.1 hand 1.4 post 13.3 gui 0.6 swap 0.3   (the frames of the Perf timer: avg 242.9 ms, worst 795.3 ms; garbage 1705 KB/frame)
BENCH   gpu ms  update 0.6 sky 42.8 lod 75.8 cull 0.1 shadow 42.8 terrain 55.3 mobs 0.1 water 0.6 clouds 8.7 fx 1.3 hand 0.2 post 13.3 gui 0.1 swap 0.0   (30 frames)
BENCH   garbage KB/frame  update 1458 sky 0 lod 28 cull 16 shadow 1 terrain 2 mobs 154 water 2 clouds 1 fx 0 hand 1 post 1 gui 4 swap 0
BENCH   worst ms  update 3.4 sky 4.6 lod 170.4 cull 0.7 shadow 573.9 terrain 92.1 mobs 3.1 water 0.8 clouds 20.0 fx 0.4 hand 3.9 post 25.4 gui 0.7 swap 0.3
BENCH no shadows           avg   227.6 ms (  4.4 fps)  95%   276.3 ms  worst   302.5 ms  draws 0  vertices 1008k  sections 280  lod 2182k  shadow 0 draws 0k
BENCH shadow every frame   avg   815.8 ms (  1.2 fps)  95%   845.7 ms  worst  1010.2 ms  draws 0  vertices 1008k  sections 280  lod 2182k  shadow 840 draws 2351k
BENCH shadow map uncut     avg   853.4 ms (  1.2 fps)  95%   886.4 ms  worst   932.6 ms  draws 0  vertices 1008k  sections 280  lod 2182k  shadow 1800 draws 4244k
BENCH no section culling   avg   244.7 ms (  4.1 fps)  95%   268.9 ms  worst   273.1 ms  draws 0  vertices 1371k  sections 509  lod 2182k  shadow 0 draws 0k
BENCH fast water           avg   223.2 ms (  4.5 fps)  95%   252.8 ms  worst   257.7 ms  draws 0  vertices 1008k  sections 280  lod 2182k  shadow 0 draws 0k
BENCH no clouds            avg   245.6 ms (  4.1 fps)  95%   344.1 ms  worst   941.6 ms  draws 0  vertices 1008k  sections 280  lod 2182k  shadow 28 draws 78k
BENCH no distant terrain   avg   108.6 ms (  9.2 fps)  95%   122.6 ms  worst   123.2 ms  draws 0  vertices 1008k  sections 280  lod 0k  shadow 0 draws 0k
BENCH no shaders           avg   193.4 ms (  5.2 fps)  95%   267.2 ms  worst   293.8 ms  draws 0  vertices 1008k  sections 280  lod 2182k  shadow 0 draws 0k
BENCH render scale 50%     avg   167.1 ms (  6.0 fps)  95%   190.3 ms  worst   198.9 ms  draws 0  vertices 1008k  sections 280  lod 2182k  shadow 0 draws 0k
BENCH view distance 4      avg   208.6 ms (  4.8 fps)  95%   222.6 ms  worst   755.5 ms  draws 0  vertices 244k  sections 107  lod 2446k  shadow 28 draws 78k
```
