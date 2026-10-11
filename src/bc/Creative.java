package bc;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Update 3.13: the creative inventory is sorted into tabs (Building Blocks, Colors, Nature, ...) and can be searched, instead of seven anonymous pages.
 * The tab of an item follows from what it is (its name and its properties), so new blocks and items land in a sensible tab on their own: a block that
 * no rule knows goes to Building Blocks, an item to Materials.
 */
final class Creative {
   static final int BUILDING = 0;
   static final int COLORS = 1;
   static final int NATURE = 2;
   static final int UTILITY = 3;
   static final int TOOLS = 4;
   static final int FOOD = 5;
   static final int MATERIALS = 6;
   static final int SEARCH = 7;
   static final int TABS = 8;
   static final String[] NAME = {
      "Building Blocks", "Colors", "Nature", "Decoration and Utility", "Tools and Combat", "Food and Farming", "Materials", "Search Items"
   };
   /** The picture on each tab (a block or an item; the search tab draws a magnifying glass instead). */
   static final int[] ICON = {16, 48, 2, 66, 309, 270, 260, 0};
   private static int[][] cache;
   private static int cacheFor = -1;

   private Creative() {
   }

   private static boolean has(String n, String... parts) {
      for (String p : parts) {
         if (n.contains(p)) {
            return true;
         }
      }

      return false;
   }

   /** The colour families: wool, concrete, terracotta, stained glass and the dyes. Returns the order inside the tab, or -1. */
   private static int colorGroup(String n) {
      if (n.endsWith(" wool")) {
         return 0;
      }

      if (n.endsWith("concrete")) {
         return 1;
      }

      if (n.endsWith("concrete powder")) {
         return 2;
      }

      if (n.endsWith("glazed terracotta")) {
         return 4;
      }

      if (n.endsWith("terracotta")) {
         return 3;
      }

      if (n.endsWith("stained glass")) {
         return 5;
      }

      if (n.endsWith(" dye")) {
         return 6;
      }

      if (n.endsWith(" carpet")) {
         return 7;
      }

      if (n.endsWith("stained glass pane")) {
         return 8;
      }

      return -1;
   }

   static int tabOf(int id) {
      if (Redstone319.isRedstoneUtility(id)) {
         return UTILITY;
      }

      String n = Items.name(id).toLowerCase(Locale.ROOT);
      if (colorGroup(n) >= 0) {
         return COLORS;
      }

      if (Items.isTool(id) || Items.isArmor(id) || Items.isHoe(id) || n.equals("bow") || n.equals("arrow") || n.equals("shears") || n.equals("flint and steel")
         || n.endsWith(" bucket") || n.equals("bucket") || n.equals("fishing rod") || n.equals("shield") || n.equals("compass") || n.equals("clock")
         || n.equals("fire charge")) {
         return TOOLS;
      }

      if (Items.isFood(id) || has(n, "seeds") || n.equals("wheat") || n.equals("bone meal") || n.equals("cake") || n.equals("sugar") || n.equals("egg")) {
         return FOOD;
      }

      boolean block = !Items.isItem(id);
      if (!block) {
         if (has(n, "door", "bed", "boat", "minecart", "sign", "banner", "painting", "frame")) {
            return UTILITY;
         }

         return MATERIALS;
      }

      if (has(n, " log", " wood") && !has(n, "stairs", "slab", "fence")) {
         return NATURE;
      }

      if (has(n, "leaves", " ore", "sapling", "poppy", "dandelion", "orchid", "allium", "bluet", "tulip", "daisy", "cornflower", "lily", "mushroom", "cactus",
         "sugar cane", "vines", "fern", "dead bush", "tall grass", "sunflower", "lilac", "rose bush", "peony", "bedrock")
         || n.equals("grass") || n.equals("grass block") || n.equals("dirt") || n.equals("coarse dirt") || n.equals("podzol") || n.equals("mycelium") || n.equals("sand")
         || n.equals("red sand") || n.equals("gravel") || n.equals("clay") || n.equals("snow") || n.equals("snow block") || n.equals("ice") || n.equals("packed ice")
         || n.equals("water") || n.equals("lava") || n.equals("pumpkin") || n.equals("melon") || n.equals("vine")) {
         return NATURE;
      }

      if (has(n, "torch", "lantern", "ladder", "fence", "door", "bed", "chest", "crafting table", "furnace", "bookshelf", "spawner", "tnt", "hay bale", "portal",
         "dragon egg", "glowstone", "carved pumpkin", "sign", "banner", "anvil", "jukebox", "note block", "enchanting", "brewing")) {
         return UTILITY;
      }

      return BUILDING;
   }

