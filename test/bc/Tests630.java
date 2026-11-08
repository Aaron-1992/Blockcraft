package bc;

import static bc.GameTests.check;
import static bc.GameTests.eq;
import static bc.GameTests.test;

import java.io.File;
import java.nio.file.Files;

/** V6.30: the one-minute in-game cinematic trailer - timeline, camera, runtime restore and wiring. */
final class Tests630 {
   private Tests630() {
   }

   static void run() {
      test("6.30: eight shots add up to exactly one minute", Tests630::timeline);
      test("6.30: every camera flies over real terrain and looks at it", Tests630::camera);
      test("6.30: the trailer starts, moves, ends itself and restores the game", Tests630::runtime);
      test("6.30: real recorded foley and the four-act soundtrack are wired in", Tests630::audio);
      test("6.30: version, title button, commands and release files point at 6.30", Tests630::wiring);
   }

   static String src(String p) throws Exception {
      return new String(Files.readAllBytes(new File(p).toPath()), "UTF-8");
   }

   static WorldSettings settings() {
      WorldSettings st = new WorldSettings();
      st.terrainVer = 3;
      st.caveVer = 4;
      st.biomeBorders = 1;
      st.aquatic = true;
      return st;
   }

   static void timeline() throws Exception {
      eq(60.0, Trailer.LENGTH, "the trailer is exactly 60 seconds");
      eq(24, Trailer.SHOTS.length, "a fast-cut montage of 24 shots");
      double t = 0.0;
      for (int i = 0; i < Trailer.SHOTS.length; i++) {
         Trailer.Shot s = Trailer.SHOTS[i];
         eq(t, s.start, "shot " + (i + 1) + " starts where the last one ended");
         check(s.end > s.start, "shot " + (i + 1) + " has a positive length");
         check(s.length() > 0.0 && s.length() <= 5.5, "shot " + (i + 1) + " is a short montage cut");
         eq(i, Trailer.shotAt(s.start + 0.001), "shotAt finds shot " + (i + 1));
         t = s.end;
      }
      eq(60.0, t, "the last cut lands exactly on 1:00");
      eq(Trailer.SHOTS.length, Trailer.shotAt(60.0), "the trailer is over at 60 seconds");
      eq(0, Trailer.shotAt(0.0), "shotAt starts at zero");
      boolean varied = false;
      for (int i = 1; i < Trailer.SHOTS.length; i++) {
         if (Trailer.SHOTS[i].sky != Trailer.SHOTS[i - 1].sky) varied = true;
         check(Trailer.SHOTS[i].sky >= 0L && Trailer.SHOTS[i].sky < 24000L,
            "shot " + (i + 1) + " has a valid time of day");
      }
      check(varied, "the montage cuts between dawn, day, storm, dusk and night");
      boolean kinds = false;
      for (int i = 1; i < Trailer.SHOTS.length; i++) {
         if (Trailer.SHOTS[i].kind != Trailer.SHOTS[0].kind) kinds = true;
      }
      check(kinds && Trailer.KIND.length == 8, "eight different camera moves are used");
      check(Trailer.skyAt(2, 0.5) > Trailer.skyAt(2, 0.0), "each shot drifts smoothly");
      for (int i = 0; i < Trailer.SHOTS.length; i++) {
         check(Trailer.actAt(i) >= 0 && Trailer.actAt(i) < Trailer.ACT_TRACKS.length,
            "shot " + (i + 1) + " has a soundtrack act");
      }
      String plan = Trailer.plan();
      check(plan.contains("length=60.0s") && plan.contains("shots=24") && plan.contains("MONTAGE"),
         "the headless plan prints the whole montage");
      check(src("docs/releases/v6.30-trailer-plan.txt").contains("length=60.0s"),
         "the plan is shipped as a document");
   }

   static void camera() throws Exception {
      TerraGen gen = new TerraGen(Trailer.SEED, settings());
      double[] base = Trailer.anchor(gen);
      check(Double.isFinite(base[0]) && Double.isFinite(base[1])
         && Math.abs(base[0]) < 1000.0 && Math.abs(base[1]) < 1000.0,
         "the fixed seed finds a deterministic anchor");
      boolean allAbove = true, allFinite = true, allMove = true;
      for (int i = 0; i < Trailer.SHOTS.length; i++) {
         double[] a = Trailer.cameraAt(gen, base[0], base[1], i, 0.0);
         double[] b = Trailer.cameraAt(gen, base[0], base[1], i, 1.0);
         for (double[] c : new double[][]{a, b, Trailer.cameraAt(gen, base[0], base[1], i, 0.5)}) {
            for (double v : c) if (!Double.isFinite(v)) allFinite = false;
            double ground = Trailer.ground(gen, c[0], c[2]);
            if (c[1] < ground + 4.0) allAbove = false;
            if (Math.abs(c[4]) > 78.0) allFinite = false;
         }
         double dist = Math.hypot(b[0] - a[0], b[2] - a[2]);
         if (dist < 0.5) allMove = false;
      }
      check(allFinite, "all camera positions, headings and pitches are finite");
      check(allAbove, "the camera stays above the real terrain in every shot");
      check(allMove, "every shot really moves its camera (or orbits)");
      check(Trailer.smooth(0.0) == 0.0 && Trailer.smooth(1.0) == 1.0
         && Trailer.smooth(0.5) == 0.5, "camera easing is smooth and bounded");
   }

