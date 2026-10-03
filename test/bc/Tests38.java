package bc;

import static bc.GameTests.GROUND;
import static bc.GameTests.check;
import static bc.GameTests.eq;
import static bc.GameTests.hit;
import static bc.GameTests.newGame;
import static bc.GameTests.test;
import static bc.GameTests.tick;

import bc.Mobs.Mob;
import java.util.Properties;
import java.util.Random;

/**
 * Headless tests of Update 3.8 - weather: the field maths (and that the GPU formula reduces to the same numbers), forcing,
 * storm cells, persistence, the game rules (rain puts fires out and waters fields, thunder and sleeping, zombies, lightning,
 * charged creepers), the cloud / rain-curtain mesh builder and the /weather command.   python3 tools/runtests.py unit
 */
final class Tests38 {
   static void run() {
      field();
      gpuParity();
      forcing();
      cells();
      dome();
      persistence();
      rules();
      lightning();
      meshes();
      commands();
   }

   static Weather fresh(long seed) {
      Weather w = new Weather(null, seed);
      w.ticks = 0;
      return w;
   }

   // ------------------------------------------------------------------ the field
   static void field() {
      test("weather field: maps, ordered thresholds and a sensible mix of fair / cloudy / rainy / stormy", () -> {
         eq(65536, Weather.MAP_A.length, "map A size");
         eq(65536, Weather.MAP_B.length, "map B size");
         check(Weather.C0 < Weather.C1 && Weather.C1 < Weather.R0 && Weather.R0 < Weather.R1 && Weather.R1 < Weather.T0 && Weather.T0 < Weather.T1,
            "thresholds are ordered: cover < rain < thunderstorm");
         check(Weather.SIGMA > 0.03F && Weather.SIGMA < 0.2F, "field spread " + Weather.SIGMA);
         Weather w = fresh(77);
         Weather.Snap s = w.snap(0.0);
         int n = 0;
         int clear = 0;
         int overcast = 0;
         int rain = 0;
         int storm = 0;
         float[] o = new float[4];
         for (int i = 0; i < 80; i++) {
            for (int j = 0; j < 80; j++) {
               s.at(i * 190.0, j * 190.0, o);
               n++;
               if (o[0] < 0.05F) {
                  clear++;
               }
               if (o[0] > 0.95F) {
                  overcast++;
               }
               if (o[1] > 0.5F) {
                  rain++;
               }
               if (o[2] > 0.5F) {
                  storm++;
               }
               check(o[0] >= 0 && o[0] <= 1 && o[1] >= 0 && o[1] <= 1 && o[2] >= 0 && o[2] <= 1, "values stay in 0..1");
               check(o[1] <= o[0] + 1e-4F, "rain only where it is overcast");
               check(o[2] <= o[1] + 1e-4F, "thunder only inside the rain");
            }
         }
         System.out.printf("          (weather sample: clear %.0f%%, overcast %.0f%%, rain %.0f%%, thunderstorm %.0f%%; thresholds %.3f %.3f %.3f %.3f %.3f %.3f, sigma %.3f)%n",
            clear * 100.0 / n, overcast * 100.0 / n, rain * 100.0 / n, storm * 100.0 / n, Weather.C0, Weather.C1, Weather.R0, Weather.R1, Weather.T0, Weather.T1, Weather.SIGMA);
         double fc = clear / (double)n;
         double fr = rain / (double)n;
         double fs = storm / (double)n;
         check(fc > 0.25 && fc < 0.60, "clear sky over " + (int)(fc * 100) + "% of the map");
         check(fr > 0.04 && fr < 0.30, "rain over " + (int)(fr * 100) + "% of the map");
         check(fs > 0.01 && fs < 0.12, "thunderstorms over " + (int)(fs * 100) + "% of the map");
         check(overcast > rain, "more overcast than rainy");
      });
      test("weather field: deterministic per seed, different seeds differ, drifts with the wind, evolves over time", () -> {
         Weather a = fresh(5);
         Weather b = fresh(5);
         Weather c = fresh(6);
         int diff = 0;
         Random r = new Random(1);
         for (int i = 0; i < 200; i++) {
            double x = r.nextDouble() * 20000 - 10000;
            double z = r.nextDouble() * 20000 - 10000;
            double t = r.nextDouble() * 5000;
            float va = a.snap(t).s(x, z);
            eq(va, b.snap(t).s(x, z), "same seed, same value");
            eq(va, a.snap(t).s(x, z), "a snapshot is pure");
            if (Math.abs(va - c.snap(t).s(x, z)) > 1e-3F) {
               diff++;
            }
         }
         check(diff > 150, "another seed gives another sky (" + diff + "/200 differ)");
         // wind: layer A moves 3..9 blocks per second on average
         double ts = 1000;
         Weather.Snap s1 = a.snap(ts);
         Weather.Snap s2 = a.snap(ts + 120);
         double dx = s2.oax - s1.oax;
         double dz = s2.oaz - s1.oaz;
         double speed = Math.sqrt(dx * dx + dz * dz) / 120.0;
         check(speed > 2.0 && speed < 9.0, "the pattern drifts at " + speed + " blocks/s");
         double wl = Math.sqrt(s1.windX * s1.windX + s1.windZ * s1.windZ);
         check(wl > 1.0 && wl < 12.0, "wind speed " + wl);
         // a storm that is here now is somewhere else later, and the field is not just a translation (layers move apart)
         int moved = 0;
         for (int i = 0; i < 100; i++) {
            double x = r.nextDouble() * 9000;
            double z = r.nextDouble() * 9000;
            if (Math.abs(a.snap(0).s(x, z) - a.snap(900).s(x, z)) > 0.02F) {
               moved++;
            }
         }
         check(moved > 60, "the weather changes within 15 minutes at most places (" + moved + "/100)");
         // continuity: a small step in space or time never jumps
         for (int i = 0; i < 300; i++) {
            double x = r.nextDouble() * 9000;
            double z = r.nextDouble() * 9000;
            double t = r.nextDouble() * 3000;
            check(Math.abs(a.snap(t).s(x, z) - a.snap(t).s(x + 2, z)) < 0.03F, "continuous in space");
            check(Math.abs(a.snap(t).s(x, z) - a.snap(t + 0.05).s(x, z)) < 0.01F, "continuous in time");
         }
      });
      test("weather field: climate cycle makes some stretches stormier than others", () -> {
         Weather w = fresh(31);
         float lo = 9;
         float hi = -9;
         for (int i = 0; i < 400; i++) {
            float c = w.climate(i * 25.0);
            lo = Math.min(lo, c);
            hi = Math.max(hi, c);
         }
         check(hi > 0.5F * Weather.SIGMA && lo < -0.5F * Weather.SIGMA, "bias swings by " + lo + " .. " + hi);
         check(hi <= 0.9F * Weather.SIGMA + 1e-5F && lo >= -0.9F * Weather.SIGMA - 1e-5F, "but stays inside the designed range");
      });
   }

