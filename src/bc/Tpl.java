package bc;

import java.util.ArrayList;
import java.util.List;

/**
 * Update 3.14: a building template - a box of cells, each one character: what the cell is made of ({@link Style} says what the
 * character means in a village of oak, sandstone, acacia or spruce), turned a quarter at a time and pasted into the chunks it touches.
 * <pre>
 * ' ' leave the cell alone            '.' air
 * '#' wall   'P' planks   'E' floor   'C' foundation   'c' mossy cobble   'S' stone bricks   'K' bricks
 * 'L' log   'X' log along x   'Z' log along z      'R' roof block   '_' roof slab   '-' slab in the top half
 * '^' 'v' '&gt;' '&lt;' roof stairs whose high side is north, south, east, west
 * 'G' glass pane   'g' glass   'F' fence   'f' fence gate   'T' torch   '*' torch on the wall next to it   '$' glowstone
 * '!' door (write it in both cells)   'H' ladder (on the wall next to it)   'b' 'B' foot and head of a bed (next to each other)
 * 'k' crafting table   'u' furnace   'x' loot chest   'q' bookshelf   'h' hay   'w' 'W' wool   '+' ':' carpet
 * 'o' water   'a' wet farmland   'r' crop   'y' dirt   'm' path   'n' coarse dirt   '1' '2' '3' trim blocks   'O' pumpkin   'z' a flower
 * </pre>
 * The template looks north (-z) at the street: the door is in the row z = 1 (row 0 is the doorstep), x grows to the right. {@link #paste}
 * turns it by q quarter turns clockwise seen from above.
 */
final class Tpl {
   /** The four directions: north (-z), south (+z), east (+x), west (-x). */
   static final int[] DX = {0, 0, 1, -1};
   static final int[] DZ = {-1, 1, 0, 0};
   /** The direction after a clockwise quarter turn. */
   static final int[] CW = {2, 3, 1, 0};
   static final int[] OPP = {1, 0, 3, 2};

   final String name;
   final int w;
   final int h;
   final int d;
   final char[] tok;
   final byte[] dir;
   /** Every door: {x, y, z, outward direction} (the lower half). */
   final List<int[]> doors = new ArrayList<>();
   int chests;
   int beds;
   /** How many layers (counted from layer 0) lie below the ground: a well, a cellar. The ground floor is layer {@link #sink}. */
   final int sink;

   private Tpl(String name, int w, int h, int d, char[] g, int sink) {
      this.name = name;
      this.w = w;
      this.h = h;
      this.d = d;
      this.sink = sink;
      this.tok = g;
      this.dir = new byte[g.length];
      java.util.Arrays.fill(this.dir, (byte)-1);
      this.resolveMarkers();
   }

   int idx(int x, int y, int z) {
      return (y * this.d + z) * this.w + x;
   }

   char at(int x, int y, int z) {
      if (x < 0 || y < 0 || z < 0 || x >= this.w || y >= this.h || z >= this.d) {
         return ' ';
      }

      return this.tok[this.idx(x, y, z)];
   }

   static boolean solidTok(char c) {
      switch (c) {
         case ' ':
         case '.':
         case 'T':
         case 'F':
         case 'f':
         case '*':
         case 'G':
         case 'H':
         case '!':
         case '|':
         case 'b':
         case 'B':
         case '+':
         case ':':
         case 'o':
         case 'r':
         case 'z':
         case 'O':
            return false;
         default:
            return true;
      }
   }

