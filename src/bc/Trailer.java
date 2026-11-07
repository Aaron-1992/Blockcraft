package bc;

/**
 * V6.30: a one-minute cinematic trailer that plays INSIDE the game.
 *
 * <p>This is not a video file. It is eight scripted camera moves through a real,
 * freshly generated world, with letterbox bars, hard cuts, title cards, a
 * time-of-day arc, a forced storm, a four-act soundtrack and foley stings from
 * the real CC0 recordings shipped in V6.29. It runs for exactly 60 seconds, is
 * skippable with any key or mouse click, and restores the game exactly where it
 * was (title panorama or a live world) when it ends.</p>
 *
 * <p>Everything is deterministic: the seed, the shot table and the camera path
 * are fixed, so the same trailer plays every time and the pure helpers can be
 * verified headlessly. No Mojang code or assets are used; the logo, music and
 * sounds are the ones already in this game.</p>
 */
final class Trailer {
   /** Exact running time, in seconds. */
   static final double LENGTH = 60.0;
   /** A fixed seed so the landscape is always the same. */
   static final long SEED = 20261108L;

   /** One scripted camera move. Angles are degrees, distances/heights are blocks. */
   static final class Shot {
      final double start, end;
      final String title, sub;
      final double angle, radius, travel, heading;
      final double h0, h1;
      final long sky0, sky1;
      final int weather;     // 0 = clear, 3 = thunderstorm
      final String cue;      // real recorded foley played on the cut
      final int act;         // soundtrack act 0..3

      Shot(double start, double end, String title, String sub, double angle, double radius,
         double travel, double heading, double h0, double h1, long sky0, long sky1,
         int weather, String cue, int act) {
         this.start = start;
         this.end = end;
         this.title = title;
         this.sub = sub;
         this.angle = angle;
         this.radius = radius;
         this.travel = travel;
         this.heading = heading;
         this.h0 = h0;
         this.h1 = h1;
         this.sky0 = sky0;
         this.sky1 = sky1;
         this.weather = weather;
         this.cue = cue;
         this.act = act;
      }

      double length() {
         return this.end - this.start;
      }
   }

   /** Eight 7.5-second shots = exactly one minute. */
   static final Shot[] SHOTS = {
      new Shot(0.0, 7.5, "A WORLD OF YOUR OWN", "Every block is yours",
         20.0, 18.0, 64.0, 35.0, 38.0, 26.0, 900L, 3200L, 0, "wind_gust", 0),
      new Shot(7.5, 15.0, "FORESTS, MOUNTAINS, OCEANS", "28 biomes in one seed",
         105.0, 46.0, 72.0, 120.0, 16.0, 11.0, 3200L, 6000L, 0, "leaf_rustle", 0),
      new Shot(15.0, 22.5, "WEATHER WITH A HORIZON", "Storms you can see coming",
         195.0, 30.0, 58.0, 200.0, 24.0, 15.0, 6000L, 7600L, 3, "cave_rumble", 1),
      new Shot(22.5, 30.0, "BENEATH THE SURFACE", "1.20 caves with real entrances",
         270.0, 60.0, 66.0, 285.0, 11.0, 6.0, 7600L, 10500L, 0, "cave_wind", 1),
      new Shot(30.0, 37.5, "BUILD ANYTHING", "Redstone, machines, note blocks",
         330.0, 24.0, 84.0, 15.0, 18.0, 30.0, 10500L, 12800L, 0, "geode_chime", 2),
      new Shot(37.5, 45.0, "PLAY TOGETHER", "Mini games, maps and multiplayer",
         60.0, 52.0, 88.0, 75.0, 26.0, 14.0, 12800L, 15200L, 0, "wind_gust", 2),
      new Shot(45.0, 52.5, "AND WHEN NIGHT FALLS", "Moonlight, shadows and survival",
         150.0, 36.0, 62.0, 165.0, 18.0, 10.0, 15200L, 18500L, 0, "cave_drip", 3),
      new Shot(52.5, 60.0, "BLOCKCRAFT", "V6.30  -  PLAY IT NOW",
         240.0, 20.0, 44.0, 250.0, 24.0, 34.0, 18500L, 22500L, 0, "geode_chime", 3)
   };

