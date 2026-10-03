package bc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Fire (Update 3.7), modelled on Minecraft 1.12's BlockFire.
 *
 * Blocks carry no metadata in Blockcraft, so the one fire block (id 194) has its age kept here: a map from
 * position to {age, next update tick}. The map is kept in sync by World.setBlock and refilled from the chunk data when a
 * chunk is loaded, so after a restart every fire simply starts young again.
 *
 *  - every fire block updates every 30-39 ticks (age grows by 0 or 1 per update, up to 15)
 *  - a fire with nothing flammable around it dies once it is older than 3 (or has no solid block below it)
 *  - it tries to burn the six neighbouring blocks away (or turn them into more fire) with a chance that depends on how
 *    flammable they are, and to jump to nearby air next to flammable blocks - further and more rarely upwards
 *  - lava sets fire to flammable blocks next to it from time to time
 *  - /firespread off freezes everything: fire stays where it is and neither burns nor spreads (like gamerule doFireTick)
 */
final class Fire {
   final World w;
   final Random rnd = new Random();
   /** packed position -> {age, tick of the next update} */
   final HashMap<Long, long[]> cells = new HashMap<>();
   boolean spread = true;

   Fire(World w) {
      this.w = w;
   }

   // ------------------------------------------------------------------ what burns

   /** How eagerly fire jumps to a block next to this one (Minecraft's "encouragement"). */
   static int encouragement(int id) {
      if (id == 5 || id == 113 || id == 118 || id >= 93 && id <= 96 || id == Blocks.SLAB_TOP0 || id >= Blocks.STAIR_TOP0 && id <= Blocks.STAIR_TOP0 + 3) {
         return 5; // oak planks, slab, fence, stairs
      } else if (id >= 9 && id <= 11) {
         return 5; // logs
      } else if (id >= 12 && id <= 14 || id >= 34 && id <= 49 || id == 160) {
         return 30; // leaves, wool, bookshelf
      } else if (id == 132) {
         return 15; // TNT
      } else {
         return id == 29 || id == 30 || id == 31 || id == 158 ? 60 : 0; // tall grass, flowers, hay bale
      }
   }

   /** How likely a block is to be consumed by neighbouring fire (Minecraft's "flammability"); 0 = does not burn. */
   static int flammability(int id) {
      if (id == 5 || id == 113 || id == 118 || id >= 93 && id <= 96 || id == Blocks.SLAB_TOP0 || id >= Blocks.STAIR_TOP0 && id <= Blocks.STAIR_TOP0 + 3) {
         return 20;
      } else if (id >= 9 && id <= 11) {
         return 5;
      } else if (id >= 12 && id <= 14 || id >= 34 && id <= 49) {
         return 60;
      } else if (id == 160 || id == 158) {
         return 20;
      } else if (id == 132 || id == 29 || id == 30 || id == 31) {
         return 100;
      } else {
         return 0;
      }
   }

   /** A full block whose top face fire can stand on. */
   static boolean solidTop(int id) {
      return id != 0 && Blocks.solid[id] && Blocks.kind[id] == 0 && !Blocks.isFluid(id);
   }

   boolean neighborCatches(int x, int y, int z) {
      return flammability(w.getBlock(x + 1, y, z)) > 0
         || flammability(w.getBlock(x - 1, y, z)) > 0
         || flammability(w.getBlock(x, y + 1, z)) > 0
         || flammability(w.getBlock(x, y - 1, z)) > 0
         || flammability(w.getBlock(x, y, z + 1)) > 0
         || flammability(w.getBlock(x, y, z - 1)) > 0;
   }

   /** For an empty cell: the best encouragement among its neighbours (0 when the cell is not empty). */
   int neighborEncouragement(int x, int y, int z) {
      if (w.getBlock(x, y, z) != 0) {
         return 0;
      }

      int best = 0;
      for (int i = 0; i < 6; i++) {
         best = Math.max(best, encouragement(w.getBlock(x + World.DX[i], y + World.DY[i], z + World.DZ[i])));
      }

      return best;
   }

   /** Fire may be lit in an empty cell that has a solid block below it or something flammable next to it. */
   public boolean canPlace(int x, int y, int z) {
      return y >= 1 && y <= 254 && w.getBlock(x, y, z) == 0 && (solidTop(w.getBlock(x, y - 1, z)) || neighborCatches(x, y, z));
   }

