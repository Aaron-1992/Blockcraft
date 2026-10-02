package bc;

import static bc.GameTests.GROUND;
import static bc.GameTests.blockAbove;
import static bc.GameTests.check;
import static bc.GameTests.countItems;
import static bc.GameTests.craft;
import static bc.GameTests.eq;
import static bc.GameTests.hit;
import static bc.GameTests.hold;
import static bc.GameTests.newGame;
import static bc.GameTests.test;
import static bc.GameTests.tick;
import static bc.GameTests.top;

import bc.Mobs.Mob;
import java.util.HashSet;
import java.util.Set;

/**
 * Headless tests of Update 3.7: sugar cane / paper / books, pumpkins / melons / stems, cake, milk, eggs,
 * animals that follow food, fire, and the new worldgen.   python3 tools/runtests.py unit
 */
final class Tests37 {
   static final int WATER = Blocks.WATER;

   static void run() {
      data();
      recipes();
      cane();
      pumpkins();
      cake();
      kitchen();
      animals();
      fire();
      worldgen();
      meshing();
   }

   // ------------------------------------------------------------------ helpers
   static int count(World w, int x0, int y0, int z0, int x1, int y1, int z1, int id) {
      int n = 0;
      for (int x = x0; x <= x1; x++) {
         for (int y = y0; y <= y1; y++) {
            for (int z = z0; z <= z1; z++) {
               if (w.getBlock(x, y, z) == id) {
                  n++;
               }
            }
         }
      }
      return n;
   }

   /** Ticks the world until the condition holds (or the limit is reached); returns the ticks used. */
   static int tickUntil(World w, int limit, java.util.function.BooleanSupplier done) {
      for (int i = 0; i < limit; i++) {
         if (done.getAsBoolean()) {
            return i;
         }
         w.tick();
      }
      return done.getAsBoolean() ? limit : -1;
   }

   static void plot(Game g, int x, int z, int farmland) {
      g.world.setBlock(x, GROUND, z, farmland);
   }

   // ------------------------------------------------------------------ data
   static void data() {
      test("3.7 data: every new block / item is defined, named, drawn and in the creative inventory", () -> {
         for (int id = 159; id < Blocks.COUNT; id++) {
            check(Blocks.name[id] != null, "block " + id + " has a name");
            int[] t = Blocks.tiles[id];
            for (int f = 0; f < 6; f++) {
               check(t[f] >= 0 && t[f] < 256, "block " + id + " face " + f + " tile in the atlas");
            }
         }
         for (int id = 341; id <= 350; id++) {
            check(Items.name[id] != null, "item " + id + " has a name");
            check(TexExtra.sprite(id) != null, "item " + id + " has a sprite");
            check(Items.tile[id] >= 236 && Items.tile[id] <= 245, "item " + id + " tile 236..245 (" + Items.tile[id] + ")");
         }
         eq(Blocks.CAKE + 7, Blocks.FIRE, "fire follows the 7 cake blocks");
         eq(1, Blocks.kind[Blocks.SUGAR_CANE], "cane is a cross plant");
         eq(2, Blocks.kind[Blocks.CAKE], "cake is a box");
         eq(1, Blocks.kind[Blocks.FIRE], "fire is a cross-shaped cutout");
         check(!Blocks.solid[Blocks.SUGAR_CANE] && !Blocks.solid[Blocks.FIRE] && !Blocks.solid[Blocks.PUMPKIN_STEM], "plants and fire are not solid");
         check(Blocks.solid[Blocks.PUMPKIN] && Blocks.opaque[Blocks.PUMPKIN] && Blocks.opaque[Blocks.MELON] && Blocks.opaque[Blocks.BOOKSHELF], "pumpkin, melon, bookshelf are opaque cubes");
         eq(15, Blocks.emit[Blocks.FIRE], "fire gives light 15");
         for (int f = 0; f < 4; f++) {
            eq(15, Blocks.emit[Blocks.JACK_O_LANTERN + f], "jack o'lantern " + f + " gives light 15");
            eq(0, Blocks.emit[Blocks.CARVED_PUMPKIN + f], "carved pumpkin " + f + " gives no light");
            eq(163, Blocks.drop[Blocks.CARVED_PUMPKIN + f], "carved pumpkin " + f + " drops the canonical item");
            eq(167, Blocks.drop[Blocks.JACK_O_LANTERN + f], "jack o'lantern " + f + " drops the canonical item");
            // exactly one side shows the face
            int faces = 0;
            for (int side = 2; side < 6; side++) {
               if (Blocks.tiles[Blocks.CARVED_PUMPKIN + f][side] == 223) {
                  faces++;
                  eq(2 + f, side, "carved face of variant " + f + " is on side " + (2 + f));
               }
            }
            eq(1, faces, "variant " + f + " has exactly one face");
         }
         eq(347, Blocks.drop[Blocks.BOOKSHELF], "a bookshelf drops books");
         eq(343, Blocks.drop[Blocks.MELON], "a melon drops slices");
         check(Blocks.replaceable[Blocks.FIRE], "blocks can be placed into fire");
         check(Blocks.coll[Blocks.FIRE] == null && Blocks.coll[Blocks.SUGAR_CANE] == null, "no collision for fire and cane");
         for (int b = 1; b < 7; b++) {
            check(Blocks.box[Blocks.CAKE + b][0] > Blocks.box[Blocks.CAKE + b - 1][0], "bitten cake " + b + " is smaller");
            eq(Blocks.tiles[Blocks.CAKE + b][5], 234, "bitten cake " + b + " shows the cut face on its west side");
         }
         eq(231, Blocks.tiles[Blocks.CAKE][0], "cake top");
         for (int b = 0; b < 8; b++) {
            eq(227 + b / 2, Blocks.tiles[Blocks.PUMPKIN_STEM + b][0], "pumpkin stem stage " + b + " tile");
            eq(227 + b / 2, Blocks.tiles[Blocks.MELON_STEM + b][0], "melon stem stage " + b + " tile");
            eq(b, Blocks.stemStage(Blocks.PUMPKIN_STEM + b), "pumpkin stem stage " + b);
            eq(b, Blocks.stemStage(Blocks.MELON_STEM + b), "melon stem stage " + b);
         }
         eq(Blocks.PUMPKIN, Blocks.stemFruit(Blocks.PUMPKIN_STEM + 3), "pumpkin stem -> pumpkin");
         eq(Blocks.MELON, Blocks.stemFruit(Blocks.MELON_STEM + 5), "melon stem -> melon");
         eq(Blocks.PUMPKIN_STEM, Items.plantBlock(Items.PUMPKIN_SEEDS), "pumpkin seeds plant a pumpkin stem");
         eq(Blocks.MELON_STEM, Items.plantBlock(Items.MELON_SEEDS), "melon seeds plant a melon stem");
         eq(Blocks.stemSeedItem(Blocks.MELON_STEM + 2), Items.MELON_SEEDS, "melon stem -> melon seeds");
         check(Items.isFood(Items.MELON_SLICE) && Items.foodPts[Items.MELON_SLICE] == 2, "melon slice: 1 drumstick");
         check(Items.isFood(Items.PUMPKIN_PIE) && Items.foodPts[Items.PUMPKIN_PIE] == 8, "pumpkin pie: 4 drumsticks");
         check(!Items.isFood(Items.SUGAR) && !Items.isFood(Items.EGG) && !Items.isFood(Items.MILK_BUCKET), "sugar, egg and milk are ingredients, not food");
         eq(1, new Stack(Items.MILK_BUCKET, 1).max(), "milk bucket stacks to 1");
         eq(1, new Stack(Items.CAKE, 1).max(), "cake stacks to 1");
         eq(16, new Stack(Items.EGG, 1).max(), "eggs stack to 16");
         eq(64, new Stack(Items.PAPER, 1).max(), "paper stacks to 64");
         Set<Integer> palette = new HashSet<>();
         for (int id : Blocks.creativeList()) {
            check(palette.add(id), "creative list has no duplicates (" + id + ")");
         }
         for (int id : new int[]{159, 160, 161, 163, 167, 170, 341, 342, 343, 344, 345, 346, 347, 348, 349, 350}) {
            check(palette.contains(id), "creative inventory offers " + Items.name(id));
         }
         for (int id : new int[]{162, 164, 165, 166, 168, 169, 171, 179, 187, 194}) {
            check(!palette.contains(id), "variant / internal block " + id + " is not offered");
         }
         eq(163, Blocks.pickItem(Blocks.CARVED_PUMPKIN + 3), "pick-block on a carved pumpkin");
         eq(167, Blocks.pickItem(Blocks.JACK_O_LANTERN + 2), "pick-block on a jack o'lantern");
         eq(350, Blocks.pickItem(Blocks.CAKE + 4), "pick-block on a cake");
         eq(0, Blocks.pickItem(Blocks.FIRE), "pick-block on fire gives nothing");
         eq(341, Blocks.pickItem(Blocks.PUMPKIN_STEM + 6), "pick-block on a stem gives its seeds");
         eq(Blocks.STONE, Blocks.pickItem(Blocks.STONE), "pick-block on stone is unchanged");
      });
   }

