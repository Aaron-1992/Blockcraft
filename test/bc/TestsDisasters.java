package bc;

import static bc.GameTests.GROUND;
import static bc.GameTests.check;
import static bc.GameTests.eq;
import static bc.GameTests.newGame;
import static bc.GameTests.test;

import bc.Mobs.Mob;
import java.util.Properties;

/**
 * Headless tests of the natural disasters (To do item 5, Update 3.10): tornadoes, earthquakes, meteors.   python3 tools/runtests.py unit
 * The simulation is deterministic (every world seeds its own random stream), so the statistical checks use generous bounds.
 */
final class TestsDisasters {
   static final int CX = 24;
   static final int CZ = 24;

   static void run() {
      tables();
      ground();
      tornadoLife();
      tornadoBlocks();
      tornadoEntities();
      debris();
      quakes();
      meteors();
      natural();
      commands();
      saving();
   }

   static Game game(int mode) throws Exception {
      Game g = newGame(mode);
      g.opt = new Options(new java.io.File(System.getProperty("java.io.tmpdir"))); // (the test game has no options: never saved here)
      g.world.disasters.enabled = true;
      return g;
   }

   static void ticks(Game g, int n) {
      for (int i = 0; i < n; i++) {
         g.world.tick();
      }
   }

   // ------------------------------------------------------------------ rules for blocks
   static void tables() {
      test("what a tornado takes: plants, leaves, glass, wool, wood work; terrain, stone, ores, chests, furnaces, logs, fire do not", () -> {
         for (int id : new int[]{12, 13, 14, 29, 30, 31, Blocks.TORCH, Blocks.SUGAR_CANE, 124, 131, Blocks.LADDER}) {
            eq(1.0F, Disasters.fragility(id), "block " + id + " goes at once");
         }

         check(Disasters.fragility(Blocks.GLASS) > 0.8F, "glass breaks easily");
         check(Disasters.fragility(34) > 0.0F && Disasters.fragility(49) > 0.0F, "wool");
         check(Disasters.fragility(Blocks.PLANKS) > 0.0F && Disasters.fragility(Blocks.PLANKS) < 0.5F, "planks need more wind");
         check(Disasters.fragility(Blocks.FENCE) > 0.0F && Disasters.fragility(73) > 0.0F && Disasters.fragility(Blocks.CRAFTING_TABLE) > 0.0F, "fences, doors, crafting tables");
         check(Disasters.fragility(Blocks.PUMPKIN) > 0.0F && Disasters.fragility(Blocks.MELON) > 0.0F && Disasters.fragility(187) > 0.0F, "pumpkins, melons, cake");
         for (int id : new int[]{0, 1, 2, 3, 4, 6, 7, 8, 9, 10, 11, 16, 17, 18, 19, 20, 22, 26, 27, 50, 58, 67, 68, 69, 70, 71, 132, 194, 123}) {
            eq(0.0F, Disasters.fragility(id), "block " + id + " (" + Blocks.name[id] + ") is never lifted");
         }

         check(Disasters.fragility(Blocks.LOG_X0) == 0.0F && Disasters.fragility(Blocks.LOG_Z0 + 2) == 0.0F, "fallen logs stay");
         check(Disasters.crumbles(Blocks.STONE) && Disasters.crumbles(Blocks.DIRT) && Disasters.crumbles(Blocks.GRASS) && Disasters.crumbles(Blocks.SAND) && Disasters.crumbles(Blocks.GRAVEL) && Disasters.crumbles(Blocks.IRON_ORE), "loose terrain crumbles");
         check(!Disasters.crumbles(Blocks.PLANKS) && !Disasters.crumbles(Blocks.BRICKS) && !Disasters.crumbles(Blocks.COBBLE) && !Disasters.crumbles(Blocks.GLASS) && !Disasters.crumbles(Blocks.OBSIDIAN) && !Disasters.crumbles(0), "what people build does not");
         check(Disasters.shakesLoose(29) && Disasters.shakesLoose(Blocks.TORCH) && Disasters.shakesLoose(Blocks.GLASS) && Disasters.shakesLoose(Blocks.LADDER) && !Disasters.shakesLoose(Blocks.STONE) && !Disasters.shakesLoose(0) && !Disasters.shakesLoose(194), "an earthquake shakes plants, torches, ladders and glass loose (not fire)");
      });
   }

   static void ground() {
      test("ground height: leaves, logs and plants are not ground, a water surface is, unloaded chunks answer -1", () -> {
         Game g = game(1);
         World w = g.world;
         Disasters ds = w.disasters;
         TestsFall.clear(g);
         eq(GROUND + 1, ds.ground(CX, CZ), "flat grass: the cell above it");
         TestsFall.plant(w, CX, CZ, 6, 9, 12);
         eq(GROUND + 1, ds.ground(CX, CZ), "under a tree: still the grass");
         eq(GROUND + 1, ds.ground(CX + 2, CZ), "under the crown");
         w.setBlock(CX + 8, GROUND + 1, CZ, 29);
         eq(GROUND + 1, ds.ground(CX + 8, CZ), "tall grass is no ground");
         w.setBlock(CX + 9, GROUND + 1, CZ, Blocks.PLANKS);
         eq(GROUND + 2, ds.ground(CX + 9, CZ), "a plank block is");
         w.setBlock(CX + 10, GROUND, CZ, 50);
         w.setBlock(CX + 10, GROUND - 1, CZ, 50);
         eq(GROUND + 1, ds.ground(CX + 10, CZ), "a lake: its surface");
         eq(-1, ds.ground(900, 900), "no chunk, no answer");
      });
   }

