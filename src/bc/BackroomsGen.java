package bc;

/**
 * Multi-Level Backrooms World Generator (v2.2).
 * <p>
 * Generates 5 separate, full-world Backrooms levels at distinct vertical strata, each featuring:
 * <ul>
 *   <li>Cohesive, detailed level architecture (baseboards, crown moldings, pilasters, coffered ceilings)</li>
 *   <li>Well-placed <b>3D Office Desks</b> ({@link BackroomsStalkerMod#B_DESK}) you can physically crawl underneath</li>
 *   <li>Well-placed <b>3D Hiding Lockers</b> ({@link BackroomsStalkerMod#B_LOCKER_LOW} / {@link BackroomsStalkerMod#B_LOCKER_HIGH}) flush against walls</li>
 *   <li>Well-placed <b>3D Almond Water Coolers</b> ({@link BackroomsStalkerMod#B_WATER_COOLER} / {@link BackroomsStalkerMod#B_COOLER_JUG})</li>
 *   <li>Dedicated <b>3D Illuminated Level Exit Doors</b> ({@link BackroomsStalkerMod#B_EXIT_LOW} / {@link BackroomsStalkerMod#B_EXIT_HIGH}) on EVERY level</li>
 * </ul>
 */
public class BackroomsGen extends Gen {
   public static final int FLOOR_Y = 64;
   public static final int CEIL_Y = 71;
   public static final int CARPET_BLOCK = Blocks.WOOL0 + 4;

   // Separate vertical floor elevations for each Backrooms Level
   public static final int L0_FLOOR_Y = 64;
   public static final int L0_CEIL_Y = 71;

   public static final int L1_FLOOR_Y = 46;
   public static final int L1_CEIL_Y = 53;

   public static final int L2_FLOOR_Y = 30;
   public static final int L2_CEIL_Y = 36;

   public static final int POOL_FLOOR_Y = 82;
   public static final int POOL_CEIL_Y = 90;

   public static final int RUN_FLOOR_Y = 14;
   public static final int RUN_CEIL_Y = 20;

   public static final int LEVEL_0 = 0;
   public static final int LEVEL_1 = 1;
   public static final int LEVEL_2 = 2;
   public static final int LEVEL_POOLROOMS = 3;
   public static final int LEVEL_RUN = 4;

