package bc;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import org.lwjgl.glfw.GLFW;

/**
 * Update 3.13: the menu screens - title, world list, Create New World, options, game menu, loading. They are drawn through {@link Ui} and {@link Gui} only
 * (no OpenGL calls of their own), so test/bc/UiShots can draw them into a picture.
 */
final class Screens {
   private Screens() {
   }

   static final String[] MODE_NAME = {"Survival", "Creative", "Adventure", "Spectator", "Hardcore"};
   static final String[] MODE_INFO = {
      "Gather resources, craft, eat and fight the mobs of the night.",
      "Unlimited blocks, no damage, and flying (tap Space twice; F3+N spectator).",
      "RPG mode: break blocks only with their proper tool, trade, quest and explore!",
      "Fly through walls (scroll to change speed, 1-6 teleport bar, click mob POV).",
      "Hardcore Survival: one life only! Hard mob damage and no respawning."
   };
   static final String[] TIPS = {
      "Double-tap Space to fly in Creative mode.", "Press T to chat and / for commands, e.g. /time set day.", "Press L to see your advancements.",
      "Hold C to zoom in. Scroll while zooming to change it.", "F5 swaps between first and third person.", "A bed lets you skip the night and sets your spawn point.",
      "Sneak (Shift) to stop yourself from falling off edges.", "Water and lava meet to make stone. Be careful with lava!",
      "Build an obsidian frame and light it to open a Nether portal.", "Villagers trade: right-click one with emeralds in your hand.",
      "A stack of 64 is a full slot. Shift-click moves stacks quickly.", "F2 takes a screenshot, F1 hides the interface.",
      "Shaders (Options > Graphics) add sun shadows and glittering water.", "Hold Ctrl or double-tap W to sprint.",
      "Your world is saved when you choose Save and Quit to Title."
   };

   // =================================================================== title
   static void title(Game g) {
      Ui ui = g.ui;
      Gui gui = g.gui;
      ui.begin();
      ui.backdrop(0.0F);
      gui.gradient(0, 0, gui.w, gui.h * 0.5F, 0x80000000, 0x00000000);
      gui.gradient(0, gui.h * 0.6F, gui.w, gui.h * 0.4F, 0x00000000, 0xB0000000);
      float cx = gui.w / 2.0F;
      float cell = Logo.fit(gui.w - 36.0F, gui.h >= 300 ? 8.0F : 6.0F);
      float top = Math.max(10.0F, gui.h * 0.08F);
      double time = ui.time();
      float bottom = Logo.draw(gui, cx, top, cell, time);
      boolean infdev = ModLoader.isInfdevActive();
      gui.centered(
         infdev ? "MINECRAFT INFDEV (JUNE 2010)" : ("JAVA EDITION  -  V" + Game.VERSION_STR),
         cx,
         bottom + 10.0F,
         infdev ? 0xFF63D43E : 0xFFD6D6E0,
         true,
         1.0F
      );
      String splashText = infdev ? "Infdev 2010!" : g.splash;
      if (splashText != null) {
         float sw = Gui.textWidth(splashText);
         float sx = Math.min(cx + Logo.width(cell) / 2.0F - 22.0F, gui.w - sw / 2.0F - 12.0F);
         gui.push();
         gui.translate(sx, bottom - 6.0F);
         gui.rotate(-18.0F);
         float pulse = 1.0F + 0.06F * (float)Math.sin(time * 6.0);
         gui.scaleBy(pulse);
         gui.centered(splashText, 0.0F, 0.0F, 0xFFFFFF00, true, 1.0F);
         gui.pop();
      }

      float bw = Math.min(200.0F, gui.w - 40.0F);
      float half = (bw - 4.0F) / 2.0F;
      float third = (bw - 8.0F) / 3.0F;
      float row = 24.0F;
      float by = Math.max(bottom + 26.0F, gui.h * 0.45F);
      by = Math.min(by, gui.h - 16.0F - row * 3.0F - 20.0F);
      float bx = cx - bw / 2.0F;
      ui.button("Singleplayer", bx, by, bw, 20.0F, true, null, () -> openWorlds(g));
      ui.button("Multiplayer", bx, by + row, half, 20.0F, true, null, () -> {
         g.mpReturn = Game.S.TITLE;
         openMultiplayer(g);
      });
      ui.button("Mods...", bx + half + 4.0F, by + row, half, 20.0F, true, null, () -> {
         g.modsReturn = Game.S.TITLE;
         g.setScreen(Game.S.MODS);
      });
      float wSmall = 58.0F;
      float wOpt = bw - 2.0F * (wSmall + 4.0F);
      ui.button("Skins", bx, by + row * 2, wSmall, 20.0F, true, null, () -> {
         g.skinReturn = Game.S.TITLE;
         g.setScreen(Game.S.SKINS);
      });
      ui.button("Packs", bx + wSmall + 4.0F, by + row * 2, wSmall, 20.0F, true, null, () -> {
         g.packsReturn = Game.S.TITLE;
         g.setScreen(Game.S.PACKS);
      });
      ui.button("Options...", bx + 2.0F * (wSmall + 4.0F), by + row * 2, wOpt, 20.0F, true, null, () -> {
         g.optionsReturn = Game.S.TITLE;
         g.setScreen(Game.S.OPTIONS);
      });
      ui.button("Quit Game", bx, by + row * 3, bw, 20.0F, true, null, () -> GLFW.glfwSetWindowShouldClose(g.window, true));
      gui.text("Blockcraft V" + Game.VERSION_STR, 4.0F, gui.h - 11.0F, 0xFFC4C4CE, true);
      String r = "Not affiliated with Mojang";
      gui.text(r, gui.w - Gui.textWidth(r) - 4.0F, gui.h - 11.0F, 0xFFC4C4CE, true);
   }

   static void openMultiplayer(Game g) {
      g.focusField = 0;
      g.setScreen(Game.S.MULTIPLAYER);
   }

