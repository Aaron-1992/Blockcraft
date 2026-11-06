package bc;

import static bc.GameTests.check;
import static bc.GameTests.eq;
import static bc.GameTests.test;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

/**
 * Update 6.23 regressions: more silhouettes and three size tiers for EVERY tree species,
 * the two new world/options knobs, deterministic variant selection across chunk seams, and
 * the hard leaf budget that keeps bigger trees from undoing the V6.22 taiga performance fix.
 */
final class Tests623 {
   private Tests623() {
   }

   static File tmp;

   static void run() {
      try {
         tmp = Files.createTempDirectory("bc623").toFile();
      } catch (Exception e) {
         throw new RuntimeException(e);
      }

      test("6.23: tree size and giant-tree settings persist and are clamped", Tests623::options);
      test("6.23: the Advanced tab exposes the tree size and giant controls", Tests623::advancedTab);
      test("6.23: all eight tree species have a full variant catalogue", Tests623::catalogue);
      test("6.23: every species really gets bigger trees at the higher size tiers", Tests623::sizes);
      test("6.23: variants and size tiers are deterministic and all of them appear", Tests623::distribution);
      test("6.23: the Tree Size slider scales every species", Tests623::scale);
      test("6.23: generated worlds contain mixed variants and stay deterministic", Tests623::generated);
      test("6.23: bigger trees keep the taiga leaf budget and the V6.22 mesh fix", Tests623::performance);
      test("6.23: the version, preview key and new-world settings are wired", Tests623::wiring);
   }

   // ------------------------------------------------------------------ helpers
   static Game screen(int gw, int gh) {
      GuiRaster r = new GuiRaster(gw * 3, gh * 3);
      Game g = new Game();
      g.gui = r;
      g.opt = new Options(new File(tmp, "screens"));
      g.fbW = gw * 3;
      g.fbH = gh * 3;
      g.winW = gw * 3;
      g.winH = gh * 3;
      r.begin(gw * 3, gh * 3, 3);
      g.activeBtnsSwap();
      g.mouseX = -1000;
      g.mouseY = -1000;
      return g;
   }

   static boolean button(Game g, String starts) {
      for (Game.Btn b : g.btns) {
         if (b.label != null && b.label.startsWith(starts)) return true;
      }

      return false;
   }

   static WorldSettings newWorld() {
      WorldSettings st = new WorldSettings();
      st.terrainVer = 3;
      st.caveVer = 2;
      st.biomeBorders = 1;
      return st;
   }

   /** Build one tree and return {blocks, leaves, max height, max horizontal radius}. */
   static int[] measure(int sp, int variant, int size, float scale, long seed) {
      TerraTrees.Buf b = new TerraTrees.Buf();
      b.begin(0, 70, 0);
      TerraTrees.build(b, new Random(seed), sp, variant, size, scale);
      int maxY = 0;
      int maxR = 0;
      for (int k = 0; k < b.nt; k++) {
         int i = b.touched[k];
         int dy = i / (TerraTrees.Buf.W * TerraTrees.Buf.W);
         int dx = i % TerraTrees.Buf.W - TerraTrees.Buf.R;
         int dz = i / TerraTrees.Buf.W % TerraTrees.Buf.W - TerraTrees.Buf.R;
         if (dy > maxY) maxY = dy;
         int rr = Math.max(Math.abs(dx), Math.abs(dz));
         if (rr > maxR) maxR = rr;
         check(dy >= 0 && dy < TerraTrees.Buf.H, "a tree stays inside the plant buffer vertically");
         check(rr <= TerraTrees.Buf.R, "a tree stays inside the plant buffer horizontally");
      }

      return new int[]{b.nt, b.leafCount, maxY, maxR};
   }

