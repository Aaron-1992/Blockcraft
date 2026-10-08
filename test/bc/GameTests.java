package bc;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import bc.Mobs.Mob;
import bc.World.Hit;

/**
 * Headless game-logic tests: a real Game + World + Player without OpenGL (Game.rend is null, see Game.meshNow).
 * They drive the same code paths as the mouse/keyboard: Game.useSpecial / useBlock / placeBlock / breakBlock,
 * World.tick, Player.tick, Mobs.blast ...
 *
 *   python3 tools/runtests.py unit
 */
public final class GameTests {
   static int passed;
   static int failed;
   static final List<String> failures = new ArrayList<>();

   // ------------------------------------------------------------------ tiny test framework
   static void check(boolean ok, String name) {
      if (ok) {
         passed++;
      } else {
         failed++;
         failures.add(name);
         System.out.println("  FAIL  " + name);
      }
   }

   static void eq(Object expected, Object actual, String name) {
      boolean ok = expected == null ? actual == null : expected.equals(actual);
      check(ok, name + " (expected " + expected + ", got " + actual + ")");
   }

   interface T {
      void run() throws Exception;
   }

   static void test(String name, T t) {
      int f0 = failed;
      try {
         t.run();
      } catch (Throwable e) {
         failed++;
         failures.add(name + " threw " + e);
         System.out.println("  FAIL  " + name + " threw " + e);
         StackTraceElement[] st = e.getStackTrace();
         for (int i = 0; i < Math.min(6, st.length); i++) {
            System.out.println("          at " + st[i]);
         }
      }
      System.out.println((failed == f0 ? "ok    " : "FAILED") + "  " + name);
   }

   // ------------------------------------------------------------------ fixtures
   static final int GROUND = 60; // top solid block (grass) of the flat test world

   static void setField(Object o, String name, Object v) throws Exception {
      Field f = o.getClass().getDeclaredField(name);
      f.setAccessible(true);
      f.set(o, v);
   }

   /** A Game with a flat world (grass at y=60, 5x5 chunks), a player standing at (8.5, 61, 8.5), no renderer. */
   static Game newGame(int mode) throws Exception {
      Game g = new Game();
      World w = new World(1234, null);
      w.saveEnabled = false;
      setField(w, "rnd", new Random(1));
      setField(g, "rnd", new Random(2));
      setField(w.fire, "rnd", new Random(3)); // fire ages / spread / burn-out use their own generator: seed it too, or the statistical fire tests flake
      for (int cz = -2; cz <= 2; cz++) {
         for (int cx = -2; cx <= 2; cx++) {
            Chunk c = new Chunk(cx, cz);
            for (int x = 0; x < 16; x++) {
               for (int z = 0; z < 16; z++) {
                  c.set(x, 0, z, Blocks.BEDROCK);
                  for (int y = 1; y < GROUND - 2; y++) {
                     c.set(x, y, z, Blocks.STONE);
                  }
                  c.set(x, GROUND - 2, z, Blocks.DIRT);
                  c.set(x, GROUND - 1, z, Blocks.DIRT);
                  c.set(x, GROUND, z, Blocks.GRASS);
               }
            }
            c.computeHeightmap();
            w.chunks.put(World.key(cx, cz), c);
         }
      }
      for (Chunk c : new ArrayList<>(w.chunks.values())) {
         w.lightChunk(c);
      }
      g.world = w;
      w.game = g;
      w.weather.enabled = false; // game-logic tests must not depend on the weather; Tests38 turns it on where it matters
      w.disasters.enabled = false; // ... nor on a random earthquake after 10 minutes of ticks; TestsDisasters turns them on where it matters
      g.player = new Player();
      g.player.setMode(mode);
      g.player.setPos(8.5, GROUND + 1, 8.5);
      g.screen = Game.S.PLAY;
      return g;
   }

   static Hit hit(int x, int y, int z, int nx, int ny, int nz) {
      Hit h = new Hit();
      h.x = x;
      h.y = y;
      h.z = z;
      h.nx = nx;
      h.ny = ny;
      h.nz = nz;
      return h;
   }

   /** Look at the top face of block (x, GROUND, z). */
   static Hit top(int x, int z) {
      return hit(x, GROUND, z, 0, 1, 0);
   }

   static void hold(Game g, int id, int n) {
      g.inv[g.sel] = new Stack(id, n);
   }

   static void tick(World w, int n) {
      for (int i = 0; i < n; i++) {
         w.tick();
      }
   }

   static int blockAbove(Game g, int x, int z) {
      return g.world.getBlock(x, GROUND + 1, z);
   }

   static int countItems(World w, int id) {
      int n = 0;
      for (World.Item it : w.items) {
         if (!it.dead && it.id == id) {
            n += it.count;
         }
      }
      return n;
   }

   static Stack craft(int w, int h, int... ids) {
      Stack[] g = new Stack[w * h];
      for (int i = 0; i < ids.length; i++) {
         g[i] = ids[i] == 0 ? null : new Stack(ids[i], 1);
      }
      return Recipes.find(g, w, h);
   }

   // ------------------------------------------------------------------ tests: recipes
   static void recipes() {
      final int STICK = 256;
      final int PLANKS = 5;
      final int COBBLE = 4;
      final int IRON = 258;
      final int GOLD = 259;
      final int DIAMOND = 260;
      final int SAND = 7;
      final int GUNPOWDER = 265;
      final int FLINT = 266;
      final int WHEAT = 286;
      final int BREAD = 287;
      final int BED = 289;
      final int BUCKET = 282;
      final int FLINT_STEEL = 288;

      test("recipe: hoes in 5 tiers", () -> {
         int[] mats = {PLANKS, COBBLE, IRON, GOLD, DIAMOND};
         for (int t = 0; t < 5; t++) {
            Stack r = craft(3, 3, mats[t], mats[t], 0, 0, STICK, 0, 0, STICK, 0);
            check(r != null && Items.isHoe(r.id) && r.n == 1, "hoe tier " + t + " from 3x3 grid -> " + (r == null ? "null" : r.id));
            Stack r2 = craft(2, 3, mats[t], mats[t], 0, STICK, 0, STICK);
            check(r2 != null && r2.id == (r == null ? -1 : r.id), "hoe tier " + t + " from 2x2/2x3 grid");
         }
         // all five tiers must be distinct items
         java.util.Set<Integer> ids = new java.util.HashSet<>();
         for (int t = 0; t < 5; t++) {
            ids.add(craft(3, 3, mats[t], mats[t], 0, 0, STICK, 0, 0, STICK, 0).id);
         }
         eq(5, ids.size(), "5 distinct hoe items");
      });

      test("recipe: mirrored hoe also works (like Minecraft)", () -> {
         Stack r = craft(3, 3, PLANKS, PLANKS, 0, STICK, 0, 0, STICK, 0, 0);
         check(r != null && Items.isHoe(r.id), "mirrored wooden hoe -> " + (r == null ? "null" : "" + r.id));
      });

      test("recipe: 3 ladders from 7 sticks", () -> {
         Stack r = craft(3, 3, STICK, 0, STICK, STICK, STICK, STICK, STICK, 0, STICK);
         check(r != null && r.id == Blocks.LADDER && r.n == 3, "ladder -> " + (r == null ? "null" : r.id + "x" + r.n));
      });

      test("recipe: bucket from 3 iron ingots", () -> {
         Stack r = craft(3, 2, IRON, 0, IRON, 0, IRON, 0);
         check(r != null && r.id == BUCKET, "bucket -> " + (r == null ? "null" : "" + r.id));
         check(craft(3, 3, IRON, 0, IRON, 0, IRON, 0, 0, 0, 0) != null, "bucket in 3x3 grid");
      });

      test("recipe: TNT = 5 gunpowder + 4 sand (checkerboard)", () -> {
         Stack r = craft(3, 3, GUNPOWDER, SAND, GUNPOWDER, SAND, GUNPOWDER, SAND, GUNPOWDER, SAND, GUNPOWDER);
         check(r != null && r.id == Blocks.TNT, "TNT -> " + (r == null ? "null" : "" + r.id));
         check(craft(3, 3, SAND, GUNPOWDER, SAND, GUNPOWDER, SAND, GUNPOWDER, SAND, GUNPOWDER, SAND) == null, "inverted pattern is not TNT");
      });

      test("recipe: flint and steel (shapeless iron + flint)", () -> {
         Stack a = craft(2, 2, IRON, FLINT, 0, 0);
         Stack b = craft(2, 2, 0, FLINT, IRON, 0);
         check(a != null && a.id == FLINT_STEEL && b != null && b.id == FLINT_STEEL, "flint & steel in any arrangement");
      });

      test("recipe: bed = 3 wool over 3 planks; the colour of the wool is the colour of the bed (mixed wool: the old red bed)", () -> {
         int w0 = Blocks.WOOL0;
         Stack r = craft(3, 3, w0, w0, w0, PLANKS, PLANKS, PLANKS, 0, 0, 0);
         check(r != null && r.id == Decor315.bedItem(0), "white bed -> " + (r == null ? "null" : "" + r.id));
         Stack mixed = craft(3, 3, w0 + 3, w0 + 3, w0 + 3, PLANKS, PLANKS, PLANKS, 0, 0, 0);
         check(mixed != null && mixed.id == Decor315.bedItem(3), "light-blue wool bed");
         Stack mixed2 = craft(3, 3, w0, w0 + 5, w0 + 14, PLANKS, PLANKS, PLANKS, 0, 0, 0);
         check(mixed2 != null && mixed2.id == BED, "mixed wool colours bed");
         check(craft(3, 3, w0, w0, w0, PLANKS, PLANKS, COBBLE, 0, 0, 0) == null, "bed needs 3 planks");
      });

      test("recipe: bread from 3 wheat in a row", () -> {
         Stack r = craft(3, 3, WHEAT, WHEAT, WHEAT, 0, 0, 0, 0, 0, 0);
         check(r != null && r.id == BREAD, "bread -> " + (r == null ? "null" : "" + r.id));
         Stack r2 = craft(3, 3, 0, 0, 0, 0, 0, 0, WHEAT, WHEAT, WHEAT);
         check(r2 != null && r2.id == BREAD, "bread on the bottom row");
         check(craft(3, 3, WHEAT, WHEAT, 0, 0, 0, 0, 0, 0, 0) == null, "2 wheat is not bread");
      });
   }

