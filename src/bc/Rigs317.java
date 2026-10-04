package bc;

import bc.Rig.Bone;
import bc.Rig.Cube;
import bc.Rig.Ctx;

/**
 * Update 3.17: the models and skins of the mobs. Every mob is a {@link Rig}: bones with boxes, a skin painted by a shader that knows where on
 * the body every texel is (so patterns run on over the edges of the boxes), and a few hand placed details (eyes, nostrils, mouths).
 * The clips that move the bones are in {@link Anim317}; their names for the bones (body, head, legFL ...) are what the two agree on.
 */
final class Rigs317 {
   /** Groups of boxes that a pose can hide. */
   static final int G_LID = 1;
   /** Tinted groups: wool (dye), a villager's robe, a parrot's feathers (see Rig.groupTint). */
   static final int G_WOOL = 2;
   static final int G_ROBE = 3;
   static final int G_FEATHER = 4;
   static final int G_HAT = 5;

   private Rigs317() {
   }

   /** The rig of a mob type, or null if the type still has its old model. */
   static Rig build(int type) {
      switch (type) {
         case Mobs.PIG:
            return pig();
         case Mobs.COW:
            return cow();
         case Mobs.SHEEP:
            return sheep();
         case Mobs.CHICKEN:
            return chicken();
         case Mobs.ZOMBIE:
            return Monsters317.zombie();
         case Mobs.SKELETON:
            return Monsters317.skeleton();
         case Mobs.CREEPER:
            return Monsters317.creeper();
         case Mobs.SPIDER:
            return Monsters317.spider();
         case Mobs.ZOMBIE_PIGMAN:
            return Monsters317.pigman();
         case Mobs.GHAST:
            return Nether317.ghast();
         case Mobs.BLAZE:
            return Nether317.blaze();
         case Mobs.MAGMA_CUBE:
            return Nether317.magma();
         case Mobs.ENDERMAN:
            return End317.enderman();
         case Mobs.VILLAGER:
            return Folk317.villager();
         case Mobs.IRON_GOLEM:
            return Folk317.golem();
         case Mobs.PARROT:
            return Folk317.parrot();
         default:
            return null;
      }
   }

   static int[] pigColors() {
      return new int[]{0xEE9E98, 0xD8858B, 0x5A4650, 0x2A1A1E};
   }

   /** Size of the eyes (model units) that {@link #lids} and {@link #eyes} agree on. */
   static final float EW = 1.4F;
   static final float EH = 1.2F;

   /**
    * Eyelids for a pair of eyes on the front face of a head box (see {@link #eyes}): two flat boxes in the skin colour in front of the eyes,
    * hidden unless the mob blinks. {@code eyeX} is the distance of an eye's middle from the middle of the face, {@code fromTop} the
    * distance of its top from the top edge of the face.
    */
   static void lids(Rig r, Bone head, Cube hc, float eyeX, float fromTop) {
      float top = hc.y + hc.h - fromTop;
      float faceZ = hc.z + hc.d;
      float mid = hc.x + hc.w / 2;
      Bone l = r.boneRel("lidL", head, mid + eyeX, top, faceZ);
      l.box(-EW / 2, -EH, 0.0F, EW, EH, 0.25F).group(G_LID);
      Bone rr = r.boneRel("lidR", head, mid - eyeX, top, faceZ);
      rr.box(-EW / 2, -EH, 0.0F, EW, EH, 0.25F).group(G_LID);
   }

