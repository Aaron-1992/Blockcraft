package bc;

/**
 * Update 3.13: the background of the title screen and of the menus that open from it - a camera that slowly turns over real terrain, in the middle of
 * a throw-away world that is never saved. The place is found by looking at the terrain of a fixed seed (relief, water, forest, variety), so it follows
 * the world generator when that changes. While a real world is open there is no panorama; Game.quitToTitle brings it back.
 */
public final class Panorama {
   /** The world behind the menus. */
   static final long SEED = 6120L;
   /** Degrees of turning per tick (a full turn takes a little under three minutes). */
   static final double TURN = 0.11;
   static final double PITCH = 7.0;

   /**
    * Update 6.18: selectable title-screen landscapes. The Classic preset keeps the original seed, scoring,
    * turning speed and sky exactly; the others find their own best place in a different generated world.
    * bias: 0 = none, 1 = prefer warm shallow oceans (reef), 2 = prefer high snowy relief (peaks).
    */
   static final class Preset {
      final String name;
      final long seed;
      final int type;
      final boolean aquatic;
      final double turn, pitch;
      final long skyShift;
      final int bias;
      final int index;

      Preset(int index, String name, long seed, int type, boolean aquatic, double turn, double pitch, long skyShift, int bias) {
         this.index = index;
         this.name = name;
         this.seed = seed;
         this.type = type;
         this.aquatic = aquatic;
         this.turn = turn;
         this.pitch = pitch;
         this.skyShift = skyShift;
         this.bias = bias;
      }

      WorldSettings settings() {
         WorldSettings ws = new WorldSettings();
         ws.type = this.type;
         ws.aquatic = this.aquatic;
         return ws;
      }
   }

   static final Preset[] PRESETS = {
      new Preset(0, "Classic", SEED, WorldSettings.DEFAULT, true, TURN, PITCH, 0L, 0),
      new Preset(1, "Coral Coast", 20261018L, WorldSettings.DEFAULT, true, 0.08, 4.5, 1100L, 1),
      new Preset(2, "Snowy Peaks", 90210L, WorldSettings.AMPLIFIED, true, 0.14, 9.0, -1300L, 2)
   };
   static final int CYCLE = PRESETS.length;
   static final int COUNT = PRESETS.length + 1;
   static final java.util.concurrent.ConcurrentHashMap<Integer, double[]> VISTA_CACHE = new java.util.concurrent.ConcurrentHashMap<>();
   /** Update 6.20: the 2D landscape behind the menus, cached as plain RGB (alpha is added while fading). */
   static final java.util.concurrent.ConcurrentHashMap<Integer, int[]> FLAT_RGB = new java.util.concurrent.ConcurrentHashMap<>();
   static final int FLAT_W = 320;
   static final int FLAT_H = 180;
   static int cycleIndex;

   static String label(int id) {
      int i = Options.clamp(id, 0, COUNT - 1);
      return i == CYCLE ? "Cycle" : PRESETS[i].name;
   }

   static Preset resolve(Game g) {
      int id = g.opt == null ? 0 : Options.clamp(g.opt.panorama, 0, COUNT - 1);
      if (id == CYCLE) {
         cycleIndex++;
         return PRESETS[Math.floorMod(cycleIndex, PRESETS.length)];
      }
      return PRESETS[id];
   }

   /** Change the landscape now (only while a panorama is actually running), and remember the choice. */
   static void switchTo(Game g) {
      if (g.opt != null) g.opt.save();
      if (g.pano == null) return; // a real world is open: the new choice applies at the next title screen
      // Update 6.20: resolve() advances Cycle exactly once, in start(); doing it here skipped a preset.
      stop(g);
      start(g);
   }

   final Game g;
   /** Update 6.18: which landscape this running panorama shows. */
   Preset preset = PRESETS[0];
   World world;
   Player cam;
   Gen gen;
   volatile double[] vista;
   boolean installed;
   /** Update 6.20: the 3D world could not be installed; the 2D landscape stays on screen. */
   boolean broken;
   double acc;
   double ticks;
   float shown;
   long installedAt;
   double yaw0;
   double[] spot;
   /** Update 6.20: a real landscape drawn while the 3D world loads (and if it never can). */
   Gui.Image flat;
   int flatAlpha = -1;

   Panorama(Game g) {
      this.g = g;
   }

   static volatile double[] cachedDefaultVista;

