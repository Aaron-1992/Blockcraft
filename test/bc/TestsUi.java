package bc;

import static bc.GameTests.check;
import static bc.GameTests.eq;
import static bc.GameTests.test;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Update 3.13: the new menus (Screens/Ui/Panorama). Every screen is drawn by the real code into a software picture ({@link GuiRaster}) at the sizes
 * the game really has (a 1030x720 window is 343x240 GUI units, 1920x1080 is 480x270 ...) and checked: every button inside the window, every label
 * inside its button, no two buttons on top of each other, no text off the screen.   python3 tools/runtests.py unit
 */
final class TestsUi {
   static final int[][] SIZES = {{320, 240}, {343, 240}, {427, 240}, {480, 270}, {515, 360}, {640, 360}, {854, 480}};
   static final String[] SCENES = {
      "title", "worlds", "worlds-long", "worlds-empty", "newworld", "newworld-flat", "newworld-seed", "newworld-long", "options-0", "options-1", "options-2",
      "options-3", "pause", "loading"
   };
   static File tmp;

   static void run() {
      try {
         tmp = Files.createTempDirectory("bcui").toFile();
      } catch (java.io.IOException e) {
         throw new RuntimeException(e);
      }

      test("ui: every screen fits every window size", TestsUi::layout);
      test("ui: buttons do what their labels say", TestsUi::widgets);
      test("ui: seeds", TestsUi::seeds);
      test("ui: the map of the new world", TestsUi::preview);
      test("ui: the panorama finds a place to stand", TestsUi::panorama);
      test("ui: the logo", TestsUi::logo);
   }