   // ------------------------------------------------------------------ pig
   private static Rig pig() {
      Rig r = new Rig(Mobs.PIG, 2, 256);
      Bone root = r.root();
      Bone body = r.bone("body", root, 0, 9.5F, 0);
      body.round(-5, -4, -8, 10, 8, 15, 0.9F);
      Bone head = r.bone("head", body, 0, 11.5F, 7.5F);
      Cube hc = head.blob(-4, -4.5F, -1, 8, 7.5F, 8, 0.7F);
      Bone snout = r.boneRel("snout", head, 0, -1.8F, 7);
      snout.box(-2.5F, -1.6F, 0, 5, 3.2F, 2.2F);
      snout.box(-2.9F, -0.6F, -0.6F, 5.8F, 1.2F, 1.0F);
      Bone earL = r.boneRel("earL", head, 3.0F, 3.1F, 2.2F).rot(-34, 0, 38);
      earL.box(-1.8F, -3.4F, -0.4F, 3.6F, 3.4F, 0.8F);
      Bone earR = r.boneRel("earR", head, -3.0F, 3.1F, 2.2F).rot(-34, 0, -38);
      earR.box(-1.8F, -3.4F, -0.4F, 3.6F, 3.4F, 0.8F);
      lids(r, head, hc, 2.4F, 1.0F);
      Bone t1 = r.bone("tail1", body, 0, 12.5F, -8);
      t1.rot(-35, 0, 0);
      t1.box(-0.6F, 0, -0.6F, 1.2F, 1.8F, 1.2F);
      Bone t2 = r.boneRel("tail2", t1, 0, 1.8F, 0).rot(40, 0, 0);
      t2.box(-0.55F, 0, -0.55F, 1.1F, 1.6F, 1.1F);
      Bone t3 = r.boneRel("tail3", t2, 0, 1.6F, 0).rot(45, 0, 0);
      t3.box(-0.5F, 0, -0.5F, 1.0F, 1.4F, 1.0F);
      for (int i = 0; i < 4; i++) {
         String n = i == 0 ? "FL" : (i == 1 ? "FR" : (i == 2 ? "BL" : "BR"));
         float x = i % 2 == 0 ? 3.1F : -3.1F;
         float z = i < 2 ? 5.0F : -5.0F;
         Bone leg = r.bone("leg" + n, root, x, 7.2F, z);
         leg.box(-2.0F, -3.7F, -2.0F, 4.0F, 4.4F, 4.0F);
         Bone shin = r.bone("shin" + n, leg, x, 3.5F, z);
         shin.box(-1.6F, -3.5F, -1.6F, 3.2F, 3.5F, 3.2F);
      }

      r.finish();
      int[] col = pigColors();
      r.paint(c -> {
         String n = c.bone.name;
         float g = RigPaint.grain(c, 3) * 0.035F;
         float blot = (RigPaint.fbm(c.x * 0.22F, c.y * 0.22F, c.z * 0.22F, 11) - 0.5F) * 0.22F;
         int base = col[0];
         if (n.equals("snout")) {
            base = col[1];
         } else if (n.startsWith("ear")) {
            base = c.nz != 0 || c.face == Rig.FRONT || c.face == Rig.BACK ? RigPaint.mix(col[0], col[1], 0.5F) : col[0];
         } else if (n.startsWith("shin") && c.y < 1.9F) {
            return RigPaint.mul(col[2], 1.0F + g * 2.0F);
         } else if (n.startsWith("tail")) {
            base = RigPaint.mix(col[0], col[1], 0.4F);
         } else if (n.startsWith("lid")) {
            base = col[0];
         }

         float belly = 1.0F - 0.16F * RigPaint.smooth(8.5F, 4.0F, c.y);
         return RigPaint.mul(base, (1.0F + g + blot) * belly);
      }, 0.05F);
      // the face: eyes and nostrils
      eyes(r, hc, 2.4F, 1.0F, 0x14100E, 0xF4EEE8);
      Cube sc = snout.cubes.get(0);
      for (int side = 0; side < 2; side++) {
         float nx = side == 0 ? 0.9F : 3.3F;
         r.patch(sc, Rig.FRONT, nx, 1.0F, 0.8F, 1.1F, RigPaint.c(col[3]));
      }

      return r;
   }

