package bc;

import static bc.GameTests.check;
import static bc.GameTests.newGame;
import static bc.GameTests.test;

import java.util.Random;

/** Update 3.18: the player's body in motion ({@link PlayerAnim}, {@link PlayerModel#build}, the states that Game.tickPose smooths). */
final class TestsPlayerAnim {
   static void run() {
      rest();
      cycles();
      limits();
      ground();
      arms();
      geometry();
      game();
   }

   private static PlayerModel.Pose walking(float limb, float amp, boolean run) {
      PlayerModel.Pose p = new PlayerModel.Pose();
      p.limb = limb;
      p.amp = amp;
      p.sprint = run;
      p.sprintK = run ? 1.0F : 0.0F;
      p.age = 50;
      return p;
   }

   static boolean near(float a, float b, float eps) {
      return Math.abs(a - b) <= eps;
   }

   // ------------------------------------------------------------------ standing and walking
   static void rest() {
      test("player anim: standing still - straight legs, relaxed arms, the feet on the floor", () -> {
         PlayerModel.Pose p = new PlayerModel.Pose();
         for (int t = 0; t < 400; t += 7) {
            p.age = t;
            PlayerAnim.Joints j = PlayerAnim.solve(p);
            for (int i = 0; i < 2; i++) {
               check(j.knee[i] < 3.0F && Math.abs(j.hip[i]) < 2.0F, "legs straight at tick " + t + ": " + j.knee[i] + " " + j.hip[i]);
               check(j.el[i] < 7.0F && Math.abs(j.sh[i]) < 5.0F, "arms relaxed at tick " + t + ": " + j.el[i] + " " + j.sh[i]);
            }

            check(near(j.dy, 0.0F, 0.2F), "the body stands on the floor: dy " + j.dy);
            check(Math.abs(j.tPitch) < 1.0F && Math.abs(j.pitch) < 0.01F, "upright");
         }
      });
   }

