package bc;

import java.util.ArrayList;

public final class Blocks {
   public static final int AIR = 0;
   public static final int STONE = 1;
   public static final int GRASS = 2;
   public static final int DIRT = 3;
   public static final int COBBLE = 4;
   public static final int PLANKS = 5;
   public static final int BEDROCK = 6;
   public static final int SAND = 7;
   public static final int GRAVEL = 8;
   public static final int LOG_OAK = 9;
   public static final int LOG_BIRCH = 10;
   public static final int LOG_SPRUCE = 11;
   public static final int LEAVES_OAK = 12;
   public static final int LEAVES_BIRCH = 13;
   public static final int LEAVES_SPRUCE = 14;
   public static final int GLASS = 15;
   public static final int BRICKS = 16;
   public static final int SANDSTONE = 17;
   public static final int SNOW = 18;
   public static final int COAL_ORE = 19;
   public static final int IRON_ORE = 20;
   public static final int GOLD_ORE = 21;
   public static final int DIAMOND_ORE = 22;
   public static final int REDSTONE_ORE = 23;
   public static final int LAPIS_ORE = 24;
   public static final int GLOWSTONE = 25;
   public static final int OBSIDIAN = 26;
   public static final int STONEBRICK = 27;
   public static final int CACTUS = 28;
   public static final int TALLGRASS = 29;
   public static final int FLOWER_RED = 30;
   public static final int FLOWER_YELLOW = 31;
   public static final int TORCH = 32;
   public static final int WOOL0 = 34;
   public static final int WATER = 50;
   public static final int LAVA = 58;
   public static final int CRAFTING_TABLE = 66;
   public static final int FURNACE = 67;
   public static final int FURNACE_LIT = 68;
   public static final int CHEST = 69;
   public static final int CHEST_LOOT = 70;
   public static final int SPAWNER = 71;
   public static final int MOSSY_COBBLE = 72;
   public static final int DOOR_LO = 73;
   public static final int DOOR_HI = 81;
   public static final int WALL_TORCH = 89;
   public static final int STAIR0 = 93;
   public static final int SLAB0 = 113;
   /** Update 3.10: upside-down stairs (20 ids: material * 4 + facing, like 93..112) and top-half slabs (5 ids). */
   public static final int STAIR_TOP0 = 195;
   public static final int SLAB_TOP0 = 215;
   public static final int FENCE = 118;
   public static final int LADDER = 119;
   public static final int FARMLAND = 123;
   public static final int WHEAT = 124;
   public static final int TNT = 132;
   public static final int BED_FOOT = 133;
   public static final int BED_HEAD = 137;
   public static final int FARMLAND_WET = 141;
   public static final int CARROT0 = 142;
   public static final int POTATO0 = 150;
   public static final int HAY_BALE = 158;
   public static final int SUGAR_CANE = 159;
   public static final int BOOKSHELF = 160;
   public static final int PUMPKIN = 161;
   public static final int CARVED_PUMPKIN = 162;
   public static final int JACK_O_LANTERN = 166;
   public static final int MELON = 170;
   public static final int PUMPKIN_STEM = 171;
   public static final int MELON_STEM = 179;
   public static final int CAKE = 187;
   public static final int FIRE = 194;
   /** Update 3.10: logs lying on their side (felled trees): 220..222 along the x axis, 223..225 along the z axis (oak, birch, spruce). */
   public static final int LOG_X0 = 220;
   public static final int LOG_Z0 = 223;
   public static final int COUNT = 226;
   /**
    * Update 3.12: block ids are 12 bits wide. The ids 0..255 are the blocks of Update 3.5 - 3.10 (226..255 stay free), 256..1023 belong to the
    * ITEMS (so every saved inventory keeps working), and the new blocks live in EXT0..MAX-1.
    */
   public static final int MAX = 4096;
   public static final int EXT0 = 1024;

