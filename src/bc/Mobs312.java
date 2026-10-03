package bc;

import bc.Mobs.Mob;
import java.util.Random;

/**
 * Update 3.12: the minds of the new mobs. The Nether: zombie pigmen (calm until you hit one, then the whole group comes), ghasts (float
 * about and throw fireballs), blazes (hover and shoot bursts of small fireballs), magma cubes (hop at you, split into smaller ones).
 * {@link Mobs#tickMob} asks {@link #think} what the mob wants this tick; walking, falling and collisions stay with Mobs.
 */
final class Mobs312 {
   private Mobs312() {
   }

   /** What a mob wants to do this tick: walk (moving, at speed times the type's speed), look somewhere (NaN = leave it). */
   static final class Intent {
      boolean moving;
      double speed = 1.0;
      float headYaw = Float.NaN;
      float headPitch = Float.NaN;
   }

   /** Something a ghast or a blaze shot. */
   static final class Fireball {
      double x;
      double y;
      double z;
      double px;
      double py;
      double pz;
      double vx;
      double vy;
      double vz;
      int life;
      /** The big ghast kind (blows a hole) or the small blaze kind (sets things alight). */
      boolean big;
   }

   static boolean peaceful(int type) {
      return type == Mobs.VILLAGER || type == Mobs.IRON_GOLEM || type == Mobs.PARROT;
   }

   static boolean fireproof(int type) {
      return type == Mobs.ZOMBIE_PIGMAN || type == Mobs.GHAST || type == Mobs.BLAZE || type == Mobs.MAGMA_CUBE || type == Mobs.ENDER_DRAGON;
   }

   /** Mobs that float: no gravity, they steer their height ({@link Mobs#flyVy}). */
   static boolean flies(int type) {
      return type == Mobs.GHAST || type == Mobs.BLAZE || type == Mobs.ENDER_DRAGON || type == Mobs.PARROT;
   }

   static int magmaHealth(int size) {
      return size >= 3 ? 16 : (size == 2 ? 6 : 2);
   }

   static void setSize(Mob m, int size) {
      m.size = size;
      m.scale = size;
      m.health = magmaHealth(size);
      m.setPos(m.x, m.y, m.z);
   }

   /** A mob has just been made. */
   static void born(Mob m, Random r) {
      if (m.type == Mobs.MAGMA_CUBE) {
         setSize(m, 3);
      }
   }

   // ------------------------------------------------------------------ thinking
   static Intent think(Mobs ms, Mob m, World w, Player p, float day) {
      Intent in = new Intent();
      boolean target = ms.targetable(p);
      double dx = p.x - m.x;
      double dz = p.z - m.z;
      double dy = p.y + 0.9 - (m.y + Mobs.H[m.type] * m.scale * 0.5);
      double d2 = Math.sqrt(dx * dx + dz * dz);
      double d3 = Math.sqrt(d2 * d2 + dy * dy);
      switch (m.type) {
         case Mobs.ZOMBIE_PIGMAN:
            pigman(ms, m, target, dx, dz, dy, d2, d3, in);
            break;
         case Mobs.GHAST:
            ghast(ms, m, w, p, target, dx, dz, dy, d2, d3, in);
            break;
         case Mobs.BLAZE:
            blaze(ms, m, w, p, target, dx, dz, dy, d2, d3, in);
            break;
         case Mobs.MAGMA_CUBE:
            magma(ms, m, target, dx, dz, dy, d2, d3, in);
            break;
         default:
            break;
      }

      return in;
   }

   private static float pitchTo(double dy, double d2) {
      return (float)(-Math.toDegrees(Math.atan2(dy, d2)));
   }

   /** Calm and wandering - until somebody hits one: then it (and every pigman near it) hunts the player for 20 seconds. */
   private static void pigman(Mobs ms, Mob m, boolean target, double dx, double dz, double dy, double d2, double d3, Intent in) {
      if (target && m.angry > 0 && d3 < 40.0) {
         m.hasTarget = true;
         m.dirYaw = Mobs.angleTo(dx, dz);
         in.headYaw = m.dirYaw;
         in.headPitch = pitchTo(dy, d2);
         in.moving = d2 > 0.8;
         in.speed = 1.55;
         if (d2 < 1.4 && Math.abs(dy) < 1.8 && m.cd <= 0) {
            m.cd = 20;
            m.attackAnim = 8;
            ms.g.damagePlayer(5, m.x, m.z);
         }
      } else {
         ms.wanderAI(m);
         in.moving = m.moving;
         in.speed = 0.55;
      }

      if (m.cd > 0) {
         m.cd--;
      }
   }

