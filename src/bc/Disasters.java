package bc;

import java.util.ArrayList;
import java.util.Properties;
import java.util.Random;

/**
 * Update 3.10: natural disasters (To do item 5). Everything here is game rules; the pictures are drawn by {@link DisasterFx}.
 *
 * <ul>
 *   <li><b>Tornadoes</b> form under a thunderstorm and wander across the land at about 2 blocks per second for two to four minutes.
 *   A funnel of {@link Tornado#height} blocks hangs from a dark wall cloud. Close to it everything is pulled in, around and up (players,
 *   animals, items - lifted at most 14 blocks, so a fall is hurt but survivable); leaves, plants, glass, wool, wooden furniture,
 *   planks and the like are torn out of the ground and spin around the funnel as {@link Debris} before they are flung out and come
 *   down (some as blocks again, some as splinters); trees are uprooted and topple (see {@link Trees}). Terrain, stone, bricks, ores,
 *   chests and furnaces are never touched.</li>
 *   <li><b>Earthquakes</b> shake the camera (stronger the nearer the epicentre), crumble loose terrain on cliffs and overhangs into
 *   falling blocks, break glass / torches / ladders / plants and topple trees.</li>
 *   <li><b>Meteors</b> streak across the sky and hit the ground with a blast of TNT size or more, set fires and leave obsidian.
 *   They come as a shower on some nights.</li>
 * </ul>
 *
 * Natural disasters are on by default (Options: Disasters, and /disaster on|off per world); they start after a grace period of a
 * few minutes and are rare ({@link #tickNatural}). /disaster tornado|quake|meteor starts one by hand, /disaster stop ends them all.
 */
public final class Disasters {
   static final int MAX_DEBRIS = 80;
   static final int MAX_PARTICLES = 1500;
   /** Screenshot hook (-Dbc.dfreeze=N): everything stops moving N ticks after it was started by -Dbc.disaster. */
   static final long FREEZE_AFTER = Long.getLong("bc.dfreeze", 0L);

   final World w;
   public boolean enabled = true;
   public final ArrayList<Tornado> tornadoes = new ArrayList<>();
   public final ArrayList<Debris> debris = new ArrayList<>();
   public final ArrayList<Meteor> meteors = new ArrayList<>();
   public Quake quake;
   /** Ticks since the world was created (saved). */
   public long clock;
   public long nextQuake;
   public long nextTornado;
   public long nextShower;
   /** A meteor shower in progress: how many are still to come, and when the next one is launched. */
   public int showerLeft;
   public long showerAt;
   long frozenAt = -1L;
   Random rnd;
   /** How many of each started in this session (statistics for tests). */
   public int tornadoCount;
   public int quakeCount;
   public int meteorCount;
   public int impacts;

   Disasters(World w) {
      this.w = w;
      this.rnd = new Random(w.seed * 0x9E3779B97F4A7C15L ^ 0xD15A57E5L);
      this.nextQuake = 12000L + this.rnd.nextInt(24000);
      this.nextTornado = 9000L + this.rnd.nextInt(9000);
      this.nextShower = 20000L + this.rnd.nextInt(30000);
   }

   // ------------------------------------------------------------------ things
   public static final class Tornado {
      public double x;
      public double z;
      public double px;
      public double pz;
      /** unit heading and speed (blocks per tick) */
      public double dx;
      public double dz;
      public double speed;
      public double groundY;
      public double pGroundY;
      /** from the ground up to the wall cloud */
      public double height;
      public float size;
      public int age;
      public int life;
      /** 0..1: the funnel grows down out of the cloud (about 12 s), and ropes out again at the end */
      public float strength;
      public float pStrength;
      public float spin;
      public float pSpin;
      public long seed;

      /** radius of the funnel at a height fraction 0..1 (0 = ground) */
      public double radiusAt(double t) {
         return this.size * (1.5 + 1.2 * t + 9.0 * t * t);
      }
   }

   public static final class Quake {
      public double x;
      public double z;
      public int age;
      public int life;
      public float magnitude;
      public double radius;

      /** 0..1: builds up in a second, then dies away */
      public double intensity() {
         double up = Math.min(1.0, this.age / 20.0);
         double down = Math.pow(Math.max(0.0, 1.0 - this.age / (double)this.life), 0.7);
         return up * down;
      }
   }

   public static final class Meteor {
      public double x;
      public double y;
      public double z;
      public double px;
      public double py;
      public double pz;
      public double vx;
      public double vy;
      public double vz;
      public float size;
      public int age;
      public int flight;
      public boolean dead;
      /** the last positions, newest first: x, y, z per entry (for the tail) */
      public final float[] trail = new float[3 * 16];
      public int trailLen;
   }

   public static final class Debris {
      public double x;
      public double y;
      public double z;
      public double px;
      public double py;
      public double pz;
      public double vx;
      public double vy;
      public double vz;
      public int id;
      public int age;
      public float rx;
      public float ry;
      public float rz;
      public float vrx;
      public float vry;
      public float vrz;
      Tornado owner;
      double ang;
      double rad;
      double rfac;
      int orbit;
      boolean thrown;
   }

   // ------------------------------------------------------------------ switches
   public boolean active() {
      Game g = this.w.game;
      return this.enabled && (g == null || g.opt == null || g.opt.disasters);
   }

   public boolean anyActive() {
      return !this.tornadoes.isEmpty() || this.quake != null || !this.meteors.isEmpty() || this.showerLeft > 0 || !this.debris.isEmpty();
   }

   void say(String text) {
      if (this.w.game != null) {
         this.w.game.say(text);
      }
   }

   // ------------------------------------------------------------------ ground, what breaks
   /** The first air cell above the ground at (x, z): leaves, logs and plants are not ground, a water surface is. -1 when the chunk is not loaded. */
   public int ground(int x, int z) {
      Chunk c = this.w.chunkAt(x, z);
      if (c == null) {
         return -1;
      }

      int y = Math.min(255, c.hm[(z & 15) * 16 + (x & 15)] + 1);
      while (y > 1) {
         int id = this.w.getBlock(x, y - 1, z);
         if (Blocks.isWater(id) || id != 0 && Blocks.solid[id] && !Blocks.isLeaves(id) && !Blocks.isAnyLog(id) && Blocks.kind[id] == 0) {
            break;
         }

         y--;
      }

      return y;
   }

