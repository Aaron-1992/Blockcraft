package bc;

import bc.Gen.Col;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Random;

/**
 * Update 3.14: the new villages. Where the old generator scattered a ring of boxes round a well, a village now has STREETS (a main street
 * through the middle with side streets, laid on the ground wherever it goes), a well in the square, and along the streets - on both sides,
 * their fronts to the road - the houses, shops and yards of {@link Buildings314}, turned the right way, set on a foundation, in the wood
 * and stone of the biome ({@link Style}). Farms lie at the ends of the streets, lamp posts stand along them, a watchtower now and then.
 * Everything is a function of the seed and the region of 320 x 320 blocks, so every chunk can draw its own piece.
 */
final class Village314 {
   private Village314() {
   }

   static final class Placed {
      Tpl tpl;
      Style st;
      int q;
      int x0;
      int y0;
      int z0;
      int salt;
      /** The footprint after turning. */
      int fw;
      int fd;
   }

   static final class Street {
      int x0;
      int z0;
      int x1;
      int z1;
      /** Half the width: the road covers c - hw .. c + hw. */
      int hw;
      boolean alongX;
      /** The coordinate of the middle line (z for a street along x, x for one along z). */
      int c;
   }

   private static final class Ctx {
      Structs s;
      Gen gen;
      Random r;
      Structs.Village v;
      int style;
      int cx;
      int cz;
      int cy;
      final ArrayList<int[]> rects = new ArrayList<>();
      final ArrayDeque<String> queue = new ArrayDeque<>();
      int houses;
      int farms;
      int stalls;
      int pens;
      int towers;
   }