   // =================================================================== V4.0: Multiplayer Screen
   static void multiplayer(Game g) {
      Ui ui = g.ui;
      Gui gui = g.gui;
      ui.begin();
      ui.backdrop(0.5F);
      float cx = gui.w / 2.0F;
      ui.title("Multiplayer", 7.0F);
      float pw = Math.min(300.0F, gui.w - 16.0F);
      float px = cx - pw / 2.0F;
      float py = 21.0F;
      float footY = gui.h - 24.0F;
      float ph = footY - py - 4.0F;
      ui.panel(px, py, pw, ph);

      float ix = px + 6.0F;
      float iw = pw - 12.0F;
      float colW = (iw - 6.0F) / 2.0F;
      float x1 = ix + colW + 6.0F;
      float iy = py + 5.0F;
      ui.label("Server Address / Share Code", ix, iy, Ui.DIM);
      ui.field(ix, iy + 9.0F, iw, 16.0F, Net319.serverAddr, "127.0.0.1:25565", g.focusField == 0, () -> g.focusField = 0);

      String stateLine = Net319.isHosting()
         ? "Hosting " + WorldSettings.TYPE_NAME[Net319.mpWorldType] + " (" + Net319.peerCount() + " online) | Code: " + Net319.shareCode
         : (Net319.isConnected() ? "Connected (" + Net319.peerCount() + " online)" : Net319.statusText);
      gui.text(Game.clipText(stateLine, iw), ix, iy + 28.0F, Net319.isActive() ? 0xFF8FE060 : Ui.FAINT, true);

      float by0 = iy + 39.0F;
      ui.button("Type: " + WorldSettings.TYPE_NAME[Net319.mpWorldType], ix, by0, colW, 20.0F, true,
         WorldSettings.TYPE_INFO[Net319.mpWorldType],
         Net319::cycleMpWorldType);
      ui.button("Mode: " + Net319.mpModeName(), x1, by0, colW, 20.0F, true,
         "Select game mode for hosted worlds and Server.bat (Survival, Creative, Adventure, Spectator).",
         Net319::cycleMpGameMode);

      float by1 = by0 + 22.0F;
      ui.button(Net319.mpWorldwide ? "Access: Worldwide" : "Access: LAN Only", ix, by1, colW, 20.0F, true,
         "Worldwide opens UPnP + Public TCP Tunnel automatically so friends anywhere on Earth can join without port forwarding.",
         Net319::toggleMpWorldwide);
      ui.button("Copy Share Code", x1, by1, colW, 20.0F, true,
         "Generate and copy a BC-... Share Code that friends anywhere on Earth can paste into Server Address to join.",
         () -> Net319.copyShareCode(g));

      float by2 = by1 + 22.0F;
      ui.button("Join Server", ix, by2, colW, 20.0F, true,
         "Connect to the IP:Port, Public Tunnel URL, or BC-... Share Code entered above.",
         () -> Net319.joinFromMenu(g, Net319.serverAddr));
      ui.button("Join LAN", x1, by2, colW, 20.0F, true,
         "Auto-discover a Blockcraft LAN or local host on your network and join.",
         () -> Net319.joinFromMenu(g, "lan"));

      float by3 = by2 + 22.0F;
      ui.button("Start Server.bat", ix, by3, colW, 20.0F, true,
         "Launches Blockcraft-server.bat with the selected World Type, Game Mode & Worldwide Tunnel and joins it.",
         () -> Net319.launchServerAndJoin(g));
      ui.button("Host World", x1, by3, colW, 20.0F, true,
         "Host a world with the selected World Type & Mode directly and generate a Worldwide Share Code.",
         () -> Net319.hostFromMenu(g));

      float by4 = by3 + 22.0F;
      if (by4 + 20.0F <= py + ph - 2.0F) {
         ui.button("Disconnect", ix, by4, colW, 20.0F, Net319.isActive(),
            "Disconnect from the active server or stop hosting.",
            () -> {
               Net319.disconnect(g);
               Net319.statusText = "Disconnected.";
            });
         ui.button("Reset Port", x1, by4, colW, 20.0F, true,
            "Reset server address to default localhost (127.0.0.1:25565).",
            () -> Net319.serverAddr = "127.0.0.1:25565");
      }

      float bw = Math.min(200.0F, pw);
      ui.button("Back", cx - bw / 2.0F, footY, bw, 20.0F, true, null, () -> g.setScreen(g.mpReturn != null ? g.mpReturn : Game.S.TITLE));
      ui.drawToast();
   }

   static void openWorlds(Game g) {
      g.scanWorlds();
      g.ui.worldScroll[0] = 0.0F;
      g.confirmDelete = false;
      if (g.worldList.isEmpty()) {
         openNewWorld(g);
      } else {
         g.setScreen(Game.S.WORLDS);
      }
   }

   static void openNewWorld(Game g) {
      g.seedField = "";
      g.newName = "";
      g.focusField = 0;
      g.ui.randomSeed = new Random().nextLong();
      g.setScreen(Game.S.NEWWORLD);
   }

