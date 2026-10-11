package bc;

import java.util.Random;

/**
 * Update 3.15 art: the pictures of the new shapes (iron bars, the edge of a glass pane, trapdoors), later the redstone parts. Painted with code
 * from fixed random seeds, like all tiles of the game. Tiles 600..999 belong to this file.
 */
final class TexArt315 {
   private TexArt315() {
   }

   static final int BARS = 600;
   /** The top of a glass pane: 601 clear, 602..617 in the colours. */
   static final int EDGE0 = 601;
   static final int BARS_EDGE = 618;
   /** Trapdoors: 620..625 the woods (oak, birch, spruce, jungle, acacia, dark oak), 626 iron. */
   static final int TRAP0 = 620;
   /** Bed tops (already turned for each facing): 630..633 the foot, 634..637 the head; 638 the side of a blanket, 639 / 640 the pillow, 641 banner cloth. */
   static final int BED_FOOT0 = 630;
   static final int BED_HEAD0 = 634;
   static final int BED_SIDE = 638;
   static final int PILLOW_TOP = 639;
   static final int PILLOW_SIDE = 640;
   static final int CLOTH = 641;
   /** Item pictures: 700..715 beds, 716..731 banners (by colour). */
   static final int BED_ITEM0 = 700;
   static final int BANNER_ITEM0 = 716;

   private static int rgb(int r, int g, int b) {
      return Tex.rgb(r, g, b);
   }

   private static int scale(int c, float f) {
      return rgb((int)((c >> 16 & 255) * f), (int)((c >> 8 & 255) * f), (int)((c & 255) * f));
   }

