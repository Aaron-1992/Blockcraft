package bc;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Mod Loader & Modrinth-style Mod Repository Manager for Blockcraft.
 * Supports:
 * <ul>
 *   <li>Curated Modrinth-style catalog mods that can be installed & toggled in-game or from the external Mod Manager</li>
 *   <li>External Java {@code .jar} mods implementing {@link BlockcraftMod}</li>
 *   <li>Declarative {@code .bcmod} / {@code .properties} / {@code .zip} mods defining custom recipes, smelting, and commands</li>
 * </ul>
 */
public final class ModLoader {
   public static final class ModEntry {
      public final String id;
      public final String name;
      public final String version;
      public final String author;
      public final String category;
      public final String summary;
      public final int downloads;
      public final int badgeColor;
      public final boolean catalogMod;
      public Path sourcePath;
      public boolean installed;
      public boolean enabled;
      public BlockcraftMod instance;
      final Properties declProps = new Properties();

      public ModEntry(
         String id,
         String name,
         String version,
         String author,
         String category,
         String summary,
         int downloads,
         int badgeColor,
         boolean catalogMod
      ) {
         this.id = id;
         this.name = name;
         this.version = version;
         this.author = author;
         this.category = category;
         this.summary = summary;
         this.downloads = downloads;
         this.badgeColor = badgeColor;
         this.catalogMod = catalogMod;
      }
   }

   public static final class Context {
      private final String modId;

      Context(String modId) {
         this.modId = modId;
      }

      public String modId() {
         return this.modId;
      }

      public void addShapelessRecipe(int outId, int outCount, int... ingredients) {
         if (!isValidItemOrBlockId(outId) || outCount <= 0 || ingredients == null || ingredients.length == 0) {
            return;
         }
         for (int in : ingredients) {
            if (!isValidItemOrBlockId(in)) {
               return;
            }
         }
         ensureBaseRecipesRecorded();
         Recipes.shapeless(outId, outCount, ingredients);
      }

      public void addShapedRecipe(int outId, int outCount, String[] pattern, Object... keys) {
         if (!isValidItemOrBlockId(outId) || outCount <= 0) {
            return;
         }
         ensureBaseRecipesRecorded();
         Recipes.shaped(outId, outCount, pattern, keys);
      }

      public void addSmelting(int inId, int outId, int outCount) {
         if (!isValidItemOrBlockId(inId) || !isValidItemOrBlockId(outId) || outCount <= 0) {
            return;
         }
         ensureBaseRecipesRecorded();
         Recipes.smelt.put(inId, new Stack(outId, outCount));
      }
   }

   private static Path customDir;
   private static boolean initialized;
   private static boolean cuttingTree;
   private static boolean nightVisionActive;
   public static volatile Game activeGame;
   private static final Map<String, ModEntry> entries = new LinkedHashMap<>();
   private static final Map<String, double[]> waypoints = new HashMap<>();
   private static int baseRecipeCount = -1;
   private static Map<Integer, Stack> baseSmeltSnapshot;

   private ModLoader() {
   }

   /** Returns true only if {@code id} is a defined, non-unknown placeable block ID. */
   public static boolean isValidBlockId(int id) {
      if (!Blocks.isBlockId(id)) {
         return false;
      }
      String n = Blocks.name[id];
      return n != null && !n.isEmpty() && !n.equalsIgnoreCase("Unknown");
   }

   /** Returns true if {@code id} is a defined, non-unknown block or item ID. */
   public static boolean isValidItemOrBlockId(int id) {
      if (id <= 0 || id >= Blocks.EXT0) {
         return false;
      }
      String n = Items.name(id);
      return n != null && !n.isEmpty() && !n.equalsIgnoreCase("Unknown");
   }

   /** Resolves either a numeric block ID or a block name (e.g. "stonebrick", "glass", "planks") to a valid block ID. */
   public static int resolveBlockId(String arg, int defaultId) {
      if (arg == null || arg.isBlank()) {
         return defaultId;
      }
      String q = arg.trim().toLowerCase(Locale.ROOT);
      try {
         int id = Integer.parseInt(q);
         return isValidBlockId(id) ? id : defaultId;
      } catch (NumberFormatException ignored) {
      }
      return switch (q) {
         case "stone" -> Blocks.STONE;
         case "grass" -> Blocks.GRASS;
         case "dirt" -> Blocks.DIRT;
         case "cobble", "cobblestone" -> Blocks.COBBLE;
         case "planks", "wood", "oak_planks" -> Blocks.PLANKS;
         case "sand" -> Blocks.SAND;
         case "gravel" -> Blocks.GRAVEL;
         case "log", "oak_log" -> Blocks.LOG_OAK;
         case "glass" -> Blocks.GLASS;
         case "brick", "bricks" -> Blocks.BRICKS;
         case "sandstone" -> Blocks.SANDSTONE;
         case "glowstone" -> Blocks.GLOWSTONE;
         case "obsidian" -> Blocks.OBSIDIAN;
         case "stonebrick", "stone_bricks" -> Blocks.STONEBRICK;
         case "torch" -> Blocks.TORCH;
         case "wool" -> Blocks.WOOL0;
         case "iron", "iron_block" -> Blocks.IRON_BLOCK;
         case "gold", "gold_block" -> Blocks.GOLD_BLOCK;
         case "diamond", "diamond_block" -> Blocks.DIAMOND_BLOCK;
         case "emerald", "emerald_block" -> Blocks.EMERALD_BLOCK;
         case "quartz" -> Blocks.QUARTZ_BLOCK;
         default -> defaultId;
      };
   }

