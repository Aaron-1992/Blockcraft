package bc;

import java.util.HashSet;
import java.util.Random;
import org.lwjgl.opengl.GL11;

/**
 * The Backrooms & Stalker Horror Total-Conversion Mod (ID: {@code backrooms-stalker}).
 * <p>
 * v2.1 Upgrade:
 * <ul>
 *   <li><b>5 Separate, Prettier Backrooms Levels</b> ({@link BackroomsGen}): Level 0 (The Lobby),
 *       Level 1 (Warehouse), Level 2 (Pipe Dreams), Level 37 (The Poolrooms), and Level "!" (Run For Your Life).</li>
 *   <li><b>Findable Almond Water Item ({@link #ALMOND_WATER} = 269)</b>: Spawns physically on Office Desks and
 *       Warehouse Supply Shelves, and dispenses from Almond Water Coolers (instead of random loot chests).</li>
 *   <li><b>Hiding Spots &amp; Stealth</b>: Hiding Lockers, Under-Desk Cubbies, and Pipe Alcoves make you
 *       undetectable ({@code [HIDING — UNDETECTABLE]}) and break Stalker chases.</li>
 *   <li><b>Balanced, Atmospheric Stalker AI</b>: Slow-burn pacing, distant corner-peeking, forgiving 4.5s
 *       stare warning, outrunnable chase speed, and 45s cooldown between encounters.</li>
 * </ul>
 */
public class BackroomsStalkerMod implements BlockcraftMod {
   public static final int STALKER_TAG = 0xB4C700;
   public static final int ALMOND_WATER = 269;
   public static final int T_ALMOND_WATER = 742;

   // Custom 3D Block Model IDs (registered in Shapes/Blocks when mod is active)
   public static final int B_DESK = 2010;
   public static final int B_LOCKER_LOW = 2011;
   public static final int B_LOCKER_HIGH = 2012;
   public static final int B_WATER_COOLER = 2013;
   public static final int B_COOLER_JUG = 2014;
   public static final int B_EXIT_LOW = 2015;
   public static final int B_EXIT_HIGH = 2016;
   private static boolean customBlocksRegistered = false;
   private int exitCooldown = 0;

   public static final int PHASE_STALK = 0;
   public static final int PHASE_FROZEN = 1;
   public static final int PHASE_AMBUSH = 2;
   public static final int PHASE_CHASE = 3;

   /** Pocket Backrooms elevation when entered via /backrooms in a normal Overworld. */
   public static final int POCKET_Y = 200;
   public static final int POCKET_FLOOR_Y = POCKET_Y;
   public static final int POCKET_RADIUS = 20;

   private static boolean active = false;
   private static BackroomsStalkerMod instance;

   private final Random rng = new Random();
   private Game lastGame;
   private boolean worldGenEnabled = true;
   private boolean inPocketBackrooms = false;
   private int currentLevel = BackroomsGen.LEVEL_0;
   private double returnX;
   private double returnY = -1.0;
   private double returnZ;

   // Saved graphics settings restored when the mod is disabled
   private boolean savedSettingsValid = false;
   private boolean savedShaders;
   private boolean savedSmooth;
   private int savedFx;
   private boolean savedClouds;
   private boolean savedWeather;
   private boolean savedDisasters;
   private float savedGamma;
   private int savedLook;

   // Stalker Entity & Horror State
   private static Mobs.Mob stalkerMob;
   private static int stalkerPhase = PHASE_STALK;
   private static int stareTicks = 0;
   private static int chaseTicks = 0;
   private static int hiddenTicks = 0;
   private static int stalkerCooldown = 600; // 30s peaceful exploration at start
   private static int sanity = 100;
   private static int heartbeatBpm = 70;
   private static float panicLevel = 0.0F;
   private static float jumpscareFlash = 0.0F;
   private static float lastDistToPlayer = 99.0F;
   private static boolean playerCurrentlyHidden = false;

   // Tracks which Supply Tables / Shelves have already spawned their Almond Water bottle
   private final HashSet<Long> stockedTables = new HashSet<>();

   // Fluorescent light blackout ring buffer
   private static int blackoutTicks = 0;
   private final int[] flickeredLightsX = new int[48];
   private final int[] flickeredLightsY = new int[48];
   private final int[] flickeredLightsZ = new int[48];
   private int flickeredCount = 0;

   public BackroomsStalkerMod() {
      instance = this;
   }

   public static boolean isActive() {
      return active;
   }

   public static boolean isStalkerMob(Mobs.Mob m) {
      return m != null && m.tag == STALKER_TAG;
   }

   public static Mobs.Mob getStalker() {
      return stalkerMob != null && !stalkerMob.dead ? stalkerMob : null;
   }

   public static int getPhase() {
      return stalkerPhase;
   }

   public static float getPanicLevel() {
      return panicLevel;
   }

   public static boolean isBlackout() {
      return blackoutTicks > 0;
   }

   public static boolean isPlayerHidden() {
      return playerCurrentlyHidden;
   }

   /** Custom 16x16 pixel art sprite for the Almond Water Bottle (atlas tile {@link #T_ALMOND_WATER}). */
   public static int[] almondWaterSprite() {
      int[] p = new int[256];
      int cap = 0xFFD9822B;       // Warm amber/gold bottle cap
      int capDark = 0xFFA85E18;
      int glass = 0xFFDCEEF2;     // Clear glass rim
      int liquid = 0xFFF2E6C9;    // Creamy sweet almond water
      int liqShade = 0xFFD9C9A3;
      int label = 0xFFF7F4EB;     // Cream bottle label
      int labelBorder = 0xFF8C6D46;
      int emblem = 0xFFC8372D;    // Red cross / almond seal on label

      // Cap (rows 2..3, cols 6..9)
      for (int x = 6; x <= 9; x++) {
         p[2 * 16 + x] = cap;
         p[3 * 16 + x] = (x == 9) ? capDark : cap;
      }
      // Bottle neck (rows 4..5, cols 6..9)
      for (int y = 4; y <= 5; y++) {
         for (int x = 6; x <= 9; x++) {
            p[y * 16 + x] = (x == 6 || x == 9) ? glass : liquid;
         }
      }
      // Bottle body (rows 6..14, cols 5..10)
      for (int y = 6; y <= 14; y++) {
         for (int x = 5; x <= 10; x++) {
            boolean edge = (x == 5 || x == 10 || y == 14);
            if (y >= 8 && y <= 11 && x >= 6 && x <= 9) {
               // Label
               if (y == 8 || y == 11) {
                  p[y * 16 + x] = labelBorder;
               } else if ((x == 7 || x == 8) && (y == 9 || y == 10)) {
                  p[y * 16 + x] = emblem;
               } else {
                  p[y * 16 + x] = label;
               }
            } else if (edge) {
               p[y * 16 + x] = (x == 10 || y == 14) ? liqShade : glass;
            } else {
               p[y * 16 + x] = (x == 9) ? liqShade : liquid;
            }
         }
      }
      return p;
   }

   public static void registerAlmondWaterItem() {
      Items.defTile(ALMOND_WATER, "Almond Water", T_ALMOND_WATER);
      Items.maxStack[ALMOND_WATER] = 16;
      Items.foodPts[ALMOND_WATER] = 6;
      Items.foodSat[ALMOND_WATER] = 8.0F;
      registerCustomBlocks();
   }

   public static void unregisterAlmondWaterItem() {
      Items.name[ALMOND_WATER] = null;
      Items.tile[ALMOND_WATER] = 0;
      Items.maxStack[ALMOND_WATER] = 0;
      Items.foodPts[ALMOND_WATER] = 0;
      Items.foodSat[ALMOND_WATER] = 0.0F;
      unregisterCustomBlocks();
   }

   public static void unregisterCustomBlocks() {
      if (!customBlocksRegistered) {
         return;
      }
      for (int id = B_DESK; id <= B_EXIT_HIGH; id++) {
         Blocks.name[id] = null;
         Blocks.solid[id] = false;
         Blocks.opaque[id] = false;
         Blocks.opacity[id] = 0;
         Blocks.emit[id] = 0;
         Blocks.hardness[id] = 0.0F;
         Blocks.drop[id] = 0;
         Blocks.kind[id] = 0;
         Blocks.coll[id] = null;
         Blocks.collMulti[id] = null;
         Blocks.box[id] = new double[]{0.0, 0.0, 0.0, 1.0, 1.0, 1.0};
         for (int f = 0; f < 6; f++) {
            Blocks.tiles[id][f] = 0;
         }
         Shapes.model(id, null);
      }
      customBlocksRegistered = false;
   }

