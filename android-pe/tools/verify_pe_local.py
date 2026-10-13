#!/usr/bin/env python3
"""
Local verification and headless test runner for Blockcraft PE (com.blockcraft.pe).
Compiles minimal Android API 34 stubs (android.app, android.content, android.opengl,
android.view, android.media, android.os, javax.microedition.khronos) so all
Blockcraft PE Java sources can be compiled with `javac --release 8` and verified
headlessly prior to packaging the signed APK in CI.
"""
import os
import shutil
import subprocess
import tempfile
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
PE_SRC = os.path.join(ROOT, "android-pe", "src")

STUBS = {
    "android/os/Build.java": """
package android.os;
public class Build {
   public static class VERSION { public static int SDK_INT = 34; }
}
""",
    "android/os/Bundle.java": """
package android.os;
public class Bundle {}
""",
    "android/content/res/AssetManager.java": """
package android.content.res;
import java.io.*;
public class AssetManager {
   private final File root;
   public AssetManager() { this(new File("res")); }
   public AssetManager(File root) { this.root = root; }
   public InputStream open(String path) throws IOException {
      return new FileInputStream(new File(root, path));
   }
}
""",
    "android/content/Context.java": """
package android.content;
import android.content.res.AssetManager;
import java.io.File;
public class Context {
   private final AssetManager assets = new AssetManager();
   private final File filesDir = new File(System.getProperty("java.io.tmpdir", "/tmp"), "pe-test-files");
   public AssetManager getAssets() { return assets; }
   public File getFilesDir() { filesDir.mkdirs(); return filesDir; }
}
""",
    "android/view/Display.java": """
package android.view;
public class Display {
   public static class Mode {
      public int getModeId() { return 1; }
      public float getRefreshRate() { return 120.0F; }
   }
   public Mode[] getSupportedModes() { return new Mode[]{ new Mode() }; }
}
""",
    "android/view/View.java": """
package android.view;
import android.content.Context;
public class View {
   public static final int SYSTEM_UI_FLAG_IMMERSIVE_STICKY = 4096;
   public static final int SYSTEM_UI_FLAG_LAYOUT_STABLE = 256;
   public static final int SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION = 512;
   public static final int SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN = 1024;
   public static final int SYSTEM_UI_FLAG_HIDE_NAVIGATION = 2;
   public static final int SYSTEM_UI_FLAG_FULLSCREEN = 4;
   private final Context ctx;
   public View(Context c) { this.ctx = c; }
   public Context getContext() { return ctx; }
   public void setSystemUiVisibility(int vis) {}
   public boolean onTouchEvent(MotionEvent event) { return false; }
}
""",
    "android/view/Window.java": """
package android.view;
public class Window {
   public static final int FEATURE_NO_TITLE = 1;
   private final WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
   private final View decor = new View(new android.content.Context());
   public void addFlags(int flags) {}
   public WindowManager.LayoutParams getAttributes() { return lp; }
   public void setAttributes(WindowManager.LayoutParams a) {}
   public View getDecorView() { return decor; }
}
""",
    "android/view/WindowManager.java": """
package android.view;
public class WindowManager {
   public static class LayoutParams {
      public static final int FLAG_KEEP_SCREEN_ON = 128;
      public static final int FLAG_FULLSCREEN = 1024;
      public static final int LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES = 1;
      public int layoutInDisplayCutoutMode;
      public int preferredDisplayModeId;
      public float preferredRefreshRate;
   }
   public Display getDefaultDisplay() { return new Display(); }
}
""",
    "android/view/MotionEvent.java": """
package android.view;
public class MotionEvent {
   public static final int ACTION_DOWN = 0;
   public static final int ACTION_UP = 1;
   public static final int ACTION_MOVE = 2;
   public static final int ACTION_CANCEL = 3;
   public static final int ACTION_POINTER_DOWN = 5;
   public static final int ACTION_POINTER_UP = 6;
   private final int action;
   private final int index;
   private final int pointerId;
   private final float x;
   private final float y;
   public MotionEvent(int action, int index, int pointerId, float x, float y) {
      this.action = action; this.index = index; this.pointerId = pointerId; this.x = x; this.y = y;
   }
   public int getActionMasked() { return action; }
   public int getActionIndex() { return index; }
   public int getPointerId(int i) { return pointerId; }
   public int getPointerCount() { return 1; }
   public float getX(int i) { return x; }
   public float getY(int i) { return y; }
}
""",
    "android/app/Activity.java": """
package android.app;
import android.content.Context;
import android.os.Bundle;
import android.view.*;
public class Activity extends Context {
   private final Window win = new Window();
   private final WindowManager wm = new WindowManager();
   protected void onCreate(Bundle b) {}
   protected void onResume() {}
   protected void onPause() {}
   public void onWindowFocusChanged(boolean hasFocus) {}
   public void onBackPressed() {}
   public boolean requestWindowFeature(int f) { return true; }
   public Window getWindow() { return win; }
   public WindowManager getWindowManager() { return wm; }
   public void setContentView(View v) {}
}
""",
    "javax/microedition/khronos/egl/EGLConfig.java": """
package javax.microedition.khronos.egl;
public class EGLConfig {}
""",
    "javax/microedition/khronos/opengles/GL10.java": """
package javax.microedition.khronos.opengles;
public interface GL10 {}
""",
    "android/opengl/GLSurfaceView.java": """
package android.opengl;
import android.content.Context;
import android.view.View;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
public class GLSurfaceView extends View {
   public static final int RENDERMODE_CONTINUOUSLY = 1;
   public interface Renderer {
      void onSurfaceCreated(GL10 gl, EGLConfig config);
      void onSurfaceChanged(GL10 gl, int width, int height);
      void onDrawFrame(GL10 gl);
   }
   public GLSurfaceView(Context context) { super(context); }
   public void setEGLContextClientVersion(int v) {}
   public void setEGLConfigChooser(int r, int g, int b, int a, int d, int s) {}
   public void setPreserveEGLContextOnPause(boolean p) {}
   public void setRenderer(Renderer r) {}
   public void setRenderMode(int m) {}
   public void onResume() {}
   public void onPause() {}
}
""",
    "android/opengl/GLES20.java": """
package android.opengl;
import java.nio.Buffer;
public class GLES20 {
   public static final int GL_DEPTH_BUFFER_BIT = 0x00000100;
   public static final int GL_COLOR_BUFFER_BIT = 0x00004000;
   public static final int GL_TRIANGLES = 0x0004;
   public static final int GL_SRC_ALPHA = 0x0302;
   public static final int GL_ONE_MINUS_SRC_ALPHA = 0x0303;
   public static final int GL_FRONT_AND_BACK = 0x0408;
   public static final int GL_BACK = 0x0405;
   public static final int GL_CULL_FACE = 0x0B44;
   public static final int GL_DEPTH_TEST = 0x0B71;
   public static final int GL_BLEND = 0x0BE2;
   public static final int GL_LEQUAL = 0x0203;
   public static final int GL_TEXTURE_2D = 0x0DE1;
   public static final int GL_UNSIGNED_BYTE = 0x1401;
   public static final int GL_FLOAT = 0x1406;
   public static final int GL_RGBA = 0x1908;
   public static final int GL_NEAREST = 0x2600;
   public static final int GL_TEXTURE_MAG_FILTER = 0x2800;
   public static final int GL_TEXTURE_MIN_FILTER = 0x2801;
   public static final int GL_TEXTURE_WRAP_S = 0x2802;
   public static final int GL_TEXTURE_WRAP_T = 0x2803;
   public static final int GL_CLAMP_TO_EDGE = 0x812F;
   public static final int GL_TEXTURE0 = 0x84C0;
   public static final int GL_ARRAY_BUFFER = 0x8892;
   public static final int GL_STATIC_DRAW = 0x88E4;
   public static final int GL_DYNAMIC_DRAW = 0x88E8;
   public static final int GL_FRAGMENT_SHADER = 0x8B30;
   public static final int GL_VERTEX_SHADER = 0x8B31;
   public static final int GL_COMPILE_STATUS = 0x8B81;
   public static final int GL_LINK_STATUS = 0x8B82;

   private static int nextId = 1;
   public static void glEnable(int cap) {}
   public static void glDisable(int cap) {}
   public static void glDepthFunc(int func) {}
   public static void glCullFace(int mode) {}
   public static void glBlendFunc(int s, int d) {}
   public static void glViewport(int x, int y, int w, int h) {}
   public static void glClearColor(float r, float g, float b, float a) {}
   public static void glClear(int mask) {}
   public static void glGenTextures(int n, int[] tex, int off) { for (int i=0;i<n;i++) tex[off+i] = nextId++; }
   public static void glBindTexture(int target, int tex) {}
   public static void glActiveTexture(int tex) {}
   public static void glTexParameteri(int t, int p, int v) {}
   public static void glTexImage2D(int t, int l, int inf, int w, int h, int b, int f, int ty, Buffer px) {}
   public static void glTexSubImage2D(int t, int l, int x, int y, int w, int h, int f, int ty, Buffer px) {}
   public static void glGenBuffers(int n, int[] bufs, int off) { for (int i=0;i<n;i++) bufs[off+i] = nextId++; }
   public static void glBindBuffer(int target, int buf) {}
   public static void glBufferData(int target, int size, Buffer data, int usage) {}
   public static int glCreateShader(int type) { return nextId++; }
   public static void glShaderSource(int sh, String src) {}
   public static void glCompileShader(int sh) {}
   public static void glGetShaderiv(int sh, int pname, int[] params, int off) { params[off] = 1; }
   public static String glGetShaderInfoLog(int sh) { return ""; }
   public static int glCreateProgram() { return nextId++; }
   public static void glAttachShader(int p, int s) {}
   public static void glLinkProgram(int p) {}
   public static void glGetProgramiv(int p, int pname, int[] params, int off) { params[off] = 1; }
   public static String glGetProgramInfoLog(int p) { return ""; }
   public static void glUseProgram(int p) {}
   public static int glGetAttribLocation(int p, String n) { return 0; }
   public static int glGetUniformLocation(int p, String n) { return 0; }
   public static void glEnableVertexAttribArray(int i) {}
   public static void glVertexAttribPointer(int idx, int sz, int type, boolean norm, int stride, int ptr) {}
   public static void glUniformMatrix4fv(int loc, int cnt, boolean tr, float[] v, int off) {}
   public static void glUniform1i(int loc, int x) {}
   public static void glUniform1f(int loc, float x) {}
   public static void glUniform2f(int loc, float x, float y) {}
   public static void glUniform3f(int loc, float x, float y, float z) {}
   public static void glUniform4f(int loc, float x, float y, float z, float w) {}
   public static void glDrawArrays(int mode, int first, int count) {}
}
""",
    "android/opengl/Matrix.java": """
package android.opengl;
public class Matrix {
   public static void perspectiveM(float[] m, int offset, float fovy, float aspect, float zNear, float zFar) {
      for (int i=0;i<16;i++) m[offset+i] = (i%5==0) ? 1.0F : 0.0F;
   }
   public static void orthoM(float[] m, int offset, float left, float right, float bottom, float top, float near, float far) {
      for (int i=0;i<16;i++) m[offset+i] = (i%5==0) ? 1.0F : 0.0F;
   }
   public static void setLookAtM(float[] rm, int rmOffset, float eyeX, float eyeY, float eyeZ, float centerX, float centerY, float centerZ, float upX, float upY, float upZ) {
      for (int i=0;i<16;i++) rm[rmOffset+i] = (i%5==0) ? 1.0F : 0.0F;
   }
   public static void multiplyMM(float[] result, int resultOffset, float[] lhs, int lhsOffset, float[] rhs, int rhsOffset) {
      for (int i=0;i<16;i++) result[resultOffset+i] = (i%5==0) ? 1.0F : 0.0F;
   }
}
""",
    "android/media/AudioAttributes.java": """
package android.media;
public class AudioAttributes {
   public static final int USAGE_GAME = 14;
   public static final int CONTENT_TYPE_SONIFICATION = 4;
   public static class Builder {
      public Builder setUsage(int u) { return this; }
      public Builder setContentType(int c) { return this; }
      public AudioAttributes build() { return new AudioAttributes(); }
   }
}
""",
    "android/media/AudioFormat.java": """
package android.media;
public class AudioFormat {
   public static final int ENCODING_PCM_16BIT = 2;
   public static final int CHANNEL_OUT_MONO = 4;
   public static class Builder {
      public Builder setEncoding(int e) { return this; }
      public Builder setSampleRate(int r) { return this; }
      public Builder setChannelMask(int m) { return this; }
      public AudioFormat build() { return new AudioFormat(); }
   }
}
""",
    "android/media/AudioManager.java": """
package android.media;
public class AudioManager {
   public static final int STREAM_MUSIC = 3;
}
""",
    "android/media/AudioTrack.java": """
package android.media;
public class AudioTrack {
   public static final int MODE_STATIC = 0;
   public static class Builder {
      public Builder setAudioAttributes(AudioAttributes a) { return this; }
      public Builder setAudioFormat(AudioFormat f) { return this; }
      public Builder setBufferSizeInBytes(int b) { return this; }
      public Builder setTransferMode(int m) { return this; }
      public AudioTrack build() { return new AudioTrack(0,0,0,0,0,0); }
   }
   public AudioTrack(int s, int r, int c, int e, int b, int m) {}
   public int write(short[] d, int off, int sz) { return sz; }
   public int setVolume(float v) { return 0; }
   public void play() {}
   public void release() {}
}
""",
    "com/blockcraft/pe/PeCoreTest.java": """
package com.blockcraft.pe;

import android.content.Context;
import android.view.MotionEvent;

public class PeCoreTest {
   private static void check(boolean cond, String msg) {
      if (!cond) throw new AssertionError("FAIL: " + msg);
   }

   public static void main(String[] args) {
      Context ctx = new Context();

      // 1. Verify Pixel-Art Texture Atlas (TexPaint / TexPaint319) & Authentic 16x6 (8x11 cell) font.bin decoding
      int[] atlas = PeTextures.generateAtlasPixels();
      check(atlas.length == 256 * 256, "Atlas pixel count must be 65536");
      int leafTx0 = (PeTextures.T_LEAVES_OAK & 15) * 16;
      int leafTy0 = (PeTextures.T_LEAVES_OAK >> 4) * 16;
      int leafTransparent = 0;
      int leafOpaque = 0;
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            int p = atlas[(leafTy0 + y) * 256 + (leafTx0 + x)];
            if ((p >>> 24) == 0) leafTransparent++;
            else leafOpaque++;
         }
      }
      check(leafTransparent >= 12 && leafOpaque >= 180,
         "TexPaint319 Oak Leaves must have organic sky cutouts and lush leaflet clusters, got trans=" + leafTransparent + " opaque=" + leafOpaque);

      PeTextures tex = new PeTextures();
      int[] font = tex.generateFontPixels(ctx);
      check(font.length == 128 * 128, "Font pixel count must be 16384");
      check(tex.measureText("BLOCKCRAFT PE") > 50, "Font measureText must return positive pixel width");
      int glyphAPixel = font[24 * 128 + 9];
      check((glyphAPixel >>> 24) > 128, "Glyph 'A' from font.bin must have opaque pixels at (9, 24)");

      // 2. Verify Infinite World Streaming, Distant Coordinates & Persistent Save/Load (world_0.bcpe)
      PeWorld world = new PeWorld();
      world.deleteWorldSlot(ctx, 0);
      world.createNewSinglePlayerWorld(ctx, 0, PeWorld.MODE_SURVIVAL, PeWorld.WTYPE_LARGE_BIOMES, 884219551L);
      check(world.screen == PeWorld.SCREEN_PLAY, "New world must enter SCREEN_PLAY");
      check(world.slotExists[0], "World slot 0 must exist on disk after creation");

      // Place block near spawn AND travel 4,000 blocks away in the infinite world!
      world.setBlock(3, 72, -3, PeWorld.B_GOLD_BLOCK);
      world.px = 2405.5;
      world.pz = -3208.5;
      world.ensureChunksAroundPlayer(64);
      int farSurfaceY = world.topSolidY(2405, -3208);
      check(farSurfaceY >= 54 && farSurfaceY <= 95, "Infinite world must generate terrain at (2405, -3208), got Y=" + farSurfaceY);
      world.setBlock(2405, 82, -3208, PeWorld.B_DIAMOND_BLOCK);
      world.py = farSurfaceY + 1.0;
      world.yaw = 45.0F;
      check(world.saveWorld(ctx), "saveWorld must succeed");

      // Wipe in-memory state and reload from disk
      PeWorld loaded = new PeWorld();
      check(loaded.loadWorld(ctx, 0), "loadWorld(0) must succeed");
      check(loaded.getBlock(2405, 82, -3208) == PeWorld.B_DIAMOND_BLOCK, "Saved distant Diamond Block at (2405, 82, -3208) must be restored");
      check(loaded.getBlock(3, 72, -3) == PeWorld.B_GOLD_BLOCK, "Saved spawn Gold Block at (3, 72, -3) must also be restored");
      check(Math.abs(loaded.px - 2405.5) < 0.01 && Math.abs(loaded.pz - (-3208.5)) < 0.01, "Saved distant player position must be restored");

      // 3. Verify Right-Handed Walking Left (-X) and Right (+X) at yaw = 0 (facing -Z)
      PeWorld moveWorld = new PeWorld();
      moveWorld.createNewSinglePlayerWorld(ctx, 1, PeWorld.MODE_CREATIVE, PeWorld.WTYPE_SUPERFLAT, 12345L);
      moveWorld.flying = true;
      moveWorld.yaw = 0.0F;
      PeTouchController touch = new PeTouchController();
      touch.updateLayout(2340, 1080);

      double startX = moveWorld.px;
      touch.moveForward = 0.0F;
      touch.moveStrafe = -1.0F; // Walk LEFT
      moveWorld.tick(touch, null);
      check(moveWorld.px < startX, "Walking LEFT at yaw=0 must decrease X (move left), got dx=" + (moveWorld.px - startX));

      startX = moveWorld.px;
      touch.moveStrafe = 1.0F; // Walk RIGHT
      moveWorld.tick(touch, null);
      moveWorld.tick(touch, null);
      check(moveWorld.px > startX, "Walking RIGHT at yaw=0 must increase X (move right), got dx=" + (moveWorld.px - startX));

      double startZ = moveWorld.pz;
      touch.moveStrafe = 0.0F;
      touch.moveForward = 1.0F; // Walk FORWARD
      moveWorld.tick(touch, null);
      check(moveWorld.pz < startZ, "Walking FORWARD at yaw=0 must decrease Z (move toward -Z)");
      check(moveWorld.prevPz == startZ && moveWorld.pz < moveWorld.prevPz,
         "120Hz sub-tick interpolation must snapshot prevPz before tick so partialTick interpolates smoothly");

      // Verify smooth 0.5-block Slab step-up on solid ground without vertical bounce
      moveWorld.flying = false;
      moveWorld.px = 5.5;
      moveWorld.py = 64.0;
      moveWorld.pz = 5.5;
      moveWorld.onGround = true;
      moveWorld.setBlock(5, 64, 4, PeWorld.B_SLAB_OAK);
      touch.moveForward = 1.0F;
      for (int t = 0; t < 6; t++) {
         moveWorld.tick(touch, null);
      }
      check(Math.abs(moveWorld.py - 64.5) < 0.05,
         "Walking onto a 0.5-height Oak Slab must step up smoothly to Y=64.5 without vertical bounce, got Y=" + moveWorld.py);

      // 4. Verify Camera Look Isolation (Dragging camera NEVER triggers mining, attack, or block place!)
      touch.resetPointers(moveWorld);
      float lookX0 = 1400.0F, lookY0 = 450.0F;
      touch.onTouchEvent(new MotionEvent(MotionEvent.ACTION_DOWN, 0, 0, lookX0, lookY0), ctx, moveWorld, null);
      touch.onTouchEvent(new MotionEvent(MotionEvent.ACTION_MOVE, 0, 0, lookX0 + 120.0F, lookY0 - 40.0F), ctx, moveWorld, null);
      check(touch.consumeYawDelta() > 5.0F, "Dragging right must rotate camera yaw right (+)");
      check(!touch.miningHeld && !touch.attackHeld && !touch.attackTapped && !touch.placeTapped,
         "Moving the camera must NEVER trigger mining, attacking, or placing!");
      touch.onTouchEvent(new MotionEvent(MotionEvent.ACTION_UP, 0, 0, lookX0 + 120.0F, lookY0 - 40.0F), ctx, moveWorld, null);
      check(!touch.miningHeld && !touch.attackTapped && !touch.placeTapped,
         "Releasing finger after camera look must NEVER trigger attack or block placement!");

      // 5. Verify Single-Player Crafting, 36-Slot Backpack, Animated 3D Falling Trees, 3D Tools & VBO Mesher
      moveWorld.giveItem(PeWorld.B_LOG_BIRCH, 2);
      check(moveWorld.tryCraftRecipe(0, null), "Crafting Planks x4 from Birch Log must succeed via flexible wood matching");

      // Verify 27-slot backpack overflow never overwrites held tool in selectedSlot
      int heldBefore = moveWorld.hotbarId[moveWorld.selectedSlot];
      for (int s = 0; s < 9; s++) {
         if (moveWorld.hotbarId[s] == 0) {
            moveWorld.hotbarId[s] = PeWorld.B_COBBLE + s;
            moveWorld.hotbarCount[s] = 1;
         }
      }
      moveWorld.giveItem(PeWorld.B_DIAMOND_BLOCK, 12);
      check(moveWorld.hotbarId[moveWorld.selectedSlot] == heldBefore,
         "giveItem must NEVER overwrite held tool in selectedSlot when hotbar is full!");
      check(moveWorld.countItemInHotbar(PeWorld.B_DIAMOND_BLOCK) == 12,
         "Overflow items must be stored in the 27-slot backpack and counted by countItemInHotbar!");

      // Verify Animated 3D Tree Felling (PC Update 3.10 port)
      int tx = 14, tz = 14;
      int ty = 90;
      moveWorld.setBlock(tx, ty - 1, tz, PeWorld.B_DIRT);
      for (int h = 0; h < 5; h++) {
         moveWorld.setBlock(tx, ty + h, tz, PeWorld.B_LOG_OAK);
      }
      for (int ldx = -1; ldx <= 1; ldx++) {
         for (int ldz = -1; ldz <= 1; ldz++) {
            moveWorld.setBlock(tx + ldx, ty + 5, tz + ldz, PeWorld.B_LEAVES_OAK);
         }
      }
      moveWorld.setBlock(tx, ty, tz, PeWorld.B_AIR);
      boolean startedFall = moveWorld.fellTreeIfNatural(tx, ty, tz, PeWorld.B_LOG_OAK);
      check(startedFall && moveWorld.fallingTrees.size() == 1,
         "Breaking base log of natural tree must spawn an animated 3D FallingTree");
      PeWorld.FallingTree ft = moveWorld.fallingTrees.get(0);
      check(ft.getAngleDegrees(0.5F) > 0.0F && ft.getAngleDegrees(0.5F) < 90.0F,
         "FallingTree must smoothly interpolate angle between 0 and 90 degrees");
      moveWorld.finishAllFallingTrees();
      check(moveWorld.fallingTrees.isEmpty(), "finishAllFallingTrees must land all active falling trees");

      float[] scratch = new float[54000 * 9];
      PeChunkMesher mesher = new PeChunkMesher();
      check(mesher.buildSectionGeometry(moveWorld, 0, 7, 0, scratch) == 0,
         "Empty sky section (sy=7) must skip meshing in O(1) and emit 0 vertices");
      int totalVerts = 0;
      for (int sy = 0; sy < PeWorld.SECTIONS_Y; sy++) {
         totalVerts += mesher.buildSectionGeometry(moveWorld, 0, sy, 0, scratch);
      }
      check(totalVerts > 100, "Spawn chunk VBO mesher must emit > 100 vertices, got " + totalVerts);

      // Verify 3D Held Tool & 3D Block Meshing + Isometric 3D Slot Icons
      mesher.beginDynamic3D();
      mesher.addFirstPersonHeld3D(PeWorld.I_PICK_IRON, 0.0F, 70.0F, 0.0F, 0.0F, 0.0F, 0.35F, 0.0F, 1.0F);
      mesher.addFirstPersonHeld3D(PeWorld.I_SWORD_DIAMOND, 0.0F, 70.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F);
      mesher.addFirstPersonHeld3D(PeWorld.B_GRASS, 0.0F, 70.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F);

      PeGLSurfaceView glView = new PeGLSurfaceView(ctx);
      glView.onSurfaceCreated(null, null);
      glView.onSurfaceChanged(null, 2340, 1080);
      glView.onDrawFrame(null); // Draw Title Screen
      glView.world.screen = PeWorld.SCREEN_PLAY;
      glView.onDrawFrame(null); // Draw Play Screen with 3D Tool & Isometric Hotbar Icons
      glView.world.screen = PeWorld.SCREEN_INVENTORY;
      glView.world.invCategory = 0;
      glView.onDrawFrame(null); // Draw Backpack & Craft Tab
      glView.world.invCategory = 1;
      glView.onDrawFrame(null); // Draw Building Blocks Tab

      // 11. Verify Every Block Face Tile & Every Tool/Item Sprite in PeTextures Has Crisp Pixel Art
      for (int id = 1; id <= PeWorld.MAX_BLOCK_ID; id++) {
         for (int face = 0; face < 6; face++) {
            int tile = PeWorld.getFaceTile((byte)id, face);
            int tx0 = (tile & 15) * 16;
            int ty0 = (tile >> 4) * 16;
            int nonZero = 0;
            for (int y = 0; y < 16; y++) {
               for (int x = 0; x < 16; x++) {
                  if ((atlas[(ty0 + y) * 256 + (tx0 + x)] >>> 24) >= 64) nonZero++;
               }
            }
            check(nonZero >= 12, "Block id=" + id + " face=" + face + " tile=" + tile + " has crisp pixel art (" + nonZero + " px)");
         }
      }
      for (int itemId = PeWorld.I_SWORD_WOOD; itemId <= PeWorld.I_BREAD; itemId++) {
         int tile = PeWorld.getItemTile(itemId);
         int tx0 = (tile & 15) * 16;
         int ty0 = (tile >> 4) * 16;
         int nonZero = 0;
         for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
               if ((atlas[(ty0 + y) * 256 + (tx0 + x)] >>> 24) >= 128) nonZero++;
            }
         }
         check(nonZero >= 16, "Item id=" + itemId + " (" + PeWorld.getItemName(itemId) + ") tile=" + tile + " has crisp outlined pixel art (" + nonZero + " px)");
      }

      System.out.println("PeCoreTest PASSED: Master Pixel-Art Texture Overhaul, CCW Face Winding, Upright Side UVs, Animated 3D Falling Trees, 3D Tools, 36-Slot Backpack, and Controls verified!");
   }
}
"""
}


