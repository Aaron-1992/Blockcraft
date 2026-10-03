package bc;

/**
 * Update 3.12: registration of the new items (ids 352.. in the item range 256..1023), their recipes and their fuel value.
 * The pictures come from {@link TexArt312#sprite}; item tiles are numbered from {@link TexArt312#ITEM0}.
 */
final class Items312 {
   private Items312() {
   }

   /** What the chests of a nether fortress hold: {item, least, most, weight}. */
   static final int[][] NETHER_LOOT = {
      {Items.GOLD_INGOT, 2, 6, 12},
      {Items.IRON_INGOT, 2, 5, 8},
      {Items.GOLD_NUGGET, 5, 14, 10},
      {Items.DIAMOND, 1, 2, 3},
      {Items.FLINT_STEEL, 1, 1, 4},
      {Blocks.OBSIDIAN, 2, 4, 4},
      {Blocks.NETHER_BRICKS, 4, 10, 5},
      {Items.COAL, 3, 8, 6},
      {Items.BONE, 2, 6, 5},
      {Items.toolId(3, 4), 1, 1, 4},
      {Items.toolId(2, 4), 1, 1, 3},
      {Items.armorId(2, 1), 1, 1, 3},
      {Items.armorId(2, 0), 1, 1, 3},
      {Items.armorId(1, 1), 1, 1, 2},
      {Items.armorId(1, 2), 1, 1, 2},
      {Items.APPLE, 2, 4, 4},
      {Items.ARROW, 4, 12, 4}
   };

   private static void item(int id, String name) {
      Items.defTile(id, name, TexArt312.ITEM0 + (id - Items.QUARTZ));
   }

   static void register() {
      item(Items.QUARTZ, "Nether Quartz");
      item(Items.NETHER_BRICK, "Nether Brick");
      item(Items.BLAZE_ROD, "Blaze Rod");
      item(Items.BLAZE_POWDER, "Blaze Powder");
      item(Items.GHAST_TEAR, "Ghast Tear");
      item(Items.GOLD_NUGGET, "Gold Nugget");
      item(Items.ENDER_PEARL, "Ender Pearl");
      Items.maxStack[Items.ENDER_PEARL] = 16;
      item(Items.EYE_OF_ENDER, "Eye of Ender");
      item(Items.EMERALD, "Emerald");
      Blocks.drop[Blocks.QUARTZ_ORE] = Items.QUARTZ;
   }

   static void recipes() {
      Recipes.shaped(Blocks.QUARTZ_BLOCK, 1, new String[]{"QQ", "QQ"}, 'Q', Items.QUARTZ);
      Recipes.shaped(Blocks.NETHER_BRICKS, 1, new String[]{"BB", "BB"}, 'B', Items.NETHER_BRICK);
      Recipes.shaped(Blocks.NETHER_BRICK_FENCE, 6, new String[]{"BBB", "BBB"}, 'B', Blocks.NETHER_BRICKS);
      Recipes.shapeless(Items.BLAZE_POWDER, 2, Items.BLAZE_ROD);
      Recipes.shapeless(Items.EYE_OF_ENDER, 1, Items.ENDER_PEARL, Items.BLAZE_POWDER);
      Recipes.shapeless(Items.GOLD_NUGGET, 9, Items.GOLD_INGOT);
      Recipes.shaped(Items.GOLD_INGOT, 1, new String[]{"NNN", "NNN", "NNN"}, 'N', Items.GOLD_NUGGET);
      Recipes.smelt.put(Blocks.NETHERRACK, new Stack(Items.NETHER_BRICK, 1));
      Recipes.smelt.put(Blocks.QUARTZ_ORE, new Stack(Items.QUARTZ, 1));
      Recipes.fuel.put(Items.BLAZE_ROD, 2400);
   }
}
