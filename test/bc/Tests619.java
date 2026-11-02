package bc;

import static bc.GameTests.check;
import static bc.GameTests.eq;
import static bc.GameTests.test;

import java.io.File;
import java.nio.file.Files;
import java.util.Properties;

/**
 * Update 6.19 regressions: moon/entity/hand lighting, LOD falloff & budgets, the new world-generation knobs
 * and their persistence. Headless: no OpenGL, no display.
 */
final class Tests619 {
   private Tests619() {
   }

   static File tmp;

   static void run() {
      try {
         tmp = Files.createTempDirectory("bc619").toFile();
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
      test("6.19: all new options persist and are clamped", Tests619::options);
      test("6.19: the Advanced tab exposes light, LOD and worldgen settings", Tests619::advancedTab);
      test("6.19: new-world settings round-trip and old saves stay classic", Tests619::worldSettings);
      test("6.19: the moon takes over the shadow light at night", Tests619::moonLight);
      test("6.19: entity shadow silhouettes are sized sensibly", Tests619::entityShadows);
      test("6.19: LOD falloff, fog and detail helpers", Tests619::lodHelpers);
      test("6.19: bigger caves and taller mountains for new worlds", Tests619::terrain);
   }

   // --------------------------------------------------------------- options
   static void options() throws Exception {
      File dir = new File(tmp, "opt");
      dir.mkdirs();
      Options fresh = new Options(dir);
      eq(true, fresh.moonShadows, "moon shadows on by default");
      eq(true, fresh.entityShadows, "entity shadows on by default");
      eq(true, fresh.handLighting, "shader hand lighting on by default");
      eq(1.0F, fresh.lodFalloff, "LOD falloff is 1 by default");
      eq(0.35F, fresh.lodFogStart, "LOD fog starts at 35%");
      eq(1.0F, fresh.lodFogEnd, "LOD fog ends at the horizon");
      eq(8, fresh.lodMaxUploads, "LOD uploads are capped by default");
      eq(4, fresh.lodMaxRequests, "LOD build requests are capped by default");
      eq(true, fresh.newTerrain, "new worlds use the V6.19 generator");

      Options o = new Options(dir);
      o.moonShadows = false;
      o.moonBrightness = 0.73F;
      o.entityShadows = false;
      o.handLighting = false;
      o.lodFalloff = 0.62F;
      o.lodFogStart = 0.2F;
      o.lodFogEnd = 0.8F;
      o.lodDetail = 2;
      o.lodMaxUploads = 3;
      o.lodMaxRequests = 9;
      o.lodMaxDraws = 220;
      o.lodWorkers = 3;
      o.newTerrain = false;
      o.caveScale = 2.1F;
      o.caveDensity = 0.8F;
      o.mountainScale = 2.2F;
      o.ravineScale = 0.7F;
      o.save();
      Options back = new Options(dir);
      back.load();
      eq(false, back.moonShadows, "moon shadows persisted");
      eq(0.73F, back.moonBrightness, "moon brightness persisted");
      eq(false, back.entityShadows, "entity shadows persisted");
      eq(false, back.handLighting, "hand lighting persisted");
      eq(0.62F, back.lodFalloff, "LOD falloff persisted");
      eq(0.2F, back.lodFogStart, "LOD fog start persisted");
      eq(0.8F, back.lodFogEnd, "LOD fog end persisted");
      eq(2, back.lodDetail, "LOD detail persisted");
      eq(3, back.lodMaxUploads, "LOD upload cap persisted");
      eq(9, back.lodMaxRequests, "LOD request cap persisted");
      eq(220, back.lodMaxDraws, "LOD draw cap persisted");
      eq(3, back.lodWorkers, "LOD workers persisted");
      eq(false, back.newTerrain, "new terrain switch persisted");
      eq(2.1F, back.caveScale, "cave scale persisted");
      eq(0.8F, back.caveDensity, "cave density persisted");
      eq(2.2F, back.mountainScale, "mountain scale persisted");
      eq(0.7F, back.ravineScale, "ravine scale persisted");

      Files.write(new File(dir, "options.txt").toPath(),
         ("lodFalloff=99\nlodFogStart=-5\nlodFogEnd=0.01\nlodDetail=42\nlodMaxUploads=0\nmoonBrightness=7\ncaveScale=-1\n").getBytes("UTF-8"));
      Options c = new Options(dir);
      c.load();
      eq(2.0F, c.lodFalloff, "a huge falloff is clamped");
      eq(0.0F, c.lodFogStart, "a negative fog start is clamped");
      check(c.lodFogEnd > c.lodFogStart, "fog end always stays after fog start");
      eq(2, c.lodDetail, "LOD detail is clamped");
      eq(1, c.lodMaxUploads, "the upload cap cannot be zero");
      eq(1.0F, c.moonBrightness, "moon brightness is clamped");
      eq(0.6F, c.caveScale, "cave scale is clamped");
   }

   // --------------------------------------------------------------- advanced tab
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

   static void advancedTab() throws Exception {
      Game g = screen(343, 1600); // tall enough for every one of the 44 scrolling rows
      g.opt.debugVideo = true;
      g.ui.optTab = 4;
      Screens.options(g);
      eq(5, Screens.tabs(g.opt).length, "the debug switch still shows five tabs");
      check(button(g, "Moon Shadows"), "moon shadows are configurable");
      check(button(g, "Entity Shadows"), "entity shadows are configurable");
      check(button(g, "Hand Lighting"), "shader hand lighting is configurable");
      check(button(g, "Moon Light"), "moon brightness is configurable");
      check(button(g, "LOD Falloff"), "LOD falloff is configurable");
      check(button(g, "LOD Fog Start"), "LOD fog start is configurable");
      check(button(g, "LOD Fog End"), "LOD fog end is configurable");
      check(button(g, "LOD Detail"), "LOD detail is configurable");
      check(button(g, "LOD Builds/Frame"), "LOD build budget is configurable");
      check(button(g, "LOD Uploads/Frame"), "LOD upload budget is configurable");
      check(button(g, "LOD Draw Cap"), "LOD draw cap is configurable");
      check(button(g, "LOD Workers"), "LOD workers are configurable");
      check(button(g, "V6.21 Caves"), "the layered cave generator is configurable");
      check(button(g, "Cave Size"), "cave size is configurable");
      check(button(g, "Cave Density"), "cave density is configurable");
      check(button(g, "Mountain Scale"), "mountain scale is configurable");
      check(button(g, "Ravine Scale"), "ravine scale is configurable");

      for (Game.Btn b : g.btns) {
         if (b.label != null && b.label.startsWith("LOD Falloff") && b.slider != null) {
            b.slider.accept(0.0F);
            break;
         }
      }
      eq(0.4F, g.opt.lodFalloff, "the LOD falloff slider changes the option");

      Screens.resetAdvanced(g.opt);
      eq(1.0F, g.opt.lodFalloff, "reset restores LOD falloff");
      eq(8, g.opt.lodMaxUploads, "reset restores the LOD upload cap");
      eq(true, g.opt.moonShadows, "reset restores moon shadows");
      eq(true, g.opt.newTerrain, "reset restores new terrain");

      Screens.applyNewWorldSettings(g);
      eq(3, g.newSettings.terrainVer, "a new world gets the V6.21 layered caves");
      eq(1.6F, g.newSettings.caveScale, "the cave size is copied into the new world");
      g.opt.newTerrain = false;
      Screens.applyNewWorldSettings(g);
      eq(1, g.newSettings.terrainVer, "turning the switch off keeps the old generator for new worlds");
   }

   // --------------------------------------------------------------- world settings
   static void worldSettings() {
      WorldSettings s = new WorldSettings();
      s.terrainVer = 2;
      s.caveScale = 1.9F;
      s.caveDensity = 1.4F;
      s.mountainScale = 1.7F;
      s.ravineScale = 1.2F;
      Properties p = new Properties();
      s.save(p);
      WorldSettings b = WorldSettings.load(p);
      eq(2, b.terrainVer, "terrain version round-trips");
      eq(1.9F, b.caveScale, "cave scale round-trips");
      eq(1.4F, b.caveDensity, "cave density round-trips");
      eq(1.7F, b.mountainScale, "mountain scale round-trips");
      eq(1.2F, b.ravineScale, "ravine scale round-trips");
      eq(s.mapId, b.mapId, "the other settings are untouched");

      Properties old = new Properties();
      old.setProperty("gentype", "1");
      old.setProperty("aquatic", "true");
      WorldSettings legacy = WorldSettings.load(old);
      eq(1, legacy.terrainVer, "an old save has no new terrain");
      eq(1.0F, legacy.caveScale, "an old save keeps its cave size");
      eq(1.0F, legacy.mountainScale, "an old save keeps its mountains");

      WorldSettings copy = s.copy();
      eq(2, copy.terrainVer, "copy keeps the terrain version");
      eq(1.9F, copy.caveScale, "copy keeps the cave scale");

      WorldPreview pv = new WorldPreview(48, 30, 20, true);
      WorldSettings w1 = new WorldSettings();
      WorldSettings w2 = new WorldSettings();
      w2.terrainVer = 2;
      w2.caveScale = 2.4F;
      check(pv.update(1234L, w1), "the first preview is drawn");
      check(pv.update(1234L, w2), "a terrain-setting change redraws the preview");
      check(!pv.update(1234L, w2), "an unchanged key does not redraw");
   }

   // --------------------------------------------------------------- light
   static void moonLight() {
      Shaders sh = new Shaders();
      Options o = new Options(new File(tmp, "light"));
      o.moonShadows = true;
      o.moonBrightness = 0.45F;

      sh.setLight(0.0F, true, o); // noon
      eq(0.0F, sh.moonF, "no moon by day");
      eq(0.0F, sh.moonUp, "no moonlight by day");
      check(sh.sunY > 0.9F, "the sun is up by day");
      check(sh.lightUp > 0.9F, "full daylight strength");

      sh.setLight((float)Math.PI, true, o); // midnight
      eq(1.0F, sh.moonF, "the moon takes over at night");
      check(sh.sunY > 0.9F, "the active light points up at night (the moon)");
      check(sh.moonUp > 0.4F && sh.moonUp < 0.5F, "moonlight is dim");
      check(sh.lightUp > 0.4F && sh.lightUp < 0.5F, "moonlight drives the shader's sunUp");
      eq(-1.0F, sh.realSunY, "the true time of day is kept for the fog glow");

      o.moonShadows = false;
      sh.setLight((float)Math.PI, true, o);
      eq(0.0F, sh.moonF, "moon shadows can be turned off");
      eq(0.0F, sh.lightUp, "no directional light without moon shadows at night");
      check(sh.sunY < -0.9F, "the sun is below the horizon again");

      o.moonBrightness = 0.8F;
      o.moonShadows = true;
      sh.setLight((float)Math.PI, true, o);
      check(sh.moonUp > 0.75F, "moon brightness is configurable");
   }

   static void entityShadows() {
      check(Renderer.entityShadowWidth(Mobs.CHICKEN) < Renderer.entityShadowWidth(Mobs.COW), "a chicken casts a narrower shadow than a cow");
      check(Renderer.entityShadowHeight(Mobs.IRON_GOLEM) > Renderer.entityShadowHeight(Mobs.COW), "an iron golem casts a taller shadow");
      check(Renderer.entityShadowWidth(Mobs.SPIDER) > Renderer.entityShadowWidth(Mobs.COW), "a spider is wide");
      check(Renderer.entityShadowHeight(Mobs.SPIDER) < Renderer.entityShadowHeight(Mobs.COW), "a spider is low");
      for (int t = 0; t < Mobs.N; t++) {
         check(Renderer.entityShadowWidth(t) > 0.0F && Renderer.entityShadowHeight(t) > 0.0F, "mob " + t + " has a shadow silhouette");
      }
   }

   // --------------------------------------------------------------- LOD
   static void lodHelpers() {
      eq(0.70, Lod.detailFactor(0), "coarse LOD splits less");
      eq(1.0, Lod.detailFactor(1), "normal LOD is unchanged");
      eq(1.35, Lod.detailFactor(2), "fine LOD splits further out");
      eq(1.35, Lod.detailFactor(99), "an out-of-range detail index is clamped");
      Options o = new Options(new File(tmp, "lod"));
      o.lodFogEnd = 0.5F;
      eq(512, Lod.fogFarBlocks(1024, o), "a 50% fog end culls half the LOD horizon");
      o.lodFogEnd = 1.0F;
      eq(1024, Lod.fogFarBlocks(1024, o), "a 100% fog end keeps the whole horizon");
      eq(1024, Lod.fogFarBlocks(1024, null), "no options keeps the whole horizon");
      o.lodFogEnd = 0.0F;
      eq(64, Lod.fogFarBlocks(1024, o), "even a zero fog end keeps a small ring");
   }

   // --------------------------------------------------------------- terrain
   static void terrain() {
      long seed = 424242L;
      WorldSettings oldWs = new WorldSettings();
      oldWs.type = WorldSettings.DEFAULT;
      oldWs.terrainVer = 1;
      WorldSettings newWs = new WorldSettings();
      newWs.type = WorldSettings.DEFAULT;
      newWs.terrainVer = 2;
      newWs.caveScale = 1.6F;
      newWs.caveDensity = 1.3F;
      newWs.mountainScale = 1.5F;
      newWs.ravineScale = 1.3F;
      TerraGen oldGen = new TerraGen(seed, oldWs);
      TerraGen newGen = new TerraGen(seed, newWs);
      Chunk a = oldGen.generate(0, 0);
      Chunk b = newGen.generate(0, 0);
      int airOld = 0, airNew = 0;
      for (int y = 6; y <= 55; y++) {
         for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
               if (a.get(x, y, z) == 0) airOld++;
               if (b.get(x, y, z) == 0) airNew++;
            }
         }
      }
      check(airNew > airOld, "the new generator carves more underground space (" + airOld + " -> " + airNew + ")");

      int different = 0, tall = 0, tallOld = 0, bounded = 0;
      for (int z = -8; z <= 8; z++) {
         for (int x = -8; x <= 8; x++) {
            int ho = oldGen.column(x * 24, z * 24).h;
            int hn = newGen.column(x * 24, z * 24).h;
            if (ho != hn) different++;
            if (hn > 120) tall++;
            if (ho > 120) tallOld++;
            if (hn >= 4 && hn <= 245) bounded++;
         }
      }
      check(different > 0, "the new generator changes the surface in places (" + different + " columns)");
      check(tall >= tallOld, "the new generator has at least as many high peaks (" + tallOld + " -> " + tall + ")");
      eq(289, bounded, "every sampled height stays inside the world");

      // old worlds must be byte-for-byte deterministic
      TerraGen again = new TerraGen(seed, oldWs);
      Chunk a2 = again.generate(0, 0);
      eq(true, java.util.Arrays.equals(a.blocks, a2.blocks), "the classic terrain is unchanged");
   }
}
