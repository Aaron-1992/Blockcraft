package bc;

import bc.Mobs.Arrow;
import bc.Mobs.Mob;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

public final class MobRender {
   static final int HEAD = 1;
   static final int BODY = 2;
   static final int LEGA = 3;
   static final int LEGB = 4;
   static final int ARMA = 5;
   static final int ARMB = 6;
   static final int WINGL = 7;
   static final int WINGR = 8;
   static final int STATIC = 9;
   static final int SLEGR = 10;
   static final int SLEGL = 11;
   static final MobRender.Model[] models = new MobRender.Model[Mobs.N];
   static boolean built;
   static final int[] SHEEP_RGB = new int[16];
   static int[] pix;
   static int PW;
   static int PH = 64;

   private MobRender() {
   }

   static float hash(int var0, int var1, int var2) {
      int var3 = var0 * 374761393 + var1 * 668265263 + var2 * 1274126177;
      var3 = (var3 ^ var3 >>> 13) * 1274126177;
      var3 ^= var3 >>> 16;
      return (var3 & 65535) / 65535.0F;
   }

   static float vnoise(float var0, float var1, int var2) {
      int var3 = (int)Math.floor(var0);
      int var4 = (int)Math.floor(var1);
      float var5 = var0 - var3;
      float var6 = var1 - var4;
      var5 = var5 * var5 * (3.0F - 2.0F * var5);
      var6 = var6 * var6 * (3.0F - 2.0F * var6);
      float var7 = hash(var3, var4, var2);
      float var8 = hash(var3 + 1, var4, var2);
      float var9 = hash(var3, var4 + 1, var2);
      float var10 = hash(var3 + 1, var4 + 1, var2);
      return (var7 + (var8 - var7) * var5) * (1.0F - var6) + (var9 + (var10 - var9) * var5) * var6;
   }

   static int mul(int var0, float var1) {
      int var2 = Math.min(255, Math.max(0, (int)((var0 >> 16 & 0xFF) * var1)));
      int var3 = Math.min(255, Math.max(0, (int)((var0 >> 8 & 0xFF) * var1)));
      int var4 = Math.min(255, Math.max(0, (int)((var0 & 0xFF) * var1)));
      return 0xFF000000 | var2 << 16 | var3 << 8 | var4;
   }

