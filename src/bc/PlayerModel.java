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
      float age;
      /** id of the item in the right hand (0 = empty hand) */
      int held;
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
      float k = 1.0F / Skins.SIZE;
      GL11.glBegin(7);
      for (int f = 0; f < 6; f++) {
         float s = bright * SHADE[f];
         GL11.glColor3f(s, s, s);
         float u0 = r[f][0] * k;
         float v0 = r[f][1] * k;
         float u1 = r[f][2] * k;
         float v1 = r[f][3] * k;
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
      GL11.glPushMatrix();
      GL11.glRotatef(-p.bodyYaw, 0.0F, 1.0F, 0.0F);
      GL11.glScalef(SCALE, SCALE, SCALE);
      float swingAmt = p.amp * 55.0F;
      float legA = (float)Math.sin(p.limb * 1.6F) * swingAmt;
      float armA = -legA * 0.9F;
      float sneakDrop = p.sneak ? 1.6F : 0.0F;
      // idle sway of the arms (and a little breathing)
      float idle = (float)Math.sin(p.age * 0.067F) * 2.5F + 2.5F;
      GL11.glTranslatef(0.0F, -sneakDrop, 0.0F);
      // legs: pivot at the hips
      for (int side = 0; side < 2; side++) {
         GL11.glPushMatrix();
         float sgn = side == 0 ? 1.0F : -1.0F;
         GL11.glTranslatef(side == 0 ? -1.9F : 1.9F, 12.0F, p.sneak ? -0.2F : 0.0F);
         GL11.glRotatef(legA * sgn, 1.0F, 0.0F, 0.0F);
         if (p.sneak) {
            GL11.glRotatef(-6.0F, 1.0F, 0.0F, 0.0F);
         }

         if (side == 0) {
            layered(-2.0F, -12.0F, -2.0F, 4, 12, 4, 0, 16, 0, 32, 0.25F, bright);
         } else {
            layered(-2.0F, -12.0F, -2.0F, 4, 12, 4, 16, 48, 0, 48, 0.25F, bright);
         }

         GL11.glPopMatrix();
      }

      // torso (tilts forward when sneaking)
      GL11.glPushMatrix();
      GL11.glTranslatef(0.0F, 12.0F, 0.0F);
      if (p.sneak) {
         GL11.glRotatef(26.0F, 1.0F, 0.0F, 0.0F); // positive about +x tips the upper body forward
      }

      GL11.glTranslatef(0.0F, -12.0F, 0.0F);
      layered(-4.0F, 12.0F, -2.0F, 8, 12, 4, 16, 16, 16, 32, 0.25F, bright);
      // arms: pivot at the shoulders (the body's tilt carries them)
      int aw = slim ? 3 : 4;
      for (int side = 0; side < 2; side++) {
         GL11.glPushMatrix();
         float sgn = side == 0 ? 1.0F : -1.0F;
         GL11.glTranslatef(side == 0 ? -5.0F : 5.0F, 22.0F, 0.0F);
         // angles about +x: positive swings a hanging limb BACKWARDS; an arm moves opposite to the leg on its side
         float back = -0.9F * legA * sgn + (side == 0 ? idle : -idle);
         if (side == 0 && p.swing >= 0.0F) {
            float s = (float)Math.sin(Math.sqrt(p.swing) * Math.PI);
            back -= 95.0F * s + 20.0F * (float)Math.sin(p.swing * Math.PI);
            GL11.glRotatef(-s * 12.0F, 0.0F, 1.0F, 0.0F);
         }

         if (p.sneak) {
            back -= 22.0F;
         }

         if (side == 0 && p.held > 0) {
            back -= 18.0F; // the arm that holds something is lifted a little, like in Minecraft
         }

         GL11.glRotatef(back, 1.0F, 0.0F, 0.0F);
         GL11.glRotatef(side == 0 ? -3.0F : 3.0F, 0.0F, 0.0F, 1.0F); // hang a little outwards
         if (side == 0) {
            layered(slim ? -2.0F : -3.0F, -10.0F, -2.0F, aw, 12, 4, 40, 16, 40, 32, 0.25F, bright);
            if (p.held > 0) {
               drawHeld(p.held, slim, bright);
               GL11.glBindTexture(3553, texture(px)); // the item drew with the block atlas
               GL11.glEnable(3008);
               GL11.glAlphaFunc(516, 0.1F);
            }
         } else {
            layered(-1.0F, -10.0F, -2.0F, aw, 12, 4, 32, 48, 48, 48, 0.25F, bright);
         }

         GL11.glPopMatrix();
      }

      // head: carried by the torso, but it looks where the player looks, so the torso's tilt is taken out of the pitch
      GL11.glPushMatrix();
      GL11.glTranslatef(0.0F, 24.0F, 0.0F);
      GL11.glRotatef(-p.headYaw, 0.0F, 1.0F, 0.0F);
      GL11.glRotatef(p.headPitch - (p.sneak ? 26.0F : 0.0F), 1.0F, 0.0F, 0.0F);
      layered(-4.0F, 0.0F, -4.0F, 8, 8, 8, 0, 0, 32, 0, 0.5F, bright);
      GL11.glPopMatrix();
      GL11.glPopMatrix();
      GL11.glPopMatrix();
      GL11.glDisable(3008);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
   }

   /**
    * The item in the right hand, in the frame of the arm (origin = shoulder, the arm hangs along -y, its front is +z), in model pixels.
    * Flat items (tools, food, ...) are held with the handle in the fist and the tip pointing forward and up, like in Minecraft;
    * blocks are a small cube in front of the fist.
    */
   static void drawHeld(int id, boolean slim, float bright) {
      GL11.glPushMatrix();
      GL11.glTranslatef(slim ? -0.5F : -1.0F, -9.0F, 0.0F);
      if (id >= 256) {
         boolean tool = Items.isTool(id) || id == 268;
         float size = tool ? 13.0F : 10.0F;
         GL11.glRotatef(-90.0F, 0.0F, 1.0F, 0.0F); // the sprite plane along the body's side, facing outwards, its right edge forward
         GL11.glScalef(size, size, size);
         GL11.glTranslatef(0.28F, 0.28F, 0.0F);      // the handle (bottom left of the sprite) sits in the fist
         Renderer.drawItemModel(id, bright);
      } else {
         float size = Blocks.kind[id] == 0 ? 7.0F : 9.0F;
         GL11.glTranslatef(0.0F, 1.0F, 4.0F);
         GL11.glRotatef(45.0F, 0.0F, 1.0F, 0.0F);
         GL11.glScalef(size, size, size);
         Renderer.drawBlockModel(id, Math.max(bright, id == 32 ? 0.9F : 0.0F));
      }

      GL11.glPopMatrix();
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