   /**
    * Registers the 7 custom 3D multi-box models ({@link Shapes#K_SHAPE}) for 3D Office Desks,
    * 3D Steel Hiding Lockers, 3D Almond Water Coolers, and 3D Illuminated Level Exit Doors.
    */
   public static void registerCustomBlocks() {
      if (customBlocksRegistered) {
         return;
      }
      customBlocksRegistered = true;

      int oakPlank = Blocks.tiles[Blocks.PLANKS][0];
      int birchPlank = Blocks.tiles[Blocks313.PLANKS_BIRCH][0];
      int darkWood = Blocks.tiles[Blocks.LOG_SPRUCE][2];
      int ironTile = Color312.T_IRON;
      int quartzTile = Blocks.tiles[Blocks.QUARTZ_BLOCK][0];
      int iceTile = Blocks.tiles[Blocks313.ICE][0];
      int lanternTile = Redstone319.T_SEA_LANTERN;

      // 1. 3D OFFICE DESK (B_DESK = 2010):
      //    Tabletop sits at y = 13..16 (0.8125..1.0), leaving y = 0..0.8125 open so crouching players (height 0.72) crawl right under!
      defModelBlock(B_DESK, "Office Desk", oakPlank, 1.5F, 0);
      Shapes.model(B_DESK, new double[][]{
         Shapes.tiles(Shapes.px(0.0, 13.0, 0.0, 16.0, 16.0, 16.0), birchPlank, oakPlank, birchPlank),
         Shapes.tiles(Shapes.px(1.0, 11.5, 1.0, 15.0, 13.0, 15.0), darkWood, darkWood, darkWood),
         Shapes.tiles(Shapes.px(2.5, 3.5, 1.0, 13.5, 11.5, 2.5), birchPlank, birchPlank, birchPlank),
         Shapes.tiles(Shapes.px(1.0, 0.0, 1.0, 3.0, 11.5, 3.0), darkWood, darkWood, darkWood),
         Shapes.tiles(Shapes.px(13.0, 0.0, 1.0, 15.0, 11.5, 3.0), darkWood, darkWood, darkWood),
         Shapes.tiles(Shapes.px(1.0, 0.0, 13.0, 3.0, 11.5, 15.0), darkWood, darkWood, darkWood),
         Shapes.tiles(Shapes.px(13.0, 0.0, 13.0, 15.0, 11.5, 15.0), darkWood, darkWood, darkWood)
      });
      Blocks.coll[B_DESK] = new double[]{0.0, 0.8125, 0.0, 1.0, 1.0, 1.0};
      Blocks.box[B_DESK] = new double[]{0.0, 0.0, 0.0, 1.0, 1.0, 1.0};

      // 2. 3D STEEL HIDING LOCKER (B_LOCKER_LOW = 2011, B_LOCKER_HIGH = 2012):
      //    Hollow 3D cabinet with louvered upper vent slits and chrome handle.
      defModelBlock(B_LOCKER_LOW, "Hiding Locker", ironTile, 2.0F, 0);
      Shapes.model(B_LOCKER_LOW, new double[][]{
         Shapes.tint(Shapes.px(1.0, 0.0, 1.0, 15.0, 1.5, 15.0), 0x707C88),
         Shapes.tint(Shapes.px(1.0, 1.5, 1.0, 2.5, 16.0, 15.0), 0x7C8894),
         Shapes.tint(Shapes.px(13.5, 1.5, 1.0, 15.0, 16.0, 15.0), 0x7C8894),
         Shapes.tint(Shapes.px(2.5, 1.5, 1.0, 13.5, 16.0, 2.5), 0x687480),
         Shapes.tint(Shapes.px(2.5, 1.5, 13.5, 4.5, 16.0, 15.0), 0x8896A4),
         Shapes.tint(Shapes.px(11.5, 1.5, 13.5, 13.5, 16.0, 15.0), 0x8896A4),
         Shapes.tint(Shapes.px(4.5, 1.5, 13.8, 11.5, 14.5, 14.8), 0x94A2B0),
         Shapes.tint(Shapes.px(10.2, 11.0, 14.8, 11.5, 15.0, 15.8), 0xF0F4F8)
      });
      double[][] lockerWalls = new double[][]{
         {0.0, 0.0, 0.0, 0.14, 1.0, 1.0},
         {0.86, 0.0, 0.0, 1.0, 1.0, 1.0},
         {0.14, 0.0, 0.0, 0.86, 1.0, 0.14}
      };
      Blocks.coll[B_LOCKER_LOW] = new double[]{0.0, 0.0, 0.0, 1.0, 1.0, 1.0};
      Blocks.collMulti[B_LOCKER_LOW] = lockerWalls;
      Blocks.box[B_LOCKER_LOW] = new double[]{0.0625, 0.0, 0.0625, 0.9375, 1.0, 0.9375};

      defModelBlock(B_LOCKER_HIGH, "Hiding Locker Top", ironTile, 2.0F, 0);
      Shapes.model(B_LOCKER_HIGH, new double[][]{
         Shapes.tint(Shapes.px(0.5, 14.0, 0.5, 15.5, 16.0, 15.5), 0x707C88),
         Shapes.tint(Shapes.px(1.0, 0.0, 1.0, 2.5, 14.0, 15.0), 0x7C8894),
         Shapes.tint(Shapes.px(13.5, 0.0, 1.0, 15.0, 14.0, 15.0), 0x7C8894),
         Shapes.tint(Shapes.px(2.5, 0.0, 1.0, 13.5, 14.0, 2.5), 0x687480),
         Shapes.tint(Shapes.px(2.5, 0.0, 13.5, 4.5, 14.0, 15.0), 0x8896A4),
         Shapes.tint(Shapes.px(11.5, 0.0, 13.5, 13.5, 14.0, 15.0), 0x8896A4),
         Shapes.tint(Shapes.px(4.5, 0.0, 13.8, 11.5, 4.5, 14.8), 0x94A2B0),
         Shapes.tint(Shapes.px(4.5, 6.0, 13.8, 11.5, 7.5, 14.8), 0x94A2B0),
         Shapes.tint(Shapes.px(4.5, 9.0, 13.8, 11.5, 10.5, 14.8), 0x94A2B0),
         Shapes.tint(Shapes.px(4.5, 12.0, 13.8, 11.5, 14.0, 14.8), 0x94A2B0)
      });
      Blocks.coll[B_LOCKER_HIGH] = new double[]{0.0, 0.0, 0.0, 1.0, 1.0, 1.0};
      Blocks.collMulti[B_LOCKER_HIGH] = lockerWalls;
      Blocks.box[B_LOCKER_HIGH] = new double[]{0.0625, 0.0, 0.0625, 0.9375, 1.0, 0.9375};

      // 3. 3D ALMOND WATER COOLER & JUG (B_WATER_COOLER = 2013, B_COOLER_JUG = 2014)
      defModelBlock(B_WATER_COOLER, "Almond Water Cooler", quartzTile, 1.2F, 4);
      Shapes.model(B_WATER_COOLER, new double[][]{
         Shapes.px(3.0, 0.0, 3.0, 13.0, 15.0, 13.0),
         Shapes.tint(Shapes.px(4.0, 8.0, 12.6, 12.0, 9.5, 14.2), 0x505860),
         Shapes.tint(Shapes.px(5.2, 11.2, 13.0, 6.8, 13.2, 14.6), 0x3598FF),
         Shapes.tint(Shapes.px(9.2, 11.2, 13.0, 10.8, 13.2, 14.6), 0xFF4242),
         Shapes.tint(Shapes.px(4.5, 15.0, 4.5, 11.5, 16.0, 11.5), 0xD8E4EC)
      });
      Blocks.coll[B_WATER_COOLER] = new double[]{0.1875, 0.0, 0.1875, 0.8125, 1.0, 0.8125};
      Blocks.box[B_WATER_COOLER] = new double[]{0.1875, 0.0, 0.1875, 0.8125, 1.0, 0.8125};

      defModelBlock(B_COOLER_JUG, "Almond Water Jug", iceTile, 0.8F, 6);
      Shapes.model(B_COOLER_JUG, new double[][]{
         Shapes.tint(Shapes.px(6.0, 0.0, 6.0, 10.0, 1.5, 10.0), 0x78D8FF),
         Shapes.tint(Shapes.px(4.2, 1.5, 4.2, 11.8, 12.0, 11.8), 0x8CE2FF),
         Shapes.tint(Shapes.px(6.0, 12.0, 6.0, 10.0, 13.5, 10.0), 0x60C8F8)
      });
      Blocks.coll[B_COOLER_JUG] = new double[]{0.26, 0.0, 0.26, 0.74, 0.85, 0.74};
      Blocks.box[B_COOLER_JUG] = new double[]{0.26, 0.0, 0.26, 0.74, 0.85, 0.74};

      // 4. 3D ILLUMINATED LEVEL EXIT DOOR & OVERHEAD EXIT SIGN (B_EXIT_LOW = 2015, B_EXIT_HIGH = 2016)
      defModelBlock(B_EXIT_LOW, "Level Exit Door", lanternTile, -1.0F, 15);
      Shapes.model(B_EXIT_LOW, new double[][]{
         Shapes.tint(Shapes.tiles(Shapes.px(0.0, 0.0, 4.0, 2.5, 16.0, 12.0), ironTile, ironTile, ironTile), 0x48525C),
         Shapes.tint(Shapes.tiles(Shapes.px(13.5, 0.0, 4.0, 16.0, 16.0, 12.0), ironTile, ironTile, ironTile), 0x48525C),
         Shapes.tint(Shapes.px(2.5, 0.0, 6.5, 13.5, 16.0, 9.5), 0x45FF88),
         Shapes.tint(Shapes.tiles(Shapes.px(3.5, 10.0, 9.5, 12.5, 11.5, 11.0), ironTile, ironTile, ironTile), 0xE8F0F8),
         Shapes.tint(Shapes.tiles(Shapes.px(3.5, 10.0, 5.0, 12.5, 11.5, 6.5), ironTile, ironTile, ironTile), 0xE8F0F8)
      });
      Blocks.coll[B_EXIT_LOW] = null;
      Blocks.box[B_EXIT_LOW] = new double[]{0.0, 0.0, 0.25, 1.0, 1.0, 0.75};

      defModelBlock(B_EXIT_HIGH, "Level Exit Sign", lanternTile, -1.0F, 15);
      Shapes.model(B_EXIT_HIGH, new double[][]{
         Shapes.tint(Shapes.tiles(Shapes.px(0.0, 0.0, 4.0, 2.5, 16.0, 12.0), ironTile, ironTile, ironTile), 0x48525C),
         Shapes.tint(Shapes.tiles(Shapes.px(13.5, 0.0, 4.0, 16.0, 16.0, 12.0), ironTile, ironTile, ironTile), 0x48525C),
         Shapes.tint(Shapes.tiles(Shapes.px(0.0, 13.0, 3.5, 16.0, 16.0, 12.5), ironTile, ironTile, ironTile), 0x48525C),
         Shapes.tint(Shapes.px(2.5, 0.0, 6.5, 13.5, 13.0, 9.5), 0x45FF88),
         Shapes.tint(Shapes.px(3.0, 12.5, 12.0, 13.0, 15.8, 15.0), 0x50FF90),
         Shapes.tint(Shapes.px(3.0, 12.5, 1.0, 13.0, 15.8, 4.0), 0x50FF90)
      });
      Blocks.coll[B_EXIT_HIGH] = null;
      Blocks.box[B_EXIT_HIGH] = new double[]{0.0, 0.0, 0.25, 1.0, 1.0, 0.75};
   }

   private static void defModelBlock(int id, String name, int tile, float hard, int emit) {
      Blocks.def(id, name, hard, false, tile);
      Blocks.solid[id] = true;
      Blocks.opaque[id] = false;
      Blocks.opacity[id] = 0;
      Blocks.emit[id] = emit;
      Blocks.drop[id] = id;
   }

   /**
    * Returns true if the player's bounding box is currently underneath a 3D Office Desk ({@link #B_DESK})
    * or a 1-block crawlspace so releasing Shift while under a desk keeps the player safely crouched.
    */
   public static boolean mustStayCrouched(World w, Player p) {
      if (w == null || p == null) {
         return false;
      }
      int by = (int)Math.floor(p.y);
      double r = 0.29;
      int x0 = (int)Math.floor(p.x - r);
      int x1 = (int)Math.floor(p.x + r);
      int z0 = (int)Math.floor(p.z - r);
      int z1 = (int)Math.floor(p.z + r);
      for (int x = x0; x <= x1; x++) {
         for (int z = z0; z <= z1; z++) {
            int b = w.getBlock(x, by, z);
            if (b == B_DESK) {
               return true;
            }
            int above = w.getBlock(x, by + 1, z);
            if (b == Blocks.AIR && above != Blocks.AIR && above != B_LOCKER_HIGH && above != B_EXIT_HIGH && Blocks.solid[above] && Blocks.coll[above] != null) {
               return true;
            }
         }
      }
      return false;
   }

   /**
    * Right-click handler for drinking Almond Water (restores +6 Health, +6 Hunger, +45% Sanity, and calms panic).
    */
   public static boolean drinkAlmondWater(Game g, Stack st) {
      if (!active || g == null || g.player == null || st == null || st.id != ALMOND_WATER) {
         return false;
      }
      g.player.health = Math.min(20, g.player.health + 6);
      g.player.food = Math.min(20, g.player.food + 6);
      g.player.sat = Math.min(g.player.food, g.player.sat + 8.0F);
      sanity = Math.min(100, sanity + 45);
      panicLevel = Math.max(0.02F, panicLevel * 0.35F);
      stareTicks = 0;
      st.n--;
      if (st.n <= 0) {
         g.inv[g.sel] = null;
      }
      g.toast("Almond Water", "+3 Hearts, +45% Sanity — Your mind clears.");
      g.say("§b[Almond Water] §7You drink the sweet vanilla-almond water. Health & Sanity restored!");
      return true;
   }

