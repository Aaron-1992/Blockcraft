package bc;

import java.util.Random;

/**
 * Update 3.12: the terrain of the Nether. A cave world between a bedrock floor (y 0) and a bedrock ceiling (y 127): a sea of lava up
 * to y 31, netherrack landmasses and floating islands rising out of it, a thick netherrack ceiling with glowstone hanging from it,
 * soul sand and gravel on the shores, quartz ore in the walls, magma where the rock touches the lava - and nether fortresses.
 *
 * Everything is a function of the seed and the position only, so a chunk can be generated again at any time (and still matches the
 * saved neighbours).
 */
final class NetherGen extends Gen {
   static final int LAVA_Y = 32;
   static final int TOP = 127;
   private final Noise nA;
   private final Noise nB;
   private final Noise nC;
   private final Noise nLand;
   private final Noise nSoul;
   private final Noise nGravel;
   final NetherFortress fortress;

   NetherGen(long seed) {
      super(seed ^ 0x4E455448455221L);
      Random r = new Random(this.seed * 6364136223846793005L + 1442695040888963407L);
      this.nA = new Noise(r.nextLong());
      this.nB = new Noise(r.nextLong());
      this.nC = new Noise(r.nextLong());
      this.nLand = new Noise(r.nextLong());
      this.nSoul = new Noise(r.nextLong());
      this.nGravel = new Noise(r.nextLong());
      this.fortress = new NetherFortress(this.seed);
   }

   /** More than 0: rock. Open caverns in the middle, solid floor and ceiling, wide plateaus where {@code land} is high. */
   double density(int x, int y, int z, double land) {
      double big = this.nA.noise(x * 0.014, y * 0.030, z * 0.014);
      double mid = this.nB.noise(x * 0.040, y * 0.065, z * 0.040);
      double fine = this.nC.noise(x * 0.105, y * 0.13, z * 0.105);
      double v = big * 0.95 + mid * 0.60 + fine * 0.12;
      double low = smooth(40.0, 8.0, y);
      double high = smooth(88.0, 118.0, y);
      return v + low * 1.05 + high * 1.45 - 0.27 + land * 0.55 * smooth(80.0, 34.0, y);
   }

   double land(int x, int z) {
      return this.nLand.noise2(x * 0.010, z * 0.010);
   }

   @Override
   public Chunk generate(int cx, int cz) {
      Chunk c = new Chunk(cx, cz);
      short[] b = c.blocks;
      Random r = new Random(this.seed ^ (cx * 341873128712L + cz * 132897987541L));
      for (int lz = 0; lz < 16; lz++) {
         for (int lx = 0; lx < 16; lx++) {
            int x = cx * 16 + lx;
            int z = cz * 16 + lz;
            double land = this.land(x, z);
            for (int y = 1; y < TOP; y++) {
               if (this.density(x, y, z, land) > 0.0) {
                  b[Chunk.idx(lx, y, lz)] = (short)Blocks.NETHERRACK;
               } else if (y <= LAVA_Y) {
                  b[Chunk.idx(lx, y, lz)] = (short)Blocks.LAVA;
               }
            }

            // bedrock: a flat sheet at both ends and a rough layer inside it
            b[Chunk.idx(lx, 0, lz)] = (short)Blocks.BEDROCK;
            b[Chunk.idx(lx, TOP, lz)] = (short)Blocks.BEDROCK;
            for (int k = 1; k <= 4; k++) {
               if (r.nextInt(k + 1) == 0) {
                  b[Chunk.idx(lx, k, lz)] = (short)Blocks.BEDROCK;
               }

               if (r.nextInt(k + 1) == 0) {
                  b[Chunk.idx(lx, TOP - k, lz)] = (short)Blocks.BEDROCK;
               }
            }
         }
      }

      this.shores(c, r);
      this.ores(c, r);
      this.glowstone(c, r);
      this.fortress.apply(c);
      c.computeHeightmap();
      return c;
   }