   public static synchronized Path dir() {
      if (customDir != null) {
         return customDir;
      }
      Path d = new File("mods").toPath();
      try {
         Files.createDirectories(d);
         seedReadme(d);
      } catch (IOException ignored) {
      }
      customDir = d;
      return d;
   }

   public static synchronized void setDirForTest(Path d) {
      customDir = d;
      initialized = false;
      refresh();
   }

   private static void seedReadme(Path d) {
      Path readme = d.resolve("README.txt");
      if (!Files.exists(readme)) {
         String txt = String.join(
            System.lineSeparator(),
            "Blockcraft Mods Folder",
            "======================",
            "Place .jar mods (implementing bc.BlockcraftMod) or declarative .bcmod/.zip mods here,",
            "or install curated mods directly from the in-game Mods screen or Blockcraft-mods.bat!",
            "",
            "Included external total-conversion JAR mod:",
            "  infdev-2010.jar - Reverts Blockcraft to Minecraft Infdev (June 2010): craggy overhangs,",
            "                    Brick Pyramids, neon-green grass, classic HUD (no hunger bar), instant food healing.",
            "",
            "Declarative .bcmod keys:",
            "  id=my-mod",
            "  name=My Custom Mod",
            "  version=1.0.0",
            "  author=Player",
            "  description=Adds custom recipes and commands",
            "  shapeless.1=266,1:8,8",
            "  smelt.1=280:261,1",
            "  command.greet=Hello from My Custom Mod!"
         );
         try {
            Files.writeString(readme, txt, StandardCharsets.UTF_8);
         } catch (IOException ignored) {
         }
      }
   }

   static synchronized void ensureBaseRecipesRecorded() {
      if (baseRecipeCount < 0) {
         int _touch = Recipes.list.size();
         baseRecipeCount = _touch;
         baseSmeltSnapshot = new HashMap<>(Recipes.smelt);
      }
   }

   private static synchronized void restoreBaseRecipes() {
      if (baseRecipeCount >= 0) {
         while (Recipes.list.size() > baseRecipeCount) {
            Recipes.list.remove(Recipes.list.size() - 1);
         }
         Recipes.smelt.clear();
         Recipes.smelt.putAll(baseSmeltSnapshot);
      }
   }

   public static synchronized void init() {
      if (!initialized) {
         refresh();
      }
   }

   public static synchronized void refresh() {
      entries.clear();
      registerBuiltInCatalog();

      Path d = dir();
      Properties state = loadState(d);

      if (Files.isDirectory(d)) {
         List<Path> files = new ArrayList<>();
         try (DirectoryStream<Path> ds = Files.newDirectoryStream(d)) {
            for (Path p : ds) {
               files.add(p);
            }
         } catch (IOException ignored) {
         }
         files.sort(Comparator.comparing(p -> p.getFileName().toString().toLowerCase(Locale.ROOT)));
         for (Path p : files) {
            String fn = p.getFileName().toString().toLowerCase(Locale.ROOT);
            if (fn.equals("mods-state.properties") || fn.equals("readme.txt")) {
               continue;
            }
            if (fn.endsWith(".bcmod") || fn.endsWith(".properties")) {
               loadDeclarativeModFile(p);
            } else if (fn.endsWith(".zip")) {
               loadZipModFile(p);
            } else if (fn.endsWith(".jar")) {
               loadJarModFile(p);
            }
         }
      }

      for (ModEntry m : entries.values()) {
         String instKey = "installed." + m.id;
         String enKey = "enabled." + m.id;
         if (!m.catalogMod) {
            m.installed = true;
            // Total-conversion mods (like infdev-2010) are pre-installed and toggled on via the Mods screen, Blockcraft-mods.bat, or /infdev
            String defEn = (m.instance != null && m.instance.totalConversionInfdev()) ? "false" : "true";
            m.enabled = Boolean.parseBoolean(state.getProperty(enKey, defEn));
         } else {
            m.installed = Boolean.parseBoolean(state.getProperty(instKey, "false"));
            m.enabled = m.installed && Boolean.parseBoolean(state.getProperty(enKey, "true"));
         }
      }

      initialized = true;
      rebuildActiveModEffects();
   }

