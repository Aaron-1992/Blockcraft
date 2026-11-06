package bc;

import java.util.ArrayList;
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
      static final int MAX_LEAVES = 1200;
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
   /**
    * A closed, gently irregular 3D canopy. Only a shell is generated (the inside of a
    * voxel crown is never visible) but the top and bottom are capped, so from every
    * angle it reads as a solid rounded volume instead of a one-block-thick plate.
    * cx/cy/cz are the crown centre in local cell space (0.5 for 2x2 trunks);
    * ox/oy/oz are integer offsets added to every block.
    */
   static void crown(Buf t, Random r, double cx, double cy, double cz,
                     double rx, double ry, double rz, int id, float rough,
                     int ox, int oy, int oz) {
      int ix = (int)Math.ceil(rx + 0.5);
      int iz = (int)Math.ceil(rz + 0.5);
      int iy = Math.max(1, (int)Math.ceil(ry));
      for (int dy = -iy; dy <= iy; dy++) {
         double f = (dy - cy) / (ry + 0.45);
         double prof = Math.sqrt(Math.max(0.0, 1.0 - f * f));
         if (prof < 0.38) prof = 0.38;              // rounded ends, never a one-cell needle
         double lx = Math.max(0.55, rx * prof);
         double lz = Math.max(0.55, rz * prof);
         boolean cap = dy == -iy || dy == iy;        // solid top and bottom
         for (int dx = -ix; dx <= ix; dx++) {
            for (int dz = -iz; dz <= iz; dz++) {
               double nx = (dx - cx) / lx;
               double nz = (dz - cz) / lz;
               double d = Math.sqrt(nx * nx + nz * nz);
               if (d > 1.02) continue;
               // Absolute shell thickness: big crowns stay cheap instead of becoming solid balls.
               double shell = Math.min(0.50, 1.55 / Math.max(1.0, (lx + lz) * 0.5));
               boolean rim = d > 1.0 - shell;
               if (!cap && !rim) continue;           // hollow core
               if (rim && !cap && rough > 0.0F && r.nextFloat() < rough) continue;
               t.leaf(ox + dx, oy + dy, oz + dz, id);
            }
         }
      }
   }

   static void blob(Buf t, int cx, int cy, int cz, double rx, double ry, double rz, int id, Random r) {
      crown(t, r, 0.0, 0.0, 0.0, rx, Math.max(1.0, ry), rz, id, 0.02F, cx, cy, cz);
   }

   static void disc(Buf t, int cy, double cx, double cz, double rad, int id, Random r, float skip) {
      // A flattened dome with real thickness, not a single layer of floating leaves.
      double ry = Math.max(1.0, Math.min(2.0, rad * 0.36));
      crown(t, r, cx, 0.0, cz, rad, ry, rad, id, Math.min(0.035F, skip * 0.10F), 0, cy, 0);
   }

   /**
    * One coherent rounded canopy built from a vertical radius profile. A stack of
    * independent discs produces overlapping half-shells and blows the leaf budget;
    * this has one closed surface, a real top and bottom, and no duplicate blocks.
    * roundness 0.3 = flat umbrella, 0.6 = full rounded crown.
    */
   static void dome(Buf t, Random r, double cx, double cz, int y0, int layers,
                    double rad, int id, double roundness, float rough) {
      // Taller crowns get more layers so a giant oak is a rounded volume, not a thick pancake.
      int n = Math.max(3, Math.max(layers, (int)Math.min(9, Math.round(rad * 0.85))));
      for (int k = 0; k < n; k++) {
         double u = n == 1 ? 0.0 : 2.0 * k / (n - 1) - 1.0;
         double prof = Math.sqrt(Math.max(0.0, 1.0 - u * u));
         // Never let a cap shrink to the single trunk cell: t.leaf() is soft, so a
         // one-cell layer would silently vanish and leave a flat / holed crown.
         double rr = Math.max(1.75, rad * (1.0 - roundness + roundness * prof));
         int ir = (int)Math.ceil(rr + 0.5);
         double shell = Math.min(0.55, 1.6 / Math.max(1.0, rr));
         boolean cap = k == 0 || k == n - 1;
         for (int dx = -ir; dx <= ir; dx++) {
            for (int dz = -ir; dz <= ir; dz++) {
               double d = Math.sqrt((dx - cx) * (dx - cx) + (dz - cz) * (dz - cz));
               if (d > rr) continue;
               if (!cap && d < rr * (1.0 - shell)) continue;
               if (!cap && rough > 0.0F && r.nextFloat() < rough) continue;
               t.leaf(dx, y0 + k, dz, id);
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

   /**
    * Measured silhouette of one species/variant/size tier. Distant LOD tiles use this
    * instead of a hand-drawn stand-in, so the low-detail forest really does depict the
    * trees the generator places (including giants, umbrellas and conifers).
    */
   static final class LodShape {
      float trunk;      // top of the trunk above the root
      float bottom;     // lowest foliage above the root
      float top;        // highest foliage above the root
      float radius;
      final float[] bands = new float[6];
      boolean wide;
      boolean conifer;

      float band(float f) {
         float u = (f < 0.0F ? 0.0F : (f > 1.0F ? 1.0F : f)) * (bands.length - 1);
         int k = (int)u;
         if (k >= bands.length - 1) return bands[bands.length - 1];
         return bands[k] + (bands[k + 1] - bands[k]) * (u - k);
      }
   }

   private static final LodShape[][][] LOD_SHAPES = measureLodShapes();

   static LodShape lodShape(int sp, int variant, int tier) {
      int s = Math.floorMod(sp, VARIANTS.length);
      int v = Math.floorMod(variant, variants(s));
      int t = tier < 0 ? 0 : (tier > 2 ? 2 : tier);
      return LOD_SHAPES[s][v][t];
   }

   private static LodShape[][][] measureLodShapes() {
      LodShape[][][] out = new LodShape[VARIANTS.length][8][3];
      for (int sp = 0; sp < VARIANTS.length; sp++) {
         for (int v = 0; v < variants(sp); v++) {
            for (int tier = 0; tier < 3; tier++) {
               LodShape acc = new LodShape();
               acc.wide = isWide(sp, v);
               acc.conifer = sp == SP_SPRUCE;
               float trunk = 0.0F, bottom = 1.0E6F, top = 0.0F;
               // Two samples smooth out the per-tree random height jitter.
               for (int sample = 0; sample < 2; sample++) {
                  Buf b = new Buf();
                  b.begin(0, 0, 0);
                  build(b, new Random(0x5EEDL + sp * 977L + v * 131L + tier * 17L + sample * 7919L), sp, v, tier, 1.0F);
                  int lo = Integer.MAX_VALUE, hi = 0;
                  for (int k = 0; k < b.nt; k++) {
                     int i = b.touched[k];
                     int id = (b.a[i] & ~Buf.SOFT) - 1;
                     if (!Blocks.isLeaves(id)) continue;
                     int dy = i / (Buf.W * Buf.W);
                     if (dy < lo) lo = dy;
                     if (dy > hi) hi = dy;
                  }
                  if (lo == Integer.MAX_VALUE) { lo = 0; hi = 1; }
                  float bl = (float)lo, th = (float)hi;
                  bottom = Math.min(bottom, bl);
                  top = Math.max(top, th);
                  for (int k = 0; k < b.nt; k++) {
                     int i = b.touched[k];
                     int id = (b.a[i] & ~Buf.SOFT) - 1;
                     int dy = i / (Buf.W * Buf.W);
                     int dx = i % Buf.W - Buf.R;
                     int dz = i / Buf.W % Buf.W - Buf.R;
                     if (!Blocks.isLeaves(id)) {
                        if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) trunk = Math.max(trunk, (float)dy);
                        continue;
                     }
                     float f = (dy - bl) / Math.max(1.0F, th - bl);
                     int bi = Math.max(0, Math.min(5, (int)(f * 5.999F)));
                     float r = (float)Math.sqrt(dx * dx + dz * dz) + 0.5F;
                     if (r > acc.bands[bi]) acc.bands[bi] = r;
                  }
               }
               acc.bottom = bottom;
               acc.top = Math.max(bottom + 1.0F, top);
               acc.trunk = Math.max(1.0F, Math.min(acc.bottom + 1.0F, trunk));
               for (int k = 0; k < 6; k++) {
                  if (acc.bands[k] < 0.75F) acc.bands[k] = 0.75F;
               }
               // Smooth isolated gaps: a real crown may have a notch at one height, but a
               // distant box must not collapse to a needle between two wide tiers.
               for (int pass = 0; pass < 2; pass++) {
                  float[] prev = acc.bands.clone();
                  for (int k = 0; k < 6; k++) {
                     float lo = k > 0 ? prev[k - 1] : prev[k];
                     float hi = k < 5 ? prev[k + 1] : prev[k];
                     float floor = 0.62F * Math.max(lo, hi);
                     if (acc.bands[k] < floor) acc.bands[k] = floor;
                     acc.bands[k] = (acc.bands[k] * 2.0F + lo + hi) * 0.25F;
                  }
               }
               for (int k = 0; k < 6; k++) {
                  if (acc.bands[k] < 0.75F) acc.bands[k] = 0.75F;
                  acc.radius = Math.max(acc.radius, acc.bands[k]);
               }
               out[sp][v][tier] = acc;
            }
         }
      }
      return out;
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
      grow(t, r, sp, v, heightScale(size, worldScale), radiusScale(size, worldScale), size);
   }

   static void build(Buf t, Random r, int sp, int variant, int size) {
      build(t, r, sp, variant, size, 1.0F);
   }

   // ------------------------------------------------------------------ V6.25: branch-skeleton trees
   //
   // A tree is no longer a geometric dome. It is a 6-connected skeleton of logs (a trunk
   // that may lean, primary branches that reach out and twigs at their tips) with separate
   // irregular leaf clusters hanging on those tips. That gives visible branches, real
   // negative space and a lumpy, asymmetric silhouette - and it puts every leaf within a
   // few steps of a log, which is exactly what the falling-tree planner needs to topple
   // the whole plant instead of leaving a floating canopy behind.

   private static final class Spec {
      int h = 8;
      double bandLo = 0.45, bandHi = 0.96;
      int branches = 9;
      double len = 4.2, rise = 0.30, cr = 2.3;
      int perBranch = 1;
      boolean whorls = false, flat = false, top = true;
      int whorlStep = 2;
      int strands = 0;
      int lean = 0;
      boolean fork = false, clump = false, knees = false, candle = false;
   }

   /** Architecture of one species and silhouette, before the size tier scales it. */
   private static Spec spec(int sp, int v, double hs, double rs, int size) {
      Spec s = new Spec();
      switch (sp) {
         case SP_OAK:
            s.h = 9; s.branches = 9; s.len = 4.3; s.rise = 0.34; s.cr = 2.30;
            s.bandLo = 0.67; s.perBranch = 2; s.lean = 1;
            if (v == 1) { s.h = 6; s.branches = 7; s.len = 3.1; s.cr = 2.0; s.bandLo = 0.58; }
            else if (v == 2) { s.h = 13; s.branches = 6; s.len = 2.9; s.cr = 2.1; s.bandLo = 0.76; s.bandHi = 1.0; s.lean = 0; }
            else if (v == 3) { s.h = 7; s.branches = 9; s.len = 6.3; s.rise = 0.10; s.cr = 2.8; s.flat = true; s.top = false; s.bandLo = 0.80; s.lean = 0; }
            else if (v == 4) { s.fork = true; s.branches = 7; s.len = 4.1; }
            else if (v == 5) { s.strands = 10; s.rise = 0.18; s.bandLo = 0.70; }
            break;
         case SP_BIRCH:
            s.h = 11; s.branches = 6; s.len = 2.8; s.rise = 0.48; s.cr = 1.95;
            s.bandLo = 0.68; s.bandHi = 0.98; s.perBranch = 2; s.lean = 1;
            if (v == 1) { s.h = 14; s.branches = 5; s.len = 2.5; s.bandLo = 0.72; s.lean = 0; }
            else if (v == 2) { s.clump = true; s.h = 8; s.branches = 6; s.len = 2.6; s.cr = 1.9; s.bandLo = 0.68; }
            else if (v == 3) { s.strands = 8; s.rise = 0.28; s.bandLo = 0.60; }
            else if (v == 4) { s.h = 15; s.branches = 8; s.len = 3.0; s.cr = 2.1; s.bandLo = 0.54; s.perBranch = 2; }
            break;
         case SP_SPRUCE:
            s.h = 13; s.branches = 5; s.len = 3.3; s.rise = -0.14; s.cr = 1.60;
            s.whorls = true; s.whorlStep = 2; s.bandLo = 0.32; s.top = true;
            if (v == 1) { s.h = 14; s.branches = 4; s.len = 2.5; s.cr = 1.45; s.whorlStep = 3; }
            else if (v == 2) { s.h = 15; s.branches = 4; s.len = 1.8; s.cr = 1.40; s.bandLo = 0.26; }
            else if (v == 5) { s.candle = true; s.h = 12; s.branches = 4; s.len = 2.6; }
            break;
         case SP_JUNGLE:
            s.h = 17; s.branches = 7; s.len = 5.6; s.rise = 0.24; s.cr = 2.8;
            s.bandLo = 0.78; s.bandHi = 1.0; s.perBranch = 2; s.lean = 1;
            if (v == 0) { s.h = 8; s.branches = 5; s.len = 2.8; s.cr = 2.0; s.bandLo = 0.66; }
            else if (v == 1) { s.h = 14; s.branches = 7; s.len = 4.4; s.cr = 2.5; s.bandLo = 0.72; }
            else if (v == 3) { s.h = 15; s.branches = 9; s.len = 7.0; s.rise = 0.12; s.cr = 2.9; s.flat = true; s.top = false; s.bandLo = 0.78; }
            else if (v == 4) { s.h = 27; s.branches = 5; s.len = 3.0; s.cr = 2.0; s.bandLo = 0.88; s.lean = 0; }
            break;
         case SP_ACACIA:
            s.h = 6; s.branches = 5; s.len = 6.2; s.rise = 0.10; s.cr = 2.8;
            s.bandLo = 0.78; s.bandHi = 1.0; s.flat = true; s.top = false; s.perBranch = 2; s.lean = 2;
            if (v == 1) { s.h = 9; s.len = 5.4; s.lean = 3; }
            else if (v == 2) { s.branches = 7; s.len = 7.0; s.cr = 3.0; s.top = true; }
            else if (v == 3) { s.fork = true; s.branches = 6; s.len = 5.0; }
            break;
         case SP_DARK_OAK:
            s.h = 8; s.branches = 10; s.len = 4.1; s.rise = 0.30; s.cr = 2.30;
            s.bandLo = 0.63; s.perBranch = 2; s.lean = 1;
            if (v == 1) { s.h = 11; s.branches = 11; s.len = 4.6; }
            else if (v == 2) { s.h = 7; s.branches = 12; s.len = 5.0; s.rise = 0.16; s.cr = 2.6; s.bandLo = 0.56; }
            break;
         case SP_SWAMP:
            s.h = 10; s.branches = 8; s.len = 4.2; s.rise = 0.22; s.cr = 2.40;
            s.bandLo = 0.68; s.perBranch = 2; s.strands = 7; s.lean = 1;
            if (v == 1) { s.h = 11; s.knees = true; s.len = 3.6; s.cr = 2.1; s.strands = 4; }
            else if (v == 2) { s.h = 9; s.branches = 10; s.len = 5.0; s.rise = 0.14; s.strands = 14; s.cr = 2.6; s.bandLo = 0.66; }
            break;
         default: // bushes
            s.h = 2; s.branches = 5; s.len = 1.7; s.rise = 0.18; s.cr = 1.8;
            s.bandLo = 0.50; s.bandHi = 1.0; s.perBranch = 1; s.top = true;
            if (v == 1) { s.h = 4; s.branches = 6; s.len = 2.2; s.cr = 2.0; }
            break;
      }

      s.h = sh(s.h, hs, 56);
      s.len = Math.min(8.0, s.len * rs);
      s.cr = Math.min(2.75, s.cr * rs);
      // Bigger trees carry more branches, but not so many that the falling planner chokes.
      s.branches += size >= 1 ? 2 : 0;
      s.branches += size >= 2 ? 2 : 0;
      if (s.whorls) s.whorlStep = Math.max(2, s.whorlStep + (size >= 2 ? 2 : (size >= 1 ? 1 : 0)));
      return s;
   }

   /** One leaf bunch: an irregular, slightly hollow blob with a ragged edge. */
   private static void leafCluster(Buf t, Random r, int cx, int cy, int cz, double rad, int id, boolean flat) {
      double cr = Math.max(1.25, Math.min(2.85, rad));
      double ry = Math.max(1.0, flat ? cr * 0.42 : cr * 0.62);
      int ix = (int)Math.ceil(cr + 0.6);
      int iy = Math.max(1, (int)Math.ceil(ry));
      double phase = r.nextDouble() * 6.2831853;
      for (int dy = -iy; dy <= iy; dy++) {
         double f = dy / (ry + 0.45);
         double prof = Math.sqrt(Math.max(0.0, 1.0 - f * f));
         if (prof < 0.44) prof = 0.44;
         double lx = Math.max(0.9, cr * prof), lz = Math.max(0.9, cr * prof);
         boolean cap = dy == -iy || dy == iy;
         for (int dx = -ix; dx <= ix; dx++) {
            for (int dz = -ix; dz <= ix; dz++) {
               double ang = Math.atan2(dz, dx);
               double edge = 1.0 + 0.15 * Math.sin(3.0 * ang + phase) + 0.08 * Math.sin(5.0 * ang - phase);
               double d = Math.sqrt(dx * dx / (lx * lx) + dz * dz / (lz * lz));
               if (d > edge) continue;
               // Keep an airy middle but always leave a cross of leaves touching the twig,
               // so the falling-tree leaf flood-fill can reach the whole cluster.
               boolean bridge = Math.abs(dx) <= 1 && Math.abs(dz) <= 1 && (dx == 0 || dz == 0);
               if (!cap && !bridge && d < 0.58) continue;
               if (d > 0.80 && !cap && !bridge && r.nextFloat() < 0.11) continue;
               t.leaf(cx + dx, cy + dy, cz + dz, id);
            }
         }
      }
   }

   /** A 6-connected log branch. Returns {tipX, tipY, tipZ, logsPlaced}. */
   private static int[] branch(Buf t, Random r, int x0, int y0, int z0, double a, int len, double rise, int lg) {
      int x = x0, y = y0, z = z0, n = 0;
      // Anchor the branch: without this a whorl one block above the trunk top would
      // be diagonally detached from the trunk and would stay standing after a fall.
      t.set(x0, y0, z0, lg);
      n++;
      for (int s = 1; s <= len; s++) {
         int nx = x0 + (int)Math.round(Math.cos(a) * s);
         int nz = z0 + (int)Math.round(Math.sin(a) * s);
         int ny = y0 + (int)Math.round(rise * s);
         int guard = 0;
         while ((x != nx || y != ny || z != nz) && guard++ < 48) {
            if (x != nx) x += Integer.signum(nx - x);
            else if (z != nz) z += Integer.signum(nz - z);
            else y += Integer.signum(ny - y);
            if (y >= 0 && y < Buf.H && Math.abs(x) <= Buf.R && Math.abs(z) <= Buf.R) {
               t.set(x, y, z, lg);
               n++;
            }
         }
      }
      if (y + 1 < Buf.H && r.nextBoolean()) { t.set(x, y + 1, z, lg); n++; }
      return new int[]{x, y, z, n};
   }

   /** Grow the whole plant: trunk, branch skeleton, then leaf bunches on the twigs. */
   private static void grow(Buf t, Random r, int sp, int v, double hs, double rs, int size) {
      Spec s = spec(sp, v, hs, rs, size);
      int lg = log(sp), lf = leaves(sp);
      boolean wide = isWide(sp, v);
      int logs = 0;
      int budget = 420;

      int[] px = new int[Buf.H], pz = new int[Buf.H];
      int x = 0, z = 0;
      int driftEvery = s.lean > 0 ? Math.max(2, s.h / (s.lean + 1)) : Integer.MAX_VALUE;
      for (int y = 0; y <= s.h && y < Buf.H; y++) {
         if (y > 0 && y % driftEvery == 0) {
            int ox = x, oz = z;
            if (r.nextBoolean()) x += r.nextBoolean() ? 1 : -1;
            else z += r.nextBoolean() ? 1 : -1;
            x = Math.max(-3, Math.min(3, x));
            z = Math.max(-3, Math.min(3, z));
            // A lean must not jump diagonally: keep an elbow log at the old column
            // on this same layer, or the trunk above is disconnected and the whole
            // crown can no longer be picked up by the falling-tree planner.
            if (x != ox || z != oz) { t.set(ox, y, oz, lg); logs++; }
         }
         px[y] = x;
         pz[y] = z;
         // Thick base tapering to a single trunk is both cheaper and more natural than
         // a 2x2 column all the way to the top.
         boolean thick = wide && y <= s.h / 3;
         if (thick) {
            t.set(x, y, z, lg); t.set(x + 1, y, z, lg);
            t.set(x, y, z + 1, lg); t.set(x + 1, y, z + 1, lg);
            logs += 4;
         } else {
            t.set(x, y, z, lg);
            logs++;
         }
      }

      // Forked / clumped stems are joined to the trunk by a horizontal log run, so the
      // falling-tree connectivity scan sees one plant rather than several loose sticks.
      if (s.fork || s.clump) {
         for (int k = 0; k < 2; k++) {
            int sx = k == 0 ? -2 : 2;
            int sz = k == 0 ? -1 : 1;
            int y0 = s.h / 3;
            int cx = px[y0], cz = pz[y0];
            while (cx != sx || cz != sz) {
               if (cx != sx) cx += Integer.signum(sx - cx);
               else cz += Integer.signum(sz - cz);
               t.set(cx, y0, cz, lg);
               logs++;
            }
            for (int y = y0; y <= s.h + 2 && y < Buf.H; y++) {
               t.set(cx, y, cz, lg);
               logs++;
            }
         }
      }

      ArrayList<int[]> clusters = new ArrayList<>();
      if (s.whorls) {
         int start = Math.max(2, (int)(s.h * s.bandLo));
         for (int y = start; y <= s.h && logs < budget && y < Buf.H; y += s.whorlStep) {
            double f = (double)(y - start) / Math.max(1.0, s.h - start);
            int n = Math.max(3, s.branches - (int)(f * 2.0));
            double bl = Math.max(1.0, s.len * (1.0 - 0.90 * f));
            int by = Math.min(y, s.h);
            for (int k = 0; k < n && logs < budget; k++) {
               double a = k * (6.2831853 / n) + y * 0.61 + r.nextDouble() * 0.35;
               int[] tip = branch(t, r, px[by], y, pz[by], a,
                  Math.max(2, (int)Math.round(bl * (0.8 + 0.4 * r.nextDouble()))), s.rise, lg);
               logs += tip[3];
               clusters.add(new int[]{tip[0], tip[1], tip[2], (int)Math.round(s.cr * (1.0 - 0.65 * f) * 100), 0});
            }
            // A small bunch around the leader closes the cone between whorls.
            clusters.add(new int[]{px[by], y, pz[by], (int)Math.round(s.cr * 0.55 * 100), 0});
         }
      } else {
         for (int k = 0; k < s.branches && logs < budget; k++) {
            double f = (k + 0.5) / s.branches;
            int y = (int)Math.round(s.h * (s.bandLo + (s.bandHi - s.bandLo) * f)) + r.nextInt(3) - 1;
            y = Math.max(2, Math.min(s.h, y));
            double taper = 1.0 - 0.28 * f;
            int len = Math.max(2, (int)Math.round(s.len * taper * (0.78 + 0.44 * r.nextDouble())));
            double a = k * 2.399963229728653 + r.nextDouble() * 0.55;
            int[] tip = branch(t, r, px[y], y, pz[y], a, len, s.rise, lg);
            logs += tip[3];
            clusters.add(new int[]{tip[0], tip[1], tip[2], (int)Math.round(s.cr * 100), s.flat ? 1 : 0});
            if (s.perBranch > 1 && r.nextBoolean()) {
               int mx = px[y] + (tip[0] - px[y]) * 3 / 5;
               int mz = pz[y] + (tip[2] - pz[y]) * 3 / 5;
               int my = y + (tip[1] - y) * 3 / 5;
               clusters.add(new int[]{mx, my, mz, (int)Math.round(s.cr * 0.82 * 100), s.flat ? 1 : 0});
            }
         }
      }
      if ((s.top || s.candle) && s.h + 1 < Buf.H) t.set(px[s.h], s.h + 1, pz[s.h], lg);
      if (s.top) clusters.add(new int[]{px[s.h], s.h + 1, pz[s.h], (int)Math.round(s.cr * 0.92 * 100), s.flat ? 1 : 0});
      if (s.candle && s.h + 4 < Buf.H) {
         for (int y = s.h + 2; y <= s.h + 4; y++) t.set(px[s.h], y, pz[s.h], lg);
      }

      // Leaves go in last: t.leaf() is soft, so the branch skeleton stays visible.
      for (int[] c : clusters) {
         if (t.leafCount > Buf.MAX_LEAVES - 130) break;
         leafCluster(t, r, c[0], c[1], c[2], c[3] / 100.0, lf, c[4] == 1);
      }

      // Weeping strands are leaves (not vines) so they travel with the falling crown.
      if (s.strands > 0) {
         int n = t.nt;
         for (int k = 0; k < s.strands && k < n; k++) {
            int i = t.touched[r.nextInt(n)];
            int id = (t.a[i] & ~Buf.SOFT) - 1;
            if (!Blocks.isLeaves(id)) continue;
            int dx = i % Buf.W - Buf.R;
            int dz = i / Buf.W % Buf.W - Buf.R;
            int dy = i / (Buf.W * Buf.W);
            int len = 1 + r.nextInt(3);
            for (int s2 = 1; s2 <= len; s2++) t.leaf(dx, dy - s2, dz, lf);
         }
      }
      if (s.knees) {
         for (int d = 0; d < 4; d++) {
            int kx = new int[]{0, 2, 0, -2}[d];
            int kz = new int[]{-2, 0, 2, 0}[d];
            t.leaf(kx, 1, kz, lf);
            t.leaf(kx, 2, kz, lf);
         }
      }
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
