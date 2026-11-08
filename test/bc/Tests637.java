package bc;

import static bc.GameTests.check;
import static bc.GameTests.eq;
import static bc.GameTests.test;

import java.io.File;
import java.nio.file.Files;

/**
 * V6.37: the rendering repair the user asked for after V6.36 - lifted clouds and
 * weather, sections above y=255, no shader/effects burn-in, and a Minecraft-like
 * (but better) UI with the shader and effects controls in normal Settings.
 */
final class Tests637 {
   private Tests637() {
   }

   static void run() {
      test("6.37: clouds, weather and the shadow plane follow the lifted sea level", Tests637::heights);
      test("6.37: normal chunks render above the old y=256 ceiling", Tests637::sections);
      test("6.37: enabling shaders or changing effects quality clears stale buffers", Tests637::burnIn);
      test("6.37: menus are Minecraft-style and the world-map preview is kept", Tests637::menus);
      test("6.37: Shaders OFF/ON/FANCY and Effects LOW/MEDIUM/HIGH are in normal settings", Tests637::settings);
      test("6.37: version, notes, preview and release wiring point at 6.37", Tests637::wiring);
   }

   static String src(String p) throws Exception {
      return new String(Files.readAllBytes(new File(p).toPath()), "UTF-8");
   }

   // ------------------------------------------------------------------ lifted heights
   static void heights() throws Exception {
      String r = src("src/bc/Renderer.java");
      check(r.contains("(infdev ? 108.0 : 130.0) + Gen.depth()"),
         "the plain cloud layer is lifted by the world's depth");
      check(r.contains("Math.min(Chunk.SECTIONS - 1"), "the camera's section is clamped to the real section count");
      check(!r.contains("var35 < 16") && !r.contains("var9 < 16"),
         "the visible-section and shadow loops are not stuck at sixteen sections");
      String wx = src("src/bc/WeatherFx.java");
      check(wx.contains("final float lift = world == null || world.settings == null ? 0.0F : (float)world.settings.depth"),
         "the CPU cloud mesh is built at the world's own depth");
      check(wx.contains("Weather.baseAlt(storm, (int)lift)") && wx.contains("Weather.baseAlt(o3[2], (int)lift)"),
         "cloud bases and rain curtains are lifted");
      check(wx.contains("this.slabLo = this.lift +") && wx.contains("this.slabHi = this.lift +"),
         "the shader's cloud slab is lifted");
      check(wx.contains("Weather.BASE_FAIR + this.lift"), "sun visibility and sky gloom use the lifted cloud plane");
      String w = src("src/bc/Weather.java");
      check(w.contains("public static float baseAlt(float storm, int lift)"), "a lifted cloud-base helper exists");
      eq(Weather.BASE_FAIR + 128.0F, Weather.baseAlt(0.0F, 128), "a deep world's fair-weather clouds are 128 higher");
      eq(Weather.BASE_STORM + 128.0F, Weather.baseAlt(1.0F, 128), "and storm clouds too");
      eq(Weather.BASE_FAIR, Weather.baseAlt(0.0F, 0), "classic worlds keep the old cloud height");
      String dec = src("src/bc/ShaderSrc.java");
      check(dec.contains("uniform float cloudLift") && dec.contains("mix(168.0, 146.0, storm) + cloudLift"),
         "the raymarched cloud shader adds the same lift");
      String sh = src("src/bc/Shaders.java");
      check(sh.contains("static float csY() { return Gen.SEA + 1.0F; }"),
         "cloud shadows are projected onto the lifted sea plane, not y=64");
      check(sh.contains("this.u1f(prog, \"cloudLift\", fx.lift)"), "the lift reaches both cloud programs");
      check(sh.contains("this.u1f(prog, \"csY\", csY())") && sh.contains("this.u1f(this.pCShadow, \"csY\", csY())"),
         "every cloud-shadow lookup uses the lifted plane");
      String b = src("src/bc/Weather.java");
      check(b.contains("return Gen.SEA + 1;") && b.contains("this.world.settings.depth"),
         "lightning tops and the ground fallback follow the lifted sea level");
   }