   /** The four soundtrack acts, all tracks already in the game's soundbank. */
   static final String[] ACT_TRACKS = {
      "lce_menu_2", "lce_battle_city_1", "lce_battle_4", "lce_credits"
   };

   private Trailer(Game g) {
      this.g = g;
   }

   // ------------------------------------------------------------------ pure, testable helpers
   static double smooth(double t) {
      double p = Math.max(0.0, Math.min(1.0, t));
      return p * p * (3.0 - 2.0 * p);
   }

   static double lerp(double a, double b, double t) {
      return a + (b - a) * t;
   }

   /** Shot index for a time in seconds, or SHOTS.length when the trailer is over. */
   static int shotAt(double t) {
      if (t <= 0.0) return 0;
      for (int i = 0; i < SHOTS.length; i++) {
         if (t < SHOTS[i].end) return i;
      }
      return SHOTS.length;
   }

   /** Interpolated time of day for a shot (a continuous 60-second time-lapse). */
   static long skyAt(int shot, double local) {
      Shot s = SHOTS[Math.max(0, Math.min(SHOTS.length - 1, shot))];
      return Math.round(lerp(s.sky0, s.sky1, smooth(local)));
   }

   static int actAt(int shot) {
      return SHOTS[Math.max(0, Math.min(SHOTS.length - 1, shot))].act;
   }

   /** Terrain height at a world column, never below sea level. */
   static double ground(Gen gen, double x, double z) {
      if (gen == null) return 70.0;
      try {
         Gen.Col c = gen.column((int)Math.floor(x), (int)Math.floor(z));
         return c == null ? 70.0 : Math.max(64.0, c.h);
      } catch (Throwable ignored) {
         return 70.0;
      }
   }

   /**
    * Camera for a shot at local progress 0..1.
    * Returns {x, eyeY, z, yaw, pitch}; the player's feet are eyeY - Player.eye.
    */
   static double[] cameraAt(Gen gen, double baseX, double baseZ, int shot, double local) {
      Shot s = SHOTS[Math.max(0, Math.min(SHOTS.length - 1, shot))];
      double p = smooth(local);
      double a = Math.toRadians(s.angle);
      double hd = Math.toRadians(s.heading);
      double x = baseX + Math.cos(a) * s.radius + Math.cos(hd) * s.travel * p;
      double z = baseZ + Math.sin(a) * s.radius + Math.sin(hd) * s.travel * p;
      double eyeY = ground(gen, x, z) + lerp(s.h0, s.h1, p);
      // Look at a point on the ground ahead: the camera always sees real terrain.
      double tx = x + Math.cos(hd) * 26.0;
      double tz = z + Math.sin(hd) * 26.0;
      double ty = ground(gen, tx, tz) + 5.0;
      double dx = tx - x, dy = ty - eyeY, dz = tz - z;
      double horiz = Math.sqrt(dx * dx + dz * dz);
      double yaw = Math.toDegrees(Math.atan2(-dx, dz));
      double pitch = -Math.toDegrees(Math.atan2(dy, horiz));
      return new double[]{x, eyeY, z, yaw, Math.max(-72.0, Math.min(72.0, pitch))};
   }

