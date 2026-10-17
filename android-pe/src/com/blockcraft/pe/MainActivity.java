package com.blockcraft.pe;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

/**
 * Generic Android entry point for the standalone Blockcraft Pocket Edition.
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

      // Let the renderer use the full landscape viewport, including supported display cutouts.
      WindowManager.LayoutParams lp = window.getAttributes();
      if (Build.VERSION.SDK_INT >= 28) {
         lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
      }
      // Prefer the fastest mode exposed by this device, without assuming a model or refresh rate.
      requestBestSupportedRefreshRate(lp);
      window.setAttributes(lp);

      this.glView = new PeGLSurfaceView(this);
      setContentView(this.glView);
      applyImmersiveFullscreen();
   }

   private void requestBestSupportedRefreshRate(WindowManager.LayoutParams lp) {
      try {
         if (Build.VERSION.SDK_INT >= 23) {
            Display display = getWindowManager().getDefaultDisplay();
            Display.Mode[] modes = display.getSupportedModes();
            Display.Mode bestMode = null;
            float bestRate = 0.0F;
            for (int i = 0; i < modes.length; i++) {
               Display.Mode candidate = modes[i];
               if (candidate.getRefreshRate() > bestRate) {
                  bestRate = candidate.getRefreshRate();
                  bestMode = candidate;
               }
            }
            if (bestMode != null) {
               lp.preferredDisplayModeId = bestMode.getModeId();
               lp.preferredRefreshRate = bestRate;
            }
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
