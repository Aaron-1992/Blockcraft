package bc;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

public final class Shaders {
   public boolean ok;
   public String error = "";
   int pWorld;
   int pDepth;
   int pBright;
   int pBlur;
   int pComp;
   int pLod;
   int pCloud;
   int pBlit;
   int pRay;
   int pEntity;
   /** Update 3.11: the water program (ShaderSrc.WATER_FS), a copy of the scene taken before the water is drawn (colour + depth) for refraction,
    *  the depth of the water and the reflections of the terrain in it. */
   int pWater;
   public boolean waterOk;
   int copyFbo;
   int copyTex;
   int copyDepth;
   /** Update 3.11: the entity program compiled (see ShaderSrc.ENTITY_FS). */
   public boolean entityOk;
   /** The entity program is the current one: Renderer.brightnessAt sends the light level of each entity to it. */
   public boolean entityOn;
   int rayFbo;
   int rayTex;
   int depthCopyDummy;
   final float[] projM = new float[16];
   final FloatBuffer projBuf = BufferUtils.createFloatBuffer(16);
   float sunU;
   float sunV;
   float rayAmt;
   final float[] rayCol = new float[]{1.0F, 0.9F, 0.7F};
   static final String RAY_FS = String.join(
      "\n",
      new CharSequence[]{
         "#version 120",
         "uniform sampler2D scene;",
         "uniform sampler2D depth;",
         "varying vec2 vUv;",
         "void main(){ float d = texture2D(depth, vUv).r; vec3 c = texture2D(scene, vUv).rgb;",
         "  float sky = step(0.999985, d); float l = dot(c, vec3(0.299, 0.587, 0.114));",
         "  float m = sky * (0.45 + 0.55 * smoothstep(0.45, 0.95, l)); gl_FragColor = vec4(vec3(m), 1.0); }"
      }
   );
   public boolean lodOk;
   public boolean cloudsOk;
   int noiseTex;
   int cloudFbo;
   int cloudTex;
   int cw;
   int ch;
   public final float[] dynL = new float[48];
   public final float[] dynC = new float[48];
   public static final int SM = 2048;
   public static final float HALF = 84.0F;
   public static final float ZR = 400.0F;
   int shadowFbo;
   int shadowTex;
   int sceneFbo;
   int sceneTex;
   int sceneDepth;
   int bloomFbo0;
   int bloomFbo1;
   int bloomTex0;
   int bloomTex1;
   int fw;
   int fh;
   final HashMap<String, Integer> locs = new HashMap();
   public final float[] shadowM = new float[16];
   public final float[] clipM = new float[16];
   public float sunX;
   public float sunY;
   public float sunZ;
   public float sunUp;
   static final String DEPTH_VS = String.join(
      "\n",
      new CharSequence[]{
         "#version 120", "varying vec2 vUv;", "void main(){ gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex; vUv = gl_MultiTexCoord0.xy; }"
      }
   );
   static final String DEPTH_FS = String.join(
      "\n",
      new CharSequence[]{
         "#version 120", "uniform sampler2D tex;", "varying vec2 vUv;", "void main(){ if (texture2D(tex, vUv).a < 0.5) discard; gl_FragColor = vec4(1.0); }"
      }
   );
   static final String QUAD_VS = String.join(
      "\n", new CharSequence[]{"#version 120", "varying vec2 vUv;", "void main(){ gl_Position = gl_Vertex; vUv = gl_MultiTexCoord0.xy; }"}
   );
   static final String BRIGHT_FS = String.join(
      "\n",
      new CharSequence[]{
         "#version 120",
         "uniform sampler2D scene;",
         "varying vec2 vUv;",
         "void main(){ vec3 c = texture2D(scene, vUv).rgb; float l = min(c.r, min(c.g, c.b));",
         "  float k = smoothstep(0.78, 1.0, l); gl_FragColor = vec4(c * k, 1.0); }"
      }
   );
   static final String BLUR_FS = String.join(
      "\n",
      new CharSequence[]{
         "#version 120",
         "uniform sampler2D img;",
         "uniform vec2 dir;",
         "varying vec2 vUv;",
         "void main(){",
         "  vec3 s = texture2D(img, vUv).rgb * 0.227027;",
         "  s += texture2D(img, vUv + dir*1.3846).rgb * 0.316216; s += texture2D(img, vUv - dir*1.3846).rgb * 0.316216;",
         "  s += texture2D(img, vUv + dir*3.2308).rgb * 0.070270; s += texture2D(img, vUv - dir*3.2308).rgb * 0.070270;",
         "  gl_FragColor = vec4(s, 1.0); }"
      }
   );
   public boolean mapValid;
   double mapX;
   double mapY;
   double mapZ;
   float mapSx;
   float mapSy;
   float mapSz;
   final float[] viewM = new float[16];
   final float[] invViewM = new float[16];
   boolean sceneActive;
   /** The slab the cloud shader marches through: lowest storm base .. highest storm top, 146 + 46 + 28 + 150 = 370 (Update 3.8). */
   public static final float CLOUD_Y0 = 136.0F;
   public static final float CLOUD_Y1 = 380.0F;
   /** Update 3.8: how much of the sun's direct light gets through the clouds (set by the renderer every frame). */
   public float sunAtten = 1.0F;
   int wnoiseTex;

   int compile(int var1, String var2) {
      int var3 = GL20.glCreateShader(var1);
      GL20.glShaderSource(var3, var2);
      GL20.glCompileShader(var3);
      if (GL20.glGetShaderi(var3, 35713) == 0) {
         throw new RuntimeException("shader: " + GL20.glGetShaderInfoLog(var3));
      } else {
         return var3;
      }
   }

   int program(String var1, String var2) {
      int var3 = GL20.glCreateProgram();
      GL20.glAttachShader(var3, this.compile(35633, var1));
      GL20.glAttachShader(var3, this.compile(35632, var2));
      GL20.glLinkProgram(var3);
      if (GL20.glGetProgrami(var3, 35714) == 0) {
         throw new RuntimeException("link: " + GL20.glGetProgramInfoLog(var3));
      } else {
         return var3;
      }
   }

