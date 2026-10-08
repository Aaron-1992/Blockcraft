package bc;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

/**
 * Update 3.19 / V4.0: Redstone mechanisms (Block of Redstone, Redstone Lamp, Redstone Torch + NOT-gate inverter,
 * Lever, Stone & Wooden Buttons, Stone & Wooden Pressure Plates, Redstone Wire power propagation with 3D dynamic
 * connections & 1-block step-up/down, Redstone Repeater, Piston & Sticky Piston, Note Block, Daylight Sensor, Observer,
 * Iron Trapdoor / Door / Gate / TNT activation) plus 1.12 polished/chiseled stones, Slime Block and Sea Lantern.
 * Block IDs 1900..2008, item ID 450 (Redstone Dust), atlas tiles 643..671.
 */
final class Redstone319 {
   static final int REDSTONE_BLOCK = 1900;
   static final int REDSTONE_LAMP = 1901;
   static final int REDSTONE_LAMP_LIT = 1902;
   static final int REDSTONE_TORCH = 1903;
   static final int LEVER_OFF = 1904;
   static final int LEVER_ON = 1905;
   static final int STONE_BUTTON = 1906;
   static final int STONE_BUTTON_ON = 1907;
   static final int WOOD_BUTTON = 1908;
   static final int WOOD_BUTTON_ON = 1909;
   static final int STONE_PLATE = 1910;
   static final int STONE_PLATE_ON = 1911;
   static final int WOOD_PLATE = 1912;
   static final int WOOD_PLATE_ON = 1913;
   static final int REDSTONE_TORCH_OFF = 1914;
   static final int REDSTONE_WIRE0 = 1920; // 1920..1935 = power 0..15

   static final int POLISHED_GRANITE = 1940;
   static final int POLISHED_DIORITE = 1941;
   static final int POLISHED_ANDESITE = 1942;
   static final int CHISELED_SANDSTONE = 1943;
   static final int SMOOTH_SANDSTONE = 1944;
   static final int CHISELED_RED_SANDSTONE = 1945;
   static final int SMOOTH_RED_SANDSTONE = 1946;
   static final int CHISELED_STONEBRICK = 1947;
   static final int MOSSY_STONEBRICK = 1948;
   static final int CRACKED_STONEBRICK = 1949;
   static final int SLIME_BLOCK = 1950;
   static final int SEA_LANTERN = 1951;

   // ---- V4.0 Redstone expansion (1960..2008) ----
   /** Redstone Repeater OFF (4 horizontal facings 0=N(-Z), 1=S(+Z), 2=E(+X), 3=W(-X)): 1960..1963. */
   static final int REPEATER_OFF = 1960;
   /** Redstone Repeater ON (4 horizontal facings 0..3): 1964..1967. */
   static final int REPEATER_ON = 1964;
   /** Piston retracted (6 facings 0=Down, 1=Up, 2=N, 3=S, 4=E, 5=W): 1968..1973. */
   static final int PISTON = 1968;
   /** Piston extended base (6 facings 0..5): 1974..1979. */
   static final int PISTON_EXT = 1974;
   /** Sticky Piston retracted (6 facings 0..5): 1980..1985. */
   static final int STICKY_PISTON = 1980;
   /** Sticky Piston extended base (6 facings 0..5): 1986..1991. */
   static final int STICKY_PISTON_EXT = 1986;
   /** Piston head (6 facings 0..5): 1992..1997. */
   static final int PISTON_HEAD = 1992;
   static final int NOTE_BLOCK = 1998;
   static final int DAYLIGHT_SENSOR = 1999;
   static final int DAYLIGHT_SENSOR_ON = 2000;
   /** Observer OFF (4 horizontal facings 0..3): 2001..2004. */
   static final int OBSERVER = 2001;
   /** Observer ON pulse (4 horizontal facings 0..3): 2005..2008. */
   static final int OBSERVER_ON = 2005;

   static final int ITEM_REDSTONE = 450;

   // Atlas tiles (643..671)
   static final int T_REDSTONE_BLOCK = 643;
   static final int T_LAMP_OFF = 644;
   static final int T_LAMP_ON = 645;
   static final int T_REDSTONE_TORCH = 646;
   static final int T_REDSTONE_WIRE = 647;
   static final int T_POL_GRANITE = 648;
   static final int T_POL_DIORITE = 649;
   static final int T_POL_ANDESITE = 650;
   static final int T_CHIS_SANDSTONE = 651;
   static final int T_SMOOTH_SANDSTONE = 652;
   static final int T_CHIS_RED_SANDSTONE = 653;
   static final int T_SMOOTH_RED_SANDSTONE = 654;
   static final int T_CHIS_STONEBRICK = 655;
   static final int T_MOSSY_STONEBRICK = 656;
   static final int T_CRACKED_STONEBRICK = 657;
   static final int T_SLIME = 658;
   static final int T_SEA_LANTERN = 659;
   static final int T_REDSTONE_DUST = 660;
   static final int T_REDSTONE_WIRE_LIT = 661;
   static final int T_REDSTONE_TORCH_OFF = 662;
   static final int T_REPEATER_OFF = 663;
   static final int T_REPEATER_ON = 664;
   static final int T_PISTON_SIDE = 665;
   static final int T_PISTON_TOP = 666;
   static final int T_PISTON_TOP_STICKY = 667;
   static final int T_PISTON_BOTTOM = 668;
   static final int T_DAYLIGHT_TOP = 669;
   static final int T_NOTE_BLOCK = 670;
   static final int T_OBSERVER_FRONT = 671;

   private static final int[] HDX = {0, 0, 1, -1};
   private static final int[] HDZ = {-1, 1, 0, 0};
   private static final int[] FDX = {0, 0, 0, 0, 1, -1};
   private static final int[] FDY = {-1, 1, 0, 0, 0, 0};
   private static final int[] FDZ = {0, 0, -1, 1, 0, 0};

   private static final Map<Long, Integer> buttonTimers = new HashMap<>();
   private static final java.util.Set<Long> redstoneOpened = java.util.concurrent.ConcurrentHashMap.newKeySet();
   private static boolean updating = false;

   private Redstone319() {
   }

   static void registerItems() {
      Items.defTile(ITEM_REDSTONE, "Redstone Dust", T_REDSTONE_DUST);
   }

   private static void cube(int id, String name, float hardness, int tile, int mineClass, boolean needTool) {
      Blocks.def(id, name, hardness, true, tile);
      Blocks.mineClass[id] = mineClass;
      Blocks.needTool[id] = needTool;
   }

   private static void modelBlock(int id, String name, float hardness, int tile, int dropId, double[] bounds, double[][] boxes) {
      Blocks.def(id, name, hardness, false, tile);
      Blocks.solid[id] = false;
      Blocks.opacity[id] = 0;
      Blocks.coll[id] = null;
      Blocks.box[id] = bounds;
      Blocks.drop[id] = dropId;
      Shapes.model(id, boxes);
   }

