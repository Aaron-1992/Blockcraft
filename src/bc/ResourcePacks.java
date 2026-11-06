package bc;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.imageio.ImageIO;

/**
 * Resource Pack system for Blockcraft.
 * Supports built-in packs (including "Default" V4.0 Painted and "Classic" OG 3.5 textures)
 * as well as custom folder and .zip packs placed in the {@code resourcepacks/} directory.
 */
public final class ResourcePacks {
   public static final String ID_DEFAULT = "Default";
   public static final String ID_V59_RELIEF = "V59Relief";
   public static final String ID_V58_FUSION = "V58Fusion";
   public static final String ID_V57_PIXEL = "V57PixelArt";
   public static final String ID_V56_PAINTED = "V56Painted";
   public static final String ID_CLASSIC = "Classic";
   public static final String ID_VIBRANT = "Vibrant";
   public static final String ID_RETRO = "Retro8Bit";

   private static String activeId = ID_DEFAULT;
   private static Path customDir;
   private static final List<Pack> packs = new ArrayList<>();
   private static boolean initialized;

   public static final class Pack {
      public final String id;
      public final String title;
      public final String subtitle;
      public final boolean builtIn;
      public final boolean classicBase;
      public final Path path;
      public final int badgeColor;
      public final String badgeText;
      final Properties props = new Properties();
      final Map<Integer, int[]> tileOverrides = new HashMap<>();
      final Map<Integer, int[][]> animations = new HashMap<>();

      public Pack(String id, String title, String subtitle, boolean builtIn, boolean classicBase, Path path, int badgeColor) {
         this(id, title, subtitle, builtIn, classicBase, path, badgeColor, classicBase ? "3.5" : "5.9");
      }

      public Pack(String id, String title, String subtitle, boolean builtIn, boolean classicBase, Path path, int badgeColor, String badgeText) {
         this.id = id;
         this.title = title;
         this.subtitle = subtitle;
         this.builtIn = builtIn;
         this.classicBase = classicBase;
         this.path = path;
         this.badgeColor = badgeColor;
         this.badgeText = badgeText;
      }
   }

   private ResourcePacks() {
   }

