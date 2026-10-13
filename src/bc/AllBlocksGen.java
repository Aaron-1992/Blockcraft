package bc;

import java.util.ArrayList;
import java.util.LinkedHashSet;

/**
 * V6.0: "All Blocks & Items Texture Showcase Gallery" world generator ({@link WorldSettings#ALL_BLOCKS}).
 * Generates a bright, walkable museum floor at Y=65 displaying EVERY single block, crop stage, plant,
 * double-plant, door, 16-color bed, 16-color banner, stair, slab, fence, gate, trapdoor, glass pane,
 * wool, concrete, terracotta, glazed terracotta, ore, Nether/End block, and redstone mechanism in the game,
 * plus entrance chests stocked with every single item, tool, weapon, bow, armor piece, food, and dye.
 */
public final class AllBlocksGen extends Gen {
   public static final int FLOOR_Y = 65;
   private static final int COLS = 28;
   private static final int START_X = -27;
   private static final int START_Z = 4;
   private static final int STEP_X = 2;
   private static final int STEP_Z = 3;
   private static int[] cachedBlocks;
   private static int[] cachedItems;

   public AllBlocksGen(long seed, WorldSettings settings) {
      super(seed);
      this.settings = settings;
   }

   public static synchronized int[] getShowcaseBlocks() {
      if (cachedBlocks != null) {
         return cachedBlocks;
      }

      LinkedHashSet<Integer> set = new LinkedHashSet<>();
      int[] creative = Blocks.creativeList();
      for (int id : creative) {
         if (!Items.isItem(id) && id > 0 && id < Blocks.MAX) {
            set.add(id);
         }
      }

      // Add every block variant / state that isn't directly in creativeList so 100% of block textures are visible:
      // Turned logs (felled trees along X and Z)
      for (int id = Blocks.LOG_X0; id <= Blocks.LOG_Z0 + 2; id++) {
         set.add(id);
      }
      for (int id = Blocks313.LOGX_JUNGLE; id <= Blocks313.LOGZ_JUNGLE + 2; id++) {
         set.add(id);
      }

      // Lit furnace, oak door, farmland dry & wet, wheat stages 0..7, carrot stages, potato stages, stems, cake, fire
      set.add(Blocks.FURNACE_LIT);
      set.add(Blocks.DOOR_LO);
      set.add(Blocks.FARMLAND);
      set.add(Blocks.FARMLAND_WET);
      for (int s = 0; s < 8; s++) {
         set.add(Blocks.WHEAT + s);
      }
      set.add(Blocks.CARROT0);
      set.add(Blocks.CARROT0 + 2);
      set.add(Blocks.CARROT0 + 4);
      set.add(Blocks.CARROT0 + 7);
      set.add(Blocks.POTATO0);
      set.add(Blocks.POTATO0 + 2);
      set.add(Blocks.POTATO0 + 4);
      set.add(Blocks.POTATO0 + 7);
      set.add(Blocks.PUMPKIN_STEM);
      set.add(Blocks.PUMPKIN_STEM + 7);
      set.add(Blocks.MELON_STEM);
      set.add(Blocks.MELON_STEM + 7);
      set.add(Blocks.CAKE);
      set.add(Blocks.CAKE + 2);
      set.add(Blocks.FIRE);
      set.add(Blocks313.SNOW_LAYER);

      // Portals & End Frame with Eye
      set.add(Blocks.PORTAL_X);
      set.add(Blocks.END_FRAME_EYE);
      set.add(Blocks.END_PORTAL);

      // All 16 colored 3D beds (foot blocks, head placed automatically at z+1)
      for (int c = 0; c < 16; c++) {
         set.add(Decor315.foot(c, 1));
      }

      // All 16 colored standing banners
      for (int c = 0; c < 16; c++) {
         set.add(Decor315.STAND0 + c * 4);
      }

      // Powered Redstone states
      set.add(Redstone319.REDSTONE_LAMP_LIT);
      set.add(Redstone319.REDSTONE_TORCH_OFF);
      set.add(Redstone319.LEVER_ON);
      set.add(Redstone319.REDSTONE_WIRE0);
      set.add(Redstone319.REDSTONE_WIRE0 + 15);
      set.add(Redstone319.REPEATER_ON);
      set.add(Redstone319.DAYLIGHT_SENSOR_ON);
      set.add(Redstone319.OBSERVER_ON);

      int[] out = new int[set.size()];
      int i = 0;
      for (int id : set) {
         out[i++] = id;
      }
      cachedBlocks = out;
      return out;
   }

