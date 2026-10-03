package bc;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;

/**
 * Update 3.8 - everything the weather looks like, for BOTH render paths (with and without shaders):
 * <ul>
 *   <li>blocky 3D clouds (the no-shader path; with shaders the raymarched clouds in {@link Shaders} are used) built on a
 *       worker thread from the same weather field the game uses: flat-bottomed cumulus, grey overcast, towering dark
 *       storm clouds with a lowered base;</li>
 *   <li>rain / snow curtains that hang below the clouds and are visible kilometres away (a vertex buffer built together
 *       with the clouds);</li>
 *   <li>rain and snow streaks around the player, only where nothing is overhead;</li>
 *   <li>lightning bolts and the flash they throw on the sky;</li>
 *   <li>the numbers the sky/fog/sun code needs: how overcast, wet and stormy it is where the camera is.</li>
 * </ul>
 * The CPU cloud shapes use the same noise as the cloud shader, so both paths show storms in the same places.
 */
final class WeatherFx {
   // ---------------------------------------------------------------- cloud mesh parameters
   /** two levels of cloud blocks: fine ones near the camera, coarse ones out to 3.6 km so storms are visible from far away */
   static final int[] CELLS = {16, 48};
   static final int[] LAYERS = {6, 16};
   static final int[] RADS = {70, 75};
   /** the fine level covers this many blocks around the camera (square), the coarse level starts there */
   static final float NEAR_R = 1120.0F;
   static final float FAR_FOG0 = 500.0F;
   static final float FAR_FOG1 = 4300.0F;
   /** the cloud shader's noise (R = coarse 1800-block structure, G = fine 300-block structure), same maps on the CPU */
   static final byte[] NOISE_R = Weather.bytes(Shaders.fbm(256, new int[]{4, 8, 16, 32}, new float[]{0.5F, 0.28F, 0.15F, 0.07F}, 1337L));
   static final byte[] NOISE_G = Weather.bytes(Shaders.fbm(256, new int[]{16, 32, 64}, new float[]{0.5F, 0.3F, 0.2F}, 4242L));

   final Renderer r;
   // ---------------------------------------------------------------- local weather at the camera (updated each frame)
   /** cloud cover / rain / thunderstorm intensity above the camera, 0..1 */
   float cover;
   float rain;
   float storm;
   /** 1 = it snows here instead of rain, 0 = rain (smoothed) */
   float snowy;
   /** lightning flash 0..1 */
   float flash;
   /** how much direct sunlight reaches the camera (clouds in the direction of the sun), 0..1 */
   float sunVis = 1.0F;
   /** 0..1: how grey/dark the sky is overall */
   float overcast;
   /** the slab the cloud shader marches: thin in fair weather (like before 3.8), tall only while a thunderhead is within reach */
   float slabLo = 140.0F;
   float slabHi = 252.0F;
   /** the strongest bolt for the cloud glow: world position of its top and the strength 0..1 */
   float flashX;
   float flashY;
   float flashZ;
   float flashAmt;
   Weather.Snap snap;
   final float[] tmp = new float[4];

   // ---------------------------------------------------------------- GL objects
   int texWhite;
   int texRain;
   int texSnow;
   int texSnowVeil;
   int texShaft;
   final int[] vboCloud = new int[2];
   int vboShaft;
   final int[] nCloud = new int[2];
   int nShaft;
   final int[] ui0 = new int[2];
   final int[] uj0 = new int[2];
   double builtTs = -1.0E9;
   double builtCamX;
   double builtCamZ;
   double builtOffX;
   double builtOffZ;
   boolean built;
   boolean wantRebuild = true;
   long lastBuildMs;
   final ExecutorService worker = Executors.newSingleThreadExecutor(run -> {
      Thread t = new Thread(run, "cloud-mesh");
      t.setDaemon(true);
      t.setPriority(3);
      return t;
   });
   final AtomicReference<Result> ready = new AtomicReference<>();
   volatile boolean building;

   WeatherFx(Renderer r) {
      this.r = r;
   }

   // ================================================================ local values
   void updateLocal(float partial) {
      Game g = this.r.g;
      if (g.world == null || g.player == null) {
         this.cover = this.rain = this.storm = this.flash = 0.0F;
         this.overcast = 0.0F;
         this.sunVis = 1.0F;
         this.snap = null;
         return;
      }

      Weather wx = g.world.weather;
      wx.suppressed = !g.opt.weather;
      this.snap = wx.snapAt(partial);
      double x = g.player.x;
      double z = g.player.z;
      this.snap.at(x, z, this.tmp);
      this.cover = this.tmp[0];
      this.rain = this.tmp[1];
      this.storm = this.tmp[2];
      int kind = wx.precipKind((int)Math.floor(x), (int)Math.floor(z));
      float target = kind == 2 ? 1.0F : 0.0F;
      this.snowy += (target - this.snowy) * 0.05F;
      this.slabLo = 164.0F - 27.0F * wx.maxStorm;
      this.slabHi = 250.0F + 134.0F * wx.maxCore;
      this.flash = wx.suppressed ? 0.0F : wx.flashAt(x, z, partial);
      this.flashAmt = 0.0F;
      if (!wx.suppressed) {
         for (Weather.Bolt b : wx.bolts) {
            double dx = b.x - x;
            double dz = b.z - z;
            float amt = b.env(partial) * (float)(1.0 / (1.0 + (dx * dx + dz * dz) / (1100.0 * 1100.0)));
            if (amt > this.flashAmt) {
               this.flashAmt = amt;
               this.flashX = (float)b.x;
               this.flashY = (float)(b.top - 8.0);
               this.flashZ = (float)b.z;
            }
         }
      }
      // light cloud does not grey the sky: only real overcast does
      this.overcast = Math.min(1.0F, Weather.smooth(0.40F, 1.0F, this.cover) * 0.95F + this.storm * 0.1F);
      // sun: look at the cloud cover where the line from the camera to the sun crosses the cloud layer
      double sa = this.r.sunAngle;
      double sx = -Math.sin(sa);
      double sy = Math.cos(sa);
      if (sy > 0.04) {
         double d = Math.min(3000.0, Math.max(0.0, (Weather.BASE_FAIR - this.r.camY)) / sy);
         float c = this.snap.cover(x + sx * d, z);
         this.sunVis = 1.0F - 0.92F * Weather.smooth(0.35F, 1.0F, c);
      } else {
         this.sunVis = 1.0F;
      }
   }