   static void build() {
      if (!built) {
         built = true;
         MobRender.Model var0 = new MobRender.Model();
         var0.type = 0;
         int var1 = 15246752;
         var0.add(2, var1, 10, 8, 16, -5.0F, 0.0F, -8.0F, 0.0F, 6.0F, 0.0F);
         var0.add(1, var1, 8, 8, 8, -4.0F, -4.0F, 0.0F, 0.0F, 12.0F, 8.0F);
         var0.add(1, 14256776, 4, 3, 1, -2.0F, -4.0F, 8.0F, 0.0F, 12.0F, 8.0F);
         MobRender.Part var2 = var0.add(3, var1, 4, 6, 4, -2.0F, -6.0F, -2.0F, 3.0F, 6.0F, 5.0F);
         var0.addLike(var2, 4, -3.0F, 6.0F, 5.0F);
         var0.addLike(var2, 4, 3.0F, 6.0F, -5.0F);
         var0.addLike(var2, 3, -3.0F, 6.0F, -5.0F);
         models[0] = var0;
         var0 = new MobRender.Model();
         var0.type = 1;
         int var3 = 4534306;
         var0.add(2, var3, 12, 10, 18, -6.0F, 0.0F, -9.0F, 0.0F, 12.0F, 0.0F);
         var0.add(1, var3, 8, 8, 6, -4.0F, -4.0F, 0.0F, 0.0F, 20.0F, 9.0F);
         var0.add(1, 13215884, 4, 3, 1, -2.0F, -4.0F, 6.0F, 0.0F, 20.0F, 9.0F);
         var0.add(1, 14210248, 1, 3, 1, -5.0F, 3.0F, 1.0F, 0.0F, 20.0F, 9.0F);
         var0.add(1, 14210248, 1, 3, 1, 4.0F, 3.0F, 1.0F, 0.0F, 20.0F, 9.0F);
         var2 = var0.add(3, var3, 4, 12, 4, -2.0F, -12.0F, -2.0F, 4.0F, 12.0F, 7.0F);
         var0.addLike(var2, 4, -4.0F, 12.0F, 7.0F);
         var0.addLike(var2, 4, 4.0F, 12.0F, -7.0F);
         var0.addLike(var2, 3, -4.0F, 12.0F, -7.0F);
         models[1] = var0;
         var0 = new MobRender.Model();
         var0.type = 2;
         int var4 = 13215117;
         var0.add(2, 15132390, 12, 10, 16, -6.0F, 0.0F, -8.0F, 0.0F, 12.0F, 0.0F);
         var0.add(1, var4, 6, 6, 8, -3.0F, -3.0F, 0.0F, 0.0F, 20.0F, 7.0F);
         var0.add(1, 15132390, 6, 2, 5, -3.0F, 3.0F, 0.0F, 0.0F, 20.0F, 7.0F);
         var2 = var0.add(3, var4, 4, 12, 4, -2.0F, -12.0F, -2.0F, 3.0F, 12.0F, 5.0F);
         var0.addLike(var2, 4, -3.0F, 12.0F, 5.0F);
         var0.addLike(var2, 4, 3.0F, 12.0F, -5.0F);
         var0.addLike(var2, 3, -3.0F, 12.0F, -5.0F);
         models[2] = var0;
         var0 = new MobRender.Model();
         var0.type = 3;
         var0.add(2, 15592941, 6, 6, 8, -3.0F, 0.0F, -4.0F, 0.0F, 5.0F, 0.0F);
         var0.add(1, 15592941, 4, 6, 3, -2.0F, 0.0F, -1.0F, 0.0F, 9.0F, 4.0F);
         var0.add(1, 14721072, 4, 2, 2, -2.0F, 2.0F, 2.0F, 0.0F, 9.0F, 4.0F);
         var0.add(1, 13115424, 2, 2, 2, -1.0F, 0.0F, 2.0F, 0.0F, 9.0F, 4.0F);
         var2 = var0.add(3, 14721072, 2, 5, 2, -1.0F, -5.0F, -1.0F, 2.0F, 5.0F, 0.0F);
         var0.addLike(var2, 4, -2.0F, 5.0F, 0.0F);
         var0.add(7, 14869218, 1, 4, 6, -0.5F, -4.0F, -3.0F, 3.5F, 10.0F, 0.0F);
         var0.add(8, 14869218, 1, 4, 6, -0.5F, -4.0F, -3.0F, -3.5F, 10.0F, 0.0F);
         models[3] = var0;
         var0 = new MobRender.Model();
         var0.type = 4;
         int var5 = 6263880;
         var0.add(1, var5, 8, 8, 8, -4.0F, 0.0F, -4.0F, 0.0F, 24.0F, 0.0F);
         var0.add(2, 2920352, 8, 12, 4, -4.0F, -12.0F, -2.0F, 0.0F, 24.0F, 0.0F);
         MobRender.Part var6 = var0.add(5, var5, 4, 12, 4, -2.0F, -10.0F, -2.0F, 6.0F, 22.0F, 0.0F);
         var0.addLike(var6, 6, -6.0F, 22.0F, 0.0F);
         var2 = var0.add(3, 3816090, 4, 12, 4, -2.0F, -12.0F, -2.0F, 2.0F, 12.0F, 0.0F);
         var0.addLike(var2, 4, -2.0F, 12.0F, 0.0F);
         models[4] = var0;
         var0 = new MobRender.Model();
         var0.type = 5;
         int var7 = 13619151;
         var0.add(1, var7, 8, 8, 8, -4.0F, 0.0F, -4.0F, 0.0F, 24.0F, 0.0F);
         var0.add(2, var7, 8, 12, 4, -4.0F, -12.0F, -2.0F, 0.0F, 24.0F, 0.0F);
         var6 = var0.add(5, var7, 2, 12, 2, -1.0F, -10.0F, -1.0F, 5.0F, 22.0F, 0.0F);
         var0.addLike(var6, 6, -5.0F, 22.0F, 0.0F);
         var2 = var0.add(3, var7, 2, 12, 2, -1.0F, -12.0F, -1.0F, 2.0F, 12.0F, 0.0F);
         var0.addLike(var2, 4, -2.0F, 12.0F, 0.0F);
         models[5] = var0;
         var0 = new MobRender.Model();
         var0.type = 6;
         int var8 = 5020222;
         var0.add(1, var8, 8, 8, 8, -4.0F, 0.0F, -4.0F, 0.0F, 18.0F, 0.0F);
         var0.add(2, var8, 8, 12, 4, -4.0F, -12.0F, -2.0F, 0.0F, 18.0F, 0.0F);
         var2 = var0.add(3, var8, 4, 6, 4, -2.0F, -6.0F, -2.0F, 2.0F, 6.0F, 4.0F);
         var0.addLike(var2, 4, -2.0F, 6.0F, 4.0F);
         var0.addLike(var2, 4, 2.0F, 6.0F, -4.0F);
         var0.addLike(var2, 3, -2.0F, 6.0F, -4.0F);
         models[6] = var0;
         var0 = new MobRender.Model();
         var0.type = 7;
         int var9 = 2958882;
         var0.add(1, var9, 8, 8, 8, -4.0F, -4.0F, 0.0F, 0.0F, 8.0F, 3.0F);
         var0.add(2, var9, 6, 6, 6, -3.0F, -3.0F, -3.0F, 0.0F, 8.0F, 0.0F);
         var0.add(2, 3813418, 10, 8, 12, -5.0F, -4.0F, -12.0F, 0.0F, 8.0F, -3.0F);
         MobRender.Part var10 = var0.add(10, 1972760, 16, 2, 2, 0.0F, -1.0F, -1.0F, 3.0F, 8.0F, 0.0F);

         for (int var11 = 0; var11 < 4; var11++) {
            float var12 = 2.5F - var11 * 1.6F;
            MobRender.Part var13 = var11 == 0 ? var10 : var0.addLike(var10, 10, 3.0F, 8.0F, var12);
            var13.pz = var12;
            var13.idx = var11;
            var13.ry = 45 - var11 * 30;
            var13.rz = -30.0F;
            MobRender.Part var14 = var0.addLike(var10, 11, -3.0F, 8.0F, var12);
            var14.ox = -16.0F;
            var14.idx = var11;
            var14.ry = -(45 - var11 * 30);
            var14.rz = 30.0F;
         }

         models[7] = var0;
         MobModels312.build(models);

         for (MobRender.Model var32 : models) {
            paint(var32);
         }
      }
   }

   static void put(int var0, int var1, int var2) {
      if (var0 >= 0 && var1 >= 0 && var0 < PW && var1 < PH) {
         pix[var1 * PW + var0] = 0xFF000000 | var2;
      }
   }

   /** Update 3.12: a see-through pixel (the alpha test throws it away). */
   static void clear(int x, int y) {
      if (x >= 0 && y >= 0 && x < PW && y < PH) {
         pix[y * PW + x] = 0;
      }
   }

   static int[] face(MobRender.Part var0, int var1) {
      switch (var1) {
         case 0:
            return new int[]{var0.u + var0.d, var0.v, var0.w, var0.d};
         case 1:
            return new int[]{var0.u + var0.d + var0.w, var0.v, var0.w, var0.d};
         case 2:
            return new int[]{var0.u, var0.v + var0.d, var0.d, var0.h};
         case 3:
            return new int[]{var0.u + var0.d, var0.v + var0.d, var0.w, var0.h};
         case 4:
            return new int[]{var0.u + var0.d + var0.w, var0.v + var0.d, var0.d, var0.h};
         default:
            return new int[]{var0.u + 2 * var0.d + var0.w, var0.v + var0.d, var0.w, var0.h};
      }
   }

