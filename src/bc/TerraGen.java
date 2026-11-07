package bc;

import java.util.Random;

/**
 * Update 3.13: the new Overworld generator. Terrain is still a height map with caves cut into it (so {@link #column} stays cheap for the LOD,
 * the villages and the weather), but the map now comes from five climate fields: continentalness (oceans, coasts, inland), erosion (flat or
 * rugged), a ridge noise for the peaks, temperature and humidity. They pick one of 28 {@link Biomes}; rivers are carved along the zero lines
 * of another noise, and every biome has its own surface, trees and plants.
 */
public final class TerraGen extends Gen {
   private static final double[] SPLINE_C = new double[]{-1.0, -0.6, -0.35, -0.16, -0.05, 0.02, 0.10, 0.30, 0.60, 1.0};
   private static final double[] SPLINE_H = new double[]{22.0, 32.0, 44.0, 55.0, 61.0, 64.0, 66.0, 72.0, 86.0, 100.0};

   final WorldSettings st;
   /** Update 6.21: the layered cave generator and the underground structures (only terrainVer >= 3). */
   final CaveWorks caves;
   final double bs;
   final double vs;
   /** Update 6.22: global tree-density multiplier from the world settings. */
   final double treeDensity;
   /** Update 6.23: height/radius scale and the Giant-tier switch. */
   final float treeScale;
   final boolean giantTrees;
   final Noise nC;
   final Noise nE;
   final Noise nT;
   final Noise nH;
   final Noise nPk;
   final Noise nMt;
   final Noise nRv;
   final Noise nD1;
   final Noise nD2;
   final Noise nVar;
   final Noise nMu;
   final Noise nSw;
   final Noise nTr;
   final Noise nS2;
   final Noise nRav;
   final Noise nRavZ;
   final Noise nRavW;
   final Noise nFl;
   final Noise nBad;
   /** Update 6.22: low-frequency warps for meandering, smooth biome borders and mountain terraces. */
   final Noise nBlend;
   final Noise nTerrace;
   /** The terracotta bands of the badlands, one entry per y. */
   final int[] bands = new int[256];

   public TerraGen(long seed, WorldSettings settings) {
      super(seed);
      this.st = settings;
      this.settings = settings;
      this.caves = new CaveWorks(seed, settings, this, this.nCave1, this.nCave2, this.nCavern, this.nEntr);
      this.treeDensity = settings.treeDensity < 0.4F ? 0.4F : (settings.treeDensity > 1.4F ? 1.4F : settings.treeDensity);
      this.treeScale = settings.treeScale < 0.7F ? 0.7F : (settings.treeScale > 1.5F ? 1.5F : settings.treeScale);
      this.giantTrees = settings.giantTrees;
      this.bs = settings.type == WorldSettings.LARGE ? 4.0 : 1.0;
      this.vs = settings.type == WorldSettings.AMPLIFIED ? 1.7 : 1.0;
      Random r = new Random(seed * 0x2545F4914F6CDD1DL + 0x7E44A6E13L);
      this.nC = new Noise(r.nextLong());
      this.nE = new Noise(r.nextLong());
      this.nT = new Noise(r.nextLong());
      this.nH = new Noise(r.nextLong());
      this.nPk = new Noise(r.nextLong());
      this.nMt = new Noise(r.nextLong());
      this.nRv = new Noise(r.nextLong());
      this.nD1 = new Noise(r.nextLong());
      this.nD2 = new Noise(r.nextLong());
      this.nVar = new Noise(r.nextLong());
      this.nMu = new Noise(r.nextLong());
      this.nSw = new Noise(r.nextLong());
      this.nTr = new Noise(r.nextLong());
      this.nS2 = new Noise(r.nextLong());
      this.nRav = new Noise(r.nextLong());
      this.nRavZ = new Noise(r.nextLong());
      this.nRavW = new Noise(r.nextLong());
      this.nFl = new Noise(r.nextLong());
      this.nBad = new Noise(r.nextLong());
      this.nBlend = new Noise(r.nextLong());
      this.nTerrace = new Noise(r.nextLong());
      int[] pal = new int[]{Blocks.STAINED0 + 1, Blocks.STAINED0, Blocks.STAINED0 + 4, Blocks.STAINED0 + 12, Blocks.STAINED0 + 14, Blocks.STAINED0 + 8, Blocks.TERRACOTTA};
      int y = 0;
      while (y < 256) {
         int id = pal[r.nextInt(pal.length)];
         int len = 1 + r.nextInt(3);
         for (int i = 0; i < len && y < 256; i++) {
            this.bands[y++] = id;
         }
      }
   }

   /** Everything the generator knows about a column before it is built. */
   static final class Cl {
      double c;
      double e;
      double t;
      double hu;
      double v;
      double mtn;
      double rf;
      double sw;
      double bad;
      double isle;
      double tEff;
      int h;
      int biome;
      /** Update 6.22: neighbour biome across a climate border and the 0..1 border weight. */
      int altBiome;
      double blend;
   }

   static final ThreadLocal<Cl[]> GRID = ThreadLocal.withInitial(() -> {
      Cl[] a = new Cl[18 * 18];
      for (int i = 0; i < a.length; i++) {
         a[i] = new Cl();
      }

      return a;
   });

   static double spline(double c) {
      if (c <= SPLINE_C[0]) {
         return SPLINE_H[0];
      }

      for (int i = 1; i < SPLINE_C.length; i++) {
         if (c <= SPLINE_C[i]) {
            double f = (c - SPLINE_C[i - 1]) / (SPLINE_C[i] - SPLINE_C[i - 1]);
            return SPLINE_H[i - 1] + (SPLINE_H[i] - SPLINE_H[i - 1]) * f;
         }
      }

      return SPLINE_H[SPLINE_H.length - 1];
   }

   /** The climate values at which pick() changes biome; distance to these defines a border band. */
   private static final double[] T_EDGE = {0.2, 0.4, 0.62};
   private static final double[] HU_EDGE = {0.3, 0.33, 0.36, 0.44, 0.5, 0.52, 0.62, 0.7};

   static double climateEdge(double t, double hu) {
      double e = 1.0;
      for (double v : T_EDGE) e = Math.min(e, Math.abs(t - v));
      for (double v : HU_EDGE) e = Math.min(e, Math.abs(hu - v));
      return e;
   }

