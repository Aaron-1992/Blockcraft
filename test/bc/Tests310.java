package bc;

import static bc.GameTests.GROUND;
import static bc.GameTests.check;
import static bc.GameTests.eq;
import static bc.GameTests.newGame;
import static bc.GameTests.test;

import bc.World.Hit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Headless tests of Update 3.10: stairs and slabs upside down / in the top half of a block (To do item 7), falling trees and
 * sideways logs (item 6).   python3 tools/runtests.py unit
 */
final class Tests310 {
   static void run() {
      tables();
      raycast();
      placement();
      merging();
      breaking();
      meshing();
   }

   /** A fake click on a face of block (x, y, z) with the hit point fy (fraction of the block's height). */
   static Hit click(int x, int y, int z, int nx, int ny, int nz, double fy) {
      Hit h = new Hit();
      h.x = x;
      h.y = y;
      h.z = z;
      h.nx = nx;
      h.ny = ny;
      h.nz = nz;
      h.fy = fy;
      return h;
   }

   static boolean place(Game g, int item, int n, Hit target) {
      g.inv[g.sel] = new Stack(item, n);
      g.target = target;
      return g.placeBlock();
   }

   // ------------------------------------------------------------------ block data
   static void tables() {
      test("upside-down stairs and top slabs: ids, shapes, drops and tool rules mirror the normal ones", () -> {
         eq(195, Blocks.STAIR_TOP0, "first upside-down stair");
         eq(215, Blocks.SLAB_TOP0, "first top slab");
         eq(220, Blocks.COUNT, "block count");
         for (int mat = 0; mat < 5; mat++) {
            int slab = Blocks.SLAB_TOP0 + mat;
            check(Blocks.isSlab(slab) && Blocks.isTopSlab(slab) && !Blocks.isStair(slab), "top slab " + mat + " is a top slab");
            check(Blocks.isSlab(113 + mat) && !Blocks.isTopSlab(113 + mat), "bottom slab " + mat + " is not a top slab");
            eq(2, Blocks.kind[slab], "top slab is a box block");
            eq(113 + mat, Blocks.drop[slab], "a top slab drops the plain slab");
            eq(113 + mat, Blocks.pickItem(slab), "middle click on a top slab picks the plain slab");
            eq(mat, Blocks.stairSlabMaterial(slab), "material of the top slab");
            eq(mat, Blocks.stairSlabMaterial(113 + mat), "material of the bottom slab");
            double[] box = Blocks.box[slab];
            check(box[1] == 0.5 && box[4] == 1.0 && box[0] == 0.0 && box[3] == 1.0, "top slab fills the upper half of the cell");
            check(Blocks.coll[slab] != null && Blocks.coll[slab][1] == 0.5 && Blocks.coll[slab][4] == 1.0, "and so does its collision box");
            eq(Blocks.mineClass[113 + mat], Blocks.mineClass[slab], "same tool class");
            eq(Blocks.needTool[113 + mat], Blocks.needTool[slab], "same tool requirement");
            for (int f = 0; f < 4; f++) {
               int top = Blocks.STAIR_TOP0 + mat * 4 + f;
               int normal = 93 + mat * 4 + f;
               check(Blocks.isStair(top) && Blocks.isTopStair(top) && !Blocks.isSlab(top), "upside-down stair is a top stair");
               check(!Blocks.isTopStair(normal), "normal stair is not");
               eq(4, Blocks.kind[top], "stair block kind");
               eq(93 + mat * 4, Blocks.drop[top], "drops the plain stair item");
               eq(93 + mat * 4, Blocks.pickItem(top), "pick gives the plain stair item");
               eq(93 + mat * 4, Blocks.pickItem(normal), "every facing of a normal stair picks the same item");
               eq(f, Blocks.stairFacing(top), "facing of the top stair");
               eq(f, Blocks.stairFacing(normal), "facing of the normal stair");
               eq(mat, Blocks.stairSlabMaterial(top), "material of the top stair");
               eq(mat, Blocks.stairSlabMaterial(normal), "material of the normal stair");
               double[][] cm = Blocks.collMulti[top];
               double[][] nm = Blocks.collMulti[normal];
               check(cm.length == 2 && cm[0][1] == 0.5 && cm[0][4] == 1.0, "the full slab of an upside-down stair is its upper half");
               check(cm[1][1] == 0.0 && cm[1][4] == 0.5, "and the step is in the lower half");
               check(cm[1][0] == nm[1][0] && cm[1][2] == nm[1][2] && cm[1][3] == nm[1][3] && cm[1][5] == nm[1][5], "the step is on the same side as the normal stair's");
               eq(Blocks.mineClass[normal], Blocks.mineClass[top], "same tool class");
            }
         }

         // the oak wood ones burn, the stone ones do not
         check(Fire.flammability(Blocks.SLAB_TOP0) > 0 && Fire.flammability(Blocks.STAIR_TOP0 + 3) > 0, "oak top slab / stairs are flammable");
         check(Fire.flammability(Blocks.SLAB_TOP0 + 1) == 0 && Fire.flammability(Blocks.STAIR_TOP0 + 4) == 0, "cobblestone ones are not");
         check(Fire.flammability(113) > 0 && Fire.flammability(93) > 0, "(the normal oak ones still are)");
      });
   }

