package bc;

import bc.Mobs.Mob;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

/**
 * Update 3.17: draws the mobs that have a {@link Rig}. The skin goes up as one texture per rig; every bone becomes display lists (one per
 * group of boxes a pose can hide) that hold only texture coordinates and vertices - the colour is set per bone, so the same list is
 * brightness, hurt flash, wool dye or glow. All the maths (bones, clips) is plain Java ({@link Anim317}, {@link Rig#world}), so what the
 * game draws is what the tests and the software viewer {@code MobView} check.
 *
 * <p>{@code -Dbc.classicmobs=true} brings back the old box models of {@link MobRender} for every mob.
 */
final class RigDraw {
   static final boolean CLASSIC = Boolean.getBoolean("bc.classicmobs");
   private static final Rig[] RIGS = new Rig[64];
   private static final boolean[] TRIED = new boolean[64];
   private static final FloatBuffer MAT = BufferUtils.createFloatBuffer(16);
   private static final Anim317.MState ST = new Anim317.MState();

   private RigDraw() {
   }

   static final java.util.concurrent.ConcurrentHashMap<Integer, Rig> BUILT = new java.util.concurrent.ConcurrentHashMap<>();
   private static boolean warming;

   /** True if the rig of this type was painted in the background and has been handed out already (test hook). */
   static boolean warmedAndTaken(int type) {
      return type >= 0 && type < TRIED.length && TRIED[type];
   }

   /** Paints every rig on a background thread (once), so the first dragon on screen does not stall a frame; the upload to the GPU is still done by the render thread. */
   static synchronized void warm() {
      if (CLASSIC || warming) {
         return;
      }

      warming = true;
      Thread t = new Thread(() -> {
         for (int type = 0; type < RIGS.length; type++) {
            try {
               Rig r = Rigs317.build(type);
               if (r != null) {
                  BUILT.put(type, r);
               }
            } catch (RuntimeException e) {
               System.err.println("rig " + type + ": " + e);
            }
         }
      }, "rig-warm");
      t.setDaemon(true);
      t.setPriority(Thread.MIN_PRIORITY);
      t.start();
   }

   /** The rig of a mob type (built and uploaded on first use), or null if the mob still has its old model. */
   static Rig rig(int type) {
      if (type < 0 || type >= RIGS.length) {
         return null;
      }

      if (!TRIED[type]) {
         TRIED[type] = true;
         Rig r = CLASSIC ? null : BUILT.remove(type);
         if (r == null && !CLASSIC) {
            r = Rigs317.build(type);
         }

         if (r != null) {
            upload(r);
         }

         RIGS[type] = r;
      }

      return RIGS[type];
   }

   static void upload(Rig r) {
      ByteBuffer buf = BufferUtils.createByteBuffer(r.tw * r.th * 4);
      for (int i = 0; i < r.tw * r.th; i++) {
         int c = r.pix[i];
         buf.put((byte)(c >> 16));
         buf.put((byte)(c >> 8));
         buf.put((byte)c);
         buf.put((byte)(c >>> 24));
      }

      buf.flip();
      r.tex = GL11.glGenTextures();
      GL11.glBindTexture(3553, r.tex);
      GL11.glTexParameteri(3553, 10241, 9728);
      GL11.glTexParameteri(3553, 10240, 9728);
      GL11.glTexImage2D(3553, 0, 6408, r.tw, r.th, 0, 6408, 5121, buf);
      for (Rig.Bone b : r.bones) {
         if (b.cubes.isEmpty()) {
            continue;
         }

         b.lists = new int[Rig.GROUPS];
         for (int g = 0; g < Rig.GROUPS; g++) {
            boolean any = false;
            for (Rig.Cube c : b.cubes) {
               any |= c.group == g;
            }

            if (!any) {
               continue;
            }

            int list = GL11.glGenLists(1);
            GL11.glNewList(list, 4864);
            GL11.glBegin(7);
            for (Rig.Cube c : b.cubes) {
               if (c.group != g) {
                  continue;
               }

               float[] q = c.quads();
               for (int k = 0; k < 24; k++) {
                  int o = k * 5;
                  GL11.glTexCoord2f(q[o + 3], q[o + 4]);
                  GL11.glVertex3f(q[o], q[o + 1], q[o + 2]);
               }
            }

            GL11.glEnd();
            GL11.glEndList();
            b.lists[g] = list;
         }
      }
   }

