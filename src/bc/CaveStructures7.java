package bc;

import java.util.concurrent.ConcurrentHashMap;

/**
 * V6.34: underground structures that are part of the cave system itself.
 *
 * <p>In V6.32 every structure was an isolated sealed box plus a "link tunnel"
 * dug out to a cave. Here the structure's rooms and corridors are returned by
 * {@link #air}, which {@link Caves7#carve} calls for every block. Caves and
 * structures therefore share one continuous air volume: a cheese cavern can
 * cut straight through a mineshaft corridor, a dungeon can open into a tunnel,
 * and nothing is ever sealed or artificially connected.</p>
 *
 * <p>{@link #finish} only places floors, walls, supports and loot into the air
 * the carver already made - and only where the neighbouring rock is solid, so
 * a cave that passes through a wall stays open. All plans are deterministic
 * in (seed, region), so chunk seams agree.</p>
 */
final class CaveStructures7 {
   static final int REGION = 112;

   static final String[] NAME = {
      "Mineshaft", "Dungeon", "Stronghold", "Amethyst Geode", "Ancient City", "Ruined Portal"
   };

   static final class Plan {
      boolean valid;
      int x, z, y, type, rot;
      long h;
   }

   final long seed;
   final Caves7 caves;
   private final ConcurrentHashMap<Long, Plan> plans = new ConcurrentHashMap<>();

   /** One cached "which structures are near this column" lookup, reused for every Y. */
   private static final class Near {
      int x = Integer.MIN_VALUE, z = Integer.MIN_VALUE;
      Plan[] list = new Plan[0];
   }
   private final ThreadLocal<Near> NEAR = ThreadLocal.withInitial(Near::new);

   CaveStructures7(long seed, Caves7 caves) {
      this.seed = seed;
      this.caves = caves;
   }

   static Plan plan(long seed, int rx, int rz) {
      long h = CaveWorks.mix(seed ^ 0x535452553734L, rx, 0, rz);
      Plan p = new Plan();
      p.h = h;
      if ((h >>> 58) >= 22) return p;   // ~34% of regions have a structure
      p.valid = true;
      p.x = rx * REGION + 16 + CaveWorks.bits(h, 8, 80);
      p.z = rz * REGION + 16 + CaveWorks.bits(h, 20, 80);
      p.type = CaveWorks.bits(h, 30, NAME.length);
      p.rot = CaveWorks.bits(h, 34, 2);
      int band = CaveWorks.bits(h, 36, 3);
      p.y = band == 0 ? 15 + CaveWorks.bits(h, 40, 10)
         : (band == 1 ? 26 + CaveWorks.bits(h, 40, 12) : 38 + CaveWorks.bits(h, 40, 12));
      return p;
   }

   private Plan cached(int rx, int rz) {
      long key = ((long)rx << 32) ^ (rz & 0xFFFFFFFFL);
      return plans.computeIfAbsent(key, k -> {
         Plan p = plan(seed, rx, rz);
         if (p.valid && caves.gen != null) {
            // Stay well below the real terrain; structures never float.
            p.y = Math.max(12, Math.min(p.y, caves.gen.column(p.x, p.z).h - 14));
         }
         return p;
      });
   }

   /** Deterministic list of structures whose footprint can touch this column. */
   Plan[] nearby(int x, int z) {
      Near n = NEAR.get();
      if (n.x == x && n.z == z) return n.list;
      int rx = Math.floorDiv(x, REGION), rz = Math.floorDiv(z, REGION);
      Plan[] tmp = new Plan[9];
      int count = 0;
      for (int i = -1; i <= 1; i++) {
         for (int j = -1; j <= 1; j++) {
            Plan p = cached(rx + i, rz + j);
            if (p.valid && Math.abs(x - p.x) <= 24 && Math.abs(z - p.z) <= 24) tmp[count++] = p;
         }
      }
      Plan[] out = java.util.Arrays.copyOf(tmp, count);
      n.x = x; n.z = z; n.list = out;
      return out;
   }

   /** True when this block belongs to a structure's carved space. */
   boolean air(int x, int y, int z, int surfaceH) {
      if (y > surfaceH - 6) return false;
      Plan[] near = nearby(x, z);
      for (Plan p : near) {
         if (inShape(p, x, y, z)) return true;
      }
      return false;
   }

