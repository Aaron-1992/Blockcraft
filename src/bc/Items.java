package bc;

public final class Items {
   public static final int BASE = 256;
   public static final int STICK = 256;
   public static final int COAL = 257;
   public static final int IRON_INGOT = 258;
   public static final int GOLD_INGOT = 259;
   public static final int DIAMOND = 260;
   public static final int LEATHER = 261;
   public static final int FEATHER = 262;
   public static final int STRING = 263;
   public static final int BONE = 264;
   public static final int GUNPOWDER = 265;
   public static final int FLINT = 266;
   public static final int ARROW = 267;
   public static final int BOW = 268;
   public static final int APPLE = 270;
   public static final int RAW_PORK = 272;
   public static final int COOKED_PORK = 273;
   public static final int RAW_BEEF = 274;
   public static final int STEAK = 275;
   public static final int RAW_CHICKEN = 276;
   public static final int COOKED_CHICKEN = 277;
   public static final int RAW_MUTTON = 278;
   public static final int COOKED_MUTTON = 279;
   public static final int ROTTEN_FLESH = 280;
   public static final int DOOR = 281;
   public static final int BUCKET = 282;
   public static final int WATER_BUCKET = 283;
   public static final int LAVA_BUCKET = 284;
   public static final int SEEDS = 285;
   public static final int WHEAT = 286;
   public static final int BREAD = 287;
   public static final int FLINT_STEEL = 288;
   public static final int BED = 289;
   public static final int HOE0 = 290;
   public static final int TOOL_BASE = 300;
   public static final int ARMOR_BASE = 320;
   public static final int CARROT = 336;
   public static final int POTATO = 337;
   public static final int BAKED_POTATO = 338;
   public static final int BONE_MEAL = 339;
   public static final int SHEARS = 340;
   public static final int PUMPKIN_SEEDS = 341;
   public static final int MELON_SEEDS = 342;
   public static final int MELON_SLICE = 343;
   public static final int PUMPKIN_PIE = 344;
   public static final int SUGAR = 345;
   public static final int PAPER = 346;
   public static final int BOOK = 347;
   public static final int MILK_BUCKET = 348;
   public static final int EGG = 349;
   public static final int CAKE = 350;
   public static final int END = 351;
   // ---- Update 3.12 ----
   public static final int QUARTZ = 352;
   public static final int NETHER_BRICK = 353;
   public static final int BLAZE_ROD = 354;
   public static final int BLAZE_POWDER = 355;
   public static final int GHAST_TEAR = 356;
   public static final int GOLD_NUGGET = 357;
   public static final int ENDER_PEARL = 358;
   public static final int EYE_OF_ENDER = 359;
   public static final int EMERALD = 360;
   public static final int ITEM_TILE_BASE = 65;
   public static final int C_NONE = 0;
   public static final int C_PICK = 1;
   public static final int C_AXE = 2;
   public static final int C_SHOVEL = 3;
   public static final int C_SWORD = 4;
   public static final String[] TIER_NAMES = new String[]{"Wooden", "Stone", "Iron", "Golden", "Diamond"};
   public static final float[] TIER_SPEED = new float[]{2.0F, 4.0F, 6.0F, 12.0F, 8.0F};
   public static final int[] TIER_DUR = new int[]{59, 131, 250, 32, 1561};
   public static final int[] TIER_LEVEL = new int[]{0, 1, 2, 0, 3};
   public static final String[] CLASS_NAMES = new String[]{"", "Pickaxe", "Axe", "Shovel", "Sword"};
   static final float[][] ATTACK = new float[][]{
      {4.0F, 5.0F, 6.0F, 4.0F, 7.0F}, {2.0F, 3.0F, 4.0F, 2.0F, 5.0F}, {7.0F, 9.0F, 9.0F, 7.0F, 9.0F}, {2.5F, 3.5F, 4.5F, 2.5F, 5.5F}
   };
   static final float[] SPEED_SWORD = new float[]{1.6F};
   static final float[] SPEED_PICK = new float[]{1.2F};
   static final float[] SPEED_AXE = new float[]{0.8F, 0.8F, 0.9F, 1.0F, 1.0F};
   static final float[] SPEED_SHOVEL = new float[]{1.0F};
   public static final String[] ARMOR_MAT = new String[]{"Leather", "Iron", "Golden", "Diamond"};
   public static final String[] ARMOR_PIECE = new String[]{"Cap", "Tunic", "Pants", "Boots"};
   public static final String[][] ARMOR_NAME = new String[][]{
      {"Leather Cap", "Leather Tunic", "Leather Pants", "Leather Boots"},
      {"Iron Helmet", "Iron Chestplate", "Iron Leggings", "Iron Boots"},
      {"Golden Helmet", "Golden Chestplate", "Golden Leggings", "Golden Boots"},
      {"Diamond Helmet", "Diamond Chestplate", "Diamond Leggings", "Diamond Boots"}
   };
   static final int[][] ARMOR_POINTS = new int[][]{{1, 3, 2, 1}, {2, 6, 5, 2}, {2, 5, 3, 1}, {3, 8, 6, 3}};
   static final int[] ARMOR_DUR_MUL = new int[]{5, 15, 7, 33};
   static final int[] ARMOR_DUR_BASE = new int[]{11, 16, 15, 13};
   static final String[] name = new String[Blocks.EXT0];
   static final int[] tile = new int[Blocks.EXT0];
   static final int[] maxDmg = new int[Blocks.EXT0];
   static final int[] maxStack = new int[Blocks.EXT0];
   static final int[] foodPts = new int[Blocks.EXT0];
   static final float[] foodSat = new float[Blocks.EXT0];