   /** The cloud shader works on coordinates reduced modulo 9000; this proves the reduction does not change the field. */
   static void gpuParity() {
      test("CPU and GPU agree: the shader's formula on coordinates reduced modulo 9000 gives the CPU value", () -> {
         Weather w = fresh(2024);
         w.force(2, 99999);
         w.forceAmt = 0.4F;
         Weather.Cell c = w.placeStorm(5000, -3000, 500);
         c.born = -100;
         Random r = new Random(9);
         for (int i = 0; i < 400; i++) {
            double ts = r.nextDouble() * 400000;
            Weather.Snap s = w.snap(ts);
            double x = r.nextDouble() * 400000 - 200000;
            double z = r.nextDouble() * 400000 - 200000;
            float cpu = s.s(x, z);
            // what the fragment shader does
            double px = mod(x, 9000);
            double pz = mod(z, 9000);
            float a = Weather.samp(Weather.MAP_A, (px - mod(s.oax, 9000)) / 9000.0, (pz - mod(s.oaz, 9000)) / 9000.0);
            float b = Weather.samp(Weather.MAP_B, (px - mod(s.obx, 9000)) / 4500.0, (pz - mod(s.obz, 9000)) / 4500.0);
            float g = 0.62F * a + 0.38F * b + s.bias;
            g += (Math.max(Weather.FAIR_LO, Math.min(g, Weather.FAIR)) - g) * s.calmAmt; // (the calm clamp comes before the cells)
            for (int k = 0; k < s.cells.length; k += 4) {
               // cell positions arrive relative to the camera (here: the sample point plays the camera)
               double ddx = x - s.cells[k];
               double ddz = z - s.cells[k + 1];
               float dl = (float)(Math.sqrt(ddx * ddx + ddz * ddz) / s.cells[k + 2]);
               if (s.cells[k + 3] > 0 && dl < 1.0F) {
                  float t = Math.max(0.0F, Math.min(1.0F, (1.0F - dl) * 1.6F));
                  g += Math.max(0.0F, Weather.cellPeak() - g) * (t * t * (3.0F - 2.0F * t)) * s.cells[k + 3];
               }
            }
            g += (Math.max(s.lo, Math.min(s.hi, g)) - g) * s.amt;
            check(Math.abs(g - cpu) < 2e-4F, "field equal at ts=" + (int)ts + " x=" + (int)x + " (" + cpu + " vs " + g + ")");
         }
      });
      test("CPU cloud density equals the shader's density on coordinates reduced modulo 9000", () -> {
         Weather w = fresh(8);
         Random r = new Random(4);
         for (int i = 0; i < 300; i++) {
            Weather.Snap s = w.snap(r.nextDouble() * 100000);
            double wx = r.nextDouble() * 300000 - 150000;
            double wz = r.nextDouble() * 300000 - 150000;
            double u = wx - s.oax;
            double v = wz - s.oaz;
            float h = r.nextFloat();
            float cover = r.nextFloat();
            float storm = r.nextFloat() * cover;
            float cpu = WeatherFx.density(u, v, h, cover, storm);
            double qx = mod(wx, 9000) - mod(s.oax, 9000);
            double qz = mod(wz, 9000) - mod(s.oaz, 9000);
            float gpu = WeatherFx.density(qx, qz, h, cover, storm);
            check(Math.abs(cpu - gpu) < 1e-4F, "density equal (" + cpu + " vs " + gpu + ")");
         }
      });
   }