   public void init() {
      try {
         this.pLod = this.program(ShaderSrc.LOD_VS, ShaderSrc.LOD_FS);
         this.lodOk = true;
      } catch (Throwable var4) {
         this.lodOk = false;
         System.err.println("LOD shader disabled: " + var4.getMessage());
      }

      try {
         if (!GL.getCapabilities().OpenGL30) {
            throw new RuntimeException("OpenGL 3.0 required");
         }

         this.pWorld = this.program(ShaderSrc.WORLD_VS, ShaderSrc.WORLD_FS);
         this.pDepth = this.program(DEPTH_VS, DEPTH_FS);
         try {
            this.pEntity = this.program(ShaderSrc.ENTITY_VS, ShaderSrc.ENTITY_FS);
            this.entityOk = true;
         } catch (Throwable var6) {
            this.entityOk = false;
            System.err.println("Entity shader disabled: " + var6.getMessage());
         }

         try {
            this.pWater = this.program(ShaderSrc.WORLD_VS, ShaderSrc.WATER_FS);
            this.waterOk = true;
         } catch (Throwable var7) {
            this.waterOk = false;
            System.err.println("Water shader disabled (the plain one is used): " + var7.getMessage());
         }

         this.pBright = this.program(QUAD_VS, BRIGHT_FS);
         this.pBlur = this.program(QUAD_VS, BLUR_FS);
         this.pComp = this.program(QUAD_VS, ShaderSrc.COMP_FS);
         this.pRay = this.program(QUAD_VS, RAY_FS);

         try {
            this.pCloud = this.program(QUAD_VS, ShaderSrc.CLOUD_FS);
            this.pBlit = this.program(QUAD_VS, ShaderSrc.BLIT_FS);
            this.makeNoise();
            this.makeWeatherNoise();
            this.cloudsOk = true;
         } catch (Throwable var2) {
            this.cloudsOk = false;
            System.err.println("Volumetric clouds disabled: " + var2.getMessage());
         }

         this.shadowTex = GL11.glGenTextures();
         GL11.glBindTexture(3553, this.shadowTex);
         GL11.glTexImage2D(3553, 0, 33190, 2048, 2048, 0, 6402, 5126, (ByteBuffer)null);
         GL11.glTexParameteri(3553, 10241, 9728);
         GL11.glTexParameteri(3553, 10240, 9728);
         GL11.glTexParameteri(3553, 10242, 33069);
         GL11.glTexParameteri(3553, 10243, 33069);
         GL11.glTexParameterfv(3553, 4100, new float[]{1.0F, 1.0F, 1.0F, 1.0F});
         this.shadowFbo = GL30.glGenFramebuffers();
         GL30.glBindFramebuffer(36160, this.shadowFbo);
         GL30.glFramebufferTexture2D(36160, 36096, 3553, this.shadowTex, 0);
         GL11.glDrawBuffer(0);
         GL11.glReadBuffer(0);
         int var1 = GL30.glCheckFramebufferStatus(36160);
         GL30.glBindFramebuffer(36160, 0);
         GL11.glDrawBuffer(1029);
         GL11.glReadBuffer(1029);
         if (var1 != 36053) {
            throw new RuntimeException("shadow fbo incomplete " + var1);
         }

         this.ok = true;
      } catch (Throwable var3) {
         this.ok = false;
         this.error = String.valueOf(var3.getMessage());
         System.err.println("Shaders disabled: " + this.error);
      }
   }

   int tex2d(int var1, int var2) {
      int var3 = GL11.glGenTextures();
      GL11.glBindTexture(3553, var3);
      GL11.glTexImage2D(3553, 0, 6408, var1, var2, 0, 6408, 5121, (ByteBuffer)null);
      GL11.glTexParameteri(3553, 10241, 9729);
      GL11.glTexParameteri(3553, 10240, 9729);
      GL11.glTexParameteri(3553, 10242, 33071);
      GL11.glTexParameteri(3553, 10243, 33071);
      return var3;
   }

   int fbo(int var1) {
      int var2 = GL30.glGenFramebuffers();
      GL30.glBindFramebuffer(36160, var2);
      GL30.glFramebufferTexture2D(36160, 36064, 3553, var1, 0);
      return var2;
   }

   void resize(int var1, int var2) {
      if (var1 != this.fw || var2 != this.fh || this.sceneFbo == 0) {
         if (this.sceneFbo != 0) {
            GL30.glDeleteFramebuffers(this.sceneFbo);
            GL30.glDeleteFramebuffers(this.bloomFbo0);
            GL30.glDeleteFramebuffers(this.bloomFbo1);
            GL11.glDeleteTextures(this.sceneTex);
            GL11.glDeleteTextures(this.bloomTex0);
            GL11.glDeleteTextures(this.bloomTex1);
            GL11.glDeleteTextures(this.sceneDepth);
            if (this.rayFbo != 0) {
               GL30.glDeleteFramebuffers(this.rayFbo);
               GL11.glDeleteTextures(this.rayTex);
            }

            if (this.cloudFbo != 0) {
               GL30.glDeleteFramebuffers(this.cloudFbo);
               GL11.glDeleteTextures(this.cloudTex);
            }

            if (this.copyFbo != 0) {
               GL30.glDeleteFramebuffers(this.copyFbo);
               GL11.glDeleteTextures(this.copyTex);
               GL11.glDeleteTextures(this.copyDepth);
               this.copyFbo = 0;
            }
         }

         this.fw = var1;
         this.fh = var2;
         this.sceneTex = this.tex2d(var1, var2);
         this.sceneFbo = this.fbo(this.sceneTex);
         this.sceneDepth = GL11.glGenTextures();
         GL11.glBindTexture(3553, this.sceneDepth);
         GL11.glTexImage2D(3553, 0, 33190, var1, var2, 0, 6402, 5126, (ByteBuffer)null);
         GL11.glTexParameteri(3553, 10241, 9728);
         GL11.glTexParameteri(3553, 10240, 9728);
         GL11.glTexParameteri(3553, 10242, 33071);
         GL11.glTexParameteri(3553, 10243, 33071);
         GL30.glFramebufferTexture2D(36160, 36096, 3553, this.sceneDepth, 0);
         int var3 = GL30.glCheckFramebufferStatus(36160);
         int var4 = Math.max(1, var1 / 2);
         int var5 = Math.max(1, var2 / 2);
         this.bloomTex0 = this.tex2d(var4, var5);
         this.bloomFbo0 = this.fbo(this.bloomTex0);
         this.bloomTex1 = this.tex2d(var4, var5);
         this.bloomFbo1 = this.fbo(this.bloomTex1);
         this.cw = var4;
         this.ch = var5;
         this.rayTex = this.tex2d(this.cw, this.ch);
         this.rayFbo = this.fbo(this.rayTex);
         this.cloudTex = this.tex2d(this.cw, this.ch);
         this.cloudFbo = this.fbo(this.cloudTex);
         if (this.waterOk) {
            // the copy of the scene (colour + depth) that the water shader reads while the water is drawn into the scene itself
            this.copyTex = this.tex2d(var1, var2);
            this.copyFbo = this.fbo(this.copyTex);
            this.copyDepth = GL11.glGenTextures();
            GL11.glBindTexture(3553, this.copyDepth);
            GL11.glTexImage2D(3553, 0, 33190, var1, var2, 0, 6402, 5126, (ByteBuffer)null);
            GL11.glTexParameteri(3553, 10241, 9728);
            GL11.glTexParameteri(3553, 10240, 9728);
            GL11.glTexParameteri(3553, 10242, 33071);
            GL11.glTexParameteri(3553, 10243, 33071);
            GL30.glFramebufferTexture2D(36160, 36096, 3553, this.copyDepth, 0);
            if (GL30.glCheckFramebufferStatus(36160) != 36053) {
               System.err.println("Water copy FBO incomplete; the plain water is used");
               this.waterOk = false;
            }
         }

         GL30.glBindFramebuffer(36160, 0);
         if (var3 != 36053) {
            System.err.println("Scene FBO incomplete; shaders disabled");
            this.ok = false;
         }
      }
   }