   private static void def(int var0, String var1, int var2) {
      name[var0] = var1;
      tile[var0] = 65 + var2;
      maxStack[var0] = 64;
   }

   /** Update 3.12: an item whose picture is the atlas tile {@code tile} (any tile, not only 65 + offset). */
   static void defTile(int id, String itemName, int atlasTile) {
      name[id] = itemName;
      tile[id] = atlasTile;
      maxStack[id] = 64;
   }

   private static void food(int var0, int var1, float var2) {
      foodPts[var0] = var1;
      foodSat[var0] = var2;
   }

   public static boolean isHoe(int var0) {
      return var0 >= 290 && var0 < 295;
   }

   /** Items that are planted on farmland: wheat/pumpkin/melon seeds, carrots, potatoes. Returns the block id of growth stage 0, or 0. */
   public static int plantBlock(int var0) {
      return var0 == 285 ? 124 : (var0 == 336 ? 142 : (var0 == 337 ? 150 : (var0 == 341 ? 171 : (var0 == 342 ? 179 : 0))));
   }

   /** Update 3.12: items own the ids 256..1023; ids from Blocks.EXT0 up are (new) blocks again. */
   public static boolean isItem(int var0) {
      return var0 >= 256 && var0 < Blocks.EXT0;
   }

   public static String name(int var0) {
      if (!isItem(var0)) {
         return var0 >= 0 && var0 < Blocks.MAX ? Blocks.name[var0] : "Block " + var0;
      } else {
         return var0 < name.length && name[var0] != null ? name[var0] : "Item " + var0;
      }
   }

   public static int tile(int var0) {
      return tile[var0];
   }

   public static int maxStack(int var0) {
      return var0 >= 256 && var0 < Blocks.EXT0 ? maxStack[var0] : 64;
   }

   public static int maxDamage(int var0) {
      return var0 >= 256 && var0 < Blocks.EXT0 ? maxDmg[var0] : 0;
   }

   public static boolean isFood(int var0) {
      return var0 >= 256 && var0 < Blocks.EXT0 && foodPts[var0] > 0;
   }

   public static boolean isTool(int var0) {
      return var0 >= 300 && var0 < 320;
   }

   public static boolean isArmor(int var0) {
      return var0 >= 320 && var0 < 336;
   }