   static double mod(double v, double m) {
      double r = v % m;
      return r < 0 ? r + m : r;
   }

   // ------------------------------------------------------------------ forcing, cells
   static void forcing() {
      test("/weather forcing: clear, rain and thunder take over everywhere, fade in and out, then the natural weather returns", () -> {
         Weather w = fresh(11);
         float[] o = new float[4];
         w.force(2, 30);
         check(w.tickSnap.amt == 0.0F, "forcing starts at 0");
         for (int i = 0; i < 205; i++) {
            w.tick();
         }
         check(w.forceAmt >= 0.999F, "fully faded in after 10 seconds");
         for (int i = 0; i < 60; i++) {
            w.tickSnap.at(i * 777.0, -i * 421.0, o);
            check(o[1] > 0.99F, "rain everywhere");
         }
         // the 30 s run out, then it fades away (20 s) and the mode is released
         for (int i = 0; i < 450 + 410; i++) {
            w.tick();
         }
         eq(0, w.forceMode, "natural again");
         w.force(3, 60);
         w.instant();
         for (int i = 0; i < 60; i++) {
            w.tickSnap.at(i * 777.0, -i * 421.0, o);
            check(o[2] > 0.99F, "thunderstorm everywhere");
         }
         w.force(1, 60);
         w.instant();
         for (int i = 0; i < 60; i++) {
            w.tickSnap.at(i * 777.0, -i * 421.0, o);
            check(o[1] == 0.0F && o[2] == 0.0F && o[0] <= 0.6F, "forced clear: only fair-weather clouds, no rain, no storm");
         }
         w.natural();
         eq(0, w.forceMode, "natural() clears the forcing");
         w.enabled = false;
         for (int i = 0; i < 100; i++) {
            Weather.Snap s = w.snap(i * 300.0);
            s.at(i * 91.0, i * 53.0, o);
            check(o[1] == 0.0F && o[2] == 0.0F, "weather off: no rain, no storm");
            check(o[0] <= 0.6F, "weather off: only fair-weather clouds (cover " + o[0] + ")");
         }
         w.enabled = true;
         w.suppressed = true;
         w.snap(5).at(1, 1, o);
         check(o[1] == 0.0F && o[2] == 0.0F, "the options switch behaves like /weather off");
      });
   }

   static void cells() {
      test("storm cells: /weather storm summons a thunderstorm upwind that drifts over the player, grows and dies", () -> {
         Weather w = fresh(21);
         w.enabled = true;
         double px = 1000;
         double pz = -500;
         Weather.Cell c = w.summonStorm(px, pz, 900);
         double dx = c.x - px;
         double dz = c.z - pz;
         double d = Math.sqrt(dx * dx + dz * dz);
         check(d > 880 && d < 960, "starts about 900 blocks away (" + d + ")");
         Weather.Snap s0 = w.snap(w.ticks / 20.0);
         check(s0.storm(c.x, c.z) < 0.01F, "just born: no storm yet");
         // let it grow and drift
         double t = (w.ticks + 1500) / 20.0;
         w.ticks += 1500;
         Weather.Snap s1 = w.snap(t);
         double ccx = s1.cells[0];
         double ccz = s1.cells[1];
         check(s1.storm(ccx, ccz) > 0.95F, "grown: a full thunderstorm at its centre");
         check(s1.storm(ccx + 600, ccz) < 0.9F || s1.storm(ccx - 600, ccz) < 0.9F, "but it is a cell, not the whole world");
         double dist1 = Math.hypot(ccx - px, ccz - pz);
         w.ticks += 2000;
         Weather.Snap s2 = w.snap(w.ticks / 20.0);
         double dist2 = Math.hypot(s2.cells[0] - px, s2.cells[1] - pz);
         check(dist2 < dist1, "it drifts towards the player: " + (int)dist1 + " -> " + (int)dist2);
         w.tick();
         check(w.nearestStorm(s2.cells[0] + 5, s2.cells[1]) != null, "nearestStorm finds it");
         // it dies
         w.ticks += 20000;
         w.tick();
         eq(0, w.cells.size(), "the cell is gone after its life");
         for (int i = 0; i < 6; i++) {
            w.summonStorm(0, 0, 100);
         }
         eq(4, w.cells.size(), "at most 4 cells at once (the shader has 4 slots)");
      });
   }

