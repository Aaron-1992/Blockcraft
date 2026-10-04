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

   // ------------------------------------------------------------------ end crystal
   /** Three nested shapes that turn on their corners: two cubes of glass (only their frames and a few sparks are drawn) round a glowing core. */
   static Rig crystal() {
      Rig r = new Rig(Mobs.END_CRYSTAL, 2, 128);
      Bone bob = r.bone("bob", r.root(), 0, 15, 0).glow();
      String[] names = {"outer", "inner", "core"};
      float[] half = {10.0F, 7.4F, 4.6F};
      for (int i = 0; i < 3; i++) {
         Bone spin = r.boneRel("spin" + i, bob, 0, 0, 0).glow();
         Bone tilt1 = r.boneRel("tilt" + i + "a", spin, 0, 0, 0).glow().rot(-35.264F, 0, 0);
         Bone tilt2 = r.boneRel(names[i], tilt1, 0, 0, 0).glow().rot(0, 0, 45.0F);
         tilt2.box(-half[i], -half[i], -half[i], half[i] * 2, half[i] * 2, half[i] * 2);
      }

      r.finish();
      r.paint(c -> {
         String n = c.bone.name;
         if (n.equals("core")) {
            float m = RigPaint.fbm(c.x * 0.4F, c.y * 0.4F, c.z * 0.4F, 3);
            return RigPaint.c(RigPaint.mix(0xFF7AE0, 0xFFE8FF, m));
         }

         boolean outer = n.equals("outer");
         int rim = c.edge <= 1 ? 1 : 0;
         if (rim > 0) {
            return RigPaint.c(c.edge == 0 ? (outer ? 0xFFC8FF : 0xFFA0F0) : (outer ? 0xE07AF0 : 0xC850E0));
         }

         float spark = RigPaint.hash(c.tx, c.ty, outer ? 3 : 7, 11);
         if (spark > 0.93F) {
            return RigPaint.c(0xF8A8FF);
         }

         if (spark > 0.88F) {
            return RigPaint.c(0xB040D0);
         }

         return RigPaint.CLEAR;
      }, 0.0F);
      return r;
   }

   // ------------------------------------------------------------------ ender dragon
   static Rig dragon() {
      Rig r = new Rig(Mobs.ENDER_DRAGON, 2, 512);
      Bone body = r.bone("body", r.root(), 0, 34, 0);
      body.round(-8, -6, -14, 16, 12, 28, 2.0F);
      for (int i = 0; i < 6; i++) {
         body.box(-0.7F, 5.6F, -12.0F + i * 4.6F, 1.4F, 2.8F, 1.8F).group(0);
      }

      Bone[] neck = new Bone[3];
      neck[0] = r.bone("neck0", body, 0, 35.0F, 12.0F);
      neck[1] = r.boneRel("neck1", neck[0], 0, 1.5F, 9.5F);
      neck[2] = r.boneRel("neck2", neck[1], 0, 1.5F, 9.5F);
      for (int i = 0; i < 3; i++) {
         neck[i].round(-4, -4, 0, 8, 8, 10.4F, 1.0F);
         neck[i].box(-0.6F, 3.8F, 2.5F, 1.2F, 2.4F, 1.6F);
         neck[i].box(-0.6F, 3.8F, 7.0F, 1.2F, 2.0F, 1.4F);
      }

      Bone head = r.boneRel("head", neck[2], 0, 1.5F, 9.5F);
      Cube skull = head.round(-5, -4, 0, 10, 8, 8.5F, 1.0F);
      Cube snout = head.round(-3.6F, -3.0F, 8.2F, 7.2F, 4.8F, 7.8F, 0.8F);
      head.box(-2.5F, 1.8F, 14.4F, 1.4F, 1.2F, 1.8F);
      head.box(1.1F, 1.8F, 14.4F, 1.4F, 1.2F, 1.8F);
      for (int i = 0; i < 5; i++) {
         head.box(2.6F, -4.9F, 8.6F + i * 1.5F, 0.8F, 1.9F, 0.8F);
         head.box(-3.4F, -4.9F, 8.6F + i * 1.5F, 0.8F, 1.9F, 0.8F);
      }

      Bone jaw = r.boneRel("jaw", head, 0, -3.2F, 1.4F);
      jaw.round(-4, -2.4F, 0, 8, 2.6F, 14, 0.7F);
      for (int i = 0; i < 5; i++) {
         jaw.box(2.4F, 0.1F, 4.0F + i * 1.9F, 0.8F, 1.6F, 0.8F);
         jaw.box(-3.2F, 0.1F, 4.0F + i * 1.9F, 0.8F, 1.6F, 0.8F);
      }

      Bone eyes = r.boneRel("eyes", head, 0, 0, 0).glow();
      eyes.box(4.95F, 0.2F, 4.4F, 0.4F, 1.6F, 2.2F);
      eyes.box(-5.35F, 0.2F, 4.4F, 0.4F, 1.6F, 2.2F);
      Bone horn0 = r.boneRel("hornL", head, 3.4F, 3.6F, 1.6F).rot(-38, 0, -10);
      horn0.box(-0.9F, 0, -0.9F, 1.8F, 7.0F, 1.8F);
      Bone horn1 = r.boneRel("hornR", head, -3.4F, 3.6F, 1.6F).rot(-38, 0, 10);
      horn1.box(-0.9F, 0, -0.9F, 1.8F, 7.0F, 1.8F);
      for (int s = 0; s < 2; s++) {
         float sx = s == 0 ? 1.0F : -1.0F;
         String n = s == 0 ? "L" : "R";
         Bone wing = r.bone("wing" + n, body, sx * 8.0F, 38.0F, 6.0F);
         wing.box(s == 0 ? 0 : -28, -2.2F, -3, 28, 4.4F, 6);
         wing.box(s == 0 ? 0 : -28, -0.6F, -17, 28, 1.2F, 14.5F);
         Bone tip = r.bone("wingTip" + n, wing, sx * 36.0F, 38.0F, 6.0F);
         tip.box(s == 0 ? 0 : -32, -1.6F, -2.5F, 32, 3.2F, 5);
         tip.box(s == 0 ? 0 : -32, -0.5F, -15.5F, 32, 1.0F, 13);
         for (int f = 0; f < 3; f++) {
            tip.box(s == 0 ? 0 : -32, -1.1F, -5.5F - f * 4.2F, 32, 1.4F, 0.9F);
         }

         // legs: thigh, shin, paw with claws
         Bone fl = r.bone("legF" + n, body, sx * 6.0F, 29, 8);
         fl.box(-2.5F, -9.5F, -2.5F, 5, 10.5F, 5);
         Bone fs = r.bone("shinF" + n, fl, sx * 6.0F, 20, 8);
         fs.box(-2.0F, -8.0F, -2.0F, 4, 8.5F, 4);
         Bone fp = r.bone("pawF" + n, fs, sx * 6.0F, 13, 8);
         fp.box(-2.6F, -1.8F, -3.5F, 5.2F, 2.4F, 8);
         for (int c = 0; c < 3; c++) {
            fp.box(-2.2F + c * 1.7F, -1.6F, 4.4F, 0.8F, 1.2F, 1.8F);
         }

         Bone hl = r.bone("legH" + n, body, sx * 6.5F, 30, -9);
         hl.box(-3.5F, -12.5F, -3.5F, 7, 13.5F, 7);
         Bone hs = r.bone("shinH" + n, hl, sx * 6.5F, 18, -9);
         hs.box(-2.8F, -10.0F, -2.8F, 5.6F, 10.5F, 5.6F);
         Bone hp = r.bone("pawH" + n, hs, sx * 6.5F, 9, -9);
         hp.box(-3.2F, -1.8F, -4.0F, 6.4F, 2.4F, 9);
         for (int c = 0; c < 3; c++) {
            hp.box(-2.7F + c * 2.1F, -1.6F, 4.9F, 1.0F, 1.2F, 2.0F);
         }
      }

      Bone prev = r.bone("tail0", body, 0, 34, -14);
      for (int i = 0; i < 7; i++) {
         if (i > 0) {
            prev = r.boneRel("tail" + i, prev, 0, 0, -9);
         }

         float w = Math.max(3.0F, 8.0F - i);
         prev.round(-w / 2, -w / 2, -9, w, w, 9, Math.min(1.0F, w * 0.15F));
         prev.box(-0.5F, w / 2 - 0.2F, -7, 1.0F, 2.0F, 2.0F);
      }

      r.finish();
      r.paint(c -> {
         String n = c.bone.name;
         float g = RigPaint.grain(c, 151) * 0.05F;
         if (n.equals("eyes")) {
            return RigPaint.mix(0xFF60FF, 0xC030F0, RigPaint.smooth(0.0F, 1.0F, c.fv));
         }

         boolean membrane = c.cube.h < 1.3F && n.startsWith("wing");
         boolean fingers = c.cube.d < 1.0F && n.startsWith("wingTip");
         if (n.startsWith("wing") && !membrane && !fingers) {
            return RigPaint.mul(RigPaint.mix(0x16111C, 0x2A2236, RigPaint.noise(c.x * 0.4F, c.y * 0.4F, c.z * 0.4F, 3)), 1.0F + g);
         }

         if (fingers) {
            return RigPaint.mul(RigPaint.c(0x120E18), 1.0F + g);
         }

         if (membrane) {
            float vein = 0.5F + 0.5F * (float)Math.sin(c.x * 0.55F + (float)Math.sin(c.z * 0.4F) * 1.2F);
            float web = RigPaint.fbm(c.x * 0.2F, c.y * 0.2F, c.z * 0.2F, 7);
            int col = RigPaint.mix(0x2E1F44, 0x4A3468, web);
            col = RigPaint.mix(col, 0x1A1226, RigPaint.smooth(0.82F, 1.0F, vein) * 0.6F);
            return RigPaint.mul(RigPaint.c(col), 1.0F + g);
         }

         if (n.startsWith("horn") || n.startsWith("paw") && c.edge >= 0 && c.cube.d < 2.5F) {
            return RigPaint.mul(RigPaint.c(0xC9C0AE), 0.85F + 0.3F * RigPaint.noise(c.x, c.y, c.z, 5) + g);
         }

         if (c.cube.w < 1.0F && c.cube.h < 2.0F || c.cube.w < 1.6F && c.cube.h < 3.0F && (n.equals("head") || n.equals("jaw"))) {
            return RigPaint.mul(RigPaint.c(0xE8E0D0), 0.9F + g); // teeth
         }

         if (c.cube.w < 1.5F && c.cube.h < 3.0F) {
            return RigPaint.mul(RigPaint.c(0x4A4254), 1.0F + g); // spikes
         }

         // scales: hexagon-like cells that are a little lighter in the middle
         float cx = c.x * 0.75F + (c.y * 0.9F);
         float cz = c.z * 0.75F;
         float cell = (float)(Math.sin(cx * 1.9F) * Math.sin(cz * 1.9F + 1.0F));
         float tone = 0.5F + 0.5F * cell;
         float broad = RigPaint.fbm(c.x * 0.15F, c.y * 0.15F, c.z * 0.15F, 9);
         int col = RigPaint.mix(0x15111C, 0x2A2236, tone * 0.55F + broad * 0.5F);
         if (c.ny < -0.5F) {
            float plate = 0.5F + 0.5F * (float)Math.sin(c.z * 1.1F);
            col = RigPaint.mix(0x2A2232, 0x4A3A5A, plate * 0.7F);
         } else if (c.edge == 0) {
            col = RigPaint.mix(col, 0x4A3868, 0.4F);
         }

         if (n.equals("jaw") && c.face == Rig.TOP) {
            col = 0x4A1A3A;
         }

         return RigPaint.mul(RigPaint.c(col), 1.0F + g);
      }, 0.06F);
      for (int s = 0; s < 2; s++) {
         r.patch(snout, Rig.FRONT, s == 0 ? 1.1F : 4.7F, 1.0F, 1.4F, 1.0F, RigPaint.c(0x0A0610));
      }

      return r;
   }
}