   /** Soul sand and gravel on the floors near the lava sea, magma right where the rock touches the lava. */
   private void shores(Chunk c, Random r) {
      short[] b = c.blocks;
      for (int lz = 0; lz < 16; lz++) {
         for (int lx = 0; lx < 16; lx++) {
            int x = c.cx * 16 + lx;
            int z = c.cz * 16 + lz;
            double soul = this.nSoul.noise2(x * 0.075, z * 0.075);
            double gravel = this.nGravel.noise2(x * 0.075 + 40.0, z * 0.075 - 17.0);
            for (int y = 18; y < 52; y++) {
               if (b[Chunk.idx(lx, y, lz)] != Blocks.NETHERRACK || b[Chunk.idx(lx, y + 1, lz)] != 0) {
                  continue;
               }

               if (soul > 0.22 && y < 46) {
                  b[Chunk.idx(lx, y, lz)] = (short)Blocks.SOUL_SAND;
                  if (soul > 0.45 && b[Chunk.idx(lx, y - 1, lz)] == Blocks.NETHERRACK) {
                     b[Chunk.idx(lx, y - 1, lz)] = (short)Blocks.SOUL_SAND;
                  }
               } else if (gravel > 0.3) {
                  b[Chunk.idx(lx, y, lz)] = (short)Blocks.GRAVEL;
               } else if (y <= LAVA_Y + 2 && touchesLava(b, lx, y, lz) && r.nextInt(4) == 0) {
                  b[Chunk.idx(lx, y, lz)] = (short)Blocks.MAGMA;
               }
            }
         }
      }
   }

   private static boolean touchesLava(short[] b, int lx, int y, int lz) {
      return lx > 0 && Blocks.isLava(b[Chunk.idx(lx - 1, y, lz)]) || lx < 15 && Blocks.isLava(b[Chunk.idx(lx + 1, y, lz)])
         || lz > 0 && Blocks.isLava(b[Chunk.idx(lx, y, lz - 1)]) || lz < 15 && Blocks.isLava(b[Chunk.idx(lx, y, lz + 1)])
         || Blocks.isLava(b[Chunk.idx(lx, y + 1, lz)]);
   }

   /** Nether quartz in veins inside the rock (most of it where a cave wall is nearby, like in Minecraft). */
   private void ores(Chunk c, Random r) {
      short[] b = c.blocks;
      for (int t = 0; t < 22; t++) {
         int x = r.nextInt(16);
         int y = 8 + r.nextInt(110);
         int z = r.nextInt(16);
         int size = 3 + r.nextInt(5);
         for (int k = 0; k < size; k++) {
            if (b[Chunk.idx(x, y, z)] == Blocks.NETHERRACK) {
               b[Chunk.idx(x, y, z)] = (short)Blocks.QUARTZ_ORE;
            }

            x = Math.max(0, Math.min(15, x + r.nextInt(3) - 1));
            y = Math.max(5, Math.min(120, y + r.nextInt(3) - 1));
            z = Math.max(0, Math.min(15, z + r.nextInt(3) - 1));
         }
      }
   }

   /** Clusters of glowstone hanging from the roof of the caverns. */
   private void glowstone(Chunk c, Random r) {
      short[] b = c.blocks;
      for (int t = 0; t < 26; t++) {
         int x = r.nextInt(16);
         int z = r.nextInt(16);
         int y = 118;
         while (y > 70 && b[Chunk.idx(x, y, z)] != 0) {
            y--;
         }

         // y is now the lowest air cell under the roof: only a ceiling counts (rock right above it)
         if (y <= 70 || b[Chunk.idx(x, y + 1, z)] != Blocks.NETHERRACK) {
            continue;
         }

         int n = 5 + r.nextInt(9);
         for (int k = 0; k < n; k++) {
            int gx = x + r.nextInt(5) - 2;
            int gy = y - r.nextInt(4);
            int gz = z + r.nextInt(5) - 2;
            if (gx >= 0 && gx < 16 && gz >= 0 && gz < 16 && gy > 60 && (b[Chunk.idx(gx, gy, gz)] == 0 || b[Chunk.idx(gx, gy, gz)] == Blocks.NETHERRACK)
               && (gy == y || b[Chunk.idx(gx, gy + 1, gz)] == Blocks.GLOWSTONE || b[Chunk.idx(gx, gy + 1, gz)] == Blocks.NETHERRACK)) {
               b[Chunk.idx(gx, gy, gz)] = (short)Blocks.GLOWSTONE;
            }
         }
      }
   }

   @Override
   public Gen.Col column(int x, int z) {
      Gen.Col col = new Gen.Col();
      col.h = 64;
      col.biome = Gen.PLAINS;
      return col;
   }

   @Override
   boolean entrance(int x, int z) {
      return false;
   }

   /** The first spot you can stand on, looking down from the roof of the cavern - for tests and for lost players. */
   public int[] findFloor(int x, int z) {
      for (int y = 118; y > 1; y--) {
         int h = this.density(x, y, z, this.land(x, z)) > 0.0 ? 1 : 0;
         int above = this.density(x, y + 1, z, this.land(x, z)) > 0.0 ? 1 : 0;
         if (h == 1 && above == 0 && y > LAVA_Y) {
            return new int[]{x, y + 1, z};
         }
      }

      return null;
   }
}
