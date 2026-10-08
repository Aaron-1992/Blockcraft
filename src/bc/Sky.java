package bc;

import java.util.Random;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

/**
 * Update 3.16: the sky. The colours are pure functions of the sun's height and of the direction you look in (so the pictures can be made
 * without a graphics card: see {@code SkyView}), the drawing is immediate mode and the same in the plain and the shader renderer.
 *
 * <ul>
 * <li>{@link #base} - the colours of a clear sky for an angle of the sun: zenith, horizon (= the fog colour) and the share of daylight.</li>
 * <li>{@link #color} - the colour of the sky in one direction: the gradient, the glow round the sun (the same glow the fog of the terrain
 * has, so the horizon and the far hills meet without a seam), the belt of orange over the horizon on the sun's side at dusk and dawn, the
 * pink arch and the blue shadow of the earth on the other side, the pale light of the moon.</li>
 * <li>{@link #draw} - the dome, the stars (1,800 of them, of different brightness, tint and size, twinkling, with a band of the Milky Way),
 * the glow round the sun and the moon, the sun, and the moon with its eight phases.</li>
 * </ul>
 */
final class Sky {
   private Sky() {
   }

   static float clamp(float v) {
      return v < 0.0F ? 0.0F : (v > 1.0F ? 1.0F : v);
   }

   static float lerp(float a, float b, float t) {
      return a + (b - a) * t;
   }

   static float smooth(float e0, float e1, float x) {
      float t = clamp((x - e0) / (e1 - e0));
      return t * t * (3.0F - 2.0F * t);
   }

   /** 1 when the sun stands on the horizon, 0 when it is high above or deep below it (e = sine of its elevation). */
   static float twilight(float e) {
      return 1.0F - smooth(0.0F, 0.30F, Math.abs(e));
   }

   /** How much of the glow round the sun there is: it goes on a little after the sun has set. (The terrain's fog uses the same number.) */
   static float glowAmount(float e, float atten) {
      return clamp((e + 0.10F) / 0.30F) * atten;
   }

   /** The strength of the glow in the fog and the sky (at the point right at the sun's side; the shader has the same formula). */
   static float glowStrength(float e) {
      return 0.08F + 0.20F * twilight(e);
   }

   /** The colour of that glow: pale warm by day, orange at dusk and dawn. */
   static void glowColor(float e, float[] out) {
      float tw = twilight(e);
      out[0] = 1.0F;
      out[1] = 0.80F - 0.26F * tw;
      out[2] = 0.55F - 0.26F * tw;
   }

   // ------------------------------------------------------------------ the colours of a clear sky
   /**
    * o[0..2] zenith, o[3..5] horizon (the fog), o[6] the share of daylight (0 night, 1 day), o[7] the sine of the sun's elevation.
    * The numbers of the day and of the night are the ones the game has always had; what is new is the dusk.
    */
   static void base(float sunAngle, float[] o) {
      float e = (float)Math.cos(sunAngle);
      float day = clamp(0.5F + e * 1.3F);
      o[0] = lerp(0.01F, 0.40F, day);
      o[1] = lerp(0.015F, 0.60F, day);
      o[2] = lerp(0.04F, 0.98F, day);
      o[3] = lerp(0.02F, 0.76F, day);
      o[4] = lerp(0.025F, 0.85F, day);
      o[5] = lerp(0.05F, 1.0F, day);
      float tw = twilight(e);
      if (tw > 0.0F) {
         o[3] = lerp(o[3], 1.0F, tw * 0.55F);
         o[4] = lerp(o[4], 0.52F, tw * 0.55F);
         o[5] = lerp(o[5], 0.30F, tw * 0.55F);
         o[0] = lerp(o[0], 0.40F, tw * 0.16F);
         o[1] = lerp(o[1], 0.30F, tw * 0.16F);
         o[2] = lerp(o[2], 0.58F, tw * 0.16F);
      }

      o[6] = day;
      o[7] = e;
   }

   private static void mixTo(float[] c, float r, float g, float b, float k) {
      k = clamp(k);
      c[0] = lerp(c[0], r, k);
      c[1] = lerp(c[1], g, k);
      c[2] = lerp(c[2], b, k);
   }

