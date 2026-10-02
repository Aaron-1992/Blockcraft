package bc;

import java.util.Random;

public final class Gen {
   public static final int SEA = 63;
   public static final int OCEAN = 0;
   public static final int BEACH = 1;
   public static final int PLAINS = 2;
   public static final int FOREST = 3;
   public static final int DESERT = 4;
   public static final int TUNDRA = 5;
   public static final int MOUNTAINS = 6;
   public static final String[] BIOME_NAMES = new String[]{"Ocean", "Beach", "Plains", "Forest", "Desert", "Ice Plains", "Extreme Hills"};
   final long seed;
   final Noise nCont;
   final Noise nMount;
   final Noise nRidge;
   final Noise nFlat;
   final Noise nDetail;
   final Noise nTemp;
   final Noise nHum;
   final Noise nCave1;
   final Noise nCave2;
   final Noise nCavern;
   final Noise nEntr;
   final Noise nSurf;
   public final Structs structs;
   static final int TRIES = 16;

   public Gen(long var1) {
      this.structs = new Structs(this, var1);
      this.seed = var1;
      Random var3 = new Random(var1);
      this.nCont = new Noise(var3.nextLong());
      this.nMount = new Noise(var3.nextLong());
      this.nRidge = new Noise(var3.nextLong());
      this.nFlat = new Noise(var3.nextLong());
      this.nDetail = new Noise(var3.nextLong());
      this.nTemp = new Noise(var3.nextLong());
      this.nHum = new Noise(var3.nextLong());
      this.nCave1 = new Noise(var3.nextLong());
      this.nCave2 = new Noise(var3.nextLong());
      this.nCavern = new Noise(var3.nextLong());
      this.nEntr = new Noise(var3.nextLong());
      this.nSurf = new Noise(var3.nextLong());
   }

   static double clamp(double var0, double var2, double var4) {
      return var0 < var2 ? var2 : (var0 > var4 ? var4 : var0);
   }

   static double smooth(double var0, double var2, double var4) {
      double var6 = clamp((var4 - var0) / (var2 - var0), 0.0, 1.0);
      return var6 * var6 * (3.0 - 2.0 * var6);
   }

   double cont(int var1, int var2) {
      return clamp(this.nCont.fbm(var1 / 380.0, var2 / 380.0, 4) * 1.7 + 0.14, -1.0, 1.0);
   }

   double mount(int var1, int var2, double var3) {
      return smooth(0.1, 0.6, this.nMount.fbm(var1 / 450.0 + 77.0, var2 / 450.0 - 33.0, 3)) * smooth(0.0, 0.35, var3);
   }

   int height(int var1, int var2, double var3, double var5) {
      double var7 = var3 >= 0.0 ? 64.0 + var3 * 22.0 : 64.0 + var3 * 32.0;
      double var9 = clamp(this.nFlat.fbm(var1 / 260.0, var2 / 260.0, 2) * 0.5 + 0.5, 0.0, 1.0);
      double var11 = 1.5 + 7.5 * var9;
      double var13 = this.nDetail.fbm(var1 / 64.0, var2 / 64.0, 5);
      double var15 = var7 + var13 * var11 * 1.5;
      if (var5 > 0.0) {
         double var17 = Math.max(0.0, 1.0 - Math.abs(this.nRidge.fbm(var1 / 140.0, var2 / 140.0, 4)));
         var15 += var5 * (var17 * var17 * 70.0 + 6.0);
      }

      return (int)clamp(Math.floor(var15), 2.0, 250.0);
   }

   double temp(int var1, int var2) {
      return clamp(this.nTemp.fbm(var1 / 800.0 + 500.0, var2 / 800.0, 3) * 0.5 + 0.5, 0.0, 1.0);
   }

   double hum(int var1, int var2) {
      return clamp(this.nHum.fbm(var1 / 800.0 - 300.0, var2 / 800.0 + 120.0, 3) * 0.5 + 0.5, 0.0, 1.0);
   }

   static int biomeOf(int var0, double var1, double var3, double var5, double var7) {
      if (var0 < 63) {
         return 0;
      } else if (var0 <= 65 && var1 < 0.1) {
         return 1;
      } else if (var3 > 0.45 && var0 > 85) {
         return 6;
      } else if (var5 > 0.6 && var7 < 0.48) {
         return 4;
      } else if (var5 < 0.3) {
         return 5;
      } else {
         return var7 > 0.55 ? 3 : 2;
      }
   }