   public static synchronized Path dir() {
      if (customDir != null) {
         return customDir;
      }
      Path d = new File("resourcepacks").toPath();
      try {
         Files.createDirectories(d);
         seedExampleReadme(d);
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

   private static void seedExampleReadme(Path dir) {
      Path readme = dir.resolve("README.txt");
      if (!Files.exists(readme)) {
         String txt = String.join(
            System.lineSeparator(),
            "Blockcraft Resource Packs",
            "=========================",
            "Place custom resource pack folders or .zip archives in this directory.",
            "",
            "Supported files inside a pack folder or .zip:",
            "  - pack.properties      : name, description, base=Default|Classic, saturation, contrast",
            "  - atlas.png            : legacy 16px atlas; tileSize=32 in pack.properties enables native 1024x1024 atlas and Default base",
            "  - tiles/<id>.png       : 16x16 or 32x32 PNG override for tile index <id> (0..1023)",
            "  - tiles/<block>.png    : PNG override by legacy alias; use exported numeric_name.png for unambiguous IDs"
         );
         try {
            Files.writeString(readme, txt, StandardCharsets.UTF_8);
         } catch (IOException ignored) {
         }
      }
   }

   public static synchronized void refresh() {
      packs.clear();
      packs.add(new Pack(
         ID_DEFAULT,
         "Natural Pixel 32 (Default)",
         "32x32 crisp pixel art, natural materials, no texture blur",
         true,
         false,
         null,
         0xFF2E7D32,
         "32px"
      ));
      packs.add(new Pack(
         ID_V59_RELIEF,
         "Blockcraft V5.9 (5.6 x MC Relief)",
         "3D micro-relief 5.6 x Minecraft fusion textures",
         true,
         false,
         null,
         0xFF388E3C,
         "5.9"
      ));
      packs.add(new Pack(
         ID_V58_FUSION,
         "Blockcraft V5.8 (5.6 x MC Fusion)",
         "5.6 procedural painted textures blended with classic Minecraft pixel art",
         true,
         false,
         null,
         0xFF00897B,
         "5.8"
      ));
      packs.add(new Pack(
         ID_V57_PIXEL,
         "Blockcraft V5.7 (Bold Pixel Art)",
         "High-contrast 5-tone pixel-art overhaul with bold dark item outlines",
         true,
         false,
         null,
         0xFF1E88E5,
         "5.7"
      ));
      packs.add(new Pack(
         ID_V56_PAINTED,
         "Blockcraft V5.6 / V4.0 (Painted)",
         "Original Update 3.18-5.6 smooth FBM/Voronoi procedural painted art",
         true,
         false,
         null,
         0xFF4CAF50,
         "5.6"
      ));
      packs.add(new Pack(
         ID_CLASSIC,
         "OG Classic 3.5",
         "Original nostalgic Blockcraft 3.5 procedural pixel art",
         true,
         true,
         null,
         0xFF8D6E63,
         "3.5"
      ));
      packs.add(new Pack(
         ID_VIBRANT,
         "Vibrant Sunlit",
         "Warm golden-hour saturation & crisp contrast boost",
         true,
         false,
         null,
         0xFFFFB300,
         "SUN"
      ));
      packs.add(new Pack(
         ID_RETRO,
         "Retro 8-Bit",
         "Classic quantized retro console colour palette",
         true,
         true,
         null,
         0xFF5C6BC0,
         "8BT"
      ));

      Path d = dir();
      if (Files.isDirectory(d)) {
         List<Path> entries = new ArrayList<>();
         try (DirectoryStream<Path> ds = Files.newDirectoryStream(d)) {
            for (Path p : ds) {
               entries.add(p);
            }
         } catch (IOException ignored) {
         }
         entries.sort(Comparator.comparing(p -> p.getFileName().toString().toLowerCase(Locale.ROOT)));
         for (Path p : entries) {
            String fn = p.getFileName().toString();
            if (Files.isDirectory(p)) {
               Pack loaded = loadFolderPack(p);
               if (loaded != null) {
                  packs.add(loaded);
               }
            } else if (fn.toLowerCase(Locale.ROOT).endsWith(".zip")) {
               Pack loaded = loadZipPack(p);
               if (loaded != null) {
                  packs.add(loaded);
               }
            }
         }
      }
      initialized = true;
   }

   public static synchronized List<Pack> list() {
      if (!initialized) {
         refresh();
      }
      return Collections.unmodifiableList(new ArrayList<>(packs));
   }

   public static synchronized Pack active() {
      if (!initialized) {
         refresh();
      }
      for (Pack p : packs) {
         if (p.id.equalsIgnoreCase(activeId)) {
            return p;
         }
      }
      return packs.get(0);
   }

   public static synchronized String activeId() {
      return active().id;
   }

   public static synchronized boolean isClassicBase() {
      if (ID_DEFAULT.equals(activeId)) {
         return false;
      }
      if (ID_CLASSIC.equalsIgnoreCase(activeId) || ID_RETRO.equalsIgnoreCase(activeId)) {
         return true;
      }
      return active().classicBase;
   }

   /**
    * Returns the active texture version mode:
    * 0 = V5.9 Realistic (Default / Vibrant),
    * 1 = V5.8 5.6 x Minecraft Classic Fusion,
    * 2 = V5.7 Bold 5-Tone Pixel Art,
    * 3 = V5.6 / V4.0 Procedural Painted,
    * 4 = OG Classic 3.5.
    */
   public static synchronized int textureVersionMode() {
      if (isClassicBase()) {
         return 4;
      }
      if (ID_V56_PAINTED.equalsIgnoreCase(activeId)) {
         return 3;
      }
      if (ID_V57_PIXEL.equalsIgnoreCase(activeId)) {
         return 2;
      }
      if (ID_V58_FUSION.equalsIgnoreCase(activeId)) {
         return 1;
      }
      if (ID_V59_RELIEF.equalsIgnoreCase(activeId)) {
         return 5;
      }
      Pack p = active();
      String ver = p.props.getProperty("version", "").trim();
      if ("5.6".equals(ver) || "4.0".equals(ver)) return 3;
      if ("5.7".equals(ver)) return 2;
      if ("5.8".equals(ver)) return 1;
      if ("5.9".equals(ver)) return 5;
      return 0;
   }

   /** Sets the active pack ID without rebuilding GL/world state (used during Options.load before Tex.init). */
   public static synchronized void setActiveId(String id) {
      if (id == null || id.isBlank()) {
         activeId = ID_DEFAULT;
      } else {
         activeId = id.trim();
      }
   }

   /** Selects a resource pack, rebuilds the texture atlas, and refreshes chunk & LOD meshes if in-game. */
   public static synchronized void select(String id, Game g) {
      setActiveId(id);
      if (!initialized) {
         refresh();
      }
      activeId = active().id;
      Tex.reloadPixels();
      BowSprite.invalidate();
      LodMap.resetAverages();
      Renderer.clearItemGeo();
      if (g != null) {
         if (g.opt != null) {
            g.opt.resourcePack = activeId;
            g.opt.save();
         }
         g.remeshAll();
         if (g.rend != null && g.rend.lod != null) {
            g.rend.lod.invalidateAll();
         }
      }
   }

   public static synchronized void reload(Game g) {
      Tilesheet.invalidate();   // V6.25: pick up a newly saved tilesheet.png
      String selected = activeId();
      refresh();
      select(selected, g);
   }

   /** Called at the end of {@link Tex#initPixels()} to apply pack-specific colour grading and PNG tile overrides. */
   public static synchronized void applyPostProcess(int[] pix) {
      if (ID_DEFAULT.equals(activeId) || ID_CLASSIC.equalsIgnoreCase(activeId)) {
         return;
      }
      Pack p = active();
      if (ID_VIBRANT.equalsIgnoreCase(p.id)) {
         gradePixels(pix, 1.24F, 1.07F, 1.03F, 1.01F, 0.96F, false);
         return;
      }
      if (ID_RETRO.equalsIgnoreCase(p.id)) {
         gradePixels(pix, 1.15F, 1.12F, 1.0F, 1.0F, 1.0F, true);
         return;
      }

      float sat = parseFloat(p.props.getProperty("saturation"), 1.0F);
      float con = parseFloat(p.props.getProperty("contrast"), 1.0F);
      float rMul = parseFloat(p.props.getProperty("tint.r"), 1.0F);
      float gMul = parseFloat(p.props.getProperty("tint.g"), 1.0F);
      float bMul = parseFloat(p.props.getProperty("tint.b"), 1.0F);
      boolean retro = Boolean.parseBoolean(p.props.getProperty("retro", "false"));
      if (sat != 1.0F || con != 1.0F || rMul != 1.0F || gMul != 1.0F || bMul != 1.0F || retro) {
         gradePixels(pix, sat, con, rMul, gMul, bMul, retro);
      }

      for (int tile = 0; tile < Tex.COLS * Tex.COLS; tile++) {
         int[] px = overridePixels(tile, 0, 16);
         if (px != null) writeTile(pix, tile, px);
      }
   }

   /** Opt-in: old packs keep their original 16px rendering. Missing native cells inherit Default. */
   static boolean native32() {
      return ID_DEFAULT.equals(activeId) || "32".equals(active().props.getProperty("tileSize", "").trim());
   }

   static int[] resize(int[] pixels, int size) {
      int from = (int)Math.sqrt(pixels.length);
      if (from == size) return pixels;
      int[] out = new int[size * size];
      for (int y = 0; y < size; y++) for (int x = 0; x < size; x++)
         out[y * size + x] = pixels[(y * from / size) * from + x * from / size];
      return out;
   }

   /** Static PNGs take precedence over animation strips, even if completely transparent. */
   static synchronized int[] overridePixels(int tile, int frame, int size) {
      if (ID_DEFAULT.equals(activeId)) return null;
      Pack p = active();
      int[] px = p.tileOverrides.get(tile);
      if (px == null) {
         int[][] frames = p.animations.get(tile);
         if (frames != null) px = frames[Math.floorMod(frame, frames.length)];
      }
      return px == null ? null : resize(px, size);
   }

   private static void readAnimations(Pack pack, String name, InputStream in) throws IOException {
      int tile = resolveTileName(name);
      if (tile < 0) return;
      if (!TextureEditor.animated(tile)) throw new IOException("This tile has no animation: " + name);
      BufferedImage img = ImageIO.read(in);
      int frames = tile == 77 ? 8 : 16;
      if (img == null || img.getHeight() != 32 || img.getWidth() != 32 * frames)
         throw new IOException("Expected " + (32 * frames) + "x32 animation PNG: " + name);
      int[][] pixels = new int[frames][1024];
      for (int i = 0; i < frames; i++) img.getRGB(i * 32, 0, 32, 32, pixels[i], 0, 32);
      pack.animations.put(tile, pixels);
   }

   private static void writeTile(int[] pix, int tile, int[] tpx) {
      int tx = (tile & 31) * 16;
      int ty = (tile >> 5) * 16;
      for (int y = 0; y < 16; y++) {
         int row = (ty + y) * 512 + tx;
         System.arraycopy(tpx, y * 16, pix, row, 16);
      }
   }

   private static void gradePixels(int[] pix, float sat, float contrast, float rMul, float gMul, float bMul, boolean quantize) {
      for (int i = 0; i < pix.length; i++) {
         int c = pix[i];
         int a = c >>> 24;
         if (a == 0) {
            continue;
         }
         float r = (c >> 16 & 0xFF) / 255.0F;
         float g = (c >> 8 & 0xFF) / 255.0F;
         float b = (c & 0xFF) / 255.0F;
         float lum = 0.299F * r + 0.587F * g + 0.114F * b;
         r = lum + (r - lum) * sat;
         g = lum + (g - lum) * sat;
         b = lum + (b - lum) * sat;
         r = (r - 0.5F) * contrast + 0.5F;
         g = (g - 0.5F) * contrast + 0.5F;
         b = (b - 0.5F) * contrast + 0.5F;
         r *= rMul;
         g *= gMul;
         b *= bMul;
         int ri = Math.max(0, Math.min(255, Math.round(r * 255.0F)));
         int gi = Math.max(0, Math.min(255, Math.round(g * 255.0F)));
         int bi = Math.max(0, Math.min(255, Math.round(b * 255.0F)));
         if (quantize) {
            ri = (ri + 16) / 36 * 36;
            gi = (gi + 16) / 36 * 36;
            bi = (bi + 16) / 42 * 42;
            ri = Math.min(255, ri);
            gi = Math.min(255, gi);
            bi = Math.min(255, bi);
         }
         pix[i] = a << 24 | ri << 16 | gi << 8 | bi;
      }
   }

   private static float parseFloat(String s, float def) {
      if (s == null || s.isBlank()) {
         return def;
      }
      try {
         return Float.parseFloat(s.trim());
      } catch (NumberFormatException e) {
         return def;
      }
   }

   private static Pack loadFolderPack(Path folder) {
      String id = folder.getFileName().toString();
      Properties props = new Properties();
      Path propFile = folder.resolve("pack.properties");
      if (Files.isRegularFile(propFile)) {
         try (InputStream in = Files.newInputStream(propFile)) {
            props.load(in);
         } catch (IOException ignored) {
         }
      }
      String title = props.getProperty("name", id.replace('_', ' '));
      String desc = props.getProperty("description", "Custom folder resource pack (" + id + ")");
      boolean classic = "classic".equalsIgnoreCase(props.getProperty("base", "default").trim());
      Pack pack = new Pack(id, title, desc, false, classic, folder, 0xFF26A69A, "32".equals(props.getProperty("tileSize")) ? "32px" : "16px");
      pack.props.putAll(props);

      Path atlasFile = folder.resolve("atlas.png");
      if (Files.isRegularFile(atlasFile)) {
         try (InputStream in = Files.newInputStream(atlasFile)) {
            loadAtlasOverrides(pack, in);
         } catch (IOException ignored) {
         }
      }

      Path animDir = folder.resolve("animations");
      if (Files.isDirectory(animDir)) {
         try (DirectoryStream<Path> ds = Files.newDirectoryStream(animDir, "*.png")) {
            for (Path file : ds) try (InputStream in = Files.newInputStream(file)) {
               readAnimations(pack, file.getFileName().toString(), in);
            } catch (IOException e) { System.err.println("[packs] " + file + ": " + e.getMessage()); }
         } catch (IOException e) { System.err.println("[packs] " + e.getMessage()); }
      }
      Path tilesDir = folder.resolve("tiles");
      if (Files.isDirectory(tilesDir)) {
         try (DirectoryStream<Path> ds = Files.newDirectoryStream(tilesDir, "*.png")) {
            for (Path tp : ds) {
               int tileIdx = resolveTileName(tp.getFileName().toString());
               if (tileIdx >= 0) {
                  try (InputStream in = Files.newInputStream(tp)) {
                     int[] px = readTile(in);
                     if (px != null) {
                        pack.tileOverrides.put(tileIdx, px);
                     }
                  } catch (IOException e) { System.err.println("[packs] " + tp + ": " + e.getMessage()); }
               }
            }
         } catch (IOException ignored) {
         }
      }
      return pack;
   }

   private static Pack loadZipPack(Path zipPath) {
      String fn = zipPath.getFileName().toString();
      String id = fn.substring(0, fn.length() - 4);
      Properties props = new Properties();
      Map<Integer, int[]> overrides = new HashMap<>();
      byte[] atlasBytes = null;
      Map<String, byte[]> animationBytes = new HashMap<>();

      try (ZipFile zf = new ZipFile(zipPath.toFile())) {
         ZipEntry pe = zf.getEntry("pack.properties");
         if (pe != null) {
            try (InputStream in = zf.getInputStream(pe)) {
               props.load(new StringReader(new String(in.readAllBytes(), StandardCharsets.UTF_8)));
            }
         }
         ZipEntry ae = zf.getEntry("atlas.png");
         if (ae != null) {
            try (InputStream in = zf.getInputStream(ae)) {
               atlasBytes = in.readAllBytes();
            }
         }
         var en = zf.entries();
         while (en.hasMoreElements()) {
            ZipEntry ze = en.nextElement();
            if (ze.isDirectory()) {
               continue;
            }
            String name = ze.getName().replace('\\', '/');
            if (name.startsWith("animations/") && name.endsWith(".png")) {
               try (InputStream in = zf.getInputStream(ze)) { animationBytes.put(name.substring(11), in.readAllBytes()); }
            }
            int slash = name.lastIndexOf('/');
            String baseName = slash >= 0 ? name.substring(slash + 1) : name;
            if ((name.startsWith("tiles/") || name.startsWith("textures/")) && baseName.toLowerCase(Locale.ROOT).endsWith(".png")) {
               int tileIdx = resolveTileName(baseName);
               if (tileIdx >= 0) {
                  try (InputStream in = zf.getInputStream(ze)) {
                     int[] px = readTile(in);
                     if (px != null) {
                        overrides.put(tileIdx, px);
                     }
                  } catch (IOException e) { System.err.println("[packs] " + fn + "/" + name + ": " + e.getMessage()); }
               }
            }
         }
      } catch (IOException e) {
         return null;
      }

      String title = props.getProperty("name", id.replace('_', ' '));
      String desc = props.getProperty("description", "Custom zip resource pack (" + fn + ")");
      boolean classic = "classic".equalsIgnoreCase(props.getProperty("base", "default").trim());
      Pack pack = new Pack(id, title, desc, false, classic, zipPath, 0xFFAB47BC, "32".equals(props.getProperty("tileSize")) ? "32px" : "16px");
      pack.props.putAll(props);
      if (atlasBytes != null) {
         loadAtlasOverrides(pack, new ByteArrayInputStream(atlasBytes));
      }
      pack.tileOverrides.putAll(overrides);
      for (Map.Entry<String, byte[]> e : animationBytes.entrySet()) {
         try { readAnimations(pack, e.getKey(), new ByteArrayInputStream(e.getValue())); }
         catch (IOException ex) { System.err.println("[packs] " + fn + ": " + ex.getMessage()); }
      }
      return pack;
   }

   private static void loadAtlasOverrides(Pack pack, InputStream in) {
      try {
         BufferedImage img = ImageIO.read(in);
         if (img == null) {
            return;
         }
         int size = "32".equals(pack.props.getProperty("tileSize")) ? 32 : 16;
         int cols = img.getWidth() / size;
         int rows = img.getHeight() / size;
         if ((cols != 16 && cols != 32) || rows > 64 || img.getWidth() % size != 0 || img.getHeight() % size != 0) return;
         for (int ty = 0; ty < rows; ty++) {
            for (int tx = 0; tx < cols; tx++) {
               int tile = cols == 16 ? (ty * 16 + tx) : (ty * 32 + tx);
               if (tile < 0 || tile >= Tex.COLS * Tex.COLS) {
                  continue;
               }
               int[] cell = new int[size * size];
               img.getRGB(tx * size, ty * size, size, size, cell, 0, size);
               boolean nonEmpty = false;
               for (int v : cell) {
                  if ((v >>> 24) != 0) {
                     nonEmpty = true;
                     break;
                  }
               }
               if (nonEmpty) {
                  pack.tileOverrides.put(tile, cell);
               }
            }
         }
      } catch (IOException ignored) {
      }
   }

   private static int[] readTile(InputStream in) throws IOException {
      BufferedImage img = ImageIO.read(in);
      if (img == null || img.getWidth() != img.getHeight() || (img.getWidth() != 16 && img.getWidth() != 32))
         throw new IOException("Tile PNG must be exactly 16x16 or 32x32 (not cropped/scaled).");
      int size = img.getWidth();
      return img.getRGB(0, 0, size, size, null, 0, size);
   }

   static int resolveTileName(String fileName) {
      String stem = fileName.endsWith(".png") || fileName.endsWith(".PNG")
         ? fileName.substring(0, fileName.length() - 4).trim().toLowerCase(Locale.ROOT)
         : fileName.trim().toLowerCase(Locale.ROOT);
      try {
         int idx = Integer.parseInt(stem.split("_", 2)[0]);
         return idx >= 0 && idx < Tex.COLS * Tex.COLS ? idx : -1;
      } catch (NumberFormatException ignored) {
      }
      switch (stem) {
         case "grass_top":
            return 0;
         case "stone":
            return 1;
         case "dirt":
            return 2;
         case "grass_side":
            return 3;
         case "oak_planks":
         case "planks":
            return 4;
         case "cobblestone":
            return 16;
         case "bedrock":
            return 17;
         case "sand":
            return 18;
         case "gravel":
            return 19;
         case "oak_log":
         case "log_oak":
            return 20;
         case "oak_log_top":
            return 21;
         case "brick":
         case "bricks":
            return 7;
         case "tnt_side":
            return 8;
         case "gold_block":
            return 23;
         case "iron_block":
            return 22;
         case "diamond_block":
            return 24;
         case "glass":
            return 49;
         case "obsidian":
            return 37;
         default:
            return -1;
      }
   }
}
