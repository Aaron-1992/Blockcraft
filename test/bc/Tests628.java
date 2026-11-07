package bc;

import static bc.GameTests.check;
import static bc.GameTests.eq;
import static bc.GameTests.test;

import java.io.File;
import java.nio.file.Files;
import java.util.Properties;

/** V6.28: 1.20-style caves with real entrances, plus new procedural foley sounds. */
final class Tests628 {
   private Tests628() {
   }

   static void run() {
      test("6.28: new worlds use the 1.20 cave carver and old saves stay unchanged", Tests628::versions);
      test("6.28: cave entrances connect the surface to the underground", Tests628::entrances);
      test("6.28: the 1.20 caverns are larger and better connected", Tests628::caverns);
      test("6.28: new tree and cave sounds exist and are wired into gameplay", Tests628::sounds);
      test("6.28: version, notes and download links point at 6.28", Tests628::wiring);
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
      settings(3).save(p);
      eq(3, WorldSettings.load(p).caveVer, "cave version 3 round-trips");
      p.setProperty("cavever", "99");
      eq(3, WorldSettings.load(p).caveVer, "future cave versions clamp to 3");
      Properties legacy = new Properties();
      legacy.setProperty("gentype", "0");
      eq(1, WorldSettings.load(legacy).caveVer, "an old save keeps the V6.21 caves");
      String s = src("src/bc/Screens.java");
      check(s.contains("g.opt.caveCliffs ? 3 : 1"), "Create New World selects the 1.20 carver");
      check(s.contains("real sloped cave entrances"), "the option describes the new entrances");
   }

   static void entrances() throws Exception {
      long seed = 20261107L;
      CaveCliffs cc = new CaveCliffs(seed, settings(3));
      TerraGen tg = new TerraGen(seed, settings(3));
      int sites = 0, linked = 0;
      for (int wx = -600; wx < 600; wx += 19) {
         for (int wz = -600; wz < 600; wz += 19) {
            CaveCliffs.Mouth[] ms = cc.mouths(wx, wz);
            if (ms == null) continue;
            for (CaveCliffs.Mouth m : ms) {
               if (!m.surfaceHole(wx, wz)) continue;
               Gen.Col c = tg.column(wx, wz);
               if (c.h < 66 || c.h > 120) continue;
               sites++;
               int bottom = Math.max(26, c.h - 46);
               int top = c.h - 3;
               double drift = (top - bottom) * 1.05;
               boolean ok = true;
               for (int k = 0; k <= 10; k++) {
                  int y = top - (top - bottom) * k / 10;
                  double t = (double)k / 10.0;
                  int sx = (int)Math.round(m.x + m.dirX * t * drift);
                  int sz = (int)Math.round(m.z + m.dirZ * t * drift);
                  if (!m.air(sx, y, sz, c.h)) { ok = false; break; }
               }
               if (ok) {
                  int gx = (int)Math.round(m.x + m.dirX * (drift + m.radius * 3.0));
                  int gz = (int)Math.round(m.z + m.dirZ * (drift + m.radius * 3.0));
                  ok = m.air(gx, bottom, gz, c.h);
               }
               if (ok) linked++;
               break;
            }
         }
      }
      check(sites >= 10, "hashed cave entrances really exist (" + sites + " sampled)");
      eq(sites, linked, "every entrance is continuous surface -> ramp -> chamber -> gallery");
      String t = src("src/bc/TerraGen.java");
      check(t.contains("m.air(wx, y, wz, h)"), "the generator carves the entrances");
      check(t.contains("m.rim(wx, wz)"), "entrances get a stone lip");
      check(t.contains("mouthSurface(wx, wz, s.h)"), "trees are kept out of the openings");
   }

   static void caverns() throws Exception {
      long seed = 1234L;
      CaveCliffs v2 = new CaveCliffs(seed, settings(2));
      CaveCliffs v3 = new CaveCliffs(seed, settings(3));
      int air2 = 0, air3 = 0, water3 = 0;
      for (int x = -160; x < 160; x += 7) {
         for (int z = -160; z < 160; z += 7) {
            for (int y = 8; y <= 76; y += 4) {
               int a = v2.carve(x, y, z);
               int b = v3.carve(x, y, z);
               if (a == CaveCliffs.AIR || a == CaveCliffs.WATER) air2++;
               if (b == CaveCliffs.AIR || b == CaveCliffs.WATER) air3++;
               if (b == CaveCliffs.WATER) water3++;
            }
         }
      }
      check(air3 > air2 * 125 / 100, "1.20 caverns are substantially larger (" + air2 + " -> " + air3 + ")");
      check(water3 > 0, "local aquifers still flood the lower caverns");
   }

   static void sounds() throws Exception {
      for (String key : new String[]{"tree_fall", "tree_crash", "leaf_rustle", "cave_drip",
         "cave_wind", "cave_rumble", "geode_chime", "stone_crumble", "tree_creak", "water_drip"}) {
         check(SoundEngine.hasSample(key), "sound sample " + key + " exists");
         SoundEngine.Sample smp = SoundEngine.getSample(key);
         int peak = 0;
         for (short v : smp.pcm) peak = Math.max(peak, Math.abs((int)v));
         check(peak > 1000, key + " is not silent");
      }
      String s = src("src/bc/SoundEngine.java");
      check(s.contains("all synthesized locally") || s.contains("no Mojang"), "new audio is original synthesis");
      check(s.contains("tree_fall\") || key.equals(\"tree_crash\""), "falling trees are audible from further away");
      check(s.contains("\"leaf_rustle\""), "breaking leaves rustles");
      String t = src("src/bc/Trees.java");
      check(t.contains("\"tree_fall\"") && t.contains("\"tree_crash\""), "tree falling and landing play sounds");
      String g = src("src/bc/Game.java");
      check(g.contains("void caveAmbience()") && g.contains("this.caveAmbience()"), "cave ambience runs every tick");
   }

   static void wiring() throws Exception {
      eq("6.28", Game.VERSION_STR, "the game version is 6.28");
      eq("6.28", src("ci/release-trigger.txt").trim(), "the release workflow publishes 6.28");
      check(src("README.md").contains("V6.28"), "README describes V6.28");
      check(src("package/README.txt").contains("V6.28"), "the packaged readme describes V6.28");
      check(new File("docs/releases/v6.28.md").isFile(), "V6.28 release notes exist");
      check(new File("docs/screenshots/v6.28-caves.png").isFile(), "the cave cross-section preview exists");
   }
}
