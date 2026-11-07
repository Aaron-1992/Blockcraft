package bc;

import static bc.GameTests.check;
import static bc.GameTests.eq;
import static bc.GameTests.test;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Update 6.22 regressions: the Caves &amp; Cliffs carver, the cheaper taiga trees, smoother biome
 * borders and world generation, entity shadows that update every frame, and the four minimap
 * corners with the advancement-toast dodge. All headless: no OpenGL, no display, no GPU.
 */
final class Tests622 {
   private Tests622() {
   }

   static File tmp;

   static void run() {
      try {
         tmp = Files.createTempDirectory("bc622").toFile();
      } catch (Exception e) {
         throw new RuntimeException(e);
      }

      test("6.22: new world, tree, shadow and minimap options persist and are clamped", Tests622::options);
      test("6.22: the Advanced tab exposes every new V6.22 control", Tests622::advancedTab);
      test("6.22: new worlds get Caves & Cliffs and smooth borders; old saves stay V6.21", Tests622::worldSettings);
      test("6.22: biome borders meander, blend and soften map colours", Tests622::biomeBorders);
      test("6.22: world generation gains terraces, valleys and gentle shores", Tests622::worldGen);
      test("6.22: the Caves & Cliffs carver has cheese, spaghetti, aquifers and geodes", Tests622::carver);
      test("6.22: generated V6.22 chunks contain aquifer water and cave biomes", Tests622::generatedWorld);
      test("6.22: spruce crowns are sparse and taiga chunks are much cheaper", Tests622::spruceTrees);
      test("6.22: entity shadows re-stamp every frame without a terrain refresh", Tests622::entityShadows);
      test("6.22: the minimap has four corners and dodges achievement toasts", Tests622::minimapPositions);
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

   static int count(Chunk c, int id) {
      int n = 0;
      for (short s : c.blocks) {
         if ((s & BlockState.ID_MASK) == id) n++;
      }

      return n;
   }

   static boolean has(Chunk c, int id) {
      for (short s : c.blocks) {
         if ((s & BlockState.ID_MASK) == id) return true;
      }

      return false;
   }

   /** Same generator as V6.21, with the V6.22 world-gen knobs off. */
   static WorldSettings legacy() {
      WorldSettings st = new WorldSettings();
      st.terrainVer = 3;
      st.caveVer = 1;
      st.biomeBorders = 0;
      st.caveScale = 1.6F;
      st.caveDensity = 1.3F;
      return st;
   }

   static WorldSettings newWorld() {
      WorldSettings st = new WorldSettings();
      st.terrainVer = 3;
      st.caveVer = 2;
      st.biomeBorders = 1;
      st.caveScale = 1.6F;
      st.caveDensity = 1.3F;
      return st;
   }

   // ------------------------------------------------------------------ settings
   static void options() throws Exception {
      File dir = new File(tmp, "opt");
      dir.mkdirs();
      Options f = new Options(dir);
      eq(true, f.caveCliffs, "Caves & Cliffs is on by default");
      eq(1, f.biomeBorders, "smooth biome borders by default");
      eq(1.0F, f.treeDensity, "normal tree density by default");
      eq(0, f.minimapPos, "top-right minimap by default");
      eq(true, f.minimapAutoMove, "toast dodging is on by default");
      eq(true, f.entityShadowMotion, "live entity shadows are on by default");

      f.caveCliffs = false;
      f.biomeBorders = 2;
      f.treeDensity = 0.7F;
      f.minimapPos = 3;
      f.minimapAutoMove = false;
      f.entityShadowMotion = false;
      f.save();
      Options back = new Options(dir);
      back.load();
      eq(false, back.caveCliffs, "the cave preset persists");
      eq(2, back.biomeBorders, "biome borders persist");
      eq(0.7F, back.treeDensity, "tree density persists");
      eq(3, back.minimapPos, "minimap corner persists");
      eq(false, back.minimapAutoMove, "toast dodging persists");
      eq(false, back.entityShadowMotion, "live shadows persist");

      Files.write(new File(dir, "options.txt").toPath(),
         ("caveCliffs=true\nbiomeBorders=99\ntreeDensity=9\nminimapPos=42\n").getBytes("UTF-8"));
      Options clamp = new Options(dir);
      clamp.load();
      eq(2, clamp.biomeBorders, "oversized border setting is clamped");
      eq(1.4F, clamp.treeDensity, "oversized tree density is clamped");
      eq(3, clamp.minimapPos, "oversized minimap corner is clamped");
      Files.write(new File(dir, "options.txt").toPath(), ("treeDensity=0.01\nbiomeBorders=-8\nminimapPos=-4\n").getBytes("UTF-8"));
      Options low = new Options(dir);
      low.load();
      eq(0.4F, low.treeDensity, "tree density has a floor");
      eq(0, low.biomeBorders, "border setting has a floor");
      eq(0, low.minimapPos, "minimap corner has a floor");
   }

   static void advancedTab() throws Exception {
      Game g = screen(343, 1800); // tall enough for every one of the 60 scrolling rows
      g.opt.debugVideo = true;
      g.ui.optTab = 4;
      Screens.options(g);
      eq(61, Screens.ADV_ROWS, "the Advanced tab knows every row it has");
      check(button(g, "Caves & Cliffs"), "the Caves & Cliffs carver is configurable");
      check(button(g, "Biome Borders"), "biome border smoothing is configurable");
      check(button(g, "Tree Density"), "tree density is configurable");
      check(button(g, "Live Entity Shadows"), "live entity shadows are configurable");
      check(button(g, "Minimap:"), "all minimap corners are configurable");
      check(button(g, "Move for Toasts"), "the toast dodge is configurable");
      check(button(g, "V6.21 Caves"), "the older cave generator switch is retained");
      check(button(g, "Cave Structures"), "the underground structures are retained");
   }

   static void worldSettings() throws Exception {
      Game g = screen(480, 360);
      Screens.applyNewWorldSettings(g);
      eq(3, g.newSettings.terrainVer, "new worlds still use terrainVer 3");
      eq(5, g.newSettings.caveVer, "new worlds get the V6.31 surface-breaking cave carver");
      eq(1, g.newSettings.biomeBorders, "new worlds get smooth biome borders");
      eq(1.0F, g.newSettings.treeDensity, "new worlds copy the tree density");
      g.opt.caveCliffs = false;
      g.opt.biomeBorders = 2;
      g.opt.treeDensity = 0.6F;
      Screens.applyNewWorldSettings(g);
      eq(1, g.newSettings.caveVer, "turning Caves & Cliffs off keeps the V6.21 caves");
      eq(2, g.newSettings.biomeBorders, "very smooth borders are copied");
      eq(0.6F, g.newSettings.treeDensity, "tree density is copied");

      Properties p = new Properties();
      newWorld().save(p);
      WorldSettings back = WorldSettings.load(p);
      eq(2, back.caveVer, "the cave version round-trips");
      eq(1, back.biomeBorders, "border smoothing round-trips");
      eq(1.0F, back.treeDensity, "tree density round-trips");
      eq(2, newWorld().copy().caveVer, "copy keeps the cave version");

      p.setProperty("cavever", "99");
      p.setProperty("biomeborders", "-4");
      p.setProperty("treedensity", "8");
      WorldSettings clamped = WorldSettings.load(p);
      eq(5, clamped.caveVer, "a future cave version is clamped");
      eq(0, clamped.biomeBorders, "a bad border setting is clamped");
      eq(1.4F, clamped.treeDensity, "tree density is clamped");

      Properties legacy = new Properties();
      legacy.setProperty("gentype", "0");
      WorldSettings old = WorldSettings.load(legacy);
      eq(1, old.caveVer, "an old save keeps the V6.21 caves");
      eq(0, old.biomeBorders, "an old save keeps sharp borders");
      eq(1.0F, old.treeDensity, "an old save keeps the old tree density");

      WorldPreview pv = new WorldPreview(48, 30, 20, true);
      check(pv.update(1234L, newWorld()), "the first preview is drawn");
      WorldSettings alt = newWorld();
      alt.caveVer = 1;
      check(pv.update(1234L, alt), "changing the cave version redraws the preview");
      alt.biomeBorders = 2;
      check(pv.update(1234L, alt), "changing border smoothing redraws the preview");
      alt.treeDensity = 0.8F;
      check(pv.update(1234L, alt), "changing tree density redraws the preview");
      check(!pv.update(1234L, alt), "an unchanged key does not redraw");
   }

   // ------------------------------------------------------------------ biome borders
   static void biomeBorders() {
      long seed = 6120L;
      TerraGen sharp = new TerraGen(seed, legacy());
      TerraGen smooth = new TerraGen(seed, newWorld());
      eq(0.0, TerraGen.climateEdge(0.20, 0.50), "a climate threshold is a border");
      check(TerraGen.climateEdge(0.31, 0.41) > 0.0, "away from a threshold there is no border");

      int blended = 0;
      int alternates = 0;
      int heightDiffs = 0;
      for (int x = -240; x < 240; x += 3) {
         for (int z = -240; z < 240; z += 3) {
            Gen.Col a = sharp.column(x, z);
            Gen.Col b = smooth.column(x, z);
            if (b.blend > 0.0F) blended++;
            if (b.altBiome >= 0) alternates++;
            if (Math.abs(a.h - b.h) > 0) heightDiffs++;
            eq(-1, a.altBiome, "sharp legacy borders have no neighbour biome");
            eq(0.0F, a.blend, "sharp legacy borders have no blend weight");
         }
      }

      check(blended > 200, "a real fraction of columns lie in a border band (" + blended + ")");
      check(alternates > 100, "border columns know the biome on the other side (" + alternates + ")");
      check(heightDiffs > 100, "climate warping and terraces actually change the terrain (" + heightDiffs + ")");

      // determinism: the same column always gives the same border answer
      Gen.Col c1 = smooth.column(913, -427);
      Gen.Col c2 = smooth.column(913, -427);
      eq(c1.biome, c2.biome, "bordered biomes are deterministic");
      eq(c1.altBiome, c2.altBiome, "neighbour biomes are deterministic");
      eq(c1.blend, c2.blend, "blend weights are deterministic");

      // map colours are softened across borders for unloaded columns
      int found = 0;
      for (int x = -600; x < 600 && found < 4; x += 7) {
         Gen.Col c = smooth.column(x, 300);
         if (c.blend > 0.5F && c.altBiome >= 0 && c.h >= 63) {
            int plain = WorldPreview.color(c.biome);
            int mixed = MapView.colorAt(null, smooth, x, 300);
            check(((mixed >> 16 & 255) - (plain >> 16 & 255)) != 0
               || ((mixed >> 8 & 255) - (plain >> 8 & 255)) != 0
               || ((mixed & 255) - (plain & 255)) != 0,
               "a border column's map colour is blended toward its neighbour");
            found++;
         }
      }

      check(found > 0, "there are blended border columns on the map (" + found + ")");
      // the preview map itself uses a 3x3 colour blur when borders are smooth
      WorldPreview a = new WorldPreview(40, 30, 12, false);
      a.update(seed, legacy());
      WorldPreview b = new WorldPreview(40, 30, 12, false);
      b.update(seed, newWorld());
      Set<Integer> cb = new HashSet<>();
      for (int v : b.img.px) cb.add(v);
      check(cb.size() > 20, "the smoothed preview still has plenty of terrain detail");
      check(!Arrays.equals(a.img.px, b.img.px), "smoothed borders change the preview map");
   }

   static void worldGen() {
      long seed = 6120L;
      TerraGen sharp = new TerraGen(seed, legacy());
      TerraGen smooth = new TerraGen(seed, newWorld());
      int terraces = 0;
      int valleys = 0;
      int shores = 0;
      for (int x = -900; x <= 900; x += 11) {
         for (int z = -900; z <= 900; z += 11) {
            int a = sharp.column(x, z).h;
            int b = smooth.column(x, z).h;
            if (a > 90 && Math.abs(a - b) >= 2) terraces++;
            if (a > 62 && a < 80 && b < a - 1) valleys++;
            if (a >= 60 && a <= 66 && Math.abs(b - 62) < Math.abs(a - 62)) shores++;
         }
      }

      check(terraces > 50, "mountain terraces change real peaks (" + terraces + ")");
      check(valleys > 20, "river valleys are broadened (" + valleys + ")");
      check(shores > 20, "shorelines are flattened into beaches (" + shores + ")");
      // old worlds must be untouched: classic settings have borders = 0
      eq(0, sharp.st.biomeBorders, "the legacy generator has no border smoothing");
      Chunk a = sharp.generate(0, 0);
      Chunk b = new TerraGen(seed, legacy()).generate(0, 0);
      eq(true, Arrays.equals(a.blocks, b.blocks), "the V6.21 generator is still deterministic");
   }

   // ------------------------------------------------------------------ caves
   static void carver() {
      long seed = 6120L;
      CaveCliffs cc = new CaveCliffs(seed, newWorld());
      int air = 0, water = 0, lava = 0, solid = 0;
      for (int x = -512; x < 512; x += 11) {
         for (int z = -512; z < 512; z += 11) {
            for (int y = 4; y < 58; y += 2) {
               int v = cc.carve(x, y, z);
               if (v == CaveCliffs.AIR) air++;
               else if (v == CaveCliffs.WATER) water++;
               else if (v == CaveCliffs.LAVA) lava++;
               else solid++;
               eq(cc.carve(x, y, z), v, "the carver is a pure function of the coordinate");
            }
         }
      }

      check(air > 100, "there are real Caves & Cliffs tunnels (" + air + ")");
      check(water > 20, "aquifers fill some carved space with water (" + water + ")");
      check(lava > 0, "deep lava provinces still exist (" + lava + ")");
      check(solid > air + water + lava, "the underground is still mostly solid rock");

      int levels = 0;
      int dry = 0;
      for (int x = -512; x < 512; x += 32) {
         for (int z = -512; z < 512; z += 32) {
            int lv = CaveCliffs.aquiferLevel(seed, x, z);
            if (lv > 0) { levels++; check(lv >= 22 && lv <= 49, "aquifer levels stay in the cave layer"); }
            else dry++;
         }
      }

      check(levels > 0 && dry > 0, "some regions have aquifers and some are dry (" + levels + "/" + dry + ")");
      eq(CaveCliffs.aquiferLevel(seed, 3, 3), CaveCliffs.aquiferLevel(seed, 3, 3), "aquifer levels are deterministic");

      Set<Integer> biomes = new HashSet<>();
      for (int x = -300; x < 300; x += 17) {
         for (int z = -300; z < 300; z += 17) {
            biomes.add(cc.caveBiome(x, 30, z));
            biomes.add(cc.caveBiome(x, 8, z));
         }
      }

      check(biomes.contains(CaveCliffs.LUSH) && biomes.contains(CaveCliffs.DRIPSTONE),
         "lush and dripstone cave biomes exist");
      check(biomes.contains(CaveCliffs.DEEP), "the deep dark exists below y=14");
      check(biomes.contains(CaveCliffs.ICE) || biomes.contains(CaveCliffs.AMETHYST),
         "ice caves or amethyst provinces exist");
      int wells = 0;
      for (int x = -512; x < 512; x += 3) {
         for (int z = -512; z < 512; z += 3) if (CaveCliffs.well(seed, x, z)) wells++;
      }

      check(wells > 0 && wells < 1024 * 1024 / 9 / 8, "vertical wells are rare but real (" + wells + ")");
   }

   static void generatedWorld() {
      long seed = 6120L;
      WorldSettings st = newWorld();
      st.structures = false;
      TerraGen tg = new TerraGen(seed, st);
      int water = 0, geode = 0, lush = 0, drip = 0, ice = 0, chest = 0;
      for (int cx = -4; cx <= 4; cx++) {
         for (int cz = -4; cz <= 4; cz++) {
            Chunk c = tg.generate(cx, cz);
            water += count(c, Blocks.WATER);
            if (has(c, Blocks.OBSIDIAN) || has(c, Redstone319.PURPUR_BLOCK)) geode++;
            if (has(c, Blocks.MOSSY_COBBLE) || has(c, Blocks313.VINE0 + 1) || has(c, Blocks.GLOWSTONE)) lush++;
            if (has(c, Blocks313.GRANITE) || has(c, Blocks313.DIORITE)) drip++;
            if (has(c, Blocks313.ICE) || has(c, Blocks313.SNOW_LAYER)) ice++;
            if (has(c, Blocks.CHEST_LOOT)) chest++;
         }
      }

      check(water > 100, "generated chunks contain aquifer water (" + water + ")");
      check(geode > 0, "amethyst geodes are stamped into real chunks (" + geode + ")");
      check(lush > 0, "lush cave decorations appear (" + lush + " chunks)");
      check(drip > 0, "dripstone / strata appear (" + drip + " chunks)");
      check(chest > 0, "underground structures are still generated (" + chest + " chunks)");

      Chunk a = tg.generate(7, -3);
      Chunk b = new TerraGen(seed, newWorld()).generate(7, -3);
      eq(true, Arrays.equals(a.blocks, b.blocks), "V6.22 generation is deterministic");
      check(a.hm[0] >= 0, "the heightmap is recomputed after cave decoration");

      // Old V6.21 worlds keep their caves bit for bit.
      WorldSettings v621 = new WorldSettings();
      v621.terrainVer = 3;
      v621.structures = false;
      Chunk legacyA = new TerraGen(seed, v621).generate(7, -3);
      WorldSettings v621b = v621.copy();
      Chunk legacyB = new TerraGen(seed, v621b).generate(7, -3);
      eq(true, Arrays.equals(legacyA.blocks, legacyB.blocks), "V6.21 saves keep their exact generator");
      check(!Arrays.equals(a.blocks, legacyA.blocks), "the V6.22 carver really is a different underground");
   }

   // ------------------------------------------------------------------ trees
   static void spruceTrees() {
      TerraTrees.Buf buf = new TerraTrees.Buf();
      int max = 0;
      int total = 0;
      for (int i = 0; i < 12; i++) {
         buf.begin(0, 70, 0);
         TerraTrees.classic(buf, new java.util.Random(i * 31L + 7L), 2, i % 2);
         int n = buf.nt;
         total += n;
         if (n > max) max = n;
      }

      check(total / 12 > 40, "a spruce still has a real crown (" + total / 12 + " blocks)");
      check(max < 420, "no spruce is a solid thousand-block cone (max " + max + ")");

      buf.begin(0, 70, 0);
      TerraTrees.megaSpruce(buf, new java.util.Random(99L));
      check(buf.nt > 60 && buf.nt < 900, "the giant spruce is tiered, not solid (" + buf.nt + " blocks)");

      long seed = 6120L;
      WorldSettings st = newWorld();
      TerraGen tg = new TerraGen(seed, st);
      int leaves = 0;
      int tx = 0, tz = 0;
      outer:
      for (int x = -1200; x < 1200; x += 5) {
         for (int z = -1200; z < 1200; z += 5) {
            int b = tg.column(x, z).biome;
            if (b == Biomes.TAIGA || b == Biomes.MEGA_TAIGA || b == Biomes.SNOWY_TAIGA) {
               tx = x; tz = z;
               break outer;
            }
         }
      }

      Chunk c = tg.generate(Math.floorDiv(tx, 16), Math.floorDiv(tz, 16));
      for (short s : c.blocks) {
         if ((s & BlockState.ID_MASK) == Blocks.LEAVES_SPRUCE) leaves++;
      }

      check(leaves < 450, "a taiga chunk no longer carries a thousand spruce leaves (" + leaves + ")");
      check(tg.treePlan(Biomes.TAIGA, 0.05F, 900, 1.0) >= 0, "taiga still grows spruces");
      eq(-1, tg.treePlan(Biomes.TAIGA, 0.60F, 900, 1.0), "taiga does not grow a tree on every column");
   }

   // ------------------------------------------------------------------ shadows
   static void entityShadows() {
      Options o = new Options(new File(tmp, "shadows"));
      o.entityShadows = true;
      o.entityShadowMotion = true;
      check(Renderer.entityShadowsNeedFrame(o, true, true), "shadows re-stamp on a live frame");
      check(Renderer.entityShadowsNeedFrame(o, true, true), "and again on the next frame");
      eq(false, Renderer.entityShadowsNeedFrame(o, false, true), "no shadow map, no entity pass");
      eq(false, Renderer.entityShadowsNeedFrame(o, true, false), "no static depth, no entity pass");
      o.entityShadowMotion = false;
      eq(false, Renderer.entityShadowsNeedFrame(o, true, true), "the old behaviour is still available");
      o.entityShadowMotion = true;
      o.entityShadows = false;
      eq(false, Renderer.entityShadowsNeedFrame(o, true, true), "entity shadows off disables the pass");
      eq(false, Renderer.entityShadowsNeedFrame(null, true, true), "a missing Options disables the pass");

      Shaders sh = new Shaders();
      eq(0, sh.staticFbo, "no GL targets exist before a context is created");
      eq(false, sh.staticValid, "the static depth starts invalid");
      sh.mapValid = true;
      sh.saveStaticShadow();
      eq(true, sh.staticValid, "saving the terrain depth marks it usable");
   }

   // ------------------------------------------------------------------ minimap
   static void minimapPositions() throws Exception {
      float[] out = new float[2];
      int size = 96;
      MapView.miniOrigin(480, 300, size, 0, false, true, out);
      eq(480.0F - size - 6.0F, out[0], "top right x");
      eq(6.0F, out[1], "top right y");
      MapView.miniOrigin(480, 300, size, 1, false, true, out);
      eq(6.0F, out[0], "top left x");
      MapView.miniOrigin(480, 300, size, 2, false, true, out);
      eq(300.0F - size - 24.0F, out[1], "bottom right y");
      MapView.miniOrigin(480, 300, size, 3, false, true, out);
      eq(6.0F, out[0], "bottom left x");
      check(MapView.miniOrigin(480, 300, size, 3, false, true, out)[1] > 100.0F, "bottom left is low");

      MapView.miniOrigin(480, 300, size, 0, true, true, out);
      eq(6.0F + MapView.TOAST_H, out[1], "an achievement toast slides a top-right minimap down");
      MapView.miniOrigin(480, 300, size, 0, true, false, out);
      eq(6.0F, out[1], "the dodge can be turned off");
      MapView.miniOrigin(480, 300, size, 1, true, true, out);
      eq(6.0F, out[1], "a top-left minimap is already clear of the toast");
      MapView.miniOrigin(480, 300, size, 2, true, true, out);
      eq(300.0F - size - 24.0F, out[1], "a bottom minimap is already clear of the toast");
      eq(4, MapView.POS_NAME.length, "there are four minimap corners");

      // Draw it into the real HUD raster in every corner, then with a toast.
      Game g = GameTests.newGame(1);
      GuiRaster r = new GuiRaster(960, 600);
      g.gui = r;
      g.opt = new Options(new File(tmp, "miniopt"));
      g.fbW = 960;
      g.fbH = 600;
      g.winW = 960;
      g.winH = 600;
      r.begin(960, 600, 3);
      g.activeBtnsSwap();
      g.setScreen(Game.S.PLAY);
      g.map.update(g);
      int[] painted = new int[4];
      for (int p = 0; p < 4; p++) {
         g.opt.minimapPos = p;
         r.fill(0);
         g.map.drawMini(g);
         for (int c : r.px) if (c != 0) painted[p]++;
      }

      for (int p = 0; p < 4; p++) check(painted[p] > 1000, "corner " + p + " paints the minimap (" + painted[p] + ")");

      g.opt.minimapPos = 0;
      g.opt.minimapAutoMove = true;
      g.adv.shown = Adv312.LIST.get(0);
      r.fill(0);
      g.map.drawMini(g);
      int moved = 0;
      for (int y = 0; y < 6 * 3; y++) {
         for (int x = 700; x < 950; x++) if (r.px[y * 960 + x] != 0) moved++;
      }

      eq(0, moved, "the minimap is no longer drawn under the advancement toast");
      g.adv.shown = null;
   }
}