   static void cycles() {
      test("player anim: a stride - the legs swing against each other, each arm against the leg on its side, the cycle repeats", () -> {
         float period = (float)(Math.PI * 2 / 1.6);
         for (float limb = 0.0F; limb < period; limb += 0.05F) {
            for (boolean run : new boolean[]{false, true}) {
               PlayerAnim.Joints a = PlayerAnim.solve(walking(limb, 0.86F, run));
               check(near(a.hip[0], -a.hip[1], 0.01F), "the legs swing against each other at " + limb);
               check(near(a.sh[0], -a.sh[1], 0.01F), "so do the arms at " + limb);
               check(a.hip[0] * a.sh[0] <= 0.01F, "the right arm goes against the right leg at " + limb + ": " + a.hip[0] + " " + a.sh[0]);
               PlayerAnim.Joints b = PlayerAnim.solve(walking(limb + period, 0.86F, run));
               check(near(a.hip[0], b.hip[0], 0.001F) && near(a.knee[1], b.knee[1], 0.001F) && near(a.el[0], b.el[0], 0.001F) && near(a.dy, b.dy, 0.001F),
                  "one stride later the body is where it was, at " + limb);
            }
         }

         // the knee bends while the leg swings forward, and not while it is on the ground behind the body
         PlayerAnim.Joints fwd = PlayerAnim.solve(walking(0.0F, 0.86F, false)); // right leg passing under the body going forward
         check(fwd.knee[0] > 25.0F, "the swinging leg bends its knee: " + fwd.knee[0]);
         PlayerAnim.Joints stance = PlayerAnim.solve(walking((float)(Math.PI / 1.6) * 0.5F + 0.0F, 0.86F, false)); // heel strike of the right leg
         check(stance.knee[0] < 6.0F, "the leg that hits the ground is straight: " + stance.knee[0]);
         // running swings further and bends more than walking
         PlayerAnim.Joints w = PlayerAnim.solve(walking(0.4F, 0.9F, false));
         PlayerAnim.Joints r = PlayerAnim.solve(walking(0.4F, 0.9F, true));
         check(Math.abs(r.hip[0]) > Math.abs(w.hip[0]) && r.el[0] > w.el[0] + 20.0F && r.tPitch > w.tPitch + 5.0F, "running: " + r.hip[0] + " " + r.el[0] + " " + r.tPitch);
      });
      test("player anim: no joint jumps from one frame to the next (the clip is continuous in every way of moving)", () -> {
         String[] ways = {"walk", "run", "sneak", "swim", "climb", "tread", "fly", "air"};
         for (String way : ways) {
            float worst = 0.0F;
            PlayerAnim.Joints prev = null;
            for (int i = 0; i <= 600; i++) {
               PlayerModel.Pose p = walking(i * 0.02F, 0.9F, way.equals("run"));
               p.age = 50 + i * 0.4F;
               p.sneakK = way.equals("sneak") ? 1.0F : 0.0F;
               p.swim = way.equals("swim") ? 1.0F : 0.0F;
               p.tread = way.equals("tread") ? 1.0F : 0.0F;
               p.climb = way.equals("climb") ? 1.0F : 0.0F;
               p.fly = way.equals("fly") ? 1.0F : 0.0F;
               p.airK = way.equals("air") ? 1.0F : 0.0F;
               p.vy = 0.3F;
               PlayerAnim.Joints j = PlayerAnim.solve(p);
               if (prev != null) {
                  for (int k = 0; k < 2; k++) {
                     worst = Math.max(worst, Math.abs(j.sh[k] - prev.sh[k]));
                     worst = Math.max(worst, Math.abs(j.el[k] - prev.el[k]));
                     worst = Math.max(worst, Math.abs(j.hip[k] - prev.hip[k]));
                     worst = Math.max(worst, Math.abs(j.knee[k] - prev.knee[k]));
                  }
               }

               prev = j;
            }

            check(worst < 7.0F, way + ": the largest step of a joint between two frames is " + worst + " degrees");
         }
      });
      test("player anim: the ways of moving are mixed, not switched - half way is half way", () -> {
         PlayerModel.Pose p = walking(0.7F, 0.9F, false);
         p.vy = 0.3F;
         PlayerModel.Pose half = walking(0.7F, 0.9F, false);
         half.vy = 0.3F;
         half.airK = 0.5F;
         PlayerModel.Pose full = walking(0.7F, 0.9F, false);
         full.vy = 0.3F;
         full.airK = 1.0F;
         PlayerAnim.Joints a = PlayerAnim.solve(p);
         PlayerAnim.Joints b = PlayerAnim.solve(half);
         PlayerAnim.Joints c = PlayerAnim.solve(full);
         for (int i = 0; i < 2; i++) {
            check(near(b.knee[i], (a.knee[i] + c.knee[i]) / 2.0F, 0.01F), "knee " + i + " is in the middle: " + a.knee[i] + " " + b.knee[i] + " " + c.knee[i]);
            check(near(b.sh[i], (a.sh[i] + c.sh[i]) / 2.0F, 0.01F), "shoulder " + i + " is in the middle");
         }
      });
   }

