package bc;

import java.io.DataInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Random;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

public final class Tex {
   public static final int GRASS_TOP = 0;
   public static final int DIRT = 1;
   public static final int GRASS_SIDE_OVL = 2;
   public static final int STONE = 3;
   public static final int COBBLE = 4;
   public static final int SAND = 5;
   public static final int GRAVEL = 6;
   public static final int LOG_SIDE = 7;
   public static final int LOG_TOP = 8;
   public static final int LEAVES = 9;
   public static final int PLANKS = 10;
   public static final int BEDROCK = 11;
   public static final int COAL = 12;
   public static final int IRON = 13;
   public static final int GOLD = 14;
   public static final int DIAMOND = 15;
   public static final int REDSTONE = 16;
   public static final int LAPIS = 17;
   public static final int SNOW = 18;
   public static final int GLASS = 19;
   public static final int BRICKS = 20;
   public static final int SANDSTONE_SIDE = 21;
   public static final int SANDSTONE_TOP = 22;
   public static final int WATER = 23;
   public static final int LAVA = 24;
   public static final int TALLGRASS = 25;
   public static final int FLOWER_RED = 26;
   public static final int FLOWER_YELLOW = 27;
   public static final int CACTUS_SIDE = 28;
   public static final int CACTUS_TOP = 29;
   public static final int GLOWSTONE = 30;
   public static final int SPRUCE_SIDE = 31;
   public static final int SPRUCE_TOP = 32;
   public static final int BIRCH_SIDE = 33;
   public static final int OBSIDIAN = 34;
   public static final int STONEBRICK = 35;
   public static final int TORCH = 36;
   public static final int CRAFT_TOP = 37;
   public static final int CRAFT_SIDE = 38;
   public static final int FURNACE_FRONT = 39;
   public static final int FURNACE_ON = 40;
   public static final int CHEST_TOP = 41;
   public static final int CHEST_SIDE = 42;
   public static final int SPAWNER = 43;
   public static final int MOSSY = 44;
   public static final int WOOL0 = 48;
   public static final int WHITE = 64;
   public static final int DOOR_LO = 160;
   public static final int DOOR_HI = 161;
   public static final int FARM_TOP = 170;
   public static final int WHEAT0 = 171;
   public static final int LADDER = 179;
   public static final int TNT_SIDE = 180;
   public static final int TNT_TOP = 181;
   public static final int TNT_BOTTOM = 182;
   public static final int BED_TOP_HEAD = 183;
   public static final int BED_TOP_FOOT = 184;
   public static final int BED_SIDE = 185;
   public static final int FRAMES = 16;
   static int[][] waterFrames = new int[16][];
   static int[][] lavaFrames = new int[16][];
   public static int atlas;
   public static int crack;
   public static int clouds;
   public static int sun;
   public static int moon;
   public static int lightmap;
   public static int fontTex;
   public static int[] fontW = new int[96];
   public static int fontCellW;
   public static int fontCellH;
   public static int fontCols;
   public static int lastFrame = -1;
   static final int[][] WOOL = new int[][]{
      {233, 236, 236},
      {240, 118, 19},
      {189, 68, 179},
      {58, 175, 217},
      {248, 197, 39},
      {112, 185, 25},
      {237, 141, 172},
      {62, 68, 71},
      {142, 142, 134},
      {21, 137, 145},
      {121, 42, 172},
      {53, 57, 157},
      {114, 71, 40},
      {84, 109, 27},
      {160, 39, 34},
      {20, 21, 25}
   };
   /** Update 3.12: the atlas holds COLS x COLS tiles of 16 x 16 pixels (it was 16 x 16 tiles = 256 x 256 pixels up to Update 3.11). */
   public static final int COLS = 32;
   public static final int PX = COLS * 16;
   /** The size of one tile in texture coordinates. */
   public static final float TS = 1.0F / COLS;
   static int[] atlasPixels = new int[PX * PX];

   /** Texture coordinate (u / v) of the left / top edge of a tile. */
   public static float tu(int tile) {
      return tile % COLS * TS;
   }

   public static float tv(int tile) {
      return tile / COLS * TS;
   }

   /** Pixel position of a tile's top left corner in the atlas. */
   public static int tx(int tile) {
      return tile % COLS * 16;
   }

   public static int ty(int tile) {
      return tile / COLS * 16;
   }
   static final int[] lmBuf = new int[256];

   static int argb(int var0, int var1, int var2, int var3) {
      return clamp(var0) << 24 | clamp(var1) << 16 | clamp(var2) << 8 | clamp(var3);
   }

   static int clamp(int var0) {
      return var0 < 0 ? 0 : (var0 > 255 ? 255 : var0);
   }

   static int rgb(int var0, int var1, int var2) {
      return argb(255, var0, var1, var2);
   }

   static int[] tStone(long var0, int var2) {
      Random var3 = new Random(var0);
      int[] var4 = new int[256];
      int[] var5 = new int[16];

      for (int var6 = 0; var6 < 16; var6++) {
         var5[var6] = var3.nextInt(17) - 8;
      }

      for (int var9 = 0; var9 < 16; var9++) {
         for (int var7 = 0; var7 < 16; var7++) {
            int var8 = var2 + var5[var9 / 4 * 4 + var7 / 4] + var3.nextInt(13) - 6;
            var4[var9 * 16 + var7] = rgb(var8, var8, var8);
         }
      }

      return var4;
   }