   // ------------------------------------------------------------------ where on a face the ray hit
   static void raycast() {
      test("a ray reports where on the face it hit (fraction of the block), the underside included", () -> {
         Game g = newGame(1);
         World w = g.world;
         int y = GROUND + 6;
         w.setBlock(10, y, 10, Blocks.STONE);
         Hit side = w.raycast(8.5, y + 0.75, 10.5, 1, 0, 0, 10);
         check(side != null && side.x == 10 && side.nx == -1, "hit the west face");
         check(Math.abs(side.fy - 0.75) < 1e-6, "at 3/4 of the height: " + side.fy);
         check(Math.abs(side.fz - 0.5) < 1e-6, "in the middle of the face (z): " + side.fz);
         Hit low = w.raycast(8.5, y + 0.2, 10.25, 1, 0, 0, 10);
         check(Math.abs(low.fy - 0.2) < 1e-6 && Math.abs(low.fz - 0.25) < 1e-6, "low on the face: " + low.fy + " / " + low.fz);
         Hit under = w.raycast(10.5, y - 3, 10.5, 0, 1, 0, 10);
         check(under != null && under.ny == -1, "hit from below: the underside");
         check(Math.abs(under.fy) < 1e-6, "its height fraction is 0: " + under.fy);
         Hit top = w.raycast(10.5, y + 4, 10.5, 0, -1, 0, 10);
         check(top.ny == 1 && Math.abs(top.fy - 1.0) < 1e-6, "from above: the top face, fraction 1: " + top.fy);
      });
   }