   /**
    * The colour of the sky in the direction (dx, dy, dz) (a unit vector, y up).
    *
    * @param z   zenith colour (3 floats)
    * @param h   horizon colour = fog colour (3 floats)
    * @param s   direction of the sun (unit vector); s[1] is the sine of its elevation
    * @param atten how much of the sun shows through the weather (1 = clear sky, 0 = it is hidden)
    * @param night how dark it is (1 = night, 0 = day): the moon's light is only there at night
    * @param moonBright 0..1 how much of the moon is lit
    */
   static void color(float[] out, float dx, float dy, float dz, float[] z, float[] h, float[] s, float atten, float night, float moonBright) {
      float e = s[1];
      float t = dy <= 0.0F ? 0.0F : (float)Math.pow(dy, 0.6);
      out[0] = lerp(h[0], z[0], t);
      out[1] = lerp(h[1], z[1], t);
      out[2] = lerp(h[2], z[2], t);
      float cs = dx * s[0] + dy * e + dz * s[2];
      // the broad glow round the sun: the fog of the terrain has the same one, so that the far hills melt into the sky in the sun's direction
      float ga = glowAmount(e, atten);
      if (ga > 0.0F && cs > 0.0F) {
         float g = (float)Math.pow(cs, 5.0) * glowStrength(e) * ga;
         float tw0 = twilight(e);
         out[0] += g;
         out[1] += g * (0.80F - 0.26F * tw0);
         out[2] += g * (0.55F - 0.26F * tw0);
      }

      float tw = twilight(e) * atten;
      float lift = smooth(0.0F, 0.06F, dy); // right at the horizon the sky is the colour of the fog, so that the far hills do not show a seam
      if (tw > 0.002F && dy > 0.0F) {
         float hl = (float)Math.sqrt(dx * dx + dz * dz);
         float sl = (float)Math.sqrt(s[0] * s[0] + s[2] * s[2]);
         float az = hl > 1.0E-4F && sl > 1.0E-4F ? (dx * s[0] + dz * s[2]) / (hl * sl) : 0.0F;
         float side = az > 0.0F ? az : 0.0F;
         float anti = az < 0.0F ? -az : 0.0F;
         // a belt of orange over the horizon, strongest towards the sun
         float belt = tw * lift * (float)Math.exp(-(dy * dy) / (2.0F * 0.17F * 0.17F)) * (0.20F + 0.80F * side * side);
         mixTo(out, 1.0F, 0.50F, 0.20F, belt * 0.60F);
         // the pink arch opposite the sun, with the blue shadow of the earth under it
         float arch = tw * lift * anti * (float)Math.exp(-((dy - 0.20F) * (dy - 0.20F)) / (2.0F * 0.11F * 0.11F));
         mixTo(out, 0.80F, 0.52F, 0.72F, arch * 0.38F);
         float shade = tw * lift * anti * (float)Math.exp(-((dy - 0.03F) * (dy - 0.03F)) / (2.0F * 0.05F * 0.05F));
         mixTo(out, 0.20F, 0.26F, 0.46F, shade * 0.40F);
      }

      if (night > 0.01F) {
         // moonlight: the sky is a little paler round the moon (the moon is opposite the sun)
         float cm = -cs;
         if (cm > 0.0F) {
            float m = (float)Math.pow(cm, 7.0) * 0.10F * night * moonBright * lift;
            out[0] += 0.50F * m;
            out[1] += 0.62F * m;
            out[2] += 0.95F * m;
         }
      }
   }

   // ------------------------------------------------------------------ the moon
   /** Which of the eight phases the moon has on this day (0 = full moon, 4 = new moon, as in Minecraft). */
   static int moonPhase(long dayTime) {
      return (int)(Math.floorDiv(dayTime, 24000L) & 7L);
   }

   /** How much of the disc is lit in a phase (1 = full, 0 = new). */
   static float moonLit(int phase) {
      return (1.0F + (float)Math.cos(phase * Math.PI / 4.0)) * 0.5F;
   }

