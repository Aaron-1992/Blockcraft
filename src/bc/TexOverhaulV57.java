package bc;

import java.util.Random;

/**
 * Update V5.7 Master Pixel-Art Texture Overhaul:
 * Brand-new hand-crafted 16x16 pixel-art textures for every single block, ore, wood species,
 * crop, work block, Redstone mechanism, tool, weapon, armor piece, food item, and material.
 */
final class TexOverhaulV57 {
   private static final int N = 16;

   private TexOverhaulV57() {
   }

   private static int c(int rgb) {
      return 0xFF000000 | (rgb & 0xFFFFFF);
   }

   private static int rgb(int r, int g, int b) {
      return TexPaint.rgb(r, g, b);
   }

   private static int clamp(int v) {
      return TexPaint.clamp(v);
   }

   private static int mul(int col, float k) {
      return TexPaint.mul(col, k);
   }

   private static int add(int col, int d) {
      return TexPaint.add(col, d);
   }

   private static int mix(int a, int b, float t) {
      return TexPaint.mix(a, b, t);
   }

   private static int step(int[] pal, float v) {
      return TexPaint.step(pal, v);
   }

   private static float hash(int x, int y, int seed) {
      return TexPaint.hash(x, y, seed);
   }

   private static float vn(float x, float y, int cells, int seed) {
      return TexPaint.vn(x, y, cells, seed);
   }

   private static float fbm(int x, int y, int seed) {
      return TexPaint.fbm(x, y, seed);
   }

   private static float contrast(float v, float k) {
      return TexPaint.contrast(v, k);
   }

   private static void set(int[] p, int x, int y, int col) {
      if (x >= 0 && x < N && y >= 0 && y < N) {
         p[y * N + x] = col;
      }
   }

   private static void setw(int[] p, int x, int y, int col) {
      p[(y & 15) * N + (x & 15)] = col;
   }

   private static void rect(int[] p, int x, int y, int w, int h, int col) {
      for (int dy = 0; dy < h; dy++) {
         for (int dx = 0; dx < w; dx++) {
            set(p, x + dx, y + dy, col);
         }
      }
   }

   private static void line(int[] p, int x0, int y0, int x1, int y1, int col) {
      int dx = Math.abs(x1 - x0);
      int dy = Math.abs(y1 - y0);
      int sx = x0 < x1 ? 1 : -1;
      int sy = y0 < y1 ? 1 : -1;
      int err = dx - dy;
      while (true) {
         set(p, x0, y0, col);
         if (x0 == x1 && y0 == y1) {
            return;
         }
         int e2 = 2 * err;
         if (e2 > -dy) {
            err -= dy;
            x0 += sx;
         }
         if (e2 < dx) {
            err += dx;
            y0 += sy;
         }
      }
   }

