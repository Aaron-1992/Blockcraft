package bc;

import bc.Rig.Bone;
import bc.Rig.Cube;

/** Update 3.17: the models and skins of the villager, the iron golem and the parrot. */
final class Folk317 {
   private Folk317() {
   }

   /** Groups of the villager rig: what a profession wears (a pose hides all but its own). */
   static final int V_GLASSES = 2;
   static final int V_CAP = 4;
   static final int V_HAT = 5;
   static final int V_SMITH = 6;
   static final int V_BUTCHER = 7;

   // ------------------------------------------------------------------ villager
   static Rig villager() {
      Rig r = new Rig(Mobs.VILLAGER, 2, 256);
      r.groupTint[Rigs317.G_ROBE] = Rig.TINT_ROBE;
      Bone body = r.bone("body", r.root(), 0, 12, 0);
      body.box(-4.7F, -10, -2.9F, 9.4F, 10, 5.8F).group(Rigs317.G_ROBE);
      body.box(-4.75F, -10, -2.95F, 9.5F, 1.4F, 5.9F).group(Rigs317.G_ROBE);
      Bone chest = r.bone("chest", body, 0, 12, 0);
      chest.box(-4.1F, 0, -2.2F, 8.2F, 10, 4.4F).group(Rigs317.G_ROBE);
      chest.box(-4.2F, 0.4F, -2.3F, 8.4F, 1.3F, 4.6F);
      Bone head = r.bone("head", chest, 0, 22, 0);
      Cube hc = head.round(-4, 0, -4, 8, 10, 8, 0.6F);
      Bone nose = r.boneRel("nose", head, 0, 4.0F, 4.0F);
      nose.box(-1, -2.0F, -0.1F, 2, 4, 2.4F);
      Bone arms = r.bone("arms", chest, 0, 17.5F, 0);
      arms.round(-4.8F, -2, 1.8F, 9.6F, 4, 4.4F, 0.5F).group(Rigs317.G_ROBE);
      arms.box(-5.2F, -1.2F, 5.0F, 2.2F, 2.4F, 1.9F);
      arms.box(3.0F, -1.2F, 5.0F, 2.2F, 2.4F, 1.9F);
      for (int s = 0; s < 2; s++) {
         float sx = s == 0 ? 1.0F : -1.0F;
         Bone leg = r.bone(s == 0 ? "legL" : "legR", body, sx * 2.0F, 12, 0);
         leg.box(-2, -12, -2, 4, 12, 4);
         leg.box(-2.1F, -12, -2.5F, 4.2F, 3.0F, 5.2F);
      }

      // what the professions wear
      Bone hat = r.boneRel("hat", head, 0, 10, 0);
      hat.round(-7.2F, 0, -7.2F, 14.4F, 0.9F, 14.4F, 0.3F).group(V_HAT);
      hat.round(-4.8F, 0.8F, -4.8F, 9.6F, 3.4F, 9.6F, 0.8F).group(V_HAT);
      Bone cap = r.boneRel("cap", head, 0, 10, 0);
      cap.round(-4.6F, -0.2F, -4.6F, 9.2F, 2.4F, 9.2F, 0.7F).group(V_CAP);
      Bone glasses = r.boneRel("glasses", head, 0, 0, 0);
      for (int s = 0; s < 2; s++) {
         float cx = s == 0 ? 1.8F : -1.8F;
         glasses.box(cx - 1.3F, 4.9F, 4.05F, 2.6F, 1.9F, 0.3F).group(V_GLASSES);
      }

      glasses.box(-0.5F, 5.9F, 4.05F, 1.0F, 0.35F, 0.3F).group(V_GLASSES);
      chest.box(-3.1F, 0.8F, 2.25F, 6.2F, 9.0F, 0.5F).group(V_SMITH);
      body.box(-3.3F, -9.6F, 2.95F, 6.6F, 9.6F, 0.5F).group(V_SMITH);
      chest.box(-3.1F, 0.8F, 2.3F, 6.2F, 9.0F, 0.5F).group(V_BUTCHER);
      body.box(-3.3F, -9.6F, 3.0F, 6.6F, 9.6F, 0.5F).group(V_BUTCHER);
      Rigs317.lids(r, head, hc, 1.8F, 3.7F);
      r.finish();
      r.paint(c -> {
         String n = c.bone.name;
         float g = RigPaint.grain(c, 121) * 0.04F;
         if (c.cube.group == Rigs317.G_ROBE) {
            // neutral cloth: the renderer multiplies it with the colour of the profession
            float weave = 0.5F + 0.5F * (float)Math.sin(c.y * 2.6F) * (float)Math.sin(c.x * 2.6F + c.z * 2.6F);
            float fold = 0.93F + 0.07F * (float)Math.sin(c.x * 1.3F + 1.0F);
            float tone = 0.9F + 0.1F * weave;
            if (n.equals("body") && c.y < 3.8F) {
               tone *= 0.82F + 0.18F * RigPaint.smooth(2.0F, 3.8F, c.y);
            }

            if (n.equals("arms") && (c.face == Rig.FRONT || c.face == Rig.TOP)) {
               tone *= 0.96F;
            }

            return RigPaint.mul(RigPaint.c(0xFFFFFF), tone * fold + g);
         }

         int skin = RigPaint.mix(0xC9987A, 0xB8856A, RigPaint.fbm(c.x * 0.3F, c.y * 0.3F, c.z * 0.3F, 5));
         if (c.cube.group == V_HAT) {
            float straw = 0.5F + 0.5F * (float)Math.sin((c.x + c.z) * 2.0F) * (float)Math.cos((c.x - c.z) * 1.6F);
            int col = RigPaint.mix(0xD9B85C, 0xB89A42, straw * 0.7F + RigPaint.noise(c.x * 2, c.y * 2, c.z * 2, 3) * 0.3F);
            if (c.y > 33.2F && c.y < 33.9F && c.face != Rig.TOP && c.face != Rig.BOTTOM) {
               col = 0x7A3B2A;
            }

            return RigPaint.mul(RigPaint.c(col), 1.0F + g);
         }

         if (c.cube.group == V_CAP) {
            return RigPaint.mul(RigPaint.c(RigPaint.mix(0x6B2E86, 0x8B3FA8, RigPaint.noise(c.x, c.y, c.z, 4))), 1.0F + g);
         }

         if (c.cube.group == V_GLASSES) {
            return RigPaint.c(0x2A2A30);
         }

         if (c.cube.group == V_SMITH) {
            return RigPaint.mul(RigPaint.c(RigPaint.mix(0x5A3A22, 0x7A5232, RigPaint.fbm(c.x * 0.8F, c.y * 0.8F, c.z * 0.8F, 6))), 1.0F + g);
         }

         if (c.cube.group == V_BUTCHER) {
            int col = 0xEEE6D4;
            if (RigPaint.fbm(c.x * 0.7F, c.y * 0.7F, c.z * 0.7F, 8) > 0.66F) {
               col = RigPaint.mix(col, 0x9A2A22, 0.8F);
            }

            return RigPaint.mul(RigPaint.c(col), 1.0F + g);
         }

         if (n.equals("nose")) {
            return RigPaint.mul(RigPaint.c(RigPaint.mix(skin, 0xA8715A, 0.55F)), 1.0F + g);
         }

         if (n.startsWith("leg")) {
            return RigPaint.mul(RigPaint.c(c.y < 4.0F ? 0x4A3624 : 0x5E4630), 1.0F + g);
         }

         if (n.equals("chest") || n.equals("body")) {
            return RigPaint.mul(RigPaint.c(0x5A4128), 1.0F + g); // the belt
         }

         if (n.equals("arms")) {
            return RigPaint.mul(RigPaint.c(skin), 1.0F + g);
         }

         int col = skin;
         if (c.y > 31.5F || c.face == Rig.TOP) {
            col = RigPaint.mix(skin, 0x8A6048, 0.25F);
         }

         return RigPaint.mul(RigPaint.c(col), 1.0F + g);
      }, 0.06F);
      // the face: a unibrow, green eyes, a small mouth under the big nose
      for (int s = 0; s < 2; s++) {
         float cx = s == 0 ? 2.2F : 5.8F;
         r.patch(hc, Rig.FRONT, cx - 0.9F, 3.7F, 1.8F, 1.3F, RigPaint.c(0xF2EEE6));
         r.patch(hc, Rig.FRONT, s == 0 ? cx : cx - 0.9F, 3.8F, 0.9F, 1.1F, RigPaint.c(0x2F8B3A));
         r.patch(hc, Rig.FRONT, s == 0 ? cx + 0.2F : cx - 0.7F, 4.1F, 0.5F, 0.6F, RigPaint.c(0x0C1A0C));
      }

      r.patch(hc, Rig.FRONT, 1.0F, 2.9F, 6.0F, 0.8F, RigPaint.c(0x3A2A20));
      r.patch(hc, Rig.FRONT, 2.6F, 7.0F, 2.8F, 0.5F, RigPaint.c(0x8A5A48));
      return r;
   }