   static int[] tNoise(long var0, int var2, int var3, int var4, int var5) {
      Random var6 = new Random(var0);
      int[] var7 = new int[256];

      for (int var8 = 0; var8 < 256; var8++) {
         int var9 = var6.nextInt(var5 * 2 + 1) - var5;
         var7[var8] = rgb(var2 + var9, var3 + var9, var4 + var9);
      }

      return var7;
   }

   static int[] tOre(long var0, int var2, int var3, int var4) {
      int[] var5 = tStone(1L, 125);
      Random var6 = new Random(var0);

      for (int var7 = 0; var7 < 9; var7++) {
         int var8 = var6.nextInt(14);
         int var9 = var6.nextInt(14);
         int[][] var10 = new int[][]{{0, 0}, {1, 0}, {0, 1}, {1, 1}};

         for (int[] var14 : var10) {
            if (var14[0] != 1 || var14[1] != 1 || !var6.nextBoolean()) {
               int var15 = var6.nextInt(31) - 15;
               var5[(var9 + var14[1]) * 16 + var8 + var14[0]] = rgb(var2 + var15, var3 + var15, var4 + var15);
            }
         }
      }

      return var5;
   }

   static int[] tVoronoi(long var0, int[] var2, int var3, int var4) {
      Random var5 = new Random(var0);
      double[][] var6 = new double[16][2];
      int[] var7 = new int[16];

      for (int var8 = 0; var8 < 4; var8++) {
         for (int var9 = 0; var9 < 4; var9++) {
            var6[var8 * 4 + var9][0] = var9 * 4 + 0.5 + var5.nextDouble() * 3.0;
            var6[var8 * 4 + var9][1] = var8 * 4 + 0.5 + var5.nextDouble() * 3.0;
            var7[var8 * 4 + var9] = var5.nextInt(var4 * 2 + 1) - var4;
         }
      }

      int[] var25 = new int[256];

      for (int var26 = 0; var26 < 16; var26++) {
         for (int var10 = 0; var10 < 16; var10++) {
            double var11 = 1.0E9;
            double var13 = 1.0E9;
            int var15 = 0;

            for (int var16 = 0; var16 < 16; var16++) {
               for (int var17 = -1; var17 <= 1; var17++) {
                  for (int var18 = -1; var18 <= 1; var18++) {
                     double var19 = var6[var16][0] + var17 * 16 - var10;
                     double var21 = var6[var16][1] + var18 * 16 - var26;
                     double var23 = var19 * var19 + var21 * var21;
                     if (var23 < var11) {
                        var13 = var11;
                        var11 = var23;
                        var15 = var16;
                     } else if (var23 < var13) {
                        var13 = var23;
                     }
                  }
               }
            }

            int var27 = var5.nextInt(9) - 4;
            if (Math.sqrt(var13) - Math.sqrt(var11) < 1.0) {
               var25[var26 * 16 + var10] = rgb(var2[0] + var3 + var27, var2[1] + var3 + var27, var2[2] + var3 + var27);
            } else {
               int var28 = var7[var15] + var27;
               var25[var26 * 16 + var10] = rgb(var2[0] + var28, var2[1] + var28, var2[2] + var28);
            }
         }
      }

      return var25;
   }

   static int[] tGrassTop() {
      Random var0 = new Random(11L);
      int[] var1 = new int[256];

      for (int var2 = 0; var2 < 256; var2++) {
         int var3 = 178 + var0.nextInt(41) - 20;
         var1[var2] = rgb(var3, var3, var3);
      }

      return var1;
   }

   static int[] tGrassOverlay() {
      Random var0 = new Random(12L);
      int[] var1 = new int[256];

      for (int var2 = 0; var2 < 16; var2++) {
         int var3 = 2 + var0.nextInt(3);

         for (int var4 = 0; var4 < var3; var4++) {
            int var5 = 178 + var0.nextInt(41) - 20;
            var1[var4 * 16 + var2] = rgb(var5, var5, var5);
         }

         if (var0.nextInt(3) == 0) {
            int var6 = 170 + var0.nextInt(30);
            var1[var3 * 16 + var2] = rgb(var6, var6, var6);
         }
      }

      return var1;
   }

   static int[] tGravel() {
      Random var0 = new Random(21L);
      int[] var1 = new int[256];

      for (int var2 = 0; var2 < 256; var2++) {
         int var3 = 128 + var0.nextInt(41) - 20;
         var1[var2] = rgb(var3, var3 - 2, var3 - 4);
      }

      for (int var8 = 0; var8 < 14; var8++) {
         int var9 = var0.nextInt(15);
         int var4 = var0.nextInt(15);
         int var5 = 85 + var0.nextInt(70);

         for (int var6 = 0; var6 < 2; var6++) {
            for (int var7 = 0; var7 < 2; var7++) {
               var1[(var4 + var6) * 16 + var9 + var7] = rgb(var5 + var0.nextInt(9), var5, var5 - 3);
            }
         }
      }

      return var1;
   }

