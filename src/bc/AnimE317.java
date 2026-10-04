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
   static void dragon(Rig r, MState s, Pose p) {
      float t = s.t;
      boolean perched = s.phase == Dragon.PERCH && !s.dead;
      boolean landing = s.phase == Dragon.LAND && !s.dead;
      float flyK = perched ? 0.0F : 1.0F;
      float ph = perched ? t * 0.06F : s.walk * 0.5F;
      float amp = s.dead ? 5.0F : (perched ? 14.0F : 32.0F);
      float beat = sin(ph);
      for (int i = 0; i < 2; i++) {
         float sg = i == 0 ? 1.0F : -1.0F;
         String n = i == 0 ? "L" : "R";
         float fold = perched ? 1.0F : (landing ? 0.35F : 0.0F);
         // the wing: it lifts and drops, the tip lags behind, on a perch it is folded back along the body
         p.rot(r.idx("wing" + n), 0.0F, sg * (8.0F * (1.0F - fold) + 40.0F * fold + 5.0F * cos(ph) * flyK), sg * (amp * beat + 6.0F - 22.0F * fold));
         p.rot(r.idx("wingTip" + n), 0.0F, sg * (10.0F * (1.0F - fold) + 78.0F * fold), sg * (amp * 0.7F * sin(ph - 0.9F) + 14.0F - 20.0F * fold));
         if (!perched) {
            // legs tucked under the body while it flies, stretched out when it lands
            float stretch = landing ? 0.7F : 0.0F;
            p.rot(r.idx("legF" + n), -54.0F * (1.0F - stretch) - 14.0F * stretch, 0.0F, sg * 4.0F);
            p.rot(r.idx("shinF" + n), 78.0F * (1.0F - stretch) + 20.0F * stretch, 0.0F, 0.0F);
            p.rot(r.idx("pawF" + n), -30.0F * (1.0F - stretch), 0.0F, 0.0F);
            p.rot(r.idx("legH" + n), 28.0F * (1.0F - stretch) - 18.0F * stretch, 0.0F, sg * 5.0F);
            p.rot(r.idx("shinH" + n), -58.0F * (1.0F - stretch) + 20.0F * stretch, 0.0F, 0.0F);
            p.rot(r.idx("pawH" + n), 30.0F * (1.0F - stretch), 0.0F, 0.0F);
         } else {
            p.rot(r.idx("legF" + n), 3.0F * sin(t * 0.05F), 0.0F, sg * 2.0F);
            p.rot(r.idx("legH" + n), -2.0F, 0.0F, sg * 2.0F);
         }
      }

      int body = r.idx("body");
      float pitch = clamp(s.headPitch, -35.0F, 35.0F);
      p.move(body, 0.0F, perched ? 0.35F * sin(t * 0.07F) : -0.9F * beat, 0.0F);
      p.rot(body, perched ? 4.0F : pitch * 0.45F, 0.0F, 0.0F);
      float breathe = 1.0F + 0.012F * sin(t * 0.09F);
      p.scale(body, breathe, breathe, 1.0F);
      // the neck is an S that follows where the head looks
      float hy = clamp(s.headYaw, -55.0F, 55.0F);
      float rise = perched ? 1.0F : 0.0F;
      float sway = sin(t * 0.045F) * (perched ? 8.0F : 2.0F);
      p.rot(r.idx("neck0"), -8.0F - 18.0F * rise + pitch * 0.18F, -hy * 0.22F + sway * 0.4F, 0.0F);
      p.rot(r.idx("neck1"), 10.0F + 12.0F * rise + pitch * 0.2F, -hy * 0.28F + sway * 0.6F, 0.0F);
      p.rot(r.idx("neck2"), 5.0F + 4.0F * rise + pitch * 0.2F, -hy * 0.3F + sway, 0.0F);
      int head = r.idx("head");
      p.rot(head, pitch * 0.4F - 6.0F - 8.0F * rise, -hy * 0.2F, sin(t * 0.03F) * 2.0F);
      float roar = perched ? Anim317.moment(s, 5, 70.0F, 28.0F) : 0.0F;
      float rest = 5.0F + sin(t * 0.12F) * 3.0F;
      float bite = s.attackT > 0.0F ? Math.min(1.0F, s.attackT / 6.0F) * Anim317.smooth(10.0F, 9.0F, s.attackT) : 0.0F;
      float open = rest + (30.0F - rest) * bite;
      if (roar > 0.0F) {
         open = Math.max(open, 36.0F * sin(Math.PI * (1.0F - roar)));
         p.rot(head, -16.0F * sin(Math.PI * (1.0F - roar)), 0.0F, 0.0F);
      }

      if (s.dead) {
         open = 28.0F;
      }

      p.rot(r.idx("jaw"), open, 0.0F, 0.0F);
      // the tail whips from side to side, more and more towards the tip
      for (int i = 0; i < 7; i++) {
         float w = sin(s.walk * 0.3F - i * 0.55F) * (2.4F + 0.55F * i);
         float l = cos(s.walk * 0.3F - i * 0.55F) * (1.3F + 0.35F * i);
         p.rot(r.idx("tail" + i), (perched ? -2.0F : 0.0F) + l * (perched ? 0.3F : 1.0F), w * (perched ? 0.4F : 1.0F) + (perched ? sin(t * 0.04F - i * 0.5F) * (1.5F + i * 0.8F) : 0.0F), 0.0F);
      }

      if (s.dead) {
         float dt = Math.min(1.0F, s.deathT / (float)Dragon.DEATH_TICKS);
         float k = 0.8F + 0.9F * dt;
         p.rootY = sin(s.deathT * 1.9F) * 0.08F * dt + 0.0001F;
         p.tintR = k;
         p.tintG = k / 0.45F;
         p.tintB = k / 0.45F;
      }
   }
}
