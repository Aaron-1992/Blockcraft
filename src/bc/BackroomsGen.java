package bc;

/**
 * Infinite Multi-Zone Level 0 Backrooms World Generator ("The Mono-Yellow Labyrinth").
 * Generates an endless subterranean interior complex between {@link #FLOOR_Y} (64) and {@link #CEIL_Y} (70),
 * sealed above and below by bedrock so no exterior sky light ever leaks in:
 * - Zone 0: Classic Level 0 segmented yellow wallpaper rooms, wooden wainscoting & damp yellow carpet
 * - Zone 1: Endless Pillar Halls (repeating 2x2 columns)
 * - Zone 2: Claustrophobic Industrial Maintenance Tunnels (stonebrick, iron bars/fences, dim lights)
 * - Zone 3: Level "!" Red Emergency Corridors (crimson carpet, brick walls, redstone emergency torches)
 * - Zone 4: Poolrooms Sub-Sector (white quartz tiles, shallow water basins, cyan sea-lantern panels)
 * - Zone 5: Blackout Ruined Sectors (shattered fluorescent tubes, debris & Almond Water supply crates)
 */
public class BackroomsGen extends Gen {
   public static final int FLOOR_Y = 64;
   public static final int CEIL_Y = 70;
   public static final int CARPET_BLOCK = Blocks.WOOL0 + 4;

   public static final int ZONE_CLASSIC = 0;
   public static final int ZONE_PILLARS = 1;
   public static final int ZONE_MAINTENANCE = 2;
   public static final int ZONE_RED_RUN = 3;
   public static final int ZONE_POOLROOMS = 4;
   public static final int ZONE_BLACKOUT = 5;

   public BackroomsGen(long seed, WorldSettings settings) {
      super(seed);
      if (settings != null) {
         this.settings = settings;
      }
   }

   private static long cellHash(long seed, int x, int z) {
      long h = seed ^ (x * 341873128712L) ^ (z * 132897987541L);
      h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
      h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
      return h ^ (h >>> 31);
   }

   /** Returns the Backrooms sector zone (0..5) at world coordinates (wx, wz). */
   public static int zoneAt(long seed, int wx, int wz) {
      if (Math.abs(wx) <= 10 && Math.abs(wz) <= 10) {
         return ZONE_CLASSIC;
      }
      int sx = Math.floorDiv(wx, 24);
      int sz = Math.floorDiv(wz, 24);
      long h = cellHash(seed ^ 0x5A17C9E3L, sx, sz);
      int roll = (int)Math.floorMod(h, 100L);
      if (roll < 46) {
         return ZONE_CLASSIC;
      } else if (roll < 66) {
         return ZONE_PILLARS;
      } else if (roll < 78) {
         return ZONE_MAINTENANCE;
      } else if (roll < 86) {
         return ZONE_POOLROOMS;
      } else if (roll < 93) {
         return ZONE_BLACKOUT;
      } else {
         return ZONE_RED_RUN;
      }
   }

   private static void setBlk(Chunk c, int lx, int y, int lz, int id) {
      if (y >= 0 && y < 256) {
         c.blocks[Chunk.idx(lx, y, lz)] = (short)id;
      }
   }

   @Override
   public Gen.Col column(int x, int z) {
      Gen.Col c = new Gen.Col();
      c.t = 0.8;
      c.hu = 0.2;
      c.biome = Biomes.DESERT;
      c.h = FLOOR_Y;
      return c;
   }

   @Override
   public int[] findSpawn() {
      return new int[]{0, FLOOR_Y + 1, 0};
   }

