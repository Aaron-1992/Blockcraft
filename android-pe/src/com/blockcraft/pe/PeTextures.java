package com.blockcraft.pe;

import android.content.Context;
import android.opengl.GLES20;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;

/**
 * Blockcraft PE (Pocket Edition) OpenGL ES Pixel-Art Texture & Font Atlas Generator.
 * Ports the Update 3.18 / 3.19 hand-crafted pixel-art painters (TexPaint + TexPaint319 + TexExtra):
 * - Overlapping 4x3 / 3x4 pixel-art leaf blade clusters with cast shadows, 4-tone sunlit leaf blades & sky cutouts
 * - Multi-tone grass top & hanging grass skirt with drop shadows over pebble-strewn dirt
 * - Voronoi cobblestone & mossy cobble, cracked FBM stone, 3D-shaded mineral ore veins
 * - Bark-grooved logs with concentric wood rings, 4-board wood planks with staggered joints
 * - Bevelled item & tool sprites with dark outlines and directional top-left specular rims
 */
public final class PeTextures {
   public static final int ATLAS_W = 256;
   public static final int ATLAS_H = 256;
   public static final int TILE_SIZE = 16;
   private static final int N = 16;

   // Tile indices (0..255) in the 16x16 atlas
   public static final int T_GRASS_TOP = 0;
   public static final int T_STONE = 1;
   public static final int T_DIRT = 2;
   public static final int T_GRASS_SIDE = 3;
   public static final int T_PLANKS_OAK = 4;
   public static final int T_STONE_SLAB_SIDE = 5;
   public static final int T_STONE_SLAB_TOP = 6;
   public static final int T_BRICKS = 7;
   public static final int T_TNT_SIDE = 8;
   public static final int T_TNT_TOP = 9;
   public static final int T_TNT_BOTTOM = 10;
   public static final int T_COBWEB = 11;
   public static final int T_FLOWER_RED = 12;
   public static final int T_FLOWER_YELLOW = 13;
   public static final int T_WATER = 14;
   public static final int T_LAVA = 15;

   public static final int T_COBBLE = 16;
   public static final int T_BEDROCK = 17;
   public static final int T_SAND = 18;
   public static final int T_GRAVEL = 19;
   public static final int T_LOG_OAK_SIDE = 20;
   public static final int T_LOG_OAK_TOP = 21;
   public static final int T_IRON_BLOCK = 22;
   public static final int T_GOLD_BLOCK = 23;
   public static final int T_DIAMOND_BLOCK = 24;
   public static final int T_EMERALD_BLOCK = 25;
   public static final int T_CHEST_TOP = 26;
   public static final int T_CHEST_SIDE = 27;
   public static final int T_CHEST_FRONT = 28;
   public static final int T_MUSHROOM_RED = 29;
   public static final int T_MUSHROOM_BROWN = 30;
   public static final int T_FIRE = 31;

   public static final int T_GOLD_ORE = 32;
   public static final int T_IRON_ORE = 33;
   public static final int T_COAL_ORE = 34;
   public static final int T_BOOKSHELF = 35;
   public static final int T_MOSSY_COBBLE = 36;
   public static final int T_OBSIDIAN = 37;
   public static final int T_TALL_GRASS = 38;
   public static final int T_CRAFT_TOP = 43;
   public static final int T_FURNACE_FRONT = 44;
   public static final int T_FURNACE_SIDE = 45;
   public static final int T_CRAFT_SIDE = 46;

   public static final int T_SPONGE = 48;
   public static final int T_GLASS = 49;
   public static final int T_DIAMOND_ORE = 50;
   public static final int T_REDSTONE_ORE = 51;
   public static final int T_LEAVES_OAK = 52;
   public static final int T_LEAVES_SPRUCE = 53;
   public static final int T_STONEBRICK = 54;
   public static final int T_CRACKED_STONEBRICK = 55;
   public static final int T_MOSSY_STONEBRICK = 56;

   public static final int T_WOOL_WHITE = 64;
   public static final int T_WOOL_RED = 65;
   public static final int T_WOOL_BLUE = 66;
   public static final int T_WOOL_GREEN = 67;
   public static final int T_WOOL_YELLOW = 68;
   public static final int T_SNOW = 69;
   public static final int T_ICE = 70;
   public static final int T_SNOW_SIDE = 71;
   public static final int T_CACTUS_TOP = 72;
   public static final int T_CACTUS_SIDE = 73;
   public static final int T_SANDSTONE_TOP = 74;
   public static final int T_SANDSTONE_SIDE = 75;
   public static final int T_TORCH = 80;
   public static final int T_GLOWSTONE = 81;
   public static final int T_NETHERRACK = 82;
   public static final int T_SOUL_SAND = 83;
   public static final int T_NETHER_BRICKS = 84;
   public static final int T_END_STONE = 85;
   public static final int T_QUARTZ_BLOCK = 86;
   public static final int T_SEA_LANTERN = 87;
   public static final int T_PRISMARINE = 88;
   public static final int T_SLIME_BLOCK = 89;
   public static final int T_REDSTONE_BLOCK = 90;
   public static final int T_COAL_BLOCK = 91;
   public static final int T_PLANKS_SPRUCE = 92;
   public static final int T_PLANKS_BIRCH = 93;
   public static final int T_LOG_SPRUCE = 94;
   public static final int T_PACKED_ICE = 95;

   public static final int T_BED_RED = 96;
   public static final int T_RED_SANDSTONE = 97;
   public static final int T_TERRACOTTA = 98;
   public static final int T_CONCRETE_GRAY = 99;
   public static final int T_CONCRETE_YELLOW = 100;
   public static final int T_CONCRETE_BLUE = 101;
   public static final int T_CONCRETE_RED = 102;
   public static final int T_PURPUR = 103;
   public static final int T_LADDER = 104;
   public static final int T_BARS = 105;

   // Crack animation stages (112..121)
   public static final int T_CRACK_0 = 112;

   // Item / Weapon sprites (128..160)
   public static final int T_ITEM_SWORD_WOOD = 128;
   public static final int T_ITEM_SWORD_STONE = 129;
   public static final int T_ITEM_SWORD_IRON = 130;
   public static final int T_ITEM_SWORD_DIAMOND = 131;
   public static final int T_ITEM_PICK_WOOD = 132;
   public static final int T_ITEM_PICK_STONE = 133;
   public static final int T_ITEM_PICK_IRON = 134;
   public static final int T_ITEM_PICK_DIAMOND = 135;
   public static final int T_ITEM_SHOVEL_IRON = 136;
   public static final int T_ITEM_SHOVEL_DIAMOND = 137;
   public static final int T_ITEM_AXE_IRON = 138;
   public static final int T_ITEM_BOW = 139;
   public static final int T_ITEM_ARROW = 140;
   public static final int T_ITEM_GOLDEN_APPLE = 141;
   public static final int T_ITEM_STEAK = 142;
   public static final int T_ITEM_ENDER_PEARL = 143;
   public static final int T_ITEM_PISTOL = 144;
   public static final int T_ITEM_MACHINE_GUN = 145;
   public static final int T_ITEM_SNIPER = 146;
   public static final int T_ITEM_TNT_GRENADE = 147;
   public static final int T_ITEM_C4_BOMB = 148;
   public static final int T_ITEM_SIEGE_CANNON = 149;
   public static final int T_ITEM_MORTAR = 150;
   public static final int T_ITEM_SNOWBALL = 151;
   public static final int T_HEART_FULL = 152;
   public static final int T_HEART_EMPTY = 153;
   public static final int T_HUNGER_FULL = 154;
   public static final int T_HUNGER_EMPTY = 155;

   public static final int FONT_COLS = 16;
   public static final int FONT_CELL_W = 8;
   public static final int FONT_CELL_H = 11;

   public int atlasTexId = 0;
   public int fontTexId = 0;
   public int whiteTexId = 0;
   public final int[] fontWidths = new int[128];

   public void initGL(Context context) {
      int[] tex = new int[3];
      GLES20.glGenTextures(3, tex, 0);
      this.atlasTexId = tex[0];
      this.fontTexId = tex[1];
      this.whiteTexId = tex[2];

      int[] pixels = generateAtlasPixels();
      uploadTexture(this.atlasTexId, ATLAS_W, ATLAS_H, pixels);

      int[] fontPixels = generateFontPixels(context);
      uploadTexture(this.fontTexId, 128, 128, fontPixels);

      int[] white = new int[16];
      for (int i = 0; i < 16; i++) {
         white[i] = 0xFFFFFFFF;
      }
      uploadTexture(this.whiteTexId, 4, 4, white);
   }

