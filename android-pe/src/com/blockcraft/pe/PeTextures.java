package com.blockcraft.pe;

import android.content.Context;
import android.opengl.GLES20;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;

/**
 * Blockcraft PE (Pocket Edition) OpenGL ES Texture & Font Atlas Generator.
 * Generates a crisp 256x256 (16x16 tiles of 16x16px) procedural block/item atlas and a 128x128 bitmap font texture
 * with GL_NEAREST filtering for sharp pixel-art rendering on Samsung Galaxy A55 5G (1080x2340 Super AMOLED).
 */
public final class PeTextures {
   public static final int ATLAS_W = 256;
   public static final int ATLAS_H = 256;
   public static final int TILE_SIZE = 16;

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

      // 1. Build 256x256 Block & Item Texture Atlas
      int[] pixels = generateAtlasPixels();
      uploadTexture(this.atlasTexId, ATLAS_W, ATLAS_H, pixels);

      // 2. Build 128x128 Bitmap Font Texture (from assets/font.bin or built-in 5x7 fallback)
      int[] fontPixels = generateFontPixels(context);
      uploadTexture(this.fontTexId, 128, 128, fontPixels);

      // 3. 4x4 Pure White Texture for untextured UI / sky / solid quads
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
      GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
      GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, w, h, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buf);
   }

   public static int[] generateAtlasPixels() {
      int[] px = new int[ATLAS_W * ATLAS_H];
      Random rng = new Random(0xB10CCAFEL);

      // Fill базовые noisy stone/dirt/grass/wood/brick/ore tiles
      paintNoiseTile(px, T_GRASS_TOP, 0xFF59A83B, 18, rng);
      paintNoiseTile(px, T_STONE, 0xFF7D7D7D, 16, rng);
      paintNoiseTile(px, T_DIRT, 0xFF866043, 18, rng);
      paintGrassSide(px, T_GRASS_SIDE, rng);
      paintPlanks(px, T_PLANKS_OAK, 0xFFA2824E, 0xFF7E6238, rng);
      paintPlanks(px, T_PLANKS_SPRUCE, 0xFF6E5031, 0xFF523B22, rng);
      paintPlanks(px, T_PLANKS_BIRCH, 0xFFD5C68C, 0xFFB5A56E, rng);
      paintSlabSide(px, T_STONE_SLAB_SIDE, rng);
      paintNoiseTile(px, T_STONE_SLAB_TOP, 0xFF9E9E9E, 10, rng);
      paintBricks(px, T_BRICKS, 0xFF964A3B, 0xFFB6B0A6, rng);
      paintTntSide(px, T_TNT_SIDE);
      paintTntTop(px, T_TNT_TOP);
      paintNoiseTile(px, T_TNT_BOTTOM, 0xFFA4382A, 14, rng);
      paintFluid(px, T_WATER, 0xCC2868D8, 0xCC3C82F2, rng);
      paintFluid(px, T_LAVA, 0xFFEE5A14, 0xFFFFB422, rng);

      paintCobble(px, T_COBBLE, 0xFF747474, false, rng);
      paintCobble(px, T_BEDROCK, 0xFF343434, false, rng);
      paintNoiseTile(px, T_SAND, 0xFFDBD3A0, 14, rng);
      paintNoiseTile(px, T_GRAVEL, 0xFF827E7C, 22, rng);
      paintLogSide(px, T_LOG_OAK_SIDE, 0xFF665132, 0xFF4F3E25, rng);
      paintLogTop(px, T_LOG_OAK_TOP, 0xFFA48552, 0xFF665132);
      paintLogSide(px, T_LOG_SPRUCE, 0xFF3B2816, 0xFF2A1C0E, rng);
      paintMetalBlock(px, T_IRON_BLOCK, 0xFFDCDCDC, 0xFFB4B4B4);
      paintMetalBlock(px, T_GOLD_BLOCK, 0xFFF9D835, 0xFFD6A319);
      paintMetalBlock(px, T_DIAMOND_BLOCK, 0xFF5CE8E2, 0xFF2CB5AF);
      paintMetalBlock(px, T_EMERALD_BLOCK, 0xFF28D862, 0xFF149E42);
      paintChest(px, T_CHEST_TOP, T_CHEST_SIDE, T_CHEST_FRONT);

      paintOre(px, T_GOLD_ORE, 0xFFFCEE4B, rng);
      paintOre(px, T_IRON_ORE, 0xFFD8AF93, rng);
      paintOre(px, T_COAL_ORE, 0xFF282828, rng);
      paintOre(px, T_DIAMOND_ORE, 0xFF5DECF5, rng);
      paintOre(px, T_REDSTONE_ORE, 0xFFE82626, rng);
      paintBookshelf(px, T_BOOKSHELF, rng);
      paintCobble(px, T_MOSSY_COBBLE, 0xFF6E766E, true, rng);
      paintNoiseTile(px, T_OBSIDIAN, 0xFF1B1429, 15, rng);
      paintCraftingTable(px, T_CRAFT_TOP, T_CRAFT_SIDE);
      paintFurnace(px, T_FURNACE_FRONT, T_FURNACE_SIDE, rng);

      paintGlass(px, T_GLASS);
      paintLeaves(px, T_LEAVES_OAK, 0xFF3C8927, rng);
      paintStoneBrick(px, T_STONEBRICK, false, false, rng);
      paintStoneBrick(px, T_CRACKED_STONEBRICK, true, false, rng);
      paintStoneBrick(px, T_MOSSY_STONEBRICK, false, true, rng);

      paintNoiseTile(px, T_WOOL_WHITE, 0xFFEAEAEA, 10, rng);
      paintNoiseTile(px, T_WOOL_RED, 0xFFB82C26, 12, rng);
      paintNoiseTile(px, T_WOOL_BLUE, 0xFF2E44AA, 12, rng);
      paintNoiseTile(px, T_WOOL_GREEN, 0xFF3E8E2A, 12, rng);
      paintNoiseTile(px, T_WOOL_YELLOW, 0xFFDCC42A, 12, rng);
      paintNoiseTile(px, T_SNOW, 0xFFF4FAFA, 8, rng);
      paintNoiseTile(px, T_ICE, 0xCC82B4F8, 12, rng);
      paintNoiseTile(px, T_PACKED_ICE, 0xFF8CB6F2, 10, rng);
      paintSandstone(px, T_SANDSTONE_TOP, T_SANDSTONE_SIDE, 0xFFDAD098, 0xFFC5BA80, rng);
      paintSandstone(px, T_RED_SANDSTONE, T_RED_SANDSTONE, 0xFFB85D28, 0xFF9E4B1D, rng);
      paintTorch(px, T_TORCH);
      paintNoiseTile(px, T_GLOWSTONE, 0xFFF2C854, 34, rng);
      paintNoiseTile(px, T_NETHERRACK, 0xFF6F3432, 22, rng);
      paintNoiseTile(px, T_SOUL_SAND, 0xFF544033, 20, rng);
      paintBricks(px, T_NETHER_BRICKS, 0xFF341A1E, 0xFF221014, rng);
      paintNoiseTile(px, T_END_STONE, 0xFFDCE09E, 18, rng);
      paintMetalBlock(px, T_QUARTZ_BLOCK, 0xFFEEEAE2, 0xFFD8D2C5);
      paintMetalBlock(px, T_SEA_LANTERN, 0xFFC8F0E8, 0xFF72C2B8);
      paintNoiseTile(px, T_PRISMARINE, 0xFF589C8E, 22, rng);
      paintMetalBlock(px, T_SLIME_BLOCK, 0xCC70C956, 0xCC52A33B);
      paintMetalBlock(px, T_REDSTONE_BLOCK, 0xFFC91E14, 0xFF8E120A);
      paintNoiseTile(px, T_COAL_BLOCK, 0xFF1E1E1E, 12, rng);

      paintBed(px, T_BED_RED);
      paintNoiseTile(px, T_TERRACOTTA, 0xFF9E6246, 10, rng);
      paintNoiseTile(px, T_CONCRETE_GRAY, 0xFF424649, 6, rng);
      paintNoiseTile(px, T_CONCRETE_YELLOW, 0xFFF0B418, 6, rng);
      paintNoiseTile(px, T_CONCRETE_BLUE, 0xFF2C3E98, 6, rng);
      paintNoiseTile(px, T_CONCRETE_RED, 0xFFA22420, 6, rng);
      paintStoneBrick(px, T_PURPUR, false, false, rng);
      paintLadder(px, T_LADDER);
      paintBars(px, T_BARS);

      // Block crack stages (0..9)
      for (int s = 0; s < 10; s++) {
         paintCrackStage(px, T_CRACK_0 + s, s);
      }

      // Weapons & Items
      paintSword(px, T_ITEM_SWORD_WOOD, 0xFF9C7A44);
      paintSword(px, T_ITEM_SWORD_STONE, 0xFF909090);
      paintSword(px, T_ITEM_SWORD_IRON, 0xFFE2E2E2);
      paintSword(px, T_ITEM_SWORD_DIAMOND, 0xFF4AECE2);
      paintPickaxe(px, T_ITEM_PICK_WOOD, 0xFF9C7A44);
      paintPickaxe(px, T_ITEM_PICK_STONE, 0xFF909090);
      paintPickaxe(px, T_ITEM_PICK_IRON, 0xFFE2E2E2);
      paintPickaxe(px, T_ITEM_PICK_DIAMOND, 0xFF4AECE2);
      paintShovel(px, T_ITEM_SHOVEL_IRON, 0xFFE2E2E2);
      paintShovel(px, T_ITEM_SHOVEL_DIAMOND, 0xFF4AECE2);
      paintAxe(px, T_ITEM_AXE_IRON, 0xFFE2E2E2);
      paintBow(px, T_ITEM_BOW);
      paintArrow(px, T_ITEM_ARROW);
      paintRoundItem(px, T_ITEM_GOLDEN_APPLE, 0xFFFAD62A, 0xFFFFF280);
      paintRoundItem(px, T_ITEM_STEAK, 0xFF8C3E26, 0xFFC26A48);
      paintRoundItem(px, T_ITEM_ENDER_PEARL, 0xFF188C78, 0xFF4CF0D2);
      paintRoundItem(px, T_ITEM_SNOWBALL, 0xFFE8F4F8, 0xFFFFFFFF);
      paintGun(px, T_ITEM_PISTOL, 0xFF3E4248, 0xFF707680, false);
      paintGun(px, T_ITEM_MACHINE_GUN, 0xFF282C30, 0xFFD88A28, true);
      paintGun(px, T_ITEM_SNIPER, 0xFF345238, 0xFF1E2226, true);
      paintRoundItem(px, T_ITEM_TNT_GRENADE, 0xFFD83224, 0xFFFFE040);
      paintRoundItem(px, T_ITEM_C4_BOMB, 0xFF4A5838, 0xFFFF2424);
      paintGun(px, T_ITEM_SIEGE_CANNON, 0xFF222226, 0xFFF2C028, true);
      paintGun(px, T_ITEM_MORTAR, 0xFF3E4838, 0xFFE86A1C, true);
      paintRoundItem(px, T_HEART_FULL, 0xFFE82424, 0xFFFF8888);
      paintRoundItem(px, T_HEART_EMPTY, 0xFF382020, 0xFF604040);

      return px;
   }

   private static void putPx(int[] px, int tile, int x, int y, int argb) {
      if (x < 0 || x >= 16 || y < 0 || y >= 16) {
         return;
      }
      int tx = (tile & 15) * 16 + x;
      int ty = (tile >> 4) * 16 + y;
      px[ty * ATLAS_W + tx] = argb;
   }

   private static int shade(int argb, int delta) {
      int a = (argb >>> 24) & 0xFF;
      int r = Math.max(0, Math.min(255, ((argb >>> 16) & 0xFF) + delta));
      int g = Math.max(0, Math.min(255, ((argb >>> 8) & 0xFF) + delta));
      int b = Math.max(0, Math.min(255, (argb & 0xFF) + delta));
      return (a << 24) | (r << 16) | (g << 8) | b;
   }

   private static void paintNoiseTile(int[] px, int tile, int baseColor, int variance, Random rng) {
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            int d = rng.nextInt(variance * 2 + 1) - variance;
            putPx(px, tile, x, y, shade(baseColor, d));
         }
      }
   }

   private static void paintGrassSide(int[] px, int tile, Random rng) {
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            int fringe = 2 + ((x * 7 + 3) & 3);
            if (y < fringe) {
               putPx(px, tile, x, y, shade(0xFF59A83B, rng.nextInt(25) - 12));
            } else {
               putPx(px, tile, x, y, shade(0xFF866043, rng.nextInt(25) - 12));
            }
         }
      }
   }

   private static void paintPlanks(int[] px, int tile, int col, int seamCol, Random rng) {
      for (int y = 0; y < 16; y++) {
         boolean hSeam = (y & 3) == 3;
         int vJoint = ((y >> 2) * 5 + 3) & 15;
         for (int x = 0; x < 16; x++) {
            if (hSeam || x == vJoint) {
               putPx(px, tile, x, y, seamCol);
            } else {
               putPx(px, tile, x, y, shade(col, rng.nextInt(15) - 7));
            }
         }
      }
   }

   private static void paintSlabSide(int[] px, int tile, Random rng) {
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            int c = (y == 7 || y == 8) ? 0xFF585858 : shade(0xFF9A9A9A, rng.nextInt(14) - 7);
            putPx(px, tile, x, y, c);
         }
      }
   }

   private static void paintBricks(int[] px, int tile, int brickCol, int mortarCol, Random rng) {
      for (int y = 0; y < 16; y++) {
         boolean mortarY = (y & 3) == 3;
         int row = y >> 2;
         for (int x = 0; x < 16; x++) {
            boolean mortarX = ((x + (row & 1) * 4) & 7) == 7;
            if (mortarY || mortarX) {
               putPx(px, tile, x, y, mortarCol);
            } else {
               putPx(px, tile, x, y, shade(brickCol, rng.nextInt(18) - 9));
            }
         }
      }
   }

   private static void paintTntSide(int[] px, int tile) {
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            if (y >= 5 && y <= 10) {
               boolean darkText = (y >= 6 && y <= 9) && (x == 3 || x == 7 || x == 8 || x == 12);
               putPx(px, tile, x, y, darkText ? 0xFF181818 : 0xFFF2F2F2);
            } else {
               putPx(px, tile, x, y, ((x >> 1) & 1) == 0 ? 0xFFD82E22 : 0xFFB82218);
            }
         }
      }
   }

   private static void paintTntTop(int[] px, int tile) {
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            boolean fuse = Math.abs(x - 7) <= 1 && Math.abs(y - 7) <= 1;
            putPx(px, tile, x, y, fuse ? 0xFF222222 : 0xFFD43022);
         }
      }
   }

   private static void paintFluid(int[] px, int tile, int c1, int c2, Random rng) {
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            boolean wave = ((x + y * 2) & 3) == 0;
            putPx(px, tile, x, y, shade(wave ? c2 : c1, rng.nextInt(12) - 6));
         }
      }
   }

   private static void paintCobble(int[] px, int tile, int base, boolean mossy, Random rng) {
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            boolean seam = ((x * 3 + y * 5) & 7) == 0;
            int col = seam ? shade(base, -28) : shade(base, rng.nextInt(26) - 13);
            if (mossy && ((x * 7 + y * 11) & 5) == 0) {
               col = 0xFF4A7C2E;
            }
            putPx(px, tile, x, y, col);
         }
      }
   }

   private static void paintLogSide(int[] px, int tile, int c1, int c2, Random rng) {
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            int col = ((x & 3) == 0) ? c2 : shade(c1, rng.nextInt(14) - 7);
            putPx(px, tile, x, y, col);
         }
      }
   }

   private static void paintLogTop(int[] px, int tile, int inner, int bark) {
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            boolean edge = x <= 1 || x >= 14 || y <= 1 || y >= 14;
            boolean ring = Math.abs(x - 7) == 4 || Math.abs(y - 7) == 4;
            putPx(px, tile, x, y, edge ? bark : (ring ? shade(inner, -16) : inner));
         }
      }
   }

   private static void paintMetalBlock(int[] px, int tile, int main, int border) {
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            boolean edge = x == 0 || y == 0 || x == 15 || y == 15;
            boolean highlight = (x + y) % 5 == 0;
            putPx(px, tile, x, y, edge ? border : (highlight ? shade(main, 16) : main));
         }
      }
   }

   private static void paintChest(int[] px, int topT, int sideT, int frontT) {
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            boolean border = x == 0 || x == 15 || y == 0 || y == 15;
            int wood = border ? 0xFF422C16 : 0xFF9C6E34;
            putPx(px, topT, x, y, wood);
            int sideC = (border || y == 5) ? 0xFF3E2814 : 0xFF9C6E34;
            putPx(px, sideT, x, y, sideC);
            boolean latch = x >= 7 && x <= 8 && y >= 4 && y <= 7;
            putPx(px, frontT, x, y, latch ? 0xFFF2D43B : sideC);
         }
      }
   }

   private static void paintOre(int[] px, int tile, int speckCol, Random rng) {
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            boolean speck = x >= 2 && x <= 13 && y >= 2 && y <= 13 && ((x * 13 + y * 7) % 11 == 0 || (x * 5 + y * 17) % 13 == 0);
            putPx(px, tile, x, y, speck ? speckCol : shade(0xFF7D7D7D, rng.nextInt(16) - 8));
         }
      }
   }

   private static void paintBookshelf(int[] px, int tile, Random rng) {
      int[] bookCols = new int[]{0xFFB82A2A, 0xFF2858B8, 0xFF2A9038, 0xFFD4A828, 0xFF783298};
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            if (y <= 1 || y >= 14 || y == 7 || y == 8) {
               putPx(px, tile, x, y, shade(0xFFA2824E, rng.nextInt(10) - 5));
            } else {
               int bCol = (x & 1) == 0 ? 0xFF382818 : bookCols[(x / 2 + (y >> 3) * 3) % bookCols.length];
               putPx(px, tile, x, y, bCol);
            }
         }
      }
   }

   private static void paintCraftingTable(int[] px, int topT, int sideT) {
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            boolean grid = (x >= 3 && x <= 12 && y >= 3 && y <= 12) && ((x - 3) % 3 == 0 || (y - 3) % 3 == 0);
            putPx(px, topT, x, y, grid ? 0xFF4E341C : 0xFFA67844);
            boolean topBand = y < 4;
            putPx(px, sideT, x, y, topBand ? 0xFF7A522C : 0xFFA2824E);
         }
      }
   }

   private static void paintFurnace(int[] px, int frontT, int sideT, Random rng) {
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            int st = shade(0xFF747474, rng.nextInt(16) - 8);
            putPx(px, sideT, x, y, st);
            boolean hole = x >= 4 && x <= 11 && ((y >= 3 && y <= 6) || (y >= 9 && y <= 13));
            putPx(px, frontT, x, y, hole ? (y >= 11 ? 0xFFF26C18 : 0xFF202020) : st);
         }
      }
   }

   private static void paintGlass(int[] px, int tile) {
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            boolean frame = x == 0 || y == 0 || x == 15 || y == 15 || (x == 3 && y >= 2 && y <= 5) || (y == 3 && x >= 2 && x <= 5);
            putPx(px, tile, x, y, frame ? 0xFFD8F4FC : 0x18FFFFFF);
         }
      }
   }

   private static void paintLeaves(int[] px, int tile, int base, Random rng) {
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            boolean gap = ((x * 5 + y * 3) & 7) == 0;
            putPx(px, tile, x, y, gap ? 0xFF1D4812 : shade(base, rng.nextInt(26) - 13));
         }
      }
   }

   private static void paintStoneBrick(int[] px, int tile, boolean cracked, boolean mossy, Random rng) {
      for (int y = 0; y < 16; y++) {
         boolean seamY = y == 0 || y == 8;
         for (int x = 0; x < 16; x++) {
            boolean seamX = (y < 8 ? (x == 0 || x == 8) : (x == 4 || x == 12));
            int c = (seamY || seamX) ? 0xFF505050 : shade(0xFF828282, rng.nextInt(14) - 7);
            if (cracked && (x + y == 14 || x - y == 3)) {
               c = 0xFF424242;
            }
            if (mossy && ((x * 7 + y * 5) & 7) == 0) {
               c = 0xFF4E8232;
            }
            putPx(px, tile, x, y, c);
         }
      }
   }

   private static void paintSandstone(int[] px, int topT, int sideT, int c1, int c2, Random rng) {
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            putPx(px, topT, x, y, shade(c1, rng.nextInt(12) - 6));
            putPx(px, sideT, x, y, y < 4 ? c1 : shade(c2, rng.nextInt(14) - 7));
         }
      }
   }

   private static void paintTorch(int[] px, int tile) {
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            if (x >= 7 && x <= 8 && y >= 6 && y <= 15) {
               putPx(px, tile, x, y, y <= 7 ? 0xFFFFE850 : (y == 8 ? 0xFFF27818 : 0xFF7E5A2E));
            } else {
               putPx(px, tile, x, y, 0x00000000);
            }
         }
      }
   }

   private static void paintBed(int[] px, int tile) {
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            putPx(px, tile, x, y, y < 4 ? 0xFFF0F0F0 : 0xFFC82822);
         }
      }
   }

   private static void paintLadder(int[] px, int tile) {
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            boolean wood = x == 2 || x == 3 || x == 12 || x == 13 || ((y & 3) == 1 && x >= 2 && x <= 13);
            putPx(px, tile, x, y, wood ? 0xFF8E6C3A : 0x00000000);
         }
      }
   }

   private static void paintBars(int[] px, int tile) {
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            boolean bar = (x & 3) == 1 || y == 0 || y == 8 || y == 15;
            putPx(px, tile, x, y, bar ? 0xFF989CA0 : 0x00000000);
         }
      }
   }

   private static void paintCrackStage(int[] px, int tile, int stage) {
      int reach = 2 + stage;
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            int dx = Math.abs(x - 8);
            int dy = Math.abs(y - 8);
            boolean crack = (dx <= reach && dy <= reach) && (dx == dy || (stage >= 4 && (x == 8 || y == 8)));
            putPx(px, tile, x, y, crack ? 0xB0181818 : 0x00000000);
         }
      }
   }

   private static void paintSword(int[] px, int tile, int bladeCol) {
      for (int i = 2; i <= 12; i++) {
         putPx(px, tile, i, 15 - i, i <= 4 ? 0xFF6E4E28 : bladeCol);
         if (i >= 5) {
            putPx(px, tile, i - 1, 15 - i, shade(bladeCol, 24));
            putPx(px, tile, i, 16 - i, shade(bladeCol, -24));
         }
      }
      putPx(px, tile, 3, 10, 0xFF523A1C);
      putPx(px, tile, 5, 12, 0xFF523A1C);
   }

   private static void paintPickaxe(int[] px, int tile, int headCol) {
      for (int i = 2; i <= 12; i++) {
         putPx(px, tile, i, 15 - i, 0xFF6E4E28);
      }
      for (int k = -3; k <= 3; k++) {
         putPx(px, tile, 11 + (k < 0 ? k : 0), 4 + (k > 0 ? k : 0), headCol);
         putPx(px, tile, 10 + (k < 0 ? k : 0), 5 + (k > 0 ? k : 0), shade(headCol, -20));
      }
   }

   private static void paintShovel(int[] px, int tile, int headCol) {
      for (int i = 2; i <= 11; i++) {
         putPx(px, tile, i, 15 - i, 0xFF6E4E28);
      }
      for (int dy = 2; dy <= 5; dy++) {
         for (int dx = 10; dx <= 13; dx++) {
            putPx(px, tile, dx, dy, headCol);
         }
      }
   }

   private static void paintAxe(int[] px, int tile, int headCol) {
      for (int i = 2; i <= 12; i++) {
         putPx(px, tile, i, 15 - i, 0xFF6E4E28);
      }
      for (int dy = 2; dy <= 7; dy++) {
         for (int dx = 8; dx <= 12; dx++) {
            putPx(px, tile, dx, dy, headCol);
         }
      }
   }

   private static void paintBow(int[] px, int tile) {
      for (int i = 3; i <= 12; i++) {
         putPx(px, tile, i, i, 0xFFD8D8D8);
         int arcX = Math.min(14, i + (int)Math.round(Math.sin((i - 3) / 9.0 * Math.PI) * 4.0));
         putPx(px, tile, arcX, i, 0xFF7E582C);
      }
   }

   private static void paintArrow(int[] px, int tile) {
      for (int i = 3; i <= 12; i++) {
         putPx(px, tile, i, 15 - i, i >= 11 ? 0xFFD8D8D8 : (i <= 4 ? 0xFFF2F2F2 : 0xFF8E6838));
      }
   }

   private static void paintRoundItem(int[] px, int tile, int col, int hi) {
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            double d = Math.hypot(x - 7.5, y - 7.5);
            if (d <= 5.2) {
               putPx(px, tile, x, y, (x <= 6 && y <= 6) ? hi : col);
            }
         }
      }
   }

   private static void paintGun(int[] px, int tile, int bodyCol, int accentCol, boolean longBarrel) {
      int minX = longBarrel ? 1 : 3;
      for (int x = minX; x <= 13; x++) {
         for (int y = 6; y <= 8; y++) {
            putPx(px, tile, x, y, y == 6 ? accentCol : bodyCol);
         }
      }
      for (int y = 9; y <= 13; y++) {
         putPx(px, tile, 10, y, bodyCol);
         putPx(px, tile, 11, y, bodyCol);
         if (longBarrel && y <= 12) {
            putPx(px, tile, 6, y, accentCol);
         }
      }
   }

   public int measureText(String s) {
      if (s == null) return 0;
      int w = 0;
      for (int i = 0; i < s.length(); i++) {
         char c = s.charAt(i);
         if (c >= 32 && c < 128) {
            w += this.fontWidths[c] + 1;
         }
      }
      return w;
   }

   public int[] generateFontPixels(Context context) {
      int[] px = new int[128 * 128];
      for (int i = 0; i < 128; i++) {
         this.fontWidths[i] = 6;
      }
      this.fontWidths[' '] = 4;
      try {
         InputStream in = null;
         if (context != null) {
            try {
               in = context.getAssets().open("font.bin");
            } catch (Exception ignored) {
            }
         }
         if (in == null) {
            in = PeTextures.class.getResourceAsStream("/font.bin");
         }
         if (in != null) {
            byte[] data = new byte[96 * 9];
            int off = 0;
            while (off < data.length) {
               int r = in.read(data, off, data.length - off);
               if (r <= 0) break;
               off += r;
            }
            in.close();
            if (off >= 96 * 9) {
               for (int ch = 32; ch < 128; ch++) {
                  int idx = (ch - 32) * 9;
                  this.fontWidths[ch] = Math.max(2, Math.min(8, data[idx] & 0xFF));
                  int gx = (ch & 15) * 8;
                  int gy = (ch >> 4) * 8;
                  for (int row = 0; row < 8; row++) {
                     int bits = data[idx + 1 + row] & 0xFF;
                     for (int col = 0; col < 8; col++) {
                        if ((bits & (1 << col)) != 0) {
                           px[(gy + row) * 128 + (gx + col)] = 0xFFFFFFFF;
                        }
                     }
                  }
               }
               return px;
            }
         }
      } catch (Exception ignored) {
      }
      return px;
   }
}