   // =================================================================== singleplayer: the list of worlds
   static void worlds(Game g) {
      Ui ui = g.ui;
      Gui gui = g.gui;
      ui.begin();
      ui.backdrop(0.5F);
      float cx = gui.w / 2.0F;
      ui.title("Singleplayer", 10.0F);
      float pw = Math.min(330.0F, gui.w - 24.0F);
      float px = cx - pw / 2.0F;
      float py = 26.0F;
      float footer = 2 * 20.0F + 4.0F + 12.0F;
      float ph = gui.h - py - footer;
      ui.panel(px, py, pw, ph);
      int n = g.worldList.size();
      float rowH = 38.0F;
      if (n == 0) {
         gui.centered("You have no worlds yet.", cx, py + ph / 2.0F - 12.0F, Ui.TEXT, true, 1.0F);
         gui.centered("Create one and start digging!", cx, py + ph / 2.0F, Ui.DIM, true, 1.0F);
      } else {
         float content = n * rowH + 4.0F;
         boolean bar = content > ph;
         ui.beginScroll(px, py, pw, ph, ui.worldScroll, content, rowH);
         int thumbs = 0;
         for (int i = 0; i < n; i++) {
            float ry = py + 3.0F + i * rowH - ui.worldScroll[0];
            if (ry < py - 0.5F || ry + rowH - 2.0F > py + ph + 0.5F) {
               continue;
            }

            Game.WInfo wi = g.worldList.get(i);
            if (wi.thumb == null && thumbs < 3) {
               thumbs++;
               wi.thumb = new WorldPreview(32, 32, 16, false);
               wi.thumb.update(wi.seed, wi.settings);
            }

            worldRow(g, wi, px + 3.0F, ry, pw - 6.0F, rowH - 2.0F);
         }

         ui.endScroll(px, py, pw, ph, content);
         if (bar) {
            gui.rect(px + pw - 1, py, 1, ph, 0);
         }
      }

      float by = gui.h - footer + 6.0F;
      float third = (pw - 8.0F) / 3.0F;
      boolean any = n > 0;
      ui.button("Play Selected World", px, by, pw, 20.0F, any, "Opens the world you picked (double-click a world to open it faster).", () -> g.startWorld(false, null, 1));
      ui.button("New World", px, by + 24.0F, third, 20.0F, true, "Make a new world: choose its seed, type and game mode.", () -> openNewWorld(g));
      ui.button(g.confirmDelete ? "\u00a7cReally delete?" : "Delete", px + third + 4.0F, by + 24.0F, third, 20.0F, any,
         "Deletes the picked world from your disk. Click twice to confirm. This cannot be undone.", () -> {
            if (!g.confirmDelete) {
               g.confirmDelete = true;
            } else {
               Game.deleteDir(new File(g.root, "saves/" + g.worldName));
               g.confirmDelete = false;
               g.scanWorlds();
               if (g.worldList.isEmpty()) {
                  g.setScreen(Game.S.TITLE);
               }
            }
         });
      ui.button("Cancel", px + 2 * (third + 4.0F), by + 24.0F, third, 20.0F, true, null, () -> g.setScreen(Game.S.TITLE));
   }

   static void worldRow(Game g, Game.WInfo wi, float x, float y, float w, float h) {
      Ui ui = g.ui;
      Gui gui = g.gui;
      boolean sel = wi.dir.equals(g.worldName);
      boolean hov = ui.over(x, y, w, h);
      gui.rect(x, y, w, h, sel ? 0xFFFFFFFF : (hov ? 0xFF8A8A98 : 0xFF3A3C48));
      gui.rect(x + 1, y + 1, w - 2, h - 2, sel ? 0xFF2C3044 : (hov ? 0xFF222532 : 0xFF161820));
      float ts = h - 6.0F;
      gui.rect(x + 2, y + 2, ts + 2, ts + 2, 0xFF000000);
      if (wi.thumb != null) {
         gui.image(wi.thumb.img, x + 3, y + 3, ts, ts);
      }

      float tx = x + ts + 9.0F;
      float tw = w - (tx - x) - 4.0F;
      String when = ago(wi.mod);
      float ww = Gui.textWidth(when);
      gui.text(when, x + w - ww - 4.0F, y + 4.0F, Ui.FAINT, true);
      gui.text(Game.clipText(wi.name, tw - ww - 8.0F), tx, y + 4.0F, 0xFFFFFFFF, true);
      gui.text(Game.clipText(MODE_NAME[Math.max(0, Math.min(MODE_NAME.length - 1, wi.mode))] + "  -  " + wi.settings.typeName(), tw), tx, y + 15.0F, Ui.DIM, true);
      gui.text(Game.clipText("Seed " + wi.seed, tw), tx, y + 25.0F, Ui.FAINT, true);
      final String dir = wi.dir;
      g.hitbox(x, y, w, h, () -> {
         long now = System.currentTimeMillis();
         if (dir.equals(g.worldName) && now - g.lastWorldClick < 400L) {
            g.startWorld(false, null, 1);
         }

         g.worldName = dir;
         g.confirmDelete = false;
         g.lastWorldClick = now;
      });
   }

   static String ago(long then) {
      long d = Math.max(0L, System.currentTimeMillis() - then) / 60000L;
      if (d < 2L) {
         return "just now";
      }

      if (d < 60L) {
         return d + " minutes ago";
      }

      if (d < 24L * 60L) {
         return d / 60L + (d / 60L == 1L ? " hour ago" : " hours ago");
      }

      if (d < 48L * 60L) {
         return "yesterday";
      }

      if (d < 60L * 24L * 60L) {
         return d / (24L * 60L) + " days ago";
      }

      return new SimpleDateFormat("yyyy-MM-dd").format(new Date(then));
   }

   // =================================================================== Create New World
   /** The number the world will be made from: what is typed in the seed field, else the random one that the preview shows. */
   static long seedValue(Game g) {
      String s = g.seedField.trim();
      if (s.isEmpty()) {
         return g.ui.randomSeed;
      }

      try {
         return Long.parseLong(s);
      } catch (NumberFormatException e) {
         return s.hashCode();
      }
   }

   static void createWorld(Game g) {
      g.startWorld(true, Long.toString(seedValue(g)), g.newMode);
   }