   private static boolean room(int dx, int dz, int cx, int cz, int r) {
      return Math.abs(dx - cx) <= r && Math.abs(dz - cz) <= r;
   }

   private static boolean inShape(Plan p, int x, int y, int z) {
      int dx = x - p.x, dz = z - p.z, dy = y - p.y;
      switch (p.type) {
         case 0: // Mineshaft: two crossing corridors plus two branches
            return dy >= 0 && dy <= 2 && (
               (Math.abs(dz) <= 1 && Math.abs(dx) <= 20)
               || (Math.abs(dx) <= 1 && Math.abs(dz) <= 20)
               || (Math.abs(dz - 8) <= 1 && Math.abs(dx) <= 12)
               || (Math.abs(dx + 8) <= 1 && Math.abs(dz) <= 12));
         case 1: // Dungeon: small closed room
            return Math.abs(dx) <= 3 && Math.abs(dz) <= 3 && dy >= 0 && dy <= 3;
         case 2: // Stronghold: three rooms and a connecting corridor
            return dy >= 0 && dy <= 4 && (
               (Math.abs(dz) <= 1 && Math.abs(dx) <= 15)
               || room(dx, dz, -10, 0, 4) || room(dx, dz, 0, 0, 5) || room(dx, dz, 10, 0, 4));
         case 3: // Amethyst geode: hollow interior of a sphere
            return dx * dx + dy * dy + dz * dz <= 9.0;
         case 4: // Ancient city: open plaza
            return Math.abs(dx) <= 6 && Math.abs(dz) <= 6 && dy >= 0 && dy <= 3;
         default: // Ruined portal: small open area
            return Math.abs(dx) <= 3 && Math.abs(dz) <= 3 && dy >= 0 && dy <= 4;
      }
   }

   private static boolean inside(int x, int y, int z) {
      return x >= 0 && x < 16 && z >= 0 && z < 16 && y >= 6 && y < 250;
   }

   private static int get(Chunk ch, int x, int y, int z) {
      return inside(x, y, z) ? ch.get(x, y, z) & BlockState.ID_MASK : Blocks.STONE;
   }

   /** Fill a carved (air) cell, leaving any cave that already cut through open. */
   private static void fill(Chunk ch, int x, int y, int z, int id) {
      if (inside(x, y, z) && ch.get(x, y, z) == 0) ch.set(x, y, z, id);
   }

   /** Replace solid rock, but never a cave that passes through. */
   private static void replace(Chunk ch, int x, int y, int z, int id) {
      if (inside(x, y, z) && ch.get(x, y, z) != 0 && ch.get(x, y, z) != 6) ch.set(x, y, z, id);
   }

   private static long hash(long seed, int x, int y, int z) {
      return CaveWorks.mix(seed ^ 0x44455441494C3734L, x, y, z);
   }

   /** Place floors, walls, supports and loot for every structure touching this chunk. */
   void finish(Chunk ch) {
      int x0 = ch.cx * 16, z0 = ch.cz * 16;
      for (int rz = Math.floorDiv(z0 - 24, REGION); rz <= Math.floorDiv(z0 + 39, REGION); rz++) {
         for (int rx = Math.floorDiv(x0 - 24, REGION); rx <= Math.floorDiv(x0 + 39, REGION); rx++) {
            Plan p = cached(rx, rz);
            if (p.valid) stamp(ch, p, x0, z0);
         }
      }
   }

   private void stamp(Chunk ch, Plan p, int x0, int z0) {
      int xa = Math.max(0, p.x - 22 - x0), xb = Math.min(15, p.x + 22 - x0);
      int za = Math.max(0, p.z - 22 - z0), zb = Math.min(15, p.z + 22 - z0);
      if (xa > xb || za > zb) return;
      for (int z = za; z <= zb; z++) {
         for (int x = xa; x <= xb; x++) {
            int wx = x0 + x, wz = z0 + z;
            int ground = caves.gen == null ? 255 : caves.gen.column(wx, wz).h;
            int dx = wx - p.x, dz = wz - p.z;
            switch (p.type) {
               case 0: mineshaft(ch, p, x, z, dx, dz, ground); break;
               case 1: dungeon(ch, p, x, z, dx, dz, ground); break;
               case 2: stronghold(ch, p, x, z, dx, dz, ground); break;
               case 3: geode(ch, p, x, z, dx, dz, ground); break;
               case 4: ancientCity(ch, p, x, z, dx, dz, ground); break;
               default: ruinedPortal(ch, p, x, z, dx, dz, ground); break;
            }
         }
      }
   }

