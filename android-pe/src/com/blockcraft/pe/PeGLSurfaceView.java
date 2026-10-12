package com.blockcraft.pe;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.view.MotionEvent;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * OpenGL ES 3.0 / 2.0 SurfaceView & Renderer for Blockcraft PE on Samsung Galaxy A55 5G (120Hz).
 */
public final class PeGLSurfaceView extends GLSurfaceView implements GLSurfaceView.Renderer {
   public final PeWorld world;
   public final PeTouchController touch;
   public final PeAudio audio;
   private final PeTextures textures;
   private final PeShaders shaders;
   private final PeChunkMesher mesher;

   private final float[] projMatrix = new float[16];
   private final float[] viewMatrix = new float[16];
   private final float[] mvpMatrix = new float[16];
   private final float[] orthoMatrix = new float[16];

   private int width = 2340;
   private int height = 1080;
   private long lastFrameNs = 0L;
   private float tickAccum = 0.0F;
   private int fpsFrames = 0;
   private long fpsTimerMs = 0L;
   private int currentFps = 120;

   public PeGLSurfaceView(Context context) {
      super(context);
      setEGLContextClientVersion(2);
      setEGLConfigChooser(8, 8, 8, 8, 16, 0);
      setPreserveEGLContextOnPause(true);

      this.world = new PeWorld();
      this.world.generateWorld(context, PeWorld.MODE_BATTLE, 0);
      this.touch = new PeTouchController();
      this.audio = new PeAudio();
      this.textures = new PeTextures();
      this.shaders = new PeShaders();
      this.mesher = new PeChunkMesher();

      setRenderer(this);
      setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
   }

   @Override
   public boolean onTouchEvent(MotionEvent event) {
      this.touch.onTouchEvent(event, getContext(), this.world, this.audio);
      return true;
   }

   @Override
   public void onSurfaceCreated(GL10 gl, EGLConfig config) {
      GLES20.glEnable(GLES20.GL_DEPTH_TEST);
      GLES20.glDepthFunc(GLES20.GL_LEQUAL);
      GLES20.glEnable(GLES20.GL_CULL_FACE);
      GLES20.glCullFace(GLES20.GL_BACK);
      GLES20.glEnable(GLES20.GL_BLEND);
      GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

      this.textures.initGL(getContext());
      this.shaders.initGL();
      this.mesher.initGL(this.world);
      this.lastFrameNs = System.nanoTime();
      this.fpsTimerMs = System.currentTimeMillis();
   }

   @Override
   public void onSurfaceChanged(GL10 gl, int w, int h) {
      this.width = Math.max(1, w);
      this.height = Math.max(1, h);
      GLES20.glViewport(0, 0, this.width, this.height);
      this.touch.updateLayout(this.width, this.height);

      float aspect = (float)this.width / (float)this.height;
      Matrix.perspectiveM(this.projMatrix, 0, 72.0F, aspect, 0.12F, 185.0F);
      Matrix.orthoM(this.orthoMatrix, 0, 0.0F, this.width, this.height, 0.0F, -1.0F, 1.0F);
   }

