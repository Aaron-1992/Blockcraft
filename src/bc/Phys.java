package bc;

import java.util.Arrays;

public final class Phys {
   static final double EPS = 1.0E-7;

   private Phys() {
   }

   public static void move(World var0, double[] var1, double var2, double var4, double var6, double[] var8) {
      double var9 = var1[0] + Math.min(var2, 0.0);
      double var11 = var1[3] + Math.max(var2, 0.0);
      double var13 = var1[1] + Math.min(var4, 0.0);
      double var15 = var1[4] + Math.max(var4, 0.0);
      double var17 = var1[2] + Math.min(var6, 0.0);
      double var19 = var1[5] + Math.max(var6, 0.0);
      int var21 = (int)Math.floor(var9);
      int var22 = (int)Math.floor(var11);
      int var23 = (int)Math.floor(var13) - 1; // Update 3.15: one cell lower, a fence, wall or gate is 1.5 high and reaches into the cell above its own
      int var24 = (int)Math.floor(var15);
      int var25 = (int)Math.floor(var17);
      int var26 = (int)Math.floor(var19);
      double[] var27 = new double[384];
      int var28 = 0;

      for (int var29 = var23; var29 <= var24; var29++) {
         for (int var30 = var25; var30 <= var26; var30++) {
            for (int var31 = var21; var31 <= var22; var31++) {
               int var32 = var0.getBlock(var31, var29, var30);
               double[] var33 = Blocks.coll[var32];
               if (var33 != null && var32 != 0) {
                  double[][] var34 = Shapes.isDyn(var32) ? Shapes.collisionBoxes(var0, var31, var29, var30, var32) : Blocks.collMulti[var32];

                  for (int var35 = 0; var35 < (var34 == null ? 1 : var34.length); var35++) {
                     if (var34 != null) {
                        var33 = var34[var35];
                     }

                     if (var28 + 6 > var27.length) {
                        var27 = Arrays.copyOf(var27, var27.length * 2);
                     }

                     var27[var28++] = var31 + var33[0];
                     var27[var28++] = var29 + var33[1];
                     var27[var28++] = var30 + var33[2];
                     var27[var28++] = var31 + var33[3];
                     var27[var28++] = var29 + var33[4];
                     var27[var28++] = var30 + var33[5];
                  }
               }
            }
         }
      }

      for (int var41 = 0; var41 < var28; var41 += 6) {
         if (!(var1[3] <= var27[var41] + 1.0E-7)
            && !(var1[0] >= var27[var41 + 3] - 1.0E-7)
            && !(var1[5] <= var27[var41 + 2] + 1.0E-7)
            && !(var1[2] >= var27[var41 + 5] - 1.0E-7)) {
            if (var4 > 0.0 && var1[4] <= var27[var41 + 1] + 1.0E-7) {
               var4 = Math.min(var4, var27[var41 + 1] - var1[4]);
            } else if (var4 < 0.0 && var1[1] >= var27[var41 + 4] - 1.0E-7) {
               var4 = Math.max(var4, var27[var41 + 4] - var1[1]);
            }
         }
      }

      var1[1] += var4;
      var1[4] += var4;

      for (int var42 = 0; var42 < var28; var42 += 6) {
         if (!(var1[4] <= var27[var42 + 1] + 1.0E-7)
            && !(var1[1] >= var27[var42 + 4] - 1.0E-7)
            && !(var1[5] <= var27[var42 + 2] + 1.0E-7)
            && !(var1[2] >= var27[var42 + 5] - 1.0E-7)) {
            if (var2 > 0.0 && var1[3] <= var27[var42] + 1.0E-7) {
               var2 = Math.min(var2, var27[var42] - var1[3]);
            } else if (var2 < 0.0 && var1[0] >= var27[var42 + 3] - 1.0E-7) {
               var2 = Math.max(var2, var27[var42 + 3] - var1[0]);
            }
         }
      }

      var1[0] += var2;
      var1[3] += var2;

      for (int var43 = 0; var43 < var28; var43 += 6) {
         if (!(var1[4] <= var27[var43 + 1] + 1.0E-7)
            && !(var1[1] >= var27[var43 + 4] - 1.0E-7)
            && !(var1[3] <= var27[var43] + 1.0E-7)
            && !(var1[0] >= var27[var43 + 3] - 1.0E-7)) {
            if (var6 > 0.0 && var1[5] <= var27[var43 + 2] + 1.0E-7) {
               var6 = Math.min(var6, var27[var43 + 2] - var1[5]);
            } else if (var6 < 0.0 && var1[2] >= var27[var43 + 5] - 1.0E-7) {
               var6 = Math.max(var6, var27[var43 + 5] - var1[2]);
            }
         }
      }

      var1[2] += var6;
      var1[5] += var6;
      var8[0] = var2;
      var8[1] = var4;
      var8[2] = var6;
   }

   public static boolean collides(World var0, double[] var1) {
      int var2 = (int)Math.floor(var1[0]);
      int var3 = (int)Math.floor(var1[3]);
      int var4 = (int)Math.floor(var1[1]) - 1;
      int var5 = (int)Math.floor(var1[4]);
      int var6 = (int)Math.floor(var1[2]);
      int var7 = (int)Math.floor(var1[5]);

      for (int var8 = var4; var8 <= var5; var8++) {
         for (int var9 = var6; var9 <= var7; var9++) {
            for (int var10 = var2; var10 <= var3; var10++) {
               int var11 = var0.getBlock(var10, var8, var9);
               double[] var12 = Blocks.coll[var11];
               if (var12 != null && var11 != 0) {
                  double[][] var13 = Shapes.isDyn(var11) ? Shapes.collisionBoxes(var0, var10, var8, var9, var11) : Blocks.collMulti[var11];

                  for (int var14 = 0; var14 < (var13 == null ? 1 : var13.length); var14++) {
                     if (var13 != null) {
                        var12 = var13[var14];
                     }

                     if (var1[3] > var10 + var12[0] + 1.0E-7
                        && var1[0] < var10 + var12[3] - 1.0E-7
                        && var1[4] > var8 + var12[1] + 1.0E-7
                        && var1[1] < var8 + var12[4] - 1.0E-7
                        && var1[5] > var9 + var12[2] + 1.0E-7
                        && var1[2] < var9 + var12[5] - 1.0E-7) {
                        return true;
                     }
                  }
               }
            }
         }
      }

      return false;
   }
}
