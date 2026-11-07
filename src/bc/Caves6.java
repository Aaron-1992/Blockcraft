package bc;

/**
 * V6.32: caves rebuilt to Minecraft 1.18/1.20 <b>density and shape</b>, written
 * entirely from scratch (no Mojang code or assets).
 *
 * <p>V6.31's carver hollowed out 19% of the ground within 20 blocks of the
 * surface and opened 10% of all surface columns, so the world was "basically
 * non existent". This carver keeps the ground solid:</p>
 * <ul>
 *   <li><b>An intact surface crust</b>: nothing at all is carved in the top 12
 *       blocks except at a rare, real cave entrance.</li>
 *   <li><b>Spaghetti / noodle tunnels</b>: thin ridged-noise worm pairs, most
 *       common between y=20 and y=60, thinning out near daylight.</li>
 *   <li><b>Cheese caves</b>: sparse large 3D-noise chambers clustered into
 *       provinces, y=8..72, with a high gate so they are rooms, not a void.</li>
 *   <li><b>Rare giant halls</b> and local <b>aquifers</b> / lava lakes at the
 *       bottom, like the 1.18 aquifer system.</li>
 *   <li><b>Entrances</b>: a strict 2D aperture gate picks a tiny fraction of
 *       dry land; a funnel that widens downward connects it to a cave that
 *       really exists below. Every opening is a different noise outline.</li>
 * </ul>
 *
 * <p>Everything is pure in (seed, x, y, z, surfaceHeight, allowSurface), so
 * chunk seams and worker threads agree. Old saves keep their own caveVer.</p>
 */
final class Caves6 {
   static final int SOLID = 0;
   static final int AIR = 1;
   static final int LAVA = 2;
   static final int WATER = 3;

   /** Only dry land above the sea may open; oceans and beaches stay sealed. */
   static boolean landBiome(int biome, int h) {
      return h >= 64 && !Biomes.ocean(biome) && !Biomes.river(biome)
         && biome != Biomes.BEACH && biome != Biomes.SNOWY_BEACH
         && biome != Biomes.MUSHROOM;
   }

   final long seed;
   final WorldSettings st;
   final Gen gen;
   final Noise wormA, wormB, longA, longB;
   final Noise cheeseA, cheeseB, province;
   final Noise aperture, crater;

