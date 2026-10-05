package bc;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.function.LongSupplier;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL33;
import org.lwjgl.opengl.GLCapabilities;

/**
 * Update 3.18: where does a frame go?
 *
 * The game is cut into phases (update, sky, distant terrain, culling, shadow map, terrain, mobs, water, clouds, effects, hand, post processing,
 * the interface, swap) and every frame says where one starts ({@link #mark}). Per phase two things are measured:
 * <ul>
 * <li>CPU time: how long the game thread needed to give the phase's commands to the driver (plus, in the "swap" phase, the time it had to wait
 * for the graphics chip or for the screen).</li>
 * <li>GPU time: how long the graphics chip needed, read with timestamp queries ({@code GL_ARB_timer_query}), without stopping anything: the results
 * of a frame are collected three frames later.</li>
 * </ul>
 * Shift+F3 shows the numbers (average and worst frame of the last second), Ctrl+F3 additionally makes every phase wait for the chip so that
 * the CPU numbers contain the GPU work as well (exact, but slower). While the panel is open one line per second goes to {@code perf.log}.
 * {@code -Dbc.bench=...} runs a scripted benchmark ({@link PerfBench}).
 */
final class Perf {
   static final int UPDATE = 0, SKY = 1, LOD = 2, CULL = 3, SHADOW = 4, TERRAIN = 5, ENTITIES = 6, WATER = 7, CLOUDS = 8, FX = 9, HAND = 10, POST = 11, GUI = 12, SWAP = 13;
   static final int N = 14;
   static final String[] NAME = {"update", "sky", "lod", "cull", "shadow", "terrain", "mobs", "water", "clouds", "fx", "hand", "post", "gui", "swap"};

   /** Measure at all (the panel is open or a benchmark runs); off, {@link #mark} returns at once. */
   static boolean on;
   /** Every mark waits for the graphics chip first (glFinish): the CPU time of a phase then contains the chip's work. Exact on every GL, but slower. */
   static boolean exact;
   /** The panel is drawn. */
   static boolean show;
   /** One line per second into perf.log. */
   static File log;

   /** The clock (tests set their own). */
   static LongSupplier clock = System::nanoTime;

   private static boolean gl;
   private static boolean queries;
   private static int phase = UPDATE;
   private static long tMark;
   private static long tFrame;
   private static final long[] curCpu = new long[N];

   // the one-second window
   private static final long[] sumCpu = new long[N];
   private static final long[] maxCpu = new long[N];
   private static final long[] sumGpu = new long[N];
   private static final long[] maxGpu = new long[N];
   private static final long[] curGpu = new long[N];
   private static long sumFrame;
   private static long maxFrame;
   private static int winFrames;
   private static int winGpuFrames;
   private static long winStart;
   private static long winAlloc;
   private static long lastAlloc;

   // what the last second said (milliseconds)
   static final double[] avgCpu = new double[N];
   static final double[] peakCpu = new double[N];
   static final double[] avgGpu = new double[N];
   static final double[] peakGpu = new double[N];
   static double avgFrame;
   static double peakFrame;
   static double avgGpuTotal;
   static double allocKbPerFrame;
   static int fps;
   static boolean haveGpu;

   // the benchmark's own totals (the one-second window is too short for a setting that gives one frame a second)
   static boolean bench;
   static final long[] bCpu = new long[N];
   static final long[] bPeakCpu = new long[N];
   static final long[] bGpu = new long[N];
   static final long[] bPeakGpu = new long[N];
   static int bFrames;
   static int bGpuFrames;
   static long bFrameSum;
   static long bFrameMax;
   static double bDraws;
   static double bVerts;
   static double bSections;
   static double bShadowDraws;
   static double bShadowVerts;
   static long bAlloc;

   /** The benchmark starts a new setting: forget its totals. */
   static void benchReset() {
      Arrays.fill(bCpu, 0L);
      Arrays.fill(bPeakCpu, 0L);
      Arrays.fill(bGpu, 0L);
      Arrays.fill(bPeakGpu, 0L);
      bFrames = 0;
      bGpuFrames = 0;
      bFrameSum = 0L;
      bFrameMax = 0L;
      bDraws = bVerts = bSections = bShadowDraws = bShadowVerts = 0.0;
      bAlloc = 0L;
      bench = true;
   }