   /** Lights a fire (flint and steel, lava, spreading). Returns false if it cannot stand there. */
   public boolean ignite(int x, int y, int z) {
      return this.canPlace(x, y, z) && w.setBlock(x, y, z, Blocks.FIRE);
   }

   // ------------------------------------------------------------------ registry (kept in sync by World)

   void added(int x, int y, int z) {
      this.cells.putIfAbsent(World.pack(x, y, z), new long[]{0L, w.tickCount + 30 + this.rnd.nextInt(10)});
   }

   void removed(int x, int y, int z) {
      this.cells.remove(World.pack(x, y, z));
   }

   /** Called when a chunk comes into the world: every fire block in it starts burning (age 0). */
   void scanChunk(Chunk c) {
      byte fire = (byte)Blocks.FIRE;
      for (int i = 0; i < c.blocks.length; i++) {
         if (c.blocks[i] == fire) {
            this.added(c.cx * 16 + (i & 15), i >> 8, c.cz * 16 + (i >> 4 & 15));
         }
      }
   }

   public int count() {
      return this.cells.size();
   }

   /** Age (0-15) of the fire at this position, or -1 if there is none. */
   public int age(int x, int y, int z) {
      long[] st = this.cells.get(World.pack(x, y, z));
      return st == null ? -1 : (int)st[0];
   }

   /** Block update of a fire block: it goes out when it has nothing to stand on or to feed on any more. */
   void check(int x, int y, int z) {
      if (w.getBlock(x, y, z) == Blocks.FIRE && !solidTop(w.getBlock(x, y - 1, z)) && !this.neighborCatches(x, y, z)) {
         w.setBlock(x, y, z, 0);
      }
   }

   // ------------------------------------------------------------------ ticking

   void tick() {
      if (!this.cells.isEmpty()) {
         long now = w.tickCount;
         ArrayList<Long> due = null;
         for (Map.Entry<Long, long[]> e : this.cells.entrySet()) {
            if (e.getValue()[1] <= now) {
               if (due == null) {
                  due = new ArrayList<>();
               }

               due.add(e.getKey());
            }
         }

         if (due != null) {
            for (long key : due) {
               long[] st = this.cells.get(key);
               if (st != null && st[1] <= now) {
                  this.update(key, st);
               }
            }
         }
      }

      this.lavaSparks();
   }

   void update(long key, long[] st) {
      int x = World.ux(key);
      int y = World.uy(key);
      int z = World.uz(key);
      if (!w.isLoaded(x, z) || w.getBlock(x, y, z) != Blocks.FIRE) {
         this.cells.remove(key);
         return;
      }

      st[1] = w.tickCount + 30 + this.rnd.nextInt(10);
      if (!this.spread) {
         return; // /firespread off: a frozen fire does not even get older
      }

      int below = w.getBlock(x, y - 1, z);
      boolean feeds = this.neighborCatches(x, y, z);
      if (!solidTop(below) && !feeds) {
         w.setBlock(x, y, z, 0);
         return;
      }

      int age = (int)st[0];
      // Update 3.8: rain puts fires out (Minecraft: 20% + 3% per age step on every update)
      if (w.weather.rainingAt(x, y, z) && this.rnd.nextFloat() < 0.2F + age * 0.03F) {
         w.setBlock(x, y, z, 0);
         return;
      }

      if (age < 15) {
         age += this.rnd.nextInt(3) / 2;
         st[0] = age;
      }

      if (!feeds) {
         if (!solidTop(below) || age > 3) {
            w.setBlock(x, y, z, 0);
         }

         return;
      }

      if (flammability(below) == 0 && age == 15 && this.rnd.nextInt(4) == 0) {
         w.setBlock(x, y, z, 0);
         return;
      }

      this.tryCatch(x + 1, y, z, 300, age);
      this.tryCatch(x - 1, y, z, 300, age);
      this.tryCatch(x, y - 1, z, 250, age);
      this.tryCatch(x, y + 1, z, 250, age);
      this.tryCatch(x, y, z - 1, 300, age);
      this.tryCatch(x, y, z + 1, 300, age);

      for (int dx = -1; dx <= 1; dx++) {
         for (int dz = -1; dz <= 1; dz++) {
            for (int dy = -1; dy <= 4; dy++) {
               if (dx != 0 || dy != 0 || dz != 0) {
                  int chance = 100;
                  if (dy > 1) {
                     chance += (dy - 1) * 100;
                  }

                  int nx = x + dx;
                  int ny = y + dy;
                  int nz = z + dz;
                  if (ny >= 0 && ny <= 255 && w.isLoaded(nx, nz)) {
                     int enc = this.neighborEncouragement(nx, ny, nz);
                     if (enc > 0) {
                        int odds = (enc + 40 + 14) / (age + 30); // 14 = "normal" difficulty in Minecraft
                        if (odds > 0 && this.rnd.nextInt(chance) <= odds) {
                           this.setFire(nx, ny, nz, Math.min(15, age + this.rnd.nextInt(5) / 4));
                        }
                     }
                  }
               }
            }
         }
      }
   }

