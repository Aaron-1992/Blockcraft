package bc;

import static bc.GameTests.check;
import static bc.GameTests.eq;
import static bc.GameTests.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Update 3.13: the inventory screens (creative tabs and search, number keys, Q, tooltips). The screens themselves are drawn into a software picture
 * (see {@link TestsUi} and {@link GuiRaster}); here the behaviour is checked.   python3 tools/runtests.py unit
 */
final class TestsInv {
   static void run() {
      test("inv: every creative item is in exactly one tab", TestsInv::tabs);
      test("inv: the tabs put things where a player looks for them", TestsInv::classification);
      test("inv: creative search", TestsInv::search);
      test("inv: the creative menu (tabs, typing, pages)", TestsInv::menu);
      test("inv: clicking the tabs", TestsInv::clicks);
      test("inv: number keys swap with the hotbar, Q drops", TestsInv::keys);
      test("inv: tooltips say what an item is", TestsInv::tooltips);
   }

   static int id(String name) {
      for (int i : Blocks.creativeList()) {
         if (Items.name(i).equals(name)) {
            return i;
         }
      }

      throw new IllegalStateException("no creative item called " + name);
   }

   static void tabs() {
      int[] all = Blocks.creativeList();
      Set<Integer> seen = new HashSet<>();
      int count = 0;
      for (int t = 0; t < Creative.SEARCH; t++) {
         int[] ids = Creative.tab(t);
         check(ids.length > 0, "the tab " + Creative.NAME[t] + " is not empty");
         for (int i : ids) {
            check(seen.add(i), Items.name(i) + " is in two tabs");
            eq(t, Creative.tabOf(i), Items.name(i) + " is in the tab the rules say");
         }

         count += ids.length;
      }

      eq(all.length, count, "the tabs together hold the whole creative list");
      for (int i : all) {
         check(seen.contains(i), Items.name(i) + " is in some tab");
      }

      eq(Creative.TABS, Creative.NAME.length, "a name for every tab");
      eq(Creative.TABS, Creative.ICON.length, "a picture for every tab");
      eq(Creative.TABS - 1, Creative.SEARCH, "the search tab is the last");
      check(new HashSet<>(Arrays.asList(Creative.NAME)).size() == Creative.TABS, "the tab names differ");
      // an unknown future block ends up somewhere sensible, never nowhere
      for (int i = 0; i < 4096; i++) {
         int t = Creative.tabOf(i);
         check(t >= 0 && t < Creative.SEARCH, "item " + i + " has a tab");
      }

      // the names in the list are unique, so that nothing looks like a duplicate in a tooltip or in the search
      Set<String> names = new HashSet<>();
      for (int i : all) {
         check(names.add(Items.name(i)), "the name " + Items.name(i) + " is used twice");
      }
   }

