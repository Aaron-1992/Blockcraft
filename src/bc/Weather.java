package bc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import java.util.Random;

/**
 * Update 3.8 - weather.
 * <p>
 * The weather is not one global switch like in Minecraft but a FIELD over the map that drifts with the wind:
 * <pre>  s(x, z, t) = 0.62 * mapA(p - offA(t)) + 0.38 * mapB(p - offB(t)) + climate(t) [+ forcing] [+ storm cells]</pre>
 * Two tileable noise maps move at slightly different speeds (so storms are born, grow and fade instead of just
 * sliding by) and a slow "climate" cycle makes some stretches dry and some stormy. Thresholds on s give the three
 * things you can SEE in the distance: cloud cover, rain and thunderstorms. A dark tower with a rain curtain hanging
 * below it is therefore exactly where it will rain when it gets to you.
 * <p>
 * The very same maps are uploaded to the GPU ({@link Shaders}) and sampled bilinearly on both sides, so the
 * volumetric clouds (shader path), the blocky clouds (no-shader path), the rain curtains and the game rules agree.
 * Everything about the field is a pure function of a {@link Snap}, so it can be evaluated on any thread.
 * <p>
 * Game rules (Minecraft 1.12 flavoured): rain puts fires out and waters farmland, zombies and skeletons do not burn in
 * it, lightning sets fire to what it hits, hurts what it hits and turns creepers into charged creepers. Cold biomes
 * get snow instead of rain, deserts stay dry. {@code /weather} controls it, {@code /weather off} switches it away.
 */
public final class Weather {
   // ------------------------------------------------------------------ the shared noise (CPU == GPU)
   /** Edge length of the two noise maps (texels). */
   public static final int NS = 256;
   /** The maps repeat every PA blocks (A) and PB blocks (B); every noise period used by the cloud shader divides PA. */
   public static final double PA = 9000.0;
   public static final double PB = 4500.0;
   static final double SA = 1.0 / PA;
   static final double SB = 1.0 / PB;
   /** Blocks per second layer A / B drift at (before the slow wobble). */
   static final double VA = 5.0;
   static final double VB = 3.7;
   /** Cloud altitudes (shared by the cloud renderers): the base is lower and the tops are far higher in a storm. */
   public static final float BASE_FAIR = 168.0F;
   public static final float BASE_STORM = 146.0F;

   public static final byte[] MAP_A = bytes(Shaders.fbm(NS, new int[]{3, 6, 12, 24}, new float[]{0.50F, 0.28F, 0.15F, 0.07F}, 88001L));
   public static final byte[] MAP_B = bytes(Shaders.fbm(NS, new int[]{2, 4, 8, 16}, new float[]{0.50F, 0.28F, 0.15F, 0.07F}, 88002L));

   /** cloud cover starts / is complete, rain starts / is full, thunderstorm starts / is full (quantiles of the field). */
   public static final float C0, C1, R0, R1, T0, T1;
   /** The cap of the field in fair weather (/weather clear, /weather off, a new world): scattered cumulus, about a quarter of the sky. */
   public static final float FAIR;
   /** Standard deviation of the field; the climate cycle swings by this much. */
   public static final float SIGMA;

   static {
      int n = 128;
      float[] v = new float[n * n];
      double sum = 0;
      for (int i = 0; i < n; i++) {
         for (int j = 0; j < n; j++) {
            double x = i * (PA / n);
            double z = j * (PA / n);
            v[i * n + j] = 0.62F * samp(MAP_A, x * SA, z * SA) + 0.38F * samp(MAP_B, x * SB, z * SB);
            sum += v[i * n + j];
         }
      }

      double mean = sum / v.length;
      double var = 0;
      for (float f : v) {
         var += (f - mean) * (f - mean);
      }

      SIGMA = (float)Math.sqrt(var / v.length);
      Arrays.sort(v);
      C0 = v[(int)(v.length * 0.42)];
      C1 = v[(int)(v.length * 0.74)];
      R0 = v[(int)(v.length * 0.83)];
      R1 = v[(int)(v.length * 0.905)];
      T0 = v[(int)(v.length * 0.925)];
      T1 = v[(int)(v.length * 0.975)];
      FAIR = C0 + 0.36F * (C1 - C0);
   }

   static byte[] bytes(float[] f) {
      byte[] b = new byte[f.length];
      for (int i = 0; i < f.length; i++) {
         b[i] = (byte)Math.round(Math.max(0.0F, Math.min(1.0F, f[i])) * 255.0F);
      }

      return b;
   }

