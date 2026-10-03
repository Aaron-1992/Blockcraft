package bc;

/**
 * Update 3.12: nether portals. A frame of obsidian (4 x 5 up to 23 x 23, corners optional) lit from the inside with fire becomes a
 * portal; stand in it for four seconds (a creative player goes at once) and you come out of a portal in the other dimension, at
 * 1/8 (Overworld to Nether) or 8 times (back) the distance from the centre. A portal that is not there yet is built for you.
 */
final class Portals {
   static final int MIN_W = 2;
   static final int MIN_H = 3;
   static final int MAX_W = 21;
   static final int MAX_H = 21;
   /** Ticks you stand in the film before you are taken away (survival). */
   static final int WAIT = 80;

   private Portals() {
   }

   /** Where a frame is: its interior starts at (x0, y0, z0) and is w wide (along x for axis 0, along z for axis 1) and h high. */
   static final class Frame {
      int axis;
      int x0;
      int y0;
      int z0;
      int w;
      int h;
   }

   static boolean inside(int id) {
      return id == 0 || id == Blocks.FIRE || Blocks.isPortal(id);
   }

   /** The complete obsidian frame (nothing but air, fire or portal inside it) around the cell (x, y, z) in the plane of {@code axis}, or null. */
   static Frame find(World w, int x, int y, int z, int axis) {
      int dx = axis == 0 ? 1 : 0;
      int dz = axis == 0 ? 0 : 1;
      if (!inside(w.getBlock(x, y, z))) {
         return null;
      }

      int by = y;
      while (by > 1 && inside(w.getBlock(x, by - 1, z)) && y - by < MAX_H) {
         by--;
      }

      if (w.getBlock(x, by - 1, z) != Blocks.OBSIDIAN) {
         return null;
      }

      int lx = x;
      int lz = z;
      int steps = 0;
      while (inside(w.getBlock(lx - dx, by, lz - dz)) && steps < MAX_W) {
         lx -= dx;
         lz -= dz;
         steps++;
      }

      if (w.getBlock(lx - dx, by, lz - dz) != Blocks.OBSIDIAN) {
         return null;
      }

      int width = 0;
      while (width <= MAX_W && inside(w.getBlock(lx + dx * width, by, lz + dz * width))) {
         width++;
      }

      if (width < MIN_W || width > MAX_W || w.getBlock(lx + dx * width, by, lz + dz * width) != Blocks.OBSIDIAN) {
         return null;
      }

      int height = 0;
      while (height <= MAX_H) {
         if (w.getBlock(lx - dx, by + height, lz - dz) != Blocks.OBSIDIAN || w.getBlock(lx + dx * width, by + height, lz + dz * width) != Blocks.OBSIDIAN) {
            break;
         }

         boolean row = true;
         for (int i = 0; i < width && row; i++) {
            row = inside(w.getBlock(lx + dx * i, by + height, lz + dz * i));
         }

         if (!row) {
            break;
         }

         height++;
      }

      if (height < MIN_H || height > MAX_H) {
         return null;
      }

      for (int i = 0; i < width; i++) {
         if (w.getBlock(lx + dx * i, by + height, lz + dz * i) != Blocks.OBSIDIAN || w.getBlock(lx + dx * i, by - 1, lz + dz * i) != Blocks.OBSIDIAN) {
            return null;
         }
      }

      Frame f = new Frame();
      f.axis = axis;
      f.x0 = lx;
      f.y0 = by;
      f.z0 = lz;
      f.w = width;
      f.h = height;
      return f;
   }

   /** Fills a frame with the portal film. */
   static void fill(World w, Frame f) {
      int dx = f.axis == 0 ? 1 : 0;
      int dz = f.axis == 0 ? 0 : 1;
      int id = Blocks.PORTAL_X + f.axis;
      for (int i = 0; i < f.w; i++) {
         for (int j = 0; j < f.h; j++) {
            w.setBlock(f.x0 + dx * i, f.y0 + j, f.z0 + dz * i, id);
         }
      }
   }

