package bc;

import java.util.Arrays;
import java.util.Random;

final class TexExtra {
   static final String[] TNT_T = new String[]{"###", ".#.", ".#.", ".#.", ".#."};
   static final String[] TNT_N = new String[]{"#.#", "###", "###", "#.#", "#.#"};
   static final int[] TIER_COL = new int[]{10518858, 8421504, 14474460, 16576075, 6151125};
   static final int[] ARMOR_COL = new int[]{10185532, 14211288, 16576075, 6151125};
   static final int STICK_C = 9859903;
   static final int STICK_D = 6178086;

   static int rgb(int var0, int var1, int var2) {
      return Tex.rgb(var0, var1, var2);
   }

   static int sc(int var0, float var1) {
      int var2 = (int)((var0 >> 16 & 0xFF) * var1);
      int var3 = (int)((var0 >> 8 & 0xFF) * var1);
      int var4 = (int)((var0 & 0xFF) * var1);
      return Tex.rgb(var2, var3, var4);
   }

   static int[] noiseTile(int[] var0, long var1, int var3) {
      Random var4 = new Random(var1);
      int[] var5 = new int[256];

      for (int var6 = 0; var6 < 256; var6++) {
         int var7 = var4.nextInt(var3 * 2 + 1) - var3;
         int var8 = var0[var6];
         var5[var6] = Tex.rgb((var8 >> 16 & 0xFF) + var7, (var8 >> 8 & 0xFF) + var7, (var8 & 0xFF) + var7);
      }

      return var5;
   }

   static void blocks() {
      Tex.blit(64, solid(16777215));
      Tex.blit(37, craftTop());
      Tex.blit(38, craftSide());
      Tex.blit(39, furnaceFront(false));
      Tex.blit(40, furnaceFront(true));
      Tex.blit(41, chestTop());
      Tex.blit(42, chestSide());
      Tex.blit(43, spawner());
      Tex.blit(44, mossy());
      Tex.blit(160, doorTile(false));
      Tex.blit(161, doorTile(true));
      Tex.blit(170, farmTop());

      for (int var0 = 0; var0 < 8; var0++) {
         Tex.blit(171 + var0, wheatTile(var0));
      }

      Tex.blit(179, ladderTile());
      Tex.blit(180, tntSide());
      Tex.blit(181, tntEnd(true));
      Tex.blit(182, tntEnd(false));
      Tex.blit(183, bedTop(true));
      Tex.blit(184, bedTop(false));
      Tex.blit(185, bedSide());
      Tex.blit(203, farmWetTop());

      for (int var3 = 0; var3 < 4; var3++) {
         Tex.blit(204 + var3, rootCropTile(false, var3));
         Tex.blit(208 + var3, rootCropTile(true, var3));
      }

      Tex.blit(212, haySide());
      Tex.blit(213, hayTop());
      TexPaint.blocks(); // Update 3.18: repaints some of the tiles above (mossy cobblestone ...)
      TexArt37.blocks();
      TexArt310.blocks();
      TexArt312.blocks();
      TexArt313.blocks();
      TexArt315.blocks();

      for (int var2 = 256; var2 < Blocks.EXT0; var2++) {
         if (Items.name[var2] != null) {
            int[] var1 = sprite(var2);
            if (var1 != null) {
               Tex.blit(Items.tile[var2], var1);
            }
         }
      }

      TexPaint319.blocks();
   }

   static int[] farmTop() {
      Random var0 = new Random(77L);
      TexExtra.Cv var1 = new TexExtra.Cv();

      for (int var2 = 0; var2 < 16; var2++) {
         for (int var3 = 0; var3 < 16; var3++) {
            int var4 = var2 % 4 == 1 ? 4073492 : 5650463;
            int var5 = var0.nextInt(13) - 6;
            var1.set(var3, var2, rgb((var4 >> 16 & 0xFF) + var5, (var4 >> 8 & 0xFF) + var5, (var4 & 0xFF) + var5));
         }
      }

      return var1.p;
   }

   static int[] wheatTile(int var0) {
      Random var1 = new Random(500 + var0);
      TexExtra.Cv var2 = new TexExtra.Cv();
      int var3 = var0 < 7 ? (var0 < 3 ? 5016106 : 8364080) : 12097082;
      int var4 = var0 < 7 ? (var0 < 3 ? 6465594 : 10336324) : 14467168;
      int var5 = var0 == 7 ? 14 : 3 + var0 * 2;
      int[] var6 = new int[]{1, 4, 7, 10, 13};

      for (int var7 = 0; var7 < var6.length; var7++) {
         int var8 = Math.min(15, var5 - var7 % 2 * 2 + var1.nextInt(2));
         int var9 = var6[var7];

         for (int var10 = 0; var10 < var8; var10++) {
            int var11 = (var10 + var7) % 3 == 0 ? var4 : var3;
            var2.set(var9, 15 - var10, var11);
            if (var0 >= 2 && var7 % 2 == 0) {
               var2.set(var9 + 1, 15 - var10, var3);
            }
         }

         if (var0 == 7) {
            for (int var12 = var8 - 4; var12 < var8; var12++) {
               var2.set(var9 - 1, 15 - var12, var4);
               var2.set(var9 + 1, 15 - var12, 12886602);
            }
         }
      }

      return var2.p;
   }

   // ------------------------------------------------------------------ Update 3.6 art
   /** Hydrated farmland: the dry furrows, but dark and damp. */
   static int[] farmWetTop() {
      int[] var0 = farmTop();
      TexExtra.Cv var1 = new TexExtra.Cv();

      for (int var2 = 0; var2 < 256; var2++) {
         int var3 = var0[var2] & 16777215;
         var1.p[var2] = 0xFF000000 | rgb((int)((var3 >> 16 & 0xFF) * 0.70F), (int)((var3 >> 8 & 0xFF) * 0.72F), (int)((var3 & 0xFF) * 0.80F));
      }

      return var1.p;
   }