   /** Bilinear sample of a repeating map exactly like OpenGL's GL_LINEAR + GL_REPEAT; u and v are in periods (1.0 = whole map). */
   static float samp(byte[] m, double u, double v) {
      double fx = u * NS - 0.5;
      double fy = v * NS - 0.5;
      int ix = (int)Math.floor(fx);
      int iy = (int)Math.floor(fy);
      float tx = (float)(fx - ix);
      float ty = (float)(fy - iy);
      int x0 = ix & (NS - 1);
      int x1 = ix + 1 & (NS - 1);
      int y0 = iy & (NS - 1);
      int y1 = iy + 1 & (NS - 1);
      float a = m[y0 * NS + x0] & 255;
      float b = m[y0 * NS + x1] & 255;
      float c = m[y1 * NS + x0] & 255;
      float d = m[y1 * NS + x1] & 255;
      float top = a + (b - a) * tx;
      float bot = c + (d - c) * tx;
      return (top + (bot - top) * ty) / 255.0F;
   }

   /** A storm cell's profile over its radius: a full thunderstorm out to half the radius, then rain, then overcast, gone at the rim. */
   static float cellShape(float d) {
      float t = Math.max(0.0F, Math.min(1.0F, (1.0F - d) * 2.0F));
      return t * t * (3.0F - 2.0F * t);
   }

   static float smooth(float e0, float e1, float x) {
      float t = Math.max(0.0F, Math.min(1.0F, (x - e0) / (e1 - e0)));
      return t * t * (3.0F - 2.0F * t);
   }

   // ------------------------------------------------------------------ state
   public final World world;
   public final long seed;
   /** /weather off: no rain, no thunder, only fair-weather clouds. */
   public boolean enabled = true;
   /** Set from the options menu ("Weather: OFF"): same effect as {@link #enabled} but not saved with the world. */
   public boolean suppressed;
   /** Test / screenshot hook (-Dbc.weather=cell:...): the natural sky is capped at fair-weather clouds, only cells and forcing add weather. */
   public boolean calm;
   /** Test / screenshot hook (-Dbc.lightning=off): no random lightning (bolts can still be made with {@link #strike}). */
   public boolean noLightning;
   /** A new world starts calm (fair-weather clouds, no rain) until this clock value; the calm fades out over the last minute. */
   public long calmUntil;
   /** Weather clock in ticks (saved with the world; independent of /time set). */
   public long ticks;
   /** 0 = natural, 1 = forced clear, 2 = forced rain, 3 = forced thunder. */
   public int forceMode;
   /** Clock value (ticks) when the forcing ends. */
   public long forceEnd;
   /** 0..1, how strongly the forcing is applied right now (it fades in and out). */
   public float forceAmt;
   /** Smoothed "the ground is wet" value (0..1) for the renderers. */
   public float wet;
   public final ArrayList<Cell> cells = new ArrayList<>();
   public final ArrayList<Bolt> bolts = new ArrayList<>();
   public long lightningCount;
   final Random rnd;
   // per-seed constants
   final double sox, soz, soBx, soBz, th0, thB, ph1, ph2, ph3, ph4, ph5, ph6;
   Snap tickSnap;

   public Weather(World w) {
      this(w, w == null ? 0L : w.seed);
   }

   public Weather(World w, long seed) {
      this.world = w;
      this.seed = seed;
      Random r = new Random(seed * -7046029254386353131L ^ 0x57EA7E12L);
      this.rnd = new Random(seed ^ 0x1F3A5C7EL);
      this.sox = r.nextDouble() * PA;
      this.soz = r.nextDouble() * PA;
      this.soBx = r.nextDouble() * PA;
      this.soBz = r.nextDouble() * PA;
      this.th0 = r.nextDouble() * Math.PI * 2.0;
      this.thB = this.th0 + (r.nextDouble() - 0.5) * 1.2;
      this.ph1 = r.nextDouble() * Math.PI * 2.0;
      this.ph2 = r.nextDouble() * Math.PI * 2.0;
      this.ph3 = r.nextDouble() * Math.PI * 2.0;
      this.ph4 = r.nextDouble() * Math.PI * 2.0;
      this.ph5 = r.nextDouble() * Math.PI * 2.0;
      this.ph6 = r.nextDouble() * Math.PI * 2.0;
      this.ticks = (long)(r.nextDouble() * 60000.0);
      this.tickSnap = this.snap(this.ticks / 20.0);
   }

