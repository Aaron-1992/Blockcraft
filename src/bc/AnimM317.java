package bc;

import bc.Anim317.MState;

/** Update 3.17: the clips of the walking monsters (see {@link Anim317} for the state they read and the shared helpers). */
final class AnimM317 {
   private AnimM317() {
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

   /**
    * What every humanoid does with its legs, hips and head: the legs swing against each other with bent knees, the hips and shoulders twist
    * against each other, the body rises with the step. Returns the strength of the walk 0..1 for the caller.
    */
   static void gait(Rig r, MState s, Pose p, float hipDeg, float kneeDeg, float bob, float twist) {
      float phi = s.walk * 1.6F;
      float a = s.amp;
      int body = r.idx("body");
      int chest = r.idx("chest");
      for (int i = 0; i < 2; i++) {
         String n = i == 0 ? "L" : "R";
         float th = phi + (i == 0 ? 0.0F : 3.1416F);
         float limp = (s.seed & 1) == 0 ? (i == 0 ? 1.0F : 0.88F) : (i == 0 ? 0.88F : 1.0F);
         p.rot(r.idx("leg" + n), -hipDeg * a * sin(th) * limp + s.air * (i == 0 ? -20.0F : 12.0F), 0.0F, (i == 0 ? -1.0F : 1.0F) * 1.5F * a);
         p.rot(r.idx("shin" + n), kneeDeg * a * Math.max(0.0F, cos(th)) * limp + s.air * 16.0F, 0.0F, 0.0F);
      }

      p.move(body, 0.0F, bob * a * (0.5F + 0.5F * cos(2.0F * phi)), 0.0F);
      p.rot(body, sin(2.0F * phi + 0.5F) * 1.2F * a, -sin(phi) * twist * a, sin(phi) * 1.8F * a);
      p.rot(chest, 0.0F, sin(phi) * twist * 1.7F * a, -sin(phi) * 1.2F * a);
   }

   static void head(Rig r, MState s, Pose p, float pitch, float lollZ, float lollSpeed) {
      int head = r.idx("head");
      float look = clamp(s.headYaw, -55.0F, 55.0F);
      p.rot(head, s.headPitch + pitch - 9.0F * Anim317.hurtK(s) - 3.0F * s.amp * sin(s.walk * 3.2F), -look, lollZ * sin(s.t * lollSpeed + s.seed));
   }

   // ------------------------------------------------------------------ zombie (and the pigman)
   static void zombie(Rig r, MState s, Pose p, boolean pigman) {
      float phi = s.walk * 1.6F;
      float a = s.amp;
      float calm = 1.0F - Math.min(1.0F, a * 2.0F);
      gait(r, s, p, 34.0F, 42.0F, 0.5F, 3.0F);
      float lunge = s.attackT > 0.0F ? sin(Math.PI * (1.0F - s.attackT / 8.0F)) : 0.0F;
      float reach = s.target ? 1.0F : 0.0F;
      float lean = 4.0F + 6.0F * a + 6.0F * reach + 8.0F * lunge;
      int chest = r.idx("chest");
      p.rot(chest, lean + sin(s.t * 0.11F + s.seed) * 1.2F, 0.0F, 0.0F);
      for (int i = 0; i < 2; i++) {
         float sg = i == 0 ? 1.0F : -1.0F;
         String n = i == 0 ? "L" : "R";
         float sway = sin(s.t * 0.09F + i * 1.7F + s.seed) * 3.0F;
         float idle = -34.0F + sway - (i == 0 ? 0.0F : 6.0F);
         float walkSwing = sin(phi + (i == 0 ? 3.1416F : 0.0F)) * 16.0F * a;
         float armX = reach > 0.0F ? -88.0F + sin(s.t * 0.13F + i * 2.0F) * 4.0F : idle + walkSwing;
         armX -= 34.0F * lunge;
         p.rot(r.idx("arm" + n), armX, sg * (reach > 0.0F ? -4.0F + 5.0F * lunge : 0.0F), sg * (4.0F + 3.0F * calm));
         float elbow = reach > 0.0F ? -6.0F - 12.0F * lunge : -26.0F - 6.0F * sin(s.t * 0.1F + i);
         p.rot(r.idx("fore" + n), elbow, 0.0F, 0.0F);
      }

      head(r, s, p, 8.0F - 0.85F * lean + 4.0F * calm * sin(s.t * 0.05F + s.seed), 6.0F, 0.05F);
      float moan = Anim317.moment(s, 9, 260.0F, 36.0F);
      if (moan > 0.0F) {
         p.rot(r.idx("head"), -20.0F * sin(Math.PI * (1.0F - moan)), 0.0F, 0.0F);
         p.rot(chest, -6.0F * sin(Math.PI * (1.0F - moan)), 0.0F, 0.0F);
      }

      p.rot(chest, 0.0F, 0.0F, sin(phi) * 2.0F * a);
      int jaw = r.idx("jaw");
      p.rot(jaw, moan > 0.0F ? 24.0F * sin(Math.PI * (1.0F - moan)) : 0.0F, 0.0F, 0.0F);
   }

   // ------------------------------------------------------------------ skeleton
   static void skeleton(Rig r, MState s, Pose p) {
      float phi = s.walk * 1.6F;
      float a = s.amp;
      float calm = 1.0F - Math.min(1.0F, a * 2.0F);
      gait(r, s, p, 32.0F, 34.0F, 0.4F, 2.5F);
      float draw = s.target ? Anim317.smooth(26.0F, 1.0F, s.cd) : 0.0F;
      float shot = s.attackT > 0.0F ? s.attackT / 6.0F : 0.0F;
      int armL = r.idx("armL");
      int armR = r.idx("armR");
      int foreL = r.idx("foreL");
      int foreR = r.idx("foreR");
      if (s.target) {
         // the bow arm points at the target, the other hand pulls the string back along it
         p.rot(armL, -86.0F + 1.5F * sin(s.t * 0.2F) + 7.0F * shot, 14.0F - 6.0F * draw, 0.0F);
         p.rot(foreL, -2.0F, 0.0F, 0.0F);
         p.rot(armR, -84.0F + 12.0F * draw + 14.0F * shot, 52.0F - 8.0F * draw, 0.0F);
         p.rot(foreR, -18.0F - 70.0F * draw + 40.0F * shot, 0.0F, 0.0F);
         p.rot(r.idx("chest"), 3.0F * draw - 3.0F * shot, -9.0F * draw, 0.0F);
      } else {
         // carries the bow in front of it, the other arm swings
         p.rot(armL, -36.0F + sin(s.t * 0.08F) * 2.0F, 8.0F, 0.0F);
         p.rot(foreL, -54.0F, 0.0F, 0.0F);
         p.rot(armR, sin(phi) * 26.0F * a - 6.0F * calm, 0.0F, -3.0F);
         p.rot(foreR, -12.0F - 14.0F * a, 0.0F, 0.0F);
      }

      float pull = draw * 5.0F;
      float ang = (float)Math.toDegrees(Math.atan2(pull, 6.9F));
      p.rot(r.idx("strU"), ang, 0.0F, 0.0F);
      p.rot(r.idx("strL"), -ang, 0.0F, 0.0F);
      float flex = draw * 14.0F;
      p.rot(r.idx("bowU1"), -flex * 0.5F, 0.0F, 0.0F);
      p.rot(r.idx("bowU2"), -flex, 0.0F, 0.0F);
      p.rot(r.idx("bowL1"), flex * 0.5F, 0.0F, 0.0F);
      p.rot(r.idx("bowL2"), flex, 0.0F, 0.0F);
      head(r, s, p, 2.0F - 4.0F * draw, 3.0F * calm, 0.04F);
      float rattle = Math.max(Anim317.momentSoft(s, 9, 200.0F, 26.0F), shot);
      p.rot(r.idx("jaw"), rattle > 0.0F ? 12.0F * Math.abs(sin(s.t * 1.7F)) * Math.min(1.0F, rattle * 2.0F) : 0.0F, 0.0F, 0.0F);
      p.rot(r.idx("chest"), 2.0F + sin(s.t * 0.1F + s.seed) * 0.8F, 0.0F, 0.0F);
   }

   // ------------------------------------------------------------------ creeper
   static void creeper(Rig r, MState s, Pose p) {
      float phi = s.walk * 1.6F;
      float a = s.amp;
      float k = s.swell;
      String[] names = {"FL", "FR", "BL", "BR"};
      float[] ph = {0.0F, 3.1416F, 3.1416F, 0.0F};
      for (int i = 0; i < 4; i++) {
         float th = phi * 1.15F + ph[i];
         p.rot(r.idx("leg" + names[i]), -40.0F * a * sin(th) + s.air * (i < 2 ? -22.0F : 22.0F) + k * (i < 2 ? -9.0F : 9.0F), 0.0F, (i % 2 == 0 ? 1.0F : -1.0F) * k * 8.0F);
         p.rot(r.idx("shin" + names[i]), 36.0F * a * Math.max(0.0F, cos(th)), 0.0F, 0.0F);
      }

      int body = r.idx("body");
      p.move(body, 0.0F, 0.35F * a * (0.5F + 0.5F * cos(2.0F * phi * 1.15F)), 0.0F);
      p.rot(body, sin(2.3F * phi + 0.4F) * 2.0F * a, sin(phi * 1.15F) * 3.0F * a, sin(phi * 1.15F) * 2.0F * a);
      p.scale(body, 1.0F, 1.0F + sin(s.t * 0.11F + s.seed) * 0.014F * (1.0F + (1.0F - Math.min(1.0F, a * 2.0F))), 1.0F);
      float look = clamp(s.headYaw, -60.0F, 60.0F);
      p.rot(r.idx("head"), s.headPitch + sin(2.3F * phi + 0.2F) * 3.0F * a - 6.0F * k - 8.0F * Anim317.hurtK(s), -look + sin(s.t * 0.021F + s.seed) * 8.0F * (1.0F - Math.min(1.0F, a * 2.0F)) * (1.0F - k), sin(s.t * 2.7F) * 4.0F * k);
      if (k > 0.0F) {
         float wob = 1.0F + sin(k * 100.0F) * k * 0.01F;
         float wide = (1.0F + k * k * 0.4F) * wob;
         p.rootScaleX = wide;
         p.rootScaleZ = wide;
         p.rootScaleY = (1.0F + k * 0.2F) * wob;
      }
   }

   // ------------------------------------------------------------------ spider
   static void spider(Rig r, MState s, Pose p) {
      float phi = s.walk * 1.6F;
      float a = s.amp;
      float calm = 1.0F - Math.min(1.0F, a * 2.0F);
      float lunge = s.attackT > 0.0F ? sin(Math.PI * (1.0F - s.attackT / 8.0F)) : 0.0F;
      float dead = Math.min(1.0F, s.death * 1.5F);
      for (int sd = 0; sd < 2; sd++) {
         float sx = sd == 0 ? 1.0F : -1.0F;
         String sn = sd == 0 ? "L" : "R";
         for (int i = 0; i < 4; i++) {
            float ph = phi * 1.25F + (i % 2) * 3.1416F + sd * 3.1416F;
            float swing = 20.0F * a * sin(ph);
            float lift = 24.0F * a * Math.max(0.0F, cos(ph));
            float idle = calm * Math.max(0.0F, sin(s.t * 0.07F + i * 1.9F + sd * 2.3F + s.seed) - 0.82F) * 90.0F;
            float front = i == 0 ? 1.0F : 0.0F;
            float up = lift + idle + front * (lunge * 46.0F + s.air * 20.0F) + s.air * 14.0F - dead * 110.0F;
            p.rot(r.idx("leg" + sn + i), 0.0F, sx * swing + sx * front * lunge * 10.0F, sx * up);
            p.rot(r.idx("shin" + sn + i), 0.0F, 0.0F, -sx * (lift * 0.9F + idle * 0.8F + front * lunge * 24.0F + dead * 14.0F));
         }
      }

      int body = r.idx("body");
      p.move(body, 0.0F, 0.5F * a * Math.abs(sin(phi * 1.25F)), 0.0F);
      p.rot(body, -12.0F * lunge, sin(phi * 1.25F) * 2.5F * a, sin(phi * 1.25F) * 2.0F * a);
      p.rot(r.idx("abdomen"), sin(s.t * 0.09F + s.seed) * 1.5F + sin(phi * 1.25F) * 3.0F * a + 6.0F * lunge, sin(phi * 1.25F + 1.0F) * 7.0F * a + sin(s.t * 0.05F) * 3.0F * calm, 0.0F);
      p.scale(r.idx("abdomen"), 1.0F + sin(s.t * 0.12F + s.seed) * 0.02F, 1.0F + sin(s.t * 0.12F + s.seed) * 0.03F, 1.0F);
      float look = clamp(s.headYaw, -45.0F, 45.0F);
      p.rot(r.idx("head"), s.headPitch + sin(phi * 2.5F) * 2.0F * a - 6.0F * Anim317.hurtK(s) + 10.0F * lunge, -look, 0.0F);
      float open = 8.0F + 34.0F * lunge + 6.0F * sin(s.t * 0.3F) * (s.target ? 1.0F : 0.2F);
      p.rot(r.idx("fangL"), 0.0F, 0.0F, -open);
      p.rot(r.idx("fangR"), 0.0F, 0.0F, open);
      p.rot(r.idx("palpL"), sin(s.t * 0.45F) * 10.0F, 0.0F, -4.0F);
      p.rot(r.idx("palpR"), sin(s.t * 0.45F + 1.5F) * 10.0F, 0.0F, 4.0F);
      if (s.death > 0.0F) {
         p.rootPivot = 0.4F * Math.min(1.0F, s.death);
         p.rootRoll = 180.0F * Math.min(1.0F, s.death * 1.2F);
      }
   }
}