   /** how gloomy the sky looks in this direction (0..1): the cover / storm where that line of sight meets the clouds */
   float gloom(double dx, double dy, double dz) {
      if (this.snap == null) {
         return 0.0F;
      }

      double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
      dx /= len;
      dy /= len;
      dz /= len;
      double up = Math.max(dy, 0.035);
      double h = Math.max(40.0, Weather.BASE_FAIR - this.r.camY);
      double sc = h / up;
      double hd = Math.sqrt(dx * dx + dz * dz) * sc;
      if (hd > 2600.0) {
         sc *= 2600.0 / hd;
      }

      // near the horizon the line of sight crosses the whole cloud layer over a long stretch: look at three places along it
      float g = 0.0F;
      for (int i = 0; i < 3; i++) {
         double f = i == 0 ? 1.0 : (i == 1 ? 0.55 : 0.3);
         if (i > 0 && sc < 500.0) {
            break;
         }

         this.snap.at(this.r.camX + dx * sc * f, this.r.camZ + dz * sc * f, this.tmp);
         g = Math.max(g, Weather.smooth(0.45F, 1.0F, this.tmp[0]) * 0.45F + this.tmp[2] * 0.55F);
      }

      // strongest overhead, weaker at the horizon (the haze there is the fog colour)
      float elev = (float)Math.max(0.0, Math.min(1.0, dy * 4.0));
      return Math.min(1.0F, g * (0.45F + 0.55F * elev));
   }

   // ================================================================ textures
   void ensureTextures() {
      if (this.texWhite != 0) {
         return;
      }

      this.texWhite = Tex.upload(1, 1, new int[]{-1}, false, true);
      this.texRain = mip(Tex.upload(64, 128, streaks(64, 128, 12, 12, 40, 0.50F, 6001L), true, true));
      this.texShaft = mip(Tex.upload(64, 256, veil(64, 256, 30, 24, 90, 6002L), true, true));
      this.texSnow = mip(Tex.upload(32, 32, flakes(32, 5, 6003L), true, true));
      this.texSnowVeil = mip(Tex.upload(64, 256, snowVeil(64, 256, 6004L), true, true));
   }

   static int mip(int tex) {
      GL11.glBindTexture(3553, tex);
      GL11.glTexParameteri(3553, 10241, 9987);
      GL11.glTexParameteri(3553, 10240, 9729);
      GL30.glGenerateMipmap(3553);
      return tex;
   }

   /** Vertical rain streaks: thin, soft, fading in at the top and with a brighter head; tileable vertically. */
   static int[] streaks(int w, int h, int count, int minLen, int maxLen, float strength, long seed) {
      Random rnd = new Random(seed);
      float[] a = new float[w * h];
      for (int i = 0; i < count; i++) {
         int x = rnd.nextInt(w);
         int len = minLen + rnd.nextInt(maxLen - minLen + 1);
         int y0 = rnd.nextInt(h);
         float amp = (0.5F + 0.5F * rnd.nextFloat()) * strength;
         int wid = rnd.nextInt(3) == 0 ? 2 : 1;
         for (int k = 0; k < len; k++) {
            float t = (float)k / len;
            float prof = t < 0.15F ? t / 0.15F : 1.0F - (t - 0.15F) / 0.85F * 0.55F;
            int y = (y0 + k) % h;
            for (int d = 0; d < wid; d++) {
               int xx = (x + d) % w;
               float v = amp * prof * (d == 0 ? 1.0F : 0.55F);
               a[y * w + xx] = Math.max(a[y * w + xx], v);
               int xl = (x - 1 + w) % w;
               a[y * w + xl] = Math.max(a[y * w + xl], v * 0.18F);
            }
         }
      }

      int[] px = new int[w * h];
      for (int i = 0; i < px.length; i++) {
         int al = Math.min(255, (int)(a[i] * 255.0F));
         px[i] = al << 24 | 0xDCE6FF;
      }

      return px;
   }

   /** A rain curtain: a soft translucent veil (so it still shows at a distance, where thin streaks average out) with streaks in it. */
   static int[] veil(int w, int h, int count, int minLen, int maxLen, long seed) {
      int[] st = streaks(w, h, count, minLen, maxLen, 0.55F, seed);
      Random rnd = new Random(seed + 77);
      float[] low = new float[w * 8];
      for (int i = 0; i < low.length; i++) {
         low[i] = rnd.nextFloat();
      }

      int[] px = new int[w * h];
      for (int y = 0; y < h; y++) {
         for (int x = 0; x < w; x++) {
            // slow variation in density: vertical bands that are denser and thinner (tileable)
            float a0 = low[(y * 8 / h) * w + x];
            float a1 = low[((y * 8 / h + 1) % 8) * w + x];
            float f = y * 8.0F / h - (float)Math.floor(y * 8.0F / h);
            float band = a0 + (a1 - a0) * f;
            float base = 0.15F + 0.07F * band;
            float streak = (st[y * w + x] >>> 24) / 255.0F;
            int al = Math.min(255, (int)((base + streak * 0.62F) * 255.0F));
            px[y * w + x] = al << 24 | 0xDCE6FF;
         }
      }

      return px;
   }

   /** Snow seen from far away: a soft pale veil with slow vertical dapple and a few short soft blobs instead of rain streaks. */
   static int[] snowVeil(int w, int h, long seed) {
      Random rnd = new Random(seed);
      float[] a = new float[w * h];
      for (int i = 0; i < 70; i++) {
         int x = rnd.nextInt(w);
         int y = rnd.nextInt(h);
         int len = 6 + rnd.nextInt(10);
         float amp = 0.25F + 0.35F * rnd.nextFloat();
         for (int k = 0; k < len; k++) {
            float t = (float)k / len;
            float prof = (float)Math.sin(Math.PI * t);
            for (int d = -1; d <= 1; d++) {
               float v = amp * prof * (d == 0 ? 1.0F : 0.4F);
               int idx = ((y + k) % h) * w + (x + d + w) % w;
               a[idx] = Math.max(a[idx], v);
            }
         }
      }

      float[] low = new float[w * 8];
      for (int i = 0; i < low.length; i++) {
         low[i] = rnd.nextFloat();
      }

      int[] px = new int[w * h];
      for (int y = 0; y < h; y++) {
         for (int x = 0; x < w; x++) {
            float a0 = low[(y * 8 / h) * w + x];
            float a1 = low[((y * 8 / h + 1) % 8) * w + x];
            float f = y * 8.0F / h - (float)Math.floor(y * 8.0F / h);
            float band = a0 + (a1 - a0) * f;
            int al = Math.min(255, (int)((0.12F + 0.10F * band + a[y * w + x]) * 255.0F));
            px[y * w + x] = al << 24 | 0xF4F8FF;
         }
      }

      return px;
   }

