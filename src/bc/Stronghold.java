package bc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 * Update 3.12: strongholds - buried castles of stone brick with the way to the End. Every world has the same kind of rings of them around
 * the middle of the map (3 at 640..960 blocks, 6 at 1500..2100, 10 at 2800..3400); an Eye of Ender thrown into the air flies towards the
 * nearest one. A stronghold is a portal room in the middle (12 End portal frames around a pit of lava, some already holding an eye; a
 * spawner next to it), corridors and bends leading out of it, rooms, libraries with bookshelves and closets with loot chests.
 *
 * Like the nether fortress it is a {@link BoxPlan}, so every chunk builds its own part of it from the seed alone.
 */
final class Stronghold {
   static final int REACH = 62;
   private static final int[][] D = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};
   private static final int ROOM = 0;
   private static final int LIBRARY = 1;
   private static final int CLOSET = 2;
   private static final int NONE = 3;
   private static final int CORNER = 4;
   private static final int PORTAL = 5;
   private static final int[][] RINGS = {{3, 640, 960}, {6, 1500, 2100}, {10, 2800, 3400}};
   private static final HashMap<Long, Stronghold[]> CACHE = new HashMap<>();

   final int cx;
   final int cz;
   final int index;
   private final long seed;
   private Plan plan;

   private Stronghold(long seed, int index, int cx, int cz) {
      this.seed = seed;
      this.index = index;
      this.cx = cx;
      this.cz = cz;
   }

   static final class Plan extends BoxPlan {
      int ox;
      int oy;
      int oz;
      int rooms;
      boolean hasLibrary;
      final List<int[]> frames = new ArrayList<>();
      final List<int[]> chests = new ArrayList<>();
      final List<int[]> spawners = new ArrayList<>();
      final List<int[]> roomRects = new ArrayList<>();

      boolean roomFree(int x, int z, int w) {
         for (int[] r : this.roomRects) {
            if (x - w <= r[2] + 1 && x + w >= r[0] - 1 && z - w <= r[3] + 1 && z + w >= r[1] - 1) {
               return false;
            }
         }

         return true;
      }

      boolean inReach(int x, int z) {
         return Math.abs(x - this.ox) <= REACH && Math.abs(z - this.oz) <= REACH;
      }
   }

   /** All the strongholds of a world (the same list every time). */
   static Stronghold[] all(long seed) {
      synchronized (CACHE) {
         Stronghold[] s = CACHE.get(seed);
         if (s == null) {
            Random r = new Random(seed * 0x2545F4914F6CDD1DL + 0x5748L);
            List<Stronghold> list = new ArrayList<>();
            for (int[] ring : RINGS) {
               double base = r.nextDouble() * Math.PI * 2.0;
               for (int i = 0; i < ring[0]; i++) {
                  double a = base + i * Math.PI * 2.0 / ring[0] + (r.nextDouble() - 0.5) * 0.45;
                  double d = ring[1] + r.nextDouble() * (ring[2] - ring[1]);
                  list.add(new Stronghold(seed, list.size(), (int)Math.round(Math.cos(a) * d), (int)Math.round(Math.sin(a) * d)));
               }
            }

            s = list.toArray(new Stronghold[0]);
            if (CACHE.size() > 16) {
               CACHE.clear();
            }

            CACHE.put(seed, s);
         }

         return s;
      }
   }

   /** The stronghold nearest to a point (horizontally). */
   static Stronghold nearest(long seed, double x, double z) {
      Stronghold best = null;
      double bd = Double.MAX_VALUE;
      for (Stronghold s : all(seed)) {
         double d = (s.cx - x) * (s.cx - x) + (s.cz - z) * (s.cz - z);
         if (d < bd) {
            bd = d;
            best = s;
         }
      }

      return best;
   }

   /** Builds the strongholds' part of a freshly generated chunk of the Overworld. */
   static void apply(long seed, Chunk c) {
      int x0 = c.cx * 16;
      int z0 = c.cz * 16;
      for (Stronghold s : all(seed)) {
         if (Math.abs(s.cx - (x0 + 8)) <= REACH + 24 && Math.abs(s.cz - (z0 + 8)) <= REACH + 24) {
            s.plan().apply(c, Blocks.STONEBRICK, 255);
         }
      }
   }

   synchronized Plan plan() {
      if (this.plan == null) {
         this.plan = this.make();
      }

      return this.plan;
   }

   private Plan make() {
      Random r = new Random(this.seed * 0x9E3779B97F4A7C15L ^ this.index * 0xC2B2AE3D27D4EB4FL ^ 0x57A0E6L);
      Plan p = null;
      for (int attempt = 0; attempt < 12; attempt++) {
         p = new Plan();
         p.ox = this.cx;
         p.oz = this.cz;
         p.oy = 20 + r.nextInt(12);
         room(p, r, p.ox, p.oz, 5, 7, PORTAL, 0);
         int libDir = r.nextInt(4);
         int closetDir = (libDir + 1 + r.nextInt(3)) & 3;
         for (int d = 0; d < 4; d++) {
            chain(p, r, p.ox + D[d][0] * 7, p.oz + D[d][1] * 7, d, 0, d == libDir ? LIBRARY : (d == closetDir ? CLOSET : -1));
         }

         if (p.hasLibrary && !p.chests.isEmpty() && p.rooms >= 5) {
            break;
         }
      }

      return p;
   }

   private static void chain(Plan p, Random r, int x, int z, int d, int depth, int forced) {
      int segs = (depth == 0 ? 2 : 1) + r.nextInt(2);
      int dx = D[d][0];
      int dz = D[d][1];
      int built = 0;
      for (int s = 0; s < segs; s++) {
         int len = 5 + r.nextInt(9);
         int ex = x + dx * (len - 1);
         int ez = z + dz * (len - 1);
         if (!p.inReach(ex + dx * 7, ez + dz * 7)) {
            break;
         }

         corridor(p, r, x, z, d, len);
         built++;
         x = ex + dx;
         z = ez + dz;
         if (s < segs - 1 && r.nextInt(100) < 50) {
            int cx = x + dx * 3;
            int cz = z + dz * 3;
            int nd = (d + (r.nextBoolean() ? 1 : 3)) & 3;
            if (!p.inReach(cx + dx * 3 + D[nd][0] * 8, cz + dz * 3 + D[nd][1] * 8) || !p.roomFree(cx, cz, 3)) {
               break;
            }

            room(p, r, cx, cz, 2, 3, CORNER, d);
            d = nd;
            dx = D[d][0];
            dz = D[d][1];
            x = cx + dx * 4;
            z = cz + dz * 4;
         }
      }

      if (built == 0) {
         return;
      }

      int kind = forced;
      if (kind < 0) {
         int roll = r.nextInt(100);
         if (depth == 0) {
            kind = roll < 55 ? ROOM : (roll < 75 ? CLOSET : (roll < 85 && !p.hasLibrary ? LIBRARY : NONE));
         } else if (depth == 1) {
            kind = roll < 25 ? ROOM : (roll < 60 ? CLOSET : (roll < 68 && !p.hasLibrary ? LIBRARY : NONE));
         } else {
            kind = roll < 40 ? CLOSET : NONE;
         }
      }

      if (kind == NONE) {
         return;
      }

      int half = kind == CLOSET ? 2 : (kind == LIBRARY ? 4 : 3);
      int h = kind == CLOSET ? 3 : (kind == LIBRARY ? 6 : 5);
      int cx = x + dx * (half + 1);
      int cz = z + dz * (half + 1);
      if (!p.inReach(cx + dx * (half + 1), cz + dz * (half + 1)) || !p.inReach(cx - (half + 1), cz - (half + 1)) || !p.inReach(cx + half + 1, cz + half + 1)
         || !p.roomFree(cx, cz, half + 1)) {
         return;
      }

      room(p, r, cx, cz, half, h, kind, d);
      if (kind == ROOM) {
         for (int nd = 0; nd < 4; nd++) {
            if (nd == ((d + 2) & 3) || r.nextInt(100) >= 70) {
               continue;
            }

            chain(p, r, cx + D[nd][0] * (half + 2), cz + D[nd][1] * (half + 2), nd, depth + 1, -1);
         }
      }
   }

   private static int[] rect(int x, int z, int d, int len, int half) {
      int dx = D[d][0];
      int dz = D[d][1];
      int ex = x + dx * (len - 1);
      int ez = z + dz * (len - 1);
      return new int[]{Math.min(x, ex) - half * Math.abs(dz), Math.min(z, ez) - half * Math.abs(dx), Math.max(x, ex) + half * Math.abs(dz), Math.max(z, ez) + half * Math.abs(dx)};
   }

   /** A corridor: 3 wide and 3 high inside, mossy stones here and there, a torch on the floor now and then. */
   private static void corridor(Plan p, Random r, int x, int z, int d, int len) {
      int oy = p.oy;
      int dx = D[d][0];
      int dz = D[d][1];
      int[] s = rect(x, z, d, len, 2);
      p.add(p.shell, s[0], oy, s[1], s[2], oy + 4, s[3], Blocks.STONEBRICK);
      for (int k = 0; k < len; k++) {
         for (int sign = -1; sign <= 1; sign += 2) {
            if (r.nextInt(5) == 0) {
               int wx = x + dx * k - dz * 2 * sign;
               int wz = z + dz * k + dx * 2 * sign;
               int wy = oy + 1 + r.nextInt(3);
               p.add(p.shell, wx, wy, wz, wx, wy, wz, r.nextBoolean() ? Blocks.MOSSY_COBBLE : Blocks.COBBLE);
            }
         }
      }

      int[] a = rect(x - dx, z - dz, d, len + 2, 1);
      p.add(p.air, a[0], oy + 1, a[1], a[2], oy + 3, a[3], 0);
      for (int k = 2; k < len - 1; k += 6) {
         int sign = (k / 6) % 2 == 0 ? 1 : -1;
         int tx = x + dx * k - dz * sign;
         int tz = z + dz * k + dx * sign;
         p.add(p.decor, tx, oy + 1, tz, tx, oy + 1, tz, Blocks.TORCH);
      }
   }

   /** A closed room: inner half-size `half`, inner height `h`, centred on (cx, cz). `d` is the direction it is entered from. */
   private static void room(Plan p, Random r, int cx, int cz, int half, int h, int kind, int d) {
      int oy = p.oy;
      int w = half + 1;
      p.rooms++;
      p.roomRects.add(new int[]{cx - w, cz - w, cx + w, cz + w});
      p.add(p.shell, cx - w, oy, cz - w, cx + w, oy + h + 1, cz + w, Blocks.STONEBRICK);
      // mossy patches in the walls
      for (int i = 0; i < 4 + half * 2; i++) {
         int side = r.nextInt(4);
         int along = r.nextInt(2 * half + 1) - half;
         int wx = cx + (side < 2 ? (side == 0 ? -w : w) : along);
         int wz = cz + (side < 2 ? along : (side == 2 ? -w : w));
         int wy = oy + 1 + r.nextInt(h);
         p.add(p.shell, wx, wy, wz, wx, wy, wz, r.nextBoolean() ? Blocks.MOSSY_COBBLE : Blocks.COBBLE);
      }

      int innerHalf = kind == LIBRARY ? half - 1 : half;
      if (kind == LIBRARY) {
         // a ring of bookshelves three high along the walls (a corridor that arrives simply cuts through them)
         for (int i = -half; i <= half; i++) {
            p.add(p.shell, cx + i, oy + 1, cz - half, cx + i, oy + 3, cz - half, Blocks.BOOKSHELF);
            p.add(p.shell, cx + i, oy + 1, cz + half, cx + i, oy + 3, cz + half, Blocks.BOOKSHELF);
            p.add(p.shell, cx - half, oy + 1, cz + i, cx - half, oy + 3, cz + i, Blocks.BOOKSHELF);
            p.add(p.shell, cx + half, oy + 1, cz + i, cx + half, oy + 3, cz + i, Blocks.BOOKSHELF);
         }
      }

      p.add(p.air, cx - innerHalf, oy + 1, cz - innerHalf, cx + innerHalf, oy + h, cz + innerHalf, 0);
      int dx = D[d][0];
      int dz = D[d][1];
      if (kind == LIBRARY) {
         // the doorway through the bookshelves, behind the door the corridor cuts through the wall
         int ex = cx - dx * half;
         int ez = cz - dz * half;
         p.add(p.air, ex - Math.abs(dz), oy + 1, ez - Math.abs(dx), ex + Math.abs(dz), oy + 3, ez + Math.abs(dx), 0);
      }
      if (kind == PORTAL) {
         // the pit of lava with the 12 frames around it; one frame in ten already has its eye
         p.add(p.decor, cx - 2, oy - 2, cz - 2, cx + 2, oy - 1, cz + 2, Blocks.STONEBRICK);
         p.add(p.decor, cx - 1, oy - 1, cz - 1, cx + 1, oy - 1, cz + 1, Blocks.LAVA);
         p.add(p.decor, cx - 1, oy, cz - 1, cx + 1, oy, cz + 1, 0);
         int eyes = 0;
         for (int k = -1; k <= 1; k++) {
            int[][] cells = {{cx - 2, cz + k}, {cx + 2, cz + k}, {cx + k, cz - 2}, {cx + k, cz + 2}};
            for (int[] cell : cells) {
               boolean eye = r.nextInt(10) == 0 && eyes < 6;
               if (eye) {
                  eyes++;
               }

               p.add(p.decor, cell[0], oy, cell[1], cell[0], oy, cell[1], eye ? Blocks.END_FRAME_EYE : Blocks.END_FRAME);
               p.frames.add(new int[]{cell[0], oy, cell[1]});
            }
         }

         for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
               p.add(p.decor, cx + sx * half, oy + 1, cz + sz * half, cx + sx * half, oy + 1, cz + sz * half, Blocks.TORCH);
            }
         }

         // the spawner stands on the floor behind the portal
         p.add(p.decor, cx, oy + 1, cz + 4, cx, oy + 1, cz + 4, Blocks.SPAWNER);
         p.spawners.add(new int[]{cx, oy + 1, cz + 4});
      } else if (kind == LIBRARY) {
         p.hasLibrary = true;
         // a chest in the middle of the far side and torches in the corners
         int chestX = cx + dx * (innerHalf);
         int chestZ = cz + dz * (innerHalf);
         p.add(p.decor, chestX, oy + 1, chestZ, chestX, oy + 1, chestZ, Blocks.CHEST_LOOT);
         p.chests.add(new int[]{chestX, oy + 1, chestZ});
         for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
               p.add(p.decor, cx + sx * innerHalf, oy + 1, cz + sz * innerHalf, cx + sx * innerHalf, oy + 1, cz + sz * innerHalf, Blocks.TORCH);
            }
         }
      } else if (kind == CLOSET) {
         int chestX = cx + dx * half;
         int chestZ = cz + dz * half;
         p.add(p.decor, chestX, oy + 1, chestZ, chestX, oy + 1, chestZ, Blocks.CHEST_LOOT);
         p.chests.add(new int[]{chestX, oy + 1, chestZ});
         p.add(p.decor, cx - dx * half - dz * half, oy + 1, cz - dz * half + dx * half, cx - dx * half - dz * half, oy + 1, cz - dz * half + dx * half, Blocks.TORCH);
      } else if (kind == ROOM) {
         for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
               p.add(p.decor, cx + sx * half, oy + 1, cz + sz * half, cx + sx * half, oy + 1, cz + sz * half, Blocks.TORCH);
            }
         }
      }
   }

   /** Does this look like a stronghold with a ring of 12 frames? Used by the eyes of ender and the tests. */
   int portalY() {
      return this.plan().oy;
   }
}