   /** Where an item stands inside its tab (a stable sort: the original order of the list decides inside a group). */
   private static int groupOf(int tab, int id) {
      String n = Items.name(id).toLowerCase(Locale.ROOT);
      switch (tab) {
         case COLORS:
            return colorGroup(n);
         case BUILDING:
            if (has(n, "stairs")) {
               return 8;
            }

            if (has(n, "slab")) {
               return 9;
            }

            if (n.equals("stone") || n.equals("granite") || n.equals("diorite") || n.equals("andesite")) {
               return 0;
            }

            if (has(n, "cobblestone")) {
               return 1;
            }

            if (has(n, "planks")) {
               return 2;
            }

            if (has(n, "bricks")) {
               return 3;
            }

            if (has(n, "sandstone")) {
               return 4;
            }

            if (has(n, "glass")) {
               return 5;
            }

            if (has(n, "block of", "quartz", "lapis")) {
               return 6;
            }

            return 7;
         case NATURE:
            if (has(n, " log", " wood")) {
               return 1;
            }

            if (has(n, "leaves")) {
               return 2;
            }

            if (has(n, " ore")) {
               return 3;
            }

            if (n.equals("water") || n.equals("lava")) {
               return 6;
            }

            if (n.equals("grass block") || n.equals("dirt") || n.equals("coarse dirt") || n.equals("podzol") || n.equals("mycelium") || n.equals("sand")
               || n.equals("red sand") || n.equals("gravel") || n.equals("clay") || n.equals("snow") || n.equals("snow block") || n.equals("ice") || n.equals("packed ice")
               || n.equals("bedrock")) {
               return 0;
            }

            return 4;
         case UTILITY:
            return Redstone319.isRedstoneUtility(id) ? 1 : 0;
         default:
            return 0;
      }
   }

   /** The items of one tab, in a sensible order. */
   static int[] tab(int tab) {
      build();
      return ModLoader.filterPalette(cache[Math.max(0, Math.min(SEARCH - 1, tab))]);
   }

   private static synchronized void build() {
      int[] all = Blocks.creativeList();
      if (cache != null && cacheFor == all.length) {
         return;
      }

      @SuppressWarnings("unchecked")
      ArrayList<int[]>[] lists = new ArrayList[SEARCH];
      for (int t = 0; t < SEARCH; t++) {
         lists[t] = new ArrayList<>();
      }

      for (int i = 0; i < all.length; i++) {
         int t = tabOf(all[i]);
         lists[t].add(new int[]{all[i], groupOf(t, all[i]), i});
      }

      int[][] out = new int[SEARCH][];
      for (int t = 0; t < SEARCH; t++) {
         lists[t].sort((a, b) -> a[1] != b[1] ? Integer.compare(a[1], b[1]) : Integer.compare(a[2], b[2]));
         out[t] = new int[lists[t].size()];
         for (int i = 0; i < out[t].length; i++) {
            out[t][i] = lists[t].get(i)[0];
         }
      }

      cache = out;
      cacheFor = all.length;
   }

   /** Everything whose name contains all the words typed (any case); an empty text finds everything. */
   static int[] search(String query) {
      int[] all = ModLoader.filterPalette(Blocks.creativeList());
      String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
      if (q.isEmpty()) {
         return all;
      }

      String[] words = q.split("\\s+");
      ArrayList<Integer> hits = new ArrayList<>();
      for (int id : all) {
         String n = Items.name(id).toLowerCase(Locale.ROOT);
         boolean ok = true;
         for (String w : words) {
            ok &= n.contains(w);
         }

         if (ok) {
            hits.add(id);
         }
      }

      int[] out = new int[hits.size()];
      for (int i = 0; i < out.length; i++) {
         out[i] = hits.get(i);
      }

      return out;
   }

   /** A short word for what an item is (shown under its name in the tooltip). */
   static String kind(int id) {
      if (Redstone319.isRedstoneUtility(id)) {
         return "Redstone";
      }

      String n = Items.name(id).toLowerCase(Locale.ROOT);
      if (Items.isArmor(id)) {
         return "Armor";
      }

      if (Items.isHoe(id) || id >= 300 && id < 320 && (id - 300 & 3) != 0 || n.equals("shears") || n.equals("flint and steel")) {
         return "Tool";
      }

      if (Items.isTool(id)) {
         return "Weapon";
      }

      if (n.equals("bow") || n.equals("arrow")) {
         return "Weapon";
      }

      if (n.endsWith("bucket")) {
         return "Bucket";
      }

      if (Items.isFood(id)) {
         return "Food";
      }

      if (n.endsWith(" dye")) {
         return "Dye";
      }

      if (!Items.isItem(id)) {
         return Blocks.isFluid(id) ? "Fluid" : "Block";
      }

      if (has(n, "seeds") || Items.plantBlock(id) != 0) {
         return "Seeds";
      }

      return "Material";
   }
}
