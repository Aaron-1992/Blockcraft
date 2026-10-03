package bc;

import static bc.GameTests.GROUND;
import static bc.GameTests.check;
import static bc.GameTests.eq;
import static bc.GameTests.newGame;
import static bc.GameTests.test;

import java.util.ArrayList;
import java.util.List;
import org.lwjgl.system.MemoryUtil;

/**
 * Headless tests of Update 3.11: faces are single-sided (back-face culling) and the face test of boxes (doors) was fixed;
 * the LOD follows what the world really looks like; hands.   python3 tools/runtests.py unit
 */
final class Tests311 {
   static void run() {
      culling();
      doors();
      lodSummaries();
      lodTiles();
      lodWorld();
      animations();
   }

   // ------------------------------------------------------------------ helpers
   /** The quads (4 vertices x, y, z) of the block at (x, y, z) that lie inside its cell, local to the block; opaque and translucent buffer. */
   static List<float[][]> quads(World w, Chunk c, int x, int y, int z, int id) {
      w.setBlock(x, y, z, id);
      Mesher.Result r = Mesher.build(w, c, y >> 4, false);
      ArrayList<float[][]> out = new ArrayList<>();
      int ly = y & 15;
      for (int pass = 0; pass < 2; pass++) {
         java.nio.FloatBuffer b = pass == 0 ? r.opaque : r.trans;
         int n = pass == 0 ? r.nO : r.nT;
         for (int i = 0; i + 3 < n; i += 4) {
            float[][] q = new float[4][3];
            boolean inside = true;
            for (int k = 0; k < 4; k++) {
               q[k][0] = b.get((i + k) * 11) - x;
               q[k][1] = b.get((i + k) * 11 + 1) - ly;
               q[k][2] = b.get((i + k) * 11 + 2) - z;
               for (int j = 0; j < 3; j++) {
                  inside &= q[k][j] >= -0.07F && q[k][j] <= 1.0001F; // crops sink 1/16 into the ground
               }
            }

            if (inside) {
               out.add(q);
            }
         }
      }

      if (r.opaque != null) {
         MemoryUtil.memFree(r.opaque);
      }

      if (r.trans != null) {
         MemoryUtil.memFree(r.trans);
      }

      w.setBlock(x, y, z, 0);
      return out;
   }

   static float[] normal(float[][] q) {
      float ax = q[1][0] - q[0][0];
      float ay = q[1][1] - q[0][1];
      float az = q[1][2] - q[0][2];
      float bx = q[2][0] - q[0][0];
      float by = q[2][1] - q[0][1];
      float bz = q[2][2] - q[0][2];
      return new float[]{ay * bz - az * by, az * bx - ax * bz, ax * by - ay * bx};
   }

   static float[] centre(float[][] q) {
      float[] c = new float[3];
      for (float[] v : q) {
         for (int j = 0; j < 3; j++) {
            c[j] += v[j] / 4.0F;
         }
      }

      return c;
   }

   static boolean same(float[] a, float[] b) {
      return Math.abs(a[0] - b[0]) < 1e-4F && Math.abs(a[1] - b[1]) < 1e-4F && Math.abs(a[2] - b[2]) < 1e-4F;
   }

   /** Does the list hold a quad with the same four corners in the opposite order (so it faces the other way)? */
   static boolean hasTwin(List<float[][]> all, float[][] q) {
      for (float[][] o : all) {
         if (o != q && same(o[0], q[0]) && same(o[1], q[3]) && same(o[2], q[2]) && same(o[3], q[1])) {
            return true;
         }
      }

      return false;
   }

