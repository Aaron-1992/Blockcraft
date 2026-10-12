package com.blockcraft.pe;

import android.content.Context;
import android.view.MotionEvent;

/**
 * Multi-Touch Pocket Edition Controller tailored for the Samsung Galaxy A55 5G
 * (2340x1080 19.5:9 landscape display with punch-hole camera safe area).
 */
public final class PeTouchController {
   public int screenW = 2340;
   public int screenH = 1080;

   // Safe area inset for hole-punch camera in landscape
   public float safeMarginX = 72.0F;

   // Movement output (-1..+1)
   public float moveForward = 0.0F;
   public float moveStrafe = 0.0F;
   public boolean jumpHeld = false;
   public boolean sneakHeld = false;
   public boolean attackHeld = false;
   public boolean miningHeld = false;
   public boolean placeTapped = false;
   public boolean attackTapped = false;

   // Control mode & overlays
   public boolean useAnalogStick = false;
   public boolean showModeMenu = false;
   public boolean showPaletteMenu = false;

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
   private boolean lookMovedSignificantly = false;
   public boolean touchMiningActive = false;
   public float touchCircleX = 0.0F;
   public float touchCircleY = 0.0F;

   private int jumpPointerId = -1;
   private int attackBtnPointerId = -1;
   private long lastJumpTapMs = 0L;

   // Accumulated camera deltas
   private float accumYawDelta = 0.0F;
   private float accumPitchDelta = 0.0F;

   // Layout geometry (computed in updateLayout)
   public float dpadCenterX, dpadCenterY, dpadRadius;
   public float jumpBtnX, jumpBtnY, jumpBtnR;
   public float atkBtnX, atkBtnY, atkBtnR;
   public float useBtnX, useBtnY, useBtnR;
   public float hotbarX, hotbarY, hotbarSlotSize;

   public PeTouchController() {
      updateLayout(2340, 1080);
   }

