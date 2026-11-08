package bc;

/**
 * V6.34: caves generated the way Minecraft 1.18/1.20 does, written entirely
 * from scratch (no Mojang code or assets).
 *
 * <p>There is <b>no entrance system at all</b> - no apertures, no funnels, no
 * shafts dug down from the surface. A cave is one continuous 3D noise shape in
 * <b>absolute world Y</b>, carved from bedrock up to the terrain surface. An
 * "entrance" is simply the place where that shape intersects the ground: on a
 * hillside it is a sloping mouth, in a valley a wide bowl, in a plain a ragged
 * slit. The walls are the cave's own noise walls, so they are angled and
 * irregular, never a straight vertical hole.</p>
 *
 * <ul>
 *   <li><b>Worm / spaghetti tunnels</b> - ridged 3D noise pairs, sheared in Y so
 *       tunnels meander diagonally instead of standing vertically;</li>
 *   <li><b>Cheese caves</b> - two large 3D noise fields clustered into 2D
 *       provinces, forming connected multi-room caverns;</li>
 *   <li><b>Noodle caves</b> - thin near-surface tubes;</li>
 *   <li><b>Giant halls</b>, local <b>aquifers</b> and lava lakes at the bottom;</li>
 *   <li><b>Structures are part of the same air volume</b>: {@link CaveStructures7}
 *       contributes its rooms and corridors to this carver, so caves cut through
 *       mineshafts and dungeons naturally instead of being linked by a tunnel.</li>
 * </ul>
 *
 * <p>The vertical profile is based on absolute Y (dense around y=38, fading out
 * towards y=96), not on depth below the local surface. That is why mountains
 * have solid ground while valleys and plains get natural cave mouths - exactly
 * like Minecraft. Everything is pure in (seed, x, y, z, surfaceHeight).</p>
 */
final class Caves7 {
   static final int SOLID = 0;
   static final int AIR = 1;
   static final int LAVA = 2;
   static final int WATER = 3;

   /** Only dry land may open at the surface; ocean floors keep a thin seal. */
   static boolean landBiome(int biome, int h) {
      return h >= 64 && !Biomes.ocean(biome) && !Biomes.river(biome)
         && biome != Biomes.BEACH && biome != Biomes.SNOWY_BEACH
         && biome != Biomes.MUSHROOM;
   }

   final long seed;
   final WorldSettings st;
   final Gen gen;
   final CaveStructures7 structures;
   final boolean withStructures;
   final Noise wormA, wormB, longA, longB;
   final Noise cheeseA, cheeseB, province;

   Caves7(long seed, WorldSettings st, Gen gen) {
      this.seed = seed;
      this.st = st;
      this.gen = gen;
      this.withStructures = st.caveStructures;
      this.structures = new CaveStructures7(seed, this);
      this.wormA = new Noise(seed ^ 0x574F524D3731L);
      this.wormB = new Noise(seed ^ 0x574F524D3732L);
      this.longA = new Noise(seed ^ 0x4C4F4E473731L);
      this.longB = new Noise(seed ^ 0x4C4F4E473732L);
      this.cheeseA = new Noise(seed ^ 0x434845453731L);
      this.cheeseB = new Noise(seed ^ 0x434845453732L);
      this.province = new Noise(seed ^ 0x50524F563731L);
   }

   /** Absolute-Y density profile: caves live between y=8 and y=96, densest at 38. */
   static double profile(int y) {
      if (y < 8 || y > 96) return 0.0;
      double t = (y - 44.0) / 30.0;
      return Math.exp(-t * t);
   }

   private double scale() {
      float cs = st.caveScale < 0.6F ? 0.6F : (st.caveScale > 3.0F ? 3.0F : st.caveScale);
      return 1.0 / cs;
   }

   private double density() {
      float d = st.caveDensity < 0.4F ? 0.4F : (st.caveDensity > 2.5F ? 2.5F : st.caveDensity);
      return d;
   }

   /** Sheared world coordinates - tunnels lean as they rise, making angled mouths. */
   private static double shearX(int x, int y) { return x + y * 0.26; }
   private static double shearZ(int z, int y) { return z - y * 0.17; }

   /** Winding spaghetti / noodle tunnels at two scales. */
   private boolean worm(int x, int y, int z, double k, double dens, double p) {
      double sx = shearX(x, y), sz = shearZ(z, y);
      double ax = wormA.noise(sx * 0.0190 * k, y * 0.0250 * k, sz * 0.0190 * k);
      double bx = wormB.noise(sx * 0.0190 * k + 31.7, y * 0.0250 * k - 11.3, sz * 0.0190 * k + 47.1);
      double wsp = 0.18 + 0.82 * p;   // tunnels stay wide enough near daylight
      if (ax * ax + bx * bx < 0.0075 * dens * wsp) return true;
      double cx = longA.noise(sx * 0.0085 * k - 71.0, y * 0.0120 * k + 5.0, sz * 0.0085 * k + 19.0);
      double cz = longB.noise(sx * 0.0085 * k + 43.0, y * 0.0120 * k - 29.0, sz * 0.0085 * k - 61.0);
      return cx * cx + cz * cz < 0.0050 * dens * wsp;
   }

