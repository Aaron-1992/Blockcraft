package com.blockcraft.pe;

import android.content.Context;
import android.view.MotionEvent;

/**
 * Multi-Touch Pocket Edition Controller tailored for the Samsung Galaxy A55 5G (2340x1080).
 *
 * Key design features:
 * 1. Camera look dragging NEVER triggers mining, block placement, or attacks by accident.
 * 2. Holding the circular [MINE] pad also allows dragging your thumb to aim while mining!
 * 3. Right-handed movement mapping: pushing Left strafes Left (-1), pushing Right strafes Right (+1).
 * 4. Ergonomic mobile layout with top quick-action pills ([PAUSE], [BAG / CRAFT], [FLY]),
 *    circular virtual joystick / 5-button D-Pad, right-thumb action arc ([MINE], [PLACE], [JUMP]),
 *    and full UI navigation across Title, World Select, Create World, Inventory/Crafting, Pause & Settings.
 */
public final class PeTouchController {
   public int screenW = 2340;
   public int screenH = 1080;

   // Safe area inset for hole-punch camera in landscape
   public float safeMarginX = 72.0F;

   // Movement output (-1..+1): moveStrafe < 0 = Left, moveStrafe > 0 = Right
   public float moveForward = 0.0F;
   public float moveStrafe = 0.0F;
   public boolean jumpHeld = false;
   public boolean sneakHeld = false;
   public boolean attackHeld = false;
   public boolean miningHeld = false;
   public boolean placeTapped = false;
   public boolean attackTapped = false;

   // User customizable control settings
   public boolean useAnalogStick = true;        // Smooth circular thumbstick by default; toggleable to Classic 5-Button D-Pad
   public boolean touchTapToInteract = false;   // False = pure camera look (actions only via buttons, 0 accidental actions!)
   public int sensitivityLevel = 1;             // 0=Low, 1=Medium, 2=High
   public boolean invertY = false;

   // Active pointer tracking
   private int movePointerId = -1;
   public float moveCurX = 0.0F;
   public float moveCurY = 0.0F;

   private int lookPointerId = -1;
   private float lookStartX = 0.0F;
   private float lookStartY = 0.0F;
   private float lookLastX = 0.0F;
   private float lookLastY = 0.0F;
   private long lookDownTimeMs = 0L;
   private boolean lookCancelledInteract = false;

   private int jumpPointerId = -1;
   private int attackBtnPointerId = -1;
   private float attackBtnLastX = 0.0F;
   private float attackBtnLastY = 0.0F;
   private long lastJumpTapMs = 0L;

   // Accumulated camera deltas
   private float accumYawDelta = 0.0F;
   private float accumPitchDelta = 0.0F;

   // Layout geometry (computed in updateLayout)
   public float dpadCenterX, dpadCenterY, dpadRadius;
   public float sneakBtnX, sneakBtnY, sneakBtnW, sneakBtnH;
   public float jumpBtnX, jumpBtnY, jumpBtnR;
   public float atkBtnX, atkBtnY, atkBtnR;
   public float useBtnX, useBtnY, useBtnR;
   public float hotbarX, hotbarY, hotbarSlotSize;
   public float topPauseX, topPauseY, topPauseW, topPauseH;
   public float topInvX, topInvY, topInvW, topInvH;
   public float topFlyX, topFlyY, topFlyW, topFlyH;

   public PeTouchController() {
      updateLayout(2340, 1080);
   }