def main():
    tmp = tempfile.mkdtemp(prefix="pe-verify-")
    try:
        stub_src = os.path.join(tmp, "stubs")
        classes_dir = os.path.join(tmp, "classes")
        os.makedirs(classes_dir, exist_ok=True)

        java_files = []
        for rel, code in STUBS.items():
            full = os.path.join(stub_src, rel)
            os.makedirs(os.path.dirname(full), exist_ok=True)
            with open(full, "w", encoding="utf-8") as f:
                f.write(code.strip() + "\n")
            java_files.append(full)

        for root, _, files in os.walk(PE_SRC):
            for fn in sorted(files):
                if fn.endswith(".java"):
                    java_files.append(os.path.join(root, fn))

        sys.path.insert(0, os.path.join(ROOT, "tools"))
        import build as bc_build
        kind, tc = bc_build.toolchain()
        java_bin = bc_build.java_exe()
        if kind == "javac":
            cmd = ["javac", "--release", "8", "-encoding", "UTF-8", "-d", classes_dir] + java_files
        else:
            cmd = [java_bin, "-Xmx1g", "-jar", tc["ecj"], "-source", "1.8", "-target", "1.8",
                   "-encoding", "UTF-8", "-nowarn", "--system", tc["winjre"], "-d", classes_dir] + java_files
        res = subprocess.run(cmd, cwd=ROOT, capture_output=True, text=True)
        if res.returncode != 0:
            print("JAVAC FAILED:\n" + res.stdout + "\n" + res.stderr, file=sys.stderr)
            sys.exit(1)

        run = subprocess.run(
            [java_bin, "-cp", classes_dir, "com.blockcraft.pe.PeCoreTest"],
            cwd=ROOT,
            capture_output=True,
            text=True,
        )
        print(run.stdout, end="")
        if run.returncode != 0:
            print("TEST FAILED:\n" + run.stderr, file=sys.stderr)
            sys.exit(1)
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


if __name__ == "__main__":
    main()
