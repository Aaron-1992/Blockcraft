package bc;

/**
 * V6.31: a one-minute cinematic trailer that plays INSIDE the game.
 *
 * <p>V6.30 was one continuous camera flying through the world. This is a real
 * trailer montage: 24 short shots with hard cuts, eight different camera moves
 * (glide, dolly, orbit, crane, dive, static pan, top-down and chase), eight
 * different locations, a cut-up dawn/noon/storm/sunset/night arc, big word
 * cards between bursts of fast cuts, a four-act soundtrack and foley stings
 * from the real CC0 WAV recordings. It runs exactly 60 seconds, is skippable
 * with any key or click, and restores the game exactly where it was.</p>
 *
 * <p>No video file, no Mojang assets: the logo, music and sounds are the ones
 * already in this game and every frame is rendered by the normal renderer.</p>
 */
final class Trailer {
   /** Exact running time, in seconds. */
   static final double LENGTH = 60.0;
   static final long SEED = 20261108L;

   /** Eight distinct camera moves, so no two shots feel like the same flythrough. */
   static final String[] KIND = {
      "Glide", "Dolly", "Orbit", "Crane", "Dive", "Static", "Top-down", "Chase"
   };

   static final class Shot {
      final double start, end;
      final int kind;
      final String title, sub;   // empty = no card (a fast montage cut)
      final int anchor;
      final long sky;
      final int weather;         // 0 clear, 3 thunderstorm
      final String cue;
      final int act;

      Shot(double start, double end, int kind, String title, String sub, int anchor,
         long sky, int weather, String cue, int act) {
         this.start = start; this.end = end; this.kind = kind;
         this.title = title; this.sub = sub; this.anchor = anchor;
         this.sky = sky; this.weather = weather; this.cue = cue; this.act = act;
      }

      double length() { return this.end - this.start; }
   }

   /** 24 shots, hard cuts, exactly 60 seconds. */
   static final Shot[] SHOTS = {
      new Shot(0.0, 2.5, 0, "A WORLD", "of your own", 0, 1000L, 0, "wind_gust", 0),
      new Shot(2.5, 4.5, 1, "", "", 1, 2400L, 0, "leaf_rustle", 0),
      new Shot(4.5, 7.0, 2, "", "", 2, 5200L, 0, "geode_chime", 0),
      new Shot(7.0, 9.0, 3, "", "", 3, 7600L, 0, "cave_wind", 0),
      new Shot(9.0, 12.0, 4, "EXPLORE", "28 biomes, one seed", 4, 6000L, 0, "tree_crash", 0),
      new Shot(12.0, 14.0, 5, "", "", 5, 9000L, 0, "wind_gust", 0),
      new Shot(14.0, 16.5, 6, "", "", 6, 12000L, 3, "cave_rumble", 1),
      new Shot(16.5, 18.5, 7, "", "", 7, 13500L, 3, "stone_crumble", 1),
      new Shot(18.5, 21.5, 0, "BUILD", "blocks, redstone, machines", 2, 11000L, 0, "cave_drip", 1),
      new Shot(21.5, 23.5, 7, "", "", 0, 8000L, 0, "leaf_rustle", 1),
      new Shot(23.5, 26.0, 2, "", "", 5, 6000L, 0, "geode_chime", 1),
      new Shot(26.0, 28.0, 1, "", "", 3, 4000L, 0, "cave_wind", 1),
      new Shot(28.0, 31.0, 3, "DIG DEEPER", "caves that reach daylight", 7, 12500L, 0, "tree_crash", 2),
      new Shot(31.0, 33.0, 6, "", "", 1, 14500L, 0, "wind_gust", 2),
      new Shot(33.0, 35.5, 4, "", "", 6, 16500L, 0, "cave_rumble", 2),
      new Shot(35.5, 37.5, 5, "", "", 4, 18500L, 0, "stone_crumble", 2),
      new Shot(37.5, 40.5, 1, "SURVIVE", "weather, night, mobs", 3, 7500L, 3, "cave_rumble", 2),
      new Shot(40.5, 42.5, 0, "", "", 5, 9500L, 3, "wind_gust", 2),
      new Shot(42.5, 45.0, 2, "", "", 0, 11500L, 0, "cave_drip", 3),
      new Shot(45.0, 47.0, 7, "", "", 2, 13500L, 0, "leaf_rustle", 3),
      new Shot(47.0, 50.0, 5, "TOGETHER", "mini games & multiplayer", 6, 18000L, 0, "geode_chime", 3),
      new Shot(50.0, 52.0, 3, "", "", 4, 20500L, 0, "cave_wind", 3),
      new Shot(52.0, 54.5, 6, "", "", 1, 22500L, 0, "tree_crash", 3),
      new Shot(54.5, 60.0, 0, "BLOCKCRAFT", "V6.31  -  PLAY IT NOW", 7, 1000L, 0, "geode_chime", 3),
   };