   static int[] flakes(int s, int count, long seed) {
      Random rnd = new Random(seed);
      float[] a = new float[s * s];
      for (int i = 0; i < count; i++) {
         float cx = rnd.nextInt(s) + 0.5F;
         float cy = rnd.nextInt(s) + 0.5F;
         float rad = 1.7F + rnd.nextFloat() * 1.5F;
         for (int y = -3; y <= 3; y++) {
            for (int x = -3; x <= 3; x++) {
               float d = (float)Math.sqrt(x * x + y * y) / rad;
               float v = Math.max(0.0F, 1.0F - d);
               int xx = ((int)cx + x + s) % s;
               int yy = ((int)cy + y + s) % s;
               a[yy * s + xx] = Math.max(a[yy * s + xx], v * v);
            }
         }
      }

      int[] px = new int[s * s];
      for (int i = 0; i < px.length; i++) {
         px[i] = Math.min(255, (int)(a[i] * 255.0F)) << 24 | 0xFFFFFF;
      }

      return px;
   }

   // ================================================================ cloud + shaft mesh (worker thread)
   /** Vertex buffers are recycled: the cloud mesh is rebuilt every few seconds and direct buffers are only freed lazily by the GC. */
   static final java.util.ArrayDeque<VB> POOL = new java.util.ArrayDeque<>();

   static VB acquire() {
      synchronized (POOL) {
         VB v = POOL.pollFirst();
         if (v != null) {
            v.b.clear();
            v.n = 0;
            return v;
         }
      }

      return new VB();
   }

   static void recycle(Result r) {
      synchronized (POOL) {
         for (VB v : new VB[]{r.cloud[0], r.cloud[1], r.shaft}) {
            if (v != null && POOL.size() < 8) {
               POOL.addFirst(v);
            }
         }
      }
   }

   static final class VB {
      ByteBuffer b = ByteBuffer.allocateDirect(1 << 20).order(ByteOrder.nativeOrder());
      int n;

      void ensure() {
         if (this.b.remaining() < 16) {
            ByteBuffer nb = ByteBuffer.allocateDirect(this.b.capacity() * 2).order(ByteOrder.nativeOrder());
            this.b.flip();
            nb.put(this.b);
            this.b = nb;
         }
      }

      void v(float x, float y, float z, int rgba) {
         this.ensure();
         this.b.putFloat(x).putFloat(y).putFloat(z).putInt(rgba);
         this.n++;
      }

      void vt(float x, float y, float z, float u, float v, int rgba) {
         if (this.b.remaining() < 24) {
            ByteBuffer nb = ByteBuffer.allocateDirect(this.b.capacity() * 2).order(ByteOrder.nativeOrder());
            this.b.flip();
            nb.put(this.b);
            this.b = nb;
         }

         this.b.putFloat(x).putFloat(y).putFloat(z).putFloat(u).putFloat(v).putInt(rgba);
         this.n++;
      }
   }

   static final class Result {
      /** cloud blocks per level (0 = fine, 1 = coarse) */
      final VB[] cloud = new VB[2];
      VB shaft;
      final int[] ui0 = new int[2];
      final int[] uj0 = new int[2];
      double ts;
      double camX;
      double camZ;
      double offX;
      double offZ;
   }

   static int rgba(float r, float g, float b, float a) {
      int ir = Math.max(0, Math.min(255, (int)(r * 255.0F)));
      int ig = Math.max(0, Math.min(255, (int)(g * 255.0F)));
      int ib = Math.max(0, Math.min(255, (int)(b * 255.0F)));
      int ia = Math.max(0, Math.min(255, (int)(a * 255.0F)));
      return ir | ig << 8 | ib << 16 | ia << 24;
   }

   /** The cloud density at one point, same function as the shader's: n = noise, w = cover/storm; h = height inside the cloud (0..1). */
   static float density(double u, double v, float h, float cover, float storm) {
      float n1 = Weather.samp(NOISE_R, u * (1.0 / 1800.0), v * (1.0 / 1800.0));
      float n2 = Weather.samp(NOISE_G, u * (1.0 / 300.0) + h * 0.012, v * (1.0 / 300.0) + h * 0.008);
      float wt = 0.66F - 0.26F * storm; // thunderheads get more fine detail: cauliflower tops
      float n = n1 * wt + n2 * (1.0F - wt);
      float top = 0.40F + 0.35F * storm;
      float prof = Weather.smooth(0.0F, 0.16F, h) * (1.0F - Weather.smooth(top, 1.0F, h));
      float edge = 0.80F + (0.20F - 0.80F) * cover + (h - 0.35F) * 0.10F * (1.0F - 1.5F * storm); // storms widen with height: an anvil
      return Math.max(0.0F, Math.min(1.0F, (n - edge) * 2.6F)) * prof;
   }

   /** Vertical size of the cloud above its base: thin sheets in fair weather, towers where the thunderhead (core) is, with lumpy tops. */
   static float thickness(float cover, float core, double u, double v) {
      float lump = Weather.samp(NOISE_R, u * (1.0 / 600.0), v * (1.0 / 600.0));
      // Update 3.12: the tower keeps rising all the way to the core (a dome: the old smooth(0.22, 0.58) flattened out into a plateau), its top is cauliflower
      float cauli = Weather.samp(NOISE_G, u * (1.0 / 130.0) + 0.21, v * (1.0 / 130.0) + 0.67);
      float dome = (float)Math.pow(Weather.smooth(0.10F, 0.98F, core), 1.2);
      return 46.0F + 28.0F * cover + 190.0F * dome * (0.70F + 0.30F * lump) * (0.88F + 0.24F * cauli);
   }

   static float groundAt(World w, int x, int z) {
      if (w == null) {
         return 64.0F;
      }

      Chunk c = w.chunkAt(x, z);
      if (c != null) {
         return c.hm[(z & 15) * 16 + (x & 15)] + 1.0F;
      }

      return Math.max(62, w.gen.column(x, z).h) + 1.0F;
   }

   /** Builds the whole cloud + rain-curtain mesh around (camX, camZ) for the weather at {@code sn}. */
   static Result build(Weather.Snap sn, World world, double camX, double camZ) {
      Result res = new Result();
      res.ts = sn.ts;
      res.camX = camX;
      res.camZ = camZ;
      res.offX = sn.oax;
      res.offZ = sn.oaz;
      for (int lv = 0; lv < 2; lv++) {
         res.cloud[lv] = cloudLevel(sn, camX, camZ, lv, res);
      }

      res.shaft = shafts(sn, world, camX, camZ, res);
      return res;
   }