   // ------------------------------------------------------------------------------------------------ the climate
   void sample(int x, int z, Cl o) {
      double k = 1.0 / this.bs;
      double c = clamp(this.nC.fbm(x * k / 760.0, z * k / 760.0, 5) * 1.9 + 0.14, -1.0, 1.0);
      double e = clamp(this.nE.fbm(x * k / 460.0 + 91.0, z * k / 460.0 - 47.0, 4) * 1.8, -1.0, 1.0);
      double t = clamp(this.nT.fbm(x * k / 900.0 + 500.0, z * k / 900.0, 3) * 0.84 + 0.5, 0.0, 1.0);
      double hu = clamp(this.nH.fbm(x * k / 760.0 - 300.0, z * k / 760.0 + 120.0, 3) * 0.88 + 0.5, 0.0, 1.0);
      double v = clamp(this.nVar.fbm(x / 170.0 + 17.0, z / 170.0 - 9.0, 2) * 0.9 + 0.5, 0.0, 1.0);
      // Update 6.22: warp the climate coordinates with two slow noises so biome boundaries meander
      // instead of following straight contour lines. Old worlds (biomeBorders = 0) are untouched.
      final int borders = this.st.biomeBorders;
      if (borders > 0) {
         double amp = borders >= 2 ? 0.052 : 0.030;
         t = clamp(t + amp * this.nBlend.fbm(x / 38.0, z / 38.0, 2)
            + amp * 0.30 * this.nTerrace.fbm(x / 15.0 + 9.0, z / 15.0 - 4.0, 2), 0.0, 1.0);
         hu = clamp(hu + amp * this.nBlend.fbm(x / 38.0 + 61.0, z / 38.0 - 27.0, 2)
            + amp * 0.28 * this.nTerrace.fbm(x / 17.0 - 5.0, z / 17.0 + 13.0, 2), 0.0, 1.0);
      }

      double coast = smooth(-0.03, 0.16, c);
      double rough = (2.0 + 14.0 * smooth(0.40, -0.55, e)) * (0.3 + 0.7 * coast);
      double d1 = this.nD1.fbm(x / 85.0, z / 85.0, 5);
      double d2 = this.nD2.fbm(x / 13.0 + 3.0, z / 13.0, 2);
      double hd = spline(c) + d1 * rough * 1.45 + d2 * (0.6 + 1.6 * coast * (0.4 + 0.6 * smooth(0.3, -0.5, e)));
      if (this.vs > 1.0) {
         hd += smooth(0.0, 0.5, c) * 10.0;
      }

      // mountains: where the land is high and rugged, in blobs
      boolean bigTerrain = this.st.terrainVer >= 2;
      double mtnBase = bigTerrain ? smooth(0.05, 0.42, c) * smooth(0.15, -0.50, e)
         : smooth(0.12, 0.46, c) * smooth(0.08, -0.42, e);
      double mtn = 0.0;
      if (mtnBase > 0.0) {
         double mm = this.nMt.fbm(x * k / 380.0 + 211.0, z * k / 380.0 + 55.0, 3);
         mtn = mtnBase * smooth(-0.05, 0.30, mm);
         if (mtn > 0.0) {
            double pk = clamp(1.0 - Math.abs(this.nPk.fbm(x * k / 210.0, z * k / 210.0, 4)) * 2.3, 0.0, 1.0);
            double ms = bigTerrain ? this.st.mountainScale : 1.0;
            hd += mtn * (12.0 + 118.0 * pk * pk * this.vs) * ms;
            if (ms > 1.0) {
               // Update 6.19: sharper ridges on top of the taller peaks
               double ridge = 1.0 - Math.abs(this.nPk.fbm(x * k / 95.0 + 7.0, z * k / 95.0 - 11.0, 3));
               hd += mtn * 26.0 * ridge * ridge * ridge * (ms - 1.0);
            }
         }
      }
      if (hd > 168.0) {
         hd = 168.0 + (hd - 168.0) * 0.55;
      }

      if (hd > 212.0) {
         hd = 212.0 + (hd - 212.0) * 0.3;
      }

      // Update 6.22: soft mountain terraces (wide natural shelves instead of a smooth cone).
      if (borders > 0 && mtn > 0.06 && hd > 76.0) {
         double stepH = 7.0;
         double f = hd / stepH;
         double fl = Math.floor(f);
         double terr = stepH * (fl + smooth(0.60, 0.97, f - fl));
         hd += (terr - hd) * 0.30 * Math.min(1.0, mtn * 2.2) * (borders >= 2 ? 1.25 : 1.0);
      }

      // badlands: stepped mesas in the hot, dry corners of the map
      double bad = smooth(0.60, 0.68, t) * smooth(0.45, 0.30, hu) * smooth(0.10, 0.30, c) * smooth(0.45, 0.58, v);
      if (bad > 0.0) {
         double mesa = 66.0 + 34.0 * smooth(-0.15, 0.55, this.nBad.fbm(x / 110.0, z / 110.0, 3));
         double f = (mesa - 64.0) / 5.0;
         double fl = Math.floor(f);
         double stepped = 64.0 + 5.0 * (fl + smooth(0.55, 0.95, f - fl));
         hd = hd + (stepped - hd) * bad;
      }

      // swamps: low, flat ground with pools
      double sw = smooth(0.60, 0.68, hu) * smooth(0.36, 0.46, t) * smooth(0.82, 0.66, t) * smooth(76.0, 67.0, hd) * smooth(0.04, 0.2, c);
      if (sw > 0.0) {
         double sh = 62.5 + this.nSw.fbm(x / 20.0, z / 20.0, 2) * 3.4;
         hd = hd + (sh - hd) * sw;
      }

      // mushroom islands in the deep sea
      double isleC = smooth(-0.30, -0.54, c);
      double isle = 0.0;
      if (isleC > 0.0) {
         double mu = this.nMu.fbm(x / 300.0 + 5.0, z / 300.0 - 8.0, 3);
         isle = smooth(0.30, 0.50, mu) * isleC;
         if (isle > 0.0) {
            hd = hd + (67.0 + d2 * 1.4 + 5.0 * smooth(0.4, 0.62, mu) - hd) * isle;
         }
      }

      // rivers along the zero lines of a noise; the distance to the line (in blocks) makes them the same width everywhere
      double rf = 0.0;
      if (c > -0.12 && !(isle > 0.2) && hd < 104.0) {
         double rx = x / 540.0 + 33.0;
         double rz = z / 540.0 - 71.0;
         double rn = this.nRv.fbm(rx, rz, 3);
         if (Math.abs(rn) < 0.10) {
            double gx = this.nRv.fbm(rx + 6.0 / 540.0, rz, 3) - this.nRv.fbm(rx - 6.0 / 540.0, rz, 3);
            double gz = this.nRv.fbm(rx, rz + 6.0 / 540.0, 3) - this.nRv.fbm(rx, rz - 6.0 / 540.0, 3);
            double grad = Math.max(1.0e-5, Math.sqrt(gx * gx + gz * gz) / 12.0);
            double dist = Math.abs(rn) / grad;
            rf = (1.0 - smooth(4.5, 21.0, dist)) * smooth(-0.12, 0.03, c) * smooth(104.0, 80.0, hd);
            if (rf > 0.0) {
               double bed = 56.0 + d2 * 1.1;
               if (hd > bed) {
                  hd += (bed - hd) * rf;
               }
            }

            // Update 6.22: a broad, smooth valley around the river instead of a knife-edge trench.
            if (borders > 0) {
               double valley = (1.0 - smooth(8.0, 44.0, dist)) * 0.26 * smooth(-0.12, 0.03, c) * smooth(104.0, 76.0, hd);
               hd += (62.0 + d2 * 0.5 - hd) * valley;
            }
         }
      }

      // Update 6.22: flatten the shoreline into wider, gentler beaches.
      if (borders > 0 && c > -0.06 && c < 0.18) {
         double w = smooth(57.0, 62.0, hd) * (1.0 - smooth(65.0, 71.0, hd));
         hd += (62.0 - hd) * w * 0.35;
      }

      if (st.aquatic && c < -.08 && isle < .2) {
         double trench=smooth(.28,.60,Math.abs(nRav.noise2(x/95.0,z/95.0)));
         hd -= 9 * smooth(-.08,-.30,c) * trench;
      }
      o.c = c;
      o.e = e;
      o.t = t;
      o.hu = hu;
      o.v = v;
      o.mtn = mtn;
      o.rf = rf;
      o.sw = sw;
      o.bad = bad;
      o.isle = isle;
      o.h = (int)clamp(Math.floor(hd), 4.0, 245.0);
      o.tEff = t - Math.max(0.0, o.h - 82.0) * 0.0035;
      int b0 = pick(o);
      double bandW = borders > 0 ? (borders >= 2 ? 0.065 : 0.038) : 0.0;
      double blend = borders > 0 ? clamp(1.0 - climateEdge(t, hu) / bandW, 0.0, 1.0) : 0.0;
      int alt = -1;
      if (blend > 0.0 && !Biomes.ocean(b0) && !Biomes.river(b0) && o.h >= 61) {
         double t0 = o.t, hu0 = o.hu;
         o.t = clamp(t0 + bandW, 0.0, 1.0);
         int a1 = pick(o);
         o.t = clamp(t0 - bandW, 0.0, 1.0);
         int a2 = pick(o);
         o.t = t0;
         o.hu = clamp(hu0 + bandW, 0.0, 1.0);
         int a3 = pick(o);
         o.hu = clamp(hu0 - bandW, 0.0, 1.0);
         int a4 = pick(o);
         o.hu = hu0;
         alt = a1 != b0 ? a1 : (a2 != b0 ? a2 : (a3 != b0 ? a3 : (a4 != b0 ? a4 : -1)));
         if (alt >= 0 && (Biomes.ocean(alt) || Biomes.river(alt))) alt = -1;
      }

      o.biome = b0;
      o.altBiome = alt;
      o.blend = blend;
      if (st.aquatic && Biomes.ocean(o.biome)) o.biome=Aquatic.ocean(t,o.h);
   }

