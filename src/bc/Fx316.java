package bc;

import java.util.Random;

/**
 * Update 3.16: the little things that make a place feel alive - particles that nobody asks for.
 * <ul>
 * <li>leaves that come off the trees and drift down (in the colour of the biome), by day;</li>
 * <li>fireflies over the grass on warm nights (small greenish lights that wander and pulse);</li>
 * <li>embers rising from lava (the Nether is full of them);</li>
 * <li>flame and smoke on torches;</li>
 * <li>dust kicked up by a sprinting player on dirt, sand, gravel and snow;</li>
 * <li>a splash when you jump into water, and bubbles when you are under it.</li>
 * </ul>
 * Everything is a few random look-ups per tick and has a ceiling on the number of particles it may add, so a big base costs nothing.
 * The particles themselves (their fading, shrinking, glowing) are in {@link Game.Particle}.
 */
final class Fx316 {
   private Fx316() {
   }

   /** Ambient particles are not added while this many are alive (explosions and break particles come first). */
   static final int CEILING = 1400;
   static boolean wasInWater;
   static double lastVy;

   /** Is it daylight (sun up) at this time of day? Tick 0 is sunrise, 12000 sunset. */
   static boolean day(long dayTime) {
      long t = Math.floorMod(dayTime, 24000L);
      return t < 12300L || t > 23700L;
   }

   /** Is it dark enough for fireflies? */
   static boolean night(long dayTime) {
      long t = Math.floorMod(dayTime, 24000L);
      return t > 13200L && t < 22800L;
   }

   static Game.Particle make(Game g, double x, double y, double z) {
      Game.Particle p = new Game.Particle();
      p.x = (float)x;
      p.y = (float)y;
      p.z = (float)z;
      p.shade = 1.0F;
      return p;
   }

   /** The particle shows one texel of the white particle tile (tile 64). */
   static void plain(Game.Particle p) {
      p.u = (Tex.tx(64) + 1) / (float)Tex.PX;
      p.v = (Tex.ty(64) + 1) / (float)Tex.PX;
   }

   static void tick(Game g) {
      World w = g.world;
      Player pl = g.player;
      if (w == null || pl == null || g.particles.size() > CEILING) {
         return;
      }

      Random r = g.rnd;
      long time = w.dayTime;
      if (w.dim == Dim.OVERWORLD) {
         if (day(time)) {
            leaves(g, w, pl, r);
         }

         if (night(time)) {
            fireflies(g, w, pl, r);
         }
      }

      embers(g, w, pl, r);
      torches(g, w, r);
      sprintDust(g, w, pl, r);
      water(g, w, pl, r);
   }

   // ------------------------------------------------------------------ leaves
   static void leaves(Game g, World w, Player pl, Random r) {
      if (g.tickCount % 3 != 0) {
         return;
      }

      for (int k = 0; k < 8; k++) {
         int x = (int)Math.floor(pl.x) + r.nextInt(41) - 20;
         int z = (int)Math.floor(pl.z) + r.nextInt(41) - 20;
         int y = (int)Math.floor(pl.y) + r.nextInt(21) - 6;
         if (y < 2 || y > 250 || !w.isLoaded(x, z)) {
            continue;
         }

         int id = w.getBlock(x, y, z);
         if (!Blocks.isLeaves(id) || w.getBlock(x, y - 1, z) != 0 || w.getBlock(x, y - 2, z) != 0) {
            continue;
         }

         spawnLeaf(g, w, r, x, y, z, id);
         return;
      }
   }

   static Game.Particle spawnLeaf(Game g, World w, Random r, int x, int y, int z, int id) {
      Game.Particle p = make(g, x + 0.2 + r.nextDouble() * 0.6, y - 0.1, z + 0.2 + r.nextDouble() * 0.6);
      int tile = Blocks.tiles[id][2];
      int tx = Tex.tx(tile);
      int ty = Tex.ty(tile);
      int px = tx + 2 + r.nextInt(12);
      int py = ty + 2 + r.nextInt(12);
      for (int tries = 0; tries < 6 && Tex.atlasPixels != null && (Tex.atlasPixels[py * Tex.PX + px] >>> 24) < 160; tries++) {
         px = tx + 2 + r.nextInt(12);
         py = ty + 2 + r.nextInt(12);
      }

      p.u = px / (float)Tex.PX;
      p.v = py / (float)Tex.PX;
      Chunk c = w.chunkAt(x, z);
      int rgb = c != null ? c.foliageColor[(z & 15) * 16 + (x & 15)] : 0;
      if (rgb == 0) {
         rgb = 0x6EA030;
      }

      p.r = (rgb >> 16 & 255) / 255.0F;
      p.g = (rgb >> 8 & 255) / 255.0F;
      p.b = (rgb & 255) / 255.0F;
      p.size = 0.045F + r.nextFloat() * 0.03F;
      p.vx = (r.nextFloat() - 0.5F) * 0.02F;
      p.vz = (r.nextFloat() - 0.5F) * 0.02F;
      p.vy = -0.01F - r.nextFloat() * 0.01F;
      p.grav = 0.0006F;
      p.sway = 0.0045F;
      p.phase = r.nextFloat() * 6.28F;
      p.life = 150 + r.nextInt(120);
      p.fade = 1;
      p.shade = 0.9F;
      g.particles.add(p);
      return p;
   }

