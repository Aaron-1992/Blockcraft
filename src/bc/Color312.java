package bc;

import java.util.Random;

/**
 * Update 3.12, "the World of Color" of Minecraft 1.12: the 15 dyes (bone meal is the white one), concrete and concrete powder (the
 * powder falls like sand and turns to concrete next to water), terracotta, stained and glazed terracotta, stained glass (translucent),
 * clay, the blocks of metal and gems, and emerald ore. Sheep take a dye. The colour index is always the one of the wool:
 * 0 white, 1 orange, 2 magenta, 3 light blue, 4 yellow, 5 lime, 6 pink, 7 gray, 8 light gray, 9 cyan, 10 purple, 11 blue, 12 brown,
 * 13 green, 14 red, 15 black.
 */
final class Color312 {
   private Color312() {
   }

   static final String[] NAME = {
      "White", "Orange", "Magenta", "Light Blue", "Yellow", "Lime", "Pink", "Gray", "Light Gray", "Cyan", "Purple", "Blue", "Brown", "Green", "Red", "Black"
   };
   /** The colours of concrete (as in Minecraft). */
   static final int[] CONCRETE = {
      0xCFD5D6, 0xE06100, 0xA9309F, 0x2489C7, 0xF1AF15, 0x5EA818, 0xD5658E, 0x36393D, 0x7D7D73, 0x157788, 0x64209C, 0x2D2F8F, 0x603C20, 0x495B24, 0x8E2121, 0x080A0F
   };
   /** The colours of stained terracotta. */
   static final int[] TERRA = {
      0xD1B2A1, 0xA15325, 0x95576C, 0x716C89, 0xBA8524, 0x677535, 0xA14E4E, 0x392A23, 0x876B62, 0x575B5B, 0x764656, 0x4A3B5B, 0x4D3324, 0x4C532A, 0x8F3D2E, 0x251610
   };
   static final int PLAIN_TERRA = 0x985E43;

   // ---- the atlas tiles (item pictures use 272.., see TexArt312.ITEM0; these start at 400 so that there is room for 128 more items) ----
   static final int T_CONCRETE0 = 400;
   static final int T_POWDER0 = 416;
   static final int T_TERRACOTTA = 432;
   static final int T_STAINED0 = 433;
   static final int T_GLAZED0 = 449;
   static final int T_CLAY = 465;
   static final int T_GLASS0 = 466;
   static final int T_IRON = 482;
   static final int T_GOLD = 483;
   static final int T_DIAMOND = 484;
   static final int T_EMERALD = 485;
   static final int T_COAL = 486;
   static final int T_LAPIS = 487;
   static final int T_EMERALD_ORE = 488;

   /** What a stained glass pane lets through (0 = clear, 1 = solid colour). */
   static final float GLASS_ALPHA = 0.7F;

   // ------------------------------------------------------------------ small helpers
   static boolean isPowder(int id) {
      return id >= Blocks.POWDER0 && id < Blocks.POWDER0 + 16;
   }

   static boolean isStainedGlass(int id) {
      return id >= Blocks.GLASS0 && id < Blocks.GLASS0 + 16;
   }

   /** The concrete a block of powder turns into. */
   static int hardened(int powder) {
      return powder - Blocks.POWDER0 + Blocks.CONCRETE0;
   }

   /** The colour (0..15) of a dye item, bone meal being white; -1 for anything else. */
   static int dyeColor(int itemId) {
      if (itemId == Items.BONE_MEAL) {
         return 0;
      }

      return itemId >= Items.DYE1 && itemId < Items.DYE1 + 15 ? itemId - Items.DYE1 + 1 : -1;
   }

   static int dyeItem(int color) {
      return color == 0 ? Items.BONE_MEAL : Items.DYE1 + color - 1;
   }

   /** True when water touches the block on one of its sides or on top (not below): wet powder sets. */
   static boolean wet(World w, int x, int y, int z) {
      return Blocks.isWater(w.getBlock(x + 1, y, z)) || Blocks.isWater(w.getBlock(x - 1, y, z)) || Blocks.isWater(w.getBlock(x, y, z + 1))
         || Blocks.isWater(w.getBlock(x, y, z - 1)) || Blocks.isWater(w.getBlock(x, y + 1, z));
   }

   private static int rgb(int r, int g, int b) {
      return Tex.rgb(r, g, b);
   }

   private static int rgbOf(int c, int add) {
      return rgb((c >> 16 & 255) + add, (c >> 8 & 255) + add, (c & 255) + add);
   }