   static void register() {
      cube(REDSTONE_BLOCK, "Block of Redstone", 5.0F, T_REDSTONE_BLOCK, 1, true);
      cube(REDSTONE_LAMP, "Redstone Lamp", 0.3F, T_LAMP_OFF, 0, false);
      cube(REDSTONE_LAMP_LIT, "Redstone Lamp", 0.3F, T_LAMP_ON, 0, false);
      Blocks.emit[REDSTONE_LAMP_LIT] = 15;
      Blocks.drop[REDSTONE_LAMP_LIT] = REDSTONE_LAMP;

      int cobble = Blocks.tiles[Blocks.COBBLE][0];
      int stone = Blocks.tiles[Blocks.STONE][0];
      int plank = Blocks.tiles[Blocks.PLANKS][0];

      modelBlock(
         REDSTONE_TORCH,
         "Redstone Torch",
         0.0F,
         T_REDSTONE_TORCH,
         REDSTONE_TORCH,
         new double[]{0.375, 0.0, 0.375, 0.625, 0.625, 0.625},
         new double[][]{
            Shapes.tiles(Shapes.px(7, 0, 7, 9, 8, 9), plank, plank, plank),
            Shapes.tiles(Shapes.px(6.5, 7, 6.5, 9.5, 10.5, 9.5), T_REDSTONE_BLOCK, T_REDSTONE_BLOCK, T_REDSTONE_BLOCK)
         }
      );
      Blocks.emit[REDSTONE_TORCH] = 7;

      modelBlock(
         REDSTONE_TORCH_OFF,
         "Redstone Torch",
         0.0F,
         T_REDSTONE_TORCH_OFF,
         REDSTONE_TORCH,
         new double[]{0.375, 0.0, 0.375, 0.625, 0.625, 0.625},
         new double[][]{
            Shapes.tiles(Shapes.px(7, 0, 7, 9, 8, 9), plank, plank, plank),
            Shapes.tiles(Shapes.px(6.5, 7, 6.5, 9.5, 10.5, 9.5), T_REDSTONE_WIRE, T_REDSTONE_WIRE, T_REDSTONE_WIRE)
         }
      );
      Blocks.emit[REDSTONE_TORCH_OFF] = 0;

      modelBlock(
         LEVER_OFF,
         "Lever",
         0.5F,
         cobble,
         LEVER_OFF,
         new double[]{0.25, 0.0, 0.25, 0.75, 0.625, 0.75},
         new double[][]{
            Shapes.tiles(Shapes.px(5, 0, 4, 11, 3, 12), cobble, cobble, cobble),
            Shapes.tiles(Shapes.px(7, 3, 5, 9, 10, 7), plank, plank, plank)
         }
      );
      modelBlock(
         LEVER_ON,
         "Lever",
         0.5F,
         cobble,
         LEVER_OFF,
         new double[]{0.25, 0.0, 0.25, 0.75, 0.625, 0.75},
         new double[][]{
            Shapes.tiles(Shapes.px(5, 0, 4, 11, 3, 12), cobble, cobble, cobble),
            Shapes.tiles(Shapes.px(7, 3, 9, 9, 10, 11), plank, T_REDSTONE_BLOCK, plank)
         }
      );

      modelBlock(
         STONE_BUTTON,
         "Stone Button",
         0.5F,
         stone,
         STONE_BUTTON,
         new double[]{0.3125, 0.0, 0.375, 0.6875, 0.1875, 0.625},
         new double[][]{Shapes.tiles(Shapes.px(5, 0, 6, 11, 3, 10), stone, stone, stone)}
      );
      modelBlock(
         STONE_BUTTON_ON,
         "Stone Button",
         0.5F,
         stone,
         STONE_BUTTON,
         new double[]{0.3125, 0.0, 0.375, 0.6875, 0.125, 0.625},
         new double[][]{Shapes.tiles(Shapes.px(5, 0, 6, 11, 1.5, 10), stone, stone, stone)}
      );

      modelBlock(
         WOOD_BUTTON,
         "Wooden Button",
         0.5F,
         plank,
         WOOD_BUTTON,
         new double[]{0.3125, 0.0, 0.375, 0.6875, 0.1875, 0.625},
         new double[][]{Shapes.tiles(Shapes.px(5, 0, 6, 11, 3, 10), plank, plank, plank)}
      );
      modelBlock(
         WOOD_BUTTON_ON,
         "Wooden Button",
         0.5F,
         plank,
         WOOD_BUTTON,
         new double[]{0.3125, 0.0, 0.375, 0.6875, 0.125, 0.625},
         new double[][]{Shapes.tiles(Shapes.px(5, 0, 6, 11, 1.5, 10), plank, plank, plank)}
      );

      modelBlock(
         STONE_PLATE,
         "Stone Pressure Plate",
         0.5F,
         stone,
         STONE_PLATE,
         new double[]{0.0625, 0.0, 0.0625, 0.9375, 0.0625, 0.9375},
         new double[][]{Shapes.tiles(Shapes.px(1, 0, 1, 15, 1, 15), stone, stone, stone)}
      );
      modelBlock(
         STONE_PLATE_ON,
         "Stone Pressure Plate",
         0.5F,
         stone,
         STONE_PLATE,
         new double[]{0.0625, 0.0, 0.0625, 0.9375, 0.03125, 0.9375},
         new double[][]{Shapes.tiles(Shapes.px(1, 0, 1, 15, 0.5, 15), stone, stone, stone)}
      );

      modelBlock(
         WOOD_PLATE,
         "Wooden Pressure Plate",
         0.5F,
         plank,
         WOOD_PLATE,
         new double[]{0.0625, 0.0, 0.0625, 0.9375, 0.0625, 0.9375},
         new double[][]{Shapes.tiles(Shapes.px(1, 0, 1, 15, 1, 15), plank, plank, plank)}
      );
      modelBlock(
         WOOD_PLATE_ON,
         "Wooden Pressure Plate",
         0.5F,
         plank,
         WOOD_PLATE,
         new double[]{0.0625, 0.0, 0.0625, 0.9375, 0.03125, 0.9375},
         new double[][]{Shapes.tiles(Shapes.px(1, 0, 1, 15, 0.5, 15), plank, plank, plank)}
      );

      // Redstone Wire 0..15: emit = 0 so wire power changes never trigger 31x31x31 block-light BFS!
      // Meshed dynamically via wireBoxes(nbr, id) in Shapes.mesh.
      for (int p = 0; p < 16; p++) {
         int id = REDSTONE_WIRE0 + p;
         int tile = p == 0 ? T_REDSTONE_WIRE : T_REDSTONE_WIRE_LIT;
         int rgb = wireTint(p);
         modelBlock(
            id,
            "Redstone Wire",
            0.0F,
            tile,
            ITEM_REDSTONE,
            new double[]{0.0, 0.0, 0.0, 1.0, 0.0625, 1.0},
            new double[][]{
               Shapes.tint(Shapes.tiles(Shapes.px(5, 0, 5, 11, 0.5, 11), tile, tile, tile), rgb),
               Shapes.tint(Shapes.tiles(Shapes.px(6, 0, 0, 10, 0.5, 5), tile, tile, tile), rgb),
               Shapes.tint(Shapes.tiles(Shapes.px(6, 0, 11, 10, 0.5, 16), tile, tile, tile), rgb),
               Shapes.tint(Shapes.tiles(Shapes.px(0, 0, 6, 5, 0.5, 10), tile, tile, tile), rgb),
               Shapes.tint(Shapes.tiles(Shapes.px(11, 0, 6, 16, 0.5, 10), tile, tile, tile), rgb)
            }
         );
         Blocks.emit[id] = 0;
      }

      // Redstone Repeater (OFF 1960..1963, ON 1964..1967)
      for (int f = 0; f < 4; f++) {
         int turns = Shapes.turns(0, f);
         int off = REPEATER_OFF + f;
         int on = REPEATER_ON + f;
         double[][] boxesOff = Shapes.turnAll(
            new double[][]{
               Shapes.tiles(Shapes.px(0, 0, 0, 16, 2, 16), T_SMOOTH_STONE(), T_REPEATER_OFF, T_SMOOTH_STONE()),
               Shapes.tiles(Shapes.px(7, 2, 2, 9, 7, 4), T_REDSTONE_WIRE, T_REDSTONE_WIRE, T_REDSTONE_WIRE),
               Shapes.tiles(Shapes.px(7, 2, 10, 9, 7, 12), T_REDSTONE_WIRE, T_REDSTONE_WIRE, T_REDSTONE_WIRE)
            },
            turns
         );
         modelBlock(off, "Redstone Repeater", 0.0F, T_REPEATER_OFF, REPEATER_OFF, new double[]{0.0, 0.0, 0.0, 1.0, 0.125, 1.0}, boxesOff);
         Blocks.solid[off] = true;
         Blocks.coll[off] = new double[]{0.0, 0.0, 0.0, 1.0, 0.125, 1.0};

         double[][] boxesOn = Shapes.turnAll(
            new double[][]{
               Shapes.tiles(Shapes.px(0, 0, 0, 16, 2, 16), T_SMOOTH_STONE(), T_REPEATER_ON, T_SMOOTH_STONE()),
               Shapes.tiles(Shapes.px(7, 2, 2, 9, 7, 4), T_REDSTONE_BLOCK, T_REDSTONE_BLOCK, T_REDSTONE_BLOCK),
               Shapes.tiles(Shapes.px(7, 2, 10, 9, 7, 12), T_REDSTONE_BLOCK, T_REDSTONE_BLOCK, T_REDSTONE_BLOCK)
            },
            turns
         );
         modelBlock(on, "Redstone Repeater", 0.0F, T_REPEATER_ON, REPEATER_OFF, new double[]{0.0, 0.0, 0.0, 1.0, 0.125, 1.0}, boxesOn);
         Blocks.solid[on] = true;
         Blocks.coll[on] = new double[]{0.0, 0.0, 0.0, 1.0, 0.125, 1.0};
         Blocks.emit[on] = 5;
      }

      // Pistons & Sticky Pistons (retracted 1968..1973 / 1980..1985, extended 1974..1979 / 1986..1991, head 1992..1997)
      for (int f = 0; f < 6; f++) {
         registerPistonVariant(PISTON + f, "Piston", PISTON, f, false, false);
         registerPistonVariant(PISTON_EXT + f, "Piston", PISTON, f, false, true);
         registerPistonVariant(STICKY_PISTON + f, "Sticky Piston", STICKY_PISTON, f, true, false);
         registerPistonVariant(STICKY_PISTON_EXT + f, "Sticky Piston", STICKY_PISTON, f, true, true);
         registerPistonHead(PISTON_HEAD + f, f);
      }

      // Note Block
      cube(NOTE_BLOCK, "Note Block", 0.8F, T_NOTE_BLOCK, 2, false);

      // Daylight Sensor
      double[] dlBounds = {0.0, 0.0, 0.0, 1.0, 0.375, 1.0};
      double[][] dlBoxes = {Shapes.tiles(Shapes.px(0, 0, 0, 16, 6, 16), plank, T_DAYLIGHT_TOP, plank)};
      modelBlock(DAYLIGHT_SENSOR, "Daylight Sensor", 0.2F, T_DAYLIGHT_TOP, DAYLIGHT_SENSOR, dlBounds, dlBoxes);
      Blocks.solid[DAYLIGHT_SENSOR] = true;
      Blocks.coll[DAYLIGHT_SENSOR] = dlBounds;

      modelBlock(DAYLIGHT_SENSOR_ON, "Daylight Sensor", 0.2F, T_DAYLIGHT_TOP, DAYLIGHT_SENSOR, dlBounds, dlBoxes);
      Blocks.solid[DAYLIGHT_SENSOR_ON] = true;
      Blocks.coll[DAYLIGHT_SENSOR_ON] = dlBounds;
      Blocks.emit[DAYLIGHT_SENSOR_ON] = 4;

      // Observer (OFF 2001..2004, ON 2005..2008)
      for (int f = 0; f < 4; f++) {
         int obs = OBSERVER + f;
         int obsOn = OBSERVER_ON + f;
         cube(obs, "Observer", 3.0F, T_OBSERVER_FRONT, 1, true);
         Blocks.faces(obs, T_PISTON_BOTTOM, T_PISTON_BOTTOM, T_OBSERVER_FRONT);
         Blocks.drop[obs] = OBSERVER;

         cube(obsOn, "Observer", 3.0F, T_OBSERVER_FRONT, 1, true);
         Blocks.faces(obsOn, T_PISTON_BOTTOM, T_PISTON_BOTTOM, T_OBSERVER_FRONT);
         Blocks.drop[obsOn] = OBSERVER;
         Blocks.emit[obsOn] = 6;
      }

      // Missing 1.12 decorative & building blocks
      cube(POLISHED_GRANITE, "Polished Granite", 1.5F, T_POL_GRANITE, 1, true);
      cube(POLISHED_DIORITE, "Polished Diorite", 1.5F, T_POL_DIORITE, 1, true);
      cube(POLISHED_ANDESITE, "Polished Andesite", 1.5F, T_POL_ANDESITE, 1, true);
      cube(CHISELED_SANDSTONE, "Chiseled Sandstone", 0.8F, T_CHIS_SANDSTONE, 1, true);
      Blocks.faces(CHISELED_SANDSTONE, Tex.SANDSTONE_TOP, Tex.SANDSTONE_TOP, T_CHIS_SANDSTONE);
      cube(SMOOTH_SANDSTONE, "Smooth Sandstone", 0.8F, T_SMOOTH_SANDSTONE, 1, true);
      Blocks.faces(SMOOTH_SANDSTONE, Tex.SANDSTONE_TOP, Tex.SANDSTONE_TOP, T_SMOOTH_SANDSTONE);
      cube(CHISELED_RED_SANDSTONE, "Chiseled Red Sandstone", 0.8F, T_CHIS_RED_SANDSTONE, 1, true);
      Blocks.faces(CHISELED_RED_SANDSTONE, TexArt313.RS_TOP, TexArt313.RS_TOP, T_CHIS_RED_SANDSTONE);
      cube(SMOOTH_RED_SANDSTONE, "Smooth Red Sandstone", 0.8F, T_SMOOTH_RED_SANDSTONE, 1, true);
      Blocks.faces(SMOOTH_RED_SANDSTONE, TexArt313.RS_TOP, TexArt313.RS_TOP, T_SMOOTH_RED_SANDSTONE);
      cube(CHISELED_STONEBRICK, "Chiseled Stone Bricks", 1.5F, T_CHIS_STONEBRICK, 1, true);
      cube(MOSSY_STONEBRICK, "Mossy Stone Bricks", 1.5F, T_MOSSY_STONEBRICK, 1, true);
      cube(CRACKED_STONEBRICK, "Cracked Stone Bricks", 1.5F, T_CRACKED_STONEBRICK, 1, true);
      cube(SLIME_BLOCK, "Slime Block", 0.0F, T_SLIME, 0, false);
      cube(SEA_LANTERN, "Sea Lantern", 0.3F, T_SEA_LANTERN, 0, false);
      Blocks.emit[SEA_LANTERN] = 15;
   }

