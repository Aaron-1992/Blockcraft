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
}