   /** Human-readable shot list, also used by --trailer-plan and the release notes. */
   static String plan() {
      StringBuilder b = new StringBuilder();
      b.append("BLOCKCRAFT V").append(Game.VERSION_STR).append(" - CINEMATIC TRAILER\n");
      b.append("length=").append(LENGTH).append("s  shots=").append(SHOTS.length)
         .append("  seed=").append(SEED).append("  letterbox + title cards + 4-act soundtrack\n\n");
      for (int i = 0; i < SHOTS.length; i++) {
         Shot s = SHOTS[i];
         b.append(String.format("%02d  %5.1fs - %5.1fs  %-28s %s%n", i + 1, s.start, s.end,
            s.title, s.sub));
         b.append(String.format("      sky %5d -> %5d  weather=%d  height %.0f -> %.0f  travel %.0f  act=%d  foley=%s%n",
            s.sky0, s.sky1, s.weather, s.h0, s.h1, s.travel, s.act, s.cue));
      }
      return b.toString();
   }

   // ------------------------------------------------------------------ runtime state
   final Game g;
   double time;
   double tickAcc;
   int shot = -1;
   double baseX, baseZ;
   World ownWorld;         // dedicated throw-away world when started from the menus
   Player ownCam;
   World savedWorld;
   Player savedPlayer;
   Game.S returnScreen = Game.S.TITLE;
   int savedView;
   int savedLook;
   boolean savedOptWeather, savedHideGui;
   long savedDayTime;
   double savedX, savedY, savedZ, savedPx, savedPy, savedPz, savedVx, savedVy, savedVz;
   float savedYaw, savedPitch, savedEye;
   boolean savedFlying;
   boolean savedWeatherEnabled, savedWeatherSuppressed, savedWeatherCalm, savedWeatherNoLightning;
   long savedWeatherCalmUntil, savedWeatherForceEnd;
   int savedWeatherForceMode;
   float savedWeatherForceAmt;

   private void savePlayer(Player p) {
      this.savedX = p.x; this.savedY = p.y; this.savedZ = p.z;
      this.savedPx = p.px; this.savedPy = p.py; this.savedPz = p.pz;
      this.savedVx = p.vx; this.savedVy = p.vy; this.savedVz = p.vz;
      this.savedYaw = p.yaw; this.savedPitch = p.pitch; this.savedEye = p.eye;
      this.savedFlying = p.flying;
   }

   private void restorePlayer(Player p) {
      p.setPos(this.savedX, this.savedY, this.savedZ);
      p.px = this.savedPx; p.py = this.savedPy; p.pz = this.savedPz;
      p.vx = this.savedVx; p.vy = this.savedVy; p.vz = this.savedVz;
      p.yaw = this.savedYaw; p.pitch = this.savedPitch; p.eye = this.savedEye;
      p.flying = this.savedFlying;
      p.updateBox();
   }

   private void saveWeather(World w) {
      if (w == null) return;
      Weather q = w.weather;
      this.savedWeatherEnabled = q.enabled;
      this.savedWeatherSuppressed = q.suppressed;
      this.savedWeatherCalm = q.calm;
      this.savedWeatherNoLightning = q.noLightning;
      this.savedWeatherCalmUntil = q.calmUntil;
      this.savedWeatherForceEnd = q.forceEnd;
      this.savedWeatherForceMode = q.forceMode;
      this.savedWeatherForceAmt = q.forceAmt;
      this.savedDayTime = w.dayTime;
   }

   private void restoreWeather(World w) {
      if (w == null) return;
      Weather q = w.weather;
      q.enabled = this.savedWeatherEnabled;
      q.suppressed = this.savedWeatherSuppressed;
      q.calm = this.savedWeatherCalm;
      q.noLightning = this.savedWeatherNoLightning;
      q.calmUntil = this.savedWeatherCalmUntil;
      q.forceEnd = this.savedWeatherForceEnd;
      q.forceMode = this.savedWeatherForceMode;
      q.forceAmt = this.savedWeatherForceAmt;
      q.tickSnap = q.snap(q.ticks / 20.0);
      w.dayTime = this.savedDayTime;
   }

