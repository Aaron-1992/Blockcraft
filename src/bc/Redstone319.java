package bc;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Update 3.19 (completing Update 3.15 & 1.12 content): Redstone mechanisms (Block of Redstone, Redstone Lamp, Redstone Torch, Lever,
 * Stone & Wooden Buttons, Stone & Wooden Pressure Plates, Redstone Wire power propagation, Iron Trapdoor / Door / Gate / TNT activation)
 * plus missing 1.12 polished/chiseled stones, Slime Block (with bounce physics) and Sea Lantern.
 * Block IDs 1900..1951, item ID 450 (Redstone Dust), atlas tiles 643..659.
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

   static final int ITEM_REDSTONE = 450;

   // Atlas tiles (643..659)
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

   private static final Map<Long, Integer> buttonTimers = new HashMap<>();
   private static boolean updating = false;

   private Redstone319() {
   }

   static void registerItems() {
      Items.defTile(ITEM_REDSTONE, "Redstone Dust", T_REDSTONE_WIRE);
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

      for (int p = 0; p < 16; p++) {
         int id = REDSTONE_WIRE0 + p;
         int r = 75 + p * 12;
         int g = p * 2;
         int b = p;
         int rgb = r << 16 | g << 8 | b;
         modelBlock(
            id,
            "Redstone Wire",
            0.0F,
            T_REDSTONE_WIRE,
            ITEM_REDSTONE,
            new double[]{0.0, 0.0, 0.0, 1.0, 0.0625, 1.0},
            new double[][]{
               Shapes.tint(Shapes.tiles(Shapes.px(0, 0, 6, 16, 0.5, 10), T_REDSTONE_WIRE, T_REDSTONE_WIRE, T_REDSTONE_WIRE), rgb),
               Shapes.tint(Shapes.tiles(Shapes.px(6, 0, 0, 10, 0.5, 16), T_REDSTONE_WIRE, T_REDSTONE_WIRE, T_REDSTONE_WIRE), rgb)
            }
         );
         if (p >= 8) {
            Blocks.emit[id] = (p - 4) / 2;
         }
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

   static void addToCreative(ArrayList<Integer> list) {
      list.add(ITEM_REDSTONE);
      list.add(REDSTONE_BLOCK);
      list.add(REDSTONE_LAMP);
      list.add(REDSTONE_TORCH);
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

   static boolean isFloorPart(int id) {
      return id == REDSTONE_TORCH || id >= LEVER_OFF && id <= WOOD_PLATE_ON || isWire(id);
   }

   /** Direct power (0..15) emitted by a source block itself (excluding wire). */
   static int sourcePower(int id) {
      return switch (id) {
         case REDSTONE_BLOCK, REDSTONE_TORCH, LEVER_ON, STONE_BUTTON_ON, WOOD_BUTTON_ON, STONE_PLATE_ON, WOOD_PLATE_ON -> 15;
         default -> 0;
      };
   }

   /** Power (0..15) at (x, y, z), including wire signal strength. */
   static int powerAt(World w, int x, int y, int z) {
      int id = w.getBlock(x, y, z);
      if (isWire(id)) {
         return id - REDSTONE_WIRE0;
      }

      return sourcePower(id);
   }

   /** True if any of the 6 neighbours of (x, y, z) — or the block below a floor mechanism — provides Redstone power > 0. */
   static boolean isPowered(World w, int x, int y, int z) {
      int[][] dirs = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
      for (int[] d : dirs) {
         if (powerAt(w, x + d[0], y + d[1], z + d[2]) > 0) {
            return true;
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

      return false;
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
      if (!g.player.creative) {
         s.n--;
         if (s.n <= 0) {
            g.inv[g.sel] = null;
         }
      }

      return true;
   }

   static boolean handlesUpdate(int id) {
      return isFloorPart(id) || id == REDSTONE_BLOCK || id == REDSTONE_LAMP || id == REDSTONE_LAMP_LIT || Shapes.isTrap(id) || Shapes.isGate(id) || id == Blocks.TNT;
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

      propagate(w, x, y, z);
   }

   /** Recomputes wire power levels within a 15-block Manhattan neighbourhood and updates lamps, doors, trapdoors, gates and TNT. */
   static void propagate(World w, int cx, int cy, int cz) {
      if (updating || w == null) {
         return;
      }

      updating = true;
      try {
         // 1. Collect all wire cells in a 15-block box around (cx, cy, cz)
         ArrayList<int[]> wires = new ArrayList<>();
         for (int dy = -3; dy <= 3; dy++) {
            int y = cy + dy;
            if (y < 1 || y > 254) {
               continue;
            }

            for (int dz = -15; dz <= 15; dz++) {
               for (int dx = -15; dx <= 15; dx++) {
                  if (Math.abs(dx) + Math.abs(dz) <= 15) {
                     int id = w.getBlock(cx + dx, y, cz + dz);
                     if (isWire(id)) {
                        wires.add(new int[]{cx + dx, y, cz + dz});
                     }
                  }
               }
            }
         }

         // 2. Relax wire power from source blocks
         if (!wires.isEmpty()) {
            int n = wires.size();
            int[] pwr = new int[n];
            int[][] nb6 = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
            for (int i = 0; i < n; i++) {
               int[] p = wires.get(i);
               int maxSrc = 0;
               for (int[] d : nb6) {
                  int sp = sourcePower(w.getBlock(p[0] + d[0], p[1] + d[1], p[2] + d[2]));
                  if (sp > maxSrc) {
                     maxSrc = sp;
                  }
               }

               pwr[i] = maxSrc;
            }

            boolean changed = true;
            while (changed) {
               changed = false;
               for (int i = 0; i < n; i++) {
                  int[] a = wires.get(i);
                  for (int j = 0; j < n; j++) {
                     if (i == j) {
                        continue;
                     }

                     int[] b = wires.get(j);
                     if (Math.abs(a[0] - b[0]) + Math.abs(a[2] - b[2]) == 1 && Math.abs(a[1] - b[1]) <= 1) {
                        if (pwr[j] - 1 > pwr[i]) {
                           pwr[i] = pwr[j] - 1;
                           changed = true;
                        }
                     }
                  }
               }
            }

            for (int i = 0; i < n; i++) {
               int[] p = wires.get(i);
               int want = REDSTONE_WIRE0 + pwr[i];
               if (w.getBlock(p[0], p[1], p[2]) != want) {
                  w.setBlock(p[0], p[1], p[2], want);
                  if (w.game != null) {
                     w.game.meshNow(p[0], p[1], p[2]);
                  }
               }
            }
         }

         // 3. Update Redstone-actuated blocks around (cx, cy, cz) and around every wire
         actuateAround(w, cx, cy, cz);
         for (int[] p : wires) {
            actuateAround(w, p[0], p[1], p[2]);
         }
      } finally {
         updating = false;
      }
   }

   private static final java.util.Set<Long> redstoneOpened = java.util.concurrent.ConcurrentHashMap.newKeySet();

   private static void actuateAround(World w, int cx, int cy, int cz) {
      for (int dy = -1; dy <= 1; dy++) {
         for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
               if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) > 1) {
                  continue;
               }

               int x = cx + dx;
               int y = cy + dy;
               int z = cz + dz;
               int id = w.getBlock(x, y, z);
               boolean pow = isPowered(w, x, y, z);
               long key = pack(x, y, z);
               if (id == REDSTONE_LAMP && pow) {
                  w.setBlock(x, y, z, REDSTONE_LAMP_LIT);
                  if (w.game != null) {
                     w.game.meshNow(x, y, z);
                  }
               } else if (id == REDSTONE_LAMP_LIT && !pow) {
                  w.setBlock(x, y, z, REDSTONE_LAMP);
                  if (w.game != null) {
                     w.game.meshNow(x, y, z);
                  }
               } else if (Shapes.isTrap(id)) {
                  boolean open = Shapes.trapOpen(id);
                  boolean iron = Shapes.trapKind(id) == 6;
                  if (pow) {
                     redstoneOpened.add(key);
                     if (!open) {
                        w.setBlock(x, y, z, Shapes.toggled(id));
                        if (w.game != null) {
                           w.game.meshNow(x, y, z);
                        }
                     }
                  } else if (open && (iron || redstoneOpened.remove(key))) {
                     w.setBlock(x, y, z, Shapes.toggled(id));
                     if (w.game != null) {
                        w.game.meshNow(x, y, z);
                     }
                  }
               } else if (Shapes.isGate(id)) {
                  boolean open = Shapes.gateOpen(id);
                  if (pow) {
                     redstoneOpened.add(key);
                     if (!open) {
                        w.setBlock(x, y, z, Shapes.toggled(id));
                        if (w.game != null) {
                           w.game.meshNow(x, y, z);
                        }
                     }
                  } else if (open && redstoneOpened.remove(key)) {
                     w.setBlock(x, y, z, Shapes.toggled(id));
                     if (w.game != null) {
                        w.game.meshNow(x, y, z);
                     }
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
                           if (w.game != null) {
                              w.game.meshNow(x, y, z);
                              w.game.meshNow(x, y + 1, z);
                           }
                        }
                     } else if (open && redstoneOpened.remove(key)) {
                        w.setBlock(x, y, z, id - 4);
                        w.setBlock(x, y + 1, z, hi - 4);
                        if (w.game != null) {
                           w.game.meshNow(x, y, z);
                           w.game.meshNow(x, y + 1, z);
                        }
                     }
                  }
               } else if (id == Blocks.TNT && pow) {
                  w.setBlock(x, y, z, 0);
                  w.primeTnt(x, y, z, 80);
                  if (w.game != null) {
                     w.game.meshNow(x, y, z);
                  }
               }
            }
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

   /** Called every tick from {@link Game}: button timers, pressure plates under entities, and Slime Block bounce. */
   static void tick(Game g) {
      if (g == null || g.world == null || g.player == null) {
         return;
      }

      World w = g.world;
      // 1. Button auto-release timers
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
               }
            } else {
               e.setValue(rem);
            }
         }
      }

      // 2. Pressure plates around the player
      int px = (int)Math.floor(g.player.x);
      int py = (int)Math.floor(g.player.y);
      int pz = (int)Math.floor(g.player.z);
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

   // ------------------------------------------------------------------ Textures (tiles 643..659)
   static void paint() {
      Tex.blit(T_REDSTONE_BLOCK, TexPaint319.gemBlock(0xD82218, 0xA8140E, 0x6E0A06, 0xFF6852, 643));
      Tex.blit(T_LAMP_OFF, lamp(false));
      Tex.blit(T_LAMP_ON, lamp(true));
      Tex.blit(T_REDSTONE_TORCH, redstoneTorch());
      Tex.blit(T_REDSTONE_WIRE, redstoneWire());
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

   private static int[] redstoneTorch() {
      int[] p = TexPaint319.torch();
      p[5 * 16 + 7] = TexPaint.c(0xD81E14);
      p[5 * 16 + 8] = TexPaint.c(0xB8140C);
      p[4 * 16 + 7] = TexPaint.c(0xFF9888);
      p[4 * 16 + 8] = TexPaint.c(0xFF4232);
      p[3 * 16 + 7] = TexPaint.c(0xFF5848);
      p[3 * 16 + 8] = TexPaint.c(0xD81E14);
      p[2 * 16 + 7] = TexPaint.c(0xB8140C);
      return p;
   }

   private static int[] redstoneWire() {
      int[] p = new int[256];
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            float dx = x - 7.5F;
            float dy = y - 7.5F;
            float r = (float)Math.hypot(dx, dy);
            if (r < 5.2F || (Math.abs(dx) < 2.2F || Math.abs(dy) < 2.2F) && r < 7.2F) {
               int g = r < 2.5F ? 245 : (r < 4.5F ? 210 : 175);
               p[y * 16 + x] = TexPaint.rgb(g, g, g);
            }
         }
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
