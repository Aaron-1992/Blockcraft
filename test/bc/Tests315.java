package bc;

import static bc.GameTests.GROUND;
import static bc.GameTests.check;
import static bc.GameTests.eq;
import static bc.GameTests.hit;
import static bc.GameTests.hold;
import static bc.GameTests.newGame;
import static bc.GameTests.test;
import static bc.GameTests.tick;

/**
 * Headless tests of Update 3.15 (block shapes, redstone, ...).   python3 tools/runtests.py unit
 */
final class Tests315 {
   static void run() {
      shapes();
   }

   private static void setAll(World w, int x0, int y0, int z0, int x1, int y1, int z1, int id) {
      for (int x = x0; x <= x1; x++) {
         for (int y = y0; y <= y1; y++) {
            for (int z = z0; z <= z1; z++) {
               w.setBlock(x, y, z, id);
            }
         }
      }
   }

   private static int vertices(World w, int cx, int cz) {
      int n = 0;
      Chunk c = w.chunk(cx, cz);
      for (int s = 0; s < 16; s++) {
         Mesher.Result r = Mesher.build(w, c, s, false);
         n += r.nO + r.nT;
      }

      return n;
   }

   static void shapes() {
      test("shapes: every block has a name, a tile with a picture, and gates / trapdoors drop their closed bottom form", () -> {
         Tex.initPixels();
         for (int id = Shapes.PANE0; id <= Shapes.LAST; id++) {
            boolean defined = Shapes.isThin(id) || Shapes.isWall(id) || Shapes.isWoodFence(id) || Shapes.isGate(id) || Shapes.isTrap(id) || Shapes.isCarpet(id);
            if (!defined) {
               check(Blocks.kind[id] == 0 && !Blocks.solid[id] || Blocks.name[id] == null || Blocks.name[id].startsWith("Unknown") || !Blocks.solid[id], "id " + id + " is unused");
               continue;
            }

            check(Blocks.name[id] != null && Blocks.name[id].length() > 3, "id " + id + " has a name");
            for (int f = 0; f < 6; f++) {
               int tile = Blocks.tiles[id][f];
               int tx = Tex.tx(tile);
               int ty = Tex.ty(tile);
               boolean any = false;
               for (int i = 0; i < 256 && !any; i++) {
                  any = (Tex.atlasPixels[(ty + i / 16) * Tex.PX + tx + i % 16] >>> 24) != 0;
               }

               check(any, Blocks.name[id] + " tile " + tile + " is painted");
            }
         }

         check(Shapes.isGate(Shapes.gate(2, true, 3)) && Shapes.gateOpen(Shapes.gate(2, true, 3)) && Shapes.gateFacing(Shapes.gate(2, true, 3)) == 3, "gate state");
         eq(Shapes.gate(4, false, 0), Blocks.drop[Shapes.gate(4, true, 2)], "an open gate drops the closed one");
         eq(Shapes.trap(3, false, false, 0), Blocks.drop[Shapes.trap(3, true, true, 1)], "a trapdoor drops the closed bottom one");
         eq("Dark Oak Fence Gate", Blocks.name[Shapes.gate(5, false, 0)], "gate name");
         eq("Iron Trapdoor", Blocks.name[Shapes.trap(6, false, false, 0)], "iron trapdoor name");
         eq("Light Blue Stained Glass Pane", Blocks.name[Shapes.PANE0 + 4], "pane name");
         for (int id : Blocks.creativeList()) {
            check(Blocks.isBlockId(id) || Items.isItem(id), "creative id " + id + " is a block or an item id");
         }
      });

      test("shapes: panes and walls grow arms towards panes, walls and solid blocks", () -> {
         Game g = newGame(0);
         World w = g.world;
         int y = GROUND + 1;
         w.setBlock(20, y, 20, Shapes.PANE0);
         Shapes.Nbr alone = (dx, dy, dz) -> w.getBlock(20 + dx, y + dy, 20 + dz);
         eq(1, Shapes.thinBoxes(alone, true).length, "a lone pane collides as a post");
         eq(5, Shapes.thinBoxes(alone, false).length, "a lone pane is drawn as the little plus (post and four arms)");
         w.setBlock(21, y, 20, Shapes.PANE0 + 3);
         w.setBlock(20, y, 19, Blocks.STONE);
         double[][] arms = Shapes.thinBoxes(alone, true);
         eq(3, arms.length, "post, an arm to the east pane and an arm to the stone north of it");
         w.setBlock(19, y, 20, Blocks.FLOWER_RED);
         eq(3, Shapes.thinBoxes(alone, true).length, "a flower is nothing to join");
         w.setBlock(20, y, 21, 12);
         eq(3, Shapes.thinBoxes(alone, true).length, "leaves are nothing to join");

         int wy = y + 3;
         w.setBlock(30, wy, 30, Shapes.WALL0);
         Shapes.Nbr wn = (dx, dy, dz) -> w.getBlock(30 + dx, wy + dy, 30 + dz);
         eq(1, Shapes.wallBoxes(wn, true).length, "a lone wall is a post");
         w.setBlock(30, wy, 29, Shapes.WALL0);
         w.setBlock(30, wy, 31, Shapes.WALL0 + 1);
         double[][] straight = Shapes.wallBoxes(wn, true);
         eq(1, straight.length, "a straight run has no post, one long arm");
         check(straight[0][2] == 0.0 && straight[0][5] == 1.0 && straight[0][4] == 1.5, "the arm runs all through the block and is 1.5 high");
         w.setBlock(31, wy, 30, Shapes.WALL0);
         double[][] tee = Shapes.wallBoxes(wn, true);
         eq(4, tee.length, "a T: post and three arms");
         double[][] drawn = Shapes.wallBoxes(wn, false);
         check(drawn[0][4] == 1.0 && drawn[1][4] == 0.875, "drawn: the post is 1 high, the arms 14/16");
         check(Shapes.isDyn(Shapes.WALL0) && Shapes.isDyn(Shapes.BARS) && !Shapes.isDyn(Shapes.CARPET0) && !Shapes.isDyn(Shapes.gate(0, false, 0)), "dynamic shapes");
      });

      test("shapes: fences of every wood join to each other, to gates (along the gate) and to solid blocks", () -> {
         int sp = Shapes.FENCE0;
         check(Blocks.isFence(sp) && Blocks.isFence(Blocks.FENCE) && Blocks.isFence(Shapes.FENCE0 + 4) && !Blocks.isFence(Shapes.WALL0), "isFence");
         check(Shapes.fenceLinks(Shapes.gate(0, false, 0), 2) && Shapes.fenceLinks(Shapes.gate(0, false, 1), 3), "a gate along x is reached from the east and the west");
         check(!Shapes.fenceLinks(Shapes.gate(0, false, 0), 0) && Shapes.fenceLinks(Shapes.gate(0, false, 2), 0), "... and a gate along z from north and south");
         check(!Shapes.fenceLinks(Shapes.CARPET0, 1), "a carpet is nothing to join");
      });

      test("shapes: you walk into a closed gate and through an open one, a wall is a block and a half high, a carpet is thin", () -> {
         Game g = newGame(0);
         World w = g.world;
         int y = GROUND + 1;
         w.setBlock(20, y, 20, Shapes.gate(0, false, 0)); // along x, closed
         double[] bb = {20.2, y, 19.9, 20.8, y + 1.8, 20.1}; // somebody standing just north of it
         check(!Phys.collides(w, bb), "next to the gate is free");
         double[] bb2 = {20.2, y, 20.4, 20.8, y + 1.8, 20.6};
         check(Phys.collides(w, bb2), "inside the closed gate is blocked");
         double[] low = {20.2, y + 1.55, 20.4, 20.8, y + 3.0, 20.6};
         check(!Phys.collides(w, low), "above 1.5 is free");
         w.setBlock(20, y, 20, Shapes.gate(0, true, 0));
         check(!Phys.collides(w, bb2), "the middle of an open gate is free");
         double[] post = {20.02, y, 20.4, 20.1, y + 1.8, 20.6};
         check(Phys.collides(w, post), "its posts still stand");

         w.setBlock(25, y, 25, Shapes.WALL0);
         double[] over = {25.3, y + 1.2, 25.3, 25.7, y + 3.0, 25.7};
         check(Phys.collides(w, over), "the wall post reaches 1.5 up");
         w.setBlock(26, y, 25, Shapes.CARPET0);
         double[] feet = {26.2, y + 0.0625, 25.2, 26.8, y + 1.9, 25.8};
         check(!Phys.collides(w, feet), "a carpet is only 1/16 high: standing on it is free");
         double[] sunk = {26.2, y, 25.2, 26.8, y + 1.9, 25.8};
         check(Phys.collides(w, sunk), "... but your feet do not go into it");
      });

      test("shapes: a ray hits the arms of a wall but passes through the empty corner of an L", () -> {
         Game g = newGame(0);
         World w = g.world;
         int y = GROUND + 1;
         w.setBlock(40, y, 40, Shapes.WALL0);
         w.setBlock(41, y, 40, Shapes.WALL0);
         w.setBlock(40, y, 41, Shapes.WALL0);
         // from the empty corner (41, 41) side: aim at the point (40.9, y + 0.5, 40.9), which is outside every box of the block at (40, 40)
         World.Hit miss = w.raycast(40.95, y + 0.5, 39.2, 0.0, 0.0, 1.0, 0.7);
         check(miss == null || miss.x != 40 || miss.z != 40, "a ray at x = 40.95 along z passes the wall block at (40, 40): its arm to the east is 5/16 .. 11/16 across");
         World.Hit hitArm = w.raycast(40.5, y + 0.5, 38.5, 0.0, 0.0, 1.0, 3.0);
         check(hitArm != null && hitArm.x == 40 && hitArm.z == 40, "a ray at x = 40.5 hits the post");
         World.Hit hitEast = w.raycast(41.6, y + 0.5, 40.5, -1.0, 0.0, 0.0, 3.0);
         check(hitEast != null && hitEast.x == 41, "from the east you hit the next block's arm first");
      });

      test("shapes: right click opens and closes gates and wooden trapdoors, the iron trapdoor stays shut", () -> {
         Game g = newGame(1);
         World w = g.world;
         int y = GROUND + 1;
         w.setBlock(22, y, 22, Shapes.gate(1, false, 2));
         g.target = hit(22, y, 22, 0, 1, 0);
         check(g.useBlock(), "use on a gate");
         eq(Shapes.gate(1, true, 2), w.getBlock(22, y, 22), "the gate is open now");
         check(g.useBlock(), "use again");
         eq(Shapes.gate(1, false, 2), w.getBlock(22, y, 22), "closed again");
         w.setBlock(23, y, 22, Shapes.trap(0, false, false, 1));
         g.target = hit(23, y, 22, 0, 1, 0);
         check(g.useBlock(), "use on a trapdoor");
         eq(Shapes.trap(0, false, true, 1), w.getBlock(23, y, 22), "it stands up");
         w.setBlock(24, y, 22, Shapes.trap(6, false, false, 0));
         g.target = hit(24, y, 22, 0, 1, 0);
         check(!g.useBlock(), "iron: nothing happens by hand");
         eq(Shapes.trap(6, false, false, 0), w.getBlock(24, y, 22), "still shut");
      });

      test("shapes: placing - a gate faces the way you look, a trapdoor sits on the wall or the floor, a carpet needs a floor", () -> {
         Game g = newGame(1);
         World w = g.world;
         int y = GROUND + 1;
         g.player.setPos(8.5, y, 8.5);
         g.player.yaw = 0.0F; // looks +z (south)
         hold(g, Shapes.gate(3, false, 0), 8);
         g.target = hit(12, GROUND, 12, 0, 1, 0);
         check(g.placeBlock(), "place a gate");
         eq(Shapes.gate(3, false, 1), w.getBlock(12, y, 12), "faces south (1)");
         g.player.yaw = -90.0F; // looks east
         g.target = hit(14, GROUND, 12, 0, 1, 0);
         check(g.placeBlock(), "place another");
         eq(Shapes.gate(3, false, 2), w.getBlock(14, y, 12), "faces east (2)");

         // a trapdoor on the east face of a block: it hangs on that block (the hinge is on the west side of the new cell)
         w.setBlock(16, y, 16, Blocks.STONE);
         hold(g, Shapes.trap(0, false, false, 0), 8);
         World.Hit side = hit(16, y, 16, 1, 0, 0);
         side.fy = 0.2;
         g.target = side;
         check(g.placeBlock(), "trapdoor on a wall, lower half");
         eq(Shapes.trap(0, false, false, 3), w.getBlock(17, y, 16), "bottom half, hinge side west (3)");
         World.Hit upper = hit(16, y, 16, 0, 0, 1);
         upper.fy = 0.8;
         g.target = upper;
         check(g.placeBlock(), "trapdoor on a wall, upper half");
         eq(Shapes.trap(0, true, false, 0), w.getBlock(16, y, 17), "top half, hinge on the north side (0)");
         g.target = hit(20, GROUND, 20, 0, 1, 0);
         g.player.yaw = 180.0F; // looks north
         check(g.placeBlock(), "trapdoor on the floor");
         eq(Shapes.trap(0, false, false, 0), w.getBlock(20, y, 20), "on the floor: bottom half");
         w.setBlock(18, y + 3, 18, Blocks.STONE);
         World.Hit under = hit(18, y + 3, 18, 0, -1, 0);
         g.target = under;
         check(g.placeBlock(), "trapdoor under a block");
         eq(Shapes.trap(0, true, false, 0), w.getBlock(18, y + 2, 18), "under a block: top half");

         hold(g, Shapes.CARPET0 + 5, 4);
         g.target = hit(30, GROUND, 30, 0, 1, 0);
         check(g.placeBlock(), "carpet on the ground");
         eq(Shapes.CARPET0 + 5, w.getBlock(30, y, 30), "a lime carpet");
         w.setBlock(34, y + 3, 34, Blocks.STONE);
         g.target = hit(34, y + 3, 34, 1, 0, 0); // the side of a floating block: there is nothing underneath the new cell
         check(!g.placeBlock(), "a carpet floating in the air is refused");
         eq(0, w.getBlock(35, y + 3, 34), "nothing there");
         eq(4, g.inv[g.sel].n, "creative mode does not use the carpets up");
      });

      test("shapes: a carpet comes loose when its floor is taken away, and drops itself", () -> {
         Game g = newGame(0);
         World w = g.world;
         int y = GROUND + 1;
         w.setBlock(30, y - 1, 30, Blocks.STONE);
         w.setBlock(30, y, 30, Shapes.CARPET0);
         w.setBlock(30, y - 1, 30, 0);
         tick(w, 3);
         eq(0, w.getBlock(30, y, 30), "the carpet is gone");
         check(GameTests.countItems(w, Shapes.CARPET0) == 1, "and lies there as an item");
      });

      test("shapes: crafting - panes, bars, walls, carpets and the wooden things in the wood of their planks", () -> {
         Stack[] grid = new Stack[9];
         for (int i = 0; i < 6; i++) {
            grid[i] = new Stack(Blocks.GLASS, 1);
         }

         Stack out = Recipes.find(grid, 3, 3);
         check(out != null && out.id == Shapes.PANE0 && out.n == 16, "6 glass -> 16 panes");
         for (int i = 0; i < 6; i++) {
            grid[i] = new Stack(Blocks.GLASS0 + 9, 1);
         }

         out = Recipes.find(grid, 3, 3);
         check(out != null && out.id == Shapes.PANE0 + 10 && out.n == 16, "6 cyan glass -> 16 cyan panes");
         for (int i = 0; i < 6; i++) {
            grid[i] = new Stack(Items.IRON_INGOT, 1);
         }

         out = Recipes.find(grid, 3, 3);
         check(out != null && out.id == Shapes.BARS && out.n == 16, "6 iron -> 16 bars");
         for (int i = 0; i < 6; i++) {
            grid[i] = new Stack(Blocks.MOSSY_COBBLE, 1);
         }

         out = Recipes.find(grid, 3, 3);
         check(out != null && out.id == Shapes.WALL0 + 1 && out.n == 6, "6 mossy cobblestone -> 6 mossy walls");
         grid = new Stack[9];
         grid[0] = new Stack(Blocks.WOOL0 + 14, 1);
         grid[1] = new Stack(Blocks.WOOL0 + 14, 1);
         out = Recipes.find(grid, 3, 3);
         check(out != null && out.id == Shapes.CARPET0 + 14 && out.n == 3, "2 red wool -> 3 red carpets");

         for (int sp = 0; sp < 6; sp++) {
            int planks = Blocks313.plank(sp);
            grid = new Stack[9];
            grid[0] = new Stack(planks, 1);
            grid[1] = new Stack(Items.STICK, 1);
            grid[2] = new Stack(planks, 1);
            grid[3] = new Stack(planks, 1);
            grid[4] = new Stack(Items.STICK, 1);
            grid[5] = new Stack(planks, 1);
            out = Recipes.find(grid, 3, 3);
            int fence = sp == 0 ? Blocks.FENCE : Shapes.FENCE0 + sp - 1;
            check(out != null && out.id == fence && out.n == 3, "fence of wood " + sp);
            grid = new Stack[9];
            grid[0] = new Stack(Items.STICK, 1);
            grid[1] = new Stack(planks, 1);
            grid[2] = new Stack(Items.STICK, 1);
            grid[3] = new Stack(Items.STICK, 1);
            grid[4] = new Stack(planks, 1);
            grid[5] = new Stack(Items.STICK, 1);
            out = Recipes.find(grid, 3, 3);
            check(out != null && out.id == Shapes.gate(sp, false, 0) && out.n == 1, "gate of wood " + sp);
            grid = new Stack[9];
            for (int i = 0; i < 6; i++) {
               grid[i] = new Stack(planks, 1);
            }

            out = Recipes.find(grid, 3, 3);
            check(out != null && out.id == Shapes.trap(sp, false, false, 0) && out.n == 2, "trapdoor of wood " + sp);
         }

         grid = new Stack[9];
         grid[0] = new Stack(Blocks.PLANKS, 1);
         grid[1] = new Stack(Items.STICK, 1);
         grid[2] = new Stack(Blocks313.PLANKS_SPRUCE, 1);
         grid[3] = new Stack(Blocks.PLANKS, 1);
         grid[4] = new Stack(Items.STICK, 1);
         grid[5] = new Stack(Blocks.PLANKS, 1);
         out = Recipes.find(grid, 3, 3);
         check(out != null && out.id == Blocks.FENCE, "planks of two woods still make an oak fence, as before");
         grid = new Stack[9];
         grid[0] = new Stack(Items.IRON_INGOT, 1);
         grid[1] = new Stack(Items.IRON_INGOT, 1);
         grid[3] = new Stack(Items.IRON_INGOT, 1);
         grid[4] = new Stack(Items.IRON_INGOT, 1);
         out = Recipes.find(grid, 3, 3);
         check(out != null && out.id == Shapes.trap(6, false, false, 0), "4 iron in a square -> an iron trapdoor");
      });

      test("shapes: the mesher draws panes (joined or alone), walls, gates and trapdoors; stained panes go to the translucent buffer", () -> {
         Game g = newGame(0);
         World w = g.world;
         int y = GROUND + 1;
         int base = vertices(w, 0, 0);
         w.setBlock(5, y, 5, Shapes.PANE0);
         int one = vertices(w, 0, 0);
         check(one > base, "a pane makes vertices");
         w.setBlock(6, y, 5, Shapes.PANE0);
         w.setBlock(7, y, 5, Shapes.PANE0);
         int three = vertices(w, 0, 0);
         check(three > one && three < one * 3 + 8, "three panes in a row are drawn without the inside faces (" + one + " -> " + three + ")");
         Mesher.Result r0 = Mesher.build(w, w.chunk(0, 0), 3, false);
         int opaqueBefore = r0.nO;
         int transBefore = r0.nT;
         w.setBlock(9, y, 5, Shapes.PANE0 + 5);
         Mesher.Result r1 = Mesher.build(w, w.chunk(0, 0), 3, false);
         check(r1.nT > transBefore && r1.nO == opaqueBefore, "a stained pane is translucent (opaque " + opaqueBefore + " -> " + r1.nO + ", translucent " + transBefore + " -> " + r1.nT + ")");
         int before = vertices(w, 0, 0);
         w.setBlock(11, y, 5, Shapes.WALL0);
         w.setBlock(12, y, 5, Shapes.gate(0, false, 0));
         w.setBlock(13, y, 5, Shapes.trap(0, true, false, 0));
         w.setBlock(14, y, 5, Shapes.CARPET0 + 3);
         w.setBlock(15, y, 5, Shapes.BARS);
         check(vertices(w, 0, 0) > before + 40, "wall, gate, trapdoor, carpet and bars all draw");
         w.setBlock(10, y, 5, Blocks.FENCE);
         w.setBlock(11, y, 6, Shapes.FENCE0 + 2);
         check(vertices(w, 0, 0) > before + 60, "fences of two woods draw");
      });

      test("shapes: a gate closed or open is one set of boxes, and the selection box covers what is drawn", () -> {
         for (int kind = 0; kind < Shapes.GATE_KINDS; kind++) {
            for (int state = 0; state < 8; state++) {
               int id = Shapes.GATE0 + kind * 8 + state;
               double[] b = Blocks.box[id];
               check(b != null && b[0] >= 0 && b[3] <= 1 && b[1] >= 0 && b[4] <= 1 && b[2] >= 0 && b[5] <= 1, "box of gate " + id + " is inside the block");
               check(b[3] > b[0] && b[4] > b[1] && b[5] > b[2], "box of gate " + id + " has a volume");
               for (double[] m : Shapes.gateBoxes(id)) {
                  check(m[0] >= b[0] - 1e-9 && m[3] <= b[3] + 1e-9 && m[2] >= b[2] - 1e-9 && m[5] <= b[5] + 1e-9, "the selection of gate " + id + " covers its boxes");
               }
            }
         }

         for (int id = Shapes.TRAP0; id < Shapes.TRAP0 + Shapes.TRAP_KINDS * 16; id++) {
            double[] b = Shapes.trapBox(id);
            check(b[3] > b[0] && b[4] > b[1] && b[5] > b[2] && b[0] >= 0 && b[3] <= 1 && b[1] >= 0 && b[4] <= 1 && b[2] >= 0 && b[5] <= 1, "trapdoor box " + id);
         }
      });
   }
}
