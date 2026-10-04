package bc;

import java.util.ArrayList;

/**
 * Update 3.15, "models": the block shapes of Minecraft 1.12 that the game was missing - glass panes (clear and in the 16 colours), iron
 * bars, walls, fence gates, trapdoors, carpets and the fences of the other woods. Ids 1500..1735 (the range 1500..1899 belongs to this file).
 * <ul>
 * <li>Panes, bars and walls join their neighbours: the arms are worked out from the blocks around them when the chunk is meshed and when
 * something collides with them or is aimed at (nothing is stored in the world), like the fences always did.</li>
 * <li>Gates and trapdoors keep their state in the id: gate = {@link #GATE0} + kind * 8 + open * 4 + facing; trapdoor = {@link #TRAP0} + kind * 16 +
 * top * 8 + open * 4 + facing. Kind is the wood (0 oak, 1 birch, 2 spruce, 3 jungle, 4 acacia, 5 dark oak, like {@link Blocks313#plank}); trapdoors
 * also have kind 6, iron (it only opens with redstone). Facing is 0 north (-z), 1 south (+z), 2 east (+x), 3 west (-x).</li>
 * </ul>
 */
final class Shapes {
   private Shapes() {
   }

   /** The mesher kind of everything that is built from several boxes ({@link Mesher} calls {@link #mesh}). */
   static final int K_SHAPE = 10;
   static final int PANE0 = 1500;
   /** The clear pane and the 16 stained ones (colour order of the wool). */
   static final int PANES = 17;
   static final int BARS = 1517;
   static final int WALL0 = 1520;
   static final int WALLS = 7;
   /** Fences of spruce, birch, jungle, acacia and dark oak (the oak one is the old {@link Blocks#FENCE}). */
   static final int FENCE0 = 1530;
   static final int FENCES = 5;
   static final int GATE0 = 1540;
   static final int GATE_KINDS = 6;
   static final int TRAP0 = 1600;
   static final int TRAP_KINDS = 7;
   static final int CARPET0 = 1720;
   static final int LAST = 1735;
   static final String[] SPECIES = {"Oak", "Birch", "Spruce", "Jungle", "Acacia", "Dark Oak"};
   /** What the walls are made of (the block whose picture they wear). */
   static final int[] WALL_BASE = {Blocks.COBBLE, Blocks.MOSSY_COBBLE, 27, 16, Blocks.SANDSTONE, Blocks313.RED_SANDSTONE, Blocks.NETHER_BRICKS};
   static final String[] WALL_NAME = {"Cobblestone", "Mossy Cobblestone", "Stone Brick", "Brick", "Sandstone", "Red Sandstone", "Nether Brick"};
   /** North, south, east, west as (dx, dz). */
   static final int[][] DIRS = {{0, -1}, {0, 1}, {1, 0}, {-1, 0}};
   /** The face of a box that looks the way DIRS says. */
   static final int[] DIR_FACE = {2, 3, 4, 5};
   private static final boolean[] DYN = new boolean[Blocks.MAX];
   private static final double[] POST_THIN = {0.4375, 0.0, 0.4375, 0.5625, 1.0, 0.5625};

   /** Looks at the block at an offset from the shape (a chunk's scratch array when meshing, the world otherwise). */
   interface Nbr {
      int at(int dx, int dy, int dz);
   }

   // ------------------------------------------------------------------ what is what
   static boolean isPane(int id) {
      return id >= PANE0 && id < PANE0 + PANES;
   }

   static boolean isStainedPane(int id) {
      return id > PANE0 && id < PANE0 + PANES;
   }

   static boolean isBars(int id) {
      return id == BARS;
   }

   static boolean isThin(int id) {
      return id >= PANE0 && id <= BARS;
   }

   static boolean isWall(int id) {
      return id >= WALL0 && id < WALL0 + WALLS;
   }

   static boolean isWoodFence(int id) {
      return id >= FENCE0 && id < FENCE0 + FENCES;
   }

   static boolean isGate(int id) {
      return id >= GATE0 && id < GATE0 + GATE_KINDS * 8;
   }

