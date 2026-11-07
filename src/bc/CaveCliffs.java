package bc;

/**
 * Update 6.22: a Caves &amp; Cliffs-style underground carver for newly created worlds
 * ({@code WorldSettings.caveVer >= 2}). This replaces the single ridged-tunnel field of V6.21 with
 * the four shapes Minecraft 1.18 uses, each with its own scale and depth range:
 *
 * <ul>
 *   <li><b>noodle caves</b> - thin, twisty tunnels close to the surface;</li>
 *   <li><b>spaghetti caves</b> - long winding tunnels through the middle depths;</li>
 *   <li><b>cheese caves</b> - big irregular low caverns in coherent underground provinces;</li>
 *   <li><b>giant halls and wells</b> - sparse hashed ellipsoids and vertical connections between layers.</li>
 * </ul>
 *
 * Below a locally hashed <b>aquifer level</b> carved space fills with water instead of air, so a cave
 * can be dry above a lake and flooded below it. Every answer is a pure function of the seed and the
 * world coordinate (the province/level hashes are on fixed world grids), so neighbouring chunks and
 * worker threads always agree at a seam. {@link #finish} adds lush caves, dripstone, ice pockets,
 * amethyst geodes, deep-dark floors and clay under water, all clipped to the chunk's inner columns.
 *
 * Old saves stay on {@code caveVer = 1} (V6.21) and never call into this class.
 */
final class CaveCliffs {
   static final int SOLID = 0;
   static final int AIR = 1;
   static final int LAVA = 2;
   static final int WATER = 3;

   /** Underground cave biomes, hashed onto a 128-block province grid. */
   static final int PLAIN = 0;
   static final int LUSH = 1;
   static final int DRIPSTONE = 2;
   static final int AMETHYST = 3;
   static final int ICE = 4;
   static final int DEEP = 5;

   static final int PROVINCE = 128;
   static final int AQUIFER = 64;
   static final int GEODE = 56;
   /** V6.28: hashed grid of real cave entrances (not ravines or sinkhole pits). */
   static final int MOUTH_CELL = 96;

   /**
    * A 1.20-style cave mouth: a visible opening in the surface that slopes down
    * through the rock and opens into a chamber joined to the underground carver.
    * All values are pure functions of the seed and the world coordinate.
    */
   static final class Mouth {
      int x, z;
      int radius;
      double dirX, dirZ;

      /** True when this column is part of the stone lip around the opening. */
      boolean rim(int wx, int wz) {
         double d = Math.sqrt((double)(wx - x) * (wx - x) + (double)(wz - z) * (wz - z));
         double r = radius * 1.35;
         return d > r + 0.5 && d < r + 2.4;
      }

      /** True when this column's surface is inside the opening (used to keep trees out). */
      boolean surfaceHole(int wx, int wz) {
         double d = Math.sqrt((double)(wx - x) * (wx - x) + (double)(wz - z) * (wz - z));
         return d <= radius * 1.35 + 0.9 * Math.sin((wx - x) * 0.6 + (wz - z) * 0.8);
      }

      /** Carve the opening, the sloped descent and the bottom chamber. */
      boolean air(int wx, int y, int wz, int surfaceH) {
         if (surfaceH < 66 || y > surfaceH || y < 20) return false;
         int bottom = Math.max(26, surfaceH - 46);
         if (y < bottom - 6) return false;

         // 1. the visible opening through the topsoil
         if (y >= surfaceH - 3) return surfaceHole(wx, wz);

         // 2. a diagonal, walkable-looking tunnel (not a vertical crack)
         int top = surfaceH - 3;
         double t = (double)(top - y) / Math.max(1, top - bottom);
         if (t < -0.02 || t > 1.08) return false;
         double drift = (top - bottom) * 1.05;   // a shallow ramp, not a vertical crack
         double cx = x + dirX * t * drift;
         double cz = z + dirZ * t * drift;
         double rr = radius * (1.0 - 0.28 * Math.max(0.0, Math.min(1.0, t)));
         double dx = wx - cx, dz = wz - cz;
         if (dx * dx + dz * dz <= rr * rr) return true;

         // 3. a rounded chamber at the bottom that definitely joins the cave system
         double bx = wx - (x + dirX * drift), bz = wz - (z + dirZ * drift);
         double dy = (y - bottom) * 1.25;
         if (y >= bottom - 5 && y <= bottom + 4
            && bx * bx + bz * bz + dy * dy <= (radius * 2.4) * (radius * 2.4)) return true;

         // 4. a low gallery running out of the chamber into the surrounding cave
         // system, so the entrance is never a dead-end pit.
         if (y < bottom - 2 || y > bottom + 3) return false;
         double along = bx * dirX + bz * dirZ;
         double side = -bx * dirZ + bz * dirX;
         return along >= -1.0 && along <= radius * 4.5
            && Math.abs(side) <= radius * 0.85 && Math.abs(y - bottom) <= 2;
      }
   }

