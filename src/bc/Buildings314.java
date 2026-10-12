package bc;

import java.util.LinkedHashMap;

/**
 * Update 3.14: the buildings of the villages, as templates ({@link Tpl}). Every one looks north (-z) at the street; row 0 is the doorstep.
 * The houses are cottages: a body of w x db cells (z = 1 .. db) with a floor of 'E' inside a ring of foundation 'C', posts of logs in the corners,
 * wall between them, a timber beam under the roof and a gable roof along x with eaves at the front and the back.
 */
final class Buildings314 {
   private Buildings314() {
   }

   static final LinkedHashMap<String, Tpl> ALL = new LinkedHashMap<>();

   static Tpl get(String name) {
      return ALL.get(name);
   }

   private static void add(Tpl t) {
      ALL.put(t.name, t);
   }

   /** The bare cottage: floor, foundation, walls, posts, beams, a door in the front wall at x = doorX, a gable roof (or a flat one with a parapet). */
   static Tpl.B cottage(String name, int w, int db, int wh, int doorX, boolean flat) {
      int d = db + 2;
      int levels = (d + 1) / 2;
      Tpl.B b = new Tpl.B(name, w, flat ? wh + 3 : wh + 2 + levels, d);
      b.box('E', 0, 0, 1, w - 1, 0, db);
      b.walls('C', 0, 0, 1, w - 1, 0, db);
      b.box('.', 1, 1, 2, w - 2, wh, db - 1);
      b.walls('#', 0, 1, 1, w - 1, wh, db);
      b.box('L', 0, 1, 1, 0, wh, 1);
      b.box('L', w - 1, 1, 1, w - 1, wh, 1);
      b.box('L', 0, 1, db, 0, wh, db);
      b.box('L', w - 1, 1, db, w - 1, wh, db);
      b.box('X', 1, wh, 1, w - 2, wh, 1);
      b.box('X', 1, wh, db, w - 2, wh, db);
      b.box('Z', 0, wh, 2, 0, wh, db - 1);
      b.box('Z', w - 1, wh, 2, w - 1, wh, db - 1);
      b.set('!', doorX, 1, 1).set('!', doorX, 2, 1);
      b.set('m', doorX, 0, 0);
      if (flat) {
         b.box('R', 0, wh + 1, 1, w - 1, wh + 1, db);
         b.walls('_', 0, wh + 2, 1, w - 1, wh + 2, db);
      } else {
         b.gableX(wh + 1, 0, w - 1, 1, db, '#');
      }

      return b;
   }

   static Tpl.B cottage(String name, int w, int db, int wh, int doorX) {
      return cottage(name, w, db, wh, doorX, false);
   }

   /** A pane in the wall at (x, y, z). */
   private static void window(Tpl.B b, int x, int z) {
      b.set('G', x, 2, z);
   }

   /** The chimney: a furnace in the room at (x, 1, z) and a column of cobblestone up to the height top. */
   private static void chimney(Tpl.B b, int x, int z, int top) {
      b.set('u', x, 1, z);
      for (int y = 2; y <= top; y++) {
         b.set('C', x, y, z);
      }
   }

   /** Two fence posts and flowers in front of the house: a little veranda under the eaves. */
   private static void veranda(Tpl.B b, int w, int wh, boolean flowers) {
      for (int y = 1; y <= wh; y++) {
         b.set('F', 1, y, 0);
         b.set('F', w - 2, y, 0);
      }

      if (flowers) {
         b.set('z', 0, 1, 0);
         b.set('z', w - 1, 1, 0);
      }
   }

   /** A bed lying along z with its foot at (x, y, z) and its head at z + 1. */
   private static void bed(Tpl.B b, int x, int y, int z) {
      b.set('b', x, y, z).set('B', x, y, z + 1);
   }

