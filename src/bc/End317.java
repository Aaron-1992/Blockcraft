package bc;

import bc.Rig.Bone;
import bc.Rig.Cube;

/** Update 3.17: the models and skins of the End - the enderman, (further down) the dragon and the crystal. */
final class End317 {
   private End317() {
   }

   // ------------------------------------------------------------------ enderman
   /** 3 blocks tall: legs of two segments (28), a pelvis and a chest (12), a head with a hinged jaw (8), arms with elbows that reach the knees. */
   static Rig enderman() {
      Rig r = new Rig(Mobs.ENDERMAN, 2, 256);
      Bone body = r.bone("body", r.root(), 0, 28, 0);
      body.box(-3.6F, 0, -2, 7.2F, 4.2F, 4);
      Bone chest = r.bone("chest", body, 0, 32, 0);
      chest.round(-4, 0, -2, 8, 8, 4, 0.4F);
      Bone head = r.bone("head", chest, 0, 40, 0);
      Cube hc = head.round(-4, 3.2F, -4, 8, 4.8F, 8, 0.5F);
      Bone jaw = r.bone("jaw", head, 0, 43.2F, -3.6F);
      Cube jc = jaw.box(-3.9F, -3.2F, 0.1F, 7.8F, 3.2F, 7.6F);
      Bone eyes = r.boneRel("eyes", head, 0, 0, 0).glow();
      eyes.box(0.9F, 4.5F, 4.0F, 2.8F, 1.1F, 0.35F);
      eyes.box(-3.7F, 4.5F, 4.0F, 2.8F, 1.1F, 0.35F);
      for (int s = 0; s < 2; s++) {
         float sx = s == 0 ? 1.0F : -1.0F;
         String n = s == 0 ? "L" : "R";
         Bone arm = r.bone("arm" + n, chest, sx * 5.0F, 38, 0);
         arm.box(-1, -15, -1, 2, 15.5F, 2);
         Bone fore = r.bone("fore" + n, arm, sx * 5.0F, 23, 0);
         fore.box(-0.9F, -15, -0.9F, 1.8F, 15.4F, 1.8F);
         fore.box(-1.1F, -17.2F, -1.2F, 2.2F, 2.6F, 2.4F);
         Bone leg = r.bone("leg" + n, body, sx * 2.0F, 28, 0);
         leg.box(-1, -14, -1, 2, 14.5F, 2);
         Bone shin = r.bone("shin" + n, leg, sx * 2.0F, 14, 0);
         shin.box(-0.9F, -14, -0.9F, 1.8F, 14, 1.8F);
         shin.box(-1.1F, -14, -1.3F, 2.2F, 1.4F, 3.0F);
      }

      r.finish();
      r.paint(c -> {
         String n = c.bone.name;
         float g = RigPaint.grain(c, 111) * 0.035F;
         if (n.equals("eyes")) {
            return RigPaint.mix(0xE860FF, 0xB020E0, RigPaint.smooth(0.0F, 1.0F, c.fv));
         }

         float tone = RigPaint.fbm(c.x * 0.25F, c.y * 0.25F, c.z * 0.25F, 3);
         int col = RigPaint.mix(0x141018, 0x211A2A, tone);
         if (RigPaint.noise(c.x * 1.1F, c.y * 1.1F, c.z * 1.1F, 7) > 0.84F) {
            col = RigPaint.mix(col, 0x3A2A4A, 0.6F);
         }

         if (n.equals("jaw") && c.face == Rig.TOP) {
            col = 0x4A2262;
         }

         if (n.equals("head") && c.face == Rig.BOTTOM) {
            col = 0x4A2262;
         }

         return RigPaint.mul(RigPaint.c(col), 1.0F + g);
      }, 0.05F);
      r.patch(hc, Rig.FRONT, 0.4F, 1.4F, 3.0F, 0.4F, RigPaint.c(0x0A080D));
      r.patch(hc, Rig.FRONT, 4.6F, 1.4F, 3.0F, 0.4F, RigPaint.c(0x0A080D));
      return r;
   }
}
