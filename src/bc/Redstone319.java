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
   static final int PRISMARINE = 1952;
   static final int PRISMARINE_BRICKS = 1953;
   static final int DARK_PRISMARINE = 1954;
   static final int PURPUR_BLOCK = 1955;
   static final int PURPUR_PILLAR = 1956;
   static final int BONE_BLOCK = 1957;

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
   // Preserve saved horizontal IDs; 2010..2020 belong to the Backrooms mod.
   static final int OBSERVER_VERTICAL = 2030; // down, up
   static final int OBSERVER_VERTICAL_ON = 2032;

   static final int ITEM_REDSTONE = 450;
   static final int GOLDEN_APPLE = 451;
   static final int GOLDEN_CARROT = 452;
   static final int COOKIE = 453;
   static final int MUSHROOM_STEW = 454;
   static final int BOWL = 455;
   static final int COMPASS = 456;
   static final int CLOCK = 457;
   static final int FIRE_CHARGE = 458;
   static final int PISTOL = 459;
   static final int MACHINE_GUN = 460;
   static final int SNIPER_RIFLE = 461;
   static final int TNT_GRENADE = 462;
   static final int MURDER_KNIFE = 463;
   static final int C4_BOMB = 464;
   static final int SIEGE_CANNON = 465;
   static final int MORTAR_STRIKE = 466;

   // Atlas tiles (643..693)
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
   static final int T_GOLDEN_APPLE = 672;
   static final int T_GOLDEN_CARROT = 673;
   static final int T_COOKIE = 674;
   static final int T_MUSHROOM_STEW = 675;
   static final int T_BOWL = 676;
   static final int T_COMPASS = 677;
   static final int T_CLOCK = 678;
   static final int T_FIRE_CHARGE = 679;
   static final int T_PRISMARINE = 680;
   static final int T_PRISMARINE_BRICKS = 681;
   static final int T_DARK_PRISMARINE = 682;
   static final int T_PURPUR_BLOCK = 683;
   static final int T_PURPUR_TOP = 684;
   static final int T_PURPUR_PILLAR = 685;
   static final int T_BONE_TOP = 686;
   static final int T_BONE_SIDE = 687;
   static final int T_PISTOL = 688;
   static final int T_MACHINE_GUN = 689;
   static final int T_SNIPER_RIFLE = 690;
   static final int T_TNT_GRENADE = 691;
   static final int T_MURDER_KNIFE = 692;
   static final int T_C4_BOMB = 693;
   static final int T_SIEGE_CANNON = 694;
   static final int T_MORTAR_STRIKE = 695;

   private static final int[] HDX = {0, 0, 1, -1};
   private static final int[] HDZ = {-1, 1, 0, 0};
   private static final int[] FDX = {0, 0, 0, 0, 1, -1};
   private static final int[] FDY = {-1, 1, 0, 0, 0, 0};
   private static final int[] FDZ = {0, 0, -1, 1, 0, 0};



   private Redstone319() {
   }

   static void registerItems() {
      Items.defTile(ITEM_REDSTONE, "Redstone Dust", T_REDSTONE_DUST);
      Items.defTile(GOLDEN_APPLE, "Golden Apple", T_GOLDEN_APPLE);
      Items.foodPts[GOLDEN_APPLE] = 4;
      Items.foodSat[GOLDEN_APPLE] = 9.6F;
      Items.defTile(GOLDEN_CARROT, "Golden Carrot", T_GOLDEN_CARROT);
      Items.foodPts[GOLDEN_CARROT] = 6;
      Items.foodSat[GOLDEN_CARROT] = 14.4F;
      Items.defTile(COOKIE, "Cookie", T_COOKIE);
      Items.foodPts[COOKIE] = 2;
      Items.foodSat[COOKIE] = 0.4F;
      Items.defTile(MUSHROOM_STEW, "Mushroom Stew", T_MUSHROOM_STEW);
      Items.foodPts[MUSHROOM_STEW] = 6;
      Items.foodSat[MUSHROOM_STEW] = 7.2F;
      Items.maxStack[MUSHROOM_STEW] = 1;
      Items.defTile(BOWL, "Bowl", T_BOWL);
      Items.defTile(COMPASS, "Compass", T_COMPASS);
      Items.defTile(CLOCK, "Clock", T_CLOCK);
      Items.defTile(FIRE_CHARGE, "Fire Charge", T_FIRE_CHARGE);
      Items.defTile(PISTOL, "Tactical Pistol", T_PISTOL);
      Items.maxStack[PISTOL] = 1;
      Items.defTile(MACHINE_GUN, "Assault SMG", T_MACHINE_GUN);
      Items.maxStack[MACHINE_GUN] = 1;
      Items.defTile(SNIPER_RIFLE, "Heavy Sniper", T_SNIPER_RIFLE);
      Items.maxStack[SNIPER_RIFLE] = 1;
      Items.defTile(TNT_GRENADE, "TNT Frag Grenade", T_TNT_GRENADE);
      Items.maxStack[TNT_GRENADE] = 16;
      Items.defTile(MURDER_KNIFE, "Murderer's Knife", T_MURDER_KNIFE);
      Items.maxStack[MURDER_KNIFE] = 1;
      Items.defTile(C4_BOMB, "C4 Explosive", T_C4_BOMB);
      Items.maxStack[C4_BOMB] = 1;
      Items.defTile(SIEGE_CANNON, "Siege Cannon", T_SIEGE_CANNON);
      Items.maxStack[SIEGE_CANNON] = 1;
      Items.defTile(MORTAR_STRIKE, "Mortar Launcher", T_MORTAR_STRIKE);
      Items.maxStack[MORTAR_STRIKE] = 1;
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
            Shapes.local(Shapes.tiles(Shapes.px(7, 0, 7, 9, 7, 9), plank, plank, plank)),
            Shapes.local(Shapes.tiles(Shapes.px(6.5, 6.5, 6.5, 9.5, 10.5, 9.5), T_REDSTONE_BLOCK, T_REDSTONE_BLOCK, T_REDSTONE_BLOCK))
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
            Shapes.local(Shapes.tiles(Shapes.px(7, 0, 7, 9, 7, 9), plank, plank, plank)),
            Shapes.local(Shapes.tiles(Shapes.px(6.5, 6.5, 6.5, 9.5, 10.5, 9.5), T_REDSTONE_WIRE, T_REDSTONE_WIRE, T_REDSTONE_WIRE))
         }
      );
      Blocks.emit[REDSTONE_TORCH_OFF] = 0;

      modelBlock(
         LEVER_OFF,
         "Lever",
         0.5F,
         cobble,
         LEVER_OFF,
         new double[]{0.25, 0.0, 0.1875, 0.75, 0.6875, 0.8125},
         new double[][]{
            Shapes.local(Shapes.tiles(Shapes.px(4, 0, 3, 12, 3, 13), cobble, cobble, cobble)),
            Shapes.local(Shapes.tiles(Shapes.px(6, 3, 5, 10, 4, 11), T_PISTON_BOTTOM, T_PISTON_BOTTOM, T_PISTON_BOTTOM)),
            Shapes.local(Shapes.tiles(Shapes.px(7, 3.5, 4.5, 9, 10, 6.5), plank, plank, plank)),
            Shapes.local(Shapes.tiles(Shapes.px(6.5, 9.5, 4, 9.5, 11, 7), T_PISTON_BOTTOM, T_PISTON_BOTTOM, T_PISTON_BOTTOM))
         }
      );
      modelBlock(
         LEVER_ON,
         "Lever",
         0.5F,
         cobble,
         LEVER_OFF,
         new double[]{0.25, 0.0, 0.1875, 0.75, 0.6875, 0.8125},
         new double[][]{
            Shapes.local(Shapes.tiles(Shapes.px(4, 0, 3, 12, 3, 13), cobble, cobble, cobble)),
            Shapes.local(Shapes.tiles(Shapes.px(6, 3, 5, 10, 4, 11), T_PISTON_BOTTOM, T_PISTON_BOTTOM, T_PISTON_BOTTOM)),
            Shapes.local(Shapes.tiles(Shapes.px(7, 3.5, 9.5, 9, 10, 11.5), plank, plank, plank)),
            Shapes.local(Shapes.tiles(Shapes.px(6.5, 9.5, 9, 9.5, 11, 12), T_REDSTONE_BLOCK, T_REDSTONE_BLOCK, T_REDSTONE_BLOCK))
         }
      );

      modelBlock(
         STONE_BUTTON,
         "Stone Button",
         0.5F,
         stone,
         STONE_BUTTON,
         new double[]{0.3125, 0.0, 0.375, 0.6875, 0.25, 0.625},
         new double[][]{Shapes.local(Shapes.tiles(Shapes.px(5, 0, 6, 11, 4, 10), stone, stone, stone))}
      );
      modelBlock(
         STONE_BUTTON_ON,
         "Stone Button",
         0.5F,
         stone,
         STONE_BUTTON,
         new double[]{0.3125, 0.0, 0.375, 0.6875, 0.125, 0.625},
         new double[][]{Shapes.local(Shapes.tiles(Shapes.px(5, 0, 6, 11, 2, 10), stone, stone, stone))}
      );

      modelBlock(
         WOOD_BUTTON,
         "Wooden Button",
         0.5F,
         plank,
         WOOD_BUTTON,
         new double[]{0.3125, 0.0, 0.375, 0.6875, 0.25, 0.625},
         new double[][]{Shapes.local(Shapes.tiles(Shapes.px(5, 0, 6, 11, 4, 10), plank, plank, plank))}
      );
      modelBlock(
         WOOD_BUTTON_ON,
         "Wooden Button",
         0.5F,
         plank,
         WOOD_BUTTON,
         new double[]{0.3125, 0.0, 0.375, 0.6875, 0.125, 0.625},
         new double[][]{Shapes.local(Shapes.tiles(Shapes.px(5, 0, 6, 11, 2, 10), plank, plank, plank))}
      );

      modelBlock(
         STONE_PLATE,
         "Stone Pressure Plate",
         0.5F,
         stone,
         STONE_PLATE,
         new double[]{0.0625, 0.0, 0.0625, 0.9375, 0.0625, 0.9375},
         new double[][]{Shapes.tiles(Shapes.px(1, 0, 1, 15, 1.5, 15), stone, stone, stone)}
      );
      modelBlock(
         STONE_PLATE_ON,
         "Stone Pressure Plate",
         0.5F,
         stone,
         STONE_PLATE,
         new double[]{0.0625, 0.0, 0.0625, 0.9375, 0.03125, 0.9375},
         new double[][]{Shapes.tiles(Shapes.px(1, 0, 1, 15, 0.75, 15), stone, stone, stone)}
      );

      modelBlock(
         WOOD_PLATE,
         "Wooden Pressure Plate",
         0.5F,
         plank,
         WOOD_PLATE,
         new double[]{0.0625, 0.0, 0.0625, 0.9375, 0.0625, 0.9375},
         new double[][]{Shapes.tiles(Shapes.px(1, 0, 1, 15, 1.5, 15), plank, plank, plank)}
      );
      modelBlock(
         WOOD_PLATE_ON,
         "Wooden Pressure Plate",
         0.5F,
         plank,
         WOOD_PLATE,
         new double[]{0.0625, 0.0, 0.0625, 0.9375, 0.03125, 0.9375},
         new double[][]{Shapes.tiles(Shapes.px(1, 0, 1, 15, 0.75, 15), plank, plank, plank)}
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
               Shapes.local(Shapes.tiles(Shapes.px(7, 2, 2, 9, 5, 4), plank, plank, plank)),
               Shapes.local(Shapes.tiles(Shapes.px(6.75, 5, 1.75, 9.25, 7, 4.25), T_REDSTONE_WIRE, T_REDSTONE_WIRE, T_REDSTONE_WIRE)),
               Shapes.local(Shapes.tiles(Shapes.px(7, 2, 10, 9, 5, 12), plank, plank, plank)),
               Shapes.local(Shapes.tiles(Shapes.px(6.75, 5, 9.75, 9.25, 7, 12.25), T_REDSTONE_WIRE, T_REDSTONE_WIRE, T_REDSTONE_WIRE))
            },
            turns
         );
         modelBlock(off, "Redstone Repeater", 0.0F, T_REPEATER_OFF, REPEATER_OFF, new double[]{0.0, 0.0, 0.0, 1.0, 0.125, 1.0}, boxesOff);
         Blocks.solid[off] = true;
         Blocks.coll[off] = new double[]{0.0, 0.0, 0.0, 1.0, 0.125, 1.0};

         double[][] boxesOn = Shapes.turnAll(
            new double[][]{
               Shapes.tiles(Shapes.px(0, 0, 0, 16, 2, 16), T_SMOOTH_STONE(), T_REPEATER_ON, T_SMOOTH_STONE()),
               Shapes.local(Shapes.tiles(Shapes.px(7, 2, 2, 9, 5, 4), plank, plank, plank)),
               Shapes.local(Shapes.tiles(Shapes.px(6.75, 5, 1.75, 9.25, 7, 4.25), T_REDSTONE_BLOCK, T_REDSTONE_BLOCK, T_REDSTONE_BLOCK)),
               Shapes.local(Shapes.tiles(Shapes.px(7, 2, 10, 9, 5, 12), plank, plank, plank)),
               Shapes.local(Shapes.tiles(Shapes.px(6.75, 5, 9.75, 9.25, 7, 12.25), T_REDSTONE_BLOCK, T_REDSTONE_BLOCK, T_REDSTONE_BLOCK))
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
      double[][] dlBoxes = {
         Shapes.tiles(Shapes.px(0, 0, 0, 16, 5, 16), plank, plank, plank),
         Shapes.tiles(Shapes.px(1, 5, 1, 15, 6, 15), T_DAYLIGHT_TOP, T_DAYLIGHT_TOP, plank)
      };
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
         int frontFace = 2 + f;
         int backFace = 2 + (f ^ 1);
         cube(obs, "Observer", 3.0F, T_PISTON_SIDE, 1, true);
         for (int face = 0; face < 6; face++) {
            Blocks.tiles[obs][face] = face == frontFace ? T_OBSERVER_FRONT : (face == backFace ? T_PISTON_BOTTOM : T_PISTON_SIDE);
         }
         Blocks.drop[obs] = OBSERVER;

         cube(obsOn, "Observer", 3.0F, T_PISTON_SIDE, 1, true);
         for (int face = 0; face < 6; face++) {
            Blocks.tiles[obsOn][face] = face == frontFace ? T_OBSERVER_FRONT : (face == backFace ? T_PISTON_BOTTOM : T_PISTON_SIDE);
         }
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
      cube(PRISMARINE, "Prismarine", 1.5F, T_PRISMARINE, 1, true);
      cube(PRISMARINE_BRICKS, "Prismarine Bricks", 1.5F, T_PRISMARINE_BRICKS, 1, true);
      cube(DARK_PRISMARINE, "Dark Prismarine", 1.5F, T_DARK_PRISMARINE, 1, true);
      cube(PURPUR_BLOCK, "Purpur Block", 1.5F, T_PURPUR_BLOCK, 1, true);
      cube(PURPUR_PILLAR, "Purpur Pillar", 1.5F, T_PURPUR_PILLAR, 1, true);
      Blocks.faces(PURPUR_PILLAR, T_PURPUR_TOP, T_PURPUR_TOP, T_PURPUR_PILLAR);
      cube(BONE_BLOCK, "Bone Block", 2.0F, T_BONE_SIDE, 1, true);
      Blocks.faces(BONE_BLOCK, T_BONE_TOP, T_BONE_TOP, T_BONE_SIDE);
   }

   private static int T_SMOOTH_STONE() {
      return Blocks.tiles[Blocks.STONE][0];
   }

   /**
    * Quarter-turn UV rotation {@code s.rot} (0..3) for face {@code face} (0..5) of a Redstone319 block {@code id}
    * so directional side and top textures align with the block's facing.
    */
   static int faceRot(int id, int face) {
      if (id >= REPEATER_OFF && id < REPEATER_ON + 4) {
         if (face != 0) {
            return 0;
         }
         int rf = (id - REPEATER_OFF) & 3;
         return rf == 0 ? 0 : (rf == 1 ? 2 : (rf == 2 ? 3 : 1));
      }

      int pf = -1;
      if (id >= PISTON && id < PISTON_HEAD + 6) {
         pf = (id - PISTON) % 6;
      } else if (id >= OBSERVER && id < OBSERVER_ON + 4) {
         pf = 2 + ((id - OBSERVER) & 3);
      }

      if (pf < 0) {
         return 0;
      }
      int frontFace = pf < 2 ? (pf ^ 1) : pf;
      if (face == frontFace || face == (frontFace ^ 1)) {
         return 0;
      }

      switch (pf) {
         case 0: // Down (-Y): top of side tile (v=0) at bottom edge
            return face >= 2 ? 2 : 0;
         case 1: // Up (+Y): top of side tile (v=0) at top edge
            return 0;
         case 2: // North (-Z)
            return face == 4 ? 3 : (face == 5 ? 1 : 0);
         case 3: // South (+Z)
            return face == 4 ? 1 : (face == 5 ? 3 : 2);
         case 4: // East (+X)
            return face == 0 ? 3 : (face == 1 ? 1 : (face == 2 ? 1 : 3));
         case 5: // West (-X)
            return face == 0 ? 1 : (face == 1 ? 3 : (face == 2 ? 3 : 1));
         default:
            return 0;
      }
   }

   private static void registerPistonVariant(int id, String name, int dropId, int f, boolean sticky, boolean ext) {
      int platen = sticky ? T_PISTON_TOP_STICKY : T_PISTON_TOP;
      int plank = Blocks.tiles[Blocks.PLANKS][0];
      int frontFace = f < 2 ? (f ^ 1) : f;
      int backFace = frontFace ^ 1;
      if (!ext) {
         cube(id, name, 0.5F, T_PISTON_SIDE, 1, false);
         Blocks.drop[id] = dropId;
         for (int face = 0; face < 6; face++) {
            Blocks.tiles[id][face] = face == frontFace ? platen : (face == backFace ? T_PISTON_BOTTOM : T_PISTON_SIDE);
         }
      } else {
         double x0 = f == 5 ? 4 : 0;
         double x1 = f == 4 ? 12 : 16;
         double y0 = f == 0 ? 4 : 0;
         double y1 = f == 1 ? 12 : 16;
         double z0 = f == 2 ? 4 : 0;
         double z1 = f == 3 ? 12 : 16;
         double sx0 = f == 4 ? 12 : (f == 5 ? 0 : 6);
         double sx1 = f == 4 ? 16 : (f == 5 ? 4 : 10);
         double sy0 = f == 1 ? 12 : (f == 0 ? 0 : 6);
         double sy1 = f == 1 ? 16 : (f == 0 ? 4 : 10);
         double sz0 = f == 3 ? 12 : (f == 2 ? 0 : 6);
         double sz1 = f == 3 ? 16 : (f == 2 ? 4 : 10);
         double[] bounds = {x0 / 16.0, y0 / 16.0, z0 / 16.0, x1 / 16.0, y1 / 16.0, z1 / 16.0};
         double[][] boxes = {
            Shapes.px(x0, y0, z0, x1, y1, z1),
            Shapes.local(Shapes.tiles(Shapes.px(sx0, sy0, sz0, sx1, sy1, sz1), plank, plank, plank))
         };
         modelBlock(id, name, 0.5F, T_PISTON_SIDE, dropId, bounds, boxes);
         for (int face = 0; face < 6; face++) {
            Blocks.tiles[id][face] = (face == frontFace || face == backFace) ? T_PISTON_BOTTOM : T_PISTON_SIDE;
         }
         Blocks.solid[id] = true;
         Blocks.coll[id] = bounds;
      }
   }

   private static void registerPistonHead(int id, int f) {
      int plank = Blocks.tiles[Blocks.PLANKS][0];
      int frontFace = f < 2 ? (f ^ 1) : f;
      int backFace = frontFace ^ 1;
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
         Shapes.px(x0, y0, z0, x1, y1, z1),
         Shapes.local(Shapes.tiles(Shapes.px(sx0, sy0, sz0, sx1, sy1, sz1), plank, plank, plank))
      };
      double[] full = {0.0, 0.0, 0.0, 1.0, 1.0, 1.0};
      modelBlock(id, "Piston Head", 0.5F, T_PISTON_TOP, 0, full, boxes);
      for (int face = 0; face < 6; face++) {
         Blocks.tiles[id][face] = (face == frontFace || face == backFace) ? T_PISTON_TOP : T_PISTON_SIDE;
      }
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
      boolean upN = upClear && Blocks.solid[n.at(0, 0, -1)] && isWire(n.at(0, 1, -1));
      boolean upS = upClear && Blocks.solid[n.at(0, 0, 1)] && isWire(n.at(0, 1, 1));
      boolean upE = upClear && Blocks.solid[n.at(1, 0, 0)] && isWire(n.at(1, 1, 0));
      boolean upW = upClear && Blocks.solid[n.at(-1, 0, 0)] && isWire(n.at(-1, 1, 0));

      boolean cn = RedstoneLogic.connects(n.at(0, 0, -1), 0) || upN || (!Blocks.opaque[n.at(0, 0, -1)] && isWire(n.at(0, -1, -1)));
      boolean cs = RedstoneLogic.connects(n.at(0, 0, 1), 1) || upS || (!Blocks.opaque[n.at(0, 0, 1)] && isWire(n.at(0, -1, 1)));
      boolean ce = RedstoneLogic.connects(n.at(1, 0, 0), 2) || upE || (!Blocks.opaque[n.at(1, 0, 0)] && isWire(n.at(1, -1, 0)));
      boolean cw = RedstoneLogic.connects(n.at(-1, 0, 0), 3) || upW || (!Blocks.opaque[n.at(-1, 0, 0)] && isWire(n.at(-1, -1, 0)));

      ArrayList<double[]> out = new ArrayList<>(6);
      boolean straightNS = (cn || cs) && !ce && !cw;
      boolean straightEW = (ce || cw) && !cn && !cs;
      if (straightNS) {
         cn=cs=true;
         out.add(Shapes.tint(Shapes.tiles(Shapes.px(6, 0, cn ? 0 : 5, 10, 0.75, cs ? 16 : 11), tile, tile, tile), rgb));
      } else if (straightEW) {
         ce=cw=true;
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
      list.add(RedstoneLogic.COMPARATOR);
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
      list.add(PRISMARINE);
      list.add(PRISMARINE_BRICKS);
      list.add(DARK_PRISMARINE);
      list.add(PURPUR_BLOCK);
      list.add(PURPUR_PILLAR);
      list.add(BONE_BLOCK);
      list.add(GOLDEN_APPLE);
      list.add(GOLDEN_CARROT);
      list.add(COOKIE);
      list.add(MUSHROOM_STEW);
      list.add(BOWL);
      list.add(COMPASS);
      list.add(CLOCK);
      list.add(FIRE_CHARGE);
   }

   static boolean isRedstoneUtility(int id) {
      if (RedstoneLogic.comparator(id)) return true;
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
      Recipes.shaped(RedstoneLogic.COMPARATOR, 1, new String[]{" T ", "TQT", "SSS"}, 'T', REDSTONE_TORCH, 'Q', Items.QUARTZ, 'S', Blocks.STONE);
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
      Recipes.shaped(PRISMARINE, 4, new String[]{"CC", "CD"}, 'C', Blocks.COBBLE, 'D', Items.DYE1 + 8);
      Recipes.shaped(PRISMARINE_BRICKS, 4, new String[]{"PP", "PP"}, 'P', PRISMARINE);
      Recipes.shaped(DARK_PRISMARINE, 8, new String[]{"PPP", "PDP", "PPP"}, 'P', PRISMARINE, 'D', Items.DYE1 + 14);
      Recipes.shaped(PURPUR_BLOCK, 4, new String[]{"EE", "ED"}, 'E', Blocks.END_STONE, 'D', Items.DYE1 + 9);
      Recipes.shaped(PURPUR_PILLAR, 2, new String[]{"P", "P"}, 'P', PURPUR_BLOCK);
      Recipes.shaped(BONE_BLOCK, 1, new String[]{"BBB", "BBB", "BBB"}, 'B', Items.BONE_MEAL);
      Recipes.shapeless(Items.BONE_MEAL, 9, BONE_BLOCK);
      Recipes.shaped(GOLDEN_APPLE, 1, new String[]{"GGG", "GAG", "GGG"}, 'G', Items.GOLD_INGOT, 'A', Items.APPLE);
      Recipes.shaped(GOLDEN_CARROT, 1, new String[]{"NNN", "NCN", "NNN"}, 'N', Items.GOLD_NUGGET, 'C', Items.CARROT);
      Recipes.shaped(COOKIE, 8, new String[]{"WCW"}, 'W', Items.WHEAT, 'C', Items.DYE1 + 11);
      Recipes.shaped(BOWL, 4, new String[]{"P P", " P "}, 'P', Blocks.PLANKS);
      Recipes.shapeless(MUSHROOM_STEW, 1, Blocks313.MUSHROOM_BROWN, Blocks313.MUSHROOM_RED, BOWL);
      Recipes.shaped(COMPASS, 1, new String[]{" I ", "IRI", " I "}, 'I', Items.IRON_INGOT, 'R', ITEM_REDSTONE);
      Recipes.shaped(CLOCK, 1, new String[]{" G ", "GRG", " G "}, 'G', Items.GOLD_INGOT, 'R', ITEM_REDSTONE);
      Recipes.shapeless(FIRE_CHARGE, 3, Items.BLAZE_POWDER, Items.COAL, Items.GUNPOWDER);
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
      return id >= OBSERVER && id < OBSERVER_ON + 4 || id >= OBSERVER_VERTICAL && id <= OBSERVER_VERTICAL_ON + 1;
   }

   static int observerFacing(int id) {
      return id >= OBSERVER_VERTICAL ? 4 + ((id - OBSERVER_VERTICAL) & 1) : (id - OBSERVER) & 3;
   }

   static boolean observerOn(int id) {return id>=OBSERVER_ON&&id<OBSERVER_ON+4||id>=OBSERVER_VERTICAL_ON&&id<=OBSERVER_VERTICAL_ON+1;}
   static int observerId(int facing,boolean on){return facing<4?(on?OBSERVER_ON:OBSERVER)+facing:(on?OBSERVER_VERTICAL_ON:OBSERVER_VERTICAL)+facing-4;}
   static int observerDx(int f){return f<4?HDX[f]:0;}
   static int observerDy(int f){return f==4?-1:f==5?1:0;}
   static int observerDz(int f){return f<4?HDZ[f]:0;}
   static boolean observerFeeds(int id,int sx,int sy,int sz,int x,int y,int z) {
      int f=observerFacing(id);return observerOn(id)&&sx-observerDx(f)==x&&sy-observerDy(f)==y&&sz-observerDz(f)==z;
   }

   static boolean isFloorPart(int id) {
      return RedstoneLogic.comparator(id) || id == REDSTONE_TORCH || id == REDSTONE_TORCH_OFF || id >= LEVER_OFF && id <= WOOD_PLATE_ON || isWire(id) || isRepeater(id);
   }

   static boolean placesOriented(int id) {
      return id == RedstoneLogic.COMPARATOR || id == REPEATER_OFF || id == PISTON || id == STICKY_PISTON || id == OBSERVER;
   }

   static int orientForPlace(Game g, int id, int x, int y, int z) {
      int hf = Shapes.playerFacing(g.player);
      if (id == RedstoneLogic.COMPARATOR) return id + hf;
      if (id == REPEATER_OFF) {
         return REPEATER_OFF + hf;
      }
      if (id == OBSERVER) {
         if(g.player.pitch>45)return observerId(4,false);
         if(g.player.pitch<-45)return observerId(5,false);
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
      if (RedstoneLogic.comparator(id)) return RedstoneLogic.COMPARATOR;
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
      if (RedstoneLogic.comparator(id)) return (id-RedstoneLogic.COMPARATOR)/4;
      if (isWire(id)) {
         return id - REDSTONE_WIRE0;
      }
      if (isRepeaterOn(id) || observerOn(id)) {
         return 15;
      }
      return sourcePower(id);
   }

   /** True if any of the 6 neighbours of (x, y, z) provides Redstone power > 0 into (x, y, z). */
   static boolean isPowered(World w, int x, int y, int z) {
      return RedstoneLogic.input(w,x,y,z,true)>0;
   }


   static boolean use(Game g, int x, int y, int z, int id) {
      if (isRepeater(id)) {
         int setting=((g.world.getState(x,y,z)>>12)+1)&3;
         g.world.setState(x,y,z,id|(setting<<12));
         g.say("Repeater delay: " + (setting+1) + " redstone ticks");
         propagate(g.world,x,y,z); return true;
      }
      if (RedstoneLogic.comparator(id)) {
         int state=g.world.getState(x,y,z)^0x1000;
         g.world.setState(x,y,z,state);
         g.say((state&0x1000)!=0?"Comparator: subtract":"Comparator: compare");
         propagate(g.world,x,y,z); return true;
      }
      if (id == LEVER_OFF || id == LEVER_ON) {
         int next = id == LEVER_OFF ? LEVER_ON : LEVER_OFF;
         g.world.setBlock(x, y, z, next);
         SoundEngine.playClickBlock(g.world, x, y, z, next == LEVER_ON);
         propagate(g.world, x, y, z);
         g.meshNow(x, y, z);
         return true;
      }

      if (id == STONE_BUTTON || id == WOOD_BUTTON) {
         int next = id == STONE_BUTTON ? STONE_BUTTON_ON : WOOD_BUTTON_ON;
         g.world.setBlock(x, y, z, next);
         SoundEngine.playClickBlock(g.world, x, y, z, true);
         g.world.redstone.buttons.put(pack(x, y, z), id == STONE_BUTTON ? 20 : 30);
         propagate(g.world, x, y, z);
         g.meshNow(x, y, z);
         return true;
      }

      if (id == NOTE_BLOCK) {
         long key = pack(x, y, z);
         int note = (g.world.redstone.notes.getOrDefault(key, -1) + 1) % 25;
         g.world.redstone.notes.put(key, note);
         playNote(g, x, y, z);
         return true;
      }

      if (id == DAYLIGHT_SENSOR || id == DAYLIGHT_SENSOR_ON) {
         int state=g.world.getState(x,y,z)^0x1000;
         g.world.setState(x,y,z,state);
         g.say((state&0x1000)!=0?"Daylight sensor: inverted":"Daylight sensor: normal");
         propagate(g.world,x,y,z);g.meshNow(x,y,z);return true;
      }

      return false;
   }



   private static void playNote(Game g, int x, int y, int z) {
      int semitone = g.world.redstone.notes.getOrDefault(pack(x, y, z), 0);
      SoundEngine.playNoteBlock(g != null ? g.world : null, x, y, z, semitone);
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
      if (w != null) RedstoneLogic.changed(w,x,y,z);
   }

   static boolean handlesUpdate(int id) {
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
      if (w == null || w.redstone.updating) {
         return;
      }

      w.redstone.updating = true;
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
         w.redstone.updating = false;
      }
   }

   private static boolean relaxWires(World w, int cx, int cy, int cz, ArrayList<int[]> wires, HashSet<Long> remesh) {
      HashMap<Long, Integer> idxMap = new HashMap<>();
      ArrayDeque<int[]> discover=new ArrayDeque<>();
      for(int dy=-2;dy<=2;dy++)for(int dz=-2;dz<=2;dz++)for(int dx=-2;dx<=2;dx++)
         if(isWire(w.getBlock(cx+dx,cy+dy,cz+dz))) discover.add(new int[]{cx+dx,cy+dy,cz+dz});
      while(!discover.isEmpty()) {
         int[] p=discover.remove();long key=pack(p[0],p[1],p[2]);
         if(idxMap.containsKey(key))continue;
         idxMap.put(key,wires.size());wires.add(p);
         for(int d=0;d<4;d++) {
            int nx=p[0]+HDX[d],nz=p[2]+HDZ[d],ny=p[1],nb=w.getBlock(nx,ny,nz);
            if(isWire(nb))discover.add(new int[]{nx,ny,nz});
            if(Blocks.solid[nb]&&!Blocks.opaque[w.getBlock(p[0],ny+1,p[2])]&&isWire(w.getBlock(nx,ny+1,nz)))discover.add(new int[]{nx,ny+1,nz});
            if(!Blocks.opaque[nb]&&isWire(w.getBlock(nx,ny-1,nz)))discover.add(new int[]{nx,ny-1,nz});
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
         int maxSrc = RedstoneLogic.input(w,p[0],p[1],p[2],false);
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
            if (upClear && Blocks.solid[w.getBlock(nx,ay,nz)] && ay + 1 <= 254) {
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
               if (id==REDSTONE_TORCH || id==REDSTONE_TORCH_OFF || isRepeater(id) || RedstoneLogic.comparator(id)) {
                  RedstoneLogic.evaluate(w,x,y,z,id);
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
                  RedstoneLogic.evaluate(w,x,y,z,id);
               } else if (Shapes.isTrap(id)) {
                  boolean open = Shapes.trapOpen(id);
                  if (pow != open) {
                     w.setBlock(x, y, z, Shapes.toggled(id));
                     SoundEngine.play3D(w, pow ? "door_open" : "door_close", x + 0.5, y + 0.5, z + 0.5, 0.8F, 1.0F);
                     remesh.add(key);
                  }
               } else if (Shapes.isGate(id)) {
                  boolean open = Shapes.gateOpen(id);
                  if (pow != open) {
                     w.setBlock(x, y, z, Shapes.toggled(id));
                     SoundEngine.play3D(w, pow ? "gate_open" : "gate_close", x + 0.5, y + 0.5, z + 0.5, 0.85F, 1.0F);
                     remesh.add(key);
                  }
               } else if (Blocks.isDoorLo(id)) {
                  boolean open = Blocks.doorOpen(id);
                  boolean iron = Blocks.mineClass[id] == Items.C_PICK;
                  int hi = w.getBlock(x, y + 1, z);
                  if (Blocks.isDoorHi(hi)) {
                     if (pow) {
                        w.redstone.opened.add(key);
                        if (!open) {
                           w.setBlock(x, y, z, id + 4);
                           w.setBlock(x, y + 1, z, hi + 4);
                           SoundEngine.play3D(w, iron ? "iron_door_open" : "door_open", x + 0.5, y + 1.0, z + 0.5, 0.85F, 1.0F);
                           remesh.add(key);
                           remesh.add(pack(x, y + 1, z));
                        }
                     } else if (open && w.redstone.opened.remove(key)) {
                        w.setBlock(x, y, z, id - 4);
                        w.setBlock(x, y + 1, z, hi - 4);
                        SoundEngine.play3D(w, iron ? "iron_door_close" : "door_close", x + 0.5, y + 1.0, z + 0.5, 0.85F, 1.0F);
                        remesh.add(key);
                        remesh.add(pack(x, y + 1, z));
                     }
                  }
               } else if (id == NOTE_BLOCK) {
                  if (pow && w.redstone.opened.add(key)) {
                     playNote(w.game, x, y, z);
                  } else if (!pow) {
                     w.redstone.opened.remove(key);
                  }
               } else if (id == Blocks.TNT && pow) {
                  w.setBlock(x, y, z, 0);
                  w.primeTnt(x, y, z, 80);
                  SoundEngine.play3D(w, "tnt_ignite", x + 0.5, y + 0.5, z + 0.5, 0.9F, 1.0F);
                  remesh.add(key);
               }
            }
         }
      }
      return changed;
   }

   private static boolean isSupportPowered(World w, int x, int y, int z) {
      return RedstoneLogic.input(w,x,y,z,true)>0;
   }

   private static boolean isPistonPowered(World w, int x, int y, int z, int facing) {
      for(int[] d:RedstoneLogic.SIX) {
         if(d[0]==FDX[facing]&&d[1]==FDY[facing]&&d[2]==FDZ[facing])continue;
         if(RedstoneLogic.signal(w,x+d[0],y+d[1],z+d[2],x,y,z,true)>0)return true;
      }
      // Java quasi-connectivity: sampled on a block update, not a global piston poll.
      return RedstoneLogic.input(w,x,y+1,z,true)>0;
   }


   private static boolean canPistonPush(int id, int y) {
      if (id == 0 || Blocks.replaceable[id]) {
         return true;
      }
      if (y <= 0
         || y >= 255
         || RasterComputer.handles(id)
         || id == Blocks.BEDROCK
         || id == Blocks.OBSIDIAN
         || id == Blocks.FURNACE
         || id == Blocks.FURNACE_LIT
         || id == Blocks.CHEST
         || id == Blocks.CHEST_LOOT
         || id == Blocks.SPAWNER
         || Blocks.isPortal(id)
         || id == Blocks.END_PORTAL
         || id == Blocks.END_FRAME
         || id == Blocks.END_FRAME_EYE
         || Blocks.isDoor(id)
         || Decor315.isBed(id)) {
         return false;
      }
      return !isPistonExtended(id) && !(id >= PISTON_HEAD && id < PISTON_HEAD + 6);
   }

   private static boolean extendPiston(World w, int x, int y, int z, int f, boolean sticky, HashSet<Long> remesh) {
      int dx = FDX[f];
      int dy = FDY[f];
      int dz = FDZ[f];
      PistonPlan plan=new PistonPlan(w,x,y,z,dx,dy,dz,false);
      if(!plan.collect(x+dx,y+dy,z+dz))return false;
      plan.apply(remesh);
      w.redstone.extendedAt.put(pack(x,y,z),w.tickCount);

      int hx = x + dx;
      int hy = y + dy;
      int hz = z + dz;
      w.setBlock(hx, hy, hz, PISTON_HEAD + f);
      w.setBlock(x, y, z, (sticky ? STICKY_PISTON_EXT : PISTON_EXT) + f);
      SoundEngine.play3D(w, "place_metal_1", x + 0.5, y + 0.5, z + 0.5, 0.78F, 0.78F);
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
      SoundEngine.play3D(w, "place_metal_1", x + 0.5, y + 0.5, z + 0.5, 0.72F, 0.68F);
      remesh.add(pack(x, y, z));
      if (w.getBlock(hx, hy, hz) == PISTON_HEAD + f) {
         w.setBlock(hx, hy, hz, 0);
         remesh.add(pack(hx, hy, hz));
      }
      long held=w.tickCount-w.redstone.extendedAt.getOrDefault(pack(x,y,z),w.tickCount-3);
      w.redstone.extendedAt.remove(pack(x,y,z));
      if (sticky && held>1) {
         int px=x+dx*2,py=y+dy*2,pz=z+dz*2,id=w.getBlock(px,py,pz);
         if(id!=0&&Blocks.solid[id]&&canPistonPush(id,py)) {
            PistonPlan plan=new PistonPlan(w,x,y,z,-dx,-dy,-dz,true);
            if(plan.collect(px,py,pz))plan.apply(remesh);
         }
      }
      return true;
   }

   /** Validate the entire slime-connected assembly before moving anything. Twelve blocks total, not per branch. */
   private static final class PistonPlan {
      final World w;final int bx,by,bz,dx,dy,dz;final boolean pulling;
      final java.util.LinkedHashMap<Long,Integer> blocks=new java.util.LinkedHashMap<>();
      final java.util.HashSet<Long> broken=new java.util.HashSet<>();
      PistonPlan(World w,int x,int y,int z,int dx,int dy,int dz,boolean pulling){this.w=w;bx=x;by=y;bz=z;this.dx=dx;this.dy=dy;this.dz=dz;this.pulling=pulling;}
      boolean collect(int x,int y,int z) {
         long key=pack(x,y,z);
         if(blocks.containsKey(key))return true;
         if(x==bx&&y==by&&z==bz)return false;
         if(y<1||y>254||!w.isLoaded(x,z))return false;
         int id=w.getBlock(x,y,z);
         if(id==0||Blocks.isFluid(id))return true;
         if(Blocks.replaceable[id]||(!Blocks.solid[id]&&canPistonPush(id,y))) {if(!pulling)broken.add(key);return !pulling;}
         if(!canPistonPush(id,y)||blocks.size()>=12)return false;
         blocks.put(key,w.getState(x,y,z));
         if(!collect(x+dx,y+dy,z+dz))return false;
         if(id==SLIME_BLOCK)for(int[] d:RedstoneLogic.SIX) {
            int nx=x+d[0],ny=y+d[1],nz=z+d[2],other=w.getBlock(nx,ny,nz);
            if(nx==bx&&ny==by&&nz==bz)continue;
            if(other>=Blocks.GLAZED0&&other<Blocks.GLAZED0+16)continue;
            if(other!=0&&Blocks.solid[other]&&canPistonPush(other,ny)&&!collect(nx,ny,nz))return false;
         }
         return true;
      }
      void apply(HashSet<Long> remesh) {
         for(long k:broken) {
            int x=unpackX(k),y=unpackY(k),z=unpackZ(k),id=w.getBlock(x,y,z);
            if(Blocks.drop[id]>0)w.dropItem(x+.5,y+.3,z+.5,Blocks.drop[id],1);
            w.setBlock(x,y,z,0);remesh.add(k);
         }
         for(long k:blocks.keySet()){w.setBlock(unpackX(k),unpackY(k),unpackZ(k),0);remesh.add(k);}
         for(java.util.Map.Entry<Long,Integer> e:blocks.entrySet()) {
            long k=e.getKey();int x=unpackX(k)+dx,y=unpackY(k)+dy,z=unpackZ(k)+dz;
            w.setState(x,y,z,e.getValue());remesh.add(pack(x,y,z));
         }
      }
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
   static void recover(World w,int x,int y,int z,int id) {
      if(id==STONE_BUTTON_ON||id==WOOD_BUTTON_ON)w.redstone.buttons.put(pack(x,y,z),id==STONE_BUTTON_ON?20:30);
      if(id==STONE_PLATE_ON||id==WOOD_PLATE_ON)w.redstone.plates.add(pack(x,y,z));
   }

   static void tick(Game g) {
      if (g == null || g.world == null || g.player == null) {
         return;
      }

      World w = g.world;
      // 1. Button & Observer auto-release timers
      if (!w.redstone.buttons.isEmpty()) {
         // Resetting a watched observer can arm another one. Iterate a snapshot
         // so a vertical chain cannot mutate the timer iterator or lose a new pulse.
         for(Map.Entry<Long,Integer> e:new HashMap<>(w.redstone.buttons).entrySet()) {
            if(!e.getValue().equals(w.redstone.buttons.get(e.getKey())))continue;
            int rem = e.getValue() - 1;
            if (rem <= 0) {
               long k = e.getKey();
               w.redstone.buttons.remove(k);
               int x = unpackX(k);
               int y = unpackY(k);
               int z = unpackZ(k);
               int id = w.getBlock(x, y, z);
               if (id == STONE_BUTTON_ON || id == WOOD_BUTTON_ON) {
                  w.setBlock(x, y, z, id == STONE_BUTTON_ON ? STONE_BUTTON : WOOD_BUTTON);
                  SoundEngine.playClickBlock(w, x, y, z, false);
                  propagate(w, x, y, z);
                  g.meshNow(x, y, z);
               } else if (observerOn(id)) {
                  int f = observerFacing(id);
                  w.setBlock(x, y, z, observerId(f,false));
                  propagate(w, x-observerDx(f), y-observerDy(f), z-observerDz(f));
                  g.meshNow(x, y, z);
               }
            } else {
               w.redstone.buttons.put(e.getKey(),rem);
            }
         }
      }

      // 2. Re-check any currently depressed pressure plates so they release even if the player sprinted/teleported/died off them!
      if (!w.redstone.plates.isEmpty()) {
         for (Long k : w.redstone.plates.toArray(new Long[0])) {
            if (k != null) {
               int x = unpackX(k);
               int y = unpackY(k);
               int z = unpackZ(k);
               int id = w.getBlock(x, y, z);
               if (id != STONE_PLATE_ON && id != WOOD_PLATE_ON) {
                  w.redstone.plates.remove(k);
               } else {
                  checkPlateAt(g, w, x, y, z);
               }
            }
         }
      }

      // 3. Pressure plates and daylight sensors around the player, mobs, and dropped items
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
                  checkPlateAt(g, w, x, y, z);
               }
            }
         }
      }
      if (g.mobs != null) {
         for (int i = 0; i < g.mobs.list.size(); i++) {
            Mobs.Mob m = g.mobs.list.get(i);
            if (m != null && !m.dead) {
               int mx = (int)Math.floor(m.x);
               int my = (int)Math.floor(m.y + 0.05);
               int mz = (int)Math.floor(m.z);
               if (w.getBlock(mx, my, mz) >= STONE_PLATE && w.getBlock(mx, my, mz) <= WOOD_PLATE_ON) {
                  checkPlateAt(g, w, mx, my, mz);
               }
            }
         }
      }
      if (w.items != null) {
         for (int i = 0; i < w.items.size(); i++) {
            World.Item it = w.items.get(i);
            if (it != null && !it.dead) {
               int ix = (int)Math.floor(it.x);
               int iy = (int)Math.floor(it.y + 0.05);
               int iz = (int)Math.floor(it.z);
               int bid = w.getBlock(ix, iy, iz);
               if (bid == WOOD_PLATE || bid == WOOD_PLATE_ON) {
                  checkPlateAt(g, w, ix, iy, iz);
               }
            }
         }
      }
   }

   private static void checkPlateAt(Game g, World w, int x, int y, int z) {
      int id = w.getBlock(x, y, z);
      if (id < STONE_PLATE || id > WOOD_PLATE_ON) {
         w.redstone.plates.remove(pack(x, y, z));
         return;
      }
      boolean isWood = id == WOOD_PLATE || id == WOOD_PLATE_ON;
      boolean stood = entityOnPlate(g, x, y, z, isWood);
      boolean wasOn = id == STONE_PLATE_ON || id == WOOD_PLATE_ON;
      long key = pack(x, y, z);
      if (stood) {
         w.redstone.plates.add(key);
      } else {
         w.redstone.plates.remove(key);
      }
      if (stood != wasOn) {
         int next = isWood ? (stood ? WOOD_PLATE_ON : WOOD_PLATE) : (stood ? STONE_PLATE_ON : STONE_PLATE);
         w.setBlock(x, y, z, next);
         SoundEngine.playClickBlock(w, x, y, z, stood);
         propagate(w, x, y, z);
         g.meshNow(x, y, z);
      }
   }

   private static boolean entityOnPlate(Game g, int x, int y, int z, boolean isWood) {
      if (!g.player.dead && !g.player.spectator && Math.abs(g.player.x - (x + 0.5)) < 0.7 && Math.abs(g.player.z - (z + 0.5)) < 0.7 && Math.abs(g.player.y - y) < 0.8) {
         return true;
      }

      if (g.mobs != null) {
         for (Mobs.Mob m : g.mobs.list) {
            if (!m.dead && Math.abs(m.x - (x + 0.5)) < 0.7 && Math.abs(m.z - (z + 0.5)) < 0.7 && Math.abs(m.y - y) < 0.8) {
               return true;
            }
         }
      }

      if (isWood && g.world != null && g.world.items != null) {
         for (World.Item it : g.world.items) {
            if (it != null && !it.dead && Math.abs(it.x - (x + 0.5)) < 0.7 && Math.abs(it.z - (z + 0.5)) < 0.7 && Math.abs(it.y - y) < 0.8) {
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
      Tex.blit(T_GOLDEN_APPLE, goldenAppleItem());
      Tex.blit(T_GOLDEN_CARROT, goldenCarrotItem());
      Tex.blit(T_COOKIE, cookieItem());
      Tex.blit(T_MUSHROOM_STEW, mushroomStewItem());
      Tex.blit(T_BOWL, bowlItem());
      Tex.blit(T_COMPASS, compassItem());
      Tex.blit(T_CLOCK, clockItem());
      Tex.blit(T_FIRE_CHARGE, fireChargeItem());
      Tex.blit(T_PRISMARINE, prismarineTile());
      Tex.blit(T_PRISMARINE_BRICKS, prismarineBricksTile());
      Tex.blit(T_DARK_PRISMARINE, darkPrismarineTile());
      Tex.blit(T_PURPUR_BLOCK, purpurBlockTile());
      Tex.blit(T_PURPUR_TOP, purpurTopTile());
      Tex.blit(T_PURPUR_PILLAR, purpurPillarTile());
      Tex.blit(T_BONE_TOP, boneTopTile());
      Tex.blit(T_BONE_SIDE, boneSideTile());
      Tex.blit(T_PISTOL, pistolItem());
      Tex.blit(T_MACHINE_GUN, machineGunItem());
      Tex.blit(T_SNIPER_RIFLE, sniperRifleItem());
      Tex.blit(T_TNT_GRENADE, tntGrenadeItem());
      Tex.blit(T_MURDER_KNIFE, murderKnifeItem());
      Tex.blit(T_C4_BOMB, c4BombItem());
      Tex.blit(T_SIEGE_CANNON, siegeCannonItem());
      Tex.blit(T_MORTAR_STRIKE, mortarStrikeItem());
   }

   static int[] sprite(int id) {
      return switch (id) {
         case ITEM_REDSTONE -> redstoneDustItem();
         case GOLDEN_APPLE -> goldenAppleItem();
         case GOLDEN_CARROT -> goldenCarrotItem();
         case COOKIE -> cookieItem();
         case MUSHROOM_STEW -> mushroomStewItem();
         case BOWL -> bowlItem();
         case COMPASS -> compassItem();
         case CLOCK -> clockItem();
         case FIRE_CHARGE -> fireChargeItem();
         case PISTOL -> pistolItem();
         case MACHINE_GUN -> machineGunItem();
         case SNIPER_RIFLE -> sniperRifleItem();
         case TNT_GRENADE -> tntGrenadeItem();
         case MURDER_KNIFE -> murderKnifeItem();
         case C4_BOMB -> c4BombItem();
         case SIEGE_CANNON -> siegeCannonItem();
         case MORTAR_STRIKE -> mortarStrikeItem();
         default -> null;
      };
   }

   private static int[] lamp(boolean lit) {
      int[] p = new int[256];
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            float n = TexPaint.fbm(x, y, lit ? 645 : 644);
            boolean border = x == 0 || y == 0 || x == 15 || y == 15;
            boolean innerRim = x == 1 || y == 1 || x == 14 || y == 14;
            boolean cornerBrace = (x <= 3 || x >= 12) && (y <= 3 || y >= 12) && (Math.abs(x - y) <= 1 || Math.abs(x + y - 15) <= 1);
            boolean cross = (x == 7 || x == 8) && (y == 2 || y == 13) || (y == 7 || y == 8) && (x == 2 || x == 13);
            if (border) {
               p[y * 16 + x] = TexPaint.mul(TexPaint.c(0x2E1E14), 0.92F + n * 0.16F);
            } else if (innerRim || cornerBrace || cross) {
               p[y * 16 + x] = TexPaint.mul(TexPaint.c(0x4E3422), 0.92F + n * 0.16F);
            } else {
               float d = (float)Math.hypot(x - 7.5F, y - 7.5F) / 7.5F + (n - 0.5F) * 0.16F;
               if (lit) {
                  int col = d < 0.32F ? 0xFFFCE0 : (d < 0.58F ? 0xFFE47A : (d < 0.82F ? 0xF2AE38 : 0xC8741E));
                  p[y * 16 + x] = TexPaint.c(col);
               } else {
                  int col = d < 0.38F ? 0x8C5A30 : (d < 0.72F ? 0x6E4422 : 0x543218);
                  p[y * 16 + x] = TexPaint.c(col);
               }
            }
         }
      }

      return p;
   }

   private static int[] redstoneTorch(boolean lit) {
      int[] p = TexPaint319.torch();
      p[6 * 16 + 7] = TexPaint.c(lit ? 0x9E140C : 0x42100C);
      p[6 * 16 + 8] = TexPaint.c(lit ? 0x861008 : 0x380C08);
      p[5 * 16 + 7] = TexPaint.c(lit ? 0xE22418 : 0x581410);
      p[5 * 16 + 8] = TexPaint.c(lit ? 0xC41810 : 0x46100C);
      p[4 * 16 + 7] = TexPaint.c(lit ? 0xFFB4A2 : 0x7A221C);
      p[4 * 16 + 8] = TexPaint.c(lit ? 0xFF5240 : 0x621812);
      p[3 * 16 + 7] = TexPaint.c(lit ? 0xFF6858 : 0x681A14);
      p[3 * 16 + 8] = TexPaint.c(lit ? 0xDC2216 : 0x52120E);
      p[2 * 16 + 7] = TexPaint.c(lit ? 0xB8140C : 0x3E0C08);
      p[2 * 16 + 8] = TexPaint.c(lit ? 0x961008 : 0x320806);
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
      int[] p = new int[256];
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            float n = TexPaint.fbm(x, y, 663);
            boolean outer = x == 0 || y == 0 || x == 15 || y == 15;
            boolean bevelHi = (x == 1 && y >= 1 && y <= 14) || (y == 1 && x >= 1 && x <= 14);
            boolean bevelLo = (x == 14 && y >= 1 && y <= 14) || (y == 14 && x >= 1 && x <= 14);
            int base = outer ? 0x727276 : (bevelHi ? 0xB4B4B8 : (bevelLo ? 0x848488 : 0x9C9CA0));
            p[y * 16 + x] = TexPaint.mul(TexPaint.c(base), 0.95F + n * 0.10F);
         }
      }
      // Recessed delay slot & redstone circuit trace
      int slot = TexPaint.c(0x46464A);
      for (int y = 8; y <= 13; y++) {
         p[y * 16 + 7] = slot;
         p[y * 16 + 8] = slot;
      }
      int trace = TexPaint.c(lit ? 0xF43022 : 0x621410);
      int traceHi = TexPaint.c(lit ? 0xFF7868 : 0x821E18);
      for (int y = 2; y <= 13; y++) {
         p[y * 16 + 7] = y <= 7 ? traceHi : trace;
         p[y * 16 + 8] = trace;
      }
      // Directional triangle / arrowhead pointing north (-Z / v=0)
      p[3 * 16 + 6] = p[3 * 16 + 9] = trace;
      p[4 * 16 + 5] = p[4 * 16 + 6] = p[4 * 16 + 9] = p[4 * 16 + 10] = trace;
      p[5 * 16 + 4] = p[5 * 16 + 5] = p[5 * 16 + 10] = p[5 * 16 + 11] = trace;
      return p;
   }

   private static int[] pistonSide() {
      int[] plank = TexPaint.oakPlanks();
      int[] cobble = TexPaint.cobble();
      int[] p = new int[256];
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            if (y < 4) {
               int c = plank[y * 16 + x];
               if (y == 0) {
                  c = TexPaint.mul(c, 1.10F);
               } else if (y == 3) {
                  c = TexPaint.mul(c, 0.78F);
               }
               if ((x == 2 || x == 13) && y >= 1 && y <= 2) {
                  c = TexPaint.c(0x585A60); // iron corner rivets on wood platen rim
               }
               p[y * 16 + x] = c;
            } else if (y == 4) {
               p[y * 16 + x] = TexPaint.c(0x3E3E44); // dark seam under platen
            } else {
               int c = cobble[y * 16 + x];
               if (x == 0 || x == 15 || y == 15) {
                  c = TexPaint.mul(c, 0.82F);
               } else if (x >= 6 && x <= 9) {
                  c = TexPaint.mul(c, 0.90F); // central iron guide channel
               }
               p[y * 16 + x] = c;
            }
         }
      }
      return p;
   }

   private static int[] pistonTop(boolean sticky) {
      int[] plank = TexPaint.oakPlanks();
      int[] p = new int[256];
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            boolean outer = x == 0 || y == 0 || x == 15 || y == 15;
            boolean bracket = (x <= 2 || x >= 13) && (y <= 2 || y >= 13);
            boolean slime = sticky && x >= 3 && x <= 12 && y >= 3 && y <= 12;
            if (bracket) {
               p[y * 16 + x] = (x == 1 || x == 14) && (y == 1 || y == 14) ? TexPaint.c(0x989CA2) : TexPaint.c(0x5A5C62);
            } else if (slime) {
               float n = TexPaint.fbm(x, y, 667);
               boolean sRim = x == 3 || x == 12 || y == 3 || y == 12;
               int base = sRim ? 0x4E9438 : ((x >= 5 && x <= 10 && y >= 5 && y <= 10) ? 0x7AD45E : 0x64B84A);
               p[y * 16 + x] = TexPaint.mul(TexPaint.c(base), 0.92F + n * 0.16F);
            } else {
               int c = plank[y * 16 + x];
               p[y * 16 + x] = outer ? TexPaint.mul(c, 0.80F) : c;
            }
         }
      }
      return p;
   }

   private static int[] pistonBottom() {
      int[] cobble = TexPaint.cobble();
      int[] p = new int[256];
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            boolean outer = x == 0 || y == 0 || x == 15 || y == 15;
            boolean ring = x >= 5 && x <= 10 && y >= 5 && y <= 10;
            boolean port = x >= 6 && x <= 9 && y >= 6 && y <= 9;
            if (port) {
               p[y * 16 + x] = (x >= 7 && x <= 8 && y >= 7 && y <= 8) ? TexPaint.c(0xC82218) : TexPaint.c(0x52120E);
            } else if (ring) {
               p[y * 16 + x] = TexPaint.c(0x48484E);
            } else {
               p[y * 16 + x] = outer ? TexPaint.mul(cobble[y * 16 + x], 0.78F) : cobble[y * 16 + x];
            }
         }
      }
      return p;
   }

   private static int[] daylightTop() {
      int[] plank = TexPaint.oakPlanks();
      int[] p = new int[256];
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            boolean border = x < 2 || y < 2 || x > 13 || y > 13;
            boolean mullion = x == 5 || x == 10 || y == 5 || y == 10;
            if (border) {
               p[y * 16 + x] = (x == 0 || y == 0 || x == 15 || y == 15) ? TexPaint.mul(plank[y * 16 + x], 0.78F) : plank[y * 16 + x];
            } else if (mullion) {
               p[y * 16 + x] = TexPaint.c(0x5A4028);
            } else {
               int cx = (x - 2) % 5;
               int cy = (y - 2) % 5;
               int col = (cx == 0 || cy == 0) ? 0xEBE6DA : ((cx == 1 && cy == 1) ? 0xFFFDF6 : 0xD6CEBE);
               p[y * 16 + x] = TexPaint.c(col);
            }
         }
      }
      return p;
   }

   private static int[] noteBlockTile() {
      int[] p = new int[256];
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            float n = TexPaint.fbm(x, y, 670);
            boolean border = x == 0 || y == 0 || x == 15 || y == 15;
            boolean innerRim = x == 1 || y == 1 || x == 14 || y == 14;
            int d = Math.abs(x - 7) + Math.abs(y - 7);
            int base;
            if (border) {
               base = 0x362010;
            } else if (innerRim) {
               base = 0x52321B;
            } else if (d <= 2) {
               base = 0x26160B;
            } else if (((x + y) & 1) == 0) {
               base = 0x7A4E2D;
            } else {
               base = 0x5E3A20;
            }
            p[y * 16 + x] = TexPaint.mul(TexPaint.c(base), 0.93F + n * 0.14F);
         }
      }
      return p;
   }

   private static int[] observerFront() {
      int[] cobble = TexPaint.cobble();
      int[] p = new int[256];
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            boolean outer = x == 0 || y == 0 || x == 15 || y == 15;
            boolean inner = x == 1 || y == 1 || x == 14 || y == 14;
            p[y * 16 + x] = outer ? TexPaint.c(0x38383E) : (inner ? TexPaint.mul(cobble[y * 16 + x], 0.85F) : cobble[y * 16 + x]);
         }
      }
      // Recessed stone brow & eyes
      for (int x = 3; x <= 12; x++) {
         p[4 * 16 + x] = TexPaint.c(0x303036);
         p[5 * 16 + x] = TexPaint.c(0x46464E);
      }
      for (int y = 6; y <= 7; y++) {
         for (int x = 3; x <= 6; x++) {
            p[y * 16 + x] = TexPaint.c(0x1E1E22);
            p[y * 16 + (15 - x)] = TexPaint.c(0x1E1E22);
         }
      }
      p[6 * 16 + 4] = p[6 * 16 + 11] = TexPaint.c(0xF0F0F4);
      p[7 * 16 + 4] = p[7 * 16 + 11] = TexPaint.c(0xC8C8D0);
      // Observer nose/mouth sensor slit
      for (int y = 9; y <= 12; y++) {
         p[y * 16 + 7] = p[y * 16 + 8] = TexPaint.c(y >= 10 && y <= 11 ? 0xD82418 : 0x3A1614);
      }
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
            float n = TexPaint.fbm(x, y, 658);
            boolean core = x >= 3 && x <= 12 && y >= 3 && y <= 12;
            boolean innerCore = x >= 5 && x <= 10 && y >= 5 && y <= 10;
            boolean border = x == 0 || y == 0 || x == 15 || y == 15;
            boolean shine = (x == 2 && y >= 2 && y <= 6) || (y == 2 && x >= 2 && x <= 6);
            int base = border ? 0x4A9236 : (shine ? 0x9EE884 : (innerCore ? 0x58AC42 : (core ? 0x64B84C : 0x7ED464)));
            p[y * 16 + x] = TexPaint.mul(TexPaint.c(base), 0.94F + n * 0.12F);
         }
      }

      return p;
   }

   private static int[] seaLantern() {
      int[] p = new int[256];
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            float n = TexPaint.fbm(x, y, 659);
            int d = Math.min(Math.min(x, 15 - x), Math.min(y, 15 - y));
            boolean diag = Math.abs(x - y) == 0 || Math.abs(x + y - 15) == 0;
            int base = d == 0 ? 0x3E746E : (d == 1 ? (diag ? 0x5A968E : 0x7CBAB2) : (d == 2 ? 0xB8ECE4 : (d == 3 ? 0xDCFBF6 : 0xF4FFFD)));
            p[y * 16 + x] = TexPaint.mul(TexPaint.c(base), 0.96F + n * 0.08F);
         }
      }

      return p;
   }

   private static int[] prismarineTile() {
      int[] p = new int[256];
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            float n = TexPaint.fbm(x, y, 680);
            boolean crack = ((x * 5 + y * 3) % 7 == 0) || ((x + y * 2) % 9 == 0);
            int base = crack ? 0x3D6E64 : (n > 0.62F ? 0x7ABBB0 : (n > 0.35F ? 0x5FA094 : 0x4E887D));
            p[y * 16 + x] = TexPaint.mul(TexPaint.c(base), 0.94F + n * 0.12F);
         }
      }

      return p;
   }

   private static int[] prismarineBricksTile() {
      int[] p = new int[256];
      for (int y = 0; y < 16; y++) {
         int row = y / 4;
         int off = (row & 1) * 4;
         boolean hJoint = (y & 3) == 0;
         for (int x = 0; x < 16; x++) {
            float n = TexPaint.fbm(x, y, 681);
            boolean vJoint = ((x + off) & 7) == 0;
            boolean hi = (y & 3) == 1 && !vJoint;
            int base = (hJoint || vJoint) ? 0x38645B : (hi ? 0x7EC0B4 : 0x5E9F93);
            p[y * 16 + x] = TexPaint.mul(TexPaint.c(base), 0.94F + n * 0.12F);
         }
      }

      return p;
   }

   private static int[] darkPrismarineTile() {
      int[] p = new int[256];
      for (int y = 0; y < 16; y++) {
         boolean hGrid = (y & 3) == 0;
         for (int x = 0; x < 16; x++) {
            float n = TexPaint.fbm(x, y, 682);
            boolean vGrid = (x & 3) == 0;
            boolean hi = (x & 3) == 1 && (y & 3) == 1;
            int base = (hGrid || vGrid) ? 0x1F3B34 : (hi ? 0x436E63 : 0x2F544B);
            p[y * 16 + x] = TexPaint.mul(TexPaint.c(base), 0.94F + n * 0.12F);
         }
      }

      return p;
   }

   private static int[] purpurBlockTile() {
      int[] p = new int[256];
      for (int y = 0; y < 16; y++) {
         boolean hSeam = (y & 7) == 0;
         for (int x = 0; x < 16; x++) {
            float n = TexPaint.fbm(x, y, 683);
            boolean vSeam = (x & 7) == 0;
            boolean hi = (x & 7) == 1 || (y & 7) == 1;
            int base = (hSeam || vSeam) ? 0x7E567E : (hi ? 0xBC92BC : 0xA67AA6);
            p[y * 16 + x] = TexPaint.mul(TexPaint.c(base), 0.95F + n * 0.10F);
         }
      }

      return p;
   }

   private static int[] purpurTopTile() {
      int[] p = new int[256];
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            float n = TexPaint.fbm(x, y, 684);
            int d = Math.max(Math.abs(x - 7), Math.abs(y - 7));
            int base = (d == 7 || d == 4) ? 0x7A527A : (d == 6 || d == 3 ? 0xBC92BC : 0xA478A4);
            p[y * 16 + x] = TexPaint.mul(TexPaint.c(base), 0.95F + n * 0.10F);
         }
      }

      return p;
   }

   private static int[] purpurPillarTile() {
      int[] p = new int[256];
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            float n = TexPaint.fbm(x, y, 685);
            boolean groove = (x & 3) == 0;
            boolean ridge = (x & 3) == 2;
            int base = (y == 0 || y == 15 || groove) ? 0x7A527A : (ridge ? 0xBC92BC : 0xA478A4);
            p[y * 16 + x] = TexPaint.mul(TexPaint.c(base), 0.95F + n * 0.10F);
         }
      }

      return p;
   }

   private static int[] boneTopTile() {
      int[] p = new int[256];
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            float n = TexPaint.fbm(x, y, 686);
            int d = Math.max(Math.abs(x - 7), Math.abs(y - 7));
            int base = d >= 7 ? 0xB8B29E : (d == 5 || d == 2 ? 0xCEC8B4 : 0xE5E1D0);
            p[y * 16 + x] = TexPaint.mul(TexPaint.c(base), 0.96F + n * 0.08F);
         }
      }

      return p;
   }

   private static int[] boneSideTile() {
      int[] p = new int[256];
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            float n = TexPaint.fbm(x, y, 687);
            boolean seam = x == 0 || x == 15 || x == 5 || x == 10;
            boolean cap = y == 0 || y == 15;
            int base = (seam || cap) ? 0xC2BCAA : ((x == 2 || x == 7 || x == 12) ? 0xEDEAE0 : 0xDEDAC8);
            p[y * 16 + x] = TexPaint.mul(TexPaint.c(base), 0.96F + n * 0.08F);
         }
      }

      return p;
   }

   private static int[] goldenAppleItem() {
      TexExtra.Cv cv = new TexExtra.Cv();
      cv.line(8, 2, 9, 4, 0x6B4423);
      cv.rect(9, 2, 2, 2, 0x48962A);
      cv.ell(7.5, 9.0, 4.8, 4.6, 0xD89614);
      cv.ell(7.5, 8.8, 4.0, 3.8, 0xF7C622);
      cv.ell(6.2, 7.5, 1.8, 1.6, 0xFFF394);
      cv.set(5, 6, 0xFFFFFF);
      cv.shade();
      return cv.p;
   }

   private static int[] goldenCarrotItem() {
      TexExtra.Cv cv = new TexExtra.Cv();
      cv.line(11, 3, 13, 2, 0x48962A);
      cv.line(11, 4, 14, 4, 0x58B436);
      cv.line(10, 3, 10, 1, 0x58B436);
      for (int i = 0; i < 9; i++) {
         int r = Math.max(1, 3 - i / 3);
         cv.ell(10 - i * 0.8, 5 + i * 0.8, r, r, i % 2 == 0 ? 0xF5C220 : 0xD89412);
      }
      cv.set(9, 5, 0xFFF5A0);
      cv.set(7, 7, 0xFFF5A0);
      cv.shade();
      return cv.p;
   }

   private static int[] cookieItem() {
      TexExtra.Cv cv = new TexExtra.Cv();
      cv.ell(7.5, 8.0, 5.2, 4.8, 0xB87333);
      cv.ell(7.5, 7.8, 4.2, 3.8, 0xD28A47);
      int chip = 0x4A2511;
      cv.set(6, 6, chip);
      cv.set(9, 6, chip);
      cv.set(5, 9, chip);
      cv.set(8, 8, chip);
      cv.set(10, 9, chip);
      cv.set(7, 10, chip);
      cv.shade();
      return cv.p;
   }

   private static int[] bowlItem() {
      TexExtra.Cv cv = new TexExtra.Cv();
      cv.ell(7.5, 9.5, 5.5, 3.6, 0x7A5230);
      cv.ell(7.5, 8.5, 5.2, 2.5, 0x9C6B3E);
      cv.ell(7.5, 7.8, 4.2, 1.6, 0x56381E);
      cv.shade();
      return cv.p;
   }

   private static int[] mushroomStewItem() {
      TexExtra.Cv cv = new TexExtra.Cv();
      cv.ell(7.5, 9.5, 5.5, 3.6, 0x7A5230);
      cv.ell(7.5, 8.5, 5.2, 2.5, 0x9C6B3E);
      cv.ell(7.5, 7.8, 4.3, 1.8, 0xCE7E62);
      cv.set(6, 7, 0xE89B7E);
      cv.set(8, 8, 0xC83C2B);
      cv.set(9, 7, 0xFFFFFF);
      cv.shade();
      return cv.p;
   }

   private static int[] compassItem() {
      TexExtra.Cv cv = new TexExtra.Cv();
      cv.ell(7.5, 8.0, 5.6, 5.2, 0x74787E);
      cv.ell(7.5, 8.0, 4.4, 4.0, 0xA8AEB6);
      cv.ell(7.5, 8.0, 3.4, 3.0, 0x262A30);
      cv.line(7, 8, 7, 5, 0xE82820);
      cv.line(8, 8, 8, 5, 0xFF4438);
      cv.line(7, 9, 7, 10, 0xD8DCE2);
      cv.shade();
      return cv.p;
   }

   private static int[] clockItem() {
      TexExtra.Cv cv = new TexExtra.Cv();
      cv.ell(7.5, 8.0, 5.6, 5.2, 0xB88614);
      cv.ell(7.5, 8.0, 4.5, 4.1, 0xF4C424);
      cv.ell(7.5, 8.0, 3.2, 2.8, 0x2C4888);
      cv.set(6, 6, 0xFCE868);
      cv.set(7, 6, 0xFCE868);
      cv.set(9, 9, 0xFCE868);
      cv.line(7, 8, 9, 7, 0xFFF6A8);
      cv.shade();
      return cv.p;
   }

   private static int[] fireChargeItem() {
      TexExtra.Cv cv = new TexExtra.Cv();
      cv.ell(7.5, 8.0, 5.4, 5.2, 0x2E1C16);
      cv.ell(7.5, 8.0, 4.4, 4.2, 0x6E2614);
      cv.ell(7.2, 7.8, 3.1, 2.9, 0xDC6218);
      cv.ell(6.8, 7.4, 1.8, 1.7, 0xF8B828);
      cv.set(6, 6, 0xFFF8A8);
      cv.set(9, 9, 0xF8B828);
      cv.shade();
      return cv.p;
   }

   private static int[] pistolItem() {
      TexExtra.Cv cv = new TexExtra.Cv();
      cv.rect(3, 5, 10, 3, 0x687280);
      cv.rect(4, 5, 8, 1, 0x98A4B4);
      cv.rect(3, 8, 4, 5, 0x2A2E36);
      cv.rect(4, 9, 2, 3, 0x4E3828);
      cv.rect(7, 8, 2, 2, 0x444A54);
      cv.set(12, 4, 0xF8A828);
      cv.set(4, 4, 0xDCE4EC);
      cv.shade();
      return cv.p;
   }

   private static int[] machineGunItem() {
      TexExtra.Cv cv = new TexExtra.Cv();
      cv.rect(1, 7, 3, 3, 0x5A3E28);
      cv.rect(4, 6, 8, 3, 0x4A525E);
      cv.rect(5, 6, 6, 1, 0x8894A4);
      cv.rect(12, 7, 3, 1, 0x2C3038);
      cv.rect(5, 9, 2, 4, 0x282C34);
      cv.rect(8, 9, 2, 5, 0xD48824);
      cv.rect(6, 4, 4, 2, 0x343A42);
      cv.set(14, 6, 0xFF9820);
      cv.shade();
      return cv.p;
   }

   private static int[] sniperRifleItem() {
      TexExtra.Cv cv = new TexExtra.Cv();
      cv.rect(1, 8, 3, 3, 0x2E4E34);
      cv.rect(4, 7, 7, 2, 0x46724E);
      cv.rect(11, 7, 4, 1, 0x363C44);
      cv.rect(5, 4, 5, 2, 0x242830);
      cv.set(9, 4, 0x64D4FF);
      cv.set(5, 4, 0x64D4FF);
      cv.rect(5, 9, 2, 3, 0x22262C);
      cv.rect(8, 9, 2, 3, 0x3E444C);
      cv.shade();
      return cv.p;
   }

   private static int[] tntGrenadeItem() {
      TexExtra.Cv cv = new TexExtra.Cv();
      cv.ell(7.5, 9.0, 4.4, 4.8, 0xC8241C);
      cv.rect(4, 8, 8, 2, 0xF2F2F2);
      cv.set(5, 9, 0x202020);
      cv.set(7, 9, 0x202020);
      cv.set(9, 9, 0x202020);
      cv.rect(6, 3, 3, 2, 0x58606A);
      cv.line(9, 4, 11, 7, 0x7A8490);
      cv.set(7, 2, 0xFFE048);
      cv.set(8, 1, 0xFF6820);
      cv.shade();
      return cv.p;
   }

   private static int[] murderKnifeItem() {
      TexExtra.Cv cv = new TexExtra.Cv();
      cv.line(3, 13, 6, 10, 0x38241C);
      cv.line(4, 13, 6, 11, 0x5A3A2C);
      cv.set(3, 13, 0xD82028);
      cv.line(5, 8, 8, 11, 0xD4A828);
      cv.line(7, 9, 13, 3, 0xDCE6F0);
      cv.line(8, 9, 13, 4, 0x98A4B4);
      cv.line(9, 8, 13, 4, 0xC81C24);
      cv.shade();
      return cv.p;
   }

   private static int[] c4BombItem() {
      TexExtra.Cv cv = new TexExtra.Cv();
      cv.rect(3, 4, 10, 9, 0x7A7250);
      cv.rect(4, 5, 8, 7, 0x9C9268);
      cv.rect(3, 7, 10, 1, 0x282C30);
      cv.rect(3, 10, 10, 1, 0x282C30);
      cv.rect(5, 6, 6, 3, 0x1C2226);
      cv.set(6, 7, 0xFF2820);
      cv.set(7, 7, 0xFF2820);
      cv.set(9, 7, 0x38F048);
      cv.line(5, 4, 6, 2, 0xE02820);
      cv.line(10, 4, 9, 2, 0x2878E0);
      cv.shade();
      return cv.p;
   }

   private static int[] siegeCannonItem() {
      TexExtra.Cv cv = new TexExtra.Cv();
      cv.rect(2, 9, 6, 4, 0x5C3A20);
      cv.rect(4, 5, 10, 4, 0x2C323B);
      cv.rect(5, 6, 8, 2, 0x485260);
      cv.rect(12, 4, 2, 6, 0xD8A432);
      cv.ell(6.0, 12.0, 2.2, 2.2, 0x3E2614);
      cv.set(6, 12, 0xC89628);
      cv.set(4, 4, 0xFF7820);
      cv.set(3, 3, 0xFFE048);
      cv.shade();
      return cv.p;
   }

   private static int[] mortarStrikeItem() {
      TexExtra.Cv cv = new TexExtra.Cv();
      cv.rect(3, 12, 10, 2, 0x4C321C);
      cv.line(5, 11, 11, 4, 0x363E4A);
      cv.line(6, 11, 12, 4, 0x505C6C);
      cv.line(7, 12, 13, 5, 0x282E38);
      cv.set(12, 3, 0xFF6820);
      cv.set(13, 2, 0xFFE450);
      cv.rect(6, 9, 2, 3, 0x8A6236);
      cv.shade();
      return cv.p;
   }
}