   /** One level of blocky clouds: a column per cell with a bottom and a top, faces only where a neighbour does not cover them. */
   static VB cloudLevel(Weather.Snap sn, double camX, double camZ, int lv, Result res) {
      final int cell = CELLS[lv];
      final float layer = LAYERS[lv];
      final int rad = RADS[lv];
      final int n = rad * 2;
      int ci = (int)Math.floor((camX - sn.oax) / cell);
      int cj = (int)Math.floor((camZ - sn.oaz) / cell);
      int ui0 = ci - rad;
      int uj0 = cj - rad;
      res.ui0[lv] = ui0;
      res.uj0[lv] = uj0;
      float[] top = new float[n * n];
      float[] bot = new float[n * n];
      float[] cov = new float[n * n];
      float[] stm = new float[n * n];
      float[] rnn = new float[n * n];
      float[] thk = new float[n * n];
      float[] o3 = new float[4];
      for (int j = 0; j < n; j++) {
         for (int i = 0; i < n; i++) {
            int idx = j * n + i;
            double u = (ui0 + i + 0.5) * cell;
            double v = (uj0 + j + 0.5) * cell;
            top[idx] = bot[idx] = Float.NaN;
            if (lv == 1 && Math.abs(u + sn.oax - camX) < NEAR_R - 24.0F && Math.abs(v + sn.oaz - camZ) < NEAR_R - 24.0F) {
               continue; // the fine level covers this
            }

            if (lv == 0 && (Math.abs(u + sn.oax - camX) > NEAR_R || Math.abs(v + sn.oaz - camZ) > NEAR_R)) {
               continue;
            }

            sn.at(u + sn.oax, v + sn.oaz, o3);
            float cover = o3[0];
            float storm = o3[2];
            cov[idx] = cover;
            stm[idx] = storm;
            rnn[idx] = o3[1];
            if (cover < 0.03F) {
               continue;
            }

            float base = Weather.baseAlt(storm);
            float thick = thickness(cover, o3[3], u, v);
            int first = -1;
            int last = -1;
            int steps = 14;
            for (int k = 0; k <= steps; k++) {
               float h = (float)k / steps;
               float d = density(u, v, h, cover, storm);
               if (d > 0.28F) {
                  if (first < 0) {
                     first = k;
                  }

                  last = k;
               }
            }

            if (first < 0) {
               continue;
            }

            float y0 = base + (first - 0.5F) / steps * thick;
            float y1 = base + (last + 0.5F) / steps * thick;
            // a ragged, lowered base under storm clouds
            y0 -= storm * (Weather.samp(NOISE_G, u * (1.0 / 140.0), v * (1.0 / 140.0)) * 22.0F);
            int kb = Math.round(y0 / layer);
            int kt = Math.max(kb + 1, Math.round(y1 / layer));
            bot[idx] = kb * layer;
            top[idx] = kt * layer;
            thk[idx] = top[idx] - bot[idx];
         }
      }

      VB cb = acquire();
      final float edgeR = RADS[1] * (float)CELLS[1];
      for (int j = 0; j < n; j++) {
         for (int i = 0; i < n; i++) {
            int idx = j * n + i;
            if (Float.isNaN(top[idx])) {
               continue;
            }

            float b = bot[idx];
            float t = top[idx];
            float x0 = i * cell;
            float x1 = x0 + cell;
            float z0 = j * cell;
            float z1 = z0 + cell;
            float storm = Math.max(stm[idx], 0.65F * rnn[idx]); // darkness: thunderheads and rain-bearing cloud are dark
            float cover = cov[idx];
            double cu = (ui0 + i + 0.5) * cell;
            double cv = (uj0 + j + 0.5) * cell;
            float dx = (float)(cu + sn.oax - camX);
            float dz = (float)(cv + sn.oaz - camZ);
            float dd = (float)Math.sqrt(dx * dx + dz * dz);
            float fade = lv == 1 ? 1.0F - Weather.smooth(edgeR * 0.80F, edgeR * 0.97F, dd) : 1.0F;
            if (fade <= 0.01F) {
               continue;
            }

            // lumpy shading: the tops of storm towers and the bellies of the clouds are not one flat grey. Sun-lit tops stay
            // fairly bright, the flanks and above all the bases of a thunderhead are dark.
            float lump = Weather.samp(NOISE_G, cu * (1.0 / 90.0) + 0.37, cv * (1.0 / 90.0) + 0.11);
            float a = 0.90F * fade;
            float grey = Weather.smooth(0.45F, 1.0F, cover);
            float tc = (1.0F - 0.22F * grey) * (1.0F - 0.38F * storm) * (0.90F + 0.20F * lump);
            int ct = rgba(tc, tc, tc * 1.02F, a);
            float bc = (0.66F - 0.54F * storm) * (1.0F - 0.30F * grey * (1.0F - storm)) * (1.0F - 0.30F * Math.min(1.0F, thk[idx] / 80.0F)) * (0.78F + 0.44F * lump);
            int cbm = rgba(bc, bc, bc * 1.06F, Math.min(1.0F, a * 1.05F));
            float sideK = (0.84F - 0.56F * storm) * (1.0F - 0.18F * grey * (1.0F - storm)) * (0.92F + 0.16F * lump);
            float grad = 0.30F + 0.40F * storm;
            // top and bottom
            cb.v(x0, t, z0, ct);
            cb.v(x0, t, z1, ct);
            cb.v(x1, t, z1, ct);
            cb.v(x1, t, z0, ct);
            cb.v(x0, b, z0, cbm);
            cb.v(x1, b, z0, cbm);
            cb.v(x1, b, z1, cbm);
            cb.v(x0, b, z1, cbm);
            // sides: only where the neighbour column does not cover them
            for (int s = 0; s < 4; s++) {
               int ni = i + (s == 0 ? 1 : (s == 1 ? -1 : 0));
               int nj = j + (s == 2 ? 1 : (s == 3 ? -1 : 0));
               float nb = Float.NaN;
               float nt = Float.NaN;
               if (ni >= 0 && nj >= 0 && ni < n && nj < n) {
                  nb = bot[nj * n + ni];
                  nt = top[nj * n + ni];
               }

               if (Float.isNaN(nb)) {
                  side(cb, s, x0, x1, z0, z1, b, t, b, t, sideK, grad, a);
               } else {
                  if (nb > b) {
                     side(cb, s, x0, x1, z0, z1, b, Math.min(t, nb), b, t, sideK, grad, a);
                  }

                  if (nt < t) {
                     side(cb, s, x0, x1, z0, z1, Math.max(b, nt), t, b, t, sideK, grad, a);
                  }
               }
            }
         }
      }

      return cb;
   }

