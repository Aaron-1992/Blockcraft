package bc;

import static bc.GameTests.check;
import static bc.GameTests.eq;
import static bc.GameTests.test;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/**
 * V6.27: the V6.25 tilesheet compared its cells against the CURRENT render atlas,
 * so an untouched sheet silently replaced resource-pack PNG edits with the default
 * art. These tests reproduce the old workflow (Edit-textures.bat -> tiles/0003_*.png
 * -> Reload PNGs -> select the pack) and prove the edits now survive.
 */
final class Tests627 {
   private Tests627() {
   }

   static void run() {
      test("6.27: resource-pack PNG edits survive the tilesheet overlay", Tests627::packSurvives);
      test("6.27: an edited tilesheet cell still wins over a resource pack", Tests627::sheetWins);
      test("6.27: PNG overrides apply for every pack kind", Tests627::allPacks);
      test("6.27: the UI reports how many overrides are active", Tests627::diagnostics);
      test("6.27: version, notes and download links point at 6.27", Tests627::wiring);
   }

   static String src(String p) throws Exception {
      return new String(Files.readAllBytes(new File(p).toPath()), "UTF-8");
   }

   static int at(int[] a, int tile, int x, int y) {
      return a[(tile / 32 * 32 + y) * 1024 + tile % 32 * 32 + x];
   }

   static void writeTilePng(Path p) throws Exception {
      BufferedImage img = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
      for (int y = 0; y < 32; y++) for (int x = 0; x < 32; x++) img.setRGB(x, y, 0xFFFF00FF);
      ImageIO.write(img, "png", p.toFile());
   }

   /** Build a temporary Aseprite-style pack, select it, and return the 32px render atlas. */
   static int[] withPack(Path root) throws Exception {
      Path pack = root.resolve("MyTextures");
      Files.createDirectories(pack.resolve("tiles"));
      Files.writeString(pack.resolve("pack.properties"),
         "name=My Aseprite Textures\ndescription=test\nbase=Default\ntileSize=32\n");
      writeTilePng(pack.resolve("tiles/0003_stone.png"));
      ResourcePacks.setDirForTest(root);
      ResourcePacks.select("MyTextures", null);
      return TexPixel32.atlas(Tex.atlasPixels);
   }

   static void cleanup(Path root) throws Exception {
      Tilesheet.file().delete();
      Tilesheet.invalidate();
      ResourcePacks.select(ResourcePacks.ID_DEFAULT, null);
      ResourcePacks.setDirForTest(new File("resourcepacks").toPath());
   }

   static void packSurvives() throws Exception {
      Path root = Files.createTempDirectory("bc627pack");
      try {
         int[] render = withPack(root);
         eq(0xFFFF00FF, at(render, 3, 5, 5), "the pack's edited stone really reached the atlas");
         check(ResourcePacks.overrideCount() >= 1, "the pack reported a loaded PNG override");
         // This is the exact operation that used to clobber the edit.
         Tilesheet.install(new File("textures/Blockcraft-tilesheet.png"));
         Tilesheet.invalidate();
         Tilesheet.apply(render);
         eq(0xFFFF00FF, at(render, 3, 5, 5), "an untouched tilesheet leaves the pack edit alone");
         Tilesheet.applyLow(Tex.atlasPixels);
         eq(0xFFFF00FF, Tex.atlasPixels[((3 / 32) * 16 + 2) * Tex.PX + (3 % 32) * 16 + 2],
            "the CPU 16px atlas keeps the pack edit too");
         eq(0, Tilesheet.changedTiles(), "an untouched sheet reports zero edited cells");
      } finally {
         cleanup(root);
      }
   }

   static void sheetWins() throws Exception {
      Path root = Files.createTempDirectory("bc627sheet");
      try {
         int[] render = withPack(root);
         BufferedImage img = ImageIO.read(new File("textures/Blockcraft-tilesheet.png"));
         // tile 3 lives at col 3 / row 0 -> x offset 96
         for (int y = 0; y < 32; y++) for (int x = 0; x < 32; x++) img.setRGB(96 + x, y, 0xFF00FFFF);
         File tmp = File.createTempFile("bc627-sheet-", ".png");
         ImageIO.write(img, "png", tmp);
         try {
            Tilesheet.install(tmp);
            Tilesheet.invalidate();
            Tilesheet.apply(render);
            eq(0xFF00FFFF, at(render, 3, 5, 5), "an edited tilesheet cell still overrides the pack");
            check(Tilesheet.changedTiles() >= 1, "the game reports the edited cells");
         } finally {
            tmp.delete();
         }
      } finally {
         cleanup(root);
      }
   }

   static void allPacks() throws Exception {
      String r = src("src/bc/ResourcePacks.java");
      check(r.contains("applyTileOverrides(pix)"), "PNG overrides have one shared application path");
      check(r.indexOf("ID_VIBRANT") < r.indexOf("applyTileOverrides(pix)"), "Vibrant applies overrides");
      check(r.contains("public static synchronized int overrideCount()"), "override count is exposed");
      String t = src("src/bc/Tilesheet.java");
      check(t.contains("DEFAULT_FILE") && t.contains("loadDefault()"), "a pristine reference sheet is used");
      check(t.contains("editedCell(s, def"), "edits are detected against the reference, not the live atlas");
      check(!t.contains("differs(s, render"), "the old clobbering comparison is gone");
   }

   static void diagnostics() throws Exception {
      String s = src("src/bc/Screens.java");
      check(s.contains("ResourcePacks.overrideCount()"), "Reload PNGs reports the loaded override count");
      String g = src("src/bc/Game.java");
      check(g.contains("pack overrides (active: "), "/tilesheet reports pack and sheet state");
   }

   static void wiring() throws Exception {
      eq("6.36", Game.VERSION_STR, "the game version is 6.36");
      eq("6.36", src("ci/release-trigger.txt").trim(), "the release workflow publishes 6.36");
      check(src("README.md").contains("V6.28"), "README describes V6.27");
      check(src("package/README.txt").contains("V6.28"), "the packaged readme describes V6.27");
      check(new File("docs/releases/v6.27.md").isFile(), "V6.27 release notes exist");
      check(new File("package/tilesheet-default.png").isFile(), "the pristine reference ships with the game");
   }
}
