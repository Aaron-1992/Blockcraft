package com.blockcraft.pe;

import android.opengl.GLES20;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;

/**
 * Blockcraft PE v1.2 — PC Update 3.5+ OpenGL ES VBO Chunk Section Mesher & 3D Sky/Cloud Renderer:
 * - Smooth Per-Vertex Ambient Occlusion (AO) on corners & walls + Update 3.19 natural block tone variation
 * - 3D Bushy Leaf Fronds (T_BUSHY_FROND_*) protruding from exposed tree canopy leaf blocks
 * - Custom 3D Block Shapes: Half-Height Slabs, Connecting Fences, 2-Block Doors, Wall Ladders,
 *   Inset Chests/Cacti, Beds, Recessed Farmland, 3D Torch Posts & Crossed Crops/Plants
 * - 3D Sky Renderer: Orbiting Sun & Moon, Night Starfield, Twilight Horizon Glow & Drifting 3D Clouds
 */
public final class PeChunkMesher {
   public static final int FLOATS_PER_VERT_3D = 9;
   public static final int STRIDE_3D = FLOATS_PER_VERT_3D * 4;

   public static final int FLOATS_PER_VERT_2D = 8;
   public static final int STRIDE_2D = FLOATS_PER_VERT_2D * 4;

   private final int[] sectionVbo = new int[PeWorld.ACTIVE_SLOTS * PeWorld.SECTIONS_Y];
   private final int[] sectionVerts = new int[PeWorld.ACTIVE_SLOTS * PeWorld.SECTIONS_Y];
   private final int[] sectionCx = new int[PeWorld.ACTIVE_SLOTS];
   private final int[] sectionCz = new int[PeWorld.ACTIVE_SLOTS];

   private final float[] scratch3D = new float[28672 * FLOATS_PER_VERT_3D];
   private final FloatBuffer uploadBuf3D;

   private int dyn3DVbo = 0;
   private final float[] dyn3DData = new float[12288 * FLOATS_PER_VERT_3D];
   private final FloatBuffer dyn3DBuf;
   private int dyn3DVerts = 0;

   private int dyn2DVbo = 0;
   private final float[] dyn2DData = new float[12288 * FLOATS_PER_VERT_2D];
   private final FloatBuffer dyn2DBuf;
   private int dyn2DVerts = 0;

   public PeChunkMesher() {
      this.uploadBuf3D = ByteBuffer.allocateDirect(this.scratch3D.length * 4)
         .order(ByteOrder.nativeOrder()).asFloatBuffer();
      this.dyn3DBuf = ByteBuffer.allocateDirect(this.dyn3DData.length * 4)
         .order(ByteOrder.nativeOrder()).asFloatBuffer();
      this.dyn2DBuf = ByteBuffer.allocateDirect(this.dyn2DData.length * 4)
         .order(ByteOrder.nativeOrder()).asFloatBuffer();
      Arrays.fill(this.sectionCx, Integer.MIN_VALUE);
      Arrays.fill(this.sectionCz, Integer.MIN_VALUE);
   }

   public void initGL(PeWorld world) {
      GLES20.glGenBuffers(this.sectionVbo.length, this.sectionVbo, 0);
      int[] tmp = new int[2];
      GLES20.glGenBuffers(2, tmp, 0);
      this.dyn3DVbo = tmp[0];
      this.dyn2DVbo = tmp[1];
      Arrays.fill(this.sectionVerts, 0);
      Arrays.fill(this.sectionCx, Integer.MIN_VALUE);
      Arrays.fill(this.sectionCz, Integer.MIN_VALUE);
      if (world != null) {
         world.markAllDirty();
      }
   }