   /** Works out the directions that the template does not spell out: where a door looks, which wall a torch or a ladder hangs on, where the head of a bed is, which way a gate runs. */
   private void resolveMarkers() {
      for (int y = 0; y < this.h; y++) {
         for (int z = 0; z < this.d; z++) {
            for (int x = 0; x < this.w; x++) {
               int i = this.idx(x, y, z);
               char c = this.tok[i];
               switch (c) {
                  case '!': {
                     boolean upper = y > 0 && this.tok[this.idx(x, y - 1, z)] == '!';
                     int toN = z;
                     int toS = this.d - 1 - z;
                     int toE = this.w - 1 - x;
                     int toW = x;
                     int best = Math.min(Math.min(toN, toS), Math.min(toE, toW));
                     int out = toN == best ? 0 : (toS == best ? 1 : (toE == best ? 2 : 3));
                     this.dir[i] = (byte)out;
                     if (upper) {
                        this.tok[i] = '|';
                     } else {
                        this.doors.add(new int[]{x, y, z, out});
                     }

                     break;
                  }
                  case '*':
                  case 'H': {
                     int found = -1;
                     for (int k = 0; k < 4 && found < 0; k++) {
                        if (solidTok(this.at(x + DX[k], y, z + DZ[k]))) {
                           found = k;
                        }
                     }

                     this.dir[i] = (byte)found;
                     if (c == 'H' && found < 0) {
                        throw new IllegalArgumentException(this.name + ": ladder at " + x + "," + y + "," + z + " has no wall");
                     }

                     break;
                  }
                  case 'b': {
                     int head = -1;
                     for (int k = 0; k < 4 && head < 0; k++) {
                        if (this.at(x + DX[k], y, z + DZ[k]) == 'B') {
                           head = k;
                        }
                     }

                     if (head < 0) {
                        throw new IllegalArgumentException(this.name + ": bed foot at " + x + "," + y + "," + z + " has no head");
                     }

                     this.dir[i] = (byte)head;
                     this.dir[this.idx(x + DX[head], y, z + DZ[head])] = (byte)head;
                     this.beds++;
                     break;
                  }
                  case 'f': {
                     boolean alongX = this.at(x - 1, y, z) == 'F' || this.at(x + 1, y, z) == 'F' || solidTok(this.at(x - 1, y, z)) && solidTok(this.at(x + 1, y, z));
                     this.dir[i] = (byte)(alongX ? 0 : 2);
                     break;
                  }
                  case '^':
                     this.dir[i] = 0;
                     break;
                  case 'v':
                     this.dir[i] = 1;
                     break;
                  case '>':
                     this.dir[i] = 2;
                     break;
                  case '<':
                     this.dir[i] = 3;
                     break;
                  case 'x':
                     this.chests++;
                     break;
                  default:
                     break;
               }
            }
         }
      }
   }

   // ------------------------------------------------------------------ turning
   /** How high the template stands above the ground (its layers from the ground floor up). */
   int height() {
      return this.h - this.sink;
   }

   /** The same template without the layers above y (for looking into a house from above). */
   Tpl cut(int maxY) {
      int nh = Math.min(this.h, maxY + 1);
      char[] g = new char[this.w * nh * this.d];
      System.arraycopy(this.tok, 0, g, 0, g.length);
      return new Tpl(this.name + "_cut", this.w, nh, this.d, g, this.sink);
   }

   int fw(int q) {
      return (q & 1) == 0 ? this.w : this.d;
   }

   int fd(int q) {
      return (q & 1) == 0 ? this.d : this.w;
   }

   static int rotX(int x, int z, int w, int d, int q) {
      switch (q & 3) {
         case 0:
            return x;
         case 1:
            return d - 1 - z;
         case 2:
            return w - 1 - x;
         default:
            return z;
      }
   }

   static int rotZ(int x, int z, int w, int d, int q) {
      switch (q & 3) {
         case 0:
            return z;
         case 1:
            return x;
         case 2:
            return d - 1 - z;
         default:
            return w - 1 - x;
      }
   }

   static int rotDir(int dir, int q) {
      for (int i = 0; i < (q & 3); i++) {
         dir = CW[dir];
      }

      return dir;
   }

   /** The world direction that the front of the template (north) looks in after q quarter turns. */
   static int frontDir(int q) {
      return rotDir(0, q);
   }

   // ------------------------------------------------------------------ into the world
   static boolean vegetation(int id) {
      return id == 29 || id == 30 || id == 31 || Blocks.isLeaves(id) || Blocks.isAnyLog(id) || Blocks313.isFlower(id) || Blocks313.isDouble(id) || Blocks313.isVine(id)
         || id == Blocks313.SNOW_LAYER || id == Blocks313.FERN || id == Blocks313.DEAD_BUSH || id == Blocks313.MUSHROOM_BROWN || id == Blocks313.MUSHROOM_RED || id == Blocks.CACTUS
         || id == Blocks313.LILY_PAD || id == Blocks.SUGAR_CANE || id == Blocks313.MUSH_BLOCK_RED || id == Blocks313.MUSH_BLOCK_BROWN || id == Blocks313.MUSH_STEM;
   }

