package bc;

import java.util.Random;

/**
 * Update 3.13 art: the tiles of the new world generator's blocks (stones, earth, woods, plants, ice, mushrooms). Painted with code from fixed
 * random seeds, like all tiles of the game. Tiles 520.. (the atlas has room up to 1023).
 */
final class TexArt313 {
   private TexArt313() {
   }

   static final int GRANITE = 520;
   static final int DIORITE = 521;
   static final int ANDESITE = 522;
   static final int RED_SAND = 523;
   static final int PODZOL_TOP = 524;
   static final int PODZOL_SIDE = 525;
   static final int MYCELIUM_TOP = 526;
   static final int MYCELIUM_SIDE = 527;
   static final int COARSE_DIRT = 528;
   static final int ICE = 529;
   static final int PACKED_ICE = 530;
   static final int RS_TOP = 531;
   static final int RS_SIDE = 532;
   /** Planks of spruce, birch, jungle, acacia, dark oak: 534..538. */
   static final int PLANKS0 = 534;
   /** Bark of jungle, acacia, dark oak: 540..542, log ends 543..545, bark turned 546..548. */
   static final int BARK0 = 540;
   static final int RING0 = 543;
   static final int TURNED0 = 546;
   /** Leaves of jungle, acacia, dark oak: 550..552. */
   static final int LEAVES0 = 550;
   static final int FERN = 553;
   static final int DEAD_BUSH = 554;
   /** Ten flowers: 556..565. */
   static final int FLOWER0 = 556;
   static final int MUSH_BROWN = 566;
   static final int MUSH_RED = 567;
   /** Six tall plants, lower and upper picture each: 570..581. */
   static final int DOUBLE0 = 570;
   static final int LILY = 582;
   static final int VINE = 583;
   static final int MUSH_BLOCK_RED = 584;
   static final int MUSH_BLOCK_BROWN = 585;
   static final int MUSH_STEM = 586;
   static final int SNOW_SIDE = 587;
   static final int ITEM0 = 590;

   static int rgb(int r, int g, int b) {
      return Tex.rgb(r, g, b);
   }

   static int lum(int c) {
      return ((c >> 16 & 255) * 77 + (c >> 8 & 255) * 151 + (c & 255) * 28) >> 8;
   }

   /** The picture with its brightness pattern but the colour of (r, g, b). */
   static int[] recolor(int[] src, int r, int g, int b) {
      long sum = 0;
      int n = 0;
      for (int p : src) {
         if (p >>> 24 != 0) {
            sum += lum(p);
            n++;
         }
      }

      double avg = n == 0 ? 128.0 : (double)sum / n;
      int[] out = new int[256];
      for (int i = 0; i < 256; i++) {
         int p = src[i];
         if (p >>> 24 == 0) {
            continue;
         }

         double f = lum(p) / avg;
         out[i] = (p & 0xFF000000) | rgb((int)Math.min(255, r * f), (int)Math.min(255, g * f), (int)Math.min(255, b * f)) & 0xFFFFFF;
      }

      return out;
   }

