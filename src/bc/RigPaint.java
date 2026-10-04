package bc;

/** Update 3.17: small helpers for painting skins (colours, noise) - no OpenGL. */
final class RigPaint {
   /** A see-through texel (what a shader returns for one): an alpha below 128. (A colour with alpha 0 means "opaque" to a shader.) */
   static final int CLEAR = 0x01FFFFFF;

   private RigPaint() {
   }

   static int rgb(int r, int g, int b) {
      return 0xFF000000 | clamp(r) << 16 | clamp(g) << 8 | clamp(b);
   }

   /** 0xRRGGBB (or 0xAARRGGBB) -> opaque colour. */
   static int c(int rgb) {
      return 0xFF000000 | rgb & 0xFFFFFF;
   }

   static int clamp(int v) {
      return Math.max(0, Math.min(255, v));
   }

   /** Multiplies the colour, keeps the alpha. */
   static int mul(int argb, float k) {
      int r = clamp((int)((argb >> 16 & 255) * k));
      int g = clamp((int)((argb >> 8 & 255) * k));
      int b = clamp((int)((argb & 255) * k));
      return argb & 0xFF000000 | r << 16 | g << 8 | b;
   }

   /** a * (1 - t) + b * t. */
   static int mix(int a, int b, float t) {
      t = Math.max(0.0F, Math.min(1.0F, t));
      int r = (int)((a >> 16 & 255) * (1.0F - t) + (b >> 16 & 255) * t + 0.5F);
      int g = (int)((a >> 8 & 255) * (1.0F - t) + (b >> 8 & 255) * t + 0.5F);
      int bl = (int)((a & 255) * (1.0F - t) + (b & 255) * t + 0.5F);
      return 0xFF000000 | clamp(r) << 16 | clamp(g) << 8 | clamp(bl);
   }

   static float smooth(float a, float b, float x) {
      float t = Math.max(0.0F, Math.min(1.0F, (x - a) / (b - a)));
      return t * t * (3.0F - 2.0F * t);
   }

   static float hash(int x, int y, int z, int seed) {
      int h = x * 374761393 + y * 668265263 + z * 1274126177 + seed * 362437;
      h = (h ^ h >>> 13) * 1274126177;
      h ^= h >>> 16;
      return (h & 0xFFFF) / 65535.0F;
   }

   /** Smooth value noise in 3D, 0..1. */
   static float noise(float x, float y, float z, int seed) {
      int ix = (int)Math.floor(x);
      int iy = (int)Math.floor(y);
      int iz = (int)Math.floor(z);
      float fx = x - ix;
      float fy = y - iy;
      float fz = z - iz;
      fx = fx * fx * (3.0F - 2.0F * fx);
      fy = fy * fy * (3.0F - 2.0F * fy);
      fz = fz * fz * (3.0F - 2.0F * fz);
      float r = 0.0F;
      for (int dz = 0; dz < 2; dz++) {
         for (int dy = 0; dy < 2; dy++) {
            for (int dx = 0; dx < 2; dx++) {
               float w = (dx == 0 ? 1.0F - fx : fx) * (dy == 0 ? 1.0F - fy : fy) * (dz == 0 ? 1.0F - fz : fz);
               r += w * hash(ix + dx, iy + dy, iz + dz, seed);
            }
         }
      }

      return r;
   }

   /** Two octaves of noise, 0..1, mostly round 0.5. */
   static float fbm(float x, float y, float z, int seed) {
      return noise(x, y, z, seed) * 0.62F + noise(x * 2.1F, y * 2.1F, z * 2.1F, seed + 7) * 0.38F;
   }

   /** Per-texel grain: -1..1, the same for the same texel of the skin. */
   static float grain(Rig.Ctx c, int seed) {
      return hash(c.tx, c.ty, c.face, seed) * 2.0F - 1.0F;
   }
}