   /**
    * Carrot or potato plant for growth group 0..3 (stage / 2). Like wheatTile the plant stands in the lower part
    * of a transparent tile; the ripe group shows the root poking out of the soil.
    */
   static int[] rootCropTile(boolean potato, int group) {
      Random rnd = new Random((potato ? 720 : 610) + group);
      TexExtra.Cv cv = new TexExtra.Cv();
      int dark = potato ? rgb(56, 108, 40) : rgb(48, 120, 34);
      int light = potato ? rgb(92, 150, 58) : rgb(92, 172, 56);
      int[] xs = new int[]{2, 6, 10, 13};
      int h = new int[]{3, 6, 9, 12}[group];

      for (int i = 0; i < xs.length; i++) {
         int x = xs[i];
         int hh = Math.max(2, h - i % 2 * 2 + rnd.nextInt(2));

         for (int y = 0; y < hh; y++) {
            cv.set(x, 15 - y, (y + i) % 3 == 0 ? light : dark);
         }

         if (group >= 1) {
            for (int y = 2; y < hh; y += 2) {
               cv.set(x - 1, 15 - y, dark);
               cv.set(x + 1, 14 - y, light);
               if (potato && group >= 2) {
                  cv.set(x - 2, 15 - y, light);
                  cv.set(x + 2, 14 - y, dark);
               }
            }

            cv.set(x - 1, 15 - hh, light);
            cv.set(x + 1, 15 - hh, light);
            cv.set(x, 14 - hh, light);
            if (!potato && group >= 2) {
               cv.set(x - 1, 14 - hh, dark);
               cv.set(x + 1, 14 - hh, dark);
            }
         }

         if (group == 3) {
            if (potato) {
               cv.set(x, 15, rgb(208, 174, 106));
               cv.set(x + 1, 15, rgb(190, 152, 88));
            } else {
               cv.set(x, 15, rgb(240, 134, 28));
               cv.set(x, 14, rgb(224, 118, 22));
            }
         }
      }

      return cv.p;
   }