   static void newWorld(Game g) {
      Ui ui = g.ui;
      Gui gui = g.gui;
      ui.begin();
      ui.backdrop(0.5F);
      WorldSettings ws = g.newSettings;
      float cx = gui.w / 2.0F;
      float colW = Math.min(152.0F, (gui.w - 28.0F) / 2.0F);
      float gap = 8.0F;
      float x0 = cx - colW - gap / 2.0F;
      float x1 = cx + gap / 2.0F;
      float extra = Math.max(0.0F, (gui.h - 240.0F) * 0.12F);
      float y = 24.0F + extra;
      ui.title("Create New World", 8.0F + extra * 0.5F);

      // ---- left column: name, seed, game mode, structures, bonus chest
      ui.label("World Name", x0, y, Ui.DIM);
      ui.field(x0, y + 10.0F, colW, 18.0F, g.newName, "New World", g.focusField == 0, () -> g.focusField = 0);
      ui.label("Seed", x0, y + 34.0F, Ui.DIM);
      ui.field(x0, y + 44.0F, colW, 18.0F, g.seedField, "Random", g.focusField == 1, () -> g.focusField = 1);
      if (ui.over(x0, y + 44.0F, colW, 18.0F)) {
         ui.hint = "The seed makes the world. The same seed (and world type) always gives the same world. Leave it empty for a random one. Words work too.";
      }

      float shift = Math.max(0.0F, Math.min(32.0F, (gui.h - 240.0F) * 0.3F)); // taller windows: a bigger map, the buttons move down with it
      float ry = y + 68.0F + shift; // the first row of buttons (both columns line up)
      ui.button("Game Mode: " + MODE_NAME[g.newMode], x0, ry, colW, 20.0F, true, null, () -> g.newMode = (g.newMode + 1) % MODE_NAME.length);
      ui.toggle("Structures", ws.structures, x0, ry + 24.0F, colW, 20.0F,
         "Villages, strongholds, ruins, temples and the other buildings that generate on their own. The Nether and the End keep theirs.", () -> ws.structures = !ws.structures);
      ui.toggle("Bonus Chest", ws.bonusChest, x0, ry + 48.0F, colW, 20.0F, "A chest with a few starting supplies next to where you appear.", () -> ws.bonusChest = !ws.bonusChest);
      float leftEnd = ry + 70.0F;

      // ---- right column: the preview, world type, preset
      float pv = 64.0F + shift;
      if (ui.preview == null) {
         ui.preview = new WorldPreview(96, 60, 20, true);
      }

      ui.preview.update(seedValue(g), ws);
      gui.rect(x1 - 1, y - 1, colW + 2, pv + 2, 0xFF000000);
      gui.image(ui.preview.img, x1, y, colW, pv);
      if (ui.over(x1, y, colW, pv)) {
         ui.hint = g.seedField.trim().isEmpty() ? "A map of the world you would get. Click it to roll another random world. The red dot is where you start." : "A map of the world this seed makes. The red dot is where you start.";
      }

      if (g.seedField.trim().isEmpty()) {
         g.hitbox(x1, y, colW, pv, () -> ui.randomSeed = new Random().nextLong());
      }

      float ty = ry;
      ui.button("Type: " + ws.typeName(), x1, ty, colW, 20.0F, true, WorldSettings.TYPE_INFO[ws.type], () -> {
         ws.type = WorldSettings.TYPE_ORDER[(ws.typeIndex() + 1) % WorldSettings.TYPE_ORDER.length];
         if (ws.isVoidMode()) {
            g.newMode = 0; // Skyblock, Skywars, and Bedwars default to Survival
         }
      });
      float iy = ty + 24.0F;
      if (ws.type == WorldSettings.FLAT) {
         ui.button("Preset: " + WorldSettings.FLAT_NAME[ws.flat], x1, iy, colW, 20.0F, true, "What the flat world is made of: grass, desert sand, snow or sandstone.", () -> {
            ws.flat = (ws.flat + 1) % WorldSettings.FLAT_PRESETS;
         });
         iy += 24.0F;
      }

      float rightEnd = ui.paragraph(WorldSettings.TYPE_INFO[ws.type], x1, iy, colW, Ui.DIM, 3);
      float seedInfoY = Math.max(leftEnd, rightEnd) + 4.0F;
      String sd = g.seedField.trim().isEmpty() ? "Random seed: " + ui.randomSeed : "Seed: " + seedValue(g);
      if (seedInfoY + 10.0F < gui.h - 56.0F) {
         gui.text(Game.clipText(sd, colW * 2 + gap), x0, seedInfoY, Ui.FAINT, true);
      }

      // ---- the bottom
      float by = gui.h - 26.0F;
      float hintH = 24.0F;
      ui.drawHint(x0, by - hintH - 6.0F, colW * 2 + gap, hintH, MODE_INFO[g.newMode]);
      ui.button("Create New World", x0, by, colW, 20.0F, true, null, () -> createWorld(g));
      ui.button("Cancel", x1, by, colW, 20.0F, true, null, () -> {
         g.scanWorlds();
         g.setScreen(g.worldList.isEmpty() ? Game.S.TITLE : Game.S.WORLDS);
      });
   }

   // =================================================================== options
   static final String[] TABS = {"Video", "Graphics", "Gameplay", "Controls"};
   static final int[] LOD_STEPS = {0, 32, 64, 128, 256, 384, 512};
   static final String[][] KEYS = {
      {"W A S D", "Walk"}, {"Space", "Jump / swim"}, {"Space x2", "Fly (Creative)"}, {"Shift", "Sneak"}, {"Ctrl", "Sprint"}, {"Mouse L", "Break / attack"},
      {"Mouse R", "Place / use"}, {"1 - 9", "Choose item"}, {"E", "Inventory"}, {"Q", "Drop item"}, {"T", "Chat"}, {"/", "Command"}, {"L", "Advancements"},
      {"C", "Zoom"}, {"F5", "3rd person"}, {"F1", "Hide the HUD"}, {"F2", "Screenshot"}, {"F3", "Debug info"}, {"F11", "Fullscreen"}, {"Esc", "Game menu"}
   };

   static void options(Game g) {
      Ui ui = g.ui;
      Gui gui = g.gui;
      Options o = g.opt;
      ui.begin();
      ui.backdrop(0.5F);
      float cx = gui.w / 2.0F;
      float pw = Math.min(330.0F, gui.w - 16.0F);
      float colW = (pw - 6.0F) / 2.0F;
      float x0 = cx - pw / 2.0F;
      float x1 = x0 + colW + 6.0F;
      ui.title("Options", 7.0F);
      ui.tabs(TABS, ui.optTab, x0, 20.0F, pw, 16.0F, i -> {
         ui.optTab = i;
         ui.optScroll[0] = 0.0F;
      });
      float row = gui.h >= 300 ? 24.0F : 22.0F;
      float top = 42.0F;
      float hintH = 26.0F;
      float doneY = gui.h - 24.0F;
      float hintY = doneY - hintH - 5.0F;
      float viewH = hintY - 4.0F - top;
      int rows;
      switch (ui.optTab) {
         case 0:
            rows = 5;
            break;
         case 1:
            rows = 4;
            break;
         case 2:
            rows = 3;
            break;
         default:
            rows = (KEYS.length + 1) / 2;
      }

      float content = ui.optTab == 3 ? rows * 11.0F + 4.0F : rows * row;
      ui.beginScroll(x0, top, pw, viewH, ui.optScroll, content, row);
      float oy = top - ui.optScroll[0];
      switch (ui.optTab) {
         case 0:
            videoTab(g, o, x0, x1, colW, oy, row, top, viewH);
            break;
         case 1:
            graphicsTab(g, o, x0, x1, colW, oy, row, top, viewH);
            break;
         case 2:
            gameTab(g, o, x0, x1, colW, oy, row, top, viewH);
            break;
         default:
            controlsTab(g, x0, x1, oy);
      }

      ui.endScroll(x0, top, pw, viewH, content);
      String[] about = {
         "Resolution, distance and the window.", "Looks: shadows, water, clouds, colours, glow.", "How the game plays and how the mouse feels.",
         "The keys of the game. They cannot be changed yet."
      };
      ui.drawHint(x0, hintY, pw, hintH, about[ui.optTab] + (ui.optTab == 3 ? "" : "  Point at a setting to see what it does."));
      ui.button("Done", cx - 100.0F, doneY, 200.0F, 20.0F, true, null, () -> {
         g.opt.save();
         g.setScreen(g.optionsReturn);
      });
   }

