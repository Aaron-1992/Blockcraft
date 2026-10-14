package bc;

import java.util.Random;

/**
 * Update 3.13: the trees, giant mushrooms, ice spikes and bushes of the new generator. A plant is first drawn into a small buffer around its
 * root ({@link Buf}) and then copied into the chunk that is being generated, clipped to it: a tree that grows over a chunk border comes
 * out the same in both chunks, whatever order they are made in.
 */
final class TerraTrees {
   private TerraTrees() {
   }

   // species: 0 oak, 1 birch, 2 spruce, 3 jungle, 4 acacia, 5 dark oak
   static int log(int sp) {
      switch (sp) {
         case 0:
            return Blocks.LOG_OAK;
         case 1:
            return Blocks.LOG_BIRCH;
         case 2:
            return Blocks.LOG_SPRUCE;
         case 3:
            return Blocks313.LOG_JUNGLE;
         case 4:
            return Blocks313.LOG_ACACIA;
         default:
            return Blocks313.LOG_DARKOAK;
      }
   }

   static int leaves(int sp) {
      switch (sp) {
         case 0:
            return Blocks.LEAVES_OAK;
         case 1:
            return Blocks.LEAVES_BIRCH;
         case 2:
            return Blocks.LEAVES_SPRUCE;
         case 3:
            return Blocks313.LEAVES_JUNGLE;
         case 4:
            return Blocks313.LEAVES_ACACIA;
         default:
            return Blocks313.LEAVES_DARKOAK;
      }
   }

   /** The picture of a plant while it is being built: ids relative to the root (dx, dy, dz), copied into a chunk by {@link #commit}. */
   static final class Buf {
      static final int R = 14;
      static final int H = 60;
      static final int W = 2 * R + 1;
      static final int SOFT = 0x40000000;
      final int[] a = new int[W * H * W];
      final int[] touched = new int[W * H * W];
      int nt;
      int ox;
      int oy;
      int oz;

      void begin(int x, int y, int z) {
         this.clear();
         this.ox = x;
         this.oy = y;
         this.oz = z;
      }

      void clear() {
         for (int i = 0; i < this.nt; i++) {
            this.a[this.touched[i]] = 0;
         }

         this.nt = 0;
      }

      private int index(int dx, int dy, int dz) {
         if (dx < -R || dx > R || dz < -R || dz > R || dy < 0 || dy >= H) {
            return -1;
         }

         return (dy * W + dz + R) * W + dx + R;
      }

      /** Hard: replaces whatever is there (trunks, ice, mushroom stems). */
      void set(int dx, int dy, int dz, int id) {
         int i = this.index(dx, dy, dz);
         if (i >= 0) {
            if (this.a[i] == 0) {
               this.touched[this.nt++] = i;
            }

            this.a[i] = id + 1;
         }
      }

      /** Soft: only where nothing else of the plant is, and in the chunk only where there is air, a plant or a leaf (crowns). */
      void leaf(int dx, int dy, int dz, int id) {
         int i = this.index(dx, dy, dz);
         if (i >= 0 && this.a[i] == 0) {
            this.touched[this.nt++] = i;
            this.a[i] = id + 1 | SOFT;
         }
      }

      /** A vine: soft (only into air), and it may be written again to add a wall to it. */
      void vine(int dx, int dy, int dz, int id) {
         int i = this.index(dx, dy, dz);
         if (i >= 0) {
            if (this.a[i] == 0) {
               this.touched[this.nt++] = i;
            }

            this.a[i] = id + 1 | SOFT;
         }
      }

      int get(int dx, int dy, int dz) {
         int i = this.index(dx, dy, dz);
         return i < 0 || this.a[i] == 0 ? 0 : (this.a[i] & ~SOFT) - 1;
      }

