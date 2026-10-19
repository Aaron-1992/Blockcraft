package bc;

import static bc.GameTests.check;
import static bc.GameTests.eq;
import static bc.GameTests.test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Headless tests of the performance work of Update 3.18: the choice of the graphics chip (GpuPref) and the measuring and adapting
 * code that follows it.   python3 tools/runtests.py unit
 */
final class TestsPerf {
   static void run() {
      gpu();
      frames();
      cull();
      sectionOccupancy();
      meshScratchMemory();
      meshCoverage();
      asynchronousLighting();
   }

   static void asynchronousLighting() {
      test("chunks: background lighting completes before a chunk becomes mesh-ready", () -> {
         World w = new World(482, null);
         w.saveEnabled = false;
         try {
            long deadline = System.nanoTime() + 5_000_000_000L;
            Chunk center = null;
            while (System.nanoTime() < deadline) {
               w.updateChunks(0, 0, 3, 4);
               center = w.chunk(0, 0);
               if (center != null && center.lit && !center.lighting) {
                  break;
               }
               Thread.sleep(2L);
            }
            check(center != null, "the center chunk is integrated");
            check(center != null && center.lit, "the background lighting pass completes");
            check(center != null && !center.lighting, "the chunk leaves the lighting state before meshing");
         } finally {
            w.shutdown();
         }
      });
   }

   static void sectionOccupancy() {
      test("chunks: indexed section occupancy stays correct through bulk generation and block edits", () -> {
         Chunk c = new Chunk(0, 0);
         int y = 37;
         int x = 4;
         int z = 9;
         check(c.isSectionEmpty(y >> 4), "an untouched section is empty");
         c.blocks[Chunk.idx(x, y, z)] = (short)Blocks.STONE; // generators use direct bulk writes
         check(!c.isSectionEmpty(y >> 4), "unknown occupancy falls back to the block data");
         c.recomputeSectionOccupancy();
         eq(1, c.sections[y >> 4].nonAirCount, "bulk indexing counts non-air blocks");
         check(!c.isSectionEmpty(y >> 4), "indexed occupied section is not empty");
         c.set(x, y, z, 0);
         eq(0, c.sections[y >> 4].nonAirCount, "block removal decrements the cache");
         check(c.isSectionEmpty(y >> 4), "last block removal empties the section");
         c.set(x + 1, y, z, Blocks.TORCH);
         eq(1, c.sections[y >> 4].nonAirCount, "block placement increments the cache");
         check(!c.isSectionEmpty(y >> 4), "newly placed block is visible through the cache");
      });
      test("chunks: partial mesh coverage remains distinct from all sixteen sections", () -> {
         Chunk c = new Chunk(0, 0);
         for (int sy = 4; sy < 16; sy++) {
            c.markMeshed(sy);
         }
         check(c.hasMeshed, "the first uploaded section is remembered");
         check(!c.everMeshed, "unbuilt deep sections are not reported as meshed");
         c.markMeshed(3);
         check(!c.everMeshed, "partial coverage remains partial");
         c.sections[2].conn = 0L;
         c.sections[2].connKnown = true;
         c.set(1, 33, 1, Blocks.STONE);
         check(!c.sections[2].connKnown && c.sections[2].conn == CaveCull.ALL, "a block edit invalidates cached connectivity conservatively");
      });
   }

   static void meshScratchMemory() {
      test("mesher: worker scratch returns oversized vertex arrays after a large section", () -> {
         Mesher.FB buffer = new Mesher.FB();
         buffer.a = new float[300000];
         buffer.n = 299999;
         buffer.clear();
         eq(32768, buffer.a.length, "large retained high-water buffer is released");
         buffer.a = new float[262144];
         buffer.trim();
         eq(262144, buffer.a.length, "normal reusable buffers stay cached below the cap");
         eq(4, Renderer.MAX_MESH_WORKERS, "worker maximum");
         eq(4, Renderer.inFlightLimit(2), "two workers have at most four staged results");
         eq(8, Renderer.inFlightLimit(4), "four workers have at most eight staged results");
         eq(8, Renderer.inFlightLimit(12), "the staging cap cannot grow with worker count");
      });
   }

   static void meshCoverage() {
      test("renderer: readiness waits for view candidates but not deferred cave sections", () -> {
         Game game = new Game();
         Renderer renderer = new Renderer(game);
         World world = new World(31917, null);
         world.saveEnabled = false;
         try {
            Chunk chunk = new Chunk(0, 0);
            chunk.set(1, 80, 1, Blocks.STONE);
            chunk.set(1, 160, 1, Blocks.STONE);
            chunk.recomputeSectionOccupancy();
            chunk.lit = true;
            for (int sy = 0; sy < 16; sy++) {
               if (sy != 5 && sy != 10) {
                  chunk.markMeshed(sy);
                  chunk.sections[sy].dirty = false;
               }
            }
            world.chunks.put(World.key(0, 0), chunk);

            renderer.camX = 8.0;
            renderer.camZ = 8.0;
            check(!renderer.meshCoverageReady(world, chunk), "a nearby unmeshed section holds readiness open");
            chunk.markMeshed(5);
            chunk.sections[5].dirty = false;

            renderer.camX = 128.0;
            renderer.rememberMeshVisibility(world, true);
            check(renderer.meshCoverageReady(world, chunk), "an off-view deep section can remain deferred");
            check(renderer.meshLoadingReady(world, chunk), "world entry does not wait for that deferred cave section");

            renderer.camX = 8.0;
            check(!renderer.meshCoverageReady(world, chunk), "the section becomes required again when it enters the warm ring");
            check(!renderer.meshLoadingReady(world, chunk), "loading waits for an unmeshed section required by the view");
            chunk.markMeshed(10);
            check(!renderer.meshLoadingReady(world, chunk), "loading waits for an in-place dirty mesh to finish");
            chunk.sections[10].dirty = false;
            check(renderer.meshLoadingReady(world, chunk), "loading completes after the required section is uploaded");
         } finally {
            renderer.shutdown();
            world.shutdown();
         }
      });
   }

