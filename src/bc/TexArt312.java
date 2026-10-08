package bc;

import java.util.Random;

/**
 * Update 3.12 art: the tiles of the Nether, the End and the new blocks. Everything is painted with code (like all tiles of the game),
 * from fixed random seeds, so a tile always looks the same. Tiles 256.. are new in 3.12 (the atlas is 32 x 32 tiles now).
 */
final class TexArt312 {
   private TexArt312() {
   }

   // ---- the Nether ----
   static final int NETHERRACK = 256;
   static final int SOUL_SAND = 257;
   static final int QUARTZ_ORE = 258;
   static final int NETHER_BRICKS = 259;
   static final int MAGMA = 260;
   static final int QUARTZ_SIDE = 261;
   static final int QUARTZ_TOP = 262;
   /** Animated (16 frames, see {@link #portalFrames}). */
   static final int PORTAL = 263;
   // ---- the End ----
   static final int END_STONE = 264;
   static final int FRAME_TOP = 265;
   static final int FRAME_TOP_EYE = 266;
   static final int FRAME_SIDE = 267;
   static final int END_PORTAL = 268;
   static final int DRAGON_EGG = 269;
   /** Item pictures: Items.QUARTZ (352) is tile 272, every following item id one tile more. */
   static final int ITEM0 = 272;

   static final int[][] portalFrames = new int[16][];

   static int rgb(int r, int g, int b) {
      return Tex.rgb(r, g, b);
   }