   static void classification() {
      eq(Creative.BUILDING, Creative.tabOf(id("Stone Bricks")), "stone bricks");
      eq(Creative.BUILDING, Creative.tabOf(id("Oak Planks")), "planks");
      eq(Creative.BUILDING, Creative.tabOf(id("Brick Stairs")), "stairs");
      eq(Creative.COLORS, Creative.tabOf(id("Red Wool")), "wool");
      eq(Creative.COLORS, Creative.tabOf(id("Lime Concrete")), "concrete");
      eq(Creative.COLORS, Creative.tabOf(id("Blue Stained Glass")), "stained glass");
      eq(Creative.NATURE, Creative.tabOf(id("Oak Log")), "logs");
      eq(Creative.NATURE, Creative.tabOf(id("Diamond Ore")), "ores");
      eq(Creative.NATURE, Creative.tabOf(id("Poppy")), "flowers");
      eq(Creative.NATURE, Creative.tabOf(id("Dirt")), "dirt");
      eq(Creative.UTILITY, Creative.tabOf(id("Torch")), "torches");
      eq(Creative.UTILITY, Creative.tabOf(id("Chest")), "chests");
      eq(Creative.UTILITY, Creative.tabOf(id("Crafting Table")), "workbenches");
      eq(Creative.TOOLS, Creative.tabOf(id("Iron Pickaxe")), "tools");
      eq(Creative.TOOLS, Creative.tabOf(id("Diamond Sword")), "swords");
      eq(Creative.TOOLS, Creative.tabOf(id("Iron Chestplate")), "armor");
      eq(Creative.TOOLS, Creative.tabOf(id("Bow")), "bows");
      eq(Creative.FOOD, Creative.tabOf(id("Bread")), "bread");
      eq(Creative.FOOD, Creative.tabOf(id("Wheat Seeds")), "seeds");
      eq(Creative.MATERIALS, Creative.tabOf(id("Iron Ingot")), "ingots");
      eq(Creative.MATERIALS, Creative.tabOf(id("Stick")), "sticks");
      // inside a tab similar things stand together: planks before bricks before stairs
      int[] b = Creative.tab(Creative.BUILDING);
      check(indexOf(b, id("Oak Planks")) < indexOf(b, id("Bricks")), "planks come before bricks");
      check(indexOf(b, id("Bricks")) < indexOf(b, id("Oak Wood Stairs")), "bricks come before stairs");
      check(indexOf(b, id("Oak Wood Stairs")) < indexOf(b, id("Oak Wood Slab")), "stairs come before slabs");
      int[] c = Creative.tab(Creative.COLORS);
      check(indexOf(c, id("Black Wool")) < indexOf(c, id("White Concrete")), "wool comes before concrete");
      check(indexOf(c, id("White Wool")) < indexOf(c, id("Black Wool")), "the colours keep their order");
   }

   static int indexOf(int[] a, int v) {
      for (int i = 0; i < a.length; i++) {
         if (a[i] == v) {
            return i;
         }
      }

      throw new IllegalStateException("not in the tab: " + Items.name(v));
   }

   static void search() {
      int[] all = Blocks.creativeList();
      check(Arrays.equals(all, Creative.search("")), "an empty text finds everything");
      check(Arrays.equals(all, Creative.search("   ")), "spaces find everything");
      check(Arrays.equals(all, Creative.search(null)), "null finds everything");
      int[] pick = Creative.search("PICKAXE");
      int expected = 0;
      for (int i : all) {
         if (Items.name(i).toLowerCase().contains("pickaxe")) {
            expected++;
         }
      }

      eq(5, expected, "five pickaxes exist");
      eq(expected, pick.length, "the search finds them all (and in any case)");
      for (int i : pick) {
         check(Items.name(i).toLowerCase().contains("pickaxe"), Items.name(i) + " matches");
      }

      int[] iron = Creative.search("iron pick");
      eq(1, iron.length, "'iron pick' finds one item");
      eq("Iron Pickaxe", Items.name(iron[0]), "... the iron pickaxe");
      check(Arrays.equals(Creative.search("pickaxe iron"), Creative.search("iron pickaxe")), "the order of the words does not matter");
      eq(0, Creative.search("zzzz").length, "nothing is found for nonsense");
      check(Creative.search("wool").length >= 16, "all the wool");
      int[] stone = Creative.search("stone");
      check(stone.length > 10, "many stone things");
      for (int i = 1; i < stone.length; i++) {
         check(indexOf(all, stone[i - 1]) < indexOf(all, stone[i]), "the hits keep the order of the list");
      }
   }

   static Game game(int w, int h) {
      GuiRaster r = new GuiRaster(w * 3, h * 3);
      return TestsUi.game(r, w, h);
   }

