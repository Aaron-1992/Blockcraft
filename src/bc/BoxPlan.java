package bc;

import java.util.ArrayList;
import java.util.List;

/**
 * Update 3.12: a structure that is a list of boxes in three layers - walls and floors first, then the air that is cut out of them (so
 * passages between pieces that cross always stay open), then the details (railings, windows, lamps, spawners, chests) - plus pillars that
 * grow down from the floor until they find rock. Used by the nether fortresses and the strongholds. A chunk builds its own part of a plan,
 * so the plan must be a function of the seed alone.
 */
class BoxPlan {
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

   int minX = Integer.MAX_VALUE;
   int maxX = Integer.MIN_VALUE;
   int minZ = Integer.MAX_VALUE;
   int maxZ = Integer.MIN_VALUE;
   final List<Box> shell = new ArrayList<>();
   final List<Box> air = new ArrayList<>();
   final List<Box> decor = new ArrayList<>();
   /** columns that grow downwards from the floor until they find rock: x, y of the topmost block, z */
   final List<int[]> pillars = new ArrayList<>();

   void add(List<Box> layer, int xa, int ya, int za, int xb, int yb, int zb, int id) {
      Box b = new Box(xa, ya, za, xb, yb, zb, id);
      layer.add(b);
      this.minX = Math.min(this.minX, b.x0);
      this.maxX = Math.max(this.maxX, b.x1);
      this.minZ = Math.min(this.minZ, b.z0);
      this.maxZ = Math.max(this.maxZ, b.z1);
   }

   void soft(int xa, int ya, int za, int xb, int yb, int zb, int id) {
      this.add(this.decor, xa, ya, za, xb, yb, zb, id);
      this.decor.get(this.decor.size() - 1).soft = true;
   }

   boolean touches(Chunk c) {
      int x0 = c.cx * 16;
      int z0 = c.cz * 16;
      return this.maxX >= x0 && this.minX <= x0 + 15 && this.maxZ >= z0 && this.minZ <= z0 + 15;
   }

   /** Builds the part of the plan that lies in this chunk; pillars are made of {@code pillarId} and stop at anything that is not air or lava. */
   void apply(Chunk c, int pillarId, int topY) {
      if (!this.touches(c)) {
         return;
      }

      boolean[] carved = new boolean[16 * 256 * 16];
      for (Box b : this.shell) {
         fill(c, b, null, false, topY);
      }

      for (int[] q : this.pillars) {
         pillar(c, q[0], q[1], q[2], pillarId);
      }

      for (Box b : this.air) {
         fill(c, b, carved, false, topY);
      }

      for (Box b : this.decor) {
         fill(c, b, carved, b.soft, topY);
      }
   }

   /** Fills the part of the box that lies in the chunk; air boxes mark what they cut out, soft boxes leave those cells alone. */
   static void fill(Chunk c, Box b, boolean[] carved, boolean soft, int topY) {
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
            for (int y = Math.max(1, b.y0); y <= Math.min(topY, b.y1); y++) {
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

   static void pillar(Chunk c, int x, int top, int z, int pillarId) {
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

         c.blocks[Chunk.idx(lx, y, lz)] = (short)pillarId;
      }
   }
}
