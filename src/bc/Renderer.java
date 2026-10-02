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
   public Lod lod;
   public Shaders sh;
   /** Update 3.8: weather rendering (clouds without shaders, rain, snow, lightning). */
   public final WeatherFx wfx = new WeatherFx(this);
   int shFrames;
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
   final float[] stars = new float[2100];
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

      this.lod = var1 == null ? null : new Lod(this, var1.gen);
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

   public boolean chunkMeshed(int var1, int var2) {
      Chunk var3 = this.g.world.chunk(var1, var2);
      if (var3 != null && var3.lit) {
         for (Section var7 : var3.sections) {
            if (var7.dirty || var7.inFlight) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public void scheduleMeshes(World var1, int var2) {
      int var3 = this.meshThreads * 6;
      if (this.inflight.get() < var3) {
         int var4 = (int)Math.floor(this.camX) >> 4;
         int var5 = (int)Math.floor(this.camZ) >> 4;
         int var6 = this.g.opt.renderDistance;
         int var7 = (var6 + 1) * (var6 + 1);
         boolean var8 = this.g.opt.smooth;
         int var9 = 0;

         label56:
         for (int[] var13 : RING) {
            if (var13[0] * var13[0] + var13[1] * var13[1] <= var7) {
               Chunk var14 = var1.chunk(var4 + var13[0], var5 + var13[1]);
               if (var14 != null) {
                  boolean var15 = false;
                  boolean var16 = false;

                  for (int var17 = 0; var17 < 16; var17++) {
                     Section var18 = var14.sections[var17];
                     if (var18.dirty && !var18.inFlight) {
                        if (!var16) {
                           var15 = this.ready(var1, var14);
                           var16 = true;
                        }

                        if (!var15) {
                           continue label56;
                        }

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

   void upload(Chunk var1, int var2, Result var3) {
      Section var4 = var1.sections[var2];
      var4.nO = var3.nO;
      var4.nT = var3.nT;
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
      for (int var5 = -1; var5 <= 1; var5++) {
         for (int var6 = -1; var6 <= 1; var6++) {
            for (int var7 = -1; var7 <= 1; var7++) {
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
                        this.upload(var11, var12, Mesher.build(var1, var11, var12, this.g.opt.smooth));
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
      float var4 = (float)(var1 % 24000L) + var3;
      this.sunAngle = (float)((Math.PI * 2) * (var4 - 6000.0F) / 24000.0);
      float var5 = (float)Math.cos(this.sunAngle);
      this.daylight = clamp01(0.5F + var5 * 1.3F);
      this.skyLightFactor = 0.2F + 0.8F * this.daylight;
      float var6 = this.daylight;
      this.topR = lerp(0.01F, 0.4F, var6);
      this.topG = lerp(0.015F, 0.6F, var6);
      this.topB = lerp(0.04F, 0.98F, var6);
      this.fogR = lerp(0.02F, 0.76F, var6);
      this.fogG = lerp(0.025F, 0.85F, var6);
      this.fogB = lerp(0.05F, 1.0F, var6);
      float var7 = Math.max(0.0F, 1.0F - Math.abs(var5) / 0.2F);
      if (var7 > 0.0F) {
         this.fogR = lerp(this.fogR, 1.0F, var7 * 0.55F);
         this.fogG = lerp(this.fogG, 0.52F, var7 * 0.55F);
         this.fogB = lerp(this.fogB, 0.3F, var7 * 0.55F);
         this.topR = lerp(this.topR, 0.45F, var7 * 0.12F);
         this.topG = lerp(this.topG, 0.35F, var7 * 0.12F);
         this.topB = lerp(this.topB, 0.55F, var7 * 0.12F);
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

   void updateLightmap() {
      float var1 = this.g.opt.gamma;
      if (Math.abs(this.skyLightFactor - this.lastLm) > 0.004F || var1 != this.lastGamma) {
         GL13.glActiveTexture(33985);
         Tex.updateLightmap(this.skyLightFactor, 0.0F, var1);
         GL13.glActiveTexture(33984);
         this.lastLm = this.skyLightFactor;
         this.lastGamma = var1;
      }
   }

   public float brightnessAt(World var1, double var2, double var4, double var6) {
      int var8 = (int)Math.floor(var2);
      int var9 = (int)Math.floor(var4);
      int var10 = (int)Math.floor(var6);
      int var11 = var1.getSky(var8, var9, var10);
      int var12 = var1.getBlk(var8, var9, var10);
      float var13 = Tex.lightCurve(var11) * this.skyLightFactor;
      float var14 = Tex.lightCurve(var12);
      return Math.max(0.08F, Math.min(1.0F, Math.max(var13, var14) + 0.05F));
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
      boolean var6 = var4.eyeInWater;
      boolean var7 = Blocks.isLava(var5.getBlock((int)Math.floor(this.camX), (int)Math.floor(this.camY), (int)Math.floor(this.camZ)));
      int var8 = (int)Math.floor(this.camX);
      int var9 = (int)Math.floor(this.camY);
      int var10 = (int)Math.floor(this.camZ);
      float var11 = Math.max(0.0F, var5.getSky(var8, var9, var10) / 15.0F);
      this.caveF = this.caveF + (var11 - this.caveF) * 0.04F;
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
      int var13 = this.g.opt.renderDistance;
      float var14 = var13 * 16.0F;
      int var15 = this.g.opt.lodDistance;
      boolean var16 = this.lod != null && var15 > var13;
      if (var16) {
         var14 = var15 * 16.0F;
      }

      var12 = var14;
      var11 = var14 * (var16 ? 0.4F : 0.55F);
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
      GL11.glViewport(0, 0, var2, var3);
      GL11.glClearColor(var41, var42, var43, 1.0F);
      GL11.glClear(16640);
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
      GL11.glRotatef(var4.pitch, 1.0F, 0.0F, 0.0F);
      GL11.glRotatef(var4.yaw + 180.0F, 0.0F, 1.0F, 0.0F);
      GL11.glGetFloatv(2983, this.proj);
      GL11.glGetFloatv(2982, this.mv);
      this.extractFrustum();
      GL11.glDisable(2912);
      if (!var6 && !var7) {
         this.drawSky(var41, var42, var43);
      }

      GL11.glEnable(2912);
      GL11.glFogi(2917, 9729);
      GL11.glFogf(2915, var11);
      GL11.glFogf(2916, var12);
      GL11.glFogfv(2918, new float[]{var41, var42, var43, 1.0F});
      float[] var26 = new float[]{var41, var42, var43};
      float[] var27 = new float[]{this.topR, this.topG, this.topB};
      float[] var28 = new float[]{this.fogR, this.fogG, this.fogB};
      this.sh.sunAtten = this.wfx.sunVis;
      if (var16 && !var6 && !var7) {
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

         this.lod.render(this.camX, this.camY, this.camZ, var15, var13, this.daylight);
         if (var29) {
            this.sh.stopLod();
         }
      }

      this.sh.sunAtten = this.wfx.sunVis;
      this.vis.clear();
      int var48 = (int)Math.floor(this.camX) >> 4;
      int var49 = (int)Math.floor(this.camZ) >> 4;

      for (Chunk var32 : var5.chunks.values()) {
         int var33 = var32.cx - var48;
         int var34 = var32.cz - var49;
         if (var33 * var33 + var34 * var34 <= (var13 + 1) * (var13 + 1)) {
            for (int var35 = 0; var35 < 16; var35++) {
               Section var36 = var32.sections[var35];
               if (var36.nO != 0 || var36.nT != 0) {
                  float var37 = (float)(var32.cx * 16 + 8 - this.camX);
                  float var38 = (float)(var35 * 16 + 8 - this.camY);
                  float var39 = (float)(var32.cz * 16 + 8 - this.camZ);
                  if (this.visible(var37, var38, var39, 14.0F)) {
                     Renderer.Vis var40 = new Renderer.Vis();
                     var40.c = var32;
                     var40.sy = var35;
                     var40.d = var37 * var37 + var38 * var38 + var39 * var39;
                     this.vis.add(var40);
                  }
               }
            }
         }
      }

      this.vis.sort((var0, var1x) -> Float.compare(var0.d, var1x.d));
      this.visibleSections = this.vis.size();
      boolean var50 = this.shadersOn();
      boolean var51 = false;
      if (var50) {
         float var52 = this.sunAngle;
         float var55 = (float)(-Math.sin(var52));
         float var58 = (float)Math.cos(var52);
         this.sh.setSun(var55, var58, 0.0F);
         var51 = var58 > 0.08F && !var7 && this.caveF > 0.08F && this.wfx.sunVis > 0.12F;
         if (var51) {
            if (++this.shFrames >= 30 || this.sh.shadowStale(var55, var58, 0.0F, this.camX, this.camY, this.camZ)) {
               this.shFrames = 0;
               this.sh.computeShadow(var55, var58, 0.0F, this.camX, this.camY, this.camZ);
               this.renderShadowPass(var2, var3);
            }
         } else {
            this.sh.mapValid = false;
         }
      }

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

      for (Renderer.Vis var56 : this.vis) {
         Section var59 = var56.c.sections[var56.sy];
         if (var59.nO != 0 && var59.vboO != 0) {
            GL11.glPushMatrix();
            GL11.glTranslatef((float)(var56.c.cx * 16 - this.camX), (float)(var56.sy * 16 - this.camY), (float)(var56.c.cz * 16 - this.camZ));
            bindPointers(var59.vboO);
            GL11.glDrawArrays(7, 0, var59.nO);
            this.drawCalls++;
            GL11.glPopMatrix();
         }
      }

      arrays(false);
      if (var50) {
         this.sh.stopWorld();
      }

      lightmapUnit(false);
      this.drawEntities(var1);
      this.drawParticles(var1);
      if (this.g.target != null && this.g.screenIsPlay()) {
         this.drawCrack();
         this.drawSelection();
      }

      if (this.g.opt.clouds && !var6 && !var7 && (!var50 || !this.sh.cloudsOk || !this.sh.sceneActive)) {
         this.wfx.drawClouds(var1, var41, var42, var43, var11, var12);
      }

      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      GL11.glDisable(3008);
      GL11.glBindTexture(3553, Tex.atlas);
      lightmapUnit(true);
      arrays(true);
      if (var50) {
         this.sh.useWorld(this.mv, this.camX, this.camY, this.camZ, true, var11, var12, var26, var27, var28, this.daylight, var51, 9, 25, 26, 27, 23);
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
            GL11.glPopMatrix();
         }
      }

      arrays(false);
      if (var50) {
         this.sh.stopWorld();
      }

      lightmapUnit(false);
      GL11.glDisable(3042);
      GL11.glDisable(2912);
      GL11.glDepthMask(true);
      if (var50 && this.g.opt.clouds && !var6 && !var7 && this.sh.cloudsOk && this.sh.sceneActive) {
         this.sh.renderClouds(this.proj, this.mv, this.camX, this.camY, this.camZ, var27, var28, var26, var2, var3, this.wfx);
      }

      if (!var6 && !var7) {
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

      for (int var8 = -2; var8 <= 2; var8++) {
         for (int var9 = -2; var9 <= 2; var9++) {
            Chunk var10 = this.g.world.chunk(var6 + var9, var7 + var8);
            if (var10 != null) {
               for (int var11 = var3; var11 <= var4; var11++) {
                  for (int var12 = 0; var12 < 16; var12++) {
                     for (int var13 = 0; var13 < 16; var13++) {
                        int var14 = var10.blocks[Chunk.idx(var13, var11, var12)] & 255;
                        if (var14 != 0 && Blocks.emit[var14] >= 10) {
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
      if (var0 == 25) {
         return new float[]{1.0F, 0.82F, 0.5F};
      } else if (Blocks.isLava(var0)) {
         return new float[]{1.0F, 0.42F, 0.1F};
      } else {
         return var0 == 68 ? new float[]{1.0F, 0.55F, 0.22F} : new float[]{1.0F, 0.66F, 0.3F};
      }
   }

   void fillDynLights() {
      Arrays.fill(this.sh.dynL, 0.0F);
      Arrays.fill(this.sh.dynC, 0.0F);
      int var1 = 0;
      float var2 = (float)(System.nanoTime() / 1.0E9);
      float var3 = 1.0F + 0.05F * (float)Math.sin(var2 * 11.0F) + 0.04F * (float)Math.sin(var2 * 23.0F + 1.0F);
      int var4 = this.g.heldId();
      int var5 = var4 > 0 && var4 < 256 ? Blocks.emit[var4] : 0;
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
      }

      for (Item var19 : this.g.world.items) {
         if (var1 >= 3) {
            break;
         }

         if (var19.id > 0 && var19.id < 256 && Blocks.emit[var19.id] > 0) {
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
         if (var24 >= 0 && var24 < 256) {
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
      this.sh.beginShadow();
      arrays(true);
      GL13.glActiveTexture(33984);
      GL11.glEnable(3553);
      GL11.glBindTexture(3553, Tex.atlas);
      int var3 = (int)Math.floor(this.camX) >> 4;
      int var4 = (int)Math.floor(this.camZ) >> 4;
      int var5 = 9;

      for (int var6 = -var5; var6 <= var5; var6++) {
         for (int var7 = -var5; var7 <= var5; var7++) {
            Chunk var8 = this.g.world.chunk(var3 + var7, var4 + var6);
            if (var8 != null) {
               for (int var9 = 0; var9 < 16; var9++) {
                  Section var10 = var8.sections[var9];
                  if (var10.nO != 0 && var10.vboO != 0) {
                     GL11.glPushMatrix();
                     GL11.glTranslatef((float)(var8.cx * 16 - this.camX), (float)(var9 * 16 - this.camY), (float)(var8.cz * 16 - this.camZ));
                     bindPointers(var10.vboO);
                     GL11.glDrawArrays(7, 0, var10.nO);
                     GL11.glPopMatrix();
                  }
               }
            }
         }
      }

      arrays(false);
      this.sh.endShadow(var1, var2, true);
   }

   void drawSky(float var1, float var2, float var3) {
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

   void drawClouds(float var1, float var2, float var3, float var4) {
      double var5 = 130.0 - this.camY;
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
      float[] var1 = (float[])itemGeo.get(var0);
      if (var1 != null) {
         return var1;
      }

      int var2 = Items.tile[var0];
      int var3 = var2 % 16 * 16;
      int var4 = var2 / 16 * 16;
      ArrayList var5 = new ArrayList();
      float var6 = 0.03125F;

      for (int var7 = 0; var7 < 16; var7++) {
         for (int var8 = 0; var8 < 16; var8++) {
            int var9 = Tex.atlasPixels[(var4 + var7) * 256 + var3 + var8];
            if (var9 >>> 24 != 0) {
               float var10 = var8 / 16.0F - 0.5F;
               float var11 = (var8 + 1) / 16.0F - 0.5F;
               float var12 = 0.5F - var7 / 16.0F;
               float var13 = 0.5F - (var7 + 1) / 16.0F;
               float var14 = (var3 + var8 + 0.5F) / 256.0F;
               float var15 = (var4 + var7 + 0.5F) / 256.0F;
               float var16 = (var3 + var8) / 256.0F;
               float var17 = (var3 + var8 + 1) / 256.0F;
               float var18 = (var4 + var7) / 256.0F;
               float var19 = (var4 + var7 + 1) / 256.0F;
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
               if (var8 == 0 || Tex.atlasPixels[(var4 + var7) * 256 + var3 + var8 - 1] >>> 24 == 0) {
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

               if (var8 == 15 || Tex.atlasPixels[(var4 + var7) * 256 + var3 + var8 + 1] >>> 24 == 0) {
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

               if (var7 == 0 || Tex.atlasPixels[(var4 + var7 - 1) * 256 + var3 + var8] >>> 24 == 0) {
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

               if (var7 == 15 || Tex.atlasPixels[(var4 + var7 + 1) * 256 + var3 + var8] >>> 24 == 0) {
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

   public static void drawItemModel(int var0, float var1) {
      float[] var2 = itemGeometry(var0);
      GL11.glEnable(3553);
      GL11.glBindTexture(3553, Tex.atlas);
      GL11.glEnable(3008);
      GL11.glAlphaFunc(516, 0.1F);
      GL11.glBegin(7);

      for (int var3 = 0; var3 < var2.length; var3 += 6) {
         float var4 = var2[var3 + 5] * var1;
         GL11.glColor3f(var4, var4, var4);
         GL11.glTexCoord2f(var2[var3 + 3], var2[var3 + 4]);
         GL11.glVertex3f(var2[var3], var2[var3 + 1], var2[var3 + 2]);
      }

      GL11.glEnd();
      GL11.glDisable(3008);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
   }

   public static void drawBlockModel(int var0, float var1) {
      GL11.glEnable(3553);
      GL11.glBindTexture(3553, Tex.atlas);
      GL11.glEnable(3008);
      GL11.glAlphaFunc(516, 0.3F);
      int var2 = Blocks.kind[var0];
      int[] var3 = Gui.tintFor(var0);
      GL11.glBegin(7);
      if (var2 != 1 && var2 != 7 && !Blocks.isLadder(var0) && var0 != 32 && !Blocks.isWallTorch(var0)) {
         float[] var22 = new float[2];
         double[][] var23 = Gui.iconBoxes(var0);
         if (var23 == null) {
            var23 = new double[][]{{0.0, 0.0, 0.0, 1.0, 1.0, 1.0}};
         }

         for (double[] var27 : var23) {
            for (int var28 = 0; var28 < 2; var28++) {
               for (int var29 = 0; var29 < 6; var29++) {
                  int var30 = var28 == 0 ? Blocks.tiles[var0][var29] : 2;
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

                     GL11.glColor3f(var32, var33, var34);
                     float var18 = var30 % 16 / 16.0F;
                     float var19 = var30 / 16 / 16.0F;

                     for (int var20 = 0; var20 < 4; var20++) {
                        int[] var21 = Mesher.FC[var29][var20];
                        Mesher.uvFor(var29, var21[0], var21[1], var21[2], var22);
                        GL11.glTexCoord2f(var18 + var22[0] / 16.0F, var19 + var22[1] / 16.0F);
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
         float var5 = var4 % 16 / 16.0F;
         float var6 = var4 / 16 / 16.0F;
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
               GL11.glTexCoord2f(var5 + var11[var17] / 16.0F, var6 + var12[var17] / 16.0F);
               GL11.glVertex3f(var16[var17 * 3], var16[var17 * 3 + 1], var16[var17 * 3 + 2]);
            }
         }
      }

      GL11.glEnd();
      GL11.glDisable(3008);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
   }

   void drawEntities(float var1) {
      World var2 = this.g.world;
      GL11.glEnable(2929);

      for (Item var4 : var2.items) {
         double var5 = var4.x + var4.vx * var1;
         double var7 = var4.y + var4.vy * var1;
         double var9 = var4.z + var4.vz * var1;
         float var11 = this.brightnessAt(var2, var5, var7 + 0.2, var9);
         float var12 = (float)(Math.sin((var4.age + var1) / 10.0) * 0.1 + 0.1);
         GL11.glPushMatrix();
         GL11.glTranslated(var5 - this.camX, var7 - this.camY + 0.25 + var12, var9 - this.camZ);
         GL11.glRotatef(var4.spin + var1 * 3.0F, 0.0F, 1.0F, 0.0F);
         float var13 = var4.id < 256 && Blocks.kind[var4.id] == 0 ? 0.25F : 0.35F;
         if (var4.id >= 256) {
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
         float var16 = this.brightnessAt(var2, var15.x, var15.y + 0.5, var15.z);
         if (var15.fuse > 0 && var15.fuse / 5 % 2 == 0) {
            var16 = 3.0F;
         }

         GL11.glPushMatrix();
         if (var15.fuse > 0 && var15.fuse < 10) {
            float var6 = 1.0F + (10 - var15.fuse) * 0.03F;
            GL11.glScalef(var6, var6, var6);
         }

         GL11.glTranslated(var15.x - this.camX, var15.y - this.camY + 0.5, var15.z - this.camZ);
         GL11.glScalef(0.99F, 0.99F, 0.99F);
         drawBlockModel(var15.id, var16);
         GL11.glPopMatrix();
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
         GL11.glBegin(7);

         for (Particle var10 : this.g.particles) {
            double var11 = var10.x + var10.vx * var1 - this.camX;
            double var13 = var10.y + var10.vy * var1 - this.camY;
            double var15 = var10.z + var10.vz * var1 - this.camZ;
            float var17 = this.brightnessAt(var2, var10.x, var10.y, var10.z) * var10.shade;
            GL11.glColor3f(var17 * var10.r, var17 * var10.g, var17 * var10.b);
            float var18 = var10.size;
            float var19 = var10.u;
            float var20 = var10.v;
            float var21 = var10.u + 0.015625F;
            float var22 = var10.v + 0.015625F;
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
         GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      }
   }

   public void renderHand(float var1, Stack var2, float var3, float var4, float var5, int var6, int var7) {
      if (var2 != null) {
         int var8 = var2.id;
         GL11.glClear(256);
         GL11.glMatrixMode(5889);
         GL11.glLoadIdentity();
         double var9 = 0.05;
         double var11 = var9 * Math.tan(Math.toRadians(35.0));
         double var13 = var11 * var6 / var7;
         GL11.glFrustum(-var13, var13, -var11, var11, var9, 20.0);
         GL11.glMatrixMode(5888);
         GL11.glLoadIdentity();
         GL11.glDisable(2912);
         GL11.glEnable(2929);
         float var15 = var3;
         float var16 = (float)Math.sqrt(var15);
         float var17 = 0.62F - 0.4F * (float)Math.sin(var16 * Math.PI);
         float var18 = -0.56F + 0.2F * (float)Math.sin(var16 * Math.PI * 2.0);
         if (var8 >= 256) {
            var17 -= 0.1F;
            var18 += 0.3F;
         }

         float var19 = -0.9F - 0.2F * (float)Math.sin(var15 * Math.PI);
         if (var4 >= 0.0F) {
            float var20 = (float)Math.abs(Math.cos(var4 * 32.0F / 4.0 * Math.PI)) * 0.1F;
            float var21 = 1.0F - (float)Math.pow(1.0F - Math.min(1.0F, var4 * 4.0F), 3.0);
            var17 -= 0.45F * var21;
            var18 += 0.12F * var21 + (var4 > 0.2F ? var20 : 0.0F);
            var19 += 0.15F * var21;
         }

         GL11.glTranslatef(var17, var18, var19);
         if (var8 >= 256) {
            boolean var22 = Items.isTool(var8) || var8 == 268;
            GL11.glRotatef(45.0F, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef((float)Math.sin(var15 * var15 * Math.PI) * -20.0F, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef((float)Math.sin(var16 * Math.PI) * -20.0F, 0.0F, 0.0F, 1.0F);
            GL11.glRotatef((float)Math.sin(var16 * Math.PI) * -80.0F, 1.0F, 0.0F, 0.0F);
            GL11.glScalef(0.7F, 0.7F, 0.7F);
            GL11.glRotatef(var22 ? 10.0F : 0.0F, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(var22 ? 15.0F : 0.0F, 0.0F, 0.0F, 1.0F);
            GL11.glTranslatef(0.0F, 0.1F, 0.0F);
            drawItemModel(var8, var5);
         } else {
            GL11.glRotatef(45.0F, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef((float)Math.sin(var15 * var15 * Math.PI) * -20.0F, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef((float)Math.sin(var16 * Math.PI) * -20.0F, 0.0F, 0.0F, 1.0F);
            GL11.glRotatef((float)Math.sin(var16 * Math.PI) * -80.0F, 1.0F, 0.0F, 0.0F);
            float var23 = Blocks.kind[var8] == 0 ? 0.4F : 0.5F;
            GL11.glScalef(var23, var23, var23);
            if (var8 == 32) {
               var5 = Math.max(var5, 0.9F);
            }

            drawBlockModel(var8, var5);
         }

         GL11.glMatrixMode(5888);
         GL11.glLoadIdentity();
      }
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
