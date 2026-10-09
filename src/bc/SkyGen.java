package bc;

/**
 * World generator and gameplay rules for {@link WorldSettings#SKYBLOCK}, {@link WorldSettings#SKYWARS},
 * and {@link WorldSettings#BEDWARS}.
 */
public final class SkyGen extends Gen {
   public static final int Y_DECK = 64;

   // 8 Skywars spawn island centers (radius 42)
   public static final int[][] SKYWARS_SPAWNS = new int[][]{
      {42, 0}, {30, 30}, {0, 42}, {-30, 30}, {-42, 0}, {-30, -30}, {0, -42}, {30, -30}
   };

   // 4 Bedwars team base centers: North (Red), South (Blue), West (Green), East (Yellow)
   public static final int[][] BEDWARS_BASES = new int[][]{
      {0, -44}, {0, 44}, {-44, 0}, {44, 0}
   };
   public static final int[] BEDWARS_COLORS = new int[]{14, 11, 13, 4}; // Red, Blue, Green, Yellow
   public static final String[] BEDWARS_NAMES = new String[]{"Red", "Blue", "Green", "Yellow"};

   // 4 Bedwars diamond generator islands
   public static final int[][] BEDWARS_DIAMONDS = new int[][]{
      {24, 24}, {-24, 24}, {24, -24}, {-24, -24}
   };

   private final int mode;

   public SkyGen(long seed, WorldSettings st) {
      super(seed);
      this.settings = st;
      this.mode = st.type;
   }

