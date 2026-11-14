package bc;

import static bc.GameTests.GROUND;
import static bc.GameTests.check;
import static bc.GameTests.eq;
import static bc.GameTests.newGame;
import static bc.GameTests.test;

import java.io.File;
import java.nio.file.Files;
import java.util.Random;

/**
 * V6.25: the new branch-skeleton trees are real trees (a choppable log network with
 * foliage hanging on it), they really topple through the /treefall system, and one
 * editable PNG tilesheet carries every texture in the game.
 */
final class Tests625 {
   private Tests625() {
   }

   static void run() {
      test("6.25: branch skeletons have visible branches, not a bare column", Tests625::skeleton);
      test("6.25: generated trees actually fall over when chopped", Tests625::falling);
      test("6.25: the falling planner accepts the new tree sizes", Tests625::limits);
      test("6.25: the LOD still measures the real tree shapes", Tests625::lod);
      test("6.25: the all-textures tilesheet is a directly editable PNG", Tests625::tilesheet);
      test("6.25: an edited tilesheet really changes the game textures", Tests625::roundTrip);
      test("6.25: version, notes and download links point at 6.25", Tests625::wiring);
   }

   static String src(String p) throws Exception {
      return new String(Files.readAllBytes(new File(p).toPath()), "UTF-8");
   }

   static void skeleton() throws Exception {
      String t = src("src/bc/TerraTrees.java");
      for (String need : new String[]{"private static void grow(", "private static int[] branch(",
         "static void leafCluster(", "6-connected", "clusters.add"}) {
         check(t.contains(need), "the tree builder contains " + need);
      }
      check(!t.contains("private static void oakShape("), "the old geometric shape builders are gone");
      int withBranches = 0;
      for (int sp = 0; sp < TerraTrees.VARIANTS.length; sp++) {
         for (int v = 0; v < TerraTrees.variants(sp); v++) {
            TerraTrees.Buf b = new TerraTrees.Buf();
            b.begin(0, 70, 0);
            TerraTrees.build(b, new Random(sp * 977L + v * 131L + 4242L), sp, v, 0, 1.0F);
            int off = 0, leaves = 0;
            for (int k = 0; k < b.nt; k++) {
               int i = b.touched[k];
               int id = (b.a[i] & ~TerraTrees.Buf.SOFT) - 1;
               int dx = i % TerraTrees.Buf.W - TerraTrees.Buf.R;
               int dz = i / TerraTrees.Buf.W % TerraTrees.Buf.W - TerraTrees.Buf.R;
               if (Blocks.isLeaves(id)) leaves++;
               else if (dx != 0 || dz != 0) off++;
            }
            check(off >= 3 && leaves > 20, TerraTrees.SPECIES_NAME[sp] + " v" + v + " has branches and foliage");
            withBranches++;
         }
      }
      eq(34, withBranches, "all 34 silhouettes were checked");
   }

   /** Build a real V6.25 tree into a flat world and chop its base log. */
   static void falling() throws Exception {
      int felled = 0;
      for (int sp = 0; sp < TerraTrees.VARIANTS.length; sp++) {
         for (int tier : new int[]{0, 2}) {
            Game g = newGame(0);
            World w = g.world;
            Chunk c = w.chunk(1, 1);
            TerraTrees.Buf b = new TerraTrees.Buf();
            b.begin(24, GROUND + 1, 24);
            TerraTrees.build(b, new Random(4242L + sp * 977L + tier * 131L), sp, 0, tier, 1.0F);
            b.commit(c);
            int logs0 = 0, leaves0 = 0;
            for (int k = 0; k < b.nt; k++) {
               int i = b.touched[k];
               int id = (b.a[i] & ~TerraTrees.Buf.SOFT) - 1;
               if (Blocks.isLeaves(id)) leaves0++; else logs0++;
            }
            check(logs0 > 5 && leaves0 > 20, TerraTrees.SPECIES_NAME[sp] + " tier " + tier + " was planted");
            g.player.setPos(18.5, GROUND + 1, 24.5);
            g.breakBlock(24, GROUND + 1, 24);
            eq(1, w.trees.falling().size(), TerraTrees.SPECIES_NAME[sp] + " tier " + tier + " topples");
            int ticks = 0;
            while (!w.trees.falling().isEmpty() && ticks < 400) { w.tick(); ticks++; }
            check(ticks < 400, "the fall lands");
            int standing = 0;
            for (int y = GROUND + 1; y <= GROUND + 40; y++) {
               for (int x = 8; x <= 40; x++) for (int z = 8; z <= 40; z++) {
                  if (Blocks.isLog(w.getBlock(x, y, z))) standing++;
               }
            }
            check(standing <= 2, TerraTrees.SPECIES_NAME[sp] + " tier " + tier
               + " leaves no trunk standing (" + standing + ")");
            felled++;
         }
      }
      eq(16, felled, "every species topples at Normal and Giant size");
   }

