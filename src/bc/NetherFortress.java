package bc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 * Update 3.12: nether fortresses - dark castles of nether bricks that hang over the lava sea: bridges with railings on pillars,
 * covered corridors with windows, pillared halls, a throne room with a blaze spawner and closets with loot chests.
 *
 * The Nether is cut into regions of {@value #REG} blocks; every region has (with {@value #CHANCE} %) one fortress, planned from the seed and
 * the region only and always lying completely inside it, so every chunk can build its own part of it without looking at its neighbours.
 * A plan is a list of boxes in three layers - walls and floors first, then the air that is cut out of them (so passages between pieces
 * always stay open), then the details (railings, windows, lamps, spawner, chests).
 */
final class NetherFortress {
   static final int REG = 256;
   static final int CHANCE = 70;
   /** no piece reaches further than this from the middle of the fortress */
   static final int REACH = 88;
   private static final int[][] D = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};
   private static final int HALL = 0;
   private static final int THRONE = 1;
   private static final int CLOSET = 2;
   private static final int NONE = 3;
   private static final int CORNER = 4;

   private final long seed;
   private final HashMap<Long, Plan> plans = new HashMap<>();

   NetherFortress(long seed) {
      this.seed = seed;
   }

   static final class Box {
      final int x0, y0, z0, x1, y1, z1;
      final short id;
      /** details like railings, windows and lamps never fill a passage that another piece cut through them */
      boolean soft;

      Box(int xa, int ya, int za, int xb, int yb, int zb, int id) {
         this.x0 = Math.min(xa, xb);
         this.x1 = Math.max(xa, xb);
         this.y0 = Math.min(ya, yb);
         this.y1 = Math.max(ya, yb);
         this.z0 = Math.min(za, zb);
         this.z1 = Math.max(za, zb);
         this.id = (short)id;
      }
   }

   static final class Plan {
      boolean present;
      int ox;
      int oy;
      int oz;
      int minX = Integer.MAX_VALUE;
      int maxX = Integer.MIN_VALUE;
      int minZ = Integer.MAX_VALUE;
      int maxZ = Integer.MIN_VALUE;
      final List<Box> shell = new ArrayList<>();
      final List<Box> air = new ArrayList<>();
      final List<Box> decor = new ArrayList<>();
      /** columns that grow downwards from the floor until they find rock: x, y of the topmost block, z */
      final List<int[]> pillars = new ArrayList<>();
      final List<int[]> spawners = new ArrayList<>();
      final List<int[]> chests = new ArrayList<>();
      /** outer x0, z0, x1, z1 of every room - rooms never overlap each other */
      final List<int[]> roomRects = new ArrayList<>();
      boolean hasThrone;
      int rooms;

      void soft(int xa, int ya, int za, int xb, int yb, int zb, int id) {
         this.add(this.decor, xa, ya, za, xb, yb, zb, id);
         this.decor.get(this.decor.size() - 1).soft = true;
      }

      boolean roomFree(int cx, int cz, int w) {
         for (int[] r : this.roomRects) {
            if (cx - w <= r[2] + 1 && cx + w >= r[0] - 1 && cz - w <= r[3] + 1 && cz + w >= r[1] - 1) {
               return false;
            }
         }

         return true;
      }

      void add(List<Box> layer, int xa, int ya, int za, int xb, int yb, int zb, int id) {
         Box b = new Box(xa, ya, za, xb, yb, zb, id);
         layer.add(b);
         this.minX = Math.min(this.minX, b.x0);
         this.maxX = Math.max(this.maxX, b.x1);
         this.minZ = Math.min(this.minZ, b.z0);
         this.maxZ = Math.max(this.maxZ, b.z1);
      }

      boolean inReach(int x, int z) {
         return Math.abs(x - this.ox) <= REACH && Math.abs(z - this.oz) <= REACH;
      }
   }

   /** The plan of the region that contains the block column (x, z). */
   Plan planAt(int x, int z) {
      return this.plan(Math.floorDiv(x, REG), Math.floorDiv(z, REG));
   }

   Plan plan(int rx, int rz) {
      long key = ((long)rx << 32) ^ (rz & 0xFFFFFFFFL);
      synchronized (this.plans) {
         Plan p = this.plans.get(key);
         if (p == null) {
            p = this.make(rx, rz);
            this.plans.put(key, p);
         }

         return p;
      }
   }

   private Plan make(int rx, int rz) {
      Random r = new Random(this.seed * 0x9E3779B97F4A7C15L ^ rx * 0xC2B2AE3D27D4EB4FL ^ rz * 0x165667B19E3779F9L ^ 0xF027F027L);
      if (r.nextInt(100) >= CHANCE) {
         return new Plan();
      }

      // a plan that has no throne room or no chest (the pieces ran into each other) is thrown away and drawn again
      Plan p = null;
      for (int attempt = 0; attempt < 12; attempt++) {
         p = new Plan();
         p.present = true;
         p.ox = rx * REG + REACH + 4 + r.nextInt(REG - 2 * REACH - 8);
         p.oz = rz * REG + REACH + 4 + r.nextInt(REG - 2 * REACH - 8);
         p.oy = 52 + r.nextInt(12);
         room(p, p.ox, p.oz, 3, 5, HALL, 0);
         int throneDir = r.nextInt(4);
         int closetDir = (throneDir + 1 + r.nextInt(3)) & 3;
         for (int d = 0; d < 4; d++) {
            chain(p, r, p.ox + D[d][0] * 5, p.oz + D[d][1] * 5, d, 0, d == throneDir ? THRONE : (d == closetDir ? CLOSET : -1));
         }

         if (p.hasThrone && !p.chests.isEmpty() && p.rooms >= 4) {
            break;
         }
      }

      return p;
   }

   /** A row of bridges and corridors starting at (x, z) - the first cell outside the wall of the room it leaves - and an end piece. */
   private static void chain(Plan p, Random r, int x, int z, int d, int depth, int forced) {
      int segs = (depth == 0 ? 3 : 2) + r.nextInt(2);
      int dx = D[d][0];
      int dz = D[d][1];
      int built = 0;
      for (int s = 0; s < segs; s++) {
         int len = 6 + r.nextInt(9);
         int ex = x + dx * (len - 1);
         int ez = z + dz * (len - 1);
         if (!p.inReach(ex + dx * 7, ez + dz * 7)) {
            break;
         }

         if (r.nextInt(100) < 62) {
            bridge(p, x, z, d, len);
         } else {
            corridor(p, x, z, d, len);
         }

         built++;
         x = ex + dx;
         z = ez + dz;
         if (s < segs - 1 && r.nextInt(100) < 50) {
            // a bend: a small closed room, the way on leaves it to the left or to the right
            int cx = x + dx * 3;
            int cz = z + dz * 3;
            int nd = (d + (r.nextBoolean() ? 1 : 3)) & 3;
            if (!p.inReach(cx + dx * 3 + D[nd][0] * 8, cz + dz * 3 + D[nd][1] * 8) || !p.roomFree(cx, cz, 3)) {
               break;
            }

            room(p, cx, cz, 2, 3, CORNER, d);
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
            kind = roll < 55 ? HALL : (roll < 78 ? CLOSET : (roll < 88 && !p.hasThrone ? THRONE : NONE));
         } else if (depth == 1) {
            kind = roll < 25 ? HALL : (roll < 60 ? CLOSET : (roll < 68 && !p.hasThrone ? THRONE : NONE));
         } else {
            kind = roll < 40 ? CLOSET : (roll < 48 && !p.hasThrone ? THRONE : NONE);
         }
      }

      if (kind == NONE) {
         return;
      }

      int half = kind == CLOSET ? 2 : 3;
      int cx = x + dx * (half + 1);
      int cz = z + dz * (half + 1);
      if (!p.inReach(cx + dx * (half + 1), cz + dz * (half + 1)) || !p.inReach(cx - (half + 1), cz - (half + 1)) || !p.inReach(cx + half + 1, cz + half + 1)
         || !p.roomFree(cx, cz, half + 1)) {
         return;
      }

      room(p, cx, cz, half, kind == CLOSET ? 3 : 5, kind, d);
      if (kind == HALL) {
         for (int nd = 0; nd < 4; nd++) {
            if (nd == ((d + 2) & 3) || r.nextInt(100) >= 75) {
               continue;
            }

            chain(p, r, cx + D[nd][0] * (half + 2), cz + D[nd][1] * (half + 2), nd, depth + 1, -1);
         }
      }
   }

   /** x range / z range of `len` cells from (x, z) along direction d, `half` cells to each side. */
   private static int[] rect(int x, int z, int d, int len, int half) {
      int dx = D[d][0];
      int dz = D[d][1];
      int ex = x + dx * (len - 1);
      int ez = z + dz * (len - 1);
      return new int[]{Math.min(x, ex) - half * Math.abs(dz), Math.min(z, ez) - half * Math.abs(dx), Math.max(x, ex) + half * Math.abs(dz), Math.max(z, ez) + half * Math.abs(dx)};
   }

   /** An open walkway of 5 blocks with a railing on both sides, standing on pillars. */
   private static void bridge(Plan p, int x, int z, int d, int len) {
      int oy = p.oy;
      int dx = D[d][0];
      int dz = D[d][1];
      int[] f = rect(x, z, d, len, 2);
      p.add(p.shell, f[0], oy, f[1], f[2], oy, f[3], Blocks.NETHER_BRICKS);
      int[] m = rect(x, z, d, len, 1);
      p.add(p.shell, m[0], oy - 1, m[1], m[2], oy - 1, m[3], Blocks.NETHER_BRICKS);
      int[] a2 = rect(x - dx, z - dz, d, len + 2, 2);
      p.add(p.air, a2[0], oy + 2, a2[1], a2[2], oy + 5, a2[3], 0);
      int[] a1 = rect(x - dx, z - dz, d, len + 2, 1);
      p.add(p.air, a1[0], oy + 1, a1[1], a1[2], oy + 1, a1[3], 0);
      for (int s = -1; s <= 1; s += 2) {
         int px = -dz * 2 * s;
         int pz = dx * 2 * s;
         int[] rail = rect(x + px, z + pz, d, len, 0);
         p.soft(rail[0], oy + 1, rail[1], rail[2], oy + 1, rail[3], Blocks.NETHER_BRICK_FENCE);
         for (int k = 2; k < len; k += 6) {
            p.pillars.add(new int[]{x + dx * k + px, oy - 1, z + dz * k + pz});
         }
      }
   }

   /** A covered corridor: 3 wide and 3 high inside, windows with fences in the walls and a lamp in the roof now and then. */
   private static void corridor(Plan p, int x, int z, int d, int len) {
      int oy = p.oy;
      int dx = D[d][0];
      int dz = D[d][1];
      int[] s = rect(x, z, d, len, 2);
      p.add(p.shell, s[0], oy, s[1], s[2], oy + 4, s[3], Blocks.NETHER_BRICKS);
      int[] a = rect(x - dx, z - dz, d, len + 2, 1);
      p.add(p.air, a[0], oy + 1, a[1], a[2], oy + 3, a[3], 0);
      for (int sign = -1; sign <= 1; sign += 2) {
         int px = -dz * 2 * sign;
         int pz = dx * 2 * sign;
         for (int k = 1; k < len - 1; k += 3) {
            int wx = x + dx * k + px;
            int wz = z + dz * k + pz;
            p.soft(wx, oy + 2, wz, wx, oy + 3, wz, Blocks.NETHER_BRICK_FENCE);
         }

         for (int k = 0; k < len; k += 6) {
            p.pillars.add(new int[]{x + dx * k + px, oy - 1, z + dz * k + pz});
         }
      }

      for (int k = 3; k < len; k += 8) {
         p.soft(x + dx * k, oy + 4, z + dz * k, x + dx * k, oy + 4, z + dz * k, Blocks.GLOWSTONE);
      }
   }

   /** A closed room: inner half-size `half`, inner height `h`, centred on (cx, cz). `d` is the direction the player enters it from. */
   private static void room(Plan p, int cx, int cz, int half, int h, int kind, int d) {
      int oy = p.oy;
      int w = half + 1;
      p.rooms++;
      p.roomRects.add(new int[]{cx - w, cz - w, cx + w, cz + w});
      p.add(p.shell, cx - w, oy, cz - w, cx + w, oy + h + 1, cz + w, Blocks.NETHER_BRICKS);
      p.add(p.air, cx - half, oy + 1, cz - half, cx + half, oy + h, cz + half, 0);
      for (int sx = -1; sx <= 1; sx++) {
         for (int sz = -1; sz <= 1; sz++) {
            p.pillars.add(new int[]{cx + sx * half, oy - 1, cz + sz * half});
         }
      }

      if (kind == HALL) {
         for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
               p.add(p.decor, cx + sx * 2, oy + 1, cz + sz * 2, cx + sx * 2, oy + h, cz + sz * 2, Blocks.NETHER_BRICKS);
            }
         }

         p.soft(cx, oy + h + 1, cz, cx, oy + h + 1, cz, Blocks.GLOWSTONE);
      } else if (kind == THRONE) {
         // the blaze spawner on a platform behind a low fence, lamps in the corners of the roof
         p.hasThrone = true;
         p.add(p.decor, cx - 1, oy + 1, cz - 1, cx + 1, oy + 1, cz + 1, Blocks.NETHER_BRICKS);
         p.add(p.decor, cx - 1, oy + 2, cz - 1, cx + 1, oy + 2, cz - 1, Blocks.NETHER_BRICK_FENCE);
         p.add(p.decor, cx - 1, oy + 2, cz + 1, cx + 1, oy + 2, cz + 1, Blocks.NETHER_BRICK_FENCE);
         p.add(p.decor, cx - 1, oy + 2, cz, cx - 1, oy + 2, cz, Blocks.NETHER_BRICK_FENCE);
         p.add(p.decor, cx + 1, oy + 2, cz, cx + 1, oy + 2, cz, Blocks.NETHER_BRICK_FENCE);
         p.add(p.decor, cx, oy + 2, cz, cx, oy + 2, cz, Blocks.SPAWNER);
         p.spawners.add(new int[]{cx, oy + 2, cz});
         for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
               p.soft(cx + sx * 3, oy + h + 1, cz + sz * 3, cx + sx * 3, oy + h + 1, cz + sz * 3, Blocks.GLOWSTONE);
            }
         }
      } else if (kind == CORNER) {
         p.soft(cx, oy + h + 1, cz, cx, oy + h + 1, cz, Blocks.GLOWSTONE);
      } else if (kind == CLOSET) {
         // one chest against the far wall, a second one in a corner
         int dx = D[d][0];
         int dz = D[d][1];
         int chestX = cx + dx * half;
         int chestZ = cz + dz * half;
         p.add(p.decor, chestX, oy + 1, chestZ, chestX, oy + 1, chestZ, Blocks.CHEST_LOOT);
         p.chests.add(new int[]{chestX, oy + 1, chestZ});
         int sideX = chestX - dz * half;
         int sideZ = chestZ + dx * half;
         p.add(p.decor, sideX, oy + 1, sideZ, sideX, oy + 1, sideZ, Blocks.CHEST_LOOT);
         p.chests.add(new int[]{sideX, oy + 1, sideZ});
         p.soft(cx, oy + h + 1, cz, cx, oy + h + 1, cz, Blocks.GLOWSTONE);
      }
   }

   /** Builds the part of the fortress that lies in this chunk. */
   void apply(Chunk c) {
      int x0 = c.cx * 16;
      int z0 = c.cz * 16;
      Plan p = this.planAt(x0 + 8, z0 + 8);
      if (!p.present || p.maxX < x0 || p.minX > x0 + 15 || p.maxZ < z0 || p.minZ > z0 + 15) {
         return;
      }

      boolean[] carved = new boolean[16 * 128 * 16];
      for (Box b : p.shell) {
         fill(c, b, null, false);
      }

      for (int[] q : p.pillars) {
         pillar(c, q[0], q[1], q[2]);
      }

      for (Box b : p.air) {
         fill(c, b, carved, false);
      }

      for (Box b : p.decor) {
         fill(c, b, carved, b.soft);
      }
   }

   /** Fills the part of the box that lies in the chunk; air boxes mark what they cut out, soft boxes leave those cells alone. */
   private static void fill(Chunk c, Box b, boolean[] carved, boolean soft) {
      int x0 = c.cx * 16;
      int z0 = c.cz * 16;
      int xa = Math.max(b.x0, x0);
      int xb = Math.min(b.x1, x0 + 15);
      int za = Math.max(b.z0, z0);
      int zb = Math.min(b.z1, z0 + 15);
      if (xa > xb || za > zb) {
         return;
      }

      for (int z = za; z <= zb; z++) {
         for (int x = xa; x <= xb; x++) {
            for (int y = Math.max(1, b.y0); y <= Math.min(NetherGen.TOP - 1, b.y1); y++) {
               int i = Chunk.idx(x - x0, y, z - z0);
               if (soft && carved[i]) {
                  continue;
               }

               c.blocks[i] = b.id;
               if (b.id == 0 && carved != null) {
                  carved[i] = true;
               }
            }
         }
      }
   }

   private static void pillar(Chunk c, int x, int top, int z) {
      int lx = x - c.cx * 16;
      int lz = z - c.cz * 16;
      if (lx < 0 || lx > 15 || lz < 0 || lz > 15) {
         return;
      }

      for (int y = top; y >= 1; y--) {
         int id = c.blocks[Chunk.idx(lx, y, lz)];
         if (id != 0 && !Blocks.isLava(id)) {
            break;
         }

         c.blocks[Chunk.idx(lx, y, lz)] = (short)Blocks.NETHER_BRICKS;
      }
   }
}
