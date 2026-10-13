package com.blockcraft.pe;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.view.MotionEvent;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * OpenGL ES 3.0 / 2.0 SurfaceView & Renderer for Blockcraft PE v1.1 (Infinite Worlds)
 * on Samsung Galaxy A55 5G (120Hz Super AMOLED 2340x1080).
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
   private float panoramaYaw = 0.0F;

   public PeGLSurfaceView(Context context) {
      super(context);
      setEGLContextClientVersion(2);
      setEGLConfigChooser(8, 8, 8, 8, 16, 0);
      setPreserveEGLContextOnPause(true);

      this.world = new PeWorld();
      this.world.refreshSaveSlots(context);
      // Generate a preview Overworld for the 3D Title Screen panorama (without overwriting save slots)
      this.world.generateWorld(null, PeWorld.MODE_SURVIVAL, PeWorld.WTYPE_LARGE_BIOMES);
      this.world.screen = PeWorld.SCREEN_TITLE;

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
      Matrix.perspectiveM(this.projMatrix, 0, 72.0F, aspect, 0.12F, 210.0F);
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

      if (this.world.screen == PeWorld.SCREEN_PLAY) {
         // Apply camera touch look deltas at full 120Hz display refresh rate
         this.world.yaw += this.touch.consumeYawDelta();
         this.world.pitch = Math.max(-89.0F, Math.min(89.0F, this.world.pitch + this.touch.consumePitchDelta()));

         // Handle dedicated button taps (MINE vs PLACE/USE)
         if (this.touch.attackTapped) {
            this.touch.attackTapped = false;
            this.world.triggerAttackAction(this.audio);
         }
         if (this.touch.placeTapped) {
            this.touch.placeTapped = false;
            this.world.triggerPlaceOrUseAction(this.audio);
         }

         // Fixed 20Hz simulation ticks
         this.tickAccum += dt * 20.0F;
         int steps = 0;
         while (this.tickAccum >= 1.0F && steps < 4) {
            this.tickAccum -= 1.0F;
            steps++;
            this.world.tick(this.touch, this.audio);
         }
      } else {
         this.touch.consumeYawDelta();
         this.touch.consumePitchDelta();
         this.panoramaYaw = (this.panoramaYaw + dt * 9.0F) % 360.0F;
      }

      // Animate Water & Lava tiles on the GPU atlas
      this.textures.updateFluidAnimations((int)this.world.tickCount);

      // Rebuild dirty chunk sections with a smooth frame budget
      this.mesher.updateDirtySections(this.world, this.world.screen == PeWorld.SCREEN_PLAY ? 4 : 12);

      // PC Update 3.5+ Day/Night Sky & Sunrise/Sunset Twilight Horizon Glow
      float sun = (float)Math.sin(this.world.dayTime * Math.PI * 2.0);
      float dayBright = Math.max(0.22F, Math.min(1.0F, 0.64F + sun * 0.44F));
      float twilight = Math.max(0.0F, 1.0F - Math.abs(sun) * 3.2F);
      float skyR = 0.40F * dayBright + twilight * 0.36F;
      float skyG = 0.68F * dayBright + twilight * 0.12F;
      float skyB = 0.98F * dayBright - twilight * 0.14F;

      GLES20.glClearColor(skyR, skyG, skyB, 1.0F);
      GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

      // 120Hz Sub-Tick Camera & View-Bobbing Interpolation (eliminates 20Hz walking jitter!)
      boolean isMenuPanorama = (this.world.screen == PeWorld.SCREEN_TITLE
         || this.world.screen == PeWorld.SCREEN_WORLD_SELECT
         || this.world.screen == PeWorld.SCREEN_CREATE_WORLD);
      float partial = (this.world.screen == PeWorld.SCREEN_PLAY)
         ? Math.max(0.0F, Math.min(1.0F, this.tickAccum)) : 1.0F;
      float interpX = (float)(this.world.prevPx + (this.world.px - this.world.prevPx) * partial);
      float interpY = (float)(this.world.prevPy + (this.world.py - this.world.prevPy) * partial);
      float interpZ = (float)(this.world.prevPz + (this.world.pz - this.world.prevPz) * partial);
      float interpEye = this.world.prevEyeHeight + (this.world.eyeHeight - this.world.prevEyeHeight) * partial;
      float interpWalk = this.world.prevWalkDist + (this.world.walkDist - this.world.prevWalkDist) * partial;

      float useYaw = isMenuPanorama ? this.panoramaYaw : this.world.yaw;
      float usePitch = isMenuPanorama ? 11.0F : this.world.pitch;
      float yawRad = (float)Math.toRadians(useYaw);
      float pitchRad = (float)Math.toRadians(usePitch);
      float cosP = (float)Math.cos(pitchRad);
      float lookX = (float)(Math.sin(yawRad) * cosP);
      float lookY = (float)(-Math.sin(pitchRad));
      float lookZ = (float)(-Math.cos(yawRad) * cosP);
      float rightX = (float)Math.cos(yawRad);
      float rightZ = (float)Math.sin(yawRad);

      // Smooth 120Hz View Bobbing while walking on solid ground
      float walkBobY = (!isMenuPanorama && this.world.onGround && !this.world.flying)
         ? (float)(Math.sin(interpWalk * 3.0F) * 0.028F) : 0.0F;
      float walkBobSide = (!isMenuPanorama && this.world.onGround && !this.world.flying)
         ? (float)(Math.cos(interpWalk * 1.5F) * 0.020F) : 0.0F;

      float camX = isMenuPanorama ? 0.5F : (interpX + rightX * walkBobSide);
      float camY = isMenuPanorama ? 74.0F : (interpY + interpEye + walkBobY);
      float camZ = isMenuPanorama ? 0.5F : (interpZ + rightZ * walkBobSide);

      Matrix.setLookAtM(this.viewMatrix, 0,
         camX, camY, camZ,
         camX + lookX, camY + lookY, camZ + lookZ,
         0.0F, 1.0F, 0.0F);
      Matrix.multiplyMM(this.mvpMatrix, 0, this.projMatrix, 0, this.viewMatrix, 0);

      // 1. Render Infinite 3D World Chunks using Hardware VBOs
      GLES20.glEnable(GLES20.GL_DEPTH_TEST);
      GLES20.glEnable(GLES20.GL_CULL_FACE);
      GLES20.glUseProgram(this.shaders.prog3D);
      GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
      GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, this.textures.atlasTexId);
      GLES20.glUniform1i(this.shaders.uTex3D, 0);
      GLES20.glUniform3f(this.shaders.uFogColor, skyR, skyG, skyB);
      float fogEnd = (this.world.viewRadiusChunks + 0.5F) * 16.0F;
      GLES20.glUniform2f(this.shaders.uFogRange, fogEnd * 0.52F, fogEnd);
      GLES20.glUniform4f(this.shaders.uTint, dayBright, dayBright, dayBright, 1.0F);

      this.mesher.drawWorldChunks(this.shaders, this.world, this.mvpMatrix, camX, camY, camZ,
         (float)Math.sin(yawRad), (float)(-Math.cos(yawRad)));

      // 2. Render 3D Sky (Orbiting Sun, Moon, Stars & Drifting 3D Clouds) + Falling Trees + Selection Box + 3D Hand/Tools
      GLES20.glDisable(GLES20.GL_CULL_FACE);
      GLES20.glUniform4f(this.shaders.uTint, 1.0F, 1.0F, 1.0F, 1.0F);
      GLES20.glUniform2f(this.shaders.uFogRange, fogEnd * 0.90F, fogEnd * 1.45F);
      this.mesher.beginDynamic3D();
      this.mesher.addSkyAndClouds3D(this.world, camX, camY, camZ, dayBright);
      this.mesher.addFallingTrees3D(this.world, partial, dayBright);

      int[] rayHit = null;
      if (!isMenuPanorama) {
         rayHit = this.world.raycastBlock(5.0);
         if (rayHit != null) {
            float bx = rayHit[0];
            float by = rayHit[1];
            float bz = rayHit[2];
            float bh = Math.max(0.5F, (float)PeWorld.getBlockHeight((byte)rayHit[6]));
            int crackStage = Math.min(9, (int)(this.world.mineProgress * 10.0F));
            if (this.world.mineProgress > 0.02F) {
               this.mesher.addBox3D(bx - 0.006F, by - 0.006F, bz - 0.006F, bx + 1.006F, by + bh + 0.006F, bz + 1.006F,
                  PeTextures.T_CRACK_0 + crackStage, 1.0F, 1.0F, 1.0F, 0.88F);
            } else {
               this.mesher.addBox3D(bx - 0.005F, by - 0.005F, bz - 0.005F, bx + 1.005F, by + bh + 0.005F, bz + 1.005F,
                  PeTextures.T_GLASS, 1.0F, 1.0F, 1.0F, 0.22F);
            }
         }

         // First-Person 3D Held Tool / Weapon / Torch / 6-Face 3D Block with smooth 120Hz walk bobbing & swing arc
         float bob = (float)Math.sin(interpWalk * 2.4F) * 0.018F;
         int heldId = this.world.hotbarId[this.world.selectedSlot];
         this.mesher.addFirstPersonHeld3D(heldId, camX, camY, camZ, useYaw, usePitch,
            this.world.swingAnim, bob, dayBright);
      }
      this.mesher.flushDynamic3D(this.shaders);

      // 3. Render 2D Mobile UI / HUD
      GLES20.glDisable(GLES20.GL_DEPTH_TEST);
      if (this.world.screen == PeWorld.SCREEN_PLAY) {
         renderPlayHud(rayHit);
      } else {
         renderMenuScreen();
      }
   }

   // ====================================================================
   //  Menu Screens (Title, World Select, Create World, Pause, Settings, Inventory/Crafting)
   // ====================================================================
   private void renderMenuScreen() {
      float scale = this.height / 1080.0F;
      float cx = this.width * 0.5F;

      if (this.world.screen == PeWorld.SCREEN_INVENTORY) {
         renderInventoryAndCraftingScreen(scale);
         return;
      }

      // Pass A: Dark-Glass Cards, Frames & Tactile Buttons
      this.mesher.begin2D();
      this.mesher.addRect2D(0, 0, this.width, this.height, 0x800A0E16);

      if (this.world.screen == PeWorld.SCREEN_TITLE) {
         // Hero Title Card
         this.mesher.drawCard2D(cx - 600.0F * scale, 105.0F * scale, 1200.0F * scale, 275.0F * scale, 0xE6141A24, 0xFF3E5674);
         // Subtitle Pill inside Hero Card
         this.mesher.addRect2D(cx - 420.0F * scale, 292.0F * scale, 840.0F * scale, 54.0F * scale, 0xD81E3428);

         float bw = 820.0F * scale;
         float bh = 114.0F * scale;
         float bx = cx - bw * 0.5F;
         this.mesher.drawButton2D(bx, 470.0F * scale, bw, bh, 0xFF286C3E, 0xFF54E882, true);
         this.mesher.drawButton2D(bx, 610.0F * scale, bw, bh, 0xFF285848, 0xFF54E882, true);
         this.mesher.drawButton2D(bx, 750.0F * scale, bw, bh, 0xFF243448, 0xFF5CA8FF, false);

         // Bottom Footer Pill
         this.mesher.drawCard2D(cx - 520.0F * scale, 932.0F * scale, 1040.0F * scale, 72.0F * scale, 0xCC121620, 0xFF2A3648);
      } else if (this.world.screen == PeWorld.SCREEN_WORLD_SELECT) {
         float pw = 1540.0F * scale;
         float px = cx - pw * 0.5F;
         this.mesher.drawCard2D(px, 82.0F * scale, pw, 880.0F * scale, 0xF0121620, 0xFF384C68);
         this.mesher.addRect2D(px + 20.0F * scale, 98.0F * scale, pw - 40.0F * scale, 92.0F * scale, 0xD01A2230);

         for (int s = 0; s < 3; s++) {
            float rowY = (232.0F + s * 186.0F) * scale;
            float rowH = 158.0F * scale;
            boolean exists = this.world.slotExists[s];
            int cardBg = exists ? 0xFF1E3E2C : 0xFF1E2634;
            int cardAccent = exists ? 0xFF4CE07A : 0xFF5288C8;
            this.mesher.drawButton2D(px + 40.0F * scale, rowY, pw - 360.0F * scale, rowH, cardBg, cardAccent, exists);
            // Slot icon frame on the left of each world card
            this.mesher.drawSlot2D(px + 66.0F * scale, rowY + 21.0F * scale, 116.0F * scale, exists, 0);

            int actBg = exists ? 0xFF682626 : 0xFF28683C;
            int actAccent = exists ? 0xFFFF6464 : 0xFF54E882;
            this.mesher.drawButton2D(px + pw - 300.0F * scale, rowY, 260.0F * scale, rowH, actBg, actAccent, false);
         }
         this.mesher.drawButton2D(cx - 340.0F * scale, 822.0F * scale, 680.0F * scale, 102.0F * scale, 0xFF283446, 0xFF70A8FF, false);
      } else if (this.world.screen == PeWorld.SCREEN_CREATE_WORLD) {
         float pw = 1180.0F * scale;
         float px = cx - pw * 0.5F;
         this.mesher.drawCard2D(px, 96.0F * scale, pw, 800.0F * scale, 0xF0121620, 0xFF3A5270);
         this.mesher.addRect2D(px + 20.0F * scale, 112.0F * scale, pw - 40.0F * scale, 90.0F * scale, 0xD01A2230);

         float bw = 1020.0F * scale;
         float bx = cx - bw * 0.5F;
         float bh = 114.0F * scale;
         this.mesher.drawButton2D(bx, 255.0F * scale, bw, bh, 0xFF223246, 0xFF5CA8FF, false);
         this.mesher.drawButton2D(bx, 395.0F * scale, bw, bh, 0xFF1E3E2E, 0xFF52E882, true);
         this.mesher.drawButton2D(bx, 535.0F * scale, bw, bh, 0xFF2C3224, 0xFFFFD858, false);
         this.mesher.drawButton2D(bx, 720.0F * scale, 640.0F * scale, 120.0F * scale, 0xFF286C3E, 0xFF54E882, true);
         this.mesher.drawButton2D(bx + 664.0F * scale, 720.0F * scale, 356.0F * scale, 120.0F * scale, 0xFF5A2828, 0xFFFF6868, false);
      } else if (this.world.screen == PeWorld.SCREEN_PAUSE) {
         float pw = 980.0F * scale;
         float px = cx - pw * 0.5F;
         this.mesher.drawCard2D(px, 92.0F * scale, pw, 806.0F * scale, 0xF0121620, 0xFF3A5270);
         this.mesher.addRect2D(px + 20.0F * scale, 108.0F * scale, pw - 40.0F * scale, 84.0F * scale, 0xD01A2230);

         float bw = 860.0F * scale;
         float bx = cx - bw * 0.5F;
         float bh = 106.0F * scale;
         this.mesher.drawButton2D(bx, 218.0F * scale, bw, bh, 0xFF286C3E, 0xFF54E882, true);
         this.mesher.drawButton2D(bx, 348.0F * scale, bw, bh, 0xFF223448, 0xFF5CA8FF, false);
         this.mesher.drawButton2D(bx, 478.0F * scale, bw, bh, 0xFF223448, 0xFF5CA8FF, false);
         this.mesher.drawButton2D(bx, 608.0F * scale, bw, bh, 0xFF224C38, 0xFF54E882, false);
         this.mesher.drawButton2D(bx, 738.0F * scale, bw, bh, 0xFF622626, 0xFFFF6868, false);
      } else if (this.world.screen == PeWorld.SCREEN_SETTINGS) {
         float pw = 1160.0F * scale;
         float px = cx - pw * 0.5F;
         this.mesher.drawCard2D(px, 82.0F * scale, pw, 892.0F * scale, 0xF0121620, 0xFF3A5270);
         this.mesher.addRect2D(px + 20.0F * scale, 98.0F * scale, pw - 40.0F * scale, 84.0F * scale, 0xD01A2230);

         float bw = 1020.0F * scale;
         float bx = cx - bw * 0.5F;
         float bh = 104.0F * scale;
      } else if (this.world.screen == PeWorld.SCREEN_SETTINGS) {
         float bw = 1040.0F * scale;
         float bx = cx - bw * 0.5F;
         float bh = 90.0F * scale;
         this.mesher.drawCard2D(bx - 56.0F * scale, 96.0F * scale, bw + 112.0F * scale, 888.0F * scale, 0xEC10141C, 0xFF3A5270);
         for (int i = 0; i < 6; i++) {
            int acc = (i == 1 || i == 4 || i == 5) ? 0xFF54E882 : 0xFF5CA8FF;
            this.mesher.drawButton2D(bx, (194.0F + i * 106.0F) * scale, bw, bh, 0xFF202C3C, acc, false);
         }
         this.mesher.drawButton2D(cx - 320.0F * scale, 846.0F * scale, 640.0F * scale, 98.0F * scale, 0xFF286C3E, 0xFF54E882, true);
      }

      this.mesher.flush2D(this.shaders, this.textures.whiteTexId, this.orthoMatrix);

      // Pass B: Atlas Icons on Menu Cards (Title Block Badges & World Slot Icons)
      this.mesher.begin2D();
      if (this.world.screen == PeWorld.SCREEN_TITLE) {
         this.mesher.addTile2D(cx - 540.0F * scale, 148.0F * scale, 116.0F * scale, 116.0F * scale, PeTextures.T_GRASS_SIDE, 0xFFFFFFFF);
         this.mesher.addTile2D(cx + 424.0F * scale, 148.0F * scale, 116.0F * scale, 116.0F * scale, PeTextures.T_CRAFT_SIDE, 0xFFFFFFFF);
      } else if (this.world.screen == PeWorld.SCREEN_WORLD_SELECT) {
         float pw = 1540.0F * scale;
         float px = cx - pw * 0.5F;
         int[] slotIcons = {PeTextures.T_GRASS_SIDE, PeTextures.T_LEAVES_OAK, PeTextures.T_CRAFT_SIDE};
         for (int s = 0; s < 3; s++) {
            float rowY = (232.0F + s * 186.0F) * scale;
            this.mesher.addTile2D(px + 84.0F * scale, rowY + 39.0F * scale, 80.0F * scale, 80.0F * scale, slotIcons[s], 0xFFFFFFFF);
         }
      }
      this.mesher.flush2D(this.shaders, this.textures.atlasTexId, this.orthoMatrix);

      // Pass C: Crisp Bitmap Font Text
      this.mesher.begin2D();
      if (this.world.screen == PeWorld.SCREEN_TITLE) {
         this.mesher.addTextCentered2D(this.textures, "BLOCKCRAFT PE", cx, 142.0F * scale, 6.4F * scale, 0xFFFFEA68);
         this.mesher.addTextCentered2D(this.textures, "v1.9  -  BRAND-NEW TEXTURES & ALL BLOCKS GALLERY", cx, 304.0F * scale, 2.50F * scale, 0xFF98FFB8);
         this.mesher.addTextCentered2D(this.textures, "PLAY SINGLEPLAYER", cx, 506.0F * scale, 4.0F * scale, 0xFFFFFFFF);
         this.mesher.addTextCentered2D(this.textures, "ALL BLOCKS & ITEMS GALLERY", cx, 646.0F * scale, 3.8F * scale, 0xFF98FFB8);
         this.mesher.addTextCentered2D(this.textures, "CONTROLS & SETTINGS", cx, 786.0F * scale, 3.8F * scale, 0xFFE4F2FF);
         this.mesher.addTextCentered2D(this.textures, "Samsung Galaxy A55 5G  |  120Hz Sub-Tick Physics & Infinite Chunks", cx, 952.0F * scale, 2.6F * scale, 0xFFB8D4F0);
      } else if (this.world.screen == PeWorld.SCREEN_WORLD_SELECT) {
         float pw = 1540.0F * scale;
         float px = cx - pw * 0.5F;
         this.mesher.addTextCentered2D(this.textures, "SELECT OR CREATE INFINITE SINGLEPLAYER WORLD", cx, 124.0F * scale, 4.0F * scale, 0xFFFFEA68);
         for (int s = 0; s < 3; s++) {
            float rowY = (232.0F + s * 186.0F) * scale;
            String title = "WORLD SLOT " + (s + 1) + (this.world.slotExists[s] ? "   [SAVED]" : "   [NEW WORLD]");
            this.mesher.addText2D(this.textures, title, px + 210.0F * scale, rowY + 30.0F * scale, 3.6F * scale, 0xFFFFFFFF);
            this.mesher.addText2D(this.textures, this.world.slotSummary[s], px + 210.0F * scale, rowY + 92.0F * scale, 2.9F * scale, 0xFF98F4B4);
            String rightLabel = this.world.slotExists[s] ? "DELETE" : "CREATE";
            this.mesher.addTextCentered2D(this.textures, rightLabel, px + pw - 170.0F * scale, rowY + 60.0F * scale, 3.4F * scale, 0xFFFFFFFF);
         }
         this.mesher.addTextCentered2D(this.textures, "BACK TO TITLE", cx, 854.0F * scale, 3.6F * scale, 0xFFFFFFFF);
      } else if (this.world.screen == PeWorld.SCREEN_CREATE_WORLD) {
         this.mesher.addTextCentered2D(this.textures, "CREATE INFINITE WORLD (SLOT " + (this.world.createSlot + 1) + ")", cx, 138.0F * scale, 4.1F * scale, 0xFFFFEA68);
         this.mesher.addTextCentered2D(this.textures, "GAME MODE:  " + PeWorld.MODE_NAMES[this.world.createMode], cx, 292.0F * scale, 3.6F * scale, 0xFFFFFFFF);
         this.mesher.addTextCentered2D(this.textures, "WORLD TYPE:  " + PeWorld.WTYPE_NAMES[this.world.createWorldType], cx, 432.0F * scale, 3.4F * scale, 0xFF98FFB8);
         this.mesher.addTextCentered2D(this.textures, "BIOME SEED:  " + PeWorld.SEED_NAMES[this.world.createSeedIdx], cx, 572.0F * scale, 3.6F * scale, 0xFFFFE282);
         this.mesher.addTextCentered2D(this.textures, "Large Biomes recommended  -  Infinite chunks stream seamlessly as you explore!", cx, 668.0F * scale, 2.6F * scale, 0xFFB8E2FF);
         float bx = cx - 510.0F * scale;
         this.mesher.addTextCentered2D(this.textures, "CREATE INFINITE WORLD", bx + 320.0F * scale, 760.0F * scale, 3.6F * scale, 0xFFFFFFFF);
         this.mesher.addTextCentered2D(this.textures, "CANCEL", bx + 842.0F * scale, 760.0F * scale, 3.5F * scale, 0xFFFFFFFF);
      } else if (this.world.screen == PeWorld.SCREEN_PAUSE) {
         this.mesher.addTextCentered2D(this.textures, "GAME PAUSED  -  " + this.world.currentWorldSummary(), cx, 132.0F * scale, 3.8F * scale, 0xFFFFEA68);
         this.mesher.addTextCentered2D(this.textures, "RESUME GAME", cx, 252.0F * scale, 3.6F * scale, 0xFFFFFFFF);
         this.mesher.addTextCentered2D(this.textures, "GAME MODE:  " + PeWorld.MODE_NAMES[this.world.mode], cx, 382.0F * scale, 3.6F * scale, 0xFF98E8FF);
         this.mesher.addTextCentered2D(this.textures, "CONTROLS & SETTINGS", cx, 512.0F * scale, 3.6F * scale, 0xFFFFFFFF);
         this.mesher.addTextCentered2D(this.textures, "SAVE WORLD NOW", cx, 642.0F * scale, 3.6F * scale, 0xFF98FFB8);
         this.mesher.addTextCentered2D(this.textures, "SAVE & QUIT TO TITLE", cx, 772.0F * scale, 3.6F * scale, 0xFFFFC4C4);
      } else if (this.world.screen == PeWorld.SCREEN_SETTINGS) {
         this.mesher.addTextCentered2D(this.textures, "CONTROLS, TEXTURE PACKS & PERFORMANCE SETTINGS", cx, 120.0F * scale, 3.8F * scale, 0xFFFFEA68);
         this.mesher.addTextCentered2D(this.textures, "LEFT MOVEMENT:  " + (this.touch.useAnalogStick ? "CIRCULAR ANALOG JOYSTICK" : "CLASSIC 5-BUTTON D-PAD"), cx, 224.0F * scale, 3.0F * scale, 0xFFFFFFFF);
         this.mesher.addTextCentered2D(this.textures, "ACTION CONTROLS:  " + (this.touch.touchTapToInteract ? "BUTTONS + STATIONARY TAP" : "DEDICATED BUTTONS ONLY (NO ACCIDENTAL MINE)"), cx, 332.0F * scale, 2.6F * scale, 0xFF98FFB8);
         String sensStr = this.touch.sensitivityLevel == 0 ? "LOW" : (this.touch.sensitivityLevel == 2 ? "HIGH" : "MEDIUM");
         this.mesher.addTextCentered2D(this.textures, "CAMERA LOOK SENSITIVITY:  " + sensStr, cx, 436.0F * scale, 3.0F * scale, 0xFFFFFFFF);
         this.mesher.addTextCentered2D(this.textures, "AUTO-JUMP:  " + (this.world.autoJump ? "ON (POCKET EDITION)" : "OFF"), cx, 542.0F * scale, 3.0F * scale, 0xFFFFE282);
         int gridW = this.world.viewRadiusChunks * 2 + 1;
         this.mesher.addTextCentered2D(this.textures, "INFINITE RENDER DISTANCE:  " + gridW + "x" + gridW + " CHUNKS (" + (gridW * 16) + " BLOCKS)", cx, 648.0F * scale, 2.9F * scale, 0xFF98E8FF);
         this.mesher.addTextCentered2D(this.textures, "RESOURCE PACK:  " + PeTextures.PACK_NAMES[PeTextures.activePackMode], cx, 754.0F * scale, 3.0F * scale, 0xFF98FFB8);
         this.mesher.addTextCentered2D(this.textures, "DONE", cx, 878.0F * scale, 3.8F * scale, 0xFFFFFFFF);
      }
      this.mesher.flush2D(this.shaders, this.textures.fontTexId, this.orthoMatrix);
   }

   // ====================================================================
   //  4-Tab Inventory, 27-Slot Player Backpack, Crafting & Creative Screen
   // ====================================================================
   private void renderInventoryAndCraftingScreen(float scale) {
      float panelW = 1940.0F * scale;
      float panelH = 790.0F * scale;
      float px = (this.width - panelW) * 0.5F;
      float py = 90.0F * scale;
      int cat = Math.max(0, Math.min(3, this.world.invCategory));
      int[] catItems = (cat >= 1) ? PeWorld.CREATIVE_TABS[cat - 1] : null;
      int baseRecipe = (cat == 0) ? 0 : ((cat - 1) * 8);

      this.mesher.begin2D();
      this.mesher.addRect2D(0, 0, this.width, this.height, 0x88080C14);
      this.mesher.drawCard2D(px, py, panelW, panelH, 0xF2121620, 0xFF3A5270);
      this.mesher.addRect2D(px + 18.0F * scale, py + 16.0F * scale, panelW - 36.0F * scale, 80.0F * scale, 0xD81A2230);

      // 4 Category Tab Buttons in Header Bar: [Backpack & Craft] [Building] [Nature] [Tools]
      float tabY = py + 18.0F * scale;
      float tabH = 74.0F * scale;
      float tabW = 310.0F * scale;
      for (int t = 0; t < 4; t++) {
         float tx = px + 390.0F * scale + t * (tabW + 14.0F * scale);
         boolean activeTab = (t == cat);
         this.mesher.drawButton2D(tx, tabY, tabW, tabH,
            activeTab ? 0xFF285842 : 0xFF202A3A,
            activeTab ? 0xFF54E882 : 0xFF4A6688, activeTab);
      }

      // Close Button
      this.mesher.drawButton2D(px + panelW - 216.0F * scale, py + 18.0F * scale, 196.0F * scale, 74.0F * scale, 0xFF682626, 0xFFFF6868, false);

      float startX = px + 38.0F * scale;
      float startY = py + 150.0F * scale;
      int curHeld = this.world.hotbarId[this.world.selectedSlot];

      if (cat == 0) {
         // Tab 0 Left Half: 9x3 Player Backpack Grid (27 Slots) + Item Action Bar
         float bagCell = 98.0F * scale;
         for (int i = 0; i < 27; i++) {
            int c = i % 9;
            int r = i / 9;
            boolean sel = (i == this.world.selectedInvSlot);
            this.mesher.drawSlot2D(startX + c * bagCell + 4.0F * scale, startY + r * bagCell + 4.0F * scale,
               bagCell - 8.0F * scale, sel, 0);
         }
         // Selected Item Inspector & Quick-Swap Bar below the 9x3 Backpack Grid
         float barY = startY + 3.0F * bagCell + 20.0F * scale;
         this.mesher.drawCard2D(startX, barY, 882.0F * scale, 148.0F * scale, 0xE618202E, 0xFF466284);
         this.mesher.drawButton2D(startX + 430.0F * scale, barY + 26.0F * scale, 220.0F * scale, 96.0F * scale,
            0xFF265C3E, 0xFF54E882, this.world.selectedInvSlot >= 0);
         this.mesher.drawButton2D(startX + 666.0F * scale, barY + 26.0F * scale, 196.0F * scale, 96.0F * scale,
            0xFF5E2828, 0xFFFF6868, false);
      } else {
         // Tabs 1..3 Left Half: 6x4 Categorized Block/Item Palette (24 Items per Tab)
         int cols = 6;
         float cell = 148.0F * scale;
         for (int i = 0; i < catItems.length; i++) {
            int c = i % cols;
            int r = i / cols;
            boolean isEquipped = (catItems[i] == curHeld);
            this.mesher.drawSlot2D(startX + c * cell + 6.0F * scale, startY + r * cell + 6.0F * scale,
               cell - 12.0F * scale, isEquipped, 0);
         }
      }

      // Right Half: 2x4 Large Crafting/Smelting Cards (8 Recipes per Tab)
      float craftStartX = px + 980.0F * scale;
      float craftColW = 448.0F * scale;
      float craftRowH = 132.0F * scale;
      for (int i = 0; i < 8; i++) {
         int recipeIdx = getDisplayRecipeIndex(cat, baseRecipe, i);
         if (recipeIdx >= PeWorld.RECIPES.length) break;
         int c = i & 1;
         int r = i >> 1;
         PeWorld.CraftRecipe rec = PeWorld.RECIPES[recipeIdx];
         boolean canCraft = (this.world.mode == PeWorld.MODE_CREATIVE)
            || (this.world.countItemInHotbar(rec.costId) >= rec.costCount);
         int bg = canCraft ? 0xFF224C34 : 0xFF202632;
         int acc = canCraft ? 0xFF54E882 : 0xFF425066;
         float rx = craftStartX + c * (craftColW + 18.0F * scale);
         float ry = startY + r * (craftRowH + 16.0F * scale);
         this.mesher.drawButton2D(rx, ry, craftColW, craftRowH, bg, acc, canCraft);
         this.mesher.drawSlot2D(rx + 14.0F * scale, ry + 18.0F * scale, 96.0F * scale, false, 0xD8161C24);
      }

      // Bottom 9-Slot Hotbar + [CLOSE] Slot
      float hs = this.touch.hotbarSlotSize;
      float hx = this.touch.hotbarX;
      float hy = this.touch.hotbarY;
      this.mesher.drawCard2D(hx - 8.0F * scale, hy - 8.0F * scale, hs * 10.0F + 16.0F * scale, hs + 16.0F * scale, 0xE812161E, 0xFF3A4E68);
      for (int s = 0; s < 10; s++) {
         float sx = hx + s * hs;
         boolean sel = (s == this.world.selectedSlot);
         int special = (s == 9) ? 0xD8682828 : 0;
         this.mesher.drawSlot2D(sx + 3.0F * scale, hy + 3.0F * scale, hs - 6.0F * scale, sel, special);
      }
      this.mesher.flush2D(this.shaders, this.textures.whiteTexId, this.orthoMatrix);

      // Pass B: Isometric 3D Block Previews & Crisp Pixel-Art Item Sprites
      this.mesher.begin2D();
      if (cat == 0) {
         float bagCell = 98.0F * scale;
         for (int i = 0; i < 27; i++) {
            int id = this.world.invId[i];
            if (id != PeWorld.B_AIR && this.world.invCount[i] > 0) {
               int c = i % 9;
               int r = i / 9;
               this.mesher.addItemOrBlockIcon2D(startX + c * bagCell + 16.0F * scale,
                  startY + r * bagCell + 16.0F * scale, bagCell - 32.0F * scale, id);
            }
         }
         int inspectId = (this.world.selectedInvSlot >= 0 && this.world.invId[this.world.selectedInvSlot] > 0)
            ? this.world.invId[this.world.selectedInvSlot] : curHeld;
         if (inspectId > 0) {
            float barY = startY + 3.0F * bagCell + 20.0F * scale;
            this.mesher.addItemOrBlockIcon2D(startX + 22.0F * scale, barY + 26.0F * scale, 96.0F * scale, inspectId);
         }
      } else {
         int cols = 6;
         float cell = 148.0F * scale;
         for (int i = 0; i < catItems.length; i++) {
            int c = i % cols;
            int r = i / cols;
            this.mesher.addItemOrBlockIcon2D(startX + c * cell + 24.0F * scale, startY + r * cell + 24.0F * scale,
               cell - 48.0F * scale, catItems[i]);
         }
      }
      for (int i = 0; i < 8; i++) {
         int recipeIdx = getDisplayRecipeIndex(cat, baseRecipe, i);
         if (recipeIdx >= PeWorld.RECIPES.length) break;
         int c = i & 1;
         int r = i >> 1;
         PeWorld.CraftRecipe rec = PeWorld.RECIPES[recipeIdx];
         float bx = craftStartX + c * (craftColW + 18.0F * scale) + 28.0F * scale;
         float by = startY + r * (craftRowH + 16.0F * scale) + 32.0F * scale;
         this.mesher.addItemOrBlockIcon2D(bx, by, 68.0F * scale, rec.outId);
      }
      for (int s = 0; s < 9; s++) {
         int id = this.world.hotbarId[s];
         if (id != PeWorld.B_AIR && this.world.hotbarCount[s] > 0) {
            this.mesher.addItemOrBlockIcon2D(hx + s * hs + 14.0F * scale, hy + 14.0F * scale,
               hs - 28.0F * scale, id);
         }
      }
      this.mesher.flush2D(this.shaders, this.textures.atlasTexId, this.orthoMatrix);

      // Pass C: Labels & Stack Counts
      this.mesher.begin2D();
      String leftHdr = (cat == 0) ? "PLAYER BACKPACK (36 SLOTS)"
         : ((this.world.mode == PeWorld.MODE_CREATIVE) ? "CREATIVE PALETTE" : "BLOCK & ITEM CATALOG");
      this.mesher.addText2D(this.textures, leftHdr, startX, py + 36.0F * scale, 2.75F * scale, 0xFFFFEA68);

      for (int t = 0; t < 4; t++) {
         float tx = px + 390.0F * scale + t * (tabW + 14.0F * scale);
         this.mesher.addTextCentered2D(this.textures, PeWorld.INV_TAB_NAMES[t],
            tx + tabW * 0.5F, tabY + 24.0F * scale, 2.35F * scale,
            t == cat ? 0xFFFFFFFF : 0xFFB8D4F0);
      }

      this.mesher.addText2D(this.textures, "TAP A RECIPE CARD TO CRAFT OR SMELT:", craftStartX, py + 114.0F * scale, 2.45F * scale, 0xFF98FFB8);
      String subHint = (cat == 0)
         ? "TAP A BACKPACK SLOT + TAP A HOTBAR SLOT BELOW TO SWAP:"
         : (this.world.mode == PeWorld.MODE_CREATIVE ? "TAP ANY BLOCK/TOOL TO EQUIP IN HOTBAR:" : "TAP CATEGORY TABS ABOVE FOR MORE RECIPES:");
      this.mesher.addText2D(this.textures, subHint, startX, py + 114.0F * scale, 2.35F * scale, 0xFFB8E2FF);
      this.mesher.addTextCentered2D(this.textures, "CLOSE", px + panelW - 118.0F * scale, py + 38.0F * scale, 3.0F * scale, 0xFFFFFFFF);

      if (cat == 0) {
         float bagCell = 98.0F * scale;
         for (int i = 0; i < 27; i++) {
            if (this.world.invId[i] != PeWorld.B_AIR && this.world.invCount[i] > 1) {
               int c = i % 9;
               int r = i / 9;
               this.mesher.addText2D(this.textures, String.valueOf(this.world.invCount[i]),
                  startX + c * bagCell + bagCell - 38.0F * scale,
                  startY + r * bagCell + bagCell - 30.0F * scale, 2.2F * scale, 0xFFFFFFFF);
            }
         }
         float barY = startY + 3.0F * bagCell + 20.0F * scale;
         int selBag = this.world.selectedInvSlot;
         int inspectId = (selBag >= 0 && this.world.invId[selBag] > 0) ? this.world.invId[selBag] : curHeld;
         int inspectCnt = (selBag >= 0 && this.world.invId[selBag] > 0) ? this.world.invCount[selBag] : this.world.hotbarCount[this.world.selectedSlot];
         String inspectTitle = (inspectId > 0) ? (PeWorld.getItemName(inspectId) + " (x" + Math.max(1, inspectCnt) + ")") : "Empty Slot Selected";
         String inspectSub = (selBag >= 0) ? ("Backpack Slot #" + (selBag + 1) + " selected") : ("Hotbar Slot #" + (this.world.selectedSlot + 1) + " active");
         this.mesher.addText2D(this.textures, inspectTitle, startX + 132.0F * scale, barY + 34.0F * scale, 2.45F * scale, 0xFFFFF4B8);
         this.mesher.addText2D(this.textures, inspectSub, startX + 132.0F * scale, barY + 80.0F * scale, 2.15F * scale, 0xFF98E8FF);
         this.mesher.addTextCentered2D(this.textures, "TO HOTBAR", startX + 540.0F * scale, barY + 58.0F * scale, 2.45F * scale, 0xFFFFFFFF);
         this.mesher.addTextCentered2D(this.textures, "DROP / CLEAR", startX + 764.0F * scale, barY + 58.0F * scale, 2.25F * scale, 0xFFFFD0D0);
      }

      for (int i = 0; i < 8; i++) {
         int recipeIdx = getDisplayRecipeIndex(cat, baseRecipe, i);
         if (recipeIdx >= PeWorld.RECIPES.length) break;
         int c = i & 1;
         int r = i >> 1;
         PeWorld.CraftRecipe rec = PeWorld.RECIPES[recipeIdx];
         int have = this.world.countItemInHotbar(rec.costId);
         boolean ok = (this.world.mode == PeWorld.MODE_CREATIVE) || (have >= rec.costCount);
         float bx = craftStartX + c * (craftColW + 18.0F * scale) + 122.0F * scale;
         float by = startY + r * (craftRowH + 16.0F * scale);
         this.mesher.addText2D(this.textures, rec.label, bx, by + 30.0F * scale, 2.25F * scale, 0xFFFFFFFF);
         String status = (this.world.mode == PeWorld.MODE_CREATIVE)
            ? "TAP TO EQUIP (" + rec.outCount + "x)"
            : (ok ? "READY TO CRAFT! (" + have + "/" + rec.costCount + ")"
                  : "Need " + rec.costCount + "x " + PeWorld.getItemName(rec.costId) + " (" + have + ")");
         this.mesher.addText2D(this.textures, status, bx, by + 76.0F * scale, 2.05F * scale,
            ok ? 0xFF88FFB0 : 0xFFFFB880);
      }
      for (int s = 0; s < 9; s++) {
         if (this.world.hotbarId[s] != PeWorld.B_AIR && this.world.hotbarCount[s] > 1) {
            this.mesher.addText2D(this.textures, String.valueOf(this.world.hotbarCount[s]),
               hx + s * hs + hs - 38.0F * scale, hy + hs - 30.0F * scale, 2.3F * scale, 0xFFFFFFFF);
         }
      }
      this.mesher.addTextCentered2D(this.textures, "X", hx + 9.5F * hs, hy + 28.0F * scale, 3.2F * scale, 0xFFFFFFFF);
      this.mesher.flush2D(this.shaders, this.textures.fontTexId, this.orthoMatrix);
   }

   public static int getDisplayRecipeIndex(int cat, int baseRecipe, int slotIdx) {
      if (cat == 0) {
         // On the Backpack & Craft tab, show the 8 most essential Survival recipes across all categories!
         final int[] essential = {0, 8, 11, 16, 17, 9, 15, 18};
         return essential[slotIdx & 7];
      }
      return baseRecipe + slotIdx;
   }

   // ====================================================================
   //  Redesigned Mobile-Friendly In-Game HUD
   // ====================================================================
   private void renderPlayHud(int[] rayHit) {
      float scale = this.height / 1080.0F;
      float cx = this.width * 0.5F;
      float cy = this.height * 0.5F;

      // Pass A: Dark-Glass Pills, Circular Joystick, Ergonomic Action Pads & Recessed Hotbar
      this.mesher.begin2D();

      // Hurt red vignette flash
      if (this.world.hurtTime > 0) {
         int alpha = Math.min(110, this.world.hurtTime * 12);
         this.mesher.addRect2D(0, 0, this.width, this.height, (alpha << 24) | 0xFF2010);
      }

      // 1. Top-Left Status Card (Hearts & Hunger in Survival, or Creative Badge)
      this.mesher.drawCard2D(this.touch.safeMarginX - 8.0F * scale, 14.0F * scale,
         412.0F * scale, 88.0F * scale, 0xBF121620, 0xC0364A64);

      // 2. Top-Center Quick-Action Pill Buttons ([|| PAUSE], [BAG / CRAFT], and optional [FLY])
      this.mesher.drawButton2D(this.touch.topPauseX, this.touch.topPauseY, this.touch.topPauseW, this.touch.topPauseH,
         0xD41C2432, 0xFF70A8FF, false);
      this.mesher.drawButton2D(this.touch.topInvX, this.touch.topInvY, this.touch.topInvW, this.touch.topInvH,
         0xD41E3C2C, 0xFF54E882, false);
      if (this.world.mode == PeWorld.MODE_CREATIVE || this.world.flying) {
         this.mesher.drawButton2D(this.touch.topFlyX, this.touch.topFlyY, this.touch.topFlyW, this.touch.topFlyH,
            this.world.flying ? 0xD8285878 : 0xCC1E2634, 0xFF68D8FF, this.world.flying);
      }

      // 3. Top-Right Coordinates, Biome & FPS Pill
      float infoW = 545.0F * scale;
      float infoX = this.width - this.touch.safeMarginX - infoW;
      this.mesher.drawCard2D(infoX, 14.0F * scale, infoW, 84.0F * scale, 0xBF121620, 0xC0364A64);

      // 4. Crisp Outlined Crosshair + Target Block Tooltip / Mining Progress Bar
      float chLen = 17.0F * scale;
      float chThick = 3.0F * scale;
      // Dark shadow outline behind crosshair so it's readable on snow/sky
      this.mesher.addRect2D(cx - chLen - 1.5F, cy - chThick * 0.5F - 1.5F, chLen * 2.0F + 3.0F, chThick + 3.0F, 0x90000000);
      this.mesher.addRect2D(cx - chThick * 0.5F - 1.5F, cy - chLen - 1.5F, chThick + 3.0F, chLen * 2.0F + 3.0F, 0x90000000);
      this.mesher.addRect2D(cx - chLen, cy - chThick * 0.5F, chLen * 2.0F, chThick, 0xF0FFFFFF);
      this.mesher.addRect2D(cx - chThick * 0.5F, cy - chLen, chThick, chLen * 2.0F, 0xF0FFFFFF);

      if (this.world.mineProgress > 0.02F) {
         float barW = 160.0F * scale;
         float barH = 16.0F * scale;
         this.mesher.drawCard2D(cx - barW * 0.5F, cy + 30.0F * scale, barW, barH, 0xD810141C, 0xFF384A60);
         this.mesher.addRect2D(cx - barW * 0.5F + 3.0F, cy + 30.0F * scale + 3.0F,
            (barW - 6.0F) * Math.min(1.0F, this.world.mineProgress), barH - 6.0F, 0xFF54F884);
      } else if (rayHit != null) {
         // Subtle target block pill under crosshair
         this.mesher.addRect2D(cx - 115.0F * scale, cy + 28.0F * scale, 230.0F * scale, 34.0F * scale, 0x8810141E);
      }

      // 5. Left Thumb Zone: Sneak Pill + Circular Analog Joystick (or 5-Button D-Pad)
      this.mesher.drawButton2D(this.touch.sneakBtnX, this.touch.sneakBtnY, this.touch.sneakBtnW, this.touch.sneakBtnH,
         this.touch.sneakHeld ? 0xD8784C18 : 0xAC1A222E,
         this.touch.sneakHeld ? 0xFFFFB848 : 0xFF527090, this.touch.sneakHeld);

      float dcx = this.touch.dpadCenterX;
      float dcy = this.touch.dpadCenterY;
      float dr = this.touch.dpadRadius;
      if (!this.touch.useAnalogStick) {
         float btnS = dr * 0.58F;
         this.mesher.drawButton2D(dcx - btnS * 0.5F, dcy - dr, btnS, btnS,
            this.touch.moveForward > 0.1F ? 0xD03868A8 : 0x981C2430, 0xFF68B0FF, this.touch.moveForward > 0.1F);
         this.mesher.drawButton2D(dcx - btnS * 0.5F, dcy + dr - btnS, btnS, btnS,
            this.touch.moveForward < -0.1F ? 0xD03868A8 : 0x981C2430, 0xFF68B0FF, this.touch.moveForward < -0.1F);
         this.mesher.drawButton2D(dcx - dr, dcy - btnS * 0.5F, btnS, btnS,
            this.touch.moveStrafe < -0.1F ? 0xD03868A8 : 0x981C2430, 0xFF68B0FF, this.touch.moveStrafe < -0.1F);
         this.mesher.drawButton2D(dcx + dr - btnS, dcy - btnS * 0.5F, btnS, btnS,
            this.touch.moveStrafe > 0.1F ? 0xD03868A8 : 0x981C2430, 0xFF68B0FF, this.touch.moveStrafe > 0.1F);
         this.mesher.drawButton2D(dcx - btnS * 0.44F, dcy - btnS * 0.44F, btnS * 0.88F, btnS * 0.88F,
            this.touch.sneakHeld ? 0xD0885818 : 0x88161C24, 0xFFFFB848, this.touch.sneakHeld);
      } else {
         // Outer Circular Virtual Joystick Ring + Cardinal Guides + Glowing Thumb Puck
         this.mesher.drawCirclePad2D(dcx, dcy, dr * 0.90F, 0x9048688E, 0x58121822);
         this.mesher.drawCirclePad2D(dcx, dcy, dr * 0.24F, 0x506890B8, 0x401A2230);
         float knobX = dcx + this.touch.moveStrafe * dr * 0.54F;
         float knobY = dcy - this.touch.moveForward * dr * 0.54F;
         boolean moving = (Math.abs(this.touch.moveForward) > 0.05F || Math.abs(this.touch.moveStrafe) > 0.05F);
         this.mesher.drawCirclePad2D(knobX, knobY, 54.0F * scale,
            moving ? 0xFF78C8FF : 0xC85888B8,
            moving ? 0xE83268A8 : 0xC0243852);
      }

      // 6. Right Thumb Zone: Circular Ergonomic Action Pads ([JUMP], [MINE], [PLACE])
      this.mesher.drawCirclePad2D(this.touch.jumpBtnX, this.touch.jumpBtnY, this.touch.jumpBtnR,
         this.touch.jumpHeld ? 0xFF78E8FF : 0xC84A88B8,
         this.touch.jumpHeld ? 0xE02A6C98 : 0xB41C3248);

      this.mesher.drawCirclePad2D(this.touch.atkBtnX, this.touch.atkBtnY, this.touch.atkBtnR,
         this.touch.miningHeld ? 0xFFFF8868 : 0xD8B84A42,
         this.touch.miningHeld ? 0xE8982822 : 0xBC4E1E1E);

      this.mesher.drawCirclePad2D(this.touch.useBtnX, this.touch.useBtnY, this.touch.useBtnR,
         0xD84CE07A, 0xBC1E4E30);

      // 7. Bottom 9-Slot Recessed Hotbar + Floating Item Name Pill + [BAG] Button
      float hs = this.touch.hotbarSlotSize;
      float hx = this.touch.hotbarX;
      float hy = this.touch.hotbarY;
      this.mesher.addRect2D(cx - 210.0F * scale, hy - 48.0F * scale, 420.0F * scale, 36.0F * scale, 0xA810141E);
      this.mesher.drawCard2D(hx - 8.0F * scale, hy - 8.0F * scale, hs * 10.0F + 16.0F * scale, hs + 16.0F * scale, 0xDF12161E, 0xFF384C66);
      for (int s = 0; s < 10; s++) {
         float sx = hx + s * hs;
         boolean sel = (s == this.world.selectedSlot);
         int special = (s == 9) ? 0xD8284868 : 0;
         this.mesher.drawSlot2D(sx + 3.0F * scale, hy + 3.0F * scale, hs - 6.0F * scale, sel, special);
      }

      this.mesher.flush2D(this.shaders, this.textures.whiteTexId, this.orthoMatrix);

      // Pass B: Atlas Icons (Hearts, Hunger, Hotbar Item Icons, Action Pad Mini-Icons)
      this.mesher.begin2D();
      if (this.world.mode == PeWorld.MODE_SURVIVAL) {
         float heartY = 22.0F * scale;
         float heartX = this.touch.safeMarginX + 6.0F * scale;
         float heartS = 34.0F * scale;
         int hearts = (this.world.health + 1) / 2;
         for (int i = 0; i < 10; i++) {
            int tile = (i < hearts) ? PeTextures.T_HEART_FULL : PeTextures.T_HEART_EMPTY;
            this.mesher.addTile2D(heartX + i * (heartS + 4.0F * scale), heartY, heartS, heartS, tile, 0xFFFFFFFF);
         }
         int food = (this.world.hunger + 1) / 2;
         for (int i = 0; i < 10; i++) {
            int tile = (i < food) ? PeTextures.T_HUNGER_FULL : PeTextures.T_HUNGER_EMPTY;
            this.mesher.addTile2D(heartX + i * (heartS + 4.0F * scale), heartY + 38.0F * scale, heartS, heartS, tile, 0xFFFFFFFF);
         }
      } else {
         this.mesher.addTile2D(this.touch.safeMarginX + 12.0F * scale, 28.0F * scale, 56.0F * scale, 56.0F * scale, PeTextures.T_GRASS_SIDE, 0xFFFFFFFF);
      }

      // Mini Pickaxe Icon inside [MINE] button & Isometric 3D Block / Item Icon inside [PLACE] button
      this.mesher.addTile2D(this.touch.atkBtnX - 24.0F * scale, this.touch.atkBtnY - 52.0F * scale,
         48.0F * scale, 48.0F * scale, PeTextures.T_ITEM_PICK_IRON, 0xFFFFFFFF);
      int selItem = this.world.hotbarId[this.world.selectedSlot];
      if (selItem > 0) {
         this.mesher.addItemOrBlockIcon2D(this.touch.useBtnX - 22.0F * scale, this.touch.useBtnY - 48.0F * scale,
            44.0F * scale, selItem);
      } else {
         this.mesher.addTile2D(this.touch.useBtnX - 22.0F * scale, this.touch.useBtnY - 48.0F * scale,
            44.0F * scale, 44.0F * scale, PeTextures.T_CRAFT_TOP, 0xFFFFFFFF);
      }

      for (int s = 0; s < 9; s++) {
         int id = this.world.hotbarId[s];
         if (id != PeWorld.B_AIR && this.world.hotbarCount[s] > 0) {
            this.mesher.addItemOrBlockIcon2D(hx + s * hs + 14.0F * scale, hy + 14.0F * scale,
               hs - 28.0F * scale, id);
         }
      }
      this.mesher.flush2D(this.shaders, this.textures.atlasTexId, this.orthoMatrix);

      // Pass C: Crisp Bitmap Font Text Labels
      this.mesher.begin2D();

      if (this.world.mode == PeWorld.MODE_CREATIVE) {
         this.mesher.addText2D(this.textures, "CREATIVE MODE", this.touch.safeMarginX + 82.0F * scale, 28.0F * scale, 2.7F * scale, 0xFFFFEA68);
         this.mesher.addText2D(this.textures, this.world.flying ? "Flight Active" : "Tap FLY to Soar", this.touch.safeMarginX + 82.0F * scale, 60.0F * scale, 2.3F * scale, 0xFF98F4B8);
      }

      this.mesher.addTextCentered2D(this.textures, "|| PAUSE",
         this.touch.topPauseX + this.touch.topPauseW * 0.5F, this.touch.topPauseY + 18.0F * scale, 2.6F * scale, 0xFFFFFFFF);
      this.mesher.addTextCentered2D(this.textures, "BAG / CRAFT",
         this.touch.topInvX + this.touch.topInvW * 0.5F, this.touch.topInvY + 18.0F * scale, 2.5F * scale, 0xFF98FFB8);
      if (this.world.mode == PeWorld.MODE_CREATIVE || this.world.flying) {
         this.mesher.addTextCentered2D(this.textures, this.world.flying ? "FLY: ON" : "FLY: OFF",
            this.touch.topFlyX + this.touch.topFlyW * 0.5F, this.touch.topFlyY + 18.0F * scale, 2.5F * scale, 0xFFD8F4FF);
      }

      // Top-Right Coordinates, Biome & FPS Readout
      int bpx = (int)Math.floor(this.world.px);
      int bpy = (int)Math.floor(this.world.py);
      int bpz = (int)Math.floor(this.world.pz);
      String posLine = String.format("XYZ: %d, %d, %d  |  %d FPS", bpx, bpy, bpz, this.currentFps);
      this.mesher.addText2D(this.textures, posLine, infoX + 18.0F * scale, 24.0F * scale, 2.45F * scale, 0xFFEAF6FF);

      String subLine = (this.world.bannerTicks > 0 && this.world.bannerMessage != null && !this.world.bannerMessage.isEmpty())
         ? this.world.bannerMessage
         : (this.world.getBiomeName(bpx, bpz) + "  |  " + this.world.currentWorldSummary());
      this.mesher.addText2D(this.textures, subLine, infoX + 18.0F * scale, 58.0F * scale, 2.3F * scale, 0xFFFFE278);

      // Target Block Tooltip under crosshair
      if (rayHit != null && this.world.mineProgress <= 0.02F) {
         String targetName = PeWorld.getItemName(rayHit[6]);
         this.mesher.addTextCentered2D(this.textures, targetName, cx, cy + 34.0F * scale, 2.2F * scale, 0xFFD8F0FF);
      }

      // D-Pad directional labels when in Classic D-Pad mode
      if (!this.touch.useAnalogStick) {
         this.mesher.addTextCentered2D(this.textures, "^", dcx, dcy - dr * 0.72F, 3.2F * scale, 0xFFFFFFFF);
         this.mesher.addTextCentered2D(this.textures, "v", dcx, dcy + dr * 0.55F, 3.0F * scale, 0xFFFFFFFF);
         this.mesher.addTextCentered2D(this.textures, "<", dcx - dr * 0.72F, dcy - 12.0F * scale, 3.0F * scale, 0xFFFFFFFF);
         this.mesher.addTextCentered2D(this.textures, ">", dcx + dr * 0.72F, dcy - 12.0F * scale, 3.0F * scale, 0xFFFFFFFF);
      }

      // Sneak & Right Circular Action Pad Labels
      this.mesher.addTextCentered2D(this.textures, this.touch.sneakHeld ? "SNEAK: ON" : "SNEAK",
         this.touch.sneakBtnX + this.touch.sneakBtnW * 0.5F, this.touch.sneakBtnY + 18.0F * scale, 2.4F * scale, 0xFFFFFFFF);
      this.mesher.addTextCentered2D(this.textures, this.world.flying ? "ASCEND" : "JUMP",
         this.touch.jumpBtnX, this.touch.jumpBtnY - 12.0F * scale, 2.6F * scale, 0xFFFFFFFF);
      this.mesher.addTextCentered2D(this.textures, "MINE",
         this.touch.atkBtnX, this.touch.atkBtnY + 6.0F * scale, 2.6F * scale, 0xFFFFE4E0);
      this.mesher.addTextCentered2D(this.textures, "PLACE",
         this.touch.useBtnX, this.touch.useBtnY + 6.0F * scale, 2.5F * scale, 0xFFD8FFE4);

      // Selected Hotbar Item Name Banner + Stack Counts
      this.mesher.addTextCentered2D(this.textures, PeWorld.getItemName(selItem),
         cx, hy - 42.0F * scale, 2.6F * scale, 0xFFFFF4B8);
      for (int s = 0; s < 9; s++) {
         if (this.world.hotbarId[s] != PeWorld.B_AIR && this.world.hotbarCount[s] > 1) {
            this.mesher.addText2D(this.textures, String.valueOf(this.world.hotbarCount[s]),
               hx + s * hs + hs - 38.0F * scale, hy + hs - 30.0F * scale, 2.3F * scale, 0xFFFFFFFF);
         }
      }
      this.mesher.addTextCentered2D(this.textures, "BAG", hx + 9.5F * hs, hy + 30.0F * scale, 2.6F * scale, 0xFFFFFFFF);

      this.mesher.flush2D(this.shaders, this.textures.fontTexId, this.orthoMatrix);
   }
}