   static final String[] ACT_TRACKS = {
      "lce_menu_2", "lce_battle_city_1", "lce_battle_4", "lce_credits"
   };

   private Trailer(Game g) { this.g = g; }

   // ------------------------------------------------------------------ pure, testable helpers
   static double smooth(double t) {
      double p = Math.max(0.0, Math.min(1.0, t));
      return p * p * (3.0 - 2.0 * p);
   }

   static double lerp(double a, double b, double t) { return a + (b - a) * t; }

   static int shotAt(double t) {
      if (t <= 0.0) return 0;
      for (int i = 0; i < SHOTS.length; i++) if (t < SHOTS[i].end) return i;
      return SHOTS.length;
   }

   static long skyAt(int shot, double local) {
      Shot s = SHOTS[Math.max(0, Math.min(SHOTS.length - 1, shot))];
      return s.sky + Math.round(local * 300.0);
   }

   static int actAt(int shot) {
      return SHOTS[Math.max(0, Math.min(SHOTS.length - 1, shot))].act;
   }

   static double ground(Gen gen, double x, double z) {
      if (gen == null) return 70.0;
      try {
         Gen.Col c = gen.column((int)Math.floor(x), (int)Math.floor(z));
         return c == null ? 70.0 : Math.max(64.0, c.h);
      } catch (Throwable ignored) {
         return 70.0;
      }
   }

   /** Eight scattered scenic locations: the montage does NOT stay in one place. */
   static double[] anchor(Gen gen, double baseX, double baseZ, int index) {
      int k = Math.floorMod(index, 8);
      double a = k * 2.399963229728653;
      double r = 90.0 + k * 62.0;
      return new double[]{baseX + Math.cos(a) * r, baseZ + Math.sin(a) * r};
   }

   static double[] anchor(Gen gen) { return anchor(gen, 0.0, 0.0, 0); }

   /**
    * Camera for a shot. Returns {x, eyeY, z, yaw, pitch}. The look target is
    * always real terrain, so every frame contains the generated world.
    */
   static double[] cameraAt(Gen gen, double baseX, double baseZ, int shot, double local) {
      Shot s = SHOTS[Math.max(0, Math.min(SHOTS.length - 1, shot))];
      double[] anc = anchor(gen, baseX, baseZ, s.anchor);
      double p = smooth(local);
      double ang = s.anchor * 0.83 + 0.4;
      double x = anc[0], z = anc[1], eye, tx, ty, tz;
      switch (s.kind) {
         case 0: // Glide: slow lateral establishing shot
            x += Math.cos(ang) * 46.0 * p; z += Math.sin(ang) * 46.0 * p;
            eye = ground(gen, x, z) + lerp(30.0, 24.0, p);
            tx = x + Math.cos(ang) * 38.0; tz = z + Math.sin(ang) * 38.0;
            ty = ground(gen, tx, tz) + 6.0;
            break;
         case 1: // Dolly: fast low forward move
            x += Math.cos(ang) * 78.0 * p; z += Math.sin(ang) * 78.0 * p;
            eye = ground(gen, x, z) + lerp(11.0, 6.0, p);
            tx = x + Math.cos(ang) * 30.0; tz = z + Math.sin(ang) * 30.0;
            ty = ground(gen, tx, tz) + 10.0;
            break;
         case 2: // Orbit: circle a point, always looking at it
            {
               double oa = ang + p * Math.PI * 1.15;
               double rr = 24.0;
               x += Math.cos(oa) * rr; z += Math.sin(oa) * rr;
               eye = ground(gen, x, z) + 17.0;
               tx = anc[0]; tz = anc[1]; ty = ground(gen, tx, tz) + 8.0;
            }
            break;
         case 3: // Crane: rise straight up while panning
            x += Math.cos(ang) * 12.0 * p; z += Math.sin(ang) * 12.0 * p;
            eye = ground(gen, x, z) + lerp(8.0, 46.0, p);
            tx = anc[0] + Math.cos(ang) * 22.0; tz = anc[1] + Math.sin(ang) * 22.0;
            ty = ground(gen, tx, tz) + 4.0;
            break;
         case 4: // Dive: descend towards the ground
            x += Math.cos(ang) * 34.0 * p; z += Math.sin(ang) * 34.0 * p;
            eye = ground(gen, x, z) + lerp(36.0, 9.0, p);
            tx = x + Math.cos(ang) * 15.0; tz = z + Math.sin(ang) * 15.0;
            ty = ground(gen, tx, tz) + 2.0;
            break;
         case 5: // Static: a held wide with a slow pan
            x += Math.cos(ang) * 4.0 * p; z += Math.sin(ang) * 4.0 * p;
            eye = ground(gen, x, z) + 21.0;
            tx = x + Math.cos(ang + 0.7 * p) * 44.0;
            tz = z + Math.sin(ang + 0.7 * p) * 44.0;
            ty = ground(gen, tx, tz) + 7.0;
            break;
         case 6: // Top-down: steep look at the terrain below
            x += Math.cos(ang) * 26.0 * p; z += Math.sin(ang) * 26.0 * p;
            eye = ground(gen, x, z) + 58.0;
            tx = x + Math.cos(ang) * 7.0; tz = z + Math.sin(ang) * 7.0;
            ty = ground(gen, tx, tz);
            break;
         default: // Chase: very fast, very low
            x += Math.cos(ang) * 96.0 * p; z += Math.sin(ang) * 96.0 * p;
            eye = ground(gen, x, z) + lerp(5.0, 8.0, p);
            tx = x + Math.cos(ang) * 34.0; tz = z + Math.sin(ang) * 34.0;
            ty = ground(gen, tx, tz) + 12.0;
            break;
      }
      double dx = tx - x, dy = ty - eye, dz = tz - z;
      double horiz = Math.sqrt(dx * dx + dz * dz);
      double yaw = Math.toDegrees(Math.atan2(-dx, dz));
      double pitch = -Math.toDegrees(Math.atan2(dy, horiz));
      return new double[]{x, eye, z, yaw, Math.max(-78.0, Math.min(78.0, pitch))};
   }