   /** Candidate entrances whose centre is near this column (at most 9). */
   Mouth[] mouths(int x, int z) {
      int cx = Math.floorDiv(x, MOUTH_CELL);
      int cz = Math.floorDiv(z, MOUTH_CELL);
      Mouth[] out = null;
      int n = 0;
      for (int i = -1; i <= 1; i++) {
         for (int j = -1; j <= 1; j++) {
            long h = CaveWorks.mix(seed ^ 0x4D4F555448L, cx + i, 0, cz + j);
            if ((h >>> 58) >= 34) continue;   // ~53% of cells get an entrance
            int px = (cx + i) * MOUTH_CELL + 24 + CaveWorks.bits(h, 8, 48);
            int pz = (cz + j) * MOUTH_CELL + 24 + CaveWorks.bits(h, 20, 48);
            int dx = x - px, dz = z - pz;
            if (dx * dx + dz * dz > MOUTH_CELL * MOUTH_CELL * 2) continue;
            if (out == null) out = new Mouth[9];
            Mouth m = new Mouth();
            m.x = px;
            m.z = pz;
            m.radius = 4 + CaveWorks.bits(h, 30, 5);   // 4..8 block half-width
            double ang = CaveWorks.bits(h, 40, 360) * Math.PI / 180.0;
            m.dirX = Math.cos(ang);
            m.dirZ = Math.sin(ang);
            out[n++] = m;
         }
      }
      if (n == 0) return null;
      return java.util.Arrays.copyOf(out, n);
   }

   final long seed;
   final WorldSettings st;
   final Noise sp;
   final Noise nd;
   final Noise cheese;
   final Noise province;

   CaveCliffs(long seed, WorldSettings st) {
      this.seed = seed;
      this.st = st;
      this.sp = new Noise(seed ^ 0x5350414748455454L);
      this.nd = new Noise(seed ^ 0x4E4F4F444C45L);
      this.cheese = new Noise(seed ^ 0x434845455345L);
      this.province = new Noise(seed ^ 0x50524F56494E4345L);
   }

   private static double band(double y, double lo, double hi) {
      if (y <= lo || y >= hi) return 0.0;
      double t = (y - lo) / (hi - lo);
      return Math.min(1.0, Math.min(t, 1.0 - t) * 4.0);
   }

   /** Thin vertical connections between cave layers, so a deep system is not a set of sealed floors. */
   static boolean well(long seed, int x, int z) {
      int cx = Math.floorDiv(x, 48);
      int cz = Math.floorDiv(z, 48);
      for (int i = -1; i <= 1; i++) {
         for (int j = -1; j <= 1; j++) {
            long h = CaveWorks.mix(seed ^ 0x57454C4CL, cx + i, 0, cz + j);
            if ((h & 15) != 0) continue; // ~6% of cells
            int px = (cx + i) * 48 + 10 + CaveWorks.bits(h, 10, 28);
            int pz = (cz + j) * 48 + 10 + CaveWorks.bits(h, 22, 28);
            int r = 1 + CaveWorks.bits(h, 34, 2);
            int dx = x - px;
            int dz = z - pz;
            if (dx * dx + dz * dz <= r * r) return true;
         }
      }

      return false;
   }

