package bc;

import java.util.Properties;

/**
 * Update 3.13 / V4.2: World generation & Console Edition Mini Game settings.
 * Standard world types (Default, Large Biomes, Amplified, Superflat, Classic) are selected on Create New World,
 * while Console Edition Mini Games (Battle, Tumble, Bedwars) and their multiple maps (Cove, Crucible, Cavern, Temple)
 * have a dedicated Mini Games menu.
 */
public final class WorldSettings {
   public static final int CLASSIC = 0;
   public static final int DEFAULT = 1;
   public static final int FLAT = 2;
   public static final int LARGE = 3;
   public static final int AMPLIFIED = 4;
   /** Console Edition Tumble Mini Game (3-layer shovel & snowball Spleef over boiling lava). */
   public static final int TUMBLE = 5;
   /** Legacy alias for TUMBLE (slot 5). */
   public static final int SKYBLOCK = 5;
   /** Console Edition Battle Mini Game (enclosed arena last-man-standing with central loot chests, refills & showdown). */
   public static final int BATTLE = 6;
   /** Legacy alias for BATTLE (slot 6). */
   public static final int SKYWARS = 6;
   /** 4-Team Bedwars Mini Game. */
   public static final int BEDWARS = 7;
   public static final int TYPES = 8;

   /** Standard world types cycled by the "Type:" button on Create New World (keeps Mini Games in their own dedicated menu). */
   public static final int[] TYPE_ORDER = new int[]{DEFAULT, LARGE, AMPLIFIED, FLAT, CLASSIC};
   /** Console Edition Mini Game modes shown on the dedicated Mini Games menu. */
   public static final int[] MINIGAME_ORDER = new int[]{BATTLE, TUMBLE, BEDWARS};

   public static final String[] TYPE_NAME = new String[]{
      "Classic (old)", "Default", "Superflat", "Large Biomes", "Amplified", "Tumble", "Battle", "Bedwars"
   };
   public static final String[] TYPE_INFO = new String[]{
      "The terrain of Update 3.5 - 3.12",
      "Oceans, rivers, 28 biomes, canyons and mountains",
      "A flat world with nothing in it",
      "The same biomes, four times as big",
      "Towering mountains and deep valleys",
      "Console Edition Tumble: destroy 3 layers of blocks over lava with shovels and snowballs!",
      "Console Edition Battle: arena last-man-standing with central chests, grace period, refills and showdown!",
      "Four team bases with beds, shopkeepers and resource generators around an emerald arena!"
   };

   // Console Edition Mini Game Maps ("like the real deal")
   public static final int MAP_COVE = 0;
   public static final int MAP_CRUCIBLE = 1;
   public static final int MAP_CAVERN = 2;
   public static final int MAP_TEMPLE = 3;
   public static final int MAP_COUNT = 4;
   public static final String[] MAP_NAMES = new String[]{"Cove", "Crucible", "Cavern", "Temple"};
   public static final String[] MAP_THEMES = new String[]{
      "Pirate Shipwreck Cove",
      "Nether Lava Fortress",
      "Underground Mushroom Cavern",
      "Ancient Jungle Temple"
   };
   public static final String[] MAP_INFO = new String[]{
      "Try to survive in this pirate's hidden camp, full of shipwrecks, skull caves and waterfalls!",
      "A world built deep in the Nether, with lava falls and obsidian bridges for your enemies to swim in!",
      "This ancient underground cavern with giant mushrooms and stalactites is perfect to ambush enemies!",
      "This mysterious stepped temple is set within the deepest jungle. Watch out for the treetop bridges!"
   };

   public static final int FLAT_PRESETS = 4;
   public static final String[] FLAT_NAME = new String[]{"Grassland", "Desert", "Snowy", "Sandstone"};

   public int type = DEFAULT;
   public boolean structures = true;
   public boolean bonusChest = false;
   public int flat = 0;
   public int structVer = 2;
   /** Selected Console Edition Arena Map (0 = Cove, 1 = Crucible, 2 = Cavern, 3 = Temple). */
   public int mapId = MAP_COVE;

   public static WorldSettings classic() {
      WorldSettings s = new WorldSettings();
      s.type = CLASSIC;
      s.structVer = 1;
      return s;
   }