   // ------------------------------------------------------------------ the plan
   static Structs.Village plan(Structs s, int rx, int rz) {
      Structs.Village v = new Structs.Village();
      Random r = new Random(s.seed * 6364136223846793005L + rx * 1442695040888963407L + rz * 987654321L + 31415L);
      if (r.nextFloat() > 0.7F) {
         return v;
      }

      Gen gen = s.gen;
      int cx = 0;
      int cz = 0;
      Col cc = null;
      for (int i = 0; i < 16 && cc == null; i++) {
         int x = rx * 320 + 70 + r.nextInt(180);
         int z = rz * 320 + 70 + r.nextInt(180);
         Col c = gen.column(x, z);
         if (Biomes.village(c.biome) && c.h >= 65 && c.h <= 92 && flatAround(gen, x, z, c.h)) {
            cx = x;
            cz = z;
            cc = c;
         }
      }

      if (cc == null) {
         return v;
      }

      Ctx k = new Ctx();
      k.s = s;
      k.gen = gen;
      k.r = r;
      k.v = v;
      k.style = Biomes.villagePalette(cc.biome);
      k.cx = cx;
      k.cz = cz;
      k.cy = cc.h;
      v.valid = true;
      v.cx = cx;
      v.cz = cz;
      v.cy = cc.h;
      v.style = k.style;
      v.desert = k.style == Style.DESERT;
      int target = 9 + r.nextInt(14);
      if (target >= 13) {
         k.queue.add("church");
      }

      k.queue.add("smithy");
      if (target >= 11) {
         k.queue.add("library");
      }

      if (target >= 10) {
         k.queue.add("butcher");
      }

      // the well in the square
      Tpl well = Buildings314.get("well");
      addPlaced(k, well, 0, cx - 2, cc.h, cz - 2, "well");

      // the streets
      boolean alongX = r.nextBoolean();
      int la = reach(gen, cx, cz, alongX ? -1 : 0, alongX ? 0 : -1, 18 + r.nextInt(26), cc.h);
      int lb = reach(gen, cx, cz, alongX ? 1 : 0, alongX ? 0 : 1, 18 + r.nextInt(26), cc.h);
      ArrayList<Street> streets = new ArrayList<>();
      streets.add(street(alongX, alongX ? cz : cx, alongX ? cx - la : cz - la, alongX ? cx + lb : cz + lb, 1));
      for (int sign = -1; sign <= 1; sign += 2) {
         int avail = sign < 0 ? la : lb;
         for (int n = 0; n < 2; n++) {
            if (r.nextInt(n == 0 ? 20 : 5) < (n == 0 ? 18 : 2) && avail >= 12 + n * 12) {
               int t = sign * (9 + n * 13 + r.nextInt(5));
               if (Math.abs(t) > avail - 3) {
                  continue;
               }

               int sx = alongX ? cx + t : cx;
               int sz = alongX ? cz : cz + t;
               int up = reach(gen, sx, sz, alongX ? 0 : -1, alongX ? -1 : 0, 9 + r.nextInt(14), gen.column(sx, sz).h);
               int down = reach(gen, sx, sz, alongX ? 0 : 1, alongX ? 1 : 0, 9 + r.nextInt(14), gen.column(sx, sz).h);
               if (up + down >= 8) {
                  streets.add(street(!alongX, alongX ? sx : sz, alongX ? sz - up : sx - up, alongX ? sz + down : sx + down, 1));
               }
            }
         }
      }

      for (Street st : streets) {
         v.streets.add(st);
         Structs.Path p = new Structs.Path();
         p.x0 = st.alongX ? st.x0 : st.c - st.hw;
         p.x1 = st.alongX ? st.x1 : st.c + st.hw;
         p.z0 = st.alongX ? st.c - st.hw : st.z0;
         p.z1 = st.alongX ? st.c + st.hw : st.z1;
         v.paths.add(p);
      }

      // the houses, shops and yards: round and round the streets until enough of them stand
      int rounds = 0;
      while (k.houses < target && rounds < 4) {
         for (int si = 0; si < streets.size() && k.houses < target; si++) {
            for (int side = -1; side <= 1 && k.houses < target; side += 2) {
               walkStreet(k, streets.get(si), side, rounds == 0 && si == 0 ? 14 : 30, target);
            }
         }

         rounds++;
      }

      // farms, a watchtower, pens: further out
      int farmsWanted = 2 + r.nextInt(3);
      for (int pass = 0; pass < 3 && k.farms < farmsWanted; pass++) {
         for (int si = streets.size() - 1; si >= 0 && k.farms < farmsWanted; si--) {
            for (int side = -1; side <= 1 && k.farms < farmsWanted; side += 2) {
               walkSpecial(k, streets.get(si), side, "farm", farmsWanted);
            }
         }
      }

      if (r.nextInt(3) != 0) {
         for (int si = streets.size() - 1; si >= 0 && k.towers < 1; si--) {
            walkSpecial(k, streets.get(si), r.nextBoolean() ? -1 : 1, "tower", 1);
         }
      }

      for (int si = 0; si < streets.size() && k.stalls < 3; si++) {
         walkSpecial(k, streets.get(si), r.nextBoolean() ? -1 : 1, "stall", 3);
      }

      // lamp posts along the streets, a spawn point for the villagers every few blocks
      for (Street st : streets) {
         lamps(k, st);
      }

      v.minX = v.minZ = Integer.MAX_VALUE;
      v.maxX = v.maxZ = Integer.MIN_VALUE;
      for (Structs.Bld b : v.blds) {
         v.minX = Math.min(v.minX, b.bx0);
         v.minZ = Math.min(v.minZ, b.bz0);
         v.maxX = Math.max(v.maxX, b.bx1);
         v.maxZ = Math.max(v.maxZ, b.bz1);
      }

      for (Structs.Path p : v.paths) {
         v.minX = Math.min(v.minX, p.x0);
         v.minZ = Math.min(v.minZ, p.z0);
         v.maxX = Math.max(v.maxX, p.x1);
         v.maxZ = Math.max(v.maxZ, p.z1);
      }

      return v;
   }

   private static Street street(boolean alongX, int c, int from, int to, int hw) {
      Street s = new Street();
      s.alongX = alongX;
      s.c = c;
      s.hw = hw;
      if (alongX) {
         s.x0 = from;
         s.x1 = to;
         s.z0 = s.z1 = c;
      } else {
         s.z0 = from;
         s.z1 = to;
         s.x0 = s.x1 = c;
      }

      return s;
   }

   /** How far a street can go from (x, z) in the direction (dx, dz) before the water, a cliff or the sea cuts it off. */
   static int reach(Gen gen, int x, int z, int dx, int dz, int max, int h0) {
      int n = 0;
      int last = h0;
      for (int i = 1; i <= max; i++) {
         if (i % 2 == 0 || i == max) {
            Col c = gen.column(x + dx * i, z + dz * i);
            if (c.h < 64 || Math.abs(c.h - last) > 3 || Biomes.ocean(c.biome) || Biomes.river(c.biome)) {
               break;
            }

            last = c.h;
         }

         n = i;
      }

      return n;
   }

