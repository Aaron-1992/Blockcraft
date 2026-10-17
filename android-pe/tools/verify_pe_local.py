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
    "android/content/SharedPreferences.java": """
package android.content;
public interface SharedPreferences {
   boolean getBoolean(String key, boolean defValue);
   int getInt(String key, int defValue);
   Editor edit();
   interface Editor {
      Editor putBoolean(String key, boolean value);
      Editor putInt(String key, int value);
      void apply();
   }
}
""",
    "android/content/Context.java": """
package android.content;
import android.content.res.AssetManager;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
public class Context {
   public static final int MODE_PRIVATE = 0;
   private static final Map<String, MemoryPreferences> ALL_PREFERENCES = new HashMap<String, MemoryPreferences>();
   private final AssetManager assets = new AssetManager();
   private final File filesDir = new File(System.getProperty("java.io.tmpdir", "/tmp"), "pe-test-files");
   public AssetManager getAssets() { return assets; }
   public File getFilesDir() { filesDir.mkdirs(); return filesDir; }
   public synchronized SharedPreferences getSharedPreferences(String name, int mode) {
      MemoryPreferences prefs = ALL_PREFERENCES.get(name);
      if (prefs == null) { prefs = new MemoryPreferences(); ALL_PREFERENCES.put(name, prefs); }
      return prefs;
   }
   private static final class MemoryPreferences implements SharedPreferences {
      private final Map<String, Object> values = new HashMap<String, Object>();
      public boolean getBoolean(String key, boolean defValue) {
         Object value = values.get(key); return value instanceof Boolean ? ((Boolean)value).booleanValue() : defValue;
      }
      public int getInt(String key, int defValue) {
         Object value = values.get(key); return value instanceof Integer ? ((Integer)value).intValue() : defValue;
      }
      public Editor edit() {
         return new Editor() {
            private final Map<String, Object> pending = new HashMap<String, Object>();
            public Editor putBoolean(String key, boolean value) { pending.put(key, Boolean.valueOf(value)); return this; }
            public Editor putInt(String key, int value) { pending.put(key, Integer.valueOf(value)); return this; }
            public void apply() { values.putAll(pending); }
         };
      }
   }
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
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.zip.GZIPOutputStream;

public class PeCoreTest {
   private static void check(boolean cond, String msg) {
      if (!cond) throw new AssertionError("FAIL: " + msg);
   }

   private static int loadedChunkCount(PeWorld world) {
      int count = 0;
      for (int i = 0; i < PeWorld.ACTIVE_SLOTS; i++) {
         if (world.chunkGrid[i].loaded) count++;
      }
      return count;
   }

   public static void main(String[] args) throws Exception {
      Context ctx = new Context();
      check("1.11".equals(PeWorld.APP_VERSION), "PE source version must match the world-generation release");

      // 1. Verify World of Color block IDs, desktop palette mapping, names, and 16x16 atlased art.
      check(PeColors.ALL_BLOCKS.length == 96, "World of Color catalogue must contain 6 complete 16-block families");
      check(PeWorld.MAX_BLOCK_ID == 178, "Unsigned-byte block catalogue must include every color block and Lapis Ore through ID 178");
      check(PeWorld.migrateLegacyItemId(98) == PeWorld.I_SWORD_WOOD
            && PeWorld.migrateLegacyItemId(115) == PeWorld.I_BREAD
            && PeWorld.migrateLegacyItemId(89) == 89,
         "Legacy v1.8 item IDs must migrate without changing existing block IDs");
      boolean[] seenColorBlocks = new boolean[256];
      boolean[] seenColorTiles = new boolean[256];
      for (int family = 0; family < PeColors.FAMILY_BLOCKS.length; family++) {
         for (int color = 0; color < 16; color++) {
            int id = PeColors.FAMILY_BLOCKS[family][color];
            int tile = PeWorld.getItemTile(id);
            check(id > 0 && id <= PeWorld.MAX_BLOCK_ID && !seenColorBlocks[id],
               "Color blocks must use distinct positive unsigned IDs; duplicate/invalid ID=" + id);
            seenColorBlocks[id] = true;
            check(tile == PeColors.FAMILY_TILES[family][color] && tile >= 0 && tile < 256 && !seenColorTiles[tile],
               "Color family block must map to a unique in-range texture tile");
            seenColorTiles[tile] = true;
            check(PeColors.nameForBlock(id) != null && PeWorld.getItemName(id).equals(PeColors.nameForBlock(id)),
               "Color block must have a material-aware item name");
            check(PeWorld.isOpaque((byte)id) == (family != 4),
               "Only stained glass family blocks should be transparent");
         }
      }
      check(PeWorld.I_SWORD_WOOD > PeWorld.MAX_BLOCK_ID && PeWorld.I_BREAD > PeWorld.MAX_BLOCK_ID
            && PeWorld.I_DYE_WHITE > PeWorld.MAX_BLOCK_ID && PeWorld.I_DYE_BLACK > PeWorld.I_DYE_WHITE,
         "Non-block inventory items and dye IDs must not collide with unsigned byte block IDs");
      check(PeColors.DYE_ITEMS.length == 16, "The Colors UI must expose all 16 dye item variants");
      for (int color = 0; color < 16; color++) {
         int dye = PeColors.DYE_ITEMS[color];
         check(PeColors.dyeColorForItem(dye) == color
               && PeWorld.getItemName(dye).equals(PeColors.NAMES[color] + " Dye")
               && PeWorld.getItemTile(dye) == PeColors.FAMILY_TILES[0][color],
            "Every dye item must keep its color identity, readable name, and matching crisp swatch icon");
      }
      check(PeWorld.getFaceTile(PeWorld.B_LAPIS_ORE, 3) == PeTextures.T_LAPIS_ORE,
         "Lapis Ore must map to its own blue ore texture");

      // 2. Verify Pixel-Art Texture Atlas (TexPaint / TexPaint319) & Authentic 16x6 (8x11 cell) font.bin decoding
      int[] atlas = PeTextures.generateAtlasPixels();
      for (int family = 0; family < PeColors.FAMILY_TILES.length; family++) {
         for (int color = 0; color < 16; color++) {
            int tile = PeColors.FAMILY_TILES[family][color];
            int tx = (tile & 15) * 16;
            int ty = (tile >> 4) * 16;
            int painted = 0;
            for (int y = 0; y < 16; y++) {
               for (int x = 0; x < 16; x++) {
                  if (atlas[(ty + y) * 256 + tx + x] != 0) painted++;
               }
            }
            check(painted >= 180, "Each World of Color material tile must contain visible 16x16 art, family=" + family + " color=" + color);
         }
      }
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
      check(loadedChunkCount(world) == 81,
         "New worlds must synchronously warm only the inner 9x9 chunk area and stream the outer ring in later ticks");
      check(world.invId[6] == PeWorld.B_WOOL_WHITE && world.invCount[6] == 4,
         "Survival must provide a small wool base stack for the new color-crafting path");
      int riverBankedColumns = 0;
      for (int ci = 0; ci < PeWorld.ACTIVE_SLOTS; ci++) {
         PeWorld.PeChunk riverChunk = world.chunkGrid[ci];
         if (!riverChunk.loaded) continue;
         for (int lz = 4; lz < 12; lz++) for (int lx = 4; lx < 12; lx++) {
            int col = (lz << 4) | lx;
            int h = riverChunk.surfaceY[col];
            if (h != PeWorld.Y_SEA - 1
                  || riverChunk.blocks[(lz << 11) | (lx << 7) | PeWorld.Y_SEA] != PeWorld.B_WATER) continue;
            boolean raisedBank = false;
            for (int dz = -3; dz <= 3 && !raisedBank; dz++) {
               for (int dx = -3; dx <= 3; dx++) {
                  if (dx == 0 && dz == 0) continue;
                  int bankY = riverChunk.surfaceY[((lz + dz) << 4) | (lx + dx)];
                  if (bankY >= PeWorld.Y_SEA + 2 && bankY <= PeWorld.Y_SEA + 18) {
                     raisedBank = true;
                     break;
                  }
               }
            }
            if (raisedBank) riverBankedColumns++;
         }
      }
      check(riverBankedColumns > 0,
         "Fixed-seed overworld generation must include water-filled low channels with raised banks");

      // Worldgen caches are compact, reset with chunk reuse, and deterministic by chunk order.
      PeWorld.PeChunk originChunk = world.chunkGrid[PeWorld.slotIndex(0, 0)];
      int spawnBiome = world.getBiomeAt(7, 7);
      int spawnBiomeCell = ((7 >> 2) << 2) | (7 >> 2);
      check(originChunk.surfaceY.length == 256 && originChunk.biomeMap.length == 16
            && originChunk.biomeMap[spawnBiomeCell] == (byte)spawnBiome
            && originChunk.surfaceY[(7 << 4) | 7] > 0,
         "Generated chunks must retain a 16x16 heightmap and a populated coarse 4x4 biome cache");
      check(world.getBiomeAt(4, 4) == spawnBiome,
         "The same aligned 4x4 climate cell must return one cached biome");

      int negativeSite = PeWorld.structureSiteChunkX(-1, 0, world.seed);
      int positiveSite = PeWorld.structureSiteChunkX(0, 0, world.seed);
      check(negativeSite >= -5 && negativeSite <= -2 && positiveSite >= 1 && positiveSite <= 4,
         "Structure regions must place sites in bounded, deterministic chunks on both sides of zero");
      int landmarkX = Integer.MIN_VALUE;
      int landmarkZ = Integer.MIN_VALUE;
      byte landmarkType = PeWorld.STRUCTURE_NONE;
      for (int rx = 0; rx < 5 && landmarkType == PeWorld.STRUCTURE_NONE; rx++) {
         for (int rz = 0; rz < 5 && landmarkType == PeWorld.STRUCTURE_NONE; rz++) {
            int sx = PeWorld.structureSiteChunkX(rx, rz, world.seed);
            int sz = PeWorld.structureSiteChunkZ(rx, rz, world.seed);
            world.topSolidY(sx * 16 + 8, sz * 16 + 8);
            PeWorld.PeChunk candidate = world.chunkGrid[PeWorld.slotIndex(sx, sz)];
            if (candidate.cx == sx && candidate.cz == sz && candidate.structureType > PeWorld.STRUCTURE_STARTER_BLACKSMITH) {
               landmarkX = sx;
               landmarkZ = sz;
               landmarkType = candidate.structureType;
            }
         }
      }
      check(landmarkType != PeWorld.STRUCTURE_NONE,
         "Fixed-seed world generation must place at least one biome-aware landmark in nearby structure regions");
      PeWorld varietyWorld = new PeWorld();
      varietyWorld.seed = world.seed;
      varietyWorld.worldType = world.worldType;
      int varietyMask = 0;
      for (int rx = -10; rx <= 10 && (varietyMask & 0x1F0) != 0x1F0; rx++) {
         for (int rz = -10; rz <= 10 && (varietyMask & 0x1F0) != 0x1F0; rz++) {
            int sx = PeWorld.structureSiteChunkX(rx, rz, varietyWorld.seed);
            int sz = PeWorld.structureSiteChunkZ(rx, rz, varietyWorld.seed);
            varietyWorld.topSolidY(sx * 16 + 8, sz * 16 + 8);
            PeWorld.PeChunk candidate = varietyWorld.chunkGrid[PeWorld.slotIndex(sx, sz)];
            if (candidate.cx == sx && candidate.cz == sz) varietyMask |= 1 << candidate.structureType;
         }
      }
      check((varietyMask & 0x1F0) == 0x1F0,
         "Fixed-seed structure regions must generate desert, mountain, forest, snowy, and meadow landmark variants");
      PeWorld reorderedWorld = new PeWorld();
      reorderedWorld.seed = world.seed;
      reorderedWorld.worldType = world.worldType;
      int landmarkSurface = world.topSolidY(landmarkX * 16 + 8, landmarkZ * 16 + 8);
      int reorderedSurface = reorderedWorld.topSolidY(landmarkX * 16 + 8, landmarkZ * 16 + 8);
      PeWorld.PeChunk reorderedLandmark = reorderedWorld.chunkGrid[PeWorld.slotIndex(landmarkX, landmarkZ)];
      check(landmarkSurface == reorderedSurface && reorderedLandmark.structureType == landmarkType,
         "Biome-aware structures and terrain must be identical when the same chunk is generated first");

      // The streaming budget is strict and repeated calls must not reload an already resident chunk.
      PeWorld streamWorld = new PeWorld();
      streamWorld.ensureChunksAroundPlayer(0);
      check(loadedChunkCount(streamWorld) == 0, "A zero streaming budget must not load chunks");
      streamWorld.ensureChunksAroundPlayer(1);
      check(loadedChunkCount(streamWorld) == 1, "Chunk streaming must respect a one-chunk generation budget");
      streamWorld.ensureChunksAroundPlayer(1);
      check(loadedChunkCount(streamWorld) == 2, "Each streaming call may add at most one new chunk");

      // Evict an edited spawn chunk to exercise the compressed in-memory save cache.
      world.setBlock(3, 72, -3, PeWorld.B_GOLD_BLOCK);
      world.px = 256.5; // Chunk (16, -1) aliases the edited chunk's 16x16 ring-buffer slot.
      world.pz = -0.5;
      world.ensureChunksAroundPlayer(1);
      long savedChunkBytes = world.savedChunkStorageBytes();
      check(savedChunkBytes > 0 && savedChunkBytes < (PeWorld.CHUNK_BYTES * 3L) / 4L,
         "Evicted terrain chunks must use less than 75% of their raw memory footprint, bytes=" + savedChunkBytes);
      System.out.println("Modified-chunk memory snapshot: " + savedChunkBytes + " of " + PeWorld.CHUNK_BYTES + " bytes");

      // Travel 4,000 blocks away in the infinite world!
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
      long reloadedChunkBytes = loaded.savedChunkStorageBytes();
      check(reloadedChunkBytes > 0 && reloadedChunkBytes < (2L * PeWorld.CHUNK_BYTES * 3L) / 4L,
         "Raw save-file chunks must re-enter the compressed in-memory cache after loading");
      check(loadedChunkCount(loaded) == 81,
         "Loading a distant save must restore its inner working set and stream outer chunks incrementally");
      check(loaded.getBlock(2405, 82, -3208) == PeWorld.B_DIAMOND_BLOCK, "Saved distant Diamond Block at (2405, 82, -3208) must be restored");
      check(loaded.getBlock(3, 72, -3) == PeWorld.B_GOLD_BLOCK, "Saved spawn Gold Block at (3, 72, -3) must also be restored");
      check(Math.abs(loaded.px - 2405.5) < 0.01 && Math.abs(loaded.pz - (-3208.5)) < 0.01, "Saved distant player position must be restored");

      // A v1.8 (format v2) save used IDs 98..115 for items; it must migrate rather than become new blocks.
      File oldSave = new File(PeWorld.getSavesDir(ctx), "world_2.bcpe");
      DataOutputStream legacy = new DataOutputStream(new GZIPOutputStream(new FileOutputStream(oldSave)));
      legacy.writeInt(0x42435045); legacy.writeInt(2);
      legacy.writeInt(PeWorld.MODE_SURVIVAL); legacy.writeInt(PeWorld.WTYPE_SUPERFLAT);
      legacy.writeLong(55L); legacy.writeLong(0L); legacy.writeFloat(0.22F);
      legacy.writeDouble(0.5); legacy.writeDouble(66.0); legacy.writeDouble(0.5);
      legacy.writeFloat(0.0F); legacy.writeFloat(0.0F); legacy.writeBoolean(false);
      legacy.writeInt(20); legacy.writeInt(20); legacy.writeInt(0);
      for (int slot = 0; slot < 9; slot++) {
         legacy.writeInt(slot == 0 ? 98 : 0); legacy.writeInt(slot == 0 ? 1 : 0);
      }
      legacy.writeInt(0); legacy.writeInt(0x42414731);
      for (int slot = 0; slot < 27; slot++) {
         legacy.writeInt(slot == 0 ? 115 : 0); legacy.writeInt(slot == 0 ? 3 : 0);
      }
      legacy.close();
      PeWorld migrated = new PeWorld();
      check(migrated.loadWorld(ctx, 2), "legacy v1.8 save must remain loadable after the item-ID expansion");
      check(migrated.hotbarId[0] == PeWorld.I_SWORD_WOOD && migrated.invId[0] == PeWorld.I_BREAD
            && migrated.invCount[0] == 3,
         "legacy hotbar and backpack IDs must migrate to non-overlapping item IDs");

      // 3. Verify concrete powder hardens beside water and falls through open space.
      PeWorld colorWorld = new PeWorld();
      colorWorld.createNewSinglePlayerWorld(ctx, 1, PeWorld.MODE_CREATIVE, PeWorld.WTYPE_SUPERFLAT, 91L);
      int powderRed = PeColors.POWDER_BLOCKS[14];
      int concreteRed = PeColors.CONCRETE_BLOCKS[14];
      colorWorld.setBlock(6, 90, 6, (byte)powderRed);
      colorWorld.setBlock(7, 90, 6, PeWorld.B_WATER);
      colorWorld.tickConcretePowderAt(6, 90, 6);
      check(colorWorld.getBlock(6, 90, 6) == (byte)concreteRed,
         "red concrete powder must harden when water touches a side");
      colorWorld.setBlock(10, 90, 10, (byte)PeColors.POWDER_BLOCKS[0]);
      colorWorld.setBlock(10, 89, 10, PeWorld.B_AIR);
      colorWorld.tickConcretePowderAt(10, 90, 10);
      check(colorWorld.getBlock(10, 90, 10) == PeWorld.B_AIR
            && colorWorld.getBlock(10, 89, 10) == (byte)PeColors.POWDER_BLOCKS[0],
         "concrete powder must fall one block at a time through open space");

      // Survival dyes: gather base colors, mix secondary colors, then make colored wool/material batches.
      PeWorld dyeWorld = new PeWorld();
      dyeWorld.mode = PeWorld.MODE_SURVIVAL;
      dyeWorld.hotbarId[0] = PeWorld.B_FLOWER_RED; dyeWorld.hotbarCount[0] = 1;
      dyeWorld.hotbarId[1] = PeWorld.B_FLOWER_YELLOW; dyeWorld.hotbarCount[1] = 1;
      check(dyeWorld.tryCraftDye(14, null) && dyeWorld.tryCraftDye(4, null),
         "Red and Yellow Dye must be craftable from gathered flowers");
      check(dyeWorld.tryCraftDye(1, null) && dyeWorld.countItemInHotbar(PeWorld.I_DYE_ORANGE) == 2,
         "Desktop dye mixing must consume Red + Yellow and produce two Orange Dye");
      dyeWorld.hotbarId[2] = PeWorld.B_WOOL_WHITE; dyeWorld.hotbarCount[2] = 1;
      dyeWorld.hotbarId[3] = PeWorld.I_DYE_RED; dyeWorld.hotbarCount[3] = 1;
      check(dyeWorld.tryCraftColorBlock(0, 14, null)
            && dyeWorld.countItemInHotbar(PeColors.WOOL_BLOCKS[14]) == 1,
         "Survival Colors family must dye wool using a gathered base block and matching dye");

      PeWorld materialCraftWorld = new PeWorld();
      materialCraftWorld.mode = PeWorld.MODE_SURVIVAL;
      materialCraftWorld.hotbarId[0] = PeWorld.B_TERRACOTTA; materialCraftWorld.hotbarCount[0] = 4;
      materialCraftWorld.hotbarId[1] = PeColors.TERRACOTTA_BLOCKS[2]; materialCraftWorld.hotbarCount[1] = 4;
      materialCraftWorld.hotbarId[2] = PeWorld.I_DYE_RED; materialCraftWorld.hotbarCount[2] = 1;
      check(materialCraftWorld.tryCraftColorBlock(2, 14, null)
            && materialCraftWorld.countItemInHotbar(PeColors.TERRACOTTA_BLOCKS[14]) == 8,
         "Eight plain or colored terracotta plus one dye must craft eight selected-color terracotta blocks");
      materialCraftWorld.hotbarId[3] = PeWorld.B_SAND; materialCraftWorld.hotbarCount[3] = 4;
      materialCraftWorld.hotbarId[4] = PeWorld.B_GRAVEL; materialCraftWorld.hotbarCount[4] = 4;
      materialCraftWorld.hotbarId[5] = PeWorld.I_DYE_BLUE; materialCraftWorld.hotbarCount[5] = 1;
      check(materialCraftWorld.tryCraftColorBlock(3, 11, null)
            && materialCraftWorld.countItemInHotbar(PeColors.POWDER_BLOCKS[11]) == 8,
         "Four sand, four gravel, and a dye must craft eight concrete powder");

      // Two adjacent stained-glass blocks should not emit their shared internal face.
      int glassA = PeColors.STAINED_GLASS_BLOCKS[14];
      int glassB = PeColors.STAINED_GLASS_BLOCKS[13];
      colorWorld.setBlock(20, 90, 20, (byte)glassA);
      colorWorld.setBlock(21, 90, 20, (byte)glassB);
      int glassVertices = new PeChunkMesher().buildSectionGeometry(colorWorld, 1, 5, 1, new float[10000]);
      check(glassVertices == 60, "adjacent stained-glass blocks must cull the two shared face sets (expected 60 vertices)");
      check(colorWorld.saveWorld(ctx), "world with unsigned color-block IDs must save in the new format");
      PeWorld colorReloaded = new PeWorld();
      check(colorReloaded.loadWorld(ctx, 1), "world containing color blocks must reload");
      check(colorReloaded.getBlock(20, 90, 20) == (byte)glassA
            && colorReloaded.getBlock(21, 90, 20) == (byte)glassB
            && !PeWorld.isOpaque(colorReloaded.getBlock(20, 90, 20)),
         "signed-byte stained-glass IDs must survive save/load and remain transparent");

      // 4. Verify World of Color inventory selection and settings persistence/hitboxes.
      PeWorld uiWorld = new PeWorld();
      uiWorld.screen = PeWorld.SCREEN_INVENTORY;
      uiWorld.mode = PeWorld.MODE_CREATIVE;
      PeTouchController uiTouch = new PeTouchController();
      uiTouch.updateLayout(540, 360);
      check(Math.abs(uiTouch.menuScale() - (540.0F / 2340.0F)) < 0.001F,
         "touch menu scale must use the real narrow landscape viewport, matching renderer scaling");
      uiTouch.updateLayout(2340, 1080);
      uiTouch.onTouchEvent(new MotionEvent(MotionEvent.ACTION_DOWN, 0, 0, 1790.0F, 145.0F), ctx, uiWorld, null);
      check(uiWorld.invCategory == 4, "fifth inventory tab must open World of Color");
      uiTouch.onTouchEvent(new MotionEvent(MotionEvent.ACTION_DOWN, 0, 0, 1870.0F, 306.0F), ctx, uiWorld, null);
      check(uiWorld.colorFamily == 1, "right-side material family card must select Concrete");
      uiTouch.onTouchEvent(new MotionEvent(MotionEvent.ACTION_DOWN, 0, 0, 753.0F, 758.0F), ctx, uiWorld, null);
      check(uiWorld.hotbarId[uiWorld.selectedSlot] == concreteRed && uiWorld.hotbarCount[uiWorld.selectedSlot] == 64,
         "4x4 World of Color swatches must equip the tapped concrete color");
      uiTouch.onTouchEvent(new MotionEvent(MotionEvent.ACTION_DOWN, 0, 0, 1650.0F, 750.0F), ctx, uiWorld, null);
      check(uiWorld.colorFamily == 6, "wide bottom color selector must open the 16-dye collection");
      uiTouch.onTouchEvent(new MotionEvent(MotionEvent.ACTION_DOWN, 0, 0, 753.0F, 758.0F), ctx, uiWorld, null);
      check(uiWorld.hotbarId[uiWorld.selectedSlot] == PeWorld.I_DYE_RED
            && uiWorld.hotbarCount[uiWorld.selectedSlot] == 64,
         "Creative Dyes swatches must equip stackable dye items instead of block-family IDs");

      PeWorld survivalColorUI = new PeWorld();
      survivalColorUI.screen = PeWorld.SCREEN_INVENTORY;
      survivalColorUI.mode = PeWorld.MODE_SURVIVAL;
      survivalColorUI.invCategory = 4;
      survivalColorUI.colorFamily = 0;
      survivalColorUI.hotbarId[0] = PeWorld.B_WOOL_WHITE; survivalColorUI.hotbarCount[0] = 1;
      survivalColorUI.hotbarId[1] = PeWorld.I_DYE_RED; survivalColorUI.hotbarCount[1] = 1;
      uiTouch.onTouchEvent(new MotionEvent(MotionEvent.ACTION_DOWN, 0, 0, 753.0F, 758.0F), ctx, survivalColorUI, null);
      check(survivalColorUI.selectedColor == 14, "survival swatch tap must store the selected dye color");
      uiTouch.onTouchEvent(new MotionEvent(MotionEvent.ACTION_DOWN, 0, 0, 1400.0F, 306.0F), ctx, survivalColorUI, null);
      check(survivalColorUI.countItemInHotbar(PeColors.WOOL_BLOCKS[14]) == 1,
         "survival material-family card must execute its matching color recipe");
      survivalColorUI.hotbarId[2] = PeWorld.B_FLOWER_RED; survivalColorUI.hotbarCount[2] = 1;
      uiTouch.onTouchEvent(new MotionEvent(MotionEvent.ACTION_DOWN, 0, 0, 1650.0F, 750.0F), ctx, survivalColorUI, null);
      uiTouch.onTouchEvent(new MotionEvent(MotionEvent.ACTION_DOWN, 0, 0, 753.0F, 758.0F), ctx, survivalColorUI, null);
      check(survivalColorUI.countItemInHotbar(PeWorld.I_DYE_RED) == 1,
         "survival Dyes collection must craft a dye from gathered flowers through touch UI");

      uiWorld.screen = PeWorld.SCREEN_SETTINGS;
      uiTouch.onTouchEvent(new MotionEvent(MotionEvent.ACTION_DOWN, 0, 0, 1170.0F, 239.0F), ctx, uiWorld, null);
      check(!uiTouch.useAnalogStick, "visible Settings row hitbox must toggle the movement control");
      uiTouch.onTouchEvent(new MotionEvent(MotionEvent.ACTION_DOWN, 0, 0, 1170.0F, 345.0F), ctx, uiWorld, null);
      check(uiTouch.touchTapToInteract, "second Settings row hitbox must toggle stationary tap interaction");
      uiTouch.onTouchEvent(new MotionEvent(MotionEvent.ACTION_DOWN, 0, 0, 1170.0F, 451.0F), ctx, uiWorld, null);
      check(uiTouch.sensitivityLevel == 2, "third Settings row hitbox must advance look sensitivity");
      uiTouch.onTouchEvent(new MotionEvent(MotionEvent.ACTION_DOWN, 0, 0, 1170.0F, 557.0F), ctx, uiWorld, null);
      check(!uiWorld.autoJump, "fourth Settings row hitbox must toggle auto-jump");
      uiTouch.onTouchEvent(new MotionEvent(MotionEvent.ACTION_DOWN, 0, 0, 1170.0F, 663.0F), ctx, uiWorld, null);
      check(uiWorld.viewRadiusChunks == 6, "fifth Settings row hitbox must advance render distance");
      uiTouch.onTouchEvent(new MotionEvent(MotionEvent.ACTION_DOWN, 0, 0, 1170.0F, 769.0F), ctx, uiWorld, null);
      check(PeTextures.activePackMode == 1, "sixth Settings row hitbox must cycle resource packs");
      PeTouchController restoredSettings = new PeTouchController();
      restoredSettings.loadPreferences(ctx, uiWorld);
      check(!restoredSettings.useAnalogStick && restoredSettings.touchTapToInteract
            && restoredSettings.sensitivityLevel == 2 && !uiWorld.autoJump && uiWorld.viewRadiusChunks == 6
            && PeTextures.activePackMode == 1,
         "all visible control, performance, and resource-pack settings must persist between activity instances");
      uiTouch.onTouchEvent(new MotionEvent(MotionEvent.ACTION_DOWN, 0, 0, 1170.0F, 895.0F), ctx, uiWorld, null);
      check(uiWorld.screen == PeWorld.SCREEN_TITLE, "Settings Done button must return to the previous screen");
      PeTextures.activePackMode = 0;
      PeTextures.dirtyAtlas = false;

      // 5. Verify Right-Handed Walking Left (-X) and Right (+X) at yaw = 0 (facing -Z)
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

      // 6. Verify Camera Look Isolation (Dragging camera NEVER triggers mining, attack, or block place!)
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

      // 7. Verify Single-Player Crafting, 36-Slot Backpack, Animated 3D Falling Trees, 3D Tools & VBO Mesher
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
      PeChunkMesher budgetMesher = new PeChunkMesher();
      PeWorld.PeChunk scheduledChunk = moveWorld.chunkGrid[PeWorld.slotIndex(0, 0)];
      check(scheduledChunk.sectionDirty[0] && scheduledChunk.sectionDirty[1],
         "Newly generated chunk sections must start dirty for GPU meshing");
      budgetMesher.updateDirtySections(moveWorld, 0);
      check(scheduledChunk.sectionDirty[0], "A zero section-mesh budget must not rebuild or clear dirty sections");
      budgetMesher.updateDirtySections(moveWorld, 1);
      check(!scheduledChunk.sectionDirty[0] && scheduledChunk.sectionDirty[1],
         "The render scheduler must rebuild no more than one non-empty section per call");

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
      check(glView.world.screen == PeWorld.SCREEN_TITLE && loadedChunkCount(glView.world) <= 25,
         "The title panorama must use a small non-persistent preview instead of synchronously generating a full playable world");
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
      for (int itemId = PeWorld.I_SWORD_WOOD; itemId <= PeWorld.I_DYE_BLACK; itemId++) {
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

      System.out.println("PeCoreTest PASSED: World of Color, save migration, compressed chunk snapshots, infinite-world streaming, cached biome/height generation, river channels, deterministic biome-aware structures, bounded section meshing, gameplay controls, mobile UI, and pixel-art atlas verified!");
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
