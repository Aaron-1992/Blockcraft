package bc;

import bc.Mobs.Mob;

/**
 * Update 3.12: the Ender Dragon and the End crystals that heal it.
 *
 * The dragon flies no path of its own through the blocks: it simply sets its position (nothing stops it). It circles round the middle of
 * the island, now and then swoops at you - breathing a fireball on the way, ramming whatever is in the way - and now and then lands on
 * the bedrock fountain, where it can be hit in peace for a while. While a crystal stands, it heals. A crystal is destroyed by any hit
 * and blows up, hurting the dragon if it is close.
 */
final class Dragon {
   static final int CIRCLE = 0;
   static final int STRAFE = 1;
   static final int LAND = 2;
   static final int PERCH = 3;
   static final int TAKEOFF = 4;
   /** How long the dying dragon stays (the explosions, the beams) before it is gone and the portal opens. */
   static final int DEATH_TICKS = 200;
   static final double CIRCLE_R = 34.0;

   private Dragon() {
   }

   static void setPos(Mob m, double x, double y, double z) {
      m.x = x;
      m.y = y;
      m.z = z;
      m.setPos(x, y, z);
   }

   private static void face(Mob m, double dx, double dz, double dy, float turn) {
      if (dx * dx + dz * dz > 1.0E-6) {
         float target = Mobs.angleTo(dx, dz);
         m.yaw += Math.max(-turn, Math.min(turn, Mobs.wrap(target - m.yaw)));
         m.dirYaw = m.yaw;
      }

      m.headYaw = m.yaw;
      m.headPitch = (float)Math.max(-35.0, Math.min(35.0, -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz) + 1.0E-6)) * 0.6));
   }

   static void tick(Mobs ms, Mob m, World w, Player p) {
      if (m.hy == 0.0) {
         if (w.dim == Dim.END) {
            m.hx = 0.5;
            m.hz = 0.5;
            m.hy = 84.0;
         } else {
            m.hx = m.x;
            m.hz = m.z;
            m.hy = m.y + 12.0;
         }

         m.rad = (float)Math.max(12.0, Math.min(CIRCLE_R, Math.hypot(m.x - m.hx, m.z - m.hz)));
         m.phase = CIRCLE;
         m.phaseT = 60;
      }

      m.phaseT++;
      m.walk += 0.4F;
      boolean target = ms.targetable(p) && Math.abs(p.x - m.hx) < 130.0 && Math.abs(p.z - m.hz) < 130.0;
      double px = m.x;
      double py = m.y;
      double pz = m.z;
      double nx = px;
      double ny = py;
      double nz = pz;
      switch (m.phase) {
         case CIRCLE: {
            double ang = Math.atan2(pz - m.hz, px - m.hx) + Math.toRadians(1.1);
            double r = Math.hypot(px - m.hx, pz - m.hz);
            r += (CIRCLE_R + 4.0 * Math.sin(m.age * 0.013) - r) * 0.03;
            nx = m.hx + Math.cos(ang) * r;
            nz = m.hz + Math.sin(ang) * r;
            ny = py + (m.hy + 7.0 * Math.sin(m.age * 0.021) - py) * 0.04;
            m.vx = nx - px;
            m.vy = ny - py;
            m.vz = nz - pz;
            if (m.phaseT > 260 && target && !m.dead) {
               int roll = ms.rnd.nextInt(100);
               boolean noCrystals = ms.g.world.fight != null && ms.g.world.fight.crystalsStanding(ms) == 0;
               if (roll < (noCrystals ? 45 : 70)) {
                  startStrafe(m, p);
               } else if (roll < (noCrystals ? 100 : 85)) {
                  m.phase = LAND;
                  m.phaseT = 0;
               } else {
                  m.phaseT = 150;
               }
            }
            break;
         }
         case STRAFE: {
            double dx = m.tx - px;
            double dy = m.ty - py;
            double dz = m.tz - pz;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz) + 1.0E-6;
            double sp = 1.05;
            // turning is gradual: the dragon swings round in an arc and carries on past you
            m.vx = m.vx * 0.9 + dx / dist * sp * 0.1;
            m.vy = m.vy * 0.9 + dy / dist * sp * 0.1;
            m.vz = m.vz * 0.9 + dz / dist * sp * 0.1;
            nx = px + m.vx;
            ny = py + m.vy;
            nz = pz + m.vz;
            if (m.fuse == 0 && dist < 46.0 && target) {
               m.fuse = 1;
               Mobs312.shootDragon(ms, m, p.x, p.y + 1.0, p.z);
            }

            if (m.phaseT > 75 || m.phaseT > 25 && dist < 3.0 && m.fuse > 0) {
               m.phase = CIRCLE;
               m.phaseT = 0;
            }
            break;
         }
         case LAND: {
            double ty = w.dim == Dim.END ? EndGen.TOP + 2.0 : m.hy - 14.0;
            double dx = m.hx - px;
            double dy = ty - py;
            double dz = m.hz - pz;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz) + 1.0E-6;
            double sp = Math.min(0.75, dist * 0.06 + 0.05);
            nx = px + dx / dist * sp;
            ny = py + dy / dist * sp;
            nz = pz + dz / dist * sp;
            m.vx = nx - px;
            m.vy = ny - py;
            m.vz = nz - pz;
            if (dist < 1.6 || m.phaseT > 400) {
               m.phase = PERCH;
               m.phaseT = 0;
            }
            break;
         }
         case PERCH: {
            m.vx = m.vy = m.vz = 0.0;
            if (target) {
               double dx = p.x - px;
               double dz = p.z - pz;
               m.yaw += Math.max(-4.0F, Math.min(4.0F, Mobs.wrap(Mobs.angleTo(dx, dz) - m.yaw)));
            }

            // the breath: a pool of purple smoke in front of the dragon, and one where you stand
            if (m.phaseT % 35 == 10 && m.phaseT < 130 && ms.g.world.fight != null) {
               double fx = px - Math.sin(Math.toRadians(m.yaw)) * 6.5;
               double fz = pz + Math.cos(Math.toRadians(m.yaw)) * 6.5;
               ms.g.world.fight.addCloud(fx, py, fz);
               if (target && Math.hypot(p.x - px, p.z - pz) < 14.0) {
                  ms.g.world.fight.addCloud(p.x, p.y, p.z);
               }
            }

            if (m.phaseT > 170) {
               m.phase = TAKEOFF;
               m.phaseT = 0;
            }
            break;
         }
         default: {
            double ang = Math.atan2(pz - m.hz, px - m.hx) + Math.toRadians(0.8);
            double r = Math.hypot(px - m.hx, pz - m.hz);
            r += (CIRCLE_R - r) * 0.04;
            nx = m.hx + Math.cos(ang) * r;
            nz = m.hz + Math.sin(ang) * r;
            ny = py + Math.min(0.55, (m.hy - py) * 0.08 + 0.1);
            m.vx = nx - px;
            m.vy = ny - py;
            m.vz = nz - pz;
            if (m.phaseT > 55 || ny >= m.hy - 1.0) {
               m.phase = CIRCLE;
               m.phaseT = 0;
            }
         }
      }

      setPos(m, nx, ny, nz);
      if (m.phase != PERCH) {
         face(m, m.vx, m.vz, m.vy, 5.0F);
      }

      // it rams: whoever is inside its body is hurt and thrown away
      if (m.cd > 0) {
         m.cd--;
      }

      if (m.cd <= 0 && ms.targetable(p) && p.bb[0] < m.bb[3] + 0.2 && p.bb[3] > m.bb[0] - 0.2 && p.bb[1] < m.bb[4] + 0.2 && p.bb[4] > m.bb[1] - 0.2 && p.bb[2] < m.bb[5] + 0.2 && p.bb[5] > m.bb[2] - 0.2) {
         m.cd = 30;
         m.attackAnim = 10;
         p.invuln = 0;
         ms.g.damagePlayer(7, m.x - m.vx * 3.0, m.z - m.vz * 3.0);
      }

      // it heals while a crystal stands, and every crystal sends it a beam
      if (ms.g.world.fight != null && m.health < EndFight.DRAGON_HP) {
         int crystals = ms.g.world.fight.crystalsStanding(ms);
         if (crystals > 0 && m.age % 10 == 0) {
            m.health++;
         }
      }

      // whatever stands in its way (but not stone, obsidian, bedrock) is torn down
      if (m.age % 4 == 0 && m.phase != PERCH) {
         smash(ms, m, w);
      }

      if (m.attackAnim > 0 && m.attackAnim % 2 == 0 && ms.g.rend != null) {
         ms.g.flameParticle(m.x, m.y + 2.0, m.z, true);
      }
   }

   private static void startStrafe(Mob m, Player p) {
      m.phase = STRAFE;
      m.phaseT = 0;
      m.fuse = 0;
      m.tx = p.x;
      m.ty = p.y + 1.0;
      m.tz = p.z;
   }

   private static void smash(Mobs ms, Mob m, World w) {
      int x0 = (int)Math.floor(m.bb[0]);
      int x1 = (int)Math.floor(m.bb[3]);
      int y0 = (int)Math.floor(m.bb[1]);
      int y1 = (int)Math.floor(m.bb[4]);
      int z0 = (int)Math.floor(m.bb[2]);
      int z1 = (int)Math.floor(m.bb[5]);
      for (int y = y0; y <= y1; y++) {
         for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
               int id = w.getBlock(x, y, z);
               if (id != 0 && Blocks.breakable(id) && id != Blocks.OBSIDIAN && id != Blocks.END_STONE && !Blocks.isFluid(id) && id != Blocks.DRAGON_EGG && !Blocks.isPortal(id)) {
                  w.setBlock(x, y, z, 0);
               }
            }
         }
      }
   }

   // ------------------------------------------------------------------ the crystals
   static void crystalTick(Mobs ms, Mob m, World w) {
      m.walk += 0.2F;
      if (m.age % 3 != 0 || ms.g.rend == null) {
         return;
      }

      Mob d = w.fight != null ? w.fight.dragon(ms) : null;
      if (d == null) {
         return;
      }

      double cx = m.x;
      double cy = m.y + 1.0;
      double cz = m.z;
      double dx = d.x - cx;
      double dy = d.y + 2.0 - cy;
      double dz = d.z - cz;
      if (dx * dx + dy * dy + dz * dz > 120.0 * 120.0) {
         return;
      }

      // a spark runs along the beam
      double t = (m.age / 3 % 12) / 12.0 + ms.rnd.nextDouble() * 0.04;
      ms.g.portalParticle(cx + dx * t, cy + dy * t, cz + dz * t, 1.0F, 0.35F, 0.9F);
   }

   /** The crystal was hit: it blows up. A dragon close by is hurt by it. */
   static void crystalBlows(Mobs ms, Mob m, World w) {
      m.deathTime = 19;
      ms.blast(m.x, m.y + 1.0, m.z, 4, m);
      if (w.fight != null) {
         w.fight.crystalDestroyed(m.tag);
         Mob d = w.fight.dragon(ms);
         if (d != null && Math.hypot(d.x - m.x, d.z - m.z) < 14.0 && Math.abs(d.y + 2.0 - m.y) < 14.0) {
            d.invuln = 0;
            ms.hurtMob(d, 10, m.x, m.z, 0.0, true);
         }
      }
   }

   // ------------------------------------------------------------------ the end of the dragon
   /** Every tick while the dragon is dying: it hangs in the air, shakes and bursts into flames. */
   static void dying(Mobs ms, Mob m, World w) {
      m.vx = m.vy = m.vz = 0.0;
      if (ms.g.rend == null) {
         return;
      }

      for (int i = 0; i < 2; i++) {
         double x = m.x + (ms.rnd.nextDouble() - 0.5) * 8.0;
         double y = m.y + ms.rnd.nextDouble() * 4.0;
         double z = m.z + (ms.rnd.nextDouble() - 0.5) * 8.0;
         ms.g.flameParticle(x, y, z, ms.rnd.nextBoolean());
      }

      if (m.deathTime % 20 == 0 && m.deathTime > 0) {
         ms.g.explosionParticles(m.x + (ms.rnd.nextDouble() - 0.5) * 6.0, m.y + 2.0 + ms.rnd.nextDouble() * 2.0, m.z + (ms.rnd.nextDouble() - 0.5) * 6.0);
      }

      if (m.deathTime > DEATH_TICKS - 40) {
         // the light pillar rises out of the fountain
         for (int i = 0; i < 6; i++) {
            ms.g.portalParticle(0.5 + (ms.rnd.nextDouble() - 0.5) * 3.0, EndGen.TOP + 1 + ms.rnd.nextDouble() * 30.0, 0.5 + (ms.rnd.nextDouble() - 0.5) * 3.0, 0.9F, 0.7F, 1.0F);
         }
      }
   }
}
