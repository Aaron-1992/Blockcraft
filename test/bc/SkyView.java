package bc;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Development tool (no OpenGL needed): paints the sky of {@link Sky} - the gradient, the glow, the sun, the moon with its phase, the stars - from the
 * same functions the game uses, over a flat green plain that disappears into the fog, so that the colours of a time of day can be judged
 * without a graphics card.
 *
 * <pre>
 *   java -cp ... bc.SkyView out.png tick yaw pitch [width height fov [overcast]]
 *   java -cp ... bc.SkyView out.png sheet sun|anti [pitch]       12 times of a day in one picture
 * </pre>
 * tick = time of day (0 sunrise, 6000 noon, 12000 sunset, 18000 midnight); yaw 0 looks along +z, -90 east (where the sun rises), 90 west.
 */
public final class SkyView {
   static int[] sunTex;
   static int[] glowTex;

   static float[] sample(int[] tex, int n, float u, float v) {
      u = Math.max(0, Math.min(0.9999F, u)) * n - 0.5F;
      v = Math.max(0, Math.min(0.9999F, v)) * n - 0.5F;
      int x0 = (int)Math.floor(u);
      int y0 = (int)Math.floor(v);
      float fx = u - x0;
      float fy = v - y0;
      float[] out = new float[4];
      for (int k = 0; k < 4; k++) {
         int x = Math.max(0, Math.min(n - 1, x0 + (k & 1)));
         int y = Math.max(0, Math.min(n - 1, y0 + (k >> 1)));
         float w = ((k & 1) == 0 ? 1 - fx : fx) * ((k >> 1) == 0 ? 1 - fy : fy);
         int p = tex[y * n + x];
         out[0] += (p >> 16 & 255) / 255.0F * w;
         out[1] += (p >> 8 & 255) / 255.0F * w;
         out[2] += (p & 255) / 255.0F * w;
         out[3] += (p >>> 24) / 255.0F * w;
      }

      return out;
   }

   /** Renders one view into img at (ox, oy) with the given size. */
   static void view(BufferedImage img, int ox, int oy, int w, int h, long tick, double yawDeg, double pitchDeg, double fovDeg, float overcast) {
      float sunAngle = (float)(Math.PI * 2.0 * ((tick % 24000L) - 6000.0) / 24000.0);
      float[] b = new float[8];
      Sky.base(sunAngle, b);
      float[] zen = {b[0], b[1], b[2]};
      float[] hor = {b[3], b[4], b[5]};
      if (overcast > 0.0F) {
         float lt = 0.3F * zen[0] + 0.59F * zen[1] + 0.11F * zen[2];
         float lf = 0.3F * hor[0] + 0.59F * hor[1] + 0.11F * hor[2];
         float k = Math.min(0.92F, overcast * 0.92F);
         zen = new float[]{Sky.lerp(zen[0], lt * 0.66F, k), Sky.lerp(zen[1], lt * 0.71F, k), Sky.lerp(zen[2], lt * 0.80F, k)};
         hor = new float[]{Sky.lerp(hor[0], lf * 0.80F, k), Sky.lerp(hor[1], lf * 0.84F, k), Sky.lerp(hor[2], lf * 0.90F, k)};
      }

      float atten = 1.0F - 0.85F * overcast;
      float[] s = {(float)-Math.sin(sunAngle), (float)Math.cos(sunAngle), 0.0F};
      float night = 1.0F - b[6];
      int phase = Sky.moonPhase(tick);
      float lit = Sky.moonLit(phase);
      int[] moon = Sky.moonPixels(phase);
      if (sunTex == null) {
         sunTex = Sky.sunPixels();
         glowTex = Sky.glowPixels();
      }

      double yaw = Math.toRadians(yawDeg);
      double pitch = Math.toRadians(pitchDeg);
      double[] f = {-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch)};
      double[] rt = {-Math.cos(yaw), 0.0, -Math.sin(yaw)};
      double[] up = {rt[1] * f[2] - rt[2] * f[1], rt[2] * f[0] - rt[0] * f[2], rt[0] * f[1] - rt[1] * f[0]};
      // up must point up: flip if needed
      if (up[1] < 0) {
         up[0] = -up[0];
         up[1] = -up[1];
         up[2] = -up[2];
      }