   private static int T_SMOOTH_STONE() {
      return Blocks.tiles[Blocks.STONE][0];
   }

   private static void registerPistonVariant(int id, String name, int dropId, int f, boolean sticky, boolean ext) {
      int platen = sticky ? T_PISTON_TOP_STICKY : T_PISTON_TOP;
      if (!ext) {
         cube(id, name, 0.5F, T_PISTON_SIDE, 1, false);
         Blocks.drop[id] = dropId;
         Blocks.tiles[id][0] = f == 1 ? platen : (f == 0 ? T_PISTON_BOTTOM : T_PISTON_SIDE);
         Blocks.tiles[id][1] = f == 0 ? platen : (f == 1 ? T_PISTON_BOTTOM : T_PISTON_SIDE);
         Blocks.tiles[id][2] = f == 2 ? platen : (f == 3 ? T_PISTON_BOTTOM : T_PISTON_SIDE);
         Blocks.tiles[id][3] = f == 3 ? platen : (f == 2 ? T_PISTON_BOTTOM : T_PISTON_SIDE);
         Blocks.tiles[id][4] = f == 4 ? platen : (f == 5 ? T_PISTON_BOTTOM : T_PISTON_SIDE);
         Blocks.tiles[id][5] = f == 5 ? platen : (f == 4 ? T_PISTON_BOTTOM : T_PISTON_SIDE);
      } else {
         double x0 = f == 5 ? 4 : 0;
         double x1 = f == 4 ? 12 : 16;
         double y0 = f == 0 ? 4 : 0;
         double y1 = f == 1 ? 12 : 16;
         double z0 = f == 2 ? 4 : 0;
         double z1 = f == 3 ? 12 : 16;
         double[] bounds = {x0 / 16.0, y0 / 16.0, z0 / 16.0, x1 / 16.0, y1 / 16.0, z1 / 16.0};
         int topT = f == 1 || f == 0 ? T_PISTON_BOTTOM : T_PISTON_SIDE;
         double[][] boxes = {Shapes.tiles(Shapes.px(x0, y0, z0, x1, y1, z1), T_PISTON_SIDE, topT, T_PISTON_BOTTOM)};
         modelBlock(id, name, 0.5F, T_PISTON_SIDE, dropId, bounds, boxes);
         Blocks.solid[id] = true;
         Blocks.coll[id] = bounds;
      }
   }

   private static void registerPistonHead(int id, int f) {
      double x0 = f == 4 ? 12 : 0;
      double x1 = f == 5 ? 4 : 16;
      double y0 = f == 1 ? 12 : 0;
      double y1 = f == 0 ? 4 : 16;
      double z0 = f == 3 ? 12 : 0;
      double z1 = f == 2 ? 4 : 16;
      double sx0 = f >= 4 ? (f == 4 ? 0 : 4) : 6;
      double sx1 = f >= 4 ? (f == 4 ? 12 : 16) : 10;
      double sy0 = f <= 1 ? (f == 1 ? 0 : 4) : 6;
      double sy1 = f <= 1 ? (f == 1 ? 12 : 16) : 10;
      double sz0 = (f == 2 || f == 3) ? (f == 3 ? 0 : 4) : 6;
      double sz1 = (f == 2 || f == 3) ? (f == 3 ? 12 : 16) : 10;
      double[][] boxes = {
         Shapes.tiles(Shapes.px(x0, y0, z0, x1, y1, z1), T_PISTON_SIDE, T_PISTON_TOP, T_PISTON_TOP),
         Shapes.tiles(Shapes.px(sx0, sy0, sz0, sx1, sy1, sz1), T_PISTON_SIDE, T_PISTON_TOP, T_PISTON_TOP)
      };
      double[] full = {0.0, 0.0, 0.0, 1.0, 1.0, 1.0};
      modelBlock(id, "Piston Head", 0.5F, T_PISTON_TOP, 0, full, boxes);
      Blocks.solid[id] = true;
      Blocks.coll[id] = full;
   }

   static int wireTint(int p) {
      if (p <= 0) {
         return 0x62100C;
      }
      int r = Math.min(255, 135 + p * 8);
      int g = Math.min(255, 12 + p * 3);
      int b = Math.min(255, 8 + p);
      return r << 16 | g << 8 | b;
   }

   /** Dynamic non-overlapping 3D boxes for Redstone Wire at power level {@code id - REDSTONE_WIRE0}. */
   static double[][] wireBoxes(Shapes.Nbr n, int id) {
      int p = Math.max(0, Math.min(15, id - REDSTONE_WIRE0));
      int tile = p == 0 ? T_REDSTONE_WIRE : T_REDSTONE_WIRE_LIT;
      int rgb = wireTint(p);

      boolean upClear = !Blocks.opaque[n.at(0, 1, 0)];
      boolean upN = upClear && isWire(n.at(0, 1, -1));
      boolean upS = upClear && isWire(n.at(0, 1, 1));
      boolean upE = upClear && isWire(n.at(1, 1, 0));
      boolean upW = upClear && isWire(n.at(-1, 1, 0));

      boolean cn = connectsWire(n.at(0, 0, -1)) || upN || (!Blocks.opaque[n.at(0, 0, -1)] && isWire(n.at(0, -1, -1)));
      boolean cs = connectsWire(n.at(0, 0, 1)) || upS || (!Blocks.opaque[n.at(0, 0, 1)] && isWire(n.at(0, -1, 1)));
      boolean ce = connectsWire(n.at(1, 0, 0)) || upE || (!Blocks.opaque[n.at(1, 0, 0)] && isWire(n.at(1, -1, 0)));
      boolean cw = connectsWire(n.at(-1, 0, 0)) || upW || (!Blocks.opaque[n.at(-1, 0, 0)] && isWire(n.at(-1, -1, 0)));

      ArrayList<double[]> out = new ArrayList<>(6);
      boolean straightNS = (cn || cs) && !ce && !cw;
      boolean straightEW = (ce || cw) && !cn && !cs;
      if (straightNS) {
         out.add(Shapes.tint(Shapes.tiles(Shapes.px(6, 0, cn ? 0 : 5, 10, 0.75, cs ? 16 : 11), tile, tile, tile), rgb));
      } else if (straightEW) {
         out.add(Shapes.tint(Shapes.tiles(Shapes.px(cw ? 0 : 5, 0, 6, ce ? 16 : 11, 0.75, 10), tile, tile, tile), rgb));
      } else {
         // Central junction hub + non-overlapping arms
         out.add(Shapes.tint(Shapes.tiles(Shapes.px(5, 0, 5, 11, 0.75, 11), tile, tile, tile), rgb));
         if (cn) {
            out.add(Shapes.tint(Shapes.tiles(Shapes.px(6, 0, 0, 10, 0.75, 5), tile, tile, tile), rgb));
         }
         if (cs) {
            out.add(Shapes.tint(Shapes.tiles(Shapes.px(6, 0, 11, 10, 0.75, 16), tile, tile, tile), rgb));
         }
         if (cw) {
            out.add(Shapes.tint(Shapes.tiles(Shapes.px(0, 0, 6, 5, 0.75, 10), tile, tile, tile), rgb));
         }
         if (ce) {
            out.add(Shapes.tint(Shapes.tiles(Shapes.px(11, 0, 6, 16, 0.75, 10), tile, tile, tile), rgb));
         }
      }

      // Vertical step-up strips when wire climbs 1 block up a ledge
      if (upN) {
         out.add(Shapes.tint(Shapes.tiles(Shapes.px(6, 0.75, 0, 10, 16, 0.6), tile, tile, tile), rgb));
      }
      if (upS) {
         out.add(Shapes.tint(Shapes.tiles(Shapes.px(6, 0.75, 15.4, 10, 16, 16), tile, tile, tile), rgb));
      }
      if (upW) {
         out.add(Shapes.tint(Shapes.tiles(Shapes.px(0, 0.75, 6, 0.6, 16, 10), tile, tile, tile), rgb));
      }
      if (upE) {
         out.add(Shapes.tint(Shapes.tiles(Shapes.px(15.4, 0.75, 6, 16, 16, 10), tile, tile, tile), rgb));
      }

      return out.toArray(new double[0][]);
   }