   // ------------------------------------------------------------------ settings
   static void options() throws Exception {
      File dir = new File(tmp, "opt");
      dir.mkdirs();
      Options f = new Options(dir);
      eq(1.0F, f.treeScale, "normal tree size by default");
      eq(true, f.giantTrees, "giant trees are on by default");
      f.treeScale = 1.35F;
      f.giantTrees = false;
      f.save();
      Options back = new Options(dir);
      back.load();
      eq(1.35F, back.treeScale, "tree size persists");
      eq(false, back.giantTrees, "the giant switch persists");

      Files.write(new File(dir, "options.txt").toPath(),
         ("treeScale=9\ngiantTrees=false\n").getBytes("UTF-8"));
      Options hi = new Options(dir);
      hi.load();
      eq(1.5F, hi.treeScale, "an oversized tree scale is clamped");
      Files.write(new File(dir, "options.txt").toPath(), ("treeScale=0.01\n").getBytes("UTF-8"));
      Options lo = new Options(dir);
      lo.load();
      eq(0.7F, lo.treeScale, "tree scale has a floor");
      eq(true, lo.giantTrees, "a missing giant switch defaults to on");

      Properties p = new Properties();
      newWorld().save(p);
      WorldSettings ws = WorldSettings.load(p);
      eq(1.0F, ws.treeScale, "a new world has normal tree size");
      eq(true, ws.giantTrees, "a new world allows giants");
      eq(1.0F, newWorld().copy().treeScale, "copy keeps tree size");
      eq(true, newWorld().copy().giantTrees, "copy keeps the giant switch");

      p.setProperty("treescale", "44");
      p.setProperty("gianttrees", "false");
      WorldSettings clamped = WorldSettings.load(p);
      eq(1.5F, clamped.treeScale, "a future tree scale is clamped");
      eq(false, clamped.giantTrees, "the giant switch round-trips");
      p.setProperty("treescale", "-3");
      eq(0.7F, WorldSettings.load(p).treeScale, "tree scale has a floor in saves");

      Properties legacy = new Properties();
      legacy.setProperty("gentype", "0");
      WorldSettings old = WorldSettings.load(legacy);
      eq(1.0F, old.treeScale, "an old save keeps its tree size");
      eq(true, old.giantTrees, "an old save defaults giants on");
   }

   static void advancedTab() throws Exception {
      Game g = screen(343, 1900); // tall enough for every one of the 61 scrolling rows
      g.opt.debugVideo = true;
      g.ui.optTab = 4;
      Screens.options(g);
      eq(61, Screens.ADV_ROWS, "the Advanced tab knows every row it has");
      check(button(g, "Tree Size"), "tree size is configurable in game");
      check(button(g, "Giant Trees"), "giant trees are configurable in game");
      check(button(g, "Tree Density"), "the V6.22 tree density is retained");
      check(button(g, "Caves & Cliffs"), "the V6.22 caves are retained");
      check(button(g, "Minimap:"), "the V6.22 minimap positions are retained");
   }

   // ------------------------------------------------------------------ catalogue
   static void catalogue() {
      eq(8, TerraTrees.VARIANTS.length, "there are eight tree species");
      int total = 0;
      for (int sp = 0; sp < TerraTrees.VARIANTS.length; sp++) {
         int n = TerraTrees.variants(sp);
         check(n >= 2, TerraTrees.SPECIES_NAME[sp] + " has at least two silhouettes");
         total += n;
         Set<String> shapes = new HashSet<>();
         for (int v = 0; v < n; v++) {
            int[] m = measure(sp, v, 1, 1.0F, sp * 977L + v * 131L + 4242L);
            check(m[0] > 10, TerraTrees.SPECIES_NAME[sp] + " variant " + v + " builds a real tree");
            check(m[1] > 0, TerraTrees.SPECIES_NAME[sp] + " variant " + v + " has leaves");
            check(m[1] <= TerraTrees.Buf.MAX_LEAVES, "no tree exceeds the leaf budget");
            shapes.add(m[0] + "/" + m[2] + "/" + m[3]);
         }

         check(shapes.size() >= Math.min(3, n), TerraTrees.SPECIES_NAME[sp] + " silhouettes really differ (" + shapes.size() + ")");
      }

      check(total >= 30, "the whole catalogue has plenty of variants (" + total + ")");
      eq(true, TerraTrees.isWide(TerraTrees.SP_SPRUCE, 3), "the 2x2 spruce needs flat ground");
      eq(true, TerraTrees.isWide(TerraTrees.SP_JUNGLE, 2), "the 2x2 jungle tree needs flat ground");
      eq(true, TerraTrees.isWide(TerraTrees.SP_DARK_OAK, 0), "dark oaks need flat ground");
      eq(false, TerraTrees.isWide(TerraTrees.SP_OAK, 0), "a normal oak has a single trunk");

      // legacy entry points still build something (used by the older regressions)
      TerraTrees.Buf b = new TerraTrees.Buf();
      for (int i = 0; i < 3; i++) {
         b.begin(0, 70, 0);
         TerraTrees.classic(b, new Random(i), 2, i);
         check(b.nt > 20 && b.leafCount < TerraTrees.Buf.MAX_LEAVES, "the classic spruce builder still works");
      }
      b.begin(0, 70, 0);
      TerraTrees.megaSpruce(b, new Random(99L));
      check(b.nt > 60, "the giant spruce builder still works");
   }