   static int[] tLogSide(long var0, int var2, int var3, int var4) {
      Random var5 = new Random(var0);
      int[] var6 = new int[256];
      int[] var7 = new int[16];

      for (int var8 = 0; var8 < 16; var8++) {
         var7[var8] = var5.nextInt(29) - 14 - (var5.nextInt(5) == 0 ? 18 : 0);
      }

      for (int var11 = 0; var11 < 16; var11++) {
         for (int var9 = 0; var9 < 16; var9++) {
            int var10 = var7[var9] + var5.nextInt(11) - 5;
            var6[var11 * 16 + var9] = rgb(var2 + var10, var3 + var10, var4 + var10);
         }
      }

      return var6;
   }

   static int[] tLogTop(long var0, int[] var2, int[] var3, int[] var4) {
      Random var5 = new Random(var0);
      int[] var6 = new int[256];

      for (int var7 = 0; var7 < 16; var7++) {
         for (int var8 = 0; var8 < 16; var8++) {
            double var9 = Math.max(Math.abs(var8 - 7.5), Math.abs(var7 - 7.5));
            int[] var11 = var9 >= 6.5 ? var2 : ((int)var9 % 2 == 0 ? var3 : var4);
            int var12 = var5.nextInt(11) - 5;
            var6[var7 * 16 + var8] = rgb(var11[0] + var12, var11[1] + var12, var11[2] + var12);
         }
      }

      return var6;
   }

   static int[] tBirchSide() {
      Random var0 = new Random(31L);
      int[] var1 = new int[256];

      for (int var2 = 0; var2 < 256; var2++) {
         int var3 = var0.nextInt(13) - 6;
         var1[var2] = rgb(214 + var3, 214 + var3, 204 + var3);
      }

      for (int var7 = 0; var7 < 8; var7++) {
         int var8 = var0.nextInt(16);
         int var4 = var0.nextInt(10);
         int var5 = 2 + var0.nextInt(5);

         for (int var6 = 0; var6 < var5 && var4 + var6 < 16; var6++) {
            var1[var8 * 16 + var4 + var6] = rgb(55 + var0.nextInt(20), 52, 48);
         }
      }

      return var1;
   }

   static int[] tLeaves() {
      Random var0 = new Random(41L);
      int[] var1 = new int[256];

      for (int var2 = 0; var2 < 256; var2++) {
         int var3 = 150 + var0.nextInt(76) - 36;
         if (var0.nextFloat() < 0.2F) {
            var1[var2] = 0;
         } else {
            var1[var2] = rgb(var3, var3, var3);
         }
      }

      return var1;
   }

   static int[] tPlanks() {
      Random var0 = new Random(51L);
      int[] var1 = new int[256];

      for (int var2 = 0; var2 < 4; var2++) {
         int var3 = var0.nextInt(16);
         int var4 = var0.nextInt(13) - 6;

         for (int var5 = var2 * 4; var5 < var2 * 4 + 4; var5++) {
            for (int var6 = 0; var6 < 16; var6++) {
               int var7 = var0.nextInt(9) - 4 + var4;
               int[] var8 = new int[]{160, 130, 78};
               if (var5 % 4 == 3) {
                  var7 -= 28;
               }

               if (var6 == var3 && var5 % 4 != 3) {
                  var7 -= 22;
               }

               var1[var5 * 16 + var6] = rgb(var8[0] + var7, var8[1] + var7, var8[2] + var7);
            }
         }
      }

      return var1;
   }

   static int[] tBedrock() {
      Random var0 = new Random(61L);
      int[] var1 = new int[256];

      for (int var2 = 0; var2 < 256; var2++) {
         int var3 = 40 + var0.nextInt(60);
         if (var0.nextInt(6) == 0) {
            var3 += 35;
         }

         var1[var2] = rgb(var3, var3, var3);
      }

      return var1;
   }

   static int[] tGlass() {
      int[] var0 = new int[256];

      for (int var1 = 0; var1 < 16; var1++) {
         for (int var2 = 0; var2 < 16; var2++) {
            if (var2 == 0 || var1 == 0 || var2 == 15 || var1 == 15) {
               var0[var1 * 16 + var2] = argb(255, 205, 232, 244);
            }
         }
      }

      int[] var3 = new int[]{2, 2, 3, 3, 4, 4, 2, 5, 5, 2};

      for (int var4 = 0; var4 < var3.length; var4 += 2) {
         var0[var3[var4 + 1] * 16 + var3[var4]] = argb(210, 255, 255, 255);
      }

      return var0;
   }

   static int[] tBricks() {
      Random var0 = new Random(71L);
      int[] var1 = new int[256];
      int[] var2 = new int[64];

      for (int var3 = 0; var3 < 64; var3++) {
         var2[var3] = var0.nextInt(25) - 12;
      }

      for (int var9 = 0; var9 < 16; var9++) {
         for (int var4 = 0; var4 < 16; var4++) {
            int var5 = var9 / 4;
            int var6 = var5 % 2 * 4;
            int var7 = (var4 + var6) % 16;
            if (var9 % 4 != 3 && var7 % 8 != 7) {
               int var10 = var2[var5 * 2 + var7 / 8] + var0.nextInt(9) - 4;
               var1[var9 * 16 + var4] = rgb(152 + var10, 72 + var10 / 2, 56 + var10 / 2);
            } else {
               int var8 = 180 + var0.nextInt(14) - 7;
               var1[var9 * 16 + var4] = rgb(var8, var8 - 4, var8 - 8);
            }
         }
      }

      return var1;
   }

