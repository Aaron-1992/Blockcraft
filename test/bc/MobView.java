package bc;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import javax.imageio.ImageIO;

/**
 * Update 3.17: a tiny software renderer for the {@link Rig} models, so the new mobs can be looked at without a GPU: it uses the same bone
 * matrices and quads as the game ({@code Rig.world}, {@code Cube.quads}), a z buffer, perspective correct nearest texturing and the alpha
 * test, and writes a PNG sheet.
 *
 * <pre>
 *   java bc.MobView out.png pig turn        four views at rest
 *   java bc.MobView out.png pig walk        a stride in eight frames
 *   java bc.MobView out.png pig skin        the skin itself
 *   java bc.MobView out.png all             every mob that has a rig, one tile each
 * </pre>
 */
public final class MobView {
   final int w;
   final int h;
   final int[] img;
   final float[] zb;
   double[] eye = new double[3];
   double[] right = new double[3];
   double[] up = new double[3];
   double[] fwd = new double[3];
   double focal;
   float mulR = 1.0F;
   float mulG = 1.0F;
   float mulB = 1.0F;

   MobView(int w, int h) {
      this.w = w;
      this.h = h;
      this.img = new int[w * h];
      this.zb = new float[w * h];
   }

   void clear(int top, int bottom) {
      for (int y = 0; y < this.h; y++) {
         float t = y / (float)(this.h - 1);
         int c = RigPaint.mix(top, bottom, t);
         for (int x = 0; x < this.w; x++) {
            this.img[y * this.w + x] = c;
            this.zb[y * this.w + x] = Float.MAX_VALUE;
         }
      }
   }

   /** Orbit camera around (tx, ty, tz) in blocks; az 0 = looking at the front of a mob that faces +z, 90 = at its left side. */
   void camera(double tx, double ty, double tz, double az, double el, double dist, double fovDeg) {
      double a = Math.toRadians(az);
      double e = Math.toRadians(el);
      this.eye[0] = tx + dist * Math.sin(a) * Math.cos(e);
      this.eye[1] = ty + dist * Math.sin(e);
      this.eye[2] = tz + dist * Math.cos(a) * Math.cos(e);
      double fx = tx - this.eye[0];
      double fy = ty - this.eye[1];
      double fz = tz - this.eye[2];
      double l = Math.sqrt(fx * fx + fy * fy + fz * fz);
      this.fwd = new double[]{fx / l, fy / l, fz / l};
      double rx = this.fwd[1] * 0 - this.fwd[2] * 1;
      double ry = this.fwd[2] * 0 - this.fwd[0] * 0;
      double rz = this.fwd[0] * 1 - this.fwd[1] * 0;
      double rl = Math.sqrt(rx * rx + ry * ry + rz * rz);
      this.right = new double[]{rx / rl, ry / rl, rz / rl};
      this.up = new double[]{
         this.right[1] * this.fwd[2] - this.right[2] * this.fwd[1],
         this.right[2] * this.fwd[0] - this.right[0] * this.fwd[2],
         this.right[0] * this.fwd[1] - this.right[1] * this.fwd[0]};
      // flip so that "up" points up
      if (this.up[1] < 0) {
         this.up = new double[]{-this.up[0], -this.up[1], -this.up[2]};
      }

      this.focal = this.h * 0.5 / Math.tan(Math.toRadians(fovDeg) * 0.5);
   }

   /** A world point in blocks -> {screen x, screen y, depth}. */
   void project(double x, double y, double z, double[] out) {
      double dx = x - this.eye[0];
      double dy = y - this.eye[1];
      double dz = z - this.eye[2];
      double d = dx * this.fwd[0] + dy * this.fwd[1] + dz * this.fwd[2];
      double px = dx * this.right[0] + dy * this.right[1] + dz * this.right[2];
      double py = dx * this.up[0] + dy * this.up[1] + dz * this.up[2];
      out[0] = this.w * 0.5 + this.focal * px / d;
      out[1] = this.h * 0.5 - this.focal * py / d;
      out[2] = d;
   }

   /** A ground plane with a 1 block grid and a soft shadow, so that the size of the mob can be seen. */
   double groundY;

