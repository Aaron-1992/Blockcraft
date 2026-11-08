package bc;

import java.io.File;
import java.util.ArrayList;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * V6.35: every menu rebuilt from scratch as the "Atlas" interface.
 *
 * <p>None of the V6.33/V6.34 screens are drawn any more: {@link Game} now routes
 * all menu states here. The look is a dark left navigation rail, a numbered
 * header with a cyan rule, large cards with coloured top strips, and a common
 * footer with the hint and the version. The one thing deliberately carried over
 * is the <b>live world-map preview</b> on Create New World / Create Multiplayer
 * World - the user asked to keep the map, not the old menu around it. It is the
 * same {@link WorldPreview} widget, redrawn in a new frame.</p>
 *
 * <p>All buttons still go through {@link Ui}, so clicks, keyboard focus,
 * scrolling, hints and toasts behave exactly as before. The old {@link Screens}
 * methods remain as a compatibility/regression surface and are no longer drawn.</p>
 */
final class Menus635 {
   private Menus635() {
   }

   // ------------------------------------------------------------------ palette
   static final int CYAN = 0xFF54D8E8;
   static final int GOLD = 0xFFFFD35A;
   static final int TEXT = 0xFFEFF5FF;
   static final int DIM = 0xFFA4B1C8;
   static final int FAINT = 0xFF6E7B93;
   static final int GOOD = 0xFF8CE0A0;
   static final int CARD = 0xEA121A28;
   static final int CARD_LO = 0xEA080C14;
   static final int EDGE = 0xFF05080E;
   static final int RULE = 0x704FD8E8;

   static final String[] NAV = {"Play", "Worlds", "Online", "Games", "Settings", "More"};
   static final String[] NAV_SUB = {"01", "02", "03", "04", "05", "06"};

   /** The rectangle the current screen may draw in. */
   static final class Box {
      float x, y, w, h, foot;
   }

   static float railW(float W) {
      return Math.max(88.0F, Math.min(112.0F, W * 0.24F));
   }

   /** The shared shell: dim the world, draw the rail, numbered header and footer. */
   static Box frame(Game g, String kicker, String heading, String sub, int nav) {
      Ui ui = g.ui;
      Gui gui = g.gui;
      ui.begin();
      ui.backdrop(0.62F);
      float W = gui.w, H = gui.h;
      float rw = railW(W);

      // ---- left navigation rail
      gui.gradient(0.0F, 0.0F, rw, H, 0xF4141D2C, 0xF0060910);
      gui.rect(rw - 1.0F, 0.0F, 1.0F, H, 0x30FFFFFF);
      gui.rect(0.0F, 0.0F, 2.0F, H, CYAN);
      gui.rect(0.0F, 0.0F, rw, 22.0F, 0xFF1A2536);
      gui.rect(0.0F, 21.0F, rw, 1.0F, RULE);
      gui.gradient(rw, 0.0F, W - rw, 50.0F, 0xC0080D16, 0x50080D16);
      gui.text(Game.clipText("BLOCKCRAFT", rw - 8.0F), 6.0F, 7.0F, TEXT, true, 1.0F);
      gui.text(Game.clipText("V" + Game.VERSION_STR, rw - 8.0F), 6.0F, 28.0F, CYAN, true, 1.0F);
      gui.text(Game.clipText("ATLAS UI", rw - 8.0F), 6.0F, 39.0F, FAINT, true, 1.0F);

      float ny = Math.max(52.0F, H * 0.22F);
      float nh = Math.min(22.0F, (H - ny - 18.0F) / 6.0F);
      for (int i = 0; i < NAV.length; i++) {
         float y = ny + i * (nh + 2.0F);
         boolean on = i == nav;
         if (on) {
            gui.rect(0.0F, y, 3.0F, nh, CYAN);
            gui.rect(3.0F, y, rw - 4.0F, nh, 0xA0203044);
         }
         gui.text(Game.clipText(NAV_SUB[i] + "  " + NAV[i], rw - 10.0F), 6.0F,
            y + (nh - 8.0F) / 2.0F, on ? TEXT : DIM, false, 1.0F);
         if (on) gui.rect(6.0F, y + nh - 2.0F, rw - 12.0F, 1.0F, CYAN);
      }

      // ---- header
      float cx = rw + 10.0F;
      float cw = W - rw - 20.0F;
      gui.text(Game.clipText(kicker.toUpperCase(), cw), cx, 7.0F, CYAN, true, 1.0F);
      gui.text(Game.clipText(heading, cw), cx, 19.0F, TEXT, true, 1.3F);
      gui.rect(cx, 35.0F, cw, 1.0F, RULE);
      gui.rect(cx, 35.0F, Math.min(48.0F, cw), 2.0F, CYAN);
      if (sub != null) {
         gui.text(Game.clipText(sub, cw), cx, 40.0F, DIM, false, 1.0F);
      }

      // ---- footer
      gui.rect(rw, H - 26.0F, W - rw, 26.0F, 0xE0070A11);
      gui.rect(rw, H - 26.0F, W - rw, 1.0F, 0x30FFFFFF);
      gui.text(Game.clipText("384-BLOCK WORLDS  /  ESC = BACK", Math.max(40.0F, W - rw - 60.0F)),
         rw + 6.0F, H - 18.0F, FAINT, false, 1.0F);
      String ver = "V" + Game.VERSION_STR;
      gui.text(ver, Math.max(rw + 4.0F, W - Gui.textWidth(ver) - 6.0F), H - 18.0F, CYAN, false, 1.0F);

      Box b = new Box();
      b.x = cx;
      b.y = 52.0F;
      b.w = cw;
      b.h = Math.max(20.0F, H - 52.0F - 32.0F);
      b.foot = H - 26.0F;
      railActions(g, nav);
      return b;
   }

   /** A clickable navigation rail item drawn as a real button (no visible label clash). */
   static void railActions(Game g, int nav) {
      Ui ui = g.ui;
      float W = g.gui.w, H = g.gui.h;
      float rw = railW(W);
      float ny = Math.max(52.0F, H * 0.22F);
      float nh = Math.min(22.0F, (H - ny - 18.0F) / 6.0F);
      for (int i = 0; i < NAV.length; i++) {
         if (i == nav) continue;
         float y = ny + i * (nh + 2.0F);
         final int idx = i;
         // hitbox only: the labels are already drawn by frame(), so no extra widget label
         g.hitbox(2.0F, y, rw - 4.0F, nh, () -> go(g, idx));
      }
   }

   static void go(Game g, int nav) {
      switch (nav) {
         case 0: g.setScreen(Game.S.TITLE); break;
         case 1: Screens.openWorlds(g); break;
         case 2: Screens.openMpHub(g); break;
         case 3: Screens.openMinigames(g, Game.S.TITLE); break;
         case 4: g.optionsReturn = Game.S.TITLE; g.setScreen(Game.S.OPTIONS); break;
         default: g.setScreen(Game.S.MORE); break;
      }
   }

   /** A toggle whose label always fits (Ui.toggle does not clip long names). */
   static void toggle(Game g, String name, boolean on, float x, float y, float w, float h, String hint, Runnable action) {
      String label = name + ": " + (on ? "\u00a7aON" : "\u00a7cOFF");
      g.ui.button(Game.clipText(label, w - 6.0F), x, y, w, h, true, hint, action);
   }

