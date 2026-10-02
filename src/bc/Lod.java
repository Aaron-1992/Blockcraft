package bc;

import bc.Gen.Col;
import bc.Structs.Bld;
import bc.Structs.Path;
import bc.Structs.Village;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;

public final class Lod {
   static final int N = 32;
   static final int MIN = 32;
   static final int MAXS = 1024;
   static final float LAND_SHIFT = 2.5F;
   static final float WATER_SHIFT = 0.2F;
   final Gen gen;
   final Renderer rend;
   final ConcurrentHashMap<Long, Lod.Tile> tiles = new ConcurrentHashMap();
   final ExecutorService pool;
   final ConcurrentLinkedQueue<Lod.Tile> uploads = new ConcurrentLinkedQueue();
   final AtomicInteger inflight = new AtomicInteger();
   long frame;
   int whiteTex;
   public int drawn;
   public int built;
   volatile boolean dead;
   double cx;
   double cz;
   double cy;
   int maxDist;
   int innerHalf;
   ArrayList<Lod.Tile> drawList = new ArrayList();

   public Lod(Renderer var1, Gen var2) {
      this.rend = var1;
      this.gen = var2;
      int var3 = Math.max(1, Math.min(3, Runtime.getRuntime().availableProcessors() - 1));
      this.pool = Executors.newFixedThreadPool(var3, var0 -> {
         Thread var1x = new Thread(var0, "lod-gen");
         var1x.setDaemon(true);
         var1x.setPriority(2);
         return var1x;
      });
   }

   void ensureTex() {
      if (this.whiteTex == 0) {
         this.whiteTex = GL11.glGenTextures();
         GL11.glBindTexture(3553, this.whiteTex);
         ByteBuffer var1 = BufferUtils.createByteBuffer(4);
         var1.put((byte)-1).put((byte)-1).put((byte)-1).put((byte)-1).flip();
         GL11.glTexParameteri(3553, 10241, 9728);
         GL11.glTexParameteri(3553, 10240, 9728);
         GL11.glTexImage2D(3553, 0, 6408, 1, 1, 0, 6408, 5121, var1);
      }
   }

   static long key(int var0, int var1, int var2) {
      return (var1 / var0 & 4194303L) << 40 | (var2 / var0 & 4194303L) << 18 | Integer.numberOfTrailingZeros(var0) & 31;
   }

   Lod.Tile tile(int var1, int var2, int var3) {
      long var4 = key(var1, var2, var3);
      Lod.Tile var6 = (Lod.Tile)this.tiles.get(var4);
      if (var6 == null) {
         var6 = new Lod.Tile(var1, var2, var3);
         Lod.Tile var7 = (Lod.Tile)this.tiles.putIfAbsent(var4, var6);
         if (var7 != null) {
            var6 = var7;
         }
      }

      return var6;
   }

   void request(Lod.Tile var1) {
      if (var1.state == 0 && this.inflight.get() < 8) {
         var1.state = 1;
         this.inflight.incrementAndGet();
         this.pool.execute(() -> {
            try {
               if (!this.dead) {
                  this.build(var1);
               }
            } catch (Throwable var6) {
               var6.printStackTrace();
               var1.state = 4;
            } finally {
               this.inflight.decrementAndGet();
            }
         });
      }
   }

   static float hash(int var0, int var1) {
      int var2 = var0 * 374761393 + var1 * 668265263;
      var2 = (var2 ^ var2 >>> 13) * 1274126177;
      return ((var2 ^ var2 >>> 16) & 65535) / 65535.0F;
   }

   static int pack(int var0, float var1, float var2) {
      float var3 = var1 * (1.0F + var2);
      int var4 = Math.min(255, (int)((var0 >> 16 & 0xFF) * var3));
      int var5 = Math.min(255, (int)((var0 >> 8 & 0xFF) * var3));
      int var6 = Math.min(255, (int)((var0 & 0xFF) * var3));
      return var4 | var5 << 8 | var6 << 16 | 0xFF000000;
   }

   static int code(int var0, int var1) {
      return var0 & 16777215 | var1 << 24;
   }

