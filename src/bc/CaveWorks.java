package bc;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Update 6.21: a complete redo of the underground for newly created worlds ({@code terrainVer >= 3}).
 * <p>
 * The V6.19 "big caves" hollowed out almost everything below y=54 (a 3D cheese noise above a low
 * threshold plus two wide worm networks). Deep worlds became one continuous swiss-cheese cavern.
 * This generator is deliberately calmer and layered:
 * <ul>
 *   <li><b>winding tunnels</b> - a ridged 3D noise pair with a depth-dependent radius: narrow under the
 *       surface, wider in the middle, tightening again near bedrock;</li>
 *   <li><b>sparse chambers</b> - a high-threshold 3D noise gated to y=14..48, so rooms are discrete
 *       instead of one endless hollow;</li>
 *   <li><b>rare giant halls</b> - hashed ellipsoids on a 96-block cell grid in the deep layer;</li>
 *   <li><b>lava lakes only in lava provinces</b>, not in every carved block below y=10;</li>
 *   <li><b>depth strata</b> - andesite/granite/diorite bands that get darker with depth, exposed on
 *       cave walls;</li>
 *   <li><b>stuff</b> - stalactites, stalagmites, columns, rubble, ore pockets, crystals, mushrooms,
 *       glowcaps, springs, pools, cobwebs of vines and the odd spawner;</li>
 *   <li><b>structures</b> - six kinds of deterministic underground build (ruined shrine, crystal
 *       geode, lava forge, mineshaft hall, mushroom grotto, flooded cistern), each in its own carved
 *       chamber with corridors reaching out towards the natural tunnels.</li>
 * </ul>
 * Everything is a pure function of world coordinates and the seed (plus a cached, seed-derived room
 * plan), so neighbouring chunks always agree on the block at a shared coordinate and generation can
 * run on any worker thread. Old saves (terrainVer 1 or 2) never call into this class.
 */
final class CaveWorks {
   /** How far apart underground sites are, in blocks. */
   static final int REGION = 112;
   /** Cell size of the rare giant halls. */
   static final int HALL = 96;

   final long seed;
   final WorldSettings st;
   final Gen gen;
   final Noise a;
   final Noise b;
   final Noise c;
   final Noise e;
   /** Update 6.22: the Caves & Cliffs carver, only used when st.caveVer >= 2. */
   final CaveCliffs cliffs;
   private final ConcurrentHashMap<Long, Plan> plans = new ConcurrentHashMap<>();

   CaveWorks(long seed, WorldSettings st, Gen gen, Noise a, Noise b, Noise c, Noise e) {
      this.seed = seed;
      this.st = st;
      this.gen = gen;
      this.a = a;
      this.b = b;
      this.c = c;
      this.e = e;
      this.cliffs = new CaveCliffs(seed, st, gen);
   }

   // ------------------------------------------------------------------ instance shortcuts
   int hollow(int x, int y, int z) {
      return hollow(this.a, this.b, this.c, this.seed, x, y, z, this.st.caveScale, this.st.caveDensity);
   }

   /** Update 6.22: the Caves & Cliffs carver (0 solid, 1 air, 2 lava, 3 aquifer water). */
   int hollowCC(int x, int y, int z) {
      return this.cliffs.carve(x, y, z);
   }

   boolean mouth(int x, int z) {
      return mouth(this.e, this.seed, x, z);
   }

   /** V6.28: the real hashed cave entrances near this column (caveVer >= 3 only). */
   CaveCliffs.Mouth[] mouths(int x, int z) {
      return this.cliffs.mouths(x, z);
   }

   /** True when the surface here is the opening of a real cave entrance. */
   boolean mouthSurface(int x, int z, int surfaceH) {
      if (this.st.caveVer < 3) return false;
      if (surfaceH < (this.st.caveVer >= 4 ? 64 : 66)) return false;
      CaveCliffs.Mouth[] ms = this.cliffs.mouths(x, z);
      if (ms == null) return false;
      for (CaveCliffs.Mouth m : ms) if (m.surfaceHole(x, z)) return true;
      return false;
   }