   // ------------------------------------------------------------------ sections above y=255
   static void sections() throws Exception {
      eq(384, Chunk.H, "new worlds are 384 blocks tall");
      eq(24, Chunk.SECTIONS, "there are 24 vertical sections");
      String cull = src("src/bc/CaveCull.java");
      check(cull.contains("static final int S = Chunk.SECTIONS"), "the culling walk indexes all sections");
      check(cull.contains("ny >= S") && cull.contains("this.seen[cell * S + sy] != 0"),
         "the walk reaches and caches sections above the old ceiling");
      check(!cull.contains("this.seen[cell * 16 + sy]"), "no sixteen-section cache is left");
      // actually walk a fully open deep world and ask for a high section
      World w = new World(1, null);
      w.saveEnabled = false;
      for (int cz = -3; cz <= 3; cz++) {
         for (int cx = -3; cx <= 3; cx++) {
            w.chunks.put(World.key(cx, cz), new Chunk(cx, cz));
         }
      }
      CaveCull.Walk walk = new CaveCull.Walk();
      walk.run(w, 0, 20, 0, 2, 8, 328, 8, null);
      check(walk.reached(0, 0, 20), "the camera's own section at y=320..335 is reached");
      check(walk.reached(1, 0, 23), "a neighbouring top section at y=368..383 is reached");
      check(!walk.reached(0, 0, 24), "and a section beyond the world is not");
      int top = 0;
      for (int i = 0; i < walk.count; i++) {
         top = Math.max(top, walk.sy(walk.order[i]));
      }
      eq(23, top, "the walk reaches the highest section of a deep world");
      String lod = src("src/bc/LodMap.java");
      check(lod.contains("Math.min(Chunk.H - 1, blocks.length / 256 - 1)"),
         "LOD summaries start at the real top of the chunk, not y=255");
      String map = src("src/bc/MapView.java");
      check(map.contains("h < Chunk.H"), "the big map samples blocks above y=255");
      for (String f : new String[]{"Fire.java", "Trees.java", "Shapes.java", "Disasters.java",
         "Mobs312.java", "Redstone319.java", "AllBlocksGen.java", "SkyGen.java"}) {
         check(src("src/bc/" + f).contains("Chunk.H"), f + " uses the real world height");
      }
   }

   // ------------------------------------------------------------------ burn-in
   static void burnIn() throws Exception {
      String sh = src("src/bc/Shaders.java");
      check(sh.contains("public void syncOptions(Options o)"), "shader options are synchronised each frame");
      check(sh.contains("private static long optionSignature(Options o)"), "a signature covers every graphics option");
      for (String opt : new String[]{"o.shaders", "o.fx", "o.waterQ", "o.bloom", "o.renderScale",
         "o.shadowRes", "o.cloudRes", "o.smooth", "o.leaves"}) {
         check(sh.contains(opt), "the signature includes " + opt);
      }
      check(sh.contains("public void invalidate()"), "there is one explicit invalidate");
      check(sh.contains("private void clearTargets()"), "auxiliary targets are cleared");
      for (String fbo : new String[]{"sceneFbo", "bloomFbo0", "bloomFbo5", "aoFbo", "aoFbo2",
         "rayFbo", "cloudFbo", "copyFbo", "csFbo", "shadowFbo", "staticFbo"}) {
         check(sh.contains(fbo), "the clear covers " + fbo);
      }
      check(sh.contains("this.mapValid = false") && sh.contains("this.staticValid = false")
         && sh.contains("this.resizeQueued = true"), "invalidation rebuilds the shadow maps and targets");
      check(sh.contains("// V6.37: the new textures have undefined contents"), "freshly allocated targets are cleared too");
      String game = src("src/bc/Game.java");
      check(game.contains("this.rend.sh.syncOptions(this.opt)"),
         "the render loop calls the synchronisation before the scene begins");
      check(src("src/bc/Game.java").contains("this.rend.computeSky") , "and before the sky is computed");
   }