   private static void houses(boolean flat) {
      String f = flat ? "_flat" : "";
      // ---- the hut: 5 x 5, one room
      {
         Tpl.B b = cottage("hut" + f, 5, 5, 3, 2, flat);
         window(b, 1, 1);
         window(b, 3, 1);
         window(b, 0, 3);
         window(b, 4, 3);
         bed(b, 1, 1, 3);
         b.set('x', 3, 1, 4);
         b.set('*', 2, 2, 4);
         b.set('z', 1, 1, 0);
         b.set('z', 3, 1, 0);
         add(b.build());
      }
      // ---- the small house: 7 x 5, a bed, a table, a chest, a chimney, a veranda
      {
         Tpl.B b = cottage("house_s" + f, 7, 5, 3, 3, flat);
         window(b, 1, 1);
         window(b, 5, 1);
         window(b, 0, 3);
         window(b, 6, 3);
         window(b, 3, 5);
         bed(b, 1, 1, 3);
         b.set('k', 5, 1, 4);
         b.set('x', 4, 1, 4);
         b.set('*', 2, 2, 4);
         b.set('*', 4, 2, 2);
         b.set('+', 3, 1, 3);
         chimney(b, 5, 2, flat ? 5 : 8);
         if (!flat) {
            veranda(b, 7, 3, true);
         }

         add(b.build());
      }
      // ---- the medium house: 7 x 7, two beds, a table, a chimney, a bookshelf, a rug
      {
         Tpl.B b = cottage("house_m" + f, 7, 7, 3, 3, flat);
         window(b, 1, 1);
         window(b, 5, 1);
         window(b, 0, 3);
         window(b, 0, 5);
         window(b, 6, 3);
         window(b, 6, 5);
         window(b, 2, 7);
         window(b, 4, 7);
         bed(b, 1, 1, 5);
         bed(b, 5, 1, 5);
         b.set('k', 2, 1, 2);
         b.set('q', 4, 1, 2).set('q', 4, 2, 2);
         b.set('x', 3, 1, 6);
         b.set('*', 3, 2, 6);
         b.set('*', 1, 2, 4);
         b.box('+', 2, 1, 3, 4, 1, 4);
         chimney(b, 5, 2, flat ? 5 : 9);
         if (!flat) {
            veranda(b, 7, 3, true);
         }

         add(b.build());
      }
      // ---- the big house: 9 x 7 and two storeys, a ladder to the beds upstairs
      {
         Tpl.B b = cottage("house_l" + f, 9, 7, 7, 4, flat);
         b.box('E', 1, 4, 2, 7, 4, 6);
         b.box('X', 1, 4, 1, 7, 4, 1);
         b.box('X', 1, 4, 7, 7, 4, 7);
         b.box('Z', 0, 4, 2, 0, 4, 6);
         b.box('Z', 8, 4, 2, 8, 4, 6);
         for (int y = 1; y <= 5; y++) {
            b.set('H', 7, y, 4);
         }

         for (int[] p : new int[][]{{2, 1}, {6, 1}, {0, 3}, {0, 5}, {8, 3}, {8, 5}, {3, 7}, {5, 7}}) {
            b.set('G', p[0], 2, p[1]);
         }

         for (int[] p : new int[][]{{2, 1}, {4, 1}, {6, 1}, {0, 3}, {0, 5}, {8, 3}, {8, 5}, {2, 7}, {4, 7}, {6, 7}}) {
            b.set('G', p[0], 6, p[1]);
         }

         b.set('k', 1, 1, 2);
         b.set('x', 1, 1, 3);
         b.set('q', 1, 1, 6).set('q', 1, 2, 6).set('q', 2, 1, 6).set('q', 2, 2, 6);
         b.box('+', 3, 1, 3, 5, 1, 5);
         b.set('*', 4, 3, 6);
         b.set('*', 1, 3, 4);
         bed(b, 1, 5, 5);
         bed(b, 3, 5, 5);
         bed(b, 5, 5, 5);
         b.set('x', 7, 5, 6);
         b.set('*', 3, 6, 6);
         chimney(b, 7, 6, flat ? 9 : 13);
         if (!flat) {
            veranda(b, 9, 3, true);
         }

         add(b.build());
      }
   }