   // ------------------------------------------------------------------ deterministic mixing
   /** A 64-bit mixer: the same (seed, x, y, z) always gives the same bits, on every thread. */
   static long mix(long seed, int x, int y, int z) {
      long h = seed ^ 0x9E3779B97F4A7C15L;
      h ^= (long)x * 0xBF58476D1CE4E5B9L;
      h ^= (long)y * 0x94D049BB133111EBL;
      h ^= (long)z * 0xD6E8FEB86659FD93L;
      h ^= h >>> 30;
      h *= 0xBF58476D1CE4E5B9L;
      h ^= h >>> 27;
      h *= 0x94D049BB133111EBL;
      h ^= h >>> 31;
      return h;
   }

   static int bits(long h, int shift, int n) {
      return (int)Math.floorMod(h >>> shift, (long)n);
   }

   // ------------------------------------------------------------------ the cave field
   /**
    * 0 = solid rock, 1 = air, 2 = lava. This is the whole new cave shape: no global cheese layer,
    * no second wide worm network, no lava under every tunnel.
    */
   static int hollow(Noise a, Noise b, Noise c, long seed, int x, int y, int z, float scale, float density) {
      if (y < 6 || y > 248) {
         return 0;
      }

      float cs = scale < 0.6F ? 0.6F : (scale > 3.0F ? 3.0F : scale);
      float dens = density < 0.4F ? 0.4F : (density > 2.5F ? 2.5F : density);
      double k = 1.0 / cs;
      // A vertical profile instead of "the deeper, the more hollow": calm near the surface,
      // roomy in the middle, tighter again just above the bedrock.
      double band = y > 54 ? 0.60 : (y > 30 ? 1.00 : (y > 16 ? 1.16 : 0.68));
      double ax = a.noise(x * 0.019 * k, y * 0.029 * k, z * 0.019 * k);
      double bx = b.noise(x * 0.019 * k + 31.7, y * 0.029 * k + 11.3, z * 0.019 * k - 47.1);
      boolean air = ax * ax + bx * bx < 0.0018 * dens * band;

      // Discrete chambers: a high threshold, gated by a slow 2D province so they cluster
      // into cave regions rather than appearing uniformly everywhere.
      if (!air && y >= 14 && y <= 48) {
         double q = c.noise(x * 0.011 * k + 91.0, y * 0.017 * k - 17.0, z * 0.011 * k + 53.0);
         double gate = 0.63 + 0.07 * a.noise(x * 0.004 * k, 7.0, z * 0.004 * k) - 0.05 * (dens - 1.0F);
         if (q > gate) air = true;
      }

      // Rare, genuinely huge deep halls on a 96-block cell grid.
      if (!air && y >= 9 && y <= 38 && hall(seed, x, y, z)) {
         air = true;
      }

      if (!air) {
         return 0;
      }

      // Lava only in coherent lava provinces, and only at the very bottom.
      if (y <= 10 && c.noise(x * 0.004, 3.0, z * 0.004) > 0.16) {
         return 2;
      }

      return 1;
   }

   /** A handful of big ellipsoidal rooms, never a continuous network. */
   static boolean hall(long seed, int x, int y, int z) {
      int cx = Math.floorDiv(x, HALL);
      int cz = Math.floorDiv(z, HALL);
      for (int i = -1; i <= 1; i++) {
         for (int j = -1; j <= 1; j++) {
            long h = mix(seed ^ 0x48414C4CL, cx + i, 0, cz + j);
            if ((h >>> 60) >= 5) {
               continue; // ~31% of cells get a hall
            }

            int px = (cx + i) * HALL + 22 + bits(h, 8, 52);
            int pz = (cz + j) * HALL + 22 + bits(h, 20, 52);
            int py = 12 + bits(h, 30, 24);
            int rx = 11 + bits(h, 38, 8);
            int ry = 4 + bits(h, 46, 3);
            double dx = (double)(x - px) / rx;
            double dy = (double)(y - py) / ry;
            double dz = (double)(z - pz) / rx;
            // rough walls: the radius wobbles by a few percent per column
            double wob = 0.94 + 0.10 * (mix(seed, x >> 1, 0, z >> 1) >>> 56) / 15.0;
            if (dx * dx + dy * dy + dz * dz < wob) {
               return true;
            }
         }
      }

      return false;
   }

