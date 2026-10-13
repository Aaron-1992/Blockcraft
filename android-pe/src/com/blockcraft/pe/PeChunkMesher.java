package com.blockcraft.pe;

import android.opengl.GLES20;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Blockcraft PE v1.1 — Infinite World Hardware VBO Chunk Mesher & Mobile UI Renderer
 * optimized for Samsung Galaxy A55 5G (Exynos 1480 / Xclipse 530 OpenGL ES 3.0 at 120Hz).
 *
 * Performance optimizations:
 * - O(1) empty-section skipping via PeChunk.sectionNonAir[sy]
 * - Direct byte[] column indexing ((lz << 11) | (lx << 7) | wy) with pre-fetched cardinal neighbor chunks
 * - Interior leaf-to-leaf face culling to eliminate tree canopy alpha overdraw
 * - View-cone & distance culling across the active 16x16 infinite chunk ring buffer
 */
public final class PeChunkMesher {
   private static final int FLOATS_PER_VERT_3D = 9; // x, y, z, u, v, r, g, b, a
   private static final int STRIDE_3D = FLOATS_PER_VERT_3D * 4;

   private static final int FLOATS_PER_VERT_2D = 8; // x, y, u, v, r, g, b, a
   private static final int STRIDE_2D = FLOATS_PER_VERT_2D * 4;

   private final int[][] vboIds = new int[PeWorld.ACTIVE_SLOTS][PeWorld.SECTIONS_Y];
   private final int[][] vertCounts = new int[PeWorld.ACTIVE_SLOTS][PeWorld.SECTIONS_Y];

   // Scratch buffer for building a 16x16x16 section VBO
   private final float[] scratch3D = new float[54000 * FLOATS_PER_VERT_3D];
   private final FloatBuffer direct3D;

   // Dynamic 3D buffer for Block Selection Box, Mining Crack Overlay & 1st-Person Held Block/Tool
   private int dyn3DVbo = 0;
   private final float[] dyn3DData = new float[4096 * FLOATS_PER_VERT_3D];
   private final FloatBuffer dyn3DBuf;
   private int dyn3DVerts = 0;

   // Dynamic 2D buffer for Mobile HUD, Circular Joystick, Action Pads, Hotbar & Menus
   private int dyn2DVbo = 0;
   private final float[] dyn2DData = new float[16000 * FLOATS_PER_VERT_2D];
   private final FloatBuffer dyn2DBuf;
   private int dyn2DVerts = 0;