   static boolean isTrap(int id) {
      return id >= TRAP0 && id < TRAP0 + TRAP_KINDS * 16;
   }

   static boolean isCarpet(int id) {
      return id >= CARPET0 && id < CARPET0 + 16;
   }

   /** Everything whose boxes depend on the blocks around it. */
   static boolean isDyn(int id) {
      return id >= 0 && id < DYN.length && DYN[id];
   }

   /** Blocks that have to be looked at when a neighbour changes. */
   static boolean handlesUpdate(int id) {
      return isCarpet(id);
   }

   // ------------------------------------------------------------------ gates and trapdoors: the state is in the id
   static int gateKind(int id) {
      return (id - GATE0) / 8;
   }

   static boolean gateOpen(int id) {
      return (id - GATE0 & 4) != 0;
   }

   static int gateFacing(int id) {
      return id - GATE0 & 3;
   }

   static int gate(int kind, boolean open, int facing) {
      return GATE0 + kind * 8 + (open ? 4 : 0) + facing;
   }

   /** A gate whose leaf runs along x (it faces north or south). */
   static boolean gateAlongX(int id) {
      return gateFacing(id) < 2;
   }

   static int trapKind(int id) {
      return id - TRAP0 >> 4;
   }

   static boolean trapOpen(int id) {
      return (id - TRAP0 & 4) != 0;
   }

   static boolean trapTop(int id) {
      return (id - TRAP0 & 8) != 0;
   }

   static int trapFacing(int id) {
      return id - TRAP0 & 3;
   }

   static int trap(int kind, boolean top, boolean open, int facing) {
      return TRAP0 + kind * 16 + (top ? 8 : 0) + (open ? 4 : 0) + facing;
   }

   /** The same gate or trapdoor with the other open state; any other id comes back unchanged. */
   static int toggled(int id) {
      if (isGate(id)) {
         return GATE0 + (id - GATE0 ^ 4);
      }

      return isTrap(id) ? TRAP0 + (id - TRAP0 ^ 4) : id;
   }

   /** Open or close (by hand a gate and a wooden trapdoor; iron ones only by redstone). */
   static boolean handToggles(int id) {
      return isGate(id) || isTrap(id) && trapKind(id) != 6;
   }

   /** A fence arm in direction dir (0 n, 1 s, 2 e, 3 w) reaches a block of this id. */
   static boolean fenceLinks(int id, int dir) {
      return isGate(id) && gateAlongX(id) == dir >= 2;
   }

   // ------------------------------------------------------------------ links
   /** A block a pane or a wall can attach to: a full cube, not leaves. */
   static boolean fullSolid(int id) {
      return Blocks.solid[id] && Blocks.kind[id] == 0 && !Blocks.isLeaves(id) && !Blocks.isFluid(id);
   }

   static boolean paneLink(int id) {
      return isThin(id) || fullSolid(id);
   }

   static boolean wallLink(int id, int dir) {
      return isWall(id) || fullSolid(id) || fenceLinks(id, dir);
   }

   // ------------------------------------------------------------------ geometry
   private static double[] bx(double x0, double y0, double z0, double x1, double y1, double z1, int hide) {
      return new double[]{x0, y0, z0, x1, y1, z1, hide};
   }

   /** The arm of a thin block (pane, bars) towards dir. */
   private static double[] thinArm(int dir, int hide) {
      switch (dir) {
         case 0:
            return bx(0.4375, 0.0, 0.0, 0.5625, 1.0, 0.4375, hide | 1 << 3);
         case 1:
            return bx(0.4375, 0.0, 0.5625, 0.5625, 1.0, 1.0, hide | 1 << 2);
         case 2:
            return bx(0.5625, 0.0, 0.4375, 1.0, 1.0, 0.5625, hide | 1 << 5);
         default:
            return bx(0.0, 0.0, 0.4375, 0.4375, 1.0, 0.5625, hide | 1 << 4);
      }
   }