   // ------------------------------------------------------------------ the culling of hidden sections
   /** The padded array of the mesher (18 x 18 x 18) filled with one block id. */
   static short[] padded(int id) {
      short[] b = new short[5832];
      java.util.Arrays.fill(b, (short)id);
      return b;
   }

   static void put(short[] b, int x, int y, int z, int id) {
      b[((y + 1) * 18 + z + 1) * 18 + x + 1] = (short)id;
   }

   static void setConn(Chunk c, int sy, long conn) {
      c.sections[sy].conn = conn;
      c.sections[sy].connKnown = true;
   }

   static void cull() {
      test("cull: connectivity of a section - solid is closed, air is open, a tunnel joins the faces it touches and no others", () -> {
         eq(0L, CaveCull.compute(padded(Blocks.STONE)), "stone");
         eq(CaveCull.ALL, CaveCull.compute(padded(0)), "air");
         short[] b = padded(Blocks.STONE);
         for (int x = 0; x < 16; x++) {
            put(b, x, 5, 7, 0); // a tunnel from the west face to the east face
         }

         long c = CaveCull.compute(b);
         check(CaveCull.connected(c, 5, 4) && CaveCull.connected(c, 4, 5), "west - east");
         int pairs = 0;
         for (int a = 0; a < 6; a++) {
            for (int d = 0; d < 6; d++) {
               if (CaveCull.connected(c, a, d)) {
                  pairs++;
               }
            }
         }

         eq(2, pairs, "nothing else");
         // an L: from the west face to a shaft that goes up through the top face
         for (int y = 5; y < 16; y++) {
            put(b, 3, y, 7, 0);
         }

         c = CaveCull.compute(b);
         check(CaveCull.connected(c, 5, 0) && CaveCull.connected(c, 0, 4) && CaveCull.connected(c, 5, 4), "the tunnel and the shaft meet: west, east and the top are all connected");
         check(!CaveCull.connected(c, 1, 0) && !CaveCull.connected(c, 2, 3), "the floor and the north / south faces are not");
         // two tunnels that do not meet
         b = padded(Blocks.STONE);
         for (int x = 0; x < 16; x++) {
            put(b, x, 2, 2, 0); // west - east
            put(b, 8, 12, x, 0); // north - south
         }

         c = CaveCull.compute(b);
         check(CaveCull.connected(c, 4, 5) && CaveCull.connected(c, 2, 3), "both tunnels");
         check(!CaveCull.connected(c, 4, 2) && !CaveCull.connected(c, 5, 3) && !CaveCull.connected(c, 4, 3), "but they do not lead into each other");
         // a dead end (a cave that opens on one face only) joins nothing
         b = padded(Blocks.STONE);
         for (int x = 0; x < 10; x++) {
            put(b, x, 8, 8, 0);
         }

         eq(0L, CaveCull.compute(b), "a dead end");
         // glass and leaves do not stop the view
         b = padded(Blocks.STONE);
         for (int x = 0; x < 16; x++) {
            put(b, x, 4, 4, x % 2 == 0 ? Blocks.GLASS : Blocks.LEAVES_OAK);
         }

         check(!Blocks.opaque[Blocks.GLASS] && !Blocks.opaque[Blocks.LEAVES_OAK] && CaveCull.connected(CaveCull.compute(b), 4, 5), "glass and leaves are see-through");
      });

      test("cull: premeshing connectivity from chunk storage matches the mesh connectivity", () -> {
         Chunk chunk = new Chunk(0, 0);
         int sy = 5;
         int base = sy << 12;
         for (int i = 0; i < 4096; i++) {
            chunk.blocks[base + i] = (short)Blocks.STONE;
         }
         short[] padded = padded(Blocks.STONE);
         for (int x = 0; x < 16; x++) {
            chunk.blocks[base + Chunk.idx(x, 5, 7)] = 0;
            put(padded, x, 5, 7, 0);
         }
         eq(CaveCull.compute(padded), CaveCull.compute(chunk, sy), "a tunnel before and after vertex meshing");
         chunk.recomputeSectionOccupancy();
         CaveCull.computeChunk(chunk);
         check(chunk.sections[sy].connKnown, "generated sections have known connectivity before meshing");
         eq(CaveCull.compute(padded), chunk.sections[sy].conn, "the precomputed tunnel connectivity");
         eq(CaveCull.ALL, chunk.sections[0].conn, "empty sections remain fully open");
      });

      test("cull: the walk goes through connected faces, not through rock, and never turns back", () -> {
         World w = new World(1, null);
         w.saveEnabled = false;
         for (int cz = -4; cz <= 4; cz++) {
            for (int cx = -4; cx <= 4; cx++) {
               Chunk c = new Chunk(cx, cz);
               w.chunks.put(World.key(cx, cz), c); // sections are open on every side (conn = ALL)
            }
         }

         CaveCull.Walk walk = new CaveCull.Walk();
         walk.run(w, 0, 8, 0, 3, 8, 136, 8, null);
         int open = walk.count;
         int expected = 0;
         for (int dz = -4; dz <= 4; dz++) {
            for (int dx = -4; dx <= 4; dx++) {
               if (dx * dx + dz * dz <= 16) {
                  expected += 16;
               }
            }
         }

         eq(expected, open, "everything inside the ring of 4 chunks is reached when it is all open");
         check(walk.reached(2, 0, 8) && !walk.reached(6, 0, 8), "the cached visibility query matches the walk bounds");
         // a wall of rock one chunk east of the camera: a whole chunk column of closed sections
         for (int sy = 0; sy < 16; sy++) {
            setConn(w.chunk(1, 0), sy, 0L);
         }

         walk.run(w, 0, 8, 0, 3, 8, 136, 8, null);
         boolean behind = false;
         boolean aside = false;
         boolean straight = false;
         for (int i = 0; i < walk.count; i++) {
            int idx = walk.order[i];
            if (walk.dx(idx) == 1 && walk.dz(idx) == 0) {
               behind = true; // the wall itself is reached (its faces are visible) ...
            }

            if (walk.dx(idx) == 2 && walk.dz(idx) == 1 && walk.sy(idx) == 8) {
               aside = true; // ... and what is behind it, but to the side, is reached through the open chunk beside the wall (a steep ray gets past its corner)
            }

            if (walk.dx(idx) == 2 && walk.dz(idx) == 0 && walk.sy(idx) == 8) {
               straight = true;
            }
         }

         check(behind && aside, "the wall is seen and what is behind it to the side is reached round the corner");
         check(!straight, "but not what is straight behind it: no ray gets through a wall of rock a chunk wide");
         // a rock ring round the camera: nothing outside is reached; one tunnel through it opens exactly that direction
         for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
               if (dx != 0 || dz != 0) {
                  for (int sy = 0; sy < 16; sy++) {
                     setConn(w.chunk(dx, dz), sy, 0L);
                  }
               }
            }
         }

         for (int sy = 0; sy < 16; sy++) {
            setConn(w.chunk(1, 0), sy, 0L);
         }

         walk.run(w, 0, 8, 0, 3, 8, 136, 8, null);
         boolean outside = false;
         for (int i = 0; i < walk.count; i++) {
            int idx = walk.order[i];
            if (Math.abs(walk.dx(idx)) >= 2 || Math.abs(walk.dz(idx)) >= 2) {
               outside = true;
            }
         }

         check(!outside, "a ring of rock round the camera's chunk (all heights): nothing outside it is reached");
         // a world of rock with the camera's chunk open and a tunnel along x at the camera's height through the three chunks east of it
         for (int cz = -4; cz <= 4; cz++) {
            for (int cx = -4; cx <= 4; cx++) {
               for (int sy = 0; sy < 16; sy++) {
                  setConn(w.chunk(cx, cz), sy, 0L);
               }
            }
         }

         for (int sy = 0; sy < 16; sy++) {
            setConn(w.chunk(0, 0), sy, CaveCull.ALL);
         }

         long tunnel = CaveCull.bit(5, 4) | CaveCull.bit(4, 5);
         setConn(w.chunk(1, 0), 8, tunnel);
         setConn(w.chunk(2, 0), 8, tunnel);
         setConn(w.chunk(3, 0), 8, tunnel);
         walk.run(w, 0, 8, 0, 3, 8, 136, 8, null);
         boolean east2 = false;
         boolean east3 = false;
         boolean east4 = false;
         boolean wrong = false;
         for (int i = 0; i < walk.count; i++) {
            int idx = walk.order[i];
            int dx = walk.dx(idx);
            int dz = walk.dz(idx);
            if (dx == 2 && dz == 0 && walk.sy(idx) == 8) {
               east2 = true;
            }

            if (dx == 3 && dz == 0 && walk.sy(idx) == 8) {
               east3 = true;
            }

            if (dx == 4 && dz == 0 && walk.sy(idx) == 8) {
               east4 = true; // the rock at the end of the tunnel is seen, and that is where it stops
            }

            if (dx >= 2 && !(dz == 0 && walk.sy(idx) == 8)) {
               wrong = true;
            }

            if (dx <= -2 || Math.abs(dz) >= 2) {
               wrong = true;
            }
         }

         check(east2 && east3 && east4, "the tunnel is followed to its end: " + east2 + east3 + east4);
         check(!wrong, "nothing is reached but the tunnel and the camera's 27 sections");
         // a bend: west -> north is connected, west -> east is not: from the west the walk turns north and does not go on east
         setConn(w.chunk(2, 0), 8, CaveCull.bit(5, 2) | CaveCull.bit(2, 5));
         for (int sy = 0; sy < 16; sy++) {
            setConn(w.chunk(2, -1), sy, sy == 8 ? CaveCull.ALL : 0L);
         }

         walk.run(w, 0, 8, 0, 3, 8, 136, 8, null);
         boolean north = false;
         boolean beyond = false;
         for (int i = 0; i < walk.count; i++) {
            int idx = walk.order[i];
            if (walk.dx(idx) == 2 && walk.dz(idx) == -1 && walk.sy(idx) == 8) {
               north = true;
            }

            if (walk.dx(idx) >= 3 && walk.dz(idx) == 0 && walk.sy(idx) == 8) {
               beyond = true;
            }
         }

         check(north, "round the bend to the north");
         check(!beyond, "and not straight on");
      });

      test("cull: a walk with a pyramid of view takes only sections in it", () -> {
         World w = new World(1, null);
         w.saveEnabled = false;
         for (int cz = -4; cz <= 4; cz++) {
            for (int cx = -4; cx <= 4; cx++) {
               w.chunks.put(World.key(cx, cz), new Chunk(cx, cz));
            }
         }

         CaveCull.Walk walk = new CaveCull.Walk();
         // looking east: only sections with a larger x than the camera (plus the 27 round the camera)
         walk.run(w, 0, 8, 0, 3, 8, 136, 8, (rx, ry, rz) -> rx > -20.0F);
         for (int i = 0; i < walk.count; i++) {
            int idx = walk.order[i];
            check(walk.dx(idx) >= -1, "no section behind the camera: dx " + walk.dx(idx));
         }

         check(walk.count > 100, "and a lot in front: " + walk.count);
      });

      test("cull: no ray from the camera ever ends in a section the walk did not reach (terrain of the new generator, cameras on the ground, in the air and in caves)", () -> {
         World w = new World(12345, null, Dim.OVERWORLD, new WorldSettings());
         w.saveEnabled = false;
         int R = 5;
         java.util.List<Chunk> cs = new ArrayList<>();
         for (int cz = -R; cz <= R; cz++) {
            for (int cx = -R; cx <= R; cx++) {
               Chunk c = w.gen.generate(cx, cz);
               cs.add(c);
               w.chunks.put(World.key(cx, cz), c);
            }
         }

         for (Chunk c : cs) {
            w.lightChunk(c);
         }

         int connectivityMismatches = 0;
         String firstConnectivityMismatch = "";
         for (Chunk c : cs) {
            c.recomputeSectionOccupancy();
            CaveCull.computeChunk(c);
            for (int sy = 0; sy < 16; sy++) {
               Mesher.Result r = Mesher.build(w, c, sy, true);
               if (r.conn != c.sections[sy].conn) {
                  connectivityMismatches++;
                  if (firstConnectivityMismatch.isEmpty()) {
                     firstConnectivityMismatch = c.cx + "," + c.cz + "," + sy;
                  }
               }
               Renderer.free(r);
            }
         }
         check(connectivityMismatches == 0, "premeshed connectivity matches the mesher for all terrain sections; mismatches " + connectivityMismatches + " (first " + firstConnectivityMismatch + ")");

         // cameras: three above the ground, three in caves (an open block that has solid rock above it and a surface well above)
         java.util.List<double[]> cams = new ArrayList<>();
         java.util.Random rnd = new java.util.Random(5);
         for (int i = 0; i < 3; i++) {
            int x = rnd.nextInt(60) - 30;
            int z = rnd.nextInt(60) - 30;
            Chunk c = w.chunkAt(x, z);
            cams.add(new double[]{x + 0.5, c.hm[(z & 15) * 16 + (x & 15)] + 2.5, z + 0.5});
         }

         int tries = 0;
         while (cams.size() < 8 && tries++ < 40000) {
            int x = rnd.nextInt(100) - 50;
            int z = rnd.nextInt(100) - 50;
            int y = 8 + rnd.nextInt(60);
            Chunk c = w.chunkAt(x, z);
            if (y < c.hm[(z & 15) * 16 + (x & 15)] - 12 && w.getBlock(x, y, z) == 0 && w.getBlock(x, y + 1, z) == 0 && w.getBlock(x, y + 2, z) != 0) {
               cams.add(new double[]{x + 0.5, y + 1.0, z + 0.5});
            }
         }

         check(cams.size() >= 6, "cameras found: " + cams.size());
         CaveCull.Walk walk = new CaveCull.Walk();
         int rays = 0;
         int misses = 0;
         int total = 0;
         int reachedTotal = 0;
         for (double[] cam : cams) {
            int cx = (int)Math.floor(cam[0]) >> 4;
            int cz = (int)Math.floor(cam[2]) >> 4;
            int sy = (int)Math.floor(cam[1]) >> 4;
            walk.run(w, cx, sy, cz, 4, cam[0], cam[1], cam[2], null);
            boolean[] reached = new boolean[walk.cells];
            for (int i = 0; i < walk.count; i++) {
               reached[walk.order[i]] = true;
            }

            int withFaces = 0;
            int reachedWithFaces = 0;
            for (int dz = -4; dz <= 4; dz++) {
               for (int dx = -4; dx <= 4; dx++) {
                  Chunk c = w.chunk(cx + dx, cz + dz);
                  if (c == null || dx * dx + dz * dz > 25) {
                     continue;
                  }

                  for (int y = 0; y < 16; y++) {
                     Mesher.Result r = Mesher.build(w, c, y, true);
                     boolean faces = r.nO + r.nT > 0;
                     Renderer.free(r);
                     if (faces) {
                        withFaces++;
                        if (reached[(((dz + 5) * 11) + dx + 5) * 16 + y]) {
                           reachedWithFaces++;
                        }
                     }
                  }
               }
            }

            total += withFaces;
            reachedTotal += reachedWithFaces;
            // 4000 rays on a spiral over the whole sphere
            int n = 4000;
            for (int k = 0; k < n; k++) {
               double yy = 1.0 - 2.0 * (k + 0.5) / n;
               double rr = Math.sqrt(1.0 - yy * yy);
               double phi = k * 2.399963229728653;
               double[] dir = {rr * Math.cos(phi), yy, rr * Math.sin(phi)};
               rays++;
               int[] voxel = {(int)Math.floor(cam[0]), (int)Math.floor(cam[1]), (int)Math.floor(cam[2])};
               double t = 0.0;
               int[] step = new int[3];
               double[] tMax = new double[3];
               double[] tDelta = new double[3];
               for (int a = 0; a < 3; a++) {
                  step[a] = dir[a] > 0 ? 1 : -1;
                  double next = dir[a] > 0 ? voxel[a] + 1 : voxel[a];
                  tMax[a] = dir[a] == 0 ? 1e30 : (next - cam[a]) / dir[a];
                  tDelta[a] = dir[a] == 0 ? 1e30 : Math.abs(1.0 / dir[a]);
               }

               while (t < 60.0) {
                  int vx = voxel[0];
                  int vy = voxel[1];
                  int vz = voxel[2];
                  if (vy < 0 || vy > 255) {
                     break;
                  }

                  Chunk c = w.chunkAt(vx, vz);
                  if (c == null) {
                     break;
                  }

                  int ddx = (vx >> 4) - cx;
                  int ddz = (vz >> 4) - cz;
                  if (ddx * ddx + ddz * ddz <= 25) {
                     int cell = (((ddz + 5) * 11) + ddx + 5) * 16 + (vy >> 4);
                     if (Math.abs(ddx) <= 5 && Math.abs(ddz) <= 5 && !reached[cell]) {
                        misses++;
                        if (misses <= 5) {
                           System.out.println("   missed: camera " + Arrays.toString(cam) + " ray " + Arrays.toString(dir) + " in block " + vx + "," + vy + "," + vz + " section " + ddx + "," + (vy >> 4) + "," + ddz);
                        }

                        break;
                     }
                  }

                  if (Blocks.opaque[w.getBlock(vx, vy, vz)]) {
                     break;
                  }

                  int axis = tMax[0] < tMax[1] ? (tMax[0] < tMax[2] ? 0 : 2) : (tMax[1] < tMax[2] ? 1 : 2);
                  t = tMax[axis];
                  tMax[axis] += tDelta[axis];
                  voxel[axis] += step[axis];
               }
            }
         }

         eq(0, misses, "rays that ended in (or passed through) a section the walk did not reach: " + misses + " of " + rays);
         check(reachedTotal < total * 0.9, "the walk does cull: it reached " + reachedTotal + " of " + total + " sections with faces round the cameras");
         System.out.println("   cull: reached " + reachedTotal + " of " + total + " sections with faces (" + (100 * reachedTotal / Math.max(1, total)) + " %), " + rays + " rays, no misses");
      });
   }

   // ------------------------------------------------------------------ the frame timer
   /** A clock the test moves by hand (milliseconds). */
   static final long[] NOW = new long[1];

   static void at(double ms) {
      NOW[0] = (long)(ms * 1e6);
   }

   static void frames() {
      test("perf: a scripted frame - the numbers come out as the clock said", () -> {
         java.util.function.LongSupplier savedClock = Perf.clock;
         try {
            Perf.clock = () -> NOW[0];
            Perf.on = true;
            Perf.exact = false;
            at(0);
            Perf.frame();
            Perf.resetWindow();
            double t = 0;
            for (int i = 0; i < 50; i++) {
               // frame start at t: update until t+2, sky until t+5, terrain until t+9, gui until t+14, swap until t+16 (the next frame)
               at(t + 2);
               Perf.mark(Perf.SKY);
               at(t + 5);
               Perf.mark(Perf.TERRAIN);
               at(t + 9);
               Perf.mark(Perf.GUI);
               at(t + 14);
               Perf.mark(Perf.SWAP);
               t += 16;
               at(t + (i == 20 ? 40 : 0)); // one slow frame: 40 ms more
               if (i == 20) {
                  t += 40;
               }

               Perf.frame();
            }

            Perf.flush();
            near(2.0, Perf.avgCpu[Perf.UPDATE], 0.01, "update");
            near(3.0, Perf.avgCpu[Perf.SKY], 0.01, "sky");
            near(4.0, Perf.avgCpu[Perf.TERRAIN], 0.01, "terrain (the slow frame's extra time is in the swap)");
            near(5.0, Perf.avgCpu[Perf.GUI], 0.01, "gui");
            check(Perf.avgCpu[Perf.SWAP] > 2.5 && Perf.avgCpu[Perf.SWAP] < 3.5, "swap " + Perf.avgCpu[Perf.SWAP]);
            check(Perf.peakCpu[Perf.SWAP] > 41.0, "the worst swap is the slow frame: " + Perf.peakCpu[Perf.SWAP]);
            near(Perf.avgCpu[Perf.UPDATE] + Perf.avgCpu[Perf.SKY] + Perf.avgCpu[Perf.TERRAIN] + Perf.avgCpu[Perf.GUI] + Perf.avgCpu[Perf.SWAP], Perf.avgFrame, 0.02, "the phases add up to the frame");
            check(Perf.peakFrame > 55.0 && Perf.peakFrame < 57.0, "worst frame " + Perf.peakFrame);
            check(Perf.fps >= 50 && Perf.fps <= 65, "50 frames in 0.84 s: " + Perf.fps);
            check(!Perf.haveGpu, "no GPU timers without a context");
            String[] lines = Perf.lines("test chip");
            check(lines.length >= 5 && lines[0].startsWith("Frame ") && lines[1].contains("test chip"), "the panel has its lines: " + String.join(" | ", lines));
         } finally {
            Perf.clock = savedClock;
            Perf.on = false;
         }
      });

      test("perf: nothing is measured while it is off, and perf.log gets one line per second after a header", () -> {
         java.util.function.LongSupplier savedClock = Perf.clock;
         File dir = java.nio.file.Files.createTempDirectory("perf").toFile();
         try {
            Perf.clock = () -> NOW[0];
            Perf.on = false;
            at(0);
            Perf.frame();
            at(5);
            Perf.mark(Perf.SKY); // ignored
            at(2000);
            Perf.frame();
            Perf.flush();
            // (flush only publishes a window that has frames)
            File log = new File(dir, "perf.log");
            Perf.on = true;
            Perf.log = log;
            at(3000);
            Perf.resetWindow();
            Perf.frame();
            double t = 3000;
            for (int sec = 0; sec < 3; sec++) {
               for (int i = 0; i < 70; i++) {
                  at(t + 3);
                  Perf.mark(Perf.SKY);
                  at(t + 10);
                  Perf.mark(Perf.SWAP);
                  t += 15;
                  at(t);
                  Perf.frame();
               }
            }

            java.util.List<String> lines = java.nio.file.Files.readAllLines(log.toPath());
            check(lines.size() >= 3 && lines.get(0).startsWith("fps;frame_avg_ms;") && lines.get(0).contains(";cpu_terrain;") && lines.get(0).contains(";gpu_swap"), "a header: " + lines);
            check(lines.get(1).split(";", -1).length == lines.get(0).split(";", -1).length, "a line has as many fields as the header: " + lines.get(1));
            check(lines.get(1).startsWith("66;") || lines.get(1).startsWith("67;") || lines.get(1).startsWith("65;"), "15 ms frames are 66 fps: " + lines.get(1));
         } finally {
            Perf.clock = savedClock;
            Perf.on = false;
            Perf.log = null;
            Tests311.deleteAll(dir);
         }
      });

      test("perf: the benchmark's list of settings has the names the report prints, and the leaf / shadow switches exist", () -> {
         eq(12, PerfBench.PRESETS.length, "settings");
         check(PerfBench.PRESETS[0].equals("as configured") && PerfBench.PRESETS[1].equals("exact phases"), "the first two");
         check(!PerfBench.enabled() && !PerfBench.running(), "off by default");
         check(!Perf.noShadow && !Perf.noCull, "switches are off");
      });
   }

   static void near(double expected, double actual, double eps, String what) {
      check(Math.abs(expected - actual) <= eps, what + ": expected " + expected + ", got " + actual);
   }

   /** A registry that lives in a map: answers reg query / add / delete the way reg.exe does. */
   static GpuPref.Runner fakeRegistry(Map<String, String> reg, List<String> log) {
      return cmd -> {
         log.add(String.join(" ", cmd));
         if (!cmd[0].equals("reg") || !cmd[2].equals(GpuPref.KEY)) {
            return new GpuPref.Out(1, "wrong command");
         }

         String name = cmd[4];
         switch (cmd[1]) {
            case "query":
               if (!reg.containsKey(name)) {
                  return new GpuPref.Out(1, "ERROR: The system was unable to find the specified registry key or value.");
               }

               return new GpuPref.Out(0, "\r\n" + GpuPref.KEY.replace("HKCU", "HKEY_CURRENT_USER") + "\r\n    " + name + "    REG_SZ    " + reg.get(name) + "\r\n\r\n");
            case "add":
               check(cmd[5].equals("/t") && cmd[6].equals("REG_SZ") && cmd[7].equals("/d") && cmd[9].equals("/f"), "add: type, data and force switches");
               reg.put(name, cmd[8]);
               return new GpuPref.Out(0, "The operation completed successfully.");
            case "delete":
               return reg.remove(name) != null ? new GpuPref.Out(0, "The operation completed successfully.") : new GpuPref.Out(1, "ERROR: not found");
            default:
               return new GpuPref.Out(1, "?");
         }
      };
   }

   static void gpu() {
      test("gpu: the commands are reg.exe's and the output of reg query is read the way it is printed", () -> {
         eq("[reg, query, HKCU\\Software\\Microsoft\\DirectX\\UserGpuPreferences, /v, C:\\Games\\jre\\bin\\javaw.exe]", Arrays.toString(GpuPref.cmdQuery("C:\\Games\\jre\\bin\\javaw.exe")), "query");
         eq("[reg, add, HKCU\\Software\\Microsoft\\DirectX\\UserGpuPreferences, /v, C:\\a b\\javaw.exe, /t, REG_SZ, /d, GpuPreference=2;, /f]", Arrays.toString(GpuPref.cmdAdd("C:\\a b\\javaw.exe")), "add (a path with a space stays one argument)");
         eq("[reg, delete, HKCU\\Software\\Microsoft\\DirectX\\UserGpuPreferences, /v, C:\\a\\javaw.exe, /f]", Arrays.toString(GpuPref.cmdDelete("C:\\a\\javaw.exe")), "delete");
         String real = "\r\nHKEY_CURRENT_USER\\Software\\Microsoft\\DirectX\\UserGpuPreferences\r\n    C:\\Users\\Me\\Desktop\\Blockcraft\\jre\\bin\\javaw.exe    REG_SZ    GpuPreference=2;\r\n\r\n";
         check(GpuPref.hasValue(real, "C:\\Users\\Me\\Desktop\\Blockcraft\\jre\\bin\\javaw.exe", GpuPref.HIGH), "found with Windows line ends");
         check(GpuPref.hasValue(real, "c:\\users\\me\\desktop\\blockcraft\\jre\\bin\\JAVAW.EXE", GpuPref.HIGH), "Windows paths do not care about capitals");
         check(!GpuPref.hasValue(real, "C:\\Users\\Me\\Desktop\\Blockcraft\\jre\\bin\\java.exe", GpuPref.HIGH), "another program is not found");
         check(!GpuPref.hasValue(real.replace("=2;", "=1;"), "C:\\Users\\Me\\Desktop\\Blockcraft\\jre\\bin\\javaw.exe", GpuPref.HIGH), "power saving is not high performance");
         check(!GpuPref.hasValue("ERROR: The system was unable to find the specified registry key or value.", "C:\\x.exe", GpuPref.HIGH) && !GpuPref.hasValue(null, "C:\\x.exe", GpuPref.HIGH), "an error / nothing is not found");
      });

      test("gpu: the programs that draw are the Java of the game (javaw.exe, java.exe) - only files that exist, every one once", () -> {
         File dir = java.nio.file.Files.createTempDirectory("jre").toFile();
         try {
            File bin = new File(dir, "bin");
            bin.mkdirs();
            File w = new File(bin, "javaw.exe");
            File j = new File(bin, "java.exe");
            java.nio.file.Files.write(w.toPath(), new byte[]{1});
            java.nio.file.Files.write(j.toPath(), new byte[]{1});
            List<String> p = GpuPref.programs(dir.getPath(), w.getPath());
            eq(2, p.size(), "javaw.exe (the running one, found twice) and java.exe");
            check(p.get(0).equals(w.getPath()) && p.get(1).equals(j.getPath()), "in that order");
            eq(2, GpuPref.programs(dir.getPath(), null).size(), "without process information java.home is enough");
            eq(0, GpuPref.programs(dir.getPath() + "x", null).size(), "no Java there, nothing to register");
            eq(1, GpuPref.programs(dir.getPath() + "x", w.getPath()).size(), "a running program that is no .exe of this Java is still registered");
            eq(0, GpuPref.programs(dir.getPath() + "x", "/usr/bin/java").size(), "on another system (no .exe) nothing happens");
         } finally {
            Tests311.deleteAll(dir);
         }
      });

      test("gpu: apply writes high performance for every program once, leaves it alone when it is there, takes only its own entries out again", () -> {
         Map<String, String> reg = new HashMap<>();
         List<String> log = new ArrayList<>();
         GpuPref.Runner saved = GpuPref.runner;
         GpuPref.runner = fakeRegistry(reg, log);
         try {
            List<String> progs = Arrays.asList("C:\\g\\jre\\bin\\javaw.exe", "C:\\g\\jre\\bin\\java.exe");
            eq(2, GpuPref.apply(true, progs), "two entries written");
            eq("GpuPreference=2;", reg.get("C:\\g\\jre\\bin\\javaw.exe"), "the data");
            log.clear();
            eq(0, GpuPref.apply(true, progs), "the second start changes nothing");
            check(log.size() == 2 && log.get(0).contains("query") && log.get(1).contains("query"), "it only asks: " + log);
            reg.put("C:\\g\\jre\\bin\\java.exe", "GpuPreference=1;"); // the player chose power saving by hand in Windows
            eq(1, GpuPref.apply(true, progs), "a different choice is replaced when the game is told to use the real card");
            eq("GpuPreference=2;", reg.get("C:\\g\\jre\\bin\\java.exe"), "replaced");
            reg.put("C:\\g\\jre\\bin\\java.exe", "GpuPreference=1;");
            eq(1, GpuPref.apply(false, progs), "windows decides: our entry goes");
            check(!reg.containsKey("C:\\g\\jre\\bin\\javaw.exe"), "javaw.exe is gone");
            eq("GpuPreference=1;", reg.get("C:\\g\\jre\\bin\\java.exe"), "the player's own choice stays");
            eq(0, GpuPref.apply(false, progs), "nothing more to take out");
            // a failing reg.exe (no rights, policy) is no crash
            GpuPref.runner = cmd -> new GpuPref.Out(1, "ERROR: Access is denied.");
            eq(0, GpuPref.apply(true, progs), "nothing could be written: nothing changed");
         } finally {
            GpuPref.runner = saved;
         }
      });

      test("gpu: Options.gpuHigh is on by default, is saved and read again, and apply(Options) does nothing outside Windows or with -Dbc.gpu=off", () -> {
         File dir = java.nio.file.Files.createTempDirectory("opt").toFile();
         Boolean savedWin = GpuPref.forceWindows;
         GpuPref.Runner savedRunner = GpuPref.runner;
         try {
            Options o = new Options(dir);
            check(o.gpuHigh, "default: high performance");
            o.gpuHigh = false;
            o.save();
            Options p = new Options(dir);
            p.load();
            check(!p.gpuHigh, "saved as off");
            o.gpuHigh = true;
            o.save();
            p.load();
            check(p.gpuHigh, "saved as on");
            List<String> log = new ArrayList<>();
            GpuPref.runner = cmd -> {
               log.add(String.join(" ", cmd));
               return new GpuPref.Out(1, "");
            };
            GpuPref.forceWindows = false;
            check(!GpuPref.apply(o) && log.isEmpty(), "not on Windows: no command is run");
            GpuPref.forceWindows = true;
            System.setProperty("bc.gpu", "off");
            check(!GpuPref.apply(o) && log.isEmpty(), "-Dbc.gpu=off: no command is run");
         } finally {
            System.clearProperty("bc.gpu");
            GpuPref.forceWindows = savedWin;
            GpuPref.runner = savedRunner;
            Tests311.deleteAll(dir);
         }
      });

      test("perf: render scale and frame limit are saved, read again and kept inside their range; the limit does nothing without a window", () -> {
         File dir = java.nio.file.Files.createTempDirectory("opt").toFile();
         try {
            Options o = new Options(dir);
            eq(100, o.renderScale, "default scale");
            eq(0, o.maxFps, "default: no limit");
            o.renderScale = 65;
            o.maxFps = 144;
            o.save();
            Options p = new Options(dir);
            p.load();
            eq(65, p.renderScale, "scale");
            eq(144, p.maxFps, "limit");
            java.util.Properties pr = new java.util.Properties();
            pr.setProperty("renderScale", "5");
            pr.setProperty("maxFps", "-3");
            try (java.io.FileOutputStream out = new java.io.FileOutputStream(new File(dir, "options.txt"))) {
               pr.store(out, "bad values");
            }

            p.load();
            eq(50, p.renderScale, "a scale below 50 is brought back to 50");
            eq(0, p.maxFps, "a negative limit is no limit");
            pr.setProperty("renderScale", "900");
            try (java.io.FileOutputStream out = new java.io.FileOutputStream(new File(dir, "options.txt"))) {
               pr.store(out, "bad values");
            }

            p.load();
            eq(100, p.renderScale, "a scale above 100 is brought back to 100");
            Game g = GameTests.newGame(1);
            eq(0, g.fpsCap(), "no window: no limit");
            for (int step : Screens.FPS_STEPS) {
               check(step == 0 || step >= 30 && step <= 240, "the limits to choose from are sensible: " + step);
            }
         } finally {
            Tests311.deleteAll(dir);
         }
      });

      test("gpu: which renderer strings are a chip inside the processor (the ones that are worth a second start)", () -> {
         String[] small = {
            "Intel(R) UHD Graphics 630", "Intel(R) HD Graphics 620", "Intel(R) Iris(R) Xe Graphics", "Intel(R) Iris(R) Plus Graphics", "Intel(R) UHD Graphics",
            "AMD Radeon(TM) Graphics", "AMD Radeon(TM) Vega 8 Graphics", "AMD Radeon(TM) RX Vega 11 Graphics", "AMD Radeon 780M Graphics", "AMD Radeon(TM) 610M", "Intel(R) UHD Graphics 770"
         };
         String[] big = {
            "NVIDIA GeForce RTX 3060 Laptop GPU/PCIe/SSE2", "NVIDIA GeForce GTX 1650/PCIe/SSE2", "Quadro T1000/PCIe/SSE2", "AMD Radeon RX 6600M", "AMD Radeon RX 580 Series", "AMD Radeon RX 7800 XT",
            "AMD Radeon(TM) RX 5700 XT", "Radeon RX 6800M", "AMD Radeon Pro 5500M", "Intel(R) Arc(TM) A770 Graphics", "llvmpipe (LLVM 17.0.6, 256 bits)", "Microsoft Basic Render Driver", "Mesa Software Renderer", "", null
         };
         for (String s : small) {
            check(GpuPref.isIntegrated(s), "a small chip: " + s);
         }

         for (String s : big) {
            check(!GpuPref.isIntegrated(s), "not a small chip: " + s);
         }
      });

      test("gpu: the names on the screens are short and plain", () -> {
         eq("NVIDIA GeForce RTX 3060 Laptop GPU", GpuPref.shortName("NVIDIA GeForce RTX 3060 Laptop GPU/PCIe/SSE2"), "the driver's suffix goes");
         eq("Intel UHD Graphics 630", GpuPref.shortName("Intel(R) UHD Graphics 630"), "(R) goes");
         eq("AMD Radeon Graphics", GpuPref.shortName("AMD Radeon(TM) Graphics"), "(TM) goes");
         eq("llvmpipe", GpuPref.shortName("llvmpipe (LLVM 17.0.6, 256 bits)"), "the software renderer");
         eq("unknown", GpuPref.shortName(""), "nothing known");
         eq("unknown", GpuPref.shortName(null), "null");
      });

      test("gpu: the second start is the same Java with the same options, the same jar and the same main class, without a debugger", () -> {
         File dir = java.nio.file.Files.createTempDirectory("jre2").toFile();
         try {
            File bin = new File(dir, "bin");
            bin.mkdirs();
            java.nio.file.Files.write(new File(bin, "javaw.exe").toPath(), new byte[]{1});
            List<String> cmd = GpuPref.relaunchCommand(dir.getPath(), Arrays.asList("-Xmx2G", "-Xms256m", "-agentlib:jdwp=transport=dt_socket", "-XX:+UseG1GC", "-Dbc.auto=1"), "C:\\g\\Blockcraft.exe", "bc.Main");
            eq(new File(bin, "javaw.exe").getPath(), cmd.get(0), "javaw.exe of the same Java");
            eq(Arrays.asList("-Xmx2G", "-Xms256m", "-XX:+UseG1GC", "-Dbc.auto=1"), cmd.subList(1, 5), "the options (without the debugger)");
            eq(Arrays.asList("-cp", "C:\\g\\Blockcraft.exe", "bc.Main"), cmd.subList(5, 8), "the jar (the exe is a jar) and the main class");
            eq(8, cmd.size(), "nothing else");
            eq("bc.Main", Main.class.getName(), "the class exists");
         } finally {
            Tests311.deleteAll(dir);
         }
      });

      test("gpu: PE export patching adds NvOptimusEnablement=1 and AmdPowerXpressRequestHighPerformance=1 to Blockcraft.exe and blockcraft-gpu.exe", () -> {
         byte[] stub = java.nio.file.Files.readAllBytes(new File("launcher/launch4j-stub.bin").toPath());
         byte[] patchedStub = GpuPref.patchPeExports(stub);
         check(patchedStub != null && GpuPref.hasPeExports(patchedStub), "Launch4j PE32 stub exports NvOptimusEnablement=1 and AmdPowerXpressRequestHighPerformance=1");
         File dir = java.nio.file.Files.createTempDirectory("jre-pe").toFile();
         try {
            File bin = new File(dir, "bin");
            bin.mkdirs();
            java.nio.file.Files.write(new File(bin, "javaw.exe").toPath(), stub);
            File gpuExe = GpuPref.ensureGpuExe(dir.getPath());
            check(gpuExe != null && gpuExe.isFile(), "blockcraft-gpu.exe created from PE javaw.exe");
            check(GpuPref.hasPeExports(java.nio.file.Files.readAllBytes(gpuExe.toPath())), "blockcraft-gpu.exe has discrete GPU PE exports");
            List<String> progs = GpuPref.programs(dir.getPath(), null);
            check(progs.contains(gpuExe.getPath()), "programs() registers blockcraft-gpu.exe for Windows DirectX GPU preference");
            List<String> cmd = GpuPref.relaunchCommand(dir.getPath(), Arrays.asList("-Xmx2G"), "C:\\g\\Blockcraft.exe", "bc.Main");
            eq(gpuExe.getPath(), cmd.get(0), "relaunchCommand prefers blockcraft-gpu.exe when javaw.exe is a real PE binary");
         } finally {
            Tests311.deleteAll(dir);
         }
      });
   }
}
