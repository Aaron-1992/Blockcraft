package bc;

/**
 * Multi-Level Backrooms World Generator.
 * <p>
 * Generates 5 separate, full-world Backrooms levels at distinct vertical strata so each level has
 * its own cohesive, detailed architecture across the entire infinite X/Z plane:
 * <ul>
 *   <li><b>Level 37 — The Poolrooms</b> ({@code Y = 82..90}): Serene white quartz atrium, raised tiled walkways,
 *       sunken turquoise water pools, fluted columns, and cyan sea-lantern skylight coffers.</li>
 *   <li><b>Level 0 — The Lobby</b> ({@code Y = 64..71}): Pretty mono-yellow wallpapered rooms with oak baseboard
 *       wainscoting, birch crown molding, fluted corner pilasters, acoustic ceiling tiles, office desks
 *       (with under-desk hiding cubbies &amp; Almond Water on top), Hiding Lockers, and Water Coolers.</li>
 *   <li><b>Level 1 — The Habitable Zone (Warehouse)</b> ({@code Y = 46..53}): Industrial concrete/stonebrick
 *       warehouse halls, heavy 2x2 pillars, wooden supply shelves with Almond Water, and Hiding Booths.</li>
 *   <li><b>Level 2 — Pipe Dreams</b> ({@code Y = 30..36}): Claustrophobic brick &amp; stonebrick maintenance
 *       tunnels with overhead steam pipes, recessed pipe-alcove hiding spots, and dim amber lights.</li>
 *   <li><b>Level "!" — Run For Your Life</b> ({@code Y = 14..20}): Endless crimson hospital corridor with
 *       red carpet, white quartz lower walls, side hiding alcoves, Almond Water tables, and redstone alarms.</li>
 * </ul>
 */
