package bc;

/**
 * World generator and full gameplay engine for {@link WorldSettings#SKYBLOCK}, {@link WorldSettings#SKYWARS},
 * and {@link WorldSettings#BEDWARS} (Update 4.1).
 */
public final class SkyGen extends Gen {
   public static final int Y_DECK = 64;

   // 8 Skywars spawn island centers (radius 42)
   public static final int[][] SKYWARS_SPAWNS = new int[][]{
      {42, 0}, {30, 30}, {0, 42}, {-30, 30}, {-42, 0}, {-30, -30}, {0, -42}, {30, -30}
   };

   // 4 Skywars mid-ring island centers
   public static final int[][] SKYWARS_MIDS = new int[][]{
      {24, 0}, {-24, 0}, {0, 24}, {0, -24}
   };

   // 4 Bedwars team base centers: North (Red), South (Blue), West (Green), East (Yellow)
   public static final int[][] BEDWARS_BASES = new int[][]{
      {0, -44}, {0, 44}, {-44, 0}, {44, 0}
   };
   public static final int[] BEDWARS_COLORS = new int[]{14, 11, 13, 4}; // Red, Blue, Green, Yellow
   public static final String[] BEDWARS_NAMES = new String[]{"Red", "Blue", "Green", "Yellow"};
   public static final String[] BEDWARS_CODES = new String[]{"\u00a7c", "\u00a79", "\u00a7a", "\u00a7e"};

   // 4 Bedwars diamond generator islands
   public static final int[][] BEDWARS_DIAMONDS = new int[][]{
      {24, 24}, {-24, 24}, {24, -24}, {-24, -24}
   };

   // Skyblock quest tracking
   public static final String[] SB_QUEST_NAMES = new String[]{
      "1. Cobble Generator (32 Cobble)",
      "2. Sky Harvester (8 Wheat/Melon)",
      "3. Island Bridger (Reach Outer Island)",
      "4. Void Hunter (4 Monster Drops)",
      "5. Nether Pioneer (10 Obsidian)"
   };
   private static final boolean[] sbQuestDone = new boolean[5];
   private static long lastWorldSeed = Long.MIN_VALUE;
   private static boolean skyblockStocked;
   private static boolean skywarsStocked;
   private static boolean skywarsWon;
   private static boolean bedwarsStocked;
   private static boolean bedwarsWon;
   private static final boolean[] bedKnownIntact = new boolean[]{true, true, true, true};

   private final int mode;

   public SkyGen(long seed, WorldSettings st) {
      super(seed);
      this.settings = st;
      this.mode = st.type;
   }

   public static void resetMatchState() {
      for (int i = 0; i < sbQuestDone.length; i++) {
         sbQuestDone[i] = false;
      }
      skyblockStocked = false;
      skywarsStocked = false;
      skywarsWon = false;
      bedwarsStocked = false;
      bedwarsWon = false;
      for (int i = 0; i < 4; i++) {
         bedKnownIntact[i] = true;
      }
   }

   @Override
   public Gen.Col column(int x, int z) {
      Gen.Col c = new Gen.Col();
      c.t = 0.5;
      c.hu = 0.5;
      c.biome = Biomes.PLAINS;
      c.h = surfaceHeightAt(x, z);
      if (c.h > 0 && this.mode == WorldSettings.SKYBLOCK) {
         if (isSkyblockSand(x, z)) {
            c.biome = Biomes.DESERT;
         } else if (isSkyblockTaiga(x, z)) {
            c.biome = Biomes.SNOWY_TAIGA;
         } else if (isSkyblockJungle(x, z)) {
            c.biome = Biomes.JUNGLE;
         }
      } else if (c.h > 0 && this.mode == WorldSettings.BEDWARS) {
         if (Math.abs(x) <= 10 && Math.abs(z) <= 10) {
            c.biome = Biomes.JUNGLE;
         } else if (Math.abs(x) >= 18 && Math.abs(z) >= 18 && Math.abs(x) <= 30 && Math.abs(z) <= 30) {
            c.biome = Biomes.ICE_SPIKES;
         }
      }
      return c;
   }

   private int surfaceHeightAt(int x, int z) {
      if (this.mode == WorldSettings.SKYBLOCK) {
         if (isSkyblockMain(x, z)
            || isSkyblockSand(x, z)
            || isSkyblockShrine(x, z)
            || isSkyblockTaiga(x, z)
            || isSkyblockJungle(x, z)) {
            return Y_DECK;
         }
         return 0;
      }
      if (this.mode == WorldSettings.SKYWARS) {
         double d0 = Math.hypot(x, z);
         if (d0 <= 11.2) {
            return Y_DECK;
         }
         for (int[] m : SKYWARS_MIDS) {
            if (Math.hypot(x - m[0], z - m[1]) <= 4.5) {
               return Y_DECK;
            }
         }
         for (int[] sp : SKYWARS_SPAWNS) {
            if (Math.hypot(x - sp[0], z - sp[1]) <= 4.6) {
               return Y_DECK;
            }
         }
         return 0;
      }
      if (this.mode == WorldSettings.BEDWARS) {
         if (Math.hypot(x, z) <= 10.5) {
            return Y_DECK;
         }
         for (int[] d : BEDWARS_DIAMONDS) {
            if (Math.hypot(x - d[0], z - d[1]) <= 4.2) {
               return Y_DECK;
            }
         }
         for (int t = 0; t < 4; t++) {
            int bx = BEDWARS_BASES[t][0];
            int bz = BEDWARS_BASES[t][1];
            int hw = t < 2 ? 5 : 6;
            int hd = t < 2 ? 6 : 5;
            if (Math.abs(x - bx) <= hw && Math.abs(z - bz) <= hd) {
               return Y_DECK;
            }
         }
         return 0;
      }
      return 0;
   }

   private static boolean isSkyblockMain(int x, int z) {
      return x >= -1 && x <= 4 && z >= -1 && z <= 4 && !(x >= 2 && z >= 2);
   }

   private static boolean isSkyblockSand(int x, int z) {
      return x >= 46 && x <= 50 && z >= -2 && z <= 2;
   }

   private static boolean isSkyblockShrine(int x, int z) {
      return x >= -50 && x <= -46 && z >= -2 && z <= 2;
   }