   private static int sc(int c, float f) {
      return TexExtra.sc(c, f);
   }

   private static int mix(int a, int b, float t) {
      int r = (int)((a >> 16 & 255) * (1.0F - t) + (b >> 16 & 255) * t);
      int g = (int)((a >> 8 & 255) * (1.0F - t) + (b >> 8 & 255) * t);
      int bl = (int)((a & 255) * (1.0F - t) + (b & 255) * t);
      return rgb(r, g, bl);
   }

   private static int wool(int c) {
      int[] w = Tex.WOOL[c];
      return w[0] << 16 | w[1] << 8 | w[2];
   }

   /** The colour a sheep with this fleece colour has (the dye colour). */
   static int fleece(int c) {
      return wool(c);
   }

   // ------------------------------------------------------------------ tiles
   static int[] concrete(int c) {
      Random r = new Random(1400L + c);
      int[] p = new int[256];
      for (int i = 0; i < 256; i++) {
         p[i] = rgbOf(CONCRETE[c], r.nextInt(7) - 3);
      }

      return p;
   }

   static int[] powder(int c) {
      Random r = new Random(1420L + c);
      int[] p = new int[256];
      int base = mix(CONCRETE[c], 0xC8C8C8, 0.22F);
      for (int i = 0; i < 256; i++) {
         p[i] = rgbOf(base, r.nextInt(25) - 12);
      }

      for (int k = 0; k < 14; k++) {
         p[r.nextInt(256)] = rgbOf(base, r.nextBoolean() ? 22 : -26);
      }

      return p;
   }