   /** Starts the panorama (the place is searched on a worker thread: the first frames show the plain menu background). */
   static void start(Game g) {
      if (g.pano != null || "off".equals(System.getProperty("bc.pano"))) {
         return;
      }

      Preset pr = resolve(g);
      Panorama p = new Panorama(g);
      p.preset = pr;
      g.pano = p;
      double[] cached = VISTA_CACHE.get(pr.index);
      if (cached != null) {
         p.vista = cached;
         return;
      }
      Thread t = new Thread(() -> {
         // Update 6.20: a failed vista search must not leave vista null forever - that kept install()
         // from ever running and left the title screen with no landscape at all.
         try {
            p.gen = Dim.makeGen(Dim.OVERWORLD, pr.seed, pr.settings());
            double[] picked = pick(p.gen, pr);
            VISTA_CACHE.put(pr.index, picked);
            if (pr.index == 0) cachedDefaultVista = picked;
            p.vista = picked;
         } catch (Throwable e) {
            System.err.println("panorama: vista search failed, using the fallback view: " + e);
            p.vista = emergency(pr);
         }
      }, "panorama-vista");
      t.setDaemon(true);
      t.start();
   }

   /** Ends the panorama: the throw-away world goes away (a real world takes its place). */
   static void stop(Game g) {
      Panorama p = g.pano;
      if (p == null) {
         return;
      }

      g.pano = null;
      if (g.rend != null) {
         // Update 6.20: do not leave the distant-terrain renderer pointing at the throw-away world.
         g.rend.setLod(null);
      }
      if (p.installed && p.world != null) {
         if (g.rend != null) {
            g.rend.clearWorldMeshes(p.world);
         } else {
            for (Chunk c : p.world.chunks.values()) {
               c.unloaded = true;
            }
         }

         p.world.chunks.clear();
         p.world.shutdown();
         if (g.world == p.world) {
            g.world = null;
            g.player = null;
         }
      }
   }

   /** A place to stand when the generator cannot be searched at all. */
   static double[] emergency(Preset pr) {
      double y = 100.0;
      try {
         y = Dim.makeGen(Dim.OVERWORLD, pr.seed, pr.settings()).column(0, 0).h + 26.0 + 1.62;
      } catch (Throwable ignored) {
      }
      return new double[]{0.0, 0.5, y, 0.5, 0.0};
   }

   void install() {
      double[] v = this.vista;
      this.spot = v;
      World w = new World(this.preset.seed, null, Dim.OVERWORLD, this.preset.settings());
      w.saveEnabled = false;
      w.weather.enabled = false;
      w.disasters.enabled = false;
      w.game = this.g;
      w.dayTime = 3000L + this.preset.skyShift;
      w.weather.startFair();
      if (this.g.rend != null) {
         w.unloadHook = this.g.rend::onUnload;
      }
      this.world = w;
      this.cam = new Player();
      this.cam.setMode(3);
      this.cam.setPos(v[1], v[2] - this.cam.eye, v[3]);
      this.yaw0 = v[4];
      this.g.world = w;
      this.g.player = this.cam;
      if (this.g.rend != null) {
         this.g.rend.setLod(w);
      }
      int cx = (int)Math.floor(v[1]) >> 4;
      int cz = (int)Math.floor(v[3]) >> 4;
      w.updateChunks(cx, cz, this.g.opt.renderDistance + 1, 64, 12000000L);
      this.installed = true;
      this.installedAt = System.nanoTime();
   }