   private static boolean isSkyblockTaiga(int x, int z) {
      return x >= -2 && x <= 2 && z >= -50 && z <= -46;
   }

   private static boolean isSkyblockJungle(int x, int z) {
      return x >= -2 && x <= 2 && z >= 46 && z <= 50;
   }

   @Override
   public int[] findSpawn() {
      if (this.mode == WorldSettings.SKYBLOCK) {
         return new int[]{0, Y_DECK + 1, 0};
      }
      if (this.mode == WorldSettings.SKYWARS) {
         return new int[]{SKYWARS_SPAWNS[0][0], Y_DECK + 1, SKYWARS_SPAWNS[0][1]};
      }
      if (this.mode == WorldSettings.BEDWARS) {
         return new int[]{BEDWARS_BASES[0][0], Y_DECK + 1, BEDWARS_BASES[0][1]};
      }
      return new int[]{0, Y_DECK + 1, 0};
   }

   public static double[] multiplayerSpawnFor(World w, int peerIndex) {
      if (w != null && w.settings != null) {
         if (w.settings.type == WorldSettings.SKYWARS) {
            int[] sp = SKYWARS_SPAWNS[Math.abs(peerIndex) % SKYWARS_SPAWNS.length];
            return new double[]{sp[0] + 0.5, Y_DECK + 1.0, sp[1] + 0.5};
         }
         if (w.settings.type == WorldSettings.BEDWARS) {
            int[] sp = BEDWARS_BASES[Math.abs(peerIndex) % BEDWARS_BASES.length];
            return new double[]{sp[0] + 0.5, Y_DECK + 1.0, sp[1] + 0.5};
         }
         if (w.settings.type == WorldSettings.SKYBLOCK) {
            return new double[]{0.5, Y_DECK + 1.0, 0.5};
         }
      }
      return null;
   }

   @Override
   public Chunk generate(int cx, int cz) {
      Chunk ch = new Chunk(cx, cz);
      int grass = grassColorFor(0.5, 0.5);
      int foliage = foliageColorFor(0.5, 0.5);
      for (int i = 0; i < 256; i++) {
         ch.biome[i] = (byte)Biomes.PLAINS;
         ch.grassColor[i] = grass;
         ch.foliageColor[i] = foliage;
         ch.hm[i] = 0;
      }

      if (this.mode == WorldSettings.SKYBLOCK) {
         generateSkyblock(ch, cx, cz);
      } else if (this.mode == WorldSettings.SKYWARS) {
         generateSkywars(ch, cx, cz);
      } else if (this.mode == WorldSettings.BEDWARS) {
         generateBedwars(ch, cx, cz);
      }

      recomputeHeightmap(ch);
      return ch;
   }

   private static void setW(Chunk ch, int wx, int y, int wz, int id) {
      int lx = wx - ch.cx * 16;
      int lz = wz - ch.cz * 16;
      if (lx >= 0 && lx < 16 && lz >= 0 && lz < 16 && y >= 0 && y < 256) {
         ch.blocks[Chunk.idx(lx, y, lz)] = (short)id;
      }
   }

   private static void recomputeHeightmap(Chunk ch) {
      for (int z = 0; z < 16; z++) {
         for (int x = 0; x < 16; x++) {
            int top = 0;
            for (int y = 255; y >= 0; y--) {
               int id = ch.blocks[Chunk.idx(x, y, z)] & 0xFFFF;
               if (Blocks.opacity[id] > 0) {
                  top = y;
                  break;
               }
            }
            ch.hm[z * 16 + x] = top;
         }
      }
   }

   private void generateSkyblock(Chunk ch, int cx, int cz) {
      if (Math.abs(cx) > 4 || Math.abs(cz) > 4) {
         return;
      }
      // 1. Main L-shaped Skyblock island around (0, 64, 0)
      for (int wz = -1; wz <= 4; wz++) {
         for (int wx = -1; wx <= 4; wx++) {
            if (isSkyblockMain(wx, wz)) {
               setW(ch, wx, Y_DECK - 2, wz, Blocks.DIRT);
               setW(ch, wx, Y_DECK - 1, wz, Blocks.DIRT);
               setW(ch, wx, Y_DECK, wz, Blocks.GRASS);
            }
         }
      }
      setW(ch, 0, Y_DECK - 2, 0, Blocks.BEDROCK);
      // Classic Oak Tree at (1, 65, 3)
      buildSmallOak(ch, 1, Y_DECK + 1, 3);
      // Starter Chest at (3, 65, 0) + Crafting Table at (3, 65, 1)
      setW(ch, 3, Y_DECK + 1, 0, Blocks.CHEST_LOOT);
      setW(ch, 3, Y_DECK + 1, 1, Blocks.CRAFTING_TABLE);

      // 2. East Desert Sand Island at (46..50, 62..64, -2..2)
      for (int wz = -2; wz <= 2; wz++) {
         for (int wx = 46; wx <= 50; wx++) {
            setW(ch, wx, Y_DECK - 2, wz, Blocks.SANDSTONE);
            setW(ch, wx, Y_DECK - 1, wz, (wx == 48 && wz == 0) ? Blocks.GOLD_ORE : Blocks.SAND);
            setW(ch, wx, Y_DECK, wz, Blocks.SAND);
         }
      }
      setW(ch, 48, Y_DECK + 1, 0, Blocks.CACTUS);
      setW(ch, 48, Y_DECK + 2, 0, Blocks.CACTUS);
      setW(ch, 47, Y_DECK + 1, -1, Blocks.CHEST_LOOT);

      // 3. West Nether/Quartz Shrine Island at (-50..-46, 62..64, -2..2)
      for (int wz = -2; wz <= 2; wz++) {
         for (int wx = -50; wx <= -46; wx++) {
            setW(ch, wx, Y_DECK - 2, wz, Blocks.OBSIDIAN);
            setW(ch, wx, Y_DECK - 1, wz, Blocks.NETHERRACK);
            setW(ch, wx, Y_DECK, wz, Blocks.QUARTZ_BLOCK);
         }
      }
      setW(ch, -48, Y_DECK + 1, 0, Blocks.GLOWSTONE);
      setW(ch, -47, Y_DECK + 1, 0, Blocks.CHEST_LOOT);

      // 4. North Snowy Taiga & Ice Island at (-2..2, 62..64, -50..-46)
      for (int wz = -50; wz <= -46; wz++) {
         for (int wx = -2; wx <= 2; wx++) {
            setW(ch, wx, Y_DECK - 2, wz, Blocks.STONE);
            setW(ch, wx, Y_DECK - 1, wz, (wx == 0 && wz == -48) ? Blocks.DIAMOND_ORE : Blocks313.ICE);
            setW(ch, wx, Y_DECK, wz, Blocks.SNOW);
         }
      }
      buildSmallOak(ch, 0, Y_DECK + 1, -48);
      setW(ch, 0, Y_DECK + 1, -47, Blocks.CHEST_LOOT);

      // 5. South Jungle & Mushroom Island at (-2..2, 62..64, 46..50)
      for (int wz = 46; wz <= 50; wz++) {
         for (int wx = -2; wx <= 2; wx++) {
            setW(ch, wx, Y_DECK - 2, wz, Blocks.STONE);
            setW(ch, wx, Y_DECK - 1, wz, Blocks.DIRT);
            setW(ch, wx, Y_DECK, wz, Blocks.GRASS);
         }
      }
      setW(ch, -1, Y_DECK + 1, 48, Blocks.MELON);
      setW(ch, 1, Y_DECK + 1, 48, Blocks.PUMPKIN);
      setW(ch, -1, Y_DECK + 1, 49, Blocks313.MUSHROOM_RED);
      setW(ch, 1, Y_DECK + 1, 49, Blocks313.MUSHROOM_BROWN);
      setW(ch, 0, Y_DECK + 1, 47, Blocks.CHEST_LOOT);
   }

