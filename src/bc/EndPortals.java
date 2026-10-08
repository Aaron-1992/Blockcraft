package bc;

/**
 * Update 3.12: the way into the End. Twelve End portal frames stand in a ring of 5 x 5 (the corners are empty) round a 3 x 3 pit; put an
 * Eye of Ender into every frame and the pit fills with End portal. An Eye of Ender thrown into the air flies towards the nearest
 * stronghold (at most 12 blocks at a time), hovers and then falls to the ground - or, now and then, breaks.
 */
final class EndPortals {
   private EndPortals() {
   }

   /** Does an Eye of Ender break at the end of its flight (one in five)? A field so that the tests can fix the dice. */
   static java.util.Random rnd = new java.util.Random();

   /** The twelve frames of a ring, relative to the middle of the pit. */
   static final int[][] RING = new int[12][];

   static {
      int n = 0;
      for (int k = -1; k <= 1; k++) {
         RING[n++] = new int[]{-2, k};
         RING[n++] = new int[]{2, k};
         RING[n++] = new int[]{k, -2};
         RING[n++] = new int[]{k, 2};
      }
   }

   /** The cell of an End portal the box touches ({x, y, z}), or null. */
   static int[] touching(World w, double[] bb) {
      for (int y = (int)Math.floor(bb[1]); y <= (int)Math.floor(bb[4]); y++) {
         for (int x = (int)Math.floor(bb[0]); x <= (int)Math.floor(bb[3]); x++) {
            for (int z = (int)Math.floor(bb[2]); z <= (int)Math.floor(bb[5]); z++) {
               if (w.getBlock(x, y, z) == Blocks.END_PORTAL) {
                  double[] b = Blocks.box[Blocks.END_PORTAL];
                  if (bb[0] < x + b[3] && bb[3] > x + b[0] && bb[1] < y + b[4] && bb[4] > y + b[1] && bb[2] < z + b[5] && bb[5] > z + b[2]) {
                     return new int[]{x, y, z};
                  }
               }
            }
         }
      }

      return null;
   }

   /** Is the ring round the pit whose middle is (cx, y, cz) complete, every frame holding its eye? */
   static boolean complete(World w, int cx, int y, int cz) {
      for (int[] r : RING) {
         if (w.getBlock(cx + r[0], y, cz + r[1]) != Blocks.END_FRAME_EYE) {
            return false;
         }
      }

      return true;
   }

   /**
    * A frame at (x, y, z) has just got its eye: if that completes a ring, the pit fills with portal. Returns true if a portal opened.
    */
   static int[] tryOpen(World w, int x, int y, int z) {
      for (int[] r : RING) {
         int cx = x - r[0];
         int cz = z - r[1];
         if (!complete(w, cx, y, cz)) {
            continue;
         }

         for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
               int id = w.getBlock(cx + dx, y, cz + dz);
               if (id != 0 && !Blocks.replaceable[id] && id != Blocks.END_PORTAL) {
                  return null;
               }
            }
         }

         for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
               w.setBlock(cx + dx, y, cz + dz, Blocks.END_PORTAL);
            }
         }

         return new int[]{cx, cz};
      }

      return null;
   }

   /** Right click on an empty frame with an Eye of Ender in the hand. */
   static boolean insertEye(Game g, int x, int y, int z) {
      World w = g.world;
      int id = w.getBlock(x, y, z);
      if (id == Blocks.END_FRAME_EYE) {
         return true; // it has one already
      }

      if (id != Blocks.END_FRAME) {
         return false;
      }

      w.setBlock(x, y, z, Blocks.END_FRAME_EYE);
      g.meshNow(x, y, z);
      if (!g.player.creative) {
         g.removeItems(Items.EYE_OF_ENDER, 1);
      }

      int[] opened = tryOpen(w, x, y, z);
      if (opened != null) {
         for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
               g.meshNow(opened[0] + dx, y, opened[1] + dz);
            }
         }

         g.say("The portal to the End has opened!");
      }

      return true;
   }

   /** Right click into the air with an Eye of Ender: it flies towards the nearest stronghold. */
   static boolean throwEye(Game g, Stack held) {
      World w = g.world;
      Player p = g.player;
      double yaw = Math.toRadians(p.yaw);
      double lx = p.x - Math.sin(yaw) * 0.6;
      double lz = p.z + Math.cos(yaw) * 0.6;
      double ly = p.y + p.eye - 0.2;
      double ax = lx;
      double az = lz;
      if (w.dim == Dim.OVERWORLD) {
         Stronghold s = Stronghold.nearest(w.seed, p.x, p.z);
         if (s != null) {
            double dx = s.cx + 0.5 - lx;
            double dz = s.cz + 0.5 - lz;
            double d = Math.sqrt(dx * dx + dz * dz);
            double k = d > 12.0 ? 12.0 / d : 1.0;
            ax = lx + dx * k;
            az = lz + dz * k;
         }
      }

      World.Item it = w.dropItem(lx, ly, lz, Items.EYE_OF_ENDER, 1, 0);
      if (it == null) {
         return false;
      }

      it.eye = true;
      it.delay = 100000;
      it.vx = it.vy = it.vz = 0.0;
      it.lx = lx;
      it.ly = ly;
      it.lz = lz;
      it.ax = ax;
      it.az = az;
      if (!p.creative) {
         held.n--;
         if (held.n <= 0) {
            g.inv[g.sel] = null;
         }
      }

      return true;
   }

   /** One tick of an eye in flight. */
   static void tickEye(World w, World.Item it) {
      it.age++;
      it.delay = 100000;
      int t = it.eyeT++;
      double p = Math.min(1.0, t / 45.0);
      double e = 1.0 - (1.0 - p) * (1.0 - p);
      double bob = 0.5 * Math.sin(t * 0.35) * (1.0 - p);
      double nx = it.lx + (it.ax - it.lx) * e;
      double nz = it.lz + (it.az - it.lz) * e;
      double ny = it.ly + 6.0 * e + bob;
      it.vx = nx - it.x;
      it.vy = ny - it.y;
      it.vz = nz - it.z;
      it.x = nx;
      it.y = ny;
      it.z = nz;
      it.spin += 4.0F;
      if (w.game != null && w.game.rend != null && t % 2 == 0) {
         w.game.portalParticle(it.x + (Math.random() - 0.5) * 0.3, it.y + 0.1, it.z + (Math.random() - 0.5) * 0.3, 0.55F, 0.2F, 0.95F);
      }

      if (t >= 62) {
         it.eye = false;
         it.eyeT = 0;
         it.delay = 10;
         it.vx = it.vy = it.vz = 0.0;
         if (rnd.nextInt(5) == 0) {
            it.dead = true; // it breaks
            if (w.game != null && w.game.rend != null) {
               for (int i = 0; i < 12; i++) {
                  w.game.portalParticle(it.x + (Math.random() - 0.5) * 0.4, it.y + (Math.random() - 0.5) * 0.4, it.z + (Math.random() - 0.5) * 0.4, 0.4F, 0.9F, 0.6F);
               }
            }
         }
      }
   }
}