   int loc(int var1, String var2) {
      String var3 = var1 + var2;
      Integer var4 = (Integer)this.locs.get(var3);
      if (var4 == null) {
         var4 = GL20.glGetUniformLocation(var1, var2);
         this.locs.put(var3, var4);
      }

      return var4;
   }

   void u1f(int var1, String var2, float var3) {
      GL20.glUniform1f(this.loc(var1, var2), var3);
   }

   void u1i(int var1, String var2, int var3) {
      GL20.glUniform1i(this.loc(var1, var2), var3);
   }

   public void setSun(float var1, float var2, float var3) {
      this.sunX = var1;
      this.sunY = var2;
      this.sunZ = var3;
   }

   public boolean shadowStale(float var1, float var2, float var3, double var4, double var6, double var8) {
      if (!this.mapValid) {
         return true;
      }

      double var10 = var4 - this.mapX;
      double var12 = var6 - this.mapY;
      double var14 = var8 - this.mapZ;
      return var10 * var10 + var12 * var12 + var14 * var14 > 144.0 ? true : var1 * this.mapSx + var2 * this.mapSy + var3 * this.mapSz < 0.999995F;
   }

   public void computeShadow(float var1, float var2, float var3, double var4, double var6, double var8) {
      this.mapValid = true;
      this.mapX = var4;
      this.mapY = var6;
      this.mapZ = var8;
      this.mapSx = var1;
      this.mapSy = var2;
      this.mapSz = var3;
      this.sunX = var1;
      this.sunY = var2;
      this.sunZ = var3;
      float var10 = -var1;
      float var11 = -var2;
      float var12 = -var3;
      float var13 = 0.0F;
      float var14 = 1.0F;
      float var15 = 0.0F;
      if (Math.abs(var11) > 0.98F) {
         var13 = 1.0F;
         var14 = 0.0F;
      }

      float var16 = var14 * var12 - var15 * var11;
      float var17 = var15 * var10 - var13 * var12;
      float var18 = var13 * var11 - var14 * var10;
      float var19 = (float)Math.sqrt(var16 * var16 + var17 * var17 + var18 * var18);
      var16 /= var19;
      var17 /= var19;
      var18 /= var19;
      float var20 = var11 * var18 - var12 * var17;
      float var21 = var12 * var16 - var10 * var18;
      float var22 = var10 * var17 - var11 * var16;
      double var23 = var16 * var4 + var17 * var6 + var18 * var8;
      double var25 = var20 * var4 + var21 * var6 + var22 * var8;
      double var27 = 0.08203125;
      double var29 = Math.floor(var23 / var27) * var27;
      double var31 = Math.floor(var25 / var27) * var27;
      float var33 = (float)(var23 - var29);
      float var34 = (float)(var25 - var31);
      float var35 = 0.005952381F;
      float[] var36 = this.shadowM;
      Arrays.fill(var36, 0.0F);
      var36[0] = var16 * var35;
      var36[1] = var17 * var35;
      var36[2] = var18 * var35;
      var36[3] = var33 * var35 + 0.5F;
      var36[4] = var20 * var35;
      var36[5] = var21 * var35;
      var36[6] = var22 * var35;
      var36[7] = var34 * var35 + 0.5F;
      float var37 = 0.0025F;
      var36[8] = var10 * var37;
      var36[9] = var11 * var37;
      var36[10] = var12 * var37;
      var36[11] = 0.5F;
      var36[15] = 1.0F;

      for (int var38 = 0; var38 < 3; var38++) {
         for (int var39 = 0; var39 < 3; var39++) {
            this.clipM[var39 * 4 + var38] = 2.0F * var36[var38 * 4 + var39];
         }

         this.clipM[12 + var38] = 2.0F * var36[var38 * 4 + 3] - 1.0F;
      }

      this.clipM[3] = this.clipM[7] = this.clipM[11] = 0.0F;
      this.clipM[15] = 1.0F;
   }

   public void beginShadow() {
      GL30.glBindFramebuffer(36160, this.shadowFbo);
      GL11.glViewport(0, 0, 2048, 2048);
      GL11.glClear(256);
      GL11.glMatrixMode(5889);
      GL11.glPushMatrix();
      GL11.glLoadMatrixf(this.clipM);
      GL11.glMatrixMode(5888);
      GL11.glPushMatrix();
      GL11.glLoadIdentity();
      GL11.glColorMask(false, false, false, false);
      GL11.glEnable(32823);
      GL11.glPolygonOffset(2.0F, 4.0F);
      GL20.glUseProgram(this.pDepth);
      this.u1i(this.pDepth, "tex", 0);
   }