   @Override
   public void onDrawFrame(GL10 gl) {
      long nowNs = System.nanoTime();
      float dt = (nowNs - this.lastFrameNs) / 1_000_000_000.0F;
      this.lastFrameNs = nowNs;
      if (dt < 0.0F || dt > 0.25F) dt = 0.016F;

      // FPS counter
      this.fpsFrames++;
      long nowMs = System.currentTimeMillis();
      if (nowMs - this.fpsTimerMs >= 500L) {
         this.currentFps = Math.round((this.fpsFrames * 1000.0F) / Math.max(1L, nowMs - this.fpsTimerMs));
         this.fpsFrames = 0;
         this.fpsTimerMs = nowMs;
      }

      // Apply immediate camera touch look deltas at full 120Hz display refresh rate!
      this.world.yaw += this.touch.consumeYawDelta();
      this.world.pitch = Math.max(-89.0F, Math.min(89.0F, this.world.pitch + this.touch.consumePitchDelta()));

      // Handle immediate taps (Attack/Fire & Place/Use)
      if (this.touch.attackTapped || this.touch.placeTapped) {
         this.touch.attackTapped = false;
         this.touch.placeTapped = false;
         this.world.triggerPrimaryAction(this.audio);
      }

      // Fixed 20Hz simulation ticks
      this.tickAccum += dt * 20.0F;
      int steps = 0;
      while (this.tickAccum >= 1.0F && steps < 4) {
         this.tickAccum -= 1.0F;
         steps++;
         this.world.tick(this.touch, this.audio);
      }

      // Rebuild up to 8 dirty chunk sections per frame
      this.mesher.updateDirtySections(this.world, 8);

      // Sky & Fog color based on current mode
      float skyR = 0.46F, skyG = 0.70F, skyB = 0.98F;
      if (this.world.mode == PeWorld.MODE_END_FIGHT) {
         skyR = 0.12F; skyG = 0.07F; skyB = 0.20F;
      } else if (this.world.mode == PeWorld.MODE_CASTLE_SIEGE) {
         skyR = 0.55F; skyG = 0.64F; skyB = 0.88F;
      } else if (this.world.mode == PeWorld.MODE_CS2) {
         skyR = 0.42F; skyG = 0.62F; skyB = 0.86F;
      }

      GLES20.glClearColor(skyR, skyG, skyB, 1.0F);
      GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

      // Build 3D camera View & MVP matrix matching PeWorld's raycast forward vector:
      // dx = -sin(yaw)*cos(pitch), dy = -sin(pitch), dz = cos(yaw)*cos(pitch)
      float camX = (float)this.world.px;
      float camY = (float)(this.world.py + (this.touch.sneakHeld && !this.world.flying ? 1.46 : 1.62));
      float camZ = (float)this.world.pz;
      float yawRad = (float)Math.toRadians(this.world.yaw);
      float pitchRad = (float)Math.toRadians(this.world.pitch);
      float cosP = (float)Math.cos(pitchRad);
      float lookX = (float)(-Math.sin(yawRad) * cosP);
      float lookY = (float)(-Math.sin(pitchRad));
      float lookZ = (float)(Math.cos(yawRad) * cosP);

      Matrix.setLookAtM(this.viewMatrix, 0,
         camX, camY, camZ,
         camX + lookX, camY + lookY, camZ + lookZ,
         0.0F, 1.0F, 0.0F);
      Matrix.multiplyMM(this.mvpMatrix, 0, this.projMatrix, 0, this.viewMatrix, 0);

      // 1. Render 3D World Chunks using Hardware VBOs
      GLES20.glEnable(GLES20.GL_DEPTH_TEST);
      GLES20.glEnable(GLES20.GL_CULL_FACE);
      GLES20.glUseProgram(this.shaders.prog3D);
      GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
      GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, this.textures.atlasTexId);
      GLES20.glUniform1i(this.shaders.uTex3D, 0);
      GLES20.glUniform3f(this.shaders.uFogColor, skyR, skyG, skyB);
      GLES20.glUniform2f(this.shaders.uFogRange, 52.0F, 118.0F);

      this.mesher.drawWorldChunks(this.shaders, this.world, this.mvpMatrix, camX, camY, camZ,
         (float)(-Math.sin(yawRad)), (float)(Math.cos(yawRad)));

