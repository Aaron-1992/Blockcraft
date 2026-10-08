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
         case "multiplayer":
            g.screen = Game.S.MULTIPLAYER;
            Screens.multiplayer(g);
            break;
         case "worlds":
         case "worlds-long":
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

            if (name.equals("worlds-long")) {
               g.worldList.add(world("a", "WWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWW", 3, Long.MIN_VALUE, 4, 86400_000L * 400));
               g.worldList.add(world("b", "A very long name that just goes on and on and on", 2, 1234567890123456789L, 3, 1000L));
               g.worldName = "a";
            }

            Screens.worlds(g);
            break;
         }
         case "newworld":
         case "newworld-flat":
         case "newworld-seed":
         case "newworld-long":
            g.screen = Game.S.NEWWORLD;
            g.newSettings = new WorldSettings();
            if (name.equals("newworld-flat")) {
               g.newSettings.type = WorldSettings.FLAT;
               g.newSettings.flat = 1;
            }

            if (name.equals("newworld-long")) {
               g.newName = "WWWWWWWWWWWWWWWWWWWWWWWW";
               g.seedField = "WWWWWWWWWWWWWWWWWWWWWWWWWWWWWW";
               g.newMode = 2;
               g.newSettings.type = WorldSettings.LARGE;
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
            g.loadFrac = 14 / 25.0F * 0.92F;
            Screens.loading(g);
            break;
         case "inv-survival":
         case "inv-table":
         case "inv-chest":
         case "inv-furnace":
         case "inv-trade":
         case "inv-creative":
         case "inv-creative-1":
         case "inv-creative-2":
         case "inv-creative-4":
         case "inv-search":
         case "inv-tip-tool":
         case "inv-tip-ore":
         case "inv-tip-food":
         case "inv-tip-tab":
         case "inv-search-none": {
            g.screen = Game.S.INV;
            g.inv[0] = new Stack(309, 1, 40);
            g.inv[1] = new Stack(1, 64);
            g.inv[2] = new Stack(270, 12);
            g.inv[3] = new Stack(4, 3);
            g.inv[9] = new Stack(260, 5);
            g.inv[10] = new Stack(16, 64);
            g.inv[20] = new Stack(34, 17);
            g.armor[1] = new Stack(325, 1, 20);
            switch (name) {
               case "inv-survival":
                  g.menu = new Menu(g, Menu.PLAYER, null);
                  break;
               case "inv-table":
                  g.menu = new Menu(g, Menu.CRAFT3, null);
                  g.menu.grid[0] = new Stack(5, 1);
                  g.menu.grid[3] = new Stack(5, 1);
                  g.menu.changed();
                  break;
               case "inv-chest": {
                  TileEntity t = new TileEntity(TileEntity.CHEST, 0, 0, 0);
                  t.slots[0] = new Stack(260, 3);
                  t.slots[4] = new Stack(258, 40);
                  g.menu = new Menu(g, Menu.CHEST, t);
                  break;
               }
               case "inv-furnace": {
                  TileEntity t = new TileEntity(TileEntity.FURNACE, 0, 0, 0);
                  t.slots[0] = new Stack(20, 8);
                  t.slots[1] = new Stack(257, 3);
                  g.menu = new Menu(g, Menu.FURNACE, t);
                  break;
               }
               case "inv-trade":
                  g.player = new Player(); // (the trades look at what the player carries)
                  g.menu = new Menu(g, Menu.TRADE, null, 0);
                  break;
               default: {
                  g.creativeTab = 0;
                  g.menu = new Menu(g, Menu.CREATIVE, null);
                  if (name.startsWith("inv-creative-")) {
                     g.menu.setTab(name.charAt(name.length() - 1) - '0');
                  } else if (name.startsWith("inv-search")) {
                     g.menu.setTab(Creative.SEARCH);
                     String q = name.endsWith("none") ? "zzz" : "pick";
                     for (char ch : q.toCharArray()) {
                        g.menu.typed(ch);
                     }
                  }
               }
            }

            if (name.startsWith("inv-tip-")) {
               if (name.endsWith("tab")) {
                  g.menu = new Menu(g, Menu.CREATIVE, null);
                  g.mouseX = (g.menuOx() + 2 * Menu.TAB_W + 10) * r.scale;
                  g.mouseY = (g.menuOy() - 10) * r.scale;
               } else {
                  g.menu = new Menu(g, Menu.PLAYER, null);
                  int idx = name.endsWith("tool") ? 0 : (name.endsWith("ore") ? 1 : 2);
                  if (idx == 1) {
                     g.inv[1] = new Stack(20, 8);
                  }

                  for (Menu.Slot s : g.menu.slots) {
                     if (s.arr == g.inv && s.idx == idx) {
                        g.mouseX = (g.menuOx() + s.x + 9) * r.scale;
                        g.mouseY = (g.menuOy() + s.y + 9) * r.scale;
                     }
                  }
               }
            }

            g.drawInventory();
            break;
         }
         default:
            throw new IllegalArgumentException("unknown scene " + name);
      }
   }
}