   static void paint(MobRender.Model var0) {
      PW = var0.tw;
      PH = var0.th;
      pix = new int[var0.tw * var0.th];

      for (MobRender.Part var2 : var0.parts) {
         if (!var2.shared) {
            for (int var3 = 0; var3 < 6; var3++) {
               int[] var4 = face(var2, var3);
               float var5 = var3 == 0 ? 1.08F : (var3 == 1 ? 0.7F : (var3 != 3 && var3 != 5 ? 0.86F : 0.94F));

               for (int var6 = 0; var6 < var4[3]; var6++) {
                  for (int var7 = 0; var7 < var4[2]; var7++) {
                     int var8 = var4[0] + var7;
                     int var9 = var4[1] + var6;
                     float var10 = (hash(var8, var9, var0.type * 31 + var2.idx) - 0.5F) * 0.14F;
                     int var11 = var2.color;
                     float var12 = 1.0F;
                     switch (var0.type) {
                        case 0:
                           var12 = 1.0F;
                           break;
                        case 1:
                           float var62 = vnoise(var8 * 0.28F, var9 * 0.34F, 5 + var2.idx);
                           if (var2.idx != 0) {
                              if (var2.idx == 4) {
                              }
                           } else if (var62 > 0.58F) {
                              var11 = 15131354;
                           }

                           if (var2.idx == 4 || var2.idx == 5) {
                              var12 = 1.0F;
                           }

                           if (var2.idx != 4) {
                              if (var2.shared) {
                              }
                           } else {
                              var12 = 1.0F;
                           }

                           if (var2.idx == 5) {
                           }

                           if (var2.idx == 0 && var3 == 3) {
                           }

                           if (var2.idx == 6) {
                           }
                           break;
                        case 2:
                           if (var2.color == 15132390) {
                              var11 = SHEEP_RGB[0];
                              var12 = 0.9F + hash(var8 / 2, var9 / 2, 3) * 0.16F;
                           }
                        case 3:
                        default:
                           break;
                        case 4:
                           var12 = 0.92F + hash(var8, var9, 77) * 0.12F;
                           break;
                        case 5:
                           if (var2.idx == 1 && (var3 == 3 || var3 == 5) && var6 % 3 == 2 && var6 < 10) {
                              var12 = 0.35F;
                           }

                           if (var2.idx == 1 && (var3 == 3 || var3 == 5) && var6 < 11 && var7 == var4[2] / 2) {
                              var12 = 0.45F;
                           }

                           if (var6 % 4 == 3 && var2.idx > 1) {
                              var12 = 0.78F;
                           }
                           break;
                        case 6:
                           float var13 = vnoise(var8 * 0.5F, var9 * 0.5F, 9);
                           var12 = 0.72F + var13 * 0.55F;
                     }

                     float[] px312 = MobModels312.pixel(var0, var2, var8, var9, var3);
                     if (px312 != null) {
                        var11 = (int)px312[0];
                        var12 = px312[1];
                     }

                     put(var8, var9, mul(var11, var5 * var12 * (1.0F + var10)) & 16777215);
                  }
               }
            }
         }
      }

      switch (var0.type) {
         case 0:
            MobRender.Part var30 = (MobRender.Part)var0.parts.get(1);
            int[] var21 = face(var30, 3);
            put(var21[0] + 1, var21[1] + 3, 2101264);
            put(var21[0] + 1, var21[1] + 3 - 0, 2101264);
            put(var21[0] + 6, var21[1] + 3, 2101264);
            put(var21[0] + 2, var21[1] + 3, 16777215);
            put(var21[0] + 5, var21[1] + 3, 16777215);
            var21 = face((MobRender.Part)var0.parts.get(2), 3);
            put(var21[0] + 1, var21[1] + 1, 8011840);
            put(var21[0] + 2, var21[1] + 1, 8011840);
            break;
         case 1:
            MobRender.Part var29 = (MobRender.Part)var0.parts.get(1);
            int[] var20 = face(var29, 3);
            put(var20[0] + 1, var20[1] + 3, 16777215);
            put(var20[0] + 2, var20[1] + 3, 1052688);
            put(var20[0] + 5, var20[1] + 3, 1052688);
            put(var20[0] + 6, var20[1] + 3, 16777215);

            for (int var37 = 0; var37 < var20[3]; var37++) {
               for (int var44 = 2; var44 < 6; var44++) {
                  if (var37 > 4) {
                     put(var20[0] + var44, var20[1] + var37, 14271400);
                  }
               }
            }
            break;
         case 2:
            MobRender.Part var28 = (MobRender.Part)var0.parts.get(1);
            int[] var19 = face(var28, 3);
            put(var19[0] + 0, var19[1] + 3, 16777215);
            put(var19[0] + 1, var19[1] + 3, 1052688);
            put(var19[0] + 4, var19[1] + 3, 1052688);
            put(var19[0] + 5, var19[1] + 3, 16777215);
            put(var19[0] + 2, var19[1] + 5, 11565690);
            put(var19[0] + 3, var19[1] + 5, 11565690);
            break;
         case 3:
            int[] var18 = face((MobRender.Part)var0.parts.get(1), 3);
            put(var18[0] + 0, var18[1] + 2, 1052688);
            put(var18[0] + 3, var18[1] + 2, 1052688);
            break;
         case 4:
            int[] var17 = face((MobRender.Part)var0.parts.get(0), 3);

            for (int var50 : new int[]{1, 2, 5, 6}) {
               put(var17[0] + var50, var17[1] + 3, 1714784);
            }

            put(var17[0] + 3, var17[1] + 4, 4155955);
            put(var17[0] + 4, var17[1] + 4, 4155955);

            for (int var27 = 2; var27 < 6; var27++) {
               put(var17[0] + var27, var17[1] + 6, 3100456);
            }
            break;
         case 5:
            int[] var16 = face((MobRender.Part)var0.parts.get(0), 3);

            for (int var49 : new int[]{1, 2, 5, 6}) {
               put(var16[0] + var49, var16[1] + 3, 1052688);
               put(var16[0] + var49, var16[1] + 2, 1052688);
            }

            put(var16[0] + 3, var16[1] + 4, 8026746);
            put(var16[0] + 4, var16[1] + 4, 8026746);

            for (int var25 = 1; var25 < 7; var25++) {
               put(var16[0] + var25, var16[1] + 6, var25 % 2 == 0 ? 3158064 : 12632256);
            }
            break;
         case 6:
            int[] var15 = face((MobRender.Part)var0.parts.get(0), 3);
            int var23 = 662026;

            for (int var51 : new int[]{1, 2, 5, 6}) {
               for (int var60 : new int[]{2, 3}) {
                  put(var15[0] + var51, var15[1] + var60, var23);
               }
            }

            for (int var52 : new int[]{3, 4}) {
               for (int var61 : new int[]{4, 5}) {
                  put(var15[0] + var52, var15[1] + var61, var23);
               }
            }

            for (int var53 : new int[]{2, 3, 4, 5}) {
               put(var15[0] + var53, var15[1] + 5, var23);
            }

            put(var15[0] + 2, var15[1] + 6, var23);
            put(var15[0] + 5, var15[1] + 6, var23);
            break;
         case 7:
            int[] var14 = face((MobRender.Part)var0.parts.get(0), 3);
            put(var14[0] + 1, var14[1] + 3, 14688288);
            put(var14[0] + 2, var14[1] + 3, 14688288);
            put(var14[0] + 5, var14[1] + 3, 14688288);
            put(var14[0] + 6, var14[1] + 3, 14688288);
            put(var14[0] + 3, var14[1] + 4, 10489872);
            put(var14[0] + 4, var14[1] + 4, 10489872);
      }

      MobModels312.details(var0);

      ByteBuffer var31 = BufferUtils.createByteBuffer(var0.tw * var0.th * 4);

      for (int var38 = 0; var38 < pix.length; var38++) {
         int var45 = pix[var38];
         var31.put((byte)(var45 >> 16)).put((byte)(var45 >> 8)).put((byte)var45).put((byte)(var45 >>> 24));
      }

      var31.flip();
      var0.tex = GL11.glGenTextures();
      GL11.glBindTexture(3553, var0.tex);
      GL11.glTexParameteri(3553, 10241, 9728);
      GL11.glTexParameteri(3553, 10240, 9728);
      GL11.glTexImage2D(3553, 0, 6408, var0.tw, var0.th, 0, 6408, 5121, var31);
   }