   private void generateSkywars(Chunk ch, int cx, int cz) {
      if (Math.abs(cx) > 4 || Math.abs(cz) > 4) {
         return;
      }

      // 1. Central Feast Island (radius 11 around 0,0)
      buildConeIsland(ch, 0, 0, 11.2, 13, Blocks.GRASS, Blocks.DIRT, Blocks.STONE, true);
      for (int dz = -3; dz <= 3; dz++) {
         for (int dx = -3; dx <= 3; dx++) {
            setW(ch, dx, Y_DECK, dz, Blocks.STONEBRICK);
         }
      }
      setW(ch, 0, Y_DECK, 0, Blocks.DIAMOND_BLOCK);
      setW(ch, 0, Y_DECK + 1, 0, Blocks.GLOWSTONE);
      for (int[] p : new int[][]{{-3, -3}, {3, -3}, {-3, 3}, {3, 3}}) {
         for (int y = 1; y <= 4; y++) {
            setW(ch, p[0], Y_DECK + y, p[1], Blocks.STONEBRICK);
         }
      }
      for (int dz = -3; dz <= 3; dz++) {
         for (int dx = -3; dx <= 3; dx++) {
            setW(ch, dx, Y_DECK + 5, dz, Blocks.SLAB0 + 4);
         }
      }
      // 4 Feast Loot Chests
      setW(ch, -2, Y_DECK + 1, 0, Blocks.CHEST_LOOT);
      setW(ch, 2, Y_DECK + 1, 0, Blocks.CHEST_LOOT);
      setW(ch, 0, Y_DECK + 1, -2, Blocks.CHEST_LOOT);
      setW(ch, 0, Y_DECK + 1, 2, Blocks.CHEST_LOOT);
      buildSmallOak(ch, -7, Y_DECK + 1, -6);
      buildSmallOak(ch, 7, Y_DECK + 1, 6);

      // 2. 4 Mid-Ring Islands
      for (int[] m : SKYWARS_MIDS) {
         buildConeIsland(ch, m[0], m[1], 4.5, 6, Blocks.GRASS, Blocks.DIRT, Blocks.STONE, true);
         setW(ch, m[0], Y_DECK + 1, m[1], Blocks.CHEST_LOOT);
         setW(ch, m[0], Y_DECK - 1, m[1], Blocks.GOLD_ORE);
         setW(ch, m[0] + 1, Y_DECK - 1, m[1], Blocks.DIAMOND_ORE);
      }

      // 3. 8 Outer Spawn Islands
      for (int[] sp : SKYWARS_SPAWNS) {
         buildConeIsland(ch, sp[0], sp[1], 4.6, 7, Blocks.GRASS, Blocks.DIRT, Blocks.STONE, true);
         buildSmallOak(ch, sp[0] - 2, Y_DECK + 1, sp[1] - 2);
         setW(ch, sp[0] + 2, Y_DECK + 1, sp[1], Blocks.CHEST_LOOT);
         setW(ch, sp[0] + 2, Y_DECK + 1, sp[1] + 1, Blocks.CRAFTING_TABLE);
         setW(ch, sp[0] + 2, Y_DECK + 1, sp[1] - 1, Blocks.FURNACE);
      }
   }

