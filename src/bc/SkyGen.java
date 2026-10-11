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
   public static volatile int swKit = 0;
   public static volatile int myKills = 0;
   public static volatile int myDeaths = 0;

   // Battle Item Sets / Kits & Tumble Weapon Modes
   public static final String[] SW_KIT_NAMES = new String[]{"Normal", "High Power", "Archer", "Pyro"};
   public static final String[] SW_KIT_DESC = new String[]{
      "Stone Sword + Chain Chestplate + 16 Planks",
      "Iron Sword + Iron Chestplate + Golden Apple + 16 Bricks",
      "Bow + 20 Arrows + Leather Tunic + 16 Cobble",
      "Iron Axe + 4 Auto-TNT + 2 Fireballs + 16 Bricks"
   };
   public static final String[] TUMBLE_MODE_NAMES = new String[]{"Mixed (Shovels & Snowballs)", "Shovels Only", "Snowballs Only"};
   public static volatile int tumbleWeaponMode = 0;
   public static volatile int battleGraceTicks = 0;
   public static volatile boolean showdownActive = false;
   private static boolean tumbleSpawnedBots = false;
   private static boolean tumbleWon = false;

   // Bedwars Team Upgrades bitmask per team (0..3)
   public static final int UPG_SHARPNESS = 1;
   public static final int UPG_PROTECTION = 2;
   public static final int UPG_FORGE = 4;
   public static final int UPG_HEAL_POOL = 8;
   public static final int[] teamUpgrades = new int[4];
   private static final int[] trapCooldown = new int[4];

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
   private final int mapId;

   public SkyGen(long seed, WorldSettings st) {
      super(seed);
      this.settings = st;
      this.mode = st.type;
      this.mapId = st != null ? Math.max(0, Math.min(WorldSettings.MAP_COUNT - 1, st.mapId)) : 0;
   }

   public static void resetMatchState() {
      Arrays.fill(sbQuestDone, false);
      Arrays.fill(sbIslandStocked, false);
      Arrays.fill(swIslandStocked, false);
      skywarsWon = false;
      tumbleSpawnedBots = false;
      tumbleWon = false;
      battleGraceTicks = 300; // 15-second Console Edition Battle invulnerability timer
      showdownActive = false;
      Arrays.fill(bwBaseStocked, false);
      bedwarsWon = false;
      Arrays.fill(bedKnownIntact, true);
      Arrays.fill(teamUpgrades, 0);
      Arrays.fill(trapCooldown, 0);
      myKills = 0;
      myDeaths = 0;
      lobbyCountdownTicks = -1;
   }

   public static boolean hasTeamSharpness(int team) {
      return (teamUpgrades[Math.abs(team) & 3] & UPG_SHARPNESS) != 0;
   }

   public static boolean hasTeamProtection(int team) {
      return (teamUpgrades[Math.abs(team) & 3] & UPG_PROTECTION) != 0;
   }

   public static boolean hasTeamForge(int team) {
      return (teamUpgrades[Math.abs(team) & 3] & UPG_FORGE) != 0;
   }

   public static boolean hasTeamHealPool(int team) {
      return (teamUpgrades[Math.abs(team) & 3] & UPG_HEAL_POOL) != 0;
   }

   public static void setTeamUpgradeMask(Game g, int team, int mask, String msg) {
      int t = Math.abs(team) & 3;
      teamUpgrades[t] = mask;
      if (g != null && msg != null && !msg.isEmpty()) {
         g.say(msg);
      }
   }

   public static boolean unlockNextTeamUpgrade(Game g, int team, boolean broadcast) {
      int t = Math.abs(team) & 3;
      int cur = teamUpgrades[t];
      int nextBit;
      String upgName;
      if ((cur & UPG_SHARPNESS) == 0) {
         nextBit = UPG_SHARPNESS;
         upgName = "Sharpened Swords (+2 Melee Damage)";
      } else if ((cur & UPG_PROTECTION) == 0) {
         nextBit = UPG_PROTECTION;
         upgName = "Reinforced Armor (+20% Team Defense)";
      } else if ((cur & UPG_FORGE) == 0) {
         nextBit = UPG_FORGE;
         upgName = "Emerald Forge (2x Base Generator Speed)";
      } else if ((cur & UPG_HEAL_POOL) == 0) {
         nextBit = UPG_HEAL_POOL;
         upgName = "Heal Pool & Bed Alarm Trap";
      } else {
         if (g != null) {
            g.say("\u00a76[Bedwars] Your team already has ALL Team Upgrades unlocked!");
         }
         return false;
      }
      int newMask = cur | nextBit;
      String msg = "\u00a7b\u00a7l[Team Upgrade] " + BEDWARS_CODES[t] + BEDWARS_NAMES[t] + " Team \u00a7eunlocked \u00a7a" + upgName + "\u00a7e!";
      setTeamUpgradeMask(g, t, newMask, msg);
      if (broadcast && Net319.isActive()) {
         Net319.sendTeamUpgrade(t, newMask, upgName);
      }
      return true;
   }

   public static void giveSkywarsKit(Game g, int kit) {
      if (g == null) {
         return;
      }
      int k = Math.abs(kit) & 3;
      swKit = k;
      Arrays.fill(g.inv, null);
      Arrays.fill(g.armor, null);
      switch (k) {
         case 0 -> { // Normal
            g.inv[0] = new Stack(Items.toolId(1, Items.C_SWORD), 1);
            g.inv[1] = new Stack(Blocks.PLANKS, 16);
            g.inv[2] = new Stack(Items.STEAK, 4);
            g.armor[1] = new Stack(Items.armorId(1, 1), 1);
         }
         case 1 -> { // High Power
            g.inv[0] = new Stack(Items.toolId(2, Items.C_SWORD), 1);
            g.inv[1] = new Stack(Redstone319.GOLDEN_APPLE, 2);
            g.inv[2] = new Stack(Blocks.STONEBRICK, 16);
            g.inv[3] = new Stack(Items.STEAK, 6);
            g.armor[1] = new Stack(Items.armorId(2, 1), 1);
            g.armor[3] = new Stack(Items.armorId(2, 3), 1);
         }
         case 2 -> { // Archer
            g.inv[0] = new Stack(Items.BOW, 1);
            g.inv[1] = new Stack(Items.ARROW, 20);
            g.inv[2] = new Stack(Blocks.COBBLE, 16);
            g.inv[3] = new Stack(Items.STEAK, 4);
            g.armor[1] = new Stack(Items.armorId(0, 1), 1);
         }
         default -> { // Pyro
            g.inv[0] = new Stack(Items.toolId(2, Items.C_AXE), 1);
            g.inv[1] = new Stack(Blocks.TNT, 4);
            g.inv[2] = new Stack(Items.BLAZE_ROD, 2);
            g.inv[3] = new Stack(Blocks.STONEBRICK, 16);
            g.armor[0] = new Stack(Items.armorId(2, 0), 1);
         }
      }
      g.sel = 0;
   }

   /** Gives the Console Edition Tumble loadout (Shovels, Snowballs, or Mixed). */
   public static void giveTumbleLoadout(Game g) {
      if (g == null) {
         return;
      }
      Arrays.fill(g.inv, null);
      Arrays.fill(g.armor, null);
      int wm = Math.abs(tumbleWeaponMode) % 3;
      if (wm == 0) { // Mixed: Diamond Tumble Shovel + Tumble Snowballs
         g.inv[0] = new Stack(Items.toolId(4, Items.C_SHOVEL), 1);
         g.inv[1] = new Stack(Blocks.SNOW, 16);
      } else if (wm == 1) { // Shovels Only
         g.inv[0] = new Stack(Items.toolId(4, Items.C_SHOVEL), 1);
      } else { // Snowballs Only
         g.inv[0] = new Stack(Blocks.SNOW, 32);
      }
      g.inv[2] = new Stack(Items.STEAK, 4);
      g.armor[3] = new Stack(Items.armorId(2, 3), 1); // Iron Boots
      g.sel = 0;
   }

   /** Returns true if (x, y, z) is a destructible block in the 3-layer Tumble arena. */
   public static boolean isTumbleLayerBlock(int x, int y, int z) {
      return Math.hypot(x, z) <= 19.2 && (y == Y_DECK || y == Y_DECK - 5 || y == Y_DECK - 10);
   }

   /** Handles right-clicking in Tumble mode to throw a Spleef Snowball that breaks the targeted layer block on impact! */
   public static boolean useTumbleItem(Game g, Stack held) {
      if (g == null || g.world == null || g.player == null || held == null) {
         return false;
      }
      int id = held.id;
      boolean isShovel = Items.isTool(id) && Items.toolClass(id) == Items.C_SHOVEL;
      boolean isSnow = id == Blocks.SNOW || id == Items.ENDER_PEARL;
      if (!isShovel && !isSnow) {
         return false;
      }
      if (isShovel && (Math.abs(tumbleWeaponMode) % 3) == 1) {
         return false; // Shovels Only mode: left-click breaks blocks, no snowball throw
      }
      Player p = g.player;
      SoundEngine.playThrow(g.world, p.x, p.y + p.eye, p.z);
      double yaw = Math.toRadians(p.yaw);
      double pitch = Math.toRadians(p.pitch);
      double dx = -Math.sin(yaw) * Math.cos(pitch);
      double dy = -Math.sin(pitch);
      double dz = Math.cos(yaw) * Math.cos(pitch);
      double rx = p.x;
      double ry = p.y + p.eye - 0.1;
      double rz = p.z;
      for (int step = 1; step <= 52; step++) {
         rx += dx * 0.5;
         ry += dy * 0.5;
         rz += dz * 0.5;
         if ((step & 1) == 0) {
            g.spawnBreakParticles((int)Math.floor(rx), (int)Math.floor(ry), (int)Math.floor(rz), Blocks.SNOW, 1);
         }
         int bx = (int)Math.floor(rx);
         int by = (int)Math.floor(ry);
         int bz = (int)Math.floor(rz);
         int b = g.world.getBlock(bx, by, bz);
         if (b != 0 && !Blocks.isFluid(b)) {
            if (isTumbleLayerBlock(bx, by, bz) && !(bx == 0 && by == Y_DECK && bz == 0 && g.world.tickCount < 40L)) {
               g.spawnBreakParticles(bx, by, bz, b, 10);
               SoundEngine.playBlockBreak(g.world, b, bx, by, bz);
               g.world.setBlock(bx, by, bz, 0);
            }
            break;
         }
      }
      if (isSnow && !p.creative) {
         held.n--;
         if (held.n <= 0) {
            g.inv[g.sel] = new Stack(Blocks.SNOW, 8); // Auto-replenish Tumble snowballs!
         }
      }
      return true;
   }

   public static void onArenaKill(Game g, String victimName) {
      myKills++;
      if (g == null || g.world == null || g.world.settings == null || g.player == null) {
         return;
      }
      int type = g.world.settings.type;
      String who = (victimName == null || victimName.isEmpty()) ? "an opponent" : victimName;
      if (type == WorldSettings.SKYWARS) {
         g.player.health = Math.min(20, g.player.health + 6);
         g.say("\u00a7d\u00a7l[Skywars Perk] \u00a7aBulldozer! +6 HP for eliminating \u00a7f" + who + "\u00a7a! (Kills: " + myKills + ")");
      } else if (type == WorldSettings.BEDWARS) {
         g.addItem(new Stack(Items.IRON_INGOT, 6));
         g.addItem(new Stack(Items.GOLD_INGOT, 2));
         g.say("\u00a7e\u00a7l[Bedwars Bounty] \u00a7aEliminated \u00a7f" + who + "\u00a7a! (+6 Iron, +2 Gold | Kills: " + myKills + ")");
      }
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
      for (int i = 0; i < 4; i++) {
         if (g.armor[i] != null) {
            g.armor[i].dmg = 0;
         }
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
         } else if (g.world.settings.type == WorldSettings.BATTLE) {
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
            giveSkywarsKit(g, swKit);
         } else if (g.world.settings.type == WorldSettings.TUMBLE) {
            int idx = Math.abs(team) % 8;
            double ang = idx * (Math.PI / 4.0);
            int sx = (int)Math.round(Math.cos(ang) * 12.0);
            int sz = (int)Math.round(Math.sin(ang) * 12.0);
            g.spawnXZ = new int[]{sx, sz};
            g.bedPos = null;
            if (g.player != null) {
               g.player.setMode(0);
               g.player.setPos(sx + 0.5, Y_DECK + 1.0, sz + 0.5);
               g.player.yaw = (float)Math.toDegrees(Math.atan2(sx, -sz));
               g.player.pitch = 12.0F;
               g.player.updateBox();
            }
            giveTumbleLoadout(g);
         }
      }
      if (broadcast) {
         Net319.sendTeamSelect(t);
      }
   }

   public static void resetArenaMap(Game g, World w, boolean broadcast) {
      resetMatchState();
      Net319.clearWorldState();
      World targetW = w != null ? w : (g != null ? g.world : null);
      if (targetW != null) {
         if (targetW.gen instanceof SkyGen && targetW.settings != null && ((SkyGen)targetW.gen).mapId != targetW.settings.mapId) {
            targetW.gen = new SkyGen(targetW.seed, targetW.settings);
         }
         targetW.resetAllChunks();
      }
      if (g != null) {
         if (g.mobs != null) {
            g.mobs.clear();
         }
         g.particles.clear();
         if (targetW != null && g.rend != null) {
            g.rend.setLod(targetW);
         }
         selectTeam(g, myTeam, false);
         if (g.player != null) {
            g.player.dead = false;
            g.player.burn = 0;
            g.player.health = 20;
            g.player.food = 20;
            g.player.sat = 5.0F;
            g.player.exh = 0.0F;
            g.player.air = 300;
            g.player.fallDistance = 0.0F;
            g.player.invuln = 40;
         }
         if (targetW != null && targetW.settings != null) {
            if (targetW.settings.type == WorldSettings.BEDWARS) {
               stockBedwarsWorld(g, targetW);
            } else if (targetW.settings.type == WorldSettings.BATTLE) {
               stockSkywarsChests(g, targetW, true);
            } else if (targetW.settings.type == WorldSettings.TUMBLE) {
               stockTumbleArena(g, targetW);
            }
         }
      }
      if (broadcast && Net319.isActive()) {
         Net319.sendMatchControl("RESET");
      }
   }

   public static void startMatch(Game g, boolean broadcast) {
      resetArenaMap(g, g != null ? g.world : null, false);
      matchState = STATE_PLAYING;
      lobbyCountdownTicks = -1;
      SoundEngine.playMinigameSound("mg_count_go");
      if (g != null && g.world != null && g.world.settings != null) {
         if (g.screen == Game.S.WAITING || g.screen == Game.S.PAUSE || g.screen == Game.S.DEAD) {
            g.setScreen(Game.S.PLAY);
         }
         String mapName = g.world.settings.mapName();
         String modeTitle = switch (g.world.settings.type) {
            case WorldSettings.BEDWARS ->
               "\u00a7e\u00a7l[Bedwars - " + mapName + "] \u00a7aMatch Started! You are on " + BEDWARS_CODES[myTeam] + BEDWARS_NAMES[myTeam] + " Team\u00a7a! Protect your bed!";
            case WorldSettings.TUMBLE ->
               "\u00a7b\u00a7l[Tumble - " + mapName + "] \u00a7aThe game has started! Use your Shovel (Left-Click) & Snowballs (Right-Click) to drop rivals into the lava!";
            default ->
               "\u00a76\u00a7l[Battle - " + mapName + "] \u00a7aThe game has started! 15s Invulnerability active — loot the central chests!";
         };
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
      if (this.mode == WorldSettings.TUMBLE) {
         if (Math.hypot(x, z) <= 22.5) {
            return Y_DECK;
         }
         return 0;
      }
      if (this.mode == WorldSettings.BATTLE) {
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
         // Bridges connecting spawn podiums, mid islands & center
         if ((Math.abs(x) <= 2 && Math.abs(z) <= 42) || (Math.abs(z) <= 2 && Math.abs(x) <= 42)
            || (Math.abs(Math.abs(x) - Math.abs(z)) <= 2 && Math.abs(x) <= 30)) {
            return Y_DECK;
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
      // Console Edition TUMBLE Mini Game: 3 destructible layers suspended over a boiling Lava Cauldron inside a themed Colosseum!
      int minX = ch.cx * 16;
      int minZ = ch.cz * 16;
      if (minX > 32 || minX + 15 < -32 || minZ > 32 || minZ + 15 < -32) {
         return;
      }
      int wallMat = switch (this.mapId) {
         case WorldSettings.MAP_CRUCIBLE -> Blocks.NETHER_BRICKS;
         case WorldSettings.MAP_CAVERN -> Blocks.MOSSY_COBBLE;
         case WorldSettings.MAP_TEMPLE -> Blocks.SANDSTONE;
         default -> Blocks.STONEBRICK;
      };
      int trimMat = switch (this.mapId) {
         case WorldSettings.MAP_CRUCIBLE -> Blocks.OBSIDIAN;
         case WorldSettings.MAP_CAVERN -> Blocks.GLOWSTONE;
         case WorldSettings.MAP_TEMPLE -> Blocks.GOLD_BLOCK;
         default -> Blocks.QUARTZ_BLOCK;
      };

      for (int lz = 0; lz < 16; lz++) {
         int wz = minZ + lz;
         for (int lx = 0; lx < 16; lx++) {
            int wx = minX + lx;
            double d = Math.hypot(wx, wz);
            int ring = ((int)Math.floor(d / 3.2)) & 3;
            int checker = (wx + wz) & 1;

            // 1. Boiling Lava Cauldron at Y_DECK - 14 over Bedrock floor at Y_DECK - 15
            if (d <= 22.2) {
               setW(ch, wx, Y_DECK - 15, wz, Blocks.BEDROCK);
               if (d <= 20.8) {
                  setW(ch, wx, Y_DECK - 14, wz, Blocks.LAVA);
                  setW(ch, wx, Y_DECK - 13, wz, Blocks.LAVA);
               } else {
                  for (int y = Y_DECK - 14; y <= Y_DECK + 5; y++) {
                     setW(ch, wx, y, wz, (y == Y_DECK + 1 || y == Y_DECK - 5 || y == Y_DECK - 10) ? trimMat : wallMat);
                  }
                  if (((wx * 7 + wz * 13) & 7) == 0) {
                     setW(ch, wx, Y_DECK + 6, wz, Blocks.GLOWSTONE);
                  }
               }
            }

            // 2. Three Destructible Tumble Layers: Layer 1 (Y_DECK), Layer 2 (Y_DECK - 5), Layer 3 (Y_DECK - 10)
            if (d <= 18.5) {
               int b1;
               int b2;
               int b3;
               switch (this.mapId) {
                  case WorldSettings.MAP_CRUCIBLE -> {
                     b1 = ring == 0 ? Blocks.NETHER_BRICKS : (ring == 1 ? Blocks.NETHERRACK : (checker == 0 ? Blocks.QUARTZ_ORE : Blocks.NETHERRACK));
                     b2 = ring == 0 ? Blocks.GLOWSTONE : (ring == 1 ? Blocks.SOUL_SAND : Blocks.COAL_BLOCK);
                     b3 = ring == 0 ? Blocks.OBSIDIAN : (checker == 0 ? Redstone319.REDSTONE_BLOCK : (Blocks.GLASS0 + 14));
                  }
                  case WorldSettings.MAP_CAVERN -> {
                     b1 = ring == 0 ? Blocks313.MYCELIUM : (ring == 1 ? Blocks313.MUSH_BLOCK_RED : (ring == 2 ? Blocks313.MUSH_BLOCK_BROWN : Blocks.MOSSY_COBBLE));
                     b2 = ring == 0 ? Blocks.DIAMOND_ORE : (ring == 1 ? Blocks.GOLD_ORE : (ring == 2 ? Blocks.IRON_ORE : Blocks.COAL_ORE));
                     b3 = ring == 0 ? Blocks313.PACKED_ICE : (checker == 0 ? Blocks313.ICE : (Blocks.GLASS0 + 11));
                  }
                  case WorldSettings.MAP_TEMPLE -> {
                     b1 = ring == 0 ? Blocks.SNOW : (ring == 1 ? (Blocks.WOOL0 + 14) : (ring == 2 ? Blocks.WOOL0 : (Blocks.WOOL0 + 5)));
                     b2 = ring == 0 ? Redstone319.SLIME_BLOCK : (ring == 1 ? Blocks.HAY_BALE : Blocks.SANDSTONE);
                     b3 = ring == 0 ? Blocks.GOLD_BLOCK : (checker == 0 ? Redstone319.SEA_LANTERN : Blocks.GLASS);
                  }
                  default -> { // Cove / Classic Tumble
                     b1 = (wx == 0 && wz == 0) ? Blocks.GRASS : (ring == 0 ? Blocks.GRASS : (ring == 1 ? Blocks313.COARSE_DIRT : (ring == 2 ? Blocks313.PODZOL : Blocks.PLANKS)));
                     b2 = ring == 0 ? Redstone319.SEA_LANTERN : (ring == 1 ? Blocks.SANDSTONE : (ring == 2 ? Blocks313.RED_SANDSTONE : Blocks.CLAY));
                     b3 = ring == 0 ? Blocks.DIAMOND_ORE : (ring == 1 ? Blocks.GOLD_ORE : (ring == 2 ? Blocks.IRON_ORE : Blocks.COBBLE));
                  }
               }
               setW(ch, wx, Y_DECK, wz, b1);
               setW(ch, wx, Y_DECK - 5, wz, b2);
               setW(ch, wx, Y_DECK - 10, wz, b3);
            }
         }
      }
      // Keep central anchor & supply chest at (0, Y_DECK-2, 0) and (3, Y_DECK+1, 0)
      setW(ch, 0, Y_DECK, 0, Blocks.GRASS);
      setW(ch, 0, Y_DECK - 2, 0, Blocks.BEDROCK);
      setW(ch, 3, Y_DECK + 1, 0, Blocks.CHEST_LOOT);
   }

   private void generateSkywars(Chunk ch) {
      // Console Edition BATTLE Mini Game Arena with 4 distinct Maps: Cove (0), Crucible (1), Cavern (2), Temple (3)!
      int topId = switch (this.mapId) {
         case WorldSettings.MAP_CRUCIBLE -> Blocks.NETHERRACK;
         case WorldSettings.MAP_CAVERN -> Blocks.MOSSY_COBBLE;
         case WorldSettings.MAP_TEMPLE -> Blocks.GRASS;
         default -> Blocks.GRASS;
      };
      int midId = switch (this.mapId) {
         case WorldSettings.MAP_CRUCIBLE -> Blocks.NETHER_BRICKS;
         case WorldSettings.MAP_CAVERN -> Blocks.STONE;
         case WorldSettings.MAP_TEMPLE -> Blocks313.COARSE_DIRT;
         default -> Blocks.SAND;
      };
      int coreId = switch (this.mapId) {
         case WorldSettings.MAP_CRUCIBLE -> Blocks.OBSIDIAN;
         case WorldSettings.MAP_CAVERN -> Blocks.STONEBRICK;
         case WorldSettings.MAP_TEMPLE -> Blocks.MOSSY_COBBLE;
         default -> Blocks.STONE;
      };
      int bridgeId = switch (this.mapId) {
         case WorldSettings.MAP_CRUCIBLE -> Blocks.NETHER_BRICKS;
         case WorldSettings.MAP_CAVERN -> Blocks.STONEBRICK;
         case WorldSettings.MAP_TEMPLE -> Blocks.MOSSY_COBBLE;
         default -> Blocks.PLANKS;
      };

      // 1. Central Battle Courtyard / Shrine (radius 11.2)
      buildConeIsland(ch, 0, 0, 11.2, 10, topId, midId, coreId, true);
      for (int dz = -2; dz <= 2; dz++) {
         for (int dx = -2; dx <= 2; dx++) {
            if (Math.abs(dx) == 2 && Math.abs(dz) == 2) {
               for (int y = 1; y <= 3; y++) {
                  setW(ch, dx, Y_DECK + y, dz, bridgeId);
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

      // 2. Connected Console Edition Arena Walkways & Map Landmarks
      int minX = ch.cx * 16;
      int minZ = ch.cz * 16;
      for (int lz = 0; lz < 16; lz++) {
         int wz = minZ + lz;
         for (int lx = 0; lx < 16; lx++) {
            int wx = minX + lx;
            double d = Math.hypot(wx, wz);
            // Cardinal and diagonal walkways connecting the 8 spawn podiums to the mid-ring and central chests
            boolean cardinalBridge = (Math.abs(wx) <= 1 && Math.abs(wz) >= 10 && Math.abs(wz) <= 38)
               || (Math.abs(wz) <= 1 && Math.abs(wx) >= 10 && Math.abs(wx) <= 38);
            boolean diagBridge = Math.abs(Math.abs(wx) - Math.abs(wz)) <= 1 && Math.abs(wx) >= 8 && Math.abs(wx) <= 27;
            boolean ringWalkway = d >= 22.5 && d <= 25.2;
            if (cardinalBridge || diagBridge || ringWalkway) {
               setW(ch, wx, Y_DECK - 1, wz, bridgeId);
               if (this.mapId == WorldSettings.MAP_CRUCIBLE && ((wx + wz) & 7) == 0) {
                  setW(ch, wx, Y_DECK - 1, wz, Blocks.OBSIDIAN);
               }
            }
            // Map-specific environmental hazards & landmarks
            if (this.mapId == WorldSettings.MAP_CRUCIBLE && d >= 12.0 && d <= 21.5 && !cardinalBridge && !diagBridge) {
               setW(ch, wx, Y_DECK - 5, wz, Blocks.NETHER_BRICKS);
               setW(ch, wx, Y_DECK - 4, wz, Blocks.LAVA);
            } else if (this.mapId == WorldSettings.MAP_CAVERN && d <= 36.0 && ((wx * 19 + wz * 31) & 31) == 0) {
               setW(ch, wx, Y_DECK + 11, wz, Blocks.STONE);
               setW(ch, wx, Y_DECK + 10, wz, Blocks.COBBLE);
               setW(ch, wx, Y_DECK + 9, wz, Blocks.GLOWSTONE);
            }
         }
      }

      // 3. 4 Mid-Ring Landmarks & Chests
      for (int[] m : SKYWARS_MIDS) {
         buildConeIsland(ch, m[0], m[1], 4.5, 6, topId, midId, coreId, true);
         setW(ch, m[0], Y_DECK + 1, m[1], Blocks.CHEST_LOOT);
         if (this.mapId == WorldSettings.MAP_COVE) {
            // Pirate Shipwreck Mast & Sail on Mid Islands
            for (int y = 1; y <= 5; y++) {
               setW(ch, m[0] + 1, Y_DECK + y, m[1] + 1, Blocks.LOG_OAK);
            }
            for (int sx = -1; sx <= 1; sx++) {
               setW(ch, m[0] + 1 + sx, Y_DECK + 4, m[1] + 1, Blocks.WOOL0);
               setW(ch, m[0] + 1 + sx, Y_DECK + 3, m[1] + 1, Blocks.WOOL0 + 14);
            }
         } else if (this.mapId == WorldSettings.MAP_CRUCIBLE) {
            // Nether Blaze Brazier Towers
            setW(ch, m[0] + 1, Y_DECK + 1, m[1] + 1, Blocks.NETHER_BRICKS);
            setW(ch, m[0] + 1, Y_DECK + 2, m[1] + 1, Blocks.GLOWSTONE);
         } else if (this.mapId == WorldSettings.MAP_CAVERN) {
            // Giant Glowing Cavern Mushrooms
            for (int y = 1; y <= 3; y++) {
               setW(ch, m[0] + 1, Y_DECK + y, m[1] + 1, Blocks313.MUSH_STEM);
            }
            for (int dx = -1; dx <= 1; dx++) {
               for (int dz = -1; dz <= 1; dz++) {
                  setW(ch, m[0] + 1 + dx, Y_DECK + 4, m[1] + 1 + dz, Blocks313.MUSH_BLOCK_RED);
               }
            }
         } else if (this.mapId == WorldSettings.MAP_TEMPLE) {
            // Stepped Jungle Temple Pillars with Vines
            for (int y = 1; y <= 4; y++) {
               setW(ch, m[0] + 1, Y_DECK + y, m[1] + 1, Blocks.MOSSY_COBBLE);
            }
            setW(ch, m[0] + 1, Y_DECK + 5, m[1] + 1, Blocks.GOLD_BLOCK);
         }
      }

      // 4. 8 Player Spawn Podiums
      for (int[] sp : SKYWARS_SPAWNS) {
         int sx = sp[0];
         int sz = sp[1];
         buildConeIsland(ch, sx, sz, 4.6, 7, topId, midId, coreId, true);
         if (this.mapId == WorldSettings.MAP_COVE || this.mapId == WorldSettings.MAP_TEMPLE) {
            buildSmallOak(ch, sx - 2, Y_DECK + 1, sz - 1);
         }
         setW(ch, sx, Y_DECK, sz, Blocks.GOLD_BLOCK);
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
         boolean wasPassive = w.inPassiveTick;
         w.inPassiveTick = true;
         try {
            w.setBlock(x, y, z, Blocks.CHEST);
         } finally {
            w.inPassiveTick = wasPassive;
         }
         TileEntity te = w.getOrCreateTile(x, y, z);
         if (te != null && te.slots != null) {
            for (int i = 0; i < te.slots.length; i++) {
               te.slots[i] = i < items.length ? items[i] : null;
            }
            Net319.applyPendingTile(w, te);
            if (!Net319.isClientConnected() || Net319.isHosting()) {
               Net319.onTileChanged(w, te);
            }
         }
         return true;
      }
      return false;
   }

   public static void stockTumbleArena(Game g, World w) {
      if (w == null) {
         return;
      }
      // Stock supply chest at (3, Y_DECK + 1, 0) with Tumble Shovels & Snowballs
      if (!sbIslandStocked[0] && putChestItems(
         w, 3, Y_DECK + 1, 0,
         new Stack(Items.toolId(4, Items.C_SHOVEL), 1),
         new Stack(Blocks.SNOW, 32),
         new Stack(Blocks313.ICE, 2),
         new Stack(Items.LAVA_BUCKET, 1),
         new Stack(Items.WATER_BUCKET, 1),
         new Stack(Items.STEAK, 8)
      )) {
         sbIslandStocked[0] = true;
      }
      // Spawn Tumble Competitor Bots around the ring in solo mode
      if (!tumbleSpawnedBots && soloBots && !Net319.isActive() && g != null && g.mobs != null && w.chunk(0, 0) != null) {
         tumbleSpawnedBots = true;
         for (int i = 1; i < 6; i++) {
            double ang = i * (2.0 * Math.PI / 6.0);
            double bx = Math.cos(ang) * 11.0 + 0.5;
            double bz = Math.sin(ang) * 11.0 + 0.5;
            Mobs.Mob bot = g.mobs.spawn(i % 2 == 0 ? Mobs.SKELETON : Mobs.ZOMBIE, bx, Y_DECK + 1.0, bz);
            if (bot != null) {
               bot.teamTag = "TumbleBot:" + i;
            }
         }
      }
   }

   public static void stockSkyblockChests(Game g, World w) {
      stockTumbleArena(g, w);
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
                  Mobs.Mob guard = g.mobs.spawn(t % 2 == 1 ? Mobs.ZOMBIE : Mobs.SKELETON, bx + 0.5, Y_DECK + 1.0, bz + 0.5);
                  if (guard != null) {
                     guard.teamTag = "Team:" + BEDWARS_NAMES[t];
                  }
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
            int sec = lobbyCountdownTicks / 20;
            g.say("\u00a7e[Lobby] Match starting in " + sec + "...");
            if (sec == 3) {
               SoundEngine.playMinigameSound("mg_count_3");
            } else if (sec == 2) {
               SoundEngine.playMinigameSound("mg_count_2");
            } else if (sec == 1) {
               SoundEngine.playMinigameSound("mg_count_1");
            }
         }
         if (lobbyCountdownTicks == 0) {
            startMatch(g, Net319.isHosting());
         }
      }
   }

   /** Runs per-tick gameplay logic for Console Edition Tumble, Battle, and Bedwars worlds. */
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

      // 1. TUMBLE: 3-layer shovel & snowball Spleef over boiling lava!
      if (type == WorldSettings.TUMBLE) {
         stockTumbleArena(g, w);
         if (g != null && g.player != null && !g.player.dead && !g.player.creative && !g.player.spectator) {
            // Instant lava cauldron elimination at the bottom of the Tumble arena
            if (g.player.y <= Y_DECK - 12.5) {
               SoundEngine.playUi("lava_fizz", 0.9F, 1.0F);
               SoundEngine.playMinigameSound("mg_lose");
               g.player.hurt(20);
            }
         }
         // Competitor bots actively mine Tumble blocks around the layers & get eliminated in lava
         if (g != null && g.mobs != null && !Net319.isConnected()) {
            int botIdx = 0;
            for (Mobs.Mob m : g.mobs.list) {
               botIdx++;
               if (m == null || m.dead || !Mobs.hostile(m.type)) {
                  continue;
               }
               if (m.y <= Y_DECK - 12.2) {
                  m.dead = true;
                  myKills++;
                  g.say("\u00a7b[Tumble] \u00a7eAn opponent tumbled into the lava!");
                  continue;
               }
               if (w.tickCount % 18L == 0L) {
                  int dir = (int)((w.tickCount / 18L + botIdx) & 3);
                  int ox = dir == 0 ? 1 : (dir == 1 ? -1 : 0);
                  int oz = dir == 2 ? 1 : (dir == 3 ? -1 : 0);
                  int bx = (int)Math.floor(m.x) + ox;
                  int by = (int)Math.floor(m.y) - 1;
                  int bz = (int)Math.floor(m.z) + oz;
                  if (isTumbleLayerBlock(bx, by, bz) && !(bx == 0 && by == Y_DECK && bz == 0)) {
                     int bid = w.getBlock(bx, by, bz);
                     if (bid != 0 && !Blocks.isFluid(bid)) {
                        g.spawnBreakParticles(bx, by, bz, bid, 6);
                        SoundEngine.playBlockBreak(w, bid, bx, by, bz);
                        w.setBlock(bx, by, bz, 0);
                     }
                  }
               }
            }
         }
         if (!tumbleWon && g != null && w.tickCount > 80L && w.tickCount % 30L == 0L) {
            int enemies = countHostiles(g) + Net319.aliveEnemyPeers(-1);
            if (enemies == 0 && (tumbleSpawnedBots || Net319.peerCount() > 0)) {
               tumbleWon = true;
               SoundEngine.playMinigameSound("mg_win");
               g.say("\u00a7b\u00a7l[Tumble] VICTORY! \u00a7aYou outlasted every rival above the lava! (/tumble reset for a new round)");
            }
         }
         return;
      }

      // 2. BATTLE: Console Edition grace period, chest refills, Showdown mode, and last-man-standing victory!
      if (type == WorldSettings.BATTLE) {
         stockSkywarsChests(g, w, false);
         if (battleGraceTicks > 0) {
            battleGraceTicks--;
            if (g != null && g.player != null && !g.player.dead) {
               g.player.invuln = Math.max(g.player.invuln, 10);
            }
            if (g != null && (battleGraceTicks == 200 || battleGraceTicks == 100)) {
               g.say("\u00a7e[Battle] Invulnerability wears off in " + (battleGraceTicks / 20) + " seconds!");
            } else if (g != null && battleGraceTicks > 0 && battleGraceTicks <= 60 && battleGraceTicks % 20 == 0) {
               int sec = battleGraceTicks / 20;
               SoundEngine.playMinigameSound("mg_count_" + sec);
            } else if (g != null && battleGraceTicks == 0) {
               SoundEngine.playMinigameSound("mg_count_go");
               g.say("\u00a7c\u00a7l[Battle] Invulnerability has worn off! FIGHT!");
            }
         }
         if (w.tickCount > 0L && w.tickCount % 2400L == 0L) {
            stockSkywarsChests(g, w, true);
            SoundEngine.playMinigameSound("mg_refill");
            if (g != null) {
               g.say("\u00a7b\u00a7l[Battle] \u00a7eAll arena chests have been REFILLED!");
            }
         }
         if (!showdownActive && w.tickCount >= 3600L) {
            showdownActive = true;
            SoundEngine.playMinigameSound("mg_showdown");
            if (g != null) {
               g.say("\u00a7c\u00a7l[Battle] SHOWDOWN! \u00a7eHead to the center courtyard — outer perimeter is now hazardous!");
            }
         }
         if (showdownActive && w.tickCount % 40L == 0L && g != null && g.player != null && !g.player.dead && !g.player.creative && !g.player.spectator) {
            if (Math.hypot(g.player.x, g.player.z) > 28.0) {
               g.player.hurt(2);
               g.say("\u00a7c[Showdown] Move toward the center shrine!");
            }
         }
         if (!skywarsWon && g != null && w.tickCount > 100L && w.tickCount % 40L == 0L) {
            int enemies = countHostiles(g) + Net319.aliveEnemyPeers(-1);
            if (enemies == 0 && swIslandStocked[0] && (soloBots || Net319.peerCount() > 0)) {
               skywarsWon = true;
               SoundEngine.playMinigameSound("mg_win");
               g.say("\u00a76\u00a7l[Battle] VICTORY! \u00a7aYou are the last champion standing! (/battle reset for a new round)");
            }
         }
         return;
      }

      // 3. BEDWARS: stock team bases, resource generators, bed destruction alerts & victory check
      if (type == WorldSettings.BEDWARS) {
         stockBedwarsWorld(g, w);
         long dPeriod = w.tickCount >= 9600L ? 150L : (w.tickCount >= 2400L ? 200L : 300L);
         long ePeriod = w.tickCount >= 9600L ? 250L : (w.tickCount >= 4800L ? 340L : 500L);
         if (g != null && w.tickCount == 2400L) {
            g.say("\u00a7b\u00a7l[Bedwars] Diamond Generators upgraded to Tier II!");
         } else if (g != null && w.tickCount == 4800L) {
            g.say("\u00a72\u00a7l[Bedwars] Emerald Generators upgraded to Tier II!");
         } else if (g != null && w.tickCount == 9600L) {
            g.say("\u00a76\u00a7l[Bedwars] All Generators reached MAX OVERDRIVE (2x Speed)!");
         }
         // Only the authoritative instance (Singleplayer or Host) spawns generator item drops to prevent network duplication
         if (!Net319.isConnected()) {
            if (w.tickCount % 30L == 0L) {
               for (int t = 0; t < 4; t++) {
                  boolean forge = hasTeamForge(t);
                  if (!forge && w.tickCount % 60L != 0L) {
                     continue;
                  }
                  int bx = BEDWARS_BASES[t][0];
                  int bz = BEDWARS_BASES[t][1];
                  int dirX = Integer.compare(0, bx);
                  int dirZ = Integer.compare(0, bz);
                  double gx = bx - dirX * 4 + 0.5;
                  double gz = bz - dirZ * 4 + 0.5;
                  if (w.chunkAt((int)gx, (int)gz) != null) {
                     dropGeneratorItemCapped(w, gx, Y_DECK + 1.2, gz, Items.IRON_INGOT, 1, 48);
                     if (w.tickCount % 60L == 0L) {
                        dropGeneratorItemCapped(w, gx, Y_DECK + 1.2, gz, Blocks.WOOL0 + BEDWARS_COLORS[t], 4, 64);
                     }
                     long gInt = forge ? 90L : 180L;
                     if (w.tickCount % gInt == 0L) {
                        dropGeneratorItemCapped(w, gx, Y_DECK + 1.2, gz, Items.GOLD_INGOT, 1, 16);
                     }
                     if (forge && w.tickCount % 480L == 0L) {
                        dropGeneratorItemCapped(w, gx, Y_DECK + 1.2, gz, Items.EMERALD, 1, 6);
                     }
                  }
               }
            }
            if (w.tickCount > 0L && w.tickCount % dPeriod == 0L) {
               for (int[] d : BEDWARS_DIAMONDS) {
                  if (w.chunkAt(d[0], d[1]) != null) {
                     dropGeneratorItemCapped(w, d[0] + 0.5, Y_DECK + 1.2, d[1] + 0.5, Items.DIAMOND, 1, 8);
                  }
               }
            }
            if (w.tickCount > 0L && w.tickCount % ePeriod == 0L) {
               if (w.chunkAt(0, 0) != null) {
                  dropGeneratorItemCapped(w, -3.5, Y_DECK + 1.2, 0.5, Items.EMERALD, 1, 8);
                  dropGeneratorItemCapped(w, 4.5, Y_DECK + 1.2, 0.5, Items.EMERALD, 1, 8);
               }
            }
         }

         if (g != null && g.player != null && w.tickCount % 20L == 0L) {
            int my = getPlayerTeam(g);
            // Heal Pool & Bed Alarm Trap check for player's team
            if (hasTeamHealPool(my) && !g.player.dead && !g.player.spectator) {
               double distBase = Math.hypot(g.player.x - BEDWARS_BASES[my][0], g.player.z - BEDWARS_BASES[my][1]);
               if (distBase <= 11.0 && w.tickCount % 60L == 0L && g.player.health < 20) {
                  g.player.health = Math.min(20, g.player.health + 1);
               }
               if (trapCooldown[my] > 0) {
                  trapCooldown[my]--;
               } else if (isTeamBedIntact(w, my)) {
                  int[] bf = getTeamBedFoot(my);
                  if (Net319.enemyPeerNear(w, my, bf[0] + 0.5, bf[1], bf[2] + 0.5, 7.5)) {
                     trapCooldown[my] = 15;
                     g.say("\u00a7c\u00a7l[ALARM TRAP] \u00a7eAn enemy player has entered your Bed courtyard!");
                  }
               }
            }
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
      if (c.equals("/tumble") || c.equals("/sb") || c.equals("/challenges")) {
         String sub = args.length > 1 ? args[1].toLowerCase() : "status";
         if (sub.equals("reset")) {
            resetArenaMap(g, g.world, true);
            matchState = STATE_PLAYING;
            g.say("\u00a7b[Tumble] All 3 Tumble layers restored and new round started!");
            return true;
         }
         if (sub.equals("mode") && args.length > 2) {
            String mn = args[2].toLowerCase();
            tumbleWeaponMode = mn.startsWith("sh") || mn.equals("1") ? 1 : (mn.startsWith("sn") || mn.equals("2") ? 2 : 0);
            giveTumbleLoadout(g);
            g.say("\u00a7b[Tumble] Weapon Mode: \u00a7e" + TUMBLE_MODE_NAMES[tumbleWeaponMode]);
            return true;
         }
         if (sub.equals("map") && args.length > 2 && g.world.settings != null) {
            int m = parseMapArg(args[2]);
            g.world.settings.mapId = m;
            resetArenaMap(g, g.world, true);
            matchState = STATE_PLAYING;
            g.say("\u00a7b[Tumble] Switched arena map to \u00a7e" + WorldSettings.MAP_NAMES[m] + "\u00a7b!");
            return true;
         }
         if (sub.equals("home") && g.player != null) {
            g.player.setPos(0.5, Y_DECK + 1.0, 0.5);
            g.player.fallDistance = 0.0F;
            g.player.updateBox();
            g.say("\u00a7b[Tumble] Teleported to Top Layer (0, 65, 0)!");
            return true;
         }
         int rivals = countHostiles(g) + Net319.aliveEnemyPeers(-1);
         String mapName = g.world.settings != null ? g.world.settings.mapName() : "Cove";
         g.say("\u00a7b[Tumble] Map: " + mapName + " | Mode: " + TUMBLE_MODE_NAMES[tumbleWeaponMode % 3] + " | Rivals: " + rivals + " (/tumble reset | /tumble mode <mixed|shovels|snowballs> | /tumble map <cove|crucible|cavern|temple>)");
         return true;
      }
      if (c.equals("/battle") || c.equals("/sw")) {
         String sub = args.length > 1 ? args[1].toLowerCase() : "status";
         if (sub.equals("kit") && args.length > 2) {
            String kn = args[2].toLowerCase();
            int k = kn.startsWith("h") || kn.equals("1") ? 1 : (kn.startsWith("a") || kn.equals("2") ? 2 : (kn.startsWith("p") || kn.equals("3") ? 3 : 0));
            giveSkywarsKit(g, k);
            g.say("\u00a76[Battle] Selected Item Set: \u00a7e" + SW_KIT_NAMES[k] + " \u00a77(" + SW_KIT_DESC[k] + ")");
            return true;
         }
         if (sub.equals("map") && args.length > 2 && g.world.settings != null) {
            int m = parseMapArg(args[2]);
            g.world.settings.mapId = m;
            resetArenaMap(g, g.world, true);
            matchState = STATE_PLAYING;
            g.say("\u00a76[Battle] Switched arena map to \u00a7e" + WorldSettings.MAP_NAMES[m] + "\u00a76!");
            return true;
         }
         if (sub.equals("refill")) {
            stockSkywarsChests(g, g.world, true);
            SoundEngine.playMinigameSound("mg_refill");
            g.say("\u00a7b[Battle] All chests refilled!");
         } else if (sub.equals("reset")) {
            resetArenaMap(g, g.world, true);
            matchState = STATE_PLAYING;
            g.say("\u00a7b[Battle] Arena map completely reset and all chests restocked!");
         } else if (sub.equals("lobby")) {
            matchState = STATE_WAITING;
            g.setScreen(Game.S.WAITING);
         } else {
            long rem = (2400L - (g.world.tickCount % 2400L)) / 20L;
            String mapName = g.world.settings != null ? g.world.settings.mapName() : "Cove";
            g.say("\u00a76[Battle] Map: " + mapName + " | Set: " + SW_KIT_NAMES[swKit & 3] + " | Kills: " + myKills + " | Opponents: " + (countHostiles(g) + Net319.aliveEnemyPeers(-1)) + " | Refill in " + rem + "s (/battle kit | /battle map | /battle reset)");
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
         if (sub.equals("upgrade")) {
            int my = getPlayerTeam(g);
            if (g.player != null && (g.player.creative || g.countItem(Items.DIAMOND) >= 4)) {
               if (unlockNextTeamUpgrade(g, my, true) && !g.player.creative) {
                  g.removeItems(Items.DIAMOND, 4);
               }
            } else {
               g.say("\u00a7c[Bedwars] Need 4 Diamonds to unlock the next Team Upgrade! (Or buy from Upgrades Shopkeeper)");
            }
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
            resetArenaMap(g, g.world, true);
            matchState = STATE_PLAYING;
            g.say("\u00a7e[Bedwars] Arena map completely reset! All bridges cleared and all 4 Team Beds restored.");
            return true;
         }
         StringBuilder sb = new StringBuilder("\u00a7e[Bedwars] Teams: ");
         for (int t = 0; t < 4; t++) {
            sb.append(BEDWARS_CODES[t]).append(BEDWARS_NAMES[t]).append(isTeamBedIntact(g.world, t) ? ":\u00a7aBED " : ":\u00a7cOUT ");
         }
         sb.append("\u00a77(/bw shop | /bw upgrades | /bw upgrade | /bw team <red|blue|green|yellow> | /bw reset)");
         g.say(sb.toString());
         return true;
      }
      return false;
   }

   private static int parseMapArg(String s) {
      if (s == null) {
         return 0;
      }
      String m = s.toLowerCase();
      if (m.startsWith("cr") || m.equals("1")) {
         return WorldSettings.MAP_CRUCIBLE;
      }
      if (m.startsWith("ca") || m.equals("2")) {
         return WorldSettings.MAP_CAVERN;
      }
      if (m.startsWith("t") || m.equals("3")) {
         return WorldSettings.MAP_TEMPLE;
      }
      return WorldSettings.MAP_COVE;
   }

   /** Renders the top-right Scoreboard HUD for Console Edition Tumble, Battle, and Bedwars. */
   public static void drawModeHud(Game g) {
      if (g == null || g.world == null || g.world.settings == null || g.debug || g.screen == Game.S.WAITING) {
         return;
      }
      int type = g.world.settings.type;
      if (!g.world.settings.isVoidMode()) {
         return;
      }
      String mapName = g.world.settings.mapName();
      Gui gui = g.gui;
      java.util.ArrayList<String> lines = new java.util.ArrayList<>();
      if (type == WorldSettings.TUMBLE) {
         int rivals = countHostiles(g) + Net319.aliveEnemyPeers(-1);
         int layer = g.player != null ? (g.player.y >= Y_DECK - 2 ? 1 : (g.player.y >= Y_DECK - 7 ? 2 : 3)) : 1;
         lines.add("\u00a7b\u00a7lTUMBLE \u00a77(" + mapName + ")");
         lines.add("\u00a7fRivals Left: " + (rivals > 0 ? "\u00a7c" + rivals : "\u00a7a0 (VICTORY!)") + " \u00a77| Layer: \u00a7e" + layer + "/3");
         lines.add("\u00a7fMode: \u00a7a" + (tumbleWeaponMode == 0 ? "Shovels+Snow" : (tumbleWeaponMode == 1 ? "Shovels" : "Snowballs")));
         lines.add("\u00a77L-Click: Break | R-Click: Throw");
         lines.add("\u00a77/tumble reset | /tumble map");
      } else if (type == WorldSettings.BATTLE) {
         long rem = (2400L - (g.world.tickCount % 2400L)) / 20L;
         int raiders = countHostiles(g) + Net319.aliveEnemyPeers(-1);
         lines.add("\u00a76\u00a7lBATTLE \u00a77(" + mapName + " - " + SW_KIT_NAMES[swKit & 3] + ")");
         lines.add("\u00a7fFighters: " + (raiders > 0 ? "\u00a7c" + raiders : "\u00a7a0 (VICTORY!)") + " \u00a77| Kills: \u00a7e" + myKills);
         if (battleGraceTicks > 0) {
            lines.add("\u00a7aGrace Period: \u00a7e" + ((battleGraceTicks + 19) / 20) + "s");
         } else if (showdownActive) {
            lines.add("\u00a7c\u00a7lSHOWDOWN ACTIVE!");
         } else {
            lines.add(String.format("\u00a7fChest Refill: \u00a7b%d:%02d", rem / 60L, rem % 60L));
         }
         lines.add("\u00a77/battle kit | /battle map | /battle reset");
      } else if (type == WorldSettings.BEDWARS) {
         int my = getPlayerTeam(g);
         long dPeriod = g.world.tickCount >= 9600L ? 150L : (g.world.tickCount >= 2400L ? 200L : 300L);
         long ePeriod = g.world.tickCount >= 9600L ? 250L : (g.world.tickCount >= 4800L ? 340L : 500L);
         long dRem = (dPeriod - (g.world.tickCount % dPeriod)) / 20L;
         long eRem = (ePeriod - (g.world.tickCount % ePeriod)) / 20L;
         String tierStr = g.world.tickCount >= 9600L ? "Max" : (g.world.tickCount >= 4800L ? "Em II" : (g.world.tickCount >= 2400L ? "Dia II" : "Tier I"));
         lines.add("\u00a7e\u00a7lBEDWARS \u00a77(" + tierStr + ")");
         for (int t = 0; t < 4; t++) {
            boolean bed = isTeamBedIntact(g.world, t);
            lines.add(BEDWARS_CODES[t] + "[" + BEDWARS_NAMES[t].charAt(0) + "] " + BEDWARS_NAMES[t] + ": " + (bed ? "\u00a7aBED \u2714" : "\u00a7cBROKEN \u2718") + (t == my ? " \u00a77(You)" : ""));
         }
         lines.add("\u00a7bDia: " + dRem + "s \u00a72Em: " + eRem + "s \u00a7eKills: " + myKills);
         int u = teamUpgrades[my];
         if (u != 0) {
            StringBuilder usb = new StringBuilder("\u00a7dUpg:");
            if ((u & UPG_SHARPNESS) != 0) usb.append(" Sharp");
            if ((u & UPG_PROTECTION) != 0) usb.append(" Prot");
            if ((u & UPG_FORGE) != 0) usb.append(" Forge");
            if ((u & UPG_HEAL_POOL) != 0) usb.append(" Pool");
            lines.add(usb.toString());
         }
         lines.add("\u00a77/shop | /upgrades | Tab");
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
