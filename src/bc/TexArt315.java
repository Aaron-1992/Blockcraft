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

   static void blocks() {
      Tex.blit(BARS, bars());
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