   static void button(Game g, String label, float x, float y, float w, float h, String hint, Runnable action) {
      g.ui.button(Game.clipText(label, w - 6.0F), x, y, w, h, true, hint, action);
   }

   static void label(Game g, String s, float x, float y, float w, int color) {
      g.gui.text(Game.clipText(s, w), x, y, color, true);
   }

   /** A numbered glass card with a title, a wrapped description and one action. */
   static void card(Game g, String no, String title, String desc, String actionLabel,
      float x, float y, float w, float h, int strip, String hint, Runnable action) {
      Ui ui = g.ui;
      Gui gui = g.gui;
      boolean hov = ui.over(x, y, w, h);
      gui.rect(x + 1.0F, y + 2.0F, w, h, 0x60000000);
      gui.rect(x - 1.0F, y - 1.0F, w + 2.0F, h + 2.0F, hov ? 0xFF6AC8DC : EDGE);
      gui.gradient(x, y, w, h, CARD, CARD_LO);
      gui.rect(x, y, w, 3.0F, strip);
      gui.rect(x, y + 3.0F, 2.0F, h - 4.0F, strip);
      gui.text(no, x + 7.0F, y + 5.0F, CYAN, true, 1.0F);
      gui.text(Game.clipText(title, w - 14.0F), x + 7.0F, y + 16.0F, TEXT, true, 1.05F);
      if (h > 66.0F && desc != null) {
         ui.paragraph(desc, x + 7.0F, y + 30.0F, w - 14.0F, DIM, h > 90.0F ? 3 : 2);
      }
      float bw = Math.min(w - 14.0F, 128.0F);
      String btn = title.equals(actionLabel) ? "OPEN" : actionLabel;
      ui.button(Game.clipText(btn, bw - 6.0F), x + 7.0F, y + h - 19.0F, bw, 16.0F, true, hint, action);
      if (hov && ui.hint == null) ui.hint = hint;
   }

   /** A compact labelled section divider. */
   static void divider(Game g, String s, float x, float y, float w) {
      Gui gui = g.gui;
      gui.text(Game.clipText(s.toUpperCase(), w), x, y, CYAN, true, 1.0F);
      gui.rect(x, y + 10.0F, w, 1.0F, 0x304FD8E8);
   }

   /** Common bottom action strip. */
   static void actions(Game g, Box b, String[] labels, Runnable[] acts, String[] hints) {
      Ui ui = g.ui;
      float gap = 4.0F;
      float bw = (b.w - gap * (labels.length - 1)) / labels.length;
      bw = Math.min(bw, 150.0F);
      for (int i = 0; i < labels.length; i++) {
         final int k = i;
         ui.button(Game.clipText(labels[i], bw - 6.0F), b.x + i * (bw + gap), b.foot - 22.0F,
            bw, 18.0F, true, hints == null ? null : hints[k], acts[k]);
      }
   }

   static void hint(Game g, Box b, String fallback) {
      g.ui.drawHint(b.x, b.foot - 46.0F, b.w, 20.0F, fallback);
   }

   // ==================================================================== TITLE
   static void title(Game g) {
      Box b = frame(g, "main menu", "Blockcraft", "A voxel sandbox - 384 blocks deep", 0);
      Ui ui = g.ui;
      Gui gui = g.gui;
      float cardW = Math.min(210.0F, b.w - 6.0F);
      float cx = b.x + (b.w - cardW) / 2.0F;
      float ch = Math.max(34.0F, Math.min(58.0F, (b.foot - 24.0F - b.y - 8.0F) / 3.0F));
      float y = b.y;

      gui.gradient(cx, y - 2.0F, cardW, 3.0F * ch + 12.0F, 0x101A2B3A, 0x060A111A);
      card(g, "01", "Singleplayer", "Open a saved world or build a new one.", "Singleplayer",
         cx, y, cardW, ch, CYAN, "Your worlds, mini games and the gallery.", () -> {
            g.newSettings = new WorldSettings();
            g.setScreen(Game.S.SP_HUB);
         });
      card(g, "02", "Multiplayer", "Join a friend or host your own world.", "Multiplayer",
         cx, y + ch + 4.0F, cardW, ch, 0xFF7C8CF0, "LAN, Share Codes, worldwide tunnels and named worlds.",
         () -> Screens.openMpHub(g));
      card(g, "03", "Settings", "Video, world generation, sound, controls.", "Settings",
         cx, y + 2.0F * (ch + 4.0F), cardW, ch, GOLD, "Every setting, including Debug Advanced.", () -> {
            g.optionsReturn = Game.S.TITLE;
            g.setScreen(Game.S.OPTIONS);
         });

      float fw = Math.min(64.0F, (b.w - 12.0F) / 3.0F);
      float fy = b.foot - 22.0F;
      ui.button("Trailer", b.x, fy, fw, 18.0F, true, "The 60-second in-game montage.", () -> Trailer.start(g));
      ui.button("Credits", b.x + fw + 4.0F, fy, fw, 18.0F, true, "Music, art and sound credits.", () -> {
         g.creditsReturn = Game.S.TITLE;
         g.creditsScroll = 0.0F;
         g.setScreen(Game.S.CREDITS);
      });
      ui.button("Quit", b.x + 2.0F * (fw + 4.0F), fy, fw, 18.0F, true, null, () -> org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose(g.window, true));
      if (b.foot - 24.0F - (y + 3.0F * (ch + 4.0F)) > 18.0F) {
         label(g, "Deep worlds: ~190 blocks of caves below sea level", b.x, b.foot - 42.0F, b.w, DIM);
      }
      ui.drawToast();
   }

   // ==================================================================== HUBS
   static void singleplayer(Game g) {
      Box b = frame(g, "singleplayer", "Choose a mode", "Everything you can play on your own", 0);
      Ui ui = g.ui;
      float gap = 5.0F;
      float cw = Math.min(200.0F, (b.w - gap) / 2.0F);
      float ch = Math.min(84.0F, Math.max(54.0F, b.h - 34.0F));
      card(g, "01", "Open World", "Survival, creative, huge deep caves and structures.", "Open World",
         b.x, b.y, cw, ch, CYAN, "Your saved worlds and Create New World.", () -> Screens.openWorlds(g));
      card(g, "02", "Mini Games", "Battle, Tumble, Bedwars, CS2 and more.", "Mini Games",
         b.x + cw + gap, b.y, cw, ch, GOLD, "Console Edition arenas.", () -> Screens.openMinigames(g, Game.S.SP_HUB));
      actions(g, b, new String[]{"All Blocks", "Back"}, new Runnable[]{
         () -> { g.ensureAllBlocksShowcaseWorld(); g.worldName = "All_Blocks_Showcase"; g.startWorld(false, null, 1); },
         () -> g.setScreen(Game.S.TITLE)}, new String[]{"A world with every block laid out.", null});
      hint(g, b, "Old saves keep their own cave version; new worlds are 384 blocks tall.");
      ui.drawToast();
   }

