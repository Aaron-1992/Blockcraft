package bc;

/** Default 32px pixel-art materials. No spatial interpolation, blur, emboss pass or generated normals.
 * A small stepped palette and coherent pixel clusters keep materials readable at Minecraft scale.
 * The 16px CPU atlas remains the pack interchange format and item extrusion mask.
 */
final class TexPixel32 {
   static final int SIZE = 32;
   private TexPixel32() {}
   static boolean enabled() { return ResourcePacks.ID_DEFAULT.equals(ResourcePacks.activeId()); }

   static float hash(int x, int y, int seed) {
      int h = x * 374761393 + y * 668265263 + seed * 144269;
      h = (h ^ (h >>> 13)) * 1274126177;
      return ((h ^ (h >>> 16)) & 65535) / 65535.0F;
   }

   static int shade(int color, int delta) {
      return (color & 0xFF000000) | Tex.clamp((color >> 16 & 255) + delta) << 16
         | Tex.clamp((color >> 8 & 255) + delta) << 8 | Tex.clamp((color & 255) + delta);
   }

   /** Clustered, discrete tones: neighbouring pixels belong to the same mineral/grain, not a blur. */
   private static int grain(int x, int y, int seed, int step) {
      float n = hash((x & 31) / 3, (y & 31) / 2, seed) * 0.70F
         + hash(x & 31, y & 31, seed + 1) * 0.30F;
      return ((int)(n * 5) - 2) * step;
   }

   private static int earth(int tile, int x, int y) {
      int base = 0xFF80827D, step = 7;
      switch (tile) {
         case Tex.GRASS_TOP: base = 0xFFA6A6A6; step = 11; break;
         case Tex.DIRT: base = 0xFF896447; step = 9; break;
         case Tex.SAND: base = 0xFFD9CA97; step = 5; break;
         case Tex.SNOW: base = 0xFFEAF0F4; step = 3; break;
         case TexArt313.RED_SAND: base = 0xFFBB713D; step = 6; break;
         case TexArt313.GRANITE: base = 0xFF9D786B; step = 10; break;
         case TexArt313.DIORITE: base = 0xFFBABBB5; step = 13; break;
         case TexArt313.ANDESITE: base = 0xFF898B86; step = 7; break;
         case Color312.T_CLAY: base = 0xFF9FA9B0; step = 4; break;
      }
      int d = grain(x, y, 701 + tile, step);
      if (tile == Tex.GRASS_TOP) {
         // Short irregular blades: pixel clusters with one highlight, not long blurry stripes.
         int blade = (int)(hash(x / 2, y / 4, 901) * 4);
         if ((y & 3) == blade && (x & 1) == 0) d += 12;
      } else if (tile == Tex.DIRT) {
         if (hash(x / 2, y / 2, 820) > 0.91F) return shade(0xFF998A77, (x + y & 1) * 8);
      } else if (tile == Tex.STONE || tile == TexArt313.ANDESITE) {
         int seam = (int)(hash(x / 6, y / 8, 300) * 8);
         if (y % 8 == seam && hash(x / 3, y / 8, 303) > 0.64F) d -= 9;
      }
      return shade(base, d);
   }

   private static boolean earthTile(int t) {
      return t == Tex.GRASS_TOP || t == Tex.DIRT || t == Tex.STONE || t == Tex.SAND || t == Tex.SNOW
         || t == TexArt313.RED_SAND || t == TexArt313.GRANITE || t == TexArt313.DIORITE
         || t == TexArt313.ANDESITE || t == Color312.T_CLAY;
   }

   private static final int[] WOOD = {0xFFA98A58, 0xFF765839, 0xFFCEBB83, 0xFFA47650, 0xFFB66B3C, 0xFF533B26};
   private static int planks(int tile, int x, int y) {
      int sp = tile == Tex.PLANKS ? 0 : tile - TexArt313.PLANKS0 + 1;
      int row = y / 8, xx = (x + (row & 1) * 16) & 31, yy = y & 7;
      int c = WOOD[sp];
      int tone = ((int)(hash(xx / 6, row, 880 + sp) * 3) - 1) * 7;
      if (yy == 0 || xx == 0) return shade(c, -36);
      if (yy == 1) return shade(c, 12 + tone);
      if (yy == 7) return shade(c, -15 + tone);
      int streak = (int)(hash(xx / 7, row, sp + 22) * 4) + 2;
      if (yy == streak) tone -= 15;
      if (yy == streak + 1 && hash(xx / 3, row, 37) > 0.55F) tone += 9;
      if (xx == 3 && yy == 4) tone -= 24; // small peg, not black outlines around every pixel
      return shade(c, tone);
   }

