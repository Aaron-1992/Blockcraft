package bc;

/**
 * V6.0 — 100% Brand-New Textures From Scratch for EVERY Block & EVERY Item in Blockcraft.
 * Every single block tile (0..687) and every single item sprite (256..731) is painted 100% new from scratch
 * (zero readTile pass-throughs or partial fusions with legacy tiles) and shaded with realistic 3D relief.
 */
final class TexBrandNewV60 {
   private static final int N = 16;

   private TexBrandNewV60() {
   }

   private static int c(int rgb) {
      return 0xFF000000 | (rgb & 0xFFFFFF);
   }

   private static int rgb(int r, int g, int b) {
      r = Math.max(0, Math.min(255, r));
      g = Math.max(0, Math.min(255, g));
      b = Math.max(0, Math.min(255, b));
      return 0xFF000000 | (r << 16) | (g << 8) | b;
   }

   private static int mul(int col, float k) {
      int a = col & 0xFF000000;
      if (a == 0) return 0;
      int r = Math.round(((col >> 16) & 255) * k);
      int g = Math.round(((col >> 8) & 255) * k);
      int b = Math.round((col & 255) * k);
      return a | (Math.max(0, Math.min(255, r)) << 16) | (Math.max(0, Math.min(255, g)) << 8) | Math.max(0, Math.min(255, b));
   }

   private static int mix(int a, int b, float t) {
      return TexPaint.mix(a, b, t);
   }

   private static float hash(int x, int y, int seed) {
      return TexPaint.hash(x, y, seed);
   }

   private static float fbm(int x, int y, int seed) {
      return TexPaint.fbm(x, y, seed);
   }

