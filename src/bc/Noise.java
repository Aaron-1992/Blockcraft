package bc;

import java.util.Random;

public final class Noise {
   private final int[] p = new int[512];

   public Noise(long var1) {
      Random var3 = new Random(var1);
      int[] var4 = new int[256];
      int var5 = 0;

      while (var5 < 256) {
         var4[var5] = var5++;
      }

      for (int var8 = 255; var8 > 0; var8--) {
         int var6 = var3.nextInt(var8 + 1);
         int var7 = var4[var8];
         var4[var8] = var4[var6];
         var4[var6] = var7;
      }

      for (int var9 = 0; var9 < 512; var9++) {
         this.p[var9] = var4[var9 & 0xFF];
      }
   }

   private static double fade(double var0) {
      return var0 * var0 * var0 * (var0 * (var0 * 6.0 - 15.0) + 10.0);
   }

   private static double lerp(double var0, double var2, double var4) {
      return var2 + var0 * (var4 - var2);
   }

   private static double grad(int var0, double var1, double var3, double var5) {
      var0 &= 15;
      double var7 = var0 < 8 ? var1 : var3;
      double var9 = var0 < 4 ? var3 : (var0 != 12 && var0 != 14 ? var5 : var1);
      return ((var0 & 1) == 0 ? var7 : -var7) + ((var0 & 2) == 0 ? var9 : -var9);
   }

   public double noise(double var1, double var3, double var5) {
      int var7 = (int)Math.floor(var1);
      int var8 = (int)Math.floor(var3);
      int var9 = (int)Math.floor(var5);
      var1 -= var7;
      var3 -= var8;
      var5 -= var9;
      var7 &= 255;
      var8 &= 255;
      var9 &= 255;
      double var10 = fade(var1);
      double var12 = fade(var3);
      double var14 = fade(var5);
      int var16 = this.p[var7] + var8;
      int var17 = this.p[var16] + var9;
      int var18 = this.p[var16 + 1] + var9;
      int var19 = this.p[var7 + 1] + var8;
      int var20 = this.p[var19] + var9;
      int var21 = this.p[var19 + 1] + var9;
      return lerp(
         var14,
         lerp(
            var12,
            lerp(var10, grad(this.p[var17], var1, var3, var5), grad(this.p[var20], var1 - 1.0, var3, var5)),
            lerp(var10, grad(this.p[var18], var1, var3 - 1.0, var5), grad(this.p[var21], var1 - 1.0, var3 - 1.0, var5))
         ),
         lerp(
            var12,
            lerp(var10, grad(this.p[var17 + 1], var1, var3, var5 - 1.0), grad(this.p[var20 + 1], var1 - 1.0, var3, var5 - 1.0)),
            lerp(var10, grad(this.p[var18 + 1], var1, var3 - 1.0, var5 - 1.0), grad(this.p[var21 + 1], var1 - 1.0, var3 - 1.0, var5 - 1.0))
         )
      );
   }

   public double noise2(double var1, double var3) {
      return this.noise(var1, 137.31, var3);
   }

   public double fbm(double var1, double var3, int var5) {
      double var6 = 0.0;
      double var8 = 1.0;
      double var10 = 1.0;
      double var12 = 0.0;

      for (int var14 = 0; var14 < var5; var14++) {
         var6 += this.noise(var1 * var10, 137.31 + var14 * 17.7, var3 * var10) * var8;
         var12 += var8;
         var8 *= 0.5;
         var10 *= 2.0;
      }

      return var6 / var12 * 1.5;
   }
}