   private static void shops() {
      // ---- the library: bookshelves along the walls, a rug, tall windows
      {
         Tpl.B b = cottage("library", 9, 7, 4, 4);
         for (int z = 2; z <= 6; z++) {
            for (int y = 1; y <= 3; y++) {
               b.set('q', 1, y, z);
               b.set('q', 7, y, z);
            }
         }

         for (int x = 2; x <= 6; x++) {
            for (int y = 1; y <= 2; y++) {
               b.set('q', x, y, 6);
            }
         }

         b.box('+', 3, 1, 3, 5, 1, 5);
         b.set('k', 2, 1, 2).set('k', 6, 1, 2);
         b.set('*', 2, 3, 5);
         b.set('*', 6, 3, 5);
         for (int x : new int[]{2, 6}) {
            b.set('G', x, 3, 1);
         }

         b.set('G', 3, 3, 7).set('G', 5, 3, 7);
         b.set('G', 0, 3, 3).set('G', 0, 3, 5).set('G', 8, 3, 3).set('G', 8, 3, 5);
         b.set('G', 2, 2, 1).set('G', 6, 2, 1);
         add(b.build());
      }
      // ---- the smithy: the front is open under the roof, three furnaces along the back wall, a table, a chest
      {
         Tpl.B b = cottage("smithy", 9, 7, 3, 4);
         b.box('.', 2, 1, 1, 6, 2, 1);
         b.box('.', 2, 1, 1, 6, 2, 1);
         b.set('F', 2, 1, 1).set('F', 6, 1, 1);
         for (int x : new int[]{2, 3, 4}) {
            b.set('u', x, 1, 6);
         }

         b.set('k', 6, 1, 6);
         b.set('x', 7, 1, 6);
         b.set('h', 1, 1, 2);
         b.set('*', 5, 2, 6);
         b.set('*', 1, 2, 4);
         b.set('G', 0, 2, 3).set('G', 8, 2, 3);
         b.set('C', 7, 1, 2).set('C', 7, 2, 2);
         chimney(b, 4, 5, 9);
         b.set('C', 4, 1, 5);
         add(b.build());
      }
      // ---- the butcher: a counter in front of the door, hay and a chest at the back
      {
         Tpl.B b = cottage("butcher", 7, 7, 3, 3);
         b.set('G', 1, 2, 1).set('G', 5, 2, 1).set('G', 2, 2, 1).set('G', 4, 2, 1);
         b.set('G', 0, 2, 4).set('G', 6, 2, 4).set('G', 3, 2, 7);
         for (int x = 1; x <= 5; x++) {
            b.set('_', x, 1, 3);
         }

         b.set('.', 3, 1, 3);
         b.set('h', 1, 1, 6).set('h', 2, 1, 6);
         b.set('x', 5, 1, 6);
         b.set('k', 5, 1, 5);
         b.set('*', 2, 2, 6);
         b.set('*', 1, 2, 5);
         chimney(b, 1, 2, 9);
         add(b.build());
      }
   }

   private static void barns() {
      // ---- the long house: 11 x 5, three beds in a row
      {
         Tpl.B b = cottage("longhouse", 11, 5, 3, 5);
         for (int x : new int[]{1, 3, 7, 9}) {
            b.set('G', x, 2, 1);
         }

         b.set('G', 0, 2, 3).set('G', 10, 2, 3);
         b.set('G', 3, 2, 5).set('G', 5, 2, 5).set('G', 7, 2, 5);
         bed(b, 1, 1, 3);
         bed(b, 3, 1, 3);
         bed(b, 9, 1, 3);
         b.set('k', 7, 1, 4).set('x', 8, 1, 4);
         b.box('+', 4, 1, 2, 6, 1, 4);
         b.set('*', 2, 2, 4).set('*', 8, 2, 2);
         b.set('h', 5, 1, 4);
         chimney(b, 9, 2, 8);
         veranda(b, 11, 3, true);
         add(b.build());
      }
      // ---- the barn: a wide doorway with the gates folded back, hay bales inside
      {
         Tpl.B b = cottage("barn", 11, 9, 4, 5);
         b.box('.', 4, 1, 1, 6, 3, 1);
         b.set('f', 4, 1, 1).set('f', 4, 2, 1).set('f', 6, 1, 1).set('f', 6, 2, 1);
         b.set('G', 1, 3, 4).set('G', 9, 3, 4).set('G', 5, 3, 9);
         b.set('G', 0, 3, 5).set('G', 10, 3, 5);
         for (int y = 1; y <= 3; y++) {
            for (int z = 2; z <= 4; z++) {
               b.set('h', 1, y, z);
               b.set('h', 2, y, z);
            }
         }

         for (int y = 1; y <= 2; y++) {
            for (int x = 7; x <= 9; x++) {
               b.set('h', x, y, 8);
            }
         }

         b.set('h', 9, 1, 6).set('h', 9, 1, 7);
         for (int z = 4; z <= 8; z++) {
            b.set('F', 6, 1, z);
         }

         b.set('F', 7, 1, 4).set('F', 8, 1, 4);
         b.set('x', 3, 1, 8);
         b.set('*', 4, 3, 8);
         b.set('*', 3, 3, 2);
         b.set('*', 8, 3, 2);
         add(b.build());
      }
   }

