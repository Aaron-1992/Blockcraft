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
   /**
    * Update 3.18: a flared robe in layers with a trimmed hem, a belt with a buckle and a pouch, folded arms with shoulders, elbows, cuffs and
    * hands, boots, a head with a heavy brow, ears, a hooked nose and a horseshoe of hair - and what the professions wear: a straw hat with
    * an ear of wheat, round glasses and a book, a mitre with a stole and an amulet, goggles and a hammer with a leather apron, a butcher's
    * cap and cleaver with a white apron. The bones the clips move (body, chest, head, nose, arms, legL, legR, lidL, lidR) are the old ones.
    */
   static Rig villager() {
      Rig r = new Rig(Mobs.VILLAGER, 2, 256);
      r.groupTint[Rigs317.G_ROBE] = Rig.TINT_ROBE;
      Bone body = r.bone("body", r.root(), 0, 12, 0);
      // the skirt: four layers that flare towards the hem
      body.box(-4.6F, -3.5F, -3.3F, 9.2F, 3.5F, 6.6F).group(Rigs317.G_ROBE).role("skirt");
      body.box(-4.9F, -7.0F, -3.45F, 9.8F, 3.5F, 6.9F).group(Rigs317.G_ROBE).role("skirt");
      body.box(-5.2F, -9.5F, -3.6F, 10.4F, 2.5F, 7.2F).group(Rigs317.G_ROBE).role("skirt");
      body.box(-5.3F, -10.8F, -3.7F, 10.6F, 1.3F, 7.4F).role("hem");
      Bone chest = r.bone("chest", body, 0, 12, 0);
      chest.box(-4.2F, 0.0F, -2.9F, 8.4F, 10.0F, 5.8F).group(Rigs317.G_ROBE).role("torso");
      chest.box(-4.7F, 7.2F, -3.2F, 9.4F, 2.8F, 6.4F).group(Rigs317.G_ROBE).role("yoke");
      chest.box(-3.5F, 4.6F, -3.95F, 7.0F, 5.4F, 0.95F).group(Rigs317.G_ROBE).role("hood");
      chest.box(-4.55F, -0.7F, -3.15F, 9.1F, 1.9F, 6.3F).role("belt");
      chest.box(-1.1F, -0.5F, 3.12F, 2.2F, 1.5F, 0.4F).role("buckle");
      chest.box(2.6F, -3.2F, 3.0F, 2.6F, 3.4F, 1.4F).role("pouch");
      Bone head = r.bone("head", chest, 0, 22, 0);
      Cube hc = head.round(-4, 0, -4, 8, 10, 8, 0.6F);
      head.box(-3.7F, 5.8F, 3.72F, 7.4F, 1.1F, 0.8F).role("brow");
      head.box(-4.75F, 3.1F, -0.9F, 0.85F, 2.8F, 2.0F).role("ear");
      head.box(3.9F, 3.1F, -0.9F, 0.85F, 2.8F, 2.0F).role("ear");
      head.box(-4.3F, 3.0F, -4.35F, 8.6F, 5.6F, 0.55F).role("hair");
      head.box(-4.35F, 3.0F, -4.0F, 0.5F, 5.0F, 5.4F).role("hair");
      head.box(3.85F, 3.0F, -4.0F, 0.5F, 5.0F, 5.4F).role("hair");
      Bone nose = r.boneRel("nose", head, 0, 4.0F, 4.0F);
      nose.box(-0.85F, -1.0F, -0.2F, 1.7F, 3.2F, 1.7F).role("bridge");
      nose.box(-1.05F, -2.0F, 0.7F, 2.1F, 2.2F, 1.8F).role("noseTip");
      // folded arms: an upper arm at each side, a forearm in a sleeve that crosses in front of the chest, a cuff and a hand
      Bone arms = r.bone("arms", chest, 0, 17.5F, 0);
      for (int s = 0; s < 2; s++) {
         float sx = s == 0 ? 1.0F : -1.0F;
         String n = s == 0 ? "L" : "R";
         arms.box(s == 0 ? 4.1F : -6.6F, -3.4F, -1.7F, 2.5F, 6.6F, 3.4F).group(Rigs317.G_ROBE).role("upper");
         Bone fore = r.bone("fore" + n, arms, sx * 5.35F, 17.4F - s * 0.5F, 1.5F - s * 0.5F).rot(0.0F, -sx * 40.0F, 0.0F);
         fore.box(-1.7F, -1.8F, 0.0F, 3.4F, 3.6F, 6.6F).group(Rigs317.G_ROBE).role("sleeve");
         fore.box(-1.95F, -2.05F, 4.8F, 3.9F, 4.1F, 1.5F).role("cuff");
         Bone hand = r.boneRel("hand" + n, fore, 0.0F, 0.0F, 6.0F);
         hand.box(-1.2F, -1.2F, 0.0F, 2.4F, 2.4F, 2.0F).role("hand");
      }

      for (int s = 0; s < 2; s++) {
         float sx = s == 0 ? 1.0F : -1.0F;
         Bone leg = r.bone(s == 0 ? "legL" : "legR", body, sx * 2.0F, 12, 0);
         leg.box(-2, -12, -2, 4, 12, 4).role("leg");
         leg.box(-2.25F, -12.0F, -2.5F, 4.5F, 3.3F, 5.5F).role("boot");
         leg.box(-2.45F, -9.0F, -2.45F, 4.9F, 1.0F, 4.9F).role("bootCuff");
      }

      // what the professions wear (a pose hides all but its own group)
      Bone hat = r.boneRel("hat", head, 0, 8.4F, 0);
      hat.round(-7.2F, 0.0F, -7.2F, 14.4F, 0.9F, 14.4F, 0.3F).group(V_HAT).role("brim");
      hat.round(-4.8F, 0.8F, -4.8F, 9.6F, 3.4F, 9.6F, 0.8F).group(V_HAT).role("crown");
      hat.box(-5.0F, 0.85F, -5.0F, 10.0F, 0.9F, 10.0F).group(V_HAT).role("band");
      hat.box(4.2F, 0.9F, 2.4F, 0.6F, 4.6F, 0.6F).group(V_HAT).role("stalk");
      hat.box(3.9F, 4.4F, 2.1F, 1.2F, 2.0F, 1.2F).group(V_HAT).role("wheat");
      Bone glasses = r.boneRel("glasses", head, 0, 0, 0);
      for (int s = 0; s < 2; s++) {
         float cx = s == 0 ? 1.9F : -1.9F;
         glasses.box(cx - 1.6F, 3.65F, 4.6F, 3.2F, 2.8F, 0.3F).group(V_GLASSES).role("lens");
         glasses.box(s == 0 ? 3.9F : -4.25F, 5.3F, 0.4F, 0.35F, 0.5F, 4.5F).group(V_GLASSES).role("temple");
      }

      glasses.box(-0.4F, 5.3F, 4.6F, 0.8F, 0.5F, 0.3F).group(V_GLASSES).role("temple");
      chest.box(-5.5F, -3.0F, -1.3F, 1.0F, 3.4F, 2.6F).group(V_GLASSES).role("book");
      Bone cap = r.boneRel("cap", head, 0, 8.6F, 0);
      cap.round(-4.7F, 0.0F, -4.7F, 9.4F, 2.2F, 9.4F, 0.5F).group(V_CAP).role("capBand");
      cap.box(-3.7F, 2.1F, -3.7F, 7.4F, 2.4F, 7.4F).group(V_CAP).role("capTier");
      cap.box(-2.6F, 4.4F, -2.6F, 5.2F, 2.2F, 5.2F).group(V_CAP).role("capTier");
      cap.box(-1.5F, 6.5F, -1.5F, 3.0F, 2.0F, 3.0F).group(V_CAP).role("capTier");
      cap.box(-0.7F, 8.4F, -0.7F, 1.4F, 1.4F, 1.4F).group(V_CAP).role("amulet");
      chest.box(-1.7F, 0.4F, 2.95F, 3.4F, 9.4F, 0.55F).group(V_CAP).role("stole");
      chest.box(-1.7F, 0.4F, -3.5F, 3.4F, 9.4F, 0.55F).group(V_CAP).role("stole");
      chest.box(-0.8F, 7.8F, 3.05F, 1.6F, 1.6F, 0.45F).group(V_CAP).role("amulet");
      Bone goggles = r.boneRel("goggles", head, 0, 0, 0);
      goggles.box(-4.25F, 7.4F, -4.25F, 8.5F, 0.7F, 8.5F).group(V_SMITH).role("strap");
      for (int s = 0; s < 2; s++) {
         float cx = s == 0 ? 1.9F : -1.9F;
         goggles.box(cx - 1.4F, 7.0F, 3.9F, 2.8F, 2.4F, 1.1F).group(V_SMITH).role("goggle");
      }

      smithApron(chest, body, V_SMITH, "apronLeather");
      chest.box(-5.6F, 0.2F, -0.1F, 1.9F, 1.5F, 1.9F).group(V_SMITH).role("hammerHead");
      chest.box(-5.1F, -3.6F, 0.4F, 0.7F, 4.0F, 0.7F).group(V_SMITH).role("hammerHandle");
      Bone bcap = r.boneRel("bcap", head, 0, 8.6F, 0);
      bcap.box(-4.5F, 0.0F, -4.5F, 9.0F, 1.6F, 9.0F).group(V_BUTCHER).role("butcherBand");
      bcap.box(-3.9F, 1.5F, -3.9F, 7.8F, 1.8F, 7.8F).group(V_BUTCHER).role("butcherPuff");
      smithApron(chest, body, V_BUTCHER, "apronWhite");
      chest.box(4.8F, -3.4F, -1.6F, 0.4F, 2.6F, 3.2F).group(V_BUTCHER).role("blade");
      chest.box(4.75F, -0.8F, -0.5F, 0.5F, 1.6F, 1.0F).group(V_BUTCHER).role("cleaverHandle");
      Rigs317.lids(r, head, hc, 1.8F, 3.7F);
      r.finish();
      r.paint(Folk317::villagerSkin, 0.06F);
      villagerFace(r, hc);
      return r;
   }

   /** The apron of the smith and the butcher: a panel over the chest and one in front of each layer of the skirt. */
   private static void smithApron(Bone chest, Bone body, int group, String role) {
      chest.box(-3.4F, 0.4F, 2.95F, 6.8F, 8.6F, 0.45F).group(group).role(role);
      body.box(-3.5F, -3.5F, 3.3F, 7.0F, 3.5F, 0.4F).group(group).role(role);
      body.box(-3.6F, -7.0F, 3.45F, 7.2F, 3.5F, 0.4F).group(group).role(role);
      body.box(-3.7F, -9.5F, 3.6F, 7.4F, 2.5F, 0.4F).group(group).role(role);
   }

   private static int leather(Rig.Ctx c, int dark, int light, float k) {
      float t = RigPaint.fbm(c.x * 0.9F, c.y * 0.9F, c.z * 0.9F, 31) * 0.7F + RigPaint.grain(c, 17) * 0.15F;
      return RigPaint.mul(RigPaint.mix(dark, light, t), k);
   }

   private static int villagerSkin(Rig.Ctx c) {
      String role = c.cube.role == null ? "" : c.cube.role;
      float g = RigPaint.grain(c, 121) * 0.04F;
      int skin = RigPaint.mix(0xC99A7C, 0xB98668, RigPaint.fbm(c.x * 0.3F, c.y * 0.3F, c.z * 0.3F, 5));
      switch (role) {
         case "skirt":
         case "torso":
         case "yoke":
         case "hood":
         case "upper":
         case "sleeve": {
            // neutral cloth: the renderer multiplies it with the colour of the profession; vertical folds, a weave, darker towards the hem
            float weave = 0.5F + 0.5F * (float)Math.sin(c.y * 2.6F) * (float)Math.sin(c.x * 2.6F + c.z * 2.6F);
            float fold = 0.90F + 0.10F * (float)Math.sin(c.x * 1.9F + c.z * 1.3F + RigPaint.noise(c.x * 0.4F, 0, c.z * 0.4F, 9) * 3.0F);
            float tone = (0.92F + 0.08F * weave) * fold;
            if (role.equals("skirt")) {
               tone *= 0.80F + 0.20F * RigPaint.smooth(1.5F, 11.5F, c.y);
            }

            if (role.equals("torso") && c.y < 14.0F) {
               tone *= 0.86F + 0.14F * RigPaint.smooth(12.0F, 14.0F, c.y);
            }

            if (role.equals("upper") && c.y > 19.0F) {
               tone *= 0.94F;
            }

            if (role.equals("hood")) {
               tone *= c.face == Rig.BOTTOM ? 0.8F : 0.9F;
               if (c.face == Rig.BACK && c.y < 17.4F + 0.5F) {
                  tone *= 0.9F;
               }
            }

            if ((role.equals("yoke") || role.equals("torso")) && c.face == Rig.FRONT) {
               // the neckline: a V where the robe opens
               float depth = 22.0F - c.y;
               if (depth < 3.0F && Math.abs(c.x) < 2.6F - depth * 0.8F) {
                  tone *= Math.abs(c.x) < 2.6F - depth * 0.8F - 0.55F ? 0.55F : 0.78F;
               }
            }

            if (role.equals("sleeve") && c.face == Rig.BOTTOM) {
               tone *= 0.85F;
            }

            return RigPaint.mul(RigPaint.c(0xFFFFFF), tone + g);
         }

         case "hem": {
            int col = 0x4A3322;
            float dots = (float)Math.floor((c.x + c.z) * 1.5F);
            if (Math.abs(c.y - 1.85F) < 0.32F && ((int)dots & 1) == 0) {
               col = 0xC9A24A;
            } else if (c.y < 1.6F || c.y > 2.1F) {
               col = 0x5E4430;
            }

            return RigPaint.mul(RigPaint.c(col), 1.0F + g);
         }

         case "belt": {
            int col = leather(c, 0x4A3220, 0x6E4C30, 1.0F);
            if (c.face != Rig.TOP && c.face != Rig.BOTTOM && Math.abs(c.y - 12.25F) < 0.3F && (((int)Math.floor((c.x + c.z) * 2.0F)) & 1) == 0) {
               col = RigPaint.mix(col, 0xC8B080, 0.5F); // stitches
            }

            return RigPaint.mul(col, 1.0F + g);
         }

         case "buckle": {
            if (c.face == Rig.FRONT) {
               boolean hole = c.uu > 0.6F && c.uu < 1.6F && c.vv > 0.35F && c.vv < 1.15F;
               return hole ? RigPaint.c(0x2A1C10) : RigPaint.c(c.edge == 0 ? 0xB8892A : 0xE6C04E);
            }

            return RigPaint.c(0xB8892A);
         }

         case "pouch": {
            int col = leather(c, 0x5A3C24, 0x7C5634, 1.0F);
            if (c.face == Rig.FRONT) {
               if (c.vv < 1.0F) {
                  col = RigPaint.mul(col, 0.85F);
               }

               if (Math.abs(c.vv - 1.0F) < 0.25F) {
                  col = RigPaint.mul(col, 0.65F);
               }

               if (c.vv > 0.9F && c.vv < 1.7F && Math.abs(c.uu - 1.3F) < 0.35F) {
                  col = RigPaint.c(0xD8B24A); // the stud of the flap
               }
            }

            return RigPaint.mul(col, 1.0F + g);
         }

         case "boot":
         case "bootCuff": {
            int col = role.equals("boot") ? leather(c, 0x3A281C, 0x5A4030, 1.0F) : leather(c, 0x6A4C32, 0x8A6A46, 1.0F);
            if (role.equals("boot") && (c.face == Rig.BOTTOM || c.y < 0.9F)) {
               col = RigPaint.mix(col, 0x1C130C, 0.7F); // the sole
            }

            return RigPaint.mul(col, 1.0F + g);
         }

         case "leg":
            return RigPaint.mul(RigPaint.c(0x5E4630), 1.0F + g);
         case "cuff": {
            int col = RigPaint.mix(0xD2C29A, 0xB9A67C, RigPaint.noise(c.x * 2, c.y * 2, c.z * 2, 4));
            if (c.face == Rig.BOTTOM || c.face == Rig.TOP) {
               col = RigPaint.mul(col, 0.85F);
            }

            return RigPaint.mul(RigPaint.c(col), 1.0F + g);
         }

         case "hand":
            return RigPaint.mul(RigPaint.c(RigPaint.mix(skin, 0xA8705A, c.face == Rig.BOTTOM ? 0.5F : 0.12F)), 1.0F + g);
         case "hair": {
            float strand = RigPaint.noise(c.x * 3.0F, c.y * 1.2F, c.z * 3.0F, 41);
            int col = RigPaint.mix(0x6E5E50, 0xA39A8C, RigPaint.smooth(0.45F, 0.8F, strand));
            return RigPaint.mul(RigPaint.c(col), 1.0F + g * 1.5F);
         }

         case "brow": {
            float strand = RigPaint.hash(c.tx, c.ty, 3, 55);
            int col = RigPaint.mix(0x4E3E32, 0x8C8274, strand > 0.78F ? 0.7F : 0.15F);
            return RigPaint.c(col);
         }

         case "ear":
            return RigPaint.mul(RigPaint.c(RigPaint.mix(skin, 0xB4705C, c.face == Rig.LEFT || c.face == Rig.RIGHT ? 0.45F : 0.2F)), 1.0F + g);
         case "bridge":
            return RigPaint.mul(RigPaint.c(RigPaint.mix(skin, 0xA8705A, 0.28F)), 1.0F + g);
         case "noseTip": {
            int col = RigPaint.mix(skin, 0xB26A56, 0.5F);
            if (c.face == Rig.BOTTOM) {
               // the nostrils
               col = RigPaint.mix(col, 0x3A1E16, c.uu > 0.45F && c.uu < 0.95F || c.uu > 1.55F && c.uu < 2.05F ? 0.85F : 0.2F);
            }

            return RigPaint.mul(RigPaint.c(col), 1.0F + g);
         }

         case "brim":
         case "crown":
         case "band": {
            float straw = 0.5F + 0.5F * (float)Math.sin((c.x + c.z) * 2.0F) * (float)Math.cos((c.x - c.z) * 1.6F);
            int col = RigPaint.mix(0xD9B85C, 0xB89A42, straw * 0.7F + RigPaint.noise(c.x * 2, c.y * 2, c.z * 2, 3) * 0.3F);
            if (role.equals("band")) {
               col = RigPaint.mix(0x7A3B2A, 0x92503A, RigPaint.noise(c.x * 2, c.y * 2, c.z * 2, 5));
            }

            if (role.equals("brim") && c.face == Rig.BOTTOM) {
               col = RigPaint.mul(col, 0.8F);
            }

            return RigPaint.mul(RigPaint.c(col), 1.0F + g);
         }

         case "stalk":
            return RigPaint.mul(RigPaint.c(0xB8A04A), 1.0F + g);
         case "wheat":
            return RigPaint.mul(RigPaint.c(RigPaint.mix(0xE8CC60, 0xC9A63A, RigPaint.hash(c.tx, c.ty, c.face, 8))), 1.0F + g);
         case "lens": {
            // a ring of brass round the glass: the glass is see-through
            if (c.face == Rig.FRONT || c.face == Rig.BACK) {
               float dx = (c.fu - 0.5F) * 3.2F;
               float dy = (c.fv - 0.5F) * 2.8F;
               float d = (float)Math.sqrt(dx * dx + dy * dy * 1.3F);
               if (d > 1.45F) {
                  return RigPaint.CLEAR;
               }

               if (d > 0.95F) {
                  return RigPaint.c(0xC9A24A);
               }

               return RigPaint.CLEAR;
            }

            return RigPaint.c(0xB8923E);
         }

         case "temple":
            return RigPaint.c(0xB8923E);
         case "book": {
            if (c.face == Rig.LEFT || c.face == Rig.RIGHT) {
               int col = 0x7A2A22;
               if (c.edge == 1) {
                  col = 0xC9A24A;
               }

               return RigPaint.mul(RigPaint.c(col), 1.0F + g);
            }

            if (c.face == Rig.BACK) {
               return RigPaint.c(0x5E2018);
            }

            return RigPaint.mul(RigPaint.c(0xE6DCC0), 0.88F + 0.12F * (float)Math.sin(c.y * 7.0F));
         }

         case "capBand":
         case "capTier": {
            int col = RigPaint.mix(0x6B2E86, 0x8B3FA8, RigPaint.noise(c.x, c.y, c.z, 4));
            if (role.equals("capBand") && c.face != Rig.TOP && c.face != Rig.BOTTOM && (c.vv < 0.45F || c.vv > 1.7F)) {
               col = 0xD8B24A;
            }

            if (role.equals("capTier") && c.face == Rig.FRONT && c.y > 31.4F && c.y < 33.4F && c.z > 3.0F) {
               boolean cross = Math.abs(c.x) < 0.3F && c.y > 31.7F && c.y < 33.2F || Math.abs(c.x) < 0.95F && c.y > 32.2F && c.y < 32.7F;
               if (cross) {
                  col = 0xE6C04E;
               }
            }

            return RigPaint.mul(RigPaint.c(col), 1.0F + g);
         }

         case "stole": {
            int col = RigPaint.mix(0x7A2E96, 0x8E3CAE, RigPaint.noise(c.x, c.y, c.z, 6));
            if (c.y < 14.2F || Math.abs(c.x) > 1.3F) {
               col = 0xD8B24A;
            }

            return RigPaint.mul(RigPaint.c(col), 1.0F + g);
         }

         case "amulet": {
            int col = 0xE2BA48;
            if (c.face == Rig.FRONT && Math.abs(c.uu - 0.8F) < 0.4F && Math.abs(c.vv - 0.8F) < 0.4F) {
               col = 0xB8201C;
            }

            return RigPaint.c(col);
         }

         case "strap":
            return RigPaint.mul(leather(c, 0x32241A, 0x4A3626, 1.0F), 1.0F + g);
         case "goggle": {
            if (c.face == Rig.FRONT) {
               float dx = (c.fu - 0.5F) * 2.8F;
               float dy = (c.fv - 0.5F) * 2.4F;
               float d = (float)Math.sqrt(dx * dx + dy * dy);
               if (d < 0.85F) {
                  return RigPaint.c(d < 0.4F ? 0x9FD8E4 : 0x4F9CB0);
               }

               return RigPaint.c(d < 1.2F ? 0xB88A3A : 0x8A6A2A);
            }

            return RigPaint.c(0x8A6A2A);
         }

         case "apronLeather": {
            int col = RigPaint.mix(0x5A3A22, 0x7A5232, RigPaint.fbm(c.x * 0.8F, c.y * 0.8F, c.z * 0.8F, 6));
            float soot = RigPaint.fbm(c.x * 0.5F + 9, c.y * 0.5F, c.z * 0.5F, 12);
            col = RigPaint.mix(col, 0x2A1C14, RigPaint.smooth(0.62F, 0.8F, soot) * 0.6F);
            if (c.edge == 1 && c.face == Rig.FRONT) {
               col = RigPaint.mix(col, 0xC8B080, 0.35F); // the stitching round the edge
            }

            return RigPaint.mul(RigPaint.c(col), 1.0F + g);
         }

         case "apronWhite": {
            int col = RigPaint.mix(0xF0EAD8, 0xE2DAC4, RigPaint.noise(c.x, c.y, c.z, 14));
            if (RigPaint.fbm(c.x * 0.7F, c.y * 0.7F, c.z * 0.7F, 8) > 0.66F) {
               col = RigPaint.mix(col, 0x9A2A22, 0.8F);
            }

            if (c.edge == 1 && c.face == Rig.FRONT) {
               col = RigPaint.mix(col, 0xB8B09A, 0.5F);
            }

            return RigPaint.mul(RigPaint.c(col), 1.0F + g);
         }

         case "hammerHead":
         case "blade": {
            int col = RigPaint.mix(0x6A6E76, 0xA4AAB4, RigPaint.noise(c.x * 2, c.y * 2, c.z * 2, 21));
            if (role.equals("blade") && c.face != Rig.FRONT && c.face != Rig.BACK && (c.face == Rig.BOTTOM)) {
               col = 0xDCE2EA; // the edge
            }

            return RigPaint.mul(RigPaint.c(col), 1.0F + g);
         }

         case "hammerHandle":
         case "cleaverHandle":
            return RigPaint.mul(RigPaint.c(RigPaint.mix(0x6A4524, 0x8A6034, RigPaint.noise(c.x * 2, c.y * 2, c.z * 2, 22))), 1.0F + g);
         case "butcherBand":
         case "butcherPuff": {
            int col = RigPaint.mix(0xF4F0E6, 0xE4DECF, RigPaint.noise(c.x, c.y, c.z, 33));
            if (c.face == Rig.FRONT || c.face == Rig.BACK || c.face == Rig.LEFT || c.face == Rig.RIGHT) {
               col = RigPaint.mul(RigPaint.c(col), 0.92F + 0.08F * (float)Math.sin((c.x + c.z) * 3.0F));
            }

            return RigPaint.mul(RigPaint.c(col), 1.0F + g);
         }

         default:
            break;
      }

      // the head
      int col = skin;
      if (c.y > 31.5F || c.face == Rig.TOP) {
         col = RigPaint.mix(skin, 0x8A6048, 0.25F);
      }

      return RigPaint.mul(RigPaint.c(col), 1.0F + g);
   }

   /** The face: bags under the eyes, green eyes, a downturned mouth, blush on the cheeks (texels of the front of the head, 16 x 18). */
   private static void villagerFace(Rig r, Cube hc) {
      int white = RigPaint.c(0xF0ECE4);
      int shade = RigPaint.c(0xD8D2C8);
      int iris = RigPaint.c(0x3FA24A);
      int irisD = RigPaint.c(0x2A7A34);
      int pupil = RigPaint.c(0x10231A);
      int lash = RigPaint.c(0x45322A);
      int bag = RigPaint.c(0xA97860);
      int blush = RigPaint.c(0xD49684);
      for (int s = 0; s < 2; s++) {
         int x0 = s == 0 ? 3 : 9;
         for (int i = 0; i < 4; i++) {
            r.put(hc, Rig.FRONT, x0 + i, 8, lash);
            boolean outer = s == 0 ? i == 0 : i == 3;
            boolean inner = s == 0 ? i == 3 : i == 0;
            r.put(hc, Rig.FRONT, x0 + i, 9, outer || inner ? white : (i == (s == 0 ? 2 : 1) ? pupil : iris));
            r.put(hc, Rig.FRONT, x0 + i, 10, outer || inner ? shade : irisD);
            r.put(hc, Rig.FRONT, x0 + i, 11, bag);
         }
      }

      for (int j = 12; j < 14; j++) {
         r.put(hc, Rig.FRONT, 2, j, blush);
         r.put(hc, Rig.FRONT, 13, j, blush);
      }

      int mouth = RigPaint.c(0x7A4A3C);
      for (int i = 5; i < 11; i++) {
         r.put(hc, Rig.FRONT, i, 16, mouth);
      }

      r.put(hc, Rig.FRONT, 4, 15, RigPaint.c(0x9A6A54));
      r.put(hc, Rig.FRONT, 11, 15, RigPaint.c(0x9A6A54));
      int wrinkle = RigPaint.c(0xB48A70);
      for (int i = 3; i < 13; i++) {
         if (i != 7 && i != 8) {
            r.put(hc, Rig.FRONT, i, 3, wrinkle);
         }
      }
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
      Bone tail = r.boneRel("tail", body, 0, -2.0F, -1.5F).rot(38, 0, 0);
      tail.box(-1.3F, -5.4F, -0.45F, 2.6F, 5.4F, 0.9F).group(Rigs317.G_FEATHER);
      tail.box(-0.7F, -7.2F, -0.2F, 1.4F, 2.2F, 0.5F);
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
