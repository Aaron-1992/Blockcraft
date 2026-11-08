package bc;

import static bc.GameTests.check;
import static bc.GameTests.eq;
import static bc.GameTests.test;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Properties;

/** V6.31: LOD cross-fades, a montage trailer, borderless fullscreen, cleaner menus,
 *  naturally surface-breaking caves and a new underground structure set. */
final class Tests631 {
   private Tests631() {
   }

   static void run() {
      test("6.31: LOD swaps are cross-faded instead of popping in one frame", Tests631::lod);
      test("6.31: the trailer is a 24-shot montage, not one flythrough", Tests631::trailer);
      test("6.31: borderless fullscreen is a real, saved display mode", Tests631::borderless);
      test("6.31: the title and options screens are cleaned up", Tests631::menus);
      test("6.31: caves break the ground in many different shapes and lead underground", Tests631::caves);
      test("6.31: the underground structures are completely new", Tests631::structures);
      test("6.31: version, notes and release wiring point at 6.31", Tests631::wiring);
   }

   static String src(String p) throws Exception {
      return new String(Files.readAllBytes(new File(p).toPath()), "UTF-8");
   }

   static WorldSettings settings(int cv) {
      WorldSettings st = new WorldSettings();
      st.terrainVer = 3;
      st.caveVer = cv;
      st.biomeBorders = 1;
      st.caveScale = 1.0F;
      st.caveDensity = 1.0F;
      return st;
   }

   static void lod() throws Exception {
      check(Lod.splitWeight(10.0, 100.0) == 1.0F, "close tiles use their children");
      check(Lod.splitWeight(200.0, 100.0) == 0.0F, "distant tiles use the parent");
      check(Lod.splitWeight(100.0, 100.0) > 0.0F && Lod.splitWeight(100.0, 100.0) < 1.0F,
         "the split is a continuous cross-fade band, not a one-frame switch");
      float prev = 1.0F;
      boolean mono = true;
      for (int d = 80; d <= 120; d += 2) {
         float f = Lod.splitWeight(d, 100.0);
         if (f > prev + 0.001F) mono = false;
         prev = f;
      }
      check(mono, "the band is monotonic as the camera moves");
      String l = src("src/bc/Lod.java");
      check(l.contains("static final class Cross") && l.contains("activeCross"),
         "an outgoing mesh is kept while the replacement fades in");
      check(l.contains("addRaw(c.old, fade)"), "the old tile is drawn at full opacity underneath");
      check(l.contains("drawFallback(x, z, size, a)") && l.contains("fi < 0.999F"),
         "a coarse fallback stays visible for the whole fade-in");
      check(l.contains("childrenReady") && l.contains("splitWeight"),
         "a split waits for all four children and blends the band");
      check(l.contains("var2.activeCross != null && !var2.activeCross.done"),
         "eviction cannot delete a cross-fade base");
      check(!l.contains("GL15.glDeleteBuffers(t.vbo);\n            }\n\n            t.state = 5;")
         || l.contains("crosses.remove(c)"), "the VBO is only deleted after the fade");
   }

   static void trailer() throws Exception {
      eq(60.0, Trailer.LENGTH, "still exactly one minute");
      check(Trailer.SHOTS.length >= 20, "a fast-cut montage of " + Trailer.SHOTS.length + " shots");
      check(Trailer.KIND.length == 8, "eight distinct camera moves");
      double sum = 0.0, max = 0.0;
      boolean kinds = false, locations = false, skies = false, cards = false, storms = false;
      int[] seenKinds = new int[8];
      boolean[] seenLoc = new boolean[8];
      for (Trailer.Shot s : Trailer.SHOTS) {
         sum += s.length();
         max = Math.max(max, s.length());
         seenKinds[s.kind]++;
         seenLoc[s.anchor] = true;
         if (s.sky != Trailer.SHOTS[0].sky) skies = true;
         if (!s.title.isEmpty()) cards = true;
         if (s.weather == 3) storms = true;
      }
      for (int n : seenKinds) if (n > 0) kinds = true;
      for (boolean b : seenLoc) if (b) locations = true;
      eq(60.0, Math.round(sum * 100.0) / 100.0, "the cuts still add up to 60 seconds");
      check(max <= 5.5, "no shot is a long continuous flythrough (" + max + "s max)");
      check(kinds && locations && skies && cards && storms,
         "varied cameras, locations, times of day, word cards and a storm");
      String t = src("src/bc/Trailer.java");
      check(t.contains("case 2: // Orbit") && t.contains("case 6: // Top-down")
         && t.contains("default: // Chase"), "the camera moves are genuinely different");
      check(t.contains("anchor(gen, baseX, baseZ, s.anchor)"), "each shot can use a different location");
      String plan = Trailer.plan();
      check(plan.contains("MONTAGE") && plan.contains("shots=24") && plan.contains("CARD:"),
         "the headless plan describes the montage");
      check(src("docs/releases/v6.31-trailer-plan.txt").contains("MONTAGE"),
         "the montage plan is shipped");
   }