   // ------------------------------------------------------------------ recipes
   static void recipes() {
      test("recipes 3.7: sugar, paper, book, bookshelf", () -> {
         Stack r = craft(1, 1, Blocks.SUGAR_CANE);
         check(r != null && r.id == Items.SUGAR && r.n == 1, "1 cane -> 1 sugar");
         r = craft(3, 1, Blocks.SUGAR_CANE, Blocks.SUGAR_CANE, Blocks.SUGAR_CANE);
         check(r != null && r.id == Items.PAPER && r.n == 3, "3 canes in a row -> 3 paper");
         r = craft(3, 3, 0, 0, 0, Blocks.SUGAR_CANE, Blocks.SUGAR_CANE, Blocks.SUGAR_CANE, 0, 0, 0);
         check(r != null && r.id == Items.PAPER && r.n == 3, "the row can be in the middle of a 3x3 grid");
         check(craft(2, 1, Blocks.SUGAR_CANE, Blocks.SUGAR_CANE) == null, "2 canes make nothing");
         check(craft(3, 3, Blocks.SUGAR_CANE, 0, 0, Blocks.SUGAR_CANE, 0, 0, Blocks.SUGAR_CANE, 0, 0) == null, "3 canes in a column make nothing");
         r = craft(2, 2, Items.PAPER, Items.PAPER, Items.PAPER, 261);
         check(r != null && r.id == Items.BOOK && r.n == 1, "3 paper + leather -> book");
         check(craft(2, 2, Items.PAPER, Items.PAPER, 261, 0) == null, "2 paper + leather -> nothing");
         r = craft(3, 3, 5, 5, 5, Items.BOOK, Items.BOOK, Items.BOOK, 5, 5, 5);
         check(r != null && r.id == Blocks.BOOKSHELF && r.n == 1, "6 planks + 3 books -> bookshelf");
         check(craft(3, 3, 5, 5, 5, Items.BOOK, Items.BOOK, 0, 5, 5, 5) == null, "bookshelf needs 3 books");
      });

      test("recipes 3.7: pumpkin seeds, melon, pie, jack o'lantern, cake", () -> {
         Stack r = craft(1, 1, Blocks.PUMPKIN);
         check(r != null && r.id == Items.PUMPKIN_SEEDS && r.n == 4, "pumpkin -> 4 seeds");
         r = craft(1, 1, Items.MELON_SLICE);
         check(r != null && r.id == Items.MELON_SEEDS && r.n == 1, "slice -> melon seeds");
         int s = Items.MELON_SLICE;
         r = craft(3, 3, s, s, s, s, s, s, s, s, s);
         check(r != null && r.id == Blocks.MELON && r.n == 1, "9 slices -> melon block");
         check(craft(3, 3, s, s, s, s, s, s, s, s, 0) == null, "8 slices make nothing");
         r = craft(3, 3, Blocks.PUMPKIN, Items.SUGAR, Items.EGG, 0, 0, 0, 0, 0, 0);
         check(r != null && r.id == Items.PUMPKIN_PIE && r.n == 1, "pumpkin + sugar + egg -> pie");
         r = craft(3, 3, 0, Items.EGG, 0, Items.SUGAR, 0, 0, Blocks.PUMPKIN, 0, 0);
         check(r != null && r.id == Items.PUMPKIN_PIE, "pie is shapeless");
         r = craft(1, 2, 163, Blocks.TORCH);
         check(r != null && r.id == 167 && r.n == 1, "carved pumpkin over a torch -> jack o'lantern");
         check(craft(1, 2, Blocks.PUMPKIN, Blocks.TORCH) == null, "an uncarved pumpkin does not make a lantern");
         check(craft(1, 2, Blocks.TORCH, 163) == null, "the torch must be below");
         int m = Items.MILK_BUCKET;
         r = craft(3, 3, m, m, m, Items.SUGAR, Items.EGG, Items.SUGAR, Items.WHEAT, Items.WHEAT, Items.WHEAT);
         check(r != null && r.id == Items.CAKE && r.n == 1, "3 milk + 2 sugar + egg + 3 wheat -> cake");
         check(craft(3, 3, m, m, Items.BUCKET, Items.SUGAR, Items.EGG, Items.SUGAR, Items.WHEAT, Items.WHEAT, Items.WHEAT) == null, "an empty bucket is not milk");
      });

      test("crafting grid: the empty buckets of a cake come back (milk bucket leaves a bucket)", () -> {
         Game g = newGame(0);
         Menu m = new Menu(g, Menu.CRAFT3, null);
         int[] ids = {Items.MILK_BUCKET, Items.MILK_BUCKET, Items.MILK_BUCKET, Items.SUGAR, Items.EGG, Items.SUGAR, Items.WHEAT, Items.WHEAT, Items.WHEAT};
         for (int i = 0; i < 9; i++) {
            m.grid[i] = new Stack(ids[i], 1);
         }
         m.changed();
         check(m.out[0] != null && m.out[0].id == Items.CAKE, "cake in the output slot");
         Menu.Slot out = null;
         for (Menu.Slot sl : m.slots) {
            if (sl.type == Menu.OUTPUT) {
               out = sl;
            }
         }
         m.takeOutput(out, false);
         check(g.cursor != null && g.cursor.id == Items.CAKE, "the cake is on the cursor");
         for (int i = 0; i < 3; i++) {
            check(m.grid[i] != null && m.grid[i].id == Items.BUCKET && m.grid[i].n == 1, "empty bucket in slot " + i);
         }
         for (int i = 3; i < 9; i++) {
            check(m.grid[i] == null, "ingredient slot " + i + " is used up");
         }
         check(m.out[0] == null, "no recipe left");
         eq(0, Recipes.leftover(Items.WHEAT), "wheat leaves nothing");
         eq(Items.BUCKET, Recipes.leftover(Items.MILK_BUCKET), "milk bucket -> bucket");
      });
   }