   public static synchronized int[] getShowcaseItems() {
      if (cachedItems != null) {
         return cachedItems;
      }

      LinkedHashSet<Integer> set = new LinkedHashSet<>();
      int[] creative = Blocks.creativeList();
      for (int id : creative) {
         if (Items.isItem(id)) {
            set.add(id);
         }
      }
      for (int id = 256; id < 1024; id++) {
         if (Items.name[id] != null && !Items.name[id].isEmpty() && !Items.name[id].equals("Unknown") && Items.tile(id) > 0) {
            set.add(id);
         }
      }
      int[] out = new int[set.size()];
      int i = 0;
      for (int id : set) {
         out[i++] = id;
      }
      cachedItems = out;
      return out;
   }

   @Override
   public Col column(int wx, int wz) {
      Col c = new Col();
      c.h = FLOOR_Y;
      c.biome = Biomes.PLAINS;
      c.t = 0.8;
      c.hu = 0.4;
      return c;
   }

   @Override
   public int[] findSpawn() {
      return new int[]{0, FLOOR_Y + 1, -4};
   }

   @Override
   public Chunk generate(int cx, int cz) {
      Chunk c = new Chunk(cx, cz);
      // Like FlatGen: tinted textures need populated biome colours, otherwise grass/leaves render black.
      java.util.Arrays.fill(c.grassColor, grassColorFor(0.8, 0.4));
      java.util.Arrays.fill(c.foliageColor, foliageColorFor(0.8, 0.4));
      for (int z = 0; z < 16; z++) {
         int wz = cz * 16 + z;
         for (int x = 0; x < 16; x++) {
            int wx = cx * 16 + x;
            c.biome[z * 16 + x] = (byte)Biomes.PLAINS;
            c.set(x, 0, z, Blocks.BEDROCK);
            for (int y = 1; y < FLOOR_Y - 1; y++) {
               c.set(x, y, z, Blocks.STONE);
            }
            c.set(x, FLOOR_Y - 1, z, Blocks.STONEBRICK);

            // Bright museum gallery floor at FLOOR_Y in the exhibit bounds, grass outside
            if (wx >= -34 && wx <= 34 && wz >= -10 && wz <= 140) {
               boolean light = Math.floorMod(wx, 4) == 0 && Math.floorMod(wz, 3) == 0;
               boolean aisle = ((wx + wz) & 1) == 0;
               c.set(x, FLOOR_Y, z, light ? Blocks.GLOWSTONE : (aisle ? Blocks.QUARTZ_BLOCK : Blocks.STONEBRICK));
            } else {
               c.set(x, FLOOR_Y, z, Blocks.GRASS);
            }
         }
      }

      int[] blocks = getShowcaseBlocks();
      for (int idx = 0; idx < blocks.length; idx++) {
         int col = idx % COLS;
         int row = idx / COLS;
         int bx = START_X + col * STEP_X;
         int bz = START_Z + row * STEP_Z;
         int id = blocks[idx];
         placeExhibitInChunk(c, cx, cz, bx, FLOOR_Y + 1, bz, id);
      }

      c.computeHeightmap();
      return c;
   }

   private static void setIfInChunk(Chunk c, int cx, int cz, int wx, int wy, int wz, int id) {
      if (wy >= 0 && wy < 256 && wx >> 4 == cx && wz >> 4 == cz) {
         c.set(wx & 15, wy, wz & 15, id);
      }
   }

