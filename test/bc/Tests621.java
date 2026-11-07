package bc;

import static bc.GameTests.check;
import static bc.GameTests.eq;
import static bc.GameTests.test;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Update 6.21 regressions: the complete cave redo (layered, calmer caves with real underground
 * structures and "stuff"), the cave/weather settings, and the prettier weather near and far.
 * All headless: no OpenGL, no display, no GPU.
 */
final class Tests621 {
   private Tests621() {
   }

   static File tmp;

   static void run() {
      try {
         tmp = Files.createTempDirectory("bc621").toFile();
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
      test("6.21: cave and weather options persist and are clamped", Tests621::options);
      test("6.21: the Advanced tab exposes every new cave and weather control", Tests621::advancedTab);
      test("6.21: new-world settings round-trip, old saves stay classic", Tests621::worldSettings);
      test("6.21: the new cave field is calmer, layered and coordinate-pure", Tests621::caveField);
      test("6.21: underground sites follow the real surface and strata are horizontal", Tests621::surfaceSites);
      test("6.21: cave structures are deterministic and really build", Tests621::caveStructures);
      test("6.21: generated V6.21 chunks contain caves, strata and structures", Tests621::generatedWorld);
      test("6.21: distant weather colours, wind, mist, rainbows and haze", Tests621::weatherHelpers);
      test("6.21: weather detail levels build different curtain and mist meshes", Tests621::weatherMesh);
      test("6.21: the cloud shader gained beauty controls and the version is 6.21", Tests621::shaderAndVersion);
   }

   // ------------------------------------------------------------------ helpers
   static Game screen(int gw, int gh) {
      GuiRaster r = new GuiRaster(gw * 3, gh * 3);
      Game g = new Game();
      g.gui = r;
      g.opt = new Options(new File(tmp, "screens"));
      g.fbW = gw * 3;
      g.fbH = gh * 3;
      g.winW = gw * 3;
      g.winH = gh * 3;
      r.begin(gw * 3, gh * 3, 3);
      g.activeBtnsSwap();
      g.mouseX = -1000;
      g.mouseY = -1000;
      return g;
   }

   static boolean button(Game g, String starts) {
      for (Game.Btn b : g.btns) {
         if (b.label != null && b.label.startsWith(starts)) return true;
      }
      return false;
   }

   static Game.Btn find(Game g, String starts) {
      for (Game.Btn b : g.btns) {
         if (b.label != null && b.label.startsWith(starts)) return b;
      }
      return null;
   }

   static Chunk solidChunk(int cx, int cz) {
      Chunk c = new Chunk(cx, cz);
      for (int z = 0; z < 16; z++) {
         for (int x = 0; x < 16; x++) {
            for (int y = 0; y <= 4; y++) c.set(x, y, z, Blocks.BEDROCK);
            for (int y = 5; y <= 70; y++) c.set(x, y, z, Blocks.STONE);
         }
      }
      return c;
   }

   static int count(Chunk c, int id) {
      int n = 0;
      for (short s : c.blocks) {
         if ((s & BlockState.ID_MASK) == id) n++;
      }
      return n;
   }

   static boolean has(Chunk c, int id) {
      for (short s : c.blocks) {
         if ((s & BlockState.ID_MASK) == id) return true;
      }
      return false;
   }

   static WorldSettings v3() {
      WorldSettings st = new WorldSettings();
      st.terrainVer = 3;
      st.caveScale = 1.6F;
      st.caveDensity = 1.3F;
      st.mountainScale = 1.5F;
      st.ravineScale = 1.3F;
      return st;
   }

   // ------------------------------------------------------------------ settings
   static void options() throws Exception {
      File dir = new File(tmp, "opt");
      dir.mkdirs();
      Options f = new Options(dir);
      eq(true, f.caveStructures, "cave structures on by default");
      eq(true, f.caveLayers, "cave strata on by default");
      eq(true, f.caveLakes, "cave lakes on by default");
      eq(1.0F, f.caveRichness, "normal cave richness by default");
      eq(2, f.weatherQuality, "detailed weather by default");
      eq(true, f.weatherRainbow, "rainbows on by default");
      eq(true, f.weatherMist, "ground mist on by default");
      eq(true, f.weatherBoltGlow, "lightning glow on by default");
      eq(1.0F, f.weatherWind, "normal wind by default");
      eq(1.0F, f.weatherReach, "normal weather distance by default");

      f.caveStructures = false;
      f.caveLayers = false;
      f.caveLakes = false;
      f.caveRichness = 1.7F;
      f.weatherQuality = 0;
      f.weatherRainbow = false;
      f.weatherMist = false;
      f.weatherBoltGlow = false;
      f.weatherWind = 0.5F;
      f.weatherReach = 1.4F;
      f.save();
      Options b = new Options(dir);
      b.load();
      eq(false, b.caveStructures, "cave structures persisted");
      eq(false, b.caveLayers, "cave strata persisted");
      eq(false, b.caveLakes, "cave lakes persisted");
      eq(1.7F, b.caveRichness, "cave richness persisted");
      eq(0, b.weatherQuality, "weather quality persisted");
      eq(false, b.weatherRainbow, "rainbows persisted");
      eq(false, b.weatherMist, "ground mist persisted");
      eq(false, b.weatherBoltGlow, "lightning glow persisted");
      eq(0.5F, b.weatherWind, "wind strength persisted");
      eq(1.4F, b.weatherReach, "weather distance persisted");

      // a hand-edited options.txt cannot break the sliders
      File dir2 = new File(tmp, "clamp");
      dir2.mkdirs();
      Files.write(new File(dir2, "options.txt").toPath(),
         ("caveRichness=-9\nweatherQuality=77\nweatherWind=99\nweatherReach=-4\n").getBytes("UTF-8"));
      Options c = new Options(dir2);
      c.load();
      eq(0.4F, c.caveRichness, "cave richness is clamped");
      eq(2, c.weatherQuality, "weather quality is clamped");
      eq(2.0F, c.weatherWind, "wind is clamped");
      eq(0.5F, c.weatherReach, "weather distance is clamped");
   }

   static void advancedTab() throws Exception {
      Game g = screen(343, 1800); // tall enough for every one of the 57 scrolling rows
      g.opt.debugVideo = true;
      g.ui.optTab = 4;
      Screens.options(g);
      eq(5, Screens.tabs(g.opt).length, "the debug switch still shows five tabs");
      eq(61, Screens.ADV_ROWS, "the Advanced tab knows every row it has");
      check(button(g, "V6.21 Caves"), "the layered cave generator is configurable");
      check(button(g, "Cave Richness"), "cave richness is configurable");
      check(button(g, "Cave Structures"), "cave structures are configurable");
      check(button(g, "Cave Strata"), "cave strata are configurable");
      check(button(g, "Cave Lakes"), "cave lakes are configurable");
      check(button(g, "Weather Detail"), "weather detail is configurable");
      check(button(g, "Weather Distance"), "weather distance is configurable");
      check(button(g, "Wind Strength"), "wind strength is configurable");
      check(button(g, "Ground Mist"), "ground mist is configurable");
      check(button(g, "Rainbows"), "rainbows are configurable");
      check(button(g, "Lightning Glow"), "lightning glow is configurable");
      check(button(g, "Minimap"), "the V6.20 map settings are still here");

      Game.Btn wd = find(g, "Weather Detail");
      check(wd != null && wd.slider != null, "weather detail is a slider");
      if (wd != null && wd.slider != null) {
         wd.slider.accept(0.0F);
         eq(0, g.opt.weatherQuality, "the weather detail slider changes the option");
      }
      Game.Btn cs = find(g, "Cave Structures");
      check(cs != null && cs.action != null, "cave structures is a toggle");
      if (cs != null && cs.action != null) {
         cs.action.run();
         eq(false, g.opt.caveStructures, "the cave structures toggle changes the option");
      }

      Screens.resetAdvanced(g.opt);
      eq(true, g.opt.caveStructures, "reset restores cave structures");
      eq(1.0F, g.opt.caveRichness, "reset restores cave richness");
      eq(2, g.opt.weatherQuality, "reset restores weather quality");
      eq(true, g.opt.weatherRainbow, "reset restores rainbows");
      eq(1.0F, g.opt.weatherWind, "reset restores wind");

      Screens.applyNewWorldSettings(g);
      eq(3, g.newSettings.terrainVer, "a new world gets the V6.21 caves");
      eq(1.6F, g.newSettings.caveScale, "cave size is copied");
      eq(true, g.newSettings.caveStructures, "cave structures are copied");
      eq(true, g.newSettings.caveLayers, "cave strata are copied");
      eq(true, g.newSettings.caveLakes, "cave lakes are copied");
      eq(1.0F, g.newSettings.caveRichness, "cave richness is copied");
      g.opt.newTerrain = false;
      Screens.applyNewWorldSettings(g);
      eq(1, g.newSettings.terrainVer, "turning the switch off keeps the classic generator");
   }

   static void worldSettings() {
      WorldSettings s = v3();
      s.caveStructures = false;
      s.caveLayers = false;
      s.caveLakes = false;
      s.caveRichness = 1.8F;
      Properties p = new Properties();
      s.save(p);
      WorldSettings b = WorldSettings.load(p);
      eq(3, b.terrainVer, "the V6.21 terrain version round-trips");
      eq(false, b.caveStructures, "cave structures round-trip");
      eq(false, b.caveLayers, "cave strata round-trip");
      eq(false, b.caveLakes, "cave lakes round-trip");
      eq(1.8F, b.caveRichness, "cave richness round-trips");
      eq(1.6F, b.caveScale, "the older cave sliders still round-trip");

      Properties old = new Properties();
      old.setProperty("gentype", "1");
      WorldSettings legacy = WorldSettings.load(old);
      eq(1, legacy.terrainVer, "an old save has no new terrain");
      eq(true, legacy.caveStructures, "an old save gets the new defaults without changing its generator");
      eq(1.0F, legacy.caveRichness, "an old save gets normal cave richness");

      Properties hi = new Properties();
      hi.setProperty("gentype", "1");
      hi.setProperty("terrainver", "99");
      eq(3, WorldSettings.load(hi).terrainVer, "a future terrain version is clamped to 3");
      hi.setProperty("terrainver", "-7");
      eq(1, WorldSettings.load(hi).terrainVer, "a negative terrain version is clamped to 1");

      WorldSettings copy = s.copy();
      eq(3, copy.terrainVer, "copy keeps the terrain version");
      eq(1.8F, copy.caveRichness, "copy keeps cave richness");
      eq(false, copy.caveLayers, "copy keeps cave strata");

      WorldPreview pv = new WorldPreview(48, 30, 20, true);
      WorldSettings w1 = v3();
      WorldSettings w2 = v3();
      w2.caveRichness = 0.6F;
      w2.caveLakes = false;
      check(pv.update(1234L, w1), "the first preview is drawn");
      check(pv.update(1234L, w2), "a new cave-setting change redraws the preview");
      check(!pv.update(1234L, w2), "an unchanged key does not redraw");
   }

   // ------------------------------------------------------------------ the cave field
   static void caveField() {
      long seed = 6120L;
      WorldSettings oldSt = new WorldSettings();
      oldSt.terrainVer = 2;
      oldSt.caveScale = 1.6F;
      oldSt.caveDensity = 1.3F;
      TerraGen tg = new TerraGen(seed, oldSt);
      int oldAir = 0;
      int newAir = 0;
      int total = 0;
      for (int x = 0; x < 96; x += 3) {
         for (int z = 0; z < 96; z += 3) {
            for (int y = 8; y < 54; y += 2) {
               total++;
               if (tg.carveBig(x, y, z)) oldAir++;
               if (CaveWorks.hollow(tg.nCave1, tg.nCave2, tg.nCavern, seed, x, y, z, 1.6F, 1.3F) != 0) newAir++;
            }
         }
      }
      check(oldAir > total / 10, "the V6.19 caves really were huge (" + oldAir + "/" + total + ")");
      check(newAir > total / 200, "the new caves still exist (" + newAir + "/" + total + ")");
      check(newAir * 2 < oldAir, "V6.21 hollows out far less rock than V6.19 (" + newAir + " vs " + oldAir + ")");

      // coordinate purity: the same world block always gets the same answer
      for (int i = 0; i < 200; i++) {
         int x = i * 37 - 400;
         int y = 8 + i * 7 % 46;
         int z = i * 53 - 600;
         eq(CaveWorks.hollow(tg.nCave1, tg.nCave2, tg.nCavern, seed, x, y, z, 1.6F, 1.3F),
            CaveWorks.hollow(tg.nCave1, tg.nCave2, tg.nCavern, seed, x, y, z, 1.6F, 1.3F),
            "the cave field is a pure function of world coordinates");
      }

      int mouths = 0;
      int shafts = 0;
      for (int x = 0; x < 128; x++) {
         for (int z = 0; z < 128; z++) {
            if (CaveWorks.mouth(tg.nEntr, seed, x, z)) mouths++;
            if (CaveWorks.shaft(seed, x, z)) shafts++;
         }
      }
      check(mouths < 128 * 128 / 4, "cave mouths are rare, not everywhere (" + mouths + ")");
      check(shafts > 0, "there are real sinkhole shafts (" + shafts + ")");

      // depth strata: deep rock is banded, shallow rock stays plain stone
      Set<Integer> deep = new HashSet<>();
      for (int y = 6; y < 16; y++) {
         for (int x = 0; x < 24; x++) deep.add(CaveWorks.deepStone(y, x, 7, seed));
      }
      check(deep.contains(Blocks313.ANDESITE) && deep.contains(Blocks313.GRANITE), "the deep layer has banded stone");
      eq(Blocks.STONE, CaveWorks.deepStone(50, 3, 3, seed), "shallow rock stays plain stone");
      check(CaveWorks.wallStone(10, 3, 3, seed) != Blocks.STONE, "cave walls are lined with layered rock");
   }

   static void surfaceSites() {
      long seed = 6120L;
      CaveWorks.Plan p = null;
      for (int rx = 0; rx <= 6 && p == null; rx++) {
         for (int rz = 0; rz <= 6 && p == null; rz++) {
            CaveWorks.Plan q = CaveWorks.plan(seed, rx, rz);
            if (q.valid) p = q;
         }
      }

      check(p != null, "the seed has an underground site");
      Gen low = new Gen(seed) {
         @Override public Gen.Col column(int x, int z) {
            Gen.Col c = new Gen.Col();
            c.h = 36; // a shallow ocean floor
            return c;
         }
      };
      Gen high = new Gen(seed) {
         @Override public Gen.Col column(int x, int z) {
            Gen.Col c = new Gen.Col();
            c.h = 120; // a mountain
            return c;
         }
      };
      eq(p.y, CaveWorks.siteY(high, p), "a tall mountain keeps the planned depth");
      eq(p.y, CaveWorks.siteY(null, p), "the pure plan is unchanged without a generator");
      eq(22, CaveWorks.siteY(low, p), "a low sea floor pushes the site under the ground");
      check(CaveWorks.siteY(low, p) < p.y, "no site floats in the water column");

      // strata must run horizontally, not look like random speckles
      int same = 0;
      for (int y = 6; y < 30; y++) {
         if (CaveWorks.deepStone(y, 3, 7, seed) == CaveWorks.deepStone(y + 1, 3, 7, seed)) same++;
      }

      check(same > 12, "deep stone forms horizontal runs (" + same + "/24 adjacent pairs)");
      eq(CaveWorks.wallStone(10, 3, 3, seed), CaveWorks.wallStone(10, 5, 3, seed),
         "cave-wall stripes run horizontally");
   }

   // ------------------------------------------------------------------ structures
   static void caveStructures() {
      long seed = 6120L;
      Set<Integer> types = new HashSet<>();
      int valid = 0;
      for (int rx = -20; rx <= 20; rx++) {
         for (int rz = -20; rz <= 20; rz++) {
            CaveWorks.Plan p = CaveWorks.plan(seed, rx, rz);
            if (p.valid) {
               valid++;
               types.add(p.type);
               check(p.x >= rx * CaveWorks.REGION && p.x < (rx + 1) * CaveWorks.REGION, "a site stays in its region");
               eq(CaveWorks.plan(seed, rx, rz).y, p.y, "room plans are deterministic");
               eq(CaveWorks.plan(seed, rx, rz).type, p.type, "room types are deterministic");
            }
         }
      }
      check(valid > 700 && valid < 1400, "about 62% of regions have a site (" + valid + ")");
      eq(6, types.size(), "all six kinds of underground build can appear");

      // every type really builds a chamber, a floor, a loot chest and a light
      for (int type = 0; type < 6; type++) {
         CaveWorks.Plan p = new CaveWorks.Plan();
         p.valid = true;
         p.x = 8;
         p.z = 8;
         p.y = 30;
         p.type = type;
         p.rx = 11;
         p.ry = 5;
         p.rz = 11;
         p.h = 12345L + type;
         Chunk c = solidChunk(0, 0);
         CaveWorks.stamp(c, p);
         int floorY = p.y - p.ry + 1;
         int ceilY = p.y + p.ry - 1;
         eq(Blocks.CHEST_LOOT, c.get(8, floorY + 2, 8), "type " + type + " has a loot chest");
         int light = type == 1 || type == 5 ? Redstone319.SEA_LANTERN : Blocks.GLOWSTONE;
         eq(light, c.get(8, ceilY, 8), "type " + type + " has a ceiling light");
         eq(0, c.get(8, floorY + 3, 8), "type " + type + " carves a real chamber");
         check(count(c, Blocks.STONE) < 16 * 16 * 66 - 100, "type " + type + " opens up the rock");
         check(has(c, Blocks.STONEBRICK) || has(c, Blocks.PLANKS) || has(c, Blocks.QUARTZ_BLOCK)
            || has(c, Blocks.OBSIDIAN) || has(c, Blocks313.MYCELIUM) || has(c, Redstone319.PRISMARINE_BRICKS),
            "type " + type + " uses its own building materials");
      }

      // a site that crosses a chunk border is identical on both sides
      CaveWorks.Plan p = new CaveWorks.Plan();
      p.valid = true;
      p.x = 24;
      p.z = 8;
      p.y = 30;
      p.type = 0;
      p.rx = 11;
      p.ry = 5;
      p.rz = 11;
      p.h = 999L;
      Chunk c0 = solidChunk(0, 0);
      Chunk c1 = solidChunk(1, 0);
      CaveWorks.stamp(c0, p);
      CaveWorks.stamp(c1, p);
      int floorY = p.y - p.ry + 1;
      eq(0, c0.get(15, floorY + 1, 8), "the corridor is carved on the left chunk");
      eq(0, c1.get(0, floorY + 1, 8), "and on the right chunk, with no seam");
      check(c0.get(15, floorY, 8) != Blocks.STONE && c1.get(0, floorY, 8) != Blocks.STONE,
         "the built floor continues across the border");
   }

   // ------------------------------------------------------------------ real generated chunks
   static void generatedWorld() {
      long seed = 6120L;
      WorldSettings st = v3();
      st.structures = false; // surface structures off: everything underground is ours
      TerraGen tg = new TerraGen(seed, st);
      CaveWorks.Plan site = null;
      for (int rx = 0; rx <= 6 && site == null; rx++) {
         for (int rz = 0; rz <= 6 && site == null; rz++) {
            CaveWorks.Plan q = CaveWorks.plan(seed, rx, rz);
            if (q.valid) site = q;
         }
      }
      check(site != null, "the seed has an underground site");
      int cx = Math.floorDiv(site.x, 16);
      int cz = Math.floorDiv(site.z, 16);
      Chunk c = tg.generate(cx, cz);
      check(has(c, Blocks.CHEST_LOOT), "a generated chunk contains a real underground loot chest");
      check(has(c, Blocks.GLOWSTONE) || has(c, Redstone319.SEA_LANTERN), "and cave lighting");
      check(has(c, Blocks313.ANDESITE) || has(c, Blocks313.GRANITE) || has(c, Blocks313.DIORITE),
         "and banded deep stone");
      check(has(c, Blocks.STONEBRICK) || has(c, Blocks.QUARTZ_BLOCK) || has(c, Blocks.PLANKS)
         || has(c, Blocks.OBSIDIAN) || has(c, Blocks313.MYCELIUM) || has(c, Redstone319.PRISMARINE_BRICKS),
         "and the site's own building materials");

      int air = 0;
      int solid = 0;
      for (int z = 0; z < 16; z++) {
         for (int x = 0; x < 16; x++) {
            for (int y = 10; y <= 50; y++) {
               int id = c.get(x, y, z);
               if (id == 0) air++;
               solid++;
            }
         }
      }
      check(air > 20, "the deep world still has caves (" + air + "/" + solid + ")");
      check(air < solid / 2, "but it is no longer one hollow cavern (" + air + "/" + solid + ")");
      check(c.hm[0] >= 60, "the generator still computes the heightmap");

      // determinism and backward compatibility
      Chunk again = new TerraGen(seed, v3()).generate(cx, cz);
      eq(true, Arrays.equals(c.blocks, again.blocks), "V6.21 generation is deterministic");
      WorldSettings s1 = new WorldSettings();
      s1.structures = true;
      Chunk a1 = new TerraGen(seed, s1).generate(0, 0);
      Chunk b1 = new TerraGen(seed, new WorldSettings()).generate(0, 0);
      eq(true, Arrays.equals(a1.blocks, b1.blocks), "the classic generator is unchanged");
      WorldSettings s2 = new WorldSettings();
      s2.terrainVer = 2;
      s2.structures = true;
      Chunk a2 = new TerraGen(seed, s2).generate(0, 0);
      WorldSettings t2 = new WorldSettings();
      t2.terrainVer = 2;
      t2.structures = true;
      Chunk b2 = new TerraGen(seed, t2).generate(0, 0);
      eq(true, Arrays.equals(a2.blocks, b2.blocks), "the V6.19 generator is still deterministic");
   }

   // ------------------------------------------------------------------ weather helpers
   static void weatherHelpers() {
      float[] a = new float[3];
      float[] b = new float[3];
      WeatherFx.curtainTint(1, 0.0F, 1.0F, 1.0F, 100.0F, 0.0F, a);
      WeatherFx.curtainTint(1, 1.0F, 1.0F, 1.0F, 100.0F, 0.0F, b);
      check(b[0] < a[0] && b[2] > a[0], "a storm darkens distant rain");
      WeatherFx.curtainTint(2, 0.0F, 1.0F, 1.0F, 100.0F, 0.0F, b);
      check(b[0] > a[0] && b[1] > a[1], "snow curtains are paler than rain");
      WeatherFx.curtainTint(1, 0.0F, 1.0F, 1.0F, 100.0F, 0.0F, a);
      WeatherFx.curtainTint(1, 0.0F, 1.0F, 1.0F, 3000.0F, 0.0F, b);
      check(b[0] > a[0], "far curtains pick up blue distance haze");
      WeatherFx.curtainTint(1, 0.0F, 1.0F, 1.0F, 100.0F, 1.0F, b);
      check(b[2] > a[2], "lightning lights the curtains");
      WeatherFx.curtainTint(1, 0.0F, 1.0F, 0.1F, 100.0F, 0.0F, b);
      check(b[0] > a[0], "a low sun puts a warm rim on the rain");

      float[] s0 = new float[2];
      float[] s1 = new float[2];
      WeatherFx.windShear(5.0F, -2.0F, 1.4F, 100.0F, 0.0F, s0);
      WeatherFx.windShear(5.0F, -2.0F, 1.4F, 100.0F, 1.0F, s1);
      eq(0.0F, s0[0], "no wind setting means no lean");
      check(s1[0] > 0.0F && s1[1] < 0.0F, "the wind leans the top of a curtain along both axes");
      WeatherFx.windShear(5.0F, -2.0F, 1.4F, 100.0F, 2.0F, s0);
      check(Math.abs(s0[0] - 2.0F * s1[0]) < 0.001F, "the wind setting scales the lean");

      check(WeatherFx.mistAlpha(1.0F, 100.0F, 0.0F) > 0.0F, "rain makes ground haze");
      check(WeatherFx.mistAlpha(1.0F, 5000.0F, 0.0F) == 0.0F, "haze fades out past the horizon");
      check(WeatherFx.mistAlpha(0.0F, 100.0F, 0.0F) == 0.0F, "no rain means no haze");
      int[] haze = WeatherFx.groundHaze(64, 64, 6005L);
      eq(64 * 64, haze.length, "the haze texture has the right size");
      check((haze[0] >>> 24) < (haze[32 * 64 + 32] >>> 24), "the haze blob is soft at the edges");

      eq(0.0F, WeatherFx.rainbowStrength(0.9F, 0.0F, 1.0F, 1.0F, 0.3F, false), "rainbows can be switched off");
      eq(0.0F, WeatherFx.rainbowStrength(0.9F, 0.0F, 1.0F, 1.0F, -0.2F, true), "no bow when the sun is down");
      eq(0.0F, WeatherFx.rainbowStrength(0.0F, 0.0F, 1.0F, 1.0F, 0.3F, true), "no bow on dry ground");
      eq(0.0F, WeatherFx.rainbowStrength(0.9F, 0.8F, 1.0F, 1.0F, 0.3F, true), "no bow in heavy rain");
      eq(0.0F, WeatherFx.rainbowStrength(0.9F, 0.0F, 0.0F, 1.0F, 0.3F, true), "no bow with the sun hidden");
      float bow = WeatherFx.rainbowStrength(0.9F, 0.02F, 1.0F, 1.0F, 0.3F, true);
      check(bow > 0.5F, "a bow appears while rain clears (" + bow + ")");

      float[] red = new float[3];
      float[] violet = new float[3];
      WeatherFx.rainbowBand(0, red);
      WeatherFx.rainbowBand(6, violet);
      check(red[0] > red[1] && red[0] > red[2], "the outer band is red");
      check(violet[2] > violet[1], "the inner band is violet");
      WeatherFx.rainbowBand(99, violet);
      check(violet[2] > 0.0F, "out-of-range bands are clamped, not thrown");

      float[] sun = new float[]{-0.95F, 0.31F, 0.0F};
      float[] d = new float[3];
      WeatherFx.rainbowDir(sun, 1.5F, 42.0F, d);
      float len = (float)Math.sqrt(d[0] * d[0] + d[1] * d[1] + d[2] * d[2]);
      check(Math.abs(len - 1.0F) < 0.001F, "rainbow directions are unit vectors");
      check(d[1] > 0.0F, "the bow is above the horizon when the sun is low");
      float dot = sun[0] * d[0] + sun[1] * d[1] + sun[2] * d[2];
      check(dot < 0.0F, "the bow is opposite the sun");
      WeatherFx.rainbowDir(sun, 1.5F, 40.0F, a);
      WeatherFx.rainbowDir(sun, 1.5F, 44.0F, b);
      check(Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]) > 0.01F, "the seven bands have real width");
   }

   static void weatherMesh() {
      Weather wx = new Weather(null, 1234L);
      wx.force(2, 99999);
      wx.instant();
      Weather.Snap sn = wx.snap(10);

      WeatherFx.Fx f0 = new WeatherFx.Fx();
      f0.quality = 0;
      f0.mist = false;
      WeatherFx.Result r0 = WeatherFx.build(sn, null, 8, 8, f0);
      WeatherFx.Fx f1 = new WeatherFx.Fx();
      f1.quality = 1;
      f1.mist = false;
      WeatherFx.Result r1 = WeatherFx.build(sn, null, 8, 8, f1);
      WeatherFx.Fx f2 = new WeatherFx.Fx();
      f2.quality = 2;
      f2.mist = false;
      WeatherFx.Result r2 = WeatherFx.build(sn, null, 8, 8, f2);
      check(r0.shaft.n > 0, "plain weather still builds rain curtains");
      check(r1.shaft.n > r0.shaft.n, "normal weather adds curtain planes");
      check(r2.shaft.n > r1.shaft.n, "detailed weather adds more curtain planes");
      eq(0, r2.mist.n, "mist is empty when switched off");

      WeatherFx.Fx f3 = new WeatherFx.Fx();
      f3.quality = 2;
      f3.mist = true;
      WeatherFx.Result r3 = WeatherFx.build(sn, null, 8, 8, f3);
      check(r3.mist.n > 0, "ground haze quads are built when enabled");
      eq(0, r3.mist.n % 4, "haze quads are complete");
      check(r3.shaft.n == r2.shaft.n, "haze is a separate mesh, it does not change the curtains");

      WeatherFx.Fx near = new WeatherFx.Fx();
      near.reach = 0.5F;
      WeatherFx.Result rn = WeatherFx.build(sn, null, 8, 8, near);
      check(rn.shaft.n < r2.shaft.n, "a shorter weather distance draws fewer distant curtains ("
         + rn.shaft.n + " < " + r2.shaft.n + ")");

      // the classic four-argument builder still works (used by the older weather tests)
      WeatherFx.Result legacy = WeatherFx.build(sn, null, 8, 8);
      eq(r2.shaft.n, legacy.shaft.n, "the legacy builder keeps the classic three-plane mesh");

   }

   static void shaderAndVersion() {
      eq("6.32", Game.VERSION_STR, "the game version is 6.32");
      check(ShaderSrc.CLOUD_FS.contains("uniform float wxBeauty"), "the cloud shader has the beauty uniform");
      check(ShaderSrc.CLOUD_FS.contains("warm sunset rim"), "the cloud shader has a warm sunset rim");
      check(ShaderSrc.CLOUD_FS.contains("silver lining"), "the cloud shader still has silver linings");
      check(ShaderSrc.CLOUD_FS.contains("ice halo"), "the cirrus layer gained an ice halo");
      check(!ShaderSrc.CLOUD_SHADOW_FS.contains("wxBeauty"), "the cloud shadow map is unchanged");
      try {
         String s = new String(Files.readAllBytes(new File("src/bc/Shaders.java").toPath()), "UTF-8");
         check(s.contains("wxBeauty"), "renderClouds sets the beauty uniform from Options");
         check(s.contains("weatherQuality"), "the beauty uniform follows the in-game setting");
      } catch (Exception e) {
         check(false, "the shader source is readable: " + e);
      }
      try {
         WeatherFx.class.getDeclaredMethod("drawRainbow", float[].class, float.class);
         String sky = new String(Files.readAllBytes(new File("src/bc/Sky.java").toPath()), "UTF-8");
         check(sky.contains("drawRainbow"), "the sky draws the rainbow");
      } catch (Exception e) {
         check(false, "the rainbow is wired into the sky: " + e);
      }
   }
}
