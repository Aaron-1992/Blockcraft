package bc;

import java.util.Random;

/**
 * Update 3.19: completes the modern pixel-art texture overhaul across all remaining blocks, biome materials, Nether/End tiles, crops, work blocks,
 * item sprites, and the 3D bushy-leaf frond tile (642). Called at the end of {@link TexExtra#blocks()} so that direct calls to the legacy painter
 * methods in {@link Tex}, {@link TexExtra} and {@link TexArt37}..{@link TexArt315} remain unchanged for equivalence tests.
 */
final class TexPaint319 {
   static final int BUSHY_FROND = 642;
   private static final int N = 16;

   private TexPaint319() {
   }

   private static int c(int rgb) {
      return TexPaint.c(rgb);
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

   private static float fbm(int x, int y, int seed) {
      return TexPaint.fbm(x, y, seed);
   }

   private static float vn(float x, float y, int cells, int seed) {
      return TexPaint.vn(x, y, cells, seed);
   }

   private static float hash(int x, int y, int seed) {
      return TexPaint.hash(x, y, seed);
   }

   private static float contrast(float v, float k) {
      return TexPaint.contrast(v, k);
   }

   static void blocks() {
      // Base tiles in 0..36 that were still using the old 3.5 painters
      Tex.blit(Tex.LEAVES, leaves(0));
      Tex.blit(Tex.BEDROCK, bedrock());
      Tex.blit(Tex.GLASS, glass());
      Tex.blit(Tex.BRICKS, bricks());
      Tex.blit(Tex.SANDSTONE_TOP, sandstoneTop(false));
      Tex.blit(Tex.TALLGRASS, tallGrass());
      Tex.blit(Tex.FLOWER_RED, flower(true));
      Tex.blit(Tex.FLOWER_YELLOW, flower(false));
      Tex.blit(Tex.CACTUS_SIDE, cactusSide());
      Tex.blit(Tex.CACTUS_TOP, cactusTop());
      Tex.blit(Tex.TORCH, torch());

      // Update 3.6 & 3.7 blocks
      Tex.blit(Tex.DOOR_LO, door(false));
      Tex.blit(Tex.DOOR_HI, door(true));
      Tex.blit(Tex.FARM_TOP, farmland(false));
      Tex.blit(203, farmland(true));
      for (int s = 0; s < 8; s++) {
         Tex.blit(Tex.WHEAT0 + s, wheat(s));
      }

      for (int s = 0; s < 4; s++) {
         Tex.blit(204 + s, rootCrop(false, s));
         Tex.blit(208 + s, rootCrop(true, s));
      }

      Tex.blit(Tex.LADDER, ladder());
      Tex.blit(Tex.TNT_SIDE, tntSide());
      Tex.blit(Tex.TNT_TOP, tntTop(true));
      Tex.blit(Tex.TNT_BOTTOM, tntTop(false));
      Tex.blit(212, haySide());
      Tex.blit(213, hayTop());
      Tex.blit(219, sugarCane());
      Tex.blit(220, bookshelf());
      Tex.blit(221, pumpkinTop());
      Tex.blit(222, pumpkinSide());
      Tex.blit(223, pumpkinFace(false));
      Tex.blit(224, pumpkinFace(true));
      Tex.blit(225, melonTop());
      Tex.blit(226, melonSide());
      Tex.blit(231, cakeTop());
      Tex.blit(232, cakeSide(false));
      Tex.blit(233, cakeBottom());
      Tex.blit(234, cakeSide(true));

      // Update 3.12 mineral blocks, Nether, End, Concrete & Terracotta
      Tex.blit(Color312.T_IRON, metalBlock(0xD8DCDF, 0xB4BCC2, 0x8E969E, 0xF4F8FA, 214));
      Tex.blit(Color312.T_GOLD, metalBlock(0xF6CE3E, 0xD8A21E, 0x9E6C0A, 0xFFF29C, 215));
      Tex.blit(Color312.T_DIAMOND, gemBlock(0x52ECE4, 0x2CB8B8, 0x168288, 0xB8FFF8, 216));
      Tex.blit(Color312.T_EMERALD, gemBlock(0x28D462, 0x149E42, 0x0A6828, 0x98FFBA, 217));
      Tex.blit(Color312.T_COAL, coalBlock());
      Tex.blit(Color312.T_LAPIS, lapisBlock());
      Tex.blit(TexArt312.NETHERRACK, netherrack());
      Tex.blit(TexArt312.SOUL_SAND, soulSand());
      Tex.blit(TexArt312.QUARTZ_ORE, quartzOre());
      Tex.blit(TexArt312.NETHER_BRICKS, netherBricks());
      Tex.blit(TexArt312.MAGMA, magma());
      Tex.blit(TexArt312.QUARTZ_SIDE, quartzBlock(false));
      Tex.blit(TexArt312.QUARTZ_TOP, quartzBlock(true));
      Tex.blit(TexArt312.END_STONE, endStone());
      Tex.blit(TexArt312.FRAME_TOP, endFrameTop());
      Tex.blit(TexArt312.FRAME_SIDE, endFrameSide());
      Tex.blit(TexArt312.DRAGON_EGG, dragonEgg());
      Tex.blit(Color312.T_EMERALD_ORE, emeraldOre());
      Tex.blit(Color312.T_CLAY, clay());
      Tex.blit(Color312.T_TERRACOTTA, terracotta(Color312.PLAIN_TERRA, 346));
      for (int i = 0; i < 16; i++) {
         Tex.blit(Color312.T_CONCRETE0 + i, concrete(Color312.CONCRETE[i], 314 + i));
         Tex.blit(Color312.T_POWDER0 + i, powder(Color312.CONCRETE[i], 330 + i));
         Tex.blit(Color312.T_STAINED0 + i, terracotta(Color312.TERRA[i], 347 + i));
      }

      // Update 3.13 woods, stones & biome surfaces
      int[][] barks = {
         bark(400, c(0x3B2916), c(0x4D361E), c(0x5E4326), c(0x4A5E28)),
         bark(401, c(0x4E4A44), c(0x625D56), c(0x767068), c(0x888278)),
         bark(402, c(0x241A0E), c(0x312314), c(0x3E2D1A), c(0x4C3822))
      };
      for (int t = 0; t < 3; t++) {
         Tex.blit(TexArt313.BARK0 + t, barks[t]);
         int[] tr = new int[N * N];
         for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
               tr[y * N + x] = barks[t][x * N + y];
            }
         }

         Tex.blit(TexArt313.TURNED0 + t, tr);
      }