   // ------------------------------------------------------------------ limits
   static void limits() {
      test("player anim: 200,000 random poses - every angle finite and inside what a body can do", () -> {
         Random rnd = new Random(318);
         for (int n = 0; n < 200_000; n++) {
            PlayerModel.Pose p = new PlayerModel.Pose();
            p.limb = rnd.nextFloat() * 400.0F - 100.0F;
            p.amp = rnd.nextFloat();
            p.age = rnd.nextInt(100000);
            p.sneakK = rnd.nextFloat();
            p.sprintK = rnd.nextFloat();
            p.airK = rnd.nextBoolean() ? rnd.nextFloat() : 0.0F;
            p.swim = rnd.nextBoolean() ? rnd.nextFloat() : 0.0F;
            p.tread = rnd.nextBoolean() ? rnd.nextFloat() : 0.0F;
            p.climb = rnd.nextBoolean() ? rnd.nextFloat() : 0.0F;
            p.fly = rnd.nextBoolean() ? rnd.nextFloat() : 0.0F;
            p.vy = rnd.nextFloat() * 2.0F - 1.0F;
            p.land = rnd.nextBoolean() ? rnd.nextFloat() : 0.0F;
            p.hurt = rnd.nextBoolean() ? rnd.nextFloat() : 0.0F;
            p.swing = rnd.nextInt(4) == 0 ? rnd.nextFloat() : -1.0F;
            p.eat = rnd.nextInt(6) == 0 ? rnd.nextFloat() : -1.0F;
            p.bow = rnd.nextInt(6) == 0 ? rnd.nextFloat() * 3.0F : -1.0F;
            p.held = rnd.nextBoolean() ? 276 : 0;
            p.headPitch = rnd.nextFloat() * 180.0F - 90.0F;
            PlayerAnim.Joints j = PlayerAnim.solve(p);
            boolean ok = Float.isFinite(j.dy) && Float.isFinite(j.dz) && Float.isFinite(j.pitch) && Float.isFinite(j.tPitch) && Float.isFinite(j.tYaw)
               && Float.isFinite(j.tRoll) && Float.isFinite(j.headPitch) && Float.isFinite(j.headRoll) && Float.isFinite(j.chest) && Float.isFinite(j.legZ);
            for (int i = 0; i < 2; i++) {
               ok &= Float.isFinite(j.sh[i]) && Float.isFinite(j.el[i]) && Float.isFinite(j.hip[i]) && Float.isFinite(j.knee[i]) && Float.isFinite(j.abd[i]);
               ok &= j.knee[i] >= 0.0F && j.knee[i] <= 150.0F && j.el[i] >= 0.0F && j.el[i] <= 150.0F;
               ok &= j.hip[i] >= -70.0F && j.hip[i] <= 110.0F && j.sh[i] >= -110.0F && j.sh[i] <= 200.0F;
               ok &= j.abd[i] >= -30.0F && j.abd[i] <= 100.0F && j.hipAbd[i] >= -10.0F && j.hipAbd[i] <= 40.0F;
            }

            if (!ok) {
               check(false, "pose " + n + " gave a joint out of range or not a number (limb " + p.limb + ")");
               return;
            }
         }

         check(true, "all poses in range");
      });
   }

   // ------------------------------------------------------------------ the floor
   static void ground() {
      test("player anim: on the ground the lower foot stands on the floor in every stride, sneak and landing", () -> {
         Random rnd = new Random(7);
         for (int n = 0; n < 20_000; n++) {
            PlayerModel.Pose p = new PlayerModel.Pose();
            p.limb = rnd.nextFloat() * 50.0F;
            p.amp = rnd.nextFloat();
            p.age = rnd.nextInt(5000);
            p.sneakK = rnd.nextBoolean() ? rnd.nextFloat() : 0.0F;
            p.sprintK = rnd.nextBoolean() ? 1.0F : 0.0F;
            p.land = rnd.nextInt(3) == 0 ? rnd.nextFloat() : 0.0F;
            PlayerAnim.Joints j = PlayerAnim.solve(p);
            float low = Math.max(PlayerAnim.reach(j.hip[0], j.knee[0]), PlayerAnim.reach(j.hip[1], j.knee[1]));
            float foot = 12.0F + j.dy - low;
            if (Math.abs(foot) > 0.01F) {
               check(false, "the lowest foot is " + foot + " pixels above the floor (limb " + p.limb + ", amp " + p.amp + ")");
               return;
            }

            if (j.dy > 1.8F) {
               check(false, "the body rises above its standing height: " + j.dy);
               return;
            }
         }

         check(true, "feet on the floor");
         // a deep landing crouches, a stride lowers the body a little
         PlayerModel.Pose land = new PlayerModel.Pose();
         land.land = 1.0F;
         check(PlayerAnim.solve(land).dy < -1.5F, "a hard landing lowers the body: " + PlayerAnim.solve(land).dy);
         PlayerModel.Pose run = walking(0.5F, 1.0F, true);
         check(PlayerAnim.solve(run).dy < 0.0F, "a stride lowers the body: " + PlayerAnim.solve(run).dy);
      });
   }

