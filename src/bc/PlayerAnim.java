package bc;

/**
 * Update 3.18: the player's body in motion - plain Java, no OpenGL, so the maths can be tested (and viewed with a software renderer, see
 * test/bc/PlayerView). {@link #solve} turns a {@link PlayerModel.Pose} into the angles of the joints of the body; {@link PlayerModel#build} puts
 * the boxes of the skin there.
 * <p>
 * The body has a hip joint and a knee in each leg, a shoulder joint and an elbow in each arm, a torso that leans, twists and rolls about the hips,
 * and a head that keeps looking where the player looks whatever the torso does. Angles are in degrees; the sagittal ones (hip, knee, shoulder,
 * elbow, torso pitch) are positive when the limb swings FORWARD (the knee and the elbow bend: the shin goes back, the forearm goes forward),
 * "abd" is the swing outwards (away from the body). Index 0 is the right limb (at -x), 1 the left one.
 * <p>
 * The poses of the different ways of moving are worked out one by one (standing, walking, running, sneaking, in the air, swimming, treading
 * water, climbing, flying) and mixed by weights that the game smooths from tick to tick, so that nothing jumps. A swing, eating and drawing a bow
 * take over an arm; then the height of the body is chosen so that the lower foot stands on the ground (the legs have no feet that could stretch).
 */
final class PlayerAnim {
   private static final float PI = (float)Math.PI;

   private PlayerAnim() {
   }

   /** The angles of the joints (degrees) and the offsets (model pixels) of one frame. */
   static final class Joints {
      /** The whole body: moved up or down and forward, turned about the hips (swimming, flying). */
      float dy;
      float dz;
      float pitch;
      float roll;
      /** The torso about the hips. */
      float tPitch;
      float tYaw;
      float tRoll;
      /** The head relative to the torso, besides looking: a tilt to the side. */
      float headRoll;
      float headPitch;
      /** The shoulders and the head rise a little when the chest fills. */
      float chest;
      /** The hip joints move forward (a crouch). */
      float legZ;
      final float[] sh = new float[2];
      final float[] abd = new float[2];
      final float[] twist = new float[2];
      final float[] el = new float[2];
      final float[] hip = new float[2];
      final float[] hipAbd = new float[2];
      final float[] knee = new float[2];

      void add(Joints o, float w) {
         if (w <= 0.0F) {
            return;
         }

         this.dy += o.dy * w;
         this.dz += o.dz * w;
         this.pitch += o.pitch * w;
         this.roll += o.roll * w;
         this.tPitch += o.tPitch * w;
         this.tYaw += o.tYaw * w;
         this.tRoll += o.tRoll * w;
         this.headRoll += o.headRoll * w;
         this.headPitch += o.headPitch * w;
         this.chest += o.chest * w;
         this.legZ += o.legZ * w;
         for (int i = 0; i < 2; i++) {
            this.sh[i] += o.sh[i] * w;
            this.abd[i] += o.abd[i] * w;
            this.twist[i] += o.twist[i] * w;
            this.el[i] += o.el[i] * w;
            this.hip[i] += o.hip[i] * w;
            this.hipAbd[i] += o.hipAbd[i] * w;
            this.knee[i] += o.knee[i] * w;
         }
      }
   }

   static float sin(float x) {
      return (float)Math.sin(x);
   }

   static float cos(float x) {
      return (float)Math.cos(x);
   }

   static float clamp(float v, float lo, float hi) {
      return v < lo ? lo : Math.min(v, hi);
   }

   static float smooth(float a, float b, float x) {
      float t = clamp((x - a) / (b - a), 0.0F, 1.0F);
      return t * t * (3.0F - 2.0F * t);
   }

   static float lerp(float a, float b, float t) {
      return a + (b - a) * t;
   }