   /** Called every frame while the panorama is the background (the menus). */
   void update(double dt) {
      if (this.broken) {
         return; // the 2D landscape stays visible instead of a black screen
      }

      if (!this.installed) {
         if (this.vista == null || this.g.window == 0L) {
            return;
         }

         try {
            this.install();
         } catch (Throwable e) {
            System.err.println("panorama: could not install the turning world: " + e);
            this.broken = true;
            if (this.g.world == this.world) {
               this.g.world = null;
               this.g.player = null;
            }
            return;
         }
      }

      if (this.g.world != this.world) {
         return; // a real world took over
      }

      this.acc += dt * 20.0;
      for (int n = 0; this.acc >= 1.0 && n < 5; n++) {
         this.acc -= 1.0;
         this.ticks++;
         this.g.tickCount++;
         this.world.dayTime++;
         this.world.weather.tick();
      }

      if (this.acc >= 1.0) {
         this.acc = 0.0;
      }

      this.g.partial = (float)this.acc;
      double t = this.ticks + this.acc;
      Player c = this.cam;
      c.yaw = (float)(this.yaw0 + t * this.preset.turn);
      c.pitch = (float)(this.preset.pitch + 3.0 * Math.sin(t / 310.0));
      double bob = 1.4 * Math.sin(t / 230.0);
      c.setPos(this.spot[1], this.spot[2] - c.eye + bob, this.spot[3]);
      // fade in once the land around is there and has been drawn
      int cx = (int)Math.floor(this.spot[1]) >> 4;
      int cz = (int)Math.floor(this.spot[3]) >> 4;
      // Update 6.20: demand meshing intentionally leaves hidden cave sections dirty forever, so the old
      // "every section of 25 chunks is clean" test could never pass and the fade stayed at black. Use the
      // renderer's view-aware readiness rule and a bounded timeout, and keep a 2D landscape as a fallback.
      boolean loaded = this.world.loadedAround(cx, cz, 3) >= 36;
      double elapsed = (System.nanoTime() - this.installedAt) / 1.0E9;
      boolean ready = readyToFade(loaded, this.meshed(cx, cz, 2), elapsed);
      if (ready || this.shown > 0.0F) {
         this.shown = Math.min(1.0F, this.shown + (float)dt * 0.9F);
      }
   }

   /** The 3D world may fade in once the land is there and either it is meshed or we have waited long enough. */
   static boolean readyToFade(boolean loaded, boolean meshed, double elapsedSeconds) {
      return loaded && (meshed || elapsedSeconds >= 6.0);
   }

   /** The landscape is there and fully visible (the screenshot hooks wait for it). */
   boolean ready() {
      return this.installed && this.shown >= 1.0F;
   }

   boolean meshed(int cx, int cz, int r) {
      for (int dz = -r; dz <= r; dz++) {
         for (int dx = -r; dx <= r; dx++) {
            Chunk ch = this.world.chunk(cx + dx, cz + dz);
            if (ch == null) {
               return false;
            }

            if (this.g.rend != null) {
               // Demand meshing deliberately defers sealed cave sections. The renderer already knows which
               // sections the current view actually needs; waiting for all 16 x 25 sections never worked.
               if (!this.g.rend.meshLoadingReady(this.world, ch)) {
                  return false;
               }
            } else {
               for (Chunk.Section s : ch.sections) {
                  if (s.dirty || s.inFlight) {
                     return false;
                  }
               }
            }
         }
      }

      return true;
   }

   /** 1 = black (nothing is there yet), 0 = the world is fully visible. */
   float fade() {
      return 1.0F - this.shown;
   }

   /**
    * Update 6.20: a guaranteed landscape behind the menus. The 3D panorama needs a generator, chunk
    * loading and meshing before it can be shown; this simple picture needs only a handful of generator
    * columns, so the title screen is never a black rectangle. It fades out as the real world fades in.
    */
   void drawFlat(Gui gui) {
      if (gui == null || this.shown >= 1.0F) return;
      if (this.flat == null) this.flat = new Gui.Image(FLAT_W, FLAT_H);
      int a = Math.round((1.0F - this.shown) * 255.0F / 8.0F) * 8;
      if (a <= 0) return;
      if (a > 255) a = 255;
      if (this.flatAlpha != a) {
         int[] rgb = flatRgb(this.preset == null ? PRESETS[0] : this.preset);
         for (int i = 0; i < this.flat.px.length; i++) {
            this.flat.px[i] = a << 24 | (rgb[i] & 0xFFFFFF);
         }
         this.flat.dirty = true;
         this.flatAlpha = a;
      }
      gui.image(this.flat, 0.0F, 0.0F, gui.w, gui.h);
   }

