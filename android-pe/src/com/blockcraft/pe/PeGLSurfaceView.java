package com.blockcraft.pe;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.view.MotionEvent;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * OpenGL ES 3.0 / 2.0 SurfaceView & Renderer for Blockcraft PE Single-Player
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

      if (this.world.screen == PeWorld.SCREEN_PLAY) {
         // Apply camera touch look deltas at full 120Hz display refresh rate
         this.world.yaw += this.touch.consumeYawDelta();
         this.world.pitch = Math.max(-89.0F, Math.min(89.0F, this.world.pitch + this.touch.consumePitchDelta()));

         // Handle dedicated button taps (MINE/HIT vs PLACE/USE)
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
         this.panoramaYaw = (this.panoramaYaw + dt * 10.0F) % 360.0F;
      }

      // Rebuild up to 8 dirty chunk sections per frame
      this.mesher.updateDirtySections(this.world, 8);

      // Day/Night Sky & Fog color
      float sun = (float)Math.sin(this.world.dayTime * Math.PI * 2.0);
      float dayBright = Math.max(0.18F, Math.min(1.0F, 0.62F + sun * 0.45F));
      float skyR = 0.44F * dayBright;
      float skyG = 0.68F * dayBright;
      float skyB = 0.98F * dayBright;

      GLES20.glClearColor(skyR, skyG, skyB, 1.0F);
      GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

      // Build Right-Handed 3D Camera View & MVP Matrix:
      // Forward = ( +sin(yaw)*cos(pitch), -sin(pitch), -cos(yaw)*cos(pitch) )
      // Right   = ( +cos(yaw),             0,          +sin(yaw)            )
      boolean isMenuPanorama = (this.world.screen == PeWorld.SCREEN_TITLE
         || this.world.screen == PeWorld.SCREEN_WORLD_SELECT
         || this.world.screen == PeWorld.SCREEN_CREATE_WORLD);
      float camX = isMenuPanorama ? 0.5F : (float)this.world.px;
      float camY = isMenuPanorama ? 74.0F : (float)(this.world.py + (this.touch.sneakHeld && !this.world.flying ? 1.46 : 1.62));
      float camZ = isMenuPanorama ? 0.5F : (float)this.world.pz;
      float useYaw = isMenuPanorama ? this.panoramaYaw : this.world.yaw;
      float usePitch = isMenuPanorama ? 12.0F : this.world.pitch;

      float yawRad = (float)Math.toRadians(useYaw);
      float pitchRad = (float)Math.toRadians(usePitch);
      float cosP = (float)Math.cos(pitchRad);
      float lookX = (float)(Math.sin(yawRad) * cosP);
      float lookY = (float)(-Math.sin(pitchRad));
      float lookZ = (float)(-Math.cos(yawRad) * cosP);

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
         (float)Math.sin(yawRad), (float)(-Math.cos(yawRad)));

      // 2. Render 3D Dynamic Entities (Mobs, Projectiles, Target Block Highlight & Crack Overlay, 1st-Person Hand)
      if (!isMenuPanorama) {
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
            this.mesher.addBox3D(px - 0.12F, py - 0.12F, pz - 0.12F, px + 0.12F, py + 0.12F, pz + 0.12F,
               PeTextures.T_PLANKS_OAK, 1.0F, 0.9F, 0.6F, 1.0F);
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
                  PeTextures.T_CRACK_0 + crackStage, 1.0F, 1.0F, 1.0F, 0.88F);
            } else {
               this.mesher.addBox3D(bx - 0.005F, by - 0.005F, bz - 0.005F, bx + 1.005F, by + 1.005F, bz + 1.005F,
                  PeTextures.T_GLASS, 1.0F, 1.0F, 1.0F, 0.25F);
            }
         }

         // First-Person Held Item / Block in bottom-right of camera view
         float rightX = (float)Math.cos(yawRad);
         float rightZ = (float)Math.sin(yawRad);
         float sw = this.world.swingAnim >= 0.0F ? (float)Math.sin(this.world.swingAnim * Math.PI) * 0.22F : 0.0F;
         float hx = camX + lookX * (0.58F - sw * 0.4F) + rightX * 0.36F;
         float hy = camY + lookY * 0.58F - 0.28F + sw * 0.15F;
         float hz = camZ + lookZ * (0.58F - sw * 0.4F) + rightZ * 0.36F;
         int heldId = this.world.hotbarId[this.world.selectedSlot];
         int heldTile = PeWorld.getItemTile(heldId);
         this.mesher.addBox3D(hx - 0.10F, hy - 0.10F, hz - 0.10F, hx + 0.10F, hy + 0.10F, hz + 0.10F,
            heldTile, dayBright, dayBright, dayBright, 1.0F);
         this.mesher.flushDynamic3D(this.shaders);
      }

      // 3. Render 2D UI / HUD
      GLES20.glDisable(GLES20.GL_DEPTH_TEST);
      if (this.world.screen == PeWorld.SCREEN_PLAY) {
         renderPlayHud();
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

      // Pass A: Colored Panels & Buttons
      this.mesher.begin2D();
      this.mesher.addRect2D(0, 0, this.width, this.height, 0x9010141C);

      if (this.world.screen == PeWorld.SCREEN_TITLE) {
         drawBevelButton(cx - 560.0F * scale, 120.0F * scale, 1120.0F * scale, 240.0F * scale, 0xD81E242E);
         float bw = 760.0F * scale;
         float bh = 116.0F * scale;
         float bx = cx - bw * 0.5F;
         drawBevelButton(bx, 500.0F * scale, bw, bh, 0xFF347844);
         drawBevelButton(bx, 650.0F * scale, bw, bh, 0xFF2E3C4E);
      } else if (this.world.screen == PeWorld.SCREEN_WORLD_SELECT) {
         float pw = 1480.0F * scale;
         float px = cx - pw * 0.5F;
         drawBevelButton(px, 90.0F * scale, pw, 860.0F * scale, 0xEE181C24);
         for (int s = 0; s < 3; s++) {
            float rowY = (240.0F + s * 185.0F) * scale;
            float rowH = 155.0F * scale;
            int slotCol = this.world.slotExists[s] ? 0xFF285838 : 0xFF2A323E;
            drawBevelButton(px + 40.0F * scale, rowY, pw - 360.0F * scale, rowH, slotCol);
            int actCol = this.world.slotExists[s] ? 0xFF7A2E2E : 0xFF346844;
            drawBevelButton(px + pw - 300.0F * scale, rowY, 260.0F * scale, rowH, actCol);
         }
         drawBevelButton(cx - 320.0F * scale, 825.0F * scale, 640.0F * scale, 100.0F * scale, 0xFF384252);
      } else if (this.world.screen == PeWorld.SCREEN_CREATE_WORLD) {
         float pw = 1140.0F * scale;
         float px = cx - pw * 0.5F;
         drawBevelButton(px, 110.0F * scale, pw, 780.0F * scale, 0xEE181C24);
         float bw = 960.0F * scale;
         float bx = cx - bw * 0.5F;
         float bh = 112.0F * scale;
         drawBevelButton(bx, 260.0F * scale, bw, bh, 0xFF2C3A4C);
         drawBevelButton(bx, 400.0F * scale, bw, bh, 0xFF2C4A3E);
         drawBevelButton(bx, 540.0F * scale, bw, bh, 0xFF2C3A4C);
         drawBevelButton(bx, 720.0F * scale, 580.0F * scale, 120.0F * scale, 0xFF347844);
         drawBevelButton(bx + 610.0F * scale, 720.0F * scale, 350.0F * scale, 120.0F * scale, 0xFF5A3232);
      } else if (this.world.screen == PeWorld.SCREEN_PAUSE) {
         float pw = 960.0F * scale;
         drawBevelButton(cx - pw * 0.5F, 100.0F * scale, pw, 790.0F * scale, 0xEE181C24);
         float bw = 820.0F * scale;
         float bx = cx - bw * 0.5F;
         float bh = 104.0F * scale;
         drawBevelButton(bx, 220.0F * scale, bw, bh, 0xFF347844);
         drawBevelButton(bx, 350.0F * scale, bw, bh, 0xFF2E3E52);
         drawBevelButton(bx, 480.0F * scale, bw, bh, 0xFF2E3E52);
         drawBevelButton(bx, 610.0F * scale, bw, bh, 0xFF2E5A44);
         drawBevelButton(bx, 740.0F * scale, bw, bh, 0xFF683232);
      } else if (this.world.screen == PeWorld.SCREEN_SETTINGS) {
         float pw = 1120.0F * scale;
         drawBevelButton(cx - pw * 0.5F, 90.0F * scale, pw, 880.0F * scale, 0xEE181C24);
         float bw = 980.0F * scale;
         float bx = cx - bw * 0.5F;
         float bh = 104.0F * scale;
         for (int i = 0; i < 5; i++) {
            drawBevelButton(bx, (210.0F + i * 125.0F) * scale, bw, bh, 0xFF2C3A4C);
         }
         drawBevelButton(cx - 300.0F * scale, 845.0F * scale, 600.0F * scale, 100.0F * scale, 0xFF347844);
      }

      this.mesher.flush2D(this.shaders, this.textures.whiteTexId, this.orthoMatrix);

      // Pass B: Crisp Bitmap Font Text
      this.mesher.begin2D();
      if (this.world.screen == PeWorld.SCREEN_TITLE) {
         this.mesher.addTextCentered2D(this.textures, "BLOCKCRAFT PE", cx, 155.0F * scale, 6.2F * scale, 0xFFFFEA68);
         this.mesher.addTextCentered2D(this.textures, "Pocket Edition - Singleplayer World Gen & Saving", cx, 265.0F * scale, 3.2F * scale, 0xFFD8F0FF);
         this.mesher.addTextCentered2D(this.textures, "PLAY SINGLEPLAYER", cx, 536.0F * scale, 4.0F * scale, 0xFFFFFFFF);
         this.mesher.addTextCentered2D(this.textures, "CONTROLS & SETTINGS", cx, 686.0F * scale, 3.8F * scale, 0xFFE0F0FF);
         this.mesher.addTextCentered2D(this.textures, "Samsung Galaxy A55 5G | 120Hz OpenGL ES VBO Engine", cx, 960.0F * scale, 2.8F * scale, 0xFFB8D0E8);
      } else if (this.world.screen == PeWorld.SCREEN_WORLD_SELECT) {
         float pw = 1480.0F * scale;
         float px = cx - pw * 0.5F;
         this.mesher.addTextCentered2D(this.textures, "SELECT OR CREATE SINGLEPLAYER WORLD", cx, 135.0F * scale, 4.2F * scale, 0xFFFFEA68);
         for (int s = 0; s < 3; s++) {
            float rowY = (240.0F + s * 185.0F) * scale;
            String title = "WORLD SLOT " + (s + 1) + (this.world.slotExists[s] ? "  [SAVED]" : "  [EMPTY]");
            this.mesher.addText2D(this.textures, title, px + 76.0F * scale, rowY + 28.0F * scale, 3.6F * scale, 0xFFFFFFFF);
            this.mesher.addText2D(this.textures, this.world.slotSummary[s], px + 76.0F * scale, rowY + 88.0F * scale, 3.0F * scale, 0xFF98E8B0);
            String rightLabel = this.world.slotExists[s] ? "DELETE" : "CREATE";
            this.mesher.addTextCentered2D(this.textures, rightLabel, px + pw - 170.0F * scale, rowY + 58.0F * scale, 3.4F * scale, 0xFFFFFFFF);
         }
         this.mesher.addTextCentered2D(this.textures, "BACK TO TITLE", cx, 855.0F * scale, 3.6F * scale, 0xFFFFFFFF);
      } else if (this.world.screen == PeWorld.SCREEN_CREATE_WORLD) {
         this.mesher.addTextCentered2D(this.textures, "CREATE NEW WORLD (SLOT " + (this.world.createSlot + 1) + ")", cx, 150.0F * scale, 4.2F * scale, 0xFFFFEA68);
         this.mesher.addTextCentered2D(this.textures, "GAME MODE: " + PeWorld.MODE_NAMES[this.world.createMode], cx, 296.0F * scale, 3.6F * scale, 0xFFFFFFFF);
         this.mesher.addTextCentered2D(this.textures, "WORLD TYPE: " + PeWorld.WTYPE_NAMES[this.world.createWorldType], cx, 436.0F * scale, 3.4F * scale, 0xFF90FFB0);
         this.mesher.addTextCentered2D(this.textures, "SEED: " + PeWorld.SEED_NAMES[this.world.createSeedIdx], cx, 576.0F * scale, 3.6F * scale, 0xFFFFE088);
         this.mesher.addTextCentered2D(this.textures, "Tip: Large Biomes recommended for best exploration!", cx, 668.0F * scale, 2.8F * scale, 0xFFB8E0FF);
         float bx = cx - 480.0F * scale;
         this.mesher.addTextCentered2D(this.textures, "CREATE WORLD", bx + 290.0F * scale, 760.0F * scale, 3.8F * scale, 0xFFFFFFFF);
         this.mesher.addTextCentered2D(this.textures, "CANCEL", bx + 785.0F * scale, 760.0F * scale, 3.6F * scale, 0xFFFFFFFF);
      } else if (this.world.screen == PeWorld.SCREEN_PAUSE) {
         this.mesher.addTextCentered2D(this.textures, "GAME PAUSED - " + this.world.currentWorldSummary(), cx, 140.0F * scale, 3.8F * scale, 0xFFFFEA68);
         this.mesher.addTextCentered2D(this.textures, "RESUME GAME", cx, 254.0F * scale, 3.6F * scale, 0xFFFFFFFF);
         this.mesher.addTextCentered2D(this.textures, "GAME MODE: " + PeWorld.MODE_NAMES[this.world.mode], cx, 384.0F * scale, 3.6F * scale, 0xFF98E8FF);
         this.mesher.addTextCentered2D(this.textures, "CONTROLS & SETTINGS", cx, 514.0F * scale, 3.6F * scale, 0xFFFFFFFF);
         this.mesher.addTextCentered2D(this.textures, "SAVE WORLD NOW", cx, 644.0F * scale, 3.6F * scale, 0xFF98FFB0);
         this.mesher.addTextCentered2D(this.textures, "SAVE & QUIT TO TITLE", cx, 774.0F * scale, 3.6F * scale, 0xFFFFC0C0);
      } else if (this.world.screen == PeWorld.SCREEN_SETTINGS) {
         this.mesher.addTextCentered2D(this.textures, "CONTROLS & SETTINGS", cx, 130.0F * scale, 4.2F * scale, 0xFFFFEA68);
         this.mesher.addTextCentered2D(this.textures, "LEFT MOVEMENT: " + (this.touch.useAnalogStick ? "ANALOG JOYSTICK" : "CLASSIC 5-BUTTON D-PAD"), cx, 244.0F * scale, 3.3F * scale, 0xFFFFFFFF);
         this.mesher.addTextCentered2D(this.textures, "CAMERA / ACTIONS: " + (this.touch.touchTapToInteract ? "BUTTONS + STATIONARY TAP" : "DEDICATED BUTTONS ONLY (NO ACCIDENTAL MINE)"), cx, 369.0F * scale, 2.8F * scale, 0xFF98FFB0);
         String sensStr = this.touch.sensitivityLevel == 0 ? "LOW" : (this.touch.sensitivityLevel == 2 ? "HIGH" : "MEDIUM");
         this.mesher.addTextCentered2D(this.textures, "CAMERA LOOK SENSITIVITY: " + sensStr, cx, 494.0F * scale, 3.3F * scale, 0xFFFFFFFF);
         this.mesher.addTextCentered2D(this.textures, "AUTO-JUMP: " + (this.world.autoJump ? "ON" : "OFF"), cx, 619.0F * scale, 3.3F * scale, 0xFFFFE088);
         this.mesher.addTextCentered2D(this.textures, "INVERT Y-AXIS: " + (this.touch.invertY ? "ON" : "OFF"), cx, 744.0F * scale, 3.3F * scale, 0xFFFFFFFF);
         this.mesher.addTextCentered2D(this.textures, "DONE", cx, 878.0F * scale, 3.8F * scale, 0xFFFFFFFF);
      }
      this.mesher.flush2D(this.shaders, this.textures.fontTexId, this.orthoMatrix);
   }

   // ====================================================================
   //  Single-Player Inventory & Crafting Screen
   // ====================================================================
   private void renderInventoryAndCraftingScreen(float scale) {
      float panelW = 1920.0F * scale;
      float panelH = 780.0F * scale;
      float px = (this.width - panelW) * 0.5F;
      float py = 95.0F * scale;

      this.mesher.begin2D();
      this.mesher.addRect2D(0, 0, this.width, this.height, 0x9010141C);
      drawBevelButton(px, py, panelW, panelH, 0xF0181C24);
      drawBevelButton(px + panelW - 220.0F * scale, py + 16.0F * scale, 200.0F * scale, 72.0F * scale, 0xFF7A2E2E);

      // Left Half: 7x4 Block/Item Grid
      int[] pal = PeTouchController.getPaletteItems();
      int cols = 7;
      float cell = 132.0F * scale;
      float startX = px + 36.0F * scale;
      float startY = py + 120.0F * scale;
      for (int i = 0; i < pal.length; i++) {
         int c = i % cols;
         int r = i / cols;
         drawBevelButton(startX + c * cell + 6.0F * scale, startY + r * cell + 6.0F * scale,
            cell - 12.0F * scale, cell - 12.0F * scale, 0xFF2A3442);
      }

      // Right Half: 2x6 Crafting Recipe Buttons
      float craftStartX = px + 1010.0F * scale;
      float craftColW = 430.0F * scale;
      float craftRowH = 98.0F * scale;
      for (int i = 0; i < 12 && i < PeWorld.RECIPES.length; i++) {
         int c = i & 1;
         int r = i >> 1;
         PeWorld.CraftRecipe rec = PeWorld.RECIPES[i];
         boolean canCraft = (this.world.mode == PeWorld.MODE_CREATIVE)
            || (this.world.countItemInHotbar(rec.costId) >= rec.costCount);
         int bg = canCraft ? 0xFF2E5C3E : 0xFF2C2E38;
         drawBevelButton(craftStartX + c * (craftColW + 18.0F * scale), startY + r * (craftRowH + 10.0F * scale),
            craftColW, craftRowH, bg);
      }

      // Bottom Hotbar
      float hs = this.touch.hotbarSlotSize;
      float hx = this.touch.hotbarX;
      float hy = this.touch.hotbarY;
      this.mesher.addRect2D(hx - 6.0F * scale, hy - 6.0F * scale, hs * 10.0F + 12.0F * scale, hs + 12.0F * scale, 0xD8181A20);
      for (int s = 0; s < 10; s++) {
         float sx = hx + s * hs;
         boolean sel = (s == this.world.selectedSlot);
         int bg = (s == 9) ? 0xC0683232 : (sel ? 0xE04E8A3E : 0xB02C3038);
         drawBevelButton(sx + 3.0F * scale, hy + 3.0F * scale, hs - 6.0F * scale, hs - 6.0F * scale, bg);
      }
      this.mesher.flush2D(this.shaders, this.textures.whiteTexId, this.orthoMatrix);

      // Pass B: Item Icons
      this.mesher.begin2D();
      for (int i = 0; i < pal.length; i++) {
         int c = i % cols;
         int r = i / cols;
         int tile = PeWorld.getItemTile(pal[i]);
         this.mesher.addTile2D(startX + c * cell + 24.0F * scale, startY + r * cell + 24.0F * scale,
            cell - 48.0F * scale, cell - 48.0F * scale, tile, 0xFFFFFFFF);
      }
      for (int i = 0; i < 12 && i < PeWorld.RECIPES.length; i++) {
         int c = i & 1;
         int r = i >> 1;
         PeWorld.CraftRecipe rec = PeWorld.RECIPES[i];
         float bx = craftStartX + c * (craftColW + 18.0F * scale) + 12.0F * scale;
         float by = startY + r * (craftRowH + 10.0F * scale) + 14.0F * scale;
         this.mesher.addTile2D(bx, by, 68.0F * scale, 68.0F * scale, PeWorld.getItemTile(rec.outId), 0xFFFFFFFF);
      }
      for (int s = 0; s < 9; s++) {
         int id = this.world.hotbarId[s];
         if (id != PeWorld.B_AIR) {
            int tile = PeWorld.getItemTile(id);
            this.mesher.addTile2D(hx + s * hs + 14.0F * scale, hy + 14.0F * scale,
               hs - 28.0F * scale, hs - 28.0F * scale, tile, 0xFFFFFFFF);
         }
      }
      this.mesher.flush2D(this.shaders, this.textures.atlasTexId, this.orthoMatrix);

      // Pass C: Labels
      this.mesher.begin2D();
      String leftHdr = (this.world.mode == PeWorld.MODE_CREATIVE)
         ? "CREATIVE BLOCKS & TOOLS (TAP TO EQUIP)"
         : "BLOCKS & ITEMS";
      this.mesher.addText2D(this.textures, leftHdr, startX, py + 38.0F * scale, 3.2F * scale, 0xFFFFEA68);
      this.mesher.addText2D(this.textures, "CRAFTING RECIPES (TAP TO CRAFT)", craftStartX, py + 38.0F * scale, 3.2F * scale, 0xFF98FFB0);
      this.mesher.addTextCentered2D(this.textures, "CLOSE", px + panelW - 120.0F * scale, py + 36.0F * scale, 3.2F * scale, 0xFFFFFFFF);

      for (int i = 0; i < 12 && i < PeWorld.RECIPES.length; i++) {
         int c = i & 1;
         int r = i >> 1;
         PeWorld.CraftRecipe rec = PeWorld.RECIPES[i];
         float bx = craftStartX + c * (craftColW + 18.0F * scale) + 92.0F * scale;
         float by = startY + r * (craftRowH + 10.0F * scale) + 34.0F * scale;
         this.mesher.addText2D(this.textures, rec.label, bx, by, 2.3F * scale, 0xFFFFFFFF);
      }
      for (int s = 0; s < 9; s++) {
         if (this.world.hotbarId[s] != PeWorld.B_AIR && this.world.hotbarCount[s] > 1) {
            this.mesher.addText2D(this.textures, String.valueOf(this.world.hotbarCount[s]),
               hx + s * hs + hs - 38.0F * scale, hy + hs - 30.0F * scale, 2.4F * scale, 0xFFFFFFFF);
         }
      }
      this.mesher.addTextCentered2D(this.textures, "X", hx + 9.5F * hs, hy + 28.0F * scale, 3.2F * scale, 0xFFFFFFFF);
      this.mesher.flush2D(this.shaders, this.textures.fontTexId, this.orthoMatrix);
   }

   // ====================================================================
   //  In-Game Single-Player HUD
   // ====================================================================
   private void renderPlayHud() {
      float scale = this.height / 1080.0F;

      // Pass A: Colored Panels, Buttons, Crosshair & Movement Pad
      this.mesher.begin2D();

      // Hurt red vignette flash
      if (this.world.hurtTime > 0) {
         int alpha = Math.min(110, this.world.hurtTime * 12);
         this.mesher.addRect2D(0, 0, this.width, this.height, (alpha << 24) | 0xFF2010);
      }

      // Top-Center Compact [|| PAUSE] and [CRAFT / INV] buttons
      drawBevelButton(this.touch.topPauseX, this.touch.topPauseY, this.touch.topPauseW, this.touch.topPauseH, 0xC8222832);
      drawBevelButton(this.touch.topInvX, this.touch.topInvY, this.touch.topInvW, this.touch.topInvH, 0xC8284834);

      // Crosshair + Mining Progress Bar at screen center
      float cx = this.width * 0.5F;
      float cy = this.height * 0.5F;
      float chLen = 18.0F * scale;
      float chThick = 3.0F * scale;
      this.mesher.addRect2D(cx - chLen, cy - chThick * 0.5F, chLen * 2.0F, chThick, 0xE8FFFFFF);
      this.mesher.addRect2D(cx - chThick * 0.5F, cy - chLen, chThick, chLen * 2.0F, 0xE8FFFFFF);

      if (this.world.mineProgress > 0.02F) {
         float barW = 140.0F * scale;
         float barH = 14.0F * scale;
         this.mesher.addRect2D(cx - barW * 0.5F, cy + 32.0F * scale, barW, barH, 0xB0101418);
         this.mesher.addRect2D(cx - barW * 0.5F, cy + 32.0F * scale, barW * this.world.mineProgress, barH, 0xFF48FF68);
      }

      // Dedicated Sneak / Crouch Toggle Button
      drawBevelButton(this.touch.sneakBtnX, this.touch.sneakBtnY, this.touch.sneakBtnW, this.touch.sneakBtnH,
         this.touch.sneakHeld ? 0xD0E89828 : 0x88242A34);

      // Left Movement Pad (Joystick or Classic 5-Button D-Pad)
      float dcx = this.touch.dpadCenterX;
      float dcy = this.touch.dpadCenterY;
      float dr = this.touch.dpadRadius;
      if (!this.touch.useAnalogStick) {
         float btnS = dr * 0.58F;
         drawBevelButton(dcx - btnS * 0.5F, dcy - dr, btnS, btnS, this.touch.moveForward > 0.1F ? 0xC070A8FF : 0x8028303A);
         drawBevelButton(dcx - btnS * 0.5F, dcy + dr - btnS, btnS, btnS, this.touch.moveForward < -0.1F ? 0xC070A8FF : 0x8028303A);
         drawBevelButton(dcx - dr, dcy - btnS * 0.5F, btnS, btnS, this.touch.moveStrafe < -0.1F ? 0xC070A8FF : 0x8028303A);
         drawBevelButton(dcx + dr - btnS, dcy - btnS * 0.5F, btnS, btnS, this.touch.moveStrafe > 0.1F ? 0xC070A8FF : 0x8028303A);
         drawBevelButton(dcx - btnS * 0.44F, dcy - btnS * 0.44F, btnS * 0.88F, btnS * 0.88F, this.touch.sneakHeld ? 0xC0FFB040 : 0x7020242C);
      } else {
         drawBevelButton(dcx - dr * 0.85F, dcy - dr * 0.85F, dr * 1.7F, dr * 1.7F, 0x60202630);
         float knobX = dcx + this.touch.moveStrafe * dr * 0.55F;
         float knobY = dcy - this.touch.moveForward * dr * 0.55F;
         float kr = 52.0F * scale;
         drawBevelButton(knobX - kr, knobY - kr, kr * 2.0F, kr * 2.0F, 0xD070A8FF);
      }

      // Right Dedicated Ergonomic Action Buttons: [JUMP/FLY], [MINE/HIT], [PLACE/USE]
      float jr = this.touch.jumpBtnR;
      drawBevelButton(this.touch.jumpBtnX - jr, this.touch.jumpBtnY - jr, jr * 2.0F, jr * 2.0F,
         this.touch.jumpHeld ? 0xD050C878 : 0x98283848);
      float ar = this.touch.atkBtnR;
      drawBevelButton(this.touch.atkBtnX - ar, this.touch.atkBtnY - ar, ar * 2.0F, ar * 2.0F,
         this.touch.miningHeld ? 0xD8FF5252 : 0xA0542828);
      float ur = this.touch.useBtnR;
      drawBevelButton(this.touch.useBtnX - ur, this.touch.useBtnY - ur, ur * 2.0F, ur * 2.0F, 0xA0285238);

      // Bottom 9-Slot Touch Hotbar + [INV] Button
      float hs = this.touch.hotbarSlotSize;
      float hx = this.touch.hotbarX;
      float hy = this.touch.hotbarY;
      this.mesher.addRect2D(hx - 6.0F * scale, hy - 6.0F * scale, hs * 10.0F + 12.0F * scale, hs + 12.0F * scale, 0xC8181A20);
      for (int s = 0; s < 10; s++) {
         float sx = hx + s * hs;
         boolean sel = (s == this.world.selectedSlot);
         int bg = (s == 9) ? 0xD0324860 : (sel ? 0xE84E8A3E : 0xA82C3038);
         drawBevelButton(sx + 3.0F * scale, hy + 3.0F * scale, hs - 6.0F * scale, hs - 6.0F * scale, bg);
      }

      this.mesher.flush2D(this.shaders, this.textures.whiteTexId, this.orthoMatrix);

      // Pass B: Atlas Icons (Hearts, Hunger, Hotbar Item Icons)
      this.mesher.begin2D();
      if (this.world.mode == PeWorld.MODE_SURVIVAL) {
         float heartY = 22.0F * scale;
         float heartX = this.touch.safeMarginX;
         float heartS = 34.0F * scale;
         int hearts = (this.world.health + 1) / 2;
         for (int i = 0; i < 10; i++) {
            int tile = (i < hearts) ? PeTextures.T_HEART_FULL : PeTextures.T_HEART_EMPTY;
            this.mesher.addTile2D(heartX + i * (heartS + 4.0F * scale), heartY, heartS, heartS, tile, 0xFFFFFFFF);
         }
         int food = (this.world.hunger + 1) / 2;
         for (int i = 0; i < 10; i++) {
            int tile = (i < food) ? PeTextures.T_HUNGER_FULL : PeTextures.T_HUNGER_EMPTY;
            this.mesher.addTile2D(heartX + i * (heartS + 4.0F * scale), heartY + 40.0F * scale, heartS, heartS, tile, 0xFFFFFFFF);
         }
      }

      for (int s = 0; s < 9; s++) {
         int id = this.world.hotbarId[s];
         if (id != PeWorld.B_AIR) {
            int tile = PeWorld.getItemTile(id);
            this.mesher.addTile2D(hx + s * hs + 14.0F * scale, hy + 14.0F * scale,
               hs - 28.0F * scale, hs - 28.0F * scale, tile, 0xFFFFFFFF);
         }
      }
      this.mesher.flush2D(this.shaders, this.textures.atlasTexId, this.orthoMatrix);

      // Pass C: Crisp Bitmap Font Text Labels
      this.mesher.begin2D();
      this.mesher.addTextCentered2D(this.textures, "PAUSE",
         this.touch.topPauseX + this.touch.topPauseW * 0.5F, this.touch.topPauseY + 18.0F * scale, 2.8F * scale, 0xFFFFFFFF);
      this.mesher.addTextCentered2D(this.textures, "CRAFT / INV",
         this.touch.topInvX + this.touch.topInvW * 0.5F, this.touch.topInvY + 18.0F * scale, 2.7F * scale, 0xFF98FFB0);

      // Top-Right Coordinates, Mode & FPS Readout
      String posLine = String.format("XYZ: %d, %d, %d | %d FPS",
         (int)Math.floor(this.world.px), (int)Math.floor(this.world.py), (int)Math.floor(this.world.pz), this.currentFps);
      this.mesher.addText2D(this.textures, posLine, this.width - this.touch.safeMarginX - 540.0F * scale, 22.0F * scale, 2.6F * scale, 0xFFE8F4FF);

      String banner = (this.world.bannerTicks > 0 && this.world.bannerMessage != null && !this.world.bannerMessage.isEmpty())
         ? this.world.bannerMessage
         : this.world.currentWorldSummary();
      this.mesher.addText2D(this.textures, banner, this.width - this.touch.safeMarginX - 540.0F * scale, 58.0F * scale, 2.5F * scale, 0xFFFFE070);

      // Sneak & Right Action Button Labels
      this.mesher.addTextCentered2D(this.textures, this.touch.sneakHeld ? "SNEAK: ON" : "SNEAK",
         this.touch.sneakBtnX + this.touch.sneakBtnW * 0.5F, this.touch.sneakBtnY + 18.0F * scale, 2.5F * scale, 0xFFFFFFFF);
      this.mesher.addTextCentered2D(this.textures, this.world.flying ? "FLY UP" : "JUMP",
         this.touch.jumpBtnX, this.touch.jumpBtnY - 14.0F * scale, 2.8F * scale, 0xFFFFFFFF);
      this.mesher.addTextCentered2D(this.textures, "MINE",
         this.touch.atkBtnX, this.touch.atkBtnY - 28.0F * scale, 2.8F * scale, 0xFFFFD8D8);
      this.mesher.addTextCentered2D(this.textures, "/ HIT",
         this.touch.atkBtnX, this.touch.atkBtnY + 4.0F * scale, 2.5F * scale, 0xFFFFD8D8);
      this.mesher.addTextCentered2D(this.textures, "PLACE",
         this.touch.useBtnX, this.touch.useBtnY - 28.0F * scale, 2.7F * scale, 0xFFD0FFD8);
      this.mesher.addTextCentered2D(this.textures, "/ USE",
         this.touch.useBtnX, this.touch.useBtnY + 4.0F * scale, 2.5F * scale, 0xFFD0FFD8);

      // Selected Hotbar Item Name Banner + Stack Counts
      int selItem = this.world.hotbarId[this.world.selectedSlot];
      this.mesher.addTextCentered2D(this.textures, PeWorld.getItemName(selItem),
         this.width * 0.5F, this.touch.hotbarY - 42.0F * scale, 3.0F * scale, 0xFFFFFFFF);
      for (int s = 0; s < 9; s++) {
         if (this.world.hotbarId[s] != PeWorld.B_AIR && this.world.hotbarCount[s] > 1) {
            this.mesher.addText2D(this.textures, String.valueOf(this.world.hotbarCount[s]),
               hx + s * hs + hs - 38.0F * scale, hy + hs - 30.0F * scale, 2.3F * scale, 0xFFFFFFFF);
         }
      }
      this.mesher.addTextCentered2D(this.textures, "INV", hx + 9.5F * hs, hy + 28.0F * scale, 2.8F * scale, 0xFFFFFFFF);

      this.mesher.flush2D(this.shaders, this.textures.fontTexId, this.orthoMatrix);
   }

   private void drawBevelButton(float x, float y, float w, float h, int bgArgb) {
      this.mesher.addRect2D(x, y, w, h, bgArgb);
      this.mesher.addRect2D(x, y, w, 4.0F, 0x58FFFFFF);
      this.mesher.addRect2D(x, y + h - 4.0F, w, 4.0F, 0x80000000);
   }
}
