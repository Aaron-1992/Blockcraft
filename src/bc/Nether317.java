package bc;

import bc.Rig.Bone;
import bc.Rig.Cube;

/** Update 3.17: the models and skins of the Nether's creatures - ghast, blaze and magma cube (the pigman is in {@link Monsters317}). */
final class Nether317 {
   private Nether317() {
   }

   /** The group that is drawn only while a ghast charges its fireball (the open face). */
   static final int G_CHARGE = 6;

   // ------------------------------------------------------------------ ghast
   /** Four blocks wide: the box is built 64 units wide with one texel per unit, a tentacle is a chain of four boxes. */
   static Rig ghast() {
      Rig r = new Rig(Mobs.GHAST, 1, 512);
      Bone body = r.bone("body", r.root(), 0, 32, 0);
      Cube bc = body.round(-32, -32, -32, 64, 64, 64, 2.0F);
      Bone face = r.boneRel("face", body, 0, 0, 0).glow();
      Cube open = face.box(-30, -26, 32.0F, 60, 56, 0.6F);
      open.group(G_CHARGE);
      int[] len = {44, 28, 48, 36, 52, 32, 40, 48, 32};
      for (int i = 0; i < 9; i++) {
         float x = (i % 3 - 1) * 20.0F;
         float z = (i / 3 - 1) * 20.0F;
         float seg = len[i] / 4.0F;
         Bone prev = r.bone("t" + i + "0", body, x, 1.0F, z);
         float w = 9.0F;
         for (int k = 0; k < 4; k++) {
            if (k > 0) {
               prev = r.boneRel("t" + i + k, prev, 0, -seg, 0);
            }

            prev.box(-w / 2, -seg, -w / 2, w, seg, w);
            w -= 1.2F;
         }
      }

      r.finish();
      r.paint(c -> {
         String n = c.bone.name;
         float g = RigPaint.grain(c, 91) * 0.03F;
         if (n.equals("face")) {
            if (c.face != Rig.FRONT) {
               return RigPaint.CLEAR;
            }

            return ghastOpenFace(c.uu + 2.0F, c.vv + 2.0F);
         }

         int white = 0xF6F6F4;
         float veins = RigPaint.fbm(c.x * 0.09F, c.y * 0.09F, c.z * 0.09F, 3);
         int col = RigPaint.mix(white, 0xD8DCE6, RigPaint.smooth(0.45F, 0.8F, veins) * 0.7F);
         if (n.startsWith("t")) {
            float band = 0.5F + 0.5F * (float)Math.sin(c.y * 0.35F);
            col = RigPaint.mix(0xEEEEF0, 0xD2D6E2, band * 0.5F + veins * 0.4F);
            if (c.y < 12.0F) {
               col = RigPaint.mix(col, 0xBCC4D8, RigPaint.smooth(12.0F, 0.0F, c.y) * 0.5F);
            }
         }

         if (n.equals("body") && c.face == Rig.FRONT) {
            int mark = ghastFace(c.uu, c.vv);
            if (mark != 0) {
               return mark;
            }
         }

         return RigPaint.mul(RigPaint.c(col), 1.0F + g);
      }, 0.05F);
      return r;
   }

   /** The sad face of a sleeping ghast: closed eyes that droop at the outer corners with tears below them, a small mouth. 64 x 64 units. */
   private static int ghastFace(float u, float v) {
      for (int e = 0; e < 2; e++) {
         float cx = e == 0 ? 19.0F : 45.0F;
         float dx = (u - cx) * (e == 0 ? 1.0F : -1.0F); // + towards the outer corner
         if (u > cx - 9.0F && u < cx + 9.0F) {
            float line = 28.0F + 0.9F * (dx + 9.0F) * 0.5F - 0.025F * dx * dx;
            if (Math.abs(v - line) < 1.5F) {
               return RigPaint.c(0x6C6C74);
            }

            if (Math.abs(v - line) < 2.3F && Math.abs(dx) < 7.0F) {
               return RigPaint.c(0xB8B8C2);
            }

            // the tear: a light blue streak running down from the outer end
            if (Math.abs(u - (cx + (e == 0 ? -6.0F : 6.0F))) < 1.0F && v > line + 2.0F && v < line + 12.0F) {
               return RigPaint.c(0xB5CBEA);
            }
         }
      }

      float mu = (u - 32.0F) / 5.0F;
      float mv = (v - 46.0F) / 3.2F;
      if (mu * mu + mv * mv < 1.0F) {
         return RigPaint.c(0x55555E);
      }

      return 0;
   }