      Tex.blit(TexArt313.RING0, logTop(c(0x3B2916), c(0x9E7450), c(0xB88B64)));
      Tex.blit(TexArt313.RING0 + 1, logTop(c(0x56514B), c(0x9E5632), c(0xB8683E)));
      Tex.blit(TexArt313.RING0 + 2, logTop(c(0x241A0E), c(0x4A3319), c(0x5E4222)));
      Tex.blit(TexArt313.PLANKS0 + 2, planks(408, c(0x845B3C), c(0x966A48), c(0xA67854), c(0xB68862), c(0x6C482E)));
      Tex.blit(TexArt313.PLANKS0 + 3, planks(409, c(0x924B28), c(0xA45730), c(0xB4633A), c(0xC27146), c(0x783B1E)));
      Tex.blit(TexArt313.PLANKS0 + 4, planks(410, c(0x32200E), c(0x3E2813), c(0x4A3118), c(0x583B1E), c(0x241608)));
      Tex.blit(TexArt313.RED_SAND, redSand());
      Tex.blit(TexArt313.RS_SIDE, redSandstoneSide());
      Tex.blit(TexArt313.RS_TOP, sandstoneTop(true));
      Tex.blit(TexArt313.GRANITE, mineralStone(418, new int[]{c(0x7E5242), c(0x926250), c(0xA2715E), c(0xB2806C), c(0x6A4234)}));
      Tex.blit(TexArt313.DIORITE, mineralStone(419, new int[]{c(0x9E9E9E), c(0xB8B8B8), c(0xCFCFCF), c(0xE2E2E2), c(0x7C7C7C)}));
      Tex.blit(TexArt313.ANDESITE, mineralStone(420, new int[]{c(0x68686A), c(0x78787A), c(0x88888A), c(0x98989A), c(0x565658)}));
      Tex.blit(TexArt313.COARSE_DIRT, coarseDirt());
      Tex.blit(TexArt313.PODZOL_TOP, podzolTop());
      Tex.blit(TexArt313.PODZOL_SIDE, podzolSide());
      Tex.blit(TexArt313.MYCELIUM_TOP, myceliumTop());
      Tex.blit(TexArt313.MYCELIUM_SIDE, myceliumSide());
      Tex.blit(TexArt313.ICE, ice(false));
      Tex.blit(TexArt313.PACKED_ICE, ice(true));
      for (int k = 0; k < 3; k++) {
         Tex.blit(TexArt313.LEAVES0 + k, leaves(k + 1));
      }