   /**
    * Right-click handler for Backrooms interactive 3D models & blocks:
    * - 3D Office Desks ({@link #B_DESK}): crouch & crawl directly underneath the desk!
    * - 3D Steel Hiding Lockers ({@link #B_LOCKER_LOW}, {@link #B_LOCKER_HIGH}): step inside / step out of the locker!
    * - 3D Almond Water Coolers ({@link #B_WATER_COOLER}, {@link #B_COOLER_JUG}, or {@code ICE} on {@code QUARTZ_BLOCK}): dispense 2x Almond Water!
    * - 3D Illuminated Level Exit Doors ({@link #B_EXIT_LOW}, {@link #B_EXIT_HIGH}): advance to the next Backrooms Level!
    */
   public static boolean onUseBlock(Game g, int x, int y, int z, int id) {
      if (!active || g == null || g.world == null || g.player == null) {
         return false;
      }
      // 1. 3D Illuminated Level Exit Door (B_EXIT_LOW / B_EXIT_HIGH)
      if (id == B_EXIT_LOW || id == B_EXIT_HIGH) {
         if (instance != null) {
            instance.useExitDoor(g);
         }
         return true;
      }

      // 2. 3D Steel Hiding Locker (B_LOCKER_LOW / B_LOCKER_HIGH)
      if (id == B_LOCKER_LOW || id == B_LOCKER_HIGH) {
         int baseY = (id == B_LOCKER_HIGH) ? (y - 1) : y;
         double dx = g.player.x - (x + 0.5);
         double dz = g.player.z - (z + 0.5);
         if (dx * dx + dz * dz < 0.35) {
            g.player.setPos(x + 0.5, baseY, z + 1.35);
            g.toast("Hiding Locker", "Stepped out of the Hiding Locker.");
         } else {
            g.player.setPos(x + 0.5, baseY, z + 0.5);
            g.player.vx = g.player.vy = g.player.vz = 0.0;
            g.toast("Hiding inside 3D Locker!", "You are undetectable through the vent slits.");
            g.say("§b[Backrooms] §7You stepped inside the §f3D Steel Hiding Locker§7. Right-click again to step out.");
         }
         return true;
      }

      // 3. 3D Office Desk (B_DESK) -> Crouch & crawl underneath the desk!
      if (id == B_DESK) {
         g.player.sneaking = true;
         g.player.updateBox();
         g.player.setPos(x + 0.5, y, z + 0.5);
         g.player.vx = g.player.vy = g.player.vz = 0.0;
         g.toast("Under 3D Office Desk", "Crouched underneath the desk — Stalker cannot see you!");
         return true;
      }

      // 4. 3D Almond Water Cooler Dispenser (B_WATER_COOLER / B_COOLER_JUG or legacy ICE on QUARTZ_BLOCK)
      if (id == B_WATER_COOLER || id == B_COOLER_JUG) {
         int baseY = (id == B_COOLER_JUG) ? (y - 1) : y;
         if (g.world.getBlock(x, baseY + 1, z) == B_COOLER_JUG) {
            g.world.setBlock(x, baseY + 1, z, Blocks.AIR);
            g.meshNow(x, baseY + 1, z);
         }
         Stack bottle = new Stack(ALMOND_WATER, 2);
         if (!g.addItem(bottle)) {
            g.world.dropItem(x + 0.5, baseY + 1.1, z + 0.5, ALMOND_WATER, 2);
         }
         g.toast("Almond Water Dispenser", "Collected 2x Almond Water bottles!");
         g.say("§b[Backrooms] §7Dispensed §f2x Almond Water§7 from the 3D Water Cooler.");
         return true;
      }
      if (id == Blocks313.ICE && g.world.getBlock(x, y - 1, z) == Blocks.QUARTZ_BLOCK) {
         g.world.setBlock(x, y, z, Blocks.GLASS);
         g.meshNow(x, y, z);
         Stack bottle = new Stack(ALMOND_WATER, 2);
         if (!g.addItem(bottle)) {
            g.world.dropItem(x + 0.5, y + 0.5, z + 0.5, ALMOND_WATER, 2);
         }
         g.toast("Almond Water Dispenser", "Collected 2x Almond Water bottles!");
         g.say("§b[Backrooms] §7Dispensed §f2x Almond Water§7 from the water cooler.");
         return true;
      }
      if (id == Blocks.QUARTZ_BLOCK && g.world.getBlock(x, y + 1, z) == Blocks313.ICE) {
         return onUseBlock(g, x, y + 1, z, Blocks313.ICE);
      }

      // 5. Legacy Inter-Level Elevator Kiosk
      if ((id == Blocks.LADDER && g.world.getBlock(x, y - 1, z) == Redstone319.SEA_LANTERN)
         || (id == Redstone319.SEA_LANTERN && g.world.getBlock(x, y + 1, z) == Blocks.LADDER)) {
         if (instance != null) {
            instance.useExitDoor(g);
            return true;
         }
      }
      return false;
   }

   /** Advances the player through a 3D Level Exit Door to the next Backrooms Level (or escapes the Pocket Backrooms). */
   public void useExitDoor(Game g) {
      if (g == null || g.world == null || g.player == null) {
         return;
      }
      this.exitCooldown = 60;
      if (!(g.world.gen instanceof BackroomsGen) && this.inPocketBackrooms) {
         // In Pocket Backrooms, upgrade worldgen or transition levels seamlessly
         g.world.gen = new BackroomsGen(g.world.seed, g.world.settings);
         this.inPocketBackrooms = false;
         teleportToLevel(g, BackroomsGen.LEVEL_1);
         return;
      }
      int curLvl = BackroomsGen.levelForY(g.player.y);
      int nextLvl;
      switch (curLvl) {
         case BackroomsGen.LEVEL_0:
            nextLvl = BackroomsGen.LEVEL_1;
            break;
         case BackroomsGen.LEVEL_1:
            nextLvl = BackroomsGen.LEVEL_2;
            break;
         case BackroomsGen.LEVEL_2:
            nextLvl = BackroomsGen.LEVEL_RUN;
            break;
         case BackroomsGen.LEVEL_RUN:
            nextLvl = BackroomsGen.LEVEL_POOLROOMS;
            break;
         case BackroomsGen.LEVEL_POOLROOMS:
         default:
            nextLvl = BackroomsGen.LEVEL_0;
            sanity = 100;
            g.player.health = 20;
            break;
      }
      teleportToLevel(g, nextLvl);
   }

   /**
    * Dynamic Backrooms fog color (RGB in 0..1), tailored per Backrooms Level:
    * - Level 0: Warm fluorescent yellow-ochre mist (0.64, 0.56, 0.26)
    * - Level 1: Cool industrial slate-grey mist (0.36, 0.39, 0.43)
    * - Level 2: Rusty steam-pipe amber mist (0.42, 0.28, 0.16)
    * - Level 37 (Poolrooms): Dreamy chlorinated aqua-cyan mist (0.58, 0.86, 0.88)
    * - Level "!": Pulsing crimson hospital emergency fog (0.52, 0.05, 0.06)
    */
   public static float[] getFogColor(float timeSec) {
      float hum = 1.0F + 0.02F * (float)Math.sin(timeSec * 376.99F);
      float baseR = 0.64F * hum;
      float baseG = 0.56F * hum;
      float baseB = 0.26F * hum;

      int lvl = instance != null ? instance.currentLevel : BackroomsGen.LEVEL_0;
      if (lvl == BackroomsGen.LEVEL_1) {
         baseR = 0.35F;
         baseG = 0.38F;
         baseB = 0.42F;
      } else if (lvl == BackroomsGen.LEVEL_2) {
         baseR = 0.38F;
         baseG = 0.25F;
         baseB = 0.15F;
      } else if (lvl == BackroomsGen.LEVEL_POOLROOMS) {
         baseR = 0.62F;
         baseG = 0.88F;
         baseB = 0.90F;
      } else if (lvl == BackroomsGen.LEVEL_RUN) {
         float alarm = 0.65F + 0.35F * (float)Math.sin(timeSec * 6.0F);
         baseR = 0.52F * alarm;
         baseG = 0.04F;
         baseB = 0.05F;
      }

      float darkMix = Math.min(1.0F, panicLevel * 0.78F + (blackoutTicks > 0 ? 0.65F : 0.0F));
      float pulse = 0.5F + 0.5F * (float)Math.sin(timeSec * 8.0F);
      float horrorR = 0.16F + 0.12F * pulse * panicLevel;
      float horrorG = 0.02F;
      float horrorB = 0.025F;

      float r = baseR + (horrorR - baseR) * darkMix;
      float g = baseG + (horrorG - baseG) * darkMix;
      float b = baseB + (horrorB - baseB) * darkMix;
      return new float[]{r, g, b};
   }

   public static float getFogNear() {
      if (blackoutTicks > 0) {
         return 0.8F;
      }
      int lvl = instance != null ? instance.currentLevel : BackroomsGen.LEVEL_0;
      if (lvl == BackroomsGen.LEVEL_POOLROOMS) {
         return 8.0F;
      }
      return Math.max(1.2F, 5.2F - panicLevel * 3.5F);
   }

   public static float getFogFar() {
      if (blackoutTicks > 0) {
         return 10.5F;
      }
      int lvl = instance != null ? instance.currentLevel : BackroomsGen.LEVEL_0;
      if (lvl == BackroomsGen.LEVEL_POOLROOMS) {
         return 38.0F;
      }
      return Math.max(12.0F, 28.0F - panicLevel * 13.0F);
   }

   @Override
   public String id() {
      return "backrooms-stalker";
   }

   @Override
   public String name() {
      return "The Backrooms & Stalker";
   }

   @Override
   public String version() {
      return "2.2.0";
   }

   @Override
   public String description() {
      return "5 Pretty Backrooms Levels, 3D Desks (Crawl Under!), 3D Hiding Lockers, 3D Water Coolers, Exits on All Levels & Stalker.";
   }

   @Override
   public boolean totalConversionInfdev() {
      return true;
   }

   private void applyHorrorSettings(Game g) {
      registerAlmondWaterItem();
      if (g == null || g.opt == null) {
         return;
      }
      if (!this.savedSettingsValid) {
         this.savedShaders = g.opt.shaders;
         this.savedSmooth = g.opt.smooth;
         this.savedFx = g.opt.fx;
         this.savedClouds = g.opt.clouds;
         this.savedWeather = g.opt.weather;
         this.savedDisasters = g.opt.disasters;
         this.savedGamma = g.opt.gamma;
         this.savedLook = g.opt.look;
         this.savedSettingsValid = true;
      }
      g.opt.shaders = true;
      g.opt.smooth = true;
      g.opt.fx = 2;
      g.opt.clouds = false;
      g.opt.weather = false;
      g.opt.disasters = false;
      g.opt.gamma = -0.12F;
      g.opt.look = Look.CINEMATIC;
   }

   private void restoreHorrorSettings(Game g) {
      unregisterAlmondWaterItem();
      if (g == null || g.opt == null || !this.savedSettingsValid) {
         return;
      }
      g.opt.shaders = this.savedShaders;
      g.opt.smooth = this.savedSmooth;
      g.opt.fx = this.savedFx;
      g.opt.clouds = this.savedClouds;
      g.opt.weather = this.savedWeather;
      g.opt.disasters = this.savedDisasters;
      g.opt.gamma = this.savedGamma;
      g.opt.look = this.savedLook;
      this.savedSettingsValid = false;
   }

   /** Ensures the player is inside a valid Backrooms level (never stranded on top of the bedrock roof). */
   public void ensurePlayerInsideBackrooms(Game g) {
      if (g == null || g.world == null || g.player == null) {
         return;
      }
      if (g.world.gen instanceof BackroomsGen) {
         if (g.player.y > BackroomsGen.POOL_CEIL_Y || g.player.y < 2.0) {
            teleportToLevel(g, BackroomsGen.LEVEL_0);
         } else {
            this.currentLevel = BackroomsGen.levelForY(g.player.y);
         }
      }
   }