   /** The face of a ghast that is about to shoot: glowing red eyes, a wide open mouth. The overlay is 60 x 56 units and starts 2 right, 2 down of the cube's face. */
   private static int ghastOpenFace(float u, float v) {
      for (int e = 0; e < 2; e++) {
         float cx = e == 0 ? 19.0F : 45.0F;
         if (u > cx - 8.0F && u < cx + 8.0F && v > 22.0F && v < 33.0F) {
            float pupil = Math.abs(u - cx) < 2.5F ? 1.0F : 0.0F;
            if (Math.abs(u - cx) > 7.0F || v < 22.8F || v > 32.2F) {
               return RigPaint.c(0x8A1010);
            }

            return pupil > 0.0F ? RigPaint.c(0xFFF2A0) : RigPaint.c(0xFF3A22);
         }
      }

      float mu = (u - 32.0F) / 11.0F;
      float mv = (v - 47.0F) / 9.0F;
      float d = mu * mu + mv * mv;
      if (d < 1.0F) {
         return d > 0.55F ? RigPaint.c(0x5A1218) : RigPaint.c(0x1A0508);
      }

      return RigPaint.CLEAR;
   }

   // ------------------------------------------------------------------ blaze
   static Rig blaze() {
      Rig r = new Rig(Mobs.BLAZE, 2, 256);
      Bone head = r.bone("head", r.root(), 0, 24, 0).glow();
      Cube hc = head.round(-4, -4, -4, 8, 8, 8, 0.5F);
      float[] radius = {9.0F, 7.0F, 5.0F};
      float[] height = {14.0F, 8.0F, 2.0F};
      for (int ring = 0; ring < 3; ring++) {
         Bone rb = r.bone("ring" + ring, r.root(), 0, 0, 0).glow();
         for (int k = 0; k < 4; k++) {
            double a = k * Math.PI / 2.0 + ring * 0.5;
            Bone rod = r.boneRel("rod" + ring + k, rb, (float)(Math.cos(a) * radius[ring]), height[ring], (float)(Math.sin(a) * radius[ring])).glow();
            rod.box(-1, 0, -1, 2, 8, 2);
         }
      }

      r.finish();
      r.paint(c -> {
         String n = c.bone.name;
         if (n.startsWith("rod")) {
            float band = 0.5F + 0.5F * (float)Math.sin(c.y * 1.9F + (n.charAt(3) - '0') * 1.3F);
            int col = RigPaint.mix(0xF0C030, 0xE8781A, band * 0.8F + RigPaint.noise(c.x * 2, c.y * 2, c.z * 2, 3) * 0.3F);
            return RigPaint.mul(RigPaint.c(col), 1.0F + RigPaint.grain(c, 97) * 0.08F);
         }

         float heat = RigPaint.fbm(c.x * 0.35F, c.y * 0.35F, c.z * 0.35F, 7);
         int col = RigPaint.mix(0xF6C838, 0xE2861C, RigPaint.smooth(0.35F, 0.75F, heat));
         if (c.face == Rig.TOP) {
            col = RigPaint.mix(col, 0xB5560E, 0.5F);
         }

         return RigPaint.mul(RigPaint.c(col), 1.0F + RigPaint.grain(c, 99) * 0.06F);
      }, 0.04F);
      // the face: angry slanted eyes, a flat mouth with a few teeth
      for (int s = 0; s < 2; s++) {
         float cx = s == 0 ? 2.2F : 5.8F;
         r.patch(hc, Rig.FRONT, cx - 1.1F, 2.6F, 2.2F, 1.4F, RigPaint.c(0x2A1208));
         r.patch(hc, Rig.FRONT, cx - 0.6F, 3.1F, 1.2F, 0.7F, RigPaint.c(0xFFD84A));
         r.patch(hc, Rig.FRONT, s == 0 ? cx - 1.3F : cx - 0.8F, 1.9F, 2.1F, 0.6F, RigPaint.c(0x8A3A0A));
      }

      r.patch(hc, Rig.FRONT, 2.0F, 5.2F, 4.0F, 1.6F, RigPaint.c(0x2A1208));
      for (int t = 0; t < 4; t++) {
         r.patch(hc, Rig.FRONT, 2.2F + t * 1.0F, 5.2F, 0.6F, 0.7F, RigPaint.c(0xFFD84A));
      }

      return r;
   }