   /** One vertical face of a cloud column between heights ya and yb; the colour darkens towards the cloud's own base. */
   static void side(VB cb, int s, float x0, float x1, float z0, float z1, float ya, float yb, float colBot, float colTop, float k, float grad, float a) {
      float ha = (ya - colBot) / Math.max(1.0F, colTop - colBot);
      float hb = (yb - colBot) / Math.max(1.0F, colTop - colBot);
      float sa = 1.0F - grad + grad * ha;
      float sb = 1.0F - grad + grad * hb;
      int ca = rgba(k * sa, k * sa, k * sa * 1.03F, a);
      int cbc = rgba(k * sb, k * sb, k * sb * 1.03F, a);
      switch (s) {
         case 0: // +x
            cb.v(x1, ya, z0, ca);
            cb.v(x1, yb, z0, cbc);
            cb.v(x1, yb, z1, cbc);
            cb.v(x1, ya, z1, ca);
            break;
         case 1: // -x
            cb.v(x0, ya, z1, ca);
            cb.v(x0, yb, z1, cbc);
            cb.v(x0, yb, z0, cbc);
            cb.v(x0, ya, z0, ca);
            break;
         case 2: // +z
            cb.v(x1, ya, z1, ca);
            cb.v(x1, yb, z1, cbc);
            cb.v(x0, yb, z1, cbc);
            cb.v(x0, ya, z1, ca);
            break;
         default: // -z
            cb.v(x0, ya, z0, ca);
            cb.v(x0, yb, z0, cbc);
            cb.v(x1, yb, z0, cbc);
            cb.v(x1, ya, z0, ca);
      }
   }

   /**
    * The rain / snow curtains: three crossing vertical planes per grid point where it rains, soft at the edges. A fine grid
    * (64 blocks) near the camera and a coarse one (192 blocks) out to 3.6 km, so a storm on the horizon has its rain too.
    */
   static VB shafts(Weather.Snap sn, World world, double camX, double camZ, Result res) {
      VB vb = acquire();
      float[] o3 = new float[4];
      Weather wx = world == null ? null : world.weather;
      for (int level = 0; level < 2; level++) {
         final int G = level == 0 ? 64 : 192;
         final int RG = level == 0 ? 20 : 19;
         final float inner = level == 0 ? 14.0F : 1180.0F;
         final float outer = level == 0 ? 1280.0F : 3600.0F;
         final float hw = G * 0.97F;
         int gx0 = (int)Math.floor(camX / G);
         int gz0 = (int)Math.floor(camZ / G);
         for (int dj = -RG; dj <= RG; dj++) {
            for (int di = -RG; di <= RG; di++) {
               double wx0 = (gx0 + di + 0.5) * G;
               double wz0 = (gz0 + dj + 0.5) * G;
               double dxc = wx0 - camX;
               double dzc = wz0 - camZ;
               float dist = (float)Math.sqrt(dxc * dxc + dzc * dzc);
               if (dist > outer || dist < inner) {
                  continue;
               }

               sn.at(wx0, wz0, o3);
               float rain = o3[1];
               if (rain < 0.10F) {
                  continue;
               }

               int kind = wx == null ? 1 : wx.precipKind((int)Math.floor(wx0), (int)Math.floor(wz0));
               if (kind < 0) {
                  continue;
               }

               float ground = groundAt(world, (int)Math.floor(wx0), (int)Math.floor(wz0));
               float topY = Weather.baseAlt(o3[2]) + 6.0F;
               if (topY - ground < 20.0F) {
                  continue;
               }

               float edgeFade = 1.0F - Weather.smooth(outer * 0.78F, outer, dist);
               float nearFade = level == 0 ? Weather.smooth(25.0F, 150.0F, dist) : 1.0F;
               float strength = Math.min(1.0F, rain * 1.25F) * edgeFade * nearFade;
               if (strength < 0.02F) {
                  continue;
               }

               float dk = 1.0F - 0.50F * o3[2];
               float cr = (kind == 2 ? 0.95F : 0.72F) * dk;
               float cg = (kind == 2 ? 0.97F : 0.80F) * dk;
               float cb2 = 1.0F * (1.0F - 0.30F * o3[2]);
               float height = topY - ground;
               float vTop = height / 90.0F;
               float ux = (float)(wx0 - res.offX - (double)res.ui0[0] * CELLS[0]);
               float uz = (float)(wz0 - res.offZ - (double)res.uj0[0] * CELLS[0]);
               for (int pl = 0; pl < 3; pl++) {
                  double ang = pl * Math.PI / 3.0 + ((gx0 + di) * 7 + (gz0 + dj) * 13) % 5 * 0.2;
                  float ax = (float)Math.cos(ang) * hw;
                  float az = (float)Math.sin(ang) * hw;
                  float ur = hw * 2.0F / 40.0F;
                  float uOff = ((gx0 + di) * 31 + (gz0 + dj) * 17 + pl * 5) % 13 * 0.31F;
                  // rows: above the cloud base (invisible), cloud base (full), mid, ground (slightly weaker); columns: edge, centre, edge
                  float[] ys = new float[]{topY + 8.0F, topY - 22.0F, ground + height * 0.35F, ground};
                  float[] al = new float[]{0.0F, 0.95F, 0.85F, 0.55F};
                  float[] vs = new float[]{vTop + 0.1F, vTop - 0.25F, vTop * 0.35F, 0.0F};
                  for (int row = 0; row < 3; row++) {
                     for (int half = 0; half < 2; half++) {
                        float xa = half == 0 ? -1.0F : 0.0F;
                        float xb = half == 0 ? 0.0F : 1.0F;
                        float fa = half == 0 ? 0.0F : 1.0F;
                        float fb = half == 0 ? 1.0F : 0.0F;
                        float ua = uOff + (xa + 1.0F) * 0.5F * ur;
                        float ub = uOff + (xb + 1.0F) * 0.5F * ur;
                        int c00 = rgba(cr, cg, cb2, al[row] * fa * strength);
                        int c01 = rgba(cr, cg, cb2, al[row] * fb * strength);
                        int c10 = rgba(cr, cg, cb2, al[row + 1] * fa * strength);
                        int c11 = rgba(cr, cg, cb2, al[row + 1] * fb * strength);
                        vb.vt(ux + ax * xa, ys[row], uz + az * xa, ua, vs[row], c00);
                        vb.vt(ux + ax * xb, ys[row], uz + az * xb, ub, vs[row], c01);
                        vb.vt(ux + ax * xb, ys[row + 1], uz + az * xb, ub, vs[row + 1], c11);
                        vb.vt(ux + ax * xa, ys[row + 1], uz + az * xa, ua, vs[row + 1], c10);
                     }
                  }
               }
            }
         }
      }

      return vb;
   }