      void commit(Chunk c) {
         for (int k = 0; k < this.nt; k++) {
            int i = this.touched[k];
            int v = this.a[i];
            int id = (v & ~SOFT) - 1;
            int dx = i % W - R;
            int dz = i / W % W - R;
            int dy = i / (W * W);
            int x = this.ox + dx - c.cx * 16;
            int z = this.oz + dz - c.cz * 16;
            int y = this.oy + dy;
            if (x < 0 || x > 15 || z < 0 || z > 15 || y < 1 || y > 254) {
               continue;
            }

            int at = Chunk.idx(x, y, z);
            int old = c.blocks[at] & BlockState.ID_MASK;
            if ((v & SOFT) != 0) {
               boolean vine = id >= Blocks313.VINE0 + 1 && id <= Blocks313.VINE0 + 15;
               if (old == 0 || !vine && softBlock(old)) {
                  c.blocks[at] = (short)id;
               }
            } else if (!Blocks.isFluid(old) && old != Blocks.BEDROCK) {
               c.blocks[at] = (short)id;
            }
         }
      }
   }

   /** What a crown may grow through: air, grass and flowers, other leaves, snow cover. */
   static boolean softBlock(int id) {
      return id == 0 || Blocks.isLeaves(id) || Blocks.kind[id] == Blocks.K_CROSS || id == Blocks313.SNOW_LAYER;
   }

   // ---------------------------------------------------------------------------------------------------- pieces
   static void blob(Buf t, int cx, int cy, int cz, double rx, double ry, double rz, int id, Random r) {
      int ix = (int)Math.ceil(rx);
      int iy = (int)Math.ceil(ry);
      int iz = (int)Math.ceil(rz);
      for (int dy = -iy; dy <= iy; dy++) {
         for (int dx = -ix; dx <= ix; dx++) {
            for (int dz = -iz; dz <= iz; dz++) {
               double d = dx * dx / (rx * rx) + dy * dy / (ry * ry) + dz * dz / (rz * rz);
               if (!(d > 1.0) && (!(d > 0.62) || !(r.nextFloat() < 0.38F))) {
                  t.leaf(cx + dx, cy + dy, cz + dz, id);
               }
            }
         }
      }
   }

   static void disc(Buf t, int cy, double cx, double cz, double rad, int id, Random r, float skip) {
      int ir = (int)Math.ceil(rad + 1);
      for (int dx = -ir; dx <= ir + 1; dx++) {
         for (int dz = -ir; dz <= ir + 1; dz++) {
            double d = Math.sqrt((dx - cx) * (dx - cx) + (dz - cz) * (dz - cz));
            if (d <= rad && (d < rad - 1.0 || r.nextFloat() >= skip)) {
               t.leaf(dx, cy, dz, id);
            }
         }
      }
   }

   static void trunk(Buf t, int x, int z, int y0, int y1, int id) {
      for (int y = y0; y <= y1; y++) {
         t.set(x, y, z, id);
      }
   }

   static void trunk2(Buf t, int y0, int y1, int id) {
      for (int y = y0; y <= y1; y++) {
         t.set(0, y, 0, id);
         t.set(1, y, 0, id);
         t.set(0, y, 1, id);
         t.set(1, y, 1, id);
      }
   }

   static boolean isVine(int id) {
      return id > Blocks313.VINE0 && id <= Blocks313.VINE0 + 15;
   }

   /** Vines on the open sides of the leaves of the plant, hanging down a few blocks. */
   static void vines(Buf t, Random r, float chance, int maxLen) {
      int n = t.nt;
      int[] dxs = new int[]{0, 1, 0, -1};
      int[] dzs = new int[]{-1, 0, 1, 0};
      for (int k = 0; k < n; k++) {
         int i = t.touched[k];
         int id = (t.a[i] & ~Buf.SOFT) - 1;
         if (!Blocks.isLeaves(id)) {
            continue;
         }

         int dx = i % Buf.W - Buf.R;
         int dz = i / Buf.W % Buf.W - Buf.R;
         int dy = i / (Buf.W * Buf.W);
         for (int d = 0; d < 4; d++) {
            if (r.nextFloat() >= chance) {
               continue;
            }

            int len = 1 + r.nextInt(maxLen);
            // the vine hangs in the cell beside the leaf and clings to the leaf: its wall is the side opposite to the way we stepped
            int vx = dx + dxs[d];
            int vz = dz + dzs[d];
            int bit = 1 << (d + 2 & 3);
            int first = t.get(vx, dy, vz);
            if (first != 0 && !isVine(first)) {
               continue;
            }

            for (int s = 0; s < len; s++) {
               int y = dy - s;
               int cur = t.get(vx, y, vz);
               if (cur != 0 && !isVine(cur)) {
                  break;
               }

               int mask = isVine(cur) ? cur - Blocks313.VINE0 : 0;
               t.vine(vx, y, vz, Blocks313.VINE0 + (mask | bit));
            }
         }
      }
   }