   // ------------------------------------------------------------------ the arms that do something
   static void arms() {
      test("player anim: swing - at rest before and after, up over the shoulder, then down in front", () -> {
         PlayerModel.Pose p = new PlayerModel.Pose();
         PlayerAnim.Joints rest = PlayerAnim.solve(p);
         p.swing = 0.0F;
         PlayerAnim.Joints s0 = PlayerAnim.solve(p);
         p.swing = 1.0F;
         PlayerAnim.Joints s1 = PlayerAnim.solve(p);
         check(near(s0.sh[0], rest.sh[0], 0.01F) && near(s1.sh[0], rest.sh[0], 0.01F), "the swing starts and ends at rest: " + s0.sh[0] + " " + s1.sh[0] + " " + rest.sh[0]);
         check(near(s0.tYaw, rest.tYaw, 0.01F) && near(s1.tYaw, rest.tYaw, 0.01F), "the body is not turned at the ends");
         float top = 0.0F;
         float at = 0.0F;
         float low = 999.0F;
         for (float t = 0.0F; t <= 1.0F; t += 0.02F) {
            p.swing = t;
            PlayerAnim.Joints j = PlayerAnim.solve(p);
            if (j.sh[0] > top) {
               top = j.sh[0];
               at = t;
            }

            if (t > at + 0.1F) {
               low = Math.min(low, j.sh[0]);
            }
         }

         check(top > 115.0F && at > 0.15F && at < 0.4F, "the arm is raised above the shoulder early in the swing: " + top + " at " + at);
         p.swing = 0.58F;
         check(near(PlayerAnim.solve(p).sh[0], 38.0F, 1.0F), "and comes down in front of the body");
         p.swing = 0.7F;
         p.eat = 0.5F;
         check(PlayerAnim.solve(p).el[0] > 100.0F, "eating wins over the swing: the hand stays at the mouth");
      });
      test("player anim: eating brings the hand to the mouth, a bow is aimed with one arm and drawn with the other", () -> {
         PlayerModel.Pose p = new PlayerModel.Pose();
         p.eat = 0.5F;
         PlayerAnim.Joints e = PlayerAnim.solve(p);
         check(e.sh[0] > 60.0F && e.sh[0] < 85.0F && e.el[0] > 110.0F, "hand at the mouth: " + e.sh[0] + " " + e.el[0]);
         p.eat = 0.0F;
         check(near(PlayerAnim.solve(p).el[0], PlayerAnim.solve(new PlayerModel.Pose()).el[0], 0.5F), "not yet");
         PlayerModel.Pose b = new PlayerModel.Pose();
         b.bow = 1.5F;
         b.headPitch = 0.0F;
         PlayerAnim.Joints j = PlayerAnim.solve(b);
         check(near(j.sh[0] + j.el[0], 90.0F, 1.0F) && j.el[0] < 10.0F, "the right arm points where the player looks: " + j.sh[0]);
         check(j.el[1] > 100.0F, "the left hand is at the cheek with the bow fully drawn: " + j.el[1]);
         b.headPitch = 40.0F;
         check(near(PlayerAnim.solve(b).sh[0] + PlayerAnim.solve(b).el[0], 50.0F, 1.5F), "looking down 40 degrees lowers the aiming arm: " + PlayerAnim.solve(b).sh[0]);
         b.bow = 0.2F;
         check(PlayerAnim.solve(b).el[1] < j.el[1] - 40.0F, "a half drawn bow bends the elbow less");
      });
      test("player anim: swimming lies down with the arms circling, treading water stays upright, climbing reaches up, a jump lifts a knee", () -> {
         PlayerModel.Pose s = new PlayerModel.Pose();
         s.swim = 1.0F;
         float lo = 999.0F;
         float hi = -999.0F;
         for (float l = 0.0F; l < 8.0F; l += 0.05F) {
            s.limb = l;
            PlayerAnim.Joints j = PlayerAnim.solve(s);
            lo = Math.min(lo, j.sh[0]);
            hi = Math.max(hi, j.sh[0]);
            check(j.pitch > 55.0F, "the swimmer lies down: " + j.pitch);
         }

         check(lo < 20.0F && hi > 160.0F, "the arm goes all the way round: " + lo + " .. " + hi);
         PlayerModel.Pose t = new PlayerModel.Pose();
         t.tread = 1.0F;
         check(Math.abs(PlayerAnim.solve(t).pitch) < 0.01F && PlayerAnim.solve(t).abd[0] > 15.0F, "treading water: upright, arms out");
         PlayerModel.Pose c = new PlayerModel.Pose();
         c.climb = 1.0F;
         float reach = 0.0F;
         float lift = 0.0F;
         for (float l = 0.0F; l < 4.0F; l += 0.05F) {
            c.limb = l;
            PlayerAnim.Joints j = PlayerAnim.solve(c);
            reach = Math.max(reach, Math.max(j.sh[0], j.sh[1]));
            lift = Math.max(lift, Math.max(j.knee[0], j.knee[1]));
         }

         check(reach > 135.0F && lift > 60.0F, "climbing: hands up to " + reach + ", knees to " + lift);
         PlayerModel.Pose a = new PlayerModel.Pose();
         a.airK = 1.0F;
         a.vy = 0.5F;
         a.limb = 1.0F;
         PlayerAnim.Joints up = PlayerAnim.solve(a);
         a.vy = -0.8F;
         PlayerAnim.Joints down = PlayerAnim.solve(a);
         check(Math.max(up.knee[0], up.knee[1]) > 50.0F && up.hip[0] + up.hip[1] > down.hip[0] + down.hip[1], "rising: a knee comes up; falling: the legs hang");
         check(down.abd[0] > up.abd[0] + 10.0F, "falling: the arms go out wide");
      });
   }