      // 2. Render 3D Dynamic Entities (Mobs, AI Bots, Projectiles, Target Block Highlight & Crack Overlay, 1st-Person Hand)
      GLES20.glDisable(GLES20.GL_CULL_FACE);
      this.mesher.beginDynamic3D();
      for (int i = 0; i < this.world.mobs.size(); i++) {
         PeWorld.PeMob m = this.world.mobs.get(i);
         if (m != null && !m.dead) {
            this.mesher.addMobModel(m);
         }
      }
      for (int i = 0; i < this.world.projectiles.size(); i++) {
         PeWorld.PeProjectile pr = this.world.projectiles.get(i);
         if (pr == null || pr.dead) continue;
         float px = (float)pr.x;
         float py = (float)pr.y;
         float pz = (float)pr.z;
         int tile = pr.type == 3 ? PeTextures.T_IRON_BLOCK : (pr.type == 2 ? PeTextures.T_SNOW : PeTextures.T_GLOWSTONE);
         float r = pr.type == 3 ? 0.35F : 1.0F;
         this.mesher.addBox3D(px - 0.14F, py - 0.14F, pz - 0.14F, px + 0.14F, py + 0.14F, pz + 0.14F, tile, r, 0.9F, 0.4F, 1.0F);
      }

      // Target Block Selection Box & Mining Crack Stage
      int[] rayHit = this.world.raycastBlock(5.0);
      if (rayHit != null) {
         float bx = rayHit[0];
         float by = rayHit[1];
         float bz = rayHit[2];
         int crackStage = Math.min(9, (int)(this.world.mineProgress * 10.0F));
         if (this.world.mineProgress > 0.02F) {
            this.mesher.addBox3D(bx - 0.006F, by - 0.006F, bz - 0.006F, bx + 1.006F, by + 1.006F, bz + 1.006F,
               PeTextures.T_CRACK_0 + crackStage, 1.0F, 1.0F, 1.0F, 0.85F);
         } else {
            this.mesher.addBox3D(bx - 0.005F, by - 0.005F, bz - 0.005F, bx + 1.005F, by + 1.005F, bz + 1.005F,
               PeTextures.T_GLASS, 1.0F, 1.0F, 1.0F, 0.28F);
         }
      }

      // First-Person Held Item / Block in bottom-right of camera view
      float rightX = (float)(-Math.cos(yawRad));
      float rightZ = (float)(-Math.sin(yawRad));
      float sw = this.world.swingAnim >= 0.0F ? (float)Math.sin(this.world.swingAnim * Math.PI) * 0.22F : 0.0F;
      float hx = camX + lookX * (0.58F - sw * 0.4F) + rightX * 0.36F;
      float hy = camY + lookY * 0.58F - 0.28F + sw * 0.15F;
      float hz = camZ + lookZ * (0.58F - sw * 0.4F) + rightZ * 0.36F;
      int heldId = this.world.hotbarId[this.world.selectedSlot];
      int heldTile = PeWorld.getItemTile(heldId);
      this.mesher.addBox3D(hx - 0.10F, hy - 0.10F, hz - 0.10F, hx + 0.10F, hy + 0.10F, hz + 0.10F, heldTile, 1.0F, 1.0F, 1.0F, 1.0F);
      this.mesher.flushDynamic3D(this.shaders);