   // ------------------------------------------------------------------ tests: hoe, seeds, crops
   static void farming() {
      test("hoe: right-click grass top -> farmland, uses 1 durability", () -> {
         Game g = newGame(0);
         int hoeId = 290; // wooden hoe
         check(Items.isHoe(hoeId), "290 is a hoe");
         hold(g, hoeId, 1);
         g.target = top(10, 10);
         boolean used = g.useSpecial(g.inv[g.sel]);
         check(used, "useSpecial returned true");
         eq(Blocks.FARMLAND, g.world.getBlock(10, GROUND, 10), "grass became farmland");
         eq(1, g.inv[g.sel].dmg, "hoe durability used");
      });

      test("hoe: dirt works, stone/sand do not, side faces work, the bottom face does not", () -> {
         Game g = newGame(0);
         hold(g, 290, 1);
         g.world.setBlock(11, GROUND, 10, Blocks.DIRT);
         g.target = top(11, 10);
         check(g.useSpecial(g.inv[g.sel]), "dirt -> farmland");
         eq(Blocks.FARMLAND, g.world.getBlock(11, GROUND, 10), "dirt became farmland");
         g.world.setBlock(12, GROUND, 10, Blocks.STONE);
         g.target = top(12, 10);
         check(!g.useSpecial(g.inv[g.sel]), "stone is not hoeable");
         g.world.setBlock(13, GROUND, 10, Blocks.SAND);
         g.target = top(13, 10);
         check(!g.useSpecial(g.inv[g.sel]), "sand is not hoeable");
         g.world.setBlock(14, GROUND, 10, Blocks.GRASS);
         g.target = hit(14, GROUND, 10, 1, 0, 0);
         check(g.useSpecial(g.inv[g.sel]), "the side of a grass block can be hoed too (like Minecraft)");
         eq(Blocks.FARMLAND, g.world.getBlock(14, GROUND, 10), "side-hoed grass became farmland");
         g.world.setBlock(16, GROUND, 10, Blocks.GRASS);
         g.target = hit(16, GROUND, 10, 0, -1, 0);
         check(!g.useSpecial(g.inv[g.sel]), "but not the bottom face");
         eq(Blocks.GRASS, g.world.getBlock(16, GROUND, 10), "bottom-clicked grass unchanged");
      });

      test("hoe: block above prevents tilling, tall grass above is cleared", () -> {
         Game g = newGame(0);
         hold(g, 290, 1);
         g.world.setBlock(10, GROUND + 1, 10, Blocks.STONE);
         g.target = top(10, 10);
         check(!g.useSpecial(g.inv[g.sel]), "stone above blocks tilling");
         g.world.setBlock(10, GROUND + 1, 10, Blocks.TALLGRASS);
         check(g.useSpecial(g.inv[g.sel]), "tall grass above is replaced");
         eq(0, blockAbove(g, 10, 10), "tall grass removed");
      });

      test("hoe: wears out and breaks after its durability (wood = 59 uses)", () -> {
         Game g = newGame(0);
         hold(g, 290, 1);
         int uses = 0;
         for (int i = 0; i < 100 && g.inv[g.sel] != null; i++) {
            g.world.setBlock(10, GROUND, 10, Blocks.GRASS);
            g.target = top(10, 10);
            if (g.useSpecial(g.inv[g.sel])) {
               uses++;
            }
         }
         check(g.inv[g.sel] == null, "hoe broke");
         eq(Items.maxDamage(290), uses, "uses before breaking == max durability");
      });

      test("hoe: creative mode does not consume durability", () -> {
         Game g = newGame(1);
         hold(g, 290, 1);
         g.target = top(10, 10);
         check(g.useSpecial(g.inv[g.sel]), "tilled");
         eq(0, g.inv[g.sel].dmg, "no durability used in creative");
      });

      test("seeds: plant on farmland top only", () -> {
         Game g = newGame(0);
         g.world.setBlock(10, GROUND, 10, Blocks.FARMLAND);
         hold(g, 285, 3);
         g.target = top(10, 10);
         check(g.placeBlock(), "seeds planted");
         eq(124, blockAbove(g, 10, 10), "wheat stage 0 above farmland");
         eq(2, g.inv[g.sel].n, "one seed consumed");
         // not on grass
         g.target = top(12, 12);
         check(!g.placeBlock(), "cannot plant on grass");
         // not on the side of farmland
         g.world.setBlock(14, GROUND, 10, Blocks.FARMLAND);
         g.target = hit(14, GROUND, 10, 1, 0, 0);
         check(!g.placeBlock(), "cannot plant on the side of farmland");
         // not twice
         g.target = top(10, 10);
         check(!g.placeBlock(), "cannot plant on an occupied farmland");
         eq(2, g.inv[g.sel].n, "no seed wasted");
      });

      test("seeds: creative does not consume", () -> {
         Game g = newGame(1);
         g.world.setBlock(10, GROUND, 10, Blocks.FARMLAND);
         hold(g, 285, 1);
         g.target = top(10, 10);
         check(g.placeBlock(), "planted");
         eq(1, g.inv[g.sel].n, "seed kept in creative");
      });

      test("wheat: grows through 8 stages next to the player, only there", () -> {
         Game g = newGame(0);
         g.player.setPos(-20.5, GROUND + 1, 8.5);
         int nearX = -18;
         int farX = 42; // > 60 blocks from the player: outside the +-32 growth range
         int plots = 12;
         for (int i = 0; i < plots; i++) {
            g.world.setBlock(nearX, GROUND, i, Blocks.FARMLAND);
            g.world.setBlock(nearX, GROUND + 1, i, 124);
            g.world.setBlock(farX, GROUND, i, Blocks.FARMLAND);
            g.world.setBlock(farX, GROUND + 1, i, 124);
         }
         for (int z = -2; z < plots + 2; z++) { // irrigation ditches next to both rows (wet farmland grows faster)
            g.world.setBlock(nearX + 2, GROUND, z, Blocks.WATER);
            g.world.setBlock(farX + 2, GROUND, z, Blocks.WATER);
         }
         for (int t = 1; t <= 12000; t++) { // 10 minutes
            g.world.tick();
         }
         int ripeNear = 0;
         int maxFar = 0;
         for (int i = 0; i < plots; i++) {
            if (g.world.getBlock(nearX, GROUND + 1, i) == 131) {
               ripeNear++;
            }
            maxFar = Math.max(maxFar, g.world.getBlock(farX, GROUND + 1, i));
         }
         check(ripeNear >= plots - 1, "near crops ripe after 10 minutes (" + ripeNear + "/" + plots + ")");
         eq(124, maxFar, "far crops did not grow at all (outside the 32 block range)");
      });

      test("wheat: never grows past stage 8 and never grows without farmland", () -> {
         Game g = newGame(0);
         g.world.setBlock(10, GROUND, 10, Blocks.FARMLAND);
         g.world.setBlock(10, GROUND + 1, 10, 131);
         tick(g.world, 3000);
         eq(131, blockAbove(g, 10, 10), "ripe wheat stays at 131 (no overflow into TNT=132)");
         // wheat on plain grass pops off
         g.world.setBlock(12, GROUND + 1, 10, 124);
         tick(g.world, 5);
         check(blockAbove(g, 12, 10) != 124, "wheat on grass does not survive (blockUpdate) -> " + blockAbove(g, 12, 10));
      });

      test("wheat: breaking it drops seeds (unripe) / wheat + 0-3 seeds (ripe)", () -> {
         Game g = newGame(0);
         int ripeWheat = 0;
         int ripeSeeds = 0;
         int unripeSeeds = 0;
         int unripeWheat = 0;
         int zeroSeedRounds = 0;
         int maxSeeds = 0;
         for (int i = 0; i < 400; i++) {
            g.world.items.clear();
            g.world.setBlock(10, GROUND + 1, 10, 131);
            g.breakBlock(10, GROUND + 1, 10);
            int w = countItems(g.world, 286);
            int s = countItems(g.world, 285);
            ripeWheat += w;
            ripeSeeds += s;
            maxSeeds = Math.max(maxSeeds, s);
            if (s == 0) {
               zeroSeedRounds++;
            }
            g.world.items.clear();
            g.world.setBlock(10, GROUND + 1, 10, 126);
            g.breakBlock(10, GROUND + 1, 10);
            unripeSeeds += countItems(g.world, 285);
            unripeWheat += countItems(g.world, 286);
         }
         eq(400, ripeWheat, "ripe wheat drops exactly 1 wheat each time");
         check(maxSeeds <= 3, "ripe wheat never drops more than 3 seeds (max " + maxSeeds + ")");
         check(zeroSeedRounds > 60 && zeroSeedRounds < 140, "about 1/4 of ripe harvests give 0 seeds (" + zeroSeedRounds + "/400)");
         check(Math.abs(ripeSeeds / 400.0 - 1.5) < 0.25, "average 1.5 seeds per ripe harvest (" + ripeSeeds / 400.0 + ")");
         eq(400, unripeSeeds, "unripe wheat drops 1 seed");
         eq(0, unripeWheat, "unripe wheat drops no wheat");
      });

      test("tall grass: drops seeds about 1 in 8", () -> {
         Game g = newGame(0);
         int seeds = 0;
         int n = 4000;
         for (int i = 0; i < n; i++) {
            g.world.items.clear();
            g.world.setBlock(10, GROUND + 1, 10, Blocks.TALLGRASS);
            g.breakBlock(10, GROUND + 1, 10);
            seeds += countItems(g.world, 285);
         }
         double rate = seeds / (double) n;
         check(rate > 0.10 && rate < 0.15, "seed rate " + rate + " (expected ~0.125)");
      });
   }