   static int[] tStoneBrick() {
      Random var0 = new Random(81L);
      int[] var1 = new int[256];

      for (int var2 = 0; var2 < 16; var2++) {
         for (int var3 = 0; var3 < 16; var3++) {
            int var4 = var2 / 8 * 4;
            boolean var5 = var2 % 8 == 7 || (var3 + var4) % 8 == 7;
            int var6 = var0.nextInt(9) - 4;
            int var7 = var5 ? 88 + var6 : 128 + var6 + (var2 % 8 != 0 && (var3 + var4) % 8 != 0 ? 0 : 10);
            var1[var2 * 16 + var3] = rgb(var7, var7, var7);
         }
      }

      return var1;
   }

   static int[] tSandstoneSide() {
      Random var0 = new Random(91L);
      int[] var1 = new int[256];

      for (int var2 = 0; var2 < 16; var2++) {
         for (int var3 = 0; var3 < 16; var3++) {
            int var4 = var0.nextInt(9) - 4;
            if (var2 == 0) {
               var4 += 8;
            }

            if (var2 == 4 || var2 == 15) {
               var4 -= 16;
            }

            if (var2 > 4 && var2 < 15 && var2 % 5 == 0) {
               var4 -= 6;
            }

            var1[var2 * 16 + var3] = rgb(214 + var4, 200 + var4, 152 + var4);
         }
      }

      return var1;
   }

   static int[] tCactusSide() {
      Random var0 = new Random(101L);
      int[] var1 = new int[256];

      for (int var2 = 0; var2 < 16; var2++) {
         for (int var3 = 0; var3 < 16; var3++) {
            int var4 = var0.nextInt(13) - 6;
            if (var3 == 0 || var3 == 15 || var3 == 7 || var3 == 8) {
               var4 -= 18;
            }

            var1[var2 * 16 + var3] = rgb(14 + var4 / 2, 108 + var4, 24 + var4 / 2);
         }
      }

      for (int var5 = 0; var5 < 10; var5++) {
         var1[var0.nextInt(16) * 16 + 1 + var0.nextInt(14)] = rgb(225, 232, 190);
      }

      return var1;
   }

   static int[] tCactusTop() {
      Random var0 = new Random(102L);
      int[] var1 = new int[256];

      for (int var2 = 0; var2 < 16; var2++) {
         for (int var3 = 0; var3 < 16; var3++) {
            int var4 = var0.nextInt(11) - 5;
            boolean var5 = var3 == 0 || var2 == 0 || var3 == 15 || var2 == 15;
            boolean var6 = var3 == 7 || var3 == 8 || var2 == 7 || var2 == 8;
            int var7 = var5 ? 70 : (var6 ? 150 : 118);
            var1[var2 * 16 + var3] = rgb(14 + var4 / 2, var7 + var4, 26 + var4 / 2);
         }
      }

      return var1;
   }

   static int[] tObsidian() {
      Random var0 = new Random(111L);
      int[] var1 = new int[256];

      for (int var2 = 0; var2 < 256; var2++) {
         int var3 = var0.nextInt(13) - 6;
         if (var0.nextInt(14) == 0) {
            var1[var2] = rgb(76 + var3, 52 + var3, 118 + var3);
         } else {
            var1[var2] = rgb(22 + var3, 18 + var3, 36 + var3);
         }
      }

      return var1;
   }

   static int[] tTallGrass() {
      Random var0 = new Random(121L);
      int[] var1 = new int[256];

      for (int var2 = 0; var2 < 8; var2++) {
         int var3 = 1 + var0.nextInt(14);
         int var4 = 5 + var0.nextInt(9);
         int var5 = var0.nextInt(3) - 1;

         for (int var6 = 0; var6 < var4; var6++) {
            int var7 = var3 + var5 * var6 / (var4 / 2 + 1);
            if (var7 >= 0 && var7 <= 15) {
               int var8 = 15 - var6;
               int var9 = 140 + var0.nextInt(70) + var6 * 2;
               var1[var8 * 16 + var7] = rgb(var9, var9, var9);
            }
         }
      }

      return var1;
   }

   static int[] tFlower(int var0, int var1, int var2) {
      int[] var3 = new int[256];
      int var4 = rgb(48, 146, 38);

      for (int var5 = 7; var5 < 16; var5++) {
         var3[var5 * 16 + 7] = var4;
      }

      var3[184] = var4;
      var3[169] = var4;
      var3[198] = var4;
      var3[181] = var4;

      for (int var10 = -2; var10 <= 2; var10++) {
         for (int var6 = -2; var6 <= 2; var6++) {
            if (Math.abs(var6) != 2 || Math.abs(var10) != 2) {
               int var7 = Math.abs(var6) + Math.abs(var10);
               int var8 = 7 + var6;
               int var9 = 4 + var10;
               var3[var9 * 16 + var8] = var7 == 0 ? rgb(250, 220, 40) : rgb(var0 - var7 * 6, var1 - var7 * 6, var2 - var7 * 4);
            }
         }
      }

      return var3;
   }

