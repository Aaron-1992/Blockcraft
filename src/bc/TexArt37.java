package bc;

import java.util.Random;

/**
 * Procedural art of Update 3.7 (atlas tiles 219-245): sugar cane, bookshelf, pumpkins, melon, stems, cake, fire
 * and the item icons of sugar / paper / book / milk / egg / seeds / melon slice / pie / cake.
 * Like all other Blockcraft textures it is drawn by code, so no image files are needed.
 * Preview it with bc.TexSheet (no OpenGL required).
 *
 *   219 sugar cane          220 bookshelf side       221 pumpkin top        222 pumpkin side
 *   223 carved face         224 jack o'lantern face  225 melon top          226 melon side
 *   227-230 stem (4 growth groups, stage / 2)         231-234 cake top / side / bottom / cut face
 *   235 fire (16 animation frames, see Tex.animate)
 *   236 sugar  237 paper  238 book  239 milk bucket  240 egg  241 pumpkin seeds  242 melon seeds
 *   243 melon slice  244 pumpkin pie  245 cake
 */
final class TexArt37 {
   /** The 16 animation frames of the fire tile (tile 235). */
   static final int[][] fireFrames = new int[16][];

   static int rgb(int r, int g, int b) {
      return Tex.rgb(r, g, b);
   }

   static int tone(int c, float f) {
      return TexExtra.sc(c & 0xFFFFFF, f);
   }

   /** Adds a little deterministic noise to every opaque pixel. */
   static void noise(int[] p, long seed, int amp) {
      Random r = new Random(seed);
      for (int i = 0; i < p.length; i++) {
         if (p[i] >>> 24 != 0) {
            int n = r.nextInt(amp * 2 + 1) - amp;
            int c = p[i];
            p[i] = rgb((c >> 16 & 0xFF) + n, (c >> 8 & 0xFF) + n, (c & 0xFF) + n);
         }
      }
   }

   /** Blits every new block tile into the atlas. Called from TexExtra.blocks(). */
   static void blocks() {
      Tex.blit(219, cane());
      Tex.blit(220, bookshelfSide());
      Tex.blit(221, pumpkinTop());
      Tex.blit(222, pumpkinSide());
      Tex.blit(223, pumpkinFace(false));
      Tex.blit(224, pumpkinFace(true));
      Tex.blit(225, melonTop());
      Tex.blit(226, melonSide());
      for (int g = 0; g < 4; g++) {
         Tex.blit(227 + g, stem(g));
      }

      Tex.blit(231, cakeTop());
      Tex.blit(232, cakeSide(false));
      Tex.blit(233, cakeBottom());
      Tex.blit(234, cakeSide(true));
      for (int f = 0; f < 16; f++) {
         fireFrames[f] = fireFrame(f);
      }

      Tex.blit(235, fireFrames[0]);
   }

   // ------------------------------------------------------------------ plants

   static int[] cane() {
      TexExtra.Cv cv = new TexExtra.Cv();
      int light = rgb(158, 208, 100);
      int mid = rgb(116, 172, 72);
      int dark = rgb(72, 128, 46);
      int pale = rgb(200, 230, 154);
      int[] xs = new int[]{2, 7, 12};
      int[] phase = new int[]{3, 0, 4};

      for (int i = 0; i < 3; i++) {
         int x = xs[i];
         for (int y = 0; y < 16; y++) {
            int k = (y + phase[i]) % 5;
            cv.set(x, y, k == 1 ? pale : light);
            cv.set(x + 1, y, mid);
            if (k == 0) {
               cv.set(x, y, dark);
               cv.set(x + 1, y, dark);
               if (y >= 2) {
                  cv.set(x - 1, y - 1, mid);
                  cv.set(x - 2, y - 2, light);
                  cv.set(x + 2, y - 1, mid);
                  cv.set(x + 3, y - 2, light);
               }
            }
         }
      }

      return cv.p;
   }

