package bc;

import java.util.Arrays;

/**
 * World generator and full gameplay engine for {@link WorldSettings#SKYBLOCK}, {@link WorldSettings#SKYWARS},
 * and {@link WorldSettings#BEDWARS} (Update 4.1).
 */
public final class SkyGen extends Gen {
   public static final int Y_DECK = 64;

   // Minigame lobby / match states
   public static final int STATE_WAITING = 0;
   public static final int STATE_PLAYING = 1;
   public static volatile int matchState = STATE_WAITING;
   public static volatile int lobbyCountdownTicks = -1;
   public static volatile int myTeam = 0;
   public static volatile boolean soloBots = true;

   // 8 Skywars spawn island centers (radius 42)
   public static final int[][] SKYWARS_SPAWNS = new int[][]{
      {42, 0}, {30, 30}, {0, 42}, {-30, 30}, {-42, 0}, {-30, -30}, {0, -42}, {30, -30}
   };

   // 4 Skywars mid-ring island centers
   public static final int[][] SKYWARS_MIDS = new int[][]{
      {24, 0}, {-24, 0}, {0, 24}, {0, -24}
   };

   // 4 Bedwars team base centers: North (Red=0), South (Blue=1), West (Green=2), East (Yellow=3)
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
   private static World lastWorldRef = null;
   private static final boolean[] sbIslandStocked = new boolean[5];
   private static final boolean[] swIslandStocked = new boolean[13];
   private static boolean skywarsWon;
   private static final boolean[] bwBaseStocked = new boolean[5];
   private static boolean bedwarsWon;
   private static final boolean[] bedKnownIntact = new boolean[]{true, true, true, true};

   private final int mode;

   public SkyGen(long seed, WorldSettings st) {
      super(seed);
      this.settings = st;
      this.mode = st.type;
   }

   public static void resetMatchState() {
      Arrays.fill(sbQuestDone, false);
      Arrays.fill(sbIslandStocked, false);
      Arrays.fill(swIslandStocked, false);
      skywarsWon = false;
      Arrays.fill(bwBaseStocked, false);
      bedwarsWon = false;
      Arrays.fill(bedKnownIntact, true);
      lobbyCountdownTicks = -1;
   }

   public static int getPlayerTeam(Game g) {
      int t = myTeam;
      if (t >= 0 && t < 4) {
         return t;
      }
      if (g != null && g.spawnXZ != null) {
         for (int i = 0; i < 4; i++) {
            if (Math.abs(g.spawnXZ[0] - BEDWARS_BASES[i][0]) <= 8 && Math.abs(g.spawnXZ[1] - BEDWARS_BASES[i][1]) <= 8) {
               myTeam = i;
               return i;
            }
         }
      }
      return 0;
   }

   public static int[] getTeamSpawnBlock(int team) {
      int t = Math.abs(team) & 3;
      return new int[]{BEDWARS_BASES[t][0], Y_DECK + 1, BEDWARS_BASES[t][1]};
   }

   public static float getTeamYaw(int team) {
      return switch (Math.abs(team) & 3) {
         case 0 -> 0.0F;    // North base (0, -44) facing South (+Z)
         case 1 -> 180.0F;  // South base (0, +44) facing North (-Z)
         case 2 -> -90.0F;  // West base (-44, 0) facing East (+X)
         default -> 90.0F;  // East base (+44, 0) facing West (-X)
      };
   }

   public static float getSkywarsYaw(int idx) {
      int[] sp = SKYWARS_SPAWNS[Math.abs(idx) % SKYWARS_SPAWNS.length];
      return (float)Math.toDegrees(Math.atan2(sp[0], -sp[1]));
   }

   public static int[] getTeamBedFoot(int team) {
      int t = Math.abs(team) & 3;
      int bx = BEDWARS_BASES[t][0];
      int bz = BEDWARS_BASES[t][1];
      int dirX = Integer.compare(0, bx);
      int dirZ = Integer.compare(0, bz);
      return new int[]{bx + dirX * 3, Y_DECK + 1, bz + dirZ * 3};
   }

   public static int[] getTeamBedHead(int team) {
      int t = Math.abs(team) & 3;
      int bx = BEDWARS_BASES[t][0];
      int bz = BEDWARS_BASES[t][1];
      int dirX = Integer.compare(0, bx);
      int dirZ = Integer.compare(0, bz);
      return new int[]{bx + dirX * 4, Y_DECK + 1, bz + dirZ * 4};
   }

   public static int getBedTeamAt(int x, int z) {
      for (int t = 0; t < 4; t++) {
         int[] f = getTeamBedFoot(t);
         int[] h = getTeamBedHead(t);
         if ((x == f[0] && z == f[2]) || (x == h[0] && z == h[2])) {
            return t;
         }
         if (Math.abs(x - BEDWARS_BASES[t][0]) <= 6 && Math.abs(z - BEDWARS_BASES[t][1]) <= 6) {
            return t;
         }
      }
      return -1;
   }

