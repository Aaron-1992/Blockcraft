package bc;

import bc.Rig.Bone;
import bc.Rig.Cube;

/**
 * Update 3.17: the models and skins of the walking monsters - zombie, skeleton, creeper, spider and the humanoid skeleton they share (a pelvis,
 * a chest that can twist, a head, arms and legs with elbows and knees). See {@link Rigs317} for the animals and the shared helpers.
 */
final class Monsters317 {
   private Monsters317() {
   }

   /** The bones of a humanoid; the mobs put their own boxes on them. */
   static final class Hum {
      Bone body;
      Bone chest;
      Bone head;
      Bone armL;
      Bone armR;
      Bone foreL;
      Bone foreR;
      Bone legL;
      Bone legR;
      Bone shinL;
      Bone shinR;
   }

   /** Hips at y = 12, waist 16.5, shoulders 22, neck 24: 32 units, 2 blocks. {@code armX} and {@code legX} are the distances of the joints from the middle. */
   static Hum hum(Rig r, float armX, float legX) {
      Hum h = new Hum();
      h.body = r.bone("body", r.root(), 0, 12, 0);
      h.chest = r.bone("chest", h.body, 0, 16.5F, 0);
      h.head = r.bone("head", h.chest, 0, 24, 0);
      h.armL = r.bone("armL", h.chest, armX, 22, 0);
      h.armR = r.bone("armR", h.chest, -armX, 22, 0);
      h.foreL = r.bone("foreL", h.armL, armX, 17.5F, 0);
      h.foreR = r.bone("foreR", h.armR, -armX, 17.5F, 0);
      h.legL = r.bone("legL", h.body, legX, 12, 0);
      h.legR = r.bone("legR", h.body, -legX, 12, 0);
      h.shinL = r.bone("shinL", h.legL, legX, 6, 0);
      h.shinR = r.bone("shinR", h.legR, -legX, 6, 0);
      return h;
   }

   // ------------------------------------------------------------------ zombie
   static final int Z_SKIN = 0x6A9A4F;
   static final int Z_SKIN_D = 0x486F36;
   static final int Z_SHIRT = 0x2F8FA3;
   static final int Z_PANTS = 0x34347F;
   static final int Z_SHOE = 0x2E2E30;