   /** A flyer must not fly into the ceiling or the floor: it steers away from whatever is a block above or below. */
   private static void keepOffTerrain(Mobs ms, World w, Mob m) {
      int x = (int)Math.floor(m.x);
      int z = (int)Math.floor(m.z);
      int top = (int)Math.floor(m.y + Mobs.H[m.type] * m.scale) + 1;
      int bottom = (int)Math.floor(m.y) - 1;
      if (Blocks.solid[w.getBlock(x, top, z)] || top > 118) {
         ms.flyVy = Math.min(ms.flyVy, -0.06);
      } else if (Blocks.solid[w.getBlock(x, bottom, z)] || Blocks.isLava(w.getBlock(x, bottom, z)) || bottom < 36 && w.dim == Dim.NETHER) {
         ms.flyVy = Math.max(ms.flyVy, 0.06);
      }
   }

   private static void ghast(Mobs ms, Mob m, World w, Player p, boolean target, double dx, double dz, double dy, double d2, double d3, Intent in) {
      boolean sees = target && d3 < 64.0 && ms.hasLOS(w, m.x, m.y + 2.0, m.z, p.x, p.y + 1.0, p.z);
      if (sees) {
         m.hasTarget = true;
         m.dirYaw = Mobs.angleTo(dx, dz);
         in.headYaw = m.dirYaw;
         in.headPitch = pitchTo(dy, d2);
         // keeps 12 to 26 blocks away, floats a little above the player
         in.moving = d2 > 26.0 || d2 < 12.0;
         if (d2 < 12.0) {
            m.dirYaw += 180.0F;
         }

         ms.flyVy = Math.max(-0.08, Math.min(0.08, (p.y + 4.0 - m.y) * 0.04));
         if (m.fuse > 0) {
            // the mouth is open, a fireball is being charged
            m.fuse--;
            m.attackAnim = Math.max(m.attackAnim, 2);
            if (m.fuse == 0) {
               shoot(ms, m, p.x, p.y + 1.0, p.z, 0.55, true);
               m.cd = 70 + ms.rnd.nextInt(50);
            }
         } else if (m.cd > 0) {
            m.cd--;
         } else {
            m.fuse = 20;
         }
      } else {
         m.fuse = 0;
         ms.wanderAI(m);
         in.moving = m.moving;
         in.speed = 0.7;
         ms.flyVy = Math.sin((m.age + m.hashCode() % 100) * 0.05) * 0.03;
      }

      keepOffTerrain(ms, w, m);
   }

   private static void blaze(Mobs ms, Mob m, World w, Player p, boolean target, double dx, double dz, double dy, double d2, double d3, Intent in) {
      boolean sees = target && d3 < 32.0 && ms.hasLOS(w, m.x, m.y + 1.2, m.z, p.x, p.y + 1.0, p.z);
      if (sees) {
         m.hasTarget = true;
         m.dirYaw = Mobs.angleTo(dx, dz);
         in.headYaw = m.dirYaw;
         in.headPitch = pitchTo(dy, d2);
         in.moving = d2 > 12.0 || d2 < 4.0;
         if (d2 < 4.0) {
            m.dirYaw += 180.0F;
         }

         ms.flyVy = Math.max(-0.1, Math.min(0.1, (p.y + 1.5 - m.y) * 0.06));
         if (m.cd > 0) {
            m.cd--;
         } else if (m.fuse == 0) {
            m.fuse = 18;
            m.cd = 90 + ms.rnd.nextInt(30);
         }

         if (m.fuse > 0) {
            m.fuse--;
            m.attackAnim = Math.max(m.attackAnim, 2);
            if (m.fuse % 6 == 0) {
               shoot(ms, m, p.x, p.y + 1.0, p.z, 0.85, false);
            }
         }
      } else {
         m.fuse = 0;
         ms.wanderAI(m);
         in.moving = m.moving;
         in.speed = 0.7;
         ms.flyVy = Math.sin((m.age + m.hashCode() % 100) * 0.07) * 0.04;
      }

      keepOffTerrain(ms, w, m);
   }

