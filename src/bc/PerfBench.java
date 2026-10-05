package bc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Update 3.18: the scripted benchmark ({@code -Dbc.bench=frames}, see {@code tools/shots.py}'s scene "bench" and the Visual workflow).
 *
 * It waits until the terrain around the camera is built, then turns the camera once around its own axis for every setting of a list - the
 * settings as they are, no shadows, fast water, no clouds, no distant terrain, no shaders, a shorter view - and prints how long a frame took
 * with each. The differences are what a feature costs. One more run switches on the exact mode (every phase waits for the chip) and prints
 * where the time goes. It is a measurement of this machine and its OpenGL; the Visual workflow runs it with the software renderer (Mesa
 * llvmpipe), where the pixel and vertex work is done by the processor: the numbers are no frame rates of a graphics card but they show which
 * parts of a frame are expensive, and they are the same for every change that is compared.
 */
final class PerfBench {
   private PerfBench() {
   }

   private static final int SETTLE = 24;
   private static boolean active;
   private static int stage; // 0 waiting for the world, 1.. = the presets
   private static int frame;
   private static int settle;
   private static int waited;
   private static int measure;
   private static double baseX;
   private static double baseY;
   private static double baseZ;
   private static float basePitch;
   private static final List<Double> times = new ArrayList<>();
   private static long last;
   private static final List<String> report = new ArrayList<>();
   private static int rd0;
   private static int lod0;
   private static int wq0;
   private static boolean clouds0;
   private static boolean shaders0;
   private static int scale0;
   private static long time0;

   static final String[] PRESETS = {
      "as configured", "exact phases", "no shadows", "shadow every frame", "shadow map uncut", "no section culling", "fast water", "no clouds", "no distant terrain",
      "no shaders", "render scale 50%", "view distance 4"
   };

   static boolean running() {
      return active;
   }

   static boolean enabled() {
      return System.getProperty("bc.bench") != null;
   }

   private static boolean loaded(Game g) {
      World w = g.world;
      int rd = g.opt.renderDistance;
      int cx = (int)Math.floor(g.player.x) >> 4;
      int cz = (int)Math.floor(g.player.z) >> 4;
      for (int dz = -rd; dz <= rd; dz++) {
         for (int dx = -rd; dx <= rd; dx++) {
            if (dx * dx + dz * dz > rd * rd) {
               continue;
            }

            Chunk c = w.chunk(cx + dx, cz + dz);
            if (c == null || !c.lit) {
               return false;
            }

            for (Chunk.Section s : c.sections) {
               if (s.dirty || s.inFlight) {
                  return false;
               }
            }
         }
      }

      return g.rend.inflight.get() == 0;
   }

   private static void apply(Game g, int preset) {
      Options o = g.opt;
      o.renderDistance = rd0;
      o.lodDistance = lod0;
      o.waterQ = wq0;
      o.clouds = clouds0;
      o.shaders = shaders0;
      o.renderScale = scale0;
      Perf.noShadow = false;
      Perf.noShadowCull = false;
      Perf.noCull = false;
      Perf.shadowEvery = false;
      Perf.exact = false;
      switch (preset) {
         case 1:
            Perf.exact = true;
            break;
         case 2:
            Perf.noShadow = true;
            break;
         case 3:
            Perf.shadowEvery = true;
            break;
         case 4:
            Perf.shadowEvery = true;
            Perf.noShadowCull = true;
            break;
         case 5:
            Perf.noCull = true;
            break;
         case 6:
            o.waterQ = 1;
            break;
         case 7:
            o.clouds = false;
            break;
         case 8:
            o.lodDistance = o.renderDistance;
            break;
         case 9:
            o.shaders = false;
            break;
         case 10:
            o.renderScale = 50;
            break;
         case 11:
            o.renderDistance = Math.min(4, rd0);
            break;
         default:
      }
   }

   /** One call per frame while the game is in play mode; true when the benchmark is over (the game then takes its last picture and ends). */
   static boolean step(Game g) {
      if (!active) {
         active = true;
         Perf.on = true;
         rd0 = g.opt.renderDistance;
         lod0 = g.opt.lodDistance;
         wq0 = g.opt.waterQ;
         clouds0 = g.opt.clouds;
         shaders0 = g.opt.shaders;
         scale0 = g.opt.renderScale;
         measure = Math.max(20, Integer.getInteger("bc.bench", 90));
         stage = 0;
         System.out.println("BENCH start: window " + g.fbW + "x" + g.fbH + ", view distance " + rd0 + ", distant terrain " + lod0 + ", " + (g.rend.shadersOn() ? "shaders" : "no shaders")
            + ", water " + wq0 + ", " + measure + " frames per setting, GPU timers " + (Perf.gpuTimers() ? "yes" : "no") + ", renderer " + g.gpuName);
      }

      long now = System.nanoTime();
      if (stage == 0) {
         waited++;
         if (waited < 40 || !loaded(g) && waited < 4000) {
            if (waited % 200 == 0) {
               System.out.println("BENCH waiting for the world: frame " + waited + ", chunks " + g.world.chunks.size() + ", mesh queue " + g.rend.inflight.get());
            }

            return false;
         }

         // the camera stands still above the ground and turns once round: every direction of the view is in the measurement
         baseX = g.player.x;
         baseZ = g.player.z;
         Chunk under = g.world.chunkAt((int)Math.floor(baseX), (int)Math.floor(baseZ));
         int surf = under == null ? 80 : under.hm[((int)Math.floor(baseZ) & 15) * 16 + ((int)Math.floor(baseX) & 15)];
         baseY = surf + 3;
         basePitch = Float.parseFloat(System.getProperty("bc.benchpitch", "8"));
         System.out.println("BENCH world ready after " + waited + " frames: " + g.world.chunks.size() + " chunks, camera at " + (int)baseX + "," + (int)baseY + "," + (int)baseZ + " (ground " + surf + ")");
         stage = 1;
         settle = 0;
         frame = 0;
         time0 = g.world.dayTime;
         apply(g, 0);
         last = now;
         return false;
      }

      g.world.dayTime = time0; // the sun stands still: the shadow map is only drawn again when the benchmark asks for it

      g.player.flying = true;
      g.player.setPos(baseX, baseY, baseZ);
      if (settle < SETTLE) {
         settle++;
         g.player.yaw = 0.0F;
         g.player.pitch = basePitch;
         if (settle == SETTLE) {
            times.clear();
            Perf.benchReset();
            last = now;
            frame = 0;
         }

         return false;
      }

      g.player.yaw = 360.0F * frame / measure;
      g.player.pitch = basePitch;
      times.add((now - last) / 1e6);
      last = now;
      frame++;
      if (frame < measure) {
         return false;
      }

      finish(g, stage - 1);
      stage++;
      if (stage > PRESETS.length) {
         apply(g, 0);
         Perf.on = false;
         active = false;
         System.out.println("BENCH done");
         for (String s : report) {
            System.out.println(s);
         }

         return true;
      }

      apply(g, stage - 1);
      // a changed view distance / shader setting needs a moment until the world has been rebuilt for it
      settle = 0;
      frame = 0;
      return false;
   }

   private static void finish(Game g, int preset) {
      Perf.bench = false;
      double[] t = new double[times.size()];
      double sum = 0.0;
      for (int i = 0; i < t.length; i++) {
         t[i] = times.get(i);
         sum += t[i];
      }

      Arrays.sort(t);
      double avg = sum / Math.max(1, t.length);
      double p95 = t[Math.min(t.length - 1, (int)(t.length * 0.95))];
      double worst = t[t.length - 1];
      double n = Math.max(1, Perf.bFrames);
      String line = String.format("BENCH %-20s avg %7.1f ms (%5.1f fps)  95%% %7.1f ms  worst %7.1f ms  draws %d  vertices %dk  sections %d  shadow %d draws %dk",
         PRESETS[preset], avg, 1000.0 / avg, p95, worst, Math.round(Perf.bDraws / n), Math.round(Perf.bVerts / n / 1000), Math.round(Perf.bSections / n),
         Math.round(Perf.bShadowDraws / n), Math.round(Perf.bShadowVerts / n / 1000));
      System.out.println(line);
      report.add(line);
      if (preset <= 1) {
         StringBuilder c = new StringBuilder("BENCH   cpu ms ");
         StringBuilder gp = new StringBuilder("BENCH   gpu ms ");
         double ng = Math.max(1, Perf.bGpuFrames);
         for (int p = 0; p < Perf.N; p++) {
            c.append(' ').append(Perf.NAME[p]).append(' ').append(Perf.f1(Perf.bCpu[p] / n / 1e6));
            gp.append(' ').append(Perf.NAME[p]).append(' ').append(Perf.f1(Perf.bGpu[p] / ng / 1e6));
         }

         c.append("   (the frames of the Perf timer: avg " + Perf.f1(Perf.bFrameSum / n / 1e6) + " ms, worst " + Perf.f1(Perf.bFrameMax / 1e6) + " ms; garbage " + Math.round(Perf.bAlloc / n / 1024.0) + " KB/frame)");
         System.out.println(c);
         report.add(c.toString());
         if (Perf.bGpuFrames > 0) {
            gp.append("   (" + Perf.bGpuFrames + " frames)");
            System.out.println(gp);
            report.add(gp.toString());
         }

         StringBuilder pk = new StringBuilder("BENCH   worst ms ");
         for (int p = 0; p < Perf.N; p++) {
            pk.append(' ').append(Perf.NAME[p]).append(' ').append(Perf.f1(Perf.bPeakCpu[p] / 1e6));
         }

         System.out.println(pk);
         report.add(pk.toString());
      }
   }
}