   /** Pumpkin / melon stem for growth group 0..3 (stage / 2): a short sprout up to a tall, yellowing stalk. */
   static int[] stem(int g) {
      TexExtra.Cv cv = new TexExtra.Cv();
      int h = new int[]{3, 7, 11, 15}[g];
      int dark = g < 3 ? rgb(66, 134, 44) : rgb(150, 124, 52);
      int light = g < 3 ? rgb(118, 192, 76) : rgb(196, 170, 76);
      int leaf = g < 3 ? rgb(94, 168, 60) : rgb(172, 148, 66);

      for (int i = 0; i < h; i++) {
         int y = 15 - i;
         int x = 7 + (g >= 2 && i >= h - 3 ? 1 : 0);
         cv.set(x, y, dark);
         cv.set(x + 1, y, light);
      }

      if (g >= 1) {
         int y = 15 - h / 2;
         cv.set(5, y, leaf);
         cv.set(6, y + 1, leaf);
         cv.set(4, y - 1, light);
         cv.set(10, y - 1, leaf);
         cv.set(9, y, leaf);
         cv.set(11, y - 2, light);
      }

      if (g >= 2) {
         cv.set(5, 15 - h + 3, leaf);
         cv.set(4, 15 - h + 2, light);
         cv.set(10, 15 - h + 4, leaf);
      }

      if (g == 3) {
         cv.set(8, 15 - h, rgb(120, 96, 40));
         cv.set(9, 15 - h + 1, rgb(120, 96, 40));
      }

      return cv.p;
   }

   // ------------------------------------------------------------------ pumpkins and melons