   /**
    * The local water table, or 0 when this 64x64 region has no aquifer. A hashed level, not a global
    * sea level, is what makes one cavern flooded and the next one dry.
    */
   static int aquiferLevel(long seed, int x, int z) {
      int cx = Math.floorDiv(x, AQUIFER);
      int cz = Math.floorDiv(z, AQUIFER);
      long h = CaveWorks.mix(seed ^ 0x41515549464552L, cx, 0, cz);
      if ((h >>> 58) >= 24) return 0; // ~38% of regions
      return 22 + CaveWorks.bits(h, 12, 28); // y = 22..49
   }

   /** Which underground biome this column belongs to. Depth turns the bottom into the deep dark. */
   int caveBiome(int x, int y, int z) {
      if (y < 14) return DEEP;
      int cx = Math.floorDiv(x, PROVINCE);
      int cz = Math.floorDiv(z, PROVINCE);
      long h = CaveWorks.mix(seed ^ 0x4341564542494FL, cx, 0, cz);
      int roll = CaveWorks.bits(h, 6, 100);
      // a slow noise warps the province borders so they are not square 128-block tiles
      double warp = province.noise2(x * 0.006, z * 0.006) * 18.0;
      int wx = Math.floorDiv((int)(x + warp), PROVINCE);
      int wz = Math.floorDiv((int)(z - warp), PROVINCE);
      if (wx != cx || wz != cz) {
         h = CaveWorks.mix(seed ^ 0x4341564542494FL, wx, 0, wz);
         roll = CaveWorks.bits(h, 6, 100);
      }

      if (roll < 24) return LUSH;
      if (roll < 50) return DRIPSTONE;
      if (roll < 60) return ICE;
      if (roll < 68) return AMETHYST;
      return PLAIN;
   }

   /** 0 = solid rock, 1 = air, 2 = lava, 3 = aquifer water. Pure in (seed, x, y, z). */
   int carve(int x, int y, int z) {
      if (y < 5 || y > 230) return SOLID;
      boolean v3 = st.caveVer >= 3;   // V6.28: larger, better-connected 1.20-style caverns
      float cs = st.caveScale < 0.6F ? 0.6F : (st.caveScale > 3.0F ? 3.0F : st.caveScale);
      float dens = st.caveDensity < 0.4F ? 0.4F : (st.caveDensity > 2.5F ? 2.5F : st.caveDensity);
      double k = 1.0 / cs;

      // 1. Noodle caves: very thin, mostly just below the surface.
      double surface = 0.20 + 0.80 * band(y, 34.0, 66.0) + (y > 66 ? 0.35 : 0.0);
      double nx = sp.noise(x * 0.034 * k + 13.0, y * 0.049 * k - 7.0, z * 0.034 * k + 29.0);
      double nz = nd.noise(x * 0.034 * k - 41.0, y * 0.049 * k + 17.0, z * 0.034 * k - 3.0);
      boolean air = nx * nx + nz * nz < 0.0016 * dens * surface;

      // 2. Spaghetti caves: the long winding tunnels of the middle depths.
      if (!air) {
         double mid = 0.35 + 0.65 * band(y, 8.0, 62.0);
         double ax = sp.noise(x * 0.016 * k, y * 0.023 * k, z * 0.016 * k);
         double bx = nd.noise(x * 0.016 * k + 31.7, y * 0.023 * k + 11.3, z * 0.016 * k - 47.1);
         air = ax * ax + bx * bx < 0.0038 * dens * mid * (v3 ? 1.35 : 1.0);
      }

      // 2b. V6.28: a third, very long tunnel scale joins the separate cave pockets
      // into continuous systems (1.18 "spaghetti" caves run for hundreds of blocks).
      if (!air && v3) {
         double cx = sp.noise(x * 0.008 * k - 71.0, y * 0.012 * k + 5.0, z * 0.008 * k + 19.0);
         double cz = nd.noise(x * 0.008 * k + 43.0, y * 0.012 * k - 29.0, z * 0.008 * k - 61.0);
         air = cx * cx + cz * cz < 0.0030 * dens * (0.45 + 0.55 * band(y, 6.0, 78.0));
      }

      // 3. Cheese caves: large irregular caverns, clustered by a slow province.
      // V6.28 reaches much higher and uses two overlapping fields, like the big
      // multi-room cheese caverns of 1.18/1.20 rather than one sealed pocket.
      if (!air && y >= 8 && y <= (v3 ? 72 : 46)) {
         double q = cheese.noise(x * 0.010 * k + 91.0, y * 0.015 * k - 17.0, z * 0.010 * k + 53.0);
         double gate = (v3 ? 0.36 : 0.42) - 0.06 * (dens - 1.0F)
            + 0.055 * province.noise2(x * 0.003 * k, z * 0.003 * k);
         double depth = band(y, 6.0, v3 ? 80.0 : 52.0);
         air = q > gate && depth > 0.16;
         if (!air && v3) {
            double q2 = cheese.noise(x * 0.014 * k - 33.0, y * 0.019 * k + 7.0, z * 0.014 * k - 91.0);
            air = q2 > gate + 0.09 && depth > 0.22;
         }
      }

      // 4. Sparse giant halls, vertical wells and geode interiors.
      if (!air && y >= 9 && y <= 38 && CaveWorks.hall(seed, x, y, z)) air = true;
      if (!air && y >= 12 && y <= 54 && well(seed, x, z)) air = true;

      if (!air) return SOLID;

      // Lava only at the very bottom and only in coherent lava provinces.
      if (y <= 10 && cheese.noise(x * 0.004, 3.0, z * 0.004) > 0.16) return LAVA;

      // A local aquifer: carved space below its hashed level is water, not air.
      int level = aquiferLevel(seed, x, z);
      if (level > 0 && y <= level && y > 12) return WATER;

      return AIR;
   }

