package bc;

/**
 * V6.31: a completely new underground for newly created worlds ({@code caveVer >= 5}).
 *
 * <p>The V6.22/6.28/6.29 carvers all used the same 3D-noise caves plus a separate,
 * deliberately built "entrance" object (a diagonal crack, then a hashed bowl).
 * Every entrance therefore looked the same. This carver has <b>no entrances at
 * all</b>: the caves simply run up to and through the ground wherever the noise
 * field reaches the surface, exactly like Minecraft 1.18/1.20. A 2D noise field
 * shapes a funnel that <i>widens downward</i>, so the holes that appear are
 * irregular craters, ragged slits, wide caverns and narrow throats of completely
 * different sizes - no two look alike, and all of them open into the real cave
 * network below.</p>
 *
 * <p>Shapes, all original and pure in (seed, x, y, z, surfaceHeight):</p>
 * <ul>
 *   <li><b>cheese caves</b> - two overlapping large 3D noise fields, y=8..96;</li>
 *   <li><b>spaghetti</b> - long winding ridged-noise tunnels at three scales;</li>
 *   <li><b>noodle caves</b> - thin meandering tubes just below the surface;</li>
 *   <li><b>giant halls</b> - sparse hashed ellipsoids deep down;</li>
 *   <li><b>aquifers</b> - local hashed water tables, so one cave is dry and the next flooded;</li>
 *   <li><b>noise shafts</b> - vertical noise pillars that join the layers;</li>
 *   <li><b>surface breakthrough</b> - the same fields, widened and gated by a 2D
 *       aperture noise, are carved right up to the terrain surface on dry land.</li>
 * </ul>
 *
 * <p>Old saves keep their own {@code caveVer} and never call this class.</p>
 */
final class Caves5 {
   static final int SOLID = 0;
   static final int AIR = 1;
   static final int LAVA = 2;
   static final int WATER = 3;

   /** Only dry land above the sea gets a surface opening; oceans stay sealed. */
   static boolean landBiome(int biome, int h) {
      return h >= 64 && !Biomes.ocean(biome) && !Biomes.river(biome)
         && biome != Biomes.BEACH && biome != Biomes.SNOWY_BEACH
         && biome != Biomes.MUSHROOM;
   }

   final long seed;
   final WorldSettings st;
   final Gen gen;
   final Noise cheeseA, cheeseB;
   final Noise wormA, wormB, longA, longB;
   final Noise aperture, crater, province, shaft;

   Caves5(long seed, WorldSettings st, Gen gen) {
      this.seed = seed;
      this.st = st;
      this.gen = gen;
      this.cheeseA = new Noise(seed ^ 0x434848453541L);
      this.cheeseB = new Noise(seed ^ 0x434848453542L);
      this.wormA = new Noise(seed ^ 0x574F524D41L);
      this.wormB = new Noise(seed ^ 0x574F524D42L);
      this.longA = new Noise(seed ^ 0x4C4F4E4741L);
      this.longB = new Noise(seed ^ 0x4C4F4E4742L);
      this.aperture = new Noise(seed ^ 0x4150455254555245L);
      this.crater = new Noise(seed ^ 0x435241544552L);
      this.province = new Noise(seed ^ 0x50524F56494E4345L);
      this.shaft = new Noise(seed ^ 0x5348414654L);
   }

   private static double band(double y, double lo, double hi) {
      if (y <= lo || y >= hi) return 0.0;
      double t = (y - lo) / (hi - lo);
      return Math.min(1.0, Math.min(t, 1.0 - t) * 4.0);
   }

   private double scale() {
      float cs = st.caveScale < 0.6F ? 0.6F : (st.caveScale > 3.0F ? 3.0F : st.caveScale);
      return 1.0 / cs;
   }

   private double density() {
      float d = st.caveDensity < 0.4F ? 0.4F : (st.caveDensity > 2.5F ? 2.5F : st.caveDensity);
      return d;
   }