   private void generateBedwars(Chunk ch, int cx, int cz) {
      if (Math.abs(cx) > 4 || Math.abs(cz) > 4) {
         return;
      }

      // 1. Central Emerald Island
      buildConeIsland(ch, 0, 0, 10.5, 9, Blocks.STONEBRICK, Blocks.STONE, Blocks.COBBLE, false);
      for (int dz = -2; dz <= 2; dz++) {
         for (int dx = -2; dx <= 2; dx++) {
            setW(ch, dx, Y_DECK, dz, Blocks.QUARTZ_BLOCK);
         }
      }
      setW(ch, -4, Y_DECK, 0, Blocks.EMERALD_BLOCK);
      setW(ch, -4, Y_DECK + 3, 0, Blocks.GLOWSTONE);
      setW(ch, 4, Y_DECK, 0, Blocks.EMERALD_BLOCK);
      setW(ch, 4, Y_DECK + 3, 0, Blocks.GLOWSTONE);
      setW(ch, 0, Y_DECK + 1, 0, Blocks.CHEST_LOOT);

      // 2. 4 Diamond Generator Islands
      for (int[] d : BEDWARS_DIAMONDS) {
         buildConeIsland(ch, d[0], d[1], 4.2, 5, Blocks.STONEBRICK, Blocks.STONE, Blocks.COBBLE, false);
         setW(ch, d[0], Y_DECK, d[1], Blocks.DIAMOND_BLOCK);
         setW(ch, d[0], Y_DECK + 3, d[1], Blocks.GLOWSTONE);
      }

      // 3. 4 Team Bases (North=Red, South=Blue, West=Green, East=Yellow)
      for (int t = 0; t < 4; t++) {
         int bx = BEDWARS_BASES[t][0];
         int bz = BEDWARS_BASES[t][1];
         int color = BEDWARS_COLORS[t];
         int wool = Blocks.WOOL0 + color;
         int dirX = Integer.compare(0, bx);
         int dirZ = Integer.compare(0, bz);
         int hw = t < 2 ? 5 : 6;
         int hd = t < 2 ? 6 : 5;

         for (int dz = -hd; dz <= hd; dz++) {
            for (int dx = -hw; dx <= hw; dx++) {
               setW(ch, bx + dx, Y_DECK - 2, bz + dz, Blocks.STONE);
               setW(ch, bx + dx, Y_DECK - 1, bz + dz, Blocks.STONEBRICK);
               boolean border = Math.abs(dx) == hw || Math.abs(dz) == hd;
               setW(ch, bx + dx, Y_DECK, bz + dz, border ? wool : Blocks.STONEBRICK);
            }
         }

         // Back generator pad (Iron + Gold blocks)
         int gx = bx - dirX * 4;
         int gz = bz - dirZ * 4;
         setW(ch, gx, Y_DECK, gz, Blocks.IRON_BLOCK);
         setW(ch, gx + (dirX == 0 ? 1 : 0), Y_DECK, gz + (dirZ == 0 ? 1 : 0), Blocks.GOLD_BLOCK);
         setW(ch, gx - (dirX == 0 ? 1 : 0), Y_DECK, gz - (dirZ == 0 ? 1 : 0), Blocks.IRON_BLOCK);
         setW(ch, gx, Y_DECK + 3, gz, Blocks.GLOWSTONE);

         // Team Shop Chest & Crafting Table on the sides
         int sx = bx + (dirX == 0 ? 3 : 0);
         int sz = bz + (dirZ == 0 ? 3 : 0);
         setW(ch, sx, Y_DECK + 1, sz, Blocks.CHEST_LOOT);
         setW(ch, bx - (dirX == 0 ? 3 : 0), Y_DECK + 1, bz - (dirZ == 0 ? 3 : 0), Blocks.CRAFTING_TABLE);

         // Team Bed on front courtyard (3..4 blocks toward center from base center)
         int bedFootX = bx + dirX * 3;
         int bedFootZ = bz + dirZ * 3;
         int bedHeadX = bx + dirX * 4;
         int bedHeadZ = bz + dirZ * 4;
         int facing = dirZ < 0 ? 0 : (dirZ > 0 ? 1 : (dirX > 0 ? 2 : 3));
         setW(ch, bedFootX, Y_DECK + 1, bedFootZ, Decor315.foot(color, facing));
         setW(ch, bedHeadX, Y_DECK + 1, bedHeadZ, Decor315.head(color, facing));

         // Enemy bases (t > 0) have a protective wool defense shell over the bed so solo players can mine/TNT through it!
         if (t > 0) {
            setW(ch, bedFootX, Y_DECK + 2, bedFootZ, wool);
            setW(ch, bedHeadX, Y_DECK + 2, bedHeadZ, wool);
         }
      }
   }

   private void buildConeIsland(Chunk ch, int ox, int oz, double rad, int depth, int topId, int midId, int coreId, boolean ores) {
      int rCeil = (int)Math.ceil(rad);
      int minX = ch.cx * 16;
      int maxX = minX + 15;
      int minZ = ch.cz * 16;
      int maxZ = minZ + 15;
      if (ox + rCeil < minX || ox - rCeil > maxX || oz + rCeil < minZ || oz - rCeil > maxZ) {
         return;
      }

      for (int dz = -rCeil; dz <= rCeil; dz++) {
         for (int dx = -rCeil; dx <= rCeil; dx++) {
            double d = Math.hypot(dx, dz);
            if (d <= rad) {
               double frac = 1.0 - (d / rad);
               int colDepth = Math.max(2, (int)Math.round(depth * frac * frac + 1.5));
               for (int dy = 0; dy < colDepth; dy++) {
                  int y = Y_DECK - dy;
                  int id = dy == 0 ? topId : (dy <= 2 ? midId : coreId);
                  if (ores && dy >= 3 && ((dx * 31 + dz * 17 + dy * 13) & 15) == 0) {
                     id = dy >= 6 ? Blocks.DIAMOND_ORE : (dy >= 4 ? Blocks.IRON_ORE : Blocks.COAL_ORE);
                  }
                  setW(ch, ox + dx, y, oz + dz, id);
               }
            }
         }
      }
   }

   private void buildSmallOak(Chunk ch, int tx, int ty, int tz) {
      for (int lz = -2; lz <= 2; lz++) {
         for (int lx = -2; lx <= 2; lx++) {
            if (Math.abs(lx) == 2 && Math.abs(lz) == 2) {
               continue;
            }
            setW(ch, tx + lx, ty + 2, tz + lz, Blocks.LEAVES_OAK);
            setW(ch, tx + lx, ty + 3, tz + lz, Blocks.LEAVES_OAK);
         }
      }
      for (int lz = -1; lz <= 1; lz++) {
         for (int lx = -1; lx <= 1; lx++) {
            if (Math.abs(lx) + Math.abs(lz) <= 1) {
               setW(ch, tx + lx, ty + 4, tz + lz, Blocks.LEAVES_OAK);
            }
         }
      }
      for (int y = 0; y < 4; y++) {
         setW(ch, tx, ty + y, tz, Blocks.LOG_OAK);
      }
   }

   private static void putChestItems(World w, int x, int y, int z, Stack... items) {
      if (w.chunkAt(x, z) == null) {
         return;
      }
      int cur = w.getBlock(x, y, z);
      if (cur == Blocks.CHEST_LOOT || cur == Blocks.CHEST) {
         w.setBlock(x, y, z, Blocks.CHEST);
         TileEntity te = w.getOrCreateTile(x, y, z);
         if (te != null && te.slots != null) {
            for (int i = 0; i < te.slots.length; i++) {
               te.slots[i] = i < items.length ? items[i] : null;
            }
         }
      }
   }

