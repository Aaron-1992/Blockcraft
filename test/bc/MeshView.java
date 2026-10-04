package bc;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;

/**
 * Dev tool: draws the chunk meshes of the real Mesher with a tiny software rasterizer (no OpenGL), so new block shapes can be looked at
 * without a GPU. Single-sided faces are culled like in the game (counter-clockwise = front).
 *
 *   java -cp ... bc.MeshView out.png scene [yaw pitch dist [w h [lookX lookY lookZ]]]
 *   scenes: see Scenes315.names()
 */
public final class MeshView {
   private final int w;
   private final int h;
   private final int[] img;
   private final float[] depth;
   private double cx;
   private double cy;
   private double cz;
   private double fx;
   private double fy;
   private double fz;
   private double rx;
   private double rz;
   private double ux;
   private double uy;
   private double uz;
   private double focal;
   boolean cull = true;
   /** 1 = full daylight; the lighting is only an impression (skylight and blocklight 0..15 -> 0.3 .. 1). */
   double sun = 1.0;

   MeshView(int w, int h) {
      this.w = w;
      this.h = h;
      this.img = new int[w * h];
      this.depth = new float[w * h];
   }

   void camera(double x, double y, double z, double yaw, double pitch, double fovDeg) {
      this.cx = x;
      this.cy = y;
      this.cz = z;
      double yw = Math.toRadians(yaw);
      double pt = Math.toRadians(pitch);
      this.fx = -Math.sin(yw) * Math.cos(pt);
      this.fy = -Math.sin(pt);
      this.fz = Math.cos(yw) * Math.cos(pt);
      this.rx = -Math.cos(yw);
      this.rz = -Math.sin(yw);
      // up = right x forward
      this.ux = 0.0 * this.fz - this.rz * this.fy;
      this.uy = this.rz * this.fx - this.rx * this.fz;
      this.uz = this.rx * this.fy - 0.0 * this.fx;
      this.focal = this.h / 2.0 / Math.tan(Math.toRadians(fovDeg) / 2.0);
   }

   void clear(int top, int bottom) {
      for (int y = 0; y < this.h; y++) {
         int a = (int)(top >> 16 & 255) + (int)(((bottom >> 16 & 255) - (top >> 16 & 255)) * (y / (double)this.h));
         int b = (int)(top >> 8 & 255) + (int)(((bottom >> 8 & 255) - (top >> 8 & 255)) * (y / (double)this.h));
         int c = (int)(top & 255) + (int)(((bottom & 255) - (top & 255)) * (y / (double)this.h));
         int col = 0xFF000000 | a << 16 | b << 8 | c;
         for (int x = 0; x < this.w; x++) {
            this.img[y * this.w + x] = col;
            this.depth[y * this.w + x] = Float.MAX_VALUE;
         }
      }
   }

   /** Draws the quads of a vertex buffer (11 floats per vertex, positions relative to the given origin). */
   void draw(FloatBuffer fb, int vertices, double ox, double oy, double oz, boolean blend) {
      float[] a = new float[vertices * 11];
      fb.position(0);
      fb.get(a, 0, Math.min(a.length, fb.limit()));
      for (int q = 0; q + 3 < vertices; q += 4) {
         double[][] p = new double[4][];
         boolean ok = true;
         for (int i = 0; i < 4; i++) {
            int o = (q + i) * 11;
            double dx = a[o] + ox - this.cx;
            double dy = a[o + 1] + oy - this.cy;
            double dz = a[o + 2] + oz - this.cz;
            double xc = dx * this.rx + dz * this.rz;
            double yc = dx * this.ux + dy * this.uy + dz * this.uz;
            double zc = dx * this.fx + dy * this.fy + dz * this.fz;
            if (zc < 0.05) {
               ok = false;
               break;
            }

            p[i] = new double[]{this.w / 2.0 + xc / zc * this.focal, this.h / 2.0 - yc / zc * this.focal, zc, a[o + 3], a[o + 4], a[o + 5], a[o + 6], a[o + 7], a[o + 8], Math.max(a[o + 9], a[o + 10] * 1.0F)};
         }

         if (!ok) {
            continue;
         }

         double area = (p[1][0] - p[0][0]) * (p[2][1] - p[0][1]) - (p[2][0] - p[0][0]) * (p[1][1] - p[0][1]);
         double area2 = (p[2][0] - p[0][0]) * (p[3][1] - p[0][1]) - (p[3][0] - p[0][0]) * (p[2][1] - p[0][1]);
         // screen y points down, so a counter-clockwise quad on the screen has a negative area here
         boolean front = area + area2 < 0.0;
         if (this.cull && !front) {
            continue;
         }

         this.tri(p[0], p[1], p[2], blend);
         this.tri(p[0], p[2], p[3], blend);
      }
   }