   /** A new world starts under a fair sky: fair-weather clouds only for the first 5 minutes, then the natural weather fades in. */
   public void startFair() {
      this.calmUntil = this.ticks + 6000L;
      this.tickSnap = this.snap(this.ticks / 20.0);
   }

   // ------------------------------------------------------------------ the field
   /** Where layer A is (blocks) at time ts seconds, i.e. how far the pattern has drifted. Includes the per-world offset. */
   double offAx(double ts) {
      return this.sox + VA * ts * Math.cos(this.th0) + 220.0 * Math.sin(ts * (Math.PI * 2 / 540.0) + this.ph1);
   }

   double offAz(double ts) {
      return this.soz + VA * ts * Math.sin(this.th0) + 220.0 * Math.cos(ts * (Math.PI * 2 / 710.0) + this.ph2);
   }

   double offBx(double ts) {
      return this.soBx + VB * ts * Math.cos(this.thB) + 160.0 * Math.sin(ts * (Math.PI * 2 / 430.0) + this.ph3);
   }

   double offBz(double ts) {
      return this.soBz + VB * ts * Math.sin(this.thB) + 160.0 * Math.cos(ts * (Math.PI * 2 / 620.0) + this.ph4);
   }

   /** The slow climate cycle: some stretches are dry, some stormy (+-0.9 standard deviations of the field). */
   float climate(double ts) {
      double c = 0.62 * Math.sin(ts * (Math.PI * 2 / 3100.0) + this.ph5) + 0.38 * Math.sin(ts * (Math.PI * 2 / 1130.0) + this.ph6);
      return (float)(c * 0.9 * SIGMA);
   }

   /** Everything the field depends on at one moment, immutable, so renderers and mesher threads can use it freely. */
   public final class Snap {
      public final double ts;
      /** drift offsets of both layers in blocks (not reduced) */
      public final double oax, oaz, obx, obz;
      public final float bias;
      /** forcing: s = lerp(s, clamp(s, lo, hi), amt) */
      public final float lo, hi, amt;
      /** storm cells x, z, radius, strength (0..1); positions are already drifted to this moment */
      public final float[] cells;
      /** wind in blocks per second (the drift of layer A) */
      public final float windX, windZ;
      /** weather switched on (/weather off keeps only fair-weather clouds) */
      public final boolean on;
      /** how calm the natural sky is held: 1 = capped at fair-weather clouds, 0 = natural (new worlds, screenshots) */
      public final float calmAmt;

      Snap(double ts) {
         this.ts = ts;
         this.on = enabled && !suppressed;
         this.calmAmt = Weather.this.calm ? 1.0F : Math.max(0.0F, Math.min(1.0F, (float)((Weather.this.calmUntil - ts * 20.0) / 1200.0)));
         this.oax = offAx(ts);
         this.oaz = offAz(ts);
         this.obx = offBx(ts);
         this.obz = offBz(ts);
         this.bias = climate(ts);
         float l = -9.0F;
         float h = 9.0F;
         switch (forceMode) {
            case 1:
               h = FAIR; // clear = fair-weather clouds only, no rain, no storms
               break;
            case 2:
               l = R1 + 0.02F;
               break;
            case 3:
               l = T1 + 0.02F;
         }

         this.lo = l;
         this.hi = h;
         this.amt = forceMode == 0 ? 0.0F : forceAmt;
         float[] c = new float[Math.min(4, Weather.this.cells.size()) * 4];
         for (int i = 0; i < c.length / 4; i++) {
            Cell k = Weather.this.cells.get(i);
            c[i * 4] = (float)(k.x + (this.oax - k.oax));
            c[i * 4 + 1] = (float)(k.z + (this.oaz - k.oaz));
            c[i * 4 + 2] = k.radius;
            c[i * 4 + 3] = k.strength(ts * 20.0);
         }

         this.cells = c;
         double dt = 0.5;
         this.windX = (float)((offAx(ts + dt) - offAx(ts - dt)) / (2 * dt));
         this.windZ = (float)((offAz(ts + dt) - offAz(ts - dt)) / (2 * dt));
      }

