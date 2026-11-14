package bc;

/**
 * V6.19: a real wind field, not a single "windy" flag.
 *
 * The base wind is a slowly turning vector with multi-scale gusts. On top of
 * that every place in the world has its own local turbulence (smooth 3D value
 * noise, seeded by the world seed) and the wind gets stronger with altitude.
 * Everything is a pure function of the world seed and {@code tick}, so a
 * multiplayer client, a reload and a headless test all see the same wind.
 *
 * Consumers: rain / snow slant and cloud drift ({@link WeatherFx}), particle
 * drift ({@link Game}), leaf and grass sway ({@link ShaderSrc#WORLD_VS}),
 * wind chill ({@link BodyTemp}), storm-cell placement ({@link Weather}) and the
 * HUD. Speeds are in blocks per second.
 */
final class Wind {
   final long seed;
   /** Current base vector in blocks/second. -Z is north, +X is east. */
   double x;
   double z;
   /** Magnitude of the base vector. */
   double speed;
   /** 0..1 gust strength. */
   double gust;
   /** Compass heading in degrees: 0 = north, 90 = east. */
   double dirDeg;
   /** Radians, 0 = north (-Z). */
   double theta;
   /** World setting: 0 = dead calm, 1 = normal, 2 = stormy. */
   float scale = 1.0F;
   long tick;

   private final double[] ph = new double[6];

   Wind(long seed, float scale) {
      this.seed = seed;
      this.scale = scale;
      // Stable phases: a given seed always has the same weather patterns.
      long h = seed ^ 0x5DEECE66DL;
      for (int i = 0; i < ph.length; i++) {
         h = h * 6364136223846793005L + 1442695040888963407L;
         ph[i] = ((h >>> 17) & 0xFFFFFF) / (double)0xFFFFFF * Math.PI * 2.0;
      }
      update(0L);
   }

   void update(long tick) {
      this.tick = tick;
      double t = tick / 20.0;              // seconds of world time
      theta = ph[0] * 0.16 + 1.90 * Math.sin(t / 211.0 + ph[1])
         + 0.70 * Math.sin(t / 53.0 + ph[2]) + 0.22 * Math.sin(t / 13.0 + ph[3]);
      double base = 2.4 + 2.2 * (0.5 + 0.5 * Math.sin(t / 97.0 + ph[4]));
      double g = 0.5 + 0.5 * Math.sin(t / 6.7 + ph[5]) * Math.sin(t / 3.3 + ph[1]);
      gust = Math.max(0.0, Math.min(1.0, g));
      speed = (base + 6.0 * gust * gust) * Math.max(0.0F, scale);
      x = Math.sin(theta) * speed;
      z = -Math.cos(theta) * speed;
      dirDeg = (Math.toDegrees(theta) + 360.0) % 360.0;
   }

   // ------------------------------------------------------------------ local field
   private double hash(int ix, int iy, int iz) {
      long h = seed ^ (long)ix * 0x9E3779B1L ^ (long)iy * 0x85EBCA77L ^ (long)iz * 0xC2B2AE3DL;
      h *= 0x27D4EB2DL;
      h ^= h >>> 15;
      return (h >>> 8 & 0xFFFFFF) / (double)0x7FFFFF - 1.0;
   }

   private static double fade(double t) {
      return t * t * (3.0 - 2.0 * t);
   }

   /** Smooth 3D value noise in [-1, 1]. */
   private double noise(double x, double y, double z) {
      int ix = (int)Math.floor(x);
      int iy = (int)Math.floor(y);
      int iz = (int)Math.floor(z);
      double fx = fade(x - ix);
      double fy = fade(y - iy);
      double fz = fade(z - iz);
      double acc = 0.0;
      for (int dz = 0; dz < 2; dz++) {
         for (int dy = 0; dy < 2; dy++) {
            for (int dx = 0; dx < 2; dx++) {
               double w = (dx == 0 ? 1 - fx : fx) * (dy == 0 ? 1 - fy : fy) * (dz == 0 ? 1 - fz : fz);
               acc += w * hash(ix + dx, iy + dy, iz + dz);
            }
         }
      }
      return acc;
   }

   /**
    * The wind at one place: {vx, vz} in blocks/second. Local turbulence turns
    * the base vector by up to ~35 degrees and changes its speed, and the wind
    * is stronger the higher above sea level you are.
    */
   double[] at(double px, double py, double pz) {
      return at(px, py, pz, new double[2]);
   }

   /** Reusable-buffer variant for per-frame render and temperature paths. */
   double[] at(double px, double py, double pz, double[] out) {
      double t = tick / 20.0;
      double turn = noise(px / 340.0, pz / 340.0, t / 70.0) * 0.60;
      double vary = noise(px / 180.0 + 11.0, pz / 180.0 - 7.0, t / 45.0 + 3.0);
      double alt = 1.0 + Math.max(0.0, py - 64.0) / 180.0;
      double s = speed * (0.80 + 0.45 * vary) * alt;
      double a = theta + turn;
      out[0] = Math.sin(a) * s;
      out[1] = -Math.cos(a) * s;
      return out;
   }

   double speedAt(double px, double py, double pz) {
      double[] v = at(px, py, pz, new double[2]);
      return Math.sqrt(v[0] * v[0] + v[1] * v[1]);
   }

   /** Beaufort-ish 0..12 from the base speed (used by the HUD and wind chill). */
   int beaufort() {
      return (int)Math.round(Math.min(12.0, Math.pow(speed / 1.6, 0.75) * 3.0));
   }

   static final String[] COMPASS = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};

   String compass() {
      return COMPASS[(int)((dirDeg + 22.5) / 45.0) % 8];
   }

   String label() {
      String g = gust > 0.72 ? " gusting" : (gust < 0.22 ? " steady" : "");
      return compass() + " " + String.format(java.util.Locale.ROOT, "%.1f", speed) + " m/s" + g;
   }
}