   static int[] pumpkinTop() {
      TexExtra.Cv cv = new TexExtra.Cv();
      int base = rgb(226, 116, 22);
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            int d = (int)Math.max(Math.abs(x - 7.5), Math.abs(y - 7.5));
            int c = base;
            if (d == 3 || d == 6) {
               c = tone(base, 0.82F);
            } else if (d >= 7) {
               c = tone(base, 0.7F);
            }

            cv.set(x, y, c);
         }
      }

      cv.rect(6, 6, 4, 4, tone(base, 0.62F));
      cv.rect(7, 7, 2, 2, rgb(94, 118, 42));
      cv.set(7, 7, rgb(120, 146, 56));
      noise(cv.p, 221L, 7);
      return cv.p;
   }

   static int[] pumpkinSide() {
      TexExtra.Cv cv = new TexExtra.Cv();
      int base = rgb(224, 114, 20);
      int[] rib = new int[]{0, 2, 1, 1, 2, 0, 2, 1, 1, 2, 0, 2, 1, 1, 2, 0};
      float[] f = new float[]{0.66F, 0.9F, 1.06F};
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            float k = f[rib[x] == 0 ? 0 : (rib[x] == 2 ? 1 : 2)];
            if (y == 0 || y == 15) {
               k *= 0.88F;
            }

            cv.set(x, y, tone(base, k));
         }
      }

      noise(cv.p, 222L, 6);
      return cv.p;
   }

   static final String[] FACE = new String[]{
      "................",
      "................",
      "................",
      "................",
      ".....#....#.....",
      "....###..###....",
      "...####..####...",
      "................",
      ".......##.......",
      "......####......",
      "................",
      "..##........##..",
      "..####.##.####..",
      "...############.",
      "....##.####.##..",
      "................"
   };

   /** The carved side: dark hollow eyes, nose and a jagged mouth; lit = the glowing face of a jack o'lantern. */
   static int[] pumpkinFace(boolean lit) {
      int[] base = pumpkinSide();
      boolean[] hole = new boolean[256];
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            hole[y * 16 + x] = FACE[y].charAt(x) == '#';
         }
      }

      int[] out = (int[])base.clone();
      Random rnd = new Random(lit ? 224L : 223L);
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            int i = y * 16 + x;
            if (hole[i]) {
               if (lit) {
                  int k = rnd.nextInt(3);
                  out[i] = k == 0 ? rgb(255, 232, 110) : (k == 1 ? rgb(255, 214, 72) : rgb(255, 196, 52));
               } else {
                  int k = rnd.nextInt(3);
                  out[i] = rgb(30 + k * 4, 18 + k * 3, 8 + k * 2);
               }
            } else {
               boolean near = x > 0 && hole[i - 1] || x < 15 && hole[i + 1] || y > 0 && hole[i - 16] || y < 15 && hole[i + 16];
               if (near) {
                  out[i] = lit ? rgb(250, 150, 26) : tone(base[i], 0.7F);
               }
            }
         }
      }

      return out;
   }

   static int[] melonTop() {
      TexExtra.Cv cv = new TexExtra.Cv();
      int base = rgb(114, 168, 46);
      int dark = rgb(72, 122, 30);
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            double dx = x - 7.5;
            double dy = y - 7.5;
            double ang = Math.atan2(dy, dx);
            int band = (int)Math.floor((ang + Math.PI) / (Math.PI * 2) * 8.0);
            int c = band % 2 == 0 ? base : tone(base, 1.08F);
            if (Math.abs(Math.sin(ang * 4.0)) < 0.16 && Math.hypot(dx, dy) > 2.0) {
               c = dark;
            }

            cv.set(x, y, c);
         }
      }

      cv.rect(6, 6, 4, 4, rgb(156, 150, 76));
      cv.rect(7, 7, 2, 2, rgb(104, 92, 44));
      noise(cv.p, 225L, 6);
      return cv.p;
   }

   static int[] melonSide() {
      TexExtra.Cv cv = new TexExtra.Cv();
      Random rnd = new Random(226L);
      int light = rgb(128, 180, 54);
      int dark = rgb(62, 108, 28);
      int mid = rgb(96, 148, 40);
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            int wob = (int)Math.round(Math.sin(y * 0.55 + x * 0.1) * 0.7);
            int col = (x + wob + 16) % 16;
            int c = col % 4 == 0 ? dark : (col % 4 == 1 ? mid : light);
            if (col % 4 == 2 && rnd.nextInt(7) == 0) {
               c = tone(light, 1.12F);
            }

            cv.set(x, y, c);
         }
      }

      noise(cv.p, 227L, 5);
      return cv.p;
   }

   // ------------------------------------------------------------------ bookshelf

   static int[] bookshelfSide() {
      int[] planks = Tex.tPlanks();
      TexExtra.Cv cv = new TexExtra.Cv();
      Random rnd = new Random(220L);
      int[] cols = new int[]{rgb(150, 44, 40), rgb(44, 74, 146), rgb(62, 124, 64), rgb(176, 134, 52), rgb(112, 62, 124), rgb(184, 92, 44), rgb(56, 56, 66), rgb(210, 200, 170)};
      int shadow = rgb(38, 24, 14);
      for (int y = 0; y < 16; y++) {
         boolean plank = y <= 1 || y == 7 || y == 8 || y >= 14;
         for (int x = 0; x < 16; x++) {
            cv.set(x, y, plank ? planks[y * 16 + x] : shadow);
         }
      }

      for (int[] shelf : new int[][]{{2, 6}, {9, 13}}) {
         int x = 0;
         while (x < 16) {
            int w = 2 + rnd.nextInt(2);
            if (x + w > 16) {
               w = 16 - x;
            }

            int c = cols[rnd.nextInt(cols.length)];
            int top = shelf[0] + (rnd.nextInt(3) == 0 ? 1 : 0);
            for (int y = top; y <= shelf[1]; y++) {
               for (int i = 0; i < w; i++) {
                  int cc = i == 0 ? tone(c, 1.2F) : (i == w - 1 ? tone(c, 0.7F) : c);
                  cv.set(x + i, y, cc);
               }
            }

            int band = shelf[0] + 2 + rnd.nextInt(2);
            cv.rect(x, band, w, 1, tone(c, 1.35F));
            x += w;
         }
      }

      return cv.p;
   }

   // ------------------------------------------------------------------ cake

   static int[] cakeTop() {
      TexExtra.Cv cv = new TexExtra.Cv();
      int cream = rgb(250, 244, 236);
      int edge = rgb(232, 206, 196);
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            boolean border = x <= 1 || y <= 1 || x >= 14 || y >= 14;
            cv.set(x, y, border ? edge : cream);
         }
      }

      int red = rgb(204, 36, 48);
      int redLight = rgb(240, 96, 104);
      for (int[] b : new int[][]{{3, 3}, {11, 3}, {3, 11}, {11, 11}, {7, 7}}) {
         cv.rect(b[0], b[1], 2, 2, red);
         cv.set(b[0], b[1], redLight);
      }

      noise(cv.p, 231L, 3);
      return cv.p;
   }

   /** Side of the cake; the 8 px high cake uses the lower half of the tile. inner = the cut face of a bitten cake. */
   static int[] cakeSide(boolean inner) {
      TexExtra.Cv cv = new TexExtra.Cv();
      Random rnd = new Random(inner ? 234L : 232L);
      int sponge = rgb(152, 94, 52);
      int spongeDark = rgb(124, 74, 40);
      int frosting = rgb(250, 244, 236);
      int frostEdge = rgb(230, 214, 200);
      int filling = rgb(246, 234, 206);
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            int c = sponge;
            if (y >= 8 && y <= 9) {
               c = frosting;
            } else if (y == 10) {
               c = frostEdge;
            } else if (y == 12 || y == 13 && inner) {
               c = filling;
            } else if (y >= 15) {
               c = spongeDark;
            }

            if (!inner && y == 8) {
               c = frostEdge;
            }

            if (inner && c == sponge && rnd.nextInt(5) == 0) {
               c = spongeDark;
            }

            cv.set(x, y, c);
         }
      }

      if (!inner) {
         int[] drip = new int[]{2, 6, 11, 14};
         for (int x : drip) {
            cv.set(x, 11, frosting);
         }
      }

      noise(cv.p, inner ? 236L : 235L, 4);
      return cv.p;
   }

   static int[] cakeBottom() {
      TexExtra.Cv cv = new TexExtra.Cv();
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            boolean border = x <= 1 || y <= 1 || x >= 14 || y >= 14;
            cv.set(x, y, border ? rgb(118, 70, 38) : rgb(146, 90, 50));
         }
      }

      noise(cv.p, 233L, 5);
      return cv.p;
   }

   // ------------------------------------------------------------------ fire

   /** One frame of the flame animation: flickering tongues of flame on a transparent tile (alpha is 0 or 255). */
   static int[] fireFrame(int f) {
      double t = f / 16.0 * Math.PI * 2.0;
      int[] p = new int[256];
      for (int x = 0; x < 16; x++) {
         double cx = Math.abs(x - 7.5) / 8.0;
         double hgt = 9.0 + 3.6 * Math.sin(x * 0.9 + t) + 2.6 * Math.sin(x * 1.7 - 2.0 * t + 1.3) + 1.6 * Math.sin(x * 2.9 + 3.0 * t + 0.4);
         hgt *= 1.0 - cx * cx * 0.6;
         hgt = Math.max(3.0, Math.min(16.0, hgt));
         for (int y = 0; y < 16; y++) {
            int dy = 15 - y;
            if (dy >= hgt) {
               continue;
            }

            double k = dy / hgt;
            double flick = 0.5 + 0.5 * Math.sin(x * 2.3 + y * 1.7 + 2.0 * t + Math.sin(y * 0.9 - t) * 1.5);
            double heat = 1.0 - k * 0.85 - (1.0 - flick) * 0.22;
            if (heat < 0.08) {
               continue;
            }

            int r = 255;
            int g = (int)(60 + heat * 190);
            int b = (int)(heat > 0.78 ? (heat - 0.78) * 520 : 0);
            if (k > 0.8 && flick < 0.45) {
               continue; // ragged tips
            }

            p[y * 16 + x] = rgb(r, Math.min(g, 245), Math.min(b, 170));
         }
      }

      return p;
   }

   // ------------------------------------------------------------------ item icons (341-350)

   static int[] sprite(int id) {
      TexExtra.Cv cv = new TexExtra.Cv();
      switch (id) {
         case 341:
            seeds(cv, rgb(232, 220, 186), rgb(168, 150, 110), rgb(255, 250, 230));
            break;
         case 342:
            seeds(cv, rgb(44, 40, 40), rgb(14, 12, 12), rgb(110, 104, 104));
            break;
         case 343:
            melonSlice(cv);
            break;
         case 344:
            pie(cv);
            break;
         case 345:
            sugar(cv);
            break;
         case 346:
            paper(cv);
            break;
         case 347:
            book(cv);
            break;
         case 348:
            return milkBucket();
         case 349:
            egg(cv);
            break;
         case 350:
            cakeItem(cv);
            break;
         default:
            return null;
      }

      return cv.p;
   }

   static void seeds(TexExtra.Cv cv, int body, int edge, int shine) {
      int[][] at = new int[][]{{3, 9}, {7, 5}, {10, 9}, {6, 11}, {11, 5}, {4, 5}, {8, 8}};
      for (int[] s : at) {
         cv.set(s[0], s[1], body);
         cv.set(s[0] + 1, s[1], body);
         cv.set(s[0], s[1] + 1, body);
         cv.set(s[0] + 1, s[1] + 1, edge);
         cv.set(s[0] + 2, s[1] + 1, edge);
         cv.set(s[0], s[1], shine);
      }
   }

   static void melonSlice(TexExtra.Cv cv) {
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            double dx = x + 0.5 - 8.0;
            double dy = y + 0.5 - 3.0;
            double r = Math.hypot(dx, dy);
            if (dy < 0.0 || r > 9.6) {
               continue;
            }

            int c;
            if (r > 8.6) {
               c = rgb(46, 104, 36);
            } else if (r > 7.6) {
               c = rgb(112, 176, 76);
            } else if (r > 6.7) {
               c = rgb(228, 238, 214);
            } else {
               c = (x + y) % 5 == 0 ? rgb(214, 44, 56) : rgb(236, 60, 72);
               if (dy < 1.6) {
                  c = rgb(246, 98, 104);
               }
            }

            cv.set(x, y, c);
         }
      }

      for (int[] s : new int[][]{{5, 7}, {8, 9}, {11, 7}, {7, 5}, {10, 5}}) {
         cv.set(s[0], s[1], rgb(24, 20, 20));
         cv.set(s[0], s[1] + 1, rgb(24, 20, 20));
      }
   }

   static void pie(TexExtra.Cv cv) {
      cv.ell(8.0, 9.6, 7.4, 5.2, rgb(190, 122, 54));
      cv.ell(8.0, 9.2, 6.2, 4.2, rgb(224, 138, 44));
      cv.ell(8.0, 9.2, 5.0, 3.2, rgb(214, 120, 30));
      // crust edge pattern
      for (int x = 1; x < 15; x += 2) {
         for (int y = 0; y < 16; y++) {
            double dx = x + 0.5 - 8.0;
            double dy = y + 0.5 - 9.6;
            double r = Math.sqrt(dx * dx / (7.4 * 7.4) + dy * dy / (5.2 * 5.2));
            if (r > 0.86 && r <= 1.0) {
               cv.set(x, y, rgb(214, 156, 78));
            }
         }
      }

      cv.rect(6, 8, 3, 2, rgb(248, 238, 214));
      cv.set(6, 8, rgb(255, 252, 240));
      cv.set(11, 8, rgb(150, 76, 22));
      cv.set(5, 10, rgb(150, 76, 22));
      cv.set(10, 11, rgb(150, 76, 22));
      cv.shade();
   }

   static void sugar(TexExtra.Cv cv) {
      int w1 = rgb(252, 252, 252);
      int w2 = rgb(222, 224, 232);
      int w3 = rgb(188, 192, 206);
      int[][] rows = new int[][]{{7, 9}, {6, 10}, {5, 11}, {4, 12}, {3, 13}, {3, 13}};
      for (int i = 0; i < rows.length; i++) {
         int y = 6 + i;
         for (int x = rows[i][0]; x <= rows[i][1]; x++) {
            int c = (x + y) % 3 == 0 ? w2 : w1;
            if (i >= 4) {
               c = (x + y) % 2 == 0 ? w2 : w3;
            }

            if (x == rows[i][1]) {
               c = w3;
            }

            cv.set(x, y, c);
         }
      }

      cv.rect(4, 12, 10, 1, w3);
      cv.set(8, 6, w1);
      cv.set(6, 8, rgb(255, 255, 255));
      cv.set(10, 9, w3);
   }

   static void paper(TexExtra.Cv cv) {
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            int s = x + y - 16;
            int d = x - y;
            if (Math.abs(s) <= 3 && Math.abs(d) <= 9) {
               int c = rgb(244, 244, 236);
               if (Math.abs(s) == 3) {
                  c = rgb(204, 204, 192);
               } else if (s == -2) {
                  c = rgb(255, 255, 250);
               }

               if (d >= 8) {
                  c = rgb(216, 216, 204);
               }

               cv.set(x, y, c);
            }
         }
      }

      cv.line(5, 10, 9, 6, rgb(176, 176, 190));
      cv.line(6, 11, 10, 7, rgb(176, 176, 190));
   }

   static void book(TexExtra.Cv cv) {
      int cover = rgb(132, 72, 40);
      cv.rect(3, 2, 10, 12, cover);
      cv.rect(3, 2, 2, 12, rgb(94, 48, 26));
      cv.rect(5, 2, 8, 1, rgb(160, 94, 56));
      cv.rect(4, 13, 9, 1, rgb(96, 50, 28));
      cv.rect(12, 3, 1, 10, rgb(112, 60, 32));
      cv.rect(6, 4, 5, 5, rgb(206, 174, 66));
      cv.rect(7, 5, 3, 3, rgb(150, 100, 46));
      cv.set(8, 6, rgb(226, 196, 90));
      cv.rect(13, 3, 1, 10, rgb(238, 228, 202));
      cv.rect(13, 4, 1, 1, rgb(200, 190, 164));
      cv.rect(13, 7, 1, 1, rgb(200, 190, 164));
      cv.rect(13, 10, 1, 1, rgb(200, 190, 164));
   }

   static int[] milkBucket() {
      int[] src = TexExtra.sprite(283);
      int[] out = new int[256];
      int water = 0xFF000000 | 3829728;
      for (int i = 0; i < 256; i++) {
         out[i] = src[i] == water ? rgb(248, 248, 244) : src[i];
      }

      for (int x = 4; x < 12; x++) {
         int i = 6 * 16 + x;
         if (out[i] == rgb(248, 248, 244)) {
            out[i] = rgb(222, 224, 226);
         }
      }

      return out;
   }

   static void egg(TexExtra.Cv cv) {
      cv.ell(8.0, 9.2, 4.2, 5.4, rgb(224, 202, 160));
      cv.ell(7.6, 8.6, 3.4, 4.6, rgb(240, 224, 186));
      cv.ell(7.0, 7.2, 1.6, 2.2, rgb(252, 244, 214));
      cv.set(9, 11, rgb(196, 170, 126));
      cv.set(6, 12, rgb(196, 170, 126));
      cv.set(10, 9, rgb(206, 182, 138));
      cv.shade();
   }

   static void cakeItem(TexExtra.Cv cv) {
      int sponge = rgb(158, 98, 54);
      int spongeDark = rgb(126, 76, 42);
      int cream = rgb(250, 244, 236);
      int creamDark = rgb(222, 206, 196);
      // sides
      cv.rect(2, 8, 12, 5, sponge);
      cv.rect(2, 12, 12, 1, spongeDark);
      cv.rect(2, 10, 12, 1, rgb(246, 234, 206));
      cv.clr(2, 12, 1, 1);
      cv.clr(13, 12, 1, 1);
      cv.clr(2, 8, 1, 1);
      cv.clr(13, 8, 1, 1);
      // top
      cv.ell(8.0, 7.6, 6.2, 3.6, creamDark);
      cv.ell(8.0, 7.4, 5.4, 3.0, cream);
      cv.rect(2, 8, 12, 1, cream);
      cv.rect(2, 9, 12, 1, creamDark);
      // cherries
      int red = rgb(204, 36, 48);
      cv.rect(5, 5, 2, 2, red);
      cv.set(5, 5, rgb(244, 110, 116));
      cv.rect(9, 6, 2, 2, red);
      cv.set(9, 6, rgb(244, 110, 116));
      cv.set(6, 4, rgb(70, 126, 44));
   }

   private TexArt37() {
   }
}
