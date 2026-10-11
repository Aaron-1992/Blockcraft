package bc;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Update 3.18 / 3.19: which graphics chip draws the game.
 *
 * A laptop (and some desktops) has two: a small one inside the processor (Intel UHD / Iris, AMD "Radeon Graphics") and a real one (GeForce, Radeon RX).
 * Windows and the NVIDIA Optimus / AMD Switchable Graphics drivers give {@code javaw.exe} the small chip unless three things come together:
 *  1. {@code HKCU\Software\Microsoft\DirectX\UserGpuPreferences} maps the executable path to {@code GpuPreference=2;};
 *  2. the executable is not called {@code javaw.exe}/{@code java.exe} (whose driver profile forces power saving) and its PE export directory exports
 *     {@code NvOptimusEnablement = 1} and {@code AmdPowerXpressRequestHighPerformance = 1} ({@link #ensureGpuExe});
 *  3. {@code SHIM_MCCOMPAT=0x800000001} is present in the environment when the process is created ({@link #relaunch}).
 *
 * Everything that touches the system goes through {@link #runner}, so the tests can run it against a pretend registry.
 */
final class GpuPref {
   static final String KEY = "HKCU\\Software\\Microsoft\\DirectX\\UserGpuPreferences";
   static final String HIGH = "GpuPreference=2;";
   /** Set (to the old renderer's name) in the environment of the second start so that it never starts a third. */
   static final String RETRY_ENV = "BLOCKCRAFT_GPU_RETRY";
   /** Dedicated-GPU wrapper copy of javaw.exe with NvOptimusEnablement and AmdPowerXpressRequestHighPerformance PE exports. */
   static final String GPU_EXE = "blockcraft-gpu.exe";

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

   // ------------------------------------------------------------------ PE export table patching (NvOptimusEnablement / AmdPowerXpressRequestHighPerformance)
   private static int r16(byte[] b, int o) {
      return (b[o] & 0xFF) | ((b[o + 1] & 0xFF) << 8);
   }

   private static int r32(byte[] b, int o) {
      return (b[o] & 0xFF) | ((b[o + 1] & 0xFF) << 8) | ((b[o + 2] & 0xFF) << 16) | ((b[o + 3] & 0xFF) << 24);
   }

   private static void w16(byte[] b, int o, int v) {
      b[o] = (byte) v;
      b[o + 1] = (byte) (v >>> 8);
   }

   private static void w32(byte[] b, int o, int v) {
      b[o] = (byte) v;
      b[o + 1] = (byte) (v >>> 8);
      b[o + 2] = (byte) (v >>> 16);
      b[o + 3] = (byte) (v >>> 24);
   }

   private static int rvaToRaw(byte[] pe, int secTable, int numSec, int rva) {
      for (int i = 0; i < numSec; i++) {
         int sh = secTable + i * 40;
         int vSize = r32(pe, sh + 8);
         int vAddr = r32(pe, sh + 12);
         int rawSize = r32(pe, sh + 16);
         int rawPtr = r32(pe, sh + 20);
         int span = Math.max(vSize, rawSize);
         if (rva >= vAddr && rva < vAddr + span) {
            int off = rawPtr + (rva - vAddr);
            return off >= 0 && off < pe.length ? off : -1;
         }
      }

      return -1;
   }

   private static String readAsciiZ(byte[] b, int off) {
      if (off < 0 || off >= b.length) {
         return "";
      }

      int end = off;
      while (end < b.length && b[end] != 0) {
         end++;
      }

      return new String(b, off, end - off, StandardCharsets.US_ASCII);
   }

   /** True if {@code pe} is a valid PE executable whose Export Directory exports both GPU high-performance symbols with value 1. */
   static boolean hasPeExports(byte[] pe) {
      if (pe == null || pe.length < 1024 || (pe[0] & 0xFF) != 'M' || (pe[1] & 0xFF) != 'Z') {
         return false;
      }

      int peOff = r32(pe, 0x3C);
      if (peOff < 64 || peOff + 264 > pe.length || pe[peOff] != 'P' || pe[peOff + 1] != 'E' || pe[peOff + 2] != 0 || pe[peOff + 3] != 0) {
         return false;
      }

      int numSec = r16(pe, peOff + 6);
      int optSize = r16(pe, peOff + 20);
      int optOff = peOff + 24;
      int magic = r16(pe, optOff);
      int ddOff = magic == 0x10B ? optOff + 96 : (magic == 0x20B ? optOff + 112 : -1);
      int secTable = optOff + optSize;
      if (ddOff < 0 || secTable + numSec * 40 > pe.length) {
         return false;
      }

      int expRva = r32(pe, ddOff);
      int expSize = r32(pe, ddOff + 4);
      if (expRva == 0 || expSize < 40) {
         return false;
      }

      int expRaw = rvaToRaw(pe, secTable, numSec, expRva);
      if (expRaw < 0 || expRaw + 40 > pe.length) {
         return false;
      }

      int numFuncs = r32(pe, expRaw + 20);
      int numNames = r32(pe, expRaw + 24);
      int funcsRva = r32(pe, expRaw + 28);
      int namesRva = r32(pe, expRaw + 32);
      int ordsRva = r32(pe, expRaw + 36);
      int funcsRaw = rvaToRaw(pe, secTable, numSec, funcsRva);
      int namesRaw = rvaToRaw(pe, secTable, numSec, namesRva);
      int ordsRaw = rvaToRaw(pe, secTable, numSec, ordsRva);
      if (numNames < 2 || numFuncs < 2 || funcsRaw < 0 || namesRaw < 0 || ordsRaw < 0) {
         return false;
      }

      boolean hasAmd = false;
      boolean hasNv = false;
      for (int i = 0; i < numNames; i++) {
         int nameRva = r32(pe, namesRaw + i * 4);
         int ord = r16(pe, ordsRaw + i * 2);
         if (ord >= numFuncs) {
            continue;
         }

         int symRva = r32(pe, funcsRaw + ord * 4);
         int nameRaw = rvaToRaw(pe, secTable, numSec, nameRva);
         int symRaw = rvaToRaw(pe, secTable, numSec, symRva);
         if (nameRaw < 0 || symRaw < 0 || symRaw + 4 > pe.length) {
            continue;
         }

         String name = readAsciiZ(pe, nameRaw);
         int val = r32(pe, symRaw);
         if ("AmdPowerXpressRequestHighPerformance".equals(name) && val == 1) {
            hasAmd = true;
         } else if ("NvOptimusEnablement".equals(name) && val == 1) {
            hasNv = true;
         }
      }

      return hasAmd && hasNv;
   }

   /**
    * Returns a copy of {@code pe} (PE32 or PE32+) with an Export Directory in the {@code .data} padding that exports
    * {@code AmdPowerXpressRequestHighPerformance = 1} and {@code NvOptimusEnablement = 1}, or {@code null} if {@code pe} is not a patchable PE file.
    */
   static byte[] patchPeExports(byte[] pe) {
      if (pe == null || pe.length < 1024 || (pe[0] & 0xFF) != 'M' || (pe[1] & 0xFF) != 'Z') {
         return null;
      }

      if (hasPeExports(pe)) {
         return pe.clone();
      }

      int peOff = r32(pe, 0x3C);
      if (peOff < 64 || peOff + 264 > pe.length || pe[peOff] != 'P' || pe[peOff + 1] != 'E' || pe[peOff + 2] != 0 || pe[peOff + 3] != 0) {
         return null;
      }

      int numSec = r16(pe, peOff + 6);
      int optSize = r16(pe, peOff + 20);
      int optOff = peOff + 24;
      int magic = r16(pe, optOff);
      int ddOff = magic == 0x10B ? optOff + 96 : (magic == 0x20B ? optOff + 112 : -1);
      int secTable = optOff + optSize;
      if (ddOff < 0 || secTable + numSec * 40 > pe.length || r32(pe, ddOff) != 0) {
         return null;
      }

      int targetSec = -1;
      int caveOff = 0;
      for (int pass = 0; pass < 2 && targetSec < 0; pass++) {
         for (int i = 0; i < numSec; i++) {
            int sh = secTable + i * 40;
            String name = readAsciiZ(pe, sh);
            int vSize = r32(pe, sh + 8);
            int rawSize = r32(pe, sh + 16);
            int rawPtr = r32(pe, sh + 20);
            int chars = r32(pe, sh + 36);
            boolean match = pass == 0 ? ".data".equals(name) : (chars & 0xC0000040) == 0xC0000040;
            if (!match) {
               continue;
            }

            int aligned = (vSize + 15) & ~15;
            if (aligned + 144 <= rawSize && rawPtr > 0 && rawPtr + aligned + 144 <= pe.length) {
               boolean allZero = true;
               for (int k = 0; k < 144; k++) {
                  if (pe[rawPtr + aligned + k] != 0) {
                     allZero = false;
                     break;
                  }
               }

               if (allZero) {
                  targetSec = sh;
                  caveOff = aligned;
                  break;
               }
            }
         }
      }

      if (targetSec < 0) {
         return null;
      }

      int vSize = r32(pe, targetSec + 8);
      int vAddr = r32(pe, targetSec + 12);
      int rawSize = r32(pe, targetSec + 16);
      int rawPtr = r32(pe, targetSec + 20);

      // Strip trailing Authenticode signature if present (modifying headers invalidates an existing signature).
      int certOff = r32(pe, ddOff + 32);
      int certSize = r32(pe, ddOff + 36);
      int outLen = certOff > 0 && certSize > 0 && certOff + certSize == pe.length && certOff >= rawPtr + rawSize ? certOff : pe.length;
      byte[] out = Arrays.copyOf(pe, outLen);
      if (outLen < pe.length) {
         w32(out, ddOff + 32, 0);
         w32(out, ddOff + 36, 0);
      }

      // Clear OptionalHeader.CheckSum.
      w32(out, optOff + 64, 0);

      byte[] modName = "blockcraft.exe\0".getBytes(StandardCharsets.US_ASCII);
      byte[] amdName = "AmdPowerXpressRequestHighPerformance\0".getBytes(StandardCharsets.US_ASCII);
      byte[] nvName = "NvOptimusEnablement\0".getBytes(StandardCharsets.US_ASCII);
      int modOff = 60;
      int amdOff = modOff + modName.length;
      int nvOff = amdOff + amdName.length;
      int expTotal = nvOff + nvName.length;

      int baseRva = vAddr + caveOff;
      int baseRaw = rawPtr + caveOff;
      // Exported DWORD values: AmdPowerXpressRequestHighPerformance = 1, NvOptimusEnablement = 1.
      w32(out, baseRaw, 1);
      w32(out, baseRaw + 4, 1);

      int expRva = baseRva + 8;
      int expRaw = baseRaw + 8;
      w32(out, expRaw + 12, expRva + modOff); // Name RVA ("blockcraft.exe\0")
      w32(out, expRaw + 16, 1);               // Base = 1
      w32(out, expRaw + 20, 2);               // NumberOfFunctions = 2
      w32(out, expRaw + 24, 2);               // NumberOfNames = 2
      w32(out, expRaw + 28, expRva + 40);     // AddressOfFunctions
      w32(out, expRaw + 32, expRva + 48);     // AddressOfNames
      w32(out, expRaw + 36, expRva + 56);     // AddressOfNameOrdinals

      w32(out, expRaw + 40, baseRva);         // Function[0] -> DWORD 1
      w32(out, expRaw + 44, baseRva + 4);     // Function[1] -> DWORD 1

      w32(out, expRaw + 48, expRva + amdOff); // Name[0] -> "AmdPowerXpressRequestHighPerformance\0"
      w32(out, expRaw + 52, expRva + nvOff);  // Name[1] -> "NvOptimusEnablement\0"

      w16(out, expRaw + 56, 0); // Ordinal[0] = 0
      w16(out, expRaw + 58, 1); // Ordinal[1] = 1

      System.arraycopy(modName, 0, out, expRaw + modOff, modName.length);
      System.arraycopy(amdName, 0, out, expRaw + amdOff, amdName.length);
      System.arraycopy(nvName, 0, out, expRaw + nvOff, nvName.length);

      w32(out, targetSec + 8, Math.max(vSize, caveOff + 8 + expTotal));
      w32(out, ddOff, expRva);
      w32(out, ddOff + 4, expTotal);
      return out;
   }

   /**
    * Ensures {@code <javaHome>/bin/blockcraft-gpu.exe} exists as a PE-patched copy of {@code javaw.exe} that exports
    * {@code NvOptimusEnablement = 1} and {@code AmdPowerXpressRequestHighPerformance = 1}. Returns the file if created/valid, or {@code null}.
    */
   static File ensureGpuExe(String javaHome) {
      if (javaHome == null) {
         return null;
      }

      File bin = new File(javaHome, "bin");
      File src = new File(bin, "javaw.exe");
      if (!src.isFile() || src.length() < 1024L) {
         return null;
      }

      File dst = new File(bin, GPU_EXE);
      try {
         if (dst.isFile() && dst.length() >= 1024L) {
            byte[] existing = Files.readAllBytes(dst.toPath());
            if (hasPeExports(existing)) {
               return dst;
            }
         }

         byte[] raw = Files.readAllBytes(src.toPath());
         byte[] patched = patchPeExports(raw);
         if (patched == null) {
            return null;
         }

         Files.write(dst.toPath(), patched);
         dst.setExecutable(true);
         return dst;
      } catch (Throwable t) {
         return dst.isFile() ? dst : null;
      }
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

   /** The programs that draw: the Java that runs us ({@code blockcraft-gpu.exe}, {@code javaw.exe}, {@code java.exe}) and the one that was started to do so. Only files that exist. */
   static List<String> programs(String javaHome, String command) {
      List<String> all = new ArrayList<>();
      List<String> seen = new ArrayList<>();
      List<String> cand = new ArrayList<>();
      if (command != null) {
         cand.add(command);
      }

      if (javaHome != null) {
         ensureGpuExe(javaHome);
         cand.add(new File(new File(javaHome, "bin"), GPU_EXE).getPath());
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

      List<String> list = programs(System.getProperty("java.home"), cmd);
      File launcher = new File(System.getProperty("user.dir", "."), "Blockcraft.exe");
      if (launcher.isFile()) {
         String p = launcher.getAbsolutePath();
         boolean found = false;
         for (String s : list) {
            if (s.equalsIgnoreCase(p)) {
               found = true;
               break;
            }
         }

         if (!found) {
            list.add(p);
         }
      }

      return list;
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
    * The command that starts the game again: prefers {@code blockcraft-gpu.exe} (with discrete-GPU PE exports) when {@code javaw.exe} is a real PE
    * binary, falling back to {@code javaw.exe} or {@code java}. {@code inputArgs} are the JVM options of this process ({@code -Xmx2G ...}).
    */
   static List<String> relaunchCommand(String javaHome, List<String> inputArgs, String classPath, String mainClass) {
      List<String> cmd = new ArrayList<>();
      File gpu = ensureGpuExe(javaHome);
      File w = new File(new File(javaHome, "bin"), "javaw.exe");
      if (gpu != null && gpu.isFile()) {
         cmd.add(gpu.getPath());
      } else if (w.isFile()) {
         cmd.add(w.getPath());
      } else {
         cmd.add(new File(new File(javaHome, "bin"), "java").getPath());
      }

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
         if (windows() && !"off".equals(System.getProperty("bc.gpu"))) {
            apply(true, programs());
         }

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
         pb.environment().put("SHIM_MCCOMPAT", "0x800000001");
         pb.environment().put("__GL_SHADER_DISK_CACHE", "1");
         pb.environment().put("__GL_SHADER_DISK_CACHE_SKIP_CLEANUP", "1");
         pb.environment().put("__GL_THREADED_OPTIMIZATIONS", "1");
         pb.environment().put("__NV_PRIME_RENDER_OFFLOAD", "1");
         pb.environment().put("DRI_PRIME", "1");
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

   public static void main(String[] args) throws Exception {
      for (String p : args) {
         File f = new File(p);
         byte[] patched = patchPeExports(Files.readAllBytes(f.toPath()));
         if (patched == null || !hasPeExports(patched)) {
            throw new IllegalStateException("Failed to patch PE exports in " + p);
         }

         Files.write(f.toPath(), patched);
         System.out.println("Patched PE exports in " + p + " (" + patched.length + " bytes)");
      }
   }
}