   public Gen.Col column(int var1, int var2) {
      Gen.Col var3 = new Gen.Col();
      double var4 = this.cont(var1, var2);
      double var6 = this.mount(var1, var2, var4);
      var3.h = this.height(var1, var2, var4, var6);
      var3.t = this.temp(var1, var2);
      var3.hu = this.hum(var1, var2);
      var3.biome = biomeOf(var3.h, var4, var6, var3.t, var3.hu);
      return var3;
   }

   boolean entrance(int var1, int var2) {
      return this.nEntr.noise2(var1 * 0.012, var2 * 0.012) > 0.38;
   }

   static int lerpColor(int var0, int var1, double var2) {
      int var4 = (int)((var0 >> 16 & 0xFF) * (1.0 - var2) + (var1 >> 16 & 0xFF) * var2);
      int var5 = (int)((var0 >> 8 & 0xFF) * (1.0 - var2) + (var1 >> 8 & 0xFF) * var2);
      int var6 = (int)((var0 & 0xFF) * (1.0 - var2) + (var1 & 0xFF) * var2);
      return var4 << 16 | var5 << 8 | var6;
   }

   public static int grassColorFor(double var0, double var2) {
      int var4 = 8369038;
      int var5 = 9420117;
      int var6 = 12432980;
      int var7 = 5027898;
      int var8 = var0 < 0.5 ? lerpColor(var4, var5, var0 * 2.0) : lerpColor(var5, var6, (var0 - 0.5) * 2.0);
      double var9 = clamp((var2 - 0.5) * 1.6, 0.0, 0.7) * smooth(0.35, 0.7, var0);
      return lerpColor(var8, var7, var9);
   }

   public static int foliageColorFor(double var0, double var2) {
      int var4 = grassColorFor(var0, var2);
      int var5 = (int)((var4 >> 16 & 0xFF) * 0.9);
      int var6 = (int)((var4 >> 8 & 0xFF) * 0.94);
      int var7 = (int)((var4 & 0xFF) * 0.75);
      return var5 << 16 | var6 << 8 | var7;
   }

   int topBlock(int var1, int var2, double var3) {
      switch (var1) {
         case 0:
            return var3 > 0.25 ? 8 : 7;
         case 1:
         case 4:
            return 7;
         case 2:
         case 3:
         default:
            return 2;
         case 5:
            return 18;
         case 6:
            return var2 > 118 ? 18 : (var2 > 98 ? 1 : 2);
      }
   }

   boolean carve(int var1, int var2, int var3) {
      double var4 = this.nCave1.noise(var1 * 0.03, var2 * 0.045, var3 * 0.03);
      double var6 = this.nCave2.noise(var1 * 0.03 + 31.7, var2 * 0.045 + 11.3, var3 * 0.03 - 47.1);
      if (var4 * var4 + var6 * var6 < 0.0055) {
         return true;
      } else if (var2 < 50) {
         double var8 = this.nCavern.noise(var1 * 0.012, var2 * 0.025, var3 * 0.012);
         return var8 > 0.52;
      } else {
         return false;
      }
   }