   static String plan() {
      StringBuilder b = new StringBuilder();
      b.append("BLOCKCRAFT V").append(Game.VERSION_STR).append(" - CINEMATIC TRAILER MONTAGE\n");
      b.append("length=").append(LENGTH).append("s  shots=").append(SHOTS.length)
         .append("  camera moves=").append(KIND.length).append("  locations=8  seed=").append(SEED).append("\n\n");
      for (int i = 0; i < SHOTS.length; i++) {
         Shot s = SHOTS[i];
         b.append(String.format("%02d  %5.1fs - %5.1fs  %4.1fs  %-8s  loc %d  sky %5d  %s  act %d  %s%s%n",
            i + 1, s.start, s.end, s.length(), KIND[s.kind], s.anchor, s.sky,
            s.weather == 3 ? "STORM" : "clear", s.act, s.cue,
            s.title.isEmpty() ? "" : "  CARD: " + s.title));
      }
      return b.toString();
   }

   // ------------------------------------------------------------------ runtime state
   final Game g;
   double time;
   double tickAcc;
   int shot = -1;
   double baseX, baseZ;
   World ownWorld;
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

   private void findAnchor(Gen gen) {
      double[] a = anchor(gen, 0.0, 0.0, 0);
      this.baseX = a[0];
      this.baseZ = a[1];
   }

