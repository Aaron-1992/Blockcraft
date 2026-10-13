package bc;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Renders procedurally generated tiles / item sprites to a PNG contact sheet (no GL needed) so art can be
 * reviewed without starting the game:   java -cp ... bc.TexSheet out.png
 */
public final class TexSheet {
   static final int S = 10; // zoom

   public static void main(String[] args) throws Exception {
      if (args.length > 1 && (args[1].equals("pixel32") || args[1].equals("smooth"))) {
         ResourcePacks.select(ResourcePacks.ID_DEFAULT, null);
         Tex.initPixels();
         int[] atlas = TexPixel32.atlas(Tex.atlasPixels);
         int width = Tex.COLS * TexPixel32.SIZE;
         BufferedImage image = new BufferedImage(width, width, BufferedImage.TYPE_INT_ARGB);
         image.setRGB(0, 0, width, width, atlas, 0, width);
         ImageIO.write(image, "png", new File(args[0]));
         System.out.println("wrote default 32px atlas to " + args[0]);
         return;
      }
      List<int[]> tiles = new ArrayList<>();
      List<String> names = new ArrayList<>();
      add(tiles, names, "dry farm", TexExtra.farmTop());
      add(tiles, names, "wet farm", TexExtra.farmWetTop());
      for (int i = 0; i < 4; i++) {
         add(tiles, names, "wheat" + i * 2 + "/" + (i * 2 + 1), TexExtra.wheatTile(i * 2 + 1));
      }
      for (int i = 0; i < 4; i++) {
         add(tiles, names, "carrot g" + i, TexExtra.rootCropTile(false, i));
      }
      for (int i = 0; i < 4; i++) {
         add(tiles, names, "potato g" + i, TexExtra.rootCropTile(true, i));
      }
      add(tiles, names, "hay side", TexExtra.haySide());
      add(tiles, names, "hay top", TexExtra.hayTop());
      for (int id : new int[]{285, 286, 287, 264, 258, 336, 337, 338, 339, 340}) {
         add(tiles, names, "item " + id, TexExtra.sprite(id));
      }
      if (args.length > 1 && args[1].equals("37")) {
         tiles.clear(); // only the Update 3.7 art:  bc.TexSheet out.png 37
         names.clear();
      }
      // ---- Update 3.7
      add(tiles, names, "sugar cane", TexArt37.cane());
      add(tiles, names, "bookshelf", TexArt37.bookshelfSide());
      add(tiles, names, "pumpkin top", TexArt37.pumpkinTop());
      add(tiles, names, "pumpkin side", TexArt37.pumpkinSide());
      add(tiles, names, "carved", TexArt37.pumpkinFace(false));
      add(tiles, names, "jack o'lantern", TexArt37.pumpkinFace(true));
      add(tiles, names, "melon top", TexArt37.melonTop());
      add(tiles, names, "melon side", TexArt37.melonSide());
      for (int i = 0; i < 4; i++) {
         add(tiles, names, "stem g" + i, TexArt37.stem(i));
      }
      add(tiles, names, "cake top", TexArt37.cakeTop());
      add(tiles, names, "cake side", TexArt37.cakeSide(false));
      add(tiles, names, "cake bottom", TexArt37.cakeBottom());
      add(tiles, names, "cake cut", TexArt37.cakeSide(true));
      for (int f = 0; f < 16; f += 3) {
         add(tiles, names, "fire f" + f, TexArt37.fireFrame(f));
      }
      for (int id = 341; id <= 350; id++) {
         add(tiles, names, "item " + id, TexExtra.sprite(id));
      }
      int cols = 8;
      int rows = (tiles.size() + cols - 1) / cols;
      int cell = 16 * S + 6;
      BufferedImage img = new BufferedImage(cols * cell, rows * cell, BufferedImage.TYPE_INT_ARGB);
      for (int y = 0; y < img.getHeight(); y++) {
         for (int x = 0; x < img.getWidth(); x++) {
            img.setRGB(x, y, ((x / 12 + y / 12) & 1) == 0 ? 0xFF5A7DA8 : 0xFF4E6E96); // blue-ish checker: shows transparency
         }
      }
      for (int t = 0; t < tiles.size(); t++) {
         int ox = t % cols * cell + 3;
         int oy = t / cols * cell + 3;
         int[] p = tiles.get(t);
         for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
               int c = p[y * 16 + x];
               if (c >>> 24 == 0) {
                  continue;
               }
               for (int dy = 0; dy < S; dy++) {
                  for (int dx = 0; dx < S; dx++) {
                     img.setRGB(ox + x * S + dx, oy + y * S + dy, c | 0xFF000000);
                  }
               }
            }
         }
      }
      ImageIO.write(img, "png", new File(args[0]));
      System.out.println("wrote " + args[0] + " (" + tiles.size() + " tiles): " + names);
   }

   static void add(List<int[]> tiles, List<String> names, String name, int[] px) {
      tiles.add(px);
      names.add(name);
   }
}