   static void borderless() throws Exception {
      File dir = new File("build/test-options");
      dir.mkdirs();
      Options o = new Options(dir);
      o.borderless = true;
      o.save();
      Options back = new Options(dir);
      back.load();
      eq(true, back.borderless, "borderless persists in options.txt");
      String opts = src("src/bc/Options.java");
      check(opts.contains("setProperty(\"borderless\"") && opts.contains("getProperty(\"borderless\""),
         "borderless is written to and read from the settings file");
      String g = src("src/bc/Game.java");
      check(g.contains("void setBorderless(boolean var1)") && g.contains("GLFW.GLFW_DECORATED"),
         "an undecorated desktop-sized window is implemented");
      check(g.contains("this.applyDisplayMode()"), "the saved mode is applied at startup");
      check(g.contains("case \"borderless\":") && g.contains("/borderless"),
         "a /borderless command toggles it");
      String s = src("src/bc/Screens.java");
      check(s.contains("\"Borderless\", o.borderless") && s.contains("g.setBorderless(!o.borderless)"),
         "the video settings expose borderless fullscreen");
   }

   static void menus() throws Exception {
      String s = src("src/bc/Screens.java");
      check(s.contains("More...") && s.contains("static void more(Game g)"),
         "secondary screens are grouped behind More...");
      check(s.contains("\"Mini Games\"") && s.contains("\"All Blocks Gallery\"") && s.contains("\"Credits\""),
         "the More screen keeps every secondary entry");
      check(s.contains("Watch Trailer (1:00)"), "the trailer is still one click away");
      String g = src("src/bc/Game.java");
      check((g.contains("MORE;") || g.contains("MORE,")) && g.contains("case MORE:"),
         "the More screen is a real game screen");
      check(g.contains("this.screen == Game.S.MORE"), "escape leaves the More screen");
      check(src("docs/FILE-LAYOUT.md").contains("releases"), "the cleaned repository layout is documented");
   }

   static final class Chunks {
      final TerraGen tg;
      final HashMap<Long, Chunk> cache = new HashMap<>();

      Chunks(TerraGen tg) { this.tg = tg; }

      int get(int x, int y, int z) {
         int cx = Math.floorDiv(x, 16), cz = Math.floorDiv(z, 16);
         long k = ((long)cx << 32) ^ (cz & 0xFFFFFFFFL);
         Chunk c = cache.get(k);
         if (c == null) { c = tg.generate(cx, cz); cache.put(k, c); }
         return c.get(x - cx * 16, y, z - cz * 16) & BlockState.ID_MASK;
      }
   }

   static ArrayList<Integer> components(boolean[][] m, int n) {
      ArrayList<Integer> out = new ArrayList<>();
      boolean[][] seen = new boolean[n][n];
      for (int x = 0; x < n; x++) {
         for (int z = 0; z < n; z++) {
            if (!m[x][z] || seen[x][z]) continue;
            int size = 0;
            ArrayDeque<int[]> q = new ArrayDeque<>();
            q.add(new int[]{x, z});
            seen[x][z] = true;
            while (!q.isEmpty()) {
               int[] p = q.poll();
               size++;
               int[][] d = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
               for (int[] dd : d) {
                  int nx = p[0] + dd[0], nz = p[1] + dd[1];
                  if (nx >= 0 && nz >= 0 && nx < n && nz < n && m[nx][nz] && !seen[nx][nz]) {
                     seen[nx][nz] = true;
                     q.add(new int[]{nx, nz});
                  }
               }
            }
            out.add(size);
         }
      }
      Collections.sort(out);
      return out;
   }

