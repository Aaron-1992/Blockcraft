package bc;

import static bc.GameTests.check;
import static bc.GameTests.eq;
import static bc.GameTests.test;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Headless tests of Update 3.14, the structure overhaul: the building templates (that every bed, chest and furnace can be reached from the
 * street, that turning keeps everything in place), the new villages (streets, plots, doors, foundations, no trees in the houses) and the other
 * structures.   python3 tools/runtests.py unit
 */
final class Tests314 {
   static void run() {
      templates();
      villages();
      settings();
   }

   // ------------------------------------------------------------------ templates
   static boolean blocks(char c) {
      return Tpl.solidTok(c) || c == 'F' || c == 'G' || c == 'O';
   }

   /** The cell of a template; outside the box it is air (above the ground) and the ground itself at y = 0. */
   static char cell(Tpl t, int x, int y, int z) {
      if (x < 0 || z < 0 || x >= t.w || z >= t.d || y >= t.h) {
         return y <= 0 ? 'C' : ' ';
      }

      if (y < 0) {
         return 'C';
      }

      char c = t.tok[t.idx(x, y, z)];
      return c == ' ' && y == 0 ? 'C' : c;
   }

   static boolean standable(Tpl t, int x, int y, int z) {
      char here = cell(t, x, y, z);
      if (here == 'H') {
         return true;
      }

      if (blocks(here) || blocks(cell(t, x, y + 1, z))) {
         return false;
      }

      char below = cell(t, x, y - 1, z);
      return Tpl.solidTok(below) || below == 'b' || below == 'B' || below == '+' || below == ':';
   }

   /** Everything a player can reach from the doorstep (or from the front of the template when it has no door). */
   static boolean[] reach(Tpl t) {
      int w = t.w + 2;
      int d = t.d + 2;
      int h = t.h + 2;
      boolean[] seen = new boolean[w * d * h];
      ArrayDeque<int[]> q = new ArrayDeque<>();
      int sx = t.doors.isEmpty() ? t.w / 2 : t.doors.get(0)[0];
      int[] start = {sx, t.sink + 1, -1};
      seen[((start[0] + 1) * d + (start[2] + 1)) * h + start[1]] = true;
      q.add(start);
      int[][] nb = {{1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}};
      while (!q.isEmpty()) {
         int[] p = q.poll();
         for (int[] n : nb) {
            for (int dy = -1; dy <= 1; dy++) {
               int nx = p[0] + n[0];
               int ny = p[1] + dy;
               int nz = p[2] + n[2];
               if (nx < -1 || nz < -1 || nx > t.w || nz > t.d || ny < 0 || ny >= t.h + 1) {
                  continue;
               }

               if (dy == 1 && blocks(cell(t, p[0], p[1] + 2, p[2]))) {
                  continue;
               }

               if (!standable(t, nx, ny, nz)) {
                  continue;
               }

               int key = ((nx + 1) * d + (nz + 1)) * h + ny;
               if (!seen[key]) {
                  seen[key] = true;
                  q.add(new int[]{nx, ny, nz});
               }
            }
         }

         // a ladder: up and down along it
         if (cell(t, p[0], p[1], p[2]) == 'H' || cell(t, p[0], p[1] - 1, p[2]) == 'H') {
            for (int dy = -1; dy <= 1; dy += 2) {
               int ny = p[1] + dy;
               if (ny >= 0 && ny < t.h + 1 && (cell(t, p[0], ny, p[2]) == 'H' || standable(t, p[0], ny, p[2]))) {
                  int key = ((p[0] + 1) * d + (p[2] + 1)) * h + ny;
                  if (!seen[key]) {
                     seen[key] = true;
                     q.add(new int[]{p[0], ny, p[2]});
                  }
               }
            }
         }
      }

      return seen;
   }