   /** Is the row at oy inside the viewport? (rows that are cut off are neither drawn nor clickable) */
   static boolean shows(float oy, float row, float top, float viewH) {
      return oy >= top - 0.5F && oy + row - 2.0F <= top + viewH + 0.5F;
   }

   static void videoTab(Game g, Options o, float x0, float x1, float colW, float oy, float row, float top, float viewH) {
      Ui ui = g.ui;
      if (shows(oy, row, top, viewH)) {
         ui.slider("Render Distance: " + o.renderDistance, x0, oy, colW, 20.0F, (o.renderDistance - 2) / 30.0F,
            "How many chunks (16x16 blocks) around you are drawn in full detail. Lower it if the game stutters.", v -> o.renderDistance = 2 + Math.round(v * 30.0F));
         ui.slider("FOV: " + o.fov, x1, oy, colW, 20.0F, (o.fov - 30) / 80.0F, "Field of view in degrees. 70 is normal; higher shows more at the sides.", v -> o.fov = 30 + Math.round(v * 80.0F));
      }

      oy += row;
      if (shows(oy, row, top, viewH)) {
         int lod = 0;
         for (int i = 0; i < LOD_STEPS.length; i++) {
            if (LOD_STEPS[i] == o.lodDistance) {
               lod = i;
            }
         }

         ui.slider("LOD Distance: " + (o.lodDistance == 0 ? "OFF" : o.lodDistance), x0, oy, colW, 20.0F, lod / (float)(LOD_STEPS.length - 1),
            "Far land beyond the render distance, drawn as a cheap simplified landscape. OFF shows nothing out there.", v -> o.lodDistance = LOD_STEPS[Math.round(v * (LOD_STEPS.length - 1))]);
         ui.slider("Brightness: " + (o.gamma <= 0.0F ? "Moody" : (o.gamma >= 1.0F ? "Bright" : (int)(o.gamma * 100.0F) + "%")), x1, oy, colW, 20.0F, o.gamma,
            "Lifts the dark: caves and the night get brighter.", v -> o.gamma = v);
      }

      oy += row;
      if (shows(oy, row, top, viewH)) {
         ui.button("GUI Scale: " + (o.guiScale == 0 ? "Auto" : Math.min(o.guiScale, g.guiScaleMax()) + ""), x0, oy, colW, 20.0F, true,
            "The size of the menus and the hotbar. Auto picks the biggest that fits your window.", () -> o.guiScale = (Math.min(o.guiScale, g.guiScaleMax()) + 1) % (g.guiScaleMax() + 1));
         ui.toggle("VSync", o.vsync, x1, oy, colW, 20.0F, "Waits for your screen's refresh: smoother and cooler, but can feel a little laggy.", () -> {
            o.vsync = !o.vsync;
            if (g.window != 0L) {
               GLFW.glfwSwapInterval(o.vsync ? 1 : 0);
            }
         });
      }

      oy += row;
      if (shows(oy, row, top, viewH)) {
         ui.toggle("Fullscreen", o.fullscreen, x0, oy, colW, 20.0F, "Uses your whole screen. F11 does the same at any time.", () -> g.setFullscreen(!o.fullscreen));
         ui.toggle("Fast GPU", o.gpuHigh, x1, oy, colW, 20.0F,
            "A laptop often has two graphics chips: a weak one inside the processor and a real graphics card. ON asks Windows to draw the game with the real one (takes effect at the next start). Drawing with now: "
               + GpuPref.shortName(g.gpuName),
            () -> {
               o.gpuHigh = !o.gpuHigh;
               GpuPref.apply(o);
            });
      }

      oy += row;
      if (shows(oy, row, top, viewH)) {
         ui.slider("Render Scale: " + o.renderScale + "%", x0, oy, colW, 20.0F, (o.renderScale - 50) / 50.0F,
            "Draws the 3D world smaller than the window and stretches it: the fastest way to get more frames out of a weak graphics chip, at the price of a softer picture. The menus and the text stay sharp. Needs Shaders ON.",
            v -> o.renderScale = 50 + Math.round(v * 10.0F) * 5);
         ui.button("Max FPS: " + (o.maxFps == 0 ? "Unlimited" : o.maxFps + ""), x1, oy, colW, 20.0F, true,
            "Stops the game from drawing more pictures per second than this (VSync already does that at your screen's rate). A fast graphics card then rests instead of running flat out. The menus use 60 at most.",
            () -> {
               int at = 0;
               for (int i = 0; i < FPS_STEPS.length; i++) {
                  if (FPS_STEPS[i] == o.maxFps) {
                     at = i;
                  }
               }

               o.maxFps = FPS_STEPS[(at + 1) % FPS_STEPS.length];
            });
      }
   }

   static final int[] FPS_STEPS = {0, 30, 60, 90, 120, 144, 165, 240};