   static void sizes() {
      for (int sp = 0; sp < TerraTrees.VARIANTS.length; sp++) {
         int n = TerraTrees.variants(sp);
         int h0 = 0, h2 = 0, l0 = 0, l2 = 0, r0 = 0, r2 = 0;
         for (int v = 0; v < n; v++) {
            long seed = sp * 977L + v * 131L + 4242L;
            int[] small = measure(sp, v, 0, 1.0F, seed);
            int[] giant = measure(sp, v, 2, 1.0F, seed);
            h0 += small[2]; h2 += giant[2];
            l0 += small[1]; l2 += giant[1];
            r0 += small[3]; r2 += giant[3];
         }

         check(h2 > h0, TerraTrees.SPECIES_NAME[sp] + " giants are taller (" + h0 + " -> " + h2 + ")");
         check(l2 > l0, TerraTrees.SPECIES_NAME[sp] + " giants have bigger crowns (" + l0 + " -> " + l2 + ")");
         check(r2 >= r0, TerraTrees.SPECIES_NAME[sp] + " giants are at least as wide");
      }

      check(TerraTrees.heightScale(2, 1.0F) > TerraTrees.heightScale(0, 1.0F), "the giant tier scales height");
      check(TerraTrees.radiusScale(2, 1.0F) > TerraTrees.radiusScale(0, 1.0F), "the giant tier scales crowns");
      check(TerraTrees.heightScale(2, 0.7F) < TerraTrees.heightScale(2, 1.5F), "the world scale multiplies the tier");
   }

   static void distribution() {
      Random rnd = new Random(6120L);
      int[] tiers = new int[3];
      int[] variants = new int[6];
      for (int i = 0; i < 20000; i++) {
         long h = rnd.nextLong();
         tiers[TerraGen.treeSizeTier(TerraGen.P_OAK_CLASSIC, h, true)]++;
         variants[TerraGen.treeVariant(TerraGen.P_OAK_CLASSIC, h)]++;
      }

      check(tiers[0] > 10000 && tiers[1] > 4000 && tiers[2] > 1000, "all three size tiers really appear " + Arrays.toString(tiers));
      check(tiers[0] > tiers[1] && tiers[1] > tiers[2], "normal is commonest, giants are rare");
      for (int v = 0; v < 6; v++) check(variants[v] > 2000, "oak variant " + v + " really appears (" + variants[v] + ")");

      int off = 0;
      for (int i = 0; i < 20000; i++) if (TerraGen.treeSizeTier(TerraGen.P_OAK_CLASSIC, rnd.nextLong(), false) == 2) off++;
      eq(0, off, "the Giant Trees switch removes the giant tier");
      check(TerraGen.treeSizeTier(TerraGen.P_MEGA_SPRUCE, 0L, false) >= 1, "a mega plan is never a normal-sized tree");

      // every silhouette of every species is reachable from a plan
      for (int sp = 0; sp < TerraTrees.VARIANTS.length; sp++) {
         int plan = planOf(sp);
         Set<Integer> seen = new HashSet<>();
         for (int i = 0; i < 4000; i++) seen.add(TerraGen.treeVariant(plan, rnd.nextLong()));
         eq(TerraTrees.variants(sp), seen.size(), TerraTrees.SPECIES_NAME[sp] + " reaches all of its variants");
      }

      // determinism across chunk seams: the same hash gives the same tree everywhere
      long h = rnd.nextLong();
      eq(TerraGen.treeVariant(TerraGen.P_SPRUCE, h), TerraGen.treeVariant(TerraGen.P_SPRUCE, h), "a variant is a pure function of the tree hash");
      eq(TerraGen.treeSizeTier(TerraGen.P_SPRUCE, h, true), TerraGen.treeSizeTier(TerraGen.P_SPRUCE, h, true), "a size tier is a pure function of the tree hash");

      int[] plans = {TerraGen.P_OAK_SMALL, TerraGen.P_OAK_CLASSIC, TerraGen.P_BIRCH, TerraGen.P_BIRCH_TALL,
         TerraGen.P_SPRUCE, TerraGen.P_SPRUCE_PINE, TerraGen.P_MEGA_SPRUCE, TerraGen.P_DARK_OAK,
         TerraGen.P_JUNGLE_SMALL, TerraGen.P_JUNGLE_TALL, TerraGen.P_MEGA_JUNGLE, TerraGen.P_BUSH,
         TerraGen.P_ACACIA, TerraGen.P_SWAMP_OAK};
      for (int p : plans) check(TerraGen.treeSpecies(p) >= 0, "plan " + p + " maps to a tree species");
      eq(-1, TerraGen.treeSpecies(TerraGen.P_ICE_SPIKE), "an ice spike is not a tree");
      eq(-1, TerraGen.treeSpecies(TerraGen.P_MUSH_RED), "a giant mushroom is not a tree");
   }