   // ------------------------------------------------------------------ chunk finishing

   /** Decorate a freshly generated V6.22 chunk. Only inner columns are touched, so seams agree. */
   void finish(Chunk ch) {
      float rich = st.caveRichness < 0.4F ? 0.4F : (st.caveRichness > 2.0F ? 2.0F : st.caveRichness);
      int scale = (int)(1000.0F / rich);
      if (scale < 300) scale = 300;

      for (int z = 2; z < 14; z++) {
         for (int x = 2; x < 14; x++) {
            int wx = ch.cx * 16 + x;
            int wz = ch.cz * 16 + z;
            for (int y = 7; y < 58; y++) {
               int id = ch.get(x, y, z);
               if (id == 0) {
                  decorateAir(ch, x, y, z, wx, wz, scale);
               } else if (id == Blocks.WATER) {
                  // clay / gravel under an aquifer, and a lily pad on a still surface
                  int below = ch.get(x, y - 1, z);
                  if (below != 0 && CaveWorks.rock(below) && CaveWorks.bits(CaveWorks.mix(seed ^ 0x434C4159L, wx, y, wz), 3, 100) < 42) {
                     ch.set(x, y - 1, z, Blocks.CLAY);
                  }

                  if (ch.get(x, y + 1, z) == 0 && CaveWorks.bits(CaveWorks.mix(seed ^ 0x4C494C59L, wx, y, wz), 3, 1000) < 45 * 1000 / scale) {
                     ch.set(x, y + 1, z, Blocks313.LILY_PAD);
                  }
               }
            }
         }
      }

      geodes(ch);
   }