   /** Rare, small, ragged cave mouths (plus sinkholes): V6.19 punched holes almost everywhere. */
   static boolean mouth(Noise e, long seed, int x, int z) {
      // A ~30-block noise keeps openings small and scattered instead of one huge
      // 100-block-wide pit; a faster second octave frays their edges.
      double q = e.noise2(x * 0.030 + 11.0, z * 0.030 - 7.0);
      double r = e.noise2(x * 0.075 + 3.0, z * 0.075 + 19.0);
      return q + 0.18 * r > 0.48 || shaft(seed, x, z);
   }

   /** A vertical sinkhole: a real shaft from the surface down into the cave layer. */
   static boolean shaft(long seed, int x, int z) {
      int cx = Math.floorDiv(x, 80);
      int cz = Math.floorDiv(z, 80);
      for (int i = -1; i <= 1; i++) {
         for (int j = -1; j <= 1; j++) {
            long h = mix(seed ^ 0x53484AF7L, cx + i, 0, cz + j);
            if ((h & 7) != 0) {
               continue;
            }

            int px = (cx + i) * 80 + 20 + bits(h, 10, 40);
            int pz = (cz + j) * 80 + 20 + bits(h, 24, 40);
            int r = 2 + bits(h, 38, 3);
            int dx = x - px;
            int dz = z - pz;
            if (dx * dx + dz * dz <= r * r) {
               return true;
            }
         }
      }

      return false;
   }

   // ------------------------------------------------------------------ rock strata
   /**
    * Deep worlds get darker, banded stone: four-block strata that undulate slowly with x/z,
    * not a random salt-and-pepper of blocks. Only plain stone is replaced, so ores survive.
    */
   static int deepStone(int y, int x, int z, long seed) {
      int warp = bits(mix(seed ^ 0x53545241L, x >> 3, 0, z >> 3), 0, 5) - 2;
      int band = Math.floorMod((y + warp) >> 2, 4);
      int q = bits(mix(seed ^ 0x42414E44L, x >> 2, y >> 2, z >> 2), 33, 256);
      if (y < 16) {
         if (band == 0) return Blocks313.GRANITE;
         if (band == 2) return Blocks313.ANDESITE;
         if (q < 20) return Redstone319.DARK_PRISMARINE;
      } else if (y < 32) {
         if (band == 1 && q < 150) return Blocks313.DIORITE;
         if (band == 3 && q < 110) return Blocks313.GRANITE;
         if (q < 10) return Blocks313.ANDESITE;
      } else if (y < 44) {
         if (band == 0 && q < 70) return Blocks313.GRANITE;
      }

      return Blocks.STONE;
   }

   /** The rock lining a cave wall: horizontal stripes of a different stone every few blocks. */
   static int wallStone(int y, int x, int z, long seed) {
      int warp = bits(mix(seed ^ 0x57414C4CL, x >> 3, 0, z >> 3), 0, 3) - 1;
      int band = Math.floorMod((y + warp) >> 2, 6);
      if (y < 18) {
         return band < 2 ? Redstone319.POLISHED_ANDESITE : (band < 4 ? Blocks313.ANDESITE : Blocks313.GRANITE);
      }

      if (y < 34) {
         return band < 2 ? Blocks313.GRANITE : (band < 3 ? Blocks313.DIORITE : Blocks.STONE);
      }

      return band < 2 ? Blocks313.ANDESITE : Blocks.STONE;
   }