   public static int getBedTeamAt(int x, int y, int z) {
      return getBedTeamAt(x, z);
   }

   public static void giveBedwarsLoadout(Game g, int team) {
      if (g == null) {
         return;
      }
      int t = Math.abs(team) & 3;
      Arrays.fill(g.inv, null);
      Arrays.fill(g.armor, null);
      g.inv[0] = new Stack(Items.TOOL_BASE, 1); // Wooden Sword
      g.inv[1] = new Stack(Blocks.WOOL0 + BEDWARS_COLORS[t], 32); // Team Wool
      g.armor[0] = new Stack(Items.ARMOR_BASE, 1); // Leather Cap
      g.armor[1] = new Stack(Items.ARMOR_BASE + 1, 1); // Leather Tunic
      g.sel = 0;
   }

   public static void giveBedwarsRespawnKit(Game g, int team) {
      if (g == null) {
         return;
      }
      int t = Math.abs(team) & 3;
      g.inv[0] = new Stack(Items.TOOL_BASE, 1);
      g.inv[1] = new Stack(Blocks.WOOL0 + BEDWARS_COLORS[t], 16);
      if (g.armor[0] == null) {
         g.armor[0] = new Stack(Items.ARMOR_BASE, 1);
      }
      if (g.armor[1] == null) {
         g.armor[1] = new Stack(Items.ARMOR_BASE + 1, 1);
      }
      g.sel = 0;
   }

   public static void selectTeam(Game g, int team, boolean broadcast) {
      int t = Math.abs(team) & 3;
      myTeam = t;
      if (g != null && g.world != null && g.world.settings != null) {
         if (g.world.settings.type == WorldSettings.BEDWARS) {
            int[] sp = getTeamSpawnBlock(t);
            g.spawnXZ = new int[]{sp[0], sp[2]};
            g.bedPos = isTeamBedIntact(g.world, t) ? getTeamBedFoot(t) : null;
            if (g.player != null) {
               g.player.setMode(0);
               g.player.setPos(sp[0] + 0.5, Y_DECK + 1.0, sp[2] + 0.5);
               g.player.yaw = getTeamYaw(t);
               g.player.pitch = 0.0F;
               g.player.updateBox();
            }
            giveBedwarsLoadout(g, t);
         } else if (g.world.settings.type == WorldSettings.SKYWARS) {
            int idx = Math.abs(team) % SKYWARS_SPAWNS.length;
            int[] sp = SKYWARS_SPAWNS[idx];
            g.spawnXZ = new int[]{sp[0], sp[1]};
            g.bedPos = null;
            if (g.player != null) {
               g.player.setMode(0);
               g.player.setPos(sp[0] + 0.5, Y_DECK + 1.0, sp[1] + 0.5);
               g.player.yaw = getSkywarsYaw(idx);
               g.player.pitch = 0.0F;
               g.player.updateBox();
            }
         }
      }
      if (broadcast) {
         Net319.sendTeamSelect(t);
      }
   }

   public static void startMatch(Game g, boolean broadcast) {
      matchState = STATE_PLAYING;
      lobbyCountdownTicks = -1;
      Arrays.fill(swIslandStocked, false);
      Arrays.fill(bwBaseStocked, false);
      Arrays.fill(bedKnownIntact, true);
      skywarsWon = false;
      bedwarsWon = false;
      if (g != null && g.world != null && g.world.settings != null) {
         g.world.items.clear();
         if (g.mobs != null) {
            g.mobs.clear();
         }
         selectTeam(g, myTeam, false);
         if (g.player != null) {
            g.player.dead = false;
            g.player.health = 20;
            g.player.food = 20;
            g.player.sat = 5.0F;
            g.player.fallDistance = 0.0F;
         }
         if (g.screen == Game.S.WAITING) {
            g.setScreen(Game.S.PLAY);
         }
         String modeTitle = g.world.settings.type == WorldSettings.BEDWARS
            ? "\u00a7e\u00a7l[Bedwars] \u00a7aMatch Started! You are on " + BEDWARS_CODES[myTeam] + BEDWARS_NAMES[myTeam] + " Team\u00a7a! Protect your bed!"
            : "\u00a76\u00a7l[Skywars] \u00a7aMatch Started! Loot your island chests and conquer the Feast!";
         g.say(modeTitle);
      }
      if (broadcast) {
         Net319.sendMatchControl("START");
      }
   }

