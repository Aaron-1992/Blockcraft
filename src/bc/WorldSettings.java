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
   /** CS2 Tactical Strike Mini Game (Counter-Terrorists vs Terrorists with pistols, machine guns, snipers, TNT grenades & C4 bomb sites). */
   public static final int CS2 = 8;
   /** Murder Mystery Mini Game (1 Murderer with concealed Knife, 1 Sheriff with Pistol, and Innocents scavenging Gold Ingots). */
   public static final int MURDER = 9;
   /** Bed-Bombing / Bed-Spam End Fight Mini Game (eliminate opponents in the End by rapidly placing and exploding beds). */
   public static final int END_FIGHT = 10;
   /** Castle Siege / Conquest with Cannons Mini Game (Attackers vs Defenders fortress breach & control zone capture with cannons, mortars & TNT). */
   public static final int CASTLE_SIEGE = 11;
   /** All Blocks & Items Texture Showcase Gallery (walkable museum with every block, plant, crop, door, bed, banner, redstone part & item chests). */
   public static final int ALL_BLOCKS = 12;
   public static final int TYPES = 13;

   /** Standard world types cycled by the "Type:" button on Create New World (keeps Mini Games in their own dedicated menu). */
   public static final int[] TYPE_ORDER = new int[]{DEFAULT, LARGE, AMPLIFIED, FLAT, ALL_BLOCKS, CLASSIC};
   /** Console Edition & Tactical Mini Game modes shown on the dedicated Mini Games menu. */
   public static final int[] MINIGAME_ORDER = new int[]{BATTLE, TUMBLE, BEDWARS, CS2, MURDER, END_FIGHT, CASTLE_SIEGE};

   public static final String[] TYPE_NAME = new String[]{
      "Classic (old)", "Default", "Superflat", "Large Biomes", "Amplified", "Tumble", "Battle", "Bedwars", "CS2 Strike", "Murder Mystery", "End Fight", "Castle Siege", "All Blocks Gallery"
   };
   public static final String[] TYPE_INFO = new String[]{
      "The terrain of Update 3.5 - 3.12",
      "Oceans, rivers, 28 biomes, canyons and mountains",
      "A flat world with nothing in it",
      "The same biomes, four times as big",
      "Towering mountains and deep valleys",
      "Console Edition Tumble: destroy 3 layers of blocks over lava with shovels and snowballs!",
      "Console Edition Battle: arena last-man-standing with central chests, grace period, refills and showdown!",
      "Four team bases with beds, shopkeepers and resource generators around an emerald arena!",
      "CS2 Tactical Strike: CT vs T combat with Machine Guns, Pistols, Snipers, TNT Grenades, Map Bomb Sites & No-Bombs Mode!",
      "Murder Mystery: 1 Murderer with a concealed Knife, 1 Sheriff with a Pistol, and Innocents scavenging 5 Gold Ingots to arm themselves!",
      "Bed-Spam End Fight: rapidly place and detonate beds in the End! Zero self-damage/knockback from your own beds; blast foes into the void!",
      "Castle Siege Conquest: Attackers vs Defenders! Fire Siege Cannons, Mortars & TNT to breach castle walls and capture the inner Control Zones!",
      "All Blocks & Items Gallery: every single block, plant, crop, door, bed, banner, redstone part & chest of all items laid out to inspect!"
   };

   // Console Edition Mini Game Maps ("like the real deal") — all 8 ported Console Edition maps
   public static final int MAP_COVE = 0;
   public static final int MAP_CRUCIBLE = 1;
   public static final int MAP_CAVERN = 2;
   public static final int MAP_TEMPLE = 3;
   public static final int MAP_LAIR = 4;
   public static final int MAP_CASTLE = 5;
   public static final int MAP_FRONTIER = 6;
   public static final int MAP_SHRUNK = 7;
   public static final int MAP_COUNT = 8;
   public static final String[] MAP_NAMES = new String[]{
      "Cove",
      "Crucible",
      "Cavern",
      "Temple",
      "Lair",
      "Castle",
      "Frontier",
      "Shrunk"
   };
   public static final String[] MAP_THEMES = new String[]{
      "Pirate Shipwreck Cove",
      "Nether Lava Fortress",
      "Underground Mushroom Cavern",
      "Ancient Jungle Temple",
      "Dragon Stronghold Lair",
      "Medieval Royal Castle",
      "Wild West Canyon Frontier",
      "Giant Bedroom Shrunk Arena"
   };
   public static final String[] MAP_INFO = new String[]{
      "Try to survive in this pirate's hidden camp, full of shipwrecks, skull caves and waterfalls!",
      "A world built deep in the Nether, with lava falls and obsidian bridges for your enemies to swim in!",
      "This ancient underground cavern with giant mushrooms and stalactites is perfect to ambush enemies!",
      "This mysterious stepped temple is set within the deepest jungle. Watch out for the treetop bridges!",
      "A cavernous subterranean Dragon Stronghold with two-story libraries, prison dungeons and high vaults!",
      "Battle across the battlements, courtyard towers, throne hall and wine cellars of a medieval royal keep!",
      "High noon in a badlands canyon town featuring a two-story saloon, sheriff's jail, bank vault and mine!",
      "You have been shrunk down inside a giant's bedroom! Climb towering bookshelves, desks and mouse tunnels!"
   };
   public static final String[] SHORT_MAP_NAMES = new String[]{
      "Cove", "Cruc", "Cavn", "Tmpl", "Lair", "Cstl", "Frnt", "Shrk"
   };

   // 8 Dedicated CS2 Tactical Strike Maps (3-Lane Defusal & Team Elimination Arenas)
   public static final String[] CS2_MAP_NAMES = new String[]{
      "Dust II",
      "Mirage",
      "Inferno",
      "Nuke",
      "Overpass",
      "Train",
      "Vertigo",
      "Anubis"
   };
   public static final String[] CS2_MAP_THEMES = new String[]{
      "Desert Bazaar & Long A",
      "Moroccan Palace & Market",
      "Italian Cobble Village",
      "Twin-Level Nuclear Plant",
      "Canal Bridge & Monster",
      "Armored Freight Railyard",
      "51st-Floor Skyscraper",
      "Pharaoh Canal Temple"
   };
   public static final String[] CS2_MAP_INFO = new String[]{
      "Iconic desert bazaar with Long A Double Doors, Catwalk Short A, Mid Doors, Upper/Lower B Tunnels, A Plateau and B Car Site!",
      "Sun-baked Moroccan courtyard featuring elevated Palace interior, A Ramp, Mid Window & Underpass, B Apartments and Market!",
      "Cobblestone village with Banana alley, Fountain B-Site, Second Mid, Apartments balcony, Archway and Pit A-Site!",
      "Multi-level nuclear facility with Upper Reactor Silo A-Site, Lower Decon B-Site, Vent drop, Outside Yard and Secret Stairs!",
      "Elevated highway bridge and canal park featuring Monster Tunnel, Water Canal, Fountain Park, Bank A-Site and Pillar B-Site!",
      "Industrial railyard packed with 6 armored freight trains, Ivy Lane, Popdog ladder drop, Main Yard A-Site and Inner Yard B-Site!",
      "High-altitude skyscraper construction deck suspended over the void with Crane A-Ramp, Steel Girder Mid, Elevator and B-Stairs!",
      "Ancient Egyptian sandstone temple complex split by a playable Water Canal Bridge between Obelisk A-Plaza and Crypt B-Site!"
   };
   public static final String[] CS2_SHORT_MAPS = new String[]{
      "Dust", "Mirg", "Infr", "Nuke", "Ovps", "Trn", "Vert", "Anub"
   };

   // 8 Dedicated Castle Siege / Conquest Fortress Maps (Attackers vs Defenders with Cannons, Mortars & Destructible Gates)
   public static final String[] SIEGE_MAP_NAMES = new String[]{
      "Ironkeep",
      "Helm's Deep",
      "Dragonstone",
      "Frosthold",
      "Sandstorm",
      "Blackrock",
      "Skyhaven",
      "Dreadfort"
   };
   public static final String[] SIEGE_MAP_THEMES = new String[]{
      "Royal Stone Citadel",
      "Hornburg Deeping Wall",
      "Volcanic Obsidian Keep",
      "Nordic Glacier Bastion",
      "Sultanate Desert Citadel",
      "Crimson Nether Bastion",
      "Cloud Marble Sanctuary",
      "Gothic Vampire Fortress"
   };
   public static final String[] SIEGE_MAP_INFO = new String[]{
      "Classic medieval stone citadel with water moat drawbridges, 3 iron portcullis gates, cannon batteries, Courtyard A and Throne Keep B!",
      "Massive curved Deeping Wall with the destructible Culvert Drain breach, raised Hornburg causeway, Inner Bailey A and Mountain Keep B!",
      "Volcanic black obsidian fortress over a boiling lava moat with basalt causeways, dragon gates, Blood Courtyard A and Dragon Altar B!",
      "Frozen packed-ice & spruce Nordic bastion with ice-trench bridges, timber-and-stone gates, Mead Hall Yard A and Jarl's Throne B!",
      "Golden-domed desert Sultanate fortress with canyon trench, arched sandstone gates, Oasis Colonnade A and Palace Sanctum B!",
      "Nether-brick & iron war bastion guarded by a fiery magma trench, reinforced iron gates, War Forge Yard A and Overlord Sanctum B!",
      "High-mountain quartz & prismarine sanctuary above a cloud chasm with 3 grand sky-bridges, Lotus Courtyard A and Celestial Sanctum B!",
      "Dark oak & mossy cobblestone gothic castle with graveyard siege approach, catacomb gates, Gallows Yard A and Crimson Crypt B!"
   };
   public static final String[] SIEGE_SHORT_MAPS = new String[]{
      "Iron", "Helm", "Drag", "Frst", "Sand", "Blck", "Skyh", "Dred"
   };

   public static String mapNameFor(int type, int mapId) {
      int idx = Math.floorMod(mapId, MAP_COUNT);
      if (type == CS2) {
         return CS2_MAP_NAMES[idx];
      }
      if (type == CASTLE_SIEGE) {
         return SIEGE_MAP_NAMES[idx];
      }
      return MAP_NAMES[idx];
   }

   public static String mapThemeFor(int type, int mapId) {
      int idx = Math.floorMod(mapId, MAP_COUNT);
      if (type == CS2) {
         return CS2_MAP_THEMES[idx];
      }
      if (type == CASTLE_SIEGE) {
         return SIEGE_MAP_THEMES[idx];
      }
      return MAP_THEMES[idx];
   }

   public static String mapInfoFor(int type, int mapId) {
      int idx = Math.floorMod(mapId, MAP_COUNT);
      if (type == CS2) {
         return CS2_MAP_INFO[idx];
      }
      if (type == CASTLE_SIEGE) {
         return SIEGE_MAP_INFO[idx];
      }
      return MAP_INFO[idx];
   }

   public static String shortMapNameFor(int type, int mapId) {
      int idx = Math.floorMod(mapId, MAP_COUNT);
      if (type == CS2) {
         return CS2_SHORT_MAPS[idx];
      }
      if (type == CASTLE_SIEGE) {
         return SIEGE_SHORT_MAPS[idx];
      }
      return SHORT_MAP_NAMES[idx];
   }

   public static final int FLAT_PRESETS = 4;
   public static final String[] FLAT_NAME = new String[]{"Grassland", "Desert", "Snowy", "Sandstone"};

   public boolean aquatic = true; // new worlds; old saves retain their generator
   public int type = DEFAULT;
   public boolean structures = true;
   public boolean bonusChest = false;
   public int flat = 0;
   public int structVer = 2;
   /** Selected Console Edition Arena Map (0 = Cove, 1 = Crucible, 2 = Cavern, 3 = Temple). */
   public int mapId = MAP_COVE;

   /** 1 = V6.18 terrain, 2 = V6.19 bigger caves / mountains, 3 = V6.21 layered caves + underground structures. Old saves keep their own version. */
   public int terrainVer = 1;
   /** Update 6.19: only used when terrainVer >= 2. */
   public float caveScale = 1.0F, caveDensity = 1.0F, mountainScale = 1.0F, ravineScale = 1.0F;
   /** Update 6.21: only used when terrainVer >= 3. All four are copied into new worlds from Options. */
   public boolean caveStructures = true;
   public boolean caveLayers = true;
   public boolean caveLakes = true;
   public float caveRichness = 1.0F;
   /** Update 6.29: 1 = V6.21 layered caves, 2 = Caves &amp; Cliffs carver, 3 = V6.28 sloped mouths,
    * 4 = V6.29 walk-in cave entrances, 5 = V6.31 natural surface-breaking caves.
    * Old saves keep the version they were created with. */
   public int caveVer = 1;
   /** Update 6.22: 0 = sharp legacy biome borders, 1 = smooth, 2 = very smooth. */
   public int biomeBorders = 0;
   /** Update 6.22: global tree-density multiplier (0.4..1.4); lower is much cheaper in taiga. */
   public float treeDensity = 1.0F;
   /** Update 6.23: height/radius multiplier of every tree (0.7..1.5). */
   public float treeScale = 1.0F;
   /** Update 6.23: allow the rare Giant size tier in new worlds. */
   public boolean giantTrees = true;

   public static WorldSettings classic() {
      WorldSettings s = new WorldSettings();
      s.type = CLASSIC;
      s.aquatic = false;
      s.structVer = 1;
      return s;
   }

   public WorldSettings copy() {
      WorldSettings s = new WorldSettings();
      s.type = this.type;
      s.aquatic = this.aquatic;
      s.structures = this.structures;
      s.bonusChest = this.bonusChest;
      s.flat = this.flat;
      s.structVer = this.structVer;
      s.mapId = this.mapId;
      s.terrainVer = this.terrainVer;
      s.caveScale = this.caveScale;
      s.caveDensity = this.caveDensity;
      s.mountainScale = this.mountainScale;
      s.ravineScale = this.ravineScale;
      s.caveStructures = this.caveStructures;
      s.caveLayers = this.caveLayers;
      s.caveLakes = this.caveLakes;
      s.caveRichness = this.caveRichness;
      s.caveVer = this.caveVer;
      s.biomeBorders = this.biomeBorders;
      s.treeDensity = this.treeDensity;
      s.treeScale = this.treeScale;
      s.giantTrees = this.giantTrees;
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
      return mapNameFor(this.type, this.mapId);
   }

   public String mapTheme() {
      return mapThemeFor(this.type, this.mapId);
   }

   public void nextMap() {
      this.mapId = (this.mapId + 1) % MAP_COUNT;
   }

   public String mapInfo() {
      return MAP_INFO[Math.max(0, Math.min(MAP_COUNT - 1, this.mapId))];
   }

   public void save(Properties p) {
      p.setProperty("aquatic", Boolean.toString(this.aquatic));
      p.setProperty("gentype", Integer.toString(this.type));
      p.setProperty("structures", Boolean.toString(this.structures));
      p.setProperty("bonus", Boolean.toString(this.bonusChest));
      p.setProperty("flat", Integer.toString(this.flat));
      p.setProperty("structver", Integer.toString(this.structVer));
      p.setProperty("mapid", Integer.toString(this.mapId));
      p.setProperty("terrainver", Integer.toString(this.terrainVer));
      p.setProperty("cavescale", Float.toString(this.caveScale));
      p.setProperty("cavedensity", Float.toString(this.caveDensity));
      p.setProperty("mountainscale", Float.toString(this.mountainScale));
      p.setProperty("ravinescale", Float.toString(this.ravineScale));
      p.setProperty("cavestructures", Boolean.toString(this.caveStructures));
      p.setProperty("cavelayers", Boolean.toString(this.caveLayers));
      p.setProperty("cavelakes", Boolean.toString(this.caveLakes));
      p.setProperty("caverichness", Float.toString(this.caveRichness));
      p.setProperty("cavever", Integer.toString(this.caveVer));
      p.setProperty("biomeborders", Integer.toString(this.biomeBorders));
      p.setProperty("treedensity", Float.toString(this.treeDensity));
      p.setProperty("treescale", Float.toString(this.treeScale));
      p.setProperty("gianttrees", Boolean.toString(this.giantTrees));
   }

   private static float f(Properties p, String key, float def, float lo, float hi) {
      float v = Float.parseFloat(p.getProperty(key, Float.toString(def)));
      return v < lo ? lo : v > hi ? hi : v;
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

      s.aquatic = Boolean.parseBoolean(p.getProperty("aquatic", "false"));
      s.structVer = 1;
      try {
         s.structVer = Math.max(1, Math.min(2, Integer.parseInt(p.getProperty("structver", "1").trim())));
      } catch (NumberFormatException e) {
         s.structVer = 1;
      }

      s.structures = Boolean.parseBoolean(p.getProperty("structures", "true"));
      s.bonusChest = Boolean.parseBoolean(p.getProperty("bonus", "false"));
      try {
         s.terrainVer = Math.max(1, Math.min(3, Integer.parseInt(p.getProperty("terrainver", "1").trim())));
         s.caveScale = f(p, "cavescale", 1.0F, 0.6F, 3.0F);
         s.caveDensity = f(p, "cavedensity", 1.0F, 0.4F, 2.5F);
         s.mountainScale = f(p, "mountainscale", 1.0F, 0.5F, 2.5F);
         s.ravineScale = f(p, "ravinescale", 1.0F, 0.4F, 2.5F);
         s.caveRichness = f(p, "caverichness", 1.0F, 0.4F, 2.0F);
      } catch (NumberFormatException e) {
         s.terrainVer = 1;
         s.caveScale = s.caveDensity = s.mountainScale = s.ravineScale = s.caveRichness = 1.0F;
      }
      s.caveStructures = Boolean.parseBoolean(p.getProperty("cavestructures", "true"));
      s.caveLayers = Boolean.parseBoolean(p.getProperty("cavelayers", "true"));
      s.caveLakes = Boolean.parseBoolean(p.getProperty("cavelakes", "true"));
      try {
         s.caveVer = Math.max(1, Math.min(5, Integer.parseInt(p.getProperty("cavever", "1").trim())));
         s.biomeBorders = Math.max(0, Math.min(2, Integer.parseInt(p.getProperty("biomeborders", "0").trim())));
         s.treeDensity = f(p, "treedensity", 1.0F, 0.4F, 1.4F);
         s.treeScale = f(p, "treescale", 1.0F, 0.7F, 1.5F);
         s.giantTrees = Boolean.parseBoolean(p.getProperty("gianttrees", "true"));
      } catch (NumberFormatException e) {
         s.caveVer = 1;
         s.biomeBorders = 0;
         s.treeDensity = 1.0F;
         s.treeScale = 1.0F;
         s.giantTrees = true;
      }

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

   public static boolean isVoidMode(int t) {
      return t == TUMBLE || t == BATTLE || t == BEDWARS || t == CS2 || t == MURDER || t == END_FIGHT || t == CASTLE_SIEGE;
   }

   public boolean isVoidMode() {
      return isVoidMode(this.type);
   }

   public static boolean isMinigame(int t) {
      return t == TUMBLE || t == BATTLE || t == BEDWARS || t == CS2 || t == MURDER || t == END_FIGHT || t == CASTLE_SIEGE;
   }

   public boolean isMinigame() {
      return isMinigame(this.type);
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
         case "cs2":
         case "strike":
         case "counterstrike":
            return CS2;
         case "murder":
         case "murder_mystery":
         case "mm":
            return MURDER;
         case "end_fight":
         case "endfight":
         case "bed_fight":
         case "bed_spam":
         case "endbed":
            return END_FIGHT;
         case "castle_siege":
         case "castlesiege":
         case "siege":
         case "conquest":
            return CASTLE_SIEGE;
         case "all_blocks":
         case "allblocks":
         case "gallery":
         case "showcase":
            return ALL_BLOCKS;
         default:
            return DEFAULT;
      }
   }
}
