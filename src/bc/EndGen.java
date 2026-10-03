package bc;

import java.util.Random;

/**
 * Update 3.12: the End. One island of end stone floating in the void (the same island in every world, so the fight is the same
 * everywhere): about 110 blocks across, flat on top at y 64 and tapering to a point underneath, ten obsidian pillars on a circle round
 * the middle (76..103 blocks high, each with an End crystal on top) and the bedrock fountain in the very middle where the exit portal
 * appears when the dragon is dead. The obsidian platform you arrive on lies on the east side of the island.
 */
final class EndGen extends Gen {
   /** The top block of the island's middle. */
   static final int TOP = 64;
   /** The arrival platform: obsidian, 5 x 5, its centre. You stand one block above. */
   static final int PLATFORM_X = 62;
   static final int PLATFORM_Y = 63;
   static final int PLATFORM_Z = 0;
   static final int PILLAR_RING = 42;
   /** {x, z, y of the bedrock block on top, radius} of the ten pillars. */
   static final int[][] PILLARS = pillars();
   private static final Noise N_RIM = new Noise(0x454E4401L);
   private static final Noise N_TOP = new Noise(0x454E4402L);
   private static final Noise N_UNDER = new Noise(0x454E4403L);

   EndGen(long seed) {
      super(seed ^ 0x454E44L);
   }

   private static int[][] pillars() {
      Integer[] order = new Integer[10];
      for (int i = 0; i < 10; i++) {
         order[i] = i;
      }

      java.util.Collections.shuffle(java.util.Arrays.asList(order), new Random(10L));
      int[][] p = new int[10][];
      for (int i = 0; i < 10; i++) {
         double a = Math.toRadians(i * 36.0);
         int k = order[i];
         p[i] = new int[]{(int)Math.round(Math.cos(a) * PILLAR_RING), (int)Math.round(Math.sin(a) * PILLAR_RING), 76 + 3 * k, 2 + k / 3};
      }

      return p;
   }

   /** How far the island reaches from the middle in the direction of (x, z). */
   static double reach(double x, double z) {
      return 56.0 + 4.5 * N_RIM.noise2(x * 0.03 + 11.0, z * 0.03) + 2.0 * N_RIM.noise2(x * 0.09, z * 0.09 + 5.0);
   }

   /** The height of the top block of the island's ground at (x, z), or -1 where there is no ground (the void). */
   static int groundTop(int x, int z) {
      double d = Math.sqrt((double)x * x + (double)z * z);
      double r = d / reach(x, z);
      if (r >= 1.0) {
         return -1;
      }

      if (d < 14.0) {
         return TOP;
      }

      int top = TOP - (int)Math.round(Math.max(0.0, r - 0.78) * 14.0) + (int)Math.round(N_TOP.noise2(x * 0.08, z * 0.08) * 1.4 * Math.min(1.0, (d - 14.0) / 10.0));
      return Math.min(TOP + 1, top);
   }

   /** The height of the lowest block of the ground at (x, z). */
   static int groundBottom(int x, int z) {
      double d = Math.sqrt((double)x * x + (double)z * z);
      double r = Math.min(1.0, d / reach(x, z));
      int thick = 2 + (int)Math.round(31.0 * Math.sqrt(Math.max(0.0, 1.0 - r * r)) + 3.5 * N_UNDER.noise2(x * 0.11, z * 0.11));
      return Math.min(groundTop(x, z), TOP - thick);
   }

   @Override
   public Chunk generate(int cx, int cz) {
      Chunk c = new Chunk(cx, cz);
      short[] b = c.blocks;
      for (int lz = 0; lz < 16; lz++) {
         for (int lx = 0; lx < 16; lx++) {
            int x = cx * 16 + lx;
            int z = cz * 16 + lz;
            int top = groundTop(x, z);
            if (top < 0) {
               continue;
            }

            int bottom = Math.max(1, groundBottom(x, z));
            for (int y = bottom; y <= top; y++) {
               b[Chunk.idx(lx, y, lz)] = (short)Blocks.END_STONE;
            }
         }
      }

      for (int[] p : PILLARS) {
         pillar(c, p);
      }

      fountain(c);
      platform(c);
      c.computeHeightmap();
      return c;
   }