   // ------------------------------------------------------------------ iron golem
   static Rig golem() {
      Rig r = new Rig(Mobs.IRON_GOLEM, 2, 256);
      Bone body = r.bone("body", r.root(), 0, 16, 0);
      body.round(-4.7F, 0, -3.2F, 9.4F, 5.6F, 6.4F, 0.8F);
      Bone chest = r.bone("chest", body, 0, 21, 0);
      chest.round(-9, 0, -5.5F, 18, 12, 11, 1.2F);
      chest.box(-6.5F, 2.0F, 5.4F, 13, 6.5F, 0.8F);
      Bone head = r.bone("head", chest, 0, 33, 0);
      Cube hc = head.round(-4, 0, -4, 8, 10, 8, 0.7F);
      head.box(-1, 3.2F, 4.0F, 2, 4.2F, 2.4F);
      head.box(-4.4F, 6.8F, -3.4F, 0.8F, 1.6F, 6.8F);
      head.box(3.6F, 6.8F, -3.4F, 0.8F, 1.6F, 6.8F);
      for (int s = 0; s < 2; s++) {
         float sx = s == 0 ? 1.0F : -1.0F;
         String n = s == 0 ? "L" : "R";
         Bone arm = r.bone("arm" + n, chest, sx * 11.4F, 31, 0);
         arm.round(-2.6F, -14, -3.2F, 5.2F, 15.5F, 6.4F, 0.6F);
         arm.box(-3.4F, -0.5F, -4.0F, 6.8F, 2.6F, 8.0F);
         Bone fore = r.bone("fore" + n, arm, sx * 11.4F, 17, 0);
         fore.round(-2.4F, -14.5F, -3.0F, 4.8F, 15.0F, 6.0F, 0.6F);
         fore.box(-3.2F, -17.5F, -3.8F, 6.4F, 3.6F, 7.6F);
         Bone leg = r.bone("leg" + n, body, sx * 5.0F, 16, 0);
         leg.round(-3, -16, -2.6F, 6, 16.4F, 5.2F, 0.6F);
         leg.box(-3.4F, -16, -3.6F, 6.8F, 3.2F, 7.2F);
      }

      Rigs317.lids(r, head, hc, 2.0F, 3.4F);
      r.finish();
      r.paint(c -> {
         String n = c.bone.name;
         float g = RigPaint.grain(c, 131) * 0.05F;
         float plate = RigPaint.fbm(c.x * 0.18F, c.y * 0.18F, c.z * 0.18F, 9);
         int col = RigPaint.mix(0xB8B4AA, 0x9C988E, RigPaint.smooth(0.35F, 0.7F, plate));
         // panel seams and rivets
         float sx = c.x * 0.5F;
         float sy = c.y * 0.5F;
         float seamY = Math.abs(((c.y + 1.0F) % 5.0F) - 2.5F);
         if (seamY > 2.35F) {
            col = RigPaint.mul(col, 0.78F);
         }

         float rv = (float)Math.hypot(((c.x + 50.0F) % 5.0F) - 2.5F, ((c.y + 50.0F) % 5.0F) - 2.5F);
         if (rv < 0.55F && c.edge > 1) {
            col = RigPaint.mul(col, 1.18F);
         }

         // rust streaks under the seams
         float rust = RigPaint.fbm(c.x * 0.35F, c.y * 0.12F, c.z * 0.35F, 11);
         if (rust > 0.62F) {
            col = RigPaint.mix(col, 0x8A5A3A, RigPaint.smooth(0.62F, 0.8F, rust) * 0.7F);
         }

         // vines
         float vine = RigPaint.fbm(c.x * 0.28F + 3.0F, c.y * 0.22F, c.z * 0.28F, 13);
         if (vine > 0.64F && (n.equals("chest") || n.startsWith("leg") || n.startsWith("arm") || n.equals("body"))) {
            col = RigPaint.mix(0x3F6B32, 0x5E8E46, RigPaint.noise(c.x * 1.5F, c.y * 1.5F, c.z * 1.5F, 3));
         }

         if (n.startsWith("lid")) {
            col = RigPaint.mix(0xB0ACA2, 0x8A867C, 0.4F);
         }

         return RigPaint.mul(RigPaint.c(col), 1.0F + g);
      }, 0.07F);
      // the face: heavy brow, two dark eyes, the long nose, a hard mouth
      for (int s = 0; s < 2; s++) {
         float cx = s == 0 ? 2.1F : 5.9F;
         r.patch(hc, Rig.FRONT, cx - 1.3F, 2.7F, 2.6F, 0.7F, RigPaint.c(0x6A665C));
         r.patch(hc, Rig.FRONT, cx - 0.9F, 3.5F, 1.8F, 1.3F, RigPaint.c(0x1A1816));
         r.patch(hc, Rig.FRONT, cx - 0.4F, 3.8F, 0.8F, 0.7F, RigPaint.c(0xE8A030));
      }

      r.patch(hc, Rig.FRONT, 2.2F, 7.4F, 3.6F, 0.7F, RigPaint.c(0x4A4640));
      return r;
   }