   public Chunk generate(int var1, int var2) {
      Chunk var3 = new Chunk(var1, var2);
      byte[] var4 = var3.blocks;
      Random var5 = new Random(this.seed * 31L + var1 * 341873128712L + var2 * 132897987541L);
      int[] var6 = new int[256];
      int[] var7 = new int[256];

      for (int var8 = 0; var8 < 16; var8++) {
         for (int var9 = 0; var9 < 16; var9++) {
            int var10 = var1 * 16 + var9;
            int var11 = var2 * 16 + var8;
            double var12 = this.cont(var10, var11);
            double var14 = this.mount(var10, var11, var12);
            int var16 = this.height(var10, var11, var12, var14);
            double var17 = this.temp(var10, var11);
            double var19 = this.hum(var10, var11);
            int var21 = biomeOf(var16, var12, var14, var17, var19);
            int var22 = var8 * 16 + var9;
            var6[var22] = var16;
            var7[var22] = var21;
            var3.biome[var22] = (byte)var21;
            var3.grassColor[var22] = grassColorFor(var17, var19);
            var3.foliageColor[var22] = foliageColorFor(var17, var19);
            double var23 = this.nSurf.noise2(var10 * 0.08, var11 * 0.08);
            int var25 = this.topBlock(var21, var16, var23);
            int var26;
            int var27;
            switch (var21) {
               case 0:
                  var26 = var25;
                  var27 = 3;
                  break;
               case 1:
               case 4:
                  var26 = 7;
                  var27 = 3;
                  break;
               case 2:
               case 3:
               case 5:
               default:
                  var26 = 3;
                  var27 = 3 + (var23 > 0.0 ? 1 : 0);
                  break;
               case 6:
                  var26 = var25 == 2 ? 3 : 1;
                  var27 = var25 == 2 ? 3 : 0;
            }

            boolean var28 = var21 == 1 || var21 == 4;

            for (int var29 = 0; var29 <= var16; var29++) {
               int var30 = 1;
               int var31 = var16 - var29;
               if (var29 == 0) {
                  var30 = 6;
               } else if (var29 <= 4 && var5.nextInt(5) < 5 - var29) {
                  var30 = 6;
               } else if (var31 == 0) {
                  var30 = var25;
               } else if (var31 <= var27) {
                  var30 = var26;
               } else if (var28 && var31 <= var27 + 3) {
                  var30 = 17;
               }

               var4[Chunk.idx(var9, var29, var8)] = (byte)var30;
            }

            for (int var47 = var16 + 1; var47 < 63; var47++) {
               var4[Chunk.idx(var9, var47, var8)] = 50;
            }

            int var48 = var16 - 3;
            if (var16 >= 64 && this.entrance(var10, var11)) {
               var48 = var16;
            }

            if (var16 < 64) {
               var48 = Math.min(var48, var16 - 6);
            }

            for (int var49 = 6; var49 <= var48; var49++) {
               int var50 = var4[Chunk.idx(var9, var49, var8)] & 255;
               if (var50 != 6 && !Blocks.isFluid(var50) && this.carve(var10, var49, var11)) {
                  var4[Chunk.idx(var9, var49, var8)] = (byte)(var49 <= 10 ? 58 : 0);
               }
            }
         }
      }

      Random var32 = new Random(this.seed ^ (var1 * 7919L + var2 * 104729L) * 1000003L);
      this.vein(var3, var32, 3, 8, 20, 0, 128);
      this.vein(var3, var32, 8, 6, 20, 0, 128);
      this.vein(var3, var32, 19, 20, 14, 0, 128);
      this.vein(var3, var32, 20, 20, 8, 0, 64);
      this.vein(var3, var32, 21, 2, 8, 0, 32);
      this.vein(var3, var32, 23, 8, 7, 0, 16);
      this.vein(var3, var32, 22, 2, 7, 0, 16);
      this.vein(var3, var32, 24, 1, 6, 0, 30);
      Random var33 = new Random(this.seed ^ var1 * 2654435761L + var2 * 40503L);

      for (int var34 = 0; var34 < 16; var34++) {
         for (int var35 = 0; var35 < 16; var35++) {
            int var36 = var6[var34 * 16 + var35];
            int var13 = var7[var34 * 16 + var35];
            if (var36 >= 63 && var36 <= 240) {
               int var37 = var4[Chunk.idx(var35, var36, var34)] & 255;
               float var15 = var33.nextFloat();
               if (var4[Chunk.idx(var35, var36 + 1, var34)] == 0) {
                  if (var37 != 2 || var13 != 2 && var13 != 3) {
                     if (var37 == 2 && var13 == 6 && var15 < 0.04) {
                        var4[Chunk.idx(var35, var36 + 1, var34)] = 29;
                     } else if (var37 == 7 && var13 == 4 && var15 < 0.006 && !this.entrance(var1 * 16 + var35, var2 * 16 + var34)) {
                        boolean var39 = true;

                        for (int[] var20 : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                           int var45 = var35 + var20[0];
                           int var46 = var34 + var20[1];
                           if (var45 >= 0 && var45 <= 15 && var46 >= 0 && var46 <= 15 && var4[Chunk.idx(var45, var36 + 1, var46)] != 0) {
                              var39 = false;
                           }
                        }

                        if (var39) {
                           int var42 = 1 + var33.nextInt(3);

                           for (int var43 = 1; var43 <= var42; var43++) {
                              var4[Chunk.idx(var35, var36 + var43, var34)] = 28;
                           }
                        }
                     }
                  } else {
                     float var38 = var13 == 2 ? 0.2F : 0.1F;
                     float var40 = var13 == 2 ? 0.014F : 0.006F;
                     if (var15 < var40) {
                        var4[Chunk.idx(var35, var36 + 1, var34)] = (byte)(var33.nextBoolean() ? 30 : 31);
                     } else if (var15 < var40 + var38) {
                        var4[Chunk.idx(var35, var36 + 1, var34)] = 29;
                     }
                  }
               }
            }
         }
      }

      this.plantTrees(var3);
      this.structs.apply(var3);
      this.plantShoreAndPatches(var3, var6, var7);
      var3.computeHeightmap();
      return var3;
   }

