package bc;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Development tool (no GL, no window): draws menu screens with the real screen code into a picture ({@link GuiRaster}) so that layouts can be checked
 * quickly. java ... bc.UiShots outdir [WxH@scale] [bg=picture.png] scene...
 */
public final class UiShots {
   public static void main(String[] args) throws Exception {
      String out = args[0];
      int w = 1030;
      int h = 720;
      int scale = 3;
      String bg = null;
      java.util.ArrayList<String> scenes = new java.util.ArrayList<>();
      for (int i = 1; i < args.length; i++) {
         String a = args[i];
         if (a.matches("\\d+x\\d+@\\d+")) {
            String[] p = a.split("[x@]");
            w = Integer.parseInt(p[0]);
            h = Integer.parseInt(p[1]);
            scale = Integer.parseInt(p[2]);
         } else if (a.startsWith("bg=")) {
            bg = a.substring(3);
         } else {
            scenes.add(a);
         }
      }

      BufferedImage bgImg = bg == null ? null : ImageIO.read(new File(bg));
      for (String scene : scenes) {
         GuiRaster r = new GuiRaster(w, h);
         r.fill(0xFF101820);
         if (bgImg != null) {
            r.drawImage(bgImg, 0, 0, w, h);
         }

         Game g = new Game();
         g.gui = r;
         g.opt = new Options(new File(out, "opt"));
         g.fbW = w;
         g.fbH = h;
         g.winW = w;
         g.winH = h;
         r.begin(w, h, scale);
         g.activeBtnsSwap();
         g.mouseX = -1000;
         g.mouseY = -1000;
         Scenes.run(g, r, scene, bgImg != null);
         r.save(out + "/" + scene + ".png");
         System.out.println(scene + " -> " + out + "/" + scene + ".png  (" + (int)r.w + "x" + (int)r.h + " gui units)");
      }
   }
}