   /** Big cheese caverns, clustered by a slow 2D province noise. */
   private boolean cheese(int x, int y, int z, double k, double dens, double p) {
      if (y < 8 || y > 70) return false;   // cheese caverns stay below the high ground
      // Above ~y=55 the gate rises sharply, so a cavern never punches a
      // giant hole in a plain; it only opens where low terrain cuts into it.
      double sx = shearX(x, y) * 0.55 + x * 0.45;
      double sz = shearZ(z, y) * 0.55 + z * 0.45;
      double gate = 0.47 - 0.045 * (dens - 1.0)
         + 0.040 * province.noise2(x * 0.0038 * k, z * 0.0038 * k) + 0.30 * (1.0 - p);
      double q = cheeseA.noise(sx * 0.0108 * k + 91.0, y * 0.0160 * k - 17.0, sz * 0.0108 * k + 53.0);
      if (q > gate && p > 0.45) return true;
      double q2 = cheeseB.noise(sx * 0.0140 * k - 33.0, y * 0.0190 * k + 7.0, sz * 0.0140 * k - 91.0);
      return q2 > gate + 0.055 && p > 0.50;
   }

   /** Rare huge deep halls. */
   private boolean hall(int x, int y, int z) {
      if (y < 9 || y > 40) return false;
      int cx = Math.floorDiv(x, 128), cz = Math.floorDiv(z, 128);
      long h = CaveWorks.mix(seed ^ 0x48414C4C3734L, cx, 0, cz);
      if ((h >>> 60) >= 2) return false;
      int px = cx * 128 + 24 + CaveWorks.bits(h, 10, 80);
      int pz = cz * 128 + 24 + CaveWorks.bits(h, 22, 80);
      int rx = 10 + CaveWorks.bits(h, 34, 7);
      int ry = 5 + CaveWorks.bits(h, 42, 5);
      int py = 14 + CaveWorks.bits(h, 48, 10);
      double dx = (x - px) / (double)rx, dy = (y - py) / (double)ry, dz = (z - pz) / (double)rx;
      return dx * dx + dy * dy + dz * dz <= 1.0;
   }

   private int aquifer(int x, int z) {
      int cx = Math.floorDiv(x, 96), cz = Math.floorDiv(z, 96);
      long h = CaveWorks.mix(seed ^ 0x415155493734L, cx, 0, cz);
      if ((h >>> 58) >= 12) return 0;
      return 22 + CaveWorks.bits(h, 12, 20);
   }

   /**
    * Carve one block. There is no crust, no aperture and no entrance shaft:
    * the same 3D field runs from bedrock to the grass, so where it meets the
    * terrain it simply opens. Over oceans a thin seafloor seal is kept.
    */
   static final int C_WORM = 1, C_CHEESE = 2, C_HALL = 4, C_STRUCT = 8;

   /** Which parts of the ONE cave volume are present here (also used by tests). */
   int cause(int x, int y, int z, int surfaceH) {
      double k = scale();
      double dens = density();
      double p = profile(y);
      int c = 0;
      if (p > 0.0) {
         if (worm(x, y, z, k, dens, p)) c |= C_WORM;
         if (cheese(x, y, z, k, dens, p)) c |= C_CHEESE;
         if (hall(x, y, z)) c |= C_HALL;
      }
      // Structures are carved by the SAME function, so caves intersect them.
      if (withStructures && y <= surfaceH - 6 && this.structures.air(x, y, z, surfaceH)) c |= C_STRUCT;
      return c;
   }

   int carve(int x, int y, int z, int surfaceH, boolean allowSurface) {
      if (y < 5 || y > 250 || y > surfaceH) return SOLID;
      if (!allowSurface && y > surfaceH - 4) return SOLID;
      boolean air = cause(x, y, z, surfaceH) != 0;
      if (!air) return SOLID;
      if (y <= 10 && cheeseA.noise(x * 0.004, 3.0, z * 0.004) > 0.30) return LAVA;
      int level = aquifer(x, z);
      if (level > 0 && y <= level && y > 12) return WATER;
      return AIR;
   }

   boolean surfaceOpen(int x, int z, int surfaceH, boolean allowSurface) {
      return allowSurface && carve(x, surfaceH, z, surfaceH, allowSurface) != SOLID;
   }

   boolean surfaceOpen(int x, int z, int surfaceH) {
      if (surfaceH < 64) return false;
      int biome = gen == null ? -1 : gen.column(x, z).biome;
      return surfaceOpen(x, z, surfaceH, landBiome(biome, surfaceH));
   }

   /** Sparse cave decoration plus the structure blocks. */
   void finish(Chunk ch) {
      for (int z = 2; z < 14; z++) {
         for (int x = 2; x < 14; x++) {
            int wx = ch.cx * 16 + x, wz = ch.cz * 16 + z;
            for (int y = 8; y < 84; y++) {
               if (ch.get(x, y, z) != 0) continue;
               int floor = ch.get(x, y - 1, z);
               if (floor == 0 || !CaveWorks.rock(floor)) continue;
               long h = CaveWorks.mix(seed ^ 0x4445434F3734L, wx, y, wz);
               int roll = CaveWorks.bits(h, 4, 1000);
               if (roll < 24) {
                  ch.set(x, y - 1, z, roll < 8 ? Blocks.MOSSY_COBBLE : Blocks.COBBLE);
               } else if (roll < 34) {
                  int ceil = y + 1, gap = 0;
                  while (ceil < 100 && ch.get(x, ceil, z) == 0 && gap < 10) { ceil++; gap++; }
                  if (gap >= 3 && gap < 10) ch.set(x, ceil - 1, z, Blocks313.ANDESITE);
               } else if (roll < 38) {
                  ch.set(x, y, z, Blocks.GLOWSTONE);
               }
            }
         }
      }
      // V6.34: place the structure walls/floors/loot into the air the carver made.
      if (withStructures) this.structures.finish(ch);
   }
}