   public static void stockSkyblockChests(Game g, World w) {
      if (w.chunk(0, 0) == null) {
         return;
      }
      // Starter Island Chest at (3, 65, 0)
      putChestItems(
         w,
         3,
         Y_DECK + 1,
         0,
         new Stack(Blocks313.ICE, 2),
         new Stack(Items.LAVA_BUCKET, 1),
         new Stack(Items.WATER_BUCKET, 1),
         new Stack(Items.SEEDS, 8),
         new Stack(Items.MELON_SLICE, 4),
         new Stack(Items.PUMPKIN_SEEDS, 2),
         new Stack(Items.MELON_SEEDS, 2),
         new Stack(Blocks.SUGAR_CANE, 2),
         new Stack(Items.CARROT, 2),
         new Stack(Items.POTATO, 2),
         new Stack(Items.BONE_MEAL, 12),
         new Stack(Items.BREAD, 8),
         new Stack(Blocks.COBBLE, 16)
      );
      // East Desert Island Chest at (47, 65, -1)
      putChestItems(
         w,
         47,
         Y_DECK + 1,
         -1,
         new Stack(Items.GOLD_INGOT, 8),
         new Stack(Blocks.SAND, 32),
         new Stack(Blocks.CACTUS, 4),
         new Stack(Blocks.SUGAR_CANE, 4),
         new Stack(Items.toolId(2, Items.C_PICK), 1),
         new Stack(Items.APPLE, 4)
      );
      // West Nether Shrine Chest at (-47, 65, 0)
      putChestItems(
         w,
         -47,
         Y_DECK + 1,
         0,
         new Stack(Blocks.OBSIDIAN, 10),
         new Stack(Items.FLINT_STEEL, 1),
         new Stack(Items.BLAZE_ROD, 4),
         new Stack(Items.QUARTZ, 16),
         new Stack(Items.GHAST_TEAR, 2),
         new Stack(Blocks.GLOWSTONE, 8)
      );
      // North Snowy Taiga Chest at (0, 65, -47)
      putChestItems(
         w,
         0,
         Y_DECK + 1,
         -47,
         new Stack(Items.DIAMOND, 2),
         new Stack(Blocks.LOG_SPRUCE, 8),
         new Stack(Blocks313.ICE, 8),
         new Stack(Items.BONE, 6),
         new Stack(Items.toolId(2, Items.C_AXE), 1)
      );
      // South Jungle & Trader Chest at (0, 65, 47)
      putChestItems(
         w,
         0,
         Y_DECK + 1,
         47,
         new Stack(Items.EMERALD, 8),
         new Stack(Blocks.BOOKSHELF, 4),
         new Stack(Items.SHEARS, 1),
         new Stack(Items.APPLE, 6),
         new Stack(Items.CAKE, 1)
      );
      if (g != null && g.mobs != null && w.chunkAt(0, 48) != null) {
         boolean hasVillager = false;
         for (Mobs.Mob m : g.mobs.list) {
            if (!m.dead && m.type == Mobs.VILLAGER && Math.hypot(m.x - 0.5, m.z - 48.5) < 12.0) {
               hasVillager = true;
               break;
            }
         }
         if (!hasVillager) {
            Mobs.Mob v = g.mobs.spawn(Mobs.VILLAGER, 0.5, Y_DECK + 1.0, 48.5);
            if (v != null) {
               v.color = Trades.FARMER;
            }
         }
      }
      skyblockStocked = true;
   }

   public static void stockSkywarsChests(Game g, World w, boolean refill) {
      if (w.chunk(0, 0) == null) {
         return;
      }
      // 1. 8 Outer Spawn Island Chests
      for (int i = 0; i < SKYWARS_SPAWNS.length; i++) {
         int[] sp = SKYWARS_SPAWNS[i];
         putChestItems(
            w,
            sp[0] + 2,
            Y_DECK + 1,
            sp[1],
            new Stack(i % 2 == 0 ? Items.toolId(1, Items.C_SWORD) : Items.toolId(2, Items.C_AXE), 1),
            new Stack(i % 2 == 0 ? Blocks.PLANKS : Blocks.COBBLE, 32),
            new Stack(Items.armorId(i % 2, 1), 1),
            new Stack(Items.armorId(0, 3), 1),
            new Stack(Items.STEAK, 6),
            new Stack(i % 3 == 0 ? Items.ENDER_PEARL : Items.WATER_BUCKET, 1),
            new Stack(Items.BOW, 1),
            new Stack(Items.ARROW, 12)
         );
         if (!refill && i > 0 && g != null && g.mobs != null && w.chunkAt(sp[0], sp[1]) != null) {
            int mobType = (i % 2 == 0) ? Mobs.SKELETON : Mobs.ZOMBIE;
            g.mobs.spawn(mobType, sp[0] + 0.5, Y_DECK + 1.0, sp[1] + 0.5);
         }
      }
      // 2. 4 Mid-Ring Island Chests
      for (int[] m : SKYWARS_MIDS) {
         putChestItems(
            w,
            m[0],
            Y_DECK + 1,
            m[1],
            new Stack(Items.toolId(2, Items.C_SWORD), 1),
            new Stack(Items.armorId(1, 0), 1),
            new Stack(Items.armorId(1, 2), 1),
            new Stack(Items.ENDER_PEARL, 2),
            new Stack(Blocks.TNT, 4),
            new Stack(Items.FLINT_STEEL, 1),
            new Stack(Items.STEAK, 8),
            new Stack(Blocks.STONEBRICK, 32)
         );
      }
      // 3. 4 Center Feast Chests
      int[][] feastChests = new int[][]{{-2, 0}, {2, 0}, {0, -2}, {0, 2}};
      for (int i = 0; i < feastChests.length; i++) {
         int[] fc = feastChests[i];
         putChestItems(
            w,
            fc[0],
            Y_DECK + 1,
            fc[1],
            new Stack(Items.toolId(4, i % 2 == 0 ? Items.C_SWORD : Items.C_AXE), 1),
            new Stack(Items.armorId(3, i % 4), 1),
            new Stack(Items.ENDER_PEARL, 3),
            new Stack(Blocks.STONEBRICK, 64),
            new Stack(Items.STEAK, 12),
            new Stack(Items.ARROW, 24)
         );
      }
      skywarsStocked = true;
   }

