package bc;

import java.util.concurrent.ConcurrentHashMap;

/**
 * V6.32: underground structures rebuilt in the shape and scale of Minecraft
 * 1.18/1.20, written from scratch with this game's own blocks (no Mojang
 * code or assets).
 *
 * <p>V6.31 built huge rotundas with side chambers and 20-block corridors, which
 * hollowed the world further. These are small and recognisable instead:</p>
 * <ul>
 *   <li><b>Mineshaft</b> - 3-wide plank-floored corridors with oak supports,
 *       torches, iron track lines and a loot chest;</li>
 *   <li><b>Dungeon</b> - a small cobble/mossy room with a spawner and chests;</li>
 *   <li><b>Stronghold</b> - stone-brick rooms, a bookshelf library and an
 *       obsidian portal frame;</li>
 *   <li><b>Amethyst geode</b> - a spherical obsidian/purpur shell with a hollow
 *       crystal interior;</li>
 *   <li><b>Ancient city</b> - a dark-prismarine floor with sea-lantern light;</li>
 *   <li><b>Ruined portal</b> - an obsidian frame with cracked brick rubble.</li>
 * </ul>
 * <p>Each site is small, deterministic, clipped to the chunk in world
 * coordinates (so seams agree), and gets a short tunnel to a real carved cave.</p>
 */
final class CaveStructures6 {
   static final int REGION = 128;

   static final String[] NAME = {
      "Mineshaft", "Dungeon", "Stronghold", "Amethyst Geode", "Ancient City", "Ruined Portal"
   };

   static final class Plan {
      boolean valid;
      int x, z, y, type, rot;
      long h;
   }

   final long seed;
   final Gen gen;
   final Caves6 caves;
   private final ConcurrentHashMap<Long, Plan> plans = new ConcurrentHashMap<>();

   CaveStructures6(long seed, Gen gen, Caves6 caves) {
      this.seed = seed;
      this.gen = gen;
      this.caves = caves;
   }

   static Plan plan(long seed, int rx, int rz) {
      long h = CaveWorks.mix(seed ^ 0x535452553632L, rx, 0, rz);
      Plan p = new Plan();
      p.h = h;
      if ((h >>> 58) >= 24) return p;   // ~38% of cells get a structure
      p.valid = true;
      p.x = rx * REGION + 16 + CaveWorks.bits(h, 8, 96);
      p.z = rz * REGION + 16 + CaveWorks.bits(h, 20, 96);
      p.type = CaveWorks.bits(h, 30, NAME.length);
      p.rot = CaveWorks.bits(h, 34, 2);
      int band = CaveWorks.bits(h, 36, 3);
      p.y = band == 0 ? 14 + CaveWorks.bits(h, 40, 10)
         : (band == 1 ? 26 + CaveWorks.bits(h, 40, 12) : 40 + CaveWorks.bits(h, 40, 12));
      return p;
   }

   private Plan cached(int rx, int rz) {
      long key = ((long)rx << 32) ^ (rz & 0xFFFFFFFFL);
      return plans.computeIfAbsent(key, k -> {
         Plan p = plan(seed, rx, rz);
         if (p.valid && gen != null) {
            // Never float in an ocean or break the surface: stay well under it.
            p.y = Math.max(12, Math.min(p.y, gen.column(p.x, p.z).h - 18));
         }
         return p;
      });
   }

   void apply(Chunk ch) {
      int margin = 44;
      int x0 = ch.cx * 16, z0 = ch.cz * 16;
      for (int rx = Math.floorDiv(x0 - margin, REGION); rx <= Math.floorDiv(x0 + 15 + margin, REGION); rx++) {
         for (int rz = Math.floorDiv(z0 - margin, REGION); rz <= Math.floorDiv(z0 + 15 + margin, REGION); rz++) {
            Plan p = cached(rx, rz);
            if (p.valid) stamp(ch, p);
         }
      }
   }

   private static boolean inside(int x, int y, int z) {
      return x >= 0 && x < 16 && z >= 0 && z < 16 && y >= 6 && y < 250;
   }

   private static void put(Chunk ch, int x, int y, int z, int id) {
      if (inside(x, y, z)) ch.set(x, y, z, id);
   }

   private static int get(Chunk ch, int x, int y, int z) {
      return inside(x, y, z) ? ch.get(x, y, z) & BlockState.ID_MASK : Blocks.STONE;
   }