   void ground(double half, double shadowR) {
      double[] a = new double[3];
      // sample the ground per pixel: intersect the view ray with y = 0
      for (int y = 0; y < this.h; y++) {
         for (int x = 0; x < this.w; x++) {
            double px = (x + 0.5 - this.w * 0.5) / this.focal;
            double py = -(y + 0.5 - this.h * 0.5) / this.focal;
            double dx = this.fwd[0] + px * this.right[0] + py * this.up[0];
            double dy = this.fwd[1] + px * this.right[1] + py * this.up[1];
            double dz = this.fwd[2] + px * this.right[2] + py * this.up[2];
            if (dy >= -1e-6) {
               continue;
            }

            double t = (this.groundY - this.eye[1]) / dy;
            double gx = this.eye[0] + dx * t;
            double gz = this.eye[2] + dz * t;
            if (Math.abs(gx) > half || Math.abs(gz) > half) {
               continue;
            }

            double depth = t * (dx * this.fwd[0] + dy * this.fwd[1] + dz * this.fwd[2]);
            boolean chk = ((int)Math.floor(gx) + (int)Math.floor(gz) & 1) == 0;
            int c = chk ? 0xFF6E9A58 : 0xFF639050;
            double r = Math.sqrt(gx * gx + gz * gz);
            double sh = 1.0 - 0.45 * RigPaint.smooth(1.0F, 0.0F, (float)(r / shadowR));
            c = RigPaint.mul(c, (float)sh);
            double fogT = Math.min(1.0, r / half);
            c = RigPaint.mix(c, 0xFFB8D0E8, (float)(fogT * fogT * 0.8));
            int i = y * this.w + x;
            if (depth < this.zb[i]) {
               this.img[i] = c;
               this.zb[i] = (float)depth;
            }
         }
      }
   }

   /** Draws a rig in a pose. {@code at} is the position of its feet (blocks), {@code yawDeg} the mob's facing (0 = +z), {@code scale} its size. */
   void draw(Rig rig, Pose pose, double yawDeg, double scale, double atX, double atZ, int color) {
      double[][] wm = rig.world(pose);
      double[] root = pose.root();
      double[] place = Rig.mul(new double[]{1, 0, 0, atX * 16, 0, 1, 0, pose.rootY * 16, 0, 0, 1, atZ * 16},
         Rig.mul(Rig.rotY(yawDeg), Rig.scaleM(scale, scale, scale)));
      double[] model = Rig.mul(place, root);
      int[] tex = rig.pix;
      float[][] vtx = new float[4][5];
      double[] p = new double[3];
      double[][] sc = new double[4][3];
      for (Rig.Bone b : rig.bones) {
         double[] m = Rig.mul(model, wm[b.index]);
         for (Rig.Cube c : b.cubes) {
            if (c.group != 0 && (pose.hide >> c.group & 1) != 0) {
               continue;
            }

            int tint = rig.groupTint[c.group];
            int trgb = tint == 0 ? 0xFFFFFF : RigDraw.tintColor(color, tint);
            this.mulR = (trgb >> 16 & 255) / 255.0F;
            this.mulG = (trgb >> 8 & 255) / 255.0F;
            this.mulB = (trgb & 255) / 255.0F;
            float[] q = c.quads();
            for (int f = 0; f < 6; f++) {
               for (int k = 0; k < 4; k++) {
                  int o = (f * 4 + k) * 5;
                  Rig.apply(m, q[o] * 16.0, q[o + 1] * 16.0, q[o + 2] * 16.0, p);
                  this.project(p[0] / 16.0, p[1] / 16.0, p[2] / 16.0, sc[k]);
                  vtx[k][3] = q[o + 3];
                  vtx[k][4] = q[o + 4];
               }

               // back face culling: seen from outside the corners (top left, top right, bottom right, bottom left) run clockwise on the
               // screen, which is a positive sum in screen coordinates (y down)
               double area = 0.0;
               for (int k = 0; k < 4; k++) {
                  int n = (k + 1) % 4;
                  area += sc[k][0] * sc[n][1] - sc[n][0] * sc[k][1];
               }

               if (area <= 0) {
                  continue;
               }

               this.tri(sc[0], vtx[0], sc[1], vtx[1], sc[2], vtx[2], tex, rig.tw, rig.th);
               this.tri(sc[0], vtx[0], sc[2], vtx[2], sc[3], vtx[3], tex, rig.tw, rig.th);
            }
         }
      }
   }