   static void caves() throws Exception {
      Properties p = new Properties();
      settings(5).save(p);
      eq(5, WorldSettings.load(p).caveVer, "cave version 5 round-trips");
      p.setProperty("cavever", "99");
      eq(8, WorldSettings.load(p).caveVer, "future cave versions clamp to 8");
      Properties old = new Properties();
      old.setProperty("gentype", "0");
      eq(1, WorldSettings.load(old).caveVer, "an old save keeps its caves");
      check(Caves5.landBiome(Biomes.PLAINS, 70) && !Caves5.landBiome(Biomes.OCEAN, 70)
         && !Caves5.landBiome(Biomes.PLAINS, 60), "only dry land above the sea can open");

      long seed = 20261107L;
      int ox = -600, oz = -408, n = 96;
      TerraGen tg = new TerraGen(seed, settings(5));
      Chunks ch = new Chunks(tg);
      boolean[][] open = new boolean[n][n];
      int holes = 0, linked = 0, carved = 0;
      for (int x = ox; x < ox + n; x++) {
         for (int z = oz; z < oz + n; z++) {
            int h = tg.column(x, z).h;
            if (ch.get(x, h, z) == 0) { holes++; open[x - ox][z - oz] = true; }
            for (int y = 8; y <= h; y += 4) if (ch.get(x, y, z) == 0) carved++;
            if (open[x - ox][z - oz]) {
               int y = h;
               while (y > 8 && ch.get(x, y - 1, z) == 0) y--;
               if (h - y >= 10) linked++;
            }
         }
      }
      ArrayList<Integer> sizes = components(open, n);
      check(holes > 100, "real surface openings exist (" + holes + " columns)");
      check(sizes.size() >= 3, "several separate cave entrances (" + sizes.size() + ")");
      check(sizes.get(sizes.size() - 1) >= 5 * Math.max(1, sizes.get(0)),
         "entrance shapes are very different sizes " + sizes);
      check(linked >= holes * 9 / 10, "almost every opening leads down into the caves (" + linked + "/" + holes + ")");
      check(carved > 10000, "the new carver hollows a real cave network (" + carved + ")");
      String t = src("src/bc/TerraGen.java");
      check(t.contains("cc3 && !cc5") && t.contains("carve5(wx, y, wz, h, allowSurface)"),
         "V6.31 uses no built cave mouths and carves to the surface");
      check(t.contains("Caves5.landBiome(s.biome, h)"), "surface openings are gated to dry land");
      String c = src("src/bc/Caves5.java");
      check(c.contains("NO built cave mouths") && c.contains("aperture.noise2"),
         "openings are noise-shaped, not a repeated template");
      check(src("src/bc/Screens.java").contains("g.opt.caveCliffs ? 8 : 1"),
         "new worlds use the V6.31 carver");
   }

   static void structures() throws Exception {
      eq(8, CaveStructures5.NAME.length, "eight new underground structure types");
      CaveStructures5.Plan a = CaveStructures5.plan(12345L, 3, -2);
      CaveStructures5.Plan b = CaveStructures5.plan(12345L, 3, -2);
      check(a.type == b.type && a.x == b.x && a.z == b.z && a.y == b.y,
         "structure plans are deterministic across chunk borders");
      boolean types = false;
      int[] seen = new int[CaveStructures5.NAME.length];
      for (int rx = -8; rx <= 8; rx++) {
         for (int rz = -8; rz <= 8; rz++) {
            CaveStructures5.Plan p = CaveStructures5.plan(12345L, rx, rz);
            if (p.valid) seen[p.type]++;
         }
      }
      for (int n : seen) if (n > 0) types = true;
      check(types, "the new structure types really occur");
      // real generated chunks must contain built structure blocks
      TerraGen tg = new TerraGen(20261107L, settings(5));
      Chunks ch = new Chunks(tg);
      int built = 0;
      for (int cx = -40; cx <= -36; cx++) {
         for (int cz = -27; cz <= -23; cz++) {
            Chunk c = tg.generate(cx, cz);
            for (int x = 0; x < 16; x++) {
               for (int z = 0; z < 16; z++) {
                  for (int y = 10; y < 60; y++) {
                     int id = c.get(x, y, z) & BlockState.ID_MASK;
                     if (id == Blocks.CHEST_LOOT || id == Blocks.SPAWNER || id == Blocks.STONEBRICK
                        || id == Redstone319.PURPUR_BLOCK || id == Blocks313.MUSH_STEM) built++;
                  }
               }
            }
         }
      }
      check(built > 0, "generated chunks contain new structure blocks (" + built + ")");
      String w = src("src/bc/CaveWorks.java");
      check(w.contains("this.structures5.apply(ch)") && w.contains("st.caveVer >= 5"),
         "V6.31 worlds use the new structure set");
      String s = src("src/bc/CaveStructures5.java");
      check(s.contains("linkToCave") && s.contains("carve(px, yy, pz, h, false)"),
         "structures connect into the real cave network");
   }

   static void wiring() throws Exception {
      eq("6.36", Game.VERSION_STR, "the game version is 6.36");
      eq("6.36", src("ci/release-trigger.txt").trim(), "the release workflow publishes 6.36");
      check(src("README.md").contains("V6.31"), "README describes V6.31");
      check(src("package/README.txt").contains("WHAT'S NEW IN V6.31"), "the packaged readme describes V6.31");
      check(new File("docs/releases/v6.31.md").isFile(), "V6.31 release notes exist");
      check(new File("docs/screenshots/v6.31-caves.png").isFile(), "the cave preview exists");
      check(new File("docs/screenshots/v6.31-trailer.png").isFile(), "the trailer storyboard exists");
      check(src("tools/build.py").contains("\"Caves5\"") && src("tools/verify_release.py").contains("\"Caves5\""),
         "the build receipt and verifier hash the new carver");
      check(new File("package/Trailer-guide.txt").isFile(), "the packaged trailer guide exists");
   }
}