   // ------------------------------------------------------------------ tornado
   static void tornadoLife() {
      test("a tornado: forms over 12 seconds, moves along its heading, ropes out and is gone at the end of its life", () -> {
         Game g = game(1);
         g.player.flying = true;
         World w = g.world;
         Disasters ds = w.disasters;
         TestsFall.clear(g);
         Disasters.Tornado t = ds.spawnTornado(CX + 0.5, CZ + 0.5, 3.0, 0.0, 1.2F, false);
         eq(1, ds.tornadoes.size(), "one tornado");
         eq(1.0, t.dx, "the heading is normalised (x)");
         eq(0.0, t.dz, "the heading is normalised (z)");
         check(t.speed >= 0.075 && t.speed <= 0.12 + 1e-9, "speed " + t.speed + " blocks per tick (1.5 - 2.4 blocks a second: slower than walking)");
         eq(64.0 + 8.0 * 1.2F, t.height, "height from size");
         check(t.life >= 2400 && t.life < 4800, "lives 2 - 4 minutes (" + t.life + " ticks)");
         eq(GROUND + 1.0, t.groundY, "stands on the ground");
         eq(0.0F, t.strength, "no funnel yet");
         float prev = 0.0F;
         boolean monotonic = true;
         for (int i = 0; i < 240; i++) {
            ticks(g, 1);
            monotonic &= t.strength >= prev;
            prev = t.strength;
            if (i == 119) {
               check(t.strength > 0.45F && t.strength < 0.55F, "half grown after 6 s: " + t.strength);
            }
         }

         check(monotonic && t.strength == 1.0F, "fully grown after 12 s");
         double x0 = t.x;
         double z0 = t.z;
         ticks(g, 200);
         double moved = Math.hypot(t.x - x0, t.z - z0);
         check(moved > 8.0 && moved < 30.0, "moved " + moved + " blocks in 10 s");
         check((t.x - x0) / moved > 0.8, "mostly along the heading (+x)");
         // the end of its life
         t.life = t.age + 240;
         float before = t.strength;
         ticks(g, 120);
         check(t.strength < before && t.strength > 0.0F, "ropes out over the last 12 s: " + t.strength);
         ticks(g, 130);
         eq(0, ds.tornadoes.size(), "gone");
         // out of the player's world: removed
         g.player.setPos(8.5, GROUND + 1, 8.5);
         Disasters.Tornado far = ds.spawnTornado(2000.0, 2000.0, 1.0, 0.0, 1.0F, true);
         ticks(g, 2);
         check(!ds.tornadoes.contains(far), "more than 900 blocks from the player: removed");
         ds.stopAll();
      });
   }

   static void tornadoBlocks() {
      test("a tornado tears out leaves, glass, wool and planks, uproots trees, and leaves terrain, stone, bricks, chests, furnaces and obsidian alone", () -> {
         Game g = game(1);
         g.player.flying = true;
         World w = g.world;
         Disasters ds = w.disasters;
         TestsFall.clear(g);
         int[] rowId = new int[]{12, Blocks.GLASS, Blocks.PLANKS, 34, Blocks.STONE, Blocks.BRICKS, Blocks.CHEST, Blocks.FURNACE, Blocks.OBSIDIAN};
         for (int row = 0; row < 9; row++) {
            for (int x = 20; x <= 28; x++) {
               w.setBlock(x, GROUND + 1, 20 + row, rowId[row]);
            }
         }

         for (int z = 20; z <= 28; z++) {
            w.setBlock(35, GROUND + 1, z, Blocks.PLANKS); // out of reach (more than 10 blocks from the axis)
         }

         Disasters.Tornado t = ds.spawnTornado(CX + 0.5, CZ + 0.5, 1.0, 0.0, 1.0F, true);
         t.speed = 0.0;
         t.life = 99999;
         int maxDebris = 0;
         for (int i = 0; i < 900; i++) {
            ticks(g, 1);
            maxDebris = Math.max(maxDebris, ds.debris.size());
         }

         int[] left = new int[9];
         for (int row = 0; row < 9; row++) {
            for (int x = 20; x <= 28; x++) {
               if (w.getBlock(x, GROUND + 1, 20 + row) == rowId[row]) {
                  left[row]++;
               }
            }
         }

         check(left[0] <= 1, "leaves: " + left[0] + " of 9 left");
         check(left[1] <= 2, "glass: " + left[1] + " of 9 left");
         check(left[2] <= 5, "planks: " + left[2] + " of 9 left (some come back down as blocks)");
         check(left[3] <= 2, "wool: " + left[3] + " of 9 left");
         for (int row = 4; row < 9; row++) {
            eq(9, left[row], Blocks.name[rowId[row]] + " is never touched");
         }

         int far = 0;
         for (int z = 20; z <= 28; z++) {
            far += w.getBlock(35, GROUND + 1, z) == Blocks.PLANKS ? 1 : 0;
         }

         eq(9, far, "planks 10 blocks away are untouched");
         int grass = 0;
         for (int x = 8; x <= 40; x++) {
            for (int z = 8; z <= 40; z++) {
               grass += w.getBlock(x, GROUND, z) == Blocks.GRASS ? 1 : 0;
            }
         }

         eq(33 * 33, grass, "not one block of the ground was dug up");
         check(maxDebris > 0 && maxDebris <= Disasters.MAX_DEBRIS, "debris flew (at most " + maxDebris + " at a time, limit " + Disasters.MAX_DEBRIS + ")");
         ds.stopAll();

         // a tree in its way is uprooted and topples away from the funnel
         TestsFall.clear(g);
         TestsFall.plant(w, CX, CZ, 6, 9, 12);
         Disasters.Tornado t2 = ds.spawnTornado(CX + 0.5, CZ + 0.5, 1.0, 0.0, 1.0F, true);
         t2.speed = 0.0;
         t2.life = 99999;
         int n = 0;
         while (w.trees.felled == 0 && n < 6000) {
            ticks(g, 1);
            n++;
         }

         check(w.trees.felled >= 1, "the tree was uprooted within " + n + " ticks");
         ds.stopAll();
      });

      test("uproot: the tree falls away from the point it is torn away from; a log that is no tree base stays", () -> {
         Game g = game(1);
         World w = g.world;
         Disasters ds = w.disasters;
         TestsFall.clear(g);
         TestsFall.plant(w, CX, CZ, 6, 9, 12);
         check(ds.uproot(CX, GROUND + 1, CZ, CX - 5.5, CZ + 0.5), "uprooted");
         eq(1, w.trees.falling().size(), "falling");
         eq(1, w.trees.falling().get(0).dx(), "away from the funnel in the west: to the east");
         ticks(g, 60);
         eq(5, TestsFall.count(w, id -> Blocks.isSideLog(id)), "five logs lie there");
         check(w.items.size() >= 1, "the root log dropped");
         TestsFall.clear(g);
         for (int k = 1; k <= 6; k++) {
            w.setBlock(CX, GROUND + k, CZ, 9);
         }

         check(!ds.uproot(CX, GROUND + 1, CZ, CX - 5.5, CZ), "a pillar without leaves is no tree");
         eq(6, TestsFall.count(w, id -> Blocks.isLog(id)), "and is put back");
         check(!ds.uproot(CX + 3, GROUND + 1, CZ, CX, CZ), "no log there: nothing");
      });
   }