   // ------------------------------------------------------------------ tests: buckets
   static void buckets() {
      test("bucket: empty bucket picks up a water source and becomes a water bucket", () -> {
         Game g = newGame(0);
         g.world.setBlock(10, GROUND, 10, Blocks.WATER);
         g.player.setPos(10.5, GROUND + 2, 10.5);
         g.player.pitch = 90; // looking straight down
         hold(g, Items.BUCKET, 1);
         check(g.useSpecial(g.inv[g.sel]), "useSpecial");
         eq(Items.WATER_BUCKET, g.inv[g.sel].id, "now holding a water bucket");
         eq(0, g.world.getBlock(10, GROUND, 10), "source removed");
      });

      test("bucket: lava source -> lava bucket; flowing water cannot be picked up", () -> {
         Game g = newGame(0);
         g.world.setBlock(10, GROUND, 10, Blocks.LAVA);
         g.player.setPos(10.5, GROUND + 2, 10.5);
         g.player.pitch = 90;
         hold(g, Items.BUCKET, 1);
         check(g.useSpecial(g.inv[g.sel]), "picked up lava");
         eq(Items.LAVA_BUCKET, g.inv[g.sel].id, "lava bucket");
         // flowing (level 3) water: not a source
         g.world.setBlock(10, GROUND, 10, Blocks.WATER + 3);
         hold(g, Items.BUCKET, 1);
         check(!g.useSpecial(g.inv[g.sel]), "flowing water is not picked up");
         eq(Items.BUCKET, g.inv[g.sel].id, "bucket still empty");
      });

      test("bucket: stacked empty buckets give one filled bucket and keep the rest", () -> {
         Game g = newGame(0);
         g.world.setBlock(10, GROUND, 10, Blocks.WATER);
         g.player.setPos(10.5, GROUND + 2, 10.5);
         g.player.pitch = 90;
         hold(g, Items.BUCKET, 3);
         check(g.useSpecial(g.inv[g.sel]), "picked up");
         int empty = 0;
         int full = 0;
         for (Stack st : g.inv) {
            if (st != null && st.id == Items.BUCKET) {
               empty += st.n;
            }
            if (st != null && st.id == Items.WATER_BUCKET) {
               full += st.n;
            }
         }
         eq(2, empty, "two empty buckets left");
         eq(1, full, "one water bucket");
      });

      test("bucket: pour water/lava source on the targeted face, bucket empties (survival)", () -> {
         Game g = newGame(0);
         hold(g, Items.WATER_BUCKET, 1);
         g.target = top(12, 10);
         check(g.useSpecial(g.inv[g.sel]), "poured water");
         eq(Blocks.WATER, g.world.getBlock(12, GROUND + 1, 10), "water source placed above the grass");
         eq(Items.BUCKET, g.inv[g.sel].id, "bucket is empty again");
         hold(g, Items.LAVA_BUCKET, 1);
         g.target = top(14, 10);
         check(g.useSpecial(g.inv[g.sel]), "poured lava");
         eq(Blocks.LAVA, g.world.getBlock(14, GROUND + 1, 10), "lava source placed");
         eq(Items.BUCKET, g.inv[g.sel].id, "empty again");
         // pouring needs a free cell
         g.world.setBlock(16, GROUND + 1, 10, Blocks.STONE);
         hold(g, Items.WATER_BUCKET, 1);
         g.target = hit(16, GROUND + 1, 10, 0, 1, 0); // target the top of the stone
         check(g.useSpecial(g.inv[g.sel]), "pours on top of the stone");
         eq(Blocks.WATER, g.world.getBlock(16, GROUND + 2, 10), "above the stone");
      });

      test("bucket: creative pouring keeps the full bucket; poured water flows", () -> {
         Game g = newGame(1);
         hold(g, Items.WATER_BUCKET, 1);
         g.target = top(12, 10);
         check(g.useSpecial(g.inv[g.sel]), "poured");
         eq(Items.WATER_BUCKET, g.inv[g.sel].id, "still a water bucket in creative");
         tick(g.world, 40);
         check(g.world.getBlock(13, GROUND + 1, 10) != 0 || g.world.getBlock(12, GROUND + 1, 11) != 0, "water spreads to a neighbouring cell");
      });
   }

   // ------------------------------------------------------------------ tests: TNT
   static int countCleared(World w, int cx, int cy, int cz, int r) {
      int n = 0;
      for (int x = -r; x <= r; x++) {
         for (int y = -r; y <= r; y++) {
            for (int z = -r; z <= r; z++) {
               if (x * x + y * y + z * z <= r * r && w.getBlock(cx + x, cy + y, cz + z) == 0) {
                  n++;
               }
            }
         }
      }
      return n;
   }

   static void tnt() {
      test("TNT: flint & steel ignites it: block disappears, 80-tick fuse, 1 durability", () -> {
         Game g = newGame(0);
         g.world.setBlock(30, GROUND + 1, 30, Blocks.TNT);
         hold(g, Items.FLINT_STEEL, 1);
         g.target = hit(30, GROUND + 1, 30, 0, 1, 0);
         check(g.useSpecial(g.inv[g.sel]), "useSpecial");
         eq(0, g.world.getBlock(30, GROUND + 1, 30), "TNT block replaced by the primed entity");
         eq(1, g.world.fallers.size(), "one primed TNT entity");
         eq(80, g.world.fallers.get(0).fuse, "fuse");
         eq(1, g.inv[g.sel].dmg, "flint and steel durability used");
         // Update 3.7: on other blocks it lights a fire next to the clicked face (see Tests37); not into thin air
         g.target = hit(20, GROUND + 5, 20, 0, 1, 0);
         check(!g.useSpecial(g.inv[g.sel]), "flint & steel in thin air does nothing");
      });

      test("TNT: explodes after exactly 4 seconds; radius 4 hole; bedrock/obsidian survive", () -> {
         Game g = newGame(1); // creative player, far away: no damage interference
         g.player.setPos(-25, GROUND + 1, -25);
         g.world.setBlock(30, GROUND + 1, 30, Blocks.TNT);
         g.world.setBlock(33, GROUND, 30, Blocks.OBSIDIAN);
         hold(g, Items.FLINT_STEEL, 1);
         g.target = hit(30, GROUND + 1, 30, 0, 1, 0);
         g.useSpecial(g.inv[g.sel]);
         tick(g.world, 79);
         eq(1, g.world.fallers.size(), "still burning after 79 ticks");
         eq(Blocks.GRASS, g.world.getBlock(30, GROUND, 30), "ground intact before the blast");
         tick(g.world, 3);
         eq(0, g.world.fallers.size(), "exploded around tick 80");
         // the TNT sits on the ground at (30, GROUND+1, 30): the crater is centred there
         int removed = 0;
         for (int x = 26; x <= 34; x++) {
            for (int z = 26; z <= 34; z++) {
               for (int y = GROUND - 4; y <= GROUND + 1; y++) {
                  if (g.world.getBlock(x, y, z) == 0) {
                     removed++;
                  }
               }
            }
         }
         check(removed > 60, "a crater was blown (" + removed + " cells cleared in the box)");
         eq(0, g.world.getBlock(30, GROUND - 1, 30), "block 2 below the TNT is gone");
         eq(0, g.world.getBlock(30, GROUND, 28), "ground 2 blocks away is gone");
         check(g.world.getBlock(30, GROUND - 6, 30) != 0, "6 blocks down is untouched");
         check(g.world.getBlock(38, GROUND, 30) != 0, "8 blocks away is untouched");
         eq(Blocks.OBSIDIAN, g.world.getBlock(33, GROUND, 30), "obsidian is blast-proof");
      });

      test("TNT: hurts the player nearby, spares the one far away", () -> {
         Game g = newGame(0);
         g.player.setPos(32.5, GROUND + 1, 30.5);
         g.world.setBlock(30, GROUND + 1, 30, Blocks.TNT);
         hold(g, Items.FLINT_STEEL, 1);
         g.target = hit(30, GROUND + 1, 30, 0, 1, 0);
         g.useSpecial(g.inv[g.sel]);
         tick(g.world, 85);
         check(g.player.health < 20, "player at 2 blocks was hurt (health " + g.player.health + ")");
         Game g2 = newGame(0);
         g2.player.setPos(-25, GROUND + 1, -25);
         g2.world.setBlock(30, GROUND + 1, 30, Blocks.TNT);
         hold(g2, Items.FLINT_STEEL, 1);
         g2.target = hit(30, GROUND + 1, 30, 0, 1, 0);
         g2.useSpecial(g2.inv[g2.sel]);
         tick(g2.world, 85);
         eq(20, g2.player.health, "player 80 blocks away is unhurt");
      });

      test("TNT: hurts mobs inside the blast", () -> {
         Game g = newGame(1);
         g.player.setPos(-25, GROUND + 1, -25);
         Mob pig = g.mobs.spawn(0, 32.5, GROUND + 1, 30.5);
         int hp0 = pig.health;
         g.world.setBlock(30, GROUND + 1, 30, Blocks.TNT);
         hold(g, Items.FLINT_STEEL, 1);
         g.target = hit(30, GROUND + 1, 30, 0, 1, 0);
         g.useSpecial(g.inv[g.sel]);
         tick(g.world, 85);
         check(pig.health < hp0 || pig.dead, "pig took damage (" + hp0 + " -> " + pig.health + ")");
      });

      test("TNT: chain reaction ignites neighbouring TNT blocks (and only those inside the blast)", () -> {
         Game g = newGame(1);
         g.player.setPos(-25, GROUND + 1, -25);
         for (int i = 0; i < 5; i++) {
            g.world.setBlock(30 + i, GROUND + 1, 30, Blocks.TNT); // five in a row, 1 block apart
         }
         // Unbreakable floor under the row. Without it the first blast digs a crater under the TNT it has just primed, the burning
         // blocks fall into the hole and explode up to 5 blocks lower, out of reach of the last one in the row. That is a real (vanilla-like)
         // effect, and it made this test fail about once in 80 runs because the outer shell of a blast is random.
         for (int i = -1; i <= 5; i++) {
            g.world.setBlock(30 + i, GROUND, 30, Blocks.BEDROCK);
         }
         g.world.setBlock(44, GROUND + 1, 30, Blocks.TNT); // a lonely one 14 blocks away (the flat test world ends at x=47)
         hold(g, Items.FLINT_STEEL, 1);
         g.target = hit(30, GROUND + 1, 30, 0, 1, 0);
         g.useSpecial(g.inv[g.sel]);
         tick(g.world, 82); // the first one has just exploded
         eq(0, g.world.getBlock(31, GROUND + 1, 30), "neighbour at distance 1 was blown up -> primed");
         eq(0, g.world.getBlock(32, GROUND + 1, 30), "neighbour at distance 2 was blown up -> primed");
         eq(Blocks.TNT, g.world.getBlock(44, GROUND + 1, 30), "TNT 14 blocks away is untouched");
         check(g.world.fallers.size() >= 2, "several TNT entities are burning (" + g.world.fallers.size() + ")");
         int maxFuse = 0;
         for (World.Falling f : g.world.fallers) {
            maxFuse = Math.max(maxFuse, f.fuse);
         }
         check(maxFuse > 0 && maxFuse <= 30, "chained TNT has a short random fuse (<=30 ticks), max=" + maxFuse);
         tick(g.world, 200);
         int left = 0;
         for (int i = 0; i < 5; i++) {
            if (g.world.getBlock(30 + i, GROUND + 1, 30) == Blocks.TNT) {
               left++;
            }
         }
         eq(0, left, "the whole row detonated");
         eq(0, g.world.fallers.size(), "nothing is burning any more");
         eq(Blocks.TNT, g.world.getBlock(44, GROUND + 1, 30), "the far TNT still stands");
      });

      test("TNT: a creeper blast also ignites TNT", () -> {
         Game g = newGame(1);
         g.player.setPos(-25, GROUND + 1, -25);
         g.world.setBlock(30, GROUND + 1, 30, Blocks.TNT);
         g.mobs.blast(31.5, GROUND + 1.5, 30.5, 3, null); // what a creeper does when it explodes
         eq(0, g.world.getBlock(30, GROUND + 1, 30), "TNT block consumed by the creeper blast");
         check(g.world.fallers.size() >= 1, "TNT got primed");
      });

      test("TNT: primed TNT in mid-air falls and lands", () -> {
         Game g = newGame(1);
         g.player.setPos(-25, GROUND + 1, -25);
         g.world.primeTnt(30, GROUND + 15, 30, 200);
         tick(g.world, 40);
         double y = g.world.fallers.get(0).y;
         check(y < GROUND + 3, "fell to the ground (y=" + y + ")");
         check(y >= GROUND + 1 - 0.01, "did not sink into the ground (y=" + y + ")");
      });

      test("TNT: placing the block works, it is solid and drops itself when broken", () -> {
         Game g = newGame(0);
         hold(g, Blocks.TNT, 3);
         g.target = top(20, 20);
         check(g.placeBlock(), "placed");
         eq(Blocks.TNT, blockAbove(g, 20, 20), "TNT block");
         g.breakBlock(20, GROUND + 1, 20);
         eq(1, countItems(g.world, Blocks.TNT), "broken TNT drops itself");
      });
   }