   static boolean rock(int id) {
      return id == Blocks.STONE || id == Blocks313.GRANITE || id == Blocks313.DIORITE || id == Blocks313.ANDESITE;
   }

   // ------------------------------------------------------------------ per-chunk finishing
   /** Called once for a freshly generated V6.21 chunk, after ores, trees and surface structures. */
   void finish(Chunk ch) {
      if (st.caveLayers) {
         strata(ch);
      }

      if (st.caveVer >= 2) {
         this.cliffs.finish(ch);
      } else {
         decorate(ch);
      }

      if (st.caveStructures) {
         structures(ch);
      }

      ch.computeHeightmap();
   }

   private void strata(Chunk ch) {
      for (int z = 0; z < 16; z++) {
         for (int x = 0; x < 16; x++) {
            for (int y = 5; y < 58; y++) {
               int id = ch.get(x, y, z);
               if (id != Blocks.STONE) {
                  continue;
               }

               int wx = ch.cx * 16 + x;
               int wz = ch.cz * 16 + z;
               int deep = deepStone(y, wx, wz, seed);
               if (deep != Blocks.STONE) {
                  ch.set(x, y, z, deep);
               }
            }
         }
      }

      // Expose the strata on the cave walls (skip the outer two columns: a wall built from
      // a neighbour's block would otherwise differ at the chunk seam).
      for (int z = 2; z < 14; z++) {
         for (int x = 2; x < 14; x++) {
            for (int y = 6; y < 56; y++) {
               int id = ch.get(x, y, z);
               if (!rock(id)) {
                  continue;
               }

               boolean exposed = ch.get(x - 1, y, z) == 0 || ch.get(x + 1, y, z) == 0
                  || ch.get(x, y, z - 1) == 0 || ch.get(x, y, z + 1) == 0
                  || ch.get(x, y - 1, z) == 0 || ch.get(x, y + 1, z) == 0;
               if (exposed) {
                  ch.set(x, y, z, wallStone(y, ch.cx * 16 + x, ch.cz * 16 + z, seed));
               }
            }
         }
      }
   }