   /**
    * How easily a tornado tears this block out of the world (0..1; 0 = never): plants, leaves, torches, ladders and glass go at
    * once, wool, cakes, pumpkins, wooden furniture and planks need some more wind. Logs are not here (trees topple instead),
    * terrain, stone, bricks, ores, chests, furnaces and everything hard never move.
    */
   public static float fragility(int id) {
      if (id == 0 || Blocks.isFluid(id) || Blocks.hardness[id] < 0.0F || Blocks.hardness[id] > 3.0F) {
         return 0.0F;
      }

      if (id == Blocks.CHEST || id == Blocks.CHEST_LOOT || id == Blocks.FURNACE || id == Blocks.FURNACE_LIT || id == Blocks.SPAWNER || id == Blocks.TNT || Blocks.isFire(id)) {
         return 0.0F;
      }

      if (Blocks.isLeaves(id)) {
         return 1.0F;
      }

      if (Blocks.kind[id] == 1 || Blocks.kind[id] == 7 || id == Blocks.CACTUS || id == Blocks.TORCH || Blocks.isWallTorch(id) || Blocks.isLadder(id)) {
         return 1.0F;
      }

      if (id == Blocks.GLASS) {
         return 0.9F;
      }

      if (id >= 34 && id <= 49) {
         return 0.5F; // wool
      }

      if (Blocks.isCake(id) || id == Blocks.PUMPKIN || Blocks.isCarvedPumpkin(id) || Blocks.isJackOLantern(id) || id == Blocks.MELON) {
         return 0.7F;
      }

      if (id == 158 || id == Blocks.CRAFTING_TABLE || id == 160) {
         return 0.4F; // hay bale, crafting table, bookshelf
      }

      if (id == Blocks.PLANKS || id == Blocks.FENCE || Blocks.isDoor(id) || Blocks.isBed(id) || id >= 93 && id <= 96 || id == 113 || id >= Blocks.STAIR_TOP0 && id <= Blocks.STAIR_TOP0 + 3 || id == Blocks.SLAB_TOP0) {
         return 0.3F; // wooden building blocks
      }

      return 0.0F;
   }

   /** Terrain that crumbles off a cliff in an earthquake. */
   public static boolean crumbles(int id) {
      return id == Blocks.STONE || id == Blocks.GRASS || id == Blocks.DIRT || id == Blocks.SAND || id == Blocks.GRAVEL || id == Blocks.SANDSTONE || id == Blocks.SNOW || id >= Blocks.COAL_ORE && id <= Blocks.LAPIS_ORE;
   }

   /** Things an earthquake shakes loose: plants, torches, ladders, glass. */
   public static boolean shakesLoose(int id) {
      return id != 0 && (Blocks.kind[id] == 1 && !Blocks.isFire(id) || Blocks.kind[id] == 7 || id == Blocks.TORCH || Blocks.isWallTorch(id) || Blocks.isLadder(id) || id == Blocks.GLASS);
   }

   boolean airy(int id) {
      return id == 0 || Blocks.replaceable[id] && !Blocks.isFluid(id);
   }

   // ------------------------------------------------------------------ starting disasters
   /** A tornado at (x, z), heading (hx, hz) (any length), size 0.8..1.5. Its funnel grows down out of the cloud over the first 12 s unless {@code grown}. */
   public Tornado spawnTornado(double x, double z, double hx, double hz, float size, boolean grown) {
      Tornado t = new Tornado();
      t.x = t.px = x;
      t.z = t.pz = z;
      double len = Math.sqrt(hx * hx + hz * hz);
      if (len < 1.0E-6) {
         double a = this.rnd.nextDouble() * Math.PI * 2.0;
         hx = Math.cos(a);
         hz = Math.sin(a);
         len = 1.0;
      }

      t.dx = hx / len;
      t.dz = hz / len;
      t.size = size;
      t.speed = 0.075 + this.rnd.nextDouble() * 0.045;
      t.height = 64.0 + 8.0 * size;
      t.life = 2400 + this.rnd.nextInt(2400);
      t.seed = this.rnd.nextLong();
      int g = this.ground((int)Math.floor(x), (int)Math.floor(z));
      Game game = this.w.game;
      t.groundY = t.pGroundY = g >= 0 ? g : (game != null && game.player != null ? game.player.y : 70.0);
      if (grown) {
         t.age = 300;
         t.strength = t.pStrength = 1.0F;
      }

      this.tornadoes.add(t);
      this.tornadoCount++;
      return t;
   }

   public Quake startQuake(double x, double z, float magnitude, int life) {
      Quake q = new Quake();
      q.x = x;
      q.z = z;
      q.magnitude = Math.max(0.5F, Math.min(3.0F, magnitude));
      q.radius = 14.0 + 7.0 * q.magnitude;
      q.life = life;
      this.quake = q;
      this.quakeCount++;
      return q;
   }

   /** A meteor that hits the ground at (tx, tz) after {@code flight} ticks, coming in from the direction (fx, fz). */
   public Meteor spawnMeteor(double tx, double tz, double fx, double fz, int flight, float size) {
      int gy = this.ground((int)Math.floor(tx), (int)Math.floor(tz));
      double ty = gy >= 0 ? gy : 64.0;
      double len = Math.max(1.0E-6, Math.sqrt(fx * fx + fz * fz));
      Meteor m = new Meteor();
      m.flight = flight;
      m.size = size;
      double sx = tx + fx / len * 90.0;
      double sz = tz + fz / len * 90.0;
      double sy = ty + 150.0;
      m.x = m.px = sx;
      m.y = m.py = sy;
      m.z = m.pz = sz;
      m.vx = (tx - sx) / flight;
      m.vy = (ty - sy) / flight;
      m.vz = (tz - sz) / flight;
      this.meteors.add(m);
      this.meteorCount++;
      return m;
   }