   static boolean flatAround(Gen gen, int x, int z, int h) {
      int bad = 0;
      int[][] ring = {{14, 0}, {-14, 0}, {0, 14}, {0, -14}, {10, 10}, {-10, 10}, {10, -10}, {-10, -10}, {24, 0}, {-24, 0}, {0, 24}, {0, -24}};
      for (int[] p : ring) {
         Col c = gen.column(x + p[0], z + p[1]);
         if (c.h < 64 || Math.abs(c.h - h) > 5 || Biomes.ocean(c.biome) || Biomes.river(c.biome)) {
            bad++;
         }
      }

      return bad <= 2;
   }

   // ------------------------------------------------------------------ plots
   private static String kindFor(Ctx k, String base) {
      if (k.style == Style.DESERT && (base.startsWith("house") || base.equals("hut"))) {
         return base + "_flat";
      }

      return base;
   }

   private static String pickKind(Ctx k, double dist) {
      Random r = k.r;
      if (!k.queue.isEmpty() && dist < 34.0) {
         return k.queue.peek();
      }

      int roll = r.nextInt(20);
      boolean cold = k.style == Style.SPRUCE || k.style == Style.SNOW;
      if (roll < 2) {
         return kindFor(k, "hut");
      } else if (roll < 7) {
         return kindFor(k, "house_s");
      } else if (roll < 12) {
         return kindFor(k, "house_m");
      } else if (roll < 15) {
         return kindFor(k, "house_l");
      } else if (roll < 17) {
         return cold ? "longhouse" : kindFor(k, "house_m");
      } else if (roll < 18 && dist > 20.0) {
         return "barn";
      } else if (roll < 19 && dist > 20.0 && k.pens < 2) {
         return "pen";
      }

      return kindFor(k, "house_s");
   }

   /** Walks along one side of a street and puts buildings along it (as many as fit within a number of blocks of the middle). */
   private static void walkStreet(Ctx k, Street st, int side, int radius, int target) {
      int from = st.alongX ? st.x0 : st.z0;
      int to = st.alongX ? st.x1 : st.z1;
      int mid = st.alongX ? k.cx : k.cz;
      int cursor = from + 2 + k.r.nextInt(3);
      while (cursor <= to - 3 && k.houses < target) {
         double dist = Math.abs(cursor - mid) + Math.abs(st.c - (st.alongX ? k.cz : k.cx)) * 0.7;
         if (dist > radius + 6) {
            cursor += 3;
            continue;
         }

         String kind = pickKind(k, dist);
         int used = tryPlace(k, st, side, cursor, kind);
         if (used == 0 && !kind.equals(kindFor(k, "house_s"))) {
            kind = kindFor(k, "hut");
            used = tryPlace(k, st, side, cursor, kind);
         }

         if (used > 0) {
            if (!k.queue.isEmpty() && kind.equals(k.queue.peek())) {
               k.queue.poll();
            }

            cursor += used + 1 + k.r.nextInt(2);
         } else {
            cursor += 2;
         }
      }
   }

   /** Farms, towers and stalls: the first one that fits further out (farms, towers) or near the middle (stalls). */
   private static void walkSpecial(Ctx k, Street st, int side, String kind, int max) {
      int from = st.alongX ? st.x0 : st.z0;
      int to = st.alongX ? st.x1 : st.z1;
      int mid = st.alongX ? k.cx : k.cz;
      boolean far = !kind.equals("stall");
      int placed = 0;
      for (int cursor = from + 1; cursor <= to - 2 && placed < 2; cursor += 2) {
         double dist = Math.abs(cursor - mid) + Math.abs(st.c - (st.alongX ? k.cz : k.cx)) * 0.7;
         if (far ? dist < 16.0 : dist > 16.0) {
            continue;
         }

         if (kind.equals("farm") && k.farms >= max || kind.equals("stall") && k.stalls >= max || kind.equals("tower") && k.towers >= max) {
            return;
         }

         int used = tryPlace(k, st, side, cursor, kind);
         if (used > 0) {
            placed++;
            cursor += used;
         }
      }
   }

   private static boolean hit(int[] a, int x0, int z0, int x1, int z1, int margin) {
      return x1 + margin >= a[0] && x0 - margin <= a[2] && z1 + margin >= a[1] && z0 - margin <= a[3];
   }