   public WorldSettings copy() {
      WorldSettings s = new WorldSettings();
      s.type = this.type;
      s.structures = this.structures;
      s.bonusChest = this.bonusChest;
      s.flat = this.flat;
      s.structVer = this.structVer;
      s.mapId = this.mapId;
      return s;
   }

   /** The generator that builds terrain with biomes, rivers and so on ({@link TerraGen}). */
   public boolean terra() {
      return this.type == DEFAULT || this.type == LARGE || this.type == AMPLIFIED;
   }

   public String typeName() {
      return TYPE_NAME[Math.max(0, Math.min(TYPES - 1, this.type))];
   }

   public String mapName() {
      return MAP_NAMES[Math.max(0, Math.min(MAP_COUNT - 1, this.mapId))];
   }

   public String mapTheme() {
      return MAP_THEMES[Math.max(0, Math.min(MAP_COUNT - 1, this.mapId))];
   }

   public void nextMap() {
      this.mapId = (this.mapId + 1) % MAP_COUNT;
   }

   public String mapInfo() {
      return MAP_INFO[Math.max(0, Math.min(MAP_COUNT - 1, this.mapId))];
   }

   public void save(Properties p) {
      p.setProperty("gentype", Integer.toString(this.type));
      p.setProperty("structures", Boolean.toString(this.structures));
      p.setProperty("bonus", Boolean.toString(this.bonusChest));
      p.setProperty("flat", Integer.toString(this.flat));
      p.setProperty("structver", Integer.toString(this.structVer));
      p.setProperty("mapid", Integer.toString(this.mapId));
   }

   public static WorldSettings load(Properties p) {
      if (p == null || p.getProperty("gentype") == null) {
         return classic();
      }

      WorldSettings s = new WorldSettings();
      try {
         s.type = Math.max(0, Math.min(TYPES - 1, Integer.parseInt(p.getProperty("gentype").trim())));
         s.flat = Math.max(0, Math.min(FLAT_PRESETS - 1, Integer.parseInt(p.getProperty("flat", "0").trim())));
         s.mapId = Math.max(0, Math.min(MAP_COUNT - 1, Integer.parseInt(p.getProperty("mapid", "0").trim())));
      } catch (NumberFormatException e) {
         return classic();
      }

      s.structVer = 1;
      try {
         s.structVer = Math.max(1, Math.min(2, Integer.parseInt(p.getProperty("structver", "1").trim())));
      } catch (NumberFormatException e) {
         s.structVer = 1;
      }

      s.structures = Boolean.parseBoolean(p.getProperty("structures", "true"));
      s.bonusChest = Boolean.parseBoolean(p.getProperty("bonus", "false"));
      return s;
   }

   /** Index of the type in {@link #TYPE_ORDER}. */
   public int typeIndex() {
      for (int i = 0; i < TYPE_ORDER.length; i++) {
         if (TYPE_ORDER[i] == this.type) {
            return i;
         }
      }

      return 0;
   }

   public boolean isVoidMode() {
      return this.type == TUMBLE || this.type == BATTLE || this.type == BEDWARS;
   }

   public boolean isMinigame() {
      return this.type == TUMBLE || this.type == BATTLE || this.type == BEDWARS;
   }

   public static int parseType(String s) {
      if (s == null || s.isBlank()) {
         return DEFAULT;
      }
      String q = s.trim().toLowerCase();
      try {
         int v = Integer.parseInt(q);
         if (v >= 0 && v < TYPES) {
            return v;
         }
      } catch (NumberFormatException ignored) {
      }
      switch (q) {
         case "classic":
         case "old":
         case "infdev":
            return CLASSIC;
         case "flat":
         case "superflat":
            return FLAT;
         case "large":
         case "large_biomes":
         case "largebiomes":
            return LARGE;
         case "amplified":
         case "amp":
            return AMPLIFIED;
         case "tumble":
         case "spleef":
         case "skyblock":
         case "sb":
            return TUMBLE;
         case "battle":
         case "skywars":
         case "sw":
            return BATTLE;
         case "bedwars":
         case "bw":
            return BEDWARS;
         default:
            return DEFAULT;
      }
   }
}