   // ------------------------------------------------------------------ parrot
   static Rig parrot() {
      Rig r = new Rig(Mobs.PARROT, 3, 256);
      r.groupTint[Rigs317.G_FEATHER] = Rig.TINT_FEATHER;
      Bone body = r.bone("body", r.root(), 0, 6, 0);
      body.blob(-2.0F, -3.0F, -1.7F, 4.0F, 6.6F, 3.4F, 0.7F).group(Rigs317.G_FEATHER);
      Bone head = r.bone("head", body, 0, 9.2F, 0.4F);
      Cube hc = head.round(-1.55F, 0, -1.55F, 3.1F, 3.1F, 3.1F, 0.45F).group(Rigs317.G_FEATHER);
      head.box(-0.5F, 2.6F, -1.5F, 1.0F, 1.5F, 2.4F).group(Rigs317.G_FEATHER);
      head.box(-0.4F, 3.6F, -1.8F, 0.8F, 1.1F, 1.8F).group(Rigs317.G_FEATHER);
      Bone beak = r.boneRel("beak", head, 0, 1.4F, 1.5F);
      beak.box(-0.6F, -0.2F, 0, 1.2F, 1.6F, 1.8F);
      beak.box(-0.45F, -0.8F, 1.4F, 0.9F, 1.0F, 0.8F);
      Bone jaw = r.boneRel("jawP", head, 0, 1.0F, 1.4F);
      jaw.box(-0.45F, -0.7F, 0, 0.9F, 0.8F, 1.3F);
      Bone tail = r.boneRel("tail", body, 0, -2.0F, -1.5F).rot(16, 0, 0);
      tail.box(-1.3F, -7.0F, -0.45F, 2.6F, 7.0F, 0.9F).group(Rigs317.G_FEATHER);
      tail.box(-0.7F, -9.0F, -0.2F, 1.4F, 2.2F, 0.5F);
      for (int s = 0; s < 2; s++) {
         float sx = s == 0 ? 1.0F : -1.0F;
         String n = s == 0 ? "L" : "R";
         Bone wing = r.boneRel("wing" + n, body, sx * 2.1F, 2.6F, 0);
         wing.box(s == 0 ? -0.05F : -0.95F, -3.6F, -1.7F, 1.0F, 3.8F, 3.4F).group(Rigs317.G_FEATHER);
         Bone tip = r.boneRel("wingTip" + n, wing, 0, -3.5F, 0);
         tip.box(s == 0 ? 0.0F : -0.8F, -3.4F, -1.6F, 0.8F, 3.6F, 3.0F);
         Bone leg = r.boneRel("leg" + n, body, sx * 1.0F, -3.0F, 0.1F);
         leg.box(-0.45F, -3.2F, -0.45F, 0.9F, 3.3F, 0.9F);
         leg.box(-0.95F, -3.2F, -1.7F, 1.9F, 0.6F, 2.3F);
      }

      r.finish();
      r.paint(c -> {
         String n = c.bone.name;
         float g = RigPaint.grain(c, 141) * 0.05F;
         if (c.cube.group == Rigs317.G_FEATHER) {
            float scale = 0.9F + 0.1F * (float)Math.sin(c.y * 3.2F + (float)Math.sin(c.x * 2.5F + c.z * 2.5F) * 1.5F);
            return RigPaint.mul(RigPaint.c(0xF8F8F4), scale + g);
         }

         if (n.startsWith("wingTip")) {
            return RigPaint.mul(RigPaint.mix(0x2C8AD8, 0x1C5FA8, RigPaint.smooth(-4.0F, 3.0F, c.y - 3.0F)), 0.95F + g);
         }

         if (n.startsWith("wing")) {
            return RigPaint.mul(RigPaint.c(0xF8F8F4), 0.92F + g);
         }

         if (n.equals("tail")) {
            return RigPaint.mul(c.y < 2.0F ? RigPaint.c(0x2C8AD8) : RigPaint.c(0xF8F8F4), 0.95F + g);
         }

         if (n.equals("beak") || n.equals("jawP")) {
            return RigPaint.mul(RigPaint.c(c.z > 12.0F && n.equals("beak") ? 0x6A5A40 : 0xE3A52E), 1.0F + g);
         }

         if (n.startsWith("leg")) {
            return RigPaint.mul(RigPaint.c(0x7C7C80), 1.0F + g);
         }

         return RigPaint.mul(RigPaint.c(0xF8F8F4), 1.0F + g);
      }, 0.05F);
      // the eyes sit on the sides of the head: a white ring round a black pupil
      r.patch(hc, Rig.RIGHT, 0.5F, 0.9F, 1.1F, 1.1F, RigPaint.c(0xF2F0E6));
      r.patch(hc, Rig.RIGHT, 0.7F, 1.1F, 0.7F, 0.7F, RigPaint.c(0x101010));
      r.patch(hc, Rig.LEFT, 1.5F, 0.9F, 1.1F, 1.1F, RigPaint.c(0xF2F0E6));
      r.patch(hc, Rig.LEFT, 1.7F, 1.1F, 0.7F, 0.7F, RigPaint.c(0x101010));
      return r;
   }
}