   private static void mineshaft(Chunk ch, Plan p, int x, int z, int dx, int dz, int ground) {
      boolean corridor = (Math.abs(dz) <= 1 && Math.abs(dx) <= 20)
         || (Math.abs(dx) <= 1 && Math.abs(dz) <= 20)
         || (Math.abs(dz - 8) <= 1 && Math.abs(dx) <= 12)
         || (Math.abs(dx + 8) <= 1 && Math.abs(dz) <= 12);
      if (!corridor || p.y >= ground - 5) return;
      int fy = p.y - 1;
      if (fy < 6) return;
      if (Math.abs(dz) == 0 || Math.abs(dx) == 0) {
         replace(ch, x, fy, z, (dx + dz & 1) == 0 ? Blocks.IRON_BLOCK : Blocks.PLANKS);
      } else {
         replace(ch, x, fy, z, Blocks.PLANKS);
      }
      if (((dx + dz) & 3) == 0 && Math.abs(dz) == 1) {
         for (int dy = 0; dy <= 2; dy++) fill(ch, x, p.y + dy, z, Blocks.LOG_OAK);
         fill(ch, x, p.y + 2, z, Blocks.TORCH);
      }
      if (dx == 0 && dz == 0) {
         fill(ch, x, p.y, z, Blocks.CHEST_LOOT);
      }
   }

   private static void dungeon(Chunk ch, Plan p, int x, int z, int dx, int dz, int ground) {
      if (Math.abs(dx) > 3 || Math.abs(dz) > 3 || p.y >= ground - 5) return;
      boolean wall = Math.abs(dx) == 3 || Math.abs(dz) == 3;
      int fy = p.y - 1;
      if (fy >= 6) {
         long h = hash(p.h, dx, 0, dz);
         replace(ch, x, fy, z, CaveWorks.bits(h, 4, 4) == 0 ? Blocks.MOSSY_COBBLE : Blocks.COBBLE);
      }
      if (p.y + 4 < ground - 3) replace(ch, x, p.y + 4, z, Blocks.COBBLE);
      if (wall) {
         // only build the wall where solid rock is outside; a cave keeps its opening
         int ox = Math.abs(dx) == 3 ? x + (dx > 0 ? 1 : -1) : x;
         int oz = Math.abs(dz) == 3 ? z + (dz > 0 ? 1 : -1) : z;
         if (get(ch, ox, p.y + 1, oz) != 0) {
            for (int dy = 0; dy <= 3; dy++) {
               long h = hash(p.h, dx, dy, dz);
               fill(ch, x, p.y + dy, z, CaveWorks.bits(h, 4, 5) == 0 ? Blocks.MOSSY_COBBLE : Blocks.COBBLE);
            }
         }
      }
      if (dx == 0 && dz == 0) fill(ch, x, p.y, z, Blocks.SPAWNER);
      if (dx == 2 && dz == 0) fill(ch, x, p.y, z, Blocks.CHEST_LOOT);
      if (dx == -2 && dz == 0) fill(ch, x, p.y, z, Blocks.CHEST_LOOT);
   }

   private static void stronghold(Chunk ch, Plan p, int x, int z, int dx, int dz, int ground) {
      if (p.y >= ground - 5) return;
      boolean inRoom = room(dx, dz, -10, 0, 4) || room(dx, dz, 0, 0, 5) || room(dx, dz, 10, 0, 4);
      boolean corridor = Math.abs(dz) <= 1 && Math.abs(dx) <= 15;
      if (!inRoom && !corridor) return;
      int fy = p.y - 1;
      if (fy >= 6) replace(ch, x, fy, z, Blocks.STONEBRICK);
      if (p.y + 5 < ground - 3) replace(ch, x, p.y + 5, z, Blocks.STONEBRICK);
      boolean wall = (!corridor && (Math.abs(dx) == 5 || Math.abs(dz) == 5 || Math.abs(dx + 10) == 4 || Math.abs(dx - 10) == 4))
         || (corridor && Math.abs(dz) == 1);
      if (wall) {
         int oz = Math.abs(dz) >= 1 ? z + (dz > 0 ? 1 : -1) : z;
         if (get(ch, x, p.y + 2, oz) != 0) {
            for (int dy = 0; dy <= 4; dy++) {
               long h = hash(p.h, dx, dy, dz);
               int id = CaveWorks.bits(h, 4, 8);
               fill(ch, x, p.y + dy, z, id == 0 ? Redstone319.CHISELED_STONEBRICK
                  : (id == 1 ? Redstone319.MOSSY_STONEBRICK : Blocks.STONEBRICK));
            }
         }
      }
      if (dz == -4 && Math.abs(dx) <= 3) {
         fill(ch, x, p.y + 1, z, Blocks.BOOKSHELF);
         fill(ch, x, p.y + 2, z, Blocks.BOOKSHELF);
      }
      if (dx == 3 && dz >= -1 && dz <= 1) {
         for (int dy = 0; dy <= 4; dy++) fill(ch, x, p.y + dy, z, Blocks.OBSIDIAN);
      }
      if (dx == 3 && dz == 0) fill(ch, x, p.y + 2, z, Blocks.GLOWSTONE);
      if (dx == -3 && dz == 0) fill(ch, x, p.y, z, Blocks.CHEST_LOOT);
   }