   public void endShadow(int var1, int var2, boolean var3) {
      GL20.glUseProgram(0);
      GL11.glDisable(32823);
      GL11.glColorMask(true, true, true, true);
      GL11.glMatrixMode(5888);
      GL11.glPopMatrix();
      GL11.glMatrixMode(5889);
      GL11.glPopMatrix();
      GL11.glMatrixMode(5888);
      GL30.glBindFramebuffer(36160, var3 ? this.sceneFbo : 0);
      GL11.glViewport(0, 0, var1, var2);
   }

   public void useWorld(
      float[] var1,
      double var2,
      double var4,
      double var6,
      boolean var8,
      float var9,
      float var10,
      float[] var11,
      float[] var12,
      float[] var13,
      float var14,
      boolean var15,
      int var16,
      int var17,
      int var18,
      int var19,
      int var20
   ) {
      this.useWorldProg(this.pWorld, var1, var2, var4, var6, var8, var9, var10, var11, var12, var13, var14, var15, var16, var17, var18, var19, var20);
   }

   /**
    * Update 3.11: the translucent pass with the water program: the same inputs as {@link #useWorld} plus the copy of the scene. Call
    * {@link #copyScene} first. {@code near} / {@code far} are the planes of the projection (to turn the depth values back into distances).
    */
   public void useWater(
      float[] mv, double camX, double camY, double camZ, float fogNear, float fogFar, float[] fogCol, float[] skyTop, float[] skyHor, float dayF, boolean shadowOn,
      int leafTile, int grassTile, int flower0, int flower1, int waterTile, int width, int height, float near, float far
   ) {
      this.useWorldProg(this.pWater, mv, camX, camY, camZ, true, fogNear, fogFar, fogCol, skyTop, skyHor, dayF, shadowOn, leafTile, grassTile, flower0, flower1, waterTile);
      this.u1i(this.pWater, "sceneColor", 3);
      this.u1i(this.pWater, "sceneDepth", 4);
      GL20.glUniform2f(this.loc(this.pWater, "invRes"), 1.0F / width, 1.0F / height);
      GL20.glUniform2f(this.loc(this.pWater, "zNF"), near, far);
      GL13.glActiveTexture(33987);
      GL11.glBindTexture(3553, this.copyTex);
      GL13.glActiveTexture(33988);
      GL11.glBindTexture(3553, this.copyDepth);
      GL13.glActiveTexture(33984);
   }

   /** Takes the copy of the scene (colour and depth) that {@link #useWater} reads; the scene framebuffer is the draw target again afterwards. */
   public void copyScene(int width, int height) {
      GL30.glBindFramebuffer(36008, this.sceneFbo);
      GL30.glBindFramebuffer(36009, this.copyFbo);
      GL30.glBlitFramebuffer(0, 0, width, height, 0, 0, width, height, 16640, 9728);
      GL30.glBindFramebuffer(36160, this.sceneFbo);
   }

   public void stopWater() {
      GL13.glActiveTexture(33987);
      GL11.glBindTexture(3553, 0);
      GL13.glActiveTexture(33988);
      GL11.glBindTexture(3553, 0);
      GL13.glActiveTexture(33984);
      this.stopWorld();
   }

   void useWorldProg(
      int prog,
      float[] var1,
      double var2,
      double var4,
      double var6,
      boolean var8,
      float var9,
      float var10,
      float[] var11,
      float[] var12,
      float[] var13,
      float var14,
      boolean var15,
      int var16,
      int var17,
      int var18,
      int var19,
      int var20
   ) {
      GL20.glUseProgram(prog);
      this.projBuf.clear();
      GL11.glGetFloatv(2983, this.projBuf);
      this.projBuf.get(this.projM);
      this.calcView(var1);
      GL20.glUniformMatrix4fv(this.loc(prog, "view"), false, this.viewM);
      GL20.glUniformMatrix4fv(this.loc(prog, "invView"), false, this.invViewM);
      float[] var21 = new float[16];

      for (int var22 = 0; var22 < 4; var22++) {
         for (int var23 = 0; var23 < 4; var23++) {
            var21[var23 * 4 + var22] = this.shadowM[var22 * 4 + var23];
         }
      }

      GL20.glUniformMatrix4fv(this.loc(prog, "shadowMat"), false, var21);
      GL20.glUniform3f(this.loc(prog, "shOff"), (float)(var2 - this.mapX), (float)(var4 - this.mapY), (float)(var6 - this.mapZ));
      this.u1f(prog, "time", (float)(System.nanoTime() / 1.0E9 % 3600.0));
      GL20.glUniform3f(this.loc(prog, "camPos"), (float)(var2 % 100000.0), (float)var4, (float)(var6 % 100000.0));
      this.u1f(prog, "leafTile", var16);
      this.u1f(prog, "grassTile", var17);
      GL20.glUniform2f(this.loc(prog, "flowerTiles"), var18, var19);
      this.u1f(prog, "waterTile", var20);
      this.u1f(prog, "waves", 1.0F);
      this.u1i(prog, "tex", 0);
      this.u1i(prog, "lightmap", 1);
      this.u1i(prog, "shadowMap", 2);
      GL20.glUniform3f(this.loc(prog, "fogColor"), var11[0], var11[1], var11[2]);
      GL20.glUniform2f(this.loc(prog, "fogRange"), var9, var10);
      GL20.glUniform3f(this.loc(prog, "sunDir"), this.sunX, this.sunY, this.sunZ);
      float var24 = Math.max(0.0F, Math.min(1.0F, (this.sunY - 0.02F) / 0.25F)) * this.sunAtten;
      this.sunUp = var24;
      this.u1f(prog, "sunUp", var24);
      this.u1f(prog, "dayF", var14);
      this.u1f(prog, "isTrans", var8 ? 1.0F : 0.0F);
      GL20.glUniform3f(this.loc(prog, "skyTop"), var12[0], var12[1], var12[2]);
      GL20.glUniform3f(this.loc(prog, "skyHor"), var13[0], var13[1], var13[2]);
      this.u1f(prog, "shadowOn", var15 ? 1.0F : 0.0F);
      GL20.glUniform4fv(this.loc(prog, "dynL[0]"), this.dynL);
      GL20.glUniform4fv(this.loc(prog, "dynC[0]"), this.dynC);
      GL13.glActiveTexture(33986);
      GL11.glBindTexture(3553, this.shadowTex);
      GL13.glActiveTexture(33984);
   }