   void setFire(int x, int y, int z, int age) {
      if (w.setBlock(x, y, z, Blocks.FIRE)) {
         long[] st = this.cells.get(World.pack(x, y, z));
         if (st != null) {
            st[0] = age;
         }
      }
   }

   /** The block at (x, y, z) may be burnt up by the fire next to it: it turns into fire or into nothing (TNT blows up). */
   void tryCatch(int x, int y, int z, int chance, int age) {
      int id = w.getBlock(x, y, z);
      int flam = flammability(id);
      if (flam > 0 && this.rnd.nextInt(chance) < flam) {
         if (this.rnd.nextInt(age + 10) < 5) {
            this.setFire(x, y, z, Math.min(age + this.rnd.nextInt(5) / 4, 15));
         } else {
            w.setBlock(x, y, z, 0);
         }

         if (id == Blocks.TNT) {
            w.primeTnt(x, y, z, 80);
         }
      }
   }

   // ------------------------------------------------------------------ lava

   /** Lava near the player sets fire to flammable blocks next to it, now and then. */
   void lavaSparks() {
      Game g = w.game;
      if (g == null || g.player == null || !this.spread) {
         return;
      }

      int px = (int)Math.floor(g.player.x);
      int pz = (int)Math.floor(g.player.z);
      for (int i = 0; i < 6; i++) {
         int x = px + this.rnd.nextInt(49) - 24;
         int z = pz + this.rnd.nextInt(49) - 24;
         Chunk c = w.chunkAt(x, z);
         if (c != null) {
            int y = c.hm[(z & 15) * 16 + (x & 15)];
            if (Blocks.isLava(w.getBlock(x, y, z)) && this.rnd.nextInt(5) == 0) {
               this.igniteAround(x, y, z);
            }
         }
      }
   }

   /** One lava "random tick" (Minecraft's BlockStaticLiquid): look for something flammable close to the lava block. */
   void igniteAround(int x, int y, int z) {
      int tries = this.rnd.nextInt(3);
      if (tries > 0) {
         for (int n = 0; n < tries; n++) {
            x += this.rnd.nextInt(3) - 1;
            y++;
            z += this.rnd.nextInt(3) - 1;
            int b = w.getBlock(x, y, z);
            if (b == 0) {
               if (this.neighborCatches(x, y, z)) {
                  w.setBlock(x, y, z, Blocks.FIRE);
                  return;
               }
            } else if (Blocks.solid[b]) {
               return;
            }
         }
      } else {
         for (int n = 0; n < 3; n++) {
            int nx = x + this.rnd.nextInt(3) - 1;
            int nz = z + this.rnd.nextInt(3) - 1;
            if (w.getBlock(nx, y + 1, nz) == 0 && flammability(w.getBlock(nx, y, nz)) > 0) {
               w.setBlock(nx, y + 1, nz, Blocks.FIRE);
            }
         }
      }
   }

   // ------------------------------------------------------------------ looks

   /** Smoke and sparks above the fires close to the player (client side only, no effect on the world). */
   void particles(Game g) {
      if (this.cells.isEmpty() || g.player == null || w.tickCount % 3 != 0 || g.particles.size() > 500) {
         return;
      }

      int spawned = 0;
      for (long key : this.cells.keySet()) {
         double dx = World.ux(key) + 0.5 - g.player.x;
         double dz = World.uz(key) + 0.5 - g.player.z;
         if (dx * dx + dz * dz < 576.0) {
            if (this.rnd.nextInt(3) == 0) {
               g.flameParticle(World.ux(key) + 0.2 + this.rnd.nextDouble() * 0.6, World.uy(key) + 0.4 + this.rnd.nextDouble() * 0.5, World.uz(key) + 0.2 + this.rnd.nextDouble() * 0.6, this.rnd.nextInt(4) == 0);
               if (++spawned >= 8) {
                  return;
               }
            }
         }
      }
   }
}