   // ---------------------------------------------------------------------------------------------------- the trees
   /** The oak of the old generator: round crown with a few side lobes (kind 0), the birch (1) and the spruce (2). size 0..2. */
   static void classic(Buf t, Random r, int sp, int size) {
      int lg = log(sp);
      int lf = leaves(sp);
      if (sp == 2) {
         int h = 7 + size * 2 + r.nextInt(3);
         int top = h;
         int maxR = h >= 10 ? 4 : 3;
         for (int y = 2; y <= top + 1; y++) {
            int f = top + 1 - y;
            int rad = Math.min(maxR, (f + 1) / 2);
            if (f % 2 == 0 && f > 1) {
               rad--;
            }

            for (int dx = -rad; dx <= rad; dx++) {
               for (int dz = -rad; dz <= rad; dz++) {
                  boolean corner = Math.abs(dx) == rad && Math.abs(dz) == rad && rad > 1;
                  if (!corner && (rad < 3 || Math.abs(dx) + Math.abs(dz) < 2 * rad - 1 || r.nextInt(3) != 0)) {
                     t.leaf(dx, y, dz, lf);
                  }
               }
            }
         }

         trunk(t, 0, 0, 0, h, lg);
      } else if (sp == 1) {
         int h = 6 + size + r.nextInt(2);
         int cy = h - 1;
         blob(t, 0, cy, 0, 2.3, 3.0, 2.3, lf, r);
         blob(t, 0, cy + 2, 0, 1.4, 1.6, 1.4, lf, r);
         trunk(t, 0, 0, 0, h, lg);
      } else {
         boolean big = r.nextInt(7) == 0;
         int h = 5 + size + r.nextInt(2) + (big ? 3 : 0);
         int cy = h - 1;
         double rr = big ? 4.2 : 3.1;
         double ry = big ? 3.2 : 2.5;
         blob(t, 0, cy, 0, rr, ry, rr, lf, r);
         int lobes = 2 + r.nextInt(3);
         for (int i = 0; i < lobes; i++) {
            double a = r.nextDouble() * Math.PI * 2.0;
            double dist = big ? 3.6 : 2.7;
            int lx = (int)Math.round(Math.cos(a) * dist);
            int lz = (int)Math.round(Math.sin(a) * dist);
            int ly = cy - r.nextInt(3);
            blob(t, lx, ly, lz, big ? 2.6 : 2.1, big ? 2.1 : 1.8, big ? 2.6 : 2.1, lf, r);
            int px = 0;
            int pz = 0;
            int py = ly - 1;
            int sx = Integer.signum(lx);
            int sz = Integer.signum(lz);
            for (int s = 0; s < 3; s++) {
               if (Math.abs(lx - px) >= Math.abs(lz - pz)) {
                  px += sx;
               } else {
                  pz += sz;
               }

               if (px == lx && pz == lz) {
                  break;
               }

               t.set(px, py, pz, lg);
            }
         }

         blob(t, 0, cy + (big ? 3 : 2), 0, big ? 2.4 : 1.7, 1.4, big ? 2.4 : 1.7, lf, r);
         trunk(t, 0, 0, 0, h, lg);
         if (big) {
            for (int y = 0; y < 2; y++) {
               t.set(1, y, 0, lg);
               t.set(0, y, 1, lg);
            }
         }
      }
   }

   /** A small, quick oak (the sort that stands alone in the plains). */
   static void smallOak(Buf t, Random r, int sp) {
      int lg = log(sp);
      int lf = leaves(sp);
      int h = 4 + r.nextInt(3);
      for (int y = h - 2; y <= h + 1; y++) {
         int rad = y <= h - 1 ? 2 : 1;
         for (int dx = -rad; dx <= rad; dx++) {
            for (int dz = -rad; dz <= rad; dz++) {
               if (Math.abs(dx) == rad && Math.abs(dz) == rad && (y >= h || r.nextInt(2) == 0)) {
                  continue;
               }

               t.leaf(dx, y, dz, lf);
            }
         }
      }

      trunk(t, 0, 0, 0, h, lg);
   }

