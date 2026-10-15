package bc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public final class Recipes {
   static final ArrayList<Recipes.Recipe> list = new ArrayList();
   static final HashMap<Integer, Stack> smelt = new HashMap();
   static final HashMap<Integer, Integer> fuel = new HashMap();
   static final int ANY_LOG = -1;
   static final int ANY_WOOL = -3;

   static boolean matchId(int var0, int var1) {
      if (var0 == 0) {
         return var1 == 0;
      } else if (var0 == -1) {
         return Blocks.isLog(var1);
      } else if (var0 == 5) {
         return Blocks313.isPlanks(var1); // Update 3.13: every kind of planks counts as planks
      } else {
         return var0 != -3 ? var0 == var1 : var1 >= 34 && var1 < 50;
      }
   }

   static void shaped(int var0, int var1, String[] var2, Object... var3) {
      HashMap var4 = new HashMap();

      for (int var5 = 0; var5 < var3.length; var5 += 2) {
         var4.put((Character)var3[var5], (Integer)var3[var5 + 1]);
      }

      Recipes.Recipe var9 = new Recipes.Recipe();
      var9.h = var2.length;
      var9.w = var2[0].length();
      var9.grid = new int[var9.w * var9.h];

      for (int var6 = 0; var6 < var9.h; var6++) {
         for (int var7 = 0; var7 < var9.w; var7++) {
            char var8 = var2[var6].charAt(var7);
            var9.grid[var6 * var9.w + var7] = var8 == ' ' ? 0 : (Integer)var4.get(var8);
         }
      }

      var9.outId = var0;
      var9.outN = var1;
      list.add(var9);
   }

   static void shapeless(int var0, int var1, int... var2) {
      Recipes.Recipe var3 = new Recipes.Recipe();
      var3.shapeless = true;
      var3.grid = var2;
      var3.w = var3.h = 0;
      var3.outId = var0;
      var3.outN = var1;
      list.add(var3);
   }

   /** What stays behind in a crafting-grid slot when one of this item is used up as an ingredient (milk bucket -> bucket). 0 = nothing. */
   public static int leftover(int id) {
      return id == Items.MILK_BUCKET ? Items.BUCKET : 0;
   }

   public static int fuelTime(Stack var0) {
      if (var0 == null) {
         return 0;
      }

      Integer var1 = (Integer)fuel.get(var0.id);
      return var1 == null ? 0 : var1;
   }

   public static Stack smeltResult(Stack var0) {
      if (var0 == null) {
         return null;
      }

      Stack var1 = (Stack)smelt.get(var0.id);
      return var1 == null ? null : var1.copy();
   }

   /**
    * Finds the recipe matching a crafting grid (gw x gh, row-major). Shaped recipes match on the
    * trimmed bounding box of the filled cells; shapeless recipes match when every ingredient is
    * used exactly once. (Rewritten by hand: the decompiler mangled this method's control flow.)
    */
   public static Stack find(Stack[] grid, int gw, int gh) {
      int minX = gw;
      int minY = gh;
      int maxX = -1;
      int maxY = -1;
      int count = 0;

      for (int y = 0; y < gh; y++) {
         for (int x = 0; x < gw; x++) {
            if (grid[y * gw + x] != null) {
               minX = Math.min(minX, x);
               minY = Math.min(minY, y);
               maxX = Math.max(maxX, x);
               maxY = Math.max(maxY, y);
               count++;
            }
         }
      }

      if (count == 0) {
         return null;
      }

      Stack colored = Color312.dyeRecipe(grid, gw, gh); // Update 3.12: the recipes that work for every dye colour
      if (colored != null) {
         return colored;
      }

      Stack wooden = Shapes.special(grid, gw, gh); // Update 3.15: fences, gates and trapdoors of the wood the planks are
      if (wooden != null) {
         return wooden;
      }

      Stack decor = Decor315.special(grid, gw, gh); // beds and banners in the colour of the wool
      if (decor != null) {
         return decor;
      }

      int w = maxX - minX + 1;
      int h = maxY - minY + 1;

      for (Recipes.Recipe r : list) {
         if (!r.shapeless) {
            if (r.w == w && r.h == h) {
               boolean ok = true;

               for (int y = 0; y < h && ok; y++) {
                  for (int x = 0; x < w && ok; x++) {
                     Stack s = grid[(minY + y) * gw + minX + x];
                     if (!matchId(r.grid[y * w + x], s == null ? 0 : s.id)) {
                        ok = false;
                     }
                  }
               }

               if (ok) {
                  return new Stack(r.outId, r.outN);
               }
            }
            continue;
         }

         if (count == r.grid.length) {
            boolean[] used = new boolean[r.grid.length];
            boolean ok = true;

            for (int y = 0; y < gh && ok; y++) {
               for (int x = 0; x < gw && ok; x++) {
                  Stack s = grid[y * gw + x];
                  if (s != null) {
                     boolean found = false;

                     for (int k = 0; k < r.grid.length; k++) {
                        if (!used[k] && matchId(r.grid[k], s.id)) {
                           used[k] = true;
                           found = true;
                           break;
                        }
                     }

                     if (!found) {
                        ok = false;
                     }
                  }
               }
            }

            if (ok) {
               return new Stack(r.outId, r.outN);
            }
         }
      }

      return null;
   }

   static {
      int var0 = 5;
      int var1 = 256;
      int var2 = 4;
      for (int sp = 0; sp < 6; sp++) {
         shapeless(Blocks313.plank(sp), 4, Blocks.WOOD_LOG[sp]); // Update 3.13: a log gives the planks of its own wood
      }

      shapeless(5, 4, -1);
      shaped(256, 4, new String[]{"P", "P"}, 'P', Integer.valueOf(var0));
      shaped(66, 1, new String[]{"PP", "PP"}, 'P', Integer.valueOf(var0));
      shaped(69, 1, new String[]{"PPP", "P P", "PPP"}, 'P', Integer.valueOf(var0));
      shaped(281, 3, new String[]{"PP", "PP", "PP"}, 'P', Integer.valueOf(var0));
      shaped(67, 1, new String[]{"CCC", "C C", "CCC"}, 'C', Integer.valueOf(var2));
      shaped(32, 4, new String[]{"C", "S"}, 'C', 257, 'S', Integer.valueOf(var1));
      shaped(27, 4, new String[]{"SS", "SS"}, 'S', 1);
      shaped(17, 1, new String[]{"SS", "SS"}, 'S', 7);
      shaped(16, 1, new String[]{"SS", "SS"}, 'S', 27);
      shaped(25, 1, new String[]{"SS", "SS"}, 'S', 265);
      shaped(34, 1, new String[]{"SS", "SS"}, 'S', 263);

      for (int var3 = 0; var3 < 5; var3++) {
         int var4 = Blocks.STAIR_MAT[var3];
         shaped(93 + var3 * 4, 4, new String[]{"M  ", "MM ", "MMM"}, 'M', var4);
         shaped(113 + var3, 6, new String[]{"MMM"}, 'M', var4);
      }

      shaped(118, 3, new String[]{"PSP", "PSP"}, 'P', Integer.valueOf(var0), 'S', Integer.valueOf(var1));
      shaped(119, 3, new String[]{"S S", "SSS", "S S"}, 'S', Integer.valueOf(var1));
      shaped(132, 1, new String[]{"GXG", "XGX", "GXG"}, 'G', 265, 'X', 7);
      shaped(289, 1, new String[]{"WWW", "PPP"}, 'W', -3, 'P', Integer.valueOf(var0));
      shaped(282, 1, new String[]{"I I", " I "}, 'I', 258);
      shapeless(288, 1, 258, 266);
      shaped(287, 1, new String[]{"WWW"}, 'W', 286);
      shaped(268, 1, new String[]{" SX", "S X", " SX"}, 'S', Integer.valueOf(var1), 'X', 263);
      shaped(267, 4, new String[]{"F", "S", "L"}, 'F', 266, 'S', Integer.valueOf(var1), 'L', 262);
      int[] var9 = new int[]{5, 4, 258, 259, 260};

      for (int var10 = 0; var10 < 5; var10++) {
         int var5 = var9[var10];
         shaped(Items.toolId(var10, 4), 1, new String[]{"M", "M", "S"}, 'M', var5, 'S', Integer.valueOf(var1));
         shaped(Items.toolId(var10, 1), 1, new String[]{"MMM", " S ", " S "}, 'M', var5, 'S', Integer.valueOf(var1));
         shaped(Items.toolId(var10, 2), 1, new String[]{"MM", "MS", " S"}, 'M', var5, 'S', Integer.valueOf(var1));
         shaped(Items.toolId(var10, 3), 1, new String[]{"M", "S", "S"}, 'M', var5, 'S', Integer.valueOf(var1));
         shaped(290 + var10, 1, new String[]{"MM", " S", " S"}, 'M', var5, 'S', Integer.valueOf(var1));
         shaped(290 + var10, 1, new String[]{"MM", "S ", "S "}, 'M', var5, 'S', Integer.valueOf(var1));
      }

      int[] var11 = new int[]{261, 258, 259, 260};

      for (int var12 = 0; var12 < 4; var12++) {
         int var6 = var11[var12];
         shaped(Items.armorId(var12, 0), 1, new String[]{"MMM", "M M"}, 'M', var6);
         shaped(Items.armorId(var12, 1), 1, new String[]{"M M", "MMM", "MMM"}, 'M', var6);
         shaped(Items.armorId(var12, 2), 1, new String[]{"MMM", "M M", "M M"}, 'M', var6);
         shaped(Items.armorId(var12, 3), 1, new String[]{"M M", "M M"}, 'M', var6);
      }

      // ---- Update 3.6 ----
      shapeless(339, 3, 264);
      shaped(340, 1, new String[]{" I", "I "}, 'I', 258);
      shaped(340, 1, new String[]{"I ", " I"}, 'I', 258);
      shaped(158, 1, new String[]{"WWW", "WWW", "WWW"}, 'W', 286);
      shapeless(286, 9, 158);
      // ---- Update 3.7 ----
      shapeless(Items.SUGAR, 1, Blocks.SUGAR_CANE);
      shaped(Items.PAPER, 3, new String[]{"CCC"}, 'C', Blocks.SUGAR_CANE);
      shapeless(Items.BOOK, 1, Items.PAPER, Items.PAPER, Items.PAPER, Items.LEATHER);
      shaped(Blocks.BOOKSHELF, 1, new String[]{"PPP", "BBB", "PPP"}, 'P', Integer.valueOf(var0), 'B', Items.BOOK);
      shapeless(Items.PUMPKIN_SEEDS, 4, Blocks.PUMPKIN);
      shapeless(Items.MELON_SEEDS, 1, Items.MELON_SLICE);
      shaped(Blocks.MELON, 1, new String[]{"MMM", "MMM", "MMM"}, 'M', Items.MELON_SLICE);
      shapeless(Items.PUMPKIN_PIE, 1, Blocks.PUMPKIN, Items.SUGAR, Items.EGG);
      shaped(Blocks.JACK_O_LANTERN + 1, 1, new String[]{"P", "T"}, 'P', Blocks.CARVED_PUMPKIN + 1, 'T', Blocks.TORCH);
      shaped(Items.CAKE, 1, new String[]{"MMM", "SES", "WWW"}, 'M', Items.MILK_BUCKET, 'S', Items.SUGAR, 'E', Items.EGG, 'W', Items.WHEAT);
      smelt.put(337, new Stack(338, 1));
      smelt.put(20, new Stack(258, 1));
      smelt.put(21, new Stack(259, 1));
      smelt.put(7, new Stack(15, 1));
      smelt.put(4, new Stack(1, 1));
      smelt.put(27, new Stack(1, 1));
      smelt.put(272, new Stack(273, 1));
      smelt.put(274, new Stack(275, 1));
      smelt.put(276, new Stack(277, 1));
      smelt.put(278, new Stack(279, 1));
      smelt.put(9, new Stack(257, 1));
      smelt.put(10, new Stack(257, 1));
      smelt.put(11, new Stack(257, 1));
      fuel.put(257, 1600);

      for (int var8 : new int[]{9, 10, 11, 5, 66, 69}) {
         fuel.put(var8, 300);
      }

      // ---- Update 3.13 ----
      for (int sp = 3; sp < 6; sp++) {
         smelt.put(Blocks.WOOD_LOG[sp], new Stack(257, 1));
         fuel.put(Blocks.WOOD_LOG[sp], 300);
      }

      for (int sp = 1; sp < 6; sp++) {
         fuel.put(Blocks313.plank(sp), 300);
      }

      shaped(Blocks313.DIORITE, 2, new String[]{"CQ", "QC"}, 'C', Blocks.COBBLE, 'Q', Items.QUARTZ);
      shapeless(Blocks313.GRANITE, 2, Blocks313.DIORITE, Items.QUARTZ);
      shapeless(Blocks313.ANDESITE, 2, Blocks313.DIORITE, Blocks.COBBLE);
      shaped(Blocks313.RED_SANDSTONE, 1, new String[]{"SS", "SS"}, 'S', Blocks313.RED_SAND);
      shapeless(Blocks313.PACKED_ICE, 1, Blocks313.ICE, Blocks313.ICE, Blocks313.ICE, Blocks313.ICE);

      fuel.put(256, 100);
      fuel.put(268, 300);

      for (int var14 = 0; var14 < 4; var14++) {
         fuel.put(Items.toolId(0, var14 == 0 ? 4 : var14), 200);
      }

      // ---- Update 3.12 ----
      Items312.recipes();
      Shapes.recipes();
      Decor315.recipes();
      Redstone319.recipes();
      Circuit.recipes();
      RasterComputer.recipes();
   }

   static final class Recipe {
      int w;
      int h;
      int[][] need;
      int[] grid;
      boolean shapeless;
      int outId;
      int outN;
   }
}
