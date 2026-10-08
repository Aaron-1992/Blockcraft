package bc;

import java.util.ArrayList;
import org.lwjgl.opengl.GL11;

/**
 * Update 3.10: everything the natural disasters draw (the rules are in {@link Disasters}). Immediate mode like the rest of the
 * entity drawing; positions are camera-relative (the modelview matrix only holds the view rotation).
 *
 * <ul>
 *   <li>{@link #drawDebris}: blocks torn out by a tornado, tumbling around the funnel and flying off (opaque, with the entities).</li>
 *   <li>{@link #drawSky}: the funnels (two translucent shells of rings that turn at different speeds, a dust skirt on the ground, a wall
 *   cloud made of stacked soft discs where the funnel hangs from) and the meteors (additive streaks with a glowing head), drawn after
 *   the clouds at the same place as the rain curtains. They are tinted like the weather clouds (night, sunset, lightning flashes).</li>
 * </ul>
 */
final class DisasterFx {
   private DisasterFx() {
   }

   static float clamp(float x) {
      return Math.max(0.0F, Math.min(1.0F, x));
   }

   static float smooth(float x) {
      float t = clamp(x);
      return t * t * (3.0F - 2.0F * t);
   }

   // ------------------------------------------------------------------ debris
   static void drawDebris(Renderer r, float partial) {
      World w = r.g.world;
      ArrayList<Disasters.Debris> list = w.disasters.debris;
      if (list.isEmpty()) {
         return;
      }

      GL11.glEnable(2929);
      for (Disasters.Debris d : list) {
         double x = d.px + (d.x - d.px) * partial;
         double y = d.py + (d.y - d.py) * partial;
         double z = d.pz + (d.z - d.pz) * partial;
         float bright = r.brightnessAt(w, x, y, z);
         GL11.glPushMatrix();
         GL11.glTranslated(x - r.camX, y - r.camY, z - r.camZ);
         GL11.glRotatef(d.rx + d.vrx * partial, 1.0F, 0.0F, 0.0F);
         GL11.glRotatef(d.ry + d.vry * partial, 0.0F, 1.0F, 0.0F);
         GL11.glRotatef(d.rz + d.vrz * partial, 0.0F, 0.0F, 1.0F);
         GL11.glScalef(0.9F, 0.9F, 0.9F);
         Renderer.drawBlockModel(d.id, bright);
         GL11.glPopMatrix();
      }
   }

   // ------------------------------------------------------------------ funnels and meteors
   static void drawSky(Renderer r, float partial) {
      Disasters ds = r.g.world.disasters;
      if (ds.tornadoes.isEmpty() && ds.meteors.isEmpty()) {
         return;
      }

      float[] tint = r.wfx.cloudTint(r.daylight, r.sunAngle);
      GL11.glDisable(3553);
      GL11.glDisable(3008);
      GL11.glDisable(2884);
      GL11.glEnable(2929);
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      GL11.glDepthMask(false);
      for (Disasters.Tornado t : ds.tornadoes) {
         drawTornado(r, t, partial, tint);
      }

      if (!ds.meteors.isEmpty()) {
         GL11.glBlendFunc(770, 1);
         for (Disasters.Meteor m : ds.meteors) {
            drawMeteor(r, m, partial);
         }

         GL11.glBlendFunc(770, 771);
      }

      GL11.glDepthMask(true);
      GL11.glDisable(3042);
      GL11.glEnable(3553);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
   }

   /** The shade of a funnel / cloud: the weather clouds' tint, but never quite black (a tornado at night is a darker shape in the dark). */
   static float shade(float[] tint, int c) {
      return Math.max(0.27F, tint[c]);
   }