   /** True for every id that can be a block (0..255 and EXT0..MAX-1); the ids in between are items. */
   public static boolean isBlockId(int id) {
      return id >= 0 && id < 256 || id >= EXT0 && id < MAX;
   }
   public static final int[] STAIR_MAT = new int[]{5, 4, 27, 16, 17};
   public static final String[] STAIR_NAME = new String[]{"Oak Wood", "Cobblestone", "Stone Brick", "Brick", "Sandstone"};
   public static final int K_CUBE = 0;
   public static final int K_CROSS = 1;
   public static final int K_BOX = 2;
   public static final int K_FLUID = 3;
   public static final int K_STAIR = 4;
   public static final int K_WTORCH = 5;
   public static final int K_FENCE = 6;
   public static final int K_CROP = 7;
   public static final int T_NONE = 0;
   public static final int T_GRASS = 1;
   public static final int T_FOLIAGE = 2;
   public static final String[] name = new String[MAX];
   public static final boolean[] opaque = new boolean[MAX];
   public static final int[] opacity = new int[MAX];
   public static final boolean[] solid = new boolean[MAX];
   public static final int[] emit = new int[MAX];
   public static final float[] hardness = new float[MAX];
   public static final int[] kind = new int[MAX];
   public static final int[] tint = new int[MAX];
   public static final int[][] tiles = new int[MAX][6];
   public static final boolean[] replaceable = new boolean[MAX];
   public static final int[] drop = new int[MAX];
   public static final boolean[] cutout = new boolean[MAX];
   public static final double[][] coll = new double[MAX][];
   public static final double[][] box = new double[MAX][];
   public static final double[][][] collMulti = new double[MAX][][];
   public static final int[] mineClass = new int[MAX];
   public static final boolean[] needTool = new boolean[MAX];
   public static final int[] harvestLevel = new int[MAX];

   static void def(int var0, String var1, float var2, boolean var3, int var4) {
      name[var0] = var1;
      hardness[var0] = var2;
      opaque[var0] = var3;
      opacity[var0] = var3 ? 15 : 0;
      solid[var0] = true;
      kind[var0] = 0;

      for (int var5 = 0; var5 < 6; var5++) {
         tiles[var0][var5] = var4;
      }

      drop[var0] = var0;
      coll[var0] = new double[]{0.0, 0.0, 0.0, 1.0, 1.0, 1.0};
   }

   static void faces(int var0, int var1, int var2, int var3) {
      tiles[var0][0] = var1;
      tiles[var0][1] = var2;

      for (int var4 = 2; var4 < 6; var4++) {
         tiles[var0][var4] = var3;
      }
   }

   public static boolean isWater(int var0) {
      return var0 >= 50 && var0 < 58;
   }

   public static boolean isLava(int var0) {
      return var0 >= 58 && var0 < 66;
   }

   public static boolean isFluid(int var0) {
      return var0 >= 50 && var0 < 66;
   }

   public static int fluidLevel(int var0) {
      return var0 - 50 & 7;
   }

   public static boolean sameFluid(int var0, int var1) {
      return isWater(var0) && isWater(var1) || isLava(var0) && isLava(var1);
   }

   public static boolean isDoor(int var0) {
      return var0 >= 73 && var0 < 89;
   }

   public static boolean isDoorLo(int var0) {
      return var0 >= 73 && var0 < 81;
   }

   public static boolean isDoorHi(int var0) {
      return var0 >= 81 && var0 < 89;
   }

   public static boolean doorOpen(int var0) {
      return (var0 - 73 & 4) != 0;
   }

   public static int doorFacing(int var0) {
      return var0 - 73 & 3;
   }

   public static boolean isWallTorch(int var0) {
      return var0 >= 89 && var0 < 93;
   }

   /** Any stair, right way up (93..112) or upside down (195..214). */
   public static boolean isStair(int var0) {
      return var0 >= 93 && var0 < 113 || var0 >= STAIR_TOP0 && var0 < STAIR_TOP0 + 20;
   }

   /** Any slab, bottom half (113..117) or top half (215..219). */
   public static boolean isSlab(int var0) {
      return var0 >= 113 && var0 < 118 || var0 >= SLAB_TOP0 && var0 < SLAB_TOP0 + 5;
   }

   /** Which way a stair faces (0..3), either way up. */
   public static int stairFacing(int var0) {
      return var0 - (isTopStair(var0) ? STAIR_TOP0 : STAIR0) & 3;
   }

   public static boolean isTopStair(int var0) {
      return var0 >= STAIR_TOP0 && var0 < STAIR_TOP0 + 20;
   }

   public static boolean isTopSlab(int var0) {
      return var0 >= SLAB_TOP0 && var0 < SLAB_TOP0 + 5;
   }

   /** The material (0..4: oak wood, cobblestone, stone brick, brick, sandstone) of a stair or slab block, either way up. */
   public static int stairSlabMaterial(int var0) {
      if (isTopSlab(var0)) {
         return var0 - SLAB_TOP0;
      } else if (isSlab(var0)) {
         return var0 - 113;
      } else if (isTopStair(var0)) {
         return (var0 - STAIR_TOP0) / 4;
      } else {
         return (var0 - 93) / 4;
      }
   }

   public static boolean isLadder(int var0) {
      return var0 >= 119 && var0 < 123;
   }