   /** A shower of n meteors, one every 3-8 seconds, the first one after 'delay' ticks. */
   public void startShower(int n, int delay) {
      this.showerLeft = n;
      this.showerAt = this.clock + delay;
   }

   /** Ends everything that is going on: funnels rope out, the ground stops shaking, meteors burn out in the sky. */
   public void stopAll() {
      for (Tornado t : this.tornadoes) {
         t.life = Math.min(t.life, t.age + 120);
      }

      this.quake = null;
      this.meteors.clear();
      this.showerLeft = 0;
      this.debris.clear();
   }

   // ------------------------------------------------------------------ the tick
   public void tick() {
      this.clock++;
      Game g = this.w.game;
      if (FREEZE_AFTER > 0L && this.frozenAt >= 0L && this.clock - this.frozenAt > FREEZE_AFTER) {
         return;
      }

      if (g != null && g.player != null && !g.player.dead && this.clock % 40 == 0) {
         this.tickNatural(g.player);
      }

      if (this.showerLeft > 0 && this.clock >= this.showerAt && g != null && g.player != null) {
         this.launchShowerMeteor(g.player);
      }

      for (int i = this.tornadoes.size() - 1; i >= 0; i--) {
         Tornado t = this.tornadoes.get(i);
         this.tickTornado(t);
         boolean far = g != null && g.player != null && Math.hypot(t.x - g.player.x, t.z - g.player.z) > 900.0;
         if (t.age >= t.life || far) {
            this.tornadoes.remove(i);
         }
      }

      if (this.quake != null) {
         this.tickQuake(this.quake);
         if (this.quake.age >= this.quake.life) {
            this.quake = null;
         }
      }

      for (int i = this.meteors.size() - 1; i >= 0; i--) {
         Meteor m = this.meteors.get(i);
         this.tickMeteor(m);
         if (m.dead) {
            this.meteors.remove(i);
         }
      }

      if (!this.debris.isEmpty()) {
         this.tickDebris();
      }
   }

   /** Rolls for natural disasters (every two seconds): a quake now and then, a tornado under a thunderstorm, meteors at night. */
   void tickNatural(Player p) {
      if (!this.active()) {
         return;
      }

      if (this.quake == null && this.clock >= this.nextQuake) {
         double a = this.rnd.nextDouble() * Math.PI * 2.0;
         double d = this.rnd.nextDouble() * 30.0;
         float mag = 1.0F + this.rnd.nextFloat() * (this.rnd.nextInt(4) == 0 ? 2.0F : 1.0F);
         this.startQuake(p.x + Math.cos(a) * d, p.z + Math.sin(a) * d, mag, 180 + this.rnd.nextInt(140));
         this.nextQuake = this.clock + 24000L + this.rnd.nextInt(30000);
         this.say("The ground is shaking!");
      }

      Weather wx = this.w.weather;
      if (this.tornadoes.size() < 2 && this.clock >= this.nextTornado && wx.enabled && !wx.suppressed) {
         Weather.Snap s = wx.snapAt(0.0F);
         for (int tries = 0; tries < 12; tries++) {
            double a = this.rnd.nextDouble() * Math.PI * 2.0;
            double r = 90.0 + this.rnd.nextDouble() * 90.0;
            double x = p.x + Math.cos(a) * r;
            double z = p.z + Math.sin(a) * r;
            if (s.storm(x, z) > 0.3F && this.w.isLoaded((int)Math.floor(x), (int)Math.floor(z))) {
               // it heads for the player's neighbourhood, passing a few dozen blocks to one side
               double lateral = (this.rnd.nextDouble() - 0.5) * 70.0;
               double hx = p.x - x - Math.sin(a) * lateral;
               double hz = p.z - z + Math.cos(a) * lateral;
               this.spawnTornado(x, z, hx, hz, 0.9F + this.rnd.nextFloat() * 0.5F, false);
               this.nextTornado = this.clock + 20000L + this.rnd.nextInt(16000);
               this.say("A tornado is forming in the storm!");
               break;
            }
         }
      }

      long dayTick = this.w.dayTime % 24000L;
      boolean night = dayTick >= 13500L && dayTick <= 22500L;
      if (night && this.showerLeft == 0 && this.meteors.isEmpty() && this.clock >= this.nextShower) {
         this.startShower(6 + this.rnd.nextInt(8), 100 + this.rnd.nextInt(300));
         this.nextShower = this.clock + 36000L + this.rnd.nextInt(48000);
         this.say("Look at the sky - a meteor shower!");
      }
   }

   void launchShowerMeteor(Player p) {
      this.showerLeft--;
      this.showerAt = this.clock + 60 + this.rnd.nextInt(140);
      for (int tries = 0; tries < 8; tries++) {
         double a = this.rnd.nextDouble() * Math.PI * 2.0;
         double r = 40.0 + this.rnd.nextDouble() * 90.0;
         int x = (int)Math.floor(p.x + Math.cos(a) * r);
         int z = (int)Math.floor(p.z + Math.sin(a) * r);
         if (this.w.isLoaded(x, z)) {
            double b = this.rnd.nextDouble() * Math.PI * 2.0;
            this.spawnMeteor(x + 0.5, z + 0.5, Math.cos(b), Math.sin(b), 50 + this.rnd.nextInt(30), 1.0F + this.rnd.nextFloat() * 1.2F);
            return;
         }
      }
   }