   /** A dry, varied place near the origin: a quick deterministic search, not a long worker job. */
   static double[] anchor(Gen gen) {
      double best = -1.0;
      double bx = 0.0, bz = 0.0;
      for (int i = 0; i < 64; i++) {
         double a = i * 2.399963229728653;   // golden angle
         double r = 20.0 + i * 6.0;
         double x = Math.cos(a) * r, z = Math.sin(a) * r;
         double h = ground(gen, x, z);
         int lo = (int)h, hi = (int)h;
         for (int k = 0; k < 4; k++) {
            double d = k * Math.PI / 2.0;
            int hh = (int)ground(gen, x + Math.cos(d) * 24.0, z + Math.sin(d) * 24.0);
            lo = Math.min(lo, hh);
            hi = Math.max(hi, hh);
         }
         double score = (hi - lo) + 0.12 * Math.min(60.0, Math.abs(h - 88.0));
         if (score > best) {
            best = score;
            bx = x;
            bz = z;
         }
      }
      return new double[]{bx, bz};
   }

   private void findAnchor(Gen gen) {
      double[] a = anchor(gen);
      this.baseX = a[0];
      this.baseZ = a[1];
   }

   private void installWorld() {
      WorldSettings ws = new WorldSettings();
      ws.type = WorldSettings.DEFAULT;
      ws.aquatic = true;
      ws.terrainVer = 3;
      ws.caveVer = 4;
      ws.biomeBorders = 1;
      World w = new World(SEED, null, Dim.OVERWORLD, ws);
      w.saveEnabled = false;
      w.weather.enabled = true;
      w.weather.startFair();
      w.disasters.enabled = false;
      w.game = this.g;
      w.dayTime = SHOTS[0].sky0;
      if (this.g.rend != null) {
         w.unloadHook = this.g.rend::onUnload;
      }
      this.findAnchor(w.gen);
      Player cam = new Player();
      cam.setMode(1);
      cam.flying = true;
      this.ownWorld = w;
      this.ownCam = cam;
      this.g.world = w;
      this.g.player = cam;
      if (this.g.rend != null) {
         this.g.rend.setLod(w);
      }
      int cx = (int)Math.floor(this.baseX) >> 4;
      int cz = (int)Math.floor(this.baseZ) >> 4;
      w.updateChunks(cx, cz, this.g.opt.renderDistance + 1, 64, 12000000L);
   }

   /** Starts the 60-second trailer. Works from the title screen and from a live world. */
   static void start(Game g) {
      if (g.trailer != null) return;
      Trailer t = new Trailer(g);
      g.trailer = t;
      t.returnScreen = g.screen;
      t.savedView = g.view;
      t.savedHideGui = g.hideGui;
      t.savedWorld = g.world;
      t.savedPlayer = g.player;
      t.saveWeather(g.world);
      if (g.opt != null) {
         t.savedLook = g.opt.look;
         t.savedOptWeather = g.opt.weather;
         g.opt.look = Look.CINEMATIC;   // cinematic colour grading while the trailer runs
         g.opt.weather = true;          // the storm shot must be visible
      }
      boolean inGame = g.screen == Game.S.PLAY && g.world != null && g.player != null;
      if (inGame) {
         t.savePlayer(g.player);
         t.baseX = g.player.x;
         t.baseZ = g.player.z;
         g.world.weather.enabled = true;   // let the storm shot render in a live world too
      } else {
         Panorama.stop(g);             // the turning menu world is replaced by the trailer world
         t.installWorld();
      }
      g.view = 0;
      g.hideGui = true;
      g.setScreen(Game.S.TRAILER);
   }