   static void quad(MobRender.Model var0, float[][] var1, int[] var2) {
      float var3 = var0.tw;
      float var4 = var0.th;
      float var5 = var2[0] / var3;
      float var6 = var2[1] / var4;
      float var7 = (var2[0] + var2[2]) / var3;
      float var8 = (var2[1] + var2[3]) / var4;
      float[] var9 = new float[]{var5, var7, var7, var5};
      float[] var10 = new float[]{var6, var6, var8, var8};

      for (int var11 = 0; var11 < 4; var11++) {
         GL11.glTexCoord2f(var9[var11], var10[var11]);
         GL11.glVertex3f(var1[var11][0], var1[var11][1], var1[var11][2]);
      }
   }

   static void drawBox(MobRender.Model var0, MobRender.Part var1, float var2, float[] var3) {
      float var4 = var1.ox / 16.0F;
      float var5 = var1.oy / 16.0F;
      float var6 = var1.oz / 16.0F;
      float var7 = var4 + var1.w / 16.0F;
      float var8 = var5 + var1.h / 16.0F;
      float var9 = var6 + var1.d / 16.0F;
      float[][][] var10 = new float[][][]{
         {{var4, var8, var6}, {var7, var8, var6}, {var7, var8, var9}, {var4, var8, var9}},
         {{var4, var5, var9}, {var7, var5, var9}, {var7, var5, var6}, {var4, var5, var6}},
         {{var7, var8, var9}, {var7, var8, var6}, {var7, var5, var6}, {var7, var5, var9}},
         {{var7, var8, var9}, {var4, var8, var9}, {var4, var5, var9}, {var7, var5, var9}},
         {{var4, var8, var6}, {var4, var8, var9}, {var4, var5, var9}, {var4, var5, var6}},
         {{var4, var8, var6}, {var7, var8, var6}, {var7, var5, var6}, {var4, var5, var6}}
      };
      GL11.glBegin(7);

      for (int var11 = 0; var11 < 6; var11++) {
         float var12 = 1.0F;
         GL11.glColor3f(var2 * var3[0] * var12, var2 * var3[1] * var12, var2 * var3[2] * var12);
         quad(var0, var10[var11], face(var1, var11));
      }

      GL11.glEnd();
   }

   static void render(Game var0, Renderer var1, float var2) {
      if (!built) {
         build();
      }

      World var3 = var0.world;
      Mobs var4 = var0.mobs;
      GL11.glEnable(3553);
      boolean var5 = GL11.glIsEnabled(2884);
      GL11.glDisable(2884);
      GL11.glEnable(2929);

      for (Mob var7 : var4.list) {
         if (var7.dead && var7.type == Mobs.END_CRYSTAL) {
            continue; // blown up
         }

         if (Mobs312.onShoulder(var7) && var0.view == 0) {
            continue; // the parrot on your shoulder is not seen from your own eyes
         }

         double var8 = var7.px + (var7.x - var7.px) * var2;
         double var10 = var7.py + (var7.y - var7.py) * var2;
         double var12 = var7.pz + (var7.z - var7.pz) * var2;
         double var14 = var8 - var1.camX;
         double var16 = var10 - var1.camY;
         double var18 = var12 - var1.camZ;
         float reach = Math.max(2.2F, Mobs.W[var7.type] * var7.scale * 0.9F);
         if (!(var14 * var14 + var16 * var16 + var18 * var18 > 6400.0 + reach * reach * 16.0) && var1.visible((float)var14, (float)var16 + Mobs.H[var7.type] * var7.scale * 0.5F, (float)var18, reach)) {
            drawMob(var7, var3, var1, var2, var8, var10, var12);
         }
      }

      drawArrows(var4, var1, var2);
      drawFireballs(var4, var1, var2);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      if (var5) {
         GL11.glEnable(2884);
      }
   }

   static float lerpAngle(float var0, float var1, float var2) {
      return var0 + Mobs.wrap(var1 - var0) * var2;
   }

