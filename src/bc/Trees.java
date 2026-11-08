package bc;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

/**
 * Update 3.10: trees fall over when you chop through their trunk (To do item 6).
 *
 * <p>When the player breaks a log, {@link #fell} looks at what hung on that log: the logs above it that are connected to each other
 * (trunk and branches) and are not held up by anything else, and the leaves that belong to them (within 4 leaf steps of one of those
 * logs, and not closer to the logs of another tree). It only counts as a tree when at least {@link #MIN_LEAVES} leaves hang on it, so a
 * log pillar or a cabin built by the player is left alone.
 *
 * <p>The whole thing then topples as one rigid piece, hinged on the cell that was cut, away from the player when there is room (else the
 * other three ways, in the order of how well they point away). Every block turns by exactly 90 degrees, so every block ends up in a
 * grid cell: an upright log becomes a log lying along the fall direction (ids 220..225), a leaf stays a leaf. If the cut was higher
 * up, the lying trunk then drops straight down until it rests on something. Leaves that would land inside the ground are pushed up
 * (up to two blocks) or crumble (apples and sticks drop like when you break leaves).
 * Mobs and the player under the landing logs take damage and are lifted on top of them.
 *
 * <p>The blocks are taken out of the world when the tree starts to fall and put back where they land ({@link #land}); in between they
 * only exist in {@link Fall}, which the renderer draws (Renderer.drawFallingTrees). Saving the world lands every tree first.
 * The fallen trunk stays in the world as side logs which are harvested (and burn) like any log.
 */
public final class Trees {
   /** More logs than this: not a normal tree, it stays. */
   /** V6.25: branch-skeleton trees have real branch networks, so the old 160-log cap made
    *  them refuse to topple at all. */
   static final int MAX_LOGS = 480;
   static final int MAX_LEAVES = 1400;
   /** How far a leaf may be from a log (in steps through leaves, diagonals included) and still hang on it. */
   static final int LEAF_REACH = 6;
   static final int MIN_LEAVES = 4;
   static final int MAX_DROP = 24;
   static final int MAX_SPAN = 18;
   /** Damage of a log that lands on a mob or the player (half hearts). */
   static final int HIT = 7;
   static final int[][] DIRS = new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

   /** Screenshot hook: -Dbc.fellangle=45 freezes every falling tree at that angle (degrees) so a picture can catch it half way down. */
   static final double FREEZE = Double.parseDouble(System.getProperty("bc.fellangle", "-1"));

   final World w;
   final ArrayList<Fall> falls = new ArrayList<>();
   /** /treefall on|off (saved with the world) */
   public boolean enabled = true;
   final Random rnd = new Random(0x7EEFA11L);
   /** Trees that have started to fall in this session (statistics for tests and the debug screen). */
   public int felled;

   Trees(World w) {
      this.w = w;
   }

   /** What is going to fall: logs first ({@link #nLogs} of them), then leaves. Cells are absolute. */
   static final class Plan {
      int px;
      int py;
      int pz;
      int n;
      /** the first nTrunk logs are the trunk (same column as the pivot), then come the branches, up to nLogs; the rest are leaves */
      int nTrunk;
      int nLogs;
      int[] x;
      int[] y;
      int[] z;
      int[] id;
      HashSet<Long> cells = new HashSet<>();
   }

   /** A tree in the air. */
   public static final class Fall {
      Plan plan;
      /** the direction it topples to, and how far the trunk drops after it lies down */
      int dx;
      int dz;
      int drop;
      /** ticks of the toppling (the angle follows an ease-in curve), then the drop */
      int rotTicks;
      int age;
      double shift;
      double pshift;
      double vy;
      boolean done;

      /** 0..90 degrees at the start of the tick, interpolated by the renderer. */
      public float angle(float partial) {
         if (FREEZE >= 0.0) {
            return (float)FREEZE;
         }

         float t = Math.min(1.0F, (this.age + partial) / this.rotTicks);
         return 90.0F * t * t;
      }

      public double drop(float partial) {
         return this.pshift + (this.shift - this.pshift) * partial;
      }

      public int count() {
         return this.plan.n;
      }

      public int blockId(int i) {
         return this.plan.id[i];
      }

      public int dx() {
         return this.dx;
      }

      public int dz() {
         return this.dz;
      }

      public int pivotX() {
         return this.plan.px;
      }

      public int pivotY() {
         return this.plan.py;
      }

      public int pivotZ() {
         return this.plan.pz;
      }