   private static void registerBuiltInCatalog() {
      addCatalog(new ModEntry(
         "waystones-hud",
         "Explorer Compass & Waypoints",
         "1.4.2",
         "Blockcraft Team",
         "Utility / HUD",
         "Adds a compact top-right Explorer HUD (compass, biome, day & FPS) plus /wp set & /wp tp commands.",
         148200,
         0xFF00C853,
         true
      ), new BlockcraftMod() {
         @Override
         public String id() {
            return "waystones-hud";
         }

         @Override
         public String name() {
            return "Explorer Compass & Waypoints";
         }

         @Override
         public boolean onCommand(Game g, String cmd, String[] args) {
            if (!cmd.equals("/wp") && !cmd.equals("/waypoint")) {
               return false;
            }
            if (args.length >= 2 && args[1].equalsIgnoreCase("set")) {
               String label = args.length >= 3 ? args[2] : "home";
               waypoints.put(label.toLowerCase(Locale.ROOT), new double[]{g.player.x, g.player.y, g.player.z});
               g.chatMsg(String.format(Locale.ROOT, "[Waypoints] Saved '%s' at %.0f, %.0f, %.0f", label, g.player.x, g.player.y, g.player.z));
               return true;
            }
            if (args.length >= 2 && (args[1].equalsIgnoreCase("tp") || args[1].equalsIgnoreCase("go"))) {
               String label = args.length >= 3 ? args[2] : "home";
               double[] pos = waypoints.get(label.toLowerCase(Locale.ROOT));
               if (pos == null) {
                  g.chatMsg("[Waypoints] Unknown waypoint: " + label);
               } else {
                  g.player.setPos(pos[0], pos[1], pos[2]);
                  g.player.vx = g.player.vy = g.player.vz = 0.0;
                  g.chatMsg("[Waypoints] Teleported to '" + label + "'");
               }
               return true;
            }
            g.chatMsg("[Waypoints] Usage: /wp set [name] | /wp tp [name] (" + waypoints.size() + " saved)");
            return true;
         }
      });

      addCatalog(new ModEntry(
         "recipe-plus",
         "CraftPlus QoL Recipes",
         "2.1.0",
         "AeroCraft",
         "Crafting",
         "Adds survival recipes: 2 Gravel->Flint, 2 Cobble->2 Sand, Wool->4 String, 2 Leaves->2 Sticks, Rotten Flesh->Leather.",
         215900,
         0xFFFFAB00,
         true
      ), new BlockcraftMod() {
         @Override
         public String id() {
            return "recipe-plus";
         }

         @Override
         public String name() {
            return "CraftPlus QoL Recipes";
         }

         @Override
         public void onInit(Context ctx) {
            // 2 Gravel (8) -> 1 Flint (266)
            ctx.addShapelessRecipe(Items.FLINT, 1, Blocks.GRAVEL, Blocks.GRAVEL);
            // 2 Cobblestone (4) -> 2 Sand (7)
            ctx.addShapelessRecipe(Blocks.SAND, 2, Blocks.COBBLE, Blocks.COBBLE);
            // 1 White Wool (34) -> 4 String (263)
            ctx.addShapelessRecipe(Items.STRING, 4, Blocks.WOOL0);
            // 2 Oak Leaves (12) -> 2 Sticks (256)
            ctx.addShapelessRecipe(Items.STICK, 2, Blocks.LEAVES_OAK, Blocks.LEAVES_OAK);
            // Smelt Rotten Flesh (280) -> Leather (261)
            ctx.addSmelting(Items.ROTTEN_FLESH, Items.LEATHER, 1);
         }
      });

      addCatalog(new ModEntry(
         "timber-assist",
         "Timber & Tree Feller",
         "1.3.0",
         "Woodsman",
         "Gameplay",
         "Chopping the base of a tree fells all connected logs above it, drops the logs, and adds /timber.",
         184500,
         0xFF8D6E63,
         true
      ), new BlockcraftMod() {
         @Override
         public String id() {
            return "timber-assist";
         }

         @Override
         public String name() {
            return "Timber & Tree Feller";
         }

         @Override
         public void onBlockBroken(World w, int x, int y, int z, int oldId) {
            if (cuttingTree || !Blocks.isAnyLog(oldId)) {
               return;
            }
            cuttingTree = true;
            try {
               int baseLog = baseLogId(oldId);
               // Fell connected logs above
               int felled = 0;
               for (int dy = 1; dy <= 14; dy++) {
                  boolean anyAtLayer = false;
                  for (int dz = -1; dz <= 1; dz++) {
                     for (int dx = -1; dx <= 1; dx++) {
                        int bx = x + dx;
                        int by = y + dy;
                        int bz = z + dz;
                        int bid = w.getBlock(bx, by, bz);
                        if (Blocks.isAnyLog(bid)) {
                           w.setBlock(bx, by, bz, Blocks.AIR);
                           if (w.game != null) {
                              w.game.meshNow(bx, by, bz);
                           }
                           felled++;
                           anyAtLayer = true;
                        }
                     }
                  }
                  if (!anyAtLayer && dy > 2) {
                     break;
                  }
               }
               if (felled > 0 && (w.game == null || w.game.player == null || !w.game.player.creative)) {
                  w.dropItem(x + 0.5, y + 0.5, z + 0.5, baseLog, felled);
               }
            } finally {
               cuttingTree = false;
            }
         }

         @Override
         public boolean onCommand(Game g, String cmd, String[] args) {
            if (!cmd.equals("/timber")) {
               return false;
            }
            int px = (int)Math.floor(g.player.x);
            int py = (int)Math.floor(g.player.y);
            int pz = (int)Math.floor(g.player.z);
            int felled = 0;
            for (int dy = -2; dy <= 12; dy++) {
               for (int dz = -4; dz <= 4; dz++) {
                  for (int dx = -4; dx <= 4; dx++) {
                     int bx = px + dx;
                     int by = py + dy;
                     int bz = pz + dz;
                     int id = g.world.getBlock(bx, by, bz);
                     if (Blocks.isAnyLog(id)) {
                        g.world.setBlock(bx, by, bz, Blocks.AIR);
                        felled++;
                     }
                  }
               }
            }
            if (felled > 0) {
               g.remeshAll();
            }
            g.chatMsg("[Timber] Harvested " + felled + " log blocks.");
            return true;
         }
      });

      addCatalog(new ModEntry(
         "builder-wand",
         "Builder's Wand & WorldEdit Lite",
         "1.8.0",
         "VoxelArchitect",
         "Building",
         "Adds /up [h], /platform [r] [block], /walls [r] [h] [block], and /sphere [r] [block] with safe block validation.",
         131400,
         0xFF29B6F6,
         true
      ), new BlockcraftMod() {
         @Override
         public String id() {
            return "builder-wand";
         }

         @Override
         public String name() {
            return "Builder's Wand & WorldEdit Lite";
         }

         @Override
         public boolean onCommand(Game g, String cmd, String[] args) {
            if (cmd.equals("/up")) {
               int h = args.length >= 2 ? parseInt(args[1], 10) : 10;
               int bx = (int)Math.floor(g.player.x);
               int by = Math.max(2, Math.min(250, (int)Math.floor(g.player.y) + h));
               int bz = (int)Math.floor(g.player.z);
               g.world.setBlock(bx, by - 1, bz, Blocks.GLASS);
               g.meshNow(bx, by - 1, bz);
               g.player.setPos(bx + 0.5, by, bz + 0.5);
               g.player.vy = 0.0;
               g.chatMsg("[Builder] Ascended to Y=" + by + " on a Glass block.");
               return true;
            }
            if (cmd.equals("/platform")) {
               int rad = args.length >= 2 ? Math.max(1, Math.min(12, parseInt(args[1], 3))) : 3;
               int blockId = args.length >= 3 ? resolveBlockId(args[2], Blocks.STONEBRICK) : Blocks.STONEBRICK;
               int bx = (int)Math.floor(g.player.x);
               int by = Math.max(1, (int)Math.floor(g.player.y) - 1);
               int bz = (int)Math.floor(g.player.z);
               int count = 0;
               for (int dz = -rad; dz <= rad; dz++) {
                  for (int dx = -rad; dx <= rad; dx++) {
                     if (g.world.setBlock(bx + dx, by, bz + dz, blockId)) {
                        count++;
                     }
                  }
               }
               g.remeshAll();
               g.chatMsg("[Builder] Built " + (rad * 2 + 1) + "x" + (rad * 2 + 1) + " " + Items.name(blockId) + " platform (" + count + " blocks).");
               return true;
            }
            if (cmd.equals("/walls")) {
               int rad = args.length >= 2 ? Math.max(1, Math.min(12, parseInt(args[1], 4))) : 4;
               int h = args.length >= 3 ? Math.max(1, Math.min(16, parseInt(args[2], 4))) : 4;
               int blockId = args.length >= 4 ? resolveBlockId(args[3], Blocks.STONEBRICK) : Blocks.STONEBRICK;
               int bx = (int)Math.floor(g.player.x);
               int by = Math.max(1, (int)Math.floor(g.player.y));
               int bz = (int)Math.floor(g.player.z);
               int count = 0;
               for (int dy = 0; dy < h; dy++) {
                  for (int dz = -rad; dz <= rad; dz++) {
                     for (int dx = -rad; dx <= rad; dx++) {
                        if (Math.abs(dx) == rad || Math.abs(dz) == rad) {
                           if (g.world.setBlock(bx + dx, by + dy, bz + dz, blockId)) {
                              count++;
                           }
                        }
                     }
                  }
               }
               g.remeshAll();
               g.chatMsg("[Builder] Built " + (rad * 2 + 1) + "x" + (rad * 2 + 1) + "x" + h + " " + Items.name(blockId) + " walls (" + count + " blocks).");
               return true;
            }
            if (cmd.equals("/sphere")) {
               int rad = args.length >= 2 ? Math.max(1, Math.min(8, parseInt(args[1], 3))) : 3;
               int blockId = args.length >= 3 ? resolveBlockId(args[2], Blocks.GLASS) : Blocks.GLASS;
               int bx = (int)Math.floor(g.player.x);
               int by = Math.max(rad + 1, (int)Math.floor(g.player.y));
               int bz = (int)Math.floor(g.player.z);
               int count = 0;
               for (int dy = -rad; dy <= rad; dy++) {
                  for (int dz = -rad; dz <= rad; dz++) {
                     for (int dx = -rad; dx <= rad; dx++) {
                        double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
                        if (d <= rad + 0.35 && d >= rad - 0.85) {
                           if (g.world.setBlock(bx + dx, by + dy, bz + dz, blockId)) {
                              count++;
                           }
                        }
                     }
                  }
               }
               g.remeshAll();
               g.chatMsg("[Builder] Built hollow " + Items.name(blockId) + " sphere (" + count + " blocks).");
               return true;
            }
            return false;
         }
      });

      addCatalog(new ModEntry(
         "peaceful-regen",
         "Campfire Comfort & Warmth",
         "1.1.4",
         "CozyRealms",
         "Survival",
         "Standing near warm light sources (block light >= 10) gently restores player health and hunger over time.",
         96400,
         0xFFEF5350,
         true
      ), new BlockcraftMod() {
         @Override
         public String id() {
            return "peaceful-regen";
         }

         @Override
         public String name() {
            return "Campfire Comfort & Warmth";
         }

         @Override
         public void onGameTick(Game g) {
            if (g.world == null || g.world.tickCount % 40L != 0L || g.player == null) {
               return;
            }
            int px = (int)Math.floor(g.player.x);
            int py = (int)Math.floor(g.player.y);
            int pz = (int)Math.floor(g.player.z);
            if (g.world.getBlk(px, py, pz) >= 10) {
               if (g.player.health > 0 && g.player.health < 20) {
                  g.player.health = Math.min(20, g.player.health + 1);
               }
               if (g.player.food < 20) {
                  g.player.food = Math.min(20, g.player.food + 1);
                  g.player.sat = Math.max(g.player.sat, 4.0F);
               }
            }
         }
      });

      addCatalog(new ModEntry(
         "torch-master",
         "Miner's Torch Assist",
         "1.2.1",
         "DeepDelver",
         "Caving",
         "Auto-places torches in dark caves while mining, plus /lightup and /nightvision commands.",
         112800,
         0xFFAB47BC,
         true
      ), new BlockcraftMod() {
         @Override
         public String id() {
            return "torch-master";
         }

         @Override
         public String name() {
            return "Miner's Torch Assist";
         }

         @Override
         public void onGameTick(Game g) {
            if (g.world == null || g.player == null || g.world.tickCount % 20L != 0L) {
               return;
            }
            int px = (int)Math.floor(g.player.x);
            int py = (int)Math.floor(g.player.y);
            int pz = (int)Math.floor(g.player.z);
            if (py > 1 && py < 56 && g.world.getBlock(px, py, pz) == Blocks.AIR && Blocks.solid[g.world.getBlock(px, py - 1, pz)]) {
               if (g.world.getBlk(px, py, pz) < 4 && g.world.getSky(px, py, pz) < 4) {
                  if (g.world.setBlock(px, py, pz, Blocks.TORCH)) {
                     g.meshNow(px, py, pz);
                  }
               }
            }
         }

         @Override
         public boolean onCommand(Game g, String cmd, String[] args) {
            if (cmd.equals("/nightvision") || cmd.equals("/nv")) {
               nightVisionActive = !nightVisionActive;
               g.chatMsg("[TorchMaster] Night Vision " + (nightVisionActive ? "ENABLED" : "DISABLED"));
               return true;
            }
            if (!cmd.equals("/lightup")) {
               return false;
            }
            int px = (int)Math.floor(g.player.x);
            int py = (int)Math.floor(g.player.y);
            int pz = (int)Math.floor(g.player.z);
            int placed = 0;
            for (int dz = -6; dz <= 6; dz += 3) {
               for (int dx = -6; dx <= 6; dx += 3) {
                  for (int dy = -2; dy <= 2; dy++) {
                     int x = px + dx;
                     int y = py + dy;
                     int z = pz + dz;
                     if (y > 1
                        && g.world.getBlock(x, y, z) == Blocks.AIR
                        && Blocks.solid[g.world.getBlock(x, y - 1, z)]
                        && g.world.getBlk(x, y, z) < 7) {
                        if (g.world.setBlock(x, y, z, Blocks.TORCH)) {
                           g.meshNow(x, y, z);
                           placed++;
                           break;
                        }
                     }
                  }
               }
            }
            g.chatMsg("[TorchMaster] Placed " + placed + " torches.");
            return true;
         }
      });
      addCatalog(new ModEntry(
         "infdev-2010",
         "Minecraft Infdev (2010 Total Conversion)",
         "0.31.20100618",
         "Notch / Blockcraft Retro",
         "Total Conversion",
         "Reverts world gen, 3D blocky clouds, sky, lighting, items, food & HUD to Minecraft Infdev (June 2010)!",
         342000,
         0xFF63D43E,
         true
      ), new InfdevMod());
   }

