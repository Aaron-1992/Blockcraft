package bc;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.lwjgl.opengl.GL11;

/**
 * World generator and full gameplay & AI engine for {@link WorldSettings#TUMBLE}, {@link WorldSettings#BATTLE},
 * and {@link WorldSettings#BEDWARS} (Update 4.2).
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
   public static volatile int myBedsBroken = 0;
   /** Optional custom prebuilt map folder name inside maps/<name> or saves/<name>. */
   public static volatile String customMapName = null;

   // Post-Match Overview & Inter-Round Lobby Map Voting State
   public static volatile boolean showingOverview = false;
   public static volatile boolean lastRoundWon = true;
   public static volatile boolean localEliminated = false;
   public static volatile boolean pendingOverviewWon = true;
   public static volatile String pendingOverviewChamp = null;
   public static volatile int overviewCountdownTicks = -1;
   public static volatile int victoryTransitionTicks = -1;
   public static volatile String lastRoundMode = "Battle";
   public static volatile String lastRoundMap = "Cove";
   public static volatile int lastRoundKills = 0;
   public static volatile int lastRoundBedsBroken = 0;
   public static volatile int lastRoundDurationSec = 0;
   public static volatile String lastRoundFirst = "You (Champion!)";
   public static volatile String lastRoundSecond = "Valkyrie";
   public static volatile String lastRoundThird = "BlazeKing";
   public static volatile int myMapVote = -1;
   public static final int[] mapVotes = new int[]{1, 1, 0, 1};
   private static final int[] aiMapVotes = new int[]{1, 1, 0, 1};
   private static final java.util.concurrent.ConcurrentHashMap<Integer, Integer> peerMapVotes = new java.util.concurrent.ConcurrentHashMap<>();

   public static final String[] BOT_NAMES = new String[]{
      "Valkyrie", "BlazeKing", "Shadow", "Frostbite", "Nova", "Titan", "Aegis", "Zephyr"
   };
   /** Bedwars AI respawn timers: [team * 2 + roleIdx] -> ticks until respawn (0 = alive or permanently eliminated). */
   private static final int[] bwBotRespawnTicks = new int[8];
   /** Bedwars AI bridge progress per team (0..32 blocks outward from base). */
   private static final int[] bwTeamBridgeStep = new int[4];
   /** Per-bot Golden Apple heal count, Ender Pearl cooldown, Gear Tier & Looting state indexed by (bot.netId & 31). */
   private static final int[] botHealsUsed = new int[32];
   private static final int[] botPearlCooldown = new int[32];
   private static final int[] botGearTier = new int[32];
   private static final int[] botResources = new int[32];
   private static final int[] botHoldingBlockTicks = new int[32];
   private static final int[] botEatTicks = new int[32];
   private static final int[] botBowDrawTicks = new int[32];
   /** Strict AI Inventory Ledger: bots can ONLY use blocks, arrows, grenades, apples, or weapons they actually own! */
   public static final int[] botBlocks = new int[32];
   public static final int[] botArrows = new int[32];
   public static final int[] botGrenades = new int[32];
   public static final int[] botApples = new int[32];
   public static final int[] botWeaponItem = new int[32];
   public static final int[] botRangedItem = new int[32];
   public static final int[] botGold = new int[32];
   public static final int[] botMurderRole = new int[32];
   private static final int[] botBuildUpCooldown = new int[32];

   // Thrown TNT Frag Grenade horizontal velocities
   private static final java.util.IdentityHashMap<World.Falling, double[]> tntHorizVel = new java.util.IdentityHashMap<>();
   public static volatile int playerGunCooldown = 0;

   // CS2 Tactical Strike state (CT = Team 0, T = Team 1; Bomb Site A = (-22, Y_DECK+1, 0), Bomb Site B = (22, Y_DECK+1, 0))
   public static final String[] CS2_KIT_NAMES = new String[]{"Assault SMG", "AWP Sniper", "Demolitions", "Tactical Support"};
   public static final String[] CS2_KIT_DESC = new String[]{
      "Assault SMG + Tactical Pistol + 64 Ammo + 2 TNT Grenades + 16 Cover Blocks",
      "Heavy Sniper + Tactical Pistol + 48 Ammo + 1 TNT Grenade + 16 Cover Blocks",
      "Assault SMG + Combat Knife + 64 Ammo + 4 TNT Grenades + 24 Cover Blocks",
      "Assault SMG + Tactical Pistol + 64 Ammo + 2 Medkits + 24 Cover Blocks"
   };
   public static volatile boolean cs2BombPlanted = false;
   public static volatile int cs2BombSite = 0; // 1 = Site A (-22, 0), 2 = Site B (+22, 0)
   public static volatile int cs2BombTimerTicks = 0;
   public static volatile int cs2DefuseTicks = 0;
   private static boolean cs2BotsSpawned = false;
   public static volatile boolean cs2RoundOver = false;

   // Murder Mystery state (0 = Innocent, 1 = Sheriff, 2 = Murderer)
   public static final int MURDER_ROLE_INNOCENT = 0;
   public static final int MURDER_ROLE_SHERIFF = 1;
   public static final int MURDER_ROLE_MURDERER = 2;
   public static final String[] MURDER_ROLE_NAMES = new String[]{"INNOCENT", "SHERIFF", "MURDERER"};
   public static final String[] MURDER_ROLE_CODES = new String[]{"\u00a7a", "\u00a79", "\u00a7c"};
   public static final String[] MURDER_PREF_NAMES = new String[]{"Random Role", "Murderer", "Sheriff", "Innocent"};
   public static volatile int murderRolePref = 0;
   public static volatile int myMurderRole = MURDER_ROLE_INNOCENT;
   public static volatile int myMurderGold = 0;
   public static volatile boolean murderWeaponsDealt = false;
   public static volatile boolean murderGunDropped = false;
   public static volatile double murderDroppedGunX = 0.5;
   public static volatile double murderDroppedGunY = Y_DECK + 1.0;
   public static volatile double murderDroppedGunZ = 0.5;
   private static boolean murderBotsSpawned = false;
   private static boolean murderRoundOver = false;

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
   private static final boolean[] swBotSpawned = new boolean[8];
   private static boolean skywarsWon;
   private static final boolean[] bwBaseStocked = new boolean[5];
   private static final boolean[] bwBotInitialSpawned = new boolean[8];
   private static boolean bedwarsWon;
   private static final boolean[] bedKnownIntact = new boolean[]{true, true, true, true};
   private static int whiteGlTex;

   public static int whiteTex() {
      if (whiteGlTex == 0) {
         whiteGlTex = org.lwjgl.opengl.GL11.glGenTextures();
         org.lwjgl.opengl.GL11.glBindTexture(3553, whiteGlTex);
         java.nio.ByteBuffer bb = org.lwjgl.BufferUtils.createByteBuffer(4);
         bb.put((byte)-1).put((byte)-1).put((byte)-1).put((byte)-1).flip();
         org.lwjgl.opengl.GL11.glTexParameteri(3553, 10241, 9728);
         org.lwjgl.opengl.GL11.glTexParameteri(3553, 10240, 9728);
         org.lwjgl.opengl.GL11.glTexImage2D(3553, 0, 6408, 1, 1, 0, 6408, 5121, bb);
      }
      return whiteGlTex;
   }

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
      Arrays.fill(swBotSpawned, false);
      skywarsWon = false;
      tumbleSpawnedBots = false;
      tumbleWon = false;
      battleGraceTicks = 300; // 15-second Console Edition Battle invulnerability timer
      showdownActive = false;
      Arrays.fill(bwBaseStocked, false);
      Arrays.fill(bwBotInitialSpawned, false);
      bedwarsWon = false;
      Arrays.fill(bedKnownIntact, true);
      Arrays.fill(teamUpgrades, 0);
      Arrays.fill(trapCooldown, 0);
      Arrays.fill(bwBotRespawnTicks, 0);
      Arrays.fill(bwTeamBridgeStep, 0);
      Arrays.fill(botHealsUsed, 0);
      Arrays.fill(botPearlCooldown, 0);
      Arrays.fill(botGearTier, 0);
      Arrays.fill(botResources, 0);
      Arrays.fill(botHoldingBlockTicks, 0);
      Arrays.fill(botEatTicks, 0);
      Arrays.fill(botBowDrawTicks, 0);
      Arrays.fill(botBlocks, 0);
      Arrays.fill(botArrows, 0);
      Arrays.fill(botGrenades, 0);
      Arrays.fill(botApples, 0);
      Arrays.fill(botWeaponItem, 0);
      Arrays.fill(botRangedItem, 0);
      Arrays.fill(botGold, 0);
      Arrays.fill(botMurderRole, 0);
      Arrays.fill(botBuildUpCooldown, 0);
      synchronized (tntHorizVel) {
         tntHorizVel.clear();
      }
      playerGunCooldown = 0;
      cs2BombPlanted = false;
      cs2BombSite = 0;
      cs2BombTimerTicks = 0;
      cs2DefuseTicks = 0;
      cs2BotsSpawned = false;
      cs2RoundOver = false;
      myMurderGold = 0;
      murderWeaponsDealt = false;
      murderGunDropped = false;
      murderBotsSpawned = false;
      murderRoundOver = false;
      myKills = 0;
      myDeaths = 0;
      myBedsBroken = 0;
      localEliminated = false;
      pendingOverviewWon = true;
      pendingOverviewChamp = null;
      victoryTransitionTicks = -1;
      lobbyCountdownTicks = -1;
   }

   /** Returns true if a player who dies in the current minigame mode is permanently eliminated for this round. */
   public static boolean shouldStayEliminated(Game g) {
      if (g == null || g.world == null || g.world.settings == null) {
         return false;
      }
      int type = g.world.settings.type;
      if (type == WorldSettings.BATTLE || type == WorldSettings.TUMBLE || type == WorldSettings.CS2 || type == WorldSettings.MURDER) {
         return true;
      }
      if (type == WorldSettings.BEDWARS) {
         return !isTeamBedIntact(g.world, getPlayerTeam(g));
      }
      return false;
   }

   /** Called when the local player dies in-game; marks localEliminated if they cannot respawn this round and notifies peers. */
   public static void onLocalPlayerDied(Game g) {
      if (shouldStayEliminated(g)) {
         localEliminated = true;
         if (Net319.isActive()) {
            Net319.sendEliminated(getPlayerTeam(g));
         }
      }
   }

   public static void initMapVoting(Game g) {
      int curMap = (g != null && g.world != null && g.world.settings != null) ? (g.world.settings.mapId & 3) : 0;
      long seed = System.nanoTime() ^ (g != null && g.world != null ? g.world.tickCount : 17L);
      Arrays.fill(aiMapVotes, 0);
      peerMapVotes.clear();
      if (soloBots) {
         // 3 AI lobby competitors cast votes across the 4 maps
         for (int i = 0; i < 3; i++) {
            int pick = (int)(Math.abs(seed + i * 31L + curMap) & 3);
            aiMapVotes[pick]++;
         }
      }
      if (myMapVote < 0 || myMapVote >= 4) {
         myMapVote = curMap;
      }
      recomputeMapVotes();
   }

   public static void onRemoteMapVote(int peerId, int mapIdx) {
      peerMapVotes.put(peerId, Math.abs(mapIdx) & 3);
      recomputeMapVotes();
   }

   private static void recomputeMapVotes() {
      for (int m = 0; m < 4; m++) {
         mapVotes[m] = (soloBots ? aiMapVotes[m] : 0) + (myMapVote == m ? 2 : 0);
      }
      for (Integer v : peerMapVotes.values()) {
         if (v != null && v >= 0 && v < 4) {
            mapVotes[v] += 2;
         }
      }
   }

   public static int winningVotedMap(int fallback) {
      int bestMap = (myMapVote >= 0 && myMapVote < 4) ? myMapVote : (fallback & 3);
      int bestVotes = mapVotes[bestMap];
      for (int m = 0; m < 4; m++) {
         if (mapVotes[m] > bestVotes) {
            bestVotes = mapVotes[m];
            bestMap = m;
         }
      }
      return bestMap;
   }

   public static void castMapVote(Game g, int mapIdx, boolean applyPreview) {
      int m = Math.abs(mapIdx) & 3;
      myMapVote = m;
      recomputeMapVotes();
      if (Net319.isActive()) {
         Net319.sendMatchControl("VOTE " + Net319.selfId + " " + m);
      }
      int win = winningVotedMap(m);
      if (applyPreview && !Net319.isClientConnected() && g != null && g.world != null && g.world.settings != null && g.world.settings.mapId != win) {
         g.world.settings.mapId = win;
         resetArenaMap(g, g.world, true);
      }
   }

   public static void openMatchOverview(Game g, boolean won) {
      openMatchOverview(g, won, null, Net319.isHosting());
   }

   public static void openMatchOverview(Game g, boolean won, String customChamp, boolean broadcast) {
      if (g == null) {
         return;
      }
      World w = g.world;
      int wType = (w != null && w.settings != null) ? w.settings.type : WorldSettings.BATTLE;
      lastRoundMode = wType == WorldSettings.BEDWARS ? "Bedwars" : (wType == WorldSettings.TUMBLE ? "Tumble" : "Battle");
      lastRoundMap = (w != null && w.settings != null) ? w.settings.mapName() : "Cove";
      lastRoundWon = won;
      lastRoundKills = myKills;
      lastRoundBedsBroken = myBedsBroken;
      lastRoundDurationSec = w != null ? Math.max(5, (int)(w.tickCount / 20L)) : 45;
      String champ = (customChamp != null && !customChamp.isBlank())
         ? customChamp
         : (won ? (Net319.isActive() ? Net319.selfName + " (Champion!)" : "You (Champion!)") : BOT_NAMES[0] + " (Champion)");
      lastRoundFirst = champ;
      lastRoundSecond = won ? BOT_NAMES[0] : (Net319.isActive() ? Net319.selfName : "You (Runner-Up)");
      lastRoundThird = BOT_NAMES[1];
      showingOverview = true;
      overviewCountdownTicks = 180; // 9 seconds on overview before auto-transitioning to Lobby
      victoryTransitionTicks = -1;
      matchState = STATE_WAITING;
      initMapVoting(g);
      g.setScreen(Game.S.WAITING);
      if (broadcast && Net319.isHosting()) {
         String safeChamp = champ.replace(' ', '_');
         Net319.sendMatchControl("OVERVIEW " + safeChamp + " " + lastRoundMode + " " + lastRoundMap + " " + lastRoundDurationSec);
      }
   }

   public static void onRemoteMatchOverview(Game g, String champToken, String modeName, String mapName, int durSec) {
      if (g == null) {
         return;
      }
      String champ = champToken == null ? "Champion" : champToken.replace('_', ' ');
      boolean iWon = champ.toLowerCase().startsWith(Net319.selfName.toLowerCase());
      lastRoundMode = modeName != null ? modeName : "Battle";
      lastRoundMap = mapName != null ? mapName : "Cove";
      lastRoundWon = iWon;
      lastRoundKills = myKills;
      lastRoundBedsBroken = myBedsBroken;
      lastRoundDurationSec = Math.max(5, durSec);
      lastRoundFirst = champ;
      lastRoundSecond = iWon ? BOT_NAMES[0] : Net319.selfName;
      lastRoundThird = BOT_NAMES[1];
      showingOverview = true;
      overviewCountdownTicks = 180;
      victoryTransitionTicks = -1;
      matchState = STATE_WAITING;
      initMapVoting(g);
      g.setScreen(Game.S.WAITING);
   }

   public static void enterInterRoundLobby(Game g) {
      showingOverview = false;
      overviewCountdownTicks = -1;
      victoryTransitionTicks = -1;
      matchState = STATE_WAITING;
      if (g != null) {
         initMapVoting(g);
         if (g.world != null && g.world.settings != null && !Net319.isClientConnected()) {
            g.world.settings.mapId = winningVotedMap(g.world.settings.mapId);
            resetArenaMap(g, g.world, true);
         }
         lobbyCountdownTicks = 240; // 12-second lobby countdown for the next round (while voting on maps!)
         g.setScreen(Game.S.WAITING);
         g.say("\u00a7e[Lobby] Returned to lobby! Vote for the next Arena Map — new round starts in 12s!");
         if (Net319.isHosting() && g.world != null && g.world.settings != null) {
            Net319.sendMatchControl("LOBBY " + g.world.settings.mapId);
         }
      }
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

   /** Returns true if (x, y, z) is a destructible block in the multi-height 3-layer Tumble arena. */
   public static boolean isTumbleLayerBlock(int x, int y, int z) {
      return Math.hypot(x, z) <= 19.2 && y >= Y_DECK - 11 && y <= Y_DECK + 5 && !(x == 0 && y == Y_DECK - 2 && z == 0);
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

   public static int getBedTeamAt(int x, int y, int z) {
      return getBedTeamAt(x, z);
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

   public static void registerThrownTnt(World.Falling f, double vx, double vz) {
      if (f != null) {
         synchronized (tntHorizVel) {
            tntHorizVel.put(f, new double[]{vx, vz});
         }
      }
   }

   public static void removeThrownTnt(World.Falling f) {
      if (f != null) {
         synchronized (tntHorizVel) {
            tntHorizVel.remove(f);
         }
      }
   }

   public static void tickThrownTntHorizontal(World w, World.Falling f) {
      if (w == null || f == null) {
         return;
      }
      double[] hv;
      synchronized (tntHorizVel) {
         hv = tntHorizVel.get(f);
      }
      if (hv == null) {
         return;
      }
      double nx = f.x + hv[0];
      double nz = f.z + hv[1];
      int by = (int)Math.floor(f.y + 0.2);
      if (Blocks.solid[w.getBlock((int)Math.floor(nx), by, (int)Math.floor(f.z))]) {
         hv[0] = -hv[0] * 0.45;
      } else {
         f.x = nx;
      }
      if (Blocks.solid[w.getBlock((int)Math.floor(f.x), by, (int)Math.floor(nz))]) {
         hv[1] = -hv[1] * 0.45;
      } else {
         f.z = nz;
      }
      boolean onGround = Blocks.solid[w.getBlock((int)Math.floor(f.x), (int)Math.floor(f.y - 0.05), (int)Math.floor(f.z))];
      double drag = onGround ? 0.72 : 0.96;
      hv[0] *= drag;
      hv[1] *= drag;
   }

   public static void onTntGrenadeDetonate(Game g, World w, double gx, double gy, double gz) {
      if (g == null || w == null) {
         return;
      }
      SoundEngine.play3D(w, "explode", gx, gy, gz, 1.0F, 0.95F);
      double radius = 5.2;
      if (g.player != null && !g.player.dead && !g.player.creative && !g.player.spectator) {
         double d = Math.sqrt((g.player.x - gx) * (g.player.x - gx) + (g.player.y + 0.9 - gy) * (g.player.y + 0.9 - gy) + (g.player.z - gz) * (g.player.z - gz));
         if (d < radius) {
            int dmg = Math.max(2, (int)Math.round(14.0 * (1.0 - d / radius)));
            g.damagePlayer(dmg, gx, gz);
         }
      }
      if (g.mobs != null) {
         for (int i = g.mobs.list.size() - 1; i >= 0; i--) {
            if (i >= g.mobs.list.size()) {
               continue;
            }
            Mobs.Mob m = g.mobs.list.get(i);
            if (m != null && !m.dead) {
               double d = Math.sqrt((m.x - gx) * (m.x - gx) + (m.y + 0.9 - gy) * (m.y + 0.9 - gy) + (m.z - gz) * (m.z - gz));
               if (d < radius) {
                  int dmg = Math.max(2, (int)Math.round(14.0 * (1.0 - d / radius)));
                  g.mobs.hurtMob(m, dmg, gx, gz, 0.65, true);
               }
            }
         }
      }
      if (Net319.isActive()) {
         for (Net319.Peer peer : Net319.peers().values()) {
            if (peer != null && !peer.spectator && !peer.eliminated && peer.health > 0) {
               double d = Math.sqrt((peer.x - gx) * (peer.x - gx) + (peer.y - gy) * (peer.y - gy) + (peer.z - gz) * (peer.z - gz));
               if (d < radius) {
                  int dmg = Math.max(2, (int)Math.round(14.0 * (1.0 - d / radius)));
                  Net319.sendHit(peer.id, dmg, gx, gz, Net319.selfName);
               }
            }
         }
      }
   }

   public static boolean tryUseTacticalWeapon(Game g, int itemId) {
      if (g == null) {
         return false;
      }
      Stack held = g.held();
      if (held == null || held.id != itemId) {
         held = new Stack(itemId, 1);
      }
      return tryUseTacticalWeapon(g, held, true);
   }

   /**
    * Fires Tactical Pistol, Assault SMG (Machine Gun), Heavy Sniper, TNT Frag Grenade, Murderer's Thrown Knife,
    * or plants/defuses the C4 Explosive!
    */
   public static boolean tryUseTacticalWeapon(Game g, Stack held, boolean isClickOrHold) {
      if (g == null || g.world == null || g.player == null || held == null || g.player.spectator || g.player.dead) {
         return false;
      }
      int id = held.id;
      Player p = g.player;
      double yaw = Math.toRadians(p.yaw);
      double pitch = Math.toRadians(p.pitch);
      double dx = -Math.sin(yaw) * Math.cos(pitch);
      double dy = -Math.sin(pitch);
      double dz = Math.cos(yaw) * Math.cos(pitch);
      double sx = p.x + dx * 0.45;
      double sy = p.y + p.eye - 0.1;
      double sz = p.z + dz * 0.45;

      // 1. Tactical Firearms (Pistol, Machine Gun, Sniper Rifle) — shoot high-velocity Arrows consuming Arrow ammo!
      if (id == Redstone319.PISTOL || id == Redstone319.MACHINE_GUN || id == Redstone319.SNIPER_RIFLE) {
         if (playerGunCooldown > 0) {
            return true;
         }
         if (!p.creative && g.countItem(Items.ARROW) <= 0) {
            if (isClickOrHold && g.world.tickCount % 15L == 0L) {
               SoundEngine.playUi("click_block", 0.55F, 1.6F);
               g.say("\u00a7c[Out of Ammo] No Arrows remaining in inventory!");
            }
            return true;
         }
         if (!p.creative) {
            g.removeItems(Items.ARROW, 1);
         }
         boolean isMurder = g.world.settings != null && g.world.settings.type == WorldSettings.MURDER;
         float speed = id == Redstone319.SNIPER_RIFLE ? 3.6F : (id == Redstone319.PISTOL ? 2.6F : 2.5F);
         int dmg = isMurder ? 24 : (id == Redstone319.SNIPER_RIFLE ? 16 : (id == Redstone319.PISTOL ? 6 : 4));
         playerGunCooldown = id == Redstone319.SNIPER_RIFLE ? 22 : (id == Redstone319.PISTOL ? 7 : 3);
         // Slight spread for full-auto SMG bursts
         double spread = id == Redstone319.MACHINE_GUN ? 0.022 : (id == Redstone319.PISTOL ? 0.008 : 0.002);
         double jx = dx + (g.rnd.nextDouble() - 0.5) * spread;
         double jy = dy + (g.rnd.nextDouble() - 0.5) * spread;
         double jz = dz + (g.rnd.nextDouble() - 0.5) * spread;
         g.mobs.shootGunArrow(sx, sy, sz, jx, jy, jz, speed, dmg, true);
         SoundEngine.play3D(g.world, "bow", p.x, p.y + p.eye, p.z, 0.9F, id == Redstone319.SNIPER_RIFLE ? 0.68F : (id == Redstone319.MACHINE_GUN ? 1.45F : 1.25F));
         SoundEngine.play3D(g.world, "hit_1", p.x, p.y + p.eye, p.z, 0.55F, id == Redstone319.SNIPER_RIFLE ? 0.72F : 1.55F);
         return true;
      }

      // 2. TNT Frag Grenade — throws a live bouncing Primed TNT Grenade!
      if (id == Redstone319.TNT_GRENADE) {
         if (playerGunCooldown > 0) {
            return true;
         }
         playerGunCooldown = 12;
         SoundEngine.playThrow(g.world, p.x, p.y + p.eye, p.z);
         SoundEngine.play3D(g.world, "tnt_prime", p.x, p.y + p.eye, p.z, 0.8F, 1.25F);
         g.world.throwTntGrenade(p.x + dx * 0.65, sy - 0.45, p.z + dz * 0.65, dx * 0.92, dy * 0.75 + 0.14, dz * 0.92, 35);
         if (!p.creative) {
            held.n--;
            if (held.n <= 0 && g.inv[g.sel] == held) {
               g.inv[g.sel] = null;
            }
         }
         return true;
      }

      // 3. Murderer's Knife — Right-Click throws a flying lethal blade (5s cooldown)!
      if (id == Redstone319.MURDER_KNIFE) {
         if (playerGunCooldown > 0) {
            g.say("\u00a7c[Knife Throw] Blade throw recharging (" + ((playerGunCooldown + 19) / 20) + "s)...");
            return true;
         }
         playerGunCooldown = 90;
         SoundEngine.playThrow(g.world, p.x, p.y + p.eye, p.z);
         g.mobs.shootGunArrow(sx, sy, sz, dx, dy, dz, 2.3F, 24, true);
         return true;
      }

      // 4. C4 Explosive / Defuser in CS2 Strike mode
      if (id == Redstone319.C4_BOMB && g.world.settings != null && g.world.settings.type == WorldSettings.CS2) {
         int my = getPlayerTeam(g) & 1; // 0 = CT, 1 = T
         if (my == 1 && !cs2BombPlanted) {
            int site = nearCs2BombSite(p.x, p.z);
            if (site > 0) {
               plantCs2Bomb(g, site, Net319.selfName, true);
            } else {
               g.say("\u00a7c[CS2 Strike] Move to Bomb Site A (West: -22, 0) or Bomb Site B (East: +22, 0) to plant the C4!");
            }
         } else if (my == 0 && cs2BombPlanted) {
            int site = nearCs2BombSite(p.x, p.z);
            if (site == cs2BombSite) {
               defuseCs2Bomb(g, Net319.selfName, true);
            } else {
               g.say("\u00a7c[CS2 Strike] Move to Planted Bomb Site " + (cs2BombSite == 1 ? "A" : "B") + " to defuse the C4!");
            }
         }
         return true;
      }
      return false;
   }

   public static int nearCs2BombSite(double x, double y, double z) {
      return Math.abs(y - (Y_DECK + 1.0)) <= 4.5 ? nearCs2BombSite(x, z) : 0;
   }

   public static int nearCs2BombSite(double x, double z) {
      if (Math.hypot(x - (-22.0), z) <= 5.5) {
         return 1; // Bomb Site A
      }
      if (Math.hypot(x - 22.0, z) <= 5.5) {
         return 2; // Bomb Site B
      }
      return 0;
   }

   public static void plantCs2Bomb(Game g, int site, String planter, boolean broadcast) {
      if (cs2BombPlanted || cs2RoundOver) {
         return;
      }
      cs2BombPlanted = true;
      cs2BombSite = site == 2 ? 2 : 1;
      cs2BombTimerTicks = 700; // 35 seconds to detonation
      cs2DefuseTicks = 0;
      int bx = cs2BombSite == 1 ? -22 : 22;
      if (g != null && g.world != null) {
         g.world.setBlock(bx, Y_DECK + 1, 0, Redstone319.REDSTONE_LAMP_LIT);
         SoundEngine.play3D(g.world, "tnt_prime", bx + 0.5, Y_DECK + 1.5, 0.5, 1.0F, 0.9F);
      }
      if (g != null) {
         String sName = cs2BombSite == 1 ? "SITE A" : "SITE B";
         g.say("\u00a7c\u00a7l[CS2 Strike] THE C4 BOMB HAS BEEN PLANTED AT " + sName + " by \u00a7f" + planter + "\u00a7c\u00a7l! (35s to detonation!)");
      }
      if (broadcast && Net319.isActive()) {
         Net319.sendMatchControl("BOMB_PLANT " + cs2BombSite + " " + (planter == null ? "Terrorist" : planter.replace(' ', '_')));
      }
   }

   public static void defuseCs2Bomb(Game g, String defuser, boolean broadcast) {
      if (!cs2BombPlanted || cs2RoundOver) {
         return;
      }
      int bx = cs2BombSite == 1 ? -22 : 22;
      cs2BombPlanted = false;
      cs2BombTimerTicks = 0;
      cs2RoundOver = true;
      if (g != null && g.world != null) {
         g.world.setBlock(bx, Y_DECK + 1, 0, Blocks.GOLD_BLOCK);
         SoundEngine.playMinigameSound("mg_win");
      }
      boolean iAmCt = (getPlayerTeam(g) & 1) == 0;
      pendingOverviewWon = iAmCt;
      pendingOverviewChamp = (defuser != null ? defuser : "Counter-Terrorists") + " (Defused C4!)";
      victoryTransitionTicks = 30;
      if (g != null) {
         g.say("\u00a79\u00a7l[CS2 Strike] BOMB HAS BEEN DEFUSED by \u00a7f" + defuser + "\u00a79\u00a7l! Counter-Terrorists Win!");
      }
      if (broadcast && Net319.isActive()) {
         Net319.sendMatchControl("BOMB_DEFUSE " + (defuser == null ? "CT" : defuser.replace(' ', '_')));
      }
   }

   public static void giveCs2Loadout(Game g) {
      giveCs2Loadout(g, getPlayerTeam(g), swKit);
   }

   public static void giveCs2Loadout(Game g, int kit) {
      giveCs2Loadout(g, getPlayerTeam(g), kit);
   }

   public static void giveCs2Loadout(Game g, int team, int kit) {
      if (g == null) {
         return;
      }
      int t = Math.abs(team) & 1; // 0 = CT (Blue), 1 = T (Red)
      int k = Math.abs(kit) & 3;
      swKit = k;
      Arrays.fill(g.inv, null);
      Arrays.fill(g.armor, null);
      int coverBlock = t == 0 ? Blocks.CONCRETE0 + 11 : Blocks.CONCRETE0 + 14;
      switch (k) {
         case 0 -> { // Assault SMG
            g.inv[0] = new Stack(Redstone319.MACHINE_GUN, 1);
            g.inv[1] = new Stack(Redstone319.PISTOL, 1);
            g.inv[2] = new Stack(Redstone319.MURDER_KNIFE, 1);
            g.inv[3] = new Stack(Redstone319.TNT_GRENADE, 2);
            g.inv[4] = new Stack(coverBlock, 16);
            g.inv[5] = new Stack(Items.ARROW, 64);
            g.inv[6] = new Stack(Items.ARROW, 32);
            g.armor[0] = new Stack(Items.armorId(1, 0), 1);
            g.armor[1] = new Stack(Items.armorId(1, 1), 1);
         }
         case 1 -> { // AWP Sniper
            g.inv[0] = new Stack(Redstone319.SNIPER_RIFLE, 1);
            g.inv[1] = new Stack(Redstone319.PISTOL, 1);
            g.inv[2] = new Stack(Redstone319.MURDER_KNIFE, 1);
            g.inv[3] = new Stack(Redstone319.TNT_GRENADE, 1);
            g.inv[4] = new Stack(coverBlock, 16);
            g.inv[5] = new Stack(Items.ARROW, 48);
            g.armor[0] = new Stack(Items.armorId(0, 0), 1);
            g.armor[1] = new Stack(Items.armorId(1, 1), 1);
         }
         case 2 -> { // Demolitions / Breacher
            g.inv[0] = new Stack(Redstone319.MACHINE_GUN, 1);
            g.inv[1] = new Stack(Redstone319.MURDER_KNIFE, 1);
            g.inv[2] = new Stack(Redstone319.TNT_GRENADE, 4);
            g.inv[3] = new Stack(coverBlock, 24);
            g.inv[4] = new Stack(Items.ARROW, 64);
            g.armor[0] = new Stack(Items.armorId(1, 0), 1);
            g.armor[1] = new Stack(Items.armorId(1, 1), 1);
            g.armor[3] = new Stack(Items.armorId(1, 3), 1);
         }
         default -> { // Tactical Support
            g.inv[0] = new Stack(Redstone319.MACHINE_GUN, 1);
            g.inv[1] = new Stack(Redstone319.PISTOL, 1);
            g.inv[2] = new Stack(Redstone319.TNT_GRENADE, 2);
            g.inv[3] = new Stack(Redstone319.GOLDEN_APPLE, 2);
            g.inv[4] = new Stack(coverBlock, 24);
            g.inv[5] = new Stack(Items.ARROW, 64);
            g.armor[0] = new Stack(Items.armorId(3, 0), 1);
            g.armor[1] = new Stack(Items.armorId(1, 1), 1);
         }
      }
      g.inv[7] = new Stack(Redstone319.C4_BOMB, 1);
      g.sel = 0;
   }

   public static void assignMurderMysteryRoles(Game g) {
      assignMurderMysteryRoles(g, g != null ? g.world : null);
   }

   public static void assignMurderMysteryRoles(Game g, World w) {
      long rSeed = (w != null ? w.seed : 1337L) ^ System.nanoTime();
      int pref = Math.abs(murderRolePref) & 3;
      if (pref == 1) {
         myMurderRole = MURDER_ROLE_MURDERER;
      } else if (pref == 2) {
         myMurderRole = MURDER_ROLE_SHERIFF;
      } else if (pref == 3) {
         myMurderRole = MURDER_ROLE_INNOCENT;
      } else {
         int roll = (int)(Math.abs(rSeed) % 3L);
         myMurderRole = roll == 0 ? MURDER_ROLE_MURDERER : (roll == 1 ? MURDER_ROLE_SHERIFF : MURDER_ROLE_INNOCENT);
      }
      myMurderGold = 0;
      murderWeaponsDealt = false;
      murderGunDropped = false;
   }

   public static void giveMurderLoadout(Game g) {
      giveMurderLoadout(g, murderWeaponsDealt);
   }

   public static void giveMurderLoadout(Game g, int role) {
      myMurderRole = Math.max(0, Math.min(2, role));
      giveMurderLoadout(g, true);
   }

   public static void giveMurderLoadout(Game g, boolean dealWeapons) {
      if (g == null) {
         return;
      }
      Arrays.fill(g.inv, null);
      Arrays.fill(g.armor, null);
      if (dealWeapons) {
         if (myMurderRole == MURDER_ROLE_MURDERER) {
            g.inv[1] = new Stack(Redstone319.MURDER_KNIFE, 1);
            g.inv[8] = new Stack(Redstone319.COMPASS, 1);
            g.sel = 0; // Start on empty slot 0 so Knife stays concealed until you press 2!
         } else if (myMurderRole == MURDER_ROLE_SHERIFF) {
            g.inv[0] = new Stack(Redstone319.PISTOL, 1);
            g.inv[1] = new Stack(Items.ARROW, 24);
            g.inv[8] = new Stack(Redstone319.COMPASS, 1);
            g.sel = 0;
         } else {
            g.inv[8] = new Stack(Redstone319.COMPASS, 1);
            g.sel = 0;
         }
      } else {
         g.inv[8] = new Stack(Redstone319.COMPASS, 1);
         g.sel = 0;
      }
   }

   public static void onMurderArrowHitBot(Game g, Mobs.Mob victim, int dmg, double sx, double sz) {
      if (g == null || victim == null || victim.dead) {
         return;
      }
      int bSlot = Math.abs(victim.netId) & 31;
      int role = botMurderRole[bSlot];
      if (role == MURDER_ROLE_MURDERER || myMurderRole == MURDER_ROLE_MURDERER) {
         g.mobs.hurtMob(victim, 24, sx, sz, 0.5, true);
      } else {
         // Innocent or Sheriff accidentally shot an Innocent!
         g.mobs.hurtMob(victim, 24, sx, sz, 0.5, true);
         g.say("\u00a7c\u00a7l[Murder Mystery] You shot an INNOCENT bystander! You dropped your weapon in guilt!");
         g.removeItems(Redstone319.PISTOL, 1);
         murderGunDropped = true;
         murderDroppedGunX = g.player.x;
         murderDroppedGunY = g.player.y;
         murderDroppedGunZ = g.player.z;
         g.world.dropItem(g.player.x, g.player.y + 0.5, g.player.z, Redstone319.PISTOL, 1);
      }
      if (victim.dead) {
         myKills++;
         onBotKilled(g, victim);
      }
   }

   public static void switchMinigameMode(Game g, int newType) {
      switchMinigameMode(g, newType, Net319.isHosting());
   }

   /** Switches the active minigame mode in-place without disconnecting any connected Multiplayer peers! */
   public static void switchMinigameMode(Game g, int newType, boolean broadcast) {
      if (g == null || g.world == null || g.world.settings == null) {
         return;
      }
      g.world.settings.type = newType;
      if (g.worldSettings != null) {
         g.worldSettings.type = newType;
      }
      resetArenaMap(g, g.world, false);
      if (broadcast && Net319.isHosting()) {
         Net319.sendMatchControl("MODE " + newType + " " + g.world.settings.mapId);
         Net319.onHostWorldSwitched(g);
      }
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
         } else if (g.world.settings.type == WorldSettings.CS2) {
            int csTeam = t & 1; // 0 = CT (North, Z = -36), 1 = T (South, Z = +36)
            int sx = ((t >> 1) == 0 ? -2 : 2);
            int sz = csTeam == 0 ? -36 : 36;
            g.spawnXZ = new int[]{sx, sz};
            g.bedPos = null;
            if (g.player != null) {
               g.player.setMode(0);
               g.player.setPos(sx + 0.5, Y_DECK + 1.0, sz + 0.5);
               g.player.yaw = csTeam == 0 ? 0.0F : 180.0F;
               g.player.pitch = 0.0F;
               g.player.updateBox();
            }
            giveCs2Loadout(g, csTeam, swKit);
         } else if (g.world.settings.type == WorldSettings.MURDER) {
            int[][] mSpawns = new int[][]{{-18, -18}, {18, -18}, {-18, 18}, {18, 18}, {0, -24}, {0, 24}, {-24, 0}, {24, 0}};
            int[] sp = mSpawns[Math.abs(t) % mSpawns.length];
            g.spawnXZ = new int[]{sp[0], sp[1]};
            g.bedPos = null;
            if (g.player != null) {
               g.player.setMode(0);
               g.player.setPos(sp[0] + 0.5, Y_DECK + 1.0, sp[1] + 0.5);
               g.player.yaw = (float)Math.toDegrees(Math.atan2(sp[0], -sp[1]));
               g.player.pitch = 0.0F;
               g.player.updateBox();
            }
            giveMurderLoadout(g, murderWeaponsDealt);
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
      if (g != null && g.rend != null && targetW != null) {
         g.rend.clearWorldMeshes(targetW);
      }
      if (targetW != null) {
         targetW.tickCount = 0L;
         if (targetW.settings != null) {
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
            g.rend.clearWorldMeshes(targetW);
            g.rend.setLod(targetW);
         }
         if (g.player != null) {
            g.player.dead = false;
            g.player.spectator = false;
            g.player.flying = false;
            g.player.creative = false;
            g.player.burn = 0;
            g.player.hurtTime = 0;
            g.player.health = 20;
            g.player.food = 20;
            g.player.sat = 5.0F;
            g.player.exh = 0.0F;
            g.player.air = 300;
            g.player.fallDistance = 0.0F;
            g.player.vx = 0.0;
            g.player.vy = 0.0;
            g.player.vz = 0.0;
            g.player.invuln = 40;
         }
         Arrays.fill(g.inv, null);
         Arrays.fill(g.armor, null);
         g.cursor = null;
         g.deathHandled = false;
         if (targetW != null && targetW.settings != null && targetW.settings.type == WorldSettings.MURDER) {
            assignMurderMysteryRoles(g, targetW);
         }
         selectTeam(g, myTeam, false);
         if (targetW != null && targetW.settings != null) {
            if (targetW.settings.type == WorldSettings.BEDWARS) {
               stockBedwarsWorld(g, targetW);
            } else if (targetW.settings.type == WorldSettings.BATTLE) {
               stockSkywarsChests(g, targetW, false);
            } else if (targetW.settings.type == WorldSettings.TUMBLE) {
               stockTumbleArena(g, targetW);
            } else if (targetW.settings.type == WorldSettings.CS2 || targetW.settings.type == WorldSettings.MURDER) {
               targetW.ensureArenaChunksLoaded();
            }
            ensureRoundBotsSpawned(g, targetW);
         }
      }
      if (broadcast && Net319.isActive()) {
         int mapIdx = (targetW != null && targetW.settings != null) ? targetW.settings.mapId : 0;
         Net319.sendMatchControl("RESET " + mapIdx);
      }
   }

   public static void startMatch(Game g, boolean broadcast) {
      showingOverview = false;
      overviewCountdownTicks = -1;
      victoryTransitionTicks = -1;
      if (g != null && g.world != null && g.world.settings != null && myMapVote >= 0 && !Net319.isClientConnected()) {
         g.world.settings.mapId = winningVotedMap(g.world.settings.mapId);
      }
      resetArenaMap(g, g != null ? g.world : null, false);
      ensureRoundBotsSpawned(g, g != null ? g.world : null);
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
            case WorldSettings.CS2 -> {
               String tName = (myTeam & 1) == 0 ? "\u00a79Counter-Terrorists [CT]" : "\u00a7cTerrorists [T]";
               yield "\u00a76\u00a7l[CS2 Strike - " + mapName + "] \u00a7aRound Live! Team: " + tName + " \u00a77— Hold Right-Click to fire Machine Gun / Pistol, throw TNT Grenades, or Plant/Defuse C4 at Site A & B!";
            }
            case WorldSettings.MURDER -> {
               String rCol = myMurderRole == MURDER_ROLE_MURDERER ? "\u00a7c\u00a7lMURDERER" : (myMurderRole == MURDER_ROLE_SHERIFF ? "\u00a79\u00a7lSHERIFF" : "\u00a7a\u00a7lINNOCENT");
               yield "\u00a7d\u00a7l[Murder Mystery - " + mapName + "] \u00a7fYour Secret Role: " + rCol + "\u00a7f! (Weapons unlock in 5s — Innocents collect 5 Gold for a Pistol!)";
            }
            default ->
               "\u00a76\u00a7l[Battle - " + mapName + "] \u00a7aThe game has started! 15s Invulnerability active — loot the central chests!";
         };
         g.say(modeTitle);
      }
      if (broadcast && Net319.isActive()) {
         int mapIdx = (g != null && g.world != null && g.world.settings != null) ? g.world.settings.mapId : 0;
         Net319.sendMatchControl("START " + mapIdx + " " + swKit + " " + tumbleWeaponMode + " " + (soloBots ? 1 : 0));
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
         if (my != team && (breakerName == null || breakerName.equals("You"))) {
            myBedsBroken++;
         }
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
      if (this.mode == WorldSettings.CS2 || this.mode == WorldSettings.MURDER) {
         if (Math.abs(x) <= 42 && Math.abs(z) <= 42) {
            return Y_DECK;
         }
         return 0;
      }
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
      if (this.mode == WorldSettings.CS2) {
         int csTeam = Math.abs(myTeam) & 1;
         return new int[]{0, Y_DECK + 1, csTeam == 0 ? -36 : 36};
      }
      if (this.mode == WorldSettings.MURDER) {
         return new int[]{-18, Y_DECK + 1, -18};
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
         if (w.settings.type == WorldSettings.CS2) {
            int csTeam = Math.abs(peerIndex) & 1;
            int ox = ((peerIndex >> 1) & 1) == 0 ? -2 : 2;
            return new double[]{ox + 0.5, Y_DECK + 1.0, (csTeam == 0 ? -36 : 36) + 0.5};
         }
         if (w.settings.type == WorldSettings.MURDER) {
            int[][] mSpawns = new int[][]{{-18, -18}, {18, -18}, {-18, 18}, {18, 18}, {0, -24}, {0, 24}, {-24, 0}, {24, 0}};
            int[] sp = mSpawns[Math.abs(peerIndex) % mSpawns.length];
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
      } else if (this.mode == WorldSettings.CS2) {
         generateCs2(ch);
      } else if (this.mode == WorldSettings.MURDER) {
         generateMurder(ch);
      }

      loadCustomChunkIfPresent(ch);
      ch.computeHeightmap();
      return ch;
   }

   /** If a custom prebuilt map folder is active (or a matching folder in maps/ exists), overlays its saved chunk blocks. */
   private void loadCustomChunkIfPresent(Chunk ch) {
      File dir = resolvePrebuiltMapDir(this.mode, this.mapId);
      if (dir == null) {
         return;
      }
      File cf = new File(dir, "c." + ch.cx + "." + ch.cz + ".gz");
      if (!cf.isFile()) {
         return;
      }
      try (DataInputStream in = new DataInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(cf))))) {
         Chunk.readBlocks(in, ch.blocks);
      } catch (Exception ignored) {
      }
   }

   public static File resolvePrebuiltMapDir(int mode, int mapId) {
      if (customMapName != null && !customMapName.isBlank()) {
         File f1 = new File("maps/" + customMapName);
         if (f1.isDirectory()) {
            return f1;
         }
         File f2 = new File("saves/" + customMapName);
         if (f2.isDirectory()) {
            return f2;
         }
      }
      String modeSlug = mode == WorldSettings.TUMBLE ? "tumble" : (mode == WorldSettings.BEDWARS ? "bedwars" : "battle");
      String mapSlug = WorldSettings.MAP_NAMES[Math.max(0, Math.min(WorldSettings.MAP_COUNT - 1, mapId))].toLowerCase();
      File autoDir = new File("maps/" + modeSlug + "_" + mapSlug);
      if (autoDir.isDirectory()) {
         return autoDir;
      }
      return null;
   }

   public static boolean saveCustomArenaMap(World w, String name) {
      if (w == null || name == null || name.isBlank()) {
         return false;
      }
      String safe = name.trim().replaceAll("[^a-zA-Z0-9_\\-]", "_");
      File dir = new File("maps/" + safe);
      dir.mkdirs();
      int saved = 0;
      for (Chunk ch : w.chunks.values()) {
         if (ch == null) {
            continue;
         }
         File cf = new File(dir, "c." + ch.cx + "." + ch.cz + ".gz");
         try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(cf))))) {
            Chunk.writeBlocks(out, ch.blocks);
            saved++;
         } catch (Exception e) {
            return false;
         }
      }
      return saved > 0;
   }

   public static ArrayList<String> listCustomMaps() {
      ArrayList<String> res = new ArrayList<>();
      File dir = new File("maps");
      if (dir.isDirectory()) {
         File[] children = dir.listFiles();
         if (children != null) {
            Arrays.sort(children, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
            for (File f : children) {
               if (f.isDirectory()) {
                  res.add(f.getName());
               }
            }
         }
      }
      return res;
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
      // Console Edition TUMBLE Mini Game with 4 Geometrically & Architecturally Distinct Maps!
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
            int ax = Math.abs(wx);
            int az = Math.abs(wz);
            int maxC = Math.max(ax, az);
            int ring = ((int)Math.floor(d / 3.2)) & 3;
            int checker = (wx + wz) & 1;

            // 1. Boiling Lava Cauldron at Y_DECK - 14 over Bedrock floor at Y_DECK - 15 + Map-Specific Colosseum Architecture
            boolean inColosseum = this.mapId == WorldSettings.MAP_TEMPLE ? (maxC <= 21) : (d <= 22.2);
            boolean inLavaBasin = this.mapId == WorldSettings.MAP_TEMPLE ? (maxC <= 19) : (d <= 20.8);
            if (inColosseum) {
               setW(ch, wx, Y_DECK - 15, wz, Blocks.BEDROCK);
               if (inLavaBasin) {
                  setW(ch, wx, Y_DECK - 14, wz, Blocks.LAVA);
                  setW(ch, wx, Y_DECK - 13, wz, Blocks.LAVA);
               } else {
                  int wallTop = this.mapId == WorldSettings.MAP_TEMPLE ? (Y_DECK + 7 - (maxC - 20) * 2) : (Y_DECK + 5);
                  for (int y = Y_DECK - 14; y <= wallTop; y++) {
                     setW(ch, wx, y, wz, (y == Y_DECK + 1 || y == Y_DECK - 5 || y == Y_DECK - 10) ? trimMat : wallMat);
                  }
                  if (((wx * 7 + wz * 13) & 7) == 0) {
                     setW(ch, wx, wallTop + 1, wz, Blocks.GLOWSTONE);
                  }
               }
            }
            // Cavern map: subterranean stalactite cavern dome overhead
            if (this.mapId == WorldSettings.MAP_CAVERN && d <= 20.0 && ((wx * 17 + wz * 29) & 15) == 0) {
               setW(ch, wx, Y_DECK + 11, wz, Blocks.STONE);
               setW(ch, wx, Y_DECK + 10, wz, Blocks.MOSSY_COBBLE);
               setW(ch, wx, Y_DECK + 9, wz, Blocks.GLOWSTONE);
            }

            // 2. Three Destructible Tumble Layers with Distinct Geometries & Multi-Height Terraces per Map!
            switch (this.mapId) {
               case WorldSettings.MAP_CRUCIBLE -> {
                  // Crucible: Volcanic Star / Spoked Hex-Plate Geometry with Raised Caldera Rim (+1..+3) & Magma Fissure Gaps!
                  boolean onSpoke = ax <= 2 || az <= 2 || Math.abs(ax - az) <= 2;
                  boolean hasL1 = d <= 11.5 || (d <= 18.2 && onSpoke) || (d >= 15.2 && d <= 18.2);
                  boolean hasL2 = d <= 18.0 && !((ax == 6 || az == 6) && d > 8.5 && d < 14.5 && !onSpoke);
                  boolean hasL3 = d <= 17.5 && (d <= 8.5 || d >= 10.2 || onSpoke);
                  if (hasL1) {
                     int b1 = ring == 0 ? Blocks.NETHER_BRICKS : (onSpoke ? Blocks.OBSIDIAN : (checker == 0 ? Blocks.QUARTZ_ORE : Blocks.NETHERRACK));
                     int elev1 = (d >= 16.5) ? 2 : ((d >= 14.8 || (onSpoke && d >= 12.0)) ? 1 : 0);
                     setW(ch, wx, Y_DECK + elev1, wz, b1);
                     if (elev1 > 0 && d <= 15.5) {
                        setW(ch, wx, Y_DECK, wz, Blocks.NETHER_BRICKS);
                     }
                  }
                  if (hasL2) {
                     int b2 = ring == 0 ? Blocks.GLOWSTONE : (ring == 1 ? Blocks.SOUL_SAND : Blocks.COAL_BLOCK);
                     int elev2 = (d >= 11.0 && d <= 14.0 && onSpoke) ? 1 : 0;
                     setW(ch, wx, Y_DECK - 5 + elev2, wz, b2);
                  }
                  if (hasL3) {
                     int b3 = ring == 0 ? Blocks.OBSIDIAN : (checker == 0 ? Redstone319.REDSTONE_BLOCK : (Blocks.GLASS0 + 14));
                     setW(ch, wx, Y_DECK - 10, wz, b3);
                  }
               }
               case WorldSettings.MAP_CAVERN -> {
                  // Cavern: 5 Overlapping Giant Mushroom Cap Discs at 3 Heights (Y_DECK, Y_DECK+1, Y_DECK+2), Ore Quarry Ring (L2), Ice Rink (L3)!
                  boolean centerCap = d <= 9.2;
                  boolean diagCap = Math.hypot(ax - 9, az - 9) <= 6.2;
                  boolean cardCap = Math.hypot(ax - 12, az) <= 5.2 || Math.hypot(ax, az - 12) <= 5.2;
                  if (centerCap || diagCap || cardCap) {
                     int b1 = centerCap
                        ? (ring == 0 ? Blocks313.MYCELIUM : Blocks313.MUSH_BLOCK_RED)
                        : (((wx > 0) ^ (wz > 0)) ? Blocks313.MUSH_BLOCK_BROWN : Blocks313.MUSH_BLOCK_RED);
                     int capY = cardCap && !centerCap ? (Y_DECK + 2) : (diagCap && !centerCap ? (Y_DECK + 1) : Y_DECK);
                     setW(ch, wx, capY, wz, b1);
                     if (capY > Y_DECK && (ax <= 8 || az <= 8)) {
                        setW(ch, wx, Y_DECK, wz, Blocks313.MUSH_STEM);
                     }
                  }
                  if (d <= 18.2 && !(d > 9.5 && d < 11.2 && ax > 2 && az > 2)) {
                     int b2 = ring == 0 ? Blocks.DIAMOND_ORE : (ring == 1 ? Blocks.GOLD_ORE : (ring == 2 ? Blocks.IRON_ORE : Blocks.MOSSY_COBBLE));
                     int elev2 = (d >= 13.5 && d <= 16.5) ? 1 : 0;
                     setW(ch, wx, Y_DECK - 5 + elev2, wz, b2);
                  }
                  if (d <= 18.5) {
                     int b3 = ring == 0 ? Blocks313.PACKED_ICE : (checker == 0 ? Blocks313.ICE : (Blocks.GLASS0 + 11));
                     setW(ch, wx, Y_DECK - 10, wz, b3);
                  }
               }
               case WorldSettings.MAP_TEMPLE -> {
                  // Temple: Stepped Mayan Pyramid Terraces with Real Height Steps (+0, +1, +2) & Bouncy Slime Trampolines on Layer 2!
                  boolean sq1 = maxC <= 16 && (ax + az) <= 26;
                  boolean sq2 = maxC <= 17 && !((ax == 9 || az == 9) && ax > 3 && az > 3);
                  boolean sq3 = maxC <= 18;
                  if (sq1) {
                     int mRing = (maxC / 3) & 3;
                     int b1 = mRing == 0 ? Blocks.GOLD_BLOCK : (mRing == 1 ? (Blocks.WOOL0 + 14) : (mRing == 2 ? Blocks.SANDSTONE : (Blocks.WOOL0 + 4)));
                     int stepY = (maxC >= 13) ? 2 : ((maxC >= 10) ? 1 : 0);
                     setW(ch, wx, Y_DECK + stepY, wz, b1);
                     if (stepY > 0 && (ax <= 2 || az <= 2)) {
                        setW(ch, wx, Y_DECK + stepY - 1, wz, Blocks.SANDSTONE);
                     }
                  }
                  if (sq2) {
                     int b2 = (ax <= 5 && az <= 5) || ((ax & 3) == 0 && (az & 3) == 0)
                        ? Redstone319.SLIME_BLOCK
                        : (checker == 0 ? Blocks.HAY_BALE : Blocks.SANDSTONE);
                     setW(ch, wx, Y_DECK - 5, wz, b2);
                  }
                  if (sq3) {
                     int b3 = (ax == az || ax == 0 || az == 0) ? Blocks.GOLD_BLOCK : (checker == 0 ? Redstone319.SEA_LANTERN : Blocks.GLASS);
                     setW(ch, wx, Y_DECK - 10, wz, b3);
                  }
               }
               default -> {
                  // Cove: Shipwreck Wheel Spokes, Concentric Pirate Decks & Raised Fore/Aft Quarterdecks (+1..+2)!
                  boolean wheelGap = d > 9.8 && d < 12.2 && ax > 2 && az > 2 && Math.abs(ax - az) > 2;
                  if (d <= 18.5 && !wheelGap) {
                     int b1 = (wx == 0 && wz == 0) ? Blocks.GRASS : (ring == 0 ? Blocks.GRASS : (ring == 1 ? Blocks313.COARSE_DIRT : (ring == 2 ? Blocks313.PODZOL : Blocks.PLANKS)));
                     int deckElev = (d >= 15.2) ? 2 : ((d >= 12.8) ? 1 : 0);
                     setW(ch, wx, Y_DECK + deckElev, wz, b1);
                     if (deckElev > 0 && (ax <= 2 || az <= 2)) {
                        setW(ch, wx, Y_DECK + deckElev - 1, wz, Blocks.PLANKS);
                     }
                  }
                  if (d <= 18.5) {
                     int b2 = ring == 0 ? Redstone319.SEA_LANTERN : (ring == 1 ? Blocks.SANDSTONE : (ring == 2 ? Blocks313.RED_SANDSTONE : Blocks.PLANKS));
                     int b3 = ring == 0 ? Blocks.DIAMOND_ORE : (ring == 1 ? Blocks.GOLD_ORE : (ring == 2 ? Blocks.IRON_ORE : Blocks.COBBLE));
                     int elev2 = (d >= 13.0 && d <= 16.0) ? 1 : 0;
                     setW(ch, wx, Y_DECK - 5 + elev2, wz, b2);
                     setW(ch, wx, Y_DECK - 10, wz, b3);
                  }
               }
            }
            // Slime Block Jump Pads at cardinal r=7 so players & bots can launch up to elevated outer terraces!
            if ((ax == 7 && wz == 0) || (wx == 0 && az == 7)) {
               setW(ch, wx, Y_DECK, wz, Redstone319.SLIME_BLOCK);
            }
         }
      }
      // Keep central anchor & supply chest at (0, Y_DECK-2, 0) and (3, Y_DECK+1, 0)
      setW(ch, 0, Y_DECK, 0, Blocks.GRASS);
      setW(ch, 0, Y_DECK - 2, 0, Blocks.BEDROCK);
      setW(ch, 3, Y_DECK + 1, 0, Blocks.CHEST_LOOT);
   }

   private void generateSkywars(Chunk ch) {
      // Console Edition BATTLE Mini Game Arena with 4 Architecturally Distinct Maps: Cove (0), Crucible (1), Cavern (2), Temple (3)!
      int topId = switch (this.mapId) {
         case WorldSettings.MAP_CRUCIBLE -> Blocks.NETHERRACK;
         case WorldSettings.MAP_CAVERN -> Blocks.MOSSY_COBBLE;
         case WorldSettings.MAP_TEMPLE -> Blocks.SANDSTONE;
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
         case WorldSettings.MAP_TEMPLE -> Blocks.SANDSTONE;
         default -> Blocks.PLANKS;
      };

      // 1. Central Battle Courtyard / Map-Specific Centerpiece
      buildConeIsland(ch, 0, 0, 11.5, 10, topId, midId, coreId, true);
      if (this.mapId == WorldSettings.MAP_COVE) {
         // Cove: Giant Pirate Galleon Ship Deck & Masts across the center island
         for (int dz = -4; dz <= 4; dz++) {
            for (int dx = -8; dx <= 8; dx++) {
               if (Math.abs(dz) <= 3 || Math.abs(dx) <= 5) {
                  setW(ch, dx, Y_DECK, dz, Blocks.PLANKS);
                  if (Math.abs(dz) == 4 || Math.abs(dx) == 8) {
                     setW(ch, dx, Y_DECK + 1, dz, Blocks.FENCE);
                  }
               }
            }
         }
         for (int y = 1; y <= 7; y++) {
            setW(ch, -5, Y_DECK + y, 0, Blocks.LOG_OAK);
            setW(ch, 5, Y_DECK + y, 0, Blocks.LOG_OAK);
         }
         for (int sz = -3; sz <= 3; sz++) {
            setW(ch, -5, Y_DECK + 5, sz, Blocks.WOOL0);
            setW(ch, -5, Y_DECK + 6, sz, Blocks.WOOL0 + 14);
            setW(ch, 5, Y_DECK + 5, sz, Blocks.WOOL0);
            setW(ch, 5, Y_DECK + 6, sz, Blocks.WOOL0 + 14);
         }
      } else if (this.mapId == WorldSettings.MAP_CRUCIBLE) {
         // Crucible: Nether Citadel Fortress with 4 Obsidian Corner Battlement Towers & Lava Channels
         int[][] towers = new int[][]{{-6, -6}, {6, -6}, {-6, 6}, {6, 6}};
         for (int[] t : towers) {
            for (int y = 1; y <= 5; y++) {
               setW(ch, t[0], Y_DECK + y, t[1], y == 5 ? Blocks.GLOWSTONE : Blocks.NETHER_BRICKS);
               setW(ch, t[0] + (t[0] < 0 ? 1 : -1), Y_DECK + y, t[1], Blocks.OBSIDIAN);
            }
         }
      } else if (this.mapId == WorldSettings.MAP_CAVERN) {
         // Cavern: Central Glowing Crystal Geode & Upper Mushroom Canopy Ring
         for (int ang = 0; ang < 8; ang++) {
            int cx = (int)Math.round(Math.cos(ang * Math.PI / 4.0) * 7.0);
            int cz = (int)Math.round(Math.sin(ang * Math.PI / 4.0) * 7.0);
            for (int y = 1; y <= 4; y++) {
               setW(ch, cx, Y_DECK + y, cz, y == 4 ? Blocks313.PACKED_ICE : Blocks.MOSSY_COBBLE);
            }
         }
      } else if (this.mapId == WorldSettings.MAP_TEMPLE) {
         // Temple: Stepped Aztec Ziggurat Terrace around the Central Shrine
         for (int dz = -7; dz <= 7; dz++) {
            for (int dx = -7; dx <= 7; dx++) {
               int distM = Math.max(Math.abs(dx), Math.abs(dz));
               if (distM >= 4 && distM <= 7 && Math.abs(dx) > 1 && Math.abs(dz) > 1) {
                  int stepH = 8 - distM;
                  for (int y = 1; y <= stepH; y++) {
                     setW(ch, dx, Y_DECK + y, dz, y == stepH ? Blocks.GOLD_BLOCK : Blocks.SANDSTONE);
                  }
               }
            }
         }
      }

      // Central Feast Shrine (kept consistent across all maps for gameplay & test invariants)
      for (int dz = -2; dz <= 2; dz++) {
         for (int dx = -2; dx <= 2; dx++) {
            if (Math.abs(dx) == 2 && Math.abs(dz) == 2) {
               for (int y = 1; y <= 5; y++) {
                  setW(ch, dx, Y_DECK + y, dz, bridgeId);
               }
               setW(ch, dx, Y_DECK + 6, dz, Blocks.GLOWSTONE);
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

      // 1b. Central 2nd-Story Sky-Sanctum (Y_DECK + 6) & Subterranean Undercroft Crypt (Y_DECK - 4) + Slime Jump Pads!
      for (int dz = -6; dz <= 6; dz++) {
         for (int dx = -6; dx <= 6; dx++) {
            double r2 = Math.hypot(dx, dz);
            // Upper Sky-Sanctum at Y_DECK + 6 with corner jump-pad openings at (±5, ±5)
            if (r2 <= 6.2 && !(Math.abs(dx) >= 4 && Math.abs(dz) >= 4)) {
               setW(ch, dx, Y_DECK + 6, dz, (dx == 0 && dz == 0) ? Blocks.GOLD_BLOCK : bridgeId);
               if (r2 >= 5.2 && ((dx + dz) & 1) == 0) {
                  setW(ch, dx, Y_DECK + 7, dz, coreId);
               }
            }
            // Subterranean Undercroft Crypt hollowed out at Y_DECK - 4..Y_DECK - 2 beneath the feast shrine
            if (r2 <= 5.2 && !(dx == 0 && dz == 0)) {
               setW(ch, dx, Y_DECK - 5, dz, coreId);
               setW(ch, dx, Y_DECK - 4, dz, 0);
               setW(ch, dx, Y_DECK - 3, dz, 0);
               setW(ch, dx, Y_DECK - 2, dz, 0);
            }
         }
      }
      // 4 Slime Block Jump Pads at (±5, Y_DECK, ±5) launching players & bots straight to the Upper Sky-Sanctum!
      for (int sx = -5; sx <= 5; sx += 10) {
         for (int sz = -5; sz <= 5; sz += 10) {
            setW(ch, sx, Y_DECK, sz, Redstone319.SLIME_BLOCK);
         }
      }
      // Stepped ramparts climbing from Y_DECK + 1 up to Y_DECK + 6 on the North/South/East/West flanks of the center island
      for (int step = 1; step <= 5; step++) {
         setW(ch, -8 + step, Y_DECK + step, -4, bridgeId);
         setW(ch, 8 - step, Y_DECK + step, 4, bridgeId);
      }

      // 2. Map-Specific Arched Walkways, Multi-Elevation Bridges, Sunken Lagoons & Environmental Architecture
      int minX = ch.cx * 16;
      int minZ = ch.cz * 16;
      for (int lz = 0; lz < 16; lz++) {
         int wz = minZ + lz;
         for (int lx = 0; lx < 16; lx++) {
            int wx = minX + lx;
            double d = Math.hypot(wx, wz);
            int ax = Math.abs(wx);
            int az = Math.abs(wz);
            boolean cardinalBridge = (ax <= 1 && az >= 10 && az <= 38) || (az <= 1 && ax >= 10 && ax <= 38);
            boolean diagBridge = Math.abs(ax - az) <= 1 && ax >= 8 && ax <= 27;
            boolean ringWalkway = this.mapId == WorldSettings.MAP_TEMPLE
               ? (Math.max(ax, az) >= 22 && Math.max(ax, az) <= 25)
               : (d >= 22.5 && d <= 25.2);
            if (cardinalBridge || diagBridge || ringWalkway) {
               // Arched elevation profile along bridges (rises +1..+2 blocks over mid-spans for dynamic vertical combat!)
               int span = Math.max(ax, az);
               int archY = (span >= 14 && span <= 19) ? (span >= 16 && span <= 17 ? 2 : 1)
                  : ((span >= 31 && span <= 35) ? 1 : 0);
               if (ringWalkway && !cardinalBridge && !diagBridge) {
                  archY = 1;
               }
               setW(ch, wx, Y_DECK - 1 + archY, wz, bridgeId);
               if (archY > 0) {
                  setW(ch, wx, Y_DECK - 1, wz, bridgeId);
               }
               if (this.mapId == WorldSettings.MAP_CRUCIBLE && ((wx + wz) & 3) == 0) {
                  setW(ch, wx, Y_DECK - 1 + archY, wz, Blocks.OBSIDIAN);
               } else if (this.mapId == WorldSettings.MAP_TEMPLE && ((wx + wz) & 5) == 0) {
                  setW(ch, wx, Y_DECK - 1 + archY, wz, Blocks.MOSSY_COBBLE);
               }
            }
            // Map-specific lower basins & upper canopies
            if (this.mapId == WorldSettings.MAP_COVE && d >= 11.5 && d <= 22.0 && !cardinalBridge && !diagBridge) {
               // Sunken Pirate Lagoon with Coral/Sea-Lantern Reefs below the boardwalks
               setW(ch, wx, Y_DECK - 5, wz, ((wx + wz) & 7) == 0 ? Redstone319.SEA_LANTERN : Blocks.SAND);
               setW(ch, wx, Y_DECK - 4, wz, Blocks.WATER);
            } else if (this.mapId == WorldSettings.MAP_CRUCIBLE && d >= 12.0 && d <= 21.5 && !cardinalBridge && !diagBridge) {
               // Boiling Nether Lava Moat
               setW(ch, wx, Y_DECK - 5, wz, Blocks.NETHER_BRICKS);
               setW(ch, wx, Y_DECK - 4, wz, Blocks.LAVA);
            } else if (this.mapId == WorldSettings.MAP_CAVERN && d <= 38.0) {
               // Subterranean Cavern Ceiling Dome & Stalactites
               int domeY = Y_DECK + 13 - (int)(d * 0.12);
               if (((wx * 19 + wz * 31) & 7) == 0) {
                  setW(ch, wx, domeY, wz, Blocks.STONE);
                  if (((wx + wz) & 15) == 0) {
                     setW(ch, wx, domeY - 1, wz, Blocks.GLOWSTONE);
                  }
               }
            } else if (this.mapId == WorldSettings.MAP_TEMPLE && (ax == 14 || az == 14) && ax <= 14 && az <= 14 && ((wx + wz) & 3) == 0) {
               // Ancient Jungle Ruin Arches
               setW(ch, wx, Y_DECK, wz, Blocks.MOSSY_COBBLE);
               setW(ch, wx, Y_DECK + 1, wz, Blocks.SANDSTONE);
            }
         }
      }

      // 3. 4 Mid-Ring 2-Story Watchtowers & Chests (Lower Deck at Y_DECK + Upper Sniper Deck at Y_DECK + 4!)
      for (int[] m : SKYWARS_MIDS) {
         buildConeIsland(ch, m[0], m[1], 4.8, 6, topId, midId, coreId, true);
         setW(ch, m[0], Y_DECK + 1, m[1], Blocks.CHEST_LOOT);
         setW(ch, m[0] - 2, Y_DECK, m[1], Redstone319.SLIME_BLOCK);
         for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
               if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) {
                  setW(ch, m[0] + dx, Y_DECK + 4, m[1] + dz, bridgeId);
               }
            }
         }
         // Steps up to the Mid-Ring Upper Sniper Deck
         setW(ch, m[0] - 2, Y_DECK + 1, m[1] - 1, bridgeId);
         setW(ch, m[0] - 1, Y_DECK + 2, m[1] - 2, bridgeId);
         setW(ch, m[0], Y_DECK + 3, m[1] - 2, bridgeId);
         if (this.mapId == WorldSettings.MAP_COVE) {
            for (int y = 1; y <= 5; y++) {
               setW(ch, m[0] + 1, Y_DECK + y, m[1] + 1, Blocks.LOG_OAK);
            }
            for (int sx = -1; sx <= 1; sx++) {
               setW(ch, m[0] + 1 + sx, Y_DECK + 4, m[1] + 1, Blocks.WOOL0);
               setW(ch, m[0] + 1 + sx, Y_DECK + 3, m[1] + 1, Blocks.WOOL0 + 14);
            }
         } else if (this.mapId == WorldSettings.MAP_CRUCIBLE) {
            for (int y = 1; y <= 4; y++) {
               setW(ch, m[0] + 1, Y_DECK + y, m[1] + 1, Blocks.NETHER_BRICKS);
            }
            setW(ch, m[0] + 1, Y_DECK + 5, m[1] + 1, Blocks.GLOWSTONE);
         } else if (this.mapId == WorldSettings.MAP_CAVERN) {
            for (int y = 1; y <= 3; y++) {
               setW(ch, m[0] + 1, Y_DECK + y, m[1] + 1, Blocks313.MUSH_STEM);
            }
            for (int dx = -1; dx <= 1; dx++) {
               for (int dz = -1; dz <= 1; dz++) {
                  setW(ch, m[0] + 1 + dx, Y_DECK + 4, m[1] + 1 + dz, Blocks313.MUSH_BLOCK_RED);
               }
            }
         } else if (this.mapId == WorldSettings.MAP_TEMPLE) {
            for (int y = 1; y <= 4; y++) {
               setW(ch, m[0] + 1, Y_DECK + y, m[1] + 1, Blocks.SANDSTONE);
            }
            setW(ch, m[0] + 1, Y_DECK + 5, m[1] + 1, Blocks.GOLD_BLOCK);
         }
      }

      // 4. 8 Player Spawn Podiums with Raised Rear Cliff Terraces (+2..+3)
      for (int[] sp : SKYWARS_SPAWNS) {
         int sx = sp[0];
         int sz = sp[1];
         buildConeIsland(ch, sx, sz, 4.6, 7, topId, midId, coreId, true);
         int outX = Integer.compare(sx, 0);
         int outZ = Integer.compare(sz, 0);
         for (int s = -1; s <= 1; s++) {
            int rx = sx + outX * 2 + (outX == 0 ? s : 0);
            int rz = sz + outZ * 2 + (outZ == 0 ? s : 0);
            setW(ch, rx, Y_DECK + 1, rz, topId);
            setW(ch, rx + outX, Y_DECK + 1, rz + outZ, midId);
            setW(ch, rx + outX, Y_DECK + 2, rz + outZ, topId);
         }
         if (this.mapId == WorldSettings.MAP_COVE || this.mapId == WorldSettings.MAP_TEMPLE) {
            buildSmallOak(ch, sx - 2, Y_DECK + 1, sz - 1);
         } else if (this.mapId == WorldSettings.MAP_CRUCIBLE) {
            setW(ch, sx - 2, Y_DECK + 1, sz - 1, Blocks.NETHER_BRICKS);
            setW(ch, sx - 2, Y_DECK + 2, sz - 1, Blocks.GLOWSTONE);
         } else if (this.mapId == WorldSettings.MAP_CAVERN) {
            setW(ch, sx - 2, Y_DECK + 1, sz - 1, Blocks313.MUSH_STEM);
            setW(ch, sx - 2, Y_DECK + 2, sz - 1, Blocks313.MUSH_BLOCK_BROWN);
         }
         setW(ch, sx, Y_DECK, sz, Blocks.GOLD_BLOCK);
         setW(ch, sx + 2, Y_DECK + 1, sz, Blocks.CHEST_LOOT);
         setW(ch, sx + 2, Y_DECK + 1, sz + 1, Blocks.CRAFTING_TABLE);
         setW(ch, sx + 1, Y_DECK + 1, sz + 2, Blocks.FURNACE);
      }
   }

   private void generateBedwars(Chunk ch) {
      // BEDWARS with 4 Architecturally Distinct Multi-Level Maps: Cove (0), Crucible (1), Cavern (2), Temple (3)!
      int centerTop = switch (this.mapId) {
         case WorldSettings.MAP_CRUCIBLE -> Blocks.NETHER_BRICKS;
         case WorldSettings.MAP_CAVERN -> Blocks.MOSSY_COBBLE;
         case WorldSettings.MAP_TEMPLE -> Blocks.SANDSTONE;
         default -> Blocks.STONEBRICK;
      };
      int centerMid = switch (this.mapId) {
         case WorldSettings.MAP_CRUCIBLE -> Blocks.OBSIDIAN;
         case WorldSettings.MAP_CAVERN -> Blocks.STONE;
         case WorldSettings.MAP_TEMPLE -> Blocks.SANDSTONE;
         default -> Blocks.STONE;
      };
      int baseFloor = switch (this.mapId) {
         case WorldSettings.MAP_CRUCIBLE -> Blocks.NETHER_BRICKS;
         case WorldSettings.MAP_CAVERN -> Blocks.MOSSY_COBBLE;
         case WorldSettings.MAP_TEMPLE -> Blocks.SANDSTONE;
         default -> Blocks.STONEBRICK;
      };

      // 1. Central Emerald Generator Arena (radius 10.5) + Upper Sky-Vault (Y_DECK + 6, r <= 5.5) + Under-Deck Catwalk (Y_DECK - 4)!
      buildConeIsland(ch, 0, 0, 10.5, 9, centerTop, centerMid, Blocks.COBBLE, false);
      for (int dz = -2; dz <= 2; dz++) {
         for (int dx = -2; dx <= 2; dx++) {
            setW(ch, dx, Y_DECK, dz, (dx == 0 && dz == 0) ? Blocks.EMERALD_BLOCK : Blocks.QUARTZ_BLOCK);
         }
      }
      for (int dz = -5; dz <= 5; dz++) {
         for (int dx = -5; dx <= 5; dx++) {
            double r = Math.hypot(dx, dz);
            if (r <= 5.0 && !(Math.abs(dx) >= 3 && Math.abs(dz) >= 3)) {
               setW(ch, dx, Y_DECK + 6, dz, (dx == 0 && dz == 0) ? Blocks.EMERALD_BLOCK : centerTop);
            }
            if (r <= 4.5 && !(dx == 0 && dz == 0)) {
               setW(ch, dx, Y_DECK - 5, dz, centerMid);
               setW(ch, dx, Y_DECK - 4, dz, 0);
               setW(ch, dx, Y_DECK - 3, dz, 0);
               setW(ch, dx, Y_DECK - 2, dz, 0);
            }
         }
      }
      // 4 Slime Jump Pads on the Center Island to bounce up to the Upper Sky-Vault (Y_DECK + 6)!
      setW(ch, -3, Y_DECK, -3, Redstone319.SLIME_BLOCK);
      setW(ch, 3, Y_DECK, -3, Redstone319.SLIME_BLOCK);
      setW(ch, -3, Y_DECK, 3, Redstone319.SLIME_BLOCK);
      setW(ch, 3, Y_DECK, 3, Redstone319.SLIME_BLOCK);
      setW(ch, -4, Y_DECK, 0, Blocks.EMERALD_BLOCK);
      setW(ch, -4, Y_DECK + 3, 0, Blocks.GLOWSTONE);
      setW(ch, 4, Y_DECK, 0, Blocks.EMERALD_BLOCK);
      setW(ch, 4, Y_DECK + 3, 0, Blocks.GLOWSTONE);
      setW(ch, 0, Y_DECK + 1, 0, Blocks.CHEST_LOOT);

      if (this.mapId == WorldSettings.MAP_CRUCIBLE) {
         int[][] pillars = new int[][]{{-6, -6}, {6, -6}, {-6, 6}, {6, 6}};
         for (int[] p : pillars) {
            for (int y = 1; y <= 4; y++) {
               setW(ch, p[0], Y_DECK + y, p[1], y == 4 ? Blocks.GLOWSTONE : Blocks.OBSIDIAN);
            }
         }
      } else if (this.mapId == WorldSettings.MAP_CAVERN) {
         int[][] crystals = new int[][]{{-5, -5}, {5, -5}, {-5, 5}, {5, 5}};
         for (int[] c : crystals) {
            for (int y = 1; y <= 3; y++) {
               setW(ch, c[0], Y_DECK + y, c[1], Blocks313.PACKED_ICE);
            }
            setW(ch, c[0], Y_DECK + 4, c[1], Blocks.GLOWSTONE);
         }
      } else if (this.mapId == WorldSettings.MAP_TEMPLE) {
         int[][] obelisks = new int[][]{{-6, -6}, {6, -6}, {-6, 6}, {6, 6}};
         for (int[] ob : obelisks) {
            for (int y = 1; y <= 4; y++) {
               setW(ch, ob[0], Y_DECK + y, ob[1], y == 4 ? Blocks.GOLD_BLOCK : Blocks.SANDSTONE);
            }
         }
      }

      // 2. 4 Diamond Generator Islands with Raised Outer Overlook Terraces (+2)
      for (int[] d : BEDWARS_DIAMONDS) {
         buildConeIsland(ch, d[0], d[1], 4.4, 5, centerTop, centerMid, Blocks.COBBLE, false);
         setW(ch, d[0], Y_DECK, d[1], Blocks.DIAMOND_BLOCK);
         setW(ch, d[0], Y_DECK + 3, d[1], Blocks.GLOWSTONE);
         int ox = Integer.compare(d[0], 0);
         int oz = Integer.compare(d[1], 0);
         setW(ch, d[0] + ox * 2, Y_DECK + 1, d[1] + oz, centerTop);
         setW(ch, d[0] + ox * 2, Y_DECK + 2, d[1] + oz * 2, centerTop);
         if (this.mapId == WorldSettings.MAP_CRUCIBLE) {
            setW(ch, d[0] + 2, Y_DECK + 1, d[1] + 2, Blocks.NETHER_BRICKS);
            setW(ch, d[0] + 2, Y_DECK + 2, d[1] + 2, Blocks.GLOWSTONE);
         } else if (this.mapId == WorldSettings.MAP_CAVERN) {
            setW(ch, d[0] + 2, Y_DECK + 1, d[1] + 2, Blocks313.MUSH_STEM);
            setW(ch, d[0] + 2, Y_DECK + 2, d[1] + 2, Blocks313.MUSH_BLOCK_RED);
         } else if (this.mapId == WorldSettings.MAP_TEMPLE) {
            setW(ch, d[0] + 2, Y_DECK + 1, d[1] + 2, Blocks.SANDSTONE);
            setW(ch, d[0] + 2, Y_DECK + 2, d[1] + 2, Blocks.GOLD_BLOCK);
         }
      }

      // 3. 4 Symmetrical 2-Story Team Base Castles (North=Red, South=Blue, West=Green, East=Yellow)
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
               setW(ch, bx + dx, Y_DECK - 2, bz + dz, centerMid);
               setW(ch, bx + dx, Y_DECK - 1, bz + dz, baseFloor);
               boolean border = Math.abs(dx) == hw || Math.abs(dz) == hd;
               setW(ch, bx + dx, Y_DECK, bz + dz, border ? wool : baseFloor);
            }
         }
         // 2nd-Story Castle Keep Roof (Y_DECK + 5) & Back Battlement Wall + Side Staircases!
         for (int s = -3; s <= 3; s++) {
            for (int back = 3; back <= 5; back++) {
               int rx = bx - dirX * back + (dirX == 0 ? s : 0);
               int rz = bz - dirZ * back + (dirZ == 0 ? s : 0);
               setW(ch, rx, Y_DECK + 5, rz, Math.abs(s) == 3 ? wool : baseFloor);
               if (back == 5 && (Math.abs(s) == 3 || s == 0)) {
                  for (int y = 1; y <= 7; y++) {
                     setW(ch, rx, Y_DECK + y, rz, y == 7 ? Blocks.GLOWSTONE : (Math.abs(s) == 3 ? wool : baseFloor));
                  }
               }
            }
         }
         // Side Staircase climbing from Y_DECK + 1 up to the Castle Roof at Y_DECK + 5
         for (int step = 1; step <= 4; step++) {
            int stX = bx - dirX * step + (dirX == 0 ? 4 : 0);
            int stZ = bz - dirZ * step + (dirZ == 0 ? 4 : 0);
            setW(ch, stX, Y_DECK + step, stZ, baseFloor);
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

   private void generateCs2(Chunk ch) {
      int minX = ch.cx * 16;
      int minZ = ch.cz * 16;
      if (minX > 48 || minX + 15 < -48 || minZ > 48 || minZ + 15 < -48) {
         return;
      }
      int floorMat = switch (this.mapId) {
         case WorldSettings.MAP_CRUCIBLE -> Blocks.STONEBRICK;
         case WorldSettings.MAP_CAVERN -> Redstone319.POLISHED_ANDESITE;
         case WorldSettings.MAP_TEMPLE -> Blocks.MOSSY_COBBLE;
         default -> Blocks.SANDSTONE;
      };
      int wallMat = switch (this.mapId) {
         case WorldSettings.MAP_CRUCIBLE -> Blocks.BRICKS;
         case WorldSettings.MAP_CAVERN -> Redstone319.MOSSY_STONEBRICK;
         case WorldSettings.MAP_TEMPLE -> Redstone319.CHISELED_SANDSTONE;
         default -> Redstone319.SMOOTH_SANDSTONE;
      };
      int crateMat = this.mapId == WorldSettings.MAP_CRUCIBLE ? Blocks.NETHER_BRICKS : Blocks.PLANKS;

      for (int lz = 0; lz < 16; lz++) {
         int wz = minZ + lz;
         for (int lx = 0; lx < 16; lx++) {
            int wx = minX + lx;
            int ax = Math.abs(wx);
            int az = Math.abs(wz);
            if (ax > 40 || az > 40) {
               continue;
            }
            setW(ch, wx, Y_DECK - 1, wz, Blocks.BEDROCK);
            int ground = floorMat;
            // CT Spawn (North) & T Spawn (South) markings
            if (az >= 32 && ax <= 8) {
               ground = wz < 0 ? (Blocks.CONCRETE0 + 11) : (Blocks.CONCRETE0 + 14);
            }
            // Bomb Site A (-22, 0) & Bomb Site B (+22, 0) target pads
            double dSiteA = Math.hypot(wx - (-22), wz);
            double dSiteB = Math.hypot(wx - 22, wz);
            if (dSiteA <= 3.5 || dSiteB <= 3.5) {
               ground = (dSiteA <= 1.2 || dSiteB <= 1.2) ? Blocks.GOLD_BLOCK : Redstone319.REDSTONE_BLOCK;
            }
            // Slime Jump Pads to reach 2nd-story Catwalk (Y_DECK + 5)
            if ((ax == 8 && az == 8) || (ax == 22 && az == 12)) {
               ground = Redstone319.SLIME_BLOCK;
            }
            setW(ch, wx, Y_DECK, wz, ground);

            // Outer Arena Perimeter Wall (height 6)
            if (ax == 40 || az == 40) {
               for (int y = 1; y <= 6; y++) {
                  setW(ch, wx, Y_DECK + y, wz, y == 6 ? Blocks.GLOWSTONE : wallMat);
               }
               continue;
            }

            // Spawn Divider Walls with Side Doors
            if (az == 29 && ax <= 10 && ax >= 3 && !(ax >= 5 && ax <= 7)) {
               for (int y = 1; y <= 4; y++) {
                  setW(ch, wx, Y_DECK + y, wz, wallMat);
               }
            }
            // Tactical Mid Divider Walls & Sniper Windows
            if (ax == 11 && az <= 20 && !(az >= 3 && az <= 6) && !(az >= 14 && az <= 17)) {
               for (int y = 1; y <= 4; y++) {
                  if (y == 2 && (az == 0 || az == 10)) {
                     continue; // Sniper peek window!
                  }
                  setW(ch, wx, Y_DECK + y, wz, wallMat);
               }
            }

            // Tactical Cover Crates in Mid & Bomb Sites
            if ((ax == 3 && az == 12) || (ax == 20 && az == 4) || (ax == 25 && az == 3) || (ax == 15 && az == 16)) {
               setW(ch, wx, Y_DECK + 1, wz, crateMat);
               setW(ch, wx, Y_DECK + 2, wz, crateMat);
            }

            // 2nd-Story Tactical Catwalk & Heaven Overlook at Y_DECK + 5
            boolean onCatwalk = (az <= 3 && ax <= 24) || (ax >= 16 && ax <= 20 && az <= 16);
            if (onCatwalk && dSiteA > 2.5 && dSiteB > 2.5) {
               setW(ch, wx, Y_DECK + 5, wz, floorMat);
            }
         }
      }
      // Staircases up to the Y_DECK + 5 Catwalk
      for (int s = 1; s <= 4; s++) {
         setW(ch, -16, Y_DECK + s, -16 - s, floorMat);
         setW(ch, 16, Y_DECK + s, 16 + s, floorMat);
      }
   }

   private void generateMurder(Chunk ch) {
      int minX = ch.cx * 16;
      int minZ = ch.cz * 16;
      if (minX > 48 || minX + 15 < -48 || minZ > 48 || minZ + 15 < -48) {
         return;
      }
      int floorMat = switch (this.mapId) {
         case WorldSettings.MAP_CRUCIBLE -> Blocks.NETHER_BRICKS;
         case WorldSettings.MAP_CAVERN -> Redstone319.POLISHED_DIORITE;
         case WorldSettings.MAP_TEMPLE -> Blocks.SANDSTONE;
         default -> Blocks.PLANKS;
      };
      int wallMat = switch (this.mapId) {
         case WorldSettings.MAP_CRUCIBLE -> Blocks.OBSIDIAN;
         case WorldSettings.MAP_CAVERN -> Blocks.STONEBRICK;
         case WorldSettings.MAP_TEMPLE -> Redstone319.CHISELED_SANDSTONE;
         default -> Blocks.QUARTZ_BLOCK;
      };

      for (int lz = 0; lz < 16; lz++) {
         int wz = minZ + lz;
         for (int lx = 0; lx < 16; lx++) {
            int wx = minX + lx;
            int ax = Math.abs(wx);
            int az = Math.abs(wz);
            if (ax > 34 || az > 34) {
               continue;
            }
            setW(ch, wx, Y_DECK - 1, wz, Blocks.BEDROCK);
            boolean carpet = (ax <= 3 || az <= 3) && ax <= 30 && az <= 30;
            int ground = carpet ? (Blocks.WOOL0 + 14) : floorMat;
            if ((ax == 10 && az == 10)) {
               ground = Redstone319.SLIME_BLOCK; // Bounce pads to 2nd floor Mezzanine!
            }
            setW(ch, wx, Y_DECK, wz, ground);

            // Outer Mansion Walls (height 8)
            if (ax == 34 || az == 34) {
               for (int y = 1; y <= 8; y++) {
                  setW(ch, wx, Y_DECK + y, wz, y == 4 || y == 8 ? Blocks.GLOWSTONE : wallMat);
               }
               continue;
            }

            // Room Partition Walls with Wide Doorways (creates rooms, corridors & hiding spots!)
            boolean roomWall = (ax == 14 && az >= 6 && az <= 30 && !(az >= 11 && az <= 15) && !(az >= 22 && az <= 25))
               || (az == 14 && ax >= 6 && ax <= 30 && !(ax >= 11 && ax <= 15) && !(ax >= 22 && ax <= 25));
            if (roomWall) {
               for (int y = 1; y <= 4; y++) {
                  setW(ch, wx, Y_DECK + y, wz, wallMat);
               }
            }

            // Grand Hall Pillars (for breaking line of sight from the Murderer!)
            if ((ax == 6 || ax == 22) && (az == 6 || az == 22)) {
               for (int y = 1; y <= 5; y++) {
                  setW(ch, wx, Y_DECK + y, wz, y == 5 ? Blocks.GLOWSTONE : wallMat);
               }
            }

            // 2nd-Story Mezzanine Balcony at Y_DECK + 5 around the outer gallery & central skybridge
            boolean mezzanine = (ax >= 24 && ax <= 33) || (az >= 24 && az <= 33) || (ax <= 2 && az <= 24);
            if (mezzanine) {
               setW(ch, wx, Y_DECK + 5, wz, floorMat);
            }
         }
      }
      // Central Monument at (0, Y_DECK + 1..3, 0)
      setW(ch, 0, Y_DECK + 1, 0, Blocks.GOLD_BLOCK);
      setW(ch, 0, Y_DECK + 2, 0, Blocks.GLOWSTONE);
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

   private static void initBotInventory(int bSlot, int blocks, int arrows, int grenades, int apples, int weaponItem, int rangedItem, int murderRole) {
      botHealsUsed[bSlot] = 0;
      botPearlCooldown[bSlot] = 0;
      botGearTier[bSlot] = 0;
      botResources[bSlot] = 0;
      botHoldingBlockTicks[bSlot] = 0;
      botEatTicks[bSlot] = 0;
      botBowDrawTicks[bSlot] = 0;
      botBlocks[bSlot] = Math.max(0, blocks);
      botArrows[bSlot] = Math.max(0, arrows);
      botGrenades[bSlot] = Math.max(0, grenades);
      botApples[bSlot] = Math.max(0, apples);
      botWeaponItem[bSlot] = weaponItem;
      botRangedItem[bSlot] = rangedItem;
      botGold[bSlot] = 0;
      botMurderRole[bSlot] = murderRole;
      botBuildUpCooldown[bSlot] = 0;
   }

   private static void spawnBattleBotAtPodium(Game g, World w, int i) {
      if (g == null || g.mobs == null || w == null || i <= 0 || i >= SKYWARS_SPAWNS.length) {
         return;
      }
      int[] sp = SKYWARS_SPAWNS[i];
      w.ensureChunkLoaded(sp[0] >> 4, sp[1] >> 4);
      if (w.getBlock(sp[0], Y_DECK, sp[1]) == 0) {
         w.setBlock(sp[0], Y_DECK, sp[1], Blocks.GOLD_BLOCK);
      }
      int mobType = (i % 3 == 0) ? Mobs.SKELETON : Mobs.ZOMBIE;
      Mobs.Mob bot = g.mobs.spawn(mobType, sp[0] + 0.5, Y_DECK + 1.0, sp[1] + 0.5);
      if (bot != null) {
         String bName = BOT_NAMES[i % BOT_NAMES.length];
         bot.teamTag = "BattleBot:" + i + ":" + bName;
         bot.variant = 0;
         int bSlot = Math.abs(bot.netId) & 31;
         int kit = (i + swKit) & 3;
         if (kit == 0) {
            initBotInventory(bSlot, 16, 0, 0, 0, Items.toolId(1, Items.C_SWORD), 0, 0);
         } else if (kit == 1) {
            initBotInventory(bSlot, 16, 0, 0, 1, Items.toolId(2, Items.C_SWORD), 0, 0);
         } else if (kit == 2) {
            initBotInventory(bSlot, 16, 16, 0, 0, Items.toolId(0, Items.C_SWORD), Items.BOW, 0);
         } else {
            initBotInventory(bSlot, 16, 0, 2, 0, Items.toolId(2, Items.C_AXE), 0, 0);
         }
      }
   }

   /**
    * Ensures all initial AI competitors are spawned at the start of every round across Tumble, Battle, Bedwars, CS2, and Murder Mystery,
    * even if chests were already stocked or if the player switched teams/settings in the waiting lobby!
    */
   public static void ensureRoundBotsSpawned(Game g, World w) {
      if (g == null || g.mobs == null || w == null || w.settings == null || !soloBots || Net319.isClientConnected()) {
         return;
      }
      w.ensureArenaChunksLoaded();
      int type = w.settings.type;
      if (type == WorldSettings.TUMBLE) {
         if (!tumbleSpawnedBots || (w.tickCount < 20L && countHostiles(g) == 0)) {
            tumbleSpawnedBots = true;
            int wm = Math.abs(tumbleWeaponMode) % 3;
            for (int i = 1; i < 6; i++) {
               double ang = i * (2.0 * Math.PI / 6.0);
               double bx = Math.cos(ang) * 11.0 + 0.5;
               double bz = Math.sin(ang) * 11.0 + 0.5;
               Mobs.Mob bot = g.mobs.spawn(i % 2 == 0 ? Mobs.SKELETON : Mobs.ZOMBIE, bx, Y_DECK + 1.0, bz);
               if (bot != null) {
                  String bName = BOT_NAMES[i % BOT_NAMES.length];
                  bot.teamTag = "TumbleBot:" + i + ":" + bName;
                  bot.variant = i;
                  int bSlot = Math.abs(bot.netId) & 31;
                  initBotInventory(bSlot, 0, wm == 1 ? 0 : 16, 0, 0, wm == 2 ? 0 : Items.toolId(4, Items.C_SHOVEL), 0, 0);
               }
            }
         }
      } else if (type == WorldSettings.BATTLE) {
         if (w.tickCount < 20L && countHostiles(g) == 0) {
            Arrays.fill(swBotSpawned, false);
         }
         int myPodium = Math.abs(getPlayerTeam(g)) % SKYWARS_SPAWNS.length;
         for (int i = 1; i < SKYWARS_SPAWNS.length; i++) {
            if (!swBotSpawned[i] && i != myPodium && !Net319.isTeamOccupiedByPeer(i)) {
               swBotSpawned[i] = true;
               spawnBattleBotAtPodium(g, w, i);
            }
         }
      } else if (type == WorldSettings.BEDWARS) {
         int myTeamIdx = getPlayerTeam(g);
         for (int t = 0; t < 4; t++) {
            if (t == myTeamIdx || Net319.isTeamOccupiedByPeer(t) || !isTeamBedIntact(w, t)) {
               continue;
            }
            for (int role = 0; role <= 1; role++) {
               int slot = t * 2 + role;
               if (!bwBotInitialSpawned[slot]) {
                  bwBotInitialSpawned[slot] = true;
                  bwBotRespawnTicks[slot] = 0;
                  spawnBedwarsTeamBot(g, t, role);
               }
            }
         }
      } else if (type == WorldSettings.CS2) {
         if (!cs2BotsSpawned || (w.tickCount < 20L && countHostiles(g) == 0)) {
            cs2BotsSpawned = true;
            int myCsTeam = getPlayerTeam(g) & 1; // 0 = CT, 1 = T
            // Spawn 3 Ally Bots & 4 Enemy Bots across CT (North) and T (South)
            for (int csTeam = 0; csTeam <= 1; csTeam++) {
               int count = (csTeam == myCsTeam) ? 3 : 4;
               for (int idx = 0; idx < count; idx++) {
                  double bx = -6.0 + idx * 4.0 + 0.5;
                  double bz = (csTeam == 0 ? -36.0 : 36.0) + 0.5;
                  Mobs.Mob bot = g.mobs.spawn(idx % 2 == 0 ? Mobs.ZOMBIE : Mobs.SKELETON, bx, Y_DECK + 1.0, bz);
                  if (bot != null) {
                     String bName = BOT_NAMES[(csTeam * 4 + idx) % BOT_NAMES.length];
                     String tName = csTeam == 0 ? "Blue" : "Red"; // Blue = CT, Red = T
                     bot.teamTag = "CS2Bot:" + tName + ":" + idx + ":" + bName;
                     bot.variant = idx & 1;
                     int bSlot = Math.abs(bot.netId) & 31;
                     int gun = (idx == 1) ? Redstone319.SNIPER_RIFLE : ((idx == 3) ? Redstone319.PISTOL : Redstone319.MACHINE_GUN);
                     initBotInventory(bSlot, 16, 48, 2, 1, Redstone319.MURDER_KNIFE, gun, 0);
                  }
               }
            }
         }
      } else if (type == WorldSettings.MURDER) {
         if (!murderBotsSpawned || (w.tickCount < 20L && countHostiles(g) == 0)) {
            murderBotsSpawned = true;
            int[][] mSpawns = new int[][]{{18, -18}, {-18, 18}, {18, 18}, {0, -24}, {0, 24}, {-24, 0}, {24, 0}};
            int murdererIdx = (myMurderRole == MURDER_ROLE_MURDERER) ? -1 : (int)(Math.abs(w.seed) % mSpawns.length);
            int sheriffIdx = (myMurderRole == MURDER_ROLE_SHERIFF) ? -1 : ((murdererIdx + 3) % mSpawns.length);
            for (int i = 0; i < mSpawns.length; i++) {
               int[] sp = mSpawns[i];
               Mobs.Mob bot = g.mobs.spawn(Mobs.ZOMBIE, sp[0] + 0.5, Y_DECK + 1.0, sp[1] + 0.5);
               if (bot != null) {
                  String bName = BOT_NAMES[(i + 1) % BOT_NAMES.length];
                  bot.teamTag = "MurderBot:" + i + ":" + bName;
                  bot.variant = i;
                  int bSlot = Math.abs(bot.netId) & 31;
                  int role = (i == murdererIdx) ? MURDER_ROLE_MURDERER : ((i == sheriffIdx) ? MURDER_ROLE_SHERIFF : MURDER_ROLE_INNOCENT);
                  initBotInventory(bSlot, 0, 0, 0, 0, 0, 0, role);
               }
            }
         }
      }
   }

   public static void stockTumbleArena(Game g, World w) {
      if (w == null) {
         return;
      }
      if (!sbIslandStocked[0]) {
         w.ensureArenaChunksLoaded();
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
      // Spawn 5 Named Tumble AI Competitors around the ring in solo mode
      ensureRoundBotsSpawned(g, w);
   }

   public static void stockSkyblockChests(Game g, World w) {
      stockTumbleArena(g, w);
   }

   public static void stockSkywarsChests(Game g, World w, boolean refill) {
      if (w != null && !swIslandStocked[0]) {
         w.ensureArenaChunksLoaded();
      }
      if (refill) {
         Arrays.fill(swIslandStocked, false);
      }
      // 1. 8 Outer Spawn Island Chests + 7 Named Battle AI Fighters on Podiums 1..7
      for (int i = 0; i < SKYWARS_SPAWNS.length; i++) {
         if (!swIslandStocked[i]) {
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
            }
         }
         if (!refill && !swBotSpawned[i] && i > 0 && soloBots && !Net319.isClientConnected() && g != null && g.mobs != null && w != null) {
            swBotSpawned[i] = true;
            spawnBattleBotAtPodium(g, w, i);
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

   private static void spawnBedwarsTeamBot(Game g, int team, int role) {
      if (g == null || g.mobs == null) {
         return;
      }
      int t = Math.abs(team) & 3;
      int bx = BEDWARS_BASES[t][0];
      int bz = BEDWARS_BASES[t][1];
      if (g.world != null) {
         g.world.ensureChunkLoaded(bx >> 4, bz >> 4);
         if (g.world.getBlock(bx, Y_DECK, bz) == 0) {
            g.world.setBlock(bx, Y_DECK, bz, Blocks.STONEBRICK);
         }
      }
      int slot = t * 2 + (role & 1);
      bwBotInitialSpawned[slot] = true;
      double ox = role == 0 ? 0.5 : -0.5;
      double oz = role == 0 ? -0.5 : 0.5;
      Mobs.Mob bot = g.mobs.spawn(role == 0 ? Mobs.ZOMBIE : Mobs.SKELETON, bx + 0.5 + ox, Y_DECK + 1.0, bz + 0.5 + oz);
      if (bot != null) {
         String bName = BOT_NAMES[(t * 2 + role) % BOT_NAMES.length];
         bot.teamTag = "Team:" + BEDWARS_NAMES[t] + ":" + role + ":" + bName;
         bot.variant = role & 1;
         int bSlot = Math.abs(bot.netId) & 31;
         initBotInventory(bSlot, 32, 0, 0, 0, Items.TOOL_BASE, 0, 0);
      }
   }

   public static void stockBedwarsWorld(Game g, World w) {
      if (w != null && !bwBaseStocked[0]) {
         w.ensureArenaChunksLoaded();
      }
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
               // Spawn 2 Smart AI Bots per unoccupied enemy base: Defender (#0) + Bridger/Raider (#1)!
               if (soloBots && !Net319.isClientConnected() && t != getPlayerTeam(g) && !Net319.isTeamOccupiedByPeer(t)) {
                  spawnBedwarsTeamBot(g, t, 0);
                  spawnBedwarsTeamBot(g, t, 1);
               }
            }
         }
      }
   }

   // ================================================================================
   //  V4.2 Minigame Smart AI Competitors (Player Rendering, Bridge Pathfinding & Combat)
   // ================================================================================

   public static final class BotIntent {
      public boolean moving;
      public double speedMul = 1.0;
      public float headYaw;
      public float headPitch;
   }

   public static boolean isMinigameBot(Mobs.Mob m) {
      if (m == null || m.teamTag == null) {
         return false;
      }
      return m.teamTag.startsWith("BattleBot:")
         || m.teamTag.startsWith("TumbleBot:")
         || m.teamTag.startsWith("Team:")
         || m.teamTag.startsWith("CS2Bot:")
         || m.teamTag.startsWith("MurderBot:");
   }

   public static int getBotNetFlags(Mobs.Mob m) {
      if (m == null) {
         return 0;
      }
      int bSlot = Math.abs(m.netId) & 31;
      return (m.onGround ? 128 : 0)
         | (m.moving ? 256 : 0)
         | (botHoldingBlockTicks[bSlot] > 0 ? 512 : 0)
         | (botEatTicks[bSlot] > 0 ? 1024 : 0)
         | (botBowDrawTicks[bSlot] > 0 ? 2048 : 0);
   }

   public static int getBotGearTier(Mobs.Mob m) {
      if (m == null) {
         return 0;
      }
      int bSlot = Math.abs(m.netId) & 31;
      return (botGearTier[bSlot] & 7) | ((botMurderRole[bSlot] & 3) << 3);
   }

   public static void applyRemoteBotState(Mobs.Mob m, int flags, int gearTier) {
      if (m == null) {
         return;
      }
      int bSlot = Math.abs(m.netId) & 31;
      m.onGround = (flags & 128) != 0;
      m.moving = (flags & 256) != 0;
      botHoldingBlockTicks[bSlot] = (flags & 512) != 0 ? 6 : 0;
      botEatTicks[bSlot] = (flags & 1024) != 0 ? 9 : 0;
      botBowDrawTicks[bSlot] = (flags & 2048) != 0 ? 10 : 0;
      botGearTier[bSlot] = Math.max(0, Math.min(4, gearTier & 7));
      botMurderRole[bSlot] = (gearTier >> 3) & 3;
   }

   public static String botDisplayName(Mobs.Mob m) {
      if (m == null || m.teamTag == null || m.teamTag.isEmpty()) {
         return "Rival";
      }
      String[] parts = m.teamTag.split(":");
      if (m.teamTag.startsWith("Team:") && parts.length >= 2) {
         String teamName = parts[1];
         String nick = parts.length >= 4 ? parts[3] : (parts.length >= 3 ? parts[2] : (teamName + " Bot"));
         return "[" + teamName.toUpperCase() + "] " + nick;
      }
      if (m.teamTag.startsWith("CS2Bot:") && parts.length >= 4) {
         String side = "Blue".equals(parts[1]) ? "CT" : "T";
         return "[" + side + "] " + parts[3];
      }
      if (parts.length >= 3) {
         return parts[2];
      }
      return m.teamTag;
   }

   public static int botTeamIndex(Mobs.Mob m) {
      if (m == null || m.teamTag == null) {
         return -1;
      }
      if (m.teamTag.startsWith("Team:")) {
         for (int t = 0; t < 4; t++) {
            if (m.teamTag.startsWith("Team:" + BEDWARS_NAMES[t])) {
               return t;
            }
         }
      }
      if (m.teamTag.startsWith("CS2Bot:")) {
         return m.teamTag.startsWith("CS2Bot:Blue:") ? 0 : 1; // 0 = CT (Blue), 1 = T (Red)
      }
      return -1;
   }

   public static int botHeldItem(Mobs.Mob bot, World w) {
      if (bot == null || w == null || w.settings == null) {
         return Items.toolId(1, Items.C_SWORD);
      }
      int type = w.settings.type;
      int bSlot = Math.abs(bot.netId) & 31;
      if (botEatTicks[bSlot] > 0 && botApples[bSlot] > 0) {
         return Redstone319.GOLDEN_APPLE;
      }
      if (type == WorldSettings.MURDER) {
         int role = botMurderRole[bSlot];
         if (role == MURDER_ROLE_MURDERER) {
            // Murderer conceals their knife unless actively striking!
            return bot.attackAnim > 0 ? botWeaponItem[bSlot] : 0;
         }
         if (botRangedItem[bSlot] != 0 && botArrows[bSlot] > 0) {
            return botRangedItem[bSlot];
         }
         return 0;
      }
      if (type == WorldSettings.CS2) {
         if (botHoldingBlockTicks[bSlot] > 0 && botBlocks[bSlot] > 0) {
            int csTeam = Math.max(0, botTeamIndex(bot));
            return csTeam == 0 ? Blocks.CONCRETE0 + 11 : Blocks.CONCRETE0 + 14;
         }
         if (botRangedItem[bSlot] != 0 && botArrows[bSlot] > 0) {
            return botRangedItem[bSlot];
         }
         return botWeaponItem[bSlot] != 0 ? botWeaponItem[bSlot] : Redstone319.MURDER_KNIFE;
      }
      if (type == WorldSettings.TUMBLE) {
         if (botWeaponItem[bSlot] != 0) {
            return botWeaponItem[bSlot];
         }
         return botArrows[bSlot] > 0 ? Blocks.SNOW : 0;
      }
      if (type == WorldSettings.BEDWARS) {
         int t = Math.max(0, botTeamIndex(bot));
         if (botHoldingBlockTicks[bSlot] > 0 && botBlocks[bSlot] > 0) {
            return Blocks.WOOL0 + BEDWARS_COLORS[t];
         }
         if (botBowDrawTicks[bSlot] > 0 && botRangedItem[bSlot] != 0 && botArrows[bSlot] > 0) {
            return botRangedItem[bSlot];
         }
         return botWeaponItem[bSlot] != 0 ? botWeaponItem[bSlot] : Items.TOOL_BASE;
      }
      if (botHoldingBlockTicks[bSlot] > 0 && botBlocks[bSlot] > 0) {
         return Blocks.STONEBRICK;
      }
      if (botBowDrawTicks[bSlot] > 0 && botRangedItem[bSlot] != 0 && botArrows[bSlot] > 0) {
         return botRangedItem[bSlot];
      }
      return botWeaponItem[bSlot];
   }

   public static void drawBotPlayer(Mobs.Mob bot, World w, Renderer r, float partial, double x, double y, double z) {
      if (bot == null || r == null || w == null) {
         return;
      }
      int bSlot = Math.abs(bot.netId) & 31;
      float bright = r.brightnessAt(w, x, y + 1.0, z);
      PlayerModel.Pose pose = new PlayerModel.Pose();
      pose.bodyYaw = MobRender.lerpAngle(bot.pyaw, bot.yaw, partial);
      float hy = MobRender.lerpAngle(bot.pheadYaw, bot.headYaw, partial);
      pose.headYaw = Math.max(-55.0F, Math.min(55.0F, Mobs.wrap(hy - pose.bodyYaw)));
      pose.headPitch = bot.pheadPitch + (bot.headPitch - bot.pheadPitch) * partial;
      pose.limb = bot.pwalk + (bot.walk - bot.pwalk) * partial;
      pose.amp = bot.pwalkAmp + (bot.walkAmp - bot.pwalkAmp) * partial;
      pose.swing = bot.attackAnim > 0 ? (1.0F - bot.attackAnim / 8.0F) : -1.0F;
      boolean speedBridging = botHoldingBlockTicks[bSlot] > 0 && bot.onGround;
      pose.sneak = speedBridging;
      pose.sneakK = speedBridging ? 0.85F : 0.0F;
      pose.sprint = bot.moving && !speedBridging;
      pose.sprintK = pose.sprint ? 1.0F : 0.0F;
      pose.air = !bot.onGround;
      pose.airK = bot.onGround ? 0.0F : 1.0F;
      pose.vy = (float)bot.vy;
      pose.eat = botEatTicks[bSlot] > 0 ? (1.0F - botEatTicks[bSlot] / 18.0F) : -1.0F;
      pose.bow = botBowDrawTicks[bSlot] > 0 ? Math.min(1.0F, botBowDrawTicks[bSlot] / 14.0F) : -1.0F;
      pose.hurt = (bot.hurtTime > 0 || bot.dead) ? 1.0F : 0.0F;
      pose.age = bot.age + partial;
      pose.held = botHeldItem(bot, w);

      int skinIdx = Math.abs(bot.netId + bot.variant);
      int team = botTeamIndex(bot);
      int col = 0xFFE680;
      if (w.settings != null && w.settings.type == WorldSettings.CS2) {
         col = team == 0 ? 0x55AAFF : 0xFF5555;
      } else if (team >= 0 && team < 4) {
         int[] rgb = new int[]{0xFF5555, 0x55AAFF, 0x55FF55, 0xFFFF55};
         col = rgb[team];
      }
      GL11.glPushMatrix();
      GL11.glTranslated(x - r.camX, y - r.camY, z - r.camZ);
      PlayerModel.draw(Net319.skinPixels(skinIdx), (skinIdx & 1) == 1, pose, bright);
      String tag = (w.settings != null && w.settings.type == WorldSettings.MURDER)
         ? botDisplayName(bot)
         : (botDisplayName(bot) + " (" + Math.max(0, bot.health) + "HP)");
      float hpFrac = (w.settings != null && w.settings.type == WorldSettings.MURDER) ? 1.0F : Math.max(0.0F, bot.health / 20.0F);
      Net319.drawNameTag(r, tag, 2.16F, col, hpFrac);
      GL11.glPopMatrix();
   }

   public static void onBotKilled(Game g, Mobs.Mob bot) {
      if (bot == null || g == null || g.world == null || g.world.settings == null) {
         return;
      }
      // Prevent duplicate death handling for the same bot instance
      if (bot.flee == -999) {
         return;
      }
      bot.flee = -999;
      int type = g.world.settings.type;
      int bSlot = Math.abs(bot.netId) & 31;
      if (type == WorldSettings.BEDWARS) {
         int team = botTeamIndex(bot);
         int role = bot.variant & 1;
         if (team >= 0 && team < 4) {
            int slot = team * 2 + role;
            if (isTeamBedIntact(g.world, team)) {
               bwBotRespawnTicks[slot] = 100; // Respawn in 5 seconds while team bed is intact!
               g.say("\u00a77[Bedwars] " + BEDWARS_CODES[team] + botDisplayName(bot) + " \u00a77was eliminated and will respawn in 5s!");
            } else {
               bwBotRespawnTicks[slot] = 0;
               g.say("\u00a7e\u00a7l[FINAL KILL] " + BEDWARS_CODES[team] + botDisplayName(bot) + " \u00a77has been permanently eliminated!");
            }
         }
      } else if (type == WorldSettings.MURDER) {
         int role = botMurderRole[bSlot];
         if (role == MURDER_ROLE_SHERIFF) {
            murderGunDropped = true;
            murderDroppedGunX = bot.x;
            murderDroppedGunY = bot.y;
            murderDroppedGunZ = bot.z;
            g.world.dropItem(bot.x, bot.y + 0.5, bot.z, Redstone319.PISTOL, 1);
            g.say("\u00a7e\u00a7l[Murder Mystery] THE SHERIFF (" + botDisplayName(bot) + ") WAS KILLED! \u00a7aTheir Pistol has been dropped!");
         } else if (role == MURDER_ROLE_MURDERER && !murderRoundOver) {
            murderRoundOver = true;
            boolean iWon = myMurderRole != MURDER_ROLE_MURDERER;
            pendingOverviewWon = iWon;
            pendingOverviewChamp = "Innocents & Sheriff (Stopped " + botDisplayName(bot) + "!)";
            victoryTransitionTicks = 30;
            SoundEngine.playMinigameSound("mg_win");
            g.say("\u00a7a\u00a7l[Murder Mystery] THE MURDERER (" + botDisplayName(bot) + ") HAS BEEN ELIMINATED! Innocents Win!");
         }
      }
   }

   private static boolean hasFootingAt(World w, int bx, int by, int bz) {
      for (int dy = -2; dy <= 5; dy++) {
         int id = w.getBlock(bx, by - dy, bz);
         if (Blocks.isLava(id)) {
            return false;
         }
         if (id != 0 && Blocks.coll[id] != null) {
            return true;
         }
      }
      return false;
   }

   /** Returns true if chest at (cx, cy, cz) exists and still has at least one item stack inside. */
   private static boolean hasChestItemsAt(World w, int cx, int cy, int cz) {
      int id = w.getBlock(cx, cy, cz);
      if (id != Blocks.CHEST && id != Blocks.CHEST_LOOT) {
         return false;
      }
      TileEntity te = w.tileAt(cx, cy, cz);
      if (te == null || te.slots == null) {
         return id == Blocks.CHEST_LOOT;
      }
      for (Stack st : te.slots) {
         if (st != null && st.n > 0) {
            return true;
         }
      }
      return false;
   }

   /** Consumes up to 4 item stacks from chest at (cx, cy, cz) and adds those exact items to the bot's inventory! */
   private static void lootChestAt(Mobs.Mob bot, World w, int cx, int cy, int cz) {
      int bSlot = Math.abs(bot.netId) & 31;
      TileEntity te = w.getOrCreateTile(cx, cy, cz);
      int taken = 0;
      if (te != null && te.slots != null) {
         for (int i = 0; i < te.slots.length && taken < 4; i++) {
            Stack st = te.slots[i];
            if (st != null && st.n > 0) {
               int id = st.id;
               int cnt = st.n;
               if (Items.isTool(id) && (Items.toolClass(id) == Items.C_SWORD || Items.toolClass(id) == Items.C_AXE)) {
                  if (botWeaponItem[bSlot] == 0 || Items.attackDamage(id) > Items.attackDamage(botWeaponItem[bSlot])) {
                     botWeaponItem[bSlot] = id;
                  }
               } else if (id == Items.BOW || id == Redstone319.PISTOL || id == Redstone319.MACHINE_GUN || id == Redstone319.SNIPER_RIFLE) {
                  botRangedItem[bSlot] = id;
               } else if (id == Items.ARROW) {
                  botArrows[bSlot] += cnt;
               } else if (id == Redstone319.GOLDEN_APPLE || Items.isFood(id)) {
                  botApples[bSlot] += Math.max(1, cnt / 3);
               } else if (id == Blocks.TNT || id == Redstone319.TNT_GRENADE || id == Redstone319.FIRE_CHARGE) {
                  botGrenades[bSlot] += cnt;
               } else if (!Items.isItem(id) && Blocks.solid[id]) {
                  botBlocks[bSlot] += cnt;
               }
               te.slots[i] = null;
               taken++;
            }
         }
         if (!Net319.isClientConnected() || Net319.isHosting()) {
            Net319.onTileChanged(w, te);
         }
      }
      if (w.settings == null || w.settings.type != WorldSettings.BEDWARS) {
         bot.variant = Math.min(3, bot.variant + 1);
      }
      botGearTier[bSlot] = Math.min(4, botGearTier[bSlot] + 1);
      botResources[bSlot] += 4;
      bot.health = Math.min(20, bot.health + 4);
      bot.attackAnim = 8;
      SoundEngine.play3D(w, "chest_open", cx + 0.5, cy + 0.5, cz + 0.5, 0.72F, 1.0F);
   }

   /**
    * Evaluates the bot's surroundings for safety (no enemy within dangerRadius, solid footing beneath) and,
    * when safe, scans for nearby dropped generator/loot items or unlooted chests to collect!
    * Returns a [targetX, targetY, targetZ] loot waypoint if safe looting is in progress, or null otherwise.
    */
   private static double[] trySmartLootSurroundings(
      Mobs mobs, Mobs.Mob bot, World w, Player player, int myBotTeam, double nearestEnemyDist, double enemyX, double enemyZ
   ) {
      // 1. Surroundings safety check: never stop to loot if an enemy is within 7.5 blocks or if falling!
      if (nearestEnemyDist < 7.5 || !bot.onGround) {
         return null;
      }
      int bSlot = Math.abs(bot.netId) & 31;

      // 2. Scan for nearby dropped items (Emeralds, Diamonds, Gold, Iron, Wool, Weapons) on safe ground
      World.Item bestItem = null;
      double bestItemScore = 999.0;
      for (World.Item it : w.items) {
         if (it == null || it.dead || it.count <= 0) {
            continue;
         }
         double d = Math.hypot(it.x - bot.x, it.z - bot.z);
         if (d > 13.5 || Math.abs(it.y - bot.y) > 4.5) {
            continue;
         }
         // Check if an enemy is closer to the item (unsafe to contest!)
         if (Math.hypot(it.x - enemyX, it.z - enemyZ) < 5.5) {
            continue;
         }
         if (!hasFootingAt(w, (int)Math.floor(it.x), (int)Math.floor(it.y), (int)Math.floor(it.z))) {
            continue;
         }
         double priority = d;
         if (it.id == Items.EMERALD || it.id == Items.DIAMOND || it.id == Redstone319.GOLDEN_APPLE || it.id == Redstone319.PISTOL) {
            priority -= 5.0; // High priority for Emeralds, Diamonds, Golden Apples & Dropped Sheriff Pistol!
         } else if (it.id == Items.GOLD_INGOT || it.id == Items.ENDER_PEARL) {
            priority -= 3.0;
         }
         if (priority < bestItemScore) {
            bestItemScore = priority;
            bestItem = it;
         }
      }
      if (bestItem != null) {
         double dItem = Math.hypot(bestItem.x - bot.x, bestItem.z - bot.z);
         if (dItem <= 1.95) {
            // Pick up the item and add it to the bot's actual inventory!
            int id = bestItem.id;
            int cnt = bestItem.count;
            bestItem.dead = true;
            bot.attackAnim = 6;
            botResources[bSlot] += cnt;
            if (id == Redstone319.GOLDEN_APPLE) {
               botApples[bSlot] += cnt;
            } else if (id == Items.ARROW) {
               botArrows[bSlot] += cnt;
            } else if (id == Redstone319.PISTOL || id == Redstone319.MACHINE_GUN || id == Redstone319.SNIPER_RIFLE || id == Items.BOW) {
               botRangedItem[bSlot] = id;
               botArrows[bSlot] = Math.max(botArrows[bSlot], 8);
               if (w.settings != null && w.settings.type == WorldSettings.MURDER) {
                  murderGunDropped = false;
                  if (mobs.g != null) {
                     mobs.g.say("\u00a7b\u00a7l[Murder Mystery] " + botDisplayName(bot) + " \u00a7apicked up the Sheriff's Pistol!");
                  }
               }
            } else if (id == Items.IRON_INGOT && w.settings != null && w.settings.type == WorldSettings.BEDWARS) {
               botBlocks[bSlot] += cnt * 4;
               if (botResources[bSlot] >= 8 && botWeaponItem[bSlot] == Items.TOOL_BASE) {
                  botWeaponItem[bSlot] = Items.toolId(1, Items.C_SWORD);
               }
            } else if (id == Items.GOLD_INGOT) {
               botGold[bSlot] += cnt;
               if (w.settings != null && w.settings.type == WorldSettings.MURDER && botGold[bSlot] >= 5 && botMurderRole[bSlot] == MURDER_ROLE_INNOCENT) {
                  botGold[bSlot] -= 5;
                  botRangedItem[bSlot] = Redstone319.PISTOL;
                  botArrows[bSlot] += 6;
               } else if (w.settings != null && w.settings.type == WorldSettings.BEDWARS && botGold[bSlot] >= 3) {
                  botGold[bSlot] -= 3;
                  botApples[bSlot]++;
                  botWeaponItem[bSlot] = Items.toolId(2, Items.C_SWORD);
                  botRangedItem[bSlot] = Items.BOW;
                  botArrows[bSlot] += 8;
               }
            } else if (id == Items.EMERALD || id == Items.DIAMOND) {
               botGearTier[bSlot] = Math.min(4, botGearTier[bSlot] + 1);
               botWeaponItem[bSlot] = Items.toolId(Math.min(4, 1 + botGearTier[bSlot]), Items.C_SWORD);
               if (w.settings.type == WorldSettings.BEDWARS && myBotTeam >= 0 && botResources[bSlot] >= 4) {
                  unlockNextTeamUpgrade(mobs.g, myBotTeam, true);
               }
            } else if (!Items.isItem(id) && Blocks.solid[id]) {
               botBlocks[bSlot] += cnt;
            }
            SoundEngine.play3D(w, "item_pop", bot.x, bot.y + 1.0, bot.z, 0.55F, 1.15F);
         } else {
            return new double[]{bestItem.x, bestItem.y, bestItem.z};
         }
      }

      // 3. Scan nearby surroundings (within 9 blocks) for unlooted Chests on safe ground
      int bbx = (int)Math.floor(bot.x);
      int bby = (int)Math.floor(bot.y);
      int bbz = (int)Math.floor(bot.z);
      int bestCx = 0, bestCy = -1, bestCz = 0;
      double bestChestD = 999.0;
      for (int dz = -9; dz <= 9; dz++) {
         for (int dx = -9; dx <= 9; dx++) {
            for (int dy = -2; dy <= 3; dy++) {
               int cx = bbx + dx;
               int cy = bby + dy;
               int cz = bbz + dz;
               if (hasChestItemsAt(w, cx, cy, cz)) {
                  double d = Math.hypot(cx + 0.5 - bot.x, cz + 0.5 - bot.z);
                  if (d < bestChestD && Math.hypot(cx + 0.5 - enemyX, cz + 0.5 - enemyZ) > 6.0) {
                     bestChestD = d;
                     bestCx = cx;
                     bestCy = cy;
                     bestCz = cz;
                  }
               }
            }
         }
      }
      if (bestCy >= 0) {
         if (bestChestD <= 2.55) {
            lootChestAt(bot, w, bestCx, bestCy, bestCz);
         } else {
            return new double[]{bestCx + 0.5, bestCy, bestCz + 0.5};
         }
      }
      return null;
   }

   /**
    * Dynamically places a 2-wide bridge at Y_DECK (or current deck height) along the bot's path toward (tx, tz)
    * ONLY if the bot actually has building blocks in its inventory (botBlocks[bSlot] > 0)!
    */
   public static void bridgeTowardTarget(Mobs.Mob bot, World w, double tx, double tz, int bridgeBlockId) {
      int bSlot = Math.abs(bot.netId) & 31;
      if (botBlocks[bSlot] <= 0) {
         return;
      }
      double dx = tx - bot.x;
      double dz = tz - bot.z;
      double dist = Math.hypot(dx, dz);
      if (dist < 1.8) {
         return;
      }
      double ux = dx / dist;
      double uz = dz / dist;
      int bridgeY = Y_DECK;
      int perpX = Math.abs(ux) >= Math.abs(uz) ? 0 : 1;
      int perpZ = Math.abs(ux) >= Math.abs(uz) ? 1 : 0;

      // Ensure footing directly under the bot and 1..2 blocks ahead along (ux, uz)
      for (int step = 0; step <= 2; step++) {
         if (botBlocks[bSlot] <= 0) {
            break;
         }
         int bx = (int)Math.floor(bot.x + ux * step * 0.95);
         int bz = (int)Math.floor(bot.z + uz * step * 0.95);
         if (!hasFootingAt(w, bx, (int)Math.floor(bot.y), bz)) {
            if (w.chunkAt(bx, bz) != null && w.getBlock(bx, bridgeY, bz) == 0) {
               w.setBlock(bx, bridgeY, bz, bridgeBlockId);
               botBlocks[bSlot]--;
               bot.attackAnim = 6;
               botHoldingBlockTicks[bSlot] = 14;
               SoundEngine.playBlockPlace(w, bridgeBlockId, bx, bridgeY, bz);
            }
            int sx = bx + perpX;
            int sz = bz + perpZ;
            if (botBlocks[bSlot] > 0 && w.chunkAt(sx, sz) != null && w.getBlock(sx, bridgeY, sz) == 0) {
               w.setBlock(sx, bridgeY, sz, bridgeBlockId);
               botBlocks[bSlot]--;
            }
         }
      }
   }

   /**
    * Allows an AI bot to build up vertically (pillar-jump / step-build) when its target is on higher ground
    * or when scaling a tall ledge, strictly consuming blocks from botBlocks[bSlot]!
    */
   public static boolean tryBotBuildUp(Mobs.Mob bot, World w, double tx, double ty, double tz, int blockId) {
      int bSlot = Math.abs(bot.netId) & 31;
      if (botBlocks[bSlot] <= 0 || botBuildUpCooldown[bSlot] > 0 || !bot.onGround) {
         return false;
      }
      double dist = Math.hypot(tx - bot.x, tz - bot.z);
      int bx = (int)Math.floor(bot.x);
      int by = (int)Math.floor(bot.y);
      int bz = (int)Math.floor(bot.z);
      if (by >= Y_DECK + 9 || w.getBlock(bx, by + 2, bz) != 0) {
         return false;
      }
      boolean targetHigher = (ty - bot.y >= 1.8) && dist <= 9.5;
      boolean wallBlocked = false;
      if (dist > 1.2) {
         double ux = (tx - bot.x) / dist;
         double uz = (tz - bot.z) / dist;
         int ax = (int)Math.floor(bot.x + ux * 0.95);
         int az = (int)Math.floor(bot.z + uz * 0.95);
         wallBlocked = Blocks.solid[w.getBlock(ax, by, az)] && Blocks.solid[w.getBlock(ax, by + 1, az)] && w.getBlock(ax, by + 2, az) == 0;
      }
      if (!targetHigher && !wallBlocked) {
         return false;
      }
      if (w.chunkAt(bx, bz) != null && w.getBlock(bx, by, bz) == 0 && Blocks.solid[w.getBlock(bx, by - 1, bz)]) {
         w.setBlock(bx, by, bz, blockId);
         botBlocks[bSlot]--;
         botBuildUpCooldown[bSlot] = 14;
         botHoldingBlockTicks[bSlot] = 14;
         bot.attackAnim = 6;
         bot.setPos(bx + 0.5, by + 1.0, bz + 0.5);
         bot.vy = 0.24;
         SoundEngine.playBlockPlace(w, blockId, bx, by, bz);
         return true;
      }
      return false;
   }

   /** Steers a minigame bot toward (tx, tz) while climbing steps/terraces, using Slime Jump Pads, and parkour-jumping gaps! */
   private static void steerBotSafely(Mobs.Mob bot, World w, double tx, double tz, BotIntent out, double baseSpeed) {
      steerBotSafely3D(bot, w, tx, bot.y, tz, out, baseSpeed);
   }

   private static void steerBotSafely3D(Mobs.Mob bot, World w, double tx, double ty, double tz, BotIntent out, double baseSpeed) {
      int bbx = (int)Math.floor(bot.x);
      int bby = (int)Math.floor(bot.y);
      int bbz = (int)Math.floor(bot.z);
      int underId = w.getBlock(bbx, bby - 1, bbz);
      if (underId == Redstone319.SLIME_BLOCK && bot.onGround) {
         bot.vy = 1.04;
         bot.onGround = false;
         SoundEngine.play3D(w, "slime_bounce", bot.x, bot.y, bot.z, 0.8F, 1.0F);
      }

      // If target is on an upper deck (ty > bot.y + 2.8), route via nearest Slime Jump Pad or staircase if nearby!
      if (ty > bot.y + 2.8 && Math.hypot(tx - bot.x, tz - bot.z) < 16.0) {
         double bestPadD = 99.0;
         int bestPx = 0;
         int bestPz = 0;
         for (int dz = -7; dz <= 7; dz++) {
            for (int dx = -7; dx <= 7; dx++) {
               int px = bbx + dx;
               int pz = bbz + dz;
               if (w.getBlock(px, bby - 1, pz) == Redstone319.SLIME_BLOCK || w.getBlock(px, Y_DECK, pz) == Redstone319.SLIME_BLOCK) {
                  double pd = Math.hypot(px + 0.5 - bot.x, pz + 0.5 - bot.z);
                  if (pd < bestPadD) {
                     bestPadD = pd;
                     bestPx = px;
                     bestPz = pz;
                  }
               }
            }
         }
         if (bestPadD < 10.5) {
            tx = bestPx + 0.5;
            tz = bestPz + 0.5;
         }
      }

      double dx = tx - bot.x;
      double dz = tz - bot.z;
      double dist = Math.hypot(dx, dz);
      if (dist < 0.45) {
         out.moving = false;
         return;
      }
      float desiredYaw = Mobs.angleTo(dx, dz);
      int by = (int)Math.floor(bot.y);

      double rad = Math.toRadians(desiredYaw);
      int stepX = (int)Math.floor(bot.x - Math.sin(rad) * 0.95);
      int stepZ = (int)Math.floor(bot.z + Math.cos(rad) * 0.95);
      int frontBlock = w.getBlock(stepX, by, stepZ);
      if (bot.onGround && frontBlock != 0 && Blocks.coll[frontBlock] != null && w.getBlock(stepX, by + 1, stepZ) == 0) {
         // Step-up jump onto 1-block higher terrace, bridge arch, or staircase!
         bot.vy = Mobs.JUMP;
      }

      // Surroundings-aware multi-angle raycast: score candidate angles by continuous footing (1..3 blocks ahead) & target alignment!
      float[] offsets = new float[]{0.0F, 22.0F, -22.0F, 45.0F, -45.0F, 70.0F, -70.0F, 95.0F, -95.0F, 130.0F, -130.0F};
      float bestYaw = desiredYaw;
      int bestSafetyScore = -1;
      for (float off : offsets) {
         float candYaw = desiredYaw + off;
         double crad = Math.toRadians(candYaw);
         double sx = -Math.sin(crad);
         double sz = Math.cos(crad);
         boolean ok1 = hasFootingAt(w, (int)Math.floor(bot.x + sx * 1.15), by, (int)Math.floor(bot.z + sz * 1.15));
         boolean ok2 = hasFootingAt(w, (int)Math.floor(bot.x + sx * 2.15), by, (int)Math.floor(bot.z + sz * 2.15));
         boolean ok3 = hasFootingAt(w, (int)Math.floor(bot.x + sx * 3.10), by, (int)Math.floor(bot.z + sz * 3.10));
         if (ok1) {
            int score = 100 - (int)(Math.abs(off) * 0.65F) + (ok2 ? 35 : 0) + (ok3 ? 20 : 0);
            if (score > bestSafetyScore) {
               bestSafetyScore = score;
               bestYaw = candYaw;
            }
         } else if (Math.abs(off) <= 25.0F && bot.onGround && dist > 2.2 && (ok2 || ok3)) {
            // Parkour jump across a 1-2 block gap with confirmed landing on the far side!
            bot.vy = Mobs.JUMP * 1.08;
            bestSafetyScore = 90;
            bestYaw = candYaw;
            break;
         }
      }

      if (bestSafetyScore < 0) {
         bot.dirYaw = Mobs.wrap(desiredYaw + 180.0F);
         out.moving = false;
         return;
      }
      int bSlot = Math.abs(bot.netId) & 31;
      bot.dirYaw = bestYaw;
      out.headYaw = bestYaw;
      out.moving = true;
      // Scale bot speed to match real human player sprinting speed (~0.22..0.25 blocks/tick)
      out.speedMul = baseSpeed * 1.38;

      // Player-like Sprint-Jumping (Bunny-Hopping) across confirmed safe straightaways!
      if (bot.onGround
         && dist > 5.5
         && bestSafetyScore >= 135
         && botHoldingBlockTicks[bSlot] <= 0
         && botEatTicks[bSlot] <= 0
         && ((bot.age + Math.abs(bot.netId) * 7) % 16 == 0)) {
         bot.vy = Mobs.JUMP;
         out.speedMul *= 1.12;
      }

      // 3D Footstep Acoustics while running on solid blocks
      if (bot.onGround && underId > 0 && ((bot.age + Math.abs(bot.netId) * 3) % 7 == 0)) {
         SoundEngine.playFootstep(w, underId, bot.x, bot.y, bot.z, 0.34F);
      }
   }

   /** Bridges across island gaps in Battle by routing Outer Spawn -> Nearest Mid-Ring -> Center -> Target. */
   private static double[] nextArenaBridgeWaypoint(World w, double sx, double sz, double tx, double tz) {
      int mx = (int)Math.floor((sx + tx) * 0.5);
      int mz = (int)Math.floor((sz + tz) * 0.5);
      if (hasFootingAt(w, mx, Y_DECK, mz) || Math.hypot(tx - sx, tz - sz) <= 9.0) {
         return new double[]{tx, tz};
      }
      double dSelf = Math.hypot(sx, sz);
      double dTarg = Math.hypot(tx, tz);
      // If on an outer spawn island (d > 26), step to the closest Mid-Ring node or Center along the cardinal/diagonal walkways
      if (dSelf > 26.0) {
         double bestD = 1e9;
         double[] best = new double[]{0.5, 0.5};
         for (int[] m : SKYWARS_MIDS) {
            double d = Math.hypot(sx - m[0], sz - m[1]);
            if (d < bestD) {
               bestD = d;
               best = new double[]{m[0] + 0.5, m[1] + 0.5};
            }
         }
         if (Math.abs(Math.abs(sx) - Math.abs(sz)) <= 6.0) {
            return new double[]{sx * 0.55, sz * 0.55};
         }
         return best;
      }
      if (dSelf > 12.0 && dTarg > 12.0 && (sx * tx + sz * tz) < 0.0) {
         return new double[]{0.5, 0.5};
      }
      return new double[]{tx, tz};
   }

   public static BotIntent tickBotAI(Mobs mobs, Mobs.Mob bot, World w, Player player) {
      if (mobs == null || bot == null || w == null || w.settings == null) {
         return null;
      }
      BotIntent out = new BotIntent();
      out.headYaw = bot.yaw;
      out.headPitch = 0.0F;
      if (bot.cd > 0) {
         bot.cd--;
      }

      int mode = w.settings.type;
      int botSlot = Math.abs(bot.netId) & 31;
      if (botPearlCooldown[botSlot] > 0) {
         botPearlCooldown[botSlot]--;
      }
      if (botHoldingBlockTicks[botSlot] > 0) {
         botHoldingBlockTicks[botSlot]--;
      }
      if (botEatTicks[botSlot] > 0) {
         botEatTicks[botSlot]--;
      }
      if (botBowDrawTicks[botSlot] > 0) {
         botBowDrawTicks[botSlot]--;
      }
      if (botBuildUpCooldown[botSlot] > 0) {
         botBuildUpCooldown[botSlot]--;
      }

      // Tactical Golden Apple Self-Healing: bots ONLY eat a Golden Apple if they actually have one in botApples[botSlot]!
      if (bot.health > 0
         && bot.health <= 10
         && botApples[botSlot] > 0
         && botEatTicks[botSlot] <= 0
         && bot.age % 30 == 0) {
         botApples[botSlot]--;
         botHealsUsed[botSlot]++;
         botEatTicks[botSlot] = 18;
         bot.health = Math.min(20, bot.health + 6);
         SoundEngine.play3D(w, "eat_1", bot.x, bot.y + 1.4, bot.z, 0.7F, 1.0F);
         if (mobs.g != null) {
            mobs.g.spawnBreakParticles((int)Math.floor(bot.x), (int)Math.floor(bot.y + 1.2), (int)Math.floor(bot.z), Blocks.GOLD_BLOCK, 6);
         }
      }

      // ====================================================================
      //  1. TUMBLE AI: Predictive Cut-Off Spleefing, High-Ground Priority & Lead Aim
      // ====================================================================
      if (mode == WorldSettings.TUMBLE) {
         // Find nearest rival on the same or adjacent terrace (either human player or another TumbleBot)
         double targX = 0.5;
         double targY = bot.y;
         double targZ = 0.5;
         double targVx = 0.0;
         double targVz = 0.0;
         double bestDist = 999.0;
         boolean hasRival = false;

         if (player != null && mobs.targetable(player) && Math.abs(player.y - bot.y) < 4.5) {
            double d = Math.hypot(player.x - bot.x, player.z - bot.z);
            if (d < bestDist) {
               bestDist = d;
               targX = player.x;
               targY = player.y;
               targZ = player.z;
               targVx = player.vx;
               targVz = player.vz;
               hasRival = true;
            }
         }
         for (Net319.Peer peer : Net319.peers().values()) {
            if (peer != null && !peer.spectator && !peer.eliminated && peer.health > 0 && peer.gameMode != 1 && peer.gameMode != 3 && Math.abs(peer.y - bot.y) < 4.5) {
               double d = Math.hypot(peer.x - bot.x, peer.z - bot.z);
               if (d < bestDist) {
                  bestDist = d;
                  targX = peer.x;
                  targY = peer.y;
                  targZ = peer.z;
                  targVx = peer.x - peer.px;
                  targVz = peer.z - peer.pz;
                  hasRival = true;
               }
            }
         }
         for (Mobs.Mob other : mobs.list) {
            if (other != null && other != bot && !other.dead && Mobs.hostile(other.type) && Math.abs(other.y - bot.y) < 4.5) {
               double d = Math.hypot(other.x - bot.x, other.z - bot.z);
               if (d < bestDist) {
                  bestDist = d;
                  targX = other.x;
                  targY = other.y;
                  targZ = other.z;
                  targVx = other.vx;
                  targVz = other.vz;
                  hasRival = true;
               }
            }
         }

         // Predict where the rival will step in ~5 ticks for cut-off trenching & snowball lead shots!
         double leadX = targX + Math.max(-1.8, Math.min(1.8, targVx * 5.5));
         double leadZ = targZ + Math.max(-1.8, Math.min(1.8, targVz * 5.5));

         double safeX = bot.x * 0.65;
         double safeZ = bot.z * 0.65;
         if (hasRival && bestDist > 4.4) {
            safeX = leadX;
            safeZ = leadZ;
         } else if (hasRival && bestDist < 2.8) {
            // Strafe around close rival so bot doesn't get easily spleefed
            if (bot.age % 30 == 0) {
               bot.strafeDir = -bot.strafeDir;
            }
            double ang = Math.atan2(bot.z - targZ, bot.x - targX) + 0.9 * bot.strafeDir;
            safeX = targX + Math.cos(ang) * 4.2;
            safeZ = targZ + Math.sin(ang) * 4.2;
         }
         steerBotSafely3D(bot, w, safeX, targY, safeZ, out, 1.22);

         if (hasRival) {
            out.headYaw = Mobs.angleTo(leadX - bot.x, leadZ - bot.z);
            out.headPitch = 18.0F;
            // Prefer digging the predicted lead block first; fall back to block directly under rival's feet
            int rbx = (int)Math.floor(leadX);
            int rby = (int)Math.floor(targY) - 1;
            int rbz = (int)Math.floor(leadZ);
            if (w.getBlock(rbx, rby, rbz) == 0) {
               rbx = (int)Math.floor(targX);
               rbz = (int)Math.floor(targZ);
            }

            // Close-range Shovel Spleef: ONLY if the bot has a Shovel in botWeaponItem[botSlot]!
            if (bestDist <= 4.8 && bot.cd <= 0 && botWeaponItem[botSlot] != 0) {
               if (isTumbleLayerBlock(rbx, rby, rbz) && !(rbx == 0 && rby == Y_DECK && rbz == 0)
                  && (Math.abs(rbx - (int)Math.floor(bot.x)) > 1 || Math.abs(rbz - (int)Math.floor(bot.z)) > 1)) {
                  int bid = w.getBlock(rbx, rby, rbz);
                  if (bid != 0 && !Blocks.isFluid(bid)) {
                     bot.cd = 12;
                     bot.attackAnim = 8;
                     SoundEngine.playSwing(w, bot.x, bot.y + 1.2, bot.z);
                     if (mobs.g != null) {
                        mobs.g.spawnBreakParticles(rbx, rby, rbz, bid, 8);
                     }
                     SoundEngine.playBlockBreak(w, bid, rbx, rby, rbz);
                     w.setBlock(rbx, rby, rbz, 0);
                  }
               }
            }
            // Medium-range Snowball Spleef: ONLY if the bot has snowballs left in botArrows[botSlot]!
            else if (bestDist > 4.0 && bestDist <= 16.5 && bot.cd <= 0 && botArrows[botSlot] > 0) {
               if (isTumbleLayerBlock(rbx, rby, rbz) && !(rbx == 0 && rby == Y_DECK && rbz == 0)) {
                  int bid = w.getBlock(rbx, rby, rbz);
                  if (bid != 0 && !Blocks.isFluid(bid)) {
                     botArrows[botSlot]--;
                     bot.cd = 20;
                     bot.attackAnim = 8;
                     SoundEngine.playThrow(w, bot.x, bot.y + 1.4, bot.z);
                     if (mobs.g != null) {
                        mobs.g.spawnBreakParticles((int)Math.floor((bot.x + leadX) * 0.5), rby + 2, (int)Math.floor((bot.z + leadZ) * 0.5), Blocks.SNOW, 4);
                        mobs.g.spawnBreakParticles(rbx, rby, rbz, bid, 8);
                     }
                     SoundEngine.playBlockBreak(w, bid, rbx, rby, rbz);
                     w.setBlock(rbx, rby, rbz, 0);
                  }
               }
            }
         }
         return out;
      }

      // ====================================================================
      //  2. BATTLE AI: Surroundings-Aware Chest/Item Looting, Dynamic Bridging, Vertical Build-Up & Combat
      // ====================================================================
      if (mode == WorldSettings.BATTLE) {
         Mobs.Mob rivalBot = null;
         int targetPeerId = -1;
         double targX = 0.5;
         double targY = Y_DECK + 1.0;
         double targZ = 0.5;
         double targVx = 0.0;
         double targVz = 0.0;
         double bestDist = 999.0;
         boolean targetIsPlayer = false;

         if (player != null && mobs.targetable(player)) {
            double d = Math.hypot(player.x - bot.x, player.z - bot.z);
            if (d < bestDist) {
               bestDist = d;
               targX = player.x;
               targY = player.y;
               targZ = player.z;
               targVx = player.vx;
               targVz = player.vz;
               targetIsPlayer = true;
               targetPeerId = -1;
            }
         }
         for (Net319.Peer peer : Net319.peers().values()) {
            if (peer != null && !peer.spectator && !peer.eliminated && peer.health > 0 && peer.gameMode != 1 && peer.gameMode != 3) {
               double d = Math.hypot(peer.x - bot.x, peer.z - bot.z);
               if (d < bestDist) {
                  bestDist = d;
                  targX = peer.x;
                  targY = peer.y;
                  targZ = peer.z;
                  targVx = peer.x - peer.px;
                  targVz = peer.z - peer.pz;
                  targetIsPlayer = false;
                  targetPeerId = peer.id;
                  rivalBot = null;
               }
            }
         }
         for (Mobs.Mob other : mobs.list) {
            if (other != null && other != bot && !other.dead && Mobs.hostile(other.type)) {
               double d = Math.hypot(other.x - bot.x, other.z - bot.z);
               if (d < bestDist) {
                  bestDist = d;
                  rivalBot = other;
                  targX = other.x;
                  targY = other.y;
                  targZ = other.z;
                  targVx = other.vx;
                  targVz = other.vz;
                  targetIsPlayer = false;
                  targetPeerId = -1;
               }
            }
         }

         // Check surroundings: if it is safe (or during Grace Period, or if the bot needs blocks/weapons), loot nearby chests & dropped items!
         double effectiveEnemyDist = (battleGraceTicks > 0 || (botWeaponItem[botSlot] == 0 && botRangedItem[botSlot] == 0)) ? 999.0 : bestDist;
         double[] lootGoal = trySmartLootSurroundings(mobs, bot, w, player, -1, effectiveEnemyDist, targX, targZ);
         if (lootGoal != null) {
            double[] wpLoot = nextArenaBridgeWaypoint(w, bot.x, bot.z, lootGoal[0], lootGoal[2]);
            bridgeTowardTarget(bot, w, wpLoot[0], wpLoot[1], Blocks.STONEBRICK);
            tryBotBuildUp(bot, w, wpLoot[0], lootGoal[1], wpLoot[1], Blocks.STONEBRICK);
            steerBotSafely3D(bot, w, wpLoot[0], lootGoal[1], wpLoot[1], out, 1.24);
            return out;
         }

         // During Grace Period (first 15s) or if unequipped and safe, advance toward Mid / Center Feast chests!
         if (battleGraceTicks > 0 || (bot.variant == 0 && bestDist > 10.0)) {
            double cx = 0.5;
            double cz = 0.5;
            if (Math.hypot(bot.x, bot.z) > 22.0 && Math.abs(Math.abs(bot.x) - Math.abs(bot.z)) > 8.0) {
               double bestC = 999.0;
               for (int[] m : SKYWARS_MIDS) {
                  if (hasChestItemsAt(w, m[0], Y_DECK + 1, m[1])) {
                     double d = Math.hypot(bot.x - m[0], bot.z - m[1]);
                     if (d < bestC) {
                        bestC = d;
                        cx = m[0] + 0.5;
                        cz = m[1] + 0.5;
                     }
                  }
               }
            }
            double[] wp = nextArenaBridgeWaypoint(w, bot.x, bot.z, cx, cz);
            bridgeTowardTarget(bot, w, wp[0], wp[1], Blocks.STONEBRICK);
            steerBotSafely3D(bot, w, wp[0], Y_DECK + 1.0, wp[1], out, 1.24);
            return out;
         }

         // Showdown active: if outside center courtyard, urgently sprint & bridge to center!
         if (showdownActive && Math.hypot(bot.x, bot.z) > 16.0) {
            double[] wp = nextArenaBridgeWaypoint(w, bot.x, bot.z, 0.5, 0.5);
            bridgeTowardTarget(bot, w, wp[0], wp[1], Blocks.STONEBRICK);
            steerBotSafely3D(bot, w, wp[0], Y_DECK + 1.0, wp[1], out, 1.34);
            return out;
         }

         double[] wp = nextArenaBridgeWaypoint(w, bot.x, bot.z, targX, targZ);
         bridgeTowardTarget(bot, w, wp[0], wp[1], Blocks.STONEBRICK);
         tryBotBuildUp(bot, w, targX, targY, targZ, Blocks.STONEBRICK);
         boolean hasBowAndAmmo = botRangedItem[botSlot] != 0 && botArrows[botSlot] > 0;
         if (hasBowAndAmmo && bestDist < 11.5 && bestDist > 3.5 && wp[0] == targX && wp[1] == targZ) {
            // Archer strafes at mid-range and seeks high ground while firing bow
            if (bot.age % 40 == 0) {
               bot.strafeDir = -bot.strafeDir;
            }
            double ang = Math.atan2(bot.z - targZ, bot.x - targX) + 0.95 * bot.strafeDir;
            steerBotSafely3D(bot, w, targX + Math.cos(ang) * 7.8, targY + 2.0, targZ + Math.sin(ang) * 7.8, out, 1.02);
         } else {
            double moveX = wp[0];
            double moveZ = wp[1];
            if (bestDist < 5.5 && bestDist > 1.8 && wp[0] == targX && wp[1] == targZ) {
               if (bot.age % 25 == 0) {
                  bot.strafeDir = -bot.strafeDir;
               }
               double perp = Math.atan2(targZ - bot.z, targX - bot.x) + (Math.PI * 0.35) * bot.strafeDir;
               moveX = bot.x + Math.cos(perp) * 2.5;
               moveZ = bot.z + Math.sin(perp) * 2.5;
            }
            if (bestDist < 3.0 && bot.onGround && bot.age % 18 == 0) {
               bot.vy = Mobs.JUMP; // Jump-crit!
            }
            steerBotSafely3D(bot, w, moveX, targY, moveZ, out, bestDist < 7.5 ? 1.28 : 1.18);
         }

         double leadX = targX + targVx * Math.min(8.0, bestDist * 0.45);
         double leadZ = targZ + targVz * Math.min(8.0, bestDist * 0.45);
         out.headYaw = Mobs.angleTo(leadX - bot.x, leadZ - bot.z);
         out.headPitch = (float)(-Math.toDegrees(Math.atan2((targY + 1.0) - (bot.y + 1.4), Math.max(0.5, bestDist))));

         boolean isCrit = !bot.onGround && bot.vy < -0.05;
         int wep = botWeaponItem[botSlot];
         int dmg = (wep != 0 ? Math.max(3, (int)Items.attackDamage(wep) / 2) : 1) + (isCrit ? 1 : 0);
         if (bestDist < 2.25 && Math.abs(targY - bot.y) < 2.4 && bot.cd <= 0) {
            bot.cd = 16;
            bot.attackAnim = 8;
            SoundEngine.playSwing(w, bot.x, bot.y + 1.2, bot.z);
            SoundEngine.playHit(w, targX, targY + 1.0, targZ, isCrit);
            if (targetIsPlayer && mobs.g != null) {
               mobs.g.damagePlayer(dmg, bot.x, bot.z);
            } else if (targetPeerId > 0) {
               Net319.sendHit(targetPeerId, dmg, bot.x, bot.z, botDisplayName(bot));
            } else if (rivalBot != null) {
               mobs.hurtMob(rivalBot, dmg, bot.x, bot.z, 0.44, false);
               if (rivalBot.dead && mobs.g != null) {
                  mobs.g.say("\u00a76[Battle] \u00a7e" + botDisplayName(bot) + " \u00a77eliminated \u00a7c" + botDisplayName(rivalBot) + "\u00a77!");
               }
            }
         } else if (botGrenades[botSlot] > 0 && bestDist >= 5.0 && bestDist <= 14.0 && bot.cd <= 0 && bot.age % 50 == 0
            && mobs.hasLOS(w, bot.x, bot.y + 1.5, bot.z, targX, targY + 1.0, targZ)) {
            botGrenades[botSlot]--;
            bot.cd = 28;
            bot.attackAnim = 8;
            double dx = (leadX - bot.x) / bestDist;
            double dz = (leadZ - bot.z) / bestDist;
            w.throwTntGrenade(bot.x + dx * 0.7, bot.y + 0.95, bot.z + dz * 0.7, dx * 0.65, 0.30, dz * 0.65, 45);
         } else if (hasBowAndAmmo && bestDist >= 2.5 && bestDist < 18.0 && mobs.hasLOS(w, bot.x, bot.y + 1.5, bot.z, targX, targY + 1.0, targZ)) {
            if (bot.cd > 0 && bot.cd <= 16) {
               botBowDrawTicks[botSlot] = 17 - bot.cd;
            } else if (bot.cd <= 0) {
               botArrows[botSlot]--;
               bot.cd = 32;
               botBowDrawTicks[botSlot] = 0;
               SoundEngine.playThrow(w, bot.x, bot.y + 1.5, bot.z);
               double arcY = targY + 1.1 + bestDist * 0.04;
               mobs.shootAtPos(bot, leadX, arcY, leadZ);
            }
         }
         return out;
      }

      // ====================================================================
      //  3. BEDWARS AI: Surroundings-Safe Resource/Chest Looting, Dynamic Bridging, Vertical Build-Up & Bed Raiding
      // ====================================================================
      if (mode == WorldSettings.BEDWARS) {
         int myBotTeam = botTeamIndex(bot);
         if (myBotTeam < 0 || myBotTeam >= 4) {
            return null;
         }
         int role = bot.variant & 1; // 0 = Defender / Support Raider, 1 = Primary Bridger / Bed Raider
         int woolId = Blocks.WOOL0 + BEDWARS_COLORS[myBotTeam];
         int baseX = BEDWARS_BASES[myBotTeam][0];
         int baseZ = BEDWARS_BASES[myBotTeam][1];

         int humanTeam = mobs.g != null ? getPlayerTeam(mobs.g) : 0;
         Mobs.Mob enemyBot = null;
         int enemyPeerId = -1;
         double enemyX = baseX + 0.5;
         double enemyY = Y_DECK + 1.0;
         double enemyZ = baseZ + 0.5;
         double enemyDist = 999.0;
         boolean enemyIsPlayer = false;

         if (player != null && mobs.targetable(player) && humanTeam != myBotTeam) {
            double d = Math.hypot(player.x - bot.x, player.z - bot.z);
            if (d < enemyDist) {
               enemyDist = d;
               enemyX = player.x;
               enemyY = player.y;
               enemyZ = player.z;
               enemyIsPlayer = true;
               enemyPeerId = -1;
            }
         }
         for (Net319.Peer peer : Net319.peers().values()) {
            if (peer != null && !peer.spectator && !peer.eliminated && peer.health > 0 && peer.gameMode != 1 && peer.gameMode != 3 && peer.team != myBotTeam) {
               double d = Math.hypot(peer.x - bot.x, peer.z - bot.z);
               if (d < enemyDist) {
                  enemyDist = d;
                  enemyX = peer.x;
                  enemyY = peer.y;
                  enemyZ = peer.z;
                  enemyIsPlayer = false;
                  enemyPeerId = peer.id;
                  enemyBot = null;
               }
            }
         }
         for (Mobs.Mob other : mobs.list) {
            if (other != null && other != bot && !other.dead && Mobs.hostile(other.type)) {
               int ot = botTeamIndex(other);
               if (ot >= 0 && ot != myBotTeam) {
                  double d = Math.hypot(other.x - bot.x, other.z - bot.z);
                  if (d < enemyDist) {
                     enemyDist = d;
                     enemyBot = other;
                     enemyX = other.x;
                     enemyY = other.y;
                     enemyZ = other.z;
                     enemyIsPlayer = false;
                     enemyPeerId = -1;
                  }
               }
            }
         }

         // 3b. Defender Bot (#0): Fortifies own team Bed ONLY if botBlocks[botSlot] > 0!
         boolean ownBedIntact = isTeamBedIntact(w, myBotTeam);
         boolean ownBedFortified = true;
         if (ownBedIntact) {
            int[] bf = getTeamBedFoot(myBotTeam);
            int[] bh = getTeamBedHead(myBotTeam);
            int[][] innerShell = new int[][]{
               {bf[0], bf[1] + 1, bf[2]}, {bh[0], bh[1] + 1, bh[2]},
               {bf[0] + 1, bf[1], bf[2]}, {bf[0] - 1, bf[1], bf[2]},
               {bh[0], bh[1], bh[2] + 1}, {bh[0], bh[1], bh[2] - 1}
            };
            for (int[] co : innerShell) {
               if (w.getBlock(co[0], co[1], co[2]) == 0) {
                  ownBedFortified = false;
                  if (role == 0 && botBlocks[botSlot] > 0 && Math.hypot(bot.x - bf[0], bot.z - bf[2]) <= 8.0 && bot.age % 16 == 3) {
                     int defBlock = (bot.age > 360 || botGearTier[botSlot] >= 2) ? Blocks.END_STONE : woolId;
                     w.setBlock(co[0], co[1], co[2], defBlock);
                     botBlocks[botSlot]--;
                     bot.attackAnim = 6;
                     botHoldingBlockTicks[botSlot] = 12;
                     SoundEngine.playBlockPlace(w, defBlock, co[0], co[1], co[2]);
                  }
                  break;
               }
            }
         }

         // 3c. Background bridge progression from team base toward Center (0, 0) ONLY if botBlocks[botSlot] > 0!
         if (role == 1 && botBlocks[botSlot] > 0 && bot.age % 10 == 0) {
            int dirX = Integer.compare(0, baseX);
            int dirZ = Integer.compare(0, baseZ);
            int step = bwTeamBridgeStep[myBotTeam];
            if (step < 32) {
               int brX = baseX + dirX * (6 + step);
               int brZ = baseZ + dirZ * (6 + step);
               if (w.chunkAt(brX, brZ) != null) {
                  if (w.getBlock(brX, Y_DECK, brZ) == 0) {
                     w.setBlock(brX, Y_DECK, brZ, woolId);
                     botBlocks[botSlot]--;
                     bot.attackAnim = 6;
                     botHoldingBlockTicks[botSlot] = 10;
                     SoundEngine.playBlockPlace(w, woolId, brX, Y_DECK, brZ);
                  }
                  int sideX = brX + (dirX == 0 ? 1 : 0);
                  int sideZ = brZ + (dirZ == 0 ? 1 : 0);
                  if (botBlocks[botSlot] > 0 && w.getBlock(sideX, Y_DECK, sideZ) == 0) {
                     w.setBlock(sideX, Y_DECK, sideZ, woolId);
                     botBlocks[botSlot]--;
                  }
                  bwTeamBridgeStep[myBotTeam] = step + 1;
               }
            }
         }

         // 3d. Surroundings-Safe Looting: if no enemy is threatening (or if out of blocks!), collect nearby generator drops or chests!
         if (enemyDist > 8.5 || botBlocks[botSlot] <= 2) {
            double[] lootGoal = trySmartLootSurroundings(mobs, bot, w, player, myBotTeam, enemyDist, enemyX, enemyZ);
            if (lootGoal != null && (role == 0 || botBlocks[botSlot] <= 2 || Math.hypot(lootGoal[0] - bot.x, lootGoal[2] - bot.z) <= 8.0)) {
               bridgeTowardTarget(bot, w, lootGoal[0], lootGoal[2], woolId);
               tryBotBuildUp(bot, w, lootGoal[0], lootGoal[1], lootGoal[2], woolId);
               steerBotSafely3D(bot, w, lootGoal[0], lootGoal[1], lootGoal[2], out, 1.22);
               return out;
            }
         }

         // 3e. Decide Strategic Objective: Defend Base vs Raid Enemy Beds!
         double goalX = baseX + 0.5;
         double goalY = Y_DECK + 1.0;
         double goalZ = baseZ + 0.5;

         int[] ownBf = getTeamBedFoot(myBotTeam);
         boolean baseUnderAttack = ownBedIntact && Math.hypot(enemyX - ownBf[0], enemyZ - ownBf[2]) <= 15.0;

         if (role == 0 && ownBedIntact && (!ownBedFortified || baseUnderAttack)) {
            if (baseUnderAttack || enemyDist <= 14.0) {
               goalX = enemyX;
               goalY = enemyY;
               goalZ = enemyZ;
            } else {
               double patrolAng = (bot.age * 0.05) + myBotTeam;
               goalX = ownBf[0] + 0.5 + Math.cos(patrolAng) * 2.2;
               goalZ = ownBf[2] + 0.5 + Math.sin(patrolAng) * 2.2;
            }
         } else {
            int targetBedTeam = -1;
            double bestBedScore = 999.0;
            for (int t = 0; t < 4; t++) {
               if (t != myBotTeam && isTeamBedIntact(w, t)) {
                  int[] bf = getTeamBedFoot(t);
                  double d = Math.hypot(bf[0] + 0.5 - bot.x, bf[2] + 0.5 - bot.z);
                  double score = d - (t == humanTeam ? 8.0 : 0.0);
                  if (score < bestBedScore) {
                     bestBedScore = score;
                     targetBedTeam = t;
                  }
               }
            }

            if (enemyDist <= 7.0) {
               goalX = enemyX;
               goalY = enemyY;
               goalZ = enemyZ;
            } else if (targetBedTeam >= 0) {
               int[] bf = getTeamBedFoot(targetBedTeam);
               int[] bh = getTeamBedHead(targetBedTeam);
               double distToBed = Math.min(
                  Math.hypot(bf[0] + 0.5 - bot.x, bf[2] + 0.5 - bot.z),
                  Math.hypot(bh[0] + 0.5 - bot.x, bh[2] + 0.5 - bot.z)
               );
               double distToCenter = Math.hypot(bot.x, bot.z);
               boolean inTargetCorridor = (bf[0] == 0 && Math.abs(bot.x) <= 6.5 && (bot.z * bf[2]) >= -4.0)
                  || (bf[2] == 0 && Math.abs(bot.z) <= 6.5 && (bot.x * bf[0]) >= -4.0);

               if (distToCenter > 10.5 && !inTargetCorridor) {
                  goalX = 0.5;
                  goalZ = 0.5;
               } else {
                  goalX = bf[0] + 0.5;
                  goalZ = bf[2] + 0.5;
               }

               if (distToBed <= 4.8 && bot.cd <= 0) {
                  bot.cd = 20;
                  bot.attackAnim = 8;
                  out.headYaw = Mobs.angleTo(bf[0] + 0.5 - bot.x, bf[2] + 0.5 - bot.z);
                  out.headPitch = 22.0F;
                  int[][] defenseChecks = new int[][]{
                     {bf[0], bf[1] + 1, bf[2]}, {bh[0], bh[1] + 1, bh[2]},
                     {bf[0], bf[1] + 2, bf[2]}, {bh[0], bh[1] + 2, bh[2]},
                     {bf[0] + 1, bf[1], bf[2]}, {bf[0] - 1, bf[1], bf[2]},
                     {bh[0], bh[1], bh[2] + 1}, {bh[0], bh[1], bh[2] - 1}
                  };
                  boolean minedDefense = false;
                  for (int[] dc : defenseChecks) {
                     int cid = w.getBlock(dc[0], dc[1], dc[2]);
                     if (cid != 0 && !Decor315.isBed(cid) && cid != Blocks.BEDROCK) {
                        if (mobs.g != null) {
                           mobs.g.spawnBreakParticles(dc[0], dc[1], dc[2], cid, 8);
                        }
                        SoundEngine.playBlockBreak(w, cid, dc[0], dc[1], dc[2]);
                        w.setBlock(dc[0], dc[1], dc[2], 0);
                        minedDefense = true;
                        break;
                     }
                  }
                  if (!minedDefense) {
                     SoundEngine.playBlockBreak(w, Blocks.PLANKS, bf[0], bf[1], bf[2]);
                     onBedDestroyed(mobs.g, w, targetBedTeam, botDisplayName(bot), true);
                  }
               }
            } else if (enemyDist < 200.0) {
               double distToCenter = Math.hypot(bot.x, bot.z);
               if (distToCenter > 11.0 && (bot.x * enemyX + bot.z * enemyZ) <= 0.0) {
                  goalX = 0.5;
                  goalZ = 0.5;
               } else {
                  goalX = enemyX;
                  goalY = enemyY;
                  goalZ = enemyZ;
               }
            }
         }

         bridgeTowardTarget(bot, w, goalX, goalZ, woolId);
         tryBotBuildUp(bot, w, goalX, goalY, goalZ, woolId);
         steerBotSafely3D(bot, w, goalX, goalY, goalZ, out, 1.22);

         // Melee or Ranged combat against enemy player or enemy team bot
         int wep = botWeaponItem[botSlot];
         if (enemyDist < 2.35 && Math.abs(enemyY - bot.y) < 2.4 && bot.cd <= 0 && wep != 0) {
            bot.cd = 16;
            bot.attackAnim = 8;
            int dmg = Math.max(3, (int)Items.attackDamage(wep) / 2) + (hasTeamSharpness(myBotTeam) ? 2 : 0);
            SoundEngine.playSwing(w, bot.x, bot.y + 1.2, bot.z);
            SoundEngine.playHit(w, enemyX, enemyY + 1.0, enemyZ, false);
            if (enemyIsPlayer && mobs.g != null) {
               mobs.g.damagePlayer(dmg, bot.x, bot.z);
            } else if (enemyPeerId > 0) {
               Net319.sendHit(enemyPeerId, dmg, bot.x, bot.z, botDisplayName(bot));
            } else if (enemyBot != null) {
               mobs.hurtMob(enemyBot, dmg, bot.x, bot.z, 0.42, false);
            }
         } else if (botRangedItem[botSlot] != 0 && botArrows[botSlot] > 0 && enemyDist >= 3.5 && enemyDist < 17.0
            && mobs.hasLOS(w, bot.x, bot.y + 1.5, bot.z, enemyX, enemyY + 1.0, enemyZ)) {
            if (bot.cd > 0 && bot.cd <= 14) {
               botBowDrawTicks[botSlot] = 15 - bot.cd;
            } else if (bot.cd <= 0) {
               botArrows[botSlot]--;
               bot.cd = 30;
               botBowDrawTicks[botSlot] = 0;
               SoundEngine.playThrow(w, bot.x, bot.y + 1.5, bot.z);
               mobs.shootAtPos(bot, enemyX, enemyY + 1.1, enemyZ);
            }
         }
         return out;
      }

      // ====================================================================
      //  4. CS2 STRIKE AI: Tactical Gunfights (Pistols/Machine Guns/Snipers), TNT Grenades, Vertical Build-Up & C4 Bomb Plant/Defuse!
      // ====================================================================
      if (mode == WorldSettings.CS2) {
         int myCsTeam = botTeamIndex(bot); // 0 = CT (Blue), 1 = T (Red)
         if (myCsTeam < 0) {
            myCsTeam = 1;
         }
         int humanCsTeam = mobs.g != null ? (getPlayerTeam(mobs.g) & 1) : 0;
         Mobs.Mob enemyBot = null;
         int enemyPeerId = -1;
         double enemyX = 0.5;
         double enemyY = Y_DECK + 1.0;
         double enemyZ = 0.5;
         double enemyVx = 0.0;
         double enemyVz = 0.0;
         double enemyDist = 999.0;
         boolean enemyIsPlayer = false;

         if (player != null && mobs.targetable(player) && humanCsTeam != myCsTeam) {
            double d = Math.hypot(player.x - bot.x, player.z - bot.z);
            if (d < enemyDist) {
               enemyDist = d;
               enemyX = player.x;
               enemyY = player.y;
               enemyZ = player.z;
               enemyVx = player.vx;
               enemyVz = player.vz;
               enemyIsPlayer = true;
            }
         }
         for (Net319.Peer peer : Net319.peers().values()) {
            if (peer != null && !peer.spectator && !peer.eliminated && peer.health > 0 && peer.gameMode != 1 && peer.gameMode != 3 && (peer.team & 1) != myCsTeam) {
               double d = Math.hypot(peer.x - bot.x, peer.z - bot.z);
               if (d < enemyDist) {
                  enemyDist = d;
                  enemyX = peer.x;
                  enemyY = peer.y;
                  enemyZ = peer.z;
                  enemyVx = peer.x - peer.px;
                  enemyVz = peer.z - peer.pz;
                  enemyIsPlayer = false;
                  enemyPeerId = peer.id;
                  enemyBot = null;
               }
            }
         }
         for (Mobs.Mob other : mobs.list) {
            if (other != null && other != bot && !other.dead && Mobs.hostile(other.type)) {
               int ot = botTeamIndex(other);
               if (ot >= 0 && ot != myCsTeam) {
                  double d = Math.hypot(other.x - bot.x, other.z - bot.z);
                  if (d < enemyDist) {
                     enemyDist = d;
                     enemyBot = other;
                     enemyX = other.x;
                     enemyY = other.y;
                     enemyZ = other.z;
                     enemyVx = other.vx;
                     enemyVz = other.vz;
                     enemyIsPlayer = false;
                     enemyPeerId = -1;
                  }
               }
            }
         }

         // Strategic Bomb Site Objective: Site A (-22, 0) or Site B (+22, 0)
         int prefSite = bot.variant & 1;
         int activeSite = cs2BombPlanted ? cs2BombSite : prefSite;
         double siteX = (activeSite == 0 ? -22.0 : 22.0) + 0.5;
         double siteZ = 0.5;
         double distToSite = Math.hypot(bot.x - siteX, bot.z - siteZ);

         if (myCsTeam == 1 && !cs2BombPlanted && !cs2RoundOver && distToSite <= 3.5 && enemyDist > 5.0 && w.tickCount > 120L) {
            // Terrorist AI plants the C4 Bomb at Site A or Site B!
            plantCs2Bomb(mobs.g, activeSite == 0 ? 1 : 2, botDisplayName(bot), true);
            bot.attackAnim = 8;
         } else if (myCsTeam == 0 && cs2BombPlanted && !cs2RoundOver && distToSite <= 3.8 && enemyDist > 4.5) {
            // Counter-Terrorist AI defuses the planted C4 Bomb!
            cs2DefuseTicks += 2;
            bot.attackAnim = 6;
            if (cs2DefuseTicks >= 100) {
               defuseCs2Bomb(mobs.g, botDisplayName(bot), true);
            }
         }

         double goalX = siteX;
         double goalY = Y_DECK + 1.0;
         double goalZ = siteZ;
         boolean hasLOS = enemyDist < 28.0 && mobs.hasLOS(w, bot.x, bot.y + 1.5, bot.z, enemyX, enemyY + 1.0, enemyZ);
         if (hasLOS && (!cs2BombPlanted || myCsTeam == 1 || enemyDist < 10.0)) {
            if (botRangedItem[botSlot] != 0 && botArrows[botSlot] > 0 && enemyDist > 4.5 && enemyDist < 15.0) {
               // Tactical strafe shooting!
               if (bot.age % 28 == 0) {
                  bot.strafeDir = -bot.strafeDir;
               }
               double ang = Math.atan2(bot.z - enemyZ, bot.x - enemyX) + 0.85 * bot.strafeDir;
               goalX = enemyX + Math.cos(ang) * 8.5;
               goalY = enemyY;
               goalZ = enemyZ + Math.sin(ang) * 8.5;
            } else {
               goalX = enemyX;
               goalY = enemyY;
               goalZ = enemyZ;
            }
         }

         int blockMat = myCsTeam == 0 ? (Blocks.CONCRETE0 + 11) : (Blocks.CONCRETE0 + 14);
         tryBotBuildUp(bot, w, goalX, goalY, goalZ, blockMat);
         steerBotSafely3D(bot, w, goalX, goalY, goalZ, out, 1.24);

         if (enemyDist < 32.0) {
            double leadX = enemyX + enemyVx * Math.min(6.0, enemyDist * 0.32);
            double leadZ = enemyZ + enemyVz * Math.min(6.0, enemyDist * 0.32);
            out.headYaw = Mobs.angleTo(leadX - bot.x, leadZ - bot.z);
            out.headPitch = (float)(-Math.toDegrees(Math.atan2((enemyY + 1.0) - (bot.y + 1.45), Math.max(0.5, enemyDist))));

            // 1. Throw TNT Grenade if enemy is grouped/behind cover at medium range and bot has grenades in botGrenades[botSlot]!
            if (botGrenades[botSlot] > 0 && enemyDist >= 6.0 && enemyDist <= 16.5 && bot.cd <= 0 && (bot.age + botSlot * 11) % 65 == 0 && hasLOS) {
               botGrenades[botSlot]--;
               bot.cd = 24;
               bot.attackAnim = 8;
               double dx = (leadX - bot.x) / enemyDist;
               double dz = (leadZ - bot.z) / enemyDist;
               w.throwTntGrenade(bot.x + dx * 0.7, bot.y + 0.95, bot.z + dz * 0.7, dx * 0.68, 0.28, dz * 0.68, 42);
            }
            // 2. Fire Gun (Machine Gun / Pistol / Sniper Rifle) ONLY if botArrows[botSlot] > 0!
            else if (botRangedItem[botSlot] != 0 && botArrows[botSlot] > 0 && enemyDist >= 2.2 && enemyDist <= 28.0 && hasLOS && bot.cd <= 0) {
               botArrows[botSlot]--;
               int gun = botRangedItem[botSlot];
               int cd = gun == Redstone319.MACHINE_GUN ? 7 : (gun == Redstone319.SNIPER_RIFLE ? 28 : 14);
               int dmg = gun == Redstone319.SNIPER_RIFLE ? 14 : (gun == Redstone319.PISTOL ? 6 : 4);
               float spd = gun == Redstone319.SNIPER_RIFLE ? 3.2F : 2.3F;
               bot.cd = cd;
               bot.attackAnim = 5;
               botBowDrawTicks[botSlot] = 6;
               double d3 = Math.max(0.5, Math.sqrt((leadX - bot.x) * (leadX - bot.x) + ((enemyY + 1.1) - (bot.y + 1.45)) * ((enemyY + 1.1) - (bot.y + 1.45)) + (leadZ - bot.z) * (leadZ - bot.z)));
               mobs.shootGunArrow(
                  bot.x,
                  bot.y + 1.45,
                  bot.z,
                  (leadX - bot.x) / d3,
                  ((enemyY + 1.1) - (bot.y + 1.45)) / d3,
                  (leadZ - bot.z) / d3,
                  spd,
                  dmg,
                  false
               );
               SoundEngine.play3D(w, "bow_shoot", bot.x, bot.y + 1.45, bot.z, 0.85F, gun == Redstone319.MACHINE_GUN ? 1.55F : 1.30F);
            }
            // 3. Close-quarters Combat Knife strike!
            else if (enemyDist < 2.35 && Math.abs(enemyY - bot.y) < 2.4 && bot.cd <= 0 && botWeaponItem[botSlot] != 0) {
               bot.cd = 14;
               bot.attackAnim = 8;
               int dmg = 6;
               SoundEngine.playSwing(w, bot.x, bot.y + 1.2, bot.z);
               SoundEngine.playHit(w, enemyX, enemyY + 1.0, enemyZ, true);
               if (enemyIsPlayer && mobs.g != null) {
                  mobs.g.damagePlayer(dmg, bot.x, bot.z);
               } else if (enemyPeerId > 0) {
                  Net319.sendHit(enemyPeerId, dmg, bot.x, bot.z, botDisplayName(bot));
               } else if (enemyBot != null) {
                  mobs.hurtMob(enemyBot, dmg, bot.x, bot.z, 0.42, false);
               }
            }
         }
         return out;
      }

      // ====================================================================
      //  5. MURDER MYSTERY AI: Murderer Stalking/Knife Throw, Sheriff Investigation & Innocent Gold Looting!
      // ====================================================================
      if (mode == WorldSettings.MURDER) {
         int role = botMurderRole[botSlot];
         if (role == MURDER_ROLE_MURDERER) {
            // Murderer Bot: hunts the nearest living human player, peer, or Innocent/Sheriff bot!
            Mobs.Mob victimBot = null;
            int victimPeerId = -1;
            double vicX = 0.5;
            double vicY = Y_DECK + 1.0;
            double vicZ = 0.5;
            double vicDist = 999.0;
            boolean vicIsPlayer = false;

            if (player != null && mobs.targetable(player)) {
               double d = Math.hypot(player.x - bot.x, player.z - bot.z);
               if (d < vicDist) {
                  vicDist = d;
                  vicX = player.x;
                  vicY = player.y;
                  vicZ = player.z;
                  vicIsPlayer = true;
               }
            }
            for (Net319.Peer peer : Net319.peers().values()) {
               if (peer != null && !peer.spectator && !peer.eliminated && peer.health > 0 && peer.gameMode != 1 && peer.gameMode != 3) {
                  double d = Math.hypot(peer.x - bot.x, peer.z - bot.z);
                  if (d < vicDist) {
                     vicDist = d;
                     vicX = peer.x;
                     vicY = peer.y;
                     vicZ = peer.z;
                     vicIsPlayer = false;
                     victimPeerId = peer.id;
                     victimBot = null;
                  }
               }
            }
            for (Mobs.Mob other : mobs.list) {
               if (other != null && other != bot && !other.dead && Mobs.hostile(other.type)) {
                  double d = Math.hypot(other.x - bot.x, other.z - bot.z);
                  if (d < vicDist) {
                     vicDist = d;
                     victimBot = other;
                     vicX = other.x;
                     vicY = other.y;
                     vicZ = other.z;
                     vicIsPlayer = false;
                     victimPeerId = -1;
                  }
               }
            }

            steerBotSafely3D(bot, w, vicX, vicY, vicZ, out, vicDist < 7.0 ? 1.32 : 1.18);
            out.headYaw = Mobs.angleTo(vicX - bot.x, vicZ - bot.z);
            out.headPitch = (float)(-Math.toDegrees(Math.atan2((vicY + 1.0) - (bot.y + 1.45), Math.max(0.5, vicDist))));

            if (murderWeaponsDealt && botWeaponItem[botSlot] != 0) {
               if (vicDist < 2.35 && Math.abs(vicY - bot.y) < 2.4 && bot.cd <= 0) {
                  bot.cd = 18;
                  bot.attackAnim = 8;
                  SoundEngine.playSwing(w, bot.x, bot.y + 1.2, bot.z);
                  SoundEngine.playHit(w, vicX, vicY + 1.0, vicZ, true);
                  if (vicIsPlayer && mobs.g != null) {
                     mobs.g.damagePlayer(24, bot.x, bot.z);
                  } else if (victimPeerId > 0) {
                     Net319.sendHit(victimPeerId, 24, bot.x, bot.z, "The Murderer");
                  } else if (victimBot != null) {
                     mobs.hurtMob(victimBot, 24, bot.x, bot.z, 0.45, false);
                     if (victimBot.dead) {
                        onBotKilled(mobs.g, victimBot);
                     }
                  }
               } else if (vicDist >= 5.5 && vicDist <= 13.5 && bot.cd <= 0 && bot.age % 65 == 0
                  && mobs.hasLOS(w, bot.x, bot.y + 1.5, bot.z, vicX, vicY + 1.0, vicZ)) {
                  // Thrown Murder Knife!
                  bot.cd = 50;
                  bot.attackAnim = 8;
                  double d3 = Math.max(0.5, Math.hypot(vicX - bot.x, vicZ - bot.z));
                  mobs.shootGunArrow(bot.x, bot.y + 1.45, bot.z, (vicX - bot.x) / d3, ((vicY + 1.0) - (bot.y + 1.45)) / d3, (vicZ - bot.z) / d3, 1.85F, 24, false);
                  SoundEngine.playThrow(w, bot.x, bot.y + 1.45, bot.z);
               }
            }
            return out;
         }

         // Sheriff or Innocent Bot: locate the Murderer (if known/nearby) and collect Gold Ingots or Dropped Sheriff Gun!
         double murdX = 0.5;
         double murdY = Y_DECK + 1.0;
         double murdZ = 0.5;
         double murdDist = 999.0;
         boolean murdIsPlayer = false;
         Mobs.Mob murdBot = null;

         if (myMurderRole == MURDER_ROLE_MURDERER && player != null && mobs.targetable(player)) {
            double d = Math.hypot(player.x - bot.x, player.z - bot.z);
            if (d < 15.0 && mobs.hasLOS(w, bot.x, bot.y + 1.5, bot.z, player.x, player.y + 1.0, player.z)) {
               murdDist = d;
               murdX = player.x;
               murdY = player.y;
               murdZ = player.z;
               murdIsPlayer = true;
            }
         }
         for (Mobs.Mob other : mobs.list) {
            if (other != null && other != bot && !other.dead && Mobs.hostile(other.type)) {
               int oSlot = Math.abs(other.netId) & 31;
               if (botMurderRole[oSlot] == MURDER_ROLE_MURDERER) {
                  double d = Math.hypot(other.x - bot.x, other.z - bot.z);
                  if (d < 15.0 && mobs.hasLOS(w, bot.x, bot.y + 1.5, bot.z, other.x, other.y + 1.0, other.z)) {
                     murdDist = d;
                     murdBot = other;
                     murdX = other.x;
                     murdY = other.y;
                     murdZ = other.z;
                     murdIsPlayer = false;
                  }
               }
            }
         }

         boolean hasGun = botRangedItem[botSlot] != 0 && botArrows[botSlot] > 0;
         if (hasGun && murderWeaponsDealt && murdDist < 15.0) {
            out.headYaw = Mobs.angleTo(murdX - bot.x, murdZ - bot.z);
            out.headPitch = (float)(-Math.toDegrees(Math.atan2((murdY + 1.0) - (bot.y + 1.45), Math.max(0.5, murdDist))));
            if (bot.cd <= 0) {
               botArrows[botSlot]--;
               bot.cd = 22;
               bot.attackAnim = 6;
               botBowDrawTicks[botSlot] = 8;
               double d3 = Math.max(0.5, Math.hypot(murdX - bot.x, murdZ - bot.z));
               mobs.shootGunArrow(bot.x, bot.y + 1.45, bot.z, (murdX - bot.x) / d3, ((murdY + 1.0) - (bot.y + 1.45)) / d3, (murdZ - bot.z) / d3, 2.5F, 24, false);
               SoundEngine.play3D(w, "bow_shoot", bot.x, bot.y + 1.45, bot.z, 0.9F, 1.35F);
               if (!murdIsPlayer && murdBot != null && mobs.rnd.nextInt(2) == 0) {
                  mobs.hurtMob(murdBot, 24, bot.x, bot.z, 0.45, false);
                  if (murdBot.dead) {
                     onBotKilled(mobs.g, murdBot);
                  }
               }
            }
         }

         // Loot Gold Ingots or Dropped Sheriff Gun when safe!
         double[] lootGoal = trySmartLootSurroundings(mobs, bot, w, player, -1, murdDist, murdX, murdZ);
         if (lootGoal != null) {
            steerBotSafely3D(bot, w, lootGoal[0], lootGoal[1], lootGoal[2], out, 1.20);
            return out;
         }

         // Patrol Mansion halls or flee if the Murderer is chasing
         double patAng = (bot.age * 0.03) + bot.variant * 1.4;
         double patX = murdDist < 8.0 ? (bot.x - (murdX - bot.x) * 1.5) : (Math.cos(patAng) * 18.0);
         double patZ = murdDist < 8.0 ? (bot.z - (murdZ - bot.z) * 1.5) : (Math.sin(patAng) * 18.0);
         steerBotSafely3D(bot, w, patX, Y_DECK + 1.0, patZ, out, murdDist < 8.0 ? 1.28 : 1.05);
         return out;
      }

      return null;
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

   /** Runs per-tick waiting lobby countdown (and Post-Match Overview countdown) before the next match starts. */
   public static void tickWaitingLobby(Game g, World w) {
      if (g == null || w == null || w.settings == null || !w.settings.isMinigame()) {
         return;
      }
      // Ensure all 9x9 arena chunks (-4..4 x -4..4) are generated and resident while players wait in the lobby
      w.ensureArenaChunksLoaded();
      if (showingOverview) {
         if (overviewCountdownTicks > 0) {
            overviewCountdownTicks--;
            if (overviewCountdownTicks == 0) {
               enterInterRoundLobby(g);
            }
         }
         return;
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

      // Celebratory 1.5s victory/round-end transition before opening the Post-Match Overview screen!
      if (victoryTransitionTicks > 0) {
         victoryTransitionTicks--;
         if (victoryTransitionTicks == 0 && g != null) {
            openMatchOverview(g, pendingOverviewWon, pendingOverviewChamp, Net319.isHosting());
            return;
         }
      }

      // Enforce that a player who was eliminated in this round stays dead or in Spectator Mode until the next round starts!
      if (localEliminated && g != null && g.player != null && !g.player.dead && !g.player.spectator) {
         g.player.setMode(3);
      }

      if (playerGunCooldown > 0) {
         playerGunCooldown--;
      }

      // Fast void death so players don't fall endlessly below Y=-18
      if (g != null && g.player != null && !g.player.dead && !g.player.creative && !g.player.spectator && g.player.y < -18.0) {
         g.player.hurt(20);
      }

      // 4. CS2 STRIKE: Counter-Terrorists vs Terrorists, C4 Bomb Plant/Defuse, Tactical Guns & TNT Grenades!
      if (type == WorldSettings.CS2) {
         ensureRoundBotsSpawned(g, w);
         int myCsTeam = g != null ? (getPlayerTeam(g) & 1) : 0; // 0 = CT (Blue), 1 = T (Red)
         if (cs2BombPlanted && !cs2RoundOver) {
            if (cs2BombTimerTicks > 0) {
               cs2BombTimerTicks--;
               // Audible C4 beep that accelerates as detonation approaches!
               int beepInterval = cs2BombTimerTicks > 400 ? 30 : (cs2BombTimerTicks > 160 ? 15 : 6);
               if (cs2BombTimerTicks % beepInterval == 0) {
                  double bx = (cs2BombSite == 0 ? -22.0 : 22.0) + 0.5;
                  SoundEngine.play3D(w, "click_block", bx, Y_DECK + 1.5, 0.5, 0.95F, cs2BombTimerTicks < 160 ? 1.85F : 1.45F);
               }
               // CT player defusing by crouching or standing right on the planted C4 at the bomb site
               if (g != null && g.player != null && !g.player.dead && !g.player.spectator && myCsTeam == 0) {
                  int nearSite = nearCs2BombSite(g.player.x, g.player.y, g.player.z);
                  if (nearSite == cs2BombSite && g.player.sneaking) {
                     cs2DefuseTicks += 2;
                     if (cs2DefuseTicks >= 100) {
                        defuseCs2Bomb(g, Net319.isActive() ? Net319.selfName : "You", true);
                     }
                  }
               }
               if (cs2BombTimerTicks == 0 && !cs2RoundOver) {
                  cs2RoundOver = true;
                  double bx = (cs2BombSite == 1 ? -22.0 : 22.0) + 0.5;
                  SoundEngine.play3D(w, "tnt_explode", bx, Y_DECK + 1.0, 0.5, 1.0F, 0.85F);
                  onTntGrenadeDetonate(g, w, bx, Y_DECK + 1.0, 0.5);
                  boolean iWon = (myCsTeam == 1);
                  pendingOverviewWon = iWon;
                  pendingOverviewChamp = "Terrorists (Bomb Detonated!)";
                  victoryTransitionTicks = 30;
                  SoundEngine.playMinigameSound(iWon ? "mg_win" : "mg_lose");
                  if (g != null) {
                     g.say("\u00a7c\u00a7l[CS2 Strike] THE C4 HAS DETONATED! Terrorists Win the Round!");
                  }
               }
            }
         }
         if (!cs2RoundOver && !Net319.isClientConnected() && g != null && w.tickCount > 60L && w.tickCount % 20L == 0L) {
            int ctAlive = countCs2TeamAlive(g, 0);
            int tAlive = countCs2TeamAlive(g, 1);
            if (ctAlive == 0 && (cs2BotsSpawned || Net319.peerCount() > 0)) {
               cs2RoundOver = true;
               pendingOverviewWon = (myCsTeam == 1);
               pendingOverviewChamp = "Terrorists (Elimination Victory!)";
               victoryTransitionTicks = 30;
               SoundEngine.playMinigameSound(pendingOverviewWon ? "mg_win" : "mg_lose");
               g.say("\u00a7c\u00a7l[CS2 Strike] TERRORISTS WIN! \u00a7eAll Counter-Terrorists eliminated!");
            } else if (tAlive == 0 && !cs2BombPlanted && (cs2BotsSpawned || Net319.peerCount() > 0)) {
               cs2RoundOver = true;
               pendingOverviewWon = (myCsTeam == 0);
               pendingOverviewChamp = "Counter-Terrorists (Elimination Victory!)";
               victoryTransitionTicks = 30;
               SoundEngine.playMinigameSound(pendingOverviewWon ? "mg_win" : "mg_lose");
               g.say("\u00a7b\u00a7l[CS2 Strike] COUNTER-TERRORISTS WIN! \u00a7aAll Terrorists eliminated!");
            }
         }
         return;
      }

      // 5. MURDER MYSTERY: 1 Murderer (Knife), 1 Sheriff (Pistol), Innocents collecting 5 Gold for a Detective Pistol!
      if (type == WorldSettings.MURDER) {
         ensureRoundBotsSpawned(g, w);
         // Deal Murder Knife & Sheriff Pistol at 5 seconds (100 ticks)
         if (!murderWeaponsDealt && w.tickCount >= 100L) {
            murderWeaponsDealt = true;
            if (g != null && g.player != null && !g.player.dead && !g.player.spectator) {
               giveMurderLoadout(g, myMurderRole);
            }
            if (g != null && g.mobs != null) {
               for (Mobs.Mob m : g.mobs.list) {
                  if (m != null && !m.dead && m.teamTag != null && m.teamTag.startsWith("MurderBot:")) {
                     int bSlot = Math.abs(m.netId) & 31;
                     if (botMurderRole[bSlot] == MURDER_ROLE_MURDERER) {
                        botWeaponItem[bSlot] = Redstone319.MURDER_KNIFE;
                     } else if (botMurderRole[bSlot] == MURDER_ROLE_SHERIFF) {
                        botRangedItem[bSlot] = Redstone319.PISTOL;
                        botArrows[bSlot] = 16;
                     }
                  }
               }
            }
            SoundEngine.playMinigameSound("mg_count_go");
            if (g != null) {
               g.say("\u00a7c\u00a7l[Murder Mystery] \u00a7eThe Murderer has received their Knife and the Sheriff has drawn their Pistol!");
            }
         }
         // Spawn Gold Ingots around the Mansion halls every 4 seconds so Innocents & Bots can collect 5 Gold for a Pistol!
         if (!Net319.isConnected() && w.tickCount > 40L && w.tickCount % 80L == 0L) {
            int[][] goldSpots = new int[][]{
               {0, -16}, {0, 16}, {-16, 0}, {16, 0}, {-20, -20}, {20, -20}, {-20, 20}, {20, 20}, {0, -28}, {0, 28}
            };
            int[] sp = goldSpots[(int)((w.tickCount / 80L) % goldSpots.length)];
            if (w.chunkAt(sp[0], sp[1]) != null) {
               dropGeneratorItemCapped(w, sp[0] + 0.5, Y_DECK + 1.2, sp[1] + 0.5, Items.GOLD_INGOT, 1, 10);
            }
         }
         // Check if the local player collected 5 Gold Ingots to automatically receive a Detective Pistol + Ammo!
         if (g != null && g.player != null && !g.player.dead && !g.player.spectator) {
            int goldCnt = g.countItem(Items.GOLD_INGOT);
            myMurderGold = goldCnt;
            if (goldCnt >= 5 && myMurderRole == MURDER_ROLE_INNOCENT && g.countItem(Redstone319.PISTOL) == 0) {
               g.removeItems(Items.GOLD_INGOT, 5);
               g.addItem(new Stack(Redstone319.PISTOL, 1));
               g.addItem(new Stack(Items.ARROW, 6));
               SoundEngine.playUi("levelup", 0.85F, 1.15F);
               g.say("\u00a7a\u00a7l[Murder Mystery] \u00a7eYou collected 5 Gold Ingots and unlocked a Detective Pistol!");
            } else if (murderGunDropped && g.countItem(Redstone319.PISTOL) > 0 && myMurderRole == MURDER_ROLE_INNOCENT) {
               murderGunDropped = false;
               g.addItem(new Stack(Items.ARROW, 6));
               SoundEngine.playUi("levelup", 0.85F, 1.2F);
               g.say("\u00a7b\u00a7l[Murder Mystery] YOU ARE NOW THE HERO! \u00a7aYou picked up the Sheriff's Pistol!");
            }
         }
         // Victory checks for Murder Mystery
         if (!murderRoundOver && !Net319.isClientConnected() && g != null && w.tickCount > 120L && w.tickCount % 20L == 0L) {
            boolean murdererAlive = isMurdererAlive(g);
            int innocentsAlive = countInnocentsAndSheriffAlive(g);
            if (!murdererAlive) {
               murderRoundOver = true;
               pendingOverviewWon = (myMurderRole != MURDER_ROLE_MURDERER);
               pendingOverviewChamp = "Innocents & Sheriff (Murderer Stopped!)";
               victoryTransitionTicks = 30;
               SoundEngine.playMinigameSound(pendingOverviewWon ? "mg_win" : "mg_lose");
               g.say("\u00a7a\u00a7l[Murder Mystery] INNOCENTS WIN! \u00a7eThe Murderer was brought to justice!");
            } else if (innocentsAlive == 0 && (murderBotsSpawned || Net319.peerCount() > 0)) {
               murderRoundOver = true;
               pendingOverviewWon = (myMurderRole == MURDER_ROLE_MURDERER);
               pendingOverviewChamp = "The Murderer (No Survivors!)";
               victoryTransitionTicks = 30;
               SoundEngine.playMinigameSound(pendingOverviewWon ? "mg_win" : "mg_lose");
               g.say("\u00a7c\u00a7l[Murder Mystery] MURDERER WINS! \u00a77No Innocents survived the night!");
            } else if (w.tickCount >= 4800L) {
               // Time limit survived -> Innocents Win!
               murderRoundOver = true;
               pendingOverviewWon = (myMurderRole != MURDER_ROLE_MURDERER);
               pendingOverviewChamp = "Innocents & Sheriff (Survived until Dawn!)";
               victoryTransitionTicks = 30;
               SoundEngine.playMinigameSound(pendingOverviewWon ? "mg_win" : "mg_lose");
               g.say("\u00a7a\u00a7l[Murder Mystery] DAWN ARRIVED! \u00a7eInnocents survived the night and win!");
            }
         }
         return;
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
         // Check if any Tumble AI competitor fell into the boiling lava cauldron
         if (g != null && g.mobs != null && !Net319.isConnected()) {
            for (Mobs.Mob m : g.mobs.list) {
               if (m == null || m.dead || !Mobs.hostile(m.type)) {
                  continue;
               }
               if (m.y <= Y_DECK - 12.2) {
                  m.dead = true;
                  myKills++;
                  SoundEngine.play3D(w, "lava_fizz", m.x, m.y, m.z, 0.85F, 1.0F);
                  g.say("\u00a7b[Tumble] \u00a7e" + botDisplayName(m) + " \u00a77tumbled into the lava!");
               }
            }
         }
         if (!tumbleWon && !Net319.isClientConnected() && g != null && w.tickCount > 80L && w.tickCount % 30L == 0L) {
            int enemies = countHostiles(g) + Net319.aliveEnemyPeers(-1);
            boolean iAmAlive = !localEliminated && g.player != null && !g.player.dead && !g.player.spectator;
            if (iAmAlive && enemies == 0 && (tumbleSpawnedBots || Net319.peerCount() > 0)) {
               tumbleWon = true;
               pendingOverviewWon = true;
               pendingOverviewChamp = Net319.isActive() ? Net319.selfName + " (Champion!)" : "You (Champion!)";
               victoryTransitionTicks = 30;
               SoundEngine.playMinigameSound("mg_win");
               g.say("\u00a7b\u00a7l[Tumble] VICTORY! \u00a7aYou outlasted every rival above the lava! Opening Match Overview...");
            } else if (!iAmAlive && enemies <= 1) {
               tumbleWon = true;
               pendingOverviewWon = false;
               pendingOverviewChamp = survivingCompetitorName(g, -1);
               victoryTransitionTicks = 30;
               g.say("\u00a7b\u00a7l[Tumble] ROUND OVER! \u00a7e" + pendingOverviewChamp + " \u00a77won the round! Opening Match Overview...");
            }
         }
         return;
      }

      // 2. BATTLE: Console Edition grace period, chest refills, Showdown mode, and last-man-standing victory!
      if (type == WorldSettings.BATTLE) {
         stockSkywarsChests(g, w, false);
         ensureRoundBotsSpawned(g, w);
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
         if (!skywarsWon && !Net319.isClientConnected() && g != null && w.tickCount > 100L && w.tickCount % 40L == 0L) {
            int enemies = countHostiles(g) + Net319.aliveEnemyPeers(-1);
            boolean iAmAlive = !localEliminated && g.player != null && !g.player.dead && !g.player.spectator;
            if (iAmAlive && enemies == 0 && swIslandStocked[0] && (soloBots || Net319.peerCount() > 0)) {
               skywarsWon = true;
               pendingOverviewWon = true;
               pendingOverviewChamp = Net319.isActive() ? Net319.selfName + " (Champion!)" : "You (Champion!)";
               victoryTransitionTicks = 30;
               SoundEngine.playMinigameSound("mg_win");
               g.say("\u00a76\u00a7l[Battle] VICTORY! \u00a7aYou are the last champion standing! Opening Match Overview...");
            } else if (!iAmAlive && swIslandStocked[0] && enemies <= 1) {
               skywarsWon = true;
               pendingOverviewWon = false;
               pendingOverviewChamp = survivingCompetitorName(g, -1);
               victoryTransitionTicks = 30;
               g.say("\u00a76\u00a7l[Battle] ROUND OVER! \u00a7e" + pendingOverviewChamp + " \u00a77won the Battle! Opening Match Overview...");
            }
         }
         return;
      }

      // 3. BEDWARS: stock team bases, AI bot respawns, resource generators, bed destruction alerts & victory check
      if (type == WorldSettings.BEDWARS) {
         stockBedwarsWorld(g, w);
         if (w.tickCount < 40L) {
            ensureRoundBotsSpawned(g, w);
         }
         // Process Bedwars AI Bot Respawn timers & watchdog while their team bed remains intact
         if (g != null && g.mobs != null && soloBots && !Net319.isClientConnected()) {
            int myTeamIdx = getPlayerTeam(g);
            for (int slot = 0; slot < 8; slot++) {
               int team = slot >> 1;
               int role = slot & 1;
               if (team == myTeamIdx || Net319.isTeamOccupiedByPeer(team)) {
                  bwBotRespawnTicks[slot] = 0;
                  continue;
               }
               if (bwBotRespawnTicks[slot] > 0) {
                  if (!isTeamBedIntact(w, team)) {
                     bwBotRespawnTicks[slot] = 0;
                  } else if (--bwBotRespawnTicks[slot] == 0) {
                     spawnBedwarsTeamBot(g, team, role);
                     String bName = BOT_NAMES[(team * 2 + role) % BOT_NAMES.length];
                     g.say("\u00a77[Bedwars] " + BEDWARS_CODES[team] + "[" + BEDWARS_NAMES[team].toUpperCase() + "] " + bName + " \u00a77has respawned at " + BEDWARS_NAMES[team] + " Base!");
                  }
               } else if (w.tickCount % 10L == 0L && bwBaseStocked[team] && isTeamBedIntact(w, team)) {
                  // Watchdog: if an enemy team's bed is intact and its role bot died/vanished without a running timer, respawn it!
                  boolean alive = false;
                  for (Mobs.Mob m : g.mobs.list) {
                     if (m != null && !m.dead && botTeamIndex(m) == team && (m.variant & 1) == role) {
                        alive = true;
                        break;
                     }
                  }
                  if (!alive) {
                     bwBotRespawnTicks[slot] = 60; // Respawn in 3 seconds
                  }
               }
            }
         }
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
            boolean myTeamAlive = (!localEliminated && !g.player.dead && !g.player.spectator) || isTeamBedIntact(w, my) || Net319.aliveTeamPeers(my) > 0;
            if (!bedwarsWon && !Net319.isClientConnected() && bwBaseStocked[my]) {
               if (myTeamAlive && enemyBeds == 0 && countHostiles(g) == 0 && enemyPlayers == 0) {
                  bedwarsWon = true;
                  pendingOverviewWon = true;
                  pendingOverviewChamp = BEDWARS_NAMES[my] + " Team (Champions!)";
                  victoryTransitionTicks = 30;
                  SoundEngine.playMinigameSound("mg_win");
                  g.say("\u00a76\u00a7l[Bedwars] VICTORY! \u00a7aAll enemy beds and opponents eliminated! Opening Match Overview...");
               } else if (!myTeamAlive && countSurvivingBedwarsTeams(g, w, my) <= 1) {
                  bedwarsWon = true;
                  pendingOverviewWon = false;
                  pendingOverviewChamp = survivingCompetitorName(g, my);
                  victoryTransitionTicks = 30;
                  g.say("\u00a76\u00a7l[Bedwars] ROUND OVER! \u00a7e" + pendingOverviewChamp + " \u00a77won the match! Opening Match Overview...");
               }
            }
         }
      }
   }

   private static int countCs2TeamAlive(Game g, int csTeam) {
      int c = 0;
      if (g != null && g.player != null && !localEliminated && !g.player.dead && !g.player.spectator && (getPlayerTeam(g) & 1) == csTeam) {
         c++;
      }
      for (Net319.Peer p : Net319.peers().values()) {
         if (p != null && !p.spectator && !p.eliminated && p.health > 0 && (p.team & 1) == csTeam) {
            c++;
         }
      }
      if (g != null && g.mobs != null) {
         for (Mobs.Mob m : g.mobs.list) {
            if (m != null && !m.dead && botTeamIndex(m) == csTeam) {
               c++;
            }
         }
      }
      return c;
   }

   private static boolean isMurdererAlive(Game g) {
      if (myMurderRole == MURDER_ROLE_MURDERER && g != null && g.player != null && !localEliminated && !g.player.dead && !g.player.spectator) {
         return true;
      }
      if (g != null && g.mobs != null) {
         for (Mobs.Mob m : g.mobs.list) {
            if (m != null && !m.dead && m.teamTag != null && m.teamTag.startsWith("MurderBot:")) {
               int bSlot = Math.abs(m.netId) & 31;
               if (botMurderRole[bSlot] == MURDER_ROLE_MURDERER) {
                  return true;
               }
            }
         }
      }
      return Net319.peerCount() > 0 && myMurderRole != MURDER_ROLE_MURDERER && Net319.aliveEnemyPeers(-1) > 0;
   }

   private static int countInnocentsAndSheriffAlive(Game g) {
      int c = 0;
      if (myMurderRole != MURDER_ROLE_MURDERER && g != null && g.player != null && !localEliminated && !g.player.dead && !g.player.spectator) {
         c++;
      }
      if (g != null && g.mobs != null) {
         for (Mobs.Mob m : g.mobs.list) {
            if (m != null && !m.dead && m.teamTag != null && m.teamTag.startsWith("MurderBot:")) {
               int bSlot = Math.abs(m.netId) & 31;
               if (botMurderRole[bSlot] != MURDER_ROLE_MURDERER) {
                  c++;
               }
            }
         }
      }
      return c;
   }

   private static int countSurvivingBedwarsTeams(Game g, World w, int excludeTeam) {
      int count = 0;
      for (int t = 0; t < 4; t++) {
         if (t == excludeTeam) {
            continue;
         }
         boolean alive = isTeamBedIntact(w, t) || Net319.aliveTeamPeers(t) > 0;
         if (!alive && g != null && g.mobs != null) {
            for (Mobs.Mob m : g.mobs.list) {
               if (m != null && !m.dead && botTeamIndex(m) == t) {
                  alive = true;
                  break;
               }
            }
         }
         if (alive) {
            count++;
         }
      }
      return count;
   }

   private static String survivingCompetitorName(Game g, int myTeam) {
      for (Net319.Peer p : Net319.peers().values()) {
         if (p != null && !p.spectator && !p.eliminated && p.health > 0 && (myTeam < 0 || p.team != myTeam)) {
            return p.name + " (Champion!)";
         }
      }
      if (g != null && g.mobs != null) {
         for (Mobs.Mob m : g.mobs.list) {
            if (m != null && !m.dead && Mobs.hostile(m.type)) {
               return botDisplayName(m) + " (Champion)";
            }
         }
      }
      return BOT_NAMES[0] + " (Champion)";
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

   /** Handles /map, /tumble, /battle, /bw, /cs2, /murder, /shop, /upgrades, and /lobby commands. */
   public static boolean onModeCommand(Game g, String cmd, String[] args) {
      if (g == null || g.world == null) {
         return false;
      }
      String c = cmd.toLowerCase();
      if (c.equals("/map")) {
         String sub = args.length > 1 ? args[1].toLowerCase() : "list";
         if (sub.equals("save") && args.length > 2) {
            String name = args[2];
            if (saveCustomArenaMap(g.world, name)) {
               customMapName = name;
               g.say("\u00a7a[Maps] Saved current arena chunks to prebuilt map folder \u00a7emaps/" + name + "\u00a7a!");
            } else {
               g.say("\u00a7c[Maps] Failed to save map " + name + ".");
            }
            return true;
         }
         if (sub.equals("load") && args.length > 2) {
            String name = args[2];
            customMapName = name;
            File f = resolvePrebuiltMapDir(g.world.settings != null ? g.world.settings.type : WorldSettings.BATTLE, 0);
            if (f != null) {
               resetArenaMap(g, g.world, true);
               matchState = STATE_PLAYING;
               g.say("\u00a7a[Maps] Loaded custom prebuilt world map \u00a7e" + f.getPath() + "\u00a7a!");
            } else {
               customMapName = null;
               g.say("\u00a7c[Maps] No folder found at maps/" + name + " or saves/" + name + "!");
            }
            return true;
         }
         if (sub.equals("clear") || sub.equals("builtin")) {
            customMapName = null;
            resetArenaMap(g, g.world, true);
            g.say("\u00a7e[Maps] Switched back to built-in arena generator (" + (g.world.settings != null ? g.world.settings.mapName() : "Cove") + ").");
            return true;
         }
         ArrayList<String> custom = listCustomMaps();
         g.say("\u00a7b[Maps] Built-in: Cove, Crucible, Cavern, Temple | Custom in maps/: "
            + (custom.isEmpty() ? "(none yet - use /map save <name>)" : String.join(", ", custom))
            + " | Commands: /map save <name> | /map load <name> | /map clear");
         return true;
      }
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
      if (c.equals("/cs2") || c.equals("/strike")) {
         String sub = args.length > 1 ? args[1].toLowerCase() : "status";
         if (sub.equals("reset")) {
            resetArenaMap(g, g.world, true);
            matchState = STATE_PLAYING;
            g.say("\u00a7b[CS2 Strike] New round started and bomb sites reset!");
            return true;
         }
         if (sub.equals("kit") && args.length > 2) {
            int k = Integer.parseInt(args[2].replaceAll("[^0-9]", "0")) & 3;
            giveCs2Loadout(g, k);
            g.say("\u00a7b[CS2 Strike] Selected Loadout: \u00a7e" + CS2_KIT_NAMES[k]);
            return true;
         }
         if (sub.equals("team") && args.length > 2) {
            int t = args[2].toLowerCase().startsWith("t") || args[2].equals("1") ? 1 : 0;
            selectTeam(g, t, true);
            g.say("\u00a7b[CS2 Strike] Joined " + (t == 0 ? "\u00a79Counter-Terrorists (CT)" : "\u00a7cTerrorists (T)") + "\u00a7b!");
            return true;
         }
         g.say("\u00a7b[CS2 Strike] CT Alive: " + countCs2TeamAlive(g, 0) + " | T Alive: " + countCs2TeamAlive(g, 1)
            + " | Bomb: " + (cs2BombPlanted ? ("\u00a7cPLANTED AT SITE " + (cs2BombSite == 0 ? "A" : "B")) : "\u00a7aNOT PLANTED")
            + " (/cs2 reset | /cs2 kit <0..3> | /cs2 team <ct|t>)");
         return true;
      }
      if (c.equals("/murder") || c.equals("/mm")) {
         String sub = args.length > 1 ? args[1].toLowerCase() : "status";
         if (sub.equals("reset")) {
            resetArenaMap(g, g.world, true);
            matchState = STATE_PLAYING;
            g.say("\u00a7d[Murder Mystery] New round started and secret roles reassigned!");
            return true;
         }
         if (sub.equals("role") && args.length > 2) {
            String r = args[2].toLowerCase();
            murderRolePref = r.startsWith("m") ? 3 : (r.startsWith("s") ? 2 : (r.startsWith("i") ? 1 : 0));
            assignMurderMysteryRoles(g, g.world);
            giveMurderLoadout(g, myMurderRole);
            g.say("\u00a7d[Murder Mystery] Role set to \u00a7e" + MURDER_ROLE_NAMES[myMurderRole] + "\u00a7d!");
            return true;
         }
         g.say("\u00a7d[Murder Mystery] Your Role: " + MURDER_ROLE_CODES[myMurderRole] + MURDER_ROLE_NAMES[myMurderRole]
            + " \u00a77| Gold: \u00a76" + myMurderGold + "/5 \u00a77| Innocents Left: \u00a7a" + countInnocentsAndSheriffAlive(g)
            + " \u00a77(/murder reset | /murder role <random|innocent|sheriff|murderer>)");
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
         if (sub.equals(" map") || (sub.equals("map") && args.length > 2 && g.world.settings != null)) {
            int m = parseMapArg(args[2]);
            g.world.settings.mapId = m;
            resetArenaMap(g, g.world, true);
            matchState = STATE_PLAYING;
            g.say("\u00a7e[Bedwars] Switched arena map to \u00a7b" + WorldSettings.MAP_NAMES[m] + "\u00a7e!");
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

   /** Renders the sleek dark-glass top-right Scoreboard HUD for Console Edition Tumble, Battle, Bedwars, CS2 Strike, and Murder Mystery. */
   public static void drawModeHud(Game g) {
      if (g == null || g.world == null || g.world.settings == null || g.debug || g.screen == Game.S.WAITING) {
         return;
      }
      int type = g.world.settings.type;
      if (!g.world.settings.isVoidMode()) {
         return;
      }
      String mapName = customMapName != null && !customMapName.isBlank() ? customMapName : g.world.settings.mapName();
      Gui gui = g.gui;
      java.util.ArrayList<String> lines = new java.util.ArrayList<>();
      int accentColor = 0xFF68F080;
      if (type == WorldSettings.TUMBLE) {
         accentColor = 0xFF48D8FF;
         int rivals = countHostiles(g) + Net319.aliveEnemyPeers(-1);
         int layer = g.player != null ? (g.player.y >= Y_DECK - 2 ? 1 : (g.player.y >= Y_DECK - 7 ? 2 : 3)) : 1;
         lines.add("\u00a7b\u00a7lTUMBLE \u00a77(" + mapName + ")");
         lines.add("\u00a7fRivals Left: " + (rivals > 0 ? "\u00a7c" + rivals : "\u00a7a0 (VICTORY!)") + " \u00a77| Layer: \u00a7e" + layer + "/3");
         lines.add("\u00a7fMode: \u00a7a" + (tumbleWeaponMode == 0 ? "Shovels+Snow" : (tumbleWeaponMode == 1 ? "Shovels" : "Snowballs")));
         lines.add("\u00a77L-Click: Break | R-Click: Throw");
         lines.add("\u00a77/tumble reset | /map save");
      } else if (type == WorldSettings.BATTLE) {
         accentColor = 0xFFFFB840;
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
         lines.add("\u00a77/battle kit | /battle map | /map save");
      } else if (type == WorldSettings.BEDWARS) {
         accentColor = 0xFFFFDF58;
         int my = getPlayerTeam(g);
         long dPeriod = g.world.tickCount >= 9600L ? 150L : (g.world.tickCount >= 2400L ? 200L : 300L);
         long ePeriod = g.world.tickCount >= 9600L ? 250L : (g.world.tickCount >= 4800L ? 340L : 500L);
         long dRem = (dPeriod - (g.world.tickCount % dPeriod)) / 20L;
         long eRem = (ePeriod - (g.world.tickCount % ePeriod)) / 20L;
         String tierStr = g.world.tickCount >= 9600L ? "Max" : (g.world.tickCount >= 4800L ? "Em II" : (g.world.tickCount >= 2400L ? "Dia II" : "Tier I"));
         lines.add("\u00a7e\u00a7lBEDWARS \u00a77(" + mapName + " - " + tierStr + ")");
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
         lines.add("\u00a77/shop | /upgrades | /map save");
      } else if (type == WorldSettings.CS2) {
         int myCs = getPlayerTeam(g) & 1;
         accentColor = myCs == 0 ? 0xFF48A8FF : 0xFFFF6448;
         int ctAlive = countCs2TeamAlive(g, 0);
         int tAlive = countCs2TeamAlive(g, 1);
         int ammo = g.countItem(Items.ARROW);
         int nades = g.countItem(Redstone319.TNT_GRENADE);
         lines.add("\u00a7b\u00a7lCS2 STRIKE \u00a77(" + mapName + ")");
         lines.add("\u00a79CT Alive: \u00a7f" + ctAlive + " \u00a77| \u00a7cT Alive: \u00a7f" + tAlive);
         lines.add("\u00a7fYour Side: " + (myCs == 0 ? "\u00a79CT (Defend/Defuse)" : "\u00a7cTerrorist (Plant C4)"));
         lines.add("\u00a7fAmmo: \u00a7e" + ammo + " \u00a77| Grenades: \u00a7c" + nades + " \u00a77| Kills: \u00a7a" + myKills);
         if (cs2BombPlanted) {
            lines.add("\u00a7c\u00a7lC4 SITE " + (cs2BombSite == 0 ? "A" : "B") + ": \u00a7e" + ((cs2BombTimerTicks + 19) / 20) + "s!");
         } else {
            lines.add("\u00a77Site A: (-22,0) | Site B: (+22,0)");
         }
      } else if (type == WorldSettings.MURDER) {
         accentColor = myMurderRole == MURDER_ROLE_MURDERER ? 0xFFFF4848 : (myMurderRole == MURDER_ROLE_SHERIFF ? 0xFF48B8FF : 0xFF58F088);
         int innoAlive = countInnocentsAndSheriffAlive(g);
         long remSec = Math.max(0L, (4800L - g.world.tickCount) / 20L);
         lines.add("\u00a7d\u00a7lMURDER MYSTERY \u00a77(" + mapName + ")");
         lines.add("\u00a7fRole: " + MURDER_ROLE_CODES[myMurderRole] + "\u00a7l" + MURDER_ROLE_NAMES[myMurderRole]);
         lines.add("\u00a7fInnocents Alive: \u00a7a" + innoAlive + " \u00a77| Dawn: \u00a7e" + (remSec / 60L) + ":" + String.format("%02d", remSec % 60L));
         lines.add("\u00a7fGold Collected: \u00a76" + myMurderGold + "/5 \u00a77(5 Gold = Pistol)");
         if (murderGunDropped) {
            lines.add("\u00a7e\u00a7lSHERIFF GUN DROPPED! (" + (int)murderDroppedGunX + ", " + (int)murderDroppedGunZ + ")");
         } else if (!murderWeaponsDealt) {
            lines.add("\u00a7eWeapons dealt in " + Math.max(1L, (100L - g.world.tickCount + 19L) / 20L) + "s...");
         }
      }

      if (lines.isEmpty()) {
         return;
      }
      float maxW = 94.0F;
      for (String s : lines) {
         maxW = Math.max(maxW, Gui.textWidth(Gui.stripCodes(s)) + 10.0F);
      }
      float boxH = lines.size() * 10.0F + 8.0F;
      float x = gui.w - maxW - 4.0F;
      float y = 26.0F;
      gui.rect(x + 1.0F, y + 1.0F, maxW + 1.0F, boxH + 1.0F, 0x55000000);
      gui.rect(x - 1.0F, y - 1.0F, maxW + 2.0F, boxH + 2.0F, 0xFF06080E);
      gui.gradient(x, y, maxW, boxH, 0xDC121826, 0xE8090C14);
      gui.rect(x, y, maxW, 1.5F, accentColor);
      gui.rect(x, y + 12.5F, maxW, 0.5F, 0x40FFFFFF);
      for (int i = 0; i < lines.size(); i++) {
         gui.text(lines.get(i), x + 5.0F, y + 4.0F + i * 10.0F, 0xFFFFFFFF, true);
      }

      // Top-Center Tactical Alerts for CS2 Bomb Countdown / Defusal & Murder Mystery Role Banner
      if (type == WorldSettings.CS2 && cs2BombPlanted && !cs2RoundOver) {
         float cx = gui.w / 2.0F;
         int sec = (cs2BombTimerTicks + 19) / 20;
         gui.centered("\u00a7c\u00a7l\u26a0 C4 PLANTED AT SITE " + (cs2BombSite == 0 ? "A" : "B") + " — " + sec + "s \u26a0", cx, 24.0F, 0xFFFF5555, true, 1.15F);
         if (cs2DefuseTicks > 0) {
            gui.centered("\u00a7bDefusing C4... " + cs2DefuseTicks + "%", cx, 36.0F, 0xFF68D8FF, true, 1.0F);
         }
      } else if (type == WorldSettings.MURDER && g.world.tickCount < 160L) {
         float cx = gui.w / 2.0F;
         gui.centered(MURDER_ROLE_CODES[myMurderRole] + "\u00a7lROLE: " + MURDER_ROLE_NAMES[myMurderRole], cx, 26.0F, 0xFFFFFFFF, true, 1.35F);
      }

      if (victoryTransitionTicks > 0) {
         float cx = gui.w / 2.0F;
         float cy = gui.h * 0.32F;
         gui.centered(pendingOverviewWon ? "\u00a76\u00a7lVICTORY!" : "\u00a7e\u00a7lROUND OVER!", cx, cy, 0xFFFFDF58, true, 2.5F);
         gui.centered("\u00a7a" + pendingOverviewChamp + " — Opening Overview...", cx, cy + 24.0F, 0xFF88FF98, true, 1.0F);
      }
   }
}