   public void updateLayout(int w, int h) {
      this.screenW = Math.max(640, w);
      this.screenH = Math.max(360, h);
      float scale = this.screenH / 1080.0F;
      this.safeMarginX = 72.0F * scale;

      // Left Thumb Zone: Circular Virtual Joystick / 5-Button D-Pad
      this.dpadRadius = 176.0F * scale;
      this.dpadCenterX = this.safeMarginX + this.dpadRadius + 26.0F * scale;
      this.dpadCenterY = this.screenH - this.dpadRadius - 48.0F * scale;

      this.sneakBtnW = 164.0F * scale;
      this.sneakBtnH = 62.0F * scale;
      this.sneakBtnX = this.dpadCenterX - this.sneakBtnW * 0.5F;
      this.sneakBtnY = this.dpadCenterY - this.dpadRadius - 82.0F * scale;

      // Right Thumb Zone: Natural Ergonomic Arc of Circular Pads ([MINE], [PLACE], [JUMP])
      this.jumpBtnR = 78.0F * scale;
      this.jumpBtnX = this.screenW - this.safeMarginX - this.jumpBtnR - 26.0F * scale;
      this.jumpBtnY = this.screenH - this.jumpBtnR - 56.0F * scale;

      this.atkBtnR = 86.0F * scale;
      this.atkBtnX = this.jumpBtnX - 196.0F * scale;
      this.atkBtnY = this.jumpBtnY - 14.0F * scale;

      this.useBtnR = 78.0F * scale;
      this.useBtnX = this.jumpBtnX - 48.0F * scale;
      this.useBtnY = this.jumpBtnY - 186.0F * scale;

      // Bottom Center: 9-Slot Recessed Hotbar + [BAG] Button
      this.hotbarSlotSize = 88.0F * scale;
      float totalHotbarW = this.hotbarSlotSize * 10.0F;
      this.hotbarX = (this.screenW - totalHotbarW) * 0.5F;
      this.hotbarY = this.screenH - this.hotbarSlotSize - 16.0F * scale;

      // Top Center / Top Right Quick-Action Pill Bar
      this.topPauseW = 176.0F * scale;
      this.topPauseH = 62.0F * scale;
      this.topPauseX = this.screenW * 0.5F - this.topPauseW - 10.0F * scale;
      this.topPauseY = 16.0F * scale;

      this.topInvW = 216.0F * scale;
      this.topInvH = 62.0F * scale;
      this.topInvX = this.screenW * 0.5F + 10.0F * scale;
      this.topInvY = 16.0F * scale;

      this.topFlyW = 162.0F * scale;
      this.topFlyH = 62.0F * scale;
      this.topFlyX = this.topInvX + this.topInvW + 14.0F * scale;
      this.topFlyY = 16.0F * scale;
   }

   public synchronized float consumeYawDelta() {
      float d = this.accumYawDelta;
      this.accumYawDelta = 0.0F;
      return d;
   }

   public synchronized float consumePitchDelta() {
      float d = this.accumPitchDelta;
      this.accumPitchDelta = 0.0F;
      return d;
   }

   private float lookSensMultiplier() {
      float base = (this.sensitivityLevel == 0) ? 0.14F : ((this.sensitivityLevel == 2) ? 0.30F : 0.21F);
      return base * (1080.0F / this.screenH);
   }

   public synchronized void onTouchEvent(MotionEvent ev, Context context, PeWorld world, PeAudio audio) {
      int action = ev.getActionMasked();
      int idx = ev.getActionIndex();
      int pid = ev.getPointerId(idx);
      float x = ev.getX(idx);
      float y = ev.getY(idx);
      long now = System.currentTimeMillis();

      // If not in active gameplay (Title, World Select, Create World, Pause, Inventory, Settings), handle menu taps
      if (world.screen != PeWorld.SCREEN_PLAY) {
         if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            resetPointers(world);
            handleMenuScreenTap(x, y, context, world, audio);
         }
         return;
      }