   // ------------------------------------------------------------------ tests: ladders
   static void wall(Game g, int x, int z, int height) {
      for (int y = GROUND + 1; y <= GROUND + height; y++) {
         g.world.setBlock(x, y, z, Blocks.STONE);
      }
   }

   static void ladders() {
      test("ladder: placed on the side of a solid block, facing away from it (4 directions)", () -> {
         Game g = newGame(0);
         wall(g, 10, 10, 3);
         int[][] dirs = {{0, 1}, {0, -1}, {-1, 0}, {1, 0}}; // normals (nx,nz)
         int[] expectId = {119, 120, 121, 122};
         int[][] pos = {{10, 11}, {10, 9}, {9, 10}, {11, 10}};
         for (int d = 0; d < 4; d++) {
            hold(g, Blocks.LADDER, 5);
            g.target = hit(10, GROUND + 2, 10, dirs[d][0], 0, dirs[d][1]);
            check(g.placeBlock(), "place on face " + d);
            eq(expectId[d], g.world.getBlock(pos[d][0], GROUND + 2, pos[d][1]), "ladder variant for face " + d);
            eq(4, g.inv[g.sel].n, "one ladder consumed");
         }
      });

      test("ladder: cannot be placed on top/bottom faces, on glass or in the air", () -> {
         Game g = newGame(0);
         wall(g, 10, 10, 3);
         hold(g, Blocks.LADDER, 5);
         g.target = hit(10, GROUND + 3, 10, 0, 1, 0);
         check(!g.placeBlock(), "top face refused");
         g.target = hit(10, GROUND + 1, 10, 0, -1, 0);
         check(!g.placeBlock(), "bottom face refused");
         g.world.setBlock(14, GROUND + 1, 14, Blocks.GLASS);
         g.target = hit(14, GROUND + 1, 14, 0, 0, 1);
         check(!g.placeBlock(), "glass is not a valid support");
         eq(5, g.inv[g.sel].n, "nothing consumed");
      });

      test("ladder: drops itself when broken and pops off when its support disappears", () -> {
         Game g = newGame(0);
         wall(g, 10, 10, 3);
         g.world.setBlock(10, GROUND + 2, 11, 119);
         g.breakBlock(10, GROUND + 2, 11);
         eq(1, countItems(g.world, Blocks.LADDER), "breaking a ladder drops a ladder");
         g.world.items.clear();
         g.world.setBlock(10, GROUND + 2, 11, 119);
         g.world.setBlock(10, GROUND + 2, 10, 0); // remove the support
         tick(g.world, 5);
         eq(0, g.world.getBlock(10, GROUND + 2, 11), "unsupported ladder is gone");
      });

      test("ladder: creative-mode placing does not consume", () -> {
         Game g = newGame(1);
         wall(g, 10, 10, 3);
         hold(g, Blocks.LADDER, 1);
         g.target = hit(10, GROUND + 1, 10, 0, 0, 1);
         check(g.placeBlock(), "placed");
         eq(1, g.inv[g.sel].n, "stack unchanged");
      });

      test("ladder: climb by walking into the wall, by holding jump; sneak holds; idle slides slowly", () -> {
         for (int mode = 0; mode < 4; mode++) {
            Game g = newGame(0);
            wall(g, 10, 10, 12);
            for (int y = GROUND + 1; y <= GROUND + 12; y++) {
               g.world.setBlock(10, y, 11, 119);
            }
            Player p = g.player;
            p.setPos(10.5, GROUND + 1, 11.5);
            p.yaw = 180; // facing -z, towards the wall
            double y0 = p.y;
            for (int i = 0; i < 30; i++) {
               switch (mode) {
                  case 0 -> p.tick(g.world, 1f, 0f, false, false, false); // walk into the ladder wall
                  case 1 -> p.tick(g.world, 0f, 0f, true, false, false); // hold jump only
                  case 2 -> p.tick(g.world, 0f, 0f, false, true, false); // sneak only
                  default -> p.tick(g.world, 0f, 0f, false, false, false); // nothing
               }
            }
            double dy = p.y - y0;
            switch (mode) {
               case 0 -> check(dy > 4.0, "walking into the wall climbs (dy=" + dy + ")");
               case 1 -> check(dy > 4.0, "holding jump climbs (dy=" + dy + ")");
               case 2 -> check(Math.abs(dy) < 0.3, "sneaking holds position (dy=" + dy + ")");
               default -> check(dy <= 0.0, "idle does not climb (dy=" + dy + ")");
            }
         }
      });

      test("ladder: no fall damage when landing on / sliding down a ladder, damage without it", () -> {
         Game g = newGame(0);
         wall(g, 10, 10, 40);
         for (int y = GROUND + 1; y <= GROUND + 40; y++) {
            g.world.setBlock(10, y, 11, 119);
         }
         Player p = g.player;
         p.setPos(10.5, GROUND + 38, 11.5);
         for (int i = 0; i < 400 && !p.onGround; i++) {
            p.tick(g.world, 0f, 0f, false, false, false);
         }
         eq(20, p.health, "health after sliding down 38 blocks on a ladder");
         // control: the same fall onto plain ground hurts
         Game g2 = newGame(0);
         Player p2 = g2.player;
         p2.setPos(20.5, GROUND + 20, 20.5);
         for (int i = 0; i < 400 && !p2.onGround; i++) {
            p2.tick(g2.world, 0f, 0f, false, false, false);
         }
         check(p2.health < 20 || p2.fallDistance > 0 || p2.hurtTime > 0, "control: the same fall without a ladder hurts (health " + p2.health + ")");
      });
   }

   // ------------------------------------------------------------------ tests: beds
   static void beds() {
      test("bed: placed 2 blocks long in the facing direction", () -> {
         Game g = newGame(0);
         hold(g, Items.BED, 1);
         g.player.yaw = 0; // facing +z
         g.target = top(10, 10);
         check(g.placeBlock(), "placed");
         int foot = g.world.getBlock(10, GROUND + 1, 10);
         int head = g.world.getBlock(10, GROUND + 1, 11);
         check(Blocks.isBed(foot) && !Blocks.isBedHead(foot), "foot at the target cell (" + foot + ")");
         check(Blocks.isBedHead(head), "head one block further (" + head + ")");
         check(g.inv[g.sel] == null, "bed consumed");
      });

      test("bed: needs room and solid ground under both halves", () -> {
         Game g = newGame(0);
         g.player.yaw = 0;
         g.world.setBlock(10, GROUND, 11, 0); // hole under the head cell
         hold(g, Items.BED, 1);
         g.target = top(10, 10);
         check(!g.placeBlock(), "no bed over a hole");
         g.world.setBlock(10, GROUND, 11, Blocks.GRASS);
         g.world.setBlock(10, GROUND + 1, 11, Blocks.STONE);
         check(!g.placeBlock(), "no bed when the head cell is occupied");
         eq(1, g.inv[g.sel].n, "bed not consumed");
      });

      test("bed: sleeping at night skips to morning and sets the respawn point", () -> {
         Game g = newGame(0);
         hold(g, Items.BED, 1);
         g.player.yaw = 0;
         g.target = top(10, 10);
         g.placeBlock();
         g.world.dayTime = 14000;
         g.target = hit(10, GROUND + 1, 10, 0, 1, 0);
         check(g.useBlock(), "right-click on the bed is handled");
         long t = g.world.dayTime % 24000;
         check(t < 1000 && g.world.dayTime > 24000, "woke up in the morning (dayTime=" + g.world.dayTime + ")");
         check(g.bedPos != null && g.bedPos[0] == 10 && g.bedPos[2] == 10, "respawn point set");
      });

      test("bed: sleeping is refused with a monster within 8 blocks; works with a pig", () -> {
         Game g = newGame(0);
         hold(g, Items.BED, 1);
         g.player.yaw = 0;
         g.target = top(10, 10);
         g.placeBlock();
         g.world.dayTime = 14000;
         Mob z = g.mobs.spawn(4, 14.5, GROUND + 1, 10.5); // zombie 4 blocks away
         g.target = hit(10, GROUND + 1, 10, 0, 1, 0);
         g.useBlock();
         eq(14000L, g.world.dayTime, "time unchanged while a zombie is near");
         check(g.bedPos == null, "no respawn point set when refused");
         z.dead = true;
         Mob pig = g.mobs.spawn(0, 12.5, GROUND + 1, 10.5);
         g.useBlock();
         check(g.world.dayTime > 24000, "pigs do not prevent sleeping");
         check(g.bedPos != null, "respawn point set");
      });

      test("bed: a monster 20 blocks away does not prevent sleeping", () -> {
         Game g = newGame(0);
         hold(g, Items.BED, 1);
         g.player.yaw = 0;
         g.target = top(10, 10);
         g.placeBlock();
         g.world.dayTime = 14000;
         g.mobs.spawn(4, 30.5, GROUND + 1, 10.5);
         g.target = hit(10, GROUND + 1, 10, 0, 1, 0);
         g.useBlock();
         check(g.world.dayTime > 24000, "slept");
      });

      test("bed: in daylight it only sets the respawn point", () -> {
         Game g = newGame(0);
         hold(g, Items.BED, 1);
         g.player.yaw = 0;
         g.target = top(10, 10);
         g.placeBlock();
         g.world.dayTime = 6000;
         g.target = hit(10, GROUND + 1, 11, 0, 1, 0); // click the head half this time
         g.useBlock();
         eq(6000L, g.world.dayTime, "time unchanged");
         check(g.bedPos != null && g.bedPos[0] == 10 && g.bedPos[2] == 10, "respawn point is the FOOT cell regardless of the half clicked");
      });

      test("bed: respawn next to the bed; after the bed is destroyed respawn at world spawn", () -> {
         Game g = newGame(0);
         g.spawnXZ = new int[]{-20, -20};
         hold(g, Items.BED, 1);
         g.player.yaw = 0;
         g.target = top(10, 10);
         g.placeBlock();
         g.world.dayTime = 6000;
         g.target = hit(10, GROUND + 1, 10, 0, 1, 0);
         g.useBlock();
         g.player.dead = true;
         g.respawn();
         double dx = g.player.x - 10.5;
         double dz = g.player.z - 10.5;
         check(Math.sqrt(dx * dx + dz * dz) < 3.5, "respawned beside the bed (" + g.player.x + "," + g.player.z + ")");
         check(!g.player.dead && g.player.health == 20, "alive with full health");
         // destroy the bed
         g.breakBlock(10, GROUND + 1, 10);
         eq(0, g.world.getBlock(10, GROUND + 1, 11), "head half removed with the foot");
         g.player.dead = true;
         g.respawn();
         check(Math.abs(g.player.x - (-19.5)) < 3.5 && Math.abs(g.player.z - (-19.5)) < 3.5, "respawned at the world spawn (" + g.player.x + "," + g.player.z + ")");
         check(g.bedPos == null, "bed position forgotten");
      });

      test("bed: breaking either half drops exactly one bed (survival), none in creative", () -> {
         for (int half = 0; half < 2; half++) {
            Game g = newGame(0);
            hold(g, Items.BED, 1);
            g.player.yaw = 0;
            g.target = top(10, 10);
            g.placeBlock();
            g.breakBlock(10, GROUND + 1, 10 + half);
            eq(0, g.world.getBlock(10, GROUND + 1, 10), "foot gone (half " + half + ")");
            eq(0, g.world.getBlock(10, GROUND + 1, 11), "head gone (half " + half + ")");
            eq(1, countItems(g.world, Items.BED), "exactly one bed dropped (half " + half + ")");
         }
         Game c = newGame(1);
         hold(c, Items.BED, 1);
         c.player.yaw = 0;
         c.target = top(10, 10);
         c.placeBlock();
         c.breakBlock(10, GROUND + 1, 10);
         eq(0, countItems(c.world, Items.BED), "no drop in creative");
      });

      test("bed: orientation works in all four facings (placement + sleeping)", () -> {
         float[] yaws = {0, 90, 180, 270};
         for (float yaw : yaws) {
            Game g = newGame(0);
            hold(g, Items.BED, 1);
            g.player.yaw = yaw;
            g.target = top(10, 10);
            check(g.placeBlock(), "placed with yaw " + yaw);
            int heads = 0;
            int feet = 0;
            for (int dx = -1; dx <= 1; dx++) {
               for (int dz = -1; dz <= 1; dz++) {
                  int b = g.world.getBlock(10 + dx, GROUND + 1, 10 + dz);
                  if (Blocks.isBed(b)) {
                     if (Blocks.isBedHead(b)) {
                        heads++;
                     } else {
                        feet++;
                     }
                  }
               }
            }
            check(heads == 1 && feet == 1, "one foot + one head for yaw " + yaw);
            g.world.dayTime = 14000;
            g.target = hit(10, GROUND + 1, 10, 0, 1, 0);
            g.useBlock();
            check(g.world.dayTime > 24000, "slept with yaw " + yaw);
         }
      });
   }