   static void graphicsTab(Game g, Options o, float x0, float x1, float colW, float oy, float row, float top, float viewH) {
      Ui ui = g.ui;
      if (shows(oy, row, top, viewH)) {
         String problem = g.shaderProblem();
         ui.button("Shaders: " + (problem != null ? "\u00a7cUNAVAILABLE" : (!o.shaders ? "\u00a7cOFF" : (o.waterQ >= 2 ? "\u00a7bFANCY" : "\u00a7aON"))), x0, oy, colW, 20.0F, true,
            problem != null
               ? "This graphics chip's driver could not start the shader pack, so the game draws the plain picture. Reason: " + problem + " (more in latest.log). F3 shows the same."
               : "ON: sun shadows, glowing light and water with depth. FANCY: the water also mirrors the land. OFF is the fastest.", () -> {
               if (!o.shaders) {
                  o.shaders = true;
                  o.waterQ = 1;
               } else if (o.waterQ < 2) {
                  o.waterQ = 2;
               } else {
                  o.shaders = false;
               }
            });
         ui.toggle("Smooth Lighting", o.smooth, x1, oy, colW, 20.0F, "Soft shading in the corners of blocks instead of flat squares of light.", () -> {
            o.smooth = !o.smooth;
            g.remeshAll();
         });
      }

      oy += row;
      if (shows(oy, row, top, viewH)) {
         ui.toggle("Clouds", o.clouds, x0, oy, colW, 20.0F, "The clouds in the sky.", () -> o.clouds = !o.clouds);
         ui.button("Leaves: " + new String[]{"Fancy", "Smart", "Fast"}[o.leaves], x1, oy, colW, 20.0F, true,
            "Fancy shows every leaf face, Smart hides the ones inside the tree, Fast draws the outside only (the fastest).", () -> {
               o.leaves = (o.leaves + 1) % 3;
               Mesher.leafMode = o.leaves;
               g.remeshAll();
            });
      }

      oy += row;
      if (shows(oy, row, top, viewH)) {
         ui.toggle("Weather", o.weather, x0, oy, colW, 20.0F, "Rain, snow and thunderstorms. Off leaves only fair-weather clouds.", () -> o.weather = !o.weather);
         ui.button("Look: " + Look.label(o.look), x1, oy, colW, 20.0F, true,
            Look.info(o.look) + " Click for the next look: Classic, Vivid, Cinematic, Dreamy. Needs Shaders ON.", () -> o.look = Look.next(o.look));
      }

      oy += row;
      if (shows(oy, row, top, viewH)) {
         ui.button("Effects: " + new String[]{"\u00a77LOW", "\u00a7aMEDIUM", "\u00a7bHIGH"}[Math.max(0, Math.min(2, o.fx))], x0, oy, colW, 20.0F, true,
            "LOW: the picture of Update 3.18. MEDIUM: lava, fire and torches glow, clouds throw shadows on the land, the ground is wet and shiny in rain. HIGH: also soft shadows in corners and under trees (a little slower). Needs Shaders ON.",
            () -> o.fx = (o.fx + 1) % 3);
         ui.toggle("Bushy Leaves", o.bushyLeaves, x1, oy, colW, 20.0F,
            "3D foliage fronds protruding from tree canopies so trees look full and organic instead of flat cubes.", () -> {
               o.bushyLeaves = !o.bushyLeaves;
               Mesher.bushyLeaves = o.bushyLeaves;
               g.remeshAll();
            });
      }
   }

   static void gameTab(Game g, Options o, float x0, float x1, float colW, float oy, float row, float top, float viewH) {
      Ui ui = g.ui;
      if (shows(oy, row, top, viewH)) {
         ui.slider("Sensitivity: " + (int)(o.sensitivity * 200.0F) + "%", x0, oy, colW, 20.0F, o.sensitivity, "How fast the view turns with the mouse.", v -> o.sensitivity = v);
         ui.toggle("Invert Mouse", o.invertY, x1, oy, colW, 20.0F, "Moving the mouse up looks down (like a flight stick).", () -> o.invertY = !o.invertY);
      }

      oy += row;
      if (shows(oy, row, top, viewH)) {
         ui.toggle("View Bobbing", o.bobbing, x0, oy, colW, 20.0F, "Your hand sways while you walk.", () -> o.bobbing = !o.bobbing);
         ui.toggle("Disasters", o.disasters, x1, oy, colW, 20.0F, "Tornadoes, earthquakes and meteors now and then. /disaster on|off changes it for one world.", () -> o.disasters = !o.disasters);
      }

      oy += row;
      if (shows(oy, row, top, viewH)) {
         ui.button("Resource Packs...", x0, oy, colW, 20.0F, true,
            "Switch texture packs (Default V4.0, OG Classic 3.5, Vibrant, Retro 8-Bit, or custom packs in resourcepacks/).", () -> {
               g.packsReturn = Game.S.OPTIONS;
               g.setScreen(Game.S.PACKS);
            });
         ui.button("Mods (" + ModLoader.activeCount() + ")...", x1, oy, colW, 20.0F, true,
            "Browse the Modrinth-style Mod Catalog, toggle installed mods, or launch the external Mod Manager.", () -> {
               g.modsReturn = Game.S.OPTIONS;
               g.setScreen(Game.S.MODS);
            });
      }
   }

   static void controlsTab(Game g, float x0, float x1, float oy) {
      Gui gui = g.gui;
      int half = (KEYS.length + 1) / 2;
      for (int i = 0; i < KEYS.length; i++) {
         float x = i < half ? x0 : x1;
         float y = oy + 2.0F + (i % half) * 11.0F;
         gui.text(KEYS[i][0], x + 2.0F, y, Ui.GOLD, true);
         gui.text(Game.clipText(KEYS[i][1], 104.0F), x + 58.0F, y, Ui.TEXT, true);
      }
   }