   // ------------------------------------------------------------------ sugar cane
   static void cane() {
      test("sugar cane: only on grass / dirt / sand with water next to the ground block (or on cane)", () -> {
         Game g = newGame(0);
         World w = g.world;
         hold(g, Blocks.SUGAR_CANE, 20);
         w.setBlock(11, GROUND, 10, WATER); // water east of (10, GROUND, 10)
         g.target = top(10, 10);
         check(g.placeBlock(), "cane on grass next to water");
         eq(Blocks.SUGAR_CANE, blockAbove(g, 10, 10), "cane placed");
         eq(19, g.inv[g.sel].n, "one cane used");
         g.target = hit(10, GROUND + 1, 10, 0, 1, 0);
         check(g.placeBlock(), "cane on top of cane");
         eq(Blocks.SUGAR_CANE, w.getBlock(10, GROUND + 2, 10), "second cane");
         g.target = top(20, 20);
         check(!g.placeBlock(), "no cane on grass far from water");
         w.setBlock(21, GROUND + 1, 21, WATER); // water one level too high
         g.target = top(20, 21);
         check(!g.placeBlock(), "water above the ground level does not count");
         w.setBlock(21, GROUND, 22, WATER); // diagonal water does not count
         g.target = top(20, 21);
         check(!g.placeBlock(), "diagonal water does not count");
         w.setBlock(30, GROUND, 30, Blocks.SAND);
         w.setBlock(30, GROUND, 31, WATER);
         g.target = top(30, 30);
         check(g.placeBlock(), "cane on sand next to water");
         w.setBlock(40, GROUND, 40, Blocks.DIRT);
         w.setBlock(39, GROUND, 40, WATER);
         g.target = top(40, 40);
         check(g.placeBlock(), "cane on dirt next to water");
         w.setBlock(50, GROUND, 50, Blocks.STONE);
         w.setBlock(49, GROUND, 50, WATER);
         g.target = top(50, 50);
         check(!g.placeBlock(), "no cane on stone");
         w.setBlock(60, GROUND, 60, Blocks.GRAVEL);
         w.setBlock(59, GROUND, 60, WATER);
         g.target = top(60, 60);
         check(!g.placeBlock(), "no cane on gravel");
         // the creative player can place it too, and cane never stands in a player's way (no collision)
         check(Blocks.coll[Blocks.SUGAR_CANE] == null, "cane has no hitbox");
      });

      test("sugar cane: pops off (dropping itself) when its ground goes - the whole stack at once", () -> {
         Game g = newGame(0);
         World w = g.world;
         w.setBlock(11, GROUND, 10, WATER);
         for (int i = 1; i <= 3; i++) {
            w.setBlock(10, GROUND + i, 10, Blocks.SUGAR_CANE);
         }
         tick(w, 5);
         eq(3, count(w, 10, GROUND + 1, 10, 10, GROUND + 3, 10, Blocks.SUGAR_CANE), "a stable stack stays");
         w.setBlock(10, GROUND, 10, 0);
         tick(w, 5);
         eq(0, count(w, 10, GROUND + 1, 10, 10, GROUND + 3, 10, Blocks.SUGAR_CANE), "the whole stack popped");
         eq(3, countItems(w, Blocks.SUGAR_CANE), "3 canes dropped");
         // breaking only the lowest block lets the rest fall too
         Game h = newGame(0);
         h.world.setBlock(11, GROUND, 10, WATER);
         for (int i = 1; i <= 3; i++) {
            h.world.setBlock(10, GROUND + i, 10, Blocks.SUGAR_CANE);
         }
         h.target = hit(10, GROUND + 1, 10, 0, 1, 0);
         h.breakBlock(10, GROUND + 1, 10);
         tick(h.world, 5);
         eq(0, count(h.world, 10, GROUND + 1, 10, 10, GROUND + 3, 10, Blocks.SUGAR_CANE), "nothing left standing");
         eq(3, countItems(h.world, Blocks.SUGAR_CANE), "3 canes in total (1 dropped by the break + 2 that popped)");
         // breaking the TOP one leaves the others
         Game k = newGame(0);
         k.world.setBlock(11, GROUND, 10, WATER);
         for (int i = 1; i <= 3; i++) {
            k.world.setBlock(10, GROUND + i, 10, Blocks.SUGAR_CANE);
         }
         k.breakBlock(10, GROUND + 3, 10);
         tick(k.world, 5);
         eq(2, count(k.world, 10, GROUND + 1, 10, 10, GROUND + 3, 10, Blocks.SUGAR_CANE), "two canes remain");
      });

      test("sugar cane: unsupported cane (no water) pops off by itself, supported cane keeps standing", () -> {
         Game g = newGame(0);
         World w = g.world;
         w.setBlock(12, GROUND + 1, 12, Blocks.SUGAR_CANE); // no water anywhere near
         w.setBlock(14, GROUND, 12, WATER);
         w.setBlock(15, GROUND + 1, 12, Blocks.SUGAR_CANE); // water next to its ground block
         // the cane at x=15 has water at x=14: valid. Remove the water *diagonally* later.
         int ticks = tickUntil(w, 20000, () -> w.getBlock(12, GROUND + 1, 12) == 0);
         check(ticks >= 0, "the lonely cane popped off after " + ticks + " ticks");
         eq(Blocks.SUGAR_CANE, w.getBlock(15, GROUND + 1, 12), "the supported cane stays");
         eq(1, countItems(w, Blocks.SUGAR_CANE), "the popped cane dropped as an item");
         w.setBlock(14, GROUND, 12, 0); // water removed (face-adjacent to the ground block: no update for the cane)
         int t2 = tickUntil(w, 20000, () -> w.getBlock(15, GROUND + 1, 12) == 0);
         check(t2 >= 0, "cane without its water popped off at a random tick after " + t2 + " ticks");
      });

      test("sugar cane: grows to 3 blocks next to water by random ticks and never beyond", () -> {
         Game g = newGame(0);
         World w = g.world;
         w.setBlock(11, GROUND, 10, WATER);
         w.setBlock(10, GROUND + 1, 10, Blocks.SUGAR_CANE);
         int ticks = tickUntil(w, 120000, () -> w.getBlock(10, GROUND + 3, 10) == Blocks.SUGAR_CANE);
         check(ticks >= 0, "the cane reached 3 blocks after " + ticks + " ticks");
         tick(w, 15000);
         eq(3, count(w, 10, GROUND + 1, 10, 10, GROUND + 8, 10, Blocks.SUGAR_CANE), "still exactly 3 high");
         // growCane itself: no growth when the cell above is occupied
         Game h = newGame(0);
         h.world.setBlock(11, GROUND, 10, WATER);
         h.world.setBlock(10, GROUND + 1, 10, Blocks.SUGAR_CANE);
         h.world.setBlock(10, GROUND + 2, 10, Blocks.STONE);
         h.world.growCane(10, GROUND + 1, 10);
         eq(Blocks.STONE, h.world.getBlock(10, GROUND + 2, 10), "growth does not replace blocks");
         h.world.setBlock(10, GROUND + 2, 10, 0);
         h.world.growCane(10, GROUND + 1, 10);
         eq(Blocks.SUGAR_CANE, h.world.getBlock(10, GROUND + 2, 10), "growCane adds one block on top");
      });

      test("sugar cane / sugar / paper: survival drops and the break of a stack", () -> {
         Game g = newGame(0);
         g.world.setBlock(11, GROUND, 10, WATER);
         g.world.setBlock(10, GROUND + 1, 10, Blocks.SUGAR_CANE);
         g.breakBlock(10, GROUND + 1, 10);
         eq(1, countItems(g.world, Blocks.SUGAR_CANE), "breaking cane drops cane");
         eq(0, g.world.getBlock(10, GROUND + 1, 10), "cane removed");
         // creative: nothing dropped
         Game c = newGame(1);
         c.world.setBlock(11, GROUND, 10, WATER);
         c.world.setBlock(10, GROUND + 1, 10, Blocks.SUGAR_CANE);
         c.breakBlock(10, GROUND + 1, 10);
         eq(0, countItems(c.world, Blocks.SUGAR_CANE), "creative: no drop");
      });
   }

