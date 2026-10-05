package bc;

import bc.Anim317.MState;

/** Update 3.17: the clips of the pigman, the creatures of the Nether and the enderman (see {@link AnimM317} for the shared gait). */
final class AnimW317 {
   private AnimW317() {
   }

   private static float sin(double a) {
      return (float)Math.sin(a);
   }

   private static float cos(double a) {
      return (float)Math.cos(a);
   }

   private static float clamp(float v, float lo, float hi) {
      return Math.max(lo, Math.min(hi, v));
   }

   // ------------------------------------------------------------------ zombie pigman
   static void pigman(Rig r, MState s, Pose p) {
      float phi = s.walk * 1.6F;
      float a = s.amp;
      float calm = 1.0F - Math.min(1.0F, a * 2.0F);
      AnimM317.gait(r, s, p, 30.0F, 38.0F, 0.45F, 3.0F);
      boolean mad = s.angry && s.target;
      float k = s.attackT > 0.0F ? 1.0F - s.attackT / 8.0F : 1.0F;
      float swing = s.attackT > 0.0F ? Anim317.smooth(0.0F, 0.75F, k) : 1.0F;
      float chop = s.attackT > 0.0F ? sin(Math.PI * k) : 0.0F;
      int chest = r.idx("chest");
      p.rot(chest, (mad ? 9.0F : 2.0F) + 5.0F * a + 12.0F * chop + sin(s.t * 0.1F + s.seed) * 1.2F, 0.0F, 0.0F);
      float sway = sin(s.t * 0.08F + s.seed);
      // the left arm reaches out when it is angry, the right one holds the sword up and brings it down on the target
      float armLx = mad ? -74.0F + sin(s.t * 0.2F) * 3.0F : -8.0F + sin(phi + 3.1416F) * 22.0F * a + sway * 2.0F;
      float armRx = mad ? -96.0F + 3.0F * sin(s.t * 0.17F) : -8.0F + sin(phi) * 22.0F * a - sway * 2.0F;
      if (s.attackT > 0.0F) {
         // raised, brought down, back to the way it stood: blended in and out so that the arm does not jump
         float env = Anim317.smooth(0.0F, 0.15F, k) * (1.0F - Anim317.smooth(0.8F, 1.0F, k));
         float down = Anim317.smooth(0.2F, 0.75F, k);
         armRx = armRx * (1.0F - env) + (-150.0F + 128.0F * down) * env;
         armLx -= 14.0F * chop;
      }

      p.rot(r.idx("armL"), armLx, mad ? -5.0F : 0.0F, 4.0F);
      p.rot(r.idx("armR"), armRx, mad ? 4.0F : 0.0F, -4.0F);
      p.rot(r.idx("foreL"), mad ? -12.0F : -10.0F - 8.0F * a, 0.0F, 0.0F);
      p.rot(r.idx("foreR"), mad ? -16.0F + (s.attackT > 0.0F ? 30.0F * chop : 0.0F) : -12.0F - 8.0F * a, 0.0F, 0.0F);
      AnimM317.head(r, s, p, mad ? -4.0F : 3.0F + 3.0F * calm * sin(s.t * 0.05F + s.seed), 3.0F, 0.04F);
      float twitch = Anim317.moment(s, 11, 150.0F, 12.0F);
      p.rot(r.idx("earL"), 0.0F, 0.0F, 14.0F * sin(twitch * 3.1416F * 3.0F) * (twitch > 0.0F ? 1.0F : 0.0F) + (mad ? 10.0F : 0.0F));
      p.rot(r.idx("earR"), 0.0F, 0.0F, -14.0F * sin(twitch * 3.1416F * 3.0F + 1.0F) * (twitch > 0.0F ? 1.0F : 0.0F) * Anim317.smooth(0.0F, 0.1F, 1.0F - twitch) * Anim317.smooth(0.0F, 0.1F, twitch) - (mad ? 10.0F : 0.0F));
   }