   /**
    * Update 3.7: sugar cane along the shore (sand / grass / dirt at sea level with water next to it) and the odd
    * pumpkin patch on grassland. Uses its own random stream, so everything generated before is untouched.
    */
   /** Test hook: false (-Dbc.noextras=true) = generate exactly like Update 3.6 (no sugar cane, no pumpkin patches). */
   static boolean extras = !Boolean.getBoolean("bc.noextras");

   void plantShoreAndPatches(Chunk c, int[] heights, int[] biomes) {
      if (!extras) {
         return;
      }

      Random r = new Random(this.seed * -7046029254386353131L ^ (c.cx * 73856093L + c.cz * 19349663L) * 1000003L + 37L);
      int[][] around = new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

      for (int z = 0; z < 16; z++) {
         for (int x = 0; x < 16; x++) {
            if (heights[z * 16 + x] != 62) {
               continue;
            }

            int ground = c.get(x, 62, z);
            if ((ground != 7 && ground != 3 && ground != 2) || c.get(x, 63, z) != 0) {
               continue;
            }

            boolean water = false;
            for (int[] d : around) {
               int nx = x + d[0];
               int nz = z + d[1];
               if (nx >= 0 && nx <= 15 && nz >= 0 && nz <= 15 && Blocks.isWater(c.get(nx, 62, nz))) {
                  water = true;
               }
            }

            if (water && r.nextInt(100) < 30) {
               int n = 1 + r.nextInt(3);
               for (int i = 0; i < n; i++) {
                  c.set(x, 63 + i, z, Blocks.SUGAR_CANE);
               }
            }
         }
      }

      if (r.nextInt(9) == 0) {
         int px = 3 + r.nextInt(10);
         int pz = 3 + r.nextInt(10);
         int placed = 0;
         for (int i = 0; i < 24 && placed < 8; i++) {
            int x = Math.max(0, Math.min(15, px + r.nextInt(7) - 3));
            int z = Math.max(0, Math.min(15, pz + r.nextInt(7) - 3));
            int h = heights[z * 16 + x];
            int biome = biomes[z * 16 + x];
            if ((biome == 2 || biome == 3) && h >= 64 && h <= 235 && c.get(x, h, z) == 2) {
               int above = c.get(x, h + 1, z);
               if (above == 0 || above == 29) {
                  c.set(x, h + 1, z, Blocks.PUMPKIN);
                  placed++;
               }
            }
         }
      }
   }

   void vein(Chunk var1, Random var2, int var3, int var4, int var5, int var6, int var7) {
      for (int var8 = 0; var8 < var4; var8++) {
         int var9 = var2.nextInt(16);
         int var10 = var2.nextInt(16);
         int var11 = var6 + var2.nextInt(var7 - var6);

         for (int var12 = 0; var12 < var5; var12++) {
            if (var9 >= 0 && var9 < 16 && var10 >= 0 && var10 < 16 && var11 > 0 && var11 < 255) {
               int var13 = var1.blocks[Chunk.idx(var9, var11, var10)] & 255;
               if (var13 == 1) {
                  var1.blocks[Chunk.idx(var9, var11, var10)] = (byte)var3;
               }
            }

            var9 += var2.nextInt(3) - 1;
            var11 += var2.nextInt(3) - 1;
            var10 += var2.nextInt(3) - 1;
         }
      }
   }