   static int pick(Cl o) {
      int h = o.h;
      double t = o.t;
      double hu = o.hu;
      double v = o.v;
      double c = o.c;
      double te = o.tEff;
      if (o.isle > 0.5 && h >= 61) {
         return Biomes.MUSHROOM;
      }

      if (o.sw > 0.5 && h <= 70) {
         return Biomes.SWAMP;
      }

      if (h < 63) {
         if (o.rf > 0.2 && c > 0.0) {
            return t < 0.2 ? Biomes.FROZEN_RIVER : Biomes.RIVER;
         }

         if (t < 0.17) {
            return Biomes.FROZEN_OCEAN;
         }

         if (h < 45) {
            return Biomes.DEEP_OCEAN;
         }

         return t > 0.80 ? Biomes.WARM_OCEAN : Biomes.OCEAN;
      }

      if (c < 0.058 && h <= 65 && o.rf < 0.3) {
         if (o.mtn > 0.02 || o.e < -0.3) {
            return Biomes.STONE_SHORE;
         }

         return te < 0.2 ? Biomes.SNOWY_BEACH : Biomes.BEACH;
      }

      if (o.mtn > 0.4 && h >= 92 || h >= 120) {
         return te < 0.26 || h >= 150 ? Biomes.FROZEN_PEAKS : Biomes.MOUNTAINS;
      }

      if (o.mtn > 0.1 && h >= 78 && hu > 0.25 && te > 0.22 && te < 0.78) {
         return Biomes.MEADOW;
      }

      if (t < 0.2) {
         if (hu >= 0.5) {
            return Biomes.SNOWY_TAIGA;
         }

         return v > 0.8 && o.e > 0.0 && hu < 0.45 ? Biomes.ICE_SPIKES : Biomes.ICE_PLAINS;
      }

      if (t < 0.4) {
         if (hu < 0.3) {
            return Biomes.PLAINS;
         }

         if (hu < 0.7) {
            return Biomes.TAIGA;
         }

         return v > 0.35 ? Biomes.MEGA_TAIGA : Biomes.TAIGA;
      }

      if (t < 0.62) {
         if (hu < 0.33) {
            return Biomes.PLAINS;
         }

         if (hu < 0.5) {
            return v > 0.68 ? Biomes.FLOWER_FOREST : (v < 0.3 ? Biomes.BIRCH_FOREST : Biomes.FOREST);
         }

         if (hu < 0.7) {
            return v > 0.7 ? Biomes.DARK_FOREST : (v < 0.4 ? Biomes.BIRCH_FOREST : Biomes.FOREST);
         }

         return Biomes.DARK_FOREST;
      }

      if (hu < 0.36) {
         return o.bad > 0.5 ? Biomes.BADLANDS : Biomes.DESERT;
      }

      if (hu < 0.52) {
         return o.bad > 0.5 && hu < 0.44 ? Biomes.BADLANDS : Biomes.SAVANNA;
      }

      if (hu < 0.62) {
         return Biomes.SAVANNA;
      }

      return Biomes.JUNGLE;
   }

   @Override
   public Gen.Col column(int x, int z) {
      Cl s = new Cl();
      this.sample(x, z, s);
      Gen.Col c = new Gen.Col();
      c.h = s.h;
      c.biome = s.biome;
      c.t = s.t;
      c.hu = s.hu;
      c.altBiome = s.altBiome;
      c.blend = (float)s.blend;
      return c;
   }

   int heightAt(int x, int z) {
      Cl s = new Cl();
      this.sample(x, z, s);
      return s.h;
   }

   // ------------------------------------------------------------------------------------------------ colours
   int grassColor(Cl s) {
      int g = grassColorFor(s.t, s.hu);
      if (s.sw > 0.0) {
         g = lerpColor(g, 0x5E6B35, Math.min(1.0, s.sw * 1.6));
      }

      if (s.biome == Biomes.BADLANDS || s.bad > 0.0) {
         g = lerpColor(g, 0x90814D, Math.min(1.0, s.bad * 1.4));
      }

      // Update 6.22: fade a biome's special tint out across a border band instead of flipping it.
      float solid = 1.0F - (float)s.blend * 0.65F;
      if (s.biome == Biomes.DARK_FOREST) {
         g = lerpColor(g, 0x2F5A22, 0.45F * solid);
      }

      if (s.biome == Biomes.MEADOW) {
         g = lerpColor(g, 0x7BC25A, 0.4F * solid);
      }

      return g;
   }

   int foliageColor(Cl s) {
      int g = foliageColorFor(s.t, s.hu);
      if (s.sw > 0.0) {
         g = lerpColor(g, 0x4B5F2C, Math.min(1.0, s.sw * 1.6));
      }

      if (s.biome == Biomes.BADLANDS) {
         g = lerpColor(g, 0x9E814D, 0.7);
      }

      float solid = 1.0F - (float)s.blend * 0.65F;
      if (s.biome == Biomes.DARK_FOREST) {
         g = lerpColor(g, 0x244E1A, 0.5F * solid);
      }

      return g;
   }