   private static void carve(Chunk ch, int x, int y, int z) {
      if (!inside(x, y, z)) return;
      int id = ch.get(x, y, z) & BlockState.ID_MASK;
      if (id == 6 || Blocks.isFluid(id)) return;
      // A link tunnel and neighbouring rooms must never erase the loot.
      if (id == Blocks.CHEST_LOOT || id == Blocks.SPAWNER || id == Blocks.GLOWSTONE) return;
      ch.set(x, y, z, 0);
   }

   void stamp(Chunk ch, Plan p) {
      int x0 = ch.cx * 16, z0 = ch.cz * 16;
      int reach = 18;
      int xa = Math.max(0, p.x - reach - x0), xb = Math.min(15, p.x + reach - x0);
      int za = Math.max(0, p.z - reach - z0), zb = Math.min(15, p.z + reach - z0);
      if (xa > xb || za > zb) return;
      switch (p.type) {
         case 0: mineshaft(ch, p, x0, z0, xa, xb, za, zb); break;
         case 1: dungeon(ch, p, x0, z0, xa, xb, za, zb); break;
         case 2: stronghold(ch, p, x0, z0, xa, xb, za, zb); break;
         case 3: geode(ch, p, x0, z0, xa, xb, za, zb); break;
         case 4: ancientCity(ch, p, x0, z0, xa, xb, za, zb); break;
         default: ruinedPortal(ch, p, x0, z0, xa, xb, za, zb); break;
      }
      linkToCave(ch, p, x0, z0);
   }

   private static long hash(long seed, int x, int y, int z) {
      return CaveWorks.mix(seed ^ 0x44455441494C36L, x, y, z);
   }

   // 1. Mineshaft: thin 3-wide corridors, plank floor, oak supports, torches, rails.
   private static void mineshaft(Chunk ch, Plan p, int x0, int z0, int xa, int xb, int za, int zb) {
      for (int z = za; z <= zb; z++) {
         for (int x = xa; x <= xb; x++) {
            int wx = x0 + x, wz = z0 + z;
            int dx = wx - p.x, dz = wz - p.z;
            boolean alongX = Math.abs(dz) <= 1 && Math.abs(dx) <= 14;
            boolean alongZ = Math.abs(dx) <= 1 && Math.abs(dz) <= 14;
            if (!alongX && !alongZ) continue;
            for (int y = p.y; y <= p.y + 2; y++) carve(ch, x, y, z);
            put(ch, x, p.y - 1, z, Blocks.PLANKS);
            if ((dx + dz & 1) == 0) put(ch, x, p.y, z, Blocks.IRON_BLOCK);   // a track line
            if ((Math.abs(dx) == 3 || Math.abs(dz) == 3) && (wx + wz & 3) == 0) {
               for (int y = p.y; y <= p.y + 2; y++) put(ch, x, y, z, Blocks.LOG_OAK);
               put(ch, x, p.y + 2, z, Blocks.TORCH);
            }
         }
      }
      put(ch, p.x - x0, p.y, p.z - z0, Blocks.CHEST_LOOT);
   }

   // 2. Dungeon: a small closed cobble room, spawner in the middle, two chests.
   private static void dungeon(Chunk ch, Plan p, int x0, int z0, int xa, int xb, int za, int zb) {
      for (int z = za; z <= zb; z++) {
         for (int x = xa; x <= xb; x++) {
            int wx = x0 + x, wz = z0 + z;
            int dx = Math.abs(wx - p.x), dz = Math.abs(wz - p.z);
            if (dx > 3 || dz > 3) continue;
            boolean wall = dx == 3 || dz == 3;
            for (int y = p.y; y <= p.y + 3; y++) {
               if (wall) {
                  long h = hash(p.h, wx, y, wz);
                  put(ch, x, y, z, CaveWorks.bits(h, 4, 5) == 0 ? Blocks.MOSSY_COBBLE : Blocks.COBBLE);
               } else {
                  carve(ch, x, y, z);
               }
            }
            long h = hash(p.h, wx, 0, wz);
            put(ch, x, p.y - 1, z, CaveWorks.bits(h, 4, 4) == 0 ? Blocks.MOSSY_COBBLE : Blocks.COBBLE);
            put(ch, x, p.y + 4, z, Blocks.COBBLE);
         }
      }
      put(ch, p.x - x0, p.y, p.z - z0, Blocks.SPAWNER);
      put(ch, p.x - x0 + 2, p.y, p.z - z0, Blocks.CHEST_LOOT);
      put(ch, p.x - x0 - 2, p.y, p.z - z0, Blocks.CHEST_LOOT);
   }