   /** Post and arms of a pane or of iron bars. The 7th number of every box is the mask of faces that are inside the whole (bit = face). */
   static double[][] thinBoxes(Nbr n, boolean collision) {
      boolean[] c = new boolean[4];
      boolean any = false;
      for (int d = 0; d < 4; d++) {
         c[d] = paneLink(n.at(DIRS[d][0], 0, DIRS[d][1]));
         any |= c[d];
      }

      if (!any) {
         if (collision) {
            return new double[][]{POST_THIN};
         }

         c[0] = c[1] = c[2] = c[3] = true; // alone: the little "+" of Minecraft 1.12
      }

      ArrayList<double[]> out = new ArrayList<>();
      int postHide = 0;
      for (int d = 0; d < 4; d++) {
         if (c[d]) {
            postHide |= 1 << DIR_FACE[d];
         }
      }

      out.add(bx(POST_THIN[0], 0.0, POST_THIN[2], POST_THIN[3], 1.0, POST_THIN[5], postHide));
      for (int d = 0; d < 4; d++) {
         if (c[d]) {
            boolean linked = paneLink(n.at(DIRS[d][0], 0, DIRS[d][1]));
            out.add(thinArm(d, linked ? 1 << DIR_FACE[d] : 0));
         }
      }

      return out.toArray(new double[0][]);
   }

   /** Post and arms of a wall. Collision is half a block taller than what is drawn (nothing jumps over a wall). */
   static double[][] wallBoxes(Nbr n, boolean collision) {
      boolean[] c = new boolean[4];
      int count = 0;
      for (int d = 0; d < 4; d++) {
         c[d] = wallLink(n.at(DIRS[d][0], 0, DIRS[d][1]), d);
         if (c[d]) {
            count++;
         }
      }

      boolean straightNS = c[0] && c[1] && !c[2] && !c[3];
      boolean straightEW = c[2] && c[3] && !c[0] && !c[1];
      boolean up = !(straightNS || straightEW);
      double top = collision ? 1.5 : 1.0;
      double armTop = collision ? 1.5 : 0.875;
      ArrayList<double[]> out = new ArrayList<>();
      if (!up) {
         // a straight run needs no post: one long arm
         if (straightNS) {
            out.add(bx(0.3125, 0.0, 0.0, 0.6875, armTop, 1.0, 0));
         } else {
            out.add(bx(0.0, 0.0, 0.3125, 1.0, armTop, 0.6875, 0));
         }

         return out.toArray(new double[0][]);
      }

      int postHide = 0;
      for (int d = 0; d < 4; d++) {
         if (c[d]) {
            postHide |= 1 << DIR_FACE[d];
         }
      }

      out.add(bx(0.25, 0.0, 0.25, 0.75, top, 0.75, postHide));
      if (c[0]) {
         out.add(bx(0.3125, 0.0, 0.0, 0.6875, armTop, 0.25, 1 << 3));
      }

      if (c[1]) {
         out.add(bx(0.3125, 0.0, 0.75, 0.6875, armTop, 1.0, 1 << 2));
      }

      if (c[2]) {
         out.add(bx(0.75, 0.0, 0.3125, 1.0, armTop, 0.6875, 1 << 5));
      }

      if (c[3]) {
         out.add(bx(0.0, 0.0, 0.3125, 0.25, armTop, 0.6875, 1 << 4));
      }

      return out.toArray(new double[0][]);
   }

   /** Swaps x and z (a gate along z is the gate along x turned). */
   private static double[] turn(double[] b) {
      return new double[]{b[2], b[1], b[0], b[5], b[4], b[3], 0};
   }