   // ------------------------------------------------------------------------------------------------ surface
   /** rule[0] top, [1] filler, [2] filler depth below the top, [3] block under the filler, [4] its depth. */
   void surface(Cl s, int slope, int wx, int wz, int[] rule) {
      int b = s.biome;
      int h = s.h;
      // Update 6.22: across a border band the two biomes' surfaces interleave column by column,
      // so sand turns into grass (or podzol into dirt) gradually instead of at one hard line.
      if (s.blend > 0.42 && s.altBiome >= 0 && s.altBiome != b && this.st.biomeBorders > 0) {
         double chance = (s.blend - 0.42) / 0.58 * 0.48;
         long hsh = CaveWorks.mix(this.seed ^ 0x424F52444552L, wx, 0, wz);
         if ((hsh & 255) < chance * 256.0) b = s.altBiome;
      }

      double n = this.nSurf.noise2(wx * 0.08, wz * 0.08);
      double n2 = this.nS2.noise2(wx * 0.03 + 40.0, wz * 0.03);
      rule[0] = Blocks.GRASS;
      rule[1] = Blocks.DIRT;
      rule[2] = 3 + (n > 0.0 ? 1 : 0);
      rule[3] = Blocks.STONE;
      rule[4] = 0;
      if (h < 63) {
         // the floor of a sea, a lake or a river
         rule[2] = 3;
         switch (b) {
            case Biomes.DEEP_COLD_OCEAN:
            case Biomes.DEEP_OCEAN:
               rule[0] = n2 > 0.1 ? Blocks.GRAVEL : (n > 0.2 ? Blocks.CLAY : Blocks.GRAVEL);
               rule[1] = Blocks.GRAVEL;
               return;
            case Biomes.DEEP_FROZEN_OCEAN:
            case Biomes.COLD_OCEAN:
            case Biomes.FROZEN_OCEAN:
            case Biomes.FROZEN_RIVER:
               rule[0] = n2 > 0.0 ? Blocks.GRAVEL : Blocks.SAND;
               rule[1] = Blocks.GRAVEL;
               return;
            case Biomes.LUKEWARM_OCEAN:
            case Biomes.DEEP_LUKEWARM_OCEAN:
            case Biomes.WARM_OCEAN:
               rule[0] = n > 0.35 ? Blocks.CLAY : Blocks.SAND;
               rule[1] = Blocks.SAND;
               return;
            case Biomes.RIVER:
               rule[0] = n > 0.25 ? Blocks.GRAVEL : (n < -0.28 ? Blocks.CLAY : Blocks.SAND);
               rule[1] = rule[0] == Blocks.GRAVEL ? Blocks.GRAVEL : Blocks.SAND;
               return;
            case Biomes.SWAMP:
               rule[0] = n > 0.1 ? Blocks.DIRT : (n < -0.2 ? Blocks.CLAY : Blocks.DIRT);
               rule[1] = Blocks.DIRT;
               return;
            case Biomes.MUSHROOM:
               rule[0] = Blocks313.MYCELIUM;
               rule[1] = Blocks.DIRT;
               return;
            default:
               rule[0] = n2 > 0.25 ? Blocks.GRAVEL : (n > 0.3 ? Blocks.CLAY : Blocks.SAND);
               rule[1] = rule[0] == Blocks.GRAVEL ? Blocks.GRAVEL : Blocks.SAND;
               return;
         }
      }

      if (s.rf > 0.12 && h <= 65 && b != Biomes.DESERT && b != Biomes.BADLANDS && b != Biomes.MUSHROOM) {
         rule[0] = n > 0.3 ? Blocks.GRAVEL : (n < -0.32 ? Blocks.CLAY : Blocks.SAND);
         rule[1] = rule[0] == Blocks.GRAVEL ? Blocks.GRAVEL : Blocks.SAND;
         rule[2] = 3;
         return;
      }

      switch (b) {
         case Biomes.BEACH:
         case Biomes.SNOWY_BEACH:
            rule[0] = Blocks.SAND;
            rule[1] = Blocks.SAND;
            rule[2] = 3;
            rule[3] = Blocks.SANDSTONE;
            rule[4] = 3;
            return;
         case Biomes.DESERT:
            rule[0] = Blocks.SAND;
            rule[1] = Blocks.SAND;
            rule[2] = 4;
            rule[3] = Blocks.SANDSTONE;
            rule[4] = 4;
            return;
         case Biomes.STONE_SHORE:
            rule[0] = n > 0.3 ? Blocks.GRAVEL : Blocks.STONE;
            rule[1] = Blocks.STONE;
            rule[2] = 3;
            return;
         case Biomes.BADLANDS:
            if (slope >= 3 || h > 78) {
               rule[0] = this.bands[h & 255];
               rule[1] = this.bands[h & 255];
               rule[2] = 12;
            } else {
               rule[0] = Blocks313.RED_SAND;
               rule[1] = Blocks313.RED_SAND;
               rule[2] = 3;
               rule[3] = this.bands[(h - 4) & 255];
               rule[4] = 6;
            }

            return;
         case Biomes.MOUNTAINS:
         case Biomes.FROZEN_PEAKS: {
            double snowline = 112.0 + (0.5 - s.t) * 44.0;
            if (this.st.biomeBorders > 0) {
               // a broken snow line, not a ruler-straight one
               snowline += (CaveWorks.mix(this.seed ^ 0x534E4F57L, wx, 0, wz) & 15) - 7;
            }

            if (b == Biomes.FROZEN_PEAKS) {
               snowline = Math.min(snowline, 128.0);
            }

            // bare rock on the steep faces; above the snow line only the really steep ones and, in patches, the steep ones (not a speckle of stone in the snow)
            boolean above = h >= snowline || b == Biomes.FROZEN_PEAKS;
            if (above ? (slope >= 6 || slope >= 4 && n2 > 0.15) : slope >= 4) {
               rule[0] = Blocks.STONE;
               rule[1] = Blocks.STONE;
               rule[2] = 3;
            } else if (h >= snowline || b == Biomes.FROZEN_PEAKS && slope <= 3) {
               rule[0] = n2 > 0.35 && h > 160 ? Blocks313.PACKED_ICE : Blocks.SNOW;
               rule[1] = Blocks.STONE;
               rule[2] = 2;
            } else if (h > 98 || slope >= 3) {
               rule[0] = n > 0.28 ? Blocks.GRAVEL : Blocks.STONE;
               rule[1] = Blocks.STONE;
               rule[2] = 3;
            } else {
               rule[0] = Blocks.GRASS;
               rule[1] = Blocks.DIRT;
               rule[2] = 3;
            }

            return;
         }

         case Biomes.MUSHROOM:
            rule[0] = Blocks313.MYCELIUM;
            return;
         case Biomes.ICE_SPIKES:
            rule[0] = Blocks.SNOW;
            rule[1] = Blocks.DIRT;
            rule[2] = 3;
            return;
         case Biomes.MEGA_TAIGA:
            rule[0] = n2 > -0.25 ? Blocks313.PODZOL : Blocks.GRASS;
            return;
         case Biomes.TAIGA:
            rule[0] = n2 > 0.4 ? Blocks313.PODZOL : Blocks.GRASS;
            return;
         case Biomes.SAVANNA:
            rule[0] = n2 > 0.3 && slope >= 1 ? Blocks313.COARSE_DIRT : Blocks.GRASS;
            return;
         case Biomes.MEADOW:
            if (slope >= 4) {
               rule[0] = Blocks.STONE;
               rule[1] = Blocks.STONE;
            }

            return;
         default:
            return;
      }
   }

   // ------------------------------------------------------------------------------------------------ ravines
   /** 0 = no ravine here, up to 1 in the middle of one. */
   double ravine(int x, int z) {
      boolean big = this.st.terrainVer >= 2;
      double zone = this.nRavZ.noise2(x / 420.0 + 12.0, z / 420.0 - 5.0);
      // V6.28: with real cave entrances available, the endless surface cracks
      // no longer need to be everywhere.
      double need = big ? (this.st.caveVer >= 4 ? 0.30 : (this.st.caveVer >= 3 ? 0.16 : 0.05)) : 0.2;
      if (zone < need) {
         return 0.0;
      }

      double d = Math.abs(this.nRav.fbm(x / 170.0, z / 170.0, 2));
      double width = (0.011 + 0.010 * (this.nRavW.noise2(x / 33.0, z / 33.0) * 0.5 + 0.5))
         * (big ? this.st.ravineScale : 1.0);
      return d >= width ? 0.0 : 1.0 - d / width;
   }

   /** Update 6.19: a larger, connected cave system for new worlds. Uses the *existing* noise fields with
    *  different scales/offsets so old seeds and old saves keep their exact terrain. */
   boolean carveBig(int x, int y, int z) {
      float cs = Math.max(0.6F, this.st.caveScale);
      float dens = Math.max(0.4F, this.st.caveDensity);
      double k = 1.0 / cs;
      // 1. wide worm tunnels (the old caves, stretched in all three axes)
      double a = this.nCave1.noise(x * 0.024 * k, y * 0.030 * k, z * 0.024 * k);
      double b = this.nCave2.noise(x * 0.024 * k + 31.7, y * 0.030 * k + 11.3, z * 0.024 * k - 47.1);
      if (a * a + b * b < 0.0055 * dens * (0.8 + 0.6 * cs)) return true;
      // 2. a second, larger-scale network that joins the first into long systems
      double c = this.nCave1.noise(x * 0.013 * k + 91.0, y * 0.021 * k - 17.0, z * 0.013 * k + 53.0);
      double d = this.nCave2.noise(x * 0.013 * k - 29.0, y * 0.021 * k + 7.0, z * 0.013 * k + 13.0);
      if (c * c + d * d < 0.0038 * dens) return true;
      // 3. deep cheese caverns
      if (y < 54) {
         double e = this.nCavern.noise(x * 0.009 * k, y * 0.016 * k, z * 0.009 * k);
         if (e > 0.50 - 0.10 * (dens - 1.0F) - 0.06 * (cs - 1.0F)) return true;
      }
      return false;
   }

   /** Update 6.19: more cave mouths so the big systems are actually reachable. */
   boolean entranceBig(int x, int z) {
      if (this.st.terrainVer < 2) return this.entrance(x, z);
      return this.nEntr.noise2(x * 0.010, z * 0.010) > 0.30;
   }