   public static void stockBedwarsWorld(Game g, World w) {
      if (w.chunk(0, 0) == null) {
         return;
      }
      // Center Emerald Chest
      putChestItems(
         w,
         0,
         Y_DECK + 1,
         0,
         new Stack(Items.EMERALD, 2),
         new Stack(Items.ENDER_PEARL, 1),
         new Stack(Blocks.OBSIDIAN, 4),
         new Stack(Items.STEAK, 4)
      );
      for (int t = 0; t < 4; t++) {
         int bx = BEDWARS_BASES[t][0];
         int bz = BEDWARS_BASES[t][1];
         if (w.chunkAt(bx, bz) == null) {
            continue;
         }
         int dirX = Integer.compare(0, bx);
         int dirZ = Integer.compare(0, bz);
         int sx = bx + (dirX == 0 ? 3 : 0);
         int sz = bz + (dirZ == 0 ? 3 : 0);
         int wool = Blocks.WOOL0 + BEDWARS_COLORS[t];
         putChestItems(
            w,
            sx,
            Y_DECK + 1,
            sz,
            new Stack(Items.toolId(0, Items.C_SWORD), 1),
            new Stack(wool, 48),
            new Stack(Items.toolId(1, Items.C_PICK), 1),
            new Stack(Items.toolId(1, Items.C_AXE), 1),
            new Stack(Items.SHEARS, 1),
            new Stack(Items.STEAK, 8)
         );
         if (g != null && g.mobs != null) {
            if (t == 0) {
               // Spawn Item Shopkeeper & Gear/Upgrades Shopkeeper on player's base
               Mobs.Mob shop1 = g.mobs.spawn(Mobs.VILLAGER, sx + 0.5, Y_DECK + 1.0, sz - 1.5);
               if (shop1 != null) {
                  shop1.color = Trades.SMITH;
               }
               int ux = bx - (dirX == 0 ? 3 : 0);
               int uz = bz - (dirZ == 0 ? 3 : 0);
               Mobs.Mob shop2 = g.mobs.spawn(Mobs.VILLAGER, ux + 0.5, Y_DECK + 1.0, uz - 1.5);
               if (shop2 != null) {
                  shop2.color = Trades.PRIEST;
               }
            } else {
               // Spawn Defender Guards on enemy team bases
               g.mobs.spawn(t % 2 == 1 ? Mobs.ZOMBIE : Mobs.SKELETON, bx + 0.5, Y_DECK + 1.0, bz + 0.5);
            }
         }
      }
      bedwarsStocked = true;
   }

   public static boolean isTeamBedIntact(World w, int team) {
      if (w == null || team < 0 || team >= 4) {
         return false;
      }
      int bx = BEDWARS_BASES[team][0];
      int bz = BEDWARS_BASES[team][1];
      int dirX = Integer.compare(0, bx);
      int dirZ = Integer.compare(0, bz);
      int bedFootX = bx + dirX * 3;
      int bedFootZ = bz + dirZ * 3;
      if (w.chunkAt(bedFootX, bedFootZ) == null) {
         return bedKnownIntact[team];
      }
      return Decor315.isBed(w.getBlock(bedFootX, Y_DECK + 1, bedFootZ));
   }

   /** Runs per-tick gameplay logic for Skyblock, Skywars, and Bedwars worlds. */
   public static void tickMode(Game g, World w) {
      if (w == null || w.settings == null) {
         return;
      }
      int type = w.settings.type;
      if (!w.settings.isVoidMode()) {
         return;
      }
      if (w.seed != lastWorldSeed) {
         lastWorldSeed = w.seed;
         resetMatchState();
      }

      // Fast void death so players don't fall endlessly below Y=-18
      if (g != null && g.player != null && !g.player.dead && !g.player.creative && g.player.y < -18.0) {
         g.player.hurt(20);
      }

      // 1. SKYBLOCK: stock island chests & evaluate Skyblock Progression Quests
      if (type == WorldSettings.SKYBLOCK) {
         if (!skyblockStocked && w.chunk(0, 0) != null) {
            stockSkyblockChests(g, w);
         }
         if (g != null && g.player != null && w.tickCount % 20L == 0L) {
            checkSkyblockQuests(g);
         }
         return;
      }

      // 2. SKYWARS: stock chests, periodic chest refills, and victory check
      if (type == WorldSettings.SKYWARS) {
         if (!skywarsStocked && w.chunk(0, 0) != null) {
            stockSkywarsChests(g, w, false);
         }
         if (w.tickCount > 0L && w.tickCount % 3600L == 0L) {
            stockSkywarsChests(g, w, true);
            if (g != null) {
               g.say("\u00a7b[Skywars] \u00a7eAll island and Feast chests have been REFILLED!");
            }
         }
         if (!skywarsWon && g != null && g.mobs != null && w.tickCount > 100L && w.tickCount % 40L == 0L) {
            int raiders = countHostiles(g);
            if (raiders == 0 && skywarsStocked) {
               skywarsWon = true;
               g.say("\u00a76\u00a7l[Skywars] VICTORY! \u00a7aYou are the last champion standing! (/sw reset for a new round)");
            }
         }
         return;
      }

      // 3. BEDWARS: stock team bases, resource generators, bed destruction alerts & victory check
      if (type == WorldSettings.BEDWARS) {
         if (!bedwarsStocked && w.chunk(0, 0) != null) {
            stockBedwarsWorld(g, w);
         }
         if (w.tickCount % 60L == 0L) {
            for (int t = 0; t < 4; t++) {
               int bx = BEDWARS_BASES[t][0];
               int bz = BEDWARS_BASES[t][1];
               int dirX = Integer.compare(0, bx);
               int dirZ = Integer.compare(0, bz);
               double gx = bx - dirX * 4 + 0.5;
               double gz = bz - dirZ * 4 + 0.5;
               if (w.chunkAt((int)gx, (int)gz) != null) {
                  w.dropItem(gx, Y_DECK + 1.2, gz, Items.IRON_INGOT, 1);
                  w.dropItem(gx, Y_DECK + 1.2, gz, Blocks.WOOL0 + BEDWARS_COLORS[t], 4);
                  if (w.tickCount % 180L == 0L) {
                     w.dropItem(gx, Y_DECK + 1.2, gz, Items.GOLD_INGOT, 1);
                  }
               }
            }
         }
         if (w.tickCount % 300L == 0L) {
            for (int[] d : BEDWARS_DIAMONDS) {
               if (w.chunkAt(d[0], d[1]) != null) {
                  w.dropItem(d[0] + 0.5, Y_DECK + 1.2, d[1] + 0.5, Items.DIAMOND, 1);
               }
            }
         }
         if (w.tickCount % 500L == 0L) {
            if (w.chunkAt(0, 0) != null) {
               w.dropItem(-3.5, Y_DECK + 1.2, 0.5, Items.EMERALD, 1);
               w.dropItem(4.5, Y_DECK + 1.2, 0.5, Items.EMERALD, 1);
            }
         }
         if (g != null && g.player != null && w.tickCount % 20L == 0L) {
            int enemyBeds = 0;
            for (int t = 0; t < 4; t++) {
               boolean intact = isTeamBedIntact(w, t);
               if (bedKnownIntact[t] && !intact) {
                  bedKnownIntact[t] = false;
                  if (t == 0) {
                     g.bedPos = null;
                     g.say("\u00a7c\u00a7l[Bedwars] YOUR BED WAS DESTROYED! \u00a77You will no longer respawn!");
                  } else {
                     g.say("\u00a7e[Bedwars] " + BEDWARS_CODES[t] + BEDWARS_NAMES[t] + " Team's Bed \u00a77was destroyed!");
                  }
               }
               if (t == 0 && intact) {
                  int bx = BEDWARS_BASES[0][0];
                  int bz = BEDWARS_BASES[0][1];
                  g.bedPos = new int[]{bx, Y_DECK + 1, bz + 3};
               }
               if (t > 0 && intact) {
                  enemyBeds++;
               }
            }
            if (!bedwarsWon && bedwarsStocked && enemyBeds == 0 && countHostiles(g) == 0) {
               bedwarsWon = true;
               g.say("\u00a76\u00a7l[Bedwars] VICTORY! \u00a7aAll enemy beds and defenders eliminated! (/bw reset to play again)");
            }
         }
      }
   }