   // ------------------------------------------------------------------ cow
   private static Rig cow() {
      Rig r = new Rig(Mobs.COW, 2, 256);
      Bone root = r.root();
      Bone body = r.bone("body", root, 0, 17.5F, 0);
      body.round(-6, -5.5F, -10.5F, 12, 11, 20, 1.0F);
      body.box(-2.4F, -6.9F, -8.2F, 4.8F, 1.6F, 5.2F);
      for (int i = 0; i < 4; i++) {
         body.box((i % 2 == 0 ? 1.0F : -1.0F) * 1.4F - 0.45F, -8.0F, i < 2 ? -7.8F : -4.2F, 0.9F, 1.3F, 0.9F);
      }

      Bone head = r.bone("head", body, 0, 20.5F, 10.0F);
      Cube hc = head.blob(-4, -4.2F, -1.5F, 8, 8.2F, 7.5F, 0.7F);
      Bone muzzle = r.boneRel("muzzle", head, 0, -2.4F, 6.0F);
      Cube mz = muzzle.box(-3.2F, -2.0F, 0, 6.4F, 4.0F, 2.2F);
      for (int s = 0; s < 2; s++) {
         float sx = s == 0 ? 1.0F : -1.0F;
         Bone horn = r.boneRel("horn" + (s == 0 ? "L" : "R"), head, sx * 3.7F, 3.5F, 1.6F).rot(0, 0, -sx * 62);
         horn.box(-0.75F, 0, -0.75F, 1.5F, 1.7F, 1.5F);
         Bone tip = r.boneRel("hornTip" + (s == 0 ? "L" : "R"), horn, 0, 1.7F, 0).rot(0, 0, sx * 52);
         tip.box(-0.55F, 0, -0.55F, 1.1F, 1.9F, 1.1F);
         Bone ear = r.boneRel("ear" + (s == 0 ? "L" : "R"), head, sx * 4.1F, 1.6F, 0.6F).rot(0, 0, sx * -14);
         ear.box(s == 0 ? 0 : -3.2F, -0.55F, -1.3F, 3.2F, 1.1F, 2.6F);
      }

      lids(r, head, hc, 2.5F, 1.2F);
      Bone t1 = r.bone("tail1", body, 0, 21.5F, -10.4F);
      t1.rot(8, 0, 0);
      t1.box(-0.7F, -5.6F, -0.7F, 1.4F, 5.8F, 1.4F);
      Bone t2 = r.boneRel("tail2", t1, 0, -5.6F, 0);
      t2.box(-1.2F, -3.6F, -1.2F, 2.4F, 3.8F, 2.4F);
      for (int i = 0; i < 4; i++) {
         String n = i == 0 ? "FL" : (i == 1 ? "FR" : (i == 2 ? "BL" : "BR"));
         float x = i % 2 == 0 ? 3.7F : -3.7F;
         float z = i < 2 ? 6.6F : -6.6F;
         Bone leg = r.bone("leg" + n, root, x, 12.6F, z);
         leg.box(-2.2F, -6.4F, -2.2F, 4.4F, 7.0F, 4.4F);
         Bone shin = r.bone("shin" + n, leg, x, 6.2F, z);
         shin.box(-1.9F, -6.2F, -1.9F, 3.8F, 6.2F, 3.8F);
      }

      r.finish();
      final int white = 0xF1EEE8;
      final int black = 0x2B2827;
      final int pink = 0xE9AFA6;
      r.paint(c -> {
         String n = c.bone.name;
         float g = RigPaint.grain(c, 7) * 0.035F;
         if (n.equals("muzzle")) {
            return RigPaint.mul(RigPaint.c(pink), 1.0F + g + (RigPaint.fbm(c.x * 0.5F, c.y * 0.5F, c.z * 0.5F, 3) - 0.5F) * 0.12F);
         }

         if (n.startsWith("horn")) {
            float tipK = n.startsWith("hornTip") ? 0.45F + 0.3F * RigPaint.smooth(0.0F, 1.0F, c.fv) : 0.0F;
            return RigPaint.mul(RigPaint.mix(0xEDE5CC, 0x6B6150, tipK), 1.0F + g);
         }

         if (n.startsWith("ear")) {
            return c.face == Rig.BOTTOM || c.face == Rig.FRONT ? RigPaint.mul(RigPaint.c(pink), 0.9F + g) : RigPaint.mul(RigPaint.c(black), 1.0F + g);
         }

         if (n.equals("body") && c.y < 12.9F && c.z < -2.0F && c.z > -9.0F && Math.abs(c.x) < 2.7F) {
            return RigPaint.mul(RigPaint.c(pink), 0.95F + g * 2.0F);
         }

         if (n.equals("body") && c.y < 11.9F) {
            return RigPaint.mul(RigPaint.c(pink), 0.92F + g * 2.0F);
         }

         if (n.startsWith("shin") && c.y < 2.0F) {
            return RigPaint.mul(RigPaint.c(0x453C34), 1.0F + g * 2.0F);
         }

         if (n.equals("tail2")) {
            return RigPaint.mul(RigPaint.c(0x3A2E2A), 1.0F + g * 2.0F);
         }

         if (n.equals("head") || n.startsWith("lid")) {
            float blaze = c.face == Rig.FRONT || c.face == Rig.TOP ? Math.abs(c.fu - 0.5F) : 9.0F;
            if (blaze < 0.16F && n.equals("head")) {
               return RigPaint.mul(RigPaint.c(white), 1.0F + g);
            }

            return RigPaint.mul(RigPaint.c(black), 1.0F + g);
         }

         float p = RigPaint.fbm(c.x * 0.072F, c.y * 0.085F, c.z * 0.072F, 5);
         float t = RigPaint.smooth(0.505F, 0.54F, p);
         int base = RigPaint.mix(white, black, t);
         if (t < 0.5F) {
            base = RigPaint.mul(base, 0.97F + (RigPaint.noise(c.x * 0.7F, c.y * 0.7F, c.z * 0.7F, 9) - 0.5F) * 0.07F);
         }

         return RigPaint.mul(base, 1.0F + g);
      }, 0.05F);
      eyes(r, hc, 2.5F, 1.2F, 0x16110F, 0xEFE8DC);
      for (int s = 0; s < 2; s++) {
         r.patch(mz, Rig.FRONT, s == 0 ? 1.0F : 4.2F, 1.4F, 1.2F, 1.3F, RigPaint.c(0x6F4A46));
      }

      return r;
   }

