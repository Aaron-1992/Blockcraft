package bc;

import static bc.GameTests.GROUND;
import static bc.GameTests.check;
import static bc.GameTests.countItems;
import static bc.GameTests.eq;
import static bc.GameTests.hit;
import static bc.GameTests.newGame;
import static bc.GameTests.test;

import bc.Mobs.Mob;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.function.IntPredicate;

/**
 * Headless tests of falling trees and logs on their side (To do item 6, Update 3.10).   python3 tools/runtests.py unit
 */
final class TestsFall {
   static final int TX = 20;
   static final int TZ = 20;

   static void run() {
      tables();
      geometry();
      detection();
      falling();
      directions();
      cutHigher();
      notATree();
      neighbours();
      realTrees();
      landing();
      harvest();
      placing();
      meshing();
      saving();
   }

   // ------------------------------------------------------------------ helpers
   /** Trunk of h logs at (x, z) on the grass and a small canopy around the top; returns {logs, leaves}. */
   static int[] plant(World w, int x, int z, int h, int log, int leaf) {
      int leaves = 0;
      for (int k = 1; k <= h; k++) {
         w.setBlock(x, GROUND + k, z, log);
      }

      int top = GROUND + h;
      for (int dy = -1; dy <= 1; dy++) {
         int r = dy == 1 ? 1 : 2;
         for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
               boolean skip = dy == 1 ? Math.abs(dx) + Math.abs(dz) > 1 : Math.abs(dx) == 2 && Math.abs(dz) == 2;
               if (skip || dx == 0 && dz == 0 && dy <= 0) {
                  continue;
               }

               if (w.setBlock(x + dx, top + dy, z + dz, leaf)) {
                  leaves++;
               }
            }
         }
      }

      return new int[]{h, leaves};
   }

   /** Empties the playing field (x 8..40, z 8..40) above the grass and forgets falling trees and items. */
   static void clear(Game g) {
      World w = g.world;
      w.trees.falls.clear();
      w.items.clear();
      for (int x = 8; x <= 40; x++) {
         for (int z = 8; z <= 40; z++) {
            for (int y = GROUND + 1; y <= GROUND + 22; y++) {
               w.setBlock(x, y, z, 0);
            }
         }
      }
   }

   static int count(World w, IntPredicate p) {
      int n = 0;
      for (int x = 8; x <= 40; x++) {
         for (int z = 8; z <= 40; z++) {
            for (int y = GROUND + 1; y <= GROUND + 22; y++) {
               if (p.test(w.getBlock(x, y, z))) {
                  n++;
               }
            }
         }
      }

      return n;
   }

   static int settle(World w) {
      int t = 0;
      while (!w.trees.falling().isEmpty() && t < 400) {
         w.tick();
         t++;
      }

      return t;
   }

   /** The player stands at (fromX, fromZ) and breaks the log (x, y, z) - the same path as the mouse. */
   static void cut(Game g, int x, int y, int z, double fromX, double fromZ) {
      g.player.setPos(fromX, GROUND + 1, fromZ);
      g.breakBlock(x, y, z);
   }

   static int logItems(World w) {
      return countItems(w, 9) + countItems(w, 10) + countItems(w, 11);
   }

   // ------------------------------------------------------------------ block data
   static void tables() {
      test("logs on their side: ids, drops, tool class, tiles (rings on the ends, bark turned), fire", () -> {
         eq(220, Blocks.LOG_X0, "x logs start at 220");
         eq(223, Blocks.LOG_Z0, "z logs start at 223");
         TexExtra.blocks();
         for (int type = 0; type < 3; type++) {
            int up = 9 + type;
            int ax = Blocks.sideLog(up, 0);
            int az = Blocks.sideLog(up, 1);
            eq(220 + type, ax, "oak/birch/spruce x log");
            eq(223 + type, az, "oak/birch/spruce z log");
            check(Blocks.isSideLog(ax) && Blocks.isSideLog(az) && !Blocks.isLog(ax) && Blocks.isAnyLog(ax) && Blocks.isAnyLog(az), "side logs are side logs");
            check(Blocks.isLog(up) && Blocks.isAnyLog(up) && !Blocks.isSideLog(up), "upright logs are not");
            eq(up, Blocks.uprightLog(ax), "upright twin of the x log");
            eq(up, Blocks.uprightLog(az), "upright twin of the z log");
            eq(0, Blocks.sideLogAxis(ax), "x log axis");
            eq(1, Blocks.sideLogAxis(az), "z log axis");
            eq(up, Blocks.drop[ax], "an x log drops the upright log");
            eq(up, Blocks.drop[az], "a z log drops the upright log");
            eq(up, Blocks.pickItem(ax), "middle click picks the upright log");
            eq(up, Blocks.pickItem(az), "middle click picks the upright log (z)");
            eq(Blocks.name[up], Blocks.name[ax], "same name");
            eq(Blocks.hardness[up], Blocks.hardness[az], "same hardness");
            eq(Blocks.mineClass[up], Blocks.mineClass[ax], "same tool class (axe)");
            eq(Blocks.needTool[up], Blocks.needTool[az], "same tool requirement");
            check(Blocks.opaque[ax] && Blocks.solid[az] && Blocks.kind[ax] == 0 && Blocks.coll[az] != null, "full opaque cubes");
            int end = Blocks.tiles[up][0];
            int bark = Blocks.tiles[up][2];
            int turned = 246 + type;
            // faces: 0 top, 1 bottom, 2 north (-z), 3 south (+z), 4 east (+x), 5 west (-x)
            for (int f = 0; f < 6; f++) {
               eq(f >= 4 ? end : turned, Blocks.tiles[ax][f], "x log face " + f);
               eq(f == 0 || f == 1 ? bark : (f <= 3 ? end : turned), Blocks.tiles[az][f], "z log face " + f);
            }

            eq(Fire.flammability(up), Fire.flammability(ax), "an x log burns like a log");
            eq(Fire.flammability(up), Fire.flammability(az), "a z log burns like a log");
            eq(Fire.encouragement(up), Fire.encouragement(ax), "and catches fire like one");
            check(Fire.flammability(az) > 0 && Fire.encouragement(az) > 0, "it does burn");
            // the turned bark tile is the bark tile transposed
            int[] src = TexArt310.bark(type);
            boolean transposed = true;
            boolean painted = false;
            for (int y = 0; y < 16; y++) {
               for (int x = 0; x < 16; x++) {
                  int atlas = Tex.atlasPixels[(Tex.ty(turned) + y) * Tex.PX + Tex.tx(turned) + x];
                  painted |= atlas >>> 24 != 0;
                  transposed &= atlas == src[x * 16 + y];
               }
            }

            check(painted && transposed, "tile " + turned + " is the bark of the " + Blocks.name[up] + " turned by 90 degrees");
         }

         HashSet<Integer> inCreative = new HashSet<>();
         for (int id : Blocks.creativeList()) {
            inCreative.add(id);
         }

         for (int id = 220; id < 226; id++) {
            check(!inCreative.contains(id), "side log " + id + " is not a creative inventory item (it comes from felled trees)");
         }
      });
   }

   // ------------------------------------------------------------------ the rotation
   static void geometry() {
      test("toppling: the 90 degree turn is exact, keeps distances, turns 'up' into the fall direction, keeps the sides", () -> {
         int[] r = new int[3];
         for (int[] d : Trees.DIRS) {
            Trees.rotated(0, 5, 0, d[0], d[1], r);
            check(r[0] == 5 * d[0] && r[1] == 0 && r[2] == 5 * d[1], "the trunk top ends 5 blocks away in the fall direction, at ground level");
            Trees.rotated(d[0], 3, d[1], d[0], d[1], r);
            check(r[1] == -1, "something on the side the tree falls to goes down");
            Trees.rotated(-d[0], 3, -d[1], d[0], d[1], r);
            check(r[1] == 1, "something on the far side goes up");
            Trees.rotated(-d[1], 3, d[0], d[0], d[1], r);
            check(r[0] == 3 * d[0] - d[1] && r[1] == 0 && r[2] == 3 * d[1] + d[0], "a sideways offset stays sideways");
         }

         Random rnd = new Random(5);
         HashSet<String> seen = new HashSet<>();
         for (int i = 0; i < 300; i++) {
            int[] d = Trees.DIRS[rnd.nextInt(4)];
            int ox = rnd.nextInt(11) - 5;
            int oy = rnd.nextInt(11) - 5;
            int oz = rnd.nextInt(11) - 5;
            Trees.rotated(ox, oy, oz, d[0], d[1], r);
            eq(ox * ox + oy * oy + oz * oz, r[0] * r[0] + r[1] * r[1] + r[2] * r[2], "a rotation keeps the distance from the pivot");
         }

         for (int[] d : Trees.DIRS) {
            seen.clear();
            for (int ox = -3; ox <= 3; ox++) {
               for (int oy = -3; oy <= 3; oy++) {
                  for (int oz = -3; oz <= 3; oz++) {
                     Trees.rotated(ox, oy, oz, d[0], d[1], r);
                     seen.add(r[0] + "," + r[1] + "," + r[2]);
                  }
               }
            }

            eq(343, seen.size(), "different blocks never land in the same cell (direction " + d[0] + "," + d[1] + ")");
         }

         int[][] order = Trees.directionOrder(6, 0, null);
         check(order[0][0] == 1 && order[0][1] == 0 && order[3][0] == -1, "away from the player first, towards him last");
         order = Trees.directionOrder(-1, 5, null);
         check(order[0][1] == 1 && order[3][1] == -1, "the strongest component decides");
         order = Trees.directionOrder(2, -3, null);
         check(order[0][1] == -1 && order[1][0] == 1 && order[2][0] == -1 && order[3][1] == 1, "second best is the other axis away from the player");
      });
   }

   // ------------------------------------------------------------------ what counts as a tree
   static void detection() {
      test("what hangs on a cut log: trunk above it, its leaves, nothing that stands on something else", () -> {
         Game g = newGame(1);
         World w = g.world;
         clear(g);
         int[] t = plant(w, TX, TZ, 6, 9, 12);
         w.setBlock(TX, GROUND + 1, TZ, 0); // the base log is cut out
         Trees.Plan p = w.trees.collect(TX, GROUND + 1, TZ);
         check(p != null, "a tree with leaves is a tree");
         if (p != null) {
            eq(5, p.nTrunk, "five trunk logs above the cut");
            eq(5, p.nLogs, "and no branches");
            eq(t[1], p.n - p.nLogs, "every leaf hangs on it");
            check(p.id[0] == 9 && p.id[p.nLogs] == 12, "ids are recorded (logs first, then leaves)");
         }

         // cut in the middle of the trunk: only what is above it
         w.setBlock(TX, GROUND + 1, TZ, 9);
         w.setBlock(TX, GROUND + 3, TZ, 0);
         p = w.trees.collect(TX, GROUND + 3, TZ);
         check(p != null && p.nTrunk == 3 && p.py == GROUND + 3, "cut in the middle: the three logs above");

         // nothing above the cut (the top log): no tree
         w.setBlock(TX, GROUND + 3, TZ, 9);
         w.setBlock(TX, GROUND + 6, TZ, 0);
         check(w.trees.collect(TX, GROUND + 6, TZ) == null, "nothing above the top log");
         w.setBlock(TX, GROUND + 6, TZ, 9);

         // a pillar without leaves is a pillar
         clear(g);
         for (int k = 1; k <= 6; k++) {
            w.setBlock(TX, GROUND + k, TZ, 9);
         }

         w.setBlock(TX, GROUND + 1, TZ, 0);
         check(w.trees.collect(TX, GROUND + 1, TZ) == null, "a log pillar is not a tree");
         // ... and three leaves next to it do not make it one, four do
         int[][] at = new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
         for (int i = 0; i < 4; i++) {
            w.setBlock(TX + at[i][0], GROUND + 6, TZ + at[i][1], 13);
            Trees.Plan q = w.trees.collect(TX, GROUND + 1, TZ);
            check(i < 3 ? q == null : q != null, "pillar with " + (i + 1) + " leaves: " + (i < 3 ? "no tree" : "a tree"));
         }

         // a log that stands on something else holds the whole thing up (a second trunk, a stone under a branch)
         clear(g);
         plant(w, TX, TZ, 6, 9, 12);
         w.setBlock(TX + 1, GROUND + 1, TZ, Blocks.STONE);
         w.setBlock(TX + 1, GROUND + 2, TZ, 9);
         w.setBlock(TX, GROUND + 1, TZ, 0);
         check(w.trees.collect(TX, GROUND + 1, TZ) == null, "held up by a log standing on a stone: nothing falls");

         // only the logs of the cut column count as a trunk: cutting a branch leaves it where it is
         clear(g);
         plant(w, TX, TZ, 6, 9, 12);
         w.setBlock(TX + 1, GROUND + 4, TZ, 9);
         w.setBlock(TX + 2, GROUND + 4, TZ, 9);
         w.setBlock(TX + 1, GROUND + 4, TZ, 0);
         check(w.trees.collect(TX + 1, GROUND + 4, TZ) == null, "a branch cut off the trunk does not make anything fall");
         // a branch, cutting the trunk: it hangs on, and counts as a branch (not rigid)
         w.setBlock(TX + 1, GROUND + 4, TZ, 9);
         w.setBlock(TX, GROUND + 1, TZ, 0);
         p = w.trees.collect(TX, GROUND + 1, TZ);
         check(p != null && p.nTrunk == 5 && p.nLogs == 7, "the two branch logs come along: 5 trunk + 2 branch logs");
      });
   }

   // ------------------------------------------------------------------ the fall
   static void falling() {
      test("cutting the base of a tree: it falls away from the player and lies there as logs on their side", () -> {
         Game g = newGame(0);
         World w = g.world;
         int[][] dirs = new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
         for (int[] d : dirs) {
            clear(g);
            int[] t = plant(w, TX, TZ, 6, 9, 12);
            double fx = TX + 0.5 - d[0] * 5;
            double fz = TZ + 0.5 - d[1] * 5;
            cut(g, TX, GROUND + 1, TZ, fx, fz);
            String name = "towards " + d[0] + "," + d[1];
            eq(1, w.trees.falling().size(), "a tree is falling " + name);
            eq(0, w.getBlock(TX, GROUND + 2, TZ), "the trunk is out of the world while it is in the air " + name);
            eq(0, w.getBlock(TX + 1, GROUND + 6, TZ), "and so are the leaves " + name);
            Trees.Fall f = w.trees.falling().get(0);
            eq(d[0], f.dx(), "fall direction x " + name);
            eq(d[1], f.dz(), "fall direction z " + name);
            check(f.angle(0.0F) == 0.0F, "it starts upright");
            float prev = -1.0F;
            boolean steady = true;
            for (int i = 0; i < f.rotTicks; i++) {
               float a = f.angle(0.0F);
               steady &= a >= prev && a <= 90.0F;
               prev = a;
               w.tick();
            }

            check(steady && prev > 0.0F, "the angle only grows " + name);
            eq(0, w.trees.falling().size(), "it has landed after rotTicks ticks " + name);
            int side = Blocks.sideLog(9, d[0] != 0 ? 0 : 1);
            for (int k = 1; k <= 5; k++) {
               eq(side, w.getBlock(TX + d[0] * k, GROUND + 1, TZ + d[1] * k), "log " + k + " of the trunk lies along the fall direction " + name);
            }

            eq(0, w.getBlock(TX, GROUND + 1, TZ), "the cut cell stays empty " + name);
            eq(0, count(w, id -> Blocks.isLog(id)), "no log is left standing " + name);
            eq(5, count(w, id -> Blocks.isSideLog(id)), "five logs lie on the ground " + name);
            int leaves = count(w, id -> Blocks.isLeaves(id));
            check(leaves > t[1] / 4 && leaves <= t[1], "the canopy came down with it (" + leaves + " of " + t[1] + " leaves left) " + name);
            int highest = 0;
            for (int y = GROUND + 1; y <= GROUND + 22; y++) {
               for (int x = 8; x <= 40; x++) {
                  for (int z = 8; z <= 40; z++) {
                     if (Blocks.isLeaves(w.getBlock(x, y, z))) {
                        highest = Math.max(highest, y - GROUND);
                     }
                  }
               }
            }

            check(highest <= 4, "no leaf is left hanging in the air up in the old crown (highest " + highest + " above the grass) " + name);
            eq(1, countItems(w, 9), "the log the player cut dropped as an item " + name);
         }

         check(w.trees.felled == 4, "four trees fell");
      });
   }

   static void directions() {
      test("blocked ways: the next best direction is taken, with no room at all the tree stays", () -> {
         Game g = newGame(0);
         World w = g.world;
         clear(g);
         plant(w, TX, TZ, 6, 9, 12);
         // the player is west: east is best, then +z (tie), then -z, then west (towards him)
         w.setBlock(TX + 1, GROUND + 1, TZ, Blocks.STONE);
         cut(g, TX, GROUND + 1, TZ, TX - 4.5, TZ + 0.5);
         eq(1, w.trees.falling().size(), "it falls");
         eq(0, w.trees.falling().get(0).dx(), "not east (stone at the foot)");
         eq(1, w.trees.falling().get(0).dz(), "but to the south (+z)");
         settle(w);
         eq(Blocks.sideLog(9, 1), w.getBlock(TX, GROUND + 1, TZ + 3), "lies along z");

         clear(g);
         plant(w, TX, TZ, 6, 9, 12);
         // a wall three blocks away to the east, high enough for the whole trunk: east is blocked by the crossing trunk
         for (int y = 1; y <= 3; y++) {
            w.setBlock(TX + 3, GROUND + y, TZ, Blocks.STONE);
         }

         cut(g, TX, GROUND + 1, TZ, TX - 4.5, TZ + 0.5);
         eq(1, w.trees.falling().size(), "it falls");
         check(w.trees.falling().get(0).dx() == 0, "not into the wall");
         settle(w);

         clear(g);
         plant(w, TX, TZ, 6, 9, 12);
         w.setBlock(TX + 1, GROUND + 1, TZ, Blocks.STONE);
         w.setBlock(TX - 1, GROUND + 1, TZ, Blocks.STONE);
         w.setBlock(TX, GROUND + 1, TZ + 1, Blocks.STONE);
         w.setBlock(TX, GROUND + 1, TZ - 1, Blocks.STONE);
         cut(g, TX, GROUND + 1, TZ, TX - 4.5, TZ + 0.5);
         eq(0, w.trees.falling().size(), "walled in on all four sides: nothing falls");
         eq(5, count(w, id -> Blocks.isLog(id)), "the trunk is untouched (old behaviour: the logs stay where they are)");
         check(count(w, id -> Blocks.isLeaves(id)) > 0, "and so are the leaves");

         // falling into water is not allowed
         clear(g);
         plant(w, TX, TZ, 6, 9, 12);
         for (int k = 1; k <= 6; k++) {
            w.setBlock(TX + k, GROUND + 1, TZ, 50);
         }

         cut(g, TX, GROUND + 1, TZ, TX - 4.5, TZ + 0.5);
         check(w.trees.falling().size() == 1 && w.trees.falling().get(0).dx() != 1, "not into a lake");
         settle(w);
         for (int k = 1; k <= 6; k++) {
            w.setBlock(TX + k, GROUND + 1, TZ, 0);
         }

         // never onto the player who is chopping: the only free way leads over him, so the tree stays
         clear(g);
         plant(w, TX, TZ, 6, 9, 12);
         w.setBlock(TX - 1, GROUND + 1, TZ, Blocks.STONE);
         w.setBlock(TX, GROUND + 1, TZ + 1, Blocks.STONE);
         w.setBlock(TX, GROUND + 1, TZ - 1, Blocks.STONE);
         cut(g, TX, GROUND + 1, TZ, TX + 3.5, TZ + 0.5);
         eq(0, w.trees.falling().size(), "the one free way (east) is over the player standing 3 blocks away: nothing falls");
         eq(5, count(w, id -> Blocks.isLog(id)), "(the logs are where they were)");
         // ... and it falls there when he stands somewhere else
         clear(g);
         plant(w, TX, TZ, 6, 9, 12);
         w.setBlock(TX - 1, GROUND + 1, TZ, Blocks.STONE);
         w.setBlock(TX, GROUND + 1, TZ + 1, Blocks.STONE);
         w.setBlock(TX, GROUND + 1, TZ - 1, Blocks.STONE);
         cut(g, TX, GROUND + 1, TZ, TX + 0.5, TZ + 8.5);
         eq(1, w.trees.falling().size(), "with the player out of the way it falls east");
         eq(1, w.trees.falling().get(0).dx(), "east");
         settle(w);

         // the setting
         clear(g);
         plant(w, TX, TZ, 6, 9, 12);
         w.trees.enabled = false;
         cut(g, TX, GROUND + 1, TZ, TX - 4.5, TZ + 0.5);
         eq(0, w.trees.falling().size(), "/treefall off: nothing falls");
         w.trees.enabled = true;
      });
   }

   static void cutHigher() {
      test("cutting higher up: the top falls, the stump stays, the trunk drops until it rests on the ground", () -> {
         Game g = newGame(0);
         World w = g.world;
         clear(g);
         int[] t = plant(w, TX, TZ, 6, 10, 13);
         cut(g, TX, GROUND + 3, TZ, TX - 5.5, TZ + 0.5);
         eq(1, w.trees.falling().size(), "falls");
         Trees.Fall f = w.trees.falling().get(0);
         eq(2, f.drop, "the lying trunk is two blocks above the ground and drops by two");
         eq(3, f.plan.nTrunk, "three logs above the cut");
         eq(10, w.getBlock(TX, GROUND + 1, TZ), "the stump (birch) stays: log 1");
         eq(10, w.getBlock(TX, GROUND + 2, TZ), "the stump: log 2");
         eq(0, w.getBlock(TX, GROUND + 3, TZ), "the cut");
         int ticks = settle(w);
         check(ticks > f.rotTicks, "it needed more time than the turning for the drop (" + ticks + " ticks)");
         for (int k = 1; k <= 3; k++) {
            eq(Blocks.sideLog(10, 0), w.getBlock(TX + k, GROUND + 1, TZ), "birch log " + k + " lies on the grass, next to the stump");
         }

         eq(2, count(w, id -> Blocks.isLog(id)), "the two logs of the stump");
         check(count(w, id -> Blocks.isLeaves(id)) > 0, "leaves landed");
         // the drop is smooth: the shift only grows and ends exactly at the drop
         clear(g);
         plant(w, TX, TZ, 6, 9, 12);
         cut(g, TX, GROUND + 3, TZ, TX - 5.5, TZ + 0.5);
         f = w.trees.falling().get(0);
         double prev = 0.0;
         boolean steady = true;
         for (int i = 0; i < 100 && !w.trees.falling().isEmpty(); i++) {
            steady &= f.drop(0.0F) >= prev - 1e-9 && f.drop(1.0F) >= f.drop(0.0F) - 1e-9 && f.drop(1.0F) <= 2.0 + 1e-9;
            prev = f.drop(0.0F);
            w.tick();
         }

         check(steady && f.shift == 2.0, "the drop is monotonic and ends at exactly 2 blocks");
      });
   }

   static void notATree() {
      test("a log pillar, a tree with the setting off and the top log do not fall", () -> {
         Game g = newGame(0);
         World w = g.world;
         clear(g);
         for (int k = 1; k <= 6; k++) {
            w.setBlock(TX, GROUND + k, TZ, 9);
         }

         cut(g, TX, GROUND + 1, TZ, TX - 5.5, TZ + 0.5);
         eq(0, w.trees.falling().size(), "a pillar without leaves stays");
         eq(5, count(w, id -> Blocks.isLog(id)), "the logs are where they were");
         clear(g);
         plant(w, TX, TZ, 6, 9, 12);
         cut(g, TX, GROUND + 6, TZ, TX - 5.5, TZ + 0.5);
         eq(0, w.trees.falling().size(), "cutting the top log: nothing above it, the crown stays");
         eq(5, count(w, id -> Blocks.isLog(id)), "(five logs left)");
      });
   }

   static void neighbours() {
      test("two trees with crowns that touch: only the leaves of the cut tree come down, the other one is untouched", () -> {
         Game g = newGame(0);
         World w = g.world;
         clear(g);
         plant(w, TX, TZ, 6, 9, 12);
         plant(w, TX + 4, TZ, 6, 9, 12);
         int rightLeaves = count(w, id -> Blocks.isLeaves(id));
         // the leaves nearer the right trunk (x >= TX + 3) belong to it
         int[] own = new int[1];
         for (int x = TX + 3; x <= TX + 6; x++) {
            for (int z = TZ - 2; z <= TZ + 2; z++) {
               for (int y = GROUND + 4; y <= GROUND + 8; y++) {
                  if (Blocks.isLeaves(w.getBlock(x, y, z))) {
                     own[0]++;
                  }
               }
            }
         }

         cut(g, TX, GROUND + 1, TZ, TX - 5.5, TZ + 0.5);
         eq(1, w.trees.falling().size(), "the left tree falls");
         check(w.trees.falling().get(0).dx() != 1, "not into the other tree's trunk");
         int still = 0;
         for (int x = TX + 3; x <= TX + 6; x++) {
            for (int z = TZ - 2; z <= TZ + 2; z++) {
               for (int y = GROUND + 4; y <= GROUND + 8; y++) {
                  if (Blocks.isLeaves(w.getBlock(x, y, z))) {
                     still++;
                  }
               }
            }
         }

         eq(own[0], still, "every leaf near the right trunk is still there while the left tree is in the air");
         eq(6, count(w, id -> id == 9), "the right trunk stands: its 6 logs (the left tree's logs are out of the world)");
         settle(w);
         check(count(w, id -> id == 9) >= 6, "the right trunk is still upright after the landing");
         check(rightLeaves > 0, "(sanity)");
      });
   }

   static void realTrees() {
      test("generated trees (oak with branches, birch, spruce, big oaks): logs and leaves are accounted for, none is left standing or hanging", () -> {
         Game g = newGame(0);
         World w = g.world;
         Chunk c = w.chunk(1, 1);
         int fell = 0;
         int rooted = 0;
         for (int type = 0; type < 3; type++) {
            for (int size = 0; size < 3; size++) {
               for (int seed = 1; seed <= 4; seed++) {
                  clear(g);
                  w.gen.buildTree(c, 24, GROUND + 1, 24, type, size, new Random(seed * 101L + type * 7 + size));
                  int logs0 = count(w, id -> Blocks.isLog(id));
                  int leaves0 = count(w, id -> Blocks.isLeaves(id));
                  String name = "type " + type + " size " + size + " seed " + seed + " (" + logs0 + " logs, " + leaves0 + " leaves)";
                  check(logs0 > 3 && leaves0 > 10, "the generator made a tree: " + name);
                  boolean bigBase = Blocks.isLog(w.getBlock(25, GROUND + 1, 24)) || Blocks.isLog(w.getBlock(24, GROUND + 1, 25));
                  cut(g, 24, GROUND + 1, 24, 18.5, 24.5);
                  if (bigBase) {
                     rooted++;
                     eq(0, w.trees.falling().size(), "a tree with a thick base stands on its other trunk logs: " + name);
                     continue;
                  }

                  fell++;
                  eq(1, w.trees.falling().size(), "falls: " + name);
                  settle(w);
                  int sideLogs = count(w, id -> Blocks.isSideLog(id));
                  eq(0, count(w, id -> Blocks.isLog(id)), "no log left standing: " + name);
                  eq(logs0, sideLogs + logItems(w), "every log lies on the ground or is an item (the cut one): " + name);
                  int leaves1 = count(w, id -> Blocks.isLeaves(id));
                  check(leaves1 <= leaves0 && leaves1 >= leaves0 / 8, "leaves: " + leaves1 + " of " + leaves0 + " are on the ground: " + name);
                  int highest = 0;
                  for (int y = GROUND + 1; y <= GROUND + 22; y++) {
                     for (int x = 8; x <= 40; x++) {
                        for (int z = 8; z <= 40; z++) {
                           if (Blocks.isAnyLog(w.getBlock(x, y, z)) || Blocks.isLeaves(w.getBlock(x, y, z))) {
                              highest = Math.max(highest, y - GROUND);
                           }
                        }
                     }
                  }

                  check(highest <= 7, "everything is within 7 blocks of the ground (" + highest + "): " + name);
                  // the trunk lies in a straight line away from the player (west of the tree -> east)
                  int along = 0;
                  while (along < 14 && Blocks.isSideLog(w.getBlock(25 + along, GROUND + 1, 24))) {
                     along++;
                  }

                  check(along >= 3, "the trunk lies east of the stump, " + along + " logs in a row: " + name);
               }
            }
         }

         check(fell >= 20, "most generated trees were single-trunk trees (" + fell + " fell, " + rooted + " big ones stand on their base)");
      });
   }

   static void landing() {
      test("a trunk that lands on a mob or on the player hurts and lifts it", () -> {
         Game g = newGame(0);
         World w = g.world;
         clear(g);
         plant(w, TX, TZ, 6, 9, 12);
         Mob cow = g.mobs.spawn(Mobs.COW, TX + 2.5, GROUND + 1, TZ + 0.5);
         int hp = cow.health;
         g.player.setPos(TX + 4.5, GROUND + 1, TZ + 0.5);
         int hpPlayer = g.player.health;
         g.player.invuln = 0;
         w.setBlock(TX, GROUND + 1, TZ, 0);
         Trees.Plan p = w.trees.collect(TX, GROUND + 1, TZ);
         check(p != null && w.trees.start(p, 1.0, 0.0, null), "the tree is made to fall east, onto both of them");
         settle(w);
         check(cow.health < hp || cow.dead, "the cow was hit (" + hp + " -> " + cow.health + ")");
         eq(hpPlayer - Trees.HIT, g.player.health, "the player lost " + Trees.HIT + " half hearts");
         check(g.player.y >= GROUND + 2.0, "and was put on top of the log (y " + g.player.y + ")");
         check(cow.dead || cow.y >= GROUND + 2.0, "so was the cow");
         eq(Blocks.sideLog(9, 0), w.getBlock(TX + 4, GROUND + 1, TZ), "the log lies where the player stood");

         // creative: no damage, but nobody is left inside a log
         Game gc = newGame(1);
         World wc = gc.world;
         clear(gc);
         plant(wc, TX, TZ, 6, 9, 12);
         gc.player.setPos(TX + 4.5, GROUND + 1, TZ + 0.5);
         wc.setBlock(TX, GROUND + 1, TZ, 0);
         p = wc.trees.collect(TX, GROUND + 1, TZ);
         wc.trees.start(p, 1.0, 0.0, null);
         settle(wc);
         eq(20, gc.player.health, "creative players are not hurt");
         check(gc.player.y >= GROUND + 2.0, "but they are not inside the log either");
      });
   }

   static void harvest() {
      test("a fallen trunk is harvested like any log: one upright log item per piece, nothing in creative, an axe is faster", () -> {
         Game g = newGame(0);
         World w = g.world;
         clear(g);
         plant(w, TX, TZ, 6, 11, 14);
         cut(g, TX, GROUND + 1, TZ, TX - 5.5, TZ + 0.5);
         settle(w);
         w.items.clear();
         int side = Blocks.sideLog(11, 0);
         eq(side, w.getBlock(TX + 2, GROUND + 1, TZ), "a spruce x log lies there");
         g.breakBlock(TX + 2, GROUND + 1, TZ);
         eq(0, w.getBlock(TX + 2, GROUND + 1, TZ), "broken");
         eq(1, countItems(w, 11), "it drops one upright spruce log");
         check(Items.canHarvest(0, side), "bare hands can harvest it (as with logs)");
         eq(Blocks.hardness[11], Blocks.hardness[side], "same breaking time as an upright log");
         eq(Items.canHarvest(Items.toolId(2, 3), 11), Items.canHarvest(Items.toolId(2, 3), side), "an axe is as good as on a log");
         // breaking a lying log must not make anything fall
         int before = w.trees.felled;
         g.breakBlock(TX + 3, GROUND + 1, TZ);
         eq(before, w.trees.felled, "nothing falls when a fallen trunk is cut");
         // creative
         Game gc = newGame(1);
         World wc = gc.world;
         wc.setBlock(TX, GROUND + 1, TZ, Blocks.LOG_Z0);
         gc.breakBlock(TX, GROUND + 1, TZ);
         eq(0, wc.getBlock(TX, GROUND + 1, TZ), "creative: gone");
         eq(0, countItems(wc, 9), "and no item");
         // burns like a log
         Game gf = newGame(0);
         gf.world.setBlock(TX, GROUND + 1, TZ, Blocks.LOG_X0);
         check(Fire.flammability(gf.world.getBlock(TX, GROUND + 1, TZ)) == 5, "flammable like a log");
      });
   }

   static void placing() {
      test("placing logs: against the side of a block they lie along that axis, on top or on the ground they stand", () -> {
         Game g = newGame(1);
         World w = g.world;
         clear(g);
         w.setBlock(TX, GROUND + 1, TZ, Blocks.STONE);
         for (int type = 0; type < 3; type++) {
            int up = 9 + type;
            int[][] faces = new int[][]{{1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}};
            for (int[] f : faces) {
               w.setBlock(TX + f[0], GROUND + 1, TZ + f[2], 0);
               g.inv[g.sel] = new Stack(up, 5);
               g.target = hit(TX, GROUND + 1, TZ, f[0], f[1], f[2]);
               check(g.placeBlock(), "placed against the " + f[0] + "," + f[2] + " face");
               eq(Blocks.sideLog(up, f[0] != 0 ? 0 : 1), w.getBlock(TX + f[0], GROUND + 1, TZ + f[2]), "the log lies along the axis you clicked along (" + f[0] + "," + f[2] + ")");
               w.setBlock(TX + f[0], GROUND + 1, TZ + f[2], 0);
            }

            g.target = hit(TX, GROUND + 1, TZ, 0, 1, 0);
            check(g.placeBlock(), "placed on top");
            eq(up, w.getBlock(TX, GROUND + 2, TZ), "on the top face a log stands upright");
            w.setBlock(TX, GROUND + 2, TZ, 0);
         }

         // placing into tall grass (replaceable target) is upright too
         w.setBlock(TX + 3, GROUND + 1, TZ, 29);
         g.inv[g.sel] = new Stack(9, 5);
         g.target = hit(TX + 3, GROUND + 1, TZ, 1, 0, 0);
         check(g.placeBlock(), "placed where the grass is");
         eq(9, w.getBlock(TX + 3, GROUND + 1, TZ), "a log put into tall grass stands upright");
         w.setBlock(TX + 3, GROUND + 1, TZ, 0);
         // survival uses one up
         Game gs = newGame(0);
         gs.world.setBlock(TX, GROUND + 1, TZ, Blocks.STONE);
         gs.inv[gs.sel] = new Stack(9, 2);
         gs.target = hit(TX, GROUND + 1, TZ, 1, 0, 0);
         check(gs.placeBlock(), "survival placement");
         eq(1, gs.inv[gs.sel].n, "one log used");
         eq(220, gs.world.getBlock(TX + 1, GROUND + 1, TZ), "a lying oak log");
      });
   }

   /** The vertices (x, y, z, u, v) of the quads the mesher emits for the block at (x, y, z), local to the block. */
   static List<float[]> quads(World w, Chunk c, int x, int y, int z, int id) {
      w.setBlock(x, y, z, id);
      Mesher.Result r = Mesher.build(w, c, y >> 4, false);
      ArrayList<float[]> out = new ArrayList<>();
      int ly = y & 15;
      for (int i = 0; i + 3 < r.nO; i += 4) {
         float[] q = new float[20];
         boolean inside = true;
         for (int k = 0; k < 4; k++) {
            float[] v = new float[5];
            for (int j = 0; j < 5; j++) {
               v[j] = r.opaque.get((i + k) * 11 + j);
            }

            v[0] -= x;
            v[1] -= ly;
            v[2] -= z;
            inside &= v[0] >= -1e-4F && v[0] <= 1.0001F && v[1] >= -1e-4F && v[1] <= 1.0001F && v[2] >= -1e-4F && v[2] <= 1.0001F;
            System.arraycopy(v, 0, q, k * 5, 5);
         }

         if (inside) {
            out.add(q);
         }
      }

      if (r.opaque != null) {
         org.lwjgl.system.MemoryUtil.memFree(r.opaque);
      }

      if (r.trans != null) {
         org.lwjgl.system.MemoryUtil.memFree(r.trans);
      }

      w.setBlock(x, y, z, 0);
      return out;
   }

   static void meshing() {
      test("meshing: each face of a log on its side shows the right tile (rings on the two ends, turned bark elsewhere)", () -> {
         Game g = newGame(1);
         World w = g.world;
         Chunk c = w.chunk(0, 0);
         int y = GROUND + 8;
         for (int type = 0; type < 3; type++) {
            for (int axis = 0; axis < 2; axis++) {
               int id = Blocks.sideLog(9 + type, axis);
               List<float[]> qs = quads(w, c, 5, y, 5, id);
               eq(6, qs.size(), "six faces: " + Blocks.name[id] + " axis " + axis);
               int ends = 0;
               for (float[] q : qs) {
                  // the face: the coordinate that is the same in all four vertices at 0 or 1
                  int face = -1;
                  for (int a = 0; a < 3; a++) {
                     float v0 = q[a];
                     boolean same = true;
                     for (int k = 1; k < 4; k++) {
                        same &= Math.abs(q[k * 5 + a] - v0) < 1e-4F;
                     }

                     if (same && (Math.abs(v0) < 1e-4F || Math.abs(v0 - 1.0F) < 1e-4F)) {
                        face = a == 1 ? (v0 > 0.5F ? 0 : 1) : (a == 2 ? (v0 > 0.5F ? 3 : 2) : (v0 > 0.5F ? 4 : 5));
                     }
                  }

                  float cu = (q[3] + q[8] + q[13] + q[18]) / 4.0F;
                  float cv = (q[4] + q[9] + q[14] + q[19]) / 4.0F;
                  int tile = (int)Math.floor(cv * Tex.COLS) * Tex.COLS + (int)Math.floor(cu * Tex.COLS);
                  check(face >= 0, "found the face of a quad");
                  if (face >= 0) {
                     eq(Blocks.tiles[id][face], tile, Blocks.name[id] + " axis " + axis + " face " + face + " uses its tile");
                     ends += tile == Blocks.tiles[9 + type][0] ? 1 : 0;
                  }
               }

               eq(2, ends, "exactly two faces show the rings: " + Blocks.name[id] + " axis " + axis);
            }
         }
      });
   }

   static void saving() {
      test("saving the world puts a tree that is still in the air on the ground first", () -> {
         Game g = newGame(0);
         World w = g.world;
         clear(g);
         plant(w, TX, TZ, 6, 9, 12);
         cut(g, TX, GROUND + 1, TZ, TX - 5.5, TZ + 0.5);
         eq(1, w.trees.falling().size(), "in the air");
         w.tick();
         w.saveAll();
         eq(0, w.trees.falling().size(), "saveAll lands it");
         eq(5, count(w, id -> Blocks.isSideLog(id)), "all five logs are in the world");
         eq(0, count(w, id -> Blocks.isLog(id)), "none is lost");
         // a second saveAll or tick changes nothing
         w.saveAll();
         tick(w, 5);
         eq(5, count(w, id -> Blocks.isSideLog(id)), "(still five)");

         // the setting is saved with the world
         java.util.Properties p = new java.util.Properties();
         g.storeFireSetting(p);
         eq("true", p.getProperty("treefall"), "saved: on");
         w.trees.enabled = false;
         g.storeFireSetting(p);
         eq("false", p.getProperty("treefall"), "saved: off");
         w.trees.enabled = true;
         g.restoreFireSetting(p);
         check(!w.trees.enabled, "and restored");
         p.remove("treefall");
         g.restoreFireSetting(p);
         check(w.trees.enabled, "worlds from before 3.10 have falling trees on");
      });
   }

   static void tick(World w, int n) {
      for (int i = 0; i < n; i++) {
         w.tick();
      }
   }
}
