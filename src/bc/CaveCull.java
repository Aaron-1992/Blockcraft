package bc;

import java.util.Arrays;

/**
 * Update 3.18: which sections can the camera see at all?
 *
 * Standing on a hillside the game drew every section that was inside the camera's pyramid of view - including the caves, tunnels, ore pockets and
 * the stone room under the hill, thousands of faces that are hidden behind the ground. The well-known remedy (it is what Minecraft does since 1.8):
 * <ol>
 * <li>As soon as chunk block data is available, find out which of each section's six faces are connected through air (or glass, water, leaves,
 * anything that is not opaque): a compact flood fill sets the pair (face a, face b) for all faces the fill reached ({@link #compute}). 36 bits in a long.</li>
 * <li>Every frame walk outward from the camera's section, one section at a time, through the faces that are connected: a ray that came in through
 * face a can only leave through a face b with (a, b) set, and it never turns back on its way ({@link Walk}). What the walk does not reach cannot
 * be seen.</li>
 * </ol>
 * Sections whose connectivity has not been analysed yet count as open, chunks that are not loaded count as open (the walk goes through them to what lies behind),
 * and the 27 sections round the camera are always taken (a camera inside a block, a spectator in the rock) - they are drawn, but the walk does not go on from the 26 that
 * are only neighbours.
 */
final class CaveCull {
   private CaveCull() {
   }

   /** Every face connected with every other one (a section of air, or one that has not been meshed yet). */
   static final long ALL = -1L;
   /** The faces in the order of {@link Mesher#FN}: 0 up, 1 down, 2 north (-z), 3 south (+z), 4 east (+x), 5 west (-x). The opposite of f is f ^ 1. */
   static final int[] DX = {0, 0, 0, 0, 1, -1};
   static final int[] DY = {1, -1, 0, 0, 0, 0};
   static final int[] DZ = {0, 0, -1, 1, 0, 0};

   static long bit(int a, int b) {
      return 1L << (a * 6 + b);
   }

   static boolean connected(long conn, int a, int b) {
      return (conn & bit(a, b)) != 0L;
   }

   private static final class Scratch {
      final long[] seen = new long[64];
      final int[] stack = new int[4096];
   }

   private static final ThreadLocal<Scratch> TL = ThreadLocal.withInitial(Scratch::new);