      double tanH = Math.tan(Math.toRadians(fovDeg) / 2.0);
      double aspect = (double)w / h;
      float[] col = new float[3];
      float[] glow = new float[3];
      float[] z = zen;
      float[] hh = hor;
      float tw = Sky.twilight(s[1]);
      float sunVis = 0.18F + 0.82F * atten;
      float sunUpFade = Sky.clamp((s[1] + 0.12F) / 0.20F);
      Sky.glowColor(s[1], glow);
      float halo = (0.20F + 0.22F * tw) * sunUpFade * sunVis;
      float hs = 120.0F + 70.0F * tw;
      float ss = 30.0F * (1.0F + 0.35F * tw);
      float nightF = Sky.clamp(night * 1.15F);
      double ca = Math.cos(sunAngle);
      double sa = Math.sin(sunAngle);
      for (int py = 0; py < h; py++) {
         for (int px = 0; px < w; px++) {
            double nx = ((px + 0.5) / w * 2.0 - 1.0) * tanH * aspect;
            double ny = (1.0 - (py + 0.5) / h * 2.0) * tanH;
            double dx = f[0] + rt[0] * nx + up[0] * ny;
            double dy = f[1] + rt[1] * nx + up[1] * ny;
            double dz = f[2] + rt[2] * nx + up[2] * ny;
            double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
            dx /= len;
            dy /= len;
            dz /= len;
            Sky.color(col, (float)dx, (float)dy, (float)dz, z, hh, s, atten, night, lit);
            float r = col[0];
            float g = col[1];
            float bl = col[2];
            if (overcast > 0.05F) {
               float lum = 0.3F * r + 0.59F * g + 0.11F * bl;
               float q = Math.min(0.85F, overcast * 0.9F);
               r = Sky.lerp(r, lum * 0.62F, q);
               g = Sky.lerp(g, lum * 0.67F, q);
               bl = Sky.lerp(bl, lum * 0.76F, q);
            }

            // the sky frame: turned about z by the sun's angle (inverse)
            double qx = dx * ca + dy * sa;
            double qy = -dx * sa + dy * ca;
            double qz = dz;
            if (qy > 0.05 && sunUpFade > 0) {
               double t = 100.0 / qy;
               double u = qx * t;
               double v = qz * t;
               float[] hl = sample(glowTex, 64, (float)(u / (2 * hs) + 0.5), (float)(v / (2 * hs) + 0.5));
               if (Math.abs(u) < hs && Math.abs(v) < hs) {
                  r += glow[0] * hl[3] * halo;
                  g += glow[1] * 0.92F * hl[3] * halo;
                  bl += glow[2] * 0.85F * hl[3] * halo;
               }

               double hs2 = hs * 0.45;
               if (Math.abs(u) < hs2 && Math.abs(v) < hs2) {
                  float[] h2 = sample(glowTex, 64, (float)(u / (2 * hs2) + 0.5), (float)(v / (2 * hs2) + 0.5));
                  r += glow[0] * h2[3] * halo * 0.7F;
                  g += glow[1] * h2[3] * halo * 0.7F;
                  bl += glow[2] * 0.9F * h2[3] * halo * 0.7F;
               }

               if (Math.abs(u) < ss && Math.abs(v) < ss) {
                  float[] st = sample(sunTex, 32, (float)(u / (2 * ss) + 0.5), (float)(v / (2 * ss) + 0.5));
                  float sv = sunVis * sunUpFade * st[3];
                  r += st[0] * sv * (1.0F - 0.10F * tw);
                  g += st[1] * sv * (1.0F - 0.36F * tw);
                  bl += st[2] * sv * (1.0F - 0.62F * tw);
               }
            }

            if (qy < -0.05 && nightF > 0.02F) {
               double t = -100.0 / qy;
               double u = qx * t;
               double v = qz * t;
               if (Math.abs(u) < 90 && Math.abs(v) < 90) {
                  float[] hl = sample(glowTex, 64, (float)(u / 180 + 0.5), (float)(v / 180 + 0.5));
                  float a = 0.20F * nightF * (0.25F + 0.75F * lit) * (1.0F - 0.8F * overcast) * hl[3];
                  r += 0.55F * a;
                  g += 0.65F * a;
                  bl += 1.0F * a;
               }

               if (Math.abs(u) < 19 && Math.abs(v) < 19) {
                  int mx = (int)Math.max(0, Math.min(31, (u / 38 + 0.5) * 32));
                  int my = (int)Math.max(0, Math.min(31, (v / 38 + 0.5) * 32));
                  int p = moon[my * 32 + mx];
                  float a = (p >>> 24) / 255.0F;
                  if (a > 0.0F) {
                     r = Sky.lerp(r, (p >> 16 & 255) / 255.0F, a);
                     g = Sky.lerp(g, (p >> 8 & 255) / 255.0F, a);
                     bl = Sky.lerp(bl, (p & 255) / 255.0F, a);
                  }
               }
            }

            if (dy < 0.0) {
               // a plain with trees: the fog colour (with the sun's glow, like the shader) at the horizon, green nearer
               double dist = Math.min(1.0, (-dy) * 7.0);
               float fogGlow = 0.0F;
               float cs = (float)(dx * s[0] + dy * s[1] + dz * s[2]);
               float ga = Sky.glowAmount(s[1], atten);
               if (ga > 0.0F && cs > 0.0F) {
                  fogGlow = (float)Math.pow(cs, 5.0) * Sky.glowStrength(s[1]) * ga;
               }

               float fr = hh[0] + fogGlow * glow[0];
               float fg = hh[1] + fogGlow * glow[1];
               float fb = hh[2] + fogGlow * glow[2];
               float day = b[6];
               float light = 0.04F + 0.96F * day;
               float gr = 0.28F * light;
               float gg = 0.50F * light;
               float gb = 0.16F * light;
               float k = (float)(dist * dist);
               r = Sky.lerp(fr, gr, k);
               g = Sky.lerp(fg, gg, k);
               bl = Sky.lerp(fb, gb, k);
            }

            img.setRGB(ox + px, oy + py, rgb(r, g, bl));
         }
      }