   /** Ends the trailer (or skips it) and restores the previous game state. */
   static void stop(Game g) {
      Trailer t = g.trailer;
      if (t == null) return;
      g.trailer = null;
      if (g.opt != null) {
         g.opt.look = t.savedLook;
         g.opt.weather = t.savedOptWeather;
      }
      g.view = t.savedView;
      g.hideGui = t.savedHideGui;
      if (t.ownWorld != null) {
         if (g.rend != null) {
            g.rend.setLod(null);
            g.rend.clearWorldMeshes(t.ownWorld);
         }
         for (Chunk c : t.ownWorld.chunks.values()) {
            c.unloaded = true;
         }
         t.ownWorld.chunks.clear();
         t.ownWorld.shutdown();
         if (g.world == t.ownWorld) {
            g.world = t.savedWorld;
            g.player = t.savedPlayer;
         }
         if (t.savedWorld == null) {
            Panorama.start(g);        // back to the turning landscape behind the menus
         } else if (g.rend != null) {
            g.rend.setLod(t.savedWorld);
         }
      } else {
         if (t.savedPlayer != null) t.restorePlayer(t.savedPlayer);
         t.restoreWeather(t.savedWorld);
      }
      g.setScreen(t.returnScreen);
   }

   /** Called every frame from Game.update(), after the panorama (if any). dt is seconds. */
   void update(double dt) {
      this.time += dt;
      if (this.time >= LENGTH) {
         stop(this.g);
         return;
      }
      this.tickAcc += dt * 20.0;
      for (int n = 0; this.tickAcc >= 1.0 && n < 6; n++) {
         this.tickAcc -= 1.0;
         this.g.tickCount++;
         if (this.g.world != null) this.g.world.weather.tick();
      }
      int s = shotAt(this.time);
      if (s != this.shot) {
         this.shot = s;
         Shot sh = SHOTS[s];
         if (sh.cue != null) SoundEngine.playUi(sh.cue, 0.55F, 1.0F);
         if (this.g.world != null) {
            this.g.world.weather.force(sh.weather == 3 ? 3 : 1, 30L);
            this.g.world.weather.instant();
         }
      }
      if (this.g.world == null || this.g.player == null) return;
      Shot sh = SHOTS[s];
      double local = (this.time - sh.start) / sh.length();
      double[] c = cameraAt(this.g.world.gen, this.baseX, this.baseZ, s, local);
      Player p = this.g.player;
      p.setPos(c[0], c[1] - p.eye, c[2]);
      p.yaw = (float)c[3];
      p.pitch = (float)c[4];
      this.g.world.dayTime = skyAt(s, local);
      this.g.partial = (float)this.tickAcc;
   }

   /** True while the trailer is on screen (used by the music selector). */
   static boolean active(Game g) {
      return g != null && g.trailer != null && g.screen == Game.S.TRAILER;
   }

   static String music(Game g) {
      int act = g != null && g.trailer != null ? actAt(g.trailer.shot < 0 ? 0 : g.trailer.shot) : 0;
      return ACT_TRACKS[Math.max(0, Math.min(ACT_TRACKS.length - 1, act))];
   }

   // ------------------------------------------------------------------ the cinematic overlay
   private static int argb(float a, int r, int g, int b) {
      int alpha = Math.max(0, Math.min(255, Math.round(a * 255.0F)));
      return (alpha << 24) | (r << 16) | (g << 8) | b;
   }

   private static float cardAlpha(double sec, double len) {
      float in = (float)Math.min(1.0, Math.max(0.0, sec / 0.65));
      float out = (float)Math.min(1.0, Math.max(0.0, (len - sec - 1.0) / 0.85));
      return Math.min(in, out);
   }