   private void tri(double[] a, float[] ta, double[] b, float[] tb, double[] c, float[] tc, int[] tex, int tw, int th) {
      if (a[2] <= 0.01 || b[2] <= 0.01 || c[2] <= 0.01) {
         return;
      }

      int minX = Math.max(0, (int)Math.floor(Math.min(a[0], Math.min(b[0], c[0]))));
      int maxX = Math.min(this.w - 1, (int)Math.ceil(Math.max(a[0], Math.max(b[0], c[0]))));
      int minY = Math.max(0, (int)Math.floor(Math.min(a[1], Math.min(b[1], c[1]))));
      int maxY = Math.min(this.h - 1, (int)Math.ceil(Math.max(a[1], Math.max(b[1], c[1]))));
      double den = (b[1] - c[1]) * (a[0] - c[0]) + (c[0] - b[0]) * (a[1] - c[1]);
      if (Math.abs(den) < 1e-12) {
         return;
      }

      double ia = 1.0 / a[2];
      double ib = 1.0 / b[2];
      double ic = 1.0 / c[2];
      for (int y = minY; y <= maxY; y++) {
         for (int x = minX; x <= maxX; x++) {
            double px = x + 0.5;
            double py = y + 0.5;
            double l0 = ((b[1] - c[1]) * (px - c[0]) + (c[0] - b[0]) * (py - c[1])) / den;
            double l1 = ((c[1] - a[1]) * (px - c[0]) + (a[0] - c[0]) * (py - c[1])) / den;
            double l2 = 1.0 - l0 - l1;
            if (l0 < -1e-9 || l1 < -1e-9 || l2 < -1e-9) {
               continue;
            }

            double inv = l0 * ia + l1 * ib + l2 * ic;
            double depth = 1.0 / inv;
            int i = y * this.w + x;
            if (depth >= this.zb[i]) {
               continue;
            }

            double u = (l0 * ta[3] * ia + l1 * tb[3] * ib + l2 * tc[3] * ic) * depth;
            double v = (l0 * ta[4] * ia + l1 * tb[4] * ib + l2 * tc[4] * ic) * depth;
            int tx = Math.max(0, Math.min(tw - 1, (int)Math.floor(u * tw)));
            int ty = Math.max(0, Math.min(th - 1, (int)Math.floor(v * th)));
            int argb = tex[ty * tw + tx];
            if ((argb >>> 24) < 128) {
               continue;
            }

            this.zb[i] = (float)depth;
            if (this.mulR != 1.0F || this.mulG != 1.0F || this.mulB != 1.0F) {
               argb = 0xFF000000 | (int)((argb >> 16 & 255) * this.mulR) << 16 | (int)((argb >> 8 & 255) * this.mulG) << 8 | (int)((argb & 255) * this.mulB);
            }

            this.img[i] = argb | 0xFF000000;
         }
      }
   }

   // ------------------------------------------------------------------ states and scenes
   static Anim317.MState calm(int seed) {
      Anim317.MState s = new Anim317.MState();
      s.seed = seed;
      s.t = 40 + seed * 7;
      return s;
   }

   static Anim317.MState walking(int seed, float t, float amp) {
      Anim317.MState s = calm(seed);
      s.t = t;
      s.walk = t * 0.8F * (amp > 0.0F ? 1.0F : 0.0F);
      s.amp = amp;
      return s;
   }

   /** The mob that the scene is made for (a few clips run at different lengths). */
   static int curType = -1;