   static int[] hayTop() {
      Random rnd = new Random(88L);
      TexExtra.Cv cv = new TexExtra.Cv();

      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            int ring = (int)Math.max(Math.abs(x - 7.5), Math.abs(y - 7.5));
            int base = ring % 2 == 0 ? rgb(208, 174, 60) : rgb(186, 150, 44);
            if (ring >= 7) {
               base = rgb(150, 118, 34);
            }

            int n = rnd.nextInt(17) - 8;
            cv.set(x, y, rgb((base >> 16 & 0xFF) + n, (base >> 8 & 0xFF) + n, (base & 0xFF) + n / 2));
         }
      }

      return cv.p;
   }

   static int[] haySide() {
      Random rnd = new Random(89L);
      TexExtra.Cv cv = new TexExtra.Cv();
      int[] streak = new int[]{rgb(214, 178, 62), rgb(192, 156, 46), rgb(204, 168, 54)};

      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            int base = streak[(x + y / 6) % 3];
            if (y == 0 || y == 15) {
               base = rgb(150, 118, 34);
            }

            if (y == 3 || y == 12) {
               base = rgb(112, 80, 32);
            } else if (y == 4 || y == 11) {
               base = rgb(160, 126, 40);
            }

            int n = rnd.nextInt(15) - 7;
            cv.set(x, y, rgb((base >> 16 & 0xFF) + n, (base >> 8 & 0xFF) + n, (base & 0xFF) + n / 2));
         }
      }

      return cv.p;
   }

   /** Item icons of Update 3.6: carrot, potato, baked potato, bone meal, shears. */
   static int[] newSprite(int var0) {
      TexExtra.Cv cv = new TexExtra.Cv();
      switch (var0) {
         case 336:
            int o1 = rgb(238, 134, 30);
            int o2 = rgb(206, 102, 18);
            int o3 = rgb(252, 172, 72);
            int g1 = rgb(60, 150, 46);
            int g2 = rgb(108, 190, 70);

            for (int i = 0; i < 9; i++) {
               int x = 5 + i;
               int y = 5 + i;
               cv.set(x, y, o1);
               if (i < 6) {
                  cv.set(x + 1, y, o2);
                  cv.set(x, y + 1, o3);
               }

               if (i < 3) {
                  cv.set(x + 1, y + 1, o1);
               }
            }

            cv.set(7, 8, o2);
            cv.set(9, 10, o2);
            cv.set(8, 6, o3);
            cv.line(5, 5, 2, 1, g1);
            cv.line(5, 4, 4, 0, g2);
            cv.line(4, 5, 0, 3, g1);
            cv.line(6, 4, 7, 0, g2);
            cv.line(4, 4, 1, 2, g2);
            break;
         case 337:
            cv.ell(8.0, 8.8, 5.6, 4.4, rgb(196, 160, 98));
            cv.ell(7.4, 7.9, 3.6, 2.5, rgb(214, 180, 118));
            cv.set(5, 9, rgb(150, 116, 66));
            cv.set(9, 11, rgb(150, 116, 66));
            cv.set(11, 8, rgb(150, 116, 66));
            cv.set(8, 6, rgb(150, 116, 66));
            cv.shade();
            break;
         case 338:
            cv.ell(8.0, 8.8, 5.6, 4.4, rgb(190, 118, 46));
            cv.ell(7.4, 7.9, 3.6, 2.5, rgb(214, 148, 70));
            cv.line(5, 7, 10, 7, rgb(246, 226, 150));
            cv.line(6, 8, 10, 8, rgb(255, 240, 176));
            cv.set(5, 10, rgb(110, 66, 28));
            cv.set(10, 11, rgb(110, 66, 28));
            cv.set(12, 9, rgb(110, 66, 28));
            cv.shade();
            break;
         case 339:
            cv.ell(8.0, 11.0, 5.4, 3.2, rgb(236, 236, 228));
            cv.ell(8.0, 9.4, 3.6, 2.4, rgb(250, 250, 244));
            cv.set(5, 11, rgb(206, 206, 198));
            cv.set(10, 12, rgb(206, 206, 198));
            cv.set(7, 8, rgb(255, 255, 255));
            cv.line(9, 3, 12, 6, rgb(232, 230, 214));
            cv.rect(8, 2, 2, 2, rgb(244, 242, 228));
            cv.rect(12, 5, 2, 2, rgb(244, 242, 228));
            cv.shade();
            break;
         default:
            int steel = rgb(196, 198, 202);
            int steel2 = rgb(150, 152, 158);
            int red = rgb(176, 36, 34);
            cv.line(3, 2, 10, 10, steel);
            cv.line(4, 2, 10, 9, steel2);
            cv.line(12, 2, 5, 10, steel);
            cv.line(11, 2, 5, 9, steel2);
            cv.ell(5.0, 12.5, 2.6, 2.6, red);
            cv.ell(11.0, 12.5, 2.6, 2.6, red);
            cv.clr(4, 12, 2, 2);
            cv.clr(10, 12, 2, 2);
            cv.set(8, 8, rgb(90, 90, 96));
      }

      return cv.p;
   }

   static int[] ladderTile() {
      TexExtra.Cv var0 = new TexExtra.Cv();
      int var1 = 8413242;
      int var2 = 5718047;

      for (int var3 = 0; var3 < 16; var3++) {
         var0.set(2, var3, var1);
         var0.set(3, var3, var2);
         var0.set(12, var3, var1);
         var0.set(13, var3, var2);
      }

      for (int var6 : new int[]{1, 5, 9, 13}) {
         for (int var7 = 4; var7 < 12; var7++) {
            var0.set(var7, var6, var1);
            var0.set(var7, var6 + 1, var2);
         }
      }

      return var0.p;
   }

   static int[] tntSide() {
      Random var0 = new Random(91L);
      TexExtra.Cv var1 = new TexExtra.Cv();

      for (int var2 = 0; var2 < 16; var2++) {
         for (int var3 = 0; var3 < 16; var3++) {
            int var4 = var0.nextInt(9) - 4;
            int var5 = var2 != 0 && var2 != 15 && var3 != 0 && var3 != 15 ? 12991532 : 11020830;
            var1.set(var3, var2, rgb((var5 >> 16 & 0xFF) + var4, (var5 >> 8 & 0xFF) + var4, (var5 & 0xFF) + var4));
         }
      }

      var1.rect(0, 4, 16, 8, 14472904);

      for (int var6 = 0; var6 < 16; var6++) {
         var1.set(var6, 4, 12104356);
         var1.set(var6, 11, 12104356);
      }

      String[][] var7 = new String[][]{TNT_T, TNT_N, TNT_T};

      for (int var8 = 0; var8 < 3; var8++) {
         for (int var9 = 0; var9 < 5; var9++) {
            for (int var10 = 0; var10 < 3; var10++) {
               if (var7[var8][var9].charAt(var10) == '#') {
                  var1.set(2 + var8 * 4 + var10 + 1, 5 + var9, 2763306);
               }
            }
         }
      }

      return var1.p;
   }

   static int[] tntEnd(boolean var0) {
      Random var1 = new Random(92L);
      TexExtra.Cv var2 = new TexExtra.Cv();

      for (int var3 = 0; var3 < 16; var3++) {
         for (int var4 = 0; var4 < 16; var4++) {
            int var5 = var1.nextInt(9) - 4;
            int var6 = 12072746;
            var2.set(var4, var3, rgb((var6 >> 16 & 0xFF) + var5, (var6 >> 8 & 0xFF) + var5, (var6 & 0xFF) + var5));
         }
      }

      if (var0) {
         var2.ell(8.0, 8.0, 4.0, 4.0, 11578012);
         var2.ell(8.0, 8.0, 2.5, 2.5, 9078136);
         var2.set(8, 8, 2763306);
      }

      return var2.p;
   }

   static int[] bedTop(boolean var0) {
      Random var1 = new Random(93L);
      TexExtra.Cv var2 = new TexExtra.Cv();

      for (int var3 = 0; var3 < 16; var3++) {
         for (int var4 = 0; var4 < 16; var4++) {
            int var5 = var1.nextInt(9) - 4;
            int var6 = var4 != 0 && var4 != 15 ? 11544608 : 9312276;
            var2.set(var4, var3, rgb((var6 >> 16 & 0xFF) + var5, (var6 >> 8 & 0xFF) + var5, (var6 & 0xFF) + var5));
         }
      }

      if (var0) {
         var2.rect(2, 1, 12, 5, 15131354);

         for (int var7 = 2; var7 < 14; var7++) {
            var2.set(var7, 5, 13157562);
         }

         for (int var8 = 1; var8 < 6; var8++) {
            var2.set(2, var8, 13157562);
            var2.set(13, var8, 13157562);
         }
      } else {
         for (int var9 = 0; var9 < 16; var9++) {
            var2.set(var9, 15, 7999502);
         }
      }

      return var2.p;
   }

   static int[] bedSide() {
      int[] var0 = Tex.tPlanks();
      TexExtra.Cv var1 = new TexExtra.Cv();

      for (int var2 = 0; var2 < 16; var2++) {
         for (int var3 = 0; var3 < 16; var3++) {
            var1.set(var3, var2, var2 < 5 ? (var2 == 4 ? 8002068 : 11151906) : var0[var2 * 16 + var3] & 16777215);
         }
      }

      return var1.p;
   }

   static int[] doorTile(boolean var0) {
      TexExtra.Cv var1 = new TexExtra.Cv();
      System.arraycopy(Tex.tPlanks(), 0, var1.p, 0, 256);
      int var2 = rgb(66, 46, 24);
      int var3 = rgb(168, 136, 84);
      int var4 = rgb(120, 92, 52);

      for (int var5 = 0; var5 < 16; var5++) {
         var1.set(0, var5, var2);
         var1.set(15, var5, var2);
         var1.set(1, var5, var4);
         var1.set(14, var5, var4);
         var1.set(var0 ? 0 : 15, var0 ? 0 : 0, var2);
      }

      for (int var11 = 0; var11 < 16; var11++) {
         var1.set(var11, var0 ? 0 : 15, var2);
      }

      if (var0) {
         int var12 = rgb(52, 38, 22);

         for (int var6 = 0; var6 < 2; var6++) {
            for (int var7 = 0; var7 < 2; var7++) {
               int var8 = 3 + var7 * 5 + (var7 == 1 ? 1 : 0);
               int var9 = 3 + var6 * 5;
               var1.rect(var8, var9, 4, 4, var12);

               for (int var10 = 0; var10 < 4; var10++) {
                  var1.set(var8 + var10, var9, var2);
                  var1.set(var8, var9 + var10, var2);
                  var1.set(var8 + var10, var9 + 3, var3);
                  var1.set(var8 + 3, var9 + var10, var3);
               }
            }
         }
      } else {
         for (int var13 = 0; var13 < 2; var13++) {
            int var14 = 2 + var13 * 7;

            for (int var15 = 3; var15 < 13; var15++) {
               var1.set(var15, var14, var2);
               var1.set(var15, var14 + 5, var3);
            }

            for (int var16 = 0; var16 <= 5; var16++) {
               var1.set(3, var14 + var16, var2);
               var1.set(12, var14 + var16, var3);
            }
         }

         var1.rect(11, 0, 2, 2, rgb(210, 210, 214));
      }

      var1.set(12, var0 ? 15 : 1, rgb(215, 215, 220));
      var1.set(12, var0 ? 14 : 2, rgb(150, 150, 155));
      return var1.p;
   }

   static int[] solid(int var0) {
      int[] var1 = new int[256];
      Arrays.fill(var1, 0xFF000000 | var0);
      return var1;
   }

   static int[] craftTop() {
      int[] var0 = Tex.tPlanks();
      TexExtra.Cv var1 = new TexExtra.Cv();
      System.arraycopy(var0, 0, var1.p, 0, 256);
      int var2 = rgb(70, 50, 28);

      for (int var3 = 2; var3 < 14; var3++) {
         var1.set(var3, 2, var2);
         var1.set(var3, 13, var2);
         var1.set(2, var3, var2);
         var1.set(13, var3, var2);
      }

      for (int var4 = 3; var4 < 13; var4++) {
         var1.set(var4, 6, rgb(110, 85, 50));
         var1.set(var4, 9, rgb(110, 85, 50));
         var1.set(6, var4, rgb(110, 85, 50));
         var1.set(9, var4, rgb(110, 85, 50));
      }

      return var1.p;
   }

   static int[] craftSide() {
      TexExtra.Cv var0 = new TexExtra.Cv();
      System.arraycopy(Tex.tPlanks(), 0, var0.p, 0, 256);
      int var1 = rgb(60, 42, 22);
      var0.rect(0, 0, 16, 2, rgb(96, 70, 40));
      var0.line(2, 6, 7, 6, rgb(190, 190, 195));
      var0.line(2, 7, 7, 7, rgb(150, 150, 155));
      var0.line(7, 4, 7, 9, var1);
      var0.line(11, 5, 11, 12, rgb(120, 80, 40));
      var0.rect(9, 4, 5, 2, rgb(120, 120, 125));
      var0.line(4, 10, 6, 13, rgb(170, 170, 175));
      return var0.p;
   }

   static int[] furnaceFront(boolean var0) {
      int[] var1 = Tex.tVoronoi(5L, new int[]{118, 118, 118}, -45, 22);
      TexExtra.Cv var2 = new TexExtra.Cv();
      System.arraycopy(var1, 0, var2.p, 0, 256);
      var2.rect(3, 9, 10, 6, rgb(25, 25, 25));
      var2.rect(3, 9, 10, 1, rgb(60, 60, 60));
      if (var0) {
         Random var3 = new Random(4L);

         for (int var4 = 4; var4 < 12; var4++) {
            for (int var5 = 11; var5 < 15; var5++) {
               var2.set(var4, var5, rgb(230 + var3.nextInt(25), 110 + var3.nextInt(80), 20));
            }
         }

         var2.rect(5, 10, 6, 1, rgb(255, 200, 80));
      }

      var2.rect(5, 2, 6, 3, rgb(40, 40, 40));
      var2.rect(6, 3, 4, 1, rgb(80, 80, 80));
      return var2.p;
   }

   static int[] chestTop() {
      TexExtra.Cv var0 = new TexExtra.Cv();
      System.arraycopy(noiseTile(Tex.tPlanks(), 3L, 0), 0, var0.p, 0, 256);
      int[] var1 = Tex.tPlanks();

      for (int var2 = 0; var2 < 256; var2++) {
         var0.p[var2] = sc(var1[var2] & 16777215, 0.85F);
      }

      int var4 = rgb(50, 32, 12);

      for (int var3 = 0; var3 < 16; var3++) {
         var0.set(var3, 0, var4);
         var0.set(var3, 15, var4);
         var0.set(0, var3, var4);
         var0.set(15, var3, var4);
      }

      return var0.p;
   }

   static int[] chestSide() {
      TexExtra.Cv var0 = new TexExtra.Cv();
      int[] var1 = Tex.tPlanks();

      for (int var2 = 0; var2 < 256; var2++) {
         var0.p[var2] = sc(var1[var2] & 16777215, 0.85F);
      }

      int var4 = rgb(50, 32, 12);

      for (int var3 = 0; var3 < 16; var3++) {
         var0.set(var3, 0, var4);
         var0.set(var3, 15, var4);
         var0.set(0, var3, var4);
         var0.set(15, var3, var4);
         var0.set(var3, 5, var4);
      }

      var0.rect(7, 3, 2, 5, rgb(205, 205, 215));
      var0.rect(7, 6, 2, 1, rgb(130, 130, 140));
      var0.rect(7, 4, 2, 1, rgb(60, 60, 70));
      return var0.p;
   }

   static int[] spawner() {
      TexExtra.Cv var0 = new TexExtra.Cv();
      System.arraycopy(Tex.tVoronoi(9L, new int[]{40, 40, 48}, -20, 8), 0, var0.p, 0, 256);
      int var1 = rgb(120, 125, 150);

      for (int var2 = 0; var2 < 16; var2++) {
         var0.set(var2, 0, var1);
         var0.set(var2, 15, var1);
         var0.set(0, var2, var1);
         var0.set(15, var2, var1);
      }

      for (int var6 = 0; var6 < 16; var6++) {
         var0.set(var6, 5, sc(var1, 0.8F));
         var0.set(var6, 10, sc(var1, 0.8F));
         var0.set(5, var6, sc(var1, 0.8F));
         var0.set(10, var6, sc(var1, 0.8F));
      }

      Random var7 = new Random(3L);

      for (int var3 = 0; var3 < 30; var3++) {
         int var4 = 1 + var7.nextInt(14);
         int var5 = 1 + var7.nextInt(14);
         if (var4 % 5 != 0 && var5 % 5 != 0) {
            var0.set(var4, var5, rgb(10, 10, 15));
         }
      }

      return var0.p;
   }

   static int[] mossy() {
      int[] var0 = Tex.tVoronoi(5L, new int[]{118, 118, 118}, -45, 22);
      Random var1 = new Random(77L);

      for (int var2 = 0; var2 < 256; var2++) {
         if (var1.nextInt(100) < 38) {
            int var3 = var1.nextInt(30) - 15;
            var0[var2] = rgb(60 + var3, 110 + var3, 45 + var3 / 2);
         }
      }

      return var0;
   }

   static int[] sprite(int var0) {
      if (var0 >= Decor315.BED_ITEM0 && var0 < 1000) {
         return TexArt315.sprite(var0);
      }

      if (var0 >= Items.QUARTZ) {
         return TexArt312.sprite(var0);
      }

      if (var0 >= 336 && var0 <= 340) {
         return newSprite(var0);
      }

      if (var0 >= 341 && var0 <= 350) {
         return TexArt37.sprite(var0);
      }

      TexExtra.Cv var1 = new TexExtra.Cv();
      switch (var0) {
         case 256:
            var1.line(3, 13, 12, 4, 9859903);
            var1.line(4, 13, 13, 4, 6178086);
            break;
         case 257:
            var1.ell(7.5, 8.5, 5.0, 4.5, 2829103);
            var1.set(6, 6, 5921378);
            var1.set(7, 6, 4868690);
            var1.set(10, 8, 4868690);
            var1.set(5, 9, 1776415);
            break;
         case 258:
            ingot(var1, 14211288);
            break;
         case 259:
            ingot(var1, 16576075);
            break;
         case 260:
            int var17 = 6151125;
            var1.rect(5, 4, 6, 2, sc(var17, 1.2F));
            var1.rect(4, 6, 8, 3, var17);
            var1.rect(5, 9, 6, 2, sc(var17, 0.85F));
            var1.rect(6, 11, 4, 1, sc(var17, 0.7F));
            var1.rect(7, 12, 2, 1, sc(var17, 0.6F));
            var1.set(6, 5, 16777215);
            var1.set(5, 7, 13631487);
            break;
         case 261:
            int var16 = 10511418;
            var1.rect(4, 4, 8, 9, var16);
            var1.rect(2, 4, 3, 4, var16);
            var1.rect(11, 4, 3, 4, var16);
            var1.rect(5, 12, 6, 1, sc(var16, 0.8F));
            var1.rect(6, 5, 4, 1, sc(var16, 1.2F));
            break;
         case 262:
            var1.line(3, 13, 12, 3, 15790320);
            var1.line(4, 13, 13, 4, 13684952);
            var1.line(5, 12, 11, 3, 16777215);
            var1.line(4, 12, 9, 12, 15263976);
            var1.line(3, 13, 2, 14, 13158600);
            break;
         case 263:
            var1.line(2, 13, 6, 9, 15921906);
            var1.line(6, 9, 9, 10, 14737632);
            var1.line(9, 10, 13, 4, 15921906);
            var1.line(13, 4, 13, 3, 13684944);
            break;
         case 264:
            var1.line(4, 12, 11, 5, 15263964);
            var1.line(5, 12, 12, 5, 13619136);
            var1.rect(2, 11, 3, 3, 15263964);
            var1.rect(11, 2, 3, 3, 15263964);
            var1.set(3, 12, 16777215);
            var1.set(12, 3, 16777215);
            break;
         case 265:
            Random var15 = new Random(5L);

            for (int var24 = 0; var24 < 60; var24++) {
               int var28 = 8 + (int)(var15.nextGaussian() * 2.4);
               int var35 = 10 + (int)(var15.nextGaussian() * 1.6);
               if (var35 > 3 && Math.abs(var28 - 8) < 6) {
                  int var37 = 70 + var15.nextInt(60);
                  var1.set(var28, var35, rgb(var37, var37, var37 + 4));
               }
            }
            break;
         case 266:
            var1.rect(6, 4, 4, 2, 4868688);
            var1.rect(5, 6, 6, 4, 3816000);
            var1.rect(6, 10, 4, 2, 2763312);
            var1.set(7, 5, 7368824);
            var1.set(6, 7, 5789791);
            break;
         case 267:
            var1.line(3, 13, 12, 4, 9859903);
            var1.line(12, 4, 14, 2, 9079440);
            var1.rect(12, 2, 3, 3, 10132128);
            var1.line(2, 12, 4, 10, 15790320);
            var1.line(3, 14, 5, 12, 15790320);
            var1.line(2, 14, 4, 14, 14737632);
            break;
         case 268:
            for (int var13 = 0; var13 <= 20; var13++) {
               double var22 = -1.208304866765305 + var13 * 2.41660973353061 / 20.0;
               int var33 = (int)Math.round(4.0 + 8.0 * Math.sin(var22) * 0.8 + 3.0);
               int var36 = (int)Math.round(8.0 - 7.0 * Math.cos(var22 + (Math.PI / 2) - (Math.PI / 2)) * 0.0 + 7.0 * Math.sin(var22) * 0.0);
            }

            for (int var14 = 0; var14 < 16; var14++) {
               double var23 = (var14 - 7.5) / 7.5;
               int var34 = (int)Math.round(11.0 - 6.0 * var23 * var23);
               var1.set(var34, var14, 9398840);
               var1.set(var34 + 1, var14, 6178086);
            }

            var1.line(13, 1, 13, 14, 15263976);
            break;
         case 269:
         case 271:
         default:
            if (Items.isHoe(var0)) {
               tool(var1, 5, var0 - 290);
            } else if (Items.isTool(var0)) {
               tool(var1, Items.toolClass(var0), Items.toolTier(var0));
            } else {
               if (!Items.isArmor(var0)) {
                  return null;
               }

               armor(var1, Items.armorSlot(var0), Items.armorMat(var0));
            }
            break;
         case 270:
            var1.ell(7.5, 9.0, 4.8, 4.6, 13639712);
            var1.ell(6.5, 8.0, 2.0, 2.0, 15222856);
            var1.line(8, 5, 9, 3, 7031330);
            var1.rect(9, 3, 3, 1, 4102699);
            var1.set(11, 4, 4102699);
            break;
         case 272:
            meat(var1, 15245472, 13135984, false);
            break;
         case 273:
            meat(var1, 12086340, 9062956, false);
            break;
         case 274:
            meat(var1, 12597304, 9445408, false);
            break;
         case 275:
            meat(var1, 9062956, 6172698, false);
            break;
         case 276:
            meat(var1, 15777968, 13144192, true);
            break;
         case 277:
            meat(var1, 14259274, 10512940, true);
            break;
         case 278:
            meat(var1, 13654104, 10500152, false);
            break;
         case 279:
            meat(var1, 10508856, 7355426, false);
            break;
         case 280:
            meat(var1, 8030788, 5661230, false);
            var1.set(6, 8, 9058874);
            var1.set(9, 10, 9058874);
            break;
         case 281:
            int var12 = rgb(66, 46, 24);
            int var21 = rgb(158, 124, 76);
            int var27 = rgb(184, 150, 96);
            var1.rect(4, 1, 8, 14, var21);

            for (int var31 = 1; var31 < 15; var31++) {
               var1.set(4, var31, var12);
               var1.set(11, var31, var12);
            }

            for (int var32 = 4; var32 < 12; var32++) {
               var1.set(var32, 1, var12);
               var1.set(var32, 14, var12);
               var1.set(var32, 8, var12);
            }

            var1.rect(5, 2, 3, 2, rgb(60, 44, 26));
            var1.rect(8, 2, 3, 2, rgb(60, 44, 26));
            var1.rect(5, 5, 3, 2, rgb(60, 44, 26));
            var1.rect(8, 5, 3, 2, rgb(60, 44, 26));
            var1.rect(5, 10, 6, 3, var27);
            var1.set(10, 8, rgb(220, 220, 224));
            break;
         case 282:
         case 283:
         case 284:
            int var11 = 11842748;
            var1.rect(3, 4, 10, 1, sc(var11, 1.15F));

            for (int var20 = 5; var20 <= 12; var20++) {
               int var26 = 3 + (var20 - 5) / 3;
               int var30 = 12 - (var20 - 5) / 3;
               var1.rect(var26, var20, var30 - var26 + 1, 1, var20 % 2 == 0 ? var11 : sc(var11, 0.9F));
            }

            if (var0 == 283) {
               var1.rect(4, 5, 8, 2, 3829728);
            } else if (var0 == 284) {
               var1.rect(4, 5, 8, 2, 15763998);
            } else {
               var1.rect(4, 5, 8, 1, 3816004);
            }

            var1.line(3, 4, 2, 2, var11);
            var1.line(12, 4, 13, 2, var11);
            var1.line(2, 2, 13, 2, sc(var11, 0.8F));
            break;
         case 285:
            int[][] var10 = new int[][]{{4, 5}, {8, 4}, {11, 7}, {6, 9}, {10, 11}, {4, 12}, {7, 7}, {12, 4}};

            for (int var19 = 0; var19 < var10.length; var19++) {
               var1.set(var10[var19][0], var10[var19][1], var19 % 2 == 0 ? 6195754 : 8035898);
               var1.set(var10[var19][0] + 1, var10[var19][1], 4086300);
            }
            break;
         case 286:
            int var9 = 13149760;
            int var18 = 14861406;
            int var25 = 9335848;

            for (int var29 = 0; var29 < 3; var29++) {
               int var6 = 4 + var29 * 3;
               var1.line(var6, 14, var6 + 4 - var29, 4, var29 == 1 ? var25 : var9);

               for (int var7 = 2; var7 < 8; var7 += 2) {
                  var1.set(var6 + 4 - var29 - 1 + 0, var7, var18);
                  var1.set(var6 + 4 - var29 + 1, var7 + 1, var9);
               }
            }

            var1.rect(3, 12, 8, 1, 8018996);
            break;
         case 287:
            var1.ell(8.0, 9.5, 6.2, 3.6, 12879414);
            var1.ell(7.5, 8.2, 5.0, 2.2, 14459992);

            for (int var5 : new int[]{5, 8, 11}) {
               var1.line(var5, 6, var5 + 1, 9, 11038762);
            }
            break;
         case 288:
            int var2 = 12105922;
            var1.line(3, 13, 3, 8, var2);
            var1.line(3, 13, 9, 13, var2);
            var1.line(9, 13, 9, 10, var2);
            var1.line(4, 8, 6, 6, var2);
            var1.line(4, 9, 6, 7, sc(var2, 0.8F));
            var1.ell(11.0, 6.0, 3.2, 3.2, 3816004);
            var1.set(10, 5, 6974072);
            var1.set(11, 4, 6974072);
            var1.set(12, 8, 2236970);
            break;
         case 289:
            var1.rect(2, 6, 12, 5, 11544608);
            var1.rect(2, 6, 4, 5, 15131354);
            var1.rect(2, 11, 2, 3, 8018996);
            var1.rect(12, 11, 2, 3, 8018996);
            var1.rect(2, 11, 12, 1, 9312276);
      }

      if (var0 != 262 && var0 != 263) {
         var1.shade();
      }

      return var1.p;
   }

   static void ingot(TexExtra.Cv var0, int var1) {
      var0.rect(3, 6, 10, 5, var1);
      var0.rect(5, 4, 8, 2, sc(var1, 1.2F));
      var0.rect(3, 11, 9, 1, sc(var1, 0.7F));
      var0.rect(12, 5, 1, 6, sc(var1, 0.8F));
      var0.set(5, 5, 16777215);
   }

   static void meat(TexExtra.Cv var0, int var1, int var2, boolean var3) {
      if (var3) {
         var0.ell(9.0, 6.5, 4.2, 3.6, var1);
         var0.line(8, 9, 4, 13, 15263964);
         var0.set(3, 13, 15263964);
         var0.set(4, 14, 15263964);
         var0.set(3, 14, 13619136);
         var0.set(7, 5, sc(var1, 1.2F));
         var0.set(10, 8, var2);
      } else {
         var0.ell(8.0, 8.5, 5.6, 4.2, var1);
         var0.ell(6.5, 7.5, 2.4, 1.6, sc(var1, 1.2F));
         var0.set(10, 10, var2);
         var0.set(11, 9, var2);
         var0.set(5, 10, var2);
         var0.set(11, 7, var2);
      }
   }

   static void tool(TexExtra.Cv var0, int var1, int var2) {
      int var3 = TIER_COL[var2];
      int var4 = sc(var3, 0.7F);
      int var5 = sc(var3, 1.25F);
      switch (var1) {
         case 1:
            var0.line(2, 14, 11, 5, 9859903);
            var0.line(3, 14, 12, 5, 6178086);

            for (int var33 = -6; var33 <= 6; var33++) {
               double var34 = var33 / 6.0 * 1.15;
               double var35 = 11.5;
               double var11 = 4.5;
               double var13 = 6.5;
               double var15 = var35 - var13 * Math.cos(Math.PI / 4);
               double var17 = var11 + var13 * Math.sin(Math.PI / 4);
               double var19 = Math.cos(Math.PI / 4);
               double var21 = -Math.sin(Math.PI / 4);
               double var23 = Math.sin(Math.PI / 4);
               double var25 = Math.cos(Math.PI / 4);

               for (double var27 = 0.0; var27 <= 1.6; var27 += 0.5) {
                  double var29 = var13 - var27;
                  int var31 = (int)Math.round(var15 + var29 * (Math.cos(var34) * var19 + Math.sin(var34) * var23));
                  int var32 = (int)Math.round(var17 + var29 * (Math.cos(var34) * var21 + Math.sin(var34) * var25));
                  var0.set(var31, var32, var27 < 0.6 ? var5 : (var27 < 1.2 ? var3 : var4));
               }
            }
            break;
         case 2:
            var0.line(2, 14, 11, 5, 9859903);
            var0.line(3, 14, 12, 5, 6178086);
            String[] var6 = new String[]{"..HHH.", ".HHHHH", "HHHHHD", "HHHHDD", ".HHDD.", "..DD.."};

            for (int var7 = 0; var7 < var6.length; var7++) {
               for (int var8 = 0; var8 < 6; var8++) {
                  char var9 = var6[var7].charAt(var8);
                  if (var9 == 'H') {
                     var0.set(7 + var8, 1 + var7, (var8 + var7) % 3 == 0 ? var5 : var3);
                  } else if (var9 == 'D') {
                     var0.set(7 + var8, 1 + var7, var4);
                  }
               }
            }
            break;
         case 3:
         default:
            var0.line(2, 14, 10, 6, 9859903);
            var0.line(3, 14, 11, 6, 6178086);
            var0.ell(11.5, 4.5, 3.0, 3.6, var3);
            var0.ell(10.8, 3.8, 1.4, 1.6, var5);
            var0.set(13, 6, var4);
            var0.set(12, 7, var4);
            break;
         case 4:
            var0.line(3, 12, 1, 14, 9859903);
            var0.line(4, 12, 2, 14, 6178086);
            var0.line(2, 9, 6, 13, 7034160);
            var0.line(3, 9, 7, 13, 4864546);
            var0.line(5, 10, 13, 2, var3);
            var0.line(6, 10, 14, 2, var3);
            var0.line(5, 9, 13, 1, var5);
            var0.line(7, 10, 14, 3, var4);
            var0.set(14, 1, var5);
            break;
         case 5:
            var0.line(2, 14, 11, 5, 9859903);
            var0.line(3, 14, 12, 5, 6178086);
            var0.rect(8, 2, 6, 2, var3);
            var0.rect(8, 2, 6, 1, var5);
            var0.rect(12, 4, 2, 3, var4);
            var0.set(13, 7, var4);
      }
   }

   static void armor(TexExtra.Cv var0, int var1, int var2) {
      int var3 = ARMOR_COL[var2];
      if (var2 == 0) {
         var3 = 10511418;
      }

      int var4 = sc(var3, 1.2F);
      int var5 = sc(var3, 0.75F);
      switch (var1) {
         case 0:
            var0.rect(3, 3, 10, 6, var3);
            var0.rect(3, 9, 3, 4, var3);
            var0.rect(10, 9, 3, 4, var3);
            var0.rect(4, 3, 8, 1, var4);
            var0.rect(6, 9, 4, 1, var5);
            break;
         case 1:
            var0.rect(2, 2, 4, 4, var3);
            var0.rect(10, 2, 4, 4, var3);
            var0.rect(3, 5, 10, 9, var3);
            var0.clr(6, 2, 4, 2);
            var0.rect(6, 4, 4, 1, var5);
            var0.rect(4, 6, 8, 1, var4);
            var0.rect(2, 6, 2, 3, var5);
            var0.rect(12, 6, 2, 3, var5);
            break;
         case 2:
            var0.rect(3, 2, 10, 4, var3);
            var0.rect(3, 6, 4, 8, var3);
            var0.rect(9, 6, 4, 8, var3);
            var0.rect(3, 2, 10, 1, var4);
            var0.rect(7, 4, 2, 2, var5);
            break;
         default:
            var0.rect(2, 8, 5, 6, var3);
            var0.rect(9, 8, 5, 6, var3);
            var0.rect(2, 12, 6, 2, var5);
            var0.rect(9, 12, 6, 2, var5);
            var0.rect(3, 8, 3, 1, var4);
            var0.rect(10, 8, 3, 1, var4);
      }

      if (var2 == 0) {
         for (int var6 = 0; var6 < 256; var6++) {
            if (var0.p[var6] >>> 24 != 0 && (var6 * 7 + var6 / 16 * 3) % 5 == 0) {
               var0.p[var6] = sc(var0.p[var6] & 16777215, 0.9F);
            }
         }
      }
   }

   static final class Cv {
      final int[] p = new int[256];

      void set(int var1, int var2, int var3) {
         if (var1 >= 0 && var1 < 16 && var2 >= 0 && var2 < 16) {
            this.p[var2 * 16 + var1] = 0xFF000000 | var3;
         }
      }

      int get(int var1, int var2) {
         return var1 >= 0 && var1 <= 15 && var2 >= 0 && var2 <= 15 ? this.p[var2 * 16 + var1] : 0;
      }

      void line(int var1, int var2, int var3, int var4, int var5) {
         int var6 = Math.abs(var3 - var1);
         int var7 = Math.abs(var4 - var2);
         int var8 = var1 < var3 ? 1 : -1;
         int var9 = var2 < var4 ? 1 : -1;
         int var10 = var6 - var7;

         while (true) {
            this.set(var1, var2, var5);
            if (var1 == var3 && var2 == var4) {
               return;
            }

            int var11 = 2 * var10;
            if (var11 > -var7) {
               var10 -= var7;
               var1 += var8;
            }

            if (var11 < var6) {
               var10 += var6;
               var2 += var9;
            }
         }
      }

      void clr(int var1, int var2, int var3, int var4) {
         for (int var5 = 0; var5 < var4; var5++) {
            for (int var6 = 0; var6 < var3; var6++) {
               if (var1 + var6 >= 0 && var1 + var6 < 16 && var2 + var5 >= 0 && var2 + var5 < 16) {
                  this.p[(var2 + var5) * 16 + var1 + var6] = 0;
               }
            }
         }
      }

      void rect(int var1, int var2, int var3, int var4, int var5) {
         for (int var6 = 0; var6 < var4; var6++) {
            for (int var7 = 0; var7 < var3; var7++) {
               this.set(var1 + var7, var2 + var6, var5);
            }
         }
      }

      void ell(double var1, double var3, double var5, double var7, int var9) {
         for (int var10 = 0; var10 < 16; var10++) {
            for (int var11 = 0; var11 < 16; var11++) {
               double var12 = (var11 + 0.5 - var1) / var5;
               double var14 = (var10 + 0.5 - var3) / var7;
               if (var12 * var12 + var14 * var14 <= 1.0) {
                  this.set(var11, var10, var9);
               }
            }
         }
      }

      void shade() {
         int[] var1 = (int[])this.p.clone();

         for (int var2 = 0; var2 < 16; var2++) {
            for (int var3 = 0; var3 < 16; var3++) {
               if (var1[var2 * 16 + var3] >>> 24 != 0) {
                  int var4 = var1[var2 * 16 + var3] & 16777215;
                  boolean var5 = var3 == 15 || var1[var2 * 16 + var3 + 1] >>> 24 == 0;
                  boolean var6 = var2 == 15 || var2 < 15 && var1[(var2 + 1) * 16 + var3] >>> 24 == 0;
                  boolean var7 = var3 == 0 || var1[var2 * 16 + var3 - 1] >>> 24 == 0;
                  boolean var8 = var2 == 0 || var2 > 0 && var1[(var2 - 1) * 16 + var3] >>> 24 == 0;
                  if (var5 || var6) {
                     this.p[var2 * 16 + var3] = 0xFF000000 | TexExtra.sc(var4, 0.62F) & 16777215;
                  } else if (var7 || var8) {
                     this.p[var2 * 16 + var3] = 0xFF000000 | TexExtra.sc(var4, 1.18F) & 16777215;
                  }
               }
            }
         }
      }
   }
}
