package bc;

import static bc.GameTests.check;
import static bc.GameTests.eq;
import static bc.GameTests.test;

import java.io.File;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

/**
 * Update 6.20 regressions: the two maps, their settings, and the title panorama that used to stay
 * black. Headless: no OpenGL, no display - every picture is drawn by the real code into GuiRaster.
 */
final class Tests620 {
   private Tests620() {
   }

   static File tmp;

   static void run() {
      try {
         tmp = Files.createTempDirectory("bc620").toFile();
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
      test("6.20: map options survive options.txt and are clamped", Tests620::persistence);
      test("6.20: the Advanced tab exposes every map setting", Tests620::advancedTab);
      test("6.20: map colours come from chunks and the generator", Tests620::colours);
      test("6.20: the minimap draws real terrain and a player marker", Tests620::minimap);
      test("6.20: the big map fills in bounded slices and is pannable", Tests620::bigMap);
      test("6.20: M opens and closes the big map, wheel zooms", Tests620::keys);
      test("6.20: the panorama fades in without waiting for hidden meshes", Tests620::panoramaReadiness);
      test("6.20: the title always has a landscape behind it", Tests620::panoramaFallback);
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

   static Game worldGame(int gw, int gh) throws Exception {
      Game g = GameTests.newGame(1);
      GuiRaster r = new GuiRaster(gw * 3, gh * 3);
      g.gui = r;
      new File(tmp, "world").mkdirs();
      g.opt = new Options(new File(tmp, "world"));
      g.fbW = gw * 3;
      g.fbH = gh * 3;
      g.winW = gw * 3;
      g.winH = gh * 3;
      r.begin(gw * 3, gh * 3, 3);
      g.activeBtnsSwap();
      g.mouseX = -1000;
      g.mouseY = -1000;
      g.player.yaw = 0.0F;
      return g;
   }

   static boolean button(Game g, String starts) {
      for (Game.Btn b : g.btns) {
         if (b.label != null && b.label.startsWith(starts)) return true;
      }
      return false;
   }

   // ------------------------------------------------------------------ settings
   static void persistence() throws Exception {
      File dir = new File(tmp, "opt");
      dir.mkdirs();
      Options fresh = new Options(dir);
      eq(true, fresh.minimap, "the minimap is on by default");
      eq(false, fresh.minimapRotate, "north-up minimap by default");
      eq(true, fresh.minimapMobs, "mob dots are on by default");
      eq(1, fresh.minimapSize, "normal minimap size by default");
      eq(128, fresh.minimapRadius, "128-block minimap radius by default");
      eq(3, fresh.mapRefresh, "minimap redraws every third frame by default");
      eq(2, fresh.bigMapZoom, "8 blocks per pixel on the big map by default");
      eq(true, fresh.mapGrid, "the map grid is on by default");

      fresh.minimap = false;
      fresh.minimapRotate = true;
      fresh.minimapMobs = false;
      fresh.minimapSize = 2;
      fresh.minimapRadius = 384;
      fresh.mapRefresh = 9;
      fresh.bigMapZoom = 5;
      fresh.mapGrid = false;
      fresh.save();
      Options back = new Options(dir);
      back.load();
      eq(false, back.minimap, "minimap switch persisted");
      eq(true, back.minimapRotate, "rotation persisted");
      eq(false, back.minimapMobs, "mob dots persisted");
      eq(2, back.minimapSize, "size persisted");
      eq(384, back.minimapRadius, "radius persisted");
      eq(9, back.mapRefresh, "refresh interval persisted");
      eq(5, back.bigMapZoom, "big map zoom persisted");
      eq(false, back.mapGrid, "grid persisted");

      Files.write(new File(dir, "options.txt").toPath(),
         ("minimapSize=99\nminimapRadius=-5\nmapRefresh=0\nbigMapZoom=42\n").getBytes("UTF-8"));
      Options clamp = new Options(dir);
      clamp.load();
      eq(2, clamp.minimapSize, "oversized minimap is clamped");
      eq(32, clamp.minimapRadius, "negative radius is clamped");
      eq(1, clamp.mapRefresh, "zero refresh interval is clamped");
      eq(5, clamp.bigMapZoom, "big map zoom is clamped");
      Screens.resetAdvanced(clamp);
      eq(true, clamp.minimap, "reset restores the minimap");
      eq(1, clamp.minimapSize, "reset restores the minimap size");
      eq(2, clamp.bigMapZoom, "reset restores the big map zoom");
   }

   static void advancedTab() throws Exception {
      Game g = screen(343, 1800); // tall enough for all 50 rows
      g.opt.debugVideo = true;
      g.ui.optTab = 4;
      Screens.options(g);
      check(button(g, "Minimap:"), "the minimap switch is configurable in game");
      check(button(g, "Rotate Minimap"), "minimap rotation is configurable");
      check(button(g, "Minimap Size"), "minimap size is configurable");
      check(button(g, "Minimap Radius"), "minimap radius is configurable");
      check(button(g, "Map Refresh"), "map refresh budget is configurable");
      check(button(g, "Big Map Zoom"), "big map zoom is configurable");
      check(button(g, "Mob Dots"), "mob dots are configurable");
      check(button(g, "Map Grid"), "the grid is configurable");
      for (Game.Btn b : g.btns) {
         if (b.label != null && b.label.startsWith("Big Map Zoom") && b.slider != null) {
            b.slider.accept(0.0F);
            break;
         }
      }
      eq(0, g.opt.bigMapZoom, "the zoom slider changes the option");
      eq(50, Screens.ADV_ROWS, "the Advanced tab knows how many rows it has");
   }

   // ------------------------------------------------------------------ picture data
   static void colours() throws Exception {
      Game g = GameTests.newGame(1);
      World w = g.world;
      int loaded = MapView.colorAt(w, w.gen, 8, 8);
      int generated = MapView.colorAt(w, w.gen, 40000, -40000);
      check(loaded != MapView.UNKNOWN && (loaded & 0xFFFFFF) != 0, "a loaded chunk has a visible map colour");
      check(generated != MapView.UNKNOWN && (generated & 0xFFFFFF) != 0, "an unloaded column is filled from the generator");
      check(MapView.colorAt(null, null, 0, 0) == MapView.UNKNOWN, "a missing world yields the unknown colour");
      check((MapView.colorAt(w, w.gen, 8, 8) >>> 24) == 255, "map pixels are opaque");
      eq(6, MapView.BIG_STEPS.length, "six zoom levels");
      eq(3, MapView.SIZES.length, "three minimap sizes");
   }

   static void minimap() throws Exception {
      Game g = worldGame(320, 200);
      g.map.update(g);
      Set<Integer> seen = new HashSet<>();
      for (int c : g.map.mini.px) seen.add(c);
      check(seen.size() > 1, "the minimap is not one flat colour");
      check(!seen.contains(MapView.UNKNOWN), "the minimap has no unknown pixels near the player");
      GuiRaster r = (GuiRaster)g.gui;
      r.fill(0);
      g.map.drawMini(g);
      int painted = 0;
      for (int c : r.px) if (c != 0) painted++;
      check(painted > 1000, "the minimap actually paints into the HUD");
      g.opt.minimap = false;
      r.fill(0);
      int before = 0;
      g.map.drawMini(g);
      for (int c : r.px) if (c != 0) before++;
      eq(0, before, "turning the minimap off removes it");
   }

   static void bigMap() throws Exception {
      Game g = worldGame(320, 240);
      g.map.open(g);
      g.setScreen(Game.S.MAP);
      for (int i = 0; i < 70; i++) g.map.update(g);
      Set<Integer> seen = new HashSet<>();
      for (int c : g.map.big.px) seen.add(c);
      check(seen.size() > 1, "the big map has terrain detail");
      check(!seen.contains(MapView.UNKNOWN), "a bounded refresh eventually fills the whole big map");
      MapView.screen(g);
      check(button(g, "Zoom In"), "the big map has a zoom-in button");
      check(button(g, "Zoom Out"), "the big map has a zoom-out button");
      check(button(g, "Center"), "the big map can be centred on the player");
      check(button(g, "Done"), "the big map can be closed");
      double before = g.map.bigX;
      check(g.map.hit(g.map.lastX + g.map.lastSize * 0.75F, g.map.lastY + g.map.lastSize * 0.5F), "the map picture is clickable");
      g.map.click(g, g.map.lastX + g.map.lastSize * 0.75F, g.map.lastY + g.map.lastSize * 0.5F);
      check(g.map.bigX > before, "clicking to the right pans the map centre east");
      double bz = g.map.bigZ;
      g.map.pan(g, 0.0, 256.0);
      eq(bz + 256.0, g.map.bigZ, "panning moves the centre");
   }

   static void keys() throws Exception {
      Game g = worldGame(320, 240);
      g.setScreen(Game.S.PLAY);
      g.onKey(77, 1, 0);
      eq(Game.S.MAP, g.screen, "M opens the big map");
      int z = g.opt.bigMapZoom;
      g.onScroll(1.0);
      eq(z - 1, g.opt.bigMapZoom, "wheel up zooms in");
      g.onScroll(-1.0);
      eq(z, g.opt.bigMapZoom, "wheel down zooms back out");
      g.onKey(77, 1, 0);
      eq(Game.S.PLAY, g.screen, "M closes the big map again");
   }

   // ------------------------------------------------------------------ panorama
   static void panoramaReadiness() throws Exception {
      check(!Panorama.readyToFade(false, true, 99.0), "no chunks means no 3D fade-in");
      check(!Panorama.readyToFade(true, false, 1.0), "meshes can still be awaited briefly");
      check(Panorama.readyToFade(true, false, 6.0), "the timeout lets the 3D world fade in");
      check(Panorama.readyToFade(true, true, 0.0), "a fully meshed world fades in at once");

      Game g = GameTests.newGame(1);
      g.opt = new Options(new File(tmp, "pano"));
      Panorama p = new Panorama(g);
      p.world = g.world;
      p.spot = new double[]{0, 8.5, 90, 8.5, 0};
      g.rend = new Renderer(g); // meshVisibilityReady = false: only the warm ring is a candidate
      // Chunk (2,2) is outside the warm ring and still has dirty sections; the old raw test failed forever.
      check(p.meshed(2, 2, 0), "the renderer's view-aware readiness ignores deferred hidden sections");
      g.rend = null;
      check(!p.meshed(2, 2, 0), "without a renderer the raw dirty-section check is still used");

      double[] v = Panorama.emergency(Panorama.PRESETS[0]);
      eq(5, v.length, "the emergency vista has the five expected values");
      check(Double.isFinite(v[2]) && v[2] > 0.0, "the emergency vista has a usable height");
   }

   static void panoramaFallback() throws Exception {
      Game g = screen(320, 200);
      g.screen = Game.S.TITLE;
      g.splash = "Also try maps!";
      Panorama p = new Panorama(g);
      p.preset = Panorama.PRESETS[1]; // Coral Coast
      p.shown = 0.0F;
      g.pano = p;
      GuiRaster r = (GuiRaster)g.gui;
      r.fill(0);
      Screens.title(g);
      check(p.flat != null, "the 2D landscape was built");
      check((p.flat.px[0] >>> 24) > 0, "the 2D landscape is visible while the 3D world is not ready");
      Set<Integer> seen = new HashSet<>();
      for (int c : r.px) seen.add(c);
      check(seen.size() > 20, "the title screen is no longer a flat black rectangle");
      p.shown = 1.0F;
      r.fill(0);
      p.drawFlat(r);
      int painted = 0;
      for (int c : r.px) if (c != 0) painted++;
      eq(0, painted, "the fallback stops drawing once the 3D world is shown");
   }
}