   /** The boxes a gate is drawn with. */
   static double[][] gateBoxes(int id) {
      boolean open = gateOpen(id);
      boolean alongX = gateAlongX(id);
      int f = gateFacing(id);
      double s = f == 0 || f == 3 ? -1.0 : 1.0; // the side the leaves swing to
      ArrayList<double[]> out = new ArrayList<>();
      double z0 = 0.4375;
      double z1 = 0.5625;
      out.add(bx(0.0, 0.3125, z0, 0.125, 1.0, z1, 0));
      out.add(bx(0.875, 0.3125, z0, 1.0, 1.0, z1, 0));
      if (!open) {
         out.add(bx(0.125, 0.375, z0, 0.875, 0.5625, z1, 0));
         out.add(bx(0.125, 0.75, z0, 0.875, 0.9375, z1, 0));
         out.add(bx(0.375, 0.5625, z0, 0.625, 0.75, z1, 0));
      } else {
         for (int side = 0; side < 2; side++) {
            double xa = side == 0 ? 0.0 : 0.875;
            double xb = side == 0 ? 0.125 : 1.0;
            double za = s > 0 ? z1 : 0.0625;
            double zb = s > 0 ? 0.9375 : z0;
            out.add(bx(xa, 0.375, za, xb, 0.5625, zb, 0));
            out.add(bx(xa, 0.75, za, xb, 0.9375, zb, 0));
            double zc = s > 0 ? 0.8125 : 0.0625;
            double zd = s > 0 ? 0.9375 : 0.1875;
            out.add(bx(xa, 0.5625, zc, xb, 0.75, zd, 0));
         }
      }

      if (!alongX) {
         for (int i = 0; i < out.size(); i++) {
            out.set(i, turn(out.get(i)));
         }
      }

      return out.toArray(new double[0][]);
   }

   /** What a gate blocks: the whole leaf while it is shut, the two posts while it is open (all of it a block and a half high). */
   static double[][] gateCollision(int id) {
      boolean alongX = gateAlongX(id);
      double[][] out;
      if (!gateOpen(id)) {
         out = new double[][]{{0.0, 0.0, 0.375, 1.0, 1.5, 0.625}};
      } else {
         out = new double[][]{{0.0, 0.0, 0.375, 0.125, 1.5, 0.625}, {0.875, 0.0, 0.375, 1.0, 1.5, 0.625}};
      }

      if (!alongX) {
         for (int i = 0; i < out.length; i++) {
            out[i] = new double[]{out[i][2], out[i][1], out[i][0], out[i][5], out[i][4], out[i][3]};
         }
      }

      return out;
   }

   /** The one box of a trapdoor. */
   static double[] trapBox(int id) {
      double t = 0.1875;
      if (!trapOpen(id)) {
         return trapTop(id) ? new double[]{0.0, 1.0 - t, 0.0, 1.0, 1.0, 1.0} : new double[]{0.0, 0.0, 0.0, 1.0, t, 1.0};
      }

      switch (trapFacing(id)) {
         case 0:
            return new double[]{0.0, 0.0, 0.0, 1.0, 1.0, t};
         case 1:
            return new double[]{0.0, 0.0, 1.0 - t, 1.0, 1.0, 1.0};
         case 2:
            return new double[]{1.0 - t, 0.0, 0.0, 1.0, 1.0, 1.0};
         default:
            return new double[]{0.0, 0.0, 0.0, t, 1.0, 1.0};
      }
   }

   private static Nbr world(World w, int x, int y, int z) {
      return (dx, dy, dz) -> w.getBlock(x + dx, y + dy, z + dz);
   }

   /** The boxes something collides with, for a block that is {@link #isDyn}. */
   static double[][] collisionBoxes(World w, int x, int y, int z, int id) {
      return isThin(id) ? thinBoxes(world(w, x, y, z), true) : wallBoxes(world(w, x, y, z), true);
   }

   /** The boxes a ray is tested against (what the outline is drawn round). */
   static double[][] selectionBoxes(World w, int x, int y, int z, int id) {
      return isThin(id) ? thinBoxes(world(w, x, y, z), false) : wallBoxes(world(w, x, y, z), false);
   }

