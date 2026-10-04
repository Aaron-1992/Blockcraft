package bc;

import java.util.Random;

/**
 * Update 3.13: the "Bonus Chest" switch of the Create New World screen. A chest with what you need for the first day (an axe, a pickaxe, wood,
 * food, torches) stands a few blocks from where you wake up.
 */
final class Bonus {
   private Bonus() {
   }

   /** Puts the chest on the ground near the spawn; returns its place {x, y, z} or null when there is no room within five blocks. */
   static int[] place(World w, int sx, int sy, int sz, long seed) {
      Random r = new Random(seed ^ 0x426F6E7573L);
      int first = r.nextInt(8);
      for (int ring = 2; ring <= 5; ring++) {
         for (int k = 0; k < 8 * ring; k++) {
            int i = (k + first * ring) % (8 * ring);
            // walk round the square ring of this radius
            int side = i / (2 * ring);
            int off = i % (2 * ring);
            int dx;
            int dz;
            switch (side) {
               case 0:
                  dx = -ring + off;
                  dz = -ring;
                  break;
               case 1:
                  dx = ring;
                  dz = -ring + off;
                  break;
               case 2:
                  dx = ring - off;
                  dz = ring;
                  break;
               default:
                  dx = -ring;
                  dz = ring - off;
                  break;
            }

            int x = sx + dx;
            int z = sz + dz;
            for (int y = sy + 3; y >= sy - 3; y--) {
               int below = w.getBlock(x, y - 1, z);
               int here = w.getBlock(x, y, z);
               int above = w.getBlock(x, y + 1, z);
               boolean ground = below != 0 && Blocks.solid[below] && Blocks.opaque[below] && !Blocks.isLeaves(below) && !Blocks.isAnyLog(below) && !Blocks.isFluid(below);
               if (ground && (here == 0 || Blocks.replaceable[here]) && !Blocks.isFluid(here) && above == 0) {
                  w.setBlock(x, y, z, Blocks.CHEST);
                  TileEntity t = w.getOrCreateTile(x, y, z);
                  if (t != null) {
                     fill(t, r);
                  }

                  return new int[]{x, y, z};
               }

               if (below != 0 && Blocks.solid[below] && !Blocks.isLeaves(below)) {
                  break; // the first solid thing from the top is the ground of this column: nothing free on it
               }
            }
         }
      }

      return null;
   }

   /** The contents: tools for wood and stone, logs of two kinds, planks, sticks, food and torches. */
   static void fill(TileEntity t, Random r) {
      java.util.ArrayList<Stack> list = new java.util.ArrayList<>();
      list.add(new Stack(Items.toolId(Items.C_AXE, 0), 1));
      list.add(new Stack(Items.toolId(Items.C_PICK, 0), 1));
      list.add(new Stack(Items.toolId(r.nextBoolean() ? Items.C_AXE : Items.C_PICK, 1), 1));
      int[] logs = new int[]{Blocks.LOG_OAK, Blocks.LOG_BIRCH, Blocks.LOG_SPRUCE};
      int a = r.nextInt(3);
      list.add(new Stack(logs[a], 4 + r.nextInt(5)));
      list.add(new Stack(logs[(a + 1 + r.nextInt(2)) % 3], 3 + r.nextInt(4)));
      list.add(new Stack(Blocks.PLANKS, 8 + r.nextInt(9)));
      list.add(new Stack(Items.STICK, 6 + r.nextInt(8)));
      list.add(new Stack(Items.BREAD, 3 + r.nextInt(3)));
      list.add(new Stack(Items.APPLE, 3 + r.nextInt(4)));
      list.add(new Stack(Blocks.TORCH, 8 + r.nextInt(9)));
      if (r.nextBoolean()) {
         list.add(new Stack(Items.COAL, 2 + r.nextInt(4)));
      }

      for (Stack s : list) {
         for (int tries = 0; tries < 60; tries++) {
            int slot = r.nextInt(t.slots.length);
            if (t.slots[slot] == null) {
               t.slots[slot] = s;
               break;
            }
         }
      }
   }
}