   /**
    * Makes room for the template in this chunk: trees, plants and snow in its volume go, and the ground below the footprint is filled up
    * (foundation, at most twelve blocks deep) so that the building does not hang in the air on a slope.
    */
   void prepare(Chunk c, Style st, int q, int x0, int y0, int z0, int ground) {
      int fw = this.fw(q);
      int fd = this.fd(q);
      if (!Structs.overlaps(c, x0, z0, x0 + fw - 1, z0 + fd - 1)) {
         return;
      }

      for (int z = 0; z < fd; z++) {
         for (int x = 0; x < fw; x++) {
            int wx = x0 + x;
            int wz = z0 + z;
            int lx = wx - c.cx * 16;
            int lz = wz - c.cz * 16;
            if (lx < 0 || lx > 15 || lz < 0 || lz > 15) {
               continue;
            }

            // the plot is levelled: whatever stands above the floor inside the footprint (hill, trees, plants) goes
            for (int y = y0 + 1; y <= Math.min(250, y0 + this.height() + 2); y++) {
               c.blocks[Chunk.idx(lx, y, lz)] = 0;
            }

            for (int y = y0, n = 0; y >= 1 && n < 12; y--, n++) {
               int id = c.blocks[Chunk.idx(lx, y, lz)] & 0xFFFF;
               if (id != 0 && !Structs.soft(id) && !vegetation(id)) {
                  break;
               }

               c.blocks[Chunk.idx(lx, y, lz)] = (short)(y == y0 ? ground : st.base);
            }
         }
      }
   }

   /** Pastes the part of the template that lies in chunk c. (x0, z0) is the corner of the turned footprint with the smallest x and z, y0 the height of layer 0. */
   void paste(Chunk c, Style st, int q, int x0, int y0, int z0, int salt) {
      int fw = this.fw(q);
      int fd = this.fd(q);
      if (!Structs.overlaps(c, x0, z0, x0 + fw - 1, z0 + fd - 1)) {
         return;
      }

      for (int y = 0; y < this.h; y++) {
         int wy = y0 - this.sink + y;
         if (wy < 1 || wy > 250) {
            continue;
         }

         for (int z = 0; z < this.d; z++) {
            for (int x = 0; x < this.w; x++) {
               int i = this.idx(x, y, z);
               char t = this.tok[i];
               if (t == ' ') {
                  continue;
               }

               int wx = x0 + rotX(x, z, this.w, this.d, q);
               int wz = z0 + rotZ(x, z, this.w, this.d, q);
               int lx = wx - c.cx * 16;
               int lz = wz - c.cz * 16;
               if (lx < 0 || lx > 15 || lz < 0 || lz > 15) {
                  continue;
               }

               int dw = this.dir[i] < 0 ? -1 : rotDir(this.dir[i], q);
               int id = st.resolve(t, dw, q, wx, wy, wz, salt);
               if (id >= 0) {
                  c.blocks[Chunk.idx(lx, wy, lz)] = (short)id;
               }
            }
         }
      }
   }

   /** The world position (x, z) of a local cell after the template was turned and put at (x0, z0). */
   int[] world(int x, int z, int q, int x0, int z0) {
      return new int[]{x0 + rotX(x, z, this.w, this.d, q), z0 + rotZ(x, z, this.w, this.d, q)};
   }

   /** For the tests and for looking: the layers of the template as text. */
   String dump() {
      StringBuilder sb = new StringBuilder();
      sb.append(this.name).append(' ').append(this.w).append('x').append(this.h).append('x').append(this.d).append('\n');
      for (int y = 0; y < this.h; y++) {
         sb.append("y=").append(y).append('\n');
         for (int z = 0; z < this.d; z++) {
            for (int x = 0; x < this.w; x++) {
               char t = this.tok[this.idx(x, y, z)];
               sb.append(t == ' ' ? '_' : t);
            }

            sb.append('\n');
         }
      }

      return sb.toString();
   }

   // ------------------------------------------------------------------ building one
   /** Builds a template from boxes, rows of text and roofs. */
   static final class B {
      final String name;
      final int w;
      final int h;
      final int d;
      final char[] g;
      int sink;

      B(String name, int w, int h, int d) {
         this.name = name;
         this.w = w;
         this.h = h;
         this.d = d;
         this.g = new char[w * h * d];
         java.util.Arrays.fill(this.g, ' ');
      }

      /** The first n layers lie below the ground (the ground floor is layer n). */
      B sink(int n) {
         this.sink = n;
         return this;
      }

      private int idx(int x, int y, int z) {
         return (y * this.d + z) * this.w + x;
      }

      B set(char t, int x, int y, int z) {
         if (x < 0 || y < 0 || z < 0 || x >= this.w || y >= this.h || z >= this.d) {
            throw new IllegalArgumentException(this.name + ": cell " + x + "," + y + "," + z + " is outside " + this.w + "x" + this.h + "x" + this.d);
         }

         this.g[this.idx(x, y, z)] = t;
         return this;
      }

      char get(int x, int y, int z) {
         if (x < 0 || y < 0 || z < 0 || x >= this.w || y >= this.h || z >= this.d) {
            return ' ';
         }

         return this.g[this.idx(x, y, z)];
      }