      /** The raw weather value at a point. */
      public float s(double x, double z) {
         float a = samp(MAP_A, (x - this.oax) * SA, (z - this.oaz) * SA);
         float b = samp(MAP_B, (x - this.obx) * SB, (z - this.obz) * SB);
         float s = 0.62F * a + 0.38F * b + this.bias;
         if (this.calmAmt > 0.0F) {
            s += (Math.min(s, FAIR) - s) * this.calmAmt;
         }

         for (int i = 0; i < this.cells.length; i += 4) {
            double dx = x - this.cells[i];
            double dz = z - this.cells[i + 1];
            double r = this.cells[i + 2];
            if (dx * dx + dz * dz < r * r && this.cells[i + 3] > 0.0F) {
               s += Math.max(0.0F, T1 + 0.06F - s) * cellShape((float)(Math.sqrt(dx * dx + dz * dz) / r)) * this.cells[i + 3];
            }
         }

         if (this.amt > 0.0F) {
            s += (Math.max(this.lo, Math.min(this.hi, s)) - s) * this.amt;
         }

         return this.on ? s : Math.min(s, FAIR);
      }

      public float cover(double x, double z) {
         return smooth(C0, C1, this.s(x, z));
      }

      public float rain(double x, double z) {
         return this.on ? smooth(R0, R1, this.s(x, z)) : 0.0F;
      }

      public float storm(double x, double z) {
         return this.on ? smooth(T0, T1, this.s(x, z)) : 0.0F;
      }

      /** cover, rain, storm in one go (out has 3 floats). */
      public void at(double x, double z, float[] out) {
         float s = this.s(x, z);
         out[0] = smooth(C0, C1, s);
         out[1] = this.on ? smooth(R0, R1, s) : 0.0F;
         out[2] = this.on ? smooth(T0, T1, s) : 0.0F;
      }
   }

   public Snap snap(double ts) {
      return new Snap(ts);
   }

   /** The field at the current tick plus a fraction of the next one (what the renderers use). */
   public Snap snapAt(float partial) {
      return this.snap((this.ticks + partial) / 20.0);
   }

   /** Cloud base altitude where the storm intensity is {@code storm} (0..1). */
   public static float baseAlt(float storm) {
      return BASE_FAIR + (BASE_STORM - BASE_FAIR) * storm;
   }

   // ------------------------------------------------------------------ cells, forcing
   /** A storm cell that was summoned (/weather storm): drifts with layer A, grows, rains itself out. */
   public static final class Cell {
      public double x;
      public double z;
      /** where layer A was when the cell was born (the cell drifts by the difference) */
      public double oax;
      public double oaz;
      public float radius;
      public long born;
      public long life;

      float strength(double tick) {
         double age = tick - this.born;
         if (age < 0.0 || age > this.life) {
            return 0.0F;
         }

         float in = smooth(0.0F, 700.0F, (float)age);
         float out = 1.0F - smooth((float)(this.life - 1500), (float)this.life, (float)age);
         return in * out;
      }
   }

   /** Summons a thunderstorm {@code dist} blocks upwind of (px, pz) that drifts over that spot. Returns the cell. */
   public Cell summonStorm(double px, double pz, double dist) {
      Snap s = this.tickSnap;
      double wl = Math.max(0.001, Math.sqrt(s.windX * s.windX + s.windZ * s.windZ));
      double ux = s.windX / wl;
      double uz = s.windZ / wl;
      Cell c = new Cell();
      double lateral = (this.rnd.nextDouble() - 0.5) * 160.0;
      c.x = px - ux * dist - uz * lateral;
      c.z = pz - uz * dist + ux * lateral;
      c.oax = s.oax;
      c.oaz = s.oaz;
      c.radius = 420.0F + this.rnd.nextInt(120);
      c.born = this.ticks;
      c.life = 16000L;
      if (this.cells.size() >= 4) {
         this.cells.remove(0);
      }

      this.cells.add(c);
      this.tickSnap = this.snap(this.ticks / 20.0);
      return c;
   }

   /** A storm cell exactly at (x, z) right now, for tests and screenshots. */
   public Cell placeStorm(double x, double z, float radius) {
      Cell c = this.summonStorm(0.0, 0.0, 0.0);
      c.x = x;
      c.z = z;
      c.radius = radius;
      c.born = this.ticks - 800L;
      this.tickSnap = this.snap(this.ticks / 20.0);
      return c;
   }

   public void force(int mode, long seconds) {
      this.forceMode = mode;
      this.forceEnd = this.ticks + Math.max(1L, seconds) * 20L;
      this.tickSnap = this.snap(this.ticks / 20.0);
   }