   private static void addCatalog(ModEntry entry, BlockcraftMod impl) {
      entry.instance = impl;
      entries.put(entry.id, entry);
   }

   private static void loadDeclarativeModFile(Path p) {
      Properties props = new Properties();
      try (InputStream in = Files.newInputStream(p)) {
         props.load(in);
      } catch (IOException e) {
         return;
      }
      registerDeclarativeProps(p, props);
   }

   private static void loadZipModFile(Path zipPath) {
      try (ZipFile zf = new ZipFile(zipPath.toFile())) {
         ZipEntry ze = zf.getEntry("mod.properties");
         if (ze == null) {
            ze = zf.getEntry("bcmod.properties");
         }
         if (ze == null) {
            return;
         }
         Properties props = new Properties();
         try (InputStream in = zf.getInputStream(ze)) {
            props.load(new StringReader(new String(in.readAllBytes(), StandardCharsets.UTF_8)));
         }
         registerDeclarativeProps(zipPath, props);
      } catch (IOException ignored) {
      }
   }

   private static void registerDeclarativeProps(Path source, Properties props) {
      String fn = source.getFileName().toString();
      int dot = fn.lastIndexOf('.');
      String fallbackId = (dot > 0 ? fn.substring(0, dot) : fn).toLowerCase(Locale.ROOT);
      String id = props.getProperty("id", fallbackId).trim();
      String name = props.getProperty("name", id);
      String ver = props.getProperty("version", "1.0.0");
      String author = props.getProperty("author", "Community");
      String cat = props.getProperty("category", "Custom Mod");
      String desc = props.getProperty("description", "Custom declarative mod (" + fn + ")");
      ModEntry entry = new ModEntry(id, name, ver, author, cat, desc, 1, 0xFF26A69A, false);
      entry.sourcePath = source;
      entry.installed = true;
      entry.enabled = true;
      entry.declProps.putAll(props);
      entry.instance = createDeclarativeInstance(entry);
      entries.put(id, entry);
   }

