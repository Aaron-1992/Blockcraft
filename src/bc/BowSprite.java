package bc;

import org.lwjgl.opengl.GL11;

/** Flat 32px bow sprites: idle, three draw stages and the same stages on release.
 * No cylinders, limb meshes, separately floating arrow or spatial texture filtering.
 */
final class BowSprite {
   static final int SIZE = 32;
   private static final int[] textures = new int[8];
   private BowSprite() {}

   static int stage(float draw) {
      if (draw <= 0) return 0;
      return draw < 0.35F ? 1 : draw < 0.75F ? 2 : 3;
   }
   static int tipX(int stage) { return 19 - stage; }
   static int tipY(int stage) { return 2 + stage; }
   static int nockX(int stage) { return 19 + stage * 3; }

   static int[] pixels(int stage, boolean arrow) {
      stage = Math.max(0, Math.min(3, stage));
      int[] p = new int[SIZE * SIZE];
      int tx = tipX(stage), ty = tipY(stage), nx = nockX(stage);
      // Continuous pixel lines always meet the limb tips and the arrow tail.
      line(p, tx, ty, nx, 16, 0xFFC6C1AA);
      line(p, nx, 16, tx, 31 - ty, 0xFFC6C1AA);
      int[][] top = {{8, 16}, {8, 12}, {10, 8}, {13, 5}, {tx, ty}};
      for (int side = 0; side < 2; side++) {
         for (int i = 0; i < top.length - 1; i++) {
            int ax = top[i][0], ay = side == 0 ? top[i][1] : 31 - top[i][1];
            int bx = top[i + 1][0], by = side == 0 ? top[i + 1][1] : 31 - top[i + 1][1];
            line(p, ax - 1, ay, bx - 1, by, 0xFF624527);
            line(p, ax, ay, bx, by, 0xFFAF8750);
            line(p, ax + 1, ay, bx + 1, by, 0xFF896135);
         }
      }
      for (int y = 13; y <= 18; y++) for (int x = 7; x <= 9; x++) {
         set(p, x, y, (y & 1) == 0 ? 0xFF6C4931 : 0xFF4D3627);
      }
      set(p, tx, ty, 0xFFD4C3A0);
      set(p, tx, 31 - ty, 0xFFD4C3A0);
      if (arrow) {
         line(p, 3, 16, nx, 16, 0xFFAA8753);
         line(p, 3, 15, nx - 2, 15, 0xFFD0B17C);
         line(p, nx - 4, 13, nx - 1, 16, 0xFFE1DECB);
         line(p, nx - 4, 18, nx - 1, 16, 0xFFAAA99A);
         line(p, 1, 16, 4, 13, 0xFFB9C2C0);
         line(p, 1, 16, 4, 18, 0xFF707F7D);
         set(p, nx, 16, 0xFFD0B17C);
      }
      return p;
   }

   static void set(int[] p, int x, int y, int c) {
      if (x >= 0 && x < SIZE && y >= 0 && y < SIZE) p[y * SIZE + x] = c;
   }
   static void line(int[] p, int x, int y, int bx, int by, int c) {
      int dx = Math.abs(bx - x), dy = -Math.abs(by - y), sx = x < bx ? 1 : -1, sy = y < by ? 1 : -1;
      int e = dx + dy;
      while (true) {
         set(p, x, y, c);
         if (x == bx && y == by) return;
         int twice = e * 2;
         if (twice >= dy) { e += dy; x += sx; }
         if (twice <= dx) { e += dx; y += sy; }
      }
   }

   static void draw(float draw, boolean nocked, float bright) {
      int s = stage(draw), index = s + (nocked ? 4 : 0);
      GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_TEXTURE_BIT | GL11.GL_COLOR_BUFFER_BIT);
      GL11.glDisable(GL11.GL_CULL_FACE);
      GL11.glDisable(GL11.GL_BLEND);
      GL11.glEnable(GL11.GL_ALPHA_TEST);
      GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
      GL11.glEnable(GL11.GL_TEXTURE_2D);
      if (textures[index] == 0) textures[index] = Tex.upload(SIZE, SIZE, pixels(s, nocked), false, false);
      GL11.glBindTexture(GL11.GL_TEXTURE_2D, textures[index]);
      GL11.glColor4f(bright, bright, bright, 1);
      GL11.glBegin(GL11.GL_QUADS);
      // Grip at (8,16) is the pivot in every frame, so drawing never moves it out of the hand.
      GL11.glTexCoord2f(0, 0); GL11.glVertex3f(-0.25F, 0.5F, 0);
      GL11.glTexCoord2f(0, 1); GL11.glVertex3f(-0.25F, -0.5F, 0);
      GL11.glTexCoord2f(1, 1); GL11.glVertex3f(0.75F, -0.5F, 0);
      GL11.glTexCoord2f(1, 0); GL11.glVertex3f(0.75F, 0.5F, 0);
      GL11.glEnd();
      GL11.glPopAttrib();
   }
}