   private static void set(int[] p, int x, int y, int col) {
      if (x >= 0 && x < N && y >= 0 && y < N) {
         p[y * N + x] = col;
      }
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
         if (x0 == x1 && y0 == y1) return;
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

   private static void putBlock(int tile, int[] p, boolean seamless) {
      if (tile >= 0 && tile < Tex.COLS * Tex.COLS) {
         Tex.blit(tile, TexOverhaulV57.applyRealisticRelief(p, 0.72F, seamless));
      }
   }

   private static void putSprite(int tile, int[] raw) {
      if (tile <= 0 || tile >= Tex.COLS * Tex.COLS) return;
      int[] p = raw.clone();
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            int col = raw[y * N + x];
            if ((col >>> 24) < 32) continue;
            boolean rOpen = x == 15 || (raw[y * N + x + 1] >>> 24) < 32;
            boolean bOpen = y == 15 || (raw[(y + 1) * N + x] >>> 24) < 32;
            boolean lOpen = x == 0 || (raw[y * N + x - 1] >>> 24) < 32;
            boolean tOpen = y == 0 || (raw[(y - 1) * N + x] >>> 24) < 32;
            if (rOpen || bOpen) {
               p[y * N + x] = mul(col, 0.66F);
            } else if (lOpen || tOpen) {
               p[y * N + x] = mul(col, 1.15F);
            } else {
               float g = 1.0F + (hash(x, y, tile + 401) - 0.5F) * 0.06F;
               p[y * N + x] = mul(col, g);
            }
         }
      }
      Tex.blit(tile, TexOverhaulV57.applyRealisticRelief(p, 0.58F, false));
   }

   private static void putItem(int itemId, int[] raw) {
      if (itemId >= 0 && itemId < Items.tile.length) {
         putSprite(Items.tile[itemId], raw);
      }
   }

   static void applyAll() {
      paintAllTerrainAndNaturalBlocks();
      paintAllWoodsLeavesAndPlants();
      paintAllMasonryOresAndMinerals();
      paintAllWorkstationsCropsAndUtility();
      paintAllNetherAndEndBlocks();
      paintAllColorsGlassAndTerracotta();
      paintAllShapesBedsBannersAndRedstone();
      paintAllAnimatedFrames();
      paintAllToolsWeaponsAndArmor();
      paintAllEverySingleItemSprite();
   }

   // ====================================================================
   //  1. Terrain & Natural Blocks (100% Brand-New From Scratch)
   // ====================================================================
   private static void paintAllTerrainAndNaturalBlocks() {
      // 0: Grass Top (grayscale biome-tinted lush multi-blade turf)
      int[] grassTop = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float cl = fbm(x, y, 6001);
            float blade = hash(x, (y + (x & 1)) & 15, 6002);
            int v = Math.round(146 + cl * 38 + (blade > 0.72F ? 18 : (blade < 0.22F ? -18 : 0)));
            if (((x * 3 + y * 5) & 7) == 0 && y > 0) v += 14;
            grassTop[y * N + x] = rgb(v, v, v);
         }
      }
      putBlock(Tex.GRASS_TOP, grassTop, true);

      // 1: Dirt, 528: Coarse Dirt, 524/525: Podzol, 526/527: Mycelium
      putBlock(Tex.DIRT, paintSoil(c(0x966844), c(0x785032), c(0x5E3C22), c(0xB2825A), false, 6010), true);
      putBlock(TexArt313.COARSE_DIRT, paintSoil(c(0x885C3A), c(0x684428), c(0x4E321C), c(0xA4988C), true, 6011), true);
      putBlock(TexArt313.PODZOL_TOP, paintPodzolTop(), true);
      putBlock(TexArt313.PODZOL_SIDE, paintSideCap(paintSoil(c(0x966844), c(0x785032), c(0x5E3C22), c(0xB2825A), false, 6012), c(0x6E441E), c(0x4E2E12), c(0x8E5A2A), 6013), true);
      putBlock(TexArt313.MYCELIUM_TOP, paintMyceliumTop(), true);
      putBlock(TexArt313.MYCELIUM_SIDE, paintSideCap(paintSoil(c(0x966844), c(0x785032), c(0x5E3C22), c(0xB2825A), false, 6014), c(0x7A6878), c(0x584858), c(0x988698), 6015), true);
      putBlock(TexArt313.SNOW_SIDE, paintSideCap(paintSoil(c(0x966844), c(0x785032), c(0x5E3C22), c(0xB2825A), false, 6016), c(0xF2F8FC), c(0xC8DCE8), c(0xFFFFFF), 6017), true);

      // 2: Grass Side Overlay (alpha-masked hanging turf blades)
      int[] grassSide = new int[N * N];
      int[] drop = {4, 5, 3, 6, 4, 3, 5, 7, 4, 3, 6, 5, 3, 5, 4, 3};
      for (int x = 0; x < N; x++) {
         int h = drop[x];
         for (int y = 0; y <= h; y++) {
            int v = y == 0 ? 186 : (y == h ? 128 : 156 + (int)((hash(x, y, 6020) - 0.5F) * 28));
            grassSide[y * N + x] = rgb(v, v, v);
         }
      }
      Tex.blit(Tex.GRASS_SIDE_OVL, grassSide);

      // 3: Stone, 520/648: Granite, 521/649: Diorite, 522/650: Andesite
      int[] stone = paintRock(c(0x828488), c(0x6E7074), c(0x585A5E), c(0x989AA0), 6030);
      putBlock(Tex.STONE, stone, true);
      putBlock(TexArt313.GRANITE, paintRock(c(0x9E6C58), c(0x825442), c(0x643E30), c(0xBA8872), 6031), true);
      putBlock(Redstone319.T_POL_GRANITE, paintPolishedStone(c(0xA4705C), c(0x845644), c(0x623E30), c(0xC4927C), 6032), false);
      putBlock(TexArt313.DIORITE, paintRock(c(0xC8C8CC), c(0x9E9EA4), c(0x727278), c(0xECECF2), 6033), true);
      putBlock(Redstone319.T_POL_DIORITE, paintPolishedStone(c(0xD2D2D6), c(0xA8A8AE), c(0x7E7E84), c(0xF6F6FA), 6034), false);
      putBlock(TexArt313.ANDESITE, paintRock(c(0x888A8E), c(0x707276), c(0x5A5C60), c(0xA4A6AA), 6035), true);
      putBlock(Redstone319.T_POL_ANDESITE, paintPolishedStone(c(0x8E9094), c(0x74767A), c(0x5A5C60), c(0xAEB0B4), 6036), false);

      // 4: Cobblestone, 44: Mossy Cobblestone
      putBlock(Tex.COBBLE, paintCobble(false), true);
      putBlock(Tex.MOSSY, paintCobble(true), true);

      // 5: Sand, 523: Red Sand, 6: Gravel, 11: Bedrock, 18: Snow, 529: Ice, 530: Packed Ice, 465: Clay
      putBlock(Tex.SAND, paintSand(c(0xE0D49E), c(0xCEBE84), c(0xB8A66C), c(0xF2E8B8), 6040), true);
      putBlock(TexArt313.RED_SAND, paintSand(c(0xC66828), c(0xAC541C), c(0x8E4212), c(0xDE823C), 6041), true);
      putBlock(Tex.GRAVEL, paintGravel(), true);
      putBlock(Tex.BEDROCK, paintBedrock(), true);
      putBlock(Tex.SNOW, paintSnow(), true);
      putBlock(TexArt313.ICE, paintIce(false), true);
      putBlock(TexArt313.PACKED_ICE, paintIce(true), true);
      putBlock(Color312.T_CLAY, paintRock(c(0xA2A8B8), c(0x8E94A4), c(0x7A8090), c(0xB8C0D0), 6048), true);
   }

   private static int[] paintSoil(int mid, int dk, int deep, int hi, boolean coarse, int seed) {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float n = fbm(x, y, seed);
            float h = hash(x, y, seed + 17);
            int col = n < 0.34F ? dk : (n > 0.68F ? mix(mid, hi, 0.45F) : mid);
            if (h < 0.14F) col = deep;
            else if (coarse && h > 0.76F) col = hi;
            else if (h > 0.86F) col = hi;
            p[y * N + x] = col;
         }
      }
      return p;
   }

   private static int[] paintPodzolTop() {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float n = fbm(x, y, 6050);
            int col = n < 0.35F ? c(0x4E3016) : (n > 0.68F ? c(0x885628) : c(0x6A421E));
            if (((x * 3 + y) & 7) == 0) col = c(0xA46C34);
            p[y * N + x] = col;
         }
      }
      return p;
   }

   private static int[] paintMyceliumTop() {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float n = fbm(x, y, 6052);
            int col = n < 0.35F ? c(0x5A4C5A) : (n > 0.68F ? c(0x8A7888) : c(0x726270));
            if (hash(x, y, 6053) > 0.84F) col = c(0x9E8E9C);
            p[y * N + x] = col;
         }
      }
      return p;
   }

   private static int[] paintSideCap(int[] dirt, int capMid, int capSh, int capHi, int seed) {
      int[] p = dirt.clone();
      int[] drop = {4, 5, 4, 6, 5, 3, 5, 6, 4, 3, 6, 5, 4, 5, 4, 3};
      for (int x = 0; x < N; x++) {
         int h = drop[(x + seed) & 15];
         for (int y = 0; y <= h; y++) {
            p[y * N + x] = y == 0 ? capHi : (y == h ? capSh : (hash(x, y, seed) > 0.65F ? capHi : capMid));
         }
      }
      return p;
   }

   private static int[] paintRock(int mid, int dk, int deep, int hi, int seed) {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float n = fbm((x + (y >> 1)) & 15, y, seed);
            float h = hash(x, y, seed + 29);
            int col = n < 0.34F ? dk : (n > 0.68F ? hi : mid);
            if (((y & 3) == 2 && h < 0.45F) || h < 0.10F) col = deep;
            p[y * N + x] = col;
         }
      }
      return p;
   }

   private static int[] paintPolishedStone(int mid, int dk, int deep, int hi, int seed) {
      int[] p = paintRock(mid, dk, deep, hi, seed);
      for (int i = 0; i < N; i++) {
         p[i] = hi;
         p[i * N] = hi;
         p[15 * N + i] = deep;
         p[i * N + 15] = deep;
      }
      return p;
   }

   private static int[] paintCobble(boolean mossy) {
      int[] p = new int[N * N];
      int[][] stones = {
         {1, 1, 6, 4}, {8, 1, 7, 5}, {0, 6, 5, 5}, {6, 6, 5, 4}, {12, 7, 4, 5},
         {1, 12, 6, 4}, {8, 11, 7, 5}
      };
      for (int i = 0; i < N * N; i++) p[i] = mossy && (i % 3 == 0) ? c(0x3E6828) : c(0x46484C);
      for (int s = 0; s < stones.length; s++) {
         int[] st = stones[s];
         for (int dy = 0; dy < st[3]; dy++) {
            for (int dx = 0; dx < st[2]; dx++) {
               int x = (st[0] + dx) & 15;
               int y = (st[1] + dy) & 15;
               int col = (dx == 0 || dy == 0) ? c(0x9E9FA4) : ((dx == st[2] - 1 || dy == st[3] - 1) ? c(0x5E6064) : c(0x7E8084));
               if (mossy && hash(x, y, 6060 + s) > 0.56F) {
                  col = dy == 0 ? c(0x72A644) : c(0x528230);
               }
               p[y * N + x] = col;
            }
         }
      }
      return p;
   }

   private static int[] paintSand(int mid, int dk, int deep, int hi, int seed) {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float wave = (float)Math.sin((x + y * 2) * Math.PI / 8.0);
            float h = hash(x, y, seed);
            int col = wave > 0.35F ? hi : (wave < -0.35F ? dk : mid);
            if (h > 0.85F) col = hi;
            else if (h < 0.12F) col = deep;
            p[y * N + x] = col;
         }
      }
      return p;
   }

   private static int[] paintGravel() {
      int[] p = new int[N * N];
      int[] pal = {c(0x585452), c(0x74706E), c(0x8C8684), c(0xA49C98), c(0xB8B0AC)};
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            int idx = (int)(hash(x >> 1, y >> 1, 6070) * 4.99F);
            int col = pal[idx];
            if ((x & 1) == 0 && (y & 1) == 0) col = mul(col, 1.12F);
            else if ((x & 1) == 1 && (y & 1) == 1) col = mul(col, 0.84F);
            p[y * N + x] = col;
         }
      }
      return p;
   }

   private static int[] paintBedrock() {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float v = hash(x, y, 6075) * 0.5F + fbm(x, y, 6076) * 0.5F;
            p[y * N + x] = v < 0.25F ? c(0x18181C) : (v < 0.50F ? c(0x323238) : (v < 0.76F ? c(0x54545C) : c(0x787882)));
         }
      }
      return p;
   }

   private static int[] paintSnow() {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float h = hash(x, y, 6080);
            p[y * N + x] = h > 0.75F ? c(0xFFFFFF) : (h < 0.20F ? c(0xD8E8F2) : c(0xEEF6FC));
         }
      }
      return p;
   }

   private static int[] paintIce(boolean packed) {
      int[] p = new int[N * N];
      int base = packed ? c(0x7EA4EC) : c(0x88B8FA);
      int hi = packed ? c(0xB4D2FF) : c(0xCEE6FF);
      int sh = packed ? c(0x5E82CE) : c(0x6A96E0);
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            int d1 = (x + y) & 7;
            int d2 = (x - y + 16) & 7;
            p[y * N + x] = d1 == 0 ? hi : (d2 == 3 ? sh : base);
         }
      }
      return p;
   }

   // ====================================================================
   //  2. All 6 Woods, Planks, Logs, Leaves & All Botanical Plants
   // ====================================================================
   private static void paintAllWoodsLeavesAndPlants() {
      // Planks (10, 534..538)
      putBlock(Tex.PLANKS, paintPlanks(c(0xAC8852), c(0x8E6C3C), c(0x6E5028), c(0xC6A268), 6100), true);
      putBlock(TexArt313.PLANKS0, paintPlanks(c(0x785834), c(0x5E4224), c(0x463018), c(0x926E44), 6101), true);
      putBlock(TexArt313.PLANKS0 + 1, paintPlanks(c(0xCEBC82), c(0xB4A068), c(0x948250), c(0xE2D29A), 6102), true);
      putBlock(TexArt313.PLANKS0 + 2, paintPlanks(c(0xA87854), c(0x8C6040), c(0x6E482E), c(0xC2906A), 6103), true);
      putBlock(TexArt313.PLANKS0 + 3, paintPlanks(c(0xBE683A), c(0x9E5028), c(0x7C3C1A), c(0xD88050), 6104), true);
      putBlock(TexArt313.PLANKS0 + 4, paintPlanks(c(0x483018), c(0x362210), c(0x241608), c(0x5E4024), 6105), true);

      // Log Barks & Rings (7, 8, 31, 32, 33, 246..248, 540..548)
      putBlock(Tex.LOG_SIDE, paintBark(c(0x6E5230), c(0x523C20), c(0x382812), c(0x886840), false, 6110), true);
      putBlock(Tex.SPRUCE_SIDE, paintBark(c(0x422C18), c(0x301E0E), c(0x201208), c(0x583C22), false, 6111), true);
      putBlock(Tex.BIRCH_SIDE, paintBirchBark(), true);
      TexArt310.blocks(); // Syncs turned logs 246..248 from 7, 33, 31!

      putBlock(Tex.LOG_TOP, paintLogRings(c(0x5E4628), c(0xBC9862), c(0x9E7C48)), false);
      putBlock(Tex.SPRUCE_TOP, paintLogRings(c(0x382412), c(0x927042), c(0x765830)), false);

      int[][] bark313 = {
         {0x5C4622, 0x423216, 0x2C3E18, 0x765C32}, // Jungle (with mossy green undertone)
         {0x6A645C, 0x524C46, 0x3C3832, 0x827C74}, // Acacia
         {0x3C2C1A, 0x2A1E10, 0x1C1208, 0x503C26}  // Dark Oak
      };
      int[][] ring313 = {
         {0x5A4422, 0xB48858, 0x966C40},
         {0x645E56, 0xBA683C, 0x9A5028},
         {0x362616, 0x583E24, 0x422E18}
      };
      for (int k = 0; k < 3; k++) {
         int[] b = bark313[k];
         putBlock(TexArt313.BARK0 + k, paintBark(c(b[0]), c(b[1]), c(b[2]), c(b[3]), false, 6120 + k), true);
         putBlock(TexArt313.RING0 + k, paintLogRings(c(ring313[k][0]), c(ring313[k][1]), c(ring313[k][2])), false);
         int[] src = TexOverhaulV57.readTile(TexArt313.BARK0 + k);
         int[] tr = new int[N * N];
         for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
               tr[y * N + x] = src[x * N + y];
            }
         }
         Tex.blit(TexArt313.TURNED0 + k, tr);
      }

      // Leaves (9, 550..552, 642)
      Tex.blit(Tex.LEAVES, paintLeaves(6130));
      for (int k = 0; k < 3; k++) {
         Tex.blit(TexArt313.LEAVES0 + k, paintLeaves(6131 + k));
      }
      Tex.blit(642, paintLeaves(6135));

      // Cactus (28, 29) & Sugar Cane (219)
      putBlock(Tex.CACTUS_SIDE, paintCactusSide(), false);
      putBlock(Tex.CACTUS_TOP, paintCactusTop(), false);
      putSprite(219, paintSugarCane());

      // Plants & Flowers (25, 26, 27, 553, 554, 556..567, 570..583)
      putSprite(Tex.TALLGRASS, paintGrassTuft(false));
      putSprite(TexArt313.FERN, paintGrassTuft(true));
      putSprite(TexArt313.DEAD_BUSH, paintDeadBush());
      putSprite(Tex.FLOWER_RED, paintFlower(c(0xE62420), c(0x98100E), c(0xFF7868), c(0x382010), 0));
      putSprite(Tex.FLOWER_YELLOW, paintFlower(c(0xFCEE38), c(0xC8A810), c(0xFFFFA8), c(0xE89810), 1));

      int[][] flPal = {
         {0x2E98F6, 0x1862B8, 0x98DCFF, 0xF8F8BA}, // 0 Blue Orchid
         {0xB852E8, 0x7E28AC, 0xE8A8FF, 0xD878FF}, // 1 Allium
         {0xDCE4F2, 0xA8B4C8, 0xFFFFFF, 0xF8D848}, // 2 Azure Bluet
         {0xE22424, 0x9A1212, 0xFF7878, 0x581010}, // 3 Red Tulip
         {0xF27820, 0xB44E0E, 0xFFB868, 0xFCE048}, // 4 Orange Tulip
         {0xF2F4F8, 0xC4C8D2, 0xFFFFFF, 0xD8E8B8}, // 5 White Tulip
         {0xF488C4, 0xBA528E, 0xFFC8E8, 0xFFF0F8}, // 6 Pink Tulip
         {0xF4F6FA, 0xC6CAD2, 0xFFFFFF, 0xF6C828}, // 7 Oxeye Daisy
         {0x3866E8, 0x1C3CA4, 0x8EB4FF, 0x202C68}, // 8 Cornflower
         {0xF6F8FC, 0xC8D0DC, 0xFFFFFF, 0xE2F0D8}  // 9 Lily of the Valley
      };
      for (int i = 0; i < 10; i++) {
         int[] fp = flPal[i];
         putSprite(TexArt313.FLOWER0 + i, paintFlower(c(fp[0]), c(fp[1]), c(fp[2]), c(fp[3]), i + 2));
      }

      putSprite(TexArt313.MUSH_BROWN, paintMushroom(false));
      putSprite(TexArt313.MUSH_RED, paintMushroom(true));

      // Double-Tall Plants (570..581)
      for (int i = 0; i < 6; i++) {
         putSprite(TexArt313.DOUBLE0 + i * 2, paintDoublePlant(i, false));
         putSprite(TexArt313.DOUBLE0 + i * 2 + 1, paintDoublePlant(i, true));
      }
      putSprite(TexArt313.LILY, paintLilyPad());
      putSprite(TexArt313.VINE, paintVines());

      // Giant Mushroom Blocks (584..586)
      putBlock(TexArt313.MUSH_BLOCK_RED, paintGiantMushRed(), true);
      putBlock(TexArt313.MUSH_BLOCK_BROWN, paintRock(c(0x987252), c(0x7E5C40), c(0x64462E), c(0xB28A68), 6180), true);
      putBlock(TexArt313.MUSH_STEM, paintBark(c(0xD4CFC4), c(0xBCB6AA), c(0xA29C90), c(0xECE8DE), false, 6181), true);
   }

   private static int[] paintPlanks(int mid, int dk, int seam, int hi, int seed) {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         int row = y >> 2;
         int py = y & 3;
         int jointX = (row * 7 + 5) & 15;
         for (int x = 0; x < N; x++) {
            float g = fbm((x + row * 5) & 15, y, seed);
            int col = py == 3 || x == jointX ? seam : (py == 0 ? mix(mid, hi, 0.55F) : (g < 0.36F ? dk : (g > 0.68F ? hi : mid)));
            p[y * N + x] = col;
         }
      }
      return p;
   }

   private static int[] paintBark(int mid, int dk, int deep, int hi, boolean horiz, int seed) {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            int u = horiz ? y : x;
            int v = horiz ? x : y;
            int rib = u & 3;
            float n = fbm(u, v >> 1, seed);
            int col = rib == 0 ? deep : (rib == 1 ? hi : (rib == 2 ? mid : dk));
            if (n < 0.28F) col = mul(col, 0.88F);
            else if (n > 0.72F) col = mul(col, 1.10F);
            p[y * N + x] = col;
         }
      }
      return p;
   }

   private static int[] paintBirchBark() {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float n = hash(x >> 1, y, 6115);
            int col = n < 0.25F ? c(0xC8C2B6) : (n > 0.75F ? c(0xECE8DE) : c(0xDCD6CA));
            if ((y == 3 && x >= 2 && x <= 6) || (y == 8 && x >= 9 && x <= 14) || (y == 13 && x >= 4 && x <= 9)) {
               col = (x & 1) == 0 ? c(0x36322C) : c(0x524C44);
            }
            p[y * N + x] = col;
         }
      }
      return p;
   }

   private static int[] paintLogRings(int bark, int sap, int heart) {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            int d = Math.max(Math.abs(x - 7) - (x >= 8 ? 1 : 0), Math.abs(y - 7) - (y >= 8 ? 1 : 0));
            p[y * N + x] = d >= 6 ? bark : ((d & 1) == 0 ? sap : heart);
         }
      }
      return p;
   }

   private static int[] paintLeaves(int seed) {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            int cx = (x + ((y >> 1) & 1)) & 3;
            int cy = y & 3;
            float h = hash(x, y, seed);
            if (cx == 3 && cy == 3 && h < 0.42F) {
               p[y * N + x] = 0;
            } else {
               int v = (cx == 0 || cy == 0) ? 188 : ((cx == 3 || cy == 3) ? 116 : 152);
               v += Math.round((h - 0.5F) * 24.0F);
               p[y * N + x] = rgb(v, v, v);
            }
         }
      }
      return TexOverhaulV57.applyRealisticRelief(p, 0.75F, true);
   }

   private static int[] paintCactusSide() {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            int rib = x & 3;
            int col = rib == 0 ? c(0x285818) : (rib == 1 ? c(0x589E38) : (rib == 2 ? c(0x428228) : c(0x326A1E)));
            if ((x == 2 || x == 6 || x == 10 || x == 14) && ((y + (x >> 1)) & 3) == 1) {
               col = c(0xE8E2B8);
            }
            p[y * N + x] = col;
         }
      }
      return p;
   }

   private static int[] paintCactusTop() {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            int d = Math.max(Math.abs(x - 7) - (x >= 8 ? 1 : 0), Math.abs(y - 7) - (y >= 8 ? 1 : 0));
            p[y * N + x] = d == 7 ? c(0x285818) : (d <= 1 ? c(0x88BA62) : ((d & 1) == 0 ? c(0x589E38) : c(0x3E7A24)));
         }
      }
      return p;
   }

   private static int[] paintSugarCane() {
      int[] p = new int[N * N];
      int[] cols = {3, 7, 12};
      for (int sx : cols) {
         for (int y = 0; y < N; y++) {
            boolean node = (y & 3) == 2;
            set(p, sx, y, node ? c(0xC2DC78) : c(0x88C848));
            set(p, sx + 1, y, node ? c(0x98B852) : c(0x5E9A2E));
            if (y == 4 || y == 10) {
               set(p, sx - 1, y, c(0x78B83C));
               set(p, sx + 2, y + 1, c(0x589228));
            }
         }
      }
      return p;
   }

   private static int[] paintGrassTuft(boolean fern) {
      int[] p = new int[N * N];
      for (int b = 0; b < 7; b++) {
         int bx = 2 + b * 2;
         int h = 6 + ((b * 3) & 7);
         for (int dy = 0; dy < h; dy++) {
            int y = 15 - dy;
            int x = bx + (dy > 4 ? ((b & 1) == 0 ? -1 : 1) : 0);
            int v = 128 + dy * 7;
            set(p, x, y, rgb(v, v, v));
            if (fern && (dy & 1) == 1) {
               set(p, x - 1, y, rgb(v - 16, v - 16, v - 16));
               set(p, x + 1, y, rgb(v - 22, v - 22, v - 22));
            }
         }
      }
      return p;
   }

   private static int[] paintDeadBush() {
      int[] p = new int[N * N];
      line(p, 7, 15, 7, 8, c(0x785430));
      line(p, 8, 15, 8, 9, c(0x583C20));
      line(p, 7, 11, 3, 6, c(0x8E663C));
      line(p, 8, 10, 12, 5, c(0x6E4C2A));
      line(p, 5, 8, 2, 4, c(0x9E7446));
      line(p, 10, 7, 13, 3, c(0x8E663C));
      return p;
   }

   private static int[] paintFlower(int mid, int sh, int hi, int center, int style) {
      int[] p = new int[N * N];
      line(p, 7, 15, 7, 7, c(0x448E2C));
      line(p, 8, 15, 8, 8, c(0x2E681C));
      rect(p, 5, 11, 2, 1, c(0x52A436));
      rect(p, 9, 12, 2, 1, c(0x3A7A22));
      if (style >= 5 && style <= 8) {
         // Cup-shaped Tulip blossom
         rect(p, 5, 4, 6, 4, mid);
         line(p, 5, 3, 5, 6, hi);
         line(p, 7, 3, 8, 3, hi);
         line(p, 10, 3, 10, 7, sh);
         rect(p, 6, 7, 4, 1, sh);
      } else {
         // Radiant multi-petal blossom
         rect(p, 5, 4, 6, 4, mid);
         rect(p, 6, 3, 4, 6, mid);
         rect(p, 6, 3, 3, 2, hi);
         rect(p, 7, 8, 3, 1, sh);
         rect(p, 7, 5, 2, 2, center);
      }
      return p;
   }

   private static int[] paintMushroom(boolean red) {
      int[] p = new int[N * N];
      rect(p, 7, 10, 2, 6, c(0xDCD4C2));
      line(p, 8, 10, 8, 15, c(0xB8AE9A));
      int cap = red ? c(0xD82420) : c(0x98704E);
      int capSh = red ? c(0x981412) : c(0x6E4E34);
      int capHi = red ? c(0xF65852) : c(0xB48A66);
      rect(p, 4, 7, 8, 3, cap);
      rect(p, 5, 6, 6, 1, capHi);
      rect(p, 4, 9, 8, 1, capSh);
      if (red) {
         set(p, 5, 7, c(0xFAF6EC));
         set(p, 9, 7, c(0xFAF6EC));
         set(p, 7, 8, c(0xFAF6EC));
      }
      return p;
   }

   private static int[] paintDoublePlant(int kind, boolean top) {
      int[] p = paintGrassTuft(kind == 1);
      if (kind < 2) return p;
      // Add vibrant blossoms on upper/lower stems for Sunflower(2), Lilac(3), Rose Bush(4), Peony(5)
      int c1 = kind == 2 ? c(0xFCEE38) : (kind == 3 ? c(0xC678E8) : (kind == 4 ? c(0xE22020) : c(0xF898D0)));
      int c2 = kind == 2 ? c(0x683C14) : (kind == 3 ? c(0xF2C8FF) : (kind == 4 ? c(0x920E0E) : c(0xFFE0F2)));
      for (int y = 2; y < 14; y++) {
         for (int x = 2; x < 14; x++) {
            if (p[y * N + x] != 0) {
               p[y * N + x] = ((x + y) & 1) == 0 ? c(0x488E2C) : c(0x326A1C);
            }
         }
      }
      if (top && kind == 2) {
         rect(p, 4, 2, 8, 8, c1);
         rect(p, 6, 4, 4, 4, c2);
      } else if (top || kind >= 3) {
         int[][] buds = {{4, 3}, {9, 4}, {6, 7}, {10, 9}, {4, 10}};
         for (int[] b : buds) {
            rect(p, b[0], b[1], 3, 3, c1);
            set(p, b[0], b[1], c2);
         }
      }
      return p;
   }

   private static int[] paintLilyPad() {
      int[] p = new int[N * N];
      for (int y = 2; y <= 13; y++) {
         for (int x = 2; x <= 13; x++) {
            float dx = (x - 7.5F) / 5.8F;
            float dy = (y - 7.5F) / 5.8F;
            if (dx * dx + dy * dy <= 1.0F && !(x >= 8 && y <= 6 && x - 7 > 7 - y)) {
               int col = (x + y) % 3 == 0 ? c(0x3E8E28) : c(0x2E721C);
               set(p, x, y, col);
            }
         }
      }
      return p;
   }

   private static int[] paintVines() {
      int[] p = new int[N * N];
      int[] xs = {2, 6, 10, 13};
      for (int vx : xs) {
         for (int y = 0; y < N; y++) {
            int x = (vx + ((y >> 1) & 1)) & 15;
            set(p, x, y, rgb(156, 156, 156));
            if ((y & 3) == 1) set(p, (x + 1) & 15, y, rgb(132, 132, 132));
         }
      }
      return p;
   }

   private static int[] paintGiantMushRed() {
      int[] p = paintRock(c(0xCC2420), c(0xAA1A18), c(0x881210), c(0xE63C38), 6182);
      rect(p, 2, 2, 3, 3, c(0xF6F2E4));
      rect(p, 10, 4, 3, 2, c(0xF6F2E4));
      rect(p, 5, 10, 3, 3, c(0xF6F2E4));
      return p;
   }

   // ====================================================================
   //  3. Masonry, Ores & Mineral Blocks (100% Brand-New From Scratch)
   // ====================================================================
   private static void paintAllMasonryOresAndMinerals() {
      // Ores (12..17, 488)
      putBlock(Tex.COAL, paintOre(c(0x28282C), c(0x141418), c(0x52525A), 6200), true);
      putBlock(Tex.IRON, paintOre(c(0xDCAFA0), c(0x9E7258), c(0xF8E2D0), 6201), true);
      putBlock(Tex.GOLD, paintOre(c(0xFCEE4B), c(0xC89012), c(0xFFFFC8), 6202), true);
      putBlock(Tex.DIAMOND, paintOre(c(0x5DECF5), c(0x2098A4), c(0xE8FFFF), 6203), true);
      putBlock(Tex.REDSTONE, paintOre(c(0xE82222), c(0x8E0A0A), c(0xFF8888), 6204), true);
      putBlock(Tex.LAPIS, paintOre(c(0x2856DC), c(0x142C86), c(0x88B2FF), 6205), true);
      putBlock(Color312.T_EMERALD_ORE, paintOre(c(0x17DD62), c(0x0A8A38), c(0xBAFFD8), 6206), true);

      // Clay Bricks (20) & Stone Bricks (35, 655..657)
      putBlock(Tex.BRICKS, paintBricks(c(0x9E4A3A), c(0x7E3628), c(0xBA6250), c(0xBCB6AC)), true);
      putBlock(Tex.STONEBRICK, paintStoneBricks(false, false, false), true);
      putBlock(Redstone319.T_CHIS_STONEBRICK, paintStoneBricks(false, false, true), false);
      putBlock(Redstone319.T_MOSSY_STONEBRICK, paintStoneBricks(false, true, false), true);
      putBlock(Redstone319.T_CRACKED_STONEBRICK, paintStoneBricks(true, false, false), true);

      // Sandstone & Red Sandstone (21, 22, 531, 532, 651..654)
      putBlock(Tex.SANDSTONE_SIDE, paintSandstoneSide(false, false), true);
      putBlock(Tex.SANDSTONE_TOP, paintPolishedStone(c(0xE0D49E), c(0xC8BA82), c(0xAC9C66), c(0xF4EAB8), 6210), false);
      putBlock(Redstone319.T_CHIS_SANDSTONE, paintSandstoneSide(false, true), false);
      putBlock(Redstone319.T_SMOOTH_SANDSTONE, paintPolishedStone(c(0xE4D8A4), c(0xCEBE88), c(0xB4A46E), c(0xF8EEBE), 6211), false);

      putBlock(TexArt313.RS_SIDE, paintSandstoneSide(true, false), true);
      putBlock(TexArt313.RS_TOP, paintPolishedStone(c(0xC46424), c(0xA85018), c(0x883C10), c(0xDC7E3A), 6212), false);
      putBlock(Redstone319.T_CHIS_RED_SANDSTONE, paintSandstoneSide(true, true), false);
      putBlock(Redstone319.T_SMOOTH_RED_SANDSTONE, paintPolishedStone(c(0xC86828), c(0xAC541C), c(0x8C4012), c(0xE0823E), 6213), false);

      // Obsidian (34) & Glowstone (30)
      putBlock(Tex.OBSIDIAN, paintObsidian(), true);
      putBlock(Tex.GLOWSTONE, paintGlowstone(), true);

      // Mineral Blocks (482..487)
      putBlock(Color312.T_IRON, paintMetalBlock(c(0xE2E4E8), c(0xC2C6CC), c(0x989EA6), c(0xFFFFFF)), false);
      putBlock(Color312.T_GOLD, paintMetalBlock(c(0xF8D428), c(0xD8A812), c(0xA47608), c(0xFFFFA8)), false);
      putBlock(Color312.T_DIAMOND, paintGemBlock(c(0x5CECE4), c(0x2EB8B0), c(0x18847E), c(0xE4FFFF)), false);
      putBlock(Color312.T_EMERALD, paintGemBlock(c(0x24DC60), c(0x14A844), c(0x0A742C), c(0xB4FFD0)), false);
      putBlock(Color312.T_COAL, paintRock(c(0x242428), c(0x18181C), c(0x0E0E12), c(0x383840), 6220), true);
      putBlock(Color312.T_LAPIS, paintPolishedStone(c(0x244EC8), c(0x18389A), c(0x10246C), c(0x5482EE), 6221), false);
   }

   private static int[] paintOre(int mid, int dk, int spec, int seed) {
      int[] p = paintRock(c(0x828488), c(0x6E7074), c(0x585A5E), c(0x989AA0), 6030);
      int[][] veins = {{3, 3}, {10, 4}, {5, 8}, {11, 10}, {4, 12}};
      for (int i = 0; i < veins.length; i++) {
         int vx = (veins[i][0] + (seed & 1)) & 13;
         int vy = veins[i][1];
         set(p, vx, vy, spec);
         set(p, vx + 1, vy, mid);
         set(p, vx, vy + 1, mid);
         set(p, vx + 1, vy + 1, dk);
         if (vx > 0) p[vy * N + vx - 1] = c(0x525458);
      }
      return p;
   }

   private static int[] paintBricks(int mid, int dk, int hi, int mortar) {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         int row = y >> 2;
         int py = y & 3;
         int off = (row & 1) * 4;
         for (int x = 0; x < N; x++) {
            int px = (x + off) & 7;
            if (py == 3 || px == 7) {
               p[y * N + x] = mortar;
            } else {
               p[y * N + x] = (py == 0 || px == 0) ? hi : (py == 2 ? dk : mid);
            }
         }
      }
      return p;
   }

   private static int[] paintStoneBricks(boolean cracked, boolean mossy, boolean chiseled) {
      int[] p = new int[N * N];
      if (chiseled) {
         for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
               int d = Math.max(Math.abs(x - 7) - (x >= 8 ? 1 : 0), Math.abs(y - 7) - (y >= 8 ? 1 : 0));
               p[y * N + x] = (d == 7 || d == 4 || d == 1) ? c(0x505256) : ((d == 6 || d == 3) ? c(0x9C9EA4) : c(0x7E8084));
            }
         }
         return p;
      }
      for (int y = 0; y < N; y++) {
         int row = y >> 3;
         int py = y & 7;
         int off = row * 8;
         for (int x = 0; x < N; x++) {
            int px = (x + off) & 15;
            if (py == 7 || px == 15) p[y * N + x] = c(0x484A4E);
            else if (py == 0 || px == 0) p[y * N + x] = c(0x9E9FA4);
            else p[y * N + x] = fbm(x, y, 6230) < 0.4F ? c(0x707276) : c(0x828488);
            if (mossy && hash(x, y, 6231) > 0.62F) {
               p[y * N + x] = (py & 1) == 0 ? c(0x6CA23E) : c(0x4C7C2A);
            }
         }
      }
      if (cracked) {
         line(p, 3, 2, 7, 6, c(0x3E4044));
         line(p, 7, 6, 5, 11, c(0x3E4044));
         line(p, 11, 9, 13, 14, c(0x3E4044));
      }
      return p;
   }

   private static int[] paintSandstoneSide(boolean red, boolean chiseled) {
      int[] p = new int[N * N];
      int mid = red ? c(0xC26224) : c(0xDECFA0);
      int dk = red ? c(0x9E4A16) : c(0xC2B27E);
      int hi = red ? c(0xDC7C3A) : c(0xF2E6B8);
      int deep = red ? c(0x7C360C) : c(0x9E8E5C);
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            int col = y == 0 || y == 4 ? hi : (y == 3 || y == 11 || y == 15 ? deep : (y < 4 ? mid : (fbm(x, y, 6240) < 0.45F ? dk : mid)));
            p[y * N + x] = col;
         }
      }
      if (chiseled) {
         rect(p, 4, 5, 8, 8, deep);
         rect(p, 5, 6, 6, 6, hi);
         rect(p, 6, 7, 2, 2, deep);
         rect(p, 9, 7, 2, 2, deep);
         rect(p, 7, 9, 3, 3, deep);
      }
      return p;
   }

   private static int[] paintObsidian() {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float v = fbm(x, y, 6250);
            int col = v < 0.34F ? c(0x100C1C) : (v > 0.70F ? c(0x3C2A56) : c(0x221834));
            if (((x + y * 2) & 7) == 0) col = c(0x523C74);
            p[y * N + x] = col;
         }
      }
      return p;
   }

   private static int[] paintGlowstone() {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float h = hash(x, y, 6255) * 0.5F + fbm(x, y, 6256) * 0.5F;
            p[y * N + x] = h > 0.72F ? c(0xFFFFB8) : (h > 0.48F ? c(0xF8D448) : (h > 0.25F ? c(0xBA7E24) : c(0x744814)));
         }
      }
      return p;
   }

   private static int[] paintMetalBlock(int mid, int dk, int deep, int hi) {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            if (x == 0 || y == 0) p[y * N + x] = hi;
            else if (x == 15 || y == 15) p[y * N + x] = deep;
            else p[y * N + x] = ((x + y * 2) & 3) == 0 ? mix(mid, hi, 0.35F) : ((y & 3) == 3 ? dk : mid);
         }
      }
      return p;
   }

   private static int[] paintGemBlock(int mid, int dk, int deep, int hi) {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            int d = Math.max(Math.abs(x - 7) - (x >= 8 ? 1 : 0), Math.abs(y - 7) - (y >= 8 ? 1 : 0));
            p[y * N + x] = d == 7 ? deep : (d == 6 || d <= 2 ? hi : (x + y < 15 ? mid : dk));
         }
      }
      return p;
   }

   // ====================================================================
   //  4. Workstations, Crops, Doors, Torch, TNT, Cake & Utility Blocks
   // ====================================================================
   private static void paintAllWorkstationsCropsAndUtility() {
      putBlock(Tex.CRAFT_TOP, paintCraftTop(), false);
      putBlock(Tex.CRAFT_SIDE, paintCraftSide(), false);
      putBlock(Tex.FURNACE_FRONT, paintFurnaceFront(false), false);
      putBlock(Tex.FURNACE_ON, paintFurnaceFront(true), false);
      putBlock(Tex.CHEST_TOP, paintChestTop(), false);
      putBlock(Tex.CHEST_SIDE, paintChestSide(), false);
      Tex.blit(43, paintSpawner());
      putSprite(Tex.TORCH, paintTorch(0));

      // Oak Door (160 lower, 161 upper) & Ladder (179)
      putBlock(160, paintOakDoor(false), false);
      putBlock(161, paintOakDoor(true), false);
      putSprite(179, paintLadder());

      // Farmland (170 dry, 203 wet), Hay Bale (212, 213), Bookshelf (220), TNT (180..182)
      putBlock(Tex.FARM_TOP, paintFarmland(false), true);
      putBlock(203, paintFarmland(true), true);
      putBlock(212, paintHaySide(), true);
      putBlock(213, paintHayTop(), true);
      putBlock(220, paintBookshelf(), false);
      putBlock(Tex.TNT_SIDE, paintTntSide(), false);
      putBlock(Tex.TNT_TOP, paintTntCap(true), false);
      putBlock(Tex.TNT_BOTTOM, paintTntCap(false), false);

      // Pumpkin, Jack o'Lantern & Melon (221..226)
      putBlock(221, paintPumpkinTop(), false);
      putBlock(222, paintPumpkinSide(), true);
      putBlock(223, paintPumpkinFace(false), false);
      putBlock(224, paintPumpkinFace(true), false);
      putBlock(225, paintMelonTop(), false);
      putBlock(226, paintMelonSide(), true);

      // Crops: Wheat 0..7 (171..178), Carrots 0..3 (204..207), Potatoes 0..3 (208..211), Stems (227..230)
      for (int s = 0; s < 8; s++) {
         putSprite(171 + s, paintWheatStage(s));
      }
      for (int s = 0; s < 4; s++) {
         putSprite(204 + s, paintRootCropStage(s, true));
         putSprite(208 + s, paintRootCropStage(s, false));
         putSprite(227 + s, paintStemStage(s));
      }

      // Cake (231..234)
      putBlock(231, paintCakeTop(), false);
      putBlock(232, paintCakeSide(false), false);
      putBlock(233, paintPolishedStone(c(0xD89844), c(0xB87828), c(0x945C18), c(0xF0B462), 6340), false);
      putBlock(234, paintCakeSide(true), false);
   }

   private static int[] paintCraftTop() {
      int[] p = paintPlanks(c(0x9E7644), c(0x7E5A30), c(0x5A3E1E), c(0xBA925C), 6300);
      rect(p, 2, 2, 12, 12, c(0x52361A));
      for (int gy = 0; gy < 3; gy++) {
         for (int gx = 0; gx < 3; gx++) {
            rect(p, 3 + gx * 4, 3 + gy * 4, 3, 3, (gx + gy & 1) == 0 ? c(0xB88C54) : c(0x9E723E));
         }
      }
      return p;
   }

   private static int[] paintCraftSide() {
      int[] p = paintPlanks(c(0xAC8852), c(0x8E6C3C), c(0x6E5028), c(0xC6A268), 6301);
      rect(p, 0, 0, 16, 4, c(0x784E28));
      rect(p, 3, 5, 4, 7, c(0x4E3218));
      line(p, 10, 5, 13, 11, c(0xD8DCE4)); // steel saw
      rect(p, 9, 5, 2, 2, c(0x9E4224));     // saw handle
      return p;
   }

   private static int[] paintFurnaceFront(boolean lit) {
      int[] p = paintCobble(false);
      rect(p, 3, 2, 10, 5, c(0x202226));
      rect(p, 3, 9, 10, 5, lit ? c(0x58240E) : c(0x181A1E));
      if (lit) {
         rect(p, 4, 10, 8, 3, c(0xFF7818));
         rect(p, 5, 11, 6, 2, c(0xFFE458));
      }
      return p;
   }

   private static int[] paintChestTop() {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            boolean rim = x <= 1 || x >= 14 || y <= 1 || y >= 14;
            p[y * N + x] = rim ? c(0x4A3218) : ((x & 3) == 0 ? c(0x8E622E) : c(0xAC7C3C));
         }
      }
      return p;
   }

   private static int[] paintChestSide() {
      int[] p = paintChestTop();
      rect(p, 0, 5, 16, 2, c(0x382410));
      rect(p, 6, 4, 4, 4, c(0xF6D038));
      rect(p, 7, 5, 2, 2, c(0xB88814));
      return p;
   }

   private static int[] paintSpawner() {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            boolean bar = x <= 1 || x >= 14 || y <= 1 || y >= 14 || x == 4 || x == 11 || y == 4 || y == 11 || Math.abs(x - y) <= 1;
            if (bar) {
               p[y * N + x] = (x == 0 || y == 0) ? c(0x54687E) : c(0x2E3A48);
            }
         }
      }
      rect(p, 7, 7, 2, 2, c(0xFF8824));
      return p;
   }

   private static int[] paintTorch(int kind) {
      int[] p = new int[N * N];
      rect(p, 7, 6, 2, 10, c(0x98723E));
      line(p, 8, 6, 8, 15, c(0x684A24));
      if (kind == 0) {
         rect(p, 6, 3, 4, 3, c(0xFF9420));
         rect(p, 7, 2, 2, 3, c(0xFFFF98));
      } else if (kind == 1) {
         rect(p, 6, 3, 4, 3, c(0xE81E1E));
         rect(p, 7, 4, 2, 2, c(0xFF9878));
      } else {
         rect(p, 6, 4, 4, 3, c(0x4E1818));
      }
      return p;
   }

   private static int[] paintOakDoor(boolean upper) {
      int[] p = paintPlanks(c(0xAC824A), c(0x8C6634), c(0x5E421E), c(0xC89E62), 6310);
      if (upper) {
         for (int y = 3; y <= 11; y++) {
            for (int x = 3; x <= 12; x++) {
               if (x != 7 && x != 8 && y != 7) p[y * N + x] = 0;
            }
         }
      } else {
         rect(p, 3, 3, 10, 10, c(0x7E582A));
         rect(p, 12, 1, 2, 2, c(0xE8C848));
      }
      return p;
   }

   private static int[] paintLadder() {
      int[] p = new int[N * N];
      rect(p, 2, 0, 2, 16, c(0x9E7642));
      rect(p, 12, 0, 2, 16, c(0x866234));
      for (int ry : new int[]{2, 6, 10, 14}) {
         rect(p, 2, ry, 12, 2, c(0xB48A52));
         line(p, 2, ry + 1, 13, ry + 1, c(0x725228));
      }
      return p;
   }

   private static int[] paintFarmland(boolean wet) {
      int[] p = new int[N * N];
      int ridge = wet ? c(0x52341C) : c(0x8C623E);
      int furrow = wet ? c(0x321E0E) : c(0x644428);
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            p[y * N + x] = (x & 3) == 0 ? furrow : (hash(x, y, 6315) > 0.65F ? mul(ridge, 1.12F) : ridge);
         }
      }
      return p;
   }

   private static int[] paintHaySide() {
      int[] p = paintBark(c(0xD8B434), c(0xBA9422), c(0x947214), c(0xF0CE4E), false, 6318);
      rect(p, 0, 4, 16, 2, c(0xB83224));
      rect(p, 0, 11, 16, 2, c(0xB83224));
      return p;
   }

   private static int[] paintHayTop() {
      return paintBark(c(0xD8B434), c(0xBA9422), c(0x947214), c(0xF0CE4E), true, 6319);
   }

   private static int[] paintBookshelf() {
      int[] p = paintPlanks(c(0xAC8852), c(0x8E6C3C), c(0x6E5028), c(0xC6A268), 6320);
      int[] bookCols = {c(0xB82824), c(0x2868B8), c(0x2E8E38), c(0xC89824), c(0x78329E)};
      for (int shelf : new int[]{3, 10}) {
         rect(p, 1, shelf, 14, 5, c(0x2A1C10));
         for (int bx = 2; bx < 14; bx += 2) {
            int bc = bookCols[(bx + shelf) % bookCols.length];
            rect(p, bx, shelf + 1, 2, 4, bc);
            set(p, bx, shelf + 2, c(0xF2DC78));
         }
      }
      return p;
   }

   private static int[] paintTntSide() {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            int stick = x >> 2;
            p[y * N + x] = (x & 3) == 3 ? c(0x8E1814) : ((stick & 1) == 0 ? c(0xD82E26) : c(0xC4241E));
         }
      }
      rect(p, 0, 5, 16, 6, c(0xEFECE4));
      // "TNT" lettering
      int blk = c(0x1C1C20);
      rect(p, 2, 6, 3, 1, blk); rect(p, 3, 6, 1, 4, blk);
      rect(p, 6, 6, 1, 4, blk); set(p, 7, 7, blk); set(p, 8, 8, blk); rect(p, 9, 6, 1, 4, blk);
      rect(p, 11, 6, 3, 1, blk); rect(p, 12, 6, 1, 4, blk);
      return p;
   }

   private static int[] paintTntCap(boolean top) {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            p[y * N + x] = ((x & 3) == 0 || (y & 3) == 0) ? c(0x881612) : c(0xCC2820);
         }
      }
      if (top) {
         rect(p, 6, 6, 4, 4, c(0x28282C));
         rect(p, 7, 7, 2, 2, c(0xECE8DC));
      }
      return p;
   }

   private static int[] paintPumpkinTop() {
      int[] p = paintCactusTop();
      for (int i = 0; i < N * N; i++) {
         int lum = (p[i] >> 8) & 255;
         p[i] = lum > 140 ? c(0xEA8824) : (lum > 100 ? c(0xCC6E16) : c(0x9E4E0C));
      }
      rect(p, 6, 6, 4, 4, c(0x6E5828));
      return p;
   }

   private static int[] paintPumpkinSide() {
      return paintBark(c(0xE07A1C), c(0xBA5E10), c(0x8E4208), c(0xF49632), false, 6330);
   }

   private static int[] paintPumpkinFace(boolean lit) {
      int[] p = paintPumpkinSide();
      int eye = lit ? c(0xFFF068) : c(0x281406);
      rect(p, 3, 4, 3, 3, eye);
      rect(p, 10, 4, 3, 3, eye);
      rect(p, 3, 10, 10, 3, eye);
      set(p, 5, 10, c(0xE07A1C));
      set(p, 10, 10, c(0xE07A1C));
      return p;
   }

   private static int[] paintMelonSide() {
      return paintBark(c(0x6EA428), c(0x4C7818), c(0x345610), c(0x98C842), false, 6335);
   }

   private static int[] paintMelonTop() {
      int[] p = paintMelonSide();
      rect(p, 6, 6, 4, 4, c(0x98B852));
      return p;
   }

   private static int[] paintWheatStage(int stage) {
      int[] p = new int[N * N];
      int h = 3 + stage * 2;
      if (h > 15) h = 15;
      int col = stage >= 6 ? c(0xDCA828) : (stage >= 3 ? c(0x8EBA32) : c(0x52A828));
      int tip = stage >= 6 ? c(0xF6D458) : c(0xA8DC48);
      for (int sx : new int[]{2, 5, 8, 11, 13}) {
         for (int dy = 0; dy < h; dy++) {
            set(p, sx, 15 - dy, dy >= h - 3 ? tip : col);
         }
      }
      return p;
   }

   private static int[] paintRootCropStage(int stage, boolean carrot) {
      int[] p = paintWheatStage(stage + 1);
      if (stage == 3) {
         int root = carrot ? c(0xF27818) : c(0xD8AE58);
         for (int sx : new int[]{2, 5, 8, 11, 13}) {
            rect(p, sx, 13, 2, 3, root);
         }
      }
      return p;
   }

   private static int[] paintStemStage(int stage) {
      int[] p = new int[N * N];
      int h = 5 + stage * 3;
      line(p, 7, 15, 7, Math.max(2, 15 - h), c(0x6EB42E));
      line(p, 8, 15, 12, 9, c(0x529220));
      return p;
   }

   private static int[] paintCakeTop() {
      int[] p = new int[N * N];
      rect(p, 1, 1, 14, 14, c(0xF8F4EC));
      int[][] berries = {{4, 4}, {10, 4}, {7, 7}, {4, 10}, {10, 10}};
      for (int[] b : berries) {
         rect(p, b[0], b[1], 2, 2, c(0xD82020));
      }
      return p;
   }

   private static int[] paintCakeSide(boolean inner) {
      int[] p = new int[N * N];
      rect(p, 1, 8, 14, 3, c(0xF8F4EC));
      rect(p, 1, 11, 14, 5, inner ? c(0xE8B458) : c(0xC88432));
      if (inner) rect(p, 1, 12, 14, 1, c(0xD82824));
      return p;
   }

   // ====================================================================
   //  5. Nether, End, Ocean & Purpur Blocks (100% Brand-New From Scratch)
   // ====================================================================
   private static void paintAllNetherAndEndBlocks() {
      putBlock(TexArt312.NETHERRACK, paintRock(c(0x722A2A), c(0x581E1E), c(0x3E1212), c(0x8E3A3A), 6400), true);
      putBlock(TexArt312.SOUL_SAND, paintSoulSand(), true);
      putBlock(TexArt312.QUARTZ_ORE, paintQuartzOre(), true);
      putBlock(TexArt312.NETHER_BRICKS, paintBricks(c(0x381C22), c(0x281216), c(0x4C2830), c(0x1C0C10)), true);
      putBlock(TexArt312.MAGMA, paintMagma(), true);
      putBlock(TexArt312.QUARTZ_SIDE, paintPolishedStone(c(0xECE8DE), c(0xD8D2C6), c(0xBEB8AA), c(0xFAF8F2), 6405), false);
      putBlock(TexArt312.QUARTZ_TOP, paintPolishedStone(c(0xF0ECE2), c(0xDCD6CA), c(0xC2BCAE), c(0xFFFFFF), 6406), false);

      putBlock(TexArt312.END_STONE, paintRock(c(0xE0E2A4), c(0xC6C888), c(0xA8AA6E), c(0xF4F6BE), 6410), true);
      putBlock(TexArt312.FRAME_TOP, paintEndFrameTop(false), false);
      putBlock(TexArt312.FRAME_SIDE, paintEndFrameSide(), false);
      putBlock(TexArt312.FRAME_TOP_EYE, paintEndFrameTop(true), false);
      putBlock(TexArt312.END_PORTAL, paintEndPortal(), true);
      putBlock(TexArt312.DRAGON_EGG, paintDragonEgg(), true);

      // Prismarine (680..682), Purpur (683..685), Bone Block (686..687), Sea Lantern (659), Slime Block (658)
      putBlock(Redstone319.T_PRISMARINE, paintCobblePrismarine(), true);
      putBlock(Redstone319.T_PRISMARINE_BRICKS, paintBricks(c(0x529E8E), c(0x3C7E70), c(0x6EBAAC), c(0x2C6256)), true);
      putBlock(Redstone319.T_DARK_PRISMARINE, paintBricks(c(0x325E52), c(0x24463C), c(0x44786A), c(0x18322A)), true);
      putBlock(Redstone319.T_PURPUR_BLOCK, paintBricks(c(0xAA7CAA), c(0x8E628E), c(0xC296C2), c(0x744C74)), true);
      putBlock(Redstone319.T_PURPUR_TOP, paintPolishedStone(c(0xB284B2), c(0x946894), c(0x785078), c(0xCA9ECA), 6420), false);
      putBlock(Redstone319.T_PURPUR_PILLAR, paintBark(c(0xAA7CAA), c(0x8E628E), c(0x744C74), c(0xC296C2), false, 6421), true);
      putBlock(Redstone319.T_BONE_TOP, paintLogRings(c(0xC4BEAA), c(0xE6E2D2), c(0xD4CEBC)), false);
      putBlock(Redstone319.T_BONE_SIDE, paintBark(c(0xE2DEC8), c(0xCCC6B0), c(0xB4AE98), c(0xF2EFE0), false, 6422), true);
      putBlock(Redstone319.T_SEA_LANTERN, paintSeaLantern(), false);
      putBlock(Redstone319.T_SLIME, paintSlime(true), false);
   }

   private static int[] paintSoulSand() {
      int[] p = paintSoil(c(0x564236), c(0x423026), c(0x2E2018), c(0x6E5646), false, 6430);
      rect(p, 3, 4, 2, 2, c(0x261A12));
      rect(p, 9, 4, 2, 2, c(0x261A12));
      rect(p, 5, 8, 4, 3, c(0x261A12));
      return p;
   }

   private static int[] paintQuartzOre() {
      int[] p = paintRock(c(0x722A2A), c(0x581E1E), c(0x3E1212), c(0x8E3A3A), 6400);
      int[][] shards = {{2, 3, 4, 2}, {9, 4, 4, 2}, {4, 9, 5, 2}, {10, 11, 4, 2}};
      for (int[] s : shards) {
         rect(p, s[0], s[1], s[2], s[3], c(0xEAE4D8));
         set(p, s[0], s[1], c(0xFFFFFF));
      }
      return p;
   }

   private static int[] paintMagma() {
      int[] p = paintRock(c(0x46221C), c(0x321612), c(0x220E0C), c(0x5E2E24), 6435);
      line(p, 1, 3, 14, 6, c(0xFF7818));
      line(p, 2, 4, 13, 7, c(0xFFE048));
      line(p, 3, 11, 13, 13, c(0xFF8820));
      return p;
   }

   private static int[] paintEndFrameTop(boolean eye) {
      int[] p = paintPolishedStone(c(0x587868), c(0x3E5A4C), c(0x2A4036), c(0x789A88), 6440);
      rect(p, 4, 4, 8, 8, c(0x1C2822));
      if (eye) {
         rect(p, 5, 5, 6, 6, c(0x4CC868));
         rect(p, 7, 6, 2, 4, c(0x142218));
      }
      return p;
   }

   private static int[] paintEndFrameSide() {
      int[] p = paintRock(c(0xE0E2A4), c(0xC6C888), c(0xA8AA6E), c(0xF4F6BE), 6410);
      rect(p, 0, 0, 16, 5, c(0x486A5C));
      rect(p, 0, 0, 16, 1, c(0x6E9282));
      return p;
   }

   private static int[] paintEndPortal() {
      int[] p = new int[N * N];
      for (int i = 0; i < N * N; i++) {
         float h = hash(i & 15, i >> 4, 6445);
         p[i] = h > 0.92F ? c(0x98FFEE) : (h > 0.80F ? c(0x387898) : c(0x0A101C));
      }
      return p;
   }

   private static int[] paintDragonEgg() {
      int[] p = paintRock(c(0x181224), c(0x0E0A16), c(0x06040A), c(0x2E2044), 6448);
      int[][] spots = {{4, 3}, {10, 5}, {6, 9}, {11, 12}};
      for (int[] s : spots) {
         rect(p, s[0], s[1], 2, 2, c(0x9E44D8));
      }
      return p;
   }

   private static int[] paintCobblePrismarine() {
      int[] p = paintCobble(false);
      for (int i = 0; i < N * N; i++) {
         int lum = (p[i] >> 8) & 255;
         p[i] = lum > 140 ? c(0x78C8B8) : (lum > 105 ? c(0x549E90) : c(0x366E64));
      }
      return p;
   }

   private static int[] paintSeaLantern() {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            int d = Math.max(Math.abs(x - 7) - (x >= 8 ? 1 : 0), Math.abs(y - 7) - (y >= 8 ? 1 : 0));
            p[y * N + x] = d >= 6 ? c(0x528E86) : (d >= 4 ? c(0x98DCD2) : c(0xEEFFFC));
         }
      }
      return p;
   }

   private static int[] paintSlime(boolean inner) {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            boolean core = inner && x >= 3 && x <= 12 && y >= 3 && y <= 12;
            p[y * N + x] = core ? c(0x52A83C) : ((x == 0 || y == 0) ? c(0x98E878) : c(0x74C858));
         }
      }
      return p;
   }

   // ====================================================================
   //  6. All 16 Colors: Wool, Concrete, Powder, Terracotta, Glazed & Glass
   // ====================================================================
   static final int[] PAL16 = {
      0xECEFF2, 0xE8761C, 0xBE44B8, 0x3A9AD8,
      0xF4C824, 0x72BC1C, 0xE87AA0, 0x42464A,
      0x969A98, 0x168694, 0x7A28AC, 0x2C389C,
      0x6E4424, 0x4A621C, 0xAC2420, 0x1A1C20
   };

   private static void paintAllColorsGlassAndTerracotta() {
      Tex.blit(Tex.GLASS, paintGlass(c(0xD8F2FA), c(0x8EB8C8), false));
      putBlock(601, paintPaneEdge(c(0xC8E8F4)), false);
      putBlock(Color312.T_TERRACOTTA, paintTerracotta(c(0x9A5E44)), true);

      for (int i = 0; i < 16; i++) {
         int base = c(PAL16[i]);
         putBlock(48 + i, paintWool(base, i), true);
         putBlock(Color312.T_CONCRETE0 + i, paintConcrete(base, i), true);
         putBlock(Color312.T_POWDER0 + i, paintPowder(base, i), true);
         putBlock(Color312.T_STAINED0 + i, paintTerracotta(mix(base, c(0x925A42), 0.34F)), true);
         putBlock(Color312.T_GLAZED0 + i, paintGlazedTerracotta(base, i), false);
         Tex.blit(Color312.T_GLASS0 + i, paintGlass(base, mul(base, 0.72F), true));
         putBlock(602 + i, paintPaneEdge(base), false);
      }
   }

   private static int[] paintWool(int base, int seed) {
      int[] p = new int[N * N];
      int hi = mul(base, 1.14F);
      int dk = mul(base, 0.84F);
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            boolean weave = ((x >> 1) + (y >> 1) & 1) == 0;
            float n = hash(x, y, 6500 + seed);
            p[y * N + x] = n > 0.78F ? hi : (n < 0.22F ? dk : (weave ? mul(base, 1.05F) : mul(base, 0.94F)));
         }
      }
      return p;
   }

   private static int[] paintConcrete(int base, int seed) {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float n = (hash(x, y, 6520 + seed) - 0.5F) * 0.08F;
            p[y * N + x] = mul(base, 1.0F + n);
         }
      }
      return p;
   }

   private static int[] paintPowder(int base, int seed) {
      return paintSand(base, mul(base, 0.86F), mul(base, 0.74F), mul(base, 1.15F), 6540 + seed);
   }

   private static int[] paintTerracotta(int base) {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float s = (float)Math.sin((y + (x >> 2)) * 0.75);
            p[y * N + x] = s > 0.35F ? mul(base, 1.09F) : (s < -0.35F ? mul(base, 0.88F) : base);
         }
      }
      return p;
   }

   private static int[] paintGlazedTerracotta(int base, int idx) {
      int[] p = new int[N * N];
      int hi = mul(base, 1.26F);
      int dk = mul(base, 0.68F);
      int acc = c(PAL16[(idx + 4) & 15]);
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            int d = Math.max(Math.abs(x - 7) - (x >= 8 ? 1 : 0), Math.abs(y - 7) - (y >= 8 ? 1 : 0));
            boolean diag = Math.abs(x - y) <= 1 || Math.abs(x + y - 15) <= 1;
            p[y * N + x] = d == 7 ? dk : (d == 5 ? hi : (d == 2 ? acc : (diag ? hi : base)));
         }
      }
      return p;
   }

   private static int[] paintGlass(int rim, int sh, boolean stained) {
      int[] p = new int[N * N];
      int fill = stained ? ((0x68 << 24) | (rim & 0xFFFFFF)) : 0;
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            if (x == 0 || y == 0) p[y * N + x] = mul(rim, 1.16F);
            else if (x == 15 || y == 15) p[y * N + x] = sh;
            else if ((x + y == 5 && x >= 2 && x <= 4) || (x + y == 8 && x >= 3 && x <= 6)) p[y * N + x] = c(0xFFFFFF);
            else p[y * N + x] = fill;
         }
      }
      return p;
   }

   private static int[] paintPaneEdge(int col) {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 6; x <= 9; x++) {
            p[y * N + x] = (x == 6 || x == 9) ? mul(col, 0.78F) : mul(col, 1.12F);
         }
      }
      return p;
   }

   // ====================================================================
   //  7. Bars, Trapdoors, Beds, Banners & All Redstone Mechanisms
   // ====================================================================
   private static void paintAllShapesBedsBannersAndRedstone() {
      putSprite(TexArt315.BARS, paintIronBars());
      putBlock(TexArt315.BARS_EDGE, paintPaneEdge(c(0x98A0AA)), false);

      int[] trapCols = {0xAC8852, 0xCEBC82, 0x785834, 0xA87854, 0xBE683A, 0x483018, 0xD4D8E0};
      for (int k = 0; k < 7; k++) {
         putSprite(TexArt315.TRAP0 + k, paintTrapdoor(c(trapCols[k]), k == 6));
      }

      putBlock(183, paintBedTop(c(0xCC2420)), false);
      putBlock(184, paintBedSide(c(0xCC2420)), false);
      putBlock(185, paintBedEnd(c(0xCC2420)), false);
      putBlock(TexArt315.BED_SIDE, paintBedSide(c(0xDADCE0)), false);
      for (int f = 0; f < 4; f++) {
         putBlock(TexArt315.BED_FOOT0 + f, paintBedTop(c(0xDADCE0)), false);
         putBlock(TexArt315.BED_HEAD0 + f, paintBedHeadTop(c(0xDADCE0)), false);
      }
      putBlock(TexArt315.CLOTH, paintWool(c(0xE4E6EA), 70), true);

      putBlock(Redstone319.T_REDSTONE_BLOCK, paintGemBlock(c(0xDC1C18), c(0xA4100C), c(0x6E0806), c(0xFF6858)), false);
      putBlock(Redstone319.T_LAMP_OFF, paintLamp(false), false);
      putBlock(Redstone319.T_LAMP_ON, paintLamp(true), false);
      putSprite(Redstone319.T_REDSTONE_TORCH, paintTorch(1));
      putSprite(Redstone319.T_REDSTONE_TORCH_OFF, paintTorch(2));
      putSprite(Redstone319.T_REDSTONE_WIRE, paintRedstoneWire(false));
      putSprite(Redstone319.T_REDSTONE_WIRE_LIT, paintRedstoneWire(true));
      putBlock(Redstone319.T_REPEATER_OFF, paintRepeater(false), false);
      putBlock(Redstone319.T_REPEATER_ON, paintRepeater(true), false);
      putBlock(Redstone319.T_PISTON_SIDE, paintPistonSide(), false);
      putBlock(Redstone319.T_PISTON_TOP, paintPistonTop(false), false);
      putBlock(Redstone319.T_PISTON_TOP_STICKY, paintPistonTop(true), false);
      putBlock(Redstone319.T_PISTON_BOTTOM, paintCobble(false), true);
      putBlock(Redstone319.T_DAYLIGHT_TOP, paintDaylightTop(), false);
      putBlock(Redstone319.T_NOTE_BLOCK, paintNoteBlock(), false);
      putBlock(Redstone319.T_OBSERVER_FRONT, paintObserverFront(), false);
   }

   private static int[] paintIronBars() {
      int[] p = new int[N * N];
      for (int bx : new int[]{2, 7, 8, 13}) {
         rect(p, bx, 0, 1, 16, bx == 2 || bx == 7 ? c(0xB8C0CA) : c(0x78808A));
      }
      for (int by : new int[]{3, 12}) {
         rect(p, 0, by, 16, 2, c(0x98A0AA));
      }
      return p;
   }

   private static int[] paintTrapdoor(int col, boolean iron) {
      int[] p = new int[N * N];
      int dk = mul(col, 0.76F);
      int hi = mul(col, 1.14F);
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            boolean hole = (x >= 3 && x <= 6 || x >= 9 && x <= 12) && (y >= 3 && y <= 6 || y >= 9 && y <= 12);
            if (!hole) {
               p[y * N + x] = (x == 0 || y == 0) ? hi : ((x == 15 || y == 15) ? dk : col);
            }
         }
      }
      if (!iron) rect(p, 7, 1, 2, 2, c(0x686E76));
      return p;
   }

   private static int[] paintBedTop(int quilt) {
      int[] p = paintWool(quilt, 80);
      rect(p, 0, 0, 16, 2, mul(quilt, 1.15F));
      return p;
   }

   private static int[] paintBedHeadTop(int quilt) {
      int[] p = paintWool(quilt, 81);
      rect(p, 1, 2, 14, 6, c(0xF6F4EC));
      rect(p, 2, 3, 12, 4, c(0xFFFFFF));
      return p;
   }

   private static int[] paintBedSide(int quilt) {
      int[] p = new int[N * N];
      rect(p, 0, 5, 16, 5, quilt);
      rect(p, 0, 10, 16, 4, c(0x9E7642));
      return p;
   }

   private static int[] paintBedEnd(int quilt) {
      return paintBedSide(quilt);
   }

   private static int[] paintCrackStage(int stage) {
      int[] p = new int[N * N];
      int len = 3 + stage * 2;
      int dark = 0xB0202024;
      line(p, 8, 8, Math.max(1, 8 - len), Math.max(1, 8 - len), dark);
      line(p, 8, 8, Math.min(14, 8 + len), Math.max(1, 8 - len + 1), dark);
      line(p, 8, 8, Math.max(1, 8 - len + 1), Math.min(14, 8 + len), dark);
      line(p, 8, 8, Math.min(14, 8 + len), Math.min(14, 8 + len - 1), dark);
      return p;
   }

   private static int[] paintLamp(boolean lit) {
      int[] p = new int[N * N];
      int core = lit ? c(0xFFF498) : c(0x6E4826);
      int mid = lit ? c(0xF6B838) : c(0x4E321A);
      int frame = c(0x2C2018);
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            boolean border = x <= 1 || x >= 14 || y <= 1 || y >= 14 || Math.abs(x - y) == 7;
            p[y * N + x] = border ? frame : ((x >= 5 && x <= 10 && y >= 5 && y <= 10) ? core : mid);
         }
      }
      return p;
   }

   private static int[] paintRedstoneWire(boolean lit) {
      int[] p = new int[N * N];
      int main = lit ? c(0xFF2820) : c(0x6E1010);
      int core = lit ? c(0xFF9878) : c(0x921818);
      rect(p, 0, 7, 16, 2, main);
      rect(p, 7, 0, 2, 16, main);
      rect(p, 6, 6, 4, 4, core);
      return p;
   }

   private static int[] paintRepeater(boolean lit) {
      int[] p = paintPolishedStone(c(0x989A9E), c(0x7C7E82), c(0x5E6064), c(0xB4B6BA), 6620);
      rect(p, 7, 2, 2, 12, lit ? c(0xFF2820) : c(0x681212));
      rect(p, 6, 3, 4, 3, lit ? c(0xFF8868) : c(0x8E1A1A));
      rect(p, 6, 10, 4, 3, lit ? c(0xFF8868) : c(0x8E1A1A));
      return p;
   }

   private static int[] paintPistonSide() {
      int[] p = paintCobble(false);
      rect(p, 0, 0, 16, 4, c(0xAC844C));
      rect(p, 0, 4, 16, 1, c(0x584224));
      return p;
   }

   private static int[] paintPistonTop(boolean sticky) {
      int[] p = paintPlanks(c(0xB48C54), c(0x946E3C), c(0x6E5028), c(0xCCA46A), 6625);
      rect(p, 5, 5, 6, 6, c(0x888E96));
      if (sticky) {
         rect(p, 4, 4, 8, 8, c(0x74C848));
         rect(p, 6, 6, 4, 4, c(0x9CE86C));
      }
      return p;
   }

   private static int[] paintDaylightTop() {
      int[] p = paintPlanks(c(0x725230), c(0x583E22), c(0x3E2A16), c(0x8C6840), 6630);
      for (int cy = 0; cy < 3; cy++) {
         for (int cx = 0; cx < 3; cx++) {
            rect(p, 2 + cx * 4, 2 + cy * 4, 3, 3, c(0xDCE4EC));
            set(p, 2 + cx * 4, 2 + cy * 4, c(0xFFFFFF));
         }
      }
      return p;
   }

   private static int[] paintDaylightSide() {
      int[] p = paintPlanks(c(0x725230), c(0x583E22), c(0x3E2A16), c(0x8C6840), 6631);
      rect(p, 0, 0, 16, 4, c(0xD8E0E8));
      return p;
   }

   private static int[] paintNoteBlock() {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            boolean rim = x == 0 || x == 15 || y == 0 || y == 15;
            p[y * N + x] = rim ? c(0x462C18) : (((x + y) & 1) == 0 ? c(0x7E522E) : c(0x643E20));
         }
      }
      rect(p, 6, 6, 4, 4, c(0x362010));
      return p;
   }

   private static int[] paintObserverFront() {
      int[] p = paintCobble(false);
      rect(p, 3, 5, 4, 3, c(0x222428));
      rect(p, 9, 5, 4, 3, c(0x222428));
      set(p, 4, 6, c(0xECEEF2));
      set(p, 10, 6, c(0xECEEF2));
      rect(p, 6, 10, 4, 2, c(0x222428));
      return p;
   }

   private static int[] paintObserverBack(boolean on) {
      int[] p = paintCobble(false);
      rect(p, 5, 5, 6, 6, c(0x383A40));
      rect(p, 6, 6, 4, 4, on ? c(0xFF3224) : c(0x781414));
      return p;
   }

   private static int[] paintObserverTop() {
      int[] p = paintPolishedStone(c(0x7C7E82), c(0x64666A), c(0x4A4C50), c(0x989AA0), 6635);
      rect(p, 7, 3, 2, 10, c(0x424448));
      line(p, 5, 5, 7, 3, c(0x424448));
      line(p, 10, 5, 8, 3, c(0x424448));
      return p;
   }

   private static int[] paintObserverSide() {
      int[] p = paintPolishedStone(c(0x7C7E82), c(0x64666A), c(0x4A4C50), c(0x989AA0), 6636);
      rect(p, 0, 7, 16, 2, c(0x424448));
      return p;
   }

   // ====================================================================
   //  8. Animated Fluid, Fire & Nether Portal Frames (16 Frames Each)
   // ====================================================================
   private static void paintAllAnimatedFrames() {
      for (int f = 0; f < 16; f++) {
         int[] w = new int[N * N];
         int[] l = new int[N * N];
         int[] fire = new int[N * N];
         int[] portal = new int[N * N];
         for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
               float wy = (y + f) & 15;
               float wWave = (float)(Math.sin((x + wy) * Math.PI / 8.0) + Math.cos((x - wy * 0.5F) * Math.PI / 4.0));
               int wc = wWave > 0.85F ? c(0x5CA8FF) : (wWave > 0.15F ? c(0x2E74E6) : (wWave > -0.55F ? c(0x1E58C8) : c(0x1440A4)));
               w[y * N + x] = (0xD8 << 24) | (wc & 0xFFFFFF);

               float ly = (y + (f >> 1)) & 15;
               float lWave = (float)(Math.sin((x + ly) * Math.PI / 8.0) + Math.sin((y - f * 0.5F) * Math.PI / 4.0));
               l[y * N + x] = lWave > 0.80F ? c(0xFFEE58) : (lWave > 0.10F ? c(0xFF8218) : (lWave > -0.55F ? c(0xD83E0C) : c(0x8E1E06)));

               float fh = 15 - y + (hash(x, (y + f) & 15, 6700) - 0.5F) * 5.0F;
               if (fh < 12.5F && x >= 1 && x <= 14) {
                  fire[y * N + x] = fh < 4.5F ? c(0xFFF688) : (fh < 8.5F ? c(0xFF8C1A) : c(0xDC2E10));
               }

               float ang = (float)(Math.atan2(y - 7.5, x - 7.5) + f * Math.PI / 8.0);
               float rad = (float)Math.hypot(x - 7.5, y - 7.5);
               float sw = (float)Math.sin(ang * 2.0 + rad * 0.65);
               int pc = sw > 0.45F ? c(0xE288FF) : (sw > -0.25F ? c(0x9E2CE6) : c(0x581098));
               portal[y * N + x] = (0xC8 << 24) | (pc & 0xFFFFFF);
            }
         }
         Tex.waterFrames[f] = w;
         Tex.lavaFrames[f] = l;
         if (TexArt37.fireFrames != null && f < TexArt37.fireFrames.length) {
            TexArt37.fireFrames[f] = fire;
         }
         if (TexArt312.portalFrames != null && f < TexArt312.portalFrames.length) {
            TexArt312.portalFrames[f] = portal;
         }
      }
      Tex.blit(23, Tex.waterFrames[0]);
      Tex.blit(24, Tex.lavaFrames[0]);
      Tex.blit(235, TexArt37.fireFrames[0]);
      Tex.blit(TexArt312.PORTAL, TexArt312.portalFrames[0]);
   }

   // ====================================================================
   //  9. All 25 Tools & Weapons, All 16 Armor Pieces & Fixed Recurve Bow
   // ====================================================================
   private static final int[][] TIER_PAL = {
      {0x644826, 0x8F6A3A, 0xAE844C, 0xC69C62},
      {0x5A5C60, 0x808286, 0x9A9CA0, 0xB4B6BC},
      {0x889098, 0xC8CED6, 0xE4E8EE, 0xFFFFFF},
      {0xB4820E, 0xEAB820, 0xFCEE4B, 0xFFFFB8},
      {0x1E8E92, 0x34C6C0, 0x5DECF5, 0xDCFFFF}
   };

   private static void paintAllToolsWeaponsAndArmor() {
      for (int tier = 0; tier < 5; tier++) {
         int[] pal = TIER_PAL[tier];
         putItem(300 + tier * 4, TexOverhaulV57.mcSword(pal));
         putItem(300 + tier * 4 + 1, TexOverhaulV57.mcPickaxe(pal));
         putItem(300 + tier * 4 + 2, TexOverhaulV57.mcAxe(pal));
         putItem(300 + tier * 4 + 3, TexOverhaulV57.mcShovel(pal));
         putItem(290 + tier, TexOverhaulV57.mcHoe(pal));
      }

      int[][] armorPal = {
         {0x6E3C20, 0xA05A32, 0xC07246, 0xD88A5C},
         TIER_PAL[2],
         TIER_PAL[3],
         TIER_PAL[4]
      };
      for (int mat = 0; mat < 4; mat++) {
         for (int slot = 0; slot < 4; slot++) {
            putItem(320 + mat * 4 + slot, TexOverhaulV57.mcArmor(slot, armorPal[mat]));
         }
      }

      putItem(Items.BOW, TexOverhaulV57.paintRealisticBow());
   }

   // ====================================================================
   //  10. EVERY SINGLE ITEM SPRITE (256..1023) Painted 100% Brand-New
   // ====================================================================
   private static void paintAllEverySingleItemSprite() {
      putItem(Items.STICK, paintStick());
      putItem(Items.COAL, paintLump(c(0x2E3036), c(0x181A1E), c(0x5E626C)));
      putItem(Items.IRON_INGOT, paintIngot(c(0xE2E6EC), c(0xB8C0CA), c(0x7E8690), c(0xFFFFFF)));
      putItem(Items.GOLD_INGOT, paintIngot(c(0xFCEE4B), c(0xDAA818), c(0x9E6E08), c(0xFFFFC8)));
      putItem(Items.NETHER_BRICK, paintIngot(c(0x522A30), c(0x3A1C20), c(0x241014), c(0x703C44)));
      putItem(Items.DIAMOND, paintGem(c(0x5DECF5), c(0x28B2BC), c(0x167882), c(0xE4FFFF)));
      putItem(Items.EMERALD, paintGem(c(0x17DD62), c(0x0EA844), c(0x086E2A), c(0xC2FFD8)));
      putItem(Items.ARROW, paintArrow());

      putItem(Items.LEATHER, paintPelt(c(0xAC6234), c(0x7E4420), c(0xC87A48)));
      putItem(Items.FEATHER, paintFeather());
      putItem(Items.STRING, paintString());
      putItem(Items.BONE, paintBone());
      putItem(Items.GUNPOWDER, paintPowderPile(c(0x5E6066), c(0x383A40), c(0x82848C)));
      putItem(Items.FLINT, paintShard(c(0x383A42), c(0x1E2026), c(0x6E727E)));
      putItem(Items.BONE_MEAL, paintPowderPile(c(0xECEAE2), c(0xC4C0B4), c(0xFFFFFF)));
      putItem(Items.SUGAR, paintPowderPile(c(0xF4F6FA), c(0xCED4DE), c(0xFFFFFF)));
      putItem(Items.PAPER, paintPaper());
      putItem(Items.BOOK, paintBook());
      putItem(Items.EGG, paintEgg());
      putItem(Items.QUARTZ, paintShard(c(0xECE8DE), c(0xC2BCAE), c(0xFFFFFF)));
      putItem(Items.BLAZE_ROD, paintBlazeRod());
      putItem(Items.BLAZE_POWDER, paintPowderPile(c(0xFF9818), c(0xC84E08), c(0xFFF068)));
      putItem(Items.GHAST_TEAR, paintDrop(c(0xDCECF4), c(0x98B8C8), c(0xFFFFFF)));
      putItem(Items.GOLD_NUGGET, paintDrop(c(0xF8D428), c(0xB4820C), c(0xFFFFB4)));
      putItem(Items.ENDER_PEARL, paintOrb(c(0x1E9886), c(0x0E5248), c(0x78FFE8), false));
      putItem(Items.EYE_OF_ENDER, paintOrb(c(0x58D868), c(0x1C6E38), c(0xD8FF98), true));

      putItem(Items.BUCKET, paintBucket(0));
      putItem(Items.WATER_BUCKET, paintBucket(1));
      putItem(Items.LAVA_BUCKET, paintBucket(2));
      putItem(Items.MILK_BUCKET, paintBucket(3));
      putItem(Items.FLINT_STEEL, paintFlintSteel());
      putItem(Items.SHEARS, paintShears());

      putItem(Items.APPLE, paintApple(c(0xE22822), c(0x9E1410), c(0xFF988E)));
      putItem(Redstone319.GOLDEN_APPLE, paintApple(c(0xFAD628), c(0xB8860E), c(0xFFFFB8)));
      putItem(Items.RAW_PORK, paintMeat(c(0xF08882), c(0xC45650), c(0xFFD2CE)));
      putItem(Items.COOKED_PORK, paintMeat(c(0xCC8E52), c(0x986230), c(0xECC288)));
      putItem(Items.RAW_BEEF, paintMeat(c(0xD83C34), c(0x9E201A), c(0xF4988E)));
      putItem(Items.STEAK, paintMeat(c(0x884828), c(0x5C2C14), c(0xB46C44)));
      putItem(Items.RAW_CHICKEN, paintChicken(c(0xF2C2B4), c(0xC89282), c(0xFFF0E8)));
      putItem(Items.COOKED_CHICKEN, paintChicken(c(0xCE7E32), c(0x98541A), c(0xF0A858)));
      putItem(Items.RAW_MUTTON, paintMeat(c(0xDC4C44), c(0xA42C26), c(0xF8B8B0)));
      putItem(Items.COOKED_MUTTON, paintMeat(c(0x965432), c(0x68341C), c(0xBE7850)));
      putItem(Items.ROTTEN_FLESH, paintMeat(c(0x9E5238), c(0x526828), c(0xB8744E)));
      putItem(Items.SEEDS, paintSeeds(c(0x78B838), c(0x4E8220)));
      putItem(Items.PUMPKIN_SEEDS, paintSeeds(c(0xE6D898), c(0xB8A664)));
      putItem(Items.MELON_SEEDS, paintSeeds(c(0x4E4234), c(0x2E241C)));
      putItem(Items.WHEAT, paintWheatBundle());
      putItem(Items.BREAD, paintBread());
      putItem(Items.CARROT, paintCarrot(c(0xF27818), c(0xBA520A), c(0x48A828)));
      putItem(Redstone319.GOLDEN_CARROT, paintCarrot(c(0xFAD428), c(0xBA880E), c(0xFFE878)));
      putItem(Items.POTATO, paintPotato(false));
      putItem(Items.BAKED_POTATO, paintPotato(true));
      putItem(Items.MELON_SLICE, paintMelonSlice());
      putItem(Items.PUMPKIN_PIE, paintPie());
      putItem(Items.CAKE, paintCakeIcon());
      putItem(Redstone319.COOKIE, paintCookie());
      putItem(Redstone319.MUSHROOM_STEW, paintBowl(true));
      putItem(Redstone319.BOWL, paintBowl(false));

      for (int i = 0; i < 15; i++) {
         int col = c(PAL16[i + 1]);
         putItem(Items.DYE1 + i, paintPowderPile(col, mul(col, 0.72F), mul(col, 1.24F)));
      }

      putItem(Items.DOOR, paintDoorIcon(c(0xAC824A)));
      int[] doorCols = {0x785834, 0xCEBC82, 0xA87854, 0xBE683A, 0x483018, 0xD8DCE4};
      for (int i = 0; i < 6; i++) {
         putSprite(590 + i, paintDoorIcon(c(doorCols[i])));
      }
      for (int i = 0; i < 16; i++) {
         putSprite(TexArt315.BED_ITEM0 + i, paintBedIcon(c(PAL16[i])));
         putSprite(TexArt315.BANNER_ITEM0 + i, paintBannerIcon(c(PAL16[i])));
      }

      putItem(Redstone319.ITEM_REDSTONE, paintPowderPile(c(0xE8201C), c(0x980C0A), c(0xFF7868)));
      putItem(Redstone319.COMPASS, paintDial(false));
      putItem(Redstone319.CLOCK, paintDial(true));
      putItem(Redstone319.FIRE_CHARGE, paintOrb(c(0xFF6818), c(0x4E2214), c(0xFFEE68), false));
      putItem(Redstone319.PISTOL, paintGun(0));
      putItem(Redstone319.MACHINE_GUN, paintGun(1));
      putItem(Redstone319.SNIPER_RIFLE, paintGun(2));
      putItem(Redstone319.TNT_GRENADE, paintGrenade());
      putItem(Redstone319.MURDER_KNIFE, paintKnife());
      putItem(Redstone319.C4_BOMB, paintC4());
      putItem(Redstone319.SIEGE_CANNON, paintCannonIcon(false));
      putItem(Redstone319.MORTAR_STRIKE, paintCannonIcon(true));
      putSprite(BackroomsStalkerMod.T_ALMOND_WATER, paintBottle());
   }

   private static int[] paintStick() {
      int[] p = new int[N * N];
      for (int i = 0; i < 10; i++) {
         set(p, 3 + i, 13 - i, c(0xA47A44));
         set(p, 4 + i, 13 - i, c(0x6E4E28));
      }
      return p;
   }

   private static int[] paintLump(int mid, int dk, int hi) {
      int[] p = new int[N * N];
      rect(p, 5, 4, 6, 8, mid);
      rect(p, 4, 5, 8, 6, mid);
      rect(p, 5, 5, 3, 2, hi);
      rect(p, 5, 11, 6, 1, dk);
      return p;
   }

   private static int[] paintIngot(int top, int mid, int sh, int spec) {
      int[] p = new int[N * N];
      rect(p, 4, 5, 8, 2, top);
      rect(p, 3, 7, 10, 3, mid);
      rect(p, 4, 10, 8, 2, sh);
      line(p, 5, 5, 9, 5, spec);
      return p;
   }

   private static int[] paintGem(int mid, int dk, int deep, int spec) {
      int[] p = new int[N * N];
      rect(p, 5, 3, 6, 2, mid);
      rect(p, 4, 5, 8, 5, mid);
      rect(p, 5, 10, 6, 2, dk);
      rect(p, 6, 12, 4, 1, deep);
      rect(p, 5, 4, 3, 3, spec);
      return p;
   }

   private static int[] paintArrow() {
      int[] p = new int[N * N];
      line(p, 1, 13, 4, 13, c(0xF4F4F8));
      line(p, 2, 14, 4, 12, c(0xD0D4DC));
      line(p, 3, 12, 11, 4, c(0x9E7642));
      line(p, 4, 12, 12, 4, c(0x684A26));
      rect(p, 11, 2, 3, 3, c(0xC8CED8));
      set(p, 13, 2, c(0xFFFFFF));
      return p;
   }

   private static int[] paintPelt(int mid, int dk, int hi) {
      int[] p = new int[N * N];
      rect(p, 4, 3, 8, 10, mid);
      rect(p, 3, 4, 10, 3, hi);
      rect(p, 3, 10, 10, 3, dk);
      return p;
   }

   private static int[] paintFeather() {
      int[] p = new int[N * N];
      line(p, 3, 13, 12, 4, c(0xD8DCE4));
      for (int i = 0; i < 7; i++) {
         rect(p, 5 + i, 10 - i, 3, 2, i < 4 ? c(0xECEFF4) : c(0xFFFFFF));
      }
      return p;
   }

   private static int[] paintString() {
      int[] p = new int[N * N];
      line(p, 3, 11, 7, 4, c(0xECEEF2));
      line(p, 7, 4, 12, 9, c(0xD4D8E0));
      line(p, 12, 9, 5, 13, c(0xF8FAFC));
      return p;
   }

   private static int[] paintBone() {
      int[] p = new int[N * N];
      rect(p, 2, 11, 3, 3, c(0xECE8DA));
      rect(p, 11, 2, 3, 3, c(0xF6F4E8));
      line(p, 4, 11, 11, 4, c(0xF2EFE2));
      line(p, 5, 11, 11, 5, c(0xCCC6B4));
      return p;
   }

   private static int[] paintPowderPile(int mid, int dk, int hi) {
      int[] p = new int[N * N];
      rect(p, 6, 5, 4, 2, hi);
      rect(p, 4, 7, 8, 3, mid);
      rect(p, 3, 10, 10, 2, dk);
      return p;
   }

   private static int[] paintShard(int mid, int dk, int hi) {
      int[] p = new int[N * N];
      rect(p, 6, 3, 4, 3, hi);
      rect(p, 4, 6, 7, 4, mid);
      rect(p, 5, 10, 5, 3, dk);
      return p;
   }

   private static int[] paintPaper() {
      int[] p = new int[N * N];
      rect(p, 4, 3, 8, 10, c(0xF4F2EA));
      line(p, 5, 5, 10, 5, c(0xC8C4B8));
      line(p, 5, 8, 10, 8, c(0xC8C4B8));
      return p;
   }

   private static int[] paintBook() {
      int[] p = new int[N * N];
      rect(p, 3, 3, 9, 10, c(0x8E4824));
      rect(p, 11, 4, 2, 8, c(0xF2EFE4));
      rect(p, 5, 5, 5, 2, c(0xE8C438));
      return p;
   }

   private static int[] paintEgg() {
      return paintLump(c(0xEDE6D2), c(0xC8BEAA), c(0xFFFDF6));
   }

   private static int[] paintBlazeRod() {
      int[] p = new int[N * N];
      for (int i = 0; i < 10; i++) {
         set(p, 3 + i, 13 - i, c(0xFFEE58));
         set(p, 4 + i, 13 - i, c(0xE87814));
      }
      return p;
   }

   private static int[] paintDrop(int mid, int dk, int hi) {
      int[] p = new int[N * N];
      rect(p, 7, 4, 2, 2, hi);
      rect(p, 5, 6, 6, 5, mid);
      rect(p, 6, 11, 4, 2, dk);
      return p;
   }

   private static int[] paintOrb(int mid, int deep, int spec, boolean eye) {
      int[] p = new int[N * N];
      for (int y = 3; y <= 12; y++) {
         for (int x = 3; x <= 12; x++) {
            float dx = (x - 7.5F) / 4.6F;
            float dy = (y - 7.5F) / 4.6F;
            if (dx * dx + dy * dy <= 1.0F) {
               set(p, x, y, dx + dy > 0.35F ? deep : mid);
            }
         }
      }
      rect(p, 5, 5, 2, 2, spec);
      if (eye) rect(p, 7, 5, 2, 6, c(0x182218));
      return p;
   }

   private static int[] paintBucket(int fill) {
      int[] p = new int[N * N];
      rect(p, 3, 5, 10, 5, c(0xC6CCD4));
      rect(p, 4, 10, 8, 3, c(0xA4ACB6));
      rect(p, 5, 13, 6, 1, c(0x78808A));
      int rim = fill == 1 ? c(0x2E78F0) : (fill == 2 ? c(0xFF6A14) : (fill == 3 ? c(0xFAF8F2) : c(0x4E545C)));
      rect(p, 4, 4, 8, 2, rim);
      return p;
   }

   private static int[] paintFlintSteel() {
      int[] p = new int[N * N];
      rect(p, 3, 4, 5, 2, c(0xD8DCE4));
      rect(p, 3, 6, 2, 5, c(0xB8C0C8));
      rect(p, 3, 10, 5, 2, c(0x8E96A0));
      rect(p, 8, 7, 5, 5, c(0x32343A));
      return p;
   }

   private static int[] paintShears() {
      int[] p = new int[N * N];
      rect(p, 2, 10, 4, 4, c(0x9E482C));
      rect(p, 6, 11, 4, 3, c(0x823820));
      line(p, 5, 9, 12, 2, c(0xE8ECF2));
      line(p, 7, 10, 14, 5, c(0xD8DEE8));
      return p;
   }

   private static int[] paintApple(int mid, int sh, int spec) {
      int[] p = paintOrb(mid, sh, spec, false);
      line(p, 8, 2, 8, 4, c(0x684826));
      rect(p, 9, 3, 3, 1, c(0x48A828));
      return p;
   }

   private static int[] paintMeat(int mid, int dk, int fat) {
      int[] p = new int[N * N];
      rect(p, 3, 5, 10, 6, mid);
      rect(p, 4, 4, 7, 2, fat);
      rect(p, 4, 11, 8, 2, dk);
      return p;
   }

   private static int[] paintChicken(int mid, int dk, int bone) {
      int[] p = paintMeat(mid, dk, mid);
      rect(p, 11, 3, 3, 2, bone);
      rect(p, 11, 10, 3, 2, bone);
      return p;
   }

   private static int[] paintSeeds(int hi, int dk) {
      int[] p = new int[N * N];
      int[][] pts = {{4, 8}, {7, 6}, {10, 8}, {5, 11}, {9, 11}};
      for (int[] s : pts) {
         rect(p, s[0], s[1], 2, 2, hi);
         set(p, s[0] + 1, s[1] + 1, dk);
      }
      return p;
   }

   private static int[] paintWheatBundle() {
      int[] p = new int[N * N];
      rect(p, 5, 3, 6, 10, c(0xDCA828));
      rect(p, 4, 4, 8, 4, c(0xF2CC48));
      rect(p, 5, 8, 6, 2, c(0xB83224));
      return p;
   }

   private static int[] paintBread() {
      int[] p = new int[N * N];
      rect(p, 3, 6, 10, 5, c(0xCE9438));
      rect(p, 2, 7, 12, 3, c(0xBA7E26));
      line(p, 5, 6, 6, 8, c(0xF4C868));
      line(p, 9, 6, 10, 8, c(0xF4C868));
      return p;
   }

   private static int[] paintCarrot(int body, int sh, int leaf) {
      int[] p = new int[N * N];
      line(p, 4, 12, 10, 6, body);
      line(p, 5, 12, 11, 6, sh);
      rect(p, 10, 3, 4, 3, leaf);
      return p;
   }

   private static int[] paintPotato(boolean baked) {
      int[] p = paintLump(c(0xC89E54), c(0x967032), c(0xE2BA72));
      if (baked) rect(p, 6, 6, 4, 3, c(0xFFF2B8));
      return p;
   }

   private static int[] paintMelonSlice() {
      int[] p = new int[N * N];
      rect(p, 3, 6, 10, 4, c(0xE23228));
      rect(p, 3, 10, 10, 2, c(0x4C8E24));
      set(p, 6, 8, c(0x282018));
      set(p, 9, 8, c(0x282018));
      return p;
   }

   private static int[] paintPie() {
      int[] p = new int[N * N];
      rect(p, 3, 6, 10, 5, c(0xDC7A22));
      rect(p, 2, 9, 12, 3, c(0xC89448));
      return p;
   }

   private static int[] paintCakeIcon() {
      int[] p = new int[N * N];
      rect(p, 3, 5, 10, 3, c(0xFAF6EE));
      rect(p, 3, 8, 10, 4, c(0xC88432));
      set(p, 6, 5, c(0xDC2020));
      set(p, 9, 5, c(0xDC2020));
      return p;
   }

   private static int[] paintCookie() {
      int[] p = paintLump(c(0xC88A3E), c(0x986424), c(0xE0A458));
      set(p, 6, 6, c(0x3E2412));
      set(p, 9, 8, c(0x3E2412));
      set(p, 7, 10, c(0x3E2412));
      return p;
   }

   private static int[] paintBowl(boolean stew) {
      int[] p = new int[N * N];
      rect(p, 3, 8, 10, 4, c(0x8E6234));
      rect(p, 4, 12, 8, 1, c(0x5E3E1E));
      if (stew) {
         rect(p, 4, 6, 8, 2, c(0xC87A54));
         set(p, 6, 6, c(0xDC2820));
      }
      return p;
   }

   private static int[] paintDoorIcon(int col) {
      int[] p = new int[N * N];
      rect(p, 4, 2, 8, 12, col);
      rect(p, 5, 3, 6, 4, mul(col, 1.18F));
      rect(p, 5, 8, 6, 5, mul(col, 0.82F));
      set(p, 10, 8, c(0xF6D038));
      return p;
   }

   private static int[] paintBedIcon(int col) {
      int[] p = new int[N * N];
      rect(p, 2, 6, 4, 4, c(0xF6F4EC));
      rect(p, 6, 6, 8, 4, col);
      rect(p, 2, 10, 12, 2, c(0x9E723C));
      rect(p, 2, 12, 2, 2, c(0x6E4C24));
      rect(p, 12, 12, 2, 2, c(0x6E4C24));
      return p;
   }

   private static int[] paintBannerIcon(int col) {
      int[] p = new int[N * N];
      rect(p, 7, 1, 2, 14, c(0x8E6638));
      rect(p, 4, 2, 8, 10, col);
      rect(p, 5, 3, 6, 8, mul(col, 1.12F));
      return p;
   }

   private static int[] paintDial(boolean clock) {
      int[] p = paintOrb(clock ? c(0xF2C828) : c(0xB8C0CA), clock ? c(0xA4780E) : c(0x6E7680), c(0xFFFFFF), false);
      rect(p, 5, 5, 6, 6, clock ? c(0x28488E) : c(0x242830));
      line(p, 7, 8, 9, 5, clock ? c(0xFFE858) : c(0xE82420));
      return p;
   }

   private static int[] paintGun(int type) {
      int[] p = new int[N * N];
      int body = type == 2 ? c(0x3E5A38) : c(0x383E46);
      int bbl = c(0x78828E);
      rect(p, 2, 6, type == 0 ? 9 : 13, 3, body);
      rect(p, 9, 5, 5, 2, bbl);
      rect(p, 4, 9, 3, 4, c(0x5A3E24));
      if (type == 2) rect(p, 5, 3, 6, 2, c(0x22262C));
      return p;
   }

   private static int[] paintGrenade() {
      int[] p = paintOrb(c(0x3E5834), c(0x24361E), c(0x6E925C), false);
      rect(p, 6, 2, 4, 2, c(0xB8C0CA));
      return p;
   }

   private static int[] paintKnife() {
      int[] p = new int[N * N];
      rect(p, 3, 10, 3, 3, c(0x4E321C));
      line(p, 6, 9, 13, 2, c(0xE8ECF4));
      line(p, 7, 10, 14, 3, c(0x98A2AE));
      return p;
   }

   private static int[] paintC4() {
      int[] p = new int[N * N];
      rect(p, 3, 4, 10, 8, c(0xC8B488));
      rect(p, 5, 6, 6, 3, c(0x222824));
      set(p, 9, 7, c(0xFF2820));
      return p;
   }

   private static int[] paintCannonIcon(boolean mortar) {
      int[] p = new int[N * N];
      rect(p, 3, 10, 10, 3, c(0x7E542C));
      if (mortar) {
         rect(p, 5, 4, 6, 6, c(0x3A4048));
      } else {
         rect(p, 2, 6, 12, 4, c(0x3A4048));
      }
      return p;
   }

   private static int[] paintBottle() {
      int[] p = new int[N * N];
      rect(p, 6, 2, 4, 2, c(0xAC7E48));
      rect(p, 4, 4, 8, 9, c(0x98D8F0));
      rect(p, 5, 6, 6, 5, c(0xE8DCC2));
      return p;
   }
}