   // ------------------------------------------------------------------ tornado
   void tickTornado(Tornado t) {
      t.px = t.x;
      t.pz = t.z;
      t.pGroundY = t.groundY;
      t.pStrength = t.strength;
      t.pSpin = t.spin;
      t.age++;
      if (t.age < 240) {
         t.strength = Math.max(t.strength, t.age / 240.0F);
      } else if (t.age > t.life - 240) {
         t.strength = Math.max(0.0F, (t.life - t.age) / 240.0F);
      } else {
         t.strength = 1.0F;
      }

      t.spin += 0.35F;
      // the heading wanders a little; the speed drops while the funnel forms and ropes out
      double a = Math.atan2(t.dz, t.dx) + (this.rnd.nextDouble() - 0.5) * 0.05 + Math.sin(t.age * 0.013 + (t.seed & 255)) * 0.004;
      t.dx = Math.cos(a);
      t.dz = Math.sin(a);
      double v = t.speed * (0.45 + 0.55 * t.strength);
      t.x += t.dx * v;
      t.z += t.dz * v;
      int g = this.ground((int)Math.floor(t.x), (int)Math.floor(t.z));
      if (g >= 0) {
         t.groundY += (g - t.groundY) * 0.25;
      }

      if (t.strength > 0.45F && this.w.isLoaded((int)Math.floor(t.x), (int)Math.floor(t.z))) {
         this.tornadoEntities(t);
         if (t.age % 4 == 0) {
            this.tornadoTrees(t);
         }

         this.tornadoBlocks(t);
         this.tornadoDust(t);
      }
   }

   /**
    * Trees close to the funnel are torn out and topple away from it - before the wind has stripped their leaves (a bare trunk is not a
    * tree any more). One tree per look: felling a tree is a lot of work for the world.
    */
   void tornadoTrees(Tornado t) {
      int reach = (int)Math.ceil(t.size * 5.5);
      int x0 = (int)Math.floor(t.x);
      int z0 = (int)Math.floor(t.z);
      for (int dx = -reach; dx <= reach; dx++) {
         for (int dz = -reach; dz <= reach; dz++) {
            if (dx * dx + dz * dz > reach * reach) {
               continue;
            }

            int x = x0 + dx;
            int z = z0 + dz;
            if (!this.w.isLoaded(x, z)) {
               continue;
            }

            int g0 = this.ground(x, z);
            if (g0 >= 2 && Blocks.isLog(this.w.getBlock(x, g0, z)) && this.rnd.nextFloat() < 0.3F * t.strength && this.uproot(x, g0, z, t.x, t.z)) {
               return;
            }
         }
      }
   }

   /**
    * The pull of a funnel on something at (ex, ey, ez): adds to vel (vx, vy, vz) and returns how deep inside it is (0 = out of reach,
    * 1 = in the core). Swirl, a pull towards the axis, and a lift that dies away 14 blocks above the ground.
    */
   double pull(Tornado t, double ex, double ey, double ez, double[] vel) {
      double rx = ex - t.x;
      double rz = ez - t.z;
      double dist = Math.sqrt(rx * rx + rz * rz);
      double hy = ey - t.groundY;
      if (hy < -3.0 || hy > t.height) {
         return 0.0;
      }

      double tt = Math.min(1.0, Math.max(0.0, hy / t.height));
      double reach = t.size * (5.0 + 12.0 * tt * tt) * (0.6 + 0.4 * t.strength);
      if (dist > reach) {
         return 0.0;
      }

      double k = 1.0 - dist / reach;
      double inv = dist < 1.0E-3 ? 0.0 : 1.0 / dist;
      double swirl = (0.09 * k + 0.02) * t.strength;
      double inward = 0.1 * (0.25 + k) * t.strength;
      vel[0] += -rz * inv * swirl - rx * inv * inward;
      vel[2] += rx * inv * swirl - rz * inv * inward;
      vel[1] += 0.2 * t.strength * (0.15 + k) * (1.0 - Math.min(1.0, Math.max(0.0, hy / 14.0)));
      return k;
   }

   void tornadoEntities(Tornado t) {
      Game g = this.w.game;
      double[] vel = new double[3];
      if (g != null && g.player != null && !g.player.dead && !g.player.spectator && !g.player.flying) {
         Player p = g.player;
         vel[0] = vel[1] = vel[2] = 0.0;
         double k = this.pull(t, p.x, p.y + 0.9, p.z, vel);
         if (k > 0.0) {
            p.vx = clampSpeed(p.vx + vel[0], 0.6);
            p.vz = clampSpeed(p.vz + vel[2], 0.6);
            p.vy = Math.min(0.5, p.vy + vel[1]);
            p.fallDistance = Math.min(p.fallDistance, 6.0F); // every landing in the swirl costs a few half hearts, not the whole fall
            if (k > 0.55 && t.age % 20 == 0) {
               g.damagePlayer(2, t.x, t.z);
            }
         }
      }

      if (g != null && g.mobs != null) {
         for (Mobs.Mob m : g.mobs.list) {
            if (m.dead) {
               continue;
            }

            vel[0] = vel[1] = vel[2] = 0.0;
            double k = this.pull(t, m.x, m.y + 0.5, m.z, vel);
            if (k > 0.0) {
               m.vx = clampSpeed(m.vx + vel[0], 0.5);
               m.vz = clampSpeed(m.vz + vel[2], 0.5);
               m.vy = Math.min(0.45, m.vy + vel[1]);
               m.onGround = false;
               if (k > 0.55 && t.age % 10 == 0) {
                  g.mobs.hurtMob(m, 2, t.x, t.z, 0.0, false);
               }
            }
         }
      }

      for (World.Item it : this.w.items) {
         vel[0] = vel[1] = vel[2] = 0.0;
         if (this.pull(t, it.x, it.y, it.z, vel) > 0.0) {
            it.vx = clampSpeed(it.vx + vel[0], 0.5);
            it.vz = clampSpeed(it.vz + vel[2], 0.5);
            it.vy = Math.min(0.4, it.vy + vel[1]);
         }
      }
   }

   static double clampSpeed(double v, double max) {
      return Math.max(-max, Math.min(max, v));
   }