   private static int bark(int tile, int x, int y) {
      if (tile >= 246 && tile <= 248) return bark(tile == 246 ? Tex.LOG_SIDE : tile == 247 ? Tex.BIRCH_SIDE : Tex.SPRUCE_SIDE, y, x);
      if (tile >= TexArt313.TURNED0 && tile < TexArt313.TURNED0 + 3) return bark(TexArt313.BARK0 + tile - TexArt313.TURNED0, y, x);
      int base = tile == Tex.SPRUCE_SIDE ? 0xFF60452D : tile == Tex.BIRCH_SIDE ? 0xFFD5D2BD : 0xFF785833;
      if (tile == TexArt313.BARK0) base = 0xFF766343;
      if (tile == TexArt313.BARK0 + 1) base = 0xFF777269;
      if (tile == TexArt313.BARK0 + 2) base = 0xFF503B29;
      int ridge = (x + (int)(hash(x / 5, y / 8, tile) * 3)) % 5;
      int d = ridge == 0 ? -25 : ridge == 1 ? 12 : grain(x / 2, y / 3, tile, 6);
      if (tile == Tex.BIRCH_SIDE) {
         d = grain(x, y, tile, 3);
         if ((y & 7) < 2 && hash(x / 5, y / 8, 44) > 0.55F) d -= 72;
      }
      return shade(base, d);
   }

   private static int rings(int tile, int x, int y) {
      float dx = x - 15.5F, dy = y - 15.5F;
      int radius = (int)Math.max(Math.abs(dx), Math.abs(dy));
      int base = tile == Tex.SPRUCE_TOP ? 0xFFAC8B58 : 0xFFC3A06B;
      if (tile >= TexArt313.RING0 && tile < TexArt313.RING0 + 3) base = WOOD[tile - TexArt313.RING0 + 3];
      if (radius >= 14) return bark(tile == Tex.SPRUCE_TOP ? Tex.SPRUCE_SIDE : Tex.LOG_SIDE, x, y);
      int r = (int)(Math.sqrt(dx * dx + dy * dy) + hash(x / 4, y / 4, 200) * 1.5F);
      return shade(base, r % 4 == 0 ? -24 : r % 4 == 1 ? 10 : grain(x, y, 509, 4));
   }

   private static int masonry(int tile, int x, int y) {
      boolean brick = tile == Tex.BRICKS;
      int row = y / 8, xx = (x + (row & 1) * 8) % 16, yy = y % 8;
      int base = brick ? 0xFFAB6B51 : 0xFF898D85;
      if (xx == 0 || yy == 0) return brick ? 0xFFACA28B : 0xFF565C53;
      int tone = grain(x, y, tile, 5) + ((int)(hash((x + (row & 1) * 8) / 16, row, 639) * 3) - 1) * 8;
      if (yy == 1 || xx == 1) tone += 9;
      if (yy == 7 || xx == 15) tone -= 9;
      return shade(base, tone);
   }

