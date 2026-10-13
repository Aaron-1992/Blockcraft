package bc;

/** 64px default material atlas. The 16px atlas remains the mask/LOD/pack interchange format.
 * Detail is generated inside each tile, never across atlas neighbours. No external assets or GL needed.
 */
final class TexSmooth {
   static final int SIZE = 64;
   private TexSmooth() {}

   static boolean enabled() {
      return ResourcePacks.ID_DEFAULT.equals(ResourcePacks.activeId());
   }

   /** Periodic value noise, with continuous first derivatives at lattice and tile boundaries. */
   static float noise(float x, float y, int period, int seed) {
      int ix = (int)Math.floor(x), iy = (int)Math.floor(y);
      float fx = x - ix, fy = y - iy;
      fx = fx * fx * (3 - 2 * fx);
      fy = fy * fy * (3 - 2 * fy);
      float a = hash(Math.floorMod(ix, period), Math.floorMod(iy, period), seed);
      float b = hash(Math.floorMod(ix + 1, period), Math.floorMod(iy, period), seed);
      float c = hash(Math.floorMod(ix, period), Math.floorMod(iy + 1, period), seed);
      float d = hash(Math.floorMod(ix + 1, period), Math.floorMod(iy + 1, period), seed);
      return (a + (b - a) * fx) * (1 - fy) + (c + (d - c) * fx) * fy;
   }

   private static float hash(int x, int y, int seed) {
      int h = x * 374761393 + y * 668265263 + seed * 144269;
      h = (h ^ (h >>> 13)) * 1274126177;
      return ((h ^ (h >>> 16)) & 65535) / 65535.0F;
   }

   private static float field(float u, float v, int seed) {
      return 0.57F * noise(u * 4, v * 4, 4, seed)
         + 0.29F * noise(u * 12, v * 12, 12, seed + 1)
         + 0.14F * noise(u * 32, v * 32, 32, seed + 2);
   }

   private static int channel(float f) { return Math.max(0, Math.min(255, Math.round(f))); }

   private static int color(int a, float r, float g, float b) {
      return a << 24 | channel(r) << 16 | channel(g) << 8 | channel(b);
   }

   private static boolean natural(int tile) {
      return tile == Tex.GRASS_TOP || tile == Tex.DIRT || tile == Tex.STONE || tile == Tex.SAND
         || tile == Tex.SNOW || tile == TexArt313.RED_SAND || tile == TexArt313.GRANITE
         || tile == TexArt313.DIORITE || tile == TexArt313.ANDESITE || tile == Color312.T_CLAY;
   }

   private static int earth(int tile, float u, float v) {
      int base;
      float range = 35, grain = 3;
      switch (tile) {
         case Tex.GRASS_TOP: base = 0xA0A0A0; range = 42; break; // biome tint is applied by the mesher
         case Tex.DIRT: base = 0x816248; range = 44; grain = 5; break;
         case Tex.SAND: base = 0xD9CC9F; range = 18; grain = 2; break;
         case Tex.SNOW: base = 0xE6EEF2; range = 12; grain = 1; break;
         case TexArt313.RED_SAND: base = 0xB96E43; range = 23; grain = 2; break;
         case TexArt313.GRANITE: base = 0x9A776A; range = 35; grain = 5; break;
         case TexArt313.DIORITE: base = 0xB7B9B6; range = 40; grain = 5; break;
         case TexArt313.ANDESITE: base = 0x858884; range = 29; break;
         case Color312.T_CLAY: base = 0x9FA9AF; range = 20; grain = 1; break;
         default: base = 0x828580;
      }
      float f = field(u, v, tile + 8100);
      float detail = (noise(u * 64, v * 64, 64, tile + 980) - 0.5F) * grain;
      float shade = (f - 0.5F) * range + detail;
      // Subtle mineral strata, not the old thresholded black cracks and bevels.
      if (tile == Tex.STONE || tile == TexArt313.ANDESITE) {
         shade += 3 * (float)Math.sin(v * Math.PI * 8 + noise(u * 4, v * 4, 4, 120) * 3);
      }
      if (tile == Tex.GRASS_TOP) {
         shade += (noise(u * 48, v * 12, 12, 903) - 0.5F) * 16;
      }
      return color(255, (base >> 16 & 255) + shade, (base >> 8 & 255) + shade, (base & 255) + shade);
   }

   private static float smooth(float lo, float hi, float v) {
      float t = Math.max(0, Math.min(1, (v - lo) / (hi - lo)));
      return t * t * (3 - 2 * t);
   }

   private static int shade(int base, float value) {
      return color(255, (base >> 16 & 255) + value, (base >> 8 & 255) + value, (base & 255) + value);
   }