   static void runtime() throws Exception {
      Game g = GameTests.newGame(0);
      g.opt = new Options(g.root);   // GameTests.newGame() has no window, so run() never made the options
      double x0 = g.player.x, y0 = g.player.y, z0 = g.player.z;
      float yaw0 = g.player.yaw, pitch0 = g.player.pitch;
      int look0 = g.opt.look;
      boolean weather0 = g.opt.weather;
      long day0 = g.world.dayTime;
      boolean weatherEnabled0 = g.world.weather.enabled;

      Trailer.start(g);
      check(g.trailer != null && g.screen == Game.S.TRAILER, "the trailer takes over the screen");
      check(g.opt.look == Look.CINEMATIC, "cinematic colour grading is applied");
      check(Trailer.active(g), "the music selector sees an active trailer");
      for (int i = 0; i < 200; i++) {
         g.trailer.update(0.05);   // 10 seconds
      }
      check(g.trailer.time >= 9.9 && g.trailer.time < 10.2, "the clock advances in real seconds");
      check(g.player.x != x0 || g.player.z != z0, "the camera actually flies");
      check(g.world.dayTime != day0, "the sky time-lapses");
      check(g.trailer.shot >= 3, "several different shots have already been cut to");
      g.trailer.shot = 2;
      eq(Trailer.ACT_TRACKS[Trailer.actAt(2)], SoundEngine.selectConsoleMusic(g),
         "the shot's soundtrack act is selected");

      Trailer.stop(g);             // the skip path (any key / click)
      check(g.trailer == null && g.screen == Game.S.PLAY, "skipping returns to the game");
      eq(x0, g.player.x, "the camera x is restored");
      eq(y0, g.player.y, "the camera y is restored");
      eq(z0, g.player.z, "the camera z is restored");
      eq(yaw0, g.player.yaw, "the yaw is restored");
      eq(pitch0, g.player.pitch, "the pitch is restored");
      eq(look0, g.opt.look, "the colour grading is restored");
      eq(weather0, g.opt.weather, "the weather option is restored");
      eq(day0, g.world.dayTime, "the daylight is restored");
      eq(weatherEnabled0, g.world.weather.enabled, "the weather state is restored");

      Trailer.start(g);            // the automatic 60-second ending
      int frames = 0;
      while (g.trailer != null && frames < 1300) {
         g.trailer.update(0.05);
         frames++;
      }
      check(g.trailer == null, "the trailer ends itself");
      check(frames >= 1199 && frames <= 1201, "it lasted exactly 60 seconds (" + frames + " frames)");
      check(g.screen == Game.S.PLAY, "the world is playable again");
   }

   static void audio() throws Exception {
      for (String track : Trailer.ACT_TRACKS) {
         check(SoundEngine.hasSample(track), "soundtrack track " + track + " exists");
      }
      for (Trailer.Shot s : Trailer.SHOTS) {
         check(SoundEngine.isRecordedFoley(s.cue),
            "shot cue " + s.cue + " is a real CC0 WAV recording, not synthesized");
      }
      String se = src("src/bc/SoundEngine.java");
      check(se.contains("g.screen == Game.S.TRAILER") && se.contains("Trailer.music(g)"),
         "the music selector switches to the trailer soundtrack");
      String t = src("src/bc/Trailer.java");
      check(t.contains("SoundEngine.playUi(sh.cue"), "the cuts play foley stings");
      check(t.contains("Panorama.stop(g)") && t.contains("Panorama.start(g)"),
         "the title panorama is suspended and restored");
      check(t.contains("Look.CINEMATIC"), "the cinematic grade is applied");
   }

   static void wiring() throws Exception {
      eq("6.34", Game.VERSION_STR, "the game version is 6.34");
      eq("6.34", src("ci/release-trigger.txt").trim(), "the release workflow publishes 6.34");
      String g = src("src/bc/Game.java");
      check(g.contains("Trailer trailer;") && g.contains("case TRAILER:")
         && g.contains("Trailer.draw(this)") && g.contains("this.trailer.update(var1)"),
         "the game has a trailer screen, overlay and update hook");
      check(g.contains("case \"trailer\":") && g.contains("/trailer"),
         "the /trailer command is available in game");
      check(g.contains("this.screen == Game.S.TRAILER") && g.contains("Trailer.stop(this)"),
         "any key or click skips it");
      String s = src("src/bc/Screens.java");
      check(s.contains("Watch Trailer (1:00)") && s.contains("Trailer.start(g)"),
         "the title screen has a Watch Trailer button");
      check(src("src/bc/Main.java").contains("--trailer-plan"), "the headless shot plan is exposed");
      check(src("src/bc/Trailer.java").contains("static final String[] KIND"), "the montage has distinct camera moves");
      check(new File("package/Trailer-guide.txt").isFile(), "the packaged trailer guide exists");
      check(new File("docs/releases/v6.30.md").isFile(), "V6.30 release notes exist");
      check(new File("docs/screenshots/v6.30-trailer.png").isFile(), "the storyboard preview exists");
      check(src("README.md").contains("V6.30"), "README describes V6.30");
      check(src("package/README.txt").contains("WHAT'S NEW IN V6.30"), "the packaged readme describes V6.30");
      check(src("tools/build.py").contains("\"Trailer\""), "the build receipt hashes the trailer class");
   }
}
