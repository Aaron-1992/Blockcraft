package bc;

import java.util.ArrayList;

/**
 * Update 3.13: the registration of the blocks that the new world generator needs (ids 1140.. in the extension range, see
 * {@link Blocks#EXT0}). Tiles come from {@link TexArt313}.
 */
final class Blocks313 {
   private Blocks313() {
   }

   // ---- stone and earth ----
   static final int GRANITE = 1140;
   static final int DIORITE = 1141;
   static final int ANDESITE = 1142;
   static final int RED_SAND = 1143;
   static final int PODZOL = 1144;
   static final int MYCELIUM = 1145;
   static final int COARSE_DIRT = 1146;
   static final int ICE = 1147;
   static final int PACKED_ICE = 1148;
   /** A thin cover of snow (2 / 16 high, you walk through it). */
   static final int SNOW_LAYER = 1149;
   static final int RED_SANDSTONE = 1150;
   // ---- wood: planks of every species, logs / leaves of jungle, acacia and dark oak ----
   static final int PLANKS_SPRUCE = 1151;
   static final int PLANKS_BIRCH = 1152;
   static final int PLANKS_JUNGLE = 1153;
   static final int PLANKS_ACACIA = 1154;
   static final int PLANKS_DARKOAK = 1155;
   static final int LOG_JUNGLE = 1156;
   static final int LOG_ACACIA = 1157;
   static final int LOG_DARKOAK = 1158;
   /** Side logs along x: 1159..1161 (jungle, acacia, dark oak), along z: 1162..1164. */
   static final int LOGX_JUNGLE = 1159;
   static final int LOGZ_JUNGLE = 1162;
   static final int LEAVES_JUNGLE = 1165;
   static final int LEAVES_ACACIA = 1166;
   static final int LEAVES_DARKOAK = 1167;
   // ---- plants ----
   static final int FERN = 1170;
   static final int DEAD_BUSH = 1171;
   /** Ten more flowers: blue orchid, allium, azure bluet, red / orange / white / pink tulip, oxeye daisy, cornflower, lily of the valley. */
   static final int FLOWER0 = 1172;
   static final int FLOWERS = 10;
   static final int MUSHROOM_BROWN = 1182;
   static final int MUSHROOM_RED = 1183;
   /** Tall plants: six kinds (tall grass, large fern, sunflower, lilac, rose bush, peony), each a lower block and an upper block. */
   static final int DOUBLE0 = 1184;
   static final int DOUBLES = 6;
   static final int LILY_PAD = 1196;
   /** Vines: 1199 + the mask of the walls they cling to (1 = north, 2 = east, 4 = south, 8 = west), so 1200..1214. */
   static final int VINE0 = 1199;
   static final int MUSH_BLOCK_RED = 1216;
   static final int MUSH_BLOCK_BROWN = 1217;
   static final int MUSH_STEM = 1218;
   static int plank(int species) {
      return species == 0 ? Blocks.PLANKS : (species == 1 ? PLANKS_BIRCH : (species == 2 ? PLANKS_SPRUCE : PLANKS_JUNGLE + species - 3));
   }

   static boolean isPlanks(int id) {
      return id == Blocks.PLANKS || id >= PLANKS_SPRUCE && id <= PLANKS_DARKOAK;
   }

   static boolean isVine(int id) {
      return id > VINE0 && id <= VINE0 + 15;
   }

   /** The vine block that clings to the walls in mask (1 north, 2 east, 4 south, 8 west). */
   static int vine(int mask) {
      return VINE0 + mask;
   }

   static boolean isDouble(int id) {
      return id >= DOUBLE0 && id < DOUBLE0 + DOUBLES * 2;
   }

   static boolean isDoubleUpper(int id) {
      return isDouble(id) && (id - DOUBLE0 & 1) == 1;
   }

   static boolean isFlower(int id) {
      return id >= FLOWER0 && id < FLOWER0 + FLOWERS || id == Blocks.FLOWER_RED || id == Blocks.FLOWER_YELLOW;
   }

   /** The plants that shears cut fast (and that give themselves when cut). */
   static boolean shearable(int id) {
      return id == Blocks.TALLGRASS || id == FERN || id == DEAD_BUSH || isVine(id) || id == DOUBLE0 || id == DOUBLE0 + 1 || id == DOUBLE0 + 2 || id == DOUBLE0 + 3;
   }