   // ------------------------------------------------------------------ pumpkins and melons
   static void pumpkins() {
      test("seeds: pumpkin / melon seeds plant a stem on farmland only", () -> {
         Game g = newGame(0);
         plot(g, 10, 10, Blocks.FARMLAND_WET);
         plot(g, 12, 10, Blocks.FARMLAND_WET);
         hold(g, Items.PUMPKIN_SEEDS, 3);
         g.target = top(10, 10);
         check(g.placeBlock(), "pumpkin seeds on farmland");
         eq(Blocks.PUMPKIN_STEM, blockAbove(g, 10, 10), "pumpkin stem stage 0");
         eq(2, g.inv[g.sel].n, "a seed is used");
         hold(g, Items.MELON_SEEDS, 3);
         g.target = top(12, 10);
         check(g.placeBlock(), "melon seeds on farmland");
         eq(Blocks.MELON_STEM, blockAbove(g, 12, 10), "melon stem stage 0");
         hold(g, Items.PUMPKIN_SEEDS, 3);
         g.target = top(20, 20);
         check(!g.placeBlock(), "no stem on plain grass");
         eq(0, blockAbove(g, 20, 20), "nothing planted");
         g.target = top(10, 10);
         check(!g.placeBlock(), "farmland that is already planted");
         // creative does not use up the seeds
         Game c = newGame(1);
         plot(c, 10, 10, Blocks.FARMLAND);
         hold(c, Items.MELON_SEEDS, 1);
         c.target = top(10, 10);
         check(c.placeBlock(), "creative planting");
         eq(1, c.inv[c.sel].n, "creative keeps the seeds");
      });

      test("stems: grow through 8 stages on farmland; a fruit appears next to the fully grown stem - one at a time", () -> {
         for (int kind = 0; kind < 2; kind++) {
            final int stem0 = kind == 0 ? Blocks.PUMPKIN_STEM : Blocks.MELON_STEM;
            final int fruit = kind == 0 ? Blocks.PUMPKIN : Blocks.MELON;
            Game g = newGame(0);
            World w = g.world;
            plot(g, 10, 10, Blocks.FARMLAND_WET);
            w.setBlock(13, GROUND, 10, WATER); // irrigation keeps the farmland wet
            w.setBlock(10, GROUND + 1, 10, stem0);
            int seen = 0;
            boolean orderly = true;
            for (int i = 0; i < 60000 && seen < 7; i++) {
               w.tick();
               int b = w.getBlock(10, GROUND + 1, 10);
               if (!(Blocks.isStem(b) && b >= stem0 && b < stem0 + 8)) {
                  orderly = false;
                  break;
               }

               int st = Blocks.stemStage(b);
               if (st < seen || st > seen + 1) {
                  orderly = false;
               }

               seen = st;
            }
            check(orderly, "stems stay stems of their own kind and grow one stage at a time");
            eq(7, seen, "the stem reached stage 7");
            int[][] sides = {{9, 10}, {11, 10}, {10, 9}, {10, 11}};
            java.util.function.IntSupplier fruits = () -> {
               int n = 0;
               for (int[] s : sides) {
                  if (w.getBlock(s[0], GROUND + 1, s[1]) == fruit) {
                     n++;
                  }
               }
               return n;
            };
            int t = tickUntil(w, 100000, () -> fruits.getAsInt() > 0);
            check(t >= 0, (kind == 0 ? "pumpkin" : "melon") + " grew next to the stem after " + t + " ticks");
            tick(w, 30000);
            eq(1, fruits.getAsInt(), "no second fruit while one is next to the stem");
            eq(stem0 + 7, w.getBlock(10, GROUND + 1, 10), "the stem is still there, fully grown");
            for (int[] s : sides) { // harvest it: a new one grows
               if (w.getBlock(s[0], GROUND + 1, s[1]) == fruit) {
                  w.setBlock(s[0], GROUND + 1, s[1], 0);
               }
            }
            int t2 = tickUntil(w, 100000, () -> fruits.getAsInt() > 0);
            check(t2 >= 0, "a new fruit grows after the first was harvested (" + t2 + " ticks)");
         }
      });

      test("stems: dry farmland grows slower than wet farmland; a stem needs farmland below", () -> {
         // wet vs dry, same random stream
         int[] used = new int[2];
         for (int dry = 0; dry < 2; dry++) {
            Game g = newGame(0);
            plot(g, 10, 10, dry == 0 ? Blocks.FARMLAND_WET : Blocks.FARMLAND);
            if (dry == 0) {
               g.world.setBlock(13, GROUND, 10, WATER);
            }

            g.world.setBlock(10, GROUND + 1, 10, Blocks.PUMPKIN_STEM);
            final Game gg = g;
            int t = tickUntil(g.world, 80000, () -> Blocks.stemStage(gg.world.getBlock(10, GROUND + 1, 10)) == 4 && Blocks.isStem(gg.world.getBlock(10, GROUND + 1, 10)));
            used[dry] = t;
            check(t >= 0, "stem reached stage 4 (dry=" + dry + ") after " + t);
         }
         check(used[0] >= 0 && used[1] >= 0 && used[0] < used[1], "wet farmland: " + used[0] + " ticks, dry: " + used[1] + " ticks");
         Game g = newGame(0);
         plot(g, 10, 10, Blocks.FARMLAND_WET);
         g.world.setBlock(10, GROUND + 1, 10, Blocks.MELON_STEM + 5);
         g.world.setBlock(10, GROUND, 10, Blocks.DIRT);
         tick(g.world, 5);
         eq(0, g.world.getBlock(10, GROUND + 1, 10), "a stem pops off when the farmland below it is gone");
         check(countItems(g.world, Items.MELON_SEEDS) == 1, "and drops its seed");
      });

      test("stems: breaking drops 0-3 seeds depending on age; bone meal grows them", () -> {
         int young = 0;
         int old = 0;
         Game k = newGame(0);
         plot(k, 10, 10, Blocks.FARMLAND_WET);
         for (int i = 0; i < 300; i++) {
            k.world.items.clear();
            k.world.setBlock(10, GROUND + 1, 10, Blocks.PUMPKIN_STEM);
            k.breakBlock(10, GROUND + 1, 10);
            young += countItems(k.world, Items.PUMPKIN_SEEDS);
            k.world.items.clear();
            k.world.setBlock(10, GROUND + 1, 10, Blocks.PUMPKIN_STEM + 7);
            k.breakBlock(10, GROUND + 1, 10);
            int n = countItems(k.world, Items.PUMPKIN_SEEDS);
            check(n <= 3, "never more than 3 seeds");
            old += n;
         }
         check(young < 300 * 0.5, "a sprout drops few seeds (" + young / 300.0 + " on average)");
         check(old > 300 * 1.0 && old < 300 * 2.0, "a grown stem drops 1.2 - 1.6 seeds on average (" + old / 300.0 + ")");
         Game g = newGame(0);
         plot(g, 10, 10, Blocks.FARMLAND_WET);
         g.world.setBlock(10, GROUND + 1, 10, Blocks.MELON_STEM);
         hold(g, 339, 5);
         g.target = hit(10, GROUND + 1, 10, 0, 1, 0);
         check(g.useSpecial(g.inv[g.sel]), "bone meal on a stem");
         int st = Blocks.stemStage(g.world.getBlock(10, GROUND + 1, 10));
         check(st >= 2 && st <= 5, "bone meal grows 2-5 stages (" + st + ")");
         check(g.world.getBlock(10, GROUND + 1, 10) >= Blocks.MELON_STEM, "it is still a melon stem");
         g.world.setBlock(10, GROUND + 1, 10, Blocks.MELON_STEM + 7);
         int before = g.inv[g.sel].n;
         g.target = top(10, 10); // targeting the farmland below
         check(!g.useSpecial(g.inv[g.sel]) || g.inv[g.sel].n == before - 1 && Blocks.stemStage(g.world.getBlock(10, GROUND + 1, 10)) == 7, "a grown stem is not changed by bone meal");
      });

      test("melon and pumpkin blocks: drops", () -> {
         Game g = newGame(0);
         int min = 99;
         int max = 0;
         for (int i = 0; i < 60; i++) {
            g.world.items.clear();
            g.world.setBlock(10, GROUND + 1, 10, Blocks.MELON);
            g.breakBlock(10, GROUND + 1, 10);
            int n = countItems(g.world, Items.MELON_SLICE);
            min = Math.min(min, n);
            max = Math.max(max, n);
         }
         g.world.items.clear();
         check(min >= 3 && max <= 7 && max > min, "a melon drops 3-7 slices (" + min + ".." + max + ")");
         g.world.setBlock(10, GROUND + 1, 10, Blocks.PUMPKIN);
         g.breakBlock(10, GROUND + 1, 10);
         eq(1, countItems(g.world, Blocks.PUMPKIN), "a pumpkin drops itself");
         g.world.setBlock(12, GROUND + 1, 10, Blocks.BOOKSHELF);
         g.breakBlock(12, GROUND + 1, 10);
         eq(3, countItems(g.world, Items.BOOK), "a bookshelf drops 3 books");
         g.world.setBlock(14, GROUND + 1, 10, Blocks.CARVED_PUMPKIN + 2);
         g.breakBlock(14, GROUND + 1, 10);
         eq(1, countItems(g.world, 163), "a carved pumpkin drops the canonical item");
         g.world.setBlock(16, GROUND + 1, 10, Blocks.JACK_O_LANTERN + 3);
         g.breakBlock(16, GROUND + 1, 10);
         eq(1, countItems(g.world, 167), "a jack o'lantern drops the canonical item");
      });

      test("shears carve a pumpkin: the face shows on the clicked side, 4 seeds drop, shears wear out by 1", () -> {
         int[][] normals = {{0, 0, -1}, {0, 0, 1}, {1, 0, 0}, {-1, 0, 0}};
         for (int f = 0; f < 4; f++) {
            Game g = newGame(0);
            g.world.setBlock(10, GROUND + 1, 10, Blocks.PUMPKIN);
            hold(g, Items.SHEARS, 1);
            g.target = hit(10, GROUND + 1, 10, normals[f][0], normals[f][1], normals[f][2]);
            check(g.useBlock(), "carved from side " + f);
            eq(Blocks.CARVED_PUMPKIN + f, g.world.getBlock(10, GROUND + 1, 10), "face on the clicked side " + f);
            eq(4, countItems(g.world, Items.PUMPKIN_SEEDS), "4 seeds");
            eq(1, g.inv[g.sel].dmg, "shears wear");
            // a carved pumpkin cannot be carved again
            g.target = hit(10, GROUND + 1, 10, 0, 0, 1);
            check(!g.useBlock(), "already carved");
         }
         // top click: towards the player. Player looks along +z (yaw 0): the face looks back at -z (variant 0)
         Game g = newGame(0);
         g.player.yaw = 0;
         g.world.setBlock(10, GROUND + 1, 10, Blocks.PUMPKIN);
         hold(g, Items.SHEARS, 1);
         g.target = hit(10, GROUND + 1, 10, 0, 1, 0);
         check(g.useBlock(), "carved from the top");
         eq(Blocks.CARVED_PUMPKIN, g.world.getBlock(10, GROUND + 1, 10), "faces the player (north)");
         // not with other items / not in adventure mode / not on stone
         Game h = newGame(0);
         h.world.setBlock(10, GROUND + 1, 10, Blocks.PUMPKIN);
         hold(h, Items.STICK, 1);
         h.target = hit(10, GROUND + 1, 10, 0, 0, 1);
         check(!h.useBlock(), "a sword does not carve");
         eq(Blocks.PUMPKIN, h.world.getBlock(10, GROUND + 1, 10), "still a pumpkin");
         Game c = newGame(1);
         c.world.setBlock(10, GROUND + 1, 10, Blocks.PUMPKIN);
         hold(c, Items.SHEARS, 1);
         c.target = hit(10, GROUND + 1, 10, 1, 0, 0);
         check(c.useBlock(), "creative can carve");
         eq(0, c.inv[c.sel].dmg, "creative: no wear");
         Game a = newGame(2);
         a.world.setBlock(10, GROUND + 1, 10, Blocks.PUMPKIN);
         hold(a, Items.SHEARS, 1);
         a.target = hit(10, GROUND + 1, 10, 1, 0, 0);
         check(!a.useBlock(), "adventure mode cannot carve");
      });

      test("carved pumpkins and jack o'lanterns are placed facing the player; the lantern lights up its surroundings", () -> {
         // yaw -> expected variant (the face looks back at the player)
         double[][] cases = {{0, 0}, {180, 1}, {90, 2}, {270, 3}};
         for (double[] cs : cases) {
            Game g = newGame(0);
            g.player.yaw = (float)cs[0];
            hold(g, 163, 2);
            g.target = top(10, 10);
            check(g.placeBlock(), "carved pumpkin placed (yaw " + cs[0] + ")");
            eq(Blocks.CARVED_PUMPKIN + (int)cs[1], blockAbove(g, 10, 10), "carved pumpkin faces the player (yaw " + cs[0] + ")");
            hold(g, 167, 2);
            g.target = top(30, 10);
            check(g.placeBlock(), "jack o'lantern placed");
            eq(Blocks.JACK_O_LANTERN + (int)cs[1], blockAbove(g, 30, 10), "lantern faces the player (yaw " + cs[0] + ")");
            check(g.world.getBlk(30, GROUND + 2, 10) >= 14, "the lantern lights the block above it: " + g.world.getBlk(30, GROUND + 2, 10));
            check(g.world.getBlk(32, GROUND + 1, 10) >= 12, "and the cells 2 away: " + g.world.getBlk(32, GROUND + 1, 10));
            eq(0, g.world.getBlk(10, GROUND + 2, 10), "a carved pumpkin without a candle does not give light");
         }
      });
   }