   static void dome() {
      test("storm cell: a dome (the thunderhead grows towards the middle), thunder under most of the cloud, distant storms are found by the sky scan", () -> {
         Weather w = fresh(40);
         w.enabled = true;
         w.calm = true;
         Weather.Cell c = w.placeStorm(1800, 0, 600);
         Weather.Snap s = w.snap(w.ticks / 20.0);
         float[] o = new float[4];
         double cx = s.cells[0];
         double cz = s.cells[1];
         float prevCore = 2.0F;
         float coreMid = 0;
         float stormEdge = 0;
         for (int k = 0; k <= 20; k++) {
            double d = k / 20.0;
            s.at(cx + d * 600, cz, o);
            check(o[3] <= prevCore + 1e-5F, "the core never grows towards the rim");
            prevCore = o[3];
            if (k == 13) {
               coreMid = o[3]; // 0.65 of the radius
            }
            if (d < 0.5) {
               check(o[2] > 0.95F, "full thunderstorm out to half the radius (at " + d + ")");
            }
            if (d > 0.85) {
               check(o[2] < 0.05F, "no thunder near the rim (at " + d + ")");
            }
            check(o[1] <= o[0] + 1e-4F && o[2] <= o[1] + 1e-4F, "cover >= rain >= thunder");
         }
         s.at(cx, cz, o);
         check(o[3] > 0.97F, "the very centre is the tallest point: " + o[3]);
         check(coreMid < o[3] - 0.5F, "two thirds out the tower is much lower, a dome: " + coreMid + " vs " + o[3]);
         // the thunderhead stays inside the slab the shader marches
         check(Weather.baseAlt(1.0F) + 46.0F + 28.0F + 150.0F <= Shaders.CLOUD_Y1 - 8.0F, "tallest tower fits the slab");
         // the sky scan
         w.tick();
         w.scanSky(0, 0);
         check(w.maxStorm > 0.95F && w.maxCore > 0.5F, "scan sees the storm 1800 blocks away: " + w.maxStorm + " / " + w.maxCore);
         w.natural();
         w.calm = true;
         w.tick();
         w.scanSky(0, 0);
         check(w.maxStorm == 0.0F && w.maxCore == 0.0F, "a calm sky has nothing to scan for");
         // the height of the mesh: centre column higher than a column halfway out
         Game g = weatherGame(0);
         g.world.weather.calm = true;
         g.world.weather.placeStorm(900, 0, 600);
         Weather.Snap sg = g.world.weather.snap(g.world.weather.ticks / 20.0);
         WeatherFx.Result r = WeatherFx.build(sg, g.world, 8, 8);
         float topC = columnTop(r, sg, sg.cells[0], sg.cells[1]);
         float topM = columnTop(r, sg, sg.cells[0] + 480, sg.cells[1]);
         check(topC > topM + 40.0F && topC > 300.0F, "centre column " + topC + " is taller than the one 0.8 of the radius out " + topM);
      });
   }

   /** Highest y of the cloud vertices within 40 blocks (horizontally) of a world point. */
   static float columnTop(WeatherFx.Result r, Weather.Snap s, double wx, double wz) {
      float best = -1e9F;
      for (int lv = 0; lv < 2; lv++) {
         for (int i = 0; i < r.cloud[lv].n; i++) {
            double x = r.cloud[lv].b.getFloat(i * 16) + (double)r.ui0[lv] * WeatherFx.CELLS[lv] + s.oax;
            double z = r.cloud[lv].b.getFloat(i * 16 + 8) + (double)r.uj0[lv] * WeatherFx.CELLS[lv] + s.oaz;
            if (Math.abs(x - wx) < 40 && Math.abs(z - wz) < 40) {
               best = Math.max(best, r.cloud[lv].b.getFloat(i * 16 + 4));
            }
         }
      }

      return best;
   }

   static void persistence() {
      test("weather is saved with the world; old worlds start under a fair sky", () -> {
         Weather w = fresh(3);
         w.ticks = 123456;
         w.force(2, 500);
         w.forceAmt = 0.75F;
         w.enabled = false;
         w.summonStorm(10, 20, 700);
         Properties p = new Properties();
         w.save(p);
         Weather r = fresh(3);
         r.load(p);
         eq(123456L, r.ticks, "clock");
         eq(2, r.forceMode, "force mode");
         eq(w.forceEnd, r.forceEnd, "force end");
         check(Math.abs(r.forceAmt - 0.75F) < 1e-6F, "force amount");
         check(!r.enabled, "off stays off");
         eq(1, r.cells.size(), "cell");
         check(Math.abs(r.cells.get(0).x - w.cells.get(0).x) < 1e-6 && r.cells.get(0).radius == w.cells.get(0).radius, "cell data");
         for (int i = 0; i < 50; i++) {
            eq(w.snap(1000 + i).s(i * 33.0, i * 71.0), r.snap(1000 + i).s(i * 33.0, i * 71.0), "identical sky after loading");
         }
         Weather old = fresh(3);
         old.load(new Properties());
         check(old.calmUntil > old.ticks, "a world from before 3.8 starts calm");
         float[] o3 = new float[4];
         for (int i = 0; i < 80; i++) {
            old.snap(old.ticks / 20.0).at(i * 211.0, i * 97.0, o3);
            check(o3[1] == 0.0F && o3[2] == 0.0F, "calm start: no rain, no storm");
            check(o3[0] <= 0.6F, "calm start: fair-weather clouds only");
         }
         old.ticks += 6000;
         int wild = 0;
         for (int i = 0; i < 400; i++) {
            old.snap(old.ticks / 20.0).at(i * 211.0, i * 97.0, o3);
            if (o3[1] > 0.5F) {
               wild++;
            }
         }
         check(wild > 10, "after five minutes the natural weather is back (" + wild + "/400 rainy)");
         Weather saved = fresh(3);
         saved.startFair();
         Properties sp = new Properties();
         saved.save(sp);
         Weather loadedCalm = fresh(3);
         loadedCalm.load(sp);
         eq(saved.calmUntil, loadedCalm.calmUntil, "calm period is saved");
         Properties junk = new Properties();
         junk.setProperty("wclock", "banana");
         Weather j = fresh(3);
         j.load(junk);
         eq(0, j.forceMode, "garbage in the save file does not crash and means natural weather");
      });
   }

