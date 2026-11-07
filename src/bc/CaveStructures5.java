package bc;

import java.util.concurrent.ConcurrentHashMap;

/**
 * V6.31: a completely new underground structure system for {@code caveVer >= 5}.
 *
 * <p>The V6.21 sites were six boxy rooms on a 112-block grid. These are eight
 * different, much larger builds on a 96-block grid: a rotunda with two side
 * chambers, four arched corridors, support pillars, a walkable floor, a linked
 * tunnel into the real noise caves, and type-specific content (mineshaft, geode
 * cathedral, ancient hall, lava foundry, mushroom grotto, flooded cistern,
 * crystal vault and dungeon). Everything is stamped in world coordinates, so a
 * structure that straddles a chunk border is identical on both sides.</p>
 */
final class CaveStructures5 {
   static final int REGION = 96;

   static final String[] NAME = {
      "Mineshaft", "Geode Cathedral", "Ancient Hall", "Lava Foundry",
      "Mushroom Grotto", "Flooded Cistern", "Crystal Vault", "Dungeon"
   };

   static final class Plan {
      boolean valid;
      int x, z, y, type, rx, rz, ry, rot;
      long h;
   }

   final long seed;
   final Gen gen;
   final Caves5 caves;
   private final ConcurrentHashMap<Long, Plan> plans = new ConcurrentHashMap<>();

   CaveStructures5(long seed, Gen gen, Caves5 caves) {
      this.seed = seed;
      this.gen = gen;
      this.caves = caves;
   }

   static Plan plan(long seed, int rx, int rz) {
      long h = CaveWorks.mix(seed ^ 0x53545255435435L, rx, 0, rz);
      Plan p = new Plan();
      p.h = h;
      if ((h >>> 58) >= 44) return p;    // ~69% of cells get a structure
      p.valid = true;
      p.x = rx * REGION + 12 + CaveWorks.bits(h, 8, 72);
      p.z = rz * REGION + 12 + CaveWorks.bits(h, 20, 72);
      p.type = CaveWorks.bits(h, 30, NAME.length);
      p.rot = CaveWorks.bits(h, 34, 2);
      int band = CaveWorks.bits(h, 36, 3);
      p.y = band == 0 ? 14 + CaveWorks.bits(h, 40, 12)
         : (band == 1 ? 28 + CaveWorks.bits(h, 40, 16) : 46 + CaveWorks.bits(h, 40, 14));
      p.rx = 11 + CaveWorks.bits(h, 46, 9);
      p.rz = 11 + CaveWorks.bits(h, 52, 9);
      p.ry = 5 + CaveWorks.bits(h, 58, 4);
      return p;
   }

   private Plan cached(int rx, int rz) {
      long key = ((long)rx << 32) ^ (rz & 0xFFFFFFFFL);
      return plans.computeIfAbsent(key, k -> {
         Plan p = plan(seed, rx, rz);
         if (p.valid && gen != null) {
            // Never float in an ocean's water column or break the surface.
            p.y = Math.max(12, Math.min(p.y, gen.column(p.x, p.z).h - 16));
         }
         return p;
      });
   }