   /** Frames of a named scene: a state per frame. */
   static ArrayList<Anim317.MState> scene(String name, int frames) {
      ArrayList<Anim317.MState> list = new ArrayList<>();
      for (int i = 0; i < frames; i++) {
         float k = i / (float)Math.max(1, frames - 1);
         Anim317.MState s;
         switch (name) {
            case "walk": {
               // 1 block per ~0.6 stride cycle: walk advances 0.8 per tick; sample one stride (2 pi / 1.6 = 3.93 walk units)
               s = walking(3, 0, 1.0F);
               s.walk = i / (float)frames * (float)(Math.PI * 2 / 1.6);
               s.t = i * 2.5F;
               break;
            }
            case "idle": {
               s = calm(3);
               s.t = i * 18.0F;
               break;
            }
            case "graze": {
               s = calm(3);
               s.graze = Math.min(1.0F, k * 1.4F);
               s.t = 60 + i * 5.0F;
               break;
            }
            case "hurt": {
               s = calm(3);
               s.hurt = 1.0F - k;
               s.t = 60 + i * 2.0F;
               break;
            }
            case "attack": {
               s = walking(3, 0, 1.0F);
               s.attack = 1.0F - k;
               s.attackT = (curType == Mobs.IRON_GOLEM || curType == Mobs.ENDER_DRAGON ? 10.0F : 8.0F) * (1.0F - k);
               s.target = true;
               s.walk = i * 0.9F;
               s.t = 60 + i * 1.5F;
               break;
            }
            case "aim": {
               s = calm(3);
               s.target = true;
               s.cd = 26.0F * (1.0F - k);
               s.attackT = i == frames - 1 ? 5.0F : 0.0F;
               s.t = 60 + i * 3.0F;
               break;
            }
            case "swell": {
               s = calm(3);
               s.swell = k;
               s.target = true;
               s.t = 60 + i * 3.0F;
               break;
            }
            case "chase": {
               s = walking(3, 0, 1.0F);
               s.target = true;
               s.walk = i / (float)frames * (float)(Math.PI * 2 / 1.6);
               s.t = 60 + i * 2.5F;
               break;
            }
            case "death": {
               s = calm(3);
               s.death = k;
               s.dead = true;
               s.deathT = k * (curType == Mobs.ENDER_DRAGON ? Dragon.DEATH_TICKS : 20.0F);
               s.t = 60 + i;
               break;
            }
            case "fly": {
               s = calm(3);
               s.phase = 0;
               s.onGround = false;
               s.walk = i * 1.57F;
               s.t = 60 + i * 0.58F;
               s.amp = 1.0F;
               break;
            }
            case "perch": {
               s = calm(3);
               s.phase = Dragon.PERCH;
               s.t = 60 + i * 22.0F;
               break;
            }
            case "land": {
               s = calm(3);
               s.phase = Dragon.LAND;
               s.walk = i * 0.8F;
               s.t = 60 + i * 3.0F;
               break;
            }
            case "flee": {
               s = walking(3, 0, 1.0F);
               s.flee = true;
               s.walk = i / (float)frames * (float)(Math.PI * 2 / 1.6);
               s.t = 60 + i * 2.5F;
               break;
            }
            case "sit": {
               s = calm(3);
               s.phase = 1;
               s.t = 60 + i * 37.0F;
               break;
            }
            case "air": {
               s = walking(3, 0, 0.4F);
               s.air = k;
               s.onGround = false;
               break;
            }
            default: {
               s = calm(3);
               break;
            }
         }

         list.add(s);
      }

      return list;
   }

   static int typeOf(String n) {
      for (int t = 0; t < 64; t++) {
         if (t < Mobs.NAME.length && Mobs.NAME[t] != null && Mobs.NAME[t].equalsIgnoreCase(n)) {
            return t;
         }
      }

      return -1;
   }

   BufferedImage image() {
      BufferedImage b = new BufferedImage(this.w, this.h, BufferedImage.TYPE_INT_ARGB);
      b.setRGB(0, 0, this.w, this.h, this.img, 0, this.w);
      return b;
   }

   /** Renders one tile into a new view. */
   static MobView tile(Rig rig, Anim317.MState s, double az, double el, int size, double zoom) {
      MobView v = new MobView(size, size);
      v.clear(0xFFCFE4F5, 0xFFA9C8E4);
      Pose p = new Pose(rig.size());
      Anim317.pose(rig, s, p);
      float[] bb = rig.bounds();
      double cx = (bb[0] + bb[3]) / 32.0;
      double cy = (bb[1] + bb[4]) / 32.0;
      double cz = (bb[2] + bb[5]) / 32.0;
      double rad = Math.sqrt(Math.pow(bb[3] - bb[0], 2) + Math.pow(bb[4] - bb[1], 2) + Math.pow(bb[5] - bb[2], 2)) / 32.0;
      v.camera(cx, cy, cz, az, el, rad / Math.sin(Math.toRadians(15)) * 1.04 / zoom, 30);
      v.groundY = Math.min(0.0, bb[1] / 16.0);
      v.ground(7, 0.8 + rad * 0.5);
      v.draw(rig, p, 0, 1.0, 0, 0, s.color);
      return v;
   }

