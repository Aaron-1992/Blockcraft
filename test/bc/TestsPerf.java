package bc;

import static bc.GameTests.check;
import static bc.GameTests.eq;
import static bc.GameTests.test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Headless tests of the performance work of Update 3.18 / 3.19: the choice of the graphics chip (GpuPref) and the measuring and adapting
 * code that follows it.   python3 tools/runtests.py unit
 */
final class TestsPerf {
   static void run() {
      gpu();
      frames();
   }

   // ------------------------------------------------------------------ the frame timer
   /** A clock the test moves by hand (milliseconds). */
   static final long[] NOW = new long[1];

   static void at(double ms) {
      NOW[0] = (long)(ms * 1e6);
   }

   static void frames() {
      test("perf: a scripted frame - the numbers come out as the clock said", () -> {
         java.util.function.LongSupplier savedClock = Perf.clock;
         try {
            Perf.clock = () -> NOW[0];
            Perf.on = true;
            Perf.exact = false;
            at(0);
            Perf.frame();
            Perf.resetWindow();
            double t = 0;
            for (int i = 0; i < 50; i++) {
               // frame start at t: update until t+2, sky until t+5, terrain until t+9, gui until t+14, swap until t+16 (the next frame)
               at(t + 2);
               Perf.mark(Perf.SKY);
               at(t + 5);
               Perf.mark(Perf.TERRAIN);
               at(t + 9);
               Perf.mark(Perf.GUI);
               at(t + 14);
               Perf.mark(Perf.SWAP);
               t += 16;
               at(t + (i == 20 ? 40 : 0)); // one slow frame: 40 ms more
               if (i == 20) {
                  t += 40;
               }

               Perf.frame();
            }

            Perf.flush();
            near(2.0, Perf.avgCpu[Perf.UPDATE], 0.01, "update");
            near(3.0, Perf.avgCpu[Perf.SKY], 0.01, "sky");
            near(4.0, Perf.avgCpu[Perf.TERRAIN], 0.01, "terrain (the slow frame's extra time is in the swap)");
            near(5.0, Perf.avgCpu[Perf.GUI], 0.01, "gui");
            check(Perf.avgCpu[Perf.SWAP] > 2.5 && Perf.avgCpu[Perf.SWAP] < 3.5, "swap " + Perf.avgCpu[Perf.SWAP]);
            check(Perf.peakCpu[Perf.SWAP] > 41.0, "the worst swap is the slow frame: " + Perf.peakCpu[Perf.SWAP]);
            near(Perf.avgCpu[Perf.UPDATE] + Perf.avgCpu[Perf.SKY] + Perf.avgCpu[Perf.TERRAIN] + Perf.avgCpu[Perf.GUI] + Perf.avgCpu[Perf.SWAP], Perf.avgFrame, 0.02, "the phases add up to the frame");
            check(Perf.peakFrame > 55.0 && Perf.peakFrame < 57.0, "worst frame " + Perf.peakFrame);
            check(Perf.fps >= 50 && Perf.fps <= 65, "50 frames in 0.84 s: " + Perf.fps);
            check(!Perf.haveGpu, "no GPU timers without a context");
            String[] lines = Perf.lines("test chip");
            check(lines.length >= 5 && lines[0].startsWith("Frame ") && lines[1].contains("test chip"), "the panel has its lines: " + String.join(" | ", lines));
         } finally {
            Perf.clock = savedClock;
            Perf.on = false;
         }
      });

      test("perf: nothing is measured while it is off, and perf.log gets one line per second after a header", () -> {
         java.util.function.LongSupplier savedClock = Perf.clock;
         File dir = java.nio.file.Files.createTempDirectory("perf").toFile();
         try {
            Perf.clock = () -> NOW[0];
            Perf.on = false;
            at(0);
            Perf.frame();
            at(5);
            Perf.mark(Perf.SKY); // ignored
            at(2000);
            Perf.frame();
            Perf.flush();
            // (flush only publishes a window that has frames)
            File log = new File(dir, "perf.log");
            Perf.on = true;
            Perf.log = log;
            at(3000);
            Perf.resetWindow();
            Perf.frame();
            double t = 3000;
            for (int sec = 0; sec < 3; sec++) {
               for (int i = 0; i < 70; i++) {
                  at(t + 3);
                  Perf.mark(Perf.SKY);
                  at(t + 10);
                  Perf.mark(Perf.SWAP);
                  t += 15;
                  at(t);
                  Perf.frame();
               }
            }

            java.util.List<String> lines = java.nio.file.Files.readAllLines(log.toPath());
            check(lines.size() >= 3 && lines.get(0).startsWith("fps;frame_avg_ms;") && lines.get(0).contains(";cpu_terrain;") && lines.get(0).contains(";gpu_swap"), "a header: " + lines);
            check(lines.get(1).split(";", -1).length == lines.get(0).split(";", -1).length, "a line has as many fields as the header: " + lines.get(1));
            check(lines.get(1).startsWith("66;") || lines.get(1).startsWith("67;") || lines.get(1).startsWith("65;"), "15 ms frames are 66 fps: " + lines.get(1));
         } finally {
            Perf.clock = savedClock;
            Perf.on = false;
            Perf.log = null;
            Tests311.deleteAll(dir);
         }
      });

      test("perf: the benchmark's list of settings has the names the report prints, and the leaf / shadow switches exist", () -> {
         eq(8, PerfBench.PRESETS.length, "settings");
         check(PerfBench.PRESETS[0].equals("as configured") && PerfBench.PRESETS[1].equals("exact phases"), "the first two");
         check(!PerfBench.enabled() && !PerfBench.running(), "off by default");
         check(!Perf.noShadow && !Perf.noCull, "switches are off");
      });
   }

