package bc;

import java.util.ArrayList;

/**
 * Update 3.15: beds in all sixteen colours (a wooden frame, a blanket in the colour of the wool, a pillow at the head) and banners in
 * sixteen colours (standing on the ground or hanging on a wall). The state is in the block id, like everything else in the game:
 * <pre>
 * bed     BED0 + colour * 8 + (head ? 4 : 0) + facing      facing = the direction from the foot to the head (0 north, 1 south, 2 east, 3 west)
 *         the old red bed (ids 133..140) is colour 14, so old worlds keep their beds
 * banner  STAND0 / WALL0 + colour * 4 + facing             facing = the way the flag looks
 * </pre>
 * The blankets and the cloth are painted in grey and get their colour as the tint of the faces (see {@link Shapes#tint}).
 */
final class Decor315 {
   private Decor315() {
   }

   static final int RED = 14;
   static final int BED0 = 2200;
   static final int BED_ITEM0 = 400;
   static final int STAND0 = 2330;
   static final int WALL0 = 2400;
   static final int BANNER_ITEM0 = 420;
   static final int[] DX = {0, 0, 1, -1};
   static final int[] DZ = {-1, 1, 0, 0};

   // ------------------------------------------------------------------ beds
   static boolean isBed(int id) {
      return id >= 133 && id < 141 || id >= BED0 && id < BED0 + 128;
   }

   static boolean isBedHead(int id) {
      return id >= 137 && id < 141 || id >= BED0 && id < BED0 + 128 && (id - BED0 & 4) != 0;
   }

   /** The direction from the foot of the bed to its head: 0 north, 1 south, 2 east, 3 west. */
   static int bedFacing(int id) {
      return id < 141 ? id - 133 & 3 : id - BED0 & 3;
   }

   static int bedColor(int id) {
      return id < 141 ? RED : id - BED0 >> 3;
   }

   static int foot(int color, int facing) {
      return color == RED ? 133 + facing : BED0 + color * 8 + facing;
   }

   static int head(int color, int facing) {
      return color == RED ? 137 + facing : BED0 + color * 8 + 4 + facing;
   }

   /** The item that a bed of this colour is in the inventory (the red one is the old bed, item 289). */
   static int bedItem(int color) {
      return color == RED ? 289 : BED_ITEM0 + color;
   }

   static boolean isBedItem(int id) {
      return id == 289 || id >= BED_ITEM0 && id < BED_ITEM0 + 16 && id != BED_ITEM0 + RED;
   }

   static int bedItemColor(int id) {
      return id == 289 ? RED : id - BED_ITEM0;
   }

   private static double[][] bedBoxes(int color, boolean head, int facing) {
      int plank = Blocks.tiles[Blocks.PLANKS][0];
      int tint = Color312.fleece(color);
      ArrayList<double[]> b = new ArrayList<>();
      int joint = head ? 4 : 8; // the face that lies against the other half
      b.add(Shapes.tiles(Shapes.hidden(Shapes.px(0, 3, 0, 16, 6, 16), 1 | joint), plank, plank, plank));
      b.add(Shapes.tint(Shapes.tiles(Shapes.hidden(Shapes.px(0, 6, 0, 16, 9, 16), 2 | joint), TexArt315.BED_SIDE, (head ? TexArt315.BED_HEAD0 : TexArt315.BED_FOOT0) + facing, plank), tint));
      double z0 = head ? 13 : 0;
      double z1 = head ? 16 : 3;
      b.add(Shapes.tiles(Shapes.hidden(Shapes.px(0, 0, z0, 3, 3, z1), 1), plank, plank, plank));
      b.add(Shapes.tiles(Shapes.hidden(Shapes.px(13, 0, z0, 16, 3, z1), 1), plank, plank, plank));
      if (head) {
         b.add(Shapes.tiles(Shapes.hidden(Shapes.px(1, 9, 8, 15, 10, 15), 2), TexArt315.PILLOW_SIDE, TexArt315.PILLOW_TOP, TexArt315.PILLOW_SIDE));
      }

      return Shapes.turnAll(b.toArray(new double[0][]), Shapes.turns(1, facing));
   }

   // ------------------------------------------------------------------ banners
   static boolean isStanding(int id) {
      return id >= STAND0 && id < STAND0 + 64;
   }

   static boolean isWallBanner(int id) {
      return id >= WALL0 && id < WALL0 + 64;
   }

   static boolean isBanner(int id) {
      return isStanding(id) || isWallBanner(id);
   }

   static int bannerColor(int id) {
      return (isStanding(id) ? id - STAND0 : id - WALL0) >> 2;
   }