   /** Fire has just appeared at (x, y, z): if it is inside a complete frame, the frame comes alive. */
   static boolean tryLight(World w, int x, int y, int z) {
      if (w.dim == Dim.END) {
         return false;
      }

      for (int axis = 0; axis < 2; axis++) {
         Frame f = find(w, x, y, z, axis);
         if (f != null) {
            fill(w, f);
            return true;
         }
      }

      return false;
   }

   private static boolean frameOrFilm(int id) {
      return id == Blocks.OBSIDIAN || Blocks.isPortal(id);
   }

   /** Does this film block still have film or obsidian on all four sides in its plane? (If not, it fades away: the frame was broken.) */
   static boolean intact(World w, int x, int y, int z) {
      int id = w.getBlock(x, y, z);
      if (!Blocks.isPortal(id)) {
         return true;
      }

      int dx = id == Blocks.PORTAL_X ? 1 : 0;
      int dz = id == Blocks.PORTAL_X ? 0 : 1;
      return frameOrFilm(w.getBlock(x, y + 1, z)) && frameOrFilm(w.getBlock(x, y - 1, z)) && frameOrFilm(w.getBlock(x - dx, y, z - dz)) && frameOrFilm(w.getBlock(x + dx, y, z + dz));
   }

   /** The cell of a portal film the box touches (null: none). Returns {x, y, z}. */
   static int[] touching(World w, double[] bb) {
      for (int y = (int)Math.floor(bb[1]); y <= (int)Math.floor(bb[4]); y++) {
         for (int x = (int)Math.floor(bb[0]); x <= (int)Math.floor(bb[3]); x++) {
            for (int z = (int)Math.floor(bb[2]); z <= (int)Math.floor(bb[5]); z++) {
               int id = w.getBlock(x, y, z);
               if (Blocks.isPortal(id)) {
                  double[] b = Blocks.box[id];
                  if (bb[0] < x + b[3] && bb[3] > x + b[0] && bb[1] < y + b[4] && bb[4] > y + b[1] && bb[2] < z + b[5] && bb[5] > z + b[2]) {
                     return new int[]{x, y, z};
                  }
               }
            }
         }
      }

      return null;
   }

   /** Every tick: is the player in a portal film? After {@link #WAIT} ticks (at once in creative mode) the way to the other dimension opens. */
   static void tickPlayer(Game g) {
      if (g.world == null || g.player == null || g.player.dead || g.screen != Game.S.PLAY) {
         return;
      }

      World w = g.world;
      Player p = g.player;
      int[] cell = touching(w, p.bb);
      if (cell != null) {
         if (!g.portalLock) {
            g.portalTime++;
            if (g.portalTime >= (p.creative ? 1 : WAIT)) {
               g.portalTime = 0;
               int to = w.dim == Dim.NETHER ? Dim.OVERWORLD : Dim.NETHER;
               double k = Dim.scale(w.dim, to);
               g.switchDimension(to, p.x * k, p.z * k, 0);
            }
         }
      } else {
         g.portalLock = false;
         if (g.portalTime > 0) {
            g.portalTime = Math.max(0, g.portalTime - 4);
         }
      }
   }

   // ------------------------------------------------------------------ building a portal for somebody who arrives