   private static void placeExhibitInChunk(Chunk c, int cx, int cz, int wx, int wy, int wz, int id) {
      if (wx < cx * 16 - 2 || wx > cx * 16 + 17 || wz < cz * 16 - 2 || wz > cz * 16 + 17) {
         return;
      }

      // Check special substrates and multi-block exhibits
      if (id == Blocks.WATER || id == Blocks.LAVA) {
         // Contained stone-brick basin so fluids don't flood the gallery
         setIfInChunk(c, cx, cz, wx, wy - 1, wz, Blocks.STONEBRICK);
         setIfInChunk(c, cx, cz, wx, wy, wz, id);
         setIfInChunk(c, cx, cz, wx - 1, wy, wz, Blocks.GLASS);
         setIfInChunk(c, cx, cz, wx + 1, wy, wz, Blocks.GLASS);
         setIfInChunk(c, cx, cz, wx, wy, wz - 1, Blocks.GLASS);
         setIfInChunk(c, cx, cz, wx, wy, wz + 1, Blocks.GLASS);
         return;
      }

      if (id == Blocks313.LILY_PAD) {
         setIfInChunk(c, cx, cz, wx, wy - 1, wz, Blocks.WATER);
         setIfInChunk(c, cx, cz, wx, wy, wz, id);
         return;
      }

      if (id == Blocks.CACTUS || id == Blocks313.DEAD_BUSH) {
         setIfInChunk(c, cx, cz, wx, wy - 1, wz, Blocks.SAND);
         setIfInChunk(c, cx, cz, wx, wy, wz, id);
         return;
      }

      if (id == Blocks.SUGAR_CANE) {
         setIfInChunk(c, cx, cz, wx, wy - 1, wz, Blocks.GRASS);
         setIfInChunk(c, cx, cz, wx, wy, wz, id);
         setIfInChunk(c, cx, cz, wx, wy + 1, wz, id);
         return;
      }

      if (id >= Blocks.WHEAT && id < Blocks.WHEAT + 8
         || id >= Blocks.CARROT0 && id < Blocks.CARROT0 + 8
         || id >= Blocks.POTATO0 && id < Blocks.POTATO0 + 8
         || id >= Blocks.PUMPKIN_STEM && id < Blocks.MELON_STEM + 8) {
         setIfInChunk(c, cx, cz, wx, wy - 1, wz, Blocks.FARMLAND_WET);
         setIfInChunk(c, cx, cz, wx, wy, wz, id);
         return;
      }

      if (id == Blocks.TALLGRASS || id == Blocks.FLOWER_RED || id == Blocks.FLOWER_YELLOW
         || id == Blocks313.FERN || id >= Blocks313.FLOWER0 && id < Blocks313.FLOWER0 + Blocks313.FLOWERS
         || id == Blocks313.MUSHROOM_BROWN || id == Blocks313.MUSHROOM_RED) {
         setIfInChunk(c, cx, cz, wx, wy - 1, wz, id == Blocks313.MUSHROOM_BROWN || id == Blocks313.MUSHROOM_RED ? Blocks313.PODZOL : Blocks.GRASS);
         setIfInChunk(c, cx, cz, wx, wy, wz, id);
         return;
      }

      if (Blocks313.isDouble(id)) {
         int lo = id - ((id - Blocks313.DOUBLE0) & 1);
         setIfInChunk(c, cx, cz, wx, wy - 1, wz, Blocks.GRASS);
         setIfInChunk(c, cx, cz, wx, wy, wz, lo);
         setIfInChunk(c, cx, cz, wx, wy + 1, wz, lo + 1);
         return;
      }

      if (Blocks313.isVine(id)) {
         setIfInChunk(c, cx, cz, wx, wy, wz + 1, Blocks.LOG_OAK);
         setIfInChunk(c, cx, cz, wx, wy, wz, Blocks313.vine(4));
         return;
      }

      if (id == Blocks.DOOR_LO) {
         setIfInChunk(c, cx, cz, wx, wy, wz, Blocks.DOOR_LO);
         setIfInChunk(c, cx, cz, wx, wy + 1, wz, Blocks.DOOR_HI);
         return;
      }

      if (Decor315.isBed(id) && !Decor315.isBedHead(id)) {
         int color = Decor315.bedColor(id);
         setIfInChunk(c, cx, cz, wx, wy, wz, Decor315.foot(color, 1));
         setIfInChunk(c, cx, cz, wx, wy, wz + 1, Decor315.head(color, 1));
         return;
      }

      if (id == Blocks.FIRE) {
         setIfInChunk(c, cx, cz, wx, wy - 1, wz, Blocks.NETHERRACK);
         setIfInChunk(c, cx, cz, wx, wy, wz, Blocks.FIRE);
         return;
      }

      // Standard block: place at ground level (wy) AND a floating inspection copy at (wy + 3) so all 6 faces (including bottom) are easy to inspect
      setIfInChunk(c, cx, cz, wx, wy, wz, id);
      if (Blocks.solid[id] && Blocks.kind[id] == Blocks.K_CUBE) {
         setIfInChunk(c, cx, cz, wx, wy + 3, wz, id);
      }
   }