   /** Stalactites, stalagmites, columns, rubble, ore pockets, crystals, mushrooms, glowcaps, pools. */
   private void decorate(Chunk ch) {
      float rich = st.caveRichness < 0.4F ? 0.4F : (st.caveRichness > 2.0F ? 2.0F : st.caveRichness);
      for (int z = 2; z < 14; z++) {
         for (int x = 2; x < 14; x++) {
            int wx = ch.cx * 16 + x;
            int wz = ch.cz * 16 + z;
            for (int y = 7; y < 56; y++) {
               if (ch.get(x, y, z) != 0) {
                  continue;
               }

               // an air block with a solid floor and a ceiling not too far away
               int floor = y - 1;
               if (floor < 6 || ch.get(x, floor, z) == 0) {
                  continue;
               }

               int ceil = y + 1;
               int gap = 0;
               while (ceil < 60 && ch.get(x, ceil, z) == 0 && gap < 9) {
                  ceil++;
                  gap++;
               }

               if (ceil >= 60 || gap < 2) {
                  continue;
               }

               long h = mix(seed ^ 0x4445434FL, wx, y, wz);
               int roll = bits(h, 4, 1000);
               int roll2 = bits(h, 20, 1000);
               int scale = (int)(1000.0F / rich);
               if (scale < 300) scale = 300;

               // hanging dripstone
               if (roll < 70 * 1000 / scale) {
                  int len = 1 + Math.min(4, gap - 1) * (roll2 % 3 == 0 ? 1 : 0) + 1;
                  len = Math.min(len, gap - 1);
                  for (int k = 0; k < len; k++) {
                     int id = k == len - 1 && y < 22 ? Blocks313.GRANITE : Blocks313.ANDESITE;
                     ch.set(x, ceil - 1 - k, z, id);
                  }
               }

               // a stalagmite growing up from the floor
               if (roll2 < 55 * 1000 / scale) {
                  int len = 1 + bits(h, 28, Math.max(1, Math.min(3, gap - 1)));
                  for (int k = 0; k < len && y + k < ceil; k++) {
                     ch.set(x, y + k, z, y < 20 ? Blocks313.DIORITE : Blocks.STONE);
                  }
               }

               // a full column where the cave is low
               if (gap <= 5 && roll < 18 * 1000 / scale) {
                  for (int k = 0; k < gap; k++) {
                     ch.set(x, y + k, z, Blocks313.ANDESITE);
                  }
               }

               // floor decorations
               if (roll >= 200 && roll < 200 + 90 * 1000 / scale) {
                  int fy = y;
                  if (y < 18 && bits(h, 32, 3) == 0) {
                     ch.set(x, fy, z, Redstone319.SEA_LANTERN);      // a glowing crystal
                  } else if (y < 34 && bits(h, 32, 5) == 0) {
                     ch.set(x, fy, z, Blocks.GLASS0 + 10);           // amethyst-purple glass
                  } else if (bits(h, 32, 4) == 0) {
                     ch.set(x, fy, z, Blocks.COBBLE);                 // fallen rubble
                  } else if (bits(h, 32, 3) == 0) {
                     ch.set(x, fy, z, Blocks313.MUSHROOM_RED);
                  } else {
                     ch.set(x, fy, z, Blocks.MOSSY_COBBLE);
                  }
               }

               // an exposed ore pocket in the floor / wall
               if (roll >= 500 && roll < 500 + 34 * 1000 / scale) {
                  int ore = y < 16 ? Blocks.DIAMOND_ORE : (y < 28 ? Blocks.GOLD_ORE : (y < 44 ? Blocks.IRON_ORE : Blocks.COAL_ORE));
                  for (int dz = 0; dz < 2; dz++) {
                     for (int dx = 0; dx < 2; dx++) {
                        if (rock(ch.get(x + dx, floor, z + dz))) {
                           ch.set(x + dx, floor, z + dz, ore);
                        }
                     }
                  }
               }

               // a glowcap in the ceiling: caves are no longer pitch black
               if (roll >= 760 && roll < 760 + 12 * 1000 / scale) {
                  ch.set(x, ceil, z, Redstone319.SEA_LANTERN);
               }

               // deep lava springs and quiet water pools, in coherent little patches
               if (st.caveLakes && roll >= 880 && roll < 880 + 22 * 1000 / scale) {
                  int fluid = y <= 12 ? Blocks.LAVA : Blocks.WATER;
                  for (int dz = -1; dz <= 1; dz++) {
                     for (int dx = -1; dx <= 1; dx++) {
                        if (dx * dx + dz * dz <= 2 && ch.get(x + dx, y, z + dz) == 0) {
                           ch.set(x + dx, y, z + dz, fluid);
                        }
                     }
                  }
               }

               // vines hanging from the ceiling of a lush grotto
               if (y > 30 && roll >= 950 && roll < 950 + 16 * 1000 / scale) {
                  ch.set(x, ceil - 1, z, Blocks313.VINE0 + 1);
               }

               // very rare monster room in a wide cave
               if (gap >= 4 && roll == 999) {
                  ch.set(x, y, z, Blocks.SPAWNER);
               }
            }
         }
      }
   }

   // ------------------------------------------------------------------ underground structures
   static final class Plan {
      boolean valid;
      int x;
      int z;
      int y;
      int type;
      int rx;
      int ry;
      int rz;
      long h;
   }