   /** Any growing crop block: wheat 124-131, carrots 142-149, potatoes 150-157 (8 growth stages each). */
   public static boolean isCrop(int var0) {
      return var0 >= 124 && var0 < 132 || var0 >= 142 && var0 < 158;
   }

   /** Dry (123) or hydrated (141) farmland. */
   public static boolean isFarmland(int var0) {
      return var0 == 123 || var0 == 141;
   }

   /** First block id (growth stage 0) of the crop family this crop block belongs to. */
   public static int cropBase(int var0) {
      return var0 < 132 ? 124 : (var0 < 150 ? 142 : 150);
   }

   public static int cropStage(int var0) {
      return var0 - cropBase(var0);
   }

   public static boolean isRipe(int var0) {
      return isCrop(var0) && cropStage(var0) == 7;
   }

   /** The item that is dropped when an unripe crop is broken / pops off: it is also the thing you plant. */
   public static int cropSeedItem(int var0) {
      int var1 = cropBase(var0);
      return var1 == 124 ? 285 : (var1 == 142 ? 336 : 337);
   }

   public static boolean isBed(int var0) {
      return var0 >= 133 && var0 < 141;
   }

   public static boolean isBedHead(int var0) {
      return var0 >= 137 && var0 < 141;
   }

   /** Pumpkin stems 171-178 and melon stems 179-186 (8 growth stages each, stage 7 = fully grown). */
   public static boolean isStem(int var0) {
      return var0 >= 171 && var0 < 187;
   }

   public static int stemStage(int var0) {
      return var0 - 171 & 7;
   }

   /** The fruit block a fully grown stem grows next to itself: pumpkin (161) or melon (170). */
   public static int stemFruit(int var0) {
      return var0 < 179 ? 161 : 170;
   }

   /** The seed item that is planted to get this stem. */
   public static int stemSeedItem(int var0) {
      return var0 < 179 ? 341 : 342;
   }

   public static boolean isCarvedPumpkin(int var0) {
      return var0 >= 162 && var0 < 166;
   }

   public static boolean isJackOLantern(int var0) {
      return var0 >= 166 && var0 < 170;
   }

   /** Cake with 0..6 slices eaten (187..193). */
   public static boolean isCake(int var0) {
      return var0 >= 187 && var0 < 194;
   }

   public static boolean isFire(int var0) {
      return var0 == 194;
   }

   /** The item you get for a block that is placed in a facing/age variant: what pick-block and drops use. 0 = nothing. */
   public static int pickItem(int var0) {
      if (isStair(var0)) {
         return 93 + stairSlabMaterial(var0) * 4;
      } else if (isSlab(var0)) {
         return 113 + stairSlabMaterial(var0);
      } else if (isCarvedPumpkin(var0)) {
         return 163;
      } else if (isJackOLantern(var0)) {
         return 167;
      } else if (isStem(var0)) {
         return stemSeedItem(var0);
      } else if (isCake(var0)) {
         return 350;
      } else if (isSideLog(var0)) {
         return uprightLog(var0);
      } else {
         return isFire(var0) ? 0 : var0;
      }
   }

   public static boolean isLeaves(int var0) {
      return var0 >= 12 && var0 <= 14;
   }

   public static boolean isLog(int var0) {
      return var0 >= 9 && var0 <= 11;
   }

   /** A log lying on its side (220..225), as a felled tree leaves them. */
   public static boolean isSideLog(int var0) {
      return var0 >= LOG_X0 && var0 < LOG_X0 + 6;
   }

   /** Upright log or log on its side. */
   public static boolean isAnyLog(int var0) {
      return var0 >= 9 && var0 <= 11 || var0 >= LOG_X0 && var0 < LOG_X0 + 6;
   }

   /** The side log of an upright log (9..11) lying along the x axis (axis 0) or the z axis (axis 1). */
   public static int sideLog(int upright, int axis) {
      return (axis == 0 ? LOG_X0 : LOG_Z0) + (upright - 9);
   }

   /** The upright log (9..11) that goes with a side log. */
   public static int uprightLog(int sideLog) {
      return 9 + (sideLog - LOG_X0) % 3;
   }

   /** 0 = a side log along the x axis, 1 = along the z axis. */
   public static int sideLogAxis(int sideLog) {
      return sideLog >= LOG_Z0 ? 1 : 0;
   }

   public static boolean targetable(int var0) {
      return var0 != 0 && !isFluid(var0);
   }

   public static boolean breakable(int var0) {
      return var0 != 0 && hardness[var0] >= 0.0F;
   }