   /** The boxes drawn in the inventory and in the hand (a wall is a post with two arms, a fence gate is the closed one ...). */
   static double[][] iconBoxes(int id) {
      if (isWall(id)) {
         return new double[][]{{0.25, 0.0, 0.25, 0.75, 1.0, 0.75}, {0.0, 0.0, 0.3125, 0.25, 0.875, 0.6875}, {0.75, 0.0, 0.3125, 1.0, 0.875, 0.6875}};
      }

      if (isGate(id)) {
         double[][] g = gateBoxes(gate(gateKind(id), false, 0));
         double[][] out = new double[g.length][];
         for (int i = 0; i < g.length; i++) {
            out[i] = new double[]{g[i][0], g[i][1], 0.0, g[i][3], g[i][4], 1.0};
            out[i][2] = 0.4375;
            out[i][5] = 0.5625;
         }

         return out;
      }

      if (isTrap(id)) {
         return new double[][]{trapBox(trap(trapKind(id), false, false, 0))};
      }

      return null;
   }

   /** Items that are drawn as a flat picture in the hand and in the inventory. */
   static boolean flatItem(int id) {
      return isThin(id);
   }

   // ------------------------------------------------------------------ meshing
   private static int tileOf(int id, int face) {
      if (isThin(id) && face < 2) {
         return isBars(id) ? TexArt315.BARS_EDGE : TexArt315.EDGE0 + (id - PANE0);
      }

      return Blocks.tiles[id][face];
   }

   /** Draws a block of kind {@link #K_SHAPE} (called by the mesher with the chunk's scratch space; x, y, z are chunk-local). */
   static void mesh(Mesher.Scratch s, int x, int y, int z, int id) {
      Nbr nbr = (dx, dy, dz) -> s.B[Mesher.ci(x + 1 + dx, y + 1 + dy, z + 1 + dz)] & 0xFFFF;
      double[][] boxes;
      if (isThin(id)) {
         boxes = thinBoxes(nbr, false);
      } else if (isWall(id)) {
         boxes = wallBoxes(nbr, false);
      } else if (isGate(id)) {
         boxes = gateBoxes(id);
      } else {
         return;
      }

      boolean translucent = isStainedPane(id);
      for (double[] b : boxes) {
         emitBox(s, x, y, z, id, b, (int)b[6], translucent ? s.t : s.o, translucent ? Color312.GLASS_ALPHA : 1.0F);
      }
   }

   /** One box of a shape: the six faces (except the hidden ones) with the texture laid over the whole block, so that the parts line up. */
   static void emitBox(Mesher.Scratch s, int x, int y, int z, int id, double[] b, int hide, Mesher.FB buf, float alpha) {
      int cell = Mesher.ci(x + 1, y + 1, z + 1);
      int own = s.L[cell] & 255;
      float[] uv = new float[2];
      for (int f = 0; f < 6; f++) {
         if ((hide >> f & 1) != 0) {
            continue;
         }

         int[] fn = Mesher.FN[f];
         int axis = f < 2 ? 1 : (f < 4 ? 2 : 0);
         boolean positive = fn[axis] > 0;
         double edge = positive ? b[3 + axis] : b[axis];
         int nb = s.B[Mesher.ci(x + 1 + fn[0], y + 1 + fn[1], z + 1 + fn[2])] & 0xFFFF;
         if ((positive ? edge >= 1.0 : edge <= 0.0) && Blocks.opaque[nb]) {
            continue; // lies against a solid block
         }

         int light = own;
         if (!Blocks.opaque[nb] && !Mesher.isPartial(nb)) {
            light = s.L[Mesher.ci(x + 1 + fn[0], y + 1 + fn[1], z + 1 + fn[2])] & 255;
         }

         float sky = light >> 4 & 15;
         float blk = light & 15;
         for (int i = 0; i < 4; i++) {
            int[] corner = Mesher.FC[f][i];
            float px = (float)(corner[0] == 0 ? b[0] : b[3]);
            float py = (float)(corner[1] == 0 ? b[1] : b[4]);
            float pz = (float)(corner[2] == 0 ? b[2] : b[5]);
            Mesher.uvFor(f, px, py, pz, uv);
            s.qx[i] = x + px;
            s.qy[i] = y + py;
            s.qz[i] = z + pz;
            s.qu[i] = uv[0];
            s.qv[i] = uv[1];
            float shade = Mesher.SHADE[f];
            s.qr[i] = shade;
            s.qg[i] = shade;
            s.qb[i] = shade;
            s.qa[i] = alpha;
            s.qm[i] = Mesher.lu(sky);
            s.ql[i] = Mesher.lu(blk);
         }

         s.ao[0] = s.ao[1] = s.ao[2] = s.ao[3] = 1.0F;
         Mesher.emit(s, buf, tileOf(id, f), false);
      }
   }

