package bc;

/**
 * Completely Randomized Multi-Level Backrooms World Generator (v2.3).
 * <p>
 * Generates 5 separate, full-world Backrooms levels at distinct vertical strata, where EVERY 12x12 room cell
 * is seed-randomized across dozens of architectural archetypes, wall/doorway layouts, ceiling styles,
 *furniture arrangements, Poolrooms 3D Waterslides ({@link BackroomsStalkerMod#B_POOL_SLIDE}),
 * 3D Office Desks ({@link BackroomsStalkerMod#B_DESK}), 3D Swivel Chairs ({@link BackroomsStalkerMod#B_OFFICE_CHAIR}),
 * 3D Steel Hiding Lockers ({@link BackroomsStalkerMod#B_LOCKER_LOW}), 3D Almond Water Coolers ({@link BackroomsStalkerMod#B_WATER_COOLER}),
 * 3D Glowing Vending Machines ({@link BackroomsStalkerMod#B_VENDING}), 3D Steam Valve Pipes ({@link BackroomsStalkerMod#B_VALVE_PIPE}),
 * and randomized Illuminated 3D Level Exit Doors ({@link BackroomsStalkerMod#B_EXIT_LOW}).
 */
public class BackroomsGen extends Gen {
   public static final int FLOOR_Y = 64;
   public static final int CEIL_Y = 71;
   public static final int CARPET_BLOCK = Blocks.WOOL0 + 4;

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

   public static long cellHash(long seed, int x, int z) {
      long h = seed ^ (x * 341873128712L) ^ (z * 132897987541L);
      h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
      h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
      return h ^ (h >>> 31);
   }

