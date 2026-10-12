package bc;

import java.util.LinkedHashMap;
import java.util.Map;

/** Scenes for {@link MeshView}: galleries of the blocks of Update 3.15 on a flat world (ground y = 60, blocks from y = 61). */
final class Scenes315 {
   private Scenes315() {
   }

   static final int Y = 61;
   private static final Map<String, MeshView.Scene> SCENES = new LinkedHashMap<>();

   static MeshView.Scene byName(String n) {
      return SCENES.get(n);
   }

   static String names() {
      return String.join(", ", SCENES.keySet());
   }

   private static void put(String name, double[] view, java.util.function.Consumer<World> build) {
      SCENES.put(name, new MeshView.Scene() {
         @Override
         public void build(World w) {
            build.accept(w);
         }

         @Override
         public double[] view() {
            return view;
         }
      });
   }

   private static void set(World w, int x, int y, int z, int id) {
      w.setBlock(x, y, z, id);
   }

   static {
      // panes, glass and bars in the first row, the walls and fences behind them
      put("panes", new double[]{8, 62, 3, 20, 35, 22}, w -> {
         set(w, 0, Y, 0, Shapes.PANE0);
         for (int i = 0; i < 4; i++) {
            set(w, 2 + i, Y, 0, Shapes.PANE0);
         }

         // a plus of panes, a corner, a pane on a stone block, a tall window
         set(w, 9, Y, 0, Shapes.PANE0);
         set(w, 8, Y, 0, Shapes.PANE0);
         set(w, 10, Y, 0, Shapes.PANE0);
         set(w, 9, Y, 1, Shapes.PANE0);
         set(w, 9, Y, -1, Shapes.PANE0);
         set(w, 13, Y, 0, Shapes.PANE0);
         set(w, 14, Y, 0, Shapes.PANE0);
         set(w, 13, Y, 1, Shapes.PANE0);
         set(w, 17, Y, 0, Blocks.STONE);
         set(w, 18, Y, 0, Shapes.PANE0);
         for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
               set(w, 21 + x, Y + y, 0, Shapes.PANE0);
            }
         }

         // the sixteen colours as one long wall
         for (int c = 0; c < 16; c++) {
            set(w, c, Y, 4, Shapes.PANE0 + 1 + c);
         }

         // the same colours as single panes, so that the top shows
         for (int c = 0; c < 16; c++) {
            set(w, c * 2, Y, 7, Shapes.PANE0 + 1 + c);
         }

         // iron bars
         set(w, 0, Y, 10, Shapes.BARS);
         for (int i = 0; i < 4; i++) {
            set(w, 2 + i, Y, 10, Shapes.BARS);
         }

         set(w, 8, Y, 10, Shapes.BARS);
         set(w, 9, Y, 10, Blocks.STONE);
         set(w, 8, Y, 11, Shapes.PANE0);
         set(w, 12, Y, 10, Shapes.BARS);
         set(w, 12, Y + 1, 10, Shapes.BARS);
         set(w, 12, Y + 2, 10, Shapes.BARS);
         set(w, 13, Y, 10, Shapes.BARS);
         set(w, 13, Y + 1, 10, Shapes.BARS);
         set(w, 13, Y + 2, 10, Shapes.BARS);
      });

      put("walls", new double[]{12, 62, 3, 20, 35, 20}, w -> {
         for (int m = 0; m < Shapes.WALLS; m++) {
            set(w, m * 3, Y, 0, Shapes.WALL0 + m);
         }

         // runs: straight, corner, T, cross, with a fence and a gate and a stone block
         for (int i = 0; i < 4; i++) {
            set(w, i, Y, 4, Shapes.WALL0);
         }

         set(w, 6, Y, 4, Shapes.WALL0);
         set(w, 7, Y, 4, Shapes.WALL0);
         set(w, 7, Y, 5, Shapes.WALL0);
         set(w, 10, Y, 4, Shapes.WALL0 + 1);
         set(w, 11, Y, 4, Shapes.WALL0 + 1);
         set(w, 12, Y, 4, Shapes.WALL0 + 1);
         set(w, 11, Y, 5, Shapes.WALL0 + 1);
         set(w, 15, Y, 4, Shapes.WALL0 + 2);
         set(w, 14, Y, 4, Shapes.WALL0 + 2);
         set(w, 16, Y, 4, Shapes.WALL0 + 2);
         set(w, 15, Y, 5, Shapes.WALL0 + 2);
         set(w, 15, Y, 3, Shapes.WALL0 + 2);
         set(w, 19, Y, 4, Shapes.WALL0 + 3);
         set(w, 20, Y, 4, Blocks.FENCE);
         set(w, 21, Y, 4, Shapes.WALL0 + 3);
         set(w, 22, Y, 4, Blocks.STONE);
         set(w, 23, Y, 4, Shapes.WALL0 + 3);
         // stacked: a wall on a wall
         set(w, 0, Y, 8, Shapes.WALL0 + 4);
         set(w, 0, Y + 1, 8, Shapes.WALL0 + 4);
         set(w, 1, Y, 8, Shapes.WALL0 + 4);
         set(w, 2, Y, 8, Shapes.WALL0 + 4);
         set(w, 2, Y + 1, 8, Shapes.WALL0 + 4);
         // fences
         for (int sp = 0; sp < 6; sp++) {
            int f = sp == 0 ? Blocks.FENCE : Shapes.FENCE0 + sp - 1;
            set(w, 6 + sp * 3, Y, 8, f);
            set(w, 7 + sp * 3, Y, 8, f);
         }
      });

      put("gates", new double[]{10, 62, 3, 20, 30, 16}, w -> {
         for (int k = 0; k < 6; k++) {
            int f = k == 0 ? Blocks.FENCE : Shapes.FENCE0 + k - 1;
            set(w, k * 4, Y, 0, f);
            set(w, k * 4 + 1, Y, 0, Shapes.gate(k, false, 0));
            set(w, k * 4 + 2, Y, 0, f);
            set(w, k * 4, Y, 3, f);
            set(w, k * 4 + 1, Y, 3, Shapes.gate(k, true, 0));
            set(w, k * 4 + 2, Y, 3, f);
            // a gate along z, facing east, closed and open
            set(w, k * 4, Y, 6, f);
            set(w, k * 4, Y, 7, Shapes.gate(k, false, 2));
            set(w, k * 4, Y, 8, f);
            set(w, k * 4 + 2, Y, 6, f);
            set(w, k * 4 + 2, Y, 7, Shapes.gate(k, true, 2));
            set(w, k * 4 + 2, Y, 8, f);
         }
      });

      put("traps", new double[]{10, 62, 3, 20, 40, 16}, w -> {
         for (int k = 0; k < 7; k++) {
            set(w, k * 3, Y, 0, Shapes.trap(k, false, false, 0));
            set(w, k * 3, Y, 2, Shapes.trap(k, true, false, 0));
            set(w, k * 3, Y + 2, 4, Shapes.trap(k, true, false, 0));
            set(w, k * 3, Y, 6, Blocks.STONE);
            set(w, k * 3, Y, 7, Shapes.trap(k, false, true, 0));
            set(w, k * 3 + 1, Y, 6, Blocks.STONE);
            set(w, k * 3 + 1, Y, 7, Shapes.trap(k, false, true, 1));
         }

         for (int s = 0; s < 4; s++) {
            set(w, 4 + s, Y, 10, Blocks.STONE);
            set(w, 4 + s, Y, 11, Shapes.trap(0, false, true, s == 0 ? 0 : s));
         }
      });

      put("beds", new double[]{10, 62, 3, 25, 35, 15}, w -> {
         int[][] dir = {{0, -1}, {0, 1}, {1, 0}, {-1, 0}};
         for (int f = 0; f < 4; f++) {
            for (int i = 0; i < 6; i++) {
               int c = (f * 5 + i * 3) % 16;
               int x = 3 + i * 3 + (f >= 2 ? 0 : 0);
               int z = 3 + f * 4;
               set(w, x, Y, z, Decor315.foot(c, f));
               set(w, x + dir[f][0], Y, z + dir[f][1], Decor315.head(c, f));
            }
         }
      });

      put("banners", new double[]{10, 62, 6, 20, 25, 18}, w -> {
         for (int c = 0; c < 16; c++) {
            set(w, c * 2, Y, 2, Decor315.STAND0 + c * 4 + (c % 4));
         }

         for (int c = 0; c < 16; c++) {
            set(w, c * 2, Y, 6, Blocks.STONE);
            set(w, c * 2, Y + 1, 6, Blocks.STONE);
            set(w, c * 2, Y + 1, 5, Decor315.WALL0 + c * 4 + 0);
         }
      });

      put("carpets", new double[]{8, 61, 3, 15, 40, 14}, w -> {
         for (int c = 0; c < 16; c++) {
            set(w, c, Y, 0, Shapes.CARPET0 + c);
         }

         for (int c = 0; c < 4; c++) {
            set(w, c, Y, 3, Blocks.WOOL0 + c);
            set(w, c, Y + 1, 3, Shapes.CARPET0 + c);
         }
      });

      put("redstone", new double[]{7.5, 61.2, 4.5, 18, 32, 13}, w -> {
         // Row z=2: Pistons (Up, Down, North, South, East, West) + Sticky Piston Up + Extended Piston & Head
         for (int f = 0; f < 6; f++) {
            set(w, 1 + f * 2, Y, 2, Redstone319.PISTON + f);
         }
         set(w, 13, Y, 2, Redstone319.STICKY_PISTON + 1);
         set(w, 15, Y, 2, Redstone319.PISTON_EXT + 3);
         set(w, 15, Y, 3, Redstone319.PISTON_HEAD + 3);

         // Row z=4: Observers (4 facings) + Repeaters OFF/ON (4 facings)
         for (int f = 0; f < 4; f++) {
            set(w, 1 + f * 2, Y, 4, Redstone319.OBSERVER + f);
            set(w, 9 + f * 2, Y, 4, Redstone319.REPEATER_ON + f);
         }

         // Row z=6: Torch, Lever OFF/ON, Buttons, Plates, Daylight Sensor, Note Block, Lamp OFF/ON, Slime, Sea Lantern, Sideways Logs
         set(w, 1, Y, 6, Redstone319.REDSTONE_TORCH);
         set(w, 2, Y, 6, Redstone319.LEVER_OFF);
         set(w, 3, Y, 6, Redstone319.LEVER_ON);
         set(w, 4, Y, 6, Redstone319.STONE_BUTTON);
         set(w, 5, Y, 6, Redstone319.WOOD_BUTTON);
         set(w, 6, Y, 6, Redstone319.DAYLIGHT_SENSOR);
         set(w, 7, Y, 6, Redstone319.NOTE_BLOCK);
         set(w, 8, Y, 6, Redstone319.REDSTONE_LAMP);
         set(w, 9, Y, 6, Redstone319.REDSTONE_LAMP_LIT);
         set(w, 10, Y, 6, Redstone319.SLIME_BLOCK);
         set(w, 11, Y, 6, Redstone319.SEA_LANTERN);
         set(w, 12, Y, 6, 220); // sideways oak log
         set(w, 13, Y, 6, 222); // sideways birch log
         set(w, 14, Y, 6, 224); // sideways spruce log
      });
   }
}