   static void tornadoEntities() {
      test("in the funnel the player and animals are swirled and lifted (at most about 14 blocks), hurt a little; flying players are left alone", () -> {
         Game g = game(0);
         World w = g.world;
         Disasters ds = w.disasters;
         TestsFall.clear(g);
         Disasters.Tornado t = ds.spawnTornado(CX + 0.5, CZ + 0.5, 1.0, 0.0, 1.0F, true);
         t.speed = 0.0;
         t.life = 99999;
         g.player.setPos(CX + 3.5, GROUND + 1, CZ + 0.5);
         Mob cow = g.mobs.spawn(Mobs.COW, CX - 3.5, GROUND + 1, CZ + 0.5);
         double maxY = 0.0;
         double maxCow = 0.0;
         int hp0 = g.player.health;
         int minHp = hp0;
         boolean swirled = false;
         double vx0 = g.player.vx;
         for (int i = 0; i < 300; i++) {
            ticks(g, 1);
            g.player.tick(w, 0.0F, 0.0F, false, false, false);
            g.mobs.tick();
            maxY = Math.max(maxY, g.player.y - (GROUND + 1));
            maxCow = Math.max(maxCow, cow.y - (GROUND + 1));
            swirled |= Math.abs(g.player.z - (CZ + 0.5)) > 1.0;
            minHp = Math.min(minHp, g.player.health);
            if (g.player.dead) {
               break;
            }
         }

         check(swirled, "the player is pushed around the funnel");
         check(maxY > 3.0, "the player was lifted " + maxY + " blocks");
         check(maxY < 30.0, "but not out of sight: " + maxY);
         check(minHp < hp0 || g.player.dead, "and hurt (" + hp0 + " -> " + minHp + " at the worst)");
         check(maxCow > 2.0 || cow.dead, "the cow was lifted too (" + maxCow + ")");
         ds.stopAll();

         // players who fly (creative) are not pulled, spectators neither
         Game gc = game(1);
         gc.player.flying = true;
         gc.player.setPos(CX + 3.5, GROUND + 5, CZ + 0.5);
         Disasters.Tornado t2 = gc.world.disasters.spawnTornado(CX + 0.5, CZ + 0.5, 1.0, 0.0, 1.0F, true);
         t2.speed = 0.0;
         t2.life = 99999;
         ticks(gc, 60);
         eq(0.0, gc.player.vx, "a flying player is not pushed (x)");
         eq(0.0, gc.player.vy, "a flying player is not lifted");
         // far outside the funnel nothing happens either
         Game gf = game(0);
         gf.player.setPos(CX + 30.5, GROUND + 1, CZ + 0.5);
         Disasters.Tornado t3 = gf.world.disasters.spawnTornado(CX + 0.5, CZ + 0.5, 1.0, 0.0, 1.0F, true);
         t3.speed = 0.0;
         t3.life = 99999;
         ticks(gf, 60);
         eq(0.0, gf.player.vx, "30 blocks away: not pushed");
         eq(20, gf.player.health, "and not hurt");
         // items are tossed about
         Game gi = game(0);
         gi.world.dropItem(CX + 3.5, GROUND + 1.2, CZ + 0.5, 5, 1);
         Disasters.Tornado t4 = gi.world.disasters.spawnTornado(CX + 0.5, CZ + 0.5, 1.0, 0.0, 1.0F, true);
         t4.speed = 0.0;
         t4.life = 99999;
         double hi = 0.0;
         for (int i = 0; i < 80; i++) {
            ticks(gi, 1);
            for (World.Item it : gi.world.items) {
               hi = Math.max(hi, it.y - (GROUND + 1));
            }
         }

         check(hi > 1.0, "a dropped item is blown up into the air (" + hi + ")");
      });
   }