   static void menu() {
      Game g = game(343, 240);
      g.creativeTab = 0;
      Menu m = new Menu(g, Menu.CREATIVE, null);
      eq(0, m.tab, "opens on the first tab");
      check(Arrays.equals(Creative.tab(0), m.palette), "... showing its items");
      eq(Math.max(1, (Creative.tab(0).length + 44) / 45), m.pages(), "a page holds 45 blocks (the first tab has " + Creative.tab(0).length + ")");
      m.setTab(Creative.COLORS);
      eq(Creative.COLORS, g.creativeTab, "the tab is remembered");
      check(Creative.tab(Creative.COLORS).length > 90, "the colour tab has more than two pages of blocks");
      eq((Creative.tab(Creative.COLORS).length + 44) / 45, m.pages(), "the colour blocks need as many pages as they fill (" + Creative.tab(Creative.COLORS).length + ")");
      m.page = 2;
      m.setTab(Creative.NATURE);
      eq(0, m.page, "a new tab starts on its first page");
      // typing only works in the search tab
      m.typed('x');
      eq(0, m.search.length(), "no typing outside the search tab");
      m.setTab(Creative.SEARCH);
      eq(Creative.NATURE, g.creativeTab, "the search tab is not remembered (the one before it is)");
      eq(Blocks.creativeList().length, m.palette.length, "an empty search lists everything");
      m.typed('P');
      m.typed('i');
      m.typed('c');
      m.typed('k');
      eq("Pick", m.search.toString(), "typed text");
      check(Arrays.equals(Creative.search("pick"), m.palette), "the list follows the text");
      eq(5, m.palette.length, "five pickaxes");
      m.typed((char)7);
      eq(4, m.search.length(), "control characters are ignored");
      m.backspace();
      eq("Pic", m.search.toString(), "backspace");
      m.backspace();
      m.backspace();
      m.backspace();
      m.backspace();
      eq(0, m.search.length(), "backspace on nothing is harmless");
      eq(Blocks.creativeList().length, m.palette.length, "everything again");
      for (int i = 0; i < 40; i++) {
         m.typed('a');
      }

      eq(24, m.search.length(), "the search text has a limit");
      eq(0, m.palette.length, "nothing is called 'aaaa...'");
      eq(1, m.pages(), "an empty list still has one page");
      // the page stays valid when the list shrinks
      m.setTab(Creative.COLORS);
      m.page = 2;
      m.setTab(Creative.SEARCH);
      check(m.page == 0, "the search starts on page one");
      // the letter of the key that opened the inventory is not typed into the search (the game checks the time)
      Menu fresh = new Menu(g, Menu.CREATIVE, null);
      check(System.currentTimeMillis() - fresh.openedAt < 5000, "the menu knows when it was opened");
      // the next creative menu opens on the tab that was open last
      m.setTab(Creative.TOOLS);
      Menu again = new Menu(g, Menu.CREATIVE, null);
      eq(Creative.TOOLS, again.tab, "the creative menu opens on the last tab");
      // survival menus have no tabs
      Menu surv = new Menu(g, Menu.PLAYER, null);
      eq(-1, surv.tabAt(5, 5, 0, 0), "no tabs in the survival inventory");
   }