   public static int toolClass(int var0) {
      if (!isTool(var0)) {
         return 0;
      }

      int var1 = var0 - 300 & 3;
      return var1 == 0 ? 4 : var1;
   }

   public static int toolTier(int var0) {
      return isTool(var0) ? (var0 - 300) / 4 : -1;
   }

   public static int armorSlot(int var0) {
      return var0 - 320 & 3;
   }

   public static int armorMat(int var0) {
      return (var0 - 320) / 4;
   }

   public static int armorPoints(int var0) {
      return isArmor(var0) ? ARMOR_POINTS[armorMat(var0)][armorSlot(var0)] : 0;
   }

   public static int toolId(int var0, int var1) {
      return 300 + var0 * 4 + (var1 == 4 ? 0 : var1);
   }

   public static int armorId(int var0, int var1) {
      return 320 + var0 * 4 + var1;
   }

   public static float attackDamage(int var0) {
      if (!isTool(var0)) {
         return 1.0F;
      }

      int var1 = toolClass(var0);
      int var2 = toolTier(var0);
      int var3 = var1 == 4 ? 0 : (var1 == 1 ? 1 : (var1 == 2 ? 2 : 3));
      return ATTACK[var3][var2];
   }

   public static float attackSpeed(int var0) {
      if (!isTool(var0)) {
         return 4.0F;
      }

      int var1 = toolClass(var0);
      int var2 = toolTier(var0);
      switch (var1) {
         case 1:
            return 1.2F;
         case 2:
            return SPEED_AXE[var2];
         case 3:
         default:
            return 1.0F;
         case 4:
            return 1.6F;
      }
   }

   public static boolean effective(int var0, int var1) {
      int var2 = toolClass(var0);
      if (var2 == 0) {
         return false;
      } else {
         return var2 != 4 ? Blocks.mineClass[var1] == var2 : var1 == 12 || Blocks.isLeaves(var1) || var1 == 29;
      }
   }

   public static boolean canHarvest(int var0, int var1) {
      if (!Blocks.needTool[var1]) {
         return true;
      }

      int var2 = toolClass(var0);
      return var2 != Blocks.mineClass[var1] ? false : TIER_LEVEL[toolTier(var0)] >= Blocks.harvestLevel[var1];
   }

   public static int breakTicks(int var0, int var1, boolean var2, boolean var3) {
      float var4 = Blocks.hardness[var0];
      if (var4 < 0.0F) {
         return Integer.MAX_VALUE;
      }

      if (var4 == 0.0F) {
         return 1;
      }

      boolean var5 = canHarvest(var1, var0);
      float var6 = 1.0F;
      if (effective(var1, var0)) {
         var6 = TIER_SPEED[Math.max(0, toolTier(var1))];
      }

      if (isTool(var1) && toolClass(var1) == 4 && effective(var1, var0)) {
         var6 = 1.5F;
      }

      if (var3) {
         var6 /= 5.0F;
      }

      if (!var2) {
         var6 /= 5.0F;
      }

      float var7 = var6 / var4 / (var5 ? 30.0F : 100.0F);
      return var7 >= 1.0F ? 1 : (int)Math.ceil(1.0F / var7);
   }