   static int[] tGlowstone() {
      int[] var0 = tVoronoi(131L, new int[]{190, 140, 70}, -50, 45);

      for (int var1 = 0; var1 < 256; var1++) {
         int var2 = var0[var1];
         int var3 = var2 >> 16 & 0xFF;
         int var4 = var3;
         int var5 = (int)(var3 * 0.78);
         int var6 = (int)(var3 * 0.38);
         var0[var1] = rgb(var4, var5, var6);
      }

      return var0;
   }

   static int[] tTorch() {
      int[] var0 = new int[256];
      Random var1 = new Random(141L);

      for (int var2 = 6; var2 < 16; var2++) {
         for (int var3 = 7; var3 <= 8; var3++) {
            int var4 = var1.nextInt(15) - 7;
            var0[var2 * 16 + var3] = rgb(122 + var4, 90 + var4, 46 + var4 / 2);
         }
      }

      var0[103] = rgb(255, 215, 70);
      var0[104] = rgb(255, 235, 110);
      var0[119] = rgb(255, 150, 30);
      var0[120] = rgb(255, 180, 40);
      return var0;
   }

   static int[] tWool(int var0) {
      Random var1 = new Random(200 + var0);
      int[] var2 = new int[256];
      int[] var3 = new int[64];

      for (int var4 = 0; var4 < 64; var4++) {
         var3[var4] = var1.nextInt(13) - 6;
      }

      for (int var7 = 0; var7 < 16; var7++) {
         for (int var5 = 0; var5 < 16; var5++) {
            int var6 = var3[var7 / 2 * 8 + var5 / 2] + var1.nextInt(5) - 2;
            var2[var7 * 16 + var5] = rgb(WOOL[var0][0] + var6, WOOL[var0][1] + var6, WOOL[var0][2] + var6);
         }
      }

      return var2;
   }

   static int[] waterFrame(int var0) {
      int[] var1 = new int[256];
      double var2 = var0 / 16.0 * Math.PI * 2.0;
      Random var4 = new Random(301L);
      double[] var5 = new double[256];

      for (int var6 = 0; var6 < 256; var6++) {
         var5[var6] = var4.nextDouble() * 2.0 - 1.0;
      }

      for (int var10 = 0; var10 < 16; var10++) {
         for (int var7 = 0; var7 < 16; var7++) {
            double var8 = Math.sin((Math.PI * 2) * var7 / 16.0 + var2) * 0.5
               + Math.sin((Math.PI * 2) * var10 * 2.0 / 16.0 - var2) * 0.5
               + Math.sin((Math.PI * 2) * (var7 + var10) / 16.0 + var2 * 1.0) * 0.5;
            var8 += var5[var10 * 16 + var7] * 0.35;
            var1[var10 * 16 + var7] = rgb((int)(38.0 + var8 * 9.0), (int)(92.0 + var8 * 12.0), (int)(214.0 + var8 * 14.0));
         }
      }

      return var1;
   }

   static int[] lavaFrame(int var0) {
      int[] var1 = new int[256];
      double var2 = var0 / 16.0 * Math.PI * 2.0;
      Random var4 = new Random(302L);
      double[] var5 = new double[256];

      for (int var6 = 0; var6 < 256; var6++) {
         var5[var6] = var4.nextDouble();
      }

      for (int var13 = 0; var13 < 16; var13++) {
         for (int var7 = 0; var7 < 16; var7++) {
            double var8 = 0.5
               + 0.25 * Math.sin((Math.PI * 2) * var7 / 16.0 + var2)
               + 0.25 * Math.sin((Math.PI * 2) * var13 / 16.0 - var2)
               + (var5[var13 * 16 + var7] - 0.5) * 0.7;
            var8 = Math.max(0.0, Math.min(1.0, var8));
            int var10 = (int)(205.0 + var8 * 50.0);
            int var11 = (int)(40.0 + var8 * 175.0);
            int var12 = (int)(5.0 + var8 * 50.0);
            var1[var13 * 16 + var7] = rgb(var10, var11, var12);
         }
      }

      return var1;
   }

   static ByteBuffer toBuffer(int[] var0) {
      ByteBuffer var1 = MemoryUtil.memAlloc(var0.length * 4);

      for (int var5 : var0) {
         var1.put((byte)(var5 >> 16 & 0xFF));
         var1.put((byte)(var5 >> 8 & 0xFF));
         var1.put((byte)(var5 & 0xFF));
         var1.put((byte)(var5 >>> 24 & 0xFF));
      }

      var1.flip();
      return var1;
   }

   static int upload(int var0, int var1, int[] var2, boolean var3, boolean var4) {
      int var5 = GL11.glGenTextures();
      GL11.glBindTexture(3553, var5);
      GL11.glTexParameteri(3553, 10241, var3 ? 9729 : 9728);
      GL11.glTexParameteri(3553, 10240, var3 ? 9729 : 9728);
      int var6 = var4 ? 10497 : 33071;
      GL11.glTexParameteri(3553, 10242, var6);
      GL11.glTexParameteri(3553, 10243, var6);
      ByteBuffer var7 = toBuffer(var2);
      GL11.glPixelStorei(3317, 1);
      GL11.glTexImage2D(3553, 0, 32856, var0, var1, 0, 6408, 5121, var7);
      MemoryUtil.memFree(var7);
      return var5;
   }