   /** Stamps every structure whose footprint can touch this chunk. */
   void apply(Chunk ch) {
      int margin = 48;
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

   /** Carves the rooms/corridors and then builds the structure. Pure in world coordinates. */
   void stamp(Chunk ch, Plan p) {
      int x0 = ch.cx * 16, z0 = ch.cz * 16;
      int reach = Math.max(p.rx, p.rz) + 20;
      int xa = Math.max(0, p.x - reach - x0);
      int xb = Math.min(15, p.x + reach - x0);
      int za = Math.max(0, p.z - reach - z0);
      int zb = Math.min(15, p.z + reach - z0);
      if (xa > xb || za > zb) return;

      int floorY = p.y - p.ry + 1;
      int ceilY = p.y + p.ry - 1;
      int sideX = p.rot == 0 ? p.rx + 12 : 0;
      int sideZ = p.rot == 0 ? 0 : p.rz + 12;
      long wall = CaveWorks.bits(p.h, 4, 4);

      // ---- pass 1: carve a rotunda, two side chambers and four arched corridors
      for (int z = za; z <= zb; z++) {
         for (int x = xa; x <= xb; x++) {
            int wx = x0 + x, wz = z0 + z;
            int dx = wx - p.x, dz = wz - p.z;
            double main = (double)(dx * dx) / (p.rx * p.rx) + (double)(dz * dz) / (p.rz * p.rz);
            int s1x = wx - (p.x + sideX), s1z = wz - (p.z + sideZ);
            int s2x = wx - (p.x - sideX), s2z = wz - (p.z - sideZ);
            double side1 = (double)(s1x * s1x + s1z * s1z) / 64.0;
            double side2 = (double)(s2x * s2x + s2z * s2z) / 64.0;
            boolean corridor = (Math.abs(dz) <= 1 && Math.abs(dx) <= reach)
               || (Math.abs(dx) <= 1 && Math.abs(dz) <= reach)
               || (p.rot == 0 && Math.abs(dz - sideZ) <= 1 && Math.abs(dx) <= sideX)
               || (p.rot == 1 && Math.abs(dx - sideX) <= 1 && Math.abs(dz) <= sideX);
            for (int y = Math.max(6, floorY - 2); y <= Math.min(249, ceilY + 3); y++) {
               double dy = (double)(y - p.y) / p.ry;
               boolean room = main + dy * dy <= 0.98 || side1 + dy * dy <= 1.0 || side2 + dy * dy <= 1.0;
               boolean tube = corridor && y >= floorY + 1 && y <= floorY + 3;
               if (!room && !tube) continue;
               int id = get(ch, x, y, z);
               if (id == 6 || Blocks.isFluid(id)) continue;
               put(ch, x, y, z, 0);
               // a solid, level floor under the carved space
               if (y == floorY + 1) {
                  int under = get(ch, x, floorY, z);
                  if (under == 0 || !Blocks.isFluid(under)) put(ch, x, floorY, z, floorBlock(p.type, wall));
               }
               // arched ceiling: leave a rock rim, not a flat lid
               if (room && y == ceilY - 1 && (dx + dz & 3) == 0) {
                  put(ch, x, y, z, ceilingBlock(p.type));
               }
            }
         }
      }

      // ---- pass 2: pillars, rails of supports along corridors, and the build itself
      for (int z = za; z <= zb; z++) {
         for (int x = xa; x <= xb; x++) {
            int wx = x0 + x, wz = z0 + z;
            int dx = wx - p.x, dz = wz - p.z;
            boolean pillar = ((Math.abs(dx) == p.rx / 2 && Math.abs(dz) == p.rz / 2)
               || (Math.abs(dx) == p.rx / 2 && dz == 0) || (dx == 0 && Math.abs(dz) == p.rz / 2));
            if (pillar) {
               for (int y = floorY + 1; y <= ceilY - 1; y++) put(ch, x, y, z, pillarBlock(p.type));
            }
            boolean rail = (Math.abs(dz) <= 1 && Math.abs(dx) <= reach)
               || (Math.abs(dx) <= 1 && Math.abs(dz) <= reach);
            if (rail && ((wx + wz) & 7) == 0) {
               put(ch, x, floorY + 1, z, supportBlock(p.type));
               put(ch, x, ceilY - 1, z, lightBlock(p.type));
            }
            if (dx == 0 && dz == 0) {
               // the centrepiece of each type
               put(ch, x, floorY + 1, z, featureBlock(p.type));
               if (p.type == 0 || p.type == 2 || p.type == 7) put(ch, x, floorY + 2, z, Blocks.CHEST_LOOT);
               if (p.type == 7) put(ch, x, floorY + 3, z, Blocks.SPAWNER);
            }
            if (p.type == 3 && (dx * dx + dz * dz) < 20) {
               put(ch, x, floorY, z, Blocks.LAVA);
            }
            if (p.type == 5 && (dx * dx + dz * dz) < 30) {
               put(ch, x, floorY + 1, z, Blocks.WATER);
            }
            if (p.type == 4 && ((wx * 31 + wz * 17) & 15) < 3) {
               put(ch, x, floorY + 1, z, Blocks313.MUSHROOM_RED);
            }
            if (p.type == 6 && ((wx + wz) & 3) == 0) {
               put(ch, x, floorY + 1, z, Blocks.GLOWSTONE);
            }
         }
      }

      // ---- pass 3: a straight tunnel out to a real carved cave so it is never sealed
      linkToCave(ch, p, floorY + 2);
   }

   private void linkToCave(Chunk ch, Plan p, int y) {
      if (caves == null || gen == null) return;
      int x0 = ch.cx * 16, z0 = ch.cz * 16;
      double dirX = ((p.h >>> 12) & 1) == 0 ? 1.0 : -1.0;
      double dirZ = ((p.h >>> 13) & 1) == 0 ? 0.35 : -0.35;
      double len = Math.sqrt(dirX * dirX + dirZ * dirZ);
      dirX /= len;
      dirZ /= len;
      int start = Math.max(p.rx, p.rz) + 4;
      int tx = 0, tz = 0, ty = y;
      boolean found = false;
      for (int d = 8; d <= 56 && !found; d += 4) {
         int px = p.x + (int)Math.round(dirX * (start + d));
         int pz = p.z + (int)Math.round(dirZ * (start + d));
         int h = gen.column(px, pz).h;
         for (int yy = y - 10; yy <= y + 6; yy += 4) {
            if (caves.carve(px, yy, pz, h, false) != Caves5.SOLID) {
               tx = px; tz = pz; ty = yy; found = true; break;
            }
         }
      }
      if (!found) return;
      double ax = p.x, az = p.z, ay = y;
      double vx = tx - ax, vy = ty - ay, vz = tz - az;
      double vv = vx * vx + vy * vy + vz * vz;
      if (vv <= 0.0) return;
      for (int z = 0; z < 16; z++) {
         for (int x = 0; x < 16; x++) {
            int wx = x0 + x, wz = z0 + z;
            double px = wx - ax, py = 0.0, pz = wz - az;
            double t = Math.max(0.0, Math.min(1.0, (px * vx + pz * vz) / vv));
            double ex = px - t * vx, ez = pz - t * vz;
            if (ex * ex + ez * ez > 6.0) continue;
            int cy = (int)Math.round(ay + t * vy);
            for (int yy = cy - 2; yy <= cy + 2; yy++) {
               int id = get(ch, x, yy, z);
               if (id != 6 && !Blocks.isFluid(id)) put(ch, x, yy, z, 0);
            }
         }
      }
   }

   // ------------------------------------------------------------------ palette per type
   private static int floorBlock(int t, long wall) {
      switch (t) {
         case 0: return Blocks.PLANKS;
         case 1: return Redstone319.PURPUR_BLOCK;
         case 2: return Blocks.STONEBRICK;
         case 3: return Blocks.COBBLE;
         case 4: return Blocks313.MYCELIUM;
         case 5: return Blocks.STONEBRICK;
         case 6: return Blocks.QUARTZ_BLOCK;
         default: return Blocks.MOSSY_COBBLE;
      }
   }

   private static int ceilingBlock(int t) {
      switch (t) {
         case 1: return Blocks.GLASS0 + 10;
         case 6: return Blocks.GLOWSTONE;
         default: return Blocks.STONEBRICK;
      }
   }

   private static int pillarBlock(int t) {
      switch (t) {
         case 0: return Blocks.LOG_OAK;
         case 1: return Redstone319.PURPUR_BLOCK;
         case 3: return Blocks.OBSIDIAN;
         case 4: return Blocks313.MUSH_STEM;
         case 6: return Blocks.QUARTZ_BLOCK;
         default: return Blocks.COBBLE;
      }
   }

   private static int supportBlock(int t) {
      return t == 0 ? Blocks.LOG_OAK : (t == 3 ? Blocks.OBSIDIAN : Blocks.COBBLE);
   }

   private static int lightBlock(int t) {
      return t == 1 || t == 6 ? Blocks.GLOWSTONE : (t == 3 ? Blocks.LAVA : Blocks.TORCH);
   }

   private static int featureBlock(int t) {
      switch (t) {
         case 0: return Blocks.CHEST_LOOT;
         case 1: return Blocks.GLASS0 + 10;
         case 2: return Blocks.BOOKSHELF;
         case 3: return Blocks.FURNACE_LIT;
         case 4: return Blocks313.MUSH_BLOCK_BROWN;
         case 5: return Blocks.CLAY;
         case 6: return Blocks.GOLD_BLOCK;
         default: return Blocks.SPAWNER;
      }
   }
}