   public PeChunkMesher() {
      this.direct3D = ByteBuffer.allocateDirect(scratch3D.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
      this.dyn3DBuf = ByteBuffer.allocateDirect(dyn3DData.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
      this.dyn2DBuf = ByteBuffer.allocateDirect(dyn2DData.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
   }

   public void initGL(PeWorld world) {
      int[] tmp = new int[2];
      GLES20.glGenBuffers(2, tmp, 0);
      this.dyn3DVbo = tmp[0];
      this.dyn2DVbo = tmp[1];

      for (int slot = 0; slot < PeWorld.ACTIVE_SLOTS; slot++) {
         GLES20.glGenBuffers(PeWorld.SECTIONS_Y, this.vboIds[slot], 0);
         for (int sy = 0; sy < PeWorld.SECTIONS_Y; sy++) {
            this.vertCounts[slot][sy] = 0;
            world.chunkGrid[slot].sectionDirty[sy] = true;
         }
      }
   }

   public void updateDirtySections(PeWorld world, int maxPerFrame) {
      int rebuilt = 0;
      int pCx = ((int)Math.floor(world.px)) >> 4;
      int pCz = ((int)Math.floor(world.pz)) >> 4;
      int rad = Math.max(3, Math.min(7, world.viewRadiusChunks));

      // Pass 0: immediate chunks (ring <= 2); Pass 1: outer visible chunks (ring <= rad)
      for (int pass = 0; pass < 2 && rebuilt < maxPerFrame; pass++) {
         for (int dz = -rad; dz <= rad && rebuilt < maxPerFrame; dz++) {
            for (int dx = -rad; dx <= rad && rebuilt < maxPerFrame; dx++) {
               int ring = Math.max(Math.abs(dx), Math.abs(dz));
               if ((pass == 0 && ring > 2) || (pass == 1 && ring <= 2)) continue;
               int cx = pCx + dx;
               int cz = pCz + dz;
               int slot = PeWorld.slotIndex(cx, cz);
               PeWorld.PeChunk chunk = world.chunkGrid[slot];
               if (!chunk.loaded || chunk.cx != cx || chunk.cz != cz) continue;

               for (int sy = 0; sy < PeWorld.SECTIONS_Y; sy++) {
                  if (chunk.sectionDirty[sy]) {
                     chunk.sectionDirty[sy] = false;
                     if (chunk.sectionNonAir[sy] <= 0) {
                        this.vertCounts[slot][sy] = 0;
                     } else {
                        rebuildSection(world, slot, cx, sy, cz);
                        rebuilt++;
                        if (rebuilt >= maxPerFrame) break;
                     }
                  }
               }
            }
         }
      }
   }

   private static PeWorld.PeChunk getLoadedNeighbor(PeWorld world, int cx, int cz) {
      PeWorld.PeChunk n = world.chunkGrid[PeWorld.slotIndex(cx, cz)];
      return (n.loaded && n.cx == cx && n.cz == cz) ? n : null;
   }

   public int buildSectionGeometry(PeWorld world, int cx, int sy, int cz, float[] out) {
      int slot = PeWorld.slotIndex(cx, cz);
      PeWorld.PeChunk chunk = world.chunkGrid[slot];
      if (!chunk.loaded || chunk.cx != cx || chunk.cz != cz || chunk.sectionNonAir[sy] <= 0) {
         return 0;
      }

      byte[] cb = chunk.blocks;
      PeWorld.PeChunk cWest = getLoadedNeighbor(world, cx - 1, cz);
      PeWorld.PeChunk cEast = getLoadedNeighbor(world, cx + 1, cz);
      PeWorld.PeChunk cNorth = getLoadedNeighbor(world, cx, cz - 1);
      PeWorld.PeChunk cSouth = getLoadedNeighbor(world, cx, cz + 1);
      byte[] bWest = cWest != null ? cWest.blocks : null;
      byte[] bEast = cEast != null ? cEast.blocks : null;
      byte[] bNorth = cNorth != null ? cNorth.blocks : null;
      byte[] bSouth = cSouth != null ? cSouth.blocks : null;

      int wx0 = cx << 4;
      int wy0 = sy << 4;
      int wz0 = cz << 4;
      int ptr = 0;
      int maxFloats = out.length - 54;

      for (int lz = 0; lz < 16; lz++) {
         int wz = wz0 + lz;
         int zOff = lz << 11;
         for (int lx = 0; lx < 16; lx++) {
            int wx = wx0 + lx;
            int colBase = zOff | (lx << 7);
            for (int ly = 0; ly < 16; ly++) {
               int wy = wy0 + ly;
               if (wy <= 0 || wy >= PeWorld.WORLD_H - 1) continue;
               int idx = colBase | wy;
               byte b = cb[idx];
               if (b == PeWorld.B_AIR) continue;
               if (ptr >= maxFloats) break;

               if (PeWorld.isCrossPlant(b)) {
                  int tile = PeWorld.getFaceTile(b, 0);
                  float cr = (b == PeWorld.B_TORCH) ? 1.0F : 0.92F;
                  float cg = (b == PeWorld.B_TORCH) ? 0.98F : 0.96F;
                  float cbCol = (b == PeWorld.B_TORCH) ? 0.88F : 0.90F;
                  ptr = emitCrossPlant(out, ptr, wx, wy, wz, tile, cr, cg, cbCol);
                  continue;
               }

               float boost = PeWorld.isLightEmitter(b) ? 0.28F : 0.0F;
               float yTop = (b == PeWorld.B_WATER || b == PeWorld.B_LAVA) ? (wy + 0.88F)
                  : (b == PeWorld.B_BED_RED ? (wy + 0.56F) : (wy + 1.0F));
               float alpha = (b == PeWorld.B_WATER) ? 0.78F
                  : ((b == PeWorld.B_ICE || b == PeWorld.B_SLIME_BLOCK) ? 0.85F : 1.0F);

               // +Y (Top)
               byte nTop = cb[idx + 1];
               if (shouldShowFace(b, nTop)) {
                  float l = Math.min(1.0F, 1.0F + boost);
                  ptr = emitQuad(out, ptr, PeWorld.getFaceTile(b, 0), l, l, l, alpha,
                     wx, yTop, wz + 1, wx + 1, yTop, wz + 1, wx + 1, yTop, wz, wx, yTop, wz);
               }
               // -Y (Bottom)
               byte nBot = cb[idx - 1];
               if (shouldShowFace(b, nBot)) {
                  float l = Math.min(1.0F, 0.52F + boost);
                  ptr = emitQuad(out, ptr, PeWorld.getFaceTile(b, 1), l, l, l, alpha,
                     wx, wy, wz, wx + 1, wy, wz, wx + 1, wy, wz + 1, wx, wy, wz + 1);
               }
               // -Z (North)
               byte nNorth = (lz > 0) ? cb[idx - 2048]
                  : (bNorth != null ? bNorth[(15 << 11) | (lx << 7) | wy] : PeWorld.B_AIR);
               if (shouldShowFace(b, nNorth)) {
                  float l = Math.min(1.0F, 0.84F + boost);
                  ptr = emitQuad(out, ptr, PeWorld.getFaceTile(b, 2), l, l, l, alpha,
                     wx + 1, wy, wz, wx, wy, wz, wx, yTop, wz, wx + 1, yTop, wz);
               }
               // +Z (South)
               byte nSouth = (lz < 15) ? cb[idx + 2048]
                  : (bSouth != null ? bSouth[(lx << 7) | wy] : PeWorld.B_AIR);
               if (shouldShowFace(b, nSouth)) {
                  float l = Math.min(1.0F, 0.84F + boost);
                  ptr = emitQuad(out, ptr, PeWorld.getFaceTile(b, 3), l, l, l, alpha,
                     wx, wy, wz + 1, wx + 1, wy, wz + 1, wx + 1, yTop, wz + 1, wx, yTop, wz + 1);
               }
               // -X (West)
               byte nWest = (lx > 0) ? cb[idx - 128]
                  : (bWest != null ? bWest[zOff | (15 << 7) | wy] : PeWorld.B_AIR);
               if (shouldShowFace(b, nWest)) {
                  float l = Math.min(1.0F, 0.70F + boost);
                  ptr = emitQuad(out, ptr, PeWorld.getFaceTile(b, 4), l, l, l, alpha,
                     wx, wy, wz, wx, wy, wz + 1, wx, yTop, wz + 1, wx, yTop, wz);
               }
               // +X (East)
               byte nEast = (lx < 15) ? cb[idx + 128]
                  : (bEast != null ? bEast[zOff | wy] : PeWorld.B_AIR);
               if (shouldShowFace(b, nEast)) {
                  float l = Math.min(1.0F, 0.70F + boost);
                  ptr = emitQuad(out, ptr, PeWorld.getFaceTile(b, 5), l, l, l, alpha,
                     wx + 1, wy, wz + 1, wx + 1, wy, wz, wx + 1, yTop, wz, wx + 1, yTop, wz + 1);
               }
            }
         }
      }
      return ptr / FLOATS_PER_VERT_3D;
   }

   private void rebuildSection(PeWorld world, int slot, int cx, int sy, int cz) {
      int verts = buildSectionGeometry(world, cx, sy, cz, this.scratch3D);
      this.vertCounts[slot][sy] = verts;
      if (verts > 0) {
         this.direct3D.clear();
         this.direct3D.put(this.scratch3D, 0, verts * FLOATS_PER_VERT_3D);
         this.direct3D.position(0);
         GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, this.vboIds[slot][sy]);
         GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, verts * STRIDE_3D, this.direct3D, GLES20.GL_STATIC_DRAW);
         GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
      }
   }

   private static boolean shouldShowFace(byte self, byte neighbor) {
      if (neighbor == PeWorld.B_AIR) return true;
      // Cull interior faces between identical fluids, glass, ice, AND leaf blocks for high FPS
      if (self == neighbor && (self == PeWorld.B_WATER || self == PeWorld.B_LAVA
         || self == PeWorld.B_GLASS || self == PeWorld.B_ICE
         || self == PeWorld.B_LEAVES_OAK || self == PeWorld.B_LEAVES_SPRUCE)) {
         return false;
      }
      return !PeWorld.isOpaque(neighbor);
   }

   private static int emitQuad(
      float[] out, int p, int tile, float r, float g, float b, float a,
      float x0, float y0, float z0,
      float x1, float y1, float z1,
      float x2, float y2, float z2,
      float x3, float y3, float z3
   ) {
      float u0 = ((tile & 15) * 16.0F + 0.15F) / 256.0F;
      float v0 = ((tile >> 4) * 16.0F + 0.15F) / 256.0F;
      float u1 = ((tile & 15) * 16.0F + 15.85F) / 256.0F;
      float v1 = ((tile >> 4) * 16.0F + 15.85F) / 256.0F;

      p = putV3D(out, p, x0, y0, z0, u0, v1, r, g, b, a);
      p = putV3D(out, p, x1, y1, z1, u1, v1, r, g, b, a);
      p = putV3D(out, p, x2, y2, z2, u1, v0, r, g, b, a);

      p = putV3D(out, p, x0, y0, z0, u0, v1, r, g, b, a);
      p = putV3D(out, p, x2, y2, z2, u1, v0, r, g, b, a);
      p = putV3D(out, p, x3, y3, z3, u0, v0, r, g, b, a);
      return p;
   }

   private static int emitCrossPlant(float[] out, int p, float x, float y, float z, int tile, float r, float g, float b) {
      p = emitQuad(out, p, tile, r, g, b, 1.0F, x + 0.15F, y, z + 0.15F, x + 0.85F, y, z + 0.85F, x + 0.85F, y + 0.9F, z + 0.85F, x + 0.15F, y + 0.9F, z + 0.15F);
      p = emitQuad(out, p, tile, r, g, b, 1.0F, x + 0.85F, y, z + 0.85F, x + 0.15F, y, z + 0.15F, x + 0.15F, y + 0.9F, z + 0.15F, x + 0.85F, y + 0.9F, z + 0.85F);
      p = emitQuad(out, p, tile, r, g, b, 1.0F, x + 0.15F, y, z + 0.85F, x + 0.85F, y, z + 0.15F, x + 0.85F, y + 0.9F, z + 0.15F, x + 0.15F, y + 0.9F, z + 0.85F);
      p = emitQuad(out, p, tile, r, g, b, 1.0F, x + 0.85F, y, z + 0.15F, x + 0.15F, y, z + 0.85F, x + 0.15F, y + 0.9F, z + 0.85F, x + 0.85F, y + 0.9F, z + 0.15F);
      return p;
   }

   private static int putV3D(float[] out, int p, float x, float y, float z, float u, float v, float r, float g, float b, float a) {
      out[p++] = x;
      out[p++] = y;
      out[p++] = z;
      out[p++] = u;
      out[p++] = v;
      out[p++] = r;
      out[p++] = g;
      out[p++] = b;
      out[p++] = a;
      return p;
   }

   public void drawWorldChunks(PeShaders sh, PeWorld world, float[] mvp, float camX, float camY, float camZ, float lookX, float lookZ) {
      GLES20.glUseProgram(sh.prog3D);
      GLES20.glUniformMatrix4fv(sh.uMVP, 1, false, mvp, 0);
      GLES20.glUniform4f(sh.uTint, 1.0F, 1.0F, 1.0F, 1.0F);
      GLES20.glEnableVertexAttribArray(sh.aPos3D);
      GLES20.glEnableVertexAttribArray(sh.aUV3D);
      GLES20.glEnableVertexAttribArray(sh.aColor3D);

      int pCx = ((int)Math.floor(camX)) >> 4;
      int pCz = ((int)Math.floor(camZ)) >> 4;
      int rad = Math.max(3, Math.min(7, world.viewRadiusChunks));
      float maxDistSq = ((rad + 0.75F) * 16.0F) * ((rad + 0.75F) * 16.0F);

      for (int dz = -rad; dz <= rad; dz++) {
         int cz = pCz + dz;
         float centerZ = (cz << 4) + 8.0F;
         for (int dx = -rad; dx <= rad; dx++) {
            int cx = pCx + dx;
            int slot = PeWorld.slotIndex(cx, cz);
            PeWorld.PeChunk chunk = world.chunkGrid[slot];
            if (!chunk.loaded || chunk.cx != cx || chunk.cz != cz) continue;

            float centerX = (cx << 4) + 8.0F;
            float rx = centerX - camX;
            float rz = centerZ - camZ;
            float distSq = rx * rx + rz * rz;
            if (distSq > maxDistSq) continue;
            // View-cone culling: always draw nearby chunks (r < 24); cull chunks behind the camera
            if (distSq > 576.0F && (rx * lookX + rz * lookZ) < -18.0F) {
               continue;
            }

            int maxSec = Math.min(PeWorld.SECTIONS_Y - 1, (chunk.maxY >> 4) + 1);
            for (int sy = 0; sy <= maxSec; sy++) {
               int count = this.vertCounts[slot][sy];
               if (count <= 0) continue;
               GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, this.vboIds[slot][sy]);
               GLES20.glVertexAttribPointer(sh.aPos3D, 3, GLES20.GL_FLOAT, false, STRIDE_3D, 0);
               GLES20.glVertexAttribPointer(sh.aUV3D, 2, GLES20.GL_FLOAT, false, STRIDE_3D, 3 * 4);
               GLES20.glVertexAttribPointer(sh.aColor3D, 4, GLES20.GL_FLOAT, false, STRIDE_3D, 5 * 4);
               GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, count);
            }
         }
      }
      GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
   }

   // ====================================================================
   //  Dynamic 3D (Target Block Selection Box, Crack Overlay & 1st-Person Hand)
   // ====================================================================
   public void beginDynamic3D() {
      this.dyn3DVerts = 0;
   }

   public void addBox3D(float x0, float y0, float z0, float x1, float y1, float z1, int tile, float r, float g, float b, float a) {
      int p = this.dyn3DVerts * FLOATS_PER_VERT_3D;
      if (p + 36 * FLOATS_PER_VERT_3D >= this.dyn3DData.length) return;
      p = emitQuad(this.dyn3DData, p, tile, r, g, b, a, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0);
      p = emitQuad(this.dyn3DData, p, tile, r * 0.55F, g * 0.55F, b * 0.55F, a, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
      p = emitQuad(this.dyn3DData, p, tile, r * 0.85F, g * 0.85F, b * 0.85F, a, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0);
      p = emitQuad(this.dyn3DData, p, tile, r * 0.85F, g * 0.85F, b * 0.85F, a, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
      p = emitQuad(this.dyn3DData, p, tile, r * 0.72F, g * 0.72F, b * 0.72F, a, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
      p = emitQuad(this.dyn3DData, p, tile, r * 0.72F, g * 0.72F, b * 0.72F, a, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1);
      this.dyn3DVerts = p / FLOATS_PER_VERT_3D;
   }

   public void flushDynamic3D(PeShaders sh) {
      if (this.dyn3DVerts <= 0) return;
      this.dyn3DBuf.clear();
      this.dyn3DBuf.put(this.dyn3DData, 0, this.dyn3DVerts * FLOATS_PER_VERT_3D);
      this.dyn3DBuf.position(0);

      GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, this.dyn3DVbo);
      GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, this.dyn3DVerts * STRIDE_3D, this.dyn3DBuf, GLES20.GL_DYNAMIC_DRAW);
      GLES20.glVertexAttribPointer(sh.aPos3D, 3, GLES20.GL_FLOAT, false, STRIDE_3D, 0);
      GLES20.glVertexAttribPointer(sh.aUV3D, 2, GLES20.GL_FLOAT, false, STRIDE_3D, 3 * 4);
      GLES20.glVertexAttribPointer(sh.aColor3D, 4, GLES20.GL_FLOAT, false, STRIDE_3D, 5 * 4);
      GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, this.dyn3DVerts);
      GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
   }

   // ====================================================================
   //  Batched 2D Orthographic Mobile Touch UI / HUD Renderer
   // ====================================================================
   public void begin2D() {
      this.dyn2DVerts = 0;
   }

   public void addRect2D(float x, float y, float w, float h, int argb) {
      addTexturedQuad2D(x, y, w, h, 0.0F, 0.0F, 1.0F, 1.0F, argb);
   }

   /** Draws a sleek dark-glass mobile UI card with drop shadow, double border, and top highlight rim. */
   public void drawCard2D(float x, float y, float w, float h, int bgArgb, int borderArgb) {
      // Soft drop shadow
      addRect2D(x + 6.0F, y + 8.0F, w, h, 0x6404060A);
      // Outer border frame
      addRect2D(x, y, w, h, borderArgb);
      // Main card fill
      addRect2D(x + 3.0F, y + 3.0F, w - 6.0F, h - 6.0F, bgArgb);
      // Top inner specular highlight
      addRect2D(x + 4.0F, y + 4.0F, w - 8.0F, 3.0F, 0x40FFFFFF);
      // Bottom inner shadow
      addRect2D(x + 4.0F, y + h - 7.0F, w - 8.0F, 3.0F, 0x60000000);
   }

   /** Draws a tactile mobile button with chamfered feel, accent bar, top highlight, and bottom lip. */
   public void drawButton2D(float x, float y, float w, float h, int bgArgb, int accentArgb, boolean active) {
      addRect2D(x + 3.0F, y + 4.0F, w, h, 0x58000000);
      addRect2D(x, y, w, h, active ? accentArgb : 0xD812161E);
      addRect2D(x + 2.5F, y + 2.5F, w - 5.0F, h - 5.0F, bgArgb);
      // Left accent pill bar
      addRect2D(x + 2.5F, y + 2.5F, 6.0F, h - 5.0F, accentArgb);
      // Top bevel highlight & bottom shadow lip
      addRect2D(x + 2.5F, y + 2.5F, w - 5.0F, 3.5F, 0x55FFFFFF);
      addRect2D(x + 2.5F, y + h - 6.5F, w - 5.0F, 4.0F, 0x70000000);
   }

   /** Draws a recessed inventory/hotbar item slot with glowing selection frame when active. */
   public void drawSlot2D(float x, float y, float s, boolean selected, int specialBg) {
      int frameCol = selected ? 0xFFFFE058 : 0xC8141820;
      addRect2D(x, y, s, s, frameCol);
      float pad = selected ? 4.0F : 2.5F;
      int fillCol = specialBg != 0 ? specialBg : (selected ? 0xE832543E : 0xD0202630);
      addRect2D(x + pad, y + pad, s - pad * 2.0F, s - pad * 2.0F, fillCol);
      // Inner recessed shadow at top-left, highlight at bottom-right
      addRect2D(x + pad, y + pad, s - pad * 2.0F, 2.5F, selected ? 0x60FFFFFF : 0x68080A0E);
      addRect2D(x + pad, y + s - pad - 2.5F, s - pad * 2.0F, 2.5F, 0x38FFFFFF);
   }

   /** Draws a smooth rounded/octagonal circular touch pad or action button for mobile thumbs. */
   public void drawCirclePad2D(float cx, float cy, float r, int ringArgb, int fillArgb) {
      // Approximate circle using overlapping horizontal/vertical rounded strips
      float r85 = r * 0.86F;
      float r68 = r * 0.68F;
      // Outer ring
      addRect2D(cx - r68, cy - r, r68 * 2.0F, r * 2.0F, ringArgb);
      addRect2D(cx - r, cy - r68, r * 2.0F, r68 * 2.0F, ringArgb);
      addRect2D(cx - r85, cy - r85, r85 * 2.0F, r85 * 2.0F, ringArgb);
      // Inner fill
      float ir = r - 4.0F;
      float ir85 = ir * 0.86F;
      float ir68 = ir * 0.68F;
      addRect2D(cx - ir68, cy - ir, ir68 * 2.0F, ir * 2.0F, fillArgb);
      addRect2D(cx - ir, cy - ir68, ir * 2.0F, ir68 * 2.0F, fillArgb);
      addRect2D(cx - ir85, cy - ir85, ir85 * 2.0F, ir85 * 2.0F, fillArgb);
      // Top specular arc
      addRect2D(cx - ir68 * 0.8F, cy - ir + 2.0F, ir68 * 1.6F, 3.5F, 0x48FFFFFF);
   }

   public void addTile2D(float x, float y, float w, float h, int tile, int argb) {
      float u0 = ((tile & 15) * 16.0F + 0.15F) / 256.0F;
      float v0 = ((tile >> 4) * 16.0F + 0.15F) / 256.0F;
      float u1 = ((tile & 15) * 16.0F + 15.85F) / 256.0F;
      float v1 = ((tile >> 4) * 16.0F + 15.85F) / 256.0F;
      addTexturedQuad2D(x, y, w, h, u0, v0, u1, v1, argb);
   }

   public void addText2D(PeTextures tex, String text, float x, float y, float scale, int argb) {
      if (text == null || text.isEmpty()) return;
      float curX = x;
      float cw = PeTextures.FONT_CELL_W * scale;
      float ch = PeTextures.FONT_CELL_H * scale;
      for (int i = 0; i < text.length(); i++) {
         char c = text.charAt(i);
         if (c < 32 || c >= 128) continue;
         int idx = c - 32;
         int col = idx & 15;
         int row = idx >> 4;
         float u0 = (col * 8.0F) / 128.0F;
         float v0 = (row * 11.0F) / 128.0F;
         float u1 = (col * 8.0F + 8.0F) / 128.0F;
         float v1 = (row * 11.0F + 11.0F) / 128.0F;
         addTexturedQuad2D(curX + scale, y + scale, cw, ch, u0, v0, u1, v1, 0xDF080A10);
         addTexturedQuad2D(curX, y, cw, ch, u0, v0, u1, v1, argb);
         curX += tex.fontWidths[c] * scale;
      }
   }

   public void addTextCentered2D(PeTextures tex, String text, float centerX, float y, float scale, int argb) {
      if (text == null || text.isEmpty()) return;
      float totalW = tex.measureText(text) * scale;
      addText2D(tex, text, centerX - totalW * 0.5F, y, scale, argb);
   }

   private void addTexturedQuad2D(float x, float y, float w, float h, float u0, float v0, float u1, float v1, int argb) {
      int p = this.dyn2DVerts * FLOATS_PER_VERT_2D;
      if (p + 6 * FLOATS_PER_VERT_2D >= this.dyn2DData.length) return;
      float a = ((argb >>> 24) & 0xFF) / 255.0F;
      float r = ((argb >>> 16) & 0xFF) / 255.0F;
      float g = ((argb >>> 8) & 0xFF) / 255.0F;
      float b = (argb & 0xFF) / 255.0F;
      float x1 = x + w;
      float y1 = y + h;

      p = putV2D(this.dyn2DData, p, x, y, u0, v0, r, g, b, a);
      p = putV2D(this.dyn2DData, p, x, y1, u0, v1, r, g, b, a);
      p = putV2D(this.dyn2DData, p, x1, y1, u1, v1, r, g, b, a);

      p = putV2D(this.dyn2DData, p, x, y, u0, v0, r, g, b, a);
      p = putV2D(this.dyn2DData, p, x1, y1, u1, v1, r, g, b, a);
      p = putV2D(this.dyn2DData, p, x1, y, u1, v0, r, g, b, a);
      this.dyn2DVerts = p / FLOATS_PER_VERT_2D;
   }

   private static int putV2D(float[] out, int p, float x, float y, float u, float v, float r, float g, float b, float a) {
      out[p++] = x;
      out[p++] = y;
      out[p++] = u;
      out[p++] = v;
      out[p++] = r;
      out[p++] = g;
      out[p++] = b;
      out[p++] = a;
      return p;
   }

   public void flush2D(PeShaders sh, int texId, float[] ortho) {
      if (this.dyn2DVerts <= 0) return;
      this.dyn2DBuf.clear();
      this.dyn2DBuf.put(this.dyn2DData, 0, this.dyn2DVerts * FLOATS_PER_VERT_2D);
      this.dyn2DBuf.position(0);

      GLES20.glUseProgram(sh.prog2D);
      GLES20.glUniformMatrix4fv(sh.uOrtho, 1, false, ortho, 0);
      GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId);
      GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, this.dyn2DVbo);
      GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, this.dyn2DVerts * STRIDE_2D, this.dyn2DBuf, GLES20.GL_DYNAMIC_DRAW);
      GLES20.glEnableVertexAttribArray(sh.aPos2D);
      GLES20.glEnableVertexAttribArray(sh.aUV2D);
      GLES20.glEnableVertexAttribArray(sh.aColor2D);
      GLES20.glVertexAttribPointer(sh.aPos2D, 2, GLES20.GL_FLOAT, false, STRIDE_2D, 0);
      GLES20.glVertexAttribPointer(sh.aUV2D, 2, GLES20.GL_FLOAT, false, STRIDE_2D, 2 * 4);
      GLES20.glVertexAttribPointer(sh.aColor2D, 4, GLES20.GL_FLOAT, false, STRIDE_2D, 4 * 4);
      GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, this.dyn2DVerts);
      GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
      this.dyn2DVerts = 0;
   }
}