      // the stars
      if (night > 0.05F) {
         float base = night * night * (1.0F - 0.92F * overcast);
         for (int i = 0; i < Sky.STARS; i++) {
            float bx = Sky.STAR[i * 7];
            float by = Sky.STAR[i * 7 + 1];
            float bz = Sky.STAR[i * 7 + 2];
            double wx = (bx * ca - by * sa) / 150.0;
            double wy = (bx * sa + by * ca) / 150.0;
            double wz = bz / 150.0;
            float horizon = Sky.smooth(-0.02F, 0.22F, (float)wy);
            if (horizon <= 0.0F) {
               continue;
            }

            double fz = wx * f[0] + wy * f[1] + wz * f[2];
            if (fz <= 0.05) {
               continue;
            }

            double sx = (wx * rt[0] + wy * rt[1] + wz * rt[2]) / fz / (tanH * aspect);
            double sy = (wx * up[0] + wy * up[1] + wz * up[2]) / fz / tanH;
            int px = (int)((sx * 0.5 + 0.5) * w);
            int py = (int)((0.5 - sy * 0.5) * h);
            float bright = Sky.STAR[i * 7 + 3];
            int size = bright > 0.80F ? 3 : (bright > 0.50F ? 2 : 1);
            float a = base * bright * 0.89F * horizon;
            Sky.starColor(Sky.STAR[i * 7 + 4], col);
            for (int dyy = 0; dyy < size; dyy++) {
               for (int dxx = 0; dxx < size; dxx++) {
                  int qx = px + dxx - size / 2;
                  int qy = py + dyy - size / 2;
                  if (qx >= 0 && qy >= 0 && qx < w && qy < h) {
                     int p = img.getRGB(ox + qx, oy + qy);
                     float r = Sky.lerp((p >> 16 & 255) / 255.0F, col[0], a);
                     float g = Sky.lerp((p >> 8 & 255) / 255.0F, col[1], a);
                     float bl = Sky.lerp((p & 255) / 255.0F, col[2], a);
                     img.setRGB(ox + qx, oy + qy, rgb(r, g, bl));
                  }
               }
            }
         }
      }
   }

   static int rgb(float r, float g, float b) {
      int ir = Math.max(0, Math.min(255, (int)(r * 255.0F + 0.5F)));
      int ig = Math.max(0, Math.min(255, (int)(g * 255.0F + 0.5F)));
      int ib = Math.max(0, Math.min(255, (int)(b * 255.0F + 0.5F)));
      return ir << 16 | ig << 8 | ib;
   }

   public static void main(String[] a) throws Exception {
      Tex.initPixels();
      if (a[1].equals("sheet")) {
         boolean sun = a[2].equals("sun");
         double pitch = a.length > 3 ? Double.parseDouble(a[3]) : 8.0;
         long[] ticks = {0, 400, 1200, 3000, 6000, 10000, 11300, 12000, 12600, 13500, 15000, 18000};
         int cw = 480;
         int ch = 270;
         BufferedImage img = new BufferedImage(cw * 4, ch * 3, BufferedImage.TYPE_INT_RGB);
         for (int i = 0; i < ticks.length; i++) {
            long t = ticks[i];
            // look towards the sun (east before noon, west after) or away from it
            double yaw = t < 6000 || t >= 18000 ? -90.0 : 90.0;
            if (!sun) {
               yaw = -yaw;
            }

            view(img, i % 4 * cw, i / 4 * ch, cw, ch, t, yaw, pitch, 80.0, 0.0F);
         }

         ImageIO.write(img, "png", new File(a[0]));
         System.out.println("wrote " + a[0]);
         return;
      }

      long tick = Long.parseLong(a[1]);
      double yaw = Double.parseDouble(a[2]);
      double pitch = Double.parseDouble(a[3]);
      int w = a.length > 4 ? Integer.parseInt(a[4]) : 800;
      int h = a.length > 5 ? Integer.parseInt(a[5]) : 450;
      double fov = a.length > 6 ? Double.parseDouble(a[6]) : 80.0;
      float overcast = a.length > 7 ? Float.parseFloat(a[7]) : 0.0F;
      BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
      view(img, 0, 0, w, h, tick, yaw, pitch, fov, overcast);
      ImageIO.write(img, "png", new File(a[0]));
      System.out.println("wrote " + a[0]);
   }
}