   static void clicks() {
      for (int[] sz : TestsUi.SIZES) {
         Game g = game(sz[0], sz[1]);
         g.creativeTab = 0;
         g.menu = new Menu(g, Menu.CREATIVE, null);
         g.screen = Game.S.INV;
         float ox = g.menuOx();
         float oy = g.menuOy();
         check(oy - Menu.TAB_H - 2 >= 0, "the tabs are on the screen at " + sz[0] + "x" + sz[1]);
         check(oy + g.menu.h + 12 <= sz[1], "the panel and the hint are on the screen at " + sz[0] + "x" + sz[1]);
         for (int i = 0; i < Creative.TABS; i++) {
            eq(i, g.menu.tabAt(ox + i * Menu.TAB_W + 12, oy - 10, ox, oy), "the middle of tab " + i);
            eq(i, g.menu.tabAt(ox + i * Menu.TAB_W + 1, oy - 20, ox, oy), "the left corner of tab " + i);
         }

         eq(-1, g.menu.tabAt(ox - 2, oy - 10, ox, oy), "left of the tabs");
         eq(-1, g.menu.tabAt(ox + Creative.TABS * Menu.TAB_W + 2, oy - 10, ox, oy), "right of the tabs");
         eq(-1, g.menu.tabAt(ox + 10, oy + 10, ox, oy), "inside the panel");
         eq(-1, g.menu.tabAt(ox + 10, oy - Menu.TAB_H - 5, ox, oy), "above the tabs");
         g.menuClick(ox + 3 * Menu.TAB_W + 12, oy - 10, 0);
         eq(3, g.menu.tab, "a click on the fourth tab opens it");
         g.menuClick(ox + 7 * Menu.TAB_W + 12, oy - 10, 0);
         eq(Creative.SEARCH, g.menu.tab, "a click on the last tab opens the search");
         eq(Blocks.creativeList().length, g.menu.palette.length, "the search shows everything at first");
         g.menuClick(ox + 5 * Menu.TAB_W + 12, oy - 10, 1);
         eq(Creative.SEARCH, g.menu.tab, "only the left button switches tabs");
         // a click on a tab never picks something up
         eq(null, g.cursor, "nothing is on the cursor");
      }
   }

   static void keys() throws Exception {
      Game g = GameTests.newGame(0);
      g.gui = new GuiRaster(343 * 3, 240 * 3);
      ((GuiRaster)g.gui).begin(343 * 3, 240 * 3, 3);
      g.winW = 343 * 3;
      g.winH = 240 * 3;
      g.fbW = 343 * 3;
      g.fbH = 240 * 3;
      g.menu = new Menu(g, Menu.PLAYER, null);
      g.screen = Game.S.INV;
      float ox = g.menuOx();
      float oy = g.menuOy();
      g.inv[9] = new Stack(Blocks.STONE, 20);
      g.inv[2] = new Stack(Blocks.DIRT, 5);
      Menu.Slot s9 = slot(g.menu, g.inv, 9);
      // 3: the hotbar slot 3 is empty: the stack moves there
      g.menu.hotbarKey(3, ox + s9.x + 9, oy + s9.y + 9, ox, oy);
      eq(null, g.inv[9], "the stack left the inventory slot");
      eq(Blocks.STONE, g.inv[3].id, "... and is in the hotbar slot");
      eq(20, g.inv[3].n, "... whole");
      // 2: swap with the dirt
      g.inv[10] = new Stack(Blocks.SAND, 7);
      Menu.Slot s10 = slot(g.menu, g.inv, 10);
      g.menu.hotbarKey(2, ox + s10.x + 9, oy + s10.y + 9, ox, oy);
      eq(Blocks.SAND, g.inv[2].id, "swapped: the sand is in the hotbar");
      eq(Blocks.DIRT, g.inv[10].id, "swapped: the dirt is in the inventory");
      eq(5, g.inv[10].n, "... with its count");
      // a hotbar slot with its own number does nothing
      Menu.Slot s2 = slot(g.menu, g.inv, 2);
      g.menu.hotbarKey(2, ox + s2.x + 9, oy + s2.y + 9, ox, oy);
      eq(Blocks.SAND, g.inv[2].id, "a slot swapped with itself stays");
      // armor slots take only armor
      Menu.Slot head = null;
      for (Menu.Slot s : g.menu.slots) {
         if (s.type == 2 && s.armorPiece == 0) {
            head = s;
         }
      }

      check(head != null, "there is a helmet slot");
      g.menu.hotbarKey(2, ox + head.x + 9, oy + head.y + 9, ox, oy);
      eq(null, g.armor[0], "number keys leave the armor slots alone");
      eq(Blocks.SAND, g.inv[2].id, "... and the hotbar too");
      // Q drops one, Ctrl+Q the whole stack
      int before = g.world.items.size();
      g.player.x = 0;
      g.player.z = 0;
      g.menu.dropKey(ox + s10.x + 9, oy + s10.y + 9, ox, oy, false);
      eq(4, g.inv[10].n, "Q drops one");
      eq(before + 1, g.world.items.size(), "... which lies in the world");
      g.menu.dropKey(ox + s10.x + 9, oy + s10.y + 9, ox, oy, true);
      eq(null, g.inv[10], "Ctrl+Q drops the whole stack");
      g.menu.dropKey(ox + s10.x + 9, oy + s10.y + 9, ox, oy, false);
      eq(null, g.inv[10], "an empty slot drops nothing");
      // the creative palette: the item goes into the hotbar slot as a full stack
      g.creativeTab = 0;
      g.menu = new Menu(g, Menu.CREATIVE, null);
      ox = g.menuOx();
      oy = g.menuOy();
      Menu.Slot first = null;
      for (Menu.Slot s : g.menu.slots) {
         if (s.type == 5 && s.idx == 0) {
            first = s;
         }
      }

      g.menu.hotbarKey(5, ox + first.x + 9, oy + first.y + 9, ox, oy);
      eq(g.menu.palette[0], g.inv[5].id, "creative: the first item of the tab");
      eq(g.inv[5].max(), g.inv[5].n, "... as a full stack");
   }