   static Rig zombie() {
      Rig r = new Rig(Mobs.ZOMBIE, 2, 256);
      Hum h = hum(r, 6.0F, 2.0F);
      h.body.round(-4, 0, -2, 8, 4.5F, 4, 0.5F);
      h.chest.round(-4, 0, -2, 8, 7.5F, 4, 0.5F);
      Cube hc = h.head.round(-4, 0, -4, 8, 8, 8, 0.6F);
      for (int s = 0; s < 2; s++) {
         Bone arm = s == 0 ? h.armL : h.armR;
         Bone fore = s == 0 ? h.foreL : h.foreR;
         Bone leg = s == 0 ? h.legL : h.legR;
         Bone shin = s == 0 ? h.shinL : h.shinR;
         arm.box(-2, -4.5F, -2, 4, 6.5F, 4);
         fore.box(-1.9F, -5.5F, -1.9F, 3.8F, 5.5F, 3.8F);
         leg.box(-2, -6, -2, 4, 6.5F, 4);
         shin.box(-1.9F, -6, -1.9F, 3.8F, 6, 3.8F);
         shin.box(-2.1F, -6, -2.5F, 4.2F, 2.4F, 5.2F);
      }

      Rigs317.lids(r, h.head, hc, 2.0F, 2.3F);
      r.finish();
      r.paint(c -> {
         String n = c.bone.name;
         float g = RigPaint.grain(c, 21) * 0.045F;
         float m1 = RigPaint.fbm(c.x * 0.33F, c.y * 0.33F, c.z * 0.33F, 31);
         int skin = RigPaint.mix(Z_SKIN, Z_SKIN_D, RigPaint.smooth(0.42F, 0.68F, m1));
         int base;
         if (n.equals("head") || n.startsWith("lid")) {
            base = skin;
            if (c.y > 31.0F || c.face == Rig.TOP) {
               base = RigPaint.mix(skin, 0x2A3A20, 0.45F);
            }
         } else if (n.equals("chest") || n.equals("body")) {
            float hem = 16.6F + 1.6F * RigPaint.noise(c.x * 0.9F, c.y * 0.9F, c.z * 0.9F, 41);
            if (c.y > hem) {
               base = RigPaint.mix(Z_SHIRT, 0x1E6676, RigPaint.fbm(c.x * 0.6F, c.y * 0.6F, c.z * 0.6F, 43) * 0.7F);
               if (RigPaint.fbm(c.x * 0.55F, c.y * 0.55F, c.z * 0.55F, 47) > 0.68F) {
                  base = RigPaint.mix(skin, 0x6A2A2A, 0.35F);
               }
            } else {
               base = RigPaint.mix(Z_PANTS, 0x24245C, RigPaint.fbm(c.x * 0.6F, c.y * 0.6F, c.z * 0.6F, 45) * 0.7F);
            }
         } else if (n.startsWith("arm")) {
            base = c.y > 20.0F ? RigPaint.mix(Z_SHIRT, 0x1E6676, RigPaint.fbm(c.x * 0.6F, c.y * 0.6F, c.z * 0.6F, 43) * 0.7F) : skin;
            if (c.y > 19.0F && c.y < 20.2F) {
               base = skin;
            }
         } else if (n.startsWith("fore")) {
            base = skin;
            float wound = RigPaint.fbm(c.x * 0.7F, c.y * 0.7F, c.z * 0.7F, 49);
            if (wound > 0.66F) {
               base = RigPaint.mix(skin, 0x6A2626, 0.55F);
            }

            if (c.y < 13.4F) {
               base = RigPaint.mix(skin, 0x3E5E2C, 0.3F);
            }
         } else if (n.startsWith("shin")) {
            if (c.y < 2.3F) {
               base = Z_SHOE;
            } else {
               base = RigPaint.mix(Z_PANTS, 0x24245C, RigPaint.fbm(c.x * 0.6F, c.y * 0.6F, c.z * 0.6F, 45) * 0.7F);
               if (c.y > 2.3F && c.y < 3.2F) {
                  base = RigPaint.mix(base, skin, 0.5F);
               }
            }
         } else {
            base = RigPaint.mix(Z_PANTS, 0x24245C, RigPaint.fbm(c.x * 0.6F, c.y * 0.6F, c.z * 0.6F, 45) * 0.7F);
            if (c.y < 9.0F && RigPaint.fbm(c.x * 0.8F, c.y * 0.8F, c.z * 0.8F, 51) > 0.68F) {
               base = skin;
            }
         }

         return RigPaint.mul(RigPaint.c(base), 1.0F + g);
      }, 0.06F);
      // the face: sockets with dead pale eyes, a brow, a nose and a mouth with a few teeth
      for (int s = 0; s < 2; s++) {
         float cx = s == 0 ? 2.0F : 6.0F;
         r.patch(hc, Rig.FRONT, cx - 1.2F, 2.0F, 2.4F, 1.8F, RigPaint.c(0x1A2614));
         r.patch(hc, Rig.FRONT, cx - 0.8F, 2.3F, 1.6F, 1.2F, RigPaint.c(0xC9D47C));
         r.patch(hc, Rig.FRONT, s == 0 ? cx : cx - 0.8F, 2.3F, 0.8F, 1.2F, RigPaint.c(0x0C0C0A));
         r.patch(hc, Rig.FRONT, cx - 1.4F, 1.4F, 2.8F, 0.5F, RigPaint.c(0x3A5A2A));
      }

      r.patch(hc, Rig.FRONT, 3.4F, 3.9F, 1.2F, 0.9F, RigPaint.c(0x3B5A2B));
      r.patch(hc, Rig.FRONT, 2.0F, 5.2F, 4.0F, 1.1F, RigPaint.c(0x20140F));
      for (int t = 0; t < 4; t++) {
         r.patch(hc, Rig.FRONT, 2.2F + t * 1.0F, 5.2F, 0.5F, 0.6F, RigPaint.c(0xD9D2B0));
      }

      return r;
   }