   // ------------------------------------------------------------------ cake
   static void cake() {
      test("cake: placed on solid ground, eaten slice by slice (+2 hunger each, only when hungry), gone after 7 bites", () -> {
         Game g = newGame(0);
         hold(g, Items.CAKE, 1);
         g.target = top(10, 10);
         check(g.placeBlock(), "cake placed");
         eq(Blocks.CAKE, blockAbove(g, 10, 10), "cake block");
         check(g.inv[g.sel] == null, "the cake item is used up");
         g.player.food = 20;
         g.target = hit(10, GROUND + 1, 10, 0, 1, 0);
         check(!g.useBlock(), "cannot eat when not hungry");
         eq(Blocks.CAKE, blockAbove(g, 10, 10), "still a whole cake");
         for (int bite = 1; bite <= 7; bite++) {
            g.player.food = 10;
            g.player.sat = 0;
            check(g.useBlock(), "bite " + bite);
            eq(12, g.player.food, "+2 hunger");
            check(g.player.sat > 0.0F && g.player.sat <= 0.4F + 1e-4F, "a little saturation");
            eq(bite < 7 ? Blocks.CAKE + bite : 0, blockAbove(g, 10, 10), "after bite " + bite);
         }
         // creative player: no eating
         Game c = newGame(1);
         c.world.setBlock(10, GROUND + 1, 10, Blocks.CAKE);
         c.target = hit(10, GROUND + 1, 10, 0, 1, 0);
         check(!c.useBlock(), "creative cannot eat cake");
         // hunger never exceeds 20
         Game h = newGame(0);
         h.world.setBlock(10, GROUND + 1, 10, Blocks.CAKE + 2);
         h.player.food = 19;
         h.target = hit(10, GROUND + 1, 10, 0, 1, 0);
         check(h.useBlock(), "ate with 19 hunger");
         eq(20, h.player.food, "capped at 20");
      });

      test("cake: needs a solid block below; breaks (without a drop) when it is removed; no cake in mid-air / inside the player", () -> {
         Game g = newGame(0);
         hold(g, Items.CAKE, 3);
         g.world.setBlock(10, GROUND + 1, 10, Blocks.STONE);
         g.target = hit(10, GROUND + 1, 10, 1, 0, 0); // east side of a stone block: the free cell next to it
         check(g.placeBlock(), "cake placed against a block side");
         eq(Blocks.CAKE, g.world.getBlock(11, GROUND + 1, 10), "cake in the neighbouring cell");
         g.world.setBlock(20, GROUND + 1, 20, Blocks.CAKE);
         g.world.setBlock(20, GROUND, 20, 0);
         tick(g.world, 3);
         eq(0, g.world.getBlock(20, GROUND + 1, 20), "cake without ground disappears");
         eq(0, countItems(g.world, Items.CAKE) + countItems(g.world, Blocks.CAKE), "and drops nothing");
         g.target = hit(30, GROUND + 6, 30, 0, 1, 0);
         check(!g.placeBlock(), "no cake in mid-air");
         g.player.setPos(40.5, GROUND + 1, 40.5);
         g.target = top(40, 40);
         check(!g.placeBlock(), "no cake inside the player");
         check(g.inv[g.sel] != null && g.inv[g.sel].n == 2, "the cake item was kept");
         // a cake on water is not possible
         g.world.setBlock(50, GROUND, 50, WATER);
         g.target = top(50, 50);
         check(!g.placeBlock(), "no cake on water");
      });

      test("cake: the shape shrinks with every bite (the cut face is on the west side)", () -> {
         double prev = -1;
         for (int b = 0; b < 7; b++) {
            double[] bx = Blocks.box[Blocks.CAKE + b];
            check(bx[0] > prev, "bite " + b + " starts further east");
            prev = bx[0];
            eq(0.5, bx[4], "cake is half a block high");
            check(bx[3] == 0.9375 && bx[2] == 0.0625 && bx[5] == 0.9375, "inset like Minecraft");
            check(Blocks.coll[Blocks.CAKE + b] != null, "cake has a hitbox");
         }
         eq(0.0625, Blocks.box[Blocks.CAKE][0], "a whole cake starts 1/16 in");
         eq(0.8125, Blocks.box[Blocks.CAKE + 6][0], "the last slice is 2/16 wide");
      });
   }

   // ------------------------------------------------------------------ milk, eggs
   static void kitchen() {
      test("milk: an empty bucket on a grown-up cow gives a milk bucket (survival); calves, other animals and creative do nothing", () -> {
         Game g = newGame(0);
         g.mobs.timer = 1;
         Mob cow = g.mobs.spawn(Mobs.COW, 12.5, GROUND + 1, 12.5);
         hold(g, Items.BUCKET, 1);
         check(g.useOnMob(cow, g.inv[g.sel]), "milked");
         eq(Items.MILK_BUCKET, g.inv[g.sel].id, "the bucket became a milk bucket");
         eq(1, g.inv[g.sel].n, "one");
         check(!g.useOnMob(cow, g.inv[g.sel]), "a milk bucket on a cow does nothing");
         // a stack of 3 buckets: 2 stay, 1 milk goes elsewhere
         hold(g, Items.BUCKET, 3);
         check(g.useOnMob(cow, g.inv[g.sel]), "milked from a stack");
         eq(2, g.inv[g.sel].n, "2 empty buckets left in the hand");
         eq(Items.BUCKET, g.inv[g.sel].id, "still buckets");
         eq(1, g.countItem(Items.MILK_BUCKET), "one milk bucket in the inventory (the first one is in slot " + g.sel + "?)");
         // calf
         Game h = newGame(0);
         Mob calf = h.mobs.spawnBaby(Mobs.COW, 12.5, GROUND + 1, 12.5);
         hold(h, Items.BUCKET, 1);
         check(!h.useOnMob(calf, h.inv[h.sel]), "calves cannot be milked");
         Mob pig = h.mobs.spawn(Mobs.PIG, 14.5, GROUND + 1, 12.5);
         check(!h.useOnMob(pig, h.inv[h.sel]), "pigs cannot be milked");
         Mob sheep = h.mobs.spawn(Mobs.SHEEP, 16.5, GROUND + 1, 12.5);
         check(!h.useOnMob(sheep, h.inv[h.sel]), "sheep cannot be milked");
         eq(Items.BUCKET, h.inv[h.sel].id, "the bucket is untouched");
         Game c = newGame(1);
         Mob cc = c.mobs.spawn(Mobs.COW, 12.5, GROUND + 1, 12.5);
         hold(c, Items.BUCKET, 1);
         check(!c.useOnMob(cc, c.inv[c.sel]), "creative: nothing");
         // interact(): a right click on a targeted cow with a bucket
         Game d = newGame(0);
         d.world.dayTime = 6000;
         Mob cow2 = d.mobs.spawn(Mobs.COW, 12.5, GROUND + 1, 12.5);
         hold(d, Items.BUCKET, 1);
         d.targetMob = cow2;
         d.target = null;
         d.rightPressed = true;
         d.rightDown = true;
         d.interact();
         eq(Items.MILK_BUCKET, d.inv[d.sel].id, "milked through interact()");
      });

      test("eggs: a grown-up chicken lays an egg every 5-10 minutes, chicks do not", () -> {
         Game g = newGame(0);
         g.mobs.timer = 1;
         g.player.setPos(-30, GROUND + 1, -30);
         Mob hen = g.mobs.spawn(Mobs.CHICKEN, 12.5, GROUND + 1, 12.5);
         int first = -1;
         for (int i = 1; i <= 26000; i++) {
            g.mobs.tickMob(hen, g.world, g.player, 1.0f);
            if (first < 0 && countItems(g.world, Items.EGG) > 0) {
               first = i;
            }
         }
         check(first >= 6000 && first <= 12000, "first egg after " + first + " ticks (6000..12000)");
         int eggs = countItems(g.world, Items.EGG);
         check(eggs >= 2 && eggs <= 4, "2-4 eggs in 26000 ticks (" + eggs + ")");
         Game c = newGame(0);
         c.player.setPos(-30, GROUND + 1, -30);
         Mob chick = c.mobs.spawnBaby(Mobs.CHICKEN, 12.5, GROUND + 1, 12.5);
         chick.babyAge = 100000;
         for (int i = 0; i < 20000; i++) {
            c.mobs.tickMob(chick, c.world, c.player, 1.0f);
         }
         eq(0, countItems(c.world, Items.EGG), "a chick lays no eggs");
         Game p = newGame(0);
         p.player.setPos(-30, GROUND + 1, -30);
         Mob pig = p.mobs.spawn(Mobs.PIG, 12.5, GROUND + 1, 12.5);
         for (int i = 0; i < 14000; i++) {
            p.mobs.tickMob(pig, p.world, p.player, 1.0f);
         }
         eq(0, countItems(p.world, Items.EGG), "pigs lay no eggs");
      });
   }