   /** A deterministic plan for one 112-block region. Cached by the owning CaveWorks. */
   static Plan plan(long seed, int rx, int rz) {
      long h = mix(seed ^ 0x4341564553L, rx, 0, rz);
      Plan p = new Plan();
      p.h = h;
      if ((h >>> 60) >= 10) {
         return p; // ~62% of regions have an underground site
      }

      p.valid = true;
      p.x = rx * REGION + 24 + bits(h, 8, 64);
      p.z = rz * REGION + 24 + bits(h, 20, 64);
      p.type = bits(h, 30, 6);
      int band = bits(h, 36, 3);
      p.y = band == 0 ? 14 + bits(h, 40, 12) : (band == 1 ? 27 + bits(h, 40, 14) : 41 + bits(h, 40, 12));
      p.rx = 10 + bits(h, 46, 6);
      p.rz = 10 + bits(h, 50, 6);
      p.ry = 4 + bits(h, 54, 3);
      return p;
   }

   /**
    * A site must be under the real ground, not floating in an ocean's water column: on a low
    * sea floor (h ~ 36) the absolute plan y would be above the surface. One cheap column sample
    * per cached site moves it down; the result is cached, so neighbouring chunks agree.
    */
   static int siteY(Gen gen, Plan p) {
      if (gen == null) return p.y;
      int h = gen.column(p.x, p.z).h;
      return Math.max(12, Math.min(p.y, h - 14));
   }

   private Plan cached(int rx, int rz) {
      long key = (long)rx << 32 ^ rz & 0xFFFFFFFFL;
      return plans.computeIfAbsent(key, k -> {
         Plan p = plan(seed, rx, rz);
         if (p.valid) {
            p.y = siteY(gen, p);
         }
         return p;
      });
   }

   private void structures(Chunk ch) {
      int margin = 36;
      int x0 = ch.cx * 16;
      int z0 = ch.cz * 16;
      int rx0 = Math.floorDiv(x0 - margin, REGION);
      int rx1 = Math.floorDiv(x0 + 15 + margin, REGION);
      int rz0 = Math.floorDiv(z0 - margin, REGION);
      int rz1 = Math.floorDiv(z0 + 15 + margin, REGION);
      for (int rx = rx0; rx <= rx1; rx++) {
         for (int rz = rz0; rz <= rz1; rz++) {
            Plan p = cached(rx, rz);
            if (p.valid) {
               stamp(ch, p);
            }
         }
      }
   }

