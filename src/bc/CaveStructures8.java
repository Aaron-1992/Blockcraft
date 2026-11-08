package bc;

import java.util.concurrent.ConcurrentHashMap;

/**
 * V6.35: twelve underground structures, all carved into the SAME air volume as
 * the caves, with a density and size gradient that follows the depth. Original
 * work - no Mojang code or assets.
 *
 * <p>{@link #air} is called by {@link Caves8#cause} for every block of every
 * column, so a mega cavern can swallow half a cathedral and a maze tunnel can
 * open into a mineshaft. There are no link tunnels and no sealed boxes. Walls
 * and floors are stamped only where the neighbouring rock is solid, so a cave
 * that passes through a wall stays open.</p>
 */
final class CaveStructures8 {
   static final int REGION = 112;

   static final String[] NAME = {
      "Mineshaft", "Dungeon", "Stronghold", "Amethyst Geode", "Ancient City", "Ruined Portal",
      "Crystal Cathedral", "Fossil Hall", "Lava Forge", "Lost Library", "Abyssal Maze", "Sunken Vault"
   };

   static final class Plan {
      boolean valid;
      int x, z, y, type, rot, r;
      long h;
   }

   final long seed;
   final Caves8 caves;
   private final ConcurrentHashMap<Long, Plan> plans = new ConcurrentHashMap<>();

   private static final class Near {
      int x = Integer.MIN_VALUE, z = Integer.MIN_VALUE;
      Plan[] list = new Plan[0];
   }
   private final ThreadLocal<Near> NEAR = ThreadLocal.withInitial(Near::new);

   CaveStructures8(long seed, Caves8 caves) {
      this.seed = seed;
      this.caves = caves;
   }

   /** slot 0 is a normal site; slot 1 is an extra site that only exists deep down. */
   static Plan plan(long seed, int rx, int rz, int slot) {
      long h = CaveWorks.mix(seed ^ 0x535452553835L ^ (long)slot * 0x9E3779B9L, rx, 0, rz);
      Plan p = new Plan();
      p.h = h;
      int gate = slot == 0 ? 24 : 15;
      if ((h >>> 58) >= gate) return p;
      p.valid = true;
      p.x = rx * REGION + 12 + CaveWorks.bits(h, 8, 88);
      p.z = rz * REGION + 12 + CaveWorks.bits(h, 20, 88);
      int band = CaveWorks.bits(h, 36, 4);
      p.y = slot == 0
         ? (band == 0 ? 12 + CaveWorks.bits(h, 40, 14)
            : (band == 1 ? 30 + CaveWorks.bits(h, 40, 18)
            : (band == 2 ? 52 + CaveWorks.bits(h, 40, 22) : 80 + CaveWorks.bits(h, 40, 26))))
         : 10 + CaveWorks.bits(h, 40, 34);   // the extra abyss site
      int t = CaveWorks.bits(h, 30, NAME.length);
      if (p.y > 145) t %= 6;                 // daylight layer keeps the classic six
      else if (p.y > 85) t %= 10;
      p.type = t;
      p.rot = CaveWorks.bits(h, 34, 2);
      // deeper structures are physically larger
      double d = Caves8.crazy(p.y, 191);
      p.r = 2 + (int)(4.0 * d);
      return p;
   }

   private Plan cached(int rx, int rz, int slot) {
      long key = ((long)rx << 35) ^ ((long)rz << 3) ^ slot;
      return plans.computeIfAbsent(key, k -> {
         Plan p = plan(seed, rx, rz, slot);
         if (p.valid && caves.gen != null) {
            p.y = Math.max(10, Math.min(p.y, caves.gen.column(p.x, p.z).h - 14));
            double d = Caves8.crazy(p.y, caves.sea());
            p.r = 2 + (int)(4.0 * d);
         }
         return p;
      });
   }

   Plan[] nearby(int x, int z) {
      Near n = NEAR.get();
      if (n.x == x && n.z == z) return n.list;
      int rx = Math.floorDiv(x, REGION), rz = Math.floorDiv(z, REGION);
      Plan[] tmp = new Plan[18];
      int count = 0;
      for (int i = -1; i <= 1; i++) {
         for (int j = -1; j <= 1; j++) {
            for (int slot = 0; slot < 2; slot++) {
               Plan p = cached(rx + i, rz + j, slot);
               if (p.valid && Math.abs(x - p.x) <= 26 + p.r && Math.abs(z - p.z) <= 26 + p.r) tmp[count++] = p;
            }
         }
      }
      Plan[] out = java.util.Arrays.copyOf(tmp, count);
      n.x = x; n.z = z; n.list = out;
      return out;
   }

   boolean air(int x, int y, int z, int surfaceH) {
      if (y > surfaceH - 6) return false;
      for (Plan p : nearby(x, z)) {
         if (inShape(p, x - p.x, y - p.y, z - p.z)) return true;
      }
      return false;
   }

   private static boolean room(int dx, int dz, int cx, int cz, int r) {
      return Math.abs(dx - cx) <= r && Math.abs(dz - cz) <= r;
   }