   /** Drawn from Game.render() after the world, inside gui.begin(). */
   static void draw(Game g) {
      Trailer t = g.trailer;
      Gui gui = g.gui;
      float w = gui.w, h = gui.h;
      double time = t == null ? 0.0 : t.time;
      int shot = shotAt(time);
      double local = 0.0;
      if (shot < SHOTS.length) {
         Shot sh = SHOTS[shot];
         local = (time - sh.start) / sh.length();
      }

      // 2.39:1 letterbox + a thin gold rule at each edge.
      float bar = Math.max(8.0F, h * 0.115F);
      gui.rect(0.0F, 0.0F, w, bar, 0xE6000000);
      gui.rect(0.0F, h - bar, w, bar, 0xE6000000);
      gui.rect(0.0F, bar - 1.0F, w, 1.0F, 0x50FFD84A);
      gui.rect(0.0F, h - bar, w, 1.0F, 0x50FFD84A);

      // Slate: shot number, running timecode, skip hint and a 60-second progress line.
      gui.text("CINEMATIC TRAILER", 8.0F, bar + 6.0F, 0xFFB9C4D8, true);
      if (shot < SHOTS.length) {
         String chapter = String.format("%02d / %02d", shot + 1, SHOTS.length);
         gui.text(chapter, 8.0F, bar + 17.0F, 0xFFFFDF58, true);
      }
      int sec = (int)Math.floor(time);
      String tc = String.format("0:%02d / 1:00", Math.min(59, sec));
      gui.text(tc, w - Gui.textWidth(tc) - 8.0F, bar + 6.0F, 0xFFB9C4D8, true);
      gui.centered("ESC or click to skip", w / 2.0F, h - bar - 15.0F, 0x80FFFFFF, true, 1.0F);
      float frac = (float)Math.max(0.0, Math.min(1.0, time / LENGTH));
      gui.rect(0.0F, h - bar - 3.0F, w, 2.0F, 0x40000000);
      gui.rect(0.0F, h - bar - 3.0F, w * frac, 2.0F, 0xC0FFD84A);

      // Title cards (in over 0.65 s, out before the cut).
      if (shot < SHOTS.length) {
         Shot sh = SHOTS[shot];
         float a = cardAlpha(local * sh.length(), sh.length());
         if (a > 0.01F) {
            gui.centered(sh.title, w / 2.0F, h * 0.37F, argb(a, 255, 246, 205), true, 2.0F);
            gui.centered(sh.sub, w / 2.0F, h * 0.37F + 24.0F, argb(a * 0.92F, 200, 214, 235), true, 1.0F);
            float lw = Math.min(180.0F, w * 0.25F);
            gui.rect(w / 2.0F - lw / 2.0F, h * 0.37F + 38.0F, lw, 1.0F, argb(a * 0.6F, 255, 216, 74));
         }
      }

      // Hard cuts: a brief flash of black at the start of every shot.
      if (shot > 0 && shot < SHOTS.length) {
         double flash = local * SHOTS[shot].length();
         if (flash < 0.22) {
            gui.rect(0.0F, 0.0F, w, h, argb((float)(1.0 - flash / 0.22), 0, 0, 0));
         }
      }

      // Opening fade from black and the final fade into the title card.
      if (time < 1.1) {
         gui.rect(0.0F, 0.0F, w, h, argb((float)(1.0 - time / 1.1), 0, 0, 0));
      }
      if (shot == SHOTS.length - 1 && time > 55.6) {
         float fa = (float)Math.min(1.0, Math.max(0.0, (time - 55.6) / 1.6));
         gui.rect(0.0F, 0.0F, w, h, argb(fa * 0.94F, 0, 0, 0));
         if (fa > 0.02F) {
            float cell = Logo.fit(w - 36.0F, 6.0F);
            float top = h * 0.30F;
            Logo.draw(gui, w / 2.0F, top, cell, time);
            gui.centered("A ONE-MINUTE IN-GAME TRAILER", w / 2.0F, top + Logo.height(cell) + 14.0F,
               argb(fa, 255, 223, 88), true, 1.0F);
            gui.centered("V" + Game.VERSION_STR + "  -  PLAY IT NOW", w / 2.0F, top + Logo.height(cell) + 28.0F,
               argb(fa, 220, 230, 245), true, 1.0F);
            gui.centered("Original game. No Mojang code or assets.", w / 2.0F, h - bar - 34.0F,
               argb(fa * 0.8F, 170, 182, 200), true, 1.0F);
         }
      }
      if (time > LENGTH - 0.7) {
         gui.rect(0.0F, 0.0F, w, h, argb((float)((time - (LENGTH - 0.7)) / 0.7), 0, 0, 0));
      }
   }
}