   // =================================================================== game menu (pause)
   static void pause(Game g) {
      Ui ui = g.ui;
      Gui gui = g.gui;
      ui.begin();
      gui.gradient(0, 0, gui.w, gui.h, 0x60101010, 0xB0000000);
      float cx = gui.w / 2.0F;
      float bw = Math.min(200.0F, gui.w - 40.0F);
      float half = (bw - 4.0F) / 2.0F;
      float y = Math.max(34.0F, gui.h / 4.0F);
      ui.title("Game Menu", y - 34.0F);
      gui.centered(Game.clipText(g.worldDisplayName, gui.w - 20.0F), cx, y - 22.0F, Ui.TEXT, true, 1.0F);
      String info = g.worldSettings.typeName() + "  -  " + MODE_NAME[Math.max(0, Math.min(MODE_NAME.length - 1, g.player == null ? 1 : g.player.rawMode()))];
      gui.centered(Game.clipText(info, gui.w - 20.0F), cx, y - 11.0F, Ui.DIM, true, 1.0F);
      float bx = cx - bw / 2.0F;
      float third = (bw - 8.0F) / 3.0F;
      ui.button("Back to Game", bx, y, bw, 20.0F, true, null, () -> g.setScreen(Game.S.PLAY));
      ui.button("Advancements", bx, y + 28.0F, half, 20.0F, true, null, () -> g.setScreen(Game.S.ADV));
      ui.button("Options...", bx + half + 4.0F, y + 28.0F, half, 20.0F, true, null, () -> {
         g.optionsReturn = Game.S.PAUSE;
         g.setScreen(Game.S.OPTIONS);
      });
      ui.button("Skins...", bx, y + 52.0F, third, 20.0F, true, null, () -> {
         g.skinReturn = Game.S.PAUSE;
         g.setScreen(Game.S.SKINS);
      });
      ui.button("Packs...", bx + third + 4.0F, y + 52.0F, third, 20.0F, true, null, () -> {
         g.packsReturn = Game.S.PAUSE;
         g.setScreen(Game.S.PACKS);
      });
      ui.button("Mods...", bx + 2.0F * (third + 4.0F), y + 52.0F, third, 20.0F, true, null, () -> {
         g.modsReturn = Game.S.PAUSE;
         g.setScreen(Game.S.MODS);
      });
      ui.button(Net319.hostLabel(), bx, y + 76.0F, half, 20.0F, true, null, () -> {
         Net319.toggleHost(g);
         ui.say(Net319.isHosting() ? "Hosting on LAN (port " + Net319.DEFAULT_PORT + ")" : "Stopped LAN host");
      });
      ui.button(Net319.joinLabel(), bx + half + 4.0F, y + 76.0F, half, 20.0F, true, null, () -> {
         Net319.toggleJoin(g);
         ui.say(Net319.isConnected() ? "Connected to LAN world" : "Not connected");
      });
      ui.button("Save and Quit to Title", bx, y + 104.0F, bw, 20.0F, true, null, g::quitToTitle);
      ui.drawToast();
   }

   // =================================================================== Resource Packs screen
   static void resourcePacks(Game g) {
      Ui ui = g.ui;
      Gui gui = g.gui;
      ui.begin();
      ui.backdrop(0.5F);
      float cx = gui.w / 2.0F;
      ui.title("Resource Packs", 8.0F);
      float pw = Math.min(330.0F, gui.w - 20.0F);
      float px = cx - pw / 2.0F;
      float py = 24.0F;
      float footer = 30.0F;
      float ph = gui.h - py - footer;
      ui.panel(px, py, pw, ph);

      java.util.List<ResourcePacks.Pack> list = ResourcePacks.list();
      String activeId = ResourcePacks.activeId();
      float rowH = 30.0F;
      float content = list.size() * rowH + 4.0F;
      ui.beginScroll(px, py, pw, ph, ui.packScroll, content, rowH);
      for (int i = 0; i < list.size(); i++) {
         float ry = py + 3.0F + i * rowH - ui.packScroll[0];
         if (ry < py - 0.5F || ry + rowH - 2.0F > py + ph + 0.5F) {
            continue;
         }
         ResourcePacks.Pack p = list.get(i);
         boolean isAct = p.id.equalsIgnoreCase(activeId);
         float rx = px + 4.0F;
         float rw = pw - 8.0F;
         float rh = rowH - 2.0F;
         gui.rect(rx, ry, rw, rh, isAct ? 0xFF4CAF50 : 0xFF3A3C48);
         gui.rect(rx + 1, ry + 1, rw - 2, rh - 2, isAct ? 0xFF1E2D24 : 0xFF161820);
         gui.rect(rx + 4, ry + 4, 20, rh - 8.0F, p.badgeColor);
         gui.centered(p.classicBase ? "OG" : "V4", rx + 14.0F, ry + rh / 2.0F - 4.0F, 0xFFFFFFFF, true, 1.0F);

         float btnW = 54.0F;
         float tx = rx + 28.0F;
         float tw = rw - 32.0F - btnW - 6.0F;
         gui.text(Game.clipText(p.title, tw), tx, ry + 4.0F, 0xFFFFFFFF, true);
         gui.text(Game.clipText(p.subtitle, tw), tx, ry + 15.0F, Ui.DIM, true);

         final String pid = p.id;
         final String ptitle = p.title;
         ui.button(isAct ? "\u00a7aActive" : "Select", rx + rw - btnW - 4.0F, ry + 4.0F, btnW, 20.0F, !isAct,
            "Apply the '" + ptitle + "' resource pack immediately.",
            () -> {
               ResourcePacks.select(pid, g);
               ui.say("Activated " + ptitle);
            });
      }
      ui.endScroll(px, py, pw, ph, content);

      float by = gui.h - 24.0F;
      float third = (pw - 8.0F) / 3.0F;
      ui.button("Refresh", px, by, third, 20.0F, true, "Scan resourcepacks/ for newly added folders or .zip packs.", () -> {
         ResourcePacks.refresh();
         ui.say("Refreshed packs (" + ResourcePacks.list().size() + ")");
      });
      ui.button("Mods...", px + third + 4.0F, by, third, 20.0F, true, "Open the Modrinth-style Mods manager.", () -> {
         g.modsReturn = g.packsReturn != null ? g.packsReturn : Game.S.TITLE;
         g.setScreen(Game.S.MODS);
      });
      ui.button("Done", px + 2.0F * (third + 4.0F), by, third, 20.0F, true, null,
         () -> g.setScreen(g.packsReturn != null ? g.packsReturn : Game.S.TITLE));
      ui.drawToast();
   }

   // =================================================================== Mods screen (In-game Modrinth Hub)
   static final String[] MOD_TABS = {"Modrinth Catalog", "Installed Mods"};