   // ================================================================ drawing: clouds (no-shader path)
   void upload(Result res) {
      if (this.vboCloud[0] == 0) {
         this.vboCloud[0] = GL15.glGenBuffers();
         this.vboCloud[1] = GL15.glGenBuffers();
         this.vboShaft = GL15.glGenBuffers();
      }

      for (int lv = 0; lv < 2; lv++) {
         res.cloud[lv].b.flip();
         GL15.glBindBuffer(34962, this.vboCloud[lv]);
         GL15.glBufferData(34962, res.cloud[lv].b, 35044);
         this.nCloud[lv] = res.cloud[lv].n;
         this.ui0[lv] = res.ui0[lv];
         this.uj0[lv] = res.uj0[lv];
      }

      res.shaft.b.flip();
      GL15.glBindBuffer(34962, this.vboShaft);
      GL15.glBufferData(34962, res.shaft.b, 35044);
      this.nShaft = res.shaft.n;
      GL15.glBindBuffer(34962, 0);
      this.builtTs = res.ts;
      this.builtCamX = res.camX;
      this.builtCamZ = res.camZ;
      this.builtOffX = res.offX;
      this.builtOffZ = res.offZ;
      this.built = true;
   }

   /** Starts a worker job when the mesh is stale (time passed, the camera moved) and uploads a finished one. */
   void maintain(Weather.Snap sn, double camX, double camZ) {
      Result done = this.ready.getAndSet(null);
      if (done != null) {
         this.upload(done);
         recycle(done);
      }

      if (this.building) {
         return;
      }

      boolean stale = !this.built
         || this.wantRebuild
         || sn.ts - this.builtTs > 2.5
         || Math.abs(camX - this.builtCamX) > 40.0
         || Math.abs(camZ - this.builtCamZ) > 40.0;
      if (!stale) {
         return;
      }

      this.wantRebuild = false;
      this.building = true;
      final World w = this.r.g.world;
      this.worker.execute(() -> {
         try {
            this.ready.set(build(sn, w, camX, camZ));
         } catch (Throwable t) {
            t.printStackTrace();
         } finally {
            this.building = false;
         }
      });
   }

   /** Colour multiplier of the clouds: warm at sunrise/sunset, blue-grey at night, bright white in the day. */
   float[] cloudTint(float daylight, float sunAngle) {
      float sy = (float)Math.cos(sunAngle);
      float day = Math.max(0.0F, Math.min(1.0F, sy * 3.0F));
      float night = 0.10F;
      float r = night + (1.0F - night) * Math.max(0.0F, Math.min(1.0F, (daylight - 0.1F) / 0.8F));
      float g = r;
      float b = night * 1.5F + (1.0F - night * 1.5F) * Math.max(0.0F, Math.min(1.0F, (daylight - 0.05F) / 0.8F));
      // low sun: orange
      float warm = Math.max(0.0F, 1.0F - Math.abs(sy) / 0.28F) * (daylight > 0.05F ? 1.0F : 0.0F);
      r = r * (1.0F - 0.08F * warm);
      g = g * (1.0F - 0.30F * warm);
      b = b * (1.0F - 0.50F * warm);
      float f = this.flash;
      r += (0.85F - r) * f * 0.8F;
      g += (0.90F - g) * f * 0.8F;
      b += (1.00F - b) * f * 0.8F;
      float grey = this.overcast * 0.06F;
      return new float[]{Math.min(1.0F, r - grey), Math.min(1.0F, g - grey), Math.min(1.0F, b - grey * 0.5F)};
   }

   /** The blocky no-shader clouds. Called where the old flat cloud layer used to be drawn (before the translucent blocks). */
   void drawClouds(float partial, float fogR, float fogG, float fogB, float fogStart, float fogEnd) {
      if (this.snap == null) {
         return;
      }

      this.ensureTextures();
      this.maintain(this.snap, this.r.camX, this.r.camZ);
      if (!this.built || this.nCloud[0] + this.nCloud[1] == 0) {
         return;
      }

      Weather.Snap sn = this.snap;
      float[] tint = this.cloudTint(this.r.daylight, this.r.sunAngle);
      GL11.glEnable(3553);
      GL11.glBindTexture(3553, this.texWhite);
      GL11.glTexEnvi(8960, 8704, 34160);
      GL11.glTexEnvi(8960, 34161, 8448);
      GL11.glTexEnvi(8960, 34176, 34167);
      GL11.glTexEnvi(8960, 34177, 34166);
      GL11.glTexEnvi(8960, 34162, 8448);
      GL11.glTexEnvi(8960, 34184, 34167);
      GL11.glTexEnvi(8960, 34185, 34166);
      GL11.glTexEnvfv(8960, 8705, new float[]{tint[0], tint[1], tint[2], 1.0F});
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      GL11.glDisable(3008);
      GL11.glEnable(2912);
      GL11.glFogi(2917, 9729);
      GL11.glFogf(2915, FAR_FOG0);
      GL11.glFogf(2916, FAR_FOG1);
      GL11.glFogfv(2918, new float[]{fogR, fogG, fogB, 1.0F});
      GL11.glEnableClientState(32884);
      GL11.glEnableClientState(32886);
      // pass 1: depth only, so every pixel is blended with the cloud surface nearest to the camera only; pass 2: colour
      for (int pass = 0; pass < 2; pass++) {
         if (pass == 0) {
            GL11.glColorMask(false, false, false, false);
            GL11.glDepthMask(true);
         } else {
            GL11.glColorMask(true, true, true, true);
            GL11.glDepthMask(false);
            GL11.glDepthFunc(515);
         }

         for (int lv = 0; lv < 2; lv++) {
            if (this.nCloud[lv] == 0) {
               continue;
            }

            GL11.glPushMatrix();
            GL11.glTranslated(this.ui0[lv] * (double)CELLS[lv] + sn.oax - this.r.camX, -this.r.camY, this.uj0[lv] * (double)CELLS[lv] + sn.oaz - this.r.camZ);
            GL15.glBindBuffer(34962, this.vboCloud[lv]);
            GL11.glVertexPointer(3, 5126, 16, 0L);
            GL11.glColorPointer(4, 5121, 16, 12L);
            GL11.glDrawArrays(7, 0, this.nCloud[lv]);
            GL11.glPopMatrix();
         }
      }

      GL11.glDepthMask(true);
      GL15.glBindBuffer(34962, 0);
      GL11.glDisableClientState(32884);
      GL11.glDisableClientState(32886);
      GL11.glTexEnvi(8960, 8704, 8448);
      GL11.glDisable(3042);
      // back to the terrain's fog for the translucent blocks that follow
      GL11.glFogf(2915, fogStart);
      GL11.glFogf(2916, fogEnd);
      GL11.glFogfv(2918, new float[]{fogR, fogG, fogB, 1.0F});
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
   }

