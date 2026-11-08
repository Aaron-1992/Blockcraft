package bc;

import java.io.File;
import java.util.ArrayList;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * V6.36: the plain menu UI.
 *
 * <p>The V6.35 "Atlas" look (nav rail, numbered cards, cryptic kickers) was
 * rejected, so every screen here is deliberately ordinary: one centred panel,
 * a plain title, a list of clearly labelled buttons and a Back button. No
 * decorative numbering, no duplicated card titles, no clipped abbreviations and
 * no nonsense footer.</p>
 *
 * <p><b>Every setting shows its actual value as a number</b> - sliders print the
 * number in gold at the right of the row and in the bar label, toggles print
 * ON/OFF, and stepped settings print the current value and the unit. The Debug
 * Advanced tab still calls {@link Screens#advancedTab} so all 61 render,
 * shader and performance settings remain configurable in game.</p>
 *
 * <p>The <b>live world-map preview</b> on Create New World / Create Multiplayer
 * World is kept (the user liked the map, not the old menu around it): it is the
 * same {@link WorldPreview} widget in a plain two-column form.</p>
 *
 * <p>All buttons still go through {@link Ui}, so clicks, keyboard focus,
 * scrolling, hints and toasts behave exactly as before.</p>
 */
final class Menus635 {
   private Menus635() {
   }

   // ------------------------------------------------------------------ palette
   static final int GOLD = 0xFFFFD86A;
   static final int TEXT = 0xFFF0F4FA;
   static final int DIM = 0xFFB4BEC8;
   static final int FAINT = 0xFF8490A4;
   static final int GOOD = 0xFF98E0A8;
   static final int BAD = 0xFFF09090;
   static final int PANEL = 0xE8101520;
   static final int PANEL_LO = 0xEE0A0E16;
   static final int EDGE = 0xFF05080E;
   static final int RULE = 0x60FFFFFF;
   static final int ROW = 0xFF161D2A;

   static final String[] MG_TABS = {"Battle", "Tumble", "Bedwars", "CS2", "Murder", "Ender", "Siege"};
   static final String[] TABS = {"Video", "World", "Controls", "Sound", "Gameplay", "Content"};
   static final String[] TABS_DEBUG = {"Video", "World", "Controls", "Sound", "Gameplay", "Content", "Advanced"};
   static final int ADV_TAB = 6;

   static String[] tabs(Options o) { return o.debugVideo ? TABS_DEBUG : TABS; }

   /** The rectangle the current screen may draw in. */
   static final class Box {
      float x, y, w, h, foot;
   }

   /** One centred panel with a plain title, a rule and a footer. */
   static Box shell(Game g, String title, String sub) {
      Ui ui = g.ui;
      Gui gui = g.gui;
      ui.begin();
      ui.backdrop(0.62F);
      float W = gui.w, H = gui.h;
      float pw = Math.max(120.0F, Math.min(340.0F, W - 12.0F));
      float px = (W - pw) / 2.0F;
      float py = 5.0F;
      float ph = H - 10.0F;

      gui.rect(px - 1.0F, py - 1.0F, pw + 2.0F, ph + 2.0F, EDGE);
      gui.gradient(px, py, pw, ph, PANEL, PANEL_LO);
      gui.rect(px, py, pw, 1.0F, 0x40FFFFFF);
      gui.rect(px, py + ph - 1.0F, pw, 1.0F, 0x40000000);
      gui.centered(Game.clipText(title, pw - 12.0F), W / 2.0F, py + 7.0F, TEXT, true, 1.25F);
      gui.rect(px + 10.0F, py + 24.0F, pw - 20.0F, 1.0F, RULE);
      if (sub != null) {
         gui.centered(Game.clipText(sub, pw - 12.0F), W / 2.0F, py + 28.0F, DIM, true, 1.0F);
      }

      Box b = new Box();
      b.x = px + 8.0F;
      b.y = py + 42.0F;
      b.w = pw - 16.0F;
      b.foot = py + ph - 46.0F;
      b.h = Math.max(20.0F, b.foot - b.y - 24.0F);
      return b;
   }

   static void footer(Game g, Box b) {
      Gui gui = g.gui;
      gui.centered(Game.clipText("Blockcraft V" + Game.VERSION_STR + "  -  original, not Mojang",
         gui.w - 10.0F), gui.w / 2.0F, b.foot + 22.0F, FAINT, true, 1.0F);
   }

   static void hint(Game g, Box b, String fallback) {
      g.ui.drawHint(b.x, b.foot - 22.0F, b.w, 18.0F, fallback);
   }

   /** A plain full-width button; the label is clipped so it always fits. */
   static void button(Game g, String label, float x, float y, float w, float h, String tip, Runnable action) {
      g.ui.button(Game.clipText(label, w - 6.0F), x, y, w, h, true, tip, action);
   }

   static void label(Game g, String s, float x, float y, float w, int color) {
      g.gui.text(Game.clipText(s, w), x, y, color, true);
   }

   static void section(Game g, String s, float x, float y, float w) {
      Gui gui = g.gui;
      gui.text(Game.clipText(s, w), x, y, GOLD, true);
      gui.rect(x, y + 10.0F, w, 1.0F, 0x28FFFFFF);
   }

   /** Bottom action strip: labels are short and obvious. */
   static void actions(Game g, Box b, String[] labels, Runnable[] acts, String[] tips) {
      float gap = 4.0F;
      float bw = (b.w - gap * (labels.length - 1)) / labels.length;
      for (int i = 0; i < labels.length; i++) {
         final int k = i;
         button(g, labels[i], b.x + i * (bw + gap), b.foot, bw, 19.0F,
            tips == null ? null : tips[k], acts[k]);
      }
   }

   // ==================================================================== TITLE
   static void title(Game g) {
      Box b = shell(g, "Blockcraft", "An original voxel sandbox");
      Ui ui = g.ui;
      float y = b.y + 2.0F;
      float step = Math.min(28.0F, Math.max(22.0F, (b.h - 46.0F) / 5.0F));
      button(g, "Singleplayer", b.x, y, b.w, 20.0F, "Play your saved worlds or make a new one.", () -> {
         g.newSettings = new WorldSettings();
         g.setScreen(Game.S.SP_HUB);
      });
      button(g, "Multiplayer", b.x, y + step, b.w, 20.0F, "Join a friend or host your own world.", () -> Screens.openMpHub(g));
      button(g, "Settings", b.x, y + 2.0F * step, b.w, 20.0F, "Video, world, sound and controls. Every value is shown.", () -> {
         g.optionsReturn = Game.S.TITLE;
         g.setScreen(Game.S.OPTIONS);
      });
      float fy = b.foot - 44.0F;
      float fw = (b.w - 8.0F) / 3.0F;
      button(g, "Mini Games", b.x, fy, fw, 19.0F, "Console Edition mini games.", () -> Screens.openMinigames(g, Game.S.TITLE));
      button(g, "Credits", b.x + fw + 4.0F, fy, fw, 19.0F, "Music, art and sound credits.", () -> {
         g.creditsReturn = Game.S.TITLE;
         g.creditsScroll = 0.0F;
         g.setScreen(Game.S.CREDITS);
      });
      button(g, "Quit Game", b.x + 2.0F * (fw + 4.0F), fy, fw, 19.0F, null,
         () -> org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose(g.window, true));
      hint(g, b, "New worlds are 384 blocks deep. Old saves keep their own height.");
      footer(g, b);
      ui.drawToast();
   }

   // ==================================================================== HUBS
   static void singleplayer(Game g) {
      Box b = shell(g, "Singleplayer", "Choose what to play");
      Ui ui = g.ui;
      float y = b.y, step = 26.0F;
      button(g, "Open World", b.x, y, b.w, 20.0F, "Your saved worlds and Create New World.", () -> Screens.openWorlds(g));
      button(g, "Mini Games", b.x, y + step, b.w, 20.0F, "Battle, Tumble, Bedwars and more.", () -> Screens.openMinigames(g, Game.S.SP_HUB));
      button(g, "All Blocks Gallery", b.x, y + 2 * step, b.w, 20.0F, "A world with every block laid out.", () -> {
         g.ensureAllBlocksShowcaseWorld();
         g.worldName = "All_Blocks_Showcase";
         g.startWorld(false, null, 1);
      });
      actions(g, b, new String[]{"Back"}, new Runnable[]{() -> g.setScreen(Game.S.TITLE)}, null);
      hint(g, b, "Create a NEW world to get the 384-block-deep caves.");
      footer(g, b);
      ui.drawToast();
   }

   static void mpHub(Game g) {
      Box b = shell(g, "Multiplayer", "Play with other people");
      Ui ui = g.ui;
      float y = b.y, step = 25.0F;
      button(g, "Open World", b.x, y, b.w, 20.0F, "Server address, LAN, Share Code and hosting.", () -> Screens.openMultiplayer(g));
      button(g, "Mini Games", b.x, y + step, b.w, 20.0F, "Console Edition mini games with friends.", () -> Screens.openMinigames(g, Game.S.MP_HUB));
      button(g, "Create & Host World", b.x, y + 2 * step, b.w, 20.0F, "A named world with a seed and a live map preview.", () -> {
         g.newName = "";
         g.seedField = "";
         g.focusField = 0;
         g.ui.randomSeed = new Random().nextLong();
         g.setScreen(Game.S.MP_CREATE);
      });
      float fy = y + 3 * step + 2.0F;
      label(g, "Server address or Share Code", b.x, fy, b.w, DIM);
      ui.field(b.x, fy + 11.0F, b.w, 17.0F, Net319.serverAddr, "127.0.0.1:25565",
         g.focusField == 0, () -> g.focusField = 0);
      actions(g, b, new String[]{"Join", "Back"}, new Runnable[]{
         () -> Net319.joinFromMenu(g, Net319.serverAddr),
         () -> g.setScreen(Game.S.TITLE)}, new String[]{"Connect to the address above.", null});
      hint(g, b, "Paste a BC-... Share Code or an IP:port and press Join.");
      footer(g, b);
      ui.drawToast();
   }

   // ==================================================================== CREATE A WORLD (map preview kept)
   static void create(Game g, boolean mp) {
      Box b = shell(g, mp ? "Create Multiplayer World" : "Create New World",
         "The map shows the world this seed makes");
      Ui ui = g.ui;
      Gui gui = g.gui;
      WorldSettings ws = g.newSettings;
      float gap = 6.0F;
      float lw = Math.min(158.0F, (b.w - gap) * 0.50F);
      float rw = b.w - lw - gap;
      float lx = b.x, rx = b.x + lw + gap;
      float y = b.y;
      float limit = b.foot - 26.0F;

      section(g, "WORLD", lx, y, lw);
      float fy = y + 14.0F;
      ui.field(lx, fy, lw, 16.0F, g.newName, mp ? "Friends' World" : "New World",
         g.focusField == 0, () -> g.focusField = 0);
      ui.field(lx, fy + 19.0F, lw, 16.0F, g.seedField, "Seed (blank = random)",
         g.focusField == 1, () -> g.focusField = 1);
      if (mp) {
         ui.field(lx, fy + 38.0F, lw, 16.0F, Net319.customNick, "Player name",
            g.focusField == 2, () -> g.focusField = 2);
      }

      float ry = fy + (mp ? 59.0F : 40.0F);
      String[] rows = {"mode", "type", "preset", "depth", "structures", "bonus"};
      for (String row : rows) {
         if (row.equals("preset") && ws.type != WorldSettings.FLAT) continue;
         if (ry + 18.0F > limit) break;
         switch (row) {
            case "mode":
               button(g, "Game Mode: " + Screens.MODE_NAME[g.newMode % Screens.MODE_NAME.length], lx, ry, lw, 17.0F,
                  "Survival, Creative, Adventure, Spectator or Hardcore. Click to change.",
                  () -> g.newMode = (g.newMode + 1) % Screens.MODE_NAME.length);
               break;
            case "type":
               button(g, "World Type: " + ws.typeName(), lx, ry, lw, 17.0F, WorldSettings.TYPE_INFO[ws.type], () -> {
                  ws.type = WorldSettings.TYPE_ORDER[(ws.typeIndex() + 1) % WorldSettings.TYPE_ORDER.length];
                  if (ws.isVoidMode()) g.newMode = 0;
                  else if (ws.type == WorldSettings.ALL_BLOCKS) g.newMode = 1;
               });
               break;
            case "preset":
               button(g, "Flat Preset: " + WorldSettings.FLAT_NAME[ws.flat], lx, ry, lw, 17.0F,
                  "What the flat world is made of.", () -> ws.flat = (ws.flat + 1) % WorldSettings.FLAT_PRESETS);
               break;
            case "depth": {
               String value = g.opt.deepWorld ? "384 blocks" : "256 blocks";
               button(g, "World Depth: " + value, lx, ry, lw, 17.0F,
                  "384 blocks gives ~190 blocks of caves below sea level. 256 is the old world height.",
                  () -> g.opt.deepWorld = !g.opt.deepWorld);
               break;
            }
            case "structures":
               button(g, "Structures: " + (ws.structures ? "ON" : "OFF"), lx, ry, lw, 17.0F,
                  "Villages, temples, ruins and the underground structures.", () -> ws.structures = !ws.structures);
               break;
            default:
               button(g, "Bonus Chest: " + (ws.bonusChest ? "ON" : "OFF"), lx, ry, lw, 17.0F,
                  "A chest with a few starting supplies.", () -> ws.bonusChest = !ws.bonusChest);
               break;
         }
         ry += 19.0F;
      }

      // ---- the world-map preview: kept, in a plain frame
      section(g, "WORLD MAP", rx, y, rw);
      if (ui.preview == null) ui.preview = new WorldPreview(96, 60, 20, true);
      ws.depth = g.opt.deepWorld && g.opt.caveCliffs && ws.terra() ? 128 : 0;
      ui.preview.update(Screens.seedValue(g), ws);
      float ph = Math.max(28.0F, Math.min(80.0F, limit - y - 16.0F));
      gui.rect(rx - 1.0F, y + 13.0F, rw + 2.0F, ph + 2.0F, 0xFF000000);
      gui.image(ui.preview.img, rx, y + 14.0F, rw, ph);
      int[] legend = ui.preview.topBiomes(8);
      for (int i = 0; i < legend.length; i++) {
         float sx = rx + 3.0F + i * 10.0F;
         float sy = y + 14.0F + ph - 10.0F;
         gui.rect(sx, sy, 9.0F, 8.0F, 0xFF000000);
         gui.rect(sx + 1.0F, sy + 1.0F, 7.0F, 6.0F, 0xFF000000 | WorldPreview.color(legend[i]));
      }
      if (g.seedField.trim().isEmpty()) {
         g.hitbox(rx, y + 14.0F, rw, ph, () -> ui.randomSeed = new Random().nextLong());
      }
      if (ui.over(rx, y + 14.0F, rw, ph)) {
         ui.hint = "Biomes in view: " + ui.preview.legendText(8) + " (" + ui.preview.distinctBiomes()
            + " of " + Biomes.N + ").";
      }
      float iy = y + 19.0F + ph;
      if (iy + 10.0F < limit) {
         label(g, "Biomes: " + ui.preview.distinctBiomes() + " of " + Biomes.N, rx, iy, rw, GOLD);
      }
      if (iy + 21.0F < limit) {
         label(g, "Seed: " + Screens.seedValue(g), rx, iy + 11.0F, rw, FAINT);
      }

      String[] labels = mp ? new String[]{"Create & Host", "Cancel"} : new String[]{"Create New World", "Cancel"};
      Runnable[] acts = new Runnable[]{
         mp ? (() -> Screens.createMpWorld(g)) : (() -> Screens.createWorld(g)),
         () -> { g.scanWorlds(); g.setScreen(g.worldList.isEmpty() ? Game.S.TITLE : Game.S.WORLDS); }
      };
      actions(g, b, labels, acts, new String[]{"Generate the world shown in the map.", null});
      hint(g, b, g.seedField.trim().isEmpty()
         ? "Leave the seed blank for a random world. Click the map to reroll it."
         : "The same seed and world type always make the same world.");
      footer(g, b);
      ui.drawToast();
   }

   static void newWorld(Game g) { create(g, false); }
   static void mpCreate(Game g) { create(g, true); }

   // ==================================================================== WORLD LIST
   static void worlds(Game g) {
      Box b = shell(g, "Your Worlds", g.worldList.size() + " saved worlds");
      Ui ui = g.ui;
      Gui gui = g.gui;
      int n = g.worldList.size();
      float listH = Math.max(24.0F, b.foot - b.y - 48.0F);
      section(g, "SAVED WORLDS", b.x, b.y, b.w);
      gui.gradient(b.x, b.y + 13.0F, b.w, listH - 13.0F, PANEL_LO, PANEL_LO);
      if (n == 0) {
         gui.centered("No worlds yet. Create a new one.", b.x + b.w / 2.0F, b.y + 30.0F, DIM, true, 1.0F);
      } else {
         float rowH = 30.0F;
         float content = n * rowH + 4.0F;
         ui.beginScroll(b.x, b.y + 13.0F, b.w, listH - 13.0F, ui.worldScroll, content, rowH);
         int thumbs = 0;
         for (int i = 0; i < n; i++) {
            float ry = b.y + 16.0F + i * rowH - ui.worldScroll[0];
            if (!Ui.visible(ry, rowH - 2.0F, b.y + 13.0F, listH - 13.0F)) continue;
            Game.WInfo wi = g.worldList.get(i);
            if (wi.thumb == null && thumbs < 3) {
               thumbs++;
               wi.thumb = new WorldPreview(24, 24, 16, false);
               wi.thumb.update(wi.seed, wi.settings);
            }
            boolean sel = wi.dir.equals(g.worldName);
            gui.rect(b.x + 2.0F, ry, b.w - 4.0F, rowH - 2.0F, sel ? 0xFF4C5C74 : 0xFF222A38);
            if (sel) gui.rect(b.x + 2.0F, ry, 2.0F, rowH - 2.0F, GOLD);
            if (wi.thumb != null) gui.image(wi.thumb.img, b.x + 6.0F, ry + 3.0F, rowH - 8.0F, rowH - 8.0F);
            float tx = b.x + rowH + 2.0F;
            label(g, wi.name, tx, ry + 4.0F, b.w - rowH - 16.0F, sel ? TEXT : DIM);
            label(g, Screens.MODE_NAME[Math.max(0, Math.min(4, wi.mode))] + "  -  " + wi.settings.typeName()
               + "  -  seed " + wi.seed, tx, ry + 15.0F, b.w - rowH - 16.0F, FAINT);
            final String dir = wi.dir;
            g.hitbox(b.x + 2.0F, ry, b.w - 4.0F, rowH - 2.0F, () -> {
               long now = System.currentTimeMillis();
               if (dir.equals(g.worldName) && now - g.lastWorldClick < 400L) g.startWorld(false, null, 1);
               g.worldName = dir;
               g.confirmDelete = false;
               g.lastWorldClick = now;
            });
         }
         ui.endScroll(b.x, b.y + 13.0F, b.w, listH - 13.0F, content);
      }
      actions(g, b, new String[]{"Play World", "New World", "Delete", "Back"}, new Runnable[]{
         () -> g.startWorld(false, null, 1),
         () -> Screens.openNewWorld(g),
         () -> {
            if (!g.confirmDelete) g.confirmDelete = true;
            else {
               Game.deleteDir(new File(g.root, "saves/" + g.worldName));
               g.confirmDelete = false;
               g.scanWorlds();
               if (g.worldList.isEmpty()) g.setScreen(Game.S.TITLE);
            }
         },
         () -> g.setScreen(Game.S.TITLE)
      }, new String[]{"Open the selected world (double-click also works).", "Create New World.",
         g.confirmDelete ? "Click again to permanently delete." : "Click twice to delete.", null});
      hint(g, b, g.confirmDelete ? "Click Delete again to permanently remove the world." : "Select a world, then Play World.");
      footer(g, b);
      ui.drawToast();
   }

   // ==================================================================== MULTIPLAYER
   static void multiplayer(Game g) {
      Box b = shell(g, "Multiplayer", Net319.statusText);
      Ui ui = g.ui;
      Gui gui = g.gui;
      float cw = (b.w - 6.0F) / 2.0F;
      float x1 = b.x + cw + 6.0F;
      label(g, "Server address or Share Code", b.x, b.y, b.w, DIM);
      ui.field(b.x, b.y + 11.0F, b.w, 17.0F, Net319.serverAddr, "127.0.0.1:25565",
         g.focusField == 0, () -> g.focusField = 0);
      String state = Net319.isHosting()
         ? "Hosting " + WorldSettings.TYPE_NAME[Net319.mpWorldType] + " | Code: " + Net319.shareCode
         : (Net319.isConnected() ? "Connected (" + Net319.peerCount() + " online)" : Net319.statusText);
      label(g, state, b.x, b.y + 31.0F, b.w, Net319.isActive() ? GOOD : FAINT);
      float y = b.y + 44.0F;
      button(g, "Join Server", b.x, y, cw, 18.0F, "Connect to the address above.", () -> Net319.joinFromMenu(g, Net319.serverAddr));
      button(g, "Join LAN", x1, y, cw, 18.0F, "Find a world on your network.", () -> Net319.joinFromMenu(g, "lan"));
      button(g, "Host World", b.x, y + 21.0F, cw, 18.0F, "Host a world and make a Share Code.", () -> Net319.hostFromMenu(g));
      button(g, "Start Server.bat", x1, y + 21.0F, cw, 18.0F, "Launch the standalone server.", () -> Net319.launchServerAndJoin(g));
      button(g, "World Type: " + WorldSettings.TYPE_NAME[Net319.mpWorldType], b.x, y + 42.0F, cw, 18.0F,
         WorldSettings.TYPE_INFO[Net319.mpWorldType], Net319::cycleMpWorldType);
      button(g, "Game Mode: " + Net319.mpModeName(), x1, y + 42.0F, cw, 18.0F,
         "Game mode for hosted worlds.", Net319::cycleMpGameMode);
      button(g, Net319.mpWorldwide ? "Access: Worldwide" : "Access: LAN only", b.x, y + 63.0F, cw, 18.0F,
         "Worldwide opens UPnP and the public tunnel.", Net319::toggleMpWorldwide);
      button(g, "Copy Share Code", x1, y + 63.0F, cw, 18.0F, "Copy a code for friends.", () -> Net319.copyShareCode(g));
      if (y + 84.0F + 18.0F < b.foot - 24.0F) {
         button(g, "Disconnect", b.x, y + 84.0F, cw, 18.0F, null, () -> {
            Net319.disconnect(g);
            Net319.statusText = "Disconnected.";
         });
         button(g, "Nickname: " + Net319.displayNick(), x1, y + 84.0F, cw, 18.0F, null, Net319::cyclePresetNick);
      }
      actions(g, b, new String[]{"Back"}, new Runnable[]{() -> g.setScreen(g.mpReturn != null ? g.mpReturn : Game.S.TITLE)}, null);
      hint(g, b, "Use Create & Host World for a named world with a live map preview.");
      footer(g, b);
      ui.drawToast();
   }

   // ==================================================================== MINI GAMES
   static void minigames(Game g) {
      Box b = shell(g, "Mini Games", "Console Edition modes");
      Ui ui = g.ui;
      Gui gui = g.gui;
      WorldSettings ws = g.newSettings;
      if (!ws.isMinigame()) ws.type = WorldSettings.MINIGAME_ORDER[Math.max(0, Math.min(WorldSettings.MINIGAME_ORDER.length - 1, g.mgTab))];
      int cols = 3;
      int tabRows = (MG_TABS.length + cols - 1) / cols;
      float tw = b.w / cols;
      for (int i = 0; i < MG_TABS.length; i++) {
         float tx = b.x + (i % cols) * tw;
         float ty = b.y + (i / cols) * 19.0F;
         final int idx = i;
         button(g, (i == g.mgTab ? "> " : "") + MG_TABS[i], tx + 1.0F, ty, tw - 2.0F, 16.0F,
            "Switch to " + MG_TABS[idx] + ".", () -> {
               g.mgTab = idx;
               ws.type = WorldSettings.MINIGAME_ORDER[idx];
               g.newMode = 0;
            });
      }
      float y = b.y + tabRows * 19.0F + 2.0F;
      float cw = Math.min(180.0F, b.w * 0.55F);
      float mw = (cw - 4.0F) / 2.0F;
      section(g, "ARENA MAPS  -  " + ws.typeName(), b.x, y, cw);
      for (int m = 0; m < WorldSettings.MAP_COUNT; m++) {
         final int mi = m;
         float mx = b.x + (m % 2) * (mw + 4.0F);
         float my = y + 13.0F + (m / 2) * 17.0F;
         if (my + 15.0F > b.foot - 24.0F) break;
         boolean sel = ws.mapId == m;
         if (sel) gui.rect(mx - 1.0F, my - 1.0F, mw + 2.0F, 17.0F, GOLD);
         button(g, (sel ? "> " : "") + WorldSettings.mapNameFor(ws.type, m), mx, my, mw, 15.0F,
            WorldSettings.mapInfoFor(ws.type, m), () -> {
               ws.mapId = mi;
               ui.say("Arena: " + WorldSettings.mapNameFor(ws.type, mi));
            });
      }
      float rx = b.x + cw + 8.0F;
      float rw = b.x + b.w - rx;
      if (rw > 30.0F) {
         section(g, "SELECTED ARENA", rx, y, rw);
         if (ui.preview == null) ui.preview = new WorldPreview(64, 48, 16, false);
         ui.preview.update(Screens.seedValue(g), ws);
         float ph = Math.min(52.0F, Math.max(22.0F, b.foot - y - 80.0F));
         gui.rect(rx - 1.0F, y + 12.0F, rw + 2.0F, ph + 2.0F, 0xFF000000);
         gui.image(ui.preview.img, rx, y + 13.0F, rw, ph);
         label(g, ws.mapName() + "  -  " + ws.mapTheme(), rx, y + 18.0F + ph, rw, GOLD);
      }
      actions(g, b, new String[]{"Play Now", "Open Lobby", "Back"}, new Runnable[]{
         () -> { g.newMode = 0; g.startWorld(true, Long.toString(ui.randomSeed), 0); SkyGen.startMatch(g, false); },
         () -> { g.newMode = 0; SkyGen.matchState = SkyGen.STATE_WAITING; g.startWorld(true, Long.toString(ui.randomSeed), 0); },
         () -> g.setScreen(g.mgReturn != null ? g.mgReturn : Game.S.TITLE)
      }, new String[]{"Start a match on the selected arena.", "Open the waiting lobby.", null});
      hint(g, b, "Pick an arena on the left, then Play Now.");
      footer(g, b);
      ui.drawToast();
   }

   // ==================================================================== SETTINGS
   /** A cursor for the settings list; rows outside the scroll area are skipped. */
   static final class Rows {
      final Game g;
      final Ui ui;
      final float x, w, top, bottom;
      float y;

      Rows(Game g, float x, float y, float w, float top, float bottom) {
         this.g = g; this.ui = g.ui; this.x = x; this.y = y; this.w = w;
         this.top = top; this.bottom = bottom;
      }

      boolean visible(float h) { return y + h > top && y < bottom; }
   }

   /** A plain ON/OFF button that always spells out its value. */
   static void toggleRow(Rows r, String name, boolean on, String tip, Runnable action) {
      if (r.visible(19.0F)) {
         button(r.g, name + ": " + (on ? "ON" : "OFF"), r.x, r.y, r.w, 17.0F, tip, action);
      }
      r.y += 20.0F;
   }

   /** A stepped numeric setting. The current value AND its unit are in the label. */
   static void numberRow(Rows r, String name, int value, int lo, int hi, int step,
      String unit, String tip, Consumer<Integer> set) {
      if (r.visible(19.0F)) {
         String text = name + ": " + value + (unit.isEmpty() ? "" : " " + unit);
         final int[] cur = {value};
         button(r.g, text, r.x, r.y, r.w, 17.0F, tip + " Click to change.", () -> {
            cur[0] = cur[0] + step > hi ? lo : cur[0] + step;
            set.accept(cur[0]);
         });
      }
      r.y += 20.0F;
   }

   /** A named choice. The current option is printed in full, never abbreviated. */
   static void choiceRow(Rows r, String name, String[] names, int idx, String tip, Consumer<Integer> set) {
      if (r.visible(19.0F)) {
         final int[] cur = {Math.max(0, Math.min(names.length - 1, idx))};
         button(r.g, name + ": " + names[cur[0]], r.x, r.y, r.w, 17.0F, tip + " Click to change.", () -> {
            cur[0] = (cur[0] + 1) % names.length;
            set.accept(cur[0]);
         });
      }
      r.y += 20.0F;
   }

   /** A slider that PRINTS its number: gold value at the right and in the bar label. */
   static void sliderRow(Rows r, String name, float value, float lo, float hi,
      String unit, boolean integer, String tip, Consumer<Float> set) {
      if (r.visible(30.0F)) {
         String num = integer ? Integer.toString(Math.round(value)) : String.format("%.2f", value);
         String valueText = num + (unit.isEmpty() ? "" : " " + unit);
         label(r.g, name, r.x, r.y, r.w - 86.0F, TEXT);
         String clipped = Game.clipText(valueText, 82.0F);
         r.g.gui.text(clipped, r.x + r.w - Gui.textWidth(clipped), r.y, GOLD, true);
         r.ui.slider(Game.clipText(valueText, r.w - 6.0F), r.x, r.y + 11.0F, r.w, 14.0F,
            Math.max(0.0F, Math.min(1.0F, (value - lo) / (hi - lo))), tip, f -> set.accept(lo + f * (hi - lo)));
      }
      r.y += 30.0F;
   }

   static void options(Game g) {
      Ui ui = g.ui;
      Gui gui = g.gui;
      Options o = g.opt;
      String[] names = tabs(o);
      int tab = Math.max(0, Math.min(names.length - 1, ui.optTab));
      Box b = shell(g, "Settings", "Every setting shows its current value");

      int cols = 4;
      int tabRows = (names.length + cols - 1) / cols;
      float tw = b.w / cols;
      for (int i = 0; i < names.length; i++) {
         float tx = b.x + (i % cols) * tw;
         float ty = b.y + (i / cols) * 19.0F;
         final int idx = i;
         button(g, (i == tab ? "> " : "") + names[i], tx + 1.0F, ty, tw - 2.0F, 16.0F,
            "Open the " + names[i] + " settings.", () -> ui.optTab = idx);
      }
      float rowsY = b.y + tabRows * 19.0F + 2.0F;
      float rowsH = Math.max(20.0F, b.foot - rowsY - 26.0F);
      gui.gradient(b.x, rowsY, b.w, rowsH, PANEL_LO, PANEL_LO);
      gui.rect(b.x, rowsY, b.w, 1.0F, RULE);

      if (tab == ADV_TAB) {
         // Debug Advanced: every render/shader/performance switch, with its value.
         float colW = Math.min(150.0F, (b.w - 10.0F) / 2.0F);
         ui.beginScroll(b.x, rowsY, b.w, rowsH, ui.optScroll, 62.0F * 20.0F, 20.0F);
         int before = g.btns.size();
         Screens.advancedTab(g, o, b.x + 3.0F, b.x + colW + 7.0F, colW, rowsY - ui.optScroll[0], 20.0F,
            rowsY, rowsH);
         for (int i = before; i < g.btns.size(); i++) {
            Game.Btn btn = g.btns.get(i);
            if (btn.label != null) btn.label = Game.clipText(btn.label, btn.w - 6.0F);
         }
         ui.endScroll(b.x, rowsY, b.w, rowsH, 62.0F * 20.0F);
      } else {
         ui.beginScroll(b.x, rowsY, b.w, rowsH, ui.optScroll, 26.0F * 24.0F, 24.0F);
         Rows r = new Rows(g, b.x + 4.0F, rowsY + 2.0F - ui.optScroll[0], b.w - 10.0F, rowsY, rowsY + rowsH);
         fillSettings(g, r, tab);
         ui.endScroll(b.x, rowsY, b.w, rowsH, 26.0F * 24.0F);
      }

      actions(g, b, new String[]{"Reset to defaults", "Back"}, new Runnable[]{
         () -> { Screens.resetAdvanced(o); ui.say("All settings reset to their defaults."); },
         () -> g.setScreen(g.optionsReturn != null ? g.optionsReturn : Game.S.TITLE)
      }, new String[]{"Reset EVERY setting (video, world, sound, controls) to its default.", null});
      hint(g, b, "Turn on Debug Video to unlock the Advanced tab with all 61 video and performance controls.");
      footer(g, b);
      ui.drawToast();
   }

   static void fillSettings(Game g, Rows r, int tab) {
      Ui ui = g.ui;
      Options o = g.opt;
      switch (tab) {
         case 0: // Video
            numberRow(r, "Render Distance", o.renderDistance, 2, 32, 1, "chunks", "How many chunks are drawn around you.", v -> o.renderDistance = v);
            sliderRow(r, "Field of View", o.fov, 50, 110, "degrees", true, "Camera field of view in degrees.", v -> o.fov = Math.round(v));
            toggleRow(r, "VSync", o.vsync, "Sync frames to the monitor refresh rate.", () -> o.vsync = !o.vsync);
            toggleRow(r, "Fullscreen", o.fullscreen, "Use the whole display.", () -> { o.fullscreen = !o.fullscreen; g.applyDisplayMode(); });
            toggleRow(r, "Borderless Fullscreen", o.borderless, "Fullscreen without a window border.", () -> { o.borderless = !o.borderless; g.applyDisplayMode(); });
            numberRow(r, "Max FPS", o.maxFps, 0, 240, 30, "fps (0 = unlimited)", "Frame rate cap. 0 means no limit.", v -> o.maxFps = v);
            numberRow(r, "GUI Scale", o.guiScale, 0, 3, 1, "(0 = automatic)", "Menu size. 0 picks a size for your window.", v -> o.guiScale = v);
            sliderRow(r, "Brightness", o.gamma, -0.5F, 0.5F, "", false, "Extra brightness.", v -> o.gamma = v);
            toggleRow(r, "Smooth Lighting", o.smooth, "Softer light on block faces.", () -> o.smooth = !o.smooth);
            toggleRow(r, "Shaders", o.shaders, "The built-in shader pack.", () -> o.shaders = !o.shaders);
            toggleRow(r, "Clouds", o.clouds, "Draw clouds in the sky.", () -> o.clouds = !o.clouds);
            choiceRow(r, "Leaves Quality", new String[]{"Fast", "Fancy"}, Math.max(0, Math.min(1, o.leaves)),
               "Leaf rendering quality.", v -> o.leaves = v);
            choiceRow(r, "Bloom", Screens.BLOOM_NAME, Math.max(0, Math.min(2, o.bloom)),
               "Bloom strength.", v -> o.bloom = v);
            toggleRow(r, "Debug Video", o.debugVideo, "Unlock the Advanced tab with every video setting.", () -> o.debugVideo = !o.debugVideo);
            break;
         case 1: // World
            toggleRow(r, "3D Cave Volumes", o.caveCliffs, "Minecraft-style cave volumes with natural entrances.", () -> o.caveCliffs = !o.caveCliffs);
            choiceRow(r, "World Depth", new String[]{"256 blocks (classic)", "384 blocks (deep)"}, o.deepWorld ? 1 : 0,
               "Deep worlds have ~190 blocks of caves below sea level.", v -> o.deepWorld = v == 1);
            toggleRow(r, "Cave Structures", o.caveStructures, "Mineshafts, dungeons, geodes and more inside the cave air.", () -> o.caveStructures = !o.caveStructures);
            toggleRow(r, "Smooth Biome Borders", o.biomeBorders > 0, "Gradual transitions between biomes.", () -> o.biomeBorders = o.biomeBorders > 0 ? 0 : 1);
            sliderRow(r, "Cave Scale", o.caveScale, 0.6F, 3.0F, "x", false, "Size of cave shapes.", v -> o.caveScale = v);
            sliderRow(r, "Cave Density", o.caveDensity, 0.4F, 2.5F, "x", false, "How much of the deep world is carved.", v -> o.caveDensity = v);
            sliderRow(r, "Tree Density", o.treeDensity, 0.4F, 1.4F, "x", false, "Number of trees. Lower is faster in taiga.", v -> o.treeDensity = v);
            sliderRow(r, "Tree Size", o.treeScale, 0.7F, 1.5F, "x", false, "Height and width of every tree.", v -> o.treeScale = v);
            toggleRow(r, "Giant Trees", o.giantTrees, "Allow the rare Giant tree size.", () -> o.giantTrees = !o.giantTrees);
            toggleRow(r, "Weather", o.weather, "Rain, snow and thunder.", () -> o.weather = !o.weather);
            break;
         case 2: // Controls
            for (int i = 0; i < Screens.KEYS.length; i++) {
               if (!r.visible(16.0F)) { r.y += 16.0F; continue; }
               label(r.g, Screens.KEYS[i][0], r.x, r.y, r.w * 0.42F, GOLD);
               label(r.g, Screens.KEYS[i][1], r.x + r.w * 0.45F, r.y, r.w * 0.55F, DIM);
               r.y += 16.0F;
            }
            break;
         case 3: // Sound
            sliderRow(r, "Sound Volume", o.soundVolume, 0.0F, 1.0F, "", false, "Master sound volume.", v -> o.soundVolume = v);
            sliderRow(r, "Music Volume", o.musicVolume, 0.0F, 1.0F, "", false, "Music volume.", v -> o.musicVolume = v);
            toggleRow(r, "3D Sound", o.realAcoustics, "Positional cave and world sound.", () -> o.realAcoustics = !o.realAcoustics);
            toggleRow(r, "Always Play Music", o.constantMusic, "Never stop the music.", () -> o.constantMusic = !o.constantMusic);
            break;
         case 4: // Gameplay
            toggleRow(r, "Invert Mouse", o.invertY, "Flip the vertical look direction.", () -> o.invertY = !o.invertY);
            sliderRow(r, "Mouse Sensitivity", o.sensitivity, 0.1F, 1.5F, "x", false, "Mouse speed.", v -> o.sensitivity = v);
            toggleRow(r, "View Bobbing", o.bobbing, "Camera movement while walking.", () -> o.bobbing = !o.bobbing);
            toggleRow(r, "Minimap", o.minimap, "Show the minimap in the corner.", () -> o.minimap = !o.minimap);
            choiceRow(r, "Minimap Corner", new String[]{"Top right", "Top left", "Bottom right", "Bottom left"},
               Math.max(0, Math.min(3, o.minimapPos)), "Which corner holds the minimap.", v -> o.minimapPos = v);
            toggleRow(r, "Move Minimap For Pop-ups", o.minimapAutoMove,
               "Move the minimap when an achievement appears.", () -> o.minimapAutoMove = !o.minimapAutoMove);
            toggleRow(r, "Disasters", o.disasters, "Enable world disasters.", () -> o.disasters = !o.disasters);
            break;
         default: // Content
            if (r.visible(19.0F)) {
               button(g, "Skins", r.x, r.y, r.w, 17.0F, "Choose or import a player skin.", () -> {
                  g.skinReturn = Game.S.OPTIONS; g.setScreen(Game.S.SKINS);
               });
            }
            r.y += 20.0F;
            if (r.visible(19.0F)) {
               button(g, "Resource Packs", r.x, r.y, r.w, 17.0F, "Reload PNGs and installed packs.", () -> {
                  g.packsReturn = Game.S.OPTIONS; g.setScreen(Game.S.PACKS);
               });
            }
            r.y += 20.0F;
            if (r.visible(19.0F)) {
               button(g, "Mods", r.x, r.y, r.w, 17.0F, "Enable or disable installed mods.", () -> {
                  g.modsReturn = Game.S.OPTIONS; g.setScreen(Game.S.MODS);
               });
            }
            r.y += 20.0F;
            if (r.visible(19.0F)) {
               button(g, "Credits", r.x, r.y, r.w, 17.0F, "Music, art and sound credits.", () -> {
                  g.creditsReturn = Game.S.OPTIONS; g.setScreen(Game.S.CREDITS);
               });
            }
            r.y += 20.0F;
            if (r.visible(19.0F)) {
               button(g, "Watch Trailer (1:00)", r.x, r.y, r.w, 17.0F, "The in-game montage.", () -> Trailer.start(g));
            }
            r.y += 20.0F;
            break;
      }
   }

   // ==================================================================== PAUSE / MORE / UTILITY
   static void pause(Game g) {
      Box b = shell(g, "Game Menu", g.worldDisplayName);
      Ui ui = g.ui;
      float cw = (b.w - 6.0F) / 2.0F;
      float y = b.y;
      button(g, "Back to Game", b.x, y, b.w, 19.0F, null, () -> g.setScreen(Game.S.PLAY));
      button(g, "Advancements", b.x, y + 22.0F, cw, 18.0F, null, () -> g.setScreen(Game.S.ADV));
      button(g, "Options", b.x + cw + 6.0F, y + 22.0F, cw, 18.0F, null, () -> {
         g.optionsReturn = Game.S.PAUSE; g.setScreen(Game.S.OPTIONS);
      });
      button(g, "Skins", b.x, y + 43.0F, cw, 18.0F, null, () -> {
         g.skinReturn = Game.S.PAUSE; g.setScreen(Game.S.SKINS);
      });
      button(g, "Packs", b.x + cw + 6.0F, y + 43.0F, cw, 18.0F, null, () -> {
         g.packsReturn = Game.S.PAUSE; g.setScreen(Game.S.PACKS);
      });
      button(g, "Mods", b.x, y + 64.0F, cw, 18.0F, null, () -> {
         g.modsReturn = Game.S.PAUSE; g.setScreen(Game.S.MODS);
      });
      button(g, "Save and Quit to Title", b.x + cw + 6.0F, y + 64.0F, cw, 18.0F, null, g::quitToTitle);
      button(g, Net319.hostLabel(), b.x, y + 85.0F, cw, 18.0F, null, () -> Net319.toggleHost(g));
      button(g, Net319.joinLabel(), b.x + cw + 6.0F, y + 85.0F, cw, 18.0F, null, () -> Net319.toggleJoin(g));
      actions(g, b, new String[]{"Back"}, new Runnable[]{() -> g.setScreen(Game.S.PLAY)}, null);
      hint(g, b, "Escape closes this menu.");
      footer(g, b);
      ui.drawToast();
   }

   static void more(Game g) {
      Box b = shell(g, "More", "Everything else");
      Ui ui = g.ui;
      String[] labels = {"Skins", "Resource Packs", "Mods", "All Blocks Gallery", "Credits", "Watch Trailer (1:00)"};
      Runnable[] acts = {
         () -> { g.skinReturn = Game.S.MORE; g.setScreen(Game.S.SKINS); },
         () -> { g.packsReturn = Game.S.MORE; g.setScreen(Game.S.PACKS); },
         () -> { g.modsReturn = Game.S.MORE; g.setScreen(Game.S.MODS); },
         () -> { g.ensureAllBlocksShowcaseWorld(); g.worldName = "All_Blocks_Showcase"; g.startWorld(false, null, 1); },
         () -> { g.creditsReturn = Game.S.MORE; g.creditsScroll = 0.0F; g.setScreen(Game.S.CREDITS); },
         () -> Trailer.start(g)
      };
      for (int i = 0; i < labels.length; i++) {
         button(g, labels[i], b.x, b.y + i * 22.0F, b.w, 18.0F, null, acts[i]);
      }
      actions(g, b, new String[]{"Back"}, new Runnable[]{() -> g.setScreen(Game.S.TITLE)}, null);
      hint(g, b, "The Debug Advanced tab in Settings has every video and performance control.");
      footer(g, b);
      ui.drawToast();
   }

   static void loading(Game g) {
      Box b = shell(g, "Loading World", g.worldDisplayName);
      Gui gui = g.gui;
      section(g, "PROGRESS", b.x, b.y, b.w);
      gui.rect(b.x, b.y + 15.0F, b.w, 10.0F, EDGE);
      gui.rect(b.x + 1.0F, b.y + 16.0F, Math.max(0.0F, (b.w - 2.0F) * g.loadFrac), 8.0F, GOLD);
      label(g, g.loadingMsg, b.x, b.y + 29.0F, b.w, DIM);
      hint(g, b, "Generating a 384-block-deep world. Large caves take a moment on the first visit.");
      footer(g, b);
   }

   static void waiting(Game g) {
      Box b = shell(g, "Waiting Lobby", "Invite friends before the match starts");
      Ui ui = g.ui;
      section(g, "LOBBY", b.x, b.y, b.w);
      label(g, "Share Code: " + Net319.shareCode, b.x, b.y + 15.0F, b.w, GOLD);
      button(g, "Start Match", b.x, b.y + 32.0F, Math.min(160.0F, b.w), 18.0F, "Start the match now.", () -> SkyGen.startMatch(g, false));
      button(g, "Leave Lobby", b.x, b.y + 54.0F, Math.min(160.0F, b.w), 18.0F, null, g::quitToTitle);
      hint(g, b, "Players on LAN or with the Share Code appear here.");
      footer(g, b);
      ui.drawToast();
   }

   static void resourcePacks(Game g) {
      Box b = shell(g, "Resource Packs", "PNG overrides and the editable tilesheet");
      Ui ui = g.ui;
      button(g, "Reload PNGs", b.x, b.y, Math.min(180.0F, b.w), 18.0F, "Re-read the loose PNG overrides.", () -> ResourcePacks.reload(g));
      button(g, "Reload Tilesheet", b.x, b.y + 22.0F, Math.min(180.0F, b.w), 18.0F, "Apply edited tilesheet cells.", () -> {
         Tilesheet.invalidate();
         ui.say("Tilesheet: " + Tilesheet.changedTiles() + " edited tiles active");
      });
      actions(g, b, new String[]{"Back"}, new Runnable[]{
         () -> g.setScreen(g.packsReturn != null ? g.packsReturn : Game.S.MORE)}, null);
      hint(g, b, "PNG overrides work with every pack kind, including Vibrant and Retro.");
      footer(g, b);
      ui.drawToast();
   }

   static void mods(Game g) {
      Box b = shell(g, "Mods", "Installed mods");
      Ui ui = g.ui;
      java.util.List<ModLoader.ModEntry> mods = ModLoader.allMods();
      float y = b.y;
      for (int i = 0; i < Math.min(6, mods.size()); i++) {
         if (y + 18.0F > b.foot - 24.0F) break;
         ModLoader.ModEntry m = mods.get(i);
         final String id = m.id;
         button(g, m.name + ": " + (m.enabled ? "ON" : "OFF"), b.x, y, b.w, 17.0F, "Enable or disable this mod.", () -> {
            ModLoader.toggleEnabled(id);
            ui.say("Mod " + id + " toggled");
         });
         y += 20.0F;
      }
      actions(g, b, new String[]{"Back"}, new Runnable[]{
         () -> g.setScreen(g.modsReturn != null ? g.modsReturn : Game.S.MORE)}, null);
      hint(g, b, "Mods are loaded from the mods folder.");
      footer(g, b);
      ui.drawToast();
   }

   static void credits(Game g) {
      Box b = shell(g, "Credits", "Original code, art, music and CC0 field recordings");
      Ui ui = g.ui;
      ui.paragraph("All world generation, textures, sounds, UI and shaders in Blockcraft are original. "
         + "The ten real CC0 WAV field recordings are credited in res/sounds/foley/CREDITS.txt. "
         + "This is NOT Minecraft and contains no Mojang code or assets.",
         b.x, b.y, b.w, DIM, 6);
      actions(g, b, new String[]{"Back"}, new Runnable[]{
         () -> g.setScreen(g.creditsReturn != null ? g.creditsReturn : Game.S.MORE)}, null);
      hint(g, b, "V6.36: plain menus, every setting shows its value.");
      footer(g, b);
      ui.drawToast();
   }
}