   static boolean connectsWire(int id) {
      return isWire(id)
         || sourcePower(id) > 0
         || id == REDSTONE_TORCH_OFF
         || id == LEVER_OFF
         || id == STONE_BUTTON
         || id == WOOD_BUTTON
         || id == STONE_PLATE
         || id == WOOD_PLATE
         || id == REDSTONE_LAMP
         || id == REDSTONE_LAMP_LIT
         || isRepeater(id)
         || isPistonBase(id)
         || id == NOTE_BLOCK
         || id == DAYLIGHT_SENSOR
         || isObserver(id)
         || Blocks.isDoor(id)
         || Shapes.isTrap(id)
         || Shapes.isGate(id)
         || id == Blocks.TNT;
   }

   static void addToCreative(ArrayList<Integer> list) {
      list.add(ITEM_REDSTONE);
      list.add(REDSTONE_TORCH);
      list.add(REPEATER_OFF);
      list.add(PISTON);
      list.add(STICKY_PISTON);
      list.add(OBSERVER);
      list.add(DAYLIGHT_SENSOR);
      list.add(NOTE_BLOCK);
      list.add(REDSTONE_BLOCK);
      list.add(REDSTONE_LAMP);
      list.add(LEVER_OFF);
      list.add(STONE_BUTTON);
      list.add(WOOD_BUTTON);
      list.add(STONE_PLATE);
      list.add(WOOD_PLATE);
      list.add(POLISHED_GRANITE);
      list.add(POLISHED_DIORITE);
      list.add(POLISHED_ANDESITE);
      list.add(CHISELED_SANDSTONE);
      list.add(SMOOTH_SANDSTONE);
      list.add(CHISELED_RED_SANDSTONE);
      list.add(SMOOTH_RED_SANDSTONE);
      list.add(CHISELED_STONEBRICK);
      list.add(MOSSY_STONEBRICK);
      list.add(CRACKED_STONEBRICK);
      list.add(SLIME_BLOCK);
      list.add(SEA_LANTERN);
   }

   static boolean isRedstoneUtility(int id) {
      return id == ITEM_REDSTONE
         || id == REDSTONE_BLOCK
         || id == REDSTONE_LAMP
         || id == REDSTONE_TORCH
         || id == LEVER_OFF
         || id == STONE_BUTTON
         || id == WOOD_BUTTON
         || id == STONE_PLATE
         || id == WOOD_PLATE
         || id == REPEATER_OFF
         || id == PISTON
         || id == STICKY_PISTON
         || id == OBSERVER
         || id == DAYLIGHT_SENSOR
         || id == NOTE_BLOCK;
   }

   static void recipes() {
      Recipes.shaped(REDSTONE_BLOCK, 1, new String[]{"RRR", "RRR", "RRR"}, 'R', ITEM_REDSTONE);
      Recipes.shapeless(ITEM_REDSTONE, 9, REDSTONE_BLOCK);
      Recipes.shaped(REDSTONE_LAMP, 1, new String[]{" R ", "RGR", " R "}, 'R', ITEM_REDSTONE, 'G', Blocks.GLOWSTONE);
      Recipes.shaped(REDSTONE_TORCH, 1, new String[]{"R", "S"}, 'R', ITEM_REDSTONE, 'S', Items.STICK);
      Recipes.shaped(LEVER_OFF, 1, new String[]{"S", "C"}, 'S', Items.STICK, 'C', Blocks.COBBLE);
      Recipes.shapeless(STONE_BUTTON, 1, Blocks.STONE);
      Recipes.shapeless(WOOD_BUTTON, 1, Blocks.PLANKS);
      Recipes.shaped(STONE_PLATE, 1, new String[]{"SS"}, 'S', Blocks.STONE);
      Recipes.shaped(WOOD_PLATE, 1, new String[]{"PP"}, 'P', Blocks.PLANKS);
      Recipes.shaped(REPEATER_OFF, 1, new String[]{"TRT", "SSS"}, 'T', REDSTONE_TORCH, 'R', ITEM_REDSTONE, 'S', Blocks.STONE);
      Recipes.shaped(PISTON, 1, new String[]{"PPP", "CIC", "CRC"}, 'P', Blocks.PLANKS, 'C', Blocks.COBBLE, 'I', Items.IRON_INGOT, 'R', ITEM_REDSTONE);
      Recipes.shaped(STICKY_PISTON, 1, new String[]{"S", "P"}, 'S', SLIME_BLOCK, 'P', PISTON);
      Recipes.shaped(NOTE_BLOCK, 1, new String[]{"PPP", "PRP", "PPP"}, 'P', Blocks.PLANKS, 'R', ITEM_REDSTONE);
      Recipes.shaped(DAYLIGHT_SENSOR, 1, new String[]{"GGG", "QQQ", "PPP"}, 'G', Blocks.GLASS, 'Q', Items.QUARTZ, 'P', Blocks.PLANKS);
      Recipes.shaped(OBSERVER, 1, new String[]{"CCC", "RRQ", "CCC"}, 'C', Blocks.COBBLE, 'R', ITEM_REDSTONE, 'Q', Items.QUARTZ);
      Recipes.shaped(POLISHED_GRANITE, 4, new String[]{"GG", "GG"}, 'G', Blocks313.GRANITE);
      Recipes.shaped(POLISHED_DIORITE, 4, new String[]{"DD", "DD"}, 'D', Blocks313.DIORITE);
      Recipes.shaped(POLISHED_ANDESITE, 4, new String[]{"AA", "AA"}, 'A', Blocks313.ANDESITE);
      Recipes.shaped(SMOOTH_SANDSTONE, 4, new String[]{"SS", "SS"}, 'S', Blocks.SANDSTONE);
      Recipes.shaped(CHISELED_SANDSTONE, 1, new String[]{"S", "S"}, 'S', 117);
      Recipes.shaped(SMOOTH_RED_SANDSTONE, 4, new String[]{"SS", "SS"}, 'S', Blocks313.RED_SANDSTONE);
      Recipes.shaped(CHISELED_STONEBRICK, 1, new String[]{"S", "S"}, 'S', 114);
      Recipes.shapeless(MOSSY_STONEBRICK, 1, Blocks.STONEBRICK, Blocks313.VINE0 + 1);
      Recipes.smelt.put(Blocks.STONEBRICK, new Stack(CRACKED_STONEBRICK, 1));
      Recipes.smelt.put(23, new Stack(ITEM_REDSTONE, 4));
      Recipes.shaped(SEA_LANTERN, 1, new String[]{"QGQ", "GGG", "QGQ"}, 'Q', Items.QUARTZ, 'G', Blocks.GLOWSTONE);
   }

   // ------------------------------------------------------------------ Redstone logic
   static boolean isWire(int id) {
      return id >= REDSTONE_WIRE0 && id < REDSTONE_WIRE0 + 16;
   }

   static boolean isRepeater(int id) {
      return id >= REPEATER_OFF && id < REPEATER_ON + 4;
   }

   static boolean isRepeaterOn(int id) {
      return id >= REPEATER_ON && id < REPEATER_ON + 4;
   }

   static int repeaterFacing(int id) {
      return (id - REPEATER_OFF) & 3;
   }

   static boolean isPistonBase(int id) {
      return id >= PISTON && id < STICKY_PISTON_EXT + 6;
   }

   static boolean isPistonExtended(int id) {
      return (id >= PISTON_EXT && id < PISTON_EXT + 6) || (id >= STICKY_PISTON_EXT && id < STICKY_PISTON_EXT + 6);
   }

   static boolean isStickyPiston(int id) {
      return id >= STICKY_PISTON && id < STICKY_PISTON_EXT + 6;
   }

   static int pistonFacing(int id) {
      if (id >= PISTON_HEAD && id < PISTON_HEAD + 6) {
         return id - PISTON_HEAD;
      }
      return (id - PISTON) % 6;
   }

   static boolean isObserver(int id) {
      return id >= OBSERVER && id < OBSERVER_ON + 4;
   }

   static int observerFacing(int id) {
      return (id - OBSERVER) & 3;
   }

   static boolean isFloorPart(int id) {
      return id == REDSTONE_TORCH || id == REDSTONE_TORCH_OFF || id >= LEVER_OFF && id <= WOOD_PLATE_ON || isWire(id) || isRepeater(id);
   }

   static boolean placesOriented(int id) {
      return id == REPEATER_OFF || id == PISTON || id == STICKY_PISTON || id == OBSERVER;
   }

   static int orientForPlace(Game g, int id, int x, int y, int z) {
      int hf = Shapes.playerFacing(g.player);
      if (id == REPEATER_OFF) {
         return REPEATER_OFF + hf;
      }
      if (id == OBSERVER) {
         return OBSERVER + hf;
      }
      if (id == PISTON || id == STICKY_PISTON) {
         double dx = g.player.x - (x + 0.5);
         double dz = g.player.z - (z + 0.5);
         if (Math.abs(dx) < 2.0 && Math.abs(dz) < 2.0) {
            double dy = (g.player.y + g.player.eye) - (y + 0.5);
            if (dy > 1.8) {
               return id + 1; // Up (+Y)
            }
            if (dy < -0.5) {
               return id; // Down (-Y)
            }
         }
         int opp = hf ^ 1;
         return id + 2 + opp;
      }
      return id;
   }

   static int pickItem(int id) {
      if (isWire(id)) {
         return ITEM_REDSTONE;
      }
      if (id == REDSTONE_TORCH_OFF) {
         return REDSTONE_TORCH;
      }
      if (isRepeater(id)) {
         return REPEATER_OFF;
      }
      if (id >= PISTON && id < PISTON_EXT + 6 || (id >= PISTON_HEAD && id < PISTON_HEAD + 6)) {
         return PISTON;
      }
      if (id >= STICKY_PISTON && id < STICKY_PISTON_EXT + 6) {
         return STICKY_PISTON;
      }
      if (id == DAYLIGHT_SENSOR_ON) {
         return DAYLIGHT_SENSOR;
      }
      if (isObserver(id)) {
         return OBSERVER;
      }
      return Blocks.drop[id] != 0 ? Blocks.drop[id] : id;
   }