   /** Jittered, periodic stone cells: broad faces with narrow mortar, not embossed source pixels. */
   private static int rubble(int tile, float u, float v) {
      int cells = tile == Tex.GRAVEL ? 9 : 4;
      float x = u * cells, y = v * cells;
      int ix = (int)x, iy = (int)y;
      float first = 100, second = 100, tone = 0;
      for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) {
         int cx = ix + dx, cy = iy + dy;
         int wx = Math.floorMod(cx, cells), wy = Math.floorMod(cy, cells);
         float px = cx + 0.18F + 0.64F * hash(wx, wy, 222);
         float py = cy + 0.18F + 0.64F * hash(wx, wy, 223);
         float dist = (x - px) * (x - px) + (y - py) * (y - py);
         if (dist < first) {
            second = first; first = dist; tone = hash(wx, wy, 224);
         } else if (dist < second) second = dist;
      }
      float edge = smooth(0.015F, 0.18F, second - first);
      float relief = -27 * (1 - edge) + (tone - 0.5F) * 23 + (field(u, v, 221) - 0.5F) * 18;
      int c = shade(tile == Tex.GRAVEL ? 0x8A8379 : 0x848780, relief);
      if (tile == Tex.MOSSY) {
         float moss = smooth(0.46F, 0.65F, field(u, v, 781));
         c = TexPaint.mix(c, shade(0x647849, relief * 0.45F), moss * 0.85F);
      }
      return c;
   }

   private static int ore(int tile, float u, float v) {
      int[] palette = {0x353738, 0xBCA08A, 0xC5A44B, 0x73B8BA, 0xA74D43, 0x526C9C};
      float x = u * 3, y = v * 3;
      int ix = (int)x, iy = (int)y;
      float nearest = 100, light = 0;
      for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) {
         int cx = ix + dx, cy = iy + dy;
         int wx = Math.floorMod(cx, 3), wy = Math.floorMod(cy, 3);
         if (hash(wx, wy, tile + 510) < 0.22F) continue;
         float px = cx + 0.2F + hash(wx, wy, tile + 511) * 0.6F;
         float py = cy + 0.2F + hash(wx, wy, tile + 512) * 0.6F;
         float d = Math.abs(x - px) * 0.85F + Math.abs(y - py);
         if (d < nearest) { nearest = d; light = -(x - px) * 32 - (y - py) * 48; }
      }
      float grain = noise(u * 32, v * 32, 32, tile + 220);
      float mask = 1 - smooth(0.19F, 0.25F, nearest + (grain - 0.5F) * 0.10F);
      return TexPaint.mix(earth(Tex.STONE, u, v), shade(palette[tile - Tex.COAL], light + (grain - 0.5F) * 9), mask);
   }

   private static int bark(int tile, float u, float v) {
      // Turned logs use precisely the same painter with swapped axes.
      if (tile >= 246 && tile <= 248) {
         int base = tile == 246 ? Tex.LOG_SIDE : tile == 247 ? Tex.BIRCH_SIDE : Tex.SPRUCE_SIDE;
         return bark(base, v, u);
      }
      if (tile >= TexArt313.TURNED0 && tile < TexArt313.TURNED0 + 3) return bark(TexArt313.BARK0 + tile - TexArt313.TURNED0, v, u);
      int base = tile == Tex.SPRUCE_SIDE ? 0x594633 : tile == Tex.BIRCH_SIDE ? 0xC9C7B8 : 0x786044;
      if (tile == TexArt313.BARK0) base = 0x6E6548;
      if (tile == TexArt313.BARK0 + 1) base = 0x7E7A70;
      if (tile == TexArt313.BARK0 + 2) base = 0x4C4032;
      float warp = noise(u * 4, v * 4, 4, tile + 48);
      float grain = noise(u * 32 + warp * 0.8F, v * 4, 4, tile + 50);
      float relief = (grain - 0.5F) * 39 + (field(u, v, tile + 49) - 0.5F) * 15;
      if (tile == Tex.BIRCH_SIDE) {
         float marks = noise(u * 8, v * 32, 8, 842);
         relief = (grain - 0.5F) * 10 - smooth(0.70F, 0.84F, marks) * 90;
      }
      return shade(base, relief);
   }

   private static int planks(int tile, int x, int y) {
      int species = tile == Tex.PLANKS ? -1 : tile - TexArt313.PLANKS0;
      int[] palette = {0xA78860, 0x73573A, 0xC3B385, 0x9E7555, 0xAF6D49, 0x51402E};
      int base = palette[species + 1];
      int row = y / 16;
      float u = x / 64.0F, v = y / 64.0F;
      float warp = noise(u * 4, v * 4, 4, 909 + species);
      float grain = (float)Math.sin((v * 32 + warp * 1.1F) * Math.PI * 2);
      float shade = (noise(u * 4, v * 32, 4, 800 + species) - 0.5F) * 14 + grain * 2.5F;
      shade += (hash(row, 0, species + 730) - 0.5F) * 9;
      int seamX = (x + (row % 2) * 32) % 64;
      int seamY = y % 16;
      if (seamY == 0 || seamX == 0) shade -= 26;
      else if (seamY == 1 || seamX == 1) shade += 5;
      return color(255, (base >> 16 & 255) + shade, (base >> 8 & 255) + shade, (base & 255) + shade);
   }

   private static int masonry(int tile, int x, int y) {
      boolean brick = tile == Tex.BRICKS;
      int row = y / 16;
      int cellX = (x + (row % 2) * 16) % 32;
      int cellY = y % 16;
      float n = field(x / 64.0F, y / 64.0F, tile + 84);
      int base = brick ? 0xA57560 : 0x858681;
      float shade = (n - 0.5F) * 24;
      if (cellX < 2 || cellY < 2) {
         base = brick ? 0xA6A08F : 0x535650;
         shade *= 0.35F;
      } else if (cellX == 2 || cellY == 2) shade += 7;
      else if (cellX == 31 || cellY == 15) shade -= 7;
      return color(255, (base >> 16 & 255) + shade, (base >> 8 & 255) + shade, (base & 255) + shade);
   }

   /** Smooth colour reconstruction with alpha-normalised samples. Keep the original silhouette exactly:
    * extruded item geometry still uses the 16px mask, so filtering must not grow/shrink its outline.
    * Animated tiles use this same path without static micrograin (no sparkling between frames).
    */
   static int[] tile(int tile, int[] src, boolean animated) {
      int[] out = new int[SIZE * SIZE];
      boolean opaque = true;
      for (int c : src) if ((c >>> 24) != 255) { opaque = false; break; }
      for (int y = 0; y < SIZE; y++) {
         for (int x = 0; x < SIZE; x++) {
            int alpha = src[(y / 4) * 16 + x / 4] >>> 24;
            if (alpha == 0) continue;
            float u = x / (float)SIZE, v = y / (float)SIZE;
            if (!animated && opaque && natural(tile)) {
               out[y * SIZE + x] = earth(tile, u, v);
               continue;
            }
            if (!animated && opaque && (tile == Tex.PLANKS || tile >= TexArt313.PLANKS0 && tile < TexArt313.PLANKS0 + 5)) {
               out[y * SIZE + x] = planks(tile, x, y);
               continue;
            }
            if (!animated && opaque && (tile == Tex.BRICKS || tile == Tex.STONEBRICK)) {
               out[y * SIZE + x] = masonry(tile, x, y);
               continue;
            }
            if (!animated && opaque) {
               if (tile == Tex.COBBLE || tile == Tex.MOSSY || tile == Tex.GRAVEL) {
                  out[y * SIZE + x] = rubble(tile, u, v); continue;
               }
               if (tile >= Tex.COAL && tile <= Tex.LAPIS) {
                  out[y * SIZE + x] = ore(tile, u, v); continue;
               }
               if (tile == Tex.LOG_SIDE || tile == Tex.SPRUCE_SIDE || tile == Tex.BIRCH_SIDE
                  || tile >= 246 && tile <= 248 || tile >= TexArt313.BARK0 && tile < TexArt313.BARK0 + 3
                  || tile >= TexArt313.TURNED0 && tile < TexArt313.TURNED0 + 3) {
                  out[y * SIZE + x] = bark(tile, u, v); continue;
               }
            }
            float sx = (x + 0.5F) / 4 - 0.5F, sy = (y + 0.5F) / 4 - 0.5F;
            int ix = (int)Math.floor(sx), iy = (int)Math.floor(sy);
            float fx = sx - ix, fy = sy - iy;
            float r = 0, g = 0, b = 0, sum = 0;
            for (int dy = 0; dy <= 1; dy++) for (int dx = 0; dx <= 1; dx++) {
               // Clamp authored borders: a workstation face must not wrap onto its opposite edge.
               int c = src[Math.max(0, Math.min(15, iy + dy)) * 16 + Math.max(0, Math.min(15, ix + dx))];
               float w = (dx == 0 ? 1 - fx : fx) * (dy == 0 ? 1 - fy : fy) * (c >>> 24) / 255.0F;
               r += (c >> 16 & 255) * w;
               g += (c >> 8 & 255) * w;
               b += (c & 255) * w;
               sum += w;
            }
            if (sum == 0) continue;
            r /= sum; g /= sum; b /= sum;
            // Reduce chalky highlights and crushed relief shadows; retain colour-coded ores and dyes.
            float detail = 0;
            if (!animated && tile != Tex.WHITE) {
               detail = (field(u, v, tile + 600) - 0.5F) * 5;
               detail += (noise(u * 64, v * 64, 64, tile) - 0.5F) * 1.8F;
            }
            out[y * SIZE + x] = color(alpha, r + detail, g + detail, b + detail);
         }
      }
      return out;
   }

   static int[] atlas(int[] source) {
      int width = SIZE * Tex.COLS;
      int[] out = new int[width * width];
      for (int t = 0; t < Tex.COLS * Tex.COLS; t++) {
         int[] low = new int[256];
         for (int y = 0; y < 16; y++) {
            System.arraycopy(source, (Tex.ty(t) + y) * Tex.PX + Tex.tx(t), low, y * 16, 16);
         }
         boolean animated = t == Tex.WATER || t == Tex.LAVA || t == 235 || t == TexArt312.PORTAL;
         int[] high = tile(t, low, animated);
         for (int y = 0; y < SIZE; y++) {
            System.arraycopy(high, y * SIZE, out, ((t / Tex.COLS) * SIZE + y) * width + (t % Tex.COLS) * SIZE, SIZE);
         }
      }
      return out;
   }
}