   static int[] terracotta(int base, long seed) {
      Random r = new Random(seed);
      int[] p = new int[256];
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            int v = r.nextInt(9) - 4;
            if ((y * 5 + x / 6 + r.nextInt(2)) % 7 == 0) {
               v -= 4;
            }

            p[y * 16 + x] = rgbOf(base, v);
         }
      }

      return p;
   }

   /** Glazed terracotta: a pattern of four kinds (diamond, cross, rings, pinwheel), each in the colour of its dye. */
   static int[] glazed(int c) {
      Random r = new Random(1440L + c);
      int[] p = new int[256];
      int main = wool(c);
      if (c == 15) {
         main = 0x2A2D34;
      }

      int dark = sc(main, 0.58F);
      int light = sc(main, 1.22F);
      int accent = c == 0 || c == 8 || c == 4 || c == 6 || c == 5 ? sc(main, 0.62F) : mix(main, 0xFFFFFF, 0.75F);
      int pat = c % 4;
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            double dx = x - 7.5;
            double dy = y - 7.5;
            double ax = Math.abs(dx);
            double ay = Math.abs(dy);
            int col;
            switch (pat) {
               case 0: {
                  double d = ax + ay;
                  col = d < 2.6 ? accent : (d < 5.5 ? light : (d < 9.0 ? main : dark));
                  break;
               }
               case 1: {
                  boolean bar = ax < 1.6 || ay < 1.6;
                  boolean corner = ax > 4.5 && ay > 4.5;
                  col = bar ? (ax < 1.6 && ay < 1.6 ? light : accent) : (corner ? dark : main);
                  break;
               }
               case 2: {
                  double d = Math.max(ax, ay);
                  col = d < 1.8 ? accent : (d < 3.8 ? main : (d < 5.8 ? light : (d < 7.2 ? dark : main)));
                  break;
               }
               default: {
                  double ang = Math.atan2(dy, dx);
                  int sector = (int)Math.floor((ang + Math.PI) / (Math.PI / 4.0));
                  col = ax < 1.4 && ay < 1.4 ? accent : (sector % 2 == 0 ? main : light);
                  if ((ax > 6.0 || ay > 6.0) && sector % 2 == 1) {
                     col = dark;
                  }
               }
            }

            int v = r.nextInt(5) - 2;
            p[y * 16 + x] = rgbOf(col, v);
         }
      }

      // a thin edge like the rim of a glazed tile
      for (int i = 0; i < 16; i++) {
         p[i] = sc(p[i], 0.8F);
         p[15 * 16 + i] = sc(p[15 * 16 + i], 0.8F);
         p[i * 16] = sc(p[i * 16], 0.8F);
         p[i * 16 + 15] = sc(p[i * 16 + 15], 0.8F);
      }

      return p;
   }

   static int[] clay() {
      Random r = new Random(1470L);
      int[] p = new int[256];
      for (int i = 0; i < 256; i++) {
         int v = r.nextInt(9) - 4;
         p[i] = rgb(160 + v, 166 + v, 178 + v);
      }

      for (int k = 0; k < 10; k++) {
         p[r.nextInt(256)] = rgb(138, 144, 158);
      }

      for (int k = 0; k < 6; k++) {
         p[r.nextInt(256)] = rgb(178, 184, 196);
      }

      return p;
   }

   /** Stained glass: the colour with a darker frame and two light streaks; the game draws it translucent. */
   static int[] glass(int c) {
      Random r = new Random(1480L + c);
      int[] p = new int[256];
      int base = wool(c);
      if (c == 15) {
         base = 0x303238;
      }

      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            int col = base;
            boolean edge = x == 0 || y == 0 || x == 15 || y == 15;
            boolean inner = x == 1 || y == 1 || x == 14 || y == 14;
            if (edge) {
               col = sc(base, 0.72F);
            } else if (inner) {
               col = sc(base, 1.12F);
            } else if ((x + y == 7 || x + y == 8) && x > 2 && x < 8) {
               col = sc(base, 1.35F);
            } else if (x + y == 11 && x > 8 && x < 12) {
               col = sc(base, 1.25F);
            }

            p[y * 16 + x] = rgbOf(col, r.nextInt(5) - 2);
         }
      }

      return p;
   }

   /** The block of a metal or gem: a bevelled square with two shiny streaks. */
   static int[] gemBlock(int base, long seed) {
      Random r = new Random(seed);
      int[] p = new int[256];
      int hi = sc(base, 1.28F);
      int lo = sc(base, 0.72F);
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            int col = sc(base, 1.0F);
            if (x == 0 || y == 0) {
               col = hi;
            } else if (x == 15 || y == 15) {
               col = lo;
            } else if (x == 1 || y == 1) {
               col = sc(base, 1.12F);
            } else if (x == 14 || y == 14) {
               col = sc(base, 0.86F);
            } else if ((x - y == 3 || x - y == 4) && x > 2 && x < 9) {
               col = sc(base, 1.3F);
            } else if (x - y == 6 && x > 9) {
               col = sc(base, 1.18F);
            }

            p[y * 16 + x] = rgbOf(col, r.nextInt(7) - 3);
         }
      }

      return p;
   }

   static int[] coalBlock() {
      Random r = new Random(1490L);
      int[] p = new int[256];
      for (int i = 0; i < 256; i++) {
         int v = r.nextInt(13) - 6;
         p[i] = rgb(24 + v, 24 + v, 26 + v);
      }

      for (int k = 0; k < 40; k++) {
         int x = r.nextInt(15);
         int y = r.nextInt(15);
         int v = 10 + r.nextInt(18);
         p[y * 16 + x] = rgb(14 + v, 14 + v, 16 + v);
         p[y * 16 + x + 1] = rgb(8, 8, 9);
      }

      return p;
   }

   static int[] lapisBlock() {
      Random r = new Random(1500L);
      int[] p = new int[256];
      for (int i = 0; i < 256; i++) {
         int v = r.nextInt(15) - 7;
         p[i] = rgb(38 + v, 66 + v, 160 + v * 2);
      }

      for (int k = 0; k < 34; k++) {
         int c = r.nextInt(3);
         p[r.nextInt(256)] = c == 0 ? rgb(120, 150, 232) : (c == 1 ? rgb(24, 40, 110) : rgb(210, 220, 250));
      }

      return p;
   }

   /** Emerald ore: the stone of the game with green gems. */
   static int[] emeraldOre() {
      int[] p = new int[256];
      int x0 = Tex.tx(1);
      int y0 = Tex.ty(1);
      for (int y = 0; y < 16; y++) {
         System.arraycopy(Tex.atlasPixels, (y0 + y) * Tex.PX + x0, p, y * 16, 16);
      }

      Random r = new Random(1510L);
      int[][] shape = {{0, 0}, {1, 0}, {0, 1}, {1, 1}};
      for (int k = 0; k < 6; k++) {
         int x = 1 + r.nextInt(12);
         int y = 1 + r.nextInt(12);
         for (int[] s : shape) {
            int v = r.nextInt(30) - 10;
            p[(y + s[1]) * 16 + x + s[0]] = rgb(24 + v / 2, 190 + v, 96 + v / 2);
         }

         p[y * 16 + x] = rgb(190, 255, 214);
      }

      return p;
   }

   /** The picture of a dye: a small heap of the colour. */
   static int[] dyeSprite(int itemId) {
      int c = itemId - Items.DYE1 + 1;
      int base = wool(c);
      if (c == 15) {
         base = 0x3A3C46;
      }

      TexExtra.Cv cv = new TexExtra.Cv();
      cv.ell(8.0, 9.6, 5.4, 3.9, sc(base, 0.55F));
      cv.ell(8.0, 9.0, 4.6, 3.2, base);
      cv.ell(6.8, 8.2, 2.4, 1.5, sc(base, 1.28F));
      cv.ell(10.2, 10.4, 2.0, 1.1, sc(base, 0.78F));
      cv.set(6, 7, mix(base, 0xFFFFFF, 0.7F));
      cv.set(7, 7, mix(base, 0xFFFFFF, 0.45F));
      cv.set(11, 8, sc(base, 1.15F));
      cv.set(5, 10, sc(base, 0.75F));
      cv.set(8, 12, sc(base, 0.5F));
      return cv.p;
   }

   /** Called from TexArt312.blocks(): all tiles of this file. */
   static void tiles() {
      for (int c = 0; c < 16; c++) {
         Tex.blit(T_CONCRETE0 + c, concrete(c));
         Tex.blit(T_POWDER0 + c, powder(c));
         Tex.blit(T_STAINED0 + c, terracotta(TERRA[c], 1450L + c));
         Tex.blit(T_GLAZED0 + c, glazed(c));
         Tex.blit(T_GLASS0 + c, glass(c));
      }

      Tex.blit(T_TERRACOTTA, terracotta(PLAIN_TERRA, 1449L));
      Tex.blit(T_CLAY, clay());
      Tex.blit(T_IRON, gemBlock(0xDCDCDC, 1471L));
      Tex.blit(T_GOLD, gemBlock(0xF6D33A, 1472L));
      Tex.blit(T_DIAMOND, gemBlock(0x62DBD5, 1473L));
      Tex.blit(T_EMERALD, gemBlock(0x2FCB5E, 1474L));
      Tex.blit(T_COAL, coalBlock());
      Tex.blit(T_LAPIS, lapisBlock());
      Tex.blit(T_EMERALD_ORE, emeraldOre());
   }

   // ------------------------------------------------------------------ registration
   private static void pick(int id, boolean needTool, int level) {
      Blocks.mineClass[id] = 1;
      Blocks.needTool[id] = needTool;
      Blocks.harvestLevel[id] = level;
   }

   static void blocks() {
      for (int c = 0; c < 16; c++) {
         int id = Blocks.CONCRETE0 + c;
         Blocks.def(id, NAME[c] + " Concrete", 1.8F, true, T_CONCRETE0 + c);
         pick(id, false, 0);
         id = Blocks.POWDER0 + c;
         Blocks.def(id, NAME[c] + " Concrete Powder", 0.5F, true, T_POWDER0 + c);
         Blocks.mineClass[id] = 3;
         id = Blocks.STAINED0 + c;
         Blocks.def(id, NAME[c] + " Terracotta", 1.25F, true, T_STAINED0 + c);
         pick(id, true, 0);
         id = Blocks.GLAZED0 + c;
         Blocks.def(id, NAME[c] + " Glazed Terracotta", 1.4F, true, T_GLAZED0 + c);
         pick(id, true, 0);
         id = Blocks.GLASS0 + c;
         Blocks.def(id, NAME[c] + " Stained Glass", 0.3F, false, T_GLASS0 + c);
         Blocks.drop[id] = 0;
      }

      Blocks.def(Blocks.TERRACOTTA, "Terracotta", 1.25F, true, T_TERRACOTTA);
      pick(Blocks.TERRACOTTA, true, 0);
      Blocks.def(Blocks.CLAY, "Clay", 0.6F, true, T_CLAY);
      Blocks.mineClass[Blocks.CLAY] = 3;
      Blocks.def(Blocks.IRON_BLOCK, "Block of Iron", 5.0F, true, T_IRON);
      pick(Blocks.IRON_BLOCK, true, 1);
      Blocks.def(Blocks.GOLD_BLOCK, "Block of Gold", 3.0F, true, T_GOLD);
      pick(Blocks.GOLD_BLOCK, true, 2);
      Blocks.def(Blocks.DIAMOND_BLOCK, "Block of Diamond", 5.0F, true, T_DIAMOND);
      pick(Blocks.DIAMOND_BLOCK, true, 2);
      Blocks.def(Blocks.EMERALD_BLOCK, "Block of Emerald", 5.0F, true, T_EMERALD);
      pick(Blocks.EMERALD_BLOCK, true, 2);
      Blocks.def(Blocks.COAL_BLOCK, "Block of Coal", 5.0F, true, T_COAL);
      pick(Blocks.COAL_BLOCK, true, 0);
      Blocks.def(Blocks.LAPIS_BLOCK, "Lapis Lazuli Block", 3.0F, true, T_LAPIS);
      pick(Blocks.LAPIS_BLOCK, true, 1);
      Blocks.def(Blocks.EMERALD_ORE, "Emerald Ore", 3.0F, true, T_EMERALD_ORE);
      pick(Blocks.EMERALD_ORE, true, 2);
      Blocks.drop[Blocks.EMERALD_ORE] = Items.EMERALD;
   }

   static void items() {
      for (int c = 1; c < 16; c++) {
         int id = Items.DYE1 + c - 1;
         Items.defTile(id, NAME[c] + " Dye", TexArt312.ITEM0 + (id - Items.QUARTZ));
      }
   }

   static void addToCreative(java.util.ArrayList<Integer> list) {
      for (int c = 1; c < 16; c++) {
         list.add(Items.DYE1 + c - 1);
      }

      for (int c = 0; c < 16; c++) {
         list.add(Blocks.CONCRETE0 + c);
      }

      for (int c = 0; c < 16; c++) {
         list.add(Blocks.POWDER0 + c);
      }

      list.add(Blocks.CLAY);
      list.add(Blocks.TERRACOTTA);
      for (int c = 0; c < 16; c++) {
         list.add(Blocks.STAINED0 + c);
      }

      for (int c = 0; c < 16; c++) {
         list.add(Blocks.GLAZED0 + c);
      }

      for (int c = 0; c < 16; c++) {
         list.add(Blocks.GLASS0 + c);
      }

      for (int id = Blocks.IRON_BLOCK; id <= Blocks.EMERALD_ORE; id++) {
         list.add(id);
      }
   }

   /** The mixes of dye: {result, one, other}; two dyes make two. */
   private static final int[][] MIX = {
      {1, 14, 4}, {2, 10, 6}, {3, 11, 0}, {5, 13, 0}, {6, 14, 0}, {7, 15, 0}, {8, 7, 0}, {9, 11, 13}, {10, 11, 14}, {12, 14, 13}
   };

   static void recipes() {
      for (int[] m : MIX) {
         Recipes.shapeless(dyeItem(m[0]), 2, dyeItem(m[1]), dyeItem(m[2]));
      }

      Recipes.shapeless(dyeItem(14), 1, 30);
      Recipes.shapeless(dyeItem(4), 1, 31);
      Recipes.shapeless(dyeItem(15), 1, Items.COAL);
      Recipes.smelt.put(28, new Stack(dyeItem(13), 1));
      // storage blocks, and back
      Recipes.shaped(Blocks.IRON_BLOCK, 1, new String[]{"III", "III", "III"}, 'I', Items.IRON_INGOT);
      Recipes.shaped(Blocks.GOLD_BLOCK, 1, new String[]{"III", "III", "III"}, 'I', Items.GOLD_INGOT);
      Recipes.shaped(Blocks.DIAMOND_BLOCK, 1, new String[]{"III", "III", "III"}, 'I', Items.DIAMOND);
      Recipes.shaped(Blocks.EMERALD_BLOCK, 1, new String[]{"III", "III", "III"}, 'I', Items.EMERALD);
      Recipes.shaped(Blocks.COAL_BLOCK, 1, new String[]{"III", "III", "III"}, 'I', Items.COAL);
      Recipes.shaped(Blocks.LAPIS_BLOCK, 1, new String[]{"III", "III", "III"}, 'I', dyeItem(11));
      Recipes.shapeless(Items.IRON_INGOT, 9, Blocks.IRON_BLOCK);
      Recipes.shapeless(Items.GOLD_INGOT, 9, Blocks.GOLD_BLOCK);
      Recipes.shapeless(Items.DIAMOND, 9, Blocks.DIAMOND_BLOCK);
      Recipes.shapeless(Items.EMERALD, 9, Blocks.EMERALD_BLOCK);
      Recipes.shapeless(Items.COAL, 9, Blocks.COAL_BLOCK);
      Recipes.shapeless(dyeItem(11), 9, Blocks.LAPIS_BLOCK);
      // clay -> terracotta -> glazed terracotta in the furnace
      Recipes.smelt.put(Blocks.CLAY, new Stack(Blocks.TERRACOTTA, 1));
      for (int c = 0; c < 16; c++) {
         Recipes.smelt.put(Blocks.STAINED0 + c, new Stack(Blocks.GLAZED0 + c, 1));
      }
   }

   /**
    * The recipes that depend on the colour of a dye (so that one rule serves all 16): dye + wool, eight terracotta (any colour) around a dye,
    * eight glass around a dye, four sand + four gravel + a dye (= eight concrete powder). Anywhere on the grid. Null if the grid is none of them.
    */
   static Stack dyeRecipe(Stack[] grid, int gw, int gh) {
      int total = 0;
      int dyes = 0;
      int dye = -1;
      int wool = 0;
      int woolColor = -1;
      int terra = 0;
      int glass = 0;
      int sand = 0;
      int gravel = 0;
      for (int i = 0; i < gw * gh; i++) {
         Stack s = grid[i];
         if (s == null) {
            continue;
         }

         total++;
         int id = s.id;
         int dc = dyeColor(id);
         if (dc >= 0) {
            dyes++;
            dye = dc;
         } else if (id >= Blocks.WOOL0 && id < Blocks.WOOL0 + 16) {
            wool++;
            woolColor = id - Blocks.WOOL0;
         } else if (id == Blocks.TERRACOTTA || id >= Blocks.STAINED0 && id < Blocks.STAINED0 + 16) {
            terra++;
         } else if (id == Blocks.GLASS || isStainedGlass(id)) {
            glass++;
         } else if (id == 7) {
            sand++;
         } else if (id == 8) {
            gravel++;
         }
      }

      if (dyes != 1) {
         return null;
      }

      if (total == 2 && wool == 1 && woolColor != dye) {
         return new Stack(Blocks.WOOL0 + dye, 1);
      }

      if (total == 9 && terra == 8) {
         return new Stack(Blocks.STAINED0 + dye, 8);
      }

      if (total == 9 && glass == 8) {
         return new Stack(Blocks.GLASS0 + dye, 8);
      }

      if (total == 9 && sand == 4 && gravel == 4) {
         return new Stack(Blocks.POWDER0 + dye, 8);
      }

      return null;
   }

   // ------------------------------------------------------------------ world generation
   /**
    * Emerald ore in the mountains (single blocks, deep) and clay in the beds of lakes and the sea. Own random stream: the rest of the
    * terrain of a chunk stays exactly as it was. Only with Gen.extras.
    */
   static void decorate(long seed, Chunk c, int[] heights, int[] biomes) {
      Random r = new Random(seed * 6364136223846793005L ^ (c.cx * 668265263L + c.cz * 374761393L) * 1000003L + 4242L);
      // emerald ore: only under mountains
      int mountain = 0;
      for (int b : biomes) {
         if (b == 6) {
            mountain++;
         }
      }

      if (mountain >= 40) {
         int tries = 3 + r.nextInt(4);
         for (int k = 0; k < tries; k++) {
            int x = r.nextInt(16);
            int z = r.nextInt(16);
            int y = 4 + r.nextInt(28);
            if (c.get(x, y, z) == 1) {
               c.set(x, y, z, Blocks.EMERALD_ORE);
            }
         }
      }

      // clay: a round patch in the bed of shallow water
      if (r.nextInt(4) == 0) {
         int px = 3 + r.nextInt(10);
         int pz = 3 + r.nextInt(10);
         int top = heights[pz * 16 + px];
         if (top >= 50 && top < 62) {
            int rad = 2 + r.nextInt(2);
            for (int dz = -rad; dz <= rad; dz++) {
               for (int dx = -rad; dx <= rad; dx++) {
                  int x = px + dx;
                  int z = pz + dz;
                  if (x < 0 || x > 15 || z < 0 || z > 15 || dx * dx + dz * dz > rad * rad + 1) {
                     continue;
                  }

                  int h = heights[z * 16 + x];
                  if (h < 50 || h > 62) {
                     continue;
                  }

                  for (int y = h; y > h - 2 && y > 1; y--) {
                     int id = c.get(x, y, z);
                     if (id == 7 || id == 8 || id == 3) {
                        c.set(x, y, z, Blocks.CLAY);
                     }
                  }
               }
            }
         }
      }
   }
}