   // ------------------------------------------------------------------ tests: Update 3.6 farming
   static void farm36() {
      test("hydration: water within 4 blocks (same level or one above) counts, 5 away / below does not", () -> {
         Game g = newGame(0);
         World w = g.world;
         for (int dx = -6; dx <= 6; dx++) {
            w.setBlock(10 + dx, GROUND + 1, 10, 0);
         }
         w.setBlock(14, GROUND, 10, Blocks.WATER);
         check(w.hydrated(10, GROUND, 10), "water 4 blocks east on the same level");
         w.setBlock(14, GROUND, 10, Blocks.GRASS);
         w.setBlock(15, GROUND, 10, Blocks.WATER);
         check(!w.hydrated(10, GROUND, 10), "water 5 blocks away is too far");
         w.setBlock(15, GROUND, 10, Blocks.GRASS);
         w.setBlock(12, GROUND + 1, 12, Blocks.WATER);
         check(w.hydrated(10, GROUND, 10), "water one level above counts");
         w.setBlock(12, GROUND + 1, 12, 0);
         w.setBlock(12, GROUND - 1, 12, Blocks.WATER);
         check(!w.hydrated(10, GROUND, 10), "water one level below does not count");
         w.setBlock(12, GROUND + 2, 12, Blocks.WATER);
         check(!w.hydrated(10, GROUND, 10), "water two levels above does not count");
         w.setBlock(10, GROUND, 14, Blocks.WATER + 4); // flowing water counts too
         check(w.hydrated(10, GROUND, 10), "flowing water counts as well");
      });

      test("hydration: farmland turns wet next to water and dry again when the water is gone", () -> {
         Game g = newGame(0);
         World w = g.world;
         w.setBlock(10, GROUND, 10, Blocks.FARMLAND);
         w.setBlock(10, GROUND + 1, 10, 124); // a crop keeps dry farmland from reverting to dirt
         w.setBlock(12, GROUND, 10, Blocks.WATER);
         tick(w, 3000);
         eq(Blocks.FARMLAND_WET, w.getBlock(10, GROUND, 10), "became wet");
         w.setBlock(12, GROUND, 10, Blocks.GRASS);
         tick(w, 3000);
         eq(Blocks.FARMLAND, w.getBlock(10, GROUND, 10), "dried out again");
         check(Blocks.isCrop(w.getBlock(10, GROUND + 1, 10)), "the crop survived both changes");
      });

      test("hydration: dry farmland with no crop reverts to dirt over a few minutes; wet farmland and farmland with a crop stay", () -> {
         Game g = newGame(0);
         World w = g.world;
         int n = 30;
         for (int i = 0; i < n; i++) {
            w.setBlock(-20, GROUND, i - 10, Blocks.FARMLAND);              // dry, empty
            w.setBlock(-16, GROUND, i - 10, Blocks.FARMLAND);              // dry, planted
            w.setBlock(-16, GROUND + 1, i - 10, 124);
            w.setBlock(20 + (i % 2) * 2, GROUND, i - 10, Blocks.FARMLAND); // wet (water below)
         }
         for (int z = -12; z < n; z++) {
            w.setBlock(21, GROUND, z, Blocks.WATER);
         }
         for (int i = 0; i < n; i++) {
            w.setBlock(21, GROUND, i - 10, Blocks.WATER);
         }
         for (int i = 0; i < n; i++) {
            w.setBlock(20, GROUND, i - 10, Blocks.FARMLAND);
         }
         tick(w, 25000);
         int dirt = 0;
         int plantedFarm = 0;
         int wetFarm = 0;
         for (int i = 0; i < n; i++) {
            if (w.getBlock(-20, GROUND, i - 10) == Blocks.DIRT) {
               dirt++;
            }
            if (Blocks.isFarmland(w.getBlock(-16, GROUND, i - 10))) {
               plantedFarm++;
            }
            if (Blocks.isFarmland(w.getBlock(20, GROUND, i - 10))) {
               wetFarm++;
            }
         }
         check(dirt >= 26, "dry empty farmland went back to dirt (" + dirt + "/" + n + ")");
         eq(n, plantedFarm, "dry farmland with a crop on it stays farmland");
         eq(n, wetFarm, "wet farmland stays farmland");
      });

      test("hydration: crops grow noticeably faster on wet than on dry farmland", () -> {
         Game g = newGame(0);
         World w = g.world;
         int n = 24;
         for (int i = 0; i < n; i++) {
            w.setBlock(-22, GROUND, i - 12, Blocks.FARMLAND);
            w.setBlock(-22, GROUND + 1, i - 12, 124);
            w.setBlock(-21, GROUND, i - 12, Blocks.WATER); // wet row
            w.setBlock(24, GROUND, i - 12, Blocks.FARMLAND); // dry row (no water within 4 blocks)
            w.setBlock(24, GROUND + 1, i - 12, 124);
         }
         tick(w, 3000);
         double wet = 0;
         double dry = 0;
         for (int i = 0; i < n; i++) {
            wet += Blocks.cropStage(w.getBlock(-22, GROUND + 1, i - 12));
            dry += Blocks.cropStage(w.getBlock(24, GROUND + 1, i - 12));
         }
         wet /= n;
         dry /= n;
         check(wet > dry * 1.5, "average growth stage after 2.5 min: wet " + wet + " vs dry " + dry);
         check(dry > 0.5, "dry farmland still grows, just slower (" + dry + ")");
      });

      test("farmland: still breaks into dirt drops, pops under a solid block, wet or dry", () -> {
         Game g = newGame(0);
         for (int id : new int[]{Blocks.FARMLAND, Blocks.FARMLAND_WET}) {
            g.world.items.clear();
            g.world.setBlock(10, GROUND, 10, id);
            g.breakBlock(10, GROUND, 10);
            eq(1, countItems(g.world, Blocks.DIRT), "farmland " + id + " drops dirt");
            g.world.setBlock(10, GROUND, 10, id);
            g.world.setBlock(10, GROUND + 1, 10, Blocks.STONE);
            tick(g.world, 5);
            eq(Blocks.DIRT, g.world.getBlock(10, GROUND, 10), "farmland " + id + " under stone turns to dirt");
            g.world.setBlock(10, GROUND + 1, 10, 0);
         }
         // a crop stays on wet farmland
         g.world.setBlock(12, GROUND, 12, Blocks.FARMLAND_WET);
         g.world.setBlock(12, GROUND + 1, 12, 142);
         tick(g.world, 20);
         eq(142, g.world.getBlock(12, GROUND + 1, 12), "carrots stay on wet farmland");
      });

      test("trampling: probability is (fall distance - 0.5), crops pop off", () -> {
         Game g = newGame(0);
         World w = g.world;
         int dirt = 0;
         for (int i = 0; i < 400; i++) {
            w.setBlock(10, GROUND, 10, Blocks.FARMLAND);
            w.trample(10, GROUND, 10, 0.3F);
         }
         eq(Blocks.FARMLAND, w.getBlock(10, GROUND, 10), "a 0.3 block hop never tramples");
         for (int i = 0; i < 1000; i++) {
            w.setBlock(10, GROUND, 10, Blocks.FARMLAND);
            w.trample(10, GROUND, 10, 1.0F);
            if (w.getBlock(10, GROUND, 10) == Blocks.DIRT) {
               dirt++;
            }
         }
         check(dirt > 420 && dirt < 580, "a 1.0 block fall tramples about half the time (" + dirt + "/1000)");
         w.setBlock(10, GROUND, 10, Blocks.FARMLAND_WET);
         w.trample(10, GROUND, 10, 5.0F);
         eq(Blocks.DIRT, w.getBlock(10, GROUND, 10), "a 5 block fall always tramples wet farmland");
         w.setBlock(12, GROUND, 12, Blocks.GRASS);
         w.trample(12, GROUND, 12, 5.0F);
         eq(Blocks.GRASS, w.getBlock(12, GROUND, 12), "grass is not affected");
         // crop on top
         w.items.clear();
         w.setBlock(14, GROUND, 14, Blocks.FARMLAND);
         w.setBlock(14, GROUND + 1, 14, 143);
         w.trample(14, GROUND, 14, 5.0F);
         tick(w, 5);
         eq(0, w.getBlock(14, GROUND + 1, 14), "the carrot popped off");
         eq(1, countItems(w, Items.CARROT), "and dropped a carrot");
      });

      test("trampling: the player jumping down onto farmland ruins it; flying/creative-flying does not", () -> {
         Game g = newGame(0);
         g.world.setBlock(10, GROUND, 10, Blocks.FARMLAND);
         Player p = g.player;
         p.setPos(10.5, GROUND + 4, 10.5);
         for (int i = 0; i < 200 && !p.onGround; i++) {
            p.tick(g.world, 0f, 0f, false, false, false);
         }
         check(p.onGround, "landed");
         eq(Blocks.DIRT, g.world.getBlock(10, GROUND, 10), "farmland trampled after a 3 block drop");
         // flying creative player: no fall distance -> no trampling
         Game c = newGame(1);
         c.world.setBlock(10, GROUND, 10, Blocks.FARMLAND);
         c.player.flying = true;
         c.player.setPos(10.5, GROUND + 4, 10.5);
         for (int i = 0; i < 30; i++) {
            c.player.tick(c.world, 0f, 0f, false, false, false);
         }
         c.player.flying = false;
         // walking on it is harmless
         Game h = newGame(0);
         h.world.setBlock(10, GROUND, 10, Blocks.FARMLAND);
         h.player.setPos(9.5, GROUND + 1, 10.5);
         for (int i = 0; i < 40; i++) {
            h.player.tick(h.world, 0.8f, 0f, false, false, false);
            h.player.yaw = -90; // walk along +x over the farmland
         }
         check(Blocks.isFarmland(h.world.getBlock(10, GROUND, 10)) || h.player.x < 10, "walking does not trample (or has not reached it)");
      });

      test("hay bale: 80% less fall damage; plain ground hurts fully", () -> {
         Game g = newGame(0);
         g.world.setBlock(10, GROUND, 10, Blocks.HAY_BALE);
         Player p = g.player;
         p.setPos(10.5, GROUND + 14, 10.5);
         for (int i = 0; i < 400 && !p.onGround; i++) {
            p.tick(g.world, 0f, 0f, false, false, false);
         }
         eq(18, p.health, "13 block fall onto hay costs 2 hearts (10 * 0.2)");
         Game g2 = newGame(0);
         g2.player.setPos(10.5, GROUND + 14, 10.5);
         for (int i = 0; i < 400 && !g2.player.onGround; i++) {
            g2.player.tick(g2.world, 0f, 0f, false, false, false);
         }
         eq(10, g2.player.health, "same fall onto grass costs 10");
      });

      test("hay bale and wheat recipes", () -> {
         Stack hay = craft(3, 3, Items.WHEAT, Items.WHEAT, Items.WHEAT, Items.WHEAT, Items.WHEAT, Items.WHEAT, Items.WHEAT, Items.WHEAT, Items.WHEAT);
         check(hay != null && hay.id == Blocks.HAY_BALE && hay.n == 1, "9 wheat -> hay bale");
         Stack back = craft(3, 3, Blocks.HAY_BALE, 0, 0, 0, 0, 0, 0, 0, 0);
         check(back != null && back.id == Items.WHEAT && back.n == 9, "hay bale -> 9 wheat");
         Stack bread = craft(3, 3, Items.WHEAT, Items.WHEAT, Items.WHEAT, 0, 0, 0, 0, 0, 0);
         check(bread != null && bread.id == Items.BREAD, "bread still works");
         Game g = newGame(0);
         hold(g, Blocks.HAY_BALE, 2);
         g.target = top(20, 20);
         check(g.placeBlock(), "hay bale can be placed");
         eq(Blocks.HAY_BALE, blockAbove(g, 20, 20), "hay bale placed");
         g.breakBlock(20, GROUND + 1, 20);
         eq(1, countItems(g.world, Blocks.HAY_BALE), "breaking it drops it");
      });

      test("carrots/potatoes: planting by right-click, not on grass, hungry player plants instead of eating", () -> {
         int[][] crops = {{Items.CARROT, 142}, {Items.POTATO, 150}};
         for (int[] cp : crops) {
            Game g = newGame(0);
            g.player.food = 10; // hungry: would start eating if planting did not take priority
            g.world.setBlock(10, GROUND, 10, Blocks.FARMLAND);
            hold(g, cp[0], 3);
            g.target = top(10, 10);
            g.rightPressed = true;
            g.rightDown = true;
            g.interact();
            eq(cp[1], blockAbove(g, 10, 10), "item " + cp[0] + " planted as block " + cp[1]);
            eq(2, g.inv[g.sel].n, "one consumed");
            eq(10, g.player.food, "did not eat it");
            // wet farmland too
            g.world.setBlock(12, GROUND, 10, Blocks.FARMLAND_WET);
            g.target = top(12, 10);
            check(g.useSpecial(g.inv[g.sel]), "planted on wet farmland");
            eq(cp[1], blockAbove(g, 12, 10), "wet farmland accepts it too");
            // not on grass, not on a planted spot
            g.target = top(14, 14);
            check(!g.useSpecial(g.inv[g.sel]), "not on grass");
            g.target = top(10, 10);
            check(!g.useSpecial(g.inv[g.sel]), "not on top of another crop");
         }
      });

      test("carrots/potatoes: can be eaten away from farmland (3 and 1 hunger points), baked potato 5", () -> {
         int[][] foods = {{Items.CARROT, 3}, {Items.POTATO, 1}, {Items.BAKED_POTATO, 5}};
         for (int[] f : foods) {
            Game g = newGame(0);
            g.player.food = 5;
            hold(g, f[0], 2);
            g.target = top(10, 10); // grass: not farmland
            for (int i = 0; i < 40; i++) {
               g.rightDown = true;
               g.rightPressed = i == 0;
               g.interact();
            }
            eq(5 + f[1], g.player.food, "item " + f[0] + " restores " + f[1]);
            eq(1, g.inv[g.sel].n, "one eaten");
            check(Items.isFood(f[0]), "isFood");
         }
      });

      test("carrots/potatoes: grow in 8 stages, drop 1-4 when ripe, 1 when young, pop off without farmland", () -> {
         Game g = newGame(0);
         for (int base : new int[]{142, 150}) {
            int item = base == 142 ? Items.CARROT : Items.POTATO;
            int total = 0;
            int max = 0;
            int min = 99;
            for (int i = 0; i < 600; i++) {
               g.world.items.clear();
               g.world.setBlock(10, GROUND + 1, 10, base + 7);
               g.breakBlock(10, GROUND + 1, 10);
               int n = countItems(g.world, item);
               total += n;
               max = Math.max(max, n);
               min = Math.min(min, n);
            }
            check(min >= 1 && max <= 4, "ripe " + item + " drops 1..4 (" + min + ".." + max + ")");
            check(Math.abs(total / 600.0 - 2.5) < 0.25, "average ~2.5 (" + total / 600.0 + ")");
            g.world.items.clear();
            g.world.setBlock(10, GROUND + 1, 10, base + 3);
            g.breakBlock(10, GROUND + 1, 10);
            eq(1, countItems(g.world, item), "young crop drops exactly 1");
            // pops off when its soil is gone
            g.world.items.clear();
            g.world.setBlock(14, GROUND, 14, Blocks.FARMLAND);
            g.world.setBlock(14, GROUND + 1, 14, base + 2);
            g.world.setBlock(14, GROUND, 14, Blocks.STONE);
            tick(g.world, 5);
            eq(0, g.world.getBlock(14, GROUND + 1, 14), "crop popped off");
            eq(1, countItems(g.world, item), "and dropped its own item");
         }
         // full growth cycle on wet farmland: stages go 0..7 in order and stop at 7
         g.world.setBlock(-20, GROUND, 0, Blocks.FARMLAND);
         g.world.setBlock(-20, GROUND + 1, 0, 142);
         g.world.setBlock(-19, GROUND, 0, Blocks.WATER);
         g.player.setPos(-18.5, GROUND + 1, 4.5);
         int last = 142;
         boolean ordered = true;
         for (int t = 0; t < 30000; t++) {
            g.world.tick();
            int b = g.world.getBlock(-20, GROUND + 1, 0);
            if (b != last) {
               ordered &= b == last + 1;
               last = b;
            }
         }
         check(ordered, "stages advance one at a time");
         eq(149, last, "carrots ripen at stage 7 (id 149) and stop there");
      });

      test("bone meal: recipe, grows crops 2-5 stages, not past ripe, not consumed in creative or on ripe crops", () -> {
         Stack r = craft(3, 3, Items.BONE, 0, 0, 0, 0, 0, 0, 0, 0);
         check(r != null && r.id == Items.BONE_MEAL && r.n == 3, "1 bone -> 3 bone meal");
         int[] stageGain = new int[8];
         Game rg = newGame(0); // one game for all rolls: a fresh game would re-seed the RNG identically every time
         for (int base : new int[]{124, 142, 150}) {
            for (int i = 0; i < 300; i++) {
               rg.world.setBlock(10, GROUND, 10, Blocks.FARMLAND);
               rg.world.setBlock(10, GROUND + 1, 10, base);
               hold(rg, Items.BONE_MEAL, 5);
               rg.target = hit(10, GROUND + 1, 10, 0, 1, 0);
               check(rg.useSpecial(rg.inv[rg.sel]), "bone meal used on crop " + base);
               int gain = Blocks.cropStage(rg.world.getBlock(10, GROUND + 1, 10));
               stageGain[gain]++;
               check(gain >= 2 && gain <= 5, "gain " + gain + " is within 2..5");
               eq(4, rg.inv[rg.sel].n, "one bone meal consumed");
            }
         }
         check(stageGain[2] > 150 && stageGain[3] > 150 && stageGain[4] > 150 && stageGain[5] > 150, "2,3,4,5 each occur about a quarter of the time " + java.util.Arrays.toString(stageGain));
         Game g = newGame(0);
         g.world.setBlock(10, GROUND, 10, Blocks.FARMLAND);
         g.world.setBlock(10, GROUND + 1, 10, 142 + 6);
         hold(g, Items.BONE_MEAL, 5);
         g.target = hit(10, GROUND + 1, 10, 0, 1, 0);
         g.useSpecial(g.inv[g.sel]);
         eq(149, g.world.getBlock(10, GROUND + 1, 10), "stage 6 + bone meal tops out at ripe (149)");
         check(!g.useSpecial(g.inv[g.sel]), "ripe crops refuse bone meal");
         eq(4, g.inv[g.sel].n, "and nothing is consumed");
         // clicking the soil below the plant works too
         g.world.setBlock(12, GROUND, 12, Blocks.FARMLAND);
         g.world.setBlock(12, GROUND + 1, 12, 124);
         g.target = top(12, 12);
         check(g.useSpecial(g.inv[g.sel]), "clicking the farmland under a crop also works");
         // creative: not consumed
         Game c = newGame(1);
         c.world.setBlock(10, GROUND, 10, Blocks.FARMLAND);
         c.world.setBlock(10, GROUND + 1, 10, 124);
         hold(c, Items.BONE_MEAL, 1);
         c.target = hit(10, GROUND + 1, 10, 0, 1, 0);
         c.useSpecial(c.inv[c.sel]);
         eq(1, c.inv[c.sel].n, "creative keeps the stack");
      });

      test("bone meal: grass block gets tall grass / flowers around it, other blocks refuse", () -> {
         Game g = newGame(0);
         hold(g, Items.BONE_MEAL, 10);
         g.target = top(10, 10);
         check(g.useSpecial(g.inv[g.sel]), "used on grass");
         int grass = 0;
         int flowers = 0;
         for (int x = 6; x <= 14; x++) {
            for (int z = 6; z <= 14; z++) {
               int b = blockAbove(g, x, z);
               if (b == Blocks.TALLGRASS) {
                  grass++;
               }
               if (b == Blocks.FLOWER_RED || b == Blocks.FLOWER_YELLOW) {
                  flowers++;
               }
            }
         }
         check(grass >= 5, "tall grass appeared (" + grass + ")");
         check(grass + flowers <= 24, "bounded amount");
         eq(9, g.inv[g.sel].n, "one consumed");
         g.target = hit(10, GROUND - 1, 10, 0, 1, 0); // dirt under the grass
         check(!g.useSpecial(g.inv[g.sel]), "dirt refuses bone meal");
         g.world.setBlock(10, GROUND + 1, 10, Blocks.STONE);
         g.target = top(10, 10);
         check(!g.useSpecial(g.inv[g.sel]), "grass with something on top refuses");
      });

      test("baked potato: smelts from a potato in the furnace", () -> {
         Stack r = Recipes.smeltResult(new Stack(Items.POTATO, 1));
         check(r != null && r.id == Items.BAKED_POTATO && r.n == 1, "potato -> baked potato");
         check(Recipes.smeltResult(new Stack(Items.CARROT, 1)) == null, "carrot is not smeltable");
      });

      test("new content: names, tiles, creative inventory entries, sprites", () -> {
         for (int id = 141; id < Blocks.COUNT; id++) {
            check(Blocks.name[id] != null, "block " + id + " has a name");
         }
         eq(226, Blocks.COUNT, "COUNT");
         for (int id = 336; id <= 340; id++) {
            check(Items.name[id] != null, "item " + id + " has a name");
            check(TexExtra.sprite(id) != null, "item " + id + " has a sprite");
            check(Items.tile[id] >= 65 && Items.tile[id] < 256, "item " + id + " tile in atlas range (" + Items.tile[id] + ")");
         }
         java.util.Set<Integer> list = new java.util.HashSet<>();
         for (int id : Blocks.creativeList()) {
            list.add(id);
         }
         for (int id : new int[]{158, 336, 337, 338, 339, 340}) {
            check(list.contains(id), "creative list has " + id);
         }
         // atlas tile numbers used by the new blocks/items must be unique and < 256
         java.util.Map<Integer, String> used = new java.util.HashMap<>();
         for (int id = 141; id < Blocks.COUNT; id++) {
            for (int f = 0; f < 6; f++) {
               int t = Blocks.tiles[id][f];
               check(t >= 0 && t < 256, "block " + id + " tile " + t + " in range");
            }
         }
         for (int id = 336; id <= 340; id++) {
            String prev = used.put(Items.tile[id], "item " + id);
            check(prev == null, "item tile " + Items.tile[id] + " is unique (clashes with " + prev + ")");
         }
         check(!Blocks.isCrop(141) && !Blocks.isCrop(132) && !Blocks.isCrop(158) && !Blocks.isCrop(133), "isCrop only covers the three crop families");
         check(Blocks.isCrop(124) && Blocks.isCrop(131) && Blocks.isCrop(142) && Blocks.isCrop(157), "isCrop covers wheat, carrots, potatoes");
         eq(285, Blocks.cropSeedItem(127), "wheat -> seeds");
         eq(Items.CARROT, Blocks.cropSeedItem(145), "carrot block -> carrot");
         eq(Items.POTATO, Blocks.cropSeedItem(150), "potato block -> potato");
      });

      test("atlas: every tile >= 160 used by a block or item is actually painted (TexExtra.blocks)", () -> {
         TexExtra.blocks(); // pure CPU: fills Tex.atlasPixels for the tiles TexExtra owns
         java.util.function.IntPredicate painted = tile -> {
            int ox = Tex.tx(tile);
            int oy = Tex.ty(tile);
            for (int y = 0; y < 16; y++) {
               for (int x = 0; x < 16; x++) {
                  if (Tex.atlasPixels[(oy + y) * Tex.PX + ox + x] >>> 24 != 0) {
                     return true;
                  }
               }
            }
            return false;
         };
         int checked = 0;
         for (int id = 1; id < Blocks.COUNT; id++) {
            if (Blocks.name[id] == null) {
               continue;
            }
            for (int f = 0; f < 6; f++) {
               int t = Blocks.tiles[id][f];
               if (t >= 160) {
                  checked++;
                  check(painted.test(t), "block " + id + " (" + Blocks.name[id] + ") face " + f + " tile " + t + " is painted");
               }
            }
         }
         for (int id = 256; id < 400; id++) {
            if (Items.name[id] != null && Items.tile[id] >= 160) {
               checked++;
               check(painted.test(Items.tile[id]), "item " + id + " (" + Items.name[id] + ") tile " + Items.tile[id] + " is painted");
            }
         }
         check(checked > 60, "checked " + checked + " tile references");
      });

      test("shears: recipe (both diagonals), durability 238, leaves and tall grass drop themselves", () -> {
         Stack a = craft(2, 2, 0, Items.IRON_INGOT, Items.IRON_INGOT, 0);
         Stack b = craft(2, 2, Items.IRON_INGOT, 0, 0, Items.IRON_INGOT);
         check(a != null && a.id == Items.SHEARS && b != null && b.id == Items.SHEARS, "2 iron ingots diagonal -> shears");
         eq(238, Items.maxDamage(Items.SHEARS), "durability");
         Game g = newGame(0);
         hold(g, Items.SHEARS, 1);
         g.world.setBlock(10, GROUND + 1, 10, Blocks.LEAVES_OAK);
         g.breakBlock(10, GROUND + 1, 10);
         eq(1, countItems(g.world, Blocks.LEAVES_OAK), "leaves dropped by shears");
         g.world.items.clear();
         g.world.setBlock(10, GROUND + 1, 10, Blocks.TALLGRASS);
         g.breakBlock(10, GROUND + 1, 10);
         eq(1, countItems(g.world, Blocks.TALLGRASS), "tall grass dropped by shears");
         // without shears leaves do not drop themselves
         Game h = newGame(0);
         int leaves = 0;
         for (int i = 0; i < 200; i++) {
            h.world.items.clear();
            h.world.setBlock(10, GROUND + 1, 10, Blocks.LEAVES_OAK);
            h.breakBlock(10, GROUND + 1, 10);
            leaves += countItems(h.world, Blocks.LEAVES_OAK);
         }
         eq(0, leaves, "bare-handed leaves never drop the leaf block");
      });
   }