   // ------------------------------------------------------------------ fireflies
   static int fireflies(Game g) {
      int n = 0;
      for (Game.Particle p : g.particles) {
         if (p.kind == 1) {
            n++;
         }
      }

      return n;
   }

   static void fireflies(Game g, World w, Player pl, Random r) {
      if (g.tickCount % 4 != 0 || r.nextInt(3) != 0 || fireflies(g) >= 14) {
         return;
      }

      int x = (int)Math.floor(pl.x) + r.nextInt(25) - 12;
      int z = (int)Math.floor(pl.z) + r.nextInt(25) - 12;
      if (!w.isLoaded(x, z)) {
         return;
      }

      int y0 = Math.min(250, (int)Math.floor(pl.y) + 6);
      for (int y = y0; y > Math.max(2, y0 - 14); y--) {
         int below = w.getBlock(x, y - 1, z);
         if (below == Blocks.GRASS && w.getBlock(x, y, z) == 0 && w.getBlock(x, y + 1, z) == 0) {
            spawnFirefly(g, r, x + 0.5 + (r.nextDouble() - 0.5) * 0.8, y + 0.8 + r.nextDouble() * 1.6, z + 0.5 + (r.nextDouble() - 0.5) * 0.8);
            return;
         }

         if (below != 0 && Blocks.solid[below]) {
            return;
         }
      }
   }

   static Game.Particle spawnFirefly(Game g, Random r, double x, double y, double z) {
      Game.Particle p = make(g, x, y, z);
      plain(p);
      p.glow = true;
      p.kind = 1;
      p.r = 0.78F;
      p.g = 1.0F;
      p.b = 0.30F;
      p.size = 0.035F;
      p.grav = 0.0F;
      p.vx = (r.nextFloat() - 0.5F) * 0.02F;
      p.vy = (r.nextFloat() - 0.5F) * 0.01F;
      p.vz = (r.nextFloat() - 0.5F) * 0.02F;
      p.sway = 0.0016F;
      p.phase = r.nextFloat() * 6.28F;
      p.life = 180 + r.nextInt(220);
      p.fade = 1;
      p.twinkle = 1;
      g.particles.add(p);
      return p;
   }

   // ------------------------------------------------------------------ embers
   static void embers(Game g, World w, Player pl, Random r) {
      for (int k = 0; k < 3; k++) {
         int x = (int)Math.floor(pl.x) + r.nextInt(37) - 18;
         int z = (int)Math.floor(pl.z) + r.nextInt(37) - 18;
         int top = Math.min(250, (int)Math.floor(pl.y) + 3);
         if (!w.isLoaded(x, z) || w.getBlock(x, top, z) != 0) {
            continue;
         }

         // look down the column to the first thing that is not air: lava lakes lie far below you in the Nether
         for (int y = top - 1; y > Math.max(1, top - 50); y--) {
            int id = w.getBlock(x, y, z);
            if (id == 0) {
               continue;
            }

            if (Blocks.isLava(id) && r.nextInt(3) == 0) {
               spawnEmber(g, r, x + r.nextDouble(), y + 1.02, z + r.nextDouble());
               return;
            }

            break;
         }
      }
   }

   static Game.Particle spawnEmber(Game g, Random r, double x, double y, double z) {
      Game.Particle p = make(g, x, y, z);
      plain(p);
      p.glow = true;
      p.r = 1.0F;
      p.g = 0.42F + r.nextFloat() * 0.25F;
      p.b = 0.08F;
      p.size = 0.025F + r.nextFloat() * 0.025F;
      p.vx = (r.nextFloat() - 0.5F) * 0.03F;
      p.vz = (r.nextFloat() - 0.5F) * 0.03F;
      p.vy = 0.03F + r.nextFloat() * 0.05F;
      p.grav = -0.0012F;
      p.sway = 0.003F;
      p.phase = r.nextFloat() * 6.28F;
      p.life = 24 + r.nextInt(30);
      p.fade = 3;
      g.particles.add(p);
      return p;
   }

