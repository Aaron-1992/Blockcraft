package bc;

import static bc.GameTests.check;
import static bc.GameTests.eq;
import static bc.GameTests.test;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;

/**
 * Update 6.18 regressions: the Advanced Video settings, the biome preview and colour key, the new panoramas,
 * redstone state pruning and the frame-spike / memory fixes. Headless: no OpenGL, no display.
 */
final class Tests618 {
   private Tests618() {
   }

   static File tmp;

   static void run() {
      try {
         tmp = Files.createTempDirectory("bc618").toFile();
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
      test("6.18: advanced video settings survive options.txt", Tests618::persistence);
      test("6.18: the debug switch reveals the Advanced tab and its widgets", Tests618::advancedTab);
      test("6.18: every biome has a colour and Create New World shows a key", Tests618::biomePreview);
      test("6.18: three panorama presets plus Cycle", Tests618::panoramas);
      test("6.18: primitive redstone tables", Tests618::packed);
      test("6.18: redstone state is pruned when a chunk unloads", Tests618::redstonePruning);
      test("6.18: pending redstone deadlines and burnout survive a save", Tests618::redstonePersistence);
      test("6.18: collision scratch is reused and particles are capped", Tests618::allocation);
      test("6.18: chunk saves are asynchronous and saveAll is bounded", Tests618::saves);
   }

   // --------------------------------------------------------------- settings
   static void persistence() throws Exception {
      File dir = new File(tmp, "opt1");
      dir.mkdirs();
      Options fresh = new Options(dir);
      eq(false, fresh.debugVideo, "debug video is off by default");
      eq(0, fresh.panorama, "Classic panorama by default");
      eq(2048, Screens.SHADOW_RES[fresh.shadowRes], "2048 shadow map by default");
      eq(48, Screens.CLOUD_STEPS[fresh.cloudSteps], "48 cloud steps by default");
      eq(500, fresh.particleBudget, "500 particles by default");
      eq(true, fresh.asyncSaves, "async saves on by default");
      eq(1200, fresh.autosaveTicks, "timed autosave on by default");

      Options o = new Options(dir);
      o.debugVideo = true;
      o.panorama = 2;
      o.shadowRes = 0;
      o.cloudRes = 2;
      o.cloudSteps = 0;
      o.bloom = 0;
      o.shadowInterval = 3;
      o.shadowMaxAge = 400;
      o.particleBudget = 17;
      o.animatedMips = false;
      o.genThreads = 6;
      o.meshWorkers = 3;
      o.chunkCacheMb = 777;
      o.unloadBudget = 9;
      o.autosaveTicks = 3600;
      o.asyncSaves = false;
      o.save();

      Options back = new Options(dir);
      back.load();
      eq(true, back.debugVideo, "debug video persisted");
      eq(2, back.panorama, "panorama persisted");
      eq(0, back.shadowRes, "shadow map persisted");
      eq(2, back.cloudRes, "cloud resolution persisted");
      eq(0, back.cloudSteps, "cloud steps persisted");
      eq(0, back.bloom, "bloom persisted");
      eq(3, back.shadowInterval, "shadow interval persisted");
      eq(400, back.shadowMaxAge, "max shadow age persisted");
      eq(17, back.particleBudget, "particle cap persisted");
      eq(false, back.animatedMips, "mipmaps persisted");
      eq(6, back.genThreads, "gen threads persisted");
      eq(3, back.meshWorkers, "mesh workers persisted");
      eq(777, back.chunkCacheMb, "chunk cache persisted");
      eq(9, back.unloadBudget, "unload budget persisted");
      eq(3600, back.autosaveTicks, "autosave persisted");
      eq(false, back.asyncSaves, "async saves persisted");

      // hand-edited values are clamped, not trusted
      Files.write(new File(dir, "options.txt").toPath(),
         ("shadowRes=99\ncloudSteps=-4\nparticleBudget=99999\nunloadBudget=0\npanorama=42\n").getBytes("UTF-8"));
      Options clamp = new Options(dir);
      clamp.load();
      eq(2, clamp.shadowRes, "a too-large shadow index is clamped");
      eq(0, clamp.cloudSteps, "a negative cloud-step index is clamped");
      eq(2000, clamp.particleBudget, "the particle cap is clamped");
      eq(1, clamp.unloadBudget, "the unload budget is clamped");
      eq(3, clamp.panorama, "the panorama id is clamped");

      Screens.resetAdvanced(back);
      eq(2, back.shadowRes, "reset restores the shadow map");
      eq(500, back.particleBudget, "reset restores the particle cap");
      eq(true, back.asyncSaves, "reset restores async saves");
   }

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

   static void advancedTab() throws Exception {
      // A tall GUI so every one of the 28 scrolling rows is actually drawn (rows outside the viewport are skipped).
      Game g = screen(343, 1000);
      eq(7, Screens.tabs(g.opt).length, "seven focused tabs before the debug switch");
      g.opt.debugVideo = true;
      eq(8, Screens.tabs(g.opt).length, "the debug switch adds the eighth tab");
      eq("Advanced", Screens.tabs(g.opt)[Screens.ADV_TAB], "the tab is called Advanced");

      g.ui.optTab = Screens.ADV_TAB;
      Screens.options(g);
      check(button(g, "Shadow Map"), "the shadow-map size is configurable in game");
      check(button(g, "Cloud Steps"), "the cloud raymarch step count is configurable in game");
      check(button(g, "Cloud Resolution"), "the cloud target resolution is configurable in game");
      check(button(g, "Bloom"), "bloom is configurable in game");
      check(button(g, "Particle Cap"), "the particle cap is configurable in game");
      check(button(g, "Chunk Cache"), "the chunk cache is configurable in game");
      check(button(g, "Chunk Gen Threads"), "worker counts are configurable in game");
      check(button(g, "Async Chunk Saves"), "async saves are configurable in game");
      check(button(g, "Autosave"), "the autosave interval is configurable in game");
      check(button(g, "Render Distance"), "the standard video settings are here too");
      check(button(g, "Max FPS"), "the frame rate limit is here too");

      // the advanced rows really change the persisted options
      for (Game.Btn b : g.btns) {
         if (b.label != null && b.label.startsWith("Shadow Map") && b.slider != null) {
            b.slider.accept(0.0F);
            break;
         }
      }
      eq(0, g.opt.shadowRes, "the shadow-map slider changes the option");

      g.opt.debugVideo = false;
      eq(7, Screens.tabs(g.opt).length, "turning the switch off hides the tab again");
      g.ui.optTab = 0;
      Screens.options(g); // must still draw without the fifth tab
      check(button(g, "Debug Video"), "the switch itself is on the Video tab");
      check(button(g, "Panorama"), "the panorama choice is on the Video tab");
   }

   // --------------------------------------------------------------- biomes
   static void biomePreview() throws Exception {
      for (int id = 0; id < Biomes.N; id++) {
         check((WorldPreview.color(id) & 0xFFFFFF) != 0, "biome " + id + " (" + Biomes.name(id) + ") is not black");
      }
      check(WorldPreview.color(Biomes.COLD_OCEAN) != WorldPreview.color(Biomes.OCEAN), "cold ocean is its own colour");
      check(WorldPreview.color(Biomes.LUKEWARM_OCEAN) != WorldPreview.color(Biomes.WARM_OCEAN), "lukewarm ocean is its own colour");
      check(WorldPreview.color(Biomes.DEEP_FROZEN_OCEAN) != 0, "deep frozen ocean is coloured");
      check((WorldPreview.color(999) & 0xFFFFFF) != 0, "an unknown future biome is never black");

      WorldSettings ws = new WorldSettings();
      WorldPreview p = new WorldPreview(96, 60, 20, true);
      check(p.update(1234L, ws), "the first preview is drawn");
      boolean black = false;
      for (int px : p.img.px) {
         if (px == 0xFF000000) black = true;
      }
      check(!black, "no black squares in the map");
      check(p.distinctBiomes() > 0, "the preview counted some biomes");
      check(p.topBiomes(8).length > 0, "the legend has entries");
      check(!p.legendText(8).isEmpty(), "the legend names the biomes");
      check(p.hasBiome(p.topBiomes(1)[0]), "the most common biome is reported present");

      WorldSettings b = new WorldSettings();
      b.aquatic = false;
      check(p.update(1234L, b), "switching the aquatic generator redraws the map");
      b.structures = false;
      check(p.update(1234L, b), "switching structures redraws the map");
      check(!p.update(1234L, b), "an unchanged key does not redraw");

      // the real Create New World screen (tall enough to show the new switch + colour key)
      Game g = screen(343, 360);
      Screens.newWorld(g);
      check(button(g, "New Oceans"), "the aquatic generation switch is on Create New World");
      check(g.ui.preview != null && g.ui.preview.distinctBiomes() > 0, "the screen drew a real preview");
      for (Game.Btn btn : g.btns) {
         if (btn.label != null && btn.label.startsWith("New Oceans")) {
            btn.action.run();
            break;
         }
      }
      eq(false, g.newSettings.aquatic, "the switch changes the world settings");
   }

   // --------------------------------------------------------------- panoramas
   static void panoramas() throws Exception {
      eq(4, Panorama.COUNT, "three presets plus Cycle");
      eq("Classic", Panorama.label(0), "Classic is the first preset");
      eq("Coral Coast", Panorama.label(1), "Coral Coast is a new panorama");
      eq("Snowy Peaks", Panorama.label(2), "Snowy Peaks is a new panorama");
      eq("Cycle", Panorama.label(3), "Cycle is the fourth choice");
      eq(Panorama.SEED, Panorama.PRESETS[0].seed, "Classic keeps the original seed");
      eq(Panorama.TURN, Panorama.PRESETS[0].turn, "Classic keeps the original turn speed");
      check(Panorama.PRESETS[1].seed != Panorama.SEED, "Coral Coast has its own seed");
      check(Panorama.PRESETS[2].type == WorldSettings.AMPLIFIED, "Snowy Peaks uses amplified terrain");
      check(Panorama.PRESETS[1].bias != 0 && Panorama.PRESETS[2].bias != 0, "new presets bias the vista search");

      Game g = new Game();
      g.opt = new Options(new File(tmp, "pano"));
      eq(Panorama.PRESETS[0], Panorama.resolve(g), "the default is Classic");
      g.opt.panorama = 3;
      Panorama.Preset a = Panorama.resolve(g);
      Panorama.Preset b = Panorama.resolve(g);
      check(a != null && b != null && a != b, "Cycle picks a different preset on each visit");

      // the new searches still find a real, finite place to stand
      for (int i = 1; i < Panorama.PRESETS.length; i++) {
         Panorama.Preset pr = Panorama.PRESETS[i];
         long t0 = System.nanoTime();
         Gen gen = Dim.makeGen(Dim.OVERWORLD, pr.seed, pr.settings());
         double[] v = Panorama.pick(gen, pr);
         long ms = (System.nanoTime() - t0) / 1000000L;
         check(ms < 8000L, pr.name + " finds a view in " + ms + " ms");
         eq(5, v.length, pr.name + " vista has five numbers");
         boolean finite = true;
         for (double d : v) finite &= !Double.isNaN(d) && !Double.isInfinite(d);
         check(finite, pr.name + " vista is finite");
         Gen.Col c = gen.column((int)Math.floor(v[1]), (int)Math.floor(v[3]));
         check(c.h >= Gen.SEA + 3, pr.name + " stands on dry land");
      }

      Panorama p = new Panorama(g);
      long sky = p.skyTime();
      check(sky >= 1400L && sky <= 5000L, "Classic keeps the morning sky");
      p.preset = Panorama.PRESETS[1];
      check(p.skyTime() != sky || Panorama.PRESETS[1].skyShift == 0L, "Coral Coast has a warmer sky");
   }

   // --------------------------------------------------------------- redstone
   static void packed() {
      Packed.Set set = new Packed.Set();
      for (int i = 0; i < 500; i++) {
         check(set.add(1000L + i), "set adds " + i);
      }
      eq(500, set.size(), "the set grew to 500 entries");
      for (int i = 0; i < 500; i++) {
         check(set.contains(1000L + i), "set still contains " + i);
      }
      check(!set.contains(999L), "an absent key is absent");
      check(!set.add(1000L), "a duplicate add returns false");
      set.clear();
      eq(0, set.size(), "clear empties the set");
      check(!set.contains(1000L), "clear removed the key");

      Packed.Map map = new Packed.Map();
      for (int i = 0; i < 500; i++) map.put(7000L + i, i * 3);
      for (int i = 0; i < 500; i++) eq(i * 3, map.get(7000L + i), "map value " + i);
      eq(-1, map.get(123L), "an absent key gives -1");
      map.put(7000L, 42);
      eq(42, map.get(7000L), "put overwrites");
      map.clear();
      eq(-1, map.get(7000L), "clear empties the map");

      Packed.IntQueue q = new Packed.IntQueue();
      for (int i = 0; i < 200; i++) q.add(i);
      for (int i = 0; i < 200; i++) eq(i, q.poll(), "int queue order " + i);
      check(q.isEmpty(), "the queue is empty");

      Packed.CoordQueue c = new Packed.CoordQueue();
      for (int i = 0; i < 200; i++) c.add(i, i + 1, i + 2);
      for (int i = 0; i < 200; i++) {
         eq(i, c.x(), "coord x " + i);
         eq(i + 1, c.y(), "coord y " + i);
         eq(i + 2, c.z(), "coord z " + i);
         c.poll();
      }
      check(c.isEmpty(), "the coordinate queue is empty");
   }

   static void redstonePruning() throws Exception {
      Game g = GameTests.newGame(0);
      World w = g.world;
      int y = GameTests.GROUND + 1;
      long inside = World.pack(8, y, 8);
      long outside = World.pack(40, y, 40);
      Chunk c = w.chunk(0, 0);
      check(c != null, "the test world has chunk 0,0");
      RedstoneLogic.schedule(w, 8, y, 8, RedstoneLogic.TORCH, 0, 20);
      RedstoneLogic.schedule(w, 40, y, 40, RedstoneLogic.TORCH, 0, 20);
      w.redstone.sensors.add(inside);
      w.redstone.sensors.add(outside);
      w.redstone.comparators.add(inside);
      w.redstone.burnout.put(inside, w.tickCount + 100L);
      w.redstone.extendedAt.put(inside, w.tickCount);
      eq(2, w.redstone.queue.size(), "two events scheduled");

      RedstoneLogic.unloaded(w, c);
      eq(1, w.redstone.queue.size(), "only the event in the other chunk survives");
      eq(1, w.redstone.pending.size(), "pending deadlines were pruned with it");
      check(!w.redstone.sensors.contains(inside), "the unloaded sensor is gone");
      check(w.redstone.sensors.contains(outside), "the other sensor stays");
      check(w.redstone.comparators.isEmpty(), "the unloaded comparator is gone");
      check(w.redstone.burnout.isEmpty(), "the unloaded burnout record is gone");
      check(w.redstone.extendedAt.isEmpty(), "the unloaded piston timestamp is gone");
      w.shutdown();
   }

   static void redstonePersistence() throws Exception {
      Game g = GameTests.newGame(0);
      World w = g.world;
      int y = GameTests.GROUND + 1;
      RedstoneLogic.schedule(w, 8, y, 8, RedstoneLogic.REPEAT, 1, 7);
      w.redstone.burnout.put(World.pack(9, y, 8), w.tickCount + 50L);
      java.util.Properties p = new java.util.Properties();
      RedstoneLogic.save(w, p);
      check(!p.getProperty("redstone.events", "").isEmpty(), "pending events are written");
      check(!p.getProperty("redstone.burnout", "").isEmpty(), "burnout deadlines are written");

      World w2 = new World(1234L, null);
      w2.game = new Game();
      RedstoneLogic.load(w2, p);
      eq(1, w2.redstone.pending.size(), "the pending event came back");
      eq(1, w2.redstone.queue.size(), "the event queue came back");
      check(w2.redstone.queue.peek().due() - w2.tickCount >= 1L, "the delay is relative to the new world's clock");
      check(w2.redstone.burnout.getOrDefault(World.pack(9, y, 8), 0L) > w2.tickCount, "the torch is still burnt out");
      w2.shutdown();
      w.shutdown();
   }

   // --------------------------------------------------------------- allocations
   static void allocation() throws Exception {
      Game g = GameTests.newGame(0);
      g.opt = new Options(new File(tmp, "alloc"));
      double[] box = new double[]{8.2, GameTests.GROUND + 1, 8.2, 8.8, GameTests.GROUND + 2.8, 8.8};
      double[] res = new double[3];
      double[] first = Phys.scratchArray();
      int cap = Phys.scratchCapacity();
      for (int i = 0; i < 100; i++) {
         Phys.move(g.world, box, 0.01, 0.0, 0.01, res);
      }
      check(first == Phys.scratchArray(), "the collision buffer is the same object after 100 moves");
      eq(cap, Phys.scratchCapacity(), "the collision buffer did not grow");
      check(Phys.scratchCapacity() >= 384, "the scratch buffer has room for boxes");

      g.opt.particleBudget = 3;
      for (int i = 0; i < 20; i++) g.flameParticle(8.5, GameTests.GROUND + 1.5, 8.5, i % 2 == 0);
      check(g.particles.size() <= 3, "the particle cap holds: " + g.particles.size());
      g.opt.particleBudget = 0;
      int before = g.particles.size();
      g.flameParticle(8.5, GameTests.GROUND + 1.5, 8.5, false);
      eq(before, g.particles.size(), "a zero cap adds no particles");

      g.opt.genThreads = 4;
      World.setThreadTuning(g.opt);
      eq(4, World.genThreads(), "the generation thread setting is applied");
      eq(4, World.genThreads(g.opt), "the per-options helper agrees");
      check(World.maxLoadedChunks(128) > 64, "the default chunk cache allows a normal view distance");
      check(World.maxLoadedChunks(256) > World.maxLoadedChunks(128), "a bigger cache allows more chunks");
      g.opt.genThreads = 0;
      World.setThreadTuning(g.opt);
      eq(World.autoGenThreads(), World.genThreads(), "Auto falls back to the CPU count");
      g.world.shutdown();
   }

   // --------------------------------------------------------------- saves
   static void saves() throws Exception {
      File dir = new File(tmp, "saves");
      dir.mkdirs();
      World w = new World(5L, dir);
      int[] cxs = {4, 5, -3};
      for (int cx : cxs) {
         Chunk c = new Chunk(cx, 7);
         c.set(2, 80, 3, 1500);
         c.modified = true;
         w.chunks.put(World.key(cx, 7), c);
      }
      w.saveAll(); // bounded, incremental walk
      check(!w.savePending, "saveAll finished in one synchronous pass");
      w.shutdown(); // waits for the async writers
      for (int cx : cxs) {
         File f = new File(dir, "c." + cx + ".7.gz");
         check(f.exists() && f.length() > 0, "chunk " + cx + " reached the disk");
      }

      // a second world with the synchronous switch still writes its file
      File dir2 = new File(tmp, "saves-sync");
      dir2.mkdirs();
      World w2 = new World(9L, dir2);
      w2.game = new Game();
      w2.game.opt = new Options(new File(tmp, "saves-sync-opt"));
      w2.game.opt.asyncSaves = false;
      Chunk c = new Chunk(1, 1);
      c.set(1, 70, 1, 77);
      w2.chunks.put(World.key(1, 1), c);
      w2.saveChunk(c);
      w2.shutdown();
      check(new File(dir2, "c.1.1.gz").exists(), "the synchronous save path still works");
   }
}