   /** Carves the chamber and corridors, then builds the structure. Pure in world coordinates. */
   static void stamp(Chunk ch, Plan p) {
      int x0 = ch.cx * 16;
      int z0 = ch.cz * 16;
      int xa = Math.max(x0, p.x - p.rx - 22);
      int xb = Math.min(x0 + 15, p.x + p.rx + 22);
      int za = Math.max(z0, p.z - p.rz - 22);
      int zb = Math.min(z0 + 15, p.z + p.rz + 22);
      if (xa > xb || za > zb) {
         return;
      }

      int floorY = p.y - p.ry + 1;
      int ceilY = p.y + p.ry - 1;
      int ya = Math.max(6, floorY - 2);
      int yb = Math.min(250, ceilY + 3);

      int px = Math.max(3, p.rx / 2);
      int pz = Math.max(3, p.rz / 2);

      // pass 1: open a chamber with a flat floor and a rounded dome, plus four short
      // walkable corridors that reach out towards the natural tunnel network.
      for (int z = za; z <= zb; z++) {
         for (int x = xa; x <= xb; x++) {
            int dx = x - p.x;
            int dz = z - p.z;
            double h2 = (double)(dx * dx) / (p.rx * p.rx) + (double)(dz * dz) / (p.rz * p.rz);
            boolean corridor = (Math.abs(dz) <= 1 || Math.abs(dx) <= 1)
               && (Math.abs(dx) <= p.rx + 20 || Math.abs(dz) <= p.rz + 20);
            for (int y = ya; y <= yb; y++) {
               double dy = (double)(y - p.y) / p.ry;
               boolean inside = h2 + dy * dy <= 0.98
                  || (h2 <= 0.92 && y >= floorY + 1 && y <= ceilY - 1);
               boolean tube = corridor && y >= floorY + 1 && y <= floorY + 3;
               if (!inside && !tube) {
                  continue;
               }

               int lx = x - x0;
               int lz = z - z0;
               if (ch.get(lx, y, lz) != Blocks.BEDROCK && !Blocks.isFluid(ch.get(lx, y, lz))) {
                  ch.set(lx, y, lz, Blocks.AIR);
               }
            }
         }
      }

      // pass 2: the build itself
      int light = p.type == 1 || p.type == 5 ? Redstone319.SEA_LANTERN : Blocks.GLOWSTONE;
      for (int z = za; z <= zb; z++) {
         for (int x = xa; x <= xb; x++) {
            int dx = x - p.x;
            int dz = z - p.z;
            double h2 = (double)(dx * dx) / (p.rx * p.rx) + (double)(dz * dz) / (p.rz * p.rz);
            long h = mix(p.h, dx, dz, 3);
            int q = bits(h, 5, 256);
            int lx = x - x0;
            int lz = z - z0;
            boolean corridor = (Math.abs(dz) <= 1 || Math.abs(dx) <= 1)
               && (Math.abs(dx) <= p.rx + 20 || Math.abs(dz) <= p.rz + 20);

            // floor
            if (h2 <= 0.92) {
               int fb = floorBlock(p.type, q, dx, dz, p);
               if (fb != 0) {
                  ch.set(lx, floorY, lz, fb);
               }
            }

            // an inner colonnade and a ring of beams under the ceiling
            boolean pillar = Math.abs(dx) == px && Math.abs(dz) == pz;
            boolean beam = (Math.abs(dx) == px && Math.abs(dz) <= pz)
               || (Math.abs(dz) == pz && Math.abs(dx) <= px);
            if (pillar) {
               for (int y = floorY + 1; y < ceilY; y++) {
                  ch.set(lx, y, lz, pillarBlock(p.type, q));
               }
               ch.set(lx, floorY + 2, lz, light);
            } else if (beam && q < 190) {
               ch.set(lx, ceilY, lz, beamBlock(p.type, q));
            }

            // the centre: a pedestal, a loot chest, a light overhead, sometimes a spawner
            if (dx == 0 && dz == 0) {
               ch.set(lx, floorY + 1, lz, pedestal(p.type));
               ch.set(lx, floorY + 2, lz, Blocks.CHEST_LOOT);
               ch.set(lx, ceilY, lz, light);
            } else if ((dx == 2 && dz == 0 || dx == 0 && dz == 2) && (p.type == 2 || p.type == 3)) {
               ch.set(lx, floorY + 1, lz, Blocks.SPAWNER);
            } else if (h2 > 0.55 && h2 < 0.90 && q < 90) {
               detail(ch, lx, floorY, ceilY, lz, p.type, q);
            }

            // supports along the corridors
            if (corridor && ((dx + dz) & 3) == 0 && q < 170
               && (Math.abs(dx) > p.rx - 2 || Math.abs(dz) > p.rz - 2)) {
               ch.set(lx, floorY + 1, lz, p.type == 3 ? Blocks.LOG_OAK : Blocks.STONEBRICK);
               ch.set(lx, floorY + 2, lz, p.type == 3 ? Blocks.TORCH : Blocks.MOSSY_COBBLE);
            }
         }
      }
   }