   Caves6(long seed, WorldSettings st, Gen gen) {
      this.seed = seed;
      this.st = st;
      this.gen = gen;
      this.wormA = new Noise(seed ^ 0x574F524D3631L);
      this.wormB = new Noise(seed ^ 0x574F524D3632L);
      this.longA = new Noise(seed ^ 0x4C4F4E473631L);
      this.longB = new Noise(seed ^ 0x4C4F4E473632L);
      this.cheeseA = new Noise(seed ^ 0x434845453631L);
      this.cheeseB = new Noise(seed ^ 0x434845453632L);
      this.province = new Noise(seed ^ 0x50524F563631L);
      this.aperture = new Noise(seed ^ 0x415045523631L);
      this.crater = new Noise(seed ^ 0x435241543631L);
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

   /** Thin winding tunnels. This is the backbone of the cave system. */
   private boolean worm(int x, int y, int z, double k, double dens) {
      double mid = 0.30 + 0.70 * band(y, 10.0, 68.0);
      double ax = wormA.noise(x * 0.0190 * k, y * 0.0270 * k, z * 0.0190 * k);
      double bx = wormB.noise(x * 0.0190 * k + 31.7, y * 0.0270 * k - 11.3, z * 0.0190 * k + 47.1);
      double r2 = 0.0110 * dens * mid;
      if (ax * ax + bx * bx < r2) return true;
      // the long, very thin "spaghetti" scale joins the pockets into systems
      double cx = longA.noise(x * 0.0088 * k - 71.0, y * 0.0125 * k + 5.0, z * 0.0088 * k + 19.0);
      double cz = longB.noise(x * 0.0088 * k + 43.0, y * 0.0125 * k - 29.0, z * 0.0088 * k - 61.0);
      return cx * cx + cz * cz < 0.0070 * dens * (0.4 + 0.6 * band(y, 8.0, 76.0));
   }

   /** Sparse cheese chambers, clustered in slow 2D provinces, never a global void. */
   private boolean cheese(int x, int y, int z, double k, double dens) {
      if (y < 8 || y > 72) return false;
      double gate = 0.455 - 0.045 * (dens - 1.0)
         + 0.035 * province.noise2(x * 0.0038 * k, z * 0.0038 * k);
      double q = cheeseA.noise(x * 0.0108 * k + 91.0, y * 0.0160 * k - 17.0, z * 0.0108 * k + 53.0);
      if (q > gate && band(y, 6.0, 80.0) > 0.22) return true;
      double q2 = cheeseB.noise(x * 0.0140 * k - 33.0, y * 0.0190 * k + 7.0, z * 0.0140 * k - 91.0);
      return q2 > gate + 0.055 && band(y, 10.0, 76.0) > 0.26;
   }

   /** Very rare, genuinely huge deep halls. */
   private boolean hall(int x, int y, int z) {
      if (y < 9 || y > 40) return false;
      int cx = Math.floorDiv(x, 128), cz = Math.floorDiv(z, 128);
      long h = CaveWorks.mix(seed ^ 0x48414C4C3632L, cx, 0, cz);
      if ((h >>> 60) >= 2) return false;
      int px = cx * 128 + 24 + CaveWorks.bits(h, 10, 80);
      int pz = cz * 128 + 24 + CaveWorks.bits(h, 22, 80);
      int rx = 10 + CaveWorks.bits(h, 34, 7);
      int ry = 5 + CaveWorks.bits(h, 42, 5);
      int py = 14 + CaveWorks.bits(h, 48, 10);
      double dx = (x - px) / (double)rx, dy = (y - py) / (double)ry, dz = (z - pz) / (double)rx;
      return dx * dx + dy * dy + dz * dz <= 1.0;
   }

   private boolean underground(int x, int y, int z, double k, double dens) {
      return worm(x, y, z, k, dens) || cheese(x, y, z, k, dens) || hall(x, y, z);
   }

   /** Is there a real cave below this column? Measured from the SURFACE, so the
    * answer is identical for every block of the column and the entrance shaft
    * is continuous instead of a row of disconnected holes. */
   private boolean caveBelow(int x, int z, int surfaceH, double k) {
      double d = density() * 1.35;
      for (int depth = 13; depth <= 34; depth += 4) {
         for (int ox = -3; ox <= 3; ox += 3) {
            for (int oz = -3; oz <= 3; oz += 3) {
               if (underground(x + ox, surfaceH - depth, z + oz, k, d)) return true;
            }
         }
      }
      return false;
   }

   private int aquifer(int x, int z) {
      int cx = Math.floorDiv(x, 96), cz = Math.floorDiv(z, 96);
      long h = CaveWorks.mix(seed ^ 0x415155493632L, cx, 0, cz);
      if ((h >>> 58) >= 12) return 0;    // most regions stay dry, like 1.18
      return 22 + CaveWorks.bits(h, 12, 20);   // y = 22..41
   }

   /**
    * The whole carver. {@code surfaceH} is the real terrain height and
    * {@code allowSurface} is false over oceans/beaches.
    */
   int carve(int x, int y, int z, int surfaceH, boolean allowSurface) {
      if (y < 5 || y > 250 || y > surfaceH) return SOLID;
      int depth = surfaceH - y;
      double k = scale();
      double dens = density();
      boolean air = false;

      // 1. A solid 12-block crust. This is what keeps the world intact.
      if (depth >= 12) {
         double taper = depth < 28 ? 0.35 + 0.65 * (depth - 12) / 16.0 : 1.0;
         air = underground(x, y, z, k, dens * taper);
      }

      // 2. Rare natural entrances: the cave noise breaks through the crust.
      // The gate is deliberately strict so only a fraction of a percent of
      // dry land opens, and the outline is pure noise (never a template).
      if (allowSurface && depth <= 26) {
         double ap = aperture.noise2(x * 0.0105 + 91.0, z * 0.0105 - 37.0);
         double cr = crater.noise2(x * 0.0068 - 13.0, z * 0.0068 + 7.0)
            + 0.28 * crater.noise2(x * 0.021 + 51.0, z * 0.021 - 23.0);
         double need = -0.45 - 0.42 * (depth / 26.0);   // narrow at grass, wide 26 blocks down
         if (ap > -0.10 && cr > need && caveBelow(x, z, surfaceH, k)) {
            air = true;   // a continuous funnel from daylight into the cave below
         }
      }

      if (!air) return SOLID;
      if (y <= 10 && cheeseA.noise(x * 0.004, 3.0, z * 0.004) > 0.30) return LAVA;
      int level = aquifer(x, z);
      if (level > 0 && y <= level && y > 12) return WATER;
      return AIR;
   }

   boolean surfaceOpen(int x, int z, int surfaceH, boolean allowSurface) {
      return allowSurface && carve(x, surfaceH, z, surfaceH, true) != SOLID;
   }

   boolean surfaceOpen(int x, int z, int surfaceH) {
      if (surfaceH < 64) return false;
      int biome = gen == null ? -1 : gen.column(x, z).biome;
      return surfaceOpen(x, z, surfaceH, landBiome(biome, surfaceH));
   }

   /** Sparse cave decoration: a floor hint, dripstone, glow, ore. Never hollows more rock. */
   void finish(Chunk ch) {
      for (int z = 2; z < 14; z++) {
         for (int x = 2; x < 14; x++) {
            int wx = ch.cx * 16 + x, wz = ch.cz * 16 + z;
            for (int y = 8; y < 72; y++) {
               if (ch.get(x, y, z) != 0) continue;
               int floor = ch.get(x, y - 1, z);
               if (floor == 0 || !CaveWorks.rock(floor)) continue;
               long h = CaveWorks.mix(seed ^ 0x4445434F3632L, wx, y, wz);
               int roll = CaveWorks.bits(h, 4, 1000);
               if (roll < 26) {
                  ch.set(x, y - 1, z, roll < 8 ? Blocks.MOSSY_COBBLE : Blocks.COBBLE);
               } else if (roll < 40) {
                  int ceil = y + 1;
                  int gap = 0;
                  while (ceil < 90 && ch.get(x, ceil, z) == 0 && gap < 10) { ceil++; gap++; }
                  if (gap >= 3 && gap < 10) ch.set(x, ceil - 1, z, Blocks313.ANDESITE);
               } else if (roll < 45) {
                  ch.set(x, y, z, Blocks.GLOWSTONE);
               }
            }
         }
      }
   }
}