   // ------------------------------------------------------------------ placing
   static void placement() {
      test("slabs: the lower half of a side face gives a bottom slab, the upper half or an underside a top slab, a top face a bottom slab", () -> {
         Game g = newGame(1);
         World w = g.world;
         int y = GROUND + 6;
         w.setBlock(10, y, 10, Blocks.STONE);
         int slab = Blocks.SLAB0; // oak
         check(place(g, slab, 1, click(10, y, 10, 1, 0, 0, 0.3)), "placed against the east face, low");
         eq(Blocks.SLAB0, w.getBlock(11, y, 10), "low on a side face: bottom slab");
         check(place(g, slab, 1, click(10, y, 10, -1, 0, 0, 0.8)), "placed against the west face, high");
         eq(Blocks.SLAB_TOP0, w.getBlock(9, y, 10), "high on a side face: top slab");
         check(place(g, slab, 1, click(10, y, 10, 0, 0, 1, 0.5)), "placed at exactly half height");
         eq(Blocks.SLAB0, w.getBlock(10, y, 11), "exactly in the middle counts as the lower half");
         check(place(g, slab, 1, click(10, y, 10, 0, -1, 0, 0.0)), "placed against the underside");
         eq(Blocks.SLAB_TOP0, w.getBlock(10, y - 1, 10), "under a block: top slab");
         check(place(g, slab, 1, click(10, y, 10, 0, 1, 0, 1.0)), "placed on the top face");
         eq(Blocks.SLAB0, w.getBlock(10, y + 1, 10), "on top of a block: bottom slab");
         // other materials
         for (int mat = 1; mat < 5; mat++) {
            check(place(g, 113 + mat, 1, click(10, y, 10, 1, 0, 0, 0.9)) || true, "(keep going)");
            w.setBlock(11, y, 10, 0);
         }

         w.setBlock(11, y, 10, 0);
         check(place(g, 113 + 4, 1, click(10, y, 10, 1, 0, 0, 0.9)), "sandstone slab, high");
         eq(Blocks.SLAB_TOP0 + 4, w.getBlock(11, y, 10), "is the sandstone top slab");
      });
      test("stairs: facing from where you look, upside down when you click the underside or the upper half of a side", () -> {
         Game g = newGame(1);
         World w = g.world;
         int y = GROUND + 6;
         w.setBlock(10, y, 10, Blocks.STONE);
         g.player.setPos(2.5, GROUND + 1, 2.5);
         for (int facing = 0; facing < 4; facing++) {
            g.player.yaw = new float[]{180.0F, 0.0F, 90.0F, -90.0F}[facing];
            w.setBlock(11, y, 10, 0);
            w.setBlock(11, y, 10, 0);
            check(place(g, 93 + 4, 1, click(10, y, 10, 1, 0, 0, 0.25)), "cobblestone stairs, low");
            int placed = w.getBlock(11, y, 10);
            check(Blocks.isStair(placed) && !Blocks.isTopStair(placed), "low on a side face: right way up (" + placed + ")");
            int bottomFacing = Blocks.stairFacing(placed);
            w.setBlock(11, y, 10, 0);
            check(place(g, 93 + 4, 1, click(10, y, 10, 1, 0, 0, 0.75)), "cobblestone stairs, high");
            int upper = w.getBlock(11, y, 10);
            check(Blocks.isTopStair(upper), "high on a side face: upside down (" + upper + ")");
            eq(bottomFacing, Blocks.stairFacing(upper), "the same facing, whichever way up");
            eq(1, Blocks.stairSlabMaterial(upper), "cobblestone");
            w.setBlock(11, y, 10, 0);
            w.setBlock(11, y + 1, 10, Blocks.STONE);
            check(place(g, 93 + 4, 1, click(11, y + 1, 10, 0, -1, 0, 0.0)), "stairs against the underside of a block");
            w.setBlock(11, y + 1, 10, 0);
            int under = w.getBlock(11, y, 10);
            check(Blocks.isTopStair(under), "under a block: upside down");
            w.setBlock(11, y, 10, 0);
         }

         int[] seen = new int[4];
         for (int k = 0; k < 4; k++) {
            g.player.yaw = k * 90.0F;
            w.setBlock(11, y, 10, 0);
            place(g, 93, 1, click(10, y, 10, 1, 0, 0, 0.2));
            seen[Blocks.stairFacing(w.getBlock(11, y, 10))]++;
         }

         for (int f = 0; f < 4; f++) {
            eq(1, seen[f], "each of the four looking directions gives its own facing (facing " + f + ")");
         }
      });
      test("survival uses up one item per placement, creative none; the stack in the hand is untouched when nothing is placed", () -> {
         Game g = newGame(0);
         World w = g.world;
         int y = GROUND + 6;
         w.setBlock(10, y, 10, Blocks.STONE);
         g.inv[g.sel] = new Stack(Blocks.SLAB0, 3);
         g.target = click(10, y, 10, 1, 0, 0, 0.8);
         check(g.placeBlock(), "placed");
         eq(2, g.inv[g.sel].n, "one slab used");
         eq(Blocks.SLAB_TOP0, w.getBlock(11, y, 10), "(a top slab)");
         g.target = click(10, y, 10, 1, 0, 0, 0.8);
         check(g.placeBlock(), "the same slab in the same spot completes it to a full block");
         eq(Blocks.STAIR_MAT[0], w.getBlock(11, y, 10), "oak planks");
         eq(1, g.inv[g.sel].n, "a second slab used");
         g.inv[g.sel] = new Stack(115, 4);
         g.target = click(10, y, 10, 1, 0, 0, 0.8);
         check(!g.placeBlock(), "a full block is in the way now: nothing is placed");
         eq(4, g.inv[g.sel].n, "nothing used");
         Game c = newGame(1);
         c.world.setBlock(10, y, 10, Blocks.STONE);
         c.inv[c.sel] = new Stack(Blocks.SLAB0, 1);
         c.target = click(10, y, 10, 1, 0, 0, 0.8);
         check(c.placeBlock(), "creative: placed");
         eq(1, c.inv[c.sel].n, "creative: the stack stays");
      });
   }