   // ------------------------------------------------------------------ game rules
   static Game weatherGame(int mode) throws Exception {
      Game g = newGame(mode);
      g.world.weather.enabled = true;
      return g;
   }

   static void rules() {
      test("rain: only where nothing is overhead; not in deserts; snow in cold biomes instead", () -> {
         Game g = weatherGame(0);
         World w = g.world;
         w.weather.force(2, 9999);
         w.weather.instant();
         check(w.weather.rainingAt(20, GROUND + 1, 20), "rain on open ground");
         w.setBlock(20, GROUND + 3, 20, Blocks.STONE);
         check(!w.weather.rainingAt(20, GROUND + 1, 20), "not under a roof");
         check(w.weather.rainingAt(20, GROUND + 4, 20), "but on the roof itself");
         check(w.weather.rainingAt(21, GROUND + 1, 20), "next to the roof it rains");
         Chunk c = w.chunkAt(40, 40);
         for (int i = 0; i < 256; i++) {
            c.biome[i] = (byte)Gen.DESERT;
         }
         check(!w.weather.rainingAt(40, GROUND + 1, 40), "no rain in the desert");
         eq(-1, w.weather.precipKind(40, 40), "desert is dry");
         for (int i = 0; i < 256; i++) {
            c.biome[i] = (byte)Gen.TUNDRA;
         }
         check(!w.weather.rainingAt(40, GROUND + 1, 40), "snow is not rain");
         eq(2, w.weather.precipKind(40, 40), "ice plains: snow");
         check(w.weather.describe(40, 40).contains("snow"), "described as snow: " + w.weather.describe(40, 40));
         for (int i = 0; i < 256; i++) {
            c.biome[i] = 2;
         }
         eq(1, w.weather.precipKind(40, 40), "plains: rain");
         w.weather.suppressed = true;
         check(!w.weather.rainingAt(20, GROUND + 4, 20), "options: weather off means dry");
      });
      test("rain puts fires out, a clear sky does not", () -> {
         int[] left = new int[2];
         for (int mode = 0; mode < 2; mode++) {
            Game g = weatherGame(0);
            World w = g.world;
            g.player.setPos(8.5, GROUND + 1, 8.5);
            if (mode == 1) {
               w.weather.force(2, 99999);
            } else {
               w.weather.force(1, 99999);
            }
            w.weather.instant();
            int placed = 0;
            for (int i = 0; i < 40; i++) {
               int x = 2 + (i % 8) * 3;
               int z = 2 + (i / 8) * 3;
               w.setBlock(x, GROUND + 1, z, Blocks.PLANKS);
               if (w.fire.ignite(x, GROUND + 2, z)) {
                  placed++;
               }
            }
            eq(40, placed, "40 fires on planks");
            for (int i = 0; i < 160; i++) {
               w.tick();
            }
            int alive = 0;
            for (int i = 0; i < 40; i++) {
               int x = 2 + (i % 8) * 3;
               int z = 2 + (i / 8) * 3;
               if (w.getBlock(x, GROUND + 2, z) == Blocks.FIRE) {
                  alive++;
               }
            }
            left[mode] = alive;
         }
         check(left[0] >= 25, "clear sky: fires keep burning (" + left[0] + "/40)");
         check(left[1] < left[0] * 0.6, "rain: most fires are out after 8 seconds (" + left[1] + " vs " + left[0] + ")");
      });
      test("rain waters farmland that is far from any water", () -> {
         Game g = weatherGame(0);
         World w = g.world;
         w.setBlock(30, GROUND, 30, Blocks.FARMLAND);
         check(!w.hydrated(30, GROUND, 30), "dry field, clear sky");
         w.weather.force(2, 9999);
         w.weather.instant();
         check(w.hydrated(30, GROUND, 30), "wet in the rain");
         w.setBlock(30, GROUND + 1, 30, Blocks.STONE);
         check(!w.hydrated(30, GROUND, 30), "a roof keeps the field dry");
         // and the random ticks turn it wet
         w.setBlock(30, GROUND + 1, 30, 0);
         g.player.setPos(30.5, GROUND + 1, 33.5);
         for (int i = 0; i < 400 && w.getBlock(30, GROUND, 30) == Blocks.FARMLAND; i++) {
            w.tick();
         }
         eq(Blocks.FARMLAND_WET, w.getBlock(30, GROUND, 30), "the rain made the farmland wet");
      });
      test("zombies and skeletons burn in the sun but not in the rain; thunderstorms let you sleep by day", () -> {
         Game g = weatherGame(0);
         World w = g.world;
         g.player.setPos(8.5, GROUND + 1, 8.5);
         Mob z = g.mobs.spawn(Mobs.ZOMBIE, 30.5, GROUND + 1, 30.5);
         w.dayTime = 6000;
         w.weather.force(1, 99999);
         w.weather.instant();
         g.mobs.tickMob(z, w, g.player, 1.0F);
         check(z.burn > 0, "burns in the daylight");
         Mob z2 = g.mobs.spawn(Mobs.ZOMBIE, 40.5, GROUND + 1, 40.5);
         w.weather.force(2, 99999);
         w.weather.instant();
         g.mobs.tickMob(z2, w, g.player, 1.0F);
         eq(0, z2.burn, "does not burn in the rain");
         // sleeping
         GameTests.hold(g, Items.BED, 1);
         g.player.yaw = 0;
         g.target = GameTests.top(10, 10);
         check(g.placeBlock(), "bed placed");
         w.weather.force(1, 99999);
         w.weather.instant();
         long before = w.dayTime;
         g.sleepAt(10, GROUND + 1, 10);
         eq(before, w.dayTime, "bright day, clear sky: cannot sleep");
         w.weather.force(3, 99999);
         w.weather.instant();
         check(w.weather.thunderingAt(8, 10), "thunderstorm overhead");
         g.sleepAt(10, GROUND + 1, 10);
         check(w.dayTime > before + 10000, "sleeping through a thunderstorm skips to the morning");
         eq(0, w.weather.forceMode, "the thunderstorm is over");
         check(w.weather.calmUntil > w.weather.ticks + 6000, "and a few calm minutes follow");
         w.weather.instant();
         check(!w.weather.thunderingAt(8, 10), "no thunder after waking up");
      });
   }