   @Override
   public Chunk generate(int cx, int cz) {
      Chunk c = new Chunk(cx, cz);
      for (int i = 0; i < 256; i++) {
         c.biome[i] = (byte)Biomes.DESERT;
      }

      int wx0 = cx << 4;
      int wz0 = cz << 4;

      for (int lx = 0; lx < 16; lx++) {
         for (int lz = 0; lz < 16; lz++) {
            int wx = wx0 + lx;
            int wz = wz0 + lz;

            int zone = zoneAt(this.seed, wx, wz);

            // Sub-floor foundation
            setBlk(c, lx, 0, lz, Blocks.BEDROCK);
            for (int y = 1; y < FLOOR_Y - 1; y++) {
               setBlk(c, lx, y, lz, Blocks.STONE);
            }
            setBlk(c, lx, FLOOR_Y - 1, lz, Blocks.BEDROCK);

            // Floor material by zone
            if (zone == ZONE_MAINTENANCE) {
               setBlk(c, lx, FLOOR_Y, lz, ((wx + wz) & 3) == 0 ? Blocks.GRAVEL : Blocks.COBBLE);
            } else if (zone == ZONE_RED_RUN) {
               setBlk(c, lx, FLOOR_Y, lz, Blocks.WOOL0 + 14); // Blood-red carpet
            } else if (zone == ZONE_POOLROOMS) {
               setBlk(c, lx, FLOOR_Y - 1, lz, Blocks.QUARTZ_BLOCK);
               int rx = Math.floorMod(wx, 8);
               int rz = Math.floorMod(wz, 8);
               boolean basin = rx >= 2 && rx <= 5 && rz >= 2 && rz <= 5;
               setBlk(c, lx, FLOOR_Y, lz, basin ? Blocks.WATER : Blocks.QUARTZ_BLOCK);
            } else {
               boolean dampSpot = zone == ZONE_BLACKOUT && Math.floorMod(cellHash(this.seed ^ 0x99L, wx, wz), 11L) == 0;
               setBlk(c, lx, FLOOR_Y, lz, dampSpot ? (Blocks.WOOL0 + 12) : CARPET_BLOCK);
            }

            // Clear interior headroom (5 blocks tall: y = 65..69)
            for (int y = FLOOR_Y + 1; y < CEIL_Y; y++) {
               setBlk(c, lx, y, lz, Blocks.AIR);
            }

            // Ceiling acoustic tiles & fluorescent light grid (every 6 blocks)
            int mx = Math.floorMod(wx, 6);
            int mz = Math.floorMod(wz, 6);
            boolean lightFixture = (mx == 2 || mx == 3) && mz == 3;
            int cellX = Math.floorDiv(wx, 6);
            int cellZ = Math.floorDiv(wz, 6);
            long cellH = cellHash(this.seed, cellX, cellZ);
            boolean spawnArea = Math.abs(wx) <= 7 && Math.abs(wz) <= 7;

            if (lightFixture) {
               if (spawnArea) {
                  setBlk(c, lx, CEIL_Y, lz, Blocks.GLOWSTONE);
               } else if (zone == ZONE_BLACKOUT) {
                  setBlk(c, lx, CEIL_Y, lz, (cellH & 7L) == 0L ? Blocks.GLOWSTONE : Blocks.GLASS);
               } else if (zone == ZONE_RED_RUN) {
                  setBlk(c, lx, CEIL_Y, lz, Blocks.BRICKS);
               } else if (zone == ZONE_POOLROOMS) {
                  setBlk(c, lx, CEIL_Y, lz, Redstone319.SEA_LANTERN);
               } else if (zone == ZONE_MAINTENANCE) {
                  setBlk(c, lx, CEIL_Y, lz, (cellH & 3L) == 0L ? Blocks.GLOWSTONE : Blocks.GLASS);
               } else {
                  boolean brokenTube = (cellH & 15L) == 0L;
                  setBlk(c, lx, CEIL_Y, lz, brokenTube ? Blocks.GLASS : Blocks.GLOWSTONE);
               }
            } else {
               int ceilBlock = zone == ZONE_POOLROOMS
                  ? Blocks.QUARTZ_BLOCK
                  : (zone == ZONE_MAINTENANCE ? Blocks.STONEBRICK : (zone == ZONE_RED_RUN ? Blocks.BRICKS : Blocks.SANDSTONE));
               setBlk(c, lx, CEIL_Y, lz, ceilBlock);
            }

            // Sealed bedrock roof cap so 0 skylight enters and roof cannot be broken into
            setBlk(c, lx, CEIL_Y + 1, lz, Blocks.BEDROCK);

            // Guarantee an open starting lobby right around (0, 0)
            if (Math.abs(wx) <= 3 && Math.abs(wz) <= 3) {
               if (wx == 3 && wz == 0) {
                  setBlk(c, lx, FLOOR_Y + 1, lz, Blocks.CHEST_LOOT);
               }
               continue;
            }

            // Procedural walls & pillars on an 8x8 room grid
            int rx = Math.floorMod(wx, 8);
            int rz = Math.floorMod(wz, 8);
            int roomX = Math.floorDiv(wx, 8);
            int roomZ = Math.floorDiv(wz, 8);
            long rh = cellHash(this.seed ^ 0x9E3779B97F4A7C15L, roomX, roomZ);

            boolean isWall = false;
            boolean isDoorway = false;
            boolean isWindow = false;
            boolean isGrate = false;

            if (zone == ZONE_PILLARS) {
               if ((rx == 0 || rx == 1) && (rz == 0 || rz == 1)) {
                  isWall = true;
               } else if (rx == 0 && (rh & 7L) == 0L && rz >= 3 && rz <= 6) {
                  isWall = true;
               }
            } else if (zone == ZONE_MAINTENANCE) {
               boolean xAxis = (rh & 1L) == 0L;
               if (xAxis && (rz == 0 || rz == 3 || rz == 7)) {
                  isWall = true;
                  if (rx == 3 || rx == 4) {
                     isDoorway = (rh & 3L) != 0L;
                  } else if (rx == 1 || rx == 6) {
                     isGrate = true;
                  }
               } else if (!xAxis && (rx == 0 || rx == 3 || rx == 7)) {
                  isWall = true;
                  if (rz == 3 || rz == 4) {
                     isDoorway = (rh & 3L) != 0L;
                  } else if (rz == 1 || rz == 6) {
                     isGrate = true;
                  }
               }
            } else {
               boolean northWall = (rh & 3L) != 0L;
               boolean westWall = ((rh >>> 2) & 3L) != 0L;
               boolean innerSplit = ((rh >>> 4) & 7L) == 0L;

               if (rx == 0 && westWall) {
                  isWall = true;
                  int doorPos = 2 + (int)Math.floorMod(rh >>> 8, 4L);
                  if (rz == doorPos || rz == doorPos + 1) {
                     isDoorway = true;
                  } else if (rz == 6 && ((rh >>> 12) & 3L) == 0L) {
                     isWindow = true;
                  }
               }
               if (rz == 0 && northWall) {
                  isWall = true;
                  int doorPos = 2 + (int)Math.floorMod(rh >>> 14, 4L);
                  if (rx == doorPos || rx == doorPos + 1) {
                     isDoorway = true;
                  } else if (rx == 6 && ((rh >>> 18) & 3L) == 0L) {
                     isWindow = true;
                  }
               }
               if (innerSplit && rx == 4 && rz >= 2 && rz <= 6) {
                  isWall = true;
                  if (rz == 4) {
                     isDoorway = true;
                  }
               }
               if (rx == 0 && rz == 0) {
                  isWall = true;
                  isDoorway = false;
                  isWindow = false;
               }
            }

            if (isWall) {
               int wallMain = Blocks.SANDSTONE;
               int wallTrim = Blocks.PLANKS;
               if (zone == ZONE_MAINTENANCE) {
                  wallMain = Blocks.STONEBRICK;
                  wallTrim = Blocks.COBBLE;
               } else if (zone == ZONE_RED_RUN) {
                  wallMain = Blocks.BRICKS;
                  wallTrim = Blocks.STONEBRICK;
               } else if (zone == ZONE_POOLROOMS) {
                  wallMain = Blocks.QUARTZ_BLOCK;
                  wallTrim = Blocks.QUARTZ_BLOCK;
               }

               for (int y = FLOOR_Y + 1; y < CEIL_Y; y++) {
                  if (isDoorway && y <= FLOOR_Y + 4) {
                     setBlk(c, lx, y, lz, Blocks.AIR);
                  } else if (isWindow && (y == FLOOR_Y + 2 || y == FLOOR_Y + 3)) {
                     setBlk(c, lx, y, lz, Blocks.GLASS);
                  } else if (isGrate && (y == FLOOR_Y + 2 || y == FLOOR_Y + 3)) {
                     setBlk(c, lx, y, lz, Blocks.FENCE);
                  } else {
                     setBlk(c, lx, y, lz, y == FLOOR_Y + 1 ? wallTrim : wallMain);
                  }
               }
            } else {
               if (zone == ZONE_RED_RUN && rx == 4 && rz == 4) {
                  setBlk(c, lx, FLOOR_Y + 1, lz, Redstone319.REDSTONE_TORCH);
               }
               if (rx == 6 && rz == 6 && (rh & 31L) == 0L) {
                  setBlk(c, lx, FLOOR_Y + 1, lz, Blocks.CHEST_LOOT);
               }
            }
         }
      }
      return c;
   }
}