   private static void ensureChunkLoaded(World w, int wx, int wz) {
      int cx = wx >> 4;
      int cz = wz >> 4;
      if (w.chunk(cx, cz) == null && w.gen != null) {
         Chunk ch = w.gen.generate(cx, cz);
         ch.modified = false;
         ch.unloaded = false;
         w.chunks.put(World.key(cx, cz), ch);
      }
   }

   /**
    * Stocks entrance showcase chests at Z = -1 with every single item, tool, weapon, bow, armor, food, and dye,
    * and equips the player with a starter showcase hotbar if their hotbar is empty.
    */
   public static void populateShowcaseChestsAndInventory(Game g, World w) {
      if (w == null) {
         return;
      }

      int[] items = getShowcaseItems();
      int chestCount = (items.length + 26) / 27;
      int startChestX = -(chestCount - 1);
      int itemIdx = 0;
      for (int ci = 0; ci < chestCount; ci++) {
         int cx = startChestX + ci * 2;
         int cy = FLOOR_Y + 1;
         int cz = -1;
         ensureChunkLoaded(w, cx, cz);
         w.setBlock(cx, cy - 1, cz, Blocks.GLOWSTONE);
         w.setBlock(cx, cy, cz, Blocks.CHEST);
         TileEntity te = w.getOrCreateTile(cx, cy, cz);
         Stack[] slots = te != null ? te.slots : null;
         if (slots != null) {
            for (int s = 0; s < 27 && itemIdx < items.length; s++, itemIdx++) {
               int id = items[itemIdx];
               int count = Items.maxStack[id] > 1 ? Math.min(16, Items.maxStack[id]) : 1;
               slots[s] = new Stack(id, count);
            }
         }
      }

      // Also place a Crafting Table and Furnace right next to spawn for convenience
      ensureChunkLoaded(w, startChestX - 2, -1);
      ensureChunkLoaded(w, startChestX + chestCount * 2, -1);
      w.setBlock(startChestX - 2, FLOOR_Y + 1, -1, Blocks.CRAFTING_TABLE);
      w.setBlock(startChestX + chestCount * 2, FLOOR_Y + 1, -1, Blocks.FURNACE);

      if (g != null && g.inv != null) {
         boolean empty = true;
         for (int i = 0; i < 9; i++) {
            if (g.inv[i] != null && g.inv[i].n > 0) {
               empty = false;
               break;
            }
         }
         if (empty) {
            int[] starter = new int[]{268, 267, 316, 317, 318, 319, 451, 283, Redstone319.REDSTONE_TORCH};
            for (int i = 0; i < starter.length && i < 9; i++) {
               int id = starter[i];
               g.inv[i] = new Stack(id, Items.maxStack[id] > 1 ? Math.min(64, Items.maxStack[id]) : 1);
            }
         }
      }
   }