   static void drawMob(Mob var0, World var1, Renderer var2, float var3, double var4, double var6, double var8) {
      if (BackroomsStalkerMod.isStalkerMob(var0)) {
         BackroomsStalkerMod.drawStalkerEntity(var0, var1, var2, var3, var4, var6, var8);
         return;
      }
      if (SkyGen.isMinigameBot(var0)) {
         SkyGen.drawBotPlayer(var0, var1, var2, var3, var4, var6, var8);
         return;
      }

      if (RigDraw.draw(var0, var1, var2, var3, var4, var6, var8)) {
         return; // Update 3.17: this mob has one of the new jointed models (see Rigs317)
      }

      MobRender.Model var10 = models[var0.type];
      float var11 = var2.brightnessAt(var1, var4, var6 + 0.5, var8);
      if (var0.type == Mobs.END_CRYSTAL) {
         var11 = 1.0F; // the crystals glow
      }

      float[] var12 = new float[]{1.0F, 1.0F, 1.0F};
      if (var0.charged) {
         // Update 3.8: struck by lightning - glows a pulsing blue
         float pulse = 0.5F + 0.5F * (float)Math.sin((var0.age + var3) * 0.3F);
         var12[0] = 0.40F + 0.18F * pulse;
         var12[1] = 0.62F + 0.22F * pulse;
         var12[2] = 1.0F;
      }

      if (var0.hurtTime > 0 || var0.dead) {
         var12[1] = 0.45F;
         var12[2] = 0.45F;
      }

      float var13 = lerpAngle(var0.pyaw, var0.yaw, var3);
      float var14 = lerpAngle(var0.pheadYaw, var0.headYaw, var3) - var13;
      float var15 = var0.pheadPitch + (var0.headPitch - var0.pheadPitch) * var3;
      float var16 = var0.pwalk + (var0.walk - var0.pwalk) * var3;
      float var17 = var0.pwalkAmp + (var0.walkAmp - var0.pwalkAmp) * var3;
      float var18 = (float)Math.sin(var16 * 1.6F) * var17 * 55.0F;
      float var19 = 0.0F;
      if (var0.type == 6) {
         var19 = Math.min(1.0F, (var0.pswell + (var0.swell - var0.pswell) * var3) / 30.0F);
      }

      boolean var20 = var0.type == 6 && var0.swell > 0 && var0.age / 3 % 2 == 0;
      GL11.glBindTexture(3553, var10.tex);
      GL11.glPushMatrix();
      GL11.glTranslated(var4 - var2.camX, var6 - var2.camY, var8 - var2.camZ);
      GL11.glRotatef(-var13, 0.0F, 1.0F, 0.0F);
      if (var0.babyAge > 0) {
         GL11.glScalef(0.5F, 0.5F, 0.5F);
      }

      float modelScale = var10.scale * var0.scale;
      if (modelScale != 1.0F) {
         GL11.glScalef(modelScale, modelScale, modelScale);
      }

      if (var0.dead && var0.type == Mobs.ENDER_DRAGON) {
         // Update 3.12: the dying dragon hangs in the air and shakes harder and harder, getting whiter and whiter
         float dt = (var0.deathTime + var3) / Dragon.DEATH_TICKS;
         GL11.glTranslatef((float)Math.sin((var0.age + var0.deathTime + var3) * 2.3F) * 0.12F * dt, (float)Math.sin((var0.deathTime + var3) * 1.9F) * 0.08F * dt, 0.0F);
         var12[0] = var12[1] = var12[2] = 0.8F + 0.9F * dt;
      } else if (var0.dead) {
         float var21 = Math.min(1.0F, (var0.deathTime + var3) / 20.0F);
         GL11.glTranslatef(0.0F, 0.3F * var21, 0.0F);
         GL11.glRotatef(var21 * 90.0F, 0.0F, 0.0F, 1.0F);
         GL11.glTranslatef(0.0F, -0.3F * var21, 0.0F);
      }

      if (!var0.dead) {
         // Update 3.11: the secondary motion (see Anim): the step bounces and rolls the body, it breathes, a hit throws it back
         GL11.glTranslatef(0.0F, Anim.bounce(var0.type, var16, var17, var0.onGround), 0.0F);
         GL11.glRotatef(Anim.roll(var0.type, var16, var17), 0.0F, 0.0F, 1.0F);
         GL11.glRotatef(Anim.hurtTilt(var0, var3), 1.0F, 0.0F, 0.0F);
         GL11.glScalef(1.0F, Anim.breathe(var0, var3), 1.0F);
      }

      if (var19 > 0.0F) {
         float var26 = 1.0F + (float)Math.sin(var19 * 100.0F) * var19 * 0.01F;
         float var22 = (1.0F + var19 * var19 * 0.4F) * var26;
         GL11.glScalef(var22, (1.0F + var19 * 0.2F) * var26, var22);
      }

      if (var20) {
         GL11.glTexEnvi(8960, 8704, 260);
      }

      if (var20) {
         var12[0] = var12[1] = var12[2] = 0.9F;
      }

      for (MobRender.Part var28 : var10.parts) {
         GL11.glPushMatrix();
         if (var28.kind == 12) {
            // Update 3.12: the rods of a blaze circle round its head, each ring at its own pace
            GL11.glRotatef((var0.age + var3) * (var28.ry > 30.0F ? -3.0F : 4.5F) + var28.ry, 0.0F, 1.0F, 0.0F);
         }

         GL11.glTranslatef(var28.px / 16.0F, var28.py / 16.0F, var28.pz / 16.0F);
         switch (var28.kind) {
            case 1:
               GL11.glRotatef(-Math.max(-50.0F, Math.min(50.0F, var14)), 0.0F, 1.0F, 0.0F);
               GL11.glRotatef(var15 + Anim.headPitch(var0, var3, var16, var17), 1.0F, 0.0F, 0.0F);
               GL11.glTranslatef(0.0F, 0.0F, Anim.peck(var0, var16, var17));
               float bh = Anim.babyHead(var0);
               if (bh != 1.0F) {
                  GL11.glScalef(bh, bh, bh);
               }
            case 2:
            case 9:
            default:
               break;
            case 3:
               GL11.glRotatef(var18 + Anim.spread(var0, var28.pz), 1.0F, 0.0F, 0.0F);
               break;
            case 4:
               GL11.glRotatef(-var18 + Anim.spread(var0, var28.pz), 1.0F, 0.0F, 0.0F);
               break;
            case 5:
            case 6:
               float var30 = var28.kind == 5 ? 1.0F : -1.0F;
               if (var0.type == 4 || var0.type == Mobs.ZOMBIE_PIGMAN) {
                  float var32 = (float)Math.sin((var0.age + var3) * 0.09F) * 3.0F;
                  float var25 = var0.hasTarget ? -90.0F : -35.0F;
                  if (var0.attackAnim > 0) {
                     var25 -= (var0.attackAnim - var3) * 3.0F;
                  }

                  GL11.glRotatef(var25 + var32 * var30 + (var0.hasTarget ? 0.0F : var18 * var30 * 0.5F), 1.0F, 0.0F, 0.0F);
               } else if (var0.type == 5) {
                  if (var0.hasTarget) {
                     GL11.glRotatef(-85.0F + (var28.kind == 6 ? 5 : 0), 1.0F, 0.0F, 0.0F);
                  } else {
                     GL11.glRotatef(-var18 * var30, 1.0F, 0.0F, 0.0F);
                  }
               } else if (var0.type == Mobs.IRON_GOLEM) {
                  // both arms swing with the step; a punch throws them up and over
                  float up = -var18 * var30 * 0.55F;
                  if (var0.attackAnim > 0) {
                     up = -100.0F + (10 - (var0.attackAnim - var3)) * 7.0F;
                  }

                  GL11.glRotatef(up, 1.0F, 0.0F, 0.0F);
               } else if (var0.type == Mobs.ENDERMAN) {
                  // the long arms hang and swing; when it is angry they come up and flail
                  float up = var0.hasTarget ? -55.0F + (float)Math.sin((var0.age + var3) * 0.5F + var30) * 9.0F : -var18 * var30 * 0.4F;
                  if (var0.attackAnim > 0) {
                     up -= (var0.attackAnim - var3) * 4.0F;
                  }

                  GL11.glRotatef(up, 1.0F, 0.0F, 0.0F);
               } else {
                  GL11.glRotatef(-var18 * var30, 1.0F, 0.0F, 0.0F);
               }
               break;
            case 7:
            case 8:
               float var29 = var0.onGround ? 0.0F : (float)(Math.sin((var0.age + var3) * 1.1F) * 50.0 + 40.0);
               GL11.glRotatef(var28.kind == 7 ? -var29 : var29, 0.0F, 0.0F, 1.0F);
               GL11.glRotatef(var28.kind == 7 ? 4.0F : -4.0F, 0.0F, 0.0F, 1.0F);
               break;
            case 12:
               GL11.glTranslatef(0.0F, (float)Math.sin((var0.age + var3) * 0.15F + var28.idx) * 0.07F, 0.0F);
               break;
            case 13:
               // the tentacles of a ghast sway
               GL11.glRotatef((float)Math.sin((var0.age + var3) * 0.12F + var28.idx * 0.9F) * 9.0F, 1.0F, 0.0F, 0.0F);
               GL11.glRotatef((float)Math.cos((var0.age + var3) * 0.1F + var28.idx * 1.3F) * 9.0F, 0.0F, 0.0F, 1.0F);
               break;
            case 14: {
               // Update 3.12: the jaw of an enderman / the dragon follows the head and drops open
               GL11.glRotatef(-Math.max(-50.0F, Math.min(50.0F, var14)), 0.0F, 1.0F, 0.0F);
               GL11.glRotatef(var15, 1.0F, 0.0F, 0.0F);
               float open;
               if (var0.type == Mobs.ENDERMAN) {
                  open = var0.angry > 0 ? 32.0F + (float)Math.sin((var0.age + var3) * 0.7F) * 6.0F : 0.0F;
                  GL11.glTranslatef(0.0F, 0.0F, -4.0F / 16.0F);
                  GL11.glRotatef(open, 1.0F, 0.0F, 0.0F);
                  GL11.glTranslatef(0.0F, 0.0F, 4.0F / 16.0F);
               } else {
                  open = var0.attackAnim > 0 ? 30.0F * Math.min(1.0F, var0.attackAnim / 6.0F) : 5.0F + (float)Math.sin((var0.age + var3) * 0.12F) * 3.0F;
                  GL11.glRotatef(open, 1.0F, 0.0F, 0.0F);
               }

               break;
            }
            case 15:
            case 16: {
               // the dragon's wings: two parts, the outer one lags behind the inner one - a wave runs along the wing
               float sgn = var28.px > 0.0F ? 1.0F : -1.0F;
               boolean perched = var0.type == Mobs.ENDER_DRAGON && var0.phase == Dragon.PERCH && !var0.dead;
               float ph = perched ? (var0.age + var3) * 0.06F : var16 * 0.5F;
               float amp = var0.dead ? 5.0F : (perched ? 14.0F : 32.0F);
               GL11.glRotatef(sgn * (amp * (float)Math.sin(ph) + 6.0F), 0.0F, 0.0F, 1.0F);
               if (var28.kind == 16) {
                  GL11.glTranslatef(sgn * 28.0F / 16.0F, 0.0F, 0.0F);
                  GL11.glRotatef(sgn * (amp * 0.7F * (float)Math.sin(ph - 0.9F) + 14.0F), 0.0F, 0.0F, 1.0F);
               }

               break;
            }
            case 17: {
               // the tail whips from side to side, more and more towards the tip
               int seg = (int)var28.rx;
               float wag = (float)Math.sin(var16 * 0.3F - seg * 0.55F) * (seg + 1) * 0.55F;
               float lift = (float)Math.cos(var16 * 0.3F - seg * 0.55F) * (seg + 1) * 0.3F;
               GL11.glTranslatef(wag / 16.0F, lift / 16.0F, 0.0F);
               break;
            }
            case 18:
               // the glass of an End crystal turns and bobs, the core turns the other way
               GL11.glTranslatef(0.0F, (float)Math.sin((var0.age + var3) * 0.1F) * 0.09F, 0.0F);
               GL11.glRotatef((var0.age + var3) * 3.0F, 0.0F, 1.0F, 0.0F);
               GL11.glRotatef(32.0F, 1.0F, 0.0F, 1.0F);
               break;
            case 19:
               GL11.glTranslatef(0.0F, (float)Math.sin((var0.age + var3) * 0.1F) * 0.09F, 0.0F);
               GL11.glRotatef(-(var0.age + var3) * 4.5F, 0.0F, 1.0F, 0.0F);
               GL11.glRotatef(32.0F, 1.0F, 0.0F, 1.0F);
               break;
            case 10:
            case 11:
               float var23 = var16 * 1.5F + var28.idx * 1.6F + (var28.kind == 11 ? 3.1F : 0.0F);
               float var24 = var28.kind == 10 ? 1.0F : -1.0F;
               GL11.glRotatef(var28.ry * 1.0F + (float)Math.sin(var23) * 22.0F * Math.max(0.15F, var17) * var24, 0.0F, 1.0F, 0.0F);
               GL11.glRotatef(var28.rz + (float)Math.max(0.0, Math.cos(var23)) * 15.0F * var17 * var24, 0.0F, 0.0F, 1.0F);
         }

         if (var0.type == 2 && var0.sheared && var28.color == 15132390 && !var20) {
            if (var28.kind == 2) {
               float[] skin = new float[]{var12[0] * 244.0F / 230.0F, var12[1] * 190.0F / 230.0F, var12[2] * 175.0F / 230.0F};
               drawBox(var10, var28, var11, skin);
            }
         } else if (var0.type == Mobs.PARROT && var28.color == MobModels312.PARROT_MARK) {
            int rgb = MobModels312.PARROT_RGB[Math.max(0, Math.min(MobModels312.PARROT_RGB.length - 1, var0.color))];
            float[] feathers = new float[]{var12[0] * (rgb >> 16 & 0xFF) / 230.0F, var12[1] * (rgb >> 8 & 0xFF) / 230.0F, var12[2] * (rgb & 0xFF) / 230.0F};
            drawBox(var10, var28, var11, feathers);
         } else if (var0.type == Mobs.VILLAGER && var28.color == MobModels312.ROBE_MARK) {
            int rgb = Trades.ROBE[Math.max(0, Math.min(Trades.ROBE.length - 1, var0.color))];
            float[] robe = new float[]{var12[0] * (rgb >> 16 & 0xFF) / 230.0F, var12[1] * (rgb >> 8 & 0xFF) / 230.0F, var12[2] * (rgb & 0xFF) / 230.0F};
            drawBox(var10, var28, var11, robe);
         } else if (var0.type == 2 && var28.color == 15132390 && !var20) {
            int var31 = SHEEP_RGB[var0.color];
            float[] var33 = new float[]{var12[0] * (var31 >> 16 & 0xFF) / 230.0F, var12[1] * (var31 >> 8 & 0xFF) / 230.0F, var12[2] * (var31 & 0xFF) / 230.0F};
            drawBox(var10, var28, var11, var33);
         } else {
            drawBox(var10, var28, var11, var12);
         }

         GL11.glPopMatrix();
      }

      if (var20) {
         GL11.glTexEnvi(8960, 8704, 8448);
      }

      GL11.glPopMatrix();
   }