   static Menu.Slot slot(Menu m, Stack[] arr, int idx) {
      for (Menu.Slot s : m.slots) {
         if (s.arr == arr && s.idx == idx) {
            return s;
         }
      }

      throw new IllegalStateException("no slot " + idx);
   }

   static boolean has(List<String> lines, String part) {
      for (String l : lines) {
         if (Gui.stripCodes(l).contains(part)) {
            return true;
         }
      }

      return false;
   }

   static void tooltips() {
      Game g = game(343, 240);
      List<String> t = g.tooltipText(new Stack(id("Iron Pickaxe"), 1, 40));
      eq("Iron Pickaxe", Gui.stripCodes(t.get(0)), "the name comes first");
      eq("Tool", Gui.stripCodes(t.get(1)), "then what it is");
      check(has(t, "Durability: 210 / 250"), "the durability left: " + t);
      check(has(t, "Attack Damage"), "the damage: " + t);
      check(has(t, "Mines stone and ores"), "what a pickaxe is for: " + t);
      t = g.tooltipText(new Stack(id("Iron Ore"), 1));
      check(has(t, "Smelts into Iron Ingot"), "ore smelts: " + t);
      eq("Block", Gui.stripCodes(t.get(1)), "ore is a block");
      t = g.tooltipText(new Stack(id("Coal"), 1));
      check(has(t, "Fuel: smelts 8 items"), "coal burns: " + t);
      t = g.tooltipText(new Stack(id("Bread"), 1));
      check(has(t, "Restores 2.5 hunger"), "bread feeds: " + t);
      eq("Food", Gui.stripCodes(t.get(1)), "bread is food");
      t = g.tooltipText(new Stack(id("Iron Chestplate"), 1, 0));
      check(has(t, "Armor") && has(t, "Chest"), "armor says where it goes: " + t);
      eq("Armor", Gui.stripCodes(t.get(1)), "armor is armor");
      t = g.tooltipText(new Stack(id("Wooden Hoe"), 1, 0));
      check(has(t, "Turns dirt into farmland"), "hoes: " + t);
      t = g.tooltipText(new Stack(id("Stone"), 1));
      check(!has(t, "id "), "no ids without F3");
      g.debug = true;
      t = g.tooltipText(new Stack(id("Stone"), 1));
      check(has(t, "id " + Blocks.STONE), "the id with F3: " + t);
      for (int i : Blocks.creativeList()) {
         ArrayList<String> lines = g.tooltipText(new Stack(i, 1));
         check(lines.size() >= 2 && lines.size() <= 8, "a sensible number of lines for " + Items.name(i) + ": " + lines);
         eq(Items.name(i), Gui.stripCodes(lines.get(0)), "the name of " + i);
      }
   }
}