   private static BlockcraftMod createDeclarativeInstance(ModEntry entry) {
      return new BlockcraftMod() {
         @Override
         public String id() {
            return entry.id;
         }

         @Override
         public String name() {
            return entry.name;
         }

         @Override
         public String version() {
            return entry.version;
         }

         @Override
         public String author() {
            return entry.author;
         }

         @Override
         public String description() {
            return entry.summary;
         }

         @Override
         public void onInit(Context ctx) {
            for (String key : entry.declProps.stringPropertyNames()) {
               String val = entry.declProps.getProperty(key, "").trim();
               if (key.startsWith("shapeless.")) {
                  parseShapelessRule(ctx, val);
               } else if (key.startsWith("smelt.")) {
                  parseSmeltRule(ctx, val);
               }
            }
         }

         @Override
         public boolean onCommand(Game g, String cmd, String[] args) {
            if (cmd.startsWith("/")) {
               String cname = cmd.substring(1).toLowerCase(Locale.ROOT);
               String reply = entry.declProps.getProperty("command." + cname);
               if (reply != null) {
                  g.chatMsg("[" + entry.name + "] " + reply);
                  return true;
               }
            }
            return false;
         }
      };
   }

   /** Format: outId,outCount:in1,in2,... */
   private static void parseShapelessRule(Context ctx, String spec) {
      int colon = spec.indexOf(':');
      if (colon <= 0) {
         return;
      }
      String[] outParts = spec.substring(0, colon).split(",");
      String[] inParts = spec.substring(colon + 1).split(",");
      int outId = parseInt(outParts[0], 0);
      int outCount = outParts.length >= 2 ? parseInt(outParts[1], 1) : 1;
      if (outId <= 0 || inParts.length == 0) {
         return;
      }
      int[] ins = new int[inParts.length];
      for (int i = 0; i < inParts.length; i++) {
         ins[i] = parseInt(inParts[i], 0);
         if (ins[i] <= 0) {
            return;
         }
      }
      ctx.addShapelessRecipe(outId, outCount, ins);
   }