   /** Tries to put a building of this kind at the cursor of one side of a street; returns how many blocks of the street it uses (0 = it did not fit). */
   private static int tryPlace(Ctx k, Street st, int side, int cursor, String kind) {
      Tpl t = Buildings314.get(kind);
      if (t == null) {
         return 0;
      }

      int q;
      if (st.alongX) {
         q = side > 0 ? 0 : 2;
      } else {
         q = side > 0 ? 3 : 1;
      }

      int fw = t.fw(q);
      int fd = t.fd(q);
      int x0;
      int z0;
      if (st.alongX) {
         x0 = cursor;
         z0 = side > 0 ? st.c + st.hw + 1 : st.c - st.hw - fd;
      } else {
         z0 = cursor;
         x0 = side > 0 ? st.c + st.hw + 1 : st.c - st.hw - fw;
      }

      int x1 = x0 + fw - 1;
      int z1 = z0 + fd - 1;
      int len = st.alongX ? fw : fd;
      if ((st.alongX ? x1 : z1) > (st.alongX ? st.x1 : st.z1) + 1) {
         return 0;
      }

      for (int[] o : k.rects) {
         if (hit(o, x0, z0, x1, z1, 1)) {
            return 0;
         }
      }

      for (Structs.Path p : k.v.paths) {
         if (hit(new int[]{p.x0, p.z0, p.x1, p.z1}, x0, z0, x1, z1, 0)) {
            return 0;
         }
      }

      // the ground: nine points, no water, not too steep, level enough to stand on one floor
      int[] hs = new int[9];
      int n = 0;
      int lo = 999;
      int hi = -999;
      int[] px = {x0, (x0 + x1) / 2, x1};
      int[] pz = {z0, (z0 + z1) / 2, z1};
      for (int i = 0; i < 3; i++) {
         for (int j = 0; j < 3; j++) {
            Col c = k.gen.column(px[i], pz[j]);
            if (c.h < 64 || Biomes.ocean(c.biome) || Biomes.river(c.biome)) {
               return 0;
            }

            hs[n++] = c.h;
            lo = Math.min(lo, c.h);
            hi = Math.max(hi, c.h);
         }
      }

      boolean flatOnly = kind.equals("farm") || kind.equals("pen") || kind.equals("stall");
      if (hi - lo > (flatOnly ? 1 : 5)) {
         return 0;
      }

      java.util.Arrays.sort(hs);
      // the doorstep is about as high as the street in front of it (a block more or less); the ground below is filled up, the ground above cut away
      int fx = st.alongX ? (x0 + x1) / 2 : st.c;
      int fz = st.alongX ? st.c : (z0 + z1) / 2;
      Col road = k.gen.column(fx, fz);
      int y0 = Math.max(road.h - 1, Math.min(road.h + 1, hs[4]));
      if (y0 - lo > (flatOnly ? 1 : 5) || hi - y0 > (flatOnly ? 1 : 4)) {
         return 0;
      }

      k.rects.add(new int[]{x0, z0, x1, z1});
      addPlaced(k, t, q, x0, y0, z0, kind);
      if (kind.equals("farm")) {
         k.farms++;
      } else if (kind.equals("stall")) {
         k.stalls++;
      } else if (kind.equals("pen")) {
         k.pens++;
      } else if (kind.equals("tower")) {
         k.towers++;
      } else {
         k.houses++;
      }

      return len;
   }

