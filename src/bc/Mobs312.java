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
      /** The dragon's breath: purple, leaves a cloud that hurts instead of blowing a hole. */
      boolean dragon;
      /** An ender pearl the player threw: it arcs down and takes the player where it lands. */
      boolean pearl;
      /** The last position that was free of blocks (where a pearl lands). */
      double lx;
      double ly;
      double lz;
   }

   static boolean peaceful(int type) {
      return type == Mobs.VILLAGER || type == Mobs.IRON_GOLEM || type == Mobs.PARROT;
   }

   /** Villagers and golems are never lost: not by walking away from them, not by leaving the game. */
   static boolean keepsForever(int type) {
      return type == Mobs.VILLAGER || type == Mobs.IRON_GOLEM;
   }

   static boolean fireproof(int type) {
      return type == Mobs.ZOMBIE_PIGMAN || type == Mobs.GHAST || type == Mobs.BLAZE || type == Mobs.MAGMA_CUBE || type == Mobs.ENDER_DRAGON || type == Mobs.END_CRYSTAL;
   }

   /** Never despawns, however far away. */
   static boolean persistent(int type) {
      return type == Mobs.ENDER_DRAGON || type == Mobs.END_CRYSTAL;
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

      if (m.type == Mobs.PARROT) {
         m.color = r.nextInt(PARROT_COLORS);
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
         case Mobs.ENDERMAN:
            enderman(ms, m, w, p, target, dx, dz, dy, d2, d3, in);
            break;
         case Mobs.VILLAGER:
            villager(ms, m, dx, dz, d2, in);
            break;
         case Mobs.IRON_GOLEM:
            golem(ms, m, in);
            break;
         case Mobs.PARROT:
            parrot(ms, m, w, p, dx, dz, dy, d2, d3, in);
            break;
         default:
            break;
      }

      return in;
   }

   // ------------------------------------------------------------------ villages
   /** Stays within about 26 blocks of where it first stood (the village). */
   private static void leash(Mobs ms, Mob m) {
      if (m.hy == 0.0) {
         m.hx = m.x;
         m.hz = m.z;
         m.hy = 1.0;
      }

      double hd = Math.hypot(m.hx - m.x, m.hz - m.z);
      if (hd > 26.0) {
         m.dirYaw = Mobs.angleTo(m.hx - m.x, m.hz - m.z);
         m.moving = true;
         m.wander = 30;
      } else {
         ms.wanderAI(m);
      }
   }

   /** Wanders about the village; runs from monsters that come close and from whoever hurt it; looks at you. */
   private static void villager(Mobs ms, Mob m, double dx, double dz, double d2, Intent in) {
      if (m.flee > 0) {
         m.flee--;
         m.dirYaw = Mobs.angleTo(m.x - m.fleeX, m.z - m.fleeZ);
         in.moving = true;
         in.speed = 1.9;
         return;
      }

      Mob scary = null;
      double best = 64.0;
      for (Mob o : ms.list) {
         if (!o.dead && Mobs.hostile(o.type) && o.type != Mobs.ENDER_DRAGON && o.type != Mobs.END_CRYSTAL && o.type != Mobs.ENDERMAN && o.type != Mobs.ZOMBIE_PIGMAN) {
            double d = (o.x - m.x) * (o.x - m.x) + (o.z - m.z) * (o.z - m.z);
            if (d < best && Math.abs(o.y - m.y) < 4.0) {
               best = d;
               scary = o;
            }
         }
      }

      if (scary != null) {
         m.dirYaw = Mobs.angleTo(m.x - scary.x, m.z - scary.z);
         in.moving = true;
         in.speed = 1.6;
         return;
      }

      leash(ms, m);
      in.moving = m.moving;
      in.speed = 0.55;
      if (d2 < 5.0 && !in.moving) {
         in.headYaw = Mobs.angleTo(dx, dz);
      }
   }

   /** Walks about the village and beats up monsters that come near it. */
   private static void golem(Mobs ms, Mob m, Intent in) {
      if (m.cd > 0) {
         m.cd--;
      }

      Mob prey = null;
      double best = 18.0 * 18.0;
      for (Mob o : ms.list) {
         boolean foe = o.type == Mobs.ZOMBIE || o.type == Mobs.SKELETON || o.type == Mobs.CREEPER || o.type == Mobs.SPIDER || o.type == Mobs.MAGMA_CUBE
            || (o.type == Mobs.ZOMBIE_PIGMAN || o.type == Mobs.ENDERMAN) && o.angry > 0;
         if (!o.dead && foe) {
            double d = (o.x - m.x) * (o.x - m.x) + (o.z - m.z) * (o.z - m.z);
            if (d < best && Math.abs(o.y - m.y) < 5.0) {
               best = d;
               prey = o;
            }
         }
      }

      if (prey != null) {
         double dx = prey.x - m.x;
         double dz = prey.z - m.z;
         double d2 = Math.sqrt(dx * dx + dz * dz);
         m.hasTarget = true;
         m.dirYaw = Mobs.angleTo(dx, dz);
         in.headYaw = m.dirYaw;
         in.moving = d2 > 2.0;
         in.speed = 1.2;
         if (d2 < 2.9 && Math.abs(prey.y - m.y) < 3.0 && m.cd <= 0) {
            m.cd = 36;
            m.attackAnim = 10;
            ms.hurtMob(prey, 7 + ms.rnd.nextInt(10), m.x, m.z, 1.0, false);
            prey.vy = 0.55;
         }
      } else {
         leash(ms, m);
         in.moving = m.moving;
         in.speed = 0.5;
      }
   }

   /** Gives every village near the player its villagers (once) - and an iron golem if it has enough houses. */
   static void spawnVillagers(Mobs ms, World w, Player p) {
      if (!Gen.extras || w.dim != Dim.OVERWORLD || p.dead) {
         return;
      }

      int rx = Math.floorDiv((int)Math.floor(p.x), 320);
      int rz = Math.floorDiv((int)Math.floor(p.z), 320);
      for (int dx = -1; dx <= 1; dx++) {
         for (int dz = -1; dz <= 1; dz++) {
            Structs.Village v = w.gen.structs.village(rx + dx, rz + dz);
            if (!v.valid) {
               continue;
            }

            long key = (long)(rx + dx) << 32 ^ (rz + dz) & 4294967295L;
            boolean near = v.style >= 0 ? p.x >= v.minX - 50 && p.x <= v.maxX + 50 && p.z >= v.minZ - 50 && p.z <= v.maxZ + 50 : Math.hypot(p.x - v.cx, p.z - v.cz) <= 70.0;
            if (w.populated.contains(key) || !near || !w.isLoaded(v.cx, v.cz)) {
               continue;
            }

            int houses = 0;
            for (Structs.Bld b : v.blds) {
               if (b.type == 0 || b.type == 4 || b.type == 2) {
                  houses++;
               }
            }

            int n = Math.max(2, Math.min(v.style >= 0 ? 9 : 6, houses / 2 + 1));
            int placed = 0;
            for (int tries = 0; tries < 60 && placed < n + (houses >= 5 ? 1 : 0); tries++) {
               int x;
               int z;
               int baseY = v.cy;
               if (v.style >= 0 && !v.spawn.isEmpty()) {
                  // Update 3.14: the villagers start at the doors and in the streets of the village, not in a ring round the well
                  int[] sp = v.spawn.get(ms.rnd.nextInt(v.spawn.size()));
                  x = sp[0];
                  z = sp[1];
                  baseY = sp[2];
               } else {
                  double a = ms.rnd.nextDouble() * Math.PI * 2.0;
                  double d = 3.0 + ms.rnd.nextDouble() * 16.0;
                  x = v.cx + (int)Math.round(Math.cos(a) * d);
                  z = v.cz + (int)Math.round(Math.sin(a) * d);
               }

               if (!w.isLoaded(x, z)) {
                  continue;
               }

               for (int y = baseY + 6; y > baseY - 6; y--) {
                  int below = w.getBlock(x, y - 1, z);
                  if (Blocks.solid[below] && !Blocks.isFluid(below) && !Blocks.isLeaves(below) && free(w, x, y, z) && free(w, x, y + 1, z)) {
                     boolean golem = placed == n;
                     Mob m = ms.spawn(golem ? Mobs.IRON_GOLEM : Mobs.VILLAGER, x + 0.5, y, z + 0.5);
                     if (!golem) {
                        int[] jobs = new int[]{0, 0, 1, 2, 3, 4, 4, 5};
                        m.color = jobs[ms.rnd.nextInt(jobs.length)];
                     }

                     m.hx = v.style >= 0 ? x + 0.5 : v.cx;
                     m.hz = v.style >= 0 ? z + 0.5 : v.cz;
                     m.hy = 1.0;
                     placed++;
                     break;
                  }
               }
            }

            if (placed > 0) {
               w.populated.add(key);
            }
         }
      }
   }

   // ------------------------------------------------------------------ the End
   /** The dragon and the crystals have their own tick (they do not walk). True: this mob has been ticked. */
   static boolean tickSpecial(Mobs ms, Mob m, World w, Player p) {
      if (m.type == Mobs.ENDER_DRAGON) {
         Dragon.tick(ms, m, w, p);
         return true;
      }

      if (m.type == Mobs.END_CRYSTAL) {
         Dragon.crystalTick(ms, m, w);
         return true;
      }

      if (m.type == Mobs.PARROT && m.phase == P_SHOULDER) {
         shoulder(ms, m, w, p);
         return true;
      }

      return false;
   }

   /** Every tick of a dead mob. */
   static void tickDying(Mobs ms, Mob m, World w) {
      if (m.type == Mobs.ENDER_DRAGON) {
         Dragon.dying(ms, m, w);
      }
   }

   /** A dead mob has been taken out of the list (its death animation is over). */
   static void removed(Mobs ms, Mob m, World w) {
      if (m.type == Mobs.ENDER_DRAGON && w.fight != null) {
         w.fight.openPortal(true);
         ms.g.toast("The Ender Dragon is dead!", "The way out has opened in the middle of the island");
      }
   }

   /** A mob was hurt. */
   static void hurt(Mobs ms, Mob m, int dmg, boolean byPlayer) {
      if (m.dead) {
         return;
      }

      if (m.type == Mobs.ENDERMAN && ms.g.world != null) {
         m.angry = Math.max(m.angry, 600);
         if (ms.rnd.nextInt(3) == 0) {
            teleport(ms, m, ms.g.world, m.x, m.z, 14.0, 4.0);
         }
      } else if (m.type == Mobs.ENDER_DRAGON && byPlayer && m.phase == Dragon.CIRCLE && m.phaseT > 60 && ms.rnd.nextInt(3) == 0) {
         m.phaseT = 400;
      }
   }

   private static boolean free(World w, int x, int y, int z) {
      int id = w.getBlock(x, y, z);
      return id == 0 || Blocks.coll[id] == null && !Blocks.isFluid(id);
   }

   /** An enderman blinks to a free spot between minD and range blocks from (cx, cz) that has solid ground. */
   static boolean teleport(Mobs ms, Mob m, World w, double cx, double cz, double range, double minD) {
      for (int i = 0; i < 24; i++) {
         double a = ms.rnd.nextDouble() * Math.PI * 2.0;
         double d = minD + ms.rnd.nextDouble() * (range - minD);
         int x = (int)Math.floor(cx + Math.cos(a) * d);
         int z = (int)Math.floor(cz + Math.sin(a) * d);
         if (!w.isLoaded(x, z)) {
            continue;
         }

         int top = Math.min(250, (int)m.y + 8);
         for (int y = top; y > top - 18 && y > 1; y--) {
            int below = w.getBlock(x, y - 1, z);
            if (Blocks.solid[below] && !Blocks.isFluid(below) && !Blocks.isLeaves(below) && free(w, x, y, z) && free(w, x, y + 1, z) && free(w, x, y + 2, z)) {
               teleportEffect(ms, m.x, m.y, m.z);
               m.px = m.x = x + 0.5;
               m.py = m.y = y;
               m.pz = m.z = z + 0.5;
               m.vx = m.vy = m.vz = 0.0;
               m.setPos(m.x, m.y, m.z);
               teleportEffect(ms, m.x, m.y, m.z);
               m.tpCd = 40;
               return true;
            }

            if (Blocks.coll[below] != null) {
               break;
            }
         }
      }

      return false;
   }

   private static void teleportEffect(Mobs ms, double x, double y, double z) {
      if (ms.g.rend == null) {
         return;
      }

      for (int i = 0; i < 14; i++) {
         ms.g.portalParticle(x + (ms.rnd.nextDouble() - 0.5) * 0.8, y + ms.rnd.nextDouble() * 2.6, z + (ms.rnd.nextDouble() - 0.5) * 0.8, 0.75F, 0.25F, 0.95F);
      }
   }

   /** Is the player looking straight at the enderman's head (and can see it)? */
   static boolean lookedAt(Mobs ms, World w, Player p, Mob m) {
      double ex = p.x;
      double ey = p.y + p.eye;
      double ez = p.z;
      double yaw = Math.toRadians(p.yaw);
      double pitch = Math.toRadians(p.pitch);
      double lx = -Math.sin(yaw) * Math.cos(pitch);
      double ly = -Math.sin(pitch);
      double lz = Math.cos(yaw) * Math.cos(pitch);
      double hx = m.x - ex;
      double hy = m.y + 2.55 - ey;
      double hz = m.z - ez;
      double dist = Math.sqrt(hx * hx + hy * hy + hz * hz);
      if (dist < 0.5) {
         return true;
      }

      if ((lx * hx + ly * hy + lz * hz) / dist < 1.0 - 0.03 / dist) {
         return false;
      }

      return ms.hasLOS(w, ex, ey, ez, m.x, m.y + 2.55, m.z);
   }

   /** Calm until you look it in the face (or hit it); then it blinks about and hits hard. Water hurts it. */
   private static void enderman(Mobs ms, Mob m, World w, Player p, boolean target, double dx, double dz, double dy, double d2, double d3, Intent in) {
      if (m.tpCd > 0) {
         m.tpCd--;
      }

      if (m.cd > 0) {
         m.cd--;
      }

      if (m.inWater) {
         // it is hurt as long as it is wet (once every half second) and tries to get away
         ms.hurtMob(m, 1, m.x, m.z, 0.0, false);
         if (m.tpCd <= 0 && !m.dead && m.invuln > 5) {
            teleport(ms, m, w, m.x, m.z, 24.0, 6.0);
         }
      }

      if (target && d3 < 64.0 && m.angry <= 0 && lookedAt(ms, w, p, m)) {
         m.angry = 600;
      }

      if (target && m.angry > 0 && d3 < 48.0) {
         m.hasTarget = true;
         m.dirYaw = Mobs.angleTo(dx, dz);
         in.headYaw = m.dirYaw;
         in.headPitch = pitchTo(dy, d2);
         in.moving = d2 > 1.3;
         in.speed = 1.5;
         if (d2 < 1.9 && Math.abs(dy) < 2.6 && m.cd <= 0) {
            m.cd = 20;
            m.attackAnim = 8;
            ms.g.damagePlayer(6, m.x, m.z);
         }

         if (m.tpCd <= 0 && d2 > 5.0 && ms.rnd.nextInt(70) == 0) {
            teleport(ms, m, w, p.x, p.z, 5.0, 2.5);
         }
      } else {
         ms.wanderAI(m);
         in.moving = m.moving;
         in.speed = 0.5;
         if (m.tpCd <= 0 && ms.rnd.nextInt(1200) == 0) {
            teleport(ms, m, w, m.x, m.z, 14.0, 4.0);
         }
      }
   }

   /** The dragon's breath: a purple fireball that leaves a cloud where it lands. */
   static void shootDragon(Mobs ms, Mob m, double tx, double ty, double tz) {
      shoot(ms, m, tx, ty, tz, 0.75, true);
      if (!ms.fireballs.isEmpty()) {
         ms.fireballs.get(ms.fireballs.size() - 1).dragon = true;
      }
   }

   /** Endermen on the ground of the island, a handful at a time. */
   static void spawnEnd(Mobs ms, World w, Player p) {
      int n = 0;
      for (Mob m : ms.list) {
         if (m.type == Mobs.ENDERMAN && !m.dead) {
            n++;
         }
      }

      if (n >= 9) {
         return;
      }

      for (int tries = 0; tries < 8; tries++) {
         double a = ms.rnd.nextDouble() * Math.PI * 2.0;
         double d = 16.0 + ms.rnd.nextDouble() * 22.0;
         int x = (int)Math.floor(p.x + Math.cos(a) * d);
         int z = (int)Math.floor(p.z + Math.sin(a) * d);
         if (!w.isLoaded(x, z)) {
            continue;
         }

         for (int y = EndGen.TOP + 6; y > EndGen.TOP - 12; y--) {
            int below = w.getBlock(x, y - 1, z);
            if (below == Blocks.END_STONE && free(w, x, y, z) && free(w, x, y + 1, z) && free(w, x, y + 2, z)) {
               ms.spawn(Mobs.ENDERMAN, x + 0.5, y, z + 0.5);
               return;
            }

            if (below != 0) {
               break;
            }
         }
      }
   }

   // ------------------------------------------------------------------ parrots
   static final int P_FLY = 0;
   static final int P_LAND = 1;
   static final int P_SHOULDER = 2;
   /** Red, blue, green, cyan, grey. */
   static final int PARROT_COLORS = 5;

   static boolean onShoulder(Mob m) {
      return m.type == Mobs.PARROT && m.phase == P_SHOULDER && !m.dead;
   }

   static boolean isSeed(int id) {
      return id == Items.SEEDS || id == Items.PUMPKIN_SEEDS || id == Items.MELON_SEEDS;
   }

   /** The y of the first solid block (leaves too: parrots sit in the trees) under (x, z), scanning down from y0; the cell above it. */
   static int surfaceAbove(World w, int x, int z, int y0) {
      for (int y = Math.min(y0, 250); y > 30; y--) {
         int b = w.getBlock(x, y, z);
         if (b != 0 && Blocks.solid[b]) {
            return y + 1;
         }
      }

      return 40;
   }

   private static void takeOff(Mobs ms, Mob m) {
      m.phase = P_FLY;
      m.phaseT = 0;
      m.fuse = 0;
      ms.flyVy = 0.1;
      m.vy = 0.2;
   }

   /** A wild parrot flutters from tree to tree and sits down now and then; a tame one stays near you. */
   private static void parrot(Mobs ms, Mob m, World w, Player p, double dx, double dz, double dy, double d2, double d3, Intent in) {
      Random r = ms.rnd;
      if (m.tamed && !p.dead && d3 > 24.0 && !p.spectator) {
         // too far behind: it catches up at once (the way pets do)
         double a = r.nextDouble() * Math.PI * 2.0;
         double ox = m.x;
         double oy = m.y;
         double oz = m.z;
         m.setPos(p.x + Math.cos(a) * 2.0, p.y + 1.6, p.z + Math.sin(a) * 2.0);
         if (Phys.collides(w, m.bb)) {
            m.setPos(ox, oy, oz);
         } else {
            m.px = m.x;
            m.py = m.y;
            m.pz = m.z;
            m.phase = P_FLY;
         }
      }

      if (m.phase == P_LAND) {
         in.moving = false;
         ms.flyVy = m.onGround ? 0.0 : -0.1;
         m.phaseT--;
         boolean frightened = !m.tamed && d2 < 4.0 && !p.sneaking;
         boolean ownerGone = m.tamed && (d2 > 7.0 || d3 > 10.0);
         if (m.phaseT <= 0 || frightened || ownerGone) {
            takeOff(ms, m);
         }

         if (m.tamed && d2 < 8.0) {
            in.headYaw = Mobs.angleTo(dx, dz);
            in.headPitch = pitchTo(dy, d2);
         }

         return;
      }

      double lastMove = Math.hypot(m.x - m.px, m.z - m.pz);
      if (m.tamed) {
         double want = p.y + 1.9 + Math.sin((m.age + m.hashCode() % 50) * 0.09) * 0.5;
         if (d2 > 4.5) {
            m.dirYaw = Mobs.angleTo(dx, dz);
            in.moving = true;
            in.speed = d2 > 12.0 ? 1.9 : 1.3;
         } else {
            ms.wanderAI(m);
            in.moving = m.moving;
            in.speed = 0.6;
            if (d2 < 1.3) {
               m.dirYaw = Mobs.angleTo(-dx, -dz);
               in.moving = true;
            }
         }

         ms.flyVy = Math.max(-0.1, Math.min(0.11, (want - m.y) * 0.09));
         if (d2 < 5.0) {
            in.headYaw = Mobs.angleTo(dx, dz);
            in.headPitch = pitchTo(dy, d2);
            if (r.nextInt(700) == 0) {
               m.phase = P_LAND;
               m.phaseT = 300 + r.nextInt(300);
            }
         }
      } else {
         double ddx = m.tx - m.x;
         double ddz = m.tz - m.z;
         if (m.phaseT <= 0 || Math.hypot(ddx, ddz) < 1.6 || m.tx == 0.0 && m.tz == 0.0) {
            double a = r.nextDouble() * Math.PI * 2.0;
            double dist = 6.0 + r.nextDouble() * 10.0;
            m.tx = m.x + Math.cos(a) * dist;
            m.tz = m.z + Math.sin(a) * dist;
            int ground = surfaceAbove(w, (int)Math.floor(m.tx), (int)Math.floor(m.tz), (int)m.y + 14);
            m.ty = ground + 2.0 + r.nextDouble() * 4.0;
            m.phaseT = 80 + r.nextInt(120);
            ddx = m.tx - m.x;
            ddz = m.tz - m.z;
         }

         m.phaseT--;
         m.dirYaw = Mobs.angleTo(ddx, ddz);
         in.moving = true;
         in.speed = 1.0;
         ms.flyVy = Math.max(-0.09, Math.min(0.11, (m.ty - m.y) * 0.1));
         if (lastMove < 0.02) {
            m.fuse++;
         } else {
            m.fuse = 0;
         }

         if (m.fuse > 6) {
            // flew into a branch: up and somewhere else
            m.fuse = 0;
            m.phaseT = 0;
            m.ty = m.y + 3.0;
            ms.flyVy = 0.11;
         }

         if (r.nextInt(500) == 0 && m.y - surfaceAbove(w, (int)Math.floor(m.x), (int)Math.floor(m.z), (int)m.y + 1) < 5.0) {
            m.phase = P_LAND;
            m.phaseT = 120 + r.nextInt(240);
         }
      }

      if (m.phase != P_LAND) {
         keepOffTerrain(ms, w, m);
      }
   }

   /** On a shoulder: it follows the player's body; it flies up when the player is hurt, swims or dies. */
   private static void shoulder(Mobs ms, Mob m, World w, Player p) {
      boolean off = p.dead || p.spectator || p.inWater || p.health < m.tx;
      m.tx = p.health;
      m.onGround = true;
      if (off) {
         takeOff(ms, m);
         m.phase = P_FLY;
         return;
      }

      double yaw = Math.toRadians(p.yaw);
      double side = m.aux >= 0.0F ? 1.0 : -1.0;
      double right = 0.36 * side;
      double x = p.x - Math.cos(yaw) * right;
      double z = p.z - Math.sin(yaw) * right;
      double y = p.y + (p.sneaking ? 1.15 : 1.4);
      m.vx = m.vy = m.vz = 0.0;
      m.setPos(x, y, z);
      m.yaw = p.yaw;
      m.headYaw = p.yaw;
      m.dirYaw = p.yaw;
      m.moving = false;
   }

   /** Which shoulder is free: +1 (right), -1 (left), 0 (none: two parrots are enough). */
   static float freeShoulder(Mobs ms) {
      boolean right = false;
      boolean left = false;
      for (Mob o : ms.list) {
         if (onShoulder(o)) {
            if (o.aux >= 0.0F) {
               right = true;
            } else {
               left = true;
            }
         }
      }

      return !right ? 1.0F : (!left ? -1.0F : 0.0F);
   }

   /** Sneak and right click with an empty hand: the parrot on your shoulder flies off. True if there was one. */
   static boolean shoulderOff(Game g) {
      Mob last = null;
      for (Mob o : g.mobs.list) {
         if (onShoulder(o)) {
            last = o;
         }
      }

      if (last == null) {
         return false;
      }

      takeOff(g.mobs, last);
      return true;
   }

   /** Right click on a parrot: seeds tame it (one in three), then an empty hand puts it on your shoulder. True if something happened. */
   static boolean useParrot(Game g, Mob m, Stack s) {
      if (m.dead) {
         return false;
      }

      if (s != null && isSeed(s.id)) {
         if (m.tamed) {
            return false;
         }

         if (!g.player.creative) {
            s.n--;
            if (s.n <= 0) {
               g.inv[g.sel] = null;
            }
         }

         if (g.mobs.rnd.nextInt(3) == 0) {
            m.tamed = true;
            m.phase = P_FLY;
            g.loveParticles(m);
            g.adv.award(g, "husbandry/tame_an_animal");
         }

         return true;
      }

      if (m.tamed && !g.player.sneaking && (s == null || !isSeed(s.id))) {
         float side = freeShoulder(g.mobs);
         if (side == 0.0F || m.phase == P_SHOULDER) {
            return false;
         }

         m.phase = P_SHOULDER;
         m.aux = side;
         m.tx = g.player.health;
         return true;
      }

      return false;
   }

   /** Now and then a pair of wild parrots appears in a forest, somewhere in the trees. */
   static void spawnParrots(Mobs ms, World w, Player p) {
      if (w.dim != Dim.OVERWORLD || ms.rnd.nextInt(8) != 0) {
         return;
      }

      int wild = 0;
      for (Mob o : ms.list) {
         if (o.type == Mobs.PARROT && !o.dead && !o.tamed) {
            wild++;
         }
      }

      if (wild >= 4) {
         return;
      }

      for (int tries = 0; tries < 6; tries++) {
         double a = ms.rnd.nextDouble() * Math.PI * 2.0;
         double d = 16.0 + ms.rnd.nextDouble() * 34.0;
         int x = (int)Math.floor(p.x + Math.cos(a) * d);
         int z = (int)Math.floor(p.z + Math.sin(a) * d);
         if (!w.isLoaded(x, z)) {
            continue;
         }

         Chunk c = w.chunk(Math.floorDiv(x, 16), Math.floorDiv(z, 16));
         if (c == null || !(c.biome[(z & 15) * 16 + (x & 15)] == 3 || c.biome[(z & 15) * 16 + (x & 15)] == Biomes.JUNGLE || c.biome[(z & 15) * 16 + (x & 15)] == Biomes.FLOWER_FOREST)) {
            continue;
         }

         int y = surfaceAbove(w, x, z, 200);
         int n = 1 + ms.rnd.nextInt(2);
         for (int i = 0; i < n; i++) {
            Mob m = ms.spawn(Mobs.PARROT, x + 0.5 + i * 0.7, y + 0.3, z + 0.5);
            if (Phys.collides(w, m.bb)) {
               ms.list.remove(m);
            }
         }

         return;
      }
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

   // ------------------------------------------------------------------ ender pearls
   /** Right click with an ender pearl: it flies off along your line of sight at 1.5 blocks a tick (one throw a second). */
   static boolean throwPearl(Game g, Stack held) {
      Player p = g.player;
      if (g.world == null || g.mobs == null || p.dead || p.spectator) {
         return false;
      }

      for (Fireball o : g.mobs.fireballs) {
         if (o.pearl && o.life < 20) {
            return false;
         }
      }

      double yaw = Math.toRadians(p.yaw);
      double pitch = Math.toRadians(p.pitch);
      double dx = -Math.sin(yaw) * Math.cos(pitch);
      double dy = -Math.sin(pitch);
      double dz = Math.cos(yaw) * Math.cos(pitch);
      Fireball f = new Fireball();
      f.pearl = true;
      f.x = f.px = f.lx = p.x + dx * 0.5;
      f.y = f.py = f.ly = p.y + p.eye - 0.1 + dy * 0.5;
      f.z = f.pz = f.lz = p.z + dz * 0.5;
      f.vx = dx * 1.5;
      f.vy = dy * 1.5;
      f.vz = dz * 1.5;
      g.mobs.fireballs.add(f);
      if (!p.creative) {
         held.n--;
         if (held.n <= 0) {
            g.inv[g.sel] = null;
         }
      }

      return true;
   }

   /** Where the player's feet fit around a point where a pearl came to rest (null: nowhere, the pearl is lost). */
   static double[] pearlSpot(World w, double x, double y, double z) {
      double[][] around = {{0, 0}, {0.35, 0}, {-0.35, 0}, {0, 0.35}, {0, -0.35}, {0.6, 0.6}, {-0.6, 0.6}, {0.6, -0.6}, {-0.6, -0.6}};
      double[] lower = {0.0, -0.3, -0.6, -0.9, -1.2, -1.6, 0.4, 0.8};
      double[] bb = new double[6];
      for (double[] o : around) {
         for (double dy : lower) {
            double fx = x + o[0];
            double fy = y + dy;
            double fz = z + o[1];
            if (fy < 1.0 || fy > 250.0) {
               continue;
            }

            bb[0] = fx - 0.3;
            bb[1] = fy;
            bb[2] = fz - 0.3;
            bb[3] = fx + 0.3;
            bb[4] = fy + 1.8;
            bb[5] = fz + 0.3;
            if (!Phys.collides(w, bb)) {
               return new double[]{fx, fy, fz};
            }
         }
      }

      return null;
   }

   /** The pearl came down: sparks, you appear there (5 damage like a fall, armour does not help), more sparks. */
   static void pearlLand(Mobs ms, World w, Player p, Fireball f) {
      Game g = ms.g;
      if (p.dead || p.spectator || g.world != w) {
         return;
      }

      double[] spot = pearlSpot(w, f.lx, f.ly, f.lz);
      if (spot == null) {
         return;
      }

      for (int k = 0; k < 24; k++) {
         g.portalParticle(p.x + (ms.rnd.nextDouble() - 0.5) * 0.6, p.y + ms.rnd.nextDouble() * 1.8, p.z + (ms.rnd.nextDouble() - 0.5) * 0.6, 0.55F, 0.25F, 0.9F);
      }

      p.setPos(spot[0], spot[1], spot[2]);
      p.fallDistance = 0.0F;
      p.flying = p.flying && p.creative;
      if (!p.creative) {
         p.invuln = 0;
         p.hurt(5);
      }

      for (int k = 0; k < 24; k++) {
         g.portalParticle(spot[0] + (ms.rnd.nextDouble() - 0.5) * 0.6, spot[1] + ms.rnd.nextDouble() * 1.8, spot[2] + (ms.rnd.nextDouble() - 0.5) * 0.6, 0.55F, 0.25F, 0.9F);
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
         if (f.pearl) {
            f.vy -= 0.03;
            f.vx *= 0.99;
            f.vy *= 0.99;
            f.vz *= 0.99;
         }

         for (int s = 0; s < 4 && !boom && !gone; s++) {
            f.lx = f.x;
            f.ly = f.y;
            f.lz = f.z;
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
            } else if (!f.pearl && ms.targetable(p) && f.x > p.bb[0] - 0.25 && f.x < p.bb[3] + 0.25 && f.y > p.bb[1] - 0.25 && f.y < p.bb[4] + 0.25 && f.z > p.bb[2] - 0.25 && f.z < p.bb[5] + 0.25) {
               boom = true;
            } else {
               for (Mob o : ms.list) {
                  if (!o.dead && (f.pearl || o.type != Mobs.GHAST && o.type != Mobs.BLAZE) && !(f.dragon && (o.type == Mobs.ENDER_DRAGON || o.type == Mobs.END_CRYSTAL)) && f.x > o.bb[0] && f.x < o.bb[3] && f.y > o.bb[1] && f.y < o.bb[4] && f.z > o.bb[2] && f.z < o.bb[5]) {
                     boom = true;
                     break;
                  }
               }
            }
         }

         if (f.pearl) {
            if (!gone) {
               ms.g.portalParticle(f.x, f.y, f.z, 0.55F, 0.25F, 0.9F);
            }
         } else if (f.life % 2 == 0 && !gone) {
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
      if (f.pearl) {
         pearlLand(ms, w, p, f);
         return;
      }

      if (f.dragon) {
         if (w.fight != null) {
            w.fight.addCloud(f.x, f.y, f.z);
         }

         ms.g.explosionParticles(f.x, f.y, f.z);
         if (ms.targetable(p)) {
            double d = Math.sqrt((p.x - f.x) * (p.x - f.x) + (p.y + 0.9 - f.y) * (p.y + 0.9 - f.y) + (p.z - f.z) * (p.z - f.z));
            if (d < 4.0) {
               p.invuln = 0;
               ms.g.damagePlayer(Math.max(2, (int)Math.round(8.0 * (1.0 - d / 4.0))), f.x, f.z);
            }
         }

         return;
      }

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
         case Mobs.PARROT:
            ms.drop(w, m, Items.FEATHER, 1 + ms.rnd.nextInt(2));
            break;
         case Mobs.ENDERMAN:
            ms.drop(w, m, Items.ENDER_PEARL, ms.rnd.nextInt(2) + (ms.rnd.nextInt(5) == 0 ? 1 : 0));
            break;
         case Mobs.END_CRYSTAL:
            Dragon.crystalBlows(ms, m, w);
            break;
         case Mobs.ENDER_DRAGON:
            if (w.fight != null) {
               w.fight.dragonKilled();
            }
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
      } else if (w.dim == Dim.END) {
         spawnEnd(ms, w, p);
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
         if (w.getBlock(x, floor - 1, z) == Blocks.NETHER_BRICKS) {
            // on the floor of a fortress: blazes and skeletons guard it
            Mob guard = ms.spawn(roll < 45 ? Mobs.BLAZE : (roll < 85 ? Mobs.SKELETON : Mobs.ZOMBIE_PIGMAN), x + 0.5, floor, z + 0.5);
            if (Phys.collides(w, guard.bb)) {
               ms.list.remove(guard);
            }

            return;
         }

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