   /**
    * Update 3.11: the program for everything that is not terrain (see ShaderSrc.ENTITY_FS). Same lighting inputs as {@link #useWorld}: the sun,
    * the terrain's shadow map, the placed lights, the fog. {@code lm} = sky light and block light at the entity (0..1), updated per entity by
    * {@link #setEntLight}.
    */
   public void useEntity(float[] mv, double camX, double camY, double camZ, float fogNear, float fogFar, float[] fogCol, boolean shadowOn, float sky, float blk) {
      GL20.glUseProgram(this.pEntity);
      this.calcView(mv);
      GL20.glUniformMatrix4fv(this.loc(this.pEntity, "invView"), false, this.invViewM);
      float[] sm = new float[16];
      for (int i = 0; i < 4; i++) {
         for (int j = 0; j < 4; j++) {
            sm[j * 4 + i] = this.shadowM[i * 4 + j];
         }
      }

      GL20.glUniformMatrix4fv(this.loc(this.pEntity, "shadowMat"), false, sm);
      GL20.glUniform3f(this.loc(this.pEntity, "shOff"), (float)(camX - this.mapX), (float)(camY - this.mapY), (float)(camZ - this.mapZ));
      this.u1i(this.pEntity, "tex", 0);
      this.u1i(this.pEntity, "shadowMap", 2);
      GL20.glUniform3f(this.loc(this.pEntity, "fogColor"), fogCol[0], fogCol[1], fogCol[2]);
      GL20.glUniform2f(this.loc(this.pEntity, "fogRange"), fogNear, fogFar);
      GL20.glUniform3f(this.loc(this.pEntity, "sunDir"), this.sunX, this.sunY, this.sunZ);
      float up = Math.max(0.0F, Math.min(1.0F, (this.sunY - 0.02F) / 0.25F)) * this.sunAtten;
      this.u1f(this.pEntity, "sunUp", up);
      this.u1f(this.pEntity, "shadowOn", shadowOn ? 1.0F : 0.0F);
      GL20.glUniform4fv(this.loc(this.pEntity, "dynL[0]"), this.dynL);
      GL20.glUniform4fv(this.loc(this.pEntity, "dynC[0]"), this.dynC);
      GL20.glUniform2f(this.loc(this.pEntity, "entLm"), sky, blk);
      GL13.glActiveTexture(33986);
      GL11.glBindTexture(3553, this.shadowTex);
      GL13.glActiveTexture(33984);
      this.entityOn = true;
   }

   /** The light level (0..1 each) of the entity that is drawn next. */
   public void setEntLight(float sky, float blk) {
      if (this.entityOn) {
         GL20.glUniform2f(this.loc(this.pEntity, "entLm"), sky, blk);
      }
   }

   public void stopEntity() {
      this.entityOn = false;
      GL20.glUseProgram(0);
      GL13.glActiveTexture(33986);
      GL11.glBindTexture(3553, 0);
      GL13.glActiveTexture(33984);
   }

   public void stopWorld() {
      GL20.glUseProgram(0);
      GL13.glActiveTexture(33986);
      GL11.glBindTexture(3553, 0);
      GL13.glActiveTexture(33984);
   }

   public void beginScene(int var1, int var2) {
      this.sceneActive = false;
      if (var1 > 0 && var2 > 0) {
         this.resize(var1, var2);
         if (this.ok) {
            this.sceneActive = true;
            GL30.glBindFramebuffer(36160, this.sceneFbo);
            GL11.glViewport(0, 0, var1, var2);
         }
      }
   }

   void quad() {
      GL11.glBegin(7);
      GL11.glTexCoord2f(0.0F, 0.0F);
      GL11.glVertex3f(-1.0F, -1.0F, 0.0F);
      GL11.glTexCoord2f(1.0F, 0.0F);
      GL11.glVertex3f(1.0F, -1.0F, 0.0F);
      GL11.glTexCoord2f(1.0F, 1.0F);
      GL11.glVertex3f(1.0F, 1.0F, 0.0F);
      GL11.glTexCoord2f(0.0F, 1.0F);
      GL11.glVertex3f(-1.0F, 1.0F, 0.0F);
      GL11.glEnd();
   }

   public void skyMask(int var1, int var2) {
      this.rayAmt = 0.0F;
      if (this.ok && this.sceneActive && this.pRay != 0 && this.rayFbo != 0) {
         float var3 = this.sunY;
         if (!(var3 < 0.01F) && !(this.projM[11] > -0.5F)) {
            float var4 = this.viewM[0] * this.sunX + this.viewM[4] * var3 + this.viewM[8] * this.sunZ;
            float var5 = this.viewM[1] * this.sunX + this.viewM[5] * var3 + this.viewM[9] * this.sunZ;
            float var6 = this.viewM[2] * this.sunX + this.viewM[6] * var3 + this.viewM[10] * this.sunZ;
            float var7 = this.projM[3] * var4 + this.projM[7] * var5 + this.projM[11] * var6;
            if (!(var7 < 0.05F)) {
               float var8 = (this.projM[0] * var4 + this.projM[4] * var5 + this.projM[8] * var6) / var7;
               float var9 = (this.projM[1] * var4 + this.projM[5] * var5 + this.projM[9] * var6) / var7;
               float var10 = Math.max(Math.abs(var8), Math.abs(var9));
               float var11 = 1.0F - Math.max(0.0F, Math.min(1.0F, (var10 - 0.9F) / 1.0F));
               float var12 = Math.max(0.0F, Math.min(1.0F, (var3 - 0.01F) / 0.12F));
               float var13 = 1.0F - Math.max(0.0F, Math.min(1.0F, var3 * 4.0F));
               this.rayAmt = 0.2F * var12 * var11 * (1.0F + 0.7F * var13) * this.sunAtten;
               if (this.rayAmt < 0.002F) {
                  this.rayAmt = 0.0F;
               } else {
                  this.sunU = var8 * 0.5F + 0.5F;
                  this.sunV = var9 * 0.5F + 0.5F;
                  float var14 = Math.max(0.0F, Math.min(1.0F, var3 * 3.0F));
                  this.rayCol[0] = 1.0F;
                  this.rayCol[1] = 0.7F + 0.27500004F * var14;
                  this.rayCol[2] = 0.46F + 0.47F * var14;
                  int var15 = Math.max(1, var1 / 2);
                  int var16 = Math.max(1, var2 / 2);
                  GL11.glDisable(2929);
                  GL11.glDisable(3042);
                  GL11.glDisable(3008);
                  GL11.glDisable(2884);
                  GL11.glMatrixMode(5889);
                  GL11.glPushMatrix();
                  GL11.glLoadIdentity();
                  GL11.glMatrixMode(5888);
                  GL11.glPushMatrix();
                  GL11.glLoadIdentity();
                  GL30.glBindFramebuffer(36160, this.rayFbo);
                  GL11.glViewport(0, 0, var15, var16);
                  GL20.glUseProgram(this.pRay);
                  this.u1i(this.pRay, "scene", 0);
                  this.u1i(this.pRay, "depth", 1);
                  GL13.glActiveTexture(33985);
                  GL11.glBindTexture(3553, this.sceneDepth);
                  GL13.glActiveTexture(33984);
                  GL11.glEnable(3553);
                  GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                  GL11.glBindTexture(3553, this.sceneTex);
                  this.quad();
                  GL20.glUseProgram(0);
                  GL13.glActiveTexture(33985);
                  GL11.glBindTexture(3553, 0);
                  GL13.glActiveTexture(33984);
                  GL30.glBindFramebuffer(36160, this.sceneFbo);
                  GL11.glViewport(0, 0, var1, var2);
                  GL11.glMatrixMode(5889);
                  GL11.glPopMatrix();
                  GL11.glMatrixMode(5888);
                  GL11.glPopMatrix();
                  GL11.glEnable(2929);
               }
            }
         }
      }
   }