   private static int countHostiles(Game g) {
      if (g == null || g.mobs == null) {
         return 0;
      }
      int c = 0;
      for (Mobs.Mob m : g.mobs.list) {
         if (!m.dead && Mobs.hostile(m.type) && m.y > 0.0) {
            c++;
         }
      }
      return c;
   }

   private static void checkSkyblockQuests(Game g) {
      // Quest 0: 32 Cobblestone
      if (!sbQuestDone[0] && g.countItem(Blocks.COBBLE) >= 32) {
         sbQuestDone[0] = true;
         g.addItem(new Stack(Items.IRON_INGOT, 8));
         g.addItem(new Stack(Items.COAL, 16));
         g.say("\u00a7a[Skyblock Quest] Completed 'Cobble Generator'! Reward: +8 Iron Ingots, +16 Coal");
      }
      // Quest 1: 8 Wheat or Melon Slices
      if (!sbQuestDone[1] && (g.countItem(Items.WHEAT) + g.countItem(Items.MELON_SLICE)) >= 8) {
         sbQuestDone[1] = true;
         g.addItem(new Stack(Blocks.DIRT, 8));
         g.addItem(new Stack(Blocks.GRASS, 4));
         g.addItem(new Stack(Items.BONE_MEAL, 16));
         g.say("\u00a7a[Skyblock Quest] Completed 'Sky Harvester'! Reward: +8 Dirt, +4 Grass, +16 Bone Meal");
      }
      // Quest 2: Bridge to an outer island (|X| >= 40 or |Z| >= 40)
      if (!sbQuestDone[2] && (Math.abs(g.player.x) >= 40.0 || Math.abs(g.player.z) >= 40.0)) {
         sbQuestDone[2] = true;
         g.addItem(new Stack(Items.DIAMOND, 3));
         g.addItem(new Stack(Items.STEAK, 4));
         g.say("\u00a7a[Skyblock Quest] Completed 'Island Bridger'! Reward: +3 Diamonds, +4 Steak");
      }
      // Quest 3: 4 Monster Drops (Rotten Flesh, Bone, String, or Gunpowder)
      int drops = g.countItem(Items.ROTTEN_FLESH) + g.countItem(Items.BONE) + g.countItem(Items.STRING) + g.countItem(Items.GUNPOWDER);
      if (!sbQuestDone[3] && drops >= 4) {
         sbQuestDone[3] = true;
         g.addItem(new Stack(Items.EMERALD, 6));
         g.addItem(new Stack(Items.BOW, 1));
         g.addItem(new Stack(Items.ARROW, 16));
         g.say("\u00a7a[Skyblock Quest] Completed 'Void Hunter'! Reward: +6 Emeralds, +1 Bow, +16 Arrows");
      }
      // Quest 4: 10 Obsidian
      if (!sbQuestDone[4] && g.countItem(Blocks.OBSIDIAN) >= 10) {
         sbQuestDone[4] = true;
         g.addItem(new Stack(Items.toolId(4, Items.C_PICK), 1));
         g.addItem(new Stack(Blocks.GOLD_BLOCK, 2));
         g.say("\u00a76[Skyblock Quest] Completed 'Nether Pioneer'! Reward: +1 Diamond Pickaxe, +2 Gold Blocks!");
      }
   }