   // ------------------------------------------------------------------ menus
   static void menus() throws Exception {
      String m = src("src/bc/Menus635.java");
      for (String bad : new String[]{"NAV_SUB", "card(Game g", "ATLAS UI", "ESC = BACK",
         "DEEP WORLD  /", "cyan", "01  /  WORLD"}) {
         check(!m.contains(bad), "no '" + bad + "' from the two rejected menu overhauls");
      }
      check(m.contains("static Box shell(Game g, String title, String sub)"), "one shell draws every screen");
      check(m.contains("ui.backdrop(0.30F)") && m.contains("gui.gradient(0.0F, 0.0F, W, 58.0F"),
         "the panorama / world stays visible behind dark top and bottom bands");
      check(!m.contains("gui.gradient(px, py, pw, ph"), "the rejected framed panel is gone");
      check(m.contains("Math.min(200.0F, W - 12.0F)"), "a classic centred 200-unit button column");
      check(m.contains("float x, y, w, h, foot;\n      float fx, fw;"),
         "forms and lists can use the full width while menu buttons stay centred");
      check(m.contains("gui.text(\"Blockcraft V\" + Game.VERSION_STR") && m.contains("Original game - not Mojang"),
         "the footer is quiet, like Minecraft's, and says what the game is");
      check(m.contains("ui.preview.update(Screens.seedValue(g), ws)") && m.contains("gui.image(ui.preview.img"),
         "Create New World still draws the live world-map preview");
      check(m.contains("ui.preview.topBiomes(8)") && m.contains("\"Biomes: \""),
         "the biome key and its number are still shown");
      check(m.contains("ws.depth = g.opt.deepWorld"), "the preview uses the real world depth");
      String scenes = src("test/bc/Scenes.java");
      String ui = src("test/bc/TestsUi.java");
      for (String scene : new String[]{"mc-title", "mc-video", "mc-newworld", "mc-worlds", "mc-advanced"}) {
         check(scenes.contains("case \"" + scene + "\""), "the " + scene + " screen has a headless scene");
         check(ui.contains("\"" + scene + "\""), "and is checked at all seven window sizes");
      }
      check(new File("docs/screenshots/v6.37-menus.png").isFile(), "the V6.37 menu board exists");
   }

   // ------------------------------------------------------------------ settings
   static void settings() throws Exception {
      String m = src("src/bc/Menus635.java");
      check(m.contains("choiceRow(r, \"Shaders\", new String[]{\"OFF\", \"ON\", \"FANCY\"}"),
         "Shaders has an OFF / ON / FANCY choice in the ordinary Video tab");
      check(m.contains("o.shaders = v > 0; o.waterQ = v == 2 ? 2 : 1;"),
         "FANCY really turns the shader pack on with mirrored water");
      check(m.contains("choiceRow(r, \"Effects\", new String[]{\"LOW\", \"MEDIUM\", \"HIGH\"}"),
         "Effects LOW / MEDIUM / HIGH is still editable in the ordinary Video tab");
      check(m.contains("v -> o.fx = v"), "changing Effects writes the real option");
      check(m.contains("Screens.advancedTab"), "Debug Advanced still exposes every video setting");
      check(m.contains("static void numberRow(") && m.contains("static void sliderRow("),
         "stepped settings and sliders still print their numbers");
      check(m.contains("\"Every setting shows its current value\""), "the promise of visible values is kept");
      check(m.contains("\"Reset to defaults\"") && m.contains("\"Back\""), "obvious buttons are still there");
      check(!m.contains("\"Gfx\"") && !m.contains("\"Adv\""), "no cryptic two-letter abbreviations");
   }

   // ------------------------------------------------------------------ wiring
   static void wiring() throws Exception {
      eq("6.37", Game.VERSION_STR, "the game version is 6.37");
      eq("6.37", src("ci/release-trigger.txt").trim(), "the release workflow publishes 6.37");
      check(src("README.md").contains("V6.37"), "README describes V6.37");
      check(src("package/README.txt").contains("WHAT'S NEW IN V6.37"), "the packaged readme describes V6.37");
      check(new File("docs/releases/v6.37.md").isFile(), "V6.37 release notes exist");
      check(src("tools/make_release.py").contains("if version == \"6.37\""), "GitHub release notes describe 6.37");
      check(src("docs/FILE-LAYOUT.md").contains("Menus635"), "the layout doc still lists the UI class");
   }
}