   // ------------------------------------------------------------------ the ways of moving
   /** Standing, walking, running, sneaking, landing: the legs swing against the arms, the knees bend as a leg goes forward. */
   static Joints ground(PlayerModel.Pose p) {
      Joints j = new Joints();
      float a = clamp(p.amp, 0.0F, 1.0F);
      float run = clamp(p.sprintK, 0.0F, 1.0F);
      float sn = clamp(p.sneakK, 0.0F, 1.0F);
      float land = clamp(p.land, 0.0F, 1.0F);
      float phi = p.limb * 1.6F;
      float t = p.age;
      float idle = 1.0F - Math.min(1.0F, a * 2.2F);
      float hipAmp = (24.0F + 20.0F * run) * a * (1.0F - 0.4F * sn);
      float armAmp = (22.0F + 28.0F * run) * a * (1.0F - 0.4F * sn);
      float kneeAmp = (46.0F + 36.0F * run) * a;
      for (int i = 0; i < 2; i++) {
         float ph = phi + i * PI;
         j.hip[i] = hipAmp * sin(ph) + idle * 1.2F * sin(t * 0.045F + i * PI) + sn * 26.0F + land * 60.0F;
         float swing = Math.max(0.0F, cos(ph + 0.35F)); // the leg that goes forward bends its knee
         j.knee[i] = 1.5F + kneeAmp * (float)Math.pow(swing, 1.25) + sn * 34.0F + land * 70.0F;
         j.hipAbd[i] = 1.0F + 2.0F * run * a;
         float aph = phi + (i == 0 ? PI : 0.0F); // the right arm goes with the left leg
         float fwd = 0.5F + 0.5F * sin(aph + 0.5F);
         j.sh[i] = armAmp * sin(aph) + idle * 2.2F * sin(t * 0.067F + i * 2.1F) - sn * 8.0F + land * 35.0F;
         j.abd[i] = 3.0F + idle * 1.2F * sin(t * 0.05F + i * 1.7F) + 2.5F * run * a;
         j.el[i] = (8.0F + 62.0F * run) * Math.min(1.0F, a * 1.6F) * (0.65F + 0.35F * fwd) + idle * 4.0F + sn * 8.0F;
      }

      j.tYaw = -(4.5F + 7.0F * run) * a * sin(phi);
      j.tRoll = 1.5F * a * sin(phi + 1.2F);
      j.tPitch = (2.5F + 12.0F * run) * a + 22.0F * sn + 24.0F * land;
      j.legZ = 2.0F * sn;
      j.chest = 0.25F * sin(t * 0.09F) * idle;
      j.headRoll = 1.0F * sin(t * 0.05F) * idle;
      return j;
   }

   /** Jumping and falling: one knee up while rising, arms flung up and out, a falling body spreads its arms. */
   static Joints air(PlayerModel.Pose p) {
      Joints j = new Joints();
      float up = clamp(p.vy * 4.0F, -1.0F, 1.0F);
      float kick = 0.5F + 0.5F * up; // 1 rising fast, 0 falling fast
      float s = sin(p.limb * 1.6F);
      float t = p.age;
      for (int i = 0; i < 2; i++) {
         float lead = 0.5F + 0.5F * (i == 0 ? s : -s); // the leg that was forward when it left the ground stays forward
         j.hip[i] = 6.0F + 32.0F * kick * lead - 5.0F * (1.0F - kick) + 3.0F * sin(t * 0.4F + i * PI) * (1.0F - kick);
         j.knee[i] = 10.0F + 66.0F * kick * lead + 16.0F * (1.0F - kick);
         j.hipAbd[i] = 3.0F + 5.0F * (1.0F - kick);
         j.sh[i] = 18.0F + 40.0F * kick + 14.0F * (1.0F - kick) * sin(t * 0.35F + i * PI);
         j.abd[i] = 24.0F + 26.0F * (1.0F - kick);
         j.el[i] = 22.0F + 18.0F * kick;
      }

      j.tPitch = 3.0F + 5.0F * kick;
      return j;
   }

   /** Swimming: the body lies down on the water, the arms crawl, the legs kick. */
   static Joints swim(PlayerModel.Pose p) {
      Joints j = new Joints();
      float psi = p.limb * 1.6F * 0.6F;
      float t = p.age;
      j.pitch = 62.0F;
      j.dy = 3.0F + 0.8F * sin(t * 0.13F);
      for (int i = 0; i < 2; i++) {
         float ph = psi + i * PI;
         j.sh[i] = 95.0F - 90.0F * cos(ph); // a full circle: 5 degrees (by the hip) to 185 (over the head)
         float recover = Math.max(0.0F, sin(ph));
         j.el[i] = 14.0F + 80.0F * (float)Math.pow(recover, 0.8) + 24.0F * Math.max(0.0F, -sin(ph)) * Math.max(0.0F, cos(ph + 1.2F));
         j.abd[i] = 6.0F + 10.0F * recover;
         float kph = psi * 2.4F + i * PI;
         j.hip[i] = 4.0F + 16.0F * sin(kph);
         j.knee[i] = 14.0F + 16.0F * (0.5F + 0.5F * cos(kph + 0.8F));
         j.hipAbd[i] = 2.0F;
      }

      j.tYaw = 5.0F * sin(psi);
      j.tRoll = 4.0F * sin(psi);
      return j;
   }