   static int[] transpose(int[] src) {
      int[] dst = new int[256];
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            dst[y * 16 + x] = src[x * 16 + y];
         }
      }

      return dst;
   }

   static int[] speckled(long seed, int r, int g, int b, int var, int[][] specks, int[] counts) {
      Random rnd = new Random(seed);
      int[] p = new int[256];
      for (int i = 0; i < 256; i++) {
         int v = rnd.nextInt(var * 2 + 1) - var;
         p[i] = rgb(r + v, g + v, b + v);
      }

      for (int k = 0; k < specks.length; k++) {
         for (int n = 0; n < counts[k]; n++) {
            int x = rnd.nextInt(16);
            int y = rnd.nextInt(16);
            int w = 1 + rnd.nextInt(2);
            int h = 1 + rnd.nextInt(2);
            for (int dy = 0; dy < h; dy++) {
               for (int dx = 0; dx < w; dx++) {
                  int v = rnd.nextInt(9) - 4;
                  p[(y + dy & 15) * 16 + (x + dx & 15)] = rgb(specks[k][0] + v, specks[k][1] + v, specks[k][2] + v);
               }
            }
         }
      }

      return p;
   }

   // ------------------------------------------------------------------------------------------------ stones and earth
   static int[] granite() {
      return speckled(1301L, 154, 106, 89, 8, new int[][]{{126, 82, 68}, {182, 132, 114}, {110, 70, 60}}, new int[]{20, 16, 7});
   }

   static int[] diorite() {
      return speckled(1302L, 188, 188, 186, 7, new int[][]{{116, 116, 116}, {152, 152, 152}, {222, 222, 220}}, new int[]{24, 16, 10});
   }

   static int[] andesite() {
      return speckled(1303L, 137, 137, 137, 6, new int[][]{{154, 154, 154}, {118, 118, 118}, {100, 100, 100}}, new int[]{16, 16, 5});
   }

   static int[] redSand() {
      return speckled(1304L, 192, 104, 34, 8, new int[][]{{208, 122, 48}, {168, 86, 26}}, new int[]{14, 12});
   }

   static int[] podzolTop() {
      return speckled(1305L, 96, 64, 28, 9, new int[][]{{70, 45, 20}, {128, 92, 42}, {110, 100, 50}}, new int[]{22, 14, 5});
   }

   static int[] myceliumTop() {
      return speckled(1306L, 110, 96, 108, 7, new int[][]{{142, 128, 140}, {88, 76, 86}, {160, 140, 170}}, new int[]{18, 12, 5});
   }

   static int[] coarseDirt() {
      return speckled(1307L, 122, 88, 60, 9, new int[][]{{150, 136, 124}, {96, 70, 50}, {108, 108, 104}}, new int[]{12, 10, 8});
   }

   /** Dirt with a ragged cover of another colour at the top (podzol, mycelium). */
   static int[] coverSide(int[] top, long seed) {
      int[] dirt = Tex.dirt();
      Random r = new Random(seed);
      for (int x = 0; x < 16; x++) {
         int depth = 3 + r.nextInt(3) - 1;
         for (int y = 0; y < depth; y++) {
            dirt[y * 16 + x] = top[(y + x * 3 & 15) * 16 + x];
         }

         if (r.nextInt(3) == 0) {
            dirt[depth * 16 + x] = top[(depth + 7) * 16 % 256 + x];
         }
      }

      return dirt;
   }

   static int[] ice() {
      Random r = new Random(1308L);
      int[] p = new int[256];
      for (int i = 0; i < 256; i++) {
         int v = r.nextInt(11) - 5;
         p[i] = rgb(146 + v, 188 + v, 252 + v / 2);
      }

      for (int k = 0; k < 5; k++) {
         int x = r.nextInt(12);
         int y = r.nextInt(12);
         int len = 3 + r.nextInt(5);
         for (int i = 0; i < len; i++) {
            int xx = x + i;
            int yy = y + i;
            if (xx < 16 && yy < 16) {
               p[yy * 16 + xx] = rgb(214, 234, 255);
            }
         }
      }

      for (int k = 0; k < 6; k++) {
         p[r.nextInt(256)] = rgb(112, 160, 236);
      }

      return p;
   }

   static int[] packedIce() {
      Random r = new Random(1309L);
      int[] p = new int[256];
      for (int i = 0; i < 256; i++) {
         int v = r.nextInt(13) - 6;
         p[i] = rgb(124 + v, 162 + v, 232 + v / 2);
      }

      for (int k = 0; k < 7; k++) {
         int x = r.nextInt(14);
         int y = r.nextInt(14);
         int len = 2 + r.nextInt(6);
         boolean horizontal = r.nextBoolean();
         for (int i = 0; i < len; i++) {
            int xx = horizontal ? x + i : x;
            int yy = horizontal ? y : y + i;
            if (xx < 16 && yy < 16) {
               p[yy * 16 + xx] = k % 2 == 0 ? rgb(176, 206, 248) : rgb(96, 132, 206);
            }
         }
      }

      return p;
   }

   // ------------------------------------------------------------------------------------------------ wood
   static final int[][] PLANK_COLORS = new int[][]{{112, 82, 46}, {196, 178, 120}, {160, 114, 78}, {172, 92, 52}, {64, 42, 20}};
   /** Bark (r, g, b), ring colours of the log end (outer, ring, ring). */
   static final int[][] BARK = new int[][]{{86, 66, 28}, {106, 98, 90}, {56, 42, 24}};
   static final int[][][] RINGS = new int[][][]{{{86, 66, 28}, {178, 130, 82}, {150, 108, 66}}, {{106, 98, 90}, {176, 98, 58}, {150, 82, 46}}, {{56, 42, 24}, {92, 60, 32}, {72, 48, 26}}};

   static int[] bark(int sp) {
      int[] p = Tex.tLogSide(1400L + sp, BARK[sp][0], BARK[sp][1], BARK[sp][2]);
      Random r = new Random(1410L + sp);
      if (sp == 0) {
         for (int k = 0; k < 14; k++) {
            int i = r.nextInt(256);
            p[i] = rgb(54 + r.nextInt(16), 78 + r.nextInt(14), 30);
         }
      } else if (sp == 1) {
         for (int k = 0; k < 10; k++) {
            int i = r.nextInt(256);
            p[i] = rgb(150 + r.nextInt(20), 92 + r.nextInt(12), 52);
         }
      }

      return p;
   }

   static int[] ring(int sp) {
      return Tex.tLogTop(1420L + sp, RINGS[sp][0], RINGS[sp][1], RINGS[sp][2]);
   }

   static int[] leaves(int sp) {
      Random r = new Random(1430L + sp);
      int[] p = new int[256];
      int base = sp == 0 ? 150 : (sp == 1 ? 176 : 118);
      float hole = sp == 0 ? 0.13F : (sp == 1 ? 0.27F : 0.07F);
      for (int i = 0; i < 256; i++) {
         int v = base + r.nextInt(76) - 36;
         p[i] = r.nextFloat() < hole ? 0 : rgb(v, v, v);
      }

      return p;
   }

   // ------------------------------------------------------------------------------------------------ plants
   static int g(int v) {
      return rgb(v, v, v);
   }

   /** The strokes of a grassy plant on a canvas of h rows, as the grey picture that is tinted with the biome's grass colour. */
   static int[] fern() {
      Random r = new Random(1501L);
      TexExtra.Cv c = new TexExtra.Cv();
      int[][] tips = new int[][]{{2, 5}, {5, 2}, {8, 1}, {11, 2}, {14, 5}, {4, 7}, {12, 7}};
      for (int[] tip : tips) {
         int x0 = 8;
         int y0 = 15;
         int steps = 12;
         for (int s = 0; s <= steps; s++) {
            double f = (double)s / steps;
            int x = (int)Math.round(x0 + (tip[0] - x0) * f);
            int y = (int)Math.round(y0 + (tip[1] - y0) * (f * (2.0 - f)));
            c.set(x, y, g(120 + r.nextInt(40) + s * 3));
            if (s > 3 && s % 2 == 0 && s < steps) {
               int d = tip[0] < x0 ? -1 : 1;
               c.set(x, y - 1, g(150 + r.nextInt(50)));
               c.set(x + d, y, g(135 + r.nextInt(40)));
            }
         }
      }

      c.set(8, 14, g(110));
      c.set(7, 15, g(120));
      c.set(9, 15, g(120));
      return c.p;
   }

   static int[] deadBush() {
      Random r = new Random(1502L);
      TexExtra.Cv c = new TexExtra.Cv();
      int[][] tips = new int[][]{{2, 6}, {4, 3}, {8, 2}, {12, 4}, {14, 7}, {6, 5}};
      for (int[] tip : tips) {
         int x0 = 8;
         int y0 = 15;
         int steps = 10;
         for (int s = 0; s <= steps; s++) {
            double f = (double)s / steps;
            int x = (int)Math.round(x0 + (tip[0] - x0) * f);
            int y = (int)Math.round(y0 + (tip[1] - y0) * f);
            int v = r.nextInt(18);
            c.set(x, y, rgb(112 + v, 82 + v / 2, 36 + v / 3));
            if (s == 6 && tip[0] != 8) {
               int d = tip[0] < x0 ? -1 : 1;
               c.set(x + d, y - 1, rgb(100, 74, 32));
               c.set(x + d * 2, y - 2, rgb(96, 70, 30));
            }
         }
      }

      return c.p;
   }

   private static void stem(TexExtra.Cv c, int x, int y0, int y1) {
      for (int y = y0; y <= y1; y++) {
         c.set(x, y, rgb(48, 140, 38));
      }
   }

   private static void leafPair(TexExtra.Cv c, int x, int y) {
      c.set(x - 1, y, rgb(60, 160, 48));
      c.set(x + 1, y - 1, rgb(60, 160, 48));
   }

   /** The ten flowers: 0 blue orchid, 1 allium, 2 azure bluet, 3 red tulip, 4 orange tulip, 5 white tulip, 6 pink tulip, 7 oxeye daisy, 8 cornflower, 9 lily of the valley. */
   static int[] flower(int k) {
      TexExtra.Cv c = new TexExtra.Cv();
      switch (k) {
         case 0: {
            stem(c, 7, 8, 15);
            leafPair(c, 7, 12);
            int petal = rgb(40, 172, 232);
            int dark = rgb(24, 128, 196);
            c.rect(5, 5, 5, 3, petal);
            c.rect(6, 4, 3, 5, petal);
            c.set(5, 5, dark);
            c.set(9, 7, dark);
            c.set(7, 6, rgb(240, 250, 255));
            c.set(7, 5, rgb(180, 230, 250));
            break;
         }

         case 1: {
            stem(c, 7, 6, 15);
            leafPair(c, 7, 13);
            leafPair(c, 7, 10);
            int ball = rgb(184, 112, 232);
            c.ell(7.5, 4.0, 3.5, 3.5, ball);
            for (int[] d : new int[][]{{5, 3}, {7, 2}, {9, 4}, {6, 5}, {8, 6}, {7, 4}}) {
               c.set(d[0], d[1], rgb(224, 170, 255));
            }

            c.set(5, 6, rgb(150, 84, 200));
            break;
         }

         case 2: {
            stem(c, 5, 10, 15);
            stem(c, 10, 11, 15);
            for (int[] f : new int[][]{{5, 9}, {10, 10}}) {
               c.set(f[0], f[1] - 1, rgb(238, 238, 238));
               c.set(f[0] - 1, f[1], rgb(238, 238, 238));
               c.set(f[0] + 1, f[1], rgb(238, 238, 238));
               c.set(f[0], f[1] + 1, rgb(214, 220, 232));
               c.set(f[0], f[1], rgb(250, 214, 60));
            }

            break;
         }

         case 3:
         case 4:
         case 5:
         case 6: {
            int[] col = k == 3 ? new int[]{214, 36, 28} : (k == 4 ? new int[]{238, 124, 22} : (k == 5 ? new int[]{240, 240, 234} : new int[]{240, 150, 188}));
            stem(c, 7, 8, 15);
            leafPair(c, 7, 13);
            leafPair(c, 7, 11);
            int main = rgb(col[0], col[1], col[2]);
            int dark = rgb(col[0] * 3 / 4, col[1] * 3 / 4, col[2] * 3 / 4);
            c.rect(5, 4, 5, 4, main);
            c.set(5, 4, 0);
            c.set(9, 4, 0);
            c.rect(6, 8, 3, 1, dark);
            c.set(7, 4, dark);
            c.set(6, 5, dark);
            c.set(8, 5, dark);
            c.set(5, 7, dark);
            c.set(9, 7, dark);
            break;
         }

         case 7: {
            stem(c, 7, 7, 15);
            leafPair(c, 7, 12);
            for (int dy = -3; dy <= 3; dy++) {
               for (int dx = -3; dx <= 3; dx++) {
                  int d = Math.abs(dx) + Math.abs(dy);
                  if (d <= 4 && !(Math.abs(dx) == 3 && Math.abs(dy) == 3) && d >= 2) {
                     c.set(7 + dx, 4 + dy, rgb(246, 246, 242));
                  }
               }
            }

            c.rect(6, 3, 3, 3, rgb(250, 200, 30));
            c.set(7, 4, rgb(222, 160, 20));
            break;
         }

         case 8: {
            stem(c, 7, 7, 15);
            leafPair(c, 7, 12);
            int blue = rgb(62, 92, 222);
            int light = rgb(116, 144, 250);
            c.ell(7.5, 5.0, 3.0, 3.0, blue);
            for (int[] d : new int[][]{{6, 3}, {8, 4}, {7, 6}}) {
               c.set(d[0], d[1], light);
            }

            c.set(7, 5, rgb(30, 40, 150));
            break;
         }

         default: {
            for (int y = 6; y <= 15; y++) {
               c.set(y < 8 ? 9 : (y < 11 ? 8 : 7), y, rgb(48, 140, 38));
            }

            leafPair(c, 7, 14);
            for (int i = 0; i < 4; i++) {
               int x = 9 - i / 2 * 0 + (i % 2 == 0 ? 1 : -1);
               int y = 7 + i * 2;
               c.rect(x - 1, y, 2, 2, rgb(248, 248, 244));
               c.set(x - 1, y + 1, rgb(220, 224, 218));
            }

            break;
         }
      }

      return c.p;
   }

   static int[] mushroom(boolean red) {
      TexExtra.Cv c = new TexExtra.Cv();
      int stemC = rgb(226, 216, 198);
      c.rect(7, 9, 2, 6, stemC);
      c.set(8, 10, rgb(196, 184, 164));
      c.set(8, 12, rgb(196, 184, 164));
      if (red) {
         int cap = rgb(214, 40, 36);
         c.rect(4, 7, 8, 3, cap);
         c.rect(5, 5, 6, 2, cap);
         c.rect(6, 4, 4, 1, cap);
         c.set(6, 6, rgb(244, 244, 238));
         c.set(9, 5, rgb(244, 244, 238));
         c.set(5, 8, rgb(244, 244, 238));
         c.set(10, 8, rgb(244, 244, 238));
         c.set(4, 9, rgb(160, 24, 24));
         c.set(11, 9, rgb(160, 24, 24));
      } else {
         int cap = rgb(150, 110, 82);
         c.rect(4, 9, 8, 2, cap);
         c.rect(5, 8, 6, 1, cap);
         c.rect(6, 7, 4, 1, cap);
         c.set(7, 8, rgb(176, 138, 108));
         c.set(4, 10, rgb(120, 86, 62));
         c.set(11, 10, rgb(120, 86, 62));
      }

      return c.p;
   }

   /** A tall plant on a 16 x 32 canvas (returned as {lower, upper}). */
   static int[][] tall(int kind) {
      int[] big = new int[16 * 32];
      Random r = new Random(1600L + kind);
      switch (kind) {
         case 0: {
            for (int s = 0; s < 11; s++) {
               int x = 1 + r.nextInt(14);
               int len = 10 + r.nextInt(20);
               int lean = r.nextInt(3) - 1;
               for (int i = 0; i < len; i++) {
                  int xx = x + lean * i / (len / 2 + 1);
                  int yy = 31 - i;
                  if (xx >= 0 && xx < 16) {
                     big[yy * 16 + xx] = g(135 + r.nextInt(70) + i);
                  }
               }
            }

            break;
         }

         case 1: {
            int[][] tips = new int[][]{{1, 14}, {4, 7}, {8, 3}, {12, 8}, {15, 15}, {3, 20}, {13, 20}};
            for (int[] tip : tips) {
               int steps = 22;
               for (int s = 0; s <= steps; s++) {
                  double f = (double)s / steps;
                  int x = (int)Math.round(8 + (tip[0] - 8) * f);
                  int y = (int)Math.round(31 + (tip[1] - 31) * (f * (2.0 - f)));
                  big[y * 16 + x] = g(120 + r.nextInt(40) + s * 2);
                  if (s > 4 && s % 2 == 0) {
                     int d = tip[0] < 8 ? -1 : 1;
                     if (x + d >= 0 && x + d < 16) {
                        big[y * 16 + x + d] = g(150 + r.nextInt(40));
                    }

                     if (y > 0) {
                        big[(y - 1) * 16 + x] = g(140 + r.nextInt(40));
                     }
                  }
               }
            }

            break;
         }

         default: {
            // a stalk with leaves, and the flower on top
            for (int y = 8; y < 32; y++) {
               big[y * 16 + 7] = rgb(48, 140, 38);
               big[y * 16 + 8] = rgb(40, 124, 32);
            }

            for (int[] lf : new int[][]{{26, -1}, {22, 1}, {17, -1}, {13, 1}}) {
               for (int i = 1; i <= 3; i++) {
                  int x = lf[1] < 0 ? 7 - i : 8 + i;
                  int y = lf[0] - i / 2;
                  big[y * 16 + x] = rgb(60, 160, 48);
                  big[(y + 1) * 16 + x] = rgb(46, 130, 36);
               }
            }

            int[] c1;
            int[] c2;
            switch (kind) {
               case 2:
                  c1 = new int[]{250, 198, 28};
                  c2 = new int[]{214, 152, 12};
                  break;
               case 3:
                  c1 = new int[]{198, 130, 222};
                  c2 = new int[]{160, 92, 190};
                  break;
               case 4:
                  c1 = new int[]{188, 28, 38};
                  c2 = new int[]{140, 16, 28};
                  break;
               default:
                  c1 = new int[]{240, 170, 204};
                  c2 = new int[]{214, 128, 170};
                  break;
            }

            if (kind == 2) {
               for (int dy = -5; dy <= 5; dy++) {
                  for (int dx = -5; dx <= 5; dx++) {
                     double d = Math.sqrt(dx * dx + dy * dy);
                     if (d <= 5.3 && d >= 2.5) {
                        big[(8 + dy) * 16 + 8 + dx] = d > 4.4 ? rgb(c2[0], c2[1], c2[2]) : rgb(c1[0], c1[1], c1[2]);
                     } else if (d < 2.5) {
                        big[(8 + dy) * 16 + 8 + dx] = rgb(98 + r.nextInt(16), 62, 14);
                     }
                  }
               }
            } else {
               for (int k = 0; k < 34; k++) {
                  int x = 3 + r.nextInt(10);
                  int y = 1 + r.nextInt(14) + (kind == 4 ? 8 : 0);
                  if (y < 22) {
                     int[] cc = r.nextInt(3) == 0 ? c2 : c1;
                     big[y * 16 + x] = rgb(cc[0], cc[1], cc[2]);
                     if (r.nextBoolean() && x + 1 < 16) {
                        big[y * 16 + x + 1] = rgb(cc[0] - 10, cc[1] - 10, cc[2] - 10);
                     }
                  }
               }

               if (kind == 4) {
                  for (int k = 0; k < 40; k++) {
                     int x = 2 + r.nextInt(12);
                     int y = 6 + r.nextInt(18);
                     big[y * 16 + x] = rgb(40 + r.nextInt(30), 110 + r.nextInt(40), 36);
                  }
               }
            }

            break;
         }
      }

      int[] upper = new int[256];
      int[] lower = new int[256];
      System.arraycopy(big, 0, upper, 0, 256);
      System.arraycopy(big, 256, lower, 0, 256);
      return new int[][]{lower, upper};
   }

   static int[] lilyPad() {
      Random r = new Random(1701L);
      int[] p = new int[256];
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            double dx = x - 7.5;
            double dy = y - 7.5;
            double d = Math.sqrt(dx * dx + dy * dy);
            boolean notch = dx > 0 && Math.abs(dy) < dx * 0.28 + 0.2;
            if (d <= 7.2 && !notch) {
               int v = r.nextInt(17) - 8;
               boolean vein = Math.abs(dx * 0.6 - dy * 0.8) < 0.5 && dx < 0 || Math.abs(dx * 0.6 + dy * 0.8) < 0.5 && dx < 0 || Math.abs(dy) < 0.5 && dx < 0;
               p[y * 16 + x] = vein ? rgb(26 + v / 2, 92, 34) : rgb(36 + v, 132 + v, 48 + v / 2);
               if (d > 6.3) {
                  p[y * 16 + x] = rgb(28 + v / 2, 104 + v, 38);
               }
            }
         }
      }

      return p;
   }

   /** The grey picture of a vine, tinted with the foliage colour: leaf clusters on thin stems. */
   static int[] vine() {
      Random r = new Random(1702L);
      int[] p = new int[256];
      for (int c = 0; c < 6; c++) {
         int x = 1 + c * 3 - r.nextInt(2);
         int len = 8 + r.nextInt(8);
         for (int y = 0; y < len; y++) {
            int xx = x + (y % 5 == 4 ? 1 : 0);
            if (xx >= 0 && xx < 16) {
               p[y * 16 + xx] = g(120 + r.nextInt(30));
            }
         }
      }

      for (int k = 0; k < 40; k++) {
         int x = r.nextInt(16);
         int y = r.nextInt(14);
         if (p[y * 16 + x] != 0 || x > 0 && p[y * 16 + x - 1] != 0 || x < 15 && p[y * 16 + x + 1] != 0) {
            p[y * 16 + x] = g(150 + r.nextInt(70));
            if (x + 1 < 16 && r.nextBoolean()) {
               p[y * 16 + x + 1] = g(140 + r.nextInt(60));
            }
         }
      }

      return p;
   }

   static int[] mushroomBlock(boolean red) {
      Random r = new Random(red ? 1801L : 1802L);
      int[] p = new int[256];
      int br = red ? 206 : 150;
      int bg = red ? 36 : 110;
      int bb = red ? 32 : 80;
      for (int i = 0; i < 256; i++) {
         int v = r.nextInt(15) - 7;
         p[i] = rgb(br + v, bg + v / 2, bb + v / 2);
      }

      int spots = red ? 7 : 4;
      for (int s = 0; s < spots; s++) {
         int x = r.nextInt(13);
         int y = r.nextInt(13);
         int w = 2 + r.nextInt(3);
         int h = 2 + r.nextInt(2);
         for (int dy = 0; dy < h; dy++) {
            for (int dx = 0; dx < w; dx++) {
               if (!(dx == 0 && dy == 0 || dx == w - 1 && dy == h - 1)) {
                  p[(y + dy) * 16 + x + dx] = red ? rgb(238 + r.nextInt(8), 236, 228) : rgb(188 + r.nextInt(10), 150, 118);
               }
            }
         }
      }

      return p;
   }

   static int[] mushroomStem() {
      Random r = new Random(1803L);
      int[] p = new int[256];
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            int v = r.nextInt(11) - 5 - (x % 4 == 1 ? 14 : 0);
            p[y * 16 + x] = rgb(222 + v, 212 + v, 192 + v);
         }
      }

      return p;
   }

   /** Snow seen from the side of a snow layer / a block with snow on it is the plain snow tile. */
   static int[] snowSide() {
      return Tex.tNoise(18L, 246, 250, 252, 3);
   }

   // ------------------------------------------------------------------------------------------------ the atlas
   static void blocks() {
      Tex.blit(GRANITE, granite());
      Tex.blit(DIORITE, diorite());
      Tex.blit(ANDESITE, andesite());
      Tex.blit(RED_SAND, redSand());
      Tex.blit(PODZOL_TOP, podzolTop());
      Tex.blit(PODZOL_SIDE, coverSide(podzolTop(), 1331L));
      Tex.blit(MYCELIUM_TOP, myceliumTop());
      Tex.blit(MYCELIUM_SIDE, coverSide(myceliumTop(), 1332L));
      Tex.blit(COARSE_DIRT, coarseDirt());
      Tex.blit(ICE, ice());
      Tex.blit(PACKED_ICE, packedIce());
      Tex.blit(RS_TOP, recolor(Tex.tNoise(22L, 219, 207, 163, 5), 184, 98, 32));
      Tex.blit(RS_SIDE, recolor(Tex.tSandstoneSide(), 184, 98, 32));
      for (int s = 0; s < 5; s++) {
         Tex.blit(PLANKS0 + s, recolor(Tex.tPlanks(), PLANK_COLORS[s][0], PLANK_COLORS[s][1], PLANK_COLORS[s][2]));
      }

      for (int sp = 0; sp < 3; sp++) {
         int[] b = bark(sp);
         Tex.blit(BARK0 + sp, b);
         Tex.blit(RING0 + sp, ring(sp));
         Tex.blit(TURNED0 + sp, transpose(b));
         Tex.blit(LEAVES0 + sp, leaves(sp));
      }

      Tex.blit(FERN, fern());
      Tex.blit(DEAD_BUSH, deadBush());
      for (int k = 0; k < 10; k++) {
         Tex.blit(FLOWER0 + k, flower(k));
      }

      Tex.blit(MUSH_BROWN, mushroom(false));
      Tex.blit(MUSH_RED, mushroom(true));
      for (int k = 0; k < 6; k++) {
         int[][] lu = tall(k);
         Tex.blit(DOUBLE0 + k * 2, lu[0]);
         Tex.blit(DOUBLE0 + k * 2 + 1, lu[1]);
      }

      Tex.blit(LILY, lilyPad());
      Tex.blit(VINE, vine());
      Tex.blit(MUSH_BLOCK_RED, mushroomBlock(true));
      Tex.blit(MUSH_BLOCK_BROWN, mushroomBlock(false));
      Tex.blit(MUSH_STEM, mushroomStem());
      Tex.blit(SNOW_SIDE, snowSide());
   }
}