   /**
    * The moon as a 32 x 32 picture: a pixelated disc with seas and craters, lit from the side the phase says (0 full, 1 waning gibbous,
    * 2 last quarter, 3 waning crescent, 4 new, 5 waxing crescent, 6 first quarter, 7 waxing gibbous). What the sun does not light is
    * almost black (earthshine).
    */
   static int[] moonPixels(int phase) {
      final int n = 32;
      int[] px = new int[n * n];
      double alpha = phase * Math.PI / 4.0;
      // light comes from this direction (in the moon's own frame: x to the right, z towards us)
      double lx = -Math.sin(alpha);
      double lz = Math.cos(alpha);
      Random r = new Random(888L);
      // dark seas: fixed blobs (in a 2 x 2 pixel grid, so the moon looks blocky)
      double[][] seas = {{-0.35, -0.30, 0.32}, {0.30, -0.45, 0.22}, {0.10, 0.10, 0.28}, {-0.30, 0.38, 0.20}, {0.52, 0.30, 0.17}};
      for (int y = 0; y < n; y++) {
         for (int x = 0; x < n; x++) {
            double u = ((x / 2) * 2 + 1 - (n - 1) / 2.0 - 0.5) / (n / 2.0 - 1.2);
            double v = ((y / 2) * 2 + 1 - (n - 1) / 2.0 - 0.5) / (n / 2.0 - 1.2);
            double rr = u * u + v * v;
            if (rr > 1.0) {
               continue;
            }

            double zz = Math.sqrt(1.0 - rr);
            double lit = u * lx + zz * lz;
            double sea = 0.0;
            for (double[] s : seas) {
               double d = Math.hypot(u - s[0], v - s[1]);
               sea = Math.max(sea, 1.0 - d / s[2]);
            }

            double grain = ((x * 7 + y * 13 + (x / 2) * (y / 2) * 3) % 5) * 0.012;
            double base = 0.93 - 0.20 * Math.max(0.0, Math.min(1.0, sea * 1.6)) - grain - 0.10 * rr;
            double shade;
            if (lit > 0.10) {
               shade = 1.0;
            } else if (lit > 0.0) {
               shade = 0.62 + 5.0 * lit * 0.38;
            } else if (lit > -0.10) {
               shade = 0.30 + 3.2 * (lit + 0.10);
            } else {
               shade = 0.05;
            }

            double k = base * shade;
            int cr = (int)Math.max(0, Math.min(255, 232 * k + 6));
            int cg = (int)Math.max(0, Math.min(255, 236 * k + 8));
            int cb = (int)Math.max(0, Math.min(255, 250 * k + 14));
            px[y * n + x] = 0xFF000000 | cr << 16 | cg << 8 | cb;
         }
      }

      return px;
   }

   /** The sun: a bright square with a one pixel soft edge (the halo round it is the glow, which is round). 32 x 32. */
   static int[] sunPixels() {
      final int n = 32;
      int[] px = new int[n * n];
      for (int y = 0; y < n; y++) {
         for (int x = 0; x < n; x++) {
            double d = Math.max(Math.abs(x - 15.5), Math.abs(y - 15.5));
            double a = d <= 8.0 ? 1.0 : (d <= 9.5 ? 1.0 - (d - 8.0) / 1.5 : 0.0);
            if (a > 0.0) {
               px[y * n + x] = (int)(255.0 * a) << 24 | 0xFFFAD0;
            }
         }
      }

      return px;
   }

   /** A soft round glow: white, the alpha falls off smoothly from the middle (64 x 64). */
   static int[] glowPixels() {
      final int n = 64;
      int[] px = new int[n * n];
      for (int y = 0; y < n; y++) {
         for (int x = 0; x < n; x++) {
            double u = (x + 0.5 - n / 2.0) / (n / 2.0);
            double v = (y + 0.5 - n / 2.0) / (n / 2.0);
            double d = Math.sqrt(u * u + v * v);
            double a = d >= 1.0 ? 0.0 : Math.pow(1.0 - d, 2.2);
            int al = (int)Math.max(0, Math.min(255, 255.0 * a));
            px[y * n + x] = al << 24 | 0xFFFFFF;
         }
      }

      return px;
   }

   // ------------------------------------------------------------------ the stars
   static final int STARS = 1800;
   /** x, y, z (on a sphere of radius 150), brightness 0..1, tint 0..1 (0 blue-white, 1 orange), twinkle phase, twinkle speed. */
   static final float[] STAR = new float[STARS * 7];

   static {
      Random r = new Random(7777L);
      for (int i = 0; i < STARS; i++) {
         double x;
         double y;
         double z;
         if (i < 1300) {
            // all over the sky
            double a = r.nextDouble() * Math.PI * 2.0;
            double yy = r.nextDouble() * 2.0 - 1.0;
            double q = Math.sqrt(1.0 - yy * yy);
            x = Math.cos(a) * q;
            y = yy;
            z = Math.sin(a) * q;
         } else {
            // a band: the Milky Way, a great circle tilted against the equator, a little thick
            double a = r.nextDouble() * Math.PI * 2.0;
            double spread = (r.nextGaussian()) * 0.10;
            double bx = Math.cos(a);
            double by = spread;
            double bz = Math.sin(a);
            // tilt about the x axis by 62 degrees
            double ct = Math.cos(Math.toRadians(62.0));
            double st = Math.sin(Math.toRadians(62.0));
            x = bx;
            y = by * ct - bz * st;
            z = by * st + bz * ct;
            double len = Math.sqrt(x * x + y * y + z * z);
            x /= len;
            y /= len;
            z /= len;
         }

         double b = Math.pow(r.nextDouble(), 3.2);
         float bright = (float)(0.22 + 0.78 * b);
         STAR[i * 7] = (float)(x * 150.0);
         STAR[i * 7 + 1] = (float)(y * 150.0);
         STAR[i * 7 + 2] = (float)(z * 150.0);
         STAR[i * 7 + 3] = i >= 1300 ? bright * 0.65F : bright;
         STAR[i * 7 + 4] = r.nextFloat();
         STAR[i * 7 + 5] = r.nextFloat() * 6.28F;
         STAR[i * 7 + 6] = 0.6F + r.nextFloat() * 2.4F;
      }
   }