   /** Applies the forcing at full strength immediately (tests, screenshots). */
   public void instant() {
      this.forceAmt = this.forceMode == 0 ? 0.0F : 1.0F;
      this.tickSnap = this.snap(this.ticks / 20.0);
   }

   public void natural() {
      this.forceMode = 0;
      this.forceAmt = 0.0F;
      this.cells.clear();
      this.tickSnap = this.snap(this.ticks / 20.0);
   }

   // ------------------------------------------------------------------ what it is like at a place
   /** Precipitation at the surface: 0 none, 1 rain, 2 snow (cold biome or high mountains), -1 dry biome. */
   public int precipKind(int x, int z) {
      int biome = -1;
      int h = 0;
      if (this.world != null) {
         Chunk c = this.world.chunkAt(x, z);
         if (c != null) {
            biome = c.biome[(z & 15) * 16 + (x & 15)];
            h = c.hm[(z & 15) * 16 + (x & 15)];
         } else {
            Gen.Col col = this.world.gen.column(x, z);
            biome = col.biome;
            h = col.h;
         }
      }

      if (biome == Gen.DESERT) {
         return -1;
      }

      if (biome == Gen.TUNDRA || biome == 6 && h >= 112) {
         return 2;
      }

      return 1;
   }

   /** Is it raining at this block (and nothing above it)? Used for putting fires out, farmland and zombies. */
   public boolean rainingAt(int x, int y, int z) {
      if (!this.enabled || this.suppressed || this.world == null || this.world.game == null) {
         return false;
      }

      if (this.tickSnap.rain(x + 0.5, z + 0.5) < 0.3F) {
         return false;
      }

      Chunk c = this.world.chunkAt(x, z);
      if (c == null || y <= c.hm[(z & 15) * 16 + (x & 15)]) {
         return false;
      }

      return this.precipKind(x, z) == 1;
   }

   /** Is a thunderstorm raging over this spot? (Sleeping through it is allowed.) */
   public boolean thunderingAt(double x, double z) {
      return this.enabled && !this.suppressed && this.tickSnap.storm(x, z) > 0.5F;
   }

   /** A short description of the weather at (x, z). */
   public String describe(double x, double z) {
      if (!this.enabled || this.suppressed) {
         return "off";
      }

      Snap s = this.tickSnap;
      float cover = s.cover(x, z);
      float rain = s.rain(x, z);
      float storm = s.storm(x, z);
      int kind = this.precipKind((int)Math.floor(x), (int)Math.floor(z));
      if (storm > 0.5F) {
         return kind == 2 ? "blizzard" : "thunderstorm";
      } else if (rain > 0.5F) {
         return kind == 2 ? "snow" : (kind < 0 ? "overcast (dry biome)" : "rain");
      } else if (rain > 0.05F) {
         return kind == 2 ? "light snow" : (kind < 0 ? "overcast (dry biome)" : "light rain");
      } else if (cover > 0.7F) {
         return "overcast";
      } else if (cover > 0.2F) {
         return "cloudy";
      } else {
         return "clear";
      }
   }

   /** Distance and compass direction of the nearest thunderstorm (storm value > 0.5) within 2500 blocks, or null. */
   public String nearestStorm(double px, double pz) {
      Snap s = this.tickSnap;
      double best = 1.0E18;
      double bx = 0;
      double bz = 0;
      for (int ring = 1; ring <= 25; ring++) {
         double r = ring * 100.0;
         for (int k = 0; k < 24; k++) {
            double a = k * Math.PI / 12.0;
            double x = px + Math.cos(a) * r;
            double z = pz + Math.sin(a) * r;
            if (r < best && s.storm(x, z) > 0.5F) {
               best = r;
               bx = x - px;
               bz = z - pz;
            }
         }
      }

      if (best > 1.0E17) {
         return null;
      }

      String[] names = new String[]{"east", "south-east", "south", "south-west", "west", "north-west", "north", "north-east"};
      double ang = Math.atan2(bz, bx);
      int idx = (int)Math.round(ang / (Math.PI / 4.0));
      return (int)best + " blocks to the " + names[(idx % 8 + 8) % 8];
   }