   /** Cached sky-and-ridge picture of one preset (plain RGB, no alpha). */
   static int[] flatRgb(Preset pr) {
      int[] cached = FLAT_RGB.get(pr.index);
      if (cached != null) return cached;
      int[] px = new int[FLAT_W * FLAT_H];
      int skyTop = pr.bias == 1 ? 0xFF4E9CD8 : pr.bias == 2 ? 0xFF3A6498 : 0xFF4C86D0;
      int skyLow = pr.bias == 1 ? 0xFFF3D9A8 : pr.bias == 2 ? 0xFFC9DCEE : 0xFFBFD9F2;
      for (int y = 0; y < FLAT_H; y++) {
         float t = y / (float)(FLAT_H - 1);
         int r = mixByte(skyTop >> 16 & 255, skyLow >> 16 & 255, t);
         int g = mixByte(skyTop >> 8 & 255, skyLow >> 8 & 255, t);
         int b = mixByte(skyTop & 255, skyLow & 255, t);
         int col = 0xFF000000 | r << 16 | g << 8 | b;
         for (int x = 0; x < FLAT_W; x++) px[y * FLAT_W + x] = col;
      }
      // a soft sun, high on the right
      int sx = FLAT_W * 72 / 100, sy = FLAT_H * 22 / 100;
      for (int y = Math.max(0, sy - 16); y < Math.min(FLAT_H, sy + 16); y++) {
         for (int x = Math.max(0, sx - 16); x < Math.min(FLAT_W, sx + 16); x++) {
            int d2 = (x - sx) * (x - sx) + (y - sy) * (y - sy);
            if (d2 <= 36) px[y * FLAT_W + x] = 0xFFFFF2C0;
            else if (d2 <= 196) px[y * FLAT_W + x] = mix(px[y * FLAT_W + x], 0xFFFFE4A0, 0.35F);
         }
      }
      // a few soft cloud streaks high in the sky
      for (int y = FLAT_H * 18 / 100; y < FLAT_H * 42 / 100; y++) {
         for (int x = 0; x < FLAT_W; x++) {
            double band = Math.sin(x * 0.021 + y * 0.11) + 0.45 * Math.sin(x * 0.057 - y * 0.23);
            if (band > 0.72) px[y * FLAT_W + x] = mix(px[y * FLAT_W + x], 0xFFE8F0FA, 0.16F);
         }
      }
      Gen gen = null;
      try {
         gen = Dim.makeGen(Dim.OVERWORLD, pr.seed, pr.settings());
      } catch (Throwable ignored) {
      }
      double yaw = Math.toRadians(pr.index * 47.0);
      // Four overlapping ridge layers. The generator gives the broad land/sea shapes; a little
      // higher-frequency relief keeps the skyline from becoming one flat colour band per biome.
      for (int layer = 0; layer < 4; layer++) {
         double dist = 340.0 - layer * 80.0;
         double scale = 0.28 + layer * 0.14;
         float haze = 0.68F + layer * 0.10F;
         for (int x = 0; x < FLAT_W; x++) {
            double a = yaw + (x / (double)FLAT_W - 0.5) * 1.9;
            int wx = (int)(-Math.sin(a) * dist);
            int wz = (int)(Math.cos(a) * dist);
            int h = 70, biome = Biomes.PLAINS;
            if (gen != null) {
               try {
                  Gen.Col c = gen.column(wx, wz);
                  h = c.h;
                  biome = c.biome;
               } catch (RuntimeException ignored) {
               }
            }
            boolean water = h < Gen.SEA;
            double relief = 9.0 * Math.sin(x * 0.11 + layer * 1.7)
               + 4.5 * Math.sin(x * 0.29 + layer * 0.6)
               + 1.8 * Math.sin(x * 0.73 - layer);
            int shownH = (int)Math.max(h + relief, Gen.SEA);
            int base = water ? 0x2F55B0 : WorldPreview.color(biome);
            if (!water && shownH > 150 && biome != Biomes.DESERT && biome != Biomes.BADLANDS) {
               base = mix(base, 0xF4F8FA, Math.min(1.0F, (shownH - 150) / 40.0F));
            }
            float shade = haze * (0.9F + Math.min(0.2F, (shownH - Gen.SEA) / 400.0F));
            int col = 0xFF000000
               | flatByte((base >> 16 & 255) * shade) << 16
               | flatByte((base >> 8 & 255) * shade) << 8
               | flatByte((base & 255) * shade);
            int top = (int)Math.round(FLAT_H * 0.58 - (shownH - 70) * scale - layer * 3.0);
            top = Math.max(0, Math.min(FLAT_H - 1, top));
            for (int y = top; y < FLAT_H; y++) px[y * FLAT_W + x] = col;
            if (water && top < FLAT_H) px[top * FLAT_W + x] = 0xFF8FC6EE;
         }
      }
      // a dark foreground so the picture has a bottom edge
      for (int y = FLAT_H - 10; y < FLAT_H; y++) {
         float t = (y - (FLAT_H - 10)) / 10.0F;
         for (int x = 0; x < FLAT_W; x++) {
            int i = y * FLAT_W + x;
            px[i] = mix(px[i], 0xFF16241A, t * 0.55F);
         }
      }
      FLAT_RGB.put(pr.index, px);
      return px;
   }