   /** Builds the entire All Blocks Gallery + Item Chests in any existing world around (ox, oy, oz). */
   public static void buildGalleryAt(Game g, World w, int ox, int oy, int oz) {
      if (w == null) {
         return;
      }
      int[] blocks = getShowcaseBlocks();
      int rows = (blocks.length + COLS - 1) / COLS;
      int maxZ = rows * STEP_Z + 6;
      for (int dz = -6; dz <= maxZ; dz++) {
         for (int dx = -30; dx <= 30; dx++) {
            int wx = ox + dx;
            int wz = oz + dz;
            boolean light = Math.floorMod(dx, 4) == 0 && Math.floorMod(dz, 3) == 0;
            boolean aisle = ((dx + dz) & 1) == 0;
            w.setBlock(wx, oy - 1, wz, light ? Blocks.GLOWSTONE : (aisle ? Blocks.QUARTZ_BLOCK : Blocks.STONEBRICK));
            for (int y = oy; y <= oy + 5; y++) {
               w.setBlock(wx, y, wz, Blocks.AIR);
            }
         }
      }

      for (int idx = 0; idx < blocks.length; idx++) {
         int col = idx % COLS;
         int row = idx / COLS;
         int wx = ox + START_X + col * STEP_X;
         int wz = oz + 4 + row * STEP_Z;
         int wy = oy;
         int id = blocks[idx];
         if (id == Blocks.WATER || id == Blocks.LAVA) {
            w.setBlock(wx, wy, wz, id);
            w.setBlock(wx - 1, wy, wz, Blocks.GLASS);
            w.setBlock(wx + 1, wy, wz, Blocks.GLASS);
            w.setBlock(wx, wy, wz - 1, Blocks.GLASS);
            w.setBlock(wx, wy, wz + 1, Blocks.GLASS);
         } else if (id == Blocks313.LILY_PAD) {
            w.setBlock(wx, wy - 1, wz, Blocks.WATER);
            w.setBlock(wx, wy, wz, id);
         } else if (id == Blocks.CACTUS || id == Blocks313.DEAD_BUSH) {
            w.setBlock(wx, wy - 1, wz, Blocks.SAND);
            w.setBlock(wx, wy, wz, id);
         } else if (id == Blocks.SUGAR_CANE) {
            w.setBlock(wx, wy - 1, wz, Blocks.GRASS);
            w.setBlock(wx, wy, wz, id);
            w.setBlock(wx, wy + 1, wz, id);
         } else if (id >= Blocks.WHEAT && id < Blocks.WHEAT + 8
            || id >= Blocks.CARROT0 && id < Blocks.CARROT0 + 8
            || id >= Blocks.POTATO0 && id < Blocks.POTATO0 + 8
            || id >= Blocks.PUMPKIN_STEM && id < Blocks.MELON_STEM + 8) {
            w.setBlock(wx, wy - 1, wz, Blocks.FARMLAND_WET);
            w.setBlock(wx, wy, wz, id);
         } else if (id == Blocks.TALLGRASS || id == Blocks.FLOWER_RED || id == Blocks.FLOWER_YELLOW
            || id == Blocks313.FERN || id >= Blocks313.FLOWER0 && id < Blocks313.FLOWER0 + Blocks313.FLOWERS
            || id == Blocks313.MUSHROOM_BROWN || id == Blocks313.MUSHROOM_RED) {
            w.setBlock(wx, wy - 1, wz, Blocks.GRASS);
            w.setBlock(wx, wy, wz, id);
         } else if (Blocks313.isDouble(id)) {
            int lo = id - ((id - Blocks313.DOUBLE0) & 1);
            w.setBlock(wx, wy - 1, wz, Blocks.GRASS);
            w.setBlock(wx, wy, wz, lo);
            w.setBlock(wx, wy + 1, wz, lo + 1);
         } else if (Blocks313.isVine(id)) {
            w.setBlock(wx, wy, wz + 1, Blocks.LOG_OAK);
            w.setBlock(wx, wy, wz, Blocks313.vine(4));
         } else if (id == Blocks.DOOR_LO) {
            w.setBlock(wx, wy, wz, Blocks.DOOR_LO);
            w.setBlock(wx, wy + 1, wz, Blocks.DOOR_HI);
         } else if (Decor315.isBed(id) && !Decor315.isBedHead(id)) {
            int color = Decor315.bedColor(id);
            w.setBlock(wx, wy, wz, Decor315.foot(color, 1));
            w.setBlock(wx, wy, wz + 1, Decor315.head(color, 1));
         } else if (id == Blocks.FIRE) {
            w.setBlock(wx, wy - 1, wz, Blocks.NETHERRACK);
            w.setBlock(wx, wy, wz, Blocks.FIRE);
         } else {
            w.setBlock(wx, wy, wz, id);
            if (Blocks.solid[id] && Blocks.kind[id] == Blocks.K_CUBE) {
               w.setBlock(wx, wy + 3, wz, id);
            }
         }
      }

      // Place item chests along oz - 1
      int[] items = getShowcaseItems();
      int chestCount = (items.length + 26) / 27;
      int startChestX = ox - (chestCount - 1);
      int itemIdx = 0;
      for (int ci = 0; ci < chestCount; ci++) {
         int cx = startChestX + ci * 2;
         int cy = oy;
         int cz = oz - 1;
         w.setBlock(cx, cy, cz, Blocks.CHEST);
         TileEntity te = w.getOrCreateTile(cx, cy, cz);
         Stack[] slots = te != null ? te.slots : null;
         if (slots != null) {
            for (int s = 0; s < 27 && itemIdx < items.length; s++, itemIdx++) {
               int id = items[itemIdx];
               int count = Items.maxStack[id] > 1 ? Math.min(16, Items.maxStack[id]) : 1;
               slots[s] = new Stack(id, count);
            }
         }
      }
   }
}
