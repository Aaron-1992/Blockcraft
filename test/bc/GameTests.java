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

      test("recipe: bed = 3 wool over 3 planks, any wool colour", () -> {
         int w0 = Blocks.WOOL0;
         Stack r = craft(3, 3, w0, w0, w0, PLANKS, PLANKS, PLANKS, 0, 0, 0);
         check(r != null && r.id == BED, "white bed -> " + (r == null ? "null" : "" + r.id));
         Stack mixed = craft(3, 3, w0 + 3, w0 + 3, w0 + 3, PLANKS, PLANKS, PLANKS, 0, 0, 0);
         check(mixed != null && mixed.id == BED, "light-blue wool bed");
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

      test("hoe: dirt works, stone/sand do not, side faces do not", () -> {
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
         g.target = hit(14, GROUND, 10, 1, 0, 0);
         check(!g.useSpecial(g.inv[g.sel]), "side face is not hoeable (current behaviour)");
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
         int firstRipe = -1;
         for (int t = 1; t <= 12000; t++) { // 10 minutes
            g.world.tick();
            if (t % 600 == 0) {
               int ripe = 0;
               for (int i = 0; i < plots; i++) {
                  if (g.world.getBlock(nearX, GROUND + 1, i) == 131) {
                     ripe++;
                  }
               }
               if (ripe == plots && firstRipe < 0) {
                  firstRipe = t;
               }
            }
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
         check(firstRipe > 0, "all near crops ripe at some point (first full ripeness at tick " + firstRipe + ")");
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
         // it does nothing on other blocks
         g.target = top(20, 20);
         check(!g.useSpecial(g.inv[g.sel]), "flint & steel on grass does nothing");
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

   public static void main(String[] args) {
      recipes();
      farming();
      buckets();
      tnt();
      ladders();
      beds();
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