   void plantTrees(Chunk var1) {
      for (int var2 = -1; var2 <= 1; var2++) {
         for (int var3 = -1; var3 <= 1; var3++) {
            int var4 = var1.cx + var2;
            int var5 = var1.cz + var3;
            Random var6 = new Random(this.seed * 7919L ^ (var4 * 734287L + var5 * 912931L) * 31L + 17L);

            for (int var7 = 0; var7 < 16; var7++) {
               int var8 = var6.nextInt(16);
               int var9 = var6.nextInt(16);
               float var10 = var6.nextFloat();
               int var11 = var6.nextInt(1000);
               int var12 = var6.nextInt(3);
               int var13 = var6.nextInt();
               int var14 = var4 * 16 + var8;
               int var15 = var5 * 16 + var9;
               if (var14 >= var1.cx * 16 - 8 && var14 <= var1.cx * 16 + 23 && var15 >= var1.cz * 16 - 8 && var15 <= var1.cz * 16 + 23) {
                  Gen.Col var16 = this.column(var14, var15);
                  if (var16.h >= 64 && var16.h <= 235 && !this.entrance(var14, var15)) {
                     int var17;
                     switch (var16.biome) {
                        case 2:
                           if (var10 > 0.035F) {
                              continue;
                           }

                           var17 = 0;
                           break;
                        case 3:
                           if (!(var10 > 0.85F)) {
                              var17 = (byte)(var11 < 250 ? 1 : 0);
                              break;
                           }
                        case 4:
                        default:
                           continue;
                        case 5:
                           if (var10 > 0.22F) {
                              continue;
                           }

                           var17 = 2;
                           break;
                        case 6:
                           if (var16.h > 98 || var10 > 0.12F) {
                              continue;
                           }

                           var17 = 2;
                     }

                     this.buildTree(var1, var14, var16.h + 1, var15, var17, var12, new Random(var13));
                  }
               }
            }
         }
      }
   }

   static void put(Chunk var0, int var1, int var2, int var3, int var4, boolean var5) {
      int var6 = var1 - var0.cx * 16;
      int var7 = var3 - var0.cz * 16;
      if (var6 >= 0 && var6 <= 15 && var7 >= 0 && var7 <= 15 && var2 >= 0 && var2 <= 255) {
         int var8 = Chunk.idx(var6, var2, var7);
         int var9 = var0.blocks[var8] & 255;
         if (!var5 || var9 == 0 || var9 == 29 || Blocks.isLeaves(var9)) {
            if (!Blocks.isFluid(var9)) {
               var0.blocks[var8] = (byte)var4;
            }
         }
      }
   }

   static void blob(Chunk var0, int var1, int var2, int var3, double var4, double var6, double var8, int var10, Random var11) {
      int var12 = (int)Math.ceil(var4);
      int var13 = (int)Math.ceil(var6);
      int var14 = (int)Math.ceil(var8);

      for (int var15 = -var13; var15 <= var13; var15++) {
         for (int var16 = -var12; var16 <= var12; var16++) {
            for (int var17 = -var14; var17 <= var14; var17++) {
               double var18 = var16 * var16 / (var4 * var4) + var15 * var15 / (var6 * var6) + var17 * var17 / (var8 * var8);
               if (!(var18 > 1.0) && (!(var18 > 0.62) || !(var11.nextFloat() < 0.38F))) {
                  put(var0, var1 + var16, var2 + var15, var3 + var17, var10, true);
               }
            }
         }
      }
   }