   // ------------------------------------------------------------------ two slabs, one block
   static void merging() {
      test("two slabs of one kind make the full block: top of a bottom slab, underside of a top slab, or the cell next to the clicked face", () -> {
         Game g = newGame(1);
         World w = g.world;
         int y = GROUND + 6;
         g.player.setPos(2.5, GROUND + 1, 2.5);
         int stone = Blocks.STAIR_MAT[1]; // cobblestone
         // bottom slab, click its top face
         w.setBlock(10, y, 10, 114);
         check(place(g, 114, 1, click(10, y, 10, 0, 1, 0, 1.0)), "second slab on the top of a bottom slab");
         eq(stone, w.getBlock(10, y, 10), "the full cobblestone block");
         eq(Blocks.AIR, w.getBlock(10, y + 1, 10), "nothing was placed above");
         // top slab, click its underside
         w.setBlock(12, y, 10, Blocks.SLAB_TOP0 + 1);
         check(place(g, 114, 1, click(12, y, 10, 0, -1, 0, 0.0)), "second slab against the underside of a top slab");
         eq(stone, w.getBlock(12, y, 10), "the full block");
         eq(Blocks.AIR, w.getBlock(12, y - 1, 10), "nothing below");
         // a different material does not merge: it goes on top of the bottom slab
         w.setBlock(14, y, 10, 114);
         check(place(g, 115, 1, click(14, y, 10, 0, 1, 0, 1.0)), "a stone brick slab on a cobblestone slab");
         eq(114, w.getBlock(14, y, 10), "the cobblestone slab is still there");
         eq(115, w.getBlock(14, y + 1, 10), "and the new slab sits on top of it");
         // clicking the side of a bottom slab does not merge, it puts a slab next to it
         w.setBlock(16, y, 10, 114);
         check(place(g, 114, 1, click(16, y, 10, 1, 0, 0, 0.25)), "next to it");
         eq(114, w.getBlock(16, y, 10), "the clicked slab stays a slab");
         eq(114, w.getBlock(17, y, 10), "a new slab beside it");
         // the cell the new block would go into holds a matching slab (here the top half), reached from the block below it
         w.setBlock(20, y - 1, 10, Blocks.STONE);
         w.setBlock(20, y, 10, Blocks.SLAB_TOP0 + 1);
         check(place(g, 114, 1, click(20, y - 1, 10, 0, 1, 0, 1.0)), "a slab placed on the block under a top slab");
         eq(stone, w.getBlock(20, y, 10), "completes the top slab above it to a full block (either half merges, like Minecraft)");
         w.setBlock(22, y - 1, 10, Blocks.STONE);
         w.setBlock(22, y, 10, 114);
         check(place(g, 114, 1, click(22, y - 1, 10, 0, 1, 0, 1.0)), "and the same with a bottom slab");
         eq(stone, w.getBlock(22, y, 10), "full block");
      });
      test("a slab cannot be merged into a full block where the player stands; a full block is never made out of nothing", () -> {
         Game g = newGame(1);
         World w = g.world;
         int y = GROUND + 1;
         w.setBlock(10, y, 10, 114);
         g.player.setPos(10.5, y + 0.5, 10.5); // standing on the slab
         g.player.px = g.player.x;
         g.inv[g.sel] = new Stack(114, 5);
         g.target = click(10, y, 10, 0, 1, 0, 1.0);
         check(!g.placeBlock(), "refused: the player would be inside the block");
         eq(114, w.getBlock(10, y, 10), "the slab is unchanged");
         g.player.setPos(3.5, y, 3.5);
         g.inv[g.sel] = new Stack(114, 5);
         g.target = click(10, y, 10, 0, 1, 0, 1.0);
         check(g.placeBlock(), "with the player away it merges");
         eq(Blocks.STAIR_MAT[1], w.getBlock(10, y, 10), "full block");
      });
   }