   private static void magma(Mobs ms, Mob m, boolean target, double dx, double dz, double dy, double d2, double d3, Intent in) {
      int size = Math.max(1, m.size);
      boolean near = target && d3 < 16.0;
      if (m.cd > 0) {
         m.cd--;
      }

      if (m.fuse > 0) {
         m.fuse--;
      }

      if (near) {
         m.hasTarget = true;
         m.dirYaw = Mobs.angleTo(dx, dz);
         in.headYaw = m.dirYaw;
         in.headPitch = pitchTo(dy, d2);
      }

      if (m.onGround && m.cd <= 0) {
         if (!near) {
            m.dirYaw = ms.rnd.nextFloat() * 360.0F;
         }

         double sp = 0.2 + 0.03 * size;
         m.vy = 0.42 + 0.03 * size;
         m.vx = -Math.sin(Math.toRadians(m.dirYaw)) * sp;
         m.vz = Math.cos(Math.toRadians(m.dirYaw)) * sp;
         m.yaw = m.dirYaw;
         m.cd = near ? 14 + ms.rnd.nextInt(10) : 30 + ms.rnd.nextInt(50);
      }

      if (near && d2 < 0.55 + 0.5 * size && Math.abs(dy) < 0.8 + 0.5 * size && m.fuse == 0) {
         m.fuse = 20;
         m.attackAnim = 8;
         ms.g.damagePlayer(size >= 3 ? 6 : (size == 2 ? 4 : 2), m.x, m.z);
      }
   }

   // ------------------------------------------------------------------ fireballs
   static void shoot(Mobs ms, Mob m, double tx, double ty, double tz, double speed, boolean big) {
      Fireball f = new Fireball();
      f.x = m.x;
      f.y = m.y + Mobs.H[m.type] * m.scale * (big ? 0.5 : 0.7);
      f.z = m.z;
      double dx = tx - f.x;
      double dy = ty - f.y;
      double dz = tz - f.z;
      double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
      if (len < 1.0E-6) {
         return;
      }

      double jitter = big ? 0.0 : 0.045;
      f.vx = dx / len * speed + ms.rnd.nextGaussian() * jitter;
      f.vy = dy / len * speed + ms.rnd.nextGaussian() * jitter;
      f.vz = dz / len * speed + ms.rnd.nextGaussian() * jitter;
      // out of the shooter's box
      f.x += f.vx / speed * 1.4 * (big ? 2.2 : 0.8);
      f.y += f.vy / speed * 1.4 * (big ? 2.2 : 0.8);
      f.z += f.vz / speed * 1.4 * (big ? 2.2 : 0.8);
      f.px = f.x;
      f.py = f.y;
      f.pz = f.z;
      f.big = big;
      ms.fireballs.add(f);
      m.attackAnim = 6;
   }

   static void tickFireballs(Mobs ms, World w, Player p) {
      for (int i = ms.fireballs.size() - 1; i >= 0; i--) {
         Fireball f = ms.fireballs.get(i);
         f.px = f.x;
         f.py = f.y;
         f.pz = f.z;
         f.life++;
         boolean boom = false;
         boolean gone = f.life > 400;
         for (int s = 0; s < 4 && !boom && !gone; s++) {
            f.x += f.vx / 4.0;
            f.y += f.vy / 4.0;
            f.z += f.vz / 4.0;
            int bx = (int)Math.floor(f.x);
            int by = (int)Math.floor(f.y);
            int bz = (int)Math.floor(f.z);
            if (!w.isLoaded(bx, bz) || by < 0 || by > 255) {
               gone = true;
               break;
            }

            int id = w.getBlock(bx, by, bz);
            if (id != 0 && Blocks.coll[id] != null) {
               boom = true;
            } else if (ms.targetable(p) && f.x > p.bb[0] - 0.25 && f.x < p.bb[3] + 0.25 && f.y > p.bb[1] - 0.25 && f.y < p.bb[4] + 0.25 && f.z > p.bb[2] - 0.25 && f.z < p.bb[5] + 0.25) {
               boom = true;
            } else {
               for (Mob o : ms.list) {
                  if (!o.dead && o.type != Mobs.GHAST && o.type != Mobs.BLAZE && f.x > o.bb[0] && f.x < o.bb[3] && f.y > o.bb[1] && f.y < o.bb[4] && f.z > o.bb[2] && f.z < o.bb[5]) {
                     boom = true;
                     break;
                  }
               }
            }
         }

         if (f.life % 2 == 0 && !gone) {
            ms.g.flameParticle(f.x, f.y, f.z, f.big);
         }

         if (boom) {
            explode(ms, w, p, f);
         }

         if (boom || gone) {
            ms.fireballs.remove(i);
         }
      }
   }