   static void lightning() {
      test("lightning: strikes during thunderstorms, sets fire, hurts what it hits and charges creepers", () -> {
         Game g = weatherGame(0);
         World w = g.world;
         g.player.setPos(8.5, GROUND + 1, 8.5);
         w.weather.force(3, 999999);
         w.weather.instant();
         int strikes = 0;
         for (int i = 0; i < 2400 && strikes < 5; i++) {
            w.tick();
            strikes = (int)w.weather.lightningCount;
         }
         check(strikes >= 5, "a thunderstorm produces lightning (" + strikes + " bolts in two minutes)");
         check(!w.weather.bolts.isEmpty() || w.weather.lightningCount > 0, "bolts are recorded for the renderer");
         // a direct hit
         Game h = weatherGame(0);
         h.world.weather.suppressed = false;
         h.player.setPos(30.5, GROUND + 1, 30.5);
         Mob creeper = h.mobs.spawn(Mobs.CREEPER, 20.5, GROUND + 1, 20.5);
         Mob pig = h.mobs.spawn(Mobs.PIG, 21.2, GROUND + 1, 20.5);
         int hp = pig.health;
         Weather.Bolt b = h.world.weather.strike(20.5, 20.5, true);
         check(b.real, "a strike near the player is real");
         check(creeper.charged, "the creeper is charged");
         check(pig.health < hp, "the pig is hurt");
         check(pig.burn > 0, "and burning");
         check(h.world.fire.count() > 0, "the grass is on fire");
         check(b.seg.length >= 7 * 8 && b.seg.length % 7 == 0, "the bolt has a path: " + b.seg.length / 7 + " segments");
         eq(0.0F, b.env(100), "the flash is over");
         check(b.env(0) > 0.9F, "starts with a blinding flash");
         // the player itself
         Game k = weatherGame(0);
         k.player.setPos(20.5, GROUND + 1, 20.5);
         int php = k.player.health;
         k.world.weather.strike(20.5, 20.5, true);
         check(k.player.health < php, "a lightning strike hurts the player (" + php + " -> " + k.player.health + ")");
         check(k.player.burn > 0, "and sets them on fire");
         Game cr = weatherGame(1);
         cr.player.setPos(20.5, GROUND + 1, 20.5);
         int chp = cr.player.health;
         cr.world.weather.strike(20.5, 20.5, true);
         eq(chp, cr.player.health, "creative players are safe");
         // far away strikes are only a show
         Game f = weatherGame(0);
         f.player.setPos(8.5, GROUND + 1, 8.5);
         Weather.Bolt far = f.world.weather.strike(900, 900, true);
         check(!far.real, "a strike 1200 blocks away does not touch the world");
         eq(0, f.world.fire.count(), "no fire");
         // not while the game is paused
         Game p = weatherGame(0);
         p.world.weather.force(3, 99999);
         p.world.weather.instant();
         p.screen = Game.S.PAUSE;
         long c0 = p.world.weather.lightningCount;
         for (int i = 0; i < 600; i++) {
            p.world.weather.tick();
         }
         eq(c0, p.world.weather.lightningCount, "no lightning in the pause menu");
      });
      test("charged creepers explode with twice the power", () -> {
         int[] holes = new int[2];
         for (int charged = 0; charged < 2; charged++) {
            Game g = weatherGame(0);
            g.player.setPos(2.5, GROUND + 1, 2.5);
            for (int x = 20; x <= 40; x++) {
               for (int y = GROUND - 12; y <= GROUND + 12; y++) {
                  for (int z = 20; z <= 40; z++) {
                     g.world.setBlock(x, y, z, y <= GROUND ? Blocks.DIRT : 0);
                  }
               }
            }
            Mob c = g.mobs.spawn(Mobs.CREEPER, 30.5, GROUND + 1, 30.5);
            c.charged = charged == 1;
            int before = Tests37.count(g.world, 20, GROUND - 12, 20, 40, GROUND, 40, Blocks.DIRT);
            g.mobs.explode(c);
            holes[charged] = before - Tests37.count(g.world, 20, GROUND - 12, 20, 40, GROUND, 40, Blocks.DIRT);
         }
         check(holes[0] > 20, "a normal creeper digs a crater (" + holes[0] + ")");
         check(holes[1] > holes[0] * 3, "a charged creeper digs a far bigger one (" + holes[1] + ")");
      });
      test("bolt geometry: a path from the cloud to the ground, forks fade, deterministic", () -> {
         float[] a = Weather.boltGeometry(5L, 100, 160, -40, 64);
         float[] b = Weather.boltGeometry(5L, 100, 160, -40, 64);
         check(java.util.Arrays.equals(a, b), "deterministic");
         check(a.length % 7 == 0 && a.length > 7 * 10, "segments: " + a.length / 7);
         float lowest = 1e9F;
         float highest = -1e9F;
         for (int i = 0; i < a.length; i += 7) {
            for (int k = 0; k < 6; k++) {
               check(!Float.isNaN(a[i + k]) && !Float.isInfinite(a[i + k]), "finite");
            }
            lowest = Math.min(lowest, Math.min(a[i + 1], a[i + 4]));
            highest = Math.max(highest, Math.max(a[i + 1], a[i + 4]));
            check(a[i + 6] > 0.0F && a[i + 6] <= 1.0F, "width in range");
         }
         check(Math.abs(lowest - 64) < 1.0F || lowest < 64, "reaches the ground");
         check(Math.abs(highest - 160) < 0.01F, "starts at the cloud base");
         // the main channel ends exactly at the strike point
         boolean ends = false;
         for (int i = 0; i < a.length; i += 7) {
            if (Math.abs(a[i + 3] - 100) < 1e-3F && Math.abs(a[i + 4] - 64) < 1e-3F && Math.abs(a[i + 5] + 40) < 1e-3F) {
               ends = true;
            }
         }
         check(ends, "the channel ends at the strike point");
      });
   }