   private void installWorld() {
      WorldSettings ws = new WorldSettings();
      ws.type = WorldSettings.DEFAULT;
      ws.aquatic = true;
      ws.terrainVer = 3;
      ws.caveVer = 7;
      ws.biomeBorders = 1;
      World w = new World(SEED, null, Dim.OVERWORLD, ws);
      w.saveEnabled = false;
      w.weather.enabled = true;
      w.weather.startFair();
      w.disasters.enabled = false;
      w.game = this.g;
      w.dayTime = SHOTS[0].sky;
      if (this.g.rend != null) w.unloadHook = this.g.rend::onUnload;
      this.findAnchor(w.gen);
      Player cam = new Player();
      cam.setMode(1);
      cam.flying = true;
      this.ownWorld = w;
      this.ownCam = cam;
      this.g.world = w;
      this.g.player = cam;
      if (this.g.rend != null) this.g.rend.setLod(w);
      int cx = (int)Math.floor(this.baseX) >> 4;
      int cz = (int)Math.floor(this.baseZ) >> 4;
      w.updateChunks(cx, cz, this.g.opt.renderDistance + 1, 64, 12000000L);
   }

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
         g.opt.look = Look.CINEMATIC;
         g.opt.weather = true;
      }
      boolean inGame = g.screen == Game.S.PLAY && g.world != null && g.player != null;
      if (inGame) {
         t.savePlayer(g.player);
         t.baseX = g.player.x;
         t.baseZ = g.player.z;
         g.world.weather.enabled = true;
      } else {
         Panorama.stop(g);
         t.installWorld();
      }
      g.view = 0;
      g.hideGui = true;
      g.setScreen(Game.S.TRAILER);
   }

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
         for (Chunk c : t.ownWorld.chunks.values()) c.unloaded = true;
         t.ownWorld.chunks.clear();
         t.ownWorld.shutdown();
         if (g.world == t.ownWorld) {
            g.world = t.savedWorld;
            g.player = t.savedPlayer;
         }
         if (t.savedWorld == null) Panorama.start(g);
         else if (g.rend != null) g.rend.setLod(t.savedWorld);
      } else {
         if (t.savedPlayer != null) t.restorePlayer(t.savedPlayer);
         t.restoreWeather(t.savedWorld);
      }
      g.setScreen(t.returnScreen);
   }

   void update(double dt) {
      this.time += dt;
      if (this.time >= LENGTH) { stop(this.g); return; }
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
         if (sh.cue != null) SoundEngine.playUi(sh.cue, 0.5F, 1.0F);
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
      float in = (float)Math.min(1.0, Math.max(0.0, sec / 0.28));
      float out = (float)Math.min(1.0, Math.max(0.0, (Math.min(len, 2.0) - sec) / 0.35));
      return Math.min(in, out);
   }

   static void draw(Game g) {
      Trailer t = g.trailer;
      Gui gui = g.gui;
      float w = gui.w, h = gui.h;
      double time = t == null ? 0.0 : t.time;
      int shot = shotAt(time);
      double sec = 0.0;
      Shot sh = null;
      if (shot < SHOTS.length) {
         sh = SHOTS[shot];
         sec = time - sh.start;
      }

      float bar = Math.max(8.0F, h * 0.115F);
      gui.rect(0.0F, 0.0F, w, bar, 0xE6000000);
      gui.rect(0.0F, h - bar, w, bar, 0xE6000000);
      gui.rect(0.0F, bar - 1.0F, w, 1.0F, 0x50FFD84A);
      gui.rect(0.0F, h - bar, w, 1.0F, 0x50FFD84A);

      gui.text("CINEMATIC TRAILER", 8.0F, bar + 6.0F, 0xFFB9C4D8, true);
      if (sh != null) {
         gui.text(String.format("CUT %02d / %02d  %s", shot + 1, SHOTS.length, KIND[sh.kind]),
            8.0F, bar + 17.0F, 0xFFFFDF58, true);
      }
      int secI = (int)Math.floor(time);
      String tc = String.format("0:%02d / 1:00", Math.min(59, secI));
      gui.text(tc, w - Gui.textWidth(tc) - 8.0F, bar + 6.0F, 0xFFB9C4D8, true);
      gui.centered("ESC or click to skip", w / 2.0F, h - bar - 15.0F, 0x80FFFFFF, true, 1.0F);
      float frac = (float)Math.max(0.0, Math.min(1.0, time / LENGTH));
      gui.rect(0.0F, h - bar - 3.0F, w, 2.0F, 0x40000000);
      gui.rect(0.0F, h - bar - 3.0F, w * frac, 2.0F, 0xC0FFD84A);

      // Big word cards between bursts of quick cuts, like a real trailer.
      if (sh != null && !sh.title.isEmpty()) {
         float a = cardAlpha(sec, sh.length());
         if (a > 0.01F) {
            gui.centered(sh.title, w / 2.0F, h * 0.40F, argb(a, 255, 246, 205), true, 2.6F);
            gui.centered(sh.sub, w / 2.0F, h * 0.40F + 28.0F, argb(a * 0.92F, 205, 216, 235), true, 1.0F);
            float lw = Math.min(150.0F, w * 0.22F);
            gui.rect(w / 2.0F - lw / 2.0F, h * 0.40F + 44.0F, lw, 1.0F, argb(a * 0.6F, 255, 216, 74));
         }
      }

      // A 0.14 s black flash on every hard cut.
      if (shot > 0 && sh != null && sec < 0.14) {
         gui.rect(0.0F, 0.0F, w, h, argb((float)(1.0 - sec / 0.14), 0, 0, 0));
      }
      if (time < 0.8) {
         gui.rect(0.0F, 0.0F, w, h, argb((float)(1.0 - time / 0.8), 0, 0, 0));
      }

      // Finale: fade to black, the block-built logo, then the tagline.
      if (time > 55.6) {
         float fa = (float)Math.min(1.0, Math.max(0.0, (time - 55.6) / 1.4));
         gui.rect(0.0F, 0.0F, w, h, argb(fa * 0.95F, 0, 0, 0));
         if (fa > 0.02F) {
            float cell = Logo.fit(w - 36.0F, 6.0F);
            float top = h * 0.30F;
            Logo.draw(gui, w / 2.0F, top, cell, time);
            gui.centered("V" + Game.VERSION_STR + "  -  AVAILABLE NOW", w / 2.0F,
               top + Logo.height(cell) + 16.0F, argb(fa, 255, 223, 88), true, 1.0F);
            gui.centered("Original game. No Mojang code or assets.", w / 2.0F,
               top + Logo.height(cell) + 30.0F, argb(fa * 0.8F, 180, 190, 208), true, 1.0F);
         }
      }
      if (time > LENGTH - 0.6) {
         gui.rect(0.0F, 0.0F, w, h, argb((float)((time - (LENGTH - 0.6)) / 0.6), 0, 0, 0));
      }
   }
}
