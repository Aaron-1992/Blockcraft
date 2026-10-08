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
import java.util.concurrent.atomic.AtomicLong;
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
   /** Update 3.11: what the world really looks like where it was changed (null: the generator only). */
   final LodMap map;
   final AtomicLong epoch = new AtomicLong();
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
      this(var1, var2, null);
   }

   public Lod(Renderer var1, Gen var2, LodMap map) {
      this.rend = var1;
      this.gen = var2;
      this.map = map;
      if (map != null) {
         map.listener = this::invalidate;
      }

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

   /**
    * Builds the mesh of a tile: 32 x 32 cells, one column each (the surface of the cell's middle), walls down to lower neighbours, trees and
    * houses as boxes. Update 3.11: where the world has a {@link LodMap.Sum summary} of the chunk (it was changed) the cell shows what is really
    * there; elsewhere it is still the generator. Every quad is counter-clockwise seen from the outside (back faces are culled).
    */
   void build(Lod.Tile t) {
      final int cs = t.size / 32; // blocks per cell
      final float sink = Math.min(2.5F, 0.4F + cs * 0.35F); // the land is sunk a little: it hides under the real terrain
      final int w = 34;
      t.builtEpoch = this.epoch.get();
      int[] topY = new int[w * w]; // the plane of the cell's surface: y of its top block + 1
      int[] rgb = new int[w * w];
      boolean[] wet = new boolean[w * w];
      int[] terrain = new int[w * w]; // height of the terrain (for the wall colours of mountains)
      int[] biome = new int[w * w];
      int[] cliff = new int[w * w]; // colour of the wall under a real surface (0 = the biome rules)
      List<Village> villages = t.size <= 256 ? this.villagesFor(t.x0 - 8, t.z0 - 8, t.x0 + t.size + 8, t.z0 + t.size + 8) : null;

      for (int cz = 0; cz < w; cz++) {
         for (int cx = 0; cx < w; cx++) {
            int x = t.x0 + (cx - 1) * cs + cs / 2;
            int z = t.z0 + (cz - 1) * cs + cs / 2;
            int i = cz * w + cx;
            LodMap.Sum sum = this.map == null ? null : this.map.get(x >> 4, z >> 4);
            if (sum != null) {
               int k = (z & 15) * 16 + (x & 15);
               wet[i] = sum.depth[k] > 0;
               topY[i] = sum.top[k];
               rgb[i] = sum.rgb[k];
               cliff[i] = sum.side[k];
               terrain[i] = sum.top[k] - 1;
               biome[i] = 2;
               continue;
            }

            Col col = this.gen.column(x, z);
            terrain[i] = col.h;
            biome[i] = col.biome;
            boolean sea = col.h < 63;
            int c;
            if (sea) {
               float d = Math.min(1.0F, (63 - col.h) / 30.0F);
               c = (int)(58.0F * (1.0F - d * 0.6F)) << 16 | (int)(104.0F * (1.0F - d * 0.5F)) << 8 | (int)(208.0F * (1.0F - d * 0.4F));
               if (col.biome > 6) {
                  c = Biomes.lodWater(col.biome, c);
               }
            } else {
               c = Biomes.lodColor(col.biome, col.h, col.t, col.hu);
            }

            if (!sea && cs <= 4 && villages != null) {
               for (Village v : villages) {
                  for (Path p : v.paths) {
                     if (x >= p.x0 && x <= p.x1 && z >= p.z0 && z <= p.z1) {
                        c = v.desert ? 14471328 : 8748922;
                     }
                  }
               }
            }

            wet[i] = sea;
            topY[i] = sea ? 63 : col.h + 1;
            rgb[i] = c;
         }
      }

      ByteBuffer bb = ByteBuffer.allocateDirect(786432).order(ByteOrder.nativeOrder());
      int[] n = new int[]{0};
      int[][] dirs = new int[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
      for (int cz = 1; cz <= 32; cz++) {
         for (int cx = 1; cx <= 32; cx++) {
            int i = cz * w + cx;
            float y = topY[i] - (wet[i] ? 0.2F : sink);
            float x0 = (cx - 1) * cs;
            float x1 = x0 + cs;
            float z0 = (cz - 1) * cs;
            float z1 = z0 + cs;
            float jitter = (hash(t.x0 / cs + cx, t.z0 / cs + cz) - 0.5F) * 0.12F;
            int base = rgb[i];
            int topCode = code(pack(base, 1.0F, jitter), wet[i] ? 180 : 255);
            bb = ensure(bb);
            float e = 0.04F * Math.max(1, cs / 8); // a sliver of overlap at the tile's edge: no crack between tiles
            float ex0 = cx == 1 ? x0 - e : x0;
            float ex1 = cx == 32 ? x1 + e : x1;
            float ez0 = cz == 1 ? z0 - e : z0;
            float ez1 = cz == 32 ? z1 + e : z1;
            quadN(bb, n, 0, 1, 0, ex0, y, ez0, ex1, y, ez0, ex1, y, ez1, ex0, y, ez1, topCode);
            for (int d = 0; d < 4; d++) {
               int ni = (cz + dirs[d][1]) * w + cx + dirs[d][0];
               float ny = topY[ni] - (wet[ni] ? 0.2F : sink);
               boolean edge = d == 0 && cx == 1 || d == 1 && cx == 32 || d == 2 && cz == 1 || d == 3 && cz == 32;
               if (!wet[i] || !wet[ni]) {
                  float low;
                  if (ny < y) {
                     low = edge ? ny - 14.0F : ny;
                  } else {
                     if (!edge) {
                        continue;
                     }

                     low = y - 14.0F;
                  }

                  int wall;
                  float drop = y - ny;
                  if (wet[i]) {
                     wall = base;
                  } else if (cliff[i] != 0) {
                     wall = drop <= 1.5F ? shade(base, 0.82F) : cliff[i];
                  } else if (biome[i] == 2 || biome[i] == 3 || biome[i] == 6 && terrain[i] <= 98) {
                     wall = drop <= 1.5F ? shade(base, 0.82F) : (drop <= 5.0F ? 7033395 : 8026746);
                  } else {
                     wall = shade(base, 0.82F);
                  }

                  float face = d < 2 ? 0.72F : 0.84F;
                  int wallCode = code(pack(wall, face, jitter), d == 0 ? 210 : (d == 1 ? 195 : (d == 2 ? 240 : 225)));
                  bb = ensure(bb);
                  switch (d) {
                     case 0:
                        quadN(bb, n, -1, 0, 0, x0, y, z1, x0, y, z0, x0, low, z0, x0, low, z1, wallCode);
                        break;
                     case 1:
                        quadN(bb, n, 1, 0, 0, x1, y, z0, x1, y, z1, x1, low, z1, x1, low, z0, wallCode);
                        break;
                     case 2:
                        quadN(bb, n, 0, 0, -1, x0, y, z0, x1, y, z0, x1, low, z0, x0, low, z0, wallCode);
                        break;
                     default:
                        quadN(bb, n, 0, 0, 1, x1, y, z1, x0, y, z1, x0, low, z1, x1, low, z1, wallCode);
                  }
               }
            }
         }
      }

      if (t.size <= 256) {
         Lod.Mb mb = new Lod.Mb();
         mb.bb = bb;
         mb.n = n[0];

         try {
            this.trees(t, mb, villages, sink);
            if (villages != null) {
               this.buildings(t, mb, villages);
            }
         } catch (Throwable ex) {
            ex.printStackTrace();
         }

         bb = mb.bb;
         n[0] = mb.n;
      }

      bb.flip();
      t.data = bb;
      t.vcount = n[0];
      t.state = 2;
      this.uploads.add(t);
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
      quadN(var16, var10, 0, 1, 0, var1, var5, var3, var4, var5, var3, var4, var5, var6, var1, var5, var6, var11);
      quadN(var16, var10, 0, 0, -1, var1, var5, var3, var4, var5, var3, var4, var2, var3, var1, var2, var3, var12);
      quadN(var16, var10, 0, 0, 1, var4, var5, var6, var1, var5, var6, var1, var2, var6, var4, var2, var6, var14);
      quadN(var16, var10, -1, 0, 0, var1, var5, var6, var1, var5, var3, var1, var2, var3, var1, var2, var6, var13);
      quadN(var16, var10, 1, 0, 0, var4, var5, var3, var4, var5, var6, var4, var2, var6, var4, var2, var3, var15);
      var0.n = var0.n + var10[0];
   }

   void trees(Lod.Tile var1, Lod.Mb var2, List<Village> var3, float var4) {
      int var5 = Math.floorDiv(var1.x0, 16);
      int var6 = Math.floorDiv(var1.x0 + var1.size - 1, 16);
      int var7 = Math.floorDiv(var1.z0, 16);
      int var8 = Math.floorDiv(var1.z0 + var1.size - 1, 16);
      boolean var9 = var1.size <= 64;
      int maxTries = var1.size <= 64 ? 16 : (var1.size == 128 ? 6 : 2);
      boolean drawTrunk = var1.size <= 128;

      for (int var10 = var5; var10 <= var6; var10++) {
         for (int var11 = var7; var11 <= var8; var11++) {
            Random var12 = new Random(this.gen.seed * 7919L ^ (var10 * 734287L + var11 * 912931L) * 31L + 17L);

            for (int var13 = 0; var13 < maxTries; var13++) {
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
                  int ground = var22.h; // y of the block the tree stands on
                  if (this.map != null) {
                     // Update 3.11: where the chunk was changed the tree is only drawn if its trunk is still there (burnt, felled, dug away: gone)
                     LodMap.Sum sum = this.map.get(var20 >> 4, var21 >> 4);
                     if (sum != null) {
                        int k = (var21 & 15) * 16 + (var20 & 15);
                        if (sum.trunk[k] < 3 || sum.depth[k] > 0) {
                           continue;
                        }

                        ground = sum.top[k] - 1;
                     }
                  }

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
                        float var49 = ground + 1 - Math.min(2.5F, 0.4F + var1.size / 32 * 0.35F);
                        float var50 = ground + 1;
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
                              if (drawTrunk) {
                                 box(var2, var47 - 0.5F, var49, var48 - 0.5F, var47 + 0.5F, var55, var48 + 0.5F, var31, 0.0F);
                              }

                              box(var2, var47 - 2.5F, var50 + 2.0F, var48 - 2.5F, var47 + 2.5F, var50 + var53 * 0.55F, var48 + 2.5F, shade(var30, 0.85F), var29);
                              if (drawTrunk) {
                                 box(var2, var47 - 1.5F, var50 + var53 * 0.55F, var48 - 1.5F, var47 + 1.5F, var55 + 1.0F, var48 + 1.5F, var30, var29);
                              }
                           }
                        } else if (var23 == 1) {
                           int var52 = 6 + var18 + var32.nextInt(2);
                           float var54 = var50 + var52 - 1.0F;
                           if (drawTrunk) {
                              box(var2, var47 - 0.5F, var49, var48 - 0.5F, var47 + 0.5F, var50 + var52 + 1.0F, var48 + 0.5F, var31, 0.0F);
                           }

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
                           if (drawTrunk) {
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
                           }

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
                              if (drawTrunk) {
                                 box(var2, var47 - 1.5F, var35 + var37, var48 - 1.5F, var47 + 1.5F, var35 + var37 + 2.0F, var48 + 1.5F, var30, var29);
                              }
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
               if (this.map != null && this.map.get((var9.bx0 + var9.bx1) / 2 >> 4, (var9.bz0 + var9.bz1) / 2 >> 4) != null) {
                  continue; // Update 3.11: the chunk in the middle of the house was changed - what is really there is part of the surface
               }

               if (var9.type == 3) {
                  // Update 3.12: a farm is a flat green field with a fence round it
                  box(var2, var9.bx0 - var4, var9.y0 - 4, var9.bz0 - var5, var9.bx1 + 1 - var4, var9.y0 + 1, var9.bz1 + 1 - var5, 4958260, 0.02F);
                  box(var2, var9.bx0 - var4, var9.y0 + 1, var9.bz0 - var5, var9.bx1 + 1 - var4, var9.y0 + 2, var9.bz1 + 1 - var5, 8283680, 0.03F);
               } else if (var9.type == 1) {
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

   /** A quad whose corners are given in any order; they are written counter-clockwise seen from the side the normal (nx, ny, nz) points to. */
   static void quadN(ByteBuffer b, int[] n, float nx, float ny, float nz, float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, int col) {
      float ax = x1 - x0;
      float ay = y1 - y0;
      float az = z1 - z0;
      float bx = x2 - x0;
      float by = y2 - y0;
      float bz = z2 - z0;
      float dot = (ay * bz - az * by) * nx + (az * bx - ax * bz) * ny + (ax * by - ay * bx) * nz;
      if (dot >= 0.0F) {
         quad(b, n, x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3, col);
      } else {
         quad(b, n, x0, y0, z0, x3, y3, z3, x2, y2, z2, x1, y1, z1, col);
      }
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
            var2.builtFrame = this.frame;
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
      // Update 3.8: the same brightness as the real terrain at full sky light (the lightmap's value for sky 15 is skyLightFactor, which
      // also contains the weather's dimming): no seam between near terrain and LOD at night or in the rain
      float var21 = Math.max(0.06F, Math.min(1.0F, var9));
      GL11.glTexEnvfv(8960, 8705, new float[]{var21, var21, var21, 1.0F});
      GL11.glDisable(3008);
      GL11.glDisable(3042);
      GL11.glEnableClientState(32884);
      GL11.glEnableClientState(32886);
      GL11.glEnable(32823);
      GL11.glPolygonOffset(1.0F, 8.0F);
      boolean cull = Renderer.cullFaces();
      if (cull) {
         GL11.glEnable(2884); // Update 3.11: every LOD quad is counter-clockwise seen from outside; the ground is see-through from below
      }

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
            Perf.lodVerts += var19.count;
            int lvl = Math.min(7, Integer.numberOfTrailingZeros(Math.max(32, var19.size) >> 5));
            Perf.lodSizeVerts[lvl] += var19.count;
            Perf.lodSizeTiles[lvl]++;
         }
      }

      if (cull) {
         GL11.glDisable(2884);
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
      double var8 = this.innerHalf + 24;
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
            if (this.rend.visible((float)(var1 + var3 / 2 - this.cx), (float)(100.0 - this.cy), (float)(var2 + var3 / 2 - this.cz), var3 * 0.72F + 28.0F)) {
               // (Update 3.11: a tile that may reach into the real chunks is always split down to 32, which is skipped where the chunks are meshed)
               boolean var6 = var3 > 32 && (var4 < var3 * 1.45 || var4 < this.innerHalf + 32.0);
               if (!var6) {
                  Lod.Tile var16 = this.tile(var3, var1, var2);
                  var16.lastUse = this.frame;
                  if (!ready(var16)) {
                     this.request(var16);
                  } else {
                     this.drawList.add(this.refresh(var16));
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
         if (this.frame - var2.lastUse > 900L && (var2.state == 3 || var2.state == 4 || var2.state == 0) && var2.replacement == null) {
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

         if (var2.replacement != null && var2.replacement.vbo != 0) {
            GL15.glDeleteBuffers(var2.replacement.vbo);
         }
      }

      this.tiles.clear();
      if (this.map != null && this.map.listener != null) {
         this.map.listener = null;
      }
   }

   // ------------------------------------------------------------------ Update 3.11: tiles follow the world
   /** The summary of chunk (cx, cz) changed: every tile that contains it is outdated and is rebuilt when it is next drawn (not more often than every few seconds). */
   void invalidate(int cx, int cz) {
      long e = this.epoch.incrementAndGet();
      for (int size = 32; size <= 1024; size *= 2) {
         int x0 = Math.floorDiv(cx * 16, size) * size;
         int z0 = Math.floorDiv(cz * 16, size) * size;
         Lod.Tile t = (Lod.Tile)this.tiles.get(key(size, x0, z0));
         if (t != null) {
            t.dirtyEpoch = e;
         }
      }
   }

   /** Frames a tile of this size is left alone after it was built before it is rebuilt for a change (the big ones hardly ever change visibly). */
   static long minAge(int size) {
      return (long)(Math.min(1800, 90L * (size / 32)) * AGE);
   }

   /** -Dbc.lodage=0.05 shortens the rest an outdated tile gets (screenshot scenes only run a couple of hundred frames). */
   static final double AGE = Double.parseDouble(System.getProperty("bc.lodage", "1"));

   /**
    * An outdated tile keeps being drawn while its replacement is built; when that is ready it takes the old one's place in one step, so
    * nothing flickers or is missing in between.
    */
   Lod.Tile refresh(Lod.Tile t) {
      Lod.Tile r = t.replacement;
      if (r != null) {
         if (ready(r)) {
            this.tiles.put(key(t.size, t.x0, t.z0), r);
            t.replacement = null;
            if (t.vbo != 0) {
               GL15.glDeleteBuffers(t.vbo);
               t.vbo = 0;
            }

            t.state = 5;
            r.lastUse = this.frame;
            return r;
         }

         if (r.state == 0) {
            this.request(r);
         }

         return t;
      }

      if (t.dirtyEpoch > t.builtEpoch && this.frame - t.builtFrame >= minAge(t.size) && this.inflight.get() < 6) {
         Lod.Tile n = new Lod.Tile(t.size, t.x0, t.z0);
         n.lastUse = this.frame;
         t.replacement = n;
         this.request(n);
      }

      return t;
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
      /** Update 3.11: the epoch (see Lod.invalidate) the tile was built at / last found outdated at, the frame it was uploaded, its replacement. */
      volatile long builtEpoch;
      volatile long dirtyEpoch;
      long builtFrame;
      volatile Lod.Tile replacement;

      Tile(int var1, int var2, int var3) {
         this.size = var1;
         this.x0 = var2;
         this.z0 = var3;
      }
   }
}