   // ------------------------------------------------------------------ debris
   static void debris() {
      test("debris: circles the funnel rising, is flung off along the tangent, comes down within a few seconds and never leaves a trace of itself behind", () -> {
         Game g = game(1);
         g.player.flying = true;
         g.player.setPos(8.5, GROUND + 30, 8.5);
         World w = g.world;
         Disasters ds = w.disasters;
         TestsFall.clear(g);
         Disasters.Tornado t = ds.spawnTornado(CX + 0.5, CZ + 0.5, 1.0, 0.0, 1.0F, true);
         t.speed = 0.0;
         t.life = 99999;
         Disasters.Debris d = new Disasters.Debris();
         d.id = Blocks.PLANKS;
         d.x = d.px = CX + 3.5;
         d.y = d.py = GROUND + 1.5;
         d.z = d.pz = CZ + 0.5;
         d.owner = t;
         d.ang = 0.0;
         d.rad = 3.0;
         d.rfac = 0.7;
         d.orbit = 60;
         ds.debris.add(d);
         double lastY = d.y;
         boolean rising = true;
         double maxRad = 0.0;
         int steps = 0;
         while (!d.thrown && steps < 200) {
            ds.tickDebris();
            steps++;
            rising &= d.y > lastY;
            lastY = d.y;
            maxRad = Math.max(maxRad, Math.hypot(d.x - t.x, d.z - t.z));
         }

         check(d.thrown, "it is flung off after its time in the funnel (" + steps + " ticks)");
         check(rising, "it rose all the time");
         check(maxRad < 14.0 && maxRad > 1.0, "within the funnel's reach while it circled (" + maxRad + ")");
         check(Math.hypot(d.vx, d.vz) > 0.2, "and leaves fast: " + Math.hypot(d.vx, d.vz));
         int guard = 0;
         while (!ds.debris.isEmpty() && guard < 800) {
            ds.tickDebris();
            guard++;
            check(!Double.isNaN(d.x) && !Double.isNaN(d.y), "no NaN");
         }

         check(ds.debris.isEmpty(), "it came down (" + guard + " ticks)");
         // a block lifted from the world becomes at most one debris
         TestsFall.clear(g);
         w.setBlock(CX + 2, GROUND + 1, CZ, Blocks.PLANKS);
         int before = ds.debris.size();
         ds.liftBlock(CX + 2, GROUND + 1, CZ, Blocks.PLANKS, t);
         eq(0, w.getBlock(CX + 2, GROUND + 1, CZ), "taken out of the world");
         eq(before + 1, ds.debris.size(), "and flying (solid blocks always)");
         ds.debris.clear();
         // thrown debris hurts what it hits
         Game gs = game(0);
         Disasters ds2 = gs.world.disasters;
         gs.player.setPos(CX + 0.5, GROUND + 1, CZ + 0.5);
         Disasters.Debris h = new Disasters.Debris();
         h.id = Blocks.PLANKS;
         h.x = h.px = CX - 0.1;
         h.y = h.py = GROUND + 1.5;
         h.z = h.pz = CZ + 0.3;
         h.vx = 0.4;
         h.thrown = true;
         ds2.debris.add(h);
         gs.player.invuln = 0;
         ds2.tickDebris();
         ds2.tickDebris();
         check(gs.player.health == 17, "a flying plank hits the player for 3 (" + gs.player.health + ")");
         check(ds2.debris.isEmpty(), "and is gone");
         // landing: solid blocks drop down as falling blocks (most of them), plants vanish
         Game gl = game(1);
         Disasters ds3 = gl.world.disasters;
         int falling = 0;
         for (int i = 0; i < 60; i++) {
            gl.world.fallers.clear();
            Disasters.Debris l = new Disasters.Debris();
            l.id = Blocks.PLANKS;
            l.px = l.x = CX + 0.5;
            l.py = l.y = GROUND + 3.5;
            l.pz = l.z = CZ + 0.5;
            ds3.land(l);
            falling += gl.world.fallers.size();
            int afterPlank = gl.world.fallers.size();
            Disasters.Debris leaf = new Disasters.Debris();
            leaf.id = 12;
            leaf.px = leaf.x = CX + 0.5;
            leaf.py = leaf.y = GROUND + 3.5;
            leaf.pz = leaf.z = CZ + 0.5;
            ds3.land(leaf);
            eq(afterPlank, gl.world.fallers.size(), "a leaf never becomes a block");
         }

         check(falling > 20 && falling < 55, "about 60 % of landing planks become falling blocks (" + falling + " of 60)");
      });
   }