   // 3. Stronghold: stone-brick rooms, a library wall and an obsidian portal frame.
   private static void stronghold(Chunk ch, Plan p, int x0, int z0, int xa, int xb, int za, int zb) {
      for (int z = za; z <= zb; z++) {
         for (int x = xa; x <= xb; x++) {
            int wx = x0 + x, wz = z0 + z;
            int dx = wx - p.x, dz = wz - p.z;
            boolean room = Math.abs(dx) <= 5 && Math.abs(dz) <= 5;
            boolean corridor = Math.abs(dz) <= 1 && Math.abs(dx) <= 12;
            if (!room && !corridor) continue;
            boolean wall = Math.abs(dx) == 5 || Math.abs(dz) == 5 || Math.abs(dz) == 1;
            for (int y = p.y; y <= p.y + 4; y++) {
               if (wall) {
                  long h = hash(p.h, wx, y, wz);
                  int id = CaveWorks.bits(h, 4, 8);
                  put(ch, x, y, z, id == 0 ? Redstone319.CHISELED_STONEBRICK
                     : (id == 1 ? Redstone319.MOSSY_STONEBRICK
                        : (id == 2 ? Redstone319.CRACKED_STONEBRICK : Blocks.STONEBRICK)));
               } else {
                  carve(ch, x, y, z);
               }
            }
            put(ch, x, p.y - 1, z, Blocks.STONEBRICK);
            put(ch, x, p.y + 5, z, Blocks.STONEBRICK);
            // bookshelves along the north wall of the room
            if (dz == -4 && Math.abs(dx) <= 3) {
               put(ch, x, p.y + 1, z, Blocks.BOOKSHELF);
               put(ch, x, p.y + 2, z, Blocks.BOOKSHELF);
            }
         }
      }
      // portal frame at the far side
      int px = p.x + 3;
      for (int dy = 0; dy <= 4; dy++) {
         put(ch, px - x0, p.y + dy, p.z - z0 - 2, Blocks.OBSIDIAN);
         put(ch, px - x0, p.y + dy, p.z - z0 + 2, Blocks.OBSIDIAN);
      }
      for (int dz = -1; dz <= 1; dz++) {
         put(ch, px - x0, p.y, p.z - z0 + dz, Blocks.OBSIDIAN);
         put(ch, px - x0, p.y + 4, p.z - z0 + dz, Blocks.OBSIDIAN);
      }
      put(ch, px - x0, p.y + 2, p.z - z0, Blocks.GLOWSTONE);
      put(ch, p.x - x0 - 3, p.y, p.z - z0, Blocks.CHEST_LOOT);
   }

   // 4. Amethyst geode: spherical shell, hollow crystal interior.
   private static void geode(Chunk ch, Plan p, int x0, int z0, int xa, int xb, int za, int zb) {
      double r = 5.2;
      for (int z = za; z <= zb; z++) {
         for (int x = xa; x <= xb; x++) {
            int wx = x0 + x, wz = z0 + z;
            double dx = wx - p.x, dz = wz - p.z;
            for (int y = Math.max(6, p.y - 6); y <= p.y + 6; y++) {
               double dy = y - p.y;
               double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
               if (d > r) continue;
               if (d > r - 1.0) {
                  put(ch, x, y, z, Blocks.OBSIDIAN);
               } else if (d > r - 2.0) {
                  put(ch, x, y, z, Redstone319.PURPUR_BLOCK);
               } else if (d > r - 2.8) {
                  put(ch, x, y, z, Blocks.GLASS0 + 10);
               } else {
                  carve(ch, x, y, z);
                  long h = hash(p.h, wx, y, wz);
                  if (dy < -1.0 && CaveWorks.bits(h, 4, 12) == 0) put(ch, x, y, z, Blocks.GLASS0 + 10);
               }
            }
         }
      }
   }

