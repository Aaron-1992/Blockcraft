package bc;

import bc.Anim317.MState;

/** Update 3.17: the clips of the villager, the iron golem and the parrot. */
final class AnimF317 {
   private AnimF317() {
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

   // ------------------------------------------------------------------ villager
   static void villager(Rig r, MState s, Pose p) {
      float phi = s.walk * 1.6F;
      float a = s.amp;
      float calm = 1.0F - Math.min(1.0F, a * 2.0F);
      float panic = s.flee ? 1.0F : 0.0F;
      for (int i = 0; i < 2; i++) {
         float th = phi + (i == 0 ? 0.0F : 3.1416F);
         p.rot(r.idx(i == 0 ? "legL" : "legR"), -28.0F * a * sin(th), 0.0F, 0.0F);
      }

      int body = r.idx("body");
      int chest = r.idx("chest");
      p.move(body, 0.0F, 0.35F * a * (0.5F + 0.5F * cos(2.0F * phi)), 0.0F);
      p.rot(body, sin(2.0F * phi) * 1.0F * a, 0.0F, sin(phi) * 2.2F * a);
      p.rot(chest, 2.0F * calm * sin(s.t * 0.05F + s.seed) * 0.0F + 5.0F * panic * a + 1.5F * a, sin(phi) * 3.0F * a, -sin(phi) * 1.5F * a);
      p.scale(chest, 1.0F, 1.0F + 0.012F * sin(s.t * 0.1F + s.seed), 1.0F);
      int arms = r.idx("arms");
      if (s.flee) {
         // the arms are thrown up and flap
         p.rot(arms, -22.0F + sin(s.t * 0.9F) * 26.0F, sin(s.t * 0.7F) * 8.0F, sin(s.t * 0.6F) * 6.0F);
         p.move(arms, 0.0F, 1.2F + sin(s.t * 0.9F) * 0.8F, 0.5F);
      } else {
         p.rot(arms, sin(s.t * 0.07F + s.seed) * 2.0F + 3.0F * a * sin(2.0F * phi), 0.0F, 0.0F);
         p.move(arms, 0.0F, sin(2.0F * phi) * 0.25F * a, 0.0F);
      }

      int head = r.idx("head");
      float look = clamp(s.headYaw, -60.0F, 60.0F);
      float glance = Anim317.moment(s, 3, 240.0F, 60.0F);
      float turn = glance > 0.0F ? 28.0F * sin(Math.PI * (1.0F - glance)) * (RigPaint.hash(s.seed, (int)(s.t / 240.0F), 3, 4) > 0.5F ? 1.0F : -1.0F) : 0.0F;
      float shake = s.hurt * 22.0F * sin(s.t * 1.9F);
      p.rot(head, s.headPitch + 2.0F + (panic > 0.0F ? -8.0F : 0.0F) - 2.0F * a * sin(2.0F * phi), -look * (1.0F - 0.0F) + turn * calm + shake, 1.5F * sin(s.t * 0.04F + s.seed));
      float sniff = Anim317.moment(s, 4, 170.0F, 16.0F);
      int nose = r.idx("nose");
      p.scale(nose, 1.0F, 1.0F + (sniff > 0.0F ? 0.22F * sin(sniff * 3.1416F * 4.0F) : 0.0F), 1.0F);
      p.rot(nose, sniff > 0.0F ? 6.0F * sin(sniff * 3.1416F * 5.0F) : 0.0F, 0.0F, 0.0F);
      // what the profession wears
      int keep;
      switch (s.color) {
         case 0:
            keep = Folk317.V_HAT;
            break;
         case 1:
            keep = Folk317.V_GLASSES;
            break;
         case 2:
            keep = Folk317.V_CAP;
            break;
         case 3:
            keep = Folk317.V_SMITH;
            break;
         case 4:
            keep = Folk317.V_BUTCHER;
            break;
         default:
            keep = -1;
            break;
      }

      int all = 1 << Folk317.V_GLASSES | 1 << Folk317.V_CAP | 1 << Folk317.V_HAT | 1 << Folk317.V_SMITH | 1 << Folk317.V_BUTCHER;
      p.hide |= all & (keep < 0 ? all : ~(1 << keep));
   }

   // ------------------------------------------------------------------ iron golem
   static void golem(Rig r, MState s, Pose p) {
      float phi = s.walk * 1.6F;
      float a = s.amp;
      float calm = 1.0F - Math.min(1.0F, a * 2.0F);
      float k = s.attackT > 0.0F ? 1.0F - s.attackT / 10.0F : -1.0F;
      float raise = k >= 0.0F ? Anim317.smooth(0.0F, 0.45F, k) : 0.0F;
      float slam = k >= 0.0F ? Anim317.smooth(0.45F, 0.8F, k) : 0.0F;
      float swing = raise * (1.0F - slam);
      for (int i = 0; i < 2; i++) {
         float th = phi + (i == 0 ? 0.0F : 3.1416F);
         String n = i == 0 ? "L" : "R";
         p.rot(r.idx("leg" + n), -24.0F * a * sin(th), 0.0F, 0.0F);
         float armSwing = sin(th + 3.1416F) * 20.0F * a;
         float armX = armSwing - 4.0F * calm * sin(s.t * 0.05F + i * 1.3F + s.seed);
         float fore = -5.0F - 7.0F * a * Math.max(0.0F, sin(th + 3.1416F)) - 3.0F * calm;
         if (k >= 0.0F) {
            armX = -118.0F * swing + 40.0F * slam * raise - armSwing * (1.0F - raise);
            fore = -26.0F * swing - 6.0F;
         }

         p.rot(r.idx("arm" + n), armX, 0.0F, (i == 0 ? 1.0F : -1.0F) * (3.0F + 2.0F * calm + 6.0F * a));
         p.rot(r.idx("fore" + n), fore, 0.0F, 0.0F);
      }

      int body = r.idx("body");
      int chest = r.idx("chest");
      p.move(body, 0.0F, 0.9F * a * Math.abs(sin(phi)) - 0.9F * slam * (1.0F - slam) * 3.0F, 0.0F);
      p.rot(body, 0.0F, -sin(phi) * 3.0F * a, sin(phi) * 2.0F * a);
      p.rot(chest, 3.0F + 2.0F * a + 14.0F * slam * (1.0F - slam) * 3.0F - 8.0F * swing, sin(phi) * 5.0F * a, -sin(phi) * 2.0F * a);
      p.scale(chest, 1.0F + 0.008F * sin(s.t * 0.08F + s.seed), 1.0F + 0.012F * sin(s.t * 0.08F + s.seed), 1.0F);
      float look = clamp(s.headYaw, -45.0F, 45.0F);
      float glance = Anim317.moment(s, 3, 300.0F, 70.0F);
      float turn = glance > 0.0F ? 20.0F * sin(Math.PI * (1.0F - glance)) * (RigPaint.hash(s.seed, (int)(s.t / 300.0F), 3, 4) > 0.5F ? 1.0F : -1.0F) : 0.0F;
      p.rot(r.idx("head"), s.headPitch - 2.0F * a - 12.0F * swing + 10.0F * slam * (1.0F - slam) * 3.0F, -look + turn * calm, 0.0F);
   }

   // ------------------------------------------------------------------ parrot
   static void parrot(Rig r, MState s, Pose p) {
      float t = s.t;
      boolean fly = s.phase == Mobs312.P_FLY && !s.onGround;
      int body = r.idx("body");
      int head = r.idx("head");
      float look = clamp(s.headYaw, -70.0F, 70.0F);
      float flap = sin(t * 1.35F + s.seed);
      if (fly) {
         float pitch = 58.0F;
         p.rot(body, pitch + 4.0F * flap, 0.0F, sin(t * 0.07F) * 4.0F);
         p.move(body, 0.0F, 0.5F * flap, 0.0F);
         p.rot(head, -pitch * 0.85F + s.headPitch, -look, 0.0F);
         for (int i = 0; i < 2; i++) {
            float sg = i == 0 ? 1.0F : -1.0F;
            String n = i == 0 ? "L" : "R";
            p.rot(r.idx("wing" + n), 0.0F, 0.0F, sg * (68.0F + 44.0F * flap));
            p.rot(r.idx("wingTip" + n), 0.0F, 0.0F, sg * (14.0F + 26.0F * sin(t * 1.35F + s.seed - 0.9F)));
            p.rot(r.idx("leg" + n), -62.0F, 0.0F, 0.0F);
         }

         p.rot(r.idx("tail"), 12.0F + 8.0F * flap, sin(t * 0.2F) * 6.0F, 0.0F);
         p.rot(r.idx("jawP"), 0.0F, 0.0F, 0.0F);
         return;
      }

      // sitting: on the ground, on a branch or on the shoulder of the player
      float breath = sin(t * 0.12F + s.seed) * 0.5F + 0.5F;
      p.rot(body, 10.0F + 1.2F * breath, 0.0F, 0.0F);
      p.scale(body, 1.0F + 0.015F * breath, 1.0F + 0.02F * breath, 1.0F);
      float peck = Anim317.moment(s, 2, 110.0F, 14.0F);
      float peckK = peck > 0.0F ? sin(Math.PI * (1.0F - peck)) : 0.0F;
      float glance = Anim317.moment(s, 3, 150.0F, 50.0F);
      float turn = glance > 0.0F ? 55.0F * Anim317.smooth(0.0F, 0.2F, 1.0F - glance) * Anim317.smooth(0.0F, 0.2F, glance) * (RigPaint.hash(s.seed, (int)(t / 150.0F), 3, 4) > 0.5F ? 1.0F : -1.0F) : 0.0F;
      float preen = Anim317.moment(s, 5, 520.0F, 90.0F);
      float preenK = preen > 0.0F ? Anim317.smooth(0.0F, 0.15F, 1.0F - preen) * Anim317.smooth(0.0F, 0.15F, preen) : 0.0F;
      float squawk = Anim317.moment(s, 6, 420.0F, 24.0F);
      float squawkK = squawk > 0.0F ? sin(Math.PI * (1.0F - squawk)) : 0.0F;
      float bob = (float)Math.max(0.0F, sin(t * 0.4F + s.seed)) * (s.moving || s.amp > 0.1F ? 3.0F : 0.0F);
      p.rot(head, s.headPitch - 10.0F + 30.0F * peckK + bob - 22.0F * squawkK + 28.0F * preenK, -look + turn + 110.0F * preenK, 4.0F * sin(t * 0.05F + s.seed) + 12.0F * preenK);
      p.rot(r.idx("jawP"), 26.0F * squawkK + (preenK > 0.0F ? 10.0F * Math.abs(sin(t * 1.6F)) : 0.0F), 0.0F, 0.0F);
      p.rot(r.idx("tail"), 16.0F + 5.0F * sin(t * 0.17F + s.seed) * Math.max(0.0F, sin(t * 0.03F)) + 10.0F * squawkK, sin(t * 0.13F) * 3.0F * Math.max(0.0F, sin(t * 0.02F + 1.0F)), 0.0F);
      float stretch = Anim317.moment(s, 7, 600.0F, 40.0F);
      float stretchK = stretch > 0.0F ? sin(Math.PI * (1.0F - stretch)) : 0.0F;
      for (int i = 0; i < 2; i++) {
         float sg = i == 0 ? 1.0F : -1.0F;
         String n = i == 0 ? "L" : "R";
         float twitch = sg * (3.0F + 2.0F * breath) + sg * 55.0F * stretchK + (preenK > 0.0F && i == 0 ? sg * 22.0F * preenK : 0.0F);
         p.rot(r.idx("wing" + n), 0.0F, 0.0F, twitch);
         p.rot(r.idx("wingTip" + n), 0.0F, 0.0F, sg * (-2.0F + 30.0F * stretchK));
         p.rot(r.idx("leg" + n), -sin(s.walk * 1.6F + (i == 0 ? 0.0F : 3.1416F)) * 28.0F * s.amp, 0.0F, 0.0F);
      }
   }
}
