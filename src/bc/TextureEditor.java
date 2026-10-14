package bc;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;

/** Headless, lossless export of the actual default render pixels. Never overwrites an artist's pack. */
public final class TextureEditor {
   private TextureEditor() {}
   static final int[] ANIMATED = {23, 24, 40, 77, 235, TexArt312.PORTAL};

   static boolean animated(int tile) {
      for (int t : ANIMATED) if (t == tile) return true;
      return false;
   }

   static int[] frame(int tile, int frame) {
      if (tile == 77) return BowSprite.pixels(frame % 4, frame >= 4);
      if (tile == 40) return TexUtility32.furnace(true, frame);
      return TexRemaining32.frame(tile, frame);
   }

   public static void export(Path target) throws Exception {
      target = target.toAbsolutePath();
      Files.createDirectories(target.getParent());
      Files.createDirectory(target); // Atomic refusal if the path already exists, even an empty folder.
      String previous = ResourcePacks.activeId();
      try {
         ResourcePacks.select(ResourcePacks.ID_DEFAULT, null);
         Files.createDirectory(target.resolve("tiles"));
         Files.createDirectory(target.resolve("animations"));
         Files.writeString(target.resolve("pack.properties"), "name=My Aseprite Textures\ndescription=Editable V" + Game.VERSION_STR
            + " native PNGs - save in Aseprite, then Reload PNGs\nbase=Default\ntileSize=32\n", StandardCharsets.UTF_8);
         try (InputStream in = TextureEditor.class.getResourceAsStream("/texture-editing.txt")) {
            if (in == null) throw new IOException("Missing texture editing instructions in game JAR");
            Files.copy(in, target.resolve("START-HERE.txt"));
         }
         Map<Integer, String> names = new HashMap<>();
         try (BufferedReader r = new BufferedReader(new InputStreamReader(
               Objects.requireNonNull(TextureEditor.class.getResourceAsStream("/texture-names.tsv")), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
               String[] columns = line.split("\t");
               names.put(Integer.parseInt(columns[0]), columns[1]);
            }
         }
         Map<Integer, Set<String>> uses = new HashMap<>();
         String[] faces = {"top", "bottom", "north", "south", "west", "east"};
         for (int id = 1; id < Blocks.MAX; id++) if (Blocks.name[id] != null && Blocks.tiles[id] != null) {
            for (int f = 0; f < 6; f++) use(uses, Blocks.tiles[id][f], Blocks.name[id] + " / " + faces[f]);
            double[][] boxes = Shapes.modelBoxes(id);
            if (boxes != null) for (double[] box : boxes) for (int f = 8; f <= 10 && f < box.length; f++)
               use(uses, (int)box[f], Blocks.name[id] + " / model component");
         }
         for (int id = 256; id < Items.name.length; id++) if (Items.name[id] != null)
            use(uses, Items.tile[id], Items.name[id] + " / item");
         StringBuilder html = new StringBuilder("<!doctype html><meta charset=\"utf-8\"><title>Blockcraft texture index</title>"
            + "<style>body{font:16px system-ui;background:#18231f;color:#eee;margin:32px}a{color:#a6dfad}input{padding:12px;width:90%;margin:16px 0}"
            + "main{display:grid;grid-template-columns:repeat(auto-fill,minmax(240px,1fr));gap:12px}article{background:#293830;padding:16px;overflow-wrap:anywhere}"
            + "img{image-rendering:pixelated;max-width:100%;height:auto;min-height:32px;background:repeating-conic-gradient(#666 0% 25%,#444 0% 50%) 0/16px 16px}"
            + "small{color:#bdcbbf}details{margin-top:10px}</style><h1>Blockcraft V" + Game.VERSION_STR + " / Aseprite textures</h1>"
            + "<p>Open a PNG in Aseprite, save/export as PNG, then Options &gt; Resource Packs &gt; Reload PNGs. "
            + "<a href=\"START-HERE.txt\">Read editing guide</a>. Filenames begin with the actual atlas tile ID, not a block ID.</p>"
            + "<input id=\"search\" placeholder=\"Filter by name, tile ID, block or item...\" aria-label=\"Search textures\"><main>");
         StringBuilder manifest = new StringBuilder("tile\tfile\tlabel\tframe_count\tused_by\n");
         int count = 0;
         int[] atlas = TexPixel32.atlas(Tex.atlasPixels);
         png(target.resolve("atlas-reference.png"), atlas, 1024, 1024);
         for (int tile = 0; tile < 1024; tile++) {
            int[] low = TexOverhaulV57.readTile(tile);
            boolean visible = false;
            for (int c : low) if ((c >>> 24) != 0) visible = true;
            if (!visible || tile == Tex.WHITE) continue; // WHITE is a shader helper, not artwork.
            String label = names.getOrDefault(tile, "Model face");
            String slug = label.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
            int frames = animated(tile) ? (tile == 77 ? 8 : 16) : 1;
            String file = (frames == 1 ? "tiles/" : "animations/") + String.format(Locale.ROOT, "%04d_", tile) + slug + ".png";
            int width = 32 * frames;
            int[] pixels = new int[width * 32];
            for (int f = 0; f < frames; f++) {
               int[] framePixels = frames == 1 ? null : frame(tile, f);
               for (int y = 0; y < 32; y++) {
               // Static images are literal slices of the actual render atlas; animation sheets retain native frames.
               if (frames == 1) System.arraycopy(atlas, ((tile / 32) * 32 + y) * 1024 + (tile % 32) * 32, pixels, y * 32, 32);
               else System.arraycopy(framePixels, y * 32, pixels, y * width + f * 32, 32);
            }
            }
            png(target.resolve(file), pixels, width, 32);
            String used = String.join("; ", uses.getOrDefault(tile, Set.of("Model-only / shared material")));
            manifest.append(tile).append('\t').append(file).append('\t').append(label).append('\t').append(frames).append('\t').append(used).append('\n');
            html.append("<article><a href=\"").append(file).append("\"><img width=\"").append(frames == 1 ? 96 : width)
               .append("\" src=\"").append(file).append("\" alt=\"\"></a><h3>").append(tile).append(" / ").append(escape(label))
               .append("</h3><a href=\"").append(file).append("\">").append(file).append("</a><p>")
               .append(frames == 1 ? "32 x 32 RGBA tile" : frames + " frames, left to right; each 32 x 32")
               .append("</p><details><summary>Used by</summary><small>").append(escape(used)).append("</small></details></article>");
            count++;
         }
         html.append("</main><script>document.querySelector('#search').addEventListener('input',e=>{const q=e.target.value.toLowerCase();"
            + "document.querySelectorAll('article').forEach(a=>a.style.display=a.textContent.toLowerCase().includes(q)?'':'none')});</script>");
         Files.writeString(target.resolve("index.html"), html, StandardCharsets.UTF_8);
         Files.writeString(target.resolve("TEXTURES.tsv"), manifest, StandardCharsets.UTF_8);
         System.out.println("Exported " + count + " editable PNGs to " + target);
         System.out.println("Open index.html to find textures. Save PNG edits, then select My Aseprite Textures and Reload PNGs in-game.");
      } finally {
         ResourcePacks.select(previous, null);
      }
   }

   /** Actual-game test hook only: edit an already-uploaded pack, then use the UI's reload path. */
   static void smokeFrame(Game game, int frame) {
      try {
         if (frame == 1) {
            Path dir = Files.createTempDirectory(game.root.toPath(), "editor-smoke-");
            export(dir.resolve("MyTextures"));
            ResourcePacks.setDirForTest(dir);
            ResourcePacks.select("MyTextures", game);
         } else if (frame == 90) {
            Path dir = ResourcePacks.active().path;
            int[] pixels = new int[1024];
            for (int y = 0; y < 32; y++) for (int x = 0; x < 32; x++)
               pixels[y * 32 + x] = ((x / 4 + y / 4) & 1) == 0 ? 0xFFE545B3 : 0xFF382644;
            png(dir.resolve("tiles/0003_stone.png"), pixels, 32, 32);
            Path bow;
            try (var paths = Files.list(dir.resolve("animations"))) {
               bow = paths.filter(p -> p.getFileName().toString().startsWith("0077_")).findFirst().orElseThrow();
            }
            BufferedImage img = ImageIO.read(bow.toFile());
            for (int y = 0; y < 32; y++) for (int x = 0; x < 256; x++) {
               int c = img.getRGB(x, y);
               if ((c >>> 24) != 0) img.setRGB(x, y, (c & 0xFF000000) | 0x26EBCD);
            }
            ImageIO.write(img, "png", bow.toFile());
            ResourcePacks.reload(game);
            System.out.println("[editor-smoke] Reloaded edited native PNGs at frame 90: checker stone, teal bow; tileSize=" + Tex.renderTileSize);
         }
      } catch (Exception e) { throw new IllegalStateException("Texture editor smoke fixture failed", e); }
   }

   private static void use(Map<Integer, Set<String>> uses, int tile, String label) {
      if (tile >= 0 && tile < 1024) uses.computeIfAbsent(tile, k -> new TreeSet<>()).add(label);
   }
   private static String escape(String s) { return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;"); }
   static void png(Path path, int[] pixels, int width, int height) throws IOException {
      BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
      image.setRGB(0, 0, width, height, pixels, 0, width);
      if (!ImageIO.write(image, "png", path.toFile())) throw new IOException("PNG writer unavailable");
   }
}