   /** Format: inId:outId,outCount */
   private static void parseSmeltRule(Context ctx, String spec) {
      int colon = spec.indexOf(':');
      if (colon <= 0) {
         return;
      }
      int inId = parseInt(spec.substring(0, colon), 0);
      String[] outParts = spec.substring(colon + 1).split(",");
      int outId = parseInt(outParts[0], 0);
      int outCount = outParts.length >= 2 ? parseInt(outParts[1], 1) : 1;
      if (inId > 0 && outId > 0) {
         ctx.addSmelting(inId, outId, outCount);
      }
   }

   private static void loadJarModFile(Path jarPath) {
      try {
         URL url = jarPath.toUri().toURL();
         URLClassLoader cl = new URLClassLoader(new URL[]{url}, ModLoader.class.getClassLoader());
         List<BlockcraftMod> found = new ArrayList<>();
         for (BlockcraftMod mod : ServiceLoader.load(BlockcraftMod.class, cl)) {
            found.add(mod);
         }
         if (found.isEmpty()) {
            try (JarFile jf = new JarFile(jarPath.toFile())) {
               Manifest mf = jf.getManifest();
               if (mf != null) {
                  String mc = mf.getMainAttributes().getValue("Mod-Class");
                  if (mc != null && !mc.isBlank()) {
                     Class<?> c = Class.forName(mc.trim(), true, cl);
                     if (BlockcraftMod.class.isAssignableFrom(c)) {
                        found.add((BlockcraftMod)c.getDeclaredConstructor().newInstance());
                     }
                  }
               }
            }
         }
         for (BlockcraftMod mod : found) {
            ModEntry e = new ModEntry(
               mod.id(),
               mod.name(),
               mod.version(),
               mod.author(),
               mod.totalConversionInfdev() ? "Total Conversion" : "JAR Mod",
               mod.description().isEmpty() ? "External Java JAR mod (" + jarPath.getFileName() + ")" : mod.description(),
               342000,
               mod.totalConversionInfdev() ? 0xFF63D43E : 0xFF7E57C2,
               false
            );
            e.sourcePath = jarPath;
            e.installed = true;
            e.enabled = true;
            e.instance = mod;
            entries.put(e.id, e);
         }
      } catch (Throwable t) {
         System.err.println("ModLoader: failed to load " + jarPath + ": " + t);
      }
   }

   private static Properties loadState(Path d) {
      Properties p = new Properties();
      Path f = d.resolve("mods-state.properties");
      if (Files.isRegularFile(f)) {
         try (InputStream in = Files.newInputStream(f)) {
            p.load(in);
         } catch (IOException ignored) {
         }
      }
      return p;
   }

   public static synchronized void saveState() {
      Path d = dir();
      Properties p = new Properties();
      for (ModEntry m : entries.values()) {
         p.setProperty("installed." + m.id, Boolean.toString(m.installed));
         p.setProperty("enabled." + m.id, Boolean.toString(m.enabled));
      }
      Path f = d.resolve("mods-state.properties");
      try (OutputStream out = Files.newOutputStream(f)) {
         p.store(out, "Blockcraft Mod State");
      } catch (IOException ignored) {
      }
   }

   public static synchronized void rebuildActiveModEffects() {
      ensureBaseRecipesRecorded();
      restoreBaseRecipes();
      for (ModEntry m : entries.values()) {
         if (m.installed && m.enabled && m.instance != null) {
            try {
               m.instance.onInit(new Context(m.id));
            } catch (Throwable t) {
               System.err.println("Mod onInit failed (" + m.id + "): " + t);
            }
         }
      }
   }

   public static synchronized List<ModEntry> allMods() {
      if (!initialized) {
         refresh();
      }
      return Collections.unmodifiableList(new ArrayList<>(entries.values()));
   }

   public static synchronized List<ModEntry> installedMods() {
      if (!initialized) {
         refresh();
      }
      List<ModEntry> out = new ArrayList<>();
      for (ModEntry m : entries.values()) {
         if (m.installed) {
            out.add(m);
         }
      }
      return out;
   }