   /** A fireball arrives. Big (ghast): a hole about 1.5 blocks wide, 6 damage near it, fires. Small (blaze): 5 damage and sets you alight, a fire. */
   static void explode(Mobs ms, World w, Player p, Fireball f) {
      double r = f.big ? 1.6 : 0.6;
      int x0 = (int)Math.floor(f.x);
      int y0 = (int)Math.floor(f.y);
      int z0 = (int)Math.floor(f.z);
      int n = (int)Math.ceil(r);
      for (int dx = -n; dx <= n; dx++) {
         for (int dy = -n; dy <= n; dy++) {
            for (int dz = -n; dz <= n; dz++) {
               if (dx * dx + dy * dy + dz * dz > r * r) {
                  continue;
               }

               int id = w.getBlock(x0 + dx, y0 + dy, z0 + dz);
               if (f.big && id != 0 && Blocks.breakable(id) && id != Blocks.OBSIDIAN && !Blocks.isFluid(id) && !Blocks.isPortal(id) && ms.rnd.nextInt(3) != 0) {
                  w.setBlock(x0 + dx, y0 + dy, z0 + dz, 0);
               }
            }
         }
      }

      // fires on the floor around
      int fires = f.big ? 5 : 1;
      for (int k = 0; k < fires * 3 && fires > 0; k++) {
         int fx = x0 + ms.rnd.nextInt(5) - 2;
         int fz = z0 + ms.rnd.nextInt(5) - 2;
         for (int fy = y0 + 2; fy >= y0 - 2; fy--) {
            if (w.getBlock(fx, fy, fz) == 0 && Blocks.solid[w.getBlock(fx, fy - 1, fz)] && !Blocks.isFluid(w.getBlock(fx, fy - 1, fz))) {
               w.setBlock(fx, fy, fz, Blocks.FIRE);
               fires--;
               break;
            }
         }
      }

      if (f.big) {
         ms.g.explosionParticles(f.x, f.y, f.z);
      }

      if (ms.targetable(p)) {
         double dx = p.x - f.x;
         double dy = p.y + 0.9 - f.y;
         double dz = p.z - f.z;
         double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
         if (f.big && d < 4.0) {
            p.invuln = 0;
            ms.g.damagePlayer(Math.max(1, (int)Math.round(7.0 * (1.0 - d / 4.0))), f.x, f.z);
         } else if (!f.big && d < 1.4) {
            p.invuln = 0;
            ms.g.damagePlayer(5, f.x, f.z);
            if (!p.creative) {
               p.burn = Math.max(p.burn, 100);
            }
         }
      }

      for (Mob o : ms.list) {
         if (!o.dead && o.type != Mobs.GHAST && o.type != Mobs.BLAZE) {
            double d = Math.sqrt((o.x - f.x) * (o.x - f.x) + (o.z - f.z) * (o.z - f.z) + (o.y - f.y) * (o.y - f.y));
            if (d < (f.big ? 3.0 : 1.5)) {
               ms.hurtMob(o, f.big ? 6 : 4, f.x, f.z, 0.5, false);
            }
         }
      }
   }

   // ------------------------------------------------------------------ pigmen
   /** A pigman was hit by the player: every pigman within 20 blocks is angry as well. */
   static void angerNeighbours(Mobs ms, Mob hit) {
      if (hit.type != Mobs.ZOMBIE_PIGMAN) {
         return;
      }

      for (Mob o : ms.list) {
         if (o != hit && !o.dead && o.type == Mobs.ZOMBIE_PIGMAN && Math.abs(o.x - hit.x) < 20.0 && Math.abs(o.z - hit.z) < 20.0 && Math.abs(o.y - hit.y) < 12.0) {
            o.angry = Math.max(o.angry, 400);
         }
      }
   }