   private static void addPlaced(Ctx k, Tpl t, int q, int x0, int y0, int z0, String kind) {
      Placed p = new Placed();
      p.tpl = t;
      p.q = q;
      p.x0 = x0;
      p.y0 = y0;
      p.z0 = z0;
      p.fw = t.fw(q);
      p.fd = t.fd(q);
      p.salt = Style.hash(x0, 0, z0, 4711);
      p.st = Style.of(k.style, (p.salt >> 5) & 3);
      k.v.placed.add(p);
      if (kind.equals("well")) {
         k.rects.add(new int[]{x0, z0, x0 + p.fw - 1, z0 + p.fd - 1});
      }

      Structs.Bld b = new Structs.Bld();
      b.type = kind.equals("well") ? 1 : (kind.equals("farm") ? 3 : (kind.equals("church") ? 2 : (kind.equals("smithy") ? 4 : (kind.equals("tower") || kind.equals("stall") || kind.equals("pen") || kind.equals("barn") ? 5 : 0))));
      b.desert = k.style == Style.DESERT;
      b.dir = q == 0 ? 0 : (q == 2 ? 1 : (q == 3 ? 2 : 3));
      b.w = t.w;
      b.d = t.d;
      b.x0 = x0;
      b.z0 = z0;
      b.y0 = y0;
      b.wallH = Math.max(3, Math.min(t.height() - 5, 8));
      b.bx0 = x0;
      b.bz0 = z0;
      b.bx1 = x0 + p.fw - 1;
      b.bz1 = z0 + p.fd - 1;
      b.chest = t.chests > 0;
      k.v.blds.add(b);
      if (!t.doors.isEmpty()) {
         int[] dr = t.doors.get(0);
         int[] in = t.world(dr[0], 0, q, x0, z0);
         k.v.spawn.add(new int[]{in[0], in[1], y0});
      }
   }

   /** A lamp post (three fences and a torch) every nine blocks beside the road, and a place for a villager to start every six. */
   private static void lamps(Ctx k, Street st) {
      int from = st.alongX ? st.x0 : st.z0;
      int to = st.alongX ? st.x1 : st.z1;
      int n = 0;
      for (int a = from + 3; a <= to - 2; a += 9) {
         int side = (n++ & 1) == 0 ? -1 : 1;
         int off = st.c + side * (st.hw + 1);
         int x = st.alongX ? a : off;
         int z = st.alongX ? off : a;
         boolean free = true;
         for (int[] o : k.rects) {
            if (x >= o[0] - 0 && x <= o[2] + 0 && z >= o[1] - 0 && z <= o[3] + 0) {
               free = false;
            }
         }

         if (free) {
            Col c = k.gen.column(x, z);
            if (c.h >= 64) {
               k.v.lamps.add(new int[]{x, z});
            }
         }
      }

      for (int a = from + 2; a <= to - 1; a += 6) {
         int x = st.alongX ? a : st.c;
         int z = st.alongX ? st.c : a;
         Col c = k.gen.column(x, z);
         if (c.h >= 64) {
            k.v.spawn.add(new int[]{x, z, c.h});
         }
      }
   }

   // ------------------------------------------------------------------ into the chunks
   static void apply(Structs s, Chunk c, Structs.Village v) {
      int ground = v.style == Style.DESERT ? Blocks.SAND : Blocks.GRASS;
      Style base = Style.of(v.style, 0);
      // no trees over the streets and between the houses
      for (Street st : v.streets) {
         clearZone(c, (st.alongX ? st.x0 : st.c - st.hw) - 2, (st.alongX ? st.c - st.hw : st.z0) - 2, (st.alongX ? st.x1 : st.c + st.hw) + 2, (st.alongX ? st.c + st.hw : st.z1) + 2);
      }

      for (Placed p : v.placed) {
         clearZone(c, p.x0 - 2, p.z0 - 2, p.x0 + p.fw + 1, p.z0 + p.fd + 1);
      }

      for (Street st : v.streets) {
         int x0 = st.alongX ? st.x0 : st.c - st.hw;
         int x1 = st.alongX ? st.x1 : st.c + st.hw;
         int z0 = st.alongX ? st.c - st.hw : st.z0;
         int z1 = st.alongX ? st.c + st.hw : st.z1;
         if (Structs.overlaps(c, x0, z0, x1, z1)) {
            drawStreet(c, base, x0, z0, x1, z1, v);
         }
      }

      for (Placed p : v.placed) {
         if (Structs.overlaps(c, p.x0 - 1, p.z0 - 1, p.x0 + p.fw, p.z0 + p.fd)) {
            p.tpl.prepare(c, p.st, p.q, p.x0, p.y0, p.z0, ground);
         }
      }

      for (Placed p : v.placed) {
         p.tpl.paste(c, p.st, p.q, p.x0, p.y0, p.z0, p.salt);
         if (p.st.snow) {
            snowCap(c, p);
         }
      }

      for (int[] lamp : v.lamps) {
         if (Structs.overlaps(c, lamp[0], lamp[1], lamp[0], lamp[1])) {
            lamp(c, base, lamp[0], lamp[1]);
         }
      }
   }