   // ------------------------------------------------------------------ earthquake
   static void quakes() {
      test("an earthquake: builds up, shakes the camera by distance, shakes plants and glass loose, makes unstable terrain crumble, topples trees, ends", () -> {
         Game g = game(1);
         World w = g.world;
         Disasters ds = w.disasters;
         g.player.flying = true;
         TestsFall.clear(g);
         Disasters.Quake q = ds.startQuake(CX + 0.5, CZ + 0.5, 3.0F, 300);
         eq(14.0 + 21.0, q.radius, "radius from the magnitude");
         eq(1, ds.quakeCount, "counted");
         check(q.intensity() == 0.0, "no shaking at the very start");
         q.age = 20;
         check(q.intensity() > 0.9, "full after a second: " + q.intensity());
         q.age = 150;
         check(q.intensity() > 0.2 && q.intensity() < 0.8, "dying away: " + q.intensity());
         q.age = 0;
         // the camera shake
         g.player.setPos(CX + 10.5, GROUND + 1, CZ + 0.5);
         g.shake = 0.0F;
         ticks(g, 40);
         check(g.shake > 0.3F, "10 blocks from the epicentre the camera shakes hard (" + g.shake + ")");
         Game far = game(1);
         far.player.setPos(CX + 300.5, GROUND + 1, CZ + 0.5);
         far.world.disasters.startQuake(CX + 0.5, CZ + 0.5, 3.0F, 300);
         ticks(far, 60);
         eq(0.0F, far.shake, "300 blocks away you feel nothing");
         Game mid = game(1);
         mid.player.setPos(CX + 60.5, GROUND + 1, CZ + 0.5);
         mid.world.disasters.startQuake(CX + 0.5, CZ + 0.5, 3.0F, 300);
         ticks(mid, 40);
         check(mid.shake > 0.0F && mid.shake < g.shake, "60 blocks away: weaker (" + mid.shake + " < " + g.shake + ")");
         ticks(g, 300);
         check(ds.quake == null, "it ends after its life");

         // shaken loose: flowers, torches, glass, tall grass in range go
         TestsFall.clear(g);
         int[] loose = new int[]{30, 31, 29, Blocks.TORCH, Blocks.GLASS};
         int planted = 0;
         for (int i = 0; i < 30; i++) {
            int x = CX - 12 + (i % 6) * 4;
            int z = CZ - 8 + (i / 6) * 4;
            w.setBlock(x, GROUND + 1, z, loose[i % 5]);
            planted++;
         }

         for (int x = CX - 12; x <= CX + 12; x += 4) {
            w.setBlock(x, GROUND + 1, CZ + 14, Blocks.STONE);
         }

         ds.startQuake(CX + 0.5, CZ + 0.5, 3.0F, 1500);
         ticks(g, 1520);
         int remaining = 0;
         for (int i = 0; i < 30; i++) {
            int x = CX - 12 + (i % 6) * 4;
            int z = CZ - 8 + (i / 6) * 4;
            remaining += w.getBlock(x, GROUND + 1, z) == loose[i % 5] ? 1 : 0;
         }

         check(remaining <= planted / 2, "most of the loose things are gone: " + remaining + " of " + planted + " left");
         int stones = 0;
         for (int x = CX - 12; x <= CX + 12; x += 4) {
            stones += w.getBlock(x, GROUND + 1, CZ + 14) == Blocks.STONE ? 1 : 0;
         }

         eq(7, stones, "stone blocks on flat ground stay");
         int grass = 0;
         for (int x = 8; x <= 40; x++) {
            for (int z = 8; z <= 40; z++) {
               grass += w.getBlock(x, GROUND, z) == Blocks.GRASS ? 1 : 0;
            }
         }

         eq(33 * 33, grass, "flat ground does not crumble: no holes");

         // a plateau: its edges are unstable and fall, its flat top and inner blocks stay
         TestsFall.clear(g);
         w.fallers.clear();
         for (int x = 21; x <= 27; x++) {
            for (int z = 21; z <= 27; z++) {
               for (int y = GROUND + 1; y <= GROUND + 4; y++) {
                  w.setBlock(x, y, z, y == GROUND + 4 ? Blocks.GRASS : Blocks.DIRT);
               }
            }
         }

         ds.startQuake(CX + 0.5, CZ + 0.5, 3.0F, 900);
         int maxFalling = 0;
         for (int i = 0; i < 900; i++) {
            ticks(g, 1);
            maxFalling = Math.max(maxFalling, w.fallers.size());
         }

         check(maxFalling > 0, "blocks fell off the plateau (at most " + maxFalling + " at a time)");
         int top = 0;
         for (int x = 21; x <= 27; x++) {
            for (int z = 21; z <= 27; z++) {
               for (int y = GROUND + 1; y <= GROUND + 4; y++) {
                  top += w.getBlock(x, y, z) != 0 ? 1 : 0;
               }
            }
         }

         check(top < 7 * 7 * 4, "the plateau lost blocks: " + top + " of " + 7 * 7 * 4 + " left");
         check(w.getBlock(CX, GROUND + 1, CZ) == Blocks.DIRT, "its core is untouched");

         // trees topple
         TestsFall.clear(g);
         TestsFall.plant(w, CX, CZ, 6, 9, 12);
         int felledBefore = w.trees.felled;
         int n = 0;
         while (w.trees.felled == felledBefore && n < 6000) {
            if (ds.quake == null) {
               ds.startQuake(CX + 0.5, CZ + 0.5, 3.0F, 300);
            }

            ticks(g, 1);
            n++;
         }

         check(w.trees.felled > felledBefore, "a tree in the quake area toppled (after " + n + " ticks)");

         // out of range: nothing
         Game gq = game(1);
         gq.world.setBlock(CX - 28, GROUND + 1, CZ, Blocks.GLASS);
         gq.world.disasters.startQuake(CX + 30.5, CZ + 0.5, 1.0F, 300);
         ticks(gq, 320);
         eq(Blocks.GLASS, gq.world.getBlock(CX - 28, GROUND + 1, CZ), "a magnitude 1 quake has a radius of 21: glass 58 blocks away is fine");
      });
   }