   public void updateLayout(int w, int h) {
      this.screenW = Math.max(640, w);
      this.screenH = Math.max(360, h);
      float scale = this.screenH / 1080.0F;
      this.safeMarginX = 68.0F * scale;

      this.dpadRadius = 175.0F * scale;
      this.dpadCenterX = this.safeMarginX + this.dpadRadius + 24.0F * scale;
      this.dpadCenterY = this.screenH - this.dpadRadius - 44.0F * scale;

      this.jumpBtnR = 72.0F * scale;
      this.jumpBtnX = this.screenW - this.safeMarginX - this.jumpBtnR - 32.0F * scale;
      this.jumpBtnY = this.screenH - this.jumpBtnR - 64.0F * scale;

      this.atkBtnR = 68.0F * scale;
      this.atkBtnX = this.jumpBtnX - 158.0F * scale;
      this.atkBtnY = this.jumpBtnY + 12.0F * scale;

      this.useBtnR = 62.0F * scale;
      this.useBtnX = this.jumpBtnX - 24.0F * scale;
      this.useBtnY = this.jumpBtnY - 156.0F * scale;

      this.hotbarSlotSize = 84.0F * scale;
      float totalHotbarW = this.hotbarSlotSize * 10.0F; // 9 slots + 1 [...] palette button
      this.hotbarX = (this.screenW - totalHotbarW) * 0.5F;
      this.hotbarY = this.screenH - this.hotbarSlotSize - 16.0F * scale;
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

   public synchronized void onTouchEvent(MotionEvent ev, Context context, PeWorld world, PeAudio audio) {
      int action = ev.getActionMasked();
      int idx = ev.getActionIndex();
      int pid = ev.getPointerId(idx);
      float x = ev.getX(idx);
      float y = ev.getY(idx);
      long now = System.currentTimeMillis();

      switch (action) {
         case MotionEvent.ACTION_DOWN:
         case MotionEvent.ACTION_POINTER_DOWN: {
            // 1. Check Modal Menus first (Gamemode Selector or Block Palette)
            if (this.showModeMenu) {
               handleModeMenuTap(x, y, context, world, audio);
               return;
            }
            if (this.showPaletteMenu) {
               handlePaletteMenuTap(x, y, world, audio);
               return;
            }

            // 2. Check Top Action Bar buttons
            if (y <= 88.0F * (this.screenH / 1080.0F)) {
               if (handleTopBarTap(x, y, context, world, audio)) {
                  return;
               }
            }

            // 3. Check Bottom 9-Slot Hotbar + [...] Palette button
            if (y >= this.hotbarY && y <= this.hotbarY + this.hotbarSlotSize
               && x >= this.hotbarX && x <= this.hotbarX + this.hotbarSlotSize * 10.0F) {
               int slot = (int)((x - this.hotbarX) / this.hotbarSlotSize);
               if (slot >= 0 && slot < 9) {
                  world.selectedSlot = slot;
                  if (audio != null) audio.playSfx("click", 0.7F, 1.0F);
               } else if (slot == 9) {
                  this.showPaletteMenu = !this.showPaletteMenu;
                  if (audio != null) audio.playSfx("click", 0.8F, 1.15F);
               }
               return;
            }

            // 4. Check Right-side Action Buttons (Jump, Attack/Fire, Place/Use)
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
               this.attackHeld = true;
               this.miningHeld = true;
               this.attackTapped = true;
               return;
            }
            if (dist(x, y, this.useBtnX, this.useBtnY) <= this.useBtnR * 1.18F) {
               this.placeTapped = true;
               return;
            }

            // 5. Check Left Movement Pad (D-Pad / Analog Stick)
            if (x < this.screenW * 0.42F && y > this.screenH * 0.35F && this.movePointerId == -1) {
               this.movePointerId = pid;
               this.moveCurX = x;
               this.moveCurY = y;
               updateMoveVector(x, y, world);
               return;
            }

            // 6. Otherwise: Right/Center Touch-Look & Touch-Circle Mining
            if (this.lookPointerId == -1) {
               this.lookPointerId = pid;
               this.lookStartX = x;
               this.lookStartY = y;
               this.lookLastX = x;
               this.lookLastY = y;
               this.lookDownTimeMs = now;
               this.lookMovedSignificantly = false;
               this.touchCircleX = x;
               this.touchCircleY = y;
            }
            break;
         }

         case MotionEvent.ACTION_MOVE: {
            int count = ev.getPointerCount();
            for (int i = 0; i < count; i++) {
               int id = ev.getPointerId(i);
               float px = ev.getX(i);
               float py = ev.getY(i);
               if (id == this.movePointerId) {
                  this.moveCurX = px;
                  this.moveCurY = py;
                  updateMoveVector(px, py, world);
               } else if (id == this.lookPointerId) {
                  float dx = px - this.lookLastX;
                  float dy = py - this.lookLastY;
                  this.lookLastX = px;
                  this.lookLastY = py;
                  this.touchCircleX = px;
                  this.touchCircleY = py;

                  float sens = 0.22F * (1080.0F / this.screenH);
                  this.accumYawDelta += dx * sens;
                  this.accumPitchDelta += dy * sens;

                  if (dist(px, py, this.lookStartX, this.lookStartY) > 22.0F) {
                     this.lookMovedSignificantly = true;
                  }
                  if (now - this.lookDownTimeMs > 240L) {
                     this.touchMiningActive = true;
                     this.attackHeld = true;
                     this.miningHeld = true;
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
               this.sneakHeld = false;
               world.sneaking = false;
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
               if (!this.lookMovedSignificantly && heldMs < 240L) {
                  this.attackTapped = true;
               }
               this.lookPointerId = -1;
               this.touchMiningActive = false;
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
         if (Math.abs(dx) < 0.30F && Math.abs(dy) < 0.30F) {
            this.moveForward = 0.0F;
            this.moveStrafe = 0.0F;
            this.sneakHeld = true;
            world.sneaking = true;
            world.sprinting = false;
            return;
         }
         this.sneakHeld = false;
         world.sneaking = false;
         this.moveStrafe = Math.abs(dx) > 0.28F ? (dx > 0 ? 1.0F : -1.0F) : 0.0F;
         this.moveForward = Math.abs(dy) > 0.28F ? (dy < 0 ? 1.0F : -1.0F) : 0.0F;
         world.sprinting = (this.moveForward > 0.5F);
      } else {
         this.sneakHeld = false;
         world.sneaking = false;
         float len = (float)Math.sqrt(dx * dx + dy * dy);
         if (len < 0.14F) {
            this.moveForward = 0.0F;
            this.moveStrafe = 0.0F;
            world.sprinting = false;
         } else {
            float scale = Math.min(1.0F, len);
            this.moveStrafe = (dx / len) * scale;
            this.moveForward = (-dy / len) * scale;
            world.sprinting = (this.moveForward > 0.72F);
         }
      }
   }

   private boolean handleTopBarTap(float x, float y, Context context, PeWorld world, PeAudio audio) {
      float scale = this.screenH / 1080.0F;
      float bx = this.safeMarginX;
      float bw = 310.0F * scale;
      float gap = 16.0F * scale;

      // Button 1: [GAMEMODE: ...]
      if (x >= bx && x <= bx + bw) {
         this.showModeMenu = !this.showModeMenu;
         this.showPaletteMenu = false;
         if (audio != null) audio.playSfx("click", 0.85F, 1.0F);
         return true;
      }
      bx += bw + gap;

      // Button 2: [MAP: ...]
      if (x >= bx && x <= bx + bw) {
         int nextMap = (world.mapId + 1) & 7;
         world.generateWorld(context, world.mode, nextMap);
         if (audio != null) audio.playSfx("click", 0.85F, 1.1F);
         return true;
      }
      bx += bw + gap;

      // Button 3: [NEW ROUND / RESPAWN]
      float rw = 240.0F * scale;
      if (x >= bx && x <= bx + rw) {
         world.generateWorld(context, world.mode, world.mapId);
         if (audio != null) audio.playSfx("levelup", 0.85F, 1.0F);
         return true;
      }
      bx += rw + gap;

      // Button 4: [D-PAD / STICK]
      float cw = 230.0F * scale;
      if (x >= bx && x <= bx + cw) {
         this.useAnalogStick = !this.useAnalogStick;
         if (audio != null) audio.playSfx("click", 0.80F, 1.2F);
         return true;
      }
      return false;
   }

   private void handleModeMenuTap(float x, float y, Context context, PeWorld world, PeAudio audio) {
      float scale = this.screenH / 1080.0F;
      float panelW = 1160.0F * scale;
      float panelH = 740.0F * scale;
      float px = (this.screenW - panelW) * 0.5F;
      float py = (this.screenH - panelH) * 0.5F;

      if (x < px || x > px + panelW || y < py || y > py + panelH) {
         this.showModeMenu = false;
         return;
      }

      float colW = (panelW - 72.0F * scale) * 0.5F;
      float rowH = 124.0F * scale;
      float startY = py + 120.0F * scale;

      for (int m = 0; m < PeWorld.MODE_NAMES.length; m++) {
         int col = m & 1;
         int row = m >> 1;
         float bx = px + 24.0F * scale + col * (colW + 24.0F * scale);
         float by = startY + row * (rowH + 18.0F * scale);
         if (x >= bx && x <= bx + colW && y >= by && y <= by + rowH) {
            this.showModeMenu = false;
            world.generateWorld(context, m, world.mapId);
            if (audio != null) audio.playSfx("levelup", 0.9F, 1.0F);
            return;
         }
      }
   }

   private static final int[] PALETTE_ITEMS = new int[]{
      PeWorld.B_GRASS, PeWorld.B_DIRT, PeWorld.B_STONE, PeWorld.B_COBBLE,
      PeWorld.B_PLANKS_OAK, PeWorld.B_BRICKS, PeWorld.B_SANDSTONE, PeWorld.B_GLASS,
      PeWorld.B_WOOL_RED, PeWorld.B_WOOL_BLUE, PeWorld.B_WOOL_GREEN, PeWorld.B_WOOL_YELLOW,
      PeWorld.B_GLOWSTONE, PeWorld.B_TORCH, PeWorld.B_TNT, PeWorld.B_OBSIDIAN,
      PeWorld.B_BED_RED, PeWorld.B_STONEBRICK, PeWorld.B_END_STONE, PeWorld.B_SLIME_BLOCK,
      PeWorld.I_SWORD_DIAMOND, PeWorld.I_SWORD_IRON, PeWorld.I_BOW, PeWorld.I_MACHINE_GUN,
      PeWorld.I_PISTOL, PeWorld.I_SNIPER, PeWorld.I_SIEGE_CANNON, PeWorld.I_GOLDEN_APPLE
   };

   public static int[] getPaletteItems() {
      return PALETTE_ITEMS;
   }

   private void handlePaletteMenuTap(float x, float y, PeWorld world, PeAudio audio) {
      float scale = this.screenH / 1080.0F;
      float panelW = 1080.0F * scale;
      float panelH = 660.0F * scale;
      float px = (this.screenW - panelW) * 0.5F;
      float py = (this.screenH - panelH) * 0.5F;

      if (x < px || x > px + panelW || y < py || y > py + panelH) {
         this.showPaletteMenu = false;
         return;
      }

      int cols = 7;
      float cell = 128.0F * scale;
      float startX = px + (panelW - cols * cell) * 0.5F;
      float startY = py + 110.0F * scale;

      for (int i = 0; i < PALETTE_ITEMS.length; i++) {
         int c = i % cols;
         int r = i / cols;
         float bx = startX + c * cell;
         float by = startY + r * cell;
         if (x >= bx && x <= bx + cell && y >= by && y <= by + cell) {
            world.hotbarId[world.selectedSlot] = PALETTE_ITEMS[i];
            world.hotbarCount[world.selectedSlot] = 64;
            this.showPaletteMenu = false;
            if (audio != null) audio.playSfx("pop", 0.85F, 1.1F);
            return;
         }
      }
   }

   private void resetPointers(PeWorld world) {
      this.movePointerId = -1;
      this.lookPointerId = -1;
      this.jumpPointerId = -1;
      this.attackBtnPointerId = -1;
      this.moveForward = 0.0F;
      this.moveStrafe = 0.0F;
      this.jumpHeld = false;
      this.sneakHeld = false;
      this.attackHeld = false;
      this.miningHeld = false;
      this.touchMiningActive = false;
      if (world != null) {
         world.sneaking = false;
         world.sprinting = false;
      }
   }

   private static float dist(float x0, float y0, float x1, float y1) {
      float dx = x0 - x1;
      float dy = y0 - y1;
      return (float)Math.sqrt(dx * dx + dy * dy);
   }
}
