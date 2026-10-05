package bc;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Update 3.18: which graphics chip draws the game.
 *
 * A laptop (and some desktops) has two: a small one inside the processor (Intel UHD / Iris, AMD "Radeon Graphics") and a real one (GeForce, Radeon RX).
 * Windows gives a program the small one unless it is told otherwise, and a Java game never tells it - the whole game ran on the small chip. Since
 * Windows 10 (April 2018) "Settings > System > Display > Graphics" keeps a preference per program in
 * {@code HKCU\Software\Microsoft\DirectX\UserGpuPreferences}: the name of the value is the full path of the .exe, its data {@code GpuPreference=2;}
 * means "high performance" (the real chip), {@code 1} power saving, {@code 0} let Windows decide. The program that draws is the {@code javaw.exe}
 * of the Java that runs the game and its folder is wherever the player unpacked the zip, so the game writes the entries itself, every start, before
 * it opens its window (a start that finds them there does nothing). A process picks its chip when it first loads the graphics driver, so if the
 * entry was only just written and the window turns out to be on a small chip anyway, the game starts itself once more ({@link #relaunch}).
 *
 * Everything that touches the system goes through {@link #runner}, so the tests can run it against a pretend registry.
 */
final class GpuPref {
   static final String KEY = "HKCU\\Software\\Microsoft\\DirectX\\UserGpuPreferences";
   static final String HIGH = "GpuPreference=2;";
   /** Set (to the old renderer's name) in the environment of the second start so that it never starts a third. */
   static final String RETRY_ENV = "BLOCKCRAFT_GPU_RETRY";

   private GpuPref() {
   }

   /** What a command did: its exit code and everything it printed. */
   static final class Out {
      final int code;
      final String text;

      Out(int code, String text) {
         this.code = code;
         this.text = text;
      }
   }

   interface Runner {
      Out run(String[] cmd);
   }

   /** Runs a command and waits for it (at most 8 seconds); never throws. */
   static final Runner SYSTEM = cmd -> {
      try {
         Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
         byte[] all = p.getInputStream().readAllBytes();
         if (!p.waitFor(8, TimeUnit.SECONDS)) {
            p.destroyForcibly();
            return new Out(-1, "timeout");
         }

         return new Out(p.exitValue(), new String(all, java.nio.charset.Charset.defaultCharset()));
      } catch (Exception e) {
         return new Out(-1, String.valueOf(e));
      }
   };

   static Runner runner = SYSTEM;
   /** Tests pretend to be on Windows. */
   static Boolean forceWindows;

   static boolean windows() {
      return forceWindows != null ? forceWindows : System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
   }

   // ------------------------------------------------------------------ the registry entries
   static String[] cmdQuery(String exe) {
      return new String[]{"reg", "query", KEY, "/v", exe};
   }

   static String[] cmdAdd(String exe) {
      return new String[]{"reg", "add", KEY, "/v", exe, "/t", "REG_SZ", "/d", HIGH, "/f"};
   }

   static String[] cmdDelete(String exe) {
      return new String[]{"reg", "delete", KEY, "/v", exe, "/f"};
   }

   /** Does the output of {@code reg query} show this program with this data? (reg prints "    <name>    REG_SZ    <data>".) */
   static boolean hasValue(String out, String exe, String data) {
      if (out == null) {
         return false;
      }

      for (String line : out.split("\\r?\\n")) {
         String t = line.trim();
         if (t.regionMatches(true, 0, exe, 0, exe.length()) && t.toLowerCase(Locale.ROOT).contains("reg_sz") && t.endsWith(data)) {
            return true;
         }
      }

      return false;
   }

   /** The programs that draw: the Java that runs us ({@code javaw.exe}, {@code java.exe}) and the one that was started to do so. Only files that exist. */
   static List<String> programs(String javaHome, String command) {
      List<String> all = new ArrayList<>();
      List<String> seen = new ArrayList<>();
      List<String> cand = new ArrayList<>();
      if (command != null) {
         cand.add(command);
      }

      if (javaHome != null) {
         cand.add(new File(new File(javaHome, "bin"), "javaw.exe").getPath());
         cand.add(new File(new File(javaHome, "bin"), "java.exe").getPath());
      }

      for (String c : cand) {
         String key = c.toLowerCase(Locale.ROOT);
         if (key.endsWith(".exe") && !seen.contains(key) && new File(c).isFile()) {
            seen.add(key);
            all.add(c);
         }
      }

      return all;
   }

   static List<String> programs() {
      String cmd = null;
      try {
         cmd = ProcessHandle.current().info().command().orElse(null);
      } catch (Throwable t) {
         // no process information: java.home is enough
      }

      return programs(System.getProperty("java.home"), cmd);
   }

   /**
    * Makes the entries match what the player wants: {@code high} writes "high performance" for every program, otherwise the entries this game wrote
    * are taken out again (an entry the player set to something else is left alone). Returns how many entries were changed.
    */
   static int apply(boolean high, List<String> programs) {
      int changed = 0;
      for (String exe : programs) {
         Out q = runner.run(cmdQuery(exe));
         boolean there = q.code == 0 && hasValue(q.text, exe, HIGH);
         if (high && !there) {
            if (runner.run(cmdAdd(exe)).code == 0) {
               changed++;
            }
         } else if (!high && there) {
            if (runner.run(cmdDelete(exe)).code == 0) {
               changed++;
            }
         }
      }

      return changed;
   }

   /** Called once before the window is made. True when an entry was written / removed just now (this process may still be on the old chip). */
   static boolean apply(Options o) {
      if (!windows() || System.getProperty("bc.gpu") != null && System.getProperty("bc.gpu").equals("off")) {
         return false;
      }

      try {
         return apply(o.gpuHigh, programs()) > 0;
      } catch (Throwable t) {
         System.err.println("GPU preference: " + t);
         return false;
      }
   }

   // ------------------------------------------------------------------ which chip is it?
   /** A graphics chip that is part of the processor (what a laptop's "small" chip is called), judged by the renderer string the driver gives. */
   static boolean isIntegrated(String renderer) {
      if (renderer == null) {
         return false;
      }

      String r = renderer.toLowerCase(Locale.ROOT);
      if (r.contains("nvidia") || r.contains("geforce") || r.contains("quadro") || r.contains("llvmpipe") || r.contains("software") || r.contains("basic render")) {
         return false;
      }

      if (r.contains("intel")) {
         return !r.contains(" arc"); // UHD, HD, Iris, Xe are part of the processor; the Arc cards are real cards
      }

      if (r.contains("radeon") || r.contains("amd")) {
         // the ones inside a processor are called "... Graphics" ("AMD Radeon(TM) Graphics", "Radeon Vega 8 Graphics", "Radeon RX Vega 11 Graphics")
         // or "780M"; the cards have a model number behind RX ("Radeon RX 6600M", "Radeon RX 580 Series") or no "Graphics" at all
         boolean card = r.matches(".*\\brx\\s?[0-9].*");
         return !card && (r.contains("graphics") || r.matches(".*\\b[0-9]{3}m\\b.*"));
      }

      return false;
   }

   /** A short name for the screens: "NVIDIA GeForce RTX 3060 Laptop GPU/PCIe/SSE2" -> "NVIDIA GeForce RTX 3060 Laptop GPU". */
   static String shortName(String renderer) {
      if (renderer == null || renderer.isEmpty()) {
         return "unknown";
      }

      String s = renderer;
      int cut = s.indexOf("/PCIe");
      if (cut > 0) {
         s = s.substring(0, cut);
      }

      cut = s.indexOf(" (");
      if (cut > 0 && s.toLowerCase(Locale.ROOT).startsWith("llvmpipe")) {
         s = s.substring(0, cut);
      }

      return s.replace("(R)", "").replace("(TM)", "").replace("  ", " ").trim();
   }

   // ------------------------------------------------------------------ the second start
   /**
    * The command that starts the game again exactly as it was started: the same Java, the same options, the same jar (or the exe, which is a jar).
    * {@code inputArgs} are the JVM options of this process ({@code -Xmx2G ...}).
    */
   static List<String> relaunchCommand(String javaHome, List<String> inputArgs, String classPath, String mainClass) {
      List<String> cmd = new ArrayList<>();
      File w = new File(new File(javaHome, "bin"), "javaw.exe");
      cmd.add(w.isFile() ? w.getPath() : new File(new File(javaHome, "bin"), "java").getPath());
      for (String a : inputArgs) {
         if (!a.startsWith("-agentlib:jdwp")) {
            cmd.add(a);
         }
      }

      cmd.add("-cp");
      cmd.add(classPath);
      cmd.add(mainClass);
      return cmd;
   }

   /** Starts the game once more (see the class comment). True if the new process is running; the caller then ends this one. */
   static boolean relaunch(String oldRenderer) {
      try {
         List<String> args = new ArrayList<>();
         try {
            args.addAll(java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments());
         } catch (Throwable t) {
            args.add("-Xmx2G"); // no management module: the options of the launcher
            args.add("-Xms256m");
            args.add("-XX:+UseG1GC");
         }

         List<String> cmd = relaunchCommand(System.getProperty("java.home"), args, System.getProperty("java.class.path"), Main.class.getName());
         ProcessBuilder pb = new ProcessBuilder(cmd);
         pb.environment().put(RETRY_ENV, oldRenderer == null || oldRenderer.isEmpty() ? "1" : oldRenderer);
         pb.redirectErrorStream(true);
         pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
         Process p = pb.start();
         // a start that works takes longer than this to get a window: if the new process is gone already, it did not work and this one goes on
         if (p.waitFor(2500, TimeUnit.MILLISECONDS)) {
            System.err.println("GPU preference: the second start ended at once (exit " + p.exitValue() + "); going on with this one");
            return false;
         }

         return true;
      } catch (Throwable t) {
         System.err.println("GPU preference: could not start again: " + t);
         return false;
      }
   }

   /** Is this the second start (after {@link #relaunch})? Then the renderer of the first one is in the environment. */
   static String retryOf() {
      String v = System.getenv(RETRY_ENV);
      return v == null || v.isEmpty() ? null : v;
   }
}