      /** A solid box (both corners included, in any order). */
      B box(char t, int xa, int ya, int za, int xb, int yb, int zb) {
         for (int y = Math.min(ya, yb); y <= Math.max(ya, yb); y++) {
            for (int z = Math.min(za, zb); z <= Math.max(za, zb); z++) {
               for (int x = Math.min(xa, xb); x <= Math.max(xa, xb); x++) {
                  this.set(t, x, y, z);
               }
            }
         }

         return this;
      }

      /** Only the four walls of a box (no floor, no ceiling, nothing inside). */
      B walls(char t, int xa, int ya, int za, int xb, int yb, int zb) {
         int x0 = Math.min(xa, xb);
         int x1 = Math.max(xa, xb);
         int z0 = Math.min(za, zb);
         int z1 = Math.max(za, zb);
         for (int y = Math.min(ya, yb); y <= Math.max(ya, yb); y++) {
            for (int z = z0; z <= z1; z++) {
               for (int x = x0; x <= x1; x++) {
                  if (x == x0 || x == x1 || z == z0 || z == z1) {
                     this.set(t, x, y, z);
                  }
               }
            }
         }

         return this;
      }

      /** Rows of text for one layer; rows run from z = 0, columns from x = 0; a space leaves the cell as it is. */
      B layer(int y, String... rows) {
         for (int z = 0; z < rows.length; z++) {
            for (int x = 0; x < rows[z].length(); x++) {
               char t = rows[z].charAt(x);
               if (t != ' ') {
                  this.set(t, x, y, z);
               }
            }
         }

         return this;
      }

      /**
       * A gable roof whose ridge runs along x over the body x0..x1 x z0..z1; the first layer lies at height y. The eaves reach one block beyond the
       * walls at the front and the back (z0 - 1 and z1 + 1); the three-cornered gap under the roof at both ends is closed with wall.
       */
      B gableX(int y, int x0, int x1, int z0, int z1, char wall) {
         int a = z0 - 1;
         int b = z1 + 1;
         for (int z = a; z <= b; z++) {
            int lvl = Math.min(z - a, b - z);
            boolean ridge = z - a == b - z;
            char t = ridge ? 'R' : (z - a < b - z ? 'v' : '^');
            for (int x = x0; x <= x1; x++) {
               this.set(t, x, y + lvl, z);
            }

            if (z >= z0 && z <= z1) {
               for (int yy = y; yy < y + lvl; yy++) {
                  this.set(wall, x0, yy, z);
                  this.set(wall, x1, yy, z);
               }
            }
         }

         return this;
      }

      /** A gable roof whose ridge runs along z over the body x0..x1 x z0..z1, eaves at x0 - 1 and x1 + 1, the gaps at the ends (z0 and z1) closed with wall. */
      B gableZ(int y, int x0, int x1, int z0, int z1, char wall) {
         int a = x0 - 1;
         int b = x1 + 1;
         for (int x = a; x <= b; x++) {
            int lvl = Math.min(x - a, b - x);
            boolean ridge = x - a == b - x;
            char t = ridge ? 'R' : (x - a < b - x ? '>' : '<');
            for (int z = z0; z <= z1; z++) {
               this.set(t, x, y + lvl, z);
            }

            if (x >= x0 && x <= x1) {
               for (int yy = y; yy < y + lvl; yy++) {
                  this.set(wall, x, yy, z0);
                  this.set(wall, x, yy, z1);
               }
            }
         }

         return this;
      }

      /** A hipped roof (four slopes) over the body, one block of eaves all round; ends in a block of roof on top. */
      B hip(int y, int x0, int x1, int z0, int z1) {
         int xa = x0 - 1;
         int xb = x1 + 1;
         int za = z0 - 1;
         int zb = z1 + 1;
         int shortSide = Math.min(xb - xa, zb - za) + 1;
         int maxLvl = (shortSide - 1) / 2;
         for (int z = za; z <= zb; z++) {
            for (int x = xa; x <= xb; x++) {
               int dxl = x - xa;
               int dxr = xb - x;
               int dzl = z - za;
               int dzr = zb - z;
               int m = Math.min(Math.min(dxl, dxr), Math.min(dzl, dzr));
               char t;
               if (m == maxLvl && shortSide % 2 == 1) {
                  t = 'R';
               } else if (dzl == m) {
                  t = 'v';
               } else if (dzr == m) {
                  t = '^';
               } else if (dxl == m) {
                  t = '>';
               } else {
                  t = '<';
               }

               this.set(t, x, y + m, z);
            }
         }

         return this;
      }

      Tpl build() {
         return new Tpl(this.name, this.w, this.h, this.d, this.g, this.sink);
      }
   }
}