   public void teleportToLevel(Game g, int level) {
      if (g == null || g.world == null || g.player == null) {
         return;
      }
      this.currentLevel = level;
      int fy = BackroomsGen.floorYForLevel(level);
      int cy = BackroomsGen.ceilYForLevel(level);
      int px = (int)Math.floor(g.player.x);
      int pz = (int)Math.floor(g.player.z);
      if (level == BackroomsGen.LEVEL_RUN) {
         px = 0; // Center of the Level "!" hospital corridor
      }

      // Guarantee a safe 3x3 arrival space at (px, fy + 1, pz)
      for (int dx = -1; dx <= 1; dx++) {
         for (int dz = -1; dz <= 1; dz++) {
            int floorBlk = level == BackroomsGen.LEVEL_POOLROOMS
               ? Blocks.QUARTZ_BLOCK
               : (level == BackroomsGen.LEVEL_RUN ? (Blocks.WOOL0 + 14) : (level == BackroomsGen.LEVEL_0 ? BackroomsGen.CARPET_BLOCK : Blocks.STONE));
            g.world.setBlock(px + dx, fy, pz + dz, floorBlk);
            for (int y = fy + 1; y < cy; y++) {
               g.world.setBlock(px + dx, y, pz + dz, Blocks.AIR);
            }
         }
      }

      despawnStalker(g);
      stalkerCooldown = level == BackroomsGen.LEVEL_POOLROOMS ? 1600 : (level == BackroomsGen.LEVEL_RUN ? 80 : 600);
      g.player.setPos(px + 0.5, fy + 1.0, pz + 0.5);
      g.player.vx = 0.0;
      g.player.vy = 0.0;
      g.player.vz = 0.0;
      g.needSpawn = false;
      g.addShake(0.35F);

      String title = getLevelTitle(level);
      g.toast(title, "Find Almond Water on desks & coolers. Hide in lockers or under desks!");
      g.say("§e[Backrooms] §7Entered §f" + title + "§7. (§f/backrooms level 0|1|2|poolrooms|run§7)");
   }

   public static String getLevelTitle(int level) {
      switch (level) {
         case BackroomsGen.LEVEL_1:
            return "LEVEL 1 // HABITABLE WAREHOUSE";
         case BackroomsGen.LEVEL_2:
            return "LEVEL 2 // PIPE DREAMS";
         case BackroomsGen.LEVEL_POOLROOMS:
            return "LEVEL 37 // THE POOLROOMS";
         case BackroomsGen.LEVEL_RUN:
            return "LEVEL ! // RUN FOR YOUR LIFE";
         case BackroomsGen.LEVEL_0:
         default:
            return "LEVEL 0 // THE LOBBY";
      }
   }

   @Override
   public void onEnable(Game g) {
      this.lastGame = g;
      active = true;
      sanity = 100;
      heartbeatBpm = 70;
      panicLevel = 0.05F;
      stalkerCooldown = 600; // 30 seconds of calm exploration before the Stalker appears
      this.stockedTables.clear();
      applyHorrorSettings(g);

      if (g != null && g.world != null && g.player != null) {
         if (!(g.world.gen instanceof BackroomsGen) && g.world.dim == Dim.OVERWORLD) {
            g.world.gen = new BackroomsGen(g.world.seed, g.world.settings);
         }
         if (g.world.gen instanceof BackroomsGen) {
            ensurePlayerInsideBackrooms(g);
         } else if (!this.inPocketBackrooms) {
            enterBackrooms(g);
         }
      }
      if (g != null) {
         g.say("§e[Backrooms] §7Active! Find §bAlmond Water§7 on desks/coolers & hide in §fLockers / Under Desks§7!");
      }
   }

   @Override
   public void onDisable(Game g) {
      active = false;
      panicLevel = 0.0F;
      jumpscareFlash = 0.0F;
      playerCurrentlyHidden = false;
      Game target = g != null ? g : this.lastGame;
      restoreFlickeredLights(target);
      despawnStalker(target);
      restoreHorrorSettings(target);
      if (target != null) {
         target.say("§c[Backrooms] §7Disabled. Normal vision and world settings restored.");
      }
   }

   @Override
   public void onInit(ModLoader.Context ctx) {
      if (ctx == null) {
         return;
      }
      // Craft 3 Damp Yellow Wallpaper (Sandstone) + 1 Torch -> 2 Fluorescent Ceiling Lights (Glowstone)
      ctx.addShapedRecipe(
         Blocks.GLOWSTONE,
         2,
         new String[]{"WW", "WT"},
         'W', Blocks.SANDSTONE,
         'T', Blocks.TORCH
      );
      // Craft 1 Water Bucket + 1 Sugar -> 2 Almond Water Bottles
      ctx.addShapelessRecipe(
         ALMOND_WATER,
         2,
         Items.WATER_BUCKET,
         Items.SUGAR
      );
   }

   @Override
   public Gen createWorldGen(int dim, long seed, WorldSettings settings) {
      if (this.worldGenEnabled && dim == Dim.OVERWORLD && (settings == null || !settings.isMinigame())) {
         return new BackroomsGen(seed, settings);
      }
      return null;
   }

   @Override
   public String hudWatermark() {
      return null;
   }

   @Override
   public boolean onCommand(Game g, String cmd, String[] args) {
      if (cmd == null) {
         return false;
      }
      String c = cmd.toLowerCase();
      if (c.equals("/backrooms")) {
         active = true;
         applyHorrorSettings(g);
         if (args != null && args.length >= 1) {
            String sub = args[0].toLowerCase();
            if (sub.equals("leave") || sub.equals("exit") || sub.equals("escape")) {
               leaveBackrooms(g);
               return true;
            }
            if (sub.equals("worldgen")) {
               this.worldGenEnabled = !this.worldGenEnabled;
               g.say("§e[Backrooms] §7Infinite Backrooms worldgen for new worlds: " + (this.worldGenEnabled ? "§aON" : "§cOFF"));
               return true;
            }
            if (sub.equals("level") && args.length >= 2) {
               sub = args[1].toLowerCase();
            }
            if (sub.equals("0") || sub.equals("lobby")) {
               teleportToLevel(g, BackroomsGen.LEVEL_0);
               return true;
            }
            if (sub.equals("1") || sub.equals("warehouse")) {
               teleportToLevel(g, BackroomsGen.LEVEL_1);
               return true;
            }
            if (sub.equals("2") || sub.equals("pipes")) {
               teleportToLevel(g, BackroomsGen.LEVEL_2);
               return true;
            }
            if (sub.equals("37") || sub.equals("poolrooms") || sub.equals("pool")) {
               teleportToLevel(g, BackroomsGen.LEVEL_POOLROOMS);
               return true;
            }
            if (sub.equals("!") || sub.equals("run")) {
               teleportToLevel(g, BackroomsGen.LEVEL_RUN);
               return true;
            }
         }
         if (g.world != null && g.world.gen instanceof BackroomsGen) {
            ensurePlayerInsideBackrooms(g);
            g.toast(getLevelTitle(this.currentLevel), "Use /backrooms level 0|1|2|poolrooms|run to switch levels!");
         } else if (this.inPocketBackrooms) {
            leaveBackrooms(g);
         } else {
            enterBackrooms(g);
         }
         return true;
      }

      if (c.equals("/stalker")) {
         active = true;
         applyHorrorSettings(g);
         if (args != null && args.length >= 1) {
            String sub = args[0].toLowerCase();
            if (sub.equals("calm") || sub.equals("despawn") || sub.equals("off")) {
               restoreFlickeredLights(g);
               despawnStalker(g);
               stalkerCooldown = 1200;
               sanity = 100;
               panicLevel = 0.03F;
               g.say("§a[Stalker] §7The footsteps fade away into the distance.");
               return true;
            }
            if (sub.equals("chase") || sub.equals("hunt") || sub.equals("attack")) {
               if (stalkerMob == null || stalkerMob.dead) {
                  spawnStalkerNearPlayer(g, false);
               }
               stalkerPhase = PHASE_CHASE;
               chaseTicks = 120;
               panicLevel = 1.0F;
               triggerLightBlackout(g, 60);
               g.addShake(0.65F);
               g.say("§4[Stalker] §c§lIT SPOTTED YOU! Duck into a Hiding Locker or crouch under a desk!");
               return true;
            }
            if (sub.equals("blackout") || sub.equals("flicker")) {
               triggerLightBlackout(g, 70);
               g.addShake(0.35F);
               g.say("§8[Stalker] §7The fluorescent tubes overhead buzz loudly and short out...");
               return true;
            }
         }
         spawnStalkerNearPlayer(g, true);
         return true;
      }

      return false;
   }

   private void enterBackrooms(Game g) {
      if (g == null || g.world == null || g.player == null) {
         return;
      }
      this.returnX = g.player.x;
      this.returnY = g.player.y;
      this.returnZ = g.player.z;
      this.inPocketBackrooms = true;
      this.currentLevel = BackroomsGen.LEVEL_0;

      int cx = (int)Math.floor(g.player.x);
      int cz = (int)Math.floor(g.player.z);
      buildPocketBackrooms(g.world, cx, POCKET_Y, cz, POCKET_RADIUS);

      g.player.setPos(cx + 0.5, POCKET_Y + 1.05, cz + 0.5);
      g.player.vx = 0.0;
      g.player.vy = 0.0;
      g.player.vz = 0.0;
      sanity = 100;
      stalkerCooldown = 400; // Peaceful exploration before Stalker appears
      // Spawn 2 Almond Water bottles right on the welcome desk at (cx + 3, POCKET_Y + 3, cz)
      g.world.dropItem(cx + 3.5, POCKET_Y + 3.15, cz + 0.5, ALMOND_WATER, 2);

      g.addShake(0.45F);
      g.toast("Noclipped into Level 0!", "Grab Almond Water on the desk & hide in lockers!");
      g.say("§e[Backrooms] §7Welcome to Level 0! Type §f/backrooms leave§7 to escape, or §f/backrooms level 1|2|poolrooms|run§7.");
   }

   private void leaveBackrooms(Game g) {
      if (g == null || g.player == null) {
         return;
      }
      restoreFlickeredLights(g);
      despawnStalker(g);
      this.inPocketBackrooms = false;
      if (this.returnY > 0.0) {
         g.player.setPos(this.returnX, this.returnY + 0.5, this.returnZ);
      } else {
         g.player.setPos(0.5, BackroomsGen.FLOOR_Y + 1.0, 0.5);
      }
      g.player.vx = 0.0;
      g.player.vy = 0.0;
      g.player.vz = 0.0;
      sanity = 100;
      panicLevel = 0.03F;
      g.say("§a[Backrooms] §7You clipped back into reality.");
   }

