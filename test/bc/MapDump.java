package bc;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Development tool (no GL needed): paints a biome map of the new world generator to a PNG and prints the share of every biome.
 * java -cp ... bc.MapDump out.png seed centreX centreZ pixels blocksPerPixel [type]
 */
public final class MapDump {
   static final int[] COL = new int[]{
      0x2B4FA8, 0xE6D9A1, 0x8DB360, 0x4F8F3A, 0xE3C878, 0xF2F6F8, 0x8A8A8A, 0x1B3278, 0xA7B9D6, 0x3E78C8, 0xB5D0EE, 0x7FA85B, 0x24401E, 0xD5E5E8, 0x3F7A58,
      0x4B6E3C, 0x4B5B2E, 0xB5A24A, 0x1F7A1F, 0xC4713B, 0xA98AB0, 0xCDEBFF, 0x7A7A74, 0x6FC46F, 0xFFFFFF, 0xF2EFE9, 0x6BB04A, 0x3FB0C0
   };

   /** java ... bc.MapDump stats [type] : the share of every biome over 8 seeds x 16 km2 */
   static void stats(int type) {
      long[] count = new long[Biomes.N];
      long total = 0;
      long land = 0;
      for (long seed = 1; seed <= 8; seed++) {
         WorldSettings st = new WorldSettings();
         st.type = type;
         Gen g = Dim.makeGen(Dim.OVERWORLD, seed * 7919L, st);
         for (int j = -256; j < 256; j += 2) {
            for (int i = -256; i < 256; i += 2) {
               Gen.Col c = g.column(i * 20, j * 20);
               count[c.biome]++;
               total++;
               if (c.h >= 63) {
                  land++;
               }
            }
         }
      }

      System.out.printf("land %.1f%% of %d samples%n", land * 100.0 / total, total);
      for (int b = 0; b < Biomes.N; b++) {
         System.out.printf("%-16s %5.1f%% of all, %5.1f%% of land%n", Biomes.NAME[b], count[b] * 100.0 / total, Biomes.ocean(b) || Biomes.river(b) ? 0.0 : count[b] * 100.0 / land);
      }
   }

   public static void main(String[] a) throws Exception {
      if (a[0].equals("stats")) {
         stats(a.length > 1 ? Integer.parseInt(a[1]) : WorldSettings.DEFAULT);
         return;
      }

      long seed = Long.parseLong(a[1]);
      int cx = Integer.parseInt(a[2]);
      int cz = Integer.parseInt(a[3]);
      int px = Integer.parseInt(a[4]);
      int step = Integer.parseInt(a[5]);
      WorldSettings st = new WorldSettings();
      st.type = a.length > 6 ? Integer.parseInt(a[6]) : WorldSettings.DEFAULT;
      Gen g = Dim.makeGen(Dim.OVERWORLD, seed, st);
      BufferedImage img = new BufferedImage(px, px, BufferedImage.TYPE_INT_RGB);
      long[] count = new long[Biomes.N];
      long land = 0;
      long total = 0;
      int[] hmin = new int[]{999};
      int hmax = 0;
      long t0 = System.nanoTime();
      int[][] hh = new int[px + 2][px + 2];
      int[][] bb = new int[px + 2][px + 2];
      for (int j = -1; j <= px; j++) {
         for (int i = -1; i <= px; i++) {
            Gen.Col c = g.column(cx + (i - px / 2) * step, cz + (j - px / 2) * step);
            hh[j + 1][i + 1] = c.h;
            bb[j + 1][i + 1] = c.biome;
         }
      }

      for (int j = 0; j < px; j++) {
         for (int i = 0; i < px; i++) {
            int h = hh[j + 1][i + 1];
            int b = bb[j + 1][i + 1];
            count[b]++;
            total++;
            if (h >= 63) {
               land++;
            }

            hmax = Math.max(hmax, h);
            hmin[0] = Math.min(hmin[0], h);
            int col = COL[Math.min(COL.length - 1, b)];
            double shade = 1.0;
            if (h >= 63) {
               double d = (hh[j][i] - hh[j + 2][i + 2]) * 0.06 / Math.max(1, step / 4.0) * 4.0;
               shade = Math.max(0.55, Math.min(1.35, 1.0 + d));
               shade *= 0.8 + 0.2 * Math.min(1.0, (h - 63) / 90.0);
            } else {
               shade = 0.6 + 0.4 * Math.max(0.0, Math.min(1.0, (h - 20) / 43.0));
            }

            int r = Math.min(255, (int)((col >> 16 & 255) * shade));
            int gg = Math.min(255, (int)((col >> 8 & 255) * shade));
            int bl = Math.min(255, (int)((col & 255) * shade));
            img.setRGB(i, j, r << 16 | gg << 8 | bl);
         }
      }

      ImageIO.write(img, "png", new File(a[0]));
      System.out.printf("%d columns in %d ms, height %d..%d, land %.1f%%%n", total, (System.nanoTime() - t0) / 1000000, hmin[0], hmax, land * 100.0 / total);
      for (int b = 0; b < Biomes.N; b++) {
         if (count[b] > 0) {
            System.out.printf("%-16s %5.1f%%%n", Biomes.NAME[b], count[b] * 100.0 / total);
         }
      }
   }
}