   static void near(double expected, double actual, double eps, String what) {
      check(Math.abs(expected - actual) <= eps, what + ": expected " + expected + ", got " + actual);
   }

   /** A registry that lives in a map: answers reg query / add / delete the way reg.exe does. */
   static GpuPref.Runner fakeRegistry(Map<String, String> reg, List<String> log) {
      return cmd -> {
         log.add(String.join(" ", cmd));
         if (!cmd[0].equals("reg") || !cmd[2].equals(GpuPref.KEY)) {
            return new GpuPref.Out(1, "wrong command");
         }

         String name = cmd[4];
         switch (cmd[1]) {
            case "query":
               if (!reg.containsKey(name)) {
                  return new GpuPref.Out(1, "ERROR: The system was unable to find the specified registry key or value.");
               }

               return new GpuPref.Out(0, "\r\n" + GpuPref.KEY.replace("HKCU", "HKEY_CURRENT_USER") + "\r\n    " + name + "    REG_SZ    " + reg.get(name) + "\r\n\r\n");
            case "add":
               check(cmd[5].equals("/t") && cmd[6].equals("REG_SZ") && cmd[7].equals("/d") && cmd[9].equals("/f"), "add: type, data and force switches");
               reg.put(name, cmd[8]);
               return new GpuPref.Out(0, "The operation completed successfully.");
            case "delete":
               return reg.remove(name) != null ? new GpuPref.Out(0, "The operation completed successfully.") : new GpuPref.Out(1, "ERROR: not found");
            default:
               return new GpuPref.Out(1, "?");
         }
      };
   }