   static int planOf(int sp) {
      switch (sp) {
         case TerraTrees.SP_OAK: return TerraGen.P_OAK_CLASSIC;
         case TerraTrees.SP_BIRCH: return TerraGen.P_BIRCH;
         case TerraTrees.SP_SPRUCE: return TerraGen.P_SPRUCE;
         case TerraTrees.SP_JUNGLE: return TerraGen.P_JUNGLE_SMALL;
         case TerraTrees.SP_ACACIA: return TerraGen.P_ACACIA;
         case TerraTrees.SP_DARK_OAK: return TerraGen.P_DARK_OAK;
         case TerraTrees.SP_SWAMP: return TerraGen.P_SWAMP_OAK;
         default: return TerraGen.P_BUSH;
      }
   }

   static void scale() {
      TerraGen tg = new TerraGen(6120L, newWorld());
      for (int sp = 0; sp < TerraTrees.VARIANTS.length; sp++) {
         int v = TerraTrees.variants(sp) - 1;
         int[] small = measure(sp, v, 1, 0.7F, 4242L + sp);
         int[] large = measure(sp, v, 1, 1.5F, 4242L + sp);
         check(large[2] >= small[2], TerraTrees.SPECIES_NAME[sp] + " responds to the Tree Size slider");
      }

      // buildPlan honours the world's scale through the six-argument overload too
      TerraTrees.Buf a = new TerraTrees.Buf();
      TerraTrees.Buf b = new TerraTrees.Buf();
      a.begin(0, 70, 0);
      b.begin(0, 70, 0);
      tg.buildPlan(a, new Random(7L), TerraGen.P_OAK_CLASSIC, 0, 1, 0.7F);
      tg.buildPlan(b, new Random(7L), TerraGen.P_OAK_CLASSIC, 0, 1, 1.5F);
      check(b.nt > a.nt, "a larger Tree Size builds a larger tree");

      // mushrooms and ice spikes also got size tiers
      a.begin(0, 70, 0);
      TerraTrees.giantMushroom(a, new Random(3L), 0, 2, 1.5F);
      check(a.nt > 20, "giant mushrooms have a Giant tier");
      b.begin(0, 70, 0);
      TerraTrees.iceSpike(b, new Random(3L), 2, 1.5F);
      check(b.nt > 20, "ice spikes scale too");
   }