   static void blit(int var0, int[] var1) {
      int var2 = tx(var0);
      int var3 = ty(var0);

      for (int var4 = 0; var4 < 16; var4++) {
         System.arraycopy(var1, var4 * 16, atlasPixels, (var3 + var4) * PX + var2, 16);
      }
   }

   public static void init() {
      initPixels();
      atlas = upload(PX, PX, atlasPixels, false, false);
      GL11.glBindTexture(3553, atlas);
      GL11.glTexParameteri(3553, 10241, 9986);
      GL11.glTexParameteri(3553, 33085, 4);
      GL30.glGenerateMipmap(3553);
      crack = upload(160, 16, crackPixels(), false, false);
      clouds = upload(256, 256, cloudPixels(), false, true);
      sun = upload(32, 32, sunPixels(), true, false);
      moon = upload(16, 16, moonPixels(), false, false);
      lightmap = upload(16, 16, new int[256], true, false);
      loadFont();
   }

   /** Update 3.11: paints every tile into {@link #atlasPixels} without touching OpenGL (the LOD colours and the tests read them). */
   static void initPixels() {
      blit(0, tGrassTop());
      blit(1, dirt());
      blit(2, tGrassOverlay());
      blit(3, tStone(1L, 125));
      blit(4, tVoronoi(5L, new int[]{118, 118, 118}, -45, 22));
      blit(5, tNoise(7L, 219, 207, 163, 8));
      blit(6, tGravel());
      blit(7, tLogSide(8L, 104, 83, 50));
      blit(8, tLogTop(9L, new int[]{104, 83, 50}, new int[]{176, 145, 89}, new int[]{150, 120, 70}));
      blit(9, tLeaves());
      blit(10, tPlanks());
      blit(11, tBedrock());
      blit(12, tOre(12L, 22, 22, 22));
      blit(13, tOre(13L, 216, 175, 147));
      blit(14, tOre(14L, 250, 238, 77));
      blit(15, tOre(15L, 92, 219, 213));
      blit(16, tOre(16L, 220, 20, 20));
      blit(17, tOre(17L, 30, 60, 200));
      blit(18, tNoise(18L, 246, 250, 252, 3));
      blit(19, tGlass());
      blit(20, tBricks());
      blit(21, tSandstoneSide());
      blit(22, tNoise(22L, 219, 207, 163, 5));

      for (int var0 = 0; var0 < 16; var0++) {
         waterFrames[var0] = waterFrame(var0);
         lavaFrames[var0] = lavaFrame(var0);
      }

      blit(23, waterFrames[0]);
      blit(24, lavaFrames[0]);
      blit(25, tTallGrass());
      blit(26, tFlower(222, 32, 32));
      blit(27, tFlower(252, 232, 44));
      blit(28, tCactusSide());
      blit(29, tCactusTop());
      blit(30, tGlowstone());
      blit(31, tLogSide(32L, 59, 42, 22));
      blit(32, tLogTop(33L, new int[]{59, 42, 22}, new int[]{128, 98, 58}, new int[]{108, 80, 46}));
      blit(33, tBirchSide());
      blit(34, tObsidian());
      blit(35, tStoneBrick());
      blit(36, tTorch());

      for (int var1 = 0; var1 < 16; var1++) {
         blit(48 + var1, tWool(var1));
      }

      TexExtra.blocks();
   }

   static int[] dirt() {
      Random var0 = new Random(3L);
      int[] var1 = new int[256];

      for (int var2 = 0; var2 < 256; var2++) {
         int var3 = var0.nextInt(25) - 12;
         var1[var2] = rgb(134 + var3, 96 + var3 * 3 / 4, 67 + var3 / 2);
      }

      for (int var5 = 0; var5 < 18; var5++) {
         int var6 = var0.nextInt(256);
         int var4 = var0.nextBoolean() ? 22 : -22;
         var1[var6] = rgb(134 + var4, 96 + var4 * 3 / 4, 67 + var4 / 2);
      }

      return var1;
   }

   public static void animate(int var0) {
      animate(var0, 0.0F);
   }

   /** the last blend that was uploaded: frame * 16 + blend step (-1 = none yet) */
   static int lastBlendKey = -1;

   /** The animation position for a tick plus the part of the tick that has passed: two ticks per frame, 16 frames, endless loop. */
   static float animPos(int tick, float partial) {
      return (tick + partial) / 2.0F;
   }