   // ------------------------------------------------------------------ the boxes
   /** A sink that records where the boxes of the body are (blocks). */
   static final class Bounds implements PlayerModel.Sink {
      double[] m = {1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0};
      final java.util.ArrayDeque<double[]> stack = new java.util.ArrayDeque<>();
      double minX = 1e9;
      double minY = 1e9;
      double minZ = 1e9;
      double maxX = -1e9;
      double maxY = -1e9;
      double maxZ = -1e9;
      double armMinY = 1e9;
      double armMaxY = -1e9;
      int boxes;
      int held;
      boolean bad;

      @Override
      public void push() {
         this.stack.push(this.m.clone());
      }

      @Override
      public void pop() {
         this.m = this.stack.pop();
      }

      @Override
      public void translate(float x, float y, float z) {
         this.m = Rig.mul(this.m, new double[]{1, 0, 0, x, 0, 1, 0, y, 0, 0, 1, z});
      }

      @Override
      public void rotate(float deg, float x, float y, float z) {
         this.m = Rig.mul(this.m, x != 0 ? Rig.rotX(deg) : y != 0 ? Rig.rotY(deg) : Rig.rotZ(deg));
      }

      @Override
      public void scale(float x, float y, float z) {
         this.m = Rig.mul(this.m, Rig.scaleM(x, y, z));
      }