   /** The carved space of a structure. Everything here is cave air too. */
   private static boolean inShape(Plan p, int dx, int dy, int dz) {
      int r = p.r;
      switch (p.type) {
         case 0: // Mineshaft: two long corridors plus branches
            return dy >= 0 && dy <= 2 && (
               (Math.abs(dz) <= 1 && Math.abs(dx) <= 18 + r)
               || (Math.abs(dx) <= 1 && Math.abs(dz) <= 18 + r)
               || (Math.abs(dz - 8) <= 1 && Math.abs(dx) <= 10 + r)
               || (Math.abs(dx + 8) <= 1 && Math.abs(dz) <= 10 + r));
         case 1: // Dungeon
            return Math.abs(dx) <= 3 && Math.abs(dz) <= 3 && dy >= 0 && dy <= 3;
         case 2: // Stronghold: three rooms and a corridor
            return dy >= 0 && dy <= 4 && (
               (Math.abs(dz) <= 1 && Math.abs(dx) <= 14 + r)
               || room(dx, dz, -10, 0, 4) || room(dx, dz, 0, 0, 5) || room(dx, dz, 10, 0, 4));
         case 3: // Amethyst geode: hollow sphere
            return dx * dx + dy * dy + dz * dz <= 9.0 + r;
         case 4: // Ancient city plaza
            return Math.abs(dx) <= 6 + r && Math.abs(dz) <= 6 + r && dy >= 0 && dy <= 3;
         case 5: // Ruined portal
            return Math.abs(dx) <= 3 && Math.abs(dz) <= 3 && dy >= 0 && dy <= 4;
         case 6: // Crystal cathedral: a tall open dome
            return dy >= 0 && dy <= 8 + r && (dx * dx + dz * dz <= (7 + r) * (7 + r) - dy * dy / 2);
         case 7: // Fossil hall: long ribbed chamber
            return dy >= 0 && dy <= 6 + r && Math.abs(dx) <= 12 + r && Math.abs(dz) <= 4 + r;
         case 8: // Lava forge: a ring of air around a solid core
            return dy >= 0 && dy <= 4 && (dx * dx + dz * dz <= (8 + r) * (8 + r))
               && (dx * dx + dz * dz >= (2 + r / 2) * (2 + r / 2));
         case 9: // Lost library: two rooms and a corridor
            return dy >= 0 && dy <= 4 && (
               room(dx, dz, -7, 0, 5 + r / 2) || room(dx, dz, 7, 0, 5 + r / 2)
               || (Math.abs(dz) <= 1 && Math.abs(dx) <= 9 + r));
         case 10: // Abyssal maze: dense narrow crossing corridors
            return dy >= 0 && dy <= 3 && (
               (Math.abs(dz % 7) <= 1 && Math.abs(dx) <= 16 + r)
               || (Math.abs(dx % 7) <= 1 && Math.abs(dz) <= 16 + r));
         default: // Sunken vault: a broad flooded hall
            return dy >= 0 && dy <= 5 && Math.abs(dx) <= 8 + r && Math.abs(dz) <= 8 + r;
      }
   }

   private static boolean inside(int x, int y, int z) {
      return x >= 0 && x < 16 && z >= 0 && z < 16 && y >= 6 && y < Chunk.H;
   }

   private static int get(Chunk ch, int x, int y, int z) {
      return inside(x, y, z) ? ch.get(x, y, z) & BlockState.ID_MASK : Blocks.STONE;
   }

   private static void fill(Chunk ch, int x, int y, int z, int id) {
      if (inside(x, y, z) && ch.get(x, y, z) == 0) ch.set(x, y, z, id);
   }

   private static void replace(Chunk ch, int x, int y, int z, int id) {
      if (inside(x, y, z) && ch.get(x, y, z) != 0 && ch.get(x, y, z) != 6) ch.set(x, y, z, id);
   }

   private static long hash(long seed, int x, int y, int z) {
      return CaveWorks.mix(seed ^ 0x44455441494C3835L, x, y, z);
   }