   void tornadoBlocks(Tornado t) {
      int columns = 3 + (int)(5.0 * t.size * t.strength);
      double reach = t.size * 7.0;
      int lifted = 0;
      for (int c = 0; c < columns; c++) {
         double a = this.rnd.nextDouble() * Math.PI * 2.0;
         double rr = reach * Math.sqrt(this.rnd.nextDouble());
         int x = (int)Math.floor(t.x + Math.cos(a) * rr);
         int z = (int)Math.floor(t.z + Math.sin(a) * rr);
         int g0 = this.ground(x, z);
         if (g0 < 0) {
            continue;
         }

         int top = Math.min(254, g0 + 26);
         for (int y = Math.max(1, g0 - 1); y <= top; y++) {
            int id = this.w.getBlock(x, y, z);
            if (id == 0) {
               continue;
            }

            if (Blocks.isLog(id)) {
               if (y == g0 && this.rnd.nextInt(12) == 0) {
                  this.uproot(x, y, z, t.x, t.z);
               }

               continue;
            }

            float f = fragility(id);
            if (f > 0.0F && this.rnd.nextFloat() < f * 0.4F * (0.5F + 0.5F * t.strength)) {
               this.liftBlock(x, y, z, id, t);
               if (++lifted >= 8) {
                  return;
               }
            }
         }
      }
   }

   /** The tree whose base log is at (x, y, z) is torn out and falls away from the point (fromX, fromZ). */
   public boolean uproot(int x, int y, int z, double fromX, double fromZ) {
      int id = this.w.getBlock(x, y, z);
      if (!Blocks.isLog(id)) {
         return false;
      }

      this.w.setBlock(x, y, z, 0);
      Trees.Plan p = this.w.trees.collect(x, y, z);
      if (p != null && this.w.trees.start(p, x + 0.5 - fromX, z + 0.5 - fromZ, this.rnd)) {
         this.w.dropItem(x + 0.5, y + 0.5, z + 0.5, id, 1);
         return true;
      }

      this.w.setBlock(x, y, z, id);
      return false;
   }

   void liftBlock(int x, int y, int z, int id, Tornado t) {
      this.w.setBlock(x, y, z, 0);
      Game g = this.w.game;
      if (g != null) {
         g.spawnBreakParticles(x, y, z, id, 3);
      }

      boolean soft = Blocks.kind[id] == 1 || Blocks.kind[id] == 7 || Blocks.isLeaves(id) || id == Blocks.TORCH || Blocks.isWallTorch(id) || Blocks.isLadder(id);
      if (this.debris.size() < MAX_DEBRIS && (!soft || this.rnd.nextInt(3) == 0)) {
         Debris d = new Debris();
         d.id = id;
         d.x = d.px = x + 0.5;
         d.y = d.py = y + 0.5;
         d.z = d.pz = z + 0.5;
         d.owner = t;
         d.ang = Math.atan2(d.z - t.z, d.x - t.x);
         d.rad = Math.hypot(d.x - t.x, d.z - t.z);
         d.rfac = 0.4 + 0.6 * this.rnd.nextDouble();
         d.orbit = 50 + this.rnd.nextInt(110);
         d.rx = this.rnd.nextFloat() * 360.0F;
         d.ry = this.rnd.nextFloat() * 360.0F;
         d.rz = this.rnd.nextFloat() * 360.0F;
         d.vrx = (this.rnd.nextFloat() - 0.5F) * 14.0F;
         d.vry = (this.rnd.nextFloat() - 0.5F) * 14.0F;
         d.vrz = (this.rnd.nextFloat() - 0.5F) * 14.0F;
         this.debris.add(d);
      } else if (!soft && this.rnd.nextInt(4) == 0 && Blocks.drop[id] != 0) {
         this.w.dropItem(x + 0.5, y + 0.5, z + 0.5, Blocks.drop[id], 1);
      }
   }

   void tornadoDust(Tornado t) {
      Game g = this.w.game;
      if (g == null || g.particles.size() > MAX_PARTICLES) {
         return;
      }

      for (int i = 0; i < 3; i++) {
         double a = this.rnd.nextDouble() * Math.PI * 2.0;
         double r = t.size * (1.5 + 4.5 * this.rnd.nextDouble());
         Game.Particle p = new Game.Particle();
         p.x = (float)(t.x + Math.cos(a) * r);
         p.y = (float)(t.groundY + 0.3 + this.rnd.nextDouble() * 2.0);
         p.z = (float)(t.z + Math.sin(a) * r);
         p.vx = (float)(-Math.sin(a) * 0.16 * t.strength);
         p.vz = (float)(Math.cos(a) * 0.16 * t.strength);
         p.vy = 0.08F + this.rnd.nextFloat() * 0.1F;
         p.grav = -0.004F;
         p.size = 0.25F + this.rnd.nextFloat() * 0.3F;
         p.u = 1.0F / 256.0F;
         p.v = 65.0F / 256.0F;
         p.life = 20 + this.rnd.nextInt(20);
         float gr = 0.42F + this.rnd.nextFloat() * 0.18F;
         p.r = gr * 1.12F;
         p.g = gr;
         p.b = gr * 0.82F;
         g.particles.add(p);
      }
   }