   /** Takes the trees, plants and snow off the ground of a rectangle (the part of it that lies in the chunk). */
   static void clearZone(Chunk c, int x0, int z0, int x1, int z1) {
      if (!Structs.overlaps(c, x0, z0, x1, z1)) {
         return;
      }

      for (int z = Math.max(z0, c.cz * 16); z <= Math.min(z1, c.cz * 16 + 15); z++) {
         for (int x = Math.max(x0, c.cx * 16); x <= Math.min(x1, c.cx * 16 + 15); x++) {
            int y = groundY(c, x, z);
            if (y < 0) {
               continue;
            }

            int lx = x - c.cx * 16;
            int lz = z - c.cz * 16;
            for (int h = y + 1; h <= Math.min(250, y + 48); h++) {
               int id = c.blocks[Chunk.idx(lx, h, lz)] & 0xFFFF;
               if (id != 0 && Tpl.vegetation(id)) {
                  c.blocks[Chunk.idx(lx, h, lz)] = 0;
               }
            }
         }
      }
   }

   /** The top ground block of the column in the chunk (plants and leaves do not count), or -1. */
   static int groundY(Chunk c, int x, int z) {
      int lx = x - c.cx * 16;
      int lz = z - c.cz * 16;
      for (int y = 200; y > 40; y--) {
         int id = c.blocks[Chunk.idx(lx, y, lz)] & 0xFFFF;
         if (id != 0 && !Structs.soft(id) && !Tpl.vegetation(id)) {
            return Blocks.isFluid(id) ? -1 : y;
         }
      }

      return -1;
   }

   static void drawStreet(Chunk c, Style st, int x0, int z0, int x1, int z1, Structs.Village v) {
      for (int z = Math.max(z0, c.cz * 16); z <= Math.min(z1, c.cz * 16 + 15); z++) {
         for (int x = Math.max(x0, c.cx * 16); x <= Math.min(x1, c.cx * 16 + 15); x++) {
            int y = groundY(c, x, z);
            if (y < 0) {
               continue;
            }

            int id = Structs.get(c, x, y, z);
            if (id == 2 || id == 3 || id == 7 || id == 8 || id == Blocks313.PODZOL || id == Blocks313.COARSE_DIRT || id == Blocks313.MYCELIUM || id == Blocks.SNOW || id == Blocks313.RED_SAND || id == 17) {
               boolean alt = (Style.hash(x, 0, z, 77) & 7) == 0;
               Structs.set(c, x, y, z, alt ? st.path2 : st.path);
            }

            for (int h = 1; h <= 3; h++) {
               int up = Structs.get(c, x, y + h, z);
               if (up > 0 && (Tpl.vegetation(up) || Blocks.isLeaves(up))) {
                  Structs.set(c, x, y + h, z, 0);
               }
            }
         }
      }
   }

   static void lamp(Chunk c, Style st, int x, int z) {
      int y = groundY(c, x, z);
      if (y < 0) {
         return;
      }

      for (int h = 1; h <= 3; h++) {
         Structs.set(c, x, y + h, z, st.fence);
      }

      Structs.set(c, x, y + 4, z, Blocks.TORCH);
   }

   /** Snow on the flat tops of the roofs (the stairs keep their slope bare). */
   static void snowCap(Chunk c, Placed p) {
      Tpl t = p.tpl;
      for (int z = 0; z < t.d; z++) {
         for (int x = 0; x < t.w; x++) {
            int top = -1;
            for (int y = t.h - 1; y >= 0; y--) {
               if (t.tok[t.idx(x, y, z)] != ' ') {
                  top = y;
                  break;
               }
            }

            if (top < 0) {
               continue;
            }

            char tk = t.tok[t.idx(x, top, z)];
            if (tk == 'R' || tk == '#' || tk == 'P' || tk == 'C' || tk == 'w' || tk == 'W' || tk == 'L' || tk == 'E') {
               int wx = p.x0 + Tpl.rotX(x, z, t.w, t.d, p.q);
               int wz = p.z0 + Tpl.rotZ(x, z, t.w, t.d, p.q);
               int wy = p.y0 - t.sink + top + 1;
               if (Structs.get(c, wx, wy, wz) == 0) {
                  Structs.set(c, wx, wy, wz, Blocks313.SNOW_LAYER);
               }
            }
         }
      }
   }
}