   /** Blocks that have to be looked at when a neighbour changes (a plant that may lose its ground, a layer of snow, a lily pad, sand that may fall). */
   static boolean needsUpdate(int id) {
      return id == RED_SAND || id == SNOW_LAYER || id == LILY_PAD || isVine(id) || isDouble(id);
   }

   /** Items that are drawn as a flat picture (not as a little block) in the hotbar and in the hand. */
   static boolean flatItem(int id) {
      return id == LILY_PAD || isVine(id) || id == SNOW_LAYER && false || Shapes.flatItem(id);
   }

   private static void box(int id, double x0, double y0, double z0, double x1, double y1, double z1) {
      Blocks.kind[id] = Blocks.K_BOX;
      Blocks.box[id] = new double[]{x0, y0, z0, x1, y1, z1};
      Blocks.coll[id] = Blocks.box[id];
   }

   private static void pick(int id, boolean needTool, int level) {
      Blocks.mineClass[id] = 1;
      Blocks.needTool[id] = needTool;
      Blocks.harvestLevel[id] = level;
   }

   private static void plant(int id, String name, int tile) {
      Blocks.def(id, name, 0.0F, false, tile);
      Blocks.kind[id] = Blocks.K_CROSS;
      Blocks.solid[id] = false;
      Blocks.coll[id] = null;
      Blocks.cutout[id] = true;
      Blocks.box[id] = new double[]{0.2, 0.0, 0.2, 0.8, 0.7, 0.8};
   }