   void buildTree(Chunk var1, int var2, int var3, int var4, int var5, int var6, Random var7) {
      int var8 = var5 == 0 ? 9 : (var5 == 1 ? 10 : 11);
      int var9 = var5 == 0 ? 12 : (var5 == 1 ? 13 : 14);
      if (var5 == 2) {
         int var33 = 7 + var6 * 2 + var7.nextInt(3);
         int var35 = var3 + var33;
         int var37 = var33 >= 10 ? 4 : 3;

         for (int var38 = var3 + 2; var38 <= var35 + 1; var38++) {
            int var14 = var35 + 1 - var38;
            int var40 = Math.min(var37, (var14 + 1) / 2);
            if (var14 % 2 == 0 && var14 > 1) {
               var40--;
            }

            for (int var16 = -var40; var16 <= var40; var16++) {
               for (int var41 = -var40; var41 <= var40; var41++) {
                  boolean var44 = Math.abs(var16) == var40 && Math.abs(var41) == var40 && var40 > 1;
                  if (!var44 && (var40 < 3 || Math.abs(var16) + Math.abs(var41) < 2 * var40 - 1 || var7.nextInt(3) != 0)) {
                     put(var1, var2 + var16, var38, var4 + var41, var9, true);
                  }
               }
            }
         }

         for (int var39 = 0; var39 <= var33; var39++) {
            put(var1, var2, var3 + var39, var4, var8, false);
         }
      } else if (var5 == 1) {
         int var32 = 6 + var6 + var7.nextInt(2);
         int var34 = var3 + var32 - 1;
         blob(var1, var2, var34, var4, 2.3, 3.0, 2.3, var9, var7);
         blob(var1, var2, var34 + 2, var4, 1.4, 1.6, 1.4, var9, var7);

         for (int var36 = 0; var36 < var32 + 1; var36++) {
            put(var1, var2, var3 + var36, var4, var8, false);
         }
      } else {
         boolean var10 = var7.nextInt(7) == 0;
         int var11 = 5 + var6 + var7.nextInt(2) + (var10 ? 3 : 0);
         int var12 = var3 + var11 - 1;
         double var13 = var10 ? 4.2 : 3.1;
         double var15 = var10 ? 3.2 : 2.5;
         blob(var1, var2, var12, var4, var13, var15, var13, var9, var7);
         int var17 = 2 + var7.nextInt(3);

         for (int var18 = 0; var18 < var17; var18++) {
            double var19 = var7.nextDouble() * Math.PI * 2.0;
            double var21 = var10 ? 3.6 : 2.7;
            int var23 = var2 + (int)Math.round(Math.cos(var19) * var21);
            int var24 = var4 + (int)Math.round(Math.sin(var19) * var21);
            int var25 = var12 - var7.nextInt(3);
            blob(var1, var23, var25, var24, var10 ? 2.6 : 2.1, var10 ? 2.1 : 1.8, var10 ? 2.6 : 2.1, var9, var7);
            int var26 = var2;
            int var27 = var4;
            int var28 = var25 - 1;
            int var29 = Integer.signum(var23 - var2);
            int var30 = Integer.signum(var24 - var4);

            for (int var31 = 0; var31 < 3; var31++) {
               if (Math.abs(var23 - var26) >= Math.abs(var24 - var27)) {
                  var26 += var29;
               } else {
                  var27 += var30;
               }

               if (var26 == var23 && var27 == var24) {
                  break;
               }

               put(var1, var26, var28, var27, var8, false);
            }
         }

         blob(var1, var2, var12 + (var10 ? 3 : 2), var4, var10 ? 2.4 : 1.7, 1.4, var10 ? 2.4 : 1.7, var9, var7);

         for (int var42 = 0; var42 <= var11; var42++) {
            put(var1, var2, var3 + var42, var4, var8, false);
         }

         if (var10) {
            for (int var43 = 0; var43 < 2; var43++) {
               put(var1, var2 + 1, var3 + var43, var4, var8, false);
               put(var1, var2, var3 + var43, var4 + 1, var8, false);
            }
         }
      }
   }

   public int[] findSpawn() {
      for (int var1 = 0; var1 < 600; var1 += 8) {
         for (int var2 = 0; var2 < (var1 == 0 ? 1 : 16); var2++) {
            double var3 = var2 * Math.PI * 2.0 / 16.0;
            int var5 = (int)(Math.cos(var3) * var1);
            int var6 = (int)(Math.sin(var3) * var1);
            Gen.Col var7 = this.column(var5, var6);
            if (var7.h >= 65 && (var7.biome == 2 || var7.biome == 3 || var7.biome == 4 || var7.biome == 5) && !this.entrance(var5, var6)) {
               return new int[]{var5, var7.h + 1, var6};
            }
         }
      }

      return new int[]{0, 90, 0};
   }

   public static final class Col {
      public int h;
      public int biome;
      public double t;
      public double hu;
   }
}
