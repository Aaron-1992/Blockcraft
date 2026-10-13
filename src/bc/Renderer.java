package bc;

import bc.Chunk.Section;
import bc.Game.Particle;
import bc.Mesher.Result;
import bc.World.Falling;
import bc.World.Hit;
import bc.World.Item;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.system.MemoryUtil;

public final class Renderer {
   final Game g;
   final ExecutorService meshPool;
   int meshThreads = 2;
   final ConcurrentLinkedQueue<Renderer.Done> done = new ConcurrentLinkedQueue();
   final AtomicInteger inflight = new AtomicInteger();
   public double camX;
   public double camY;
   public double camZ;
   /** the direction the camera looks (the player's, or turned around for the camera in front of the player) */
   float viewYaw;
   float viewPitch;
   public Lod lod;
   public Shaders sh;
   /** Update 3.8: weather rendering (clouds without shaders, rain, snow, lightning). */
   public final WeatherFx wfx = new WeatherFx(this);
   int shFrames;
   /** Update 3.18: a section near the player was built or changed since the shadow map was drawn. */
   boolean shadowDirty;
   final float[] proj = new float[16];
   final float[] mv = new float[16];
   final float[][] planes = new float[6][4];
   public float fogR;
   public float fogG;
   public float fogB;
   public float topR;
   public float topG;
   public float topB;
   float caveF = 1.0F;
   public float daylight = 1.0F;
   public float skyLightFactor = 1.0F;
   public float sunAngle;
   float lastLm = -1.0F;
   float lastGamma = -1.0F;
   float lastAmbient = -1.0F;
   final float[] stars = new float[2100];
   final float[] skyTmp = new float[8];
   public int visibleSections;
   public int drawCalls;
   static final int STRIDE = 44;
   final ArrayList<Renderer.Cand> cands = new ArrayList();
   static final int[][] RING;
   final ArrayList<long[]> tmp = new ArrayList();
   final ArrayList<Renderer.Vis> vis = new ArrayList();
   final ArrayList<float[]> placedLights = new ArrayList();
   int lightScanCd = 0;
   double lsX;
   double lsY;
   double lsZ;
   static final HashMap<Integer, float[]> itemGeo;

   public boolean shadersOn() {
      return this.sh != null && this.sh.ok && this.g.opt.shaders;
   }

   public void setLod(World var1) {
      if (this.sh != null) {
         this.sh.mapValid = false;
      }

      if (this.lod != null) {
         this.lod.dispose();
      }

      // Update 3.12: only the Overworld has distant terrain (LOD); the Nether, the End, and void modes (Skyblock/Skywars/Bedwars) have open sky/void
      boolean voidMode = var1 != null && var1.settings != null && var1.settings.isVoidMode();
      this.lod = var1 == null || var1.dim != Dim.OVERWORLD || voidMode ? null : new Lod(this, var1.gen, var1.lodMap);
   }

   public Renderer(Game var1) {
      this.g = var1;
      int var2 = World.genThreads();
      this.meshThreads = var2;
      this.meshPool = Executors.newFixedThreadPool(var2, var0 -> {
         Thread var1x = new Thread(var0, "mesher");
         var1x.setDaemon(true);
         var1x.setPriority(4);
         return var1x;
      });
      Random var3 = new Random(4242L);

      for (int var4 = 0; var4 < 700; var4++) {
         double var5 = var3.nextDouble() * Math.PI * 2.0;
         double var7 = var3.nextDouble() * 2.0 - 1.0;
         double var9 = Math.sqrt(1.0 - var7 * var7);
         this.stars[var4 * 3] = (float)(Math.cos(var5) * var9 * 150.0);
         this.stars[var4 * 3 + 1] = (float)(var7 * 150.0);
         this.stars[var4 * 3 + 2] = (float)(Math.sin(var5) * var9 * 150.0);
      }
   }

   public void shutdown() {
      this.meshPool.shutdownNow();
      this.wfx.dispose();
      if (this.lod != null) {
         this.lod.dispose();
      }
   }

   public static void clearItemGeo() {
      synchronized (itemGeo) {
         itemGeo.clear();
      }
   }

   public void onUnload(Chunk var1) {
      for (Section var5 : var1.sections) {
         if (var5.vboO != 0) {
            GL15.glDeleteBuffers(var5.vboO);
         }

         if (var5.vboT != 0) {
            GL15.glDeleteBuffers(var5.vboT);
         }

         var5.vboO = var5.vboT = 0;
         var5.nO = var5.nT = 0;
      }
   }

   /** Completely purges all GPU section VBOs and in-flight mesh jobs when an arena world is reset. */
   public void clearWorldMeshes(World w) {
      if (w != null && this.g != null && this.g.window != 0L) {
         for (Chunk c : w.chunks.values()) {
            this.onUnload(c);
         }
      }
      Renderer.Done d;
      while ((d = (Renderer.Done)this.done.poll()) != null) {
         this.inflight.decrementAndGet();
         d.c.sections[d.sy].inFlight = false;
         free(d.r);
      }
      this.vis.clear();
      this.shadowDirty = true;
      if (this.lod != null) {
         this.lod.invalidateAll();
      }
   }

   boolean ready(World var1, Chunk var2) {
      if (!var2.lit) {
         return false;
      }

      for (int var3 = -1; var3 <= 1; var3++) {
         for (int var4 = -1; var4 <= 1; var4++) {
            Chunk var5 = var1.chunk(var2.cx + var3, var2.cz + var4);
            if (var5 == null || !var5.lit) {
               return false;
            }
         }
      }

      return true;
   }

   /**
    * Is the chunk on the screen as real terrain, so that the LOD under it can stay hidden? Update 3.11: once every section has been meshed
    * once, yes - a section that is being re-meshed (a burning tree, a block placed) still shows its old mesh until the new one replaces it,
    * and the distant terrain, with its generator trees, used to flash up through the real chunk for those moments.
    */
   public boolean chunkMeshed(int var1, int var2) {
      Chunk var3 = this.g.world.chunk(var1, var2);
      return var3 != null && var3.lit && var3.everMeshed;
   }

   public void scheduleMeshes(World var1, int var2) {
      int var3 = this.meshThreads * 6;
      if (this.inflight.get() < var3) {
         int var4 = (int)Math.floor(this.camX) >> 4;
         int var5 = (int)Math.floor(this.camZ) >> 4;
         boolean isVoid = var1.settings != null && var1.settings.isVoidMode();
         int var6 = isVoid ? Math.max(10, this.g.opt.renderDistance) : this.g.opt.renderDistance;
         int var7 = (var6 + 1) * (var6 + 1);
         boolean var8 = this.g.opt.smooth;
         int var9 = 0;

         label56:
         for (int[] var13 : RING) {
            if (var13[0] * var13[0] + var13[1] * var13[1] <= var7) {
               Chunk var14 = var1.chunk(var4 + var13[0], var5 + var13[1]);
               if (var14 != null) {
                  boolean hasDirty = false;
                  for (int sy = 0; sy < 16; sy++) {
                     if (var14.sections[sy].dirty && !var14.sections[sy].inFlight) {
                        hasDirty = true;
                        break;
                     }
                  }
                  if (!hasDirty) {
                     continue;
                  }
                  if (!this.ready(var1, var14)) {
                     continue label56;
                  }

                  // Fast inline pass: clear empty air sections immediately without scanning underground rock on the main thread
                  for (int sy = 0; sy < 16; sy++) {
                     Section sec = var14.sections[sy];
                     if (sec.dirty && !sec.inFlight && var14.isSectionEmpty(sy)) {
                        sec.dirty = false;
                        sec.conn = CaveCull.ALL;
                        sec.nO = 0;
                        sec.nT = 0;
                        var14.markMeshed(sy);
                        if (sec.vboO != 0) {
                           GL15.glDeleteBuffers(sec.vboO);
                           sec.vboO = 0;
                        }
                        if (sec.vboT != 0) {
                           GL15.glDeleteBuffers(sec.vboT);
                           sec.vboT = 0;
                        }
                     }
                  }

                  int surfSy = Math.max(3, Math.min(14, var14.hm[136] >> 4));
                  for (int step = 0; step < 16; step++) {
                     int var17 = surfSy + ((step & 1) == 0 ? -(step >> 1) : ((step + 1) >> 1));
                     if (var17 < 0 || var17 >= 16) {
                        continue;
                     }
                     Section var18 = var14.sections[var17];
                     if (var18.dirty && !var18.inFlight) {
                        if (var9 >= var2 || this.inflight.get() >= var3) {
                           return;
                        }

                        int var19 = var17;
                        var18.dirty = false;
                        var18.inFlight = true;
                        this.inflight.incrementAndGet();
                        var9++;
                        this.meshPool.execute(() -> {
                           Renderer.Done var5x = new Renderer.Done();
                           var5x.c = var14;
                           var5x.sy = var19;

                           try {
                              var5x.r = Mesher.build(var1, var14, var19, var8);
                           } catch (Throwable var7x) {
                              var7x.printStackTrace();
                              var5x.r = new Result();
                           }

                           this.done.add(var5x);
                        });
                     }
                  }
               }
               continue;
            }
            break;
         }

         return;
      }
   }

   public void uploadMeshes(int var1) {
      this.uploadMeshes(var1, 3000000L);
   }

   public void uploadMeshes(int var1, long var2) {
      long var4 = System.nanoTime();

      for (int var6 = 0; var6 < var1 && (var6 <= 0 || System.nanoTime() - var4 <= var2); var6++) {
         Renderer.Done var7 = (Renderer.Done)this.done.poll();
         if (var7 == null) {
            break;
         }

         this.inflight.decrementAndGet();
         Section var8 = var7.c.sections[var7.sy];
         var8.inFlight = false;
         if (var7.c.unloaded) {
            free(var7.r);
         } else {
            this.upload(var7.c, var7.sy, var7.r);
         }
      }
   }

   static void free(Result var0) {
      if (var0.opaque != null) {
         MemoryUtil.memFree(var0.opaque);
      }

      if (var0.trans != null) {
         MemoryUtil.memFree(var0.trans);
      }
   }

   /** The sections to draw this frame: one object per section, kept from frame to frame (the frame used to make a thousand of them). */
   private final ArrayList<Renderer.Vis> visPool = new ArrayList<>();
   private int visN;
   private final CaveCull.Walk walk = new CaveCull.Walk();
   private final CaveCull.Visible frustumTest = (rx, ry, rz) -> this.visible(rx, ry, rz, 14.0F);
   static final boolean CAVE_CULL = !"false".equals(System.getProperty("bc.cavecull"));
   static final java.util.Comparator<Renderer.Vis> VIS_NEAR_FIRST = (a, b) -> Float.compare(a.d, b.d);

   /** Adds section {@code sy} of {@code c} to the list of what is drawn, if it has something to draw and is inside the pyramid of view. */
   private void addVisible(Chunk c, int sy) {
      Section s = c.sections[sy];
      if (s.nO != 0 || s.nT != 0) {
         float rx = (float)(c.cx * 16 + 8 - this.camX);
         float ry = (float)(sy * 16 + 8 - this.camY);
         float rz = (float)(c.cz * 16 + 8 - this.camZ);
         if (this.visible(rx, ry, rz, 14.0F)) {
            if (this.visN == this.visPool.size()) {
               this.visPool.add(new Renderer.Vis());
            }

            Renderer.Vis v = this.visPool.get(this.visN++);
            v.c = c;
            v.sy = sy;
            v.d = rx * rx + ry * ry + rz * rz;
            this.vis.add(v);
         }
      }
   }

   void upload(Chunk var1, int var2, Result var3) {
      Section var4 = var1.sections[var2];
      var4.conn = var3.conn;
      if (Math.abs(var1.cx * 16 + 8 - this.camX) < 110.0 && Math.abs(var1.cz * 16 + 8 - this.camZ) < 110.0 && (var3.nO > 0 || var4.nO > 0)) {
         this.shadowDirty = true;
      }

      var4.nO = var3.nO;
      var4.nT = var3.nT;
      var1.markMeshed(var2);

      if (var3.nO > 0) {
         if (var4.vboO == 0) {
            var4.vboO = GL15.glGenBuffers();
         }

         GL15.glBindBuffer(34962, var4.vboO);
         GL15.glBufferData(34962, var3.opaque, 35044);
      } else if (var4.vboO != 0) {
         GL15.glDeleteBuffers(var4.vboO);
         var4.vboO = 0;
      }

      if (var3.nT > 0) {
         if (var4.vboT == 0) {
            var4.vboT = GL15.glGenBuffers();
         }

         GL15.glBindBuffer(34962, var4.vboT);
         GL15.glBufferData(34962, var3.trans, 35044);
      } else if (var4.vboT != 0) {
         GL15.glDeleteBuffers(var4.vboT);
         var4.vboT = 0;
      }

      GL15.glBindBuffer(34962, 0);
      free(var3);
   }

   public void meshNow(World var1, int var2, int var3, int var4) {
      int lx = var2 & 15;
      int ly = var3 & 15;
      int lz = var4 & 15;
      int minDx = lx == 0 ? -1 : 0;
      int maxDx = lx == 15 ? 1 : 0;
      int minDy = ly == 0 ? -1 : 0;
      int maxDy = ly == 15 ? 1 : 0;
      int minDz = lz == 0 ? -1 : 0;
      int maxDz = lz == 15 ? 1 : 0;
      for (int var5 = minDx; var5 <= maxDx; var5++) {
         for (int var6 = minDz; var6 <= maxDz; var6++) {
            for (int var7 = minDy; var7 <= maxDy; var7++) {
               if (Math.abs(var5) + Math.abs(var6) + Math.abs(var7) > 1) {
                  continue;
               }
               int var8 = var2 + var5 * 16;
               int var9 = var3 + var7 * 16;
               int var10 = var4 + var6 * 16;
               if (var9 >= 0 && var9 <= 255) {
                  Chunk var11 = var1.chunkAt(var8, var10);
                  if (var11 != null) {
                     int var12 = var9 >> 4;
                     Section var13 = var11.sections[var12];
                     if (var13.dirty && !var13.inFlight && this.ready(var1, var11)) {
                        var13.dirty = false;
                        if (var11.isSectionEmpty(var12)) {
                           var13.conn = CaveCull.ALL;
                           var13.nO = 0;
                           var13.nT = 0;
                           var11.markMeshed(var12);
                           if (var13.vboO != 0) {
                              GL15.glDeleteBuffers(var13.vboO);
                              var13.vboO = 0;
                           }
                           if (var13.vboT != 0) {
                              GL15.glDeleteBuffers(var13.vboT);
                              var13.vboT = 0;
                           }
                        } else {
                           this.upload(var11, var12, Mesher.build(var1, var11, var12, this.g.opt.smooth));
                        }
                     }
                  }
               }
            }
         }
      }
   }

   static float lerp(float var0, float var1, float var2) {
      return var0 + (var1 - var0) * var2;
   }

   static float clamp01(float var0) {
      return var0 < 0.0F ? 0.0F : (var0 > 1.0F ? 1.0F : var0);
   }

