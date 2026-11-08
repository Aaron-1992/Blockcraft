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
         // ---- V6.35 Atlas UI: the screens the game actually draws now
         case "plain-video": g.screen = Game.S.OPTIONS; g.ui.optTab = 0; Menus635.options(g); return;
         case "plain-world": g.screen = Game.S.OPTIONS; g.newSettings = new WorldSettings(); g.ui.optTab = 1; Menus635.options(g); return;
         case "plain-gameplay": g.screen = Game.S.OPTIONS; g.ui.optTab = 4; Menus635.options(g); return;
         case "plain-advanced": g.screen = Game.S.OPTIONS; g.opt.debugVideo = true; g.ui.optTab = Menus635.ADV_TAB; Menus635.options(g); return;
         case "atlas-title": g.screen = Game.S.TITLE; Menus635.title(g); return;
         case "atlas-sp": g.screen = Game.S.SP_HUB; Menus635.singleplayer(g); return;
         case "atlas-mp": g.screen = Game.S.MP_HUB; g.focusField = 0; Menus635.mpHub(g); return;
         case "atlas-mpcreate": g.screen = Game.S.MP_CREATE; g.newSettings = new WorldSettings(); g.newName = ""; g.seedField = ""; g.focusField = 0; Menus635.mpCreate(g); return;
         case "atlas-newworld": g.screen = Game.S.NEWWORLD; g.newSettings = new WorldSettings(); Menus635.newWorld(g); return;
         case "atlas-worlds":
            g.screen = Game.S.WORLDS;
            g.worldList = new ArrayList<>();
            g.worldList.add(world("a", "My Survival World", 0, 12345L, 1, 3600_000L * 3));
            g.worldList.add(world("b", "Creative Castle", 1, -987654321L, 1, 3600_000L * 30));
            g.worldName = "b";
            Menus635.worlds(g); return;
         case "atlas-minigames": g.screen = Game.S.MINIGAMES; g.newSettings = new WorldSettings(); g.newSettings.type = WorldSettings.BATTLE; Menus635.minigames(g); return;
         case "atlas-options": g.screen = Game.S.OPTIONS; g.ui.optTab = 0; Menus635.options(g); return;
         case "atlas-advanced": g.screen = Game.S.OPTIONS; g.opt.debugVideo = true; g.ui.optTab = Menus635.ADV_TAB; Menus635.options(g); return;
         case "atlas-pause": g.screen = Game.S.PAUSE; g.worldDisplayName = "My Survival World"; Menus635.pause(g); return;
         case "atlas-more": g.screen = Game.S.MORE; Menus635.more(g); return;
         case "atlas-packs": g.screen = Game.S.PACKS; Menus635.resourcePacks(g); return;
         case "atlas-mods": g.screen = Game.S.MODS; Menus635.mods(g); return;
         case "atlas-credits": g.screen = Game.S.CREDITS; Menus635.credits(g); return;
         case "atlas-loading": g.screen = Game.S.LOADING; g.worldDisplayName = "My Survival World"; g.loadingMsg = "Chunks: 14/25"; g.loadFrac = 0.5F; Menus635.loading(g); return;
         case "atlas-waiting": g.screen = Game.S.WAITING; Menus635.waiting(g); return;
         case "atlas-multiplayer": g.screen = Game.S.MULTIPLAYER; g.focusField = 0; Menus635.multiplayer(g); return;
         case "title":
            g.screen = Game.S.TITLE;
            Screens.title(g);
            break;
         case "multiplayer":
            g.screen = Game.S.MULTIPLAYER;
            Screens.multiplayer(g);
            break;
         case "minigames":
            g.screen = Game.S.MINIGAMES;
            g.newSettings = new WorldSettings();
            g.newSettings.type = WorldSettings.BATTLE;
            g.newSettings.mapId = WorldSettings.MAP_COVE;
            Screens.minigames(g);
            break;
         case "sp-hub":
            g.screen = Game.S.SP_HUB;
            g.newSettings = new WorldSettings();
            Screens.singleplayer(g);
            break;
         case "mp-hub":
            g.screen = Game.S.MP_HUB;
            g.newSettings = new WorldSettings();
            g.focusField = 0;
            Screens.mpHub(g);
            break;
         case "mp-create":
            g.screen = Game.S.MP_CREATE;
            g.newSettings = new WorldSettings();
            g.newName = "";
            g.seedField = "";
            g.focusField = 0;
            Screens.mpCreate(g);
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
         case "options-4":
         case "options-5":
         case "options-6":
         case "options-7":
            g.screen = Game.S.OPTIONS;
            {
               int t = name.charAt(name.length() - 1) - '0';
               g.opt.debugVideo = t >= Screens.ADV_TAB;
               g.ui.optTab = t;
            }
            Screens.options(g);
            break;
         case "pause":
            g.screen = Game.S.PAUSE;
            g.worldDisplayName = "My Survival World";
            Screens.pause(g);
            break;
         case "packs":
            g.screen = Game.S.PACKS;
            Screens.resourcePacks(g);
            break;
         case "mods":
            g.screen = Game.S.MODS;
            Screens.mods(g);
            break;
         case "credits":
            g.screen = Game.S.CREDITS;
            Screens.credits(g);
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