   public void updateDirtySections(PeWorld world, int maxRebuildsPerFrame) {
      int rebuilt = 0;
      int pCx = ((int)Math.floor(world.px)) >> 4;
      int pCz = ((int)Math.floor(world.pz)) >> 4;
      int rad = Math.max(3, Math.min(7, world.viewRadiusChunks));

      for (int ring = 0; ring <= rad && rebuilt < maxRebuildsPerFrame; ring++) {
         for (int dz = -ring; dz <= ring && rebuilt < maxRebuildsPerFrame; dz++) {
            for (int dx = -ring; dx <= ring && rebuilt < maxRebuildsPerFrame; dx++) {
               if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) continue;
               int cx = pCx + dx;
               int cz = pCz + dz;
               int slot = PeWorld.slotIndex(cx, cz);
               PeWorld.PeChunk chunk = world.chunkGrid[slot];
               if (!chunk.loaded || chunk.cx != cx || chunk.cz != cz) continue;

               if (this.sectionCx[slot] != cx || this.sectionCz[slot] != cz) {
                  this.sectionCx[slot] = cx;
                  this.sectionCz[slot] = cz;
                  for (int sy = 0; sy < PeWorld.SECTIONS_Y; sy++) {
                     chunk.sectionDirty[sy] = true;
                     this.sectionVerts[slot * PeWorld.SECTIONS_Y + sy] = 0;
                  }
               }

               for (int sy = 0; sy < PeWorld.SECTIONS_Y; sy++) {
                  if (!chunk.sectionDirty[sy]) continue;
                  chunk.sectionDirty[sy] = false;
                  int idx = slot * PeWorld.SECTIONS_Y + sy;

                  if (chunk.sectionNonAir[sy] <= 0) {
                     this.sectionVerts[idx] = 0;
                     continue;
                  }

                  int verts = meshSection(world, chunk, cx, sy, cz, this.scratch3D);
                  this.sectionVerts[idx] = verts;
                  if (verts > 0) {
                     this.uploadBuf3D.clear();
                     this.uploadBuf3D.put(this.scratch3D, 0, verts * FLOATS_PER_VERT_3D);
                     this.uploadBuf3D.position(0);
                     GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, this.sectionVbo[idx]);
                     GLES20.glBufferData(
                        GLES20.GL_ARRAY_BUFFER,
                        verts * STRIDE_3D,
                        this.uploadBuf3D,
                        GLES20.GL_STATIC_DRAW
                     );
                     GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
                  }
                  rebuilt++;
                  if (rebuilt >= maxRebuildsPerFrame) {
                     return;
                  }
               }
            }
         }
      }
   }

   public void drawWorldChunks(PeShaders sh, PeWorld world, float[] mvp,
                               float camX, float camY, float camZ, float fwdX, float fwdZ) {
      GLES20.glUniformMatrix4fv(sh.uMVP, 1, false, mvp, 0);
      GLES20.glEnableVertexAttribArray(sh.aPos3D);
      GLES20.glEnableVertexAttribArray(sh.aUV3D);
      GLES20.glEnableVertexAttribArray(sh.aColor3D);

      int pCx = ((int)Math.floor(camX)) >> 4;
      int pCz = ((int)Math.floor(camZ)) >> 4;
      int rad = Math.max(3, Math.min(7, world.viewRadiusChunks));
      float fogDist = (rad + 0.5F) * 16.0F;
      float maxDistSq = (fogDist + 20.0F) * (fogDist + 20.0F);

      for (int dz = -rad; dz <= rad; dz++) {
         int cz = pCz + dz;
         float centerZ = (cz << 4) + 8.0F;
         float dZ = centerZ - camZ;
         for (int dx = -rad; dx <= rad; dx++) {
            int cx = pCx + dx;
            float centerX = (cx << 4) + 8.0F;
            float dX = centerX - camX;
            float distSq = dX * dX + dZ * dZ;
            if (distSq > maxDistSq) continue;

            if (distSq > 1024.0F) {
               float dot = (dX * fwdX + dZ * fwdZ) / (float)Math.sqrt(distSq);
               if (dot < -0.68F) continue;
            }

            int slot = PeWorld.slotIndex(cx, cz);
            if (this.sectionCx[slot] != cx || this.sectionCz[slot] != cz) continue;

            int baseIdx = slot * PeWorld.SECTIONS_Y;
            for (int sy = 0; sy < PeWorld.SECTIONS_Y; sy++) {
               int idx = baseIdx + sy;
               int count = this.sectionVerts[idx];
               if (count <= 0) continue;

               GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, this.sectionVbo[idx]);
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
   //  PC Update 3.5+ Section Mesher (Smooth AO, Bushy Leaves & Shapes)
   // ====================================================================
   private static boolean shouldRenderFace(byte self, byte neighbor, int face) {
      if (neighbor == PeWorld.B_AIR) return true;
      // Cutout foliage blocks (Oak, Spruce, Birch Leaves) have sky cutouts so all 6 faces
      // not against a solid opaque block must be rendered to prevent one-way invisible faces!
      if (PeWorld.isLeaves(self)) {
         return !PeWorld.isOpaque(neighbor);
      }
      if (self == neighbor) {
         if (PeWorld.isSlab(self) && (face == 0 || face == 1)) {
            return true;
         }
         if (PeWorld.isStair(self) || self == PeWorld.B_FENCE_OAK || self == PeWorld.B_CHEST || self == PeWorld.B_CACTUS) {
            return true;
         }
         return false;
      }
      if ((PeWorld.isSlab(self) || PeWorld.isStair(self) || self == PeWorld.B_FARMLAND || self == PeWorld.B_BED_RED) && face == 0) {
         return true;
      }
      return !PeWorld.isOpaque(neighbor);
   }

   private static float blockVariation(int wx, int wy, int wz) {
      int h = (wx * 734287) ^ (wy * 912931) ^ (wz * 439829);
      h = (h ^ (h >>> 13)) * 1274126177;
      return 1.0F + (((h & 255) / 127.5F) - 1.0F) * 0.042F;
   }

   public int buildSectionGeometry(PeWorld world, int cx, int sy, int cz, float[] out) {
      int slot = PeWorld.slotIndex(cx, cz);
      PeWorld.PeChunk chunk = world.chunkGrid[slot];
      if (!chunk.loaded || chunk.cx != cx || chunk.cz != cz || chunk.sectionNonAir[sy] <= 0) {
         return 0;
      }
      return meshSection(world, chunk, cx, sy, cz, out);
   }

   private static int meshSection(PeWorld world, PeWorld.PeChunk chunk, int cx, int sy, int cz, float[] out) {
      int ptr = 0;
      int x0 = cx << 4;
      int y0 = sy << 4;
      int z0 = cz << 4;
      int maxFloats = out.length - 72 * FLOATS_PER_VERT_3D;
      byte[] cBlocks = chunk.blocks;
      boolean useBushy = world.bushyLeaves;

      for (int ly = 0; ly < 16; ly++) {
         int y = y0 + ly;
         for (int lz = 0; lz < 16; lz++) {
            int z = z0 + lz;
            int zBase = lz << 11;
            for (int lx = 0; lx < 16; lx++) {
               int colBase = zBase | (lx << 7);
               byte b = cBlocks[colBase | y];
               if (b == PeWorld.B_AIR) continue;
               if (ptr >= maxFloats) return ptr / FLOATS_PER_VERT_3D;

               int x = x0 + lx;
               float boost = PeWorld.isLightEmitter(b) ? 1.0F : 0.0F;
               if (boost == 0.0F) {
                  // Check 6 cardinal neighbors for Torch / Glowstone / Jack o'Lantern / Lava illumination
                  if (PeWorld.isLightEmitter(world.getBlockFast(x, y + 1, z))
                     || PeWorld.isLightEmitter(world.getBlockFast(x + 1, y, z))
                     || PeWorld.isLightEmitter(world.getBlockFast(x - 1, y, z))
                     || PeWorld.isLightEmitter(world.getBlockFast(x, y, z + 1))
                     || PeWorld.isLightEmitter(world.getBlockFast(x, y, z - 1))) {
                     boost = 0.55F;
                  }
               }
               float varJit = blockVariation(x, y, z);

               // 1. Torch: 3D slim wooden post with glowing flame tip
               if (b == PeWorld.B_TORCH) {
                  ptr = emitCrossPlant(out, ptr, x, y, z, PeTextures.T_TORCH, 1.0F);
                  continue;
               }

               // 2. Crossed Plants & Crops (Poppy, Dandelion, Tall Grass, Wheat, Sugar Cane)
               if (PeWorld.isCrossPlant(b)) {
                  ptr = emitCrossPlant(out, ptr, x, y, z, PeWorld.getFaceTile(b, 0), boost);
                  continue;
               }

               // 3. Wall Ladder (B_LADDER)
               if (b == PeWorld.B_LADDER) {
                  ptr = emitLadder(world, out, ptr, x, y, z);
                  continue;
               }

               // 4. Oak Door (B_DOOR_OAK closed or B_DOOR_OAK_OPEN swung 90 degrees open)
               if (b == PeWorld.B_DOOR_OAK || b == PeWorld.B_DOOR_OAK_OPEN) {
                  ptr = emitDoor(out, ptr, x, y, z, b == PeWorld.B_DOOR_OAK_OPEN);
                  continue;
               }

               // 5. Oak Fence (B_FENCE_OAK: center post + connecting rails to adjacent fences/solids)
               if (b == PeWorld.B_FENCE_OAK) {
                  ptr = emitFence(world, out, ptr, x, y, z, varJit);
                  continue;
               }

               // 6. 3D Stairs (B_STAIR_OAK, B_STAIR_COBBLE, B_STAIR_STONEBRICK)
               if (PeWorld.isStair(b)) {
                  ptr = emitCustomBox(out, ptr, x, y, z, 0.0F, 0.0F, 0.0F, 1.0F, 0.5F, 1.0F, b, varJit, boost);
                  ptr = emitCustomBox(out, ptr, x, y, z, 0.0F, 0.5F, 0.0F, 1.0F, 1.0F, 0.5F, b, varJit, boost);
                  continue;
               }

               // 7. Half-Height Slabs, Beds, Recessed Farmland, Inset Chests & Cacti
               if (PeWorld.isSlab(b)) {
                  ptr = emitCustomBox(out, ptr, x, y, z, 0.0F, 0.0F, 0.0F, 1.0F, 0.5F, 1.0F, b, varJit, boost);
                  continue;
               }
               if (b == PeWorld.B_BED_RED) {
                  ptr = emitCustomBox(out, ptr, x, y, z, 0.0F, 0.0F, 0.0F, 1.0F, 0.5625F, 1.0F, b, varJit, boost);
                  continue;
               }
               if (b == PeWorld.B_FARMLAND) {
                  ptr = emitCustomBox(out, ptr, x, y, z, 0.0F, 0.0F, 0.0F, 1.0F, 0.9375F, 1.0F, b, varJit, boost);
                  continue;
               }
               if (b == PeWorld.B_CHEST || b == PeWorld.B_CACTUS) {
                  ptr = emitCustomBox(out, ptr, x, y, z, 0.0625F, 0.0F, 0.0625F, 0.9375F, 0.9375F, 0.9375F, b, varJit, boost);
                  continue;
               }

               // 8. Standard Cube with Smooth Corner Ambient Occlusion (AO) + Optional 3D Bushy Leaf Fronds
               byte nUp = (y < PeWorld.WORLD_H - 1) ? cBlocks[colBase | (y + 1)] : PeWorld.B_AIR;
               byte nDn = (y > 0) ? cBlocks[colBase | (y - 1)] : PeWorld.B_BEDROCK;
               byte nN = (lz > 0) ? cBlocks[((lz - 1) << 11) | (lx << 7) | y] : world.getBlockFast(x, y, z - 1);
               byte nS = (lz < 15) ? cBlocks[((lz + 1) << 11) | (lx << 7) | y] : world.getBlockFast(x, y, z + 1);
               byte nE = (lx < 15) ? cBlocks[zBase | ((lx + 1) << 7) | y] : world.getBlockFast(x + 1, y, z);
               byte nW = (lx > 0) ? cBlocks[zBase | ((lx - 1) << 7) | y] : world.getBlockFast(x - 1, y, z);
               float topY = ((b == PeWorld.B_WATER || b == PeWorld.B_LAVA) && nUp != b) ? 0.88F : 1.0F;

               // All 6 faces use (Top-Left, Bottom-Left, Bottom-Right, Top-Right) so normals point OUTWARD (CCW)
               // and side UVs have v=v0 at top (Y1) and v=v1 at bottom (Y0)!
               // Face 0: +Y (Top, outward normal = (0, +1, 0))
               if (shouldRenderFace(b, nUp, 0)) {
                  int tile = PeWorld.getFaceTile(b, 0);
                  float sBase = 1.0F * varJit;
                  boolean occN = (y < PeWorld.WORLD_H - 1) && PeWorld.isOpaque(world.getBlockFast(x, y + 1, z - 1));
                  boolean occS = (y < PeWorld.WORLD_H - 1) && PeWorld.isOpaque(world.getBlockFast(x, y + 1, z + 1));
                  boolean occW = (y < PeWorld.WORLD_H - 1) && PeWorld.isOpaque(world.getBlockFast(x - 1, y + 1, z));
                  boolean occE = (y < PeWorld.WORLD_H - 1) && PeWorld.isOpaque(world.getBlockFast(x + 1, y + 1, z));
                  float aoNW = sBase * (1.0F - (occN ? 0.12F : 0.0F) - (occW ? 0.12F : 0.0F));
                  float aoSW = sBase * (1.0F - (occS ? 0.12F : 0.0F) - (occW ? 0.12F : 0.0F));
                  float aoSE = sBase * (1.0F - (occS ? 0.12F : 0.0F) - (occE ? 0.12F : 0.0F));
                  float aoNE = sBase * (1.0F - (occN ? 0.12F : 0.0F) - (occE ? 0.12F : 0.0F));
                  ptr = emitFaceAO(out, ptr, tile, boost,
                     x,     y + topY, z,     aoNW,
                     x,     y + topY, z + 1, aoSW,
                     x + 1, y + topY, z + 1, aoSE,
                     x + 1, y + topY, z,     aoNE);
               }
               // Face 1: -Y (Bottom, outward normal = (0, -1, 0))
               if (y > 0 && shouldRenderFace(b, nDn, 1)) {
                  int tile = PeWorld.getFaceTile(b, 1);
                  float s = 0.54F * varJit;
                  ptr = emitFaceAO(out, ptr, tile, boost,
                     x,     y, z + 1, s,
                     x,     y, z,     s,
                     x + 1, y, z,     s,
                     x + 1, y, z + 1, s);
               }
               // Face 2: -Z (North, outward normal = (0, 0, -1), upright UVs: top at y+topY, bottom at y)
               if (shouldRenderFace(b, nN, 2)) {
                  int tile = PeWorld.getFaceTile(b, 2);
                  float sTop = 0.84F * varJit * (PeWorld.isOpaque(world.getBlockFast(x, y + 1, z - 1)) ? 0.84F : 1.0F);
                  float sBot = 0.84F * varJit * (PeWorld.isOpaque(world.getBlockFast(x, y - 1, z - 1)) ? 0.82F : 1.0F);
                  ptr = emitFaceAO(out, ptr, tile, boost,
                     x + 1, y + topY, z, sTop,
                     x + 1, y,        z, sBot,
                     x,     y,        z, sBot,
                     x,     y + topY, z, sTop);
               }
               // Face 3: +Z (South, outward normal = (0, 0, +1), upright UVs: top at y+topY, bottom at y)
               if (shouldRenderFace(b, nS, 3)) {
                  int tile = PeWorld.getFaceTile(b, 3);
                  float sTop = 0.88F * varJit * (PeWorld.isOpaque(world.getBlockFast(x, y + 1, z + 1)) ? 0.84F : 1.0F);
                  float sBot = 0.88F * varJit * (PeWorld.isOpaque(world.getBlockFast(x, y - 1, z + 1)) ? 0.82F : 1.0F);
                  ptr = emitFaceAO(out, ptr, tile, boost,
                     x,     y + topY, z + 1, sTop,
                     x,     y,        z + 1, sBot,
                     x + 1, y,        z + 1, sBot,
                     x + 1, y + topY, z + 1, sTop);
               }
               // Face 4: +X (East, outward normal = (+1, 0, 0), upright UVs: top at y+topY, bottom at y)
               if (shouldRenderFace(b, nE, 4)) {
                  int tile = PeWorld.getFaceTile(b, 4);
                  float sTop = 0.73F * varJit * (PeWorld.isOpaque(world.getBlockFast(x + 1, y + 1, z)) ? 0.84F : 1.0F);
                  float sBot = 0.73F * varJit * (PeWorld.isOpaque(world.getBlockFast(x + 1, y - 1, z)) ? 0.82F : 1.0F);
                  ptr = emitFaceAO(out, ptr, tile, boost,
                     x + 1, y + topY, z + 1, sTop,
                     x + 1, y,        z + 1, sBot,
                     x + 1, y,        z,     sBot,
                     x + 1, y + topY, z,     sTop);
               }
               // Face 5: -X (West, outward normal = (-1, 0, 0), upright UVs: top at y+topY, bottom at y)
               if (shouldRenderFace(b, nW, 5)) {
                  int tile = PeWorld.getFaceTile(b, 5);
                  float sTop = 0.71F * varJit * (PeWorld.isOpaque(world.getBlockFast(x - 1, y + 1, z)) ? 0.84F : 1.0F);
                  float sBot = 0.71F * varJit * (PeWorld.isOpaque(world.getBlockFast(x - 1, y - 1, z)) ? 0.82F : 1.0F);
                  ptr = emitFaceAO(out, ptr, tile, boost,
                     x, y + topY, z,     sTop,
                     x, y,        z,     sBot,
                     x, y,        z + 1, sBot,
                     x, y + topY, z + 1, sTop);
               }

               // Update 3.19 Bushy Leaves: emit protruding diagonal 3D foliage fronds on exposed outer canopy leaves
               if (useBushy && PeWorld.isLeaves(b) && (nUp == PeWorld.B_AIR || nN == PeWorld.B_AIR || nS == PeWorld.B_AIR || nE == PeWorld.B_AIR || nW == PeWorld.B_AIR)) {
                  if (((x + y + z) & 1) == 0 && ptr < maxFloats) {
                     int frondTile = (b == PeWorld.B_LEAVES_SPRUCE) ? PeTextures.T_BUSHY_FROND_SPRUCE
                        : ((b == PeWorld.B_LEAVES_BIRCH) ? PeTextures.T_BUSHY_FROND_BIRCH : PeTextures.T_BUSHY_FROND_OAK);
                     ptr = emitBushyFrond(out, ptr, x, y, z, frondTile, varJit);
                  }
               }
            }
         }
      }
      return ptr / FLOATS_PER_VERT_3D;
   }

   private static int emitBushyFrond(float[] out, int ptr, int x, int y, int z, int tile, float jit) {
      float u0 = ((tile & 15) * 16.0F + 0.15F) / 256.0F;
      float v0 = ((tile >> 4) * 16.0F + 0.15F) / 256.0F;
      float u1 = ((tile & 15) * 16.0F + 15.85F) / 256.0F;
      float v1 = ((tile >> 4) * 16.0F + 15.85F) / 256.0F;
      float sTop = 0.96F * jit;
      float sBot = 0.78F * jit;
      float cx = x + 0.5F;
      float cy = y + 0.5F;
      float cz = z + 0.5F;
      float r = 0.68F;
      float hy = 0.68F;
      ptr = emitQuadTwoSidedAO(out, ptr, u0, v0, u1, v1, sTop, sBot, 0.0F,
         cx - r, cy + hy, cz - r,
         cx + r, cy + hy, cz + r,
         cx + r, cy - hy, cz + r,
         cx - r, cy - hy, cz - r);
      ptr = emitQuadTwoSidedAO(out, ptr, u0, v0, u1, v1, sTop, sBot, 0.0F,
         cx - r, cy + hy, cz + r,
         cx + r, cy + hy, cz - r,
         cx + r, cy - hy, cz - r,
         cx - r, cy - hy, cz + r);
      return ptr;
   }

   private static int emitCustomBox(float[] out, int ptr, int x, int y, int z,
                                    float x0, float y0, float z0, float x1, float y1, float z1,
                                    byte b, float jit, float boost) {
      float X0 = x + x0, Y0 = y + y0, Z0 = z + z0;
      float X1 = x + x1, Y1 = y + y1, Z1 = z + z1;
      // Top (+Y)
      ptr = emitFaceAO(out, ptr, PeWorld.getFaceTile(b, 0), boost,
         X0, Y1, Z0, 1.0F * jit,
         X0, Y1, Z1, 1.0F * jit,
         X1, Y1, Z1, 1.0F * jit,
         X1, Y1, Z0, 1.0F * jit);
      // Bottom (-Y)
      ptr = emitFaceAO(out, ptr, PeWorld.getFaceTile(b, 1), boost,
         X0, Y0, Z1, 0.55F * jit,
         X0, Y0, Z0, 0.55F * jit,
         X1, Y0, Z0, 0.55F * jit,
         X1, Y0, Z1, 0.55F * jit);
      // North (-Z, outward normal = (0, 0, -1), upright UVs)
      ptr = emitFaceAO(out, ptr, PeWorld.getFaceTile(b, 2), boost,
         X1, Y1, Z0, 0.84F * jit,
         X1, Y0, Z0, 0.78F * jit,
         X0, Y0, Z0, 0.78F * jit,
         X0, Y1, Z0, 0.84F * jit);
      // South (+Z, outward normal = (0, 0, +1), upright UVs)
      ptr = emitFaceAO(out, ptr, PeWorld.getFaceTile(b, 3), boost,
         X0, Y1, Z1, 0.88F * jit,
         X0, Y0, Z1, 0.80F * jit,
         X1, Y0, Z1, 0.80F * jit,
         X1, Y1, Z1, 0.88F * jit);
      // East (+X, outward normal = (+1, 0, 0), upright UVs)
      ptr = emitFaceAO(out, ptr, PeWorld.getFaceTile(b, 4), boost,
         X1, Y1, Z1, 0.74F * jit,
         X1, Y0, Z1, 0.68F * jit,
         X1, Y0, Z0, 0.68F * jit,
         X1, Y1, Z0, 0.74F * jit);
      // West (-X, outward normal = (-1, 0, 0), upright UVs)
      ptr = emitFaceAO(out, ptr, PeWorld.getFaceTile(b, 5), boost,
         X0, Y1, Z0, 0.72F * jit,
         X0, Y0, Z0, 0.66F * jit,
         X0, Y0, Z1, 0.66F * jit,
         X0, Y1, Z1, 0.72F * jit);
      return ptr;
   }

   private static int emitFence(PeWorld world, float[] out, int ptr, int x, int y, int z, float jit) {
      // Center vertical post (0.375..0.625)
      ptr = emitCustomBox(out, ptr, x, y, z, 0.375F, 0.0F, 0.375F, 0.625F, 1.0F, 0.625F, PeWorld.B_PLANKS_OAK, jit, 0.0F);
      boolean connN = PeWorld.isSolid(world.getBlockFast(x, y, z - 1));
      boolean connS = PeWorld.isSolid(world.getBlockFast(x, y, z + 1));
      boolean connW = PeWorld.isSolid(world.getBlockFast(x - 1, y, z));
      boolean connE = PeWorld.isSolid(world.getBlockFast(x + 1, y, z));
      if (connN) {
         ptr = emitCustomBox(out, ptr, x, y, z, 0.4375F, 0.375F, 0.0F, 0.5625F, 0.5625F, 0.375F, PeWorld.B_PLANKS_OAK, jit, 0.0F);
         ptr = emitCustomBox(out, ptr, x, y, z, 0.4375F, 0.75F, 0.0F, 0.5625F, 0.9375F, 0.375F, PeWorld.B_PLANKS_OAK, jit, 0.0F);
      }
      if (connS) {
         ptr = emitCustomBox(out, ptr, x, y, z, 0.4375F, 0.375F, 0.625F, 0.5625F, 0.5625F, 1.0F, PeWorld.B_PLANKS_OAK, jit, 0.0F);
         ptr = emitCustomBox(out, ptr, x, y, z, 0.4375F, 0.75F, 0.625F, 0.5625F, 0.9375F, 1.0F, PeWorld.B_PLANKS_OAK, jit, 0.0F);
      }
      if (connW) {
         ptr = emitCustomBox(out, ptr, x, y, z, 0.0F, 0.375F, 0.4375F, 0.375F, 0.5625F, 0.5625F, PeWorld.B_PLANKS_OAK, jit, 0.0F);
         ptr = emitCustomBox(out, ptr, x, y, z, 0.0F, 0.75F, 0.4375F, 0.375F, 0.9375F, 0.5625F, PeWorld.B_PLANKS_OAK, jit, 0.0F);
      }
      if (connE) {
         ptr = emitCustomBox(out, ptr, x, y, z, 0.625F, 0.375F, 0.4375F, 1.0F, 0.5625F, 0.5625F, PeWorld.B_PLANKS_OAK, jit, 0.0F);
         ptr = emitCustomBox(out, ptr, x, y, z, 0.625F, 0.75F, 0.4375F, 1.0F, 0.9375F, 0.5625F, PeWorld.B_PLANKS_OAK, jit, 0.0F);
      }
      return ptr;
   }

   private static int emitDoor(float[] out, int ptr, int x, int y, int z, boolean open) {
      // Lower half (y .. y+1) with T_DOOR_BOTTOM, upper half (y+1 .. y+2) with T_DOOR_TOP
      float u0B = ((PeTextures.T_DOOR_BOTTOM & 15) * 16.0F + 0.15F) / 256.0F;
      float v0B = ((PeTextures.T_DOOR_BOTTOM >> 4) * 16.0F + 0.15F) / 256.0F;
      float u1B = ((PeTextures.T_DOOR_BOTTOM & 15) * 16.0F + 15.85F) / 256.0F;
      float v1B = ((PeTextures.T_DOOR_BOTTOM >> 4) * 16.0F + 15.85F) / 256.0F;

      float u0T = ((PeTextures.T_DOOR_TOP & 15) * 16.0F + 0.15F) / 256.0F;
      float v0T = ((PeTextures.T_DOOR_TOP >> 4) * 16.0F + 0.15F) / 256.0F;
      float u1T = ((PeTextures.T_DOOR_TOP & 15) * 16.0F + 15.85F) / 256.0F;
      float v1T = ((PeTextures.T_DOOR_TOP >> 4) * 16.0F + 15.85F) / 256.0F;

      if (open) {
         float xPos = x + 0.08F;
         ptr = emitQuadTwoSidedAO(out, ptr, u0B, v0B, u1B, v1B, 0.92F, 0.84F, 0.0F,
            xPos, y + 1.0F, z,
            xPos, y + 1.0F, z + 1,
            xPos, y,        z + 1,
            xPos, y,        z);
         ptr = emitQuadTwoSidedAO(out, ptr, u0T, v0T, u1T, v1T, 0.95F, 0.90F, 0.0F,
            xPos, y + 2.0F, z,
            xPos, y + 2.0F, z + 1,
            xPos, y + 1.0F, z + 1,
            xPos, y + 1.0F, z);
         return ptr;
      }

      float zPos = z + 0.5F;
      ptr = emitQuadTwoSidedAO(out, ptr, u0B, v0B, u1B, v1B, 0.92F, 0.84F, 0.0F,
         x,     y + 1.0F, zPos,
         x + 1, y + 1.0F, zPos,
         x + 1, y,        zPos,
         x,     y,        zPos);
      ptr = emitQuadTwoSidedAO(out, ptr, u0T, v0T, u1T, v1T, 0.95F, 0.90F, 0.0F,
         x,     y + 2.0F, zPos,
         x + 1, y + 2.0F, zPos,
         x + 1, y + 1.0F, zPos,
         x,     y + 1.0F, zPos);
      return ptr;
   }

   private static int emitLadder(PeWorld world, float[] out, int ptr, int x, int y, int z) {
      int tile = PeTextures.T_LADDER;
      float u0 = ((tile & 15) * 16.0F + 0.15F) / 256.0F;
      float v0 = ((tile >> 4) * 16.0F + 0.15F) / 256.0F;
      float u1 = ((tile & 15) * 16.0F + 15.85F) / 256.0F;
      float v1 = ((tile >> 4) * 16.0F + 15.85F) / 256.0F;
      if (PeWorld.isOpaque(world.getBlockFast(x, y, z - 1))) {
         return emitQuadTwoSidedAO(out, ptr, u0, v0, u1, v1, 0.92F, 0.86F, 0.0F,
            x,     y + 1.0F, z + 0.06F,
            x + 1, y + 1.0F, z + 0.06F,
            x + 1, y,        z + 0.06F,
            x,     y,        z + 0.06F);
      }
      if (PeWorld.isOpaque(world.getBlockFast(x, y, z + 1))) {
         return emitQuadTwoSidedAO(out, ptr, u0, v0, u1, v1, 0.92F, 0.86F, 0.0F,
            x + 1, y + 1.0F, z + 0.94F,
            x,     y + 1.0F, z + 0.94F,
            x,     y,        z + 0.94F,
            x + 1, y,        z + 0.94F);
      }
      if (PeWorld.isOpaque(world.getBlockFast(x - 1, y, z))) {
         return emitQuadTwoSidedAO(out, ptr, u0, v0, u1, v1, 0.92F, 0.86F, 0.0F,
            x + 0.06F, y + 1.0F, z + 1,
            x + 0.06F, y + 1.0F, z,
            x + 0.06F, y,        z,
            x + 0.06F, y,        z + 1);
      }
      return emitQuadTwoSidedAO(out, ptr, u0, v0, u1, v1, 0.92F, 0.86F, 0.0F,
         x + 0.94F, y + 1.0F, z,
         x + 0.94F, y + 1.0F, z + 1,
         x + 0.94F, y,        z + 1,
         x + 0.94F, y,        z);
   }

   private static int emitFaceAO(float[] out, int ptr, int tile, float boost,
                                 float x0, float y0, float z0, float s0,
                                 float x1, float y1, float z1, float s1,
                                 float x2, float y2, float z2, float s2,
                                 float x3, float y3, float z3, float s3) {
      float u0 = ((tile & 15) * 16.0F + 0.15F) / 256.0F;
      float v0 = ((tile >> 4) * 16.0F + 0.15F) / 256.0F;
      float u1 = ((tile & 15) * 16.0F + 15.85F) / 256.0F;
      float v1 = ((tile >> 4) * 16.0F + 15.85F) / 256.0F;
      float b0 = Math.min(1.35F, s0 + boost * 0.35F);
      float b1 = Math.min(1.35F, s1 + boost * 0.35F);
      float b2 = Math.min(1.35F, s2 + boost * 0.35F);
      float b3 = Math.min(1.35F, s3 + boost * 0.35F);

      ptr = putV3D(out, ptr, x0, y0, z0, u0, v0, b0, b0, b0, 1.0F);
      ptr = putV3D(out, ptr, x1, y1, z1, u0, v1, b1, b1, b1, 1.0F);
      ptr = putV3D(out, ptr, x2, y2, z2, u1, v1, b2, b2, b2, 1.0F);

      ptr = putV3D(out, ptr, x0, y0, z0, u0, v0, b0, b0, b0, 1.0F);
      ptr = putV3D(out, ptr, x2, y2, z2, u1, v1, b2, b2, b2, 1.0F);
      ptr = putV3D(out, ptr, x3, y3, z3, u1, v0, b3, b3, b3, 1.0F);
      return ptr;
   }

   private static int emitCrossPlant(float[] out, int ptr, int x, int y, int z, int tile, float boost) {
      float u0 = ((tile & 15) * 16.0F + 0.15F) / 256.0F;
      float v0 = ((tile >> 4) * 16.0F + 0.15F) / 256.0F;
      float u1 = ((tile & 15) * 16.0F + 15.85F) / 256.0F;
      float v1 = ((tile >> 4) * 16.0F + 15.85F) / 256.0F;
      float sTop = 0.95F;
      float sBot = 0.80F;

      ptr = emitQuadTwoSidedAO(out, ptr, u0, v0, u1, v1, sTop, sBot, boost,
         x + 0.15F, y + 1.0F, z + 0.15F,
         x + 0.85F, y + 1.0F, z + 0.85F,
         x + 0.85F, y,        z + 0.85F,
         x + 0.15F, y,        z + 0.15F);
      ptr = emitQuadTwoSidedAO(out, ptr, u0, v0, u1, v1, sTop, sBot, boost,
         x + 0.15F, y + 1.0F, z + 0.85F,
         x + 0.85F, y + 1.0F, z + 0.15F,
         x + 0.85F, y,        z + 0.15F,
         x + 0.15F, y,        z + 0.85F);
      return ptr;
   }

   private static int emitQuadTwoSidedAO(float[] out, int ptr,
                                         float u0, float v0, float u1, float v1,
                                         float sTop, float sBot, float boost,
                                         float x0, float y0, float z0,
                                         float x1, float y1, float z1,
                                         float x2, float y2, float z2,
                                         float x3, float y3, float z3) {
      float bT = Math.min(1.35F, sTop + boost * 0.35F);
      float bB = Math.min(1.35F, sBot + boost * 0.35F);
      ptr = putV3D(out, ptr, x0, y0, z0, u0, v0, bT, bT, bT, 1.0F);
      ptr = putV3D(out, ptr, x3, y3, z3, u0, v1, bB, bB, bB, 1.0F);
      ptr = putV3D(out, ptr, x2, y2, z2, u1, v1, bB, bB, bB, 1.0F);
      ptr = putV3D(out, ptr, x0, y0, z0, u0, v0, bT, bT, bT, 1.0F);
      ptr = putV3D(out, ptr, x2, y2, z2, u1, v1, bB, bB, bB, 1.0F);
      ptr = putV3D(out, ptr, x1, y1, z1, u1, v0, bT, bT, bT, 1.0F);

      ptr = putV3D(out, ptr, x1, y1, z1, u0, v0, bT, bT, bT, 1.0F);
      ptr = putV3D(out, ptr, x2, y2, z2, u0, v1, bB, bB, bB, 1.0F);
      ptr = putV3D(out, ptr, x3, y3, z3, u1, v1, bB, bB, bB, 1.0F);
      ptr = putV3D(out, ptr, x1, y1, z1, u0, v0, bT, bT, bT, 1.0F);
      ptr = putV3D(out, ptr, x3, y3, z3, u1, v1, bB, bB, bB, 1.0F);
      ptr = putV3D(out, ptr, x0, y0, z0, u1, v0, bT, bT, bT, 1.0F);
      return ptr;
   }

   private static int putV3D(float[] out, int p,
                             float x, float y, float z,
                             float u, float v,
                             float r, float g, float b, float a) {
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

   // ====================================================================
   //  Dynamic 3D Batch (3D Sky Sun/Moon/Clouds, Selection Box, 1st-Person Hand)
   // ====================================================================
   public void beginDynamic3D() {
      this.dyn3DVerts = 0;
   }

   /**
    * Renders PC Update 3.5+ 3D Sky Elements:
    * - Orbiting 3D Sun (T_SUN) & Moon (T_MOON) across the sky vault
    * - Twinkling Starfield at dusk/night
    * - Drifting 3D Cloud Layer at Y = 106 above the infinite world
    */
   public void addSkyAndClouds3D(PeWorld world, float camX, float camY, float camZ, float sunBright) {
      double angle = world.dayTime * Math.PI * 2.0;
      float sunDist = 68.0F;
      float sx = camX + (float)Math.cos(angle) * sunDist;
      float sy = camY + (float)Math.sin(angle) * sunDist;
      float sz = camZ - 14.0F;

      // 1. 3D Sun Quad (facing camera)
      if (sy > camY - 18.0F) {
         float r = 7.5F;
         addDynamicBillboardQuad(sx, sy, sz, r, PeTextures.T_SUN, 1.0F, 0.98F, 0.88F, 1.0F);
      }

      // 2. 3D Moon Quad (opposite the Sun)
      float mx = camX - (float)Math.cos(angle) * sunDist;
      float my = camY - (float)Math.sin(angle) * sunDist;
      float mz = camZ + 14.0F;
      if (my > camY - 18.0F) {
         float r = 6.2F;
         addDynamicBillboardQuad(mx, my, mz, r, PeTextures.T_MOON, 0.92F, 0.96F, 1.0F, 1.0F);
      }

      // 3. Night & Twilight Starfield overhead
      if (sunBright < 0.72F) {
         float starBright = Math.min(1.0F, (0.72F - sunBright) * 2.2F);
         for (int i = 0; i < 28; i++) {
            double a1 = (i * 1.61803398875) * Math.PI * 2.0;
            double elev = 0.25 + ((i * 7) % 13) / 16.0;
            float stX = camX + (float)(Math.cos(a1) * Math.cos(elev) * 64.0);
            float stY = camY + (float)(Math.sin(elev) * 58.0);
            float stZ = camZ + (float)(Math.sin(a1) * Math.cos(elev) * 64.0);
            addDynamicBox(stX - 0.35F, stY - 0.35F, stZ - 0.35F,
               stX + 0.35F, stY + 0.35F, stZ + 0.35F,
               PeTextures.T_SNOW, starBright, starBright, starBright, 1.0F);
         }
      }

      // 4. Drifting 3D Volumetric Cloud Layer at Y = 104
      float cloudY = 104.0F;
      float drift = (world.tickCount % 4800L) * 0.025F;
      int baseCellX = ((int)Math.floor((camX + drift) / 16.0F));
      int baseCellZ = ((int)Math.floor(camZ / 16.0F));
      float cShade = 0.35F + 0.65F * sunBright;

      for (int dz = -4; dz <= 4; dz++) {
         for (int dx = -4; dx <= 4; dx++) {
            int cx = baseCellX + dx;
            int cz = baseCellZ + dz;
            int h = (cx * 374761393) ^ (cz * 668265263);
            h = (h ^ (h >>> 13)) * 1274126177;
            if ((h & 7) <= 1) {
               float wx0 = cx * 16.0F - drift;
               float wz0 = cz * 16.0F;
               float wSpan = ((h & 8) != 0) ? 24.0F : 16.0F;
               float dSpan = ((h & 16) != 0) ? 20.0F : 14.0F;
               addDynamicBox(wx0, cloudY, wz0, wx0 + wSpan, cloudY + 2.6F, wz0 + dSpan,
                  PeTextures.T_SNOW, cShade, cShade, cShade * 1.02F, 0.85F);
            }
         }
      }
   }

   private void addDynamicBillboardQuad(float cx, float cy, float cz, float r, int tile,
                                        float cr, float cg, float cb, float boost) {
      int p = this.dyn3DVerts * FLOATS_PER_VERT_3D;
      if (p + 12 * FLOATS_PER_VERT_3D >= this.dyn3DData.length) return;
      float u0 = ((tile & 15) * 16.0F + 0.15F) / 256.0F;
      float v0 = ((tile >> 4) * 16.0F + 0.15F) / 256.0F;
      float u1 = ((tile & 15) * 16.0F + 15.85F) / 256.0F;
      float v1 = ((tile >> 4) * 16.0F + 15.85F) / 256.0F;
      p = emitQuadTwoSidedAO(this.dyn3DData, p, u0, v0, u1, v1, cr, cg * 0.96F, boost,
         cx - r, cy + r, cz - r * 0.4F,
         cx + r, cy + r, cz + r * 0.4F,
         cx + r, cy - r, cz + r * 0.4F,
         cx - r, cy - r, cz - r * 0.4F);
      this.dyn3DVerts = p / FLOATS_PER_VERT_3D;
   }

   public void addBox3D(float x0, float y0, float z0, float x1, float y1, float z1,
                        int tile, float r, float g, float b, float a) {
      addDynamicBox(x0, y0, z0, x1, y1, z1, tile, r, g, b, a);
   }

   public void addDynamicBox(float x0, float y0, float z0, float x1, float y1, float z1,
                             int tile, float r, float g, float b, float a) {
      int p = this.dyn3DVerts * FLOATS_PER_VERT_3D;
      if (p + 36 * FLOATS_PER_VERT_3D >= this.dyn3DData.length) return;

      float u0 = ((tile & 15) * 16.0F + 0.15F) / 256.0F;
      float v0 = ((tile >> 4) * 16.0F + 0.15F) / 256.0F;
      float u1 = ((tile & 15) * 16.0F + 15.85F) / 256.0F;
      float v1 = ((tile >> 4) * 16.0F + 15.85F) / 256.0F;

      // Top (+Y)
      p = emitDynamicQuad(this.dyn3DData, p, u0, v0, u1, v1, r, g, b, a,
         x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
      // Bottom (-Y)
      p = emitDynamicQuad(this.dyn3DData, p, u0, v0, u1, v1, r * 0.6F, g * 0.6F, b * 0.6F, a,
         x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1);
      // North (-Z, outward normal = (0, 0, -1), upright UVs)
      p = emitDynamicQuad(this.dyn3DData, p, u0, v0, u1, v1, r * 0.85F, g * 0.85F, b * 0.85F, a,
         x1, y1, z0, x1, y0, z0, x0, y0, z0, x0, y1, z0);
      // South (+Z, outward normal = (0, 0, +1), upright UVs)
      p = emitDynamicQuad(this.dyn3DData, p, u0, v0, u1, v1, r * 0.9F, g * 0.9F, b * 0.9F, a,
         x0, y1, z1, x0, y0, z1, x1, y0, z1, x1, y1, z1);
      // East (+X, outward normal = (+1, 0, 0), upright UVs)
      p = emitDynamicQuad(this.dyn3DData, p, u0, v0, u1, v1, r * 0.75F, g * 0.75F, b * 0.75F, a,
         x1, y1, z1, x1, y0, z1, x1, y0, z0, x1, y1, z0);
      // West (-X, outward normal = (-1, 0, 0), upright UVs)
      p = emitDynamicQuad(this.dyn3DData, p, u0, v0, u1, v1, r * 0.72F, g * 0.72F, b * 0.72F, a,
         x0, y1, z0, x0, y0, z0, x0, y0, z1, x0, y1, z1);

      this.dyn3DVerts = p / FLOATS_PER_VERT_3D;
   }

   /**
    * PC Update 3.10 Animated 3D Falling Trees Renderer:
    * Smoothly rotates every trunk log and canopy leaf block of each active FallingTree
    * around its cut hinge pivot (pivotX + 0.5, pivotY, pivotZ + 0.5) by getAngleDegrees(partial)
    * from 0..90 degrees at 120Hz!
    */
   public void addFallingTrees3D(PeWorld world, float partial, float dayBright) {
      if (world.fallingTrees.isEmpty()) return;
      for (int tIdx = 0; tIdx < world.fallingTrees.size(); tIdx++) {
         PeWorld.FallingTree tree = world.fallingTrees.get(tIdx);
         if (tree.done) continue;
         float deg = tree.getAngleDegrees(partial);
         double rad = Math.toRadians(deg);
         float cosA = (float)Math.cos(rad);
         float sinA = (float)Math.sin(rad);
         float pvx = tree.pivotX + 0.5F;
         float pvy = tree.pivotY;
         float pvz = tree.pivotZ + 0.5F;
         int dx = tree.dx;
         int dz = tree.dz;

         for (int i = 0; i < tree.nTotal; i++) {
            int p = this.dyn3DVerts * FLOATS_PER_VERT_3D;
            if (p + 36 * FLOATS_PER_VERT_3D >= this.dyn3DData.length) break;
            byte b = tree.blockId[i];
            float ox0 = tree.offX[i] - 0.5F;
            float ox1 = ox0 + 1.0F;
            float oy0 = tree.offY[i];
            float oy1 = oy0 + 1.0F;
            float oz0 = tree.offZ[i] - 0.5F;
            float oz1 = oz0 + 1.0F;

            // Compute rotated 8 corners around hinge (pvx, pvy, pvz) along (dx, dz)
            float[] cx = new float[8];
            float[] cy = new float[8];
            float[] cz = new float[8];
            for (int c = 0; c < 8; c++) {
               float lx = ((c & 1) != 0) ? ox1 : ox0;
               float ly = ((c & 2) != 0) ? oy1 : oy0;
               float lz = ((c & 4) != 0) ? oz1 : oz0;
               float along = lx * dx + lz * dz;
               float side = -lx * dz + lz * dx;
               float rotAlong = along * cosA + ly * sinA;
               float rotUp = ly * cosA - along * sinA;
               cx[c] = pvx + rotAlong * dx - side * dz;
               cy[c] = pvy + rotUp;
               cz[c] = pvz + rotAlong * dz + side * dx;
            }
            p = emitOrientedBlockFaces(this.dyn3DData, p, cx, cy, cz, b, dayBright);
            this.dyn3DVerts = p / FLOATS_PER_VERT_3D;
         }
      }
   }

   /**
    * Renders True 3D First-Person Held Tools, Weapons, Torches & 6-Face 3D Blocks
    * in the camera's local coordinate frame (right, up, fwd) with 120Hz walk bobbing & swing arc!
    */
   public void addFirstPersonHeld3D(int heldId, float camX, float camY, float camZ,
                                    float yawDeg, float pitchDeg, float swingProgress,
                                    float walkBob, float dayBright) {
      float yawRad = (float)Math.toRadians(yawDeg);
      float pitchRad = (float)Math.toRadians(pitchDeg);
      float cosP = (float)Math.cos(pitchRad);
      float sinP = (float)Math.sin(pitchRad);
      float sinY = (float)Math.sin(yawRad);
      float cosY = (float)Math.cos(yawRad);

      // Right-handed camera basis vectors: Right (+X_cam), Up (+Y_cam), Forward (+Z_cam into scene)
      float rx = cosY,           ry = 0.0F,  rz = sinY;
      float fx = sinY * cosP,    fy = -sinP, fz = -cosY * cosP;
      float ux = sinY * sinP,    uy = cosP,  uz = -cosY * sinP;

      float swSin = (swingProgress >= 0.0F) ? (float)Math.sin(swingProgress * Math.PI) : 0.0F;
      float swArc = (swingProgress >= 0.0F) ? (float)Math.sin(swingProgress * Math.PI * 0.85F) : 0.0F;

      // Hand anchor in front-right of camera
      float hx = camX + fx * (0.56F - swSin * 0.14F) + rx * (0.36F - swSin * 0.12F) + ux * (-0.26F - swArc * 0.14F + walkBob);
      float hy = camY + fy * (0.56F - swSin * 0.14F) + ry * (0.36F - swSin * 0.12F) + uy * (-0.26F - swArc * 0.14F + walkBob);
      float hz = camZ + fz * (0.56F - swSin * 0.14F) + rz * (0.36F - swSin * 0.12F) + uz * (-0.26F - swArc * 0.14F + walkBob);

      // Pitch the tool forward during swing
      float tilt = 0.35F + swSin * 0.85F;
      float cosT = (float)Math.cos(tilt);
      float sinT = (float)Math.sin(tilt);
      float tUx = ux * cosT + fx * sinT;
      float tUy = uy * cosT + fy * sinT;
      float tUz = uz * cosT + fz * sinT;
      float tFx = fx * cosT - ux * sinT;
      float tFy = fy * cosT - uy * sinT;
      float tFz = fz * cosT - uz * sinT;

      float lit = Math.max(0.45F, dayBright);

      // 1. Empty Hand: 3D Angled First-Person Player Arm
      if (heldId == 0) {
         addLocalBox3D(hx, hy, hz, rx, ry, rz, tUx, tUy, tUz, tFx, tFy, tFz,
            -0.045F, -0.05F, -0.14F, 0.045F, 0.05F, 0.14F,
            PeTextures.T_SANDSTONE_TOP, lit * 0.94F, lit * 0.74F, lit * 0.58F);
         return;
      }

      // 2. 3D Pickaxe (Wood, Iron, Diamond) — 3D Handle Shaft + Cross-Head Bar + Left/Right Curved Pick Tips + Top Cap
      if (heldId == PeWorld.I_PICK_WOOD || heldId == PeWorld.I_PICK_IRON || heldId == PeWorld.I_PICK_DIAMOND) {
         int headTile = (heldId == PeWorld.I_PICK_DIAMOND) ? PeTextures.T_DIAMOND_BLOCK
            : ((heldId == PeWorld.I_PICK_IRON) ? PeTextures.T_IRON_BLOCK : PeTextures.T_PLANKS_BIRCH);
         // 3D Oak Handle Shaft
         addLocalBox3D(hx, hy, hz, rx, ry, rz, tUx, tUy, tUz, tFx, tFy, tFz,
            -0.018F, -0.15F, -0.018F, 0.018F, 0.17F, 0.018F,
            PeTextures.T_PLANKS_OAK, lit, lit, lit);
         // 3D Pickaxe Cross-Head Bar
         addLocalBox3D(hx, hy, hz, rx, ry, rz, tUx, tUy, tUz, tFx, tFy, tFz,
            -0.024F, 0.11F, -0.13F, 0.024F, 0.155F, 0.13F,
            headTile, lit, lit, lit);
         // Curved Front & Back Pick Tips
         addLocalBox3D(hx, hy, hz, rx, ry, rz, tUx, tUy, tUz, tFx, tFy, tFz,
            -0.020F, 0.075F, 0.10F, 0.020F, 0.125F, 0.165F,
            headTile, lit * 0.94F, lit * 0.94F, lit * 0.94F);
         addLocalBox3D(hx, hy, hz, rx, ry, rz, tUx, tUy, tUz, tFx, tFy, tFz,
            -0.020F, 0.075F, -0.165F, 0.020F, 0.125F, -0.10F,
            headTile, lit * 0.94F, lit * 0.94F, lit * 0.94F);
         // Top Cap Lug
         addLocalBox3D(hx, hy, hz, rx, ry, rz, tUx, tUy, tUz, tFx, tFy, tFz,
            -0.020F, 0.155F, -0.024F, 0.020F, 0.185F, 0.024F,
            PeTextures.T_PLANKS_OAK, lit * 0.9F, lit * 0.9F, lit * 0.9F);
         return;
      }

      // 3. 3D Sword (Wood, Iron, Diamond) — 3D Pommel + Grip + Wide Crossguard + Long Tapered 3D Blade
      if (heldId == PeWorld.I_SWORD_WOOD || heldId == PeWorld.I_SWORD_IRON || heldId == PeWorld.I_SWORD_DIAMOND) {
         int bladeTile = (heldId == PeWorld.I_SWORD_DIAMOND) ? PeTextures.T_DIAMOND_BLOCK
            : ((heldId == PeWorld.I_SWORD_IRON) ? PeTextures.T_IRON_BLOCK : PeTextures.T_PLANKS_OAK);
         int guardTile = (heldId == PeWorld.I_SWORD_DIAMOND) ? PeTextures.T_GOLD_BLOCK : PeTextures.T_IRON_BLOCK;
         // Pommel
         addLocalBox3D(hx, hy, hz, rx, ry, rz, tUx, tUy, tUz, tFx, tFy, tFz,
            -0.025F, -0.165F, -0.025F, 0.025F, -0.125F, 0.025F,
            guardTile, lit, lit, lit);
         // Grip
         addLocalBox3D(hx, hy, hz, rx, ry, rz, tUx, tUy, tUz, tFx, tFy, tFz,
            -0.017F, -0.125F, -0.020F, 0.017F, -0.025F, 0.020F,
            PeTextures.T_LOG_OAK_SIDE, lit, lit, lit);
         // Wide 3D Crossguard
         addLocalBox3D(hx, hy, hz, rx, ry, rz, tUx, tUy, tUz, tFx, tFy, tFz,
            -0.024F, -0.025F, -0.095F, 0.024F, 0.012F, 0.095F,
            guardTile, lit, lit, lit);
         // Main 3D Double-Edged Blade
         addLocalBox3D(hx, hy, hz, rx, ry, rz, tUx, tUy, tUz, tFx, tFy, tFz,
            -0.014F, 0.012F, -0.042F, 0.014F, 0.255F, 0.042F,
            bladeTile, lit, lit, lit);
         // Pointed Blade Tip
         addLocalBox3D(hx, hy, hz, rx, ry, rz, tUx, tUy, tUz, tFx, tFy, tFz,
            -0.011F, 0.255F, -0.024F, 0.011F, 0.305F, 0.024F,
            bladeTile, lit * 1.05F, lit * 1.05F, lit * 1.05F);
         return;
      }

      // 4. 3D Axe (I_AXE_IRON) — 3D Handle Shaft + Sweeping Iron Axe Bit + Back Poll
      if (heldId == PeWorld.I_AXE_IRON) {
         addLocalBox3D(hx, hy, hz, rx, ry, rz, tUx, tUy, tUz, tFx, tFy, tFz,
            -0.018F, -0.15F, -0.018F, 0.018F, 0.175F, 0.018F,
            PeTextures.T_PLANKS_OAK, lit, lit, lit);
         addLocalBox3D(hx, hy, hz, rx, ry, rz, tUx, tUy, tUz, tFx, tFy, tFz,
            -0.024F, 0.085F, -0.045F, 0.024F, 0.165F, 0.095F,
            PeTextures.T_IRON_BLOCK, lit, lit, lit);
         addLocalBox3D(hx, hy, hz, rx, ry, rz, tUx, tUy, tUz, tFx, tFy, tFz,
            -0.018F, 0.055F, 0.055F, 0.018F, 0.175F, 0.135F,
            PeTextures.T_IRON_BLOCK, lit * 1.04F, lit * 1.04F, lit * 1.04F);
         return;
      }

      // 5. 3D Shovel (I_SHOVEL_DIAMOND) — 3D Handle Shaft + Beveled Diamond Spade Scoop
      if (heldId == PeWorld.I_SHOVEL_DIAMOND) {
         addLocalBox3D(hx, hy, hz, rx, ry, rz, tUx, tUy, tUz, tFx, tFy, tFz,
            -0.017F, -0.15F, -0.017F, 0.017F, 0.12F, 0.017F,
            PeTextures.T_PLANKS_OAK, lit, lit, lit);
         addLocalBox3D(hx, hy, hz, rx, ry, rz, tUx, tUy, tUz, tFx, tFy, tFz,
            -0.055F, 0.115F, -0.016F, 0.055F, 0.235F, 0.016F,
            PeTextures.T_DIAMOND_BLOCK, lit, lit, lit);
         return;
      }

      // 6. 3D Bow (I_BOW) — Curved 6-Segment 3D Wooden Recurve Bow + Bowstring + Nocked Arrow
      if (heldId == PeWorld.I_BOW) {
         // Central wrapped leather riser grip curving forward (+Z)
         addLocalBox3D(hx, hy, hz, rx, ry, rz, tUx, tUy, tUz, tFx, tFy, tFz,
            -0.018F, -0.065F, 0.045F, 0.018F, 0.065F, 0.082F,
            PeTextures.T_LOG_SPRUCE, lit, lit, lit);
         // Upper & lower mid-limbs curving back toward string
         addLocalBox3D(hx, hy, hz, rx, ry, rz, tUx, tUy, tUz, tFx, tFy, tFz,
            -0.016F, 0.055F, 0.012F, 0.016F, 0.135F, 0.052F,
            PeTextures.T_PLANKS_OAK, lit, lit, lit);
         addLocalBox3D(hx, hy, hz, rx, ry, rz, tUx, tUy, tUz, tFx, tFy, tFz,
            -0.016F, -0.135F, 0.012F, 0.016F, -0.055F, 0.052F,
            PeTextures.T_PLANKS_OAK, lit, lit, lit);
         // Upper & lower nock tips meeting the bowstring at z = -0.028
         addLocalBox3D(hx, hy, hz, rx, ry, rz, tUx, tUy, tUz, tFx, tFy, tFz,
            -0.014F, 0.125F, -0.030F, 0.014F, 0.195F, 0.018F,
            PeTextures.T_LOG_OAK_SIDE, lit, lit, lit);
         addLocalBox3D(hx, hy, hz, rx, ry, rz, tUx, tUy, tUz, tFx, tFy, tFz,
            -0.014F, -0.195F, -0.030F, 0.014F, -0.125F, 0.018F,
            PeTextures.T_LOG_OAK_SIDE, lit, lit, lit);
         // Taut vertical braided bowstring connecting upper & lower nock tips
         addLocalBox3D(hx, hy, hz, rx, ry, rz, tUx, tUy, tUz, tFx, tFy, tFz,
            -0.005F, -0.188F, -0.032F, 0.005F, 0.188F, -0.022F,
            PeTextures.T_WOOL_WHITE, lit, lit, lit);
         // Nocked 3D arrow resting across the riser
         addLocalBox3D(hx, hy, hz, rx, ry, rz, tUx, tUy, tUz, tFx, tFy, tFz,
            -0.007F, -0.007F, -0.028F, 0.007F, 0.007F, 0.135F,
            PeTextures.T_PLANKS_OAK, lit, lit, lit);
         addLocalBox3D(hx, hy, hz, rx, ry, rz, tUx, tUy, tUz, tFx, tFy, tFz,
            -0.015F, -0.015F, 0.130F, 0.015F, 0.015F, 0.165F,
            PeTextures.T_IRON_BLOCK, lit, lit, lit);
         return;
      }

      // 7. 3D Held Torch — Wooden Post + Bright Glowing Flame Head
      if (heldId == PeWorld.B_TORCH) {
         addLocalBox3D(hx, hy, hz, rx, ry, rz, tUx, tUy, tUz, tFx, tFy, tFz,
            -0.022F, -0.11F, -0.022F, 0.022F, 0.09F, 0.022F,
            PeTextures.T_PLANKS_OAK, lit, lit, lit);
         addLocalBox3D(hx, hy, hz, rx, ry, rz, tUx, tUy, tUz, tFx, tFy, tFz,
            -0.028F, 0.09F, -0.028F, 0.028F, 0.155F, 0.028F,
            PeTextures.T_LAVA, 1.0F, 0.95F, 0.65F);
         return;
      }

      // 8. Non-Block Held Items (Food, Ingots, Gems, Ender Pearl): 3D Extruded Item Tablet
      if (heldId > PeWorld.MAX_BLOCK_ID) {
         int itemTile = PeWorld.getItemTile(heldId);
         addLocalBox3D(hx, hy, hz, rx, ry, rz, tUx, tUy, tUz, tFx, tFy, tFz,
            -0.095F, -0.095F, -0.022F, 0.095F, 0.095F, 0.022F,
            itemTile, lit, lit, lit);
         return;
      }

      // 9. True 6-Face 3D Held Block (using getFaceTile(b, 0..5) and Slab/Stair geometry!)
      byte b = (byte)heldId;
      // Rotate block slightly by 35 degrees in hand so Top, Front, and Side faces are all clearly visible
      float cR = 0.82F, sR = 0.57F;
      float bRx = rx * cR + fx * sR;
      float bRy = ry * cR + fy * sR;
      float bRz = rz * cR + fz * sR;
      float bFx = fx * cR - rx * sR;
      float bFy = fy * cR - ry * sR;
      float bFz = fz * cR - rz * sR;

      if (PeWorld.isSlab(b)) {
         addLocalBlockBox6Face(hx, hy, hz, bRx, bRy, bRz, ux, uy, uz, bFx, bFy, bFz,
            -0.105F, -0.09F, -0.105F, 0.105F, 0.015F, 0.105F, b, lit);
      } else if (PeWorld.isStair(b)) {
         addLocalBlockBox6Face(hx, hy, hz, bRx, bRy, bRz, ux, uy, uz, bFx, bFy, bFz,
            -0.105F, -0.09F, -0.105F, 0.105F, 0.015F, 0.105F, b, lit);
         addLocalBlockBox6Face(hx, hy, hz, bRx, bRy, bRz, ux, uy, uz, bFx, bFy, bFz,
            -0.105F, 0.015F, -0.105F, 0.105F, 0.12F, 0.0F, b, lit);
      } else {
         addLocalBlockBox6Face(hx, hy, hz, bRx, bRy, bRz, ux, uy, uz, bFx, bFy, bFz,
            -0.105F, -0.105F, -0.105F, 0.105F, 0.105F, 0.105F, b, lit);
      }
   }

   private void addLocalBox3D(float ox, float oy, float oz,
                              float rx, float ry, float rz,
                              float ux, float uy, float uz,
                              float fx, float fy, float fz,
                              float x0, float y0, float z0, float x1, float y1, float z1,
                              int tile, float cr, float cg, float cb) {
      int p = this.dyn3DVerts * FLOATS_PER_VERT_3D;
      if (p + 36 * FLOATS_PER_VERT_3D >= this.dyn3DData.length) return;
      float[] cx = new float[8];
      float[] cy = new float[8];
      float[] cz = new float[8];
      for (int c = 0; c < 8; c++) {
         float lx = ((c & 1) != 0) ? x1 : x0;
         float ly = ((c & 2) != 0) ? y1 : y0;
         float lz = ((c & 4) != 0) ? z1 : z0;
         cx[c] = ox + rx * lx + ux * ly + fx * lz;
         cy[c] = oy + ry * lx + uy * ly + fy * lz;
         cz[c] = oz + rz * lx + uz * ly + fz * lz;
      }
      float u0 = ((tile & 15) * 16.0F + 0.15F) / 256.0F;
      float v0 = ((tile >> 4) * 16.0F + 0.15F) / 256.0F;
      float u1 = ((tile & 15) * 16.0F + 15.85F) / 256.0F;
      float v1 = ((tile >> 4) * 16.0F + 15.85F) / 256.0F;
      // Top (+Y: 2, 6, 7, 3)
      p = emitDynamicQuad(this.dyn3DData, p, u0, v0, u1, v1, cr, cg, cb, 1.0F,
         cx[2], cy[2], cz[2], cx[6], cy[6], cz[6], cx[7], cy[7], cz[7], cx[3], cy[3], cz[3]);
      // Bottom (-Y: 4, 0, 1, 5)
      p = emitDynamicQuad(this.dyn3DData, p, u0, v0, u1, v1, cr * 0.6F, cg * 0.6F, cb * 0.6F, 1.0F,
         cx[4], cy[4], cz[4], cx[0], cy[0], cz[0], cx[1], cy[1], cz[1], cx[5], cy[5], cz[5]);
      // North (-Z: 3, 1, 0, 2)
      p = emitDynamicQuad(this.dyn3DData, p, u0, v0, u1, v1, cr * 0.85F, cg * 0.85F, cb * 0.85F, 1.0F,
         cx[3], cy[3], cz[3], cx[1], cy[1], cz[1], cx[0], cy[0], cz[0], cx[2], cy[2], cz[2]);
      // South (+Z: 6, 4, 5, 7)
      p = emitDynamicQuad(this.dyn3DData, p, u0, v0, u1, v1, cr * 0.90F, cg * 0.90F, cb * 0.90F, 1.0F,
         cx[6], cy[6], cz[6], cx[4], cy[4], cz[4], cx[5], cy[5], cz[5], cx[7], cy[7], cz[7]);
      // East (+X: 7, 5, 1, 3)
      p = emitDynamicQuad(this.dyn3DData, p, u0, v0, u1, v1, cr * 0.76F, cg * 0.76F, cb * 0.76F, 1.0F,
         cx[7], cy[7], cz[7], cx[5], cy[5], cz[5], cx[1], cy[1], cz[1], cx[3], cy[3], cz[3]);
      // West (-X: 2, 0, 4, 6)
      p = emitDynamicQuad(this.dyn3DData, p, u0, v0, u1, v1, cr * 0.72F, cg * 0.72F, cb * 0.72F, 1.0F,
         cx[2], cy[2], cz[2], cx[0], cy[0], cz[0], cx[4], cy[4], cz[4], cx[6], cy[6], cz[6]);
      this.dyn3DVerts = p / FLOATS_PER_VERT_3D;
   }

   private void addLocalBlockBox6Face(float ox, float oy, float oz,
                                      float rx, float ry, float rz,
                                      float ux, float uy, float uz,
                                      float fx, float fy, float fz,
                                      float x0, float y0, float z0, float x1, float y1, float z1,
                                      byte b, float shade) {
      int p = this.dyn3DVerts * FLOATS_PER_VERT_3D;
      if (p + 36 * FLOATS_PER_VERT_3D >= this.dyn3DData.length) return;
      float[] cx = new float[8];
      float[] cy = new float[8];
      float[] cz = new float[8];
      for (int c = 0; c < 8; c++) {
         float lx = ((c & 1) != 0) ? x1 : x0;
         float ly = ((c & 2) != 0) ? y1 : y0;
         float lz = ((c & 4) != 0) ? z1 : z0;
         cx[c] = ox + rx * lx + ux * ly + fx * lz;
         cy[c] = oy + ry * lx + uy * ly + fy * lz;
         cz[c] = oz + rz * lx + uz * ly + fz * lz;
      }
      p = emitOrientedBlockFaces(this.dyn3DData, p, cx, cy, cz, b, shade);
      this.dyn3DVerts = p / FLOATS_PER_VERT_3D;
   }

   private static int emitOrientedBlockFaces(float[] out, int p, float[] cx, float[] cy, float[] cz, byte b, float shade) {
      // Face 0: Top (+Y: 2, 6, 7, 3)
      p = emitFaceAO(out, p, PeWorld.getFaceTile(b, 0), 0.0F,
         cx[2], cy[2], cz[2], shade,
         cx[6], cy[6], cz[6], shade,
         cx[7], cy[7], cz[7], shade,
         cx[3], cy[3], cz[3], shade);
      // Face 1: Bottom (-Y: 4, 0, 1, 5)
      p = emitFaceAO(out, p, PeWorld.getFaceTile(b, 1), 0.0F,
         cx[4], cy[4], cz[4], shade * 0.56F,
         cx[0], cy[0], cz[0], shade * 0.56F,
         cx[1], cy[1], cz[1], shade * 0.56F,
         cx[5], cy[5], cz[5], shade * 0.56F);
      // Face 2: North (-Z: 3, 1, 0, 2)
      p = emitFaceAO(out, p, PeWorld.getFaceTile(b, 2), 0.0F,
         cx[3], cy[3], cz[3], shade * 0.85F,
         cx[1], cy[1], cz[1], shade * 0.80F,
         cx[0], cy[0], cz[0], shade * 0.80F,
         cx[2], cy[2], cz[2], shade * 0.85F);
      // Face 3: South (+Z: 6, 4, 5, 7)
      p = emitFaceAO(out, p, PeWorld.getFaceTile(b, 3), 0.0F,
         cx[6], cy[6], cz[6], shade * 0.90F,
         cx[4], cy[4], cz[4], shade * 0.84F,
         cx[5], cy[5], cz[5], shade * 0.84F,
         cx[7], cy[7], cz[7], shade * 0.90F);
      // Face 4: East (+X: 7, 5, 1, 3)
      p = emitFaceAO(out, p, PeWorld.getFaceTile(b, 4), 0.0F,
         cx[7], cy[7], cz[7], shade * 0.76F,
         cx[5], cy[5], cz[5], shade * 0.70F,
         cx[1], cy[1], cz[1], shade * 0.70F,
         cx[3], cy[3], cz[3], shade * 0.76F);
      // Face 5: West (-X: 2, 0, 4, 6)
      p = emitFaceAO(out, p, PeWorld.getFaceTile(b, 5), 0.0F,
         cx[2], cy[2], cz[2], shade * 0.72F,
         cx[0], cy[0], cz[0], shade * 0.66F,
         cx[4], cy[4], cz[4], shade * 0.66F,
         cx[6], cy[6], cz[6], shade * 0.72F);
      return p;
   }

   private static int emitDynamicQuad(float[] out, int p,
                                      float u0, float v0, float u1, float v1,
                                      float r, float g, float b, float a,
                                      float x0, float y0, float z0,
                                      float x1, float y1, float z1,
                                      float x2, float y2, float z2,
                                      float x3, float y3, float z3) {
      p = putV3D(out, p, x0, y0, z0, u0, v0, r, g, b, a);
      p = putV3D(out, p, x1, y1, z1, u0, v1, r, g, b, a);
      p = putV3D(out, p, x2, y2, z2, u1, v1, r, g, b, a);
      p = putV3D(out, p, x0, y0, z0, u0, v0, r, g, b, a);
      p = putV3D(out, p, x2, y2, z2, u1, v1, r, g, b, a);
      p = putV3D(out, p, x3, y3, z3, u1, v0, r, g, b, a);
      return p;
   }

   public void flushDynamic3D(PeShaders sh) {
      if (this.dyn3DVerts <= 0) return;
      this.dyn3DBuf.clear();
      this.dyn3DBuf.put(this.dyn3DData, 0, this.dyn3DVerts * FLOATS_PER_VERT_3D);
      this.dyn3DBuf.position(0);

      GLES20.glUseProgram(sh.prog3D);
      GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, this.dyn3DVbo);
      GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, this.dyn3DVerts * STRIDE_3D, this.dyn3DBuf, GLES20.GL_DYNAMIC_DRAW);
      GLES20.glEnableVertexAttribArray(sh.aPos3D);
      GLES20.glEnableVertexAttribArray(sh.aUV3D);
      GLES20.glEnableVertexAttribArray(sh.aColor3D);
      GLES20.glVertexAttribPointer(sh.aPos3D, 3, GLES20.GL_FLOAT, false, STRIDE_3D, 0);
      GLES20.glVertexAttribPointer(sh.aUV3D, 2, GLES20.GL_FLOAT, false, STRIDE_3D, 3 * 4);
      GLES20.glVertexAttribPointer(sh.aColor3D, 4, GLES20.GL_FLOAT, false, STRIDE_3D, 5 * 4);
      GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, this.dyn3DVerts);
      GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
      this.dyn3DVerts = 0;
   }

   // ====================================================================
   //  2D UI & Bitmap Font Batcher (Cards, Bevel Buttons, Slots, Circle Pads)
   // ====================================================================
   public void begin2D() {
      this.dyn2DVerts = 0;
   }

   public void addRect2D(float x, float y, float w, float h, int argb) {
      addTexturedQuad2D(x, y, w, h, 0.0F, 0.0F, 1.0F, 1.0F, argb);
   }

   public void drawCard2D(float x, float y, float w, float h, int bgArgb, int borderArgb) {
      drawCard2D(x, y, w, h, bgArgb, borderArgb, 0, 0);
   }

   public void drawCard2D(float x, float y, float w, float h, int bgArgb, int borderArgb, int headerH, int headerArgb) {
      addRect2D(x + 4.0F, y + 6.0F, w, h, 0x78000000);
      addRect2D(x - 2.0F, y - 2.0F, w + 4.0F, h + 4.0F, borderArgb);
      addRect2D(x, y, w, h, bgArgb);
      if (headerH > 0) {
         addRect2D(x, y, w, headerH, headerArgb);
         addRect2D(x, y + headerH - 2.0F, w, 2.0F, borderArgb);
      }
      addRect2D(x + 2.0F, y + 2.0F, w - 4.0F, 2.0F, 0x38FFFFFF);
   }

   public void drawButton2D(float x, float y, float w, float h, int fillArgb, int rimArgb, boolean activeGlow) {
      addRect2D(x + 2.0F, y + 4.0F, w, h, 0x66000000);
      addRect2D(x - 2.0F, y - 2.0F, w + 4.0F, h + 4.0F, activeGlow ? 0xFFF5D86C : rimArgb);
      addRect2D(x, y, w, h, fillArgb);
      addRect2D(x + 2.0F, y + 2.0F, w - 4.0F, Math.max(3.0F, h * 0.16F), 0x38FFFFFF);
      addRect2D(x + 2.0F, y + h - Math.max(3.0F, h * 0.14F) - 1.0F, w - 4.0F, Math.max(3.0F, h * 0.14F), 0x48000000);
   }

   public void drawSlot2D(float x, float y, float s, boolean selected, int specialBg) {
      int outerCol = selected ? 0xFFFFE268 : 0xEB2A3348;
      int fillCol = specialBg != 0 ? specialBg : (selected ? 0xF23C4A68 : 0xDC131822);
      float pad = selected ? 0.0F : 2.0F;
      addRect2D(x + pad - 2.0F, y + pad - 2.0F, s - pad * 2.0F + 4.0F, s - pad * 2.0F + 4.0F, outerCol);
      addRect2D(x + pad, y + pad, s - pad * 2.0F, s - pad * 2.0F, fillCol);
      addRect2D(x + pad, y + pad, s - pad * 2.0F, 2.5F, selected ? 0x60FFFFFF : 0x68080A0E);
      addRect2D(x + pad, y + s - pad - 2.5F, s - pad * 2.0F, 2.5F, 0x38FFFFFF);
   }

   public void drawCirclePad2D(float cx, float cy, float r, int ringArgb, int fillArgb) {
      float r85 = r * 0.86F;
      float r68 = r * 0.68F;
      addRect2D(cx - r68, cy - r, r68 * 2.0F, r * 2.0F, ringArgb);
      addRect2D(cx - r, cy - r68, r * 2.0F, r68 * 2.0F, ringArgb);
      addRect2D(cx - r85, cy - r85, r85 * 2.0F, r85 * 2.0F, ringArgb);
      float ir = r - 4.0F;
      float ir85 = ir * 0.86F;
      float ir68 = ir * 0.68F;
      addRect2D(cx - ir68, cy - ir, ir68 * 2.0F, ir * 2.0F, fillArgb);
      addRect2D(cx - ir, cy - ir68, ir * 2.0F, ir68 * 2.0F, fillArgb);
      addRect2D(cx - ir85, cy - ir85, ir85 * 2.0F, ir85 * 2.0F, fillArgb);
      addRect2D(cx - ir68 * 0.8F, cy - ir + 2.0F, ir68 * 1.6F, 3.5F, 0x48FFFFFF);
   }

   public void addTile2D(float x, float y, float w, float h, int tile, int argb) {
      float u0 = ((tile & 15) * 16.0F + 0.15F) / 256.0F;
      float v0 = ((tile >> 4) * 16.0F + 0.15F) / 256.0F;
      float u1 = ((tile & 15) * 16.0F + 15.85F) / 256.0F;
      float v1 = ((tile >> 4) * 16.0F + 15.85F) / 256.0F;
      addTexturedQuad2D(x, y, w, h, u0, v0, u1, v1, argb);
   }

   /**
    * Renders an Item or Block inside an Inventory or Hotbar Slot:
    * - Tools, Weapons, Food, Ores/Ingots, Cross-Plants, Doors & Ladders render as crisp 2D pixel-art sprites.
    * - 3D Blocks, Slabs & Stairs render as 3-face Isometric 3D Block Previews (Top + Left + Right faces with shading)!
    */
   public void addItemOrBlockIcon2D(float x, float y, float s, int id) {
      if (id <= 0) return;
      if (id > PeWorld.MAX_BLOCK_ID
         || PeWorld.isCrossPlant((byte)id)
         || id == PeWorld.B_DOOR_OAK || id == PeWorld.B_DOOR_OAK_OPEN
         || id == PeWorld.B_LADDER || id == PeWorld.B_BED_RED) {
         addTile2D(x, y, s, s, PeWorld.getItemTile(id), 0xFFFFFFFF);
         return;
      }

      byte b = (byte)id;
      int topTile = PeWorld.getFaceTile(b, 0);
      int leftTile = PeWorld.getFaceTile(b, 3);
      int rightTile = PeWorld.getFaceTile(b, 4);

      float cx = x + s * 0.50F;
      float rx = s * 0.44F;
      float ry = s * 0.22F;
      float hFull = s * 0.48F;
      float hUsed = PeWorld.isSlab(b) ? (hFull * 0.52F) : hFull;
      float topY = y + s * 0.28F + (hFull - hUsed) * 0.65F;

      // Top diamond face: N=(cx, topY - ry), W=(cx - rx, topY), S=(cx, topY + ry), E=(cx + rx, topY)
      addTileFreeQuad2D(topTile, 1.0F,
         cx,      topY - ry,
         cx - rx, topY,
         cx,      topY + ry,
         cx + rx, topY);
      // Left-Front face (South): TL=(cx - rx, topY), BL=(cx - rx, topY + hUsed), BR=(cx, topY + ry + hUsed), TR=(cx, topY + ry)
      addTileFreeQuad2D(leftTile, 0.86F,
         cx - rx, topY,
         cx - rx, topY + hUsed,
         cx,      topY + ry + hUsed,
         cx,      topY + ry);
      // Right-Side face (East): TL=(cx, topY + ry), BL=(cx, topY + ry + hUsed), BR=(cx + rx, topY + hUsed), TR=(cx + rx, topY)
      addTileFreeQuad2D(rightTile, 0.68F,
         cx,      topY + ry,
         cx,      topY + ry + hUsed,
         cx + rx, topY + hUsed,
         cx + rx, topY);
   }

   private void addTileFreeQuad2D(int tile, float shade,
                                  float x0, float y0, float x1, float y1,
                                  float x2, float y2, float x3, float y3) {
      int p = this.dyn2DVerts * FLOATS_PER_VERT_2D;
      if (p + 6 * FLOATS_PER_VERT_2D >= this.dyn2DData.length) return;
      float u0 = ((tile & 15) * 16.0F + 0.15F) / 256.0F;
      float v0 = ((tile >> 4) * 16.0F + 0.15F) / 256.0F;
      float u1 = ((tile & 15) * 16.0F + 15.85F) / 256.0F;
      float v1 = ((tile >> 4) * 16.0F + 15.85F) / 256.0F;
      p = putV2D(this.dyn2DData, p, x0, y0, u0, v0, shade, shade, shade, 1.0F);
      p = putV2D(this.dyn2DData, p, x1, y1, u0, v1, shade, shade, shade, 1.0F);
      p = putV2D(this.dyn2DData, p, x2, y2, u1, v1, shade, shade, shade, 1.0F);
      p = putV2D(this.dyn2DData, p, x0, y0, u0, v0, shade, shade, shade, 1.0F);
      p = putV2D(this.dyn2DData, p, x2, y2, u1, v1, shade, shade, shade, 1.0F);
      p = putV2D(this.dyn2DData, p, x3, y3, u1, v0, shade, shade, shade, 1.0F);
      this.dyn2DVerts = p / FLOATS_PER_VERT_2D;
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