   /** Direct power (0..15) emitted by a non-directional source block itself (excluding wire). */
   static int sourcePower(int id) {
      return switch (id) {
         case REDSTONE_BLOCK, REDSTONE_TORCH, LEVER_ON, STONE_BUTTON_ON, WOOD_BUTTON_ON, STONE_PLATE_ON, WOOD_PLATE_ON, DAYLIGHT_SENSOR_ON -> 15;
         default -> 0;
      };
   }

   /** Power (0..15) at (x, y, z), including wire signal strength. */
   static int powerAt(World w, int x, int y, int z) {
      int id = w.getBlock(x, y, z);
      if (isWire(id)) {
         return id - REDSTONE_WIRE0;
      }
      if (isRepeaterOn(id) || (id >= OBSERVER_ON && id < OBSERVER_ON + 4)) {
         return 15;
      }
      return sourcePower(id);
   }

   /** True if any of the 6 neighbours of (x, y, z) provides Redstone power > 0 into (x, y, z). */
   static boolean isPowered(World w, int x, int y, int z) {
      int[][] dirs = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
      for (int[] d : dirs) {
         int nx = x + d[0];
         int ny = y + d[1];
         int nz = z + d[2];
         int nb = w.getBlock(nx, ny, nz);
         if (sourcePower(nb) > 0 || (isWire(nb) && nb > REDSTONE_WIRE0)) {
            return true;
         }
         if (isRepeaterOn(nb)) {
            int rf = repeaterFacing(nb);
            if (nx + HDX[rf] == x && ny == y && nz + HDZ[rf] == z) {
               return true;
            }
         }
         if (nb >= OBSERVER_ON && nb < OBSERVER_ON + 4) {
            int of = observerFacing(nb);
            if (nx - HDX[of] == x && ny == y && nz - HDZ[of] == z) {
               return true;
            }
         }
      }
      return false;
   }

   static boolean use(Game g, int x, int y, int z, int id) {
      if (id == LEVER_OFF || id == LEVER_ON) {
         int next = id == LEVER_OFF ? LEVER_ON : LEVER_OFF;
         g.world.setBlock(x, y, z, next);
         propagate(g.world, x, y, z);
         g.meshNow(x, y, z);
         return true;
      }

      if (id == STONE_BUTTON || id == WOOD_BUTTON) {
         int next = id == STONE_BUTTON ? STONE_BUTTON_ON : WOOD_BUTTON_ON;
         g.world.setBlock(x, y, z, next);
         buttonTimers.put(pack(x, y, z), id == STONE_BUTTON ? 20 : 30);
         propagate(g.world, x, y, z);
         g.meshNow(x, y, z);
         return true;
      }

      if (id == NOTE_BLOCK) {
         playNote(g, x, y, z);
         return true;
      }

      if (id == DAYLIGHT_SENSOR || id == DAYLIGHT_SENSOR_ON) {
         int next = id == DAYLIGHT_SENSOR ? DAYLIGHT_SENSOR_ON : DAYLIGHT_SENSOR;
         g.world.setBlock(x, y, z, next);
         propagate(g.world, x, y, z);
         g.meshNow(x, y, z);
         return true;
      }

      return false;
   }

   private static void playNote(Game g, int x, int y, int z) {
      if (g != null && g.particles != null) {
         g.spawnBreakParticles(x, y + 1, z, REDSTONE_TORCH, 4);
      }
   }

   static boolean placeWire(Game g, Stack s) {
      if (s == null || s.id != ITEM_REDSTONE || g.target == null || g.player.adventure) {
         return false;
      }

      int x = g.target.x;
      int y = g.target.y;
      int z = g.target.z;
      if (!Blocks.replaceable[g.world.getBlock(x, y, z)]) {
         x += g.target.nx;
         y += g.target.ny;
         z += g.target.nz;
      }

      if (y < 1 || y > 254 || !Blocks.replaceable[g.world.getBlock(x, y, z)] || !Blocks.solid[g.world.getBlock(x, y - 1, z)]) {
         return false;
      }

      g.world.setBlock(x, y, z, REDSTONE_WIRE0);
      propagate(g.world, x, y, z);
      g.meshNow(x, y, z);
      // Re-mesh adjacent blocks so dynamic wire arms connect immediately
      for (int d = 0; d < 4; d++) {
         g.meshNow(x + HDX[d], y, z + HDZ[d]);
         if (y < 254) {
            g.meshNow(x + HDX[d], y + 1, z + HDZ[d]);
         }
         if (y > 1) {
            g.meshNow(x + HDX[d], y - 1, z + HDZ[d]);
         }
      }
      if (!g.player.creative) {
         s.n--;
         if (s.n <= 0) {
            g.inv[g.sel] = null;
         }
      }

      return true;
   }

   /** Called from World.notifyAround when block (x, y, z) changes so any Observer watching it emits a pulse. */
   static void onBlockChanged(World w, int x, int y, int z) {
      if (w == null || updating || y < 1 || y > 254) {
         return;
      }
      for (int f = 0; f < 4; f++) {
         int ox = x - HDX[f];
         int oz = z - HDZ[f];
         int ob = w.getBlock(ox, y, oz);
         if (ob == OBSERVER + f) {
            w.setBlock(ox, y, oz, OBSERVER_ON + f);
            buttonTimers.put(pack(ox, y, oz), 4);
            if (w.game != null) {
               w.game.meshNow(ox, y, oz);
            }
            propagate(w, ox - HDX[f], y, oz - HDZ[f]);
         }
      }
   }

   static boolean handlesUpdate(int id) {
      if (updating) {
         return false;
      }
      return isFloorPart(id)
         || id == REDSTONE_BLOCK
         || id == REDSTONE_LAMP
         || id == REDSTONE_LAMP_LIT
         || isPistonBase(id)
         || (id >= PISTON_HEAD && id < PISTON_HEAD + 6)
         || id == NOTE_BLOCK
         || isObserver(id)
         || Shapes.isTrap(id)
         || Shapes.isGate(id)
         || id == Blocks.TNT;
   }

   static void update(World w, int x, int y, int z, int id) {
      if (isFloorPart(id) && !Blocks.solid[w.getBlock(x, y - 1, z)]) {
         int drop = Blocks.drop[id];
         w.setBlock(x, y, z, 0);
         if (drop > 0) {
            w.dropItem(x + 0.5, y + 0.3, z + 0.5, drop, 1);
         }

         propagate(w, x, y, z);
         return;
      }
      if (id >= PISTON_HEAD && id < PISTON_HEAD + 6) {
         int f = id - PISTON_HEAD;
         int base = w.getBlock(x - FDX[f], y - FDY[f], z - FDZ[f]);
         if (!isPistonExtended(base) || pistonFacing(base) != f) {
            w.setBlock(x, y, z, 0);
         }
         return;
      }
      if (isPistonExtended(id)) {
         int f = pistonFacing(id);
         if (w.getBlock(x + FDX[f], y + FDY[f], z + FDZ[f]) != PISTON_HEAD + f) {
            w.setBlock(x, y, z, (isStickyPiston(id) ? STICKY_PISTON : PISTON) + f);
            return;
         }
      }

      propagate(w, x, y, z);
   }

   /**
    * Recomputes wire power levels within a 15-block neighbourhood using fast BFS relaxation and updates
    * Redstone Torches (NOT-gate inverters), Repeaters, Pistons, Lamps, Doors, Trapdoors, Gates, Note Blocks, and TNT.
    * Batches all chunk section re-meshes to execute once at the end.
    */
   static void propagate(World w, int cx, int cy, int cz) {
      if (updating || w == null) {
         return;
      }

      updating = true;
      try {
         HashSet<Long> remesh = new HashSet<>();
         for (int pass = 0; pass < 3; pass++) {
            ArrayList<int[]> wires = new ArrayList<>();
            boolean stateChanged = relaxWires(w, cx, cy, cz, wires, remesh);
            stateChanged |= actuateNetwork(w, cx, cy, cz, wires, remesh);
            if (!stateChanged) {
               break;
            }
         }

         if (w.game != null && !remesh.isEmpty()) {
            for (long k : remesh) {
               w.game.meshNow(unpackX(k), unpackY(k), unpackZ(k));
            }
         }
      } finally {
         updating = false;
      }
   }