   // ------------------------------------------------------------------ meshes (CPU part of the renderers)
   static int nv(WeatherFx.Result r) {
      return r.cloud[0].n + r.cloud[1].n;
   }

   /** lowest and highest y of all cloud vertices (parses the vertex buffer: x, y, z floats + colour). */
   static float[] heights(WeatherFx.Result r) {
      float lo = 1e9F;
      float hi = -1e9F;
      for (int lv = 0; lv < 2; lv++) {
         for (int i = 0; i < r.cloud[lv].n; i++) {
            float y = r.cloud[lv].b.getFloat(i * 16 + 4);
            lo = Math.min(lo, y);
            hi = Math.max(hi, y);
         }
      }

      return new float[]{lo, hi};
   }

   static void meshes() {
      test("cloud mesh: nothing in a clear sky, a big dark lowered mass in a storm, rain curtains only where it rains", () -> {
         Game g = weatherGame(0);
         World w = g.world;
         Weather wx = w.weather;
         wx.force(1, 99999);
         wx.instant();
         WeatherFx.Result clear = WeatherFx.build(wx.snap(10), w, 8, 8);
         check(nv(clear) > 0, "fair-weather clouds when forced clear (" + nv(clear) + " vertices)");
         eq(0, clear.shaft.n, "no rain curtains");
         wx.force(2, 99999);
         wx.instant();
         WeatherFx.Result rain = WeatherFx.build(wx.snap(10), w, 8, 8);
         check(nv(rain) > 4000, "overcast: a lot of cloud (" + nv(rain) + " vertices)");
         check(nv(clear) < nv(rain) / 2, "far less cloud than in the rain (" + nv(clear) + " vs " + nv(rain) + ")");
         check(rain.shaft.n > 400, "rain curtains hang under it (" + rain.shaft.n + " vertices)");
         eq(0, rain.cloud[0].n % 4, "quads");
         eq(0, rain.cloud[1].n % 4, "quads");
         eq(0, rain.shaft.n % 4, "quads");
         check(rain.cloud[0].n > 0 && rain.cloud[1].n > 0, "both the fine near level and the coarse far level have clouds");
         wx.force(3, 99999);
         wx.instant();
         WeatherFx.Result storm = WeatherFx.build(wx.snap(10), w, 8, 8);
         float[] hs = heights(storm);
         float[] hc = heights(clear);
         check(hs[1] > 300.0F && hs[1] > hc[1] + 70.0F, "storm clouds tower: top " + hs[1] + " vs " + hc[1] + " in fair weather");
         check(hs[0] < 150.0F && hs[0] < hc[0], "and hang lower: base " + hs[0] + " vs " + hc[0]);
         check(hc[0] >= 160.0F && hc[1] < 260.0F, "fair-weather cumulus stay in a thin layer: " + hc[0] + " .. " + hc[1]);
         check(hs[0] >= 110.0F && hs[1] <= Shaders.CLOUD_Y1 + 4.0F, "storms stay inside the slab the shader marches (" + Shaders.CLOUD_Y0 + ".." + Shaders.CLOUD_Y1 + ")");
         // natural weather: a mix
         wx.natural();
         WeatherFx.Result nat = WeatherFx.build(wx.snap(500), w, 3000, -2000);
         check(nv(nat) >= 0 && nv(nat) < 4_000_000, "natural weather builds, sane size " + nv(nat));
         // desert: no curtains (dry)
         Game d = weatherGame(0);
         for (Chunk c : d.world.chunks.values()) {
            for (int i = 0; i < 256; i++) {
               c.biome[i] = (byte)Gen.DESERT;
            }
         }
         d.world.weather.force(2, 99999);
         d.world.weather.instant();
         WeatherFx.Result dry = WeatherFx.build(d.world.weather.snap(10), d.world, 8, 8);
         check(dry.shaft.n < rain.shaft.n, "fewer curtains over a desert (" + dry.shaft.n + " < " + rain.shaft.n + ")");
         // a summoned storm cell on the horizon is visible as a bump of cloud and curtains in its direction only
         Game s = weatherGame(0);
         s.world.weather.force(1, 99999);
         s.world.weather.instant();
         s.world.weather.forceMode = 0;
         s.world.weather.forceAmt = 0;
         s.world.weather.placeStorm(900, 0, 350);
         s.world.weather.enabled = true;
         WeatherFx.Result cell = WeatherFx.build(s.world.weather.snap(s.world.weather.ticks / 20.0), s.world, 8, 8);
         check(nv(cell) > 0, "the cell has clouds");
         // the storm 900 blocks east is seen by the coarse level and has its rain curtains, nothing of it is west of the player
         check(cell.shaft.n > 0, "and rain curtains");
      });
      test("cloud density: more cover, more cloud; storms are thicker; nothing when it is clear", () -> {
         int nClear = 0;
         int nCloud = 0;
         int nStorm = 0;
         Random r = new Random(5);
         for (int i = 0; i < 4000; i++) {
            double u = r.nextDouble() * 9000;
            double v = r.nextDouble() * 9000;
            float h = r.nextFloat();
            if (WeatherFx.density(u, v, h, 0.0F, 0.0F) > 0.01F) {
               nClear++;
            }
            if (WeatherFx.density(u, v, h, 0.6F, 0.0F) > 0.01F) {
               nCloud++;
            }
            if (WeatherFx.density(u, v, h, 1.0F, 0.0F) > 0.01F) {
               nStorm++;
            }
         }
         check(nClear < 80, "cover 0 is (almost) empty: " + nClear);
         check(nCloud > nClear * 3, "cover 0.6 has clouds: " + nCloud);
         check(nStorm > nCloud, "full cover has more: " + nStorm);
      });
   }