      @Override
      public void box(float x0, float y0, float z0, float x1, float y1, float z1, float[] r, float bright) {
         this.boxes++;
         if (r.length != 24) {
            this.bad = true;
         }

         for (int f = 0; f < 6; f++) {
            // a texture rectangle must lie inside the skin and not be upside down
            if (r[f * 4] < 0 || r[f * 4 + 2] > Skins.SIZE || r[f * 4 + 1] < 0 || r[f * 4 + 3] > Skins.SIZE || r[f * 4 + 2] < r[f * 4] || r[f * 4 + 3] < r[f * 4 + 1]) {
               this.bad = true;
            }
         }

         double[] p = new double[3];
         double[] c = new double[3];
         Rig.apply(this.m, (x0 + x1) * 0.5, (y0 + y1) * 0.5, (z0 + z1) * 0.5, c);
         boolean isArm = Math.abs(c[0]) > 0.28;
         for (int i = 0; i < 8; i++) {
            Rig.apply(this.m, (i & 1) == 0 ? x0 : x1, (i & 2) == 0 ? y0 : y1, (i & 4) == 0 ? z0 : z1, p);
            this.minX = Math.min(this.minX, p[0]);
            this.minY = Math.min(this.minY, p[1]);
            this.minZ = Math.min(this.minZ, p[2]);
            this.maxX = Math.max(this.maxX, p[0]);
            this.maxY = Math.max(this.maxY, p[1]);
            this.maxZ = Math.max(this.maxZ, p[2]);
            if (isArm) {
               this.armMinY = Math.min(this.armMinY, p[1]);
               this.armMaxY = Math.max(this.armMaxY, p[1]);
            }
         }
      }

      @Override
      public void held(int id, boolean slim, float bright) {
         this.held++;
      }
   }

   static Bounds bounds(PlayerModel.Pose p, boolean slim) {
      Bounds b = new Bounds();
      PlayerModel.build(PlayerAnim.solve(p), p, slim, 1.0F, b);
      return b;
   }

   static void geometry() {
      test("player model: a standing body is 1.875 blocks tall and stands on the floor; limbs are two boxes, joints have caps", () -> {
         for (boolean slim : new boolean[]{false, true}) {
            PlayerModel.Pose p = new PlayerModel.Pose();
            Bounds b = bounds(p, slim);
            check(!b.bad, "texture rectangles inside the skin");
            check(near((float)b.minY, 0.0F, 0.03F), "the soles are at the floor: " + b.minY);
            check(near((float)b.maxY, 1.875F + 0.0235F, 0.05F), "the head is 1.875 blocks up (+ the hat layer): " + b.maxY);
            check(near((float)b.armMaxY, 24.0F * 0.9375F / 16.0F + 0.02F, 0.04F), "the top of the shoulders is flush with the top of the torso (y = 24px): " + b.armMaxY);
            check(near((float)b.armMinY, 12.0F * 0.9375F / 16.0F, 0.04F), "the hands end at the waist (y = 12px): " + b.armMinY);
            float width = (float)(b.maxX - b.minX) * 16.0F / 0.9375F; // in model pixels
            check(width > (slim ? 14.0F : 16.0F) && width < (slim ? 17.5F : 19.5F), "width over the arms (" + slim + "): " + width + " pixels");
            check(b.boxes == 2 * 2 * 2 + 2 + 2 * 2 * 2 + 2, "boxes: 4 for each leg, 2 for the torso, 4 for each arm, 2 for the head - and no caps while the joints are straight: " + b.boxes);
         }

         PlayerModel.Pose run = walking(0.5F, 1.0F, true);
         Bounds r = bounds(run, false);
         check(r.boxes > 20 && r.boxes <= 28, "caps appear at bent joints: " + r.boxes);
         check(near((float)r.minY, 0.0F, 0.03F), "a running body's lower foot is on the floor: " + r.minY);
         PlayerModel.Pose held = new PlayerModel.Pose();
         held.held = 276;
         check(bounds(held, false).held == 1, "the item in the hand is drawn once");
      });
      test("player model: the two halves of a limb use the whole texture of the long box between them", () -> {
         for (int[] box : new int[][]{{0, 16, 4, 4}, {40, 16, 4, 4}, {40, 16, 3, 4}, {16, 48, 4, 4}, {32, 48, 4, 4}}) {
            int u = box[0];
            int v = box[1];
            int w = box[2];
            int d = box[3];
            float[] up = PlayerModel.partRects(u, v, w, d, 0, 6);
            float[] lo = PlayerModel.partRects(u, v, w, d, 6, 12);
            float[] whole = PlayerModel.rects(u, v, w, 12, d);
            for (int f = 2; f < 6; f++) {
               check(near(up[f * 4 + 1], whole[f * 4 + 1], 0.001F) && near(lo[f * 4 + 3], whole[f * 4 + 3], 0.001F) && near(up[f * 4 + 3], lo[f * 4 + 1], 0.001F),
                  "the sides of the halves meet and cover the box (" + u + "," + v + ") face " + f);
               check(near(up[f * 4], whole[f * 4], 0.001F) && near(lo[f * 4 + 2], whole[f * 4 + 2], 0.001F), "same columns");
            }

            check(near(up[1], whole[1], 0.001F) && near(up[3], whole[3], 0.001F), "the top of the upper half is the top of the box");
            check(near(lo[5], whole[5], 0.001F) && near(lo[7], whole[7], 0.001F), "the bottom of the lower half is the bottom of the box");
            float[] cap = PlayerModel.capRects(u, v, w, d, 6);
            for (int f = 0; f < 6; f++) {
               check(near(cap[f * 4 + 1], cap[f * 4 + 3], 0.001F) && cap[f * 4 + 1] >= v + d + 5.0F && cap[f * 4 + 1] <= v + d + 6.0F, "the cap uses one row of the skin at the joint");
            }
         }
      });
   }