   /** Palette: {floor, wall, accent, light, loot-ish feature}. */
   private static int[] palette(int type, long h, int dx, int dy, int dz) {
      int q = CaveWorks.bits(hash(h, dx, dy, dz), 4, 8);
      switch (type) {
         case 0: return new int[]{Blocks.PLANKS, Blocks.LOG_OAK, Blocks.IRON_BLOCK, Blocks.TORCH, Blocks.CHEST_LOOT};
         case 1: return new int[]{q == 0 ? Blocks.MOSSY_COBBLE : Blocks.COBBLE,
            Blocks.COBBLE, Blocks.MOSSY_COBBLE, Blocks.GLOWSTONE, Blocks.SPAWNER};
         case 2: return new int[]{Blocks.STONEBRICK, Blocks.STONEBRICK,
            q == 0 ? Redstone319.CHISELED_STONEBRICK : Redstone319.MOSSY_STONEBRICK,
            Blocks.GLOWSTONE, Blocks.BOOKSHELF};
         case 3: return new int[]{Blocks.OBSIDIAN, Redstone319.PURPUR_BLOCK,
            Blocks.GLASS0 + 10, Blocks.GLOWSTONE, Blocks.GLASS0 + 10};
         case 4: return new int[]{Redstone319.DARK_PRISMARINE, Redstone319.DARK_PRISMARINE,
            Blocks.STONEBRICK, Redstone319.SEA_LANTERN, Blocks.CHEST_LOOT};
         case 5: return new int[]{Redstone319.CRACKED_STONEBRICK, Blocks.OBSIDIAN,
            Blocks.STONEBRICK, Blocks.GLOWSTONE, Blocks.CHEST_LOOT};
         case 6: return new int[]{Redstone319.PURPUR_BLOCK, Blocks.GLASS0 + 10,
            Blocks.GLASS0 + 10, Blocks.GLOWSTONE, Blocks.CHEST_LOOT};
         case 7: return new int[]{Blocks.STONEBRICK, Blocks.QUARTZ_BLOCK,
            Blocks313.ANDESITE, Blocks.GLOWSTONE, Blocks.CHEST_LOOT};
         case 8: return new int[]{Blocks.OBSIDIAN, Blocks.OBSIDIAN,
            Blocks313.ANDESITE, Blocks.LAVA, Blocks.CHEST_LOOT};
         case 9: return new int[]{Blocks.STONEBRICK, Blocks.BOOKSHELF,
            Redstone319.CHISELED_STONEBRICK, Blocks.GLOWSTONE, Blocks.CHEST_LOOT};
         case 10: return new int[]{Blocks.COBBLE, Blocks.MOSSY_COBBLE,
            Blocks.COBBLE, Blocks.GLOWSTONE, Blocks.CHEST_LOOT};
         default: return new int[]{Redstone319.PURPUR_BLOCK, Redstone319.DARK_PRISMARINE,
            Blocks.STONEBRICK, Redstone319.SEA_LANTERN, Blocks.CHEST_LOOT};
      }
   }

   void finish(Chunk ch) {
      int x0 = ch.cx * 16, z0 = ch.cz * 16;
      for (int rz = Math.floorDiv(z0 - 30, REGION); rz <= Math.floorDiv(z0 + 45, REGION); rz++) {
         for (int rx = Math.floorDiv(x0 - 30, REGION); rx <= Math.floorDiv(x0 + 45, REGION); rx++) {
            for (int slot = 0; slot < 2; slot++) {
               Plan p = cached(rx, rz, slot);
               if (p.valid) stamp(ch, p, x0, z0);
            }
         }
      }
   }

   private void stamp(Chunk ch, Plan p, int x0, int z0) {
      int reach = 24 + p.r;
      int xa = Math.max(0, p.x - reach - x0), xb = Math.min(15, p.x + reach - x0);
      int za = Math.max(0, p.z - reach - z0), zb = Math.min(15, p.z + reach - z0);
      if (xa > xb || za > zb) return;
      for (int z = za; z <= zb; z++) {
         for (int x = xa; x <= xb; x++) {
            int wx = x0 + x, wz = z0 + z;
            int ground = caves.gen == null ? Chunk.H - 1 : caves.gen.column(wx, wz).h;
            int dx = wx - p.x, dz = wz - p.z;
            int fy = p.y - 1;
            if (fy < 8 || p.y >= ground - 5) continue;
            if (!inShape(p, dx, 0, dz)) continue;
            int[] pal = palette(p.type, p.h, dx, 0, dz);
            // Floor: replace rock, but never a cave that already cut through.
            replace(ch, x, fy, z, pal[0]);
            // Walls: only where the rock just outside is solid.
            boolean edge = !inShape(p, dx + 1, 0, dz) || !inShape(p, dx - 1, 0, dz)
               || !inShape(p, dx, 0, dz + 1) || !inShape(p, dx, 0, dz - 1);
            if (edge) {
               int ox = !inShape(p, dx + 1, 0, dz) ? x + 1 : (!inShape(p, dx - 1, 0, dz) ? x - 1 : x);
               int oz = !inShape(p, dx, 0, dz + 1) ? z + 1 : (!inShape(p, dx, 0, dz - 1) ? z - 1 : z);
               if (get(ch, ox, p.y + 1, oz) != 0) {
                  int height = p.type == 6 || p.type == 7 ? 6 : 3;
                  for (int dy = 0; dy <= height; dy++) {
                     fill(ch, x, p.y + dy, z, pal[1]);
                  }
               }
            }
            // Accents, lights and loot on a sparse hashed lattice.
            long h = hash(p.h, dx, 0, dz);
            int roll = CaveWorks.bits(h, 12, 100);
            if (roll < 6 && ((dx + dz) & 3) == 0) {
               fill(ch, x, p.y, z, pal[2]);
            } else if (roll < 9 && ((dx + dz) & 7) == 0) {
               fill(ch, x, p.y + 1, z, pal[3]);
            } else if (dx == 0 && dz == 0) {
               fill(ch, x, p.y, z, pal[4]);
            }
         }
      }
   }
}