   static void blit(BufferedImage dst, MobView v, int ox, int oy) {
      dst.setRGB(ox, oy, v.w, v.h, v.img, 0, v.w);
   }

   public static void main(String[] a) throws Exception {
      String out = a.length > 0 ? a[0] : "build/mobview.png";
      String what = a.length > 1 ? a[1] : "pig";
      String sceneName = a.length > 2 ? a[2] : "turn";
      if (what.equals("all")) {
         ArrayList<Rig> rigs = new ArrayList<>();
         for (int t = 0; t < 64; t++) {
            Rig r = Rigs317.build(t);
            if (r != null) {
               rigs.add(r);
            }
         }

         int cols = Math.min(6, Math.max(1, rigs.size()));
         int rows = (rigs.size() + cols - 1) / cols;
         int tile = 280;
         BufferedImage sheet = new BufferedImage(cols * tile, rows * tile, BufferedImage.TYPE_INT_ARGB);
         for (int i = 0; i < rigs.size(); i++) {
            MobView v = tile(rigs.get(i), calm(i), 35, 14, tile, 1.0);
            blit(sheet, v, i % cols * tile, i / cols * tile);
         }

         ImageIO.write(sheet, "png", new File(out));
         System.out.println("wrote " + out + " (" + rigs.size() + " mobs)");
         return;
      }

      int type = typeOf(what);
      if (type < 0) {
         throw new IllegalArgumentException("unknown mob " + what);
      }

      Rig rig = Rigs317.build(type);
      if (rig == null) {
         throw new IllegalArgumentException(what + " has no rig yet");
      }

      if (sceneName.equals("skin")) {
         int zoom = 4;
         BufferedImage b = new BufferedImage(rig.tw * zoom, rig.th * zoom, BufferedImage.TYPE_INT_ARGB);
         for (int y = 0; y < rig.th * zoom; y++) {
            for (int x = 0; x < rig.tw * zoom; x++) {
               int c = rig.pix[y / zoom * rig.tw + x / zoom];
               int bg = ((x / 8 + y / 8) & 1) == 0 ? 0xFF404040 : 0xFF505050;
               b.setRGB(x, y, (c >>> 24) < 128 ? bg : c | 0xFF000000);
            }
         }

         ImageIO.write(b, "png", new File(out));
         System.out.println("wrote " + out);
         return;
      }

      int tile = a.length > 3 ? Integer.parseInt(a[3]) : 300;
      if (sceneName.equals("turn")) {
         BufferedImage sheet = new BufferedImage(tile * 4, tile, BufferedImage.TYPE_INT_ARGB);
         double[] az = {30, 90, 180, -60};
         double[] el = {16, 6, 16, 28};
         for (int i = 0; i < 4; i++) {
            blit(sheet, tile(rig, calm(2), az[i], el[i], tile, 1.0), i * tile, 0);
         }

         ImageIO.write(sheet, "png", new File(out));
         System.out.println("wrote " + out);
         return;
      }

      int frames = 8;
      curType = type;
      ArrayList<Anim317.MState> list = scene(sceneName, frames);
      double az = sceneName.equals("attack") || sceneName.equals("hurt") || sceneName.equals("swell") ? 40 : 90;
      if (a.length > 4) {
         az = Double.parseDouble(a[4]);
      }

      BufferedImage sheet = new BufferedImage(tile * 4, tile * 2, BufferedImage.TYPE_INT_ARGB);
      for (int i = 0; i < frames; i++) {
         blit(sheet, tile(rig, list.get(i), az, 8, tile, 1.0), i % 4 * tile, i / 4 * tile);
      }

      ImageIO.write(sheet, "png", new File(out));
      System.out.println("wrote " + out);
   }
}