   public static synchronized int activeCount() {
      if (!initialized) {
         refresh();
      }
      int n = 0;
      for (ModEntry m : entries.values()) {
         if (m.installed && m.enabled) {
            n++;
         }
      }
      return n;
   }

   public static synchronized ModEntry get(String id) {
      if (!initialized) {
         refresh();
      }
      return entries.get(id);
   }

   public static synchronized void install(String id) {
      if (!initialized) {
         refresh();
      }
      ModEntry m = entries.get(id);
      if (m != null) {
         boolean wasEnabled = m.enabled;
         m.installed = true;
         m.enabled = true;
         saveState();
         rebuildActiveModEffects();
         if (!wasEnabled && m.instance != null) {
            try {
               m.instance.onEnable(activeGame);
            } catch (Throwable ignored) {
            }
         }
         if (Net319.isHosting()) {
            Net319.sendModsSync();
         }
      }
   }

   public static synchronized void uninstall(String id) {
      if (!initialized) {
         refresh();
      }
      ModEntry m = entries.get(id);
      if (m != null && m.catalogMod) {
         boolean wasEnabled = m.enabled;
         m.installed = false;
         m.enabled = false;
         saveState();
         rebuildActiveModEffects();
         if (wasEnabled && m.instance != null) {
            try {
               m.instance.onDisable(activeGame);
            } catch (Throwable ignored) {
            }
         }
         if (Net319.isHosting()) {
            Net319.sendModsSync();
         }
      }
   }

   public static synchronized void setEnabled(String id, boolean en) {
      if (!initialized) {
         refresh();
      }
      ModEntry m = entries.get(id);
      if (m != null) {
         if (en) {
            m.installed = true;
         }
         boolean changed = m.enabled != en;
         m.enabled = en;
         saveState();
         rebuildActiveModEffects();
         if (changed && m.instance != null) {
            try {
               if (en) {
                  m.instance.onEnable(activeGame);
               } else {
                  m.instance.onDisable(activeGame);
               }
            } catch (Throwable ignored) {
            }
         }
         if (changed && Net319.isHosting()) {
            Net319.sendModsSync();
         }
      }
   }

   private static Map<String, boolean[]> preSessionMods;

   public static synchronized String getActiveModsCsv() {
      if (!initialized) {
         init();
      }
      StringBuilder sb = new StringBuilder();
      for (ModEntry m : entries.values()) {
         if (m.installed && m.enabled) {
            if (sb.length() > 0) {
               sb.append(',');
            }
            sb.append(m.id);
         }
      }
      return sb.length() == 0 ? "NONE" : sb.toString();
   }

   public static synchronized void applySyncedModsCsv(String csv, Game g) {
      if (!initialized) {
         init();
      }
      if (g != null) {
         activeGame = g;
      }
      if (preSessionMods == null) {
         preSessionMods = new HashMap<>();
         for (ModEntry m : entries.values()) {
            preSessionMods.put(m.id, new boolean[]{m.installed, m.enabled});
         }
      }
      java.util.HashSet<String> want = new java.util.HashSet<>();
      if (csv != null && !csv.isEmpty() && !"NONE".equalsIgnoreCase(csv)) {
         for (String part : csv.split(",")) {
            String id = part.trim();
            if (!id.isEmpty()) {
               want.add(id);
            }
         }
      }
      boolean infdevChanged = false;
      boolean wasInfdev = isInfdevActive();
      for (ModEntry m : entries.values()) {
         boolean en = want.contains(m.id);
         if (en) {
            m.installed = true;
         }
         if (m.enabled != en) {
            m.enabled = en;
            if (m.instance != null) {
               try {
                  if (en) {
                     m.instance.onEnable(activeGame);
                  } else {
                     m.instance.onDisable(activeGame);
                  }
               } catch (Throwable ignored) {
               }
            }
         }
      }
      rebuildActiveModEffects();
      if (wasInfdev != isInfdevActive()) {
         infdevChanged = true;
      }
      if (infdevChanged && g != null && g.world != null) {
         g.world.gen = Dim.makeGen(g.world.dim, g.world.seed, g.world.settings);
         g.world.resetAllChunks();
         if (g.rend != null) {
            g.rend.setLod(g.world);
         }
      }
   }

   public static synchronized void restoreSessionMods(Game g) {
      if (preSessionMods == null) {
         return;
      }
      if (g != null) {
         activeGame = g;
      }
      Map<String, boolean[]> snap = preSessionMods;
      preSessionMods = null;
      for (ModEntry m : entries.values()) {
         boolean[] st = snap.get(m.id);
         if (st != null) {
            boolean wasEn = m.enabled;
            m.installed = st[0];
            m.enabled = st[1];
            if (wasEn != m.enabled && m.instance != null) {
               try {
                  if (m.enabled) {
                     m.instance.onEnable(activeGame);
                  } else {
                     m.instance.onDisable(activeGame);
                  }
               } catch (Throwable ignored) {
               }
            }
         }
      }
      rebuildActiveModEffects();
   }

   public static synchronized void toggleEnabled(String id) {
      if (!initialized) {
         refresh();
      }
      ModEntry m = entries.get(id);
      if (m != null && m.installed) {
         setEnabled(id, !m.enabled);
      }
   }

   // ------------------------------------------------------------------ runtime & total-conversion hooks

   public static void onGameTick(Game g) {
      activeGame = g;
      if (!initialized) {
         init();
      }
      for (ModEntry m : entries.values()) {
         if (m.installed && m.enabled && m.instance != null) {
            try {
               m.instance.onGameTick(g);
               if (g.world != null) {
                  m.instance.onWorldTick(g.world);
               }
            } catch (Throwable ignored) {
            }
         }
      }
   }