   /** Update 3.12: fireballs are glowing cubes (an orange one with a yellow heart) that tumble as they fly; not lit by the world, they glow. */
   static void drawFireballs(Mobs ms, Renderer rend, float partial) {
      if (ms.fireballs.isEmpty()) {
         return;
      }

      GL11.glDisable(3553);
      for (Mobs312.Fireball f : ms.fireballs) {
         double x = f.px + (f.x - f.px) * partial - rend.camX;
         double y = f.py + (f.y - f.py) * partial - rend.camY;
         double z = f.pz + (f.z - f.pz) * partial - rend.camZ;
         float s = f.pearl ? 0.13F : (f.dragon ? 0.5F : (f.big ? 0.42F : 0.2F));
         GL11.glPushMatrix();
         GL11.glTranslated(x, y, z);
         GL11.glRotatef((f.life + partial) * 23.0F, 1.0F, 0.6F, 0.3F);
         for (int layer = 0; layer < 2; layer++) {
            float h = layer == 0 ? s : s * 0.55F;
            if (layer == 0) {
               if (f.pearl) {
                  GL11.glColor3f(0.08F, 0.42F, 0.36F);
               } else if (f.dragon) {
                  GL11.glColor3f(0.65F, 0.18F, 0.95F);
               } else {
                  GL11.glColor3f(1.0F, 0.45F, 0.06F);
               }
            } else if (f.pearl) {
               GL11.glColor3f(0.55F, 0.95F, 0.82F);
            } else if (f.dragon) {
               GL11.glColor3f(1.0F, 0.75F, 1.0F);
            } else {
               GL11.glColor3f(1.0F, 0.9F, 0.35F);
            }

            GL11.glBegin(7);
            for (int face = 0; face < 6; face++) {
               int axis = face / 2;
               float sign = face % 2 == 0 ? 1.0F : -1.0F;
               for (int c = 0; c < 4; c++) {
                  float u = c == 0 || c == 3 ? -h : h;
                  float v = c < 2 ? -h : h;
                  float vx = axis == 0 ? sign * h : (axis == 1 ? u : u);
                  float vy = axis == 1 ? sign * h : v;
                  float vz = axis == 2 ? sign * h : (axis == 0 ? u : v);
                  GL11.glVertex3f(vx, vy, vz);
               }
            }

            GL11.glEnd();
         }

         GL11.glPopMatrix();
      }

      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      GL11.glEnable(3553);
   }