   /**
    * Builds a portal whose interior (2 wide, 3 high) starts at (x, y, z), with the frame, a floor to stand on and the room in front of
    * and behind it cleared. Returns the cell the traveller stands in.
    */
   static double[] build(World w, int x, int y, int z, int axis) {
      int dx = axis == 0 ? 1 : 0;
      int dz = axis == 0 ? 0 : 1;
      int sx = axis == 0 ? 0 : 1; // the direction across the film
      int sz = axis == 0 ? 1 : 0;
      for (int i = -1; i <= 2; i++) {
         for (int k = -1; k <= 1; k++) {
            int fx = x + dx * i + sx * k;
            int fz = z + dz * i + sz * k;
            // the floor: obsidian under the film and the frame, whatever is there, and under the room in front of / behind it a solid block
            if (k == 0) {
               w.setBlock(fx, y - 1, fz, Blocks.OBSIDIAN);
            } else if (!Blocks.solid[w.getBlock(fx, y - 1, fz)] || Blocks.isLava(w.getBlock(fx, y - 1, fz))) {
               w.setBlock(fx, y - 1, fz, Blocks.OBSIDIAN);
            }

            for (int j = 0; j < 4; j++) {
               if (k != 0 || i >= 0 && i <= 1) {
                  w.setBlock(fx, y + j, fz, 0);
               }
            }
         }
      }

      for (int j = 0; j < 4; j++) {
         if (j < 3) {
            w.setBlock(x - dx, y + j, z - dz, Blocks.OBSIDIAN);
            w.setBlock(x + dx * 2, y + j, z + dz * 2, Blocks.OBSIDIAN);
         }
      }

      for (int i = 0; i < 2; i++) {
         w.setBlock(x + dx * i, y + 3, z + dz * i, Blocks.OBSIDIAN);
      }

      Frame f = new Frame();
      f.axis = axis;
      f.x0 = x;
      f.y0 = y;
      f.z0 = z;
      f.w = 2;
      f.h = 3;
      fill(w, f);
      return new double[]{x + 0.5 + dx * 0.5 + sx * 0.0, y, z + 0.5 + dz * 0.5};
   }

   /** Is there room for a portal with its interior starting here? Floor solid (not lava) under the 4 x 3 footprint, air in the 4 x 4 x 3 room. */
   static boolean fits(World w, int x, int y, int z, int axis) {
      int dx = axis == 0 ? 1 : 0;
      int dz = axis == 0 ? 0 : 1;
      int sx = axis == 0 ? 0 : 1;
      int sz = axis == 0 ? 1 : 0;
      for (int i = -1; i <= 2; i++) {
         for (int k = -1; k <= 1; k++) {
            int fx = x + dx * i + sx * k;
            int fz = z + dz * i + sz * k;
            if (!w.isLoaded(fx, fz)) {
               return false;
            }

            int floor = w.getBlock(fx, y - 1, fz);
            if (!Blocks.solid[floor] || Blocks.isLava(floor) || Blocks.isLeaves(floor) || Blocks.isAnyLog(floor)) {
               return false;
            }

            for (int j = 0; j < 4; j++) {
               int id = w.getBlock(fx, y + j, fz);
               if (id != 0 && !Blocks.replaceable[id] || Blocks.isFluid(id)) {
                  return false;
               }
            }
         }
      }

      return true;
   }

   /** The film cell nearest to (tx, tz), within {@code radius} blocks, in the part of the world that is loaded; its lowest cell. Null: none. */
   static int[] findPortal(World w, double tx, double tz, int radius) {
      int[] best = null;
      double bestD = Double.MAX_VALUE;
      int x0 = (int)Math.floor(tx);
      int z0 = (int)Math.floor(tz);
      for (int dz = -radius; dz <= radius; dz++) {
         for (int dx = -radius; dx <= radius; dx++) {
            int x = x0 + dx;
            int z = z0 + dz;
            Chunk c = w.chunkAt(x, z);
            if (c == null) {
               continue;
            }

            for (int y = w.dim == Dim.NETHER ? 120 : 250; y > 1; y--) {
               if (Blocks.isPortal(c.get(x & 15, y, z & 15)) && !Blocks.isPortal(c.get(x & 15, y - 1, z & 15))) {
                  double d = dx * dx + dz * dz;
                  if (d < bestD) {
                     bestD = d;
                     best = new int[]{x, y, z};
                  }
               }
            }
         }
      }

      return best;
   }