   public void computeSky(long var1, float var3) {
      World cw = this.g.world;
      if (cw != null && cw.dim != Dim.OVERWORLD) {
         // Update 3.12: no sun, no day and night, no weather: a fixed fog colour, a fixed light
         float[] fc = Dim.fog(cw.dim);
         this.fogR = this.topR = fc[0];
         this.fogG = this.topG = fc[1];
         this.fogB = this.topB = fc[2];
         this.daylight = cw.dim == Dim.NETHER ? 0.4F : 0.3F;
         this.skyLightFactor = 1.0F;
         this.sunAngle = (float)(Math.PI * 1.5);
         this.wfx.updateLocal(var3);
         return;
      }

      float var4 = (float)(var1 % 24000L) + var3;
      this.sunAngle = (float)((Math.PI * 2) * (var4 - 6000.0F) / 24000.0);
      // Update 3.16: the colours of a clear sky come from Sky.base (the same numbers as before by day and by night, a longer and softer dusk)
      Sky.base(this.sunAngle, this.skyTmp);
      this.daylight = this.skyTmp[6];
      this.skyLightFactor = 0.2F + 0.8F * this.daylight;
      this.topR = this.skyTmp[0];
      this.topG = this.skyTmp[1];
      this.topB = this.skyTmp[2];
      this.fogR = this.skyTmp[3];
      this.fogG = this.skyTmp[4];
      this.fogB = this.skyTmp[5];
      if (ModLoader.isInfdevActive()) {
         float d = Math.max(0.18F, this.daylight);
         this.topR = 0.56F * d;
         this.topG = 0.78F * d;
         this.topB = 1.00F * d;
         this.fogR = 0.76F * d;
         this.fogG = 0.87F * d;
         this.fogB = 1.00F * d;
      }
      if (BackroomsStalkerMod.isActive()) {
         float[] bfc = BackroomsStalkerMod.getFogColor((float)Perf.seconds(1.0E9));
         this.topR = this.fogR = bfc[0];
         this.topG = this.fogG = bfc[1];
         this.topB = this.fogB = bfc[2];
         this.daylight = BackroomsStalkerMod.isBlackout() ? 0.12F : 0.35F;
         this.skyLightFactor = this.daylight;
         return;
      }

      // Update 3.8 - weather: overcast skies are grey and dark, storms darker still, lightning lights everything up
      this.wfx.updateLocal(var3);
      float ov = this.wfx.overcast;
      float st = this.wfx.storm;
      if (ov > 0.001F) {
         float k = Math.min(0.92F, ov * 0.92F);
         float dk = 1.0F - 0.34F * st;
         float lt = 0.3F * this.topR + 0.59F * this.topG + 0.11F * this.topB;
         float lf = 0.3F * this.fogR + 0.59F * this.fogG + 0.11F * this.fogB;
         this.topR = lerp(this.topR, lt * 0.66F * dk, k);
         this.topG = lerp(this.topG, lt * 0.71F * dk, k);
         this.topB = lerp(this.topB, lt * 0.80F * dk, k);
         this.fogR = lerp(this.fogR, lf * 0.80F * dk, k);
         this.fogG = lerp(this.fogG, lf * 0.84F * dk, k);
         this.fogB = lerp(this.fogB, lf * 0.90F * dk, k);
         this.skyLightFactor *= 1.0F - 0.32F * ov - 0.14F * st;
      }

      // wet ground is darker; going through the (shared) lightmap keeps real terrain and LOD identical in both render paths
      if (this.g.world != null) {
         this.skyLightFactor *= 1.0F - 0.10F * this.g.world.weather.wet;
      }

      float fl = this.wfx.flash;
      if (fl > 0.001F) {
         this.topR = lerp(this.topR, 0.62F, fl * 0.55F);
         this.topG = lerp(this.topG, 0.70F, fl * 0.55F);
         this.topB = lerp(this.topB, 0.92F, fl * 0.55F);
         this.fogR = lerp(this.fogR, 0.78F, fl * 0.5F);
         this.fogG = lerp(this.fogG, 0.84F, fl * 0.5F);
         this.fogB = lerp(this.fogB, 0.98F, fl * 0.5F);
         this.skyLightFactor = lerp(this.skyLightFactor, 1.0F, fl * 0.7F);
      }
   }

   static final float AMBIENT_HOOK = Float.parseFloat(System.getProperty("bc.ambient", "0"));

   void updateLightmap() {
      float var1 = this.g.opt.gamma;
      float ambient = this.g.world == null ? 0.0F : Dim.ambient(this.g.world.dim);
      if (AMBIENT_HOOK > 0.0F) {
         ambient = Math.max(ambient, AMBIENT_HOOK); // test hook -Dbc.ambient=0.8: caves and the night can be photographed
      }

      if (Math.abs(this.skyLightFactor - this.lastLm) > 0.004F || var1 != this.lastGamma || ambient != this.lastAmbient || BackroomsStalkerMod.isActive()) {
         float[] tint = Dim.ambientTint(this.g.world == null ? Dim.OVERWORLD : this.g.world.dim);
         if (BackroomsStalkerMod.isActive()) {
            float panic = BackroomsStalkerMod.getPanicLevel();
            Tex.ambG = 0.90F - panic * 0.45F;
            Tex.ambB = 0.54F - panic * 0.35F;
         } else {
            Tex.ambG = tint[0];
            Tex.ambB = tint[1];
         }
         GL13.glActiveTexture(33985);
         Tex.updateLightmap(this.skyLightFactor, ambient, var1);
         GL13.glActiveTexture(33984);
         this.lastLm = this.skyLightFactor;
         this.lastGamma = var1;
         this.lastAmbient = ambient;
      }
   }

   /** Update 3.11: sky / block light (0..1) of the last place {@link #brightnessAt} was asked about: the entity shader's light input. */
   float lastSky = 1.0F;
   float lastBlk;
   /** What the entity program needs again for the hand, which is drawn after the world (see renderHand). */
   float entFogNear;
   float entFogFar;
   float[] entFogCol = new float[]{0.5F, 0.6F, 0.8F};
   boolean entShadow;

   public float brightnessAt(World var1, double var2, double var4, double var6) {
      int var8 = (int)Math.floor(var2);
      int var9 = (int)Math.floor(var4);
      int var10 = (int)Math.floor(var6);
      int var11 = var1.getSky(var8, var9, var10);
      int var12 = var1.getBlk(var8, var9, var10);
      this.lastSky = (var11 + 0.5F) / 16.0F;
      this.lastBlk = (var12 + 0.5F) / 16.0F;
      if (this.sh != null && this.sh.entityOn) {
         this.sh.setEntLight(this.lastSky, this.lastBlk);
      }

      float var13 = Tex.lightCurve(var11) * this.skyLightFactor;
      float var14 = Tex.lightCurve(var12);
      return Math.max(0.08F + Dim.ambient(var1.dim), Math.min(1.0F, Math.max(var13, var14) + 0.05F));
   }

   void extractFrustum() {
      float[] var1 = new float[16];

      for (int var2 = 0; var2 < 4; var2++) {
         for (int var3 = 0; var3 < 4; var3++) {
            float var4 = 0.0F;

            for (int var5 = 0; var5 < 4; var5++) {
               var4 += this.proj[var5 * 4 + var3] * this.mv[var2 * 4 + var5];
            }

            var1[var2 * 4 + var3] = var4;
         }
      }

      float[][] var9 = new float[4][4];

      for (int var10 = 0; var10 < 4; var10++) {
         var9[var10][0] = var1[var10];
         var9[var10][1] = var1[4 + var10];
         var9[var10][2] = var1[8 + var10];
         var9[var10][3] = var1[12 + var10];
      }

      for (int var11 = 0; var11 < 4; var11++) {
         this.planes[0][var11] = var9[3][var11] + var9[0][var11];
         this.planes[1][var11] = var9[3][var11] - var9[0][var11];
         this.planes[2][var11] = var9[3][var11] + var9[1][var11];
         this.planes[3][var11] = var9[3][var11] - var9[1][var11];
         this.planes[4][var11] = var9[3][var11] + var9[2][var11];
         this.planes[5][var11] = var9[3][var11] - var9[2][var11];
      }

      for (float[] var6 : this.planes) {
         float var7 = (float)Math.sqrt(var6[0] * var6[0] + var6[1] * var6[1] + var6[2] * var6[2]);

         for (int var8 = 0; var8 < 4; var8++) {
            var6[var8] /= var7;
         }
      }
   }

   boolean visible(float var1, float var2, float var3, float var4) {
      for (float[] var8 : this.planes) {
         if (var8[0] * var1 + var8[1] * var2 + var8[2] * var3 + var8[3] < -var4) {
            return false;
         }
      }

      return true;
   }

   /** Update 3.11: is the camera inside the ground (more than 3 blocks under the top of the terrain)? Only used for spectators, who can fly through it. */
   static boolean underGround(World w, double x, double y, double z) {
      int bx = (int)Math.floor(x);
      int bz = (int)Math.floor(z);
      Chunk c = w.chunkAt(bx, bz);
      return c != null && y < c.hm[(bz & 15) * 16 + (bx & 15)] - 3;
   }

   static final boolean NOCULL = Boolean.getBoolean("bc.nocull");

   /** Update 3.11: back faces are not drawn (the meshes of Update 3.5 are drawn double-sided, as before). */
   static boolean cullFaces() {
      return !NOCULL && !Mesher.legacy;
   }

   static void lightmapUnit(boolean var0) {
      GL13.glActiveTexture(33985);
      if (var0) {
         GL11.glEnable(3553);
      } else {
         GL11.glDisable(3553);
      }

      GL13.glActiveTexture(33984);
   }

   static void arrays(boolean var0) {
      if (var0) {
         GL11.glEnableClientState(32884);
         GL11.glEnableClientState(32886);
         GL13.glClientActiveTexture(33984);
         GL11.glEnableClientState(32888);
         GL13.glClientActiveTexture(33985);
         GL11.glEnableClientState(32888);
      } else {
         GL11.glDisableClientState(32884);
         GL11.glDisableClientState(32886);
         GL13.glClientActiveTexture(33984);
         GL11.glDisableClientState(32888);
         GL13.glClientActiveTexture(33985);
         GL11.glDisableClientState(32888);
         GL13.glClientActiveTexture(33984);
         GL15.glBindBuffer(34962, 0);
      }
   }

   static void bindPointers(int var0) {
      GL15.glBindBuffer(34962, var0);
      GL11.glVertexPointer(3, 5126, 44, 0L);
      GL13.glClientActiveTexture(33984);
      GL11.glTexCoordPointer(2, 5126, 44, 12L);
      GL11.glColorPointer(4, 5126, 44, 20L);
      GL13.glClientActiveTexture(33985);
      GL11.glTexCoordPointer(2, 5126, 44, 36L);
      GL13.glClientActiveTexture(33984);
   }

