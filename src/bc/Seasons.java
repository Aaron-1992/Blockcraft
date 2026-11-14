package bc;

/**
 * V6.19: a real four-season cycle, driven only by {@link World#dayTime}.
 *
 * A year is {@code 4 * seasonDays} Minecraft days (8 days per season by default,
 * so a full year is 32 days). Nothing about a season is stored in a save: the
 * world's saved day counter is enough, so old worlds simply join the cycle at
 * the right point and multiplayer clients stay in sync.
 *
 * The last 18% of each season blends into the next one, so temperature, grass,
 * leaves and the sky change smoothly instead of snapping at midnight.
 *
 * The published {@link #current}, {@link #phase} and {@link #blend} fields are
 * volatile: the mesher, LOD and map workers read them without a lock.
 */
final class Seasons {
   static final int SPRING = 0;
   static final int SUMMER = 1;
   static final int AUTUMN = 2;
   static final int WINTER = 3;
   static final int COUNT = 4;

   static final String[] NAME = {"Spring", "Summer", "Autumn", "Winter"};
   static final long DAY = 24000L;
   static final int DEFAULT_DAYS = 8;

   /** Set once per world tick on the main thread. */
   public static volatile boolean enabled = true;
   public static volatile int current = SPRING;
   /** 0..1 through the current season. */
   public static volatile float phase = 0.0F;
   /** 0..1: how much of the NEXT season is already mixed in. */
   public static volatile float blend = 0.0F;

   private Seasons() {
   }

   static int clampDays(int d) {
      return Math.max(1, Math.min(64, d));
   }

   static long seasonTicks(int days) {
      return DAY * clampDays(days);
   }

   static long yearTicks(int days) {
      return seasonTicks(days) * COUNT;
   }

   static int index(long t, int days) {
      long y = yearTicks(days);
      long p = Math.floorMod(t, y);
      return (int)(p / seasonTicks(days));
   }

   static float phase(long t, int days) {
      long s = seasonTicks(days);
      return (float)(Math.floorMod(t, s) / (double)s);
   }

   /** Smooth cross-fade over the last 18% of a season. */
   static float blend(long t, int days) {
      float p = phase(t, days);
      return p < 0.82F ? 0.0F : (p - 0.82F) / 0.18F;
   }

   static int next(int s) {
      return (s + 1) % COUNT;
   }

   static void publish(long t, int days, boolean on) {
      enabled = on;
      if (!on) {
         current = SPRING;
         phase = 0.0F;
         blend = 0.0F;
         return;
      }
      current = index(t, days);
      phase = phase(t, days);
      blend = blend(t, days);
   }

   static int dayOfSeason(long t, int days) {
      return (int)(Math.floorMod(t, seasonTicks(days)) / DAY) + 1;
   }

   static int dayOfYear(long t, int days) {
      return (int)(Math.floorMod(t, yearTicks(days)) / DAY) + 1;
   }

   static String label(long t, int days) {
      int s = index(t, days);
      return NAME[s] + " - day " + dayOfSeason(t, days) + " of " + clampDays(days);
   }

   /** Degrees Celsius this season adds to a biome's base air temperature. */
   static float tempBias(int s) {
      switch (s) {
         case SPRING:
            return 2.0F;
         case SUMMER:
            return 11.0F;
         case AUTUMN:
            return 1.0F;
         default:
            return -15.0F;
      }
   }

   static float bias(long t, int days, boolean on) {
      if (!on) {
         return 0.0F;
      }
      int s = index(t, days);
      float b = blend(t, days);
      return tempBias(s) * (1.0F - b) + tempBias(next(s)) * b;
   }

   /** How likely rain is to fall as snow: winter is snow, summer is rain. */
   static float snowChance(int s) {
      switch (s) {
         case WINTER:
            return 0.96F;
         case AUTUMN:
            return 0.18F;
         case SUMMER:
            return 0.0F;
         default:
            return 0.05F;
      }
   }

   /** Crop / sapling growth multiplier. */
   static float growth(int s) {
      switch (s) {
         case SUMMER:
            return 1.30F;
         case SPRING:
            return 1.15F;
         case AUTUMN:
            return 0.85F;
         default:
            return 0.40F;
      }
   }

   static float growth(long t, int days, boolean on) {
      if (!on) return 1.0F;
      int s = index(t, days);
      float b = blend(t, days);
      return growth(s) * (1.0F - b) + growth(next(s)) * b;
   }

   /** Raininess: autumn is wet, summer is drier. */
   static float humidity(int s) {
      switch (s) {
         case AUTUMN:
            return 1.10F;
         case SPRING:
            return 1.00F;
         case SUMMER:
            return 0.70F;
         default:
            return 0.85F;
      }
   }