   // ------------------------------------------------------------------------------------------------ the chunk
   @Override
   public Chunk generate(int cx, int cz) {
      Chunk ch = new Chunk(cx, cz);
      short[] b = ch.blocks;
      Cl[] g = GRID.get();
      for (int gz = 0; gz < 18; gz++) {
         for (int gx = 0; gx < 18; gx++) {
            this.sample(cx * 16 + gx - 1, cz * 16 + gz - 1, g[gz * 18 + gx]);
         }
      }

      Random r = new Random(this.seed * 31L + cx * 341873128712L + cz * 132897987541L);
      final boolean ver3 = this.st.terrainVer >= 3;
      final boolean cc = ver3 && this.st.caveVer >= 2;
      final boolean cc3 = ver3 && this.st.caveVer >= 3;   // V6.28 entrances + larger caverns
      final boolean cc4 = ver3 && this.st.caveVer >= 4;   // V6.29 walk-in entrances
      int[] hs = new int[256];
      int[] bm = new int[256];
      int[] rule = new int[5];
      for (int z = 0; z < 16; z++) {
         for (int x = 0; x < 16; x++) {
            Cl s = g[(z + 1) * 18 + x + 1];
            int i = z * 16 + x;
            int wx = cx * 16 + x;
            int wz = cz * 16 + z;
            int h = s.h;
            hs[i] = h;
            bm[i] = s.biome;
            ch.biome[i] = (byte)s.biome;
            ch.grassColor[i] = this.grassColor(s);
            ch.foliageColor[i] = this.foliageColor(s);
            ch.waterColor[i] = Biomes.waterTint(s.biome);
            int slope = Math.max(Math.max(Math.abs(h - g[(z + 1) * 18 + x].h), Math.abs(h - g[(z + 1) * 18 + x + 2].h)), Math.max(Math.abs(h - g[z * 18 + x + 1].h), Math.abs(h - g[(z + 2) * 18 + x + 1].h)));
            this.surface(s, slope, wx, wz, rule);
            int top = rule[0];
            int fill = rule[1];
            int fd = rule[2];
            int under = rule[3];
            int ud = rule[4];
            boolean banded = s.biome == Biomes.BADLANDS && h >= 60;
            for (int y = 0; y <= h; y++) {
               int id = 1;
               int d = h - y;
               if (y == 0) {
                  id = 6;
               } else if (y <= 4 && r.nextInt(5) < 5 - y) {
                  id = 6;
               } else if (d == 0) {
                  id = top;
               } else if (d <= fd) {
                  id = fill;
               } else if (ud > 0 && d <= fd + ud) {
                  id = under;
               } else if (banded && y >= 44) {
                  id = this.bands[y];
               }

               b[Chunk.idx(x, y, z)] = (short)id;
            }

            for (int y = h + 1; y < 63; y++) {
               b[Chunk.idx(x, y, z)] = 50;
            }

            if (h < 63 && (s.biome == Biomes.FROZEN_OCEAN || s.biome == Biomes.DEEP_FROZEN_OCEAN || s.biome == Biomes.FROZEN_RIVER)) {
               b[Chunk.idx(x, 62, z)] = (short)Blocks313.ICE;
            }

            // caves
            CaveCliffs.Mouth[] ms = cc3 && (cc4 || h >= 64) ? this.caves.mouths(wx, wz) : null;
            boolean inMouth = false;
            if (ms != null) {
               for (CaveCliffs.Mouth m : ms) if (m.surfaceHole(wx, wz)) { inMouth = true; break; }
            }
            int top2 = h - 3;
            // V6.29: with real walk-in cave mouths, the old noise sinkholes and
            // vertical shafts (the endless cracks in the ground) are switched off.
            boolean mouth = !cc4 && h >= 64 && (ver3 ? this.caves.mouth(wx, wz) : this.entranceBig(wx, wz)) && s.biome != Biomes.MUSHROOM;
            boolean shaft = ver3 && !cc4 && CaveWorks.shaft(this.seed, wx, wz);
            if (mouth || shaft || inMouth) {
               top2 = h;
            }

            if (h < 64) {
               top2 = Math.min(top2, h - 6);
            }

            boolean bigCaves = this.st.terrainVer >= 2;
            for (int y = 6; y <= top2; y++) {
               int id = b[Chunk.idx(x, y, z)] & BlockState.ID_MASK;
               if (id == 6 || Blocks.isFluid(id)) {
                  continue;
               }

               if (ver3) {
                  // V6.28/V6.29: a real cave mouth cuts a walk-in opening from
                  // the surface down into a chamber that joins the cave system.
                  boolean entranceAir = false;
                  if (ms != null) {
                     for (CaveCliffs.Mouth m : ms) {
                        if (m.air(wx, y, wz, h)) { entranceAir = true; break; }
                     }
                  }
                  // a sinkhole cuts a real vertical shaft down to y=30, whatever the noise says
                  if (shaft && y >= 30) {
                     b[Chunk.idx(x, y, z)] = 0;
                  } else if (entranceAir) {
                     b[Chunk.idx(x, y, z)] = 0;
                  } else {
                     // V6.29: keep the solid walk-in ramp under the opening; a
                     // noise cave must not hollow it out into a hidden pit.
                     boolean ramp = false;
                     if (ms != null) {
                        for (CaveCliffs.Mouth mm : ms) {
                           int fy = mm.floorAt(wx, wz);
                           if (fy >= 0 && y <= fy && y >= fy - 4) { ramp = true; break; }
                        }
                     }
                     if (!ramp) {
                        int cv = cc ? this.caves.hollowCC(wx, y, wz) : this.caves.hollow(wx, y, wz);
                        if (cv != 0) {
                           b[Chunk.idx(x, y, z)] = (short)(cv == CaveCliffs.LAVA ? Blocks.LAVA
                              : (cv == CaveCliffs.WATER ? Blocks.WATER : 0));
                        }
                     }
                  }
               } else if (bigCaves ? this.carveBig(wx, y, wz) : this.carve(wx, y, wz)) {
                  b[Chunk.idx(x, y, z)] = (short)(y <= 10 ? 58 : 0);
               }
            }

            // A stone lip makes the mouth read as a cave entrance rather than
            // a random hole, and stops grass/trees growing on its edge.
            if (ms != null && h >= 64) {
               for (CaveCliffs.Mouth m : ms) {
                  if (m.rim(wx, wz)) {
                     b[Chunk.idx(x, h, z)] = (short)Blocks.STONE;
                     break;
                  }
               }
            }

            // ravines
            if (h >= 67 && s.biome != Biomes.MUSHROOM) {
               double rv = this.ravine(wx, wz);
               if (rv > 0.0) {
                  int floor = 14 + (int)((this.nRavW.noise2(wx / 90.0 + 9.0, wz / 90.0) * 0.5 + 0.5) * 8.0);
                  for (int y = floor; y <= h + 1; y++) {
                     double f = (double)(y - floor) / Math.max(1, h - floor);
                     double need = 0.85 * (1.0 - Math.pow(Math.min(1.0, f), 0.55))
                        / (this.st.terrainVer >= 2 ? this.st.ravineScale : 1.0);
                     if (rv > need) {
                        int id = b[Chunk.idx(x, y, z)] & BlockState.ID_MASK;
                        if (id != 6 && !Blocks.isFluid(id)) {
                           b[Chunk.idx(x, y, z)] = (short)(y <= floor + 1 && y <= 12 ? 58 : 0);
                        }
                     }
                  }
               }
            }
         }
      }

      // stone of other kinds, ores
      Random vr = new Random(this.seed ^ (cx * 7919L + cz * 104729L) * 1000003L);
      this.vein(ch, vr, 3, 8, 20, 0, 128);
      this.vein(ch, vr, 8, 6, 20, 0, 128);
      this.vein(ch, vr, Blocks313.GRANITE, 6, 26, 0, 90);
      this.vein(ch, vr, Blocks313.DIORITE, 6, 26, 0, 90);
      this.vein(ch, vr, Blocks313.ANDESITE, 6, 26, 0, 90);
      this.vein(ch, vr, 19, 20, 14, 0, 128);
      this.vein(ch, vr, 20, 20, 8, 0, 64);
      this.vein(ch, vr, 21, 2, 8, 0, 32);
      this.vein(ch, vr, 23, 8, 7, 0, 16);
      this.vein(ch, vr, 22, 2, 7, 0, 16);
      this.vein(ch, vr, 24, 1, 6, 0, 30);
      if (bm[136] == Biomes.BADLANDS) {
         this.vein(ch, vr, 21, 6, 8, 32, 80);
      }

      this.decorate(ch, hs, bm, g);
      this.treesFor(ch);
      if (this.settings.structures) {
         this.structs.apply(ch);
         if (extras) {
            Stronghold.apply(this.seed, ch);
         }
      }

      if (extras) {
         Color312.decorate(this.seed, ch, hs, bm);
      }

      this.plantShoreAndPatches(ch, hs, bm);
      if (st.aquatic) Aquatic.decorate(this,ch,hs,bm);
      if (ver3) {
         this.caves.finish(ch); // strata, cave decorations and underground structures
      } else {
         ch.computeHeightmap();
      }
      return ch;
   }

