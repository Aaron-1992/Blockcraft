package bc;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Development tool (no GL needed): pastes one building template onto flat ground and draws the real chunk meshes of it with the software
 * rasterizer of {@link MeshView} (stairs, panes, doors and fences have their true shapes and pictures).
 * java ... bc.StructView out.png name style variant turns yaw pitch distance [width] [height] [fov]
 * (name = a key of Buildings314.ALL, style 0 oak 1 desert 2 savanna 3 spruce 4 snow, turns = quarter turns of the template, yaw 0 looks south, 180 north,
 * yaw / pitch / distance place the camera round the middle of the building)
 */
public final class StructView {
   static final int GROUND = 60;

   static BufferedImage shot(World world, double lx, double ly, double lz, double yaw, double pitch, double dist, int w, int h, double fov) {
      int ss = 2;
      MeshView mv = new MeshView(w * ss, h * ss);
      double cxp = lx + Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)) * dist;
      double cyp = ly + Math.sin(Math.toRadians(pitch)) * dist;
      double czp = lz - Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)) * dist;
      mv.camera(cxp, cyp, czp, yaw, pitch, fov);
      mv.clear(0x78A8E8, 0xC8DCF0);
      mv.drawWorld(world);
      BufferedImage big = mv.render(world, ss);
      BufferedImage small = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
      for (int y = 0; y < h; y++) {
         for (int x = 0; x < w; x++) {
            int r = 0;
            int g = 0;
            int b = 0;
            for (int j = 0; j < ss; j++) {
               for (int i = 0; i < ss; i++) {
                  int p = big.getRGB(x * ss + i, y * ss + j);
                  r += p >> 16 & 255;
                  g += p >> 8 & 255;
                  b += p & 255;
               }
            }

            int n = ss * ss;
            small.setRGB(x, y, (r / n) << 16 | (g / n) << 8 | b / n);
         }
      }

      return small;
   }

   public static void main(String[] a) throws Exception {
      Tex.initPixels();
      Tpl t = Buildings314.get(a[1]);
      if (t == null) {
         throw new IllegalArgumentException("no template " + a[1] + " in " + Buildings314.ALL.keySet());
      }

      Style st = Style.of(Integer.parseInt(a[2]), Integer.parseInt(a[3]));
      int q = Integer.parseInt(a[4]);
      double yaw = Double.parseDouble(a[5]);
      double pitch = Double.parseDouble(a[6]);
      double dist = Double.parseDouble(a[7]);
      int w = a.length > 8 ? Integer.parseInt(a[8]) : 720;
      int h = a.length > 9 ? Integer.parseInt(a[9]) : 480;
      double fov = a.length > 10 ? Double.parseDouble(a[10]) : 60.0;
      World world = MeshView.flatWorld(3, GROUND);
      int fw = t.fw(q);
      int fd = t.fd(q);
      int x0 = -fw / 2;
      int z0 = -fd / 2;
      for (Chunk c : world.chunks.values()) {
         t.prepare(c, st, q, x0, GROUND, z0, Blocks.GRASS);
         t.paste(c, st, q, x0, GROUND, z0, 12345);
         c.computeHeightmap();
      }

      MeshView.relight(world);
      BufferedImage img = shot(world, x0 + fw / 2.0, GROUND + 1 + t.h * 0.35, z0 + fd / 2.0, yaw, pitch, dist, w, h, fov);
      ImageIO.write(img, "png", new File(a[0]));
      System.out.println("ok " + t.name + " " + t.w + "x" + t.h + "x" + t.d + " doors " + t.doors.size() + " beds " + t.beds + " chests " + t.chests);
   }
}