   // ================================================================ drawing: curtains, near precipitation, lightning
   /** Rain curtains, rain / snow around the player and lightning: drawn last, over the (cloud-composited) scene. */
   void drawOverlays(float partial, float fogR, float fogG, float fogB) {
      if (this.snap == null) {
         return;
      }

      Weather wx = this.r.g.world.weather;
      if (wx.suppressed) {
         return;
      }

      this.ensureTextures();
      if (this.r.shadersOn() && this.r.sh.cloudsOk && this.r.sh.sceneActive) {
         // the shader path draws no blocky clouds but still needs the curtains: keep the (cheap) mesh job running
         this.maintain(this.snap, this.r.camX, this.r.camZ);
      }

      GL11.glEnable(2929);
      GL11.glDepthMask(false);
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      GL11.glDisable(3008);
      GL11.glDisable(2884);
      float[] tint = this.cloudTint(this.r.daylight, this.r.sunAngle);
      this.drawShafts(partial, tint, fogR, fogG, fogB);
      this.drawNear(partial, tint);
      this.drawBolts(partial);
      GL11.glDepthMask(true);
      GL11.glDisable(3042);
      GL11.glDisable(2912);
      GL11.glTexEnvi(8960, 8704, 8448);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
   }

   void drawShafts(float partial, float[] tint, float fogR, float fogG, float fogB) {
      if (!this.built || this.nShaft == 0) {
         return;
      }

      Weather.Snap sn = this.snap;
      GL11.glPushMatrix();
      GL11.glTranslated(this.builtOffXNow(sn) - this.r.camX, -this.r.camY, this.builtOffZNow(sn) - this.r.camZ);
      GL11.glEnable(3553);
      GL11.glBindTexture(3553, this.snowy > 0.5F ? this.texSnowVeil : this.texShaft);
      GL11.glTexEnvi(8960, 8704, 8448);
      // unit 1: multiply with the light tint (previous * constant), alpha untouched
      GL13.glActiveTexture(33985);
      GL11.glEnable(3553);
      GL11.glBindTexture(3553, this.texWhite);
      GL11.glTexEnvi(8960, 8704, 34160);
      GL11.glTexEnvi(8960, 34161, 8448);
      GL11.glTexEnvi(8960, 34176, 34168);
      GL11.glTexEnvi(8960, 34177, 34166);
      GL11.glTexEnvi(8960, 34162, 7681);
      GL11.glTexEnvi(8960, 34184, 34168);
      float bright = 0.40F + 0.60F * Math.max(0.0F, Math.min(1.0F, this.r.daylight * 1.2F));
      GL11.glTexEnvfv(8960, 8705, new float[]{tint[0] * bright, tint[1] * bright, tint[2] * bright, 1.0F});
      GL13.glActiveTexture(33984);
      GL11.glEnable(2912);
      GL11.glFogi(2917, 9729);
      GL11.glFogf(2915, FAR_FOG0 * 0.8F);
      GL11.glFogf(2916, FAR_FOG1 * 0.8F);
      GL11.glFogfv(2918, new float[]{fogR, fogG, fogB, 1.0F});
      // rain falls: scroll the texture downwards
      GL11.glMatrixMode(5890);
      GL11.glPushMatrix();
      GL11.glLoadIdentity();
      GL11.glTranslatef(0.0F, (float)(sn.ts * 1.9 % 1.0), 0.0F);
      GL11.glMatrixMode(5888);
      GL11.glEnableClientState(32884);
      GL11.glEnableClientState(32886);
      GL13.glClientActiveTexture(33984);
      GL11.glEnableClientState(32888);
      GL15.glBindBuffer(34962, this.vboShaft);
      GL11.glVertexPointer(3, 5126, 24, 0L);
      GL11.glTexCoordPointer(2, 5126, 24, 12L);
      GL11.glColorPointer(4, 5121, 24, 20L);
      GL11.glDrawArrays(7, 0, this.nShaft);
      GL15.glBindBuffer(34962, 0);
      GL11.glDisableClientState(32888);
      GL11.glDisableClientState(32884);
      GL11.glDisableClientState(32886);
      GL11.glMatrixMode(5890);
      GL11.glPopMatrix();
      GL11.glMatrixMode(5888);
      GL11.glPopMatrix();
      GL13.glActiveTexture(33985);
      GL11.glDisable(3553);
      GL13.glActiveTexture(33984);
      GL11.glDisable(2912);
   }

   double builtOffXNow(Weather.Snap sn) {
      return this.ui0[0] * (double)CELLS[0] + sn.oax;
   }

   double builtOffZNow(Weather.Snap sn) {
      return this.uj0[0] * (double)CELLS[0] + sn.oaz;
   }