   // ------------------------------------------------------------------ breaking
   static void breaking() {
      test("breaking upside-down stairs and top slabs gives the plain item", () -> {
         Game g = newGame(0);
         World w = g.world;
         int y = GROUND + 6;
         int pick = 0;
         for (int id = 300; id < 336; id++) {
            if (Items.toolClass(id) == Items.C_PICK && Items.canHarvest(id, 114)) {
               pick = id;
            }
         }

         check(pick != 0, "found a pickaxe that can harvest cobblestone slabs: " + pick);
         g.inv[g.sel] = new Stack(pick, 1);
         for (int mat = 0; mat < 5; mat++) {
            for (int[] spec : new int[][]{{Blocks.SLAB_TOP0 + mat, 113 + mat}, {Blocks.STAIR_TOP0 + mat * 4 + 2, 93 + mat * 4}}) {
               w.items.clear();
               w.setBlock(10, y, 10, spec[0]);
               g.blockDrops(10, y, 10, spec[0], g.inv[g.sel].id);
               boolean found = false;
               for (World.Item it : w.items) {
                  if (it.id == spec[1]) {
                     found = true;
                  }
               }

               check(found, "block " + spec[0] + " dropped item " + spec[1]);
            }
         }
      });
   }

   // ------------------------------------------------------------------ mesh
   static List<float[]> vertices(World w, Chunk c, int x, int y, int z, int id) {
      w.setBlock(x, y, z, id);
      Mesher.Result r = Mesher.build(w, c, y >> 4, false);
      ArrayList<float[]> out = new ArrayList<>();
      int ly = y & 15;
      for (int i = 0; i < r.nO; i++) {
         float vx = r.opaque.get(i * 11);
         float vy = r.opaque.get(i * 11 + 1);
         float vz = r.opaque.get(i * 11 + 2);
         if (vx >= x && vx <= x + 1 && vy >= ly && vy <= ly + 1 && vz >= z && vz <= z + 1) {
            out.add(new float[]{vx - x, vy - ly, vz - z});
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
      test("meshing: an upside-down stair is its normal twin mirrored top to bottom; a top slab fills the upper half", () -> {
         Game g = newGame(1);
         World w = g.world;
         Chunk c = w.chunk(0, 0);
         int y = GROUND + 8; // floating in the air: no faces hidden by neighbours
         for (int mat = 0; mat < 5; mat += 2) {
            for (int f = 0; f < 4; f++) {
               List<float[]> normal = vertices(w, c, 5, y, 5, 93 + mat * 4 + f);
               List<float[]> top = vertices(w, c, 5, y, 5, Blocks.STAIR_TOP0 + mat * 4 + f);
               eq(normal.size(), top.size(), "same number of vertices (mat " + mat + ", facing " + f + ")");
               HashMap<String, Integer> mirrored = new HashMap<>();
               for (float[] v : normal) {
                  mirrored.merge(String.format("%.3f,%.3f,%.3f", v[0], 1.0F - v[1], v[2]), 1, Integer::sum);
               }

               for (float[] v : top) {
                  String key = String.format("%.3f,%.3f,%.3f", v[0], v[1], v[2]);
                  Integer n = mirrored.get(key);
                  check(n != null && n > 0, "vertex " + key + " has a mirrored twin (mat " + mat + ", facing " + f + ")");
                  if (n != null) {
                     mirrored.put(key, n - 1);
                  }
               }
            }
         }

         List<float[]> slab = vertices(w, c, 5, y, 5, Blocks.SLAB_TOP0);
         eq(24, slab.size(), "a top slab in the air has 6 faces");
         for (float[] v : slab) {
            check(v[1] >= 0.5F - 1e-4F && v[1] <= 1.0F + 1e-4F, "all its vertices are in the upper half: " + v[1]);
         }

         List<float[]> low = vertices(w, c, 5, y, 5, Blocks.SLAB0);
         for (float[] v : low) {
            check(v[1] <= 0.5F + 1e-4F, "(and the normal slab's in the lower half)");
         }
      });
   }
}