   public static int[] creativeList() {
      int[] var0 = new int[]{1, 2, 3, 4, 5, 7, 8, 17, 18, 9, 10, 11, 12, 13, 14, 15, 16, 27, 26, 19, 20, 21, 22, 23, 24, 25, 32, 28, 29, 30, 31, 6, 50, 58};
      ArrayList var1 = new ArrayList();

      for (int var5 : var0) {
         var1.add(var5);
      }

      for (int var22 : new int[]{66, 67, 69, 72, 71}) {
         var1.add(var22);
      }

      for (int var7 = 0; var7 < 5; var7++) {
         var1.add(93 + var7 * 4);
      }

      for (int var8 = 0; var8 < 5; var8++) {
         var1.add(113 + var8);
      }

      var1.add(118);
      var1.add(119);
      var1.add(132);
      var1.add(158);
      var1.add(159);
      var1.add(160);
      var1.add(161);
      var1.add(163);
      var1.add(167);
      var1.add(170);

      for (int var23 : new int[]{281, 289, 282, 283, 284, 288, 285, 286, 287, 336, 337, 338, 339, 340, 341, 342, 343, 344, 345, 346, 347, 348, 349, 350}) {
         var1.add(var23);
      }

      for (int var10 = 0; var10 < 5; var10++) {
         var1.add(290 + var10);
      }

      for (int var11 = 0; var11 < 16; var11++) {
         var1.add(34 + var11);
      }

      for (int var24 : new int[]{256, 257, 258, 259, 260, 261, 262, 263, 264, 265, 266, 267, 268, 270, 272, 273, 274, 275, 276, 277, 278, 279, 280}) {
         var1.add(var24);
      }

      for (int var13 = 300; var13 < 336; var13++) {
         var1.add(var13);
      }

      int[] var14 = new int[var1.size()];

      for (int var18 = 0; var18 < var14.length; var18++) {
         var14[var18] = (Integer)var1.get(var18);
      }

      return var14;
   }

   public static int breakTicks(int var0) {
      float var1 = hardness[var0];
      if (var1 <= 0.0F) {
         return 1;
      }

      float var2 = Math.min(var1 * 1.5F, 12.0F);
      return Math.max(1, Math.round(var2 * 20.0F));
   }

