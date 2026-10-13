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
      String subTitle = infdev ? "MINECRAFT INFDEV (JUNE 2010)" : ("JAVA & CONSOLE EDITION  -  V" + Game.VERSION_STR);
      float stW = Gui.textWidth(subTitle) + 16.0F;
      gui.gradient(cx - stW * 0.5F, bottom + 8.0F, stW, 11.0F, 0x90121826, 0x900A0D16);
      gui.rect(cx - stW * 0.5F, bottom + 19.0F, stW, 1.0F, infdev ? 0x8063D43E : 0x80FFD84A);
      gui.centered(
         subTitle,
         cx,
         bottom + 10.0F,
         infdev ? 0xFF63D43E : 0xFFFFF4C8,
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
      ui.button("Singleplayer", bx, by, half, 20.0F, true, null, () -> openWorlds(g));
      ui.button("Mini Games", bx + half + 4.0F, by, half, 20.0F, true, null, () -> openMinigames(g, Game.S.TITLE));
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
      ui.button("All Blocks", bx, by + row * 3, half, 20.0F, true,
         "Open the All Blocks & Items Showcase Gallery with every block & item laid out!", () -> {
            g.ensureAllBlocksShowcaseWorld();
            g.worldName = "All_Blocks_Showcase";
            g.startWorld(false, null, 1);
         });
      ui.button("Quit Game", bx + half + 4.0F, by + row * 3, half, 20.0F, true, null, () -> GLFW.glfwSetWindowShouldClose(g.window, true));
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
         ui.button("Nick: " + Net319.displayNick(), x1, by4, colW, 20.0F, true,
            "Cycle your Multiplayer Nickname (or use /nick <name> in-game).",
            Net319::cyclePresetNick);
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
      ui.button("Play World", px, by, third, 20.0F, any, "Opens the world you picked (double-click a world to open it faster).", () -> g.startWorld(false, null, 1));
      ui.button("All Blocks", px + third + 4.0F, by, third, 20.0F, true,
         "Open the All Blocks & Items Showcase Gallery with every single block, plant, crop, bed, banner & item chest laid out!", () -> {
            g.ensureAllBlocksShowcaseWorld();
            g.worldName = "All_Blocks_Showcase";
            g.startWorld(false, null, 1);
         });
      ui.button("Mini Games", px + 2 * (third + 4.0F), by, third, 20.0F, true, "Play Minecraft Console Edition Battle, Tumble, and Bedwars Mini Games across 4 arenas!", () -> openMinigames(g, Game.S.WORLDS));
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
      int border = sel ? 0xFF68F080 : (hov ? 0xFF7892BE : 0xFF2E3548);
      gui.rect(x, y, w, h, border);
      int topFill = sel ? 0xFF28384E : (hov ? 0xFF22293B : 0xFF151926);
      int botFill = sel ? 0xFF1A2536 : (hov ? 0xFF181D2B : 0xFF0F121C);
      gui.gradient(x + 1, y + 1, w - 2, h - 2, topFill, botFill);
      if (sel) {
         gui.rect(x + 1, y + 1, 2.0F, h - 2, 0xFF68F080);
      }
      float ts = h - 6.0F;
      gui.rect(x + 3, y + 2, ts + 2, ts + 2, 0xFF080A10);
      if (wi.thumb != null) {
         gui.image(wi.thumb.img, x + 4, y + 3, ts, ts);
      }

      float tx = x + ts + 10.0F;
      float tw = w - (tx - x) - 4.0F;
      String when = ago(wi.mod);
      float ww = Gui.textWidth(when);
      gui.text(when, x + w - ww - 4.0F, y + 4.0F, Ui.FAINT, true);
      gui.text(Game.clipText(wi.name, tw - ww - 8.0F), tx, y + 4.0F, sel ? 0xFFFFF6B8 : 0xFFF4F8FF, true);
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
         } else if (ws.type == WorldSettings.ALL_BLOCKS) {
            g.newMode = 1; // All Blocks Gallery defaults to Creative Mode
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
      float recY = Math.max(leftEnd, rightEnd) + 2.0F;
      gui.text("Large Biomes recommended", x0, recY, 0xFFE8D068, true);
      float seedInfoY = recY + 12.0F;
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
            rows = 6;
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

      oy += row;
      if (shows(oy, row, top, viewH)) {
         ui.slider("Sound: " + (o.soundVolume <= 0.0F ? "OFF" : Math.round(o.soundVolume * 100.0F) + "%"), x0, oy, colW, 20.0F, o.soundVolume,
            "Volume of realistic world sound effects (footsteps, mining, doors, chests, fire, water, explosions).", v -> {
               o.soundVolume = v;
               SoundEngine.setVolumes(o.soundVolume, o.musicVolume, o.realAcoustics);
            });
         ui.slider("Music: " + (o.musicVolume <= 0.0F ? "OFF" : Math.round(o.musicVolume * 100.0F) + "%"), x1, oy, colW, 20.0F, o.musicVolume,
            "Volume of the Minecraft Console Edition & Battle Minigame soundtrack.", v -> {
               o.musicVolume = v;
               SoundEngine.setVolumes(o.soundVolume, o.musicVolume, o.realAcoustics);
            });
      }

      oy += row;
      if (shows(oy, row, top, viewH)) {
         ui.toggle("Constant Music", o.constantMusic, x0, oy, colW, 20.0F,
            "OFF: music plays randomly with natural quiet delays like real Minecraft. ON: music plays constantly back-to-back without waiting.", () -> {
               o.constantMusic = !o.constantMusic;
               SoundEngine.setConstantMusic(o.constantMusic);
            });
         ui.toggle("3D Acoustics", o.realAcoustics, x1, oy, colW, 20.0F,
            "Real-life sound physics: wall muffling/occlusion, 3D cave reverb & echo, speed-of-sound delay (343 m/s), and Doppler shift.", () -> {
               o.realAcoustics = !o.realAcoustics;
               SoundEngine.setVolumes(o.soundVolume, o.musicVolume, o.realAcoustics);
            });
      }

      oy += row;
      if (shows(oy, row, top, viewH)) {
         ui.button("Next Song", x0, oy, colW, 20.0F, true,
            "Now playing: " + SoundEngine.currentMusicDisplayName() + " — Click to skip to the next Console Edition track (33 tracks).", () -> {
               SoundEngine.nextBattleTrack();
               SoundEngine.tick(g);
               ui.say("Music: " + SoundEngine.currentMusicDisplayName());
            });
         ui.button("End Credits...", x1, oy, colW, 20.0F, true,
            "View the End Poem and the full Music & Sound Credits.", () -> {
               g.creditsReturn = Game.S.OPTIONS;
               g.creditsScroll = 0.0F;
               g.setScreen(Game.S.CREDITS);
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
      boolean isMg = g.world != null && g.world.settings != null && g.world.settings.isMinigame();
      if (isMg && !Net319.isClientConnected()) {
         ui.button("New Round", bx, y + 104.0F, third, 20.0F, true,
            "Reset the arena map and start a fresh round with all AI competitors respawned.",
            () -> SkyGen.startMatch(g, true));
         ui.button("Lobby / Mode", bx + third + 4.0F, y + 104.0F, third, 20.0F, true,
            "Open the Waiting Lobby to switch Minigame Modes, vote on Maps, or change Teams without disconnecting players.",
            () -> SkyGen.enterInterRoundLobby(g));
         ui.button("Leave", bx + 2.0F * (third + 4.0F), y + 104.0F, third, 20.0F, true, null, g::quitToTitle);
      } else {
         ui.button(isMg ? "Leave Minigame" : "Save and Quit to Title", bx, y + 104.0F, bw, 20.0F, true, null, g::quitToTitle);
      }
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
         gui.centered(p.badgeText != null ? p.badgeText : (p.classicBase ? "3.5" : "5.9"), rx + 14.0F, ry + rh / 2.0F - 4.0F, 0xFFFFFFFF, true, 1.0F);

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

   // =================================================================== V4.3/V5.5: Console Edition Mini Games Hub
   static final String[] MINIGAME_TABS = {"Brawl", "Tumbl", "Beds", "CS2", "Murdr", "End", "Siege"};

   static void openMinigames(Game g, Game.S ret) {
      g.mgReturn = ret != null ? ret : Game.S.TITLE;
      if (!g.newSettings.isMinigame()) {
         g.newSettings.type = WorldSettings.BATTLE;
         g.mgTab = 0;
      } else {
         g.mgTab = 0;
         for (int i = 0; i < WorldSettings.MINIGAME_ORDER.length; i++) {
            if (WorldSettings.MINIGAME_ORDER[i] == g.newSettings.type) {
               g.mgTab = i;
               break;
            }
         }
      }
      g.newMode = 0;
      g.ui.randomSeed = new Random().nextLong();
      g.setScreen(Game.S.MINIGAMES);
   }

   static void minigames(Game g) {
      Ui ui = g.ui;
      Gui gui = g.gui;
      ui.begin();
      ui.backdrop(0.5F);
      WorldSettings ws = g.newSettings;
      if (!ws.isMinigame()) {
         ws.type = WorldSettings.MINIGAME_ORDER[Math.max(0, Math.min(WorldSettings.MINIGAME_ORDER.length - 1, g.mgTab))];
      }
      float cx = gui.w / 2.0F;
      float pw = Math.min(316.0F, gui.w - 16.0F);
      float px = cx - pw / 2.0F;
      ui.title("Console Edition Mini Games", 7.0F);
      ui.tabs(MINIGAME_TABS, g.mgTab, px, 20.0F, pw, 16.0F, i -> {
         g.mgTab = i;
         ws.type = WorldSettings.MINIGAME_ORDER[i];
         g.newMode = 0;
      });

      float py = 40.0F;
      float footY = gui.h - 24.0F;
      float ph = footY - py - 4.0F;
      ui.panel(px, py, pw, ph);

      float ix = px + 6.0F;
      float iw = pw - 12.0F;
      float colW = (iw - 6.0F) / 2.0F;
      float x1 = ix + colW + 6.0F;

      // Left column: 8 Mode-Specific Arena Map Buttons + Loadout / Bot Options + Music Track
      ui.label("Select Arena Map", ix, py + 4.0F, Ui.GOLD);
      float mW = (colW - 4.0F) / 2.0F;
      for (int m = 0; m < WorldSettings.MAP_COUNT; m++) {
         final int mapIdx = m;
         float mx = ix + (m % 2) * (mW + 4.0F);
         float my = py + 13.0F + (m / 2) * 18.0F;
         boolean sel = ws.mapId == m;
         String mName = WorldSettings.mapNameFor(ws.type, m);
         String mTheme = WorldSettings.mapThemeFor(ws.type, m);
         String mInfo = WorldSettings.mapInfoFor(ws.type, m);
         String rawLbl = sel ? "[" + mName + "]" : mName;
         String lbl = Game.clipText(rawLbl, mW - 8.0F);
         ui.button(lbl, mx, my, mW, 16.0F, true,
            mName + " (" + mTheme + "): " + mInfo,
            () -> {
               ws.mapId = mapIdx;
               ui.say("Map: " + WorldSettings.mapNameFor(ws.type, mapIdx));
            });
      }

      float optY0 = py + 86.0F;
      if (ws.type == WorldSettings.BATTLE) {
         String kitName = SkyGen.SW_KIT_NAMES[SkyGen.swKit & 3];
         ui.button("Set: " + kitName, ix, optY0, colW, 18.0F, true,
            "Battle Item Set: " + SkyGen.SW_KIT_DESC[SkyGen.swKit & 3],
            () -> {
               SkyGen.swKit = (SkyGen.swKit + 1) & 3;
               ui.say("Item Set: " + SkyGen.SW_KIT_NAMES[SkyGen.swKit]);
            });
      } else if (ws.type == WorldSettings.TUMBLE) {
         String wmLabel = SkyGen.tumbleWeaponMode == 0 ? "Mixed" : (SkyGen.tumbleWeaponMode == 1 ? "Shovels" : "Snowballs");
         ui.button("Mode: " + wmLabel, ix, optY0, colW, 18.0F, true,
            "Tumble Layer Weapons: Mixed (Shovels + Snowballs), Shovels Only, or Snowballs Only.",
            () -> {
               SkyGen.tumbleWeaponMode = (SkyGen.tumbleWeaponMode + 1) % 3;
               ui.say("Tumble: " + SkyGen.TUMBLE_MODE_NAMES[SkyGen.tumbleWeaponMode]);
            });
      } else if (ws.type == WorldSettings.CS2) {
         String kitName = SkyGen.CS2_KIT_NAMES[SkyGen.swKit & 3];
         ui.button(Game.clipText("Kit: " + kitName, colW - 8.0F), ix, optY0, colW, 18.0F, true,
            "CS2 Strike Loadout: " + SkyGen.CS2_KIT_DESC[SkyGen.swKit & 3],
            () -> {
               SkyGen.swKit = (SkyGen.swKit + 1) & 3;
               ui.say("CS2 Kit: " + SkyGen.CS2_KIT_NAMES[SkyGen.swKit]);
            });
      } else if (ws.type == WorldSettings.MURDER) {
         String rolePref = SkyGen.MURDER_PREF_NAMES[SkyGen.murderRolePref & 3];
         ui.button("Role: " + rolePref, ix, optY0, colW, 18.0F, true,
            "Select preferred Murder Mystery role (Random, Innocent, Sheriff, or Murderer).",
            () -> {
               SkyGen.murderRolePref = (SkyGen.murderRolePref + 1) & 3;
               ui.say("Role Preference: " + SkyGen.MURDER_PREF_NAMES[SkyGen.murderRolePref]);
            });
      } else if (ws.type == WorldSettings.END_FIGHT) {
         String kitName = SkyGen.END_FIGHT_KIT_NAMES[SkyGen.swKit & 3];
         ui.button(Game.clipText("Kit: " + kitName, colW - 8.0F), ix, optY0, colW, 18.0F, true,
            "End Fight Loadout: " + SkyGen.END_FIGHT_KIT_DESC[SkyGen.swKit & 3],
            () -> {
               SkyGen.swKit = (SkyGen.swKit + 1) & 3;
               ui.say("End Fight Kit: " + SkyGen.END_FIGHT_KIT_NAMES[SkyGen.swKit]);
            });
      } else if (ws.type == WorldSettings.CASTLE_SIEGE) {
         String kitName = SkyGen.SIEGE_KIT_NAMES[SkyGen.swKit & 3];
         ui.button(Game.clipText("Kit: " + kitName, colW - 8.0F), ix, optY0, colW, 18.0F, true,
            "Castle Siege Loadout: " + SkyGen.SIEGE_KIT_DESC[SkyGen.swKit & 3],
            () -> {
               SkyGen.swKit = (SkyGen.swKit + 1) & 3;
               ui.say("Siege Kit: " + SkyGen.SIEGE_KIT_NAMES[SkyGen.swKit]);
            });
      } else {
         int t = SkyGen.myTeam & 3;
         ui.button("Team: " + SkyGen.BEDWARS_NAMES[t], ix, optY0, colW, 18.0F, true,
            "Select your starting Bedwars Team Base (Red, Blue, Green, or Yellow).",
            () -> {
               SkyGen.myTeam = (SkyGen.myTeam + 1) & 3;
               ui.say("Team: " + SkyGen.BEDWARS_NAMES[SkyGen.myTeam]);
            });
      }

      float optY1 = optY0 + 19.0F;
      if (ws.type == WorldSettings.CS2) {
         ui.button("Bots: " + (SkyGen.soloBots ? "ON" : "OFF"), ix, optY1, mW, 18.0F, true,
            "Spawn AI Counter-Terrorist and Terrorist bots in solo matches.",
            () -> {
               SkyGen.soloBots = !SkyGen.soloBots;
               ui.say("AI Bots: " + (SkyGen.soloBots ? "ON" : "OFF"));
            });
         ui.button(SkyGen.cs2NoBombs ? "Bombs:OFF" : "Bombs:ON", ix + mW + 4.0F, optY1, mW, 18.0F, true,
            "Toggle between CS2 Bomb Defusal (map-wide bomb sites A & B) and CS2 No-Bombs Team Elimination mode!",
            () -> {
               SkyGen.cs2NoBombs = !SkyGen.cs2NoBombs;
               ui.say("CS2 Mode: " + (SkyGen.cs2NoBombs ? SkyGen.CS2_SUBMODE_NAMES[1] : SkyGen.CS2_SUBMODE_NAMES[0]));
            });
      } else {
         ui.button("AI Bots: " + (SkyGen.soloBots ? "ON" : "OFF"), ix, optY1, colW, 18.0F, true,
            "Spawn AI rival fighters/competitors in solo matches.",
            () -> {
               SkyGen.soloBots = !SkyGen.soloBots;
               ui.say("AI Bots: " + (SkyGen.soloBots ? "ON" : "OFF"));
            });
      }

      float optY2 = optY1 + 19.0F;
      if (optY2 + 18.0F <= py + ph - 4.0F) {
         ui.button("Next Song", ix, optY2, colW, 18.0F, true,
            "Now playing: " + SoundEngine.currentMusicDisplayName() + " — Click to cycle through all 16 Gareth Coker Battle & Tumble tracks!",
            () -> {
               SoundEngine.nextBattleTrack();
               SoundEngine.tick(g);
               ui.say("Music: " + SoundEngine.currentMusicDisplayName());
            });
      }

      // Right column: Live Arena Preview + Map Theme + Mode Info
      float pvH = Math.min(68.0F, ph * 0.42F);
      if (ui.preview == null) {
         ui.preview = new WorldPreview(96, 60, 20, true);
      }
      ui.preview.update(ui.randomSeed + ws.mapId * 1337L, ws);
      gui.rect(x1 - 1, py + 5.0F, colW + 2, pvH + 2, 0xFF000000);
      gui.image(ui.preview.img, x1, py + 6.0F, colW, pvH);

      float infoY = py + pvH + 11.0F;
      gui.text(Game.clipText(ws.mapName() + " - " + ws.mapTheme(), colW), x1, infoY, Ui.GOLD, true);
      if (infoY + 12.0F < py + ph - 6.0F) {
         ui.paragraph(WorldSettings.mapInfoFor(ws.type, ws.mapId) + " " + WorldSettings.TYPE_INFO[ws.type], x1, infoY + 11.0F, colW, Ui.DIM, 4);
      }

      float third = (pw - 8.0F) / 3.0F;
      ui.button("Play Now", px, footY, third, 20.0F, true,
         "Jump straight into the " + ws.typeName() + " (" + ws.mapName() + ") match!",
         () -> {
            g.newMode = 0;
            g.startWorld(true, Long.toString(ui.randomSeed), 0);
            SkyGen.startMatch(g, false);
         });
      ui.button("Open Lobby", px + third + 4.0F, footY, third, 20.0F, true,
         "Open the Waiting Lobby to invite LAN / Radmin VPN / Share Code friends before starting.",
         () -> {
            g.newMode = 0;
            SkyGen.matchState = SkyGen.STATE_WAITING;
            g.startWorld(true, Long.toString(ui.randomSeed), 0);
         });
      ui.button("Back", px + 2.0F * (third + 4.0F), footY, third, 20.0F, true, null,
         () -> g.setScreen(g.mgReturn != null ? g.mgReturn : Game.S.TITLE));
      ui.drawToast();
   }

   // =================================================================== V4.1/V4.3: Minigame Overview & Waiting Lobby Screen
   static void waiting(Game g) {
      Ui ui = g.ui;
      Gui gui = g.gui;
      ui.begin();
      gui.gradient(0, 0, gui.w, gui.h, 0x6810141C, 0xC006080C);
      float cx = gui.w / 2.0F;
      int wType = (g.world != null && g.world.settings != null) ? g.world.settings.type : WorldSettings.BATTLE;
      String mapName = (g.world != null && g.world.settings != null) ? g.world.settings.mapName() : "Cove";
      boolean isBedwars = wType == WorldSettings.BEDWARS;
      boolean isTumble = wType == WorldSettings.TUMBLE;
      boolean isCs2 = wType == WorldSettings.CS2;
      boolean isMurder = wType == WorldSettings.MURDER;
      boolean isEndFight = wType == WorldSettings.END_FIGHT;
      boolean isCastleSiege = wType == WorldSettings.CASTLE_SIEGE;

      float pw = Math.min(300.0F, gui.w - 16.0F);
      float px = cx - pw / 2.0F;
      float py = 21.0F;
      float footY = gui.h - 24.0F;
      float ph = footY - py - 4.0F;
      float ix = px + 6.0F;
      float iw = pw - 12.0F;
      float colW = (iw - 6.0F) / 2.0F;
      float x1 = ix + colW + 6.0F;
      float iy = py + 5.0F;
      float qW = (iw - 12.0F) / 4.0F;

      // 1. POST-MATCH OVERVIEW SCREEN (shown after winning or completing a round before entering the inter-round lobby!)
      if (SkyGen.showingOverview) {
         String ovTitle = (SkyGen.lastRoundWon ? "Victory Overview - " : "Match Overview - ") + SkyGen.lastRoundMode;
         ui.title(ovTitle, 7.0F);
         ui.panel(px, py, pw, ph);

         String banner = SkyGen.lastRoundWon
            ? "CHAMPION OF THE ARENA! (#1 Place Victory)"
            : "ROUND COMPLETE - MATCH STANDINGS";
         gui.text(Game.clipText(banner, iw), ix, iy, SkyGen.lastRoundWon ? Ui.GOLD : 0xFF8FE060, true);

         String pod1 = "1st Place: " + SkyGen.lastRoundFirst + "  |  Arena: " + SkyGen.lastRoundMap;
         String pod2 = "2nd: " + SkyGen.lastRoundSecond + "  |  3rd: " + SkyGen.lastRoundThird;
         String stats = String.format("Kills: %d  |  Beds Broken: %d  |  Match Time: %d:%02d",
            SkyGen.lastRoundKills, SkyGen.lastRoundBedsBroken, SkyGen.lastRoundDurationSec / 60, SkyGen.lastRoundDurationSec % 60);
         gui.text(Game.clipText(pod1, iw), ix, iy + 13.0F, 0xFF68F080, true);
         gui.text(Game.clipText(pod2, iw), ix, iy + 25.0F, Ui.TEXT, true);
         gui.text(Game.clipText(stats, iw), ix, iy + 37.0F, Ui.DIM, true);

         gui.text(Game.clipText("Vote for Next Round's Arena Map:", iw), ix, iy + 50.0F, Ui.GOLD, true);
         float voteY = iy + 60.0F;
         for (int m = 0; m < WorldSettings.MAP_COUNT; m++) {
            final int mapIdx = m;
            String mName = WorldSettings.mapNameFor(wType, m);
            String shortName = WorldSettings.shortMapNameFor(wType, m);
            int votes = SkyGen.mapVotes[m];
            boolean voted = SkyGen.myMapVote == m;
            String vLbl = voted ? ("*" + shortName + ":" + votes + "*") : (shortName + ":" + votes);
            int col = m & 3;
            int row = m >> 2;
            ui.button(Game.clipText(vLbl, qW - 6.0F), ix + col * (qW + 4.0F), voteY + row * 19.0F, qW, 17.0F, true,
               "Vote to play next round on " + mName + " Arena.",
               () -> {
                  SkyGen.castMapVote(g, mapIdx, false);
                  ui.say("Voted for " + mName + " (" + SkyGen.mapVotes[mapIdx] + " votes)");
               });
         }

         int ovSecs = Math.max(1, (SkyGen.overviewCountdownTicks + 19) / 20);
         float actY = voteY + 40.0F;
         ui.button("Continue to Lobby (" + ovSecs + "s)", ix, actY, iw, 20.0F, true,
            "Proceed to the Waiting Lobby to wait for the next round, vote on maps, or switch gamemodes.",
            () -> SkyGen.enterInterRoundLobby(g));

         ui.button("Play Next Round Now", ix, footY, colW, 20.0F, true,
            "Completely reset the arena with the winning voted map and start immediately.",
            () -> SkyGen.startMatch(g, true));
         ui.button("Leave Lobby", x1, footY, colW, 20.0F, true, null, g::quitToTitle);
         ui.drawToast();
         return;
      }

      // 2. INTER-ROUND WAITING LOBBY WITH LIVE MAP VOTING & GAMEMODE SWITCHER
      if (SkyGen.myMapVote < 0) {
         SkyGen.initMapVoting(g);
      }
      int leadMap = SkyGen.winningVotedMap(g.world != null && g.world.settings != null ? g.world.settings.mapId : 0);
      String leadMapName = WorldSettings.mapNameFor(wType, leadMap);
      String modeTitle = WorldSettings.TYPE_NAME[Math.max(0, Math.min(WorldSettings.TYPES - 1, wType))];
      String lobbyTitle = modeTitle + " Lobby - " + mapName;
      ui.title(lobbyTitle, 7.0F);
      ui.panel(px, py, pw, ph);

      int totalPlayers = 1 + Net319.peerCount();
      String statusLine;
      if (SkyGen.lobbyCountdownTicks > 0) {
         int secs = (SkyGen.lobbyCountdownTicks + 19) / 20;
         statusLine = "Next round in " + secs + "s! Leading Map Vote: " + leadMapName;
      } else {
         statusLine = "Lobby (" + totalPlayers + "/8) - Vote Map, Switch Mode, or click Start!";
      }
      gui.text(Game.clipText(statusLine, iw), ix, iy, SkyGen.lobbyCountdownTicks > 0 ? 0xFF8FE060 : Ui.GOLD, true);

      int myTeam = SkyGen.getPlayerTeam(g);
      String teamDesc = isBedwars
         ? (SkyGen.BEDWARS_NAMES[myTeam] + " Team")
         : (isCs2
            ? ((myTeam & 1) == 0 ? "CT (Blue)" : "T (Red)")
            : (isCastleSiege
               ? ((myTeam & 1) == 0 ? "Defenders (Blue)" : "Attackers (Red)")
               : (isMurder
                  ? ("Role: " + SkyGen.MURDER_ROLE_NAMES[SkyGen.myMurderRole & 3])
                  : ("Podium #" + (myTeam + 1)))));
      String netRoster;
      if (Net319.isHosting()) {
         netRoster = "Host: " + Net319.publicAddress + " | Code: " + Net319.shareCode;
      } else if (Net319.isConnected()) {
         netRoster = "Connected (" + Net319.peerCount() + " peers) | " + teamDesc;
      } else {
         netRoster = "You: " + teamDesc + " | LAN IP: " + Net319.localLanIp();
      }
      gui.text(Game.clipText(netRoster, iw), ix, iy + 11.0F, Ui.TEXT, true);

      // Row 1 (by0): Team / Spawn Podium / Faction Selection
      float by0 = iy + 22.0F;
      for (int t = 0; t < 4; t++) {
         final int teamIdx = t;
         boolean sel = (isCs2 || isCastleSiege) ? ((myTeam & 1) == (t & 1) && t < 2) : (myTeam == t);
         String lbl;
         String tip;
         if (isBedwars) {
            lbl = sel ? "[" + SkyGen.BEDWARS_NAMES[t] + "]" : SkyGen.BEDWARS_NAMES[t];
            tip = "Join " + SkyGen.BEDWARS_NAMES[t] + " Team base.";
         } else if (isCs2) {
            String[] cs2Btn = new String[]{"CT Blue", "T Red", "Bombs:ON", "Bombs:OFF"};
            if (t < 2) {
               sel = ((myTeam & 1) == t);
               lbl = sel ? "[" + cs2Btn[t] + "]" : cs2Btn[t];
               tip = (t == 0) ? "Join Counter-Terrorists (Defend Sites A & B / Defuse C4)." : "Join Terrorists (Plant C4 at Site A or B).";
            } else {
               boolean wantNoBombs = (t == 3);
               sel = (SkyGen.cs2NoBombs == wantNoBombs);
               lbl = sel ? "[" + cs2Btn[t] + "]" : cs2Btn[t];
               tip = wantNoBombs ? "Play CS2 No-Bombs Mode (Pure Team Elimination without C4)." : "Play CS2 Bomb Defusal Mode (Map-specific Bomb Sites A & B).";
            }
         } else if (isCastleSiege) {
            String[] siegeBtn = new String[]{"Defend", "Attack", "Def #2", "Atk #2"};
            sel = (myTeam == t);
            lbl = sel ? "[" + siegeBtn[t] + "]" : siegeBtn[t];
            tip = (t & 1) == 0 ? "Join Castle Defenders (Hold the Fortress & Control Zones A/B)." : "Join Siege Attackers (Breach the Castle Gates with Cannons & Capture Zones A/B).";
         } else {
            lbl = sel ? "[Spot " + (t + 1) + "]" : "Spot " + (t + 1);
            tip = isMurder ? "Select Mansion starting room #" + (t + 1) + "." : "Select starting podium #" + (t + 1) + ".";
         }
         ui.button(Game.clipText(lbl, qW - 6.0F), ix + t * (qW + 4.0F), by0, qW, 17.0F, true, tip, () -> {
            if (isCs2 && teamIdx >= 2) {
               SkyGen.cs2NoBombs = (teamIdx == 3);
               SkyGen.giveCs2Loadout(g);
               ui.say("CS2 Mode: " + (SkyGen.cs2NoBombs ? SkyGen.CS2_SUBMODE_NAMES[1] : SkyGen.CS2_SUBMODE_NAMES[0]));
               return;
            }
            SkyGen.selectTeam(g, teamIdx, true);
            ui.say(isBedwars
               ? "Selected " + SkyGen.BEDWARS_NAMES[teamIdx] + " Team"
               : (isCs2
                  ? ("Joined " + ((teamIdx & 1) == 0 ? "Counter-Terrorists" : "Terrorists"))
                  : (isCastleSiege
                     ? ("Joined " + ((teamIdx & 1) == 0 ? "Castle Defenders" : "Siege Attackers"))
                     : ("Selected Spot #" + (teamIdx + 1)))));
         });
      }

      // Row 2 & 2b (byVote): Votable Mode-Specific Arena Maps
      float byVote = by0 + 19.0F;
      for (int m = 0; m < WorldSettings.MAP_COUNT; m++) {
         final int mapIdx = m;
         String mName = WorldSettings.mapNameFor(wType, m);
         String shortName = WorldSettings.shortMapNameFor(wType, m);
         int votes = SkyGen.mapVotes[m];
         boolean voted = SkyGen.myMapVote == m;
         String vLbl = voted ? ("*" + shortName + ":" + votes + "*") : (shortName + ":" + votes);
         int col = m & 3;
         int row = m >> 2;
         ui.button(Game.clipText(vLbl, qW - 6.0F), ix + col * (qW + 4.0F), byVote + row * 19.0F, qW, 17.0F, true,
            "Vote for " + mName + " Arena. Winning map loads when round starts!",
            () -> {
               SkyGen.castMapVote(g, mapIdx, true);
               ui.say("Voted for " + mName + " (" + SkyGen.mapVotes[mapIdx] + " votes) — Arena updated!");
            });
      }

      // Row 3 (by1): Start Match & Countdown Timer
      float by1 = byVote + 38.0F;
      ui.button("Start Match", ix, by1, colW, 18.0F, true,
         "Begin the minigame immediately on the winning voted map.",
         () -> SkyGen.startMatch(g, true));
      ui.button(SkyGen.lobbyCountdownTicks > 0 ? "Cancel Timer" : "Countdown (10s)", x1, by1, colW, 18.0F, true,
         "Start or cancel the lobby countdown timer for the next round.",
         () -> {
            if (SkyGen.lobbyCountdownTicks > 0) {
               SkyGen.lobbyCountdownTicks = -1;
               ui.say("Countdown cancelled");
            } else {
               SkyGen.lobbyCountdownTicks = 200;
               ui.say("New round starting in 10 seconds...");
            }
         });

      // Row 4 (by2): Mode Loadout/Settings & Seamless Gamemode Switcher
      float by2 = by1 + 20.0F;
      if (isBedwars) {
         ui.button("Solo Bots: " + (SkyGen.soloBots ? "ON" : "OFF"), ix, by2, colW, 18.0F, true,
            "When ON in solo play, unoccupied enemy bases spawn AI defenders/raiders.",
            () -> {
               SkyGen.soloBots = !SkyGen.soloBots;
               ui.say("Solo AI Bots: " + (SkyGen.soloBots ? "ON" : "OFF"));
            });
      } else if (isTumble) {
         String wmLabel = SkyGen.tumbleWeaponMode == 0 ? "Mixed" : (SkyGen.tumbleWeaponMode == 1 ? "Shovels" : "Snowballs");
         ui.button("Mode: " + wmLabel, ix, by2, colW, 18.0F, true,
            "Select Tumble weapon mode (Mixed, Shovels Only, or Snowballs Only).",
            () -> {
               SkyGen.tumbleWeaponMode = (SkyGen.tumbleWeaponMode + 1) % 3;
               SkyGen.giveTumbleLoadout(g);
               ui.say("Tumble Mode: " + SkyGen.TUMBLE_MODE_NAMES[SkyGen.tumbleWeaponMode]);
            });
      } else if (isCs2) {
         String kitName = SkyGen.CS2_KIT_NAMES[SkyGen.swKit & 3];
         ui.button(Game.clipText("Kit: " + kitName, colW - 8.0F), ix, by2, colW, 18.0F, true,
            "Select CS2 Strike Loadout (" + SkyGen.CS2_KIT_DESC[SkyGen.swKit & 3] + ").",
            () -> {
               SkyGen.swKit = (SkyGen.swKit + 1) & 3;
               SkyGen.giveCs2Loadout(g);
               ui.say("CS2 Kit: " + SkyGen.CS2_KIT_NAMES[SkyGen.swKit]);
            });
      } else if (isMurder) {
         String rolePref = SkyGen.MURDER_PREF_NAMES[SkyGen.murderRolePref & 3];
         ui.button(Game.clipText("Role: " + rolePref, colW - 8.0F), ix, by2, colW, 18.0F, true,
            "Select preferred Murder Mystery role (Random, Innocent, Sheriff, or Murderer).",
            () -> {
               SkyGen.murderRolePref = (SkyGen.murderRolePref + 1) & 3;
               SkyGen.assignMurderMysteryRoles(g);
               SkyGen.giveMurderLoadout(g);
               ui.say("Role Preference: " + SkyGen.MURDER_PREF_NAMES[SkyGen.murderRolePref]);
            });
      } else if (isEndFight) {
         String kitName = SkyGen.END_FIGHT_KIT_NAMES[SkyGen.swKit & 3];
         ui.button(Game.clipText("Kit: " + kitName, colW - 8.0F), ix, by2, colW, 18.0F, true,
            "Select End Fight Bed-Bombing Loadout (" + SkyGen.END_FIGHT_KIT_DESC[SkyGen.swKit & 3] + ").",
            () -> {
               SkyGen.swKit = (SkyGen.swKit + 1) & 3;
               SkyGen.giveEndFightLoadout(g);
               ui.say("End Fight Kit: " + SkyGen.END_FIGHT_KIT_NAMES[SkyGen.swKit]);
            });
      } else if (isCastleSiege) {
         String kitName = SkyGen.SIEGE_KIT_NAMES[SkyGen.swKit & 3];
         ui.button(Game.clipText("Kit: " + kitName, colW - 8.0F), ix, by2, colW, 18.0F, true,
            "Select Castle Siege Loadout (" + SkyGen.SIEGE_KIT_DESC[SkyGen.swKit & 3] + ").",
            () -> {
               SkyGen.swKit = (SkyGen.swKit + 1) & 3;
               SkyGen.giveCastleSiegeLoadout(g);
               ui.say("Siege Kit: " + SkyGen.SIEGE_KIT_NAMES[SkyGen.swKit]);
            });
      } else {
         String kitName = SkyGen.SW_KIT_NAMES[SkyGen.swKit & 3];
         ui.button("Set: " + kitName, ix, by2, colW, 18.0F, true,
            "Select your Battle Item Set (" + SkyGen.SW_KIT_DESC[SkyGen.swKit & 3] + ").",
            () -> {
               int nextKit = (SkyGen.swKit + 1) & 3;
               SkyGen.giveSkywarsKit(g, nextKit);
               ui.say("Item Set: " + SkyGen.SW_KIT_NAMES[nextKit] + " (" + SkyGen.SW_KIT_DESC[nextKit] + ")");
            });
      }
      ui.button(Game.clipText("Game: " + modeTitle, colW - 8.0F), x1, by2, colW, 18.0F, !Net319.isClientConnected(),
         "Switch active Minigame (Battle, Tumble, Bedwars, CS2 Strike, Murder Mystery) without disconnecting players!",
         () -> {
            int curIdx = 0;
            for (int i = 0; i < WorldSettings.MINIGAME_ORDER.length; i++) {
               if (WorldSettings.MINIGAME_ORDER[i] == wType) {
                  curIdx = i;
                  break;
               }
            }
            int nextType = WorldSettings.MINIGAME_ORDER[(curIdx + 1) % WorldSettings.MINIGAME_ORDER.length];
            SkyGen.switchMinigameMode(g, nextType, true);
            ui.say("Switched Game to " + WorldSettings.TYPE_NAME[nextType]);
         });

      // Row 5 (by3): Multiplayer Hosting & Share Code
      float by3 = by2 + 20.0F;
      if (by3 + 18.0F <= py + ph - 2.0F) {
         ui.button(Net319.isHosting() ? "Stop Host (" + Net319.peerCount() + ")" : "Host LAN / VPN", ix, by3, colW, 18.0F, true,
            "Open this minigame lobby on LAN / Radmin VPN / Worldwide so friends can join.",
            () -> {
               Net319.toggleHost(g);
               ui.say(Net319.isHosting() ? "Hosting on " + Net319.publicAddress : "Stopped hosting");
            });
         ui.button("Copy Share Code", x1, by3, colW, 18.0F, true,
            "Copy the Share Code (includes Radmin VPN, LAN, and Worldwide Tunnel endpoints).",
            () -> {
               Net319.copyShareCode(g);
               ui.say("Copied Share Code: " + Net319.shareCode);
            });
      }

      ui.button("Reset Arena", ix, footY, colW, 20.0F, true,
         "Completely regenerate all arena chunks, islands, beds, chests, and clear all placed blocks.",
         () -> {
            SkyGen.resetArenaMap(g, g.world, true);
            ui.say("Arena map completely reset!");
         });
      ui.button("Leave Lobby", x1, footY, colW, 20.0F, true, null, g::quitToTitle);
      ui.drawToast();
   }

   static final String[][] END_POEM_LINES = new String[][]{
      {"#28C8B8", "I see the player you mean."},
      {"#38B848", "Does it know that we love it? That the universe is kind?"},
      {"#28C8B8", "Sometimes, through the noise of its thoughts, it hears the universe, yes."},
      {"#38B848", "Wait. It has reached the higher level. It can read our thoughts."},
      {"#28C8B8", "It thinks we are part of the game. I like this player. It played well. It did not give up."},
      {"#38B848", "And the game was over and the player woke up from the dream."},
      {"#28C8B8", "And the player began a new dream. And the player dreamed again, dreamed better."},
      {"#F6D365", "You are the player. Wake up."}
   };

   // ================================================================================
   //  End Poem & Music / Sound Credits (triggered by End Exit Portal, /credits, or Options)
   // ================================================================================

   static void credits(Game g) {
      Ui ui = g.ui;
      Gui gui = g.gui;
      ui.begin();
      ui.backdrop(0.75F);
      gui.rect(0.0F, 0.0F, gui.w, gui.h, 0xD8050810);

      float cx = gui.w * 0.5F;
      float pw = Math.min(340.0F, gui.w - 16.0F);
      float px = cx - pw * 0.5F;
      float topY = 26.0F;
      float footY = gui.h - 26.0F;
      float viewH = Math.max(80.0F, footY - topY - 6.0F);

      ui.panel(px, topY - 20.0F, pw, viewH + 22.0F);
      gui.centered("BLOCKCRAFT  -  END POEM & CREDITS", cx, topY - 14.0F, Ui.GOLD, true, 1.0F);

      // Calculate total content height
      int totalLines = END_POEM_LINES.length + 4;
      for (int s = 0; s < SoundEngine.CREDITS_SECTIONS.length; s++) {
         totalLines += 2 + SoundEngine.CREDITS_SECTIONS[s].length;
      }
      float lineStep = 12.0F;
      float contentH = (float)totalLines * lineStep + 32.0F;
      float maxScroll = Math.max(0.0F, contentH - viewH);

      // Gentle auto-scroll
      g.creditsScroll = Math.min(maxScroll, g.creditsScroll + 0.35F);

      float y = topY + 6.0F - g.creditsScroll;
      float textX = px + 10.0F;
      float maxTextW = pw - 20.0F;

      // 1. End Poem
      if (y + 10.0F >= topY && y <= topY + viewH - 10.0F) {
         gui.centered(Game.clipText("=== THE END POEM ===", maxTextW), cx, y, 0xFFFAD666, true, 1.0F);
      }
      y += lineStep + 4.0F;
      for (int i = 0; i < END_POEM_LINES.length; i++) {
         String col = END_POEM_LINES[i][0];
         String line = Game.clipText(END_POEM_LINES[i][1], maxTextW);
         if (y + 10.0F >= topY && y <= topY + viewH - 10.0F) {
            int color = col.equals("#28C8B8") ? 0xFF28C8B8 : (col.equals("#38B848") ? 0xFF38C848 : 0xFFF6D365);
            gui.text(line, textX, y, color, true);
         }
         y += lineStep;
      }
      y += 8.0F;

      // 2. Music, Sound Effects & 3D Acoustics Attributions
      for (int s = 0; s < SoundEngine.CREDITS_SECTIONS.length; s++) {
         String[] sec = SoundEngine.CREDITS_SECTIONS[s];
         String header = Game.clipText(sec[0], maxTextW);
         if (y + 10.0F >= topY && y <= topY + viewH - 10.0F) {
            gui.centered(header, cx, y, 0xFFF5D161, true, 1.0F);
         }
         y += lineStep + 2.0F;
         for (int i = 1; i < sec.length; i++) {
            String line = Game.clipText(sec[i], maxTextW);
            if (y + 10.0F >= topY && y <= topY + viewH - 10.0F) {
               boolean highlight = sec[i].startsWith("Original") || sec[i].startsWith("Curated") || sec[i].startsWith("Freesound") || sec[i].startsWith("CC-BY") || sec[i].startsWith("CC0");
               int color = highlight ? 0xFFB8E6FF : 0xFFD6D6E0;
               gui.text(line, textX, y, color, true);
            }
            y += lineStep;
         }
         y += 8.0F;
      }

      float btnW = Math.min(140.0F, (pw - 24.0F) * 0.5F);
      ui.button("Top", cx - btnW - 4.0F, footY, btnW, 20.0F, true, "Scroll back to the start of the credits.", () -> {
         g.creditsScroll = 0.0F;
      });
      ui.button("Done", cx + 4.0F, footY, btnW, 20.0F, true, null, () -> {
         Game.S ret = g.creditsReturn != null ? g.creditsReturn : (g.world != null ? Game.S.PLAY : Game.S.TITLE);
         g.setScreen(ret);
      });
      ui.drawToast();
   }
}