   /**
    * 0 = no clouds blown in, 1 = full. Used by the weather field so a season
    * actually changes how often it rains instead of only changing the colours.
    */
   static float cloudiness(int s) {
      switch (s) {
         case AUTUMN:
            return 1.0F;
         case WINTER:
            return 0.92F;
         case SPRING:
            return 0.85F;
         default:
            return 0.65F;
      }
   }

   /** RGB grade of the sky and fog: autumn is warm, winter is pale and cold. */
   static float[] grade(int s) {
      switch (s) {
         case SUMMER:
            return new float[]{1.05F, 1.02F, 0.94F};
         case AUTUMN:
            return new float[]{1.10F, 0.96F, 0.82F};
         case WINTER:
            return new float[]{0.92F, 0.98F, 1.12F};
         default:
            return new float[]{1.00F, 1.05F, 0.98F};
      }
   }

   static float[] grade(long t, int days, boolean on, float[] out) {
      if (!on) { out[0] = out[1] = out[2] = 1.0F; return out; }
      int s = index(t, days);
      float b = blend(t, days);
      for (int i = 0; i < 3; i++) out[i] = gradeValue(s, i) * (1 - b) + gradeValue(next(s), i) * b;
      return out;
   }

   private static float gradeValue(int season, int component) {
      switch (season) {
         case SUMMER: return component == 0 ? 1.05F : component == 1 ? 1.02F : 0.94F;
         case AUTUMN: return component == 0 ? 1.10F : component == 1 ? 0.96F : 0.82F;
         case WINTER: return component == 0 ? 0.92F : component == 1 ? 0.98F : 1.12F;
         default: return component == 1 ? 1.05F : component == 2 ? 0.98F : 1.00F;
      }
   }

   static float[] grade(long t, int days, boolean on) {
      if (!on) {
         return new float[]{1.0F, 1.0F, 1.0F};
      }
      int s = index(t, days);
      float b = blend(t, days);
      float[] a = grade(s);
      float[] c = grade(next(s));
      return new float[]{a[0] * (1 - b) + c[0] * b, a[1] * (1 - b) + c[1] * b, a[2] * (1 - b) + c[2] * b};
   }

   static int mix(int a, int b, float t) {
      t = Math.max(0.0F, Math.min(1.0F, t));
      int r = (int)((a >> 16 & 255) * (1 - t) + (b >> 16 & 255) * t);
      int g = (int)((a >> 8 & 255) * (1 - t) + (b >> 8 & 255) * t);
      int bl = (int)((a & 255) * (1 - t) + (b & 255) * t);
      return r << 16 | g << 8 | bl;
   }

   private static int scale(int rgb, float r, float g, float b) {
      int rr = Math.max(0, Math.min(255, (int)((rgb >> 16 & 255) * r)));
      int gg = Math.max(0, Math.min(255, (int)((rgb >> 8 & 255) * g)));
      int bb = Math.max(0, Math.min(255, (int)((rgb & 255) * b)));
      return rr << 16 | gg << 8 | bb;
   }

   /** The colour of grass / leaves in one season, before the cross-fade. */
   private static int tintRaw(int rgb, int s, boolean foliage) {
      switch (s) {
         case SPRING:
            return scale(rgb, 0.98F, 1.09F, 0.96F);
         case SUMMER:
            return scale(rgb, 0.90F, 1.02F, 0.86F);
         case AUTUMN:
            return mix(rgb, foliage ? 0xC86A24 : 0xA3914B, foliage ? 0.62F : 0.40F);
         default:
            // Winter: grass frosts over, evergreen foliage only desaturates a little.
            return mix(rgb, 0xCBD6DE, foliage ? 0.28F : 0.58F);
      }
   }

   /** Pure seasonal tint helper, also used by headless palette regression tests. */
   static int tintForSeason(int rgb, int season, boolean foliage) {
      return tintRaw(rgb, season, foliage);
   }

   /** Render-time seasonal colour for a biome / LOD / preview colour. Never changes saved chunk data. */
   static int tint(int rgb, boolean foliage) {
      if (!enabled) {
         return rgb;
      }
      int a = tintRaw(rgb, current, foliage);
      if (blend > 0.001F) {
         a = mix(a, tintRaw(rgb, next(current), foliage), blend);
      }
      return a;
   }

   /**
    * Precipitation with the season applied. Deserts stay dry, cold biomes stay
    * snowy, but winter turns ordinary rain into snow down to y=32 and a hot
    * summer melts mountain snow back into rain.
    */
   static int precipitation(int base, int biome, int h, long t, int days, boolean on) {
      if (base < 0 || !on) {
         return base;
      }
      int s = index(t, days);
      float b = blend(t, days);
      float snow = snowChance(s) * (1 - b) + snowChance(next(s)) * b;
      if (base == 1) {
         return snow > 0.45F && h >= 32 ? 2 : 1;
      }
      // base == 2: only a genuinely frozen biome keeps its snow all summer.
      if (snow < 0.10F && !Biomes.snowy(biome) && h < 140) {
         return 1;
      }
      return 2;
   }
}