   static void gpu() {
      test("gpu: the commands are reg.exe's and the output of reg query is read the way it is printed", () -> {
         eq("[reg, query, HKCU\\Software\\Microsoft\\DirectX\\UserGpuPreferences, /v, C:\\Games\\jre\\bin\\javaw.exe]", Arrays.toString(GpuPref.cmdQuery("C:\\Games\\jre\\bin\\javaw.exe")), "query");
         eq("[reg, add, HKCU\\Software\\Microsoft\\DirectX\\UserGpuPreferences, /v, C:\\a b\\javaw.exe, /t, REG_SZ, /d, GpuPreference=2;, /f]", Arrays.toString(GpuPref.cmdAdd("C:\\a b\\javaw.exe")), "add (a path with a space stays one argument)");
         eq("[reg, delete, HKCU\\Software\\Microsoft\\DirectX\\UserGpuPreferences, /v, C:\\a\\javaw.exe, /f]", Arrays.toString(GpuPref.cmdDelete("C:\\a\\javaw.exe")), "delete");
         String real = "\r\nHKEY_CURRENT_USER\\Software\\Microsoft\\DirectX\\UserGpuPreferences\r\n    C:\\Users\\Me\\Desktop\\Blockcraft\\jre\\bin\\javaw.exe    REG_SZ    GpuPreference=2;\r\n\r\n";
         check(GpuPref.hasValue(real, "C:\\Users\\Me\\Desktop\\Blockcraft\\jre\\bin\\javaw.exe", GpuPref.HIGH), "found with Windows line ends");
         check(GpuPref.hasValue(real, "c:\\users\\me\\desktop\\blockcraft\\jre\\bin\\JAVAW.EXE", GpuPref.HIGH), "Windows paths do not care about capitals");
         check(!GpuPref.hasValue(real, "C:\\Users\\Me\\Desktop\\Blockcraft\\jre\\bin\\java.exe", GpuPref.HIGH), "another program is not found");
         check(!GpuPref.hasValue(real.replace("=2;", "=1;"), "C:\\Users\\Me\\Desktop\\Blockcraft\\jre\\bin\\javaw.exe", GpuPref.HIGH), "power saving is not high performance");
         check(!GpuPref.hasValue("ERROR: The system was unable to find the specified registry key or value.", "C:\\x.exe", GpuPref.HIGH) && !GpuPref.hasValue(null, "C:\\x.exe", GpuPref.HIGH), "an error / nothing is not found");
      });

      test("gpu: the programs that draw are the Java of the game (javaw.exe, java.exe) - only files that exist, every one once", () -> {
         File dir = java.nio.file.Files.createTempDirectory("jre").toFile();
         try {
            File bin = new File(dir, "bin");
            bin.mkdirs();
            File w = new File(bin, "javaw.exe");
            File j = new File(bin, "java.exe");
            java.nio.file.Files.write(w.toPath(), new byte[]{1});
            java.nio.file.Files.write(j.toPath(), new byte[]{1});
            List<String> p = GpuPref.programs(dir.getPath(), w.getPath());
            eq(2, p.size(), "javaw.exe (the running one, found twice) and java.exe");
            check(p.get(0).equals(w.getPath()) && p.get(1).equals(j.getPath()), "in that order");
            eq(2, GpuPref.programs(dir.getPath(), null).size(), "without process information java.home is enough");
            eq(0, GpuPref.programs(dir.getPath() + "x", null).size(), "no Java there, nothing to register");
            eq(1, GpuPref.programs(dir.getPath() + "x", w.getPath()).size(), "a running program that is no .exe of this Java is still registered");
            eq(0, GpuPref.programs(dir.getPath() + "x", "/usr/bin/java").size(), "on another system (no .exe) nothing happens");
         } finally {
            Tests311.deleteAll(dir);
         }
      });

      test("gpu: apply writes high performance for every program once, leaves it alone when it is there, takes only its own entries out again", () -> {
         Map<String, String> reg = new HashMap<>();
         List<String> log = new ArrayList<>();
         GpuPref.Runner saved = GpuPref.runner;
         GpuPref.runner = fakeRegistry(reg, log);
         try {
            List<String> progs = Arrays.asList("C:\\g\\jre\\bin\\javaw.exe", "C:\\g\\jre\\bin\\java.exe");
            eq(2, GpuPref.apply(true, progs), "two entries written");
            eq("GpuPreference=2;", reg.get("C:\\g\\jre\\bin\\javaw.exe"), "the data");
            log.clear();
            eq(0, GpuPref.apply(true, progs), "the second start changes nothing");
            check(log.size() == 2 && log.get(0).contains("query") && log.get(1).contains("query"), "it only asks: " + log);
            reg.put("C:\\g\\jre\\bin\\java.exe", "GpuPreference=1;"); // the player chose power saving by hand in Windows
            eq(1, GpuPref.apply(true, progs), "a different choice is replaced when the game is told to use the real card");
            eq("GpuPreference=2;", reg.get("C:\\g\\jre\\bin\\java.exe"), "replaced");
            reg.put("C:\\g\\jre\\bin\\java.exe", "GpuPreference=1;");
            eq(1, GpuPref.apply(false, progs), "windows decides: our entry goes");
            check(!reg.containsKey("C:\\g\\jre\\bin\\javaw.exe"), "javaw.exe is gone");
            eq("GpuPreference=1;", reg.get("C:\\g\\jre\\bin\\java.exe"), "the player's own choice stays");
            eq(0, GpuPref.apply(false, progs), "nothing more to take out");
            // a failing reg.exe (no rights, policy) is no crash
            GpuPref.runner = cmd -> new GpuPref.Out(1, "ERROR: Access is denied.");
            eq(0, GpuPref.apply(true, progs), "nothing could be written: nothing changed");
         } finally {
            GpuPref.runner = saved;
         }
      });

      test("gpu: Options.gpuHigh is on by default, is saved and read again, and apply(Options) does nothing outside Windows or with -Dbc.gpu=off", () -> {
         File dir = java.nio.file.Files.createTempDirectory("opt").toFile();
         Boolean savedWin = GpuPref.forceWindows;
         GpuPref.Runner savedRunner = GpuPref.runner;
         try {
            Options o = new Options(dir);
            check(o.gpuHigh, "default: high performance");
            o.gpuHigh = false;
            o.save();
            Options p = new Options(dir);
            p.load();
            check(!p.gpuHigh, "saved as off");
            o.gpuHigh = true;
            o.save();
            p.load();
            check(p.gpuHigh, "saved as on");
            List<String> log = new ArrayList<>();
            GpuPref.runner = cmd -> {
               log.add(String.join(" ", cmd));
               return new GpuPref.Out(1, "");
            };
            GpuPref.forceWindows = false;
            check(!GpuPref.apply(o) && log.isEmpty(), "not on Windows: no command is run");
            GpuPref.forceWindows = true;
            System.setProperty("bc.gpu", "off");
            check(!GpuPref.apply(o) && log.isEmpty(), "-Dbc.gpu=off: no command is run");
         } finally {
            System.clearProperty("bc.gpu");
            GpuPref.forceWindows = savedWin;
            GpuPref.runner = savedRunner;
            Tests311.deleteAll(dir);
         }
      });

      test("gpu: which renderer strings are a chip inside the processor (the ones that are worth a second start)", () -> {
         String[] small = {
            "Intel(R) UHD Graphics 630", "Intel(R) HD Graphics 620", "Intel(R) Iris(R) Xe Graphics", "Intel(R) Iris(R) Plus Graphics", "Intel(R) UHD Graphics",
            "AMD Radeon(TM) Graphics", "AMD Radeon(TM) Vega 8 Graphics", "AMD Radeon(TM) RX Vega 11 Graphics", "AMD Radeon 780M Graphics", "AMD Radeon(TM) 610M", "Intel(R) UHD Graphics 770"
         };
         String[] big = {
            "NVIDIA GeForce RTX 3060 Laptop GPU/PCIe/SSE2", "NVIDIA GeForce GTX 1650/PCIe/SSE2", "Quadro T1000/PCIe/SSE2", "AMD Radeon RX 6600M", "AMD Radeon RX 580 Series", "AMD Radeon RX 7800 XT",
            "AMD Radeon(TM) RX 5700 XT", "Radeon RX 6800M", "AMD Radeon Pro 5500M", "Intel(R) Arc(TM) A770 Graphics", "llvmpipe (LLVM 17.0.6, 256 bits)", "Microsoft Basic Render Driver", "Mesa Software Renderer", "", null
         };
         for (String s : small) {
            check(GpuPref.isIntegrated(s), "a small chip: " + s);
         }

         for (String s : big) {
            check(!GpuPref.isIntegrated(s), "not a small chip: " + s);
         }
      });

      test("gpu: the names on the screens are short and plain", () -> {
         eq("NVIDIA GeForce RTX 3060 Laptop GPU", GpuPref.shortName("NVIDIA GeForce RTX 3060 Laptop GPU/PCIe/SSE2"), "the driver's suffix goes");
         eq("Intel UHD Graphics 630", GpuPref.shortName("Intel(R) UHD Graphics 630"), "(R) goes");
         eq("AMD Radeon Graphics", GpuPref.shortName("AMD Radeon(TM) Graphics"), "(TM) goes");
         eq("llvmpipe", GpuPref.shortName("llvmpipe (LLVM 17.0.6, 256 bits)"), "the software renderer");
         eq("unknown", GpuPref.shortName(""), "nothing known");
         eq("unknown", GpuPref.shortName(null), "null");
      });

      test("gpu: the second start is the same Java with the same options, the same jar and the same main class, without a debugger", () -> {
         File dir = java.nio.file.Files.createTempDirectory("jre2").toFile();
         try {
            File bin = new File(dir, "bin");
            bin.mkdirs();
            java.nio.file.Files.write(new File(bin, "javaw.exe").toPath(), new byte[]{1});
            List<String> cmd = GpuPref.relaunchCommand(dir.getPath(), Arrays.asList("-Xmx2G", "-Xms256m", "-agentlib:jdwp=transport=dt_socket", "-XX:+UseG1GC", "-Dbc.auto=1"), "C:\\g\\Blockcraft.exe", "bc.Main");
            eq(new File(bin, "javaw.exe").getPath(), cmd.get(0), "javaw.exe of the same Java");
            eq(Arrays.asList("-Xmx2G", "-Xms256m", "-XX:+UseG1GC", "-Dbc.auto=1"), cmd.subList(1, 5), "the options (without the debugger)");
            eq(Arrays.asList("-cp", "C:\\g\\Blockcraft.exe", "bc.Main"), cmd.subList(5, 8), "the jar (the exe is a jar) and the main class");
            eq(8, cmd.size(), "nothing else");
            eq("bc.Main", Main.class.getName(), "the class exists");
         } finally {
            Tests311.deleteAll(dir);
         }
      });
   }
}