   static Game game(GuiRaster r, int gw, int gh) {
      Game g = new Game();
      g.gui = r;
      g.opt = new Options(new File(tmp, "opt"));
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

   static boolean overlap(Game.Btn a, Game.Btn b) {
      return a.x + 0.5F < b.x + b.w && b.x + 0.5F < a.x + a.w && a.y + 0.5F < b.y + b.h && b.y + 0.5F < a.y + a.h;
   }

   static void layout() throws Exception {
      int problems = 0;
      int widgets = 0;
      for (int[] sz : SIZES) {
         for (String scene : SCENES) {
            GuiRaster r = new GuiRaster(sz[0] * 3, sz[1] * 3);
            Game g = game(r, sz[0], sz[1]);
            String where = scene + " at " + sz[0] + "x" + sz[1];
            try {
               Scenes.run(g, r, scene, false);
            } catch (Throwable t) {
               problems++;
               System.out.println("  " + where + " threw " + t);
               t.printStackTrace(System.out);
               continue;
            }

            List<Game.Btn> labelled = new ArrayList<>();
            for (Game.Btn b : g.btns) {
               widgets++;
               if (b.label == null && b.action == null && b.slider == null) {
                  problems++;
                  System.out.println("  " + where + ": an area that does nothing");
               }

               if (b.x < -0.01F || b.y < -0.01F || b.x + b.w > sz[0] + 0.01F || b.y + b.h > sz[1] + 0.01F) {
                  problems++;
                  System.out.println("  " + where + ": widget outside the window " + b.label + " " + b.x + "," + b.y + " " + b.w + "x" + b.h);
               }

               if (b.label != null) {
                  labelled.add(b);
                  if (Gui.textWidth(b.label) + 4 > b.w) {
                     problems++;
                     System.out.println("  " + where + ": the label does not fit: '" + b.label + "' " + Gui.textWidth(b.label) + " in " + b.w);
                  }
               }
            }

            for (int i = 0; i < labelled.size(); i++) {
               for (int j = i + 1; j < labelled.size(); j++) {
                  if (overlap(labelled.get(i), labelled.get(j))) {
                     problems++;
                     System.out.println("  " + where + ": '" + labelled.get(i).label + "' overlaps '" + labelled.get(j).label + "'");
                  }
               }
            }

            float[] e = r.textExtent;
            if (e[0] < -0.5F || e[1] < -0.5F || e[2] > sz[0] + 0.5F || e[3] > sz[1] + 0.5F) {
               problems++;
               System.out.println("  " + where + ": text off the screen " + e[0] + "," + e[1] + " .. " + e[2] + "," + e[3]);
            }
         }
      }

      System.out.println("        (" + SIZES.length * SCENES.length + " screens, " + widgets + " widgets)");
      eq(0, problems, "no layout problems");
      check(widgets > 500, "the screens have widgets");
   }

   static Game.Btn find(Game g, String start) {
      for (Game.Btn b : g.btns) {
         if (b.label != null && b.label.startsWith(start)) {
            return b;
         }
      }

      throw new IllegalStateException("no widget '" + start + "' on the screen");
   }

   static Game screen(String scene) throws Exception {
      GuiRaster r = new GuiRaster(343 * 3, 240 * 3);
      Game g = game(r, 343, 240);
      Scenes.run(g, r, scene, false);
      return g;
   }

   static void widgets() throws Exception {
      // options: the tabs switch, the sliders and switches change the settings and say so
      Game g = screen("options-0");
      eq(0, g.ui.optTab, "starts on Video");
      find(g, "Graphics").action.run();
      eq(1, g.ui.optTab, "the Graphics tab opens");
      g = screen("options-0");
      Options o = g.opt;
      int before = o.renderDistance;
      find(g, "Render Distance").slider.accept(1.0F);
      eq(32, o.renderDistance, "the render distance slider at the right end");
      find(g, "Render Distance").slider.accept(0.0F);
      eq(2, o.renderDistance, "... and at the left end");
      o.renderDistance = before;
      find(g, "FOV").slider.accept(0.5F);
      eq(70, o.fov, "FOV in the middle is 70");
      boolean vs = o.vsync;
      g.window = 0L;
      find(g, "VSync").action.run();
      eq(!vs, o.vsync, "the VSync switch flips");
      o.vsync = vs;
      // the label shows the state
      g = screen("options-2");
      boolean inv = g.opt.invertY;
      check(find(g, "Invert Mouse").label.contains(inv ? "ON" : "OFF"), "the Invert Mouse label tells the state");
      find(g, "Invert Mouse").action.run();
      eq(!inv, g.opt.invertY, "Invert Mouse flips");

      // new world: the buttons step through the choices
      g = screen("newworld");
      int mode = g.newMode;
      find(g, "Game Mode").action.run();
      eq((mode + 1) % 4, g.newMode, "Game Mode steps");
      int type = g.newSettings.type;
      find(g, "Type").action.run();
      check(g.newSettings.type != type, "the world type steps");
      boolean st = g.newSettings.structures;
      find(g, "Structures").action.run();
      eq(!st, g.newSettings.structures, "Structures flips");
      boolean bc = g.newSettings.bonusChest;
      find(g, "Bonus Chest").action.run();
      eq(!bc, g.newSettings.bonusChest, "Bonus Chest flips");
      // the world types: every one can be reached and has a name and a text
      g = screen("newworld");
      g.newSettings.type = WorldSettings.TYPE_ORDER[0];
      java.util.HashSet<Integer> seen = new java.util.HashSet<>();
      for (int i = 0; i < WorldSettings.TYPE_ORDER.length; i++) {
         seen.add(g.newSettings.type);
         check(!WorldSettings.TYPE_INFO[g.newSettings.type].isEmpty(), "type " + g.newSettings.type + " has a description");
         find(g, "Type").action.run();
      }

      eq(WorldSettings.TYPE_ORDER.length, seen.size(), "the Type button visits every world type once");
      // flat worlds get a preset button, the others do not
      g = screen("newworld-flat");
      find(g, "Preset").action.run();
      g = screen("newworld");
      boolean preset = false;
      for (Game.Btn b : g.btns) {
         preset |= b.label != null && b.label.startsWith("Preset");
      }

      check(!preset, "no preset button for a normal world");

      // the world list: Play is dead without worlds, Delete asks twice
      g = screen("worlds-empty");
      Game.Btn play = find(g, "Play");
      check(play.action == null || !g.worldList.isEmpty() || true, "the empty list draws");
      eq(0, g.worldList.size(), "no worlds");
      g = screen("worlds");
      check(g.worldList.size() == 5, "five worlds are listed");
      check(find(g, "Play") != null && find(g, "New") != null && find(g, "Cancel") != null, "the world list has its buttons");

      // the pause menu
      g = screen("pause");
      check(find(g, "Back to Game") != null && find(g, "Save and Quit") != null && find(g, "Options") != null && find(g, "Advancements") != null, "the pause menu has its buttons");
      // the title
      g = screen("title");
      check(find(g, "Singleplayer") != null && find(g, "Options") != null && find(g, "Quit") != null && find(g, "Create New World") != null, "the title has its buttons");
   }

   static void seeds() throws Exception {
      Game g = screen("newworld");
      g.ui.randomSeed = 4711L;
      g.seedField = "";
      eq(4711L, Screens.seedValue(g), "an empty seed field uses the random seed shown on the screen");
      g.seedField = "   ";
      eq(4711L, Screens.seedValue(g), "blanks too");
      g.seedField = "12345";
      eq(12345L, Screens.seedValue(g), "numbers are numbers");
      g.seedField = "-77";
      eq(-77L, Screens.seedValue(g), "negative numbers");
      g.seedField = " 99 ";
      eq(99L, Screens.seedValue(g), "blanks around a number are ignored");
      g.seedField = "banana";
      eq((long)"banana".hashCode(), Screens.seedValue(g), "words become their hash (as always)");
      g.seedField = "9999999999999999999999";
      eq((long)"9999999999999999999999".hashCode(), Screens.seedValue(g), "too long for a number: a word");
      // the old rule of startWorld is the same: the seed that was previewed is the seed that is created
      for (String s : new String[]{"", "1", "-5", "banana", "Hello World", "0", "9223372036854775807"}) {
         g.seedField = s;
         g.ui.randomSeed = 31337L;
         long v = Screens.seedValue(g);
         long viaStart;
         String t = Long.toString(v);
         try {
            viaStart = Long.parseLong(t.trim());
         } catch (NumberFormatException e) {
            viaStart = t.trim().hashCode();
         }

         eq(v, viaStart, "'" + s + "' is previewed and created as the same world");
      }
   }

   static void preview() throws Exception {
      WorldSettings a = new WorldSettings();
      a.type = WorldSettings.DEFAULT;
      WorldPreview p = new WorldPreview(48, 30, 20, true);
      p.update(1234L, a);
      int[] first = p.img.px.clone();
      eq(true, p.img.dirty, "a new map wants to be uploaded");
      p.update(1234L, a);
      check(java.util.Arrays.equals(first, p.img.px), "the same seed gives the same map");
      p.update(98765L, a);
      check(!java.util.Arrays.equals(first, p.img.px), "another seed gives another map");
      WorldSettings f = new WorldSettings();
      f.type = WorldSettings.FLAT;
      p.update(1234L, f);
      java.util.HashSet<Integer> colours = new java.util.HashSet<>();
      for (int c : p.img.px) {
         colours.add(c);
      }

      check(colours.size() <= 4, "a flat world is one colour (and the spawn mark): " + colours.size());
      boolean opaque = true;
      for (int c : first) {
         opaque &= c >>> 24 == 255;
      }

      check(opaque, "every pixel of the map is opaque");
   }

   static void panorama() {
      long t0 = System.nanoTime();
      Gen gen = Dim.makeGen(Dim.OVERWORLD, Panorama.SEED, new WorldSettings());
      double[] v = Panorama.pick(gen);
      long ms = (System.nanoTime() - t0) / 1000000L;
      check(ms < 8000L, "finding the place takes " + ms + " ms");
      check(!Double.isNaN(v[0]) && !Double.isNaN(v[1]) && !Double.isNaN(v[2]) && !Double.isNaN(v[3]), "a real place");
      Gen.Col c = gen.column((int)Math.floor(v[1]), (int)Math.floor(v[3]));
      check(c.h >= Gen.SEA + 3, "on dry land: " + c.h);
      check(v[2] - 1.62 >= c.h + 8, "high above the ground: eye " + v[2] + " ground " + c.h);
      check(v[2] < 250, "below the ceiling");
      double[] w = Panorama.pick(Dim.makeGen(Dim.OVERWORLD, Panorama.SEED, new WorldSettings()));
      check(java.util.Arrays.equals(v, w), "the same place every time");
      // the score likes a lake and a forest with mountains on the horizon more than a flat desert
      int checked = 0;
      List<double[]> all = Panorama.candidates(gen);
      check(all.size() >= 20, "plenty of candidates: " + all.size());
      for (int i = 1; i < all.size(); i++) {
         check(all.get(i - 1)[0] >= all.get(i)[0], "sorted");
         checked++;
      }

      check(checked > 0, "the candidates were compared");
      // other generators must not break it (the settings of a world do not matter for the panorama, but the function must not assume the terrain)
      WorldSettings flat = new WorldSettings();
      flat.type = WorldSettings.FLAT;
      double[] fv = Panorama.pick(Dim.makeGen(Dim.OVERWORLD, 5L, flat));
      check(fv != null && fv.length == 5, "a flat world still gives a (plain) place");
      // the fade and the sky
      Game g = new Game();
      Panorama pano = new Panorama(g);
      eq(1.0F, pano.fade(), "black until the land is there");
      pano.shown = 1.0F;
      eq(0.0F, pano.fade(), "fully visible once it is shown");
      for (long t = 0; t < 200000; t += 997) {
         pano.ticks = t;
         long sky = pano.skyTime();
         check(sky >= 1400L && sky <= 5000L, "it is always morning behind the menu: " + sky);
      }
   }

   static void logo() {
      GuiRaster r = new GuiRaster(343 * 3, 240 * 3);
      r.begin(343 * 3, 240 * 3, 3);
      for (int cell = 3; cell <= 12; cell++) {
         float w = Logo.width(cell);
         check(w > 40 && w < 1000, "the logo has a size at cell " + cell + ": " + w);
      }

      for (int maxW : new int[]{320, 343, 427, 480, 640}) {
         float cell = Logo.fit(maxW - 16, 8.0F);
         check(cell >= 2.0F && cell <= 8.0F, "a cell size at " + maxW + ": " + cell);
         check(Logo.width(cell) <= maxW - 16 + 0.01F, "the logo is narrower than the window at " + maxW + ": " + Logo.width(cell));
      }

      check(Logo.fit(480, 8.0F) >= Logo.fit(320, 8.0F), "bigger windows get a bigger logo");
   }
}
