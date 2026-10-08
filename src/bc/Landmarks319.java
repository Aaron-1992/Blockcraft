package bc;

import bc.Gen.Col;
import java.util.Random;

/**
 * Update 3.19: surface landmarks and overhauled world structures outside villages — Desert Pyramids, Jungle Temples, Swamp Witch Huts,
 * Snowy Igloos, and Ruined Watchtowers / Outposts — plus richer dungeon stonework and mineshaft sleepers.
 * Deterministic per 256 x 256 region from the world seed when {@code Gen.extras && gen.settings.structVer >= 2 && gen.settings.terra()}.
 */
final class Landmarks319 {
   static final int REG = 256;
   static final int K_NONE = 0;
   static final int K_PYRAMID = 1;
   static final int K_JUNGLE = 2;
   static final int K_WITCH = 3;
   static final int K_IGLOO = 4;
   static final int K_OUTPOST = 5;

   private Landmarks319() {
   }

   static final class Site {
      int kind;
      int x0;
      int y0;
      int z0;
      int w;
      int d;
   }

   static Site plan(Structs s, int rx, int rz, Chunk ch) {
      Random r = new Random(s.seed * 3935559000370003845L + rx * 2862933555777941757L + rz * 7046029254386353131L + 31901L);
      if (r.nextFloat() > 0.55F) {
         return null;
      }

      int x = rx * REG + 48 + r.nextInt(REG - 96);
      int z = rz * REG + 48 + r.nextInt(REG - 96);
      if (ch != null && !Structs.overlaps(ch, x - 8, z - 8, x + 8, z + 8)) {
         return null;
      }

      Gen gen = s.gen;
      Col c = gen.column(x, z);
      if (c.h < 64 || c.h > 96 || Biomes.ocean(c.biome) || Biomes.river(c.biome)) {
         return null;
      }

      int kind;
      int w;
      int d;
      switch (c.biome) {
         case Biomes.DESERT -> {
            kind = K_PYRAMID;
            w = 15;
            d = 15;
         }
         case Biomes.JUNGLE -> {
            kind = K_JUNGLE;
            w = 11;
            d = 13;
         }
         case Biomes.SWAMP -> {
            kind = K_WITCH;
            w = 7;
            d = 7;
         }
         case Biomes.ICE_PLAINS, Biomes.SNOWY_TAIGA, Biomes.ICE_SPIKES -> {
            kind = K_IGLOO;
            w = 7;
            d = 8;
         }
         case Biomes.PLAINS, Biomes.SAVANNA, Biomes.BADLANDS, Biomes.FOREST, Biomes.TAIGA, Biomes.MOUNTAINS -> {
            if (r.nextInt(3) != 0) {
               return null;
            }

            kind = K_OUTPOST;
            w = 7;
            d = 7;
         }
         default -> {
            return null;
         }
      }

      // Check terrain flatness across footprint
      int x0 = x - w / 2;
      int z0 = z - d / 2;
      int y0 = c.h;
      for (int dz = 0; dz < d; dz += 3) {
         for (int dx = 0; dx < w; dx += 3) {
            Col cc = gen.column(x0 + dx, z0 + dz);
            if (cc.h < 63 || Math.abs(cc.h - y0) > 2 || Biomes.ocean(cc.biome) || Biomes.river(cc.biome)) {
               return null;
            }
         }
      }

      // Stay clear of any village in this village region
      int vrx = Math.floorDiv(x, 320);
      int vrz = Math.floorDiv(z, 320);
      Structs.Village v = s.village(vrx, vrz);
      if (v != null && v.valid && x0 + w + 16 >= v.minX && x0 - 16 <= v.maxX && z0 + d + 16 >= v.minZ && z0 - 16 <= v.maxZ) {
         return null;
      }

      Site site = new Site();
      site.kind = kind;
      site.x0 = x0;
      site.y0 = y0;
      site.z0 = z0;
      site.w = w;
      site.d = d;
      return site;
   }

   static Site plan(Structs s, int rx, int rz) {
      return plan(s, rx, rz, null);
   }

   static void apply(Structs s, Chunk ch) {
      if (!Gen.extras || s.gen.settings.structVer < 2 || !s.gen.settings.terra()) {
         return;
      }

      int rx = Math.floorDiv(ch.cx * 16 + 8, REG);
      int rz = Math.floorDiv(ch.cz * 16 + 8, REG);
      Site site = plan(s, rx, rz, ch);
      if (site != null && Structs.overlaps(ch, site.x0, site.z0, site.x0 + site.w - 1, site.z0 + site.d - 1)) {
         buildSite(ch, site);
      }
   }