   private static void yards() {
      // ---- a farm: 11 x 9, a fence with a gate in front, a water channel down the middle, four rows of crops
      {
         Tpl.B b = new Tpl.B("farm", 11, 4, 9);
         b.box('y', 0, 0, 0, 10, 0, 8);
         for (int z = 1; z <= 7; z++) {
            for (int x = 1; x <= 9; x++) {
               if (z == 4) {
                  b.set('o', x, 0, z);
               } else {
                  b.set('a', x, 0, z);
                  b.set('r', x, 1, z);
               }
            }
         }

         b.walls('F', 0, 1, 0, 10, 1, 8);
         b.set('f', 5, 1, 0);
         for (int[] p : new int[][]{{0, 0}, {10, 0}, {0, 8}, {10, 8}}) {
            b.set('F', p[0], 2, p[1]);
            b.set('T', p[0], 3, p[1]);
         }

         add(b.build());
      }
      // ---- a market stall: four posts, a striped awning of wool, wares on the counter
      {
         Tpl.B b = new Tpl.B("stall", 5, 5, 4);
         b.box('m', 0, 0, 1, 4, 0, 3);
         for (int[] p : new int[][]{{0, 1}, {4, 1}, {0, 3}, {4, 3}}) {
            b.box('F', p[0], 1, p[1], p[0], 3, p[1]);
         }

         for (int x = 0; x <= 4; x++) {
            for (int z = 0; z <= 3; z++) {
               b.set(x % 2 == 0 ? 'w' : 'W', x, 4, z);
            }
         }

         b.set('h', 1, 1, 1).set('x', 2, 1, 1).set('q', 3, 1, 1);
         b.set('k', 2, 1, 3);
         add(b.build());
      }
      // ---- the well: a shaft of cobblestone with water in it, four posts and a little roof
      {
         Tpl.B b = new Tpl.B("well", 5, 9, 5).sink(2);
         b.box('m', 0, 2, 0, 4, 2, 4);
         b.walls('C', 1, 0, 1, 3, 2, 3);
         b.box('C', 2, 0, 2, 2, 0, 2);
         b.set('o', 2, 1, 2);
         b.set('o', 2, 2, 2);
         for (int[] p : new int[][]{{1, 1}, {3, 1}, {1, 3}, {3, 3}}) {
            b.set('F', p[0], 3, p[1]);
            b.set('F', p[0], 4, p[1]);
         }

         b.hip(5, 1, 3, 1, 3);
         add(b.build());
      }
      // ---- an animal pen: a fence, a gate, a hay bale
      {
         Tpl.B b = new Tpl.B("pen", 7, 3, 7);
         b.walls('F', 0, 1, 0, 6, 1, 6);
         b.set('f', 3, 1, 0);
         b.set('h', 5, 1, 5).set('h', 1, 1, 5);
         b.set('z', 3, 1, 3);
         add(b.build());
      }
   }