   @Override
   public Gen.Col column(int x, int z) {
      Gen.Col c = new Gen.Col();
      c.t = 0.5;
      c.hu = 0.5;
      c.biome = Biomes.PLAINS;
      c.h = surfaceHeightAt(x, z);
      if (c.h > 0 && this.mode == WorldSettings.BEDWARS) {
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
         if (isSkyblockMain(x, z) || isSkyblockSand(x, z) || isSkyblockShrine(x, z)) {
            return Y_DECK;
         }
         return 0;
      }
      if (this.mode == WorldSettings.SKYWARS) {
         double d0 = Math.hypot(x, z);
         if (d0 <= 11.2) {
            return Y_DECK;
         }
         for (int[] m : new int[][]{{24, 0}, {-24, 0}, {0, 24}, {0, -24}}) {
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
      // Classic Oak Tree at (4, 65, -1) -> or (1, 65, 3)
      buildSmallOak(ch, 1, Y_DECK + 1, 3);
      // Starter Chest at (3, 65, 0)
      setW(ch, 3, Y_DECK + 1, 0, Blocks.CHEST_LOOT);

      // 2. Distant Sand Island at (46..50, 62..64, -2..2)
      for (int wz = -2; wz <= 2; wz++) {
         for (int wx = 46; wx <= 50; wx++) {
            setW(ch, wx, Y_DECK - 2, wz, Blocks.SANDSTONE);
            setW(ch, wx, Y_DECK - 1, wz, Blocks.SAND);
            setW(ch, wx, Y_DECK, wz, Blocks.SAND);
         }
      }
      setW(ch, 48, Y_DECK + 1, 0, Blocks.CACTUS);
      setW(ch, 48, Y_DECK + 2, 0, Blocks.CACTUS);
      setW(ch, 47, Y_DECK + 1, -1, Blocks.CHEST_LOOT);

      // 3. Distant Obsidian/Quartz Shrine Island at (-50..-46, 62..64, -2..2)
      for (int wz = -2; wz <= 2; wz++) {
         for (int wx = -50; wx <= -46; wx++) {
            setW(ch, wx, Y_DECK - 2, wz, Blocks.OBSIDIAN);
            setW(ch, wx, Y_DECK - 1, wz, Blocks.NETHERRACK);
            setW(ch, wx, Y_DECK, wz, Blocks.QUARTZ_BLOCK);
         }
      }
      setW(ch, -48, Y_DECK + 1, 0, Blocks.GLOWSTONE);
      setW(ch, -47, Y_DECK + 1, 0, Blocks.CHEST_LOOT);
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
      for (int[] m : new int[][]{{24, 0}, {-24, 0}, {0, 24}, {0, -24}}) {
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
         // Direction toward center (0,0)
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

         // Team Shop / Chest & Crafting Table on the sides
         int sx = bx + (dirX == 0 ? 3 : 0);
         int sz = bz + (dirZ == 0 ? 3 : 0);
         setW(ch, sx, Y_DECK + 1, sz, Blocks.CHEST_LOOT);
         setW(ch, bx - (dirX == 0 ? 3 : 0), Y_DECK + 1, bz - (dirZ == 0 ? 3 : 0), Blocks.CRAFTING_TABLE);

         // Team Bed on front courtyard (4 blocks toward center from base center)
         int bedFootX = bx + dirX * 3;
         int bedFootZ = bz + dirZ * 3;
         int bedHeadX = bx + dirX * 4;
         int bedHeadZ = bz + dirZ * 4;
         // facing: 0 north (-Z), 1 south (+Z), 2 east (+X), 3 west (-X)
         int facing = dirZ < 0 ? 0 : (dirZ > 0 ? 1 : (dirX > 0 ? 2 : 3));
         setW(ch, bedFootX, Y_DECK + 1, bedFootZ, Decor315.foot(color, facing));
         setW(ch, bedHeadX, Y_DECK + 1, bedHeadZ, Decor315.head(color, facing));
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

   /** Runs per-tick gameplay logic for Skyblock, Skywars, and Bedwars worlds. */
   public static void tickMode(Game g, World w) {
      if (w == null || w.settings == null) {
         return;
      }
      int type = w.settings.type;
      if (!w.settings.isVoidMode()) {
         return;
      }

      // Fast void death so players don't fall endlessly below Y=-20
      if (g != null && g.player != null && !g.player.dead && !g.player.creative && g.player.y < -18.0) {
         g.player.hurt(20);
      }

      // Skyblock: stock the starter chest at (3, 65, 0) once chunk (0, 0) is loaded
      if (type == WorldSettings.SKYBLOCK && w.tickCount == 10L) {
         Chunk c0 = w.chunk(0, 0);
         if (c0 != null && w.getBlock(3, Y_DECK + 1, 0) == Blocks.CHEST_LOOT) {
            w.setBlock(3, Y_DECK + 1, 0, Blocks.CHEST);
            TileEntity te = w.getOrCreateTile(3, Y_DECK + 1, 0);
            if (te != null && te.slots != null && te.slots.length >= 10) {
               te.slots[0] = new Stack(Blocks313.ICE, 2);
               te.slots[1] = new Stack(Items.LAVA_BUCKET, 1);
               te.slots[2] = new Stack(Items.WATER_BUCKET, 1);
               te.slots[3] = new Stack(Items.SEEDS, 4);
               te.slots[4] = new Stack(Items.MELON_SLICE, 4);
               te.slots[5] = new Stack(Items.BONE_MEAL, 8);
               te.slots[6] = new Stack(Items.BREAD, 6);
               te.slots[7] = new Stack(Blocks.COBBLE, 16);
            }
         }
      }

      // Bedwars: periodic resource generators & bed respawn tracking
      if (type == WorldSettings.BEDWARS) {
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
            // Find nearest team base (or Red base by default)
            int bx = BEDWARS_BASES[0][0];
            int bz = BEDWARS_BASES[0][1];
            int dirZ = 1;
            int bedFootZ = bz + dirZ * 3;
            if (w.chunkAt(bx, bedFootZ) != null) {
               boolean intact = Decor315.isBed(w.getBlock(bx, Y_DECK + 1, bedFootZ));
               if (intact) {
                  g.bedPos = new int[]{bx, Y_DECK + 1, bedFootZ};
               } else if (g.bedPos != null) {
                  g.bedPos = null;
                  g.say("§c[Bedwars] Your team's Bed was destroyed! You will no longer respawn!");
               }
            }
         }
      }
   }
}