   // ------------------------------------------------------------------ meteors
   static void meteors() {
      test("a meteor: comes down in a straight line from high up, hits with a blast, sets fires, leaves obsidian, shakes the ground; at an unloaded spot nothing happens", () -> {
         Game g = game(0);
         World w = g.world;
         Disasters ds = w.disasters;
         TestsFall.clear(g);
         g.player.setPos(CX + 3.5, GROUND + 1, CZ + 0.5);
         for (int dy = 1; dy <= 3; dy++) {
            w.setBlock(CX, GROUND + dy, CZ + 2, Blocks.STONE);
         }

         int solid0 = 0;
         for (int x = CX - 6; x <= CX + 6; x++) {
            for (int y = GROUND - 6; y <= GROUND + 3; y++) {
               for (int z = CZ - 6; z <= CZ + 6; z++) {
                  solid0 += w.getBlock(x, y, z) != 0 ? 1 : 0;
               }
            }
         }

         Disasters.Meteor m = ds.spawnMeteor(CX + 0.5, CZ + 0.5, 1.0, 0.0, 40, 1.5F);
         eq(1, ds.meteors.size(), "one meteor");
         check(m.y > GROUND + 100.0 && Math.abs(m.x - (CX + 0.5 + 90.0)) < 1e-6, "it starts 90 blocks off and more than 100 up");
         check(m.vy < 0.0 && m.vx < 0.0, "and comes down towards us");
         double x0 = m.x;
         double y0 = m.y;
         ticks(g, 10);
         check(Math.abs(m.x - (x0 + 10 * m.vx)) < 1e-6 && Math.abs(m.y - (y0 + 10 * m.vy)) < 1e-6, "a straight line");
         eq(10, m.trailLen, "its tail is made of the last positions");
         check(Math.abs(m.trail[0] - (m.x - m.vx)) < 1e-4, "the newest tail point is where it was one tick ago");
         int hp0 = g.player.health;
         ticks(g, 35);
         eq(0, ds.meteors.size(), "it has hit");
         eq(1, ds.impacts, "one impact");
         int solid1 = 0;
         int obsidian = 0;
         for (int x = CX - 6; x <= CX + 6; x++) {
            for (int y = GROUND - 6; y <= GROUND + 3; y++) {
               for (int z = CZ - 6; z <= CZ + 6; z++) {
                  int id = w.getBlock(x, y, z);
                  solid1 += id != 0 ? 1 : 0;
                  obsidian += id == Blocks.OBSIDIAN ? 1 : 0;
               }
            }
         }

         check(solid1 < solid0 - 150, "a crater: " + (solid0 - solid1) + " blocks gone");
         eq(Blocks.AIR, w.getBlock(CX, GROUND + 2, CZ + 2), "the stone pillar next to the impact is gone");
         check(obsidian >= 1 && obsidian <= 3, "1 to 3 blocks of obsidian are left: " + obsidian);
         check(w.fire.count() >= 1, "fires around the crater: " + w.fire.count());
         check(g.player.health < hp0, "the player 3 blocks from it was hurt (" + hp0 + " -> " + g.player.health + ")");
         check(g.shake > 0.5F, "the ground thumps: shake " + g.shake);

         // a long way off: nothing happens to anybody
         Game far = game(0);
         far.player.setPos(CX - 100.0, GROUND + 1, CZ);
         Disasters ds2 = far.world.disasters;
         ds2.spawnMeteor(CX + 0.5, CZ + 0.5, 0.0, 1.0, 30, 1.0F);
         ticks(far, 40);
         eq(1, ds2.impacts, "it hit");
         eq(20, far.player.health, "but 100 blocks away it is only a light");
         check(far.shake < 0.5F, "a distant thump: " + far.shake);

         // not loaded: nothing to hit
         Game none = game(0);
         Disasters ds3 = none.world.disasters;
         ds3.spawnMeteor(2000.5, 2000.5, 1.0, 0.0, 30, 1.0F);
         ticks(none, 40);
         eq(0, ds3.meteors.size(), "burnt out");
         eq(0, ds3.impacts, "without an impact in a place that does not exist");
      });

      test("meteor shower: the number of meteors counts down, one after the other, and stopAll ends it", () -> {
         Game g = game(0);
         g.player.setPos(8.5, GROUND + 1, 8.5);
         World w = g.world;
         Disasters ds = w.disasters;
         ds.startShower(5, 10);
         eq(5, ds.showerLeft, "five to come");
         ticks(g, 8);
         eq(5, ds.showerLeft, "not before the delay");
         ticks(g, 4);
         eq(4, ds.showerLeft, "the first one is launched");
         long next = ds.showerAt;
         check(next - ds.clock >= 60 && next - ds.clock <= 200, "the next in 3 - 10 seconds: " + (next - ds.clock));
         ticks(g, 2000);
         eq(0, ds.showerLeft, "all launched after a while");
         check(ds.meteorCount <= 5, "not more than five (spots that are not loaded are skipped): " + ds.meteorCount);
         ds.startShower(8, 10);
         ds.stopAll();
         eq(0, ds.showerLeft, "stopAll ends the shower");
         eq(0, ds.meteors.size(), "and the meteors in the sky");
      });
   }