   /** Adds a crisp dark pixel-art outline around any non-transparent sprite silhouette and bevels interior pixels. */
   private static int[] outlineAndBevelSprite(int[] raw, int outlineRgb, int seed) {
      int[] out = raw.clone();
      int outline = c(outlineRgb);
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            if ((raw[y * N + x] >>> 24) != 0) {
               continue;
            }
            boolean adj = (x > 0 && (raw[y * N + x - 1] >>> 24) != 0)
               || (x < 15 && (raw[y * N + x + 1] >>> 24) != 0)
               || (y > 0 && (raw[(y - 1) * N + x] >>> 24) != 0)
               || (y < 15 && (raw[(y + 1) * N + x] >>> 24) != 0);
            if (adj) {
               boolean br = (x > 0 && (raw[y * N + x - 1] >>> 24) != 0) || (y > 0 && (raw[(y - 1) * N + x] >>> 24) != 0);
               out[y * N + x] = br ? mul(outline, 0.78F) : outline;
            }
         }
      }
      for (int y = 1; y < 15; y++) {
         for (int x = 1; x < 15; x++) {
            int p = raw[y * N + x];
            if ((p >>> 24) == 0) {
               continue;
            }
            boolean tlOpen = (raw[(y - 1) * N + x] >>> 24) == 0 || (raw[y * N + x - 1] >>> 24) == 0;
            boolean brOpen = (raw[(y + 1) * N + x] >>> 24) == 0 || (raw[y * N + x + 1] >>> 24) == 0;
            float k = 1.0F + (hash(x, y, seed) - 0.5F) * 0.05F;
            if (tlOpen && !brOpen) {
               k += 0.14F;
            } else if (brOpen && !tlOpen) {
               k -= 0.12F;
            }
            out[y * N + x] = mul(p, k);
         }
      }
      return out;
   }

   // ====================================================================
   //  Master Entry Point — Repaints All Blocks, Tools, Armor & Items
   // ====================================================================
   static void applyAll() {
      // 1. Base & Overworld Terrain Blocks (Tiles 0..63)
      Tex.blit(Tex.GRASS_TOP, grassTopNew());
      Tex.blit(Tex.DIRT, dirtNew());
      Tex.blit(Tex.GRASS_SIDE_OVL, grassSideOvlNew());
      Tex.blit(Tex.STONE, stoneNew(101));
      Tex.blit(Tex.COBBLE, cobbleNew(false));
      Tex.blit(Tex.MOSSY, cobbleNew(true));
      Tex.blit(Tex.SAND, sandNew(false));
      Tex.blit(Tex.GRAVEL, gravelNew());
      Tex.blit(Tex.LOG_SIDE, TexPaint.oakSide());
      Tex.blit(Tex.LOG_TOP, logTopNew(c(0x4A361E), c(0x684E2C), c(0xC6A268), c(0xAD884E), c(0x8E6B38)));
      Tex.blit(Tex.SPRUCE_SIDE, TexPaint.spruceSide());
      Tex.blit(Tex.SPRUCE_TOP, logTopNew(c(0x2A1C0E), c(0x3E2B16), c(0x9A7648), c(0x7E5E36), c(0x644826)));
      Tex.blit(Tex.BIRCH_SIDE, TexPaint.birchSide());
      // Ensure turned logs (246..248) match TexArt310.bark(0..2)
      TexArt310.blocks();

      Tex.blit(Tex.PLANKS, planksNew(c(0xB89056), c(0xC8A266), c(0x9E7842), c(0x6E5028), 510));
      Tex.blit(Tex.BEDROCK, bedrockNew());
      Tex.blit(Tex.COAL, oreNew(c(0x2A2A30), c(0x121216), c(0x646470), c(0x42424C), 120));
      Tex.blit(Tex.IRON, oreNew(c(0xE2BA98), c(0xA67856), c(0xFFF0DF), c(0xC69874), 130));
      Tex.blit(Tex.GOLD, oreNew(c(0xFFE038), c(0xC48E08), c(0xFFFFB8), c(0xE8B81C), 140));
      Tex.blit(Tex.DIAMOND, oreNew(c(0x4CF4EC), c(0x189CA4), c(0xE4FFFF), c(0x2CC8CE), 150));
      Tex.blit(Tex.REDSTONE, oreNew(c(0xEC1E1E), c(0x880606), c(0xFF8878), c(0xBA1010), 160));
      Tex.blit(Tex.LAPIS, oreNew(c(0x2E5CE0), c(0x142E8A), c(0x8EB4FF), c(0xD8B848), 170));
      Tex.blit(Color312.T_EMERALD_ORE, oreNew(c(0x28EE68), c(0x0C8E34), c(0xB8FFD0), c(0x18BE4E), 180));

      Tex.blit(Tex.SNOW, snowNew());
      Tex.blit(Tex.GLASS, glassNew());
      Tex.blit(Tex.BRICKS, bricksNew());
      Tex.blit(Tex.SANDSTONE_SIDE, sandstoneSideNew(false));
      Tex.blit(Tex.SANDSTONE_TOP, sandstoneTopNew(false));
      Tex.blit(Tex.GLOWSTONE, glowstoneNew());
      Tex.blit(Tex.OBSIDIAN, obsidianNew());
      Tex.blit(Tex.STONEBRICK, stoneBrickNew(0));

      for (int i = 0; i < 16; i++) {
         Tex.blit(Tex.WOOL0 + i, woolNew(i));
      }

      // 2. Work Blocks & Furniture
      Tex.blit(Tex.CRAFT_TOP, craftTopNew());
      Tex.blit(Tex.CRAFT_SIDE, craftSideNew());
      Tex.blit(Tex.FURNACE_FRONT, furnaceFrontNew(false));
      Tex.blit(Tex.FURNACE_ON, furnaceFrontNew(true));
      Tex.blit(Tex.CHEST_TOP, chestTopNew());
      Tex.blit(Tex.CHEST_SIDE, chestSideNew());
      Tex.blit(Tex.SPAWNER, spawnerNew());
      Tex.blit(220, bookshelfNew());
      Tex.blit(Tex.TNT_SIDE, tntSideNew());
      Tex.blit(Tex.TNT_TOP, tntTopNew(true));
      Tex.blit(Tex.TNT_BOTTOM, tntTopNew(false));
      Tex.blit(Tex.LADDER, ladderNew());
      Tex.blit(Tex.DOOR_LO, doorNew(false));
      Tex.blit(Tex.DOOR_HI, doorNew(true));
      Tex.blit(Tex.TORCH, torchNew(false));

      // 3. Woods, Leaves, Stones, Plants, Crops, Nether, End & Biome Surfaces
      Tex.blit(Tex.LEAVES, leavesNew(0));
      for (int k = 0; k < 3; k++) {
         Tex.blit(TexArt313.LEAVES0 + k, leavesNew(k + 1));
      }
      Tex.blit(TexPaint319.BUSHY_FROND, bushyFrondNew());
      Tex.blit(Tex.TALLGRASS, tallGrassNew(false));
      Tex.blit(TexArt313.FERN, tallGrassNew(true));
      Tex.blit(TexArt313.DEAD_BUSH, deadBushNew());
      Tex.blit(Tex.FLOWER_RED, flowerNew(0));
      Tex.blit(Tex.FLOWER_YELLOW, flowerNew(1));
      for (int f = 0; f < 10; f++) {
         Tex.blit(TexArt313.FLOWER0 + f, flowerNew(2 + f));
      }
      Tex.blit(TexArt313.MUSH_BROWN, mushroomNew(false));
      Tex.blit(TexArt313.MUSH_RED, mushroomNew(true));
      Tex.blit(Tex.CACTUS_SIDE, cactusSideNew());
      Tex.blit(Tex.CACTUS_TOP, cactusTopNew());
      Tex.blit(219, sugarCaneNew());
      Tex.blit(Tex.FARM_TOP, farmlandNew(false));
      Tex.blit(203, farmlandNew(true));
      for (int s = 0; s < 8; s++) {
         Tex.blit(Tex.WHEAT0 + s, wheatCropNew(s));
      }
      for (int s = 0; s < 4; s++) {
         Tex.blit(204 + s, rootCropNew(false, s));
         Tex.blit(208 + s, rootCropNew(true, s));
      }
      Tex.blit(212, haySideNew());
      Tex.blit(213, hayTopNew());
      Tex.blit(221, pumpkinTopNew());
      Tex.blit(222, pumpkinSideNew());
      Tex.blit(223, pumpkinFaceNew(false));
      Tex.blit(224, pumpkinFaceNew(true));
      Tex.blit(225, melonTopNew());
      Tex.blit(226, melonSideNew());

      // Mineral Blocks, Nether & End Blocks
      Tex.blit(Color312.T_IRON, mineralBlockNew(0xE8EEF4, 0xC6CFD8, 0x8E98A4, 0xFFFFFF, false));
      Tex.blit(Color312.T_GOLD, mineralBlockNew(0xFFE042, 0xDCAC18, 0x9C6C08, 0xFFFFB8, false));
      Tex.blit(Color312.T_DIAMOND, mineralBlockNew(0x5CF6EE, 0x24C0C4, 0x107882, 0xE4FFFF, true));
      Tex.blit(Color312.T_EMERALD, mineralBlockNew(0x36F072, 0x16AC48, 0x0A6828, 0xC8FFDC, true));
      Tex.blit(Color312.T_COAL, mineralStoneNew(218, c(0x121216), c(0x1C1C22), c(0x282830), c(0x363640), c(0x0A0A0C)));
      Tex.blit(Color312.T_LAPIS, lapisBlockNew());
      Tex.blit(TexArt312.NETHERRACK, mineralStoneNew(256, c(0x4C1818), c(0x642222), c(0x7C2E2E), c(0x963C3C), c(0x361010)));
      Tex.blit(TexArt312.SOUL_SAND, soulSandNew());
      Tex.blit(TexArt312.QUARTZ_ORE, quartzOreNew());
      Tex.blit(TexArt312.NETHER_BRICKS, netherBricksNew());
      Tex.blit(TexArt312.MAGMA, TexPaint319.magma());
      Tex.blit(TexArt312.QUARTZ_SIDE, polishedStoneNew(c(0xECE8E0), c(0xFFFFFF), c(0xC8C2B8), 261));
      Tex.blit(TexArt312.QUARTZ_TOP, polishedStoneNew(c(0xF2EFE8), c(0xFFFFFF), c(0xCECAC0), 262));
      Tex.blit(TexArt312.END_STONE, endStoneNew());
      Tex.blit(TexArt312.DRAGON_EGG, mineralStoneNew(269, c(0x120A1E), c(0x221238), c(0x361E56), c(0x522E80), c(0x08040E)));
      Tex.blit(Color312.T_CLAY, mineralStoneNew(379, c(0x8C92A0), c(0x9CA2B0), c(0xACB2C0), c(0xBCC2D0), c(0x787E8C)));

      Tex.blit(TexArt313.PLANKS0, planksNew(c(0x7A5836), c(0x8C6640), c(0x66482A), c(0x48321C), 534)); // Spruce
      Tex.blit(TexArt313.PLANKS0 + 1, planksNew(c(0xD6C68C), c(0xE4D69E), c(0xBEA872), c(0x948052), 535)); // Birch
      Tex.blit(TexArt313.PLANKS0 + 2, planksNew(c(0xAB7650), c(0xBC865E), c(0x94623E), c(0x6E462A), 536)); // Jungle
      Tex.blit(TexArt313.PLANKS0 + 3, planksNew(c(0xBA6436), c(0xCC7442), c(0x9E5028), c(0x743818), 537)); // Acacia
      Tex.blit(TexArt313.PLANKS0 + 4, planksNew(c(0x462E18), c(0x563A20), c(0x362210), c(0x221408), 538)); // Dark Oak

      Tex.blit(TexArt313.RED_SAND, sandNew(true));
      Tex.blit(TexArt313.RS_SIDE, sandstoneSideNew(true));
      Tex.blit(TexArt313.RS_TOP, sandstoneTopNew(true));
      Tex.blit(TexArt313.GRANITE, mineralStoneNew(520, c(0x7A4C3C), c(0x94604E), c(0xAC7460), c(0xC28A76), c(0x5E382A)));
      Tex.blit(TexArt313.DIORITE, mineralStoneNew(521, c(0x9C9C9E), c(0xB8B8BA), c(0xD2D2D4), c(0xEAEAE8), c(0x78787A)));
      Tex.blit(TexArt313.ANDESITE, mineralStoneNew(522, c(0x646668), c(0x787A7C), c(0x8C8E90), c(0xA0A2A4), c(0x505254)));
      Tex.blit(TexArt313.COARSE_DIRT, coarseDirtNew());
      Tex.blit(TexArt313.PODZOL_TOP, mineralStoneNew(524, c(0x422A14), c(0x56381C), c(0x6C4826), c(0x825A32), c(0x2E1C0C)));
      Tex.blit(TexArt313.MYCELIUM_TOP, mineralStoneNew(526, c(0x584A58), c(0x6A5A6A), c(0x7C6C7C), c(0x908090), c(0x443844)));
      Tex.blit(TexArt313.ICE, iceNew(false));
      Tex.blit(TexArt313.PACKED_ICE, iceNew(true));
      Tex.blit(TexArt315.BARS, barsNew());

      // 4. Redstone & 1.12 Expansion Tiles (643..695)
      Tex.blit(Redstone319.T_REDSTONE_BLOCK, redstoneBlockNew());
      Tex.blit(Redstone319.T_LAMP_OFF, redstoneLampNew(false));
      Tex.blit(Redstone319.T_LAMP_ON, redstoneLampNew(true));
      Tex.blit(Redstone319.T_REDSTONE_TORCH, torchNew(true));
      Tex.blit(Redstone319.T_POL_GRANITE, polishedStoneNew(c(0x986452), c(0xB8806C), c(0x684032), 648));
      Tex.blit(Redstone319.T_POL_DIORITE, polishedStoneNew(c(0xC4C4C6), c(0xE8E8EA), c(0x88888A), 649));
      Tex.blit(Redstone319.T_POL_ANDESITE, polishedStoneNew(c(0x808284), c(0x9FA2A4), c(0x585A5C), 650));
      Tex.blit(Redstone319.T_CHIS_SANDSTONE, TexPaint319.carvedSandstone(false));
      Tex.blit(Redstone319.T_SMOOTH_SANDSTONE, TexPaint319.smoothSandstone(false));
      Tex.blit(Redstone319.T_CHIS_RED_SANDSTONE, TexPaint319.carvedSandstone(true));
      Tex.blit(Redstone319.T_SMOOTH_RED_SANDSTONE, TexPaint319.smoothSandstone(true));
      Tex.blit(Redstone319.T_CHIS_STONEBRICK, stoneBrickNew(1));
      Tex.blit(Redstone319.T_MOSSY_STONEBRICK, stoneBrickNew(2));
      Tex.blit(Redstone319.T_CRACKED_STONEBRICK, stoneBrickNew(3));
      Tex.blit(Redstone319.T_SLIME, slimeBlockNew());
      Tex.blit(Redstone319.T_SEA_LANTERN, seaLanternNew());
      Tex.blit(Redstone319.T_PRISMARINE, prismarineNew(0));
      Tex.blit(Redstone319.T_PRISMARINE_BRICKS, prismarineNew(1));
      Tex.blit(Redstone319.T_DARK_PRISMARINE, prismarineNew(2));
      Tex.blit(Redstone319.T_PURPUR_BLOCK, purpurNew(false));
      Tex.blit(Redstone319.T_PURPUR_PILLAR, purpurNew(true));
      Tex.blit(Redstone319.T_NOTE_BLOCK, noteBlockNew());
      Tex.blit(Redstone319.T_OBSERVER_FRONT, observerFrontNew());
      Tex.blit(Redstone319.T_DAYLIGHT_TOP, daylightTopNew());
      Tex.blit(Redstone319.T_PISTON_TOP, pistonTopNew(false));
      Tex.blit(Redstone319.T_PISTON_TOP_STICKY, pistonTopNew(true));

      // 5. All Tools, Weapons, Hoes & Armor Pieces
      paintAllToolsAndArmor();

      // 6. All Items, Ingots, Gems, Food, Crops, Buckets & Minigame Gear
      paintAllItems();
   }

   // ====================================================================
   //  New Block Painters
   // ====================================================================
   static int[] grassTopNew() {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float v = fbm(x, y, 701) * 0.65F + hash(x, y, 702) * 0.35F;
            int g = 148 + (int)(contrast(v, 2.1F) * 64.0F);
            p[y * N + x] = rgb(g, g, g);
         }
      }
      // Crisp 3-pixel grass blade tufts with sunlit tips and root shadows
      int[][] tufts = {
         {1, 2}, {5, 1}, {10, 2}, {14, 3},
         {3, 6}, {8, 5}, {12, 7}, {0, 10},
         {4, 11}, {9, 10}, {13, 12}, {2, 14},
         {7, 13}, {11, 14}, {15, 8}, {6, 8}
      };
      for (int i = 0; i < tufts.length; i++) {
         int tx = tufts[i][0];
         int ty = tufts[i][1];
         setw(p, tx, ty, rgb(222, 222, 222));
         setw(p, tx, ty + 1, rgb(192, 192, 192));
         setw(p, tx + ((i & 1) == 0 ? 1 : -1), ty + 1, rgb(206, 206, 206));
         setw(p, tx, ty + 2, rgb(138, 138, 138));
      }
      return p;
   }

   static int[] dirtNew() {
      int[] pal = {c(0x644228), c(0x744E30), c(0x845A38), c(0x946642), c(0xA4744C)};
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float v = fbm(x, y, 710) * 0.68F + hash(x, y, 711) * 0.32F;
            p[y * N + x] = step(pal, contrast(v, 1.9F));
         }
      }
      // Small embedded mineral pebbles & loam clods
      int[][] pebbles = {{2, 3}, {9, 2}, {13, 6}, {5, 8}, {11, 11}, {3, 13}, {14, 14}};
      for (int i = 0; i < pebbles.length; i++) {
         int px = pebbles[i][0];
         int py = pebbles[i][1];
         setw(p, px, py, (i & 1) == 0 ? c(0xAC9C88) : c(0x987858));
         setw(p, px + 1, py, c(0x847666));
         setw(p, px, py + 1, c(0x523620));
      }
      return p;
   }

   static int[] grassSideOvlNew() {
      int[] p = new int[N * N];
      int[] depths = {4, 5, 3, 4, 6, 5, 3, 4, 5, 6, 4, 3, 5, 4, 3, 5};
      for (int x = 0; x < N; x++) {
         int d = depths[x];
         for (int y = 0; y < d; y++) {
            int g = y == 0 ? 214 : (y == d - 1 ? 146 : 184 + (int)((hash(x, y, 720) - 0.5F) * 24.0F));
            p[y * N + x] = rgb(g, g, g);
         }
      }
      return p;
   }

   static int[] stoneNew(int seed) {
      int[] pal = {c(0x64666A), c(0x727478), c(0x808286), c(0x8E9094), c(0x9EA0A4)};
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            // Stratified horizontal-diagonal rock layers
            float strata = vn((x + y * 0.5F) * 0.5F + 0.5F, y * 1.4F + 0.5F, 4, seed) * 0.58F
               + vn(x + 0.5F, y + 0.5F, 8, seed + 13) * 0.24F
               + hash(x, y, seed + 29) * 0.18F;
            p[y * N + x] = step(pal, contrast(strata, 2.3F));
         }
      }
      // Crisp micro-fissures with upper-left highlight lips
      int[][] cracks = {{2, 4, 5}, {9, 8, 5}, {4, 13, 4}, {11, 2, 4}};
      for (int[] cr : cracks) {
         for (int k = 0; k < cr[2]; k++) {
            int cx = cr[0] + k;
            int cy = cr[1] + (k >> 1);
            setw(p, cx, cy, c(0x525458));
            setw(p, cx, cy + 1, c(0xA8AAAE));
         }
      }
      return p;
   }

   static int[] cobbleNew(boolean mossy) {
      float[][] pt = TexPaint.points(4, 735, 0.82F);
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float[] cc = TexPaint.cells(pt[0], pt[1], x + 0.5F, y + 0.5F);
            int idx = (int)cc[2];
            float edge = cc[1] - cc[0];
            if (edge < 0.82F) {
               p[y * N + x] = mossy && hash(x, y, 740) > 0.35F ? c(0x365E24) : c(0x46484C);
            } else {
               float dx = x + 0.5F - pt[0][idx];
               float dy = y + 0.5F - pt[1][idx];
               if (dx > 8) dx -= N; else if (dx < -8) dx += N;
               if (dy > 8) dy -= N; else if (dy < -8) dy += N;
               int base = 122 + (int)(hash(idx, 7, 736) * 34.0F);
               int lit = (int)((-dx - dy) * 3.8F + (hash(x, y, 737) - 0.5F) * 10.0F);
               int v = base + lit - (edge < 1.55F ? 10 : 0);
               p[y * N + x] = rgb(v, v + 1, v + 3);
            }
         }
      }
      if (mossy) {
         int[] mossPal = {c(0x2E541E), c(0x42722A), c(0x568C36), c(0x6CA444)};
         for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
               float mv = vn(x + 0.5F, y + 0.5F, 4, 742) * 0.7F + hash(x, y, 743) * 0.3F;
               if (mv > 0.56F) {
                  p[y * N + x] = step(mossPal, (mv - 0.56F) / 0.44F);
               }
            }
         }
      }
      return p;
   }

   static int[] sandNew(boolean red) {
      int[] pal = red
         ? new int[]{c(0xA44E1C), c(0xB45A22), c(0xC4662A), c(0xD27434), c(0xE08440)}
         : new int[]{c(0xCBB886), c(0xD6C492), c(0xE0CF9E), c(0xEADAA9), c(0xF3E5B6)};
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float wave = (float)Math.sin((y + vn(x + 0.5F, y + 0.5F, 4, 750) * 2.5F) * 0.85F) * 0.5F + 0.5F;
            float v = wave * 0.58F + hash(x, y, 751) * 0.42F;
            p[y * N + x] = step(pal, contrast(v, 1.7F));
         }
      }
      return p;
   }

   static int[] gravelNew() {
      float[][] pt = TexPaint.points(5, 760, 0.78F);
      int[] pal = {c(0x74706E), c(0x847E7A), c(0x948E88), c(0x686668), c(0xA29A92), c(0x888078)};
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float[] cc = TexPaint.cells(pt[0], pt[1], x + 0.5F, y + 0.5F);
            int idx = (int)cc[2];
            float edge = cc[1] - cc[0];
            if (edge < 0.68F) {
               p[y * N + x] = c(0x4E4C4A);
            } else {
               float dx = x + 0.5F - pt[0][idx];
               float dy = y + 0.5F - pt[1][idx];
               if (dx > 8) dx -= N; else if (dx < -8) dx += N;
               if (dy > 8) dy -= N; else if (dy < -8) dy += N;
               int base = pal[idx % pal.length];
               p[y * N + x] = add(base, (int)((-dx - dy) * 3.4F));
            }
         }
      }
      return p;
   }

   static int[] logTopNew(int barkOuter, int barkInner, int woodLight, int woodDark, int heart) {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            int d = Math.max(Math.abs(x - 7 - (x >= 8 ? 1 : 0)), Math.abs(y - 7 - (y >= 8 ? 1 : 0)));
            int col;
            if (d == 7) {
               col = (x + y & 1) == 0 ? barkOuter : barkInner;
            } else if (d == 6) {
               col = barkInner;
            } else if (d <= 1) {
               col = heart;
            } else {
               col = (d & 1) == 0 ? woodLight : woodDark;
            }
            p[y * N + x] = add(col, (int)((hash(x, y, 770) - 0.5F) * 10.0F));
         }
      }
      return p;
   }

   static int[] planksNew(int mid, int hi, int dark, int seam, int seed) {
      int[] p = new int[N * N];
      int[] joints = {7, 13, 4, 11};
      for (int y = 0; y < N; y++) {
         int bd = y >> 2;
         int ry = y & 3;
         int jx = joints[bd];
         for (int x = 0; x < N; x++) {
            if (ry == 3 || x == jx) {
               p[y * N + x] = seam;
               continue;
            }
            float gr = vn((x + 0.5F) * 0.35F, y + 0.5F, 8, seed + bd) - 0.5F;
            int col = ry == 0 ? mix(mid, hi, 0.55F) : (ry == 2 ? mix(mid, dark, 0.35F) : mid);
            col = mul(col, 1.0F + gr * 0.18F + (hash(x, y, seed + 19) - 0.5F) * 0.04F);
            // Nail pegs on either side of the vertical board joint
            if (ry == 1 && (x == ((jx + 2) & 15) || x == ((jx + 14) & 15))) {
               col = mix(dark, seam, 0.5F);
            }
            p[y * N + x] = col;
         }
      }
      return p;
   }

   static int[] bedrockNew() {
      int[] pal = {c(0x18181C), c(0x28282E), c(0x3A3A42), c(0x4E4E58), c(0x686874)};
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float v = vn(x * 1.4F + 0.5F, y * 1.4F + 0.5F, 4, 780) * 0.55F + hash(x, y, 781) * 0.45F;
            p[y * N + x] = step(pal, contrast(v, 2.4F));
         }
      }
      return p;
   }

   static int[] oreNew(int main, int dark, int spec, int rim, int seed) {
      int[] p = stoneNew(101);
      int[][][] clusters = {
         {{2, 2}, {3, 2}, {4, 2}, {2, 3}, {3, 3}, {4, 3}, {3, 4}},
         {{10, 2}, {11, 2}, {12, 3}, {10, 3}, {11, 3}, {11, 4}},
         {{5, 6}, {6, 6}, {7, 6}, {6, 7}, {7, 7}, {8, 7}, {7, 8}},
         {{2, 10}, {3, 10}, {2, 11}, {3, 11}, {4, 11}, {3, 12}},
         {{10, 10}, {11, 10}, {12, 10}, {11, 11}, {12, 11}, {13, 11}, {11, 12}}
      };
      for (int cIdx = 0; cIdx < clusters.length; cIdx++) {
         int[][] cl = clusters[cIdx];
         for (int i = 0; i < cl.length; i++) {
            int x = cl[i][0];
            int y = cl[i][1];
            int col = i == 0 || i == 1 ? spec : (i >= cl.length - 2 ? dark : (i == 2 ? rim : main));
            set(p, x, y, col);
            // Dark socket rim below crystal
            if (y + 1 < N) {
               p[(y + 1) * N + x] = mul(p[(y + 1) * N + x], 0.78F);
            }
         }
      }
      return p;
   }

   static int[] snowNew() {
      int[] pal = {c(0xDCE8F6), c(0xE8F1FC), c(0xF3F8FF), c(0xFFFFFF)};
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float v = fbm(x, y, 790) * 0.7F + hash(x, y, 791) * 0.3F;
            p[y * N + x] = step(pal, contrast(v, 1.9F));
         }
      }
      return p;
   }

   static int[] glassNew() {
      int[] p = new int[N * N];
      int hi = c(0xF0FCFF);
      int mid = c(0xAADCEE);
      int dark = c(0x6498B0);
      for (int i = 0; i < N; i++) {
         p[i] = i < 15 ? hi : dark;
         p[i * N] = i < 15 ? hi : dark;
         p[15 * N + i] = dark;
         p[i * N + 15] = dark;
      }
      // Inner bevel corners & crisp diagonal specular streaks
      p[1 * N + 1] = hi;
      p[1 * N + 14] = mid;
      p[14 * N + 1] = mid;
      p[14 * N + 14] = dark;
      line(p, 3, 6, 6, 3, hi);
      line(p, 3, 9, 9, 3, mid);
      line(p, 10, 13, 13, 10, hi);
      return p;
   }

   static int[] bricksNew() {
      int[] p = new int[N * N];
      int mortar = c(0xC2B8AC);
      int mortarDark = c(0x9A9084);
      for (int y = 0; y < N; y++) {
         int row = y >> 2;
         int ry = y & 3;
         for (int x = 0; x < N; x++) {
            int xs = (x + (row & 1) * 4) & 15;
            int rx = xs & 7;
            if (ry == 0 || rx == 0) {
               p[y * N + x] = (ry == 0 && rx > 0) ? mortar : mortarDark;
               continue;
            }
            int bid = (row << 1) + (xs >> 3);
            int baseR = 164 + (int)((hash(bid, row, 800) - 0.5F) * 24.0F);
            int baseG = 76 + (int)((hash(bid, row, 801) - 0.5F) * 16.0F);
            int baseB = 58 + (int)((hash(bid, row, 802) - 0.5F) * 14.0F);
            int bev = (ry == 1 || rx == 1) ? 14 : (ry == 3 || rx == 7 ? -12 : 0);
            int n = (int)((hash(x, y, 803) - 0.5F) * 10.0F);
            p[y * N + x] = rgb(baseR + bev + n, baseG + (bev + n) * 2 / 3, baseB + (bev + n) / 2);
         }
      }
      return p;
   }

   static int[] sandstoneSideNew(boolean red) {
      int[] pal = red
         ? new int[]{c(0x964618), c(0xAC5420), c(0xBE6228), c(0xCE7032), c(0xDE803E)}
         : new int[]{c(0xC6B684), c(0xD4C492), c(0xE0D1A0), c(0xECDEAE), c(0xF6EABC)};
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            if (y == 0 || y == 4) {
               p[y * N + x] = pal[4];
            } else if (y == 3 || y == 11 || y == 15) {
               p[y * N + x] = pal[0];
            } else {
               float v = vn(x * 0.5F + 0.5F, y + 0.5F, 4, red ? 811 : 810) * 0.65F + hash(x, y, 812) * 0.35F;
               p[y * N + x] = step(pal, contrast(v, 1.8F));
            }
         }
      }
      return p;
   }

   static int[] sandstoneTopNew(boolean red) {
      int[] pal = red
         ? new int[]{c(0x9E4C1C), c(0xB25A24), c(0xC4682E), c(0xD47638), c(0xE28442)}
         : new int[]{c(0xCCB988), c(0xD8C696), c(0xE4D3A4), c(0xEFE0B2), c(0xF8ECC0)};
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float v = fbm(x, y, red ? 815 : 814) * 0.7F + hash(x, y, 816) * 0.3F;
            int col = step(pal, contrast(v, 1.9F));
            if (x == 0 || y == 0) col = mul(col, 1.06F);
            else if (x == 15 || y == 15) col = mul(col, 0.92F);
            p[y * N + x] = col;
         }
      }
      return p;
   }

   static int[] glowstoneNew() {
      float[][] pt = TexPaint.points(4, 820, 0.85F);
      int[] pal = {c(0x7A4214), c(0xB87224), c(0xE8A838), c(0xFCD868), c(0xFFFFC4)};
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float[] cc = TexPaint.cells(pt[0], pt[1], x + 0.5F, y + 0.5F);
            float d = cc[0];
            int col = d < 0.9F ? pal[4] : (d < 1.6F ? pal[3] : (d < 2.2F ? pal[2] : (d < 2.8F ? pal[1] : pal[0])));
            p[y * N + x] = col;
         }
      }
      return p;
   }

   static int[] obsidianNew() {
      int[] pal = {c(0x0B0816), c(0x161028), c(0x241A3E), c(0x36265A), c(0x523A82)};
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float v = vn((x + y) * 0.6F + 0.5F, (y - x) * 0.6F + 0.5F, 4, 825) * 0.7F + hash(x, y, 826) * 0.3F;
            p[y * N + x] = step(pal, contrast(v, 2.3F));
         }
      }
      // Conchoidal volcanic glass fracture arcs
      line(p, 2, 5, 6, 2, c(0x7454B4));
      line(p, 9, 13, 13, 9, c(0x6446A0));
      return p;
   }

   /** Stone Brick variants: 0 = regular, 1 = chiseled, 2 = mossy, 3 = cracked. */
   static int[] stoneBrickNew(int kind) {
      int[] p = new int[N * N];
      if (kind == 1) {
         // Chiseled Stone Brick concentric temple relief
         for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
               int r = Math.max(Math.abs(x - 7 - (x >= 8 ? 1 : 0)), Math.abs(y - 7 - (y >= 8 ? 1 : 0)));
               int g = (r == 7 || r == 4 || r == 1) ? 88 : ((r == 6 || r == 3) ? 156 : 132);
               g += (int)((hash(x, y, 831) - 0.5F) * 12.0F);
               p[y * N + x] = rgb(g, g, g + 2);
            }
         }
         return p;
      }
      for (int y = 0; y < N; y++) {
         int row = y >> 3;
         int ry = y & 7;
         for (int x = 0; x < N; x++) {
            int xs = (x + row * 4) & 15;
            int rx = xs & 7;
            if (ry == 0 || rx == 7) {
               p[y * N + x] = c(0x505256);
               continue;
            }
            int g = 132 + (int)((hash(x, y, 830) - 0.5F) * 18.0F);
            if (ry == 1 || rx == 0) g += 16;
            else if (ry == 7 || rx == 6) g -= 14;
            p[y * N + x] = rgb(g, g + 1, g + 3);
         }
      }
      if (kind == 2) {
         // Mossy Stone Bricks
         int[] moss = {c(0x366024), c(0x4C7E32), c(0x649A42)};
         for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
               float mv = vn(x + 0.5F, y + 0.5F, 4, 832) * 0.7F + hash(x, y, 833) * 0.3F;
               if (mv > 0.58F) {
                  p[y * N + x] = step(moss, (mv - 0.58F) / 0.42F);
               }
            }
         }
      } else if (kind == 3) {
         // Cracked Stone Bricks
         line(p, 3, 2, 6, 6, c(0x44464A));
         line(p, 6, 6, 5, 11, c(0x44464A));
         line(p, 11, 4, 13, 12, c(0x44464A));
         line(p, 11, 9, 8, 13, c(0x44464A));
      }
      return p;
   }

   static int[] woolNew(int colour) {
      int[] rgbArr = Tex.WOOL[colour];
      int base = rgb(rgbArr[0], rgbArr[1], rgbArr[2]);
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            // Plush herringbone cross-weave
            int weave = ((x + (y & 1) * 2) >> 1) & 1;
            float k = 1.0F + (weave == 0 ? 0.065F : -0.065F) + (fbm(x, y, 840 + colour) - 0.5F) * 0.14F;
            p[y * N + x] = mul(base, k);
         }
      }
      return p;
   }

   static int[] craftTopNew() {
      int[] p = planksNew(c(0xB89056), c(0xC8A266), c(0x9E7842), c(0x6E5028), 510);
      int rimDark = c(0x3E2812);
      int rimGold = c(0xD8B048);
      int gridSq = c(0x886234);
      int gridHi = c(0xA87C48);
      rect(p, 1, 1, 14, 14, rimDark);
      rect(p, 2, 2, 12, 12, c(0x583A1C));
      // Corner brass brackets
      set(p, 1, 1, rimGold); set(p, 14, 1, rimGold);
      set(p, 1, 14, rimGold); set(p, 14, 14, rimGold);
      for (int gy = 0; gy < 3; gy++) {
         for (int gx = 0; gx < 3; gx++) {
            int sx = 3 + gx * 3 + gx;
            int sy = 3 + gy * 3 + gy;
            rect(p, sx, sy, 2, 2, gridSq);
            set(p, sx, sy, gridHi);
         }
      }
      return p;
   }

   static int[] craftSideNew() {
      int[] p = planksNew(c(0xB89056), c(0xC8A266), c(0x9E7842), c(0x6E5028), 510);
      // Dark walnut top apron
      rect(p, 0, 0, 16, 3, c(0x52361A));
      line(p, 0, 3, 15, 3, c(0x34200E));
      // Left hanging handsaw
      rect(p, 2, 5, 3, 2, c(0x9E4424));
      line(p, 3, 7, 3, 13, c(0xDCE2E8));
      line(p, 4, 7, 4, 12, c(0x9EA6B0));
      // Right claw hammer & square
      line(p, 11, 6, 11, 13, c(0x7C5428));
      rect(p, 9, 5, 5, 2, c(0xC8D0D8));
      set(p, 10, 5, c(0xF0F6FF));
      return p;
   }

   static int[] furnaceFrontNew(boolean on) {
      int[] p = stoneBrickNew(0);
      // Upper smelt chamber arch
      rect(p, 4, 2, 8, 4, c(0x222428));
      rect(p, 5, 3, 6, 2, c(0x141618));
      // Lower hearth fire-box with iron grate bars
      rect(p, 3, 8, 10, 7, c(0x1C1E22));
      if (on) {
         for (int y = 9; y <= 14; y++) {
            for (int x = 4; x <= 11; x++) {
               int col = y <= 10 ? c(0xFFFFA8) : (y <= 12 ? c(0xFFA824) : c(0xE0440C));
               p[y * N + x] = col;
            }
         }
      }
      // Vertical iron grate bars over lower hearth
      for (int gx : new int[]{5, 8, 10}) {
         line(p, gx, 9, gx, 14, c(0x46484E));
      }
      return p;
   }

   static int[] chestTopNew() {
      int[] p = planksNew(c(0xAC7834), c(0xC48E44), c(0x8E5E24), c(0x5E3C14), 850);
      int band = c(0x362410);
      for (int i = 0; i < N; i++) {
         set(p, i, 0, band); set(p, i, 15, band);
         set(p, 0, i, band); set(p, 15, i, band);
      }
      return p;
   }

   static int[] chestSideNew() {
      int[] p = chestTopNew();
      line(p, 0, 5, 15, 5, c(0x281808));
      line(p, 0, 6, 15, 6, c(0xD49E52));
      // Ornate gold/silver clasp
      rect(p, 6, 3, 4, 5, c(0x423018));
      rect(p, 7, 4, 2, 4, c(0xF6D448));
      set(p, 7, 4, c(0xFFFFB0));
      return p;
   }

   static int[] spawnerNew() {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            boolean bar = x == 0 || x == 15 || y == 0 || y == 15 || x == 5 || x == 10 || y == 5 || y == 10;
            if (bar) {
               p[y * N + x] = ((x + y) & 1) == 0 ? c(0x526478) : c(0x364454);
            } else {
               p[y * N + x] = c(0x12161E);
            }
         }
      }
      set(p, 7, 7, c(0xFF5428));
      set(p, 8, 7, c(0xFF5428));
      return p;
   }

   static int[] bookshelfNew() {
      int[] p = planksNew(c(0xB89056), c(0xC8A266), c(0x9E7842), c(0x6E5028), 510);
      int[] cols = {c(0xB82824), c(0x2458A8), c(0x28843C), c(0x883898), c(0xC8781E), c(0x228288)};
      for (int shelf = 0; shelf < 2; shelf++) {
         int y0 = shelf == 0 ? 2 : 9;
         rect(p, 1, y0, 14, 5, c(0x24160A));
         for (int b = 0; b < 6; b++) {
            int bx = 2 + b * 2;
            int col = cols[(b + shelf * 3) % cols.length];
            rect(p, bx, y0 + (b & 1), 2, 5 - (b & 1), col);
            set(p, bx, y0 + (b & 1), mul(col, 1.2F));
            set(p, bx, y0 + 2, c(0xF0D058));
         }
      }
      return p;
   }

   static int[] tntSideNew() {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            int st = x & 3;
            if (y >= 5 && y <= 10) {
               p[y * N + x] = (y == 5 || y == 10) ? c(0xCCC6BC) : c(0xF8F4EC);
            } else {
               int r = st == 0 ? c(0xF03E2A) : (st == 1 ? c(0xD82E1C) : (st == 2 ? c(0xBC2212) : c(0x88160A)));
               p[y * N + x] = (y == 0 || y == 15) ? mul(r, 0.78F) : r;
            }
         }
      }
      int ink = c(0x18181C);
      int[][] tnt = {
         {2, 6}, {3, 6}, {4, 6}, {3, 7}, {3, 8}, {3, 9},
         {6, 6}, {6, 7}, {6, 8}, {6, 9}, {7, 7}, {8, 8}, {9, 6}, {9, 7}, {9, 8}, {9, 9},
         {11, 6}, {12, 6}, {13, 6}, {12, 7}, {12, 8}, {12, 9}
      };
      for (int[] pt : tnt) {
         p[pt[1] * N + pt[0]] = ink;
      }
      return p;
   }

   static int[] tntTopNew(boolean top) {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            boolean seam = (x & 3) == 3 || (y & 3) == 3;
            p[y * N + x] = seam ? c(0x78160C) : c(0xDC3420);
         }
      }
      if (top) {
         rect(p, 6, 6, 4, 4, c(0x383430));
         rect(p, 7, 7, 2, 2, c(0xEAE4D8));
      }
      return p;
   }

   static int[] ladderNew() {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         p[y * N + 2] = c(0xB88E52); p[y * N + 3] = c(0x826032);
         p[y * N + 12] = c(0xB88E52); p[y * N + 13] = c(0x826032);
      }
      for (int ry : new int[]{1, 5, 9, 13}) {
         for (int x = 1; x <= 14; x++) {
            p[ry * N + x] = c(0xCCA264);
            p[(ry + 1) * N + x] = c(0x8E6838);
         }
      }
      return p;
   }

   static int[] doorNew(boolean upper) {
      return TexPaint319.door(upper);
   }

   static int[] torchNew(boolean redstone) {
      int[] p = new int[N * N];
      for (int y = 6; y < 16; y++) {
         p[y * N + 7] = c(0xA87E48);
         p[y * N + 8] = c(0x78562C);
      }
      if (redstone) {
         rect(p, 6, 3, 4, 3, c(0xC81818));
         rect(p, 7, 3, 2, 2, c(0xFF7868));
         set(p, 7, 4, c(0xFFFFD0));
      } else {
         rect(p, 6, 3, 4, 3, c(0xFF9618));
         rect(p, 7, 3, 2, 2, c(0xFFFFB8));
         set(p, 7, 2, c(0xFFD048));
      }
      return p;
   }

   static int[] mineralStoneNew(int seed, int c0, int c1, int c2, int c3, int c4) {
      int[] pal = {c4, c0, c1, c2, c3};
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float v = fbm(x, y, seed) * 0.65F + hash(x, y, seed + 17) * 0.35F;
            p[y * N + x] = step(pal, contrast(v, 2.2F));
         }
      }
      return p;
   }

   static int[] polishedStoneNew(int mid, int hi, int dark, int seed) {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            if (x == 0 || y == 0) {
               p[y * N + x] = hi;
            } else if (x == 15 || y == 15) {
               p[y * N + x] = dark;
            } else {
               float v = (fbm(x, y, seed) - 0.5F) * 0.18F;
               p[y * N + x] = mul(mid, 1.0F + v);
            }
         }
      }
      return p;
   }

   static int[] iceNew(boolean packed) {
      int[] pal = packed
         ? new int[]{c(0x6894DC), c(0x7CA6EC), c(0x90B8F8), c(0xA8CCFF)}
         : new int[]{c(0x74A8F8), c(0x88BAFF), c(0x9ECAFF), c(0xBCE0FF)};
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float v = fbm(x, y, packed ? 861 : 860) * 0.7F + hash(x, y, 862) * 0.3F;
            p[y * N + x] = step(pal, contrast(v, 1.9F));
         }
      }
      line(p, 2, 12, 12, 2, c(0xE4F4FF));
      line(p, 6, 13, 13, 6, c(0xD0EAFF));
      return p;
   }

   static int[] redstoneBlockNew() {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            int d = Math.min(Math.min(x, 15 - x), Math.min(y, 15 - y));
            int col = d == 0 ? c(0x780C08) : (d == 1 ? c(0xE82818) : (d == 2 ? c(0x9E140E) : c(0xC81E14)));
            p[y * N + x] = add(col, (int)((hash(x, y, 870) - 0.5F) * 12.0F));
         }
      }
      return p;
   }

   static int[] redstoneLampNew(boolean lit) {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            boolean frame = x == 0 || x == 15 || y == 0 || y == 15 || Math.abs(x - y) == 0 || Math.abs(x + y - 15) == 0;
            if (frame) {
               p[y * N + x] = c(0x3A2818);
            } else if (lit) {
               int d = Math.max(Math.abs(x - 7), Math.abs(y - 7));
               p[y * N + x] = d <= 3 ? c(0xFFFFB8) : (d <= 5 ? c(0xF8C858) : c(0xD88E28));
            } else {
               p[y * N + x] = c(0x6E4826);
            }
         }
      }
      return p;
   }

   static int[] slimeBlockNew() {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            boolean core = x >= 3 && x <= 12 && y >= 3 && y <= 12;
            p[y * N + x] = core ? c(0x58B842) : c(0x78D862);
         }
      }
      line(p, 2, 2, 6, 2, c(0xB8FF9E));
      return p;
   }

   static int[] seaLanternNew() {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            int d = Math.min(Math.min(x, 15 - x), Math.min(y, 15 - y));
            p[y * N + x] = d == 0 ? c(0x467E78) : (d <= 2 ? c(0x98DCD4) : c(0xECFFFF));
         }
      }
      return p;
   }

   static int[] prismarineNew(int kind) {
      int[] pal = kind == 2
         ? new int[]{c(0x223E36), c(0x2E5046), c(0x3A6256), c(0x487668)}
         : new int[]{c(0x427A6E), c(0x549284), c(0x68AA9C), c(0x82C2B4)};
      return mineralStoneNew(880 + kind, pal[0], pal[1], pal[2], pal[3], mul(pal[0], 0.85F));
   }

   static int[] purpurNew(boolean pillar) {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            boolean seam = pillar ? (x % 4 == 0) : ((y % 4 == 0) || (((x + (y >> 2) * 4) & 7) == 0));
            p[y * N + x] = seam ? c(0x865E86) : add(c(0xAC80AC), (int)((hash(x, y, 890) - 0.5F) * 10.0F));
         }
      }
      return p;
   }

   static int[] leavesNew(int kind) {
      int[] p = new int[N * N];
      int seed = 6600 + kind * 101;
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            int d = 112 + (int)(hash(x, y, seed) * 20.0F);
            p[y * N + x] = rgb(d, d, d);
         }
      }
      int[][] clusters = {
         {1, 1, 0}, {5, 0, 1}, {10, 1, 0}, {13, 4, 1},
         {2, 5, 1}, {7, 4, 0}, {11, 7, 1}, {0, 9, 0},
         {5, 9, 1}, {12, 10, 0}, {3, 13, 0}, {8, 12, 1},
         {13, 13, 1}, {4, 2, 1}, {9, 5, 0}, {14, 1, 0}
      };
      for (int i = 0; i < clusters.length; i++) {
         int cx = (clusters[i][0] + kind * 3) & 15;
         int cy = (clusters[i][1] + kind * 5) & 15;
         int dir = (clusters[i][2] + kind) & 1;
         int[][] blade = dir == 0
            ? new int[][]{{0, 0, 206}, {1, 0, 224}, {2, 0, 198}, {0, 1, 196}, {1, 1, 174}, {2, 1, 158}, {3, 0, 168}, {1, 2, 138}, {2, 2, 102}}
            : new int[][]{{0, 0, 206}, {1, 0, 196}, {0, 1, 224}, {1, 1, 174}, {2, 1, 142}, {0, 2, 196}, {1, 2, 158}, {1, 3, 136}, {2, 2, 102}};
         for (int[] b : blade) {
            int bx = (cx + b[0]) & 15;
            int by = (cy + b[1]) & 15;
            int v = b[2] + (int)((hash(bx, by, seed + i) - 0.5F) * 10.0F);
            p[by * N + bx] = rgb(v, v, v);
         }
      }
      int[][] gaps = {
         {0, 3}, {4, 0}, {8, 2}, {12, 0}, {15, 3},
         {3, 4}, {7, 6}, {11, 5}, {14, 7}, {1, 8},
         {5, 7}, {9, 9}, {13, 8}, {2, 11}, {6, 11},
         {10, 13}, {15, 11}, {0, 14}, {4, 15}, {12, 15}
      };
      for (int[] g : gaps) {
         int gx = (g[0] + kind * 3) & 15;
         int gy = (g[1] + kind * 5) & 15;
         p[gy * N + gx] = 0;
      }
      return p;
   }

   static int[] bushyFrondNew() {
      return TexPaint319.bushyFrond();
   }

   static int[] tallGrassNew(boolean fern) {
      int[] p = new int[N * N];
      int[] xs = {1, 3, 5, 7, 9, 11, 13, 14};
      int[] tops = fern ? new int[]{6, 4, 2, 1, 2, 4, 6, 8} : new int[]{4, 2, 5, 1, 3, 2, 5, 3};
      for (int i = 0; i < xs.length; i++) {
         int x0 = xs[i];
         int top = tops[i];
         for (int y = top; y < N; y++) {
            float k = (15 - y) / (float)Math.max(1, 15 - top);
            int g = 144 + (int)(k * 66.0F);
            set(p, x0, y, rgb(g, g, g));
            if (fern && (y & 1) == 0 && y < 14) {
               set(p, x0 - 1, y, rgb(g - 18, g - 18, g - 18));
               set(p, x0 + 1, y, rgb(g - 18, g - 18, g - 18));
            }
         }
      }
      return p;
   }

   static int[] deadBushNew() {
      int[] p = new int[N * N];
      line(p, 7, 15, 7, 8, c(0x785630));
      line(p, 8, 15, 8, 9, c(0x5A3E20));
      line(p, 7, 11, 3, 5, c(0x8C663A));
      line(p, 8, 10, 12, 4, c(0x8C663A));
      line(p, 5, 8, 2, 3, c(0xA47B48));
      line(p, 10, 7, 13, 2, c(0xA47B48));
      return p;
   }

   static int[] flowerNew(int kind) {
      int[] p = new int[N * N];
      for (int y = 8; y < 16; y++) {
         set(p, 7, y, c(0x42982C));
         set(p, 8, y, c(0x2C6E1C));
      }
      set(p, 6, 12, c(0x4CA834)); set(p, 5, 11, c(0x62C044));
      set(p, 9, 13, c(0x4CA834)); set(p, 10, 12, c(0x62C044));
      int[][] cols = {
         {0xF43628, 0xB81810, 0x281810}, // Poppy
         {0xFFF038, 0xD8A810, 0xF89814}, // Dandelion
         {0x58B8FF, 0x2878C8, 0xFFF068}, // Blue Orchid
         {0xD868F0, 0x9834B4, 0xFFF498}, // Allium
         {0xE8F0F8, 0xB8C4D0, 0xFFE048}, // Azure Bluet
         {0xF03828, 0xAC1C14, 0xFFE058}, // Red Tulip
         {0xFF8824, 0xC8580C, 0xFFE868}, // Orange Tulip
         {0xF8F8FC, 0xC8CCD4, 0xFFE458}, // White Tulip
         {0xF898C8, 0xC4588E, 0xFFE878}, // Pink Tulip
         {0xF8F8FC, 0xC8CCD4, 0xF8C818}  // Oxeye Daisy
      };
      int[] pal = cols[kind % cols.length];
      for (int y = 3; y <= 8; y++) {
         for (int x = 4; x <= 11; x++) {
            float d = (x - 7.5F) * (x - 7.5F) + (y - 5.5F) * (y - 5.5F) * 1.2F;
            if (d <= 10.5F) {
               p[y * N + x] = d < 2.2F ? c(pal[2]) : (y <= 5 ? c(pal[0]) : c(pal[1]));
            }
         }
      }
      return p;
   }

   static int[] mushroomNew(boolean red) {
      int[] p = new int[N * N];
      rect(p, 6, 9, 4, 7, c(0xE8E0CE));
      rect(p, 8, 9, 2, 7, c(0xC6BCAA));
      for (int y = 4; y <= 9; y++) {
         for (int x = 3; x <= 12; x++) {
            float dx = (x - 7.5F) / 4.8F;
            float dy = (y - 8.5F) / 4.2F;
            if (dx * dx + dy * dy <= 1.0F) {
               p[y * N + x] = red ? (y <= 6 ? c(0xEE2C24) : c(0xB41812)) : (y <= 6 ? c(0xA87848) : c(0x7E5630));
            }
         }
      }
      if (red) {
         set(p, 5, 6, c(0xFFF8EE)); set(p, 8, 5, c(0xFFF8EE)); set(p, 10, 7, c(0xFFF8EE));
      }
      return p;
   }

   static int[] cactusSideNew() {
      return TexPaint319.cactusSide();
   }

   static int[] cactusTopNew() {
      return TexPaint319.cactusTop();
   }

   static int[] sugarCaneNew() {
      return TexPaint319.sugarCane();
   }

   static int[] farmlandNew(boolean wet) {
      return TexPaint319.farmland(wet);
   }

   static int[] wheatCropNew(int stage) {
      return TexPaint319.wheat(stage);
   }

   static int[] rootCropNew(boolean potato, int stage) {
      return TexPaint319.rootCrop(potato, stage);
   }

   static int[] haySideNew() {
      return TexPaint319.haySide();
   }

   static int[] hayTopNew() {
      return TexPaint319.hayTop();
   }

   static int[] pumpkinTopNew() {
      return TexPaint319.pumpkinTop();
   }

   static int[] pumpkinSideNew() {
      return TexPaint319.pumpkinSide();
   }

   static int[] pumpkinFaceNew(boolean lit) {
      return TexPaint319.pumpkinFace(lit);
   }

   static int[] melonTopNew() {
      return TexPaint319.melonTop();
   }

   static int[] melonSideNew() {
      return TexPaint319.melonSide();
   }

   static int[] mineralBlockNew(int hi, int mid, int dark, int spec, boolean gem) {
      return gem ? TexPaint319.gemBlock(mid, dark, mul(c(dark), 0.75F) & 0xFFFFFF, spec, 960)
                 : TexPaint319.metalBlock(mid, dark, mul(c(dark), 0.75F) & 0xFFFFFF, spec, 961);
   }

   static int[] lapisBlockNew() {
      return TexPaint319.lapisBlock();
   }

   static int[] soulSandNew() {
      int[] p = TexPaint319.soulSand();
      int[][] hollows = {{3, 4}, {9, 4}, {5, 10}, {11, 10}};
      for (int[] h : hollows) {
         rect(p, h[0], h[1], 2, 2, c(0x241812));
      }
      return p;
   }

   static int[] quartzOreNew() {
      return TexPaint319.quartzOre();
   }

   static int[] netherBricksNew() {
      return TexPaint319.netherBricks();
   }

   static int[] endStoneNew() {
      return TexPaint319.endStone();
   }

   static int[] coarseDirtNew() {
      return TexPaint319.coarseDirt();
   }

   static int[] barsNew() {
      return TexArt315.bars();
   }

   static int[] noteBlockNew() {
      int[] p = planksNew(c(0x784E2C), c(0x926238), c(0x5E3A1E), c(0x3E2410), 670);
      for (int i = 0; i < N; i++) {
         set(p, i, 0, c(0x3A2210)); set(p, i, 15, c(0x3A2210));
         set(p, 0, i, c(0x3A2210)); set(p, 15, i, c(0x3A2210));
      }
      for (int y = 4; y <= 11; y++) {
         for (int x = 4; x <= 11; x++) {
            if (((x + y) & 1) == 0) {
               p[y * N + x] = mul(p[y * N + x], 0.78F);
            }
         }
      }
      return p;
   }

   static int[] observerFrontNew() {
      int[] p = cobbleNew(false);
      rect(p, 3, 5, 4, 3, c(0x222428));
      rect(p, 9, 5, 4, 3, c(0x222428));
      rect(p, 4, 6, 2, 1, c(0xE8EEF4));
      rect(p, 10, 6, 2, 1, c(0xE8EEF4));
      rect(p, 6, 11, 4, 2, c(0x222428));
      return p;
   }

   static int[] daylightTopNew() {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            boolean frame = x < 2 || x > 13 || y < 2 || y > 13 || x == 5 || x == 10 || y == 5 || y == 10;
            p[y * N + x] = frame ? c(0x5E4224) : c(0xDCE8F4);
         }
      }
      return p;
   }

   static int[] pistonTopNew(boolean sticky) {
      int[] p = planksNew(c(0xB89056), c(0xC8A266), c(0x9E7842), c(0x6E5028), 666);
      rect(p, 0, 0, 2, 2, c(0x98A0A8)); rect(p, 14, 0, 2, 2, c(0x98A0A8));
      rect(p, 0, 14, 2, 2, c(0x98A0A8)); rect(p, 14, 14, 2, 2, c(0x98A0A8));
      if (sticky) {
         for (int y = 4; y <= 11; y++) {
            for (int x = 4; x <= 11; x++) {
               if (Math.hypot(x - 7.5F, y - 7.5F) <= 4.0F) {
                  p[y * N + x] = (x + y & 1) == 0 ? c(0x78D858) : c(0x52A838);
               }
            }
         }
      }
      return p;
   }

   // ====================================================================
   //  Brand-New Pixel-Art Sprites for All Tools, Weapons, Hoes & Armor
   // ====================================================================
   private static final int[][] TIER_PAL = {
      // {darkOutline, shadow, mid, highlight, specular}
      {0x382410, 0x6E4C26, 0x946A38, 0xB8884E, 0xD4A668}, // 0: Wooden
      {0x2C2E32, 0x626468, 0x84868A, 0xA4A6AA, 0xC4C6CA}, // 1: Stone
      {0x323840, 0x8E96A0, 0xC2CAD2, 0xE4ECF4, 0xFFFFFF}, // 2: Iron
      {0x4A3006, 0xB87E10, 0xE8B824, 0xFFE458, 0xFFFFC8}, // 3: Golden
      {0x103E42, 0x1E98A0, 0x38D8CE, 0x78F8EE, 0xE4FFFF}  // 4: Diamond
   };

   private static void paintAllToolsAndArmor() {
      for (int tier = 0; tier < 5; tier++) {
         int[] pal = TIER_PAL[tier];
         // 4 Tool classes: 0->Sword(4), 1->Pickaxe(1), 2->Axe(2), 3->Shovel(3)
         Tex.blit(Items.tile[300 + tier * 4 + 0], paintSwordSprite(pal));
         Tex.blit(Items.tile[300 + tier * 4 + 1], paintPickaxeSprite(pal));
         Tex.blit(Items.tile[300 + tier * 4 + 2], paintAxeSprite(pal));
         Tex.blit(Items.tile[300 + tier * 4 + 3], paintShovelSprite(pal));
         // Hoe (290 + tier)
         Tex.blit(Items.tile[290 + tier], paintHoeSprite(pal));
      }
      int[][] armorPal = {
         {0x3E2010, 0x784022, 0xA45A32, 0xC87648, 0xE29466}, // Leather
         TIER_PAL[2], // Iron
         TIER_PAL[3], // Golden
         TIER_PAL[4]  // Diamond
      };
      for (int mat = 0; mat < 4; mat++) {
         for (int slot = 0; slot < 4; slot++) {
            Tex.blit(Items.tile[320 + mat * 4 + slot], paintArmorSprite(slot, armorPal[mat]));
         }
      }
   }

   private static void drawToolHandle(int[] p, int x0, int y0, int steps) {
      int hDark = c(0x4E3418);
      int hMid = c(0x7C562C);
      int hHi = c(0xA47842);
      for (int i = 0; i < steps; i++) {
         int x = x0 + i;
         int y = y0 - i;
         set(p, x, y, hHi);
         set(p, x + 1, y, hMid);
         set(p, x, y + 1, hDark);
      }
   }

   static int[] paintSwordSprite(int[] pal) {
      int[] p = new int[N * N];
      int sh = c(pal[1]), mid = c(pal[2]), hi = c(pal[3]), spec = c(pal[4]);
      // Pommel & leather grip
      set(p, 1, 14, spec); set(p, 2, 14, mid); set(p, 1, 13, mid);
      line(p, 2, 13, 4, 11, c(0x6E4822));
      line(p, 3, 13, 5, 11, c(0x4E3216));
      // Broad bevelled blade from (5,10) to (14,1)
      for (int i = 0; i < 9; i++) {
         int bx = 6 + i;
         int by = 9 - i;
         set(p, bx - 1, by, hi);
         set(p, bx, by, i >= 7 ? spec : mid);
         set(p, bx, by + 1, sh);
      }
      set(p, 14, 1, spec);
      // Swept crossguard
      line(p, 3, 9, 7, 13, mid);
      line(p, 3, 8, 6, 11, hi);
      set(p, 5, 10, spec);
      return outlineAndBevelSprite(p, pal[0], 901);
   }

   static int[] paintPickaxeSprite(int[] pal) {
      int[] p = new int[N * N];
      int sh = c(pal[1]), mid = c(pal[2]), hi = c(pal[3]), spec = c(pal[4]);
      drawToolHandle(p, 2, 13, 10);
      // Curved twin-pronged pick head at top-right
      int[][] headHi = {{8, 2}, {9, 2}, {10, 2}, {11, 2}, {12, 3}, {7, 3}};
      int[][] headMid = {{8, 3}, {9, 3}, {10, 3}, {11, 3}, {12, 4}, {13, 5}, {13, 6}, {6, 4}};
      int[][] headSh = {{9, 4}, {10, 4}, {11, 4}, {12, 5}, {13, 7}, {12, 7}};
      for (int[] q : headHi) set(p, q[0], q[1], hi);
      for (int[] q : headMid) set(p, q[0], q[1], mid);
      for (int[] q : headSh) set(p, q[0], q[1], sh);
      set(p, 10, 2, spec);
      set(p, 11, 3, spec);
      return outlineAndBevelSprite(p, pal[0], 902);
   }

   static int[] paintAxeSprite(int[] pal) {
      int[] p = new int[N * N];
      int sh = c(pal[1]), mid = c(pal[2]), hi = c(pal[3]), spec = c(pal[4]);
      drawToolHandle(p, 2, 13, 10);
      // Broad bearded axe blade + back poll
      int[][] blade = {
         {8, 2, 3}, {9, 2, 3}, {10, 2, 2},
         {7, 3, 3}, {8, 3, 2}, {9, 3, 2}, {10, 3, 1}, {11, 3, 1},
         {7, 4, 3}, {8, 4, 2}, {9, 4, 2}, {10, 4, 1}, {11, 4, 1},
         {7, 5, 3}, {8, 5, 2}, {9, 5, 1}, {8, 6, 1}
      };
      for (int[] b : blade) {
         set(p, b[0], b[1], b[2] == 3 ? spec : (b[2] == 2 ? hi : (b[2] == 1 ? mid : sh)));
      }
      return outlineAndBevelSprite(p, pal[0], 903);
   }

   static int[] paintShovelSprite(int[] pal) {
      int[] p = new int[N * N];
      int sh = c(pal[1]), mid = c(pal[2]), hi = c(pal[3]), spec = c(pal[4]);
      drawToolHandle(p, 2, 13, 8);
      // Rounded spade scoop at top-right
      rect(p, 10, 2, 4, 4, mid);
      line(p, 10, 2, 13, 2, hi);
      line(p, 10, 2, 10, 5, hi);
      line(p, 13, 3, 13, 5, sh);
      line(p, 10, 5, 13, 5, sh);
      set(p, 12, 2, spec);
      set(p, 11, 3, spec);
      return outlineAndBevelSprite(p, pal[0], 904);
   }

   static int[] paintHoeSprite(int[] pal) {
      int[] p = new int[N * N];
      int sh = c(pal[1]), mid = c(pal[2]), hi = c(pal[3]), spec = c(pal[4]);
      drawToolHandle(p, 2, 13, 10);
      rect(p, 8, 2, 5, 2, mid);
      line(p, 8, 2, 12, 2, hi);
      set(p, 10, 2, spec);
      rect(p, 7, 3, 2, 2, sh);
      return outlineAndBevelSprite(p, pal[0], 905);
   }

   static int[] paintArmorSprite(int slot, int[] pal) {
      int[] p = new int[N * N];
      int sh = c(pal[1]), mid = c(pal[2]), hi = c(pal[3]), spec = c(pal[4]);
      if (slot == 0) {
         // Helmet
         rect(p, 4, 3, 8, 3, hi);
         rect(p, 3, 6, 10, 6, mid);
         rect(p, 5, 8, 6, 4, 0); // Face opening
         rect(p, 7, 6, 2, 3, sh); // Nasal guard
         set(p, 5, 4, spec); set(p, 6, 4, spec);
      } else if (slot == 1) {
         // Chestplate
         rect(p, 2, 3, 4, 4, hi);
         rect(p, 10, 3, 4, 4, hi);
         rect(p, 4, 5, 8, 9, mid);
         rect(p, 5, 6, 6, 3, hi);
         line(p, 7, 5, 7, 13, sh);
         set(p, 5, 6, spec);
      } else if (slot == 2) {
         // Leggings
         rect(p, 4, 2, 8, 3, sh);
         rect(p, 4, 5, 3, 9, mid);
         rect(p, 9, 5, 3, 9, mid);
         line(p, 4, 5, 4, 13, hi);
         line(p, 9, 5, 9, 13, hi);
      } else {
         // Boots
         rect(p, 3, 7, 4, 6, mid);
         rect(p, 9, 7, 4, 6, mid);
         rect(p, 2, 11, 5, 2, sh);
         rect(p, 9, 11, 5, 2, sh);
         set(p, 4, 8, hi); set(p, 10, 8, hi);
      }
      return outlineAndBevelSprite(p, pal[0], 910 + slot);
   }

   // ====================================================================
   //  Brand-New Pixel-Art Sprites for All Items, Food, Gems & Materials
   // ====================================================================
   private static void paintAllItems() {
      Tex.blit(Items.tile[Items.STICK], paintStick());
      Tex.blit(Items.tile[Items.COAL], paintGemOrLump(0x141418, 0x2C2C34, 0x484854, 0x727282, false));
      Tex.blit(Items.tile[Items.IRON_INGOT], paintIngot(0x363C44, 0x8E96A0, 0xC8D0D8, 0xFFFFFF));
      Tex.blit(Items.tile[Items.GOLD_INGOT], paintIngot(0x4E3204, 0xC4880C, 0xF8CC28, 0xFFFFB8));
      Tex.blit(Items.tile[Items.DIAMOND], paintGemOrLump(0x0E3E44, 0x1EA4AC, 0x46EEE4, 0xE8FFFF, true));
      Tex.blit(Items.tile[Items.EMERALD], paintGemOrLump(0x083E1C, 0x149A42, 0x32EC6E, 0xC8FFDA, true));
      Tex.blit(Items.tile[Items.APPLE], paintApple(false));
      Tex.blit(Items.tile[Redstone319.GOLDEN_APPLE], paintApple(true));
      Tex.blit(Items.tile[Items.BOW], paintBow());
      Tex.blit(Items.tile[Items.ARROW], paintArrow());
      Tex.blit(Items.tile[Items.BREAD], paintBread());
      Tex.blit(Items.tile[Items.BUCKET], paintBucket(0));
      Tex.blit(Items.tile[Items.WATER_BUCKET], paintBucket(1));
      Tex.blit(Items.tile[Items.LAVA_BUCKET], paintBucket(2));
      Tex.blit(Items.tile[Items.MILK_BUCKET], paintBucket(3));
      Tex.blit(Items.tile[Items.ENDER_PEARL], paintEnderPearl(false));
      Tex.blit(Items.tile[Items.EYE_OF_ENDER], paintEnderPearl(true));
      Tex.blit(Items.tile[Items.RAW_PORK], paintMeatChop(0xE88278, 0xF8B8B0, 0xB84E46));
      Tex.blit(Items.tile[Items.COOKED_PORK], paintMeatChop(0xC88248, 0xE8AC72, 0x8E5224));
      Tex.blit(Items.tile[Items.RAW_BEEF], paintMeatChop(0xC8342C, 0xF0887E, 0x8E1C16));
      Tex.blit(Items.tile[Items.STEAK], paintMeatChop(0x7E4222, 0xA86238, 0x4E2410));
      Tex.blit(Items.tile[Items.RAW_CHICKEN], paintDrumstick(0xF2B8A4, 0xFFE0D4, 0xC48470));
      Tex.blit(Items.tile[Items.COOKED_CHICKEN], paintDrumstick(0xD48438, 0xF4AC58, 0x98541C));
      Tex.blit(Items.tile[Items.RAW_MUTTON], paintMeatChop(0xD44842, 0xF4A49E, 0x942824));
      Tex.blit(Items.tile[Items.COOKED_MUTTON], paintMeatChop(0x884828, 0xB46C44, 0x582A14));
      Tex.blit(Items.tile[Items.ROTTEN_FLESH], paintMeatChop(0x785838, 0x628E3A, 0x4E341E));
      Tex.blit(Items.tile[Items.CARROT], paintCarrot(false));
      Tex.blit(Items.tile[Redstone319.GOLDEN_CARROT], paintCarrot(true));
      Tex.blit(Items.tile[Items.POTATO], paintPotato(false));
      Tex.blit(Items.tile[Items.BAKED_POTATO], paintPotato(true));
      Tex.blit(Items.tile[Items.MELON_SLICE], paintMelonSlice());
      Tex.blit(Items.tile[Items.PUMPKIN_PIE], paintPumpkinPie());
      Tex.blit(Items.tile[Redstone319.COOKIE], paintCookie());
      Tex.blit(Items.tile[Redstone319.MUSHROOM_STEW], paintStew(true));
      Tex.blit(Items.tile[Redstone319.BOWL], paintStew(false));
      Tex.blit(Items.tile[Items.FLINT], paintGemOrLump(0x18181E, 0x2E3038, 0x484C56, 0x828894, false));
      Tex.blit(Items.tile[Items.QUARTZ], paintGemOrLump(0x48423C, 0xC4BCB2, 0xECE6DE, 0xFFFFFF, true));
      Tex.blit(Items.tile[Items.NETHER_BRICK], paintIngot(0x1E0C10, 0x3C1C22, 0x582C34, 0x7A424C));
      Tex.blit(Items.tile[Items.BLAZE_ROD], paintBlazeRod());
      Tex.blit(Items.tile[Items.SHEARS], paintShears());
      Tex.blit(Items.tile[Items.FLINT_STEEL], paintFlintAndSteel());
      Tex.blit(Items.tile[Redstone319.COMPASS], paintDial(false));
      Tex.blit(Items.tile[Redstone319.CLOCK], paintDial(true));
      Tex.blit(Items.tile[Redstone319.ITEM_REDSTONE], paintDust(0x3E0404, 0x980C0C, 0xEC2018, 0xFF7868));
      Tex.blit(Items.tile[Items.GUNPOWDER], paintDust(0x1E1E22, 0x484850, 0x6E6E78, 0x9A9AA4));
      Tex.blit(Items.tile[Items.SUGAR], paintDust(0x484A52, 0xC4C8D2, 0xEAEEF6, 0xFFFFFF));
      Tex.blit(Items.tile[Items.BLAZE_POWDER], paintDust(0x4E2204, 0xD8680C, 0xFFA81C, 0xFFFF98));
      Tex.blit(Items.tile[Items.BONE], paintBone());
      Tex.blit(Items.tile[Items.BONE_MEAL], paintDust(0x46443E, 0xC2BEB2, 0xEAE6DC, 0xFFFFFF));
      Tex.blit(Items.tile[Items.FEATHER], paintFeather());
      Tex.blit(Items.tile[Items.STRING], paintString());
      Tex.blit(Items.tile[Items.LEATHER], paintLeather());
      Tex.blit(Items.tile[Items.WHEAT], paintWheatBundle());
      Tex.blit(Items.tile[Items.SEEDS], paintSeeds(0x2E5814, 0x62A832, 0x9EE058));
      Tex.blit(Items.tile[Items.PUMPKIN_SEEDS], paintSeeds(0x5E481E, 0xC8A862, 0xF0DC98));
      Tex.blit(Items.tile[Items.MELON_SEEDS], paintSeeds(0x221810, 0x4E3824, 0x7A5C3E));
      Tex.blit(Items.tile[Items.PAPER], paintPaper());
      Tex.blit(Items.tile[Items.BOOK], paintBook());
      Tex.blit(Items.tile[Items.EGG], paintEgg());
      Tex.blit(Items.tile[Items.CAKE], paintCakeIcon());
      Tex.blit(Items.tile[Items.DOOR], paintDoorIcon());
      Tex.blit(Items.tile[Items.BED], paintBedIcon());
      Tex.blit(Items.tile[Items.GHAST_TEAR], paintGhastTear());
      Tex.blit(Items.tile[Items.GOLD_NUGGET], paintGoldNugget());
      Tex.blit(Items.tile[Redstone319.FIRE_CHARGE], paintFireCharge());
      Tex.blit(Items.tile[Redstone319.PISTOL], paintGunSprite(0));
      Tex.blit(Items.tile[Redstone319.MACHINE_GUN], paintGunSprite(1));
      Tex.blit(Items.tile[Redstone319.SNIPER_RIFLE], paintGunSprite(2));
      Tex.blit(Items.tile[Redstone319.TNT_GRENADE], paintGrenadeSprite());
      Tex.blit(Items.tile[Redstone319.MURDER_KNIFE], paintSwordSprite(new int[]{0x281018, 0x8E2028, 0xD83838, 0xF0ECF4, 0xFFFFFF}));
      Tex.blit(Items.tile[Redstone319.C4_BOMB], paintC4Sprite());
      Tex.blit(Items.tile[Redstone319.SIEGE_CANNON], paintCannonSprite(false));
      Tex.blit(Items.tile[Redstone319.MORTAR_STRIKE], paintCannonSprite(true));

      // Polish all remaining item tiles in 256..1023 with crisp dark outlines and top-left specular bevels
      boolean[] polished = new boolean[Tex.COLS * Tex.COLS];
      for (int id = 256; id < Blocks.EXT0; id++) {
         if (Items.name[id] == null) continue;
         int t = Items.tile[id];
         if (t <= 0 || t >= polished.length || polished[t]) continue;
         polished[t] = true;
         int tx0 = Tex.tx(t);
         int ty0 = Tex.ty(t);
         int[] raw = new int[256];
         for (int y = 0; y < 16; y++) {
            System.arraycopy(Tex.atlasPixels, (ty0 + y) * Tex.PX + tx0, raw, y * 16, 16);
         }
         Tex.blit(t, outlineAndBevelSprite(raw, 0x1A1816, t + 950));
      }
   }

   private static int[] paintStick() {
      int[] p = new int[N * N];
      drawToolHandle(p, 3, 12, 9);
      return outlineAndBevelSprite(p, 0x2A1A0A, 920);
   }

   private static int[] paintIngot(int outRgb, int shRgb, int midRgb, int hiRgb) {
      int[] p = new int[N * N];
      rect(p, 3, 6, 10, 5, c(midRgb));
      rect(p, 4, 5, 8, 2, c(hiRgb));
      rect(p, 3, 10, 10, 1, c(shRgb));
      set(p, 5, 5, c(0xFFFFFF));
      return outlineAndBevelSprite(p, outRgb, 921);
   }

   private static int[] paintGemOrLump(int outRgb, int shRgb, int midRgb, int hiRgb, boolean oct) {
      int[] p = new int[N * N];
      for (int y = 3; y <= 12; y++) {
         for (int x = 4; x <= 11; x++) {
            float dx = Math.abs(x - 7.5F);
            float dy = Math.abs(y - 7.5F);
            if (dx + dy <= (oct ? 5.2F : 5.6F)) {
               int col = (x <= 6 && y <= 6) ? c(hiRgb) : ((x >= 9 || y >= 10) ? c(shRgb) : c(midRgb));
               p[y * N + x] = col;
            }
         }
      }
      return outlineAndBevelSprite(p, outRgb, 922);
   }

   private static int[] paintApple(boolean gold) {
      int[] p = new int[N * N];
      int sh = gold ? c(0xC4880C) : c(0x9E1614);
      int mid = gold ? c(0xF8CC28) : c(0xE02820);
      int hi = gold ? c(0xFFFFB8) : c(0xFF8878);
      for (int y = 4; y <= 13; y++) {
         for (int x = 3; x <= 12; x++) {
            float dx = (x - 7.5F) / 4.6F;
            float dy = (y - 8.5F) / 4.6F;
            if (dx * dx + dy * dy <= 1.0F) {
               p[y * N + x] = (x <= 6 && y <= 7) ? hi : ((x >= 10 || y >= 11) ? sh : mid);
            }
         }
      }
      set(p, 8, 2, c(0x6E4822));
      set(p, 8, 3, c(0x6E4822));
      set(p, 9, 3, c(0x489E28));
      set(p, 10, 2, c(0x64C038));
      return outlineAndBevelSprite(p, gold ? 0x4E3204 : 0x380808, 923);
   }

   private static int[] paintBow() {
      int[] p = new int[N * N];
      // Taut bowstring
      line(p, 12, 2, 4, 10, c(0xE4E4EC));
      // Recurved wooden bow limbs
      int[][] arc = {{12, 2}, {10, 2}, {8, 3}, {6, 4}, {5, 6}, {4, 8}, {4, 10}, {4, 12}};
      for (int[] a : arc) {
         set(p, a[0], a[1], c(0xB88848));
         set(p, a[0] + 1, a[1], c(0x785226));
      }
      return outlineAndBevelSprite(p, 0x2E1C0A, 924);
   }

   private static int[] paintArrow() {
      int[] p = new int[N * N];
      drawToolHandle(p, 3, 12, 9);
      // Flint tip
      rect(p, 11, 2, 3, 3, c(0xD8E0E8));
      set(p, 13, 2, c(0xFFFFFF));
      // Feather fletching
      rect(p, 2, 11, 3, 3, c(0xF0F4F8));
      return outlineAndBevelSprite(p, 0x24262A, 925);
   }

   private static int[] paintBread() {
      int[] p = new int[N * N];
      rect(p, 2, 6, 12, 5, c(0xC88E38));
      rect(p, 3, 5, 10, 2, c(0xE4AC4E));
      set(p, 5, 6, c(0xFFF098));
      set(p, 9, 6, c(0xFFF098));
      return outlineAndBevelSprite(p, 0x482E0E, 926);
   }

   private static int[] paintBucket(int fill) {
      int[] p = new int[N * N];
      rect(p, 3, 4, 10, 8, c(0xBAC2CA));
      rect(p, 4, 12, 8, 2, c(0x889098));
      int topCol = fill == 1 ? c(0x3278F0) : (fill == 2 ? c(0xFF6814) : (fill == 3 ? c(0xFFFFFF) : c(0x484E56)));
      rect(p, 4, 4, 8, 2, topCol);
      return outlineAndBevelSprite(p, 0x2A2E34, 927 + fill);
   }

   private static int[] paintEnderPearl(boolean eye) {
      int[] p = new int[N * N];
      for (int y = 3; y <= 12; y++) {
         for (int x = 3; x <= 12; x++) {
            float r = (float)Math.hypot(x - 7.5F, y - 7.5F);
            if (r <= 4.8F) {
               int col = r < 1.8F ? (eye ? c(0xB8FF58) : c(0x64F0D8))
                  : (r < 3.4F ? c(0x249888) : c(0x104E48));
               p[y * N + x] = col;
            }
         }
      }
      set(p, 6, 5, c(0xE8FFFF));
      return outlineAndBevelSprite(p, 0x082422, 932);
   }

   private static int[] paintMeatChop(int midRgb, int hiRgb, int shRgb) {
      int[] p = new int[N * N];
      for (int y = 4; y <= 12; y++) {
         for (int x = 2; x <= 13; x++) {
            float dx = (x - 7.5F) / 5.4F;
            float dy = (y - 8.2F) / 4.2F;
            if (dx * dx + dy * dy <= 1.0F) {
               p[y * N + x] = (x <= 6 && y <= 7) ? c(hiRgb) : ((x >= 10 || y >= 10) ? c(shRgb) : c(midRgb));
            }
         }
      }
      rect(p, 4, 7, 2, 2, c(0xF8F4EC));
      return outlineAndBevelSprite(p, 0x2A140C, 933);
   }

   private static int[] paintDrumstick(int midRgb, int hiRgb, int shRgb) {
      int[] p = new int[N * N];
      line(p, 3, 13, 8, 8, c(0xF4F0E8));
      set(p, 2, 13, c(0xFFFFFF)); set(p, 3, 14, c(0xDDD8CE));
      for (int y = 3; y <= 10; y++) {
         for (int x = 6; x <= 13; x++) {
            float dx = (x - 9.5F) / 3.8F;
            float dy = (y - 6.5F) / 3.5F;
            if (dx * dx + dy * dy <= 1.0F) {
               p[y * N + x] = (x <= 8 && y <= 5) ? c(hiRgb) : (y >= 8 ? c(shRgb) : c(midRgb));
            }
         }
      }
      return outlineAndBevelSprite(p, 0x2A160C, 934);
   }

   private static int[] paintCarrot(boolean gold) {
      int[] p = new int[N * N];
      int mid = gold ? c(0xF8C824) : c(0xF47818);
      int hi = gold ? c(0xFFFFB0) : c(0xFFA848);
      int sh = gold ? c(0xB88208) : c(0xB84E0C);
      for (int i = 0; i < 9; i++) {
         int x = 3 + i;
         int y = 13 - i;
         int w = 1 + i / 3;
         rect(p, x, y, w, w, mid);
         set(p, x, y, hi);
         set(p, x + w - 1, y + w - 1, sh);
      }
      rect(p, 11, 2, 3, 3, c(0x48B82E));
      set(p, 13, 2, c(0x78E052));
      return outlineAndBevelSprite(p, 0x2E1806, 935);
   }

   private static int[] paintPotato(boolean baked) {
      int[] p = new int[N * N];
      for (int y = 4; y <= 12; y++) {
         for (int x = 4; x <= 11; x++) {
            float dx = (x - 7.5F) / 3.8F;
            float dy = (y - 8.2F) / 4.2F;
            if (dx * dx + dy * dy <= 1.0F) {
               p[y * N + x] = (x <= 6 && y <= 6) ? c(0xE4BA72) : (y >= 10 ? c(0x986E34) : c(0xC6964E));
            }
         }
      }
      if (baked) {
         rect(p, 6, 6, 4, 3, c(0xFFF6D8));
         set(p, 7, 7, c(0xFFD848));
      }
      return outlineAndBevelSprite(p, 0x36220C, 936);
   }

   private static int[] paintMelonSlice() {
      int[] p = new int[N * N];
      for (int y = 4; y <= 12; y++) {
         for (int x = 2; x <= 13; x++) {
            if (y >= 6 && Math.abs(x - 7.5F) <= (13 - y) * 1.1F) {
               p[y * N + x] = y == 12 ? c(0x3E781E) : (y == 11 ? c(0x94D04E) : c(0xE83228));
            }
         }
      }
      set(p, 6, 9, c(0x281810)); set(p, 9, 9, c(0x281810));
      return outlineAndBevelSprite(p, 0x1E2E10, 937);
   }

   private static int[] paintPumpkinPie() {
      int[] p = new int[N * N];
      rect(p, 3, 6, 10, 6, c(0xD8741E));
      rect(p, 2, 5, 12, 2, c(0xE8B468));
      rect(p, 3, 11, 10, 2, c(0xAC7838));
      return outlineAndBevelSprite(p, 0x3A200C, 938);
   }

   private static int[] paintCookie() {
      int[] p = new int[N * N];
      for (int y = 4; y <= 11; y++) {
         for (int x = 4; x <= 11; x++) {
            if (Math.hypot(x - 7.5F, y - 7.5F) <= 4.2F) {
               p[y * N + x] = c(0xD49848);
            }
         }
      }
      set(p, 6, 6, c(0x422410)); set(p, 9, 7, c(0x422410)); set(p, 7, 9, c(0x422410));
      return outlineAndBevelSprite(p, 0x38200C, 939);
   }

   private static int[] paintStew(boolean filled) {
      int[] p = new int[N * N];
      rect(p, 3, 7, 10, 5, c(0x8E6032));
      rect(p, 4, 12, 8, 1, c(0x644220));
      if (filled) {
         rect(p, 4, 6, 8, 2, c(0xD48868));
         set(p, 6, 6, c(0xE03228)); set(p, 9, 6, c(0xA47448));
      } else {
         rect(p, 4, 6, 8, 2, c(0x4E3418));
      }
      return outlineAndBevelSprite(p, 0x2A1A0A, 940);
   }

   private static int[] paintBlazeRod() {
      int[] p = new int[N * N];
      for (int i = 0; i < 10; i++) {
         int x = 3 + i;
         int y = 12 - i;
         set(p, x, y, c(0xFFFFA8));
         set(p, x + 1, y, c(0xFFA818));
         set(p, x, y + 1, c(0xD85808));
      }
      return outlineAndBevelSprite(p, 0x4E1804, 941);
   }

   private static int[] paintShears() {
      int[] p = new int[N * N];
      rect(p, 3, 9, 4, 4, c(0xB83224));
      rect(p, 7, 10, 4, 4, c(0xB83224));
      line(p, 6, 8, 12, 2, c(0xE4ECF4));
      line(p, 7, 9, 13, 5, c(0xBAC2CA));
      return outlineAndBevelSprite(p, 0x282C32, 942);
   }

   private static int[] paintFlintAndSteel() {
      int[] p = new int[N * N];
      rect(p, 3, 5, 4, 7, c(0xC8D0D8));
      rect(p, 4, 10, 5, 3, c(0x98A0A8));
      rect(p, 9, 5, 4, 5, c(0x383C44));
      set(p, 10, 6, c(0x78808C));
      return outlineAndBevelSprite(p, 0x1C1E24, 943);
   }

   private static int[] paintDial(boolean clock) {
      int[] p = new int[N * N];
      int rim = clock ? c(0xF8CC28) : c(0xC4CCD4);
      int face = clock ? c(0x3258A8) : c(0x282C34);
      for (int y = 3; y <= 12; y++) {
         for (int x = 3; x <= 12; x++) {
            float r = (float)Math.hypot(x - 7.5F, y - 7.5F);
            if (r <= 4.8F) {
               p[y * N + x] = r >= 3.4F ? rim : face;
            }
         }
      }
      if (clock) {
         set(p, 7, 6, c(0xFFFF98)); set(p, 8, 6, c(0xFFFF98));
      } else {
         line(p, 7, 5, 7, 7, c(0xFF3228));
         line(p, 7, 8, 7, 10, c(0xE8EEF4));
      }
      return outlineAndBevelSprite(p, 0x222428, 944);
   }

   private static int[] paintDust(int outRgb, int shRgb, int midRgb, int hiRgb) {
      int[] p = new int[N * N];
      for (int y = 5; y <= 12; y++) {
         for (int x = 3; x <= 12; x++) {
            if (Math.abs(x - 7.5F) <= (y - 3) * 0.65F) {
               p[y * N + x] = y <= 7 ? c(hiRgb) : (y >= 11 ? c(shRgb) : c(midRgb));
            }
         }
      }
      return outlineAndBevelSprite(p, outRgb, 945);
   }

   private static int[] paintBone() {
      int[] p = new int[N * N];
      line(p, 4, 12, 11, 5, c(0xEAE6DC));
      line(p, 5, 12, 12, 5, c(0xC8C2B4));
      rect(p, 2, 11, 3, 3, c(0xF6F4EC));
      rect(p, 11, 2, 3, 3, c(0xF6F4EC));
      return outlineAndBevelSprite(p, 0x3C3830, 946);
   }

   private static int[] paintFeather() {
      int[] p = new int[N * N];
      line(p, 3, 13, 12, 3, c(0xE8EEF6));
      line(p, 4, 11, 11, 3, c(0xFFFFFF));
      line(p, 5, 12, 13, 4, c(0xC2CAD4));
      return outlineAndBevelSprite(p, 0x383E46, 947);
   }

   private static int[] paintString() {
      int[] p = new int[N * N];
      line(p, 3, 12, 7, 8, c(0xF0F4F8));
      line(p, 7, 8, 10, 10, c(0xD8E0E8));
      line(p, 10, 10, 13, 4, c(0xFFFFFF));
      return outlineAndBevelSprite(p, 0x383E46, 948);
   }

   private static int[] paintLeather() {
      int[] p = new int[N * N];
      rect(p, 4, 4, 8, 9, c(0xB46638));
      rect(p, 2, 4, 3, 4, c(0xC87846));
      rect(p, 11, 4, 3, 4, c(0x9A5228));
      return outlineAndBevelSprite(p, 0x3E1E0C, 949);
   }

   private static int[] paintWheatBundle() {
      int[] p = new int[N * N];
      rect(p, 4, 3, 8, 10, c(0xE4BA44));
      rect(p, 5, 2, 6, 3, c(0xF8DC68));
      rect(p, 4, 9, 8, 2, c(0xB83224));
      return outlineAndBevelSprite(p, 0x48320C, 950);
   }

   private static int[] paintSeeds(int outRgb, int midRgb, int hiRgb) {
      int[] p = new int[N * N];
      int[][] pts = {{4, 6}, {8, 5}, {11, 7}, {5, 10}, {9, 10}, {7, 8}};
      for (int[] pt : pts) {
         rect(p, pt[0], pt[1], 2, 2, c(midRgb));
         set(p, pt[0], pt[1], c(hiRgb));
      }
      return outlineAndBevelSprite(p, outRgb, 951);
   }

   private static int[] paintPaper() {
      int[] p = new int[N * N];
      rect(p, 4, 3, 8, 10, c(0xF4F2EA));
      line(p, 5, 5, 10, 5, c(0xC8C4B8));
      line(p, 5, 8, 10, 8, c(0xC8C4B8));
      line(p, 5, 11, 9, 11, c(0xC8C4B8));
      return outlineAndBevelSprite(p, 0x48443C, 952);
   }

   private static int[] paintBook() {
      int[] p = new int[N * N];
      rect(p, 4, 3, 8, 10, c(0x8E4822));
      rect(p, 4, 3, 2, 10, c(0x5E2C12));
      rect(p, 11, 4, 2, 8, c(0xF4EFE2));
      line(p, 6, 6, 9, 6, c(0xF8D048));
      return outlineAndBevelSprite(p, 0x2C1408, 953);
   }

   private static int[] paintEgg() {
      int[] p = new int[N * N];
      for (int y = 3; y <= 12; y++) {
         for (int x = 4; x <= 11; x++) {
            float dx = (x - 7.5F) / 3.6F;
            float dy = (y - 8.0F) / 4.6F;
            if (dx * dx + dy * dy <= 1.0F) {
               p[y * N + x] = y <= 6 ? c(0xFFFBF0) : c(0xE6DCC8);
            }
         }
      }
      return outlineAndBevelSprite(p, 0x4A4236, 954);
   }

   private static int[] paintCakeIcon() {
      int[] p = new int[N * N];
      rect(p, 3, 8, 10, 5, c(0xC88642));
      rect(p, 3, 5, 10, 3, c(0xF8F4EE));
      set(p, 5, 6, c(0xE02822)); set(p, 8, 5, c(0xE02822)); set(p, 10, 6, c(0xE02822));
      return outlineAndBevelSprite(p, 0x3A220E, 955);
   }

   private static int[] paintDoorIcon() {
      int[] p = new int[N * N];
      rect(p, 4, 2, 8, 12, c(0xB88E52));
      rect(p, 5, 3, 2, 3, c(0x463018));
      rect(p, 9, 3, 2, 3, c(0x463018));
      set(p, 10, 8, c(0xF0F4F8));
      return outlineAndBevelSprite(p, 0x2E1C0A, 956);
   }

   private static int[] paintBedIcon() {
      int[] p = new int[N * N];
      rect(p, 2, 6, 12, 5, c(0xD82C24));
      rect(p, 2, 6, 4, 5, c(0xF4F2EC));
      rect(p, 2, 11, 12, 2, c(0x9E7440));
      return outlineAndBevelSprite(p, 0x2E1A0C, 957);
   }

   private static int[] paintGhastTear() {
      int[] p = new int[N * N];
      rect(p, 6, 4, 4, 8, c(0xDCEEF8));
      rect(p, 5, 7, 6, 4, c(0xB8D8EC));
      set(p, 7, 3, c(0xFFFFFF));
      return outlineAndBevelSprite(p, 0x365064, 958);
   }

   private static int[] paintGoldNugget() {
      int[] p = new int[N * N];
      rect(p, 5, 5, 6, 6, c(0xF8CC28));
      set(p, 6, 6, c(0xFFFFB8));
      return outlineAndBevelSprite(p, 0x4E3204, 959);
   }

   private static int[] paintFireCharge() {
      int[] p = new int[N * N];
      for (int y = 3; y <= 12; y++) {
         for (int x = 3; x <= 12; x++) {
            float r = (float)Math.hypot(x - 7.5F, y - 7.5F);
            if (r <= 4.6F) {
               p[y * N + x] = r < 2.0F ? c(0xFFFF98) : (r < 3.4F ? c(0xFF7818) : c(0x42241C));
            }
         }
      }
      return outlineAndBevelSprite(p, 0x1A0C08, 960);
   }

   private static int[] paintGunSprite(int kind) {
      int[] p = new int[N * N];
      if (kind == 0) {
         // Pistol
         rect(p, 3, 5, 9, 3, c(0x525864));
         rect(p, 3, 5, 9, 1, c(0x8892A0));
         rect(p, 3, 8, 3, 5, c(0x6E4826));
      } else if (kind == 1) {
         // SMG / Machine Gun
         rect(p, 2, 5, 12, 3, c(0x464C56));
         rect(p, 3, 5, 10, 1, c(0x828C9A));
         rect(p, 3, 8, 3, 4, c(0x32363E));
         rect(p, 8, 8, 2, 5, c(0xD8A828));
      } else {
         // Sniper Rifle
         rect(p, 1, 6, 14, 2, c(0x48583E));
         rect(p, 5, 4, 5, 2, c(0x282C34));
         rect(p, 6, 4, 3, 1, c(0x68D8FF));
         rect(p, 2, 8, 4, 3, c(0x6E4826));
      }
      return outlineAndBevelSprite(p, 0x14161A, 961 + kind);
   }

   private static int[] paintGrenadeSprite() {
      int[] p = new int[N * N];
      rect(p, 5, 5, 6, 8, c(0x3E6432));
      rect(p, 6, 3, 4, 2, c(0x98A0A8));
      line(p, 5, 8, 10, 8, c(0x284220));
      return outlineAndBevelSprite(p, 0x182412, 964);
   }

   private static int[] paintC4Sprite() {
      int[] p = new int[N * N];
      rect(p, 3, 4, 10, 8, c(0x8E8268));
      rect(p, 5, 6, 6, 4, c(0x22262C));
      set(p, 7, 7, c(0xFF2820)); set(p, 8, 7, c(0xFF2820));
      return outlineAndBevelSprite(p, 0x242018, 965);
   }

   private static int[] paintCannonSprite(boolean mortar) {
      int[] p = new int[N * N];
      if (mortar) {
         line(p, 4, 12, 11, 3, c(0x4A525C));
         line(p, 5, 12, 12, 3, c(0x788290));
         rect(p, 3, 12, 8, 2, c(0x6E4826));
         set(p, 12, 2, c(0xFFA824));
      } else {
         rect(p, 2, 5, 11, 4, c(0x3C424A));
         rect(p, 3, 5, 9, 1, c(0x788290));
         rect(p, 4, 9, 5, 4, c(0x7C5428));
         set(p, 13, 6, c(0xFFA824));
      }
      return outlineAndBevelSprite(p, 0x16181C, 966);
   }
}