   private static int floorBlock(int type, int q, int dx, int dz, Plan p) {
      switch (type) {
         case 0:
            return q < 34 ? Blocks.MOSSY_COBBLE : (q < 56 ? Redstone319.CRACKED_STONEBRICK : Blocks.STONEBRICK);
         case 1:
            return q < 20 ? Redstone319.SEA_LANTERN : (q < 90 ? Blocks.QUARTZ_BLOCK : Redstone319.PURPUR_BLOCK);
         case 2: {
            boolean pool = Math.abs(dx) <= 2 && Math.abs(dz) <= 2;
            return pool ? Blocks.LAVA : (q < 40 ? Blocks.OBSIDIAN : Blocks.COBBLE);
         }
         case 3:
            return (dx + dz & 3) == 0 ? Blocks.AIR : (q < 30 ? Blocks.GRAVEL : Blocks.PLANKS);
         case 4:
            return q < 46 ? Blocks313.MYCELIUM : (q < 76 ? Blocks313.PODZOL : Blocks313.MUSH_BLOCK_BROWN);
         default: {
            boolean water = Math.abs(dx) >= 2 && Math.abs(dz) >= 2 && Math.abs(dx) <= 5 && Math.abs(dz) <= 5;
            return water ? Blocks.WATER : (q < 50 ? Redstone319.PRISMARINE_BRICKS : Blocks.STONEBRICK);
         }
      }
   }

   private static int pillarBlock(int type, int q) {
      switch (type) {
         case 1: return q < 90 ? Redstone319.PURPUR_PILLAR : Blocks.QUARTZ_BLOCK;
         case 2: return q < 60 ? Blocks.OBSIDIAN : Blocks.MOSSY_COBBLE;
         case 3: return Blocks.LOG_OAK;
         case 4: return Blocks313.MUSH_STEM;
         case 5: return Redstone319.DARK_PRISMARINE;
         default: return q < 40 ? Redstone319.MOSSY_STONEBRICK : Blocks.STONEBRICK;
      }
   }

   private static int beamBlock(int type, int q) {
      switch (type) {
         case 1: return Redstone319.PURPUR_BLOCK;
         case 2: return Blocks.OBSIDIAN;
         case 3: return Blocks.PLANKS;
         case 4: return Blocks313.MUSH_BLOCK_RED;
         case 5: return Redstone319.PRISMARINE;
         default: return q < 70 ? Redstone319.CHISELED_STONEBRICK : Blocks.STONEBRICK;
      }
   }

   private static int pedestal(int type) {
      switch (type) {
         case 1: return Blocks.QUARTZ_BLOCK;
         case 2: return Blocks.OBSIDIAN;
         case 3: return Blocks.PLANKS;
         case 4: return Blocks313.MUSH_BLOCK_RED;
         case 5: return Redstone319.DARK_PRISMARINE;
         default: return Redstone319.CHISELED_STONEBRICK;
      }
   }

   /** The little things that make each site worth finding. */
   private static void detail(Chunk ch, int x, int floorY, int ceilY, int z, int type, int q) {
      switch (type) {
         case 0:
            ch.set(x, floorY + 1, z, q < 40 ? Blocks313.VINE0 + 1 : Blocks.MOSSY_COBBLE);
            break;
         case 1:
            ch.set(x, floorY + 1, z, q < 30 ? Redstone319.SEA_LANTERN : (q < 60 ? Blocks.GLASS0 + 10 : Blocks.GLASS0 + 9));
            if (q < 22) ch.set(x, ceilY, z, Blocks.GLASS0 + 2);
            break;
         case 2:
            if (q < 34) {
               ch.set(x, ceilY, z, Blocks.LAVA);
               ch.set(x, ceilY - 1, z, Blocks.LAVA);
            } else {
               ch.set(x, floorY + 1, z, Blocks.GLOWSTONE);
            }
            break;
         case 3:
            ch.set(x, floorY + 1, z, q < 50 ? Blocks.FENCE : Blocks.COAL_ORE);
            break;
         case 4:
            ch.set(x, floorY + 1, z, q < 40 ? Blocks313.MUSHROOM_RED : Blocks313.MUSH_BLOCK_RED);
            if (q < 25) ch.set(x, ceilY, z, Blocks.GLOWSTONE);
            break;
         default:
            ch.set(x, floorY + 1, z, q < 46 ? Blocks313.LILY_PAD : Redstone319.SEA_LANTERN);
            break;
      }
   }
}