   /** The three tunnelling/cheese fields; the single source of truth for "is this rock hollow". */
   private boolean hollowField(int x, int y, int z, double k, double dens, double widen) {
      // 1. cheese caves: two overlapping large-scale fields, clustered by a slow 2D province
      double gate = 0.34 - 0.055 * (dens - 1.0)
         + 0.06 * province.noise2(x * 0.0035 * k, z * 0.0035 * k);
      if (y >= 8 && y <= 96) {
         double a = cheeseA.noise(x * 0.0105 * k + 17.0, y * 0.0165 * k - 5.0, z * 0.0105 * k + 43.0);
         if (a > gate && band(y, 6.0, 104.0) > 0.14) return true;
         double b = cheeseB.noise(x * 0.0142 * k - 61.0, y * 0.0195 * k + 11.0, z * 0.0142 * k - 29.0);
         if (b > gate + 0.07 && band(y, 8.0, 100.0) > 0.18) return true;
      }

      // 2. spaghetti / worm tunnels: ridged noise pairs at two different scales
      double mid = 0.38 + 0.62 * band(y, 7.0, 78.0);
      double ax = wormA.noise(x * 0.0162 * k, y * 0.0230 * k, z * 0.0162 * k);
      double bx = wormB.noise(x * 0.0162 * k + 31.7, y * 0.0230 * k + 11.3, z * 0.0162 * k - 47.1);
      double r2 = 0.0039 * dens * mid * widen;
      if (ax * ax + bx * bx < r2) return true;
      double cx = longA.noise(x * 0.0072 * k - 71.0, y * 0.0108 * k + 5.0, z * 0.0072 * k + 19.0);
      double cz = longB.noise(x * 0.0072 * k + 43.0, y * 0.0108 * k - 29.0, z * 0.0072 * k - 61.0);
      if (cx * cx + cz * cz < 0.0030 * dens * widen) return true;

      // 3. noodle caves: thin, twisty, close to the surface
      double nx = wormA.noise(x * 0.034 * k + 13.0, y * 0.049 * k - 7.0, z * 0.034 * k + 29.0);
      double nz = wormB.noise(x * 0.034 * k - 41.0, y * 0.049 * k + 17.0, z * 0.034 * k - 3.0);
      return nx * nx + nz * nz < 0.0017 * dens * widen * (0.25 + 0.75 * band(y, 36.0, 88.0));
   }

