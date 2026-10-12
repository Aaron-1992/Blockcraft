package com.blockcraft.pe;

import android.opengl.GLES20;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Blockcraft PE Hardware VBO Chunk Mesher, 3D Entity/Weapon Renderer, and Batched 2D Touch HUD Renderer
 * optimized for Samsung Galaxy A55 5G (Exynos 1480 / Xclipse 530 OpenGL ES 3.0 at 120Hz).
 */
public final class PeChunkMesher {
   private static final int FLOATS_PER_VERT_3D = 9; // x, y, z, u, v, r, g, b, a
   private static final int STRIDE_3D = FLOATS_PER_VERT_3D * 4;

   private static final int FLOATS_PER_VERT_2D = 8; // x, y, u, v, r, g, b, a
   private static final int STRIDE_2D = FLOATS_PER_VERT_2D * 4;

   private final int[][] vboIds = new int[PeWorld.CHUNK_GRID * PeWorld.CHUNK_GRID][PeWorld.SECTIONS_Y];
   private final int[][] vertCounts = new int[PeWorld.CHUNK_GRID * PeWorld.CHUNK_GRID][PeWorld.SECTIONS_Y];

   // Scratch buffer for building a 16x16x16 section VBO (max ~12,000 vertices)
   private final float[] scratch3D = new float[72000 * FLOATS_PER_VERT_3D];
   private final FloatBuffer direct3D;

   // Dynamic 3D buffer for Mobs, Projectiles, Selection Box & 1st-Person Hand/Weapon
   private int dyn3DVbo = 0;
   private final float[] dyn3DData = new float[12000 * FLOATS_PER_VERT_3D];
   private final FloatBuffer dyn3DBuf;
   private int dyn3DVerts = 0;

   // Dynamic 2D buffer for Touch Controls, Hotbar, Hearts, Crosshair & Menus
   private int dyn2DVbo = 0;
   private final float[] dyn2DData = new float[12000 * FLOATS_PER_VERT_2D];
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