   /** Rain / snow streaks in a disc around the camera, stopped by whatever is overhead (the chunk heightmap). */
   void drawNear(float partial, float[] tint) {
      float amount = this.rain;
      if (amount < 0.04F || this.r.g.player.eyeInWater) {
         return;
      }

      World w = this.r.g.world;
      double cx = this.r.camX;
      double cy = this.r.camY;
      double cz = this.r.camZ;
      int rad = 4 + (int)(7.0F * amount);
      int bx = (int)Math.floor(cx);
      int bz = (int)Math.floor(cz);
      double t = (w.weather.ticks + partial) / 20.0;
      float bright = 0.45F + 0.55F * Math.max(0.0F, Math.min(1.0F, this.r.daylight * 1.1F));
      float lr = tint[0] * bright;
      float lg = tint[1] * bright;
      float lb = tint[2] * bright;
      float wx = this.snap.windX * 0.012F;
      float wz = this.snap.windZ * 0.012F;
      GL11.glEnable(3553);
      GL11.glTexEnvi(8960, 8704, 8448);
      for (int pass = 0; pass < 2; pass++) {
         // pass 0: rain columns, pass 1: snow columns (two texture binds, every column picks one by its biome)
         GL11.glBindTexture(3553, pass == 0 ? this.texRain : this.texSnow);
         GL11.glBegin(7);

         for (int dz = -rad; dz <= rad; dz++) {
            for (int dx = -rad; dx <= rad; dx++) {
               double d2 = dx * dx + dz * dz;
               if (d2 > rad * rad || d2 < 0.3) {
                  continue;
               }

               int x = bx + dx;
               int z = bz + dz;
               Chunk c = w.chunkAt(x, z);
               if (c == null) {
                  continue;
               }

               int kind = w.weather.precipKind(x, z);
               if (kind < 0 || (kind == 2) != (pass == 1)) {
                  continue;
               }

               int hm = c.hm[(z & 15) * 16 + (x & 15)] + 1;
               double yb = Math.max(hm, cy - 16.0);
               double yt = cy + 14.0;
               if (yb >= yt) {
                  continue;
               }

               int hash = x * 73856093 ^ z * 19349663;
               float f = (float)Math.sqrt(d2) / rad;
               double ddx = x + 0.5 - cx;
               double ddz = z + 0.5 - cz;
               double dl = Math.sqrt(ddx * ddx + ddz * ddz);
               // thin out the streaks that pass right in front of the lens: they would be fat bright bands on screen
               float alpha = (1.0F - f * f) * (pass == 0 ? 0.42F : 0.80F) * Math.min(1.0F, amount * 1.4F) * Weather.smooth(1.4F, 6.5F, (float)dl);
               float px = (float)(-ddz / dl * 0.5);
               float pz = (float)(ddx / dl * 0.5);
               float uoff = (hash & 31) / 32.0F;
               float speed = pass == 0 ? (float)(t * (1.55 + (hash >> 5 & 7) * 0.05)) : (float)(t * (0.28 + (hash >> 5 & 7) * 0.02));
               float vOff = (hash >> 8 & 63) / 64.0F + speed;
               float sway = pass == 0 ? 0.0F : (float)(Math.sin(t * 1.3 + (hash & 255)) * 0.35);
               float hgt = (float)(yt - yb);
               float v0 = vOff;
               float v1 = vOff + hgt / (pass == 0 ? 14.0F : 6.0F);
               float u0 = uoff;
               float u1 = uoff + (pass == 0 ? 0.5F : 1.0F);
               GL11.glColor4f(lr, lg, lb, alpha);
               float tx = (float)(x + 0.5 - cx) + sway;
               float tz2 = (float)(z + 0.5 - cz);
               float topShiftX = wx * hgt * (pass == 0 ? 1.0F : 0.4F);
               float topShiftZ = wz * hgt * (pass == 0 ? 1.0F : 0.4F);
               GL11.glTexCoord2f(u0, v0);
               GL11.glVertex3f(tx - px, (float)(yb - cy), tz2 - pz);
               GL11.glTexCoord2f(u1, v0);
               GL11.glVertex3f(tx + px, (float)(yb - cy), tz2 + pz);
               GL11.glTexCoord2f(u1, v1);
               GL11.glVertex3f(tx + px + topShiftX, (float)(yt - cy), tz2 + pz + topShiftZ);
               GL11.glTexCoord2f(u0, v1);
               GL11.glVertex3f(tx - px + topShiftX, (float)(yt - cy), tz2 - pz + topShiftZ);
            }
         }

         GL11.glEnd();
      }
   }

   /** Lightning: glowing jagged ribbons facing the camera, additive, hidden behind terrain. */
   void drawBolts(float partial) {
      Weather wx = this.r.g.world.weather;
      if (wx.bolts.isEmpty()) {
         return;
      }

      GL11.glDisable(3553);
      GL11.glBlendFunc(770, 1);
      GL11.glLineWidth(1.0F);
      double cx = this.r.camX;
      double cy = this.r.camY;
      double cz = this.r.camZ;
      for (int pass = 0; pass < 2; pass++) {
         GL11.glBegin(7);
         for (Weather.Bolt b : wx.bolts) {
            float e = b.env(partial);
            if (e < 0.02F) {
               continue;
            }

            double mdx = b.x - cx;
            double mdz = b.z - cz;
            double dist = Math.sqrt(mdx * mdx + mdz * mdz) + 20.0;
            float wscale = (float)Math.max(0.8, dist * 0.0050);
            if (pass == 0) {
               GL11.glColor4f(0.45F, 0.58F, 1.0F, 0.30F * e);
            } else {
               GL11.glColor4f(0.92F, 0.95F, 1.0F, Math.min(1.0F, 0.35F + e));
            }

            float widen = pass == 0 ? 6.5F : 1.0F;
            for (int i = 0; i < b.seg.length; i += 7) {
               double x0 = b.seg[i] - cx;
               double y0 = b.seg[i + 1] - cy;
               double z0 = b.seg[i + 2] - cz;
               double x1 = b.seg[i + 3] - cx;
               double y1 = b.seg[i + 4] - cy;
               double z1 = b.seg[i + 5] - cz;
               double dx = x1 - x0;
               double dy = y1 - y0;
               double dz = z1 - z0;
               double mx = (x0 + x1) * 0.5;
               double my = (y0 + y1) * 0.5;
               double mz = (z0 + z1) * 0.5;
               // perpendicular to both the segment and the line of sight
               double px = dy * mz - dz * my;
               double py = dz * mx - dx * mz;
               double pz = dx * my - dy * mx;
               double pl = Math.sqrt(px * px + py * py + pz * pz);
               if (pl < 1.0E-6) {
                  continue;
               }

               double wd = b.seg[i + 6] * wscale * widen * 0.5 / pl;
               px *= wd;
               py *= wd;
               pz *= wd;
               GL11.glVertex3d(x0 - px, y0 - py, z0 - pz);
               GL11.glVertex3d(x0 + px, y0 + py, z0 + pz);
               GL11.glVertex3d(x1 + px, y1 + py, z1 + pz);
               GL11.glVertex3d(x1 - px, y1 - py, z1 - pz);
            }
         }

         GL11.glEnd();
      }

      GL11.glBlendFunc(770, 771);
      GL11.glEnable(3553);
   }

   void dispose() {
      this.worker.shutdownNow();
   }
}