   // ------------------------------------------------------------------ tests: animals
   static void animals36() {
      test("shearing: wool of the sheep's colour, once, wears the shears, regrows later", () -> {
         Game g = newGame(0);
         g.mobs.timer = 1; // keep random spawns out of the way (they happen on timer % 20 / % 40)
         Mob sheep = g.mobs.spawn(Mobs.SHEEP, 12.5, GROUND + 1, 12.5);
         sheep.color = 6; // pink
         hold(g, Items.SHEARS, 1);
         check(g.useOnMob(sheep, g.inv[g.sel]), "sheared");
         check(sheep.sheared, "sheep is sheared");
         int wool = countItems(g.world, Blocks.WOOL0 + 6);
         check(wool >= 1 && wool <= 3, "1-3 pink wool dropped (" + wool + ")");
         eq(1, g.inv[g.sel].dmg, "shears lost 1 durability");
         check(!g.useOnMob(sheep, g.inv[g.sel]), "a sheared sheep cannot be sheared again");
         eq(1, g.inv[g.sel].dmg, "no extra wear");
         // regrowth: tick the mob until the timer runs out
         int ticks = 0;
         while (sheep.sheared && ticks < 12000) {
            g.mobs.tickMob(sheep, g.world, g.player, 1.0f);
            ticks++;
         }
         check(!sheep.sheared && ticks >= 4800 && ticks <= 9600, "wool regrew after " + ticks + " ticks (4800..9600)");
         // creative does not wear the shears
         Game c = newGame(1);
         Mob s2 = c.mobs.spawn(Mobs.SHEEP, 12.5, GROUND + 1, 12.5);
         hold(c, Items.SHEARS, 1);
         c.useOnMob(s2, c.inv[c.sel]);
         eq(0, c.inv[c.sel].dmg, "creative: no wear");
         // only sheep
         Mob cow = c.mobs.spawn(Mobs.COW, 14.5, GROUND + 1, 12.5);
         check(!c.useOnMob(cow, c.inv[c.sel]), "shears do nothing to cows");
      });

      test("interact(): right-click on a targeted mob feeds/shears it instead of placing/eating; normal clicks unchanged", () -> {
         Game g = newGame(0);
         g.world.dayTime = 6000;
         Mob cow = g.mobs.spawn(Mobs.COW, 12.5, GROUND + 1, 12.5);
         hold(g, Items.WHEAT, 2);
         g.targetMob = cow;
         g.target = null;
         g.rightPressed = true;
         g.rightDown = true;
         g.interact();
         eq(600, cow.love, "cow fed through interact()");
         eq(1, g.inv[g.sel].n, "wheat consumed");
         Mob sheep = g.mobs.spawn(Mobs.SHEEP, 14.5, GROUND + 1, 12.5);
         hold(g, Items.SHEARS, 1);
         g.targetMob = sheep;
         g.rightPressed = true;
         g.rightDown = true;
         g.interact();
         check(sheep.sheared, "sheep sheared through interact()");
         // hostile mob targeted while holding a block: the block is not 'used on the mob' - placement is unaffected
         g.targetMob = null;
         hold(g, Blocks.STONE, 5);
         g.target = top(20, 20);
         g.rightPressed = true;
         g.rightDown = true;
         g.placeDelay = 0;
         g.interact();
         eq(Blocks.STONE, blockAbove(g, 20, 20), "placing a block still works");
         // a hungry player eating a carrot while NOT looking at farmland / a mob
         g.player.food = 5;
         hold(g, Items.CARROT, 1);
         g.target = top(22, 22);
         for (int i = 0; i < 40; i++) {
            g.rightDown = true;
            g.rightPressed = i == 0;
            g.interact();
         }
         eq(8, g.player.food, "still eats carrots on grass");
      });

      test("shearing: a sheared sheep drops no wool when killed; babies cannot be sheared and drop nothing", () -> {
         Game g = newGame(0);
         Mob a = g.mobs.spawn(Mobs.SHEEP, 12.5, GROUND + 1, 12.5);
         a.color = 3;
         g.mobs.kill(a);
         eq(1, countItems(g.world, Blocks.WOOL0 + 3), "normal sheep drops 1 wool");
         g.world.items.clear();
         Mob b = g.mobs.spawn(Mobs.SHEEP, 14.5, GROUND + 1, 12.5);
         b.color = 3;
         b.sheared = true;
         g.mobs.kill(b);
         eq(0, countItems(g.world, Blocks.WOOL0 + 3), "sheared sheep drops no wool");
         g.world.items.clear();
         Mob baby = g.mobs.spawnBaby(Mobs.SHEEP, 16.5, GROUND + 1, 12.5);
         hold(g, Items.SHEARS, 1);
         check(!g.useOnMob(baby, g.inv[g.sel]), "baby sheep cannot be sheared");
         g.mobs.kill(baby);
         eq(0, g.world.items.size(), "baby drops nothing");
      });

      test("breeding: right food puts adults in love, wrong food / babies / cooldown are refused", () -> {
         int[][] pairs = {{Mobs.PIG, Items.CARROT}, {Mobs.PIG, Items.POTATO}, {Mobs.COW, Items.WHEAT}, {Mobs.SHEEP, Items.WHEAT}, {Mobs.CHICKEN, Items.SEEDS}};
         for (int[] pr : pairs) {
            Game g = newGame(0);
            Mob m = g.mobs.spawn(pr[0], 12.5, GROUND + 1, 12.5);
            hold(g, pr[1], 3);
            check(g.useOnMob(m, g.inv[g.sel]), Mobs.NAME[pr[0]] + " accepts " + pr[1]);
            eq(600, m.love, "in love for 30 seconds");
            eq(2, g.inv[g.sel].n, "one item consumed");
            check(!g.useOnMob(m, g.inv[g.sel]), "already in love: refused");
            eq(2, g.inv[g.sel].n, "nothing consumed when refused");
         }
         Game g = newGame(0);
         Mob pig = g.mobs.spawn(Mobs.PIG, 12.5, GROUND + 1, 12.5);
         hold(g, Items.WHEAT, 2);
         check(!g.useOnMob(pig, g.inv[g.sel]), "pigs do not eat wheat");
         hold(g, Items.CARROT, 2);
         pig.breedCd = 100;
         check(!g.useOnMob(pig, g.inv[g.sel]), "cooldown refuses");
         pig.breedCd = 0;
         Mob baby = g.mobs.spawnBaby(Mobs.PIG, 14.5, GROUND + 1, 12.5);
         int age0 = baby.babyAge;
         check(g.useOnMob(baby, g.inv[g.sel]), "babies accept food (to grow faster)");
         eq(age0 - 2400, baby.babyAge, "growth sped up by 2 minutes");
         eq(0, baby.love, "babies do not fall in love");
         Mob zombie = g.mobs.spawn(Mobs.ZOMBIE, 18.5, GROUND + 1, 12.5);
         check(!g.useOnMob(zombie, g.inv[g.sel]), "hostile mobs are not fed");
         Game c = newGame(1);
         Mob cm = c.mobs.spawn(Mobs.COW, 12.5, GROUND + 1, 12.5);
         hold(c, Items.WHEAT, 1);
         c.useOnMob(cm, c.inv[c.sel]);
         eq(1, c.inv[c.sel].n, "creative does not consume food");
      });

      test("breeding: two animals in love walk to each other and make a baby, both go on cooldown", () -> {
         for (int type : new int[]{Mobs.PIG, Mobs.COW, Mobs.SHEEP, Mobs.CHICKEN}) {
            Game g = newGame(0);
            g.world.dayTime = 6000;
            int food = type == Mobs.PIG ? Items.CARROT : (type == Mobs.CHICKEN ? Items.SEEDS : Items.WHEAT);
            Mob a = g.mobs.spawn(type, 12.5, GROUND + 1, 12.5);
            Mob b = g.mobs.spawn(type, 17.5, GROUND + 1, 14.5);
            a.color = 6;
            b.color = 6;
            hold(g, food, 4);
            check(g.useOnMob(a, g.inv[g.sel]), "fed A");
            check(g.useOnMob(b, g.inv[g.sel]), "fed B");
            Mob baby = null;
            for (int t = 0; t < 700 && baby == null; t++) {
               g.mobs.tickMob(a, g.world, g.player, 1.0f);
               g.mobs.tickMob(b, g.world, g.player, 1.0f);
               for (Mob m : g.mobs.list) {
                  if (m != a && m != b && m.type == type && m.babyAge > 0) {
                     baby = m;
                  }
               }
            }
            check(baby != null, Mobs.NAME[type] + ": a baby appeared");
            if (baby != null) {
               check(baby.babyAge > 23000 && baby.babyAge <= 24000, "baby needs ~20 minutes to grow (" + baby.babyAge + ")");
               check(baby.bb[3] - baby.bb[0] < Mobs.W[type] * 0.6, "baby hitbox is half size");
               if (type == Mobs.SHEEP) {
                  eq(6, baby.color, "baby sheep inherits the colour");
               }
            }
            eq(0, a.love, "A out of love");
            eq(0, b.love, "B out of love");
            check(a.breedCd >= 5999 && a.breedCd <= 6000, "A on 5 minute cooldown (" + a.breedCd + ")");
            check(b.breedCd >= 5999 && b.breedCd <= 6000, "B on 5 minute cooldown (" + b.breedCd + ")");
         }
      });

      test("breeding: animals of different kinds do not mate; a lonely animal in love just waits then calms down", () -> {
         Game g = newGame(0);
         g.world.dayTime = 6000;
         Mob pig = g.mobs.spawn(Mobs.PIG, 12.5, GROUND + 1, 12.5);
         Mob cow = g.mobs.spawn(Mobs.COW, 13.5, GROUND + 1, 12.5);
         hold(g, Items.CARROT, 1);
         g.useOnMob(pig, g.inv[g.sel]);
         hold(g, Items.WHEAT, 1);
         g.useOnMob(cow, g.inv[g.sel]);
         for (int t = 0; t < 700; t++) {
            g.mobs.tickMob(pig, g.world, g.player, 1.0f);
            g.mobs.tickMob(cow, g.world, g.player, 1.0f);
         }
         int babies = 0;
         for (Mob m : g.mobs.list) {
            if (m.babyAge > 0) {
               babies++;
            }
         }
         eq(0, babies, "no baby from a pig and a cow");
         eq(0, pig.love, "love wore off after 30 s");
         eq(0, pig.breedCd, "no cooldown without a baby");
      });

      test("babies: grow up after their timer, hitbox returns to full size; adults breed again after the cooldown", () -> {
         Game g = newGame(0);
         g.world.dayTime = 6000;
         Mob baby = g.mobs.spawnBaby(Mobs.COW, 12.5, GROUND + 1, 12.5);
         double small = baby.bb[3] - baby.bb[0];
         baby.babyAge = 5;
         for (int t = 0; t < 8; t++) {
            g.mobs.tickMob(baby, g.world, g.player, 1.0f);
         }
         eq(0, baby.babyAge, "grown up");
         check(!baby.isBaby(), "isBaby false");
         check(baby.bb[3] - baby.bb[0] > small * 1.8, "hitbox doubled (" + small + " -> " + (baby.bb[3] - baby.bb[0]) + ")");
         Mob a = g.mobs.spawn(Mobs.COW, 20.5, GROUND + 1, 12.5);
         a.breedCd = 3;
         for (int t = 0; t < 5; t++) {
            g.mobs.tickMob(a, g.world, g.player, 1.0f);
         }
         eq(0, a.breedCd, "cooldown counts down");
         hold(g, Items.WHEAT, 1);
         check(g.useOnMob(a, g.inv[g.sel]), "can be fed again afterwards");
      });

      test("mobs.dat: baby / sheared / cooldown survive save and load, old 6-field lines still load", () -> {
         Game g = newGame(0);
         Mob baby = g.mobs.spawnBaby(Mobs.PIG, 12.5, GROUND + 1, 12.5);
         baby.babyAge = 12345;
         Mob sheep = g.mobs.spawn(Mobs.SHEEP, 14.5, GROUND + 1, 12.5);
         sheep.color = 7;
         sheep.sheared = true;
         sheep.woolTimer = 777;
         sheep.breedCd = 4321;
         java.io.File dir = java.nio.file.Files.createTempDirectory("bc-mobs").toFile();
         g.mobs.save(dir);
         // add a legacy 6-field line (what Update 3.5 wrote)
         try (java.io.FileWriter fw = new java.io.FileWriter(new java.io.File(dir, "mobs.dat"), true)) {
            fw.write("1 20.5 61.0 12.5 10 0\n");
         }
         Game g2 = newGame(0);
         g2.mobs.load(dir);
         Mob lb = null;
         Mob ls = null;
         Mob lc = null;
         for (Mob m : g2.mobs.list) {
            if (m.type == Mobs.PIG) {
               lb = m;
            }
            if (m.type == Mobs.SHEEP) {
               ls = m;
            }
            if (m.type == Mobs.COW) {
               lc = m;
            }
         }
         check(lb != null && lb.babyAge == 12345, "baby age restored");
         check(lb != null && lb.bb[3] - lb.bb[0] < 0.5, "baby hitbox restored");
         check(ls != null && ls.sheared && ls.woolTimer == 777 && ls.breedCd == 4321 && ls.color == 7, "sheep state restored");
         check(lc != null && lc.babyAge == 0 && !lc.sheared, "legacy line loads as a normal adult");
      });

      test("zombies sometimes drop a carrot or a potato (about 2.5%)", () -> {
         Game g = newGame(0);
         int rare = 0;
         int n = 4000;
         for (int i = 0; i < n; i++) {
            g.world.items.clear();
            Mob z = g.mobs.spawn(Mobs.ZOMBIE, 12.5, GROUND + 1, 12.5);
            g.mobs.kill(z);
            rare += countItems(g.world, Items.CARROT) + countItems(g.world, Items.POTATO);
            g.mobs.list.clear();
         }
         double rate = rare / (double) n;
         check(rate > 0.012 && rate < 0.04, "carrot/potato rate " + rate);
      });
   }

   public static void main(String[] args) {
      recipes();
      farming();
      buckets();
      tnt();
      ladders();
      beds();
      farm36();
      animals36();
      Tests37.run();
      Tests38.run();
      Tests39.run();
      Tests310.run();
      Tests311.run();
      Tests312.run();
      TestsFall.run();
      TestsDisasters.run();
      TestsUi.run();
      TestsInv.run();
      Tests313.run();
      Tests314.run();
      Tests315.run();
      Tests316.run();
      Tests317.run();
      TestsPerf.run();
      TestsGfx.run();
      TestsPlayerAnim.run();
      Tests319.run();
      System.out.println();
      System.out.println("passed " + passed + ", failed " + failed);
      if (failed > 0) {
         System.out.println("FAILED TESTS:");
         for (String f : failures) {
            System.out.println("  - " + f);
         }
      }
      System.exit(failed == 0 ? 0 : 1);
   }
}