   /** The colour a tinted group of boxes is multiplied with (0xRRGGBB); {@code color} is the mob's colour number (dye, profession ...). */
   static int tintColor(int color, int tint) {
      switch (tint) {
         case Rig.TINT_WOOL:
            return MobRender.SHEEP_RGB[Math.max(0, Math.min(15, color))];
         case Rig.TINT_ROBE:
            return Trades.ROBE[Math.max(0, Math.min(Trades.ROBE.length - 1, color))];
         case Rig.TINT_FEATHER:
            return MobModels312.PARROT_RGB[Math.max(0, Math.min(MobModels312.PARROT_RGB.length - 1, color))];
         default:
            return 0xFFFFFF;
      }
   }

   /** What a clip needs to know, read from the mob (an interpolated frame between two ticks). */
   static void fill(Anim317.MState s, Mob m, float partial) {
      s.t = m.age + partial;
      s.walk = m.pwalk + (m.walk - m.pwalk) * partial;
      s.amp = m.pwalkAmp + (m.walkAmp - m.pwalkAmp) * partial;
      float body = MobRender.lerpAngle(m.pyaw, m.yaw, partial);
      s.headYaw = MobRender.lerpAngle(m.pheadYaw, m.headYaw, partial) - body;
      s.headPitch = m.pheadPitch + (m.headPitch - m.pheadPitch) * partial;
      s.turn = Mobs.wrap(m.yaw - m.pyaw);
      s.onGround = m.onGround;
      s.inWater = m.inWater;
      s.vy = (float)m.vy;
      s.hurt = m.hurtTime > 0 ? Math.max(0.0F, Math.min(1.0F, (m.hurtTime - partial) / 10.0F)) : 0.0F;
      s.attackT = m.attackAnim > 0 ? Math.max(0.0F, m.attackAnim - partial) : 0.0F;
      s.attack = Math.min(1.0F, s.attackT / 8.0F);
      s.death = m.dead ? Math.min(1.0F, (m.deathTime + partial) / 20.0F) : 0.0F;
      s.deathT = m.dead ? m.deathTime + partial : 0.0F;
      s.target = m.hasTarget;
      s.angry = m.angry > 0;
      s.baby = m.babyAge > 0;
      s.flee = m.flee > 0;
      s.love = m.love > 0;
      s.graze = Anim.graze(m, partial);
      s.swell = m.type == Mobs.CREEPER ? Math.min(1.0F, (m.pswell + (m.swell - m.pswell) * partial) / 30.0F) : 0.0F;
      s.charged = m.charged;
      s.sheared = m.sheared;
      s.color = m.color;
      s.variant = m.variant;
      s.phase = m.phase;
      s.seed = m.seed;
      s.air = Anim.air(m);
      s.size = m.size;
      s.moving = m.moving;
      s.dead = m.dead;
      s.cd = Math.max(0.0F, m.cd - partial);
      s.fuse = m.type == Mobs.GHAST || m.type == Mobs.BLAZE ? Math.max(0.0F, m.fuse - partial) : 0.0F;
   }

   /** Writes a 3x4 matrix (model units) as a GL matrix in blocks. */
   static void load(double[] m, FloatBuffer out) {
      out.clear();
      out.put((float)m[0]).put((float)m[4]).put((float)m[8]).put(0.0F);
      out.put((float)m[1]).put((float)m[5]).put((float)m[9]).put(0.0F);
      out.put((float)m[2]).put((float)m[6]).put((float)m[10]).put(0.0F);
      out.put((float)(m[3] / 16.0)).put((float)(m[7] / 16.0)).put((float)(m[11] / 16.0)).put(1.0F);
      out.flip();
   }

