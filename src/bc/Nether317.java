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
   /**
    * Update 3.18: a head with a heavy brow, slanted eyes and a toothy mouth, a crown of flames and flares at the cheeks, an ember core hanging
    * under it, twelve segmented rods in three rings (the clip turns the rings and bobs the rods) and six embers circling further out.
    * Everything glows. The bones the clip knew (head, ring0..2, rodRK) are the old ones; core, crown, flame0..3, cheekL/R, embers and
    * ember0..5 are new (AnimW317.blaze flickers and turns them).
    */
   static Rig blaze() {
      Rig r = new Rig(Mobs.BLAZE, 2, 256);
      Bone head = r.bone("head", r.root(), 0, 24, 0).glow();
      Cube hc = head.round(-4, -4, -4, 8, 8, 8, 0.5F);
      head.box(-3.9F, 0.9F, 3.8F, 7.8F, 1.0F, 0.9F).role("brow");
      head.box(-3.0F, -4.6F, 0.5F, 6.0F, 1.2F, 3.9F).role("jaw");
      Bone crown = r.boneRel("crown", head, 0, 4.0F, 0).glow();
      crown.box(-1.6F, -0.3F, -1.6F, 3.2F, 2.3F, 3.2F).role("flame");
      crown.box(-1.05F, 2.0F, -1.05F, 2.1F, 2.1F, 2.1F).role("flame");
      crown.box(-0.6F, 4.0F, -0.6F, 1.2F, 1.7F, 1.2F).role("flame");
      for (int k = 0; k < 4; k++) {
         float sx = (k & 1) == 0 ? 1.0F : -1.0F;
         float sz = (k & 2) == 0 ? 1.0F : -1.0F;
         Bone f = r.boneRel("flame" + k, head, sx * 2.5F, 4.0F, sz * 2.4F).rot(sz * 15.0F, 0.0F, -sx * 15.0F).glow();
         f.box(-1.0F, -0.3F, -1.0F, 2.0F, 2.0F, 2.0F).role("flame");
         f.box(-0.65F, 1.7F, -0.65F, 1.3F, 1.8F, 1.3F).role("flame");
         f.box(-0.4F, 3.3F, -0.4F, 0.8F, 1.3F, 0.8F).role("flame");
      }

      for (int s = 0; s < 2; s++) {
         float sx = s == 0 ? 1.0F : -1.0F;
         Bone ch = r.boneRel("cheek" + (s == 0 ? "L" : "R"), head, sx * 3.8F, -0.6F, 0.2F).rot(0.0F, 0.0F, -sx * 62.0F).glow();
         ch.box(-1.0F, -0.3F, -1.3F, 2.0F, 2.2F, 2.6F).role("flame");
         ch.box(-0.65F, 1.9F, -0.8F, 1.3F, 1.8F, 1.6F).role("flame");
      }

      Bone core = r.bone("core", r.root(), 0, 20, 0).glow();
      core.box(-2.5F, -3.2F, -2.5F, 5.0F, 3.2F, 5.0F).role("core");
      core.box(-1.9F, -6.4F, -1.9F, 3.8F, 3.3F, 3.8F).role("core");
      core.box(-1.3F, -9.4F, -1.3F, 2.6F, 3.1F, 2.6F).role("core");
      float[] radius = {9.0F, 7.0F, 5.0F};
      float[] height = {14.0F, 8.0F, 2.0F};
      for (int ring = 0; ring < 3; ring++) {
         Bone rb = r.bone("ring" + ring, r.root(), 0, 0, 0).glow();
         for (int k = 0; k < 4; k++) {
            double a = k * Math.PI / 2.0 + ring * 0.5;
            Bone rod = r.boneRel("rod" + ring + k, rb, (float)(Math.cos(a) * radius[ring]), height[ring], (float)(Math.sin(a) * radius[ring])).glow();
            rod.box(-0.95F, 0.0F, -0.95F, 1.9F, 2.4F, 1.9F).role("rodFoot");
            rod.box(-1.25F, 2.4F, -1.25F, 2.5F, 3.4F, 2.5F).role("rodBody");
            rod.box(-0.95F, 5.8F, -0.95F, 1.9F, 1.8F, 1.9F).role("rodTop");
            rod.box(-0.55F, 7.6F, -0.55F, 1.1F, 1.5F, 1.1F).role("rodTip");
         }
      }

      Bone embers = r.bone("embers", r.root(), 0, 0, 0).glow();
      for (int k = 0; k < 6; k++) {
         double a = k * Math.PI / 3.0 + 0.4;
         Bone e = r.boneRel("ember" + k, embers, (float)(Math.cos(a) * 12.5F), 5.0F + k * 3.4F, (float)(Math.sin(a) * 12.5F)).glow();
         e.box(-0.55F, -0.55F, -0.55F, 1.1F, 1.1F, 1.1F).role("ember");
         e.box(-0.3F, -2.0F, -0.3F, 0.6F, 1.5F, 0.6F).role("ember");
      }

      r.finish();
      r.paint(Nether317::blazeSkin, 0.04F);
      blazeFace(r, hc);
      return r;
   }

   private static int blazeSkin(Rig.Ctx c) {
      String role = c.cube.role == null ? "" : c.cube.role;
      float g = RigPaint.grain(c, 99) * 0.05F;
      switch (role) {
         case "flame": {
            // from the root (orange) to the tip (white-yellow); a few darker streaks
            float dx = c.x - c.bone.ax;
            float dy = c.y - c.bone.ay;
            float dz = c.z - c.bone.az;
            float len = c.bone.name.equals("crown") ? 5.8F : c.bone.name.startsWith("cheek") ? 4.0F : 4.8F;
            float t = RigPaint.smooth(0.2F, len, (float)Math.sqrt(dx * dx + dy * dy + dz * dz));

            int col = RigPaint.mix(0xE2601A, 0xF6A424, RigPaint.smooth(0.0F, 0.5F, t));
            col = RigPaint.mix(col, 0xFFF0A8, RigPaint.smooth(0.55F, 1.0F, t));
            float streak = RigPaint.noise(c.x * 2.0F, c.y * 0.6F, c.z * 2.0F, 61);
            return RigPaint.mul(RigPaint.mix(col, 0xD8480E, RigPaint.smooth(0.6F, 0.85F, streak) * 0.45F), 1.0F + g);
         }

         case "core": {
            float swirl = RigPaint.fbm(c.x * 0.5F, c.y * 0.7F, c.z * 0.5F, 71);
            int col = RigPaint.mix(0xF08A1E, 0xFFE070, RigPaint.smooth(0.35F, 0.75F, swirl));
            float crack = Math.abs(RigPaint.fbm(c.x * 0.8F + 3, c.y * 0.6F, c.z * 0.8F, 73) - 0.5F);
            if (crack < 0.03F) {
               col = 0xFFFAD0;
            }

            return RigPaint.mul(RigPaint.c(col), 1.0F + g);
         }

         case "rodFoot":
         case "rodBody":
         case "rodTop":
         case "rodTip": {
            int idx = c.bone.name.charAt(3) - '0';
            float band = 0.5F + 0.5F * (float)Math.sin(c.y * 2.1F + idx * 1.3F);
            int col;
            switch (role) {
               case "rodFoot":
                  col = RigPaint.mix(0x9A3410, 0xD0561A, band);
                  break;
               case "rodBody":
                  col = RigPaint.mix(0xF0A020, 0xE8781A, band * 0.8F + RigPaint.noise(c.x * 2, c.y * 2, c.z * 2, 3) * 0.3F);
                  break;
               case "rodTop":
                  col = RigPaint.mix(0xF8C838, 0xF09A22, band);
                  break;
               default:
                  col = RigPaint.mix(0xFFF0A0, 0xFFD060, band);
                  break;
            }

            return RigPaint.mul(RigPaint.c(col), 1.0F + g);
         }

         case "ember":
            return RigPaint.c(RigPaint.mix(0xFFD870, 0xFFA030, RigPaint.hash(c.tx, c.ty, c.face, 5)));
         case "brow":
            return RigPaint.mul(RigPaint.c(RigPaint.mix(0xB5400E, 0xD8601A, RigPaint.noise(c.x * 2, c.y * 2, c.z * 2, 8))), 1.0F + g);
         case "jaw":
            return RigPaint.mul(RigPaint.c(RigPaint.mix(0xD87A1C, 0xF0A030, RigPaint.noise(c.x * 2, c.y * 2, c.z * 2, 9))), 1.0F + g);
         default:
            break;
      }

      // the head: golden, hotter towards the middle, orange at the top, dark red cracks
      float heat = RigPaint.fbm(c.x * 0.35F, c.y * 0.35F, c.z * 0.35F, 7);
      int col = RigPaint.mix(0xF6C838, 0xE2861C, RigPaint.smooth(0.35F, 0.75F, heat));
      if (c.face == Rig.TOP) {
         col = RigPaint.mix(col, 0xB5560E, 0.5F);
      }

      float crack = Math.abs(RigPaint.fbm(c.x * 0.55F + 5, c.y * 0.55F, c.z * 0.55F, 17) - 0.5F);
      if (crack < 0.022F) {
         col = RigPaint.mix(col, 0x9A2A08, 0.75F);
      }

      return RigPaint.mul(RigPaint.c(col), 1.0F + g);
   }

   /** Slanted angry eyes with a glowing slit, a jagged mouth with teeth (texels of the face of the head, 16 x 14). */
   private static void blazeFace(Rig r, Cube hc) {
      int dark = RigPaint.c(0x2A1208);
      int glow = RigPaint.c(0xFFF2A8);
      int[][] eye = {{2, 6}, {3, 6}, {4, 6}, {3, 7}, {4, 7}, {5, 7}, {6, 7}, {5, 8}, {6, 8}, {7, 8}};
      int[][] pupil = {{4, 7}, {5, 7}};
      for (int s = 0; s < 2; s++) {
         for (int[] p : eye) {
            r.put(hc, Rig.FRONT, s == 0 ? p[0] : 15 - p[0], p[1], dark);
         }

         for (int[] p : pupil) {
            r.put(hc, Rig.FRONT, s == 0 ? p[0] : 15 - p[0], p[1], glow);
         }
      }

      for (int i = 4; i < 12; i++) {
         r.put(hc, Rig.FRONT, i, 10, dark);
         r.put(hc, Rig.FRONT, i, 11, dark);
      }

      int tooth = RigPaint.c(0xFFE880);
      for (int i = 5; i < 11; i += 2) {
         r.put(hc, Rig.FRONT, i, 10, tooth);
      }

      for (int i = 6; i < 11; i += 2) {
         r.put(hc, Rig.FRONT, i, 11, tooth);
      }
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
