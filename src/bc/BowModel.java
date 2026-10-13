package bc;

import org.lwjgl.opengl.GL11;

/** Articulated recurve bow. Limbs, tips, string and arrow share one draw state in both views. */
final class BowModel {
   private BowModel() {}

   static void draw(float draw, boolean nocked, float bright) {
      float d = WeaponPose.clamp(draw);
      GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_TEXTURE_BIT);
      GL11.glDisable(GL11.GL_CULL_FACE);
      GL11.glDisable(GL11.GL_ALPHA_TEST);
      GL11.glEnable(GL11.GL_TEXTURE_2D);
      GL11.glBindTexture(GL11.GL_TEXTURE_2D, SkyGen.whiteTex());
      GL11.glBegin(GL11.GL_QUADS);
      for (int side : new int[]{-1, 1}) {
         for (int i = 0; i < 18; i++) {
            float a = i / 18.0F, b = (i + 1) / 18.0F;
            float radius = 0.026F - a * 0.017F;
            tube(WeaponPose.limbX(a, d), side * a * WeaponPose.tipY(d),
               WeaponPose.limbX(b, d), side * b * WeaponPose.tipY(d), 0, radius,
               i > 15 ? 0xC2AD83 : 0x997044, bright);
         }
         // String ends are the exact limb endpoints; the middle is also the arrow's nock.
         tube3(WeaponPose.tipX(d), side * WeaponPose.tipY(d), 0, WeaponPose.nockX(d), 0,
            WeaponPose.nockZ(d), 0.003F, 0xD7D2BA, bright);
      }
      tube(0, -0.09F, 0, 0.09F, 0, 0.033F, 0x4A3529, bright);
      for (int i = 0; i < 9; i++) {
         float y = -0.081F + i * 0.02F;
         tube(0, y, 0, y + 0.005F, 0, 0.034F, 0x70513A, bright);
      }
      if (nocked) {
         float tail = WeaponPose.nockX(d);
         float head = tail + 0.86F;
         float z = WeaponPose.nockZ(d);
         tube(tail, 0, head, 0, z, 0.006F, 0xBCA37A, bright);
         tube(head - 0.018F, 0, head + 0.06F, 0, z, 0.016F, 0x9DA8AD, bright);
         // Two feather vanes; the arrow remains aligned along +X, never rotates across the string.
         for (int side : new int[]{-1, 1}) {
            tube(tail + 0.015F, side * 0.018F, tail + 0.11F, side * 0.012F,
               z, 0.009F, 0xD6D2C6, bright);
         }
      }
      GL11.glEnd();
      GL11.glPopAttrib();
   }

   /** Taper-free octagonal rod in the XY plane, with smooth wraparound material shading. */
   private static void tube(float ax, float ay, float bx, float by, float z, float r, int rgb, float bright) {
      tube3(ax, ay, z, bx, by, z, r, rgb, bright);
   }

   private static void tube3(float ax, float ay, float az, float bx, float by, float bz, float r, int rgb, float bright) {
      float dx = bx - ax, dy = by - ay, dz = bz - az;
      float len = (float)Math.sqrt(dx * dx + dy * dy + dz * dz);
      float planar = (float)Math.sqrt(dx * dx + dy * dy);
      if (len < 0.00001F || planar < 0.00001F) return;
      float nx = -dy / planar, ny = dx / planar;
      float mx = -dz * ny / len, my = dz * nx / len, mz = (dx * ny - dy * nx) / len;
      for (int i = 0; i < 8; i++) {
         double a = i * Math.PI / 4, b = (i + 1) * Math.PI / 4;
         vertex(ax, ay, az, nx, ny, mx, my, mz, r, a, rgb, bright);
         vertex(bx, by, bz, nx, ny, mx, my, mz, r, a, rgb, bright);
         vertex(bx, by, bz, nx, ny, mx, my, mz, r, b, rgb, bright);
         vertex(ax, ay, az, nx, ny, mx, my, mz, r, b, rgb, bright);
      }
   }

   private static void vertex(float x, float y, float z, float nx, float ny, float mx, float my, float mz, float r, double a, int rgb, float bright) {
      float c = (float)Math.cos(a), s = (float)Math.sin(a);
      float light = bright * (0.77F + 0.19F * s + 0.04F * c);
      GL11.glColor3f((rgb >> 16 & 255) / 255.0F * light, (rgb >> 8 & 255) / 255.0F * light, (rgb & 255) / 255.0F * light);
      GL11.glTexCoord2f(0.5F, 0.5F);
      GL11.glVertex3f(x + r * (nx * c + mx * s), y + r * (ny * c + my * s), z + r * mz * s);
   }
}