   // Legacy zone constants kept for compatibility
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
      BackroomsStalkerMod.registerCustomBlocks();
   }

   private static long cellHash(long seed, int x, int z) {
      long h = seed ^ (x * 341873128712L) ^ (z * 132897987541L);
      h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
      h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
      return h ^ (h >>> 31);
   }

   public static int levelForY(double y) {
      if (y >= POOL_FLOOR_Y - 3) {
         return LEVEL_POOLROOMS;
      } else if (y >= L0_FLOOR_Y - 4) {
         return LEVEL_0;
      } else if (y >= L1_FLOOR_Y - 4) {
         return LEVEL_1;
      } else if (y >= L2_FLOOR_Y - 4) {
         return LEVEL_2;
      } else {
         return LEVEL_RUN;
      }
   }

   public static int floorYForLevel(int level) {
      switch (level) {
         case LEVEL_1:
            return L1_FLOOR_Y;
         case LEVEL_2:
            return L2_FLOOR_Y;
         case LEVEL_POOLROOMS:
            return POOL_FLOOR_Y;
         case LEVEL_RUN:
            return RUN_FLOOR_Y;
         case LEVEL_0:
         default:
            return L0_FLOOR_Y;
      }
   }

   public static int ceilYForLevel(int level) {
      switch (level) {
         case LEVEL_1:
            return L1_CEIL_Y;
         case LEVEL_2:
            return L2_CEIL_Y;
         case LEVEL_POOLROOMS:
            return POOL_CEIL_Y;
         case LEVEL_RUN:
            return RUN_CEIL_Y;
         case LEVEL_0:
         default:
            return L0_CEIL_Y;
      }
   }

   /** Returns the nearest Level Exit coordinates (wx, wz) for a player at (px, pz). Exits repeat on a 48-block grid at (0, 24 + 48*k). */
   public static int[] nearestExitXZ(double px, double pz) {
      int gx = (int)Math.round(px / 48.0) * 48;
      int gz = (int)Math.round((pz - 24.0) / 48.0) * 48 + 24;
      return new int[]{gx, gz};
   }

   private static boolean isExitVestibule(int wx, int wz) {
      int ex = Math.floorMod(wx + 24, 48) - 24;
      int ez = Math.floorMod(wz - 24 + 24, 48) - 24;
      return Math.abs(ex) <= 2 && Math.abs(ez) <= 2;
   }

   private static boolean isExitDoorPos(int wx, int wz) {
      int ex = Math.floorMod(wx + 24, 48) - 24;
      int ez = Math.floorMod(wz - 24 + 24, 48) - 24;
      return ex == 0 && ez == 0;
   }

   public static int zoneAt(long seed, int wx, int wz) {
      int rx = Math.floorDiv(wx, 32);
      int rz = Math.floorDiv(wz, 32);
      long h = cellHash(seed ^ 0x5A17C9E3L, rx, rz);
      return (Math.abs(wx) <= 12 && Math.abs(wz) <= 12) ? ZONE_CLASSIC : (int)Math.floorMod(h, 2L);
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
      BackroomsStalkerMod.registerCustomBlocks();
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

            setBlk(c, lx, 0, lz, Blocks.BEDROCK);
            for (int y = 1; y <= POOL_CEIL_Y + 1; y++) {
               setBlk(c, lx, y, lz, Blocks.STONE);
            }

            generateLevelRun(c, lx, lz, wx, wz);
            generateLevel2(c, lx, lz, wx, wz);
            generateLevel1(c, lx, lz, wx, wz);
            generateLevel0(c, lx, lz, wx, wz);
            generatePoolrooms(c, lx, lz, wx, wz);

            setBlk(c, lx, POOL_CEIL_Y + 1, lz, Blocks.BEDROCK);
         }
      }
      return c;
   }

   /** Places the universal Starting Lobby furniture & Level Exit Vestibule for any level. */
   private boolean placeLobbyOrExit(Chunk c, int lx, int lz, int wx, int wz, int fy, int cy) {
      // 1. Level Exit Vestibule every 48 blocks (guaranteed at (0, 24), (0, -24), (48, 24), etc.)
      if (isExitVestibule(wx, wz)) {
         int ex = Math.floorMod(wx + 24, 48) - 24;
         int ez = Math.floorMod(wz - 24 + 24, 48) - 24;
         // Emerald/Green carpet approach pad
         setBlk(c, lx, fy, lz, (Math.abs(ex) <= 1 && Math.abs(ez) <= 1) ? (Blocks.WOOL0 + 5) : Blocks313.PLANKS_BIRCH);
         setBlk(c, lx, cy, lz, Redstone319.SEA_LANTERN);
         if (isExitDoorPos(wx, wz)) {
            setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_EXIT_LOW);
            setBlk(c, lx, fy + 2, lz, BackroomsStalkerMod.B_EXIT_HIGH);
         } else if (Math.abs(ex) == 1 && ez == 0) {
            // Framed archway pillars flanking the 3D Exit Door
            for (int y = fy + 1; y <= fy + 3; y++) {
               setBlk(c, lx, y, lz, Blocks.QUARTZ_BLOCK);
            }
         }
         return true;
      }

      // 2. Starting Lobby around (0, 0) on every Level:
      //    - 2-block 3D Office Desk at (wx = 3, wz = 0..1) that you can crouch & crawl right underneath!
      //    - Pair of 3D Hiding Lockers flush against the wall at (wx = -2..-1, wz = -4)
      //    - 3D Almond Water Cooler at (wx = -3, wz = 0)
      if (Math.abs(wx) <= 4 && Math.abs(wz) <= 4) {
         if (wx == 3 && (wz == 0 || wz == 1)) {
            setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_DESK);
         } else if ((wx == -2 || wx == -1) && wz == -4) {
            setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_LOCKER_LOW);
            setBlk(c, lx, fy + 2, lz, BackroomsStalkerMod.B_LOCKER_HIGH);
         } else if (wx == -3 && wz == 0) {
            setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_WATER_COOLER);
            setBlk(c, lx, fy + 2, lz, BackroomsStalkerMod.B_COOLER_JUG);
         }
         return true;
      }
      return false;
   }

   // ====================================================================================================
   // LEVEL 0 — "THE LOBBY" (Y = 64..71): Pretty Mono-Yellow Wallpaper, Moldings, 3D Desks & 3D Lockers
   // ====================================================================================================
   private void generateLevel0(Chunk c, int lx, int lz, int wx, int wz) {
      int fy = L0_FLOOR_Y;
      int cy = L0_CEIL_Y;

      setBlk(c, lx, fy - 1, lz, Blocks.BEDROCK);

      // 12x12 Architectural Room Grid
      int rx = Math.floorMod(wx, 12);
      int rz = Math.floorMod(wz, 12);
      int roomX = Math.floorDiv(wx, 12);
      int roomZ = Math.floorDiv(wz, 12);
      long rh = cellHash(this.seed ^ 0x9E3779B97F4A7C15L, roomX, roomZ);
      int roomStyle = (int)Math.floorMod(rh, 5L);

      // Pretty bordered carpet floor: warm brown runner border along room perimeter (rx==1,11 or rz==1,11)
      boolean carpetBorder = (rx == 1 || rx == 11 || rz == 1 || rz == 11) && rx != 0 && rz != 0;
      setBlk(c, lx, fy, lz, carpetBorder ? (Blocks.WOOL0 + 12) : CARPET_BLOCK);

      for (int y = fy + 1; y < cy; y++) {
         setBlk(c, lx, y, lz, Blocks.AIR);
      }

      // Coffered Acoustic Ceiling with Birch Beams & 2x2 Recessed Fluorescent Light Panels
      boolean lightPanel = (rx == 5 || rx == 6) && (rz == 5 || rz == 6);
      boolean ceilingBeam = rx == 0 || rz == 0 || rx == 6 || rz == 6;
      if (lightPanel) {
         boolean working = Math.abs(wx) <= 8 && Math.abs(wz) <= 8 || (rh & 15L) != 0L;
         setBlk(c, lx, cy, lz, working ? Blocks.GLOWSTONE : Blocks.GLASS);
      } else {
         setBlk(c, lx, cy, lz, ceilingBeam ? Blocks313.PLANKS_BIRCH : Blocks.SANDSTONE);
      }
      setBlk(c, lx, cy + 1, lz, Blocks.BEDROCK);

      if (placeLobbyOrExit(c, lx, lz, wx, wz, fy, cy)) {
         return;
      }

      boolean isWall = false;
      boolean isDoor = false;
      boolean isWindow = false;
      boolean isPilaster = false;

      if (roomStyle == 3) {
         // Grand Fluted Pillar Gallery: 2x2 columns at (3..4, 3..4) and (8..9, 8..9)
         if (((rx == 3 || rx == 4) || (rx == 8 || rx == 9)) && ((rz == 3 || rz == 4) || (rz == 8 || rz == 9))) {
            isWall = true;
            isPilaster = (rx == 3 || rx == 9) && (rz == 3 || rz == 9);
         }
      } else {
         boolean westWall = (rh & 3L) != 0L;
         boolean northWall = ((rh >>> 2) & 3L) != 0L;
         if (rx == 0 && westWall) {
            isWall = true;
            if (rz >= 5 && rz <= 7) {
               isDoor = true;
            } else if ((rz == 3 || rz == 9) && ((rh >>> 8) & 1L) == 0L) {
               isWindow = true;
            } else if (rz == 4 || rz == 8) {
               isPilaster = true;
            }
         }
         if (rz == 0 && northWall) {
            isWall = true;
            if (rx >= 5 && rx <= 7) {
               isDoor = true;
            } else if ((rx == 3 || rx == 9) && ((rh >>> 10) & 1L) == 0L) {
               isWindow = true;
            } else if (rx == 4 || rx == 8) {
               isPilaster = true;
            }
         }
         if (rx == 0 && rz == 0) {
            isWall = true;
            isPilaster = true;
            isDoor = false;
            isWindow = false;
         }
      }

      if (isWall) {
         if (isDoor) {
            setBlk(c, lx, fy, lz, Blocks313.PLANKS_BIRCH);
         }
         for (int y = fy + 1; y < cy; y++) {
            if (isDoor && y <= fy + 4) {
               setBlk(c, lx, y, lz, Blocks.AIR);
            } else if (isWindow && (y == fy + 2 || y == fy + 3)) {
               setBlk(c, lx, y, lz, Blocks.GLASS);
            } else if (y == fy + 1) {
               setBlk(c, lx, y, lz, isPilaster ? Blocks.LOG_BIRCH : Blocks.PLANKS);
            } else if (y == cy - 1) {
               setBlk(c, lx, y, lz, Blocks313.PLANKS_BIRCH);
            } else {
               setBlk(c, lx, y, lz, isPilaster ? Blocks.LOG_BIRCH : Blocks.SANDSTONE);
            }
         }
         return;
      }

      // ---- WELL-PLACED 3D FURNITURE MODELS (Flush against walls, never blocking walkways!) ----
      boolean hasNorthWall = roomStyle != 3 && ((rh >>> 2) & 3L) != 0L;
      boolean hasWestWall = roomStyle != 3 && (rh & 3L) != 0L;

      // 1. Pair of 3D Hiding Lockers flush against the solid North Wall at (rx = 2..3, rz = 1)
      if (hasNorthWall && (rx == 2 || rx == 3) && rz == 1) {
         setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_LOCKER_LOW);
         setBlk(c, lx, fy + 2, lz, BackroomsStalkerMod.B_LOCKER_HIGH);
         return;
      }

      // 2. 2-block-wide 3D Executive Office Desk at (rx = 8..9, rz = 2) or (rx = 2, rz = 8..9)
      if ((roomStyle == 0 || roomStyle == 1) && (rx == 8 || rx == 9) && rz == 2) {
         setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_DESK);
         return;
      }
      if ((roomStyle == 2 || roomStyle == 4) && rx == 2 && (rz == 8 || rz == 9)) {
         setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_DESK);
         return;
      }

      // 3. 3D Almond Water Cooler placed neatly against the West Wall at (rx = 1, rz = 2)
      if (hasWestWall && rx == 1 && rz == 2 && (rh & 1L) == 0L) {
         setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_WATER_COOLER);
         setBlk(c, lx, fy + 2, lz, BackroomsStalkerMod.B_COOLER_JUG);
      }
   }

   // ====================================================================================================
   // LEVEL 1 — "THE HABITABLE ZONE / WAREHOUSE" (Y = 46..53)
   // ====================================================================================================
   private void generateLevel1(Chunk c, int lx, int lz, int wx, int wz) {
      int fy = L1_FLOOR_Y;
      int cy = L1_CEIL_Y;

      setBlk(c, lx, fy - 1, lz, Blocks.BEDROCK);

      int rx = Math.floorMod(wx, 12);
      int rz = Math.floorMod(wz, 12);
      long rh = cellHash(this.seed ^ 0x11112222L, Math.floorDiv(wx, 12), Math.floorDiv(wz, 12));

      boolean lane = rx == 5 || rx == 6 || rz == 5 || rz == 6;
      setBlk(c, lx, fy, lz, lane ? Redstone319.POLISHED_ANDESITE : Blocks.STONE);
      for (int y = fy + 1; y < cy; y++) {
         setBlk(c, lx, y, lz, Blocks.AIR);
      }

      boolean light = (rx == 5 || rx == 6) && (rz == 5 || rz == 6);
      boolean girder = rx == 0 || rz == 0;
      setBlk(c, lx, cy, lz, light ? Blocks.GLOWSTONE : (girder ? Redstone319.CHISELED_STONEBRICK : Blocks.STONEBRICK));
      setBlk(c, lx, cy + 1, lz, Blocks.BEDROCK);

      if (placeLobbyOrExit(c, lx, lz, wx, wz, fy, cy)) {
         return;
      }

      // Heavy 2x2 Warehouse Pillars at (rx = 0..1, rz = 0..1)
      if ((rx == 0 || rx == 1) && (rz == 0 || rz == 1)) {
         for (int y = fy + 1; y < cy; y++) {
            setBlk(c, lx, y, lz, (y == fy + 1 || y == cy - 1) ? Redstone319.CHISELED_STONEBRICK : Blocks.STONEBRICK);
         }
         return;
      }

      // Partition wall along rx == 0 with wide bay doorway at rz = 4..7
      if (rx == 0 && (rh & 3L) != 0L && (rz < 4 || rz > 7)) {
         for (int y = fy + 1; y < cy; y++) {
            setBlk(c, lx, y, lz, y == fy + 1 ? Redstone319.POLISHED_ANDESITE : Blocks.STONEBRICK);
         }
         return;
      }

      // Well-placed 3D Desks, 3D Lockers & 3D Water Coolers in Level 1 Warehouse
      if ((rx == 3 || rx == 4) && rz == 2) {
         setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_DESK);
      } else if ((rx == 8 || rx == 9) && rz == 2) {
         setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_LOCKER_LOW);
         setBlk(c, lx, fy + 2, lz, BackroomsStalkerMod.B_LOCKER_HIGH);
      } else if (rx == 2 && rz == 9 && (rh & 1L) == 0L) {
         setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_WATER_COOLER);
         setBlk(c, lx, fy + 2, lz, BackroomsStalkerMod.B_COOLER_JUG);
      } else if ((rh & 3L) == 0L && (rx == 8 || rx == 9 || rx == 10) && rz == 9) {
         // Warehouse supply rack
         setBlk(c, lx, fy + 1, lz, Blocks.BOOKSHELF);
         setBlk(c, lx, fy + 2, lz, Blocks.PLANKS);
      }
   }

   // ====================================================================================================
   // LEVEL 2 — "PIPE DREAMS" (Y = 30..36)
   // ====================================================================================================
   private void generateLevel2(Chunk c, int lx, int lz, int wx, int wz) {
      int fy = L2_FLOOR_Y;
      int cy = L2_CEIL_Y;

      setBlk(c, lx, fy - 1, lz, Blocks.BEDROCK);
      int rx = Math.floorMod(wx, 12);
      int rz = Math.floorMod(wz, 12);

      setBlk(c, lx, fy, lz, (rx == 5 || rx == 6 || rz == 5 || rz == 6) ? Redstone319.POLISHED_ANDESITE : Blocks.COBBLE);
      for (int y = fy + 1; y < cy; y++) {
         setBlk(c, lx, y, lz, Blocks.AIR);
      }

      boolean light = (rx == 5 || rx == 6) && (rz == 5 || rz == 6);
      setBlk(c, lx, cy, lz, light ? Blocks.GLOWSTONE : Blocks.STONEBRICK);
      setBlk(c, lx, cy + 1, lz, Blocks.BEDROCK);

      if (placeLobbyOrExit(c, lx, lz, wx, wz, fy, cy)) {
         return;
      }

      boolean corrX = rz >= 3 && rz <= 8;
      boolean corrZ = rx >= 3 && rx <= 8;
      if (!corrX && !corrZ) {
         for (int y = fy + 1; y < cy; y++) {
            setBlk(c, lx, y, lz, y == fy + 1 ? Blocks.STONEBRICK : Blocks.BRICKS);
         }
         return;
      }

      // Overhead steam pipe conduits along tunnel ceiling edges
      if ((rx == 3 || rx == 8) && !corrX || (rz == 3 || rz == 8) && !corrZ) {
         setBlk(c, lx, cy - 1, lz, Blocks.LOG_SPRUCE);
      }

      // Well-placed 3D Hiding Lockers, 3D Maintenance Desks & 3D Water Coolers recessed against tunnel walls
      if (rx == 3 && rz == 1) {
         setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_LOCKER_LOW);
         setBlk(c, lx, fy + 2, lz, BackroomsStalkerMod.B_LOCKER_HIGH);
      } else if ((rx == 7 || rx == 8) && rz == 3 && !corrZ) {
         setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_DESK);
      } else if (rx == 8 && rz == 1) {
         setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_WATER_COOLER);
         setBlk(c, lx, fy + 2, lz, BackroomsStalkerMod.B_COOLER_JUG);
      }
   }

   // ====================================================================================================
   // LEVEL 37 — "THE POOLROOMS" (Y = 82..90): White Quartz Atrium, Walkways, Pools & 3D Furniture
   // ====================================================================================================
   private void generatePoolrooms(Chunk c, int lx, int lz, int wx, int wz) {
      int fy = POOL_FLOOR_Y;
      int cy = POOL_CEIL_Y;

      setBlk(c, lx, fy - 2, lz, Blocks.BEDROCK);
      setBlk(c, lx, fy - 1, lz, Blocks.QUARTZ_BLOCK);

      int rx = Math.floorMod(wx, 12);
      int rz = Math.floorMod(wz, 12);
      long rh = cellHash(this.seed ^ 0x37373737L, Math.floorDiv(wx, 12), Math.floorDiv(wz, 12));

      boolean dryWalkway = (Math.abs(wx) <= 5 && Math.abs(wz) <= 5)
         || isExitVestibule(wx, wz)
         || rx <= 3 || rx >= 9 || rz <= 3 || rz >= 9;
      setBlk(c, lx, fy, lz, dryWalkway ? Blocks.QUARTZ_BLOCK : Blocks.WATER);

      for (int y = fy + 1; y < cy; y++) {
         setBlk(c, lx, y, lz, Blocks.AIR);
      }

      boolean skylight = rx >= 4 && rx <= 7 && rz >= 4 && rz <= 7;
      setBlk(c, lx, cy, lz, skylight ? Redstone319.SEA_LANTERN : Blocks.QUARTZ_BLOCK);

      if (placeLobbyOrExit(c, lx, lz, wx, wz, fy, cy)) {
         return;
      }

      boolean pillar = (rx == 3 || rx == 9) && (rz == 3 || rz == 9);
      boolean wall = (rx == 0 && (rh & 3L) == 0L && (rz < 4 || rz > 7))
         || (rz == 0 && ((rh >>> 2) & 3L) == 0L && (rx < 4 || rx > 7));
      if (pillar || wall) {
         for (int y = fy + 1; y < cy; y++) {
            setBlk(c, lx, y, lz, Blocks.QUARTZ_BLOCK);
         }
         return;
      }

      // Poolside 3D Locker Cabanas, 3D Tables & 3D Almond Water Coolers on the dry walkways
      if (rx == 1 && (rz == 2 || rz == 3) && (rh & 3L) == 0L) {
         setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_DESK);
      } else if (rx == 2 && rz == 1 && (rh & 3L) == 1L) {
         setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_LOCKER_LOW);
         setBlk(c, lx, fy + 2, lz, BackroomsStalkerMod.B_LOCKER_HIGH);
      } else if (rx == 1 && rz == 8 && (rh & 3L) == 2L) {
         setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_WATER_COOLER);
         setBlk(c, lx, fy + 2, lz, BackroomsStalkerMod.B_COOLER_JUG);
      }
   }

   // ====================================================================================================
   // LEVEL "!" — "RUN FOR YOUR LIFE" (Y = 14..20): Crimson Hospital Corridor with Desks, Lockers & Exits
   // ====================================================================================================
   private void generateLevelRun(Chunk c, int lx, int lz, int wx, int wz) {
      int fy = RUN_FLOOR_Y;
      int cy = RUN_CEIL_Y;

      setBlk(c, lx, fy - 1, lz, Blocks.BEDROCK);
      int rx = Math.floorMod(wx + 12, 24) - 12;
      int rz = Math.floorMod(wz, 12);

      // Crimson carpet center runner with white quartz hospital borders
      boolean redRunner = Math.abs(rx) <= 2 || (rz >= 4 && rz <= 7);
      setBlk(c, lx, fy, lz, redRunner ? (Blocks.WOOL0 + 14) : Blocks.QUARTZ_BLOCK);
      for (int y = fy + 1; y < cy; y++) {
         setBlk(c, lx, y, lz, Blocks.AIR);
      }

      setBlk(c, lx, cy, lz, (rz == 5 || rz == 6) ? Blocks.GLOWSTONE : Blocks.BRICKS);
      setBlk(c, lx, cy + 1, lz, Blocks.BEDROCK);

      if (placeLobbyOrExit(c, lx, lz, wx, wz, fy, cy)) {
         return;
      }

      boolean inHallX = Math.abs(rx) <= 4;
      boolean inHallZ = rz >= 3 && rz <= 8;

      if (!inHallX && !inHallZ) {
         for (int y = fy + 1; y < cy; y++) {
            setBlk(c, lx, y, lz, y == fy + 1 ? Blocks.QUARTZ_BLOCK : Blocks.BRICKS);
         }
         return;
      }

      // Hospital Corridor 3D Desks, 3D Lockers, 3D Water Coolers & Emergency Redstone Torches along the side walls (|rx| == 4)
      if (rx == -4 && (rz == 1 || rz == 2)) {
         setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_DESK);
      } else if (rx == 4 && rz == 1) {
         setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_LOCKER_LOW);
         setBlk(c, lx, fy + 2, lz, BackroomsStalkerMod.B_LOCKER_HIGH);
      } else if (rx == 4 && rz == 10) {
         setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_WATER_COOLER);
         setBlk(c, lx, fy + 2, lz, BackroomsStalkerMod.B_COOLER_JUG);
      } else if (Math.abs(rx) == 4 && rz == 0) {
         setBlk(c, lx, fy + 1, lz, Redstone319.REDSTONE_TORCH);
      }
   }
}