   // ------------------------------------------------------------------------------------------------ plants
   private static final int[] PLAINS_FLOWERS = new int[]{Blocks.FLOWER_YELLOW, Blocks.FLOWER_RED, Blocks313.FLOWER0 + 2, Blocks313.FLOWER0 + 3, Blocks313.FLOWER0 + 4, Blocks313.FLOWER0 + 5,
      Blocks313.FLOWER0 + 6, Blocks313.FLOWER0 + 7, Blocks313.FLOWER0 + 8};
   private static final int[] FOREST_FLOWERS = new int[]{Blocks.FLOWER_RED, Blocks.FLOWER_YELLOW, Blocks313.FLOWER0 + 9, Blocks313.FLOWER0 + 1};
   private static final int[] FLOWER_FOREST = new int[]{Blocks313.FLOWER0 + 1, Blocks313.FLOWER0 + 2, Blocks313.FLOWER0 + 3, Blocks313.FLOWER0 + 4, Blocks313.FLOWER0 + 5, Blocks313.FLOWER0 + 6,
      Blocks313.FLOWER0 + 7, Blocks313.FLOWER0 + 8, Blocks313.FLOWER0 + 9, Blocks.FLOWER_YELLOW, Blocks.FLOWER_RED};
   private static final int[] MEADOW_FLOWERS = new int[]{Blocks.FLOWER_YELLOW, Blocks313.FLOWER0 + 2, Blocks313.FLOWER0 + 7, Blocks313.FLOWER0 + 8, Blocks313.FLOWER0 + 1, Blocks313.FLOWER0 + 9};
   private static final int[] SWAMP_FLOWERS = new int[]{Blocks313.FLOWER0};

   private int pickFlower(int[] list, int wx, int wz) {
      double n = this.nFl.noise2(wx / 14.0, wz / 14.0) * 0.5 + 0.5;
      int i = (int)(clamp((n - 0.15) / 0.7, 0.0, 0.999) * list.length);
      return list[i];
   }

   /** Tall grass, ferns, flowers, mushrooms, dead bushes, cacti, snow, lily pads. */
   private void decorate(Chunk ch, int[] hs, int[] bm, Cl[] g) {
      Random r = new Random(this.seed * -7046029254386353131L ^ (ch.cx * 73856093L + ch.cz * 19349663L) * 1000003L + 91L);
      for (int z = 0; z < 16; z++) {
         for (int x = 0; x < 16; x++) {
            int i = z * 16 + x;
            int h = hs[i];
            int biome = bm[i];
            int wx = ch.cx * 16 + x;
            int wz = ch.cz * 16 + z;
            float p = r.nextFloat();
            float q = r.nextFloat();
            if (h < 63) {
               // lily pads on the still water of swamps and (rarely) rivers, jungles' pools
               if (h <= 61 && ch.get(x, 62, z) == 50 && ch.get(x, 63, z) == 0 && (biome == Biomes.SWAMP && p < 0.16F || biome == Biomes.RIVER && p < 0.015F)) {
                  ch.set(x, 63, z, Blocks313.LILY_PAD);
               }

               continue;
            }

            if (h > 240) {
               continue;
            }

            int ground = ch.get(x, h, z);
            if (ch.get(x, h + 1, z) != 0) {
               continue;
            }

            if (Biomes.snowy(biome) && biome != Biomes.FROZEN_PEAKS && (ground == Blocks.GRASS || ground == Blocks.SAND || ground == Blocks.DIRT || ground == Blocks313.PODZOL)) {
               ch.set(x, h + 1, z, Blocks313.SNOW_LAYER);
               continue;
            }

            boolean grassy = ground == Blocks.GRASS || ground == Blocks313.PODZOL;
            // Update 6.22: flowers and grasses of both biomes mix in the border band.
            if (this.st.biomeBorders > 0 && g[(z + 1) * 18 + x + 1].blend > 0.6
               && g[(z + 1) * 18 + x + 1].altBiome >= 0
               && (CaveWorks.mix(this.seed ^ 0x464C4F57L, wx, 0, wz) & 255) < 96) {
               biome = g[(z + 1) * 18 + x + 1].altBiome;
            }

            switch (biome) {
               case Biomes.PLAINS:
                  if (grassy) {
                     if (p < 0.012F) {
                        ch.set(x, h + 1, z, this.pickFlower(PLAINS_FLOWERS, wx, wz));
                     } else if (p < 0.05F) {
                        this.tall(ch, x, h, z, 0);
                     } else if (p < 0.30F) {
                        ch.set(x, h + 1, z, Blocks.TALLGRASS);
                     }
                  }

                  break;
               case Biomes.FOREST:
               case Biomes.BIRCH_FOREST:
                  if (grassy) {
                     if (p < 0.006F) {
                        ch.set(x, h + 1, z, this.pickFlower(FOREST_FLOWERS, wx, wz));
                     } else if (p < 0.024F) {
                        this.tall(ch, x, h, z, 0);
                     } else if (p < 0.14F) {
                        ch.set(x, h + 1, z, Blocks.TALLGRASS);
                     } else if (q < 0.004F) {
                        ch.set(x, h + 1, z, Blocks313.MUSHROOM_BROWN);
                     }
                  }

                  break;
               case Biomes.FLOWER_FOREST:
                  if (grassy) {
                     if (p < 0.22F) {
                        ch.set(x, h + 1, z, this.pickFlower(FLOWER_FOREST, wx, wz));
                     } else if (p < 0.26F) {
                        this.tall(ch, x, h, z, 2 + (int)(q * 4.0F));
                     } else if (p < 0.38F) {
                        ch.set(x, h + 1, z, Blocks.TALLGRASS);
                     }
                  }

                  break;
               case Biomes.MEADOW:
                  if (grassy) {
                     if (p < 0.10F) {
                        ch.set(x, h + 1, z, this.pickFlower(MEADOW_FLOWERS, wx, wz));
                     } else if (p < 0.35F) {
                        ch.set(x, h + 1, z, Blocks.TALLGRASS);
                     } else if (p < 0.38F) {
                        this.tall(ch, x, h, z, 0);
                     }
                  }

                  break;
               case Biomes.DARK_FOREST:
                  if (grassy) {
                     if (p < 0.03F) {
                        ch.set(x, h + 1, z, q < 0.5F ? Blocks313.MUSHROOM_BROWN : Blocks313.MUSHROOM_RED);
                     } else if (p < 0.10F) {
                        ch.set(x, h + 1, z, Blocks.TALLGRASS);
                     } else if (p < 0.12F) {
                        ch.set(x, h + 1, z, Blocks313.FERN);
                     }
                  }

                  break;
               case Biomes.TAIGA:
               case Biomes.MEGA_TAIGA:
               case Biomes.SNOWY_TAIGA:
                  if (grassy) {
                     if (p < 0.12F) {
                        ch.set(x, h + 1, z, Blocks313.FERN);
                     } else if (p < 0.20F) {
                        ch.set(x, h + 1, z, Blocks.TALLGRASS);
                     } else if (p < 0.235F && biome == Biomes.MEGA_TAIGA) {
                        this.tall(ch, x, h, z, 1);
                     } else if (q < 0.006F) {
                        ch.set(x, h + 1, z, q < 0.003F ? Blocks313.MUSHROOM_RED : Blocks313.MUSHROOM_BROWN);
                     }
                  }

                  break;
               case Biomes.SAVANNA:
                  if (grassy) {
                     if (p < 0.07F) {
                        this.tall(ch, x, h, z, 0);
                     } else if (p < 0.50F) {
                        ch.set(x, h + 1, z, Blocks.TALLGRASS);
                     }
                  } else if (ground == Blocks313.COARSE_DIRT && p < 0.02F) {
                     ch.set(x, h + 1, z, Blocks313.DEAD_BUSH);
                  }

                  break;
               case Biomes.JUNGLE:
                  if (grassy) {
                     if (p < 0.12F) {
                        ch.set(x, h + 1, z, Blocks313.FERN);
                     } else if (p < 0.34F) {
                        ch.set(x, h + 1, z, Blocks.TALLGRASS);
                     } else if (p < 0.42F) {
                        this.tall(ch, x, h, z, q < 0.5F ? 0 : 1);
                     } else if (q < 0.0015F) {
                        ch.set(x, h + 1, z, Blocks.MELON);
                     }
                  }

                  break;
               case Biomes.SWAMP:
                  if (grassy) {
                     if (p < 0.004F) {
                        ch.set(x, h + 1, z, Blocks313.FLOWER0);
                     } else if (p < 0.24F) {
                        ch.set(x, h + 1, z, Blocks.TALLGRASS);
                     } else if (q < 0.008F) {
                        ch.set(x, h + 1, z, q < 0.004F ? Blocks313.MUSHROOM_RED : Blocks313.MUSHROOM_BROWN);
                     }
                  }

                  break;
               case Biomes.MUSHROOM:
                  if (ground == Blocks313.MYCELIUM && p < 0.03F) {
                     ch.set(x, h + 1, z, q < 0.5F ? Blocks313.MUSHROOM_RED : Blocks313.MUSHROOM_BROWN);
                  }

                  break;
               case Biomes.DESERT:
                  if (ground == Blocks.SAND) {
                     if (p < 0.012F) {
                        ch.set(x, h + 1, z, Blocks313.DEAD_BUSH);
                     } else if (p < 0.0145F && this.cactusRoom(ch, x, h, z)) {
                        int n = 1 + r.nextInt(3);
                        for (int k = 1; k <= n; k++) {
                           ch.set(x, h + k, z, Blocks.CACTUS);
                        }
                     }
                  }

                  break;
               case Biomes.BADLANDS:
                  if (ground == Blocks313.RED_SAND) {
                     if (p < 0.02F) {
                        ch.set(x, h + 1, z, Blocks313.DEAD_BUSH);
                     } else if (p < 0.026F && this.cactusRoom(ch, x, h, z)) {
                        int n = 1 + r.nextInt(3);
                        for (int k = 1; k <= n; k++) {
                           ch.set(x, h + 1 + k - 1, z, Blocks.CACTUS);
                        }
                     }
                  }

                  break;
               case Biomes.MOUNTAINS:
                  if (ground == Blocks.GRASS && p < 0.05F) {
                     ch.set(x, h + 1, z, Blocks.TALLGRASS);
                  }

                  break;
               case Biomes.BEACH:
                  break;
               default:
                  break;
            }
         }
      }
   }