   /** Irregular pixel-edged fieldstones. Hard mortar edge + restrained one-pixel rim. */
   private static int rubble(int tile, int x, int y) {
      int cells = tile == Tex.GRAVEL ? 8 : 4;
      float px = x * cells / 32.0F, py = y * cells / 32.0F;
      int ix = (int)px, iy = (int)py;
      float a = 100, b = 100, variation = 0;
      for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) {
         int cx = ix + dx, cy = iy + dy, wx = Math.floorMod(cx, cells), wy = Math.floorMod(cy, cells);
         float sx = cx + 0.2F + hash(wx, wy, 11) * 0.6F, sy = cy + 0.2F + hash(wx, wy, 12) * 0.6F;
         float dist = (px - sx) * (px - sx) + (py - sy) * (py - sy);
         if (dist < a) { b = a; a = dist; variation = hash(wx, wy, 13); }
         else if (dist < b) b = dist;
      }
      float edge = b - a;
      int base = tile == Tex.GRAVEL ? 0xFF8F877B : 0xFF868980;
      if (edge < 0.12F) return shade(base, -32);
      int d = ((int)(variation * 5) - 2) * 6 + grain(x, y, 287, 4);
      if (edge < 0.27F) d += 8;
      if (tile == Tex.MOSSY && hash(x / 3, y / 3, 478) > 0.58F) base = 0xFF687B41;
      return shade(base, d);
   }

   private static final int[] ORES = {0xFF383B3B, 0xFFBD9D84, 0xFFE0BC52, 0xFF63C7CA, 0xFFB83E32, 0xFF4965AF};
   private static int ore(int tile, int x, int y) {
      int cellX = x / 8, cellY = y / 8;
      int cx = cellX * 8 + 2 + (int)(hash(cellX, cellY, tile + 75) * 3);
      int cy = cellY * 8 + 2 + (int)(hash(cellX, cellY, tile + 76) * 3);
      int dx = x - cx, dy = y - cy;
      if (hash(cellX, cellY, tile + 77) > 0.42F && Math.abs(dx) + Math.abs(dy) <= 3) {
         int tone = dx + dy < 0 ? 22 : dx + dy > 1 ? -25 : 0;
         return shade(ORES[tile - Tex.COAL], tone);
      }
      return earth(Tex.STONE, x, y);
   }

   /** Keep every authored pixel and alpha edge intact. No averaging neighbouring colours at all. */
   static int[] tile(int tile, int[] src, boolean animated) {
      int[] out = new int[SIZE * SIZE];
      boolean opaque = true;
      for (int c : src) if ((c >>> 24) != 255) { opaque = false; break; }
      for (int y = 0; y < SIZE; y++) for (int x = 0; x < SIZE; x++) {
         int c = src[(y / 2) * 16 + x / 2];
         if ((c >>> 24) == 0) continue;
         if (!animated && opaque) {
            if (earthTile(tile)) c = earth(tile, x, y);
            else if (tile == Tex.PLANKS || tile >= TexArt313.PLANKS0 && tile < TexArt313.PLANKS0 + 5) c = planks(tile, x, y);
            else if (tile == Tex.BRICKS || tile == Tex.STONEBRICK) c = masonry(tile, x, y);
            else if (tile == Tex.COBBLE || tile == Tex.MOSSY || tile == Tex.GRAVEL) c = rubble(tile, x, y);
            else if (tile >= Tex.COAL && tile <= Tex.LAPIS) c = ore(tile, x, y);
            else if (tile == Tex.LOG_SIDE || tile == Tex.SPRUCE_SIDE || tile == Tex.BIRCH_SIDE
               || tile >= 246 && tile <= 248 || tile >= TexArt313.BARK0 && tile < TexArt313.BARK0 + 3
               || tile >= TexArt313.TURNED0 && tile < TexArt313.TURNED0 + 3) c = bark(tile, x, y);
            else if (tile == Tex.LOG_TOP || tile == Tex.SPRUCE_TOP || tile >= TexArt313.RING0 && tile < TexArt313.RING0 + 3) c = rings(tile, x, y);
            else if (tile >= Tex.WOOL0 && tile < Tex.WOOL0 + 16) {
               int[] wool = Tex.WOOL[tile - Tex.WOOL0];
               c = Tex.rgb(wool[0], wool[1], wool[2]);
               c = shade(c, ((x + y) & 1) == 0 ? 6 : -6);
            }
         }
         out[y * SIZE + x] = c;
      }
      return out;
   }

   static int[] atlas(int[] source) {
      int width = SIZE * Tex.COLS;
      int[] out = new int[width * width];
      for (int t = 0; t < Tex.COLS * Tex.COLS; t++) {
         int[] low = new int[256];
         for (int y = 0; y < 16; y++) System.arraycopy(source, (Tex.ty(t) + y) * Tex.PX + Tex.tx(t), low, y * 16, 16);
         boolean anim = t == Tex.WATER || t == Tex.LAVA || t == 235 || t == TexArt312.PORTAL;
         int[] high = tile(t, low, anim);
         if (t == Items.tile[Items.BOW]) high = BowSprite.pixels(0, false);
         for (int y = 0; y < SIZE; y++) System.arraycopy(high, y * SIZE, out, ((t / Tex.COLS) * SIZE + y) * width + (t % Tex.COLS) * SIZE, SIZE);
      }
      return out;
   }
}