   // ------------------------------------------------------------------ skeleton
   static Rig skeleton() {
      Rig r = new Rig(Mobs.SKELETON, 2, 256);
      Hum h = hum(r, 5.2F, 2.0F);
      h.body.box(-3.4F, 0.2F, -1.6F, 6.8F, 2.3F, 3.2F);
      h.body.box(-0.8F, 2.5F, -0.9F, 1.6F, 2.0F, 1.6F);
      h.chest.box(-0.8F, 0, -1.3F, 1.6F, 7.2F, 1.6F);
      for (int i = 0; i < 4; i++) {
         h.chest.box(0.9F, 0.4F + i * 1.75F, -2.0F, 2.8F, 1.0F, 4.0F);
         h.chest.box(-3.7F, 0.4F + i * 1.75F, -2.0F, 2.8F, 1.0F, 4.0F);
      }

      h.chest.box(-0.7F, 0.5F, 1.2F, 1.4F, 6.0F, 0.8F);
      h.chest.box(-4.7F, 6.4F, -1.0F, 9.4F, 1.0F, 2.0F);
      Cube hc = h.head.round(-4, 0, -4, 8, 8, 8, 0.6F);
      Bone jaw = r.bone("jaw", h.head, 0, 24.8F, 3.6F);
      jaw.box(-2.6F, -1.0F, -6.4F, 5.2F, 1.2F, 6.8F);
      for (int s = 0; s < 2; s++) {
         Bone arm = s == 0 ? h.armL : h.armR;
         Bone fore = s == 0 ? h.foreL : h.foreR;
         Bone leg = s == 0 ? h.legL : h.legR;
         Bone shin = s == 0 ? h.shinL : h.shinR;
         arm.box(-1, -4.5F, -1, 2, 6.5F, 2);
         fore.box(-0.9F, -5.5F, -0.9F, 1.8F, 5.5F, 1.8F);
         fore.box(-1.2F, -6.2F, -1.2F, 2.4F, 1.4F, 2.4F);
         leg.box(-1, -6, -1, 2, 6.5F, 2);
         shin.box(-0.9F, -6, -0.9F, 1.8F, 6, 1.8F);
         shin.box(-1.2F, -6, -1.9F, 2.4F, 1.2F, 3.8F);
      }

      // the bow: held in the left hand, the limbs along the arm's z axis (up when the arm points forward), the tips bend towards the archer
      Bone bow = r.boneRel("bow", h.foreL, 0, -5.4F, 0);
      bow.box(-0.6F, -0.9F, -1.0F, 1.2F, 1.8F, 2.0F);
      Bone u1 = r.boneRel("bowU1", bow, 0, 0, 0.9F).rot(-12, 0, 0);
      u1.box(-0.45F, -0.45F, 0, 0.9F, 0.9F, 3.4F);
      Bone u2 = r.boneRel("bowU2", u1, 0, 0, 3.4F).rot(-22, 0, 0);
      u2.box(-0.4F, -0.4F, 0, 0.8F, 0.8F, 3.2F);
      Bone l1 = r.boneRel("bowL1", bow, 0, 0, -0.9F).rot(12, 0, 0);
      l1.box(-0.45F, -0.45F, -3.4F, 0.9F, 0.9F, 3.4F);
      Bone l2 = r.boneRel("bowL2", l1, 0, 0, -3.4F).rot(22, 0, 0);
      l2.box(-0.4F, -0.4F, -3.2F, 0.8F, 0.8F, 3.2F);
      Bone sU = r.boneRel("strU", bow, 0, 2.5F, 6.9F);
      sU.box(-0.12F, -0.12F, -6.9F, 0.24F, 0.24F, 6.9F);
      Bone sL = r.boneRel("strL", bow, 0, 2.5F, -6.9F);
      sL.box(-0.12F, -0.12F, 0, 0.24F, 0.24F, 6.9F);
      Rigs317.lids(r, h.head, hc, 2.0F, 2.2F);
      r.finish();
      r.paint(c -> {
         String n = c.bone.name;
         float g = RigPaint.grain(c, 61) * 0.05F;
         int bone = 0xD9D9CC;
         if (n.startsWith("bow") || n.startsWith("str")) {
            int wood = RigPaint.mix(0x6B4A2A, 0x8A6238, RigPaint.noise(c.x * 0.5F, c.y * 0.5F, c.z * 0.5F, 3));
            return n.startsWith("str") ? RigPaint.c(0xE6E2D6) : RigPaint.mul(RigPaint.c(c.bone.name.equals("bow") ? 0x3A2818 : wood), 1.0F + g);
         }

         float streak = 0.93F + 0.12F * RigPaint.noise(c.x * 0.8F, c.y * 0.22F, c.z * 0.8F, 5);
         float crack = RigPaint.fbm(c.x * 0.9F, c.y * 0.9F, c.z * 0.9F, 7) > 0.72F ? 0.8F : 1.0F;
         int col = RigPaint.mix(bone, 0xB8B49C, RigPaint.fbm(c.x * 0.3F, c.y * 0.3F, c.z * 0.3F, 9) * 0.6F);
         if (n.equals("jaw") && c.face == Rig.TOP) {
            col = RigPaint.mix(col, 0x2A2420, 0.0F);
         }

         return RigPaint.mul(RigPaint.c(col), (streak + g) * crack);
      }, 0.07F);
      // the skull: eye sockets, nose, a row of teeth on the jaw
      for (int s = 0; s < 2; s++) {
         float cx = s == 0 ? 2.0F : 6.0F;
         r.patch(hc, Rig.FRONT, cx - 1.0F, 2.0F, 2.0F, 1.8F, RigPaint.c(0x14110F));
         r.patch(hc, Rig.FRONT, s == 0 ? cx + 0.2F : cx - 0.6F, 2.5F, 0.5F, 0.6F, RigPaint.c(0x6A0E0E));
      }

      r.patch(hc, Rig.FRONT, 3.4F, 4.0F, 1.2F, 1.0F, RigPaint.c(0x2A2420));
      Cube jc = jaw.cubes.get(0);
      for (int t = 0; t < 6; t++) {
         r.patch(jc, Rig.TOP, 0.4F + t * 0.8F, 0.0F, 0.4F, 6.8F, RigPaint.c(0x3A322C));
      }

      return r;
   }