   /**
    * Update 3.9: smooth water / lava / fire. The three animations used to jump to the next of their 16 frames every two ticks (10 times a
    * second). Now every picture the game draws shows the two neighbouring frames mixed by how far the animation has got, so the flow moves
    * continuously at any frame rate (the frames are the same pattern shifted a little, so the mix looks like motion, not like a double image).
    * The atlas tiles are refreshed whenever the mix moves by 1/16 of a frame; mip maps are rebuilt every fourth refresh.
    */
   public static void animate(int var0, float partial) {
      float pos = animPos(var0, partial);
      int f0 = (int)Math.floor(pos);
      float w = pos - f0;
      int step = Math.min(15, (int)(w * 16.0F));
      int key = (f0 & 15) * 16 + step;
      if (key != lastBlendKey) {
         lastBlendKey = key;
         float mix = step / 16.0F;
         int a = f0 & 15;
         int b = a + 1 & 15;
         GL11.glBindTexture(3553, atlas);
         upload(tx(23), ty(23), blendFrames(waterFrames[a], waterFrames[b], mix));
         upload(tx(24), ty(24), blendFrames(lavaFrames[a], lavaFrames[b], mix));
         upload(tx(235), ty(235), blendFrames(TexArt37.fireFrames[a], TexArt37.fireFrames[b], mix));
         if ((key & 3) == 0) {
            GL30.glGenerateMipmap(3553);
         }
      }
   }

   private static void upload(int x, int y, int[] pixels) {
      ByteBuffer buf = toBuffer(pixels);
      GL11.glTexSubImage2D(3553, 0, x, y, 16, 16, 6408, 5121, buf);
      MemoryUtil.memFree(buf);
   }

   /** The pixel-wise mix of two same-sized ARGB pictures: a at weight 1 - w, b at weight w (every channel, alpha included). */
   public static int[] blendFrames(int[] a, int[] b, float w) {
      int[] out = new int[a.length];
      int wb = Math.round(Math.max(0.0F, Math.min(1.0F, w)) * 256.0F);
      int wa = 256 - wb;
      for (int i = 0; i < a.length; i++) {
         int pa = a[i];
         int pb = b[i];
         int al = ((pa >>> 24) * wa + (pb >>> 24) * wb) >> 8;
         int r = ((pa >> 16 & 255) * wa + (pb >> 16 & 255) * wb) >> 8;
         int g = ((pa >> 8 & 255) * wa + (pb >> 8 & 255) * wb) >> 8;
         int bl = ((pa & 255) * wa + (pb & 255) * wb) >> 8;
         out[i] = al << 24 | r << 16 | g << 8 | bl;
      }

      return out;
   }

   static int[] crackPixels() {
      int[] var0 = new int[2560];
      Random var1 = new Random(555L);
      boolean[][] var2 = new boolean[10][256];
      boolean[] var3 = new boolean[256];
      int var4 = 14;

      for (int var5 = 0; var5 < 10; var5++) {
         int var6 = 2 + var5 * 2;

         for (int var7 = 0; var7 < var6 / 2 + 1; var7++) {
            int var8 = var1.nextInt(16);
            int var9 = var1.nextInt(16);
            if (var5 == 0 && var7 == 0) {
               var8 = 8;
               var9 = 8;
            }

            int var10 = 5 + var1.nextInt(6);
            int var11 = var1.nextInt(3) - 1;
            int var12 = var1.nextInt(3) - 1;
            if (var11 == 0 && var12 == 0) {
               var11 = 1;
            }

            for (int var13 = 0; var13 < var10; var13++) {
               if (var8 >= 0 && var8 < 16 && var9 >= 0 && var9 < 16) {
                  var3[var9 * 16 + var8] = true;
               }

               if (var1.nextInt(3) == 0) {
                  var11 = Math.max(-1, Math.min(1, var11 + var1.nextInt(3) - 1));
               }

               if (var1.nextInt(3) == 0) {
                  var12 = Math.max(-1, Math.min(1, var12 + var1.nextInt(3) - 1));
               }

               var8 += var11;
               var9 += var12;
            }
         }

         var2[var5] = (boolean[])var3.clone();
      }

      for (int var14 = 0; var14 < 10; var14++) {
         for (int var15 = 0; var15 < 16; var15++) {
            for (int var16 = 0; var16 < 16; var16++) {
               if (var2[var14][var15 * 16 + var16]) {
                  var0[var15 * 160 + var14 * 16 + var16] = argb(190, 0, 0, 0);
               }
            }
         }
      }

      return var0;
   }

   static int[] cloudPixels() {
      int var0 = 256;
      double[] var1 = new double[var0 * var0];
      Random var2 = new Random(777L);
      int[] var3 = new int[]{8, 16, 32};
      double[] var4 = new double[]{0.5, 0.33, 0.17};

      for (int var5 = 0; var5 < var3.length; var5++) {
         int var6 = var3[var5];
         double[][] var7 = new double[var6][var6];

         for (int var8 = 0; var8 < var6; var8++) {
            for (int var9 = 0; var9 < var6; var9++) {
               var7[var8][var9] = var2.nextDouble();
            }
         }

         double var30 = (double)var0 / var6;

         for (int var10 = 0; var10 < var0; var10++) {
            for (int var11 = 0; var11 < var0; var11++) {
               double var12 = var11 / var30;
               double var14 = var10 / var30;
               int var16 = (int)var12;
               int var17 = (int)var14;
               double var18 = var12 - var16;
               double var20 = var14 - var17;
               var18 = var18 * var18 * (3.0 - 2.0 * var18);
               var20 = var20 * var20 * (3.0 - 2.0 * var20);
               int var22 = (var16 + 1) % var6;
               int var23 = (var17 + 1) % var6;
               double var24 = var7[var16][var17] * (1.0 - var18) + var7[var22][var17] * var18;
               double var26 = var7[var16][var23] * (1.0 - var18) + var7[var22][var23] * var18;
               var1[var10 * var0 + var11] = var1[var10 * var0 + var11] + (var24 * (1.0 - var20) + var26 * var20) * var4[var5];
            }
         }
      }

      int[] var28 = new int[var0 * var0];

      for (int var29 = 0; var29 < var0 * var0; var29++) {
         var28[var29] = var1[var29] > 0.53 ? argb(255, 255, 255, 255) : 0;
      }

      return var28;
   }