   public void endScene(int var1, int var2, float var3) {
      if (this.ok && this.sceneActive) {
         this.sceneActive = false;
         int var4 = Math.max(1, var1 / 2);
         int var5 = Math.max(1, var2 / 2);
         GL11.glDisable(2929);
         GL11.glDisable(3042);
         GL11.glDisable(3008);
         GL11.glDisable(2884);
         GL11.glDisable(2912);
         GL11.glMatrixMode(5889);
         GL11.glPushMatrix();
         GL11.glLoadIdentity();
         GL11.glMatrixMode(5888);
         GL11.glPushMatrix();
         GL11.glLoadIdentity();
         GL13.glActiveTexture(33984);
         GL11.glEnable(3553);
         GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
         GL30.glBindFramebuffer(36160, this.bloomFbo0);
         GL11.glViewport(0, 0, var4, var5);
         GL20.glUseProgram(this.pBright);
         this.u1i(this.pBright, "scene", 0);
         GL11.glBindTexture(3553, this.sceneTex);
         this.quad();

         for (int var6 = 0; var6 < 2; var6++) {
            GL30.glBindFramebuffer(36160, this.bloomFbo1);
            GL20.glUseProgram(this.pBlur);
            this.u1i(this.pBlur, "img", 0);
            GL20.glUniform2f(this.loc(this.pBlur, "dir"), 1.0F / var4, 0.0F);
            GL11.glBindTexture(3553, this.bloomTex0);
            this.quad();
            GL30.glBindFramebuffer(36160, this.bloomFbo0);
            GL20.glUniform2f(this.loc(this.pBlur, "dir"), 0.0F, 1.0F / var5);
            GL11.glBindTexture(3553, this.bloomTex1);
            this.quad();
         }

         GL30.glBindFramebuffer(36160, 0);
         GL11.glViewport(0, 0, var1, var2);
         GL20.glUseProgram(this.pComp);
         this.u1i(this.pComp, "scene", 0);
         this.u1i(this.pComp, "bloom", 1);
         this.u1f(this.pComp, "strength", var3);
         this.u1i(this.pComp, "rays", 2);
         this.u1f(this.pComp, "rayAmt", this.rayAmt);
         GL20.glUniform2f(this.loc(this.pComp, "sunUv"), this.sunU, this.sunV);
         GL20.glUniform3f(this.loc(this.pComp, "rayCol"), this.rayCol[0], this.rayCol[1], this.rayCol[2]);
         GL13.glActiveTexture(33986);
         GL11.glBindTexture(3553, this.rayTex);
         GL20.glUniform2f(this.loc(this.pComp, "invRes"), 1.0F / var1, 1.0F / var2);
         GL13.glActiveTexture(33985);
         GL11.glBindTexture(3553, this.bloomTex0);
         GL13.glActiveTexture(33984);
         GL11.glBindTexture(3553, this.sceneTex);
         this.quad();
         GL20.glUseProgram(0);
         GL13.glActiveTexture(33985);
         GL11.glBindTexture(3553, 0);
         GL13.glActiveTexture(33984);
         GL11.glMatrixMode(5889);
         GL11.glPopMatrix();
         GL11.glMatrixMode(5888);
         GL11.glPopMatrix();
         GL11.glEnable(2929);
      }
   }

   void calcView(float[] var1) {
      for (int var2 = 0; var2 < 16; var2++) {
         this.viewM[var2] = var1[var2];
      }

      this.viewM[12] = this.viewM[13] = this.viewM[14] = 0.0F;

      for (int var4 = 0; var4 < 3; var4++) {
         for (int var3 = 0; var3 < 3; var3++) {
            this.invViewM[var3 * 4 + var4] = this.viewM[var4 * 4 + var3];
         }
      }

      this.invViewM[3] = this.invViewM[7] = this.invViewM[11] = 0.0F;
      this.invViewM[12] = this.invViewM[13] = this.invViewM[14] = 0.0F;
      this.invViewM[15] = 1.0F;
   }

