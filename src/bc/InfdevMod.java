package bc;

/**
 * External JAR total-conversion mod ({@code mods/infdev-2010.jar}) that reverts Blockcraft
 * to the state of Minecraft Infdev (June 2010):
 * <ul>
 *   <li><b>World Generation</b> ({@link InfdevGen}): Craggy 3D/2D Perlin overhangs, single-biome neon-green
 *       grass &amp; leaves ({@code #63D43E}), gravel/sand beaches, and solid Infdev Brick Pyramids.</li>
 *   <li><b>Items &amp; Survival Mechanics</b>: No hunger bar, no sprinting, food items restore hearts
 *       instantly on right-click, and the inventory palette is filtered to classic June 2010 Infdev blocks &amp; items.</li>
 *   <li><b>GUI &amp; HUD</b>: Classic {@code "Minecraft Infdev"} top-left watermark, hearts + right-aligned armor bar
 *       above the hotbar (no hunger/XP bar), and retro Infdev title screen branding.</li>
 * </ul>
 */
public final class InfdevMod implements BlockcraftMod {
   private static boolean settingsApplied;
   private static boolean savedShaders = true;
   private static boolean savedSmooth = true;
   private static boolean savedBushy = true;
   private static boolean savedWeather = true;
   private static boolean savedDisasters = true;
   private static boolean savedClouds = true;
   private static boolean savedVariation = true;
   private static int savedLeafMode = 1;
   private static String savedResourcePack = ResourcePacks.ID_DEFAULT;

   @Override
   public String id() {
      return "infdev-2010";
   }

   @Override
   public String name() {
      return "Minecraft Infdev (2010 Total Conversion)";
   }

   @Override
   public String version() {
      return "0.31.20100618";
   }

   @Override
   public String author() {
      return "Notch / Blockcraft Retro";
   }

   @Override
   public String description() {
      return "Reverts world gen, settings (3D blocky clouds, flat lighting, Classic textures, neon grass), HUD & survival to Infdev (June 2010)!";
   }

   @Override
   public void onEnable(Game g) {
      applyInfdevSettings(g);
   }

   @Override
   public void onDisable(Game g) {
      restoreSettings(g);
   }

   public static void applyInfdevSettings(Game g) {
      if (!settingsApplied) {
         savedVariation = Mesher.variation;
         savedLeafMode = Mesher.leafMode;
         savedResourcePack = ResourcePacks.activeId();
         if (g != null && g.opt != null) {
            savedShaders = g.opt.shaders;
            savedSmooth = g.opt.smooth;
            savedBushy = g.opt.bushyLeaves;
            savedWeather = g.opt.weather;
            savedDisasters = g.opt.disasters;
            savedClouds = g.opt.clouds;
            if (g.opt.resourcePack != null && !g.opt.resourcePack.isBlank()) {
               savedResourcePack = g.opt.resourcePack;
            }
         }
         settingsApplied = true;
      }
      // Classic June 2010 Infdev visual & gameplay settings:
      // - Flat per-face lighting (g.opt.smooth = false, no Beta 1.3 smooth ambient occlusion)
      // - No shaders, no bushy leaf protrusions, no random block brightness/rotation variation
      // - Classic 3D blocky clouds enabled at Y=108..112
      // - Fair Infdev skies (no modern weather or disasters)
      // - Classic 16x16 texture pack + radioactive neon-green grass/foliage tint (#63D43E)
      Mesher.bushyLeaves = false;
      Mesher.variation = false;
      Mesher.leafMode = 0;
      if (g != null && g.opt != null) {
         g.opt.shaders = false;
         g.opt.smooth = false;
         g.opt.bushyLeaves = false;
         g.opt.clouds = true;
         g.opt.weather = false;
         g.opt.disasters = false;
      }
      ResourcePacks.select(ResourcePacks.ID_CLASSIC, g);
      if (g != null) {
         g.remeshAll();
      }
   }

   public static void restoreSettings(Game g) {
      if (!settingsApplied) {
         return;
      }
      settingsApplied = false;
      Mesher.bushyLeaves = savedBushy;
      Mesher.variation = savedVariation;
      Mesher.leafMode = savedLeafMode;
      if (g != null && g.opt != null) {
         g.opt.shaders = savedShaders;
         g.opt.smooth = savedSmooth;
         g.opt.bushyLeaves = savedBushy;
         g.opt.clouds = savedClouds;
         g.opt.weather = savedWeather;
         g.opt.disasters = savedDisasters;
      }
      ResourcePacks.select(savedResourcePack != null ? savedResourcePack : ResourcePacks.ID_DEFAULT, g);
      if (g != null) {
         g.remeshAll();
      }
   }

   @Override
   public boolean totalConversionInfdev() {
      return true;
   }

   @Override
   public String hudWatermark() {
      return "Minecraft Infdev";
   }

   @Override
   public boolean classicHudNoHunger() {
      return true;
   }