   static int bannerFacing(int id) {
      return (isStanding(id) ? id - STAND0 : id - WALL0) & 3;
   }

   static boolean isBannerItem(int id) {
      return id >= BANNER_ITEM0 && id < BANNER_ITEM0 + 16;
   }

   private static double[][] bannerBoxes(int color, boolean wall, int facing) {
      int plank = Blocks.tiles[Blocks.PLANKS][0];
      int tint = Color312.fleece(color);
      int cloth = TexArt315.CLOTH;
      ArrayList<double[]> b = new ArrayList<>();
      // canonical: the flag looks north (towards -z), the pole / wall is behind it
      if (wall) {
         b.add(Shapes.local(Shapes.tiles(Shapes.px(0, 13, 13, 16, 15, 16), plank, plank, plank)));
         b.add(Shapes.local(Shapes.tint(Shapes.tiles(Shapes.px(0.5, -11, 12, 15.5, 13, 13), cloth, cloth, cloth), tint)));
      } else {
         b.add(Shapes.local(Shapes.tiles(Shapes.px(7, 0, 7, 9, 28, 9), plank, plank, plank)));
         b.add(Shapes.local(Shapes.tiles(Shapes.px(0, 28, 6, 16, 30, 8), plank, plank, plank)));
         b.add(Shapes.local(Shapes.tint(Shapes.tiles(Shapes.px(0.5, 4, 6, 15.5, 28, 7), cloth, cloth, cloth), tint)));
      }

      return Shapes.turnAll(b.toArray(new double[0][]), Shapes.turns(0, facing));
   }

   // ------------------------------------------------------------------ registration
   static void register() {
      double[] bedBox = {0.0, 0.0, 0.0, 1.0, 0.5625, 1.0};
      for (int c = 0; c < 16; c++) {
         for (int head = 0; head < 2; head++) {
            for (int f = 0; f < 4; f++) {
               int id = head == 0 ? foot(c, f) : head(c, f);
               Blocks.def(id, Color312.NAME[c] + " Bed", 0.2F, false, Blocks.tiles[Blocks.WOOL0 + c][0]);
               Blocks.tiles[id][1] = Blocks.tiles[Blocks.PLANKS][0];
               Blocks.opacity[id] = 0;
               Blocks.box[id] = bedBox;
               Blocks.coll[id] = bedBox;
               Blocks.drop[id] = head == 0 ? bedItem(c) : 0;
               Shapes.model(id, bedBoxes(c, head == 1, f));
            }
         }
      }

      for (int c = 0; c < 16; c++) {
         for (int f = 0; f < 4; f++) {
            int stand = STAND0 + c * 4 + f;
            int wall = WALL0 + c * 4 + f;
            for (int id : new int[]{stand, wall}) {
               Blocks.def(id, Color312.NAME[c] + " Banner", 1.0F, false, Blocks.tiles[Blocks.WOOL0 + c][0]);
               Blocks.solid[id] = false;
               Blocks.coll[id] = null;
               Blocks.opacity[id] = 0;
               Blocks.drop[id] = BANNER_ITEM0 + c;
               Blocks.mineClass[id] = 2;
            }

            Blocks.box[stand] = new double[]{0.4375, 0.0, 0.4375, 0.5625, 1.0, 0.5625};
            Shapes.model(stand, bannerBoxes(c, false, f));
            double[] w = Shapes.turn(new double[]{0.0, 0.0, 0.8125, 1.0, 1.0, 0.9375, 0, -1, -1, -1, -1, 0}, Shapes.turns(0, f));
            Blocks.box[wall] = new double[]{w[0], w[1], w[2], w[3], w[4], w[5]};
            Shapes.model(wall, bannerBoxes(c, true, f));
         }
      }
   }

   static void registerItems() {
      for (int c = 0; c < 16; c++) {
         if (c != RED) {
            Items.defTile(BED_ITEM0 + c, Color312.NAME[c] + " Bed", TexArt315.BED_ITEM0 + c);
         }

         Items.defTile(BANNER_ITEM0 + c, Color312.NAME[c] + " Banner", TexArt315.BANNER_ITEM0 + c);
         Items.maxStack[BANNER_ITEM0 + c] = 16;
      }

      Items.tile[289] = TexArt315.BED_ITEM0 + RED;
      Items.name[289] = "Red Bed";
   }

   static void addToCreative(ArrayList<Integer> list) {
      for (int c = 0; c < 16; c++) {
         if (c != RED) {
            list.add(BED_ITEM0 + c);
         }
      }

      for (int c = 0; c < 16; c++) {
         list.add(BANNER_ITEM0 + c);
      }
   }

