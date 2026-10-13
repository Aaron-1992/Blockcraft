package bc;

/**
 * Update V5.7 — 5.6 x Minecraft Texture Fusion Engine:
 * Fuses the organic FBM/Voronoi procedural textures of Blockcraft 5.6 (TexPaint + TexPaint319 + TexExtra)
 * directly with iconic 16x16 Minecraft pixel-art patterns, ore vein layouts, wood/brick joints, work-block
 * details, and self-shaded 16x16 tool/weapon/armor/item silhouettes (without heavy external black halos).
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

   /** Reads the existing 16x16 5.6 tile from Tex.atlasPixels. */
   static int[] readTile(int tile) {
      int[] out = new int[N * N];
      if (tile < 0 || tile >= Tex.COLS * Tex.COLS) {
         return out;
      }
      int tx0 = Tex.tx(tile);
      int ty0 = Tex.ty(tile);
      for (int y = 0; y < N; y++) {
         System.arraycopy(Tex.atlasPixels, (ty0 + y) * Tex.PX + tx0, out, y * N, N);
      }
      return out;
   }

   /** Fuses the existing 5.6 block tile with a Minecraft pixel-art pattern layer. */
   private static void fuseTile(int tile, int[] mc, float mcWeight) {
      if (tile < 0 || tile >= Tex.COLS * Tex.COLS) {
         return;
      }
      int[] v56 = readTile(tile);
      int[] out = new int[N * N];
      for (int i = 0; i < N * N; i++) {
         int a = v56[i];
         int b = mc[i];
         if ((b >>> 24) == 0) {
            out[i] = a;
         } else if ((a >>> 24) == 0) {
            out[i] = b;
         } else {
            out[i] = mix(a, b, mcWeight);
         }
      }
      Tex.blit(tile, out);
   }

   /**
    * Applies 5.6 + Minecraft self-colored edge shading (top-left highlight, bottom-right darkened self-edge)
    * without adding any external black outline border, then fuses with the 5.6 TexExtra sprite.
    */
   private static int[] selfShadeAndFuseSprite(int[] v56, int[] mcRaw, float mcWeight, int seed) {
      int[] mc = mcRaw.clone();
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            int col = mcRaw[y * N + x];
            if ((col >>> 24) == 0) continue;
            boolean rOpen = x == 15 || (mcRaw[y * N + x + 1] >>> 24) == 0;
            boolean bOpen = y == 15 || (mcRaw[(y + 1) * N + x] >>> 24) == 0;
            boolean lOpen = x == 0 || (mcRaw[y * N + x - 1] >>> 24) == 0;
            boolean tOpen = y == 0 || (mcRaw[(y - 1) * N + x] >>> 24) == 0;
            if (rOpen || bOpen) {
               mc[y * N + x] = mul(col, 0.66F);
            } else if (lOpen || tOpen) {
               mc[y * N + x] = mul(col, 1.15F);
            } else {
               float grain = 1.0F + (hash(x, y, seed) - 0.5F) * 0.05F;
               mc[y * N + x] = mul(col, grain);
            }
         }
      }
      if (v56 == null) {
         return mc;
      }
      int[] out = new int[N * N];
      for (int i = 0; i < N * N; i++) {
         boolean has56 = (v56[i] >>> 24) >= 128;
         boolean hasMc = (mc[i] >>> 24) >= 128;
         if (has56 && hasMc) {
            out[i] = mix(v56[i], mc[i], mcWeight);
         } else if (hasMc) {
            out[i] = mc[i];
         } else if (has56) {
            // Keep overlapping 5.6 interior detail where adjacent to mc
            int x = i & 15, y = i >> 4;
            boolean nearMc = (x > 0 && (mc[i - 1] >>> 24) >= 128)
               || (x < 15 && (mc[i + 1] >>> 24) >= 128)
               || (y > 0 && (mc[i - 16] >>> 24) >= 128)
               || (y < 15 && (mc[i + 16] >>> 24) >= 128);
            out[i] = nearMc ? v56[i] : 0;
         }
      }
      return out;
   }

   private static void fuseItemSprite(int itemId, int[] mcRaw, float mcWeight) {
      int tile = Items.tile[itemId];
      if (tile <= 0) return;
      int[] v56 = readTile(tile);
      Tex.blit(tile, selfShadeAndFuseSprite(v56, mcRaw, mcWeight, tile + 700));
   }

   // ====================================================================
   //  Master Entry Point — Fuses 5.6 Atlas with Minecraft Pixel Art
   // ====================================================================
   static void applyAll() {
      // 1. Fuse Base Terrain Blocks (5.6 TexPaint + Minecraft Pixel Art)
      fuseTile(Tex.GRASS_TOP, mcGrassTop(), 0.45F);
      fuseTile(Tex.DIRT, mcDirt(), 0.48F);
      fuseGrassSideOverlay();
      fuseTile(Tex.STONE, mcStone(101), 0.46F);
      fuseTile(Tex.COBBLE, mcCobble(false), 0.44F);
      fuseTile(Tex.MOSSY, mcCobble(true), 0.46F);
      fuseTile(Tex.SAND, mcSand(false), 0.45F);
      fuseTile(Tex.GRAVEL, mcGravel(), 0.46F);
      fuseTile(Tex.BEDROCK, mcBedrock(), 0.48F);

      // 2. Fuse Logs, Planks & Leaves (keeping 5.6 turned logs 246..248 synced with TexArt310)
      Tex.blit(Tex.LOG_SIDE, TexPaint.oakSide());
      Tex.blit(Tex.SPRUCE_SIDE, TexPaint.spruceSide());
      Tex.blit(Tex.BIRCH_SIDE, TexPaint.birchSide());
      TexArt310.blocks();
      fuseTile(Tex.LOG_TOP, mcLogTop(c(0x584327), c(0xB8945E), c(0x9E7C48)), 0.45F);
      fuseTile(Tex.SPRUCE_TOP, mcLogTop(c(0x332211), c(0x8E6C3E), c(0x74562E)), 0.45F);
      fuseTile(Tex.PLANKS, mcPlanks(c(0xA2824E), 510), 0.48F);
      fuseTile(TexArt313.PLANKS0, mcPlanks(c(0x725430), 511), 0.48F);
      fuseTile(TexArt313.PLANKS0 + 1, mcPlanks(c(0xC8B77A), 512), 0.48F);
      fuseTile(TexArt313.PLANKS0 + 2, mcPlanks(c(0xA07350), 513), 0.48F);
      fuseTile(TexArt313.PLANKS0 + 3, mcPlanks(c(0xBA6337), 514), 0.48F);
      fuseTile(TexArt313.PLANKS0 + 4, mcPlanks(c(0x422B15), 515), 0.48F);

      fuseLeavesTile(Tex.LEAVES, 600);
      for (int k = 0; k < 3; k++) {
         fuseLeavesTile(TexArt313.LEAVES0 + k, 610 + k);
      }
      fuseLeavesTile(642, 642);

      // 3. Fuse Ores (5.6 Stone + 5.6 Ore + Iconic Minecraft Staggered Ore Veins)
      fuseTile(Tex.COAL, mcOre(c(0x28282C), c(0x161618), c(0x46464C), 120, false), 0.54F);
      fuseTile(Tex.IRON, mcOre(c(0xD8AF93), c(0xA87E62), c(0xF2D4BE), 130, false), 0.54F);
      fuseTile(Tex.GOLD, mcOre(c(0xFCEE4B), c(0xC89614), c(0xFFFFB8), 140, false), 0.54F);
      fuseTile(Tex.DIAMOND, mcOre(c(0x5DECF5), c(0x249EA8), c(0xDCFFFF), 150, false), 0.54F);
      fuseTile(Tex.REDSTONE, mcOre(c(0xE62020), c(0x8E0C0C), c(0xFF7878), 160, false), 0.54F);
      fuseTile(Tex.LAPIS, mcOre(c(0x2654D8), c(0x142E88), c(0x78A2FF), 170, false), 0.54F);
      fuseTile(Color312.T_EMERALD_ORE, mcOre(c(0x17DD62), c(0x0C8E3C), c(0xA8FFD0), 180, true), 0.54F);

      // 4. Fuse Bricks, Stone Bricks, Sandstone, Glass, Obsidian, Glowstone, Snow, Ice
      fuseTile(Tex.BRICKS, mcBricks(), 0.48F);
      fuseTile(Tex.STONEBRICK, mcStoneBrick(false, false, false), 0.48F);
      fuseTile(Tex.SANDSTONE_SIDE, mcSandstoneSide(false), 0.46F);
      fuseTile(Tex.SANDSTONE_TOP, mcSandstoneTop(false), 0.46F);
      fuseTile(TexArt313.RED_SAND, mcSand(true), 0.45F);
      fuseTile(TexArt313.RS_SIDE, mcSandstoneSide(true), 0.46F);
      fuseTile(TexArt313.RS_TOP, mcSandstoneTop(true), 0.46F);
      fuseGlassTile(Tex.GLASS);
      fuseTile(Tex.OBSIDIAN, mcObsidian(), 0.48F);
      fuseTile(Tex.GLOWSTONE, mcGlowstone(), 0.48F);
      fuseTile(Tex.SNOW, mcSnow(), 0.44F);
      fuseTile(TexArt313.ICE, mcIce(false), 0.44F);
      fuseTile(TexArt313.PACKED_ICE, mcIce(true), 0.44F);

      // 5. Fuse Work Blocks, Crops, Nether, End & Mineral Blocks
      fuseTile(Tex.CRAFT_TOP, mcCraftTop(), 0.50F);
      fuseTile(Tex.CRAFT_SIDE, mcCraftSide(), 0.50F);
      fuseTile(Tex.FURNACE_FRONT, mcFurnaceFront(false), 0.50F);
      fuseTile(Tex.FURNACE_ON, mcFurnaceFront(true), 0.50F);
      fuseTile(Tex.CHEST_TOP, mcChestTop(), 0.48F);
      fuseTile(Tex.CHEST_SIDE, mcChestSide(), 0.48F);
      fuseTile(220, mcBookshelf(), 0.50F);
      fuseTile(Tex.TNT_SIDE, mcTntSide(), 0.50F);
      fuseTile(Tex.TNT_TOP, mcTntTop(true), 0.48F);
      fuseTile(Tex.TNT_BOTTOM, mcTntTop(false), 0.48F);
      fuseTile(Tex.FARM_TOP, mcFarmland(false), 0.46F);
      fuseTile(203, mcFarmland(true), 0.46F);
      fuseTile(212, mcHaySide(), 0.46F);
      fuseTile(213, mcHayTop(), 0.46F);
      fuseTile(221, mcPumpkinTop(), 0.46F);
      fuseTile(222, mcPumpkinSide(), 0.46F);
      fuseTile(223, mcPumpkinFace(false), 0.48F);
      fuseTile(224, mcPumpkinFace(true), 0.48F);
      fuseTile(225, mcMelonTop(), 0.46F);
      fuseTile(226, mcMelonSide(), 0.46F);

      fuseTile(Color312.T_IRON, mcMetalBlock(c(0xDCDCDC), c(0xC2C2C2), c(0x9E9E9E), c(0xF6F6F6)), 0.48F);
      fuseTile(Color312.T_GOLD, mcMetalBlock(c(0xF6D028), c(0xD8AC14), c(0xAA7E0A), c(0xFFF28E)), 0.48F);
      fuseTile(Color312.T_DIAMOND, mcGemBlock(c(0x5CECE4), c(0x32C0B8), c(0x1C8E88), c(0xD4FFFF)), 0.48F);
      fuseTile(Color312.T_EMERALD, mcGemBlock(c(0x28D862), c(0x16AC48), c(0x0E7A30), c(0x9CFFBE)), 0.48F);
      fuseTile(TexArt312.NETHERRACK, mcNetherrack(), 0.46F);
      fuseTile(TexArt312.SOUL_SAND, mcSoulSand(), 0.48F);
      fuseTile(TexArt312.QUARTZ_ORE, mcQuartzOre(), 0.52F);
      fuseTile(TexArt312.NETHER_BRICKS, mcNetherBricks(), 0.48F);
      fuseTile(TexArt312.END_STONE, mcEndStone(), 0.48F);

      // 6. Fuse All Tools, Weapons, Armor & Items (5.6 TexExtra + Minecraft 16x16 Self-Shaded Pixel Art)
      fuseAllToolsAndArmor();
      fuseAllItems();
   }

   // ====================================================================
   //  Minecraft x 5.6 Block Pattern Generators
   // ====================================================================
   private static int[] mcGrassTop() {
      int[] pal = {rgb(154, 154, 154), rgb(168, 168, 168), rgb(180, 180, 180), rgb(194, 194, 194), rgb(206, 206, 206)};
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float v = fbm(x, y, 7501) * 0.65F + hash(x, y, 7502) * 0.35F;
            int col = step(pal, contrast(v, 1.85F));
            if (((x * 3 + y * 5) & 7) == 0) col = rgb(214, 214, 214);
            else if (((x * 5 + y * 3) & 7) == 3) col = rgb(148, 148, 148);
            p[y * N + x] = col;
         }
      }
      return p;
   }

   private static int[] mcDirt() {
      int[] pal = {c(0x654730), c(0x79553A), c(0x866043), c(0x966C4A), c(0xA47854)};
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float v = fbm(x, y, 7303) * 0.65F + hash(x, y, 7304) * 0.35F;
            p[y * N + x] = step(pal, contrast(v, 1.9F));
         }
      }
      int[][] specks = {
         {2, 3, 1}, {7, 2, 0}, {12, 4, 1}, {4, 7, 0}, {10, 8, 1},
         {14, 10, 0}, {3, 12, 1}, {8, 13, 0}, {13, 14, 1}, {6, 5, 1}
      };
      for (int[] s : specks) {
         p[s[1] * N + s[0]] = s[2] == 1 ? c(0x9A8E82) : c(0x523926);
      }
      return p;
   }

   private static void fuseGrassSideOverlay() {
      int[] v56 = readTile(Tex.GRASS_SIDE_OVL);
      int[] lip = {3, 2, 4, 4, 2, 3, 5, 3, 2, 2, 4, 5, 3, 2, 3, 4};
      int[] out = new int[N * N];
      for (int x = 0; x < N; x++) {
         int h = lip[x];
         for (int y = 0; y < N; y++) {
            int p56 = v56[y * N + x];
            if (y < h || (p56 >>> 24) != 0) {
               int g = 172 + (int)((hash(x, y, 7512) - 0.5F) * 28.0F);
               if (y == 0) g += 16;
               else if (y >= h - 1) g -= 16;
               int mc = rgb(g, g, g);
               out[y * N + x] = (p56 >>> 24) != 0 ? mix(p56, mc, 0.45F) : mc;
            }
         }
      }
      Tex.blit(Tex.GRASS_SIDE_OVL, out);
   }

   private static int[] mcStone(int seed) {
      int[] pal = {c(0x686868), c(0x747474), c(0x7D7D7D), c(0x888888), c(0x929292)};
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float strata = vn((x + (y & 1) * 2) * 0.5F, y * 1.2F, 4, seed) * 0.68F + hash(x, y, seed + 5) * 0.32F;
            p[y * N + x] = step(pal, contrast(strata, 1.95F));
         }
      }
      return p;
   }

   private static int[] mcCobble(boolean mossy) {
      int[] p = TexPaint.cobble(202, 0, 0, 0);
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            int col = p[y * N + x];
            int lum = (col >> 16) & 255;
            if (mossy && ((lum < 92 && fbm(x, y, 7601) > 0.36F) || fbm(x, y, 7602) > 0.64F)) {
               int g = 102 + (int)(hash(x, y, 7603) * 36.0F);
               p[y * N + x] = rgb(g * 58 / 100, g, g * 38 / 100);
            }
         }
      }
      return p;
   }

   private static int[] mcSand(boolean red) {
      int[] pal = red
         ? new int[]{c(0xA8561A), c(0xB6601E), c(0xC26A24), c(0xCE762C), c(0xDA8236)}
         : new int[]{c(0xC6BC86), c(0xD2C894), c(0xDBD3A0), c(0xE3DCAC), c(0xECE6BA)};
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float v = fbm(x, y, red ? 7410 : 7401) * 0.68F + hash(x, y, red ? 7411 : 7402) * 0.32F;
            p[y * N + x] = step(pal, contrast(v, 1.85F));
         }
      }
      return p;
   }

   private static int[] mcGravel() {
      int[] pal = {c(0x6E6A68), c(0x7E7978), c(0x8A8482), c(0x989290), c(0xA6A09E)};
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float v = fbm(x * 2, y * 2, 7450) * 0.55F + hash(x, y, 7451) * 0.45F;
            p[y * N + x] = step(pal, contrast(v, 2.0F));
         }
      }
      return p;
   }

   private static int[] mcBedrock() {
      int[] pal = {c(0x202020), c(0x363636), c(0x525252), c(0x6C6C6C), c(0x848484)};
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float v = vn(x * 0.8F, y * 1.4F, 4, 7110) * 0.55F + hash(x, y, 7111) * 0.45F;
            p[y * N + x] = step(pal, contrast(v, 2.2F));
         }
      }
      return p;
   }

   private static int[] mcLogTop(int bark, int ringLight, int ringDark) {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            int edge = Math.min(Math.min(x, 15 - x), Math.min(y, 15 - y));
            if (edge <= 1) {
               p[y * N + x] = edge == 0 ? bark : mul(bark, 1.15F);
            } else {
               int ring = Math.max(Math.abs(x - 7) - (x >= 8 ? 1 : 0), Math.abs(y - 7) - (y >= 8 ? 1 : 0));
               p[y * N + x] = (ring & 1) == 0 ? ringLight : ringDark;
            }
         }
      }
      return p;
   }

   private static int[] mcPlanks(int base, int seed) {
      int seam = mul(base, 0.68F);
      int sh = mul(base, 0.88F);
      int lt = mul(base, 1.08F);
      int[] pal = {sh, base, lt};
      int[] joints = {7, 15, 3, 11};
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         int row = y >> 2;
         int py = y & 3;
         int jx = joints[row];
         for (int x = 0; x < N; x++) {
            if (py == 3 || x == jx) {
               p[y * N + x] = seam;
            } else {
               float g = vn((x + row * 5) * 0.25F, row + py * 0.5F, 4, seed) * 0.72F + hash(x, y, seed + 3) * 0.28F;
               int col = step(pal, contrast(g, 1.75F));
               if (py == 0) col = mul(col, 1.05F);
               p[y * N + x] = col;
            }
         }
      }
      return p;
   }

   private static void fuseLeavesTile(int tile, int seed) {
      int[] v56 = readTile(tile);
      int[] out = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            int p = v56[y * N + x];
            if ((p >>> 24) == 0) {
               out[y * N + x] = 0;
               continue;
            }
            // Fuse 5.6 leaf clusters with Minecraft's soft 2x2 leaf dither
            int mcLum = 158 + (((x >> 1) + (y >> 1) + ((x + y) & 1)) & 3) * 14;
            int mc = rgb(mcLum, mcLum, mcLum);
            out[y * N + x] = mix(p, mc, 0.36F);
         }
      }
      Tex.blit(tile, out);
   }

   private static int[] mcOre(int main, int shade, int hi, int seed, boolean emeraldStyle) {
      int[] p = readTile(Tex.STONE);
      int[][][] veins = emeraldStyle
         ? new int[][][]{
            {{3, 2, 2}, {4, 2, 1}, {3, 3, 1}, {4, 3, 0}},
            {{11, 3, 2}, {12, 3, 1}, {11, 4, 0}},
            {{6, 6, 2}, {7, 6, 1}, {6, 7, 1}, {7, 7, 0}},
            {{2, 10, 2}, {3, 10, 1}, {2, 11, 0}},
            {{10, 9, 2}, {11, 9, 1}, {10, 10, 1}, {11, 10, 0}},
            {{7, 13, 2}, {8, 13, 1}, {7, 14, 0}}
         }
         : new int[][][]{
            {{3, 2, 2}, {4, 2, 1}, {2, 3, 2}, {3, 3, 1}, {4, 3, 1}, {5, 3, 0}},
            {{10, 3, 2}, {11, 3, 1}, {12, 3, 0}, {9, 4, 1}, {10, 4, 1}, {11, 4, 0}},
            {{5, 6, 2}, {6, 6, 1}, {7, 6, 1}, {8, 6, 0}, {4, 7, 1}, {5, 7, 1}, {6, 7, 0}},
            {{11, 8, 2}, {12, 8, 1}, {13, 8, 0}, {11, 9, 1}, {12, 9, 0}},
            {{2, 10, 2}, {3, 10, 1}, {4, 10, 0}, {3, 11, 1}, {4, 11, 0}},
            {{8, 12, 2}, {9, 12, 1}, {10, 12, 1}, {11, 12, 0}, {7, 13, 1}, {8, 13, 1}, {9, 13, 0}}
         };
      int shiftX = (seed / 10) & 1;
      for (int[][] v : veins) {
         for (int[] pt : v) {
            int x = (pt[0] + shiftX) & 15;
            int y = pt[1] & 15;
            int col = pt[2] == 2 ? hi : (pt[2] == 1 ? main : shade);
            p[y * N + x] = col;
            int sx = (x + 1) & 15;
            int sy = (y + 1) & 15;
            if (p[sy * N + sx] == readTile(Tex.STONE)[sy * N + sx]) {
               p[sy * N + sx] = mul(p[sy * N + sx], 0.82F);
            }
         }
      }
      return p;
   }

   private static int[] mcBricks() {
      int[] p = new int[N * N];
      int mortar = c(0xB8B2AC);
      int mortarSh = c(0x9E9892);
      for (int y = 0; y < N; y++) {
         int row = y >> 2;
         int ry = y & 3;
         for (int x = 0; x < N; x++) {
            int xs = (x + (row & 1) * 4) & 15;
            int rx = xs & 7;
            if (ry == 0 || rx == 0) {
               p[y * N + x] = ry == 0 && rx > 0 ? mortar : mortarSh;
            } else {
               int base = ((row + (xs >> 3)) & 1) == 0 ? c(0x966153) : c(0xA46A5B);
               if (ry == 1 || rx == 1) base = mul(base, 1.08F);
               else if (ry == 3 || rx == 7) base = mul(base, 0.90F);
               p[y * N + x] = add(base, (int)((hash(x, y, 7200) - 0.5F) * 10));
            }
         }
      }
      return p;
   }

   private static int[] mcStoneBrick(boolean cracked, boolean mossy, boolean chiseled) {
      int[] p = mcStone(7500);
      for (int y = 0; y < N; y++) {
         int row = y >> 3;
         int by = y & 7;
         int off = (row & 1) * 8;
         for (int x = 0; x < N; x++) {
            int bx = (x + off) & 15;
            if (chiseled) {
               int d = Math.max(Math.abs(x - 7) - (x >= 8 ? 1 : 0), Math.abs(y - 7) - (y >= 8 ? 1 : 0));
               if (d == 7 || d == 4 || d == 1) p[y * N + x] = c(0x4E4E4E);
               else if (d == 6 || d == 3) p[y * N + x] = mul(p[y * N + x], 1.12F);
            } else if (by == 7 || bx == 15) {
               p[y * N + x] = c(0x4E4E4E);
            } else if (by == 0 || bx == 0) {
               p[y * N + x] = mul(p[y * N + x], 1.12F);
            } else if (by == 6 || bx == 14) {
               p[y * N + x] = mul(p[y * N + x], 0.88F);
            }
         }
      }
      if (cracked) {
         int[][] cr = {{4, 2}, {5, 3}, {5, 4}, {6, 5}, {11, 10}, {12, 11}, {11, 12}, {10, 13}};
         for (int[] k : cr) p[k[1] * N + k[0]] = c(0x3E3E3E);
      }
      if (mossy) {
         for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
               if (fbm(x, y, 7509) > 0.56F) {
                  int g = 104 + (int)(hash(x, y, 7510) * 34.0F);
                  p[y * N + x] = rgb(g * 56 / 100, g, g * 38 / 100);
               }
            }
         }
      }
      return p;
   }

   private static int[] mcSandstoneSide(boolean red) {
      int[] p = mcSand(red);
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            if (y == 0 || y == 3) p[y * N + x] = mul(p[y * N + x], 1.08F);
            else if (y == 2 || y == 15) p[y * N + x] = mul(p[y * N + x], 0.84F);
         }
      }
      return p;
   }

   private static int[] mcSandstoneTop(boolean red) {
      int[] p = mcSand(red);
      for (int i = 0; i < N; i++) {
         p[i] = mul(p[i], 1.07F);
         p[i * N] = mul(p[i * N], 1.07F);
         p[15 * N + i] = mul(p[15 * N + i], 0.90F);
         p[i * N + 15] = mul(p[i * N + 15], 0.90F);
      }
      return p;
   }

   private static void fuseGlassTile(int tile) {
      int[] v56 = readTile(tile);
      int[] out = v56.clone();
      int frame = c(0xA8D4E2);
      int dark = c(0x6A9EB0);
      int hi = c(0xEAF8FF);
      for (int i = 0; i < N; i++) {
         out[i] = i < 15 ? frame : dark;
         out[i * N] = i < 15 ? frame : dark;
         out[15 * N + i] = dark;
         out[i * N + 15] = dark;
      }
      for (int k = 0; k < 4; k++) {
         out[(2 + k) * N + (5 - k)] = hi;
      }
      Tex.blit(tile, out);
   }

   private static int[] mcObsidian() {
      int[] pal = {c(0x0E0B16), c(0x171224), c(0x241B38), c(0x33264E), c(0x443466)};
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float v = fbm(x, y, 7801) * 0.68F + hash(x, y, 7802) * 0.32F;
            p[y * N + x] = step(pal, contrast(v, 2.0F));
         }
      }
      return p;
   }

   private static int[] mcGlowstone() {
      int[] pal = {c(0x6E4418), c(0x9E6822), c(0xCE9838), c(0xF6D264), c(0xFFF8BE)};
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float v = vn(x * 0.5F, y * 0.5F, 8, 7701) * 0.65F + hash(x, y, 7702) * 0.35F;
            p[y * N + x] = step(pal, contrast(v, 2.2F));
         }
      }
      return p;
   }

   private static int[] mcSnow() {
      int[] pal = {c(0xE0ECF4), c(0xEAF2F8), c(0xF2F8FC), c(0xFAFDFF), c(0xFFFFFF)};
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            p[y * N + x] = step(pal, fbm(x, y, 7901) * 0.7F + hash(x, y, 7902) * 0.3F);
         }
      }
      return p;
   }

   private static int[] mcIce(boolean packed) {
      int[] pal = packed
         ? new int[]{c(0x7298E2), c(0x82A8EE), c(0x94B8F8), c(0xA8C8FF)}
         : new int[]{c(0x78AAF8), c(0x8AB8FF), c(0x9CC6FF), c(0xB4D6FF)};
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            p[y * N + x] = step(pal, fbm(x, y, packed ? 7427 : 7426));
         }
      }
      return p;
   }

   private static int[] mcCraftTop() {
      int[] p = mcPlanks(c(0xA2824E), 510);
      for (int y = 2; y <= 13; y++) {
         for (int x = 2; x <= 13; x++) {
            if (x == 2 || x == 13 || y == 2 || y == 13) p[y * N + x] = c(0x4E381E);
            else if (x == 6 || x == 9 || y == 6 || y == 9) p[y * N + x] = c(0x3C2A14);
            else p[y * N + x] = c(0xB8925A);
         }
      }
      return p;
   }

   private static int[] mcCraftSide() {
      int[] p = mcPlanks(c(0xA2824E), 510);
      for (int y = 0; y < 4; y++) {
         for (int x = 0; x < N; x++) {
            p[y * N + x] = y == 3 ? c(0x3C2A14) : c(0x7E5C32);
         }
      }
      return p;
   }

   private static int[] mcFurnaceFront(boolean lit) {
      int[] p = mcStoneBrick(false, false, false);
      for (int y = 3; y <= 7; y++) {
         for (int x = 4; x <= 11; x++) {
            if (y == 3 && (x == 4 || x == 11)) continue;
            p[y * N + x] = c(0x1C1C20);
         }
      }
      for (int y = 10; y <= 13; y++) {
         for (int x = 4; x <= 11; x++) {
            p[y * N + x] = (lit && y >= 11) ? (((x + y) & 1) == 0 ? c(0xFF9E22) : c(0xFFE468)) : c(0x323236);
         }
      }
      return p;
   }

   private static int[] mcChestTop() {
      int[] p = mcPlanks(c(0xA87638), 7210);
      for (int i = 0; i < N; i++) {
         p[i] = p[15 * N + i] = p[i * N] = p[i * N + 15] = c(0x382612);
      }
      return p;
   }

   private static int[] mcChestSide() {
      int[] p = mcChestTop();
      for (int x = 0; x < N; x++) {
         p[5 * N + x] = c(0x2E1E0C);
      }
      return p;
   }

   private static int[] mcBookshelf() {
      return TexPaint319.bookshelf();
   }

   private static int[] mcTntSide() {
      return TexPaint319.tntSide();
   }

   private static int[] mcTntTop(boolean top) {
      return TexPaint319.tntTop(top);
   }

   private static int[] mcFarmland(boolean wet) {
      return TexPaint319.farmland(wet);
   }

   private static int[] mcHaySide() {
      return TexPaint319.haySide();
   }

   private static int[] mcHayTop() {
      return TexPaint319.hayTop();
   }

   private static int[] mcPumpkinTop() {
      return TexPaint319.pumpkinTop();
   }

   private static int[] mcPumpkinSide() {
      return TexPaint319.pumpkinSide();
   }

   private static int[] mcPumpkinFace(boolean lit) {
      return TexPaint319.pumpkinFace(lit);
   }

   private static int[] mcMelonTop() {
      return TexPaint319.melonTop();
   }

   private static int[] mcMelonSide() {
      return TexPaint319.melonSide();
   }

   private static int[] mcMetalBlock(int mid, int dark, int deep, int hi) {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            if (x == 0 || y == 0) p[y * N + x] = hi;
            else if (x == 15 || y == 15) p[y * N + x] = deep;
            else p[y * N + x] = ((x + y * 2) & 3) == 0 ? mix(mid, hi, 0.25F) : ((y & 3) == 3 ? dark : mid);
         }
      }
      return p;
   }

   private static int[] mcGemBlock(int mid, int dark, int deep, int hi) {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            int d = Math.max(Math.abs(x - 7) - (x >= 8 ? 1 : 0), Math.abs(y - 7) - (y >= 8 ? 1 : 0));
            p[y * N + x] = d == 7 ? deep : (d == 6 ? hi : (d <= 2 ? hi : (x + y < 15 ? mid : dark)));
         }
      }
      return p;
   }

   private static int[] mcNetherrack() {
      return TexPaint319.netherrack();
   }

   private static int[] mcSoulSand() {
      return TexPaint319.soulSand();
   }

   private static int[] mcQuartzOre() {
      return TexPaint319.quartzOre();
   }

   private static int[] mcNetherBricks() {
      return TexPaint319.netherBricks();
   }

   private static int[] mcEndStone() {
      return TexPaint319.endStone();
   }

   // ====================================================================
   //  5.6 x Minecraft Fused Tools, Weapons, Armor & Items
   //  (Self-colored edge shading — NO external black border bloat!)
   // ====================================================================
   private static final int[][] TIER_PAL = {
      // {darkEdge, midCore, brightFace, specularGlint}
      {0x644826, 0x8F6A3A, 0xAE844C, 0xC69C62}, // 0: Wood
      {0x5A5C60, 0x808286, 0x9A9CA0, 0xB4B6BC}, // 1: Stone
      {0x889098, 0xC8CED6, 0xE4E8EE, 0xFFFFFF}, // 2: Iron
      {0xB4820E, 0xEAB820, 0xFCEE4B, 0xFFFFB8}, // 3: Gold
      {0x1E8E92, 0x34C6C0, 0x5DECF5, 0xDCFFFF}  // 4: Diamond
   };

   private static void fuseAllToolsAndArmor() {
      for (int tier = 0; tier < 5; tier++) {
         int[] pal = TIER_PAL[tier];
         // Sword (300 + tier * 4 + 0)
         fuseItemSprite(300 + tier * 4, mcSword(pal), 0.50F);
         // Pickaxe (300 + tier * 4 + 1)
         fuseItemSprite(300 + tier * 4 + 1, mcPickaxe(pal), 0.50F);
         // Axe (300 + tier * 4 + 2)
         fuseItemSprite(300 + tier * 4 + 2, mcAxe(pal), 0.50F);
         // Shovel (300 + tier * 4 + 3)
         fuseItemSprite(300 + tier * 4 + 3, mcShovel(pal), 0.50F);
         // Hoe (290 + tier)
         fuseItemSprite(290 + tier, mcHoe(pal), 0.50F);
      }

      int[][] armorPal = {
         {0x6E3C20, 0xA05A32, 0xC07246, 0xD88A5C}, // Leather
         TIER_PAL[2], // Iron
         TIER_PAL[3], // Gold
         TIER_PAL[4]  // Diamond
      };
      for (int mat = 0; mat < 4; mat++) {
         for (int slot = 0; slot < 4; slot++) {
            fuseItemSprite(320 + mat * 4 + slot, mcArmor(slot, armorPal[mat]), 0.48F);
         }
      }
   }

   private static void drawMcHandle(int[] p, int x0, int y0, int steps) {
      int hLight = c(0x987440);
      int hDark = c(0x644824);
      for (int i = 0; i < steps; i++) {
         set(p, x0 + i, y0 - i, hLight);
         set(p, x0 + i + 1, y0 - i, hDark);
      }
   }

   static int[] mcSword(int[] pal) {
      int[] p = new int[N * N];
      int sh = c(pal[0]), mid = c(pal[1]), hi = c(pal[2]), spec = c(pal[3]);
      // Pommel & wooden grip
      set(p, 1, 14, sh); set(p, 2, 14, hi); set(p, 1, 13, hi);
      line(p, 2, 13, 4, 11, c(0x8F6838));
      line(p, 3, 13, 5, 11, c(0x624420));
      // Diagonal blade (5.6 + Minecraft stepped diagonal)
      for (int i = 0; i < 9; i++) {
         int bx = 5 + i;
         int by = 10 - i;
         set(p, bx, by, mid);
         set(p, bx, by - 1, i >= 6 ? spec : hi);
         set(p, bx + 1, by, sh);
      }
      set(p, 14, 1, spec);
      // Crossguard
      line(p, 2, 9, 6, 13, mid);
      line(p, 3, 9, 7, 13, sh);
      set(p, 4, 10, hi);
      return p;
   }

   static int[] mcPickaxe(int[] pal) {
      int[] p = new int[N * N];
      int sh = c(pal[0]), mid = c(pal[1]), hi = c(pal[2]), spec = c(pal[3]);
      drawMcHandle(p, 2, 14, 10);
      int[][] head = {
         {8, 1, 2}, {9, 1, 3}, {10, 1, 2}, {11, 1, 1},
         {7, 2, 2}, {8, 2, 2}, {9, 2, 1}, {10, 2, 1}, {11, 2, 1}, {12, 2, 0},
         {6, 3, 1}, {7, 3, 0}, {11, 3, 1}, {12, 3, 1}, {13, 3, 0},
         {12, 4, 1}, {13, 4, 1}, {14, 4, 0},
         {13, 5, 1}, {14, 5, 0}, {13, 6, 0}
      };
      for (int[] q : head) {
         set(p, q[0], q[1], q[2] == 3 ? spec : (q[2] == 2 ? hi : (q[2] == 1 ? mid : sh)));
      }
      return p;
   }

   static int[] mcAxe(int[] pal) {
      int[] p = new int[N * N];
      int sh = c(pal[0]), mid = c(pal[1]), hi = c(pal[2]), spec = c(pal[3]);
      drawMcHandle(p, 2, 14, 10);
      int[][] blade = {
         {8, 1, 3}, {9, 1, 2}, {10, 1, 1},
         {7, 2, 3}, {8, 2, 2}, {9, 2, 1}, {10, 2, 1}, {11, 2, 0},
         {7, 3, 2}, {8, 3, 2}, {9, 3, 1}, {10, 3, 1}, {11, 3, 0}, {12, 3, 0},
         {7, 4, 2}, {8, 4, 1}, {9, 4, 1}, {10, 4, 0}, {11, 4, 0},
         {8, 5, 1}, {9, 5, 0}, {10, 5, 0}
      };
      for (int[] b : blade) {
         set(p, b[0], b[1], b[2] == 3 ? spec : (b[2] == 2 ? hi : (b[2] == 1 ? mid : sh)));
      }
      return p;
   }

   static int[] mcShovel(int[] pal) {
      int[] p = new int[N * N];
      int sh = c(pal[0]), mid = c(pal[1]), hi = c(pal[2]), spec = c(pal[3]);
      drawMcHandle(p, 2, 14, 9);
      rect(p, 10, 2, 4, 4, mid);
      line(p, 10, 2, 13, 2, hi);
      line(p, 10, 2, 10, 5, hi);
      line(p, 13, 3, 13, 5, sh);
      line(p, 10, 5, 13, 5, sh);
      set(p, 11, 2, spec);
      return p;
   }

   static int[] mcHoe(int[] pal) {
      int[] p = new int[N * N];
      int sh = c(pal[0]), mid = c(pal[1]), hi = c(pal[2]), spec = c(pal[3]);
      drawMcHandle(p, 2, 14, 10);
      rect(p, 8, 2, 6, 2, mid);
      line(p, 8, 2, 13, 2, hi);
      set(p, 10, 2, spec);
      rect(p, 12, 4, 2, 2, sh);
      return p;
   }

   static int[] mcArmor(int slot, int[] pal) {
      int[] p = new int[N * N];
      int sh = c(pal[0]), mid = c(pal[1]), hi = c(pal[2]);
      if (slot == 0) {
         rect(p, 3, 3, 10, 6, mid);
         rect(p, 3, 9, 3, 4, mid);
         rect(p, 10, 9, 3, 4, mid);
         rect(p, 4, 3, 8, 1, hi);
         rect(p, 6, 9, 4, 1, sh);
      } else if (slot == 1) {
         rect(p, 2, 2, 4, 4, mid);
         rect(p, 10, 2, 4, 4, mid);
         rect(p, 3, 5, 10, 9, mid);
         rect(p, 4, 6, 8, 1, hi);
         rect(p, 2, 6, 2, 3, sh);
         rect(p, 12, 6, 2, 3, sh);
      } else if (slot == 2) {
         rect(p, 3, 2, 10, 4, mid);
         rect(p, 3, 6, 4, 8, mid);
         rect(p, 9, 6, 4, 8, mid);
         rect(p, 3, 2, 10, 1, hi);
         rect(p, 7, 4, 2, 2, sh);
      } else {
         rect(p, 2, 8, 5, 6, mid);
         rect(p, 9, 8, 5, 6, mid);
         rect(p, 2, 12, 6, 2, sh);
         rect(p, 9, 12, 6, 2, sh);
         rect(p, 3, 8, 3, 1, hi);
         rect(p, 10, 8, 3, 1, hi);
      }
      return p;
   }

   private static void fuseAllItems() {
      // Polish all 5.6 item sprites by blending with TexPaint319's soft self-colored directional bevel
      // while preserving 5.6's clean silhouettes (no black outline bloat)
      boolean[] visited = new boolean[Tex.COLS * Tex.COLS];
      for (int id = 256; id < Blocks.EXT0; id++) {
         if (Items.name[id] == null) continue;
         int t = Items.tile[id];
         if (t <= 0 || t >= visited.length || visited[t]) continue;
         visited[t] = true;
         int[] v56 = readTile(t);
         int[] out = v56.clone();
         for (int y = 1; y < 15; y++) {
            for (int x = 1; x < 15; x++) {
               int p = v56[y * N + x];
               if ((p >>> 24) < 128) continue;
               boolean tlOpen = (v56[(y - 1) * N + x] >>> 24) < 128 || (v56[y * N + x - 1] >>> 24) < 128;
               boolean brOpen = (v56[(y + 1) * N + x] >>> 24) < 128 || (v56[y * N + x + 1] >>> 24) < 128;
               float k = 1.0F + (hash(x, y, t + 900) - 0.5F) * 0.05F;
               if (tlOpen && !brOpen) k += 0.08F;
               else if (brOpen && !tlOpen) k -= 0.08F;
               out[y * N + x] = mul(p, k);
            }
         }
         Tex.blit(t, out);
      }
   }
}
