package bc;

/**
 * Update 3.18, the texture overhaul: the painters of the tiles of the old blocks and items (tiles 0..399, same ids, same atlas, 16 x 16 pixels).
 * The old painters in {@link Tex} paint every pixel on its own (white noise); these ones paint like a person who draws pixel art: soft blotches
 * (noise that wraps round, so the tile repeats without a seam) cut into a few shades of one palette, cracks, pebbles, highlights on the upper left
 * edge of every stone and a dark outline round every object. No OpenGL in here - {@code bc.AtlasSheet} shows the result.
 *
 * <p>Colours of the tiles that the game tints (grass top, grass side, leaves, vines ...) are grey: the tint multiplies them, so their average stays
 * where the old painters had it (about 180 for grass, 150 for leaves).
 */
final class TexPaint {
   private TexPaint() {
   }

   static final int N = 16;

   // ------------------------------------------------------------------ colours
   static int rgb(int r, int g, int b) {
      return 0xFF000000 | clamp(r) << 16 | clamp(g) << 8 | clamp(b);
   }

   static int clamp(int v) {
      return v < 0 ? 0 : (v > 255 ? 255 : v);
   }

   /** 0xRRGGBB to an opaque colour. */
   static int c(int rgb) {
      return 0xFF000000 | rgb & 0xFFFFFF;
   }

   static int r(int c) {
      return c >> 16 & 255;
   }

   static int g(int c) {
      return c >> 8 & 255;
   }

   static int b(int c) {
      return c & 255;
   }

   /** Each channel times k (alpha kept). */
   static int mul(int c, float k) {
      return c & 0xFF000000 | clamp((int)(r(c) * k + 0.5F)) << 16 | clamp((int)(g(c) * k + 0.5F)) << 8 | clamp((int)(b(c) * k + 0.5F));
   }

   /** Adds d to every channel. */
   static int add(int c, int d) {
      return c & 0xFF000000 | clamp(r(c) + d) << 16 | clamp(g(c) + d) << 8 | clamp(b(c) + d);
   }

   static int mix(int a, int bb, float t) {
      t = t < 0.0F ? 0.0F : (t > 1.0F ? 1.0F : t);
      return 0xFF000000 | clamp((int)(r(a) + (r(bb) - r(a)) * t + 0.5F)) << 16 | clamp((int)(g(a) + (g(bb) - g(a)) * t + 0.5F)) << 8 | clamp((int)(b(a) + (b(bb) - b(a)) * t + 0.5F));
   }

   /** The colour of a palette for a value 0..1 (the palette is cut into equal steps: no blending, that is what makes it look drawn). */
   static int step(int[] pal, float v) {
      int i = (int)(v * pal.length);
      return pal[i < 0 ? 0 : (i >= pal.length ? pal.length - 1 : i)];
   }

   // ------------------------------------------------------------------ noise that wraps round the tile
   static float hash(int x, int y, int seed) {
      int n = x * 374761393 + y * 668265263 + seed * 1274126177;
      n = (n ^ n >>> 13) * 1274126177;
      n ^= n >>> 16;
      return (n & 0xFFFFFF) / 16777216.0F;
   }

