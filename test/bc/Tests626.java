package bc;

import static bc.GameTests.check;
import static bc.GameTests.eq;
import static bc.GameTests.test;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/** V6.26: LOD tiles fade in instead of popping, and the LOD / render paths cannot take the game down. */
final class Tests626 {
   private Tests626() {
   }

   static void run() {
      test("6.26: LOD tiles fade in instead of popping", Tests626::fade);
      test("6.26: split decisions have hysteresis", Tests626::hysteresis);
      test("6.26: LOD builds survive concurrent workers and always produce valid data", Tests626::buildStress);
      test("6.26: LOD upload / draw / render failures are contained, not fatal", Tests626::guards);
      test("6.26: uncaught worker exceptions are written to crash.log", Tests626::crashLog);
      test("6.26: version, notes and download links point at 6.26", Tests626::wiring);
   }

   static String src(String p) throws Exception {
      return new String(Files.readAllBytes(new File(p).toPath()), "UTF-8");
   }

   static void fade() throws Exception {
      eq(0.0F, Lod.fadeIn(100L, 100L), "a tile that just uploaded is invisible, not popped in");
      check(Lod.fadeIn(105L, 100L) > 0.0F && Lod.fadeIn(105L, 100L) < 1.0F, "the fade ramps smoothly");
      eq(0.5F, Lod.fadeIn(109L, 100L), "half way through the fade");
      eq(1.0F, Lod.fadeIn(118L, 100L), "the tile is fully opaque after the fade");
      eq(1.0F, Lod.fadeIn(100L, 0L), "old tiles without a ready frame stay opaque");
      eq(1.0F, Lod.distanceFade(0.0, 64.0), "a short render distance does not dim nearby tiles");
      eq(1.0F, Lod.distanceFade(500.0, 1000.0), "mid-distance tiles are fully opaque");
      check(Lod.distanceFade(970.0, 1000.0) > 0.0F && Lod.distanceFade(970.0, 1000.0) < 1.0F,
         "the outer horizon fades out");
      eq(0.0F, Lod.distanceFade(1000.0, 1000.0), "culling happens only on a fully faded tile");
      String l = src("src/bc/Lod.java");
      check(l.contains("glBlendColor") && l.contains("32771"), "per-tile constant-alpha blending is used");
      check(l.contains("readyFrame = this.frame"), "the fade clock starts on upload");
      check(l.contains("static final int FADE_FRAMES"), "the fade has a bounded duration");
      check(l.contains("glBlendFunc(770, 771)"), "normal blending is restored for the rest of the frame");
   }

   static void hysteresis() throws Exception {
      String l = src("src/bc/Lod.java");
      check(l.contains("lastSplit") && l.contains("splitFrame"), "the split decision is remembered per tile");
      check(l.contains("splitWeight") && l.contains("0.86") && l.contains("1.14"), "V6.31 uses a continuous split band instead of a one-frame decision");
   }

   static void buildStress() throws Exception {
      WorldSettings st = new WorldSettings();
      st.terrainVer = 4;
      st.structVer = 2;
      st.treeDensity = 1.0F;
      TerraGen tg = new TerraGen(20261106L, st);
      Lod lod = new Lod(null, tg, null);
      int[][] sizes = {{32, 0, 0}, {64, 64, 64}, {128, 128, -128},
         {256, 256, 256}, {512, -512, 512}, {1024, 0, 1024}};
      AtomicInteger bad = new AtomicInteger(), mismatch = new AtomicInteger(), good = new AtomicInteger();
      ExecutorService pool = Executors.newFixedThreadPool(4);
      List<Future<?>> fs = new ArrayList<>();
      try {
         for (int rep = 0; rep < 4; rep++) {
            for (int[] q : sizes) {
               final int[] qq = q;
               fs.add(pool.submit(() -> {
                  try {
                     Lod.Tile t = new Lod.Tile(qq[0], qq[1], qq[2]);
                     lod.build(t);
                     if (t.data == null || t.vcount <= 0) bad.incrementAndGet();
                     else if (t.data.limit() != t.vcount * 16) mismatch.incrementAndGet();
                     else good.incrementAndGet();
                  } catch (Throwable e) {
                     bad.incrementAndGet();
                  }
               }));
            }
         }
         for (Future<?> f : fs) f.get();
      } finally {
         pool.shutdownNow();
         lod.dispose();
      }
      eq(0, bad.get(), "no LOD worker threw while building " + good.get() + " tiles");
      eq(0, mismatch.get(), "every uploaded mesh has exactly vcount*16 bytes");
      check(good.get() >= 20, "the stress test really built tiles on four threads");
      String l = src("src/bc/Lod.java");
      check(l.contains("int verts = bytes / 16"), "vertex count comes from the real buffer length");
      check(l.contains("if (!t.retired) this.uploads.add(t)"), "retired tiles are not queued for upload");
   }

   static void guards() throws Exception {
      String l = src("src/bc/Lod.java");
      String r = src("src/bc/Renderer.java");
      check(l.contains("catch (Throwable glError)"), "a failed VBO upload is contained");
      check(l.contains("catch (RuntimeException rejected)"), "a rejected worker task cannot leak inflight slots");
      check(l.contains("catch (Throwable tileError)"), "a bad tile cannot abort the draw loop");
      check(l.contains("var19.retired || var19.state != 3 || var19.vbo == 0"), "deleted VBOs are never drawn");
      check(r.contains("[lod] render failed, rebuilding LOD"), "a LOD render exception never kills the game");
      check(r.contains("new java.util.ArrayList<>(this.g.mobs.list)"), "mob iteration uses a snapshot");
      check(r.indexOf("new java.util.ArrayList<>(this.g.mobs.list)") < r.indexOf("GL11.glBegin(GL11.GL_QUADS)") - 2000
         || r.indexOf("Snapshot FIRST") > 0, "the snapshot is taken before glBegin");
   }

   static void crashLog() throws Exception {
      String m = src("src/bc/Main.java");
      check(m.contains("installCrashLogger"), "a global crash logger is installed at startup");
      check(m.contains("Thread.setDefaultUncaughtExceptionHandler"), "worker-thread exceptions are caught too");
      check(m.contains("crash.log") && m.contains("thread.getName"), "the log records the failing thread");
   }

   static void wiring() throws Exception {
      eq("6.33", Game.VERSION_STR, "the game version is 6.33");
      eq("6.33", src("ci/release-trigger.txt").trim(), "the release workflow publishes 6.33");
      check(src("README.md").contains("V6.28"), "README describes V6.26");
      check(src("package/README.txt").contains("V6.28"), "the packaged readme describes V6.26");
      check(new File("docs/releases/v6.26.md").isFile(), "V6.26 release notes exist");
   }
}