   private static void prep(Chunk ch, Site s, int baseBlock, int clearH) {
      for (int z = s.z0; z < s.z0 + s.d; z++) {
         for (int x = s.x0; x < s.x0 + s.w; x++) {
            if (!Structs.overlaps(ch, x, z, x, z)) {
               continue;
            }

            for (int y = s.y0 + 3; y <= s.y0 + clearH; y++) {
               Structs.set(ch, x, y, z, 0);
            }

            for (int y = s.y0 + 2; y >= s.y0 - 4; y--) {
               int cur = Structs.get(ch, x, y, z);
               if (y <= s.y0 || cur == 0 || Structs.soft(cur) || Tpl.vegetation(cur)) {
                  Structs.set(ch, x, y, z, y <= s.y0 ? baseBlock : 0);
               }
            }
         }
      }
   }

   static void buildSite(Chunk ch, Site s) {
      switch (s.kind) {
         case K_PYRAMID -> buildPyramid(ch, s);
         case K_JUNGLE -> buildJungleTemple(ch, s);
         case K_WITCH -> buildWitchHut(ch, s);
         case K_IGLOO -> buildIgloo(ch, s);
         case K_OUTPOST -> buildOutpost(ch, s);
         default -> {
         }
      }
   }

   private static void buildPyramid(Chunk ch, Site s) {
      int ss = Blocks.SANDSTONE;
      int orange = Blocks.STAINED0 + 1;
      int blue = Blocks.STAINED0 + 11;
      prep(ch, s, ss, 14);
      // Stepped pyramid shell (15 x 15, 7 steps high)
      for (int step = 0; step < 7; step++) {
         int y = s.y0 + 1 + step;
         for (int dz = step; dz < 15 - step; dz++) {
            for (int dx = step; dx < 15 - step; dx++) {
               boolean edge = dx == step || dx == 14 - step || dz == step || dz == 14 - step;
               int blk = edge ? ((step == 2 || step == 4) && (dx == 7 || dz == 7) ? orange : ss) : 0;
               if (step == 6) {
                  blk = ss;
               }

               Structs.set(ch, s.x0 + dx, y, s.z0 + dz, blk);
            }
         }
      }

      // Twin flanking entrance pylons on the north side (dz = 0..2)
      for (int side : new int[]{1, 11}) {
         for (int y = 1; y <= 6; y++) {
            for (int dz = 0; dz < 3; dz++) {
               for (int dx = side; dx < side + 3; dx++) {
                  boolean wall = dx == side || dx == side + 2 || dz == 0 || dz == 2 || y == 6;
                  int blk = wall ? ((y == 3 || y == 4) && dx == side + 1 && dz == 0 ? orange : ss) : 0;
                  Structs.set(ch, s.x0 + dx, s.y0 + y, s.z0 + dz, blk);
               }
            }
         }
      }

      // Entrance doorway at dx = 7, dz = 0..1
      Structs.set(ch, s.x0 + 7, s.y0 + 1, s.z0, 0);
      Structs.set(ch, s.x0 + 7, s.y0 + 2, s.z0, 0);
      Structs.set(ch, s.x0 + 7, s.y0 + 1, s.z0 + 1, 0);
      Structs.set(ch, s.x0 + 7, s.y0 + 2, s.z0 + 1, 0);
      // Mosaic sun floor & interior sandstone pillars
      Structs.set(ch, s.x0 + 7, s.y0, s.z0 + 7, blue);
      for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
         Structs.set(ch, s.x0 + 7 + d[0], s.y0, s.z0 + 7 + d[1], orange);
      }

      for (int[] p : new int[][]{{4, 4}, {10, 4}, {4, 10}, {10, 10}}) {
         for (int y = 1; y <= 3; y++) {
            Structs.set(ch, s.x0 + p[0], s.y0 + y, s.z0 + p[1], ss);
         }
      }

      // Subterranean treasure chamber deep below (y0 - 8 .. y0 - 5) so surface col.h stays 100% solid
      for (int y = s.y0 - 8; y <= s.y0 - 5; y++) {
         for (int dz = 5; dz <= 9; dz++) {
            for (int dx = 5; dx <= 9; dx++) {
               boolean wall = dx == 5 || dx == 9 || dz == 5 || dz == 9 || y == s.y0 - 8;
               Structs.set(ch, s.x0 + dx, y, s.z0 + dz, wall ? ss : 0);
            }
         }
      }