   /**
    * The connectivity of a section from its blocks: {@code b} is the padded 18 x 18 x 18 array of the mesher (index (y * 18 + z) * 18 + x, the section
    * itself is 1..16 on every axis).
    */
   static long compute(short[] b) {
      boolean[] opaque = Blocks.opaque;
      int open = 0;
      for (int y = 1; y <= 16; y++) {
         for (int z = 1; z <= 16; z++) {
            int row = (y * 18 + z) * 18;
            for (int x = 1; x <= 16; x++) {
               if (!opaque[b[row + x] & BlockState.ID_MASK]) {
                  open++;
               }
            }
         }
      }

      if (open == 0) {
         return 0L; // solid: nothing gets through
      }

      if (open == 4096) {
         return ALL;
      }

      Scratch s = TL.get();
      long[] seen = s.seen;
      int[] stack = s.stack;
      Arrays.fill(seen, 0L);
      long conn = 0L;
      for (int y = 0; y < 16; y++) {
         for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
               if (x != 0 && x != 15 && y != 0 && y != 15 && z != 0 && z != 15) {
                  continue; // only the blocks on the surface of the section start a fill
               }

               int v = y << 8 | z << 4 | x;
               if ((seen[v >> 6] >>> (v & 63) & 1L) != 0L || opaque[b[((y + 1) * 18 + z + 1) * 18 + x + 1] & BlockState.ID_MASK]) {
                  continue;
               }

               int faces = 0;
               int sp = 0;
               stack[sp++] = v;
               seen[v >> 6] |= 1L << (v & 63);
               while (sp > 0) {
                  int u = stack[--sp];
                  int ux = u & 15;
                  int uz = u >> 4 & 15;
                  int uy = u >> 8;
                  if (uy == 15) {
                     faces |= 1;
                  }

                  if (uy == 0) {
                     faces |= 2;
                  }

                  if (uz == 0) {
                     faces |= 4;
                  }

                  if (uz == 15) {
                     faces |= 8;
                  }

                  if (ux == 15) {
                     faces |= 16;
                  }

                  if (ux == 0) {
                     faces |= 32;
                  }

                  for (int f = 0; f < 6; f++) {
                     int nx = ux + DX[f];
                     int ny = uy + DY[f];
                     int nz = uz + DZ[f];
                     if ((nx | ny | nz) < 0 || nx > 15 || ny > 15 || nz > 15) {
                        continue;
                     }

                     int n = ny << 8 | nz << 4 | nx;
                     if ((seen[n >> 6] >>> (n & 63) & 1L) != 0L || opaque[b[((ny + 1) * 18 + nz + 1) * 18 + nx + 1] & BlockState.ID_MASK]) {
                        continue;
                     }

                     seen[n >> 6] |= 1L << (n & 63);
                     stack[sp++] = n;
                  }
               }

               for (int a = 0; a < 6; a++) {
                  if ((faces >> a & 1) != 0) {
                     for (int c = 0; c < 6; c++) {
                        if (c != a && (faces >> c & 1) != 0) {
                           conn |= bit(a, c);
                        }
                     }
                  }
               }
            }
         }
      }

      return conn;
   }

   /** Computes section connectivity directly from chunk storage, without building vertex buffers. */
   static long compute(Chunk chunk, int sy) {
      short[] blocks = chunk.blocks;
      int base = sy << 12;
      boolean[] opaque = Blocks.opaque;
      int open = 0;
      for (int i = 0; i < 4096; i++) {
         if (!opaque[blocks[base + i] & BlockState.ID_MASK]) {
            open++;
         }
      }
      if (open == 0) {
         return 0L;
      }
      if (open == 4096) {
         return ALL;
      }

      Scratch s = TL.get();
      long[] seen = s.seen;
      int[] stack = s.stack;
      Arrays.fill(seen, 0L);
      long conn = 0L;
      for (int y = 0; y < 16; y++) {
         for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
               if (x != 0 && x != 15 && y != 0 && y != 15 && z != 0 && z != 15) {
                  continue;
               }
               int v = y << 8 | z << 4 | x;
               if ((seen[v >> 6] >>> (v & 63) & 1L) != 0L || opaque[blocks[base + v] & BlockState.ID_MASK]) {
                  continue;
               }

               int faces = 0;
               int sp = 0;
               stack[sp++] = v;
               seen[v >> 6] |= 1L << (v & 63);
               while (sp > 0) {
                  int u = stack[--sp];
                  int ux = u & 15;
                  int uz = u >> 4 & 15;
                  int uy = u >> 8;
                  if (uy == 15) faces |= 1;
                  if (uy == 0) faces |= 2;
                  if (uz == 0) faces |= 4;
                  if (uz == 15) faces |= 8;
                  if (ux == 15) faces |= 16;
                  if (ux == 0) faces |= 32;

                  for (int f = 0; f < 6; f++) {
                     int nx = ux + DX[f];
                     int ny = uy + DY[f];
                     int nz = uz + DZ[f];
                     if ((nx | ny | nz) < 0 || nx > 15 || ny > 15 || nz > 15) {
                        continue;
                     }
                     int n = ny << 8 | nz << 4 | nx;
                     if ((seen[n >> 6] >>> (n & 63) & 1L) != 0L || opaque[blocks[base + n] & BlockState.ID_MASK]) {
                        continue;
                     }
                     seen[n >> 6] |= 1L << (n & 63);
                     stack[sp++] = n;
                  }
               }

               for (int a = 0; a < 6; a++) {
                  if ((faces >> a & 1) != 0) {
                     for (int c = 0; c < 6; c++) {
                        if (c != a && (faces >> c & 1) != 0) {
                           conn |= bit(a, c);
                        }
                     }
                  }
               }
            }
         }
      }
      return conn;
   }

   /** Computes accurate connectivity for every section once, on the chunk-generation worker, before the chunk becomes visible. */
   static void computeChunk(Chunk chunk) {
      for (int sy = 0; sy < Chunk.SECTIONS; sy++) {
         Chunk.Section section = chunk.sections[sy];
         section.conn = section.nonAirCount == 0 ? ALL : compute(chunk, sy);
         section.connKnown = true;
      }
   }

   /** Is a section at this offset from the camera (centre, in blocks) inside the pyramid of view? */
   interface Visible {
      boolean test(float rx, float ry, float rz);
   }

   /** The walk of every frame. The arrays are kept between frames; they are made again when the view distance changes. */
   static final class Walk {
      private int radius = -1;
      private int side;
      private Chunk[] grid;
      private byte[] seen;
      private int[] queue;
      /** The sections the walk reached, in the order it found them: index (cell * 16 + section). */
      int[] order;
      int count;
      int cells;
      int centerCx;
      int centerCz;
      boolean hasRun;

      /** True when this section was reached by the most recent culling walk. */
      boolean reached(int cx, int cz, int sy) {
         if (!this.hasRun || sy < 0 || sy > 15) {
            return false;
         }
         int dx = cx - this.centerCx;
         int dz = cz - this.centerCz;
         int r = this.radius;
         if (dx < -r || dx > r || dz < -r || dz > r || dx * dx + dz * dz > r * r) {
            return false;
         }
         int cell = (dz + r) * this.side + dx + r;
         return this.seen[cell * 16 + sy] != 0;
      }

      private void size(int r) {
         if (r == this.radius) {
            return;
         }

         this.radius = r;
         this.side = 2 * r + 1;
         this.cells = this.side * this.side * 16;
         this.grid = new Chunk[this.side * this.side];
         this.seen = new byte[this.cells];
         this.queue = new int[this.cells * 6 + 64];
         this.order = new int[this.cells];
      }

      /** The chunk of the section {@code idx} of the last walk (null: not loaded). */
      Chunk chunk(int idx) {
         return this.grid[idx >> 4];
      }

      /** Chunk x of the section {@code idx} of the last walk, relative to the centre. */
      int dx(int idx) {
         return idx / 16 % this.side - this.radius;
      }

      int dz(int idx) {
         return idx / 16 / this.side - this.radius;
      }

      int sy(int idx) {
         return idx & 15;
      }

      /**
       * Walks from the camera's section ({@code cx, sy, cz}) through the world {@code w} out to {@code view} chunks. {@code vis} (may be null) decides whether a
       * section is in the pyramid of view; {@code camX..camZ} is the position of the camera. Afterwards {@link #order} holds {@link #count} sections.
       */
      void run(World w, int cx, int sy, int cz, int view, double camX, double camY, double camZ, Visible vis) {
         int r = view + 1;
         this.size(r);
         Arrays.fill(this.seen, (byte)0);
         this.count = 0;
         int side = this.side;
         for (int dz = -r; dz <= r; dz++) {
            for (int dx = -r; dx <= r; dx++) {
               this.grid[(dz + r) * side + dx + r] = dx * dx + dz * dz <= r * r ? w.chunk(cx + dx, cz + dz) : null;
            }
         }

         int head = 0;
         int tail = 0;
         int[] q = this.queue;
         // the walk starts in the camera's section, which is open to every side
         int start = (r * side + r) * 16 + sy;
         this.seen[start] = (byte)0x40;
         this.order[this.count++] = start;
         q[tail++] = start | 7 << 20;
         int limit = r * r;
         while (head < tail) {
            int st = q[head++];
            int cell = st & 0xFFFFF;
            int entry = st >> 20 & 7;
            int dirs = st >> 23 & 63;
            int y = cell & 15;
            int column = cell >> 4;
            int dx = column % side - r;
            int dz = column / side - r;
            Chunk c = this.grid[column];
            long conn = c == null || !c.sections[y].connKnown ? ALL : c.sections[y].conn;
            for (int f = 0; f < 6; f++) {
               if (f == entry || entry != 7 && !connected(conn, entry, f) || (dirs >> (f ^ 1) & 1) != 0) {
                  continue;
               }

               int ny = y + DY[f];
               int ndx = dx + DX[f];
               int ndz = dz + DZ[f];
               if (ny < 0 || ny > 15 || ndx * ndx + ndz * ndz > limit) {
                  continue;
               }

               int ncell = ((ndz + r) * side + ndx + r) * 16 + ny;
               int bit = 1 << (f ^ 1);
               if ((this.seen[ncell] & (bit | 0x40)) != 0) {
                  continue; // been through this face already, or one of the sections the walk starts with (they are open to every side)
               }

               if (vis != null
                  && !vis.test((float)((cx + ndx) * 16 + 8 - camX), (float)(ny * 16 + 8 - camY), (float)((cz + ndz) * 16 + 8 - camZ))) {
                  continue;
               }

               if (this.seen[ncell] == 0) {
                  this.order[this.count++] = ncell;
               }

               this.seen[ncell] |= (byte)bit;
               q[tail++] = ncell | (f ^ 1) << 20 | (dirs | 1 << f) << 23;
            }
         }

         // the 26 sections that touch the camera's are always taken (a ray can slip past an edge or a corner; a camera inside a block sees nothing but them)
         for (int dy = -1; dy <= 1; dy++) {
            for (int dz = -1; dz <= 1; dz++) {
               for (int dx = -1; dx <= 1; dx++) {
                  int y = sy + dy;
                  if (y < 0 || y > 15 || dx < -r || dx > r || dz < -r || dz > r) {
                     continue;
                  }

                  int cell = ((dz + r) * side + dx + r) * 16 + y;
                  if (this.seen[cell] == 0) {
                     this.seen[cell] = (byte)0x40;
                     this.order[this.count++] = cell;
                  }
               }
            }
         }
         this.centerCx = cx;
         this.centerCz = cz;
         this.hasRun = true;
      }
   }
}