   static void register() {
      // ---- stone and earth ----
      int[][] stones = new int[][]{{GRANITE, TexArt313.GRANITE}, {DIORITE, TexArt313.DIORITE}, {ANDESITE, TexArt313.ANDESITE}};
      String[] stoneNames = new String[]{"Granite", "Diorite", "Andesite"};
      for (int i = 0; i < 3; i++) {
         Blocks.def(stones[i][0], stoneNames[i], 1.5F, true, stones[i][1]);
         pick(stones[i][0], true, 0);
      }

      Blocks.def(RED_SAND, "Red Sand", 0.5F, true, TexArt313.RED_SAND);
      Blocks.mineClass[RED_SAND] = 3;
      Blocks.def(PODZOL, "Podzol", 0.5F, true, TexArt313.PODZOL_SIDE);
      Blocks.faces(PODZOL, TexArt313.PODZOL_TOP, 1, TexArt313.PODZOL_SIDE);
      Blocks.mineClass[PODZOL] = 3;
      Blocks.def(MYCELIUM, "Mycelium", 0.6F, true, TexArt313.MYCELIUM_SIDE);
      Blocks.faces(MYCELIUM, TexArt313.MYCELIUM_TOP, 1, TexArt313.MYCELIUM_SIDE);
      Blocks.mineClass[MYCELIUM] = 3;
      Blocks.drop[MYCELIUM] = MYCELIUM;
      Blocks.def(COARSE_DIRT, "Coarse Dirt", 0.5F, true, TexArt313.COARSE_DIRT);
      Blocks.mineClass[COARSE_DIRT] = 3;
      Blocks.def(ICE, "Ice", 0.5F, false, TexArt313.ICE);
      Blocks.opacity[ICE] = 1;
      Blocks.drop[ICE] = 0;
      pick(ICE, false, 0);
      Blocks.def(PACKED_ICE, "Packed Ice", 0.5F, true, TexArt313.PACKED_ICE);
      pick(PACKED_ICE, false, 0);
      Blocks.drop[PACKED_ICE] = 0;
      // Loose snow is snow on every face, not a snow-capped dirt cube.
      Blocks.def(SNOW_LAYER, "Snow", 0.1F, false, Tex.SNOW);
      box(SNOW_LAYER, 0.0, 0.0, 0.0, 1.0, 0.125, 1.0);
      Blocks.solid[SNOW_LAYER] = false;
      Blocks.coll[SNOW_LAYER] = null;
      Blocks.replaceable[SNOW_LAYER] = true;
      Blocks.mineClass[SNOW_LAYER] = 3;
      Blocks.drop[SNOW_LAYER] = 0;
      Blocks.def(RED_SANDSTONE, "Red Sandstone", 0.8F, true, TexArt313.RS_SIDE);
      Blocks.faces(RED_SANDSTONE, TexArt313.RS_TOP, TexArt313.RS_TOP, TexArt313.RS_SIDE);
      pick(RED_SANDSTONE, true, 0);
      // ---- wood ----
      String[] woodName = new String[]{"Spruce", "Birch", "Jungle", "Acacia", "Dark Oak"};
      int[] planks = new int[]{PLANKS_SPRUCE, PLANKS_BIRCH, PLANKS_JUNGLE, PLANKS_ACACIA, PLANKS_DARKOAK};
      for (int i = 0; i < 5; i++) {
         Blocks.def(planks[i], woodName[i] + " Planks", 2.0F, true, TexArt313.PLANKS0 + i);
         Blocks.mineClass[planks[i]] = 2;
      }

      String[] logName = new String[]{"Jungle Log", "Acacia Log", "Dark Oak Log"};
      for (int i = 0; i < 3; i++) {
         int bark = TexArt313.BARK0 + i;
         int ring = TexArt313.RING0 + i;
         int turned = TexArt313.TURNED0 + i;
         int up = LOG_JUNGLE + i;
         Blocks.def(up, logName[i], 2.0F, true, bark);
         Blocks.faces(up, ring, ring, bark);
         Blocks.mineClass[up] = 2;
         for (int axis = 0; axis < 2; axis++) {
            int id = (axis == 0 ? LOGX_JUNGLE : LOGZ_JUNGLE) + i;
            Blocks.def(id, logName[i], 2.0F, true, bark);
            int[] tx = new int[]{turned, turned, turned, turned, ring, ring};
            int[] tz = new int[]{bark, bark, ring, ring, turned, turned};
            for (int f = 0; f < 6; f++) {
               Blocks.tiles[id][f] = axis == 0 ? tx[f] : tz[f];
            }

            Blocks.drop[id] = up;
            Blocks.mineClass[id] = 2;
         }
      }

      String[] leafName = new String[]{"Jungle Leaves", "Acacia Leaves", "Dark Oak Leaves"};
      for (int i = 0; i < 3; i++) {
         int id = LEAVES_JUNGLE + i;
         Blocks.def(id, leafName[i], 0.2F, false, TexArt313.LEAVES0 + i);
         Blocks.opacity[id] = 1;
         Blocks.tint[id] = 2;
         Blocks.drop[id] = 0;
         Blocks.cutout[id] = true;
      }

      // ---- plants ----
      plant(FERN, "Fern", TexArt313.FERN);
      Blocks.tint[FERN] = 1;
      Blocks.replaceable[FERN] = true;
      Blocks.drop[FERN] = 0;
      plant(DEAD_BUSH, "Dead Bush", TexArt313.DEAD_BUSH);
      Blocks.replaceable[DEAD_BUSH] = true;
      Blocks.drop[DEAD_BUSH] = 0;
      String[] flowerName = new String[]{"Blue Orchid", "Allium", "Azure Bluet", "Red Tulip", "Orange Tulip", "White Tulip", "Pink Tulip", "Oxeye Daisy", "Cornflower", "Lily of the Valley"};
      for (int i = 0; i < FLOWERS; i++) {
         plant(FLOWER0 + i, flowerName[i], TexArt313.FLOWER0 + i);
         Blocks.box[FLOWER0 + i] = new double[]{0.3, 0.0, 0.3, 0.7, 0.65, 0.7};
      }

      plant(MUSHROOM_BROWN, "Brown Mushroom", TexArt313.MUSH_BROWN);
      Blocks.box[MUSHROOM_BROWN] = new double[]{0.3, 0.0, 0.3, 0.7, 0.4, 0.7};
      Blocks.emit[MUSHROOM_BROWN] = 1;
      plant(MUSHROOM_RED, "Red Mushroom", TexArt313.MUSH_RED);
      Blocks.box[MUSHROOM_RED] = new double[]{0.3, 0.0, 0.3, 0.7, 0.4, 0.7};
      String[] tallName = new String[]{"Tall Grass", "Large Fern", "Sunflower", "Lilac", "Rose Bush", "Peony"};
      for (int i = 0; i < DOUBLES; i++) {
         int lo = DOUBLE0 + i * 2;
         plant(lo, tallName[i], TexArt313.DOUBLE0 + i * 2);
         plant(lo + 1, tallName[i], TexArt313.DOUBLE0 + i * 2 + 1);
         Blocks.box[lo] = new double[]{0.15, 0.0, 0.15, 0.85, 1.0, 0.85};
         Blocks.box[lo + 1] = new double[]{0.15, 0.0, 0.15, 0.85, 0.9, 0.85};
         Blocks.drop[lo + 1] = 0;
         if (i < 2) {
            Blocks.tint[lo] = 1;
            Blocks.tint[lo + 1] = 1;
            Blocks.replaceable[lo] = true;
            Blocks.replaceable[lo + 1] = true;
            Blocks.drop[lo] = 0;
         }
      }

      Blocks.def(LILY_PAD, "Lily Pad", 0.0F, false, TexArt313.LILY);
      box(LILY_PAD, 0.0, 0.0, 0.0, 1.0, 0.015625, 1.0);
      Blocks.coll[LILY_PAD] = new double[]{0.0625, 0.0, 0.0625, 0.9375, 0.09375, 0.9375};
      Blocks.cutout[LILY_PAD] = true;
      for (int m = 1; m <= 15; m++) {
         int id = VINE0 + m;
         Blocks.def(id, "Vines", 0.2F, false, TexArt313.VINE);
         Blocks.kind[id] = Blocks.K_VINE;
         Blocks.solid[id] = false;
         Blocks.coll[id] = null;
         Blocks.cutout[id] = true;
         Blocks.tint[id] = 2;
         Blocks.replaceable[id] = true;
         Blocks.drop[id] = VINE0 + 1;
         Blocks.mineClass[id] = 2;
         Blocks.box[id] = new double[]{0.0, 0.0, 0.0, 1.0, 1.0, 1.0};
      }

      // ---- giant mushrooms ----
      Blocks.def(MUSH_BLOCK_RED, "Red Mushroom Block", 0.2F, true, TexArt313.MUSH_BLOCK_RED);
      Blocks.def(MUSH_BLOCK_BROWN, "Brown Mushroom Block", 0.2F, true, TexArt313.MUSH_BLOCK_BROWN);
      Blocks.def(MUSH_STEM, "Mushroom Stem", 0.2F, true, TexArt313.MUSH_STEM);
      for (int id : new int[]{MUSH_BLOCK_RED, MUSH_BLOCK_BROWN, MUSH_STEM}) {
         Blocks.mineClass[id] = 2;
         Blocks.drop[id] = 0;
      }

      Blocks.drop[MUSHROOM_BROWN] = MUSHROOM_BROWN;
      Blocks.drop[MUSHROOM_RED] = MUSHROOM_RED;
      for (int i = 2; i < DOUBLES; i++) {
         Blocks.drop[DOUBLE0 + i * 2] = DOUBLE0 + i * 2;
      }

      Blocks.drop[LILY_PAD] = LILY_PAD;
      // the tables of the wood species (logs, leaves) are in Blocks
   }