      Structs.set(ch, s.x0 + 7, s.y0 - 7, s.z0 + 6, Blocks.CHEST_LOOT);
      Structs.set(ch, s.x0 + 7, s.y0 - 7, s.z0 + 8, Blocks.CHEST_LOOT);
      Structs.set(ch, s.x0 + 6, s.y0 - 7, s.z0 + 7, Blocks.CHEST_LOOT);
      Structs.set(ch, s.x0 + 8, s.y0 - 7, s.z0 + 7, Blocks.CHEST_LOOT);
      Structs.set(ch, s.x0 + 7, s.y0 - 7, s.z0 + 7, Blocks.GOLD_BLOCK);
   }

   private static void buildJungleTemple(Chunk ch, Site s) {
      int cb = Blocks.COBBLE;
      int mc = Blocks.MOSSY_COBBLE;
      int sb = Blocks.STONEBRICK;
      prep(ch, s, mc, 11);
      for (int y = 1; y <= 4; y++) {
         for (int dz = 1; dz < 12; dz++) {
            for (int dx = 1; dx < 10; dx++) {
               boolean wall = dx == 1 || dx == 9 || dz == 1 || dz == 11 || y == 4;
               int blk = wall ? (((dx + y + dz) & 1) == 0 ? mc : cb) : 0;
               Structs.set(ch, s.x0 + dx, s.y0 + y, s.z0 + dz, blk);
            }
         }
      }

      // Upper sanctuary tier
      for (int y = 5; y <= 7; y++) {
         for (int dz = 3; dz < 10; dz++) {
            for (int dx = 3; dx < 8; dx++) {
               boolean wall = dx == 3 || dx == 7 || dz == 3 || dz == 9 || y == 7;
               Structs.set(ch, s.x0 + dx, s.y0 + y, s.z0 + dz, wall ? sb : 0);
            }
         }
      }

      Structs.set(ch, s.x0 + 5, s.y0 + 1, s.z0 + 1, 0);
      Structs.set(ch, s.x0 + 5, s.y0 + 2, s.z0 + 1, 0);
      Structs.set(ch, s.x0 + 3, s.y0 + 1, s.z0 + 10, Blocks.CHEST_LOOT);
      Structs.set(ch, s.x0 + 7, s.y0 + 1, s.z0 + 10, Blocks.CHEST_LOOT);
      Structs.set(ch, s.x0 + 5, s.y0 + 1, s.z0 + 10, Blocks.EMERALD_BLOCK);
      Structs.set(ch, s.x0 + 2, s.y0 + 2, s.z0 + 2, Blocks.TORCH);
      Structs.set(ch, s.x0 + 8, s.y0 + 2, s.z0 + 2, Blocks.TORCH);
   }

   private static void buildWitchHut(Chunk ch, Site s) {
      int sp = Blocks313.PLANKS_SPRUCE;
      int log = Blocks.LOG_OAK;
      int fence = Shapes.FENCE0;
      prep(ch, s, Blocks.DIRT, 9);
      // Four corner oak log stilts down to ground and up to roof
      for (int[] p : new int[][]{{1, 1}, {5, 1}, {1, 5}, {5, 5}}) {
         for (int y = -2; y <= 5; y++) {
            Structs.set(ch, s.x0 + p[0], s.y0 + y, s.z0 + p[1], log);
         }
      }

      for (int dz = 1; dz <= 5; dz++) {
         for (int dx = 1; dx <= 5; dx++) {
            Structs.set(ch, s.x0 + dx, s.y0 + 1, s.z0 + dz, sp);
            for (int y = 2; y <= 4; y++) {
               boolean wall = dx == 1 || dx == 5 || dz == 1 || dz == 5;
               if (wall && !(dx == 3 && dz == 1 && y <= 3)) {
                  int blk = (y == 3 && (dx == 3 || dz == 3)) ? fence : sp;
                  Structs.set(ch, s.x0 + dx, s.y0 + y, s.z0 + dz, blk);
               } else {
                  Structs.set(ch, s.x0 + dx, s.y0 + y, s.z0 + dz, 0);
               }
            }
         }
      }

      // Roof overhang
      for (int dz = 0; dz <= 6; dz++) {
         for (int dx = 0; dx <= 6; dx++) {
            Structs.set(ch, s.x0 + dx, s.y0 + 5, s.z0 + dz, sp);
         }
      }

      // Front steps & interior furnishings
      Structs.set(ch, s.x0 + 3, s.y0 + 1, s.z0, sp);
      Structs.set(ch, s.x0 + 2, s.y0 + 2, s.z0 + 4, Blocks.CRAFTING_TABLE);
      Structs.set(ch, s.x0 + 4, s.y0 + 2, s.z0 + 4, Blocks.FURNACE);
      Structs.set(ch, s.x0 + 3, s.y0 + 2, s.z0 + 4, Blocks.CHEST_LOOT);
   }

   private static void buildIgloo(Chunk ch, Site s) {
      int snow = Blocks.SNOW;
      int ice = Blocks313.PACKED_ICE;
      prep(ch, s, snow, 7);
      for (int dz = 2; dz <= 6; dz++) {
         for (int dx = 1; dx <= 5; dx++) {
            for (int y = 1; y <= 3; y++) {
               boolean wall = dx == 1 || dx == 5 || dz == 2 || dz == 6;
               int blk = wall ? (y == 2 && (dx == 1 || dx == 5) && dz == 4 ? ice : snow) : 0;
               Structs.set(ch, s.x0 + dx, s.y0 + y, s.z0 + dz, blk);
            }

            if (dx >= 2 && dx <= 4 && dz >= 3 && dz <= 5) {
               Structs.set(ch, s.x0 + dx, s.y0 + 4, s.z0 + dz, snow);
               Structs.set(ch, s.x0 + dx, s.y0 + 1, s.z0 + dz, Shapes.CARPET0 + ((dx + dz) & 1) * 14);
            }
         }
      }

      // Entrance tunnel at dx = 3, dz = 0..2
      for (int dz = 0; dz <= 2; dz++) {
         Structs.set(ch, s.x0 + 2, s.y0 + 1, s.z0 + dz, snow);
         Structs.set(ch, s.x0 + 2, s.y0 + 2, s.z0 + dz, snow);
         Structs.set(ch, s.x0 + 4, s.y0 + 1, s.z0 + dz, snow);
         Structs.set(ch, s.x0 + 4, s.y0 + 2, s.z0 + dz, snow);
         Structs.set(ch, s.x0 + 3, s.y0 + 3, s.z0 + dz, snow);
         Structs.set(ch, s.x0 + 3, s.y0 + 1, s.z0 + dz, 0);
         Structs.set(ch, s.x0 + 3, s.y0 + 2, s.z0 + dz, 0);
      }

      Structs.set(ch, s.x0 + 2, s.y0 + 1, s.z0 + 5, Blocks.FURNACE);
      Structs.set(ch, s.x0 + 3, s.y0 + 1, s.z0 + 5, Blocks.CRAFTING_TABLE);
      Structs.set(ch, s.x0 + 4, s.y0 + 1, s.z0 + 5, Blocks.CHEST_LOOT);
      Structs.set(ch, s.x0 + 3, s.y0 + 2, s.z0 + 5, Blocks.TORCH);
   }

   private static void buildOutpost(Chunk ch, Site s) {
      int sb = Blocks.STONEBRICK;
      int mc = Blocks.MOSSY_COBBLE;
      int log = Blocks313.LOG_DARKOAK;
      int pl = Blocks313.PLANKS_DARKOAK;
      prep(ch, s, sb, 13);
      for (int y = 1; y <= 8; y++) {
         for (int dz = 1; dz <= 5; dz++) {
            for (int dx = 1; dx <= 5; dx++) {
               boolean corner = (dx == 1 || dx == 5) && (dz == 1 || dz == 5);
               boolean wall = dx == 1 || dx == 5 || dz == 1 || dz == 5;
               int blk = corner ? log : (wall ? (y <= 4 ? (((dx + y + dz) & 1) == 0 ? sb : mc) : pl) : (y == 4 || y == 8 ? pl : 0));
               if (dx == 3 && dz == 1 && y <= 2) {
                  blk = 0;
               }

               if (wall && (y == 3 || y == 6) && (dx == 3 || dz == 3)) {
                  blk = Shapes.FENCE0 + 5;
               }

               Structs.set(ch, s.x0 + dx, s.y0 + y, s.z0 + dz, blk);
            }
         }

         if (y <= 8) {
            Structs.set(ch, s.x0 + 4, s.y0 + y, s.z0 + 4, 119); // ladder up the inside corner
         }
      }

      // Battlement parapet at y = 9
      for (int dz = 1; dz <= 5; dz++) {
         for (int dx = 1; dx <= 5; dx++) {
            if ((dx == 1 || dx == 5 || dz == 1 || dz == 5) && ((dx + dz) & 1) == 0) {
               Structs.set(ch, s.x0 + dx, s.y0 + 9, s.z0 + dz, sb);
            }
         }
      }

      Structs.set(ch, s.x0 + 2, s.y0 + 1, s.z0 + 4, Blocks.CHEST_LOOT);
      Structs.set(ch, s.x0 + 2, s.y0 + 5, s.z0 + 2, Blocks.CHEST_LOOT);
      Structs.set(ch, s.x0 + 3, s.y0 + 9, s.z0 + 3, Blocks.TORCH);
   }
}