   // ------------------------------------------------------------------ the world: use, place, update
   /** Right click: a gate or a wooden trapdoor opens or closes. */
   static boolean use(Game g, int x, int y, int z, int id) {
      if (!handToggles(id)) {
         return false;
      }

      g.world.setBlock(x, y, z, toggled(id));
      g.meshNow(x, y, z);
      return true;
   }

   /** What a block does when something next to it changes: a carpet whose floor is gone comes loose. */
   static void update(World w, int x, int y, int z, int id) {
      if (isCarpet(id) && !Blocks.solid[w.getBlock(x, y - 1, z)]) {
         w.setBlock(x, y, z, 0);
         w.dropItem(x + 0.5, y + 0.3, z + 0.5, id, 1);
      }
   }

   static boolean placesSpecially(int id) {
      return isGate(id) || isTrap(id) || isCarpet(id);
   }

   /** The way the player looks, as a facing (0 north, 1 south, 2 east, 3 west). */
   static int playerFacing(Player p) {
      double lx = -Math.sin(Math.toRadians(p.yaw));
      double lz = Math.cos(Math.toRadians(p.yaw));
      return Math.abs(lz) >= Math.abs(lx) ? (lz > 0.0 ? 1 : 0) : (lx > 0.0 ? 2 : 3);
   }

   /** The facing that points from a cell to the neighbour at (dx, dz). */
   static int facingOf(int dx, int dz) {
      return dz < 0 ? 0 : (dz > 0 ? 1 : (dx > 0 ? 2 : 3));
   }

   private static boolean hitsPlayer(Game g, int x, int y, int z, int id) {
      double[][] boxes = Blocks.collMulti[id] != null ? Blocks.collMulti[id] : (Blocks.coll[id] != null ? new double[][]{Blocks.coll[id]} : new double[0][]);
      double[] p = g.player.bb;
      for (double[] b : boxes) {
         if (p[3] > x + b[0] && p[0] < x + b[3] && p[4] > y + b[1] && p[1] < y + b[4] && p[5] > z + b[2] && p[2] < z + b[5]) {
            return true;
         }
      }

      return false;
   }

   /** Puts down a gate, a trapdoor or a carpet the way the player aims (facing, upper or lower half, needs a floor). */
   static boolean place(Game g, Stack held) {
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

      if (y < 0 || y > 255 || !Blocks.replaceable[w.getBlock(x, y, z)]) {
         return false;
      }

      int id = held.id;
      int put = id;
      if (isGate(id)) {
         put = gate(gateKind(id), false, playerFacing(g.player));
      } else if (isTrap(id)) {
         if (ny == 0) {
            put = trap(trapKind(id), t.fy > 0.5, false, facingOf(-nx, -nz));
         } else {
            put = trap(trapKind(id), ny == -1, false, playerFacing(g.player));
         }
      } else if (!Blocks.solid[w.getBlock(x, y - 1, z)]) {
         return false;
      }

      if (hitsPlayer(g, x, y, z, put) || !w.setBlock(x, y, z, put)) {
         return false;
      }

      g.meshNow(x, y, z);
      if (!g.player.creative) {
         held.n--;
         if (held.n <= 0) {
            g.inv[g.sel] = null;
         }
      }

      return true;
   }

   // ------------------------------------------------------------------ recipes
   private static int speciesOfPlanks(int id) {
      for (int sp = 0; sp < 6; sp++) {
         if (Blocks313.plank(sp) == id) {
            return sp;
         }
      }

      return -1;
   }