   static {
      for (int var0 = 0; var0 < MAX; var0++) {
         name[var0] = "Unknown";
         drop[var0] = var0;
         hardness[var0] = 1.0F;
      }

      name[0] = "Air";
      drop[0] = 0;
      hardness[0] = 0.0F;
      replaceable[0] = true;
      def(1, "Stone", 1.5F, true, 3);
      drop[1] = 4;
      def(2, "Grass Block", 0.6F, true, 1);
      faces(2, 0, 1, 1);
      drop[2] = 3;
      tint[2] = 1;
      def(3, "Dirt", 0.5F, true, 1);
      def(4, "Cobblestone", 2.0F, true, 4);
      def(5, "Oak Planks", 2.0F, true, 10);
      def(6, "Bedrock", -1.0F, true, 11);
      def(7, "Sand", 0.5F, true, 5);
      def(8, "Gravel", 0.6F, true, 6);
      def(9, "Oak Log", 2.0F, true, 7);
      faces(9, 8, 8, 7);
      def(10, "Birch Log", 2.0F, true, 33);
      faces(10, 8, 8, 33);
      def(11, "Spruce Log", 2.0F, true, 31);
      faces(11, 32, 32, 31);

      for (int var3 : new int[]{12, 13, 14}) {
         def(var3, "Leaves", 0.2F, false, 9);
         opacity[var3] = 1;
         tint[var3] = 2;
         drop[var3] = 0;
         cutout[var3] = true;
      }

      name[12] = "Oak Leaves";
      name[13] = "Birch Leaves";
      name[14] = "Spruce Leaves";
      def(15, "Glass", 0.3F, false, 19);
      drop[15] = 0;
      cutout[15] = true;
      def(16, "Bricks", 2.0F, true, 20);
      def(17, "Sandstone", 0.8F, true, 21);
      faces(17, 22, 22, 21);
      def(18, "Snow Block", 0.2F, true, 18);
      def(19, "Coal Ore", 3.0F, true, 12);
      def(20, "Iron Ore", 3.0F, true, 13);
      def(21, "Gold Ore", 3.0F, true, 14);
      def(22, "Diamond Ore", 3.0F, true, 15);
      def(23, "Redstone Ore", 3.0F, true, 16);
      def(24, "Lapis Ore", 3.0F, true, 17);
      def(25, "Glowstone", 0.3F, true, 30);
      emit[25] = 15;
      def(26, "Obsidian", 50.0F, true, 34);
      def(27, "Stone Bricks", 1.5F, true, 35);
      def(28, "Cactus", 0.4F, false, 28);
      faces(28, 29, 29, 28);
      kind[28] = 2;
      box[28] = new double[]{0.0625, 0.0, 0.0625, 0.9375, 1.0, 0.9375};
      coll[28] = new double[]{0.0625, 0.0, 0.0625, 0.9375, 0.9375, 0.9375};
      def(29, "Grass", 0.0F, false, 25);
      kind[29] = 1;
      solid[29] = false;
      coll[29] = null;
      tint[29] = 1;
      replaceable[29] = true;
      drop[29] = 0;
      cutout[29] = true;
      box[29] = new double[]{0.1, 0.0, 0.1, 0.9, 0.8, 0.9};
      def(30, "Poppy", 0.0F, false, 26);
      kind[30] = 1;
      solid[30] = false;
      coll[30] = null;
      cutout[30] = true;
      box[30] = new double[]{0.3, 0.0, 0.3, 0.7, 0.6, 0.7};
      def(31, "Dandelion", 0.0F, false, 27);
      kind[31] = 1;
      solid[31] = false;
      coll[31] = null;
      cutout[31] = true;
      box[31] = new double[]{0.3, 0.0, 0.3, 0.7, 0.6, 0.7};
      def(32, "Torch", 0.0F, false, 36);
      kind[32] = 2;
      solid[32] = false;
      coll[32] = null;
      cutout[32] = true;
      emit[32] = 14;
      box[32] = new double[]{0.4375, 0.0, 0.4375, 0.5625, 0.625, 0.5625};
      def(66, "Crafting Table", 2.5F, true, 38);
      faces(66, 37, 10, 38);
      def(67, "Furnace", 3.5F, true, 39);
      faces(67, 4, 4, 39);
      def(68, "Furnace", 3.5F, true, 40);
      faces(68, 4, 4, 40);
      emit[68] = 13;
      drop[68] = 67;

      for (int var49 : new int[]{69, 70}) {
         def(var49, "Chest", 2.5F, false, 42);
         faces(var49, 41, 41, 42);
         kind[var49] = 2;
         box[var49] = new double[]{0.0625, 0.0, 0.0625, 0.9375, 0.875, 0.9375};
         coll[var49] = box[var49];
         cutout[var49] = false;
      }

      drop[70] = 69;
      def(71, "Monster Spawner", 5.0F, true, 43);
      drop[71] = 0;
      def(72, "Mossy Cobblestone", 2.0F, true, 44);

      for (int var13 = 0; var13 < 16; var13++) {
         int var25 = 73 + var13;
         boolean var38 = var13 >= 8;
         int var50 = var13 & 7;
         int var4 = var50 & 3;
         boolean var5 = var50 >> 2 == 1;
         int var6 = var5 ? new int[]{3, 2, 0, 1}[var4] : var4;
         double var7 = 0.1875;
         double[] var9 = var6 == 0
            ? new double[]{0.0, 0.0, 0.0, 1.0, 1.0, var7}
            : (
               var6 == 1
                  ? new double[]{0.0, 0.0, 1.0 - var7, 1.0, 1.0, 1.0}
                  : (var6 == 2 ? new double[]{1.0 - var7, 0.0, 0.0, 1.0, 1.0, 1.0} : new double[]{0.0, 0.0, 0.0, var7, 1.0, 1.0})
            );
         int var10 = var38 ? 161 : 160;
         def(var25, "Oak Door", 3.0F, false, var10);
         tiles[var25][0] = tiles[var25][1] = 10;
         kind[var25] = 2;
         box[var25] = var9;
         coll[var25] = var9;
         mineClass[var25] = 2;
         drop[var25] = var38 ? 0 : 281;
      }

      for (int var14 = 0; var14 < 4; var14++) {
         int var26 = 89 + var14;
         def(var26, "Torch", 0.0F, false, 36);
         kind[var26] = 5;
         solid[var26] = false;
         coll[var26] = null;
         cutout[var26] = true;
         emit[var26] = 14;
         drop[var26] = 32;
         double[][] var39 = new double[][]{
            {0.34, 0.12, 0.0, 0.66, 0.86, 0.46}, {0.34, 0.12, 0.54, 0.66, 0.86, 1.0}, {0.54, 0.12, 0.34, 1.0, 0.86, 0.66}, {0.0, 0.12, 0.34, 0.46, 0.86, 0.66}
         };
         box[var26] = var39[var14];
      }

      for (int var15 = 0; var15 < 5; var15++) {
         int var27 = STAIR_MAT[var15];

         for (int var40 = 0; var40 < 4; var40++) {
            int var51 = 93 + var15 * 4 + var40;
            def(var51, STAIR_NAME[var15] + " Stairs", hardness[var27] > 0.0F ? hardness[var27] : 2.0F, false, tiles[var27][2]);
            tiles[var51] = (int[])tiles[var27].clone();
            kind[var51] = 4;
            opacity[var51] = 15;
            drop[var51] = 93 + var15 * 4;
            box[var51] = new double[]{0.0, 0.0, 0.0, 1.0, 1.0, 1.0};
            double[][] var56 = new double[][]{{0.0, 0.0, 0.0, 1.0, 0.5, 1.0}, null};
            double[] var58 = var40 == 0
               ? new double[]{0.0, 0.5, 0.0, 1.0, 1.0, 0.5}
               : (
                  var40 == 1
                     ? new double[]{0.0, 0.5, 0.5, 1.0, 1.0, 1.0}
                     : (var40 == 2 ? new double[]{0.5, 0.5, 0.0, 1.0, 1.0, 1.0} : new double[]{0.0, 0.5, 0.0, 0.5, 1.0, 1.0})
               );
            var56[1] = var58;
            collMulti[var51] = var56;
         }

         int var41 = 113 + var15;
         def(var41, STAIR_NAME[var15] + " Slab", hardness[var27] > 0.0F ? hardness[var27] : 2.0F, false, tiles[var27][2]);
         tiles[var41] = (int[])tiles[var27].clone();
         kind[var41] = 2;
         opacity[var41] = 15;
         box[var41] = new double[]{0.0, 0.0, 0.0, 1.0, 0.5, 1.0};
         coll[var41] = box[var41];
      }

      // Update 3.10: the same stairs and slabs upside down / in the top half of the cell
      for (int mat = 0; mat < 5; mat++) {
         int base = STAIR_MAT[mat];
         float hard = hardness[base] > 0.0F ? hardness[base] : 2.0F;
         for (int face = 0; face < 4; face++) {
            int id = STAIR_TOP0 + mat * 4 + face;
            def(id, STAIR_NAME[mat] + " Stairs", hard, false, tiles[base][2]);
            tiles[id] = (int[])tiles[base].clone();
            kind[id] = 4;
            opacity[id] = 15;
            drop[id] = 93 + mat * 4;
            box[id] = new double[]{0.0, 0.0, 0.0, 1.0, 1.0, 1.0};
            double[] step = collMulti[93 + mat * 4 + face][1];
            // mirrored top to bottom: the full slab is the upper half, the step sits in the lower half
            collMulti[id] = new double[][]{{0.0, 0.5, 0.0, 1.0, 1.0, 1.0}, {step[0], 0.0, step[2], step[3], 0.5, step[5]}};
         }

         int sid = SLAB_TOP0 + mat;
         def(sid, STAIR_NAME[mat] + " Slab", hard, false, tiles[base][2]);
         tiles[sid] = (int[])tiles[base].clone();
         kind[sid] = 2;
         opacity[sid] = 15;
         drop[sid] = 113 + mat;
         box[sid] = new double[]{0.0, 0.5, 0.0, 1.0, 1.0, 1.0};
         coll[sid] = box[sid];
      }

      def(118, "Fence", 2.0F, false, 10);
      kind[118] = 6;
      mineClass[118] = 2;
      box[118] = new double[]{0.375, 0.0, 0.375, 0.625, 1.0, 0.625};
      coll[118] = new double[]{0.375, 0.0, 0.375, 0.625, 1.5, 0.625};

      for (int var16 = 0; var16 < 4; var16++) {
         int var28 = 119 + var16;
         def(var28, "Ladder", 0.4F, false, 179);
         kind[var28] = 2;
         solid[var28] = false;
         coll[var28] = null;
         cutout[var28] = true;
         mineClass[var28] = 2;
         drop[var28] = 119;
         double[][] var42 = new double[][]{
            {0.0, 0.0, 0.0, 1.0, 1.0, 0.125}, {0.0, 0.0, 0.875, 1.0, 1.0, 1.0}, {0.875, 0.0, 0.0, 1.0, 1.0, 1.0}, {0.0, 0.0, 0.0, 0.125, 1.0, 1.0}
         };
         box[var28] = var42[var16];
      }

      def(123, "Farmland", 0.6F, false, 1);
      tiles[123][0] = 170;
      kind[123] = 2;
      opacity[123] = 15;
      box[123] = new double[]{0.0, 0.0, 0.0, 1.0, 0.9375, 1.0};
      coll[123] = box[123];
      drop[123] = 3;
      mineClass[123] = 3;

      for (int var17 = 0; var17 < 8; var17++) {
         int var29 = 124 + var17;
         def(var29, "Wheat", 0.0F, false, 171 + var17);
         kind[var29] = 7;
         solid[var29] = false;
         coll[var29] = null;
         cutout[var29] = true;
         drop[var29] = 0;
         box[var29] = new double[]{0.1, 0.0, 0.1, 0.9, 0.25 + 0.09 * var17, 0.9};
      }

      def(132, "TNT", 0.0F, true, 180);
      faces(132, 181, 182, 180);

      for (int var18 = 0; var18 < 8; var18++) {
         int var30 = 133 + var18;
         boolean var43 = var18 >= 4;
         def(var30, "Bed", 0.2F, false, 185);
         tiles[var30][0] = var43 ? 183 : 184;
         tiles[var30][1] = 10;
         kind[var30] = 2;
         box[var30] = new double[]{0.0, 0.0, 0.0, 1.0, 0.5625, 1.0};
         coll[var30] = box[var30];
         drop[var30] = var43 ? 0 : 289;
      }

      // ---- Update 3.6: hydrated farmland, carrots, potatoes, hay bale ----
      def(141, "Farmland", 0.6F, false, 1);
      tiles[141][0] = 203;
      kind[141] = 2;
      opacity[141] = 15;
      box[141] = new double[]{0.0, 0.0, 0.0, 1.0, 0.9375, 1.0};
      coll[141] = box[141];
      drop[141] = 3;
      mineClass[141] = 3;

      for (int var62 = 0; var62 < 16; var62++) {
         int var63 = 142 + var62;
         int var64 = var62 & 7;
         int var65 = var62 < 8 ? 204 + var64 / 2 : 208 + var64 / 2;
         def(var63, var62 < 8 ? "Carrots" : "Potatoes", 0.0F, false, var65);
         kind[var63] = 7;
         solid[var63] = false;
         coll[var63] = null;
         cutout[var63] = true;
         drop[var63] = 0;
         box[var63] = new double[]{0.1, 0.0, 0.1, 0.9, 0.25 + 0.09 * var64, 0.9};
      }

      def(158, "Hay Bale", 0.5F, true, 212);
      faces(158, 213, 213, 212);

      for (int var52 : new int[]{1, 4, 16, 17, 27, 67, 68, 72, 71, 19, 20, 21, 22, 23, 24, 26, 25}) {
         mineClass[var52] = 1;
         needTool[var52] = var52 != 25;
      }

      harvestLevel[20] = 1;
      harvestLevel[24] = 1;
      harvestLevel[21] = 2;
      harvestLevel[22] = 2;
      harvestLevel[23] = 2;
      harvestLevel[26] = 3;

      for (int var53 : new int[]{9, 10, 11, 5, 66, 69, 70, 28}) {
         mineClass[var53] = 2;
      }

      for (int var54 : new int[]{3, 2, 7, 8, 18}) {
         mineClass[var54] = 3;
      }

      for (int var22 = 0; var22 < 5; var22++) {
         int var34 = STAIR_MAT[var22];

         for (int var47 = 0; var47 < 4; var47++) {
            mineClass[93 + var22 * 4 + var47] = mineClass[var34];
            needTool[93 + var22 * 4 + var47] = needTool[var34];
         }

         mineClass[113 + var22] = mineClass[var34];
         needTool[113 + var22] = needTool[var34];
         for (int var47 = 0; var47 < 4; var47++) {
            mineClass[STAIR_TOP0 + var22 * 4 + var47] = mineClass[var34];
            needTool[STAIR_TOP0 + var22 * 4 + var47] = needTool[var34];
         }

         mineClass[SLAB_TOP0 + var22] = mineClass[var34];
         needTool[SLAB_TOP0 + var22] = needTool[var34];
      }

      String[] var23 = new String[]{
         "White", "Orange", "Magenta", "Light Blue", "Yellow", "Lime", "Pink", "Gray", "Light Gray", "Cyan", "Purple", "Blue", "Brown", "Green", "Red", "Black"
      };

      for (int var35 = 0; var35 < 16; var35++) {
         def(34 + var35, var23[var35] + " Wool", 0.8F, true, 48 + var35);
      }

      for (int var36 = 0; var36 < 8; var36++) {
         int var48 = 50 + var36;
         int var55 = 58 + var36;
         def(var48, "Water", -1.0F, false, 23);
         def(var55, "Lava", -1.0F, false, 24);

         for (int var61 : new int[]{var48, var55}) {
            kind[var61] = 3;
            solid[var61] = false;
            coll[var61] = null;
            replaceable[var61] = true;
            drop[var61] = 0;
         }

         opacity[var48] = 3;
         opacity[var55] = 2;
         emit[var55] = 15;
      }

      // ---- Update 3.7: sugar cane, bookshelf, pumpkins, melons, cake, fire ----
      def(159, "Sugar Cane", 0.0F, false, 219);
      kind[159] = 1;
      solid[159] = false;
      coll[159] = null;
      cutout[159] = true;
      box[159] = new double[]{0.1, 0.0, 0.1, 0.9, 1.0, 0.9};

      def(160, "Bookshelf", 1.5F, true, 220);
      tiles[160][0] = tiles[160][1] = 10;
      drop[160] = 347;
      mineClass[160] = 2;

      def(161, "Pumpkin", 1.0F, true, 222);
      faces(161, 221, 221, 222);
      mineClass[161] = 2;

      for (int var70 = 0; var70 < 4; var70++) {
         int var71 = 162 + var70;
         def(var71, "Carved Pumpkin", 1.0F, true, 222);
         faces(var71, 221, 221, 222);
         tiles[var71][2 + var70] = 223;
         drop[var71] = 163;
         mineClass[var71] = 2;
         int var72 = 166 + var70;
         def(var72, "Jack o'Lantern", 1.0F, true, 222);
         faces(var72, 221, 221, 222);
         tiles[var72][2 + var70] = 224;
         emit[var72] = 15;
         drop[var72] = 167;
         mineClass[var72] = 2;
      }

      def(170, "Melon", 1.0F, true, 226);
      faces(170, 225, 225, 226);
      drop[170] = 343;
      mineClass[170] = 2;

      for (int var73 = 0; var73 < 16; var73++) {
         int var74 = 171 + var73;
         int var75 = var73 & 7;
         def(var74, var73 < 8 ? "Pumpkin Stem" : "Melon Stem", 0.0F, false, 227 + var75 / 2);
         kind[var74] = 1;
         solid[var74] = false;
         coll[var74] = null;
         cutout[var74] = true;
         drop[var74] = 0;
         box[var74] = new double[]{0.3, 0.0, 0.3, 0.7, 0.2 + 0.1 * var75, 0.7};
      }

      for (int var76 = 0; var76 < 7; var76++) {
         int var77 = 187 + var76;
         def(var77, "Cake", 0.5F, false, 232);
         tiles[var77][0] = 231;
         tiles[var77][1] = 233;
         if (var76 > 0) {
            tiles[var77][5] = 234;
         }

         kind[var77] = 2;
         box[var77] = new double[]{(1 + 2 * var76) / 16.0, 0.0, 0.0625, 0.9375, 0.5, 0.9375};
         coll[var77] = box[var77];
         drop[var77] = 0;
      }

      def(194, "Fire", 0.0F, false, 235);
      kind[194] = 1;
      solid[194] = false;
      coll[194] = null;
      cutout[194] = true;
      replaceable[194] = true;
      emit[194] = 15;
      drop[194] = 0;
      box[194] = new double[]{0.1, 0.0, 0.1, 0.9, 0.9, 0.9};

      // ---- Update 3.10: logs lying on their side (felled trees). Tiles 246..248 are the bark turned by 90 degrees (TexArt310). ----
      int[] barkTile = new int[]{7, 33, 31};
      int[] endTile = new int[]{8, 8, 32};
      String[] logName = new String[]{"Oak Log", "Birch Log", "Spruce Log"};
      for (int var80 = 0; var80 < 3; var80++) {
         int bark = barkTile[var80];
         int barkTurned = 246 + var80;
         int end = endTile[var80];
         int ax = LOG_X0 + var80;
         int az = LOG_Z0 + var80;
         def(ax, logName[var80], 2.0F, true, bark);
         def(az, logName[var80], 2.0F, true, bark);
         // faces: 0 top, 1 bottom, 2 north (-z), 3 south (+z), 4 east (+x), 5 west (-x): the ends show the rings, the grain runs along the log
         int[] tx = new int[]{barkTurned, barkTurned, barkTurned, barkTurned, end, end};
         int[] tz = new int[]{bark, bark, end, end, barkTurned, barkTurned};
         for (int var81 = 0; var81 < 6; var81++) {
            tiles[ax][var81] = tx[var81];
            tiles[az][var81] = tz[var81];
         }

         drop[ax] = 9 + var80;
         drop[az] = 9 + var80;
         mineClass[ax] = 2;
         mineClass[az] = 2;
      }
   }
}
