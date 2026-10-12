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
      float thrust = cos(u);
      float stretch = perched ? Anim317.moment(s, 6, 420.0F, 70.0F) : 0.0F;
      float stretchK = stretch > 0.0F ? sin(Math.PI * (1.0F - stretch)) : 0.0F;
      float fold = perched ? 1.0F - 0.6F * stretchK : (landing ? 0.35F : 0.0F);
      float amp = dead ? 0.12F : (perched ? 0.16F + 0.8F * stretchK : 1.0F);
      float climb = perched ? 0.0F : clamp(-s.vy * 48.0F, -28.0F, 28.0F);
      float bank = perched ? 0.0F : clamp(s.turn * 5.5F, -26.0F, 26.0F);
      float[] fan = {6.0F, 27.0F, 50.0F};
      for (int i = 0; i < 2; i++) {
         float sg = i == 0 ? 1.0F : -1.0F;
         String n = i == 0 ? "L" : "R";
         float e0 = flap(ph);
         float e1 = flap(ph - 0.65F);
         float e2 = flap(ph - 1.25F);
         float e3 = flap(ph - 1.65F);
         // 1 while the wing goes up (it folds then), 0 while it goes down
         float up = Anim317.smooth(-0.02F, 0.12F, flap(ph + 0.06F) - flap(ph - 0.06F)) * flyK;
         // 3D figure-8 wing stroke: shoulder sweeps forward on the power downstroke and pitches camber along the wing
         float sweepX = flyK * amp * (11.0F * sin(u) - 4.0F * cos(u));
         float midPitchX = flyK * amp * 8.5F * sin(u - 0.55F);
         float tipPitchX = flyK * amp * 7.0F * sin(u - 1.1F);
         p.rot(r.idx("wing" + n), sweepX, sg * lerp(5.0F + 16.0F * up, 36.0F, fold), sg * lerp(amp * 38.0F * e0 + 4.0F, 12.0F + amp * 8.0F * e0, fold));
         p.rot(r.idx("wingMid" + n), midPitchX, sg * lerp(4.0F + 30.0F * up, 118.0F, fold), sg * lerp(amp * 32.0F * e1 + 3.0F, -6.0F, fold));
         p.rot(r.idx("wingTip" + n), tipPitchX, sg * lerp(6.0F + 38.0F * up, 36.0F, fold), sg * lerp(amp * 26.0F * e2 + 2.0F, 0.0F, fold));
         for (int f = 0; f < 3; f++) {
            // the fingers close up on the upstroke and when the wing is folded, and flutter behind the wrist
            float close = Math.max(up * 0.62F, fold * 0.92F);
            float fe = flap(ph - 1.55F - 0.15F * f);
            p.rot(r.idx("finger" + f + n), flyK * (3.0F + 1.5F * f) * sin(u - 1.2F - 0.2F * f), -sg * fan[f] * close, sg * amp * (13.0F + 6.0F * f) * fe);
         }

         if (!perched) {
            // legs tucked under the body while it flies, stretched out when it lands; they pulse and trail with every wingbeat
            float stretchL = landing ? 0.72F : 0.0F;
            float sidePh = i * 0.35F;
            float swing = sin(u - 1.05F + sidePh);
            float drag = clamp(s.vy * 16.0F, -12.0F, 12.0F);
            p.rot(r.idx("legF" + n), -52.0F * (1.0F - stretchL) - 12.0F * stretchL + 10.0F * swing + drag, sg * 3.0F * cos(u + sidePh), sg * (5.0F + 3.0F * lift));
            p.rot(r.idx("shinF" + n), 76.0F * (1.0F - stretchL) + 22.0F * stretchL + 12.0F * sin(u - 1.55F + sidePh), 0.0F, 0.0F);
            p.rot(r.idx("pawF" + n), -28.0F * (1.0F - stretchL) + 10.0F * sin(u - 2.0F + sidePh), 0.0F, 0.0F);
            p.rot(r.idx("legH" + n), 26.0F * (1.0F - stretchL) - 18.0F * stretchL + 9.0F * sin(u - 1.35F - sidePh) - drag, -sg * 3.0F * sin(u), sg * (6.0F + 3.0F * lift));
            p.rot(r.idx("shinH" + n), -56.0F * (1.0F - stretchL) + 22.0F * stretchL + 11.0F * sin(u - 1.85F - sidePh), 0.0F, 0.0F);
            p.rot(r.idx("pawH" + n), 28.0F * (1.0F - stretchL) + 9.0F * sin(u - 2.25F - sidePh), 0.0F, 0.0F);
         } else {
            float shift = sin(t * 0.035F + 1.3F) * 4.5F;
            float kneeFlex = 6.0F + 4.0F * sin(t * 0.07F + i * 1.2F);
            p.rot(r.idx("legF" + n), -6.0F + 4.5F * sin(t * 0.05F + i), sg * 3.0F, sg * 3.5F);
            p.rot(r.idx("shinF" + n), kneeFlex, 0.0F, 0.0F);
            p.rot(r.idx("pawF" + n), -kneeFlex * 0.6F, 0.0F, 0.0F);
            p.rot(r.idx("legH" + n), -4.0F + sg * shift, -sg * 2.5F, sg * 3.5F);
            p.rot(r.idx("shinH" + n), -kneeFlex * 0.7F, 0.0F, 0.0F);
            p.rot(r.idx("pawH" + n), kneeFlex * 0.5F, 0.0F, 0.0F);
         }
      }

      int body = r.idx("body");
      float pitch = clamp(s.headPitch, -35.0F, 35.0F);
      float bodyHeave = perched ? 0.85F * sin(t * 0.07F) : 3.2F * lift;
      float bodySurge = perched ? 0.0F : 0.9F * thrust;
      p.move(body, 0.0F, bodyHeave, bodySurge);
      p.rot(body, perched ? 4.5F + 2.0F * sin(t * 0.07F) : pitch * 0.55F + climb - 9.0F * lift, sin(t * 0.033F) * (perched ? 3.5F : 2.5F), bank + (perched ? 1.8F * cos(t * 0.041F) : 2.5F * sin(t * 0.037F)));
      float breathe = 1.0F + 0.018F * sin(t * 0.09F);
      p.scale(body, breathe, breathe, 1.0F);
      // the neck is an S that follows where the head looks; its pieces follow the wings with a travelling wave and serpentine sway
      float hy = clamp(s.headYaw, -55.0F, 55.0F);
      float rise = perched ? 1.0F : 0.0F;
      float[] base = {-10.0F, 4.0F, 6.0F, 5.0F, 3.0F};
      float[] riseAdd = {-18.0F, 8.0F, 4.0F, 2.0F, 2.0F};
      float[] yawW = {0.14F, 0.18F, 0.22F, 0.26F, 0.30F};
      float wobble = 0.0F;
      for (int k = 0; k < 5; k++) {
         float wave = flyK * (4.4F + 0.95F * k) * sin(u - 0.45F - 0.42F * k) + (perched ? 1.8F * sin(t * 0.07F - 0.35F * k) : 0.0F);
         float sway = sin(t * 0.052F - 0.32F * k) * (perched ? 9.5F : 3.6F);
         p.rot(r.idx("neck" + k), base[k] + rise * riseAdd[k] + pitch * 0.14F + wave, -hy * yawW[k] + sway * (0.28F + 0.11F * k), -bank * 0.1F + sin(t * 0.04F - 0.3F * k) * 1.6F);
         wobble += wave;
      }

      int head = r.idx("head");
      p.rot(head, pitch * 0.45F - 6.0F - 8.0F * rise - 0.58F * wobble, -hy * 0.24F, sin(t * 0.035F) * 3.2F - bank * 0.15F);
      float roar = perched ? Anim317.moment(s, 5, 70.0F, 28.0F) : 0.0F;
      float rest = 6.0F + sin(t * 0.12F) * 3.5F + flyK * 4.0F * Math.max(0.0F, -lift);
      float bite = s.attackT > 0.0F ? Math.min(1.0F, s.attackT / 6.0F) * Anim317.smooth(10.0F, 9.0F, s.attackT) : 0.0F;
      float open = rest + (32.0F - rest) * bite;
      if (roar > 0.0F) {
         open = Math.max(open, 38.0F * sin(Math.PI * (1.0F - roar)));
         p.rot(head, -18.0F * sin(Math.PI * (1.0F - roar)), 0.0F, 0.0F);
      }

      if (dead) {
         open = 28.0F;
      }

      p.rot(r.idx("jaw"), open, 0.0F, 0.0F);
      // the tail whips from side to side, more and more towards the tip, bobs with a delay after the wings and trails behind a turn
      float lag = clamp(s.turn, -4.5F, 4.5F) * flyK;
      for (int k = 0; k < 9; k++) {
         float w = sin((perched ? t * 0.05F : s.walk * 0.35F) - k * 0.52F) * ((perched ? 2.2F : 3.4F) + (perched ? 0.7F : 0.95F) * k);
         float l = flyK * (2.6F + 1.05F * k) * sin(u - 0.75F - 0.52F * k) + climb * 0.14F + (perched ? sin(t * 0.055F - k * 0.45F) * (1.2F + 0.35F * k) : 0.0F);
         float rollT = sin((perched ? t * 0.05F : s.walk * 0.35F) - k * 0.52F) * (0.7F + 0.3F * k);
         p.rot(r.idx("tail" + k), (perched ? -2.0F : 0.0F) + l, w - lag * (0.5F + 0.06F * k), rollT);
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