   /** Smooth value noise, 0..1, with {@code cells} lattice cells across the 16 pixels (wraps, so the tile repeats). */
   static float vn(float px, float py, int cells, int seed) {
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

   /** Three octaves, 0..1, mostly 0.3 .. 0.7; (x, y) is the pixel centre. */
   static float fbm(int x, int y, int seed) {
      float px = x + 0.5F;
      float py = y + 0.5F;
      return vn(px, py, 2, seed) * 0.34F + vn(px, py, 4, seed + 11) * 0.38F + vn(px, py, 8, seed + 23) * 0.28F;
   }

   /** A value spread out so that it uses the whole 0..1 (value noise piles up in the middle). */
   static float contrast(float v, float k) {
      float t = (v - 0.5F) * k + 0.5F;
      return t < 0.0F ? 0.0F : (t > 1.0F ? 1.0F : t);
   }

   /** Per pixel noise -1..1. */
   static float grain(int x, int y, int seed) {
      return hash(x & 15, y & 15, seed) * 2.0F - 1.0F;
   }

   // ------------------------------------------------------------------ canvas
   static int[] canvas(int fill) {
      int[] p = new int[N * N];
      java.util.Arrays.fill(p, fill);
      return p;
   }

   static void set(int[] p, int x, int y, int c) {
      if (x >= 0 && y >= 0 && x < N && y < N) {
         p[y * N + x] = c;
      }
   }

   /** Wrapping variant: what goes out on the right comes back on the left (for details of a repeating tile). */
   static void setw(int[] p, int x, int y, int c) {
      p[(y & 15) * N + (x & 15)] = c;
   }

   static int get(int[] p, int x, int y) {
      return p[(y & 15) * N + (x & 15)];
   }

   static java.util.Random rnd(int seed) {
      return new java.util.Random(seed * 7919L + 13L);
   }

   /** A random walk that paints a crack: starts at (x, y), 'len' steps, mostly in direction (dx, dy). */
   static void crack(int[] p, java.util.Random r, int x, int y, int len, int dx, int dy, int dark, int light) {
      for (int i = 0; i < len; i++) {
         setw(p, x, y, dark);
         if (light != 0 && (i & 1) == 0) {
            int lx = x - dy;
            int ly = y + dx;
            if (get(p, lx, ly) != dark) {
               setw(p, lx, ly, light);
            }
         }

         int s = r.nextInt(5);
         if (s == 0) {
            x += dx;
         } else if (s == 1) {
            y += dy;
         } else if (s == 2) {
            x += dx;
            y += dy;
         } else if (s == 3) {
            x += dx;
            y += dy == 0 ? r.nextInt(3) - 1 : 0;
         } else {
            y += dy;
            x += dx == 0 ? r.nextInt(3) - 1 : 0;
         }
      }
   }

   /** Nearest and second nearest feature point of a wrapping point set: {distance to the nearest, distance to the second, index of the nearest}. */
   static float[] cells(float[] fx, float[] fy, float px, float py) {
      float best = 1.0E9F;
      float second = 1.0E9F;
      int bi = 0;
      for (int i = 0; i < fx.length; i++) {
         for (int oy = -1; oy <= 1; oy++) {
            for (int ox = -1; ox <= 1; ox++) {
               float dx = fx[i] + ox * N - px;
               float dy = fy[i] + oy * N - py;
               float d = dx * dx + dy * dy;
               if (d < best) {
                  second = best;
                  best = d;
                  bi = i;
               } else if (d < second) {
                  second = d;
               }
            }
         }
      }

      return new float[]{(float)Math.sqrt(best), (float)Math.sqrt(second), bi};
   }

   /** Feature points on a jittered grid (n x n cells): wraps, spreads evenly. */
   static float[][] points(int n, int seed, float jitter) {
      float[] fx = new float[n * n];
      float[] fy = new float[n * n];
      float cell = (float)N / n;
      for (int j = 0; j < n; j++) {
         for (int i = 0; i < n; i++) {
            fx[j * n + i] = (i + 0.5F + (hash(i, j, seed) - 0.5F) * jitter) * cell;
            fy[j * n + i] = (j + 0.5F + (hash(i, j, seed + 5) - 0.5F) * jitter) * cell;
         }
      }

      return new float[][]{fx, fy};
   }

   // ------------------------------------------------------------------ the stones
   /** Stone: blotches of five greys, a few cracks with a light lip, a few bright flecks. */
   static int[] stone() {
      return stone(1);
   }

   static int[] stone(int seed) {
      int[] pal = {c(0x666666), c(0x747474), c(0x818181), c(0x8F8F8F), c(0x9D9D9D)};
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float v = fbm(x, y, seed * 31) * 0.52F + vn(x + 0.5F, y + 0.5F, 8, seed + 77) * 0.22F + hash(x, y, seed + 3) * 0.26F;
            p[y * N + x] = step(pal, contrast(v, 2.5F));
         }
      }

      java.util.Random r = rnd(seed + 40);
      for (int i = 0; i < 2; i++) {
         crack(p, r, r.nextInt(N), r.nextInt(N), 3 + r.nextInt(3), r.nextBoolean() ? 1 : -1, r.nextInt(3) - 1, c(0x585858), 0);
      }

      for (int i = 0; i < 6; i++) {
         setw(p, r.nextInt(N), r.nextInt(N), c(0xA9A9A9));
         setw(p, r.nextInt(N), r.nextInt(N), c(0x5E5E5E));
      }

