package bc;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.imageio.ImageIO;

/**
 * V6.25: one editable PNG containing EVERY texture in the game.
 *
 * <p>The sheet is a 1024x1024 RGBA image laid out as 32x32 cells of 32x32 pixels - exactly
 * the game's native render atlas. Cell 0 is top-left; cell {@code row * 32 + col} is tile
 * {@code row * 32 + col}. Edit it in Aseprite / Photoshop / any pixel editor, keep the
 * canvas 1024x1024 and the alpha, save as PNG, then type {@code /tilesheet} in game (or
 * Options > Resource Packs > Reload Tilesheet). No Java, Python or compiling needed.
 *
 * <p>Only cells that DIFFER from the built-in art are applied, so an untouched sheet is a
 * complete no-op and cannot change the default game. Animated tiles (water, lava, portal,
 * fire, ... ) are listed in the index but intentionally left alone so the animations keep
 * running; edit those through the animation strips instead.
 */
public final class Tilesheet {
   public static final int CELL = 32;
   public static final int COLS = 32;
   public static final int SIZE = CELL * COLS;   // 1024
   public static final int TILES = COLS * COLS;  // 1024
   public static final String FILE = "tilesheet.png";

   private static int[] sheet;
   private static long stamp = Long.MIN_VALUE;
   private static long length = -1L;
   private static boolean suppress;

   private Tilesheet() {
   }

   /** The sheet lives next to Blockcraft.exe (the game's working directory). */
   public static File file() {
      return new File(FILE);
   }

   public static synchronized void invalidate() {
      sheet = null;
      stamp = Long.MIN_VALUE;
      length = -1L;
   }

   /** Returns the ARGB pixels of a valid sheet, or null when there is none / it is malformed. */
   static synchronized int[] load() {
      if (suppress) return null;   // export() must reproduce the pristine default art
      File f = file();
      if (!f.isFile()) {
         sheet = null;
         return null;
      }
      long lm = f.lastModified();
      long ln = f.length();
      if (sheet != null && lm == stamp && ln == length) return sheet;
      try {
         BufferedImage img = ImageIO.read(f);
         if (img == null || img.getWidth() != SIZE || img.getHeight() != SIZE) {
            sheet = null;
            return null;
         }
         sheet = img.getRGB(0, 0, SIZE, SIZE, null, 0, SIZE);
         stamp = lm;
         length = ln;
         return sheet;
      } catch (IOException e) {
         sheet = null;
         return null;
      }
   }

   private static boolean editable(int t) {
      return !TextureEditor.animated(t) && t != Tex.WHITE;
   }

   private static boolean differs(int[] s, int[] ref, int tx, int ty) {
      for (int y = 0; y < CELL; y++) {
         int row = (ty + y) * SIZE + tx;
         for (int x = 0; x < CELL; x++) {
            if (s[row + x] != ref[row + x]) return true;
         }
      }
      return false;
   }

   /** Overlay the edited cells onto the final 32px render atlas, at full resolution. */
   public static synchronized void apply(int[] render) {
      int[] s = load();
      if (s == null || render == null || render.length != SIZE * SIZE) return;
      for (int t = 0; t < TILES; t++) {
         if (!editable(t)) continue;
         int tx = t % COLS * CELL;
         int ty = t / COLS * CELL;
         if (!differs(s, render, tx, ty)) continue;
         for (int y = 0; y < CELL; y++) {
            System.arraycopy(s, (ty + y) * SIZE + tx, render, (ty + y) * SIZE + tx, CELL);
         }
      }
   }