   // ------------------------------------------------------------------ animals follow food
   static void animals() {
      test("animals follow a player who holds their food, and ignore other items", () -> {
         int[][] likes = {{Mobs.PIG, Items.CARROT}, {Mobs.PIG, Items.POTATO}, {Mobs.COW, Items.WHEAT}, {Mobs.SHEEP, Items.WHEAT}, {Mobs.CHICKEN, 285}};
         for (int[] l : likes) {
            Game g = newGame(0);
            g.mobs.timer = 1;
            g.player.setPos(8.5, GROUND + 1, 8.5);
            hold(g, l[1], 3);
            Mob m = g.mobs.spawn(l[0], 15.5, GROUND + 1, 8.5);
            double d0 = Math.hypot(m.x - g.player.x, m.z - g.player.z);
            for (int i = 0; i < 150; i++) {
               g.mobs.tickMob(m, g.world, g.player, 1.0f);
            }
            double d1 = Math.hypot(m.x - g.player.x, m.z - g.player.z);
            check(d1 < 3.2, Mobs.NAME[l[0]] + " holding-" + l[1] + ": walked from " + d0 + " to within " + d1);
            check(d1 > 1.0, Mobs.NAME[l[0]] + " stops in front of the player, not inside (" + d1 + ")");
         }
         // wrong item: no systematic approach
         Game g = newGame(0);
         g.mobs.timer = 1;
         hold(g, Items.CARROT, 3);
         Mob cow = g.mobs.spawn(Mobs.COW, 15.5, GROUND + 1, 8.5);
         check(!g.mobs.tempted(cow, g.player), "a cow is not tempted by carrots");
         hold(g, Items.WHEAT, 3);
         check(g.mobs.tempted(cow, g.player), "a cow is tempted by wheat");
         g.player.setPos(30.5, GROUND + 1, 8.5);
         check(!g.mobs.tempted(cow, g.player), "but not from far away (> 9 blocks)");
         g.player.setPos(8.5, GROUND + 1, 8.5);
         g.inv[g.sel] = null;
         check(!g.mobs.tempted(cow, g.player), "nor with an empty hand");
         g.player.setMode(3);
         hold(g, Items.WHEAT, 3);
         check(!g.mobs.tempted(cow, g.player), "nor when the player is a spectator");
      });

      test("animals in love / fleeing ignore the food; a tempted animal that is fed goes into love mode as before", () -> {
         Game g = newGame(0);
         g.mobs.timer = 1;
         hold(g, Items.WHEAT, 5);
         Mob cow = g.mobs.spawn(Mobs.COW, 15.5, GROUND + 1, 8.5);
         cow.flee = 40;
         cow.fleeX = 8.5;
         cow.fleeZ = 8.5;
         double d0 = Math.hypot(cow.x - 8.5, cow.z - 8.5);
         for (int i = 0; i < 30; i++) {
            g.mobs.tickMob(cow, g.world, g.player, 1.0f);
         }
         check(Math.hypot(cow.x - 8.5, cow.z - 8.5) > d0, "a fleeing cow keeps running away from the player");
         Mob calf = g.mobs.spawnBaby(Mobs.COW, 10.5, GROUND + 1, 8.5);
         check(g.useOnMob(calf, g.inv[g.sel]), "feeding still works on a calf");
         Mob c2 = g.mobs.spawn(Mobs.COW, 11.5, GROUND + 1, 8.5);
         check(g.useOnMob(c2, g.inv[g.sel]), "feeding still works on an adult");
         eq(600, c2.love, "love mode");
      });
   }