   // ------------------------------------------------------------------ ticking
   public void tick() {
      this.ticks++;
      if (this.forceMode != 0) {
         if (this.ticks < this.forceEnd) {
            this.forceAmt = Math.min(1.0F, this.forceAmt + 1.0F / 200.0F);
         } else {
            this.forceAmt = Math.max(0.0F, this.forceAmt - 1.0F / 400.0F);
            if (this.forceAmt <= 0.0F) {
               this.forceMode = 0;
            }
         }
      }

      for (int i = this.cells.size() - 1; i >= 0; i--) {
         Cell c = this.cells.get(i);
         if (this.ticks > c.born + c.life) {
            this.cells.remove(i);
         }
      }

      this.tickSnap = this.snap(this.ticks / 20.0);
      Game g = this.world == null ? null : this.world.game;
      if (g != null && g.player != null && this.enabled && !this.suppressed) {
         double px = g.player.x;
         double pz = g.player.z;
         float local = this.tickSnap.rain(px, pz);
         boolean wetting = local > 0.25F && this.precipKind((int)Math.floor(px), (int)Math.floor(pz)) == 1;
         this.wet = wetting ? Math.min(1.0F, this.wet + 1.0F / 300.0F) : Math.max(0.0F, this.wet - 1.0F / 1500.0F);
         this.tickLightning(g, px, pz);
         this.tickRainFx(g, px, pz);
      } else {
         this.wet = Math.max(0.0F, this.wet - 1.0F / 1500.0F);
      }

      for (int i = this.bolts.size() - 1; i >= 0; i--) {
         Bolt b = this.bolts.get(i);
         if (++b.age > b.life) {
            this.bolts.remove(i);
         }
      }
   }

   // ------------------------------------------------------------------ rain on the ground: splashes
   void tickRainFx(Game g, double px, double pz) {
      Snap s = this.tickSnap;
      float rain = s.rain(px, pz);
      if (rain < 0.05F || g.screen == Game.S.PAUSE) {
         return;
      }

      int n = (int)(rain * 14.0F) + 1;
      for (int i = 0; i < n; i++) {
         int x = (int)Math.floor(px) + this.rnd.nextInt(25) - 12;
         int z = (int)Math.floor(pz) + this.rnd.nextInt(25) - 12;
         Chunk c = this.world.chunkAt(x, z);
         if (c == null || this.precipKind(x, z) != 1) {
            continue;
         }

         int y = c.hm[(z & 15) * 16 + (x & 15)];
         int id = c.get(x & 15, y, z & 15);
         if (Blocks.isWater(id)) {
            g.splashParticle(x + this.rnd.nextDouble(), y + 0.88, z + this.rnd.nextDouble(), true);
         } else if (id != 0 && !Blocks.isLava(id)) {
            g.splashParticle(x + this.rnd.nextDouble(), y + 1.02, z + this.rnd.nextDouble(), false);
         }
      }
   }

   // ------------------------------------------------------------------ lightning
   /** A lightning bolt: a jagged channel from the cloud base to the ground, drawn for a fraction of a second. */
   public static final class Bolt {
      public double x;
      public double z;
      public double top;
      public double ground;
      public int age;
      public int life;
      /** segments: x0, y0, z0, x1, y1, z1, width (blocks) */
      public float[] seg;
      /** the strike hurts and ignites (it is close to the player, in a loaded area) */
      public boolean real;

      /** Brightness of the flash 0..1 over its lifetime: a hard first stroke and one or two weaker re-strikes. */
      public float env(float partial) {
         float t = this.age + partial;
         float e = 0.0F;
         if (t < 3.0F) {
            e = 1.0F;
         } else if (t < 5.0F) {
            e = 0.25F;
         } else if (t < 8.0F) {
            e = 0.85F;
         } else if (t < 10.0F) {
            e = 0.15F;
         } else if (t < 13.0F) {
            e = 0.55F - (t - 10.0F) * 0.15F;
         } else if (t < this.life) {
            e = Math.max(0.0F, 0.12F * (1.0F - (t - 13.0F) / Math.max(1.0F, this.life - 13.0F)));
         }

         return e;
      }
   }