   // ------------------------------------------------------------------ what the game feeds the body
   static void game() {
      test("player anim: the game smooths the states - swimming, treading water, flying, the air, landing, a hit, eating, drawing a bow", () -> {
         Game g = newGame(0);
         Player pl = g.player;
         pl.onGround = true;
         for (int i = 0; i < 12; i++) {
            pl.px = pl.x;
            pl.pz = pl.z;
            g.tickPose();
         }

         check(g.animK[Game.K_SWIM] < 0.01F && g.animK[Game.K_AIR] < 0.01F && g.animK[Game.K_FLY] < 0.01F, "standing on the floor: no state");
         // deep water, not moving: treading water
         pl.inWater = true;
         pl.onGround = false;
         for (int i = 0; i < 14; i++) {
            pl.px = pl.x;
            pl.pz = pl.z;
            g.tickPose();
         }

         check(g.animK[Game.K_TREAD] > 0.9F && g.animK[Game.K_SWIM] < 0.05F && g.animK[Game.K_AIR] < 0.05F, "treading water: " + g.animK[Game.K_TREAD]);
         // moving through it: swimming
         for (int i = 0; i < 14; i++) {
            pl.px = pl.x;
            pl.pz = pl.z;
            pl.x += 0.1;
            g.tickPose();
         }

         check(g.animK[Game.K_SWIM] > 0.9F, "swimming: " + g.animK[Game.K_SWIM]);
         PlayerModel.Pose sw = g.pose(0.5F);
         check(sw.swim > 0.9F && sw.tread > 0.9F, "the pose carries it");
         // out of the water, still in the air: a fall; the state builds up over a few ticks, not at once
         pl.inWater = false;
         pl.onGround = false;
         pl.vy = -0.5;
         pl.px = pl.x;
         pl.pz = pl.z;
         g.tickPose();
         check(g.animK[Game.K_AIR] < 0.5F, "one tick in the air is not yet a jump: " + g.animK[Game.K_AIR]);
         for (int i = 0; i < 24; i++) {
            pl.px = pl.x;
            pl.pz = pl.z;
            g.tickPose();
         }

         check(g.animK[Game.K_AIR] > 0.9F && g.animK[Game.K_SWIM] < 0.05F, "in the air: " + g.animK[Game.K_AIR] + " " + g.animK[Game.K_SWIM]);
         // the landing: a hard one bends the knees and it fades
         pl.onGround = true;
         pl.vy = 0.0;
         g.lastVy = -0.8;
         pl.px = pl.x;
         pl.pz = pl.z;
         g.tickPose();
         check(g.animK[Game.K_LAND] > 0.8F, "a hard landing: " + g.animK[Game.K_LAND]);
         for (int i = 0; i < 20; i++) {
            pl.px = pl.x;
            pl.pz = pl.z;
            g.tickPose();
         }

         check(g.animK[Game.K_LAND] < 0.03F && g.animK[Game.K_AIR] < 0.03F, "and it is over: " + g.animK[Game.K_LAND]);
         pl.onGround = false;
         g.lastGround = false;
         pl.onGround = true;
         g.lastVy = -0.1;
         g.tickPose();
         check(g.animK[Game.K_LAND] < 0.03F, "a small step down is no landing");
         // flying
         pl.flying = true;
         for (int i = 0; i < 20; i++) {
            pl.px = pl.x;
            pl.pz = pl.z;
            g.tickPose();
         }

         check(g.animK[Game.K_FLY] > 0.9F, "flying: " + g.animK[Game.K_FLY]);
         pl.flying = false;
         // a hit
         pl.hurtTime = 10;
         g.tickPose();
         check(g.animK[Game.K_HURT] > 0.99F && g.pose(1.0F).hurt > 0.99F, "hurt");
         pl.hurtTime = 0;
         g.tickPose();
         check(g.animK[Game.K_HURT] == 0.0F, "no longer hurt");
         // eating and the bow
         g.eatTicks = 16;
         check(near(g.pose(0.0F).eat, 0.5F, 0.001F), "halfway through a meal");
         g.eatTicks = 0;
         check(g.pose(0.0F).eat < 0.0F, "not eating");
         g.bowCharge = 10;
         check(near(g.pose(0.0F).bow, 0.5F, 0.001F), "half drawn");
         g.bowCharge = 0;
         check(g.pose(0.0F).bow < 0.0F, "no bow");
         // the pose is interpolated between two ticks
         g.pAnimK[Game.K_FLY] = 0.0F;
         g.animK[Game.K_FLY] = 1.0F;
         check(near(g.pose(0.25F).fly, 0.25F, 0.001F), "interpolated");
      });
      test("player anim: on a ladder the climbing phase follows the climb, not the walk", () -> {
         Game g = newGame(0);
         Player pl = g.player;
         int x = (int)Math.floor(pl.x);
         int z = (int)Math.floor(pl.z);
         for (int y = GameTests.GROUND + 1; y < GameTests.GROUND + 6; y++) {
            g.world.setBlock(x, y, z, Blocks.LADDER);
         }

         pl.onGround = false;
         pl.y = GameTests.GROUND + 2.0;
         pl.py = pl.y;
         pl.px = pl.x;
         pl.pz = pl.z;
         float before = g.walkPhase;
         for (int i = 0; i < 10; i++) {
            pl.py = pl.y;
            pl.y += 0.12;
            pl.px = pl.x;
            pl.pz = pl.z;
            g.tickPose();
         }

         check(g.animK[Game.K_CLIMB] > 0.9F, "climbing: " + g.animK[Game.K_CLIMB]);
         check(g.walkPhase > before + 2.0F, "the phase runs with the climb although the player does not move sideways: " + (g.walkPhase - before));
         check(g.animK[Game.K_AIR] < 0.05F, "a ladder is not a fall");
      });
   }
}
