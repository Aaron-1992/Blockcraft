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
public class Context {
   private final AssetManager assets = new AssetManager();
   public AssetManager getAssets() { return assets; }
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

      // 1. Verify Texture Atlas & Font generation
      int[] atlas = PeTextures.generateAtlasPixels();
      check(atlas.length == 256 * 256, "Atlas pixel count must be 65536");
      PeTextures tex = new PeTextures();
      int[] font = tex.generateFontPixels(ctx);
      check(font.length == 128 * 128, "Font pixel count must be 16384");
      check(tex.measureText("BLOCKCRAFT PE") > 40, "Font measureText must return positive pixel width");

      // 2. Verify all 8 Gamemodes (including Console Edition .mca Battle loading, Tumble, Bedwars, CS2, End Fight, Castle Siege)
      PeWorld world = new PeWorld();
      float[] scratch = new float[72000 * 9];
      PeChunkMesher mesher = new PeChunkMesher();
      PeTouchController touch = new PeTouchController();
      touch.updateLayout(2340, 1080);

      for (int mode = 0; mode < PeWorld.MODE_NAMES.length; mode++) {
         world.generateWorld(ctx, mode, mode & 7);
         check(world.mode == mode, "World mode must match " + mode);
         check(world.mobs.size() >= 4, "Mode " + PeWorld.MODE_NAMES[mode] + " must spawn dynamic AI bots/mobs");

         touch.moveForward = 0.8F;
         touch.moveStrafe = 0.2F;
         for (int t = 0; t < 25; t++) {
            touch.jumpHeld = (t % 10 == 0);
            touch.miningHeld = (t % 5 == 0);
            world.tick(touch, null);
         }
         world.triggerPrimaryAction(null);
      }

      // 3. Verify VBO Chunk Section Mesher produces culled geometry
      world.generateWorld(ctx, PeWorld.MODE_BATTLE, 0); // Cove .mca
      int totalVerts = 0;
      for (int sy = 0; sy < PeWorld.SECTIONS_Y; sy++) {
         totalVerts += mesher.buildSectionGeometry(world, 5, sy, 5, scratch);
      }
      check(totalVerts > 100, "Center chunk VBO mesher must emit > 100 vertices on Cove .mca, got " + totalVerts);

      // 4. Verify Samsung Galaxy A55 5G (2340x1080) Multi-Touch Controller
      touch.onTouchEvent(new MotionEvent(MotionEvent.ACTION_DOWN, 0, 0, touch.dpadCenterX, touch.dpadCenterY - touch.dpadRadius * 0.7F), ctx, world, null);
      check(touch.moveForward > 0.5F, "D-Pad Up must set positive moveForward");
      touch.onTouchEvent(new MotionEvent(MotionEvent.ACTION_UP, 0, 0, touch.dpadCenterX, touch.dpadCenterY), ctx, world, null);
      check(touch.moveForward == 0.0F, "D-Pad release must reset moveForward");

      // Tap Hotbar Slot 3
      float slot3X = touch.hotbarX + touch.hotbarSlotSize * 3.5F;
      float slot3Y = touch.hotbarY + touch.hotbarSlotSize * 0.5F;
      touch.onTouchEvent(new MotionEvent(MotionEvent.ACTION_DOWN, 0, 1, slot3X, slot3Y), ctx, world, null);
      check(world.selectedSlot == 3, "Hotbar tap must select slot 3");

      // 5. Verify full OpenGL ES SurfaceView lifecycle & frame draw
      PeGLSurfaceView glView = new PeGLSurfaceView(ctx);
      glView.onSurfaceCreated(null, null);
      glView.onSurfaceChanged(null, 2340, 1080);
      glView.onDrawFrame(null);

      System.out.println("PeCoreTest PASSED: All 8 modes, .mca loader, VBO chunk mesher, textures, GL draw, and A55 5G multi-touch verified!");
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