   // ------------------------------------------------------------------ ghast
   static void ghast(Rig r, MState s, Pose p) {
      float t = s.t;
      float charge = s.fuse > 0.0F ? 1.0F - Math.min(1.0F, s.fuse / 20.0F) : 0.0F;
      int body = r.idx("body");
      float bob = sin(t * 0.06F + s.seed) * 3.0F;
      p.move(body, 0.0F, bob + charge * 2.5F, 0.0F);
      float look = clamp(s.headYaw, -50.0F, 50.0F);
      p.rot(body, clamp(s.headPitch, -30.0F, 30.0F) * 0.5F + s.amp * 7.0F + sin(t * 0.05F + 1.0F) * 1.5F - 6.0F * charge, -look * 0.6F, sin(t * 0.045F + s.seed) * 2.5F);
      float puff = 1.0F + 0.035F * charge + 0.012F * sin(t * 0.12F);
      p.scale(body, puff, puff + 0.03F * charge, puff);
      if (charge > 0.0F) {
         p.move(body, sin(t * 2.6F) * charge * 1.1F, 0.0F, sin(t * 3.1F + 1.0F) * charge * 1.1F);
      } else {
         p.hide |= 1 << Nether317.G_CHARGE;
      }

      for (int i = 0; i < 9; i++) {
         for (int k = 0; k < 4; k++) {
            float ph = t * 0.09F + i * 0.8F + k * 0.65F + s.seed * 0.3F;
            float lag = (k + 1) * s.amp * 5.0F;
            p.rot(r.idx("t" + i + k), sin(ph) * (4.0F + k * 2.5F) - lag + charge * (4.0F + 2.0F * k), 0.0F, cos(ph * 0.83F + 1.0F) * (3.0F + k * 2.2F));
         }
      }
   }

   // ------------------------------------------------------------------ blaze
   static void blaze(Rig r, MState s, Pose p) {
      float t = s.t;
      // the burst: the AI keeps the fuse at 18 down to 1 while it fires; it swells and dies away over a few ticks instead of switching on and off
      float fireK = Math.max(Anim317.smooth(0.0F, 4.0F, s.fuse) * Anim317.smooth(18.0F, 14.0F, s.fuse), Anim317.smooth(0.0F, 1.5F, s.attackT) * Anim317.smooth(10.0F, 8.5F, s.attackT));
      int head = r.idx("head");
      p.move(head, 0.0F, sin(t * 0.1F + s.seed) * 0.9F, 0.0F);
      p.rot(head, s.headPitch - 6.0F * fireK, -clamp(s.headYaw, -60.0F, 60.0F) + sin(t * 3.0F) * 4.0F * fireK, sin(t * 0.07F) * 3.0F);
      float[] speed = {3.0F, -4.4F, 5.6F};
      for (int ring = 0; ring < 3; ring++) {
         p.rot(r.idx("ring" + ring), 0.0F, (t * speed[ring] + ring * 20.0F) % 360.0F, 0.0F); // wrapped: after hours the angle would lose its fraction
         for (int k = 0; k < 4; k++) {
            double a = k * Math.PI / 2.0 + ring * 0.5;
            float out = 2.6F * fireK;
            int rod = r.idx("rod" + ring + k);
            p.move(rod, (float)Math.cos(a) * out, sin(t * 0.17F + k * 1.7F + ring * 0.9F) * 0.9F + 1.5F * fireK, (float)Math.sin(a) * out);
            p.rot(rod, sin(t * 0.13F + k + ring) * 4.0F, 0.0F, cos(t * 0.11F + k * 2.0F) * 4.0F);
         }
      }

      // the crown and the flares flicker (and leap up when it fires), the core pulses and turns, the embers circle the other way
      int core = r.idx("core");
      p.move(core, 0.0F, sin(t * 0.1F + s.seed) * 0.7F, 0.0F);
      p.scale(core, 1.0F + 0.05F * sin(t * 0.31F), 1.0F + 0.09F * sin(t * 0.37F + 1.0F) + 0.12F * fireK, 1.0F + 0.05F * sin(t * 0.31F));
      p.rot(core, 0.0F, t * 1.5F % 360.0F, 0.0F);
      p.scale(r.idx("crown"), 1.0F, 0.9F + 0.18F * sin(t * 0.52F) + 0.3F * fireK, 1.0F);
      for (int k = 0; k < 4; k++) {
         int f = r.idx("flame" + k);
         p.scale(f, 1.0F, 0.85F + 0.25F * sin(t * 0.47F + k * 1.9F) + 0.35F * fireK, 1.0F);
         p.rot(f, 4.0F * sin(t * 0.31F + k * 1.3F), 0.0F, 4.0F * cos(t * 0.27F + k * 2.1F));
      }

      for (int k = 0; k < 2; k++) {
         p.scale(r.idx(k == 0 ? "cheekL" : "cheekR"), 1.0F, 0.9F + 0.2F * sin(t * 0.41F + k * 2.0F) + 0.4F * fireK, 1.0F);
      }

      p.rot(r.idx("embers"), 0.0F, -t * 1.1F % 360.0F, 0.0F);
      for (int k = 0; k < 6; k++) {
         p.move(r.idx("ember" + k), 0.0F, sin(t * 0.09F + k * 1.3F) * 1.8F, 0.0F);
      }
   }

