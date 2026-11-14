package bc;

import static bc.GameTests.check;
import static bc.GameTests.eq;
import static bc.GameTests.test;

import java.io.File;
import java.nio.file.Files;

/** V6.33: all menus redone - three main choices, mode hubs, named multiplayer
 *  world creation, a better mini games hub and seven focused settings tabs. */
final class Tests633 {
   private Tests633() {
   }

   static void run() {
      test("6.33: the main screen has Singleplayer, Multiplayer and Settings", Tests633::title);
      test("6.33: both hubs offer Open World and Mini Games", Tests633::hubs);
      test("6.33: multiplayer world creation has a real name, seed and player name", Tests633::mpCreate);
      test("6.33: settings have seven focused tabs plus the debug tab", Tests633::tabs);
      test("6.33: the mini games hub and the release wiring are done", Tests633::wiring);
   }

   static String src(String p) throws Exception {
      return new String(Files.readAllBytes(new File(p).toPath()), "UTF-8");
   }

   static void title() throws Exception {
      String s = src("src/bc/Screens.java");
      check(s.contains("ui.button(\"Singleplayer\", bx, by, bw, 22.0F"),
         "Singleplayer is a main-screen button");
      check(s.contains("ui.button(\"Multiplayer\", bx, by + row, bw, 22.0F"),
         "Multiplayer is a main-screen button");
      check(s.contains("ui.button(\"Settings\", bx, by + row * 2, bw, 22.0F"),
         "Settings is a main-screen button");
      check(!s.contains("ui.button(\"Options...\", bx, by + row * 3"),
         "the old five-button title stack is gone");
      check(s.contains("Game.S.SP_HUB") && s.contains("openMpHub(g)"),
         "Singleplayer and Multiplayer open their hubs");
      String g = src("src/bc/Game.java");
      check(g.contains("SP_HUB") && g.contains("MP_HUB") && g.contains("MP_CREATE"),
         "the three new screens are real game screens");
      check(g.contains("case SP_HUB:") && g.contains("case MP_HUB:") && g.contains("case MP_CREATE:"),
         "all three are drawn and handle keys");
   }

   static void hubs() throws Exception {
      String s = src("src/bc/Screens.java");
      check(s.contains("static void singleplayer(Game g)") && s.contains("static void mpHub(Game g)"),
         "there is a hub for each play style");
      check(s.contains("card(g, ui, gui, ix, cardY, iw, cardH, \"Open World\"")
         && s.contains("\"Mini Games\", \"Battle, Tumble, Bedwars, CS2 and more\""),
         "the singleplayer hub has Open World and Mini Games cards");
      check(s.contains("\"Open World\", \"Join or host a survival world\"")
         && s.contains("\"Mini Games\", \"Battle, Tumble, Bedwars with friends\""),
         "the multiplayer hub has the same two choices");
      check(s.contains("openMinigames(g, Game.S.SP_HUB)") && s.contains("openMinigames(g, Game.S.MP_HUB)"),
         "mini games are reachable from both hubs");
      check(s.contains("Create & Host World") && s.contains("Game.S.MP_CREATE"),
         "the multiplayer hub leads to named world creation");
      check(s.contains("Server Address / Share Code") && s.contains("Net319.joinFromMenu"),
         "direct address joining is still on the multiplayer hub");
   }

   static void mpCreate() throws Exception {
      String s = src("src/bc/Screens.java");
      check(s.contains("static void mpCreate(Game g)") && s.contains("Create Multiplayer World"),
         "a multiplayer creation screen exists");
      check(s.contains("ui.label(\"World Name\"") && s.contains("ui.label(\"Seed\"")
         && s.contains("ui.label(\"Player Name\""),
         "it has a real world name, seed and player name");
      check(s.contains("ui.preview.update(seedValue(g), ws)") && s.contains("Create & Host"),
         "it has the singleplayer-style live preview and a create button");
      check(s.contains("static void createMpWorld(Game g)") && s.contains("Net319.hostFromMenu(g)"),
         "creating actually hosts the world");
      String n = src("src/bc/Net319.java");
      check(n.contains("mpWorldName") && n.contains("mpSeed")
         && n.contains("g.worldDisplayName = mpWorldName"),
         "the chosen name and seed reach the hosted world");
      String g = src("src/bc/Game.java");
      check(g.contains("case MP_CREATE:") && g.contains("Net319.customNick"),
         "typing works in all three fields");
   }

   static void tabs() throws Exception {
      eq(7, Screens.TABS.length, "seven focused settings tabs");
      eq(8, Screens.TABS_DEBUG.length, "eight with the debug tab");
      eq(7, Screens.ADV_TAB, "Advanced is the last tab");
      eq("Sound", Screens.TABS[4], "a dedicated Sound tab");
      eq("World", Screens.TABS[5], "a dedicated World tab");
      eq("Content", Screens.TABS[6], "a dedicated Content tab");
      String s = src("src/bc/Screens.java");
      check(s.contains("Arrays.copyOfRange(names, 0, half)") && s.contains("top = 56.0F"),
         "tabs are drawn in two readable rows");
      check(s.contains("static void soundTab(") && s.contains("static void worldTab(")
         && s.contains("static void contentTab("),
         "the three new tabs have real content");
      check(s.contains("Sound Volume") && s.contains("3D Acoustics")
         && s.contains("Caves & Cliffs") && s.contains("Watch Trailer (1:00)"),
         "settings that used to be hard to find now have their own places");
      check(s.contains("\"More...\"") && s.contains("static void more(Game g)"),
         "the extras screen is still reachable from Content");
   }

   static void wiring() throws Exception {
      String s = src("src/bc/Screens.java");
      check(s.contains("ARENA MAPS") && s.contains("0x80FFD84A")
         && s.contains("gui.centered(Game.clipText(ws.typeName()"),
         "the mini games hub has a proper card look and a selected-map highlight");
      eq("6.38", Game.VERSION_STR, "the game version is 6.37");
      eq("6.38", src("ci/release-trigger.txt").trim(), "the release workflow publishes 6.37");
      check(src("README.md").contains("V6.33"), "README describes V6.33");
      check(src("package/README.txt").contains("WHAT'S NEW IN V6.33"), "the packaged readme describes V6.33");
      check(new File("docs/releases/v6.33.md").isFile(), "V6.33 release notes exist");
      check(new File("docs/screenshots/v6.33-menus.png").isFile(), "the menu preview exists");
   }
}