   // ------------------------------------------------------------------ sheep
   private static Rig sheep() {
      Rig r = new Rig(Mobs.SHEEP, 2, 256);
      r.groupTint[G_WOOL] = Rig.TINT_WOOL;
      Bone root = r.root();
      Bone body = r.bone("body", root, 0, 16.0F, 0);
      body.round(-4.2F, -4.4F, -7.6F, 8.4F, 8.8F, 15.2F, 0.8F);
      body.blob(-6.0F, -6.4F, -9.6F, 12, 12.8F, 19.2F, 1.8F).group(G_WOOL);
      Bone head = r.bone("head", body, 0, 18.6F, 8.8F);
      Cube hc = head.blob(-3.0F, -3.8F, -0.8F, 6, 7.2F, 6.8F, 0.6F);
      head.blob(-3.7F, 1.6F, -1.4F, 7.4F, 3.0F, 6.0F, 0.9F).group(G_WOOL);
      head.box(-2.4F, -3.9F, 4.8F, 4.8F, 3.0F, 2.2F);
      for (int s = 0; s < 2; s++) {
         float sx = s == 0 ? 1.0F : -1.0F;
         Bone ear = r.boneRel("ear" + (s == 0 ? "L" : "R"), head, sx * 3.0F, 1.2F, 0.8F).rot(0, 0, sx * -22);
         ear.box(s == 0 ? 0 : -3.0F, -0.6F, -1.1F, 3.0F, 1.2F, 2.2F);
      }

      lids(r, head, hc, 1.6F, 1.0F);
      Bone tail = r.bone("tail1", body, 0, 19.0F, -9.4F);
      tail.box(-1.4F, -1.8F, -2.2F, 2.8F, 3.6F, 2.4F).group(G_WOOL);
      for (int i = 0; i < 4; i++) {
         String n = i == 0 ? "FL" : (i == 1 ? "FR" : (i == 2 ? "BL" : "BR"));
         float x = i % 2 == 0 ? 3.0F : -3.0F;
         float z = i < 2 ? 5.6F : -5.6F;
         Bone leg = r.bone("leg" + n, root, x, 11.4F, z);
         leg.box(-1.8F, -5.6F, -1.8F, 3.6F, 6.2F, 3.6F);
         leg.box(-2.5F, -5.2F, -2.5F, 5.0F, 5.8F, 5.0F).group(G_WOOL);
         Bone shin = r.bone("shin" + n, leg, x, 5.8F, z);
         shin.box(-1.6F, -5.8F, -1.6F, 3.2F, 5.8F, 3.2F);
      }

      r.finish();
      r.paint(c -> {
         String n = c.bone.name;
         float g = RigPaint.grain(c, 11) * 0.035F;
         if (c.cube.group == G_WOOL) {
            float a = RigPaint.noise(c.x * 0.75F, c.y * 0.75F, c.z * 0.75F, 5);
            float b = RigPaint.noise(c.x * 1.7F, c.y * 1.7F, c.z * 1.7F, 6);
            float v = 0.80F + 0.30F * (a * 0.6F + b * 0.4F) + g;
            return RigPaint.mul(RigPaint.c(0xF4F2EE), v);
         }

         if (n.equals("head") || n.startsWith("lid")) {
            return RigPaint.mul(RigPaint.c(0xCDBFA8), 1.0F + g + (RigPaint.fbm(c.x * 0.4F, c.y * 0.4F, c.z * 0.4F, 4) - 0.5F) * 0.12F);
         }

         if (n.startsWith("ear")) {
            return c.face == Rig.BOTTOM || c.face == Rig.FRONT ? RigPaint.mul(RigPaint.c(0xD9A99B), 1.0F + g) : RigPaint.mul(RigPaint.c(0xB5A48F), 1.0F + g);
         }

         if (n.startsWith("shin") || n.startsWith("leg")) {
            return RigPaint.mul(RigPaint.c(c.y < 1.8F && n.startsWith("shin") ? 0x3A322C : 0x6B5E52), 1.0F + g * 2.0F);
         }

         if (n.equals("head")) {
            return RigPaint.c(0xCDBFA8);
         }

         return RigPaint.mul(RigPaint.c(0xE0B7A5), 1.0F + g + (RigPaint.fbm(c.x * 0.3F, c.y * 0.3F, c.z * 0.3F, 8) - 0.5F) * 0.1F);
      }, 0.04F);
      eyes(r, hc, 1.6F, 1.0F, 0x14100E, 0xF2EEE6);
      Cube sn = head.cubes.get(head.cubes.size() - 1);
      for (int s = 0; s < 2; s++) {
         r.patch(sn, Rig.FRONT, s == 0 ? 1.0F : 3.0F, 1.0F, 0.9F, 0.9F, RigPaint.c(0x4A3A34));
      }

      return r;
   }