   private static int average(int tile) {
      long r = 0;
      long g = 0;
      long b = 0;
      int tx = Tex.tx(tile);
      int ty = Tex.ty(tile);
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            int p = Tex.atlasPixels[(ty + y) * Tex.PX + tx + x];
            r += p >> 16 & 255;
            g += p >> 8 & 255;
            b += p & 255;
         }
      }

      return rgb((int)(r / 256), (int)(g / 256), (int)(b / 256));
   }

   /** The 2 pixel wide cross in the middle of a tile, which is what the top and the bottom of a thin pane show. */
   static int[] edge(int color) {
      int[] p = new int[256];
      Random r = new Random(1500L + color);
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            if (x == 7 || x == 8 || y == 7 || y == 8) {
               int v = r.nextInt(9) - 4;
               p[y * 16 + x] = rgb((color >> 16 & 255) + v, (color >> 8 & 255) + v, (color & 255) + v);
            }
         }
      }

      return p;
   }

   /** Iron bars: a prison grating, dark vertical rods with a lit edge, 2 pixels wide every 4. */
   static int[] bars() {
      int[] p = new int[256];
      Random r = new Random(1517L);
      for (int rod = 0; rod < 4; rod++) {
         int x0 = 1 + rod * 4;
         for (int y = 0; y < 16; y++) {
            int v = r.nextInt(7) - 3;
            p[y * 16 + x0] = rgb(190 + v, 192 + v, 196 + v);
            p[y * 16 + x0 + 1] = rgb(104 + v, 106 + v, 112 + v);
         }

         p[x0] = rgb(226, 228, 232);
         p[16 + x0] = rgb(214, 216, 220);
         p[15 * 16 + x0 + 1] = rgb(70, 72, 78);
      }

      return p;
   }

   /** A wooden trapdoor: a frame round three planks with dark gaps. */
   static int[] woodTrap(int plankTile, long seed) {
      int base = average(plankTile);
      int[] p = new int[256];
      Random r = new Random(seed);
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            boolean frame = x < 2 || x > 13 || y < 2 || y > 13;
            boolean gap = !frame && (y == 5 || y == 10);
            float f = frame ? 0.78F : (gap ? 0.45F : 1.0F);
            float v = 1.0F + (r.nextInt(13) - 6) / 100.0F;
            p[y * 16 + x] = scale(base, f * v);
         }
      }

      // nails in the corners of the frame
      for (int[] n : new int[][]{{1, 1}, {14, 1}, {1, 14}, {14, 14}}) {
         p[n[1] * 16 + n[0]] = scale(base, 0.45F);
      }

      return p;
   }

   private static boolean inCell(int v) {
      return v >= 2 && v <= 6 || v >= 9 && v <= 13;
   }

   /** An iron trapdoor: a grey plate with a frame and a grid of recessed squares. */
   static int[] ironTrap() {
      int[] p = new int[256];
      Random r = new Random(1526L);
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            int v = r.nextInt(7) - 3;
            boolean frame = x < 2 || x > 13 || y < 2 || y > 13;
            int g = frame ? 176 : 200;
            if (!frame && inCell(x) && inCell(y)) {
               g = x == 2 || x == 9 || y == 2 || y == 9 ? 96 : 128;
            }

            p[y * 16 + x] = rgb(g + v, g + v, g + 4 + v);
         }
      }

      return p;
   }

   /** A picture turned the way a bed is turned (facing 1 = the head towards +z is the original). */
   static int[] turnedFor(int[] p, int facing) {
      int[] o = new int[256];
      for (int z = 0; z < 16; z++) {
         for (int x = 0; x < 16; x++) {
            int nx;
            int nz;
            switch (facing) {
               case 0:
                  nx = 15 - x;
                  nz = 15 - z;
                  break;
               case 2:
                  nx = z;
                  nz = 15 - x;
                  break;
               case 3:
                  nx = 15 - z;
                  nz = x;
                  break;
               default:
                  nx = x;
                  nz = z;
            }

            o[nz * 16 + nx] = p[z * 16 + x];
         }
      }

      return o;
   }

   private static int grey(int v) {
      return rgb(v, v, v);
   }

   /** The top of the foot of a bed: a woven blanket in light grey (the dye colour comes as the tint of the faces), with a fold. z = 0 is the foot end. */
   static int[] bedFootTop() {
      int[] p = new int[256];
      Random r = new Random(1630L);
      for (int z = 0; z < 16; z++) {
         for (int x = 0; x < 16; x++) {
            int v = 240 + r.nextInt(13) - 6;
            if (z % 2 == (x / 2) % 2) {
               v -= 5;
            }

            if (x == 0 || x == 15 || z == 0) {
               v -= 34;
            }

            if (z == 6 || z == 7) {
               v += z == 6 ? 10 : -22;
            }

            p[z * 16 + x] = grey(Math.min(255, v));
         }
      }

      return p;
   }

   /** The top of the head of a bed: the blanket again (the pillow is a box of its own). z = 15 is the head end. */
   static int[] bedHeadTop() {
      int[] p = new int[256];
      Random r = new Random(1634L);
      for (int z = 0; z < 16; z++) {
         for (int x = 0; x < 16; x++) {
            int v = 240 + r.nextInt(13) - 6;
            if (z % 2 == (x / 2) % 2) {
               v -= 5;
            }

            if (x == 0 || x == 15 || z == 15) {
               v -= 34;
            }

            p[z * 16 + x] = grey(Math.min(255, v));
         }
      }

      return p;
   }

   /** The side of a blanket: light with a darker lower edge (the rows that matter are 7..9). */
   static int[] bedSide() {
      int[] p = new int[256];
      Random r = new Random(1638L);
      for (int z = 0; z < 16; z++) {
         for (int x = 0; x < 16; x++) {
            int v = 232 + r.nextInt(11) - 5;
            if (z == 7) {
               v += 14;
            } else if (z == 9) {
               v -= 40;
            } else if (z > 9) {
               v -= 70;
            }

            p[z * 16 + x] = grey(Math.min(255, v));
         }
      }

      return p;
   }

   /** The pillow: white linen, a little darker at the seam. */
   static int[] pillow(boolean top) {
      int[] p = new int[256];
      Random r = new Random(top ? 1639L : 1640L);
      for (int z = 0; z < 16; z++) {
         for (int x = 0; x < 16; x++) {
            int v = 238 + r.nextInt(9) - 4;
            if (top && (x == 0 || x == 15 || z == 0 || z == 15)) {
               v -= 22;
            }

            p[z * 16 + x] = rgb(Math.min(255, v), Math.min(255, v), Math.min(255, v + 6));
         }
      }

      return p;
   }

   /** Banner cloth: a tight weave in light grey (tinted by the dye), darker at the bottom, lighter at the top. */
   static int[] cloth() {
      int[] p = new int[256];
      Random r = new Random(1641L);
      for (int z = 0; z < 16; z++) {
         for (int x = 0; x < 16; x++) {
            int v = 236 + r.nextInt(11) - 5;
            if ((x + z) % 4 == 0) {
               v -= 8;
            }

            v -= z * 2;
            p[z * 16 + x] = grey(Math.min(255, v));
            // the swallowtail: a notch in the lower edge
            if (z >= 13 && Math.abs(x - 7.5) < (z - 12) * 1.6) {
               p[z * 16 + x] = 0;
            }
         }
      }

      return p;
   }

   private static int[] canvas() {
      return new int[256];
   }

   private static void fill(int[] p, int x0, int y0, int x1, int y1, int c) {
      for (int y = y0; y <= y1; y++) {
         for (int x = x0; x <= x1; x++) {
            if (x >= 0 && x < 16 && y >= 0 && y < 16) {
               p[y * 16 + x] = c;
            }
         }
      }
   }

   private static int light(int c, int add) {
      return rgb(Math.min(255, (c >> 16 & 255) + add), Math.min(255, (c >> 8 & 255) + add), Math.min(255, (c & 255) + add));
   }

   /** The picture of a bed in the inventory: wooden frame and legs, a blanket in the dye colour and a white pillow. */
   static int[] bedSprite(int colour) {
      int[] p = canvas();
      int blanket = Color312.fleece(colour);
      int dark = light(blanket, -50);
      int wood = rgb(120, 86, 48);
      int woodDark = rgb(84, 60, 32);
      fill(p, 1, 11, 14, 12, wood);
      fill(p, 1, 12, 14, 12, woodDark);
      fill(p, 1, 13, 2, 14, woodDark);
      fill(p, 13, 13, 14, 14, woodDark);
      fill(p, 1, 6, 14, 10, blanket);
      fill(p, 1, 6, 14, 6, light(blanket, 28));
      fill(p, 1, 10, 14, 10, dark);
      fill(p, 6, 7, 14, 9, light(blanket, 8));
      fill(p, 1, 4, 5, 6, rgb(240, 240, 246));
      fill(p, 1, 4, 5, 4, rgb(255, 255, 255));
      fill(p, 1, 6, 5, 6, rgb(206, 206, 216));
      fill(p, 0, 5, 0, 12, woodDark);
      return p;
   }

   /** A banner in the inventory: a wooden pole and a flag in the dye colour with a notched lower edge. */
   static int[] bannerSprite(int colour) {
      int[] p = canvas();
      int flag = Color312.fleece(colour);
      int dark = light(flag, -46);
      int wood = rgb(130, 96, 54);
      fill(p, 7, 1, 8, 15, wood);
      fill(p, 2, 1, 13, 2, rgb(150, 112, 62));
      fill(p, 3, 3, 12, 11, flag);
      fill(p, 3, 3, 12, 3, light(flag, 26));
      fill(p, 3, 11, 12, 11, dark);
      fill(p, 3, 12, 5, 12, dark);
      fill(p, 10, 12, 12, 12, dark);
      fill(p, 7, 3, 8, 12, light(wood, -20));
      return p;
   }

   /** The pictures of the items of this update (called by TexExtra for every item that has a name). */
   static int[] sprite(int id) {
      if (id >= Decor315.BED_ITEM0 && id < Decor315.BED_ITEM0 + 16) {
         return bedSprite(id - Decor315.BED_ITEM0);
      }

      if (id >= Decor315.BANNER_ITEM0 && id < Decor315.BANNER_ITEM0 + 16) {
         return bannerSprite(id - Decor315.BANNER_ITEM0);
      }

      if (id >= Redstone319.ITEM_REDSTONE && id <= Redstone319.FIRE_CHARGE) {
         return Redstone319.sprite(id);
      }

      return null;
   }

   static void blocks() {
      Tex.blit(BARS, bars());
      for (int f = 0; f < 4; f++) {
         Tex.blit(BED_FOOT0 + f, turnedFor(bedFootTop(), f));
         Tex.blit(BED_HEAD0 + f, turnedFor(bedHeadTop(), f));
      }

      Tex.blit(BED_SIDE, bedSide());
      Tex.blit(PILLOW_TOP, pillow(true));
      Tex.blit(PILLOW_SIDE, pillow(false));
      Tex.blit(CLOTH, cloth());
      Tex.blit(EDGE0, edge(rgb(206, 216, 222)));
      for (int c = 0; c < 16; c++) {
         int col = Color312.CONCRETE[c];
         int light = rgb(Math.min(255, (col >> 16 & 255) + 40), Math.min(255, (col >> 8 & 255) + 40), Math.min(255, (col & 255) + 40));
         Tex.blit(EDGE0 + 1 + c, edge(light));
      }

      Tex.blit(BARS_EDGE, edge(rgb(112, 114, 120)));
      for (int sp = 0; sp < 6; sp++) {
         Tex.blit(TRAP0 + sp, woodTrap(Blocks.tiles[Blocks313.plank(sp)][0], 1530L + sp));
      }

      Tex.blit(TRAP0 + 6, ironTrap());
   }
}
