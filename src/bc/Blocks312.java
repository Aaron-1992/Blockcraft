package bc;

/**
 * Update 3.12: the registration of every new block (ids from {@link Blocks#EXT0} up). Called from the end of Blocks' static
 * initialiser, so the old tables are complete when it runs. Tiles come from {@link TexArt312}.
 */
final class Blocks312 {
   private Blocks312() {
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

   /** The blocks and items of Update 3.12 that the creative inventory offers. */
   static void addToCreative(java.util.ArrayList<Integer> list) {
      for (int id : new int[]{Blocks.NETHERRACK, Blocks.SOUL_SAND, Blocks.QUARTZ_ORE, Blocks.NETHER_BRICKS, Blocks.NETHER_BRICK_FENCE, Blocks.MAGMA, Blocks.QUARTZ_BLOCK,
         Blocks.END_STONE, Blocks.END_FRAME, Blocks.END_FRAME_EYE, Blocks.DRAGON_EGG}) {
         list.add(id);
      }

      for (int id = Items.QUARTZ; id <= Items.EMERALD; id++) {
         list.add(id);
      }

      Color312.addToCreative(list);
   }

   static void register() {
      nether();
      end();
      Color312.blocks();
   }

   static void nether() {
      Blocks.def(Blocks.NETHERRACK, "Netherrack", 0.4F, true, TexArt312.NETHERRACK);
      pick(Blocks.NETHERRACK, true, 0);
      Blocks.def(Blocks.SOUL_SAND, "Soul Sand", 0.5F, true, TexArt312.SOUL_SAND);
      Blocks.mineClass[Blocks.SOUL_SAND] = 3;
      Blocks.def(Blocks.QUARTZ_ORE, "Nether Quartz Ore", 3.0F, true, TexArt312.QUARTZ_ORE);
      pick(Blocks.QUARTZ_ORE, true, 0);
      Blocks.def(Blocks.NETHER_BRICKS, "Nether Bricks", 2.0F, true, TexArt312.NETHER_BRICKS);
      pick(Blocks.NETHER_BRICKS, true, 0);
      Blocks.def(Blocks.MAGMA, "Magma Block", 0.5F, true, TexArt312.MAGMA);
      Blocks.emit[Blocks.MAGMA] = 3;
      pick(Blocks.MAGMA, true, 0);
      Blocks.def(Blocks.QUARTZ_BLOCK, "Block of Quartz", 0.8F, true, TexArt312.QUARTZ_SIDE);
      Blocks.faces(Blocks.QUARTZ_BLOCK, TexArt312.QUARTZ_TOP, TexArt312.QUARTZ_TOP, TexArt312.QUARTZ_SIDE);
      pick(Blocks.QUARTZ_BLOCK, true, 0);
      Blocks.def(Blocks.NETHER_BRICK_FENCE, "Nether Brick Fence", 2.0F, false, TexArt312.NETHER_BRICKS);
      Blocks.kind[Blocks.NETHER_BRICK_FENCE] = Blocks.K_FENCE;
      pick(Blocks.NETHER_BRICK_FENCE, true, 0);
      Blocks.box[Blocks.NETHER_BRICK_FENCE] = new double[]{0.375, 0.0, 0.375, 0.625, 1.0, 0.625};
      Blocks.coll[Blocks.NETHER_BRICK_FENCE] = new double[]{0.375, 0.0, 0.375, 0.625, 1.5, 0.625};
      // the portal: a thin film you can walk through, it glows, it cannot be mined (it goes when the frame goes)
      for (int axis = 0; axis < 2; axis++) {
         int id = Blocks.PORTAL_X + axis;
         Blocks.def(id, "Nether Portal", -1.0F, false, TexArt312.PORTAL);
         Blocks.kind[id] = Blocks.K_PORTAL;
         Blocks.solid[id] = false;
         Blocks.coll[id] = null;
         Blocks.emit[id] = 11;
         Blocks.drop[id] = 0;
         Blocks.replaceable[id] = true;
         Blocks.box[id] = axis == 0 ? new double[]{0.0, 0.0, 0.375, 1.0, 1.0, 0.625} : new double[]{0.375, 0.0, 0.0, 0.625, 1.0, 1.0};
      }
   }

   static void end() {
      Blocks.def(Blocks.END_STONE, "End Stone", 3.0F, true, TexArt312.END_STONE);
      pick(Blocks.END_STONE, true, 0);
      for (int eye = 0; eye < 2; eye++) {
         int id = Blocks.END_FRAME + eye;
         Blocks.def(id, eye == 0 ? "End Portal Frame" : "End Portal Frame with Eye", -1.0F, false, TexArt312.FRAME_SIDE);
         Blocks.tiles[id][0] = eye == 0 ? TexArt312.FRAME_TOP : TexArt312.FRAME_TOP_EYE;
         Blocks.tiles[id][1] = TexArt312.END_STONE;
         box(id, 0.0, 0.0, 0.0, 1.0, 0.8125, 1.0);
         Blocks.opacity[id] = 15;
         Blocks.drop[id] = 0;
      }

      int id = Blocks.END_PORTAL;
      Blocks.def(id, "End Portal", -1.0F, false, TexArt312.END_PORTAL);
      box(id, 0.0, 0.0, 0.0, 1.0, 0.75, 1.0);
      Blocks.solid[id] = false;
      Blocks.coll[id] = null;
      Blocks.emit[id] = 15;
      Blocks.drop[id] = 0;
      Blocks.def(Blocks.DRAGON_EGG, "Dragon Egg", 3.0F, true, TexArt312.DRAGON_EGG);
      Blocks.emit[Blocks.DRAGON_EGG] = 1;
      pick(Blocks.DRAGON_EGG, false, 0);
   }
}