      public int offX(int i) {
         return this.plan.x[i] - this.plan.px;
      }

      public int offY(int i) {
         return this.plan.y[i] - this.plan.py;
      }

      public int offZ(int i) {
         return this.plan.z[i] - this.plan.pz;
      }
   }

   public ArrayList<Fall> falling() {
      return this.falls;
   }

   // ------------------------------------------------------------------ what hangs on a cut log

   /** Can a falling log pass through / land in this cell? Air, plants and leaves yes; solid blocks and fluids no. */
   boolean passable(int id) {
      return id == 0 || Blocks.isLeaves(id) || Blocks.replaceable[id] && !Blocks.isFluid(id);
   }

   boolean free(int x, int y, int z, HashSet<Long> own) {
      if (y < 1 || y > 254 || !this.w.isLoaded(x, z)) {
         return false;
      }

      if (own != null && own.contains(World.pack(x, y, z))) {
         return true;
      }

      return this.passable(this.w.getBlock(x, y, z));
   }

   /**
    * The logs and leaves that were hanging on the log cell (px, py, pz) which has just been cut out, or null when this is not a tree
    * that can fall (nothing above, held up by something else, too big, no leaves).
    */
   Plan collect(int px, int py, int pz) {
      World w = this.w;
      long pivot = World.pack(px, py, pz);
      HashSet<Long> logSet = new HashSet<>();
      ArrayList<int[]> logs = new ArrayList<>();
      ArrayDeque<int[]> q = new ArrayDeque<>();
      int[][] seeds = new int[][]{{0, 1, 0}, {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}};
      for (int[] d : seeds) {
         int x = px + d[0];
         int y = py + d[1];
         int z = pz + d[2];
         if (Blocks.isLog(w.getBlock(x, y, z)) && logSet.add(World.pack(x, y, z))) {
            int[] c = new int[]{x, y, z};
            logs.add(c);
            q.add(c);
         }
      }

      if (logs.isEmpty()) {
         return null;
      }

      while (!q.isEmpty()) {
         int[] c = q.poll();
         for (int f = 0; f < 6; f++) {
            int x = c[0] + World.DX[f];
            int y = c[1] + World.DY[f];
            int z = c[2] + World.DZ[f];
            if (y < py || !Blocks.isLog(w.getBlock(x, y, z)) || x == px && y == py && z == pz) {
               continue;
            }

            if (Math.abs(x - px) > MAX_SPAN || Math.abs(z - pz) > MAX_SPAN || y > py + 64 || logs.size() >= MAX_LOGS) {
               return null;
            }

            if (logSet.add(World.pack(x, y, z))) {
               int[] n = new int[]{x, y, z};
               logs.add(n);
               q.add(n);
            }
         }
      }

      // held up by something that is not part of it (the ground, another log, a block): it does not fall
      for (int[] c : logs) {
         long below = World.pack(c[0], c[1] - 1, c[2]);
         if (below == pivot || logSet.contains(below)) {
            continue;
         }

         int b = w.getBlock(c[0], c[1] - 1, c[2]);
         if (b != 0 && Blocks.solid[b] && !Blocks.isLeaves(b)) {
            // A 2x2 trunk base legitimately rests on the ground right next to the cut.
            // Those logs are part of the stump, not a cabin propping the tree up.
            boolean thickBase = false;
            if (c[1] <= py + 1 && Math.abs(c[0] - px) <= 1 && Math.abs(c[2] - pz) <= 1) {
               int around = 0;
               for (int ax = px - 1; ax <= px + 1; ax++) {
                  for (int az = pz - 1; az <= pz + 1; az++) {
                     if (logSet.contains(World.pack(ax, c[1], az))) around++;
                  }
               }
               // Only a real 2x2 trunk base (three or four logs on one layer) is
               // allowed to rest on the ground. A single log on a stone still props
               // a player-built structure up.
               // On the cut layer the pivot itself is already gone, so two neighbouring
               // base logs are enough; one layer up a real 2x2 needs three or four.
               thickBase = around >= (c[1] == py ? 2 : 3);
            }
            if (!thickBase) {
               return null;
            }
         }
      }

      // the leaves that hang on these logs: breadth first from the logs through leaf cells, at most LEAF_REACH steps
      int x0 = Integer.MAX_VALUE;
      int y0 = Integer.MAX_VALUE;
      int z0 = Integer.MAX_VALUE;
      int x1 = Integer.MIN_VALUE;
      int y1 = Integer.MIN_VALUE;
      int z1 = Integer.MIN_VALUE;
      for (int[] c : logs) {
         x0 = Math.min(x0, c[0]);
         y0 = Math.min(y0, c[1]);
         z0 = Math.min(z0, c[2]);
         x1 = Math.max(x1, c[0]);
         y1 = Math.max(y1, c[1]);
         z1 = Math.max(z1, c[2]);
      }

      HashMap<Long, Integer> dOther = new HashMap<>();
      ArrayDeque<int[]> oq = new ArrayDeque<>();
      int reach = 2 * LEAF_REACH;
      if ((long)(x1 - x0 + 2 * reach + 1) * (y1 - y0 + 2 * reach + 1) * (z1 - z0 + 2 * reach + 1) < 400000L) {
         for (int x = x0 - reach; x <= x1 + reach; x++) {
            for (int z = z0 - reach; z <= z1 + reach; z++) {
               for (int y = Math.max(0, y0 - reach); y <= Math.min(255, y1 + reach); y++) {
                  long k = World.pack(x, y, z);
                  if (k != pivot && !logSet.contains(k) && Blocks.isLog(w.getBlock(x, y, z))) {
                     dOther.put(k, 0);
                     oq.add(new int[]{x, y, z, 0});
                  }
               }
            }
         }
      }

      this.spreadThroughLeaves(oq, dOther, null, null);
      HashMap<Long, Integer> dOwn = new HashMap<>();
      ArrayDeque<int[]> lq = new ArrayDeque<>();
      for (int[] c : logs) {
         dOwn.put(World.pack(c[0], c[1], c[2]), 0);
         lq.add(new int[]{c[0], c[1], c[2], 0});
      }

      ArrayList<int[]> leaves = new ArrayList<>();
      this.spreadThroughLeaves(lq, dOwn, dOther, leaves);
      if (leaves.size() < MIN_LEAVES) {
         return null;
      }

      ArrayList<int[]> ordered = new ArrayList<>();
      for (int[] c : logs) {
         if (c[0] == px && c[2] == pz) {
            ordered.add(c);
         }
      }

      if (ordered.isEmpty()) {
         return null; // nothing of a trunk above the cut (a branch was cut): it stays where it is
      }

      Plan p = new Plan();
      p.nTrunk = ordered.size();
      for (int[] c : logs) {
         if (c[0] != px || c[2] != pz) {
            ordered.add(c);
         }
      }

      logs = ordered;
      p.px = px;
      p.py = py;
      p.pz = pz;
      p.nLogs = logs.size();
      p.n = logs.size() + leaves.size();
      p.x = new int[p.n];
      p.y = new int[p.n];
      p.z = new int[p.n];
      p.id = new int[p.n];
      for (int i = 0; i < p.n; i++) {
         int[] c = i < p.nLogs ? logs.get(i) : leaves.get(i - p.nLogs);
         p.x[i] = c[0];
         p.y[i] = c[1];
         p.z[i] = c[2];
         p.id[i] = w.getBlock(c[0], c[1], c[2]);
         p.cells.add(World.pack(c[0], c[1], c[2]));
      }

      return p;
   }