   public void renderWorld(float var1, int var2, int var3) {
      Player var4 = this.g.player;
      World var5 = this.g.world;
      this.camX = var4.ix(var1);
      this.camY = var4.iy(var1) + var4.ieye(var1);
      this.camZ = var4.iz(var1);
      this.viewYaw = var4.yaw;
      this.viewPitch = var4.pitch;
      if (this.g.view != 0 && !var4.spectator) {
         double[] cv = thirdPerson(var5, this.camX, this.camY, this.camZ, var4.yaw, var4.pitch, this.g.view);
         this.camX = cv[0];
         this.camY = cv[1];
         this.camZ = cv[2];
         this.viewYaw = (float)cv[3];
         this.viewPitch = (float)cv[4];
      }

      float shk = this.g.pshake + (this.g.shake - this.g.pshake) * var1;
      if (shk > 0.0F) {
         // Update 3.10: earthquakes, trees hitting the ground and meteors shake the camera (position and a little rotation)
         double ph = (this.g.tickCount + var1) * 0.9;
         this.camX += (Math.sin(ph * 1.7) + Math.sin(ph * 3.1 + 0.7)) * 0.5 * shk * 0.12;
         this.camY += Math.sin(ph * 2.3 + 1.0) * shk * 0.14;
         this.camZ += (Math.sin(ph * 1.3 + 2.0) + Math.sin(ph * 2.9 + 1.9)) * 0.5 * shk * 0.12;
         this.viewYaw += (float)(Math.sin(ph * 2.1 + 0.5) * shk * 1.6);
         this.viewPitch += (float)(Math.sin(ph * 1.9 + 2.5) * shk * 1.2);
      }

      boolean var6 = var4.eyeInWater;
      boolean var7 = Blocks.isLava(var5.getBlock((int)Math.floor(this.camX), (int)Math.floor(this.camY), (int)Math.floor(this.camZ)));
      int var8 = (int)Math.floor(this.camX);
      int var9 = (int)Math.floor(this.camY);
      int var10 = (int)Math.floor(this.camZ);
      float var11 = Math.max(0.0F, var5.getSky(var8, var9, var10) / 15.0F);
      if (var4.spectator) {
         var11 = 1.0F; // Update 3.11: the ground is see-through for a spectator: the sky shows through it, no cave darkness
      }

      // Update 3.12: the Nether and the End have no sky: nothing of the sky is drawn, and being "under ground" does not darken the fog
      final boolean skyOn = var5.dim == Dim.OVERWORLD && !BackroomsStalkerMod.isActive();
      if (!skyOn) {
         var11 = 1.0F;
      }

      this.caveF = BackroomsStalkerMod.isActive() ? 1.0F : (this.caveF + (var11 - this.caveF) * 0.04F);
      float var12 = 0.04F + 0.96F * this.caveF;
      this.fogR *= var12;
      this.fogG *= var12;
      this.fogB *= var12;
      this.topR *= var12;
      this.topG *= var12;
      this.topB *= var12;
      float var41 = this.fogR;
      float var42 = this.fogG;
      float var43 = this.fogB;
      boolean isVoidRender = var5.settings != null && var5.settings.isVoidMode();
      int var13 = isVoidRender ? Math.max(10, this.g.opt.renderDistance) : this.g.opt.renderDistance;
      float var14 = var13 * 16.0F;
      int var15 = this.g.opt.lodDistance;
      boolean var16 = this.lod != null && var15 > var13;
      if (var16) {
         var15 = Math.max(var15, var13 * 3);
         var14 = var15 * 16.0F;
      }

      var12 = var14;
      var11 = var14 * (var16 ? 0.4F : 0.55F);
      if (var5.dim == Dim.NETHER) {
         var12 = Math.min(var12, 104.0F); // the thick red haze of the Nether
         var11 = var12 * 0.1F;
      }
      if (BackroomsStalkerMod.isActive()) {
         var11 = BackroomsStalkerMod.getFogNear();
         var12 = BackroomsStalkerMod.getFogFar();
      }
      // Update 3.8: rain and storms pull the haze in close
      float wxFog = Math.min(1.0F, this.wfx.rain * 0.9F + this.wfx.storm * 0.1F);
      var11 *= 1.0F - 0.55F * wxFog;
      var12 *= 1.0F - 0.30F * wxFog;
      if (var6) {
         float var17 = 0.35F + 0.65F * this.daylight;
         var41 = 0.05F * var17;
         var42 = 0.16F * var17;
         var43 = 0.38F * var17;
         var11 = 0.0F;
         var12 = 18.0F;
      }

      if (var7) {
         var41 = 0.6F;
         var42 = 0.1F;
         var43 = 0.0F;
         var11 = 0.0F;
         var12 = 2.5F;
      }

      this.updateLightmap();
      Perf.mark(Perf.SKY);
      GL11.glViewport(0, 0, var2, var3);
      // Update 3.19: the scene's alpha channel is the glow mask of the light sources (0 everywhere, the terrain pass writes it); nothing else may touch it
      boolean glow = this.shadersOn() && this.sh.sceneActive && this.sh.fxOk && this.g.opt.fx >= 1;
      if (this.sh != null) {
         this.sh.glowOn = glow;
         this.sh.fxLevel = this.g.opt.fx;
      }

      GL11.glColorMask(true, true, true, true);
      GL11.glClearColor(var41, var42, var43, glow ? 0.0F : 1.0F);
      GL11.glClear(16640);
      if (glow) {
         GL11.glColorMask(true, true, true, false);
      }

      GL11.glDisable(2884);
      GL11.glDepthFunc(515);
      float var46 = this.g.opt.fov * var4.ifov(var1);
      var46 /= this.g.zoomDiv();
      if (var4.eyeInWater) {
         var46 *= 0.85714287F;
      }

      double var18 = 0.05;
      double var20 = Math.max(this.g.opt.clouds || this.g.opt.weather ? 4200.0 : 400.0, var14 * 1.15 + 64.0);
      double var22 = var18 * Math.tan(Math.toRadians(var46) / 2.0);
      double var24 = var22 * var2 / var3;
      GL11.glMatrixMode(5889);
      GL11.glLoadIdentity();
      GL11.glFrustum(-var24, var24, -var22, var22, var18, var20);
      GL11.glMatrixMode(5888);
      GL11.glLoadIdentity();
      GL11.glRotatef(this.viewPitch, 1.0F, 0.0F, 0.0F);
      GL11.glRotatef(this.viewYaw + 180.0F, 0.0F, 1.0F, 0.0F);
      GL11.glGetFloatv(2983, this.proj);
      GL11.glGetFloatv(2982, this.mv);
      this.extractFrustum();
      GL11.glDisable(2912);
      if (!var6 && !var7 && skyOn) {
         this.drawSky(var41, var42, var43);
      } else if (!var6 && !var7 && var5.dim == Dim.END) {
         this.drawEndSky();
      }

      GL11.glEnable(2929);
      GL11.glDepthMask(true);
      GL11.glEnable(2912);
      GL11.glFogi(2917, 9729);
      GL11.glFogf(2915, var11);
      GL11.glFogf(2916, var12);
      GL11.glFogfv(2918, new float[]{var41, var42, var43, 1.0F});
      float[] var26 = new float[]{var41, var42, var43};
      float[] var27 = new float[]{this.topR, this.topG, this.topB};
      float[] var28 = new float[]{this.fogR, this.fogG, this.fogB};
      this.sh.sunAtten = this.wfx.sunVis;
      Perf.mark(Perf.CULL);
      this.vis.clear();
      this.visN = 0;
      int var48 = (int)Math.floor(this.camX) >> 4;
      int var49 = (int)Math.floor(this.camZ) >> 4;
      // Update 3.18: sections that the ground hides are not drawn (CaveCull): the walk from the camera's section through the open faces finds
      // the ones that can be seen. Not for a spectator (who flies through the rock) and not when the camera is inside a solid block.
      boolean walked = !Perf.noCull && CAVE_CULL && !var4.spectator
         && !Blocks.opaque[var5.getBlock((int)Math.floor(this.camX), (int)Math.floor(this.camY), (int)Math.floor(this.camZ))];
      if (walked) {
         int camSy = Math.max(0, Math.min(15, (int)Math.floor(this.camY) >> 4));
         this.walk.run(var5, var48, camSy, var49, var13, this.camX, this.camY, this.camZ, this.frustumTest);
         for (int i = 0; i < this.walk.count; i++) {
            int idx = this.walk.order[i];
            Chunk var32 = this.walk.chunk(idx);
            if (var32 != null) {
               this.addVisible(var32, this.walk.sy(idx));
            }
         }
      } else {
         for (Chunk var32 : var5.chunks.values()) {
            int var33 = var32.cx - var48;
            int var34 = var32.cz - var49;
            if (var33 * var33 + var34 * var34 <= (var13 + 1) * (var13 + 1)) {
               for (int var35 = 0; var35 < 16; var35++) {
                  this.addVisible(var32, var35);
               }
            }
         }
      }

      this.vis.sort(VIS_NEAR_FIRST);
      this.visibleSections = this.vis.size();
      Perf.sections = this.visibleSections;
      Perf.mark(Perf.SHADOW);
      boolean var50 = this.shadersOn();
      boolean var51 = false;
      if (var50) {
         float var52 = this.sunAngle;
         float var55 = (float)(-Math.sin(var52));
         float var58 = (float)Math.cos(var52);
         this.sh.setSun(var55, var58, 0.0F);
         var51 = skyOn && var58 > 0.08F && !var7 && this.caveF > 0.08F && this.wfx.sunVis > 0.12F && !Perf.noShadow;
         if (var51) {
            // Update 3.18: the map is drawn again when the player has moved 12 m or the sun has turned a third of a degree (as before), when terrain near
            // the player was built or changed (at most every 8 frames) and at the latest after 150 frames; it was every 30 frames whatever had happened
            this.shFrames++;
            boolean shStale = this.sh.shadowStale(var55, var58, 0.0F, this.camX, this.camY, this.camZ);
            if (shStale || Perf.shadowEvery || this.shadowDirty && this.shFrames >= 8 || this.shFrames >= 150) {
               this.shFrames = 0;
               this.shadowDirty = false;
               this.sh.computeShadow(var55, var58, 0.0F, this.camX, this.camY, this.camZ);
               this.renderShadowPass(var2, var3);
            }
         } else {
            this.sh.mapValid = false;
         }
      }

      if (var50 && glow) {
         // Update 3.19: the shadows of the clouds (a small map, drawn before the terrain that reads it) and the wetness of the world in rain
         this.sh.wet = this.g.opt.weather ? Math.min(1.0F, this.wfx.rain * 1.15F) * (1.0F - Math.min(1.0F, this.wfx.snowy)) : 0.0F;
         this.sh.cloudShadows(this.camX, this.camY, this.camZ, this.wfx, skyOn && this.g.opt.clouds, var2, var3);
      } else if (var50) {
         this.sh.wet = 0.0F;
         this.sh.cloudShadows(this.camX, this.camY, this.camZ, this.wfx, false, var2, var3);
      }

      Perf.mark(Perf.TERRAIN);
      this.drawCalls = 0;
      GL11.glEnable(2929);
      GL11.glDepthMask(true);
      GL11.glDisable(3042);
      GL11.glEnable(3008);
      GL11.glAlphaFunc(516, 0.5F);
      GL11.glEnable(3553);
      GL11.glBindTexture(3553, Tex.atlas);
      GL11.glTexEnvi(8960, 8704, 8448);
      lightmapUnit(true);
      GL13.glActiveTexture(33985);
      GL11.glBindTexture(3553, Tex.lightmap);
      GL11.glTexEnvi(8960, 8704, 8448);
      GL13.glActiveTexture(33984);
      arrays(true);
      if (var50) {
         this.fillDynLights();
      }

      if (var50) {
         this.sh.useWorld(this.mv, this.camX, this.camY, this.camZ, false, var11, var12, var26, var27, var28, this.daylight, var51, 9, 25, 26, 27, 23);
      }

      // Update 3.11: faces are single-sided like in Minecraft (go under the map in spectator mode: the ground is see-through from below)
      boolean cull = cullFaces();
      if (cull) {
         GL11.glEnable(2884);
      }

      if (glow) {
         // Update 3.19: the terrain writes the glow mask into the alpha channel; the shader throws the see-through texels away itself, so the fixed alpha test (which would eat the mask) is off
         GL11.glColorMask(true, true, true, true);
         GL11.glDisable(3008);
      }

      for (Renderer.Vis var56 : this.vis) {
         Section var59 = var56.c.sections[var56.sy];
         if (var59.nO != 0 && var59.vboO != 0) {
            GL11.glPushMatrix();
            GL11.glTranslatef((float)(var56.c.cx * 16 - this.camX), (float)(var56.sy * 16 - this.camY), (float)(var56.c.cz * 16 - this.camZ));
            bindPointers(var59.vboO);
            GL11.glDrawArrays(7, 0, var59.nO);
            this.drawCalls++;
            Perf.verts += var59.nO;
            GL11.glPopMatrix();
         }
      }

      if (cull) {
         GL11.glDisable(2884);
      }

      if (glow) {
         GL11.glColorMask(true, true, true, false);
         GL11.glEnable(3008);
      }

      arrays(false);
      if (var50) {
         this.sh.stopWorld();
      }

      lightmapUnit(false);
      Perf.mark(Perf.LOD);
      if (var16 && !var6 && !var7 && !(var4.spectator && underGround(var5, this.camX, this.camY, this.camZ))) {
         GL11.glEnable(2929);
         GL11.glDepthMask(true);
         boolean var29 = this.sh.lodOk;
         if (var29) {
            boolean var30 = this.shadersOn();
            if (var30) {
               this.sh.setSun((float)(-Math.sin(this.sunAngle)), (float)Math.cos(this.sunAngle), 0.0F);
            } else {
               this.sh.setSun(0.0F, 1.0F, 0.0F);
            }

            this.sh.useLod(this.mv, this.camX, this.camY, this.camZ, var11, var12, var26, var27, var28, var30);
         }

         this.lod.render(this.camX, this.camY, this.camZ, var15, var13, this.skyLightFactor);
         if (var29) {
            this.sh.stopLod();
         }

         GL11.glEnable(3008);
         GL11.glAlphaFunc(516, 0.5F);
         GL11.glBindTexture(3553, Tex.atlas);
      }

      this.sh.sunAtten = this.wfx.sunVis;
      Perf.mark(Perf.ENTITIES);
      // Update 3.11: with shaders on, animals, monsters, items, falling blocks and your own body are lit like the terrain (sun, shade, shadows, torches, fog)
      this.entFogNear = var11;
      this.entFogFar = var12;
      this.entFogCol = var26;
      this.entShadow = var51;
      boolean entShader = var50 && this.sh.entityOk;
      if (entShader) {
         this.sh.useEntity(this.mv, this.camX, this.camY, this.camZ, var11, var12, var26, var51, this.lastSky, this.lastBlk);
      }

      this.drawEntities(var1);
      this.drawSelf(var1);
      if (entShader) {
         this.sh.stopEntity();
      }

      this.drawParticles(var1);
      if (this.g.target != null && this.g.screenIsPlay()) {
         this.drawCrack();
         this.drawSelection();
      }

      if (skyOn && this.g.opt.clouds && !var6 && !var7 && (!var50 || !this.sh.cloudsOk || !this.sh.sceneActive)) {
         if (ModLoader.isInfdevActive()) {
            this.drawClouds(var1, var41, var42, var43);
         } else {
            this.wfx.drawClouds(var1, var41, var42, var43, var11, var12);
         }
         if (glow) {
            GL11.glColorMask(true, true, true, false);   // the plain clouds switch the colour mask back on
         }
      }

      Perf.mark(Perf.WATER);
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      GL11.glDisable(3008);
      GL11.glEnable(3553); // Update 3.12: the selection box switches texturing off; without clouds (the Nether) nothing switched it back on: the portal film was a flat colour
      GL11.glBindTexture(3553, Tex.atlas);
      lightmapUnit(true);
      arrays(true);
      // Update 3.11: water with shaders reads a copy of what is behind it (colour and depth): depth-based colour, refraction, reflections, foam
      boolean waterShader = false;
      if (var50 && this.sh.waterOk && this.sh.sceneActive && !var6 && !var7) {
         for (Renderer.Vis wv : this.vis) {
            if (wv.c.sections[wv.sy].nT != 0) {
               waterShader = true;
               break;
            }
         }
      }

      if (waterShader) {
         this.sh.copyScene(var2, var3);
         lightmapUnit(true);
         this.sh.useWater(this.mv, this.camX, this.camY, this.camZ, var11, var12, var26, var27, var28, this.daylight, var51, 9, 25, 26, 27, 23, var2, var3, (float)var18, (float)var20, this.g.opt.waterQ);
      } else if (var50) {
         this.sh.useWorld(this.mv, this.camX, this.camY, this.camZ, true, var11, var12, var26, var27, var28, this.daylight, var51, 9, 25, 26, 27, 23);
      }

      if (cull) {
         GL11.glEnable(2884);
      }

      for (int var54 = this.vis.size() - 1; var54 >= 0; var54--) {
         Renderer.Vis var57 = (Renderer.Vis)this.vis.get(var54);
         Section var60 = var57.c.sections[var57.sy];
         if (var60.nT != 0 && var60.vboT != 0) {
            GL11.glPushMatrix();
            GL11.glTranslatef((float)(var57.c.cx * 16 - this.camX), (float)(var57.sy * 16 - this.camY), (float)(var57.c.cz * 16 - this.camZ));
            bindPointers(var60.vboT);
            GL11.glDrawArrays(7, 0, var60.nT);
            this.drawCalls++;
            Perf.verts += var60.nT;
            GL11.glPopMatrix();
         }
      }

      if (cull) {
         GL11.glDisable(2884);
      }

      arrays(false);
      if (waterShader) {
         this.sh.stopWater();
      } else if (var50) {
         this.sh.stopWorld();
      }

      lightmapUnit(false);
      GL11.glDisable(3042);
      GL11.glDisable(2912);
      GL11.glDepthMask(true);
      Perf.mark(Perf.CLOUDS);
      if (skyOn && var50 && this.g.opt.clouds && !var6 && !var7 && this.sh.cloudsOk && this.sh.sceneActive) {
         this.sh.renderClouds(this.proj, this.mv, this.camX, this.camY, this.camZ, var27, var28, var26, var2, var3, this.wfx);
      }

      Perf.mark(Perf.FX);
      if (skyOn && !var6 && !var7) {
         // Update 3.10: tornado funnels and wall clouds, meteors
         DisasterFx.drawSky(this, var1);
         // rain curtains in the distance, rain / snow around you, lightning
         this.wfx.drawOverlays(var1, var41, var42, var43);
      }
   }