   /** Handles /sb, /challenges, /sw, /bw, /shop, and /upgrades commands. */
   public static boolean onModeCommand(Game g, String cmd, String[] args) {
      if (g == null || g.world == null) {
         return false;
      }
      String c = cmd.toLowerCase();
      if (c.equals("/shop")) {
         g.menu = new Menu(g, Menu.TRADE, null, Trades.BEDWARS_SHOP);
         g.setScreen(Game.S.INV);
         return true;
      }
      if (c.equals("/upgrades")) {
         g.menu = new Menu(g, Menu.TRADE, null, Trades.BEDWARS_ARMOR);
         g.setScreen(Game.S.INV);
         return true;
      }
      if (c.equals("/sb") || c.equals("/challenges")) {
         int done = 0;
         for (boolean b : sbQuestDone) {
            if (b) {
               done++;
            }
         }
         g.say("\u00a7b[Skyblock Quests] (" + done + "/" + SB_QUEST_NAMES.length + " completed):");
         for (int i = 0; i < SB_QUEST_NAMES.length; i++) {
            g.say((sbQuestDone[i] ? " \u00a7a\u2714 " : " \u00a77\u25cb ") + SB_QUEST_NAMES[i]);
         }
         return true;
      }
      if (c.equals("/sw")) {
         String sub = args.length > 1 ? args[1].toLowerCase() : "status";
         if (sub.equals("refill")) {
            stockSkywarsChests(g, g.world, true);
            g.say("\u00a7b[Skywars] All chests refilled!");
         } else if (sub.equals("reset")) {
            skywarsStocked = false;
            skywarsWon = false;
            stockSkywarsChests(g, g.world, false);
            g.say("\u00a7b[Skywars] Match reset! Islands restocked and gladiators spawned.");
         } else {
            long rem = (3600L - (g.world.tickCount % 3600L)) / 20L;
            g.say("\u00a7b[Skywars] Raiders remaining: " + countHostiles(g) + " | Next chest refill in " + rem + "s (/sw refill | /sw reset)");
         }
         return true;
      }
      if (c.equals("/bw")) {
         String sub = args.length > 1 ? args[1].toLowerCase() : "status";
         if (sub.equals("shop")) {
            g.menu = new Menu(g, Menu.TRADE, null, Trades.BEDWARS_SHOP);
            g.setScreen(Game.S.INV);
            return true;
         }
         if (sub.equals("upgrades")) {
            g.menu = new Menu(g, Menu.TRADE, null, Trades.BEDWARS_ARMOR);
            g.setScreen(Game.S.INV);
            return true;
         }
         if (sub.equals("reset")) {
            resetMatchState();
            for (int t = 0; t < 4; t++) {
               int bx = BEDWARS_BASES[t][0];
               int bz = BEDWARS_BASES[t][1];
               int dirX = Integer.compare(0, bx);
               int dirZ = Integer.compare(0, bz);
               int facing = dirZ < 0 ? 0 : (dirZ > 0 ? 1 : (dirX > 0 ? 2 : 3));
               int color = BEDWARS_COLORS[t];
               g.world.setBlock(bx + dirX * 3, Y_DECK + 1, bz + dirZ * 3, Decor315.foot(color, facing));
               g.world.setBlock(bx + dirX * 4, Y_DECK + 1, bz + dirZ * 4, Decor315.head(color, facing));
            }
            stockBedwarsWorld(g, g.world);
            g.say("\u00a7e[Bedwars] Match reset! All 4 Team Beds restored.");
            return true;
         }
         StringBuilder sb = new StringBuilder("\u00a7e[Bedwars] Teams: ");
         for (int t = 0; t < 4; t++) {
            sb.append(BEDWARS_CODES[t]).append(BEDWARS_NAMES[t]).append(isTeamBedIntact(g.world, t) ? ":\u00a7aBED " : ":\u00a7cOUT ");
         }
         sb.append("\u00a77(/bw shop | /bw upgrades | /bw reset)");
         g.say(sb.toString());
         return true;
      }
      return false;
   }

   /** Renders the top-right Scoreboard / Quest HUD for Skyblock, Skywars, and Bedwars. */
   public static void drawModeHud(Game g) {
      if (g == null || g.world == null || g.world.settings == null || g.debug) {
         return;
      }
      int type = g.world.settings.type;
      if (!g.world.settings.isVoidMode()) {
         return;
      }
      Gui gui = g.gui;
      java.util.ArrayList<String> lines = new java.util.ArrayList<>();
      if (type == WorldSettings.SKYBLOCK) {
         lines.add("\u00a7b\u00a7lSKYBLOCK");
         int done = 0;
         int nextIdx = -1;
         for (int i = 0; i < SB_QUEST_NAMES.length; i++) {
            if (sbQuestDone[i]) {
               done++;
            } else if (nextIdx < 0) {
               nextIdx = i;
            }
         }
         lines.add("\u00a7fQuests: \u00a7a" + done + "/" + SB_QUEST_NAMES.length);
         if (nextIdx >= 0) {
            lines.add("\u00a7e" + SB_QUEST_NAMES[nextIdx]);
         } else {
            lines.add("\u00a76All Quests Complete!");
         }
         lines.add("\u00a77Type /sb for quest list");
      } else if (type == WorldSettings.SKYWARS) {
         long rem = (3600L - (g.world.tickCount % 3600L)) / 20L;
         int raiders = countHostiles(g);
         lines.add("\u00a76\u00a7lSKYWARS");
         lines.add("\u00a7fRaiders Left: " + (raiders > 0 ? "\u00a7c" + raiders : "\u00a7a0 (VICTORY!)"));
         lines.add(String.format("\u00a7fChest Refill: \u00a7b%d:%02d", rem / 60L, rem % 60L));
         lines.add("\u00a77/sw refill | /sw reset");
      } else if (type == WorldSettings.BEDWARS) {
         long dRem = (300L - (g.world.tickCount % 300L)) / 20L;
         long eRem = (500L - (g.world.tickCount % 500L)) / 20L;
         lines.add("\u00a7e\u00a7lBEDWARS");
         for (int t = 0; t < 4; t++) {
            boolean bed = isTeamBedIntact(g.world, t);
            lines.add(BEDWARS_CODES[t] + "[" + BEDWARS_NAMES[t].charAt(0) + "] " + BEDWARS_NAMES[t] + ": " + (bed ? "\u00a7aBED \u2714" : "\u00a7cBROKEN \u2718") + (t == 0 ? " \u00a77(You)" : ""));
         }
         lines.add("\u00a7bDiamond: " + dRem + "s \u00a72Emerald: " + eRem + "s");
         lines.add("\u00a77/shop | /upgrades");
      }

      if (lines.isEmpty()) {
         return;
      }
      float maxW = 90.0F;
      for (String s : lines) {
         maxW = Math.max(maxW, Gui.textWidth(Gui.stripCodes(s)) + 8.0F);
      }
      float boxH = lines.size() * 10.0F + 6.0F;
      float x = gui.w - maxW - 4.0F;
      float y = 26.0F;
      gui.rect(x, y, maxW, boxH, 0x90000000);
      gui.rect(x, y, maxW, 1.0F, 0xFF555555);
      for (int i = 0; i < lines.size(); i++) {
         gui.text(lines.get(i), x + 4.0F, y + 3.0F + i * 10.0F, 0xFFFFFFFF, true);
      }
   }
}