   private void tri(double[] a, double[] b, double[] c, boolean blend) {
      int minX = (int)Math.max(0, Math.floor(Math.min(a[0], Math.min(b[0], c[0]))));
      int maxX = (int)Math.min(this.w - 1, Math.ceil(Math.max(a[0], Math.max(b[0], c[0]))));
      int minY = (int)Math.max(0, Math.floor(Math.min(a[1], Math.min(b[1], c[1]))));
      int maxY = (int)Math.min(this.h - 1, Math.ceil(Math.max(a[1], Math.max(b[1], c[1]))));
      double den = (b[1] - c[1]) * (a[0] - c[0]) + (c[0] - b[0]) * (a[1] - c[1]);
      if (Math.abs(den) < 1e-12) {
         return;
      }

      for (int y = minY; y <= maxY; y++) {
         for (int x = minX; x <= maxX; x++) {
            double px = x + 0.5;
            double py = y + 0.5;
            double l0 = ((b[1] - c[1]) * (px - c[0]) + (c[0] - b[0]) * (py - c[1])) / den;
            double l1 = ((c[1] - a[1]) * (px - c[0]) + (a[0] - c[0]) * (py - c[1])) / den;
            double l2 = 1.0 - l0 - l1;
            if (l0 < -1e-6 || l1 < -1e-6 || l2 < -1e-6) {
               continue;
            }

            double iz = l0 / a[2] + l1 / b[2] + l2 / c[2];
            double z = 1.0 / iz;
            int idx = y * this.w + x;
            if (z >= this.depth[idx]) {
               continue;
            }

            double u = (l0 * a[3] / a[2] + l1 * b[3] / b[2] + l2 * c[3] / c[2]) * z;
            double v = (l0 * a[4] / a[2] + l1 * b[4] / b[2] + l2 * c[4] / c[2]) * z;
            int tx = Math.min(Tex.PX - 1, Math.max(0, (int)Math.floor(u * Tex.PX)));
            int ty = Math.min(Tex.PX - 1, Math.max(0, (int)Math.floor(v * Tex.PX)));
            int t = Tex.atlasPixels[ty * Tex.PX + tx];
            double alpha = (t >>> 24) / 255.0;
            double qa = (l0 * a[8] / a[2] + l1 * b[8] / b[2] + l2 * c[8] / c[2]) * z;
            alpha *= qa;
            if (alpha < (blend ? 0.02 : 0.5)) {
               continue;
            }

            double qr = (l0 * a[5] / a[2] + l1 * b[5] / b[2] + l2 * c[5] / c[2]) * z;
            double qg = (l0 * a[6] / a[2] + l1 * b[6] / b[2] + l2 * c[6] / c[2]) * z;
            double qb = (l0 * a[7] / a[2] + l1 * b[7] / b[2] + l2 * c[7] / c[2]) * z;
            double lt = (l0 * a[9] / a[2] + l1 * b[9] / b[2] + l2 * c[9] / c[2]) * z;
            double br = 0.28 + 0.72 * Math.min(1.0, lt * (0.4 + 0.6 * this.sun) / 0.97);
            double r = (t >> 16 & 255) * qr * br;
            double g = (t >> 8 & 255) * qg * br;
            double bl = (t & 255) * qb * br;
            if (blend) {
               int d = this.img[idx];
               r = r * alpha + (d >> 16 & 255) * (1.0 - alpha);
               g = g * alpha + (d >> 8 & 255) * (1.0 - alpha);
               bl = bl * alpha + (d & 255) * (1.0 - alpha);
            } else {
               this.depth[idx] = (float)z;
            }

            this.img[idx] = 0xFF000000 | clamp(r) << 16 | clamp(g) << 8 | clamp(bl);
         }
      }
   }

   private static int clamp(double v) {
      return v < 0.0 ? 0 : (v > 255.0 ? 255 : (int)v);
   }

   // ------------------------------------------------------------------ scenes
   /** A scene: fills a flat world; the camera is set by the scene (a look-at point and a distance), the command line can override. */
   interface Scene {
      void build(World w);

