package bc;

import org.lwjgl.opengl.GL11;

/**
 * Update 3.9 - the player's body: head, torso, two arms (classic 4 px or slim 3 px), two legs, each with its second skin layer (hat, jacket,
 * sleeves, trousers), textured with a standard Minecraft 64x64 skin ({@link Skins}). Used for the third-person camera (F5), the arm in
 * the first-person view and the preview on the Skins screen.
 * <p>
 * Model space: pixels, y up, the model stands on the floor at the origin and looks towards +z (like yaw 0 in the game); its right arm is
 * at -x. One model pixel is 1/16 block, scaled by 15/16 (a player is 1.875 blocks tall). The UV layout is Minecraft's: a box w x h x d at
 * skin position (u, v) uses top (u+d, v), bottom (u+d+w, v), right (u, v+d), front (u+d, v+d), left (u+d+w, v+d), back (u+d+w+d, v+d).
 */
final class PlayerModel {
   static final float SCALE = 0.9375F / 16.0F;
   /** brightness of the six faces: top, bottom, right, front, left, back */
   static final float[] SHADE = {1.0F, 0.55F, 0.72F, 0.86F, 0.72F, 0.78F};
   static int texId;
   static int[] texFor;

   /** How the body is posed: everything in degrees except the walk phase. */
   static final class Pose {
      float bodyYaw;
      float headYaw;
      float headPitch;
      float limb;
      float amp;
      /** attack / mining swing progress 0..1, or < 0 for none */
      float swing = -1.0F;
      boolean sneak;
      /** Update 3.11: running (the upper body leans forward) / in the air (the arms go out). */
      boolean sprint;
      boolean air;
      float age;
      /** id of the item in the right hand (0 = empty hand) */
      int held;
      /** Update 3.18, all smoothed by the game from tick to tick: how much the player is sneaking / sprinting / in the air (0..1). */
      float sneakK;
      float sprintK;
      float airK;
      /** Vertical speed (blocks per tick, up is positive). */
      float vy;
      /** 0..1: swimming (moving in deep water), treading water, climbing a ladder, flying. */
      float swim;
      float tread;
      float climb;
      float fly;
      /** Progress 0..1 of eating, or < 0 for none; how far the bow is drawn (1 = full), or < 0 for none. */
      float eat = -1.0F;
      float bow = -1.0F;
      /** 1 right after a hard landing (the knees give way), fading to 0; 1 right after a hit, fading to 0. */
      float land;
      float hurt;
   }

   /** Where the boxes of the body go: the OpenGL drawer and the software viewer of the tests both are one (see {@link #build}). */
   interface Sink {
      void push();

      void pop();

      void translate(float x, float y, float z);

      void rotate(float deg, float x, float y, float z);

      void scale(float x, float y, float z);

      /** A box with the 24 texture coordinates {u0, v0, u1, v1} (skin pixels) of its six faces (top, bottom, right, front, left, back). */
      void box(float x0, float y0, float z0, float x1, float y1, float z1, float[] rects, float bright);

      /** The item in the right hand, in the frame of the arm with its origin at the shoulder. */
      void held(int id, boolean slim, float bright);
   }

   // ------------------------------------------------------------------ geometry
   /** Texture rectangles {u0, v0, u1, v1} (in skin pixels) of the six faces of a box: 0 top, 1 bottom, 2 right, 3 front, 4 left, 5 back. */
   static int[][] faceRects(int u, int v, int w, int h, int d) {
      return new int[][]{
         {u + d, v, u + d + w, v + d},
         {u + d + w, v, u + d + w + w, v + d},
         {u, v + d, u + d, v + d + h},
         {u + d, v + d, u + d + w, v + d + h},
         {u + d + w, v + d, u + d + w + d, v + d + h},
         {u + d + w + d, v + d, u + d + w + d + w, v + d + h}
      };
   }

   /** One skin box: model-space bounds (pixels) with the texture layout of the box's own size (w, h, d) at skin position (u, v). */
   static void box(float x0, float y0, float z0, float x1, float y1, float z1, int u, int v, int w, int h, int d, float bright) {
      int[][] r = faceRects(u, v, w, h, d);
      float[] q = new float[24];
      for (int f = 0; f < 6; f++) {
         for (int k = 0; k < 4; k++) {
            q[f * 4 + k] = r[f][k];
         }
      }

      box(x0, y0, z0, x1, y1, z1, q, bright);
   }