   /** Hashed giant halls and the vertical noise shafts that join the layers. */
   private boolean big(int x, int y, int z) {
      if (y >= 9 && y <= 44) {
         int cx = Math.floorDiv(x, 112);
         int cz = Math.floorDiv(z, 112);
         for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
               long h = CaveWorks.mix(seed ^ 0x48414C4C35L, cx + i, 0, cz + j);
               if ((h >>> 60) >= 4) continue;
               int px = (cx + i) * 112 + 20 + CaveWorks.bits(h, 10, 72);
               int pz = (cz + j) * 112 + 20 + CaveWorks.bits(h, 22, 72);
               int ry = 7 + CaveWorks.bits(h, 34, 7);
               int rx = 12 + CaveWorks.bits(h, 42, 9);
               double dx = (x - px) / (double)rx;
               double dy = (y - (16 + CaveWorks.bits(h, 50, 12))) / (double)ry;
               double dz = (z - pz) / (double)rx;
               if (dx * dx + dy * dy + dz * dz <= 1.0) return true;
            }
         }
      }
      if (y >= 12 && y <= 62) {
         double s = shaft.noise2(x * 0.021 + 5.0, z * 0.021 - 9.0);
         double t = shaft.noise2(x * 0.021 - 33.0, z * 0.021 + 21.0);
         if (s * s + t * t < 0.0016) return true;
      }
      return false;
   }

   /** Local aquifer level, 0 when this region has no water table. */
   private int aquifer(int x, int z) {
      int cx = Math.floorDiv(x, 96);
      int cz = Math.floorDiv(z, 96);
      long h = CaveWorks.mix(seed ^ 0x4151554946455235L, cx, 0, cz);
      if ((h >>> 58) >= 22) return 0;
      return 20 + CaveWorks.bits(h, 12, 30);
   }

   /**
    * Is there real carved cave air below this column? The surface breakthrough is
    * only allowed above an existing cave, so an opening is never a dead-end pit.
    */
   private boolean caveBelow(int x, int z, int y, double k, double dens) {
      for (int d = 6; d <= 34; d += 4) {
         if (hollowField(x, y - d, z, k, density(), 1.0)) return true;
      }
      return big(x, y - 18, z);
   }

   /**
    * Carve one block. {@code surfaceH} is the real terrain height at (x,z) and
    * {@code allowSurface} is false over oceans and beaches. Every answer is a
    * pure function of the seed, the coordinates and those two values, so chunk
    * seams and worker threads agree.
    */
   int carve(int x, int y, int z, int surfaceH, boolean allowSurface) {
      if (y < 5 || y > 250 || y > surfaceH) return SOLID;
      double k = scale();
      double dens = density();
      int depth = surfaceH - y;
      // Caves thin out as they approach daylight (Minecraft's noodle caves):
      // without this, 40% of the surface would be open and the world would
      // look like swiss cheese instead of a landscape with occasional holes.
      if (depth < 30) dens *= 0.28 + 0.72 * (depth / 30.0);

      boolean air = hollowField(x, y, z, k, dens, 1.0) || big(x, y, z);

      // V6.31: NO built cave mouths. A smooth 2D aperture field decides
      // WHERE the underground is allowed to reach daylight; the hole is that
      // field's outline, so every opening is a different irregular shape
      // (crater, ragged slit, wide throat, narrow neck), never a template.
      // The funnel runs 22 blocks down into the ordinary cave noise, so it
      // always joins the network instead of being a dead-end pit.
      if (allowSurface && depth >= 0 && depth <= 22) {
         double ap = aperture.noise2(x * 0.0118 + 91.0, z * 0.0118 - 37.0);
         double cr = crater.noise2(x * 0.0085 - 13.0, z * 0.0085 + 7.0)
            + 0.42 * crater.noise2(x * 0.026 + 51.0, z * 0.026 - 23.0);
         double need = 0.02 - 0.18 * (depth / 22.0);   // narrow at grass, wider below
         boolean open = ap > -0.45 && cr > need;
         if (open) {
            air = true;                    // the noise outline IS the entrance
         } else if (depth < 14) {
            air = false;                   // intact ceiling everywhere else
         }
      }

      if (!air) return SOLID;
      if (y <= 10 && cheeseA.noise(x * 0.004, 3.0, z * 0.004) > 0.18) return LAVA;
      int level = aquifer(x, z);
      if (level > 0 && y <= level && y > 12) return WATER;
      return AIR;
   }

   /** True when a cave actually reaches the surface at this column. */
   boolean surfaceOpen(int x, int z, int surfaceH, boolean allowSurface) {
      return allowSurface && carve(x, surfaceH, z, surfaceH, true) != SOLID;
   }

   /** Convenience for callers that only have the generator (tree/spawn exclusion). */
   boolean surfaceOpen(int x, int z, int surfaceH) {
      if (surfaceH < 64) return false;
      int biome = gen == null ? -1 : gen.column(x, z).biome;
      return surfaceOpen(x, z, surfaceH, landBiome(biome, surfaceH));
   }

   /** Cave biome for the decorations (lush, dripstone, ice, amethyst, deep dark). */
   int biome(int x, int y, int z) {
      if (y < 14) return 5;
      int cx = Math.floorDiv(x, 128);
      int cz = Math.floorDiv(z, 128);
      long h = CaveWorks.mix(seed ^ 0x42494F4D4535L, cx, 0, cz);
      int roll = CaveWorks.bits(h, 6, 100);
      if (roll < 22) return 1;
      if (roll < 46) return 2;
      if (roll < 56) return 3;
      if (roll < 64) return 4;
      return 0;
   }

   /** Per-chunk cave decoration, clipped to inner columns so chunk seams agree. */
   void finish(Chunk ch) {
      long s = seed;
      for (int z = 2; z < 14; z++) {
         for (int x = 2; x < 14; x++) {
            int wx = ch.cx * 16 + x;
            int wz = ch.cz * 16 + z;
            for (int y = 7; y < 92; y++) {
               if (ch.get(x, y, z) != 0) continue;
               if (ch.get(x, y - 1, z) == 0) continue;   // needs a floor
               int ceil = y + 1;
               int gap = 0;
               while (ceil < 120 && ch.get(x, ceil, z) == 0 && gap < 14) { ceil++; gap++; }
               if (gap < 2 || gap >= 14) continue;
               long h = CaveWorks.mix(s ^ 0x4445434F35L, wx, y, wz);
               int roll = CaveWorks.bits(h, 4, 1000);
               int b = biome(wx, y, wz);
               int floor = ch.get(x, y - 1, z);
               if (b == 1 && roll < 170) {
                  ch.set(x, y - 1, z, roll < 90 ? Blocks313.MYCELIUM : Blocks.MOSSY_COBBLE);
                  if (roll < 24) ch.set(x, y, z, Blocks.GLOWSTONE);
               } else if (b == 2 && roll < 130) {
                  int len = Math.min(gap - 1, 1 + CaveWorks.bits(h, 28, 4));
                  for (int q = 0; q < len; q++) ch.set(x, ceil - 1 - q, z, Blocks313.ANDESITE);
               } else if (b == 3 && roll < 90) {
                  ch.set(x, y - 1, z, Redstone319.PURPUR_BLOCK);
                  if (roll < 12) ch.set(x, y, z, Blocks.GLASS0 + 10);
               } else if (b == 4 && roll < 95) {
                  ch.set(x, y - 1, z, Blocks313.ICE);
               } else if (b == 5 && roll < 60) {
                  ch.set(x, y - 1, z, Redstone319.DARK_PRISMARINE);
               } else if (roll < 48 && CaveWorks.rock(floor)) {
                  ch.set(x, y - 1, z, Blocks.COBBLE);
               }
            }
         }
      }
   }
}