   public void useLod(float[] var1, double var2, double var4, double var6, float var8, float var9, float[] var10, float[] var11, float[] var12, boolean var13) {
      if (this.lodOk) {
         this.calcView(var1);
         GL20.glUseProgram(this.pLod);
         GL20.glUniformMatrix4fv(this.loc(this.pLod, "invView"), false, this.invViewM);
         GL20.glUniform3f(this.loc(this.pLod, "camPos"), (float)(var2 % 100000.0), (float)var4, (float)(var6 % 100000.0));
         this.u1i(this.pLod, "lightmap", 1);
         GL20.glUniform3f(this.loc(this.pLod, "fogColor"), var10[0], var10[1], var10[2]);
         GL20.glUniform2f(this.loc(this.pLod, "fogRange"), var8, var9);
         GL20.glUniform3f(this.loc(this.pLod, "sunDir"), this.sunX, this.sunY, this.sunZ);
         float var14 = Math.max(0.0F, Math.min(1.0F, (this.sunY - 0.02F) / 0.25F)) * this.sunAtten;
         this.u1f(this.pLod, "sunUp", var14);
         this.u1f(this.pLod, "time", (float)(System.nanoTime() / 1.0E9 % 3600.0));
         GL20.glUniform3f(this.loc(this.pLod, "skyTop"), var11[0], var11[1], var11[2]);
         GL20.glUniform3f(this.loc(this.pLod, "skyHor"), var12[0], var12[1], var12[2]);
         this.u1f(this.pLod, "fancy", var13 ? 1.0F : 0.0F);
         GL13.glActiveTexture(33985);
         GL11.glBindTexture(3553, Tex.lightmap);
         GL13.glActiveTexture(33984);
      }
   }

   public void stopLod() {
      if (this.lodOk) {
         GL20.glUseProgram(0);
      }
   }

   static float[] vnoise(int var0, int var1, long var2) {
      Random var4 = new Random(var2);
      float[] var5 = new float[var1 * var1];

      for (int var6 = 0; var6 < var5.length; var6++) {
         var5[var6] = var4.nextFloat();
      }

      float[] var21 = new float[var0 * var0];

      for (int var7 = 0; var7 < var0; var7++) {
         float var8 = (float)(var7 * var1) / var0;
         int var9 = (int)var8;
         float var10 = var8 - var9;
         var10 = var10 * var10 * (3.0F - 2.0F * var10);

         for (int var11 = 0; var11 < var0; var11++) {
            float var12 = (float)(var11 * var1) / var0;
            int var13 = (int)var12;
            float var14 = var12 - var13;
            var14 = var14 * var14 * (3.0F - 2.0F * var14);
            int var15 = (var13 + 1) % var1;
            int var16 = (var9 + 1) % var1;
            float var17 = var5[var9 * var1 + var13];
            float var18 = var5[var9 * var1 + var15];
            float var19 = var5[var16 * var1 + var13];
            float var20 = var5[var16 * var1 + var15];
            var21[var7 * var0 + var11] = (var17 + (var18 - var17) * var14) * (1.0F - var10) + (var19 + (var20 - var19) * var14) * var10;
         }
      }

      return var21;
   }

   static float[] fbm(int var0, int[] var1, float[] var2, long var3) {
      float[] var5 = new float[var0 * var0];

      for (int var6 = 0; var6 < var1.length; var6++) {
         float[] var7 = vnoise(var0, var1[var6], var3 + var6 * 7919L);

         for (int var8 = 0; var8 < var5.length; var8++) {
            var5[var8] += var7[var8] * var2[var6];
         }
      }

      float var12 = 1.0E9F;
      float var13 = -1.0E9F;

      for (float var11 : var5) {
         var12 = Math.min(var12, var11);
         var13 = Math.max(var13, var11);
      }

      for (int var15 = 0; var15 < var5.length; var15++) {
         var5[var15] = (var5[var15] - var12) / (var13 - var12);
      }

      return var5;
   }

   void makeNoise() {
      int var1 = 256;
      float[] var2 = fbm(var1, new int[]{4, 8, 16, 32}, new float[]{0.5F, 0.28F, 0.15F, 0.07F}, 1337L);
      float[] var3 = fbm(var1, new int[]{16, 32, 64}, new float[]{0.5F, 0.3F, 0.2F}, 4242L);
      ByteBuffer var4 = BufferUtils.createByteBuffer(var1 * var1 * 4);

      for (int var5 = 0; var5 < var1 * var1; var5++) {
         var4.put((byte)(var2[var5] * 255.0F)).put((byte)(var3[var5] * 255.0F)).put((byte)0).put((byte)-1);
      }

      var4.flip();
      this.noiseTex = GL11.glGenTextures();
      GL11.glBindTexture(3553, this.noiseTex);
      GL11.glTexImage2D(3553, 0, 6408, var1, var1, 0, 6408, 5121, var4);
      GL11.glTexParameteri(3553, 10241, 9729);
      GL11.glTexParameteri(3553, 10240, 9729);
      GL11.glTexParameteri(3553, 10242, 10497);
      GL11.glTexParameteri(3553, 10243, 10497);
   }

   /** The two weather maps (R = layer A, G = layer B) the cloud shader reads; the CPU samples the same bytes. */
   void makeWeatherNoise() {
      int n = Weather.NS;
      ByteBuffer buf = BufferUtils.createByteBuffer(n * n * 4);
      for (int i = 0; i < n * n; i++) {
         buf.put(Weather.MAP_A[i]).put(Weather.MAP_B[i]).put((byte)0).put((byte)-1);
      }

      buf.flip();
      this.wnoiseTex = GL11.glGenTextures();
      GL11.glBindTexture(3553, this.wnoiseTex);
      GL11.glTexImage2D(3553, 0, 6408, n, n, 0, 6408, 5121, buf);
      GL11.glTexParameteri(3553, 10241, 9729);
      GL11.glTexParameteri(3553, 10240, 9729);
      GL11.glTexParameteri(3553, 10242, 10497);
      GL11.glTexParameteri(3553, 10243, 10497);
   }

   static double mod9000(double v) {
      double m = v % 9000.0;
      return m < 0 ? m + 9000.0 : m;
   }

   void quadZ(float var1) {
      GL11.glBegin(7);
      GL11.glTexCoord2f(0.0F, 0.0F);
      GL11.glVertex3f(-1.0F, -1.0F, var1);
      GL11.glTexCoord2f(1.0F, 0.0F);
      GL11.glVertex3f(1.0F, -1.0F, var1);
      GL11.glTexCoord2f(1.0F, 1.0F);
      GL11.glVertex3f(1.0F, 1.0F, var1);
      GL11.glTexCoord2f(0.0F, 1.0F);
      GL11.glVertex3f(-1.0F, 1.0F, var1);
      GL11.glEnd();
   }