   /** Draws one mob; false if it has no rig (the caller then draws the old model). */
   static boolean draw(Mob m, World w, Renderer rend, float partial, double x, double y, double z) {
      Rig r = rig(m.type);
      if (r == null) {
         return false;
      }

      float bright = rend.brightnessAt(w, x, y + 0.5, z);
      if (m.type == Mobs.END_CRYSTAL) {
         bright = 1.0F;
      }

      float cr = 1.0F;
      float cg = 1.0F;
      float cb = 1.0F;
      if (m.charged) {
         float pulse = 0.5F + 0.5F * (float)Math.sin((m.age + partial) * 0.3F);
         cr = 0.40F + 0.18F * pulse;
         cg = 0.62F + 0.22F * pulse;
         cb = 1.0F;
      }

      if (m.hurtTime > 0 || m.dead) {
         cg = 0.45F;
         cb = 0.45F;
      }

      Anim317.MState s = ST;
      fill(s, m, partial);
      Pose p = r.pose();
      p.reset();
      Anim317.pose(r, s, p);
      cr *= p.tintR;
      cg *= p.tintG;
      cb *= p.tintB;
      boolean flash = m.type == Mobs.CREEPER && m.swell > 0 && m.age / 3 % 2 == 0;
      if (flash) {
         cr = cg = cb = 0.9F;
      }

      float bodyYaw = MobRender.lerpAngle(m.pyaw, m.yaw, partial);
      boolean alphaTest = GL11.glIsEnabled(3008);
      GL11.glEnable(3008);
      GL11.glAlphaFunc(516, 0.5F);
      GL11.glBindTexture(3553, r.tex);
      GL11.glPushMatrix();
      GL11.glTranslated(x - rend.camX, y - rend.camY, z - rend.camZ);
      GL11.glRotatef(-bodyYaw, 0.0F, 1.0F, 0.0F);
      if (m.babyAge > 0) {
         GL11.glScalef(0.5F, 0.5F, 0.5F);
      }

      float modelScale = r.scale * m.scale;
      if (modelScale != 1.0F) {
         GL11.glScalef(modelScale, modelScale, modelScale);
      }

      if (flash) {
         GL11.glTexEnvi(8960, 8704, 260);
      }

      double[] root = p.root();
      double[][] wm = r.world(p);
      for (Rig.Bone b : r.bones) {
         if (b.lists == null) {
            continue;
         }

         float k = b.glow ? 1.0F : bright;
         float pr = cr * k;
         float pg = cg * k;
         float pb = cb * k;
         GL11.glPushMatrix();
         load(Rig.mul(root, wm[b.index]), MAT);
         GL11.glMultMatrixf(MAT);
         for (int g = 0; g < Rig.GROUPS; g++) {
            if (b.lists[g] != 0 && (g == 0 || (p.hide >> g & 1) == 0)) {
               int tint = r.groupTint[g];
               if (tint != 0) {
                  int rgb = tintColor(m.color, tint);
                  GL11.glColor3f(pr * (rgb >> 16 & 0xFF) / 255.0F, pg * (rgb >> 8 & 0xFF) / 255.0F, pb * (rgb & 0xFF) / 255.0F);
               } else {
                  GL11.glColor3f(pr, pg, pb);
               }

               GL11.glCallList(b.lists[g]);
            }
         }

         GL11.glPopMatrix();
      }

      if (flash) {
         GL11.glTexEnvi(8960, 8704, 8448);
      }

      GL11.glPopMatrix();
      if (!alphaTest) {
         GL11.glDisable(3008);
      }

      return true;
   }
}
