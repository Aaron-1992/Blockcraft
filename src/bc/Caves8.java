package bc;

/**
 * V6.35: a 384-block-deep world whose caves get progressively wilder the
 * further down you go. Entirely original - no Mojang code or assets.
 *
 * <p>There is still <b>no entrance machinery</b> (V6.34's rule is kept): a cave
 * mouth is simply where the 3D cave volume intersects the terrain. What is new
 * is a <b>depth gradient</b> measured from sea level down to bedrock:</p>
 *
 * <ul>
 *   <li><b>Upper / daylight</b>: calm sheared spaghetti tunnels, occasional
 *       natural mouths, the ground stays intact;</li>
 *   <li><b>Middle</b>: wider worm networks, clustered cheese caverns, water
 *       aquifers and the first big halls;</li>
 *   <li><b>Deep</b>: a second "mega cheese" field opens cathedral-sized
 *       chambers, an extra cross-sheared maze of tunnels, and structures get
 *       bigger and more numerous;</li>
 *   <li><b>Abyss</b>: huge halls, lava seas, crystal fields and a dense maze
 *       where it is genuinely easy to get completely lost.</li>
 * </ul>
 *
 * <p>Everything is a pure function of (seed, x, y, z, surfaceHeight) plus the
 * world's {@code depth} setting, so chunk seams and worker threads agree and
 * old saves (depth 0, caveVer &lt; 8) never use this carver.</p>
 */
final class Caves8 {
   static final int SOLID = 0;
   static final int AIR = 1;
   static final int LAVA = 2;
   static final int WATER = 3;

   static final int C_WORM = 1, C_CHEESE = 2, C_HALL = 4, C_STRUCT = 8, C_MEGA = 16, C_MAZE = 32;

   /** Only dry land above THIS world's sea can open; oceans keep a thin seal. */
   static boolean landBiome(int biome, int h, int sea) {
      return h >= sea && !Biomes.ocean(biome) && !Biomes.river(biome)
         && biome != Biomes.BEACH && biome != Biomes.SNOWY_BEACH
         && biome != Biomes.MUSHROOM;
   }

   final long seed;
   final WorldSettings st;
   final Gen gen;
   final CaveStructures8 structures;
   final boolean withStructures;
   final Noise wormA, wormB, longA, longB;
   final Noise cheeseA, cheeseB, province;
   final Noise megaA, megaB, mazeA, mazeB;

   Caves8(long seed, WorldSettings st, Gen gen) {
      this.seed = seed;
      this.st = st;
      this.gen = gen;
      this.withStructures = st.caveStructures;
      this.structures = new CaveStructures8(seed, this);
      this.wormA = new Noise(seed ^ 0x573841574F314CL);
      this.wormB = new Noise(seed ^ 0x573841574F324CL);
      this.longA = new Noise(seed ^ 0x4C38314C4F4E47L);
      this.longB = new Noise(seed ^ 0x4C38324C4F4E47L);
      this.cheeseA = new Noise(seed ^ 0x43483843484531L);
      this.cheeseB = new Noise(seed ^ 0x43483843484532L);
      this.province = new Noise(seed ^ 0x50523850524F56L);
      this.megaA = new Noise(seed ^ 0x4D454741384131L);
      this.megaB = new Noise(seed ^ 0x4D454741384132L);
      this.mazeA = new Noise(seed ^ 0x4D415A45384131L);
      this.mazeB = new Noise(seed ^ 0x4D415A45384132L);
   }

   int sea() { return 63 + this.st.depth; }

   /** 0 at/above the surface, 1 at bedrock. The single knob that makes depth matter. */
   static double crazy(int y, int sea) {
      double d = (sea + 24.0 - y) / (sea + 24.0);
      return d < 0.0 ? 0.0 : (d > 1.0 ? 1.0 : d);
   }

   private double scale() {
      float cs = st.caveScale < 0.6F ? 0.6F : (st.caveScale > 3.0F ? 3.0F : st.caveScale);
      return 1.0 / cs;
   }

   private double density() {
      float d = st.caveDensity < 0.4F ? 0.4F : (st.caveDensity > 2.5F ? 2.5F : st.caveDensity);
      return d;
   }