   // ------------------------------------------------------------------ the command
   static void commands() {
      test("/weather: clear, rain, thunder, storm, natural, on / off and the status line", () -> {
         Game g = weatherGame(1);
         Weather w = g.world.weather;
         g.runCommand("/weather rain 45");
         eq(2, w.forceMode, "rain");
         eq(w.ticks + 900, w.forceEnd, "for 45 seconds");
         g.runCommand("/weather thunder");
         eq(3, w.forceMode, "thunder");
         g.runCommand("/weather clear 10");
         eq(1, w.forceMode, "clear");
         g.runCommand("/weather storm 600");
         eq(1, w.cells.size(), "a storm cell was summoned");
         g.runCommand("/weather natural");
         eq(0, w.forceMode, "natural");
         eq(0, w.cells.size(), "cells cleared");
         g.runCommand("/weather off");
         check(!w.enabled, "off");
         g.runCommand("/weather on");
         check(w.enabled, "on");
         int lines = g.chatLog.size();
         g.runCommand("/weather");
         check(g.chatLog.size() >= lines + 2, "status prints two lines");
         check(g.chatLog.get(g.chatLog.size() - 2).startsWith("Weather here:"), "status: " + g.chatLog.get(g.chatLog.size() - 2));
         g.runCommand("/weather rain banana");
         check(g.chatLog.get(g.chatLog.size() - 1).contains("Invalid"), "bad arguments are reported, not thrown");
         g.runCommand("/help");
         check(g.chatLog.get(g.chatLog.size() - 1).contains("/weather"), "listed in /help");
      });
      test("the version strings follow the build", () -> {
         check(Game.VERSION_STR.matches("[0-9]+\\.[0-9]+"), "VERSION_STR is a version: " + Game.VERSION_STR);
         check(Game.TITLE_STR.endsWith("Update " + Game.VERSION_STR), "title string");
      });
   }
}
