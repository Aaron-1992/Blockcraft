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

   final Game g;
   World world;
   Player cam;
   Gen gen;
   volatile double[] vista;
   boolean installed;
   double acc;
   double ticks;
   float shown;
   long installedAt;
   double yaw0;
   double[] spot;

   Panorama(Game g) {
      this.g = g;
   }

   /** Starts the panorama (the place is searched on a worker thread: the first frames show the plain menu background). */
   static void start(Game g) {
      if (g.pano != null || "off".equals(System.getProperty("bc.pano"))) {
         return;
      }

      Panorama p = new Panorama(g);
      g.pano = p;
      Thread t = new Thread(() -> {
         WorldSettings ws = new WorldSettings();
         p.gen = Dim.makeGen(Dim.OVERWORLD, SEED, ws);
         p.vista = pick(p.gen);
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
      if (p.installed && p.world != null && g.world == p.world) {
         for (Chunk c : p.world.chunks.values()) {
            c.unloaded = true;
            g.rend.onUnload(c);
         }

         p.world.chunks.clear();
         p.world.shutdown();
         g.world = null;
         g.player = null;
      }
   }

   void install() {
      double[] v = this.vista;
      this.spot = v;
      WorldSettings ws = new WorldSettings();
      World w = new World(SEED, null, Dim.OVERWORLD, ws);
      w.saveEnabled = false;
      w.weather.enabled = false;
      w.disasters.enabled = false;
      w.game = this.g;
      w.dayTime = 3000L;
      w.weather.startFair();
      w.unloadHook = this.g.rend::onUnload;
      this.world = w;
      this.cam = new Player();
      this.cam.setMode(3);
      this.cam.setPos(v[1], v[2] - this.cam.eye, v[3]);
      this.yaw0 = v[4];
      this.g.world = w;
      this.g.player = this.cam;
      this.g.rend.setLod(w);
      this.installed = true;
      this.installedAt = System.nanoTime();
   }

   /** Called every frame while the panorama is the background (the menus). */
   void update(double dt) {
      if (!this.installed) {
         if (this.vista == null || this.g.window == 0L) {
            return;
         }

         this.install();
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
      c.yaw = (float)(this.yaw0 + t * TURN);
      c.pitch = (float)(PITCH + 3.0 * Math.sin(t / 310.0));
      double bob = 1.4 * Math.sin(t / 230.0);
      c.setPos(this.spot[1], this.spot[2] - c.eye + bob, this.spot[3]);
      // fade in once the land around is there and has been drawn
      int cx = (int)Math.floor(this.spot[1]) >> 4;
      int cz = (int)Math.floor(this.spot[3]) >> 4;
      boolean ready = this.world.loadedAround(cx, cz, 3) >= 36 && this.meshed(cx, cz, 2);
      if (ready || this.shown > 0.0F) {
         this.shown = Math.min(1.0F, this.shown + (float)dt * 0.9F);
      }
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

            for (Chunk.Section s : ch.sections) {
               if (s.dirty || s.inFlight) {
                  return false;
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

   /** The time of day the sky is drawn for: a slow morning that never turns into night. */
   long skyTime() {
      return 3200L + (long)(1800.0 * Math.sin(this.ticks / 4200.0));
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
            Gen.Col c = gen.column(x + (i - n / 2) * step, z + (j - n / 2) * step);
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

      int centre = hs[(n / 2) * n + n / 2];
      if (centre < Gen.SEA + 3 || centre > 112) {
         return null;
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