   private static int flatByte(float v) {
      return v < 0.0F ? 0 : v > 255.0F ? 255 : (int)(v + 0.5F);
   }

   private static int mixByte(int a, int b, float t) {
      return Math.max(0, Math.min(255, Math.round(a + (b - a) * t)));
   }

   private static int mix(int a, int b, float t) {
      int r = mixByte(a >> 16 & 255, b >> 16 & 255, t);
      int g = mixByte(a >> 8 & 255, b >> 8 & 255, t);
      int bl = mixByte(a & 255, b & 255, t);
      return 0xFF000000 | r << 16 | g << 8 | bl;
   }

   /** The time of day the sky is drawn for: a slow morning that never turns into night. */
   long skyTime() {
      long base = 3200L + (long)(1800.0 * Math.sin(this.ticks / 4200.0));
      long shifted = base + (this.preset == null ? 0L : this.preset.skyShift);
      return Math.max(1400L, Math.min(5000L, shifted));
   }

   // ------------------------------------------------------------------ finding a place
   /** The best place of the terrain: {score, x, y(eye), z, yaw}. */
   static double[] pick(Gen gen) {
      java.util.List<double[]> all = candidates(gen);
      if (all.isEmpty()) {
         int gh = gen.column(0, 0).h;
         return new double[]{0, 0.5, gh + 20, 0.5, 0};
      }

      return all.get(0);
   }

   /** Update 6.18: pick a place for one of the new landscape presets. */
   static double[] pick(Gen gen, Preset p) {
      java.util.List<double[]> all = candidates(gen, p);
      if (all.isEmpty()) {
         int gh = gen.column(0, 0).h;
         return new double[]{0, 0.5, gh + 20, 0.5, 0};
      }
      return all.get(0);
   }

   /** Every place that can be stood on, the best first. */
   static java.util.List<double[]> candidates(Gen gen) {
      java.util.ArrayList<double[]> all = new java.util.ArrayList<>();
      for (int gz = -1600; gz <= 1600; gz += 160) {
         for (int gx = -1600; gx <= 1600; gx += 160) {
            double[] v = score(gen, gx, gz);
            if (v != null) {
               all.add(v);
            }
         }
      }

      all.sort((p, q) -> Double.compare(q[0], p[0]));
      return all;
   }

   /** The same search, with the preset's small bias added to each score. */
   static java.util.List<double[]> candidates(Gen gen, Preset pr) {
      java.util.ArrayList<double[]> all = new java.util.ArrayList<>();
      for (int gz = -1600; gz <= 1600; gz += 160) {
         for (int gx = -1600; gx <= 1600; gx += 160) {
            double[] v = score(gen, gx, gz, pr);
            if (v != null) {
               all.add(v);
            }
         }
      }
      all.sort((p, q) -> Double.compare(q[0], p[0]));
      return all;
   }

   static double[] score(Gen gen, int x, int z, Preset pr) {
      double[] v = score(gen, x, z);
      if (v != null && pr != null && pr.bias != 0) {
         v[0] += biasScore(gen, x, z, pr.bias);
      }
      return v;
   }

   /** A cheap extra preference over 25 nearby columns; the Classic preset never calls this. */
   private static double biasScore(Gen gen, int x, int z, int bias) {
      int ocean = 0, warm = 0, high = 0, snow = 0;
      for (int dz = -2; dz <= 2; dz++) {
         for (int dx = -2; dx <= 2; dx++) {
            Gen.Col c = gen.column(x + dx * 48, z + dz * 48);
            if (Biomes.ocean(c.biome)) ocean++;
            if (c.biome == Biomes.WARM_OCEAN || c.biome == Biomes.LUKEWARM_OCEAN || c.biome == Biomes.DEEP_LUKEWARM_OCEAN) warm++;
            if (c.h > 140) high++;
            if (Biomes.snowy(c.biome)) snow++;
         }
      }
      if (bias == 1) {
         return 1.1 * warm / 25.0 + 0.7 * ocean / 25.0;
      }
      return 1.3 * high / 25.0 + 0.7 * snow / 25.0;
   }

   static double bell(double x, double mu, double sigma) {
      double d = (x - mu) / sigma;
      return Math.exp(-d * d);
   }