   private static long levelSalt(int level) {
      return (level + 1L) * 0x9E3779B97F4A7C15L;
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

   /**
    * Computes the randomized Exit Room cell {@code (roomX, roomZ)} inside a 4x4 room macro-sector {@code (secX, secZ)}
    * for the given world {@code seed} and {@code level}. Never picks room (0, 0) so the spawn room stays open.
    */
   public static int[] exitRoomInSector(long seed, int level, int secX, int secZ) {
      long h = cellHash(seed ^ levelSalt(level) ^ 0xE817D00DL, secX, secZ);
      int lx = (int)Math.floorMod(h, 4L);
      int lz = (int)Math.floorMod(h >>> 8, 4L);
      int roomX = secX * 4 + lx;
      int roomZ = secZ * 4 + lz;
      if (roomX == 0 && roomZ == 0) {
         roomZ = 1;
      }
      return new int[]{roomX, roomZ};
   }

   /** Returns the world coordinates {@code (wx, wz)} of the 3D Exit Door inside a macro-sector. */
   public static int[] exitDoorXZInSector(long seed, int level, int secX, int secZ) {
      int[] rm = exitRoomInSector(seed, level, secX, secZ);
      return new int[]{rm[0] * 12 + 6, rm[1] * 12 + 6};
   }

   /** Finds the nearest randomized Level Exit Door {@code (wx, wz)} for a player at {@code (px, pz)}. */
   public static int[] nearestExitXZ(long seed, int level, double px, double pz) {
      int curSecX = (int)Math.floor(px / 48.0);
      int curSecZ = (int)Math.floor(pz / 48.0);
      int[] best = null;
      double bestD2 = Double.MAX_VALUE;
      for (int dx = -1; dx <= 1; dx++) {
         for (int dz = -1; dz <= 1; dz++) {
            int[] cand = exitDoorXZInSector(seed, level, curSecX + dx, curSecZ + dz);
            double d2 = (cand[0] - px) * (cand[0] - px) + (cand[1] - pz) * (cand[1] - pz);
            if (d2 < bestD2) {
               bestD2 = d2;
               best = cand;
            }
         }
      }
      return best != null ? best : new int[]{6, 18};
   }

   /** Legacy overload kept for callers without seed/level. */
   public static int[] nearestExitXZ(double px, double pz) {
      return nearestExitXZ(0L, LEVEL_0, px, pz);
   }

   private boolean isExitRoom(int level, int roomX, int roomZ) {
      int secX = Math.floorDiv(roomX, 4);
      int secZ = Math.floorDiv(roomZ, 4);
      int[] ex = exitRoomInSector(this.seed, level, secX, secZ);
      return ex[0] == roomX && ex[1] == roomZ;
   }

   public static int zoneAt(long seed, int wx, int wz) {
      int rx = Math.floorDiv(wx, 24);
      int rz = Math.floorDiv(wz, 24);
      long h = cellHash(seed ^ 0x5A17C9E3L, rx, rz);
      return (Math.abs(wx) <= 8 && Math.abs(wz) <= 8) ? ZONE_CLASSIC : (int)Math.floorMod(h, 6L);
   }

   private static void setBlk(Chunk c, int lx, int y, int lz, int id) {
      if (y >= 0 && y < Chunk.H) {
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

   /**
    * Places the Starting Spawn Room at (wx in -4..4, wz in -4..4) or the Randomized Exit Vestibule
    * if the current room (roomX, roomZ) is the sector's Exit Room.
    */
   private boolean placeSpawnOrExitRoom(Chunk c, int lx, int lz, int wx, int wz, int level, int fy, int cy, int wallBlock, int trimBlock) {
      int roomX = Math.floorDiv(wx, 12);
      int roomZ = Math.floorDiv(wz, 12);
      int rx = Math.floorMod(wx, 12);
      int rz = Math.floorMod(wz, 12);

      // 1. Randomized Exit Vestibule Room for this 4x4 sector
      if (isExitRoom(level, roomX, roomZ) && rx >= 3 && rx <= 9 && rz >= 3 && rz <= 9) {
         boolean carpetPad = rx >= 4 && rx <= 8 && rz >= 4 && rz <= 8;
         setBlk(c, lx, fy, lz, carpetPad ? (Blocks.WOOL0 + 5) : Blocks.QUARTZ_BLOCK);
         setBlk(c, lx, cy, lz, Redstone319.SEA_LANTERN);
         if (rx == 6 && rz == 6) {
            setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_EXIT_LOW);
            setBlk(c, lx, fy + 2, lz, BackroomsStalkerMod.B_EXIT_HIGH);
         } else if ((rx == 5 || rx == 7) && rz == 6) {
            for (int y = fy + 1; y <= fy + 3; y++) {
               setBlk(c, lx, y, lz, Blocks.QUARTZ_BLOCK);
            }
         } else if (rx == 4 && rz == 4) {
            setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_VENDING);
         } else if (rx == 8 && rz == 4) {
            setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_WATER_COOLER);
            setBlk(c, lx, fy + 2, lz, BackroomsStalkerMod.B_COOLER_JUG);
         }
         return true;
      }

      // 2. Starting Lobby Room around (0, 0) on every Level so spawn is always clear and well-equipped
      if (Math.abs(wx) <= 4 && Math.abs(wz) <= 4) {
         if (wx == 3 && (wz == 0 || wz == 1)) {
            setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_DESK);
         } else if (wx == 4 && wz == 0) {
            setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_OFFICE_CHAIR);
         } else if ((wx == -2 || wx == -1) && wz == -4) {
            setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_LOCKER_LOW);
            setBlk(c, lx, fy + 2, lz, BackroomsStalkerMod.B_LOCKER_HIGH);
         } else if (wx == -3 && wz == 0) {
            setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_WATER_COOLER);
            setBlk(c, lx, fy + 2, lz, BackroomsStalkerMod.B_COOLER_JUG);
         } else if (wx == -3 && wz == 2) {
            setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_VENDING);
         }
         return true;
      }

      // 3. Randomized Shared Room Boundary Walls (rx == 0 or rz == 0) with randomized doorway positions & windows
      if (rx == 0 || rz == 0) {
         long wxHash = cellHash(this.seed ^ levelSalt(level) ^ 0xA1B2C3D4L, roomX, roomZ);
         if (rx == 0 && rz == 0) {
            for (int y = fy + 1; y < cy; y++) {
               setBlk(c, lx, y, lz, trimBlock);
            }
            return true;
         }
         if (rx == 0) {
            boolean hasWall = (wxHash & 3L) != 0L; // 75% wall, 25% wide-open merged double room
            if (!hasWall) {
               return false;
            }
            int doorCenter = 4 + (int)Math.floorMod(wxHash >>> 4, 5L); // Randomized doorway at 4, 5, 6, 7, or 8!
            boolean isDoor = Math.abs(rz - doorCenter) <= 1;
            boolean isWin = !isDoor && ((wxHash & 16L) != 0L) && (rz == 2 || rz == 10);
            if (isDoor) {
               setBlk(c, lx, fy, lz, trimBlock);
               for (int y = fy + 5; y < cy; y++) {
                  setBlk(c, lx, y, lz, wallBlock);
               }
            } else {
               for (int y = fy + 1; y < cy; y++) {
                  if (isWin && (y == fy + 2 || y == fy + 3)) {
                     setBlk(c, lx, y, lz, Blocks.GLASS);
                  } else if (y == fy + 1 || y == cy - 1) {
                     setBlk(c, lx, y, lz, trimBlock);
                  } else {
                     setBlk(c, lx, y, lz, wallBlock);
                  }
               }
            }
            return true;
         }
         if (rz == 0) {
            boolean hasWall = ((wxHash >>> 2) & 3L) != 0L;
            if (!hasWall) {
               return false;
            }
            int doorCenter = 4 + (int)Math.floorMod(wxHash >>> 9, 5L); // Randomized doorway at 4, 5, 6, 7, or 8!
            boolean isDoor = Math.abs(rx - doorCenter) <= 1;
            boolean isWin = !isDoor && ((wxHash & 32L) != 0L) && (rx == 2 || rx == 10);
            if (isDoor) {
               setBlk(c, lx, fy, lz, trimBlock);
               for (int y = fy + 5; y < cy; y++) {
                  setBlk(c, lx, y, lz, wallBlock);
               }
            } else {
               for (int y = fy + 1; y < cy; y++) {
                  if (isWin && (y == fy + 2 || y == fy + 3)) {
                     setBlk(c, lx, y, lz, Blocks.GLASS);
                  } else if (y == fy + 1 || y == cy - 1) {
                     setBlk(c, lx, y, lz, trimBlock);
                  } else {
                     setBlk(c, lx, y, lz, wallBlock);
                  }
               }
            }
            return true;
         }
      }
      return false;
   }

   // ====================================================================================================
   // LEVEL 0 — "THE LOBBY" (Y = 64..71): 8 Completely Randomized Room Archetypes
   // ====================================================================================================
   private void generateLevel0(Chunk c, int lx, int lz, int wx, int wz) {
      int fy = L0_FLOOR_Y;
      int cy = L0_CEIL_Y;
      setBlk(c, lx, fy - 1, lz, Blocks.BEDROCK);

      int rx = Math.floorMod(wx, 12);
      int rz = Math.floorMod(wz, 12);
      int roomX = Math.floorDiv(wx, 12);
      int roomZ = Math.floorDiv(wz, 12);
      long rh = cellHash(this.seed ^ levelSalt(LEVEL_0), roomX, roomZ);
      int archetype = (int)Math.floorMod(rh, 8L);
      int rot = (int)Math.floorMod(rh >>> 6, 4L);

      // Rotated room coordinates (urx, urz in 1..11) so every room layout can face any of the 4 cardinal orientations!
      int urx = rx;
      int urz = rz;
      if (rot == 1) {
         urx = rz;
         urz = 12 - rx;
      } else if (rot == 2) {
         urx = 12 - rx;
         urz = 12 - rz;
      } else if (rot == 3) {
         urx = 12 - rz;
         urz = rx;
      }

      // Randomized carpet pattern per room archetype
      boolean border = (rx == 1 || rx == 11 || rz == 1 || rz == 11) && rx != 0 && rz != 0;
      int floorId = CARPET_BLOCK;
      if (archetype == 1 && (rx + rz) % 2 == 0 && rx >= 3 && rx <= 9 && rz >= 3 && rz <= 9) {
         floorId = Blocks313.PLANKS_BIRCH; // Breakroom parquet floor
      } else if (archetype == 5 && ((rh >>> 12) & 1L) == 0L && rx == 6 && rz == 6) {
         floorId = Blocks.WATER; // Damp puddle in moldering room
      } else if (border) {
         floorId = Blocks.WOOL0 + 12;
      }
      setBlk(c, lx, fy, lz, floorId);

      for (int y = fy + 1; y < cy; y++) {
         setBlk(c, lx, y, lz, Blocks.AIR);
      }

      // Randomized ceiling lighting style
      boolean light;
      if (archetype == 2 || archetype == 4) {
         light = (rx == 3 || rx == 9) && (rz == 3 || rz == 9) || (rx >= 5 && rx <= 7 && rz >= 5 && rz <= 7);
      } else if (archetype == 5) {
         light = rx == 6 && rz == 6 && ((rh & 8L) == 0L); // Flickering/broken lights in decay room
      } else {
         light = (rx == 4 || rx == 8) && (rz == 4 || rz == 8);
      }
      boolean beam = rx == 0 || rz == 0 || rx == 6 || rz == 6;
      setBlk(c, lx, cy, lz, light ? Blocks.GLOWSTONE : (beam ? Blocks313.PLANKS_BIRCH : Blocks.SANDSTONE));
      setBlk(c, lx, cy + 1, lz, Blocks.BEDROCK);

      if (placeSpawnOrExitRoom(c, lx, lz, wx, wz, LEVEL_0, fy, cy, Blocks.SANDSTONE, Blocks313.PLANKS_BIRCH)) {
         return;
      }

      switch (archetype) {
         case 0: // Segmented Cubicle Office (Half-walls, 3D Desks, 3D Swivel Chairs, 3D Lockers)
            if (urx == 6 && urz >= 3 && urz <= 8 && urz != 5) {
               setBlk(c, lx, fy + 1, lz, Blocks.PLANKS);
               setBlk(c, lx, fy + 2, lz, urz == 6 ? Blocks.GLASS : Blocks.SANDSTONE);
            } else if ((urx == 4 || urx == 5) && (urz == 4 || urz == 8)) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_DESK);
            } else if (urx == 3 && (urz == 4 || urz == 8)) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_OFFICE_CHAIR);
            } else if ((urx == 9 || urx == 10) && urz == 2) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_LOCKER_LOW);
               setBlk(c, lx, fy + 2, lz, BackroomsStalkerMod.B_LOCKER_HIGH);
            }
            break;

         case 1: // Executive Lounge & Breakroom (3D Vending Machine, 3D Water Cooler, Conference Desks & Chairs)
            if ((urx == 5 || urx == 6 || urx == 7) && urz == 6) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_DESK);
            } else if ((urx == 5 || urx == 7) && (urz == 5 || urz == 7)) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_OFFICE_CHAIR);
            } else if (urx == 2 && urz == 2) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_VENDING);
            } else if (urx == 3 && urz == 2) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_WATER_COOLER);
               setBlk(c, lx, fy + 2, lz, BackroomsStalkerMod.B_COOLER_JUG);
            } else if (urx == 10 && urz == 2) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_LOCKER_LOW);
               setBlk(c, lx, fy + 2, lz, BackroomsStalkerMod.B_LOCKER_HIGH);
            }
            break;

         case 2: // Fluted Pillar Colonnade Gallery
            if ((urx == 3 || urx == 9) && (urz == 3 || urz == 9)) {
               for (int y = fy + 1; y < cy; y++) {
                  setBlk(c, lx, y, lz, Blocks.LOG_BIRCH);
               }
            } else if (urx == 6 && urz == 2) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_VENDING);
            } else if ((urx == 2 || urx == 3) && urz == 10) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_DESK);
            }
            break;

         case 3: // Archive & Filing Vault (Rows of Bookshelves, 3D Lockers, and Archivist's 3D Desk)
            if ((urx == 3 || urx == 8) && (urz >= 3 && urz <= 7)) {
               if (urz == 3 || urz == 7) {
                  setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_LOCKER_LOW);
                  setBlk(c, lx, fy + 2, lz, BackroomsStalkerMod.B_LOCKER_HIGH);
               } else {
                  setBlk(c, lx, fy + 1, lz, Blocks.BOOKSHELF);
                  setBlk(c, lx, fy + 2, lz, Blocks.BOOKSHELF);
                  setBlk(c, lx, fy + 3, lz, Blocks.PLANKS);
               }
            } else if ((urx == 5 || urx == 6) && urz == 10) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_DESK);
            } else if (urx == 6 && urz == 11) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_OFFICE_CHAIR);
            }
            break;

         case 4: // Sunken Carpet Pit / Boardroom
            if ((urx == 5 || urx == 7) && (urz >= 4 && urz <= 8)) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_DESK);
            } else if ((urx == 4 || urx == 8) && (urz == 5 || urz == 7)) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_OFFICE_CHAIR);
            } else if (urx == 2 && urz == 2) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_WATER_COOLER);
               setBlk(c, lx, fy + 2, lz, BackroomsStalkerMod.B_COOLER_JUG);
            }
            break;

         case 5: // Moldering / Decayed Wallpaper Room
            if (urx == 4 && urz == 4) {
               for (int y = fy + 1; y < cy; y++) {
                  setBlk(c, lx, y, lz, Redstone319.MOSSY_STONEBRICK);
               }
            } else if ((urx == 8 || urx == 9) && urz == 3) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_DESK);
            } else if (urx == 2 && (urz == 8 || urz == 9)) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_LOCKER_LOW);
               setBlk(c, lx, fy + 2, lz, BackroomsStalkerMod.B_LOCKER_HIGH);
            }
            break;

         case 6: // Zig-Zag Baffle Hallway with Glass Observation Partition
            if ((urz == 4 && urx >= 2 && urx <= 7) || (urz == 8 && urx >= 5 && urx <= 10)) {
               for (int y = fy + 1; y < cy; y++) {
                  setBlk(c, lx, y, lz, (y == fy + 2 || y == fy + 3) && (urx == 4 || urx == 8) ? Blocks.GLASS : Blocks.SANDSTONE);
               }
            } else if (urx == 9 && urz == 2) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_DESK);
            } else if (urx == 2 && urz == 10) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_LOCKER_LOW);
               setBlk(c, lx, fy + 2, lz, BackroomsStalkerMod.B_LOCKER_HIGH);
            }
            break;

         case 7: // Reception Atrium with Wrap-Around 3D Desk Counter
         default:
            if ((urx >= 4 && urx <= 8 && urz == 4) || (urx == 4 && urz >= 4 && urz <= 7)) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_DESK);
            } else if (urx == 6 && urz == 6) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_OFFICE_CHAIR);
            } else if (urx == 10 && urz == 2) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_VENDING);
            } else if (urx == 10 && urz == 3) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_WATER_COOLER);
               setBlk(c, lx, fy + 2, lz, BackroomsStalkerMod.B_COOLER_JUG);
            }
            break;
      }
   }

   // ====================================================================================================
   // LEVEL 37 — "THE POOLROOMS" (Y = 82..90): 8 Randomized Archetypes with 3D Fiberglass Waterslides!
   // ====================================================================================================
   private void generatePoolrooms(Chunk c, int lx, int lz, int wx, int wz) {
      int fy = POOL_FLOOR_Y;
      int cy = POOL_CEIL_Y;

      setBlk(c, lx, fy - 2, lz, Blocks.BEDROCK);
      setBlk(c, lx, fy - 1, lz, Blocks.QUARTZ_BLOCK);

      int rx = Math.floorMod(wx, 12);
      int rz = Math.floorMod(wz, 12);
      int roomX = Math.floorDiv(wx, 12);
      int roomZ = Math.floorDiv(wz, 12);
      long rh = cellHash(this.seed ^ levelSalt(LEVEL_POOLROOMS), roomX, roomZ);
      int archetype = (int)Math.floorMod(rh, 8L);

      // Default floor & air clear
      setBlk(c, lx, fy, lz, Blocks.QUARTZ_BLOCK);
      for (int y = fy + 1; y < cy; y++) {
         setBlk(c, lx, y, lz, Blocks.AIR);
      }

      boolean skylight = (rx >= 4 && rx <= 8 && rz >= 4 && rz <= 8) && ((rx + rz) % 2 == 0 || archetype <= 1);
      setBlk(c, lx, cy, lz, skylight ? Redstone319.SEA_LANTERN : Blocks.QUARTZ_BLOCK);

      if (placeSpawnOrExitRoom(c, lx, lz, wx, wz, LEVEL_POOLROOMS, fy, cy, Blocks.QUARTZ_BLOCK, Blocks.QUARTZ_BLOCK)) {
         return;
      }

      switch (archetype) {
         case 0: // Giant Fiberglass Waterslide Tower & Splash Lagoon!
            // Splash Pool at rz = 6..10, rx = 3..9
            if (rx >= 3 && rx <= 9 && rz >= 6 && rz <= 10) {
               setBlk(c, lx, fy - 1, lz, (rx == 6 && rz == 8) ? Redstone319.SEA_LANTERN : Blocks.QUARTZ_BLOCK);
               setBlk(c, lx, fy, lz, Blocks.WATER);
            }
            // Waterslide Launch Tower at (rx = 5..7, rz = 2) with Ladder at (rx = 6, rz = 1)
            if (rx >= 5 && rx <= 7 && rz == 2) {
               for (int y = fy + 1; y <= fy + 4; y++) {
                  setBlk(c, lx, y, lz, y == fy + 4 ? Redstone319.SEA_LANTERN : Blocks.QUARTZ_BLOCK);
               }
            } else if (rx == 6 && rz == 1) {
               for (int y = fy + 1; y <= fy + 4; y++) {
                  setBlk(c, lx, y, lz, Blocks.LADDER);
               }
            }
            // Descending 3D Waterslide Chute (B_POOL_SLIDE) from (rx=6, rz=3, y=fy+4) -> (rz=4, y=fy+3) -> (rz=5, y=fy+2) -> (rz=6, y=fy+1) into the pool!
            if (rx == 6 && rz >= 3 && rz <= 6) {
               int slideY = fy + (7 - rz); // rz=3 -> fy+4, rz=4 -> fy+3, rz=5 -> fy+2, rz=6 -> fy+1
               for (int y = fy + 1; y < slideY; y++) {
                  if (rz <= 4) {
                     setBlk(c, lx, y, lz, Blocks.QUARTZ_BLOCK); // Support column under high slide
                  }
               }
               setBlk(c, lx, slideY, lz, BackroomsStalkerMod.B_POOL_SLIDE);
            } else if (rx == 2 && rz == 2) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_LOCKER_LOW);
               setBlk(c, lx, fy + 2, lz, BackroomsStalkerMod.B_LOCKER_HIGH);
            } else if (rx == 10 && rz == 2) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_WATER_COOLER);
               setBlk(c, lx, fy + 2, lz, BackroomsStalkerMod.B_COOLER_JUG);
            }
            break;

         case 1: // Twin Racing 3D Waterslides (rx = 4 and rx = 8) into Central Lagoon!
            if (rx >= 3 && rx <= 9 && rz >= 6 && rz <= 10) {
               setBlk(c, lx, fy - 1, lz, ((rx + rz) & 1) == 0 ? Redstone319.SEA_LANTERN : Blocks.QUARTZ_BLOCK);
               setBlk(c, lx, fy, lz, Blocks.WATER);
            }
            if ((rx >= 4 && rx <= 8) && rz == 2) {
               for (int y = fy + 1; y <= fy + 3; y++) {
                  setBlk(c, lx, y, lz, Blocks.QUARTZ_BLOCK);
               }
            } else if (rx == 6 && rz == 1) {
               for (int y = fy + 1; y <= fy + 3; y++) {
                  setBlk(c, lx, y, lz, Blocks.LADDER);
               }
            }
            if ((rx == 4 || rx == 8) && rz >= 3 && rz <= 5) {
               int slideY = fy + (6 - rz); // rz=3 -> fy+3, rz=4 -> fy+2, rz=5 -> fy+1
               if (rz == 3) {
                  setBlk(c, lx, fy + 1, lz, Blocks.QUARTZ_BLOCK);
                  setBlk(c, lx, fy + 2, lz, Blocks.QUARTZ_BLOCK);
               } else if (rz == 4) {
                  setBlk(c, lx, fy + 1, lz, Blocks.QUARTZ_BLOCK);
               }
               setBlk(c, lx, slideY, lz, BackroomsStalkerMod.B_POOL_SLIDE);
            }
            break;

         case 2: // Sunken Thermal Spa / Jacuzzi Rotunda & Poolside Lounge
            if (rx >= 4 && rx <= 8 && rz >= 4 && rz <= 8) {
               setBlk(c, lx, fy - 1, lz, Redstone319.SEA_LANTERN);
               setBlk(c, lx, fy, lz, Blocks.WATER);
            } else if ((rx == 2 || rx == 3) && rz == 2) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_DESK);
            } else if (rx == 2 && rz == 3) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_OFFICE_CHAIR);
            } else if (rx == 10 && rz == 2) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_VENDING);
            } else if (rx == 10 && rz == 3) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_WATER_COOLER);
               setBlk(c, lx, fy + 2, lz, BackroomsStalkerMod.B_COOLER_JUG);
            }
            break;

         case 3: // Stepped Waterfall Cascade & Aqueduct
            if (rx >= 4 && rx <= 8 && rz >= 3 && rz <= 9) {
               setBlk(c, lx, fy, lz, Blocks.WATER);
               if (rz == 3) {
                  for (int y = fy + 1; y <= fy + 4; y++) {
                     setBlk(c, lx, y, lz, rx == 6 ? Blocks.WATER : Redstone319.SEA_LANTERN);
                  }
               }
            } else if (rx == 2 && (rz == 5 || rz == 6)) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_LOCKER_LOW);
               setBlk(c, lx, fy + 2, lz, BackroomsStalkerMod.B_LOCKER_HIGH);
            }
            break;

         case 4: // Tiled Poolside Locker & Changing Cabana
            if ((rx == 3 || rx == 4 || rx == 8 || rx == 9) && rz == 2) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_LOCKER_LOW);
               setBlk(c, lx, fy + 2, lz, BackroomsStalkerMod.B_LOCKER_HIGH);
            } else if (rx == 6 && rz == 2) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_VENDING);
            } else if (rx == 6 && rz == 10) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_WATER_COOLER);
               setBlk(c, lx, fy + 2, lz, BackroomsStalkerMod.B_COOLER_JUG);
            } else if ((rx == 3 || rx == 9) && rz == 8) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_DESK);
            }
            break;

         case 5: // Elevated Quartz Bridge Over Deep Pool with Mini Waterslide Drop
            if (rx >= 2 && rx <= 10 && rz >= 2 && rz <= 10 && rx != 6 && rz != 6) {
               setBlk(c, lx, fy, lz, Blocks.WATER);
            }
            if ((rx == 3 || rx == 9) && (rz == 3 || rz == 9)) {
               for (int y = fy + 1; y < cy; y++) {
                  setBlk(c, lx, y, lz, Blocks.QUARTZ_BLOCK);
               }
            } else if (rx == 6 && rz == 8) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_POOL_SLIDE);
            }
            break;

         case 6: // Lazy River Winding Channel & Island Pavilion
            boolean river = (rx == 3 || rx == 4 || rx == 8 || rx == 9 || rz == 3 || rz == 4 || rz == 8 || rz == 9)
               && rx >= 3 && rx <= 9 && rz >= 3 && rz <= 9 && rx != 6;
            if (river) {
               setBlk(c, lx, fy, lz, Blocks.WATER);
            } else if (rx == 6 && rz == 6) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_DESK);
            } else if (rx == 2 && rz == 2) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_LOCKER_LOW);
               setBlk(c, lx, fy + 2, lz, BackroomsStalkerMod.B_LOCKER_HIGH);
            }
            break;

         case 7: // Sunlit Skylight Rotunda & Fountain
         default:
            if (rx >= 4 && rx <= 8 && rz >= 4 && rz <= 8) {
               if (rx == 6 && rz == 6) {
                  for (int y = fy + 1; y <= fy + 3; y++) {
                     setBlk(c, lx, y, lz, y == fy + 3 ? Blocks.WATER : Redstone319.SEA_LANTERN);
                  }
               } else {
                  setBlk(c, lx, fy, lz, Blocks.WATER);
               }
            } else if (rx == 2 && rz == 6) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_POOL_SLIDE);
            }
            break;
      }
   }

   // ====================================================================================================
   // LEVEL 1 — "THE HABITABLE WAREHOUSE" (Y = 46..53): 7 Randomized Industrial Archetypes
   // ====================================================================================================
   private void generateLevel1(Chunk c, int lx, int lz, int wx, int wz) {
      int fy = L1_FLOOR_Y;
      int cy = L1_CEIL_Y;
      setBlk(c, lx, fy - 1, lz, Blocks.BEDROCK);

      int rx = Math.floorMod(wx, 12);
      int rz = Math.floorMod(wz, 12);
      int roomX = Math.floorDiv(wx, 12);
      int roomZ = Math.floorDiv(wz, 12);
      long rh = cellHash(this.seed ^ levelSalt(LEVEL_1), roomX, roomZ);
      int archetype = (int)Math.floorMod(rh, 7L);

      boolean lane = rx == 5 || rx == 6 || rz == 5 || rz == 6;
      setBlk(c, lx, fy, lz, lane ? Redstone319.POLISHED_ANDESITE : Blocks.STONE);
      for (int y = fy + 1; y < cy; y++) {
         setBlk(c, lx, y, lz, Blocks.AIR);
      }

      boolean light = (rx == 4 || rx == 8) && (rz == 4 || rz == 8);
      setBlk(c, lx, cy, lz, light ? Blocks.GLOWSTONE : ((rx == 0 || rz == 0) ? Redstone319.CHISELED_STONEBRICK : Blocks.STONEBRICK));
      setBlk(c, lx, cy + 1, lz, Blocks.BEDROCK);

      if (placeSpawnOrExitRoom(c, lx, lz, wx, wz, LEVEL_1, fy, cy, Blocks.STONEBRICK, Redstone319.POLISHED_ANDESITE)) {
         return;
      }

      switch (archetype) {
         case 0: // Loading Dock & Colored Shipping Containers
            if (rx >= 2 && rx <= 4 && rz >= 2 && rz <= 4) {
               int containerCol = Blocks.WOOL0 + ((int)Math.floorMod(rh >>> 5, 4L) == 0 ? 14 : 11);
               for (int y = fy + 1; y <= fy + 3; y++) {
                  setBlk(c, lx, y, lz, containerCol);
               }
            } else if ((rx == 8 || rx == 9) && rz == 2) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_DESK);
            } else if (rx == 10 && rz == 2) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_OFFICE_CHAIR);
            } else if ((rx == 8 || rx == 9) && rz == 10) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_LOCKER_LOW);
               setBlk(c, lx, fy + 2, lz, BackroomsStalkerMod.B_LOCKER_HIGH);
            }
            break;

         case 1: // Mezzanine Catwalk & Steam Valve Bay
            if (rz == 3 && rx >= 2 && rx <= 10) {
               setBlk(c, lx, fy + 3, lz, Blocks.STONEBRICK);
               if (rx == 2) {
                  for (int y = fy + 1; y <= fy + 3; y++) {
                     setBlk(c, lx, y, lz, Blocks.LADDER);
                  }
               }
            } else if ((rx == 4 || rx == 8) && rz == 2) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_VALVE_PIPE);
               setBlk(c, lx, fy + 2, lz, BackroomsStalkerMod.B_VALVE_PIPE);
            } else if ((rx == 4 || rx == 5) && rz == 9) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_DESK);
            }
            break;

         case 2: // Warehouse Employee Breakroom (Vending Machines, Water Coolers, Desks & Chairs)
            if (rx == 2 && (rz == 2 || rz == 3)) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_VENDING);
            } else if (rx == 2 && rz == 4) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_WATER_COOLER);
               setBlk(c, lx, fy + 2, lz, BackroomsStalkerMod.B_COOLER_JUG);
            } else if ((rx == 7 || rx == 8) && (rz == 3 || rz == 8)) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_DESK);
            } else if (rx == 9 && (rz == 3 || rz == 8)) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_OFFICE_CHAIR);
            }
            break;

         case 3: // High-Bay Supply Rack Labyrinth
            if ((rx == 3 || rx == 9) && rz >= 2 && rz <= 9 && rz != 5 && rz != 6) {
               setBlk(c, lx, fy + 1, lz, Blocks.BOOKSHELF);
               setBlk(c, lx, fy + 2, lz, Blocks.PLANKS);
               setBlk(c, lx, fy + 3, lz, Blocks.BOOKSHELF);
            } else if (rx == 6 && rz == 2) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_LOCKER_LOW);
               setBlk(c, lx, fy + 2, lz, BackroomsStalkerMod.B_LOCKER_HIGH);
            }
            break;

         case 4: // Generator & Transformer Chamber
            if ((rx == 3 || rx == 9) && (rz == 3 || rz == 9)) {
               setBlk(c, lx, fy + 1, lz, Redstone319.REDSTONE_LAMP_LIT);
               setBlk(c, lx, fy + 2, lz, BackroomsStalkerMod.B_VALVE_PIPE);
               setBlk(c, lx, fy + 3, lz, Blocks.STONEBRICK);
            } else if ((rx == 5 || rx == 6) && rz == 2) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_DESK);
            }
            break;

         case 5: // Flooded Warehouse Sump Bay
            if (rx >= 2 && rx <= 10 && rz >= 2 && rz <= 10 && !lane) {
               setBlk(c, lx, fy, lz, Blocks.WATER);
            } else if (rx == 2 && rz == 5) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_VALVE_PIPE);
            } else if (rx == 10 && rz == 5) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_LOCKER_LOW);
               setBlk(c, lx, fy + 2, lz, BackroomsStalkerMod.B_LOCKER_HIGH);
            }
            break;

         case 6: // Security Guard Checkpoint Booth
         default:
            if (rx >= 2 && rx <= 4 && rz >= 2 && rz <= 4) {
               if (rx == 4 || rz == 4) {
                  setBlk(c, lx, fy + 1, lz, Blocks.STONEBRICK);
                  setBlk(c, lx, fy + 2, lz, Blocks.GLASS);
                  setBlk(c, lx, fy + 3, lz, Blocks.STONEBRICK);
               } else if (rx == 3 && rz == 3) {
                  setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_DESK);
               } else if (rx == 2 && rz == 2) {
                  setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_OFFICE_CHAIR);
               }
            } else if (rx == 9 && rz == 2) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_VENDING);
            } else if (rx == 9 && rz == 9) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_LOCKER_LOW);
               setBlk(c, lx, fy + 2, lz, BackroomsStalkerMod.B_LOCKER_HIGH);
            }
            break;
      }
   }

   // ====================================================================================================
   // LEVEL 2 — "PIPE DREAMS" (Y = 30..36): 7 Randomized Steam Tunnel & Boiler Archetypes
   // ====================================================================================================
   private void generateLevel2(Chunk c, int lx, int lz, int wx, int wz) {
      int fy = L2_FLOOR_Y;
      int cy = L2_CEIL_Y;
      setBlk(c, lx, fy - 1, lz, Blocks.BEDROCK);

      int rx = Math.floorMod(wx, 12);
      int rz = Math.floorMod(wz, 12);
      int roomX = Math.floorDiv(wx, 12);
      int roomZ = Math.floorDiv(wz, 12);
      long rh = cellHash(this.seed ^ levelSalt(LEVEL_2), roomX, roomZ);
      int archetype = (int)Math.floorMod(rh, 7L);

      setBlk(c, lx, fy, lz, (rx >= 4 && rx <= 7 || rz >= 4 && rz <= 7) ? Redstone319.POLISHED_ANDESITE : Blocks.COBBLE);
      for (int y = fy + 1; y < cy; y++) {
         setBlk(c, lx, y, lz, Blocks.AIR);
      }
      setBlk(c, lx, cy, lz, ((rx == 5 || rx == 6) && (rz == 5 || rz == 6)) ? Blocks.GLOWSTONE : Blocks.STONEBRICK);
      setBlk(c, lx, cy + 1, lz, Blocks.BEDROCK);

      if (placeSpawnOrExitRoom(c, lx, lz, wx, wz, LEVEL_2, fy, cy, Blocks.BRICKS, Blocks.STONEBRICK)) {
         return;
      }

      switch (archetype) {
         case 0: // Winding Steam Conduit Tunnel with 3D Valve Pipes & Recessed Lockers
            if ((rx < 3 || rx > 9) && (rz < 3 || rz > 9)) {
               for (int y = fy + 1; y < cy; y++) {
                  setBlk(c, lx, y, lz, Blocks.BRICKS);
               }
            } else if ((rx == 3 || rx == 9) && rz == 2) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_VALVE_PIPE);
               setBlk(c, lx, fy + 2, lz, BackroomsStalkerMod.B_VALVE_PIPE);
            } else if (rx == 3 && rz == 9) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_LOCKER_LOW);
               setBlk(c, lx, fy + 2, lz, BackroomsStalkerMod.B_LOCKER_HIGH);
            } else if ((rx == 8 || rx == 9) && rz == 9) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_DESK);
            }
            break;

         case 1: // High-Pressure Boiler Chamber
            if (rx >= 2 && rx <= 4 && rz >= 2 && rz <= 4) {
               for (int y = fy + 1; y <= fy + 4; y++) {
                  setBlk(c, lx, y, lz, (rx == 3 && rz == 3 && y == fy + 2) ? Blocks.GLOWSTONE : Blocks.BRICKS);
               }
            } else if (rx == 5 && rz == 3) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_VALVE_PIPE);
               setBlk(c, lx, fy + 2, lz, BackroomsStalkerMod.B_VALVE_PIPE);
            } else if ((rx == 8 || rx == 9) && rz == 3) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_DESK);
            } else if (rx == 9 && rz == 2) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_OFFICE_CHAIR);
            }
            break;

         case 2: // Valve Control Rotunda
            if ((rx == 4 || rx == 8) && (rz == 4 || rz == 8)) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_VALVE_PIPE);
               setBlk(c, lx, fy + 2, lz, BackroomsStalkerMod.B_VALVE_PIPE);
               setBlk(c, lx, fy + 3, lz, Blocks.LOG_SPRUCE);
            } else if (rx == 2 && rz == 6) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_WATER_COOLER);
               setBlk(c, lx, fy + 2, lz, BackroomsStalkerMod.B_COOLER_JUG);
            } else if (rx == 10 && rz == 6) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_LOCKER_LOW);
               setBlk(c, lx, fy + 2, lz, BackroomsStalkerMod.B_LOCKER_HIGH);
            }
            break;

         case 3: // Catwalk Bridge Over Sludge Canal
            if (rx >= 3 && rx <= 9 && (rz == 2 || rz == 3 || rz == 9 || rz == 10)) {
               setBlk(c, lx, fy, lz, Blocks.WATER);
            } else if (rx == 2 && rz == 5) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_VENDING);
            } else if ((rx == 9 || rx == 10) && rz == 5) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_DESK);
            }
            break;

         case 4: // Ruptured Pipe & Cracked Brick Hall
            if ((rx == 3 || rx == 9) && (rz == 3 || rz == 9)) {
               for (int y = fy + 1; y < cy; y++) {
                  setBlk(c, lx, y, lz, Redstone319.CRACKED_STONEBRICK);
               }
            } else if (rx == 6 && (rz == 2 || rz == 10)) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_VALVE_PIPE);
            } else if ((rx == 2 || rx == 3) && rz == 6) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_DESK);
            }
            break;

         case 5: // Pump Engineer's Control Office
            if (rx == 8 && rz == 2) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_DESK);
            } else if (rx == 9 && rz == 2) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_OFFICE_CHAIR);
            } else if (rx == 2 && rz == 2) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_WATER_COOLER);
               setBlk(c, lx, fy + 2, lz, BackroomsStalkerMod.B_COOLER_JUG);
            } else if (rx == 2 && rz == 9) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_LOCKER_LOW);
               setBlk(c, lx, fy + 2, lz, BackroomsStalkerMod.B_LOCKER_HIGH);
            }
            break;

         case 6: // Underground Pipe Storage Vault
         default:
            if ((rx == 2 || rx == 10) && (rz == 3 || rz == 8)) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_LOCKER_LOW);
               setBlk(c, lx, fy + 2, lz, BackroomsStalkerMod.B_LOCKER_HIGH);
            } else if (rx == 6 && rz == 2) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_VENDING);
            } else if ((rx == 4 || rx == 8) && rz == 10) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_VALVE_PIPE);
            }
            break;
      }
   }

   // ====================================================================================================
   // LEVEL "!" — "RUN FOR YOUR LIFE" (Y = 14..20): 7 Randomized Emergency Hospital Archetypes
   // ====================================================================================================
   private void generateLevelRun(Chunk c, int lx, int lz, int wx, int wz) {
      int fy = RUN_FLOOR_Y;
      int cy = RUN_CEIL_Y;
      setBlk(c, lx, fy - 1, lz, Blocks.BEDROCK);

      int rx = Math.floorMod(wx, 12);
      int rz = Math.floorMod(wz, 12);
      int roomX = Math.floorDiv(wx, 12);
      int roomZ = Math.floorDiv(wz, 12);
      long rh = cellHash(this.seed ^ levelSalt(LEVEL_RUN), roomX, roomZ);
      int archetype = (int)Math.floorMod(rh, 7L);

      boolean redRunner = (rx >= 4 && rx <= 7) || (rz >= 4 && rz <= 7) || (Math.abs(wx) <= 2 && Math.abs(wz) <= 2);
      setBlk(c, lx, fy, lz, redRunner ? (Blocks.WOOL0 + 14) : Blocks.QUARTZ_BLOCK);
      for (int y = fy + 1; y < cy; y++) {
         setBlk(c, lx, y, lz, Blocks.AIR);
      }

      setBlk(c, lx, cy, lz, ((rx == 5 || rx == 6) && (rz == 5 || rz == 6)) ? Blocks.GLOWSTONE : Blocks.BRICKS);
      setBlk(c, lx, cy + 1, lz, Blocks.BEDROCK);

      if (placeSpawnOrExitRoom(c, lx, lz, wx, wz, LEVEL_RUN, fy, cy, Blocks.BRICKS, Blocks.QUARTZ_BLOCK)) {
         return;
      }

      switch (archetype) {
         case 0: // Emergency Hospital Ward & Gurney Barricade
            if ((rx == 2 || rx == 3) && (rz == 2 || rz == 9)) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_DESK);
            } else if (rx == 9 && (rz == 2 || rz == 3)) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_LOCKER_LOW);
               setBlk(c, lx, fy + 2, lz, BackroomsStalkerMod.B_LOCKER_HIGH);
            } else if (rx == 2 && rz == 6) {
               setBlk(c, lx, fy + 1, lz, Redstone319.REDSTONE_TORCH);
            }
            break;

         case 1: // Nurses' Station Rotunda (Wrap-Around 3D Desk, Chairs, Vending Machine & Water Cooler)
            if ((rx == 4 || rx == 8) && (rz >= 4 && rz <= 7)) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_DESK);
            } else if (rx == 6 && rz == 6) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_OFFICE_CHAIR);
            } else if (rx == 2 && rz == 2) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_VENDING);
            } else if (rx == 10 && rz == 2) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_WATER_COOLER);
               setBlk(c, lx, fy + 2, lz, BackroomsStalkerMod.B_COOLER_JUG);
            }
            break;

         case 2: // Quarantine Glass Airlock
            if (rz == 4 && rx >= 2 && rx <= 10 && rx != 6) {
               setBlk(c, lx, fy + 1, lz, Blocks.QUARTZ_BLOCK);
               setBlk(c, lx, fy + 2, lz, Blocks.GLASS);
               setBlk(c, lx, fy + 3, lz, Blocks.QUARTZ_BLOCK);
            } else if (rx == 2 && rz == 2) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_VALVE_PIPE);
            } else if (rx == 10 && rz == 9) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_LOCKER_LOW);
               setBlk(c, lx, fy + 2, lz, BackroomsStalkerMod.B_LOCKER_HIGH);
            }
            break;

         case 3: // Triage Curtain Bays
            if ((rx == 3 || rx == 9) && (rz == 2 || rz == 3)) {
               setBlk(c, lx, fy + 1, lz, Blocks.WOOL0);
               setBlk(c, lx, fy + 2, lz, Blocks.WOOL0 + 14);
            } else if ((rx == 5 || rx == 6) && rz == 2) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_DESK);
            } else if (rx == 2 && rz == 9) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_WATER_COOLER);
               setBlk(c, lx, fy + 2, lz, BackroomsStalkerMod.B_COOLER_JUG);
            }
            break;

         case 4: // Hospital Waiting Lounge & Vending Alcove
            if ((rx == 3 || rx == 9) && (rz == 4 || rz == 6 || rz == 8)) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_OFFICE_CHAIR);
            } else if (rx == 2 && (rz == 2 || rz == 3)) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_VENDING);
            } else if (rx == 10 && rz == 2) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_DESK);
            }
            break;

         case 5: // Surgical / Morgue Locker Room
            if ((rx == 2 || rx == 3 || rx == 9 || rx == 10) && rz == 2) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_LOCKER_LOW);
               setBlk(c, lx, fy + 2, lz, BackroomsStalkerMod.B_LOCKER_HIGH);
            } else if (rx == 6 && rz == 2) {
               setBlk(c, lx, fy + 1, lz, Redstone319.REDSTONE_TORCH);
            } else if (rx == 6 && rz == 9) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_DESK);
            }
            break;

         case 6: // Collapsed Hospital Barricade (Crawl under 3D Desks!)
         default:
            if ((rx == 5 || rx == 6 || rx == 7) && rz == 3) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_DESK);
            } else if (rx == 2 && rz == 8) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_LOCKER_LOW);
               setBlk(c, lx, fy + 2, lz, BackroomsStalkerMod.B_LOCKER_HIGH);
            } else if (rx == 10 && rz == 8) {
               setBlk(c, lx, fy + 1, lz, BackroomsStalkerMod.B_VALVE_PIPE);
            }
            break;
      }
   }
}