   // ------------------------------------------------------------------ the natural ones
   static void natural() {
      test("natural disasters: a quake some minutes in, a tornado under a thunderstorm, a meteor shower on some nights - only when switched on", () -> {
         // earthquake
         Game g = game(1);
         World w = g.world;
         Disasters ds = w.disasters;
         g.player.setPos(CX + 0.5, GROUND + 1, CZ + 0.5);
         check(ds.nextQuake >= 12000L && ds.nextQuake < 36000L, "the first quake is 10 - 30 minutes away: " + ds.nextQuake);
         check(ds.nextTornado >= 9000L && ds.nextTornado < 18000L, "the first tornado is possible after 7.5 - 15 minutes: " + ds.nextTornado);
         ticks(g, 2000);
         eq(0, ds.quakeCount, "nothing in the first minutes");
         ds.nextQuake = ds.clock;
         ticks(g, 60);
         eq(1, ds.quakeCount, "the quake starts");
         check(ds.quake != null && Math.hypot(ds.quake.x - g.player.x, ds.quake.z - g.player.z) <= 31.0, "near the player");
         check(ds.quake.magnitude >= 1.0F && ds.quake.magnitude <= 3.0F, "magnitude " + ds.quake.magnitude);
         check(ds.nextQuake >= ds.clock + 24000L - 61, "the next one is at least 20 minutes later");
         check(g.chatLog.contains("The ground is shaking!"), "the player is told");
         ds.stopAll();

         // switched off: world setting, option
         Game off = game(1);
         off.player.setPos(CX + 0.5, GROUND + 1, CZ + 0.5);
         off.world.disasters.enabled = false;
         off.world.disasters.nextQuake = 0;
         ticks(off, 100);
         eq(0, off.world.disasters.quakeCount, "/disaster off: nothing happens by itself");
         Game opt = game(1);
         opt.player.setPos(CX + 0.5, GROUND + 1, CZ + 0.5);
         opt.opt.disasters = false;
         opt.world.disasters.nextQuake = 0;
         ticks(opt, 100);
         eq(0, opt.world.disasters.quakeCount, "Options > Disasters: OFF: nothing happens by itself");
         check(!opt.world.disasters.active(), "inactive");

         // tornado: only under a storm
         Game clear = game(1);
         clear.player.setPos(CX + 0.5, GROUND + 1, CZ + 0.5);
         clear.world.weather.enabled = true;
         clear.world.weather.calm = true;
         clear.world.disasters.nextTornado = 0;
         ticks(clear, 600);
         eq(0, clear.world.disasters.tornadoCount, "a fair sky makes no tornado");
         Game storm = game(1);
         storm.player.setPos(CX + 0.5, GROUND + 1, CZ + 0.5);
         storm.world.weather.enabled = true;
         storm.world.weather.calm = true;
         storm.world.weather.noLightning = true;
         Weather.Cell cell = storm.world.weather.placeStorm(storm.player.x + 120.0, storm.player.z, 900.0F);
         check(cell != null, "a thunderstorm 120 blocks away");
         storm.world.disasters.nextTornado = 0;
         // the test world is small (only 80 x 80 blocks are loaded): the funnel needs loaded ground to be born on
         for (int cx = -9; cx <= 14; cx++) {
            for (int cz = -9; cz <= 9; cz++) {
               if (!storm.world.chunks.containsKey(World.key(cx, cz))) {
                  Chunk c = new Chunk(cx, cz);
                  for (int x = 0; x < 16; x++) {
                     for (int z = 0; z < 16; z++) {
                        c.set(x, 0, z, Blocks.BEDROCK);
                        c.set(x, GROUND, z, Blocks.GRASS);
                     }
                  }

                  c.computeHeightmap();
                  storm.world.chunks.put(World.key(cx, cz), c);
               }
            }
         }

         int born = 0;
         while (storm.world.disasters.tornadoCount == 0 && born < 1200) {
            ticks(storm, 1);
            born++;
         }

         eq(1, storm.world.disasters.tornadoCount, "a tornado is born in the storm (after " + born + " ticks)");
         Disasters.Tornado nat = storm.world.disasters.tornadoes.isEmpty() ? null : storm.world.disasters.tornadoes.get(0);
         check(nat != null, "it is there");
         if (nat != null) {
            double dist = Math.hypot(nat.x - storm.player.x, nat.z - storm.player.z);
            check(dist > 60.0 && dist < 200.0, "90 - 180 blocks away when born, now " + dist);
            double toX = storm.player.x - nat.x;
            double toZ = storm.player.z - nat.z;
            check((nat.dx * toX + nat.dz * toZ) / Math.max(1.0, dist) > 0.2, "heading for the player's neighbourhood");
         }

         check(storm.chatLog.contains("A tornado is forming in the storm!"), "the player is told");
         check(storm.world.disasters.nextTornado >= storm.world.disasters.clock + 20000L - 41, "the next one only after 15 more minutes");
         storm.world.disasters.stopAll();
         Game weatherOff = game(1);
         weatherOff.player.setPos(CX + 0.5, GROUND + 1, CZ + 0.5);
         weatherOff.world.weather.enabled = true;
         weatherOff.world.weather.suppressed = true;
         weatherOff.world.weather.placeStorm(weatherOff.player.x + 100.0, weatherOff.player.z, 900.0F);
         weatherOff.world.disasters.nextTornado = 0;
         ticks(weatherOff, 600);
         eq(0, weatherOff.world.disasters.tornadoCount, "Weather: OFF means no storms and so no tornadoes");

         // meteors at night
         Game night = game(1);
         night.player.setPos(CX + 0.5, GROUND + 1, CZ + 0.5);
         night.world.dayTime = 6000L;
         night.world.disasters.nextShower = 0;
         ticks(night, 200);
         eq(0, night.world.disasters.showerLeft, "no meteor shower by day");
         night.world.dayTime = 18000L;
         ticks(night, 80);
         check(night.world.disasters.showerLeft >= 6 && night.world.disasters.showerLeft <= 13, "a shower of 6 - 13 meteors at night: " + night.world.disasters.showerLeft);
         check(night.world.disasters.nextShower >= night.world.disasters.clock + 36000L - 100, "the next one at least half an hour later");
         check(night.chatLog.contains("Look at the sky - a meteor shower!"), "the player is told");
      });
   }