   // ------------------------------------------------------------------ magma cube
   static Rig magma() {
      Rig r = new Rig(Mobs.MAGMA_CUBE, 2, 128);
      Bone core = r.bone("core", r.root(), 0, 4, 0).glow();
      core.box(-3.2F, -3.2F, -3.2F, 6.4F, 6.4F, 6.4F);
      for (int i = 0; i < 5; i++) {
         Bone s = r.bone("slice" + i, r.root(), 0, 0.65F + i * 1.675F, 0);
         s.box(-4, -0.65F, -4, 8, 1.3F, 8);
      }

      r.finish();
      r.paint(c -> {
         if (c.bone.name.equals("core")) {
            float m = RigPaint.fbm(c.x * 0.5F, c.y * 0.5F, c.z * 0.5F, 5);
            return RigPaint.c(RigPaint.mix(0xFF5A10, 0xFFC840, m));
         }

         int idx = c.bone.name.charAt(5) - '0';
         float rock = RigPaint.fbm(c.x * 0.45F, c.y * 0.9F, c.z * 0.45F, 11);
         int col = RigPaint.mix(0x2A1A14, 0x5A3A2A, RigPaint.smooth(0.35F, 0.8F, rock));
         float crack = Math.abs(RigPaint.fbm(c.x * 0.42F + idx * 3.1F, c.y * 0.3F, c.z * 0.42F, 13) - 0.5F);
         if (crack < 0.014F) {
            col = RigPaint.mix(0xFF8A20, 0xFFD860, RigPaint.noise(c.x, c.y, c.z, 3));
         } else if (crack < 0.032F) {
            col = RigPaint.mix(col, 0xB8400E, 0.7F);
         }

         if (c.face == Rig.TOP && idx == 4) {
            col = RigPaint.mix(col, 0x7A4A30, 0.3F);
         }

         if (c.face == Rig.FRONT && idx == 3) {
            float u = c.uu;
            if (c.vv > 0.2F && c.vv < 1.15F && (u > 0.9F && u < 3.3F || u > 4.7F && u < 7.1F)) {
               col = RigPaint.c(0x0C0504);
               if (c.vv > 0.45F && c.vv < 0.95F && (u > 1.5F && u < 2.7F || u > 5.3F && u < 6.5F)) {
                  col = RigPaint.c(0xFFB030);
               }
            }
         }

         if (c.face == Rig.FRONT && (idx == 1 || idx == 2) && c.vv > 0.3F && c.vv < 1.0F && c.uu > 1.4F && c.uu < 6.6F) {
            col = idx == 2 ? RigPaint.c(0x120706) : RigPaint.c(0x2A0E08);
         }

         return RigPaint.mul(RigPaint.c(col), 1.0F + RigPaint.grain(c, 103) * 0.07F);
      }, 0.1F);
      return r;
   }
}