      // 3. Render 2D Pocket Edition Touch UI & HUD
      GLES20.glDisable(GLES20.GL_DEPTH_TEST);
      renderTouchHud();
   }

   private void renderTouchHud() {
      float scale = this.height / 1080.0F;

      // Pass A: Colored Panels, Buttons, Bars, Crosshair & D-Pad (using whiteTexId)
      this.mesher.begin2D();

      // Hurt red vignette flash
      if (this.world.hurtTime > 0) {
         int alpha = Math.min(110, this.world.hurtTime * 12);
         this.mesher.addRect2D(0, 0, this.width, this.height, (alpha << 24) | 0xFF2010);
      }

      // Top Action Bar buttons
      float bx = this.touch.safeMarginX;
      float by = 14.0F * scale;
      float bh = 64.0F * scale;
      float bw = 310.0F * scale;
      float gap = 16.0F * scale;
      drawBevelButton(bx, by, bw, bh, this.touch.showModeMenu ? 0xD82E6B34 : 0xB822262E);
      bx += bw + gap;
      drawBevelButton(bx, by, bw, bh, 0xB822262E);
      bx += bw + gap;
      drawBevelButton(bx, by, 240.0F * scale, bh, 0xB82B3A4A);
      bx += 240.0F * scale + gap;
      drawBevelButton(bx, by, 230.0F * scale, bh, 0xB822262E);

      // Crosshair + Animated Hitmarker at screen center
      float cx = this.width * 0.5F;
      float cy = this.height * 0.5F;
      float chLen = 18.0F * scale;
      float chThick = 3.0F * scale;
      this.mesher.addRect2D(cx - chLen, cy - chThick * 0.5F, chLen * 2.0F, chThick, 0xE0FFFFFF);
      this.mesher.addRect2D(cx - chThick * 0.5F, cy - chLen, chThick, chLen * 2.0F, 0xE0FFFFFF);
      if (this.world.hitmarkerTicks > 0) {
         float hm = 14.0F * scale;
         int hmCol = this.world.hitmarkerCrit ? 0xFFFF3838 : 0xFFFFFFFF;
         this.mesher.addRect2D(cx - hm, cy - hm, 7.0F * scale, 4.0F * scale, hmCol);
         this.mesher.addRect2D(cx + hm - 7.0F * scale, cy - hm, 7.0F * scale, 4.0F * scale, hmCol);
         this.mesher.addRect2D(cx - hm, cy + hm - 4.0F * scale, 7.0F * scale, 4.0F * scale, hmCol);
         this.mesher.addRect2D(cx + hm - 7.0F * scale, cy + hm - 4.0F * scale, 7.0F * scale, 4.0F * scale, hmCol);
      }

      // Touch-Circle Mining Progress Indicator
      if (this.touch.touchMiningActive || this.world.mineProgress > 0.02F) {
         float tx = this.touch.touchMiningActive ? this.touch.touchCircleX : cx;
         float ty = this.touch.touchMiningActive ? this.touch.touchCircleY : cy;
         float r = 64.0F * scale;
         this.mesher.addRect2D(tx - r, ty - r, r * 2.0F, 5.0F * scale, 0x90FFFFFF);
         this.mesher.addRect2D(tx - r, ty + r - 5.0F * scale, r * 2.0F, 5.0F * scale, 0x90FFFFFF);
         this.mesher.addRect2D(tx - r, ty - r, 5.0F * scale, r * 2.0F, 0x90FFFFFF);
         this.mesher.addRect2D(tx + r - 5.0F * scale, ty - r, 5.0F * scale, r * 2.0F, 0x90FFFFFF);
         float fill = r * 2.0F * this.world.mineProgress;
         this.mesher.addRect2D(tx - r, ty + r - fill, r * 2.0F, fill, 0x6040FF60);
      }

      // Left D-Pad or Virtual Analog Stick
      float dcx = this.touch.dpadCenterX;
      float dcy = this.touch.dpadCenterY;
      float dr = this.touch.dpadRadius;
      if (!this.touch.useAnalogStick) {
         float btnS = dr * 0.58F;
         drawBevelButton(dcx - btnS * 0.5F, dcy - dr, btnS, btnS, this.touch.moveForward > 0.1F ? 0xB070A8FF : 0x7830343C);
         drawBevelButton(dcx - btnS * 0.5F, dcy + dr - btnS, btnS, btnS, this.touch.moveForward < -0.1F ? 0xB070A8FF : 0x7830343C);
         drawBevelButton(dcx - dr, dcy - btnS * 0.5F, btnS, btnS, this.touch.moveStrafe < -0.1F ? 0xB070A8FF : 0x7830343C);
         drawBevelButton(dcx + dr - btnS, dcy - btnS * 0.5F, btnS, btnS, this.touch.moveStrafe > 0.1F ? 0xB070A8FF : 0x7830343C);
         drawBevelButton(dcx - btnS * 0.44F, dcy - btnS * 0.44F, btnS * 0.88F, btnS * 0.88F, this.touch.sneakHeld ? 0xB0FFB040 : 0x68242830);
      } else {
         drawBevelButton(dcx - dr * 0.85F, dcy - dr * 0.85F, dr * 1.7F, dr * 1.7F, 0x58242830);
         float knobX = dcx + this.touch.moveStrafe * dr * 0.55F;
         float knobY = dcy - this.touch.moveForward * dr * 0.55F;
         float kr = 48.0F * scale;
         drawBevelButton(knobX - kr, knobY - kr, kr * 2.0F, kr * 2.0F, 0xC070A8FF);
      }

      // Right Action Buttons: JUMP/FLY, ATK/FIRE, USE/PLACE
      float jr = this.touch.jumpBtnR;
      drawBevelButton(this.touch.jumpBtnX - jr, this.touch.jumpBtnY - jr, jr * 2.0F, jr * 2.0F,
         this.touch.jumpHeld ? 0xC050C878 : 0x88283442);
      float ar = this.touch.atkBtnR;
      drawBevelButton(this.touch.atkBtnX - ar, this.touch.atkBtnY - ar, ar * 2.0F, ar * 2.0F,
         this.touch.attackHeld ? 0xC0FF5252 : 0x88422828);
      float ur = this.touch.useBtnR;
      drawBevelButton(this.touch.useBtnX - ur, this.touch.useBtnY - ur, ur * 2.0F, ur * 2.0F, 0x88284234);

      // Bottom 9-Slot Touch Hotbar + [...] Palette Button
      float hs = this.touch.hotbarSlotSize;
      float hx = this.touch.hotbarX;
      float hy = this.touch.hotbarY;
      this.mesher.addRect2D(hx - 6.0F * scale, hy - 6.0F * scale, hs * 10.0F + 12.0F * scale, hs + 12.0F * scale, 0xB8181A20);
      for (int s = 0; s < 10; s++) {
         float sx = hx + s * hs;
         boolean sel = (s == this.world.selectedSlot);
         int bg = (s == 9) ? 0xC0324054 : (sel ? 0xE04E7A3E : 0xA02C3038);
         drawBevelButton(sx + 3.0F * scale, hy + 3.0F * scale, hs - 6.0F * scale, hs - 6.0F * scale, bg);
      }

      // Modal Menus Background (if open)
      if (this.touch.showModeMenu) {
         float pw = 1160.0F * scale;
         float ph = 740.0F * scale;
         float px = (this.width - pw) * 0.5F;
         float py = (this.height - ph) * 0.5F;
         drawBevelButton(px, py, pw, ph, 0xEE181C24);
         float colW = (pw - 72.0F * scale) * 0.5F;
         float rowH = 124.0F * scale;
         float startY = py + 120.0F * scale;
         for (int m = 0; m < PeWorld.MODE_NAMES.length; m++) {
            int col = m & 1;
            int row = m >> 1;
            float mbx = px + 24.0F * scale + col * (colW + 24.0F * scale);
            float mby = startY + row * (rowH + 18.0F * scale);
            int color = (m == this.world.mode) ? 0xFF347844 : 0xFF283240;
            drawBevelButton(mbx, mby, colW, rowH, color);
         }
      } else if (this.touch.showPaletteMenu) {
         float pw = 1080.0F * scale;
         float ph = 660.0F * scale;
         float px = (this.width - pw) * 0.5F;
         float py = (this.height - ph) * 0.5F;
         drawBevelButton(px, py, pw, ph, 0xEE181C24);
         int[] pal = PeTouchController.getPaletteItems();
         int cols = 7;
         float cell = 128.0F * scale;
         float startX = px + (pw - cols * cell) * 0.5F;
         float startY = py + 110.0F * scale;
         for (int i = 0; i < pal.length; i++) {
            int c = i % cols;
            int r = i / cols;
            drawBevelButton(startX + c * cell + 6.0F * scale, startY + r * cell + 6.0F * scale,
               cell - 12.0F * scale, cell - 12.0F * scale, 0xFF2C3442);
         }
      }

      this.mesher.flush2D(this.shaders, this.textures.whiteTexId, this.orthoMatrix);

      // Pass B: Atlas Icons (Hearts, Hotbar Item Icons, Palette Icons)
      this.mesher.begin2D();
      float heartY = 92.0F * scale;
      float heartX = this.touch.safeMarginX;
      float heartS = 30.0F * scale;
      int hearts = (this.world.health + 1) / 2;
      for (int i = 0; i < 10; i++) {
         int tile = (i < hearts) ? PeTextures.T_HEART_FULL : PeTextures.T_HEART_EMPTY;
         this.mesher.addTile2D(heartX + i * (heartS + 4.0F * scale), heartY, heartS, heartS, tile, 0xFFFFFFFF);
      }

      for (int s = 0; s < 9; s++) {
         int id = this.world.hotbarId[s];
         if (id != PeWorld.B_AIR) {
            int tile = PeWorld.getItemTile(id);
            float sx = hx + s * hs + 14.0F * scale;
            float sy = hy + 14.0F * scale;
            this.mesher.addTile2D(sx, sy, hs - 28.0F * scale, hs - 28.0F * scale, tile, 0xFFFFFFFF);
         }
      }

      if (this.touch.showPaletteMenu) {
         float pw = 1080.0F * scale;
         float ph = 660.0F * scale;
         float px = (this.width - pw) * 0.5F;
         float py = (this.height - ph) * 0.5F;
         int[] pal = PeTouchController.getPaletteItems();
         int cols = 7;
         float cell = 128.0F * scale;
         float startX = px + (pw - cols * cell) * 0.5F;
         float startY = py + 110.0F * scale;
         for (int i = 0; i < pal.length; i++) {
            int c = i % cols;
            int r = i / cols;
            int tile = PeWorld.getItemTile(pal[i]);
            this.mesher.addTile2D(startX + c * cell + 22.0F * scale, startY + r * cell + 22.0F * scale,
               cell - 44.0F * scale, cell - 44.0F * scale, tile, 0xFFFFFFFF);
         }
      }
      this.mesher.flush2D(this.shaders, this.textures.atlasTexId, this.orthoMatrix);

      // Pass C: Bitmap Font Text Labels (using fontTexId)
      this.mesher.begin2D();
      float tScale = 2.5F * scale;
      float bxText = this.touch.safeMarginX + 16.0F * scale;
      float byText = 34.0F * scale;
      this.mesher.addText2D(this.textures, "MODE: " + PeWorld.MODE_NAMES[this.world.mode], bxText, byText, tScale, 0xFFFFFFFF);
      bxText += bw + gap;
      this.mesher.addText2D(this.textures, "MAP: " + this.world.currentMapName(), bxText, byText, tScale, 0xFFFFE070);
      bxText += bw + gap;
      this.mesher.addText2D(this.textures, "NEW ROUND", bxText, byText, tScale, 0xFF80FF98);
      bxText += 240.0F * scale + gap;
      this.mesher.addText2D(this.textures, this.touch.useAnalogStick ? "CTRL: STICK" : "CTRL: D-PAD", bxText, byText, tScale, 0xFF90D8FF);

      // Top-Right Device & Match Status Readout
      String fpsLine = "BLOCKCRAFT PE v1.0 | A55 5G " + this.currentFps + " FPS";
      this.mesher.addText2D(this.textures, fpsLine, this.width - this.touch.safeMarginX - 580.0F * scale, 26.0F * scale, 2.3F * scale, 0xFFE0F4FF);
      String status = (this.world.bannerTicks > 0 && this.world.bannerMessage != null && !this.world.bannerMessage.isEmpty())
         ? this.world.bannerMessage
         : (PeWorld.MODE_NAMES[this.world.mode] + " - " + this.world.currentMapName());
      this.mesher.addText2D(this.textures, status + " | KILLS: " + this.world.kills,
         this.width - this.touch.safeMarginX - 580.0F * scale, 58.0F * scale, 2.3F * scale, 0xFFFFD868);

      // Action Button Labels
      this.mesher.addText2D(this.textures, this.world.flying ? "FLY" : "JUMP",
         this.touch.jumpBtnX - 36.0F * scale, this.touch.jumpBtnY - 10.0F * scale, 2.6F * scale, 0xFFFFFFFF);
      this.mesher.addText2D(this.textures, "ATK",
         this.touch.atkBtnX - 28.0F * scale, this.touch.atkBtnY - 10.0F * scale, 2.6F * scale, 0xFFFFD0D0);
      this.mesher.addText2D(this.textures, "USE",
         this.touch.useBtnX - 28.0F * scale, this.touch.useBtnY - 10.0F * scale, 2.5F * scale, 0xFFD0FFD8);

      // Selected Hotbar Item Name Banner + Stack Counts
      int selItem = this.world.hotbarId[this.world.selectedSlot];
      this.mesher.addText2D(this.textures, PeWorld.getItemName(selItem),
         this.touch.hotbarX, this.touch.hotbarY - 34.0F * scale, 2.6F * scale, 0xFFFFFFFF);
      for (int s = 0; s < 9; s++) {
         if (this.world.hotbarId[s] != PeWorld.B_AIR && this.world.hotbarCount[s] > 1) {
            this.mesher.addText2D(this.textures, String.valueOf(this.world.hotbarCount[s]),
               hx + s * hs + hs - 36.0F * scale, hy + hs - 26.0F * scale, 2.0F * scale, 0xFFFFFFFF);
         }
      }
      this.mesher.addText2D(this.textures, "...", hx + 9 * hs + 24.0F * scale, hy + 28.0F * scale, 3.2F * scale, 0xFFFFFFFF);

      // Modal Text
      if (this.touch.showModeMenu) {
         float pw = 1160.0F * scale;
         float ph = 740.0F * scale;
         float px = (this.width - pw) * 0.5F;
         float py = (this.height - ph) * 0.5F;
         this.mesher.addText2D(this.textures, "SELECT GAMEMODE (BLOCKCRAFT PE)", px + 36.0F * scale, py + 38.0F * scale, 3.4F * scale, 0xFFFFE878);
         float colW = (pw - 72.0F * scale) * 0.5F;
         float rowH = 124.0F * scale;
         float startY = py + 120.0F * scale;
         for (int m = 0; m < PeWorld.MODE_NAMES.length; m++) {
            int col = m & 1;
            int row = m >> 1;
            float mbx = px + 24.0F * scale + col * (colW + 24.0F * scale);
            float mby = startY + row * (rowH + 18.0F * scale);
            this.mesher.addText2D(this.textures, PeWorld.MODE_NAMES[m], mbx + 28.0F * scale, mby + 46.0F * scale, 3.0F * scale, 0xFFFFFFFF);
         }
      } else if (this.touch.showPaletteMenu) {
         float pw = 1080.0F * scale;
         float ph = 660.0F * scale;
         float px = (this.width - pw) * 0.5F;
         float py = (this.height - ph) * 0.5F;
         this.mesher.addText2D(this.textures, "TAP BLOCK OR WEAPON TO EQUIP", px + 36.0F * scale, py + 38.0F * scale, 3.2F * scale, 0xFFFFE878);
      }

      this.mesher.flush2D(this.shaders, this.textures.fontTexId, this.orthoMatrix);
   }

   private void drawBevelButton(float x, float y, float w, float h, int bgArgb) {
      this.mesher.addRect2D(x, y, w, h, bgArgb);
      this.mesher.addRect2D(x, y, w, 3.0F, 0x50FFFFFF);
      this.mesher.addRect2D(x, y + h - 3.0F, w, 3.0F, 0x70000000);
   }
}