   void build(Lod.Tile var1) {
      int var2 = var1.size / 32;
      float var3 = Math.min(2.5F, 0.4F + var2 * 0.35F);
      int var4 = 34;
      int[] var5 = new int[var4 * var4];
      int[] var6 = new int[var4 * var4];
      boolean[] var7 = new boolean[var4 * var4];
      int[] var8 = new int[var4 * var4];
      int[] var9 = new int[var4 * var4];
      List<Village> var10 = var1.size <= 256 ? this.villagesFor(var1.x0 - 8, var1.z0 - 8, var1.x0 + var1.size + 8, var1.z0 + var1.size + 8) : null;

      for (int var11 = 0; var11 < var4; var11++) {
         for (int var12 = 0; var12 < var4; var12++) {
            int var13 = var1.x0 + (var12 - 1) * var2 + var2 / 2;
            int var14 = var1.z0 + (var11 - 1) * var2 + var2 / 2;
            Col var15 = this.gen.column(var13, var14);
            int var16 = var11 * var4 + var12;
            var8[var16] = var15.h;
            var9[var16] = var15.biome;
            boolean var18 = var15.h < 63;
            int var17;
            if (var18) {
               float var19 = Math.min(1.0F, (63 - var15.h) / 30.0F);
               int var20 = (int)(58.0F * (1.0F - var19 * 0.6F));
               int var21 = (int)(104.0F * (1.0F - var19 * 0.5F));
               int var22 = (int)(208.0F * (1.0F - var19 * 0.4F));
               var17 = var20 << 16 | var21 << 8 | var22;
               if (var15.biome == 5) {
               }
            } else {
               switch (var15.biome) {
                  case 1:
                     var17 = 14735012;
                     break;
                  case 2:
                  default:
                     var17 = shade(Gen.grassColorFor(var15.t, var15.hu), 0.8F);
                     break;
                  case 3:
                     int var52 = Gen.grassColorFor(var15.t, var15.hu);
                     var17 = (int)((var52 >> 16 & 0xFF) * 0.72F) << 16 | (int)((var52 >> 8 & 0xFF) * 0.76F) << 8 | (int)((var52 & 0xFF) * 0.72F);
                     break;
                  case 4:
                     var17 = 14931876;
                     break;
                  case 5:
                     var17 = 15594485;
                     break;
                  case 6:
                     if (var15.h > 125) {
                        var17 = 15791350;
                     } else if (var15.h > 98) {
                        var17 = 9079434;
                     } else {
                        var17 = shade(Gen.grassColorFor(var15.t, var15.hu), 0.8F);
                     }
               }
            }

            if (!var18 && var2 <= 4 && var10 != null) {
               for (Village var55 : var10) {
                  for (Path var59 : var55.paths) {
                     if (var13 >= var59.x0 && var13 <= var59.x1 && var14 >= var59.z0 && var14 <= var59.z1) {
                        var17 = var55.desert ? 14471328 : 8748922;
                     }
                  }
               }
            }

            var7[var16] = var18;
            var5[var16] = var18 ? 63 : var15.h + 1;
            var6[var16] = var17;
         }
      }

      ByteBuffer var43 = ByteBuffer.allocateDirect(786432).order(ByteOrder.nativeOrder());
      int[] var44 = new int[]{0};

      for (int var45 = 1; var45 <= 32; var45++) {
         for (int var47 = 1; var47 <= 32; var47++) {
            int var48 = var45 * var4 + var47;
            float var49 = var7[var48] ? 0.2F : var3;
            float var50 = var5[var48] - var49;
            float var51 = (var47 - 1) * var2;
            float var54 = var51 + var2;
            float var56 = (var45 - 1) * var2;
            float var58 = var56 + var2;
            float var60 = (hash(var1.x0 / var2 + var47, var1.z0 / var2 + var45) - 0.5F) * 0.12F;
            int var23 = var6[var48];
            int var24 = code(pack(var23, 1.0F, var60), var7[var48] ? 180 : 255);
            var43 = ensure(var43);
            float var25 = 0.04F * Math.max(1, var2 / 8);
            float var26 = var47 == 1 ? var51 - var25 : var51;
            float var27 = var47 == 32 ? var54 + var25 : var54;
            float var28 = var45 == 1 ? var56 - var25 : var56;
            float var29 = var45 == 32 ? var58 + var25 : var58;
            quad(var43, var44, var26, var50, var28, var27, var50, var28, var27, var50, var29, var26, var50, var29, var24);
            int[][] var30 = new int[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

            for (int var31 = 0; var31 < 4; var31++) {
               int var32 = var47 + var30[var31][0];
               int var33 = var45 + var30[var31][1];
               int var34 = var33 * var4 + var32;
               float var35 = var5[var34] - (var7[var34] ? 0.2F : var3);
               boolean var36 = var31 == 0 && var47 == 1 || var31 == 1 && var47 == 32 || var31 == 2 && var45 == 1 || var31 == 3 && var45 == 32;
               if (!var7[var48] || !var7[var34]) {
                  float var37;
                  if (var35 < var50) {
                     var37 = var36 ? var35 - 14.0F : var35;
                  } else {
                     if (!var36) {
                        continue;
                     }

                     var37 = var50 - 14.0F;
                  }

                  boolean var39 = var9[var48] == 2 || var9[var48] == 3 || var9[var48] == 6 && var8[var48] <= 98;
                  int var38;
                  if (var7[var48]) {
                     var38 = var23;
                  } else if (var39) {
                     float var40 = var50 - var35;
                     if (var40 <= 1.5F) {
                        var38 = (int)((var23 >> 16 & 0xFF) * 0.82F) << 16 | (int)((var23 >> 8 & 0xFF) * 0.82F) << 8 | (int)((var23 & 0xFF) * 0.82F);
                     } else {
                        var38 = var40 <= 5.0F ? 7033395 : 8026746;
                     }
                  } else {
                     var38 = (int)((var23 >> 16 & 0xFF) * 0.82F) << 16 | (int)((var23 >> 8 & 0xFF) * 0.82F) << 8 | (int)((var23 & 0xFF) * 0.82F);
                  }

                  float var61 = var31 < 2 ? 0.72F : 0.84F;
                  int var41 = code(pack(var38, var61, var60), var31 == 0 ? 210 : (var31 == 1 ? 195 : (var31 == 2 ? 240 : 225)));
                  var43 = ensure(var43);
                  switch (var31) {
                     case 0:
                        quad(var43, var44, var51, var50, var58, var51, var50, var56, var51, var37, var56, var51, var37, var58, var41);
                        break;
                     case 1:
                        quad(var43, var44, var54, var50, var56, var54, var50, var58, var54, var37, var58, var54, var37, var56, var41);
                        break;
                     case 2:
                        quad(var43, var44, var51, var50, var56, var54, var50, var56, var54, var37, var56, var51, var37, var56, var41);
                        break;
                     default:
                        quad(var43, var44, var54, var50, var58, var51, var50, var58, var51, var37, var58, var54, var37, var58, var41);
                  }
               }
            }
         }
      }

      if (var1.size <= 256) {
         Lod.Mb var46 = new Lod.Mb();
         var46.bb = var43;
         var46.n = var44[0];

         try {
            this.trees(var1, var46, var10, var3);
            if (var10 != null) {
               this.buildings(var1, var46, var10);
            }
         } catch (Throwable var42) {
            var42.printStackTrace();
         }

         var43 = var46.bb;
         var44[0] = var46.n;
      }

      var43.flip();
      var1.data = var43;
      var1.vcount = var44[0];
      var1.state = 2;
      this.uploads.add(var1);
   }

   List<Village> villagesFor(int var1, int var2, int var3, int var4) {
      int var5 = 320;
      ArrayList var6 = new ArrayList();
      int var7 = Math.floorDiv(var1 - 100, var5);
      int var8 = Math.floorDiv(var3 + 100, var5);
      int var9 = Math.floorDiv(var2 - 100, var5);
      int var10 = Math.floorDiv(var4 + 100, var5);

      for (int var11 = var7; var11 <= var8; var11++) {
         for (int var12 = var9; var12 <= var10; var12++) {
            Village var13 = this.gen.structs.village(var11, var12);
            if (var13.valid && var13.maxX + 2 >= var1 && var13.minX - 2 <= var3 && var13.maxZ + 2 >= var2 && var13.minZ - 2 <= var4) {
               var6.add(var13);
            }
         }
      }

      return var6;
   }

   static int shade(int var0, float var1) {
      return (int)((var0 >> 16 & 0xFF) * var1) << 16 | (int)((var0 >> 8 & 0xFF) * var1) << 8 | (int)((var0 & 0xFF) * var1);
   }

   static void box(Lod.Mb var0, float var1, float var2, float var3, float var4, float var5, float var6, int var7, float var8) {
      if (var0.bb.remaining() < 448) {
         ByteBuffer var9 = ByteBuffer.allocateDirect(var0.bb.capacity() * 2).order(ByteOrder.nativeOrder());
         var0.bb.flip();
         var9.put(var0.bb);
         var0.bb = var9;
      }

      ByteBuffer var16 = var0.bb;
      int[] var10 = new int[]{0};
      int var11 = pack(var7, 1.0F, var8);
      int var12 = code(pack(var7, 0.82F, var8), 240);
      int var13 = code(pack(var7, 0.68F, var8), 210);
      int var14 = code(var12, 225);
      int var15 = code(var13, 195);
      quad(var16, var10, var1, var5, var3, var4, var5, var3, var4, var5, var6, var1, var5, var6, var11);
      quad(var16, var10, var1, var5, var3, var4, var5, var3, var4, var2, var3, var1, var2, var3, var12);
      quad(var16, var10, var4, var5, var6, var1, var5, var6, var1, var2, var6, var4, var2, var6, var14);
      quad(var16, var10, var1, var5, var6, var1, var5, var3, var1, var2, var3, var1, var2, var6, var13);
      quad(var16, var10, var4, var5, var3, var4, var5, var6, var4, var2, var6, var4, var2, var3, var15);
      var0.n = var0.n + var10[0];
   }

   void trees(Lod.Tile var1, Lod.Mb var2, List<Village> var3, float var4) {
      int var5 = Math.floorDiv(var1.x0, 16);
      int var6 = Math.floorDiv(var1.x0 + var1.size - 1, 16);
      int var7 = Math.floorDiv(var1.z0, 16);
      int var8 = Math.floorDiv(var1.z0 + var1.size - 1, 16);
      boolean var9 = var1.size <= 128;

      for (int var10 = var5; var10 <= var6; var10++) {
         for (int var11 = var7; var11 <= var8; var11++) {
            Random var12 = new Random(this.gen.seed * 7919L ^ (var10 * 734287L + var11 * 912931L) * 31L + 17L);

            for (int var13 = 0; var13 < 16; var13++) {
               int var14 = var12.nextInt(16);
               int var15 = var12.nextInt(16);
               float var16 = var12.nextFloat();
               int var17 = var12.nextInt(1000);
               int var18 = var12.nextInt(3);
               int var19 = var12.nextInt();
               int var20 = var10 * 16 + var14;
               int var21 = var11 * 16 + var15;
               if (var20 >= var1.x0 && var20 < var1.x0 + var1.size && var21 >= var1.z0 && var21 < var1.z0 + var1.size && !(var16 > 0.85F)) {
                  Col var22 = this.gen.column(var20, var21);
                  if (var22.h >= 64 && var22.h <= 235 && !this.gen.entrance(var20, var21)) {
                     int var23;
                     switch (var22.biome) {
                        case 2:
                           if (var16 > 0.035F) {
                              continue;
                           }

                           var23 = 0;
                           break;
                        case 3:
                           if (!(var16 > 0.85F)) {
                              var23 = (byte)(var17 < 250 ? 1 : 0);
                              break;
                           }
                        case 4:
                        default:
                           continue;
                        case 5:
                           if (var16 > 0.22F) {
                              continue;
                           }

                           var23 = 2;
                           break;
                        case 6:
                           if (var22.h > 98 || var16 > 0.12F) {
                              continue;
                           }

                           var23 = 2;
                     }

                     boolean var24 = false;

                     for (Village var26 : var3 == null ? Collections.<Village>emptyList() : var3) {
                        for (Bld var28 : var26.blds) {
                           if (var20 >= var28.bx0 - 3 && var20 <= var28.bx1 + 3 && var21 >= var28.bz0 - 3 && var21 <= var28.bz1 + 3) {
                              var24 = true;
                           }
                        }
                     }

                     if (!var24) {
                        float var47 = var20 - var1.x0 + 0.5F;
                        float var48 = var21 - var1.z0 + 0.5F;
                        float var49 = var22.h + 1 - Math.min(2.5F, 0.4F + var1.size / 32 * 0.35F);
                        float var50 = var22.h + 1;
                        float var29 = (hash(var20, var21) - 0.5F) * 0.14F;
                        Random var32 = new Random(var19);
                        int var30;
                        int var31;
                        if (var23 == 0) {
                           int var33 = Gen.foliageColorFor(var22.t, var22.hu);
                           var30 = shade(var33, 0.66F);
                           var31 = 7034162;
                        } else if (var23 == 1) {
                           var30 = shade(8431445, 0.66F);
                           var31 = 13816512;
                        } else {
                           var30 = shade(6396257, 0.58F);
                           var31 = 3877398;
                        }

                        if (var23 == 2) {
                           int var53 = 7 + var18 * 2 + var32.nextInt(3);
                           float var55 = var50 + var53;
                           int var56 = var53 >= 10 ? 4 : 3;
                           if (var9) {
                              box(var2, var47 - 0.5F, var49, var48 - 0.5F, var47 + 0.5F, var55, var48 + 0.5F, var31, 0.0F);

                              for (float var57 = var50 + 2.0F; var57 <= var55 + 1.0F; var57 += 2.0F) {
                                 int var58 = (int)(var55 + 1.0F - var57);
                                 float var59 = Math.max(spR(var58, var56), spR(var58 - 1, var56)) + 0.5F;
                                 float var60 = 0.82F + 0.3F * (var57 - var50) / var53;
                                 box(
                                    var2,
                                    var47 - var59,
                                    var57,
                                    var48 - var59,
                                    var47 + var59,
                                    Math.min(var57 + 2.0F, var55 + 2.0F),
                                    var48 + var59,
                                    shade(var30, var60),
                                    var29
                                 );
                              }
                           } else {
                              box(var2, var47 - 0.5F, var49, var48 - 0.5F, var47 + 0.5F, var55, var48 + 0.5F, var31, 0.0F);
                              box(var2, var47 - 2.5F, var50 + 2.0F, var48 - 2.5F, var47 + 2.5F, var50 + var53 * 0.55F, var48 + 2.5F, shade(var30, 0.85F), var29);
                              box(var2, var47 - 1.5F, var50 + var53 * 0.55F, var48 - 1.5F, var47 + 1.5F, var55 + 1.0F, var48 + 1.5F, var30, var29);
                           }
                        } else if (var23 == 1) {
                           int var52 = 6 + var18 + var32.nextInt(2);
                           float var54 = var50 + var52 - 1.0F;
                           box(var2, var47 - 0.5F, var49, var48 - 0.5F, var47 + 0.5F, var50 + var52 + 1.0F, var48 + 0.5F, var31, 0.0F);
                           if (var9) {
                              canopy(var2, var47, var54, var48, 2.3F, 3.0F, 2.3F, var30, var29, 2);
                              canopy(var2, var47, var54 + 2.0F, var48, 1.4F, 1.6F, 1.4F, var30, var29, 2);
                           } else {
                              box(var2, var47 - 1.5F, var54 - 3.0F, var48 - 1.5F, var47 + 1.5F, var54 + 3.0F, var48 + 1.5F, var30, var29);
                           }
                        } else {
                           boolean var51 = var32.nextInt(7) == 0;
                           int var34 = 5 + var18 + var32.nextInt(2) + (var51 ? 3 : 0);
                           float var35 = var50 + var34 - 1.0F;
                           float var36 = var51 ? 4.2F : 3.1F;
                           float var37 = var51 ? 3.2F : 2.5F;
                           box(
                              var2,
                              var47 - 0.5F,
                              var49,
                              var48 - 0.5F,
                              var47 + (var51 ? 1.5F : 0.5F),
                              var50 + var34 + 1.0F,
                              var48 + (var51 ? 1.5F : 0.5F),
                              var31,
                              0.0F
                           );
                           if (!var9) {
                              box(
                                 var2,
                                 var47 - var36 + 0.5F,
                                 var35 - var37,
                                 var48 - var36 + 0.5F,
                                 var47 + var36 - 0.5F,
                                 var35 + var37,
                                 var48 + var36 - 0.5F,
                                 shade(var30, 0.92F),
                                 var29
                              );
                              box(var2, var47 - 1.5F, var35 + var37, var48 - 1.5F, var47 + 1.5F, var35 + var37 + 2.0F, var48 + 1.5F, var30, var29);
                           } else {
                              canopy(var2, var47, var35, var48, var36, var37, var36, var30, var29, 1);
                              Random var38 = new Random(var19 * 31L + 7L);
                              int var39 = 2 + var38.nextInt(3);

                              for (int var40 = 0; var40 < var39; var40++) {
                                 double var41 = var38.nextDouble() * Math.PI * 2.0;
                                 double var43 = var51 ? 3.6 : 2.7;
                                 float var45 = var47 + (float)Math.round(Math.cos(var41) * var43);
                                 float var46 = var48 + (float)Math.round(Math.sin(var41) * var43);
                                 canopy(
                                    var2,
                                    var45,
                                    var35 - var38.nextInt(3),
                                    var46,
                                    var51 ? 2.6F : 2.1F,
                                    var51 ? 2.1F : 1.8F,
                                    var51 ? 2.6F : 2.1F,
                                    var30,
                                    var29,
                                    2
                                 );
                              }

                              canopy(var2, var47, var35 + (var51 ? 3 : 2), var48, var51 ? 2.4F : 1.7F, 1.4F, var51 ? 2.4F : 1.7F, var30, var29, 2);
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   static int spR(int var0, int var1) {
      if (var0 < 0) {
         return 0;
      }

      int var2 = Math.min(var1, (var0 + 1) / 2);
      if (var0 % 2 == 0 && var0 > 1) {
         var2--;
      }

      return Math.max(0, var2);
   }

   static void canopy(Lod.Mb var0, float var1, float var2, float var3, float var4, float var5, float var6, int var7, float var8, int var9) {
      int var10 = (int)Math.ceil(var5);
      int var11 = -var10;

      while (var11 < var10) {
         float var12 = var11 + 0.5F * var9;
         float var13 = var12 / var5;
         float var14 = 1.0F - var13 * var13;
         if (!(var14 <= 0.05F)) {
            float var15 = (float)Math.sqrt(var14);
            float var16 = (float)Math.floor(var4 * var15) + 0.5F;
            float var17 = (float)Math.floor(var6 * var15) + 0.5F;
            float var18 = 0.84F + 0.22F * (var11 + var10) / (2.0F * var10);
            box(var0, var1 - var16, var2 + var11, var3 - var17, var1 + var16, var2 + var11 + var9, var3 + var17, shade(var7, var18), var8);
         }

         var11 += var9;
      }
   }

   void buildings(Lod.Tile var1, Lod.Mb var2, List<Village> var3) {
      float var4 = var1.x0;
      float var5 = var1.z0;

      for (Village var7 : var3) {
         for (Bld var9 : var7.blds) {
            if (var9.bx1 >= var1.x0 - 4 && var9.bx0 <= var1.x0 + var1.size + 4 && var9.bz1 >= var1.z0 - 4 && var9.bz0 <= var1.z0 + var1.size + 4) {
               if (var9.type == 1) {
                  int var25 = var9.desert ? 14208152 : 8026746;
                  box(var2, var9.x0 - var4, var9.y0 - 4, var9.z0 - var5, var9.x0 + 3 - var4, var9.y0 + 1, var9.z0 + 3 - var5, var25, 0.02F);
                  int[][] var26 = new int[][]{{0, 0}, {2, 0}, {0, 2}, {2, 2}};
                  int var27 = 7034162;

                  for (int[] var31 : var26) {
                     box(
                        var2,
                        var9.x0 + var31[0] - var4,
                        var9.y0 + 1,
                        var9.z0 + var31[1] - var5,
                        var9.x0 + var31[0] + 1 - var4,
                        var9.y0 + 4,
                        var9.z0 + var31[1] + 1 - var5,
                        var27,
                        0.0F
                     );
                  }

                  box(
                     var2,
                     var9.x0 - var4,
                     var9.y0 + 4,
                     var9.z0 - var5,
                     var9.x0 + 3 - var4,
                     var9.y0 + 5,
                     var9.z0 + 3 - var5,
                     var9.desert ? 14208152 : 9071678,
                     0.02F
                  );
               } else {
                  boolean var10 = var9.type == 2;
                  int var11 = var10 ? 8158332 : (var9.desert ? 14537376 : 10255950);
                  int var12 = var10 ? 6842472 : (var9.desert ? 13813392 : 8282680);
                  float var13 = var9.bx0 - var4;
                  float var14 = var9.bz0 - var5;
                  float var15 = var9.bx1 + 1 - var4;
                  float var16 = var9.bz1 + 1 - var5;
                  box(var2, var13, var9.y0 - 4, var14, var15, var9.y0 + var9.wallH + 1, var16, var11, 0.02F);
                  int var17 = (var9.w + 1) / 2;

                  for (int var18 = 0; var18 < var17; var18++) {
                     int[] var19 = Structs.worldPos(var9, var18, -1);
                     int[] var20 = Structs.worldPos(var9, var9.w - 1 - var18, var9.d);
                     float var21 = Math.min(var19[0], var20[0]) - var4;
                     float var22 = Math.max(var19[0], var20[0]) + 1 - var4;
                     float var23 = Math.min(var19[1], var20[1]) - var5;
                     float var24 = Math.max(var19[1], var20[1]) + 1 - var5;
                     box(var2, var21, var9.y0 + var9.wallH + 1 + var18, var23, var22, var9.y0 + var9.wallH + 2 + var18, var24, var12, 0.03F);
                  }

                  if (var10) {
                     int[] var32 = Structs.worldPos(var9, 1, var9.d - 1);
                     int[] var33 = Structs.worldPos(var9, var9.w - 2, var9.d - 1);
                     float var34 = Math.min(var32[0], var33[0]) - var4;
                     float var35 = Math.max(var32[0], var33[0]) + 1 - var4;
                     float var36 = Math.min(var32[1], var33[1]) - var5;
                     float var37 = Math.max(var32[1], var33[1]) + 1 - var5;
                     box(var2, var34, var9.y0 + var9.wallH + 1, var36, var35, var9.y0 + var9.wallH + 5, var37, var11, 0.02F);
                  }
               }
            }
         }
      }
   }

   static ByteBuffer ensure(ByteBuffer var0) {
      if (var0.remaining() >= 128) {
         return var0;
      }

      ByteBuffer var1 = ByteBuffer.allocateDirect(var0.capacity() * 2).order(ByteOrder.nativeOrder());
      var0.flip();
      var1.put(var0);
      return var1;
   }

   static void v(ByteBuffer var0, float var1, float var2, float var3, int var4) {
      var0.putFloat(var1).putFloat(var2).putFloat(var3).putInt(var4);
   }

   static void quad(
      ByteBuffer var0,
      int[] var1,
      float var2,
      float var3,
      float var4,
      float var5,
      float var6,
      float var7,
      float var8,
      float var9,
      float var10,
      float var11,
      float var12,
      float var13,
      int var14
   ) {
      v(var0, var2, var3, var4, var14);
      v(var0, var5, var6, var7, var14);
      v(var0, var8, var9, var10, var14);
      v(var0, var11, var12, var13, var14);
      var1[0] += 4;
   }

   void uploadPending() {
      int var1 = 0;

      Lod.Tile var2;
      while (var1 < 3 && (var2 = (Lod.Tile)this.uploads.poll()) != null) {
         var1++;
         if (this.dead) {
            var2.data = null;
         } else if (var2.vcount == 0) {
            var2.state = 4;
         } else {
            var2.vbo = GL15.glGenBuffers();
            GL15.glBindBuffer(34962, var2.vbo);
            GL15.glBufferData(34962, var2.data, 35044);
            GL15.glBindBuffer(34962, 0);
            var2.count = var2.vcount;
            var2.data = null;
            var2.state = 3;
            this.built++;
         }
      }
   }

   static boolean ready(Lod.Tile var0) {
      return var0.state == 3 || var0.state == 4;
   }

   public void render(double var1, double var3, double var5, int var7, int var8, float var9) {
      this.ensureTex();
      this.frame++;
      this.cx = var1;
      this.cz = var5;
      this.cy = var3;
      this.maxDist = var7 * 16;
      this.innerHalf = Math.max(0, (var8 - 1) * 16);
      this.uploadPending();
      this.drawList.clear();
      int var10 = (int)Math.floor(var1);
      int var11 = (int)Math.floor(var5);
      int var12 = Math.floorDiv(var10 - this.maxDist, 1024);
      int var13 = Math.floorDiv(var10 + this.maxDist, 1024);
      int var14 = Math.floorDiv(var11 - this.maxDist, 1024);
      int var15 = Math.floorDiv(var11 + this.maxDist, 1024);
      ArrayList<int[]> var16 = new ArrayList<>();

      for (int var17 = var12; var17 <= var13; var17++) {
         for (int var18 = var14; var18 <= var15; var18++) {
            var16.add(new int[]{var17 * 1024, var18 * 1024});
         }
      }

      var16.sort((var1x, var2) -> Double.compare(this.nearest(var1x[0], var1x[1], 1024), this.nearest(var2[0], var2[1], 1024)));

      for (int[] var22 : var16) {
         this.visit(var22[0], var22[1], 1024);
      }

      GL13.glActiveTexture(33984);
      GL11.glEnable(3553);
      GL11.glBindTexture(3553, this.whiteTex);
      GL11.glTexEnvi(8960, 8704, 34160);
      GL11.glTexEnvi(8960, 34161, 8448);
      GL11.glTexEnvi(8960, 34176, 34168);
      GL11.glTexEnvi(8960, 34177, 34166);
      GL11.glTexEnvi(8960, 34162, 7681);
      GL11.glTexEnvi(8960, 34184, 34168);
      float var21 = 0.3F + 0.7F * var9;
      GL11.glTexEnvfv(8960, 8705, new float[]{var21, var21, var21, 1.0F});
      GL11.glDisable(3008);
      GL11.glDisable(3042);
      GL11.glEnableClientState(32884);
      GL11.glEnableClientState(32886);
      GL11.glEnable(32823);
      GL11.glPolygonOffset(1.0F, 8.0F);
      this.drawn = 0;

      for (Lod.Tile var19 : this.drawList) {
         if (var19.state == 3) {
            GL15.glBindBuffer(34962, var19.vbo);
            GL11.glVertexPointer(3, 5126, 16, 0L);
            GL11.glColorPointer(4, 5121, 16, 12L);
            GL11.glPushMatrix();
            GL11.glTranslated(var19.x0 - var1, -var3, var19.z0 - var5);
            GL11.glDrawArrays(7, 0, var19.count);
            GL11.glPopMatrix();
            this.drawn++;
         }
      }

      GL11.glDisable(32823);
      GL15.glBindBuffer(34962, 0);
      GL11.glDisableClientState(32886);
      GL11.glDisableClientState(32884);
      GL11.glTexEnvi(8960, 8704, 8448);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      if (this.frame % 120L == 0L) {
         this.evict();
      }
   }

   double nearest(int var1, int var2, int var3) {
      double var4 = Math.max(Math.max(var1 - this.cx, this.cx - (var1 + var3)), 0.0);
      double var6 = Math.max(Math.max(var2 - this.cz, this.cz - (var2 + var3)), 0.0);
      return Math.sqrt(var4 * var4 + var6 * var6);
   }

   boolean insideInner(int var1, int var2, int var3) {
      double var4 = Math.max(Math.abs(var1 - this.cx), Math.abs(var1 + var3 - this.cx));
      double var6 = Math.max(Math.abs(var2 - this.cz), Math.abs(var2 + var3 - this.cz));
      double var8 = this.innerHalf + 8;
      return var4 * var4 + var6 * var6 <= var8 * var8;
   }

   boolean covered(int var1, int var2) {
      Lod.Tile var3 = this.tile(32, var1, var2);
      var3.lastUse = this.frame;
      if (var3.coverFrame != 0L && this.frame - var3.coverFrame < 15L) {
         return var3.covered;
      }

      boolean var4 = true;

      for (int var5 = var1 >> 4; var4 && var5 <= var1 + 32 - 1 >> 4; var5++) {
         for (int var6 = var2 >> 4; var4 && var6 <= var2 + 32 - 1 >> 4; var6++) {
            if (!this.rend.chunkMeshed(var5, var6)) {
               var4 = false;
            }
         }
      }

      var3.coverFrame = this.frame;
      var3.covered = var4;
      return var4;
   }

   void visit(int var1, int var2, int var3) {
      double var4 = this.nearest(var1, var2, var3);
      if (!(var4 > this.maxDist)) {
         if (var3 != 32 || !this.insideInner(var1, var2, var3) || !this.covered(var1, var2)) {
            if (this.rend.visible((float)(var1 + var3 / 2 - this.cx), (float)(100.0 - this.cy), (float)(var2 + var3 / 2 - this.cz), var3 * 0.78F + 80.0F)) {
               boolean var6 = var3 > 32 && var4 < var3 * 3.0;
               if (!var6) {
                  Lod.Tile var16 = this.tile(var3, var1, var2);
                  var16.lastUse = this.frame;
                  if (!ready(var16)) {
                     this.request(var16);
                  } else {
                     this.drawList.add(var16);
                  }
               } else {
                  int var7 = var3 / 2;
                  int[][] var8 = new int[][]{{var1, var2}, {var1 + var7, var2}, {var1, var2 + var7}, {var1 + var7, var2 + var7}};
                  boolean var9 = true;

                  for (int[] var13 : var8) {
                     if (!(var13[0] + var7 <= this.cx - this.maxDist)
                        && !(var13[0] >= this.cx + this.maxDist)
                        && !(var13[1] + var7 <= this.cz - this.maxDist)
                        && !(var13[1] >= this.cz + this.maxDist)
                        && (var7 != 32 || !this.insideInner(var13[0], var13[1], var7) || !this.covered(var13[0], var13[1]))) {
                        Lod.Tile var14 = this.tile(var7, var13[0], var13[1]);
                        if (!ready(var14)) {
                           var9 = false;
                           this.request(var14);
                        }

                        var14.lastUse = this.frame;
                     }
                  }

                  Lod.Tile var17 = this.tile(var3, var1, var2);
                  var17.lastUse = this.frame;
                  if (var9) {
                     for (int[] var24 : var8) {
                        this.visit(var24[0], var24[1], var7);
                     }
                  } else if (!ready(var17)) {
                     this.request(var17);

                     for (int[] var25 : var8) {
                        Lod.Tile var15 = this.tile(var7, var25[0], var25[1]);
                        if (ready(var15)) {
                           this.visit(var25[0], var25[1], var7);
                        }
                     }
                  } else {
                     this.drawList.add(var17);
                  }
               }
            }
         }
      }
   }

   void evict() {
      Iterator var1 = this.tiles.entrySet().iterator();

      while (var1.hasNext()) {
         Lod.Tile var2 = (Lod.Tile)((Entry)var1.next()).getValue();
         if (this.frame - var2.lastUse > 900L && (var2.state == 3 || var2.state == 4 || var2.state == 0)) {
            if (var2.vbo != 0) {
               GL15.glDeleteBuffers(var2.vbo);
            }

            var1.remove();
         }
      }
   }

   public void dispose() {
      this.dead = true;
      this.pool.shutdownNow();

      for (Lod.Tile var2 : this.tiles.values()) {
         if (var2.vbo != 0) {
            GL15.glDeleteBuffers(var2.vbo);
         }
      }

      this.tiles.clear();
   }

   static final class Mb {
      ByteBuffer bb;
      int n;
   }

   static final class Tile {
      final int size;
      final int x0;
      final int z0;
      volatile int state;
      int vbo;
      int count;
      volatile ByteBuffer data;
      volatile int vcount;
      long lastUse;
      long coverFrame;
      boolean covered;

      Tile(int var1, int var2, int var3) {
         this.size = var1;
         this.x0 = var2;
         this.z0 = var3;
      }
   }
}