   static boolean forest(int b) {
      return b == Biomes.FOREST || b == Biomes.BIRCH_FOREST || b == Biomes.DARK_FOREST || b == Biomes.FLOWER_FOREST || b == Biomes.TAIGA || b == Biomes.MEGA_TAIGA
         || b == Biomes.JUNGLE || b == Biomes.MEADOW || b == Biomes.SAVANNA;
   }

   /**
    * How good is the view from here? A little water, forest, some variety, hills or mountains on the horizon, and nothing that blocks the view
    * (ground higher than the camera right next to it).
    */
   static double[] score(Gen gen, int x, int z) {
      Gen.Col centreCol = gen.column(x, z);
      int centre = centreCol.h;
      if (centre < Gen.SEA + 3 || centre > 112) {
         return null;
      }

      final int n = 11;
      final int step = 24;
      int[] hs = new int[n * n];
      boolean[] seen = new boolean[Biomes.N];
      int kinds = 0;
      int water = 0;
      int wood = 0;
      int dry = 0;
      int snow = 0;
      for (int j = 0; j < n; j++) {
         for (int i = 0; i < n; i++) {
            Gen.Col c = (i == n / 2 && j == n / 2) ? centreCol : gen.column(x + (i - n / 2) * step, z + (j - n / 2) * step);
            hs[j * n + i] = c.h;
            if (c.h < Gen.SEA + 1) {
               water++;
            }

            if (forest(c.biome)) {
               wood++;
            }

            if (Biomes.dry(c.biome)) {
               dry++;
            }

            if (Biomes.snowy(c.biome)) {
               snow++;
            }

            if (!Biomes.ocean(c.biome) && !seen[c.biome]) {
               seen[c.biome] = true;
               kinds++;
            }
         }
      }

      // stand on the highest ground near the middle, 26 blocks above it (higher than the trees)
      int ground = centre;
      for (int j = n / 2 - 1; j <= n / 2 + 1; j++) {
         for (int i = n / 2 - 1; i <= n / 2 + 1; i++) {
            ground = Math.max(ground, hs[j * n + i]);
         }
      }

      double camY = ground + 26;
      int blocked = 0;
      for (int h : hs) {
         if (h > camY - 8) {
            blocked++;
         }
      }

      int[] sorted = hs.clone();
      java.util.Arrays.sort(sorted);
      double relief = sorted[sorted.length * 9 / 10] - sorted[sorted.length / 10];
      double total = n * n;
      // the horizon: rings of samples around the place
      int farHigh = 0;
      int farWet = 0;
      int farN = 0;
      double[] dirInterest = new double[8];
      double peak = 0.0;
      for (int ring = 0; ring < 3; ring++) {
         double r = 160.0 + ring * 130.0;
         for (int d = 0; d < 16; d++) {
            double a = Math.toRadians(d * 22.5);
            Gen.Col c = gen.column((int)(x - Math.sin(a) * r), (int)(z + Math.cos(a) * r));
            farN++;
            peak = Math.max(peak, c.h - camY);
            if (c.h > camY + 4) {
               farHigh++;
               dirInterest[d / 2] += 1.0;
            }

            if (c.h < Gen.SEA) {
               farWet++;
               dirInterest[d / 2] += 0.6;
            }
         }
      }

      double fh = farHigh / (double)farN;
      double fw = farWet / (double)farN;
      double ws = water / total;
      double score = 1.3 * Math.min(1.0, relief / 40.0) + 1.5 * bell(ws, 0.22, 0.12) + 0.6 * Math.min(kinds, 5) / 5.0 + 0.8 * bell(wood / total, 0.3, 0.2)
         + 1.4 * bell(fh, 0.3, 0.15) + 0.5 * bell(fw, 0.15, 0.12) + 0.7 * Math.min(1.0, peak / 60.0) - 3.0 * blocked / total - 0.9 * dry / total - 0.7 * snow / total;
      // the first direction to look in: where the horizon is most interesting (mountains, water)
      double bestDir = 0.0;
      double bestInterest = -1.0;
      for (int d = 0; d < 8; d++) {
         if (dirInterest[d] > bestInterest) {
            bestInterest = dirInterest[d];
            bestDir = d * 45.0 + 11.25;
         }
      }

      return new double[]{score, x + 0.5, camY + 1.62, z + 0.5, bestDir};
   }
}