      // Bushy leaf frond tile (Update 3.19 graphics overhaul)
      Tex.blit(BUSHY_FROND, bushyFrond());
      Tex.blit(BackroomsStalkerMod.T_ALMOND_WATER, BackroomsStalkerMod.almondWaterSprite());
      Redstone319.paint();
      TexOverhaulV57.applyAll();
      bevelItemSprites();
   }

   /** Gives all item sprites (256..1023) a directional top-left specular bevel and bottom-right shadow rim while preserving their silhouette. */
   static void bevelItemSprites() {
      boolean[] done = new boolean[Tex.COLS * Tex.COLS];
      for (int id = 256; id < Blocks.EXT0; id++) {
         if (Items.name[id] == null) {
            continue;
         }

         int tile = Items.tile[id];
         if (tile <= 0 || tile >= done.length || done[tile]) {
            continue;
         }

         done[tile] = true;
         int tx0 = Tex.tx(tile);
         int ty0 = Tex.ty(tile);
         int[] src = new int[256];
         for (int y = 0; y < 16; y++) {
            System.arraycopy(Tex.atlasPixels, (ty0 + y) * Tex.PX + tx0, src, y * 16, 16);
         }

         int[] out = src.clone();
         for (int y = 1; y < 15; y++) {
            for (int x = 1; x < 15; x++) {
               int p = src[y * 16 + x];
               if ((p >>> 24) < 128) {
                  continue;
               }

               boolean topOpen = (src[(y - 1) * 16 + x] >>> 24) < 128;
               boolean leftOpen = (src[y * 16 + (x - 1)] >>> 24) < 128;
               boolean botOpen = (src[(y + 1) * 16 + x] >>> 24) < 128;
               boolean rightOpen = (src[y * 16 + (x + 1)] >>> 24) < 128;
               if (topOpen || leftOpen || botOpen || rightOpen) {
                  // Keep dark outline pixel crisp, or slightly darken bottom-right outline
                  if (botOpen || rightOpen) {
                     out[y * 16 + x] = mul(p, 0.90F);
                  }
               } else {
                  boolean tlBorder = (src[(y - 2 >= 0 ? y - 2 : 0) * 16 + x] >>> 24) < 128 || (src[y * 16 + (x - 2 >= 0 ? x - 2 : 0)] >>> 24) < 128;
                  boolean brBorder = (src[(y + 2 < 16 ? y + 2 : 15) * 16 + x] >>> 24) < 128 || (src[y * 16 + (x + 2 < 16 ? x + 2 : 15)] >>> 24) < 128;
                  float k = 1.0F + (hash(x, y, tile + 900) - 0.5F) * 0.06F;
                  if (tlBorder && !brBorder) {
                     k += 0.10F;
                  } else if (brBorder && !tlBorder) {
                     k -= 0.08F;
                  }

                  out[y * 16 + x] = mul(p, k);
               }
            }
         }

         Tex.blit(tile, out);
      }
   }

   static int[] bedrock() {
      int[] pal = {c(0x222225), c(0x343438), c(0x4A4A50), c(0x606066), c(0x74747A)};
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float v = vn(x + 0.5F, y * 1.5F + 0.5F, 4, 110) * 0.5F + vn(x + 0.5F, y + 0.5F, 8, 111) * 0.35F + hash(x, y, 112) * 0.15F;
            p[y * N + x] = step(pal, contrast(v, 2.2F));
         }
      }

      return p;
   }

   static int[] glass() {
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

   static int[] bricks() {
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
            if (ry == 1) {
               d += 12;
            } else if (ry == 3) {
               d -= 10;
            }

            if (rx == 1) {
               d += 7;
            } else if (rx == 7) {
               d -= 7;
            }

            p[y * N + x] = rgb(baseR + d, baseG + d * 2 / 3, baseB + d / 2);
         }
      }

      return p;
   }

   static int[] sandstoneTop(boolean red) {
      int[] pal = red
         ? new int[]{c(0x9E4E1E), c(0xAC5824), c(0xB8622A), c(0xC46C32), c(0xD0783C)}
         : new int[]{c(0xC8BC8E), c(0xD3C79A), c(0xDDD2A6), c(0xE6DCB2), c(0xEEE5BE)};
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float v = fbm(x, y, red ? 415 : 220) * 0.75F + hash(x, y, red ? 416 : 221) * 0.25F;
            int col = step(pal, contrast(v, 1.9F));
            if (x == 0 || y == 0) {
               col = mul(col, 1.05F);
            } else if (x == 15 || y == 15) {
               col = mul(col, 0.93F);
            }

            p[y * N + x] = col;
         }
      }

      return p;
   }

   static int[] tallGrass() {
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
            int g = (front ? 166 : 142) + (int)(k * 42.0F) + (int)((hash(x, y, 250 + i) - 0.5F) * 12.0F);
            p[y * N + x] = rgb(g, g, g);
            if (y >= top + 2 && x + 1 < N && (front || y >= 11)) {
               int g2 = g - 14;
               if (p[y * N + x + 1] == 0) {
                  p[y * N + x + 1] = rgb(g2, g2, g2);
               }
            }
         }
      }

      return p;
   }

   static int[] flower(boolean poppy) {
      int[] p = new int[N * N];
      // Stem and leaves
      for (int y = 8; y < 16; y++) {
         int sx = y < 11 ? 7 : 8;
         p[y * N + sx] = y % 2 == 0 ? c(0x3A8828) : c(0x2E701E);
      }

      p[12 * N + 6] = c(0x3A8828);
      p[11 * N + 5] = c(0x4CA036);
      p[13 * N + 9] = c(0x3A8828);
      p[12 * N + 10] = c(0x4CA036);
      // Blossom
      for (int y = 3; y <= 8; y++) {
         for (int x = 4; x <= 11; x++) {
            float dx = x - 7.5F;
            float dy = y - 5.5F;
            float d = dx * dx + dy * dy * 1.2F;
            if (d <= 10.5F) {
               if (poppy) {
                  int col = d < 2.2F ? c(0x281812) : (dy < -0.5F ? c(0xF2382C) : (dy < 1.0F ? c(0xD42018) : c(0x9E1410)));
                  p[y * N + x] = col;
               } else {
                  int col = d < 2.0F ? c(0xF69E14) : (dy < -0.5F ? c(0xFFF048) : (dy < 1.0F ? c(0xF8D220) : c(0xD4A410)));
                  p[y * N + x] = col;
               }
            }
         }
      }

      return p;
   }

   static int[] cactusSide() {
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

   static int[] cactusTop() {
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

   static int[] torch() {
      int[] p = new int[N * N];
      for (int y = 6; y < 16; y++) {
         int cL = y == 6 ? c(0x52463A) : (y == 7 ? c(0xA67C46) : c(0x8C6230));
         int cR = y == 6 ? c(0x383028) : (y == 7 ? c(0x7C582A) : c(0x664620));
         p[y * N + 7] = add(cL, (int)((hash(7, y, 360) - 0.5F) * 10));
         p[y * N + 8] = add(cR, (int)((hash(8, y, 360) - 0.5F) * 10));
      }

      // Flame head
      p[5 * N + 7] = c(0xFF9E22);
      p[5 * N + 8] = c(0xFF8818);
      p[4 * N + 7] = c(0xFFF6B0);
      p[4 * N + 8] = c(0xFFD658);
      p[3 * N + 7] = c(0xFFE478);
      p[3 * N + 8] = c(0xFFA828);
      p[2 * N + 7] = c(0xFF8818);
      return p;
   }

   static int[] door(boolean upper) {
      int[] p = TexPaint.oakPlanks();
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
            if (y == 7 || y == 8) {
               continue;
            }

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

   static int[] farmland(boolean wet) {
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

   static int[] wheat(int stage) {
      int[] p = new int[N * N];
      int h = Math.min(15, 3 + stage * 2);
      int[] xs = {1, 3, 6, 8, 11, 14};
      for (int i = 0; i < xs.length; i++) {
         int x0 = xs[i];
         int sh = Math.max(2, h - (i & 1));
         for (int k = 0; k < sh; k++) {
            int y = 15 - k;
            float frac = k / (float)Math.max(1, sh - 1);
            int col;
            if (stage < 4) {
               col = frac > 0.6F ? c(0x6CB838) : c(0x468822);
            } else if (stage < 7) {
               col = frac > 0.55F ? c(0xB8AC38) : c(0x7A9428);
            } else {
               col = frac > 0.5F ? (k % 2 == 0 ? c(0xF0CA4E) : c(0xD4A632)) : c(0x9E8228);
            }

            p[y * N + x0] = col;
            if (stage >= 5 && frac > 0.55F && x0 + 1 < N) {
               p[y * N + x0 + 1] = mul(col, 0.88F);
            }
         }
      }

      return p;
   }

   static int[] rootCrop(boolean potato, int stage) {
      int[] p = new int[N * N];
      int h = 4 + stage * 3;
      int[] xs = {2, 5, 8, 11, 13};
      for (int i = 0; i < xs.length; i++) {
         int x0 = xs[i];
         int sh = Math.min(13, h - (i & 1));
         for (int k = 0; k < sh; k++) {
            int y = 15 - k;
            int col = k > sh * 2 / 3 ? c(0x64B836) : (k > sh / 3 ? c(0x489226) : c(0x326E18));
            if (stage == 3 && k <= 2) {
               col = potato ? c(0xC89E54) : c(0xF07818);
            }

            p[y * N + x0] = col;
            if (k >= 2 && x0 + 1 < N && (k & 1) == 0) {
               p[y * N + x0 + 1] = mul(col, 0.9F);
            }
         }
      }

      return p;
   }

   static int[] ladder() {
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

   static int[] tntSide() {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            int stick = x & 3;
            if (y >= 5 && y <= 10) {
               int w = (y == 5 || y == 10) ? c(0xC8C4BC) : c(0xF0ECE4);
               p[y * N + x] = stick == 3 ? mul(w, 0.92F) : w;
            } else {
               int r = stick == 0 ? c(0xE03C28) : (stick == 1 ? c(0xC82E1C) : (stick == 2 ? c(0xB22414) : c(0x7E180C)));
               if (y == 0 || y == 15) {
                  r = mul(r, 0.78F);
               }

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

   static int[] tntTop(boolean top) {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            int cx = (x & 3) - 1;
            int cy = (y & 3) - 1;
            boolean border = (x & 3) == 3 || (y & 3) == 3;
            int col = border ? c(0x7A1A10) : ((cx == 0 && cy == 0) ? (top ? c(0x484440) : c(0xB42818)) : c(0xD63824));
            p[y * N + x] = col;
         }
      }

      if (top) {
         for (int y = 6; y <= 9; y++) {
            for (int x = 6; x <= 9; x++) {
               p[y * N + x] = (x + y & 1) == 0 ? c(0xDCD8CE) : c(0x8E8A80);
            }
         }
      }

      return p;
   }

   static int[] haySide() {
      int[] p = hayTop();
      for (int y : new int[]{3, 4, 11, 12}) {
         for (int x = 0; x < N; x++) {
            p[y * N + x] = (y == 3 || y == 11) ? c(0xB83224) : c(0x862218);
         }
      }

      return p;
   }

   static int[] hayTop() {
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

   static int[] sugarCane() {
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

   static int[] bookshelf() {
      int[] p = TexPaint.oakPlanks();
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
            if (x + w > 15) {
               w = 15 - x;
            }

            int top = y0 + (b % 3 == 0 ? 1 : 0);
            int col = spines[b % spines.length];
            for (int y = top; y < y0 + 5; y++) {
               for (int dx = 0; dx < w; dx++) {
                  int c = dx == 0 ? mul(col, 1.12F) : (dx == w - 1 ? mul(col, 0.78F) : col);
                  if (y == top + 1) {
                     c = c(0xE0C058);
                  }

                  p[y * N + x + dx] = c;
               }
            }

            x += w;
            b++;
         }
      }

      return p;
   }

   static int[] pumpkinSide() {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            int rib = x & 3;
            int base = rib == 0 ? c(0xAC5612) : (rib == 1 ? c(0xE47E1E) : (rib == 2 ? c(0xD46E18) : c(0xC05E14)));
            float v = (fbm(x, y, 222) - 0.5F) * 0.16F;
            if (y == 0 || y == 15) {
               v -= 0.08F;
            }

            p[y * N + x] = mul(base, 1.0F + v);
         }
      }

      return p;
   }

   static int[] pumpkinTop() {
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

   static int[] pumpkinFace(boolean lit) {
      int[] p = pumpkinSide();
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

   static int[] melonSide() {
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

   static int[] melonTop() {
      int[] p = melonSide();
      p[7 * N + 7] = p[7 * N + 8] = p[8 * N + 7] = p[8 * N + 8] = c(0x9EB850);
      return p;
   }

   static int[] cakeTop() {
      int[] p = new int[N * N];
      for (int y = 1; y < 15; y++) {
         for (int x = 1; x < 15; x++) {
            boolean rim = x == 1 || x == 14 || y == 1 || y == 14;
            int col = rim ? c(0xD8CFC4) : c(0xF8F4EE);
            p[y * N + x] = add(col, (int)((hash(x, y, 230) - 0.5F) * 8));
         }
      }

      int[][] berries = {{4, 4}, {11, 4}, {7, 7}, {8, 8}, {4, 11}, {11, 11}};
      for (int[] b : berries) {
         p[b[1] * N + b[0]] = c(0xD82420);
         p[b[1] * N + b[0] + 1] = c(0x9E1614);
      }

      return p;
   }

   static int[] cakeSide(boolean inner) {
      int[] p = new int[N * N];
      for (int y = 8; y < 16; y++) {
         for (int x = 1; x < 15; x++) {
            if (!inner && (y < 10 || (y == 10 && ((x + 1) % 3 != 0)))) {
               p[y * N + x] = y == 8 ? c(0xF8F4EE) : c(0xE0D8CE);
            } else if (y == 8) {
               p[y * N + x] = c(0xF8F4EE);
            } else if (inner && y == 12) {
               p[y * N + x] = c(0xC82822);
            } else {
               int sponge = y == 15 ? c(0x8E5624) : c(0xC88642);
               p[y * N + x] = add(sponge, (int)((hash(x, y, 231) - 0.5F) * 14));
            }
         }
      }

      return p;
   }

   static int[] cakeBottom() {
      int[] p = new int[N * N];
      for (int y = 1; y < 15; y++) {
         for (int x = 1; x < 15; x++) {
            p[y * N + x] = add(c(0x985E28), (int)((hash(x, y, 233) - 0.5F) * 12));
         }
      }

      return p;
   }

   static int[] metalBlock(int mid, int dark, int deep, int hi, int seed) {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            boolean border = x == 0 || y == 0 || x == 15 || y == 15;
            if (border) {
               p[y * N + x] = (x == 0 || y == 0) ? c(hi) : c(deep);
               continue;
            }

            float sheen = (float)Math.sin((x + y) * 0.45F) * 0.5F + 0.5F;
            int col = sheen > 0.72F ? c(hi) : (sheen > 0.35F ? c(mid) : c(dark));
            if (x == 1 || y == 1) {
               col = mix(col, c(hi), 0.45F);
            } else if (x == 14 || y == 14) {
               col = mix(col, c(deep), 0.4F);
            }

            p[y * N + x] = add(col, (int)((hash(x, y, seed) - 0.5F) * 8));
         }
      }

      return p;
   }

   static int[] gemBlock(int mid, int dark, int deep, int hi, int seed) {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            int dx = Math.min(x, 15 - x);
            int dy = Math.min(y, 15 - y);
            int ring = Math.min(dx, dy);
            int col;
            if (ring == 0) {
               col = (x == 0 || y == 0) ? c(hi) : c(deep);
            } else if (ring <= 2) {
               col = (x + y < 15) ? c(mid) : c(dark);
            } else if (ring == 3) {
               col = (x <= 3 || y <= 3) ? c(deep) : c(hi);
            } else {
               col = (x + y < 14) ? c(hi) : ((x + y < 17) ? c(mid) : c(dark));
            }

            p[y * N + x] = add(col, (int)((hash(x, y, seed) - 0.5F) * 8));
         }
      }

      return p;
   }

   static int[] coalBlock() {
      int[] pal = {c(0x101012), c(0x18181C), c(0x222228), c(0x2E2E36), c(0x3A3A44)};
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float v = fbm(x, y, 218) * 0.7F + hash(x, y, 2180) * 0.3F;
            p[y * N + x] = step(pal, contrast(v, 2.1F));
         }
      }

      return p;
   }

   static int[] lapisBlock() {
      int[] pal = {c(0x162E7A), c(0x1E4098), c(0x2852B4), c(0x3866CC), c(0x5282E0)};
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float v = fbm(x, y, 219) * 0.7F + hash(x, y, 2190) * 0.3F;
            int col = step(pal, contrast(v, 2.0F));
            if (hash(x, y, 2191) > 0.92F) {
               col = c(0xD8B448); // golden pyrite flecks in lapis lazuli
            }

            p[y * N + x] = col;
         }
      }

      return p;
   }

   static int[] netherrack() {
      int[] pal = {c(0x461818), c(0x5A2020), c(0x6E2A2A), c(0x823636), c(0x964444)};
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float v = fbm(x, y, 300) * 0.68F + hash(x, y, 3001) * 0.32F;
            p[y * N + x] = step(pal, contrast(v, 2.2F));
         }
      }

      return p;
   }

   static int[] soulSand() {
      int[] pal = {c(0x32241C), c(0x423026), c(0x523C30), c(0x644A3C), c(0x745848)};
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float v = fbm(x, y, 301) * 0.75F + hash(x, y, 3011) * 0.25F;
            p[y * N + x] = step(pal, contrast(v, 2.1F));
         }
      }

      return p;
   }

   static int[] quartzOre() {
      int[] p = netherrack();
      int[][] shards = {{3, 3}, {4, 3}, {4, 4}, {10, 4}, {11, 5}, {12, 5}, {5, 10}, {6, 10}, {6, 11}, {11, 11}, {12, 12}};
      for (int[] s : shards) {
         p[s[1] * N + s[0]] = (s[0] + s[1] & 1) == 0 ? c(0xF6F2EA) : c(0xD4C8BC);
      }

      return p;
   }

   static int[] emeraldOre() {
      int[] p = TexPaint.stone();
      int[][] gems = {{4, 3}, {11, 4}, {7, 7}, {3, 10}, {12, 11}, {8, 13}};
      for (int[] g : gems) {
         p[g[1] * N + g[0]] = c(0x48F078);
         p[(g[1] + 1) * N + g[0]] = c(0x169E42);
      }

      return p;
   }

   static int[] netherBricks() {
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

   static int[] magma() {
      int[] crustPal = {c(0x221014), c(0x2E151B), c(0x3C1C23), c(0x4C242C), c(0x5A2C35)};
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float wx = x + 0.5F + (vn(x + 0.5F, y + 0.5F, 4, 3040) - 0.5F) * 3.2F;
            float wy = y + 0.5F + (vn(x + 0.5F, y + 0.5F, 4, 3041) - 0.5F) * 3.2F;
            float r1 = Math.abs(vn(wx, wy, 4, 3042) - 0.50F);
            float r2 = Math.abs(vn(wx + 4.3F, wy + 2.7F, 4, 3043) - 0.48F);
            float fissure = Math.min(r1, r2 * 1.08F);
            // Secondary hot pocket veins so every quadrant has interconnected glowing magma
            float pocket = vn(x + 0.5F, y + 0.5F, 4, 3044);
            if (pocket > 0.72F) {
               fissure = Math.min(fissure, (0.86F - pocket) * 0.75F);
            }

            if (fissure < 0.042F) {
               p[y * N + x] = c(0xFFF488); // white-hot yellow magma core
            } else if (fissure < 0.082F) {
               p[y * N + x] = c(0xFFA61A); // bright molten orange
            } else if (fissure < 0.125F) {
               p[y * N + x] = c(0xE0440A); // fiery red-orange lava
            } else if (fissure < 0.165F) {
               p[y * N + x] = c(0x8E2012); // glowing heated crust rim
            } else {
               float cv = fbm(x, y, 3045) * 0.72F + hash(x, y, 3046) * 0.28F;
               p[y * N + x] = step(crustPal, contrast(cv, 2.1F));
            }
         }
      }

      return p;
   }

   static int[] quartzBlock(boolean top) {
      int[] pal = {c(0xDCD6CE), c(0xE6E1DA), c(0xEFECE6), c(0xF7F5F0)};
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float v = fbm(x, y, top ? 306 : 305);
            int col = step(pal, contrast(v, 1.8F));
            if (x == 0 || y == 0) {
               col = c(0xFBF9F5);
            } else if (x == 15 || y == 15) {
               col = c(0xCECAC0);
            }

            p[y * N + x] = col;
         }
      }

      return p;
   }

   static int[] endStone() {
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

   static int[] endFrameTop() {
      int[] p = endStone();
      for (int y = 2; y < 14; y++) {
         for (int x = 2; x < 14; x++) {
            if (x >= 5 && x <= 10 && y >= 5 && y <= 10) {
               p[y * N + x] = c(0x121A18);
            } else if (x <= 3 || x >= 12 || y <= 3 || y >= 12) {
               p[y * N + x] = ((x + y) & 1) == 0 ? c(0x367868) : c(0x24564A);
            }
         }
      }

      return p;
   }

   static int[] endFrameSide() {
      int[] p = endStone();
      for (int y = 0; y < 5; y++) {
         for (int x = 0; x < N; x++) {
            p[y * N + x] = (y == 0 || y == 4) ? c(0x24564A) : (((x / 2 + y) & 1) == 0 ? c(0x3E8876) : c(0x2C6658));
         }
      }

      return p;
   }

   static int[] dragonEgg() {
      int[] pal = {c(0x0A0610), c(0x140C20), c(0x201234), c(0x321C50), c(0x4A2874)};
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float v = fbm(x, y, 313) * 0.75F + hash(x, y, 3130) * 0.25F;
            p[y * N + x] = step(pal, contrast(v, 2.2F));
         }
      }

      return p;
   }

   static int[] clay() {
      int[] pal = {c(0x8E94A2), c(0x989EAC), c(0xA4AAB8), c(0xB0B6C4)};
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            p[y * N + x] = step(pal, contrast(fbm(x, y, 379), 1.9F));
         }
      }

      return p;
   }

   static int[] concrete(int baseRgb, int seed) {
      int base = c(baseRgb);
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float d = (fbm(x, y, seed) - 0.5F) * 0.06F + (hash(x, y, seed + 50) - 0.5F) * 0.03F;
            p[y * N + x] = mul(base, 1.0F + d);
         }
      }

      return p;
   }

   static int[] powder(int baseRgb, int seed) {
      int base = c(baseRgb);
      int[] pal = {mul(base, 0.90F), mul(base, 0.96F), base, mul(base, 1.05F), mul(base, 1.10F)};
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float v = fbm(x, y, seed) * 0.55F + hash(x, y, seed + 99) * 0.45F;
            p[y * N + x] = step(pal, contrast(v, 2.0F));
         }
      }

      return p;
   }

   static int[] terracotta(int baseRgb, int seed) {
      int base = c(baseRgb);
      int[] pal = {mul(base, 0.91F), mul(base, 0.97F), base, mul(base, 1.04F)};
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float v = vn(x + 0.5F, y * 2.0F + 0.5F, 4, seed) * 0.7F + hash(x, y, seed + 33) * 0.3F;
            p[y * N + x] = step(pal, contrast(v, 1.9F));
         }
      }

      return p;
   }

   static int[] bark(int seed, int c0, int c1, int c2, int c3) {
      int[] pal = {c0, c1, c2, c3};
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float v = vn(x * 2.0F + 0.5F, y * 0.5F + 0.5F, 4, seed) * 0.65F + hash(x, y, seed + 1) * 0.35F;
            p[y * N + x] = step(pal, contrast(v, 2.1F));
         }
      }

      return p;
   }

   static int[] logTop(int barkCol, int ringDark, int ringLight) {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            int d = Math.max(Math.abs(x - 7 - (x >= 8 ? 1 : 0)), Math.abs(y - 7 - (y >= 8 ? 1 : 0)));
            int col = d >= 7 ? barkCol : ((d & 1) == 0 ? ringLight : ringDark);
            p[y * N + x] = add(col, (int)((hash(x, y, 405) - 0.5F) * 8));
         }
      }

      return p;
   }

   static int[] planks(int seed, int c0, int c1, int c2, int c3, int seam) {
      int[] pal = {c0, c1, c2, c3};
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         int row = y / 4;
         int ry = y % 4;
         int joint = (row * 5 + 3) & 15;
         for (int x = 0; x < N; x++) {
            if (ry == 3 || x == joint) {
               p[y * N + x] = seam;
               continue;
            }

            float v = vn(x * 0.5F + 0.5F, y + 0.5F, 4, seed + row) * 0.7F + hash(x, y, seed + 9) * 0.3F;
            int col = step(pal, contrast(v, 1.9F));
            if (ry == 0) {
               col = mul(col, 1.05F);
            }

            p[y * N + x] = col;
         }
      }

      return p;
   }

   static int[] carvedSandstone(boolean red) {
      int[] p = smoothSandstone(red);
      int dark = red ? c(0x7E3812) : c(0x9E8E64);
      int hi = red ? c(0xD87E3E) : c(0xEEE6BE);
      for (int x = 0; x < N; x++) {
         p[2 * N + x] = dark;
         p[3 * N + x] = hi;
         p[13 * N + x] = dark;
      }

      for (int y = 5; y <= 11; y++) {
         for (int x = 3; x <= 12; x++) {
            boolean groove = (y == 5 || y == 11 || x == 3 || x == 12) || (y == 7 && (x == 5 || x == 6 || x == 9 || x == 10)) || (y >= 9 && x >= 6 && x <= 9);
            if (groove) {
               p[y * N + x] = dark;
            }
         }
      }

      return p;
   }

   static int[] smoothSandstone(boolean red) {
      int[] p = sandstoneTop(red);
      int topC = red ? c(0xD47A3C) : c(0xEAE2BA);
      int botC = red ? c(0x883E14) : c(0xA89A70);
      for (int x = 0; x < N; x++) {
         p[x] = topC;
         p[15 * N + x] = botC;
      }

      return p;
   }

   static int[] redSand() {
      int[] pal = {c(0x9E4C1A), c(0xAC5620), c(0xBA6026), c(0xC86C2E), c(0xD47836)};
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float v = vn(x + 0.5F, y * 1.4F + 0.5F, 4, 413) * 0.65F + hash(x, y, 4130) * 0.35F;
            p[y * N + x] = step(pal, contrast(v, 2.0F));
         }
      }

      return p;
   }

   static int[] redSandstoneSide() {
      int[] pal = {c(0x924416), c(0xA4501E), c(0xB45C24), c(0xC2682C), c(0xCE7434)};
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float band = y < 3 ? 0.78F : (y == 3 ? 0.25F : (y == 9 ? 0.30F : 0.52F));
            float v = band * 0.65F + fbm(x, y, 414) * 0.35F;
            p[y * N + x] = step(pal, contrast(v, 2.0F));
         }
      }

      return p;
   }

   static int[] mineralStone(int seed, int[] pal) {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float v = fbm(x, y, seed) * 0.65F + hash(x, y, seed + 7) * 0.35F;
            p[y * N + x] = step(pal, contrast(v, 2.2F));
         }
      }

      return p;
   }

   static int[] coarseDirt() {
      int[] p = TexPaint.dirt();
      Random r = new Random(421L);
      for (int i = 0; i < 36; i++) {
         int x = r.nextInt(N);
         int y = r.nextInt(N);
         p[y * N + x] = (i & 1) == 0 ? c(0x5A402A) : c(0x96826E);
      }

      return p;
   }

   static int[] podzolTop() {
      int[] pal = {c(0x3E2814), c(0x4E331A), c(0x5E3E20), c(0x704C28), c(0x825A30)};
      return mineralStone(422, pal);
   }

   static int[] podzolSide() {
      int[] p = TexPaint.dirt();
      int[] top = podzolTop();
      for (int x = 0; x < N; x++) {
         int d = 3 + ((int)(hash(x, 0, 423) * 3.0F));
         for (int y = 0; y < d; y++) {
            p[y * N + x] = y == d - 1 ? mul(top[y * N + x], 0.82F) : top[y * N + x];
         }
      }

      return p;
   }

   static int[] myceliumTop() {
      int[] pal = {c(0x544852), c(0x645662), c(0x726470), c(0x80727E), c(0x8E808C)};
      return mineralStone(424, pal);
   }

   static int[] myceliumSide() {
      int[] p = TexPaint.dirt();
      int[] top = myceliumTop();
      for (int x = 0; x < N; x++) {
         int d = 3 + ((int)(hash(x, 0, 425) * 3.0F));
         for (int y = 0; y < d; y++) {
            p[y * N + x] = y == d - 1 ? mul(top[y * N + x], 0.82F) : top[y * N + x];
         }
      }

      return p;
   }

   static int[] ice(boolean packed) {
      int[] pal = packed
         ? new int[]{c(0x6E98E0), c(0x7EA8EC), c(0x90B8F6), c(0xA4C8FF)}
         : new int[]{c(0x74A8F8), c(0x86B6FF), c(0x98C4FF), c(0xB0D4FF)};
      int[] p = mineralStone(packed ? 427 : 426, pal);
      for (int k = 2; k < 13; k++) {
         p[k * N + (14 - k)] = c(0xC8E2FF);
      }

      return p;
   }

   static int[] gravelRoad() {
      int[] pal = {c(0x66605A), c(0x78726B), c(0x8A847C), c(0x9C968E), c(0xAEA8A0)};
      return mineralStone(445, pal);
   }

   /**
    * Update 3.19: crisp pixel-art overlapping leaflet clusters for Oak/Spruce/Birch (kind 0), Jungle (kind 1), Acacia (kind 2), and Dark Oak (kind 3).
    * Painted in greyscale (average ~160) so the mesher tints it with the exact biome/species foliage colour.
    */
   static int[] leaves(int kind) {
      int[] p = new int[N * N];
      int seed = 5500 + kind * 97;
      // 1. Deep shaded interior canopy base with small organic sky gaps
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float g = vn(x + 0.5F, y + 0.5F, 4, seed) * 0.65F + hash(x, y, seed + 1) * 0.35F;
            if (g < 0.22F && ((x + y + kind) & 1) == 0) {
               p[y * N + x] = 0;
            } else {
               int d = 108 + (int)(hash(x, y, seed + 2) * 18.0F);
               p[y * N + x] = rgb(d, d, d);
            }
         }
      }

      // 2. Hand-layered overlapping 4x3 / 3x4 pixel-art leaf blades tiled seamlessly modulo 16
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
         paintLeaflet(p, cx, cy, dir, true, seed + i * 13);
      }

      return p;
   }

   private static void paintLeaflet(int[] p, int cx, int cy, int dir, boolean wrap, int seed) {
      // Cast shadow under the lower-right rim of the leaflet
      int[][] shadow = dir == 0
         ? new int[][]{{1, 2}, {2, 2}, {3, 1}}
         : new int[][]{{1, 3}, {2, 2}, {2, 1}};
      for (int[] s : shadow) {
         int x = wrap ? (cx + s[0]) & 15 : cx + s[0];
         int y = wrap ? (cy + s[1]) & 15 : cy + s[1];
         if (x >= 0 && x < N && y >= 0 && y < N && p[y * N + x] != 0) {
            int sh = 98 + (int)(hash(x, y, seed) * 12.0F);
            p[y * N + x] = rgb(sh, sh, sh);
         }
      }

      // Leaflet blade pixels: {dx, dy, tone} where tone 0 = shaded stem/rim, 1 = mid blade, 2 = sunlit top-left edge, 3 = bright tip
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
         int x = wrap ? (cx + b[0]) & 15 : cx + b[0];
         int y = wrap ? (cy + b[1]) & 15 : cy + b[1];
         if (x >= 0 && x < N && y >= 0 && y < N) {
            int jit = (int)((hash(x, y, seed + 7) - 0.5F) * 10.0F);
            int v = b[2] == 3 ? 206 + jit : (b[2] == 2 ? 186 + jit : (b[2] == 1 ? 160 + jit : 134 + jit));
            p[y * N + x] = rgb(v, v, v);
         }
      }
   }

   /**
    * Update 3.19: 16 x 16 lush multi-leaflet foliage branch spray used by the 3D fronds of {@link Mesher#bushyLeaves}.
    * Painted in greyscale with the same leaflet clusters as {@link #leaves(int)} so the 3D fronds and the leaf cube blend seamlessly.
    */
   static int[] bushyFrond() {
      int[] p = new int[N * N];
      // 1. Broad scalloped foliage silhouette extending out to the edges of the 16x16 tile so fronds protrude lushly beyond the block
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float dx = (x + 0.5F - 8.0F) / 8.0F;
            float dy = (y + 0.5F - 8.0F) / 8.0F;
            double ang = Math.atan2(dy, dx);
            float r = (float)Math.hypot(dx, dy);
            float lobe = 0.94F + 0.14F * (float)Math.cos(ang * 5.0 + 0.4) + 0.10F * (float)Math.sin(ang * 7.0);
            if (r <= lobe) {
               // Small organic airy gaps inside the branch spray
               if (r > 0.35F && hash(x, y, 6425) < 0.14F) {
                  continue;
               }

               int base = (int)(132.0F - dy * 16.0F + (hash(x, y, 6420) - 0.5F) * 18.0F);
               p[y * N + x] = rgb(base, base, base);
            }
         }
      }

      // 2. Overlapping pixel-art leaflets radiating from the core out to the branch tips
      int[][] sprays = {
         {2, 2, 0}, {6, 1, 1}, {10, 1, 0}, {12, 3, 1},
         {1, 6, 1}, {5, 5, 0}, {9, 4, 1}, {12, 7, 0},
         {1, 10, 0}, {4, 9, 1}, {8, 8, 0}, {11, 10, 1},
         {3, 12, 0}, {7, 12, 1}, {10, 12, 0}, {6, 6, 1}
      };
      for (int i = 0; i < sprays.length; i++) {
         paintLeaflet(p, sprays[i][0], sprays[i][1], sprays[i][2], false, 6430 + i * 17);
      }

      return p;
   }
}