   private void decorateAir(Chunk ch, int x, int y, int z, int wx, int wz, int scale) {
      int floor = y - 1;
      if (floor < 6 || ch.get(x, floor, z) == 0) return;
      int ceil = y + 1;
      int gap = 0;
      while (ceil < 62 && ch.get(x, ceil, z) == 0 && gap < 12) {
         ceil++;
         gap++;
      }

      if (ceil >= 62 || gap < 2) return;
      int biome = caveBiome(wx, y, wz);
      long h = CaveWorks.mix(seed ^ 0x4445434FL, wx, y, wz);
      int roll = CaveWorks.bits(h, 4, 1000);
      int roll2 = CaveWorks.bits(h, 20, 1000);
      int floorId = ch.get(x, floor, z);

      switch (biome) {
         case LUSH: {
            if (roll < 210 * 1000 / scale && CaveWorks.rock(floorId)) {
               ch.set(x, floor, z, roll2 < 400 ? Blocks.MOSSY_COBBLE : Blocks313.MYCELIUM);
            }

            if (roll >= 210 && roll < 210 + 85 * 1000 / scale && CaveWorks.rock(floorId)) {
               // azalea bush: a few leaves and a log on a mossy floor
               ch.set(x, floor, z, Blocks.MOSSY_COBBLE);
               ch.set(x, y, z, Blocks.LOG_OAK);
               ch.set(x, y + 1, z, Blocks.LEAVES_OAK);
               if (x > 2 && x < 13) ch.set(x + 1, y + 1, z, Blocks.LEAVES_OAK);
               if (z > 2 && z < 13) ch.set(x, y + 1, z + 1, Blocks.LEAVES_OAK);
            }

            if (roll >= 330 && roll < 330 + 60 * 1000 / scale) {
               ch.set(x, ceil, z, Blocks313.VINE0 + 1);       // hanging cave vines
            }

            if (roll >= 410 && roll < 410 + 42 * 1000 / scale) {
               ch.set(x, ceil, z, Blocks.GLOWSTONE);          // glow berries
            }

            if (roll >= 470 && roll < 470 + 22 * 1000 / scale) {
               ch.set(x, ceil - 1, z, Blocks.GLASS0 + 12);    // a pale spore blossom
            }

            break;
         }
         case DRIPSTONE: {
            if (roll < 130 * 1000 / scale) {
               int len = Math.min(gap - 1, 1 + CaveWorks.bits(h, 28, 4));
               for (int k = 0; k < len; k++) {
                  ch.set(x, ceil - 1 - k, z, k == len - 1 ? Blocks313.GRANITE : Blocks313.ANDESITE);
               }
            }

            if (roll2 < 105 * 1000 / scale) {
               int len = Math.min(gap - 1, 1 + CaveWorks.bits(h, 34, 3));
               for (int k = 0; k < len && y + k < ceil; k++) {
                  ch.set(x, y + k, z, k == len - 1 ? Blocks313.DIORITE : Blocks.STONE);
               }
            }

            if (gap <= 6 && roll >= 800 && roll < 800 + 28 * 1000 / scale) {
               for (int k = 0; k < gap; k++) ch.set(x, y + k, z, Blocks313.ANDESITE);
            }

            if (CaveWorks.rock(floorId) && roll >= 900 && roll < 900 + 40 * 1000 / scale) {
               ch.set(x, floor, z, Blocks.COBBLE);
            }

            break;
         }
         case ICE: {
            if (roll < 120 * 1000 / scale && CaveWorks.rock(floorId)) {
               ch.set(x, floor, z, Blocks313.ICE);
            }

            if (roll >= 120 && roll < 120 + 70 * 1000 / scale) {
               ch.set(x, ceil, z, Blocks313.ICE);             // icicles
            }

            if (roll >= 200 && roll < 200 + 35 * 1000 / scale && y + 1 < 62 && ch.get(x, y + 1, z) == 0) {
               ch.set(x, y, z, Blocks313.SNOW_LAYER);
            }

            break;
         }
         case DEEP: {
            if (roll < 150 * 1000 / scale && CaveWorks.rock(floorId)) {
               ch.set(x, floor, z, Redstone319.DARK_PRISMARINE);
            }

            if (roll >= 150 && roll < 150 + 12 * 1000 / scale) {
               ch.set(x, ceil, z, Redstone319.SEA_LANTERN);
            }

            if (roll >= 700 && roll < 700 + 8 * 1000 / scale) {
               ch.set(x, y, z, Blocks.SPAWNER);
            }

            break;
         }
         case AMETHYST: {
            if (roll < 70 * 1000 / scale && CaveWorks.rock(floorId)) {
               ch.set(x, floor, z, Redstone319.PURPUR_BLOCK);
            }

            if (roll >= 70 && roll < 70 + 30 * 1000 / scale) {
               ch.set(x, y, z, Blocks.GLASS0 + 10);
            }

            break;
         }
         default: {
            // ordinary caves still get dripstone, rubble, exposed ore and glowcaps
            if (roll < 55 * 1000 / scale) {
               int len = Math.min(gap - 1, 1 + CaveWorks.bits(h, 28, 3));
               for (int k = 0; k < len; k++) ch.set(x, ceil - 1 - k, z, Blocks313.ANDESITE);
            }

            if (roll2 < 45 * 1000 / scale) {
               ch.set(x, y, z, Blocks.STONE);
            }

            if (roll >= 500 && roll < 500 + 30 * 1000 / scale && CaveWorks.rock(floorId)) {
               int ore = y < 16 ? Blocks.DIAMOND_ORE : (y < 28 ? Blocks.GOLD_ORE : (y < 44 ? Blocks.IRON_ORE : Blocks.COAL_ORE));
               ch.set(x, floor, z, ore);
            }

            if (roll >= 760 && roll < 760 + 10 * 1000 / scale) {
               ch.set(x, ceil, z, Blocks.GLOWSTONE);
            }

            break;
         }
      }
   }