   void scanPlacedLights() {
      this.placedLights.clear();
      this.lsX = this.camX;
      this.lsY = this.camY;
      this.lsZ = this.camZ;
      int var3 = Math.max(0, (int)this.camY - 14);
      int var4 = Math.min(255, (int)this.camY + 14);
      ArrayList<float[]> var5 = new ArrayList<>();
      int var6 = (int)Math.floor(this.camX) >> 4;
      int var7 = (int)Math.floor(this.camZ) >> 4;
      int syLo = var3 >> 4;
      int syHi = var4 >> 4;

      for (int var8 = -2; var8 <= 2; var8++) {
         for (int var9 = -2; var9 <= 2; var9++) {
            Chunk var10 = this.g.world.chunk(var6 + var9, var7 + var8);
            if (var10 != null) {
               for (int sy = syLo; sy <= syHi; sy++) {
                  if (var10.isSectionEmpty(sy)) {
                     continue;
                  }
                  int yStart = Math.max(var3, sy << 4);
                  int yEnd = Math.min(var4, (sy << 4) + 15);
                  for (int var11 = yStart; var11 <= yEnd; var11++) {
                     for (int var12 = 0; var12 < 16; var12++) {
                        for (int var13 = 0; var13 < 16; var13++) {
                           int var14 = var10.blocks[Chunk.idx(var13, var11, var12)] & 0xFFFF;
                           if (var14 != 0 && Blocks.emit[var14] >= 7) {
                              if (Blocks.isLava(var14) && ((var13 + var12) & 3) != 0) {
                                 continue;
                              }
                              float var15 = var10.cx * 16 + var13 + 0.5F;
                              float var16 = var10.cz * 16 + var12 + 0.5F;
                              float var17 = (float)(var15 - this.camX);
                              float var18 = (float)(var16 - this.camZ);
                              float var19 = (float)(var11 + 0.5 - this.camY);
                              float var20 = var17 * var17 + var19 * var19 + var18 * var18;
                              if (!(var20 > 576.0F)) {
                                 var5.add(new float[]{var15, var11 + 0.5F, var16, var14, var20});
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      var5.sort((var0, var1) -> Float.compare(var0[4], var1[4]));

      for (float[] var22 : var5) {
         if (this.placedLights.size() >= 9) {
            break;
         }

         boolean var23 = false;
         Iterator var24 = this.placedLights.iterator();

         while (true) {
            if (var24.hasNext()) {
               float[] var25 = (float[])var24.next();
               float var26 = var25[0] - var22[0];
               float var27 = var25[1] - var22[1];
               float var28 = var25[2] - var22[2];
               if (!(var26 * var26 + var27 * var27 + var28 * var28 < 10.240001F)) {
                  continue;
               }

               var23 = true;
            }

            if (!var23) {
               this.placedLights.add(var22);
            }
            break;
         }
      }
   }

   static float[] lightColor(int var0) {
      if (var0 == 25 || var0 == Redstone319.REDSTONE_LAMP_LIT) {
         return new float[]{1.0F, 0.84F, 0.52F};
      } else if (var0 == Redstone319.SEA_LANTERN) {
         return new float[]{0.68F, 0.96F, 1.0F};
      } else if (var0 == Redstone319.REDSTONE_TORCH) {
         return new float[]{1.0F, 0.22F, 0.16F};
      } else if (Blocks.isLava(var0) || var0 == Blocks.MAGMA) {
         return new float[]{1.0F, 0.42F, 0.1F};
      } else if (Blocks.isPortal(var0)) {
         return new float[]{0.62F, 0.3F, 1.0F};
      } else {
         return var0 == 68 ? new float[]{1.0F, 0.55F, 0.22F} : new float[]{1.0F, 0.66F, 0.3F};
      }
   }

   void fillDynLights() {
      Arrays.fill(this.sh.dynL, 0.0F);
      Arrays.fill(this.sh.dynC, 0.0F);
      int var1 = 0;
      float var2 = (float)Perf.seconds(1.0E9);
      float var3 = 1.0F + 0.05F * (float)Math.sin(var2 * 11.0F) + 0.04F * (float)Math.sin(var2 * 23.0F + 1.0F);
      int var4 = this.g.heldId();
      int var5 = var4 > 0 && Blocks.isBlockId(var4) ? Blocks.emit[var4] : 0;
      if (var5 > 0) {
         float var6 = this.mv[0];
         float var7 = this.mv[4];
         float var8 = this.mv[8];
         float var9 = this.mv[1];
         float var10 = this.mv[5];
         float var11 = this.mv[9];
         float var12 = -this.mv[2];
         float var13 = -this.mv[6];
         float var14 = -this.mv[10];
         this.sh.dynL[0] = var12 * 0.6F + var6 * 0.35F - var9 * 0.25F;
         this.sh.dynL[1] = var13 * 0.6F + var7 * 0.35F - var10 * 0.25F;
         this.sh.dynL[2] = var14 * 0.6F + var8 * 0.35F - var11 * 0.25F;
         this.sh.dynL[3] = (4.0F + var5 * 0.6F) * var3;
         float[] var15 = lightColor(var4);
         float var16 = 0.5F + 0.5F * var5 / 15.0F;
         this.sh.dynC[0] = var15[0] * var16;
         this.sh.dynC[1] = var15[1] * var16;
         this.sh.dynC[2] = var15[2] * var16;
         this.sh.dynC[3] = 1.0F;
         var1 = 1;
      } else if (BackroomsStalkerMod.isActive()) {
         // Camcorder handheld flashlight beam projected ahead of the player
         float flk = 1.0F + 0.08F * (float)Math.sin(var2 * 47.0F) * BackroomsStalkerMod.getPanicLevel();
         if (BackroomsStalkerMod.isBlackout() && ((int)(var2 * 18.0F) & 3) == 0) {
            flk *= 0.35F;
         }
         float fdx = -this.mv[2];
         float fdy = -this.mv[6];
         float fdz = -this.mv[10];
         this.sh.dynL[0] = fdx * 1.65F;
         this.sh.dynL[1] = fdy * 1.65F - 0.1F;
         this.sh.dynL[2] = fdz * 1.65F;
         this.sh.dynL[3] = 8.5F * flk;
         this.sh.dynC[0] = 1.0F * flk;
         this.sh.dynC[1] = 0.91F * flk;
         this.sh.dynC[2] = 0.62F * flk;
         this.sh.dynC[3] = 1.0F;
         var1 = 1;
      }

      if (BackroomsStalkerMod.isActive() && var1 < 8) {
         Mobs.Mob stalker = BackroomsStalkerMod.getStalker();
         if (stalker != null) {
            double sx = stalker.x - this.camX;
            double sy = stalker.y + 2.1 - this.camY;
            double sz = stalker.z - this.camZ;
            if (sx * sx + sy * sy + sz * sz < 1200.0) {
               float pulse = 0.75F + 0.25F * (float)Math.sin(var2 * 14.0F);
               this.sh.dynL[var1 * 4] = (float)sx;
               this.sh.dynL[var1 * 4 + 1] = (float)sy;
               this.sh.dynL[var1 * 4 + 2] = (float)sz;
               this.sh.dynL[var1 * 4 + 3] = (BackroomsStalkerMod.getPhase() == BackroomsStalkerMod.PHASE_CHASE ? 11.5F : 7.5F) * pulse;
               this.sh.dynC[var1 * 4] = 1.0F * pulse;
               this.sh.dynC[var1 * 4 + 1] = 0.07F * pulse;
               this.sh.dynC[var1 * 4 + 2] = 0.05F * pulse;
               this.sh.dynC[var1 * 4 + 3] = 1.0F;
               var1++;
            }
         }
      }

      for (Item var19 : this.g.world.items) {
         if (var1 >= 3) {
            break;
         }

         if (var19.id > 0 && Blocks.isBlockId(var19.id) && Blocks.emit[var19.id] > 0) {
            double var20 = var19.x - this.camX;
            double var23 = var19.y + 0.25 - this.camY;
            double var26 = var19.z - this.camZ;
            if (!(var20 * var20 + var23 * var23 + var26 * var26 > 1600.0)) {
               float[] var29 = lightColor(var19.id);
               this.sh.dynL[var1 * 4] = (float)var20;
               this.sh.dynL[var1 * 4 + 1] = (float)var23;
               this.sh.dynL[var1 * 4 + 2] = (float)var26;
               this.sh.dynL[var1 * 4 + 3] = (3.0F + Blocks.emit[var19.id] * 0.4F) * var3;
               this.sh.dynC[var1 * 4] = var29[0] * 0.8F;
               this.sh.dynC[var1 * 4 + 1] = var29[1] * 0.8F;
               this.sh.dynC[var1 * 4 + 2] = var29[2] * 0.8F;
               this.sh.dynC[var1 * 4 + 3] = 1.0F;
               var1++;
            }
         }
      }

      double var18 = (this.camX - this.lsX) * (this.camX - this.lsX)
         + (this.camY - this.lsY) * (this.camY - this.lsY)
         + (this.camZ - this.lsZ) * (this.camZ - this.lsZ);
      if (--this.lightScanCd <= 0 || var18 > 9.0) {
         this.scanPlacedLights();
         this.lightScanCd = 12;
      }

      for (float[] var22 : this.placedLights) {
         if (var1 >= 12) {
            break;
         }

         int var24 = (int)var22[3];
         if (Blocks.isBlockId(var24)) {
            float[] var25 = lightColor(var24);
            float var27 = var22[0] * 12.9898F + var22[2] * 78.233F;
            float var28 = 1.0F + 0.06F * (float)Math.sin(var2 * 9.0F + var27) + 0.04F * (float)Math.sin(var2 * 17.0F + var27 * 2.0F);
            this.sh.dynL[var1 * 4] = (float)(var22[0] - this.camX);
            this.sh.dynL[var1 * 4 + 1] = (float)(var22[1] - this.camY);
            this.sh.dynL[var1 * 4 + 2] = (float)(var22[2] - this.camZ);
            this.sh.dynL[var1 * 4 + 3] = (3.5F + Blocks.emit[var24] * 0.55F) * var28;
            this.sh.dynC[var1 * 4] = var25[0] * 0.95F;
            this.sh.dynC[var1 * 4 + 1] = var25[1] * 0.95F;
            this.sh.dynC[var1 * 4 + 2] = var25[2] * 0.95F;
            this.sh.dynC[var1 * 4 + 3] = 0.0F;
            var1++;
         }
      }
   }

   void renderShadowPass(int var1, int var2) {
      GL11.glEnable(2929);
      GL11.glDepthMask(true);
      GL11.glDepthFunc(515);
      GL11.glDisable(2884);
      this.sh.beginShadow();
      arrays(true);
      GL13.glActiveTexture(33984);
      GL11.glEnable(3553);
      GL11.glBindTexture(3553, Tex.atlas);
      int var3 = (int)Math.floor(this.camX) >> 4;
      int var4 = (int)Math.floor(this.camZ) >> 4;
      boolean isVoidShadow = this.g.world != null && this.g.world.settings != null && this.g.world.settings.isVoidMode();
      boolean isPanoShadow = this.g.pano != null && this.g.world == this.g.pano.world;
      int var5 = Math.min(9, isPanoShadow ? Math.min(4, this.g.opt.renderDistance) : (isVoidShadow ? 7 : this.g.opt.renderDistance));
      // Update 3.18: only what can land inside the map is drawn into it (the map is a 168 m square of light space: the 19 x 19 chunks round the player were
      // 90 000 m2, with the vertices of every section, underground ones too, going through the vertex shader to be clipped away) - same picture, a third of the work
      float[] sm = this.sh.shadowM;
      float eu = 8.0F * (Math.abs(sm[0]) + Math.abs(sm[1]) + Math.abs(sm[2]));
      float ev = 8.0F * (Math.abs(sm[4]) + Math.abs(sm[5]) + Math.abs(sm[6]));
      float ed = 8.0F * (Math.abs(sm[8]) + Math.abs(sm[9]) + Math.abs(sm[10]));
      int span = 2 * var5 + 3;
      int[] low = new int[span * span]; // the lowest surface of every chunk of the 21 x 21 chunks round the player
      for (int var6 = -var5 - 1; var6 <= var5 + 1; var6++) {
         for (int var7 = -var5 - 1; var7 <= var5 + 1; var7++) {
            Chunk lc = this.g.world.chunk(var3 + var7, var4 + var6);
            int m = 0;
            if (lc != null) {
               m = 255;
               for (int i = 0; i < 256; i++) {
                  m = Math.min(m, lc.hm[i]);
               }
            }

            low[(var6 + var5 + 1) * span + var7 + var5 + 1] = m;
         }
      }

      for (int var6 = -var5; var6 <= var5; var6++) {
         for (int var7 = -var5; var7 <= var5; var7++) {
            Chunk var8 = this.g.world.chunk(var3 + var7, var4 + var6);
            if (var8 != null) {
               // sections that lie below the surface of the chunk and all eight round it can neither be seen nor throw a shadow on anything that is seen
               int buried = 255;
               for (int dz = -1; dz <= 1; dz++) {
                  for (int dx = -1; dx <= 1; dx++) {
                     buried = Math.min(buried, low[(var6 + dz + var5 + 1) * span + var7 + dx + var5 + 1]);
                  }
               }

               buried -= 4;
               for (int var9 = 0; var9 < 16; var9++) {
                  Section var10 = var8.sections[var9];
                  if (var10.nO != 0 && var10.vboO != 0 && (Perf.noShadowCull || var9 * 16 + 15 >= buried)) {
                     float rx = (float)(var8.cx * 16 + 8 - this.camX);
                     float ry = (float)(var9 * 16 + 8 - this.camY);
                     float rz = (float)(var8.cz * 16 + 8 - this.camZ);
                     float u = sm[0] * rx + sm[1] * ry + sm[2] * rz + sm[3];
                     float v = sm[4] * rx + sm[5] * ry + sm[6] * rz + sm[7];
                     float d = sm[8] * rx + sm[9] * ry + sm[10] * rz + sm[11];
                     if (!Perf.noShadowCull && (u + eu < 0.0F || u - eu > 1.0F || v + ev < 0.0F || v - ev > 1.0F || d + ed < 0.0F || d - ed > 1.0F)) {
                        continue;
                     }

                     GL11.glPushMatrix();
                     GL11.glTranslatef((float)(var8.cx * 16 - this.camX), (float)(var9 * 16 - this.camY), (float)(var8.cz * 16 - this.camZ));
                     bindPointers(var10.vboO);
                     GL11.glDrawArrays(7, 0, var10.nO);
                     Perf.shadowDraws++;
                     Perf.shadowVerts += var10.nO;
                     GL11.glPopMatrix();
                  }
               }
            }
         }
      }

      arrays(false);
      this.sh.endShadow(var1, var2, true);
   }

   /** Update 3.12: the sky of the End - no sun, no moon: stars on the deep purple, turning very slowly. */
   void drawEndSky() {
      GL11.glDisable(2929);
      GL11.glDisable(3553);
      GL11.glDisable(3008);
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      GL11.glPushMatrix();
      GL11.glRotatef((float)(this.g.tickCount * 0.004), 0.3F, 1.0F, 0.2F);
      for (int pass = 0; pass < 2; pass++) {
         GL11.glPointSize(pass == 0 ? 2.0F : 3.0F);
         GL11.glBegin(0);
         for (int i = pass; i < 700; i += pass == 0 ? 1 : 7) {
            float k = 0.55F + 0.45F * (float)Math.sin(i * 12.9898);
            GL11.glColor4f(0.75F + 0.2F * k, 0.6F + 0.2F * k, 1.0F, pass == 0 ? 0.65F : 0.95F);
            GL11.glVertex3f(this.stars[i * 3], this.stars[i * 3 + 1], this.stars[i * 3 + 2]);
         }

         GL11.glEnd();
      }

      GL11.glPopMatrix();
      GL11.glDisable(3042);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
   }

   void drawSky(float var1, float var2, float var3) {
      if (this.g.world != null) {
         // Update 3.16: the sky has its own class (Sky): the dusk, the moon's phases, the stars, the glow round the sun
         Sky.draw(this, var1, var2, var3, this.g.world.dayTime, Perf.seconds(100000.0));
         return;
      }

      GL11.glDisable(2929);
      GL11.glDisable(3553);
      GL11.glDisable(3008);
      GL11.glDisable(3042);
      float var4 = 200.0F;
      int var5 = 24;
      double[] var6 = new double[]{-30.0, 0.0, 8.0, 20.0, 40.0, 65.0, 90.0};

      for (int var7 = 0; var7 < var6.length - 1; var7++) {
         GL11.glBegin(8);

         for (int var8 = 0; var8 <= var5; var8++) {
            double var9 = var8 * Math.PI * 2.0 / var5;

            for (int var11 = 0; var11 < 2; var11++) {
               double var12 = Math.toRadians(var6[var7 + var11]);
               float var14 = var6[var7 + var11] <= 0.0 ? 0.0F : (float)Math.pow(Math.sin(var12), 0.6);
               double vx = Math.cos(var9) * Math.cos(var12);
               double vy = Math.sin(var12);
               double vz = Math.sin(var9) * Math.cos(var12);
               float cr = lerp(var1, this.topR, var14);
               float cg = lerp(var2, this.topG, var14);
               float cb = lerp(var3, this.topB, var14);
               float gl = this.wfx.gloom(vx, vy, vz);
               if (gl > 0.002F) {
                  // Update 3.8: the sky turns grey and dark in the direction of clouds and storms
                  float lum = 0.3F * cr + 0.59F * cg + 0.11F * cb;
                  float k = Math.min(0.85F, gl * 0.9F);
                  cr = lerp(cr, lum * 0.62F, k);
                  cg = lerp(cg, lum * 0.67F, k);
                  cb = lerp(cb, lum * 0.76F, k);
               }

               GL11.glColor3f(cr, cg, cb);
               GL11.glVertex3d(vx * var4, vy * var4, vz * var4);
            }
         }

         GL11.glEnd();
      }

      float var15 = 1.0F - this.daylight;
      GL11.glPushMatrix();
      GL11.glRotatef((float)Math.toDegrees(this.sunAngle), 0.0F, 0.0F, 1.0F);
      if (var15 > 0.05F) {
         GL11.glEnable(3042);
         GL11.glBlendFunc(770, 771);
         GL11.glPointSize(2.0F);
         GL11.glColor4f(1.0F, 1.0F, 1.0F, var15 * var15 * 0.9F * (1.0F - 0.9F * this.wfx.overcast));
         GL11.glBegin(0);

         for (int var16 = 0; var16 < 700; var16++) {
            GL11.glVertex3f(this.stars[var16 * 3], this.stars[var16 * 3 + 1], this.stars[var16 * 3 + 2]);
         }

         GL11.glEnd();
      }

      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 1);
      GL11.glEnable(3553);
      GL13.glActiveTexture(33984);
      GL11.glBindTexture(3553, Tex.sun);
      float sv = 0.18F + 0.82F * this.wfx.sunVis;
      GL11.glColor4f(sv, sv, sv, 1.0F);
      float var17 = 30.0F;
      float var19 = 100.0F;
      GL11.glBegin(7);
      GL11.glTexCoord2f(0.0F, 0.0F);
      GL11.glVertex3f(-var17, var19, -var17);
      GL11.glTexCoord2f(1.0F, 0.0F);
      GL11.glVertex3f(var17, var19, -var17);
      GL11.glTexCoord2f(1.0F, 1.0F);
      GL11.glVertex3f(var17, var19, var17);
      GL11.glTexCoord2f(0.0F, 1.0F);
      GL11.glVertex3f(-var17, var19, var17);
      GL11.glEnd();
      GL11.glBlendFunc(770, 771);
      GL11.glBindTexture(3553, Tex.moon);
      var17 = 20.0F;
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      GL11.glBegin(7);
      GL11.glTexCoord2f(0.0F, 0.0F);
      GL11.glVertex3f(-var17, -var19, -var17);
      GL11.glTexCoord2f(1.0F, 0.0F);
      GL11.glVertex3f(var17, -var19, -var17);
      GL11.glTexCoord2f(1.0F, 1.0F);
      GL11.glVertex3f(var17, -var19, var17);
      GL11.glTexCoord2f(0.0F, 1.0F);
      GL11.glVertex3f(-var17, -var19, var17);
      GL11.glEnd();
      GL11.glPopMatrix();
      GL11.glDisable(3042);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
   }

   private static boolean[] infdevCloudGrid;

   private static boolean isInfdevCloudCell(int gx, int gz) {
      if (infdevCloudGrid == null) {
         int[] px = Tex.cloudPixels();
         boolean[] grid = new boolean[64 * 64];
         for (int z = 0; z < 64; z++) {
            for (int x = 0; x < 64; x++) {
               grid[z * 64 + x] = px[(z * 4) * 256 + (x * 4)] != 0;
            }
         }
         infdevCloudGrid = grid;
      }
      return infdevCloudGrid[(gz & 63) * 64 + (gx & 63)];
   }

   void drawClouds(float var1, float var2, float var3, float var4) {
      boolean infdev = ModLoader.isInfdevActive();
      double baseAlt = infdev ? 108.0 : 130.0;
      double var5 = baseAlt - this.camY;
      float var7 = Math.max(250.0F, this.g.opt.renderDistance * 16.0F * 1.4F);
      double var8 = ((float)this.g.world.dayTime + var1) * 0.03;
      double var10 = 3072.0;
      double var12 = (this.camX - var7 + var8) / var10;
      double var14 = (this.camX + var7 + var8) / var10;
      double var16 = (this.camZ - var7) / var10;
      double var18 = (this.camZ + var7) / var10;
      float var20 = 0.35F + 0.65F * this.daylight;
      GL11.glEnable(3553);
      GL11.glBindTexture(3553, Tex.clouds);
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      GL11.glDisable(3008);
      if (infdev) {
         // Classic Minecraft Infdev 4-block-thick 3D blocky clouds at Y=108..112
         double topY = var5 + 4.0;
         // Bottom face (0.70 brightness)
         GL11.glColor4f(var20 * 0.72F, var20 * 0.72F, var20 * 0.76F, 0.85F);
         GL11.glBegin(7);
         GL11.glTexCoord2d(var12, var16);
         GL11.glVertex3d(-var7, var5, -var7);
         GL11.glTexCoord2d(var14, var16);
         GL11.glVertex3d(var7, var5, -var7);
         GL11.glTexCoord2d(var14, var18);
         GL11.glVertex3d(var7, var5, var7);
         GL11.glTexCoord2d(var12, var18);
         GL11.glVertex3d(-var7, var5, var7);
         GL11.glEnd();
         // Top face (1.00 brightness)
         GL11.glColor4f(var20, var20, var20, 0.85F);
         GL11.glBegin(7);
         GL11.glTexCoord2d(var12, var16);
         GL11.glVertex3d(-var7, topY, -var7);
         GL11.glTexCoord2d(var14, var16);
         GL11.glVertex3d(var7, topY, -var7);
         GL11.glTexCoord2d(var14, var18);
         GL11.glVertex3d(var7, topY, var7);
         GL11.glTexCoord2d(var12, var18);
         GL11.glVertex3d(-var7, topY, var7);
         GL11.glEnd();
         // 3D extruded side walls around cloud cells near the camera (48x48 block cells matching 3072/64)
         GL11.glDisable(3553);
         double cell = 48.0;
         double worldX = this.camX + var8;
         int cx0 = (int)Math.floor(worldX / cell);
         int cz0 = (int)Math.floor(this.camZ / cell);
         GL11.glBegin(7);
         for (int dz = -5; dz <= 5; dz++) {
            for (int dx = -5; dx <= 5; dx++) {
               int gx = cx0 + dx;
               int gz = cz0 + dz;
               if (!isInfdevCloudCell(gx, gz)) {
                  continue;
               }
               double x0 = gx * cell - worldX;
               double x1 = x0 + cell;
               double z0 = gz * cell - this.camZ;
               double z1 = z0 + cell;
               if (!isInfdevCloudCell(gx - 1, gz)) {
                  GL11.glColor4f(var20 * 0.90F, var20 * 0.90F, var20 * 0.92F, 0.85F);
                  GL11.glVertex3d(x0, var5, z0);
                  GL11.glVertex3d(x0, var5, z1);
                  GL11.glVertex3d(x0, topY, z1);
                  GL11.glVertex3d(x0, topY, z0);
               }
               if (!isInfdevCloudCell(gx + 1, gz)) {
                  GL11.glColor4f(var20 * 0.90F, var20 * 0.90F, var20 * 0.92F, 0.85F);
                  GL11.glVertex3d(x1, var5, z1);
                  GL11.glVertex3d(x1, var5, z0);
                  GL11.glVertex3d(x1, topY, z0);
                  GL11.glVertex3d(x1, topY, z1);
               }
               if (!isInfdevCloudCell(gx, gz - 1)) {
                  GL11.glColor4f(var20 * 0.80F, var20 * 0.80F, var20 * 0.84F, 0.85F);
                  GL11.glVertex3d(x1, var5, z0);
                  GL11.glVertex3d(x0, var5, z0);
                  GL11.glVertex3d(x0, topY, z0);
                  GL11.glVertex3d(x1, topY, z0);
               }
               if (!isInfdevCloudCell(gx, gz + 1)) {
                  GL11.glColor4f(var20 * 0.80F, var20 * 0.80F, var20 * 0.84F, 0.85F);
                  GL11.glVertex3d(x0, var5, z1);
                  GL11.glVertex3d(x1, var5, z1);
                  GL11.glVertex3d(x1, topY, z1);
                  GL11.glVertex3d(x0, topY, z1);
               }
            }
         }
         GL11.glEnd();
         GL11.glEnable(3553);
      } else {
         GL11.glColor4f(var20, var20, var20 * 1.0F, 0.82F);
         GL11.glBegin(7);
         GL11.glTexCoord2d(var12, var16);
         GL11.glVertex3d(-var7, var5, -var7);
         GL11.glTexCoord2d(var14, var16);
         GL11.glVertex3d(var7, var5, -var7);
         GL11.glTexCoord2d(var14, var18);
         GL11.glVertex3d(var7, var5, var7);
         GL11.glTexCoord2d(var12, var18);
         GL11.glVertex3d(-var7, var5, var7);
         GL11.glEnd();
      }
      GL11.glDisable(3042);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
   }

   void drawSelection() {
      Hit var1 = this.g.target;
      double[] var2 = this.g.world.selectionBox(this.g.world.getBlock(var1.x, var1.y, var1.z));
      double var3 = 0.002;
      double var5 = var1.x + var2[0] - var3 - this.camX;
      double var7 = var1.y + var2[1] - var3 - this.camY;
      double var9 = var1.z + var2[2] - var3 - this.camZ;
      double var11 = var1.x + var2[3] + var3 - this.camX;
      double var13 = var1.y + var2[4] + var3 - this.camY;
      double var15 = var1.z + var2[5] + var3 - this.camZ;
      GL11.glDisable(3553);
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      GL11.glColor4f(0.0F, 0.0F, 0.0F, 0.45F);
      GL11.glLineWidth(2.0F);
      GL11.glBegin(1);
      double[][] var17 = new double[][]{
         {var5, var7, var9},
         {var11, var7, var9},
         {var11, var7, var15},
         {var5, var7, var15},
         {var5, var13, var9},
         {var11, var13, var9},
         {var11, var13, var15},
         {var5, var13, var15}
      };
      int[][] var18 = new int[][]{{0, 1}, {1, 2}, {2, 3}, {3, 0}, {4, 5}, {5, 6}, {6, 7}, {7, 4}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};

      for (int[] var22 : var18) {
         GL11.glVertex3d(var17[var22[0]][0], var17[var22[0]][1], var17[var22[0]][2]);
         GL11.glVertex3d(var17[var22[1]][0], var17[var22[1]][1], var17[var22[1]][2]);
      }

      GL11.glEnd();
      GL11.glDisable(3042);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
   }

   void drawCrack() {
      int var1 = this.g.crackStage();
      if (var1 >= 0) {
         Hit var2 = this.g.target;
         double[] var3 = this.g.world.selectionBox(this.g.world.getBlock(var2.x, var2.y, var2.z));
         double var4 = 0.003;
         double var6 = var2.x + var3[0] - var4 - this.camX;
         double var8 = var2.y + var3[1] - var4 - this.camY;
         double var10 = var2.z + var3[2] - var4 - this.camZ;
         double var12 = var2.x + var3[3] + var4 - this.camX;
         double var14 = var2.y + var3[4] + var4 - this.camY;
         double var16 = var2.z + var3[5] + var4 - this.camZ;
         float var18 = var1 / 10.0F;
         float var19 = (var1 + 1) / 10.0F;
         GL11.glEnable(3553);
         GL11.glBindTexture(3553, Tex.crack);
         GL11.glEnable(3042);
         GL11.glBlendFunc(770, 771);
         GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
         GL11.glBegin(7);

         for (int var20 = 0; var20 < 6; var20++) {
            for (int var21 = 0; var21 < 4; var21++) {
               int[] var22 = Mesher.FC[var20][var21];
               double var23 = var22[0] == 0 ? var6 : var12;
               double var25 = var22[1] == 0 ? var8 : var14;
               double var27 = var22[2] == 0 ? var10 : var16;
               float[] var29 = new float[2];
               Mesher.uvFor(var20, var22[0], var22[1], var22[2], var29);
               GL11.glTexCoord2f(var18 + (var19 - var18) * var29[0], var29[1]);
               GL11.glVertex3d(var23, var25, var27);
            }
         }

         GL11.glEnd();
         GL11.glDisable(3042);
      }
   }

   static float[] itemGeometry(int var0) {
      return spriteGeometry(Items.tile[var0], var0);
   }

   /** The atlas tile {@code var2} as a sprite of one pixel's thickness, one block wide, centred on the origin (cached under {@code var0}). */
   static float[] spriteGeometry(int var2, int var0) {
      float[] var1 = (float[])itemGeo.get(var0);
      if (var1 != null) {
         return var1;
      }

      int var3 = Tex.tx(var2);
      int var4 = Tex.ty(var2);
      ArrayList var5 = new ArrayList();
      float var6 = 0.03125F;

      for (int var7 = 0; var7 < 16; var7++) {
         for (int var8 = 0; var8 < 16; var8++) {
            int var9 = Tex.atlasPixels[(var4 + var7) * Tex.PX + var3 + var8];
            if (var9 >>> 24 != 0) {
               float var10 = var8 / 16.0F - 0.5F;
               float var11 = (var8 + 1) / 16.0F - 0.5F;
               float var12 = 0.5F - var7 / 16.0F;
               float var13 = 0.5F - (var7 + 1) / 16.0F;
               float var14 = (var3 + var8 + 0.5F) / Tex.PX;
               float var15 = (var4 + var7 + 0.5F) / Tex.PX;
               float var16 = (var3 + var8) / (float)Tex.PX;
               float var17 = (var3 + var8 + 1) / (float)Tex.PX;
               float var18 = (var4 + var7) / (float)Tex.PX;
               float var19 = (var4 + var7 + 1) / (float)Tex.PX;
               var5.add(
                  new float[]{
                     var10,
                     var12,
                     var6,
                     var16,
                     var18,
                     1.0F,
                     var11,
                     var12,
                     var6,
                     var17,
                     var18,
                     1.0F,
                     var11,
                     var13,
                     var6,
                     var17,
                     var19,
                     1.0F,
                     var10,
                     var13,
                     var6,
                     var16,
                     var19,
                     1.0F
                  }
               );
               var5.add(
                  new float[]{
                     var11,
                     var12,
                     -var6,
                     var17,
                     var18,
                     0.8F,
                     var10,
                     var12,
                     -var6,
                     var16,
                     var18,
                     0.8F,
                     var10,
                     var13,
                     -var6,
                     var16,
                     var19,
                     0.8F,
                     var11,
                     var13,
                     -var6,
                     var17,
                     var19,
                     0.8F
                  }
               );
               if (var8 == 0 || Tex.atlasPixels[(var4 + var7) * Tex.PX + var3 + var8 - 1] >>> 24 == 0) {
                  var5.add(
                     new float[]{
                        var10,
                        var12,
                        -var6,
                        var14,
                        var15,
                        0.6F,
                        var10,
                        var12,
                        var6,
                        var14,
                        var15,
                        0.6F,
                        var10,
                        var13,
                        var6,
                        var14,
                        var15,
                        0.6F,
                        var10,
                        var13,
                        -var6,
                        var14,
                        var15,
                        0.6F
                     }
                  );
               }

               if (var8 == 15 || Tex.atlasPixels[(var4 + var7) * Tex.PX + var3 + var8 + 1] >>> 24 == 0) {
                  var5.add(
                     new float[]{
                        var11,
                        var12,
                        var6,
                        var14,
                        var15,
                        0.6F,
                        var11,
                        var12,
                        -var6,
                        var14,
                        var15,
                        0.6F,
                        var11,
                        var13,
                        -var6,
                        var14,
                        var15,
                        0.6F,
                        var11,
                        var13,
                        var6,
                        var14,
                        var15,
                        0.6F
                     }
                  );
               }

               if (var7 == 0 || Tex.atlasPixels[(var4 + var7 - 1) * Tex.PX + var3 + var8] >>> 24 == 0) {
                  var5.add(
                     new float[]{
                        var10,
                        var12,
                        -var6,
                        var14,
                        var15,
                        0.9F,
                        var11,
                        var12,
                        -var6,
                        var14,
                        var15,
                        0.9F,
                        var11,
                        var12,
                        var6,
                        var14,
                        var15,
                        0.9F,
                        var10,
                        var12,
                        var6,
                        var14,
                        var15,
                        0.9F
                     }
                  );
               }

               if (var7 == 15 || Tex.atlasPixels[(var4 + var7 + 1) * Tex.PX + var3 + var8] >>> 24 == 0) {
                  var5.add(
                     new float[]{
                        var10,
                        var13,
                        var6,
                        var14,
                        var15,
                        0.5F,
                        var11,
                        var13,
                        var6,
                        var14,
                        var15,
                        0.5F,
                        var11,
                        var13,
                        -var6,
                        var14,
                        var15,
                        0.5F,
                        var10,
                        var13,
                        -var6,
                        var14,
                        var15,
                        0.5F
                     }
                  );
               }
            }
         }
      }

      var1 = new float[var5.size() * 24];

      for (int var21 = 0; var21 < var5.size(); var21++) {
         System.arraycopy(var5.get(var21), 0, var1, var21 * 24, 24);
      }

      itemGeo.put(var0, var1);
      return var1;
   }

   public static boolean isGun(int id) {
      return id == Redstone319.PISTOL
         || id == Redstone319.MACHINE_GUN
         || id == Redstone319.SNIPER_RIFLE
         || id == Redstone319.SIEGE_CANNON
         || id == Redstone319.MORTAR_STRIKE;
   }

   private static void gunBox(float x0, float y0, float z0, float x1, float y1, float z1, int rgb, float bright) {
      float r = ((rgb >> 16) & 255) / 255.0F;
      float g = ((rgb >> 8) & 255) / 255.0F;
      float b = (rgb & 255) / 255.0F;
      // Top (+Y)
      float s0 = bright * 1.00F;
      GL11.glColor3f(r * s0, g * s0, b * s0);
      GL11.glTexCoord2f(0.5F, 0.5F);
      GL11.glVertex3f(x0, y1, z0);
      GL11.glVertex3f(x0, y1, z1);
      GL11.glVertex3f(x1, y1, z1);
      GL11.glVertex3f(x1, y1, z0);
      // Bottom (-Y)
      float s1 = bright * 0.56F;
      GL11.glColor3f(r * s1, g * s1, b * s1);
      GL11.glTexCoord2f(0.5F, 0.5F);
      GL11.glVertex3f(x0, y0, z1);
      GL11.glVertex3f(x0, y0, z0);
      GL11.glVertex3f(x1, y0, z0);
      GL11.glVertex3f(x1, y0, z1);
      // Front/Muzzle (+X)
      float s2 = bright * 0.88F;
      GL11.glColor3f(r * s2, g * s2, b * s2);
      GL11.glTexCoord2f(0.5F, 0.5F);
      GL11.glVertex3f(x1, y0, z1);
      GL11.glVertex3f(x1, y0, z0);
      GL11.glVertex3f(x1, y1, z0);
      GL11.glVertex3f(x1, y1, z1);
      // Rear/Butt (-X)
      float s3 = bright * 0.74F;
      GL11.glColor3f(r * s3, g * s3, b * s3);
      GL11.glTexCoord2f(0.5F, 0.5F);
      GL11.glVertex3f(x0, y0, z0);
      GL11.glVertex3f(x0, y0, z1);
      GL11.glVertex3f(x0, y1, z1);
      GL11.glVertex3f(x0, y1, z0);
      // Right Side (+Z)
      float s4 = bright * 0.82F;
      GL11.glColor3f(r * s4, g * s4, b * s4);
      GL11.glTexCoord2f(0.5F, 0.5F);
      GL11.glVertex3f(x0, y0, z1);
      GL11.glVertex3f(x1, y0, z1);
      GL11.glVertex3f(x1, y1, z1);
      GL11.glVertex3f(x0, y1, z1);
      // Left Side (-Z)
      float s5 = bright * 0.78F;
      GL11.glColor3f(r * s5, g * s5, b * s5);
      GL11.glTexCoord2f(0.5F, 0.5F);
      GL11.glVertex3f(x1, y0, z0);
      GL11.glVertex3f(x0, y0, z0);
      GL11.glVertex3f(x0, y1, z0);
      GL11.glVertex3f(x1, y1, z0);
   }

   /** Renders a multi-box 3D firearm model for PISTOL (459), MACHINE_GUN (460), and SNIPER_RIFLE (461). */
   public static void drawGunModel3D(int id, float bright) {
      boolean prevCull = GL11.glIsEnabled(2884);
      boolean prevAlpha = GL11.glIsEnabled(3008);
      GL11.glDisable(2884);
      GL11.glEnable(3553);
      GL11.glBindTexture(3553, SkyGen.whiteTex());
      GL11.glDisable(3008);
      GL11.glBegin(7);

      if (id == Redstone319.PISTOL) {
         // 1. Angled Ergonomic Polymer Grip & Magazine Baseplate
         gunBox(-0.27F, -0.36F, -0.052F, -0.09F, -0.18F, 0.052F, 0x242830, bright);
         gunBox(-0.24F, -0.20F, -0.052F, -0.06F, -0.02F, 0.052F, 0x282C35, bright);
         gunBox(-0.23F, -0.32F, -0.060F, -0.11F, -0.06F, 0.060F, 0x1A1D24, bright); // Textured side grip panels
         gunBox(-0.29F, -0.40F, -0.058F, -0.08F, -0.36F, 0.058F, 0x15181E, bright); // Flared mag baseplate
         // 2. Trigger Guard & Curved Steel Trigger
         gunBox(-0.06F, -0.14F, -0.022F, 0.09F, -0.115F, 0.022F, 0x2C313A, bright);
         gunBox(0.065F, -0.14F, -0.022F, 0.09F, -0.02F, 0.022F, 0x2C313A, bright);
         gunBox(-0.02F, -0.11F, -0.015F, 0.02F, -0.03F, 0.015F, 0x9098A6, bright); // Silver trigger
         // 3. Lower Receiver Frame & Under-Barrel Tactical Laser/Light Module
         gunBox(-0.28F, -0.03F, -0.054F, 0.34F, 0.04F, 0.054F, 0x353B46, bright);
         gunBox(0.09F, -0.095F, -0.042F, 0.29F, -0.03F, 0.042F, 0x22262E, bright); // Under-barrel tac-laser housing
         gunBox(0.285F, -0.082F, -0.026F, 0.305F, -0.042F, 0.026F, 0xFF3830, 1.0F); // Glowing red laser lens
         // 4. Machined Gunmetal Upper Slide, Ejection Port & Rear Hammer
         gunBox(-0.30F, 0.04F, -0.062F, 0.36F, 0.155F, 0.062F, 0x626C7C, bright);
         gunBox(-0.28F, 0.145F, -0.048F, 0.34F, 0.165F, 0.048F, 0x7A8596, bright); // Top slide bevel highlight
         gunBox(-0.05F, 0.09F, 0.010F, 0.11F, 0.160F, 0.065F, 0xC8D0DC, bright); // Silver barrel hood / ejection port
         gunBox(-0.33F, 0.065F, -0.020F, -0.29F, 0.125F, 0.020F, 0x454C58, bright); // Rear hammer spur
         // Rear slide serrations
         gunBox(-0.25F, 0.055F, -0.066F, -0.23F, 0.14F, 0.066F, 0x3E4450, bright);
         gunBox(-0.20F, 0.055F, -0.066F, -0.18F, 0.14F, 0.066F, 0x3E4450, bright);
         gunBox(-0.15F, 0.055F, -0.066F, -0.13F, 0.14F, 0.066F, 0x3E4450, bright);
         // 5. Extended Threaded Barrel & Dark Bore
         gunBox(0.36F, 0.068F, -0.032F, 0.44F, 0.132F, 0.032F, 0x3C424E, bright);
         gunBox(0.435F, 0.082F, -0.020F, 0.446F, 0.118F, 0.020F, 0x0E1014, bright); // Muzzle bore
         // 6. Glowing Tritium 3-Dot Iron Sights
         gunBox(-0.28F, 0.162F, -0.048F, -0.22F, 0.195F, -0.016F, 0x262A32, bright);
         gunBox(-0.28F, 0.162F, 0.016F, -0.22F, 0.195F, 0.048F, 0x262A32, bright);
         gunBox(-0.285F, 0.172F, -0.038F, -0.275F, 0.188F, -0.024F, 0x50FF78, 1.0F); // Left rear tritium dot
         gunBox(-0.285F, 0.172F, 0.024F, -0.275F, 0.188F, 0.038F, 0x50FF78, 1.0F);  // Right rear tritium dot
         gunBox(0.29F, 0.162F, -0.016F, 0.34F, 0.198F, 0.016F, 0x262A32, bright);    // Front sight blade
         gunBox(0.285F, 0.175F, -0.012F, 0.305F, 0.195F, 0.012F, 0x50FF78, 1.0F);    // Front tritium dot
      } else if (id == Redstone319.MACHINE_GUN) {
         // 1. Tactical Collapsible Buttstock & Rubber Recoil Pad
         gunBox(-0.54F, -0.18F, -0.052F, -0.48F, 0.08F, 0.052F, 0x181B20, bright);  // Rubberized shoulder pad
         gunBox(-0.48F, -0.06F, -0.044F, -0.24F, 0.075F, 0.044F, 0x2A2F38, bright); // Upper stock tube
         gunBox(-0.48F, -0.14F, -0.032F, -0.26F, -0.09F, 0.032F, 0x383E4A, bright); // Lower stock strut
         // 2. Heavy Steel Upper & Lower Receiver
         gunBox(-0.24F, -0.045F, -0.066F, 0.17F, 0.140F, 0.066F, 0x464E5C, bright);
         gunBox(-0.22F, 0.010F, -0.070F, 0.14F, 0.115F, 0.070F, 0x586272, bright);  // Receiver side stamping
         gunBox(-0.04F, 0.055F, 0.010F, 0.11F, 0.120F, 0.074F, 0xB8C2D0, bright);   // Steel bolt carrier in ejection port
         gunBox(0.08F, 0.085F, 0.066F, 0.14F, 0.115F, 0.125F, 0x282C34, bright);    // Side charging handle
         gunBox(-0.14F, 0.015F, -0.076F, -0.06F, 0.045F, 0.076F, 0xD83830, bright); // Red fire-selector switch
         // 3. Ergonomic Pistol Grip & Trigger Guard
         gunBox(-0.21F, -0.35F, -0.048F, -0.07F, -0.045F, 0.048F, 0x22262E, bright);
         gunBox(-0.07F, -0.14F, -0.022F, 0.02F, -0.115F, 0.022F, 0x2E333D, bright);
         gunBox(-0.04F, -0.115F, -0.015F, -0.01F, -0.045F, 0.015F, 0x9098A6, bright);
         // 4. Curved 30-Round Banana Magazine (2 stepped boxes + brass ammo window)
         gunBox(-0.01F, -0.23F, -0.044F, 0.12F, -0.045F, 0.044F, 0x262B34, bright);
         gunBox(0.02F, -0.43F, -0.042F, 0.15F, -0.22F, 0.042F, 0x20252D, bright);
         gunBox(0.045F, -0.36F, -0.046F, 0.095F, -0.12F, 0.046F, 0xD8A438, bright); // Brass 5.56mm ammo window stripe!
         // 5. Ventilated Quad-Rail Handguard & Vertical Tactical Foregrip
         gunBox(0.17F, -0.025F, -0.058F, 0.41F, 0.120F, 0.058F, 0x343A45, bright);
         gunBox(0.20F, 0.025F, -0.064F, 0.38F, 0.075F, 0.064F, 0x22262E, bright);   // Side Picatinny rail & cooling vents
         gunBox(0.23F, -0.26F, -0.038F, 0.31F, -0.025F, 0.038F, 0x1E222A, bright);  // Vertical tactical foregrip
         // 6. Rifled Barrel, Front Sight Post & Slotted Flash Hider
         gunBox(0.41F, 0.025F, -0.030F, 0.56F, 0.085F, 0.030F, 0x525A68, bright);
         gunBox(0.50F, 0.012F, -0.042F, 0.59F, 0.098F, 0.042F, 0x2C313A, bright);   // Slotted muzzle brake / flash hider
         gunBox(0.36F, 0.120F, -0.018F, 0.40F, 0.195F, 0.018F, 0x2C313A, bright);   // Front iron sight tower
         // 7. Top Picatinny Rail & 3D Holographic Red-Dot Reflex Sight
         gunBox(-0.20F, 0.140F, -0.038F, 0.35F, 0.165F, 0.038F, 0x2A2F38, bright);
         gunBox(-0.14F, 0.165F, -0.052F, 0.04F, 0.195F, 0.052F, 0x1E222A, bright);  // Holo sight base
         gunBox(-0.12F, 0.195F, -0.052F, 0.02F, 0.270F, -0.036F, 0x282D36, bright); // Holo sight left wall
         gunBox(-0.12F, 0.195F, 0.036F, 0.02F, 0.270F, 0.052F, 0x282D36, bright);   // Holo sight right wall
         gunBox(-0.12F, 0.254F, -0.052F, 0.02F, 0.272F, 0.052F, 0x282D36, bright);  // Holo sight top hood
         gunBox(-0.06F, 0.218F, -0.014F, -0.03F, 0.242F, 0.014F, 0xFF3830, 1.0F);   // Glowing red holographic reticle dot!
      } else if (id == Redstone319.SNIPER_RIFLE) {
         // 1. AWP Olive-Drab Tactical Stock, Adjustable Cheek Rest & Rubber Buttpad
         gunBox(-0.64F, -0.19F, -0.058F, -0.58F, 0.085F, 0.058F, 0x181B20, bright); // Heavy rubber recoil pad
         gunBox(-0.58F, -0.15F, -0.054F, -0.20F, 0.080F, 0.054F, 0x3A5834, bright); // Olive-drab AWP stock body
         gunBox(-0.46F, 0.080F, -0.048F, -0.26F, 0.135F, 0.048F, 0x22262E, bright); // Black adjustable cheek rest
         // 2. Olive-Drab Sniper Chassis & Pistol Grip
         gunBox(-0.20F, -0.055F, -0.062F, 0.32F, 0.080F, 0.062F, 0x44663C, bright);
         gunBox(-0.22F, -0.33F, -0.048F, -0.09F, -0.055F, 0.048F, 0x2E4628, bright);
         // 3. Heavy Detachable Box Magazine (.338 Lapua)
         gunBox(-0.03F, -0.26F, -0.046F, 0.14F, -0.055F, 0.046F, 0x242830, bright);
         gunBox(-0.04F, -0.28F, -0.050F, 0.15F, -0.25F, 0.050F, 0x181B20, bright);
         // 4. Machined Steel Bolt-Action Receiver & Protruding Bolt Handle
         gunBox(-0.18F, 0.080F, -0.056F, 0.22F, 0.155F, 0.056F, 0x586272, bright);
         gunBox(-0.05F, 0.095F, 0.010F, 0.12F, 0.150F, 0.062F, 0xC8D2DE, bright);   // Polished steel bolt body
         gunBox(-0.12F, 0.095F, 0.056F, -0.06F, 0.130F, 0.145F, 0x909AA8, bright);  // Protruding bolt handle & knob
         // 5. Long Free-Floating Fluted Bull Barrel & Dual-Baffle Tank Muzzle Brake
         gunBox(0.32F, 0.048F, -0.032F, 0.68F, 0.112F, 0.032F, 0x4E5664, bright);
         gunBox(0.65F, 0.028F, -0.056F, 0.76F, 0.132F, 0.056F, 0x2C313A, bright);   // Heavy rectangular muzzle brake
         // 6. Folded Front Bipod Legs Under Fore-End
         gunBox(0.18F, -0.125F, -0.068F, 0.38F, -0.055F, -0.028F, 0x262A32, bright);
         gunBox(0.18F, -0.125F, 0.028F, 0.38F, -0.055F, 0.068F, 0x262A32, bright);
         // 7. Dual Scope Rings & Large 3D Telescopic Sniper Scope (8x)
         gunBox(-0.13F, 0.155F, -0.036F, -0.07F, 0.210F, 0.036F, 0x22262E, bright); // Rear scope mount ring
         gunBox(0.07F, 0.155F, -0.036F, 0.13F, 0.210F, 0.036F, 0x22262E, bright);   // Front scope mount ring
         gunBox(-0.22F, 0.205F, -0.044F, 0.24F, 0.288F, 0.044F, 0x262B34, bright);  // Main scope tube
         gunBox(-0.31F, 0.190F, -0.056F, -0.20F, 0.302F, 0.056F, 0x1E222A, bright); // Rear eyepiece bell
         gunBox(0.22F, 0.182F, -0.064F, 0.34F, 0.310F, 0.064F, 0x1E222A, bright);   // Front objective bell
         gunBox(-0.03F, 0.288F, -0.028F, 0.03F, 0.328F, 0.028F, 0x383E4A, bright);  // Top elevation turret knob
         gunBox(-0.03F, 0.225F, 0.044F, 0.03F, 0.270F, 0.082F, 0x383E4A, bright);   // Side windage turret knob
         gunBox(0.335F, 0.198F, -0.048F, 0.346F, 0.294F, 0.048F, 0x48E8FF, 1.0F);   // Glowing cyan coated front lens
         gunBox(-0.315F, 0.205F, -0.040F, -0.305F, 0.286F, 0.040F, 0x48E8FF, 1.0F); // Glowing cyan rear eyepiece lens
      } else if (id == Redstone319.SIEGE_CANNON || id == Redstone319.MORTAR_STRIKE) {
         boolean isMortar = id == Redstone319.MORTAR_STRIKE;
         // Heavy Oak Carriage Base & Brass Reinforcing Bands
         gunBox(-0.46F, -0.22F, -0.11F, 0.18F, -0.04F, 0.11F, 0x5C3A20, bright);
         gunBox(-0.36F, -0.34F, -0.055F, -0.18F, -0.04F, 0.055F, 0x3E2614, bright);
         // Heavy Cast-Iron Cannon Barrel & Flared Brass Muzzle Ring
         gunBox(-0.34F, -0.04F, -0.105F, 0.52F, 0.20F, 0.105F, isMortar ? 0x38424E : 0x262B34, bright);
         gunBox(-0.12F, -0.055F, -0.118F, -0.04F, 0.215F, 0.118F, 0xD4A230, bright);
         gunBox(0.18F, -0.055F, -0.118F, 0.26F, 0.215F, 0.118F, 0xD4A230, bright);
         gunBox(0.46F, -0.070F, -0.128F, 0.58F, 0.230F, 0.128F, isMortar ? 0xE07824 : 0xD8A432, bright);
         // Glowing Fuse & Sight Post
         gunBox(-0.30F, 0.20F, -0.025F, -0.22F, 0.28F, 0.025F, 0xFF7820, 1.0F);
      }

      GL11.glEnd();
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      GL11.glBindTexture(3553, Tex.atlas);
      if (prevCull) {
         GL11.glEnable(2884);
      }
      if (prevAlpha) GL11.glEnable(3008);
   }

   public static void drawItemModel(int var0, float var1) {
      if (var0 == Items.BOW) {
         GL11.glPushMatrix();
         GL11.glTranslatef(-0.25F, 0, 0);
         BowSprite.draw(0, false, var1);
         GL11.glPopMatrix();
         return;
      }
      if (isGun(var0)) {
         drawGunModel3D(var0, var1);
         return;
      }
      drawSprite(itemGeometry(var0), var1, null);
   }

   /** Update 3.11: a torch, flower, ladder ... in the hand is its tile as a sprite, like Minecraft's flat items. */
   static boolean flatBlock(int id) {
      int k = Blocks.kind[id];
      return k == 1 || k == 7 || Blocks.isLadder(id) || id == 32 || Blocks.isWallTorch(id) || Blocks313.flatItem(id);
   }

   static void drawFlatBlock(int id, float bright) {
      drawSprite(spriteGeometry(Blocks.tiles[id][0], 100000 + id), id == 32 ? Math.max(bright, 0.9F) : bright, Gui.tintFor(id));
   }

   static void drawSprite(float[] var2, float var1, int[] tint) {
      GL11.glEnable(3553);
      GL11.glBindTexture(3553, Tex.atlas);
      GL11.glEnable(3008);
      GL11.glAlphaFunc(516, 0.1F);
      GL11.glBegin(7);

      for (int var3 = 0; var3 < var2.length; var3 += 6) {
         float var4 = var2[var3 + 5] * var1;
         if (tint != null) {
            GL11.glColor3f(var4 * tint[0] / 255.0F, var4 * tint[1] / 255.0F, var4 * tint[2] / 255.0F);
         } else {
            GL11.glColor3f(var4, var4, var4);
         }

         GL11.glTexCoord2f(var2[var3 + 3], var2[var3 + 4]);
         GL11.glVertex3f(var2[var3], var2[var3 + 1], var2[var3 + 2]);
      }

      GL11.glEnd();
      GL11.glDisable(3008);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
   }

   public static void drawBlockModel(int var0, float var1) {
      if (var0 == Redstone319.PISTON || var0 == Redstone319.STICKY_PISTON || var0 == Redstone319.OBSERVER) {
         var0++;
      }

      GL11.glEnable(3553);
      GL11.glBindTexture(3553, Tex.atlas);
      GL11.glEnable(3008);
      GL11.glAlphaFunc(516, 0.3F);
      int var2 = Blocks.kind[var0];
      int[] var3 = Gui.tintFor(var0);
      GL11.glBegin(7);
      if (var2 != 1 && var2 != 7 && !Blocks.isLadder(var0) && var0 != 32 && !Blocks.isWallTorch(var0) && !Blocks313.flatItem(var0)) {
         float[] var22 = new float[2];
         double[][] var23 = Gui.iconBoxes(var0);
         if (var23 == null) {
            var23 = new double[][]{{0.0, 0.0, 0.0, 1.0, 1.0, 1.0}};
         }

         for (double[] var27 : var23) {
            for (int var28 = 0; var28 < 2; var28++) {
               for (int var29 = 0; var29 < 6; var29++) {
                  int var30 = var28 == 0 ? Blocks.tiles[var0][var29] : 2;
                  if (var28 == 0 && var27.length > 10) {
                     double ov = var29 == 0 ? var27[9] : (var29 == 1 ? var27[10] : var27[8]);
                     if (ov >= 0) {
                        var30 = (int)ov;
                     }
                  }

                  if (var28 != 1 || var0 == 2 && var29 >= 2) {
                     float var31 = Mesher.SHADE[var29] * var1;
                     float var32 = var31;
                     float var33 = var31;
                     float var34 = var31;
                     boolean var35 = var3 != null && (Blocks.tint[var0] == 2 || var29 == 0 && var28 == 0 || var28 == 1);
                     if (var35) {
                        var32 *= var3[0] / 255.0F;
                        var33 *= var3[1] / 255.0F;
                        var34 *= var3[2] / 255.0F;
                     }

                     if (var28 == 0 && var27.length > 7 && var27[7] >= 0) {
                        int c = (int)var27[7];
                        var32 *= (c >> 16 & 255) / 255.0F;
                        var33 *= (c >> 8 & 255) / 255.0F;
                        var34 *= (c & 255) / 255.0F;
                     }

                     GL11.glColor3f(var32, var33, var34);
                     float var18 = Tex.tu(var30);
                     float var19 = Tex.tv(var30);

                     for (int var20 = 0; var20 < 4; var20++) {
                        int[] var21 = Mesher.FC[var29][var20];
                        Mesher.uvFor(var29, var21[0], var21[1], var21[2], var22);
                        GL11.glTexCoord2f(var18 + var22[0] * Tex.TS, var19 + var22[1] * Tex.TS);
                        GL11.glVertex3f(
                           (float)(var27[0] + var21[0] * (var27[3] - var27[0])) - 0.5F,
                           (float)(var27[1] + var21[1] * (var27[4] - var27[1])) - 0.5F,
                           (float)(var27[2] + var21[2] * (var27[5] - var27[2])) - 0.5F
                        );
                     }
                  }
               }
            }
         }
      } else {
         int var4 = Blocks.tiles[var0][0];
         float var5 = Tex.tu(var4);
         float var6 = Tex.tv(var4);
         float var7 = var1;
         float var8 = var1;
         float var9 = var1;
         if (var3 != null) {
            var7 *= var3[0] / 255.0F;
            var8 *= var3[1] / 255.0F;
            var9 *= var3[2] / 255.0F;
         }

         GL11.glColor3f(var7, var8, var9);
         float[][] var10 = new float[][]{
            {-0.5F, -0.5F, 0.0F, 0.5F, -0.5F, 0.0F, 0.5F, 0.5F, 0.0F, -0.5F, 0.5F, 0.0F},
            {0.0F, -0.5F, -0.5F, 0.0F, -0.5F, 0.5F, 0.0F, 0.5F, 0.5F, 0.0F, 0.5F, -0.5F}
         };
         float[] var11 = new float[]{0.0F, 1.0F, 1.0F, 0.0F};
         float[] var12 = new float[]{1.0F, 1.0F, 0.0F, 0.0F};

         for (float[] var16 : var10) {
            for (int var17 = 0; var17 < 4; var17++) {
               GL11.glTexCoord2f(var5 + var11[var17] * Tex.TS, var6 + var12[var17] * Tex.TS);
               GL11.glVertex3f(var16[var17 * 3], var16[var17 * 3 + 1], var16[var17 * 3 + 2]);
            }
         }
      }

      GL11.glEnd();
      GL11.glDisable(3008);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
   }

   /**
    * Update 3.9: the camera of the third-person views. mode 1 = behind the player, 2 = in front of the player looking back. The camera
    * moves 4 blocks along the line of sight from the eye and stops short of any block in the way. Returns {x, y, z, yaw, pitch}.
    */
   static double[] thirdPerson(World w, double ex, double ey, double ez, float yaw, float pitch, int mode) {
      double yr = Math.toRadians(yaw);
      double pr = Math.toRadians(pitch);
      double sgn = mode == 1 ? -1.0 : 1.0;
      double dx = sgn * -Math.sin(yr) * Math.cos(pr);
      double dy = sgn * -Math.sin(pr);
      double dz = sgn * Math.cos(yr) * Math.cos(pr);
      double dist = 4.0;
      Hit hh = w.raycast(ex, ey, ez, dx, dy, dz, dist + 0.35);
      if (hh != null) {
         dist = Math.max(0.25, hh.t - 0.35);
      }

      return new double[]{ex + dx * dist, ey + dy * dist, ez + dz * dist, mode == 2 ? yaw + 180.0F : yaw, mode == 2 ? -pitch : pitch};
   }

   /** Update 3.9: the player's own body (skin, walking, hitting, sneaking) in the third-person views. */
   void drawSelf(float partial) {
      Net319.drawPeers(this, partial);
      Game g = this.g;
      Player pl = g.player;
      if (g.view == 0 || pl.spectator || pl.dead) {
         return;
      }

      double x = pl.ix(partial);
      double y = pl.iy(partial);
      double z = pl.iz(partial);
      float bright = this.brightnessAt(g.world, x, y + 1.0, z);
      GL11.glEnable(2929);
      GL11.glDisable(2884);
      GL11.glPushMatrix();
      GL11.glTranslated(x - this.camX, y - this.camY, z - this.camZ);
      PlayerModel.draw(g.skinPixels(), g.skinSlim(), g.pose(partial), bright);
      GL11.glPopMatrix();
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
   }

   void drawEntities(float var1) {
      World var2 = this.g.world;
      GL11.glEnable(2929);

      for (Item var4 : var2.items) {
         double var5 = var4.x + var4.vx * var1;
         double var7 = var4.y + var4.vy * var1;
         double var9 = var4.z + var4.vz * var1;
         double dx = var5 - this.camX;
         double dy = var7 - this.camY;
         double dz = var9 - this.camZ;
         if (dx * dx + dy * dy + dz * dz > 4096.0 || !this.visible((float)dx, (float)dy + 0.25F, (float)dz, 0.65F)) {
            continue;
         }
         float var11 = this.brightnessAt(var2, var5, var7 + 0.2, var9);
         float var12 = (float)(Math.sin((var4.age + var1) / 10.0) * 0.1 + 0.1);
         GL11.glPushMatrix();
         GL11.glTranslated(dx, dy + 0.25 + var12, dz);
         GL11.glRotatef(var4.spin + var1 * 3.0F, 0.0F, 1.0F, 0.0F);
         float var13 = Blocks.isBlockId(var4.id) && Blocks.kind[var4.id] == 0 ? 0.25F : 0.35F;
         if (Items.isItem(var4.id)) {
            GL11.glScalef(0.45F, 0.45F, 0.45F);
            drawItemModel(var4.id, var11);
         } else {
            GL11.glScalef(var13, var13, var13);
            drawBlockModel(var4.id, var11);
         }

         GL11.glPopMatrix();
      }

      MobRender.render(this.g, this, var1);

      for (Falling var15 : var2.fallers) {
         // Update 3.9: interpolated between the last two ticks (sand and TNT used to step 20 times a second)
         double fx = Double.isNaN(var15.px) ? var15.x : var15.px + (var15.x - var15.px) * var1;
         double fy = Double.isNaN(var15.px) ? var15.y : var15.py + (var15.y - var15.py) * var1;
         double fz = Double.isNaN(var15.px) ? var15.z : var15.pz + (var15.z - var15.pz) * var1;
         double fdx = fx - this.camX;
         double fdy = fy - this.camY;
         double fdz = fz - this.camZ;
         if (fdx * fdx + fdy * fdy + fdz * fdz > 6400.0 || !this.visible((float)fdx, (float)fdy + 0.5F, (float)fdz, 0.85F)) {
            continue;
         }
         float var16 = this.brightnessAt(var2, fx, fy + 0.5, fz);
         if (var15.fuse > 0 && var15.fuse / 5 % 2 == 0) {
            var16 = 3.0F;
         }

         GL11.glPushMatrix();
         GL11.glTranslated(fdx, fdy + 0.5, fdz);
         if (var15.fuse > 0 && var15.fuse < 10) {
            // (the swelling of a TNT about to blow used to be applied before the translation, which also scaled its position)
            float var6 = 1.0F + (10 - var15.fuse) * 0.03F;
            GL11.glScalef(var6, var6, var6);
         }

         GL11.glScalef(0.99F, 0.99F, 0.99F);
         drawBlockModel(var15.id, var16);
         GL11.glPopMatrix();
      }

      this.drawFallingTrees(var1);
      DisasterFx.drawDebris(this, var1);
   }

   /**
    * Update 3.10: trees that are falling over (see Trees): the blocks turn as one piece about the cell that was cut, 0..90 degrees
    * (the axis (dz, 0, -dx) tips "up" towards the fall direction), then the lying trunk drops.
    */
   void drawFallingTrees(float partial) {
      World w = this.g.world;
      if (w.trees.falling().isEmpty()) {
         return;
      }

      GL11.glEnable(2929);
      GL11.glEnable(3553);
      GL11.glBindTexture(3553, Tex.atlas);
      GL11.glEnable(3008);
      GL11.glAlphaFunc(516, 0.3F);
      float[] uv = new float[2];
      for (Trees.Fall f : w.trees.falling()) {
         double cx = f.pivotX() + 0.5;
         double cy = f.pivotY() + 0.5 - f.drop(partial);
         double cz = f.pivotZ() + 0.5;
         float bright = this.brightnessAt(w, cx + f.dx() * 2.0, cy + 1.5, cz + f.dz() * 2.0);
         GL11.glPushMatrix();
         GL11.glTranslated(cx - this.camX, cy - this.camY, cz - this.camZ);
         GL11.glRotatef(f.angle(partial), f.dz(), 0.0F, -f.dx());
         GL11.glBegin(7);
         for (int i = 0; i < f.count(); i++) {
            emitCube(f.blockId(i), f.offX(i), f.offY(i), f.offZ(i), bright, uv);
         }

         GL11.glEnd();
         GL11.glPopMatrix();
      }

      GL11.glDisable(3008);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
   }

   /** The six faces of a full block centred at (ox, oy, oz) in immediate mode (what drawBlockModel does for a cube, without the state changes). */
   static void emitCube(int id, float ox, float oy, float oz, float bright, float[] uv) {
      int[] tint = Blocks.tint[id] == 2 ? Gui.tintFor(id) : null;
      for (int face = 0; face < 6; face++) {
         int tile = Blocks.tiles[id][face];
         float shade = Mesher.SHADE[face] * bright;
         if (tint != null) {
            GL11.glColor3f(shade * tint[0] / 255.0F, shade * tint[1] / 255.0F, shade * tint[2] / 255.0F);
         } else {
            GL11.glColor3f(shade, shade, shade);
         }

         float u0 = Tex.tu(tile);
         float v0 = Tex.tv(tile);
         for (int k = 0; k < 4; k++) {
            int[] c = Mesher.FC[face][k];
            Mesher.uvFor(face, c[0], c[1], c[2], uv);
            GL11.glTexCoord2f(u0 + uv[0] * Tex.TS, v0 + uv[1] * Tex.TS);
            GL11.glVertex3f(ox + c[0] - 0.5F, oy + c[1] - 0.5F, oz + c[2] - 0.5F);
         }
      }
   }

   void drawParticles(float var1) {
      if (!this.g.particles.isEmpty()) {
         World var2 = this.g.world;
         GL11.glEnable(3553);
         GL11.glBindTexture(3553, Tex.atlas);
         GL11.glDisable(3008);
         double var3 = Math.toRadians(this.g.player.yaw);
         double var5 = -Math.cos(var3);
         double var7 = -Math.sin(var3);
         // Update 3.16: two passes - ordinary particles (they may fade out and shrink), then the glowing ones (flames, embers, fireflies, bubbles), added to the picture
         GL11.glEnable(3042);
         for (int pass = 0; pass < 2; pass++) {
            GL11.glBlendFunc(770, pass == 0 ? 771 : 1);
            if (pass == 1) {
               GL11.glDepthMask(false);
            }

            GL11.glBegin(7);

            for (Particle var10 : this.g.particles) {
               if (var10.glow != (pass == 1)) {
                  continue;
               }

               double var11 = var10.x + var10.vx * var1 - this.camX;
               double var13 = var10.y + var10.vy * var1 - this.camY;
               double var15 = var10.z + var10.vz * var1 - this.camZ;
               if (var11 * var11 + var13 * var13 + var15 * var15 > 2304.0 || !this.visible((float)var11, (float)var13, (float)var15, 0.65F)) {
                  continue;
               }
               float var17 = var10.glow ? var10.shade : this.brightnessAt(var2, var10.x, var10.y, var10.z) * var10.shade;
               float left = var10.life0 > 0 ? Math.max(0.0F, Math.min(1.0F, (var10.life - var1) / var10.life0)) : 1.0F;
               float alpha = var10.alpha;
               float var18 = var10.size;
               if ((var10.fade & 1) != 0) {
                  alpha *= Math.min(1.0F, left * 2.5F) * Math.min(1.0F, (1.0F - left) * 10.0F + 0.15F);
               }

               if ((var10.fade & 2) != 0) {
                  var18 *= 0.35F + 0.65F * left;
               }

               if (var10.twinkle != 0) {
                  alpha *= 0.55F + 0.45F * (float)Math.sin((var10.life0 - var10.life + var1) * 0.35F + var10.phase);
               }

               GL11.glColor4f(var17 * var10.r, var17 * var10.g, var17 * var10.b, alpha);
               float var19 = var10.u;
               float var20 = var10.v;
               float var21 = var10.u + 4.0F / Tex.PX;
               float var22 = var10.v + 4.0F / Tex.PX;
               GL11.glTexCoord2f(var19, var22);
               GL11.glVertex3d(var11 - var5 * var18, var13 - var18, var15 - var7 * var18);
               GL11.glTexCoord2f(var21, var22);
               GL11.glVertex3d(var11 + var5 * var18, var13 - var18, var15 + var7 * var18);
               GL11.glTexCoord2f(var21, var20);
               GL11.glVertex3d(var11 + var5 * var18, var13 + var18, var15 + var7 * var18);
               GL11.glTexCoord2f(var19, var20);
               GL11.glVertex3d(var11 - var5 * var18, var13 + var18, var15 - var7 * var18);
            }

            GL11.glEnd();
            if (pass == 1) {
               GL11.glDepthMask(true);
            }
         }

         GL11.glDisable(3042);
         GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      }
   }

   /**
    * The hand in the first-person view, with Minecraft 1.12's own numbers (ItemRenderer.renderItemInFirstPerson): the empty hand is the arm in
    * the player's skin, an item sits where Minecraft puts it for that kind of model (flat items, tools, blocks), the swing, the equip
    * animation (swapping the item) and the eating animation are Minecraft's formulas, and the hand lags a little behind the camera when you turn.
    *
    * @param swing     0..1 progress of the swing (attack / dig / place), 0 when not swinging
    * @param use       0..1 progress of eating, or -1
    * @param equip     0..1: 1 = the item is fully raised
    * @param swayPitch the hand's lag behind the camera, degrees (already scaled)
    */
   public void renderHand(float partial, Stack item, float swing, float use, float bright, int fbW, int fbH, float equip, float swayPitch, float swayYaw, float bobDist, float bobAmp) {
      GL11.glDepthMask(true);
      GL11.glEnable(2929);
      GL11.glDepthFunc(515);
      GL11.glDisable(3042);
      GL11.glDisable(2884);
      GL11.glColorMask(true, true, true, true);
      GL11.glClear(256);
      GL11.glMatrixMode(5889);
      GL11.glLoadIdentity();
      double near = 0.05;
      double top = near * Math.tan(Math.toRadians(35.0));
      double right = top * fbW / fbH;
      GL11.glFrustum(-right, right, -top, top, near, 20.0);
      GL11.glMatrixMode(5888);
      GL11.glLoadIdentity();
      GL11.glDisable(2912);
      float p = 1.0F - equip;
      // Smooth equip curve (ease-out)
      p = p * p * (3.0F - 2.0F * p);
      // Organic idle breathing + vertical jump/fall inertia + sprint cadence
      float idleT = (float)Perf.seconds(600.0);
      float breathX = (float)Math.cos(idleT * 1.15F) * 0.008F;
      float breathY = (float)Math.sin(idleT * 2.10F) * 0.013F;
      float breathPitch = (float)Math.cos(idleT * 2.10F) * 1.15F;
      float breathRoll = (float)Math.sin(idleT * 1.15F) * 0.85F;
      Player pl = this.g != null ? this.g.player : null;
      float vyOff = 0.0F;
      float vyTilt = 0.0F;
      float sprintPitch = 0.0F;
      if (pl != null) {
         vyOff = Math.max(-0.065F, Math.min(0.065F, (float)(-pl.vy * 0.085)));
         vyTilt = Math.max(-7.5F, Math.min(7.5F, (float)(-pl.vy * 10.5)));
         if (pl.sprinting && bobAmp > 0.01F) {
            sprintPitch = -5.5F * Math.min(1.0F, bobAmp * 8.0F);
         }
      }

      GL11.glTranslatef(breathX, breathY + vyOff, 0.0F);
      if (bobAmp > 0.0F) {
         // Minecraft's EntityRenderer.applyBobbing + figure-8 wrist roll
         float bSin = (float)Math.sin(bobDist * Math.PI);
         float bCos = (float)Math.cos(bobDist * Math.PI);
         GL11.glTranslatef(bSin * bobAmp * 0.58F, -Math.abs(bCos * bobAmp) * 1.08F, -Math.abs(bSin * bobAmp) * 0.18F);
         GL11.glRotatef(bSin * bobAmp * 3.8F, 0.0F, 0.0F, 1.0F);
         GL11.glRotatef(Math.abs((float)Math.cos(bobDist * Math.PI - 0.2F) * bobAmp) * 5.6F, 1.0F, 0.0F, 0.0F);
      }

      float clPitch = Math.max(-14.0F, Math.min(14.0F, swayPitch * 1.35F));
      float clYaw = Math.max(-16.0F, Math.min(16.0F, swayYaw * 1.35F));
      GL11.glRotatef(clPitch + breathPitch + vyTilt + sprintPitch, 1.0F, 0.0F, 0.0F);
      GL11.glRotatef(clYaw, 0.0F, 1.0F, 0.0F);
      GL11.glRotatef(-clYaw * 0.32F + breathRoll, 0.0F, 0.0F, 1.0F);
      boolean entShader = this.shadersOn() && this.sh.entityOk;
      if (entShader) {
         this.sh.useHand(this.mv, this.camX, this.camY, this.camZ, this.lastSky, this.lastBlk);
         bright = 1.0F; // The hand shader applies the lightmap once; vertex colours only shade model faces.
      } else {
         bright = HandLighting.fallback(bright, this.g.opt.gamma);
      }

      float s1 = (float)Math.sqrt(swing);
      if (item == null) {
         PlayerModel.drawArmFirstPerson(this.g.skinPixels(), this.g.skinSlim(), bright, swing, p);
      } else {
         int id = item.id;
         if (isGun(id)) {
            float recoil = WeaponPose.recoil(id, SkyGen.playerGunCooldown, partial);
            boolean flash = SkyGen.playerGunCooldown > 0
               && WeaponPose.cooldown(id) - SkyGen.playerGunCooldown + partial < 1.0F;
            GL11.glTranslatef(0.36F, -0.32F + p * -0.45F + recoil * 0.025F, -0.70F + recoil * 0.08F);
            GL11.glRotatef(WeaponPose.FIRST_PERSON_YAW, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(2.0F + recoil * 7.0F, 0.0F, 0.0F, 1.0F);
            float gunScale = id == Redstone319.SNIPER_RIFLE ? 0.78F : (id == Redstone319.MACHINE_GUN ? 0.74F : 0.68F);
            GL11.glScalef(gunScale, gunScale, gunScale);
            drawGunModel3D(id, bright);
            if (flash) {
               float mx = WeaponPose.muzzleX(id);
               float my = WeaponPose.muzzleY(id);
               GL11.glEnable(3553);
               GL11.glBindTexture(3553, SkyGen.whiteTex());
               GL11.glBegin(7);
               gunBox(mx, my - 0.09F, -0.015F, mx + 0.16F, my + 0.09F, 0.015F, 0xFFE868, 1.0F);
               gunBox(mx, my - 0.015F, -0.09F, mx + 0.16F, my + 0.015F, 0.09F, 0xFF9828, 1.0F);
               gunBox(mx - 0.02F, my - 0.045F, -0.045F, mx + 0.08F, my + 0.045F, 0.045F, 0xFFFFFF, 1.0F);
               GL11.glEnd();
               GL11.glBindTexture(3553, Tex.atlas);
            }
         } else if (id == Items.BOW) {
            float pull = this.g.bowVisualDraw(partial);
            boolean nocked = this.g.bowCharge > 0;
            String preview = System.getProperty("bc.bowdraw");
            if (preview != null && System.getProperty("bc.auto") != null) {
               try { pull = WeaponPose.clamp(Float.parseFloat(preview)); nocked = pull > 0; }
               catch (NumberFormatException ignored) { }
            }
            // A flat pixel-art bow. Drawing changes the sprite, not a tube/limb mesh.
            GL11.glTranslatef(0.33F - 0.08F * pull, -0.24F - p * 0.5F + 0.035F * pull, -0.92F);
            GL11.glRotatef(-18.0F + 6.0F * pull, 0, 0, 1);
            GL11.glRotatef(-20.0F, 0, 1, 0); // keep the flat sprite visible, not edge-on like a gun barrel
            GL11.glScalef(0.82F, 0.82F, 0.82F);
            BowSprite.draw(pull, nocked, bright);
         } else {
            if (use >= 0.0F) {
               float f = 32.0F * (1.0F - use) + 1.0F; // ticks left
               float f1 = f / 32.0F;
               if (f1 < 0.8F) {
                  GL11.glTranslatef(0.0F, Math.abs((float)Math.cos(f / 4.0F * Math.PI) * 0.1F), 0.0F);
               }

               float f3 = 1.0F - (float)Math.pow(f1, 27.0);
               GL11.glTranslatef(f3 * 0.6F, f3 * -0.5F, 0.0F);
               GL11.glRotatef(f3 * 90.0F, 0.0F, 1.0F, 0.0F);
               GL11.glRotatef(f3 * 10.0F, 1.0F, 0.0F, 0.0F);
               GL11.glRotatef(f3 * 30.0F, 0.0F, 0.0F, 1.0F);
               GL11.glTranslatef(0.56F, -0.52F + p * -0.6F, -0.72F);
            } else {
               float swArc = (float)Math.sin(s1 * Math.PI);
               float swSnap = (float)Math.sin(swing * Math.PI);
               GL11.glTranslatef(-0.42F * swArc, 0.24F * (float)Math.sin(s1 * Math.PI * 2.0), -0.28F * swSnap);
               GL11.glTranslatef(0.56F, -0.52F + p * -0.6F, -0.72F);
               float f = (float)Math.sin(swing * swing * Math.PI);
               GL11.glRotatef(45.0F - f * 24.0F, 0.0F, 1.0F, 0.0F);
               GL11.glRotatef(swArc * -24.0F, 0.0F, 0.0F, 1.0F);
               GL11.glRotatef(swArc * -84.0F, 1.0F, 0.0F, 0.0F);
               GL11.glRotatef(-45.0F, 0.0F, 1.0F, 0.0F);
            }

            if (Items.isItem(id) || flatBlock(id)) {
               // item/generated and item/handheld, "firstperson_righthand": rotation [0, -90, 25], translation [1.13, 3.2, 1.13], scale 0.68
               GL11.glTranslatef(1.13F / 16.0F, 3.2F / 16.0F, 1.13F / 16.0F);
               GL11.glRotatef(-90.0F, 0.0F, 1.0F, 0.0F);
               GL11.glRotatef(25.0F, 0.0F, 0.0F, 1.0F);
               GL11.glScalef(0.68F, 0.68F, 0.68F);
               if (Items.isItem(id)) {
                  drawItemModel(id, bright);
               } else {
                  drawFlatBlock(id, bright);
               }
            } else {
               // block/block, "firstperson_righthand": rotation [0, 45, 0], scale 0.4
               GL11.glRotatef(45.0F, 0.0F, 1.0F, 0.0F);
               GL11.glScalef(0.4F, 0.4F, 0.4F);
               drawBlockModel(id, bright);
            }
         }
      }

      if (entShader) {
         this.sh.stopEntity();
      }

      GL11.glMatrixMode(5888);
      GL11.glLoadIdentity();
   }

   static {
      ArrayList<int[]> var0 = new ArrayList<>();

      for (int var1 = -34; var1 <= 34; var1++) {
         for (int var2 = -34; var2 <= 34; var2++) {
            var0.add(new int[]{var1, var2});
         }
      }

      var0.sort(Comparator.comparingInt(var0x -> var0x[0] * var0x[0] + var0x[1] * var0x[1]));
      RING = (int[][])var0.toArray(new int[0][]);
      itemGeo = new HashMap();
   }

   static final class Cand implements Comparable<Renderer.Cand> {
      Chunk c;
      int sy;
      double d;

      public int compareTo(Renderer.Cand var1) {
         return Double.compare(this.d, var1.d);
      }
   }

   static final class Done {
      Chunk c;
      int sy;
      Result r;
   }

   static final class Vis {
      Chunk c;
      int sy;
      float d;
   }
}