   private static void pillar(Chunk c, int[] p) {
      int x0 = c.cx * 16;
      int z0 = c.cz * 16;
      int rad = p[3];
      if (p[0] + rad < x0 || p[0] - rad > x0 + 15 || p[1] + rad < z0 || p[1] - rad > z0 + 15) {
         return;
      }

      for (int dz = -rad; dz <= rad; dz++) {
         for (int dx = -rad; dx <= rad; dx++) {
            if (dx * dx + dz * dz > rad * rad + 1) {
               continue;
            }

            int x = p[0] + dx;
            int z = p[1] + dz;
            if (x < x0 || x > x0 + 15 || z < z0 || z > z0 + 15) {
               continue;
            }

            int ground = groundTop(x, z);
            int from = ground >= 0 ? Math.max(1, groundBottom(x, z)) : 40;
            for (int y = from; y <= p[2]; y++) {
               c.blocks[Chunk.idx(x - x0, y, z - z0)] = (short)Blocks.OBSIDIAN;
            }

            if (dx == 0 && dz == 0) {
               c.blocks[Chunk.idx(x - x0, p[2], z - z0)] = (short)Blocks.BEDROCK;
            }
         }
      }
   }

   /** The bedrock fountain: a dish 7 x 7 (corners cut off) with a rim, a pillar of bedrock in the middle and torches. */
   private static void fountain(Chunk c) {
      int x0 = c.cx * 16;
      int z0 = c.cz * 16;
      if (x0 > 4 || x0 + 15 < -4 || z0 > 4 || z0 + 15 < -4) {
         return;
      }

      for (int dz = -3; dz <= 3; dz++) {
         for (int dx = -3; dx <= 3; dx++) {
            if (Math.abs(dx) == 3 && Math.abs(dz) == 3) {
               continue;
            }

            int lx = dx - x0;
            int lz = dz - z0;
            if (lx < 0 || lx > 15 || lz < 0 || lz > 15) {
               continue;
            }

            c.blocks[Chunk.idx(lx, TOP, lz)] = (short)Blocks.BEDROCK;
            boolean rim = Math.abs(dx) == 3 || Math.abs(dz) == 3;
            if (rim) {
               c.blocks[Chunk.idx(lx, TOP + 1, lz)] = (short)Blocks.BEDROCK;
            }

            if (rim && (dx == 0 || dz == 0)) {
               c.blocks[Chunk.idx(lx, TOP + 2, lz)] = (short)Blocks.TORCH;
            }
         }
      }

      int lx = -x0;
      int lz = -z0;
      if (lx >= 0 && lx <= 15 && lz >= 0 && lz <= 15) {
         for (int y = TOP + 1; y <= TOP + 4; y++) {
            c.blocks[Chunk.idx(lx, y, lz)] = (short)Blocks.BEDROCK;
         }
      }
   }

   /** The platform you arrive on: obsidian 5 x 5 with free air above. */
   private static void platform(Chunk c) {
      int x0 = c.cx * 16;
      int z0 = c.cz * 16;
      for (int dz = -2; dz <= 2; dz++) {
         for (int dx = -2; dx <= 2; dx++) {
            int lx = PLATFORM_X + dx - x0;
            int lz = PLATFORM_Z + dz - z0;
            if (lx < 0 || lx > 15 || lz < 0 || lz > 15) {
               continue;
            }

            for (int y = PLATFORM_Y + 1; y <= PLATFORM_Y + 4; y++) {
               c.blocks[Chunk.idx(lx, y, lz)] = 0;
            }

            c.blocks[Chunk.idx(lx, PLATFORM_Y, lz)] = (short)Blocks.OBSIDIAN;
         }
      }
   }

   /** Where the exit portal's 8 blocks go (a ring round the bedrock pillar in the middle), y of the portal blocks included. */
   static int[][] exitPortalCells() {
      int[][] cells = new int[8][];
      int n = 0;
      for (int dz = -1; dz <= 1; dz++) {
         for (int dx = -1; dx <= 1; dx++) {
            if (dx != 0 || dz != 0) {
               cells[n++] = new int[]{dx, TOP + 1, dz};
            }
         }
      }

      return cells;
   }
}