   static float[] boltGeometry(long seed, double x, double top, double z, double ground) {
      Random r = new Random(seed);
      ArrayList<float[]> segs = new ArrayList<>();
      double sx = x + (r.nextDouble() - 0.5) * 60.0;
      double sz = z + (r.nextDouble() - 0.5) * 60.0;
      int steps = Math.max(8, Math.min(46, (int)((top - ground) / 7.0)));
      double cx = sx;
      double cz = sz;
      double cy = top;
      for (int i = 1; i <= steps; i++) {
         double f = (double)i / steps;
         double ty = top + (ground - top) * f;
         // the channel wanders but is pulled towards the strike point more and more
         double pull = f * f;
         double tx = sx + (x - sx) * f + (r.nextDouble() - 0.5) * 16.0 * (1.0 - pull) * 0.9;
         double tz = sz + (z - sz) * f + (r.nextDouble() - 0.5) * 16.0 * (1.0 - pull) * 0.9;
         if (i == steps) {
            tx = x;
            tz = z;
            ty = ground;
         }

         segs.add(new float[]{(float)cx, (float)cy, (float)cz, (float)tx, (float)ty, (float)tz, 1.0F});
         if (r.nextInt(4) == 0 && i < steps - 2) {
            // a fork that leaves sideways and fades out
            double fx = tx;
            double fy = ty;
            double fz = tz;
            double dx = (r.nextDouble() - 0.5) * 22.0;
            double dz = (r.nextDouble() - 0.5) * 22.0;
            int fl = 2 + r.nextInt(4);
            for (int k = 0; k < fl; k++) {
               double nx = fx + dx + (r.nextDouble() - 0.5) * 7.0;
               double nz = fz + dz + (r.nextDouble() - 0.5) * 7.0;
               double ny = fy - 5.0 - r.nextDouble() * 7.0;
               segs.add(new float[]{(float)fx, (float)fy, (float)fz, (float)nx, (float)ny, (float)nz, 0.5F - k * 0.07F});
               fx = nx;
               fy = ny;
               fz = nz;
            }
         }

         cx = tx;
         cy = ty;
         cz = tz;
      }

      float[] out = new float[segs.size() * 7];
      for (int i = 0; i < segs.size(); i++) {
         System.arraycopy(segs.get(i), 0, out, i * 7, 7);
      }

      return out;
   }

   int surfaceY(int x, int z) {
      if (this.world != null) {
         Chunk c = this.world.chunkAt(x, z);
         if (c != null) {
            return c.hm[(z & 15) * 16 + (x & 15)];
         }

         return Math.max(62, this.world.gen.column(x, z).h);
      }

      return 64;
   }

   /** Makes a bolt strike at (x, z). Close strikes in loaded terrain also hurt and ignite things. */
   public Bolt strike(double x, double z, boolean allowEffects) {
      Snap s = this.tickSnap;
      Bolt b = new Bolt();
      b.x = x;
      b.z = z;
      int gy = this.surfaceY((int)Math.floor(x), (int)Math.floor(z));
      b.ground = gy + 1;
      b.top = baseAlt(s.storm(x, z)) + 6.0;
      b.life = 22;
      b.seg = boltGeometry(this.rnd.nextLong(), x, b.top, z, b.ground);
      Game g = this.world == null ? null : this.world.game;
      if (allowEffects && g != null && g.player != null) {
         double dx = x - g.player.x;
         double dz = z - g.player.z;
         if (dx * dx + dz * dz < 140.0 * 140.0 && this.world.isLoaded((int)Math.floor(x), (int)Math.floor(z))) {
            b.real = true;
            this.strikeEffects(g, (int)Math.floor(x), gy, (int)Math.floor(z));
         }
      }

      this.lightningCount++;
      this.bolts.add(b);
      return b;
   }

   /** What a bolt that hits (x, y, z) does: fire, damage, charged creepers. */
   void strikeEffects(Game g, int x, int y, int z) {
      World w = this.world;
      int hit = w.getBlock(x, y, z);
      if (!Blocks.isWater(hit) && !Blocks.isLava(hit)) {
         w.fire.ignite(x, y + 1, z);
         for (int k = 0; k < 4; k++) {
            int fx = x + this.rnd.nextInt(5) - 2;
            int fz = z + this.rnd.nextInt(5) - 2;
            int fy = y + this.rnd.nextInt(3);
            if (w.getBlock(fx, fy, fz) == 0 && w.fire.canPlace(fx, fy, fz)) {
               w.fire.ignite(fx, fy, fz);
            }
         }
      }

      for (Mobs.Mob m : g.mobs.list) {
         if (m.dead) {
            continue;
         }

         double dx = m.x - (x + 0.5);
         double dz = m.z - (z + 0.5);
         if (dx * dx + dz * dz < 12.0 && Math.abs(m.y - y) < 6.0) {
            if (m.type == 6) {
               m.charged = true;
            } else {
               g.mobs.hurtMob(m, 8, x + 0.5, z + 0.5, 0.0, false);
               m.burn = Math.max(m.burn, 160);
            }
         }
      }

      Player p = g.player;
      double dx = p.x - (x + 0.5);
      double dz = p.z - (z + 0.5);
      if (dx * dx + dz * dz < 12.0 && Math.abs(p.y - y) < 6.0 && !p.creative && !p.dead) {
         p.invuln = 0;
         g.damagePlayer(5, x + 0.5, z + 0.5);
         p.burn = Math.max(p.burn, 160);
      }
   }