   private static double shearX(int x, int y) { return x + y * 0.26; }
   private static double shearZ(int z, int y) { return z - y * 0.17; }

   /** Sheared spaghetti / noodle tunnels. Radius grows sharply with depth. */
   private boolean worm(int x, int y, int z, double k, double dens, double d) {
      double sx = shearX(x, y), sz = shearZ(z, y);
      double w = 0.55 + 1.8 * d * d;
      double ax = wormA.noise(sx * 0.0190 * k, y * 0.0250 * k, sz * 0.0190 * k);
      double bx = wormB.noise(sx * 0.0190 * k + 31.7, y * 0.0250 * k - 11.3, sz * 0.0190 * k + 47.1);
      if (ax * ax + bx * bx < 0.0075 * dens * w) return true;
      double cx = longA.noise(sx * 0.0085 * k - 71.0, y * 0.0120 * k + 5.0, sz * 0.0085 * k + 19.0);
      double cz = longB.noise(sx * 0.0085 * k + 43.0, y * 0.0120 * k - 29.0, sz * 0.0085 * k - 61.0);
      return cx * cx + cz * cz < 0.0050 * dens * w;
   }

   /** Clustered cheese caverns; the gate drops a long way as you descend. */
   private boolean cheese(int x, int y, int z, double k, double dens, double d) {
      if (y < 8) return false;
      double sx = shearX(x, y) * 0.55 + x * 0.45;
      double sz = shearZ(z, y) * 0.55 + z * 0.45;
      // A strong daylight taper: big caverns never punch a hole in a plain.
      // They only exist once you are properly underground.
      double day = 0.26 * (1.0 - Math.min(1.0, d / 0.25));
      double gate = 0.55 - 0.14 * d - 0.045 * (dens - 1.0) + day
         + 0.040 * province.noise2(x * 0.0038 * k, z * 0.0038 * k);
      double q = cheeseA.noise(sx * 0.0108 * k + 91.0, y * 0.0160 * k - 17.0, sz * 0.0108 * k + 53.0);
      if (q > gate) return true;
      double q2 = cheeseB.noise(sx * 0.0140 * k - 33.0, y * 0.0190 * k + 7.0, sz * 0.0140 * k - 91.0);
      return q2 > gate + 0.055;
   }

   /** Cathedral-sized chambers: only in the deep half, never near the surface. */
   private boolean mega(int x, int y, int z, double k, double d) {
      if (d < 0.42) return false;
      double m = megaA.noise(x * 0.0062 * k + 12.0, y * 0.0085 * k - 41.0, z * 0.0062 * k + 77.0);
      if (m > 0.55 - 0.10 * d) return true;
      double m2 = megaB.noise(x * 0.0045 * k - 88.0, y * 0.0060 * k + 21.0, z * 0.0045 * k - 5.0);
      return m2 > 0.57 - 0.10 * d;
   }

   /** A second, cross-sheared tunnel maze in the abyss. */
   private boolean maze(int x, int y, int z, double k, double d) {
      if (d < 0.60) return false;
      double sx = x - y * 0.31, sz = z + y * 0.22;
      double a = mazeA.noise(sx * 0.0230 * k, y * 0.0300 * k, sz * 0.0230 * k);
      double b = mazeB.noise(sx * 0.0230 * k - 19.0, y * 0.0300 * k + 63.0, sz * 0.0230 * k + 7.0);
      return a * a + b * b < 0.0025 * (0.30 + d);
   }

   /** Rare giant halls; deeper ones are far bigger and more common. */
   private boolean hall(int x, int y, int z, double d) {
      if (y < 9 || y > 150) return false;
      int cx = Math.floorDiv(x, 128), cz = Math.floorDiv(z, 128);
      long h = CaveWorks.mix(seed ^ 0x48414C4C3835L, cx, 0, cz);
      int limit = d > 0.55 ? 4 : 2;
      if ((h >>> 60) >= limit) return false;
      int px = cx * 128 + 16 + CaveWorks.bits(h, 10, 96);
      int pz = cz * 128 + 16 + CaveWorks.bits(h, 22, 96);
      int rx = 10 + CaveWorks.bits(h, 34, 8) + (int)(12 * d);
      int ry = 5 + CaveWorks.bits(h, 42, 5) + (int)(6 * d);
      int py = 14 + CaveWorks.bits(h, 48, 12);
      double dx = (x - px) / (double)rx, dy = (y - py) / (double)ry, dz = (z - pz) / (double)rx;
      return dx * dx + dy * dy + dz * dz <= 1.0;
   }