   /** Somewhere in the loaded area near (tx, tz) where a portal fits. Returns {x, y, z, axis} or null. */
   static int[] findSite(World w, double tx, double tz) {
      int x0 = (int)Math.floor(tx);
      int z0 = (int)Math.floor(tz);
      int lo = w.dim == Dim.NETHER ? 34 : 60;
      int hi = w.dim == Dim.NETHER ? 118 : 200;
      for (int r = 0; r <= 28; r += 2) {
         for (int dz = -r; dz <= r; dz += 2) {
            for (int dx = -r; dx <= r; dx += 2) {
               if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                  continue;
               }

               int x = x0 + dx;
               int z = z0 + dz;
               Chunk c = w.chunkAt(x, z);
               if (c == null) {
                  continue;
               }

               if (w.dim == Dim.OVERWORLD) {
                  int y = surface(w, x, z);
                  if (y >= lo && y < hi && fits(w, x, y + 1, z, 0)) {
                     return new int[]{x, y + 1, z, 0};
                  }
               } else {
                  for (int y = hi; y >= lo; y--) {
                     if (fits(w, x, y, z, 0)) {
                        return new int[]{x, y, z, 0};
                     }
                  }
               }
            }
         }
      }

      return null;
   }

   /** The highest block of the column that is solid ground (not a leaf, log or a plant): the y you stand on, or 0. */
   static int surface(World w, int x, int z) {
      Chunk c = w.chunkAt(x, z);
      if (c == null) {
         return 0;
      }

      for (int y = Math.min(250, c.hm[(z & 15) * 16 + (x & 15)]); y > 1; y--) {
         int id = c.get(x & 15, y, z & 15);
         if (id != 0 && Blocks.solid[id] && !Blocks.isLeaves(id) && !Blocks.isAnyLog(id) && id != Blocks.CACTUS) {
            return Blocks.isFluid(id) ? 0 : y;
         }

         if (Blocks.isFluid(id)) {
            return 0;
         }
      }

      return 0;
   }

   /**
    * The player has just come into a new dimension and the area around (tx, tz) is loaded: use the portal that is there, or make one, and
    * put the player in it. The cell the player stands at is returned as {x, y, z}.
    */
   static double[] arrive(World w, double tx, double tz) {
      int[] p = findPortal(w, tx, tz, 40);
      if (p != null) {
         int dx = w.getBlock(p[0], p[1], p[2]) == Blocks.PORTAL_X ? 1 : 0;
         int dz = 1 - dx;
         // the middle of the film, at the bottom: walk to its start, then count how wide it is
         int sx = p[0];
         int sz = p[2];
         while (Blocks.isPortal(w.getBlock(sx - dx, p[1], sz - dz))) {
            sx -= dx;
            sz -= dz;
         }

         int n = 1;
         while (Blocks.isPortal(w.getBlock(sx + dx * n, p[1], sz + dz * n))) {
            n++;
         }

         return new double[]{sx + 0.5 + dx * (n - 1) * 0.5, p[1], sz + 0.5 + dz * (n - 1) * 0.5};
      }

      int[] s = findSite(w, tx, tz);
      if (s != null) {
         return build(w, s[0], s[1], s[2], s[3]);
      }

      // nowhere that fits: make a place (the Nether: in the middle of the cavern, the Overworld: on top of the ground)
      int x = (int)Math.floor(tx);
      int z = (int)Math.floor(tz);
      int y;
      if (w.dim == Dim.NETHER) {
         y = 64;
         if (w.gen instanceof NetherGen) {
            int[] floor = ((NetherGen)w.gen).findFloor(x, z);
            if (floor != null) {
               y = floor[1];
            }
         }
      } else {
         y = Math.max(w.dim == Dim.OVERWORLD ? Gen.SEA + 1 : 60, surface(w, x, z) + 1);
      }

      return build(w, x, y, z, 0);
   }
}