   static void drawArrows(Mobs var0, Renderer var1, float var2) {
      if (!var0.arrows.isEmpty()) {
         GL11.glDisable(3553);

         for (Arrow var4 : var0.arrows) {
            double var5 = var4.px + (var4.x - var4.px) * var2 - var1.camX;
            double var7 = var4.py + (var4.y - var4.py) * var2 - var1.camY;
            double var9 = var4.pz + (var4.z - var4.pz) * var2 - var1.camZ;
            double var11 = var4.vx;
            double var13 = var4.vy;
            double var15 = var4.vz;
            if (var4.stuck) {
               var11 = var4.x - var4.px;
               var13 = 0.0;
               var15 = 0.0;
               var11 = 0.001;
               var13 = -0.002;
            }

            double var17 = Math.sqrt(var11 * var11 + var13 * var13 + var15 * var15);
            if (!(var17 < 1.0E-6)) {
               var11 /= var17;
               var13 /= var17;
               var15 /= var17;
               double var19 = -var15;
               double var21 = 0.0;
               double var23 = var11;
               double var25 = Math.sqrt(var19 * var19 + var23 * var23);
               if (var25 < 1.0E-4) {
                  var19 = 1.0;
                  var23 = 0.0;
                  var25 = 1.0;
               }

               var19 /= var25;
               var23 /= var25;
               double var27 = var13 * var23 - var15 * var21;
               double var29 = var15 * var19 - var11 * var23;
               double var31 = var11 * var21 - var13 * var19;
               double var33 = 0.6;
               double var35 = 0.025;

               for (int var37 = 0; var37 < 2; var37++) {
                  double var38 = var37 == 0 ? var19 : var27;
                  double var40 = var37 == 0 ? var21 : var29;
                  double var42 = var37 == 0 ? var23 : var31;
                  GL11.glBegin(7);
                  GL11.glColor3f(0.55F, 0.4F, 0.22F);
                  GL11.glVertex3d(var5 - var11 * var33 + var38 * var35, var7 - var13 * var33 + var40 * var35, var9 - var15 * var33 + var42 * var35);
                  GL11.glVertex3d(var5 - var11 * var33 - var38 * var35, var7 - var13 * var33 - var40 * var35, var9 - var15 * var33 - var42 * var35);
                  GL11.glColor3f(0.7F, 0.7F, 0.72F);
                  GL11.glVertex3d(var5 - var38 * var35, var7 - var40 * var35, var9 - var42 * var35);
                  GL11.glVertex3d(var5 + var38 * var35, var7 + var40 * var35, var9 + var42 * var35);
                  GL11.glEnd();
                  GL11.glBegin(7);
                  GL11.glColor3f(0.92F, 0.92F, 0.92F);
                  double var44 = 0.1;
                  double var46 = 0.08;
                  GL11.glVertex3d(var5 - var11 * var33 + var38 * var46, var7 - var13 * var33 + var40 * var46, var9 - var15 * var33 + var42 * var46);
                  GL11.glVertex3d(var5 - var11 * var33 - var38 * var46, var7 - var13 * var33 - var40 * var46, var9 - var15 * var33 - var42 * var46);
                  GL11.glVertex3d(
                     var5 - var11 * (var33 - var44) - var38 * var46,
                     var7 - var13 * (var33 - var44) - var40 * var46,
                     var9 - var15 * (var33 - var44) - var42 * var46
                  );
                  GL11.glVertex3d(
                     var5 - var11 * (var33 - var44) + var38 * var46,
                     var7 - var13 * (var33 - var44) + var40 * var46,
                     var9 - var15 * (var33 - var44) + var42 * var46
                  );
                  GL11.glEnd();
               }
            }
         }

         GL11.glEnable(3553);
      }
   }