   public static boolean onCommand(Game g, String cmd, String[] args) {
      activeGame = g;
      if (!initialized) {
         init();
      }
      if (cmd.equals("/mods")) {
         int act = activeCount();
         StringBuilder sb = new StringBuilder("[Mods] " + act + " active: ");
         boolean first = true;
         for (ModEntry m : entries.values()) {
            if (m.installed && m.enabled) {
               if (!first) {
                  sb.append(", ");
               }
               sb.append(m.name);
               first = false;
            }
         }
         if (first) {
            sb.append("none (open Pause -> Mods to install/enable mods)");
         }
         g.chatMsg(sb.toString());
         return true;
      }
      if (cmd.equals("/infdev")) {
         ModEntry inf = entries.get("infdev-2010");
         if (inf != null) {
            boolean next = !inf.enabled;
            if (args.length >= 2) {
               next = args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("true") || args[1].equals("1");
            }
            if (next && !inf.installed) {
               install("infdev-2010");
            } else {
               setEnabled("infdev-2010", next);
            }
            g.chatMsg(
               "[Infdev 2010] Total Conversion is now "
                  + (
                     next
                        ? "ENABLED (applied Infdev 3D clouds, flat lighting, Classic textures & neon grass; create a new world for Infdev terrain & Brick Pyramids!)"
                        : "DISABLED (restored previous settings)"
                  )
            );
            return true;
         }
      }
      for (ModEntry m : entries.values()) {
         if (m.installed && m.enabled && m.instance != null) {
            try {
               if (m.instance.onCommand(g, cmd, args)) {
                  return true;
               }
            } catch (Throwable ignored) {
            }
         }
      }
      return false;
   }

   public static void onBlockBroken(World w, int x, int y, int z, int oldId) {
      if (!initialized) {
         init();
      }
      for (ModEntry m : entries.values()) {
         if (m.installed && m.enabled && m.instance != null) {
            try {
               m.instance.onBlockBroken(w, x, y, z, oldId);
            } catch (Throwable ignored) {
            }
         }
      }
   }

   public static Gen createModWorldGen(int dim, long seed, WorldSettings settings) {
      if (!initialized) {
         init();
      }
      for (ModEntry m : entries.values()) {
         if (m.installed && m.enabled && m.instance != null) {
            try {
               Gen g = m.instance.createWorldGen(dim, seed, settings);
               if (g != null) {
                  return g;
               }
            } catch (Throwable ignored) {
            }
         }
      }
      return null;
   }

   public static boolean isInfdevActive() {
      if (!initialized) {
         init();
      }
      for (ModEntry m : entries.values()) {
         if (m.installed && m.enabled && m.instance != null && m.instance.totalConversionInfdev()) {
            return true;
         }
      }
      return false;
   }

   public static boolean isClassicHudNoHunger() {
      if (!initialized) {
         init();
      }
      for (ModEntry m : entries.values()) {
         if (m.installed && m.enabled && m.instance != null && m.instance.classicHudNoHunger()) {
            return true;
         }
      }
      return false;
   }

   public static String hudWatermark() {
      if (!initialized) {
         init();
      }
      for (ModEntry m : entries.values()) {
         if (m.installed && m.enabled && m.instance != null) {
            String w = m.instance.hudWatermark();
            if (w != null && !w.isEmpty()) {
               return w;
            }
         }
      }
      return null;
   }

   public static boolean allowPaletteItem(int id) {
      if (!initialized) {
         init();
      }
      for (ModEntry m : entries.values()) {
         if (m.installed && m.enabled && m.instance != null && !m.instance.allowPaletteItem(id)) {
            return false;
         }
      }
      return true;
   }

   public static int[] filterPalette(int[] ids) {
      if (ids == null || !isInfdevActive()) {
         return ids;
      }
      int count = 0;
      for (int id : ids) {
         if (allowPaletteItem(id)) {
            count++;
         }
      }
      if (count == ids.length) {
         return ids;
      }
      int[] out = new int[count];
      int k = 0;
      for (int id : ids) {
         if (allowPaletteItem(id)) {
            out[k++] = id;
         }
      }
      return out;
   }

   public static boolean isWaystonesHudActive() {
      if (!initialized) {
         init();
      }
      ModEntry m = entries.get("waystones-hud");
      return m != null && m.installed && m.enabled;
   }

   private static int baseLogId(int id) {
      if (id >= Blocks.LOG_X0 && id < Blocks.LOG_X0 + 3) {
         return Blocks.LOG_OAK + (id - Blocks.LOG_X0);
      }
      if (id >= Blocks.LOG_Z0 && id < Blocks.LOG_Z0 + 3) {
         return Blocks.LOG_OAK + (id - Blocks.LOG_Z0);
      }
      if (id >= Blocks313.LOGX_JUNGLE && id < Blocks313.LOGX_JUNGLE + 3) {
         return Blocks313.LOG_JUNGLE + (id - Blocks313.LOGX_JUNGLE);
      }
      if (id >= Blocks313.LOGZ_JUNGLE && id < Blocks313.LOGZ_JUNGLE + 3) {
         return Blocks313.LOG_JUNGLE + (id - Blocks313.LOGZ_JUNGLE);
      }
      return isValidBlockId(id) ? id : Blocks.LOG_OAK;
   }

   private static int parseInt(String s, int def) {
      try {
         return Integer.parseInt(s.trim());
      } catch (NumberFormatException e) {
         return def;
      }
   }
}