      /** x, y, z of the point to look at, yaw, pitch, distance */
      double[] view();
   }

   static World flatWorld(int radius, int ground) {
      World w = new World(1234, null);
      w.saveEnabled = false;
      for (int cz = -radius; cz <= radius; cz++) {
         for (int cx = -radius; cx <= radius; cx++) {
            Chunk c = new Chunk(cx, cz);
            for (int x = 0; x < 16; x++) {
               for (int z = 0; z < 16; z++) {
                  c.set(x, 0, z, Blocks.BEDROCK);
                  for (int y = 1; y < ground - 2; y++) {
                     c.set(x, y, z, Blocks.STONE);
                  }

                  c.set(x, ground - 2, z, Blocks.DIRT);
                  c.set(x, ground - 1, z, Blocks.DIRT);
                  c.set(x, ground, z, Blocks.GRASS);
               }
            }

            java.util.Arrays.fill(c.grassColor, 0x7CBD6B);
            java.util.Arrays.fill(c.foliageColor, 0x59AE30);
            c.computeHeightmap();
            w.chunks.put(World.key(cx, cz), c);
         }
      }

      return w;
   }

   static void relight(World w) {
      for (Chunk c : new ArrayList<>(w.chunks.values())) {
         w.lightChunk(c);
      }
   }

   BufferedImage render(World world, int ss) {
      BufferedImage out = new BufferedImage(this.w, this.h, BufferedImage.TYPE_INT_ARGB);
      out.setRGB(0, 0, this.w, this.h, this.img, 0, this.w);
      return out;
   }

   /** Meshes every section of every loaded chunk and draws them: opaque first, then the translucent parts back to front. */
   void drawWorld(World world) {
      List<Chunk> chunks = new ArrayList<>(world.chunks.values());
      List<Object[]> trans = new ArrayList<>();
      for (Chunk c : chunks) {
         for (int s = 0; s < 16; s++) {
            Mesher.Result r = Mesher.build(world, c, s, false);
            if (r.nO > 0) {
               this.draw(r.opaque, r.nO, c.cx * 16, s * 16, c.cz * 16, false);
            }

            if (r.nT > 0) {
               double mx = c.cx * 16 + 8 - this.cx;
               double mz = c.cz * 16 + 8 - this.cz;
               double my = s * 16 + 8 - this.cy;
               trans.add(new Object[]{r.trans, r.nT, c.cx * 16.0, s * 16.0, c.cz * 16.0, mx * mx + my * my + mz * mz});
            }
         }
      }

      trans.sort((p, q) -> Double.compare((Double)q[5], (Double)p[5]));
      for (Object[] t : trans) {
         this.draw((FloatBuffer)t[0], (Integer)t[1], (Double)t[2], (Double)t[3], (Double)t[4], true);
      }
   }

   public static void main(String[] args) throws Exception {
      Tex.initPixels();
      String out = args[0];
      String name = args[1];
      Scene scene = Scenes315.byName(name);
      if (scene == null) {
         System.out.println("unknown scene " + name + "; known: " + Scenes315.names());
         System.exit(2);
      }

      World world = flatWorld(2, 60);
      scene.build(world);
      relight(world);
      double[] v = scene.view().clone();
      if (args.length > 2) {
         v[3] = Double.parseDouble(args[2]);
      }

      if (args.length > 3) {
         v[4] = Double.parseDouble(args[3]);
      }

      if (args.length > 4) {
         v[5] = Double.parseDouble(args[4]);
      }

      int w = args.length > 5 ? Integer.parseInt(args[5]) : 960;
      int h = args.length > 6 ? Integer.parseInt(args[6]) : 540;
      if (args.length > 9) {
         v[0] = Double.parseDouble(args[7]);
         v[1] = Double.parseDouble(args[8]);
         v[2] = Double.parseDouble(args[9]);
      }

      int ss = 2;
      MeshView mv = new MeshView(w * ss, h * ss);
      double yaw = v[3];
      double pitch = v[4];
      double dist = v[5];
      double cxp = v[0] + Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)) * dist;
      double cyp = v[1] + Math.sin(Math.toRadians(pitch)) * dist;
      double czp = v[2] - Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)) * dist;
      mv.camera(cxp, cyp, czp, yaw, pitch, 60.0);
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

      ImageIO.write(small, "png", new File(out));
      System.out.println("wrote " + out);
   }
}