   // ------------------------------------------------------------------ fire
   static void fire() {
      test("fire: flint and steel lights the cell next to the clicked face (1 durability); not in mid-air, not into solid blocks", () -> {
         Game g = newGame(0);
         hold(g, Items.FLINT_STEEL, 1);
         g.target = top(20, 20);
         check(g.useSpecial(g.inv[g.sel]), "lit a fire");
         eq(Blocks.FIRE, blockAbove(g, 20, 20), "fire above the grass");
         eq(1, g.inv[g.sel].dmg, "durability used");
         eq(1, g.world.fire.count(), "registered");
         eq(0, g.world.fire.age(20, GROUND + 1, 20), "age 0");
         g.target = hit(20, GROUND + 1, 20, 0, 1, 0);
         check(!g.useSpecial(g.inv[g.sel]), "no fire on top of a fire (nothing to stand on)");
         g.target = hit(20, GROUND, 20, 1, 0, 0);
         check(!g.useSpecial(g.inv[g.sel]), "no fire inside a solid block");
         g.target = hit(30, GROUND + 5, 30, 0, 1, 0);
         check(!g.useSpecial(g.inv[g.sel]), "no fire floating in the air");
         eq(1, g.inv[g.sel].dmg, "failed attempts cost nothing");
         // sides of a block with something solid below the target cell
         g.target = hit(25, GROUND, 25, -1, 0, 0);
         check(!g.useSpecial(g.inv[g.sel]), "the cell beside ground level is grass: not empty");
         g.world.setBlock(25, GROUND + 1, 25, Blocks.STONE);
         g.target = hit(25, GROUND + 1, 25, -1, 0, 0);
         check(g.useSpecial(g.inv[g.sel]), "fire next to a stone block, on the grass");
         eq(Blocks.FIRE, g.world.getBlock(24, GROUND + 1, 25), "fire on the grass beside the stone");
         Game c = newGame(1);
         hold(c, Items.FLINT_STEEL, 1);
         c.target = top(20, 20);
         check(c.useSpecial(c.inv[c.sel]), "creative can light fires");
         eq(0, c.inv[c.sel].dmg, "creative: no wear");
         Game a = newGame(2);
         hold(a, Items.FLINT_STEEL, 1);
         a.target = top(20, 20);
         check(!a.useSpecial(a.inv[a.sel]), "adventure mode cannot");
         // TNT still works as before
         Game t = newGame(0);
         t.world.setBlock(30, GROUND + 1, 30, Blocks.TNT);
         hold(t, Items.FLINT_STEEL, 1);
         t.target = hit(30, GROUND + 1, 30, 0, 1, 0);
         check(t.useSpecial(t.inv[t.sel]), "TNT is primed");
         eq(0, t.world.fire.count(), "no fire from priming TNT");
      });

      test("fire: on bare ground it burns out by itself after a while (age > 3), leaving nothing", () -> {
         Game g = newGame(0);
         g.world.fire.rnd.setSeed(11);
         check(g.world.fire.ignite(20, GROUND + 1, 20), "lit");
         int t = tickUntil(g.world, 6000, () -> g.world.getBlock(20, GROUND + 1, 20) != Blocks.FIRE);
         check(t >= 60 && t <= 3000, "burned out after " + t + " ticks (expected roughly 100-1500)");
         eq(0, g.world.fire.count(), "registry is empty again");
         eq(Blocks.GRASS, g.world.getBlock(20, GROUND, 20), "the grass below is untouched");
         // several fires age differently but all end
         Game h = newGame(0);
         h.world.fire.rnd.setSeed(12);
         for (int i = 0; i < 6; i++) {
            h.world.fire.ignite(20 + i * 3, GROUND + 1, 20);
         }
         eq(6, h.world.fire.count(), "six fires");
         int t2 = tickUntil(h.world, 8000, () -> h.world.fire.count() == 0);
         check(t2 >= 0, "all six burned out (" + t2 + ")");
      });

      test("fire: ages 0..15, spreads through planks and logs, consumes them and finally dies", () -> {
         Game g = newGame(0);
         g.world.fire.rnd.setSeed(5);
         for (int x = 20; x < 25; x++) {
            for (int z = 20; z < 25; z++) {
               g.world.setBlock(x, GROUND + 1, z, Blocks.PLANKS);
               g.world.setBlock(x, GROUND + 2, z, x % 2 == 0 ? Blocks.LOG_OAK : Blocks.PLANKS);
            }
         }
         int before = count(g.world, 20, GROUND + 1, 20, 24, GROUND + 2, 24, Blocks.PLANKS) + count(g.world, 20, GROUND + 1, 20, 24, GROUND + 2, 24, Blocks.LOG_OAK);
         eq(50, before, "50 wooden blocks");
         check(g.world.fire.ignite(22, GROUND + 3, 22), "fire on top of the wood");
         int maxFires = 0;
         int maxAge = 0;
         for (int i = 0; i < 40000; i++) {
            g.world.tick();
            int n = g.world.fire.count();
            maxFires = Math.max(maxFires, n);
            if (i % 50 == 0) {
               for (java.util.Map.Entry<Long, long[]> e : g.world.fire.cells.entrySet()) {
                  maxAge = Math.max(maxAge, (int)e.getValue()[0]);
               }
            }
            if (n == 0 && i > 100) {
               break;
            }
         }
         int after = count(g.world, 20, GROUND + 1, 20, 24, GROUND + 2, 24, Blocks.PLANKS) + count(g.world, 20, GROUND + 1, 20, 24, GROUND + 2, 24, Blocks.LOG_OAK);
         check(maxFires >= 3, "the fire spread (up to " + maxFires + " fires at once)");
         check(maxAge >= 4 && maxAge <= 15, "fires get older but never beyond 15 (max seen " + maxAge + ")");
         check(after <= before * 0.5, "most of the wood burned: " + after + " of " + before + " left");
         eq(0, g.world.fire.count(), "the fire died when nothing was left to burn");
         eq(0, count(g.world, 18, GROUND + 1, 18, 26, GROUND + 6, 26, Blocks.FIRE), "no fire blocks left in the world");
      });

      test("fire: burning TNT next to a fire is primed; /firespread off freezes fires, on resumes them", () -> {
         Game g = newGame(0);
         g.world.fire.rnd.setSeed(9);
         g.world.setBlock(21, GROUND + 1, 20, Blocks.TNT);
         check(g.world.fire.ignite(20, GROUND + 1, 20), "fire next to TNT");
         int t = tickUntil(g.world, 20000, () -> g.world.getBlock(21, GROUND + 1, 20) != Blocks.TNT);
         check(t >= 0, "the TNT was lit by the fire after " + t + " ticks");
         // frozen fire
         Game h = newGame(0);
         h.world.fire.rnd.setSeed(10);
         h.runCommand("/firespread off");
         check(!h.world.fire.spread, "spread is off");
         h.world.setBlock(21, GROUND + 1, 20, Blocks.PLANKS);
         check(h.world.fire.ignite(20, GROUND + 1, 20), "lit");
         tick(h.world, 5000);
         eq(Blocks.FIRE, h.world.getBlock(20, GROUND + 1, 20), "the fire is still there after 5000 ticks");
         eq(Blocks.PLANKS, h.world.getBlock(21, GROUND + 1, 20), "and did not burn the plank next to it");
         h.runCommand("/firespread on");
         check(h.world.fire.spread, "spread is on again");
         int t2 = tickUntil(h.world, 20000, () -> h.world.getBlock(21, GROUND + 1, 20) != Blocks.PLANKS);
         check(t2 >= 0, "now the plank burns (" + t2 + ")");
         h.runCommand("/firespread");
         check(h.world.fire.spread, "no argument keeps the setting");
      });

      test("fire: water puts it out (the block is replaced), blocks can be placed into it, fire needs support", () -> {
         Game g = newGame(0);
         g.world.fire.ignite(20, GROUND + 1, 20);
         eq(1, g.world.fire.count(), "one fire");
         g.world.setBlock(20, GROUND + 1, 20, WATER);
         eq(0, g.world.fire.count(), "water replaced the fire");
         // placing a block into a fire replaces it
         g.world.fire.ignite(22, GROUND + 1, 20);
         hold(g, Blocks.STONE, 5);
         g.target = hit(22, GROUND + 1, 20, 0, 1, 0);
         check(g.placeBlock(), "placed into the fire");
         eq(Blocks.STONE, g.world.getBlock(22, GROUND + 1, 20), "stone where the fire was");
         eq(0, g.world.fire.count(), "fire gone");
         // fire without support goes out: ground removed
         g.world.fire.ignite(24, GROUND + 1, 20);
         g.world.setBlock(24, GROUND, 20, 0);
         tick(g.world, 3);
         eq(0, g.world.getBlock(24, GROUND + 1, 20), "fire over a hole goes out");
         // a left click puts a fire out and drops nothing
         g.world.fire.ignite(26, GROUND + 1, 20);
         g.breakBlock(26, GROUND + 1, 20);
         eq(0, g.world.getBlock(26, GROUND + 1, 20), "punched out");
         eq(0, g.world.fire.count(), "registry empty");
         eq(0, g.world.items.size(), "no drops");
      });

      test("fire: chunks that contain fire register it when they load; unloaded fire is forgotten", () -> {
         Game g = newGame(0);
         Chunk c = new Chunk(30, 30);
         c.set(3, 70, 4, Blocks.FIRE);
         c.set(15, 200, 0, Blocks.FIRE);
         g.world.fire.scanChunk(c);
         eq(2, g.world.fire.count(), "two fires found");
         eq(0, g.world.fire.age(30 * 16 + 3, 70, 30 * 16 + 4), "age 0");
         eq(0, g.world.fire.age(30 * 16 + 15, 200, 30 * 16), "age 0");
         tick(g.world, 100); // that chunk is not loaded: the entries are dropped when they come due
         eq(0, g.world.fire.count(), "forgotten while unloaded");
      });

      test("fire: lava sets fire to wool and wood next to it, and the occasional random tick near the player does it too", () -> {
         Game g = newGame(0);
         g.world.fire.rnd.setSeed(2);
         g.world.setBlock(20, GROUND + 1, 20, Blocks.LAVA);
         g.world.setBlock(21, GROUND + 1, 20, Blocks.WOOL0);
         boolean lit = false;
         for (int i = 0; i < 400 && !lit; i++) {
            g.world.fire.igniteAround(20, GROUND + 1, 20);
            lit = g.world.getBlock(21, GROUND + 2, 20) == Blocks.FIRE || g.world.getBlock(20, GROUND + 2, 20) == Blocks.FIRE || count(g.world, 18, GROUND + 1, 18, 23, GROUND + 4, 23, Blocks.FIRE) > 0;
         }
         check(lit, "fire appeared next to the lava");
         // stone-only surroundings: lava never lights anything
         Game h = newGame(0);
         h.world.fire.rnd.setSeed(3);
         h.world.setBlock(20, GROUND + 1, 20, Blocks.LAVA);
         for (int i = 0; i < 500; i++) {
            h.world.fire.igniteAround(20, GROUND + 1, 20);
         }
         eq(0, h.world.fire.count(), "no fuel, no fire");
         // the random sampling around the player finds the lava source
         Game k = newGame(0);
         k.world.fire.rnd.setSeed(4);
         k.world.setBlock(20, GROUND + 1, 20, Blocks.LAVA);
         k.world.setBlock(21, GROUND + 1, 20, Blocks.PLANKS);
         k.world.setBlock(19, GROUND + 1, 20, Blocks.PLANKS);
         int t = tickUntil(k.world, 60000, () -> k.world.fire.count() > 0 || k.world.getBlock(21, GROUND + 1, 20) != Blocks.PLANKS);
         check(t >= 0, "random lava ticks started a fire after " + t + " ticks");
      });

      test("fire: the player standing in it burns (1 damage per second, water puts it out, creative is immune)", () -> {
         Game g = newGame(0);
         g.player.food = 10; // below 18: no natural regeneration that would hide the damage
         g.player.sat = 0;
         g.player.setPos(20.5, GROUND + 1, 20.5);
         g.world.setBlock(20, GROUND + 1, 20, Blocks.FIRE);
         g.player.tick(g.world, 0, 0, false, false, false);
         check(g.player.burn >= 150, "set alight (" + g.player.burn + ")");
         int hp = g.player.health;
         check(hp < 20, "hurt at once (" + hp + ")");
         g.world.setBlock(20, GROUND + 1, 20, 0);
         for (int i = 0; i < 100; i++) {
            g.player.tick(g.world, 0, 0, false, false, false);
         }
         check(g.player.health < hp, "still burning after leaving the fire: " + g.player.health + " < " + hp);
         check(g.player.burn > 0 && g.player.burn < 100, "burn counter runs down (" + g.player.burn + ")");
         // water
         g.world.setBlock(20, GROUND + 1, 20, WATER);
         g.world.setBlock(20, GROUND + 2, 20, WATER);
         g.player.setPos(20.5, GROUND + 1, 20.5);
         g.player.tick(g.world, 0, 0, false, false, false);
         eq(0, g.player.burn, "water puts the flames out");
         // it burns out by itself: no further damage
         Game h = newGame(0);
         h.player.food = 10;
         h.player.sat = 0;
         h.player.burn = 100;
         int hp2 = h.player.health;
         for (int i = 0; i < 400; i++) {
            h.player.tick(h.world, 0, 0, false, false, false);
         }
         eq(0, h.player.burn, "burn ran out");
         eq(hp2 - 5, h.player.health, "5 damage for 100 ticks of burning");
         Game c = newGame(1);
         c.player.setPos(20.5, GROUND + 1, 20.5);
         c.world.setBlock(20, GROUND + 1, 20, Blocks.FIRE);
         c.player.tick(c.world, 0, 0, false, false, false);
         eq(0, c.player.burn, "creative does not burn");
         eq(20, c.player.health, "creative takes no fire damage");
         // lava sets the player alight
         Game l = newGame(0);
         l.world.setBlock(20, GROUND + 1, 20, Blocks.LAVA);
         l.player.setPos(20.5, GROUND + 1, 20.5);
         l.player.tick(l.world, 0, 0, false, false, false);
         check(l.player.burn >= 250, "lava sets the player on fire (" + l.player.burn + ")");
         // respawn clears it
         l.player.dead = true;
         l.respawn();
         eq(0, l.player.burn, "respawn extinguishes");
      });

      test("fire: animals standing in it burn, take damage and drop cooked meat when they die", () -> {
         Game g = newGame(0);
         g.mobs.timer = 1;
         g.player.setPos(-30, GROUND + 1, -30);
         Mob pig = g.mobs.spawn(Mobs.PIG, 20.5, GROUND + 1, 20.5);
         pig.health = 4;
         g.world.setBlock(20, GROUND + 1, 20, Blocks.FIRE);
         for (int i = 0; i < 40 && !pig.dead; i++) {
            g.mobs.tickMob(pig, g.world, g.player, 1.0f);
         }
         check(pig.burn > 0 || pig.dead, "the pig is alight");
         for (int i = 0; i < 300 && !pig.dead; i++) {
            g.mobs.tickMob(pig, g.world, g.player, 1.0f);
         }
         check(pig.dead, "the burning pig died");
         check(countItems(g.world, 273) >= 1, "cooked porkchop dropped");
         eq(0, countItems(g.world, 272), "no raw porkchop");
         Game h = newGame(0);
         h.mobs.timer = 1;
         h.player.setPos(-30, GROUND + 1, -30);
         Mob safe = h.mobs.spawn(Mobs.COW, 20.5, GROUND + 1, 20.5);
         for (int i = 0; i < 300; i++) {
            h.mobs.tickMob(safe, h.world, h.player, 1.0f);
         }
         check(!safe.dead && safe.burn == 0, "a cow away from fire is fine");
         // lava
         Game l = newGame(0);
         l.player.setPos(-30, GROUND + 1, -30);
         Mob cow = l.mobs.spawn(Mobs.COW, 20.5, GROUND + 1, 20.5);
         l.world.setBlock(20, GROUND + 1, 20, Blocks.LAVA);
         l.mobs.tickMob(cow, l.world, l.player, 1.0f);
         check(cow.burn >= 150, "lava sets creatures alight (" + cow.burn + ")");
      });

      test("fire: a forest fire at the edge of the loaded world: no crash, nothing spreads into unloaded chunks, it dies down", () -> {
         Game g = newGame(0);
         g.world.fire.rnd.setSeed(21);
         // a grove of logs with leaves on top, right at the east edge of the loaded area (chunks -2..2 -> x up to 47)
         for (int x = 36; x <= 47; x++) {
            for (int z = 36; z <= 47; z++) {
               if ((x + z) % 3 == 0) {
                  for (int y = 1; y <= 4; y++) {
                     g.world.setBlock(x, GROUND + y, z, Blocks.LOG_OAK);
                  }
               }
               for (int y = 5; y <= 6; y++) {
                  g.world.setBlock(x, GROUND + y, z, Blocks.LEAVES_OAK);
               }
            }
         }
         check(g.world.fire.ignite(40, GROUND + 7, 40), "lit the canopy");
         long start = System.nanoTime();
         int peak = 0;
         for (int i = 0; i < 12000 && (i < 200 || g.world.fire.count() > 0); i++) {
            g.world.tick();
            peak = Math.max(peak, g.world.fire.count());
         }
         long ms = (System.nanoTime() - start) / 1000000L;
         check(peak >= 10, "a real forest fire: up to " + peak + " fires at once");
         check(ms < 20000, "and it is cheap enough to simulate: 12000 ticks took " + ms + " ms");
         eq(0, g.world.fire.count(), "the fire died down");
         int leaves = count(g.world, 36, GROUND + 5, 36, 47, GROUND + 6, 47, Blocks.LEAVES_OAK);
         check(leaves < 288 * 0.7, "most leaves burned (" + leaves + " of 288 left)");
         eq(0, g.world.getBlock(48, GROUND + 5, 40) + g.world.getBlock(50, GROUND + 5, 40), "nothing exists beyond the loaded area");
      });

      test("fire: dropped items burn up in a fire; /firespread is saved with the world", () -> {
         Game g = newGame(0);
         g.world.fire.ignite(20, GROUND + 1, 20);
         g.world.dropItem(20.5, GROUND + 1.2, 20.5, Blocks.PLANKS, 3);
         g.world.dropItem(24.5, GROUND + 1.2, 20.5, Blocks.PLANKS, 3);
         eq(6, countItems(g.world, Blocks.PLANKS), "two stacks lying around");
         g.world.setBlock(20, GROUND + 1, 20, Blocks.FIRE); // (already burning)
         tick(g.world, 3);
         eq(3, countItems(g.world, Blocks.PLANKS), "the stack in the fire burned up, the other one is fine");
         // level.properties round trip
         java.util.Properties p = new java.util.Properties();
         g.storeFireSetting(p);
         eq("true", p.getProperty("firespread"), "stored: on");
         g.runCommand("/firespread off");
         g.storeFireSetting(p);
         eq("false", p.getProperty("firespread"), "stored: off");
         Game h = newGame(0);
         h.restoreFireSetting(p);
         check(!h.world.fire.spread, "a world saved with fire off loads with fire off");
         h.restoreFireSetting(new java.util.Properties());
         check(h.world.fire.spread, "worlds from older versions (no setting) have fire on, like Minecraft");
      });

      test("fire: the visuals - 16 distinct animation frames with transparency, particles of a fire block", () -> {
         Set<Long> sums = new HashSet<>();
         for (int f = 0; f < 16; f++) {
            int[] p = TexArt37.fireFrame(f);
            long sum = 0;
            int opaque = 0;
            for (int i = 0; i < 256; i++) {
               int a = p[i] >>> 24;
               check(a == 0 || a == 255, "frame " + f + " pixel " + i + " is fully transparent or opaque");
               if (a == 255) {
                  opaque++;
                  sum = sum * 31 + (p[i] & 0xFFFFFF) + i;
               }
            }
            check(opaque > 40 && opaque < 230, "frame " + f + " has flames and empty space (" + opaque + " pixels)");
            check((p[15 * 16 + 7] >>> 24) == 255 && (p[15 * 16 + 8] >>> 24) == 255, "frame " + f + ": the flames stand on the bottom edge");
            sums.add(sum);
         }
         check(sums.size() >= 14, "the animation has many different frames (" + sums.size() + ")");
         Game g = newGame(0);
         g.world.fire.ignite(20, GROUND + 1, 20);
         g.player.setPos(18.5, GROUND + 1, 18.5);
         int before = g.particles.size();
         for (int i = 0; i < 90; i++) {
            g.world.tick();
            g.world.fire.particles(g);
         }
         check(g.particles.size() > before, "a fire sends up sparks and smoke (" + g.particles.size() + ")");
      });
   }