   /** The new blocks the creative inventory offers (the creative list of Blocks adds them at the end of its own). */
   static void addToCreative(ArrayList<Integer> list) {
      for (int id : new int[]{GRANITE, DIORITE, ANDESITE, RED_SAND, RED_SANDSTONE, PODZOL, MYCELIUM, COARSE_DIRT, ICE, PACKED_ICE, SNOW_LAYER, PLANKS_SPRUCE, PLANKS_BIRCH, PLANKS_JUNGLE,
         PLANKS_ACACIA, PLANKS_DARKOAK, LOG_JUNGLE, LOG_ACACIA, LOG_DARKOAK, LEAVES_JUNGLE, LEAVES_ACACIA, LEAVES_DARKOAK}) {
         list.add(id);
      }

      list.add(FERN);
      list.add(DEAD_BUSH);
      for (int i = 0; i < FLOWERS; i++) {
         list.add(FLOWER0 + i);
      }

      list.add(MUSHROOM_BROWN);
      list.add(MUSHROOM_RED);
      list.add(DOUBLE0);
      list.add(DOUBLE0 + 2);
      list.add(DOUBLE0 + 4);
      list.add(DOUBLE0 + 6);
      list.add(DOUBLE0 + 8);
      list.add(DOUBLE0 + 10);
      list.add(LILY_PAD);
      list.add(VINE0 + 1);
      list.add(MUSH_BLOCK_RED);
      list.add(MUSH_BLOCK_BROWN);
      list.add(MUSH_STEM);
   }
}