   static {
      SHEEP_RGB[0] = 15132390;
      SHEEP_RGB[6] = 15769784;
      SHEEP_RGB[7] = 5789784;
      SHEEP_RGB[8] = 10526880;
      SHEEP_RGB[12] = 7030056;
      SHEEP_RGB[15] = 2763306;

      for (int c = 0; c < 16; c++) {
         if (SHEEP_RGB[c] == 0) {
            SHEEP_RGB[c] = Color312.fleece(c); // Update 3.12: a sheep can be dyed any colour
         }
      }
   }

   static final class Model {
      final ArrayList<MobRender.Part> parts = new ArrayList();
      int tw = 128;
      int th = 64;
      int tex;
      int cx = 0;
      int cy = 0;
      int rowH = 0;
      int type;
      /** Update 3.12: the whole model is drawn this many times bigger (the ghast). */
      float scale = 1.0F;

      MobRender.Part add(int var1, int var2, int var3, int var4, int var5, float var6, float var7, float var8, float var9, float var10, float var11) {
         MobRender.Part var12 = new MobRender.Part();
         var12.kind = var1;
         var12.color = var2;
         var12.w = var3;
         var12.h = var4;
         var12.d = var5;
         var12.ox = var6;
         var12.oy = var7;
         var12.oz = var8;
         var12.px = var9;
         var12.py = var10;
         var12.pz = var11;
         int var13 = 2 * var5 + 2 * var3;
         int var14 = var5 + var4;
         if (this.cx + var13 > this.tw) {
            this.cx = 0;
            this.cy = this.cy + this.rowH;
            this.rowH = 0;
         }

         var12.u = this.cx;
         var12.v = this.cy;
         this.cx += var13;
         this.rowH = Math.max(this.rowH, var14);
         var12.idx = this.parts.size();
         this.parts.add(var12);
         return var12;
      }

      MobRender.Part addLike(MobRender.Part var1, int var2, float var3, float var4, float var5) {
         MobRender.Part var6 = new MobRender.Part();
         var6.kind = var2;
         var6.color = var1.color;
         var6.w = var1.w;
         var6.h = var1.h;
         var6.d = var1.d;
         var6.ox = var1.ox;
         var6.oy = var1.oy;
         var6.oz = var1.oz;
         var6.px = var3;
         var6.py = var4;
         var6.pz = var5;
         var6.u = var1.u;
         var6.v = var1.v;
         var6.shared = true;
         var6.idx = this.parts.size();
         var6.rx = var1.rx;
         var6.ry = var1.ry;
         var6.rz = var1.rz;
         this.parts.add(var6);
         return var6;
      }
   }

   static final class Part {
      int kind;
      int w;
      int h;
      int d;
      int u;
      int v;
      int color;
      int idx;
      float ox;
      float oy;
      float oz;
      float px;
      float py;
      float pz;
      float rx;
      float ry;
      float rz;
      boolean shared;
   }
}