   /** One skin box with the texture rectangles of its faces given (skin pixels, 4 numbers per face: top, bottom, right, front, left, back). */
   static void box(float x0, float y0, float z0, float x1, float y1, float z1, float[] r, float bright) {
      float k = 1.0F / Skins.SIZE;
      GL11.glBegin(7);
      for (int f = 0; f < 6; f++) {
         float s = bright * SHADE[f];
         GL11.glColor3f(s, s, s);
         float u0 = r[f * 4] * k;
         float v0 = r[f * 4 + 1] * k;
         float u1 = r[f * 4 + 2] * k;
         float v1 = r[f * 4 + 3] * k;
         switch (f) {
            case 0: // top (+y): the image's bottom edge is the front
               vtx(x0, y1, z0, u0, v0);
               vtx(x1, y1, z0, u1, v0);
               vtx(x1, y1, z1, u1, v1);
               vtx(x0, y1, z1, u0, v1);
               break;
            case 1: // bottom (-y): the image's top edge is the front
               vtx(x0, y0, z1, u0, v0);
               vtx(x1, y0, z1, u1, v0);
               vtx(x1, y0, z0, u1, v1);
               vtx(x0, y0, z0, u0, v1);
               break;
            case 2: // right (-x), seen from outside: back on the left of the image, front on the right
               vtx(x0, y0, z0, u0, v1);
               vtx(x0, y0, z1, u1, v1);
               vtx(x0, y1, z1, u1, v0);
               vtx(x0, y1, z0, u0, v0);
               break;
            case 3: // front (+z): the character's right (-x) is on the left of the image
               vtx(x0, y0, z1, u0, v1);
               vtx(x1, y0, z1, u1, v1);
               vtx(x1, y1, z1, u1, v0);
               vtx(x0, y1, z1, u0, v0);
               break;
            case 4: // left (+x)
               vtx(x1, y0, z1, u0, v1);
               vtx(x1, y0, z0, u1, v1);
               vtx(x1, y1, z0, u1, v0);
               vtx(x1, y1, z1, u0, v0);
               break;
            default: // back (-z)
               vtx(x1, y0, z0, u0, v1);
               vtx(x0, y0, z0, u1, v1);
               vtx(x0, y1, z0, u1, v0);
               vtx(x1, y1, z0, u0, v0);
         }
      }

      GL11.glEnd();
   }

   static void vtx(float x, float y, float z, float u, float v) {
      GL11.glTexCoord2f(u, v);
      GL11.glVertex3f(x, y, z);
   }

   /** A base box and its second layer, which is the same box inflated by a fraction of a pixel (the layer is transparent in most skins). */
   static void layered(float x0, float y0, float z0, float w, float h, float d, int u, int v, int u2, int v2, float inflate, float bright) {
      box(x0, y0, z0, x0 + w, y0 + h, z0 + d, u, v, (int)Math.round(w), (int)Math.round(h), (int)Math.round(d), bright);
      box(x0 - inflate, y0 - inflate, z0 - inflate, x0 + w + inflate, y0 + h + inflate, z0 + d + inflate, u2, v2, (int)Math.round(w), (int)Math.round(h), (int)Math.round(d), bright);
   }

   static int texture(int[] px) {
      if (texFor != px) {
         if (texId != 0) {
            GL11.glDeleteTextures(texId);
         }

         texId = Tex.upload(Skins.SIZE, Skins.SIZE, px, false, false);
         texFor = px;
      }

      return texId;
   }

   // ------------------------------------------------------------------ drawing
   /**
    * Draws the whole body at the current origin (feet), facing +z. {@code bright} multiplies the colours (the world's light at the player).
    */
   static void draw(int[] px, boolean slim, Pose p, float bright) {
      GL11.glEnable(3553);
      GL11.glBindTexture(3553, texture(px));
      GL11.glTexEnvi(8960, 8704, 8448);
      GL11.glEnable(3008);
      GL11.glAlphaFunc(516, 0.1F);
      GL11.glDisable(3042);
      build(PlayerAnim.solve(p), p, slim, bright, new GlSink(px));
      GL11.glDisable(3008);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
   }

   /** The OpenGL end of {@link Sink}. */
   private static final class GlSink implements Sink {
      private final int[] px;

      GlSink(int[] px) {
         this.px = px;
      }

      @Override
      public void push() {
         GL11.glPushMatrix();
      }

      @Override
      public void pop() {
         GL11.glPopMatrix();
      }