   // ------------------------------------------------------------------ creeper
   static Rig creeper() {
      Rig r = new Rig(Mobs.CREEPER, 2, 128);
      Bone body = r.bone("body", r.root(), 0, 18, 0);
      body.round(-4, -12, -2, 8, 12, 4, 0.6F);
      Bone head = r.bone("head", body, 0, 18, 0);
      Cube hc = head.round(-4, 0, -4, 8, 8, 8, 0.6F);
      for (int i = 0; i < 4; i++) {
         String n = i == 0 ? "FL" : (i == 1 ? "FR" : (i == 2 ? "BL" : "BR"));
         float x = i % 2 == 0 ? 2.0F : -2.0F;
         float z = i < 2 ? 4.0F : -4.0F;
         Bone leg = r.bone("leg" + n, r.root(), x, 6.2F, z);
         leg.box(-2, -3.2F, -2, 4, 3.6F, 4);
         Bone shin = r.bone("shin" + n, leg, x, 3.0F, z);
         shin.box(-1.9F, -3.0F, -1.9F, 3.8F, 3.0F, 3.8F);
      }

      r.finish();
      r.paint(c -> {
         float g = RigPaint.grain(c, 71) * 0.06F;
         float a = RigPaint.noise(c.x * 0.55F, c.y * 0.55F, c.z * 0.55F, 3);
         float b = RigPaint.noise(c.x * 1.3F, c.y * 1.3F, c.z * 1.3F, 4);
         int col = 0x59A84A;
         if (a + b * 0.5F > 0.95F) {
            col = 0x2F6E2A;
         } else if (a * 0.8F + b * 0.5F < 0.5F) {
            col = 0x7DC468;
         }

         if (b > 0.74F) {
            col = RigPaint.mix(col, 0x1E4A1C, 0.5F);
         }

         if (c.bone.name.startsWith("shin") && c.y < 1.2F) {
            col = RigPaint.mix(col, 0x2A5A24, 0.5F);
         }

         return RigPaint.mul(RigPaint.c(col), 1.0F + g);
      }, 0.06F);
      // the face: two eyes and the frowning mouth (the classic one, drawn at twice the size)
      int dark = RigPaint.c(0x0B0F0B);
      r.patch(hc, Rig.FRONT, 1.0F, 1.2F, 2.0F, 2.0F, dark);
      r.patch(hc, Rig.FRONT, 5.0F, 1.2F, 2.0F, 2.0F, dark);
      r.patch(hc, Rig.FRONT, 3.0F, 3.2F, 2.0F, 3.0F, dark);
      r.patch(hc, Rig.FRONT, 2.0F, 4.2F, 1.0F, 2.2F, dark);
      r.patch(hc, Rig.FRONT, 5.0F, 4.2F, 1.0F, 2.2F, dark);
      return r;
   }