   // ------------------------------------------------------------------ magma cube
   static void magma(Rig r, MState s, Pose p) {
      float t = s.t;
      float stretch;
      float crouch;
      if (s.onGround) {
         stretch = 0.0F;
         crouch = 0.05F + 0.2F * Anim317.smooth(7.0F, 0.0F, s.cd) + 0.03F * sin(t * 0.15F + s.seed);
      } else {
         stretch = clamp(s.vy * 0.85F, -0.3F, 0.38F);
         crouch = 0.0F;
      }

      float bite = s.attackT > 0.0F ? sin(Math.PI * (1.0F - s.attackT / 8.0F)) * 0.18F : 0.0F;
      float sy = 1.0F + stretch - crouch + bite;
      float sxz = 1.0F / (float)Math.sqrt(Math.max(0.3F, sy));
      p.rootScaleY = sy;
      p.rootScaleX = sxz;
      p.rootScaleZ = sxz;
      float jelly = 0.05F + 0.12F * Math.abs(stretch) + 0.2F * crouch;
      for (int i = 0; i < 5; i++) {
         float w = sin(t * 0.55F - i * 0.8F) * jelly;
         p.scale(r.idx("slice" + i), 1.0F + w, 1.0F, 1.0F - w);
         p.move(r.idx("slice" + i), 0.0F, (i - 2) * (stretch * 0.5F - crouch * 0.6F) * 0.4F, 0.0F);
      }

      float pulse = 1.0F + 0.07F * sin(t * 0.2F + s.seed);
      p.scale(r.idx("core"), pulse, pulse, pulse);
   }

   // ------------------------------------------------------------------ enderman
   static void enderman(Rig r, MState s, Pose p) {
      float phi = s.walk * 1.6F;
      float a = s.amp;
      float calm = 1.0F - Math.min(1.0F, a * 2.0F);
      AnimM317.gait(r, s, p, 30.0F, 30.0F, 0.7F, 2.0F);
      boolean mad = s.angry;
      float lunge = s.attackT > 0.0F ? sin(Math.PI * (1.0F - s.attackT / 8.0F)) : 0.0F;
      int chest = r.idx("chest");
      p.rot(chest, 2.0F + (mad ? 9.0F : 0.0F) + 4.0F * a + 12.0F * lunge, 0.0F, 0.0F);
      for (int i = 0; i < 2; i++) {
         float sg = i == 0 ? 1.0F : -1.0F;
         String n = i == 0 ? "L" : "R";
         float th = phi + (i == 0 ? 3.1416F : 0.0F);
         float sway = sin(s.t * 0.05F + i * 2.1F + s.seed);
         float armX = sin(th) * 24.0F * a + sway * 2.5F - 4.0F;
         float armZ = sg * (3.0F + 2.0F * calm);
         float elbow = -8.0F - 16.0F * a * Math.max(0.0F, -sin(th)) - 5.0F * calm * (0.5F + 0.5F * sin(s.t * 0.07F + i));
         if (mad) {
            armX = -28.0F + sin(s.t * 0.6F + i * 1.9F) * 8.0F - 55.0F * lunge;
            armZ = sg * (16.0F + 6.0F * sin(s.t * 0.5F + i));
            elbow = -22.0F - 20.0F * lunge;
         }

         p.rot(r.idx("arm" + n), armX, 0.0F, armZ);
         p.rot(r.idx("fore" + n), elbow, 0.0F, 0.0F);
      }

      AnimM317.head(r, s, p, mad ? -10.0F : 0.0F, mad ? 0.0F : 5.0F, 0.03F);
      int head = r.idx("head");
      if (mad) {
         p.rot(head, sin(s.t * 2.9F) * 2.8F, sin(s.t * 3.7F) * 3.4F, sin(s.t * 2.3F) * 2.2F);
         p.move(head, sin(s.t * 3.3F) * 0.25F, 0.0F, sin(s.t * 2.7F) * 0.25F);
      }

      float jawOpen = mad ? 30.0F + 8.0F * sin(s.t * 0.5F) : 0.0F;
      float tilt = Anim317.moment(s, 13, 320.0F, 40.0F);
      if (tilt > 0.0F) {
         p.rot(head, 0.0F, 0.0F, 14.0F * sin(Math.PI * (1.0F - tilt)));
      }

      p.rot(r.idx("jaw"), jawOpen + 3.0F * (float)Math.max(0.0F, sin(s.t * 0.09F + s.seed) - 0.9F) * 10.0F, 0.0F, 0.0F);
   }
}
