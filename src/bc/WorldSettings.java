package bc;

import java.util.Properties;

/**
 * Update 3.13: what you choose on the Create New World screen. It is stored in level.properties (gentype, structures, bonus, flat), so a
 * world is always opened with the generator it was made with: worlds from before 3.13 have no "gentype" and keep the classic generator
 * (their terrain must not change under the chunks that are already saved).
 */
public final class WorldSettings {
   public static final int CLASSIC = 0;
   public static final int DEFAULT = 1;
   public static final int FLAT = 2;
   public static final int LARGE = 3;
   public static final int AMPLIFIED = 4;
   public static final int SKYBLOCK = 5;
   public static final int SKYWARS = 6;
   public static final int BEDWARS = 7;
   public static final int TYPES = 8;
   /** The order of the "World Type" button (the new default first, the classic generator last). */
   public static final int[] TYPE_ORDER = new int[]{DEFAULT, LARGE, AMPLIFIED, FLAT, SKYBLOCK, SKYWARS, BEDWARS, CLASSIC};
   public static final String[] TYPE_NAME = new String[]{
      "Classic (old)", "Default", "Superflat", "Large Biomes", "Amplified", "Skyblock", "Skywars", "Bedwars"
   };
   public static final String[] TYPE_INFO = new String[]{
      "The terrain of Update 3.5 - 3.12",
      "Oceans, rivers, 28 biomes, canyons and mountains",
      "A flat world with nothing in it",
      "The same biomes, four times as big",
      "Towering mountains and deep valleys",
      "Survive on a floating island in the void with a tree, ice and lava",
      "Eight spawn islands around a rich central feast island in the sky",
      "Four team bases with beds and resource generators around an emerald arena"
   };
   public static final int FLAT_PRESETS = 4;
   public static final String[] FLAT_NAME = new String[]{"Grassland", "Desert", "Snowy", "Sandstone"};

   public int type = DEFAULT;
   public boolean structures = true;
   public boolean bonusChest = false;
   public int flat = 0;
   /**
    * Update 3.14: 1 = the villages and structures of 3.13 (worlds made before 3.14 keep them: what is built must not move), 2 = the new ones (streets,
    * buildings in the style of the biome, temples, igloos, ships ...). A world without the key in level.properties has 1; new worlds get 2.
    */
   public int structVer = 2;

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
      return s;
   }

   /** The generator that builds terrain with biomes, rivers and so on ({@link TerraGen}). */
   public boolean terra() {
      return this.type == DEFAULT || this.type == LARGE || this.type == AMPLIFIED;
   }

   public String typeName() {
      return TYPE_NAME[Math.max(0, Math.min(TYPES - 1, this.type))];
   }

   public void save(Properties p) {
      p.setProperty("gentype", Integer.toString(this.type));
      p.setProperty("structures", Boolean.toString(this.structures));
      p.setProperty("bonus", Boolean.toString(this.bonusChest));
      p.setProperty("flat", Integer.toString(this.flat));
      p.setProperty("structver", Integer.toString(this.structVer));
   }

   public static WorldSettings load(Properties p) {
      if (p == null || p.getProperty("gentype") == null) {
         return classic();
      }

      WorldSettings s = new WorldSettings();
      try {
         s.type = Math.max(0, Math.min(TYPES - 1, Integer.parseInt(p.getProperty("gentype").trim())));
         s.flat = Math.max(0, Math.min(FLAT_PRESETS - 1, Integer.parseInt(p.getProperty("flat", "0").trim())));
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
      return this.type == SKYBLOCK || this.type == SKYWARS || this.type == BEDWARS;
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
         case "skyblock":
         case "sb":
            return SKYBLOCK;
         case "skywars":
         case "sw":
            return SKYWARS;
         case "bedwars":
         case "bw":
            return BEDWARS;
         default:
            return DEFAULT;
      }
   }
}