   /** The colour of a star of a tint (0 blue-white .. 1 orange) at a brightness. */
   static void starColor(float tint, float[] out) {
      float t = tint * tint;
      out[0] = lerp(0.74F, 1.0F, t);
      out[1] = lerp(0.82F, 0.80F, t);
      out[2] = lerp(1.0F, 0.62F, t);
   }

   // ------------------------------------------------------------------ drawing
   static int glowTex;
   static int sunTex;
   static final int[] moonTex = new int[8];
   static boolean uploaded;

   private static int upload(int w, int h, int[] pixels, boolean linear) {
      return Tex.upload(w, h, pixels, linear, false);
   }

   static void init() {
      if (uploaded) {
         return;
      }

      uploaded = true;
      glowTex = upload(64, 64, glowPixels(), true);
      sunTex = upload(32, 32, sunPixels(), true);
      for (int p = 0; p < 8; p++) {
         moonTex[p] = upload(32, 32, moonPixels(p), false);
      }
   }

   private static final float[] RING_DEG = {-30.0F, 0.0F, 2.0F, 5.0F, 9.0F, 14.0F, 20.0F, 28.0F, 38.0F, 50.0F, 64.0F, 78.0F, 90.0F};
   private static final int SEGMENTS = 48;
   private static final float[] TMP = new float[3];
   private static final float[] ZEN = new float[3];
   private static final float[] HOR = new float[3];
   private static final float[] SUN = new float[3];
   private static final float[] COL = new float[3];