   /** The tall pale birch (one block thin, narrow crown). */
   static void tallBirch(Buf t, Random r) {
      int h = 9 + r.nextInt(4);
      int lf = Blocks.LEAVES_BIRCH;
      for (int y = h - 4; y <= h + 1; y++) {
         int rad = y >= h ? 1 : (y >= h - 1 ? 2 : 2);
         if (y == h + 1) {
            rad = 1;
         }

         for (int dx = -rad; dx <= rad; dx++) {
            for (int dz = -rad; dz <= rad; dz++) {
               if (Math.abs(dx) == rad && Math.abs(dz) == rad && (rad == 1 || r.nextInt(3) == 0)) {
                  continue;
               }

               t.leaf(dx, y, dz, lf);
            }
         }
      }

      trunk(t, 0, 0, 0, h, Blocks.LOG_BIRCH);
   }

   /** The old, wide oak of the swamp: low and spread, with vines hanging all round. */
   static void swampOak(Buf t, Random r) {
      int h = 5 + r.nextInt(3);
      int lf = Blocks.LEAVES_OAK;
      for (int y = h - 2; y <= h + 1; y++) {
         int rad = y <= h - 1 ? 3 : 2;
         for (int dx = -rad; dx <= rad; dx++) {
            for (int dz = -rad; dz <= rad; dz++) {
               if (Math.abs(dx) == rad && Math.abs(dz) == rad && (y >= h || r.nextInt(2) == 0)) {
                  continue;
               }

               t.leaf(dx, y, dz, lf);
            }
         }
      }

      trunk(t, 0, 0, 0, h, Blocks.LOG_OAK);
      vines(t, r, 0.28F, 4);
   }

   /** The 2 x 2 spruce of the giant taiga. */
   static void megaSpruce(Buf t, Random r) {
      int h = 14 + r.nextInt(12);
      int lf = Blocks.LEAVES_SPRUCE;
      int maxR = 5 + (h > 20 ? 1 : 0);
      for (int y = h + 2; y >= 4; y--) {
         int f = h + 2 - y;
         double rad = Math.min(maxR, 0.5 + f * 0.42);
         if (f % 4 == 3) {
            rad = Math.max(1.0, rad - 1.6);
         }

         if (y == h + 2) {
            rad = 0.8;
         }

         disc(t, y, 0.5, 0.5, rad, lf, r, 0.35F);
      }

      trunk2(t, 0, h + 1, Blocks.LOG_SPRUCE);
   }

   /** The 2 x 2 dark oak: a short thick trunk under a broad flat crown. */
   static void darkOak(Buf t, Random r) {
      int h = 6 + r.nextInt(3);
      int lf = Blocks313.LEAVES_DARKOAK;
      disc(t, h - 1, 0.5, 0.5, 3.2, lf, r, 0.5F);
      disc(t, h, 0.5, 0.5, 4.3, lf, r, 0.35F);
      disc(t, h + 1, 0.5, 0.5, 4.0, lf, r, 0.3F);
      disc(t, h + 2, 0.5, 0.5, 2.6, lf, r, 0.3F);
      // branches: a few blocks of log going out and a puff of leaves at their ends
      for (int i = 0; i < 3; i++) {
         double a = r.nextDouble() * Math.PI * 2.0;
         int bx = (int)Math.round(0.5 + Math.cos(a) * 3.0);
         int bz = (int)Math.round(0.5 + Math.sin(a) * 3.0);
         blob(t, bx, h - 1 - r.nextInt(2), bz, 2.0, 1.6, 2.0, lf, r);
      }

      trunk2(t, 0, h, Blocks313.LOG_DARKOAK);
      t.set(-Integer.signum(r.nextInt(3) - 1), h, 0, Blocks313.LOG_DARKOAK);
   }