   // ------------------------------------------------------------------ the world
   static boolean handlesUpdate(int id) {
      return isBanner(id);
   }

   /** A banner needs something to hold it: the ground below a standing one, the wall behind a hanging one. */
   static void update(World w, int x, int y, int z, int id) {
      boolean held;
      if (isStanding(id)) {
         held = Blocks.solid[w.getBlock(x, y - 1, z)];
      } else {
         int f = bannerFacing(id);
         held = Blocks.solid[w.getBlock(x - DX[f], y, z - DZ[f])];
      }

      if (!held) {
         w.setBlock(x, y, z, 0);
         w.dropItem(x + 0.5, y + 0.3, z + 0.5, Blocks.drop[id], 1);
      }
   }

   private static void use(Game g, Stack held) {
      if (!g.player.creative) {
         held.n--;
         if (held.n <= 0) {
            g.inv[g.sel] = null;
         }
      }
   }

   /** Sets a banner down: on top of a block it stands, on the side of a block it hangs; the flag looks towards the player. */
   static boolean placeBanner(Game g, Stack held) {
      World.Hit t = g.target;
      World w = g.world;
      int x = t.x;
      int y = t.y;
      int z = t.z;
      boolean inPlace = Blocks.replaceable[w.getBlock(x, y, z)];
      int nx = inPlace ? 0 : t.nx;
      int ny = inPlace ? 1 : t.ny;
      int nz = inPlace ? 0 : t.nz;
      if (!inPlace) {
         x += nx;
         y += ny;
         z += nz;
      }

      if (y < 1 || y > 254 || !Blocks.replaceable[w.getBlock(x, y, z)] || ny < 0) {
         return false;
      }

      int color = held.id - BANNER_ITEM0;
      int put;
      if (ny > 0 || nx == 0 && nz == 0) {
         if (!Blocks.solid[w.getBlock(x, y - 1, z)]) {
            return false;
         }

         // the flag looks at the player: the opposite of the way he looks
         int look = Shapes.playerFacing(g.player);
         put = STAND0 + color * 4 + (look ^ 1);
      } else {
         int f = Shapes.facingOf(nx, nz);
         if (!Blocks.solid[w.getBlock(x - nx, y, z - nz)]) {
            return false;
         }

         put = WALL0 + color * 4 + f;
      }

      if (!w.setBlock(x, y, z, put)) {
         return false;
      }

      g.meshNow(x, y, z);
      use(g, held);
      return true;
   }

   // ------------------------------------------------------------------ recipes
   /** Three wool of one colour and three planks make a bed of that colour; six wool and a stick a banner. */
   static Stack special(Stack[] grid, int gw, int gh) {
      int minX = gw;
      int minY = gh;
      int maxX = -1;
      int maxY = -1;
      for (int y = 0; y < gh; y++) {
         for (int x = 0; x < gw; x++) {
            if (grid[y * gw + x] != null) {
               minX = Math.min(minX, x);
               minY = Math.min(minY, y);
               maxX = Math.max(maxX, x);
               maxY = Math.max(maxY, y);
            }
         }
      }

      int w = maxX - minX + 1;
      int h = maxY - minY + 1;
      if (w != 3 || h < 2 || h > 3) {
         return null;
      }

      int color = -1;
      StringBuilder sig = new StringBuilder();
      for (int y = 0; y < h; y++) {
         for (int x = 0; x < w; x++) {
            Stack s = grid[(minY + y) * gw + minX + x];
            if (s == null) {
               sig.append('.');
            } else if (s.id >= Blocks.WOOL0 && s.id < Blocks.WOOL0 + 16) {
               int c = s.id - Blocks.WOOL0;
               if (color >= 0 && color != c) {
                  return null;
               }

               color = c;
               sig.append('W');
            } else if (s.id == Items.STICK) {
               sig.append('S');
            } else if (isPlanks(s.id)) {
               sig.append('P');
            } else {
               return null;
            }
         }
      }

      String key = sig.toString();
      if (color < 0) {
         return null;
      }

      if (key.equals("WWWPPP")) {
         return new Stack(bedItem(color), 1);
      }

      return key.equals("WWWWWW.S.") ? new Stack(BANNER_ITEM0 + color, 1) : null;
   }

   private static boolean isPlanks(int id) {
      for (int sp = 0; sp < 6; sp++) {
         if (Blocks313.plank(sp) == id) {
            return true;
         }
      }

      return false;
   }

   static void recipes() {
      for (int c = 0; c < 16; c++) {
         Recipes.fuel.put(bedItem(c), 300);
      }
   }
}