   // ------------------------------------------------------------------ back-face culling
   static void culling() {
      test("culling: every face of every cube, box, stair, torch, ladder, door and lava / water side is wound counter-clockwise seen from outside", () -> {
         Game g = newGame(1);
         World w = g.world;
         Chunk c = w.chunk(0, 0);
         int y = GROUND + 8;
         int checked = 0;
         for (int id = 1; id < Blocks.COUNT; id++) {
            int kind = Blocks.kind[id];
            if (kind == 1 || kind == 7 || kind == 6 || id == Blocks.FIRE) {
               continue; // plants / crops / flames are two-sided (next test), a fence's bars touch its post
            }

            List<float[][]> qs = quads(w, c, 5, y, 5, id);
            double[] b = Blocks.box[id] != null ? Blocks.box[id] : new double[]{0, 0, 0, 1, 1, 1};
            float[] mid = {(float)(b[0] + b[3]) / 2, (float)(b[1] + b[4]) / 2, (float)(b[2] + b[5]) / 2};
            if (kind == 5 || kind == 3) { // wall torches are tilted boxes and a water surface sits low in its cell: judge them against their own middle
               float[] lo = {9, 9, 9};
               float[] hi = {-9, -9, -9};
               for (float[][] q : qs) {
                  for (float[] v : q) {
                     for (int j = 0; j < 3; j++) {
                        lo[j] = Math.min(lo[j], v[j]);
                        hi[j] = Math.max(hi[j], v[j]);
                     }
                  }
               }

               for (int j = 0; j < 3; j++) {
                  mid[j] = (lo[j] + hi[j]) / 2;
               }
            }

            for (float[][] q : qs) {
               float[] n = normal(q);
               float len = (float)Math.sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2]);
               if (len < 1e-9F) {
                  continue; // degenerate
               }

               if (kind == 3 && hasTwin(qs, q) && n[1] < 0) {
                  continue; // the back of a water / lava surface (checked in the next test)
               }

               float[] cq = centre(q);
               float dot = (n[0] * (cq[0] - mid[0]) + n[1] * (cq[1] - mid[1]) + n[2] * (cq[2] - mid[2])) / len;
               check(dot >= -1e-3F, "block " + id + " (" + Blocks.name[id] + "): a face points into the block (dot " + dot + ")");
               checked++;
            }
         }

         check(checked > 1200, "a lot of faces were checked: " + checked);
      });

      test("culling: plants, crops, flames and the top of a water / lava surface come in pairs that face opposite ways", () -> {
         Game g = newGame(1);
         World w = g.world;
         Chunk c = w.chunk(0, 0);
         int y = GROUND + 8;
         int blocks = 0;
         for (int id = 1; id < Blocks.COUNT; id++) {
            int kind = Blocks.kind[id];
            if (kind != 1 && kind != 7 && id != Blocks.FIRE) {
               continue;
            }

            List<float[][]> qs = quads(w, c, 5, y, 5, id);
            check(!qs.isEmpty(), "block " + id + " (" + Blocks.name[id] + ") has quads");
            eq(0, qs.size() % 2, "block " + id + ": an even number of quads");
            for (float[][] q : qs) {
               check(hasTwin(qs, q), "block " + id + " (" + Blocks.name[id] + "): every quad has its back");
            }

            blocks++;
         }

         check(blocks > 20, "plants checked: " + blocks);
         for (int id : new int[]{Blocks.WATER, Blocks.LAVA}) {
            List<float[][]> qs = quads(w, c, 5, y, 5, id);
            int ups = 0;
            int downs = 0;
            for (float[][] q : qs) {
               float[] n = normal(q);
               // the top surface: all four corners at the same height above the middle of the cell
               boolean flat = Math.abs(q[0][1] - q[2][1]) < 1e-4F && Math.abs(q[1][1] - q[3][1]) < 1e-4F && Math.abs(q[0][1] - q[1][1]) < 1e-4F && q[0][1] > 0.05F;
               if (flat) {
                  if (n[1] > 0) {
                     ups++;
                  } else {
                     downs++;
                  }
               }
            }

            eq(1, ups, "block " + id + ": one top surface seen from above");
            eq(1, downs, "block " + id + ": and one seen from below");
         }
      });

      test("culling: -Dbc.legacymesh restores the Update 3.5 meshes (no back faces)", () -> {
         Game g = newGame(1);
         World w = g.world;
         Chunk c = w.chunk(0, 0);
         int y = GROUND + 8;
         int tall = Blocks.TALLGRASS;
         int now = quads(w, c, 5, y, 5, tall).size();
         Mesher.legacy = true;
         try {
            int old = quads(w, c, 5, y, 5, tall).size();
            eq(2, old, "3.5: two crossed planes");
            eq(4, now, "now: with their backs");
         } finally {
            Mesher.legacy = false;
         }
      });
   }

   // ------------------------------------------------------------------ doors
   /** An independent statement of which faces of the box of block {@code id} may be left out: those lying on the cell's wall with an opaque block behind. */
   static void doors() {
      test("doors: every face of a door that is not pressed against an opaque neighbour is drawn - all 16 door blocks, all 64 combinations of opaque neighbours", () -> {
         Game g = newGame(1);
         World w = g.world;
         Chunk c = w.chunk(0, 0);
         int y = GROUND + 8;
         int[][] fn = Mesher.FN;
         int combos = 0;
         for (int id = Blocks.DOOR_LO; id < Blocks.DOOR_LO + 16; id++) {
            double[] box = Blocks.box[id];
            for (int mask = 0; mask < 64; mask++) {
               for (int f = 0; f < 6; f++) {
                  w.setBlock(5 + fn[f][0], y + fn[f][1], 5 + fn[f][2], (mask >> f & 1) != 0 ? Blocks.STONE : 0);
               }

               List<float[][]> qs = quads(w, c, 5, y, 5, id);
               for (int f = 0; f < 6; f++) {
                  int axis = f < 2 ? 1 : (f < 4 ? 2 : 0);
                  boolean positive = fn[f][axis] > 0;
                  double plane = positive ? box[3 + axis] : box[axis];
                  boolean onWall = positive ? plane >= 1.0 : plane <= 0.0;
                  boolean hidden = onWall && (mask >> f & 1) != 0;
                  // is there a quad on that plane whose normal points the right way?
                  boolean found = false;
                  for (float[][] q : qs) {
                     float[] n = normal(q);
                     boolean flat = true;
                     for (float[] v : q) {
                        flat &= Math.abs(v[axis] - plane) < 1e-3;
                     }

                     if (flat && n[axis] * fn[f][axis] > 0) {
                        found = true;
                     }
                  }

                  if (hidden) {
                     check(!found, "door " + id + " mask " + mask + ": face " + f + " is against an opaque block and is not drawn");
                  } else {
                     check(found, "door " + id + " mask " + mask + ": face " + f + " must be drawn");
                  }
               }

               combos++;
            }

            for (int f = 0; f < 6; f++) {
               w.setBlock(5 + fn[f][0], y + fn[f][1], 5 + fn[f][2], 0);
            }
         }

         eq(16 * 64, combos, "combinations tried");
      });

      test("doors: the old test lost the big face of a thin door when an opaque block stood against it (facing 0 + block behind, facing 1 + block in front)", () -> {
         Game g = newGame(1);
         World w = g.world;
         Chunk c = w.chunk(0, 0);
         int y = GROUND + 8;
         // closed door, facing 0 = a slab at z 0 .. 0.1875 whose visible big faces are -z (at z = 0) and +z (at z = 0.1875)
         w.setBlock(5, y, 6, Blocks.STONE);
         List<float[][]> qs = quads(w, c, 5, y, 5, Blocks.DOOR_LO);
         boolean back = false;
         for (float[][] q : qs) {
            back |= Math.abs(q[0][2] - 0.1875F) < 1e-4F && Math.abs(q[2][2] - 0.1875F) < 1e-4F && normal(q)[2] > 0;
         }

         check(back, "the +z face of a facing-0 door is drawn although stone stands at z + 1");
         w.setBlock(5, y, 6, 0);
         w.setBlock(5, y, 4, Blocks.STONE);
         qs = quads(w, c, 5, y, 5, Blocks.DOOR_LO + 1);
         back = false;
         for (float[][] q : qs) {
            back |= Math.abs(q[0][2] - 0.8125F) < 1e-4F && Math.abs(q[2][2] - 0.8125F) < 1e-4F && normal(q)[2] < 0;
         }

         check(back, "the -z face of a facing-1 door is drawn although stone stands at z - 1");
         w.setBlock(5, y, 4, 0);
         Mesher.legacy = true;
         try {
            w.setBlock(5, y, 4, Blocks.STONE);
            qs = quads(w, c, 5, y, 5, Blocks.DOOR_LO + 1);
            back = false;
            for (float[][] q : qs) {
               back |= Math.abs(q[0][2] - 0.8125F) < 1e-4F && Math.abs(q[2][2] - 0.8125F) < 1e-4F && normal(q)[2] < 0;
            }

            check(!back, "(the legacy mesh still has the old gap - proof that the test sees it)");
         } finally {
            Mesher.legacy = false;
            w.setBlock(5, y, 4, 0);
         }
      });
   }

   // ------------------------------------------------------------------ the LOD follows the world
   static final int SEA = 60;

   /** A chunk of flat ground: stone to y 58, dirt 59-60, grass at y 61 (so the surface plane is 62). */
   static byte[] flatChunk() {
      byte[] b = new byte[65536];
      for (int z = 0; z < 16; z++) {
         for (int x = 0; x < 16; x++) {
            b[Chunk.idx(x, 0, z)] = (byte)Blocks.BEDROCK;
            for (int y = 1; y <= 58; y++) {
               b[Chunk.idx(x, y, z)] = (byte)Blocks.STONE;
            }

            b[Chunk.idx(x, 59, z)] = (byte)Blocks.DIRT;
            b[Chunk.idx(x, 60, z)] = (byte)Blocks.DIRT;
            b[Chunk.idx(x, 61, z)] = (byte)Blocks.GRASS;
         }
      }

      return b;
   }

   static void put(byte[] b, int x, int y, int z, int id) {
      b[Chunk.idx(x, y, z)] = (byte)id;
   }

   static int grassTint() {
      return 0x5B9A3A;
   }

   static void lodSummaries() {
      Tex.initPixels();
      test("LOD summary: flat ground, a tower, a pit, water, a tree, a cabin pillar and plants - what is really on top, and what is not ground", () -> {
         byte[] b = flatChunk();
         int[] grass = new int[256];
         java.util.Arrays.fill(grass, grassTint());
         for (int y = 62; y <= 64; y++) {
            put(b, 4, y, 5, Blocks.STONE); // a tower of three stone blocks
         }

         for (int y = 61; y >= 57; y--) {
            put(b, 8, y, 8, Blocks.AIR); // a pit down to y 57 (stone at 57? no: dug out down to 57, so the floor is stone at y 56)
         }

         put(b, 12, 61, 3, Blocks.WATER); // water on dirt
         put(b, 12, 62, 3, Blocks.WATER);
         for (int y = 62; y <= 67; y++) {
            put(b, 10, y, 10, Blocks.LOG_OAK); // a trunk of 6 logs ...
         }

         for (int y = 66; y <= 68; y++) {
            put(b, 10, y, 10, Blocks.LEAVES_OAK); // ... with leaves over its top three: logs 62..65 are the trunk (4)
            put(b, 11, y, 10, Blocks.LEAVES_OAK);
         }

         for (int y = 62; y <= 64; y++) {
            put(b, 2, y, 12, Blocks.LOG_OAK); // a pillar of logs without leaves: a cabin post, solid ground
         }

         put(b, 6, 62, 6, Blocks.TALLGRASS);
         put(b, 7, 62, 6, Blocks.FLOWER_RED);
         put(b, 7, 62, 7, Blocks.TORCH);
         LodMap.Sum s = LodMap.summarize(b, grass, null, null, 0, 0);
         int flat = 3 * 16 + 14;
         eq(62, (int)s.top[flat], "flat ground: the plane is the top of the grass block");
         eq(0, (int)s.depth[flat], "no water");
         eq(0, (int)s.trunk[flat], "no tree");
         eq(LodMap.shade(grassTint(), 0.8F), s.rgb[flat], "grass takes its biome colour");
         eq(65, (int)s.top[5 * 16 + 4], "the tower: 3 blocks higher");
         eq(57, (int)s.top[8 * 16 + 8], "the pit: dug down to y 57, the floor is the stone block at y 56, so the plane is 57");
         eq(63, (int)s.top[3 * 16 + 12], "water: the plane of its surface (top water block y 62 + 1 = 63; the dirt is under it)");
         check(s.depth[3 * 16 + 12] == 2, "two blocks of water over its bed");
         eq(62, (int)s.top[10 * 16 + 10], "a tree: the crown and the trunk are not ground - the plane is the grass under it");
         eq(4, (int)s.trunk[10 * 16 + 10], "its four trunk logs (the ones with leaves over them) are counted");
         eq(62, (int)s.top[10 * 16 + 11], "a leaf-only column is not ground either");
         eq(0, (int)s.trunk[10 * 16 + 11], "and has no trunk");
         eq(65, (int)s.top[12 * 16 + 2], "a pillar of logs without leaves is solid ground");
         eq(0, (int)s.trunk[12 * 16 + 2], "and not a tree");
         eq(62, (int)s.top[6 * 16 + 6], "tall grass is not ground");
         eq(62, (int)s.top[6 * 16 + 7], "a flower is not ground");
         eq(62, (int)s.top[7 * 16 + 7], "a torch is not ground");
         check(s.side[flat] != 0 && s.side[flat] != s.rgb[flat], "the cliff under grass is dirt, not grass");
      });

      test("LOD summary: file round trip (lod.dat), chunk files without a summary are summarised by the scan, a newer chunk file replaces an old summary", () -> {
         java.io.File dir = java.nio.file.Files.createTempDirectory("lodmap").toFile();
         try {
            LodMap m = new LodMap();
            byte[] b = flatChunk();
            for (int y = 62; y <= 66; y++) {
               put(b, 3, y, 3, Blocks.COBBLE);
            }

            LodMap.Sum s = LodMap.summarize(b, null, null, new Gen(5), 3, -2);
            s.stamp = 1234L;
            m.put(3, -2, s);
            check(m.dirtyFile, "marked dirty");
            m.save(dir);
            check(!m.dirtyFile, "saved");
            LodMap m2 = new LodMap();
            m2.load(dir);
            eq(1, m2.size(), "one summary read back");
            LodMap.Sum r = m2.get(3, -2);
            check(r != null, "the summary of chunk 3,-2 is there");
            check(java.util.Arrays.equals(s.top, r.top) && java.util.Arrays.equals(s.rgb, r.rgb) && java.util.Arrays.equals(s.side, r.side)
               && java.util.Arrays.equals(s.depth, r.depth) && java.util.Arrays.equals(s.trunk, r.trunk), "every column the same");
            eq(1234L, r.stamp, "the stamp too");
            // the scan: chunk files c.<x>.<z>.gz hold the gzipped blocks
            java.io.File f = new java.io.File(dir, "c.-4.7.gz");
            try (java.io.DataOutputStream o = new java.io.DataOutputStream(new java.util.zip.GZIPOutputStream(new java.io.FileOutputStream(f)))) {
               o.write(b);
            }

            LodMap m3 = new LodMap();
            eq(1, m3.scanNow(dir, new Gen(5), null, 0), "the scan summarised the file");
            check(m3.get(-4, 7) != null, "chunk -4,7");
            eq(f.lastModified(), m3.get(-4, 7).stamp, "stamped with the file's time");
            eq(67, (int)m3.get(-4, 7).top[3 * 16 + 3], "and shows the pillar (5 cobblestone blocks on the grass at y 61: top block y 66)");
            eq(0, m3.scanNow(dir, new Gen(5), null, 0), "a second scan finds nothing new");
            check(f.setLastModified(f.lastModified() + 60000L), "touch the file");
            eq(1, m3.scanNow(dir, new Gen(5), null, 0), "a newer file is summarised again");
            eq(0, m3.scanNow(dir, new Gen(5), (cx, cz) -> true, 0), "(skipped chunks are left to their live data)");
            LodMap m4 = new LodMap();
            m4.put(-4, 7, LodMap.summarize(b, null, null, null, -4, 7)); // a live summary: stamp = MAX
            eq(0, m4.scanNow(dir, new Gen(5), null, 0), "a summary of live data is never replaced by a file");
         } finally {
            deleteAll(dir);
         }
      });
   }

   static void deleteAll(java.io.File f) {
      java.io.File[] kids = f.listFiles();
      if (kids != null) {
         for (java.io.File k : kids) {
            deleteAll(k);
         }
      }

      f.delete();
   }

   /** One decoded LOD tile: quads of 4 corners (x, y, z) and the vertex colour as it is stored. */
   static final class LodQuad {
      float[][] v = new float[4][3];
      int col;
   }

   static List<LodQuad> decode(Lod.Tile t) {
      ArrayList<LodQuad> out = new ArrayList<>();
      java.nio.ByteBuffer b = t.data.duplicate().order(java.nio.ByteOrder.nativeOrder());
      b.rewind();
      for (int q = 0; q < t.vcount / 4; q++) {
         LodQuad lq = new LodQuad();
         for (int k = 0; k < 4; k++) {
            lq.v[k][0] = b.getFloat();
            lq.v[k][1] = b.getFloat();
            lq.v[k][2] = b.getFloat();
            lq.col = b.getInt();
         }

         out.add(lq);
      }

      return out;
   }

   /** The top quad (face code 255 / 180) of the cell whose lower corner is (x, z) in tile-local coordinates, or null. */
   static LodQuad topOf(List<LodQuad> qs, float x, float z) {
      for (LodQuad q : qs) {
         int code = q.col >>> 24;
         if ((code == 255 || code == 180) && Math.abs(q.v[0][1] - q.v[2][1]) < 1e-4F && Math.min(Math.min(q.v[0][0], q.v[1][0]), Math.min(q.v[2][0], q.v[3][0])) > x - 0.2F
            && Math.min(Math.min(q.v[0][0], q.v[1][0]), Math.min(q.v[2][0], q.v[3][0])) < x + 0.2F
            && Math.min(Math.min(q.v[0][2], q.v[1][2]), Math.min(q.v[2][2], q.v[3][2])) > z - 0.2F
            && Math.min(Math.min(q.v[0][2], q.v[1][2]), Math.min(q.v[2][2], q.v[3][2])) < z + 0.2F) {
            return q;
         }
      }

      return null;
   }

   static Lod.Tile built(Lod lod, int size, int x0, int z0) {
      Lod.Tile t = new Lod.Tile(size, x0, z0);
      lod.build(t);
      return t;
   }

   static void lodTiles() {
      test("LOD tile: a cell over a changed chunk shows its real surface, the rest is still the generator; trees follow their trunks; all quads face outwards", () -> {
         Gen gen = new Gen(12345L);
         LodMap map = new LodMap();
         Lod lod = new Lod(null, gen, map);
         try {
            Lod.Tile plain = built(lod, 32, 0, 0);
            LodQuad g0 = topOf(decode(plain), 20, 20);
            check(g0 != null, "a top quad at 20,20");
            Gen.Col col = gen.column(20, 20);
            check(Math.abs(g0.v[0][1] - (col.h + 1 - 0.75F)) < 1e-3F || col.h < 63, "the generator's height under the 1-block cells: " + g0.v[0][1] + " vs " + (col.h + 1));
            // a summary for chunk 0,0 only: a 5-high tower at (4, 5) on flat ground at plane 70
            byte[] b = flatChunk();
            for (int y = 0; y < 256; y++) {
               if (y > 61) {
                  break;
               }
            }

            LodMap.Sum s = new LodMap.Sum();
            for (int i = 0; i < 256; i++) {
               s.top[i] = 70;
               s.rgb[i] = 0x336699;
               s.side[i] = 0x445566;
            }

            s.top[5 * 16 + 4] = 75;
            s.rgb[5 * 16 + 4] = 0xAA2222;
            map.put(0, 0, s);
            Lod.Tile tile = built(lod, 32, 0, 0);
            List<LodQuad> qs = decode(tile);
            LodQuad tower = topOf(qs, 4, 5);
            check(tower != null, "the tower's cell has a top quad");
            check(Math.abs(tower.v[0][1] - (75 - 0.75F)) < 1e-3F, "its top is at the real height: " + tower.v[0][1]);
            eq(255, tower.col >>> 24, "face code 255");
            int r = tower.col & 255;
            int g = tower.col >> 8 & 255;
            int bl = tower.col >> 16 & 255;
            check(Math.abs(r - 0xAA) < 24 && Math.abs(g - 0x22) < 12 && Math.abs(bl - 0x22) < 12, "its colour is the real one (r, g, b from the low byte up, with the little brightness jitter): " + r + "," + g + "," + bl);
            LodQuad plainCell = topOf(qs, 10, 10);
            check(Math.abs(plainCell.v[0][1] - (70 - 0.75F)) < 1e-3F, "the cells around it are flat at 70: " + plainCell.v[0][1]);
            LodQuad outside = topOf(qs, 20, 20);
            check(Math.abs(outside.v[0][1] - g0.v[0][1]) < 1e-3F, "over chunk 1 (no summary) the generator still rules: " + outside.v[0][1]);
            // walls: the tower has a wall down to its neighbours, coloured with the cliff colour
            boolean wall = false;
            for (LodQuad q : qs) {
               int code = q.col >>> 24;
               if (code != 255 && code != 180 && Math.abs(q.v[0][0] - 4) < 1e-3F && Math.abs(q.v[1][0] - 4) < 1e-3F && q.v[0][1] > 73) {
                  wall = true;
               }
            }

            check(wall, "the tower has a wall on its west side");
         } finally {
            lod.dispose();
         }
      });

      test("LOD tile: a tree of the generator is drawn only while its trunk stands in the changed chunk; houses of a changed chunk are not drawn", () -> {
         Gen gen = new Gen(12345L);
         int treeTiles = 0;
         int houseTiles = 0;
         Lod probe = new Lod(null, gen, new LodMap());
         try {
            for (int i = 0; i < 48 && (treeTiles < 2 || houseTiles < 1); i++) {
               int tx = (i % 8) * 64 - 256;
               int tz = (i / 8) * 64 - 128;
               LodMap m = new LodMap();
               Lod lod = new Lod(null, gen, m);
               try {
                  boolean village = !lod.villagesFor(tx - 8, tz - 8, tx + 40, tz + 40).isEmpty();
                  int baseline = built(lod, 32, tx, tz).vcount;
                  // every chunk of the tile summarised as the generator's own surface, with no trunks / with a trunk in every column
                  for (int cx = tx >> 4; cx <= tx + 31 >> 4; cx++) {
                     for (int cz = tz >> 4; cz <= tz + 31 >> 4; cz++) {
                        m.put(cx, cz, genSummary(gen, cx, cz, 0));
                     }
                  }

                  int noTrees = built(lod, 32, tx, tz).vcount;
                  for (int cx = tx >> 4; cx <= tx + 31 >> 4; cx++) {
                     for (int cz = tz >> 4; cz <= tz + 31 >> 4; cz++) {
                        m.put(cx, cz, genSummary(gen, cx, cz, 5));
                     }
                  }

                  int withTrunks = built(lod, 32, tx, tz).vcount;
                  eq(0, (baseline - noTrees) % 4, "(whole quads)");
                  if (village) {
                     check(withTrunks < baseline || baseline == withTrunks, "a tile with a village (" + tx + "," + tz + "): no generator houses over changed chunks: " + withTrunks + " <= " + baseline);
                     if (withTrunks < baseline) {
                        houseTiles++;
                     }
                  } else if (noTrees < baseline) {
                     eq(baseline, withTrunks, "no village: with a trunk in every column the tile is exactly what the generator draws (tile " + tx + "," + tz + ")");
                     treeTiles++;
                  }
               } finally {
                  lod.dispose();
               }
            }

            check(treeTiles >= 2, "tiles with trees but no village were found and checked: " + treeTiles);
            check(houseTiles >= 1, "a tile with a village lost its houses over changed chunks: " + houseTiles);
         } finally {
            probe.dispose();
         }
      });

      test("LOD tile: every quad - land, water, walls, skirts, trees, houses - is counter-clockwise seen from the side its face code names", () -> {
         Gen gen = new Gen(12345L);
         Lod lod = new Lod(null, gen, new LodMap());
         try {
            int quads = 0;
            for (int[] spec : new int[][]{{32, 96, 32}, {32, -128, 64}, {64, 0, -128}, {128, 128, 128}, {256, -512, 0}, {32, 130, 60}}) {
               Lod.Tile t = built(lod, spec[0], spec[1], spec[2]);
               for (LodQuad q : decode(t)) {
                  float ax = q.v[1][0] - q.v[0][0];
                  float ay = q.v[1][1] - q.v[0][1];
                  float az = q.v[1][2] - q.v[0][2];
                  float bx = q.v[2][0] - q.v[0][0];
                  float by = q.v[2][1] - q.v[0][1];
                  float bz = q.v[2][2] - q.v[0][2];
                  float nx = ay * bz - az * by;
                  float ny = az * bx - ax * bz;
                  float nz = ax * by - ay * bx;
                  float len = (float)Math.sqrt(nx * nx + ny * ny + nz * nz);
                  if (len < 1e-6F) {
                     continue;
                  }

                  int code = q.col >>> 24;
                  float[] want = code >= 247 || code == 180 ? new float[]{0, 1, 0} : code > 233 ? new float[]{0, 0, -1} : code > 218 ? new float[]{0, 0, 1}
                     : code > 203 ? new float[]{-1, 0, 0} : new float[]{1, 0, 0};
                  float dot = (nx * want[0] + ny * want[1] + nz * want[2]) / len;
                  check(dot > 0.999F, "tile " + spec[0] + " at " + spec[1] + "," + spec[2] + ": a quad with face code " + code + " faces the wrong way (dot " + dot + ")");
                  quads++;
               }
            }

            check(quads > 6000, "quads checked: " + quads);
         } finally {
            lod.dispose();
         }
      });

      test("LOD tile: a change makes the tiles that contain the chunk outdated; the rebuilt one takes the old one's place in one step", () -> {
         Gen gen = new Gen(12345L);
         LodMap map = new LodMap();
         Lod lod = new Lod(null, gen, map);
         try {
            Lod.Tile a = built(lod, 32, 0, 0); // contains chunk 0,0
            Lod.Tile far = built(lod, 32, 512, 512);
            Lod.Tile big = built(lod, 256, 0, 0);
            a.state = far.state = big.state = 3;
            lod.tiles.put(Lod.key(32, 0, 0), a);
            lod.tiles.put(Lod.key(32, 512, 512), far);
            lod.tiles.put(Lod.key(256, 0, 0), big);
            long before = a.builtEpoch;
            map.put(0, 0, genSummary(gen, 0, 0, 0)); // the listener of the Lod
            check(a.dirtyEpoch > a.builtEpoch, "the tile over chunk 0,0 is outdated");
            check(big.dirtyEpoch > big.builtEpoch, "the 256 tile that contains it too");
            check(far.dirtyEpoch <= far.builtEpoch, "a tile elsewhere is not");
            lod.frame = 1; // too early: nothing is rebuilt yet
            check(lod.refresh(a) == a && a.replacement == null, "not rebuilt before the tile has had its rest");
            lod.frame = 10000;
            check(lod.refresh(a) == a, "still the old one is drawn");
            check(a.replacement != null, "the rebuild was started");
            long t0 = System.currentTimeMillis();
            while (a.replacement.state != 2 && System.currentTimeMillis() - t0 < 20000) {
               Thread.sleep(5);
            }

            eq(2, a.replacement.state, "the replacement was built");
            check(lod.refresh(a) == a, "built but not uploaded: the old tile is still the one that is drawn");
            Lod.Tile r = a.replacement;
            r.state = 3; // (the upload needs OpenGL)
            check(lod.refresh(a) == r, "uploaded: the new tile replaces it");
            check(lod.tiles.get(Lod.key(32, 0, 0)) == r, "and sits in the tile map");
            check(r.builtEpoch >= a.dirtyEpoch, "it was built after the change");
            check(lod.refresh(r) == r && r.replacement == null, "and is not outdated");
         } finally {
            lod.dispose();
         }
      });

      test("covered: a chunk counts as meshed once all 16 sections were, and stays so while a section is being meshed again", () -> {
         Chunk c = new Chunk(0, 0);
         for (int sy = 0; sy < 15; sy++) {
            c.markMeshed(sy);
            check(!c.everMeshed, "not yet after " + (sy + 1) + " sections");
         }

         c.markMeshed(15);
         check(c.everMeshed, "all 16");
         c.sections[7].dirty = true;
         c.sections[7].inFlight = true;
         check(c.everMeshed, "a section that is re-meshed does not uncover the chunk");
      });
   }

   /** A summary that says what the generator would draw (surface plane h + 1, grass colour), with a trunk count in every column. */
   static LodMap.Sum genSummary(Gen gen, int cx, int cz, int trunk) {
      LodMap.Sum s = new LodMap.Sum();
      for (int z = 0; z < 16; z++) {
         for (int x = 0; x < 16; x++) {
            Gen.Col c = gen.column(cx * 16 + x, cz * 16 + z);
            int i = z * 16 + x;
            boolean sea = c.h < 63;
            s.top[i] = (short)(sea ? 63 : c.h + 1);
            s.depth[i] = (byte)(sea ? 5 : 0);
            s.rgb[i] = sea ? 0x3A68D0 : 0x5B9A3A;
            s.side[i] = 0x6B4F33;
            s.trunk[i] = (byte)trunk;
         }
      }

      return s;
   }

   static void lodWorld() {
      test("LOD in the world: a changed chunk gets a summary two seconds after the change and when it is saved; lod.dat and old chunk files are read at the next start", () -> {
         java.io.File dir = java.nio.file.Files.createTempDirectory("lodworld").toFile();
         try {
            World w = new World(777L, dir);
            for (int cz = 0; cz <= 1; cz++) {
               for (int cx = 0; cx <= 1; cx++) {
                  Chunk c = new Chunk(cx, cz);
                  byte[] flat = flatChunk();
                  System.arraycopy(flat, 0, c.blocks, 0, flat.length);
                  c.computeHeightmap();
                  c.lit = true;
                  w.chunks.put(World.key(cx, cz), c);
               }
            }

            Chunk c00 = w.chunk(0, 0);
            eq(0L, c00.lodDirty, "a fresh chunk is clean");
            check(w.lodMap.get(0, 0) == null, "and has no summary: the generator draws it");
            w.setBlock(5, 62, 5, Blocks.STONE);
            w.setBlock(5, 63, 5, Blocks.STONE);
            check(c00.lodDirty != 0L, "a change marks the chunk");
            for (int i = 0; i < 39; i++) {
               w.tick();
            }

            check(w.lodMap.get(0, 0) == null, "no summary before it has been quiet for two seconds");
            for (int i = 0; i < 25; i++) {
               w.tick();
            }

            LodMap.Sum s = w.lodMap.get(0, 0);
            check(s != null, "two seconds after the last change: a summary");
            eq(64, (int)s.top[5 * 16 + 5], "with the two blocks on top");
            eq(0L, c00.lodDirty, "the chunk is clean again");
            check(w.lodMap.get(1, 0) == null, "other chunks are not summarised");
            w.setBlock(21, 62, 5, Blocks.COBBLE); // chunk 1,0
            w.saveAll();
            check(new java.io.File(dir, "lod.dat").exists(), "lod.dat was written");
            check(w.lodMap.get(1, 0) != null, "saving a changed chunk summarises it at once (it is about to be unloaded)");
            check(w.lodMap.get(1, 0).stamp != Long.MAX_VALUE, "and the summary carries the chunk file's time");
            w.shutdown();
            World w2 = new World(777L, dir); // the next start
            eq(2, w2.lodMap.size(), "both summaries were read from lod.dat");
            eq(64, (int)w2.lodMap.get(0, 0).top[5 * 16 + 5], "with the same surface");
            // an old save: chunk files, no lod.dat
            java.io.File dir2 = java.nio.file.Files.createTempDirectory("lodold").toFile();
            try {
               java.io.File f = new java.io.File(dir2, "c.2.3.gz");
               byte[] flat = flatChunk();
               put(flat, 1, 62, 1, Blocks.BRICKS);
               try (java.io.DataOutputStream o = new java.io.DataOutputStream(new java.util.zip.GZIPOutputStream(new java.io.FileOutputStream(f)))) {
                  o.write(flat);
               }

               World w3 = new World(777L, dir2);
               long t0 = System.currentTimeMillis();
               while (w3.lodMap.get(2, 3) == null && System.currentTimeMillis() - t0 < 20000) {
                  Thread.sleep(10);
               }

               check(w3.lodMap.get(2, 3) != null, "a chunk file from before 3.11 was summarised by the background scan");
               eq(63, (int)w3.lodMap.get(2, 3).top[1 * 16 + 1], "showing the brick block on it");
               w3.shutdown();
            } finally {
               deleteAll(dir2);
            }

            w2.shutdown();
         } finally {
            deleteAll(dir);
         }
      });
   }

   // ------------------------------------------------------------------ animations (Anim)
   static Mobs.Mob mob(int type) {
      Mobs.Mob m = new Mobs.Mob();
      m.type = type;
      m.health = 10;
      m.onGround = true;
      m.setPos(10.5, 61, 10.5);
      return m;
   }

   static void animations() {
      test("animation: the step's bounce and roll are bounded, smooth, and absent where they do not belong (air, creeper, spider)", () -> {
         for (int type = 0; type < Mobs.N; type++) {
            float last = Float.NaN;
            float lastRoll = Float.NaN;
            for (float walk = 0.0F; walk < 60.0F; walk += 0.02F) {
               for (float amp : new float[]{0.0F, 0.3F, 1.0F}) {
                  float b = Anim.bounce(type, walk, amp, true);
                  float r = Anim.roll(type, walk, amp);
                  check(b >= 0.0F && b <= 0.0501F, "type " + type + ": bounce within 0..0.05 blocks: " + b);
                  check(Math.abs(r) <= 5.001F, "type " + type + ": roll within 5 degrees: " + r);
                  check(Anim.bounce(type, walk, amp, false) == 0.0F, "no bounce in the air");
                  if (amp == 1.0F) {
                     if (!Float.isNaN(last)) {
                        check(Math.abs(b - last) < 0.004F, "bounce is smooth: " + last + " -> " + b);
                        check(Math.abs(r - lastRoll) < 0.4F, "roll is smooth: " + lastRoll + " -> " + r);
                     }

                     last = b;
                     lastRoll = r;
                  }
               }
            }
         }

         check(Anim.bounce(Mobs.CREEPER, 1.0F, 1.0F, true) == 0.0F && Anim.bounce(Mobs.SPIDER, 1.0F, 1.0F, true) == 0.0F, "creepers and spiders glide");
         check(Anim.bounce(Mobs.PIG, 0.98F, 0.0F, true) == 0.0F, "a standing animal does not bounce");
      });

      test("animation: breathing stays within 2 %, a hit throws the body back and is over after 10 ticks", () -> {
         Mobs.Mob m = mob(Mobs.COW);
         float lo = 9;
         float hi = -9;
         for (int age = 0; age < 2000; age++) {
            m.age = age;
            float b = Anim.breathe(m, 0.5F);
            lo = Math.min(lo, b);
            hi = Math.max(hi, b);
         }

         check(lo > 0.98F && hi < 1.02F && hi - lo > 0.01F, "breathing between " + lo + " and " + hi);
         eq(0.0F, Anim.hurtTilt(m, 0.0F), "not hurt: no tilt");
         float worst = 0;
         for (int t = 10; t >= 1; t--) {
            m.hurtTime = t;
            for (float partial = 0.0F; partial < 1.0F; partial += 0.1F) {
               float tilt = Anim.hurtTilt(m, partial);
               check(tilt <= 0.0F && tilt >= -14.001F, "tilt is backwards and at most 14 degrees: " + tilt);
               worst = Math.min(worst, tilt);
            }
         }

         check(worst < -12.0F, "the worst tilt is close to 14 degrees: " + worst);
         m.hurtTime = 0;
         eq(0.0F, Anim.hurtTilt(m, 0.9F), "over");
      });

      test("animation: animals graze (head down, chewing) now and then while they stand still - never when walking, hunted, in love, dead or of another kind", () -> {
         for (int type : new int[]{Mobs.PIG, Mobs.COW, Mobs.SHEEP}) {
            Mobs.Mob m = mob(type);
            float max = 0;
            float prev = Float.NaN;
            float biggestStep = 0;
            int grazing = 0;
            for (int age = 0; age < 4000; age++) {
               for (float partial = 0.0F; partial < 1.0F; partial += 0.25F) {
                  m.age = age;
                  float g = Anim.graze(m, partial);
                  check(g >= 0.0F && g <= 1.0F, "graze within 0..1: " + g);
                  max = Math.max(max, g);
                  if (!Float.isNaN(prev)) {
                     biggestStep = Math.max(biggestStep, Math.abs(g - prev));
                  }

                  prev = g;
                  if (g > 0.5F) {
                     grazing++;
                  }

                  float pitch = Anim.headPitch(m, partial, 0.0F, 0.0F);
                  check(pitch >= -0.001F && pitch <= 64.5F, "head pitch within 0..64.5: " + pitch);
               }
            }

            check(max > 0.99F, "type " + type + " reaches a full graze");
            check(biggestStep < 0.04F, "type " + type + ": the head moves smoothly (largest step per quarter tick " + biggestStep + ")");
            check(grazing > 4 * 4000 / 400 * 30 / 2 && grazing < 4 * 4000 * 0.3, "about 15 % of the time: " + grazing + " of " + 4 * 4000);
            m.age = 0;
            for (int age = 0; age < 400; age++) {
               m.age = age;
               m.moving = true;
               check(Anim.graze(m, 0.0F) == 0.0F, "no grazing while walking");
               m.moving = false;
               m.walkAmp = 0.5F;
               check(Anim.graze(m, 0.0F) == 0.0F, "no grazing while the legs still move");
               m.walkAmp = 0.0F;
               m.hasTarget = true;
               check(Anim.graze(m, 0.0F) == 0.0F, "no grazing while hunted");
               m.hasTarget = false;
               m.love = 5;
               check(Anim.graze(m, 0.0F) == 0.0F, "no grazing in love");
               m.love = 0;
               m.dead = true;
               check(Anim.graze(m, 0.0F) == 0.0F, "no grazing when dead");
               m.dead = false;
            }
         }

         for (int type : new int[]{Mobs.CHICKEN, Mobs.ZOMBIE, Mobs.SKELETON, Mobs.CREEPER, Mobs.SPIDER}) {
            Mobs.Mob m = mob(type);
            for (int age = 0; age < 800; age++) {
               m.age = age;
               check(Anim.graze(m, 0.3F) == 0.0F, "type " + type + " never grazes");
            }
         }
      });

      test("animation: legs in the air spread (front forward, back backward), babies get bigger heads, chickens peck", () -> {
         Mobs.Mob pig = mob(Mobs.PIG);
         eq(0.0F, Anim.air(pig), "on the ground: no spread");
         eq(0.0F, Anim.spread(pig, 5.0F), "(neither of the legs)");
         pig.onGround = false;
         pig.vy = 0.4;
         float front = Anim.spread(pig, 5.0F);
         float back = Anim.spread(pig, -5.0F);
         check(front < -10.0F && back > 10.0F && Math.abs(front + back) < 1e-4F, "front legs forward, back legs backward: " + front + " / " + back);
         check(Anim.air(pig) <= 1.0F, "at most 1");
         pig.inWater = true;
         eq(0.0F, Anim.air(pig), "swimming: no spread");
         Mobs.Mob zombie = mob(Mobs.ZOMBIE);
         zombie.onGround = false;
         eq(0.0F, Anim.spread(zombie, 5.0F), "a zombie's legs are left alone");
         eq(1.0F, Anim.babyHead(pig), "an adult");
         pig.babyAge = 100;
         check(Anim.babyHead(pig) > 1.2F, "a baby's head is bigger");
         Mobs.Mob hen = mob(Mobs.CHICKEN);
         float lo = 9;
         float hi = -9;
         for (float walk = 0; walk < 20; walk += 0.01F) {
            float p = Anim.peck(hen, walk, 1.0F);
            lo = Math.min(lo, p);
            hi = Math.max(hi, p);
         }

         check(lo < -0.04F && hi > 0.04F, "the head goes back and forth: " + lo + " .. " + hi);
         eq(0.0F, Anim.peck(pig, 1.0F, 1.0F), "pigs do not peck");
      });
   }
}