      @Override
      public void translate(float x, float y, float z) {
         GL11.glTranslatef(x, y, z);
      }

      @Override
      public void rotate(float deg, float x, float y, float z) {
         if (deg != 0.0F) {
            GL11.glRotatef(deg, x, y, z);
         }
      }

      @Override
      public void scale(float x, float y, float z) {
         GL11.glScalef(x, y, z);
      }

      @Override
      public void box(float x0, float y0, float z0, float x1, float y1, float z1, float[] rects, float bright) {
         PlayerModel.box(x0, y0, z0, x1, y1, z1, rects, bright);
      }

      @Override
      public void held(int id, boolean slim, float bright) {
         drawHeld(id, slim, bright);
         GL11.glBindTexture(3553, texture(this.px)); // the item drew with the block atlas
         GL11.glEnable(3008);
         GL11.glAlphaFunc(516, 0.1F);
      }
   }

   /**
    * The body, joint by joint (Update 3.18): the hips and knees of the legs, the shoulders and elbows of the arms (each limb is two boxes that
    * share the texture of the one long box of a Minecraft skin, and a cap in the colour of the joint closes the gap that opens on the outside
    * of a bend), the torso leaning and twisting about the hips, the head looking where the player looks.
    */
   static void build(PlayerAnim.Joints j, Pose p, boolean slim, float bright, Sink s) {
      int aw = slim ? 3 : 4;
      s.push();
      s.rotate(-p.bodyYaw, 0.0F, 1.0F, 0.0F);
      s.scale(SCALE, SCALE, SCALE);
      s.translate(0.0F, j.dy, j.dz);
      s.translate(0.0F, 12.0F, 0.0F);
      s.rotate(j.pitch, 1.0F, 0.0F, 0.0F);
      s.rotate(j.roll, 0.0F, 0.0F, 1.0F);
      s.translate(0.0F, -12.0F, 0.0F);
      // legs: the pivot of the hips, the knee 6 pixels below
      for (int side = 0; side < 2; side++) {
         s.push();
         s.translate(side == 0 ? -1.9F : 1.9F, 12.0F, j.legZ);
         s.rotate(-j.hip[side], 1.0F, 0.0F, 0.0F);
         s.rotate((side == 0 ? -1.0F : 1.0F) * j.hipAbd[side], 0.0F, 0.0F, 1.0F);
         if (side == 0) {
            limb(s, -2.0F, -2.0F, 4, 4, 0, 16, 0, 32, bright, j.knee[side], null);
         } else {
            limb(s, -2.0F, -2.0F, 4, 4, 16, 48, 0, 48, bright, j.knee[side], null);
         }

         s.pop();
      }

      // torso: leans, twists and rolls about the hips
      s.push();
      s.translate(0.0F, 12.0F, 0.0F);
      s.rotate(j.tPitch, 1.0F, 0.0F, 0.0F); // positive about +x tips the upper body forward
      s.rotate(j.tYaw, 0.0F, 1.0F, 0.0F);
      s.rotate(j.tRoll, 0.0F, 0.0F, 1.0F);
      s.translate(0.0F, -12.0F, 0.0F);
      layered(s, -4.0F, 12.0F, -2.0F, 8, 12, 4, 16, 16, 16, 32, 0.25F, bright);
      s.translate(0.0F, j.chest, 0.0F);
      // arms: pivot at the shoulders (the body's tilt carries them), the elbow 6 pixels below
      for (int side = 0; side < 2; side++) {
         s.push();
         s.translate(side == 0 ? -5.0F : 5.0F, 22.0F, 0.0F);
         s.rotate(-j.sh[side], 1.0F, 0.0F, 0.0F); // a forward swing is a negative angle about +x for a hanging limb
         s.rotate((side == 0 ? -1.0F : 1.0F) * j.abd[side], 0.0F, 0.0F, 1.0F);
         if (side == 0) {
            final int held = p.held;
            limb(s, slim ? -2.0F : -3.0F, -2.0F, aw, 4, 40, 16, 40, 32, bright, -j.el[side], held > 0 ? () -> {
               s.translate(0.0F, 6.0F, 0.0F); // the item is placed relative to the shoulder
               s.held(held, slim, bright);
            } : null);
         } else {
            limb(s, -1.0F, -2.0F, aw, 4, 32, 48, 48, 48, bright, -j.el[side], null);
         }

         s.pop();
      }

      // head: carried by the torso, but it looks where the player looks, so the lean of the body is taken out of the pitch
      s.push();
      s.translate(0.0F, 24.0F, 0.0F);
      s.rotate(-j.tYaw, 0.0F, 1.0F, 0.0F);
      s.rotate(-p.headYaw, 0.0F, 1.0F, 0.0F);
      float local = Math.max(-80.0F, Math.min(80.0F, p.headPitch - j.tPitch - j.pitch)) + j.headPitch;
      s.rotate(local, 1.0F, 0.0F, 0.0F);
      s.rotate(j.headRoll, 0.0F, 0.0F, 1.0F);
      layered(s, -4.0F, 0.0F, -4.0F, 8, 8, 8, 0, 0, 32, 0, 0.5F, bright);
      s.pop();
      s.pop();
      s.pop();
   }