   /**
    * Draws the sky round the camera (the matrix is the camera's rotation only).
    *
    * @param fog  the colour of the fog / horizon (after the cave darkening)
    * @param r    renderer (the top colour, the sun's angle, the weather)
    * @param time the time of day in ticks, for the moon's phase, and a clock for the twinkling
    */
   static void draw(Renderer r, float fogR, float fogG, float fogB, long dayTime, double clock) {
      init();
      GL11.glDisable(2929);
      GL11.glDisable(3553);
      GL11.glDisable(3008);
      GL11.glDisable(3042);
      ZEN[0] = r.topR;
      ZEN[1] = r.topG;
      ZEN[2] = r.topB;
      HOR[0] = fogR;
      HOR[1] = fogG;
      HOR[2] = fogB;
      float sa = r.sunAngle;
      SUN[0] = (float)-Math.sin(sa);
      SUN[1] = (float)Math.cos(sa);
      SUN[2] = 0.0F;
      float atten = r.wfx.sunVis;
      float night = 1.0F - r.daylight;
      int phase = moonPhase(dayTime);
      float moonLit = moonLit(phase);
      float radius = 200.0F;
      for (int ring = 0; ring < RING_DEG.length - 1; ring++) {
         GL11.glBegin(8);
         for (int seg = 0; seg <= SEGMENTS; seg++) {
            double az = seg * Math.PI * 2.0 / SEGMENTS;
            for (int k = 0; k < 2; k++) {
               double el = Math.toRadians(RING_DEG[ring + k]);
               double vx = Math.cos(az) * Math.cos(el);
               double vy = Math.sin(el);
               double vz = Math.sin(az) * Math.cos(el);
               color(COL, (float)vx, (float)vy, (float)vz, ZEN, HOR, SUN, atten, night, moonLit);
               float cr = COL[0];
               float cg = COL[1];
               float cb = COL[2];
               float gl = r.wfx.gloom(vx, vy, vz);
               if (gl > 0.002F) {
                  // the sky turns grey and dark in the direction of clouds and storms
                  float lum = 0.3F * cr + 0.59F * cg + 0.11F * cb;
                  float q = Math.min(0.85F, gl * 0.9F);
                  cr = lerp(cr, lum * 0.62F, q);
                  cg = lerp(cg, lum * 0.67F, q);
                  cb = lerp(cb, lum * 0.76F, q);
               }

               GL11.glColor3f(cr, cg, cb);
               GL11.glVertex3d(vx * radius, vy * radius, vz * radius);
            }
         }

         GL11.glEnd();
      }

      float over = r.wfx.overcast;
      GL11.glPushMatrix();
      GL11.glRotatef((float)Math.toDegrees(sa), 0.0F, 0.0F, 1.0F);
      // the stars
      if (night > 0.05F) {
         GL11.glEnable(3042);
         GL11.glBlendFunc(770, 771);
         float base = night * night * (1.0F - 0.92F * over);
         double sinA = Math.sin(sa);
         double cosA = Math.cos(sa);
         for (int size = 0; size < 3; size++) {
            GL11.glPointSize(size == 0 ? 1.0F : (size == 1 ? 2.0F : 3.0F));
            GL11.glBegin(0);
            for (int i = 0; i < STARS; i++) {
               float b = STAR[i * 7 + 3];
               int cls = b > 0.80F ? 2 : (b > 0.50F ? 1 : 0);
               if (cls != size) {
                  continue;
               }

               float x = STAR[i * 7];
               float y = STAR[i * 7 + 1];
               float z = STAR[i * 7 + 2];
               // the height of the star over the horizon after the sky has turned: no stars right at the horizon (haze)
               float worldY = (float)((x * sinA + y * cosA) / 150.0);
               float horizon = smooth(-0.02F, 0.22F, worldY);
               if (horizon <= 0.0F) {
                  continue;
               }

               float tw = 0.78F + 0.22F * (float)Math.sin(clock * STAR[i * 7 + 6] + STAR[i * 7 + 5]);
               float a = base * b * tw * horizon;
               starColor(STAR[i * 7 + 4], TMP);
               GL11.glColor4f(TMP[0], TMP[1], TMP[2], a);
               GL11.glVertex3f(x, y, z);
            }

            GL11.glEnd();
         }
      }

      // the glow round the sun and the moon, the sun, the moon
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 1);
      GL11.glEnable(3553);
      GL13.glActiveTexture(33984);
      float e = SUN[1];
      float tw = twilight(e);
      float sunVis = 0.18F + 0.82F * atten;
      float sunUpFade = clamp((e + 0.12F) / 0.20F);
      if (sunUpFade > 0.0F) {
         glowColor(e, TMP);
         GL11.glBindTexture(3553, glowTex);
         float halo = (0.20F + 0.22F * tw) * sunUpFade * sunVis;
         float hs = 120.0F + 70.0F * tw;
         GL11.glColor4f(TMP[0], TMP[1] * 0.92F, TMP[2] * 0.85F, halo);
         quad(hs * 1.0F, 100.0F);
         GL11.glColor4f(TMP[0], TMP[1], TMP[2] * 0.9F, halo * 0.7F);
         quad(hs * 0.45F, 100.0F);
      }

      GL11.glBindTexture(3553, sunTex);
      float sv = sunVis * sunUpFade;
      float ss = 30.0F * (1.0F + 0.35F * tw);
      GL11.glColor4f(sv * (1.0F - 0.10F * tw), sv * (1.0F - 0.36F * tw), sv * (1.0F - 0.62F * tw), 1.0F);
      quad(ss, 100.0F);
      // the moon: its glow, the disc of its phase
      float nightF = clamp(night * 1.15F);
      if (nightF > 0.02F) {
         GL11.glBindTexture(3553, glowTex);
         GL11.glColor4f(0.55F, 0.65F, 1.0F, 0.20F * nightF * (0.25F + 0.75F * moonLit) * (1.0F - 0.8F * over));
         quad(90.0F, -100.0F);
      }

      GL11.glBlendFunc(770, 771);
      GL11.glBindTexture(3553, moonTex[phase]);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      quad(19.0F, -100.0F);
      GL11.glPopMatrix();
      GL11.glDisable(3042);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
   }

   /** A square of half-size s in the plane y = d (the sun's and the moon's plane in the turning frame). */
   private static void quad(float s, float d) {
      GL11.glBegin(7);
      GL11.glTexCoord2f(0.0F, 0.0F);
      GL11.glVertex3f(-s, d, -s);
      GL11.glTexCoord2f(1.0F, 0.0F);
      GL11.glVertex3f(s, d, -s);
      GL11.glTexCoord2f(1.0F, 1.0F);
      GL11.glVertex3f(s, d, s);
      GL11.glTexCoord2f(0.0F, 1.0F);
      GL11.glVertex3f(-s, d, s);
      GL11.glEnd();
   }
}