      return p;
   }

   /** Cobblestone: round stones, each lit on its upper left, dark gaps between them. */
   static int[] cobble() {
      return cobble(5, 0, 0, 0);
   }

   /** Cobblestone tinted by (tr, tg, tb) added to every channel (mossy, andesite ... use it). */
   static int[] cobble(int seed, int tr, int tg, int tb) {
      float[][] pt = points(4, seed * 17, 0.85F);
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float[] c = cells(pt[0], pt[1], x + 0.5F, y + 0.5F);
            int i = (int)c[2];
            float edge = c[1] - c[0];
            float base = 0.30F + hash(i, 3, seed) * 0.5F;
            int v;
            if (edge < 0.85F) {
               v = 76;
            } else {
               float cx = pt[0][i];
               float cy = pt[1][i];
               float dx = x + 0.5F - cx;
               float dy = y + 0.5F - cy;
               // wrapped offsets
               if (dx > 8) {
                  dx -= N;
               } else if (dx < -8) {
                  dx += N;
               }

               if (dy > 8) {
                  dy -= N;
               } else if (dy < -8) {
                  dy += N;
               }

               float light = (-dx - dy) * 3.2F;
               v = (int)(128 + base * 40 + light + grain(x, y, seed + 9) * 5.0F);
               if (edge < 1.7F) {
                  v -= 9; // the rim of a stone is a little darker, it rounds off
               }
            }

            p[y * N + x] = rgb(v + tr, v + tg, v + tb);
         }
      }

      return p;
   }

   /** Dirt: brown blotches, pebbles with a shadow, dark specks. */
   static int[] dirt() {
      int[] pal = {c(0x6F4A2E), c(0x7C5636), c(0x8A6140), c(0x986C49), c(0xA67853)};
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float v = fbm(x, y, 77) * 0.72F + hash(x, y, 9) * 0.28F;
            p[y * N + x] = step(pal, contrast(v, 1.8F));
         }
      }

      java.util.Random r = rnd(3);
      for (int i = 0; i < 4; i++) {
         int x = r.nextInt(N);
         int y = r.nextInt(N);
         setw(p, x, y, c(0xA39A8C));
         setw(p, x + 1, y, c(0x8F877B));
         setw(p, x, y + 1, c(0x6F6256));
         if (i < 2) {
            setw(p, x + 1, y + 1, c(0x5A4F45));
         }
      }

      for (int i = 0; i < 9; i++) {
         setw(p, r.nextInt(N), r.nextInt(N), c(0x5B3C25));
      }

      return p;
   }

   /** Sand: fine warm grain, paler and darker grains, a hint of ripples. */
   static int[] sand() {
      return sand(0, 0, 0);
   }

   static int[] sand(int tr, int tg, int tb) {
      int[] pal = {c(0xCDBB8C), c(0xD6C497), c(0xDDCDA2), c(0xE4D5AB), c(0xEBDDB5)};
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float v = vn(x + 0.5F, y * 2 + 0.5F, 8, 5) * 0.45F + hash(x, y, 21) * 0.55F;
            int col = step(pal, contrast(v, 1.5F));
            p[y * N + x] = rgb(r(col) + tr, g(col) + tg, b(col) + tb);
         }
      }

      java.util.Random rr = rnd(5);
      for (int i = 0; i < 7; i++) {
         int x = rr.nextInt(N);
         int y = rr.nextInt(N);
         int d = get(p, x, y);
         setw(p, x, y, rgb(r(d) - 24 + tr * 0, g(d) - 28, b(d) - 34));
      }

      return p;
   }

   /** Gravel: pebbles of grey, brown and blue-grey, each lit on its upper left, thin dark gaps. */
   static int[] gravel() {
      float[][] pt = points(5, 21, 0.8F);
      int[][] pal = {{0x8E8A86}, {0x7E7B79}, {0x9C9892}, {0x716F70}, {0x938E86}, {0x85848A}, {0xA6A19A}, {0x8A847A}};
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float[] c = cells(pt[0], pt[1], x + 0.5F, y + 0.5F);
            int i = (int)c[2];
            float edge = c[1] - c[0];
            int base = pal[(int)(hash(i, 1, 4) * pal.length)][0];
            if (edge < 0.7F) {
               p[y * N + x] = c(0x585654);
            } else {
               float dx = x + 0.5F - pt[0][i];
               float dy = y + 0.5F - pt[1][i];
               if (dx > 8) {
                  dx -= N;
               } else if (dx < -8) {
                  dx += N;
               }

               if (dy > 8) {
                  dy -= N;
               } else if (dy < -8) {
                  dy += N;
               }

               int col = add(c(base), (int)((-dx - dy) * 3.2F + grain(x, y, 6) * 4.0F));
               if (edge < 1.3F) {
                  col = mul(col, 0.9F);
               }

               p[y * N + x] = col;
            }
         }
      }

      return p;
   }

   // ------------------------------------------------------------------ grass
   /** The top of grass (grey, the biome tints it): clumps of brighter and darker blades. */
   static int[] grassTop() {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float v = fbm(x, y, 11) * 0.7F + hash(x, y, 12) * 0.3F;
            int g = (int)(150 + contrast(v, 2.0F) * 62);
            p[y * N + x] = rgb(g, g, g);
         }
      }

      java.util.Random r = rnd(11);
      for (int i = 0; i < 26; i++) {
         int x = r.nextInt(N);
         int y = r.nextInt(N);
         int len = 2 + r.nextInt(2);
         for (int k = 0; k < len; k++) {
            int g = k == 0 ? 218 : (k == len - 1 ? 150 : 190);
            setw(p, x, y + k, rgb(g, g, g));
         }
      }

      return p;
   }

   /** The edge of grass on the side of a block (grey, drawn over the dirt): a fringe of blades that hang to different depths. */
   static int[] grassSide() {
      int[] p = new int[N * N];
      java.util.Random r = rnd(12);
      for (int x = 0; x < N; x++) {
         int len = 3 + (int)(vn(x + 0.5F, 0.5F, 4, 90) * 2.6F) + (r.nextInt(5) == 0 ? 1 : 0);
         for (int y = 0; y < len; y++) {
            int g = 186 + r.nextInt(30) - 12;
            if (y == 0) {
               g += 12;
            }

            if (y == len - 1) {
               g -= 34;
            }

            p[y * N + x] = rgb(g, g, g);
         }

         if (r.nextInt(3) == 0) {
            int g = 168 + r.nextInt(14);
            p[len * N + x] = rgb(g, g, g);
         }
      }

      return p;
   }

   // ------------------------------------------------------------------ wood
   /** Bark: plates of bark between dark vertical grooves that wiggle now and then, a knot or two. */
   static int[] logSide(int dark, int mid, int light, int high, int seed) {
      int[] pal = {mid, light, high};
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float v = vn(x + 0.5F, (y + 0.5F) * 0.3F, 8, seed) * 0.55F + vn(x + 0.5F, (y + 0.5F) * 0.22F, 4, seed + 2) * 0.2F + hash(x, y, seed + 1) * 0.25F;
            int col = step(pal, contrast(v, 2.0F));
            p[y * N + x] = col == mid && hash(x, y, seed + 9) < 0.18F ? mul(mid, 0.86F) : col;
         }
      }

      for (int i = 0; i < 4; i++) {
         int gx = i * 4 + (int)(hash(i, 0, seed + 5) * 3.0F);
         for (int y = 0; y < N; y++) {
            int wob = (int)(vn(0.5F, y + 0.5F, 4, seed + 30 + i) * 2.6F) - 1;
            if (hash(y >> 2, i, seed + 7) > 0.14F) {
               setw(p, gx + wob, y, dark);
               if (hash(y, i, seed + 8) < 0.5F) {
                  setw(p, gx + wob + 1, y, mix(dark, mid, 0.45F));
               }

               if (hash(y, i, seed + 12) < 0.55F) {
                  setw(p, gx + wob - 1, y, high);
               }
            }
         }
      }

      for (int i = 0; i < 2; i++) {
         int kx = 1 + (int)(hash(i, 3, seed + 20) * 13.0F);
         int ky = 1 + (int)(hash(i, 4, seed + 20) * 12.0F);
         setw(p, kx, ky, mul(dark, 0.9F));
         setw(p, kx + 1, ky, dark);
         setw(p, kx, ky + 1, dark);
         setw(p, kx + 1, ky + 1, mix(dark, mid, 0.5F));
         setw(p, kx - 1, ky - 1, light);
      }

      return p;
   }

   /** The end of a log: a bark rim, then rings of two woods (not quite square: the rings wander). */
   static int[] logTop(int bark, int barkLight, int ringA, int ringB, int core, int seed) {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float dx = Math.abs(x - 7.5F);
            float dy = Math.abs(y - 7.5F);
            float m = Math.max(dx, dy);
            float n = (vn(x + 0.5F, y + 0.5F, 4, seed) - 0.5F) * 0.9F;
            int col;
            if (m > 5.9F) {
               col = hash(x, y, seed + 2) < 0.4F ? barkLight : bark;
               if (m > 6.9F) {
                  col = mul(bark, 0.82F);
               }
            } else {
               float d = m * 0.8F + Math.min(dx, dy) * 0.2F + n;
               int idx = (int)(d / 1.3F);
               col = idx <= 0 ? core : (idx % 2 == 1 ? ringB : ringA);
               if (hash(x, y, seed + 3) < 0.16F) {
                  col = idx % 2 == 1 ? ringA : ringB;
               }
            }

            p[y * N + x] = col;
         }
      }

      return p;
   }

   static int[] oakSide() {
      int[] v56 = logSide(c(0x45341D), c(0x66502F), c(0x78613A), c(0x8A7045), 8);
      int[] mcCols = {c(0x48361E), c(0x6D5532), c(0x7E633B), c(0x584327)};
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            int mc = mcCols[(x + (y >> 2)) & 3];
            v56[y * N + x] = mix(v56[y * N + x], mc, 0.44F);
         }
      }
      return v56;
   }

   static int[] oakTop() {
      return logTop(c(0x5E4A2B), c(0x6E5832), c(0xB08E58), c(0xC4A56C), c(0x9C7C47), 9);
   }

   static int[] spruceSide() {
      int[] v56 = logSide(c(0x2B1F10), c(0x40301B), c(0x4E3B22), c(0x5C4729), 32);
      int[] mcCols = {c(0x281B0E), c(0x3B2814), c(0x4A331B), c(0x332211)};
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            int mc = mcCols[(x + (y >> 2)) & 3];
            v56[y * N + x] = mix(v56[y * N + x], mc, 0.44F);
         }
      }
      return v56;
   }

   static int[] spruceTop() {
      return logTop(c(0x3A2B17), c(0x47361E), c(0x7E6038), c(0x93724A), c(0x6D5230), 33);
   }

   /** Birch: 5.6 soft streaks fused with Minecraft's creamy birch bark & dark horizontal lenticel dashes. */
   static int[] birchSide() {
      int[] pal = {c(0xCFCCB8), c(0xD8D5C2), c(0xE0DDCC), c(0xE8E6D8)};
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float v = vn(x + 0.5F, (y + 0.5F) * 0.4F, 8, 31) * 0.6F + hash(x, y, 32) * 0.4F;
            p[y * N + x] = mix(step(pal, contrast(v, 1.8F)), c(0xDFDCD0), 0.35F);
         }
      }

      java.util.Random r = rnd(31);
      for (int i = 0; i < 9; i++) {
         int x = r.nextInt(N);
         int y = r.nextInt(N);
         int len = 2 + r.nextInt(4);
         for (int k = 0; k < len; k++) {
            setw(p, x + k, y, c(0x383430));
            if (k == 0 || r.nextInt(3) == 0) {
               setw(p, x + k, y - 1, c(0xBBB7A6));
            }
            if (r.nextInt(3) != 0) {
               setw(p, x + k, y + 1, c(0xC2BEB0));
            }
         }
      }

      return p;
   }

   /** Leaves (grey, the biome tints them): clumps with a dark underside, a bright upper left edge, and holes where the sky shows through. */
   static int[] leaves() {
      int[] p = new int[N * N];
      float[] t = new float[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float v = fbm(x, y, 41) * 0.62F + hash(x, y, 42) * 0.38F;
            t[y * N + x] = contrast(v, 2.3F);
         }
      }

      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float v = t[y * N + x];
            if (v < 0.17F) {
               p[y * N + x] = 0;
               continue;
            }

            int g = (int)(112 + v * 100);
            float ul = t[(y - 1 & 15) * N + (x - 1 & 15)];
            float lr = t[(y + 1 & 15) * N + (x + 1 & 15)];
            if (ul < 0.17F) {
               g += 22;
            }

            if (lr < 0.17F) {
               g -= 30;
            }

            p[y * N + x] = rgb(g, g, g);
         }
      }

      return p;
   }

   /** Planks: four boards with grain, a dark seam under each, and a joint in each board. */
   static int[] planks(int base, int seed) {
      int[] p = new int[N * N];
      for (int bd = 0; bd < 4; bd++) {
         float tone = (hash(bd, 1, seed) - 0.5F) * 0.12F;
         int joint = 2 + (int)(hash(bd, 2, seed) * 12.0F);
         for (int row = 0; row < 4; row++) {
            int y = bd * 4 + row;
            for (int x = 0; x < N; x++) {
               float gr = vn((x + 0.5F) * 0.33F, (y + 0.5F) * 1.1F, 8, seed + 4) - 0.5F;
               float k = 1.0F + tone + gr * 0.22F + grain(x, y, seed + 6) * 0.025F;
               int col = mul(base, k);
               if (row == 3) {
                  col = mul(base, 0.64F + gr * 0.1F);
               } else if (row == 0) {
                  col = mul(col, 1.07F);
               }

               if (x == joint && row < 3) {
                  col = mul(base, 0.72F);
               } else if (x == (joint + 1 & 15) && row < 3) {
                  col = mul(col, 1.06F);
               }

               p[y * N + x] = col;
            }
         }
      }

      return p;
   }

   static int[] oakPlanks() {
      return planks(c(0xB08A52), 51);
   }

   // ------------------------------------------------------------------ ores
   private static final int[][][] BLOBS = {
      {{0, 0}, {1, 0}, {0, 1}, {1, 1}},
      {{0, 0}, {1, 0}, {2, 0}, {0, 1}, {1, 1}},
      {{0, 0}, {0, 1}, {1, 1}, {1, 2}, {0, 2}},
      {{1, 0}, {2, 0}, {0, 1}, {1, 1}, {2, 1}},
      {{0, 0}, {1, 0}, {0, 1}, {1, 1}, {2, 1}},
      {{1, 0}, {0, 1}, {1, 1}, {2, 1}, {1, 2}},
      {{0, 0}, {1, 0}, {2, 0}, {0, 1}, {1, 1}, {1, 2}}
   };
   private static final int[][][] DOTS = {
      {{0, 0}, {1, 0}, {0, 1}, {1, 1}},
      {{0, 0}, {1, 0}, {1, 1}},
      {{0, 0}, {0, 1}, {1, 1}}
   };

   /** Stone with blobs of an ore: main colour, a darker lower right edge and a bright upper left pixel. {@code n} blobs of the shapes above (small ones when {@code small}). */
   static int[] ore(int main, int shade, int high, int seed, int n, boolean small) {
      int[] p = stone(1);
      boolean[] used = new boolean[9];
      java.util.Random r = rnd(seed);
      for (int i = 0; i < n; i++) {
         int cell = r.nextInt(9);
         for (int tries = 0; used[cell] && tries < 9; tries++) {
            cell = (cell + 1) % 9;
         }

         used[cell] = true;
         int[][] sh = small ? DOTS[r.nextInt(DOTS.length)] : BLOBS[r.nextInt(BLOBS.length)];
         int ox = cell % 3 * 5 + r.nextInt(3);
         int oy = cell / 3 * 5 + r.nextInt(3);
         for (int[] q : sh) {
            int x = ox + q[0];
            int y = oy + q[1];
            boolean right = false;
            boolean below = false;
            boolean left = false;
            boolean above = false;
            for (int[] o : sh) {
               right |= o[0] == q[0] + 1 && o[1] == q[1];
               below |= o[0] == q[0] && o[1] == q[1] + 1;
               left |= o[0] == q[0] - 1 && o[1] == q[1];
               above |= o[0] == q[0] && o[1] == q[1] - 1;
            }

            int col = main;
            if (!right && !below) {
               col = shade;
            } else if (!right || !below) {
               col = mix(main, shade, 0.5F);
            }

            if (!left && !above) {
               col = high;
            }

            set(p, x, y, col);
            int sx = x + 1;
            int sy = y + 1;
            if (sx < N && sy < N && !blobAt(sh, ox, oy, sx, sy)) {
               int under = p[sy * N + sx];
               if (under != main && under != shade && under != high) {
                  p[sy * N + sx] = mul(under, 0.78F);
               }
            }
         }
      }

      return p;
   }

   private static boolean blobAt(int[][] sh, int ox, int oy, int x, int y) {
      for (int[] q : sh) {
         if (ox + q[0] == x && oy + q[1] == y) {
            return true;
         }
      }

      return false;
   }

   static int[] coalOre() {
      return ore(c(0x2B2B2B), c(0x111111), c(0x5E5E5E), 12, 5, false);
   }

   static int[] ironOre() {
      return ore(c(0xD8B093), c(0xAA7F5E), c(0xF2DCC8), 13, 5, false);
   }

   static int[] goldOre() {
      return ore(c(0xF8D838), c(0xC89A0C), c(0xFFF6A8), 14, 5, false);
   }

   static int[] diamondOre() {
      return ore(c(0x4EDCD2), c(0x239CA2), c(0xD6FFFA), 15, 5, false);
   }

   static int[] redstoneOre() {
      return ore(c(0xD01414), c(0x800000), c(0xFF6E5E), 16, 7, true);
   }

   static int[] lapisOre() {
      return ore(c(0x2E52C8), c(0x17308C), c(0x86A4F4), 17, 5, false);
   }

   // ------------------------------------------------------------------ the rest of the first page
   /** Snow: white with pale blue shade in soft patches and a few sparkles. */
   static int[] snow() {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float v = fbm(x, y, 18) * 0.7F + hash(x, y, 19) * 0.3F;
            int col = v < 0.4F ? c(0xE0E8F4) : (v < 0.5F ? c(0xEBF1FA) : (v > 0.66F ? c(0xFFFFFF) : c(0xF4F8FC)));
            p[y * N + x] = col;
         }
      }

      java.util.Random r = rnd(18);
      for (int i = 0; i < 4; i++) {
         setw(p, r.nextInt(N), r.nextInt(N), c(0xCCD8EC));
      }

      return p;
   }

   /** Wool: a weave of threads (alternating light and dark, the rows shifted), soft clumps. */
   static int[] wool(int colour) {
      int[] col = Tex.WOOL[colour];
      int base = rgb(col[0], col[1], col[2]);
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            int thread = (x >> 1) + y & 1;
            float k = 1.0F + (thread == 0 ? 0.045F : -0.045F) + (fbm(x, y, 5 + colour) - 0.5F) * 0.16F + grain(x, y, 9 + colour) * 0.025F;
            p[y * N + x] = mul(base, k);
         }
      }

      return p;
   }

   static int[] sandstoneSide() {
      return sandstoneSide(0, 0, 0);
   }

   /** The side of sandstone: light bands, a dark seam near the top and the bottom, fine grain. */
   static int[] sandstoneSide(int tr, int tg, int tb) {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float v = vn((x + 0.5F) * 0.5F, (y + 0.5F) * 1.3F, 8, 21) * 0.45F + hash(x, y, 22) * 0.55F;
            int[] pal = {c(0xD2C192), c(0xD9C99B), c(0xE0D1A5), c(0xE6D8AE)};
            int col = step(pal, contrast(v, 1.4F));
            if (y == 3 || y == 12) {
               col = mul(col, 0.88F);
            } else if (y == 4 || y == 13) {
               col = mul(col, 0.95F);
            } else if (y == 11) {
               col = mul(col, 1.04F);
            }

            p[y * N + x] = rgb(r(col) + tr, g(col) + tg, b(col) + tb);
         }
      }

      return p;
   }

   /** Called after TexExtra has painted its tiles: the ones of them that the overhaul redraws. */
   static void blocks() {
      Tex.blit(Tex.MOSSY, mossyCobble());
      Tex.blit(Tex.CRAFT_TOP, craftTop());
      Tex.blit(Tex.CRAFT_SIDE, craftSide());
      Tex.blit(Tex.FURNACE_FRONT, furnaceFront(false));
      Tex.blit(Tex.FURNACE_ON, furnaceFront(true));
      Tex.blit(Tex.CHEST_TOP, chestTop());
      Tex.blit(Tex.CHEST_SIDE, chestSide());
      Tex.blit(Tex.SPAWNER, spawner());
   }

   // ------------------------------------------------------------------ built and special blocks
   /** Stone bricks: two rows of bricks (the second shifted), each a piece of stone of its own, a light upper edge and a dark lower edge. */
   static int[] stoneBrick() {
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         int row = y / 8;
         int ry = y % 8;
         for (int x = 0; x < N; x++) {
            int xs = row == 0 ? x : (x + 4) & 15;
            int brick = row * 2 + xs / 8;
            boolean mortar = ry == 0 || (xs & 7) == 7;
            if (mortar) {
               p[y * N + x] = c(0x62625F);
               continue;
            }

            float v = vn(x + 0.5F, y + 0.5F, 8, 60 + brick * 3) * 0.5F + hash(x, y, 61 + brick) * 0.5F;
            int g = 128 + (int)((v - 0.5F) * 34) + (brick == 1 || brick == 2 ? 6 : -2);
            if (ry == 1) {
               g += 10;
            } else if (ry == 7) {
               g -= 12;
            }

            if ((xs & 7) == 0) {
               g += 6;
            }

            p[y * N + x] = rgb(g, g, g - 1);
         }
      }

      return p;
   }

   /** Mossy cobblestone: the cobblestone with patches of moss in three greens. */
   static int[] mossyCobble() {
      int[] p = cobble(5, -6, -2, -8);
      int[] moss = {c(0x3F6A2B), c(0x4F7F36), c(0x628F40), c(0x2F5420)};
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float v = vn(x + 0.5F, y + 0.5F, 4, 71) * 0.65F + hash(x, y, 72) * 0.35F;
            if (v > 0.6F) {
               int col = moss[(int)(hash(x, y, 73) * 3.99F)];
               if (v < 0.65F) {
                  col = mix(p[y * N + x], col, 0.55F);
               }

               p[y * N + x] = col;
            }
         }
      }

      return p;
   }

   /** Glowstone: crystal cells of yellow and amber with dark orange seams and bright sparks. */
   static int[] glowstone() {
      float[][] pt = points(4, 31, 0.9F);
      int[] pal = {c(0xB7762D), c(0xD39A47), c(0xEDB95A), c(0xF8D77C), c(0xFFE9A0)};
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float[] cc = cells(pt[0], pt[1], x + 0.5F, y + 0.5F);
            int i = (int)cc[2];
            float edge = cc[1] - cc[0];
            int col;
            if (edge < 0.8F) {
               col = c(0x8E5420);
            } else {
               float dx = x + 0.5F - pt[0][i];
               float dy = y + 0.5F - pt[1][i];
               float k = hash(i, 5, 31) * 0.55F + 0.2F - (dx + dy) * 0.045F + grain(x, y, 32) * 0.12F;
               col = step(pal, k);
            }

            p[y * N + x] = col;
         }
      }

      java.util.Random r = rnd(31);
      for (int i = 0; i < 4; i++) {
         setw(p, r.nextInt(N), r.nextInt(N), c(0xFFF6CC));
      }

      return p;
   }

   /** Obsidian: almost black, with violet clouds and a few lighter cracks. */
   static int[] obsidian() {
      int[] pal = {c(0x0D0A18), c(0x150F26), c(0x1F1636), c(0x2D2050), c(0x433070)};
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float v = fbm(x, y, 34) * 0.7F + hash(x, y, 35) * 0.3F;
            p[y * N + x] = step(pal, contrast(v, 2.2F));
         }
      }

      java.util.Random r = rnd(34);
      for (int i = 0; i < 3; i++) {
         crack(p, r, r.nextInt(N), r.nextInt(N), 3 + r.nextInt(3), r.nextBoolean() ? 1 : -1, r.nextInt(3) - 1, c(0x6A4AA8), 0);
      }

      return p;
   }

   // ------------------------------------------------------------------ work blocks (they wear the new planks and cobblestone)
   private static void rect(int[] p, int x, int y, int w, int h, int col) {
      for (int j = 0; j < h; j++) {
         for (int i = 0; i < w; i++) {
            set(p, x + i, y + j, col);
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

   /** The top of a crafting table: planks round a sunken 3 x 3 grid. */
   static int[] craftTop() {
      int[] p = oakPlanks();
      int dark = c(0x4E361C);
      int line = c(0x6A4D2A);
      for (int i = 2; i < 14; i++) {
         set(p, i, 2, dark);
         set(p, i, 13, dark);
         set(p, 2, i, dark);
         set(p, 13, i, dark);
      }

      for (int y = 3; y < 13; y++) {
         for (int x = 3; x < 13; x++) {
            p[y * N + x] = mul(p[y * N + x], 0.8F);
         }
      }

      for (int i = 3; i < 13; i++) {
         set(p, i, 6, line);
         set(p, i, 9, line);
         set(p, 6, i, line);
         set(p, 9, i, line);
      }

      return p;
   }

   /** The side of a crafting table: planks, a dark rim under the top, a saw on the left and a hammer on the right. */
   static int[] craftSide() {
      int[] p = oakPlanks();
      for (int y = 0; y < 2; y++) {
         for (int x = 0; x < N; x++) {
            p[y * N + x] = mul(p[y * N + x], 0.72F);
         }
      }

      for (int x = 0; x < N; x++) {
         p[2 * N + x] = mul(p[2 * N + x], 0.86F);
      }

      int steel = c(0xC4C6CC);
      int steelDark = c(0x8C8E96);
      int outline = c(0x3A2810);
      int wood = c(0x7A5226);
      line(p, 2, 5, 8, 5, outline);
      line(p, 2, 6, 8, 6, steel);
      line(p, 2, 7, 8, 7, steelDark);
      line(p, 2, 8, 8, 8, outline);
      rect(p, 8, 4, 2, 5, wood);
      line(p, 4, 10, 6, 13, steel);
      line(p, 5, 10, 7, 13, outline);
      line(p, 12, 5, 12, 13, wood);
      line(p, 13, 5, 13, 13, outline);
      rect(p, 10, 3, 5, 3, steelDark);
      rect(p, 10, 3, 5, 1, steel);
      rect(p, 10, 5, 5, 1, outline);
      return p;
   }

   /** The front of a furnace: cobblestone, a slot above and the fire door below (lit when {@code on}). */
   static int[] furnaceFront(boolean on) {
      int[] p = cobble();
      int soot = c(0x1B1B1D);
      rect(p, 3, 8, 10, 7, c(0x2A2A2C));
      rect(p, 4, 9, 8, 5, soot);
      rect(p, 3, 8, 10, 1, c(0x5C5C60));
      rect(p, 3, 8, 1, 7, c(0x4A4A4E));
      if (on) {
         java.util.Random r = rnd(40);
         for (int x = 4; x < 12; x++) {
            int top = 10 + (int)(vn(x + 0.5F, 0.5F, 4, 41) * 2.5F);
            for (int y = top; y < 14; y++) {
               float k = (y - top) / (float)Math.max(1, 13 - top);
               int col = k < 0.4F ? c(0xFFE070) : (k < 0.75F ? c(0xFF9A28) : c(0xE05A14));
               p[y * N + x] = add(col, r.nextInt(13) - 6);
            }
         }

         for (int x = 4; x < 12; x++) {
            set(p, x, 9, c(0xFFC060));
         }

         for (int x = 2; x < 14; x++) {
            set(p, x, 7, add(p[7 * N + x], 14));
            set(p, x, 15, add(p[15 * N + x], 10));
         }
      }

      rect(p, 5, 2, 6, 3, c(0x2A2A2C));
      rect(p, 6, 3, 4, 1, c(0x16161A));
      rect(p, 5, 4, 6, 1, c(0x5C5C60));
      return p;
   }

   private static int[] chestWood(float k) {
      int[] p = oakPlanks();
      for (int i = 0; i < p.length; i++) {
         p[i] = mul(p[i], k);
      }

      return p;
   }

   private static void rimmed(int[] p, int dark, int light) {
      for (int i = 0; i < N; i++) {
         set(p, i, 0, dark);
         set(p, i, 15, dark);
         set(p, 0, i, dark);
         set(p, 15, i, dark);
      }

      for (int i = 1; i < 15; i++) {
         set(p, i, 1, light);
         set(p, 1, i, light);
      }
   }

   static int[] chestTop() {
      int[] p = chestWood(0.86F);
      rimmed(p, c(0x321F0A), c(0xA77E48));
      return p;
   }

   static int[] chestSide() {
      int[] p = chestWood(0.86F);
      rimmed(p, c(0x321F0A), c(0xA77E48));
      for (int i = 0; i < N; i++) {
         set(p, i, 5, c(0x321F0A));
         set(p, i, 6, mul(p[6 * N + i], 1.12F));
      }

      rect(p, 7, 3, 2, 5, c(0xCDCDD8));
      rect(p, 7, 6, 2, 1, c(0x85858F));
      rect(p, 7, 4, 2, 1, c(0x3C3C46));
      rect(p, 6, 3, 1, 5, c(0x2A2A30));
      rect(p, 9, 3, 1, 5, c(0x2A2A30));
      return p;
   }

   /** The monster spawner: a dark cage of metal bars round three cells of darkness. */
   static int[] spawner() {
      int[] p = cobble(9, -92, -92, -80);
      int bar = c(0x7A82A0);
      int barLight = c(0xA4ACC8);
      int barDark = c(0x4A5068);
      for (int i = 0; i < N; i++) {
         for (int k = 0; k < 3; k++) {
            int pos = k == 0 ? 0 : (k == 1 ? 5 : 10);
            set(p, i, pos, bar);
            set(p, pos, i, bar);
            set(p, i, pos + 1, i == 0 ? barLight : barDark);
            set(p, pos + 1, i, barDark);
         }
      }

      for (int i = 0; i < N; i++) {
         set(p, i, 15, bar);
         set(p, 15, i, bar);
         set(p, i, 0, barLight);
         set(p, 0, i, barLight);
      }

      java.util.Random r = rnd(43);
      for (int i = 0; i < 26; i++) {
         int x = 1 + r.nextInt(14);
         int y = 1 + r.nextInt(14);
         if (x % 5 > 1 && y % 5 > 1 && x < 15 && y < 15) {
            set(p, x, y, c(0x0A0A10));
         }
      }

      return p;
   }

   // ------------------------------------------------------------------ water and lava (16 frames that loop)
   /** One frame of the water: two layers of ripples that drift against each other (the noise is sampled along a circle, so frame 15 flows into frame 0). */
   static int[] waterFrame(int f) {
      double a = f / 16.0 * Math.PI * 2.0;
      float ax = (float)(Math.cos(a) * 3.0);
      float ay = (float)(Math.sin(a) * 3.0);
      float bx = (float)(Math.cos(a * 2.0 + 1.0) * 1.5);
      float by = (float)(Math.sin(a * 2.0 + 1.0) * 1.5);
      int[] pal = {c(0x1F4CC0), c(0x2658D4), c(0x2F66E2), c(0x417AEE), c(0x5F94F7)};
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float v = vn(x + 0.5F + ax, y + 0.5F + ay, 4, 301) * 0.5F + vn(x + 0.5F - bx, y + 0.5F + by, 8, 302) * 0.32F + vn(x + 0.5F + bx, y + 0.5F - ay, 8, 303) * 0.18F;
            p[y * N + x] = step(pal, contrast(v, 2.3F));
         }
      }

      return p;
   }

   /** One frame of the lava: slow blotches of red, orange and yellow with dark crust patches. */
   static int[] lavaFrame(int f) {
      double a = f / 16.0 * Math.PI * 2.0;
      float ax = (float)(Math.cos(a) * 2.0);
      float ay = (float)(Math.sin(a) * 2.0);
      float bx = (float)(Math.cos(a + 2.0) * 1.0);
      float by = (float)(Math.sin(a + 2.0) * 1.0);
      int[] pal = {c(0x7A1404), c(0xB82A06), c(0xE0480A), c(0xF47A12), c(0xFFA824), c(0xFFD257)};
      int[] p = new int[N * N];
      for (int y = 0; y < N; y++) {
         for (int x = 0; x < N; x++) {
            float v = vn(x + 0.5F + ax, y + 0.5F - ay, 4, 311) * 0.55F + vn(x + 0.5F - bx, y + 0.5F + by, 8, 312) * 0.3F + hash(x, y, 313) * 0.15F;
            p[y * N + x] = step(pal, contrast(v, 2.1F));
         }
      }

      return p;
   }
}