   @Override
   public Gen createWorldGen(int dim, long seed, WorldSettings settings) {
      if (dim == Dim.OVERWORLD && (settings == null || (!settings.isVoidMode() && settings.type != WorldSettings.FLAT))) {
         return new InfdevGen(seed, settings != null ? settings : new WorldSettings());
      }
      return null;
   }

   @Override
   public boolean allowPaletteItem(int id) {
      // Classic June 2010 Infdev block & item allowlist
      return switch (id) {
         case Blocks.STONE,
            Blocks.GRASS,
            Blocks.DIRT,
            Blocks.COBBLE,
            Blocks.PLANKS,
            Blocks.BEDROCK,
            Blocks.SAND,
            Blocks.GRAVEL,
            Blocks.LOG_OAK,
            Blocks.LEAVES_OAK,
            Blocks.GLASS,
            Blocks.BRICKS,
            Blocks.COAL_ORE,
            Blocks.IRON_ORE,
            Blocks.GOLD_ORE,
            Blocks.DIAMOND_ORE,
            Blocks.MOSSY_COBBLE,
            Blocks.BOOKSHELF,
            Blocks.OBSIDIAN,
            Blocks.FLOWER_YELLOW,
            Blocks.FLOWER_RED,
            Blocks.TORCH,
            Blocks.WATER,
            Blocks.LAVA,
            Blocks.CRAFTING_TABLE,
            Blocks.FURNACE,
            Blocks.CHEST,
            Blocks.TNT,
            Blocks.SLAB0,
            Blocks.SLAB0 + 1,
            Blocks313.MUSHROOM_BROWN,
            Blocks313.MUSHROOM_RED,
            Blocks.IRON_BLOCK,
            Blocks.GOLD_BLOCK,
            Blocks.DIAMOND_BLOCK,
            Blocks.LADDER,
            Redstone319.LEVER_OFF,
            Items.STICK,
            Items.COAL,
            Items.IRON_INGOT,
            Items.GOLD_INGOT,
            Items.DIAMOND,
            Items.FEATHER,
            Items.STRING,
            Items.GUNPOWDER,
            Items.FLINT,
            Items.ARROW,
            Items.BOW,
            Items.APPLE,
            Items.RAW_PORK,
            Items.COOKED_PORK,
            Items.DOOR,
            Items.BUCKET,
            Items.WATER_BUCKET,
            Items.LAVA_BUCKET,
            Items.SEEDS,
            Items.WHEAT,
            Items.BREAD,
            Items.FLINT_STEEL -> true;
         default -> (id >= Blocks.WOOL0 && id < Blocks.WOOL0 + 16)
            || (id >= Blocks.STAIR0 && id < Blocks.STAIR0 + 8)
            || (id >= Items.HOE0 && id < Items.HOE0 + 5)
            || (id >= Items.TOOL_BASE && id < Items.TOOL_BASE + 20)
            || (id >= Items.ARMOR_BASE && id < Items.ARMOR_BASE + 16);
      };
   }

   @Override
   public void onInit(ModLoader.Context ctx) {
      // Classic Infdev 2x2 String -> White Wool recipe
      ctx.addShapelessRecipe(Blocks.WOOL0, 1, Items.STRING, Items.STRING, Items.STRING, Items.STRING);
   }

   @Override
   public void onGameTick(Game g) {
      if (g == null) {
         return;
      }
      if (!settingsApplied || (g.opt != null && (g.opt.smooth || g.opt.shaders || g.opt.bushyLeaves))) {
         applyInfdevSettings(g);
      }
      if (g.player == null) {
         return;
      }
      // In Infdev, there is no hunger drain, no hunger auto-regen, and no sprinting
      g.player.food = 14;
      g.player.sat = 0.0F;
      g.player.exh = 0.0F;
      g.player.sprinting = false;
   }

   @Override
   public boolean onCommand(Game g, String cmd, String[] args) {
      if (cmd.equals("/pyramid")) {
         int top = InfdevGen.pyramidTopAt(InfdevGen.PYRAMID_X, InfdevGen.PYRAMID_Z);
         g.player.setPos(InfdevGen.PYRAMID_X + 0.5, top + 2.0, InfdevGen.PYRAMID_Z - 18.5);
         g.player.yaw = 0.0F;
         g.player.pitch = 10.0F;
         g.player.vx = g.player.vy = g.player.vz = 0.0;
         g.chatMsg("[Infdev] Teleported to the Infdev Brick Pyramid at (" + InfdevGen.PYRAMID_X + ", " + top + ", " + InfdevGen.PYRAMID_Z + ")!");
         return true;
      }
      if (cmd.equals("/infdev")) {
         applyInfdevSettings(g);
         g.chatMsg("[Infdev 2010] Active! Applied Infdev settings (flat lighting, 3D blocky clouds, Classic textures, neon grass, no hunger).");
         return true;
      }
      return false;
   }
}