   static int[] sunPixels() {
      int[] var0 = new int[1024];

      for (int var1 = 0; var1 < 32; var1++) {
         for (int var2 = 0; var2 < 32; var2++) {
            double var3 = Math.max(Math.abs(var2 - 15.5), Math.abs(var1 - 15.5));
            if (var3 <= 8.0) {
               var0[var1 * 32 + var2] = argb(255, 255, 250, 205);
            } else if (var3 <= 15.5) {
               var0[var1 * 32 + var2] = argb((int)(90.0 * (1.0 - (var3 - 8.0) / 7.5)), 255, 240, 180);
            }
         }
      }

      return var0;
   }

   static int[] moonPixels() {
      int[] var0 = new int[256];
      Random var1 = new Random(888L);

      for (int var2 = 0; var2 < 256; var2++) {
         int var3 = var1.nextInt(9) - 4;
         var0[var2] = rgb(224 + var3, 226 + var3, 236 + var3);
      }

      int[][] var9 = new int[][]{{3, 3}, {10, 5}, {5, 10}, {11, 11}};

      for (int[] var6 : var9) {
         for (int var7 = 0; var7 < 3; var7++) {
            for (int var8 = 0; var8 < 3; var8++) {
               var0[(var6[1] + var7) * 16 + var6[0] + var8] = rgb(185, 188, 205);
            }
         }
      }

      return var0;
   }

   static float lightCurve(float var0) {
      float var1 = var0 / 15.0F;
      return var1 / (4.0F - 3.0F * var1);
   }

   public static void updateLightmap(float var0, float var1, float var2) {
      for (int var3 = 0; var3 < 16; var3++) {
         for (int var4 = 0; var4 < 16; var4++) {
            float var5 = lightCurve(var3) * var0;
            float var6 = lightCurve(var4);
            float var7 = var0 < 0.99F ? 0.82F + 0.18F * var0 : 1.0F;
            float var8 = var0 < 0.99F ? 1.0F : 1.0F;
            float var9 = var5 * var7 + var6 * 1.0F;
            float var10 = var5 * (var0 < 0.99F ? 0.86F + 0.14F * var0 : 1.0F) + var6 * (0.62F + 0.38F * var6);
            float var11 = var5 * var8 + var6 * (0.35F + 0.65F * var6 * var6);
            float var12 = 0.045F;
            var9 = Math.max(var12 * 0.9F, var9);
            var10 = Math.max(var12 * 0.9F, var10);
            var11 = Math.max(var12 * 1.2F, var11);
            var9 = gamma(var9, var2);
            var10 = gamma(var10, var2);
            var11 = gamma(var11, var2);
            lmBuf[var4 * 16 + var3] = argb(
               255, (int)(Math.min(1.0F, var9) * 255.0F), (int)(Math.min(1.0F, var10) * 255.0F), (int)(Math.min(1.0F, var11) * 255.0F)
            );
         }
      }

      GL11.glBindTexture(3553, lightmap);
      ByteBuffer var13 = toBuffer(lmBuf);
      GL11.glTexSubImage2D(3553, 0, 0, 0, 16, 16, 6408, 5121, var13);
      MemoryUtil.memFree(var13);
   }

   static float gamma(float var0, float var1) {
      if (var1 <= 0.0F) {
         return var0;
      }

      float var2 = 1.0F - var0;
      float var3 = 1.0F - var2 * var2 * var2 * var2;
      return var0 * (1.0F - var1) + var3 * var1;
   }

   static void loadFont() {
      try {
         InputStream var0 = Tex.class.getResourceAsStream("/font.bin");

         try {
            DataInputStream var1 = new DataInputStream(var0);
            fontCols = var1.readUnsignedByte();
            int var2 = var1.readUnsignedByte();
            fontCellW = var1.readUnsignedByte();
            fontCellH = var1.readUnsignedByte();

            for (int var3 = 0; var3 < 96; var3++) {
               fontW[var3] = var1.readUnsignedByte();
            }

            int var11 = fontCols * fontCellW;
            int var4 = var2 * fontCellH;
            byte[] var5 = new byte[var11 * var4];
            var1.readFully(var5);
            int[] var6 = new int[var11 * var4];

            for (int var7 = 0; var7 < var6.length; var7++) {
               var6[var7] = (var5[var7] & 255) << 24 | 16777215;
            }

            fontTex = upload(var11, var4, var6, false, false);
         } catch (Throwable var9) {
            if (var0 != null) {
               try {
                  var0.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }
            }

            throw var9;
         }

         if (var0 != null) {
            var0.close();
         }
      } catch (Exception var10) {
         throw new RuntimeException("font load failed", var10);
      }
   }
}