   void tickLightning(Game g, double px, double pz) {
      Snap s = this.tickSnap;
      if (g.screen == Game.S.PAUSE || this.noLightning) {
         return;
      }

      // close strikes: only while a thunderstorm is roughly overhead; about one per 12 seconds at full strength
      float here = s.storm(px, pz);
      if (here > 0.45F && this.rnd.nextFloat() < here * here / 240.0F) {
         double a = this.rnd.nextDouble() * Math.PI * 2.0;
         double r = 12.0 + Math.sqrt(this.rnd.nextDouble()) * 85.0;
         this.strike(px + Math.cos(a) * r, pz + Math.sin(a) * r, true);
      }

      // far strikes (a show in the distance): every few ticks look at random places up to 1500 blocks away
      if (this.ticks % 6 == 0) {
         double best = 0;
         double bx = 0;
         double bz = 0;
         for (int i = 0; i < 90; i++) {
            double a = this.rnd.nextDouble() * Math.PI * 2.0;
            double r = 120.0 + Math.pow(this.rnd.nextDouble(), 1.5) * 1400.0;
            double x = px + Math.cos(a) * r;
            double z = pz + Math.sin(a) * r;
            float st = s.storm(x, z);
            if (st > 0.45F && st > best) {
               best = st;
               bx = x;
               bz = z;
            }
         }

         if (best > 0 && this.rnd.nextFloat() < 0.10F + 0.14F * best) {
            this.strike(bx, bz, false);
         }
      }
   }

   /** Sky flash 0..1 as seen from (x, z): near bolts are blinding, far ones only light up the clouds a little. */
   public float flashAt(double x, double z, float partial) {
      float f = 0.0F;
      for (Bolt b : this.bolts) {
         double dx = b.x - x;
         double dz = b.z - z;
         double d2 = dx * dx + dz * dz;
         f = Math.max(f, b.env(partial) * (float)(0.16 + 0.84 / (1.0 + d2 / (260.0 * 260.0))));
      }

      return Math.min(1.0F, f);
   }

   // ------------------------------------------------------------------ persistence
   public void save(Properties p) {
      p.setProperty("wclock", this.ticks + "");
      p.setProperty("wenabled", this.enabled + "");
      p.setProperty("wforce", this.forceMode + "," + this.forceEnd + "," + this.forceAmt);
      p.setProperty("wcalm", this.calmUntil + "");
      StringBuilder sb = new StringBuilder();
      for (Cell c : this.cells) {
         sb.append(c.x).append(',').append(c.z).append(',').append(c.oax).append(',').append(c.oaz).append(',').append(c.radius).append(',')
            .append(c.born).append(',').append(c.life).append(';');
      }

      p.setProperty("wcells", sb.toString());
   }

   public void load(Properties p) {
      try {
         String t = p.getProperty("wclock");
         if (t == null) {
            this.startFair(); // an old world: starts under a fair sky
            return;
         }

         this.ticks = Long.parseLong(t.trim());
         this.enabled = !"false".equals(p.getProperty("wenabled"));
         String[] f = p.getProperty("wforce", "0,0,0").split(",");
         this.forceMode = Integer.parseInt(f[0].trim());
         this.forceEnd = Long.parseLong(f[1].trim());
         this.forceAmt = Float.parseFloat(f[2].trim());
         this.calmUntil = Long.parseLong(p.getProperty("wcalm", "0").trim());
         this.cells.clear();
         for (String c : p.getProperty("wcells", "").split(";")) {
            String[] v = c.split(",");
            if (v.length >= 7) {
               Cell k = new Cell();
               k.x = Double.parseDouble(v[0]);
               k.z = Double.parseDouble(v[1]);
               k.oax = Double.parseDouble(v[2]);
               k.oaz = Double.parseDouble(v[3]);
               k.radius = Float.parseFloat(v[4]);
               k.born = Long.parseLong(v[5]);
               k.life = Long.parseLong(v[6]);
               this.cells.add(k);
            }
         }
      } catch (Exception e) {
         this.natural();
      }

      this.tickSnap = this.snap(this.ticks / 20.0);
   }
}