   /** A base box and its second layer (the same box a fraction of a pixel bigger; transparent in most skins), through a {@link Sink}. */
   static void layered(Sink s, float x0, float y0, float z0, float w, float h, float d, int u, int v, int u2, int v2, float inflate, float bright) {
      int iw = Math.round(w);
      int ih = Math.round(h);
      int id = Math.round(d);
      s.box(x0, y0, z0, x0 + w, y0 + h, z0 + d, rects(u, v, iw, ih, id), bright);
      s.box(x0 - inflate, y0 - inflate, z0 - inflate, x0 + w + inflate, y0 + h + inflate, z0 + d + inflate, rects(u2, v2, iw, ih, id), bright);
   }

   static float[] rects(int u, int v, int w, int h, int d) {
      int[][] r = faceRects(u, v, w, h, d);
      float[] q = new float[24];
      for (int f = 0; f < 6; f++) {
         for (int k = 0; k < 4; k++) {
            q[f * 4 + k] = r[f][k];
         }
      }

      return q;
   }

   /** Texture rectangles of the part of a limb box between {@code from} and {@code to} pixels below its top (the box is 12 tall in the skin). */
   static float[] partRects(int u, int v, int w, int d, int from, int to) {
      float[] q = rects(u, v, w, 12, d);
      float vs = v + d + from;
      float ve = v + d + to;
      if (from != 0) {
         q[1] = q[3] = vs + 0.5F; // the top of the lower part is inside: a row of the skin
      }

      if (to != 12) {
         q[5] = q[7] = ve - 0.5F; // the bottom of the upper part likewise
      }

      for (int f = 2; f < 6; f++) {
         q[f * 4 + 1] = vs;
         q[f * 4 + 3] = ve;
      }

      return q;
   }

   /** The rectangles of the cap of a joint: the row of the skin at the joint on every face. */
   static float[] capRects(int u, int v, int w, int d, int row) {
      float[] q = rects(u, v, w, 12, d);
      float vr = v + d + row - 0.5F;
      float fu0 = q[12]; // the horizontal range of the front face
      float fu1 = q[14];
      for (int f = 0; f < 6; f++) {
         q[f * 4 + 1] = vr;
         q[f * 4 + 3] = vr;
         if (f < 2) {
            q[f * 4] = fu0;
            q[f * 4 + 2] = fu1;
         }
      }

      return q;
   }

   /**
    * A leg or an arm hanging from its joint at the origin: the upper half, a cap at the joint turned by half the bend, the lower half turned by
    * {@code bendDeg} about +x. {@code inLower} draws what is held in the lower half's frame. (x0, z0) is the corner of the box, w x 4 its
    * section; (u, v) the base layer in the skin, (u2, v2) the second layer.
    */
   static void limb(Sink s, float x0, float z0, int w, int d, int u, int v, int u2, int v2, float bright, float bendDeg, Runnable inLower) {
      float e = 0.25F;
      // the upper half (the second layer is a quarter of a pixel bigger, except where the halves meet)
      s.box(x0, -6.0F, z0, x0 + w, 0.0F, z0 + d, partRects(u, v, w, d, 0, 6), bright);
      s.box(x0 - e, -6.0F, z0 - e, x0 + w + e, e, z0 + d + e, partRects(u2, v2, w, d, 0, 6), bright);
      s.translate(0.0F, -6.0F, 0.0F);
      float bend = Math.abs(bendDeg);
      if (bend > 7.0F) {
         // the cap: a box at the joint, as high as the gap on the outside of the bend is, in the colour of the skin at the joint
         float h = Math.max(1.4F, Math.min(4.6F, d * (float)Math.tan(Math.toRadians(bend * 0.5)) * 0.95F + 0.9F));
         s.push();
         s.rotate(bendDeg * 0.5F, 1.0F, 0.0F, 0.0F);
         float c = 0.03F;
         s.box(x0 - c, -h * 0.5F, z0 - c, x0 + w + c, h * 0.5F, z0 + d + c, capRects(u, v, w, d, 6), bright);
         s.box(x0 - e - c, -h * 0.5F - e, z0 - e - c, x0 + w + e + c, h * 0.5F + e, z0 + d + e + c, capRects(u2, v2, w, d, 6), bright);
         s.pop();
      }

      s.rotate(bendDeg, 1.0F, 0.0F, 0.0F);
      s.box(x0, -6.0F, z0, x0 + w, 0.0F, z0 + d, partRects(u, v, w, d, 6, 12), bright);
      s.box(x0 - e, -6.0F - e, z0 - e, x0 + w + e, 0.0F, z0 + d + e, partRects(u2, v2, w, d, 6, 12), bright);
      if (inLower != null) {
         inLower.run();
      }
   }