   private static void tall() {
      // ---- the watchtower: cobblestone, a ladder inside, a platform with a rail and a pointed roof on four posts
      {
         Tpl.B b = new Tpl.B("tower", 5, 16, 6);
         b.box('E', 0, 0, 1, 4, 0, 5);
         b.walls('C', 0, 0, 1, 4, 0, 5);
         b.box('.', 1, 1, 2, 3, 8, 4);
         b.walls('C', 0, 1, 1, 4, 8, 5);
         b.set('!', 2, 1, 1).set('!', 2, 2, 1);
         b.set('m', 2, 0, 0);
         for (int y = 1; y <= 8; y++) {
            b.set('H', 3, y, 4);
         }

         b.box('E', 1, 4, 2, 3, 4, 4);
         b.set('H', 3, 4, 4);
         b.box('E', 0, 9, 1, 4, 9, 5);
         b.set('H', 3, 9, 4);
         b.set('.', 3, 9, 4);
         b.set('H', 3, 9, 4);
         b.walls('F', 0, 10, 1, 4, 10, 5);
         b.set('.', 3, 10, 4);
         for (int[] p : new int[][]{{0, 1}, {4, 1}, {0, 5}, {4, 5}}) {
            b.box('L', p[0], 10, p[1], p[0], 12, p[1]);
         }

         b.set('G', 2, 3, 1).set('G', 0, 3, 3).set('G', 4, 3, 3).set('G', 2, 3, 5);
         b.set('G', 2, 7, 1).set('G', 0, 7, 3).set('G', 4, 7, 3).set('G', 2, 7, 5);
         b.set('*', 1, 3, 2);
         b.set('*', 1, 7, 4);
         b.set('T', 2, 10, 3);
         b.hip(13, 1, 3, 2, 4);
         add(b.build());
      }
      // ---- the church: a belfry tower in front, the nave behind it with a steep roof, an altar at the back
      {
         Tpl.B b = new Tpl.B("church", 9, 15, 15);
         // the nave: x 1..7, z 5..14
         b.box('E', 1, 0, 5, 7, 0, 14);
         b.walls('C', 1, 0, 5, 7, 0, 14);
         b.box('.', 2, 1, 6, 6, 5, 13);
         b.walls('C', 1, 1, 5, 7, 5, 14);
         for (int[] p : new int[][]{{1, 5}, {7, 5}, {1, 14}, {7, 14}, {1, 9}, {7, 9}}) {
            b.box('L', p[0], 1, p[1], p[0], 5, p[1]);
         }

         b.box('X', 2, 5, 14, 6, 5, 14);
         b.box('Z', 1, 5, 6, 1, 5, 13);
         b.box('Z', 7, 5, 6, 7, 5, 13);
         for (int z : new int[]{7, 11}) {
            for (int y = 2; y <= 4; y++) {
               b.set('G', 1, y, z);
               b.set('G', 7, y, z);
            }
         }

         for (int y = 2; y <= 4; y++) {
            b.set('G', 4, y, 14);
         }

         b.gableZ(6, 1, 7, 5, 14, '#');
         // the aisle, the altar
         b.box('+', 4, 1, 7, 4, 1, 12);
         b.box('C', 3, 1, 13, 5, 1, 13);
         b.set('$', 4, 2, 13);
         b.set('*', 2, 3, 12);
         b.set('*', 6, 3, 12);
         b.set('*', 2, 3, 8);
         b.set('*', 6, 3, 8);
         b.set('q', 2, 1, 13).set('q', 6, 1, 13);
         // the tower: x 2..6, z 1..5, walls to y 9, the hall of the tower is the entrance
         b.box('E', 2, 0, 1, 6, 0, 5);
         b.walls('C', 2, 0, 1, 6, 0, 5);
         b.box('.', 3, 1, 2, 5, 9, 4);
         b.walls('C', 2, 1, 1, 6, 9, 5);
         b.set('!', 4, 1, 1).set('!', 4, 2, 1);
         b.set('m', 4, 0, 0);
         b.box('.', 4, 1, 5, 4, 3, 5);
         b.box('.', 3, 1, 5, 5, 3, 5);
         for (int[] p : new int[][]{{2, 1}, {6, 1}, {2, 5}, {6, 5}}) {
            b.box('L', p[0], 1, p[1], p[0], 9, p[1]);
         }

         b.set('G', 3, 3, 1).set('G', 5, 3, 1).set('G', 6, 3, 3);
         b.box('E', 3, 6, 2, 5, 6, 4);
         for (int y = 1; y <= 6; y++) {
            b.set('H', 3, y, 3);
         }

         b.set('.', 3, 6, 3);
         b.set('H', 3, 6, 3);
         for (int y = 7; y <= 8; y++) {
            b.set('H', 3, y, 3);
         }

         b.box('.', 4, 7, 1, 4, 8, 1);
         b.box('.', 4, 7, 5, 4, 8, 5);
         b.box('.', 6, 7, 3, 6, 8, 3);
         b.set('$', 4, 8, 3);
         b.set('*', 4, 3, 2);
         b.hip(10, 2, 6, 1, 5);
         add(b.build());
      }
   }