   static void drawTornado(Renderer r, Disasters.Tornado t, float partial, float[] tint) {
      double cx = t.px + (t.x - t.px) * partial;
      double cz = t.pz + (t.z - t.pz) * partial;
      double gy = t.pGroundY + (t.groundY - t.pGroundY) * partial;
      float st = t.pStrength + (t.strength - t.pStrength) * partial;
      float spin = t.pSpin + (t.spin - t.pSpin) * partial;
      double time = t.age + partial;
      double dist = Math.hypot(cx - r.camX, cz - r.camZ);
      if (dist > 3500.0) {
         return;
      }

      double height = t.height;
      double top = gy + height;
      float formed = smooth(st * 1.15F);
      double bottom = top - height * (0.16 + 0.84 * formed);
      float cloudFade = Math.min(1.0F, (float)(time / 120.0)) * (t.age > t.life - 120 ? Math.max(0.0F, (t.life - t.age) / 120.0F) : 1.0F);
      float lr = shade(tint, 0);
      float lg = shade(tint, 1);
      float lb = shade(tint, 2);
      double ph1 = (t.seed & 1023) * 0.0061;
      double ph2 = (t.seed >> 10 & 1023) * 0.0061;

      // the funnel: rings from the cloud down to the ground (or to its tip while it is still forming)
      int rings = 22;
      int segs = 20;
      double[] ry = new double[rings + 1];
      double[] rr = new double[rings + 1];
      double[] rcx = new double[rings + 1];
      double[] rcz = new double[rings + 1];
      float[] rh = new float[rings + 1];
      for (int k = 0; k <= rings; k++) {
         double f = k / (double)rings;
         double y = bottom + (top - bottom) * f;
         double hf = Math.max(0.0, Math.min(1.0, (y - gy) / height));
         double rad = t.radiusAt(hf) * 1.2;
         rad *= (0.30 + 0.70 * smooth((float)(f * 2.2))) * (0.55 + 0.45 * formed);
         double sway = t.size * (0.6 + 5.0 * Math.pow(hf, 1.5));
         ry[k] = y;
         rr[k] = rad;
         rcx[k] = cx + Math.sin(time * 0.045 + hf * 3.3 + ph1) * sway;
         rcz[k] = cz + Math.cos(time * 0.037 + hf * 2.6 + ph2) * sway;
         rh[k] = (float)hf;
      }

      // wall cloud first: stacked soft discs, dark underneath
      double wr = t.size * 30.0;
      float[][] cloudCol = new float[][]{{0.12F, 0.12F, 0.15F}, {0.16F, 0.16F, 0.19F}, {0.21F, 0.21F, 0.24F}, {0.26F, 0.26F, 0.29F}};
      double[] cloudY = new double[]{top - 1.5, top + 1.5, top + 4.5, top + 8.0};
      double[] cloudR = new double[]{wr * 0.95, wr, wr * 0.78, wr * 0.52};
      for (int i = 0; i < 4; i++) {
         disc(r, cx, cz, cloudY[i], cloudR[i], new float[]{cloudCol[i][0] * lr * 1.6F, cloudCol[i][1] * lg * 1.6F, cloudCol[i][2] * lb * 1.6F}, 0.90F * cloudFade, 0.0F, t.seed * 0.001 + i * 1.7, time);
      }

      // two shells: the inner one is dense and turns fast, the outer one is a soft veil turning the other way
      for (int shell = 0; shell < 2; shell++) {
         double grow = shell == 0 ? 1.0 : 1.38;
         float alphaMul = shell == 0 ? 1.0F : 0.30F;
         float lighten = shell == 0 ? 1.0F : 1.3F;
         double dir = shell == 0 ? 1.0 : -0.6;
         GL11.glBegin(7);
         for (int k = 0; k < rings; k++) {
            for (int j = 0; j < segs; j++) {
               for (int c = 0; c < 4; c++) {
                  int kk = k + (c == 2 || c == 3 ? 1 : 0);
                  int jj = j + (c == 1 || c == 2 ? 1 : 0);
                  double a = (spin * dir * (1.7 - rh[kk]) + jj * Math.PI * 2.0 / segs) + kk * 0.35 * dir;
                  float hf = rh[kk];
                  float dust = Math.max(0.0F, 1.0F - hf * 3.0F) * 0.85F * formed;
                  float gray = 0.46F - 0.22F * hf;
                  float streak = 0.80F + 0.20F * (float)Math.sin(jj * 2.1 + kk * 0.7 + spin * 1.7 * dir) + 0.06F * (float)Math.sin(jj * 5.3 - spin * 2.9);
                  float cr = (gray + (0.55F - gray) * dust) * streak * lighten * lr;
                  float cg = (gray + (0.45F - gray) * dust) * streak * lighten * lg;
                  float cb = (gray * 1.04F + (0.32F - gray * 1.04F) * dust) * streak * lighten * lb;
                  float al = Math.min(0.92F, (0.88F - 0.20F * hf) * (0.55F + 0.45F * smooth(kk / (float)rings * 3.0F)) * smooth(formed * 1.4F + 0.15F) * alphaMul);
                  GL11.glColor4f(cr, cg, cb, al);
                  GL11.glVertex3d(rcx[kk] + Math.cos(a) * rr[kk] * grow - r.camX, ry[kk] - r.camY, rcz[kk] + Math.sin(a) * rr[kk] * grow - r.camZ);
               }
            }
         }

         GL11.glEnd();
      }

      // the dust skirt around the foot
      if (st > 0.5F) {
         float sk = clamp((st - 0.5F) * 2.0F);
         double[] sy = new double[]{gy + 0.2, gy + 2.5, gy + 7.0};
         double[] sr = new double[]{t.size * 6.5, t.size * 4.4, t.size * 2.8};
         float[] sa = new float[]{0.40F * sk, 0.24F * sk, 0.0F};
         GL11.glBegin(7);
         for (int i = 0; i < 2; i++) {
            for (int j = 0; j < segs; j++) {
               for (int c = 0; c < 4; c++) {
                  int ii = i + (c == 2 || c == 3 ? 1 : 0);
                  int jj = j + (c == 1 || c == 2 ? 1 : 0);
                  double a = spin * -0.5 + jj * Math.PI * 2.0 / segs;
                  double wob = 1.0 + 0.18 * Math.sin(jj * 1.9 + time * 0.07 + ii);
                  GL11.glColor4f(0.52F * lr, 0.43F * lg, 0.31F * lb, sa[ii]);
                  GL11.glVertex3d(cx + Math.cos(a) * sr[ii] * wob - r.camX, sy[ii] - r.camY, cz + Math.sin(a) * sr[ii] * wob - r.camZ);
               }
            }
         }

         GL11.glEnd();
      }
   }