   /**
    * The crafting recipes that depend on the kind of wood (Recipes.find asks this first): four planks and two sticks make three fences of that
    * wood, a gate is two planks and four sticks, a trapdoor six planks (two trapdoors). Planks of different woods do not mix.
    */
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
      if (w != 3 || h != 2) {
         return null;
      }

      StringBuilder sig = new StringBuilder();
      int species = -2;
      for (int y = 0; y < h; y++) {
         for (int x = 0; x < w; x++) {
            Stack s = grid[(minY + y) * gw + minX + x];
            if (s == null) {
               sig.append('.');
            } else if (s.id == Items.STICK) {
               sig.append('S');
            } else if (speciesOfPlanks(s.id) >= 0) {
               int sp = speciesOfPlanks(s.id);
               if (species != -2 && species != sp) {
                  return null;
               }

               species = sp;
               sig.append('P');
            } else {
               return null;
            }
         }
      }

      String key = sig.toString();
      if (species < 0) {
         return null;
      }

      if (key.equals("PSPPSP")) {
         return new Stack(species == 0 ? Blocks.FENCE : FENCE0 + species - 1, 3);
      }

      if (key.equals("SPSSPS")) {
         return new Stack(gate(species, false, 0), 1);
      }

      return key.equals("PPPPPP") ? new Stack(trap(species, false, false, 0), 2) : null;
   }

   static void recipes() {
      Recipes.shaped(PANE0, 16, new String[]{"GGG", "GGG"}, 'G', Blocks.GLASS);
      for (int c = 0; c < 16; c++) {
         Recipes.shaped(PANE0 + 1 + c, 16, new String[]{"GGG", "GGG"}, 'G', Blocks.GLASS0 + c);
         Recipes.shaped(CARPET0 + c, 3, new String[]{"WW"}, 'W', Blocks.WOOL0 + c);
      }

      Recipes.shaped(BARS, 16, new String[]{"III", "III"}, 'I', Items.IRON_INGOT);
      for (int i = 0; i < WALLS; i++) {
         Recipes.shaped(WALL0 + i, 6, new String[]{"BBB", "BBB"}, 'B', WALL_BASE[i]);
      }

      Recipes.shaped(trap(6, false, false, 0), 1, new String[]{"II", "II"}, 'I', Items.IRON_INGOT);
      for (int sp = 0; sp < 6; sp++) {
         Recipes.fuel.put(gate(sp, false, 0), 300);
         Recipes.fuel.put(trap(sp, false, false, 0), 300);
      }

      for (int i = 0; i < FENCES; i++) {
         Recipes.fuel.put(FENCE0 + i, 300);
      }
   }

   // ------------------------------------------------------------------ registration
   private static void bound(int id, double[][] boxes) {
      double[] u = {1.0, 1.0, 1.0, 0.0, 0.0, 0.0};
      for (double[] b : boxes) {
         for (int i = 0; i < 3; i++) {
            u[i] = Math.min(u[i], b[i]);
            u[i + 3] = Math.max(u[i + 3], b[i + 3]);
         }
      }

      Blocks.box[id] = u;
   }

   private static void shape(int id, String name, float hardness, int tile) {
      Blocks.def(id, name, hardness, false, tile);
      Blocks.kind[id] = K_SHAPE;
      Blocks.opacity[id] = 0;
   }

   private static void dyn(int id, double[] post) {
      DYN[id] = true;
      Blocks.box[id] = post;
      Blocks.coll[id] = post;
   }

   private static void tool(int id, int mineClass, boolean needTool) {
      Blocks.mineClass[id] = mineClass;
      Blocks.needTool[id] = needTool;
      Blocks.harvestLevel[id] = 0;
   }

   static void register() {
      for (int i = 0; i < PANES; i++) {
         int id = PANE0 + i;
         shape(id, i == 0 ? "Glass Pane" : Color312.NAME[i - 1] + " Stained Glass Pane", 0.3F, i == 0 ? Blocks.tiles[Blocks.GLASS][2] : Color312.T_GLASS0 + i - 1);
         dyn(id, POST_THIN);
      }

      shape(BARS, "Iron Bars", 5.0F, TexArt315.BARS);
      dyn(BARS, POST_THIN);
      tool(BARS, 1, true);
      for (int i = 0; i < WALLS; i++) {
         int id = WALL0 + i;
         shape(id, WALL_NAME[i] + " Wall", 2.0F, Blocks.tiles[WALL_BASE[i]][2]);
         dyn(id, new double[]{0.25, 0.0, 0.25, 0.75, 1.5, 0.75});
         tool(id, 1, true);
      }

      for (int i = 0; i < FENCES; i++) {
         int id = FENCE0 + i;
         int species = i + 1; // 1 birch ... 5 dark oak (the oak one is the old fence)
         Blocks.def(id, SPECIES[species] + " Fence", 2.0F, false, Blocks.tiles[Blocks313.plank(species)][0]);
         Blocks.kind[id] = Blocks.K_FENCE;
         Blocks.opacity[id] = 0;
         Blocks.box[id] = new double[]{0.375, 0.0, 0.375, 0.625, 1.0, 0.625};
         Blocks.coll[id] = new double[]{0.375, 0.0, 0.375, 0.625, 1.5, 0.625};
         tool(id, 2, false);
      }

      for (int kind = 0; kind < GATE_KINDS; kind++) {
         int plank = Blocks.tiles[Blocks313.plank(kind)][0];
         for (int state = 0; state < 8; state++) {
            int id = GATE0 + kind * 8 + state;
            shape(id, SPECIES[kind] + " Fence Gate", 2.0F, plank);
            boolean open = (state & 4) != 0;
            double[][] mesh = gateBoxes(id);
            bound(id, mesh);
            double[][] coll = gateCollision(id);
            Blocks.coll[id] = coll[0];
            Blocks.collMulti[id] = coll.length > 1 ? coll : null;
            Blocks.solid[id] = !open;
            Blocks.drop[id] = gate(kind, false, 0);
            tool(id, 2, false);
         }
      }

      for (int kind = 0; kind < TRAP_KINDS; kind++) {
         for (int state = 0; state < 16; state++) {
            int id = TRAP0 + kind * 16 + state;
            String name = kind == 6 ? "Iron Trapdoor" : SPECIES[kind] + " Trapdoor";
            Blocks.def(id, name, kind == 6 ? 5.0F : 3.0F, false, TexArt315.TRAP0 + kind);
            double[] b = trapBox(id);
            Blocks.kind[id] = Blocks.K_BOX;
            Blocks.box[id] = b;
            Blocks.coll[id] = b;
            Blocks.opacity[id] = 0;
            Blocks.drop[id] = trap(kind, false, false, 0);
            if (kind == 6) {
               tool(id, 1, true);
            } else {
               tool(id, 2, false);
            }
         }
      }

      for (int c = 0; c < 16; c++) {
         int id = CARPET0 + c;
         Blocks.def(id, Color312.NAME[c] + " Carpet", 0.1F, false, Blocks.tiles[Blocks.WOOL0 + c][0]);
         Blocks.kind[id] = Blocks.K_BOX;
         Blocks.box[id] = new double[]{0.0, 0.0, 0.0, 1.0, 0.0625, 1.0};
         Blocks.coll[id] = Blocks.box[id];
         Blocks.opacity[id] = 0;
      }
   }

   /** The new blocks the creative inventory offers (one of every gate and trapdoor: the closed, bottom, north one). */
   static void addToCreative(ArrayList<Integer> list) {
      for (int i = 0; i < PANES; i++) {
         list.add(PANE0 + i);
      }

      list.add(BARS);
      for (int i = 0; i < WALLS; i++) {
         list.add(WALL0 + i);
      }

      for (int i = 0; i < FENCES; i++) {
         list.add(FENCE0 + i);
      }

      for (int kind = 0; kind < GATE_KINDS; kind++) {
         list.add(gate(kind, false, 0));
      }

      for (int kind = 0; kind < TRAP_KINDS; kind++) {
         list.add(trap(kind, false, false, 0));
      }

      for (int c = 0; c < 16; c++) {
         list.add(CARPET0 + c);
      }
   }
}