   // 5. Ancient city: a dark floor, low walls and sea-lantern light.
   private static void ancientCity(Chunk ch, Plan p, int x0, int z0, int xa, int xb, int za, int zb) {
      for (int z = za; z <= zb; z++) {
         for (int x = xa; x <= xb; x++) {
            int wx = x0 + x, wz = z0 + z;
            int dx = Math.abs(wx - p.x), dz = Math.abs(wz - p.z);
            if (dx > 7 || dz > 7) continue;
            for (int y = p.y; y <= p.y + 3; y++) carve(ch, x, y, z);
            put(ch, x, p.y - 1, z, Redstone319.DARK_PRISMARINE);
            if (dx == 7 || dz == 7) {
               put(ch, x, p.y, z, Redstone319.DARK_PRISMARINE);
               put(ch, x, p.y + 1, z, Redstone319.DARK_PRISMARINE);
            }
         }
      }
      put(ch, p.x - x0, p.y, p.z - z0, Redstone319.SEA_LANTERN);
      put(ch, p.x - x0 + 3, p.y, p.z - z0, Blocks.CHEST_LOOT);
   }

   // 6. Ruined portal: an obsidian frame and cracked-stone-brick rubble.
   private static void ruinedPortal(Chunk ch, Plan p, int x0, int z0, int xa, int xb, int za, int zb) {
      for (int z = za; z <= zb; z++) {
         for (int x = xa; x <= xb; x++) {
            int wx = x0 + x, wz = z0 + z;
            int dx = Math.abs(wx - p.x), dz = Math.abs(wz - p.z);
            if (dx > 4 || dz > 4) continue;
            for (int y = p.y; y <= p.y + 4; y++) carve(ch, x, y, z);
            long h = hash(p.h, wx, 0, wz);
            put(ch, x, p.y - 1, z, CaveWorks.bits(h, 4, 3) == 0
               ? Redstone319.CRACKED_STONEBRICK : Blocks.STONEBRICK);
         }
      }
      int pz = p.z - 2;
      for (int dy = 0; dy <= 4; dy++) {
         put(ch, p.x - x0 - 2, p.y + dy, pz - z0, Blocks.OBSIDIAN);
         put(ch, p.x - x0 + 2, p.y + dy, pz - z0, Blocks.OBSIDIAN);
      }
      for (int dx = -1; dx <= 1; dx++) {
         put(ch, p.x - x0 + dx, p.y + 4, pz - z0, Blocks.OBSIDIAN);
         put(ch, p.x - x0 + dx, p.y, pz - z0, Blocks.OBSIDIAN);
      }
      put(ch, p.x - x0, p.y + 2, pz - z0, Blocks.GLOWSTONE);
      put(ch, p.x - x0 + 3, p.y, p.z - z0 + 2, Blocks.CHEST_LOOT);
   }

   /** A short 2x3 tunnel out to a real cave, so a structure is never sealed. */
   private void linkToCave(Chunk ch, Plan p, int x0, int z0) {
      if (caves == null || gen == null) return;
      double dirX = ((p.h >>> 12) & 1) == 0 ? 1.0 : -1.0;
      double dirZ = ((p.h >>> 13) & 1) == 0 ? 0.30 : -0.30;
      double len = Math.sqrt(dirX * dirX + dirZ * dirZ);
      dirX /= len; dirZ /= len;
      int start = 8, tx = 0, tz = 0, ty = p.y + 1;
      boolean found = false;
      for (int d = 6; d <= 30 && !found; d += 4) {
         int px = p.x + (int)Math.round(dirX * (start + d));
         int pz = p.z + (int)Math.round(dirZ * (start + d));
         int h = gen.column(px, pz).h;
         for (int yy = p.y - 4; yy <= p.y + 6; yy += 3) {
            if (caves.carve(px, yy, pz, h, false) != Caves6.SOLID) {
               tx = px; tz = pz; ty = yy; found = true; break;
            }
         }
      }
      if (!found) return;
      double ax = p.x, az = p.z, ay = p.y + 1;
      double vx = tx - ax, vy = ty - ay, vz = tz - az;
      double vv = vx * vx + vy * vy + vz * vz;
      if (vv <= 0.0) return;
      for (int z = 0; z < 16; z++) {
         for (int x = 0; x < 16; x++) {
            int wx = x0 + x, wz = z0 + z;
            double px = wx - ax, pz = wz - az;
            double t = Math.max(0.0, Math.min(1.0, (px * vx + pz * vz) / vv));
            double ex = px - t * vx, ez = pz - t * vz;
            if (ex * ex + ez * ez > 2.4) continue;
            int cy = (int)Math.round(ay + t * vy);
            for (int yy = cy - 1; yy <= cy + 1; yy++) carve(ch, x, yy, z);
         }
      }
   }
}