   static void mods(Game g) {
      ModLoader.activeGame = g;
      Ui ui = g.ui;
      Gui gui = g.gui;
      ui.begin();
      ui.backdrop(0.5F);
      float cx = gui.w / 2.0F;
      float pw = Math.min(330.0F, gui.w - 20.0F);
      float px = cx - pw / 2.0F;
      ui.title("Mods (" + ModLoader.activeCount() + " Active)", 7.0F);
      ui.tabs(MOD_TABS, ui.modTab, px, 20.0F, pw, 16.0F, i -> {
         ui.modTab = i;
         ui.modScroll[0] = 0.0F;
      });

      float py = 40.0F;
      float footer = 28.0F;
      float ph = gui.h - py - footer;
      ui.panel(px, py, pw, ph);

      java.util.List<ModLoader.ModEntry> list = ui.modTab == 0 ? ModLoader.allMods() : ModLoader.installedMods();
      float rowH = 30.0F;
      if (list.isEmpty()) {
         gui.centered("No mods installed yet.", cx, py + ph / 2.0F - 10.0F, Ui.TEXT, true, 1.0F);
         gui.centered("Open 'Modrinth Catalog' tab to install mods!", cx, py + ph / 2.0F + 2.0F, Ui.DIM, true, 1.0F);
      } else {
         float content = list.size() * rowH + 4.0F;
         ui.beginScroll(px, py, pw, ph, ui.modScroll, content, rowH);
         for (int i = 0; i < list.size(); i++) {
            float ry = py + 3.0F + i * rowH - ui.modScroll[0];
            if (ry < py - 0.5F || ry + rowH - 2.0F > py + ph + 0.5F) {
               continue;
            }
            ModLoader.ModEntry m = list.get(i);
            float rx = px + 4.0F;
            float rw = pw - 8.0F;
            float rh = rowH - 2.0F;
            boolean active = m.installed && m.enabled;
            gui.rect(rx, ry, rw, rh, active ? 0xFF1BD96A : 0xFF3A3C48);
            gui.rect(rx + 1, ry + 1, rw - 2, rh - 2, active ? 0xFF1A2922 : 0xFF161820);
            gui.rect(rx + 4, ry + 4, 18, rh - 8.0F, m.badgeColor);
            gui.centered(m.name.substring(0, 1).toUpperCase(), rx + 13.0F, ry + rh / 2.0F - 4.0F, 0xFFFFFFFF, true, 1.0F);

            float btnW = 56.0F;
            float tx = rx + 26.0F;
            float tw = rw - 30.0F - btnW - 6.0F;
            gui.text(Game.clipText(m.name + " v" + m.version + " (" + m.category + ")", tw), tx, ry + 4.0F, 0xFFFFFFFF, true);
            gui.text(Game.clipText(m.summary, tw), tx, ry + 15.0F, Ui.DIM, true);

            final String mid = m.id;
            final String mname = m.name;
            if (!m.installed) {
               ui.button("Install", rx + rw - btnW - 4.0F, ry + 4.0F, btnW, 20.0F, true,
                  "Install and enable '" + mname + "'.",
                  () -> {
                     ModLoader.install(mid);
                     ui.say("Installed " + mname);
                  });
            } else {
               ui.button(m.enabled ? "\u00a7aON" : "\u00a7cOFF", rx + rw - btnW - 4.0F, ry + 4.0F, btnW, 20.0F, true,
                  "Toggle '" + mname + "' ON or OFF.",
                  () -> {
                     ModLoader.toggleEnabled(mid);
                     ui.say(mname + (ModLoader.get(mid).enabled ? " enabled" : " disabled"));
                  });
            }
         }
         ui.endScroll(px, py, pw, ph, content);
      }

      float by = gui.h - 24.0F;
      float third = (pw - 8.0F) / 3.0F;
      ui.button("Refresh", px, by, third, 20.0F, true, "Reload mods from the mods/ folder.", () -> {
         ModLoader.refresh();
         ui.say("Reloaded mods (" + ModLoader.activeCount() + " active)");
      });
      ui.button("Desktop Hub", px + third + 4.0F, by, third, 20.0F, true,
         "Open the standalone Modrinth-style Desktop Mod & Resource Pack Manager window.",
         () -> {
            ModManagerMain.openInProcess(g);
            ui.say("Opened Desktop Mod Manager");
         });
      ui.button("Done", px + 2.0F * (third + 4.0F), by, third, 20.0F, true, null,
         () -> g.setScreen(g.modsReturn != null ? g.modsReturn : Game.S.TITLE));
      ui.drawToast();
   }

   // =================================================================== loading
   static void loading(Game g) {
      Ui ui = g.ui;
      Gui gui = g.gui;
      ui.begin();
      gui.dirtBackground(0.25F);
      gui.gradient(0, 0, gui.w, gui.h, 0x20000000, 0x90000000);
      float cx = gui.w / 2.0F;
      float cy = gui.h / 2.0F;
      gui.centered(Game.clipText(g.worldDisplayName, gui.w - 20.0F), cx, cy - 34.0F, 0xFFFFFFFF, true, 1.0F);
      gui.centered("Building the terrain...", cx, cy - 20.0F, Ui.DIM, true, 1.0F);
      float frac = Math.max(0.0F, Math.min(1.0F, g.loadFrac));

      float bw = 140.0F;
      gui.rect(cx - bw / 2.0F - 1, cy - 4.0F, bw + 2, 8.0F, 0xFF000000);
      gui.rect(cx - bw / 2.0F, cy - 3.0F, bw, 6.0F, 0xFF2A2C34);
      if (frac > 0.0F) {
         gui.gradient(cx - bw / 2.0F, cy - 3.0F, bw * frac, 6.0F, 0xFF8FE060, 0xFF4E9A36);
      }

      gui.centered(g.loadingMsg, cx, cy + 8.0F, Ui.FAINT, true, 1.0F);
      int tip = (int)((g.worldName.hashCode() & 0x7FFFFFFF) % TIPS.length);
      if (ui.time() > 0) {
         tip = (int)(ui.time() / 6.0 + (g.worldName.hashCode() & 0x7FFF)) % TIPS.length;
      }

      float tw = Math.min(300.0F, gui.w - 24.0F);
      float ty = cy + 30.0F;
      gui.centered("Did you know?", cx, ty, Ui.GOLD, true, 1.0F);
      java.util.List<String> lines = Game.wrapText(TIPS[tip], tw);
      for (int i = 0; i < lines.size() && i < 2; i++) {
         gui.centered(lines.get(i), cx, ty + 11.0F + i * 10.0F, Ui.TEXT, true, 1.0F);
      }
   }
}