   // ------------------------------------------------------------------ commands, saving
   static void commands() {
      test("/disaster: tornado, quake, meteor, stop, on, off, status - and bad arguments are refused", () -> {
         Game g = game(1);
         World w = g.world;
         Disasters ds = w.disasters;
         g.player.setPos(CX + 0.5, GROUND + 1, CZ + 0.5);
         g.player.yaw = 0.0F;
         g.runCommand("/disaster tornado 60");
         eq(1, ds.tornadoes.size(), "a tornado");
         Disasters.Tornado t = ds.tornadoes.get(0);
         check(Math.abs(t.x - (CX + 0.5)) < 1e-6 && Math.abs(t.z - (CZ + 0.5 + 60.0)) < 1e-6, "60 blocks ahead of the player (looking along +z)");
         check((t.dx * (g.player.x - t.x) + t.dz * (g.player.z - t.z)) > 0.0, "heading towards the player");
         g.runCommand("/disaster tornado");
         eq(2, ds.tornadoes.size(), "another one (default 80 blocks)");
         g.runCommand("/disaster quake 3");
         check(ds.quake != null && ds.quake.magnitude == 3.0F, "a magnitude 3 quake");
         g.runCommand("/disaster quake");
         check(ds.quake.magnitude == 2.0F, "default magnitude 2");
         check(Math.hypot(ds.quake.x - g.player.x, ds.quake.z - g.player.z) < 13.0, "close to the player");
         g.runCommand("/disaster meteor 4");
         eq(4, ds.showerLeft, "four meteors");
         g.runCommand("/disaster meteor");
         eq(1, ds.showerLeft, "one by default");
         g.runCommand("/disaster stop");
         eq(0, ds.showerLeft, "stopped: no meteors");
         check(ds.quake == null, "no quake");
         for (Disasters.Tornado x : ds.tornadoes) {
            check(x.life <= x.age + 120, "the tornadoes rope out within 6 seconds");
         }

         g.runCommand("/disaster off");
         check(!ds.enabled, "off");
         g.runCommand("/disaster on");
         check(ds.enabled, "on");
         g.chatLog.clear();
         g.runCommand("/disaster");
         check(g.chatLog.size() >= 2 && g.chatLog.get(0).startsWith("Disasters: on"), "status: " + g.chatLog);
         g.chatLog.clear();
         g.runCommand("/disasters tornado abc");
         check(g.chatLog.size() == 1 && g.chatLog.get(0).contains("Invalid"), "bad number: " + g.chatLog);
         g.chatLog.clear();
         g.runCommand("/help");
         check(g.chatLog.get(0).contains("/disaster") && g.chatLog.get(0).contains("/treefall"), "the help lists the new commands");
         // a tornado called while the weather is on also brings a grey, rainy sky (but no lightning)
         Game gw = game(1);
         gw.world.weather.enabled = true;
         gw.player.setPos(CX + 0.5, GROUND + 1, CZ + 0.5);
         int cells = gw.world.weather.cells.size();
         gw.runCommand("/disaster tornado 70");
         eq(cells, gw.world.weather.cells.size(), "no storm cell (that would bring lightning)");
         eq(2, gw.world.weather.forceMode, "rain for a few minutes");
         Game gt = game(1);
         gt.world.weather.enabled = true;
         gt.world.weather.force(3, 600L);
         gt.runCommand("/disaster tornado 70");
         eq(3, gt.world.weather.forceMode, "a thunderstorm that is already forced is left alone");
      });
   }

   static void saving() {
      test("saved with the world: the switch and the timers (a quake is not saved while it is going on)", () -> {
         Game g = game(1);
         Disasters ds = g.world.disasters;
         ds.enabled = false;
         ds.clock = 12345L;
         ds.nextQuake = 20000L;
         ds.nextTornado = 21000L;
         ds.nextShower = 22000L;
         Properties p = new Properties();
         g.storeFireSetting(p);
         eq("false", p.getProperty("disasters"), "the switch");
         eq("12345", p.getProperty("dclock"), "the clock");
         Game h = game(1);
         h.restoreFireSetting(p);
         Disasters d2 = h.world.disasters;
         check(!d2.enabled && d2.clock == 12345L && d2.nextQuake == 20000L && d2.nextTornado == 21000L && d2.nextShower == 22000L, "all restored");
         // a world from before 3.10: disasters on, a fresh schedule
         Properties old = new Properties();
         Game k = game(1);
         long q0 = k.world.disasters.nextQuake;
         k.restoreFireSetting(old);
         check(k.world.disasters.enabled && k.world.disasters.nextQuake == q0 && k.world.disasters.clock == 0L, "old worlds start with disasters on and the default schedule");
         Properties junk = new Properties();
         junk.setProperty("dclock", "abc");
         k.restoreFireSetting(junk);
         check(k.world.disasters.nextQuake == q0, "garbage is ignored");
         eq(Boolean.TRUE, new Options(new java.io.File(System.getProperty("java.io.tmpdir"))).disasters, "the option is on by default");
      });
   }
}