   private static void geode(Chunk ch, Plan p, int x, int z, int dx, int dz, int ground) {
      for (int dy = -6; dy <= 6; dy++) {
         int y = p.y + dy;
         if (y < 6 || y >= 250 || y >= ground - 3) continue;
         double d2 = dx * dx + dy * dy + dz * dz;
         if (d2 > 30.0) continue;
         double d = Math.sqrt(d2);
         if (d > 4.2) replace(ch, x, y, z, Blocks.OBSIDIAN);
         else if (d > 3.5) replace(ch, x, y, z, Redstone319.PURPUR_BLOCK);
         else if (d > 2.9) replace(ch, x, y, z, Blocks.GLASS0 + 10);
         else if (dy < -1 && CaveWorks.bits(hash(p.h, dx, dy, dz), 4, 12) == 0) {
            fill(ch, x, y, z, Blocks.GLASS0 + 10);
         }
      }
   }

   private static void ancientCity(Chunk ch, Plan p, int x, int z, int dx, int dz, int ground) {
      if (Math.abs(dx) > 6 || Math.abs(dz) > 6 || p.y >= ground - 5) return;
      int fy = p.y - 1;
      if (fy >= 6) replace(ch, x, fy, z, Redstone319.DARK_PRISMARINE);
      if (Math.abs(dx) == 6 || Math.abs(dz) == 6) {
         int ox = Math.abs(dx) == 6 ? x + (dx > 0 ? 1 : -1) : x;
         int oz = Math.abs(dz) == 6 ? z + (dz > 0 ? 1 : -1) : z;
         if (get(ch, ox, p.y, oz) != 0) {
            fill(ch, x, p.y, z, Redstone319.DARK_PRISMARINE);
            fill(ch, x, p.y + 1, z, Redstone319.DARK_PRISMARINE);
         }
      }
      if (dx == 0 && dz == 0) fill(ch, x, p.y, z, Redstone319.SEA_LANTERN);
      if (dx == 3 && dz == 0) fill(ch, x, p.y, z, Blocks.CHEST_LOOT);
   }

   private static void ruinedPortal(Chunk ch, Plan p, int x, int z, int dx, int dz, int ground) {
      if (Math.abs(dx) > 3 || Math.abs(dz) > 3 || p.y >= ground - 5) return;
      int fy = p.y - 1;
      if (fy >= 6) {
         long h = hash(p.h, dx, 0, dz);
         replace(ch, x, fy, z, CaveWorks.bits(h, 4, 3) == 0
            ? Redstone319.CRACKED_STONEBRICK : Blocks.STONEBRICK);
      }
      if (dz == -2 && (dx == -2 || dx == 2)) {
         for (int dy = 0; dy <= 4; dy++) fill(ch, x, p.y + dy, z, Blocks.OBSIDIAN);
      }
      if (dz == -2 && Math.abs(dx) <= 1 && (p.y + 4 < ground - 3)) fill(ch, x, p.y + 4, z, Blocks.OBSIDIAN);
      if (dx == 0 && dz == -2) fill(ch, x, p.y + 2, z, Blocks.GLOWSTONE);
      if (dx == 3 && dz == 2) fill(ch, x, p.y, z, Blocks.CHEST_LOOT);
   }
}