   /** Treading water: upright, the arms sweep sideways, the legs pedal. */
   static Joints tread(PlayerModel.Pose p) {
      Joints j = new Joints();
      float t = p.age;
      j.dy = 1.0F * sin(t * 0.1F);
      for (int i = 0; i < 2; i++) {
         float ph = t * 0.2F + i * PI;
         j.abd[i] = 40.0F + 24.0F * sin(ph);
         j.sh[i] = 26.0F + 18.0F * sin(ph + 0.6F);
         j.el[i] = 36.0F + 20.0F * cos(ph);
         j.hip[i] = 18.0F * sin(ph + 0.4F) + 6.0F;
         j.knee[i] = 28.0F + 26.0F * Math.max(0.0F, sin(ph + 1.4F));
         j.hipAbd[i] = 4.0F;
      }

      j.tPitch = 4.0F;
      j.tRoll = 1.5F * sin(t * 0.1F);
      return j;
   }

   /** Climbing a ladder or a vine: the hands reach up one after the other, the knees come up one after the other. */
   static Joints climb(PlayerModel.Pose p) {
      Joints j = new Joints();
      float phi = p.limb * 1.6F;
      for (int i = 0; i < 2; i++) {
         float ph = phi + i * PI;
         j.sh[i] = 108.0F + 46.0F * sin(ph);
         j.el[i] = 52.0F - 38.0F * sin(ph);
         j.abd[i] = 8.0F;
         float lift = Math.max(0.0F, sin(ph + PI + 0.4F));
         j.hip[i] = 8.0F + 38.0F * lift;
         j.knee[i] = 10.0F + 72.0F * lift;
         j.hipAbd[i] = 3.0F;
      }

      j.tPitch = 6.0F;
      j.tYaw = 4.0F * sin(phi);
      j.tRoll = 2.0F * sin(phi);
      return j;
   }

   /** Flying (creative): the body leans into the flight, the legs trail behind, the arms are out a little. */
   static Joints fly(PlayerModel.Pose p) {
      Joints j = new Joints();
      float a = clamp(p.amp / 0.3F, 0.0F, 1.0F);
      float t = p.age;
      j.pitch = 30.0F * a;
      for (int i = 0; i < 2; i++) {
         j.hip[i] = -10.0F * a + 4.0F * sin(t * 0.07F + i * 2.0F);
         j.knee[i] = 6.0F + 12.0F * a + 4.0F * sin(t * 0.09F + i * 1.3F);
         j.hipAbd[i] = 3.0F;
         j.sh[i] = -8.0F * a + 6.0F * sin(t * 0.06F + i * 1.1F);
         j.abd[i] = 16.0F + 8.0F * sin(t * 0.05F + i * 2.1F);
         j.el[i] = 10.0F;
      }

      j.chest = 0.3F * sin(t * 0.09F);
      return j;
   }

   // ------------------------------------------------------------------ the arms that do something
   /** The swing of the right arm (hitting, mining, placing): wind up behind the shoulder, strike, come back. progress 0..1. */
   static void swing(Joints j, float progress, float weight) {
      float t = clamp(progress, 0.0F, 1.0F);
      float up = smooth(0.0F, 0.28F, t); // the arm goes up over the shoulder ...
      float chop = smooth(0.28F, 0.58F, t); // ... comes down in front of the body ...
      float back = smooth(0.58F, 1.0F, t); // ... and returns
      float sh = 128.0F * up * (1.0F - chop) + 38.0F * chop * (1.0F - back);
      float el = 78.0F * up * (1.0F - chop) + 18.0F * chop * (1.0F - back);
      float yaw = -12.0F * up * (1.0F - chop) + 16.0F * chop * (1.0F - back);
      j.sh[0] = lerp(j.sh[0], sh, weight);
      j.el[0] = lerp(j.el[0], el, weight);
      j.abd[0] = lerp(j.abd[0], 5.0F - 8.0F * chop * (1.0F - back), weight);
      j.tYaw += yaw * weight;
      j.tPitch += (-4.0F * up * (1.0F - chop) + 10.0F * chop * (1.0F - back)) * weight;
   }

   /** Eating or drinking: the right hand goes to the mouth and stays there, the head nods with every bite. progress 0..1. */
   static void eat(Joints j, float progress) {
      float k = smooth(0.0F, 0.1F, progress) * (1.0F - smooth(0.9F, 1.0F, progress));
      float bob = sin(progress * 32.0F * 0.9F);
      j.sh[0] = lerp(j.sh[0], 72.0F + 4.0F * bob, k);
      j.el[0] = lerp(j.el[0], 122.0F + 6.0F * bob, k);
      j.abd[0] = lerp(j.abd[0], -10.0F, k);
      j.headPitch += 5.0F * bob * k - 4.0F * k;
   }