      switch (action) {
         case MotionEvent.ACTION_DOWN:
         case MotionEvent.ACTION_POINTER_DOWN: {
            // 1. Top Pill Buttons: [|| PAUSE], [BAG / CRAFT], and [FLY]
            if (inRect(x, y, this.topPauseX, this.topPauseY, this.topPauseW, this.topPauseH)) {
               resetPointers(world);
               world.saveWorld(context);
               world.screen = PeWorld.SCREEN_PAUSE;
               if (audio != null) audio.playSfx("click", 0.85F, 1.0F);
               return;
            }
            if (inRect(x, y, this.topInvX, this.topInvY, this.topInvW, this.topInvH)) {
               resetPointers(world);
               world.previousScreen = PeWorld.SCREEN_PLAY;
               world.screen = PeWorld.SCREEN_INVENTORY;
               if (audio != null) audio.playSfx("click", 0.85F, 1.1F);
               return;
            }
            if ((world.mode == PeWorld.MODE_CREATIVE || world.flying)
               && inRect(x, y, this.topFlyX, this.topFlyY, this.topFlyW, this.topFlyH)) {
               world.flying = !world.flying;
               world.showBanner(world.flying ? "Creative Flight: ON" : "Creative Flight: OFF");
               if (audio != null) audio.playSfx("click", 0.8F, 1.15F);
               return;
            }

            // 2. Bottom 9-Slot Hotbar + [BAG] button
            if (y >= this.hotbarY && y <= this.hotbarY + this.hotbarSlotSize
               && x >= this.hotbarX && x <= this.hotbarX + this.hotbarSlotSize * 10.0F) {
               int slot = (int)((x - this.hotbarX) / this.hotbarSlotSize);
               if (slot >= 0 && slot < 9) {
                  world.selectedSlot = slot;
                  if (audio != null) audio.playSfx("click", 0.75F, 1.0F);
               } else if (slot == 9) {
                  resetPointers(world);
                  world.previousScreen = PeWorld.SCREEN_PLAY;
                  world.screen = PeWorld.SCREEN_INVENTORY;
                  if (audio != null) audio.playSfx("click", 0.85F, 1.15F);
               }
               return;
            }

            // 3. Dedicated Sneak / Crouch Toggle Pill (above Joystick/D-Pad)
            if (inRect(x, y, this.sneakBtnX, this.sneakBtnY, this.sneakBtnW, this.sneakBtnH)) {
               this.sneakHeld = !this.sneakHeld;
               world.sneaking = this.sneakHeld;
               if (audio != null) audio.playSfx("click", 0.7F, 0.9F);
               return;
            }

            // 4. Right-Side Dedicated Circular Action Buttons: [JUMP], [MINE], [PLACE]
            if (dist(x, y, this.jumpBtnX, this.jumpBtnY) <= this.jumpBtnR * 1.18F) {
               this.jumpPointerId = pid;
               this.jumpHeld = true;
               if (now - this.lastJumpTapMs < 320L && world.mode == PeWorld.MODE_CREATIVE) {
                  world.flying = !world.flying;
               }
               this.lastJumpTapMs = now;
               return;
            }
            if (dist(x, y, this.atkBtnX, this.atkBtnY) <= this.atkBtnR * 1.18F) {
               this.attackBtnPointerId = pid;
               this.attackBtnLastX = x;
               this.attackBtnLastY = y;
               this.attackHeld = true;
               this.miningHeld = true;
               this.attackTapped = true;
               return;
            }
            if (dist(x, y, this.useBtnX, this.useBtnY) <= this.useBtnR * 1.18F) {
               this.placeTapped = true;
               return;
            }

            // 5. Left Movement Pad (Joystick or D-Pad)
            if (dist(x, y, this.dpadCenterX, this.dpadCenterY) <= this.dpadRadius * 1.35F && this.movePointerId == -1) {
               this.movePointerId = pid;
               this.moveCurX = x;
               this.moveCurY = y;
               updateMoveVector(x, y, world);
               return;
            }

            // 6. Camera Look Area — Pure Camera Rotation!
            if (this.lookPointerId == -1) {
               this.lookPointerId = pid;
               this.lookStartX = x;
               this.lookStartY = y;
               this.lookLastX = x;
               this.lookLastY = y;
               this.lookDownTimeMs = now;
               this.lookCancelledInteract = false;
            }
            break;
         }

         case MotionEvent.ACTION_MOVE: {
            int count = ev.getPointerCount();
            float sens = lookSensMultiplier();
            float ySign = this.invertY ? -1.0F : 1.0F;
            for (int i = 0; i < count; i++) {
               int id = ev.getPointerId(i);
               float px = ev.getX(i);
               float py = ev.getY(i);
               if (id == this.movePointerId) {
                  this.moveCurX = px;
                  this.moveCurY = py;
                  updateMoveVector(px, py, world);
               } else if (id == this.attackBtnPointerId) {
                  // Allow aiming the camera while holding down the [MINE] button!
                  float dx = px - this.attackBtnLastX;
                  float dy = py - this.attackBtnLastY;
                  this.attackBtnLastX = px;
                  this.attackBtnLastY = py;
                  this.accumYawDelta += dx * sens;
                  this.accumPitchDelta += dy * sens * ySign;
               } else if (id == this.lookPointerId) {
                  float dx = px - this.lookLastX;
                  float dy = py - this.lookLastY;
                  this.lookLastX = px;
                  this.lookLastY = py;

                  this.accumYawDelta += dx * sens;
                  this.accumPitchDelta += dy * sens * ySign;

                  // The moment the finger moves >= 10px, lock out any tap/hold interaction on this touch!
                  if (dist(px, py, this.lookStartX, this.lookStartY) >= 10.0F) {
                     this.lookCancelledInteract = true;
                     if (this.attackBtnPointerId == -1) {
                        this.miningHeld = false;
                        this.attackHeld = false;
                     }
                  } else if (this.touchTapToInteract && !this.lookCancelledInteract && (now - this.lookDownTimeMs > 360L)) {
                     this.miningHeld = true;
                     this.attackHeld = true;
                  }
               }
            }
            break;
         }

         case MotionEvent.ACTION_UP:
         case MotionEvent.ACTION_POINTER_UP:
         case MotionEvent.ACTION_CANCEL: {
            if (action == MotionEvent.ACTION_CANCEL) {
               resetPointers(world);
               return;
            }
            if (pid == this.movePointerId) {
               this.movePointerId = -1;
               this.moveForward = 0.0F;
               this.moveStrafe = 0.0F;
               world.sprinting = false;
            } else if (pid == this.jumpPointerId) {
               this.jumpPointerId = -1;
               this.jumpHeld = false;
            } else if (pid == this.attackBtnPointerId) {
               this.attackBtnPointerId = -1;
               this.attackHeld = false;
               this.miningHeld = false;
            } else if (pid == this.lookPointerId) {
               long heldMs = now - this.lookDownTimeMs;
               if (this.touchTapToInteract && !this.lookCancelledInteract && heldMs < 240L) {
                  this.placeTapped = true;
               }
               this.lookPointerId = -1;
               if (this.attackBtnPointerId == -1) {
                  this.attackHeld = false;
                  this.miningHeld = false;
               }
            }
            break;
         }
      }
   }

   private void updateMoveVector(float x, float y, PeWorld world) {
      float dx = (x - this.dpadCenterX) / this.dpadRadius;
      float dy = (y - this.dpadCenterY) / this.dpadRadius;

      if (!this.useAnalogStick) {
         // Classic 5-Button D-Pad: Center = Sneak/Descend, Left = -1, Right = +1, Up = +1, Down = -1
         if (Math.abs(dx) < 0.28F && Math.abs(dy) < 0.28F) {
            this.moveForward = 0.0F;
            this.moveStrafe = 0.0F;
            this.sneakHeld = true;
            world.sneaking = true;
            world.sprinting = false;
            return;
         }
         this.moveStrafe = Math.abs(dx) > 0.26F ? (dx > 0 ? 1.0F : -1.0F) : 0.0F;
         this.moveForward = Math.abs(dy) > 0.26F ? (dy < 0 ? 1.0F : -1.0F) : 0.0F;
         world.sprinting = (this.moveForward > 0.5F && !this.sneakHeld);
      } else {
         float len = (float)Math.sqrt(dx * dx + dy * dy);
         if (len < 0.12F) {
            this.moveForward = 0.0F;
            this.moveStrafe = 0.0F;
            world.sprinting = false;
         } else {
            float scale = Math.min(1.0F, len);
            this.moveStrafe = (dx / len) * scale;
            this.moveForward = (-dy / len) * scale;
            world.sprinting = (this.moveForward > 0.78F && !this.sneakHeld);
         }
      }
   }

   // ====================================================================
   //  Menu & Modal Screen Touch Handling
   // ====================================================================
   private void handleMenuScreenTap(float x, float y, Context context, PeWorld world, PeAudio audio) {
      float scale = this.screenH / 1080.0F;
      float cx = this.screenW * 0.5F;

      if (world.screen == PeWorld.SCREEN_TITLE) {
         float bw = 820.0F * scale;
         float bh = 122.0F * scale;
         float bx = cx - bw * 0.5F;
         // [ PLAY SINGLEPLAYER ]
         if (inRect(x, y, bx, 500.0F * scale, bw, bh)) {
            world.refreshSaveSlots(context);
            world.screen = PeWorld.SCREEN_WORLD_SELECT;
            if (audio != null) audio.playSfx("click", 0.9F, 1.0F);
            return;
         }
         // [ CONTROLS & SETTINGS ]
         if (inRect(x, y, bx, 650.0F * scale, bw, bh)) {
            world.previousScreen = PeWorld.SCREEN_TITLE;
            world.screen = PeWorld.SCREEN_SETTINGS;
            if (audio != null) audio.playSfx("click", 0.9F, 1.05F);
         }
         return;
      }

      if (world.screen == PeWorld.SCREEN_WORLD_SELECT) {
         float pw = 1540.0F * scale;
         float px = cx - pw * 0.5F;
         for (int s = 0; s < 3; s++) {
            float rowY = (232.0F + s * 186.0F) * scale;
            float rowH = 158.0F * scale;
            // Main Slot Card [ PLAY / CREATE ]
            if (inRect(x, y, px + 40.0F * scale, rowY, pw - 360.0F * scale, rowH)) {
               if (world.slotExists[s]) {
                  world.loadWorld(context, s);
                  if (audio != null) audio.playSfx("levelup", 0.9F, 1.0F);
               } else {
                  world.createSlot = s;
                  world.createMode = PeWorld.MODE_SURVIVAL;
                  world.createWorldType = PeWorld.WTYPE_LARGE_BIOMES;
                  world.createSeedIdx = 0;
                  world.screen = PeWorld.SCREEN_CREATE_WORLD;
                  if (audio != null) audio.playSfx("click", 0.9F, 1.05F);
               }
               return;
            }
            // Right Slot Action Button [ CREATE / DELETE ]
            if (inRect(x, y, px + pw - 300.0F * scale, rowY, 260.0F * scale, rowH)) {
               if (world.slotExists[s]) {
                  world.deleteWorldSlot(context, s);
                  if (audio != null) audio.playSfx("dig", 0.85F, 0.9F);
               } else {
                  world.createSlot = s;
                  world.screen = PeWorld.SCREEN_CREATE_WORLD;
                  if (audio != null) audio.playSfx("click", 0.9F, 1.05F);
               }
               return;
            }
         }
         // [ BACK TO TITLE ]
         if (inRect(x, y, cx - 340.0F * scale, 822.0F * scale, 680.0F * scale, 102.0F * scale)) {
            world.screen = PeWorld.SCREEN_TITLE;
            if (audio != null) audio.playSfx("click", 0.85F, 0.95F);
         }
         return;
      }

      if (world.screen == PeWorld.SCREEN_CREATE_WORLD) {
         float bw = 1020.0F * scale;
         float bx = cx - bw * 0.5F;
         float bh = 114.0F * scale;
         // Row 1: Game Mode toggle
         if (inRect(x, y, bx, 255.0F * scale, bw, bh)) {
            world.createMode = (world.createMode + 1) % PeWorld.MODE_NAMES.length;
            if (audio != null) audio.playSfx("click", 0.85F, 1.05F);
            return;
         }
         // Row 2: World Type toggle (Large Biomes Recommended / Default / Superflat)
         if (inRect(x, y, bx, 395.0F * scale, bw, bh)) {
            world.createWorldType = (world.createWorldType + 1) % PeWorld.WTYPE_NAMES.length;
            if (audio != null) audio.playSfx("click", 0.85F, 1.05F);
            return;
         }
         // Row 3: Seed Preset toggle
         if (inRect(x, y, bx, 535.0F * scale, bw, bh)) {
            world.createSeedIdx = (world.createSeedIdx + 1) % PeWorld.SEED_NAMES.length;
            if (audio != null) audio.playSfx("click", 0.85F, 1.05F);
            return;
         }
         // Row 4: [ CREATE INFINITE WORLD ] and [ CANCEL ]
         if (inRect(x, y, bx, 720.0F * scale, 640.0F * scale, 120.0F * scale)) {
            long seed = PeWorld.SEED_PRESETS[world.createSeedIdx];
            world.createNewSinglePlayerWorld(context, world.createSlot, world.createMode, world.createWorldType, seed);
            if (audio != null) audio.playSfx("levelup", 0.95F, 1.0F);
            return;
         }
         if (inRect(x, y, bx + 664.0F * scale, 720.0F * scale, 356.0F * scale, 120.0F * scale)) {
            world.screen = PeWorld.SCREEN_WORLD_SELECT;
            if (audio != null) audio.playSfx("click", 0.85F, 0.95F);
         }
         return;
      }

      if (world.screen == PeWorld.SCREEN_PAUSE) {
         float bw = 860.0F * scale;
         float bx = cx - bw * 0.5F;
         float bh = 106.0F * scale;
         // 1. [ RESUME GAME ]
         if (inRect(x, y, bx, 218.0F * scale, bw, bh)) {
            world.screen = PeWorld.SCREEN_PLAY;
            if (audio != null) audio.playSfx("click", 0.85F, 1.05F);
            return;
         }
         // 2. [ GAME MODE: SURVIVAL / CREATIVE ]
         if (inRect(x, y, bx, 348.0F * scale, bw, bh)) {
            world.mode = (world.mode == PeWorld.MODE_SURVIVAL) ? PeWorld.MODE_CREATIVE : PeWorld.MODE_SURVIVAL;
            if (world.mode == PeWorld.MODE_SURVIVAL) world.flying = false;
            world.showBanner("Switched to " + PeWorld.MODE_NAMES[world.mode] + " Mode");
            if (audio != null) audio.playSfx("levelup", 0.85F, 1.1F);
            return;
         }
         // 3. [ CONTROLS & SETTINGS ]
         if (inRect(x, y, bx, 478.0F * scale, bw, bh)) {
            world.previousScreen = PeWorld.SCREEN_PAUSE;
            world.screen = PeWorld.SCREEN_SETTINGS;
            if (audio != null) audio.playSfx("click", 0.85F, 1.05F);
            return;
         }
         // 4. [ SAVE WORLD NOW ]
         if (inRect(x, y, bx, 608.0F * scale, bw, bh)) {
            world.saveWorld(context);
            if (audio != null) audio.playSfx("levelup", 0.9F, 1.0F);
            return;
         }
         // 5. [ SAVE & QUIT TO TITLE ]
         if (inRect(x, y, bx, 738.0F * scale, bw, bh)) {
            world.saveWorld(context);
            world.refreshSaveSlots(context);
            world.screen = PeWorld.SCREEN_TITLE;
            if (audio != null) audio.playSfx("click", 0.85F, 0.95F);
         }
         return;
      }

      if (world.screen == PeWorld.SCREEN_SETTINGS) {
         float bw = 1020.0F * scale;
         float bx = cx - bw * 0.5F;
         float bh = 104.0F * scale;
         // 1. Movement Pad: Joystick vs D-Pad
         if (inRect(x, y, bx, 206.0F * scale, bw, bh)) {
            this.useAnalogStick = !this.useAnalogStick;
            if (audio != null) audio.playSfx("click", 0.85F, 1.1F);
            return;
         }
         // 2. Look / Interact Mode: Dedicated Buttons Only vs Buttons + Stationary Tap
         if (inRect(x, y, bx, 330.0F * scale, bw, bh)) {
            this.touchTapToInteract = !this.touchTapToInteract;
            if (audio != null) audio.playSfx("click", 0.85F, 1.1F);
            return;
         }
         // 3. Look Sensitivity
         if (inRect(x, y, bx, 454.0F * scale, bw, bh)) {
            this.sensitivityLevel = (this.sensitivityLevel + 1) % 3;
            if (audio != null) audio.playSfx("click", 0.85F, 1.1F);
            return;
         }
         // 4. Auto-Jump ON / OFF
         if (inRect(x, y, bx, 578.0F * scale, bw, bh)) {
            world.autoJump = !world.autoJump;
            if (audio != null) audio.playSfx("click", 0.85F, 1.1F);
            return;
         }
         // 5. Render Distance (9x9 / 11x11 / 13x13 Chunks)
         if (inRect(x, y, bx, 702.0F * scale, bw, bh)) {
            world.viewRadiusChunks = (world.viewRadiusChunks >= 6) ? 4 : (world.viewRadiusChunks + 1);
            world.ensureChunksAroundPlayer(64);
            if (audio != null) audio.playSfx("click", 0.85F, 1.1F);
            return;
         }
         // 6. [ DONE ]
         if (inRect(x, y, cx - 320.0F * scale, 840.0F * scale, 640.0F * scale, 102.0F * scale)) {
            world.screen = world.previousScreen;
            if (audio != null) audio.playSfx("click", 0.85F, 1.0F);
         }
         return;
      }

      if (world.screen == PeWorld.SCREEN_INVENTORY) {
         handleInventoryAndCraftingTap(x, y, world, audio);
      }
   }

   private static final int[] PALETTE_ITEMS = new int[]{
      PeWorld.B_GRASS, PeWorld.B_DIRT, PeWorld.B_STONE, PeWorld.B_COBBLE,
      PeWorld.B_PLANKS_OAK, PeWorld.B_PLANKS_SPRUCE, PeWorld.B_LOG_OAK, PeWorld.B_LOG_SPRUCE,
      PeWorld.B_LEAVES_OAK, PeWorld.B_LEAVES_SPRUCE, PeWorld.B_BRICKS, PeWorld.B_STONEBRICK,
      PeWorld.B_SAND, PeWorld.B_SANDSTONE, PeWorld.B_GLASS, PeWorld.B_TORCH,
      PeWorld.B_GLOWSTONE, PeWorld.B_CRAFTING_TABLE, PeWorld.B_FURNACE, PeWorld.B_CHEST,
      PeWorld.B_BED_RED, PeWorld.B_BOOKSHELF, PeWorld.B_FLOWER_RED, PeWorld.B_FLOWER_YELLOW,
      PeWorld.I_PICK_DIAMOND, PeWorld.I_SWORD_DIAMOND, PeWorld.I_AXE_IRON, PeWorld.I_GOLDEN_APPLE
   };

   public static int[] getPaletteItems() {
      return PALETTE_ITEMS;
   }

   private void handleInventoryAndCraftingTap(float x, float y, PeWorld world, PeAudio audio) {
      float scale = this.screenH / 1080.0F;

      // Bottom Hotbar selection
      if (y >= this.hotbarY && y <= this.hotbarY + this.hotbarSlotSize
         && x >= this.hotbarX && x <= this.hotbarX + this.hotbarSlotSize * 10.0F) {
         int slot = (int)((x - this.hotbarX) / this.hotbarSlotSize);
         if (slot >= 0 && slot < 9) {
            world.selectedSlot = slot;
            if (audio != null) audio.playSfx("click", 0.75F, 1.0F);
         } else {
            world.screen = PeWorld.SCREEN_PLAY;
            if (audio != null) audio.playSfx("click", 0.85F, 1.0F);
         }
         return;
      }

      float panelW = 1940.0F * scale;
      float panelH = 790.0F * scale;
      float px = (this.screenW - panelW) * 0.5F;
      float py = 90.0F * scale;

      // Top-Right [ X CLOSE ] button
      if (inRect(x, y, px + panelW - 230.0F * scale, py + 18.0F * scale, 210.0F * scale, 74.0F * scale)
         || !inRect(x, y, px, py, panelW, panelH)) {
         world.screen = PeWorld.SCREEN_PLAY;
         if (audio != null) audio.playSfx("click", 0.85F, 0.95F);
         return;
      }

      // 3 Category Tabs in Header Bar: [ BUILDING BLOCKS ]  [ NATURE & DECOR ]  [ TOOLS & COMBAT ]
      float tabY = py + 18.0F * scale;
      float tabH = 74.0F * scale;
      float tabW = 360.0F * scale;
      for (int t = 0; t < 3; t++) {
         float tx = px + 520.0F * scale + t * (tabW + 16.0F * scale);
         if (inRect(x, y, tx, tabY, tabW, tabH)) {
            world.invCategory = t;
            if (audio != null) audio.playSfx("click", 0.85F, 1.1F);
            return;
         }
      }

      int cat = Math.max(0, Math.min(2, world.invCategory));
      int[] catItems = PeWorld.CREATIVE_TABS[cat];

      // Left Half (24 Category Blocks/Items): 6 cols x 4 rows
      int cols = 6;
      float cell = 148.0F * scale;
      float startX = px + 38.0F * scale;
      float startY = py + 150.0F * scale;
      for (int i = 0; i < catItems.length; i++) {
         int c = i % cols;
         int r = i / cols;
         float bx = startX + c * cell;
         float by = startY + r * cell;
         if (inRect(x, y, bx, by, cell, cell)) {
            int item = catItems[i];
            if (world.mode == PeWorld.MODE_CREATIVE) {
               world.hotbarId[world.selectedSlot] = item;
               world.hotbarCount[world.selectedSlot] = (item <= PeWorld.MAX_BLOCK_ID) ? 64 : 1;
               world.showBanner("Equipped " + PeWorld.getItemName(item));
               if (audio != null) audio.playSfx("pop", 0.85F, 1.1F);
            } else {
               world.showBanner(PeWorld.getItemName(item) + " - Mine in world or Craft on the right!");
               if (audio != null) audio.playSfx("click", 0.75F, 1.0F);
            }
            return;
         }
      }

      // Right Half (8 Category Crafting/Smelting Recipes): 2 cols x 4 rows
      float craftStartX = px + 980.0F * scale;
      float craftColW = 448.0F * scale;
      float craftRowH = 132.0F * scale;
      int baseRecipe = cat * 8;
      for (int i = 0; i < 8; i++) {
         int recipeIdx = baseRecipe + i;
         if (recipeIdx >= PeWorld.RECIPES.length) break;
         int c = i & 1;
         int r = i >> 1;
         float bx = craftStartX + c * (craftColW + 18.0F * scale);
         float by = startY + r * (craftRowH + 16.0F * scale);
         if (inRect(x, y, bx, by, craftColW, craftRowH)) {
            world.tryCraftRecipe(recipeIdx, audio);
            return;
         }
      }
   }

   public void resetPointers(PeWorld world) {
      this.movePointerId = -1;
      this.lookPointerId = -1;
      this.jumpPointerId = -1;
      this.attackBtnPointerId = -1;
      this.moveForward = 0.0F;
      this.moveStrafe = 0.0F;
      this.jumpHeld = false;
      this.attackHeld = false;
      this.miningHeld = false;
      if (world != null) {
         world.sprinting = false;
      }
   }

   private static boolean inRect(float x, float y, float rx, float ry, float rw, float rh) {
      return x >= rx && x <= rx + rw && y >= ry && y <= ry + rh;
   }

   private static float dist(float x0, float y0, float x1, float y1) {
      float dx = x0 - x1;
      float dy = y0 - y1;
      return (float)Math.sqrt(dx * dx + dy * dy);
   }
}