   private static void small() {
      // ---- a tool shed: 5 x 4, hay, a chest, a crafting table
      {
         Tpl.B b = cottage("shed", 5, 4, 3, 2);
         b.set('G', 0, 2, 2).set('G', 4, 2, 2).set('G', 2, 2, 4);
         b.set('h', 1, 1, 3).set('h', 1, 2, 3);
         b.set('x', 3, 1, 3);
         b.set('k', 3, 1, 2);
         b.set('*', 2, 2, 3);
         add(b.build());
      }
      // ---- a garden: flowers in beds with a path across, a bench of stairs and a lamp
      {
         Tpl.B b = new Tpl.B("garden", 7, 5, 6);
         b.box('y', 0, 0, 1, 6, 0, 5);
         b.box('m', 3, 0, 0, 3, 0, 5);
         for (int z = 1; z <= 4; z++) {
            for (int x : new int[]{1, 2, 4, 5}) {
               b.set('z', x, 1, z);
            }
         }

         b.set('F', 3, 1, 1).set('F', 3, 2, 1).set('F', 3, 3, 1).set('T', 3, 4, 1);
         b.set('_', 1, 1, 5).set('_', 2, 1, 5);
         add(b.build());
      }
      // ---- the inn / tavern: 11 x 7, two storeys with a hearth, bar counter and upstairs guest beds
      {
         Tpl.B b = cottage("inn", 11, 7, 7, 5, false);
         b.box('E', 1, 4, 2, 9, 4, 6);
         b.box('X', 1, 4, 1, 9, 4, 1);
         b.box('X', 1, 4, 7, 9, 4, 7);
         b.box('Z', 0, 4, 2, 0, 4, 6);
         b.box('Z', 10, 4, 2, 10, 4, 6);
         for (int y = 1; y <= 5; y++) {
            b.set('H', 8, y, 6);
         }

         for (int[] p : new int[][]{{2, 1}, {8, 1}, {0, 3}, {0, 5}, {10, 3}, {10, 5}, {3, 7}, {7, 7}}) {
            b.set('G', p[0], 2, p[1]);
            b.set('G', p[0], 6, p[1]);
         }

         b.set('_', 1, 1, 4).set('_', 2, 1, 4).set('_', 3, 1, 4);
         b.set('k', 1, 1, 6);
         b.set('x', 2, 1, 6);
         b.set('q', 9, 1, 6).set('q', 9, 2, 6);
         b.box('+', 4, 1, 3, 7, 1, 5);
         b.set(':', 5, 1, 4).set(':', 6, 1, 4);
         b.set('*', 5, 3, 6).set('*', 2, 3, 2).set('*', 8, 3, 2);
         bed(b, 1, 5, 5);
         bed(b, 4, 5, 5);
         bed(b, 6, 5, 5);
         b.set('x', 2, 5, 2);
         b.set('*', 5, 6, 6).set('*', 4, 6, 2);
         chimney(b, 9, 2, 13);
         veranda(b, 11, 3, true);
         add(b.build());
      }
      // ---- the bakery: 7 x 7, twin ovens, brick chimney, display counter and striped awning
      {
         Tpl.B b = cottage("bakery", 7, 7, 3, 3, false);
         window(b, 1, 1);
         window(b, 5, 1);
         window(b, 0, 4);
         window(b, 6, 4);
         b.set('u', 2, 1, 6).set('u', 4, 1, 6);
         for (int y = 1; y <= 9; y++) {
            b.set('K', 3, y, 6);
         }

         b.set('_', 1, 1, 3).set('_', 2, 1, 3);
         b.set('k', 5, 1, 3);
         b.set('x', 5, 1, 6);
         b.set('h', 5, 1, 2);
         b.set('+', 3, 1, 3).set(':', 3, 1, 4);
         b.set('*', 1, 2, 6).set('*', 5, 2, 3);
         for (int x = 1; x <= 5; x++) {
            b.set((x & 1) == 0 ? 'w' : 'W', x, 3, 0);
         }

         b.set('z', 0, 1, 0).set('z', 6, 1, 0);
         add(b.build());
      }
      // ---- the windmill: 7 x 7, two-storey grain mill with upper loft and sails on the front
      {
         Tpl.B b = cottage("windmill", 7, 7, 7, 3, false);
         b.box('E', 1, 4, 2, 5, 4, 6);
         b.box('X', 1, 4, 1, 5, 4, 1);
         b.box('X', 1, 4, 7, 5, 4, 7);
         b.box('Z', 0, 4, 2, 0, 4, 6);
         b.box('Z', 6, 4, 2, 6, 4, 6);
         for (int y = 1; y <= 5; y++) {
            b.set('H', 5, y, 6);
         }

         b.set('G', 0, 2, 4).set('G', 6, 2, 4).set('G', 3, 2, 7);
         b.set('G', 0, 6, 4).set('G', 6, 6, 4).set('G', 3, 6, 7);
         b.set('h', 1, 1, 2).set('h', 1, 2, 2).set('h', 2, 1, 2);
         b.set('k', 1, 1, 6);
         b.set('x', 3, 1, 6);
         b.set('x', 1, 5, 6);
         b.set('*', 2, 3, 6).set('*', 4, 3, 2).set('*', 2, 6, 6);
         // Windmill hub and sails on the front facade (z = 0)
         b.set('L', 3, 6, 1);
         for (int dy = -2; dy <= 2; dy++) {
            b.set('F', 3, 6 + dy, 0);
            if (dy != 0) {
               b.set('w', 2, 6 + dy, 0);
               b.set('W', 4, 6 + dy, 0);
            }
         }

         for (int dx = -2; dx <= 2; dx++) {
            b.set('F', 3 + dx, 6, 0);
         }

         add(b.build());
      }
      // ---- the gazebo: 7 x 7 town pavilion with corner pillars, benches and lantern
      {
         Tpl.B b = new Tpl.B("gazebo", 7, 8, 7);
         b.box('E', 1, 0, 1, 5, 0, 5);
         b.walls('C', 0, 0, 0, 6, 0, 6);
         b.set('m', 3, 0, 0).set('m', 3, 0, 6).set('m', 0, 0, 3).set('m', 6, 0, 3);
         for (int[] p : new int[][]{{1, 1}, {5, 1}, {1, 5}, {5, 5}}) {
            b.box('L', p[0], 1, p[1], p[0], 3, p[1]);
         }

         b.set('_', 2, 1, 1).set('_', 4, 1, 1).set('_', 2, 1, 5).set('_', 4, 1, 5);
         b.set('+', 2, 1, 2).set(':', 3, 1, 2).set('+', 4, 1, 2);
         b.set(':', 2, 1, 3).set('+', 3, 1, 3).set(':', 4, 1, 3);
         b.set('+', 2, 1, 4).set(':', 3, 1, 4).set('+', 4, 1, 4);
         b.set('$', 3, 3, 3);
         b.hip(4, 1, 5, 1, 5);
         add(b.build());
      }
      // ---- the archery range: 9 x 7, covered shooting stand and bullseye targets
      {
         Tpl.B b = new Tpl.B("archery", 9, 5, 7);
         b.box('m', 0, 0, 0, 8, 0, 6);
         b.box('E', 1, 0, 0, 7, 0, 2);
         for (int x : new int[]{1, 7}) {
            b.box('F', x, 1, 0, x, 3, 0);
            b.box('F', x, 1, 2, x, 3, 2);
         }

         b.box('_', 1, 4, 0, 7, 4, 2);
         for (int z = 2; z <= 6; z++) {
            b.set('F', 0, 1, z);
            b.set('F', 8, 1, z);
         }

         b.set('k', 2, 1, 1);
         b.set('x', 6, 1, 1);
         for (int tx : new int[]{2, 6}) {
            b.set('h', tx, 1, 6);
            b.set('w', tx, 2, 6);
            b.set('F', tx, 3, 6);
            b.set('T', tx, 4, 6);
         }

         add(b.build());
      }
   }

   static {
      houses(false);
      houses(true);
      shops();
      barns();
      yards();
      tall();
      small();
   }
}
