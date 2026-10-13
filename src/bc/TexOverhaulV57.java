package bc;

/**
 * Update V5.9 — Realistic 5.6 x Minecraft HD-Feel Texture Engine & Multi-Version Resource Pack Switcher:
 * - Fixes the Bow (268) with an authentic diagonal Recurve Bow (taut bowstring attached to both nock tips,
 *   curved yew wood limbs, wrapped leather riser grip).
 * - Updates EVERY block and item texture that was previously left on legacy 3.5 / 3.12 / 3.13 / 3.15 procedural art.
 * - Adds PBR-inspired directional micro-normal relief shading, crevice ambient occlusion, and natural warm/cool
 *   lighting across all blocks and items in Default (V5.9 Realistic) mode while supporting V5.8 Fusion,
 *   V5.7 Bold Pixel Art, and V5.6 Painted via ResourcePacks.textureVersionMode().
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

   private static int lum(int col) {
      int r = (col >> 16) & 255;
      int g = (col >> 8) & 255;
      int b = col & 255;
      return (r * 77 + g * 150 + b * 29) >> 8;
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

   static int[] readTile(int tile) {
      int[] out = new int[N * N];
      if (tile < 0 || tile >= Tex.COLS * Tex.COLS) {
         return out;
      }
      int tx0 = Tex.tx(tile);
      int ty0 = Tex.ty(tile);
      if ((ty0 + 15) * Tex.PX + tx0 + 16 > Tex.atlasPixels.length) {
         return out;
      }
      for (int y = 0; y < N; y++) {
         System.arraycopy(Tex.atlasPixels, (ty0 + y) * Tex.PX + tx0, out, y * N, N);
      }
      return out;
   }

   /**
    * PBR-inspired directional micro-normal relief + crevice ambient occlusion + natural warm/cool grading.
    * Treats each pixel's luminance as a micro-heightfield and computes top-left directional sunlight
    * and local cavity shadow, making 16x16 tiles look tactile, three-dimensional, and realistic.
    */
   static int[] applyRealisticRelief(int[] src, float reliefStrength, boolean wrapSeamless) {
      int[] out = new int[N * N];
      float[] h = new float[N * N];
      for (int i = 0; i < N * N; i++) {
         h[i] = (src[i] >>> 24) < 32 ? -1.0F : lum(src[i]) / 255.0F;
      }
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            int col = src[y * N + x];
            int alpha = col >>> 24;
            if (alpha < 32) {
               out[y * N + x] = col;
               continue;
            }
            int xl = wrapSeamless ? ((x - 1) & 15) : Math.max(0, x - 1);
            int xr = wrapSeamless ? ((x + 1) & 15) : Math.min(15, x + 1);
            int yt = wrapSeamless ? ((y - 1) & 15) : Math.max(0, y - 1);
            int yb = wrapSeamless ? ((y + 1) & 15) : Math.min(15, y + 1);

            float hc = h[y * N + x];
            float hl = h[y * N + xl] < 0 ? hc : h[y * N + xl];
            float hr = h[y * N + xr] < 0 ? hc : h[y * N + xr];
            float ht = h[yt * N + x] < 0 ? hc : h[yt * N + x];
            float hb = h[yb * N + x] < 0 ? hc : h[yb * N + x];

            // Directional slope from top-left light source + local Laplacian cavity AO
            float slope = ((hc - hl) * 0.65F + (hc - ht) * 0.85F - (hc - hr) * 0.25F - (hc - hb) * 0.35F);
            float avgNeighbor = 0.25F * (hl + hr + ht + hb);
            float cavity = (hc - avgNeighbor) * 0.55F;
            float delta = (slope * 0.72F + cavity * 0.48F) * reliefStrength;
            if (delta > 0.22F) delta = 0.22F;
            if (delta < -0.22F) delta = -0.22F;

            int r = (col >> 16) & 255;
            int g = (col >> 8) & 255;
            int b = col & 255;
            if (delta >= 0.0F) {
               // Warm sunlit highlight
               r = Math.min(255, Math.round(r * (1.0F + delta * 1.06F) + delta * 14.0F));
               g = Math.min(255, Math.round(g * (1.0F + delta * 1.02F) + delta * 10.0F));
               b = Math.min(255, Math.round(b * (1.0F + delta * 0.94F) + delta * 5.0F));
            } else {
               // Cool ambient crevice shadow
               r = Math.max(0, Math.round(r * (1.0F + delta * 1.08F)));
               g = Math.max(0, Math.round(g * (1.0F + delta * 1.02F)));
               b = Math.max(0, Math.round(b * (1.0F + delta * 0.92F)));
            }
            out[y * N + x] = (alpha << 24) | (r << 16) | (g << 8) | b;
         }
      }
      return out;
   }

   private static void fuseTile(int tile, int[] mc, float mcWeight, int mode) {
      if (tile < 0 || tile >= Tex.COLS * Tex.COLS) {
         return;
      }
      int[] v56 = readTile(tile);
      int[] out = new int[N * N];
      float w = mode == 2 ? Math.min(0.92F, mcWeight + 0.35F) : mcWeight;
      for (int i = 0; i < N * N; i++) {
         int a = v56[i];
         int b = mc[i];
         if ((b >>> 24) == 0) {
            out[i] = a;
         } else if ((a >>> 24) == 0) {
            out[i] = b;
         } else {
            out[i] = mix(a, b, w);
         }
      }
      if (mode == 0) {
         out = applyRealisticRelief(out, 0.78F, true);
      }
      Tex.blit(tile, out);
   }

   private static int[] shadeAndFuseSprite(int[] v56, int[] mcRaw, float mcWeight, int seed, int mode) {
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
               mc[y * N + x] = mul(col, mode == 2 ? 0.50F : 0.68F);
            } else if (lOpen || tOpen) {
               mc[y * N + x] = mul(col, 1.16F);
            } else {
               float grain = 1.0F + (hash(x, y, seed) - 0.5F) * 0.06F;
               mc[y * N + x] = mul(col, grain);
            }
         }
      }
      if (mode == 0) {
         mc = applyRealisticRelief(mc, 0.65F, false);
      }
      if (v56 == null || mode == 2) {
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
         }
      }
      return out;
   }

   private static void setItemSprite(int itemId, int[] mcRaw, float mcWeight, int mode) {
      int tile = Items.tile[itemId];
      if (tile <= 0 || tile >= Tex.COLS * Tex.COLS) return;
      int[] v56 = readTile(tile);
      Tex.blit(tile, shadeAndFuseSprite(v56, mcRaw, mcWeight, tile + 700, mode));
   }

   // ====================================================================
   //  Master Entry Point — Supports All Texture Versions & Updates Everything
   // ====================================================================
   static void applyAll() {
      int mode = ResourcePacks.textureVersionMode();
      // Always fix the Bow (268) in every non-3.5 pack so it is never backwards!
      Tex.blit(Items.tile[Items.BOW], shadeAndFuseSprite(null, paintRealisticBow(), 1.0F, 968, mode == 3 ? 1 : mode));
      if (mode == 3) {
         // Pure V5.6 / V4.0 Painted pack (with fixed Bow)
         return;
      }

      // 1. Fuse Base Terrain Blocks
      fuseTile(Tex.GRASS_TOP, mcGrassTop(), 0.45F, mode);
      fuseTile(Tex.DIRT, mcDirt(), 0.48F, mode);
      fuseGrassSideOverlay(mode);
      fuseTile(Tex.STONE, mcStone(101), 0.46F, mode);
      fuseTile(Tex.COBBLE, mcCobble(false), 0.44F, mode);
      fuseTile(Tex.MOSSY, mcCobble(true), 0.46F, mode);
      fuseTile(Tex.SAND, mcSand(false), 0.45F, mode);
      fuseTile(Tex.GRAVEL, mcGravel(), 0.46F, mode);
      fuseTile(Tex.BEDROCK, mcBedrock(), 0.48F, mode);

      // 2. Logs, Planks & Leaves (keeping turned logs 246..248 synced with TexArt310)
      Tex.blit(Tex.LOG_SIDE, TexPaint.oakSide());
      Tex.blit(Tex.SPRUCE_SIDE, TexPaint.spruceSide());
      Tex.blit(Tex.BIRCH_SIDE, TexPaint.birchSide());
      TexArt310.blocks();
      fuseTile(Tex.LOG_TOP, mcLogTop(c(0x584327), c(0xB8945E), c(0x9E7C48)), 0.45F, mode);
      fuseTile(Tex.SPRUCE_TOP, mcLogTop(c(0x332211), c(0x8E6C3E), c(0x74562E)), 0.45F, mode);
      fuseTile(Tex.PLANKS, mcPlanks(c(0xA2824E), 510), 0.48F, mode);
      fuseTile(TexArt313.PLANKS0, mcPlanks(c(0x725430), 511), 0.48F, mode);
      fuseTile(TexArt313.PLANKS0 + 1, mcPlanks(c(0xC8B77A), 512), 0.48F, mode);
      fuseTile(TexArt313.PLANKS0 + 2, mcPlanks(c(0xA07350), 513), 0.48F, mode);
      fuseTile(TexArt313.PLANKS0 + 3, mcPlanks(c(0xBA6337), 514), 0.48F, mode);
      fuseTile(TexArt313.PLANKS0 + 4, mcPlanks(c(0x422B15), 515), 0.48F, mode);

      fuseLeavesTile(Tex.LEAVES, mode);
      for (int k = 0; k < 3; k++) {
         fuseLeavesTile(TexArt313.LEAVES0 + k, mode);
      }
      fuseLeavesTile(642, mode);

      // 3. Ores (5.6 Stone + 5.6 Ore + Realistic Multi-Faceted Minecraft Ore Veins)
      fuseTile(Tex.COAL, mcOre(c(0x28282C), c(0x141418), c(0x505058), 120, false), 0.56F, mode);
      fuseTile(Tex.IRON, mcOre(c(0xD8AF93), c(0x9E7256), c(0xF8DEC8), 130, false), 0.56F, mode);
      fuseTile(Tex.GOLD, mcOre(c(0xFCEE4B), c(0xC49010), c(0xFFFFC4), 140, false), 0.56F, mode);
      fuseTile(Tex.DIAMOND, mcOre(c(0x5DECF5), c(0x2098A4), c(0xE4FFFF), 150, false), 0.56F, mode);
      fuseTile(Tex.REDSTONE, mcOre(c(0xE62020), c(0x880A0A), c(0xFF8282), 160, false), 0.56F, mode);
      fuseTile(Tex.LAPIS, mcOre(c(0x2654D8), c(0x122A82), c(0x82ACFF), 170, false), 0.56F, mode);
      fuseTile(Color312.T_EMERALD_ORE, mcOre(c(0x17DD62), c(0x0A8838), c(0xB4FFD8), 180, true), 0.56F, mode);

      // 4. Bricks, Stone Bricks (including 655..657), Sandstone (including 651..654), Glass, Obsidian, Glowstone, Snow, Ice
      fuseTile(Tex.BRICKS, mcBricks(), 0.48F, mode);
      fuseTile(Tex.STONEBRICK, mcStoneBrick(false, false, false), 0.48F, mode);
      fuseTile(Redstone319.T_CHIS_STONEBRICK, mcStoneBrick(false, false, true), 0.52F, mode);
      fuseTile(Redstone319.T_MOSSY_STONEBRICK, mcStoneBrick(false, true, false), 0.52F, mode);
      fuseTile(Redstone319.T_CRACKED_STONEBRICK, mcStoneBrick(true, false, false), 0.52F, mode);
      fuseTile(Tex.SANDSTONE_SIDE, mcSandstoneSide(false), 0.46F, mode);
      fuseTile(Tex.SANDSTONE_TOP, mcSandstoneTop(false), 0.46F, mode);
      fuseTile(TexArt313.RED_SAND, mcSand(true), 0.45F, mode);
      fuseTile(TexArt313.RS_SIDE, mcSandstoneSide(true), 0.46F, mode);
      fuseTile(TexArt313.RS_TOP, mcSandstoneTop(true), 0.46F, mode);
      fuseTile(Redstone319.T_CHIS_SANDSTONE, mcSandstoneSide(false), 0.46F, mode);
      fuseTile(Redstone319.T_SMOOTH_SANDSTONE, mcSandstoneTop(false), 0.46F, mode);
      fuseTile(Redstone319.T_CHIS_RED_SANDSTONE, mcSandstoneSide(true), 0.46F, mode);
      fuseTile(Redstone319.T_SMOOTH_RED_SANDSTONE, mcSandstoneTop(true), 0.46F, mode);
      fuseGlassTile(Tex.GLASS);
      fuseTile(Tex.OBSIDIAN, mcObsidian(), 0.48F, mode);
      fuseTile(Tex.GLOWSTONE, mcGlowstone(), 0.48F, mode);
      fuseTile(Tex.SNOW, mcSnow(), 0.44F, mode);
      fuseTile(TexArt313.ICE, mcIce(false), 0.44F, mode);
      fuseTile(TexArt313.PACKED_ICE, mcIce(true), 0.44F, mode);

      // 5. Animated Water & Lava Frames (so Tex.animate() uses realistic caustics & magma crust!)
      upgradeFluidFrames(mode);

      // 6. Work Blocks, Spawner, Crops, Nether, End, Mineral Blocks, Wool, Concrete, Terracotta, Plants & Redstone
      fuseTile(Tex.CRAFT_TOP, mcCraftTop(), 0.50F, mode);
      fuseTile(Tex.CRAFT_SIDE, mcCraftSide(), 0.50F, mode);
      fuseTile(Tex.FURNACE_FRONT, mcFurnaceFront(false), 0.50F, mode);
      fuseTile(Tex.FURNACE_ON, mcFurnaceFront(true), 0.50F, mode);
      fuseTile(Tex.CHEST_TOP, mcChestTop(), 0.48F, mode);
      fuseTile(Tex.CHEST_SIDE, mcChestSide(), 0.48F, mode);
      Tex.blit(43, paintRealisticSpawner());
      fuseTile(220, TexPaint319.bookshelf(), 0.50F, mode);
      fuseTile(Tex.TNT_SIDE, TexPaint319.tntSide(), 0.50F, mode);
      fuseTile(Tex.TNT_TOP, TexPaint319.tntTop(true), 0.48F, mode);
      fuseTile(Tex.TNT_BOTTOM, TexPaint319.tntTop(false), 0.48F, mode);
      fuseTile(Tex.FARM_TOP, TexPaint319.farmland(false), 0.46F, mode);
      fuseTile(203, TexPaint319.farmland(true), 0.46F, mode);
      fuseTile(212, TexPaint319.haySide(), 0.46F, mode);
      fuseTile(213, TexPaint319.hayTop(), 0.46F, mode);
      fuseTile(221, TexPaint319.pumpkinTop(), 0.46F, mode);
      fuseTile(222, TexPaint319.pumpkinSide(), 0.46F, mode);
      fuseTile(223, TexPaint319.pumpkinFace(false), 0.48F, mode);
      fuseTile(224, TexPaint319.pumpkinFace(true), 0.48F, mode);
      fuseTile(225, TexPaint319.melonTop(), 0.46F, mode);
      fuseTile(226, TexPaint319.melonSide(), 0.46F, mode);

      fuseTile(Color312.T_IRON, mcMetalBlock(c(0xDCDCDC), c(0xC2C2C2), c(0x9E9E9E), c(0xF8F8F8)), 0.48F, mode);
      fuseTile(Color312.T_GOLD, mcMetalBlock(c(0xF6D028), c(0xD8AC14), c(0xAA7E0A), c(0xFFF498)), 0.48F, mode);
      fuseTile(Color312.T_DIAMOND, mcGemBlock(c(0x5CECE4), c(0x32C0B8), c(0x1C8E88), c(0xDCFFFF)), 0.48F, mode);
      fuseTile(Color312.T_EMERALD, mcGemBlock(c(0x28D862), c(0x16AC48), c(0x0E7A30), c(0xA4FFC4)), 0.48F, mode);
      fuseTile(TexArt312.NETHERRACK, TexPaint319.netherrack(), 0.46F, mode);
      fuseTile(TexArt312.SOUL_SAND, TexPaint319.soulSand(), 0.48F, mode);
      fuseTile(TexArt312.QUARTZ_ORE, TexPaint319.quartzOre(), 0.52F, mode);
      fuseTile(TexArt312.NETHER_BRICKS, TexPaint319.netherBricks(), 0.48F, mode);
      fuseTile(TexArt312.END_STONE, TexPaint319.endStone(), 0.48F, mode);

      // 7. Upgrade ALL remaining blocks in the atlas (Wool, Concrete, Terracotta, Stained Glass, 3.13 Plants/Woods/Stones, 3.15 Trapdoors/Bars/Beds, 3.19 Redstone/Prismarine/Purpur)
      upgradeAllRemainingBlocks(mode);

      // 8. Upgrade ALL Tools, Weapons, Armor & Every Single Item Sprite (256..1023)
      fuseAllToolsAndArmor(mode);
      upgradeAllItemSprites(mode);
   }

   // ====================================================================
   //  Realistic Fluid Frames & Previously Untouched Block Upgrades
   // ====================================================================
   private static void upgradeFluidFrames(int mode) {
      if (mode != 0) return;
      for (int f = 0; f < 16; f++) {
         if (Tex.waterFrames[f] != null) {
            Tex.waterFrames[f] = applyRealisticRelief(Tex.waterFrames[f], 0.55F, true);
         }
         if (Tex.lavaFrames[f] != null) {
            Tex.lavaFrames[f] = applyRealisticRelief(Tex.lavaFrames[f], 0.65F, true);
         }
      }
      if (Tex.waterFrames[0] != null) Tex.blit(23, Tex.waterFrames[0]);
      if (Tex.lavaFrames[0] != null) Tex.blit(24, Tex.lavaFrames[0]);
   }

   private static int[] paintRealisticSpawner() {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            boolean frame = x <= 1 || x >= 14 || y <= 1 || y >= 14;
            boolean bar = x == 4 || x == 7 || x == 8 || x == 11 || y == 4 || y == 7 || y == 8 || y == 11;
            boolean diag = Math.abs(x - y) <= 1 || Math.abs(x + y - 15) <= 1;
            if (frame || bar || diag) {
               int base = frame ? c(0x28323C) : c(0x364250);
               if (x == 0 || y == 0 || x == 4 || y == 4) base = mul(base, 1.24F);
               else if (x == 15 || y == 15) base = mul(base, 0.72F);
               p[y * N + x] = base;
            }
         }
      }
      // Glowing ember core inside cage
      p[7 * N + 7] = c(0xFF7820);
      p[8 * N + 8] = c(0xFFB838);
      return p;
   }

   private static void upgradeAllRemainingBlocks(int mode) {
      // Wool 48..63: realistic woven fleece relief
      for (int i = 0; i < 16; i++) {
         int t = 48 + i;
         Tex.blit(t, applyRealisticRelief(readTile(t), mode == 0 ? 0.75F : 0.45F, true));
      }
      // Beds 183..185 & 630..641: quilted wool weave & plump pillow shading
      int[] bedTiles = {183, 184, 185, 630, 631, 632, 633, 634, 635, 636, 637, 638, 639, 640, 641};
      for (int t : bedTiles) {
         Tex.blit(t, applyRealisticRelief(readTile(t), 0.70F, false));
      }
      // Doors, Ladder, Torch, Crops, Stems, Cake (160, 161, 171..179, 204..211, 219, 227..234)
      int[] cutouts = {
         36, 160, 161, 171, 172, 173, 174, 175, 176, 177, 178, 179,
         204, 205, 206, 207, 208, 209, 210, 211, 219, 227, 228, 229, 230, 231, 232, 233, 234
      };
      for (int t : cutouts) {
         Tex.blit(t, applyRealisticRelief(readTile(t), 0.65F, false));
      }
      // Update 3.12 blocks (260..269 & 400..488): Magma, Quartz, End Frame, Dragon Egg, Concrete, Powder, Terracotta, Glazed, Clay, Stained Glass, Coal/Lapis blocks
      for (int t = 260; t <= 269; t++) {
         Tex.blit(t, applyRealisticRelief(readTile(t), 0.75F, true));
      }
      for (int t = 400; t <= 487; t++) {
         if (t >= 482 && t <= 485) continue; // already fused metal/gem blocks
         boolean glass = t >= Color312.T_GLASS0 && t < Color312.T_GLASS0 + 16;
         if (glass) {
            fuseStainedGlassTile(t);
         } else {
            Tex.blit(t, applyRealisticRelief(readTile(t), 0.72F, true));
         }
      }
      // Update 3.13 blocks (520..587): Granite, Diorite, Andesite, Podzol, Mycelium, Coarse Dirt, Jungle/Acacia/DarkOak Barks & Rings, Flowers, Double Plants, Lily, Vine, Mushrooms, Snow Side
      for (int t = 520; t <= 587; t++) {
         if ((t >= 534 && t <= 538) || (t >= 550 && t <= 552) || t == 523 || t == 529 || t == 530 || t == 531 || t == 532) {
            continue;
         }
         boolean seamless = t <= 548 || (t >= 584 && t <= 587);
         Tex.blit(t, applyRealisticRelief(readTile(t), 0.75F, seamless));
      }
      // Re-transpose turned barks 546..548 from upgraded barks 540..542 so fallen trees match!
      for (int k = 0; k < 3; k++) {
         int[] src = readTile(TexArt313.BARK0 + k);
         int[] tr = new int[N * N];
         for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
               tr[y * N + x] = src[x * N + y];
            }
         }
         Tex.blit(TexArt313.TURNED0 + k, tr);
      }
      // Update 3.15 blocks (600..626): Iron Bars, Glass Pane Edges, Wooden & Iron Trapdoors
      for (int t = 600; t <= 626; t++) {
         Tex.blit(t, applyRealisticRelief(readTile(t), 0.75F, false));
      }
      // Update 3.19 Redstone, Prismarine, Purpur, Bone Block, Slime, Sea Lantern (643..687)
      for (int t = 643; t <= 687; t++) {
         if (t >= 651 && t <= 657) continue; // already fused sandstone/stonebrick variants
         if (t >= 672 && t <= 679) continue; // item tiles handled below
         Tex.blit(t, applyRealisticRelief(readTile(t), 0.76F, true));
      }
   }

   private static void fuseStainedGlassTile(int tile) {
      int[] p = readTile(tile).clone();
      int border = p[0];
      int hi = mul(border, 1.32F) | (border & 0xFF000000);
      int sh = mul(border, 0.75F) | (border & 0xFF000000);
      for (int i = 0; i < N; i++) {
         p[i] = i < 15 ? hi : sh;
         p[i * N] = i < 15 ? hi : sh;
         p[15 * N + i] = sh;
         p[i * N + 15] = sh;
      }
      for (int k = 0; k < 4; k++) {
         p[(2 + k) * N + (5 - k)] = hi;
      }
      Tex.blit(tile, p);
   }

   // ====================================================================
   //  Minecraft x 5.6 Block Pattern Generators
   // ====================================================================
   private static int[] mcGrassTop() {
      int[] pal = {rgb(152, 152, 152), rgb(166, 166, 166), rgb(180, 180, 180), rgb(194, 194, 194), rgb(208, 208, 208)};
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float v = fbm(x, y, 7501) * 0.65F + hash(x, y, 7502) * 0.35F;
            int col = step(pal, contrast(v, 1.85F));
            if (((x * 3 + y * 5) & 7) == 0) col = rgb(216, 216, 216);
            else if (((x * 5 + y * 3) & 7) == 3) col = rgb(146, 146, 146);
            p[y * N + x] = col;
         }
      }
      return p;
   }

   private static int[] mcDirt() {
      int[] pal = {c(0x63452E), c(0x785438), c(0x866043), c(0x966C4A), c(0xA67A56)};
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
         p[s[1] * N + s[0]] = s[2] == 1 ? c(0x9E9082) : c(0x4E3622);
      }
      return p;
   }

   private static void fuseGrassSideOverlay(int mode) {
      int[] v56 = readTile(Tex.GRASS_SIDE_OVL);
      int[] lip = {3, 2, 4, 4, 2, 3, 5, 3, 2, 2, 4, 5, 3, 2, 3, 4};
      int[] out = new int[N * N];
      for (int x = 0; x < N; x++) {
         int h = lip[x];
         for (int y = 0; y < N; y++) {
            int p56 = v56[y * N + x];
            if (y < h || (p56 >>> 24) != 0) {
               int g = 172 + (int)((hash(x, y, 7512) - 0.5F) * 28.0F);
               if (y == 0) g += 18;
               else if (y >= h - 1) g -= 18;
               int mc = rgb(g, g, g);
               out[y * N + x] = (p56 >>> 24) != 0 ? mix(p56, mc, 0.45F) : mc;
            }
         }
      }
      if (mode == 0) out = applyRealisticRelief(out, 0.55F, false);
      Tex.blit(Tex.GRASS_SIDE_OVL, out);
   }

   private static int[] mcStone(int seed) {
      int[] pal = {c(0x666668), c(0x737375), c(0x7E7E80), c(0x89898B), c(0x949496)};
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

   private static void fuseLeavesTile(int tile, int mode) {
      int[] v56 = readTile(tile);
      int[] out = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            int p = v56[y * N + x];
            if ((p >>> 24) == 0) {
               out[y * N + x] = 0;
               continue;
            }
            int mcLum = 156 + (((x >> 1) + (y >> 1) + ((x + y) & 1)) & 3) * 15;
            int mc = rgb(mcLum, mcLum, mcLum);
            out[y * N + x] = mix(p, mc, 0.36F);
         }
      }
      if (mode == 0) {
         out = applyRealisticRelief(out, 0.55F, false);
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
               p[sy * N + sx] = mul(p[sy * N + sx], 0.80F);
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

   // ====================================================================
   //  Realistic Tools, Weapons, Armor & Fixed Recurve Bow
   // ====================================================================
   private static final int[][] TIER_PAL = {
      {0x644826, 0x8F6A3A, 0xAE844C, 0xC69C62}, // 0: Wood
      {0x5A5C60, 0x808286, 0x9A9CA0, 0xB4B6BC}, // 1: Stone
      {0x889098, 0xC8CED6, 0xE4E8EE, 0xFFFFFF}, // 2: Iron
      {0xB4820E, 0xEAB820, 0xFCEE4B, 0xFFFFB8}, // 3: Gold
      {0x1E8E92, 0x34C6C0, 0x5DECF5, 0xDCFFFF}  // 4: Diamond
   };

   private static void fuseAllToolsAndArmor(int mode) {
      for (int tier = 0; tier < 5; tier++) {
         int[] pal = TIER_PAL[tier];
         setItemSprite(300 + tier * 4, mcSword(pal), 0.52F, mode);
         setItemSprite(300 + tier * 4 + 1, mcPickaxe(pal), 0.52F, mode);
         setItemSprite(300 + tier * 4 + 2, mcAxe(pal), 0.52F, mode);
         setItemSprite(300 + tier * 4 + 3, mcShovel(pal), 0.52F, mode);
         setItemSprite(290 + tier, mcHoe(pal), 0.52F, mode);
      }

      int[][] armorPal = {
         {0x6E3C20, 0xA05A32, 0xC07246, 0xD88A5C},
         TIER_PAL[2],
         TIER_PAL[3],
         TIER_PAL[4]
      };
      for (int mat = 0; mat < 4; mat++) {
         for (int slot = 0; slot < 4; slot++) {
            setItemSprite(320 + mat * 4 + slot, mcArmor(slot, armorPal[mat]), 0.50F, mode);
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
      set(p, 1, 14, sh); set(p, 2, 14, hi); set(p, 1, 13, hi);
      line(p, 2, 13, 4, 11, c(0x8F6838));
      line(p, 3, 13, 5, 11, c(0x624420));
      for (int i = 0; i < 9; i++) {
         int bx = 5 + i;
         int by = 10 - i;
         set(p, bx, by, mid);
         set(p, bx, by - 1, i >= 6 ? spec : hi);
         set(p, bx + 1, by, sh);
      }
      set(p, 14, 1, spec);
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

   /**
    * FIXED Realistic Recurve Bow (Items.BOW = 268):
    * - Curved yew wood belly arches outward toward the top-left (3, 3)..(5, 5),
    * - Carved horn nock tips at (12, 1) and (1, 12),
    * - Taut braided linen bowstring stretches diagonally from (12, 2) to (2, 12) directly connecting both tips!
    */
   static int[] paintRealisticBow() {
      int[] p = new int[N * N];
      int wHi = c(0xBA8E54);
      int wMid = c(0x8F6836);
      int wDark = c(0x5E4220);
      int gripLt = c(0x6E4826);
      int gripDk = c(0x462C16);
      int bandGold = c(0xDFB438);
      int strHi = c(0xEEECE4);
      int strSh = c(0xC2BEB2);

      // 1. Taut diagonal bowstring connecting upper nock (12, 2) to lower nock (2, 12)
      for (int i = 0; i <= 10; i++) {
         int sx = 12 - i;
         int sy = 2 + i;
         set(p, sx, sy, (i & 1) == 0 ? strHi : strSh);
      }

      // 2. Curved wooden recurve limbs & wrapped leather riser grip
      int[][] limb = {
         // Upper horn nock & upper limb
         {12, 1, 2}, {13, 1, 0}, {13, 2, 0},
         {10, 1, 2}, {11, 1, 1}, {11, 2, 0},
         {8, 1, 2}, {9, 1, 1}, {9, 2, 0}, {10, 2, 0},
         {6, 2, 2}, {7, 2, 1}, {8, 2, 1}, {7, 3, 0},
         {5, 3, 4}, {6, 3, 1}, {6, 4, 0},
         // Central wrapped leather grip (riser)
         {4, 4, 3}, {5, 4, 3}, {5, 5, 4},
         {3, 5, 3}, {4, 5, 3}, {4, 6, 0},
         {3, 6, 4},
         // Lower limb & lower horn nock
         {2, 6, 2}, {2, 7, 1}, {3, 7, 1}, {4, 7, 0},
         {2, 8, 2}, {2, 9, 1}, {3, 8, 0}, {3, 9, 0},
         {1, 9, 2}, {1, 10, 1}, {2, 10, 0}, {2, 11, 0},
         {1, 11, 2}, {1, 12, 1}, {1, 13, 0}, {2, 13, 0}
      };
      for (int[] pt : limb) {
         int col = pt[2] == 4 ? bandGold : (pt[2] == 3 ? ((pt[0] + pt[1] & 1) == 0 ? gripLt : gripDk)
            : (pt[2] == 2 ? wHi : (pt[2] == 1 ? wMid : wDark)));
         set(p, pt[0], pt[1], col);
      }
      return p;
   }

   // ====================================================================
   //  Complete Realistic Item Sprite Upgrade (Every Previously Untouched Item!)
   // ====================================================================
   private static void upgradeAllItemSprites(int mode) {
      // 1. Core materials, ingots, gems, bow, arrow, buckets, food & utility items
      Tex.blit(Items.tile[Items.BOW], shadeAndFuseSprite(null, paintRealisticBow(), 1.0F, 968, mode));
      setItemSprite(Items.ARROW, paintRealisticArrow(), 0.72F, mode);
      setItemSprite(Items.STICK, paintRealisticStick(), 0.65F, mode);
      setItemSprite(Items.COAL, paintRealisticCoal(), 0.68F, mode);
      setItemSprite(Items.IRON_INGOT, paintRealisticIngot(c(0xE2E6EC), c(0xB8C0CA), c(0x7E8690), c(0xFFFFFF)), 0.70F, mode);
      setItemSprite(Items.GOLD_INGOT, paintRealisticIngot(c(0xFCEE4B), c(0xDAA818), c(0x9E6E08), c(0xFFFFC8)), 0.70F, mode);
      setItemSprite(Items.NETHER_BRICK, paintRealisticIngot(c(0x4E282E), c(0x381A1E), c(0x241014), c(0x6C3A42)), 0.70F, mode);
      setItemSprite(Items.DIAMOND, paintRealisticGem(c(0x5DECF5), c(0x28B2BC), c(0x167882), c(0xE4FFFF)), 0.72F, mode);
      setItemSprite(Items.EMERALD, paintRealisticGem(c(0x17DD62), c(0x0EA844), c(0x086E2A), c(0xC2FFD8)), 0.72F, mode);
      setItemSprite(Items.APPLE, paintRealisticApple(c(0xE22822), c(0x9E1410), c(0xFF988E)), 0.72F, mode);
      setItemSprite(Redstone319.GOLDEN_APPLE, paintRealisticApple(c(0xFAD628), c(0xB8860E), c(0xFFFFB8)), 0.72F, mode);
      setItemSprite(Items.BREAD, paintRealisticBread(), 0.68F, mode);
      setItemSprite(Items.BUCKET, paintRealisticBucket(0), 0.70F, mode);
      setItemSprite(Items.WATER_BUCKET, paintRealisticBucket(1), 0.70F, mode);
      setItemSprite(Items.LAVA_BUCKET, paintRealisticBucket(2), 0.70F, mode);
      setItemSprite(Items.MILK_BUCKET, paintRealisticBucket(3), 0.70F, mode);
      setItemSprite(Items.FLINT_STEEL, paintRealisticFlintSteel(), 0.72F, mode);
      setItemSprite(Items.SHEARS, paintRealisticShears(), 0.72F, mode);
      setItemSprite(Items.ENDER_PEARL, paintRealisticOrb(c(0x1E9886), c(0x0E5248), c(0x78FFE8), false), 0.72F, mode);
      setItemSprite(Items.EYE_OF_ENDER, paintRealisticOrb(c(0x58D868), c(0x1C6E38), c(0xD8FF98), true), 0.75F, mode);

      // 2. Apply realistic relief & specular polish to every single remaining item tile (256..Blocks.EXT0)
      boolean[] visited = new boolean[Tex.COLS * Tex.COLS];
      visited[Items.tile[Items.BOW]] = true;
      for (int id = 256; id < Blocks.EXT0; id++) {
         if (Items.name[id] == null) continue;
         int t = Items.tile[id];
         if (t <= 0 || t >= visited.length || visited[t]) continue;
         visited[t] = true;
         int[] src = readTile(t);
         Tex.blit(t, shadeAndFuseSprite(src, src, 0.55F, t + 900, mode));
      }
   }

   private static int[] paintRealisticArrow() {
      int[] p = new int[N * N];
      // White feather fletching at bottom-left
      line(p, 1, 13, 4, 13, c(0xF4F4F8));
      line(p, 2, 14, 4, 12, c(0xD2D4DC));
      line(p, 2, 11, 2, 14, c(0xECECF2));
      // Cedar wood shaft
      line(p, 3, 12, 11, 4, c(0x9E7642));
      line(p, 4, 12, 12, 4, c(0x684A26));
      // Forged steel bodkin arrowhead at top-right
      rect(p, 11, 2, 3, 3, c(0xC2C8D2));
      line(p, 10, 2, 13, 2, c(0xEEF2F8));
      line(p, 13, 2, 13, 5, c(0x78808A));
      set(p, 14, 1, c(0xFFFFFF));
      return p;
   }

   private static int[] paintRealisticStick() {
      int[] p = new int[N * N];
      for (int i = 0; i < 10; i++) {
         set(p, 3 + i, 13 - i, i == 8 ? c(0xBA9258) : c(0x9E7640));
         set(p, 4 + i, 13 - i, c(0x684A24));
      }
      return p;
   }

   private static int[] paintRealisticCoal() {
      int[] p = new int[N * N];
      for (int y = 4; y <= 12; y++) {
         for (int x = 4; x <= 12; x++) {
            float dx = (x - 8.0F) / 4.2F;
            float dy = (y - 8.3F) / 3.8F;
            if (dx * dx + dy * dy <= 1.0F) {
               int col = dx + dy < -0.35F ? c(0x4E5058) : (dx + dy > 0.35F ? c(0x18181C) : c(0x2C2E34));
               set(p, x, y, col);
            }
         }
      }
      set(p, 6, 6, c(0x6E727C));
      set(p, 7, 6, c(0x5A5E68));
      return p;
   }

   private static int[] paintRealisticIngot(int top, int mid, int sh, int spec) {
      int[] p = new int[N * N];
      rect(p, 4, 5, 8, 2, top);
      rect(p, 3, 7, 10, 3, mid);
      rect(p, 4, 10, 8, 2, sh);
      line(p, 5, 5, 9, 5, spec);
      line(p, 4, 6, 11, 6, top);
      return p;
   }

   private static int[] paintRealisticGem(int mid, int dark, int deep, int spec) {
      int[] p = new int[N * N];
      rect(p, 5, 3, 6, 2, mid);
      rect(p, 4, 5, 8, 5, mid);
      rect(p, 5, 10, 6, 2, dark);
      rect(p, 6, 12, 4, 1, deep);
      rect(p, 5, 4, 3, 3, spec);
      line(p, 8, 8, 11, 8, dark);
      return p;
   }

   private static int[] paintRealisticApple(int mid, int sh, int spec) {
      int[] p = new int[N * N];
      for (int y = 4; y <= 13; y++) {
         for (int x = 3; x <= 12; x++) {
            float dx = (x - 7.5F) / 4.5F;
            float dy = (y - 8.8F) / 4.3F;
            if (dx * dx + dy * dy <= 1.0F) {
               set(p, x, y, dx + dy > 0.35F ? sh : mid);
            }
         }
      }
      rect(p, 5, 6, 2, 2, spec);
      line(p, 8, 2, 8, 4, c(0x684826));
      rect(p, 9, 3, 3, 1, c(0x48A828));
      return p;
   }

   private static int[] paintRealisticBread() {
      int[] p = new int[N * N];
      rect(p, 3, 6, 10, 5, c(0xCE9438));
      rect(p, 2, 7, 12, 3, c(0xBA7E26));
      rect(p, 3, 11, 10, 1, c(0x7E5216));
      line(p, 5, 6, 6, 8, c(0xF4C868));
      line(p, 8, 6, 9, 8, c(0xF4C868));
      line(p, 11, 6, 12, 8, c(0xF4C868));
      return p;
   }

   private static int[] paintRealisticBucket(int fill) {
      int[] p = new int[N * N];
      // Tapered riveted iron pail
      rect(p, 3, 5, 10, 5, c(0xC6CCD4));
      rect(p, 4, 10, 8, 3, c(0xA4ACB6));
      rect(p, 5, 13, 6, 1, c(0x78808A));
      line(p, 4, 5, 4, 11, c(0xEEF2F8));
      int rim = fill == 1 ? c(0x2E78F0) : (fill == 2 ? c(0xFF6A14) : (fill == 3 ? c(0xFAF8F2) : c(0x4E545C)));
      int rimHi = fill == 1 ? c(0x78B8FF) : (fill == 2 ? c(0xFFD848) : (fill == 3 ? c(0xFFFFFF) : c(0x68707A)));
      rect(p, 4, 4, 8, 2, rim);
      line(p, 5, 4, 9, 4, rimHi);
      return p;
   }

   private static int[] paintRealisticFlintSteel() {
      int[] p = new int[N * N];
      // C-shaped forged steel striker on left
      rect(p, 3, 4, 5, 2, c(0xD8DCE4));
      rect(p, 3, 6, 2, 5, c(0xB8C0C8));
      rect(p, 3, 10, 5, 2, c(0x8E96A0));
      // Dark knapped flint shard on right
      rect(p, 8, 7, 5, 5, c(0x32343A));
      rect(p, 9, 8, 2, 2, c(0x5E626C));
      return p;
   }

   private static int[] paintRealisticShears() {
      int[] p = new int[N * N];
      // Leather-dipped handles at bottom-left
      rect(p, 2, 10, 4, 4, c(0x9E482C));
      rect(p, 6, 11, 4, 3, c(0x823820));
      // Crossed bevelled steel blades at top-right
      line(p, 5, 9, 12, 2, c(0xE8ECF2));
      line(p, 6, 9, 13, 2, c(0xACBAC6));
      line(p, 7, 10, 14, 5, c(0xD8DEE8));
      line(p, 8, 10, 14, 6, c(0x8E98A4));
      set(p, 6, 9, c(0x4E545C)); // pivot rivet
      return p;
   }

   private static int[] paintRealisticOrb(int mid, int deep, int spec, boolean eye) {
      int[] p = new int[N * N];
      for (int y = 3; y <= 12; y++) {
         for (int x = 3; x <= 12; x++) {
            float dx = (x - 7.5F) / 4.6F;
            float dy = (y - 7.5F) / 4.6F;
            float r2 = dx * dx + dy * dy;
            if (r2 <= 1.0F) {
               set(p, x, y, r2 > 0.62F ? deep : mid);
            }
         }
      }
      rect(p, 5, 5, 2, 2, spec);
      if (eye) {
         rect(p, 7, 5, 2, 6, c(0x182218));
         line(p, 6, 6, 6, 9, c(0xFF9820));
         line(p, 9, 6, 9, 9, c(0xFF9820));
      }
      return p;
   }
}