   private static void uploadTexture(int texId, int w, int h, int[] argb) {
      ByteBuffer buf = ByteBuffer.allocateDirect(w * h * 4).order(ByteOrder.nativeOrder());
      for (int i = 0; i < w * h; i++) {
         int c = argb[i];
         int a = (c >>> 24) & 0xFF;
         int r = (c >>> 16) & 0xFF;
         int g = (c >>> 8) & 0xFF;
         int b = c & 0xFF;
         buf.put((byte)r);
         buf.put((byte)g);
         buf.put((byte)b);
         buf.put((byte)a);
      }
      buf.position(0);
      GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId);
      GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
      GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
      GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
      GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_TEXTURE_WRAP_T == 0 ? 0 : GLES20.GL_CLAMP_TO_EDGE);
      GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, w, h, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buf);
   }

   // ====================================================================
   //  Color, Hash, FBM & Voronoi Helpers (from TexPaint & TexPaint319)
   // ====================================================================
   private static int clamp(int v) {
      return v < 0 ? 0 : (v > 255 ? 255 : v);
   }

   private static int rgb(int r, int g, int b) {
      return 0xFF000000 | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
   }

   private static int argb(int a, int r, int g, int b) {
      return (clamp(a) << 24) | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
   }

   private static int c(int hex) {
      return 0xFF000000 | (hex & 0xFFFFFF);
   }

   private static int mul(int col, float k) {
      int a = (col >>> 24) & 255;
      int r = (int)(((col >> 16) & 255) * k);
      int g = (int)(((col >> 8) & 255) * k);
      int b = (int)((col & 255) * k);
      return (a << 24) | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
   }

   private static int tintRgb(int col, float kr, float kg, float kb) {
      int a = (col >>> 24) & 255;
      if (a == 0) return 0;
      int r = (int)(((col >> 16) & 255) * kr);
      int g = (int)(((col >> 8) & 255) * kg);
      int b = (int)((col & 255) * kb);
      return (a << 24) | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
   }

   private static int add(int col, int d) {
      int a = (col >>> 24) & 255;
      return (a << 24)
         | (clamp(((col >> 16) & 255) + d) << 16)
         | (clamp(((col >> 8) & 255) + d) << 8)
         | clamp((col & 255) + d);
   }

   private static int mix(int a, int b, float t) {
      float u = 1.0F - t;
      int al = (int)(((a >>> 24) & 255) * u + ((b >>> 24) & 255) * t);
      int r = (int)(((a >> 16) & 255) * u + ((b >> 16) & 255) * t);
      int g = (int)(((a >> 8) & 255) * u + ((b >> 8) & 255) * t);
      int bl = (int)((a & 255) * u + (b & 255) * t);
      return (clamp(al) << 24) | (clamp(r) << 16) | (clamp(g) << 8) | clamp(bl);
   }

   private static int step(int[] pal, float v) {
      if (v <= 0.0F) return pal[0];
      if (v >= 1.0F) return pal[pal.length - 1];
      int i = (int)(v * pal.length);
      return pal[i >= pal.length ? pal.length - 1 : i];
   }

   private static int h32(int x, int y, int seed) {
      int n = x * 374761393 + y * 668265263 + seed * 1274126177;
      n = (n ^ (n >>> 13)) * 1274126177;
      return n ^ (n >>> 16);
   }

   private static float hash(int x, int y, int seed) {
      return (h32(x, y, seed) & 0xFFFFFF) / 16777216.0F;
   }

   private static float vn(float px, float py, int cells, int seed) {
      float x = px * cells / N;
      float y = py * cells / N;
      int x0 = (int)Math.floor(x);
      int y0 = (int)Math.floor(y);
      float fx = x - x0;
      float fy = y - y0;
      fx = fx * fx * (3.0F - 2.0F * fx);
      fy = fy * fy * (3.0F - 2.0F * fy);
      int xa = Math.floorMod(x0, cells);
      int xb = Math.floorMod(x0 + 1, cells);
      int ya = Math.floorMod(y0, cells);
      int yb = Math.floorMod(y0 + 1, cells);
      float a = hash(xa, ya, seed);
      float bb = hash(xb, ya, seed);
      float cc = hash(xa, yb, seed);
      float d = hash(xb, yb, seed);
      float top = a + (bb - a) * fx;
      float bot = cc + (d - cc) * fx;
      return top + (bot - top) * fy;
   }

   private static float fbm(int x, int y, int seed) {
      float px = x + 0.5F;
      float py = y + 0.5F;
      return vn(px, py, 2, seed) * 0.34F + vn(px, py, 4, seed + 11) * 0.38F + vn(px, py, 8, seed + 23) * 0.28F;
   }

   private static float contrast(float v, float k) {
      float d = (v - 0.5F) * k + 0.5F;
      return d < 0.0F ? 0.0F : (d > 1.0F ? 1.0F : d);
   }

   private static void blitTile(int[] atlas, int tile, int[] tilePixels) {
      int tx0 = (tile & 15) * 16;
      int ty0 = (tile >> 4) * 16;
      for (int y = 0; y < 16; y++) {
         System.arraycopy(tilePixels, y * 16, atlas, (ty0 + y) * ATLAS_W + tx0, 16);
      }
   }

   // ====================================================================
   //  Master Atlas Builder (TexPaint + TexPaint319 + TexExtra)
   // ====================================================================
   public static int[] generateAtlasPixels() {
      int[] px = new int[ATLAS_W * ATLAS_H];

      // 1. Grass Top & Grass Side (Rich emerald biome tint baked in for crisp 3D + 2D HUD icons)
      blitTile(px, T_GRASS_TOP, paintGrassTop());
      blitTile(px, T_DIRT, paintDirt());
      blitTile(px, T_GRASS_SIDE, paintGrassSide(false));
      blitTile(px, T_SNOW_SIDE, paintGrassSide(true));
      blitTile(px, T_TALL_GRASS, paintTallGrass());

      // 2. Crisp Pixel-Art Leaves (Update 3.19 overlapping leaflet blades with cast shadows & sky gaps)
      blitTile(px, T_LEAVES_OAK, paintLeaves(0, 0.34F, 0.78F, 0.24F));
      blitTile(px, T_LEAVES_SPRUCE, paintLeaves(1, 0.26F, 0.58F, 0.30F));

      // 3. Stone, Cobble, Mossy Cobble, Bedrock, Sand, Gravel, Sandstone
      blitTile(px, T_STONE, paintStone(101));
      blitTile(px, T_COBBLE, paintCobble(202, 0, 0, 0));
      blitTile(px, T_MOSSY_COBBLE, paintMossyCobble());
      blitTile(px, T_BEDROCK, paintBedrock());
      blitTile(px, T_SAND, paintSand(0, 0, 0));
      blitTile(px, T_GRAVEL, paintGravel());
      blitTile(px, T_SANDSTONE_TOP, paintSandstoneTop(false));
      blitTile(px, T_SANDSTONE_SIDE, paintSandstoneSide(0, 0, 0));
      blitTile(px, T_RED_SANDSTONE, paintSandstoneSide(44, -28, -46));
      blitTile(px, T_STONE_SLAB_TOP, paintStoneSlabTop());
      blitTile(px, T_STONE_SLAB_SIDE, paintStoneSlabSide());

      // 4. Logs & Planks (Oak, Spruce, Birch)
      blitTile(px, T_LOG_OAK_SIDE, paintLogSide(c(0x47361F), c(0x634C2D), c(0x785D38), c(0x8B6E44), 601));
      blitTile(px, T_LOG_OAK_TOP, paintLogTop(c(0x523E24), c(0x6B5231), c(0xAA8954), c(0x947344), c(0xBFA067), 602));
      blitTile(px, T_LOG_SPRUCE, paintLogSide(c(0x281B0E), c(0x3B2814), c(0x4A331B), c(0x5A3F22), 603));
      blitTile(px, T_PLANKS_OAK, paintPlanks(c(0xA6834E), 701));
      blitTile(px, T_PLANKS_SPRUCE, paintPlanks(c(0x725432), 702));
      blitTile(px, T_PLANKS_BIRCH, paintPlanks(c(0xD4C388), 703));

      // 5. Ores (3D shaded mineral blobs embedded in stone)
      blitTile(px, T_COAL_ORE, paintOre(c(0x2A2A2E), c(0x16161A), c(0x48484E), 801, 7, false));
      blitTile(px, T_IRON_ORE, paintOre(c(0xD8AF93), c(0xA67B5E), c(0xF2D3BC), 802, 6, false));
      blitTile(px, T_GOLD_ORE, paintOre(c(0xFCEE4B), c(0xC99E18), c(0xFFFFB5), 803, 6, false));
      blitTile(px, T_DIAMOND_ORE, paintOre(c(0x5DECF5), c(0x25A0B5), c(0xD2FFFF), 804, 5, true));
      blitTile(px, T_REDSTONE_ORE, paintOre(c(0xE32626), c(0x8F0E0E), c(0xFF7B7B), 805, 7, true));

      // 6. Bricks, Stone Bricks, Glass, Obsidian, Glowstone
      blitTile(px, T_BRICKS, paintBricks());
      blitTile(px, T_STONEBRICK, paintStoneBrick(false, false));
      blitTile(px, T_CRACKED_STONEBRICK, paintStoneBrick(true, false));
      blitTile(px, T_MOSSY_STONEBRICK, paintStoneBrick(false, true));
      blitTile(px, T_GLASS, paintGlass());
      blitTile(px, T_OBSIDIAN, paintObsidian());
      blitTile(px, T_GLOWSTONE, paintGlowstone());

      // 7. Utility Blocks (Crafting Table, Furnace, Chest, Bookshelf, TNT, Torch, Ladder, Bed, Bars)
      blitTile(px, T_CRAFT_TOP, paintCraftTop());
      blitTile(px, T_CRAFT_SIDE, paintCraftSide());
      blitTile(px, T_FURNACE_FRONT, paintFurnaceFront(false));
      blitTile(px, T_FURNACE_SIDE, paintStoneBrick(false, false));
      blitTile(px, T_CHEST_TOP, paintChestTop());
      blitTile(px, T_CHEST_SIDE, paintChestSide(false));
      blitTile(px, T_CHEST_FRONT, paintChestSide(true));
      blitTile(px, T_BOOKSHELF, paintBookshelf());
      blitTile(px, T_TNT_SIDE, paintTntSide());
      blitTile(px, T_TNT_TOP, paintTntTop(true));
      blitTile(px, T_TNT_BOTTOM, paintTntTop(false));
      blitTile(px, T_TORCH, paintTorch());
      blitTile(px, T_LADDER, paintLadder());
      blitTile(px, T_BED_RED, paintBedTop());
      blitTile(px, T_BARS, paintBars());

      // 8. Fluids, Snow, Ice, Plants, Flowers, Cactus
      blitTile(px, T_WATER, paintWaterFrame(0));
      blitTile(px, T_LAVA, paintLavaFrame(0));
      blitTile(px, T_SNOW, paintSnow());
      blitTile(px, T_ICE, paintIce(false));
      blitTile(px, T_PACKED_ICE, paintIce(true));
      blitTile(px, T_CACTUS_SIDE, paintCactusSide());
      blitTile(px, T_CACTUS_TOP, paintCactusTop());
      blitTile(px, T_FLOWER_RED, paintFlower(true));
      blitTile(px, T_FLOWER_YELLOW, paintFlower(false));

      // 9. Metal/Gem Blocks, Wool, Nether, End & Concrete
      blitTile(px, T_IRON_BLOCK, paintMetalBlock(c(0xDCDCDC), c(0xC4C4C4), c(0x9E9E9E), c(0xF6F6F6), 310));
      blitTile(px, T_GOLD_BLOCK, paintMetalBlock(c(0xF6D028), c(0xD8AC16), c(0xAA7E0A), c(0xFFF088), 311));
      blitTile(px, T_DIAMOND_BLOCK, paintGemBlock(c(0x5CE8E0), c(0x34C0B8), c(0x1C8E88), c(0xC8FFFF), 312));
      blitTile(px, T_EMERALD_BLOCK, paintGemBlock(c(0x28D862), c(0x16AC48), c(0x0E7A30), c(0x98FFBA), 313));
      blitTile(px, T_REDSTONE_BLOCK, paintGemBlock(c(0xC91E14), c(0x9E140C), c(0x6E0C06), c(0xFF6858), 314));
      blitTile(px, T_COAL_BLOCK, paintCoalBlock());
      blitTile(px, T_QUARTZ_BLOCK, paintQuartzBlock());
      blitTile(px, T_SEA_LANTERN, paintSeaLantern());
      blitTile(px, T_PRISMARINE, paintMineralStone(390, new int[]{c(0x3E786E), c(0x4E8E82), c(0x60A496), c(0x74B8AA)}));
      blitTile(px, T_SLIME_BLOCK, paintSlimeBlock());
      blitTile(px, T_NETHERRACK, paintNetherrack());
      blitTile(px, T_SOUL_SAND, paintSoulSand());
      blitTile(px, T_NETHER_BRICKS, paintNetherBricks());
      blitTile(px, T_END_STONE, paintEndStone());
      blitTile(px, T_PURPUR, paintPurpur());

      blitTile(px, T_WOOL_WHITE, paintWool(0xEAECED, 1000));
      blitTile(px, T_WOOL_RED, paintWool(0xA62B24, 1014));
      blitTile(px, T_WOOL_BLUE, paintWool(0x2F3E9E, 1011));
      blitTile(px, T_WOOL_GREEN, paintWool(0x3B511A, 1013));
      blitTile(px, T_WOOL_YELLOW, paintWool(0xF0C629, 1004));
      blitTile(px, T_TERRACOTTA, paintMineralStone(480, new int[]{c(0x8E563C), c(0x985E42), c(0xA2664A), c(0xAC7052)}));
      blitTile(px, T_CONCRETE_GRAY, paintConcrete(0x383C40, 501));
      blitTile(px, T_CONCRETE_YELLOW, paintConcrete(0xF0B418, 502));
      blitTile(px, T_CONCRETE_BLUE, paintConcrete(0x2C3E98, 503));
      blitTile(px, T_CONCRETE_RED, paintConcrete(0xA22420, 504));

      // 10. Crack Stages (0..9)
      paintCrackStages(px);

      // 11. Tools, Weapons, Food & HUD Icons (TexExtra + TexPaint319 directional bevel)
      blitTile(px, T_ITEM_SWORD_WOOD, bevelSprite(paintToolSprite(4, 0x9C7A44), T_ITEM_SWORD_WOOD));
      blitTile(px, T_ITEM_SWORD_STONE, bevelSprite(paintToolSprite(4, 0x929498), T_ITEM_SWORD_STONE));
      blitTile(px, T_ITEM_SWORD_IRON, bevelSprite(paintToolSprite(4, 0xE2E6EC), T_ITEM_SWORD_IRON));
      blitTile(px, T_ITEM_SWORD_DIAMOND, bevelSprite(paintToolSprite(4, 0x4CF2E8), T_ITEM_SWORD_DIAMOND));

      blitTile(px, T_ITEM_PICK_WOOD, bevelSprite(paintToolSprite(1, 0x9C7A44), T_ITEM_PICK_WOOD));
      blitTile(px, T_ITEM_PICK_STONE, bevelSprite(paintToolSprite(1, 0x929498), T_ITEM_PICK_STONE));
      blitTile(px, T_ITEM_PICK_IRON, bevelSprite(paintToolSprite(1, 0xE2E6EC), T_ITEM_PICK_IRON));
      blitTile(px, T_ITEM_PICK_DIAMOND, bevelSprite(paintToolSprite(1, 0x4CF2E8), T_ITEM_PICK_DIAMOND));

      blitTile(px, T_ITEM_SHOVEL_IRON, bevelSprite(paintToolSprite(3, 0xE2E6EC), T_ITEM_SHOVEL_IRON));
      blitTile(px, T_ITEM_SHOVEL_DIAMOND, bevelSprite(paintToolSprite(3, 0x4CF2E8), T_ITEM_SHOVEL_DIAMOND));
      blitTile(px, T_ITEM_AXE_IRON, bevelSprite(paintToolSprite(2, 0xE2E6EC), T_ITEM_AXE_IRON));

      blitTile(px, T_ITEM_BOW, bevelSprite(paintBowSprite(), T_ITEM_BOW));
      blitTile(px, T_ITEM_ARROW, bevelSprite(paintArrowSprite(), T_ITEM_ARROW));
      blitTile(px, T_ITEM_GOLDEN_APPLE, bevelSprite(paintAppleSprite(0xFAD62A, 0xFFF488), T_ITEM_GOLDEN_APPLE));
      blitTile(px, T_ITEM_STEAK, bevelSprite(paintSteakSprite(), T_ITEM_STEAK));
      blitTile(px, T_ITEM_ENDER_PEARL, bevelSprite(paintPearlSprite(0x188C78, 0x5CF6DC), T_ITEM_ENDER_PEARL));
      blitTile(px, T_ITEM_SNOWBALL, bevelSprite(paintPearlSprite(0xDCEAF4, 0xFFFFFF), T_ITEM_SNOWBALL));

      blitTile(px, T_HEART_FULL, paintHeartTile(0xFFE82424, 0xFFFF8888, 0xFF280606));
      blitTile(px, T_HEART_EMPTY, paintHeartTile(0xFF2C1A1A, 0xFF482C2C, 0xFF120606));
      blitTile(px, T_HUNGER_FULL, paintHungerTile(0xFFD47A2A, 0xFFFFB868, 0xFF341808));
      blitTile(px, T_HUNGER_EMPTY, paintHungerTile(0xFF322216, 0xFF4C3624, 0xFF160C06));

      return px;
   }

   // ====================================================================
   //  TexPaint & TexPaint319 Terrain, Foliage & Block Painters
   // ====================================================================
   private static int[] paintLeaves(int kind, float kr, float kg, float kb) {
      int[] p = new int[N * N];
      int seed = 5500 + kind * 97;
      // 1. Deep shaded interior canopy base
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            int d = 112 + (int)(hash(x, y, seed + 2) * 20.0F);
            p[y * N + x] = tintRgb(rgb(d, d, d), kr, kg, kb);
         }
      }

      // 2. 16 hand-layered overlapping 4x3 / 3x4 pixel-art leaf blades tiled seamlessly modulo 16
      int[][] clusters = {
         {1, 1, 0}, {6, 0, 1}, {11, 2, 0}, {14, 5, 1},
         {2, 5, 1}, {7, 4, 0}, {10, 7, 1}, {0, 9, 0},
         {5, 9, 1}, {13, 10, 0}, {3, 13, 0}, {8, 12, 1},
         {12, 14, 1}, {4, 2, 1}, {9, 5, 0}, {15, 1, 0}
      };
      for (int i = 0; i < clusters.length; i++) {
         int cx = (clusters[i][0] + kind * 3) & 15;
         int cy = (clusters[i][1] + kind * 5) & 15;
         int dir = (clusters[i][2] + kind) & 1;
         paintLeaflet(p, cx, cy, dir, seed + i * 13, kr, kg, kb);
      }

      // 3. Carve crisp organic sky gaps in the deep pockets between overlapping leaf blades
      int[][] gaps = {
         {0, 4}, {5, 3}, {10, 1}, {13, 4},
         {3, 8}, {8, 3}, {12, 6}, {15, 8},
         {1, 12}, {6, 8}, {11, 11}, {14, 13},
         {4, 15}, {9, 14}, {7, 7}, {2, 0},
         {5, 12}, {10, 5}, {15, 3}, {0, 14}
      };
      for (int i = 0; i < gaps.length; i++) {
         int gx = (gaps[i][0] + kind * 3) & 15;
         int gy = (gaps[i][1] + kind * 5) & 15;
         p[gy * N + gx] = 0;
         // Darken the leaf rim directly above-left of each sky gap for extra depth
         int rx = (gx - 1) & 15;
         int ry = (gy - 1) & 15;
         if (p[ry * N + rx] != 0) {
            p[ry * N + rx] = mul(p[ry * N + rx], 0.82F);
         }
      }
      return p;
   }

   private static void paintLeaflet(int[] p, int cx, int cy, int dir, int seed, float kr, float kg, float kb) {
      int[][] shadow = dir == 0
         ? new int[][]{{1, 2}, {2, 2}, {3, 1}}
         : new int[][]{{1, 3}, {2, 2}, {2, 1}};
      for (int[] s : shadow) {
         int x = (cx + s[0]) & 15;
         int y = (cy + s[1]) & 15;
         if (p[y * N + x] != 0) {
            int sh = 102 + (int)(hash(x, y, seed) * 12.0F);
            p[y * N + x] = tintRgb(rgb(sh, sh, sh), kr, kg, kb);
         }
      }

      int[][] blade = dir == 0
         ? new int[][]{
            {0, 0, 2}, {1, 0, 3}, {2, 0, 2},
            {0, 1, 2}, {1, 1, 1}, {2, 1, 1}, {3, 0, 1},
            {1, 2, 0}, {2, 1, 0}
         }
         : new int[][]{
            {0, 0, 2}, {1, 0, 2},
            {0, 1, 3}, {1, 1, 1}, {2, 1, 0},
            {0, 2, 2}, {1, 2, 1}, {1, 3, 0}
         };
      for (int[] b : blade) {
         int x = (cx + b[0]) & 15;
         int y = (cy + b[1]) & 15;
         int jit = (int)((hash(x, y, seed + 7) - 0.5F) * 10.0F);
         int v = b[2] == 3 ? 224 + jit : (b[2] == 2 ? 202 + jit : (b[2] == 1 ? 174 + jit : 144 + jit));
         p[y * N + x] = tintRgb(rgb(v, v, v), kr, kg, kb);
      }
   }

   private static int[] paintGrassTop() {
      int[] pal = {rgb(154, 154, 154), rgb(168, 168, 168), rgb(182, 182, 182), rgb(196, 196, 196), rgb(210, 210, 210)};
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float v = fbm(x, y, 501) * 0.72F + hash(x, y, 502) * 0.28F;
            p[y * N + x] = step(pal, contrast(v, 1.9F));
         }
      }
      Random r = new Random(503L);
      for (int i = 0; i < 28; i++) {
         int bx = r.nextInt(N);
         int by = r.nextInt(N);
         boolean bright = (i & 1) == 0;
         int c1 = bright ? rgb(224, 224, 224) : rgb(142, 142, 142);
         int c2 = bright ? rgb(206, 206, 206) : rgb(156, 156, 156);
         p[by * N + bx] = c1;
         p[((by + 1) & 15) * N + bx] = c2;
      }
      for (int i = 0; i < p.length; i++) {
         p[i] = tintRgb(p[i], 0.45F, 0.84F, 0.30F);
      }
      return p;
   }

   private static int[] paintDirt() {
      int[] pal = {c(0x6C4C32), c(0x7B573B), c(0x886243), c(0x966D4B), c(0xA47956)};
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float v = fbm(x, y, 303) * 0.7F + hash(x, y, 304) * 0.3F;
            p[y * N + x] = step(pal, contrast(v, 2.0F));
         }
      }
      Random r = new Random(305L);
      for (int i = 0; i < 10; i++) {
         int px = r.nextInt(N);
         int py = r.nextInt(N);
         int pebble = i < 5 ? c(0xA88868) : c(0x5B3F28);
         p[py * N + px] = pebble;
         p[((py + 1) & 15) * N + px] = mul(p[((py + 1) & 15) * N + px], 0.85F);
      }
      return p;
   }

   private static int[] paintGrassSide(boolean snowy) {
      int[] p = paintDirt();
      for (int x = 0; x < N; x++) {
         int lip = 2 + (int)(vn(x * 0.5F, 0.5F, 8, 510) * 3.8F) + ((h32(x, 0, 511) & 1));
         // Drop shadow on dirt directly under the hanging grass fringe
         if (lip < N) {
            p[lip * N + x] = mul(p[lip * N + x], 0.74F);
         }
         for (int y = 0; y < lip && y < N; y++) {
            if (snowy) {
               int w = (y == lip - 1) ? c(0xD4E4F2) : c(0xF4FAFF);
               p[y * N + x] = w;
            } else {
               float v = fbm(x, y, 512) * 0.65F + hash(x, y, 513) * 0.35F;
               int g = 162 + (int)(v * 48.0F);
               if (y == 0) g += 14;
               else if (y == lip - 1) g -= 20;
               p[y * N + x] = tintRgb(rgb(g, g, g), 0.45F, 0.84F, 0.30F);
            }
         }
      }
      return p;
   }

   private static int[] paintStone(int seed) {
      int[] pal = {c(0x66666A), c(0x747478), c(0x808084), c(0x8C8C90), c(0x98989C)};
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float v = fbm(x * 2, y, seed) * 0.78F + hash(x, y, seed + 3) * 0.22F;
            p[y * N + x] = step(pal, contrast(v, 2.2F));
         }
      }
      Random r = new Random(seed + 9);
      for (int c = 0; c < 4; c++) {
         int cx = r.nextInt(N);
         int cy = r.nextInt(N);
         int len = 3 + r.nextInt(3);
         for (int i = 0; i < len; i++) {
            p[(cy & 15) * N + (cx & 15)] = pal[0];
            p[((cy - 1) & 15) * N + (cx & 15)] = pal[4];
            cx += r.nextBoolean() ? 1 : -1;
            if (r.nextInt(3) == 0) cy++;
         }
      }
      return p;
   }

   private static int[] paintCobble(int seed, int tr, int tg, int tb) {
      int M = 8;
      Random r = new Random(seed);
      float[] sx = new float[M];
      float[] sy = new float[M];
      int[] shade = new int[M];
      for (int i = 0; i < M; i++) {
         sx[i] = (i & 3) * 4.0F + 1.0F + r.nextFloat() * 2.2F;
         sy[i] = (i >> 2) * 8.0F + 1.2F + r.nextFloat() * 5.0F;
         shade[i] = -14 + r.nextInt(29);
      }
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float d1 = 999.0F, d2 = 999.0F;
            int best = 0;
            float bestDx = 0.0F, bestDy = 0.0F;
            for (int i = 0; i < M; i++) {
               float dx = x - sx[i];
               float dy = y - sy[i];
               if (dx > 8.0F) dx -= 16.0F;
               if (dx < -8.0F) dx += 16.0F;
               if (dy > 8.0F) dy -= 16.0F;
               if (dy < -8.0F) dy += 16.0F;
               float d = (float)Math.sqrt(dx * dx + dy * dy);
               if (d < d1) {
                  d2 = d1;
                  d1 = d;
                  best = i;
                  bestDx = dx;
                  bestDy = dy;
               } else if (d < d2) {
                  d2 = d;
               }
            }
            float edge = d2 - d1;
            int v;
            if (edge < 0.72F) {
               v = 68 + (int)((hash(x, y, seed + 5) - 0.5F) * 12.0F);
            } else {
               float light = (-bestDx * 0.6F - bestDy * 0.8F) / 3.4F;
               int grain = (int)((hash(x, y, seed + 11) - 0.5F) * 16.0F);
               v = 122 + shade[best] + (int)(light * 24.0F) + grain;
               v = v / 8 * 8;
            }
            p[y * N + x] = rgb(v + tr, v + tg, v + tb);
         }
      }
      return p;
   }

   private static int[] paintMossyCobble() {
      int[] p = paintCobble(202, 0, 0, 0);
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            int col = p[y * N + x];
            int lum = (col >> 16) & 255;
            float m = fbm(x, y, 1601);
            if ((lum < 86 && m > 0.36F) || m > 0.64F) {
               int g = 98 + (int)(hash(x, y, 1602) * 38.0F);
               p[y * N + x] = rgb(g * 58 / 100, g, g * 42 / 100);
            }
         }
      }
      return p;
   }

   private static int[] paintBedrock() {
      int[] pal = {c(0x222225), c(0x343438), c(0x4A4A50), c(0x606066), c(0x74747A)};
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float v = vn(x + 0.5F, y * 1.5F + 0.5F, 4, 110) * 0.5F
               + vn(x + 0.5F, y + 0.5F, 8, 111) * 0.35F
               + hash(x, y, 112) * 0.15F;
            p[y * N + x] = step(pal, contrast(v, 2.2F));
         }
      }
      return p;
   }

   private static int[] paintSand(int tr, int tg, int tb) {
      int[] pal = {
         rgb(206 + tr, 195 + tg, 142 + tb),
         rgb(216 + tr, 206 + tg, 152 + tb),
         rgb(225 + tr, 216 + tg, 162 + tb),
         rgb(234 + tr, 225 + tg, 172 + tb),
         rgb(242 + tr, 234 + tg, 184 + tb)
      };
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float wave = (float)Math.sin((y + vn(x * 0.5F, y * 0.5F, 8, 401) * 2.5F) * Math.PI / 4.0) * 0.18F;
            float v = fbm(x, y, 402) * 0.65F + hash(x, y, 403) * 0.35F + wave;
            p[y * N + x] = step(pal, contrast(v, 1.9F));
         }
      }
      return p;
   }

   private static int[] paintGravel() {
      int[][] tones = {
         {112, 108, 106}, {132, 126, 124}, {148, 140, 136},
         {96, 92, 90}, {138, 122, 112}, {120, 122, 128}
      };
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            int cellX = (x + (h32(x >> 1, y >> 1, 450) & 1)) >> 1;
            int cellY = (y + (h32(x >> 1, y >> 1, 451) & 1)) >> 1;
            int h = h32(cellX, cellY, 452) & 0x7FFFFFFF;
            int[] t = tones[h % tones.length];
            int corner = ((x & 1) == 0 && (y & 1) == 0) ? 12 : (((x & 1) == 1 && (y & 1) == 1) ? -14 : 0);
            int jit = (int)((hash(x, y, 453) - 0.5F) * 14.0F);
            p[y * N + x] = rgb(t[0] + corner + jit, t[1] + corner + jit, t[2] + corner + jit);
         }
      }
      return p;
   }

   private static int[] paintSandstoneTop(boolean red) {
      int[] pal = red
         ? new int[]{c(0x9E4E1E), c(0xAC5824), c(0xB8622A), c(0xC46C32), c(0xD0783C)}
         : new int[]{c(0xC8BC8E), c(0xD3C79A), c(0xDDD2A6), c(0xE6DCB2), c(0xEEE5BE)};
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float v = fbm(x, y, red ? 415 : 220) * 0.75F + hash(x, y, red ? 416 : 221) * 0.25F;
            int col = step(pal, contrast(v, 1.9F));
            if (x == 0 || y == 0) col = mul(col, 1.05F);
            else if (x == 15 || y == 15) col = mul(col, 0.93F);
            p[y * N + x] = col;
         }
      }
      return p;
   }

   private static int[] paintSandstoneSide(int tr, int tg, int tb) {
      int[] p = new int[N * N];
      int[] band = {234, 224, 198, 220, 214, 192, 226, 218, 208, 222, 196, 216, 224, 212, 198, 210};
      for (int y = 0; y < N; y++) {
         int b = band[y];
         for (int x = 0; x < N; x++) {
            int j = (int)((hash(x, y, 1401) - 0.5F) * 10.0F);
            int v = b + j;
            if (y < 3 && (x & 3) == 0) v -= 14;
            p[y * N + x] = rgb(v + tr, v * 95 / 100 + tg, v * 72 / 100 + tb);
         }
      }
      return p;
   }

   private static int[] paintStoneSlabTop() {
      int[] p = paintStone(115);
      for (int i = 0; i < N; i++) {
         p[i] = mul(p[i], 1.12F);
         p[i * N] = mul(p[i * N], 1.12F);
         p[15 * N + i] = mul(p[15 * N + i], 0.82F);
         p[i * N + 15] = mul(p[i * N + 15], 0.82F);
      }
      return p;
   }

   private static int[] paintStoneSlabSide() {
      int[] p = paintStoneSlabTop();
      for (int x = 0; x < N; x++) {
         p[7 * N + x] = mul(p[7 * N + x], 0.72F);
         p[8 * N + x] = mul(p[8 * N + x], 1.14F);
      }
      return p;
   }

   private static int[] paintLogSide(int dark, int mid, int light, int high, int seed) {
      int[] pal = {dark, mid, light, high};
      int[] p = new int[N * N];
      for (int x = 0; x < N; x++) {
         float colTone = vn(x * 0.5F, 0.5F, 8, seed);
         boolean groove = (x % 4 == 0) || (colTone < 0.25F);
         for (int y = 0; y < N; y++) {
            float streak = vn(x * 0.5F, y * 0.25F, 4, seed + 1) * 0.65F + hash(x, y, seed + 2) * 0.35F;
            float v = groove ? streak * 0.45F : 0.25F + streak * 0.72F;
            p[y * N + x] = step(pal, v);
         }
      }
      p[5 * N + 6] = dark;
      p[6 * N + 6] = dark;
      p[6 * N + 7] = mid;
      return p;
   }

   private static int[] paintLogTop(int bark, int barkLight, int ringA, int ringB, int core, int seed) {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            int edge = Math.min(Math.min(x, 15 - x), Math.min(y, 15 - y));
            if (edge == 0) {
               p[y * N + x] = ((x + y) & 1) == 0 ? bark : barkLight;
            } else if (edge == 1) {
               p[y * N + x] = barkLight;
            } else {
               float dx = x - 7.5F;
               float dy = y - 7.5F;
               float r = (float)Math.sqrt(dx * dx + dy * dy);
               int ring = (int)Math.round(r);
               int base = r < 1.6F ? bark : ((ring & 1) == 0 ? ringA : ringB);
               if (edge == 2) base = core;
               int jit = (int)((hash(x, y, seed) - 0.5F) * 10.0F);
               p[y * N + x] = add(base, jit);
            }
         }
      }
      return p;
   }

   private static int[] paintPlanks(int base, int seed) {
      int c0 = mul(base, 0.72F);
      int c1 = mul(base, 0.88F);
      int c2 = base;
      int c3 = mul(base, 1.09F);
      int[] pal = {c1, c2, c3};
      int[] p = new int[N * N];
      int[] joints = {7, 13, 4, 10};
      for (int y = 0; y < N; y++) {
         int plank = y >> 2;
         int py = y & 3;
         int jx = joints[plank];
         for (int x = 0; x < N; x++) {
            if (py == 3 || x == jx) {
               p[y * N + x] = c0;
            } else {
               float g = vn((x + plank * 5) * 0.25F, plank + py * 0.5F, 4, seed) * 0.75F
                  + hash(x, y, seed + 2) * 0.25F;
               int col = step(pal, contrast(g, 1.8F));
               if (py == 0) col = mul(col, 1.05F);
               p[y * N + x] = col;
            }
         }
      }
      p[N + 2] = c0;
      p[5 * N + 9] = c0;
      p[9 * N + 14] = c0;
      p[13 * N + 5] = c0;
      return p;
   }

   private static int[] paintOre(int main, int shade, int high, int seed, int n, boolean small) {
      int[] p = paintStone(101);
      Random r = new Random(seed);
      int[][] shapeBig = {{0, 0}, {1, 0}, {0, 1}, {1, 1}, {2, 1}};
      int[][] shapeSmall = {{0, 0}, {1, 0}, {0, 1}};
      int[][] shape = small ? shapeSmall : shapeBig;
      for (int i = 0; i < n; i++) {
         int bx = 2 + r.nextInt(11);
         int by = 2 + r.nextInt(11);
         for (int k = 0; k < shape.length; k++) {
            int x = (bx + shape[k][0]) & 15;
            int y = (by + shape[k][1]) & 15;
            int col = k == 0 ? high : (k == shape.length - 1 ? shade : main);
            p[y * N + x] = col;
            int sx = (x + 1) & 15;
            int sy = (y + 1) & 15;
            if (p[sy * N + sx] != main && p[sy * N + sx] != high && p[sy * N + sx] != shade) {
               p[sy * N + sx] = c(0x525256);
            }
         }
      }
      return p;
   }

   private static int[] paintBricks() {
      int[] p = new int[N * N];
      int mortar = c(0xB5AAA0);
      int mortarShadow = c(0x988D82);
      for (int y = 0; y < N; y++) {
         int row = y / 4;
         int ry = y % 4;
         for (int x = 0; x < N; x++) {
            int xs = (x + (row & 1) * 4) & 15;
            int bx = xs / 8;
            int rx = xs % 8;
            if (ry == 0 || rx == 0) {
               p[y * N + x] = ry == 0 && rx > 0 ? mortar : mortarShadow;
               continue;
            }
            int id = row * 2 + bx;
            int baseR = 152 + (int)((hash(id, row, 200) - 0.5F) * 22);
            int baseG = 72 + (int)((hash(id, row, 201) - 0.5F) * 14);
            int baseB = 56 + (int)((hash(id, row, 202) - 0.5F) * 12);
            int d = (int)((hash(x, y, 203) - 0.5F) * 10);
            if (ry == 1) d += 12;
            else if (ry == 3) d -= 10;
            if (rx == 1) d += 7;
            else if (rx == 7) d -= 7;
            p[y * N + x] = rgb(baseR + d, baseG + d * 2 / 3, baseB + d / 2);
         }
      }
      return p;
   }

   private static int[] paintStoneBrick(boolean cracked, boolean mossy) {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         int row = y >> 3;
         int by = y & 7;
         int off = (row & 1) * 8;
         for (int x = 0; x < N; x++) {
            int bx = (x + off) & 15;
            if (by == 7 || bx == 15) {
               p[y * N + x] = c(0x46464A);
            } else {
               int v = 126 + (int)((fbm(x, y, 1501) - 0.5F) * 22.0F);
               if (by == 0 || bx == 0) v += 16;
               else if (by == 6 || bx == 14) v -= 14;
               v = v / 6 * 6;
               p[y * N + x] = rgb(v, v, v + 2);
            }
         }
      }
      if (cracked) {
         int[][] cr = {{4, 2}, {5, 3}, {5, 4}, {6, 5}, {11, 10}, {12, 11}, {11, 12}, {11, 13}};
         for (int[] k : cr) {
            p[k[1] * N + k[0]] = c(0x3A3A3E);
         }
      }
      if (mossy) {
         for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
               if (fbm(x, y, 1509) > 0.58F) {
                  int g = 104 + (int)(hash(x, y, 1510) * 34.0F);
                  p[y * N + x] = rgb(g * 56 / 100, g, g * 40 / 100);
               }
            }
         }
      }
      return p;
   }

   private static int[] paintGlass() {
      int[] p = new int[N * N];
      int edge = c(0xB8E0EE);
      int dark = c(0x6FA0B4);
      int hi = c(0xEAF8FF);
      for (int i = 0; i < N; i++) {
         p[i] = i < 15 ? edge : dark;
         p[i * N] = i < 15 ? edge : dark;
         p[15 * N + i] = dark;
         p[i * N + 15] = dark;
      }
      p[N + 1] = hi;
      for (int k = 0; k < 4; k++) {
         p[(3 + k) * N + (5 - k)] = hi;
         if (k < 3) {
            p[(10 + k) * N + (12 - k)] = edge;
         }
      }
      return p;
   }

   private static int[] paintGlowstone() {
      int[] pal = {c(0x6B4214), c(0x9E6820), c(0xCF9634), c(0xF5D062), c(0xFFF6B8)};
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float v = vn(x * 0.5F, y * 0.5F, 8, 1701) * 0.65F + hash(x, y, 1702) * 0.35F;
            p[y * N + x] = step(pal, contrast(v, 2.2F));
         }
      }
      return p;
   }

   private static int[] paintObsidian() {
      int[] pal = {c(0x100C1C), c(0x181228), c(0x221A38), c(0x2E234A), c(0x3D2E5E)};
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float v = fbm(x, y, 1801) * 0.7F + hash(x, y, 1802) * 0.3F;
            p[y * N + x] = step(pal, contrast(v, 2.0F));
         }
      }
      return p;
   }

   private static int[] paintCraftTop() {
      int[] p = paintPlanks(c(0xA6834E), 701);
      int border = c(0x523B20);
      int grid = c(0x3F2C16);
      for (int y = 2; y <= 13; y++) {
         for (int x = 2; x <= 13; x++) {
            if (x == 2 || x == 13 || y == 2 || y == 13) {
               p[y * N + x] = border;
            } else if (x == 6 || x == 9 || y == 6 || y == 9) {
               p[y * N + x] = grid;
            } else {
               int jit = (int)((hash(x, y, 1901) - 0.5F) * 10.0F);
               p[y * N + x] = add(c(0xB8915A), jit);
            }
         }
      }
      return p;
   }

   private static int[] paintCraftSide() {
      int[] p = paintPlanks(c(0xA6834E), 701);
      for (int y = 0; y < 4; y++) {
         for (int x = 0; x < N; x++) {
            p[y * N + x] = y == 3 ? c(0x3F2C16) : ((x & 3) == 0 ? c(0x6B4E2B) : c(0x8A673B));
         }
      }
      for (int y = 5; y <= 12; y++) {
         p[y * N + 3] = c(0x4E381E);
         p[y * N + 12] = c(0x4E381E);
      }
      // Saw & Hammer on tool rack
      for (int y = 6; y <= 10; y++) {
         p[y * N + 6] = c(0xC8CDD2);
         p[y * N + 7] = y < 8 ? c(0x989EA4) : c(0x7A5830);
      }
      for (int y = 6; y <= 11; y++) {
         p[y * N + 10] = y < 8 ? c(0xB8BDC2) : c(0x7A5830);
      }
      p[6 * N + 9] = c(0xC8CDD2);
      p[6 * N + 11] = c(0x888E94);
      return p;
   }

   private static int[] paintFurnaceFront(boolean on) {
      int[] p = paintStoneBrick(false, false);
      for (int y = 3; y <= 7; y++) {
         for (int x = 4; x <= 11; x++) {
            if (y == 3 && (x == 4 || x == 11)) continue;
            p[y * N + x] = c(0x1C1C20);
         }
      }
      for (int y = 10; y <= 13; y++) {
         for (int x = 4; x <= 11; x++) {
            if (on && y >= 11) {
               p[y * N + x] = ((x + y) & 1) == 0 ? c(0xFF9E22) : c(0xFFE055);
            } else {
               p[y * N + x] = (x & 1) == 0 ? c(0x28282C) : c(0x525258);
            }
         }
      }
      return p;
   }

   private static int[] paintChestWood(float k) {
      int[] p = new int[N * N];
      int rim = c(0x3B2914);
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            if (x == 0 || x == 15 || y == 0 || y == 15) {
               p[y * N + x] = rim;
            } else {
               float g = vn(x * 0.25F, y * 0.5F, 4, 2101) * 0.7F + hash(x, y, 2102) * 0.3F;
               int base = step(new int[]{c(0x8F632E), c(0xA47438), c(0xB88442), c(0xC9934D)}, g);
               p[y * N + x] = mul(base, k);
            }
         }
      }
      return p;
   }

   private static int[] paintChestTop() {
      return paintChestWood(1.04F);
   }

   private static int[] paintChestSide(boolean front) {
      int[] p = paintChestWood(0.96F);
      for (int x = 0; x < N; x++) {
         p[5 * N + x] = c(0x2E1F0E);
         p[6 * N + x] = c(0x52391C);
      }
      if (front) {
         for (int y = 4; y <= 8; y++) {
            for (int x = 6; x <= 9; x++) {
               p[y * N + x] = (x == 6 || y == 4) ? c(0xFFF078) : c(0xD8A828);
            }
         }
      }
      return p;
   }

   private static int[] paintBookshelf() {
      int[] p = paintPlanks(c(0xA6834E), 701);
      int[] spines = {c(0x9E2822), c(0x285698), c(0x2E7A38), c(0x884898), c(0xB87824), c(0x267878), c(0x7A3820)};
      for (int shelf = 0; shelf < 2; shelf++) {
         int y0 = shelf == 0 ? 2 : 9;
         for (int y = y0; y < y0 + 5; y++) {
            for (int x = 1; x < 15; x++) {
               p[y * N + x] = c(0x22160C);
            }
         }
         int x = 1;
         int b = shelf * 5;
         while (x < 15) {
            int w = 2 + (b & 1);
            if (x + w > 15) w = 15 - x;
            int top = y0 + (b % 3 == 0 ? 1 : 0);
            int col = spines[b % spines.length];
            for (int y = top; y < y0 + 5; y++) {
               for (int dx = 0; dx < w; dx++) {
                  int sc = dx == 0 ? mul(col, 1.12F) : (dx == w - 1 ? mul(col, 0.78F) : col);
                  if (y == top + 1) sc = c(0xE0C058);
                  p[y * N + x + dx] = sc;
               }
            }
            x += w;
            b++;
         }
      }
      return p;
   }

   private static int[] paintTntSide() {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            int stick = x & 3;
            if (y >= 5 && y <= 10) {
               int w = (y == 5 || y == 10) ? c(0xC8C4BC) : c(0xF0ECE4);
               p[y * N + x] = stick == 3 ? mul(w, 0.92F) : w;
            } else {
               int r = stick == 0 ? c(0xE03C28) : (stick == 1 ? c(0xC82E1C) : (stick == 2 ? c(0xB22414) : c(0x7E180C)));
               if (y == 0 || y == 15) r = mul(r, 0.78F);
               p[y * N + x] = r;
            }
         }
      }
      int ink = c(0x1E1E22);
      int[][] letters = {
         {2, 6}, {3, 6}, {4, 6}, {3, 7}, {3, 8}, {3, 9},
         {6, 6}, {6, 7}, {6, 8}, {6, 9}, {7, 7}, {8, 8}, {9, 6}, {9, 7}, {9, 8}, {9, 9},
         {11, 6}, {12, 6}, {13, 6}, {12, 7}, {12, 8}, {12, 9}
      };
      for (int[] l : letters) {
         p[l[1] * N + l[0]] = ink;
      }
      return p;
   }

   private static int[] paintTntTop(boolean top) {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            int cx = (x & 3) - 1;
            int cy = (y & 3) - 1;
            boolean border = (x & 3) == 3 || (y & 3) == 3;
            p[y * N + x] = border ? c(0x7A1A10) : ((cx == 0 && cy == 0) ? (top ? c(0x484440) : c(0xB42818)) : c(0xD63824));
         }
      }
      if (top) {
         for (int y = 6; y <= 9; y++) {
            for (int x = 6; x <= 9; x++) {
               p[y * N + x] = ((x + y) & 1) == 0 ? c(0xDCD8CE) : c(0x8E8A80);
            }
         }
      }
      return p;
   }

   private static int[] paintTorch() {
      int[] p = new int[N * N];
      for (int y = 6; y < 16; y++) {
         int cL = y == 6 ? c(0x52463A) : (y == 7 ? c(0xA67C46) : c(0x8C6230));
         int cR = y == 6 ? c(0x383028) : (y == 7 ? c(0x7C582A) : c(0x664620));
         p[y * N + 7] = add(cL, (int)((hash(7, y, 360) - 0.5F) * 10));
         p[y * N + 8] = add(cR, (int)((hash(8, y, 360) - 0.5F) * 10));
      }
      p[5 * N + 7] = c(0xFF9E22);
      p[5 * N + 8] = c(0xFF8818);
      p[4 * N + 7] = c(0xFFF6B0);
      p[4 * N + 8] = c(0xFFD658);
      p[3 * N + 7] = c(0xFFE478);
      p[3 * N + 8] = c(0xFFA828);
      p[2 * N + 7] = c(0xFF8818);
      return p;
   }

   private static int[] paintLadder() {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         p[y * N + 2] = c(0x9E7844);
         p[y * N + 3] = c(0x78582E);
         p[y * N + 12] = c(0x9E7844);
         p[y * N + 13] = c(0x78582E);
      }
      int[] rungs = {1, 5, 9, 13};
      for (int ry : rungs) {
         for (int x = 1; x <= 14; x++) {
            p[ry * N + x] = c(0xB89056);
            p[(ry + 1) * N + x] = c(0x826034);
         }
      }
      return p;
   }

   private static int[] paintBedTop() {
      int[] p = paintWool(0xB82822, 1014);
      for (int y = 0; y < 5; y++) {
         for (int x = 1; x < 15; x++) {
            p[y * N + x] = (y == 4) ? c(0xC8C4BA) : c(0xF2EFE8);
         }
      }
      for (int x = 0; x < N; x++) {
         p[5 * N + x] = c(0x7A1814);
      }
      return p;
   }

   private static int[] paintBars() {
      int[] p = new int[N * N];
      int[] cols = {2, 7, 12};
      for (int cx : cols) {
         for (int y = 0; y < N; y++) {
            p[y * N + cx] = c(0xAEB4BC);
            p[y * N + cx + 1] = c(0x6E747C);
         }
      }
      for (int x = 0; x < N; x++) {
         p[3 * N + x] = c(0x90969E);
         p[11 * N + x] = c(0x90969E);
      }
      return p;
   }

   private static int[] paintWaterFrame(int f) {
      int[] p = new int[N * N];
      double t = f * Math.PI * 2.0 / 16.0;
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            double w1 = Math.sin((x + y * 0.6) * Math.PI / 4.0 + t);
            double w2 = Math.cos((y - x * 0.5) * Math.PI / 4.0 - t);
            float v = (float)((w1 + w2) * 0.25 + 0.5);
            int band = (int)(v * 4.0F);
            int r = 24 + band * 8;
            int g = 92 + band * 14;
            int b = 198 + band * 14;
            p[y * N + x] = argb(204, r, g, b);
         }
      }
      return p;
   }

   private static int[] paintLavaFrame(int f) {
      int[] pal = {c(0x8E1E08), c(0xC8380C), c(0xEA6414), c(0xFCA422), c(0xFFF068)};
      int[] p = new int[N * N];
      double t = f * Math.PI * 2.0 / 16.0;
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float sx = (float)(x + Math.sin(t + y * 0.4) * 1.5);
            float sy = (float)(y + Math.cos(t + x * 0.4) * 1.5);
            float v = vn(sx * 0.5F, sy * 0.5F, 8, 2301);
            p[y * N + x] = step(pal, contrast(v, 1.9F));
         }
      }
      return p;
   }

   private static int[] paintSnow() {
      int[] pal = {c(0xDCE8F2), c(0xE6F0F8), c(0xEFF7FC), c(0xF8FCFF), c(0xFFFFFF)};
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float v = fbm(x, y, 901) * 0.7F + hash(x, y, 902) * 0.3F;
            p[y * N + x] = step(pal, v);
         }
      }
      return p;
   }

   private static int[] paintIce(boolean packed) {
      int[] pal = packed
         ? new int[]{c(0x6E98E0), c(0x7EA8EC), c(0x90B8F6), c(0xA4C8FF)}
         : new int[]{c(0x74A8F8), c(0x86B6FF), c(0x98C4FF), c(0xB0D4FF)};
      int[] p = paintMineralStone(packed ? 427 : 426, pal);
      for (int k = 2; k < 13; k++) {
         p[k * N + (14 - k)] = c(0xC8E2FF);
      }
      if (!packed) {
         for (int i = 0; i < p.length; i++) {
            p[i] = (0xDC << 24) | (p[i] & 0xFFFFFF);
         }
      }
      return p;
   }

   private static int[] paintTallGrass() {
      int[] p = new int[N * N];
      int[] cols = {1, 3, 5, 7, 8, 10, 12, 14};
      int[] tops = {5, 2, 4, 1, 3, 2, 5, 4};
      int[] sway = {-1, 0, 1, -1, 1, 0, -1, 1};
      for (int i = 0; i < cols.length; i++) {
         int x0 = cols[i];
         int top = tops[i];
         boolean front = (i & 1) == 1;
         for (int y = top; y < N; y++) {
            float k = (15 - y) / (float)(15 - top);
            int x = Math.max(0, Math.min(15, x0 + (k > 0.65F ? sway[i] : 0)));
            int g = (front ? 172 : 148) + (int)(k * 42.0F) + (int)((hash(x, y, 250 + i) - 0.5F) * 12.0F);
            p[y * N + x] = tintRgb(rgb(g, g, g), 0.44F, 0.84F, 0.30F);
            if (y >= top + 2 && x + 1 < N && (front || y >= 11)) {
               int g2 = g - 14;
               if (p[y * N + x + 1] == 0) {
                  p[y * N + x + 1] = tintRgb(rgb(g2, g2, g2), 0.44F, 0.84F, 0.30F);
               }
            }
         }
      }
      return p;
   }

   private static int[] paintFlower(boolean poppy) {
      int[] p = new int[N * N];
      for (int y = 8; y < 16; y++) {
         int sx = y < 11 ? 7 : 8;
         p[y * N + sx] = y % 2 == 0 ? c(0x3A8828) : c(0x2E701E);
      }
      p[12 * N + 6] = c(0x3A8828);
      p[11 * N + 5] = c(0x4CA036);
      p[13 * N + 9] = c(0x3A8828);
      p[12 * N + 10] = c(0x4CA036);
      for (int y = 3; y <= 8; y++) {
         for (int x = 4; x <= 11; x++) {
            float dx = x - 7.5F;
            float dy = y - 5.5F;
            float d = dx * dx + dy * dy * 1.2F;
            if (d <= 10.5F) {
               if (poppy) {
                  p[y * N + x] = d < 2.2F ? c(0x281812) : (dy < -0.5F ? c(0xF2382C) : (dy < 1.0F ? c(0xD42018) : c(0x9E1410)));
               } else {
                  p[y * N + x] = d < 2.0F ? c(0xF69E14) : (dy < -0.5F ? c(0xFFF048) : (dy < 1.0F ? c(0xF8D220) : c(0xD4A410)));
               }
            }
         }
      }
      return p;
   }

   private static int[] paintCactusSide() {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            int rib = Math.min(Math.min(Math.abs(x - 2), Math.abs(x - 6)), Math.min(Math.abs(x - 10), Math.abs(x - 14)));
            float v = fbm(x, y, 280) * 0.16F - 0.08F;
            int col = rib == 0 ? c(0x5C943B) : (rib == 1 ? c(0x46762A) : c(0x2E541A));
            p[y * N + x] = mul(col, 1.0F + v);
         }
      }
      int spine = c(0xEAE2C4);
      int[][] sp = {{1, 3}, {14, 5}, {5, 7}, {10, 11}, {1, 13}, {14, 14}, {6, 2}, {9, 6}};
      for (int[] s : sp) {
         p[s[1] * N + s[0]] = spine;
      }
      return p;
   }

   private static int[] paintCactusTop() {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float dx = x - 7.5F;
            float dy = y - 7.5F;
            float r = (float)Math.hypot(dx, dy);
            int col = r < 2.2F ? c(0x88B45E) : (r < 6.5F ? c(0x588C38) : c(0x366020));
            if ((x == 7 || x == 8 || y == 7 || y == 8) && r < 7.0F) {
               col = mul(col, 1.12F);
            }
            p[y * N + x] = mul(col, 0.95F + 0.1F * hash(x, y, 290));
         }
      }
      return p;
   }

   private static int[] paintMetalBlock(int mid, int dark, int deep, int hi, int seed) {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            if (x == 0 || y == 0) {
               p[y * N + x] = hi;
            } else if (x == 15 || y == 15) {
               p[y * N + x] = deep;
            } else if (x == 1 || y == 1) {
               p[y * N + x] = mix(mid, hi, 0.55F);
            } else if (x == 14 || y == 14) {
               p[y * N + x] = dark;
            } else {
               float sheen = ((x + y * 2) & 7) / 7.0F * 0.10F - 0.05F + (hash(x, y, seed) - 0.5F) * 0.06F;
               p[y * N + x] = mul(mid, 1.0F + sheen);
            }
         }
      }
      return p;
   }

   private static int[] paintGemBlock(int mid, int dark, int deep, int hi, int seed) {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            int d = Math.max(Math.abs(x - 7), Math.abs(y - 7));
            int col;
            if (x == 0 || y == 0) col = hi;
            else if (x == 15 || y == 15) col = deep;
            else if (d <= 2) col = (x + y < 14) ? hi : mid;
            else if (d <= 5) col = (x < y) ? mix(mid, hi, 0.35F) : dark;
            else col = (x + y < 15) ? mid : dark;
            p[y * N + x] = mul(col, 0.96F + 0.08F * hash(x, y, seed));
         }
      }
      return p;
   }

   private static int[] paintCoalBlock() {
      int[] pal = {c(0x101014), c(0x18181E), c(0x222228), c(0x2C2C34), c(0x383842)};
      return paintMineralStone(241, pal);
   }

   private static int[] paintQuartzBlock() {
      int[] pal = {c(0xDCD6CE), c(0xE6E1DA), c(0xEFECE6), c(0xF7F5F0)};
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            int col = step(pal, contrast(fbm(x, y, 306), 1.8F));
            if (x == 0 || y == 0) col = c(0xFBF9F5);
            else if (x == 15 || y == 15) col = c(0xCECAC0);
            p[y * N + x] = col;
         }
      }
      return p;
   }

   private static int[] paintSeaLantern() {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            int d = Math.max(Math.abs(x - 7), Math.abs(y - 7));
            int col = d <= 3 ? c(0xF0FFFF) : (d <= 5 ? c(0xC2EDE6) : c(0x5E9E96));
            p[y * N + x] = mul(col, 0.96F + 0.08F * hash(x, y, 387));
         }
      }
      return p;
   }

   private static int[] paintSlimeBlock() {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            boolean core = x >= 4 && x <= 11 && y >= 4 && y <= 11;
            int col = core ? c(0x62BA48) : c(0x7CD860);
            p[y * N + x] = (0xDC << 24) | (mul(col, 0.96F + 0.08F * hash(x, y, 389)) & 0xFFFFFF);
         }
      }
      return p;
   }

   private static int[] paintNetherrack() {
      int[] pal = {c(0x461818), c(0x5A2020), c(0x6E2A2A), c(0x823636), c(0x964444)};
      return paintMineralStone(300, pal);
   }

   private static int[] paintSoulSand() {
      int[] pal = {c(0x32241C), c(0x423026), c(0x523C30), c(0x644A3C), c(0x745848)};
      return paintMineralStone(301, pal);
   }

   private static int[] paintNetherBricks() {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         int row = y / 4;
         int ry = y % 4;
         for (int x = 0; x < N; x++) {
            int xs = (x + (row & 1) * 4) & 15;
            int rx = xs % 8;
            if (ry == 0 || rx == 0) {
               p[y * N + x] = c(0x1E0E12);
               continue;
            }
            int base = (ry == 1 || rx == 1) ? c(0x46242A) : (ry == 3 ? c(0x2A1418) : c(0x381C22));
            p[y * N + x] = add(base, (int)((hash(x, y, 303) - 0.5F) * 8));
         }
      }
      return p;
   }

   private static int[] paintEndStone() {
      int[] pal = {c(0xB8B47E), c(0xCCC890), c(0xDEDAA4), c(0xEAE6B4), c(0xF4F0C4)};
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float v = 1.0F - fbm(x, y, 308);
            p[y * N + x] = step(pal, contrast(v, 2.2F));
         }
      }
      return p;
   }

   private static int[] paintPurpur() {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            boolean seam = (x & 7) == 7 || (y & 7) == 7;
            int base = seam ? c(0x7E527E) : c(0xA878A8);
            p[y * N + x] = add(base, (int)((hash(x, y, 512) - 0.5F) * 10));
         }
      }
      return p;
   }

   private static int[] paintMineralStone(int seed, int[] pal) {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float v = fbm(x, y, seed) * 0.65F + hash(x, y, seed + 7) * 0.35F;
            p[y * N + x] = step(pal, contrast(v, 2.2F));
         }
      }
      return p;
   }

   private static int[] paintWool(int hex, int seed) {
      int base = c(hex);
      int[] pal = {mul(base, 0.84F), mul(base, 0.94F), base, mul(base, 1.08F)};
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float weave = (((x + (y & 1)) & 1) == 0 ? 0.14F : -0.14F);
            float v = fbm(x, y, seed) * 0.65F + hash(x, y, seed + 1) * 0.35F + weave;
            p[y * N + x] = step(pal, v);
         }
      }
      return p;
   }

   private static int[] paintConcrete(int hex, int seed) {
      int base = c(hex);
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float k = 0.97F + 0.06F * fbm(x, y, seed);
            p[y * N + x] = mul(base, k);
         }
      }
      return p;
   }

   private static void paintCrackStages(int[] atlas) {
      Random rng = new Random(555L);
      boolean[] mask = new boolean[256];
      for (int s = 0; s < 10; s++) {
         int branches = 2 + s;
         for (int b = 0; b < branches; b++) {
            int cx = (s == 0 && b == 0) ? 8 : rng.nextInt(16);
            int cy = (s == 0 && b == 0) ? 8 : rng.nextInt(16);
            int len = 5 + rng.nextInt(6);
            int dx = rng.nextInt(3) - 1;
            int dy = rng.nextInt(3) - 1;
            if (dx == 0 && dy == 0) dx = 1;
            for (int i = 0; i < len; i++) {
               if (cx >= 0 && cx < 16 && cy >= 0 && cy < 16) {
                  mask[cy * 16 + cx] = true;
               }
               if (rng.nextInt(3) == 0) dx = Math.max(-1, Math.min(1, dx + rng.nextInt(3) - 1));
               if (rng.nextInt(3) == 0) dy = Math.max(-1, Math.min(1, dy + rng.nextInt(3) - 1));
               cx += dx;
               cy += dy;
            }
         }
         int[] tile = new int[256];
         for (int i = 0; i < 256; i++) {
            if (mask[i]) tile[i] = 0xBE141418;
         }
         blitTile(atlas, T_CRACK_0 + s, tile);
      }
   }

   // ====================================================================
   //  TexExtra + TexPaint319 Tool & Item Sprite Painters
   // ====================================================================
   private static final class Cv {
      final int[] p = new int[256];

      void set(int x, int y, int rgb) {
         if (x >= 0 && x < 16 && y >= 0 && y < 16) {
            this.p[y * 16 + x] = 0xFF000000 | (rgb & 0xFFFFFF);
         }
      }

      void line(int x0, int y0, int x1, int y1, int rgb) {
         int dx = Math.abs(x1 - x0);
         int dy = Math.abs(y1 - y0);
         int sx = x0 < x1 ? 1 : -1;
         int sy = y0 < y1 ? 1 : -1;
         int err = dx - dy;
         while (true) {
            set(x0, y0, rgb);
            if (x0 == x1 && y0 == y1) return;
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x0 += sx; }
            if (e2 < dx) { err += dx; y0 += sy; }
         }
      }

      void rect(int x, int y, int w, int h, int rgb) {
         for (int dy = 0; dy < h; dy++) {
            for (int dx = 0; dx < w; dx++) {
               set(x + dx, y + dy, rgb);
            }
         }
      }

      void ell(double cx, double cy, double rx, double ry, int rgb) {
         for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
               double nx = (x + 0.5 - cx) / rx;
               double ny = (y + 0.5 - cy) / ry;
               if (nx * nx + ny * ny <= 1.0) {
                  set(x, y, rgb);
               }
            }
         }
      }

      void shade() {
         int[] src = this.p.clone();
         for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
               if ((src[y * 16 + x] >>> 24) != 0) {
                  int col = src[y * 16 + x];
                  boolean rOpen = x == 15 || (src[y * 16 + x + 1] >>> 24) == 0;
                  boolean bOpen = y == 15 || (src[(y + 1) * 16 + x] >>> 24) == 0;
                  boolean lOpen = x == 0 || (src[y * 16 + x - 1] >>> 24) == 0;
                  boolean tOpen = y == 0 || (src[(y - 1) * 16 + x] >>> 24) == 0;
                  if (rOpen || bOpen) {
                     this.p[y * 16 + x] = mul(col, 0.62F);
                  } else if (lOpen || tOpen) {
                     this.p[y * 16 + x] = mul(col, 1.18F);
                  }
               }
            }
         }
      }
   }

   private static int[] bevelSprite(int[] src, int seedTile) {
      int[] out = src.clone();
      for (int y = 1; y < 15; y++) {
         for (int x = 1; x < 15; x++) {
            int p = src[y * 16 + x];
            if ((p >>> 24) < 128) continue;
            boolean topOpen = (src[(y - 1) * 16 + x] >>> 24) < 128;
            boolean leftOpen = (src[y * 16 + (x - 1)] >>> 24) < 128;
            boolean botOpen = (src[(y + 1) * 16 + x] >>> 24) < 128;
            boolean rightOpen = (src[y * 16 + (x + 1)] >>> 24) < 128;
            if (topOpen || leftOpen || botOpen || rightOpen) {
               if (botOpen || rightOpen) {
                  out[y * 16 + x] = mul(p, 0.90F);
               }
            } else {
               boolean tlBorder = (src[(y - 2 >= 0 ? y - 2 : 0) * 16 + x] >>> 24) < 128
                  || (src[y * 16 + (x - 2 >= 0 ? x - 2 : 0)] >>> 24) < 128;
               boolean brBorder = (src[(y + 2 < 16 ? y + 2 : 15) * 16 + x] >>> 24) < 128
                  || (src[y * 16 + (x + 2 < 16 ? x + 2 : 15)] >>> 24) < 128;
               float k = 1.0F + (hash(x, y, seedTile + 900) - 0.5F) * 0.06F;
               if (tlBorder && !brBorder) k += 0.10F;
               else if (brBorder && !tlBorder) k -= 0.08F;
               out[y * 16 + x] = mul(p, k);
            }
         }
      }
      return out;
   }

   // toolClass: 1=Pickaxe, 2=Axe, 3=Shovel, 4=Sword
   private static int[] paintToolSprite(int toolClass, int tierHex) {
      Cv cv = new Cv();
      int base = c(tierHex);
      int dark = mul(base, 0.70F);
      int hi = mul(base, 1.25F);
      switch (toolClass) {
         case 1: { // Pickaxe
            cv.line(2, 14, 11, 5, 0x96733F);
            cv.line(3, 14, 12, 5, 0x5E4526);
            for (int i = -6; i <= 6; i++) {
               double a = i / 6.0 * 1.15;
               double cx = 11.5 - 6.5 * Math.cos(Math.PI / 4);
               double cy = 4.5 + 6.5 * Math.sin(Math.PI / 4);
               double ux = Math.cos(Math.PI / 4), uy = -Math.sin(Math.PI / 4);
               double vx = Math.sin(Math.PI / 4), vy = Math.cos(Math.PI / 4);
               for (double dr = 0.0; dr <= 1.6; dr += 0.5) {
                  double rad = 6.5 - dr;
                  int px = (int)Math.round(cx + rad * (Math.cos(a) * ux + Math.sin(a) * vx));
                  int py = (int)Math.round(cy + rad * (Math.cos(a) * uy + Math.sin(a) * vy));
                  cv.set(px, py, dr < 0.6 ? hi : (dr < 1.2 ? base : dark));
               }
            }
            break;
         }
         case 2: { // Axe
            cv.line(2, 14, 11, 5, 0x96733F);
            cv.line(3, 14, 12, 5, 0x5E4526);
            String[] mask = {"..HHH.", ".HHHHH", "HHHHHD", "HHHHDD", ".HHDD.", "..DD.."};
            for (int r = 0; r < mask.length; r++) {
               for (int col = 0; col < 6; col++) {
                  char ch = mask[r].charAt(col);
                  if (ch == 'H') cv.set(7 + col, 1 + r, (col + r) % 3 == 0 ? hi : base);
                  else if (ch == 'D') cv.set(7 + col, 1 + r, dark);
               }
            }
            break;
         }
         case 3: { // Shovel
            cv.line(2, 14, 10, 6, 0x96733F);
            cv.line(3, 14, 11, 6, 0x5E4526);
            cv.ell(11.5, 4.5, 3.0, 3.6, base);
            cv.ell(10.8, 3.8, 1.4, 1.6, hi);
            cv.set(13, 6, dark);
            cv.set(12, 7, dark);
            break;
         }
         case 4:
         default: { // Sword
            cv.line(3, 12, 1, 14, 0x96733F);
            cv.line(4, 12, 2, 14, 0x5E4526);
            cv.line(2, 9, 6, 13, 0x6B5430);
            cv.line(3, 9, 7, 13, 0x4A3A22);
            cv.line(5, 10, 13, 2, base);
            cv.line(6, 10, 14, 2, base);
            cv.line(5, 9, 13, 1, hi);
            cv.line(7, 10, 14, 3, dark);
            cv.set(14, 1, hi);
            break;
         }
      }
      cv.shade();
      return cv.p;
   }

   private static int[] paintBowSprite() {
      Cv cv = new Cv();
      for (int y = 1; y < 15; y++) {
         double ny = (y - 7.5) / 7.5;
         int x = (int)Math.round(11.0 - 6.0 * ny * ny);
         cv.set(x, y, 0x8F6B38);
         cv.set(x + 1, y, 0x5E4526);
      }
      cv.line(12, 2, 12, 13, 0xE8E8EC);
      cv.shade();
      return cv.p;
   }

   private static int[] paintArrowSprite() {
      Cv cv = new Cv();
      cv.line(3, 13, 12, 4, 0x96733F);
      cv.line(12, 4, 14, 2, 0xD8DCE2);
      cv.rect(12, 2, 3, 3, 0xE4E8EE);
      cv.line(2, 12, 4, 10, 0xF0F0F4);
      cv.line(3, 14, 5, 12, 0xF0F0F4);
      cv.shade();
      return cv.p;
   }

   private static int[] paintAppleSprite(int bodyHex, int hiHex) {
      Cv cv = new Cv();
      cv.ell(7.5, 9.0, 4.8, 4.6, bodyHex);
      cv.ell(6.2, 7.8, 2.0, 2.0, hiHex);
      cv.line(8, 4, 9, 2, 0x6B4B2A);
      cv.rect(9, 2, 3, 2, 0x3E9A2B);
      cv.shade();
      return cv.p;
   }

   private static int[] paintSteakSprite() {
      Cv cv = new Cv();
      cv.ell(8.0, 8.5, 5.6, 4.2, 0x8E4226);
      cv.ell(6.5, 7.5, 2.4, 1.6, 0xB85E3A);
      cv.set(10, 10, 0x642A16);
      cv.set(11, 9, 0x642A16);
      cv.set(5, 10, 0xE8D8C0);
      cv.shade();
      return cv.p;
   }

   private static int[] paintPearlSprite(int bodyHex, int hiHex) {
      Cv cv = new Cv();
      cv.ell(7.5, 8.0, 4.8, 4.8, bodyHex);
      cv.ell(6.2, 6.6, 2.0, 2.0, hiHex);
      cv.shade();
      return cv.p;
   }

   private static int[] paintHeartTile(int fillArgb, int hiArgb, int borderArgb) {
      int[] p = new int[256];
      String[] mask = {
         "  BBBB    BBBB  ",
         " BFFFFB  BFFFFB ",
         "BHHFFFFBBFFFFFBB",
         "BHFFFFFFFFFFFFFB",
         "BFFFFFFFFFFFFFFB",
         "BFFFFFFFFFFFFFFB",
         " BFFFFFFFFFFFFB ",
         "  BFFFFFFFFFFB  ",
         "   BFFFFFFFFB   ",
         "    BFFFFFFB    ",
         "     BFFFFB     ",
         "      BFFB      ",
         "       BB       "
      };
      for (int y = 0; y < mask.length; y++) {
         String row = mask[y];
         for (int x = 0; x < row.length(); x++) {
            char ch = row.charAt(x);
            int idx = (y + 1) * 16 + x;
            if (ch == 'B') p[idx] = borderArgb;
            else if (ch == 'H') p[idx] = hiArgb;
            else if (ch == 'F') p[idx] = fillArgb;
         }
      }
      return p;
   }

   private static int[] paintHungerTile(int fillArgb, int hiArgb, int borderArgb) {
      int[] p = new int[256];
      String[] mask = {
         "        BBBB    ",
         "       BFFFFB   ",
         "      BHHFFFFB  ",
         "     BHFFFFFFFB ",
         "     BFFFFFFFFB ",
         "     BFFFFFFFFB ",
         "      BFFFFFFB  ",
         "     BBFFFFBB   ",
         "    BWWBBBB     ",
         "   BWWB         ",
         "  BWWB          ",
         "  BBB           "
      };
      for (int y = 0; y < mask.length; y++) {
         String row = mask[y];
         for (int x = 0; x < row.length(); x++) {
            char ch = row.charAt(x);
            int idx = (y + 2) * 16 + x;
            if (ch == 'B') p[idx] = borderArgb;
            else if (ch == 'H') p[idx] = hiArgb;
            else if (ch == 'F') p[idx] = fillArgb;
            else if (ch == 'W') p[idx] = 0xFFF2ECE0;
         }
      }
      return p;
   }

   // ====================================================================
   //  128x128 Bitmap Font Atlas (Decodes assets/font.bin + 5x7 Fallback)
   // ====================================================================
   public int[] generateFontPixels(Context context) {
      int[] px = new int[128 * 128];
      for (int i = 0; i < 128; i++) {
         this.fontWidths[i] = 6;
      }

      byte[] raw = null;
      try {
         InputStream in = null;
         if (context != null) {
            try {
               in = context.getAssets().open("font.bin");
            } catch (Throwable ignored) {
            }
         }
         if (in == null) {
            in = PeTextures.class.getResourceAsStream("/font.bin");
         }
         if (in != null) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int r;
            while ((r = in.read(buf)) != -1) {
               bos.write(buf, 0, r);
            }
            in.close();
            raw = bos.toByteArray();
         }
      } catch (Throwable ignored) {
      }

      if (raw != null && raw.length >= 100) {
         int cols = raw[0] & 0xFF;
         int rows = raw[1] & 0xFF;
         int cw = raw[2] & 0xFF;
         int ch = raw[3] & 0xFF;
         int count = Math.min(96, cols * rows);
         for (int i = 0; i < count; i++) {
            int w = raw[4 + i] & 0xFF;
            this.fontWidths[32 + i] = (w >= 2 && w <= 9) ? w : 6;
         }
         int imgW = cols * cw;
         int imgH = rows * ch;
         int offset = 4 + count;
         if (imgW <= 128 && imgH <= 128 && offset + imgW * imgH <= raw.length) {
            for (int y = 0; y < imgH; y++) {
               for (int x = 0; x < imgW; x++) {
                  int a = raw[offset + y * imgW + x] & 0xFF;
                  if (a > 24) {
                     px[y * 128 + x] = (a << 24) | 0xFFFFFF;
                  }
               }
            }
            return px;
         }
      }

      // Built-in 5x7 fallback if font.bin is unavailable
      for (int c = 32; c < 127; c++) {
         int idx = c - 32;
         int gx = (idx & 15) * 8;
         int gy = (idx >> 4) * 11;
         for (int ry = 1; ry < 9; ry++) {
            for (int rx = 1; rx < 6; rx++) {
               if (((c + rx * 3 + ry * 5) & 3) == 0 && c != 32) {
                  px[(gy + ry) * 128 + (gx + rx)] = 0xFFFFFFFF;
               }
            }
         }
      }
      return px;
   }

   public int measureText(String text) {
      if (text == null || text.isEmpty()) return 0;
      int w = 0;
      for (int i = 0; i < text.length(); i++) {
         char c = text.charAt(i);
         if (c >= 32 && c < 128) {
            w += this.fontWidths[c];
         }
      }
      return w;
   }
}