   // what the renderer drew in the last frame
   static int draws;
   static int verts;
   static int sections;
   static int shadowDraws;
   static int shadowVerts;
   /** Test / benchmark switches: leave the shadow map out, draw everything into it again (no window test), leave the culling of the main pass out. */
   static boolean noShadow;
   static boolean noShadowCull;
   static boolean noCull;
   /** Benchmark: draw the shadow map in every frame (what a moving sun or a moving player makes happen now and then). */
   static boolean shadowEvery;

   // GPU timestamps: RING frames in flight, MAXM marks per frame
   private static final int RING = 4;
   private static final int MAXM = 24;
   private static int[] qid;
   private static final int[] qph = new int[RING * MAXM];
   private static final int[] qn = new int[RING];
   private static int ring;
   private static boolean arb;

   private Perf() {
   }

   /** Called once the GL context exists: finds out whether the chip's own clock can be read. */
   static void init() {
      gl = true;
      try {
         GLCapabilities caps = GL.getCapabilities();
         arb = !caps.OpenGL33 && caps.GL_ARB_timer_query;
         if (caps.OpenGL33 || arb) {
            qid = new int[RING * MAXM];
            for (int i = 0; i < qid.length; i++) {
               qid[i] = GL15.glGenQueries();
            }

            queries = true;
         }
      } catch (Throwable t) {
         queries = false;
      }
   }

   static boolean gpuTimers() {
      return queries;
   }

   /** Start of a frame (called right after the buffers were swapped): closes the last frame and opens the next one with the update phase. */
   static void frame() {
      long now = clock.getAsLong();
      if (!on) {
         tMark = now;
         tFrame = 0L;
         return;
      }

      if (tFrame != 0L) {
         curCpu[phase] += now - tMark;
         collect(now);
      }

      tFrame = now;
      tMark = now;
      phase = UPDATE;
      Arrays.fill(curCpu, 0L);
      draws = 0;
      verts = 0;
      sections = 0;
      shadowDraws = 0;
      shadowVerts = 0;
      if (queries) {
         ring = (ring + 1) % RING;
         qn[ring] = 0;
         stamp(UPDATE);
      }
   }

   /** The phase {@code p} starts here (the one before ends). */
   static void mark(int p) {
      if (!on) {
         return;
      }

      if (exact && gl) {
         GL11.glFinish();
      }

      long now = clock.getAsLong();
      curCpu[phase] += now - tMark;
      tMark = now;
      phase = p;
      if (queries) {
         stamp(p);
      }
   }

   private static void stamp(int p) {
      int n = qn[ring];
      if (n >= MAXM) {
         return;
      }

      try {
         if (arb) {
            org.lwjgl.opengl.ARBTimerQuery.glQueryCounter(qid[ring * MAXM + n], org.lwjgl.opengl.ARBTimerQuery.GL_TIMESTAMP);
         } else {
            GL33.glQueryCounter(qid[ring * MAXM + n], GL33.GL_TIMESTAMP);
         }

         qph[ring * MAXM + n] = p;
         qn[ring] = n + 1;
      } catch (Throwable t) {
         queries = false;
      }
   }

   private static void readGpu() {
      if (!queries) {
         return;
      }

      int slot = (ring + 1) % RING; // issued three frames ago: the chip is long done with it
      int n = qn[slot];
      qn[slot] = 0;
      if (n < 2) {
         return;
      }

      try {
         int last = qid[slot * MAXM + n - 1];
         if (GL15.glGetQueryObjecti(last, GL15.GL_QUERY_RESULT_AVAILABLE) == 0) {
            return;
         }

         Arrays.fill(curGpu, 0L);
         long prev = 0L;
         for (int i = 0; i < n; i++) {
            long ts = arb ? org.lwjgl.opengl.ARBTimerQuery.glGetQueryObjecti64(qid[slot * MAXM + i], GL15.GL_QUERY_RESULT)
               : GL33.glGetQueryObjecti64(qid[slot * MAXM + i], GL15.GL_QUERY_RESULT);
            if (i > 0 && ts >= prev) {
               curGpu[qph[slot * MAXM + i - 1]] += ts - prev;
            }

            prev = ts;
         }

         winGpuFrames++;
         if (bench) {
            bGpuFrames++;
         }

         for (int p = 0; p < N; p++) {
            sumGpu[p] += curGpu[p];
            maxGpu[p] = Math.max(maxGpu[p], curGpu[p]);
            if (bench) {
               bGpu[p] += curGpu[p];
               bPeakGpu[p] = Math.max(bPeakGpu[p], curGpu[p]);
            }
         }
      } catch (Throwable t) {
         queries = false;
      }
   }

