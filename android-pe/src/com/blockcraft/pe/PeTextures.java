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
   public static final int T_LOG_BIRCH_SIDE = 39;
   public static final int T_LEAVES_BIRCH = 40;
   public static final int T_BUSHY_FROND_OAK = 41;
   public static final int T_SUGAR_CANE = 42;
   public static final int T_CRAFT_TOP = 43;
   public static final int T_FURNACE_FRONT = 44;
   public static final int T_FURNACE_SIDE = 45;
   public static final int T_CRAFT_SIDE = 46;
   public static final int T_WHEAT = 47;

   public static final int T_SPONGE = 48;
   public static final int T_GLASS = 49;
   public static final int T_DIAMOND_ORE = 50;
   public static final int T_REDSTONE_ORE = 51;
   public static final int T_LEAVES_OAK = 52;
   public static final int T_LEAVES_SPRUCE = 53;
   public static final int T_STONEBRICK = 54;
   public static final int T_CRACKED_STONEBRICK = 55;
   public static final int T_MOSSY_STONEBRICK = 56;
   public static final int T_PUMPKIN_SIDE = 57;
   public static final int T_PUMPKIN_TOP = 58;
   public static final int T_PUMPKIN_FACE = 59;
   public static final int T_JACK_O_LANTERN = 60;
   public static final int T_MELON_SIDE = 61;
   public static final int T_MELON_TOP = 62;
   public static final int T_HAY_SIDE = 63;

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
   public static final int T_HAY_TOP = 76;
   public static final int T_DOOR_TOP = 77;
   public static final int T_DOOR_BOTTOM = 78;
   public static final int T_FARM_WET = 79;
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
   public static final int T_SUN = 106;
   public static final int T_MOON = 107;
   public static final int T_BUSHY_FROND_SPRUCE = 108;
   public static final int T_BUSHY_FROND_BIRCH = 109;
   public static final int T_FURNACE_LIT = 110;

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
   public static final int T_ITEM_IRON_INGOT = 156;
   public static final int T_ITEM_GOLD_INGOT = 157;
   public static final int T_ITEM_DIAMOND = 158;
   public static final int T_ITEM_COAL = 159;
   public static final int T_ITEM_BREAD = 160;

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

      // 2. Crisp Pixel-Art Leaves & 3D Bushy Fronds (Update 3.19 overlapping leaflet blades with cast shadows & sky gaps)
      blitTile(px, T_LEAVES_OAK, paintLeaves(0, 0.34F, 0.78F, 0.24F));
      blitTile(px, T_LEAVES_SPRUCE, paintLeaves(1, 0.26F, 0.58F, 0.30F));
      // 2. Crisp Pixel-Art Leaves & 3D Bushy Fronds (PE v1.5 / PC v5.7 Overhaul: overlapping leaflet blades with cast shadows & sky gaps)
      blitTile(px, T_LEAVES_OAK, paintLeaves(0, 0.36F, 0.82F, 0.26F));
      blitTile(px, T_LEAVES_SPRUCE, paintLeaves(1, 0.28F, 0.62F, 0.32F));
      blitTile(px, T_LEAVES_BIRCH, paintLeaves(2, 0.50F, 0.80F, 0.34F));
      blitTile(px, T_BUSHY_FROND_OAK, paintBushyFrond(0.36F, 0.82F, 0.26F));
      blitTile(px, T_BUSHY_FROND_SPRUCE, paintBushyFrond(0.28F, 0.62F, 0.32F));
      blitTile(px, T_BUSHY_FROND_BIRCH, paintBushyFrond(0.50F, 0.80F, 0.34F));

      // 3. Stone, Cobble, Mossy Cobble, Bedrock, Sand, Gravel, Sandstone
      blitTile(px, T_STONE, paintStone(101));
      blitTile(px, T_COBBLE, paintCobble(202, 0, 0, 0));
      blitTile(px, T_MOSSY_COBBLE, paintMossyCobble());
      blitTile(px, T_BEDROCK, paintBedrock());
      blitTile(px, T_SAND, paintSand(0, 0, 0));
      blitTile(px, T_GRAVEL, paintGravel());
      blitTile(px, T_SANDSTONE_TOP, paintSandstoneTop(false));
      blitTile(px, T_SANDSTONE_SIDE, paintSandstoneSide(0, 0, 0));
      blitTile(px, T_RED_SANDSTONE, paintSandstoneSide(48, -32, -48));
      blitTile(px, T_STONE_SLAB_TOP, paintStoneSlabTop());
      blitTile(px, T_STONE_SLAB_SIDE, paintStoneSlabSide());

      // 4. Logs & Planks (Oak, Spruce, Birch)
      blitTile(px, T_LOG_OAK_SIDE, paintLogSide(c(0x3D2C16), c(0x584022), c(0x70532E), c(0x88673C), 601));
      blitTile(px, T_LOG_OAK_TOP, paintLogTop(c(0x4A361C), c(0x644A28), c(0xA4824C), c(0x8C6B3A), c(0xBC9A62), 602));
      blitTile(px, T_LOG_SPRUCE, paintLogSide(c(0x22150A), c(0x342110), c(0x462E18), c(0x583B20), 603));
      blitTile(px, T_LOG_BIRCH_SIDE, paintBirchSide());
      blitTile(px, T_PLANKS_OAK, paintPlanks(c(0xAC864E), 701));
      blitTile(px, T_PLANKS_SPRUCE, paintPlanks(c(0x70502E), 702));
      blitTile(px, T_PLANKS_BIRCH, paintPlanks(c(0xD6C486), 703));

      // 5. Ores (5-tone 3D faceted mineral veins embedded in recessed stone sockets)
      blitTile(px, T_COAL_ORE, paintOre(c(0x2C2E34), c(0x14161A), c(0x525660), 801, 7, false));
      blitTile(px, T_IRON_ORE, paintOre(c(0xDCA888), c(0x9E6C50), c(0xFAECD8), 802, 6, false));
      blitTile(px, T_GOLD_ORE, paintOre(c(0xFAD628), c(0xBA860C), c(0xFFFFB8), 803, 6, false));
      blitTile(px, T_DIAMOND_ORE, paintOre(c(0x4AECE6), c(0x1C9EA4), c(0xE0FFFF), 804, 5, true));
      blitTile(px, T_REDSTONE_ORE, paintOre(c(0xE62020), c(0x860A0A), c(0xFF8888), 805, 7, true));

      // 6. Bricks, Stone Bricks, Glass, Obsidian, Glowstone, Sponge, Cobweb
      blitTile(px, T_BRICKS, paintBricks());
      blitTile(px, T_STONEBRICK, paintStoneBrick(false, false));
      blitTile(px, T_CRACKED_STONEBRICK, paintStoneBrick(true, false));
      blitTile(px, T_MOSSY_STONEBRICK, paintStoneBrick(false, true));
      blitTile(px, T_GLASS, paintGlass());
      blitTile(px, T_OBSIDIAN, paintObsidian());
      blitTile(px, T_GLOWSTONE, paintGlowstone());
      blitTile(px, T_SPONGE, paintSponge());
      blitTile(px, T_COBWEB, paintCobweb());

      // 7. Utility Blocks, Crops & Decor (Crafting Table, Furnace, Chest, Bookshelf, TNT, Torch, Ladder, Bed, Bars, Doors, Crops, Pumpkins, Melons, Hay)
      blitTile(px, T_CRAFT_TOP, paintCraftTop());
      blitTile(px, T_CRAFT_SIDE, paintCraftSide());
      blitTile(px, T_FURNACE_FRONT, paintFurnaceFront(false));
      blitTile(px, T_FURNACE_LIT, paintFurnaceFront(true));
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
      blitTile(px, T_DOOR_TOP, paintDoor(true));
      blitTile(px, T_DOOR_BOTTOM, paintDoor(false));
      blitTile(px, T_FARM_WET, paintFarmland(true));
      blitTile(px, T_WHEAT, paintWheat(7));
      blitTile(px, T_SUGAR_CANE, paintSugarCane());
      blitTile(px, T_PUMPKIN_SIDE, paintPumpkinSide());
      blitTile(px, T_PUMPKIN_TOP, paintPumpkinTop());
      blitTile(px, T_PUMPKIN_FACE, paintPumpkinFace(false));
      blitTile(px, T_JACK_O_LANTERN, paintPumpkinFace(true));
      blitTile(px, T_MELON_SIDE, paintMelonSide());
      blitTile(px, T_MELON_TOP, paintMelonTop());
      blitTile(px, T_HAY_SIDE, paintHaySide());
      blitTile(px, T_HAY_TOP, paintHayTop());
      blitTile(px, T_SUN, paintSunTile());
      blitTile(px, T_MOON, paintMoonTile());

      // 8. Fluids, Fire, Snow, Ice, Plants, Flowers, Mushrooms, Cactus
      blitTile(px, T_WATER, paintWaterFrame(0));
      blitTile(px, T_LAVA, paintLavaFrame(0));
      blitTile(px, T_FIRE, paintFireTile());
      blitTile(px, T_SNOW, paintSnow());
      blitTile(px, T_ICE, paintIce(false));
      blitTile(px, T_PACKED_ICE, paintIce(true));
      blitTile(px, T_CACTUS_SIDE, paintCactusSide());
      blitTile(px, T_CACTUS_TOP, paintCactusTop());
      blitTile(px, T_FLOWER_RED, paintFlower(true));
      blitTile(px, T_FLOWER_YELLOW, paintFlower(false));
      blitTile(px, T_MUSHROOM_RED, paintMushroom(true));
      blitTile(px, T_MUSHROOM_BROWN, paintMushroom(false));

      // 9. Metal/Gem Blocks, Wool, Nether, End & Concrete
      blitTile(px, T_IRON_BLOCK, paintMetalBlock(c(0xDCE0E6), c(0xB8BEC8), c(0x8C94A0), c(0xFAFCFF), 310));
      blitTile(px, T_GOLD_BLOCK, paintMetalBlock(c(0xF8CC22), c(0xD49C10), c(0x9E6C06), c(0xFFF698), 311));
      blitTile(px, T_DIAMOND_BLOCK, paintGemBlock(c(0x4CECE2), c(0x28BEB6), c(0x14827C), c(0xD8FFFF), 312));
      blitTile(px, T_EMERALD_BLOCK, paintGemBlock(c(0x26DC5E), c(0x14A842), c(0x0A742A), c(0xA4FFC0), 313));
      blitTile(px, T_REDSTONE_BLOCK, paintGemBlock(c(0xD21E14), c(0x9E120A), c(0x680804), c(0xFF7262), 314));
      blitTile(px, T_COAL_BLOCK, paintCoalBlock());
      blitTile(px, T_QUARTZ_BLOCK, paintQuartzBlock());
      blitTile(px, T_SEA_LANTERN, paintSeaLantern());
      blitTile(px, T_PRISMARINE, paintMineralStone(390, new int[]{c(0x38746A), c(0x4C8C80), c(0x60A698), c(0x78BEAE)}));
      blitTile(px, T_SLIME_BLOCK, paintSlimeBlock());
      blitTile(px, T_NETHERRACK, paintNetherrack());
      blitTile(px, T_SOUL_SAND, paintSoulSand());
      blitTile(px, T_NETHER_BRICKS, paintNetherBricks());
      blitTile(px, T_END_STONE, paintEndStone());
      blitTile(px, T_PURPUR, paintPurpur());

      blitTile(px, T_WOOL_WHITE, paintWool(0xF0F2F4, 1000));
      blitTile(px, T_WOOL_RED, paintWool(0xB42820, 1014));
      blitTile(px, T_WOOL_BLUE, paintWool(0x2C3CA8, 1011));
      blitTile(px, T_WOOL_GREEN, paintWool(0x3E5C1C, 1013));
      blitTile(px, T_WOOL_YELLOW, paintWool(0xF4CA26, 1004));
      blitTile(px, T_TERRACOTTA, paintMineralStone(480, new int[]{c(0x8A5238), c(0x965C40), c(0xA26648), c(0xAE7052)}));
      blitTile(px, T_CONCRETE_GRAY, paintConcrete(0x363A3E, 501));
      blitTile(px, T_CONCRETE_YELLOW, paintConcrete(0xF2B818, 502));
      blitTile(px, T_CONCRETE_BLUE, paintConcrete(0x2A3C9C, 503));
      blitTile(px, T_CONCRETE_RED, paintConcrete(0xA8221E, 504));

      // 10. Crack Stages (0..9)
      paintCrackStages(px);

      // 11. Master Pixel-Art Tools, Weapons, Food, Ingots, Gems & HUD Icons (PE v1.5 / PC v5.7 Overhaul)
      blitTile(px, T_ITEM_SWORD_WOOD, paintToolSprite(4, 0));
      blitTile(px, T_ITEM_SWORD_STONE, paintToolSprite(4, 1));
      blitTile(px, T_ITEM_SWORD_IRON, paintToolSprite(4, 2));
      blitTile(px, T_ITEM_SWORD_DIAMOND, paintToolSprite(4, 3));

      blitTile(px, T_ITEM_PICK_WOOD, paintToolSprite(1, 0));
      blitTile(px, T_ITEM_PICK_STONE, paintToolSprite(1, 1));
      blitTile(px, T_ITEM_PICK_IRON, paintToolSprite(1, 2));
      blitTile(px, T_ITEM_PICK_DIAMOND, paintToolSprite(1, 3));

      blitTile(px, T_ITEM_SHOVEL_IRON, paintToolSprite(3, 2));
      blitTile(px, T_ITEM_SHOVEL_DIAMOND, paintToolSprite(3, 3));
      blitTile(px, T_ITEM_AXE_IRON, paintToolSprite(2, 2));

      blitTile(px, T_ITEM_BOW, paintBowSprite());
      blitTile(px, T_ITEM_ARROW, paintArrowSprite());
      blitTile(px, T_ITEM_GOLDEN_APPLE, paintAppleSprite(0xFAD62A, 0xFFFF98));
      blitTile(px, T_ITEM_STEAK, paintSteakSprite());
      blitTile(px, T_ITEM_ENDER_PEARL, paintPearlSprite(0x188C78, 0x68FCE2));
      blitTile(px, T_ITEM_SNOWBALL, paintPearlSprite(0xDCEAF6, 0xFFFFFF));

      blitTile(px, T_ITEM_PISTOL, paintGunSprite(0));
      blitTile(px, T_ITEM_MACHINE_GUN, paintGunSprite(1));
      blitTile(px, T_ITEM_SNIPER, paintGunSprite(2));
      blitTile(px, T_ITEM_TNT_GRENADE, paintGrenadeSprite());
      blitTile(px, T_ITEM_C4_BOMB, paintC4Sprite());
      blitTile(px, T_ITEM_SIEGE_CANNON, paintCannonSprite(false));
      blitTile(px, T_ITEM_MORTAR, paintCannonSprite(true));

      blitTile(px, T_HEART_FULL, paintHeartTile(0xFFE82424, 0xFFFF9292, 0xFF280606));
      blitTile(px, T_HEART_EMPTY, paintHeartTile(0xFF2C1A1A, 0xFF482C2C, 0xFF120606));
      blitTile(px, T_HUNGER_FULL, paintHungerTile(0xFFD87E2C, 0xFFFFC070, 0xFF341808));
      blitTile(px, T_HUNGER_EMPTY, paintHungerTile(0xFF322216, 0xFF4C3624, 0xFF160C06));

      blitTile(px, T_ITEM_IRON_INGOT, paintIngotSprite(0xD8DCE4, 0xFAFCFF, 0x8C929E));
      blitTile(px, T_ITEM_GOLD_INGOT, paintIngotSprite(0xF8D028, 0xFFFFA8, 0xAA780A));
      blitTile(px, T_ITEM_DIAMOND, paintGemSprite(0x48F2E8, 0xE0FFFF, 0x18908A));
      blitTile(px, T_ITEM_COAL, paintCoalSprite());
      blitTile(px, T_ITEM_BREAD, paintBreadSprite());

      return px;
   }

   private int lastAnimFrame = -1;
   private ByteBuffer fluidSubBuf;

   /** Smoothly animates Water (tile 14) and Lava (tile 15) on the GPU atlas every 3 ticks (Update 3.9 Tex.animate). */
   public void updateFluidAnimations(int worldTick) {
      if (this.atlasTexId == 0) return;
      int frame = (worldTick / 3) & 31;
      if (frame == this.lastAnimFrame) return;
      this.lastAnimFrame = frame;
      if (this.fluidSubBuf == null) {
         this.fluidSubBuf = ByteBuffer.allocateDirect(16 * 16 * 4).order(ByteOrder.nativeOrder());
      }
      GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, this.atlasTexId);
      uploadSubTile(T_WATER, paintWaterFrame(frame));
      uploadSubTile(T_LAVA, paintLavaFrame(frame / 2));
   }

   private void uploadSubTile(int tile, int[] argb) {
      this.fluidSubBuf.clear();
      for (int i = 0; i < 256; i++) {
         int p = argb[i];
         int a = (p >>> 24) & 0xFF;
         int r = (p >>> 16) & 0xFF;
         int g = (p >>> 8) & 0xFF;
         int b = p & 0xFF;
         this.fluidSubBuf.put((byte)r);
         this.fluidSubBuf.put((byte)g);
         this.fluidSubBuf.put((byte)b);
         this.fluidSubBuf.put((byte)a);
      }
      this.fluidSubBuf.flip();
      int tx0 = (tile & 15) * 16;
      int ty0 = (tile >> 4) * 16;
      GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, tx0, ty0, 16, 16, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, this.fluidSubBuf);
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
      int[] pal = {rgb(146, 146, 146), rgb(164, 164, 164), rgb(180, 180, 180), rgb(198, 198, 198), rgb(216, 216, 216)};
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float v = fbm(x, y, 501) * 0.68F + hash(x, y, 502) * 0.32F;
            p[y * N + x] = step(pal, contrast(v, 2.0F));
         }
      }
      int[][] tufts = {
         {2, 2}, {6, 1}, {11, 2}, {14, 4},
         {1, 6}, {5, 5}, {9, 6}, {12, 8},
         {3, 10}, {7, 9}, {10, 12}, {14, 11},
         {1, 14}, {5, 13}, {8, 14}, {13, 14}
      };
      for (int i = 0; i < tufts.length; i++) {
         int tx = tufts[i][0], ty = tufts[i][1];
         boolean bright = (i & 1) == 0;
         p[ty * N + tx] = bright ? rgb(232, 232, 232) : rgb(134, 134, 134);
         p[((ty + 1) & 15) * N + tx] = bright ? rgb(208, 208, 208) : rgb(152, 152, 152);
         p[ty * N + ((tx + 1) & 15)] = rgb(142, 142, 142);
      }
      for (int i = 0; i < p.length; i++) {
         p[i] = tintRgb(p[i], 0.44F, 0.86F, 0.30F);
      }
      return p;
   }

   private static int[] paintDirt() {
      int[] pal = {c(0x64452B), c(0x755235), c(0x865E3E), c(0x966B48), c(0xA87854)};
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float v = fbm(x, y, 303) * 0.68F + hash(x, y, 304) * 0.32F;
            p[y * N + x] = step(pal, contrast(v, 2.0F));
         }
      }
      int[][] clods = {{3, 2, 1}, {9, 3, 0}, {13, 6, 1}, {2, 8, 0}, {7, 9, 1}, {11, 12, 0}, {4, 13, 1}, {14, 14, 0}};
      for (int[] cl : clods) {
         int x = cl[0], y = cl[1];
         boolean hi = cl[2] == 1;
         p[y * N + x] = hi ? c(0xBA946E) : c(0x523620);
         p[y * N + ((x + 1) & 15)] = hi ? c(0xA47E58) : c(0x482E1A);
         p[((y + 1) & 15) * N + x] = c(0x4E331E);
      }
      return p;
   }

   private static int[] paintGrassSide(boolean snowy) {
      int[] p = paintDirt();
      int[] skirt = {4, 5, 3, 2, 4, 6, 4, 3, 5, 4, 2, 3, 5, 6, 4, 3};
      for (int x = 0; x < N; x++) {
         int lip = skirt[x];
         if (lip < N) {
            p[lip * N + x] = mul(p[lip * N + x], 0.68F);
         }
         if (lip + 1 < N) {
            p[(lip + 1) * N + x] = mul(p[(lip + 1) * N + x], 0.85F);
         }
         for (int y = 0; y < lip && y < N; y++) {
            if (snowy) {
               int w = (y == 0) ? c(0xFFFFFF) : ((y == lip - 1) ? c(0xC8DCEB) : c(0xEEF7FF));
               p[y * N + x] = w;
            } else {
               float v = fbm(x, y, 512) * 0.65F + hash(x, y, 513) * 0.35F;
               int g = 160 + (int)(v * 48.0F);
               if (y == 0) g += 20;
               else if (y == lip - 1) g -= 24;
               p[y * N + x] = tintRgb(rgb(g, g, g), 0.44F, 0.86F, 0.30F);
            }
         }
      }
      return p;
   }

   private static int[] paintStone(int seed) {
      int[] pal = {c(0x62646A), c(0x72747A), c(0x808288), c(0x8E9096), c(0x9CA0A6)};
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float v = fbm(x * 2, y, seed) * 0.74F + hash(x, y, seed + 3) * 0.26F;
            if ((y & 3) == 0) v += 0.08F;
            if ((y & 3) == 3) v -= 0.08F;
            p[y * N + x] = step(pal, contrast(v, 2.2F));
         }
      }
      int[][] seams = {{2, 3, 5}, {9, 6, 5}, {3, 11, 6}, {10, 14, 4}};
      for (int[] s : seams) {
         int sx = s[0], sy = s[1], len = s[2];
         for (int i = 0; i < len; i++) {
            int x = (sx + i) & 15;
            int y = (sy + ((i == 2 || i == 4) ? 1 : 0)) & 15;
            p[y * N + x] = c(0x505258);
            p[((y + 1) & 15) * N + x] = c(0xA6AAB0);
         }
      }
      return p;
   }

   private static int[] paintCobble(int seed, int tr, int tg, int tb) {
      int M = 9;
      Random r = new Random(seed);
      float[] sx = new float[M];
      float[] sy = new float[M];
      int[] shade = new int[M];
      for (int i = 0; i < M; i++) {
         sx[i] = (i % 3) * 5.2F + 1.0F + r.nextFloat() * 2.4F;
         sy[i] = (i / 3) * 5.2F + 1.0F + r.nextFloat() * 2.4F;
         shade[i] = -16 + r.nextInt(33);
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
            if (edge < 0.78F) {
               v = 56 + (int)((hash(x, y, seed + 5) - 0.5F) * 10.0F);
            } else {
               float light = (-bestDx * 0.65F - bestDy * 0.85F) / 3.0F;
               int grain = (int)((hash(x, y, seed + 11) - 0.5F) * 14.0F);
               v = 124 + shade[best] + (int)(light * 28.0F) + grain;
               v = v / 6 * 6;
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
            if ((lum < 88 && m > 0.34F) || m > 0.62F) {
               int g = 104 + (int)(hash(x, y, 1602) * 42.0F);
               p[y * N + x] = rgb(g * 54 / 100, g, g * 38 / 100);
            }
         }
      }
      return p;
   }

   private static int[] paintBedrock() {
      int[] pal = {c(0x18181C), c(0x2A2C32), c(0x42444C), c(0x5E6068), c(0x7C8088)};
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float v = vn(x + 0.5F, y * 1.5F + 0.5F, 4, 110) * 0.5F
               + vn(x + 0.5F, y + 0.5F, 8, 111) * 0.35F
               + hash(x, y, 112) * 0.15F;
            p[y * N + x] = step(pal, contrast(v, 2.35F));
         }
      }
      return p;
   }

   private static int[] paintSand(int tr, int tg, int tb) {
      int[] pal = {
         rgb(202 + tr, 190 + tg, 136 + tb),
         rgb(214 + tr, 202 + tg, 148 + tb),
         rgb(225 + tr, 214 + tg, 160 + tb),
         rgb(235 + tr, 226 + tg, 172 + tb),
         rgb(245 + tr, 236 + tg, 186 + tb)
      };
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float wave = (float)Math.sin((y + vn(x * 0.5F, y * 0.5F, 8, 401) * 2.5F) * Math.PI / 4.0) * 0.22F;
            float v = fbm(x, y, 402) * 0.62F + hash(x, y, 403) * 0.38F + wave;
            p[y * N + x] = step(pal, contrast(v, 2.0F));
         }
      }
      return p;
   }

   private static int[] paintGravel() {
      int[][] tones = {
         {106, 102, 100}, {126, 120, 118}, {146, 138, 134},
         {92, 88, 86}, {136, 118, 108}, {116, 120, 128}
      };
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            int cellX = (x + (h32(x >> 1, y >> 1, 450) & 1)) >> 1;
            int cellY = (y + (h32(x >> 1, y >> 1, 451) & 1)) >> 1;
            int h = h32(cellX, cellY, 452) & 0x7FFFFFFF;
            int[] t = tones[h % tones.length];
            int corner = ((x & 1) == 0 && (y & 1) == 0) ? 14 : (((x & 1) == 1 && (y & 1) == 1) ? -16 : 0);
            int jit = (int)((hash(x, y, 453) - 0.5F) * 14.0F);
            p[y * N + x] = rgb(t[0] + corner + jit, t[1] + corner + jit, t[2] + corner + jit);
         }
      }
      return p;
   }

   private static int[] paintSandstoneTop(boolean red) {
      int[] pal = red
         ? new int[]{c(0x98481A), c(0xA85420), c(0xB86028), c(0xC66C30), c(0xD4783A)}
         : new int[]{c(0xC4B686), c(0xD0C292), c(0xDCD0A0), c(0xE6DAAE), c(0xF0E6BC)};
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float v = fbm(x, y, red ? 415 : 220) * 0.74F + hash(x, y, red ? 416 : 221) * 0.26F;
            int col = step(pal, contrast(v, 1.95F));
            if (x == 0 || y == 0) col = mul(col, 1.07F);
            else if (x == 15 || y == 15) col = mul(col, 0.91F);
            p[y * N + x] = col;
         }
      }
      return p;
   }

   private static int[] paintSandstoneSide(int tr, int tg, int tb) {
      int[] p = new int[N * N];
      int[] band = {238, 228, 192, 224, 216, 188, 230, 220, 206, 224, 190, 218, 226, 212, 194, 208};
      for (int y = 0; y < N; y++) {
         int b = band[y];
         for (int x = 0; x < N; x++) {
            int j = (int)((hash(x, y, 1401) - 0.5F) * 10.0F);
            int v = b + j;
            if (y < 4 && (x & 3) == 0) v -= 16;
            p[y * N + x] = rgb(v + tr, v * 95 / 100 + tg, v * 72 / 100 + tb);
         }
      }
      return p;
   }

   private static int[] paintStoneSlabTop() {
      int[] p = paintStone(115);
      for (int i = 0; i < N; i++) {
         p[i] = mul(p[i], 1.15F);
         p[i * N] = mul(p[i * N], 1.15F);
         p[15 * N + i] = mul(p[15 * N + i], 0.78F);
         p[i * N + 15] = mul(p[i * N + 15], 0.78F);
      }
      return p;
   }

   private static int[] paintStoneSlabSide() {
      int[] p = paintStoneSlabTop();
      for (int x = 0; x < N; x++) {
         p[7 * N + x] = mul(p[7 * N + x], 0.68F);
         p[8 * N + x] = mul(p[8 * N + x], 1.18F);
      }
      return p;
   }

   private static int[] paintLogSide(int dark, int mid, int light, int high, int seed) {
      int[] pal = {dark, mid, light, high};
      int[] p = new int[N * N];
      for (int x = 0; x < N; x++) {
         int colMod = x & 3;
         for (int y = 0; y < N; y++) {
            float streak = vn(x * 0.5F, y * 0.25F, 4, seed + 1) * 0.65F + hash(x, y, seed + 2) * 0.35F;
            float v = colMod == 0 ? streak * 0.34F : (colMod == 1 ? 0.52F + streak * 0.48F : (colMod == 2 ? 0.30F + streak * 0.52F : 0.16F + streak * 0.42F));
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
               int base = r < 1.5F ? bark : ((ring & 1) == 0 ? ringA : ringB);
               if (edge == 2) base = core;
               int jit = (int)((hash(x, y, seed) - 0.5F) * 10.0F);
               p[y * N + x] = add(base, jit);
            }
         }
      }
      return p;
   }

   private static int[] paintPlanks(int base, int seed) {
      int cSeam = mul(base, 0.58F);
      int cSh = mul(base, 0.84F);
      int cMid = base;
      int cLt = mul(base, 1.09F);
      int cHi = mul(base, 1.18F);
      int[] pal = {cSh, cMid, cLt};
      int[] p = new int[N * N];
      int[] joints = {7, 13, 4, 10};
      for (int y = 0; y < N; y++) {
         int plank = y >> 2;
         int py = y & 3;
         int jx = joints[plank];
         for (int x = 0; x < N; x++) {
            if (py == 3 || x == jx) {
               p[y * N + x] = cSeam;
            } else {
               float g = vn((x + plank * 5) * 0.25F, plank + py * 0.5F, 4, seed) * 0.72F
                  + hash(x, y, seed + 2) * 0.28F;
               int col = step(pal, contrast(g, 1.85F));
               if (py == 0) col = mix(col, cHi, 0.45F);
               else if (x == ((jx + 1) & 15)) col = cHi;
               p[y * N + x] = col;
            }
         }
      }
      for (int plank = 0; plank < 4; plank++) {
         int jx = joints[plank];
         int yNail = plank * 4 + 1;
         p[yNail * N + ((jx + 2) & 15)] = cSeam;
         p[yNail * N + ((jx - 2) & 15)] = cSeam;
      }
      return p;
   }

   private static int[] paintOre(int main, int shade, int high, int seed, int n, boolean small) {
      int[] p = paintStone(101);
      int[][] clusters = {
         {3, 2, 1}, {10, 3, 0}, {6, 6, 1},
         {2, 9, 0}, {11, 8, 1}, {5, 12, 0}, {10, 13, 0}
      };
      int count = Math.min(n, clusters.length);
      for (int i = 0; i < count; i++) {
         int cx = (clusters[i][0] + (seed & 3)) & 15;
         int cy = (clusters[i][1] + ((seed >> 2) & 3)) & 15;
         boolean big = !small && clusters[i][2] == 1;
         int[][] shape = big
            ? new int[][]{{0, 0, 3}, {1, 0, 2}, {0, 1, 2}, {1, 1, 1}, {2, 1, 0}, {1, 2, 0}}
            : new int[][]{{0, 0, 3}, {1, 0, 2}, {0, 1, 1}, {1, 1, 0}};
         for (int[] s : shape) {
            int rx = (cx + s[0] + 1) & 15;
            int ry = (cy + s[1] + 1) & 15;
            p[ry * N + rx] = c(0x484A50);
         }
         for (int[] s : shape) {
            int x = (cx + s[0]) & 15;
            int y = (cy + s[1]) & 15;
            p[y * N + x] = s[2] == 3 ? high : (s[2] == 2 ? mix(main, high, 0.45F) : (s[2] == 1 ? main : shade));
         }
      }
      return p;
   }

   private static int[] paintBricks() {
      int[] p = new int[N * N];
      int mortar = c(0xC2B6AA);
      int mortarShadow = c(0x988C80);
      for (int y = 0; y < N; y++) {
         int row = y >> 2;
         int ry = y & 3;
         for (int x = 0; x < N; x++) {
            int xs = (x + (row & 1) * 4) & 15;
            int bx = xs >> 3;
            int rx = xs & 7;
            if (ry == 0 || rx == 0) {
               p[y * N + x] = (ry == 0 && rx > 0) ? mortar : mortarShadow;
               continue;
            }
            int id = row * 2 + bx;
            int baseR = 164 + (int)((hash(id, row, 200) - 0.5F) * 24);
            int baseG = 74 + (int)((hash(id, row, 201) - 0.5F) * 16);
            int baseB = 56 + (int)((hash(id, row, 202) - 0.5F) * 14);
            int d = (int)((hash(x, y, 203) - 0.5F) * 10);
            if (ry == 1) d += 15;
            else if (ry == 3) d -= 12;
            if (rx == 1) d += 9;
            else if (rx == 7) d -= 9;
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
               p[y * N + x] = c(0x3E4046);
            } else {
               int v = 128 + (int)((fbm(x, y, 1501) - 0.5F) * 20.0F);
               if (by == 0 || bx == 0) v += 20;
               else if (by == 6 || bx == 14) v -= 18;
               v = v / 6 * 6;
               p[y * N + x] = rgb(v, v, v + 3);
            }
         }
      }
      if (cracked) {
         int[][] cr = {{4, 1}, {4, 2}, {5, 3}, {5, 4}, {6, 5}, {11, 9}, {11, 10}, {12, 11}, {11, 12}, {10, 13}};
         for (int[] k : cr) {
            p[k[1] * N + k[0]] = c(0x2E3036);
            p[k[1] * N + ((k[0] + 1) & 15)] = c(0x9EA2A8);
         }
      }
      if (mossy) {
         for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
               if (fbm(x, y, 1509) > 0.54F) {
                  int g = 108 + (int)(hash(x, y, 1510) * 38.0F);
                  p[y * N + x] = rgb(g * 54 / 100, g, g * 38 / 100);
               }
            }
         }
      }
      return p;
   }

   private static int[] paintGlass() {
      int[] p = new int[N * N];
      int edge = c(0xBCE8F6);
      int dark = c(0x649CB2);
      int hi = c(0xF2FCFF);
      for (int i = 0; i < N; i++) {
         p[i] = i < 15 ? hi : dark;
         p[i * N] = i < 15 ? hi : dark;
         p[15 * N + i] = dark;
         p[i * N + 15] = dark;
      }
      p[N + 1] = hi;
      for (int k = 0; k < 5; k++) {
         p[(2 + k) * N + (6 - k)] = hi;
         if (k < 3) {
            p[(9 + k) * N + (12 - k)] = edge;
         }
      }
      return p;
   }

   private static int[] paintGlowstone() {
      int[] pal = {c(0x5E360E), c(0x985E18), c(0xD2942A), c(0xF8D458), c(0xFFFFCA)};
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float v = vn(x * 0.5F, y * 0.5F, 8, 1701) * 0.65F + hash(x, y, 1702) * 0.35F;
            p[y * N + x] = step(pal, contrast(v, 2.35F));
         }
      }
      return p;
   }

   private static int[] paintObsidian() {
      int[] pal = {c(0x0C0816), c(0x161026), c(0x22183A), c(0x322452), c(0x4A3672)};
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float v = fbm(x, y, 1801) * 0.68F + hash(x, y, 1802) * 0.32F;
            p[y * N + x] = step(pal, contrast(v, 2.15F));
         }
      }
      int[][] glints = {{3, 3}, {4, 4}, {11, 5}, {12, 6}, {5, 11}, {6, 12}};
      for (int[] g : glints) {
         p[g[1] * N + g[0]] = c(0x644A98);
      }
      return p;
   }

   private static int[] paintCraftTop() {
      int[] p = paintPlanks(c(0xAC864E), 701);
      int border = c(0x4A341A);
      int grid = c(0x382610);
      for (int y = 2; y <= 13; y++) {
         for (int x = 2; x <= 13; x++) {
            if (x == 2 || x == 13 || y == 2 || y == 13) {
               p[y * N + x] = border;
            } else if (x == 6 || x == 9 || y == 6 || y == 9) {
               p[y * N + x] = grid;
            } else {
               int jit = (int)((hash(x, y, 1901) - 0.5F) * 10.0F);
               p[y * N + x] = add(c(0xC2985E), jit);
            }
         }
      }
      p[2 * N + 2] = p[2 * N + 13] = p[13 * N + 2] = p[13 * N + 13] = c(0xC8CED6);
      return p;
   }

   private static int[] paintCraftSide() {
      int[] p = paintPlanks(c(0xAC864E), 701);
      for (int y = 0; y < 4; y++) {
         for (int x = 0; x < N; x++) {
            p[y * N + x] = y == 3 ? c(0x382610) : ((x & 3) == 0 ? c(0x684A26) : c(0x8C6638));
         }
      }
      for (int y = 5; y <= 13; y++) {
         p[y * N + 3] = c(0x4A341A);
         p[y * N + 12] = c(0x4A341A);
      }
      for (int y = 6; y <= 11; y++) {
         p[y * N + 6] = c(0xDCE2EA);
         p[y * N + 7] = y < 8 ? c(0x9CA4AE) : c(0x7C562C);
      }
      for (int y = 6; y <= 12; y++) {
         p[y * N + 10] = y < 8 ? c(0xBCC4CE) : c(0x7C562C);
      }
      p[6 * N + 9] = c(0xDCE2EA);
      p[6 * N + 11] = c(0x88909A);
      return p;
   }

   private static int[] paintFurnaceFront(boolean on) {
      int[] p = paintStoneBrick(false, false);
      for (int y = 2; y <= 7; y++) {
         for (int x = 3; x <= 12; x++) {
            if (y == 2 && (x == 3 || x == 12)) continue;
            boolean archRim = x == 3 || x == 12 || y == 2;
            p[y * N + x] = archRim ? c(0x52545A) : c(0x18181C);
         }
      }
      for (int y = 9; y <= 13; y++) {
         for (int x = 3; x <= 12; x++) {
            boolean rim = x == 3 || x == 12 || y == 9;
            if (rim) {
               p[y * N + x] = c(0x4E5056);
            } else if (on && y >= 10) {
               p[y * N + x] = ((x + y) & 1) == 0 ? c(0xFF981E) : (y == 12 ? c(0xFFFFA8) : c(0xFFD848));
            } else {
               p[y * N + x] = (x & 1) == 0 ? c(0x222226) : c(0x565860);
            }
         }
      }
      return p;
   }

   private static int[] paintChestWood(float k) {
      int[] p = new int[N * N];
      int rim = c(0x362410);
      int innerRim = c(0x583C1C);
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            if (x == 0 || x == 15 || y == 0 || y == 15) {
               p[y * N + x] = rim;
            } else if (x == 1 || x == 14 || y == 1 || y == 14) {
               p[y * N + x] = innerRim;
            } else {
               float g = vn(x * 0.25F, y * 0.5F, 4, 2101) * 0.7F + hash(x, y, 2102) * 0.3F;
               int base = step(new int[]{c(0x94642C), c(0xAA7636), c(0xBE8842), c(0xD29A4E)}, g);
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
         p[5 * N + x] = c(0x2A1B0A);
         p[6 * N + x] = c(0x52381A);
      }
      if (front) {
         for (int y = 4; y <= 8; y++) {
            for (int x = 6; x <= 9; x++) {
               boolean border = x == 6 || x == 9 || y == 4 || y == 8;
               p[y * N + x] = (x == 6 || y == 4) ? c(0xFFF698) : (border ? c(0xB88618) : c(0xECC438));
            }
         }
         p[6 * N + 7] = p[6 * N + 8] = c(0x362410);
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

   private static int[] outlineAndBevelSprite(int[] src, int outlineHex, int seedTile) {
      int[] out = src.clone();
      int outCol = c(outlineHex);
      int outRim = mul(outCol, 1.32F);
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            int p = src[y * 16 + x];
            if ((p >>> 24) < 128) continue;
            boolean topOpen = y == 0 || (src[(y - 1) * 16 + x] >>> 24) < 128;
            boolean leftOpen = x == 0 || (src[y * 16 + (x - 1)] >>> 24) < 128;
            boolean botOpen = y == 15 || (src[(y + 1) * 16 + x] >>> 24) < 128;
            boolean rightOpen = x == 15 || (src[y * 16 + (x + 1)] >>> 24) < 128;
            if (topOpen || leftOpen || botOpen || rightOpen) {
               out[y * 16 + x] = (botOpen || rightOpen) ? outCol : ((topOpen && leftOpen) ? outRim : mix(p, outCol, 0.58F));
            } else {
               boolean tlNear = (y < 2 || (src[(y - 2) * 16 + x] >>> 24) < 128)
                  || (x < 2 || (src[y * 16 + (x - 2)] >>> 24) < 128);
               boolean brNear = (y > 13 || (src[(y + 2) * 16 + x] >>> 24) < 128)
                  || (x > 13 || (src[y * 16 + (x + 2)] >>> 24) < 128);
               float k = 1.0F + (hash(x, y, seedTile + 900) - 0.5F) * 0.06F;
               if (tlNear && !brNear) k += 0.14F;
               else if (brNear && !tlNear) k -= 0.10F;
               out[y * 16 + x] = mul(p, k);
            }
         }
      }
      return out;
   }

   private static final int[][] PE_TIER_PAL = {
      {0x362210, 0x6A4826, 0x8F6436, 0xAD7E48, 0xC8985E}, // 0: Wood
      {0x24262A, 0x56585E, 0x767880, 0x94969E, 0xB2B6BE}, // 1: Stone
      {0x2C323A, 0x7A828E, 0xAEB6C2, 0xD8E0EC, 0xFAFCFF}, // 2: Iron
      {0x0E3A3C, 0x1E8C90, 0x32C4C0, 0x54F2EC, 0xD8FFFF}  // 3: Diamond
   };

   // toolClass: 1=Pickaxe, 2=Axe, 3=Shovel, 4=Sword; tier: 0=Wood, 1=Stone, 2=Iron, 3=Diamond
   private static int[] paintToolSprite(int toolClass, int tier) {
      int[] pal = PE_TIER_PAL[Math.max(0, Math.min(3, tier))];
      int out = pal[0], sh = pal[1], mid = pal[2], lt = pal[3], hi = pal[4];
      Cv cv = new Cv();
      if (toolClass == 4) {
         // Sword with custom fuller, crossguard & pommel
         cv.rect(1, 13, 2, 2, lt);
         cv.set(1, 14, out);
         cv.line(3, 12, 5, 10, 0x8C6234);
         cv.line(4, 12, 6, 10, 0x5C3E1E);
         cv.line(3, 8, 7, 12, sh);
         cv.line(4, 8, 8, 12, lt);
         cv.set(3, 7, hi);
         cv.set(8, 12, out);
         for (int i = 0; i < 8; i++) {
            int bx = 6 + i;
            int by = 9 - i;
            cv.set(bx, by, mid);
            cv.set(bx, by - 1, i >= 5 ? hi : lt);
            cv.set(bx + 1, by, sh);
            if (i == 7) {
               cv.set(14, 1, hi);
               cv.set(13, 1, hi);
               cv.set(14, 2, lt);
            }
         }
         return outlineAndBevelSprite(cv.p, out, 800 + tier);
      }
      // Diagonal wooden shaft
      for (int i = 0; i < 9; i++) {
         int hx = 2 + i;
         int hy = 13 - i;
         cv.set(hx, hy, 0x9E723E);
         cv.set(hx + 1, hy, 0x684824);
      }
      cv.set(2, 14, 0x36200E);
      if (toolClass == 1) {
         // Pickaxe head
         String[] mask = {
            ".....HHHHHH....",
            "....HHLLLLMH...",
            "...HLLMMMMSSH..",
            "......MMSS.LMS.",
            "..........LMMSS",
            "...........LMSS",
            "............MSS"
         };
         stampToolMask(cv, mask, 0, 1, sh, mid, lt, hi);
      } else if (toolClass == 2) {
         // Axe head
         String[] mask = {
            "..HHHH...",
            ".HHLLLM..",
            "HHLLMMMS.",
            "HLLMMMSS.",
            ".LLMMSS..",
            "..MMSS..."
         };
         stampToolMask(cv, mask, 6, 1, sh, mid, lt, hi);
      } else {
         // Shovel head
         String[] mask = {
            "..HHH..",
            ".HHLLH.",
            ".HLLMMS",
            ".LLMSSS",
            "..MSSS."
         };
         stampToolMask(cv, mask, 8, 1, sh, mid, lt, hi);
      }
      return outlineAndBevelSprite(cv.p, out, 810 + toolClass * 10 + tier);
   }

   private static void stampToolMask(Cv cv, String[] mask, int x0, int y0, int sh, int mid, int lt, int hi) {
      for (int r = 0; r < mask.length; r++) {
         String row = mask[r];
         for (int col = 0; col < row.length(); col++) {
            char ch = row.charAt(col);
            if (ch == '.') continue;
            int rgb = ch == 'H' ? hi : (ch == 'L' ? lt : (ch == 'M' ? mid : sh));
            cv.set(x0 + col, y0 + r, rgb);
         }
      }
   }

   private static int[] paintBowSprite() {
      Cv cv = new Cv();
      for (int y = 2; y <= 13; y++) {
         double ny = (y - 7.5) / 6.0;
         int x = (int)Math.round(10.5 - 5.2 * ny * ny);
         cv.set(x, y, y >= 6 && y <= 9 ? 0xD8B048 : 0x9C6E38);
         cv.set(x + 1, y, 0x62421E);
      }
      cv.line(11, 2, 11, 13, 0xE6ECF4);
      return outlineAndBevelSprite(cv.p, 0x2E1C0C, T_ITEM_BOW);
   }

   private static int[] paintArrowSprite() {
      Cv cv = new Cv();
      cv.line(3, 12, 12, 3, 0x9C723C);
      cv.line(4, 12, 12, 4, 0x684822);
      cv.rect(11, 2, 3, 3, 0xDCE2EC);
      cv.set(13, 2, 0xFFFFFF);
      cv.line(2, 11, 4, 9, 0xF4F6FA);
      cv.line(4, 13, 6, 11, 0xD0D8E2);
      return outlineAndBevelSprite(cv.p, 0x2C241C, T_ITEM_ARROW);
   }

   private static int[] paintAppleSprite(int bodyHex, int hiHex) {
      Cv cv = new Cv();
      cv.ell(7.5, 9.0, 4.6, 4.4, bodyHex);
      cv.ell(6.0, 7.6, 1.9, 1.9, hiHex);
      cv.line(8, 4, 8, 2, 0x6A4622);
      cv.rect(9, 2, 3, 2, 0x42A82E);
      return outlineAndBevelSprite(cv.p, 0x4A2E06, T_ITEM_GOLDEN_APPLE);
   }

   private static int[] paintSteakSprite() {
      Cv cv = new Cv();
      cv.ell(8.0, 8.5, 5.4, 4.0, 0x7E381E);
      cv.ell(6.6, 7.5, 2.5, 1.7, 0xA85230);
      cv.rect(5, 9, 2, 2, 0xF2E6D2);
      cv.line(8, 7, 10, 9, 0x4E1E0C);
      cv.line(6, 9, 8, 11, 0x4E1E0C);
      return outlineAndBevelSprite(cv.p, 0x321208, T_ITEM_STEAK);
   }

   private static int[] paintPearlSprite(int bodyHex, int hiHex) {
      Cv cv = new Cv();
      cv.ell(7.5, 8.0, 4.6, 4.6, bodyHex);
      cv.ell(6.2, 6.6, 1.8, 1.8, hiHex);
      cv.set(6, 6, 0xFFFFFF);
      return outlineAndBevelSprite(cv.p, 0x0A2C26, T_ITEM_ENDER_PEARL);
   }

   private static int[] paintIngotSprite(int bodyHex, int hiHex, int darkHex) {
      Cv cv = new Cv();
      for (int y = 5; y <= 11; y++) {
         int shift = (11 - y) / 2;
         for (int x = 3 + shift; x <= 11 + shift; x++) {
            int col = (y <= 6 || x == 3 + shift) ? hiHex : ((y >= 10 || x == 11 + shift) ? darkHex : bodyHex);
            cv.set(x, y, col);
         }
      }
      return outlineAndBevelSprite(cv.p, 0x26282E, T_ITEM_IRON_INGOT);
   }

   private static int[] paintGemSprite(int bodyHex, int hiHex, int darkHex) {
      Cv cv = new Cv();
      cv.ell(7.5, 8.0, 4.4, 5.0, bodyHex);
      cv.ell(6.4, 6.6, 2.0, 2.2, hiHex);
      cv.set(6, 6, 0xFFFFFF);
      cv.line(5, 11, 10, 11, darkHex);
      return outlineAndBevelSprite(cv.p, 0x0C3836, T_ITEM_DIAMOND);
   }

   private static int[] paintCoalSprite() {
      Cv cv = new Cv();
      cv.ell(7.5, 8.5, 4.6, 4.2, 0x2C2E36);
      cv.ell(6.2, 7.2, 2.0, 1.8, 0x545864);
      cv.set(9, 10, 0x16181C);
      return outlineAndBevelSprite(cv.p, 0x0E1014, T_ITEM_COAL);
   }

   private static int[] paintBreadSprite() {
      Cv cv = new Cv();
      cv.ell(7.5, 8.5, 5.4, 3.8, 0xC88A2E);
      cv.ell(7.0, 7.5, 4.0, 2.2, 0xECC058);
      cv.line(5, 7, 6, 8, 0x8E5816);
      cv.line(8, 7, 9, 8, 0x8E5816);
      return outlineAndBevelSprite(cv.p, 0x3E2408, T_ITEM_BREAD);
   }

   private static int[] paintGunSprite(int kind) {
      Cv cv = new Cv();
      if (kind == 0) {
         cv.rect(3, 5, 9, 3, 0x525864);
         cv.rect(3, 5, 9, 1, 0x8892A0);
         cv.rect(3, 8, 3, 5, 0x6E4826);
      } else if (kind == 1) {
         cv.rect(2, 5, 12, 3, 0x464C56);
         cv.rect(3, 5, 10, 1, 0x828C9A);
         cv.rect(3, 8, 3, 4, 0x32363E);
         cv.rect(8, 8, 2, 5, 0xD8A828);
      } else {
         cv.rect(1, 6, 14, 2, 0x48583E);
         cv.rect(5, 4, 5, 2, 0x282C34);
         cv.rect(6, 4, 3, 1, 0x68D8FF);
         cv.rect(2, 8, 4, 3, 0x6E4826);
      }
      return outlineAndBevelSprite(cv.p, 0x14161A, T_ITEM_PISTOL + kind);
   }

   private static int[] paintGrenadeSprite() {
      Cv cv = new Cv();
      cv.rect(5, 5, 6, 8, 0x3E6432);
      cv.rect(6, 3, 4, 2, 0x98A0A8);
      cv.line(5, 8, 10, 8, 0x284220);
      return outlineAndBevelSprite(cv.p, 0x182412, T_ITEM_TNT_GRENADE);
   }

   private static int[] paintC4Sprite() {
      Cv cv = new Cv();
      cv.rect(3, 4, 10, 8, 0x8E8268);
      cv.rect(5, 6, 6, 4, 0x22262C);
      cv.set(7, 7, 0xFF2820);
      cv.set(8, 7, 0xFF2820);
      return outlineAndBevelSprite(cv.p, 0x242018, T_ITEM_C4_BOMB);
   }

   private static int[] paintCannonSprite(boolean mortar) {
      Cv cv = new Cv();
      if (mortar) {
         cv.line(4, 12, 11, 3, 0x4A525C);
         cv.line(5, 12, 12, 3, 0x788290);
         cv.rect(3, 12, 8, 2, 0x6E4826);
         cv.set(12, 2, 0xFFA824);
      } else {
         cv.rect(2, 5, 11, 4, 0x3C424A);
         cv.rect(3, 5, 9, 1, 0x788290);
         cv.rect(4, 9, 5, 4, 0x7C5428);
         cv.set(13, 6, 0xFFA824);
      }
      return outlineAndBevelSprite(cv.p, 0x16181C, T_ITEM_SIEGE_CANNON);
   }

   private static int[] paintSponge() {
      int[] pal = {c(0x9C9228), c(0xB8AE34), c(0xCFC442), c(0xE2D854)};
      int[] p = paintMineralStone(481, pal);
      int[][] pores = {{3, 3}, {8, 2}, {12, 4}, {2, 8}, {7, 7}, {13, 9}, {4, 12}, {10, 13}};
      for (int[] pr : pores) {
         p[pr[1] * N + pr[0]] = c(0x6E6618);
         p[pr[1] * N + ((pr[0] + 1) & 15)] = c(0x82781E);
      }
      return p;
   }

   private static int[] paintCobweb() {
      int[] p = new int[N * N];
      for (int i = 0; i < N; i++) {
         p[i * N + i] = c(0xE8ECF2);
         p[i * N + (15 - i)] = c(0xE8ECF2);
         p[7 * N + i] = c(0xD4DCE4);
         p[i * N + 7] = c(0xD4DCE4);
      }
      int[] rings = {3, 6};
      for (int r : rings) {
         for (int a = -r; a <= r; a++) {
            p[(7 - r) * N + (7 + a)] = c(0xF4F8FF);
            p[(7 + r) * N + (7 + a)] = c(0xF4F8FF);
            p[(7 + a) * N + (7 - r)] = c(0xF4F8FF);
            p[(7 + a) * N + (7 + r)] = c(0xF4F8FF);
         }
      }
      return p;
   }

   private static int[] paintFireTile() {
      int[] p = new int[N * N];
      for (int x = 1; x < 15; x++) {
         int top = 2 + ((x * 5) & 5);
         for (int y = top; y < 16; y++) {
            float frac = (15 - y) / (float)Math.max(1, 15 - top);
            p[y * N + x] = frac < 0.35F ? c(0xFFF698) : (frac < 0.7F ? c(0xFF9818) : c(0xE0320C));
         }
      }
      return p;
   }

   private static int[] paintMushroom(boolean red) {
      int[] p = new int[N * N];
      for (int y = 10; y < 16; y++) {
         p[y * N + 7] = c(0xE8E0CE);
         p[y * N + 8] = c(0xC8BEA8);
      }
      for (int y = 6; y <= 10; y++) {
         int hw = y == 6 ? 2 : (y == 7 ? 4 : (y <= 9 ? 5 : 4));
         for (int x = 8 - hw; x < 8 + hw; x++) {
            p[y * N + x] = red ? (y <= 7 ? c(0xEA2E24) : c(0xB81C14)) : (y <= 7 ? c(0x9E7248) : c(0x785432));
         }
      }
      if (red) {
         p[7 * N + 6] = p[7 * N + 10] = p[8 * N + 8] = c(0xF8F4EC);
      }
      return p;
   }

   private static int[] paintBirchSide() {
      int[] pal = {c(0xCFCCB8), c(0xD8D5C2), c(0xE0DDCC), c(0xE8E6D8)};
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float v = vn(x + 0.5F, (y + 0.5F) * 0.4F, 8, 31) * 0.6F + hash(x, y, 32) * 0.4F;
            p[y * N + x] = step(pal, contrast(v, 1.8F));
         }
      }
      Random r = new Random(31 * 341873128712L + 0xB10C4C2AF7L);
      for (int i = 0; i < 9; i++) {
         int x = r.nextInt(N);
         int y = r.nextInt(N);
         int len = 2 + r.nextInt(4);
         for (int k = 0; k < len; k++) {
            p[(y & 15) * N + ((x + k) & 15)] = c(0x3C3834);
            if (k == 0 || r.nextInt(3) == 0) {
               p[((y - 1) & 15) * N + ((x + k) & 15)] = c(0xB8B4A2);
            }
            if (r.nextInt(3) != 0) {
               p[((y + 1) & 15) * N + ((x + k) & 15)] = c(0xBDB9A6);
            }
         }
      }
      return p;
   }

   private static int[] paintBushyFrond(float kr, float kg, float kb) {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float dx = (x + 0.5F - 8.0F) / 8.0F;
            float dy = (y + 0.5F - 8.0F) / 8.0F;
            double ang = Math.atan2(dy, dx);
            float r = (float)Math.hypot(dx, dy);
            float lobe = 0.94F + 0.14F * (float)Math.cos(ang * 5.0 + 0.4) + 0.10F * (float)Math.sin(ang * 7.0);
            if (r <= lobe) {
               if (r > 0.35F && hash(x, y, 6425) < 0.22F) {
                  continue;
               }
               int base = (int)(136.0F - dy * 16.0F + (hash(x, y, 6420) - 0.5F) * 18.0F);
               p[y * N + x] = tintRgb(rgb(base, base, base), kr, kg, kb);
            }
         }
      }
      int[][] sprays = {
         {2, 2, 0}, {6, 1, 1}, {10, 1, 0}, {12, 3, 1},
         {1, 6, 1}, {5, 5, 0}, {9, 4, 1}, {12, 7, 0},
         {1, 10, 0}, {4, 9, 1}, {8, 8, 0}, {11, 10, 1},
         {3, 12, 0}, {7, 12, 1}, {10, 12, 0}, {6, 6, 1}
      };
      for (int i = 0; i < sprays.length; i++) {
         paintLeaflet(p, sprays[i][0], sprays[i][1], sprays[i][2], 6430 + i * 17, kr, kg, kb);
      }
      return p;
   }

   private static int[] paintDoor(boolean upper) {
      int[] p = paintPlanks(c(0xA6834E), 701);
      int frame = c(0x6E502A);
      int bevL = c(0xBA9458);
      int bevD = c(0x4E361A);
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            if (x < 2 || x >= 14 || (upper ? y < 2 || y >= 13 : y < 2 || y >= 14 || y == 7 || y == 8)) {
               p[y * N + x] = (x == 0 || y == 0) ? bevL : ((x == 15 || y == 15) ? bevD : frame);
            }
         }
      }
      if (upper) {
         for (int y = 3; y < 12; y++) {
            for (int x = 3; x < 13; x++) {
               boolean bar = x == 7 || x == 8 || y == 7;
               p[y * N + x] = bar ? frame : 0;
            }
         }
      } else {
         for (int y = 3; y < 13; y++) {
            if (y == 7 || y == 8) continue;
            for (int x = 3; x < 13; x++) {
               p[y * N + x] = mul(p[y * N + x], (x == 3 || y == 3 || y == 9) ? 0.80F : 0.92F);
            }
         }
         p[1 * N + 12] = c(0xD8DCDF);
         p[1 * N + 13] = c(0x787C82);
         p[2 * N + 12] = c(0x989CA2);
      }
      return p;
   }

   private static int[] paintFarmland(boolean wet) {
      int[] p = new int[N * N];
      int[] pal = wet
         ? new int[]{c(0x28170A), c(0x362010), c(0x442A16), c(0x52341C)}
         : new int[]{c(0x583C22), c(0x6C4B2C), c(0x7E5A36), c(0x906840)};
      for (int y = 0; y < N; y++) {
         int fy = y & 3;
         float ridge = fy == 0 ? 0.15F : (fy == 1 ? 0.55F : (fy == 2 ? 0.88F : 0.45F));
         for (int x = 0; x < N; x++) {
            float v = ridge * 0.72F + hash(x, y, wet ? 203 : 170) * 0.28F;
            p[y * N + x] = step(pal, v);
         }
      }
      return p;
   }

   private static int[] paintWheat(int stage) {
      int[] p = new int[N * N];
      int h = Math.min(15, 3 + stage * 2);
      int[] xs = {1, 3, 6, 8, 11, 14};
      for (int i = 0; i < xs.length; i++) {
         int x0 = xs[i];
         int sh = Math.max(2, h - (i & 1));
         for (int k = 0; k < sh; k++) {
            int y = 15 - k;
            float frac = k / (float)Math.max(1, sh - 1);
            int col = frac > 0.5F ? (k % 2 == 0 ? c(0xF0CA4E) : c(0xD4A632)) : c(0x9E8228);
            p[y * N + x0] = col;
            if (frac > 0.55F && x0 + 1 < N) {
               p[y * N + x0 + 1] = mul(col, 0.88F);
            }
         }
      }
      return p;
   }

   private static int[] paintSugarCane() {
      int[] p = new int[N * N];
      int[] cols = {2, 6, 10, 13};
      for (int i = 0; i < cols.length; i++) {
         int x = cols[i];
         for (int y = 0; y < N; y++) {
            boolean node = (y + i * 2) % 5 == 0;
            int cL = node ? c(0xA8D870) : c(0x78B848);
            int cR = node ? c(0x82B250) : c(0x56902E);
            p[y * N + x] = cL;
            p[y * N + x + 1] = cR;
         }
      }
      return p;
   }

   private static int[] paintPumpkinSide() {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            int rib = x & 3;
            int base = rib == 0 ? c(0xAC5612) : (rib == 1 ? c(0xE47E1E) : (rib == 2 ? c(0xD46E18) : c(0xC05E14)));
            float v = (fbm(x, y, 222) - 0.5F) * 0.16F;
            if (y == 0 || y == 15) v -= 0.08F;
            p[y * N + x] = mul(base, 1.0F + v);
         }
      }
      return p;
   }

   private static int[] paintPumpkinTop() {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float dx = x - 7.5F;
            float dy = y - 7.5F;
            float r = (float)Math.hypot(dx, dy);
            if (r < 2.2F) {
               p[y * N + x] = r < 1.2F ? c(0x6E582C) : c(0x4E3E1E);
            } else {
               boolean groove = Math.abs(dx) < 0.8F || Math.abs(dy) < 0.8F || Math.abs(Math.abs(dx) - Math.abs(dy)) < 0.9F;
               int base = groove ? c(0xB45C14) : c(0xDC761C);
               p[y * N + x] = mul(base, 0.94F + 0.12F * fbm(x, y, 223));
            }
         }
      }
      return p;
   }

   private static int[] paintPumpkinFace(boolean lit) {
      int[] p = paintPumpkinSide();
      String[] mask = {
         "................",
         "................",
         "................",
         "..###......###..",
         "..####....####..",
         "...###....###...",
         ".......##.......",
         "......####......",
         "................",
         "..##..####..##..",
         ".##############.",
         "..###..##..###..",
         "...##......##...",
         "................",
         "................",
         "................"
      };
      for (int y = 0; y < N; y++) {
         String row = mask[y];
         for (int x = 0; x < N; x++) {
            if (row.charAt(x) == '#') {
               p[y * N + x] = lit ? ((y == 4 || y == 10) ? c(0xFFF498) : c(0xF8B828)) : c(0x321808);
            } else if (y > 0 && mask[y - 1].charAt(x) == '#') {
               p[y * N + x] = lit ? c(0xF08818) : c(0x7A380C);
            }
         }
      }
      return p;
   }

   private static int[] paintMelonSide() {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            int rib = x & 3;
            int base = rib == 0 ? c(0x3E681A) : (rib == 1 ? c(0x74A22C) : (rib == 2 ? c(0x88B436) : c(0x588422)));
            float v = (hash(x, y, 226) - 0.5F) * 0.14F;
            p[y * N + x] = mul(base, 1.0F + v);
         }
      }
      return p;
   }

   private static int[] paintMelonTop() {
      int[] p = paintMelonSide();
      p[7 * N + 7] = p[7 * N + 8] = p[8 * N + 7] = p[8 * N + 8] = c(0x9EB850);
      return p;
   }

   private static int[] paintHayTop() {
      int[] pal = {c(0xA68420), c(0xC29E2A), c(0xD8B436), c(0xECC844)};
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float v = vn(x * 2.0F + 0.5F, y + 0.5F, 8, 212) * 0.65F + hash(x, y, 213) * 0.35F;
            p[y * N + x] = step(pal, contrast(v, 2.0F));
         }
      }
      return p;
   }

   private static int[] paintHaySide() {
      int[] p = paintHayTop();
      for (int y : new int[]{3, 4, 11, 12}) {
         for (int x = 0; x < N; x++) {
            p[y * N + x] = (y == 3 || y == 11) ? c(0xB83224) : c(0x862218);
         }
      }
      return p;
   }

   private static int[] paintSunTile() {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            int d = Math.max(Math.abs(x - 7) - (x >= 8 ? 1 : 0), Math.abs(y - 7) - (y >= 8 ? 1 : 0));
            if (d <= 4) {
               p[y * N + x] = d <= 2 ? c(0xFFFFE8) : c(0xFFE878);
            } else if (d <= 6) {
               p[y * N + x] = (180 << 24) | 0xFFCE42;
            } else if (d == 7) {
               p[y * N + x] = (72 << 24) | 0xFFA824;
            }
         }
      }
      return p;
   }

   private static int[] paintMoonTile() {
      int[] p = new int[N * N];
      for (int y = 2; y <= 13; y++) {
         for (int x = 2; x <= 13; x++) {
            boolean rim = x == 2 || x == 13 || y == 2 || y == 13;
            boolean crater = (x >= 5 && x <= 7 && y >= 5 && y <= 6) || (x >= 9 && x <= 11 && y >= 8 && y <= 10) || (x == 6 && y == 10);
            p[y * N + x] = rim ? c(0xB8C6DC) : (crater ? c(0x98A8C2) : c(0xE8F0FF));
         }
      }
      return p;
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
