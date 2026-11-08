package bc;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Development tool (no GL needed): composes the headless {@link UiShots} scenes of
 * the V6.37 menus into one review board. NOT a GPU screenshot.
 *
 * java ... bc.MenuBoard637 out.png indir scene:captions...
 */
public final class MenuBoard637 {
   private MenuBoard637() {
   }

   public static void main(String[] a) throws Exception {
      String out = a[0];
      String dir = a[1];
      int n = a.length - 2;
      int cellW = 512;
      int cellH = 358;
      int cap = 24;
      int head = 56;
      int gap = 10;
      int cols = 2;
      int rows = (n + cols - 1) / cols;
      int W = cols * cellW + (cols + 1) * gap;
      int H = head + rows * (cellH + cap + gap) + gap;
      BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
      Graphics2D g = img.createGraphics();
      g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
      g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g.setColor(new Color(0x0A0D12));
      g.fillRect(0, 0, W, H);
      g.setColor(Color.WHITE);
      g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
      g.drawString("V6.37 - Minecraft-style menus, lifted clouds, clean shader toggles", gap, 26);
      g.setColor(new Color(0x9AA6BC));
      g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
      g.drawString("Headless software renders of the real menu code at 343x240 GUI units. NOT GPU screenshots.", gap, 44);

      for (int i = 0; i < n; i++) {
         String spec = a[i + 2];
         int colon = spec.indexOf(':');
         String scene = colon < 0 ? spec : spec.substring(0, colon);
         String caption = colon < 0 ? spec : spec.substring(colon + 1);
         BufferedImage shot = ImageIO.read(new File(dir, scene + ".png"));
         int cx = gap + (i % cols) * (cellW + gap);
         int cy = head + (i / cols) * (cellH + cap + gap);
         g.setColor(new Color(0x20262F));
         g.fillRect(cx - 1, cy - 1, cellW + 2, cellH + 2);
         g.drawImage(shot, cx, cy, cellW, cellH, null);
         g.setColor(new Color(0xFFDF58));
         g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
         g.drawString((i + 1) + " / " + caption, cx, cy + cellH + 17);
      }
      g.dispose();
      ImageIO.write(img, "png", new File(out));
      System.out.println("wrote " + out + " (" + W + "x" + H + ", " + n + " screens)");
   }
}