   static boolean reached(Tpl t, boolean[] seen, int x, int y, int z) {
      int d = t.d + 2;
      int h = t.h + 2;
      for (int[] n : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {0, 0}}) {
         int nx = x + n[0];
         int nz = z + n[1];
         if (nx >= -1 && nz >= -1 && nx <= t.w && nz <= t.d && y < h && seen[((nx + 1) * d + (nz + 1)) * h + y]) {
            return true;
         }
      }

      return false;
   }

   static void templates() {
      test("templates: every building has a sane size, no unknown character, a door where its outside is, a wall behind every ladder and torch", () -> {
         check(Buildings314.ALL.size() >= 20, "at least twenty templates: " + Buildings314.ALL.size());
         for (Tpl t : Buildings314.ALL.values()) {
            check(t.w >= 3 && t.w <= 17 && t.d >= 3 && t.d <= 17 && t.h >= 2 && t.h <= 26, t.name + " has a size of " + t.w + " x " + t.h + " x " + t.d);
            Style st = Style.of(Style.OAK, 0);
            int solid = 0;
            for (int y = 0; y < t.h; y++) {
               for (int z = 0; z < t.d; z++) {
                  for (int x = 0; x < t.w; x++) {
                     char c = t.tok[t.idx(x, y, z)];
                     if (c != ' ') {
                        int id = st.resolve(c, t.dir[t.idx(x, y, z)], 0, x, y, z, 1);
                        check(id >= 0, t.name + ": the character '" + c + "' at " + x + "," + y + "," + z + " means nothing");
                        if (id > 0 && Blocks.solid[id]) {
                           solid++;
                        }

                        if (c == '*' || c == 'H') {
                           int dd = t.dir[t.idx(x, y, z)];
                           check(dd >= 0, t.name + ": " + c + " at " + x + "," + y + "," + z + " has no wall");
                        }
                     }
                  }
               }
            }

            check(solid > 20, t.name + " is not empty");
            for (int[] dr : t.doors) {
               check(t.at(dr[0], dr[1] + 1, dr[2]) == '|', t.name + ": a door is two blocks high");
               int ox = dr[0] + Tpl.DX[dr[3]];
               int oz = dr[2] + Tpl.DZ[dr[3]];
               check(!blocks(cell(t, ox, dr[1], oz)) && !blocks(cell(t, ox, dr[1] + 1, oz)), t.name + ": the door opens onto something free");
            }
         }
      });

      test("templates: from the doorstep you reach every bed, chest, crafting table and furnace; ladders lead up to the floors above", () -> {
         for (Tpl t : Buildings314.ALL.values()) {
            boolean[] seen = reach(t);
            for (int y = 0; y < t.h; y++) {
               for (int z = 0; z < t.d; z++) {
                  for (int x = 0; x < t.w; x++) {
                     char c = t.tok[t.idx(x, y, z)];
                     if (c == 'b' || c == 'B' || c == 'x' || c == 'k' || c == 'u') {
                        check(reached(t, seen, x, y, z), t.name + ": '" + c + "' at " + x + "," + y + "," + z + " cannot be reached");
                     }

                     if (c == 'H' && y + 1 < t.h && t.tok[t.idx(x, y + 1, z)] != 'H') {
                        check(reached(t, seen, x, y, z) || seen[((x + 1) * (t.d + 2) + (z + 1)) * (t.h + 2) + y], t.name + ": the top of the ladder at " + x + "," + y + "," + z + " cannot be reached");
                     }
                  }
               }
            }
         }
      });

      test("templates: turned four ways each one lands where it should - the same blocks, the door on the street side, the doorstep free", () -> {
         for (Tpl t : Buildings314.ALL.values()) {
            Style st = Style.of(Style.OAK, 0);
            int base = -1;
            for (int q = 0; q < 4; q++) {
               Chunk[][] cs = scratch();
               int x0 = 5;
               int z0 = 6;
               for (Chunk[] row : cs) {
                  for (Chunk c : row) {
                     t.prepare(c, st, q, x0, 60, z0, Blocks.GRASS);
                     t.paste(c, st, q, x0, 60, z0, 99);
                  }
               }

               int count = 0;
               for (int y = 61; y < 61 + t.height() + 3; y++) {
                  for (int z = 0; z < 32; z++) {
                     for (int x = 0; x < 32; x++) {
                        if (blockAt(cs, x, y, z) != 0) {
                           count++;
                           check(x >= x0 && x < x0 + t.fw(q) && z >= z0 && z < z0 + t.fd(q), t.name + " turned " + q + ": a block outside the footprint at " + x + "," + y + "," + z);
                        }
                     }
                  }
               }

               if (base < 0) {
                  base = count;
               }

               eq(base, count, t.name + " turned " + q + ": as many blocks as turned 0");
               for (int[] dr : t.doors) {
                  int[] wp = t.world(dr[0], dr[2], q, x0, z0);
                  int id = blockAt(cs, wp[0], 60 - t.sink + dr[1], wp[1]);
                  check(Blocks.isDoorLo(id), t.name + " turned " + q + ": the door is a door (" + id + ")");
                  check(Blocks.doorFacing(id) == Tpl.rotDir(dr[3], q), t.name + " turned " + q + ": the door looks " + Blocks.doorFacing(id) + " not " + Tpl.rotDir(dr[3], q));
                  int od = Tpl.rotDir(dr[3], q);
                  int ox = wp[0] + Tpl.DX[od];
                  int oz = wp[1] + Tpl.DZ[od];
                  int below = blockAt(cs, ox, 60, oz);
                  check(below != 0 && Blocks.solid[below], t.name + " turned " + q + ": ground in front of the door");
                  check(blockAt(cs, ox, 61, oz) == 0 || !Blocks.solid[blockAt(cs, ox, 61, oz)], t.name + " turned " + q + ": nothing in front of the door");
               }
            }
         }
      });
   }

   static Chunk[][] scratch() {
      Chunk[][] cs = new Chunk[2][2];
      for (int cz = 0; cz < 2; cz++) {
         for (int cx = 0; cx < 2; cx++) {
            Chunk c = new Chunk(cx, cz);
            for (int x = 0; x < 16; x++) {
               for (int z = 0; z < 16; z++) {
                  for (int y = 0; y <= 60; y++) {
                     c.blocks[Chunk.idx(x, y, z)] = (short)(y == 60 ? Blocks.GRASS : (y > 55 ? Blocks.DIRT : Blocks.STONE));
                  }
               }
            }

            cs[cx][cz] = c;
         }
      }

      return cs;
   }

   static int blockAt(Chunk[][] cs, int x, int y, int z) {
      if (x < 0 || z < 0 || x >= 32 || z >= 32 || y < 0 || y > 255) {
         return 0;
      }

      return cs[x >> 4][z >> 4].blocks[Chunk.idx(x & 15, y, z & 15)] & 0xFFFF;
   }

   // ------------------------------------------------------------------ villages
   static Gen gen(long seed, int type) {
      WorldSettings st = new WorldSettings();
      st.type = type;
      return Dim.makeGen(Dim.OVERWORLD, seed, st);
   }

   static boolean intersects(int[] a, int[] b) {
      return a[2] >= b[0] && a[0] <= b[2] && a[3] >= b[1] && a[1] <= b[3];
   }

   static void villages() {
      test("villages: a seed has several, they are the same every time, in the style of their biome, and every building stands inside the village", () -> {
         boolean[] styles = new boolean[5];
         int total = 0;
         for (long seed = 1; seed <= 40; seed++) {
            Gen g = gen(seed, WorldSettings.DEFAULT);
            Gen g2 = gen(seed, WorldSettings.DEFAULT);
            for (int rx = -2; rx <= 2; rx++) {
               for (int rz = -2; rz <= 2; rz++) {
                  Structs.Village v = g.structs.village(rx, rz);
                  Structs.Village w = g2.structs.village(rx, rz);
                  eq(v.valid, w.valid, "determinism of the region " + rx + "," + rz);
                  if (!v.valid) {
                     continue;
                  }

                  total++;
                  check(v.style >= 0 && v.style < 5, "a new style");
                  styles[v.style] = true;
                  eq(v.placed.size(), w.placed.size(), "the same number of buildings");
                  check(v.placed.size() >= 4, "a village of " + v.placed.size() + " buildings at " + v.cx + "," + v.cz + " (seed " + seed + ")");
                  check(!v.streets.isEmpty(), "streets");
                  for (int i = 0; i < v.placed.size(); i++) {
                     Village314.Placed p = v.placed.get(i);
                     Village314.Placed o = w.placed.get(i);
                     check(p.x0 == o.x0 && p.z0 == o.z0 && p.y0 == o.y0 && p.q == o.q && p.tpl == o.tpl, "building " + i + " is where it was");
                     check(p.x0 >= v.minX && p.x0 + p.fw - 1 <= v.maxX && p.z0 >= v.minZ && p.z0 + p.fd - 1 <= v.maxZ, "inside the bounds of the village");
                  }
               }
            }
         }

         check(total >= 40, "villages exist: " + total);
         check(styles[Style.OAK] && styles[Style.DESERT] && styles[Style.SPRUCE] && styles[Style.SNOW], "oak, desert, spruce and snow villages turn up in 40 seeds");
      });

      test("villages: no two buildings touch, none stands on a street, every one has its front on a street, ground and a free doorstep", () -> {
         int checked = 0;
         for (long seed = 1; seed <= 30; seed++) {
            Gen g = gen(seed, WorldSettings.DEFAULT);
            for (int rx = -1; rx <= 1; rx++) {
               for (int rz = -1; rz <= 1; rz++) {
                  Structs.Village v = g.structs.village(rx, rz);
                  if (!v.valid) {
                     continue;
                  }

                  checked++;
                  for (int i = 0; i < v.placed.size(); i++) {
                     Village314.Placed a = v.placed.get(i);
                     int[] ra = {a.x0, a.z0, a.x0 + a.fw - 1, a.z0 + a.fd - 1};
                     for (int j = i + 1; j < v.placed.size(); j++) {
                        Village314.Placed b = v.placed.get(j);
                        int[] rb = {b.x0 - 1, b.z0 - 1, b.x0 + b.fw, b.z0 + b.fd};
                        check(!intersects(ra, rb), "buildings " + a.tpl.name + " and " + b.tpl.name + " of the village at " + v.cx + "," + v.cz + " touch");
                     }

                     if (a.tpl.name.equals("well")) {
                        continue;
                     }

                     for (Structs.Path p : v.paths) {
                        check(!intersects(ra, new int[]{p.x0, p.z0, p.x1, p.z1}), a.tpl.name + " stands on a street");
                     }

                     // the front of the building looks at a street: the cell in front of the middle of its front row is part of a street
                     int fd = Tpl.frontDir(a.q);
                     int[] mid = a.tpl.world(a.tpl.w / 2, 0, a.q, a.x0, a.z0);
                     int fx = mid[0] + Tpl.DX[fd];
                     int fz = mid[1] + Tpl.DZ[fd];
                     boolean onStreet = false;
                     for (Structs.Path p : v.paths) {
                        if (fx >= p.x0 && fx <= p.x1 && fz >= p.z0 && fz <= p.z1) {
                           onStreet = true;
                        }
                     }

                     check(onStreet, a.tpl.name + " of the village at " + v.cx + "," + v.cz + " does not look at a street");
                  }
               }
            }
         }

         check(checked >= 20, "villages checked: " + checked);
      });

      test("villages: drawn into the chunks - doors are doors, nothing grows through the houses, floors rest on the ground, streets are paths, lamps stand", () -> {
         int villagesChecked = 0;
         int doors = 0;
         int beds = 0;
         int lampsFound = 0;
         int pathCells = 0;
         int paved = 0;
         for (long seed = 3; seed <= 40 && villagesChecked < 5; seed += 3) {
            Gen g = gen(seed, WorldSettings.DEFAULT);
            Structs.Village v = null;
            for (int rx = -1; rx <= 1 && v == null; rx++) {
               for (int rz = -1; rz <= 1 && v == null; rz++) {
                  Structs.Village c = g.structs.village(rx, rz);
                  if (c.valid && c.placed.size() >= 8) {
                     v = c;
                  }
               }
            }

            if (v == null) {
               continue;
            }

            villagesChecked++;
            int cx0 = Math.floorDiv(v.minX - 3, 16);
            int cx1 = Math.floorDiv(v.maxX + 3, 16);
            int cz0 = Math.floorDiv(v.minZ - 3, 16);
            int cz1 = Math.floorDiv(v.maxZ + 3, 16);
            Map<Long, Chunk> map = new HashMap<>();
            for (int cz = cz0; cz <= cz1; cz++) {
               for (int cx = cx0; cx <= cx1; cx++) {
                  map.put((long)cx << 32 ^ cz & 0xFFFFFFFFL, g.generate(cx, cz));
               }
            }

            for (Village314.Placed p : v.placed) {
               Tpl t = p.tpl;
               for (int[] dr : t.doors) {
                  int[] wp = t.world(dr[0], dr[2], p.q, p.x0, p.z0);
                  int id = get(map, wp[0], p.y0 + 1, wp[1]);
                  int up = get(map, wp[0], p.y0 + 2, wp[1]);
                  check(Blocks.isDoorLo(id) && Blocks.isDoorHi(up), t.name + " at " + p.x0 + "," + p.z0 + ": door " + id + "/" + up);
                  doors++;
                  int od = Tpl.rotDir(dr[3], p.q);
                  int sx = wp[0] + Tpl.DX[od];
                  int sz = wp[1] + Tpl.DZ[od];
                  int step = get(map, sx, p.y0, sz);
                  check(step > 0 && Blocks.solid[step], t.name + ": the doorstep has ground");
                  check(!Blocks.solid[get(map, sx, p.y0 + 1, sz)] && !Blocks.solid[get(map, sx, p.y0 + 2, sz)], t.name + ": the doorstep is free");
               }

               // nothing of a tree or a bush inside the walls
               for (int z = p.z0; z < p.z0 + p.fd; z++) {
                  for (int x = p.x0; x < p.x0 + p.fw; x++) {
                     for (int y = p.y0 + 1; y <= p.y0 + t.height(); y++) {
                        int id = get(map, x, y, z);
                        check(!Blocks.isLeaves(id), t.name + ": leaves inside the building at " + x + "," + y + "," + z);
                     }

                     if (!t.name.equals("farm") && !t.name.equals("pen") && !t.name.equals("stall") && !t.name.equals("well")) {
                        int floor = get(map, x, p.y0 - 1, z);
                        check(floor > 0 && Blocks.solid[floor] && !Blocks.isFluid(floor), t.name + ": the ground under " + x + "," + z + " is " + floor);
                     }
                  }
               }

               for (int z = p.z0; z < p.z0 + p.fd; z++) {
                  for (int x = p.x0; x < p.x0 + p.fw; x++) {
                     for (int y = p.y0; y <= p.y0 + t.height(); y++) {
                        if (Blocks.isBed(get(map, x, y, z))) {
                           beds++;
                        }
                     }
                  }
               }
            }

            for (Structs.Path pa : v.paths) {
               for (int z = pa.z0; z <= pa.z1; z += 2) {
                  for (int x = pa.x0; x <= pa.x1; x += 2) {
                     boolean inBuilding = false;
                     for (Village314.Placed p : v.placed) {
                        if (x >= p.x0 - 1 && x <= p.x0 + p.fw && z >= p.z0 - 1 && z <= p.z0 + p.fd) {
                           inBuilding = true;
                        }
                     }

                     if (inBuilding) {
                        continue;
                     }

                     int y = -1;
                     for (int yy = 140; yy > 40 && y < 0; yy--) {
                        int id = get(map, x, yy, z);
                        if (id != 0 && !Structs.soft(id) && !Tpl.vegetation(id)) {
                           y = yy;
                        }
                     }

                     int top = y < 0 ? 0 : get(map, x, y, z);
                     Style s = Style.of(v.style, 0);
                     if (y >= 0) {
                        pathCells++;
                        if (top == s.path || top == s.path2) {
                           paved++;
                        }
                     }
                  }
               }
            }

            for (int[] lamp : v.lamps) {
               boolean found = false;
               for (int y = 150; y > 40 && !found; y--) {
                  if (get(map, lamp[0], y, lamp[1]) == Blocks.TORCH && Blocks.isFence(get(map, lamp[0], y - 1, lamp[1]))) {
                     found = true;
                  }
               }

               if (found) {
                  lampsFound++;
               }
            }
         }

         check(villagesChecked >= 3, "villages drawn: " + villagesChecked);
         check(doors >= 20, "doors found: " + doors);
         check(beds >= 10, "beds found: " + beds);
         check(pathCells > 100, "street cells checked: " + pathCells);
         check(paved * 10 >= pathCells * 7, "most of the street is paved: " + paved + " of " + pathCells);
         check(lampsFound >= 5, "lamp posts found: " + lampsFound);
      });
   }

   static int get(Map<Long, Chunk> map, int x, int y, int z) {
      Chunk c = map.get((long)(x >> 4) << 32 ^ (z >> 4) & 0xFFFFFFFFL);
      if (c == null || y < 0 || y > 255) {
         return 0;
      }

      return c.blocks[Chunk.idx(x & 15, y, z & 15)] & 0xFFFF;
   }

   // ------------------------------------------------------------------ settings
   static void settings() {
      test("settings: new worlds get the new structures, old worlds (classic, or from 3.13 without the key) keep the villages they have", () -> {
         eq(2, new WorldSettings().structVer, "a new world");
         eq(1, WorldSettings.classic().structVer, "classic");
         Properties p = new Properties();
         WorldSettings s = new WorldSettings();
         s.save(p);
         eq("2", p.getProperty("structver"), "saved");
         eq(2, WorldSettings.load(p).structVer, "loaded");
         p.remove("structver");
         eq(1, WorldSettings.load(p).structVer, "a world of 3.13 has no key: version 1");
         eq(1, WorldSettings.load(new Properties()).structVer, "an empty file is classic");
         WorldSettings c = new WorldSettings();
         c.structVer = 1;
         eq(1, c.copy().structVer, "copy keeps it");
         // an old-kind world makes old-kind villages, a new one new ones
         boolean oldSeen = false;
         boolean newSeen = false;
         for (long seed = 1; seed <= 12; seed++) {
            WorldSettings o = new WorldSettings();
            o.structVer = 1;
            Gen go = Dim.makeGen(Dim.OVERWORLD, seed, o);
            Gen gn = gen(seed, WorldSettings.DEFAULT);
            for (int rx = -1; rx <= 1; rx++) {
               for (int rz = -1; rz <= 1; rz++) {
                  Structs.Village a = go.structs.village(rx, rz);
                  Structs.Village b = gn.structs.village(rx, rz);
                  if (a.valid) {
                     eq(-1, a.style, "an old world has old villages");
                     oldSeen = true;
                  }

                  if (b.valid) {
                     check(b.style >= 0, "a new world has new villages");
                     newSeen = true;
                  }
               }
            }
         }

         check(oldSeen && newSeen, "both kinds were seen");
      });
   }
}