   public static void onBedDestroyed(Game g, World w, int team, String breakerName, boolean broadcast) {
      if (team < 0 || team >= 4) {
         return;
      }
      boolean wasIntact = bedKnownIntact[team];
      bedKnownIntact[team] = false;
      if (w != null) {
         int[] foot = getTeamBedFoot(team);
         int[] head = getTeamBedHead(team);
         if (w.chunkAt(foot[0], foot[2]) != null && Decor315.isBed(w.getBlock(foot[0], foot[1], foot[2]))) {
            w.setBlock(foot[0], foot[1], foot[2], 0);
         }
         if (w.chunkAt(head[0], head[2]) != null && Decor315.isBed(w.getBlock(head[0], head[1], head[2]))) {
            w.setBlock(head[0], head[1], head[2], 0);
         }
      }
      int my = getPlayerTeam(g);
      if (g != null && my == team) {
         g.bedPos = null;
      }
      if (wasIntact && g != null) {
         String byWho = (breakerName != null && !breakerName.isBlank()) ? " by \u00a7f" + breakerName : "";
         if (my == team) {
            g.say("\u00a7c\u00a7l[Bedwars] YOUR BED WAS DESTROYED" + byWho + "\u00a7c\u00a7l! \u00a77You will no longer respawn!");
         } else {
            g.say("\u00a7e[Bedwars] " + BEDWARS_CODES[team] + BEDWARS_NAMES[team] + " Team's Bed \u00a77was destroyed" + byWho + "\u00a77!");
         }
      }
      if (broadcast) {
         Net319.sendBedDestroyed(team, breakerName);
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
         int t = Math.abs(myTeam) & 3;
         return new int[]{BEDWARS_BASES[t][0], Y_DECK + 1, BEDWARS_BASES[t][1]};
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
      for (int z = 0; z < 16; z++) {
         for (int x = 0; x < 16; x++) {
            int idx = z * 16 + x;
            ch.biome[idx] = Biomes.PLAINS;
            ch.grassColor[idx] = Gen.grassColorFor(0.8, 0.4);
            ch.foliageColor[idx] = Gen.foliageColorFor(0.8, 0.4);
         }
      }

      if (this.mode == WorldSettings.SKYBLOCK) {
         generateSkyblock(ch);
      } else if (this.mode == WorldSettings.SKYWARS) {
         generateSkywars(ch);
      } else if (this.mode == WorldSettings.BEDWARS) {
         generateBedwars(ch);
      }

      ch.computeHeightmap();
      return ch;
   }

   private static void setW(Chunk ch, int wx, int wy, int wz, int id) {
      if (wy < 0 || wy > 255) {
         return;
      }
      if ((wx >> 4) == ch.cx && (wz >> 4) == ch.cz) {
         ch.set(wx & 15, wy, wz & 15, id);
      }
   }

   private void generateSkyblock(Chunk ch) {
      // 1. Classic L-shaped Skyblock starter island around (0, 64, 0)
      for (int wz = -1; wz <= 4; wz++) {
         for (int wx = -1; wx <= 4; wx++) {
            if (!isSkyblockMain(wx, wz)) {
               continue;
            }
            setW(ch, wx, Y_DECK - 2, wz, Blocks.DIRT);
            setW(ch, wx, Y_DECK - 1, wz, (wx == 0 && wz == 0) ? Blocks.SAND : Blocks.DIRT);
            setW(ch, wx, Y_DECK, wz, Blocks.GRASS);
         }
      }
      setW(ch, 0, Y_DECK - 2, 0, Blocks.BEDROCK);
      buildSmallOak(ch, -1, Y_DECK + 1, 4);
      setW(ch, 3, Y_DECK + 1, 0, Blocks.CHEST_LOOT);

      // 2. East Desert Oasis Island around (48, 64, 0)
      for (int wz = -2; wz <= 2; wz++) {
         for (int wx = 46; wx <= 50; wx++) {
            setW(ch, wx, Y_DECK - 2, wz, Blocks.SANDSTONE);
            setW(ch, wx, Y_DECK - 1, wz, Blocks.SAND);
            setW(ch, wx, Y_DECK, wz, Blocks.SAND);
         }
      }
      setW(ch, 48, Y_DECK + 1, 0, Blocks.CACTUS);
      setW(ch, 48, Y_DECK + 2, 0, Blocks.CACTUS);
      setW(ch, 49, Y_DECK, 1, Blocks.WATER);
      setW(ch, 49, Y_DECK + 1, 0, Blocks.SUGAR_CANE);
      setW(ch, 47, Y_DECK + 1, -1, Blocks.CHEST_LOOT);

      // 3. West Nether Shrine Island around (-48, 64, 0)
      for (int wz = -2; wz <= 2; wz++) {
         for (int wx = -50; wx <= -46; wx++) {
            setW(ch, wx, Y_DECK - 2, wz, Blocks.OBSIDIAN);
            setW(ch, wx, Y_DECK - 1, wz, Blocks.NETHERRACK);
            setW(ch, wx, Y_DECK, wz, (Math.abs(wx + 48) <= 1 && Math.abs(wz) <= 1) ? Blocks.QUARTZ_BLOCK : Blocks.NETHERRACK);
         }
      }
      setW(ch, -48, Y_DECK + 1, -2, Blocks.OBSIDIAN);
      setW(ch, -48, Y_DECK + 2, -2, Blocks.OBSIDIAN);
      setW(ch, -48, Y_DECK + 3, -2, Blocks.GLOWSTONE);
      setW(ch, -47, Y_DECK + 1, 0, Blocks.CHEST_LOOT);

      // 4. North Snowy Taiga & Quarry Island around (0, 64, -48)
      for (int wz = -50; wz <= -46; wz++) {
         for (int wx = -2; wx <= 2; wx++) {
            setW(ch, wx, Y_DECK - 2, wz, (wx == 0 && wz == -48) ? Blocks.DIAMOND_ORE : Blocks.IRON_ORE);
            setW(ch, wx, Y_DECK - 1, wz, Blocks313.ICE);
            setW(ch, wx, Y_DECK, wz, Blocks.SNOW);
         }
      }
      for (int y = 1; y <= 4; y++) {
         setW(ch, -1, Y_DECK + y, -49, Blocks.LOG_SPRUCE);
      }
      for (int lz = -50; lz <= -48; lz++) {
         for (int lx = -2; lx <= 0; lx++) {
            setW(ch, lx, Y_DECK + 3, lz, Blocks.LEAVES_SPRUCE);
            if (lx == -1 || lz == -49) {
               setW(ch, lx, Y_DECK + 4, lz, Blocks.LEAVES_SPRUCE);
            }
         }
      }
      setW(ch, -1, Y_DECK + 5, -49, Blocks.LEAVES_SPRUCE);
      setW(ch, 0, Y_DECK + 1, -47, Blocks.CHEST_LOOT);

      // 5. South Jungle & Mushroom Trader Island around (0, 64, 48)
      for (int wz = 46; wz <= 50; wz++) {
         for (int wx = -2; wx <= 2; wx++) {
            setW(ch, wx, Y_DECK - 2, wz, Blocks.STONEBRICK);
            setW(ch, wx, Y_DECK - 1, wz, Blocks.DIRT);
            setW(ch, wx, Y_DECK, wz, Blocks.GRASS);
         }
      }
      for (int y = 1; y <= 4; y++) {
         setW(ch, 1, Y_DECK + y, 49, Blocks313.LOG_JUNGLE);
      }
      for (int lz = 48; lz <= 50; lz++) {
         for (int lx = 0; lx <= 2; lx++) {
            setW(ch, lx, Y_DECK + 4, lz, Blocks.LEAVES_OAK);
         }
      }
      setW(ch, -1, Y_DECK + 1, 49, Blocks313.MUSHROOM_RED);
      setW(ch, -2, Y_DECK + 1, 48, Blocks313.MUSHROOM_BROWN);
      setW(ch, 2, Y_DECK + 1, 47, Blocks.MELON);
      setW(ch, -2, Y_DECK + 1, 47, Blocks.PUMPKIN);
      setW(ch, 0, Y_DECK + 1, 47, Blocks.CHEST_LOOT);
   }

   private void generateSkywars(Chunk ch) {
      // 1. Central Feast Island (radius 11)
      buildConeIsland(ch, 0, 0, 11.2, 10, Blocks.GRASS, Blocks.DIRT, Blocks.STONE, true);
      for (int dz = -2; dz <= 2; dz++) {
         for (int dx = -2; dx <= 2; dx++) {
            if (Math.abs(dx) == 2 && Math.abs(dz) == 2) {
               for (int y = 1; y <= 3; y++) {
                  setW(ch, dx, Y_DECK + y, dz, Blocks.STONEBRICK);
               }
               setW(ch, dx, Y_DECK + 4, dz, Blocks.GLOWSTONE);
            } else {
               setW(ch, dx, Y_DECK, dz, (dx == 0 && dz == 0) ? Blocks.DIAMOND_BLOCK : Blocks.STONEBRICK);
            }
            if (Math.abs(dx) <= 2 && Math.abs(dz) <= 2) {
               setW(ch, dx, Y_DECK + 4, dz, Blocks.SLAB0 + 3);
            }
         }
      }
      setW(ch, 0, Y_DECK + 1, 0, Blocks.CRAFTING_TABLE);
      setW(ch, -2, Y_DECK + 1, 0, Blocks.CHEST_LOOT);
      setW(ch, 2, Y_DECK + 1, 0, Blocks.CHEST_LOOT);
      setW(ch, 0, Y_DECK + 1, -2, Blocks.CHEST_LOOT);
      setW(ch, 0, Y_DECK + 1, 2, Blocks.CHEST_LOOT);

      // 2. 4 Mid-Ring Islands
      for (int[] m : SKYWARS_MIDS) {
         buildConeIsland(ch, m[0], m[1], 4.5, 6, Blocks.GRASS, Blocks.DIRT, Blocks.STONE, true);
         setW(ch, m[0], Y_DECK + 1, m[1], Blocks.CHEST_LOOT);
      }

      // 3. 8 Player Spawn Islands
      for (int[] sp : SKYWARS_SPAWNS) {
         int sx = sp[0];
         int sz = sp[1];
         buildConeIsland(ch, sx, sz, 4.6, 7, Blocks.GRASS, Blocks.DIRT, Blocks.STONE, true);
         buildSmallOak(ch, sx - 2, Y_DECK + 1, sz - 1);
         setW(ch, sx + 2, Y_DECK + 1, sz, Blocks.CHEST_LOOT);
         setW(ch, sx + 2, Y_DECK + 1, sz + 1, Blocks.CRAFTING_TABLE);
         setW(ch, sx + 1, Y_DECK + 1, sz + 2, Blocks.FURNACE);
      }
   }

   private void generateBedwars(Chunk ch) {
      // 1. Central Emerald Generator Arena (radius 10.5)
      buildConeIsland(ch, 0, 0, 10.5, 9, Blocks.STONEBRICK, Blocks.STONE, Blocks.COBBLE, false);
      for (int dz = -2; dz <= 2; dz++) {
         for (int dx = -2; dx <= 2; dx++) {
            setW(ch, dx, Y_DECK, dz, (dx == 0 && dz == 0) ? Blocks.EMERALD_BLOCK : Blocks.QUARTZ_BLOCK);
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

      // 3. 4 Symmetrical Team Bases (North=Red, South=Blue, West=Green, East=Yellow)
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

   private static boolean putChestItems(World w, int x, int y, int z, Stack... items) {
      if (w.chunkAt(x, z) == null) {
         return false;
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
         return true;
      }
      return false;
   }

   public static void stockSkyblockChests(Game g, World w) {
      // 0: Starter Island Chest at (3, 65, 0)
      if (!sbIslandStocked[0] && putChestItems(
         w, 3, Y_DECK + 1, 0,
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
      )) {
         sbIslandStocked[0] = true;
      }
      // 1: East Desert Island Chest at (47, 65, -1)
      if (!sbIslandStocked[1] && putChestItems(
         w, 47, Y_DECK + 1, -1,
         new Stack(Items.GOLD_INGOT, 8),
         new Stack(Blocks.SAND, 32),
         new Stack(Blocks.CACTUS, 4),
         new Stack(Blocks.SUGAR_CANE, 4),
         new Stack(Items.toolId(2, Items.C_PICK), 1),
         new Stack(Items.APPLE, 4)
      )) {
         sbIslandStocked[1] = true;
      }
      // 2: West Nether Shrine Chest at (-47, 65, 0)
      if (!sbIslandStocked[2] && putChestItems(
         w, -47, Y_DECK + 1, 0,
         new Stack(Blocks.OBSIDIAN, 10),
         new Stack(Items.FLINT_STEEL, 1),
         new Stack(Items.BLAZE_ROD, 4),
         new Stack(Items.QUARTZ, 16),
         new Stack(Items.GHAST_TEAR, 2),
         new Stack(Blocks.GLOWSTONE, 8)
      )) {
         sbIslandStocked[2] = true;
      }
      // 3: North Snowy Taiga Chest at (0, 65, -47)
      if (!sbIslandStocked[3] && putChestItems(
         w, 0, Y_DECK + 1, -47,
         new Stack(Items.DIAMOND, 2),
         new Stack(Blocks.LOG_SPRUCE, 8),
         new Stack(Blocks313.ICE, 8),
         new Stack(Items.BONE, 6),
         new Stack(Items.toolId(2, Items.C_AXE), 1)
      )) {
         sbIslandStocked[3] = true;
      }
      // 4: South Jungle & Trader Chest at (0, 65, 47)
      if (!sbIslandStocked[4] && putChestItems(
         w, 0, Y_DECK + 1, 47,
         new Stack(Items.EMERALD, 8),
         new Stack(Blocks.BOOKSHELF, 4),
         new Stack(Items.SHEARS, 1),
         new Stack(Items.APPLE, 6),
         new Stack(Items.CAKE, 1)
      )) {
         sbIslandStocked[4] = true;
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
      }
   }

   public static void stockSkywarsChests(Game g, World w, boolean refill) {
      if (refill) {
         Arrays.fill(swIslandStocked, false);
      }
      // 1. 8 Outer Spawn Island Chests
      for (int i = 0; i < SKYWARS_SPAWNS.length; i++) {
         if (swIslandStocked[i]) {
            continue;
         }
         int[] sp = SKYWARS_SPAWNS[i];
         if (putChestItems(
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
         )) {
            swIslandStocked[i] = true;
            if (!refill && i > 0 && soloBots && !Net319.isActive() && g != null && g.mobs != null && w.chunkAt(sp[0], sp[1]) != null) {
               int mobType = (i % 2 == 0) ? Mobs.SKELETON : Mobs.ZOMBIE;
               g.mobs.spawn(mobType, sp[0] + 0.5, Y_DECK + 1.0, sp[1] + 0.5);
            }
         }
      }
      // 2. 4 Mid-Ring Island Chests
      for (int j = 0; j < SKYWARS_MIDS.length; j++) {
         int idx = 8 + j;
         if (swIslandStocked[idx]) {
            continue;
         }
         int[] m = SKYWARS_MIDS[j];
         if (putChestItems(
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
         )) {
            swIslandStocked[idx] = true;
         }
      }
      // 3. 4 Center Feast Chests
      if (!swIslandStocked[12] && w.chunk(0, 0) != null) {
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
         swIslandStocked[12] = true;
      }
   }

   public static void stockBedwarsWorld(Game g, World w) {
      // Center Emerald Chest (slot 4)
      if (!bwBaseStocked[4] && putChestItems(
         w, 0, Y_DECK + 1, 0,
         new Stack(Items.EMERALD, 2),
         new Stack(Items.ENDER_PEARL, 1),
         new Stack(Blocks.OBSIDIAN, 4),
         new Stack(Items.STEAK, 4)
      )) {
         bwBaseStocked[4] = true;
      }
      for (int t = 0; t < 4; t++) {
         if (bwBaseStocked[t]) {
            continue;
         }
         int bx = BEDWARS_BASES[t][0];
         int bz = BEDWARS_BASES[t][1];
         if (w.chunkAt(bx, bz) == null) {
            continue;
         }
         int dirX = Integer.compare(0, bx);
         int dirZ = Integer.compare(0, bz);
         int sx = bx + (dirX == 0 ? 3 : 0);
         int sz = bz + (dirZ == 0 ? 3 : 0);
         int ux = bx - (dirX == 0 ? 3 : 0);
         int uz = bz - (dirZ == 0 ? 3 : 0);
         int wool = Blocks.WOOL0 + BEDWARS_COLORS[t];
         if (putChestItems(
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
         )) {
            bwBaseStocked[t] = true;
            if (g != null && g.mobs != null) {
               // Every team base gets both Item Shopkeeper (SMITH) and Upgrades Shopkeeper (PRIEST)!
               Mobs.Mob shop1 = g.mobs.spawn(Mobs.VILLAGER, sx + dirX * 1.5 + 0.5, Y_DECK + 1.0, sz + dirZ * 1.5 + 0.5);
               if (shop1 != null) {
                  shop1.color = Trades.SMITH;
               }
               Mobs.Mob shop2 = g.mobs.spawn(Mobs.VILLAGER, ux + dirX * 1.5 + 0.5, Y_DECK + 1.0, uz + dirZ * 1.5 + 0.5);
               if (shop2 != null) {
                  shop2.color = Trades.PRIEST;
               }
               // Only spawn AI Defender Guards on unoccupied enemy bases when soloBots is enabled and not in Multiplayer
               if (soloBots && !Net319.isActive() && t != getPlayerTeam(g)) {
                  g.mobs.spawn(t % 2 == 1 ? Mobs.ZOMBIE : Mobs.SKELETON, bx + 0.5, Y_DECK + 1.0, bz + 0.5);
               }
            }
         }
      }
   }

   public static boolean isTeamBedIntact(World w, int team) {
      if (w == null || team < 0 || team >= 4 || !bedKnownIntact[team]) {
         return false;
      }
      int[] foot = getTeamBedFoot(team);
      int[] head = getTeamBedHead(team);
      if (w.chunkAt(foot[0], foot[2]) == null || w.chunkAt(head[0], head[2]) == null) {
         return bedKnownIntact[team];
      }
      return Decor315.isBed(w.getBlock(foot[0], foot[1], foot[2])) && Decor315.isBed(w.getBlock(head[0], head[1], head[2]));
   }

   private static void dropGeneratorItemCapped(World w, double x, double y, double z, int id, int count, int maxNearby) {
      int existing = 0;
      for (World.Item it : w.items) {
         if (it != null && !it.dead && it.id == id && Math.abs(it.x - x) < 2.5 && Math.abs(it.z - z) < 2.5) {
            existing += it.count;
            if (existing < maxNearby && it.count + count <= Items.maxStack(id)) {
               it.count += count;
               return;
            }
         }
      }
      if (existing < maxNearby) {
         w.dropItem(x, y, z, id, count);
      }
   }

   /** Runs per-tick waiting lobby countdown and keeps players safely at their spawn island before the match starts. */
   public static void tickWaitingLobby(Game g, World w) {
      if (g == null || w == null || w.settings == null || !w.settings.isMinigame()) {
         return;
      }
      // Keep arena chunks (-3..3, -3..3) warming while players wait in the lobby
      for (int cz = -3; cz <= 3; cz++) {
         for (int cx = -3; cx <= 3; cx++) {
            w.getBlock(cx * 16 + 8, Y_DECK, cz * 16 + 8);
         }
      }
      // Auto-start 10-second countdown when hosting and at least 1 friend has joined
      if (Net319.isHosting() && Net319.peerCount() > 0 && lobbyCountdownTicks < 0) {
         lobbyCountdownTicks = 200; // 10 seconds
         g.say("\u00a7a[Lobby] Player joined! Match starting in 10 seconds...");
      }
      if (lobbyCountdownTicks > 0) {
         lobbyCountdownTicks--;
         if (lobbyCountdownTicks > 0 && lobbyCountdownTicks % 20 == 0 && lobbyCountdownTicks <= 100) {
            g.say("\u00a7e[Lobby] Match starting in " + (lobbyCountdownTicks / 20) + "...");
         }
         if (lobbyCountdownTicks == 0) {
            startMatch(g, Net319.isHosting());
         }
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
      if (w != lastWorldRef) {
         lastWorldRef = w;
         resetMatchState();
      }

      if (g != null && g.screen == Game.S.WAITING) {
         tickWaitingLobby(g, w);
         return;
      }

      // Fast void death so players don't fall endlessly below Y=-18
      if (g != null && g.player != null && !g.player.dead && !g.player.creative && !g.player.spectator && g.player.y < -18.0) {
         g.player.hurt(20);
      }

      // 1. SKYBLOCK: stock island chests & evaluate Skyblock Progression Quests
      if (type == WorldSettings.SKYBLOCK) {
         stockSkyblockChests(g, w);
         if (g != null && g.player != null && w.tickCount % 20L == 0L) {
            checkSkyblockQuests(g);
         }
         return;
      }

      // 2. SKYWARS: stock chests, periodic chest refills, and victory check
      if (type == WorldSettings.SKYWARS) {
         stockSkywarsChests(g, w, false);
         if (w.tickCount > 0L && w.tickCount % 3600L == 0L) {
            stockSkywarsChests(g, w, true);
            if (g != null) {
               g.say("\u00a7b[Skywars] \u00a7eAll island and Feast chests have been REFILLED!");
            }
         }
         if (!skywarsWon && g != null && w.tickCount > 100L && w.tickCount % 40L == 0L) {
            int enemies = countHostiles(g) + Net319.aliveEnemyPeers(-1);
            if (enemies == 0 && swIslandStocked[0] && (soloBots || Net319.peerCount() > 0)) {
               skywarsWon = true;
               g.say("\u00a76\u00a7l[Skywars] VICTORY! \u00a7aYou are the last champion standing! (/sw reset for a new round)");
            }
         }
         return;
      }

      // 3. BEDWARS: stock team bases, resource generators, bed destruction alerts & victory check
      if (type == WorldSettings.BEDWARS) {
         stockBedwarsWorld(g, w);
         // Only the authoritative instance (Singleplayer or Host) spawns generator item drops to prevent network duplication
         if (!Net319.isConnected()) {
            if (w.tickCount % 60L == 0L) {
               for (int t = 0; t < 4; t++) {
                  int bx = BEDWARS_BASES[t][0];
                  int bz = BEDWARS_BASES[t][1];
                  int dirX = Integer.compare(0, bx);
                  int dirZ = Integer.compare(0, bz);
                  double gx = bx - dirX * 4 + 0.5;
                  double gz = bz - dirZ * 4 + 0.5;
                  if (w.chunkAt((int)gx, (int)gz) != null) {
                     dropGeneratorItemCapped(w, gx, Y_DECK + 1.2, gz, Items.IRON_INGOT, 1, 48);
                     dropGeneratorItemCapped(w, gx, Y_DECK + 1.2, gz, Blocks.WOOL0 + BEDWARS_COLORS[t], 4, 64);
                     if (w.tickCount % 180L == 0L) {
                        dropGeneratorItemCapped(w, gx, Y_DECK + 1.2, gz, Items.GOLD_INGOT, 1, 16);
                     }
                  }
               }
            }
            if (w.tickCount % 300L == 0L) {
               for (int[] d : BEDWARS_DIAMONDS) {
                  if (w.chunkAt(d[0], d[1]) != null) {
                     dropGeneratorItemCapped(w, d[0] + 0.5, Y_DECK + 1.2, d[1] + 0.5, Items.DIAMOND, 1, 8);
                  }
               }
            }
            if (w.tickCount % 500L == 0L) {
               if (w.chunkAt(0, 0) != null) {
                  dropGeneratorItemCapped(w, -3.5, Y_DECK + 1.2, 0.5, Items.EMERALD, 1, 8);
                  dropGeneratorItemCapped(w, 4.5, Y_DECK + 1.2, 0.5, Items.EMERALD, 1, 8);
               }
            }
         }

         if (g != null && g.player != null && w.tickCount % 20L == 0L) {
            int my = getPlayerTeam(g);
            int enemyBeds = 0;
            for (int t = 0; t < 4; t++) {
               boolean intact = isTeamBedIntact(w, t);
               if (bedKnownIntact[t] && !intact) {
                  onBedDestroyed(g, w, t, null, false);
               }
               if (t == my) {
                  if (intact) {
                     g.bedPos = getTeamBedFoot(my);
                     g.spawnXZ = new int[]{BEDWARS_BASES[my][0], BEDWARS_BASES[my][1]};
                  } else {
                     g.bedPos = null;
                  }
               } else if (intact) {
                  enemyBeds++;
               }
            }
            int enemyPlayers = Net319.aliveEnemyPeers(my);
            if (!bedwarsWon && bwBaseStocked[my] && enemyBeds == 0 && countHostiles(g) == 0 && enemyPlayers == 0) {
               bedwarsWon = true;
               g.say("\u00a76\u00a7l[Bedwars] VICTORY! \u00a7aAll enemy beds and opponents eliminated! (/bw reset to play again)");
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

   /** Handles /sb, /challenges, /sw, /bw, /shop, /upgrades, and /lobby commands. */
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
      if (c.equals("/lobby") && g.world.settings != null && g.world.settings.isMinigame()) {
         matchState = STATE_WAITING;
         lobbyCountdownTicks = -1;
         g.setScreen(Game.S.WAITING);
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
            resetMatchState();
            startMatch(g, Net319.isHosting());
            g.say("\u00a7b[Skywars] Match reset! Islands restocked.");
         } else if (sub.equals("lobby")) {
            matchState = STATE_WAITING;
            g.setScreen(Game.S.WAITING);
         } else {
            long rem = (3600L - (g.world.tickCount % 3600L)) / 20L;
            g.say("\u00a7b[Skywars] Opponents remaining: " + (countHostiles(g) + Net319.aliveEnemyPeers(-1)) + " | Next chest refill in " + rem + "s (/sw refill | /sw reset)");
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
         if (sub.equals("team") && args.length > 2) {
            String tn = args[2].toLowerCase();
            int t = tn.startsWith("b") || tn.equals("1") ? 1 : (tn.startsWith("g") || tn.equals("2") ? 2 : (tn.startsWith("y") || tn.equals("3") ? 3 : 0));
            selectTeam(g, t, true);
            g.say("\u00a7e[Bedwars] Joined " + BEDWARS_CODES[t] + BEDWARS_NAMES[t] + " Team\u00a7e!");
            return true;
         }
         if (sub.equals("lobby")) {
            matchState = STATE_WAITING;
            g.setScreen(Game.S.WAITING);
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
            startMatch(g, Net319.isHosting());
            g.say("\u00a7e[Bedwars] Match reset! All 4 Team Beds restored.");
            return true;
         }
         StringBuilder sb = new StringBuilder("\u00a7e[Bedwars] Teams: ");
         for (int t = 0; t < 4; t++) {
            sb.append(BEDWARS_CODES[t]).append(BEDWARS_NAMES[t]).append(isTeamBedIntact(g.world, t) ? ":\u00a7aBED " : ":\u00a7cOUT ");
         }
         sb.append("\u00a77(/bw shop | /bw upgrades | /bw team <red|blue|green|yellow> | /bw reset)");
         g.say(sb.toString());
         return true;
      }
      return false;
   }

   /** Renders the top-right Scoreboard / Quest HUD for Skyblock, Skywars, and Bedwars. */
   public static void drawModeHud(Game g) {
      if (g == null || g.world == null || g.world.settings == null || g.debug || g.screen == Game.S.WAITING) {
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
         int raiders = countHostiles(g) + Net319.aliveEnemyPeers(-1);
         lines.add("\u00a76\u00a7lSKYWARS");
         lines.add("\u00a7fOpponents: " + (raiders > 0 ? "\u00a7c" + raiders : "\u00a7a0 (VICTORY!)"));
         lines.add(String.format("\u00a7fChest Refill: \u00a7b%d:%02d", rem / 60L, rem % 60L));
         lines.add("\u00a77/sw refill | /sw reset");
      } else if (type == WorldSettings.BEDWARS) {
         int my = getPlayerTeam(g);
         long dRem = (300L - (g.world.tickCount % 300L)) / 20L;
         long eRem = (500L - (g.world.tickCount % 500L)) / 20L;
         lines.add("\u00a7e\u00a7lBEDWARS");
         for (int t = 0; t < 4; t++) {
            boolean bed = isTeamBedIntact(g.world, t);
            lines.add(BEDWARS_CODES[t] + "[" + BEDWARS_NAMES[t].charAt(0) + "] " + BEDWARS_NAMES[t] + ": " + (bed ? "\u00a7aBED \u2714" : "\u00a7cBROKEN \u2718") + (t == my ? " \u00a77(You)" : ""));
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
