package bc;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Development tool (no GL needed): paints tiles of the real atlas to a PNG for review.
 * java ... bc.AtlasSheet out.png firstTile lastTile columns zoom [dark|light]
 */
public final class AtlasSheet {
   public static void main(String[] a) throws Exception {
      Tex.initPixels();
      int first = Integer.parseInt(a[1]);
      int last = Integer.parseInt(a[2]);
      int cols = Integer.parseInt(a[3]);
      int zoom = Integer.parseInt(a[4]);
      boolean dark = a.length > 5 && a[5].equals("dark");
      int n = last - first + 1;
      int rows = (n + cols - 1) / cols;
      int cell = 16 * zoom + 4;
      BufferedImage img = new BufferedImage(cols * cell, rows * cell, BufferedImage.TYPE_INT_RGB);
      Graphics2D g = img.createGraphics();
      for (int y = 0; y < img.getHeight(); y++) {
         for (int x = 0; x < img.getWidth(); x++) {
            boolean chk = ((x / 8 + y / 8) & 1) == 0;
            img.setRGB(x, y, dark ? (chk ? 0x2A2E36 : 0x22252C) : (chk ? 0x5A7DA8 : 0x4E6E96));
         }
      }

      for (int t = 0; t < n; t++) {
         int ox = t % cols * cell + 2;
         int oy = t / cols * cell + 2;
         int tx = Tex.tx(first + t);
         int ty = Tex.ty(first + t);
         for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
               int c = Tex.atlasPixels[(ty + y) * Tex.PX + tx + x];
               int al = c >>> 24;
               if (al == 0) {
                  continue;
               }

               for (int dy = 0; dy < zoom; dy++) {
                  for (int dx = 0; dx < zoom; dx++) {
                     img.setRGB(ox + x * zoom + dx, oy + y * zoom + dy, c & 0xFFFFFF);
                  }
               }
            }
         }
      }

      g.dispose();
      ImageIO.write(img, "png", new File(a[0]));
      System.out.println("wrote " + a[0] + " tiles " + first + ".." + last);
   }
}
