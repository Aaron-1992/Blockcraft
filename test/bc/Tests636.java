package bc;

import static bc.GameTests.check;
import static bc.GameTests.eq;
import static bc.GameTests.test;

import java.io.File;
import java.nio.file.Files;

/**
 * V6.36: the V6.35 "Atlas" UI was rejected. Every menu is a plain centred panel
 * again, and every setting shows its real numeric value with a clear label.
 */
final class Tests636 {
   private Tests636() {
   }

   static void run() {
      test("6.36: the rejected Atlas look is gone", Tests636::plain);
      test("6.36: every setting prints its current value", Tests636::numbers);
      test("6.36: labels are plain and make sense", Tests636::labels);
      test("6.36: the world-map preview is still there", Tests636::preview);
      test("6.36: version, notes, preview and release wiring point at 6.36", Tests636::wiring);
   }

   static String src(String p) throws Exception {
      return new String(Files.readAllBytes(new File(p).toPath()), "UTF-8");
   }

   static void plain() throws Exception {
      String m = src("src/bc/Menus635.java");
      for (String bad : new String[]{"NAV_SUB", "card(Game g", "ATLAS UI", "ESC = BACK",
         "DEEP WORLD  /", "cyan", "01  /  WORLD"}) {
         check(!m.contains(bad), "no '" + bad + "' left from the rejected nav-rail/card UI");
      }
      check(m.contains("static Box shell(Game g, String title, String sub)"),
         "all screens use one plain centred panel");
      check(m.contains("ui.backdrop(0.30F)") && m.contains("gui.gradient(0.0F, 0.0F, W, 58.0F"),
         "V6.37: the Minecraft-style scrim keeps the panorama visible, with no framed panel");
      String game = src("src/bc/Game.java");
      for (String s : new String[]{"title", "worlds", "multiplayer", "newWorld", "minigames",
         "singleplayer", "mpHub", "mpCreate", "loading", "waiting", "pause", "more",
         "resourcePacks", "mods", "credits", "options"}) {
         check(game.contains("Menus635." + s + "(this)"), "the " + s + " screen is routed to the plain UI");
      }
      check(!game.contains("Screens.title(this)") && !game.contains("Screens.options(this)"),
         "no old menu screen is drawn");
   }

   static void numbers() throws Exception {
      String m = src("src/bc/Menus635.java");
      check(m.contains("static void numberRow(") && m.contains("static void sliderRow("),
         "there are dedicated numeric rows and sliders");
      check(m.contains("name + \": \" + value") || m.contains("name + \": \" + (on"),
         "stepped settings and toggles print their value in the label");
      check(m.contains("r.g.gui.text(clipped, r.x + r.w - Gui.textWidth(clipped), r.y, GOLD, true)"),
         "sliders print the number in gold on the right of the row");
      check(m.contains("r.ui.slider(Game.clipText(valueText"),
         "the slider bar itself also prints the number");
      for (String unit : new String[]{"chunks", "degrees", "fps (0 = unlimited)", "blocks", "x"}) {
         check(m.contains(unit), "the '" + unit + "' unit is spelled out");
      }
      check(m.contains("\"World Depth: \"") || m.contains("\"World Depth\""),
         "the world-depth setting shows a number");
      check(m.contains("Screens.advancedTab"),
         "Debug Advanced still exposes all 61 video / shader / performance settings");
      check(m.contains("\"Every setting shows its current value\""),
         "the settings screen states that values are visible");
      String ui = src("test/bc/TestsUi.java");
      check(ui.contains("\"plain-video\"") && ui.contains("\"plain-advanced\""),
         "the numeric setting screens are checked at all seven window sizes");
   }

   static void labels() throws Exception {
      String m = src("src/bc/Menus635.java");
      check(m.contains("MG_TABS") && m.contains("\"Bedwars\"") && m.contains("\"Murder\""),
         "mini game tabs use full names, not 'Beds' / 'Murdr'");
      check(m.contains("\"Minimap Corner\"") && m.contains("\"Top right\""),
         "the minimap corner is spelled out instead of TR/TL/BR/BL");
      check(m.contains("\"Move Minimap For Pop-ups\""), "no cryptic 'Dodge Toasts' label");
      check(m.contains("\"Reset to defaults\""), "the reset button says exactly what it does");
      check(m.contains("\"Back\""), "every screen has an obvious Back button");
      check(m.contains("\"Server address or Share Code\""), "the multiplayer field is clearly labelled");
      check(!m.contains("\"Gfx\"") && !m.contains("\"Adv\""), "no two-letter abbreviations");
   }

   static void preview() throws Exception {
      String m = src("src/bc/Menus635.java");
      check(m.contains("ui.preview.update(Screens.seedValue(g), ws)"),
         "Create New World still computes the live world-map preview");
      check(m.contains("gui.image(ui.preview.img"), "the map is actually drawn");
      check(m.contains("ui.preview.topBiomes(8)"), "the biome colour key is still there");
      check(m.contains("\"Biomes: \""), "the biome count is shown as a number");
      check(m.contains("ws.depth = g.opt.deepWorld"), "the map previews the real world depth");
      check(new File("docs/screenshots/v6.36-menus.png").isFile(), "the plain-UI preview exists");
   }

   static void wiring() throws Exception {
      eq("6.37", Game.VERSION_STR, "the game version is 6.37");
      eq("6.37", src("ci/release-trigger.txt").trim(), "the release workflow publishes 6.37");
      check(src("README.md").contains("V6.36"), "README describes V6.36");
      check(src("package/README.txt").contains("WHAT'S NEW IN V6.36"), "the packaged readme describes V6.36");
      check(new File("docs/releases/v6.36.md").isFile(), "V6.36 release notes exist");
      check(src("tools/make_release.py").contains("if version == \"6.36\""), "the GitHub release notes describe 6.36");
      check(src("docs/FILE-LAYOUT.md").contains("Menus635"), "the layout doc still lists the UI class");
   }
}