public class BackroomsGen extends Gen {
   // Level 0 ("The Lobby") default constants
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
   }

   private static long cellHash(long seed, int x, int z) {
      long h = seed ^ (x * 341873128712L) ^ (z * 132897987541L);
      h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
      h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
      return h ^ (h >>> 31);
   }

   /** Returns which Backrooms Level (0..4) corresponds to a vertical Y coordinate. */
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

            // Fill solid stone/bedrock strata between separate levels
            setBlk(c, lx, 0, lz, Blocks.BEDROCK);
            for (int y = 1; y <= POOL_CEIL_Y + 1; y++) {
               setBlk(c, lx, y, lz, Blocks.STONE);
            }

            // Generate each separate Backrooms Level at its own vertical stratum
            generateLevelRun(c, lx, lz, wx, wz);
            generateLevel2(c, lx, lz, wx, wz);
            generateLevel1(c, lx, lz, wx, wz);
            generateLevel0(c, lx, lz, wx, wz);
            generatePoolrooms(c, lx, lz, wx, wz);

            // Seal top cap above Poolrooms with Bedrock so 0 sky light ever leaks in
            setBlk(c, lx, POOL_CEIL_Y + 1, lz, Blocks.BEDROCK);
         }
      }
      return c;
   }

   // ====================================================================================================
   // LEVEL 0 — "THE LOBBY" (Y = 64..71): Pretty Mono-Yellow Wallpaper, Baseboards, Offices & Hiding Spots
   // ====================================================================================================
   private void generateLevel0(Chunk c, int lx, int lz, int wx, int wz) {
      int fy = L0_FLOOR_Y;
      int cy = L0_CEIL_Y;

      setBlk(c, lx, fy - 1, lz, Blocks.BEDROCK);
      setBlk(c, lx, fy, lz, CARPET_BLOCK);
      for (int y = fy + 1; y < cy; y++) {
         setBlk(c, lx, y, lz, Blocks.AIR);
      }

      // Pretty coffered acoustic ceiling: Birch Planks framing Sandstone panels + 2x2 Glowstone lights
      int cmx = Math.floorMod(wx, 8);
      int cmz = Math.floorMod(wz, 8);
      boolean lightPanel = (cmx == 3 || cmx == 4) && (cmz == 3 || cmz == 4);
      boolean ceilingGridFrame = cmx == 0 || cmz == 0;
      int cellX = Math.floorDiv(wx, 8);
      int cellZ = Math.floorDiv(wz, 8);
      long cellH = cellHash(this.seed, cellX, cellZ);
      boolean spawnLobby = Math.abs(wx) <= 5 && Math.abs(wz) <= 5;

      if (lightPanel) {
         boolean working = spawnLobby || (cellH & 15L) != 0L;
         setBlk(c, lx, cy, lz, working ? Blocks.GLOWSTONE : Blocks.GLASS);
      } else {
         setBlk(c, lx, cy, lz, ceilingGridFrame ? Blocks313.PLANKS_BIRCH : Blocks.SANDSTONE);
      }
      setBlk(c, lx, cy + 1, lz, Blocks.BEDROCK);

      // Starting Lobby around (0, 0) with a Welcome Supply Desk & Water Cooler (no random junk chest!)
      if (spawnLobby) {
         // Carpet border runner around the spawn room
         if (Math.abs(wx) == 4 || Math.abs(wz) == 4) {
            setBlk(c, lx, fy, lz, Blocks.WOOL0 + 12); // Warm brown carpet border
         }
         // Welcome Desk at (3, 0..1) with an under-desk hiding cubby and Almond Water on top
         if (wx == 3 && (wz == 0 || wz == 1)) {
            setBlk(c, lx, fy + 1, lz, wz == 0 ? Blocks.FENCE : Blocks.AIR); //wz==1 is open under-desk hiding cubby!
            setBlk(c, lx, fy + 2, lz, Blocks.PLANKS);
         }
         // Almond Water Cooler at (-3, 0)
         if (wx == -3 && wz == 0) {
            setBlk(c, lx, fy + 1, lz, Blocks.QUARTZ_BLOCK);
            setBlk(c, lx, fy + 2, lz, Blocks313.ICE);
         }
         // Hiding Locker Booth at (-4, -4..-3)
         if (wx == -4 && wz == -4) {
            setBlk(c, lx, fy + 1, lz, Blocks.PLANKS);
            setBlk(c, lx, fy + 2, lz, Blocks.PLANKS);
            setBlk(c, lx, fy + 3, lz, Blocks.PLANKS);
         }
         return;
      }

      // 10x10 Architectural Room Grid for spacious, pretty rooms
      int rx = Math.floorMod(wx, 10);
      int rz = Math.floorMod(wz, 10);
      int roomX = Math.floorDiv(wx, 10);
      int roomZ = Math.floorDiv(wz, 10);
      long rh = cellHash(this.seed ^ 0x9E3779B97F4A7C15L, roomX, roomZ);
      int roomStyle = (int)Math.floorMod(rh, 5L); // 0..2: Wallpapered Rooms/Offices, 3: Pillar Gallery, 4: Lounge

      boolean isWall = false;
      boolean isDoor = false;
      boolean isWindow = false;
      boolean isPilaster = false;

      if (roomStyle == 3) {
         // Symmetric Louvered Pillar Gallery: 2x2 fluted pillars with oak baseboards
         if ((rx == 2 || rx == 3 || rx == 7 || rx == 8) && (rz == 2 || rz == 3 || rz == 7 || rz == 8)) {
            isWall = true;
            isPilaster = (rx == 2 || rx == 8) && (rz == 2 || rz == 8);
         }
      } else {
         boolean westWall = (rh & 3L) != 0L;
         boolean northWall = ((rh >>> 2) & 3L) != 0L;
         if (rx == 0 && westWall) {
            isWall = true;
            int dp = 3 + (int)Math.floorMod(rh >>> 8, 3L);
            if (rz == dp || rz == dp + 1) {
               isDoor = true;
            } else if ((rz == 7 || rz == 8) && ((rh >>> 12) & 3L) == 0L) {
               isWindow = true;
            }
         }
         if (rz == 0 && northWall) {
            isWall = true;
            int dp = 3 + (int)Math.floorMod(rh >>> 14, 3L);
            if (rx == dp || rx == dp + 1) {
               isDoor = true;
            } else if ((rx == 7 || rx == 8) && ((rh >>> 18) & 3L) == 0L) {
               isWindow = true;
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
            // Pretty birch wood threshold under every doorway
            setBlk(c, lx, fy, lz, Blocks313.PLANKS_BIRCH);
         }
         for (int y = fy + 1; y < cy; y++) {
            if (isDoor && y <= fy + 4) {
               setBlk(c, lx, y, lz, Blocks.AIR);
            } else if (isWindow && (y == fy + 2 || y == fy + 3)) {
               setBlk(c, lx, y, lz, Blocks.GLASS);
            } else if (y == fy + 1) {
               // Baseboard wainscoting
               setBlk(c, lx, y, lz, isPilaster ? Blocks.LOG_BIRCH : Blocks.PLANKS);
            } else if (y == cy - 1) {
               // Crown molding
               setBlk(c, lx, y, lz, Blocks313.PLANKS_BIRCH);
            } else {
               setBlk(c, lx, y, lz, isPilaster ? Blocks.LOG_BIRCH : Blocks.SANDSTONE);
            }
         }
         return;
      }

      // ---- INTERIOR FURNISHINGS, HIDING SPOTS & FINDABLE ALMOND WATER ----
      // 1. Hiding Locker Booth in corners (rx = 1..2, rz = 1..2) of rooms where (rh & 7) == 0
      if ((rh & 7L) == 0L && rx >= 1 && rx <= 2 && rz >= 1 && rz <= 2) {
         // 1x1 interior hiding spot at (rx=1, rz=1) surrounded by oak locker walls, entry at (rx=2, rz=1)
         if (rx == 1 && rz == 1) {
            // Inside the Hiding Locker: dark oak floor & overhead locker roof at fy+3
            setBlk(c, lx, fy, lz, Blocks.PLANKS);
            setBlk(c, lx, fy + 3, lz, Blocks.PLANKS);
         } else if (rx == 2 && rz == 1) {
            // Locker doorway with header at fy+3
            setBlk(c, lx, fy + 3, lz, Blocks.PLANKS);
         } else {
            // Solid locker side walls
            for (int y = fy + 1; y <= fy + 3; y++) {
               setBlk(c, lx, y, lz, Blocks.LOG_OAK);
            }
         }
         return;
      }

      // 2. Office Supply Desk with Under-Desk Hiding Cubby & Almond Water on top
      if (roomStyle == 1 && rx >= 5 && rx <= 7 && rz == 5) {
         if (rx == 5 || rx == 7) {
            setBlk(c, lx, fy + 1, lz, Blocks.FENCE);
         } else {
            // rx == 6: Open under-desk crawl cubby at fy+1 (crouch here to hide!)
            setBlk(c, lx, fy + 1, lz, Blocks.AIR);
         }
         setBlk(c, lx, fy + 2, lz, Blocks.PLANKS);
         return;
      }

      // 3. Almond Water Cooler Dispenser in lounges
      if (roomStyle == 4 && rx == 5 && rz == 2) {
         setBlk(c, lx, fy + 1, lz, Blocks.QUARTZ_BLOCK);
         setBlk(c, lx, fy + 2, lz, Blocks313.ICE);
      }

      // 4. Inter-Level Stairwell / Service Elevator Kiosk every 60 blocks (plus at (15, 5))
      if ((wx == 15 && wz == 5) || (Math.floorMod(wx, 60) == 5 && Math.floorMod(wz, 60) == 5)) {
         setBlk(c, lx, fy, lz, Redstone319.SEA_LANTERN);
         setBlk(c, lx, fy + 1, lz, Blocks.LADDER);
      }
   }

   // ====================================================================================================
   // LEVEL 1 — "THE HABITABLE ZONE / WAREHOUSE" (Y = 46..53)
   // ====================================================================================================
   private void generateLevel1(Chunk c, int lx, int lz, int wx, int wz) {
      int fy = L1_FLOOR_Y;
      int cy = L1_CEIL_Y;

      setBlk(c, lx, fy - 1, lz, Blocks.BEDROCK);
      boolean lane = Math.floorMod(wx, 12) == 6 || Math.floorMod(wz, 12) == 6;
      setBlk(c, lx, fy, lz, lane ? Blocks.COBBLE : Blocks.STONE);
      for (int y = fy + 1; y < cy; y++) {
         setBlk(c, lx, y, lz, Blocks.AIR);
      }

      int mx = Math.floorMod(wx, 8);
      int mz = Math.floorMod(wz, 8);
      boolean light = (mx == 3 || mx == 4) && mz == 4;
      setBlk(c, lx, cy, lz, light ? Blocks.GLOWSTONE : Blocks.STONEBRICK);
      setBlk(c, lx, cy + 1, lz, Blocks.BEDROCK);

      if (Math.abs(wx) <= 4 && Math.abs(wz) <= 4) {
         if (wx == 3 && wz == 0) {
            setBlk(c, lx, fy + 1, lz, Blocks.QUARTZ_BLOCK);
            setBlk(c, lx, fy + 2, lz, Blocks313.ICE);
         }
         return;
      }

      int rx = Math.floorMod(wx, 12);
      int rz = Math.floorMod(wz, 12);
      long rh = cellHash(this.seed ^ 0x11112222L, Math.floorDiv(wx, 12), Math.floorDiv(wz, 12));

      // Concrete/Stonebrick 2x2 warehouse pillars
      if ((rx == 0 || rx == 1) && (rz == 0 || rz == 1)) {
         for (int y = fy + 1; y < cy; y++) {
            setBlk(c, lx, y, lz, Blocks.STONEBRICK);
         }
         return;
      }

      // Warehouse Supply Shelving Racks & Under-Shelf Hiding Cubbies
      if ((rh & 3L) == 0L && rx >= 4 && rx <= 8 && rz == 3) {
         setBlk(c, lx, fy + 1, lz, (rx == 5 || rx == 7) ? Blocks.AIR : Blocks.BOOKSHELF);
         setBlk(c, lx, fy + 2, lz, Blocks.PLANKS);
         setBlk(c, lx, fy + 3, lz, Blocks.BOOKSHELF);
      }

      // Hiding Locker Booth in Warehouse
      if ((rh & 7L) == 1L && rx >= 9 && rx <= 10 && rz >= 9 && rz <= 10) {
         if (rx == 9 && rz == 9) {
            setBlk(c, lx, fy + 3, lz, Blocks.STONEBRICK);
         } else if (!(rx == 10 && rz == 9)) {
            for (int y = fy + 1; y <= fy + 3; y++) {
               setBlk(c, lx, y, lz, Blocks.STONEBRICK);
            }
         }
      }
   }

   // ====================================================================================================
   // LEVEL 2 — "PIPE DREAMS" (Y = 30..36)
   // ====================================================================================================
   private void generateLevel2(Chunk c, int lx, int lz, int wx, int wz) {
      int fy = L2_FLOOR_Y;
      int cy = L2_CEIL_Y;

      setBlk(c, lx, fy - 1, lz, Blocks.BEDROCK);
      setBlk(c, lx, fy, lz, Blocks.COBBLE);
      for (int y = fy + 1; y < cy; y++) {
         setBlk(c, lx, y, lz, Blocks.AIR);
      }

      int rx = Math.floorMod(wx, 8);
      int rz = Math.floorMod(wz, 8);
      boolean light = rx == 4 && rz == 4;
      setBlk(c, lx, cy, lz, light ? Blocks.GLOWSTONE : Blocks.STONEBRICK);
      setBlk(c, lx, cy + 1, lz, Blocks.BEDROCK);

      if (Math.abs(wx) <= 3 && Math.abs(wz) <= 3) {
         return;
      }

      // Narrow maintenance corridors along X and Z with recessed hiding alcoves
      boolean corrX = rz >= 2 && rz <= 5;
      boolean corrZ = rx >= 2 && rx <= 5;
      if (!corrX && !corrZ) {
         // Carve a 1x1 Hiding Pipe Alcove at (rx=1, rz=3)
         if (rx == 1 && rz == 3) {
            setBlk(c, lx, fy + 3, lz, Blocks.BRICKS);
            return;
         }
         for (int y = fy + 1; y < cy; y++) {
            setBlk(c, lx, y, lz, y == fy + 1 ? Blocks.STONEBRICK : Blocks.BRICKS);
         }
      } else if (rx == 2 || rz == 2) {
         // Overhead steam pipe conduit along corridor ceiling edge
         setBlk(c, lx, cy - 1, lz, Blocks.LOG_SPRUCE);
      }
   }

   // ====================================================================================================
   // LEVEL 37 — "THE POOLROOMS" (Y = 82..90): White Quartz Tiles, Walkways & Sunken Pools
   // ====================================================================================================
   private void generatePoolrooms(Chunk c, int lx, int lz, int wx, int wz) {
      int fy = POOL_FLOOR_Y;
      int cy = POOL_CEIL_Y;

      setBlk(c, lx, fy - 2, lz, Blocks.BEDROCK);
      setBlk(c, lx, fy - 1, lz, Blocks.QUARTZ_BLOCK);

      int rx = Math.floorMod(wx, 12);
      int rz = Math.floorMod(wz, 12);
      long rh = cellHash(this.seed ^ 0x37373737L, Math.floorDiv(wx, 12), Math.floorDiv(wz, 12));

      // Dry tiled walkways around the perimeter of each 12x12 atrium, sunken water pool in the middle
      boolean dryWalkway = (Math.abs(wx) <= 3 && Math.abs(wz) <= 3)
         || rx <= 2 || rx >= 10 || rz <= 2 || rz >= 10;
      setBlk(c, lx, fy, lz, dryWalkway ? Blocks.QUARTZ_BLOCK : Blocks.WATER);

      for (int y = fy + 1; y < cy; y++) {
         setBlk(c, lx, y, lz, Blocks.AIR);
      }

      // Vaulted ceiling with glowing Sea Lantern skylight coffers
      boolean skylight = rx >= 4 && rx <= 7 && rz >= 4 && rz <= 7;
      setBlk(c, lx, cy, lz, skylight ? Redstone319.SEA_LANTERN : Blocks.QUARTZ_BLOCK);

      if (Math.abs(wx) <= 3 && Math.abs(wz) <= 3) {
         return;
      }

      // Fluted white quartz columns & arched partition walls
      boolean pillar = (rx == 2 || rx == 9) && (rz == 2 || rz == 9);
      boolean wall = (rx == 0 && (rh & 3L) == 0L && (rz < 4 || rz > 7))
         || (rz == 0 && ((rh >>> 2) & 3L) == 0L && (rx < 4 || rx > 7));
      if (pillar || wall) {
         for (int y = fy + 1; y < cy; y++) {
            setBlk(c, lx, y, lz, Blocks.QUARTZ_BLOCK);
         }
      } else if (rx == 1 && rz == 5 && (rh & 7L) == 0L) {
         // Poolrooms Almond Water Cooler
         setBlk(c, lx, fy + 1, lz, Blocks.QUARTZ_BLOCK);
         setBlk(c, lx, fy + 2, lz, Blocks313.ICE);
      }
   }

   // ====================================================================================================
   // LEVEL "!" — "RUN FOR YOUR LIFE" (Y = 14..20): Crimson Emergency Hospital Corridor
   // ====================================================================================================
   private void generateLevelRun(Chunk c, int lx, int lz, int wx, int wz) {
      int fy = RUN_FLOOR_Y;
      int cy = RUN_CEIL_Y;

      setBlk(c, lx, fy - 1, lz, Blocks.BEDROCK);
      setBlk(c, lx, fy, lz, Blocks.WOOL0 + 14); // Blood-crimson carpet
      for (int y = fy + 1; y < cy; y++) {
         setBlk(c, lx, y, lz, Blocks.AIR);
      }

      int rz = Math.floorMod(wz, 8);
      setBlk(c, lx, cy, lz, Blocks.BRICKS);
      setBlk(c, lx, cy + 1, lz, Blocks.BEDROCK);

      // Main hospital corridor runs along Z around X = -4..4, plus cross-corridors every 16 blocks
      int rx = Math.floorMod(wx + 8, 16) - 8;
      boolean inHallX = Math.abs(rx) <= 3;
      boolean inHallZ = Math.abs(rz - 4) <= 2;

      if (!inHallX && !inHallZ) {
         // Carve side hospital hiding alcoves at rx == 4, rz == 1
         if (Math.abs(rx) == 4 && rz == 1) {
            setBlk(c, lx, fy + 3, lz, Blocks.BRICKS);
            return;
         }
         for (int y = fy + 1; y < cy; y++) {
            setBlk(c, lx, y, lz, y == fy + 1 ? Blocks.QUARTZ_BLOCK : Blocks.BRICKS);
         }
      } else if (Math.abs(rx) == 3 && rz == 0) {
         // Emergency Redstone alarm light & Almond Water table
         setBlk(c, lx, fy + 1, lz, Redstone319.REDSTONE_TORCH);
      }
   }
}