   // ------------------------------------------------------------------ spider
   static Rig spider() {
      Rig r = new Rig(Mobs.SPIDER, 2, 256);
      Bone body = r.bone("body", r.root(), 0, 9, 0);
      body.round(-3.5F, -3.5F, -3.5F, 7, 7, 7, 0.8F);
      Bone abd = r.bone("abdomen", body, 0, 9.4F, -3.0F);
      abd.blob(-5.5F, -4.6F, -13.5F, 11, 9.2F, 14, 1.5F);
      Bone head = r.bone("head", body, 0, 9, 3.0F);
      Cube hc = head.round(-4, -3.8F, 0, 8, 7.6F, 7.5F, 0.8F);
      for (int s = 0; s < 2; s++) {
         float sx = s == 0 ? 1.0F : -1.0F;
         Bone fang = r.boneRel("fang" + (s == 0 ? "L" : "R"), head, sx * 1.5F, -2.6F, 7.2F).rot(-8, 0, 0);
         fang.box(-0.6F, -3.0F, -0.6F, 1.2F, 3.2F, 1.2F);
         Bone palp = r.boneRel("palp" + (s == 0 ? "L" : "R"), head, sx * 3.3F, -2.5F, 6.8F).rot(-20, 0, 0);
         palp.box(-0.8F, -3.0F, -0.8F, 1.6F, 3.4F, 1.6F);
      }

      Bone eyes = r.boneRel("eyes", head, 0, 0, 0).glow();
      float[][] eye = {{2.0F, 1.2F, 1.6F}, {-2.0F, 1.2F, 1.6F}, {3.2F, 2.4F, 1.1F}, {-3.2F, 2.4F, 1.1F}, {0.9F, 2.5F, 0.9F}, {-0.9F, 2.5F, 0.9F}, {2.9F, 0.0F, 0.9F}, {-2.9F, 0.0F, 0.9F}};
      for (float[] e : eye) {
         eyes.box(e[0] - e[2] / 2, e[1] - e[2] / 2, 7.4F, e[2], e[2], 0.28F);
      }

      float[] zs = {2.4F, 0.8F, -0.8F, -2.4F};
      float[] yaw = {-42.0F, -16.0F, 16.0F, 42.0F};
      for (int s = 0; s < 2; s++) {
         float sx = s == 0 ? 1.0F : -1.0F;
         String sn = s == 0 ? "L" : "R";
         for (int i = 0; i < 4; i++) {
            Bone up = r.boneRel("leg" + sn + i, body, sx * 3.2F, 0, zs[i]).rot(0, sx * yaw[i], sx * 28);
            up.box(s == 0 ? 0 : -7.6F, -0.95F, -0.95F, 7.6F, 1.9F, 1.9F);
            Bone lo = r.boneRel("shin" + sn + i, up, sx * 7.6F, 0, 0).rot(0, sx * yaw[i] * 0.6F, -sx * 92);
            lo.box(s == 0 ? 0 : -13.5F, -0.8F, -0.8F, 13.5F, 1.6F, 1.6F);
         }
      }

      r.finish();
      r.paint(c -> {
         String n = c.bone.name;
         float g = RigPaint.grain(c, 81) * 0.07F;
         int dark = 0x2B2220;
         if (n.equals("eyes")) {
            return RigPaint.c(0xE02A2A);
         }

         if (n.startsWith("fang")) {
            return RigPaint.mul(RigPaint.mix(0xB9A98E, 0x3A1A14, RigPaint.smooth(0.0F, 1.0F, c.fv)), 1.0F + g);
         }

         int col = dark;
         float hair = RigPaint.noise(c.x * 1.6F, c.y * 1.6F, c.z * 1.6F, 5);
         col = RigPaint.mix(col, 0x4B3A30, RigPaint.smooth(0.55F, 0.9F, hair) * 0.8F);
         if (n.startsWith("shin") || n.startsWith("leg")) {
            float band = 0.5F + 0.5F * (float)Math.sin((Math.abs(c.x) + c.z * 0.2F) * 1.1F);
            col = RigPaint.mix(col, 0x4A382E, band * 0.6F);
            if (n.startsWith("shin") && Math.abs(c.x) > 18.0F) {
               col = RigPaint.mix(col, 0x1A1412, 0.4F);
            }
         }

         if (n.equals("abdomen")) {
            float mark = Math.abs(c.x) < 2.0F + 1.2F * (float)Math.sin(c.z * 0.5F) ? 1.0F : 0.0F;
            if (c.ny > 0.5F && mark > 0.0F) {
               col = RigPaint.mix(col, 0x7A5A44, 0.7F);
            }
         }

         return RigPaint.mul(RigPaint.c(col), 1.0F + g);
      }, 0.06F);
      return r;
   }
}
