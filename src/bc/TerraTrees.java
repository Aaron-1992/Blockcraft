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
      /** Update 6.23: a little more room for giant crowns (still bounded, so RAM stays flat). */
      static final int R = 15;
      static final int H = 64;
      static final int W = 2 * R + 1;
      static final int SOFT = 0x40000000;
      /** A hard ceiling: no tree, however giant, can become a thousand-leaf mesh spike. */
      static final int MAX_LEAVES = 900;
      final int[] a = new int[W * H * W];
      final int[] touched = new int[W * H * W];
      int nt;
      int leafCount;
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
         this.leafCount = 0;
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
         if (i < 0 || this.a[i] != 0) return;
         // Leaves are the expensive part to mesh; vines and flowers still fit inside the budget.
         if (Blocks.isLeaves(id) && this.leafCount >= MAX_LEAVES) return;
         this.touched[this.nt++] = i;
         this.a[i] = id + 1 | SOFT;
         if (Blocks.isLeaves(id)) this.leafCount++;
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

   // ---------------------------------------------------------------------------------------------------- varieties
   /**
    * Update 6.23: every tree species has several silhouettes and three size tiers. The variant and
    * the size are chosen by TerraGen from the seed-derived tree hash, so a tree that crosses a chunk
    * border is rebuilt identically by every neighbouring chunk. All shapes share the same bounded
    * {@link Buf} and the same hard leaf budget, so "bigger" never becomes a mesh-time spike.
    */
   static final int SP_OAK = 0;
   static final int SP_BIRCH = 1;
   static final int SP_SPRUCE = 2;
   static final int SP_JUNGLE = 3;
   static final int SP_ACACIA = 4;
   static final int SP_DARK_OAK = 5;
   static final int SP_SWAMP = 6;
   static final int SP_BUSH = 7;
   /** Number of distinct silhouettes of each species. */
   static final int[] VARIANTS = {6, 5, 6, 5, 4, 3, 3, 2};
   static final String[] SPECIES_NAME = {"Oak", "Birch", "Spruce", "Jungle", "Acacia", "Dark Oak", "Swamp", "Bush"};
   static final String[] SIZE_NAME = {"Normal", "Large", "Giant"};
   static final double[] H_STEP = {1.0, 1.55, 2.05};
   static final double[] R_STEP = {1.0, 1.22, 1.44};

   static int variants(int sp) {
      return VARIANTS[sp >= 0 && sp < VARIANTS.length ? sp : 0];
   }

   /** 2x2 trunks need flat ground under all four columns. */
   static boolean isWide(int sp, int variant) {
      int v = Math.floorMod(variant, variants(sp));
      return sp == SP_SPRUCE && (v == 3 || v == 4) || sp == SP_JUNGLE && v == 2 || sp == SP_DARK_OAK;
   }

   static double heightScale(int size, float worldScale) {
      double s = H_STEP[size < 0 ? 0 : (size > 2 ? 2 : size)] * worldScale;
      return s < 0.60 ? 0.60 : (s > 2.10 ? 2.10 : s);
   }

   static double radiusScale(int size, float worldScale) {
      double s = R_STEP[size < 0 ? 0 : (size > 2 ? 2 : size)] * (0.80 + 0.20 * worldScale);
      return s < 0.65 ? 0.65 : (s > 1.62 ? 1.62 : s);
   }

   /** Scaled integer with a ceiling that keeps the plant inside {@link Buf}. */
   private static int sh(int base, double s, int max) {
      int v = (int)Math.round(base * s);
      return v < 2 ? 2 : (v > max ? max : v);
   }

   private static double sr(double base, double s, double max) {
      double v = base * s;
      return v > max ? max : v;
   }

   static void build(Buf t, Random r, int sp, int variant, int size, float worldScale) {
      int v = Math.floorMod(variant, variants(sp));
      double hs = heightScale(size, worldScale);
      double rs = radiusScale(size, worldScale);
      switch (sp) {
         case SP_BIRCH:
            birchShape(t, r, v, hs, rs);
            break;
         case SP_SPRUCE:
            spruceShape(t, r, v, hs, rs, size);
            break;
         case SP_JUNGLE:
            jungleShape(t, r, v, hs, rs);
            break;
         case SP_ACACIA:
            acaciaShape(t, r, v, hs, rs);
            break;
         case SP_DARK_OAK:
            darkOakShape(t, r, v, hs, rs);
            break;
         case SP_SWAMP:
            swampShape(t, r, v, hs, rs);
            break;
         case SP_BUSH:
            bushShape(t, r, v, hs, rs);
            break;
         default:
            oakShape(t, r, v, hs, rs);
            break;
      }
   }

   static void build(Buf t, Random r, int sp, int variant, int size) {
      build(t, r, sp, variant, size, 1.0F);
   }

   // ------------------------------------------------------------------ oak: round, small, columnar, umbrella, forked, weeping
   private static void oakShape(Buf t, Random r, int v, double hs, double rs) {
      int lg = log(0);
      int lf = leaves(0);
      switch (v) {
         case 1: { // small squat oak
            int h = sh(4 + r.nextInt(3), hs, 44);
            double rad = sr(2.0, rs, 5.0);
            for (int y = h - 2; y <= h + 1; y++) {
               disc(t, y, 0, 0, y <= h - 1 ? rad : rad * 0.6, lf, r, 0.25F);
            }
            trunk(t, 0, 0, 0, h, lg);
            break;
         }
         case 2: { // columnar oak: tall bare trunk, compact high crown
            int h = sh(11 + r.nextInt(6), hs, 54);
            double rad = sr(2.6, rs, 5.2);
            disc(t, h - 2, 0, 0, rad * 0.8, lf, r, 0.28F);
            disc(t, h - 1, 0, 0, rad, lf, r, 0.24F);
            disc(t, h, 0, 0, rad, lf, r, 0.24F);
            disc(t, h + 1, 0, 0, rad * 0.72, lf, r, 0.2F);
            disc(t, h + 2, 0, 0, rad * 0.4, lf, r, 0.15F);
            trunk(t, 0, 0, 0, h, lg);
            break;
         }
         case 3: { // wide umbrella oak, short trunk and long arms
            int h = sh(5 + r.nextInt(3), hs, 44);
            double rad = sr(5.2, rs, 10.5);
            disc(t, h, 0, 0, rad, lf, r, 0.34F);
            disc(t, h + 1, 0, 0, rad * 0.92, lf, r, 0.32F);
            disc(t, h + 2, 0, 0, rad * 0.62, lf, r, 0.28F);
            disc(t, h - 1, 0, 0, rad * 0.72, lf, r, 0.42F);
            trunk(t, 0, 0, 0, h + 1, lg);
            for (int i = 0; i < 4; i++) {
               double a = (i + 0.25) * Math.PI / 2.0;
               int bx = (int)Math.round(Math.cos(a) * rad * 0.7);
               int bz = (int)Math.round(Math.sin(a) * rad * 0.7);
               for (int s = 1; s <= 3; s++) {
                  t.set(bx * s / 3, h - 1 + (s > 2 ? 1 : 0), bz * s / 3, lg);
               }
               blob(t, bx, h, bz, sr(2.0, rs, 3.6), 1.5, sr(2.0, rs, 3.6), lf, r);
            }
            break;
         }
         case 4: { // forked twin-trunk oak
            int h = sh(6 + r.nextInt(4), hs, 48);
            int lean = 2 + r.nextInt(2);
            for (int y = 0; y <= h; y++) {
               t.set(0, y, 0, lg);
               if (y > h / 3) {
                  t.set(lean * (y - h / 3) / (h - h / 3 + 1), y, 0, lg);
                  t.set(-lean * (y - h / 3) / (h - h / 3 + 1), y, 0, lg);
               }
            }
            double rad = sr(2.6, rs, 5.0);
            for (int side = -1; side <= 1; side += 2) {
               int cx = side * lean;
               blob(t, cx, h + 1, 0, rad, sr(2.4, rs, 4.2), rad, lf, r);
               blob(t, cx, h + 3, 0, rad * 0.6, 1.4, rad * 0.6, lf, r);
            }
            blob(t, 0, h, 0, rad, sr(2.0, rs, 3.6), rad, lf, r);
            break;
         }
         case 5: { // weeping oak with drooping branch tips and vines
            int h = sh(7 + r.nextInt(4), hs, 48);
            double rad = sr(3.4, rs, 7.0);
            disc(t, h - 1, 0, 0, rad * 0.8, lf, r, 0.30F);
            disc(t, h, 0, 0, rad, lf, r, 0.28F);
            disc(t, h + 1, 0, 0, rad * 0.86, lf, r, 0.26F);
            disc(t, h + 2, 0, 0, rad * 0.55, lf, r, 0.22F);
            int n = (int)rad;
            for (int i = 0; i < 8; i++) {
               double a = r.nextDouble() * Math.PI * 2.0;
               int bx = (int)Math.round(Math.cos(a) * n);
               int bz = (int)Math.round(Math.sin(a) * n);
               int len = 1 + r.nextInt(3);
               for (int d = 0; d < len; d++) t.leaf(bx, h - 2 - d, bz, lf);
            }
            trunk(t, 0, 0, 0, h, lg);
            vines(t, r, 0.20F, 4);
            break;
         }
         default: { // 0: the classic round, lobed oak
            int h = sh(5 + r.nextInt(3), hs, 46);
            int cy = h - 1;
            double rad = sr(3.1, rs, 6.2);
            double ry = sr(2.5, rs * 0.9, 4.2);
            blob(t, 0, cy, 0, rad, ry, rad, lf, r);
            int lobes = 2 + r.nextInt(3);
            for (int i = 0; i < lobes; i++) {
               double a = r.nextDouble() * Math.PI * 2.0;
               double dist = rad * 0.86;
               int lx = (int)Math.round(Math.cos(a) * dist);
               int lz = (int)Math.round(Math.sin(a) * dist);
               int ly = cy - r.nextInt(3);
               blob(t, lx, ly, lz, sr(2.1, rs, 3.8), sr(1.8, rs, 3.0), sr(2.1, rs, 3.8), lf, r);
               int sx = Integer.signum(lx);
               int sz = Integer.signum(lz);
               int px = 0, pz = 0;
               for (int s = 0; s < 3 && !(px == lx && pz == lz); s++) {
                  if (Math.abs(lx - px) >= Math.abs(lz - pz)) px += sx;
                  else pz += sz;
                  t.set(px, ly - 1, pz, lg);
               }
            }
            blob(t, 0, cy + 2, 0, rad * 0.55, 1.4, rad * 0.55, lf, r);
            trunk(t, 0, 0, 0, h, lg);
            break;
         }
      }
   }

   // ------------------------------------------------------------------ birch: classic, tall, clump, weeping, grand
   private static void birchShape(Buf t, Random r, int v, double hs, double rs) {
      int lg = log(1);
      int lf = leaves(1);
      switch (v) {
         case 1: { // tall pale birch
            int h = sh(9 + r.nextInt(4), hs, 56);
            double rad = sr(2.0, rs, 3.6);
            for (int y = h - 4; y <= h + 1; y++) {
               double rr = y >= h ? rad * 0.6 : rad;
               disc(t, y, 0, 0, rr, lf, r, 0.30F);
            }
            trunk(t, 0, 0, 0, h, lg);
            break;
         }
         case 2: { // a clump of three thin stems from one root
            int h = sh(7 + r.nextInt(4), hs, 50);
            int[][] stems = {{0, 0}, {2, 1}, {-2, -1}};
            for (int[] q : stems) {
               for (int y = 0; y <= h; y++) t.set(q[0], y, q[1], lg);
               blob(t, q[0], h, q[1], sr(1.9, rs, 3.4), sr(2.2, rs, 3.6), sr(1.9, rs, 3.4), lf, r);
            }
            blob(t, 0, h + 2, 0, sr(1.4, rs, 2.6), 1.4, sr(1.4, rs, 2.6), lf, r);
            break;
         }
         case 3: { // weeping birch
            int h = sh(10 + r.nextInt(4), hs, 56);
            double rad = sr(1.9, rs, 3.4);
            disc(t, h - 2, 0, 0, rad, lf, r, 0.26F);
            disc(t, h, 0, 0, rad, lf, r, 0.24F);
            disc(t, h + 1, 0, 0, rad * 0.6, lf, r, 0.18F);
            for (int i = 0; i < 8; i++) {
               double a = r.nextDouble() * Math.PI * 2.0;
               int bx = (int)Math.round(Math.cos(a) * rad);
               int bz = (int)Math.round(Math.sin(a) * rad);
               for (int d = 0; d < 2 + r.nextInt(3); d++) t.leaf(bx, h - 3 - d, bz, lf);
            }
            trunk(t, 0, 0, 0, h, lg);
            break;
         }
         case 4: { // grand birch: two distinct crown tiers
            int h = sh(13 + r.nextInt(5), hs, 58);
            double rad = sr(2.6, rs, 5.0);
            disc(t, h - 6, 0, 0, rad * 0.85, lf, r, 0.32F);
            disc(t, h - 5, 0, 0, rad, lf, r, 0.28F);
            disc(t, h - 2, 0, 0, rad * 0.8, lf, r, 0.28F);
            disc(t, h - 1, 0, 0, rad * 0.95, lf, r, 0.26F);
            disc(t, h, 0, 0, rad * 0.7, lf, r, 0.22F);
            disc(t, h + 1, 0, 0, rad * 0.4, lf, r, 0.18F);
            trunk(t, 0, 0, 0, h, lg);
            break;
         }
         default: { // classic narrow birch
            int h = sh(6 + r.nextInt(3), hs, 46);
            int cy = h - 1;
            blob(t, 0, cy, 0, sr(2.3, rs, 4.2), sr(3.0, rs, 4.6), sr(2.3, rs, 4.2), lf, r);
            blob(t, 0, cy + 2, 0, sr(1.4, rs, 2.6), 1.6, sr(1.4, rs, 2.6), lf, r);
            trunk(t, 0, 0, 0, h, lg);
            break;
         }
      }
   }

   // ------------------------------------------------------------------ spruce: tiered, pine, columnar, mega, grand, candle
   private static void spruceShape(Buf t, Random r, int v, double hs, double rs, int size) {
      int lg = log(2);
      int lf = leaves(2);
      boolean wide = v == 3 || v == 4;
      // Giant conifers grow TALL, not fat: tiers are spaced out and the crown stays a hollow shell.
      int step = size >= 2 ? 3 : 2;
      int h = sh(v == 1 || v == 2 ? 12 + r.nextInt(6) : (wide ? 15 + r.nextInt(9) : 9 + r.nextInt(5)), hs, 56);
      int maxR = Math.min(6, Math.max(2, (int)Math.round((wide ? 4.6 : 3.3) * rs)));
      if (v == 2) maxR = Math.max(2, maxR - 2); // columnar
      if (v == 1) maxR = Math.max(2, maxR - 1); // sparse pine
      double cx = wide ? 0.5 : 0.0;
      double cz = wide ? 0.5 : 0.0;
      for (int y = h + 1; y >= 3; y -= step) {
         int f = h + 1 - y;
         int rad = Math.max(0, maxR - f / (v == 2 ? 7 : 5));
         if (f % (step * 4) == step * 3) rad = Math.max(0, rad - 1); // gap between tiers
         if (rad == 0) {
            t.leaf(0, y, 0, lf);
            continue;
         }
         for (int dx = -rad; dx <= rad; dx++) {
            for (int dz = -rad; dz <= rad; dz++) {
               double d = Math.sqrt((dx - cx) * (dx - cx) + (dz - cz) * (dz - cz));
               if (d > rad + 0.25) continue;
               // hollow interior: bigger trees stay cheap to mesh
               if (d < rad - (size >= 2 ? 1.7 : 1.1) && r.nextInt(3) != 0) continue;
               t.leaf(dx, y, dz, lf);
               if (d >= rad - 0.7 && y > 3 && r.nextInt(4) == 0) t.leaf(dx, y - 1, dz, lf);
            }
         }
      }
      if (wide) trunk2(t, 0, h + 1, lg);
      else trunk(t, 0, 0, 0, h, lg);
      if (v == 5) { // candle spruce: a long bare spire above the crown
         for (int y = h + 2; y <= Math.min(Buf.H - 2, h + 4); y++) t.set(0, y, 0, lg);
      }
   }

   // ------------------------------------------------------------------ jungle: small, tall, mega, canopy, tower
   private static void jungleShape(Buf t, Random r, int v, double hs, double rs) {
      int lg = log(3);
      int lf = leaves(3);
      switch (v) {
         case 1: { // tall layered jungle tree
            int h = sh(11 + r.nextInt(9), hs, 56);
            double rad = sr(3.8, rs, 7.4);
            disc(t, h - 2, 0, 0, rad * 0.82, lf, r, 0.40F);
            disc(t, h - 1, 0, 0, rad, lf, r, 0.34F);
            disc(t, h, 0, 0, rad * 0.82, lf, r, 0.30F);
            disc(t, h + 1, 0, 0, rad * 0.5, lf, r, 0.20F);
            double a = r.nextDouble() * Math.PI * 2.0;
            int bx = (int)Math.round(Math.cos(a) * rad * 0.8);
            int bz = (int)Math.round(Math.sin(a) * rad * 0.8);
            blob(t, bx, h - 4, bz, sr(2.2, rs, 4.0), 1.6, sr(2.2, rs, 4.0), lf, r);
            trunk(t, 0, 0, 0, h, lg);
            jungleVines(t, r, h);
            break;
         }
         case 2: { // giant 2x2 jungle tree
            int h = sh(20 + r.nextInt(12), hs, 58);
            double rad = sr(5.8, rs, 9.0);
            disc(t, h, 0.5, 0.5, rad, lf, r, 0.38F);
            disc(t, h + 1, 0.5, 0.5, rad * 0.85, lf, r, 0.34F);
            disc(t, h + 2, 0.5, 0.5, rad * 0.55, lf, r, 0.28F);
            for (int i = 0; i < 3; i++) {
               double a = r.nextDouble() * Math.PI * 2.0;
               int bx = (int)Math.round(Math.cos(a) * rad * 0.85);
               int bz = (int)Math.round(Math.sin(a) * rad * 0.85);
               blob(t, bx, h - 5 - r.nextInt(4), bz, sr(2.8, rs, 4.0), 1.8, sr(2.8, rs, 4.0), lf, r);
               for (int s = 1; s <= 4; s++) t.set(bx * s / 4, h - 6 + (s > 2 ? 1 : 0), bz * s / 4, lg);
            }
            trunk2(t, 0, h, lg);
            vines(t, r, 0.26F, 6);
            break;
         }
         case 3: { // huge flat rainforest canopy with buttress roots
            int h = sh(16 + r.nextInt(8), hs, 58);
            double rad = sr(6.6, rs, 9.5);
            disc(t, h - 1, 0, 0, rad * 0.86, lf, r, 0.42F);
            disc(t, h, 0, 0, rad, lf, r, 0.38F);
            disc(t, h + 1, 0, 0, rad * 0.94, lf, r, 0.34F);
            disc(t, h + 2, 0, 0, rad * 0.6, lf, r, 0.28F);
            for (int i = 0; i < 4; i++) {
               double a = i * Math.PI / 2.0;
               int bx = (int)Math.round(Math.cos(a) * rad * 0.75);
               int bz = (int)Math.round(Math.sin(a) * rad * 0.75);
               blob(t, bx, h, bz, sr(2.2, rs, 3.2), 1.4, sr(2.2, rs, 3.2), lf, r);
               for (int y = 0; y < 3; y++) t.set(bx * (3 - y) / 3, y, bz * (3 - y) / 3, lg);
            }
            trunk(t, 0, 0, 0, h, lg);
            vines(t, r, 0.24F, 7);
            break;
         }
         case 4: { // very tall emergent with a tiny high crown
            int h = sh(24 + r.nextInt(9), hs, 59);
            double rad = sr(2.6, rs, 4.6);
            disc(t, h - 1, 0, 0, rad, lf, r, 0.34F);
            disc(t, h, 0, 0, rad * 0.85, lf, r, 0.28F);
            disc(t, h + 1, 0, 0, rad * 0.5, lf, r, 0.20F);
            trunk(t, 0, 0, 0, h, lg);
            jungleVines(t, r, h);
            break;
         }
         default: { // small jungle tree
            int h = sh(5 + r.nextInt(5), hs, 46);
            double rad = sr(2.0, rs, 3.8);
            for (int y = h - 2; y <= h + 1; y++) disc(t, y, 0, 0, y <= h - 1 ? rad : rad * 0.6, lf, r, 0.26F);
            trunk(t, 0, 0, 0, h, lg);
            vines(t, r, 0.28F, 3);
            break;
         }
      }
   }

   private static void jungleVines(Buf t, Random r, int h) {
      for (int d = 0; d < 4; d++) {
         if (r.nextInt(2) != 0) continue;
         int vx = new int[]{0, 1, 0, -1}[d];
         int vz = new int[]{-1, 0, 1, 0}[d];
         int bit = 1 << (d + 2 & 3);
         for (int y = 2; y < h - 1; y++) t.leaf(vx, y, vz, Blocks313.VINE0 + bit);
      }
      vines(t, r, 0.20F, 5);
   }

   // ------------------------------------------------------------------ acacia: bent, tall double-bend, wide plate, forked
   private static void acaciaShape(Buf t, Random r, int v, double hs, double rs) {
      int lg = log(4);
      int lf = leaves(4);
      int h = sh(v == 1 ? 8 + r.nextInt(4) : (v == 2 ? 5 + r.nextInt(3) : 6 + r.nextInt(3)), hs, 50);
      int dir = r.nextInt(4);
      int dx = new int[]{0, 1, 0, -1}[dir];
      int dz = new int[]{-1, 0, 1, 0}[dir];
      int bend = h - 2 - r.nextInt(2);
      int x = 0, z = 0, y = 0;
      for (; y <= bend; y++) t.set(x, y, z, lg);
      int steps = h - bend + 1 + (v == 1 ? 3 : 0);
      for (int s = 0; s < steps; s++) {
         x += dx;
         z += dz;
         if (v == 1 && s == steps / 2) { dx = -dx; dz = -dz; } // a second bend
         t.set(x, y, z, lg);
         if (s % 2 == 1) y++;
      }
      double rad = sr(v == 2 ? 4.4 : 3.0, rs, 8.0);
      disc(t, y + 1, x, z, rad, lf, r, 0.34F);
      disc(t, y + 2, x, z, rad * (v == 2 ? 0.9 : 0.7), lf, r, 0.30F);
      if (v == 2) disc(t, y + 3, x, z, rad * 0.55, lf, r, 0.26F);
      if (v == 3 || r.nextInt(3) != 0) {
         int ax = -dx, az = -dz;
         int py = bend - 1, qx = 0, qz = 0;
         for (int s = 0; s < 3; s++) {
            qx += ax;
            qz += az;
            t.set(qx, py, qz, lg);
            if (s % 2 == 0) py++;
         }
         disc(t, py + 1, qx, qz, rad * 0.7, lf, r, 0.32F);
      }
   }

   // ------------------------------------------------------------------ dark oak, swamp and bush
   private static void darkOakShape(Buf t, Random r, int v, double hs, double rs) {
      int lf = leaves(5);
      int lg = log(5);
      int h = sh(v == 1 ? 10 + r.nextInt(5) : (v == 2 ? 5 + r.nextInt(3) : 6 + r.nextInt(3)), hs, 52);
      double rad = sr(v == 2 ? 5.2 : 4.0, rs, 9.0);
      disc(t, h - 1, 0.5, 0.5, rad * 0.78, lf, r, 0.42F);
      disc(t, h, 0.5, 0.5, rad, lf, r, 0.34F);
      disc(t, h + 1, 0.5, 0.5, rad * 0.92, lf, r, 0.30F);
      disc(t, h + 2, 0.5, 0.5, rad * 0.6, lf, r, 0.28F);
      if (v == 1) { disc(t, h + 3, 0.5, 0.5, rad * 0.38, lf, r, 0.24F); }
      int branches = v == 2 ? 5 : 3;
      for (int i = 0; i < branches; i++) {
         double a = r.nextDouble() * Math.PI * 2.0;
         int bx = (int)Math.round(0.5 + Math.cos(a) * rad * 0.72);
         int bz = (int)Math.round(0.5 + Math.sin(a) * rad * 0.72);
         blob(t, bx, h - 1 - r.nextInt(2), bz, sr(2.0, rs, 3.4), 1.6, sr(2.0, rs, 3.4), lf, r);
         for (int s = 1; s <= 2; s++) t.set(bx * s / 2, h - 1, bz * s / 2, lg);
      }
      trunk2(t, 0, h, lg);
   }

   private static void swampShape(Buf t, Random r, int v, double hs, double rs) {
      int lf = leaves(0);
      int lg = log(0);
      switch (v) {
         case 1: { // swamp cypress with knees
            int h = sh(9 + r.nextInt(5), hs, 52);
            double rad = sr(2.8, rs, 5.0);
            for (int y = h - 3; y <= h + 1; y++) disc(t, y, 0, 0, rad * (y >= h ? 0.6 : 1.0), lf, r, 0.32F);
            trunk(t, 0, 0, 0, h, lg);
            for (int i = 0; i < 4; i++) {
               double a = i * Math.PI / 2.0;
               int kx = (int)Math.round(Math.cos(a) * 2.0);
               int kz = (int)Math.round(Math.sin(a) * 2.0);
               t.set(kx, 0, kz, lg);
               t.set(kx, 1, kz, lg);
            }
            vines(t, r, 0.24F, 5);
            break;
         }
         case 2: { // wide willow with long hanging curtains
            int h = sh(7 + r.nextInt(4), hs, 50);
            double rad = sr(4.4, rs, 8.4);
            disc(t, h - 1, 0, 0, rad * 0.8, lf, r, 0.38F);
            disc(t, h, 0, 0, rad, lf, r, 0.34F);
            disc(t, h + 1, 0, 0, rad * 0.7, lf, r, 0.30F);
            int n = (int)rad;
            for (int i = 0; i < 10; i++) {
               double a = r.nextDouble() * Math.PI * 2.0;
               int bx = (int)Math.round(Math.cos(a) * n);
               int bz = (int)Math.round(Math.sin(a) * n);
               for (int d = 0; d < 2 + r.nextInt(4); d++) t.leaf(bx, h - 2 - d, bz, lf);
            }
            trunk(t, 0, 0, 0, h, lg);
            vines(t, r, 0.36F, 7);
            break;
         }
         default: { // the old, wide swamp oak
            int h = sh(5 + r.nextInt(3), hs, 46);
            double rad = sr(3.0, rs, 6.0);
            for (int y = h - 2; y <= h + 1; y++) disc(t, y, 0, 0, y <= h - 1 ? rad : rad * 0.66, lf, r, 0.30F);
            trunk(t, 0, 0, 0, h, lg);
            vines(t, r, 0.28F, 4);
            break;
         }
      }
   }

   private static void bushShape(Buf t, Random r, int v, double hs, double rs) {
      int sp = 3;
      int lg = log(sp);
      int lf = leaves(sp);
      double rad = sr(v == 1 ? 3.0 : 2.2, rs, 5.2);
      int h = sh(v == 1 ? 3 : 1, hs, 6);
      t.set(0, 0, 0, lg);
      for (int y = 1; y <= h; y++) t.set(0, y, 0, lg);
      blob(t, 0, h + 1, 0, rad, sr(1.5, rs, 2.8), rad, lf, r);
      t.leaf(0, h + 2, 0, lf);
      if (v == 1) vines(t, r, 0.20F, 2);
   }

   // ---------------------------------------------------------------------------------------------------- legacy entry points
   /** The classic oak / birch / spruce of the old generator. Kept for the headless regressions. */
   static void classic(Buf t, Random r, int sp, int size) {
      build(t, r, sp == 1 ? SP_BIRCH : (sp == 2 ? SP_SPRUCE : SP_OAK), 0, 0, 1.0F);
   }

   static void smallOak(Buf t, Random r, int sp) {
      build(t, r, SP_OAK, 1, 0, 1.0F);
   }

   static void tallBirch(Buf t, Random r) {
      build(t, r, SP_BIRCH, 1, 0, 1.0F);
   }

   static void megaSpruce(Buf t, Random r) {
      build(t, r, SP_SPRUCE, 3, 0, 1.0F);
   }

   static void darkOak(Buf t, Random r) {
      build(t, r, SP_DARK_OAK, 0, 0, 1.0F);
   }

   static void jungleSmall(Buf t, Random r) {
      build(t, r, SP_JUNGLE, 0, 0, 1.0F);
   }

   static void jungleTall(Buf t, Random r) {
      build(t, r, SP_JUNGLE, 1, 0, 1.0F);
   }

   static void megaJungle(Buf t, Random r) {
      build(t, r, SP_JUNGLE, 2, 0, 1.0F);
   }

   static void bush(Buf t, Random r, int sp) {
      build(t, r, SP_BUSH, 0, 0, 1.0F);
   }

   static void acacia(Buf t, Random r) {
      build(t, r, SP_ACACIA, 0, 0, 1.0F);
   }

   static void swampOak(Buf t, Random r) {
      build(t, r, SP_SWAMP, 0, 0, 1.0F);
   }

   // ---------------------------------------------------------------------------------------------------- mushrooms & ice
   static void giantMushroom(Buf t, Random r, int kind) {
      giantMushroom(t, r, kind, 0, 1.0F);
   }

   /** Update 6.23: mushrooms get the same size tiers (they are tree-sized structures). */
   static void giantMushroom(Buf t, Random r, int kind, int size, float scale) {
      int h = sh(4 + r.nextInt(3), heightScale(size, scale) * 0.75, 18);
      int cap = kind == 0 ? Blocks313.MUSH_BLOCK_RED : Blocks313.MUSH_BLOCK_BROWN;
      int rad = Math.max(2, Math.min(Buf.R - 1, (int)Math.round((kind == 0 ? 3.0 : 3.3) * radiusScale(size, scale))));
      for (int y = 0; y < h; y++) t.set(0, y, 0, Blocks313.MUSH_STEM);
      if (kind == 0) {
         for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            if (Math.abs(dx) == 2 && Math.abs(dz) == 2) continue;
            t.set(dx, h, dz, cap);
         }
         for (int dy = 1; dy <= rad - 1; dy++) {
            for (int dx = -rad; dx <= rad; dx++) for (int dz = -rad; dz <= rad; dz++) {
               if (Math.max(Math.abs(dx), Math.abs(dz)) == rad && Math.abs(dx) + Math.abs(dz) <= rad + 2) {
                  t.set(dx, h - dy, dz, cap);
               }
            }
         }
      } else {
         for (int dx = -rad; dx <= rad; dx++) for (int dz = -rad; dz <= rad; dz++) {
            if (Math.abs(dx) + Math.abs(dz) > rad + 1 || Math.abs(dx) == rad && Math.abs(dz) == rad) continue;
            t.set(dx, h, dz, cap);
         }
      }
   }

   static void iceSpike(Buf t, Random r) {
      iceSpike(t, r, 0, 1.0F);
   }

   static void iceSpike(Buf t, Random r, int size, float scale) {
      int height = sh(8 + r.nextInt(18), heightScale(size, scale), 52);
      int base = 1 + r.nextInt(3);
      boolean tall = r.nextInt(4) == 0;
      if (tall) { height = Math.min(56, height + 10); base++; }
      for (int y = -2; y < height; y++) {
         double f = y <= 0 ? 1.0 : 1.0 - (double)y / height;
         double rad = base * f + (f > 0.3 ? 0.4 : 0.0);
         int ir = (int)Math.ceil(rad);
         for (int dx = -ir; dx <= ir; dx++) for (int dz = -ir; dz <= ir; dz++) {
            if (Math.sqrt(dx * dx + dz * dz) <= rad + (y <= 0 ? 0.6 : 0.0)) {
               t.set(dx, y + 2, dz, y % 5 == 4 ? Blocks313.ICE : Blocks313.PACKED_ICE);
            }
         }
      }
      t.set(0, height + 2, 0, Blocks313.PACKED_ICE);
   }
}