   private boolean cactusRoom(Chunk ch, int x, int h, int z) {
      int[][] d = new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
      for (int[] o : d) {
         int nx = x + o[0];
         int nz = z + o[1];
         if (nx >= 0 && nx <= 15 && nz >= 0 && nz <= 15 && ch.get(nx, h + 1, nz) != 0) {
            return false;
         }
      }

      return true;
   }

   /** A two block tall plant: kind 0 tall grass, 1 large fern, 2 sunflower, 3 lilac, 4 rose bush, 5 peony. */
   private void tall(Chunk ch, int x, int h, int z, int kind) {
      if (ch.get(x, h + 2, z) == 0) {
         ch.set(x, h + 1, z, Blocks313.DOUBLE0 + kind * 2);
         ch.set(x, h + 2, z, Blocks313.DOUBLE0 + kind * 2 + 1);
      }
   }

   // ------------------------------------------------------------------------------------------------ trees
   private void treesFor(Chunk ch) {
      TerraTrees.Buf buf = BUF.get();
      int[] rule = new int[5];
      Cl s = new Cl();
      int built = 0;
      int cap = (int)Math.round(30.0 * this.treeDensity);
      if (cap < 8) cap = 8;
      for (int dx = -1; dx <= 1 && built < cap; dx++) {
         for (int dz = -1; dz <= 1 && built < cap; dz++) {
            int ncx = ch.cx + dx;
            int ncz = ch.cz + dz;
            Random tr = new Random(treeAttemptSeed(this.seed, ncx, ncz));
            for (int i = 0; i < TREE_ATTEMPTS && built < cap; i++) {
               int lx = tr.nextInt(16);
               int lz = tr.nextInt(16);
               float p = tr.nextFloat();
               int kind = tr.nextInt(1000);
               long ts = tr.nextLong();
               int wx = ncx * 16 + lx;
               int wz = ncz * 16 + lz;
               if (p >= 0.63F || wx < ch.cx * 16 - 10 || wx > ch.cx * 16 + 25 || wz < ch.cz * 16 - 10 || wz > ch.cz * 16 + 25) {
                  continue;
               }

               this.sample(wx, wz, s);
               if (s.h < 64 || s.h > 232) {
                  continue;
               }

               double dens = this.treeDensityAt(wx, wz);
               // Update 6.22: tree lines fade out across a biome border instead of stopping dead.
               if (s.blend > 0.0) dens *= 1.0 - 0.30 * s.blend;
               int treeBiome = s.biome;
               if (s.blend > 0.55 && s.altBiome >= 0
                  && (CaveWorks.mix(this.seed ^ 0x54524545L, wx, 0, wz) & 255) < (s.blend - 0.55) * 400.0) {
                  treeBiome = s.altBiome;
               }

               int plan = this.treePlan(treeBiome, p, kind, dens);
               if (plan < 0) {
                  continue;
               }

               // Update 6.23: every plan is one of several silhouettes at one of three size tiers.
               // Both come from the tree's seed-derived hash, so chunk seams still agree.
               int species = treeSpecies(plan);
               int variant = treeVariant(plan, ts);
               int tier = treeSizeTier(plan, ts, this.giantTrees);

               if (this.entrance(wx, wz) || this.ravine(wx, wz) > 0.0
                  || this.caves.mouthSurface(wx, wz, s.h)) {
                  continue;
               }

               int slope = Math.max(Math.max(Math.abs(s.h - this.heightAt(wx - 1, wz)), Math.abs(s.h - this.heightAt(wx + 1, wz))),
                  Math.max(Math.abs(s.h - this.heightAt(wx, wz - 1)), Math.abs(s.h - this.heightAt(wx, wz + 1))));
               this.surface(s, slope, wx, wz, rule);
               int top = rule[0];
               boolean ok = top == Blocks.GRASS || top == Blocks.DIRT || top == Blocks313.PODZOL || top == Blocks313.COARSE_DIRT || top == Blocks313.MYCELIUM;
               if (!ok || slope > 3) {
                  continue;
               }

               boolean wide = TerraTrees.isWide(species, variant)
                  || plan == P_MEGA_SPRUCE || plan == P_DARK_OAK || plan == P_MEGA_JUNGLE;
               if (wide) {
                  if (this.heightAt(wx + 1, wz) != s.h || this.heightAt(wx, wz + 1) != s.h || this.heightAt(wx + 1, wz + 1) != s.h) {
                     continue;
                  }
               }

               buf.begin(wx, s.h + 1, wz);
               Random rnd = new Random(ts);
               this.buildPlan(buf, rnd, plan, variant, tier);
               buf.commit(ch);
               built++;
               if (plan == P_MEGA_SPRUCE) {
                  // podzol under the giant spruce
                  for (int px = -2; px <= 3; px++) {
                     for (int pz = -2; pz <= 3; pz++) {
                        int x = wx + px - ch.cx * 16;
                        int zz = wz + pz - ch.cz * 16;
                        if (x >= 0 && x <= 15 && zz >= 0 && zz <= 15 && px * px + pz * pz < 10 && ch.get(x, s.h, zz) == Blocks.GRASS) {
                           ch.set(x, s.h, zz, Blocks313.PODZOL);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   static final ThreadLocal<TerraTrees.Buf> BUF = ThreadLocal.withInitial(TerraTrees.Buf::new);
   static final int P_OAK_SMALL = 0;
   static final int P_OAK_CLASSIC = 1;
   static final int P_BIRCH = 2;
   static final int P_BIRCH_TALL = 3;
   static final int P_SPRUCE = 4;
   static final int P_MEGA_SPRUCE = 5;
   static final int P_DARK_OAK = 6;
   static final int P_JUNGLE_SMALL = 7;
   static final int P_JUNGLE_TALL = 8;
   static final int P_MEGA_JUNGLE = 9;
   static final int P_BUSH = 10;
   static final int P_ACACIA = 11;
   static final int P_SWAMP_OAK = 12;
   static final int P_MUSH_RED = 13;
   static final int P_MUSH_BROWN = 14;
   static final int P_ICE_SPIKE = 15;
   static final int P_SPRUCE_PINE = 16;

   /** The exact dice a chunk uses for its trees. LOD tiles replay this sequence so
    *  distant trees sit where the real ones do. */
   static long treeAttemptSeed(long seed, int cx, int cz) {
      return seed * 7919L ^ (cx * 734287L + cz * 912931L) * 31L + 17L;
   }

   static final int TREE_ATTEMPTS = 44;

   /** Local tree density used by treePlan; shared with the LOD so biome forests match. */
   double treeDensityAt(int wx, int wz) {
      return (0.35 + 1.3 * clamp(this.nTr.noise2(wx / 70.0, wz / 70.0) * 0.9 + 0.5, 0.0, 1.0)) * this.treeDensity;
   }

   /** Which plant (a P_ constant) a biome grows at this attempt, or -1. p is the dice (0..1), kind a second die (0..999), dens the local density. */
   static int treePlan(int biome, float p, int kind, double dens) {
      switch (biome) {
         case Biomes.PLAINS:
            return p < 0.006 * dens ? (kind < 150 ? P_OAK_CLASSIC : P_OAK_SMALL) : -1;
         case Biomes.FOREST:
            return p < 0.17 * dens ? (kind < 200 ? P_BIRCH : (kind < 330 ? P_OAK_CLASSIC : (kind < 520 ? P_OAK_SMALL : P_OAK_CLASSIC))) : -1;
         case Biomes.BIRCH_FOREST:
            return p < 0.17 * dens ? (kind < 250 ? P_BIRCH_TALL : P_BIRCH) : -1;
         case Biomes.FLOWER_FOREST:
            return p < 0.07 * dens ? (kind < 500 ? P_BIRCH : P_OAK_CLASSIC) : -1;
         case Biomes.DARK_FOREST:
            return p < 0.24 * dens ? (kind < 700 ? P_DARK_OAK : (kind < 780 ? P_MUSH_RED : (kind < 850 ? P_MUSH_BROWN : P_OAK_SMALL))) : -1;
         case Biomes.TAIGA:
            return p < 0.105 * dens ? (kind < 220 ? P_SPRUCE_PINE : P_SPRUCE) : -1;
         case Biomes.SNOWY_TAIGA:
            return p < 0.085 * dens ? (kind < 220 ? P_SPRUCE_PINE : P_SPRUCE) : -1;
         case Biomes.MEGA_TAIGA:
            return p < 0.095 * dens ? (kind < 300 ? P_MEGA_SPRUCE : (kind < 620 ? P_SPRUCE_PINE : P_SPRUCE)) : -1;
         case Biomes.SWAMP:
            return p < 0.055 * dens ? P_SWAMP_OAK : -1;
         case Biomes.SAVANNA:
            return p < 0.028 * dens ? (kind < 800 ? P_ACACIA : P_OAK_SMALL) : -1;
         case Biomes.JUNGLE:
            return p < 0.31 * dens ? (kind < 500 ? P_JUNGLE_SMALL : (kind < 780 ? P_JUNGLE_TALL : (kind < 880 ? P_MEGA_JUNGLE : P_BUSH))) : -1;
         case Biomes.MEADOW:
            return p < 0.003 ? P_OAK_SMALL : -1;
         case Biomes.ICE_PLAINS:
            return p < 0.004 ? P_SPRUCE : -1;
         case Biomes.ICE_SPIKES:
            return p < 0.06 ? P_ICE_SPIKE : -1;
         case Biomes.MUSHROOM:
            return p < 0.03 * dens ? (kind < 500 ? P_MUSH_RED : P_MUSH_BROWN) : -1;
         case Biomes.MOUNTAINS:
            return p < 0.05 ? P_SPRUCE : -1;
         default:
            return -1;
      }
   }

   /** Update 6.23: which tree species a plant plan belongs to (-1 for mushrooms / ice spikes). */
   static int treeSpecies(int plan) {
      switch (plan) {
         case P_OAK_SMALL:
         case P_OAK_CLASSIC:
            return TerraTrees.SP_OAK;
         case P_BIRCH:
         case P_BIRCH_TALL:
            return TerraTrees.SP_BIRCH;
         case P_SPRUCE:
         case P_SPRUCE_PINE:
         case P_MEGA_SPRUCE:
            return TerraTrees.SP_SPRUCE;
         case P_JUNGLE_SMALL:
         case P_JUNGLE_TALL:
         case P_MEGA_JUNGLE:
         case P_BUSH:
            return plan == P_BUSH ? TerraTrees.SP_BUSH : TerraTrees.SP_JUNGLE;
         case P_ACACIA:
            return TerraTrees.SP_ACACIA;
         case P_DARK_OAK:
            return TerraTrees.SP_DARK_OAK;
         case P_SWAMP_OAK:
            return TerraTrees.SP_SWAMP;
         default:
            return -1;
      }
   }

   /** The silhouette the biome originally asked for; the hash rotates the rest of the catalogue in. */
   static int baseVariant(int plan) {
      switch (plan) {
         case P_OAK_SMALL: return 1;
         case P_BIRCH_TALL: return 1;
         case P_SPRUCE_PINE: return 1;
         case P_MEGA_SPRUCE: return 3;
         case P_JUNGLE_TALL: return 1;
         case P_MEGA_JUNGLE: return 2;
         case P_BUSH: return 0;
         default: return 0;
      }
   }

   /** All silhouettes of a species really appear, but the biome's preferred one is the most likely. */
   static int treeVariant(int plan, long hash) {
      int sp = treeSpecies(plan);
      if (sp < 0) return 0;
      int n = TerraTrees.variants(sp);
      int jitter = (int)Math.floorMod(hash >>> 17, n);
      return (baseVariant(plan) + jitter) % n;
   }

   /** 58% normal, 30% large, 12% giant. Giant becomes large when the switch is off. */
   static int treeSizeTier(int plan, long hash, boolean giants) {
      long roll = Math.floorMod(hash >>> 41, 100L);
      int tier = roll < 58 ? 0 : (roll < 88 ? 1 : 2);
      if (tier == 2 && !giants) tier = 1;
      if (plan == P_MEGA_SPRUCE || plan == P_MEGA_JUNGLE || plan == P_DARK_OAK) {
         tier = Math.max(1, tier);
      }
      return tier;
   }

   void buildPlan(TerraTrees.Buf t, Random r, int plan) {
      buildPlan(t, r, plan, baseVariant(plan), 0, 1.0F);
   }

   /** Update 6.23: build a plant with a chosen silhouette and size tier. */
   void buildPlan(TerraTrees.Buf t, Random r, int plan, int variant, int tier) {
      buildPlan(t, r, plan, variant, tier, this.treeScale);
   }

   void buildPlan(TerraTrees.Buf t, Random r, int plan, int variant, int tier, float scale) {
      int sp = treeSpecies(plan);
      if (sp >= 0) {
         TerraTrees.build(t, r, sp, variant, tier, scale);
         return;
      }

      switch (plan) {
         case P_MUSH_RED:
            TerraTrees.giantMushroom(t, r, 0, tier, scale);
            break;
         case P_MUSH_BROWN:
            TerraTrees.giantMushroom(t, r, 1, tier, scale);
            break;
         case P_ICE_SPIKE:
            TerraTrees.iceSpike(t, r, tier, scale);
            break;
         default:
            break;
      }
   }

   // ------------------------------------------------------------------------------------------------ the world's spawn
   @Override
   public int[] findSpawn() {
      // rings round the middle, 8 blocks apart; Large Biomes can have a sea that wide, so in 1 of 20 worlds the search goes on (to 4000 blocks)
      for (int ring = 0; ring < 4000; ring += ring < 900 ? 8 : 16) {
         int n = ring == 0 ? 1 : (ring < 900 ? 16 : 48);
         for (int k = 0; k < n; k++) {
            double a = k * Math.PI * 2.0 / n;
            int x = (int)(Math.cos(a) * ring);
            int z = (int)(Math.sin(a) * ring);
            Gen.Col c = this.column(x, z);
            if (c.h >= 65 && c.h <= 100 && Biomes.spawnable(c.biome) && !this.entrance(x, z)
               && this.ravine(x, z) <= 0.0 && !this.caves.mouthSurface(x, z, c.h)) {
               return new int[]{x, c.h + 1, z};
            }
         }
      }

      return new int[]{0, Math.max(65, this.column(0, 0).h) + 1, 0};
   }
}