   /** A small jungle tree with vines. */
   static void jungleSmall(Buf t, Random r) {
      int h = 5 + r.nextInt(5);
      int lf = Blocks313.LEAVES_JUNGLE;
      for (int y = h - 2; y <= h + 1; y++) {
         int rad = y <= h - 1 ? 2 : 1;
         for (int dx = -rad; dx <= rad; dx++) {
            for (int dz = -rad; dz <= rad; dz++) {
               if (Math.abs(dx) == rad && Math.abs(dz) == rad && (y >= h || r.nextInt(2) == 0)) {
                  continue;
               }

               t.leaf(dx, y, dz, lf);
            }
         }
      }

      trunk(t, 0, 0, 0, h, Blocks313.LOG_JUNGLE);
      vines(t, r, 0.3F, 3);
   }

   /** A tall jungle tree: a long bare trunk and a layered crown at the top. */
   static void jungleTall(Buf t, Random r) {
      int h = 11 + r.nextInt(9);
      int lf = Blocks313.LEAVES_JUNGLE;
      disc(t, h - 2, 0, 0, 3.4, lf, r, 0.4F);
      disc(t, h - 1, 0, 0, 4.2, lf, r, 0.35F);
      disc(t, h, 0, 0, 3.4, lf, r, 0.3F);
      disc(t, h + 1, 0, 0, 2.0, lf, r, 0.2F);
      // a side branch with its own tuft
      double a = r.nextDouble() * Math.PI * 2.0;
      int bx = (int)Math.round(Math.cos(a) * 3.0);
      int bz = (int)Math.round(Math.sin(a) * 3.0);
      int by = h - 5 - r.nextInt(3);
      blob(t, bx, by + 1, bz, 2.4, 1.6, 2.4, lf, r);
      for (int s = 1; s <= 3; s++) {
         t.set(bx * s / 3, by + (s > 1 ? 1 : 0), bz * s / 3, Blocks313.LOG_JUNGLE);
      }

      trunk(t, 0, 0, 0, h, Blocks313.LOG_JUNGLE);
      // a few vines up the trunk
      for (int d = 0; d < 4; d++) {
         if (r.nextInt(2) == 0) {
            int vx = new int[]{0, 1, 0, -1}[d];
            int vz = new int[]{-1, 0, 1, 0}[d];
            int bit = 1 << (d + 2 & 3);
            for (int y = 2; y < h - 1; y++) {
               t.leaf(vx, y, vz, Blocks313.VINE0 + bit);
            }
         }
      }

      vines(t, r, 0.22F, 5);
   }

   /** The giant jungle tree: 2 x 2 trunk, wide crown. */
   static void megaJungle(Buf t, Random r) {
      int h = 20 + r.nextInt(12);
      int lf = Blocks313.LEAVES_JUNGLE;
      blob(t, 0, h, 0, 6.2, 2.6, 6.2, lf, r);
      blob(t, 0, h + 2, 0, 3.6, 1.8, 3.6, lf, r);
      for (int i = 0; i < 3; i++) {
         double a = r.nextDouble() * Math.PI * 2.0;
         int bx = (int)Math.round(Math.cos(a) * 5.0);
         int bz = (int)Math.round(Math.sin(a) * 5.0);
         int by = h - 6 - r.nextInt(6);
         blob(t, bx, by + 1, bz, 3.2, 1.8, 3.2, lf, r);
         for (int s = 1; s <= 4; s++) {
            t.set(bx * s / 4, by + (s > 2 ? 1 : 0) - (s < 2 ? 1 : 0), bz * s / 4, Blocks313.LOG_JUNGLE);
         }
      }

      trunk2(t, 0, h, Blocks313.LOG_JUNGLE);
      vines(t, r, 0.3F, 6);
   }

   /** A jungle bush: one log with a ball of leaves. */
   static void bush(Buf t, Random r, int sp) {
      t.set(0, 0, 0, log(sp));
      blob(t, 0, 1, 0, 2.2, 1.5, 2.2, leaves(sp), r);
      t.leaf(0, 2, 0, leaves(sp));
   }