   // ------------------------------------------------------------------ what they leave behind
   static void kill(Mobs ms, Mob m, World w) {
      switch (m.type) {
         case Mobs.ZOMBIE_PIGMAN:
            ms.drop(w, m, Items.ROTTEN_FLESH, ms.rnd.nextInt(2));
            ms.drop(w, m, Items.GOLD_NUGGET, ms.rnd.nextInt(3));
            if (ms.rnd.nextInt(40) == 0) {
               ms.drop(w, m, Items.GOLD_INGOT, 1);
            }
            break;
         case Mobs.GHAST:
            ms.drop(w, m, Items.GHAST_TEAR, ms.rnd.nextInt(2));
            ms.drop(w, m, Items.GUNPOWDER, ms.rnd.nextInt(3));
            break;
         case Mobs.BLAZE:
            ms.drop(w, m, Items.BLAZE_ROD, ms.rnd.nextInt(2));
            break;
         case Mobs.MAGMA_CUBE:
            if (m.size > 1) {
               int n = 2 + ms.rnd.nextInt(2);
               for (int i = 0; i < n; i++) {
                  Mob c = ms.spawn(Mobs.MAGMA_CUBE, m.x + (ms.rnd.nextDouble() - 0.5) * m.size * 0.6, m.y + 0.1, m.z + (ms.rnd.nextDouble() - 0.5) * m.size * 0.6);
                  setSize(c, m.size - 1);
                  c.vx = (ms.rnd.nextDouble() - 0.5) * 0.4;
                  c.vz = (ms.rnd.nextDouble() - 0.5) * 0.4;
                  c.vy = 0.25;
                  c.cd = 10;
               }
            }
            break;
         default:
            break;
      }
   }

   // ------------------------------------------------------------------ appearing
   /** A room of 5 x 4 x 5 free cells with the corner at (x - 2, y, z - 2): a ghast fits. */
   static boolean roomFor(World w, int x, int y, int z, int rx, int ry) {
      for (int dx = -rx; dx <= rx; dx++) {
         for (int dz = -rx; dz <= rx; dz++) {
            if (!w.isLoaded(x + dx, z + dz)) {
               return false;
            }

            for (int dy = 0; dy < ry; dy++) {
               if (w.getBlock(x + dx, y + dy, z + dz) != 0) {
                  return false;
               }
            }
         }
      }

      return true;
   }

   static void spawnElsewhere(Mobs ms, World w, Player p) {
      if (p.dead) {
         return;
      }

      if (w.dim == Dim.NETHER) {
         spawnNether(ms, w, p);
      }
   }

   static void spawnNether(Mobs ms, World w, Player p) {
      if (ms.count(true) >= 26) {
         return;
      }

      for (int attempt = 0; attempt < 8; attempt++) {
         double a = ms.rnd.nextDouble() * Math.PI * 2.0;
         double d = 20.0 + ms.rnd.nextDouble() * 18.0;
         int x = (int)Math.floor(p.x + Math.cos(a) * d);
         int z = (int)Math.floor(p.z + Math.sin(a) * d);
         if (!w.isLoaded(x, z)) {
            continue;
         }

         int floor = -1;
         for (int y = Math.min(118, (int)p.y + 16); y > Math.max(2, (int)p.y - 26); y--) {
            int below = w.getBlock(x, y - 1, z);
            if (below != 0 && Blocks.solid[below] && !Blocks.isLava(below) && below != Blocks.BEDROCK && w.getBlock(x, y, z) == 0 && w.getBlock(x, y + 1, z) == 0) {
               floor = y;
               break;
            }
         }

         if (floor < 0) {
            continue;
         }

         int roll = ms.rnd.nextInt(100);
         if (roll < 14 && floor + 3 < 118 && roomFor(w, x, floor + 3, z, 2, 4)) {
            Mob g = ms.spawn(Mobs.GHAST, x + 0.5, floor + 3, z + 0.5);
            if (Phys.collides(w, g.bb)) {
               ms.list.remove(g);
            }

            return;
         }

         if (roll < 38 && floor < 52) {
            Mob c = ms.spawn(Mobs.MAGMA_CUBE, x + 0.5, floor, z + 0.5);
            setSize(c, 1 + ms.rnd.nextInt(3));
            if (Phys.collides(w, c.bb)) {
               ms.list.remove(c);
            }

            return;
         }

         int n = 1 + ms.rnd.nextInt(3);
         for (int i = 0; i < n; i++) {
            double sx = x + 0.5 + (i > 0 ? ms.rnd.nextInt(3) - 1 : 0) * 0.8;
            double sz = z + 0.5 + (i > 0 ? ms.rnd.nextInt(3) - 1 : 0) * 0.8;
            Mob pig = ms.spawn(Mobs.ZOMBIE_PIGMAN, sx, floor, sz);
            if (Phys.collides(w, pig.bb)) {
               ms.list.remove(pig);
            }
         }

         return;
      }
   }
}