   private static boolean relaxWires(World w, int cx, int cy, int cz, ArrayList<int[]> wires, HashSet<Long> remesh) {
      HashMap<Long, Integer> idxMap = new HashMap<>();
      for (int dy = -4; dy <= 4; dy++) {
         int y = cy + dy;
         if (y < 1 || y > 254) {
            continue;
         }

         for (int dz = -15; dz <= 15; dz++) {
            for (int dx = -15; dx <= 15; dx++) {
               if (Math.abs(dx) + Math.abs(dz) <= 16) {
                  int wx = cx + dx;
                  int wz = cz + dz;
                  int id = w.getBlock(wx, y, wz);
                  if (isWire(id)) {
                     idxMap.put(pack(wx, y, wz), wires.size());
                     wires.add(new int[]{wx, y, wz});
                  }
               }
            }
         }
      }

      if (wires.isEmpty()) {
         return false;
      }

      int n = wires.size();
      int[] pwr = new int[n];
      ArrayDeque<Integer> q = new ArrayDeque<>();
      int[][] nb6 = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
      for (int i = 0; i < n; i++) {
         int[] p = wires.get(i);
         int maxSrc = 0;
         for (int[] d : nb6) {
            int nx = p[0] + d[0];
            int ny = p[1] + d[1];
            int nz = p[2] + d[2];
            int nb = w.getBlock(nx, ny, nz);
            int sp = sourcePower(nb);
            if (isRepeaterOn(nb)) {
               int rf = repeaterFacing(nb);
               if (nx + HDX[rf] == p[0] && ny == p[1] && nz + HDZ[rf] == p[2]) {
                  sp = 15;
               }
            } else if (nb >= OBSERVER_ON && nb < OBSERVER_ON + 4) {
               int of = observerFacing(nb);
               if (nx - HDX[of] == p[0] && ny == p[1] && nz - HDZ[of] == p[2]) {
                  sp = 15;
               }
            }
            if (sp > maxSrc) {
               maxSrc = sp;
            }
         }
         pwr[i] = maxSrc;
         if (maxSrc > 1) {
            q.add(i);
         }
      }

      while (!q.isEmpty()) {
         int idx = q.poll();
         int curP = pwr[idx];
         if (curP <= 1) {
            continue;
         }
         int nextP = curP - 1;
         int[] a = wires.get(idx);
         int ax = a[0];
         int ay = a[1];
         int az = a[2];
         boolean upClear = ay < 254 && !Blocks.opaque[w.getBlock(ax, ay + 1, az)];
         for (int d = 0; d < 4; d++) {
            int nx = ax + HDX[d];
            int nz = az + HDZ[d];
            offerWireIdx(q, idxMap, pwr, nx, ay, nz, nextP);
            if (upClear && ay + 1 <= 254) {
               offerWireIdx(q, idxMap, pwr, nx, ay + 1, nz, nextP);
            }
            if (ay > 1 && !Blocks.opaque[w.getBlock(nx, ay, nz)]) {
               offerWireIdx(q, idxMap, pwr, nx, ay - 1, nz, nextP);
            }
         }
      }

      boolean changed = false;
      for (int i = 0; i < n; i++) {
         int[] p = wires.get(i);
         int want = REDSTONE_WIRE0 + pwr[i];
         if (w.getBlock(p[0], p[1], p[2]) != want) {
            w.setBlock(p[0], p[1], p[2], want);
            remesh.add(pack(p[0], p[1], p[2]));
            changed = true;
         }
      }
      return changed;
   }

   private static void offerWireIdx(ArrayDeque<Integer> q, HashMap<Long, Integer> idxMap, int[] pwr, int nx, int ny, int nz, int nextP) {
      Integer j = idxMap.get(pack(nx, ny, nz));
      if (j != null && nextP > pwr[j]) {
         pwr[j] = nextP;
         q.add(j);
      }
   }

   private static boolean actuateNetwork(World w, int cx, int cy, int cz, ArrayList<int[]> wires, HashSet<Long> remesh) {
      HashSet<Long> visited = new HashSet<>();
      boolean changed = actuateAround(w, cx, cy, cz, visited, remesh);
      for (int[] p : wires) {
         changed |= actuateAround(w, p[0], p[1], p[2], visited, remesh);
      }
      return changed;
   }