   static void mpHub(Game g) {
      Box b = frame(g, "multiplayer", "Play together", "Host, join or pick a mini game", 2);
      Ui ui = g.ui;
      float gap = 5.0F;
      float cw = Math.min(200.0F, (b.w - gap) / 2.0F);
      float ch = Math.min(70.0F, Math.max(48.0F, (b.h - 44.0F) / 2.0F));
      card(g, "01", "Open World", "Full join / host / Share Code screen.", "Open World",
         b.x, b.y, cw, ch, CYAN, "Server address, LAN, Share Code, host.", () -> Screens.openMultiplayer(g));
      card(g, "02", "Mini Games", "Battle, Tumble, Bedwars with friends.", "Mini Games",
         b.x + cw + gap, b.y, cw, ch, GOLD, "Console Edition arenas.", () -> Screens.openMinigames(g, Game.S.MP_HUB));
      card(g, "03", "Create & Host World", "A named world with a real seed and player name.", "Create & Host",
         b.x, b.y + ch + 4.0F, cw, ch, 0xFF7C8CF0, "Create Multiplayer World", () -> {
            g.newName = "";
            g.seedField = "";
            g.focusField = 0;
            g.ui.randomSeed = new Random().nextLong();
            g.setScreen(Game.S.MP_CREATE);
         });
      label(g, "SERVER ADDRESS / SHARE CODE", b.x + cw + gap, b.y + ch + 10.0F, cw, DIM);
      ui.field(b.x + cw + gap, b.y + ch + 21.0F, cw, 17.0F, Net319.serverAddr, "127.0.0.1:25565",
         g.focusField == 0, () -> g.focusField = 0);
      actions(g, b, new String[]{"Join", "Back"}, new Runnable[]{
         () -> Net319.joinFromMenu(g, Net319.serverAddr),
         () -> g.setScreen(Game.S.TITLE)}, new String[]{"Connect to the address or code above.", null});
      hint(g, b, "Multiplayer world creation now uses the same live world-map preview as singleplayer.");
      ui.drawToast();
   }

   // ==================================================================== CREATE A WORLD (map preview kept)
   static void create(Game g, boolean mp) {
      Box b = frame(g, mp ? "multiplayer / create" : "singleplayer / create",
         mp ? "Create Multiplayer World" : "Create New World",
         "The map on the right is the world you will get", mp ? 2 : 1);
      Ui ui = g.ui;
      Gui gui = g.gui;
      WorldSettings ws = g.newSettings;
      boolean compact = gui.h < 300.0F;
      float gap = 6.0F;
      float lw = Math.min(160.0F, (b.w - gap) * 0.48F);
      float rw = b.w - lw - gap;
      float lx = b.x, rx = b.x + lw + gap;
      float y = b.y;
      float limit = b.foot - 50.0F;      // above the hint strip and the bottom actions

      divider(g, "01  /  WORLD", lx, y, lw);
      float fy = y + (compact ? 13.0F : 25.0F);
      float fh = compact ? 15.0F : 17.0F;
      if (!compact) label(g, "World Name", lx, y + 15.0F, lw, DIM);
      ui.field(lx, fy, lw, fh, g.newName, mp ? "Friends' World" : "New World",
         g.focusField == 0, () -> g.focusField = 0);
      float fy2 = fy + (compact ? 18.0F : 30.0F);
      if (!compact) label(g, "Seed", lx, y + 45.0F, lw, DIM);
      ui.field(lx, fy2, lw, fh, g.seedField, "Random", g.focusField == 1, () -> g.focusField = 1);
      float fy3 = fy2 + (compact ? 18.0F : 30.0F);
      if (mp) {
         if (!compact) label(g, "Player Name", lx, y + 75.0F, lw, DIM);
         ui.field(lx, fy3, lw, fh, Net319.customNick, "Steve", g.focusField == 2, () -> g.focusField = 2);
      }

      float ry = (mp ? fy3 : fy2) + 20.0F;
      float step = compact ? 19.0F : 21.0F;
      String[] rows = {"mode", "type", "preset", "deep", "structures", "bonus"};
      for (String row : rows) {
         if (row.equals("preset") && ws.type != WorldSettings.FLAT) continue;
         if (ry + 18.0F > limit) break;
         switch (row) {
            case "mode":
               button(g, "Game Mode: " + Screens.MODE_NAME[g.newMode % Screens.MODE_NAME.length], lx, ry, lw, 18.0F,
                  "Survival, Creative, Adventure, Spectator or Hardcore.",
                  () -> g.newMode = (g.newMode + 1) % Screens.MODE_NAME.length);
               break;
            case "type":
               button(g, "Type: " + ws.typeName(), lx, ry, lw, 18.0F, WorldSettings.TYPE_INFO[ws.type], () -> {
                  ws.type = WorldSettings.TYPE_ORDER[(ws.typeIndex() + 1) % WorldSettings.TYPE_ORDER.length];
                  if (ws.isVoidMode()) g.newMode = 0;
                  else if (ws.type == WorldSettings.ALL_BLOCKS) g.newMode = 1;
               });
               break;
            case "preset":
               button(g, "Preset: " + WorldSettings.FLAT_NAME[ws.flat], lx, ry, lw, 18.0F,
                  "What the flat world is made of.", () -> ws.flat = (ws.flat + 1) % WorldSettings.FLAT_PRESETS);
               break;
            case "structures":
               toggle(g, "Structures", ws.structures, lx, ry, lw, 18.0F,
                  "Villages, strongholds, ruins and the underground structures.", () -> ws.structures = !ws.structures);
               break;
            case "bonus":
               toggle(g, "Bonus Chest", ws.bonusChest, lx, ry, lw, 18.0F,
                  "A chest with starting supplies.", () -> ws.bonusChest = !ws.bonusChest);
               break;
            default:
               toggle(g, "Deep World", g.opt.deepWorld, lx, ry, lw, 18.0F,
                  "V6.35: 384-block worlds with ~190 blocks of caves below sea level. OFF = classic height.",
                  () -> g.opt.deepWorld = !g.opt.deepWorld);
               break;
         }
         ry += step;
      }

      // ---- the world-map preview: kept from the old menu, in a new frame
      divider(g, "02  /  LIVE WORLD MAP", rx, y, rw);
      if (ui.preview == null) ui.preview = new WorldPreview(96, 60, 20, true);
      // Preview the world that will actually be created, including its depth.
      ws.depth = g.opt.deepWorld && g.opt.caveCliffs && ws.terra() ? 128 : 0;
      ui.preview.update(Screens.seedValue(g), ws);
      float ph = Math.max(28.0F, Math.min(96.0F, limit - y - 16.0F));
      gui.rect(rx - 1.0F, y + 12.0F, rw + 2.0F, ph + 2.0F, 0xFF000000);
      gui.rect(rx - 2.0F, y + 11.0F, rw + 4.0F, 1.0F, CYAN);
      gui.image(ui.preview.img, rx, y + 13.0F, rw, ph);
      int[] legend = ui.preview.topBiomes(8);
      for (int i = 0; i < legend.length; i++) {
         float sx = rx + 3.0F + i * 10.0F;
         float sy = y + 13.0F + ph - 10.0F;
         gui.rect(sx, sy, 9.0F, 8.0F, 0xFF000000);
         gui.rect(sx + 1.0F, sy + 1.0F, 7.0F, 6.0F, 0xFF000000 | WorldPreview.color(legend[i]));
      }
      if (g.seedField.trim().isEmpty()) {
         g.hitbox(rx, y + 13.0F, rw, ph, () -> ui.randomSeed = new Random().nextLong());
      }
      if (ui.over(rx, y + 13.0F, rw, ph)) {
         ui.hint = "A real map of this seed: " + ui.preview.legendText(8)
            + " (" + ui.preview.distinctBiomes() + " of " + Biomes.N + " biomes).";
      }
      float iy = y + 18.0F + ph;
      if (iy + 10.0F < limit) {
         label(g, ui.preview.distinctBiomes() + " biomes  /  " + ws.typeName(), rx, iy, rw, GOLD);
      }

      String[] labels = mp ? new String[]{"Create & Host", "Cancel"} : new String[]{"Create New World", "Cancel"};
      Runnable[] acts = new Runnable[]{
         mp ? (() -> Screens.createMpWorld(g)) : (() -> Screens.createWorld(g)),
         () -> { g.scanWorlds(); g.setScreen(g.worldList.isEmpty() ? Game.S.TITLE : Game.S.WORLDS); }
      };
      actions(g, b, labels, acts, new String[]{"Generate the world shown in the map.", null});
      hint(g, b, g.seedField.trim().isEmpty() ? "Leave the seed empty for a random world. Click the map to reroll."
         : "Seed: " + Screens.seedValue(g));
      ui.drawToast();
   }