   // ------------------------------------------------------------------ debris
   void tickDebris() {
      Game g = this.w.game;
      for (int i = this.debris.size() - 1; i >= 0; i--) {
         Debris d = this.debris.get(i);
         d.px = d.x;
         d.py = d.y;
         d.pz = d.z;
         d.age++;
         d.rx += d.vrx;
         d.ry += d.vry;
         d.rz += d.vrz;
         Tornado t = d.owner;
         if (t != null && !d.thrown && this.tornadoes.contains(t)) {
            double hy = d.y - t.groundY;
            double tt = Math.min(1.0, Math.max(0.0, hy / t.height));
            double omega = 0.30 - 0.14 * tt;
            d.ang += omega;
            double target = t.size * (1.5 + 7.0 * tt * tt) * d.rfac * 1.2 + 0.8;
            d.rad += (target - d.rad) * 0.12;
            d.x = t.x + Math.cos(d.ang) * d.rad;
            d.z = t.z + Math.sin(d.ang) * d.rad;
            d.y += 0.16 + 0.12 * d.rfac;
            if (d.age > d.orbit || hy > 42.0) {
               d.thrown = true;
               double sp = d.rad * omega * 0.8 + 0.15;
               d.vx = -Math.sin(d.ang) * sp + Math.cos(d.ang) * 0.2 + t.dx * t.speed;
               d.vz = Math.cos(d.ang) * sp + Math.sin(d.ang) * 0.2 + t.dz * t.speed;
               d.vy = 0.08 + 0.12 * this.rnd.nextDouble();
            }

            continue;
         }

         if (!d.thrown) {
            d.thrown = true; // its tornado is gone: it just falls
         }

         d.vy -= 0.04;
         d.vx *= 0.995;
         d.vz *= 0.995;
         d.vy *= 0.99;
         double nx = d.x + d.vx;
         double ny = d.y + d.vy;
         double nz = d.z + d.vz;
         int bx = (int)Math.floor(nx);
         int by = (int)Math.floor(ny);
         int bz = (int)Math.floor(nz);
         if (!this.w.isLoaded(bx, bz) || ny < 1.0 || d.age > 700) {
            this.debris.remove(i);
            continue;
         }

         int bid = this.w.getBlock(bx, by, bz);
         if (bid != 0 && Blocks.solid[bid] && !Blocks.isLeaves(bid)) {
            this.land(d);
            this.debris.remove(i);
            continue;
         }

         d.x = nx;
         d.y = ny;
         d.z = nz;
         if (g != null && d.age % 2 == 0 && Math.abs(d.vx) + Math.abs(d.vz) > 0.3) {
            if (this.hitEntities(d, g)) {
               this.debris.remove(i);
            }
         }
      }
   }

   /** Thrown debris hits whatever it flies into: 3 damage and it is gone. */
   boolean hitEntities(Debris d, Game g) {
      Player p = g.player;
      if (p != null && !p.dead && !p.spectator && Math.abs(p.x - d.x) < 0.6 && Math.abs(p.z - d.z) < 0.6 && d.y > p.y - 0.3 && d.y < p.y + 1.9) {
         g.damagePlayer(3, d.x - d.vx, d.z - d.vz);
         g.spawnBreakParticles((int)Math.floor(d.x), (int)Math.floor(d.y), (int)Math.floor(d.z), d.id, 4);
         return true;
      }

      if (g.mobs != null) {
         for (Mobs.Mob m : g.mobs.list) {
            if (!m.dead && Math.abs(m.x - d.x) < 0.7 && Math.abs(m.z - d.z) < 0.7 && d.y > m.y - 0.3 && d.y < m.y + 1.6) {
               g.mobs.hurtMob(m, 3, d.x - d.vx, d.z - d.vz, 0.3, false);
               g.spawnBreakParticles((int)Math.floor(d.x), (int)Math.floor(d.y), (int)Math.floor(d.z), d.id, 4);
               return true;
            }
         }
      }

      return false;
   }

   /** Debris has hit something solid: most of the solid kind drops down as a falling block, the rest breaks. */
   void land(Debris d) {
      int x = (int)Math.floor(d.px);
      int y = (int)Math.floor(d.py);
      int z = (int)Math.floor(d.pz);
      Game g = this.w.game;
      if (g != null) {
         g.spawnBreakParticles(x, y, z, d.id, 3);
      }

      boolean solidKind = Blocks.kind[d.id] != 1 && Blocks.kind[d.id] != 7 && !Blocks.isLeaves(d.id) && d.id != Blocks.GLASS && d.id != Blocks.TORCH && !Blocks.isWallTorch(d.id) && !Blocks.isLadder(d.id);
      int here = this.w.getBlock(x, y, z);
      if (solidKind && this.rnd.nextInt(10) < 6 && this.w.isLoaded(x, z) && (here == 0 || Blocks.replaceable[here] && !Blocks.isFluid(here))) {
         World.Falling f = new World.Falling();
         f.x = x + 0.5;
         f.y = y;
         f.z = z + 0.5;
         f.id = d.id;
         this.w.fallers.add(f);
      } else if (solidKind && this.rnd.nextInt(3) == 0 && Blocks.drop[d.id] != 0) {
         this.w.dropItem(x + 0.5, y + 0.5, z + 0.5, Blocks.drop[d.id], 1);
      }
   }

   // ------------------------------------------------------------------ earthquake
   void tickQuake(Quake q) {
      q.age++;
      double in = q.intensity();
      Game g = this.w.game;
      if (g != null && g.player != null) {
         double dist = Math.hypot(g.player.x - q.x, g.player.z - q.z);
         double felt = q.radius * 2.4;
         if (dist < felt) {
            g.addShake((float)(0.3 * q.magnitude * in * (1.0 - dist / felt)));
         }
      }

      int picks = 4 + (int)(q.magnitude * 9.0 * in);
      for (int n = 0; n < picks; n++) {
         double a = this.rnd.nextDouble() * Math.PI * 2.0;
         double r = q.radius * Math.sqrt(this.rnd.nextDouble());
         int x = (int)Math.floor(q.x + Math.cos(a) * r);
         int z = (int)Math.floor(q.z + Math.sin(a) * r);
         if (!this.w.isLoaded(x, z)) {
            continue;
         }

         this.shakeColumn(x, z, in);
      }

      if (g != null && g.player != null && g.particles.size() < MAX_PARTICLES && Math.hypot(g.player.x - q.x, g.player.z - q.z) < q.radius * 2.0) {
         for (int i = 0; i < 3; i++) {
            double a = this.rnd.nextDouble() * Math.PI * 2.0;
            double r = q.radius * Math.sqrt(this.rnd.nextDouble());
            int x = (int)Math.floor(q.x + Math.cos(a) * r);
            int z = (int)Math.floor(q.z + Math.sin(a) * r);
            int gy = this.ground(x, z);
            if (gy > 0) {
               Game.Particle p = new Game.Particle();
               p.x = x + 0.5F;
               p.y = gy + 0.1F;
               p.z = z + 0.5F;
               p.vx = (this.rnd.nextFloat() - 0.5F) * 0.04F;
               p.vz = (this.rnd.nextFloat() - 0.5F) * 0.04F;
               p.vy = 0.05F + this.rnd.nextFloat() * 0.06F;
               p.grav = -0.001F;
               p.size = 0.12F + this.rnd.nextFloat() * 0.16F;
               p.u = 1.0F / 256.0F;
               p.v = 65.0F / 256.0F;
               p.life = 25 + this.rnd.nextInt(25);
               float gr = 0.5F + this.rnd.nextFloat() * 0.15F;
               p.r = gr * 1.1F;
               p.g = gr;
               p.b = gr * 0.85F;
               g.particles.add(p);
            }
         }
      }
   }

