package com.blockcraft.pe;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

/**
 * Blockcraft PE (Pocket Edition) Main Activity for Samsung Galaxy A55 5G
 * (Android 14 / One UI 6, 120Hz Super AMOLED 2340x1080 edge-to-edge landscape).
 */
public final class MainActivity extends Activity {
   private PeGLSurfaceView glView;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      requestWindowFeature(Window.FEATURE_NO_TITLE);
      Window window = getWindow();
      window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
      window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

      // Enable edge-to-edge display across the Samsung Galaxy A55 5G hole-punch camera cutout
      WindowManager.LayoutParams lp = window.getAttributes();
      if (Build.VERSION.SDK_INT >= 28) {
         lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
      }
      // Request 120Hz Super AMOLED refresh rate on Samsung Galaxy A55 5G
      request120HzDisplayMode(lp);
      window.setAttributes(lp);

      this.glView = new PeGLSurfaceView(this);
      setContentView(this.glView);
      applyImmersiveFullscreen();
   }

   private void request120HzDisplayMode(WindowManager.LayoutParams lp) {
      try {
         if (Build.VERSION.SDK_INT >= 23) {
            Display display = getWindowManager().getDefaultDisplay();
            Display.Mode[] modes = display.getSupportedModes();
            Display.Mode bestMode = null;
            float bestRate = 60.0F;
            for (int i = 0; i < modes.length; i++) {
               Display.Mode m = modes[i];
               if (m.getRefreshRate() > bestRate) {
                  bestRate = m.getRefreshRate();
                  bestMode = m;
               }
            }
            if (bestMode != null) {
               lp.preferredDisplayModeId = bestMode.getModeId();
            }
            lp.preferredRefreshRate = 120.0F;
         }
      } catch (Throwable ignored) {
      }
   }

   private void applyImmersiveFullscreen() {
      View decor = getWindow().getDecorView();
      decor.setSystemUiVisibility(
         View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN
      );
   }

   @Override
   public void onWindowFocusChanged(boolean hasFocus) {
      super.onWindowFocusChanged(hasFocus);
      if (hasFocus) {
         applyImmersiveFullscreen();
      }
   }

   @Override
   protected void onResume() {
      super.onResume();
      if (this.glView != null) {
         this.glView.onResume();
      }
      applyImmersiveFullscreen();
   }

   @Override
   protected void onPause() {
      if (this.glView != null) {
         if (this.glView.world != null) {
            this.glView.world.saveWorld(this);
         }
         this.glView.onPause();
      }
      super.onPause();
   }

   @Override
   @SuppressWarnings("deprecation")
   public void onBackPressed() {
      if (this.glView != null && this.glView.world != null) {
         PeWorld w = this.glView.world;
         if (w.screen == PeWorld.SCREEN_INVENTORY) {
            w.screen = PeWorld.SCREEN_PLAY;
            return;
         }
         if (w.screen == PeWorld.SCREEN_SETTINGS) {
            w.screen = w.previousScreen;
            return;
         }
         if (w.screen == PeWorld.SCREEN_PLAY) {
            w.saveWorld(this);
            w.screen = PeWorld.SCREEN_PAUSE;
            return;
         }
         if (w.screen == PeWorld.SCREEN_PAUSE) {
            w.screen = PeWorld.SCREEN_PLAY;
            return;
         }
         if (w.screen == PeWorld.SCREEN_CREATE_WORLD) {
            w.screen = PeWorld.SCREEN_WORLD_SELECT;
            return;
         }
         if (w.screen == PeWorld.SCREEN_WORLD_SELECT) {
            w.screen = PeWorld.SCREEN_TITLE;
            return;
         }
      }
      super.onBackPressed();
   }
}