   public void renderClouds(
      float[] var1, float[] var2, double var3, double var5, double var7, float[] var9, float[] var10, float[] var11, int var12, int var13, WeatherFx fx
   ) {
      if (this.cloudsOk && this.ok && this.sceneActive && fx.snap != null) {
         this.calcView(var2);
         float var14 = Math.max(0.0F, Math.min(1.0F, (this.sunY - 0.02F) / 0.25F));
         float var15 = Math.max(0.0F, Math.min(1.0F, this.sunY * 3.0F));
         float var16 = 1.0F + 0.0F * var15;
         float var17 = 0.52F + 0.43F * var15;
         float var18 = 0.28F + 0.57000005F * var15;
         float var19 = 0.95F;
         float var20 = (var10[0] + var9[0]) * 0.5F * var19 + 0.03F;
         float var21 = (var10[1] + var9[1]) * 0.5F * var19 + 0.03F;
         float var22 = (var10[2] + var9[2]) * 0.5F * var19 + 0.04F;
         float var23 = var1[14] / (var1[10] - 1.0F);
         float var24 = var1[14] / (var1[10] + 1.0F);
         GL11.glDisable(2929);
         GL11.glDisable(3042);
         GL11.glDisable(3008);
         GL11.glDisable(2884);
         GL11.glDisable(2912);
         GL11.glDepthMask(false);
         GL11.glMatrixMode(5889);
         GL11.glPushMatrix();
         GL11.glLoadIdentity();
         GL11.glMatrixMode(5888);
         GL11.glPushMatrix();
         GL11.glLoadIdentity();
         GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
         GL30.glBindFramebuffer(36160, this.cloudFbo);
         GL11.glViewport(0, 0, this.cw, this.ch);
         GL11.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
         GL11.glClear(16384);
         GL20.glUseProgram(this.pCloud);
         this.u1i(this.pCloud, "noiseTex", 0);
         this.u1i(this.pCloud, "depthTex", 1);
         GL20.glUniformMatrix4fv(this.loc(this.pCloud, "invView"), false, this.invViewM);
         GL20.glUniform2f(this.loc(this.pCloud, "projScale"), var1[0], var1[5]);
         GL20.glUniform3f(this.loc(this.pCloud, "camPos"), (float)mod9000(var3), (float)var5, (float)mod9000(var7));
         this.u1f(this.pCloud, "time", (float)(System.nanoTime() / 1.0E9 % 3600.0));
         this.u1i(this.pCloud, "wnoise", 2);
         Weather.Snap sn = fx.snap;
         GL20.glUniform4f(this.loc(this.pCloud, "wOff"), (float)mod9000(sn.oax), (float)mod9000(sn.oaz), (float)mod9000(sn.obx), (float)mod9000(sn.obz));
         GL20.glUniform4f(this.loc(this.pCloud, "wThr"), Weather.C0, Weather.C1, Weather.T0, Weather.T1);
         GL20.glUniform2f(this.loc(this.pCloud, "wRain"), Weather.R0, Weather.R1);
         GL20.glUniform4f(this.loc(this.pCloud, "wFx"), sn.bias, sn.lo, sn.hi, sn.amt);
         float[] cells = new float[16];
         for (int var30 = 0; var30 < sn.cells.length / 4; var30++) {
            cells[var30 * 4] = (float)(sn.cells[var30 * 4] - var3);
            cells[var30 * 4 + 1] = (float)(sn.cells[var30 * 4 + 1] - var7);
            cells[var30 * 4 + 2] = sn.cells[var30 * 4 + 2];
            cells[var30 * 4 + 3] = sn.cells[var30 * 4 + 3];
         }

         GL20.glUniform4fv(this.loc(this.pCloud, "wCells[0]"), cells);
         GL20.glUniform4f(this.loc(this.pCloud, "wFlash"), fx.flashX - (float)var3, fx.flashY - (float)var5, fx.flashZ - (float)var7, fx.flashAmt);
         GL20.glUniform4f(this.loc(this.pCloud, "wOnCap"), sn.on ? 1.0F : 0.0F, Weather.FAIR, sn.calmAmt, Weather.FAIR_LO);
         GL20.glUniform3f(this.loc(this.pCloud, "sunDir"), this.sunX, this.sunY, this.sunZ);
         GL20.glUniform3f(this.loc(this.pCloud, "sunCol"), var16 * var14, var17 * var14, var18 * var14);
         GL20.glUniform3f(this.loc(this.pCloud, "ambCol"), var20, var21, var22);
         GL20.glUniform3f(this.loc(this.pCloud, "fogCol"), var11[0], var11[1], var11[2]);
         GL20.glUniform2f(this.loc(this.pCloud, "zNF"), var23, var24);
         GL20.glUniform2f(this.loc(this.pCloud, "cloudY"), fx.slabLo, fx.slabHi);
         GL13.glActiveTexture(33986);
         GL11.glBindTexture(3553, this.wnoiseTex);
         GL13.glActiveTexture(33985);
         GL11.glBindTexture(3553, this.sceneDepth);
         GL13.glActiveTexture(33984);
         GL11.glBindTexture(3553, this.noiseTex);
         this.quad();
         GL13.glActiveTexture(33986);
         GL11.glBindTexture(3553, 0);
         GL13.glActiveTexture(33984);
         GL30.glBindFramebuffer(36160, this.sceneFbo);
         GL11.glViewport(0, 0, var12, var13);
         GL13.glActiveTexture(33985);
         GL11.glBindTexture(3553, 0);
         GL13.glActiveTexture(33984);
         GL11.glBindTexture(3553, this.cloudTex);
         GL20.glUseProgram(this.pBlit);
         this.u1i(this.pBlit, "img", 0);
         GL11.glEnable(2929);
         GL11.glDepthFunc(519); // storm clouds can be in front of mountains and the camera can be inside them: composite everywhere
         GL11.glEnable(3042);
         GL11.glBlendFunc(1, 771);
         this.quadZ(0.99999F);
         GL11.glDisable(3042);
         GL11.glDepthFunc(515);
         GL11.glDepthMask(true);
         GL20.glUseProgram(0);
         GL11.glMatrixMode(5889);
         GL11.glPopMatrix();
         GL11.glMatrixMode(5888);
         GL11.glPopMatrix();
      }
   }
}