   static {
      for (int var0 = 0; var0 < Blocks.EXT0; var0++) {
         maxStack[var0] = 64;
      }

      int var5 = 0;
      def(256, "Stick", var5++);
      def(257, "Coal", var5++);
      def(258, "Iron Ingot", var5++);
      def(259, "Gold Ingot", var5++);
      def(260, "Diamond", var5++);
      def(261, "Leather", var5++);
      def(262, "Feather", var5++);
      def(263, "String", var5++);
      def(264, "Bone", var5++);
      def(265, "Gunpowder", var5++);
      def(266, "Flint", var5++);
      def(267, "Arrow", var5++);
      def(268, "Bow", var5++);
      maxStack[268] = 1;
      maxDmg[268] = 384;
      def(270, "Apple", var5++);
      def(272, "Raw Porkchop", var5++);
      def(273, "Cooked Porkchop", var5++);
      def(274, "Raw Beef", var5++);
      def(275, "Steak", var5++);
      def(276, "Raw Chicken", var5++);
      def(277, "Cooked Chicken", var5++);
      def(278, "Raw Mutton", var5++);
      def(279, "Cooked Mutton", var5++);
      def(280, "Rotten Flesh", var5++);
      def(281, "Oak Door", var5++);
      def(282, "Bucket", 125);
      def(283, "Water Bucket", 126);
      def(284, "Lava Bucket", 127);
      def(285, "Wheat Seeds", 128);
      def(286, "Wheat", 129);
      def(287, "Bread", 130);
      def(288, "Flint and Steel", 131);
      def(289, "Bed", 132);
      maxStack[282] = 16;
      maxStack[283] = 1;
      maxStack[284] = 1;
      maxStack[288] = 1;
      maxStack[289] = 1;
      maxDmg[288] = 64;
      def(336, "Carrot", 149);
      def(337, "Potato", 150);
      def(338, "Baked Potato", 151);
      def(339, "Bone Meal", 152);
      def(340, "Shears", 153);
      maxStack[340] = 1;
      maxDmg[340] = 238;

      // ---- Update 3.7 (atlas tiles 236-245, see TexArt37) ----
      def(341, "Pumpkin Seeds", 241 - 65);
      def(342, "Melon Seeds", 242 - 65);
      def(343, "Melon Slice", 243 - 65);
      def(344, "Pumpkin Pie", 244 - 65);
      def(345, "Sugar", 236 - 65);
      def(346, "Paper", 237 - 65);
      def(347, "Book", 238 - 65);
      def(348, "Milk Bucket", 239 - 65);
      def(349, "Egg", 240 - 65);
      def(350, "Cake", 245 - 65);
      maxStack[348] = 1;
      maxStack[349] = 16;
      maxStack[350] = 1;

      for (int var1 = 0; var1 < 5; var1++) {
         int var2 = 290 + var1;
         name[var2] = TIER_NAMES[var1] + " Hoe";
         tile[var2] = 198 + var1;
         maxStack[var2] = 1;
         maxDmg[var2] = TIER_DUR[var1];
      }

      food(287, 5, 6.0F);
      food(270, 4, 2.4F);
      food(272, 3, 1.8F);
      food(273, 8, 12.8F);
      food(274, 3, 1.8F);
      food(275, 8, 12.8F);
      food(276, 2, 1.2F);
      food(277, 6, 7.2F);
      food(278, 2, 1.2F);
      food(279, 6, 9.6F);
      food(280, 4, 0.8F);
      food(336, 3, 3.6F);
      food(337, 1, 0.6F);
      food(338, 5, 6.0F);
      food(343, 2, 1.2F);
      food(344, 8, 4.8F);

      for (int var30 = 0; var30 < 5; var30++) {
         for (int var32 = 0; var32 < 4; var32++) {
            int var3 = 300 + var30 * 4 + var32;
            byte var4 = (byte)(var32 == 0 ? 4 : var32);
            name[var3] = TIER_NAMES[var30] + " " + CLASS_NAMES[var4];
            tile[var3] = 105 + var30 * 4 + var32;
            maxStack[var3] = 1;
            maxDmg[var3] = TIER_DUR[var30];
         }
      }

      for (int var31 = 0; var31 < 4; var31++) {
         for (int var33 = 0; var33 < 4; var33++) {
            int var34 = 320 + var31 * 4 + var33;
            name[var34] = ARMOR_NAME[var31][var33];
            tile[var34] = 135 + var31 * 4 + var33;
            maxStack[var34] = 1;
            maxDmg[var34] = ARMOR_DUR_BASE[var33] * ARMOR_DUR_MUL[var31];
         }
      }

      // ---- Update 3.12 ----
      Items312.register();
   }
}