   private static void collect(long now) {
      readGpu();
      long frame = now - tFrame;
      sumFrame += frame;
      maxFrame = Math.max(maxFrame, frame);
      winFrames++;
      if (bench) {
         bFrames++;
         bFrameSum += frame;
         bFrameMax = Math.max(bFrameMax, frame);
         bDraws += draws;
         bVerts += verts;
         bSections += sections;
         bShadowDraws += shadowDraws;
         bShadowVerts += shadowVerts;
      }

      for (int p = 0; p < N; p++) {
         sumCpu[p] += curCpu[p];
         maxCpu[p] = Math.max(maxCpu[p], curCpu[p]);
         if (bench) {
            bCpu[p] += curCpu[p];
            bPeakCpu[p] = Math.max(bPeakCpu[p], curCpu[p]);
         }
      }

      long a = Alloc.bytes();
      if (a > 0L) {
         if (lastAlloc > 0L) {
            winAlloc += a - lastAlloc;
            if (bench) {
               bAlloc += a - lastAlloc;
            }
         }

         lastAlloc = a;
      }

      if (winStart == 0L) {
         winStart = now;
      }

      if (now - winStart >= 1_000_000_000L && winFrames > 0) {
         publish(now);
      }
   }

   /** Publishes what the current window has seen right now (the benchmark reads its numbers at the end of every setting). */
   static void flush() {
      if (winFrames > 0) {
         publish(clock.getAsLong());
      }
   }

   /** Forgets what the current window has seen (the benchmark starts a setting). */
   static void resetWindow() {
      Arrays.fill(sumCpu, 0L);
      Arrays.fill(maxCpu, 0L);
      Arrays.fill(sumGpu, 0L);
      Arrays.fill(maxGpu, 0L);
      sumFrame = 0L;
      maxFrame = 0L;
      winFrames = 0;
      winGpuFrames = 0;
      winAlloc = 0L;
      winStart = clock.getAsLong();
   }

   private static void publish(long now) {
      double f = winFrames;
      avgFrame = sumFrame / f / 1e6;
      peakFrame = maxFrame / 1e6;
      fps = (int)Math.round(winFrames * 1e9 / (now - winStart));
      allocKbPerFrame = winAlloc / f / 1024.0;
      haveGpu = winGpuFrames > 0;
      avgGpuTotal = 0.0;
      for (int p = 0; p < N; p++) {
         avgCpu[p] = sumCpu[p] / f / 1e6;
         peakCpu[p] = maxCpu[p] / 1e6;
         avgGpu[p] = winGpuFrames > 0 ? sumGpu[p] / (double)winGpuFrames / 1e6 : 0.0;
         peakGpu[p] = maxGpu[p] / 1e6;
         avgGpuTotal += avgGpu[p];
      }

      if (log != null) {
         writeLog();
      }

      Arrays.fill(sumCpu, 0L);
      Arrays.fill(maxCpu, 0L);
      Arrays.fill(sumGpu, 0L);
      Arrays.fill(maxGpu, 0L);
      sumFrame = 0L;
      maxFrame = 0L;
      winFrames = 0;
      winGpuFrames = 0;
      winAlloc = 0L;
      winStart = now;
   }

   private static int logLines;