   // ------------------------------------------------------------------ chicken
   private static Rig chicken() {
      Rig r = new Rig(Mobs.CHICKEN, 2, 128);
      Bone root = r.root();
      Bone body = r.bone("body", root, 0, 8.6F, 0);
      body.round(-3.0F, -3.0F, -4.2F, 6, 6, 8.4F, 0.8F);
      Bone tail = r.bone("tail1", body, 0, 10.0F, -4.2F);
      tail.rot(-38, 0, 0);
      tail.box(-1.5F, -0.4F, -1.0F, 3, 3.6F, 1.6F);
      tail.box(-0.7F, 2.4F, -0.8F, 1.4F, 1.6F, 1.2F);
      Bone head = r.bone("head", body, 0, 11.2F, 3.4F);
      Cube hc = head.blob(-2.0F, -1.0F, -1.8F, 4, 5.4F, 3.6F, 0.6F);
      Bone beak = r.boneRel("beak", head, 0, 1.6F, 1.8F);
      beak.box(-1.0F, 0.1F, 0, 2, 0.8F, 1.7F);
      Bone jaw = r.boneRel("jaw", head, 0, 1.2F, 1.8F);
      jaw.box(-0.8F, -0.6F, 0, 1.6F, 0.7F, 1.3F);
      head.box(-0.5F, 4.3F, -1.4F, 1.0F, 1.6F, 3.0F);
      head.box(-0.4F, -0.4F, 1.6F, 0.8F, 1.9F, 0.7F);
      Cube side = head.blobSide;
      for (int sd = 0; sd < 2; sd++) {
         float sx = sd == 0 ? 1.0F : -1.0F;
         Bone lid = r.boneRel("lid" + (sd == 0 ? "L" : "R"), head, sx * (side.w / 2), side.y + side.h - 1.2F, side.z + side.d - 1.4F);
         lid.box(sd == 0 ? 0.0F : -0.2F, -1.1F, 0.0F, 0.2F, 1.1F, 1.1F).group(G_LID);
      }
      for (int s = 0; s < 2; s++) {
         float sx = s == 0 ? 1.0F : -1.0F;
         Bone wing = r.bone("wing" + (s == 0 ? "L" : "R"), body, sx * 3.1F, 11.0F, 0.2F);
         wing.rot(0, 0, sx * -6);
         wing.box(s == 0 ? -0.2F : -0.6F, -3.8F, -3.0F, 0.8F, 3.8F, 6.0F);
         Bone leg = r.bone("leg" + (s == 0 ? "L" : "R"), root, sx * 1.7F, 5.4F, 0.4F);
         leg.box(-0.45F, -5.4F, -0.45F, 0.9F, 5.4F, 0.9F);
         Bone foot = r.boneRel("foot" + (s == 0 ? "L" : "R"), leg, 0, -5.2F, 0);
         foot.box(-1.5F, -0.2F, -2.4F, 3.0F, 0.6F, 2.4F);
         foot.box(-0.5F, -0.2F, 0.0F, 1.0F, 0.6F, 1.2F);
         foot.box(-1.5F, -0.2F, -1.2F, 0.9F, 0.6F, 1.2F);
         foot.box(0.6F, -0.2F, -1.2F, 0.9F, 0.6F, 1.2F);
      }

      r.finish();
      r.paint(c -> {
         String n = c.bone.name;
         float g = RigPaint.grain(c, 13) * 0.03F;
         if (n.equals("beak") || n.equals("jaw")) {
            return RigPaint.mul(RigPaint.c(n.equals("jaw") ? 0xE89A28 : 0xF2AE38), 1.0F + g);
         }

         if (n.startsWith("leg") || n.startsWith("foot")) {
            float scales = 0.9F + 0.12F * (((int)(c.y * 1.4F)) & 1);
            return RigPaint.mul(RigPaint.c(0xE5A23C), scales + g);
         }

         if (n.equals("head") && c.cube.h < 2.0F) {
            return RigPaint.mul(RigPaint.c(0xD02A26), 0.95F + g);
         }

         if (n.equals("head") && c.cube.w < 1.1F) {
            return RigPaint.mul(RigPaint.c(0xD02A26), 0.95F + g);
         }

         if (n.startsWith("wing")) {
            float edge = RigPaint.smooth(2.0F, 0.2F, c.y - 8.0F);
            return RigPaint.mul(RigPaint.mix(0xF1EEE8, 0xC9C4BB, edge * 0.8F + (c.z < -1.0F ? 0.0F : 0.0F)), 0.96F + g + 0.05F * (((int)(c.z * 1.6F)) & 1));
         }

         float band = 0.5F + 0.5F * (float)Math.sin(c.y * 2.6F + c.z * 0.4F + RigPaint.noise(c.x * 0.6F, c.y * 0.6F, c.z * 0.6F, 3) * 3.0F);
         int base = RigPaint.mix(0xF4F2EC, 0xDDD9D0, band * 0.55F);
         if (n.startsWith("tail")) {
            base = RigPaint.mix(base, 0xBDB8AE, 0.35F);
         }

         return RigPaint.mul(base, 1.0F + g);
      }, 0.05F);
      // eyes on the sides of the head: black with a thin ring
      for (int s = 0; s < 2; s++) {
         int face = s == 0 ? Rig.RIGHT : Rig.LEFT;
         float u = s == 0 ? 0.3F : side.d - 1.4F;
         r.patch(side, face, u, 1.2F, 1.1F, 1.1F, RigPaint.c(0xE9A83A));
         r.patch(side, face, u + 0.25F, 1.45F, 0.6F, 0.6F, RigPaint.c(0x120D0B));
      }

      return r;
   }

   /**
    * Two eyes on the front face of a head box (see {@link #lids}): a white with a dark pupil on the inner half. {@code x} is the distance of
    * an eye's middle from the middle of the face, {@code fromTop} the distance of its top from the top edge of the face.
    */
   static void eyes(Rig r, Cube head, float x, float fromTop, int pupil, int white) {
      float fw = head.w;
      for (int side = 0; side < 2; side++) {
         float cx = fw / 2 + (side == 0 ? 1 : -1) * x;
         // seen from the front the mob's left (+x) is on the viewer's right
         float u = cx - EW / 2;
         r.patch(head, Rig.FRONT, u, fromTop, EW, EH, RigPaint.c(white));
         float pu = side == 0 ? u : u + EW / 2;
         r.patch(head, Rig.FRONT, pu, fromTop, EW / 2, EH, RigPaint.c(pupil));
      }
   }
}