   /**
    * Breadth first search through leaf cells. {@code q} holds {x, y, z, distance} sources; {@code dist} gets the distance of every
    * leaf reached. With {@code rival} (distances from the logs of other trees) a leaf that another tree is closer to is not taken (and
    * not searched through). Accepted leaves are appended to {@code out}.
    */
   void spreadThroughLeaves(ArrayDeque<int[]> q, HashMap<Long, Integer> dist, HashMap<Long, Integer> rival, ArrayList<int[]> out) {
      while (!q.isEmpty()) {
         int[] c = q.poll();
         if (c[3] >= LEAF_REACH) {
            continue;
         }

         // 26 neighbours (diagonals count): the generated crowns have leaves that only touch the rest at an edge or a corner
         for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
               for (int dz = -1; dz <= 1; dz++) {
                  int x = c[0] + dx;
                  int y = c[1] + dy;
                  int z = c[2] + dz;
                  long k = World.pack(x, y, z);
                  if (y < 0 || y >= Chunk.H || dist.containsKey(k) || !Blocks.isLeaves(this.w.getBlock(x, y, z))) {
                     continue;
                  }

                  int d = c[3] + 1;
                  if (rival != null) {
                     Integer o = rival.get(k);
                     if (o != null && o < d) {
                        continue;
                     }
                  }

                  if (out != null && out.size() >= MAX_LEAVES) {
                     return;
                  }

                  dist.put(k, d);
                  if (out != null) {
                     out.add(new int[]{x, y, z});
                  }

                  q.add(new int[]{x, y, z, d});
               }
            }
         }
      }
   }

   // ------------------------------------------------------------------ where it lands

   /** The offset (from the pivot) of a block that was at (ox, oy, oz) after the tree has toppled 90 degrees towards (dx, dz). */
   static void rotated(int ox, int oy, int oz, int dx, int dz, int[] out) {
      int along = ox * dx + oz * dz; // in the fall direction
      int side = -ox * dz + oz * dx; // sideways, stays
      out[0] = oy * dx - side * dz;
      out[1] = -along;
      out[2] = oy * dz + side * dx;
   }

   /** How far the toppled trunk drops until it rests on something, or -1 when this way is blocked (terrain, other trees' trunks, water). */
   int checkDirection(Plan p, int dx, int dz) {
      return this.checkDirection(p, dx, dz, null);
   }

   /** As {@link #checkDirection(Plan, int, int)}, but the trunk must not come to lie where {@code avoid} (a bounding box) is: never onto the player who is chopping. */
   int checkDirection(Plan p, int dx, int dz, double[] avoid) {
      int nl = p.nTrunk; // branches and leaves are not rigid: what does not fit crumbles or is pushed up when it lands
      int[] fx = new int[nl];
      int[] fy = new int[nl];
      int[] fz = new int[nl];
      int[] r = new int[3];
      for (int i = 0; i < nl; i++) {
         rotated(p.x[i] - p.px, p.y[i] - p.py, p.z[i] - p.pz, dx, dz, r);
         fx[i] = p.px + r[0];
         fy[i] = p.py + r[1];
         fz[i] = p.pz + r[2];
      }

      // the way down: the cell each log centre passes at 15, 30 ... 75 degrees
      for (int k = 1; k <= 5; k++) {
         double th = Math.toRadians(15.0 * k);
         double c = Math.cos(th);
         double s = Math.sin(th);
         for (int i = 0; i < nl; i++) {
            int ox = p.x[i] - p.px;
            int oy = p.y[i] - p.py;
            int oz = p.z[i] - p.pz;
            double od = ox * dx + oz * dz;
            double op = -ox * dz + oz * dx;
            double along = od * c + oy * s;
            double up = oy * c - od * s;
            int cx = (int)Math.floor(p.px + 0.5 + along * dx - op * dz);
            int cy = (int)Math.floor(p.py + 0.5 + up);
            int cz = (int)Math.floor(p.pz + 0.5 + along * dz + op * dx);
            if (!this.free(cx, cy, cz, p.cells)) {
               return -1;
            }
         }
      }

      if (!this.logsFree(p, fx, fy, fz, 0, avoid)) {
         return -1;
      }

      int drop = 0;
      while (drop < MAX_DROP && this.logsFree(p, fx, fy, fz, drop + 1, avoid)) {
         drop++;
      }

      return drop >= MAX_DROP ? -1 : drop;
   }

   boolean logsFree(Plan p, int[] fx, int[] fy, int[] fz, int shift, double[] avoid) {
      for (int i = 0; i < p.nTrunk; i++) {
         if (!this.free(fx[i], fy[i] - shift, fz[i], p.cells)) {
            return false;
         }

         // (a margin of a third of a block around the player: the log must not land against him either)
         if (avoid != null && avoid[3] + 0.35 > fx[i] && avoid[0] - 0.35 < fx[i] + 1 && avoid[4] > fy[i] - shift && avoid[1] < fy[i] - shift + 1 && avoid[5] + 0.35 > fz[i] && avoid[2] - 0.35 < fz[i] + 1) {
            return false;
         }
      }

      return true;
   }

   /** The directions to try, best first: the one pointing away from (fromX, fromZ), then the others in the order of how well they point away. */
   static int[][] directionOrder(double awayX, double awayZ, Random rnd) {
      int[][] order = new int[][]{DIRS[0], DIRS[1], DIRS[2], DIRS[3]};
      final double[] score = new double[4];
      for (int i = 0; i < 4; i++) {
         score[i] = order[i][0] * awayX + order[i][1] * awayZ + (rnd == null ? 0.0 : rnd.nextDouble() * 1.0E-3);
      }

      // insertion sort, highest score first
      for (int i = 1; i < 4; i++) {
         for (int j = i; j > 0 && score[j] > score[j - 1]; j--) {
            double ts = score[j];
            score[j] = score[j - 1];
            score[j - 1] = ts;
            int[] td = order[j];
            order[j] = order[j - 1];
            order[j - 1] = td;
         }
      }

      return order;
   }

   // ------------------------------------------------------------------ falling

   /**
    * The log cell (px, py, pz) was just cut out by the player standing at (fromX, fromZ): makes what hung on it fall over.
    * Returns false (and changes nothing) when it is not a tree or there is no room to fall.
    */
   public boolean fell(int px, int py, int pz, double fromX, double fromZ) {
      if (!this.enabled) {
         return false;
      }

      Plan p = this.collect(px, py, pz);
      if (p == null) {
         return false;
      }

      double ax = px + 0.5 - fromX;
      double az = pz + 0.5 - fromZ;
      Player pl = this.w.game == null ? null : this.w.game.player;
      return this.start(p, ax, az, null, pl == null || pl.dead || pl.spectator ? null : pl.bb);
   }

   /** Starts the fall of a collected tree: first free direction in {@link #directionOrder}. */
   boolean start(Plan p, double awayX, double awayZ, Random jitter) {
      return this.start(p, awayX, awayZ, jitter, null);
   }

   boolean start(Plan p, double awayX, double awayZ, Random jitter, double[] avoid) {
      for (int[] d : directionOrder(awayX, awayZ, jitter)) {
         int drop = this.checkDirection(p, d[0], d[1], avoid);
         if (drop >= 0) {
            this.launch(p, d[0], d[1], drop);
            return true;
         }
      }

      return false;
   }

   void launch(Plan p, int dx, int dz, int drop) {
      Fall f = new Fall();
      f.plan = p;
      f.dx = dx;
      f.dz = dz;
      f.drop = drop;
      int height = 1;
      for (int i = 0; i < p.nTrunk; i++) {
         height = Math.max(height, p.y[i] - p.py);
      }

      f.rotTicks = Math.max(20, Math.min(44, 14 + 2 * height));
      for (int i = p.n - 1; i >= 0; i--) {
         this.w.setBlock(p.x[i], p.y[i], p.z[i], 0);
      }

      this.falls.add(f);
      this.felled++;
      // V6.28: a creak and a rush of leaves when the trunk lets go.
      SoundEngine.play3D(this.w, "tree_fall", p.px + 0.5, p.py + 1.0, p.pz + 0.5,
         0.88F, 0.90F + this.rnd.nextFloat() * 0.18F);
   }

   /** Called every world tick. */
   public void tick() {
      if (this.falls.isEmpty() || FREEZE >= 0.0) {
         return;
      }

      for (Fall f : new ArrayList<>(this.falls)) {
         f.pshift = f.shift;
         if (f.age < f.rotTicks) {
            f.age++;
            if (f.age >= f.rotTicks && f.drop == 0) {
               this.land(f);
            }
         } else {
            f.vy = Math.min(1.2, f.vy + 0.08);
            f.shift += f.vy;
            if (f.shift >= f.drop) {
               f.shift = f.drop;
               this.land(f);
            }
         }
      }

      this.falls.removeIf(f -> f.done);
   }

   /** Puts every tree in the air where it would land (before the world is saved, or when the game ends). */
   public void finishAll() {
      for (Fall f : new ArrayList<>(this.falls)) {
         this.land(f);
      }

      this.falls.removeIf(f -> f.done);
   }

   void land(Fall f) {
      if (f.done) {
         return;
      }

      f.done = true;
      Plan p = f.plan;
      World w = this.w;
      Game g = w.game;
      // V6.28: the heavy thud as the crown lands.
      SoundEngine.play3D(w, "tree_crash", p.px + 0.5, p.py + 0.5, p.pz + 0.5,
         1.0F, 0.88F + this.rnd.nextFloat() * 0.20F);
      int axis = f.dx != 0 ? 0 : 1;
      HashSet<Long> used = new HashSet<>();
      int[] r = new int[3];
      int particles = 0;
      // 1. the trunk: it was checked, so it lies where the rotation puts it (unless somebody built there meanwhile)
      for (int i = 0; i < p.nTrunk; i++) {
         rotated(p.x[i] - p.px, p.y[i] - p.py, p.z[i] - p.pz, f.dx, f.dz, r);
         int x = p.px + r[0];
         int y = p.py + r[1] - f.drop;
         int z = p.pz + r[2];
         this.squash(x, y, z);
         if (this.free(x, y, z, null)) {
            w.setBlock(x, y, z, Blocks.sideLog(p.id[i], axis));
            used.add(World.pack(x, y, z));
            if (g != null && particles < 120) {
               g.spawnBreakParticles(x, y, z, p.id[i], 3);
               particles += 3;
            }
         } else {
            w.dropItem(x + 0.5, y + 0.5, z + 0.5, p.id[i], 1);
         }
      }

      // 2. branches, then 3. leaves: put where the rotation says, pushed up (at most 2) when that is the ground or taken;
      // a branch that does not fit snaps off as a log item, a leaf crumbles like when it is broken
      for (int i = p.nTrunk; i < p.n; i++) {
         boolean log = i < p.nLogs;
         rotated(p.x[i] - p.px, p.y[i] - p.py, p.z[i] - p.pz, f.dx, f.dz, r);
         int x = p.px + r[0];
         int y0 = p.py + r[1] - f.drop;
         int z = p.pz + r[2];
         boolean placed = false;
         for (int up = 0; up <= 2 && !placed; up++) {
            long key = World.pack(x, y0 + up, z);
            if (!used.contains(key) && this.free(x, y0 + up, z, null) && !this.entityIn(x, y0 + up, z)) {
               w.setBlock(x, y0 + up, z, log ? Blocks.sideLog(p.id[i], axis) : p.id[i]);
               used.add(key);
               placed = true;
            }
         }

         if (!placed) {
            if (log) {
               w.dropItem(x + 0.5, Math.max(2, y0) + 0.5, z + 0.5, p.id[i], 1);
            } else {
               this.leafDrop(x, y0, z, p.id[i]);
            }

            if (g != null && particles < 160) {
               g.spawnBreakParticles(x, Math.max(1, y0), z, p.id[i], 1);
               particles++;
            }
         }
      }

      if (g != null && g.player != null) {
         double dxp = g.player.x - (p.px + 0.5 + f.dx * 3.0);
         double dzp = g.player.z - (p.pz + 0.5 + f.dz * 3.0);
         double dist = Math.sqrt(dxp * dxp + dzp * dzp);
         g.addShake((float)Math.max(0.0, 0.5 * (1.0 - dist / 28.0)));
      }
   }

   /** What a leaf that is destroyed drops: the same chances as breaking it by hand (an apple from oak, a stick). */
   void leafDrop(int x, int y, int z, int id) {
      if (this.rnd.nextInt(20) == 0 && id == Blocks.LEAVES_OAK) {
         this.w.dropItem(x + 0.5, y + 0.3, z + 0.5, 270, 1);
      } else if (this.rnd.nextInt(50) == 0) {
         this.w.dropItem(x + 0.5, y + 0.3, z + 0.5, 256, 1);
      }
   }

   /** Anything standing in the cell a log is about to land in is hurt and put on top of it. */
   void squash(int cx, int cy, int cz) {
      Game g = this.w.game;
      if (g == null) {
         return;
      }

      Player pl = g.player;
      if (pl != null && !pl.dead && !pl.spectator && overlaps(pl.bb, cx, cy, cz)) {
         g.damagePlayer(HIT, cx + 0.5, cz + 0.5);
         pl.setPos(pl.x, cy + 1.001, pl.z);
      }

      if (g.mobs != null) {
         for (Mobs.Mob m : g.mobs.list) {
            if (!m.dead && overlaps(m.bb, cx, cy, cz)) {
               g.mobs.hurtMob(m, HIT, cx + 0.5, cz + 0.5, 0.3, false);
               m.y = cy + 1.001;
            }
         }
      }
   }

   /** Is a mob or the player in this cell? (Leaves never land on anybody.) */
   boolean entityIn(int cx, int cy, int cz) {
      Game g = this.w.game;
      if (g == null) {
         return false;
      }

      if (g.player != null && !g.player.dead && !g.player.spectator && overlaps(g.player.bb, cx, cy, cz)) {
         return true;
      }

      if (g.mobs != null) {
         for (Mobs.Mob m : g.mobs.list) {
            if (!m.dead && overlaps(m.bb, cx, cy, cz)) {
               return true;
            }
         }
      }

      return false;
   }

   static boolean overlaps(double[] bb, int cx, int cy, int cz) {
      return bb[3] > cx && bb[0] < cx + 1 && bb[4] > cy && bb[1] < cy + 1 && bb[5] > cz && bb[2] < cz + 1;
   }
}