   // ------------------------------------------------------------------ torches
   static void torches(Game g, World w, Random r) {
      if (g.rend == null || g.tickCount % 3 != 0) {
         return;
      }

      for (float[] l : g.rend.placedLights) {
         int id = (int)l[3];
         if (id != Blocks.TORCH && id != Blocks.WALL_TORCH) {
            continue;
         }

         int n = r.nextInt(6);
         if (n < 2) {
            g.flameParticle(l[0] + (r.nextDouble() - 0.5) * 0.08, l[1] + 0.22, l[2] + (r.nextDouble() - 0.5) * 0.08, false);
         } else if (n == 2) {
            g.flameParticle(l[0], l[1] + 0.3, l[2], true);
         }
      }
   }

   // ------------------------------------------------------------------ dust
   static boolean dusty(int id) {
      return id == Blocks.DIRT || id == Blocks.SAND || id == Blocks.GRAVEL || id == Blocks.GRASS || id == Blocks.SNOW;
   }

   static void sprintDust(Game g, World w, Player pl, Random r) {
      if (!pl.sprinting || !pl.onGround || pl.spectator || g.tickCount % 3 != 0 || Math.hypot(pl.vx, pl.vz) < 0.1) {
         return;
      }

      int bx = (int)Math.floor(pl.x);
      int bz = (int)Math.floor(pl.z);
      int by = (int)Math.floor(pl.y - 0.05);
      int id = w.getBlock(bx, by, bz);
      if (!dusty(id)) {
         return;
      }

      int tileId = id == Blocks.GRASS ? Blocks.DIRT : id;
      int tile = Blocks.tiles[tileId][0];
      int tx = Tex.tx(tile);
      int ty = Tex.ty(tile);
      for (int i = 0; i < 2; i++) {
         Game.Particle p = make(g, pl.x + (r.nextDouble() - 0.5) * 0.5, pl.y + 0.05, pl.z + (r.nextDouble() - 0.5) * 0.5);
         p.u = (tx + r.nextInt(12)) / (float)Tex.PX;
         p.v = (ty + r.nextInt(12)) / (float)Tex.PX;
         p.size = 0.035F + r.nextFloat() * 0.03F;
         p.vx = (float)(-pl.vx * 0.3 + (r.nextFloat() - 0.5F) * 0.04);
         p.vz = (float)(-pl.vz * 0.3 + (r.nextFloat() - 0.5F) * 0.04);
         p.vy = 0.04F + r.nextFloat() * 0.05F;
         p.grav = 0.03F;
         p.life = 8 + r.nextInt(8);
         p.fade = 3;
         p.shade = 0.8F;
         g.particles.add(p);
      }
   }

   // ------------------------------------------------------------------ water
   static void water(Game g, World w, Player pl, Random r) {
      if (pl.spectator) {
         wasInWater = false;
         return;
      }

      if (pl.inWater && !wasInWater && lastVy < -0.25) {
         int n = 10 + (int)Math.min(14, -lastVy * 24.0);
         for (int i = 0; i < n; i++) {
            double a = r.nextDouble() * Math.PI * 2.0;
            double d = r.nextDouble() * 0.5;
            Game.Particle p = make(g, pl.x + Math.cos(a) * d, pl.y + 0.9, pl.z + Math.sin(a) * d);
            plain(p);
            p.r = 0.80F;
            p.g = 0.90F;
            p.b = 1.0F;
            p.size = 0.035F + r.nextFloat() * 0.035F;
            p.vx = (float)(Math.cos(a) * (0.03 + r.nextDouble() * 0.06));
            p.vz = (float)(Math.sin(a) * (0.03 + r.nextDouble() * 0.06));
            p.vy = 0.14F + r.nextFloat() * 0.22F;
            p.grav = 0.045F;
            p.life = 12 + r.nextInt(8);
            p.fade = 1;
            g.particles.add(p);
         }
      }

      if (pl.eyeInWater && r.nextInt(14) == 0) {
         double a = r.nextDouble() * Math.PI * 2.0;
         Game.Particle p = make(g, pl.x + Math.cos(a) * 0.25, pl.y + pl.eye - 0.25, pl.z + Math.sin(a) * 0.25);
         plain(p);
         p.glow = true;
         p.r = 0.70F;
         p.g = 0.85F;
         p.b = 1.0F;
         p.size = 0.025F + r.nextFloat() * 0.025F;
         p.vy = 0.025F + r.nextFloat() * 0.02F;
         p.vx = (r.nextFloat() - 0.5F) * 0.01F;
         p.vz = (r.nextFloat() - 0.5F) * 0.01F;
         p.grav = -0.001F;
         p.sway = 0.002F;
         p.phase = r.nextFloat() * 6.28F;
         p.life = 30 + r.nextInt(30);
         p.fade = 3;
         g.particles.add(p);
      }

      wasInWater = pl.inWater;
      lastVy = pl.vy;
   }
}