   static int[] netherrack() {
      Random r = new Random(1201L);
      int[] p = new int[256];
      int[] blotch = new int[16];
      for (int i = 0; i < 16; i++) {
         blotch[i] = r.nextInt(33) - 16;
      }

      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            int v = blotch[y / 4 * 4 + x / 4] + r.nextInt(21) - 10;
            p[y * 16 + x] = rgb(112 + v, 55 + v / 2, 54 + v / 2);
         }
      }

      for (int k = 0; k < 16; k++) {
         p[r.nextInt(256)] = rgb(72, 31, 33);
      }

      for (int k = 0; k < 9; k++) {
         p[r.nextInt(256)] = rgb(152, 84, 80);
      }

      return p;
   }

   static int[] soulSand() {
      Random r = new Random(1202L);
      int[] p = new int[256];
      for (int i = 0; i < 256; i++) {
         int v = r.nextInt(17) - 8;
         p[i] = rgb(86 + v, 66 + v, 52 + v);
      }

      // the faces in the sand: small dark pits (an eye or a mouth), a few of them
      for (int k = 0; k < 7; k++) {
         int x = r.nextInt(14);
         int y = r.nextInt(14);
         for (int dy = 0; dy < 2; dy++) {
            for (int dx = 0; dx < 2; dx++) {
               p[(y + dy) * 16 + x + dx] = rgb(48 + dx * 4, 36 + dy * 3, 30);
            }
         }
      }

      return p;
   }

   static int[] quartzOre() {
      int[] p = netherrack();
      Random r = new Random(1203L);
      for (int k = 0; k < 8; k++) {
         int x = r.nextInt(13);
         int y = r.nextInt(13);
         int[][] shape = new int[][]{{0, 0}, {1, 0}, {0, 1}, {1, 1}, {2, 1}};
         for (int[] s : shape) {
            if (r.nextInt(5) != 0) {
               int v = r.nextInt(25) - 12;
               p[(y + s[1]) * 16 + x + s[0]] = rgb(232 + v / 2, 224 + v / 2, 212 + v);
            }
         }
      }

      return p;
   }

   static int[] netherBricks() {
      Random r = new Random(1204L);
      int[] p = new int[256];
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            int row = y / 4;
            int shifted = (x + row % 2 * 4) % 8;
            boolean mortar = y % 4 == 3 || shifted == 7;
            int n = r.nextInt(9) - 4;
            p[y * 16 + x] = mortar ? rgb(26 + n, 13 + n / 2, 17 + n / 2) : rgb(66 + n, 33 + n / 2, 38 + n / 2);
         }
      }

      return p;
   }

   /** Dark rock with glowing cracks (the lines between the cells of a random pattern). */
   static int[] magma() {
      Random r = new Random(1205L);
      double[][] seeds = new double[9][2];
      for (double[] s : seeds) {
         s[0] = r.nextDouble() * 16.0;
         s[1] = r.nextDouble() * 16.0;
      }

      int[] p = new int[256];
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            double d1 = 99.0;
            double d2 = 99.0;
            for (double[] s : seeds) {
               for (int ox = -1; ox <= 1; ox++) {
                  for (int oy = -1; oy <= 1; oy++) {
                     double dx = x + 0.5 - (s[0] + ox * 16.0);
                     double dy = y + 0.5 - (s[1] + oy * 16.0);
                     double d = Math.sqrt(dx * dx + dy * dy);
                     if (d < d1) {
                        d2 = d1;
                        d1 = d;
                     } else if (d < d2) {
                        d2 = d;
                     }
                  }
               }
            }

            int n = r.nextInt(15) - 7;
            double edge = d2 - d1;
            if (edge < 1.15) {
               p[y * 16 + x] = rgb(255, 150 + n, 40 + n);
            } else if (edge < 1.9) {
               p[y * 16 + x] = rgb(214 + n, 84 + n, 14);
            } else {
               p[y * 16 + x] = rgb(66 + n, 25 + n / 2, 16);
            }
         }
      }

      return p;
   }

   static int[] quartz(boolean top) {
      Random r = new Random(top ? 1207L : 1206L);
      int[] p = new int[256];
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            int n = r.nextInt(7) - 3;
            boolean border = top ? x < 1 || y < 1 || x > 14 || y > 14 : y == 15;
            int base = border ? 219 : 236;
            p[y * 16 + x] = rgb(base + n, base - 5 + n, base - 12 + n);
         }
      }

      return p;
   }

   /** The film of a nether portal: 16 frames of purple light that move; every frame tiles, and the loop is endless. */
   static int[][] portal() {
      int[][] frames = new int[16][256];
      for (int f = 0; f < 16; f++) {
         double ph = f / 16.0 * Math.PI * 2.0;
         Random r = new Random(1208L + f);
         for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
               double u = x / 16.0 * Math.PI * 2.0;
               double v = y / 16.0 * Math.PI * 2.0;
               double n = Math.sin(u * 2.0 + v + ph) + 0.8 * Math.sin(v * 2.0 - u - ph * 2.0) + 0.6 * Math.sin(u * 3.0 + v * 2.0 - ph);
               double t = (n + 2.4) / 4.8; // 0..1
               int spark = r.nextInt(30) == 0 ? 70 : 0;
               int rr = (int)(70 + 110 * t) + spark;
               int gg = (int)(14 + 40 * t) + spark;
               int bb = (int)(130 + 110 * t) + spark;
               int a = (int)(150 + 70 * t);
               frames[f][y * 16 + x] = Tex.argb(a, rr, gg, bb);
            }
         }
      }

      return frames;
   }

   static int[] endStone() {
      Random r = new Random(1209L);
      int[] p = new int[256];
      int[] blotch = new int[16];
      for (int i = 0; i < 16; i++) {
         blotch[i] = r.nextInt(17) - 8;
      }

      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            int v = blotch[y / 4 * 4 + x / 4] + r.nextInt(13) - 6;
            p[y * 16 + x] = rgb(222 + v, 224 + v, 168 + v);
         }
      }

      for (int k = 0; k < 18; k++) {
         int i = r.nextInt(256);
         p[i] = rgb(196, 198, 138);
      }

      return p;
   }

   /** Top of an end portal frame: a green slab; with the eye in it, a round dark green eye with a black pupil. */
   static int[] frameTop(boolean eye) {
      Random r = new Random(eye ? 1211L : 1210L);
      int[] p = new int[256];
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            int n = r.nextInt(9) - 4;
            int edge = Math.min(Math.min(x, 15 - x), Math.min(y, 15 - y));
            if (edge < 2) {
               p[y * 16 + x] = rgb(76 + n, 140 + n, 104 + n);
            } else if (edge < 3) {
               p[y * 16 + x] = rgb(44 + n, 98 + n, 74 + n);
            } else {
               p[y * 16 + x] = rgb(22 + n, 60 + n, 50 + n);
            }
         }
      }

      if (eye) {
         for (int y = 3; y < 13; y++) {
            for (int x = 3; x < 13; x++) {
               double dx = x - 7.5;
               double dy = y - 7.5;
               double d = Math.sqrt(dx * dx + dy * dy);
               if (d < 4.6) {
                  int n = r.nextInt(7) - 3;
                  p[y * 16 + x] = d < 1.9 ? rgb(6, 20, 18) : rgb(40 + n, 128 + n, 104 + n);
               }
            }
         }
      }

      return p;
   }

   static int[] frameSide() {
      Random r = new Random(1212L);
      int[] p = endStone();
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            int n = r.nextInt(9) - 4;
            if (y < 3) {
               p[y * 16 + x] = rgb(60 + n, 118 + n, 90 + n);
            } else if (y == 3) {
               p[y * 16 + x] = rgb(38 + n, 86 + n, 66 + n);
            }
         }
      }

      return p;
   }

   static int[] endPortal() {
      Random r = new Random(1213L);
      int[] p = new int[256];
      for (int i = 0; i < 256; i++) {
         int n = r.nextInt(7);
         p[i] = rgb(4 + n, 6 + n, 14 + n * 2);
         int s = r.nextInt(26);
         if (s == 0) {
            p[i] = rgb(110, 224, 208);
         } else if (s == 1) {
            p[i] = rgb(60, 130, 170);
         } else if (s == 2) {
            p[i] = rgb(210, 240, 255);
         }
      }

      return p;
   }

   static int[] dragonEgg() {
      Random r = new Random(1214L);
      int[] p = new int[256];
      for (int i = 0; i < 256; i++) {
         int n = r.nextInt(9) - 4;
         p[i] = rgb(14 + n, 6 + n / 2, 22 + n);
         if (r.nextInt(7) == 0) {
            p[i] = rgb(68 + n * 2, 22 + n, 104 + n * 2);
         }
      }

      return p;
   }

   static void blocks() {
      Tex.blit(NETHERRACK, netherrack());
      Tex.blit(SOUL_SAND, soulSand());
      Tex.blit(QUARTZ_ORE, quartzOre());
      Tex.blit(NETHER_BRICKS, netherBricks());
      Tex.blit(MAGMA, magma());
      Tex.blit(QUARTZ_SIDE, quartz(false));
      Tex.blit(QUARTZ_TOP, quartz(true));
      int[][] pf = portal();
      System.arraycopy(pf, 0, portalFrames, 0, 16);
      Tex.blit(PORTAL, portalFrames[0]);
      Tex.blit(END_STONE, endStone());
      Tex.blit(FRAME_TOP, frameTop(false));
      Tex.blit(FRAME_TOP_EYE, frameTop(true));
      Tex.blit(FRAME_SIDE, frameSide());
      Tex.blit(END_PORTAL, endPortal());
      Tex.blit(DRAGON_EGG, dragonEgg());
      Color312.tiles();
   }

   // ------------------------------------------------------------------ item pictures
   private static int sc(int c, float f) {
      return TexExtra.sc(c, f);
   }

   /** The 16 x 16 picture of an item of Update 3.12 (null: none). */
   static int[] sprite(int id) {
      TexExtra.Cv cv = new TexExtra.Cv();
      switch (id) {
         case Items.QUARTZ: {
            int c = 0xE8E2D8;
            cv.line(4, 12, 10, 3, sc(c, 0.85F));
            cv.line(5, 12, 11, 4, c);
            cv.line(6, 12, 12, 5, c);
            cv.line(7, 12, 12, 6, sc(c, 0.9F));
            cv.rect(5, 12, 5, 1, sc(c, 0.7F));
            cv.set(6, 6, 0xFFFFFF);
            cv.set(7, 5, 0xFFFFFF);
            cv.set(9, 4, sc(c, 1.15F));
            return cv.p;
         }
         case Items.NETHER_BRICK: {
            int c = 0x4A222A;
            cv.rect(3, 5, 10, 7, c);
            cv.rect(3, 5, 10, 1, sc(c, 1.35F));
            cv.rect(3, 11, 10, 1, sc(c, 0.65F));
            cv.rect(12, 6, 1, 5, sc(c, 0.8F));
            cv.set(5, 7, sc(c, 1.3F));
            cv.set(8, 8, sc(c, 1.25F));
            cv.set(6, 9, sc(c, 0.8F));
            return cv.p;
         }
         case Items.BLAZE_ROD: {
            cv.line(3, 13, 12, 3, 0xE8A010);
            cv.line(4, 13, 13, 3, 0xF8C830);
            cv.line(5, 13, 13, 4, 0xC47808);
            for (int k = 0; k < 4; k++) {
               cv.set(5 + k * 2, 11 - k * 2, 0x8A4A04);
            }

            cv.set(12, 3, 0xFFF0A0);
            cv.set(13, 3, 0xFFF0A0);
            return cv.p;
         }
         case Items.BLAZE_POWDER: {
            cv.ell(8.0, 10.0, 5.0, 3.5, 0xE89A10);
            cv.ell(8.0, 9.0, 4.0, 2.5, 0xF8C020);
            cv.set(6, 8, 0xFFE860);
            cv.set(9, 8, 0xFFE860);
            cv.set(10, 10, 0xFFF4B0);
            cv.set(5, 11, 0xB86408);
            cv.set(11, 12, 0xB86408);
            return cv.p;
         }
         case Items.GHAST_TEAR: {
            cv.ell(8.0, 9.5, 3.8, 4.2, 0xE6F0F4);
            cv.rect(7, 3, 2, 3, 0xE6F0F4);
            cv.rect(8, 5, 1, 2, 0xE6F0F4);
            cv.ell(8.0, 10.0, 2.6, 3.0, 0xCBE0EA);
            cv.set(6, 8, 0xFFFFFF);
            cv.set(7, 7, 0xFFFFFF);
            cv.set(9, 12, 0xA9C4D2);
            return cv.p;
         }
         case Items.GOLD_NUGGET: {
            cv.ell(8.0, 9.0, 3.2, 2.8, 0xF2C230);
            cv.ell(7.5, 8.5, 2.0, 1.6, 0xFCE060);
            cv.set(6, 8, 0xFFFFFF);
            cv.set(9, 11, 0xB88810);
            cv.set(10, 10, 0xB88810);
            return cv.p;
         }
         case Items.ENDER_PEARL: {
            cv.ell(8.0, 8.0, 5.5, 5.5, 0x0C5A4A);
            cv.ell(8.0, 8.5, 4.6, 4.6, 0x1B8C70);
            cv.ell(7.0, 7.0, 2.4, 2.4, 0x2FBA96);
            cv.set(6, 5, 0xBDF5E4);
            cv.set(5, 7, 0x58D8B4);
            cv.set(11, 11, 0x083C30);
            cv.set(10, 12, 0x083C30);
            return cv.p;
         }
         case Items.EYE_OF_ENDER: {
            cv.ell(8.0, 8.0, 5.5, 5.5, 0x0C5A4A);
            cv.ell(8.0, 8.0, 4.7, 4.7, 0x28A080);
            cv.ell(8.0, 8.0, 2.7, 2.7, 0x0E3A32);
            cv.ell(8.0, 8.0, 1.6, 1.6, 0x74F0C4);
            cv.set(8, 8, 0x040E0C);
            cv.set(7, 7, 0xDFFFF5);
            return cv.p;
         }
         case Items.EMERALD: {
            int c = 0x17C964;
            cv.rect(6, 3, 4, 1, sc(c, 1.2F));
            cv.rect(4, 4, 8, 2, sc(c, 1.1F));
            cv.rect(3, 6, 10, 3, c);
            cv.rect(4, 9, 8, 2, sc(c, 0.8F));
            cv.rect(6, 11, 4, 2, sc(c, 0.65F));
            cv.rect(6, 5, 4, 5, sc(c, 1.25F));
            cv.set(5, 5, 0xE8FFF0);
            cv.set(6, 4, 0xE8FFF0);
            return cv.p;
         }
         default:
            return id >= Items.DYE1 && id < Items.DYE1 + 15 ? Color312.dyeSprite(id) : null;
      }
   }
}
