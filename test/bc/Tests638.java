package bc;

import static bc.GameTests.check;
import static bc.GameTests.eq;
import static bc.GameTests.test;

import java.io.File;
import java.nio.file.Files;
import java.util.Properties;

/** V6.38 climate and classic Screens restoration regression coverage. */
final class Tests638 {
   private Tests638() {
   }

   static void run() {
      test("6.38: seasons cycle and blend on the world clock", Tests638::seasons);
      test("6.38: local wind is seed-stable, spatial and tunable", Tests638::wind);
      test("6.38: climate settings survive world save/load", Tests638::persistence);
      test("6.38: classic menus, climate controls and release wiring", Tests638::wiring);
   }

   static void seasons() {
      eq(Seasons.SPRING, Seasons.index(0L, 8), "year begins in spring");
      eq(Seasons.SUMMER, Seasons.index(8L * Seasons.DAY, 8), "summer begins after one season");
      eq(Seasons.AUTUMN, Seasons.index(16L * Seasons.DAY, 8), "autumn follows summer");
      eq(Seasons.WINTER, Seasons.index(24L * Seasons.DAY, 8), "winter follows autumn");
      eq(Seasons.SPRING, Seasons.index(32L * Seasons.DAY, 8), "the year wraps");
      check(Seasons.growth(Seasons.SUMMER) > Seasons.growth(Seasons.WINTER), "summer crops grow faster than winter crops");
      check(Seasons.precipitation(1, Biomes.PLAINS, 64, 24L * Seasons.DAY, 8, true) == 2,
         "an ordinary biome gets snow in winter");
      check(Seasons.label(0L, 8).startsWith("Spring"), "the HUD can label the current season");
   }

   static void wind() {
      Wind a = new Wind(91234L, 1.0F);
      Wind b = new Wind(91234L, 1.0F);
      a.update(2400L);
      b.update(2400L);
      eq(a.x, b.x, "identical seeds and ticks have identical base wind");
      double[] p = a.at(0.0, 70.0, 0.0);
      double[] q = a.at(800.0, 70.0, -640.0);
      check(Math.abs(p[0] - q[0]) + Math.abs(p[1] - q[1]) > 0.01, "the local field varies by position");
      a.scale = 0.0F;
      a.update(2400L);
      eq(0.0, a.speed, "calm disables wind");
   }

   static void persistence() {
      WorldSettings source = new WorldSettings();
      source.seasons = false;
      source.seasonDays = 16;
      source.windLevel = 2;
      Properties saved = new Properties();
      source.save(saved);
      WorldSettings loaded = WorldSettings.load(saved);
      check(!loaded.seasons, "season switch is saved");
      eq(16, loaded.seasonDays, "season length is saved");
      eq(2, loaded.windLevel, "wind strength is saved");
      WorldSettings copied = source.copy();
      eq(source.windLevel, copied.windLevel, "world copies retain climate settings");
   }

   static void wiring() throws Exception {
      eq("6.38", Game.VERSION_STR, "game version");
      eq("6.38", new String(Files.readAllBytes(new File("ci/release-trigger.txt").toPath()), "UTF-8").trim(), "release trigger");
      String game = new String(Files.readAllBytes(new File("src/bc/Game.java").toPath()), "UTF-8");
      for (String name : new String[]{"title", "worlds", "multiplayer", "newWorld", "options", "pause"}) {
         check(game.contains("Screens." + name + "(this)"), "classic UI route for " + name);
      }
      String screens = new String(Files.readAllBytes(new File("src/bc/Screens.java").toPath()), "UTF-8");
      check(screens.contains("Four Seasons") && screens.contains("Climate HUD") && screens.contains("Season Length"),
         "season, length and climate HUD controls are present");
      check(screens.contains("ui.preview.update"), "live Create New World map preview remains");
   }
}