   static void newWorld(Game g) { create(g, false); }
   static void mpCreate(Game g) { create(g, true); }

   // ==================================================================== WORLD LIST
   static void worlds(Game g) {
      Box b = frame(g, "singleplayer", "Your worlds", g.worldList.size() + " saved", 1);
      Ui ui = g.ui;
      Gui gui = g.gui;
      int n = g.worldList.size();
      float rowH = 30.0F;
      float listH = Math.max(24.0F, b.foot - b.y - 50.0F);
      divider(g, "SAVED WORLDS", b.x, b.y, b.w);
      gui.gradient(b.x, b.y + 13.0F, b.w, listH - 13.0F, CARD, CARD_LO);
      if (n == 0) {
         gui.centered("No worlds yet - create one!", b.x + b.w / 2.0F, b.y + 30.0F, DIM, true, 1.0F);
      } else {
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
            boolean hov = ui.over(b.x + 2.0F, ry, b.w - 4.0F, rowH - 2.0F);
            gui.rect(b.x + 2.0F, ry, b.w - 4.0F, rowH - 2.0F, sel ? CYAN : (hov ? 0xFF3A4A62 : 0xFF222A38));
            gui.gradient(b.x + 3.0F, ry + 1.0F, b.w - 6.0F, rowH - 4.0F,
               sel ? 0xFF203244 : 0xFF121924, 0xFF0B1019);
            if (sel) gui.rect(b.x + 3.0F, ry + 1.0F, 2.0F, rowH - 4.0F, CYAN);
            if (wi.thumb != null) gui.image(wi.thumb.img, b.x + 7.0F, ry + 3.0F, rowH - 8.0F, rowH - 8.0F);
            float tx = b.x + rowH + 2.0F;
            gui.text(Game.clipText(wi.name, b.w - rowH - 20.0F), tx, ry + 4.0F, sel ? TEXT : DIM, true);
            gui.text(Game.clipText(Screens.MODE_NAME[Math.max(0, Math.min(4, wi.mode))] + "  -  " + wi.settings.typeName()
               + "  -  seed " + wi.seed, b.w - rowH - 20.0F), tx, ry + 15.0F, FAINT, true);
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
      actions(g, b, new String[]{"Play", "New", "Delete", "Cancel"}, new Runnable[]{
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
      }, new String[]{"Open the selected world.", "Create New World.", "Click twice to delete.", null});
      hint(g, b, g.confirmDelete ? "Click Delete again to permanently remove the world." : "Double-click a world to open it.");
      ui.drawToast();
   }

   // ==================================================================== MULTIPLAYER
   static void multiplayer(Game g) {
      Box b = frame(g, "multiplayer", "Connect", Net319.statusText, 2);
      Ui ui = g.ui;
      Gui gui = g.gui;
      float cw = (b.w - 6.0F) / 2.0F;
      float x1 = b.x + cw + 6.0F;
      divider(g, "SERVER ADDRESS / SHARE CODE", b.x, b.y, b.w);
      ui.field(b.x, b.y + 13.0F, b.w, 17.0F, Net319.serverAddr, "127.0.0.1:25565",
         g.focusField == 0, () -> g.focusField = 0);
      String state = Net319.isHosting()
         ? "Hosting " + WorldSettings.TYPE_NAME[Net319.mpWorldType] + " | Code: " + Net319.shareCode
         : (Net319.isConnected() ? "Connected (" + Net319.peerCount() + " online)" : Net319.statusText);
      gui.text(Game.clipText(state, b.w), b.x, b.y + 33.0F, Net319.isActive() ? GOOD : FAINT, true);
      float y = b.y + 46.0F;
      button(g, "Join Server", b.x, y, cw, 18.0F, "Connect to the address or Share Code above.",
         () -> Net319.joinFromMenu(g, Net319.serverAddr));
      button(g, "Join LAN", x1, y, cw, 18.0F, "Auto-discover a LAN world.", () -> Net319.joinFromMenu(g, "lan"));
      button(g, "Host World", b.x, y + 21.0F, cw, 18.0F, "Host a world directly and make a Share Code.",
         () -> Net319.hostFromMenu(g));
      button(g, "Start Server.bat", x1, y + 21.0F, cw, 18.0F, "Launch the standalone server and join it.",
         () -> Net319.launchServerAndJoin(g));
      button(g, "Type: " + WorldSettings.TYPE_NAME[Net319.mpWorldType], b.x, y + 42.0F, cw, 18.0F,
         WorldSettings.TYPE_INFO[Net319.mpWorldType], Net319::cycleMpWorldType);
      button(g, "Mode: " + Net319.mpModeName(), x1, y + 42.0F, cw, 18.0F,
         "Game mode for hosted worlds.", Net319::cycleMpGameMode);
      button(g, Net319.mpWorldwide ? "Access: Worldwide" : "Access: LAN Only", b.x, y + 63.0F, cw, 18.0F,
         "Worldwide opens UPnP and the public tunnel.", Net319::toggleMpWorldwide);
      button(g, "Copy Share Code", x1, y + 63.0F, cw, 18.0F, "Copy a BC-... code for friends.",
         () -> Net319.copyShareCode(g));
      if (y + 84.0F + 18.0F < b.foot - 48.0F) {
         button(g, "Disconnect", b.x, y + 84.0F, cw, 18.0F, null, () -> {
            Net319.disconnect(g); Net319.statusText = "Disconnected.";
         });
         button(g, "Nick: " + Net319.displayNick(), x1, y + 84.0F, cw, 18.0F, null, Net319::cyclePresetNick);
      }
      actions(g, b, new String[]{"Back"}, new Runnable[]{() -> g.setScreen(g.mpReturn != null ? g.mpReturn : Game.S.TITLE)}, null);
      hint(g, b, "Use Create & Host World on the Multiplayer hub for a named world with a live map preview.");
      ui.drawToast();
   }

   // ==================================================================== MINI GAMES
   static void minigames(Game g) {
      Box b = frame(g, "mini games", "Arenas", "Console Edition modes", 3);
      Ui ui = g.ui;
      Gui gui = g.gui;
      WorldSettings ws = g.newSettings;
      if (!ws.isMinigame()) ws.type = WorldSettings.MINIGAME_ORDER[Math.max(0, Math.min(WorldSettings.MINIGAME_ORDER.length - 1, g.mgTab))];
      float tabW = b.w / Screens.MINIGAME_TABS.length;
      for (int i = 0; i < Screens.MINIGAME_TABS.length; i++) {
         float tx = b.x + i * tabW;
         boolean on = i == g.mgTab;
         gui.rect(tx, b.y, tabW, 16.0F, on ? 0xFF1E3648 : 0xFF101620);
         if (on) gui.rect(tx, b.y + 14.0F, tabW, 2.0F, CYAN);
         final int idx = i;
         ui.button(Game.clipText(Screens.MINIGAME_TABS[i], tabW - 6.0F), tx + 1.0F, b.y, tabW - 2.0F, 16.0F, true,
            "Switch to " + Screens.MINIGAME_TABS[idx] + ".", () -> {
               g.mgTab = idx;
               ws.type = WorldSettings.MINIGAME_ORDER[idx];
               g.newMode = 0;
            });
      }
      float y = b.y + 21.0F;
      float cw = Math.min(180.0F, b.w * 0.55F);
      float mw = (cw - 4.0F) / 2.0F;
      divider(g, "ARENA MAPS  /  " + ws.typeName(), b.x, y, cw);
      for (int m = 0; m < WorldSettings.MAP_COUNT; m++) {
         final int mi = m;
         float mx = b.x + (m % 2) * (mw + 4.0F);
         float my = y + 13.0F + (m / 2) * 17.0F;
         boolean sel = ws.mapId == m;
         if (sel) gui.rect(mx - 1.0F, my - 1.0F, mw + 2.0F, 17.0F, CYAN);
         ui.button(Game.clipText((sel ? "[ " : "") + WorldSettings.mapNameFor(ws.type, m) + (sel ? " ]" : ""), mw - 6.0F),
            mx, my, mw, 15.0F, true, WorldSettings.mapInfoFor(ws.type, m), () -> {
               ws.mapId = mi;
               ui.say("Map: " + WorldSettings.mapNameFor(ws.type, mi));
            });
      }
      float rx = b.x + cw + 8.0F;
      float rw = b.x + b.w - rx;
      if (rw > 30.0F) {
         divider(g, "SELECTED ARENA", rx, y, rw);
         if (ui.preview == null) ui.preview = new WorldPreview(64, 48, 16, false);
         ui.preview.update(Screens.seedValue(g), ws);
         float ph = Math.min(56.0F, Math.max(24.0F, b.foot - y - 76.0F));
         gui.rect(rx - 1.0F, y + 12.0F, rw + 2.0F, ph + 2.0F, 0xFF000000);
         gui.image(ui.preview.img, rx, y + 13.0F, rw, ph);
         gui.text(Game.clipText(ws.mapName() + "  -  " + ws.mapTheme(), rw), rx, y + 18.0F + ph, GOLD, true);
         if (y + 30.0F + ph < b.foot - 48.0F) {
            ui.paragraph(WorldSettings.mapInfoFor(ws.type, ws.mapId), rx, y + 30.0F + ph, rw, DIM, 2);
         }
      }
      actions(g, b, new String[]{"Play Now", "Open Lobby", "Back"}, new Runnable[]{
         () -> { g.newMode = 0; g.startWorld(true, Long.toString(ui.randomSeed), 0); SkyGen.startMatch(g, false); },
         () -> { g.newMode = 0; SkyGen.matchState = SkyGen.STATE_WAITING; g.startWorld(true, Long.toString(ui.randomSeed), 0); },
         () -> g.setScreen(g.mgReturn != null ? g.mgReturn : Game.S.TITLE)
      }, new String[]{"Jump straight into a match.", "Open the waiting lobby.", null});
      hint(g, b, "Pick an arena on the left; the map on the right is a live world preview.");
      ui.drawToast();
   }

   // ==================================================================== SETTINGS
   static final String[] TABS = {"Display", "Graphics", "Gameplay", "Controls", "Sound", "World", "Content"};
   static final String[] TABS_DEBUG = {"Display", "Graphics", "Gameplay", "Controls", "Sound", "World", "Content", "Advanced"};
   static final int ADV_TAB = 7;

   static String[] tabs(Options o) { return o.debugVideo ? TABS_DEBUG : TABS; }

   static void options(Game g) {
      Ui ui = g.ui;
      Gui gui = g.gui;
      Options o = g.opt;
      String[] names = tabs(o);
      int tab = Math.max(0, Math.min(names.length - 1, ui.optTab));
      Box b = frame(g, "settings", "Settings", "Everything in one place", 4);

      // three columns of wide segmented tabs so every name fits at 320px too
      final int cols = 3;
      int tabRows = (names.length + cols - 1) / cols;
      float tw = b.w / cols;
      for (int i = 0; i < names.length; i++) {
         int row = i / cols, col = i % cols;
         float tx = b.x + col * tw, ty = b.y + row * 19.0F;
         boolean on = i == tab;
         gui.rect(tx, ty, tw, 16.0F, on ? 0xFF1C3446 : 0xFF101722);
         if (on) gui.rect(tx, ty + 14.0F, tw, 2.0F, CYAN);
         final int idx = i;
         ui.button(Game.clipText(names[i], tw - 6.0F), tx + 1.0F, ty, tw - 2.0F, 16.0F, true,
            "Open the " + names[i] + " settings.", () -> ui.optTab = idx);
      }
      float rowsY = b.y + tabRows * 19.0F + 2.0F;
      float rowsH = Math.max(20.0F, b.foot - rowsY - 48.0F);
      gui.gradient(b.x, rowsY, b.w, rowsH, CARD, CARD_LO);
      gui.rect(b.x, rowsY, b.w, 1.0F, RULE);

      if (tab == ADV_TAB) {
         // Debug Advanced: keep EVERY render/shader/performance switch available.
         float colW = Math.min(150.0F, (b.w - 10.0F) / 2.0F);
         ui.beginScroll(b.x, rowsY, b.w, rowsH, ui.optScroll, 62.0F * 20.0F, 20.0F);
         int before = g.btns.size();
         Screens.advancedTab(g, o, b.x + 3.0F, b.x + colW + 7.0F, colW, rowsY - ui.optScroll[0], 20.0F,
            rowsY, rowsH);
         // the advanced tab was written for a wider two-column panel: clip its
         // labels so they still fit inside the Atlas rail layout.
         for (int i = before; i < g.btns.size(); i++) {
            Game.Btn btn = g.btns.get(i);
            if (btn.label != null) btn.label = Game.clipText(btn.label, btn.w - 6.0F);
         }
         ui.endScroll(b.x, rowsY, b.w, rowsH, 62.0F * 20.0F);
      } else {
         ui.beginScroll(b.x, rowsY, b.w, rowsH, ui.optScroll, 22.0F * 20.0F, 20.0F);
         Rows r = new Rows(g, b.x + 4.0F, rowsY + 2.0F - ui.optScroll[0], b.w - 10.0F);
         r.top = rowsY;
         r.bottom = rowsY + rowsH;
         fill(g, r, tab);
         ui.endScroll(b.x, rowsY, b.w, rowsH, 22.0F * 20.0F);
      }

      actions(g, b, new String[]{"Reset", "Back"}, new Runnable[]{
         () -> { Screens.resetAdvanced(o); ui.say("Settings reset to defaults."); },
         () -> g.setScreen(g.optionsReturn != null ? g.optionsReturn : Game.S.TITLE)
      }, new String[]{"Reset these settings to their defaults.", null});
      hint(g, b, "Enable Debug Video to unlock the Advanced tab with every shader and LOD control.");
      ui.drawToast();
   }

   /** Small helper that lays out labelled rows in two columns and returns the y after each. */
   static final class Rows {
      final Game g;
      final Ui ui;
      final float x, w;
      float y, top, bottom;
      float colW;
      int count;

      Rows(Game g, float x, float y, float w) {
         this.g = g; this.ui = g.ui; this.x = x; this.y = y; this.w = w;
         this.colW = Math.max(80.0F, (w - 6.0F) / 2.0F);
         this.top = y; this.bottom = Float.MAX_VALUE;
      }

      boolean visible(float py) { return py + 17.0F > top && py < bottom; }

      float next(int span) {
         float px = x + (span == 1 ? colW + 6.0F : 0.0F);
         float pw = span == 0 ? colW : (span == 1 ? colW : w);
         float py = y;
         if (span != 1) y += 20.0F;
         count++;
         return py;
      }

      float px(int span) { return span == 1 ? x + colW + 6.0F : x; }
      float pw(int span) { return span == 2 ? w : colW; }
   }

   /** span 0 = left column (wait for its partner), 1 = right column, 2 = full width. */
   static void advance(Rows r, int span) {
      if (span != 0) r.y += 20.0F;
      r.count++;
   }

   static void toggleRow(Game g, Rows r, String label, String hint, Supplier<Boolean> get, Consumer<Boolean> set, int span) {
      float px = r.px(span), pw = r.pw(span), py = r.y;
      if (r.visible(py)) toggle(g, label, get.get(), px, py, pw, 17.0F, hint, () -> set.accept(!get.get()));
      advance(r, span);
   }

   static void cycleRow(Game g, Rows r, String label, String[] names, int idx, String hint, Consumer<Integer> set, int span) {
      float px = r.px(span), pw = r.pw(span), py = r.y;
      final int[] cur = {idx};
      if (r.visible(py)) button(g, label + ": " + names[cur[0]], px, py, pw, 17.0F, hint,
         () -> { cur[0] = (cur[0] + 1) % names.length; set.accept(cur[0]); });
      advance(r, span);
   }

   static void sliderRow(Game g, Rows r, String label, String hint, float v, float lo, float hi,
      Consumer<Float> set, int span) {
      float px = r.px(span), pw = r.pw(span), py = r.y;
      if (r.visible(py)) {
         r.ui.slider(Game.clipText(label, pw - 6.0F), px, py, pw, 17.0F,
            Math.max(0.0F, Math.min(1.0F, (v - lo) / (hi - lo))), hint, f -> set.accept(lo + f * (hi - lo)));
      }
      advance(r, span);
   }

   static void fill(Game g, Rows r, int tab) {
      Ui ui = g.ui;
      Options o = g.opt;
      switch (tab) {
         case 0: // Display
            sliderRow(g, r, "Render Distance", "How many chunks are drawn.", o.renderDistance, 2, 32, f -> o.renderDistance = Math.round(f), 0);
            sliderRow(g, r, "FOV", "Field of view in degrees.", o.fov, 50, 110, f -> o.fov = Math.round(f), 1);
            toggleRow(g, r, "VSync", "Sync frames to the display.", () -> o.vsync, v -> o.vsync = v, 0);
            toggleRow(g, r, "Fullscreen", "Use the whole display.", () -> o.fullscreen, v -> { o.fullscreen = v; g.applyDisplayMode(); }, 1);
            toggleRow(g, r, "Borderless", "Borderless fullscreen window.", () -> o.borderless, v -> { o.borderless = v; g.applyDisplayMode(); }, 0);
            sliderRow(g, r, "GUI Scale", "Menu size (0 = automatic).", o.guiScale, 0, 3, f -> o.guiScale = Math.round(f), 1);
            sliderRow(g, r, "Max FPS", "0 = unlimited.", o.maxFps, 0, 240, f -> o.maxFps = Math.round(f), 0);
            sliderRow(g, r, "Gamma", "Brightness.", o.gamma, -0.5F, 0.5F, f -> o.gamma = f, 1);
            toggleRow(g, r, "Smooth Lighting", "Softer light on blocks.", () -> o.smooth, v -> o.smooth = v, 0);
            toggleRow(g, r, "Panorama", "Animated world behind menus.", () -> o.panorama > 0, v -> o.panorama = v ? 1 : 0, 1);
            break;
         case 1: // Graphics
            toggleRow(g, r, "Shaders", "The built-in shader pack.", () -> o.shaders, v -> o.shaders = v, 0);
            toggleRow(g, r, "Clouds", "Draw clouds.", () -> o.clouds, v -> o.clouds = v, 1);
            cycleRow(g, r, "Leaves", new String[]{"Fast", "Fancy"}, Math.max(0, Math.min(1, o.leaves)), "Leaf quality.", v -> o.leaves = v, 0);
            cycleRow(g, r, "Bloom", Screens.BLOOM_NAME, o.bloom, "Bloom strength.", v -> o.bloom = v, 1);
            toggleRow(g, r, "Moon Shadows", "The moon casts shadows too.", () -> o.moonShadows, v -> o.moonShadows = v, 0);
            toggleRow(g, r, "Entity Shadows", "Mobs and players cast shadows.", () -> o.entityShadows, v -> o.entityShadows = v, 1);
            toggleRow(g, r, "Hand Lighting", "Your hand follows the lighting.", () -> o.handLighting, v -> o.handLighting = v, 0);
            toggleRow(g, r, "Weather", "Rain, snow and thunder.", () -> o.weather, v -> o.weather = v, 1);
            sliderRow(g, r, "LOD Falloff", "How fast distant LOD fades.", o.lodFalloff, 0.2F, 2.0F, f -> o.lodFalloff = f, 0);
            sliderRow(g, r, "LOD Distance", "Where LOD chunks start.", o.lodDistance, 16, 256, f -> o.lodDistance = Math.round(f), 1);
            toggleRow(g, r, "Debug Video", "Unlock the Advanced tab.", () -> o.debugVideo, v -> o.debugVideo = v, 2);
            break;
         case 2: // Gameplay
            toggleRow(g, r, "Invert Mouse", "Flip vertical look.", () -> o.invertY, v -> o.invertY = v, 0);
            sliderRow(g, r, "Sensitivity", "Mouse sensitivity.", o.sensitivity, 0.1F, 1.5F, f -> o.sensitivity = f, 1);
            toggleRow(g, r, "Bobbing", "View bobbing while walking.", () -> o.bobbing, v -> o.bobbing = v, 0);
            toggleRow(g, r, "Minimap", "Show the minimap.", () -> o.minimap, v -> o.minimap = v, 1);
            toggleRow(g, r, "Rotate Minimap", "Minimap follows your heading.", () -> o.minimapRotate, v -> o.minimapRotate = v, 0);
            cycleRow(g, r, "Minimap Pos", new String[]{"TR", "TL", "BR", "BL"}, Math.max(0, Math.min(3, o.minimapPos)),
               "Which corner holds the minimap.", v -> o.minimapPos = v, 1);
            toggleRow(g, r, "Dodge Toasts", "Move the minimap for achievement toasts.", () -> o.minimapAutoMove, v -> o.minimapAutoMove = v, 0);
            toggleRow(g, r, "Disasters", "Enable world disasters.", () -> o.disasters, v -> o.disasters = v, 1);
            break;
         case 3: // Controls
            for (int i = 0; i < Screens.KEYS.length && i < 12; i++) {
               float y = r.y;
               g.gui.text(Game.clipText(Screens.KEYS[i][0], r.colW - 4.0F), r.x, y + 2.0F, CYAN, true);
               g.gui.text(Game.clipText(Screens.KEYS[i][1], r.colW - 4.0F), r.x + r.colW + 6.0F, y + 2.0F, DIM, true);
               r.y += 15.0F;
            }
            break;
         case 4: // Sound
            sliderRow(g, r, "Volume", "Master sound volume.", o.soundVolume, 0.0F, 1.0F, f -> o.soundVolume = f, 0);
            sliderRow(g, r, "Music", "Music volume.", o.musicVolume, 0.0F, 1.0F, f -> o.musicVolume = f, 1);
            toggleRow(g, r, "3D Acoustics", "Positional cave and world sound.", () -> o.realAcoustics, v -> o.realAcoustics = v, 0);
            toggleRow(g, r, "Constant Music", "Never stop the music.", () -> o.constantMusic, v -> o.constantMusic = v, 1);
            break;
         case 5: // World
            toggleRow(g, r, "Caves & Cliffs", "Minecraft-style 3D cave volumes.", () -> o.caveCliffs, v -> o.caveCliffs = v, 0);
            toggleRow(g, r, "Deep World", "384-block worlds with ~190 blocks of caves below sea level.",
               () -> o.deepWorld, v -> o.deepWorld = v, 1);
            toggleRow(g, r, "Cave Structures", "Mineshafts, dungeons, geodes and more in the cave air.",
               () -> o.caveStructures, v -> o.caveStructures = v, 0);
            toggleRow(g, r, "New Terrain", "Smooth biome borders and layered generation.", () -> o.newTerrain, v -> o.newTerrain = v, 1);
            sliderRow(g, r, "Cave Scale", "Size of cave shapes.", o.caveScale, 0.6F, 3.0F, f -> o.caveScale = f, 0);
            sliderRow(g, r, "Cave Density", "How much of the deep world is carved.", o.caveDensity, 0.4F, 2.5F, f -> o.caveDensity = f, 1);
            sliderRow(g, r, "Biome Borders", "0 sharp, 2 very smooth.", o.biomeBorders, 0, 2, f -> o.biomeBorders = Math.round(f), 0);
            sliderRow(g, r, "Tree Density", "Fewer trees in taiga = better performance.", o.treeDensity, 0.4F, 1.4F, f -> o.treeDensity = f, 1);
            break;
         default: // Content
            ui.button("Skins", r.px(0), r.y, r.pw(0), 17.0F, true, "Choose or import a skin.", () -> {
               g.skinReturn = Game.S.OPTIONS; g.setScreen(Game.S.SKINS);
            });
            ui.button("Resource Packs", r.px(1), r.y, r.pw(1), 17.0F, true, "Reload PNGs and packs.", () -> {
               g.packsReturn = Game.S.OPTIONS; g.setScreen(Game.S.PACKS);
            });
            r.y += 20.0F;
            ui.button("Mods", r.px(0), r.y, r.pw(0), 17.0F, true, "Enable or disable mods.", () -> {
               g.modsReturn = Game.S.OPTIONS; g.setScreen(Game.S.MODS);
            });
            ui.button("Credits", r.px(1), r.y, r.pw(1), 17.0F, true, "Music and sound credits.", () -> {
               g.creditsReturn = Game.S.OPTIONS; g.setScreen(Game.S.CREDITS);
            });
            r.y += 20.0F;
            ui.button("Watch Trailer", r.px(0), r.y, r.pw(0), 17.0F, true, "The 60-second montage.", () -> Trailer.start(g));
            ui.button("All Blocks", r.px(1), r.y, r.pw(1), 17.0F, true, "A gallery world with every block.", () -> {
               g.ensureAllBlocksShowcaseWorld(); g.worldName = "All_Blocks_Showcase"; g.startWorld(false, null, 1);
            });
            r.y += 20.0F;
            ui.button("Tilesheet", r.px(2), r.y, r.pw(2), 17.0F, true, "The editable one-sheet texture atlas.",
               () -> {
               Tilesheet.invalidate();
               g.ui.say("Tilesheet: " + Tilesheet.changedTiles() + " edited tiles active");
            });
            break;
      }
   }

   // ==================================================================== PAUSE / MORE / UTILITY
   static void pause(Game g) {
      Box b = frame(g, "in game", "Game Menu", g.worldDisplayName, -1);
      Ui ui = g.ui;
      float cw = Math.min(180.0F, (b.w - 6.0F) / 2.0F);
      float y = b.y;
      ui.button("Back to Game", b.x, y, cw, 18.0F, true, null, () -> g.setScreen(Game.S.PLAY));
      ui.button("Advancements", b.x + cw + 6.0F, y, cw, 18.0F, true, null, () -> g.setScreen(Game.S.ADV));
      ui.button("Options", b.x, y + 21.0F, cw, 18.0F, true, null, () -> {
         g.optionsReturn = Game.S.PAUSE; g.setScreen(Game.S.OPTIONS);
      });
      ui.button("Skins", b.x + cw + 6.0F, y + 21.0F, cw, 18.0F, true, null, () -> {
         g.skinReturn = Game.S.PAUSE; g.setScreen(Game.S.SKINS);
      });
      ui.button("Packs", b.x, y + 42.0F, cw, 18.0F, true, null, () -> {
         g.packsReturn = Game.S.PAUSE; g.setScreen(Game.S.PACKS);
      });
      ui.button("Mods", b.x + cw + 6.0F, y + 42.0F, cw, 18.0F, true, null, () -> {
         g.modsReturn = Game.S.PAUSE; g.setScreen(Game.S.MODS);
      });
      ui.button(Net319.hostLabel(), b.x, y + 63.0F, cw, 18.0F, true, null, () -> Net319.toggleHost(g));
      ui.button(Net319.joinLabel(), b.x + cw + 6.0F, y + 63.0F, cw, 18.0F, true, null, () -> Net319.toggleJoin(g));
      boolean mg = g.world != null && g.world.settings != null && g.world.settings.isMinigame();
      if (mg && !Net319.isClientConnected()) {
         ui.button("New Round", b.x, y + 84.0F, cw, 18.0F, true, null, () -> SkyGen.startMatch(g, true));
         ui.button("Save and Quit to Title", b.x + cw + 6.0F, y + 84.0F, cw, 18.0F, true, null, g::quitToTitle);
      } else {
         ui.button("Save and Quit to Title", b.x, y + 84.0F, b.w, 18.0F, true, null, g::quitToTitle);
      }
      hint(g, b, "Escape closes the menu. The world keeps running while you are here.");
      ui.drawToast();
   }

   static void more(Game g) {
      Box b = frame(g, "extras", "More", "Everything that did not fit on the main screen", 5);
      Ui ui = g.ui;
      String[] labels = {"Skins", "Resource Packs", "Mods", "All Blocks", "Credits", "Trailer"};
      Runnable[] acts = {
         () -> { g.skinReturn = Game.S.MORE; g.setScreen(Game.S.SKINS); },
         () -> { g.packsReturn = Game.S.MORE; g.setScreen(Game.S.PACKS); },
         () -> { g.modsReturn = Game.S.MORE; g.setScreen(Game.S.MODS); },
         () -> { g.ensureAllBlocksShowcaseWorld(); g.worldName = "All_Blocks_Showcase"; g.startWorld(false, null, 1); },
         () -> { g.creditsReturn = Game.S.MORE; g.creditsScroll = 0.0F; g.setScreen(Game.S.CREDITS); },
         () -> Trailer.start(g)
      };
      float cw = (b.w - 6.0F) / 2.0F;
      for (int i = 0; i < labels.length; i++) {
         float x = b.x + (i % 2) * (cw + 6.0F);
         float y = b.y + (i / 2) * 22.0F;
         ui.button(labels[i], x, y, cw, 18.0F, true, null, acts[i]);
      }
      actions(g, b, new String[]{"Back"}, new Runnable[]{() -> g.setScreen(Game.S.TITLE)}, null);
      hint(g, b, "The Debug Advanced tab holds every shader and performance setting.");
      ui.drawToast();
   }

   static void loading(Game g) {
      Box b = frame(g, "loading", "Building world", g.worldDisplayName, -1);
      Gui gui = g.gui;
      divider(g, "PROGRESS", b.x, b.y, b.w);
      float w = b.w;
      gui.rect(b.x, b.y + 16.0F, w, 10.0F, 0xFF05080E);
      gui.rect(b.x + 1.0F, b.y + 17.0F, Math.max(0.0F, (w - 2.0F) * g.loadFrac), 8.0F, CYAN);
      gui.text(Game.clipText(g.loadingMsg, w), b.x, b.y + 30.0F, DIM, true);
      uiHint(g, b, "Generating a 384-block-deep world. Large caves take a moment on the first visit.");
   }

   static void waiting(Game g) {
      Box b = frame(g, "lobby", "Waiting Lobby", "Invite friends before the match starts", 3);
      Ui ui = g.ui;
      divider(g, "LOBBY", b.x, b.y, b.w);
      guiText(g, b, "Share Code: " + Net319.shareCode, b.y + 15.0F, GOLD);
      ui.button("Start Match", b.x, b.y + 32.0F, Math.min(140.0F, b.w), 18.0F, true,
         "Start the match now.", () -> SkyGen.startMatch(g, false));
      ui.button("Leave", b.x, b.y + 54.0F, Math.min(140.0F, b.w), 18.0F, true, null, g::quitToTitle);
      hint(g, b, "Players on LAN or with the Share Code appear here.");
      ui.drawToast();
   }

   static void resourcePacks(Game g) {
      Box b = frame(g, "content", "Resource Packs", "PNG overrides and the editable tilesheet", 5);
      Ui ui = g.ui;
      ui.button("Reload PNGs", b.x, b.y, Math.min(160.0F, b.w), 18.0F, true,
         "Re-read the loose PNG overrides.", () -> ResourcePacks.reload(g));
      ui.button("Tilesheet", b.x, b.y + 21.0F, Math.min(160.0F, b.w), 18.0F, true,
         "Open the one-sheet editable atlas.", () -> {
               Tilesheet.invalidate();
               g.ui.say("Tilesheet: " + Tilesheet.changedTiles() + " edited tiles active");
            });
      actions(g, b, new String[]{"Back"}, new Runnable[]{
         () -> g.setScreen(g.packsReturn != null ? g.packsReturn : Game.S.MORE)}, null);
      hint(g, b, "PNG overrides still work with every pack kind, including Vibrant and Retro.");
      ui.drawToast();
   }

   static void mods(Game g) {
      Box b = frame(g, "content", "Mods", "Installed mods and the catalog", 5);
      Ui ui = g.ui;
      ui.button("Browse Catalog", b.x, b.y, Math.min(160.0F, b.w), 18.0F, true, null, () -> g.ui.modTab = 0);
      ui.button("Installed Mods", b.x, b.y + 21.0F, Math.min(160.0F, b.w), 18.0F, true, null, () -> g.ui.modTab = 1);
      java.util.List<ModLoader.ModEntry> mods = ModLoader.allMods();
      float y = b.y + 46.0F;
      for (int i = 0; i < Math.min(6, mods.size()); i++) {
         if (y + 18.0F > b.foot - 48.0F) break;
         ModLoader.ModEntry m = mods.get(i);
         final String id = m.id;
         ui.button(Game.clipText((m.enabled ? "ON  " : "OFF ") + m.name, Math.min(160.0F, b.w) - 6.0F), b.x, y,
            Math.min(160.0F, b.w), 17.0F, true, "Toggle this mod.", () -> {
               ModLoader.toggleEnabled(id);
               ui.say("Mod " + id + " toggled");
            });
         y += 20.0F;
      }
      actions(g, b, new String[]{"Back"}, new Runnable[]{
         () -> g.setScreen(g.modsReturn != null ? g.modsReturn : Game.S.MORE)}, null);
      hint(g, b, "Mods are loaded from the mods folder.");
      ui.drawToast();
   }

   static void credits(Game g) {
      Box b = frame(g, "credits", "Credits", "Original code, art, music and CC0 field recordings", 5);
      Ui ui = g.ui;
      divider(g, "THANK YOU", b.x, b.y, b.w);
      ui.paragraph("All world generation, textures, sounds, UI and shaders in Blockcraft are original. "
         + "The ten real CC0 WAV field recordings are credited in res/sounds/foley/CREDITS.txt. "
         + "This is NOT Minecraft and contains no Mojang code or assets.",
         b.x, b.y + 15.0F, b.w, DIM, 6);
      actions(g, b, new String[]{"Back"}, new Runnable[]{
         () -> g.setScreen(g.creditsReturn != null ? g.creditsReturn : Game.S.MORE)}, null);
      hint(g, b, "V6.35: deep worlds, crazier caves with depth, and the Atlas menus.");
      ui.drawToast();
   }

   static void uiHint(Game g, Box b, String s) { g.ui.drawHint(b.x, b.foot - 46.0F, b.w, 20.0F, s); }
   static void guiText(Game g, Box b, String s, float y, int color) {
      g.gui.text(Game.clipText(s, b.w), b.x, y, color, true);
   }
}
