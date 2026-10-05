package bc;

import bc.Anim317.MState;

/** Update 3.17: the clips of the End - the crystal and the dragon. */
final class AnimE317 {
   private AnimE317() {
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

   // ------------------------------------------------------------------ end crystal
   static void crystal(Rig r, MState s, Pose p) {
      float t = s.t;
      p.move(r.idx("bob"), 0.0F, sin(t * 0.1F) * 1.45F, 0.0F);
      p.rot(r.idx("spin0"), 0.0F, t * 3.0F % 360.0F, 0.0F);
      p.rot(r.idx("spin1"), 0.0F, -t * 4.0F % 360.0F, 0.0F);
      p.rot(r.idx("spin2"), 0.0F, t * 5.5F % 360.0F, 0.0F);
      float k = 1.0F + 0.1F * sin(t * 0.3F);
      p.scale(r.idx("core"), k, k, k);
   }

   // ------------------------------------------------------------------ ender dragon
   /** Where in the beat of a wing it is (radians, 0 = the top, pi = the bottom, 2 pi = the top again): the downstroke takes 60 % of the beat, the upstroke 40 %. */
   static float stroke(float ph) {
      float c = ph / (2.0F * (float)Math.PI);
      c -= (float)Math.floor(c);
      return c < 0.6F ? (float)Math.PI * c / 0.6F : (float)Math.PI + (float)Math.PI * (c - 0.6F) / 0.4F;
   }

   /** The height of a wing: +1 at the top of the beat, -1 at the bottom. */
   static float flap(float ph) {
      return cos(stroke(ph));
   }

   static float lerp(float a, float b, float t) {
      return a + (b - a) * t;
   }

   /**
    * Update 3.18: the Ender Dragon. A wing has three joints (shoulder, elbow, wrist) and three fingers; each joint follows the one before it a
    * little later, so the beat runs out along the wing like a wave, the wing is spread on the downstroke and folds up on the upstroke, the fingers
    * close as it rises. The body rises with the downstroke and sinks with the upstroke (nose up, nose down), leans into a turn (banking), noses up in
    * a climb and down in a dive; the five pieces of the neck and the nine of the tail wave with a delay, the head stays level against the neck's
    * motion, the legs dangle and swing a little behind. Perched it breathes, sways its neck, stretches its wings now and then and roars.
    */
   static void dragon(Rig r, MState s, Pose p) {
      float t = s.t;
      boolean dead = s.dead;
      boolean perched = s.phase == Dragon.PERCH && !dead;
      boolean landing = s.phase == Dragon.LAND && !dead;
      float flyK = perched ? 0.0F : 1.0F;
      float ph = perched ? t * 0.06F : s.walk * 0.5F;
      float u = stroke(ph);
      float lift = sin(u);
      float stretch = perched ? Anim317.moment(s, 6, 420.0F, 70.0F) : 0.0F;
      float stretchK = stretch > 0.0F ? sin(Math.PI * (1.0F - stretch)) : 0.0F;
      float fold = perched ? 1.0F - 0.6F * stretchK : (landing ? 0.35F : 0.0F);
      float amp = dead ? 0.12F : (perched ? 0.14F + 0.8F * stretchK : 1.0F);
      float climb = perched ? 0.0F : clamp(-s.vy * 45.0F, -28.0F, 28.0F);
      float bank = perched ? 0.0F : clamp(s.turn * 5.0F, -24.0F, 24.0F);
      float[] fan = {6.0F, 27.0F, 50.0F};
      for (int i = 0; i < 2; i++) {
         float sg = i == 0 ? 1.0F : -1.0F;
         String n = i == 0 ? "L" : "R";
         float e0 = flap(ph);
         float e1 = flap(ph - 0.55F);
         float e2 = flap(ph - 1.1F);
         float e3 = flap(ph - 1.5F);
         // 1 while the wing goes up (it folds then), 0 while it goes down
         float up = Anim317.smooth(-0.02F, 0.12F, flap(ph + 0.06F) - flap(ph - 0.06F)) * flyK;
         // the shoulder, the elbow, the wrist: flying (spread, beating) and perched (folded back along the body), mixed by fold
         p.rot(r.idx("wing" + n), 0.0F, sg * lerp(6.0F + 12.0F * up, 36.0F, fold), sg * lerp(amp * 30.0F * e0 + 4.0F, 12.0F + amp * 8.0F * e0, fold));
         p.rot(r.idx("wingMid" + n), 0.0F, sg * lerp(4.0F + 24.0F * up, 118.0F, fold), sg * lerp(amp * 24.0F * e1 + 3.0F, -6.0F, fold));
         p.rot(r.idx("wingTip" + n), 0.0F, sg * lerp(6.0F + 32.0F * up, 36.0F, fold), sg * lerp(amp * 20.0F * e2 + 2.0F, 0.0F, fold));
         for (int f = 0; f < 3; f++) {
            // the fingers close up on the upstroke and when the wing is folded, and flutter behind the wrist
            float close = Math.max(up * 0.55F, fold * 0.92F);
            p.rot(r.idx("finger" + f + n), 0.0F, -sg * fan[f] * close, sg * amp * (10.0F + 5.0F * f) * e3);
         }

         if (!perched) {
            // legs tucked under the body while it flies, stretched out when it lands; they dangle and swing a little behind the body
            float stretchL = landing ? 0.7F : 0.0F;
            float swing = sin(u - 1.1F);
            float drag = clamp(s.vy * 14.0F, -10.0F, 10.0F);
            p.rot(r.idx("legF" + n), -54.0F * (1.0F - stretchL) - 14.0F * stretchL + 5.0F * swing + drag, 0.0F, sg * 4.0F);
            p.rot(r.idx("shinF" + n), 78.0F * (1.0F - stretchL) + 20.0F * stretchL + 6.0F * sin(u - 1.6F), 0.0F, 0.0F);
            p.rot(r.idx("pawF" + n), -30.0F * (1.0F - stretchL) + 5.0F * sin(u - 2.0F), 0.0F, 0.0F);
            p.rot(r.idx("legH" + n), 28.0F * (1.0F - stretchL) - 18.0F * stretchL + 4.0F * sin(u - 1.4F) - drag, 0.0F, sg * 5.0F);
            p.rot(r.idx("shinH" + n), -58.0F * (1.0F - stretchL) + 20.0F * stretchL + 5.0F * sin(u - 1.9F), 0.0F, 0.0F);
            p.rot(r.idx("pawH" + n), 30.0F * (1.0F - stretchL) + 4.0F * sin(u - 2.3F), 0.0F, 0.0F);
         } else {
            float shift = sin(t * 0.021F + 1.3F) * 2.0F;
            p.rot(r.idx("legF" + n), 3.0F * sin(t * 0.05F), 0.0F, sg * 2.0F);
            p.rot(r.idx("legH" + n), -2.0F + sg * shift, 0.0F, sg * 2.0F);
         }
      }

      int body = r.idx("body");
      float pitch = clamp(s.headPitch, -35.0F, 35.0F);
      p.move(body, 0.0F, perched ? 0.35F * sin(t * 0.07F) : 1.1F * lift, 0.0F);
      p.rot(body, perched ? 4.0F : pitch * 0.45F + climb - 3.5F * lift, 0.0F, bank);
      float breathe = 1.0F + 0.012F * sin(t * 0.09F);
      p.scale(body, breathe, breathe, 1.0F);
      // the neck is an S that follows where the head looks; its pieces follow the wings a little later, one after the other
      float hy = clamp(s.headYaw, -55.0F, 55.0F);
      float rise = perched ? 1.0F : 0.0F;
      float sway = sin(t * 0.045F) * (perched ? 8.0F : 2.0F);
      float[] base = {-10.0F, 4.0F, 6.0F, 5.0F, 3.0F};
      float[] riseAdd = {-18.0F, 8.0F, 4.0F, 2.0F, 2.0F};
      float[] yawW = {0.12F, 0.16F, 0.2F, 0.24F, 0.28F};
      float wobble = 0.0F;
      for (int k = 0; k < 5; k++) {
         float wave = flyK * (2.2F + 0.5F * k) * sin(u - 0.5F - 0.4F * k);
         p.rot(r.idx("neck" + k), base[k] + rise * riseAdd[k] + pitch * 0.12F + wave, -hy * yawW[k] + sway * (0.25F + 0.1F * k), 0.0F);
         wobble += wave;
      }

      int head = r.idx("head");
      p.rot(head, pitch * 0.4F - 6.0F - 8.0F * rise - 0.55F * wobble, -hy * 0.2F, sin(t * 0.03F) * 2.0F);
      float roar = perched ? Anim317.moment(s, 5, 70.0F, 28.0F) : 0.0F;
      float rest = 5.0F + sin(t * 0.12F) * 3.0F;
      float bite = s.attackT > 0.0F ? Math.min(1.0F, s.attackT / 6.0F) * Anim317.smooth(10.0F, 9.0F, s.attackT) : 0.0F;
      float open = rest + (30.0F - rest) * bite;
      if (roar > 0.0F) {
         open = Math.max(open, 36.0F * sin(Math.PI * (1.0F - roar)));
         p.rot(head, -16.0F * sin(Math.PI * (1.0F - roar)), 0.0F, 0.0F);
      }

      if (dead) {
         open = 28.0F;
      }

      p.rot(r.idx("jaw"), open, 0.0F, 0.0F);
      // the tail whips from side to side, more and more towards the tip, bobs with a delay after the wings and trails behind a turn
      float lag = clamp(s.turn, -4.0F, 4.0F) * flyK;
      for (int k = 0; k < 9; k++) {
         float w = sin(s.walk * 0.3F - k * 0.55F) * (1.8F + 0.45F * k);
         float l = flyK * (1.0F + 0.55F * k) * sin(u - 0.8F - 0.55F * k) + climb * 0.1F;
         p.rot(r.idx("tail" + k), (perched ? -2.0F : 0.0F) + l, w * (perched ? 0.4F : 1.0F) + (perched ? sin(t * 0.04F - k * 0.5F) * (1.2F + k * 0.6F) : 0.0F) - lag * 0.45F, 0.0F);
      }

      if (dead) {
         float dt = Math.min(1.0F, s.deathT / (float)Dragon.DEATH_TICKS);
         float k = 0.8F + 0.9F * dt;
         p.rootY = sin(s.deathT * 1.9F) * 0.08F * dt + 0.0001F;
         p.tintR = k;
         p.tintG = k / 0.45F;
         p.tintB = k / 0.45F;
      }
   }
}