   /** Drawing a bow: the right arm aims along the line of sight, the left hand pulls the string back to the cheek. draw 0..1 (full). */
   static void bow(Joints j, float draw, float lookPitch, float weight) {
      float k = smooth(0.0F, 0.08F, draw + 0.01F) * weight;
      float d = clamp(draw, 0.0F, 1.0F);
      float aim = 90.0F - lookPitch;
      j.sh[0] = lerp(j.sh[0], aim - 4.0F, k);
      j.el[0] = lerp(j.el[0], 4.0F, k);
      j.abd[0] = lerp(j.abd[0], -4.0F, k);
      j.sh[1] = lerp(j.sh[1], aim - 4.0F, k);
      j.el[1] = lerp(j.el[1], 16.0F + 100.0F * d, k);
      j.abd[1] = lerp(j.abd[1], -12.0F - 6.0F * d, k);
      j.tYaw += -14.0F * k;
      j.tPitch += 2.0F * k;
   }

   // ------------------------------------------------------------------ everything together
   /** How high the lowest foot is below the hips (model pixels) when the legs are at these angles. */
   static float reach(float hipDeg, float kneeDeg) {
      double h = Math.toRadians(hipDeg);
      double a = Math.toRadians(hipDeg - kneeDeg);
      return (float)(6.0 * Math.cos(h) + 6.0 * Math.cos(a) + 2.0 * Math.abs(Math.sin(a)));
   }

   static Joints solve(PlayerModel.Pose p) {
      float air = clamp(p.airK, 0.0F, 1.0F);
      float rest = 1.0F - air;
      float wSwim = rest * clamp(p.swim, 0.0F, 1.0F);
      rest -= wSwim;
      float wTread = rest * clamp(p.tread, 0.0F, 1.0F);
      rest -= wTread;
      float wClimb = rest * clamp(p.climb, 0.0F, 1.0F);
      rest -= wClimb;
      float wFly = rest * clamp(p.fly, 0.0F, 1.0F);
      rest -= wFly;
      float wGround = rest;
      Joints j = new Joints();
      j.add(ground(p), wGround);
      j.add(air(p), air);
      j.add(swim(p), wSwim);
      j.add(tread(p), wTread);
      j.add(climb(p), wClimb);
      j.add(fly(p), wFly);
      // carrying something: the forearm comes up a little
      if (p.held > 0) {
         j.sh[0] += 12.0F;
         j.el[0] += 26.0F;
      }

      float lookPitch = p.headPitch - j.tPitch - j.pitch;
      if (p.bow >= 0.0F) {
         bow(j, p.bow, clamp(lookPitch, -80.0F, 80.0F), 1.0F);
      } else if (p.eat >= 0.0F) {
         eat(j, p.eat);
      } else if (Renderer.isGun(p.held)) {
         float aim = 90.0F - clamp(lookPitch, -75.0F, 75.0F);
         // The lower arm rotates by shoulder + elbow, so compensate at the shoulder.
         // Otherwise the weapon pitches upward by the elbow bend even while looking level.
         j.el[0] = p.held == Redstone319.PISTOL ? 12.0F : 22.0F;
         j.sh[0] = aim - j.el[0];
         j.abd[0] = 0;
         j.el[1] = p.held == Redstone319.PISTOL ? 28.0F : 42.0F;
         j.sh[1] = aim - j.el[1];
         j.abd[1] = -22.0F;
         j.tYaw = 0;

      }

      if (p.swing >= 0.0F && p.bow < 0.0F && !Renderer.isGun(p.held)) {
         swing(j, p.swing, p.eat >= 0.0F ? 0.0F : 1.0F);
      }

      // a hit: the body is thrown back, the arms fly up
      float hurt = clamp(p.hurt, 0.0F, 1.0F);
      if (hurt > 0.0F) {
         j.tPitch -= 14.0F * hurt;
         j.headPitch -= 8.0F * hurt;
         j.sh[0] -= 20.0F * hurt;
         j.sh[1] -= 20.0F * hurt;
         j.abd[0] += 14.0F * hurt;
         j.abd[1] += 14.0F * hurt;
      }

      // limits of the joints
      for (int i = 0; i < 2; i++) {
         j.sh[i] = clamp(j.sh[i], -110.0F, 200.0F);
         j.el[i] = clamp(j.el[i], 0.0F, 150.0F);
         j.hip[i] = clamp(j.hip[i], -70.0F, 110.0F);
         j.knee[i] = clamp(j.knee[i], 0.0F, 150.0F);
         j.abd[i] = clamp(j.abd[i], -30.0F, 100.0F);
         j.hipAbd[i] = clamp(j.hipAbd[i], -10.0F, 40.0F);
      }

      // the body stands on the lower foot: the legs cannot stretch, so the body goes down as the legs spread or bend
      float lowest = Math.max(reach(j.hip[0], j.knee[0]), reach(j.hip[1], j.knee[1]));
      j.dy += (lowest - 12.0F) * wGround;
      return j;
   }
}