   /** The shell and crystal spikes of an amethyst geode, clipped to this chunk. */
   private void geodes(Chunk ch) {
      for (int gy = 0; gy <= 1; gy++) {
         for (int gz = Math.floorDiv(ch.cz * 16 - 8, GEODE); gz <= Math.floorDiv(ch.cz * 16 + 23, GEODE); gz++) {
            for (int gx = Math.floorDiv(ch.cx * 16 - 8, GEODE); gx <= Math.floorDiv(ch.cx * 16 + 23, GEODE); gx++) {
               long h = CaveWorks.mix(seed ^ 0x47454F4445L, gx, gy, gz);
               if ((h >>> 58) >= 9) continue;
               int px = gx * GEODE + 10 + CaveWorks.bits(h, 8, 36);
               int py = gy * 40 + 8 + CaveWorks.bits(h, 20, 22);
               int pz = gz * GEODE + 10 + CaveWorks.bits(h, 30, 36);
               int r = 3 + CaveWorks.bits(h, 42, 3);
               for (int dy = -r - 1; dy <= r + 1; dy++) {
                  int wy = py + dy;
                  if (wy < 6 || wy > 60) continue;
                  for (int dz = -r - 1; dz <= r + 1; dz++) {
                     for (int dx = -r - 1; dx <= r + 1; dx++) {
                        double d = Math.sqrt(dx * dx + dy * dy * 1.6 + dz * dz);
                        if (d > r + 0.4) continue;
                        int lx = px + dx - ch.cx * 16;
                        int lz = pz + dz - ch.cz * 16;
                        if (lx < 1 || lx > 14 || lz < 1 || lz > 14) continue;
                        int id = ch.get(lx, wy, lz);
                        if (id == Blocks.BEDROCK || Blocks.isFluid(id)) continue;
                        if (d >= r - 0.6) {
                           ch.set(lx, wy, lz, d > r - 0.1 ? Blocks.OBSIDIAN : Redstone319.PURPUR_BLOCK);
                        } else {
                           ch.set(lx, wy, lz, 0);
                           // amethyst crystals grow inward from the shell
                           long q = CaveWorks.mix(seed ^ 0x414D455448595354L, px + dx, py + dy, pz + dz);
                           if (d > r - 1.8 && dy >= -1 && CaveWorks.bits(q, 3, 100) < 42) {
                              ch.set(lx, wy, lz, Blocks.GLASS0 + 10);
                           } else if (d > r - 2.6 && dy == -r + 2 && CaveWorks.bits(q, 12, 100) < 25) {
                              ch.set(lx, wy, lz, Redstone319.SEA_LANTERN);
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }
}