   /** The flat-topped acacia: a trunk that bends once, an umbrella of leaves. */
   static void acacia(Buf t, Random r) {
      int lg = Blocks313.LOG_ACACIA;
      int lf = Blocks313.LEAVES_ACACIA;
      int h = 5 + r.nextInt(3);
      int dir = r.nextInt(4);
      int dx = new int[]{0, 1, 0, -1}[dir];
      int dz = new int[]{-1, 0, 1, 0}[dir];
      int bend = h - 2 - r.nextInt(2);
      int x = 0;
      int z = 0;
      int y = 0;
      for (; y <= bend; y++) {
         t.set(x, y, z, lg);
      }

      int steps = h - bend + 1;
      for (int s = 0; s < steps; s++) {
         x += dx;
         z += dz;
         t.set(x, y, z, lg);
         if (s % 2 == 1) {
            y++;
         }
      }

      int tx = x;
      int tz = z;
      int ty = y;
      disc3(t, tx, ty + 1, tz, 3, lf, r);
      disc3(t, tx, ty + 2, tz, 2, lf, r);
      if (r.nextInt(3) != 0) {
         // a second, lower arm in the other direction
         int ax = -dx;
         int az = -dz;
         int py = bend - 1;
         int qx = 0;
         int qz = 0;
         for (int s = 0; s < 3; s++) {
            qx += ax;
            qz += az;
            t.set(qx, py, qz, lg);
            if (s % 2 == 0) {
               py++;
            }
         }

         disc3(t, qx, py + 1, qz, 2, lf, r);
      }
   }

   private static void disc3(Buf t, int cx, int cy, int cz, int rad, int id, Random r) {
      for (int dx = -rad; dx <= rad; dx++) {
         for (int dz = -rad; dz <= rad; dz++) {
            if (Math.abs(dx) == rad && Math.abs(dz) == rad && (rad > 2 || r.nextInt(2) == 0)) {
               continue;
            }

            t.leaf(cx + dx, cy, cz + dz, id);
         }
      }
   }

   /** Red (kind 0) or brown (1) giant mushroom. */
   static void giantMushroom(Buf t, Random r, int kind) {
      int h = 4 + r.nextInt(3);
      int cap = kind == 0 ? Blocks313.MUSH_BLOCK_RED : Blocks313.MUSH_BLOCK_BROWN;
      for (int y = 0; y < h; y++) {
         t.set(0, y, 0, Blocks313.MUSH_STEM);
      }

      if (kind == 0) {
         // a dome: flat top, sides coming down 3 blocks
         for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
               if (Math.abs(dx) == 2 && Math.abs(dz) == 2) {
                  continue;
               }

               t.set(dx, h, dz, cap);
            }
         }

         for (int dy = 1; dy <= 3; dy++) {
            for (int dx = -3; dx <= 3; dx++) {
               for (int dz = -3; dz <= 3; dz++) {
                  int m = Math.max(Math.abs(dx), Math.abs(dz));
                  boolean ring = m == 3 && Math.abs(dx) + Math.abs(dz) <= 4;
                  if (ring && Math.abs(dx) + Math.abs(dz) < 6) {
                     t.set(dx, h - dy + 1, dz, cap);
                  }
               }
            }
         }
      } else {
         // a wide flat plate
         for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
               if (Math.abs(dx) + Math.abs(dz) > 4 || Math.abs(dx) == 3 && Math.abs(dz) == 3) {
                  continue;
               }

               t.set(dx, h, dz, cap);
            }
         }
      }
   }

   /** A spike of packed ice rising from the snow (height 8..24). */
   static void iceSpike(Buf t, Random r) {
      int height = 8 + r.nextInt(18);
      int base = 1 + r.nextInt(3);
      boolean tall = r.nextInt(4) == 0;
      if (tall) {
         height += 10;
         base++;
      }

      for (int y = -2; y < height; y++) {
         double f = y <= 0 ? 1.0 : 1.0 - (double)y / height;
         double rad = base * f + (f > 0.3 ? 0.4 : 0.0);
         int ir = (int)Math.ceil(rad);
         for (int dx = -ir; dx <= ir; dx++) {
            for (int dz = -ir; dz <= ir; dz++) {
               if (Math.sqrt(dx * dx + dz * dz) <= rad + (y <= 0 ? 0.6 : 0.0)) {
                  t.set(dx, y + 2, dz, y % 5 == 4 ? Blocks313.ICE : Blocks313.PACKED_ICE);
               }
            }
         }
      }

      t.set(0, height + 2, 0, Blocks313.PACKED_ICE);
   }
}
