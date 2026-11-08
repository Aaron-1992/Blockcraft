package bc;

import static bc.GameTests.check;
import static bc.GameTests.eq;
import static bc.GameTests.test;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Properties;

/** V6.29: normal walk-in cave entrances and real recorded CC0 foley instead of generated tones. */
final class Tests629 {
   private Tests629() {
   }

   static void run() {
      test("6.29: new worlds use walk-in cave entrances and old saves keep their version", Tests629::versions);
      test("6.29: an entrance is a wide bowl with a solid sloped floor, not a diagonal crack", Tests629::geometry);
      test("6.29: every entrance joins a real carved cave", Tests629::links);
      test("6.29: world foley is real recorded WAV audio, not procedural synthesis", Tests629::sounds);
      test("6.29: version, release wiring and packaged credits point at 6.29", Tests629::wiring);
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

   static void versions() throws Exception {
      Properties p = new Properties();
      settings(4).save(p);
      eq(4, WorldSettings.load(p).caveVer, "cave version 4 round-trips");
      p.setProperty("cavever", "99");
      eq(8, WorldSettings.load(p).caveVer, "future cave versions clamp to 8");
      Properties v628 = new Properties();
      v628.setProperty("gentype", "1");
      v628.setProperty("cavever", "3");
      eq(3, WorldSettings.load(v628).caveVer, "a V6.28 world keeps its old caves");
      Properties legacy = new Properties();
      legacy.setProperty("gentype", "0");
      eq(1, WorldSettings.load(legacy).caveVer, "an old save keeps the V6.21 caves");
      String s = src("src/bc/Screens.java");
      check(s.contains("g.opt.caveCliffs ? 8 : 1"), "Create New World selects the walk-in carver");
      check(s.contains("break through the ground naturally"), "the option describes the new entrances");
      String cg = src("src/bc/CaveCliffs.java");
      check(cg.contains("MOUTH4_CELL = 64") && cg.contains("int ver = 3"),
         "V6.28 mouths are preserved behind a version flag");
   }

   /** Collect distinct v4 mouths using the real generator (with its terrain sampler). */
   static LinkedHashMap<String, CaveCliffs.Mouth> mouths(TerraGen tg, int radius, int step) {
      LinkedHashMap<String, CaveCliffs.Mouth> out = new LinkedHashMap<>();
      for (int x = -radius; x <= radius; x += step) {
         for (int z = -radius; z <= radius; z += step) {
            CaveCliffs.Mouth[] ms = tg.caves.mouths(x, z);
            if (ms == null) continue;
            for (CaveCliffs.Mouth m : ms) out.put(m.x + "," + m.z, m);
         }
      }
      return out;
   }

   static int[] axis(CaveCliffs.Mouth m, double along, double side) {
      double ox = m.dirX * m.radius * 0.25, oz = m.dirZ * m.radius * 0.25;
      return new int[]{
         (int)Math.round(m.x + ox + m.dirX * along - m.dirZ * side),
         (int)Math.round(m.z + oz + m.dirZ * along + m.dirX * side)
      };
   }

   static final class Chunks {
      final TerraGen tg;
      final HashMap<Long, Chunk> cache = new HashMap<>();

      Chunks(TerraGen tg) {
         this.tg = tg;
      }

      int get(int x, int y, int z) {
         int cx = Math.floorDiv(x, 16), cz = Math.floorDiv(z, 16);
         long k = ((long)cx << 32) ^ (cz & 0xFFFFFFFFL);
         Chunk c = cache.get(k);
         if (c == null) {
            c = tg.generate(cx, cz);
            cache.put(k, c);
         }
         return c.get(x - cx * 16, y, z - cz * 16) & BlockState.ID_MASK;
      }
   }

   static void geometry() throws Exception {
      long seed = 20261107L;
      TerraGen tg = new TerraGen(seed, settings(4));
      LinkedHashMap<String, CaveCliffs.Mouth> found = mouths(tg, 320, 8);
      check(found.size() >= 20, "hashed walk-in entrances really exist (" + found.size() + ")");
      CaveCliffs.Mouth pick = null;
      int near = 0;
      for (CaveCliffs.Mouth m : found.values()) {
         if (m.x * m.x + m.z * m.z < 200 * 200) near++;
         if (pick == null || m.siteH > pick.siteH) pick = m;
      }
      check(near >= 3, "several entrances are within a short walk of spawn (" + near + ")");
      CaveCliffs.Mouth m = pick;
      check(m.ver >= 4 && m.radius >= 6 && m.siteH >= 64, "the mouth is a V6.29 dry-land site");
      check(m.surfaceHole(m.x, m.z), "the centre of the mouth is open to the sky");

      // A solid, monotonically descending walk-in floor (not a 45-block diagonal crack).
      double rx = m.radius * 1.05;
      int last = Integer.MAX_VALUE;
      boolean ramp = true;
      for (int k = 0; k <= 10; k++) {
         int[] p = axis(m, (-0.75 + 1.50 * k / 10.0) * rx, 0);
         int fy = m.floorAt(p[0], p[1]);
         if (fy < 0 || fy > last) { ramp = false; break; }
         last = fy;
      }
      check(ramp, "the opening has a continuous sloped solid floor");
      check(m.floorAt(m.x, m.z) > m.chamberY, "the ramp ends above the chamber floor");

      // The generator really opens the surface and keeps the ramp solid.
      Chunks ch = new Chunks(tg);
      int open = 0, minA = 999, maxA = -999, minB = 999, maxB = -999;
      for (int dx = -18; dx <= 18; dx++) {
         for (int dz = -18; dz <= 18; dz++) {
            int wx = m.x + dx, wz = m.z + dz;
            int h = tg.column(wx, wz).h;
            if (ch.get(wx, h, wz) == 0) {
               open++;
               int A = (int)Math.round(dx * m.dirX + dz * m.dirZ);
               int B = (int)Math.round(-dx * m.dirZ + dz * m.dirX);
               minA = Math.min(minA, A); maxA = Math.max(maxA, A);
               minB = Math.min(minB, B); maxB = Math.max(maxB, B);
            }
         }
      }
      check(open >= 60, "a wide visible hole is actually carved (" + open + " columns)");
      check(maxA - minA >= 8 && maxB - minB >= 8, "the opening is wide in both directions");
      int[] c = axis(m, 0.0, 0.0);
      int cf = m.floorAt(c[0], c[1]);
      check(ch.get(c[0], cf, c[1]) != 0, "noise caves cannot hollow out the walk-in floor");
      check(ch.get(c[0], cf + 1, c[1]) == 0, "the space above the ramp is open");

      // No long diagonal crack just below the surface 45 blocks away.
      int[] far = axis(m, 45.0, 0.0);
      check(!m.air(far[0], m.siteH - 3, far[1], tg.column(far[0], far[1]).h),
         "no endless diagonal crack leaves the mouth near the surface");
   }

   static void links() throws Exception {
      long seed = 20261107L;
      TerraGen tg = new TerraGen(seed, settings(4));
      LinkedHashMap<String, CaveCliffs.Mouth> found = mouths(tg, 320, 16);
      int linked = 0;
      for (CaveCliffs.Mouth m : found.values()) {
         int h = tg.column(m.linkX, m.linkZ).h;
         if (m.linkFound
            && m.air(m.linkX, m.linkY, m.linkZ, h)
            && tg.caves.cliffs.carve(m.linkX, m.linkY, m.linkZ) != CaveCliffs.SOLID
            && m.wellBottom < m.chamberY) linked++;
      }
      check(found.size() >= 10, "a real sample of entrances was checked (" + found.size() + ")");
      eq(found.size(), linked, "every entrance tunnels into a real carved cave");
      String t = src("src/bc/TerraGen.java");
      check(t.contains("mm.floorAt(wx, wz)"), "the generator protects the ramp floor");
      check(t.contains("ver3 && !cc4 && !cc5 && !cc6 && !cc7 && !cc8 && CaveWorks.shaft"), "old sinkhole shafts are off for new worlds");
   }

   static void sounds() throws Exception {
      String[] keys = {"tree_fall", "tree_crash", "tree_creak", "leaf_rustle", "wind_gust",
         "cave_drip", "cave_wind", "cave_rumble", "geode_chime", "stone_crumble"};
      eq(10, SoundEngine.recordedFoleyCount(), "all ten recorded WAVs were loaded");
      for (String key : keys) {
         check(SoundEngine.isRecordedFoley(key), key + " is backed by a real recording");
         SoundEngine.Sample s = SoundEngine.getSample(key);
         check(s != null && s.pcm.length > 1000 && s.sampleRate == 22050,
            key + " is a decoded 22 kHz PCM recording");
         int peak = 0;
         for (short v : s.pcm) peak = Math.max(peak, Math.abs((int)v));
         check(peak > 4000, key + " is not silent");
      }
      check(!SoundEngine.isRecordedFoley("thunder"), "unrelated synthesized fallbacks are unchanged");
      String s = src("src/bc/SoundEngine.java");
      check(!s.contains("V6.28 procedural world foley"), "the generated V6.28 tones were removed");
      check(s.contains("loadRecordedFoley()") && s.contains("private static Sample decodeWav"),
         "a WAV decoder installs the real recordings");
      check(s.contains("No Mojang") || s.contains("no Mojang"), "no Mojang audio is used");
      String credits = src("res/sounds/foley/CREDITS.txt");
      check(credits.contains("CC0 1.0") && credits.contains("matt_beer") && credits.contains("Anya_Media"),
         "every recording has documented CC0 provenance");
      for (String key : keys) {
         check(new File("res/sounds/foley/" + key + ".wav").isFile(), key + ".wav is shipped");
      }
      String g = src("src/bc/Game.java");
      check(g.contains("\"wind_gust\""), "cave ambience also plays real wind gusts");
      check(g.contains("case \"sounds\":") && g.contains("/sounds"), "a /sounds command reports and auditions the recordings");
      String b = src("tools/build.py");
      check(b.contains("def foley_files()"), "the release ships loose editable WAVs");
   }

   static void wiring() throws Exception {
      eq("6.36", Game.VERSION_STR, "the game version is 6.36");
      eq("6.36", src("ci/release-trigger.txt").trim(), "the release workflow publishes 6.36");
      check(src("README.md").contains("V6.29"), "README describes V6.29");
      check(src("package/README.txt").contains("WHAT'S NEW IN V6.29"), "the packaged readme describes V6.29");
      check(new File("docs/releases/v6.29.md").isFile(), "V6.29 release notes exist");
      check(new File("docs/screenshots/v6.29-caves.png").isFile(), "the entrance cross-section exists");
   }
}
