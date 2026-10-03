package bc;

import java.util.ArrayList;

/** The screens that {@link UiShots} can draw (each sets up the game state a screen needs and calls its drawing code). */
final class Scenes {
   private Scenes() {
   }

   static Game.WInfo world(String dir, String name, int mode, long seed, int type, long agoMs) {
      Game.WInfo w = new Game.WInfo();
      w.dir = dir;
      w.name = name;
      w.mode = mode;
      w.seed = seed;
      w.mod = System.currentTimeMillis() - agoMs;
      w.settings = new WorldSettings();
      w.settings.type = type;
      return w;
   }

   static void run(Game g, GuiRaster r, String name, boolean hasBg) throws Exception {
      if (hasBg) {
         // stand in for the turning world: the picture is already there, the menu only dims it
         g.world = new World(1, null);
         g.pano = new Panorama(g);
         g.pano.shown = 1.0F;
      }

      g.splash = "Also try voxels!";
      g.ui.randomSeed = 7311L;
      switch (name) {
         case "title":
            g.screen = Game.S.TITLE;
            Screens.title(g);
            break;
         case "worlds":
         case "worlds-empty": {
            g.screen = Game.S.WORLDS;
            g.worldList = new ArrayList<>();
            if (name.equals("worlds")) {
               g.worldList.add(world("a", "My Survival World", 0, 12345L, 1, 3600_000L * 3));
               g.worldList.add(world("b", "Creative Castle", 1, -987654321L, 1, 3600_000L * 30));
               g.worldList.add(world("c", "Old World from Update 3.5", 0, 424242L, 0, 86400_000L * 12));
               g.worldList.add(world("d", "Flat Test", 1, 7L, 2, 86400_000L * 40));
               g.worldList.add(world("e", "Amplified Mountains", 0, 99L, 4, 86400_000L * 90));
               g.worldName = "b";
            }

            Screens.worlds(g);
            break;
         }
         case "newworld":
         case "newworld-flat":
         case "newworld-seed":
            g.screen = Game.S.NEWWORLD;
            g.newSettings = new WorldSettings();
            if (name.equals("newworld-flat")) {
               g.newSettings.type = WorldSettings.FLAT;
               g.newSettings.flat = 1;
            }

            if (name.equals("newworld-seed")) {
               g.newName = "Mountain Home";
               g.seedField = "banana";
               g.newMode = 0;
               g.focusField = 1;
            }

            Screens.newWorld(g);
            break;
         case "options-0":
         case "options-1":
         case "options-2":
         case "options-3":
            g.screen = Game.S.OPTIONS;
            g.ui.optTab = name.charAt(name.length() - 1) - '0';
            Screens.options(g);
            break;
         case "pause":
            g.screen = Game.S.PAUSE;
            g.worldDisplayName = "My Survival World";
            Screens.pause(g);
            break;
         case "loading":
            g.screen = Game.S.LOADING;
            g.worldDisplayName = "My Survival World";
            g.worldName = "My Survival World";
            g.loadingMsg = "Chunks: 14/25";
            Screens.loading(g);
            break;
         default:
            throw new IllegalArgumentException("unknown scene " + name);
      }
   }
}