   /** A soft, slightly irregular disc: opaque in the middle, fading out to the edge. */
   static void disc(Renderer r, double cx, double cz, double y, double radius, float[] c, float centerAlpha, float edgeAlpha, double seed, double time) {
      int n = 28;
      GL11.glBegin(6);
      GL11.glColor4f(Math.min(1.0F, c[0]), Math.min(1.0F, c[1]), Math.min(1.0F, c[2]), centerAlpha);
      GL11.glVertex3d(cx - r.camX, y - r.camY, cz - r.camZ);
      for (int i = 0; i <= n; i++) {
         double a = i * Math.PI * 2.0 / n;
         double wob = 1.0 + 0.13 * Math.sin(a * 3.0 + seed) + 0.08 * Math.sin(a * 5.0 - seed * 1.3 + time * 0.01) + 0.05 * Math.sin(a * 9.0 + seed * 2.0);
         double rad = radius * wob;
         GL11.glColor4f(Math.min(1.0F, c[0]), Math.min(1.0F, c[1]), Math.min(1.0F, c[2]), edgeAlpha);
         GL11.glVertex3d(cx + Math.cos(a) * rad - r.camX, y - 1.2 - r.camY, cz + Math.sin(a) * rad - r.camZ);
      }

      GL11.glEnd();
   }