   /** What the shaking does to one column (x, z): shakes loose a plant / glass / torch, topples a tree, or lets an overhang crumble. */
   void shakeColumn(int x, int z, double in) {
      int gy = this.ground(x, z);
      if (gy < 2) {
         return;
      }

      double roll = this.rnd.nextDouble();
      int above = this.w.getBlock(x, gy, z);
      if (Blocks.isLog(above) && this.rnd.nextDouble() < 0.22 * in) {
         this.uproot(x, gy, z, x + this.rnd.nextDouble() * 2.0 - 1.0 + (this.rnd.nextBoolean() ? 1 : -1) * 3.0, z + this.rnd.nextDouble() * 2.0 - 1.0);
         return;
      }

      if (roll < 0.5 * in) {
         // shaken loose: the plant / torch / glass / ladder in the first two cells above the ground
         for (int y = gy; y <= gy + 1; y++) {
            int id = this.w.getBlock(x, y, z);
            if (shakesLoose(id)) {
               if (id != Blocks.GLASS && Blocks.drop[id] != 0 && this.rnd.nextInt(2) == 0) {
                  this.w.dropItem(x + 0.5, y + 0.4, z + 0.5, Blocks.drop[id], 1);
               }

               if (this.w.game != null) {
                  this.w.game.spawnBreakParticles(x, y, z, id, 4);
               }

               this.w.setBlock(x, y, z, 0);
               return;
            }
         }
      }

      if (roll < 0.9 * in + 0.1) {
         // an unstable block of the surface (a cliff edge, an overhang) crumbles and falls
         for (int y = gy - 1; y >= Math.max(1, gy - 3); y--) {
            int id = this.w.getBlock(x, y, z);
            if (!crumbles(id)) {
               continue;
            }

            // a block with nothing under it falls where it is; one at the edge of a cliff slides off into the open column next to it
            int sx = x;
            int sz = z;
            boolean unstable = this.airy(this.w.getBlock(x, y - 1, z));
            int[][] around = new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            int first = this.rnd.nextInt(4);
            for (int i = 0; i < 4 && !unstable; i++) {
               int[] d = around[(first + i) & 3];
               int nx = x + d[0];
               int nz = z + d[1];
               if (this.w.isLoaded(nx, nz) && this.airy(this.w.getBlock(nx, y, nz)) && this.airy(this.w.getBlock(nx, y - 1, nz))) {
                  unstable = true;
                  sx = nx;
                  sz = nz;
               }
            }

            if (unstable && this.rnd.nextDouble() < 0.5) {
               this.w.setBlock(x, y, z, 0);
               World.Falling fl = new World.Falling();
               fl.x = sx + 0.5;
               fl.y = y;
               fl.z = sz + 0.5;
               fl.id = id;
               this.w.fallers.add(fl);
               if (this.w.game != null) {
                  this.w.game.spawnBreakParticles(x, y, z, id, 3);
               }
            }

            return;
         }
      }
   }

   // ------------------------------------------------------------------ meteors
   void tickMeteor(Meteor m) {
      m.px = m.x;
      m.py = m.y;
      m.pz = m.z;
      m.age++;
      // the tail: shift the trail by one entry
      int keep = Math.min(m.trailLen, 15);
      System.arraycopy(m.trail, 0, m.trail, 3, keep * 3);
      m.trail[0] = (float)m.x;
      m.trail[1] = (float)m.y;
      m.trail[2] = (float)m.z;
      m.trailLen = Math.min(16, m.trailLen + 1);
      Game g = this.w.game;
      // move in steps of less than a block so a thin roof cannot be flown through
      double speed = Math.sqrt(m.vx * m.vx + m.vy * m.vy + m.vz * m.vz);
      int steps = Math.max(1, (int)Math.ceil(speed / 0.8));
      boolean hit = false;
      boolean loaded = false;
      for (int i = 0; i < steps && !hit; i++) {
         m.x += m.vx / steps;
         m.y += m.vy / steps;
         m.z += m.vz / steps;
         int bx = (int)Math.floor(m.x);
         int by = (int)Math.floor(m.y);
         int bz = (int)Math.floor(m.z);
         loaded = this.w.isLoaded(bx, bz);
         hit = loaded && by >= 1 && by < 256 && Blocks.solid[this.w.getBlock(bx, by, bz)];
      }

      if (g != null && m.age % 2 == 0 && g.particles.size() < MAX_PARTICLES && g.player != null && Math.hypot(m.x - g.player.x, m.z - g.player.z) < 150.0) {
         g.flameParticle(m.x, m.y, m.z, this.rnd.nextInt(3) == 0);
      }

      if (hit || m.age >= m.flight + 4) {
         if (loaded) {
            this.impact(m);
         }

         m.dead = true;
      }
   }