   /** Keep the CPU-side 16px atlas (LOD colours, held items) in sync with edited art. */
   public static synchronized void applyLow(int[] low) {
      int[] s = load();
      if (s == null || low == null) return;
      int[] ref = TexPixel32.atlas(low);
      int px = Tex.PX;
      for (int t = 0; t < TILES; t++) {
         if (!editable(t)) continue;
         int sx = t % COLS * CELL, sy = t / COLS * CELL;
         if (!differs(s, ref, sx, sy)) continue;
         int dx = t % COLS * 16, dy = t / COLS * 16;
         for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
               low[(dy + y) * px + dx + x] = s[(sy + y * 2) * SIZE + sx + x * 2];
            }
         }
      }
   }

   /** Install an edited 1024x1024 PNG as the live tilesheet. */
   public static boolean install(File src) throws IOException {
      BufferedImage img = ImageIO.read(src);
      if (img == null || img.getWidth() != SIZE || img.getHeight() != SIZE) {
         throw new IOException("A Blockcraft tilesheet must be exactly 1024x1024 PNG (got "
            + (img == null ? "not a PNG" : img.getWidth() + "x" + img.getHeight()) + ")");
      }
      Files.copy(src.toPath(), file().toPath(), StandardCopyOption.REPLACE_EXISTING);
      invalidate();
      return true;
   }

   public static int changedTiles() {
      int[] s = load();
      if (s == null) return 0;
      int[] ref = TexPixel32.atlas(Tex.atlasPixels);
      int n = 0;
      for (int t = 0; t < TILES; t++) {
         if (editable(t) && differs(s, ref, t % COLS * CELL, t / COLS * CELL)) n++;
      }
      return n;
   }

   // ------------------------------------------------------------------ generator (offline / in-game)
   static Map<Integer, String> names() throws IOException {
      Map<Integer, String> out = new HashMap<>();
      try (InputStream in = Tilesheet.class.getResourceAsStream("/texture-names.tsv")) {
         if (in == null) return out;
         try (BufferedReader r = new BufferedReader(new InputStreamReader(Objects.requireNonNull(in), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
               String[] c = line.split("\t");
               if (c.length >= 2) {
                  try { out.put(Integer.parseInt(c[0].trim()), c[1].trim()); } catch (NumberFormatException ignored) { }
               }
            }
         }
      }
      return out;
   }

   /** Write the master PNG, the TSV index, a searchable HTML index and a numbered reference map. */
   public static void export(File dir) throws Exception {
      suppress = true;
      try {
         exportClean(dir);
      } finally {
         suppress = false;
         invalidate();
      }
   }

   private static void exportClean(File dir) throws Exception {
      ResourcePacks.select(ResourcePacks.ID_DEFAULT, null);
      int[] atlas = TexPixel32.atlas(Tex.atlasPixels);
      BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
      img.setRGB(0, 0, SIZE, SIZE, atlas, 0, SIZE);
      if (!dir.isDirectory() && !dir.mkdirs()) throw new IOException("Cannot create " + dir);
      ImageIO.write(img, "png", new File(dir, "Blockcraft-tilesheet.png"));
      Map<Integer, String> names = names();

      StringBuilder tsv = new StringBuilder("tile\trow\tcol\tname\ttype\tcell_pixels\n");
      StringBuilder html = new StringBuilder("<!doctype html><meta charset=\"utf-8\"><title>Blockcraft tilesheet</title>"
         + "<style>body{font:15px system-ui;background:#10171d;color:#e8eef2;margin:24px}"
         + "input{width:100%;padding:12px;margin:12px 0;font-size:16px}#g{display:grid;grid-template-columns:repeat(auto-fill,minmax(150px,1fr));gap:8px}"
         + "article{background:#1c2832;padding:8px;border-radius:4px;overflow-wrap:anywhere}"
         + ".t{width:64px;height:64px;image-rendering:pixelated;background:#000;display:block}"
         + "b{color:#8fd0ff}small{color:#9fb0bc}</style>"
         + "<h1>Blockcraft V" + Game.VERSION_STR + " - all 1024 textures in ONE PNG</h1>"
         + "<p>Edit <b>tilesheet.png</b> (1024x1024). Cell <b>row*32+col</b> = tile id. "
         + "Keep the canvas size and alpha. Then type <b>/tilesheet</b> in game or press "
         + "<b>Reload Tilesheet</b> in Options &gt; Resource Packs. Animated cells are marked "
         + "and are left to the animation strips.</p>"
         + "<input id=q placeholder=\"Search tile id or name...\"><div id=g>");
      for (int t = 0; t < TILES; t++) {
         String name = names.getOrDefault(t, "Model face / shared material");
         boolean anim = TextureEditor.animated(t);
         int row = t / COLS, col = t % COLS;
         tsv.append(t).append('\t').append(row).append('\t').append(col).append('\t')
            .append(name).append('\t').append(anim ? "animated (not overridden)" : "editable")
            .append("\t").append(col * CELL).append(',').append(row * CELL).append(",32,32\n");
         String bg = "-" + col * CELL + "px -" + row * CELL + "px";
         html.append("<article data-s=\"").append(t).append(' ').append(esc(name).toLowerCase())
            .append("\"><div class=t style=\"background-image:url(Blockcraft-tilesheet.png);")
            .append("background-size:1024px 1024px;background-position:").append(bg).append("\"></div>")
            .append("<b>").append(t).append("</b> <small>r").append(row).append(" c").append(col)
            .append("</small><br>").append(esc(name))
            .append(anim ? "<br><small>ANIMATED - not overridden</small>" : "").append("</article>");
      }
      html.append("</div><script>document.getElementById('q').addEventListener('input',e=>{"
         + "const q=e.target.value.toLowerCase();document.querySelectorAll('#g article').forEach(a=>"
         + "a.style.display=a.dataset.s.includes(q)?'':'none')});</script>");
      Files.writeString(new File(dir, "TILESHEET.tsv").toPath(), tsv.toString(), StandardCharsets.UTF_8);
      Files.writeString(new File(dir, "tilesheet-index.html").toPath(), html.toString(), StandardCharsets.UTF_8);

      // A 2x reference map with ids: helpful in Aseprite as a separate layer / second tab.
      BufferedImage map = new BufferedImage(SIZE * 2, SIZE * 2, BufferedImage.TYPE_INT_ARGB);
      Graphics2D g = map.createGraphics();
      g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
      g.setFont(new Font("SansSerif", Font.PLAIN, 11));
      for (int row = 0; row < COLS; row++) {
         for (int col = 0; col < COLS; col++) {
            int t = row * COLS + col;
            int x = col * CELL * 2, y = row * CELL * 2;
            g.drawImage(img.getSubimage(col * CELL, row * CELL, CELL, CELL), x, y, CELL * 2, CELL * 2, null);
            g.setColor(new Color(0, 0, 0, 150));
            g.fillRect(x, y, 34, 14);
            g.setColor(Color.WHITE);
            g.drawString(Integer.toString(t), x + 2, y + 11);
            g.setColor(new Color(255, 255, 255, 60));
            g.drawRect(x, y, CELL * 2, CELL * 2);
         }
      }
      g.dispose();
      ImageIO.write(map, "png", new File(dir, "tilesheet-map.png"));
      System.out.println("[tilesheet] wrote " + new File(dir, "Blockcraft-tilesheet.png").getAbsolutePath()
         + " (" + TILES + " tiles) + TILESHEET.tsv + tilesheet-index.html + tilesheet-map.png");
   }

   private static String esc(String s) {
      return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
   }

   /** CLI: export [dir] | import <png> | check <png> */
   public static void main(String[] args) throws Exception {
      String cmd = args.length == 0 ? "export" : args[0];
      if ("export".equals(cmd)) {
         export(new File(args.length > 1 ? args[1] : "textures"));
      } else if ("import".equals(cmd)) {
         if (args.length < 2) throw new IllegalArgumentException("Usage: import <edited.png>");
         install(new File(args[1]));
         System.out.println("[tilesheet] installed " + args[1] + " -> " + file().getAbsolutePath());
      } else if ("check".equals(cmd)) {
         if (args.length < 2) throw new IllegalArgumentException("Usage: check <png>");
         BufferedImage img = ImageIO.read(new File(args[1]));
         if (img == null || img.getWidth() != SIZE || img.getHeight() != SIZE) {
            throw new IOException("Not a 1024x1024 PNG tilesheet");
         }
         System.out.println("[tilesheet] OK " + args[1] + " " + img.getWidth() + "x" + img.getHeight());
      } else {
         throw new IllegalArgumentException("Unknown command: " + cmd);
      }
   }
}