   static void drawMeteor(Renderer r, Disasters.Meteor m, float partial) {
      double hx = m.px + (m.x - m.px) * partial;
      double hy = m.py + (m.y - m.py) * partial;
      double hz = m.pz + (m.z - m.pz) * partial;
      double dx = hx - r.camX;
      double dy = hy - r.camY;
      double dz = hz - r.camZ;
      double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
      if (dist > 3500.0 || dist < 1.0) {
         return;
      }

      // the tail: the head, the positions of the last ticks, then the same direction on to a long streak
      int pts = 20;
      double[] px = new double[pts];
      double[] py = new double[pts];
      double[] pz = new double[pts];
      px[0] = hx;
      py[0] = hy;
      pz[0] = hz;
      int have = 1;
      for (int i = 0; i < m.trailLen && have < pts; i++) {
         px[have] = m.trail[i * 3];
         py[have] = m.trail[i * 3 + 1];
         pz[have] = m.trail[i * 3 + 2];
         have++;
      }

      while (have < pts) {
         px[have] = px[have - 1] - m.vx * 1.1;
         py[have] = py[have - 1] - m.vy * 1.1;
         pz[have] = pz[have - 1] - m.vz * 1.1;
         have++;
      }

      double base = Math.max(0.55 * m.size, dist * 0.0055);
      for (int i = 0; i + 1 < pts; i++) {
         double sx = px[i + 1] - px[i];
         double sy = py[i + 1] - py[i];
         double sz = pz[i + 1] - pz[i];
         double vx = px[i] - r.camX;
         double vy = py[i] - r.camY;
         double vz = pz[i] - r.camZ;
         // sideways = segment x line of sight
         double ox = sy * vz - sz * vy;
         double oy = sz * vx - sx * vz;
         double oz = sx * vy - sy * vx;
         double ol = Math.sqrt(ox * ox + oy * oy + oz * oz);
         if (ol < 1.0E-9) {
            continue;
         }

         ox /= ol;
         oy /= ol;
         oz /= ol;
         float f0 = 1.0F - i / (float)(pts - 1);
         float f1 = 1.0F - (i + 1) / (float)(pts - 1);
         double w0 = base * Math.pow(f0, 1.3);
         double w1 = base * Math.pow(f1, 1.3);
         GL11.glBegin(7);
         GL11.glColor4f(1.0F, 0.35F + 0.6F * f0, 0.08F + 0.65F * f0 * f0, 0.95F * f0);
         GL11.glVertex3d(px[i] - ox * w0 - r.camX, py[i] - oy * w0 - r.camY, pz[i] - oz * w0 - r.camZ);
         GL11.glVertex3d(px[i] + ox * w0 - r.camX, py[i] + oy * w0 - r.camY, pz[i] + oz * w0 - r.camZ);
         GL11.glColor4f(1.0F, 0.35F + 0.6F * f1, 0.08F + 0.65F * f1 * f1, 0.95F * f1);
         GL11.glVertex3d(px[i + 1] + ox * w1 - r.camX, py[i + 1] + oy * w1 - r.camY, pz[i + 1] + oz * w1 - r.camZ);
         GL11.glVertex3d(px[i + 1] - ox * w1 - r.camX, py[i + 1] - oy * w1 - r.camY, pz[i + 1] - oz * w1 - r.camZ);
         GL11.glEnd();
      }

      // the glowing head: a bright core and two soft halos (billboards that face the player)
      double yaw = Math.toRadians(r.viewYaw);
      double rx = -Math.cos(yaw);
      double rz = -Math.sin(yaw);
      double s1 = Math.max(1.1 * m.size, dist * 0.010);
      double[] sizes = new double[]{s1, s1 * 2.6, s1 * 6.0};
      float[][] cols = new float[][]{{1.0F, 0.97F, 0.82F, 1.0F}, {1.0F, 0.62F, 0.2F, 0.50F}, {1.0F, 0.35F, 0.1F, 0.16F}};
      for (int i = 0; i < 3; i++) {
         double s = sizes[i];
         GL11.glBegin(6);
         GL11.glColor4f(cols[i][0], cols[i][1], cols[i][2], cols[i][3]);
         GL11.glVertex3d(dx, dy, dz);
         for (int k = 0; k <= 14; k++) {
            double a = k * Math.PI * 2.0 / 14.0;
            double ca = Math.cos(a) * s;
            double sa = Math.sin(a) * s;
            GL11.glColor4f(cols[i][0], cols[i][1], cols[i][2], 0.0F);
            GL11.glVertex3d(dx + rx * ca, dy + sa, dz + rz * ca);
         }

         GL11.glEnd();
      }
   }
}