   /** The meteor hits the ground: a blast (TNT size and up), fires, a crater floor of obsidian, a thump you can feel. */
   void impact(Meteor m) {
      this.impacts++;
      int bx = (int)Math.floor(m.x);
      int by = (int)Math.floor(m.y);
      int bz = (int)Math.floor(m.z);
      int power = Math.round(3.0F + m.size * 1.2F);
      Game g = this.w.game;
      if (g != null && g.mobs != null) {
         g.mobs.blast(m.x, m.y, m.z, power, null);
      } else {
         for (int dx = -power; dx <= power; dx++) {
            for (int dy = -power; dy <= power; dy++) {
               for (int dz = -power; dz <= power; dz++) {
                  int id = this.w.getBlock(bx + dx, by + dy, bz + dz);
                  if (dx * dx + dy * dy + dz * dz <= power * power && id != 0 && Blocks.breakable(id) && id != Blocks.OBSIDIAN && !Blocks.isFluid(id)) {
                     this.w.setBlock(bx + dx, by + dy, bz + dz, 0);
                  }
               }
            }
         }
      }

      // fires around the crater
      for (int i = 0; i < 18; i++) {
         int fx = bx + this.rnd.nextInt(2 * power + 5) - power - 2;
         int fz = bz + this.rnd.nextInt(2 * power + 5) - power - 2;
         for (int fy = by + power; fy >= by - power - 1; fy--) {
            if (this.w.fire.ignite(fx, fy, fz)) {
               break;
            }
         }
      }

      // what is left of the meteor: a few blocks of obsidian at the bottom of the crater
      int left = 1 + this.rnd.nextInt(3);
      for (int i = 0; i < left; i++) {
         int ox = bx + this.rnd.nextInt(3) - 1;
         int oz = bz + this.rnd.nextInt(3) - 1;
         for (int oy = by + 1; oy >= by - power - 1; oy--) {
            int below = this.w.getBlock(ox, oy - 1, oz);
            if (this.w.getBlock(ox, oy, oz) == 0 && below != 0 && Blocks.solid[below] && !Blocks.isLeaves(below)) {
               this.w.setBlock(ox, oy, oz, Blocks.OBSIDIAN);
               break;
            }
         }
      }

      if (g != null && g.player != null) {
         double dist = Math.hypot(g.player.x - m.x, g.player.z - m.z);
         g.addShake((float)Math.max(0.0, 0.9 * (1.0 - dist / 120.0)));
      }
   }

   // ------------------------------------------------------------------ commands
   /** /disaster [on|off|stop|tornado [distance]|quake [magnitude]|meteor [count]|status] */
   public void command(Game g, String[] a) {
      String sub = a.length > 1 ? a[1].toLowerCase() : "status";
      Player p = g.player;
      double yaw = Math.toRadians(p.yaw);
      double fx = -Math.sin(yaw);
      double fz = Math.cos(yaw);
      switch (sub) {
         case "on":
            this.enabled = true;
            g.say("Natural disasters are on (they are rare; /disaster tornado|quake|meteor starts one now)");
            break;
         case "off":
            this.enabled = false;
            g.say("Natural disasters are off in this world");
            break;
         case "stop":
         case "clear":
            this.stopAll();
            g.say("Disasters stopped");
            break;
         case "tornado": {
            double dist = a.length > 2 ? Math.max(20.0, Double.parseDouble(a[2])) : 80.0;
            double x = p.x + fx * dist;
            double z = p.z + fz * dist;
            // it comes towards the player, passing a little to one side
            double hx = p.x - x - fz * 25.0;
            double hz = p.z - z + fx * 25.0;
            Weather wx = this.w.weather;
            if (wx.enabled && !wx.suppressed && wx.forceMode == 0) {
               wx.force(2, 300L); // a grey, rainy sky for the occasion (a thunderstorm would also strike you with lightning)
            }

            this.spawnTornado(x, z, hx, hz, 1.0F + this.rnd.nextFloat() * 0.3F, false);
            g.say("A tornado is forming " + (int)dist + " blocks ahead and heading your way - get away from it!");
            break;
         }

         case "quake":
         case "earthquake": {
            float mag = a.length > 2 ? Float.parseFloat(a[2]) : 2.0F;
            double ang = this.rnd.nextDouble() * Math.PI * 2.0;
            this.startQuake(p.x + Math.cos(ang) * 12.0, p.z + Math.sin(ang) * 12.0, mag, 240);
            g.say("The ground is shaking! (magnitude " + String.format("%.1f", this.quake.magnitude) + ")");
            break;
         }

         case "meteor":
         case "meteors": {
            int n = a.length > 2 ? Math.max(1, Math.min(40, Integer.parseInt(a[2]))) : 1;
            this.startShower(n, 40);
            g.say(n == 1 ? "A meteor is coming..." : "A shower of " + n + " meteors is coming...");
            break;
         }

         default:
            StringBuilder sb = new StringBuilder("Disasters: " + (this.enabled ? "on" : "off") + (g.opt == null || g.opt.disasters ? "" : " (switched off in Options)"));
            sb.append("; active: ").append(this.tornadoes.size()).append(" tornado(es), ").append(this.quake != null ? "an earthquake, " : "no earthquake, ").append(this.meteors.size() + this.showerLeft).append(" meteor(s)");
            g.say(sb.toString());
            g.say("/disaster tornado [distance]   /disaster quake [1-3]   /disaster meteor [count]   /disaster stop   /disaster on|off");
      }
   }

   // ------------------------------------------------------------------ saving
   public void save(Properties p) {
      p.setProperty("disasters", this.enabled + "");
      p.setProperty("dclock", this.clock + "");
      p.setProperty("dquake", this.nextQuake + "");
      p.setProperty("dtornado", this.nextTornado + "");
      p.setProperty("dshower", this.nextShower + "");
   }

   public void load(Properties p) {
      this.enabled = !"false".equals(p.getProperty("disasters"));
      try {
         if (p.getProperty("dclock") != null) {
            this.clock = Long.parseLong(p.getProperty("dclock"));
            this.nextQuake = Long.parseLong(p.getProperty("dquake", this.nextQuake + ""));
            this.nextTornado = Long.parseLong(p.getProperty("dtornado", this.nextTornado + ""));
            this.nextShower = Long.parseLong(p.getProperty("dshower", this.nextShower + ""));
         }
      } catch (NumberFormatException e) {
         // keep the defaults of a new world
      }
   }
}