   /**
    * The item in the right hand, in the frame of the arm (origin = shoulder), in model pixels - Minecraft 1.12's LayerHeldItem and the
    * "thirdperson_righthand" transforms of the item models: the arm's frame turned the way MC's y-down model space is, then
    * rotate(-90, x) rotate(180, y) translate(1/16, 0.125, -0.625) and the model's own display transform
    * (flat items: translation [0, 3, 1] scale 0.55; tools and sticks: rotation [0, -90, 55] translation [0, 4, 0.5] scale 0.85;
    * blocks: rotation [75, 45, 0] translation [0, 2.5, 0] scale 0.375; the bow has its own).
    */
   static void drawHeld(int id, boolean slim, float bright) {
      GL11.glPushMatrix();
      GL11.glRotatef(180.0F, 1.0F, 0.0F, 0.0F); // our model hangs along -y and faces +z, Minecraft's raw model space is y down
      if (slim) {
         GL11.glTranslatef(0.5F, 0.0F, 0.0F); // (ModelPlayer.postRenderArm moves the pivot half a pixel for slim arms)
      }

      GL11.glRotatef(-90.0F, 1.0F, 0.0F, 0.0F);
      GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
      GL11.glTranslatef(1.0F, 2.0F, -10.0F); // 1/16, 0.125, -0.625 blocks
      if (id >= 256 || Renderer.flatBlock(id)) {
         boolean handheld = id >= 256 && (Items.isTool(id) || id == 256);
         if (id == 268) { // the bow
            GL11.glTranslatef(-1.0F, -2.0F, 2.5F);
            GL11.glRotatef(-80.0F, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(260.0F, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(-40.0F, 0.0F, 0.0F, 1.0F);
            GL11.glScalef(0.9F * 16.0F, 0.9F * 16.0F, 0.9F * 16.0F);
         } else if (handheld) {
            GL11.glTranslatef(0.0F, 4.0F, 0.5F);
            GL11.glRotatef(-90.0F, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(55.0F, 0.0F, 0.0F, 1.0F);
            GL11.glScalef(0.85F * 16.0F, 0.85F * 16.0F, 0.85F * 16.0F);
         } else {
            GL11.glTranslatef(0.0F, 3.0F, 1.0F);
            GL11.glScalef(0.55F * 16.0F, 0.55F * 16.0F, 0.55F * 16.0F);
         }

         if (id >= 256) {
            Renderer.drawItemModel(id, bright);
         } else {
            Renderer.drawFlatBlock(id, bright);
         }
      } else {
         GL11.glTranslatef(0.0F, 2.5F, 0.0F);
         GL11.glRotatef(75.0F, 1.0F, 0.0F, 0.0F);
         GL11.glRotatef(45.0F, 0.0F, 1.0F, 0.0F);
         GL11.glScalef(0.375F * 16.0F, 0.375F * 16.0F, 0.375F * 16.0F);
         Renderer.drawBlockModel(id, bright);
      }

      GL11.glPopMatrix();
   }

   /**
    * The right arm in the first-person view when the hand is empty: ItemRenderer.renderArmFirstPerson with Minecraft 1.12's numbers
    * (the arm is drawn in MC's raw model space, which is y down, so the model is turned by 180 degrees about x first).
    *
    * @param swing    0..1 progress of the swing
    * @param equipInv 1 - the equip progress (0 = the hand is fully raised)
    */
   static void drawArmFirstPerson(int[] px, boolean slim, float bright, float swing, float equipInv) {
      float f1 = (float)Math.sqrt(swing);
      GL11.glTranslatef(-0.3F * (float)Math.sin(f1 * Math.PI) + 0.64000005F, 0.4F * (float)Math.sin(f1 * Math.PI * 2.0) - 0.6F + equipInv * -0.6F, -0.4F * (float)Math.sin(swing * Math.PI) - 0.71999997F);
      GL11.glRotatef(45.0F, 0.0F, 1.0F, 0.0F);
      GL11.glRotatef((float)Math.sin(f1 * Math.PI) * 70.0F, 0.0F, 1.0F, 0.0F);
      GL11.glRotatef((float)Math.sin(swing * swing * Math.PI) * -20.0F, 0.0F, 0.0F, 1.0F);
      GL11.glTranslatef(-1.0F, 3.6F, 3.5F);
      GL11.glRotatef(120.0F, 0.0F, 0.0F, 1.0F);
      GL11.glRotatef(200.0F, 1.0F, 0.0F, 0.0F);
      GL11.glRotatef(-135.0F, 0.0F, 1.0F, 0.0F);
      GL11.glTranslatef(5.6F, 0.0F, 0.0F);
      GL11.glTranslatef(-5.0F / 16.0F, (slim ? 2.5F : 2.0F) / 16.0F, 0.0F); // the arm's pivot (the shoulder joint)
      GL11.glRotatef(5.73F, 0.0F, 0.0F, 1.0F); // its idle lean: 0.1 radian
      GL11.glRotatef(180.0F, 1.0F, 0.0F, 0.0F);
      drawArm(px, slim, bright);
   }

   /** The right arm alone for the first-person view (empty hand): origin at the shoulder, hanging down along -y. */
   static void drawArm(int[] px, boolean slim, float bright) {
      GL11.glEnable(3553);
      GL11.glBindTexture(3553, texture(px));
      GL11.glTexEnvi(8960, 8704, 8448);
      GL11.glEnable(3008);
      GL11.glAlphaFunc(516, 0.1F);
      GL11.glPushMatrix();
      GL11.glScalef(SCALE, SCALE, SCALE);
      layered(slim ? -2.0F : -3.0F, -10.0F, -2.0F, slim ? 3 : 4, 12, 4, 40, 16, 40, 32, 0.25F, bright);
      GL11.glPopMatrix();
      GL11.glDisable(3008);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
   }

   /** The model inside a menu (the GUI's 2D projection): feet at (cx, cy), {@code scale} screen units per block, turned by yaw degrees. */
   static void drawPreview(int[] px, boolean slim, float cx, float cy, float scale, float yaw, float time) {
      Pose p = new Pose();
      p.bodyYaw = 0.0F;
      p.age = time * 20.0F;
      p.amp = 0.0F;
      drawIn2D(px, slim, cx, cy, scale, yaw, p);
   }

   /**
    * The model in the survival inventory: it faces you and turns its head and body towards the mouse like the player in Minecraft's
    * inventory. (mx, my) is the mouse in GUI units, the feet are at (cx, cy).
    */
   static void drawLooking(int[] px, boolean slim, float cx, float cy, float scale, float mx, float my, float time) {
      float eyeY = cy - 1.62F * scale;
      float ax = (float)Math.atan((mx - cx) / 40.0F); // > 0: the mouse is to the right of the player
      float ay = (float)Math.atan((eyeY - my) / 40.0F); // > 0: the mouse is above the eyes
      Pose p = new Pose();
      p.age = time * 20.0F;
      p.headYaw = -ax * 20.0F; // the head turns 20 degrees further than the body (positive headYaw turns towards -x = the viewer's left)
      p.headPitch = -ay * 20.0F;
      drawIn2D(px, slim, cx, cy, scale, ax * 20.0F, p);
   }

   private static void drawIn2D(int[] px, boolean slim, float cx, float cy, float scale, float yaw, Pose p) {
      GL11.glClear(256);
      GL11.glEnable(2929);
      GL11.glPushMatrix();
      GL11.glTranslatef(cx, cy, 0.0F);
      GL11.glScalef(scale, -scale, scale);
      GL11.glRotatef(yaw, 0.0F, 1.0F, 0.0F);
      draw(px, slim, p, 1.0F);
      GL11.glPopMatrix();
      GL11.glDisable(2929);
      GL11.glDisable(3553);
      GL11.glEnable(3042);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
   }
}