   static void generated() {
      long seed = 6120L;
      WorldSettings on = newWorld();
      on.structures = false;
      WorldSettings off = newWorld();
      off.structures = false;
      off.giantTrees = false;
      off.treeScale = 0.7F;
      TerraGen a = new TerraGen(seed, on);
      TerraGen b = new TerraGen(seed, off);

      // Find a wooded part of the world: the origin of this seed is ocean.
      int fcx = 0, fcz = 0;
      outer:
      for (int x = -1600; x < 1600; x += 16) {
         for (int z = -1600; z < 1600; z += 16) {
            int biome = a.column(x, z).biome;
            if (biome == Biomes.FOREST || biome == Biomes.JUNGLE || biome == Biomes.TAIGA
               || biome == Biomes.BIRCH_FOREST || biome == Biomes.DARK_FOREST) {
               fcx = Math.floorDiv(x, 16);
               fcz = Math.floorDiv(z, 16);
               break outer;
            }
         }
      }

      int leavesOn = 0, leavesOff = 0;
      int different = 0;
      for (int cx = fcx - 2; cx <= fcx + 2; cx++) {
         for (int cz = fcz - 2; cz <= fcz + 2; cz++) {
            Chunk ca = a.generate(cx, cz);
            Chunk cb = b.generate(cx, cz);
            for (short s : ca.blocks) if (Blocks.isLeaves(s & BlockState.ID_MASK)) leavesOn++;
            for (short s : cb.blocks) if (Blocks.isLeaves(s & BlockState.ID_MASK)) leavesOff++;
            if (!Arrays.equals(ca.blocks, cb.blocks)) different++;
         }
      }

      check(leavesOn > 100, "generated chunks really contain varied trees (" + leavesOn + " leaves)");
      check(leavesOn > leavesOff, "Giant Trees + Tree Size make the canopy bigger (" + leavesOn + " vs " + leavesOff + ")");
      check(different > 10, "the tree settings change real generated chunks (" + different + "/25)");

      Chunk c1 = a.generate(11, -7);
      Chunk c2 = new TerraGen(seed, on).generate(11, -7);
      eq(true, Arrays.equals(c1.blocks, c2.blocks), "variant selection is deterministic across chunk seams");
      check(c1.hm[0] >= 0, "the heightmap is still recomputed after trees");
   }

   static void performance() {
      // The V6.22 taiga fix must survive the bigger/varied trees.
      TerraTrees.Buf b = new TerraTrees.Buf();
      int worst = 0;
      for (int sp = 0; sp < TerraTrees.VARIANTS.length; sp++) {
         for (int v = 0; v < TerraTrees.variants(sp); v++) {
            for (int size = 0; size < 3; size++) {
               for (int k = 0; k < 4; k++) {
                  b.begin(0, 70, 0);
                  TerraTrees.build(b, new Random(sp * 977L + v * 131L + size * 37L + k * 7L + 1L), sp, v, size, 1.5F);
                  if (b.leafCount > worst) worst = b.leafCount;
                  check(b.leafCount <= TerraTrees.Buf.MAX_LEAVES, "the hard leaf budget holds at maximum Tree Size");
               }
            }
         }
      }

      check(worst > 200, "giant trees are genuinely big (" + worst + " leaves at most)");
      check(worst <= TerraTrees.Buf.MAX_LEAVES, "the biggest tree is still bounded (" + worst + ")");

      long seed = 6120L;
      TerraGen tg = new TerraGen(seed, newWorld());
      int tx = 0, tz = 0;
      outer:
      for (int x = -1200; x < 1200; x += 5) {
         for (int z = -1200; z < 1200; z += 5) {
            int biome = tg.column(x, z).biome;
            if (biome == Biomes.TAIGA || biome == Biomes.MEGA_TAIGA || biome == Biomes.SNOWY_TAIGA) {
               tx = x;
               tz = z;
               break outer;
            }
         }
      }

      Chunk c = tg.generate(Math.floorDiv(tx, 16), Math.floorDiv(tz, 16));
      int spruce = 0;
      for (short s : c.blocks) if ((s & BlockState.ID_MASK) == Blocks.LEAVES_SPRUCE) spruce++;
      check(spruce < 500, "a taiga chunk still has a bounded leaf count (" + spruce + ")");
   }

   static void wiring() {
      eq("6.26", Game.VERSION_STR, "the game version is 6.26");
      Game g = screen(480, 360);
      g.opt.treeScale = 1.3F;
      g.opt.giantTrees = false;
      Screens.applyNewWorldSettings(g);
      eq(1.3F, g.newSettings.treeScale, "new worlds copy Tree Size");
      eq(false, g.newSettings.giantTrees, "new worlds copy Giant Trees");

      WorldPreview pv = new WorldPreview(40, 30, 12, false);
      check(pv.update(1234L, newWorld()), "the first preview is drawn");
      WorldSettings alt = newWorld();
      alt.treeScale = 1.25F;
      check(pv.update(1234L, alt), "changing Tree Size redraws the preview");
      alt.giantTrees = false;
      check(pv.update(1234L, alt), "changing Giant Trees redraws the preview");
      check(!pv.update(1234L, alt), "an unchanged key does not redraw");
   }
}