   /** Builds a pretty 41x41 enclosed Level 0 Backrooms complex around (cx, baseY, cz) with desks, coolers & hiding lockers. */
   private void buildPocketBackrooms(World w, int cx, int baseY, int cz, int radius) {
      int ceilY = baseY + 7;
      for (int dx = -radius; dx <= radius; dx++) {
         for (int dz = -radius; dz <= radius; dz++) {
            int wx = cx + dx;
            int wz = cz + dz;
            boolean outerBoundary = Math.abs(dx) == radius || Math.abs(dz) == radius;

            w.setBlock(wx, baseY - 1, wz, Blocks.BEDROCK);
            w.setBlock(wx, baseY, wz, BackroomsGen.CARPET_BLOCK);
            for (int y = baseY + 1; y < ceilY; y++) {
               w.setBlock(wx, y, wz, Blocks.AIR);
            }

            int cmx = Math.floorMod(wx, 8);
            int cmz = Math.floorMod(wz, 8);
            boolean light = (cmx == 3 || cmx == 4) && (cmz == 3 || cmz == 4);
            w.setBlock(wx, ceilY, wz, light ? Blocks.GLOWSTONE : ((cmx == 0 || cmz == 0) ? Blocks313.PLANKS_BIRCH : Blocks.SANDSTONE));
            w.setBlock(wx, ceilY + 1, wz, Blocks.BEDROCK);

            if (outerBoundary) {
               for (int y = baseY + 1; y < ceilY; y++) {
                  w.setBlock(wx, y, wz, Blocks.BEDROCK);
               }
               continue;
            }

            if (Math.abs(dx) <= 4 && Math.abs(dz) <= 4) {
               continue;
            }

            int rx = Math.floorMod(dx, 9);
            int rz = Math.floorMod(dz, 9);
            long h = (wx * 73428767L) ^ (wz * 912931L) ^ w.seed;
            boolean wall = false;
            boolean door = false;
            boolean pilaster = false;
            if (rx == 0 && (h & 3L) != 0L) {
               wall = true;
               if (rz == 4 || rz == 5) {
                  door = true;
               }
            }
            if (rz == 0 && ((h >>> 2) & 3L) != 0L) {
               wall = true;
               if (rx == 4 || rx == 5) {
                  door = true;
               }
            }
            if (rx == 0 && rz == 0) {
               wall = true;
               pilaster = true;
               door = false;
            }

            if (wall) {
               if (door) {
                  w.setBlock(wx, baseY, wz, Blocks313.PLANKS_BIRCH);
               }
               for (int y = baseY + 1; y < ceilY; y++) {
                  if (door && y <= baseY + 4) {
                     w.setBlock(wx, y, wz, Blocks.AIR);
                  } else if (y == baseY + 1) {
                     w.setBlock(wx, y, wz, pilaster ? Blocks.LOG_BIRCH : Blocks.PLANKS);
                  } else if (y == ceilY - 1) {
                     w.setBlock(wx, y, wz, Blocks313.PLANKS_BIRCH);
                  } else {
                     w.setBlock(wx, y, wz, pilaster ? Blocks.LOG_BIRCH : Blocks.SANDSTONE);
                  }
               }
            }
         }
      }
      // Welcome 3D Office Desk at (cx + 3, cz..cz+1) — hold Shift to crawl right underneath the 3D desk!
      w.setBlock(cx + 3, baseY + 1, cz, B_DESK);
      w.setBlock(cx + 3, baseY + 1, cz + 1, B_DESK);
      // Pair of 3D Hiding Lockers flush against the wall at (cx - 2..cx - 1, cz - 4)
      w.setBlock(cx - 2, baseY + 1, cz - 4, B_LOCKER_LOW);
      w.setBlock(cx - 2, baseY + 2, cz - 4, B_LOCKER_HIGH);
      w.setBlock(cx - 1, baseY + 1, cz - 4, B_LOCKER_LOW);
      w.setBlock(cx - 1, baseY + 2, cz - 4, B_LOCKER_HIGH);
      // 3D Almond Water Cooler at (cx - 3, cz)
      w.setBlock(cx - 3, baseY + 1, cz, B_WATER_COOLER);
      w.setBlock(cx - 3, baseY + 2, cz, B_COOLER_JUG);
      // 3D Illuminated Level Exit Door at (cx, cz + 16)
      for (int ex = -1; ex <= 1; ex++) {
         for (int ez = -1; ez <= 1; ez++) {
            w.setBlock(cx + ex, baseY, cz + 16 + ez, Blocks.WOOL0 + 5);
            for (int y = baseY + 1; y <= baseY + 3; y++) {
               w.setBlock(cx + ex, y, cz + 16 + ez, Blocks.AIR);
            }
         }
      }
      w.setBlock(cx, baseY + 1, cz + 16, B_EXIT_LOW);
      w.setBlock(cx, baseY + 2, cz + 16, B_EXIT_HIGH);
   }