      for (int c = 0; c < PeWorld.CHUNK_GRID * PeWorld.CHUNK_GRID; c++) {
         GLES20.glGenBuffers(PeWorld.SECTIONS_Y, this.vboIds[c], 0);
         for (int sy = 0; sy < PeWorld.SECTIONS_Y; sy++) {
            this.vertCounts[c][sy] = 0;
            world.sectionDirty[c][sy] = true;
         }
      }
   }

   public void updateDirtySections(PeWorld world, int maxPerFrame) {
      int rebuilt = 0;
      int pCx = ((int)Math.floor(world.px) + PeWorld.HALF_W) >> 4;
      int pCz = ((int)Math.floor(world.pz) + PeWorld.HALF_W) >> 4;

      // Prioritize chunks closest to the player first!
      for (int pass = 0; pass < 2 && rebuilt < maxPerFrame; pass++) {
         for (int cz = 0; cz < PeWorld.CHUNK_GRID && rebuilt < maxPerFrame; cz++) {
            for (int cx = 0; cx < PeWorld.CHUNK_GRID && rebuilt < maxPerFrame; cx++) {
               int dist = Math.abs(cx - pCx) + Math.abs(cz - pCz);
               if ((pass == 0 && dist > 2) || (pass == 1 && dist <= 2)) continue;
               int cIdx = cz * PeWorld.CHUNK_GRID + cx;
               for (int sy = 0; sy < PeWorld.SECTIONS_Y; sy++) {
                  if (world.sectionDirty[cIdx][sy]) {
                     world.sectionDirty[cIdx][sy] = false;
                     rebuildSection(world, cx, sy, cz);
                     rebuilt++;
                     if (rebuilt >= maxPerFrame) break;
                  }
               }
            }
         }
      }
   }

   public int buildSectionGeometry(PeWorld world, int cx, int sy, int cz, float[] out) {
      int wx0 = cx * 16 - PeWorld.HALF_W;
      int wy0 = sy * 16;
      int wz0 = cz * 16 - PeWorld.HALF_W;
      int ptr = 0;
      int maxFloats = out.length - 54;

      for (int ly = 0; ly < 16; ly++) {
         int wy = wy0 + ly;
         if (wy <= 0 || wy >= PeWorld.WORLD_H - 1) continue;
         for (int lz = 0; lz < 16; lz++) {
            int wz = wz0 + lz;
            for (int lx = 0; lx < 16; lx++) {
               int wx = wx0 + lx;
               byte b = world.getBlock(wx, wy, wz);
               if (b == PeWorld.B_AIR) continue;
               if (ptr >= maxFloats) break;

               if (b == PeWorld.B_TORCH) {
                  ptr = emitCrossPlant(out, ptr, wx, wy, wz, PeTextures.T_TORCH, 1.0F, 0.98F, 0.88F);
                  continue;
               }

               float boost = PeWorld.isLightEmitter(b) ? 0.28F : 0.0F;
               float yTop = (b == PeWorld.B_WATER || b == PeWorld.B_LAVA) ? (wy + 0.88F) : (b == PeWorld.B_BED_RED ? (wy + 0.56F) : (wy + 1.0F));
               float alpha = (b == PeWorld.B_WATER) ? 0.78F : ((b == PeWorld.B_ICE || b == PeWorld.B_SLIME_BLOCK) ? 0.85F : 1.0F);

               // +Y (Top)
               byte nTop = world.getBlock(wx, wy + 1, wz);
               if (shouldShowFace(b, nTop)) {
                  float l = Math.min(1.0F, 1.0F + boost);
                  ptr = emitQuad(out, ptr, PeWorld.getFaceTile(b, 0), l, l, l, alpha,
                     wx, yTop, wz + 1, wx + 1, yTop, wz + 1, wx + 1, yTop, wz, wx, yTop, wz);
               }
               // -Y (Bottom)
               byte nBot = world.getBlock(wx, wy - 1, wz);
               if (shouldShowFace(b, nBot)) {
                  float l = Math.min(1.0F, 0.52F + boost);
                  ptr = emitQuad(out, ptr, PeWorld.getFaceTile(b, 1), l, l, l, alpha,
                     wx, wy, wz, wx + 1, wy, wz, wx + 1, wy, wz + 1, wx, wy, wz + 1);
               }
               // -Z (North)
               byte nNorth = world.getBlock(wx, wy, wz - 1);
               if (shouldShowFace(b, nNorth)) {
                  float l = Math.min(1.0F, 0.82F + boost);
                  ptr = emitQuad(out, ptr, PeWorld.getFaceTile(b, 2), l, l, l, alpha,
                     wx + 1, wy, wz, wx, wy, wz, wx, yTop, wz, wx + 1, yTop, wz);
               }
               // +Z (South)
               byte nSouth = world.getBlock(wx, wy, wz + 1);
               if (shouldShowFace(b, nSouth)) {
                  float l = Math.min(1.0F, 0.82F + boost);
                  ptr = emitQuad(out, ptr, PeWorld.getFaceTile(b, 3), l, l, l, alpha,
                     wx, wy, wz + 1, wx + 1, wy, wz + 1, wx + 1, yTop, wz + 1, wx, yTop, wz + 1);
               }
               // -X (West)
               byte nWest = world.getBlock(wx - 1, wy, wz);
               if (shouldShowFace(b, nWest)) {
                  float l = Math.min(1.0F, 0.68F + boost);
                  ptr = emitQuad(out, ptr, PeWorld.getFaceTile(b, 4), l, l, l, alpha,
                     wx, wy, wz, wx, wy, wz + 1, wx, yTop, wz + 1, wx, yTop, wz);
               }
               // +X (East)
               byte nEast = world.getBlock(wx + 1, wy, wz);
               if (shouldShowFace(b, nEast)) {
                  float l = Math.min(1.0F, 0.68F + boost);
                  ptr = emitQuad(out, ptr, PeWorld.getFaceTile(b, 5), l, l, l, alpha,
                     wx + 1, wy, wz + 1, wx + 1, wy, wz, wx + 1, yTop, wz, wx + 1, yTop, wz + 1);
               }
            }
         }
      }
      return ptr / FLOATS_PER_VERT_3D;
   }

   private void rebuildSection(PeWorld world, int cx, int sy, int cz) {
      int verts = buildSectionGeometry(world, cx, sy, cz, this.scratch3D);
      int cIdx = cz * PeWorld.CHUNK_GRID + cx;
      this.vertCounts[cIdx][sy] = verts;
      if (verts > 0) {
         this.direct3D.clear();
         this.direct3D.put(this.scratch3D, 0, verts * FLOATS_PER_VERT_3D);
         this.direct3D.position(0);
         GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, this.vboIds[cIdx][sy]);
         GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, verts * STRIDE_3D, this.direct3D, GLES20.GL_STATIC_DRAW);
         GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
      }
   }

   private static boolean shouldShowFace(byte self, byte neighbor) {
      if (neighbor == PeWorld.B_AIR) return true;
      if (self == neighbor && (self == PeWorld.B_WATER || self == PeWorld.B_LAVA || self == PeWorld.B_GLASS || self == PeWorld.B_ICE)) {
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

      // Tri 1: 0, 1, 2
      p = putV3D(out, p, x0, y0, z0, u0, v1, r, g, b, a);
      p = putV3D(out, p, x1, y1, z1, u1, v1, r, g, b, a);
      p = putV3D(out, p, x2, y2, z2, u1, v0, r, g, b, a);
      // Tri 2: 0, 2, 3
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

      for (int cz = 0; cz < PeWorld.CHUNK_GRID; cz++) {
         float centerZ = (cz * 16 - PeWorld.HALF_W) + 8.0F;
         for (int cx = 0; cx < PeWorld.CHUNK_GRID; cx++) {
            float centerX = (cx * 16 - PeWorld.HALF_W) + 8.0F;
            float dx = centerX - camX;
            float dz = centerZ - camZ;
            float distSq = dx * dx + dz * dz;
            // View-cone culling: always draw nearby chunks (r < 24); cull chunks behind the camera
            if (distSq > 576.0F && (dx * lookX + dz * lookZ) < -18.0F) {
               continue;
            }
            int cIdx = cz * PeWorld.CHUNK_GRID + cx;
            for (int sy = 0; sy < PeWorld.SECTIONS_Y; sy++) {
               int count = this.vertCounts[cIdx][sy];
               if (count <= 0) continue;
               GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, this.vboIds[cIdx][sy]);
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
   //  Dynamic 3D Entities (Mobs, AI Bots, Projectiles, Crack Overlay & 1st-Person Hand/Weapon)
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

   public void addMobModel(PeWorld.PeMob m) {
      float mx = (float)m.x;
      float my = (float)m.y;
      float mz = (float)m.z;
      float hurtR = m.hurtTime > 0 ? 1.0F : 0.88F;
      float hurtGB = m.hurtTime > 0 ? 0.35F : 0.88F;

      // Team/Type Tunic Color
      float tr = 0.25F, tg = 0.65F, tb = 0.92F;
      if (m.type == 1) { tr = 0.28F; tg = 0.68F; tb = 0.32F; } // Zombie green
      else if (m.type == 2) { tr = 0.82F; tg = 0.82F; tb = 0.82F; } // Skeleton bone
      else if (m.type == 3) { tr = 0.22F; tg = 0.80F; tb = 0.24F; } // Creeper
      else if (m.team == 0) { tr = 0.25F; tg = 0.55F; tb = 0.98F; } // Blue/CT/Def
      else if (m.team == 1) { tr = 0.95F; tg = 0.26F; tb = 0.24F; } // Red/T/Atk
      else if (m.team == 2) { tr = 0.25F; tg = 0.88F; tb = 0.32F; } // Green
      else if (m.team == 3) { tr = 0.95F; tg = 0.85F; tb = 0.20F; } // Yellow

      float legSwing = (float)Math.sin(m.walkPhase) * 0.16F;
      // Legs
      addBox3D(mx - 0.22F, my, mz - 0.12F + legSwing, mx - 0.02F, my + 0.72F, mz + 0.12F + legSwing,
         PeTextures.T_WOOL_BLUE, 0.35F * hurtR, 0.38F * hurtGB, 0.52F * hurtGB, 1.0F);
      addBox3D(mx + 0.02F, my, mz - 0.12F - legSwing, mx + 0.22F, my + 0.72F, mz + 0.12F - legSwing,
         PeTextures.T_WOOL_BLUE, 0.35F * hurtR, 0.38F * hurtGB, 0.52F * hurtGB, 1.0F);
      // Torso
      addBox3D(mx - 0.26F, my + 0.72F, mz - 0.15F, mx + 0.26F, my + 1.38F, mz + 0.15F,
         PeTextures.T_WOOL_WHITE, tr * hurtR, tg * hurtGB, tb * hurtGB, 1.0F);
      // Arms
      addBox3D(mx - 0.42F, my + 0.76F, mz - 0.12F - legSwing, mx - 0.26F, my + 1.36F, mz + 0.12F - legSwing,
         PeTextures.T_WOOL_WHITE, tr * hurtR, tg * hurtGB, tb * hurtGB, 1.0F);
      addBox3D(mx + 0.26F, my + 0.76F, mz - 0.12F + legSwing, mx + 0.42F, my + 1.36F, mz + 0.12F + legSwing,
         PeTextures.T_WOOL_WHITE, tr * hurtR, tg * hurtGB, tb * hurtGB, 1.0F);
      // Head
      addBox3D(mx - 0.24F, my + 1.38F, mz - 0.24F, mx + 0.24F, my + 1.84F, mz + 0.24F,
         PeTextures.T_SAND, 0.92F * hurtR, 0.76F * hurtGB, 0.62F * hurtGB, 1.0F);
      // Floating HP Bar above head
      float hpFrac = Math.max(0.0F, Math.min(1.0F, m.health / (float)m.maxHealth));
      addBox3D(mx - 0.35F, my + 2.02F, mz - 0.04F, mx - 0.35F + 0.70F * hpFrac, my + 2.12F, mz + 0.04F,
         PeTextures.T_WOOL_WHITE, 1.0F - hpFrac * 0.7F, 0.25F + hpFrac * 0.7F, 0.2F, 1.0F);
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
   //  Batched 2D Orthographic Touch UI / HUD Renderer
   // ====================================================================
   public void begin2D() {
      this.dyn2DVerts = 0;
   }

   public void addRect2D(float x, float y, float w, float h, int argb) {
      // Uses tile (15, 15) center UV or whiteTex when bound
      addTexturedQuad2D(x, y, w, h, 0.0F, 0.0F, 1.0F, 1.0F, argb);
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
      for (int i = 0; i < text.length(); i++) {
         char c = text.charAt(i);
         if (c < 32 || c >= 128) continue;
         float u0 = ((c & 15) * 8.0F) / 128.0F;
         float v0 = ((c >> 4) * 8.0F) / 128.0F;
         float u1 = u0 + 8.0F / 128.0F;
         float v1 = v0 + 8.0F / 128.0F;
         addTexturedQuad2D(curX + scale, y + scale, 8.0F * scale, 8.0F * scale, u0, v0, u1, v1, 0xB0101010);
         addTexturedQuad2D(curX, y, 8.0F * scale, 8.0F * scale, u0, v0, u1, v1, argb);
         curX += (tex.fontWidths[c] + 1) * scale;
      }
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