   private static void writeLog() {
      if (logLines >= 600) {
         return; // ten minutes are enough
      }

      try (FileWriter w = new FileWriter(log, true)) {
         if (logLines++ == 0) {
            StringBuilder h = new StringBuilder("fps;frame_avg_ms;frame_worst_ms;draws;vertices;sections;shadow_draws;shadow_vertices;alloc_kb_per_frame");
            for (String n : NAME) {
               h.append(";cpu_").append(n);
            }

            for (String n : NAME) {
               h.append(";gpu_").append(n);
            }

            w.write(h.append('\n').toString());
         }

         StringBuilder s = new StringBuilder();
         s.append(fps).append(';').append(f2(avgFrame)).append(';').append(f2(peakFrame)).append(';').append(draws).append(';').append(verts).append(';')
            .append(sections).append(';').append(shadowDraws).append(';').append(shadowVerts).append(';').append(f2(allocKbPerFrame));
         for (int p = 0; p < N; p++) {
            s.append(';').append(f2(avgCpu[p]));
         }

         for (int p = 0; p < N; p++) {
            s.append(';').append(haveGpu ? f2(avgGpu[p]) : "");
         }

         w.write(s.append('\n').toString());
      } catch (IOException e) {
         log = null;
      }
   }

   static String f1(double v) {
      return String.format(Locale.ROOT, "%.1f", v);
   }

   static String f2(double v) {
      return String.format(Locale.ROOT, "%.2f", v);
   }

   /** The lines of the panel (Shift+F3). */
   static String[] lines(String gpu) {
      StringBuilder c1 = new StringBuilder("CPU ms avg/worst:");
      StringBuilder c2 = new StringBuilder("               ");
      StringBuilder g1 = new StringBuilder("GPU ms avg/worst:");
      StringBuilder g2 = new StringBuilder("               ");
      for (int p = 0; p < N; p++) {
         StringBuilder c = p < 7 ? c1 : c2;
         c.append("  ").append(NAME[p]).append(' ').append(f1(avgCpu[p])).append('/').append(f1(peakCpu[p]));
         if (p != SWAP) {
            StringBuilder g = p < 7 ? g1 : g2;
            g.append("  ").append(NAME[p]).append(' ').append(f1(avgGpu[p])).append('/').append(f1(peakGpu[p]));
         }
      }

      String head = "Frame " + f1(avgFrame) + " ms, worst " + f1(peakFrame) + " ms (" + fps + " fps)   draws " + draws + "   vertices " + verts / 1000 + "k   sections "
         + sections + "   shadow " + shadowDraws + " draws / " + shadowVerts / 1000 + "k   garbage " + Math.round(allocKbPerFrame) + " KB/frame";
      String mode = (exact ? "exact (every phase waits for the chip: Ctrl+F3 switches it off)" : "fast (Ctrl+F3: exact)") + "   " + gpu;
      if (!haveGpu) {
         return new String[]{head, mode, c1.toString(), c2.toString(), queries ? "GPU times: collecting ..." : "GPU times: this chip has no timer queries - use exact mode"};
      }

      return new String[]{head, mode, c1.toString(), c2.toString(), g1.toString(), g2.toString(), "GPU total " + f1(avgGpuTotal) + " ms"};
   }

   /** Shift+F3: show / hide the panel (and start / stop measuring). */
   static void toggleShow(File logFile) {
      show = !show;
      on = show || PerfBench.running();
      if (show) {
         log = logFile;
         logLines = 0;
         lastAlloc = 0L;
      } else {
         log = null;
         exact = false;
      }
   }

   /** Ctrl+F3: exact mode on / off. */
   static void toggleExact() {
      exact = !exact;
      if (exact) {
         on = true;
      }
   }

   /** The bytes this thread has allocated so far (0 where the JVM cannot say): the garbage the frame makes. */
   static final class Alloc {
      private static boolean fail;
      private static Object bean;

      static long bytes() {
         if (fail) {
            return 0L;
         }

         try {
            if (bean == null) {
               bean = java.lang.management.ManagementFactory.getThreadMXBean();
            }

            return ((com.sun.management.ThreadMXBean)bean).getThreadAllocatedBytes(Thread.currentThread().getId());
         } catch (Throwable t) {
            fail = true;
            return 0L;
         }
      }
   }
}