   /**
    * Checks whether the player is currently hidden underneath a 3D Office Desk ({@link #B_DESK}),
    * inside a 3D Steel Hiding Locker ({@link #B_LOCKER_LOW}/{@link #B_LOCKER_HIGH}), or in a 3-walled alcove.
    */
   public static boolean checkPlayerHidden(World w, Player p) {
      if (w == null || p == null) {
         return false;
      }
      int bx = (int)Math.floor(p.x);
      int by = (int)Math.floor(p.y);
      int bz = (int)Math.floor(p.z);

      int here = w.getBlock(bx, by, bz);
      int head = w.getBlock(bx, by + 1, bz);

      // 1. Directly underneath a 3D Office Desk (B_DESK) or inside a 3D Steel Hiding Locker (B_LOCKER_LOW/HIGH)
      if (here == B_DESK || here == B_LOCKER_LOW || head == B_LOCKER_HIGH) {
         return true;
      }

      // 2. Crouching beside a 3D Office Desk or 3D Locker, or under a solid block at (by + 1) or (by + 2)
      int overhead1 = w.getBlock(bx, by + 1, bz);
      int overhead2 = w.getBlock(bx, by + 2, bz);
      int overhead3 = w.getBlock(bx, by + 3, bz);
      if (p.sneaking) {
         if ((overhead1 != Blocks.AIR && Blocks.solid[overhead1]) || (overhead2 != Blocks.AIR && Blocks.solid[overhead2])) {
            return true;
         }
         for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
               int nb = w.getBlock(bx + dx, by, bz + dz);
               if (nb == B_DESK || nb == B_LOCKER_LOW) {
                  return true;
               }
            }
         }
      }

      // Count solid walls around the player at body/head height
      int wallsAround = 0;
      if (Blocks.solid[w.getBlock(bx + 1, by + 1, bz)]) wallsAround++;
      if (Blocks.solid[w.getBlock(bx - 1, by + 1, bz)]) wallsAround++;
      if (Blocks.solid[w.getBlock(bx, by + 1, bz + 1)]) wallsAround++;
      if (Blocks.solid[w.getBlock(bx, by + 1, bz - 1)]) wallsAround++;

      if (wallsAround >= 3) {
         return true;
      }
      if (p.sneaking && (wallsAround >= 2 || (overhead3 != Blocks.AIR && overhead3 != Blocks.SANDSTONE && overhead3 != Blocks.GLOWSTONE))) {
         return true;
      }
      return false;
   }

   /**
    * Periodically spawns physical, findable Almond Water bottles on top of nearby 3D Office Desks ({@link #B_DESK})
    * and Warehouse Supply Shelves so the player can find Almond Water naturally in the world!
    */
   private void tickFindableAlmondWater(Game g) {
      if ((g.tickCount % 35) != 0 || g.world == null || g.player == null) {
         return;
      }
      World w = g.world;
      int px = (int)Math.floor(g.player.x);
      int py = (int)Math.floor(g.player.y);
      int pz = (int)Math.floor(g.player.z);

      for (int dx = -12; dx <= 12; dx++) {
         for (int dz = -12; dz <= 12; dz++) {
            for (int dy = -2; dy <= 3; dy++) {
               int wx = px + dx;
               int wy = py + dy;
               int wz = pz + dz;
               int b = w.getBlock(wx, wy, wz);
               if (b == B_DESK && w.getBlock(wx, wy + 1, wz) == Blocks.AIR) {
                  long key = ((long)(wx & 0x3FFFFFF) << 38) | ((long)(wz & 0x3FFFFFF) << 12) | (wy & 0xFFF);
                  if (this.stockedTables.add(key)) {
                     World.Item dropped = w.dropItem(wx + 0.5, wy + 1.08, wz + 0.5, ALMOND_WATER, 1, 0);
                     if (dropped != null) {
                        dropped.vx = 0.0;
                        dropped.vy = 0.0;
                        dropped.vz = 0.0;
                     }
                     return;
                  }
               }
               if (b == Blocks.PLANKS && w.getBlock(wx, wy + 1, wz) == Blocks.AIR) {
                  int below = w.getBlock(wx, wy - 1, wz);
                  if (below == Blocks.FENCE || below == Blocks.BOOKSHELF) {
                     long key = ((long)(wx & 0x3FFFFFF) << 38) | ((long)(wz & 0x3FFFFFF) << 12) | (wy & 0xFFF);
                     if (this.stockedTables.add(key)) {
                        World.Item dropped = w.dropItem(wx + 0.5, wy + 1.15, wz + 0.5, ALMOND_WATER, 1, 0);
                        if (dropped != null) {
                           dropped.vx = 0.0;
                           dropped.vy = 0.0;
                           dropped.vz = 0.0;
                        }
                        return;
                     }
                  }
               }
            }
         }
      }
   }

   @Override
   public void onGameTick(Game g) {
      if (g == null || g.world == null || g.player == null || g.mobs == null) {
         return;
      }
      this.lastGame = g;
      active = true;

      if (!this.savedSettingsValid) {
         applyHorrorSettings(g);
      }

      if (g.world.gen instanceof BackroomsGen) {
         ensurePlayerInsideBackrooms(g);
      }

      // Check if player walked into a 3D Illuminated Level Exit Door (B_EXIT_LOW / B_EXIT_HIGH)
      if (this.exitCooldown > 0) {
         this.exitCooldown--;
      } else {
         int bx = (int)Math.floor(g.player.x);
         int by = (int)Math.floor(g.player.y);
         int bz = (int)Math.floor(g.player.z);
         int b0 = g.world.getBlock(bx, by, bz);
         int b1 = g.world.getBlock(bx, by + 1, bz);
         if (b0 == B_EXIT_LOW || b0 == B_EXIT_HIGH || b1 == B_EXIT_LOW || b1 == B_EXIT_HIGH) {
            useExitDoor(g);
            return;
         }
      }

      // Update Hiding Spot status
      playerCurrentlyHidden = checkPlayerHidden(g.world, g.player);
      if (playerCurrentlyHidden && g.tickCount % 25 == 0 && sanity < 100) {
         sanity = Math.min(100, sanity + 1);
      }
      // Poolrooms is a tranquil sanctuary that gently restores Sanity
      if (this.currentLevel == BackroomsGen.LEVEL_POOLROOMS && g.tickCount % 40 == 0 && sanity < 100) {
         sanity = Math.min(100, sanity + 1);
      }

      // Stock nearby Office Desks & Supply Shelves with physical Almond Water bottles
      tickFindableAlmondWater(g);

      if (jumpscareFlash > 0.01F) {
         jumpscareFlash *= 0.85F;
      } else {
         jumpscareFlash = 0.0F;
      }

      if (blackoutTicks > 0) {
         blackoutTicks--;
         if (blackoutTicks == 0) {
            restoreFlickeredLights(g);
         }
      }

      boolean inBackroomsNow = this.inPocketBackrooms || (g.world.gen instanceof BackroomsGen);

      // Clean up dead reference
      if (stalkerMob != null && (stalkerMob.dead || !g.mobs.list.contains(stalkerMob))) {
         stalkerMob = null;
         stalkerPhase = PHASE_STALK;
         stalkerCooldown = 900; // 45s cooldown after despawn/kill
      }

      // Balanced, slow-burn Stalker spawning (rare in Poolrooms, active in Level 0/1/2/!)
      if (stalkerMob == null) {
         panicLevel *= 0.92F;
         heartbeatBpm = (int)(68 + panicLevel * 60.0F);
         if (stalkerCooldown > 0) {
            stalkerCooldown--;
         } else if (inBackroomsNow && !playerCurrentlyHidden) {
            int spawnChance = this.currentLevel == BackroomsGen.LEVEL_POOLROOMS ? 800 : 120;
            if (this.rng.nextInt(spawnChance) == 0) {
               spawnStalkerNearPlayer(g, false);
               stalkerCooldown = 900;
            }
         }
      } else {
         tickStalkerAtmosphere(g);
      }
   }

   /**
    * Called directly from {@link Mobs312} during {@code Mobs.tick()} so the Stalker uses native movement,
    * stride animation, and smooth 60 FPS pose interpolation — with balanced speed and Hiding Spot awareness!
    */
   public static void stepStalkerIntent(
      Mobs ms,
      Mobs.Mob m,
      World w,
      Player p,
      double dx,
      double dz,
      double dy,
      double d2,
      double d3,
      Mobs312.Intent in
   ) {
      stalkerMob = m;
      m.health = Math.max(m.health, 60);
      m.burn = 0;
      if (m.cd > 0) {
         m.cd--;
      }

      // If the player is inside a Hiding Locker or crouched under a desk, the Stalker cannot lock onto them!
      if (playerCurrentlyHidden && d2 > 2.6) {
         m.angry = 0;
         m.hasTarget = false;
         ms.wanderAI(m);
         in.moving = m.moving;
         in.speed = 0.45;
         return;
      }

      boolean watched = isPlayerLookingAtStalker(ms, w, p, m, d3);
      m.dirYaw = Mobs.angleTo(dx, dz);
      in.headYaw = m.dirYaw;
      in.headPitch = (float)Math.toDegrees(-Math.atan2(dy, Math.max(0.01, d2)));

      if (stalkerPhase == PHASE_CHASE) {
         m.angry = 200;
         m.hasTarget = true;
         in.moving = d2 > 1.25;
         // Outrunnable chase speed (1.16 vs old 2.05) so sprinting players can escape & hide!
         in.speed = 1.16;
         if (d2 < 1.95 && Math.abs(dy) < 2.6 && m.cd <= 0) {
            m.cd = 36; // 1.8s between swings
            m.attackAnim = 10;
            jumpscareFlash = 0.85F;
            ms.g.addShake(0.75F);
            ms.g.damagePlayer(3, m.x, m.z); // 1.5 hearts instead of 3 hearts
         }
      } else if (watched && d3 < 30.0) {
         // Weeping-Angel freeze while stared at: locks eyes with you from afar
         stalkerPhase = PHASE_FROZEN;
         m.angry = 100;
         in.moving = false;
         in.speed = 0.0;
      } else {
         // Distant corner-stalking: stays 11..24 blocks away (never walks right into your face unprovoked!)
         stalkerPhase = PHASE_STALK;
         m.angry = 0;
         if (d2 > 12.5) {
            in.moving = true;
            in.speed = d2 > 22.0 ? 0.95 : 0.62;
         } else {
            in.moving = false;
            in.speed = 0.0;
         }
      }
   }

   private static boolean isPlayerLookingAtStalker(Mobs ms, World w, Player p, Mobs.Mob m, double d3) {
      if (playerCurrentlyHidden) {
         return false;
      }
      if (d3 < 0.8) {
         return true;
      }
      double ex = p.x;
      double ey = p.y + p.eye;
      double ez = p.z;
      double yaw = Math.toRadians(p.yaw);
      double pitch = Math.toRadians(p.pitch);
      double lx = -Math.sin(yaw) * Math.cos(pitch);
      double ly = -Math.sin(pitch);
      double lz = Math.cos(yaw) * Math.cos(pitch);

      double hx = m.x - ex;
      double hy = (m.y + 2.3) - ey;
      double hz = m.z - ez;
      double dist = Math.sqrt(hx * hx + hy * hy + hz * hz);
      double dot = (lx * hx + ly * hy + lz * hz) / Math.max(0.001, dist);
      if (dot < 0.78) {
         return false;
      }
      return ms.hasLOS(w, ex, ey, ez, m.x, m.y + 2.3, m.z);
   }

   private void tickStalkerAtmosphere(Game g) {
      Player p = g.player;
      double dx = p.x - stalkerMob.x;
      double dy = p.y - stalkerMob.y;
      double dz = p.z - stalkerMob.z;
      double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
      lastDistToPlayer = (float)dist;

      // If the player escaped > 42 blocks away or switched levels, despawn the Stalker peacefully
      if (dist > 42.0 || Math.abs(dy) > 10.0) {
         despawnStalker(g);
         stalkerCooldown = 700;
         return;
      }

      // If the player ducks into a Hiding Spot, the Stalker loses track of them and leaves!
      if (playerCurrentlyHidden) {
         hiddenTicks++;
         stareTicks = Math.max(0, stareTicks - 4);
         if (stalkerPhase == PHASE_CHASE && hiddenTicks > 35 && dist > 3.0) {
            stalkerPhase = PHASE_STALK;
            chaseTicks = 0;
            despawnStalker(g);
            stalkerCooldown = 900;
            g.toast("You lost it!", "Hiding kept you safe. The Stalker wandered away.");
            g.say("§a[Backrooms] §7The Stalker lost your trail while you were hiding.");
            return;
         }
      } else {
         hiddenTicks = 0;
      }

      boolean watched = isPlayerLookingAtStalker(g.mobs, g.world, p, stalkerMob, dist);

      // Update continuous panic level (0..1) and heart rate BPM for VHS shader & HUD
      float targetPanic = 0.04F;
      if (stalkerPhase == PHASE_CHASE) {
         targetPanic = 0.92F;
      } else if (watched) {
         targetPanic = Math.min(0.85F, 0.30F + (float)stareTicks / 95.0F);
      } else if (dist < 16.0 && !playerCurrentlyHidden) {
         targetPanic = Math.max(0.10F, (float)((16.0 - dist) / 16.0) * 0.45F);
      }
      if (blackoutTicks > 0) {
         targetPanic = Math.max(targetPanic, 0.65F);
      }
      panicLevel += (targetPanic - panicLevel) * 0.10F;
      heartbeatBpm = (int)(68 + panicLevel * 108.0F + (g.tickCount % 7 == 0 ? 2 : 0));

      // Emit dark shadow/crimson static particles around the Stalker
      if (g.rend != null && (g.tickCount & 1) == 0) {
         g.portalParticle(
            stalkerMob.x + (this.rng.nextDouble() - 0.5) * 0.9,
            stalkerMob.y + 0.4 + this.rng.nextDouble() * 2.7,
            stalkerMob.z + (this.rng.nextDouble() - 0.5) * 0.9,
            0.85F, 0.06F, 0.08F
         );
      }

      if (stalkerPhase == PHASE_CHASE) {
         chaseTicks--;
         if (dist < 10.0 && g.tickCount % 6 == 0) {
            g.addShake(Math.min(0.45F, (float)((11.0 - dist) * 0.035)));
         }
         if (g.tickCount % 45 == 0) {
            sanity = Math.max(0, sanity - 2);
         }
         // When the chase ends, the Stalker vanishes completely for 45 seconds so the player gets a breather!
         if (chaseTicks <= 0) {
            triggerLightBlackout(g, 35);
            despawnStalker(g);
            stalkerCooldown = 900;
            g.say("§7[Stalker] §8The entity vanished back into the dark corridors.");
         }
         return;
      }

      // Forgiving 4.5-second (90-tick) stare window before the Stalker snaps
      if (watched && dist < 26.0) {
         stareTicks++;
         g.addShake(Math.min(0.28F, 0.03F + stareTicks * 0.003F));
         if (stareTicks % 20 == 0) {
            sanity = Math.max(0, sanity - 2);
         }
         if (stareTicks == 20) {
            g.toast("It sees you staring...", "Look away or duck into a Hiding Locker!");
         }
         if (stareTicks > 90) {
            stareTicks = 0;
            triggerLightBlackout(g, 55);
            jumpscareFlash = 0.65F;
            g.addShake(0.65F);
            // 50% chance to vanish in a spook blackout, 50% chance to chase for 6 seconds
            if (this.rng.nextBoolean()) {
               stalkerPhase = PHASE_CHASE;
               chaseTicks = 120;
               g.say("§4[Stalker] §c§lTHE LIGHTS SHORTED OUT — RUN OR HIDE!");
            } else {
               despawnStalker(g);
               stalkerCooldown = 700;
               g.say("§8[Stalker] §7The lights flickered and the silhouette vanished into thin air.");
            }
         }
      } else {
         if (stareTicks > 0) {
            stareTicks = Math.max(0, stareTicks - 3);
         }
         if (g.tickCount % 60 == 0 && sanity < 100) {
            sanity = Math.min(100, sanity + 1);
         }
         // If the player walks right up to the Stalker (< 6.5m), it usually vanishes in a blackout (75%) instead of always attacking
         if (dist < 6.5 && !playerCurrentlyHidden) {
            triggerLightBlackout(g, 40);
            if (this.rng.nextInt(4) == 0) {
               stalkerPhase = PHASE_CHASE;
               chaseTicks = 110;
               g.toast("Too close!", "Run or duck into a Hiding Locker!");
            } else {
               despawnStalker(g);
               stalkerCooldown = 600;
               g.toast("It vanished...", "The silhouette dissolved into static as you approached.");
            }
         }
      }
   }

   private void spawnStalkerNearPlayer(Game g, boolean announce) {
      if (g == null || g.world == null || g.player == null || g.mobs == null) {
         return;
      }
      despawnStalker(g);
      double[] pos = findStalkerSpot(g, 16.0, 26.0);
      stalkerMob = g.mobs.spawn(Mobs.ENDERMAN, pos[0], pos[1], pos[2]);
      if (stalkerMob != null) {
         stalkerMob.tag = STALKER_TAG;
         stalkerMob.health = 60;
         stalkerMob.scale = 1.0F;
         stalkerPhase = PHASE_STALK;
         stareTicks = 0;
         chaseTicks = 0;
         if (announce) {
            triggerLightBlackout(g, 35);
            g.addShake(0.35F);
            g.toast("Distant footsteps...", "Something is watching from down the hall.");
            g.say("§4[Stalker] §7You hear distant footsteps echoing through the corridors...");
         }
      }
   }

   private double[] findStalkerSpot(Game g, double minDist, double maxDist) {
      Player p = g.player;
      World w = g.world;
      double yawRad = Math.toRadians(p.yaw);
      double behindAngle = Math.atan2(-Math.cos(yawRad), Math.sin(yawRad));

      int targetFloorY = (w.gen instanceof BackroomsGen)
         ? BackroomsGen.floorYForLevel(BackroomsGen.levelForY(p.y))
         : (this.inPocketBackrooms ? POCKET_Y : (int)Math.floor(p.y) - 1);

      for (int attempt = 0; attempt < 28; attempt++) {
         double spread = (this.rng.nextDouble() - 0.5) * Math.PI * 1.4;
         double ang = behindAngle + spread;
         double d = minDist + this.rng.nextDouble() * (maxDist - minDist);
         int bx = (int)Math.floor(p.x + Math.cos(ang) * d);
         int bz = (int)Math.floor(p.z + Math.sin(ang) * d);

         if ((w.gen instanceof BackroomsGen) || this.inPocketBackrooms) {
            if (w.getBlock(bx, targetFloorY + 1, bz) == Blocks.AIR
               && w.getBlock(bx, targetFloorY + 2, bz) == Blocks.AIR
               && w.getBlock(bx, targetFloorY + 3, bz) == Blocks.AIR) {
               return new double[]{bx + 0.5, targetFloorY + 1.0, bz + 0.5};
            }
         } else {
            for (int y = (int)Math.floor(p.y) + 6; y >= Math.max(1, (int)Math.floor(p.y) - 8); y--) {
               if (Blocks.solid[w.getBlock(bx, y, bz)]
                  && w.getBlock(bx, y + 1, bz) == Blocks.AIR
                  && w.getBlock(bx, y + 2, bz) == Blocks.AIR
                  && w.getBlock(bx, y + 3, bz) == Blocks.AIR) {
                  return new double[]{bx + 0.5, y + 1.0, bz + 0.5};
               }
            }
         }
      }
      double fallbackX = p.x + Math.sin(yawRad) * minDist;
      double fallbackZ = p.z - Math.cos(yawRad) * minDist;
      return new double[]{fallbackX, targetFloorY + 1.0, fallbackZ};
   }

   private void triggerLightBlackout(Game g, int durationTicks) {
      if (g == null || g.world == null || g.player == null) {
         return;
      }
      restoreFlickeredLights(g);
      int px = (int)Math.floor(g.player.x);
      int py = (int)Math.floor(g.player.y);
      int pz = (int)Math.floor(g.player.z);

      for (int dx = -12; dx <= 12 && this.flickeredCount < this.flickeredLightsX.length; dx++) {
         for (int dz = -12; dz <= 12 && this.flickeredCount < this.flickeredLightsX.length; dz++) {
            for (int dy = -2; dy <= 8 && this.flickeredCount < this.flickeredLightsX.length; dy++) {
               int wx = px + dx;
               int wy = py + dy;
               int wz = pz + dz;
               if (g.world.getBlock(wx, wy, wz) == Blocks.GLOWSTONE) {
                  this.flickeredLightsX[this.flickeredCount] = wx;
                  this.flickeredLightsY[this.flickeredCount] = wy;
                  this.flickeredLightsZ[this.flickeredCount] = wz;
                  this.flickeredCount++;
                  g.world.setBlock(wx, wy, wz, Blocks.GLASS);
               }
            }
         }
      }
      blackoutTicks = durationTicks;
   }

   private void restoreFlickeredLights(Game g) {
      if (g != null && g.world != null) {
         for (int i = 0; i < this.flickeredCount; i++) {
            int wx = this.flickeredLightsX[i];
            int wy = this.flickeredLightsY[i];
            int wz = this.flickeredLightsZ[i];
            if (g.world.getBlock(wx, wy, wz) == Blocks.GLASS) {
               g.world.setBlock(wx, wy, wz, Blocks.GLOWSTONE);
            }
         }
      }
      this.flickeredCount = 0;
      blackoutTicks = 0;
   }

   private void despawnStalker(Game g) {
      if (stalkerMob != null) {
         stalkerMob.dead = true;
         if (g != null && g.mobs != null) {
            g.mobs.list.remove(stalkerMob);
         }
         stalkerMob = null;
      }
      stalkerPhase = PHASE_STALK;
      stareTicks = 0;
      chaseTicks = 0;
   }

   // ====================================================================================================
   // CUSTOM 3D HORROR ENTITY RENDERER: "THE BACTERIA / WIRE-STALKER"
   // ====================================================================================================

   private static void stalkerBox(
      float x0,
      float y0,
      float z0,
      float x1,
      float y1,
      float z1,
      int rgb,
      float shadeScale
   ) {
      float r = ((rgb >> 16) & 0xFF) / 255.0F * shadeScale;
      float g = ((rgb >> 8) & 0xFF) / 255.0F * shadeScale;
      float b = (rgb & 0xFF) / 255.0F * shadeScale;

      GL11.glBegin(GL11.GL_QUADS);
      GL11.glColor3f(Math.min(1.0F, r * 1.12F), Math.min(1.0F, g * 1.12F), Math.min(1.0F, b * 1.12F));
      GL11.glVertex3f(x0, y1, z0);
      GL11.glVertex3f(x1, y1, z0);
      GL11.glVertex3f(x1, y1, z1);
      GL11.glVertex3f(x0, y1, z1);

      GL11.glColor3f(r * 0.55F, g * 0.55F, b * 0.55F);
      GL11.glVertex3f(x0, y0, z1);
      GL11.glVertex3f(x1, y0, z1);
      GL11.glVertex3f(x1, y0, z0);
      GL11.glVertex3f(x0, y0, z0);

      GL11.glColor3f(r, g, b);
      GL11.glVertex3f(x0, y0, z1);
      GL11.glVertex3f(x0, y1, z1);
      GL11.glVertex3f(x1, y1, z1);
      GL11.glVertex3f(x1, y0, z1);

      GL11.glColor3f(r * 0.82F, g * 0.82F, b * 0.82F);
      GL11.glVertex3f(x1, y0, z0);
      GL11.glVertex3f(x1, y1, z0);
      GL11.glVertex3f(x0, y1, z0);
      GL11.glVertex3f(x0, y0, z0);

      GL11.glColor3f(r * 0.72F, g * 0.72F, b * 0.72F);
      GL11.glVertex3f(x1, y0, z1);
      GL11.glVertex3f(x1, y1, z1);
      GL11.glVertex3f(x1, y1, z0);
      GL11.glVertex3f(x1, y0, z0);

      GL11.glColor3f(r * 0.72F, g * 0.72F, b * 0.72F);
      GL11.glVertex3f(x0, y0, z0);
      GL11.glVertex3f(x0, y1, z0);
      GL11.glVertex3f(x0, y1, z1);
      GL11.glVertex3f(x0, y0, z1);
      GL11.glEnd();
   }

   public static void drawStalkerEntity(
      Mobs.Mob m,
      World w,
      Renderer rend,
      float pt,
      double x,
      double y,
      double z
   ) {
      float bodyYaw = MobRender.lerpAngle(m.pyaw, m.yaw, pt);
      float headYaw = MobRender.lerpAngle(m.pheadYaw, m.headYaw, pt) - bodyYaw;
      float headPitch = m.pheadPitch + (m.headPitch - m.pheadPitch) * pt;
      float walk = m.pwalk + (m.walk - m.pwalk) * pt;
      float walkAmp = m.pwalkAmp + (m.walkAmp - m.pwalkAmp) * pt;
      float time = m.age + pt;
      float bright = Math.max(0.35F, rend.brightnessAt(w, x, y + 1.5, z));

      int VoidBlack = 0x0A090E;
      int BoneDark = 0x1A161E;
      int CrimsonFlesh = 0x4A090E;
      int ToothPale = 0xDFD6BE;
      int ClawCrimson = 0x8A0D14;

      if (m.hurtTime > 0) {
         VoidBlack = 0x5A0E12;
         BoneDark = 0x7A181E;
      }

      boolean texWasOn = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
      GL11.glDisable(GL11.GL_TEXTURE_2D);
      GL11.glPushMatrix();
      GL11.glTranslated(x - rend.camX, y - rend.camY, z - rend.camZ);
      GL11.glRotatef(-bodyYaw, 0.0F, 1.0F, 0.0F);

      float twitch = (stalkerPhase == PHASE_FROZEN || stalkerPhase == PHASE_CHASE)
         ? (float)Math.sin(time * 2.9F) * 1.8F + (float)Math.sin(time * 17.3F) * 2.6F
         : (float)Math.sin(time * 0.15F) * 0.8F;

      // 1. Spidery Digitigrade Legs
      float legSwing = (float)Math.sin(walk * 0.7F) * 38.0F * walkAmp;
      for (int side = -1; side <= 1; side += 2) {
         float sSwing = side * legSwing;
         GL11.glPushMatrix();
         GL11.glTranslatef(side * 0.19F, 1.45F, 0.0F);
         GL11.glRotatef(sSwing - 8.0F, 1.0F, 0.0F, 0.0F);
         GL11.glRotatef(side * 3.0F, 0.0F, 0.0F, 1.0F);
         stalkerBox(-0.06F, -0.75F, -0.06F, 0.06F, 0.0F, 0.06F, VoidBlack, bright);
         GL11.glTranslatef(0.0F, -0.72F, 0.0F);
         GL11.glRotatef(18.0F - sSwing * 0.35F, 1.0F, 0.0F, 0.0F);
         stalkerBox(-0.048F, -0.76F, -0.048F, 0.048F, 0.02F, 0.048F, BoneDark, bright);
         stalkerBox(-0.07F, -0.76F, -0.05F, 0.07F, -0.70F, 0.22F, VoidBlack, bright);
         GL11.glPopMatrix();
      }

      // 2. Contorted Skeletal Torso & Ribcage
      GL11.glPushMatrix();
      GL11.glTranslatef(0.0F, 1.42F, 0.0F);
      float hunch = stalkerPhase == PHASE_CHASE ? 22.0F : 12.0F;
      GL11.glRotatef(hunch + (float)Math.sin(time * 0.12F) * 2.5F, 1.0F, 0.0F, 0.0F);
      GL11.glRotatef(twitch * 0.4F, 0.0F, 0.0F, 1.0F);

      stalkerBox(-0.22F, -0.05F, -0.11F, 0.22F, 0.14F, 0.11F, VoidBlack, bright);
      stalkerBox(-0.07F, 0.10F, -0.09F, 0.07F, 1.15F, 0.07F, VoidBlack, bright);
      stalkerBox(-0.11F, 0.45F, -0.05F, 0.11F, 0.88F, 0.09F, CrimsonFlesh, 1.0F);

      float breath = (float)Math.sin(time * 0.22F) * 0.015F;
      for (int r = 0; r < 5; r++) {
         float ry = 0.28F + r * 0.16F;
         float rw = 0.23F + (r >= 2 ? 0.05F : 0.0F) + breath;
         stalkerBox(-rw, ry, -0.13F, rw, ry + 0.065F, 0.14F, BoneDark, bright);
      }
      stalkerBox(-0.38F, 1.02F, -0.14F, 0.38F, 1.16F, 0.14F, VoidBlack, bright);

      // 3. Six Writhing Dorsal Tendrils
      for (int t = 0; t < 6; t++) {
         int side = (t & 1) == 0 ? -1 : 1;
         int row = t / 2;
         GL11.glPushMatrix();
         GL11.glTranslatef(side * (0.14F + row * 0.06F), 0.55F + row * 0.22F, -0.11F);
         float wave1 = (float)Math.sin(time * 0.24F + t * 1.3F) * 28.0F;
         float wave2 = (float)Math.cos(time * 0.19F + t * 0.9F) * 24.0F;
         GL11.glRotatef(side * (28.0F + row * 12.0F) + wave1, 0.0F, 1.0F, 0.0F);
         GL11.glRotatef(-20.0F + wave2, 1.0F, 0.0F, 0.0F);
         stalkerBox(-0.03F, -0.03F, -0.48F, 0.03F, 0.03F, 0.0F, VoidBlack, bright);
         GL11.glTranslatef(0.0F, 0.0F, -0.45F);
         GL11.glRotatef(wave1 * 0.8F, 0.0F, 1.0F, 0.0F);
         GL11.glRotatef(wave2 * 0.8F, 1.0F, 0.0F, 0.0F);
         stalkerBox(-0.02F, -0.02F, -0.46F, 0.02F, 0.02F, 0.0F, ClawCrimson, 0.95F);
         GL11.glPopMatrix();
      }

      // 4. Multi-Jointed Arms & 3-Pronged Claws
      float armSwing = (float)Math.cos(walk * 0.7F) * 32.0F * walkAmp;
      for (int side = -1; side <= 1; side += 2) {
         GL11.glPushMatrix();
         GL11.glTranslatef(side * 0.36F, 1.08F, 0.02F);
         float reachPitch;
         if (stalkerPhase == PHASE_CHASE) {
            reachPitch = 78.0F + side * armSwing * 0.6F + (float)Math.sin(time * 0.9F + side) * 12.0F;
            if (m.attackAnim > 0) {
               reachPitch += (float)Math.sin((10 - m.attackAnim + pt) * 0.5F) * 45.0F;
            }
         } else if (stalkerPhase == PHASE_FROZEN) {
            reachPitch = 28.0F + twitch * 1.5F;
         } else {
            reachPitch = 10.0F - side * armSwing;
         }
         GL11.glRotatef(reachPitch, 1.0F, 0.0F, 0.0F);
         GL11.glRotatef(side * (8.0F + (float)Math.sin(time * 0.14F) * 4.0F), 0.0F, 0.0F, 1.0F);

         stalkerBox(-0.055F, -0.78F, -0.055F, 0.055F, 0.04F, 0.055F, VoidBlack, bright);
         GL11.glTranslatef(0.0F, -0.75F, 0.0F);
         GL11.glRotatef(26.0F + (stalkerPhase == PHASE_CHASE ? 20.0F : 0.0F), 1.0F, 0.0F, 0.0F);
         stalkerBox(-0.045F, -0.82F, -0.045F, 0.045F, 0.02F, 0.045F, BoneDark, bright);

         for (int c = -1; c <= 1; c++) {
            stalkerBox(c * 0.035F - 0.012F, -1.08F, -0.02F, c * 0.035F + 0.012F, -0.80F, 0.04F, ClawCrimson, 0.95F);
         }
         GL11.glPopMatrix();
      }

      // 5. Twitching Crooked Skull, Unhinging Jaw & Glowing Eyes
      GL11.glPushMatrix();
      GL11.glTranslatef(0.0F, 1.15F, 0.04F);
      stalkerBox(-0.06F, -0.02F, -0.06F, 0.06F, 0.24F, 0.06F, BoneDark, bright);
      GL11.glTranslatef(0.0F, 0.22F, 0.02F);

      GL11.glRotatef(headYaw, 0.0F, 1.0F, 0.0F);
      GL11.glRotatef(headPitch, 1.0F, 0.0F, 0.0F);

      float headTilt;
      if (stalkerPhase == PHASE_FROZEN) {
         headTilt = 36.0F + twitch * 3.2F;
      } else if (stalkerPhase == PHASE_CHASE) {
         headTilt = (float)Math.sin(time * 1.7F) * 26.0F + twitch * 2.5F;
      } else {
         headTilt = 14.0F * (float)Math.sin(time * 0.09F);
      }
      GL11.glRotatef(headTilt, 0.0F, 0.0F, 1.0F);

      stalkerBox(-0.21F, 0.04F, -0.22F, 0.21F, 0.52F, 0.22F, VoidBlack, bright);
      stalkerBox(-0.23F, 0.26F, 0.16F, 0.23F, 0.35F, 0.25F, BoneDark, bright);
      stalkerBox(-0.18F, 0.50F, -0.08F, -0.11F, 0.78F, -0.01F, VoidBlack, bright);
      stalkerBox(0.11F, 0.50F, -0.08F, 0.18F, 0.84F, -0.01F, VoidBlack, bright);

      for (int t = -3; t <= 3; t += 2) {
         stalkerBox(t * 0.045F - 0.018F, -0.03F, 0.17F, t * 0.045F + 0.018F, 0.05F, 0.21F, ToothPale, 0.95F);
      }

      float jawDrop = stalkerPhase == PHASE_CHASE
         ? 0.26F + 0.06F * (float)Math.sin(time * 1.4F)
         : (stalkerPhase == PHASE_FROZEN ? 0.18F : 0.04F);
      GL11.glPushMatrix();
      GL11.glTranslatef(0.0F, 0.04F - jawDrop, 0.0F);
      GL11.glRotatef(jawDrop * 85.0F, 1.0F, 0.0F, 0.0F);
      stalkerBox(-0.19F, -0.14F, -0.18F, 0.19F, 0.0F, 0.23F, BoneDark, bright);
      for (int t = -3; t <= 3; t += 2) {
         stalkerBox(t * 0.045F - 0.018F, 0.0F, 0.17F, t * 0.045F + 0.018F, 0.075F, 0.21F, ToothPale, 0.95F);
      }
      GL11.glPopMatrix();

      // 6. Emissive Glowing Eyes (pierces fog & darkness)
      boolean fogWasOn = GL11.glIsEnabled(GL11.GL_FOG);
      GL11.glDisable(GL11.GL_FOG);

      int EyeCrimson = 0xFF1A1E;
      int EyeCoreWhite = 0xFFF8EC;
      stalkerBox(-0.155F, 0.20F, 0.215F, -0.055F, 0.275F, 0.245F, EyeCrimson, 1.0F);
      stalkerBox(0.055F, 0.20F, 0.215F, 0.155F, 0.275F, 0.245F, EyeCrimson, 1.0F);
      stalkerBox(-0.118F, 0.222F, 0.232F, -0.088F, 0.252F, 0.256F, EyeCoreWhite, 1.0F);
      stalkerBox(0.088F, 0.222F, 0.232F, 0.118F, 0.252F, 0.256F, EyeCoreWhite, 1.0F);

      if (jawDrop > 0.08F) {
         stalkerBox(-0.14F, 0.04F - jawDrop * 0.7F, 0.12F, 0.14F, 0.06F, 0.19F, 0xD80E16, 1.0F);
      }

      if (fogWasOn) {
         GL11.glEnable(GL11.GL_FOG);
      }

      GL11.glPopMatrix();
      GL11.glPopMatrix();
      GL11.glPopMatrix();

      if (texWasOn) {
         GL11.glEnable(GL11.GL_TEXTURE_2D);
      }
   }

   // ====================================================================================================
   // FOUND-FOOTAGE VHS CAMCORDER HUD OVERLAY
   // ====================================================================================================

   public static void drawHudOverlay(Game g) {
      if (!active || g == null || g.gui == null || g.debug) {
         return;
      }
      Gui gui = g.gui;
      float w = gui.w;
      float h = gui.h;

      // 1. VHS Static Interference Bars & Crimson Flash
      if (panicLevel > 0.22F || jumpscareFlash > 0.02F) {
         int barCount = (int)(1 + panicLevel * 5.0F);
         for (int i = 0; i < barCount; i++) {
            float by = (float)((g.tickCount * 19 + i * 73) % (int)Math.max(1.0F, h));
            float bh = 2.0F + (i % 3) * 2.0F;
            int alpha = Math.min(120, (int)(panicLevel * 80.0F));
            int col = (alpha << 24) | 0x00E8DFB0;
            gui.rect(0.0F, by, w, bh, col);
         }
         if (jumpscareFlash > 0.02F) {
            int flashA = Math.min(150, (int)(jumpscareFlash * 150.0F));
            gui.rect(0.0F, 0.0F, w, h, (flashA << 24) | 0x008A080C);
         }
      }

      // 2. Camcorder Viewfinder Frame Brackets
      int frameCol = 0x88E6DFB8;
      float m = 10.0F;
      float len = 18.0F;
      gui.rect(m, m, len, 1.5F, frameCol);
      gui.rect(m, m, 1.5F, len, frameCol);
      gui.rect(w - m - len, m, len, 1.5F, frameCol);
      gui.rect(w - m - 1.5F, m, 1.5F, len, frameCol);
      gui.rect(m, h - m - 1.5F, len, 1.5F, frameCol);
      gui.rect(m, h - m - len, 1.5F, len, frameCol);
      gui.rect(w - m - len, h - m - 1.5F, len, 1.5F, frameCol);
      gui.rect(w - m - 1.5F, h - m - len, 1.5F, len, frameCol);

      // 3. Top-Left Tape Header & Current Separate Level Name
      int lvl = instance != null ? instance.currentLevel : BackroomsGen.LEVEL_0;
      String levelTitle = getLevelTitle(lvl);
      boolean recBlink = ((g.tickCount / 12) & 1) == 0;
      String recStr = (recBlink ? "§c● REC " : "§8○ REC ") + "§e" + levelTitle;
      gui.text(recStr, 16.0F, 15.0F, 0xFFFFFFFF, true, 1.0F);

      // 4. Sanity, Heart Rate, Exit Beacon Distance & Stealth Hiding Readout
      int[] exitXZ = BackroomsGen.nearestExitXZ(g.player != null ? g.player.x : 0.0, g.player != null ? g.player.z : 0.0);
      int exitDist = g.player != null ? (int)Math.round(Math.hypot(exitXZ[0] - g.player.x, exitXZ[1] - g.player.z)) : 24;
      String sanCol = sanity > 60 ? "§a" : (sanity > 30 ? "§e" : "§c");
      String bpmCol = heartbeatBpm > 135 ? "§c" : (heartbeatBpm > 100 ? "§e" : "§f");
      String stealthTag = playerCurrentlyHidden ? "  §8|  §b[HIDING — UNDETECTABLE]" : ("  §8|  §aEXIT: §f" + exitDist + "m");
      String status = "§7SANITY: " + sanCol + sanity + "%  §8|  §7BPM: " + bpmCol + heartbeatBpm + " ♥" + stealthTag;
      gui.text(status, 16.0F, 27.0F, 0xFFFFFFFF, true, 1.0F);

      // 5. Entity Proximity & Hiding Alert
      if (playerCurrentlyHidden && stalkerMob != null && !stalkerMob.dead) {
         gui.centered("§b[HIDDEN IN COVER — STALKER CANNOT SEE YOU]", w / 2.0F, 18.0F, 0xFFFFFFFF, true, 1.0F);
      } else if (stalkerMob != null && !stalkerMob.dead) {
         String entityAlert = null;
         if (stalkerPhase == PHASE_CHASE) {
            entityAlert = "§4§l! ENTITY HUNTING (" + (int)lastDistToPlayer + "m) — DUCK INTO A LOCKER OR UNDER A DESK !";
         } else if (stalkerPhase == PHASE_FROZEN && stareTicks > 12) {
            entityAlert = "§c! VISUAL CONTACT — LOOK AWAY OR HIDE !";
         } else if (blackoutTicks > 0) {
            entityAlert = "§8[POWER FAILURE — FLUORESCENT GRID OFFLINE]";
         } else if (lastDistToPlayer < 15.0F) {
            entityAlert = "§6[FOOTSTEPS NEARBY: " + (int)lastDistToPlayer + "m]";
         }
         if (entityAlert != null) {
            gui.centered(entityAlert, w / 2.0F, 18.0F, 0xFFFFFFFF, true, 1.0F);
         }
      }

      // 6. Top-Right VHS Tape Timecode
      int totalSec = g.tickCount / 20;
      int mm = (totalSec / 60) % 60;
      int ss = totalSec % 60;
      int ff = (g.tickCount % 20) * 3;
      String tc = String.format("§7TAPE 1996-08  %02d:%02d:%02d", mm, ss, ff);
      gui.text(tc, w - Gui.textWidth(Gui.stripCodes(tc)) - 18.0F, 15.0F, 0xFFFFFFFF, true, 1.0F);
   }
}