   static void limits() throws Exception {
      check(Trees.MAX_LOGS >= 300, "the log cap fits a branch skeleton");
      check(Trees.MAX_LEAVES >= 1100, "the leaf cap fits the biggest crown");
      check(Trees.LEAF_REACH >= 6, "weeping strands still hang on a log");
      check(Trees.MAX_SPAN >= 16, "wide crowns stay inside the fall scan");
      String s = src("src/bc/Trees.java");
      check(s.contains("thickBase"), "2x2 trunk bases no longer block the whole fall");
   }

   static void lod() throws Exception {
      TerraTrees.LodShape small = TerraTrees.lodShape(TerraTrees.SP_OAK, 0, 0);
      TerraTrees.LodShape giant = TerraTrees.lodShape(TerraTrees.SP_OAK, 0, 2);
      check(giant.top > small.top, "LOD still measures the taller giant");
      check(giant.radius >= 1.0F && giant.bands[0] > 0.7F, "LOD bands are measured from real foliage");
      String l = src("src/bc/Lod.java");
      check(l.contains("TerraTrees.lodShape"), "LOD still uses the measured silhouettes");
      check(l.contains("treeAttemptSeed"), "LOD still replays the generator's dice");
   }

   static void tilesheet() throws Exception {
      File png = new File("textures/Blockcraft-tilesheet.png");
      check(png.isFile(), "the all-texture tilesheet exists");
      if (png.isFile()) {
         java.awt.image.BufferedImage bi = javax.imageio.ImageIO.read(png);
         check(bi != null && bi.getWidth() == 1024 && bi.getHeight() == 1024, "the sheet is a full 1024x1024 atlas");
         int opaque = 0;
         for (int px : bi.getRGB(0, 0, 1024, 1024, null, 0, 1024)) if ((px >>> 24) != 0) opaque++;
         check(opaque > 100000, "the tilesheet contains all the real art (" + opaque + " opaque pixels)");
      }
      check(new File("textures/TILESHEET.tsv").isFile(), "the tile index exists");
      check(new File("textures/tilesheet-index.html").isFile(), "the searchable index exists");
      check(new File("tools/tilesheet.py").isFile(), "the import/export tool exists");
      String t = src("src/bc/Tilesheet.java");
      check(t.contains("void apply(int[] render)"), "the game applies the edited sheet");
      check(t.contains("boolean install(File src)"), "an edited sheet can be installed");
      check(new File("package/tilesheet.png").isFile(), "the tilesheet ships in the Windows zip");
   }

   /** Install a modified sheet and prove the render + CPU atlases pick up the edit. */
   static void roundTrip() throws Exception {
      java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(new File("textures/Blockcraft-tilesheet.png"));
      int tile = 3;                            // stone
      int tx = tile % 32 * 32, ty = tile / 32 * 32;
      img.setRGB(tx + 4, ty + 4, 0xFFFF00FF);
      File tmp = File.createTempFile("bc625-sheet-", ".png");
      javax.imageio.ImageIO.write(img, "png", tmp);
      File live = Tilesheet.file();
      byte[] old = live.isFile() ? Files.readAllBytes(live.toPath()) : null;
      int[] saved = Tex.atlasPixels.clone();
      try {
         Tilesheet.install(tmp);
         Tilesheet.invalidate();
         int[] render = TexPixel32.atlas(Tex.atlasPixels);
         Tilesheet.apply(render);
         check(render[(ty + 4) * 1024 + tx + 4] == 0xFFFF00FF, "the 32px render atlas picks up the edit");
         Tilesheet.applyLow(Tex.atlasPixels);
         check(Tex.atlasPixels[((tile / 32) * 16 + 2) * Tex.PX + (tile % 32) * 16 + 2] == 0xFFFF00FF,
            "the 16px CPU atlas (LOD colours / item icons) picks up the edit");
         check(Tilesheet.changedTiles() >= 1, "the game reports the edited tiles");
      } finally {
         tmp.delete();
         if (old == null) live.delete(); else Files.write(live.toPath(), old);
         System.arraycopy(saved, 0, Tex.atlasPixels, 0, saved.length);
         Tilesheet.invalidate();
      }
   }

   static void wiring() throws Exception {
      eq("6.38", Game.VERSION_STR, "the game version is 6.37");
      eq("6.38", src("ci/release-trigger.txt").trim(), "the release workflow publishes 6.37");
      check(src("README.md").contains("V6.28"), "README describes V6.25");
      check(src("package/README.txt").contains("V6.28"), "the packaged readme describes V6.25");
      check(new File("docs/releases/v6.25.md").isFile(), "V6.25 release notes exist");
      check(new File("docs/screenshots/v6.25-trees.png").isFile(), "the tree preview exists");
   }
}