   private int aquifer(int x, int z) {
      int cx = Math.floorDiv(x, 96), cz = Math.floorDiv(z, 96);
      long h = CaveWorks.mix(seed ^ 0x415155493835L, cx, 0, cz);
      if ((h >>> 58) >= 5) return 0;
      return 16 + CaveWorks.bits(h, 12, 24);
   }

   /** Which parts of the one cave volume are present here (also used by the tests). */
   int cause(int x, int y, int z, int surfaceH) {
      double k = scale(), dens = density(), d = crazy(y, sea());
      int c = 0;
      if (worm(x, y, z, k, dens, d)) c |= C_WORM;
      if (cheese(x, y, z, k, dens, d)) c |= C_CHEESE;
      if (hall(x, y, z, d)) c |= C_HALL;
      if (mega(x, y, z, k, d)) c |= C_MEGA;
      if (maze(x, y, z, k, d)) c |= C_MAZE;
      // Structures are carved by the SAME function, so caves cut through them.
      if (withStructures && y <= surfaceH - 6 && this.structures.air(x, y, z, surfaceH)) c |= C_STRUCT;
      return c;
   }

   int carve(int x, int y, int z, int surfaceH, boolean allowSurface) {
      if (y < 5 || y >= Chunk.H || y > surfaceH) return SOLID;
      if (!allowSurface && y > surfaceH - 4) return SOLID;
      int c = cause(x, y, z, surfaceH);
      if (c == 0) return SOLID;
      double d = crazy(y, sea());
      if (y <= 12 && megaA.noise(x * 0.004 + 3.0, 3.0, z * 0.004 - 7.0) > 0.30 - 0.15 * d) return LAVA;
      int level = aquifer(x, z);
      if (level > 0 && y <= level && y > 12) return WATER;
      return AIR;
   }

   boolean surfaceOpen(int x, int z, int surfaceH, boolean allowSurface) {
      return allowSurface && carve(x, surfaceH, z, surfaceH, allowSurface) != SOLID;
   }

   boolean surfaceOpen(int x, int z, int surfaceH) {
      int sea = sea();
      if (surfaceH < sea) return false;
      int biome = gen == null ? -1 : gen.column(x, z).biome;
      return surfaceOpen(x, z, surfaceH, landBiome(biome, surfaceH, sea));
   }

   /** Sparse decorations (richer with depth) plus the integrated structures. */
   void finish(Chunk ch) {
      for (int z = 2; z < 14; z++) {
         for (int x = 2; x < 14; x++) {
            int wx = ch.cx * 16 + x, wz = ch.cz * 16 + z;
            for (int y = 8; y < Chunk.H - 8; y++) {
               if (ch.get(x, y, z) != 0) continue;
               int floor = ch.get(x, y - 1, z);
               if (floor == 0 || !CaveWorks.rock(floor)) continue;
               long h = CaveWorks.mix(seed ^ 0x4445434F3835L, wx, y, wz);
               int roll = CaveWorks.bits(h, 4, 1000);
               double d = crazy(y, sea());
               if (roll < 22) {
                  ch.set(x, y - 1, z, roll < 8 ? Blocks.MOSSY_COBBLE : Blocks.COBBLE);
               } else if (roll < 22 + (int)(55 * d)) {
                  // deeper caves glow: amethyst, glowstone and crystal clusters
                  int id = roll % 3 == 0 ? Blocks.GLOWSTONE
                     : (roll % 3 == 1 ? Blocks.GLASS0 + 10 : Blocks313.ANDESITE);
                  ch.set(x, y - 1, z, id);
               } else if (roll < 38) {
                  int ceil = y + 1, gap = 0;
                  while (ceil < Chunk.H - 1 && ch.get(x, ceil, z) == 0 && gap < 12) { ceil++; gap++; }
                  if (gap >= 3 && gap < 12) ch.set(x, ceil - 1, z, Blocks313.ANDESITE);
               }
            }
         }
      }
      if (withStructures) this.structures.finish(ch);
   }
}