   // ------------------------------------------------------------------ worldgen
   static void worldgen() {
      test("worldgen: sugar cane only on shores with water at the same level, never above 3; pumpkin patches on grass; everything else unchanged", () -> {
         long seed = 12345L;
         Gen gen = new Gen(seed);
         int canes = 0;
         int stacks = 0;
         int pumpkins = 0;
         int diff = 0;
         for (int cz = -6; cz <= 14; cz++) {
            for (int cx = -2; cx <= 22; cx++) {
               Gen.extras = false;
               Chunk ref = gen.generate(cx, cz);
               Gen.extras = true;
               Chunk now = gen.generate(cx, cz);
               for (int i = 0; i < ref.blocks.length; i++) {
                  int a = ref.blocks[i] & 255;
                  int b = now.blocks[i] & 255;
                  if (a != b) {
                     diff++;
                     check(b == Blocks.SUGAR_CANE || b == Blocks.PUMPKIN, "only cane / pumpkins were added (" + b + " replaced " + a + ")");
                     check(a == 0 || a == Blocks.TALLGRASS, "...into empty air (or tall grass), never replacing a solid block (" + a + ")");
                     int x = i & 15;
                     int z = i >> 4 & 15;
                     int y = i >> 8;
                     if (b == Blocks.SUGAR_CANE) {
                        canes++;
                        int below = now.blocks[Chunk.idx(x, y - 1, z)] & 255;
                        if (below != Blocks.SUGAR_CANE) {
                           stacks++;
                           check(y == 63 && below == Blocks.SAND, "cane stacks start at y=63 on sand (y=" + y + ", below " + below + ")");
                           boolean water = false;
                           int[][] around = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
                           for (int[] d : around) {
                              int nx = x + d[0];
                              int nz = z + d[1];
                              if (nx >= 0 && nx < 16 && nz >= 0 && nz < 16 && Blocks.isWater(now.blocks[Chunk.idx(nx, y - 1, nz)] & 255)) {
                                 water = true;
                              }
                           }
                           check(water, "every cane stack has water next to its ground block");
                        }
                        check(y <= 65, "cane is at most 3 high");
                     } else {
                        pumpkins++;
                        eq(Blocks.GRASS, now.blocks[Chunk.idx(x, y - 1, z)] & 255, "pumpkins sit on grass");
                     }
                  }
               }
            }
         }
         Gen.extras = true;
         System.out.println("          (worldgen sample: " + stacks + " cane stacks, " + canes + " cane blocks, " + pumpkins + " pumpkins, " + diff + " changed blocks in 525 chunks)");
         check(stacks >= 3, "found sugar cane stacks on the shores (" + stacks + ")");
         check(pumpkins >= 3, "found pumpkins (" + pumpkins + ")");
      });

      test("worldgen: deterministic (the same chunk twice gives the same blocks)", () -> {
         Gen gen = new Gen(777L);
         for (int c = 0; c < 6; c++) {
            Chunk a = gen.generate(c * 3, -c);
            Chunk b = gen.generate(c * 3, -c);
            check(java.util.Arrays.equals(a.blocks, b.blocks), "chunk " + c + " identical");
         }
      });
   }

   // ------------------------------------------------------------------ meshing
   static void meshing() {
      test("meshing: every new block id produces geometry (cross plants 8 vertices, cubes 20, boxes more), nothing throws", () -> {
         Game g = newGame(1);
         World w = g.world;
         Chunk c = w.chunk(0, 0);
         for (int id = Blocks.SUGAR_CANE; id < Blocks.COUNT; id++) {
            w.setBlock(5, GROUND + 1, 5, id);
            Mesher.Result r = Mesher.build(w, c, 3, false);
            int verts = r.nO;
            w.setBlock(5, GROUND + 1, 5, 0);
            Mesher.Result base = Mesher.build(w, c, 3, false);
            int added = verts - base.nO;
            if (r.opaque != null) {
               org.lwjgl.system.MemoryUtil.memFree(r.opaque);
            }
            if (r.trans != null) {
               org.lwjgl.system.MemoryUtil.memFree(r.trans);
            }
            if (base.opaque != null) {
               org.lwjgl.system.MemoryUtil.memFree(base.opaque);
            }
            if (base.trans != null) {
               org.lwjgl.system.MemoryUtil.memFree(base.trans);
            }
            if (Blocks.kind[id] == 1) {
               eq(8, added, "block " + id + " (" + Blocks.name[id] + ") cross-shaped: 2 quads");
            } else if (Blocks.kind[id] == 0) {
               eq(16, added, "block " + id + " (" + Blocks.name[id] + ") opaque cube: 5 faces, minus the grass top it covers");
            } else {
               eq(20, added, "block " + id + " (" + Blocks.name[id] + ") box: 5 faces (the bottom touches the grass)");
            }
         }
      });
   }
}