   private static boolean actuateAround(World w, int cx, int cy, int cz, HashSet<Long> visited, HashSet<Long> remesh) {
      boolean changed = false;
      for (int dy = -1; dy <= 2; dy++) {
         for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
               int x = cx + dx;
               int y = cy + dy;
               int z = cz + dz;
               if (y < 1 || y > 254) {
                  continue;
               }
               long key = pack(x, y, z);
               if (!visited.add(key)) {
                  continue;
               }

               int id = w.getBlock(x, y, z);
               // 1. Redstone Torch NOT-gate inverter (turns OFF when block beneath it is powered)
               if (id == REDSTONE_TORCH || id == REDSTONE_TORCH_OFF) {
                  boolean supportPow = y > 1 && isSupportPowered(w, x, y - 1, z);
                  int want = supportPow ? REDSTONE_TORCH_OFF : REDSTONE_TORCH;
                  if (id != want) {
                     w.setBlock(x, y, z, want);
                     remesh.add(key);
                     changed = true;
                  }
                  continue;
               }
               // 2. Redstone Repeater (reads rear input, outputs full 15 forward)
               if (isRepeater(id)) {
                  int f = repeaterFacing(id);
                  int inX = x - HDX[f];
                  int inZ = z - HDZ[f];
                  int inB = w.getBlock(inX, y, inZ);
                  boolean inPow = sourcePower(inB) > 0
                     || (isWire(inB) && inB > REDSTONE_WIRE0)
                     || (isRepeaterOn(inB) && repeaterFacing(inB) == f)
                     || (Blocks.opaque[inB] && isSupportPowered(w, inX, y, inZ));
                  int want = (inPow ? REPEATER_ON : REPEATER_OFF) + f;
                  if (id != want) {
                     w.setBlock(x, y, z, want);
                     remesh.add(key);
                     changed = true;
                  }
                  continue;
               }
               // 3. Pistons & Sticky Pistons
               if (isPistonBase(id)) {
                  int f = pistonFacing(id);
                  boolean ext = isPistonExtended(id);
                  boolean pow = isPistonPowered(w, x, y, z, f);
                  if (pow && !ext) {
                     changed |= extendPiston(w, x, y, z, f, isStickyPiston(id), remesh);
                  } else if (!pow && ext) {
                     changed |= retractPiston(w, x, y, z, f, isStickyPiston(id), remesh);
                  }
                  continue;
               }

               boolean pow = isPowered(w, x, y, z);
               if (id == REDSTONE_LAMP && pow) {
                  w.setBlock(x, y, z, REDSTONE_LAMP_LIT);
                  remesh.add(key);
                  changed = true;
               } else if (id == REDSTONE_LAMP_LIT && !pow) {
                  w.setBlock(x, y, z, REDSTONE_LAMP);
                  remesh.add(key);
                  changed = true;
               } else if (Shapes.isTrap(id)) {
                  boolean open = Shapes.trapOpen(id);
                  if (pow != open) {
                     w.setBlock(x, y, z, Shapes.toggled(id));
                     remesh.add(key);
                  }
               } else if (Shapes.isGate(id)) {
                  boolean open = Shapes.gateOpen(id);
                  if (pow != open) {
                     w.setBlock(x, y, z, Shapes.toggled(id));
                     remesh.add(key);
                  }
               } else if (Blocks.isDoorLo(id)) {
                  boolean open = Blocks.doorOpen(id);
                  int hi = w.getBlock(x, y + 1, z);
                  if (Blocks.isDoorHi(hi)) {
                     if (pow) {
                        redstoneOpened.add(key);
                        if (!open) {
                           w.setBlock(x, y, z, id + 4);
                           w.setBlock(x, y + 1, z, hi + 4);
                           remesh.add(key);
                           remesh.add(pack(x, y + 1, z));
                        }
                     } else if (open && redstoneOpened.remove(key)) {
                        w.setBlock(x, y, z, id - 4);
                        w.setBlock(x, y + 1, z, hi - 4);
                        remesh.add(key);
                        remesh.add(pack(x, y + 1, z));
                     }
                  }
               } else if (id == NOTE_BLOCK) {
                  if (pow && redstoneOpened.add(key)) {
                     playNote(w.game, x, y, z);
                  } else if (!pow) {
                     redstoneOpened.remove(key);
                  }
               } else if (id == Blocks.TNT && pow) {
                  w.setBlock(x, y, z, 0);
                  w.primeTnt(x, y, z, 80);
                  remesh.add(key);
               }
            }
         }
      }
      return changed;
   }

   private static boolean isSupportPowered(World w, int bx, int by, int bz) {
      int[][] dirs = {{1, 0, 0}, {-1, 0, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
      for (int[] d : dirs) {
         int nx = bx + d[0];
         int ny = by + d[1];
         int nz = bz + d[2];
         int nb = w.getBlock(nx, ny, nz);
         if (sourcePower(nb) > 0 || (isWire(nb) && nb > REDSTONE_WIRE0)) {
            return true;
         }
         if (isRepeaterOn(nb)) {
            int rf = repeaterFacing(nb);
            if (nx + HDX[rf] == bx && ny == by && nz + HDZ[rf] == bz) {
               return true;
            }
         }
      }
      return false;
   }

   private static boolean isPistonPowered(World w, int x, int y, int z, int facing) {
      int[][] dirs = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
      for (int[] d : dirs) {
         if (d[0] == FDX[facing] && d[1] == FDY[facing] && d[2] == FDZ[facing]) {
            continue;
         }
         int nx = x + d[0];
         int ny = y + d[1];
         int nz = z + d[2];
         int nb = w.getBlock(nx, ny, nz);
         if (sourcePower(nb) > 0 || (isWire(nb) && nb > REDSTONE_WIRE0)) {
            return true;
         }
         if (isRepeaterOn(nb)) {
            int rf = repeaterFacing(nb);
            if (nx + HDX[rf] == x && ny == y && nz + HDZ[rf] == z) {
               return true;
            }
         }
      }
      return false;
   }

   private static boolean canPistonPush(int id, int y) {
      if (id == 0 || Blocks.replaceable[id]) {
         return true;
      }
      if (y <= 0 || y >= 255 || id == Blocks.BEDROCK || id == Blocks.OBSIDIAN || id == Blocks.FURNACE || id == Blocks.FURNACE_LIT || id == Blocks.CHEST || id == Blocks.CHEST_LOOT || Blocks.isDoor(id)) {
         return false;
      }
      return !isPistonExtended(id) && !(id >= PISTON_HEAD && id < PISTON_HEAD + 6);
   }

   private static boolean extendPiston(World w, int x, int y, int z, int f, boolean sticky, HashSet<Long> remesh) {
      int dx = FDX[f];
      int dy = FDY[f];
      int dz = FDZ[f];
      int count = 0;
      while (count < 12) {
         int tx = x + dx * (count + 1);
         int ty = y + dy * (count + 1);
         int tz = z + dz * (count + 1);
         if (ty < 1 || ty > 254) {
            return false;
         }
         int tb = w.getBlock(tx, ty, tz);
         if (tb == 0 || Blocks.replaceable[tb]) {
            break;
         }
         if (!canPistonPush(tb, ty)) {
            return false;
         }
         count++;
         if (count == 12) {
            int endB = w.getBlock(x + dx * 13, y + dy * 13, z + dz * 13);
            if (endB != 0 && !Blocks.replaceable[endB]) {
               return false;
            }
         }
      }

      for (int i = count; i >= 1; i--) {
         int sx = x + dx * i;
         int sy = y + dy * i;
         int sz = z + dz * i;
         int dstX = sx + dx;
         int dstY = sy + dy;
         int dstZ = sz + dz;
         int moved = w.getBlock(sx, sy, sz);
         if (!Blocks.solid[moved] && moved != SLIME_BLOCK) {
            int drop = Blocks.drop[moved];
            if (drop > 0) {
               w.dropItem(sx + 0.5, sy + 0.3, sz + 0.5, drop, 1);
            }
            w.setBlock(sx, sy, sz, 0);
         } else {
            w.setBlock(dstX, dstY, dstZ, moved);
            remesh.add(pack(dstX, dstY, dstZ));
         }
         remesh.add(pack(sx, sy, sz));
      }

      int hx = x + dx;
      int hy = y + dy;
      int hz = z + dz;
      w.setBlock(hx, hy, hz, PISTON_HEAD + f);
      w.setBlock(x, y, z, (sticky ? STICKY_PISTON_EXT : PISTON_EXT) + f);
      remesh.add(pack(hx, hy, hz));
      remesh.add(pack(x, y, z));
      return true;
   }

   private static boolean retractPiston(World w, int x, int y, int z, int f, boolean sticky, HashSet<Long> remesh) {
      int dx = FDX[f];
      int dy = FDY[f];
      int dz = FDZ[f];
      int hx = x + dx;
      int hy = y + dy;
      int hz = z + dz;
      w.setBlock(x, y, z, (sticky ? STICKY_PISTON : PISTON) + f);
      remesh.add(pack(x, y, z));
      if (w.getBlock(hx, hy, hz) == PISTON_HEAD + f) {
         w.setBlock(hx, hy, hz, 0);
         remesh.add(pack(hx, hy, hz));
      }
      if (sticky) {
         int px = x + dx * 2;
         int py = y + dy * 2;
         int pz = z + dz * 2;
         if (py >= 1 && py <= 254) {
            int pull = w.getBlock(px, py, pz);
            if (pull != 0 && Blocks.solid[pull] && canPistonPush(pull, py)) {
               w.setBlock(hx, hy, hz, pull);
               w.setBlock(px, py, pz, 0);
               remesh.add(pack(hx, hy, hz));
               remesh.add(pack(px, py, pz));
            }
         }
      }
      return true;
   }

   private static long pack(int x, int y, int z) {
      return ((long)(x & 0x3FFFFFF) << 38) | ((long)(z & 0x3FFFFFF) << 12) | (y & 0xFFF);
   }

   private static int unpackX(long k) {
      int v = (int)(k >> 38);
      return (v & 0x2000000) != 0 ? (v | 0xFC000000) : v;
   }

   private static int unpackZ(long k) {
      int v = (int)((k >> 12) & 0x3FFFFFFL);
      return (v & 0x2000000) != 0 ? (v | 0xFC000000) : v;
   }

   private static int unpackY(long k) {
      return (int)(k & 0xFFFL);
   }

   /** Called every tick from {@link Game}: button/observer timers, pressure plates, daylight sensors, and Slime Block bounce. */
   static void tick(Game g) {
      if (g == null || g.world == null || g.player == null) {
         return;
      }

      World w = g.world;
      // 1. Button & Observer auto-release timers
      if (!buttonTimers.isEmpty()) {
         Iterator<Map.Entry<Long, Integer>> it = buttonTimers.entrySet().iterator();
         while (it.hasNext()) {
            Map.Entry<Long, Integer> e = it.next();
            int rem = e.getValue() - 1;
            if (rem <= 0) {
               long k = e.getKey();
               it.remove();
               int x = unpackX(k);
               int y = unpackY(k);
               int z = unpackZ(k);
               int id = w.getBlock(x, y, z);
               if (id == STONE_BUTTON_ON || id == WOOD_BUTTON_ON) {
                  w.setBlock(x, y, z, id == STONE_BUTTON_ON ? STONE_BUTTON : WOOD_BUTTON);
                  propagate(w, x, y, z);
                  g.meshNow(x, y, z);
               } else if (id >= OBSERVER_ON && id < OBSERVER_ON + 4) {
                  int f = id - OBSERVER_ON;
                  w.setBlock(x, y, z, OBSERVER + f);
                  propagate(w, x - HDX[f], y, z - HDZ[f]);
                  g.meshNow(x, y, z);
               }
            } else {
               e.setValue(rem);
            }
         }
      }

      // 2. Pressure plates and daylight sensors around the player
      int px = (int)Math.floor(g.player.x);
      int py = (int)Math.floor(g.player.y);
      int pz = (int)Math.floor(g.player.z);
      long tod = ((w.dayTime % 24000L) + 24000L) % 24000L;
      boolean day = w.dim == Dim.OVERWORLD && (tod < 12500L || tod > 23500L);
      for (int dz = -2; dz <= 2; dz++) {
         for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
               int x = px + dx;
               int y = py + dy;
               int z = pz + dz;
               int id = w.getBlock(x, y, z);
               if (id >= STONE_PLATE && id <= WOOD_PLATE_ON) {
                  boolean stood = entityOnPlate(g, x, y, z);
                  boolean wasOn = id == STONE_PLATE_ON || id == WOOD_PLATE_ON;
                  if (stood != wasOn) {
                     int next = (id == STONE_PLATE || id == STONE_PLATE_ON) ? (stood ? STONE_PLATE_ON : STONE_PLATE) : (stood ? WOOD_PLATE_ON : WOOD_PLATE);
                     w.setBlock(x, y, z, next);
                     propagate(w, x, y, z);
                     g.meshNow(x, y, z);
                  }
               } else if ((id == DAYLIGHT_SENSOR || id == DAYLIGHT_SENSOR_ON) && (w.tickCount & 19L) == 0L) {
                  int want = day && w.getSky(x, y, z) >= 10 ? DAYLIGHT_SENSOR_ON : DAYLIGHT_SENSOR;
                  if (id != want) {
                     w.setBlock(x, y, z, want);
                     propagate(w, x, y, z);
                     g.meshNow(x, y, z);
                  }
               }
            }
         }
      }
   }

   private static boolean entityOnPlate(Game g, int x, int y, int z) {
      if (!g.player.spectator && Math.abs(g.player.x - (x + 0.5)) < 0.7 && Math.abs(g.player.z - (z + 0.5)) < 0.7 && Math.abs(g.player.y - y) < 0.8) {
         return true;
      }

      if (g.mobs != null) {
         for (Mobs.Mob m : g.mobs.list) {
            if (!m.dead && Math.abs(m.x - (x + 0.5)) < 0.7 && Math.abs(m.z - (z + 0.5)) < 0.7 && Math.abs(m.y - y) < 0.8) {
               return true;
            }
         }
      }

      return false;
   }

   // ------------------------------------------------------------------ Textures (tiles 643..671)
   static void paint() {
      Tex.blit(T_REDSTONE_BLOCK, TexPaint319.gemBlock(0xD82218, 0xA8140E, 0x6E0A06, 0xFF6852, 643));
      Tex.blit(T_LAMP_OFF, lamp(false));
      Tex.blit(T_LAMP_ON, lamp(true));
      Tex.blit(T_REDSTONE_TORCH, redstoneTorch(true));
      Tex.blit(T_REDSTONE_TORCH_OFF, redstoneTorch(false));
      Tex.blit(T_REDSTONE_WIRE, redstoneWire(false));
      Tex.blit(T_REDSTONE_WIRE_LIT, redstoneWire(true));
      Tex.blit(T_REDSTONE_DUST, redstoneDustItem());
      Tex.blit(T_REPEATER_OFF, repeaterTop(false));
      Tex.blit(T_REPEATER_ON, repeaterTop(true));
      Tex.blit(T_PISTON_SIDE, pistonSide());
      Tex.blit(T_PISTON_TOP, pistonTop(false));
      Tex.blit(T_PISTON_TOP_STICKY, pistonTop(true));
      Tex.blit(T_PISTON_BOTTOM, pistonBottom());
      Tex.blit(T_DAYLIGHT_TOP, daylightTop());
      Tex.blit(T_NOTE_BLOCK, noteBlockTile());
      Tex.blit(T_OBSERVER_FRONT, observerFront());
      Tex.blit(T_POL_GRANITE, polished(TexPaint319.mineralStone(418, new int[]{TexPaint.c(0x7E5242), TexPaint.c(0x926250), TexPaint.c(0xA2715E), TexPaint.c(0xB2806C)}), 0xC89682, 0x5C382C));
      Tex.blit(T_POL_DIORITE, polished(TexPaint319.mineralStone(419, new int[]{TexPaint.c(0x9E9E9E), TexPaint.c(0xB8B8B8), TexPaint.c(0xCFCFCF), TexPaint.c(0xE2E2E2)}), 0xF4F4F4, 0x747474));
      Tex.blit(T_POL_ANDESITE, polished(TexPaint319.mineralStone(420, new int[]{TexPaint.c(0x68686A), TexPaint.c(0x78787A), TexPaint.c(0x88888A), TexPaint.c(0x98989A)}), 0xB2B2B6, 0x4E4E52));
      Tex.blit(T_CHIS_SANDSTONE, TexPaint319.carvedSandstone(false));
      Tex.blit(T_SMOOTH_SANDSTONE, TexPaint319.smoothSandstone(false));
      Tex.blit(T_CHIS_RED_SANDSTONE, TexPaint319.carvedSandstone(true));
      Tex.blit(T_SMOOTH_RED_SANDSTONE, TexPaint319.smoothSandstone(true));
      Tex.blit(T_CHIS_STONEBRICK, chiseledStoneBrick());
      Tex.blit(T_MOSSY_STONEBRICK, mossyStoneBrick());
      Tex.blit(T_CRACKED_STONEBRICK, crackedStoneBrick());
      Tex.blit(T_SLIME, slimeBlock());
      Tex.blit(T_SEA_LANTERN, seaLantern());
   }

   private static int[] lamp(boolean lit) {
      int[] p = new int[256];
      int frame = TexPaint.c(0x362418);
      int rim = TexPaint.c(0x583C28);
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            boolean border = x == 0 || y == 0 || x == 15 || y == 15;
            boolean lattice = x == 1 || y == 1 || x == 14 || y == 14 || Math.abs(x - y) == 0 && (x < 4 || x > 11) || Math.abs(x + y - 15) == 0 && (x < 4 || x > 11);
            if (border) {
               p[y * 16 + x] = frame;
            } else if (lattice) {
               p[y * 16 + x] = rim;
            } else {
               float d = (float)Math.hypot(x - 7.5F, y - 7.5F) / 8.0F;
               if (lit) {
                  p[y * 16 + x] = d < 0.45F ? TexPaint.c(0xFFF8C0) : (d < 0.75F ? TexPaint.c(0xF8CC58) : TexPaint.c(0xD89228));
               } else {
                  p[y * 16 + x] = d < 0.5F ? TexPaint.c(0x7A4E28) : TexPaint.c(0x5E3A1C);
               }
            }
         }
      }

      return p;
   }

   private static int[] redstoneTorch(boolean lit) {
      int[] p = TexPaint319.torch();
      p[5 * 16 + 7] = TexPaint.c(lit ? 0xD81E14 : 0x581410);
      p[5 * 16 + 8] = TexPaint.c(lit ? 0xB8140C : 0x46100C);
      p[4 * 16 + 7] = TexPaint.c(lit ? 0xFF9888 : 0x7A221C);
      p[4 * 16 + 8] = TexPaint.c(lit ? 0xFF4232 : 0x621812);
      p[3 * 16 + 7] = TexPaint.c(lit ? 0xFF5848 : 0x681A14);
      p[3 * 16 + 8] = TexPaint.c(lit ? 0xD81E14 : 0x52120E);
      p[2 * 16 + 7] = TexPaint.c(lit ? 0xB8140C : 0x3E0C08);
      return p;
   }

   /**
    * Full 16x16 opaque granular redstone wire texture so dynamic wireBoxes (dot, line, corner, T, cross, step-up)
    * never have transparent holes or UV-rotation gaps.
    */
   private static int[] redstoneWire(boolean lit) {
      int[] p = new int[256];
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            float n = TexPaint.fbm(x, y, lit ? 661 : 647);
            float center = 1.0F - Math.min(1.0F, Math.min(Math.abs(x - 7.5F), Math.abs(y - 7.5F)) / 3.5F);
            int g = (int)Math.min(255.0F, (lit ? 205.0F : 165.0F) + center * 38.0F + (n - 0.5F) * 36.0F);
            int r = lit ? Math.min(255, g + 25) : g;
            int gb = lit ? Math.min(255, (int)(g * 0.85F)) : g;
            p[y * 16 + x] = TexPaint.rgb(r, gb, gb);
         }
      }
      return p;
   }

   /** Item icon for Redstone Dust (450): a sparkling mound of crimson redstone powder. */
   private static int[] redstoneDustItem() {
      int[] p = new int[256];
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            float dx = (x - 7.5F) / 5.8F;
            float dy = (y - 9.0F) / 4.8F;
            float d = dx * dx + dy * dy + (TexPaint.fbm(x, y, 660) - 0.5F) * 0.35F;
            if (y >= 4 && y <= 14 && d < 1.0F) {
               int col = d < 0.25F ? 0xFF6450 : (d < 0.55F ? 0xE6261A : (d < 0.82F ? 0xB8140C : 0x7A0A06));
               p[y * 16 + x] = TexPaint.c(col);
            }
         }
      }
      return p;
   }

   private static int[] repeaterTop(boolean lit) {
      int[] p = TexPaint319.smoothSandstone(false);
      for (int i = 0; i < 256; i++) {
         p[i] = TexPaint.c(0x96969A);
      }
      int trace = TexPaint.c(lit ? 0xF0281C : 0x58100C);
      for (int y = 2; y <= 13; y++) {
         p[y * 16 + 7] = trace;
         p[y * 16 + 8] = trace;
      }
      // Arrowhead pointing north (-Z / top of tile)
      p[4 * 16 + 6] = trace;
      p[4 * 16 + 9] = trace;
      p[5 * 16 + 5] = trace;
      p[5 * 16 + 10] = trace;
      return p;
   }

   private static int[] pistonSide() {
      int[] p = new int[256];
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            if (y < 4) {
               p[y * 16 + x] = ((x + y) & 1) == 0 ? TexPaint.c(0x9E7E52) : TexPaint.c(0x886A42);
            } else {
               p[y * 16 + x] = ((x * 3 + y) & 3) == 0 ? TexPaint.c(0x5E5E62) : TexPaint.c(0x76767A);
            }
         }
      }
      return p;
   }

   private static int[] pistonTop(boolean sticky) {
      int[] p = new int[256];
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            boolean rim = x == 0 || y == 0 || x == 15 || y == 15;
            boolean slime = sticky && x >= 4 && x <= 11 && y >= 4 && y <= 11;
            if (slime) {
               p[y * 16 + x] = ((x + y) & 1) == 0 ? TexPaint.c(0x74C854) : TexPaint.c(0x569E3C);
            } else {
               p[y * 16 + x] = rim ? TexPaint.c(0x6E5434) : TexPaint.c(0xA48456);
            }
         }
      }
      return p;
   }

   private static int[] pistonBottom() {
      int[] p = new int[256];
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            boolean rim = x == 0 || y == 0 || x == 15 || y == 15;
            p[y * 16 + x] = rim ? TexPaint.c(0x4E4E52) : TexPaint.c(0x6E6E74);
         }
      }
      return p;
   }

   private static int[] daylightTop() {
      int[] p = new int[256];
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            boolean frame = x < 2 || y < 2 || x > 13 || y > 13 || x == 5 || x == 10 || y == 5 || y == 10;
            p[y * 16 + x] = frame ? TexPaint.c(0x6E5236) : TexPaint.c(0xC8D8E4);
         }
      }
      return p;
   }

   private static int[] noteBlockTile() {
      int[] p = new int[256];
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            boolean rim = x == 0 || y == 0 || x == 15 || y == 15;
            boolean grid = ((x + y) & 1) == 0;
            p[y * 16 + x] = rim ? TexPaint.c(0x422816) : (grid ? TexPaint.c(0x6E4628) : TexPaint.c(0x5A3820));
         }
      }
      return p;
   }

   private static int[] observerFront() {
      int[] p = pistonBottom();
      // Observer face eyes & redstone sensor brow
      p[6 * 16 + 4] = p[6 * 16 + 5] = p[6 * 16 + 10] = p[6 * 16 + 11] = TexPaint.c(0x222226);
      p[7 * 16 + 4] = p[7 * 16 + 11] = TexPaint.c(0xE8E8EC);
      p[7 * 16 + 5] = p[7 * 16 + 10] = TexPaint.c(0x18181C);
      p[10 * 16 + 7] = p[10 * 16 + 8] = TexPaint.c(0xD82418);
      return p;
   }

   private static int[] polished(int[] base, int hiRgb, int loRgb) {
      int[] p = base.clone();
      int hi = TexPaint.c(hiRgb);
      int lo = TexPaint.c(loRgb);
      for (int i = 0; i < 16; i++) {
         p[i] = i < 15 ? hi : lo;
         p[i * 16] = i < 15 ? hi : lo;
         p[15 * 16 + i] = lo;
         p[i * 16 + 15] = lo;
      }

      return p;
   }

   private static int[] chiseledStoneBrick() {
      int[] p = TexPaint.stoneBrick();
      int dark = TexPaint.c(0x4E4E52);
      int hi = TexPaint.c(0xA8A8AE);
      for (int ring : new int[]{2, 5}) {
         for (int i = ring; i <= 15 - ring; i++) {
            p[ring * 16 + i] = hi;
            p[i * 16 + ring] = hi;
            p[(15 - ring) * 16 + i] = dark;
            p[i * 16 + (15 - ring)] = dark;
         }
      }

      return p;
   }

   private static int[] mossyStoneBrick() {
      int[] p = TexPaint.stoneBrick();
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            if (TexPaint.fbm(x, y, 656) > 0.56F) {
               p[y * 16 + x] = ((x + y) & 1) == 0 ? TexPaint.c(0x4E7A32) : TexPaint.c(0x3A5E24);
            }
         }
      }

      return p;
   }

   private static int[] crackedStoneBrick() {
      int[] p = TexPaint.stoneBrick();
      int crack = TexPaint.c(0x38383C);
      int[][] pts = {{3, 2}, {4, 3}, {4, 4}, {5, 5}, {11, 6}, {10, 7}, {10, 8}, {9, 9}, {6, 11}, {7, 12}, {7, 13}};
      for (int[] c : pts) {
         p[c[1] * 16 + c[0]] = crack;
      }

      return p;
   }

   private static int[] slimeBlock() {
      int[] p = new int[256];
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            boolean core = x >= 3 && x <= 12 && y >= 3 && y <= 12;
            boolean border = x == 0 || y == 0 || x == 15 || y == 15;
            p[y * 16 + x] = border ? TexPaint.c(0x529E3E) : (core ? TexPaint.c(0x64B84C) : TexPaint.c(0x7ED464));
         }
      }

      return p;
   }

   private static int[] seaLantern() {
      int[] p = new int[256];
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            int d = Math.min(Math.min(x, 15 - x), Math.min(y, 15 - y));
            p[y * 16 + x] = d == 0 ? TexPaint.c(0x467E78) : (d == 1 ? TexPaint.c(0x7AB8B0) : (d <= 3 ? TexPaint.c(0xC8F4EC) : TexPaint.c(0xF0FFFC)));
         }
      }

      return p;
   }
}
