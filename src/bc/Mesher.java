package bc;

import java.nio.FloatBuffer;
import java.util.Arrays;
import org.lwjgl.system.MemoryUtil;

public final class Mesher {
   public static final int FLOATS = 11;
   static final ThreadLocal<Mesher.Scratch> TL = ThreadLocal.withInitial(Mesher.Scratch::new);
   static final int[][] FN = new int[][]{{0, 1, 0}, {0, -1, 0}, {0, 0, -1}, {0, 0, 1}, {1, 0, 0}, {-1, 0, 0}};
   static final int[][][] FC = new int[][][]{
      {{0, 1, 0}, {0, 1, 1}, {1, 1, 1}, {1, 1, 0}},
      {{0, 0, 0}, {1, 0, 0}, {1, 0, 1}, {0, 0, 1}},
      {{1, 0, 0}, {0, 0, 0}, {0, 1, 0}, {1, 1, 0}},
      {{0, 0, 1}, {1, 0, 1}, {1, 1, 1}, {0, 1, 1}},
      {{1, 0, 1}, {1, 0, 0}, {1, 1, 0}, {1, 1, 1}},
      {{0, 0, 0}, {0, 0, 1}, {0, 1, 1}, {0, 1, 0}}
   };
   static final int[][] PLANE = new int[][]{{0, 2}, {0, 2}, {0, 1}, {0, 1}, {1, 2}, {1, 2}};
   static final float[] SHADE = new float[]{1.0F, 0.5F, 0.8F, 0.8F, 0.6F, 0.6F};
   static final float[] AOF = new float[]{1.0F, 0.8F, 0.62F, 0.46F};
   static final float E = 2.0E-4F;
   static final ThreadLocal<float[]> UVT = ThreadLocal.withInitial(() -> new float[2]);

   static int ci(int var0, int var1, int var2) {
      return (var1 * 18 + var2) * 18 + var0;
   }

   /**
    * Update 3.9: faces between two leaf blocks. 0 = fancy (every face, like 3.5), 1 = smart (default: no face between two leaves that are both
    * enclosed by leaves / solid blocks - nobody can see it, even through the holes in the leaf texture), 2 = fast (no leaf-to-leaf faces
    * at all). In a forest around 30 % of everything the mesher emitted used to be leaf-to-leaf faces, about 12 % of them between enclosed leaves.
    * Set from the options (and -Dbc.leaves=0|1|2 for tests).
    */
   public static volatile int leafMode = Integer.getInteger("bc.leaves", 1);

   /**
    * Update 3.19: 3D crossed diagonal foliage fronds protruding from exposed canopy leaf blocks. Defaults to false for headless unit tests that
    * count exact cube faces; {@link Game} and {@link Screens} sync it with {@link Options#bushyLeaves} (which defaults to true).
    */
   public static volatile boolean bushyLeaves = Boolean.getBoolean("bc.bushy");

   /**
    * Update 3.11: faces are drawn from their front side only (GL back-face culling, like Minecraft), so everything that has to be seen from
    * both sides - plants, crops, flames, the top of a water or lava surface - gets a second quad that faces the other way, and the test that
    * decides whether the face of a box lies against a neighbouring block now looks at the right side for the z faces (before, a thin door
    * lost its visible big face when an opaque block stood against that side). {@code -Dbc.legacymesh=true} restores the meshes of Update 3.5
    * exactly (the strict equivalence pass compares them with the original jar).
    */
   public static volatile boolean legacy = Boolean.getBoolean("bc.legacymesh");

   /** True if every face neighbour of the block at padded position (x, y, z) is solid or a leaf (so none of its faces can be seen from outside). */
   static boolean enclosed(short[] blocks, int x, int y, int z) {
      if (x < 1 || x > 16 || y < 1 || y > 16 || z < 1 || z > 16) {
         return false; // its neighbours are outside the data we have: assume it is exposed
      }

      for (int f = 0; f < 6; f++) {
         int id = blocks[ci(x + FN[f][0], y + FN[f][1], z + FN[f][2])] & 0xFFFF;
         if (!Blocks.opaque[id] && !Blocks.isLeaves(id)) {
            return false;
         }
      }

      return true;
   }

   /** Whether the face of leaf block (x, y, z) towards face neighbour f is hidden because the neighbour is a leaf too (see {@link #leafMode}). */
   static boolean hiddenLeafFace(short[] blocks, int x, int y, int z, int f, int neighbour) {
      int mode = leafMode;
      if (mode == 0 || !Blocks.isLeaves(neighbour)) {
         return false;
      }

      return mode == 2 || enclosed(blocks, x, y, z) && enclosed(blocks, x + FN[f][0], y + FN[f][1], z + FN[f][2]);
   }

   public static Mesher.Result build(World var0, Chunk var1, int var2, boolean var3) {
      short[] cblocks = var1.blocks;
      int secStart = var2 << 12;
      int secEnd = secStart + 4096;
      boolean anyBlock = false;
      boolean allOpaque = !legacy && var2 < 4;
      for (int i = secStart; i < secEnd; i++) {
         int id = cblocks[i] & 0xFFFF;
         if (id != 0) {
            anyBlock = true;
            if (allOpaque && !Blocks.opaque[id]) {
               allOpaque = false;
            }
         } else {
            allOpaque = false;
         }
         if (anyBlock && !allOpaque) {
            break;
         }
      }

      if (!anyBlock) {
         Mesher.Result empty = new Mesher.Result();
         empty.conn = CaveCull.ALL;
         return empty;
      }

      if (allOpaque && var0 != null && var1.isSectionFullyEnclosedOpaque(var0, var2)) {
         Mesher.Result enclosed = new Mesher.Result();
         enclosed.conn = 0L;
         return enclosed;
      }

      Mesher.Scratch var4 = (Mesher.Scratch)TL.get();
      var4.o.clear();
      var4.t.clear();
      int var5 = var1.cx * 16;
      int var6 = var2 * 16;
      int var7 = var1.cz * 16;
      var4.ox = var5;
      var4.oy = var6;
      var4.oz = var7;
      var4.rot = 0;
      Chunk[] var8 = var4.chunks9;

      for (int var9 = -1; var9 <= 1; var9++) {
         for (int var10 = -1; var10 <= 1; var10++) {
            var8[(var9 + 1) * 3 + var10 + 1] = var0.chunk(var1.cx + var10, var1.cz + var9);
         }
      }

      short[] var22 = var4.B;
      byte[] var23 = var4.L;

      for (int var11 = 0; var11 < 18; var11++) {
         int var12 = var6 - 1 + var11;

         for (int var13 = 0; var13 < 18; var13++) {
            int var14 = var7 - 1 + var13;
            int var15 = (var14 >> 4) - var1.cz + 1;
            int var16 = var14 & 15;
            int rowBase = ci(0, var11, var13);
            if (var12 < 0) {
               Arrays.fill(var22, rowBase, rowBase + 18, (short)6);
               Arrays.fill(var23, rowBase, rowBase + 18, (byte)0);
            } else if (var12 > 255) {
               Arrays.fill(var22, rowBase, rowBase + 18, (short)0);
               Arrays.fill(var23, rowBase, rowBase + 18, (byte)-16);
            } else {
               // Left border (var17 = 0, local x = 15 of cx - 1)
               Chunk cLeft = var8[var15 * 3];
               if (cLeft == null) {
                  var22[rowBase] = 6;
                  var23[rowBase] = 0;
               } else {
                  int idxL = Chunk.idx(15, var12, var16);
                  var22[rowBase] = cLeft.blocks[idxL];
                  var23[rowBase] = cLeft.light[idxL];
               }
               // Interior 16 columns (var17 = 1..16, local x = 0..15 of cx)
               Chunk cMid = var8[var15 * 3 + 1];
               if (cMid == null) {
                  Arrays.fill(var22, rowBase + 1, rowBase + 17, (short)6);
                  Arrays.fill(var23, rowBase + 1, rowBase + 17, (byte)0);
               } else {
                  int idxM = Chunk.idx(0, var12, var16);
                  System.arraycopy(cMid.blocks, idxM, var22, rowBase + 1, 16);
                  System.arraycopy(cMid.light, idxM, var23, rowBase + 1, 16);
               }
               // Right border (var17 = 17, local x = 0 of cx + 1)
               Chunk cRight = var8[var15 * 3 + 2];
               if (cRight == null) {
                  var22[rowBase + 17] = 6;
                  var23[rowBase + 17] = 0;
               } else {
                  int idxR = Chunk.idx(0, var12, var16);
                  var22[rowBase + 17] = cRight.blocks[idxR];
                  var23[rowBase + 17] = cRight.light[idxR];
               }
            }
         }
      }
      Arrays.fill(var8, null);

      boolean var24 = false;

      for (int var26 = 0; var26 < 16; var26++) {
         for (int var28 = 0; var28 < 16; var28++) {
            for (int var29 = 0; var29 < 16; var29++) {
               int var30 = var22[ci(var29 + 1, var26 + 1, var28 + 1)] & 0xFFFF;
               if (var30 != 0) {
                  var24 = true;
                  switch (Blocks.kind[var30]) {
                     case 0:
                        cube(var4, var1, var29, var26, var28, var30, var3);
                        break;
                     case 1:
                        if (var30 == Blocks.FIRE) {
                           fire(var4, var1, var29, var26, var28);
                        } else {
                           cross(var4, var1, var29, var26, var28, var30);
                        }
                        break;
                     case 2:
                        box(var4, var1, var29, var26, var28, var30);
                        break;
                     case 3:
                        fluid(var4, var1, var29, var26, var28, var30);
                        break;
                     case 4:
                        stair(var4, var1, var29, var26, var28, var30);
                        break;
                     case 5:
                        wallTorch(var4, var1, var29, var26, var28, var30);
                        break;
                     case 6:
                        fence(var4, var1, var29, var26, var28, var30);
                        break;
                     case 7:
                        crop(var4, var1, var29, var26, var28, var30);
                        break;
                     case 8:
                        portal(var4, var29, var26, var28, var30);
                        break;
                     case 9:
                        vine(var4, var1, var29, var26, var28, var30);
                        break;
                     case Shapes.K_SHAPE:
                        Shapes.mesh(var4, var29, var26, var28, var30);
                  }
               }
            }
         }
      }

      Mesher.Result var27 = new Mesher.Result();
      var27.conn = CaveCull.compute(var22);
      var27.nO = var4.o.n / 11;
      var27.nT = var4.t.n / 11;
      if (var27.nO > 0) {
         var27.opaque = MemoryUtil.memAllocFloat(var4.o.n);
         var27.opaque.put(var4.o.a, 0, var4.o.n).flip();
      }

      if (var27.nT > 0) {
         var27.trans = MemoryUtil.memAllocFloat(var4.t.n);
         var27.trans.put(var4.t.a, 0, var4.t.n).flip();
      }

      return var27;
   }

   static float lu(float var0) {
      return (var0 + 0.5F) / 16.0F;
   }

   static int tintColor(Chunk var0, int var1, int var2, int var3) {
      if (ModLoader.isInfdevActive() && (Blocks.tint[var1] == 1 || Blocks.tint[var1] == 2)) {
         return InfdevGen.INFDEV_GREEN;
      }
      switch (Blocks.tint[var1]) {
         case 1:
            return var0.grassColor[var3 * 16 + var2];
         case 2:
            if (var1 == 14) {
               return 6396257;
            } else {
               if (var1 == 13) {
                  return 8431445;
               }

               return var0.foliageColor[var3 * 16 + var2];
            }
         default:
            return 16777215;
      }
   }

   /** How far a quad's texture coordinates stay inside their tile (half a hundredth of a pixel of the atlas: 0.05 px, as it was with the 256 px atlas). */
   static final float INSET = 0.05F / Tex.PX;

   static void emit(Mesher.Scratch var0, Mesher.FB var1, int var2, boolean var3) {
      float var4 = Tex.tu(var2);
      float var5 = Tex.tv(var2);
      float var6 = var4 + INSET;
      float var7 = var4 + Tex.TS - INSET;
      float var8 = var5 + INSET;
      float var9 = var5 + Tex.TS - INSET;

      for (int var10 = 0; var10 < 4; var10++) {
         int var11 = var3 ? var10 + 1 & 3 : var10;
         int var12 = var0.rot == 0 ? var11 : var11 + var0.rot & 3;
         var1.v(
            var0.qx[var11],
            var0.qy[var11],
            var0.qz[var11],
            var6 + (var7 - var6) * var0.qu[var12],
            var8 + (var9 - var8) * var0.qv[var12],
            var0.qr[var11],
            var0.qg[var11],
            var0.qb[var11],
            var0.qa[var11],
            var0.qm[var11],
            var0.ql[var11]
         );
      }
   }

   /** The quad that is in the scratch arrays, once more with the corners in the opposite order: it faces the other way (back-face culling). */
   static void emitBack(Mesher.Scratch var0, Mesher.FB var1, int var2) {
      if (legacy) {
         return;
      }

      float var4 = Tex.tu(var2);
      float var5 = Tex.tv(var2);
      float var6 = var4 + INSET;
      float var7 = var4 + Tex.TS - INSET;
      float var8 = var5 + INSET;
      float var9 = var5 + Tex.TS - INSET;

      for (int var10 = 0; var10 < 4; var10++) {
         int var11 = -var10 & 3;
         var1.v(
            var0.qx[var11],
            var0.qy[var11],
            var0.qz[var11],
            var6 + (var7 - var6) * var0.qu[var11],
            var8 + (var9 - var8) * var0.qv[var11],
            var0.qr[var11],
            var0.qg[var11],
            var0.qb[var11],
            var0.qa[var11],
            var0.qm[var11],
            var0.ql[var11]
         );
      }
   }

   static void uvFor(int var0, float var1, float var2, float var3, float[] var4) {
      switch (var0) {
         case 0:
         case 1:
            var4[0] = var1;
            var4[1] = var3;
            break;
         case 2:
            var4[0] = 1.0F - var1;
            var4[1] = 1.0F - var2;
            break;
         case 3:
            var4[0] = var1;
            var4[1] = 1.0F - var2;
            break;
         case 4:
            var4[0] = 1.0F - var3;
            var4[1] = 1.0F - var2;
            break;
         default:
            var4[0] = var3;
            var4[1] = 1.0F - var2;
      }
   }

   static void cube(Mesher.Scratch var0, Chunk var1, int var2, int var3, int var4, int var5, boolean var6) {
      short[] var7 = var0.B;
      byte[] var8 = var0.L;
      int var9 = var2 + 1;
      int var10 = var3 + 1;
      int var11 = var4 + 1;
      int var12 = ci(var9, var10, var11);
      // Fast-path skip for completely enclosed underground/interior cubes (no exposed faces and not a slime block)
      if (var5 != Redstone319.SLIME_BLOCK
         && Blocks.opaque[var7[var12 + 324] & 0xFFFF]
         && Blocks.opaque[var7[var12 - 324] & 0xFFFF]
         && Blocks.opaque[var7[var12 - 18] & 0xFFFF]
         && Blocks.opaque[var7[var12 + 18] & 0xFFFF]
         && Blocks.opaque[var7[var12 + 1] & 0xFFFF]
         && Blocks.opaque[var7[var12 - 1] & 0xFFFF]) {
         return;
      }
      int var13 = tintColor(var1, var5, var2, var4);
      float[] var14 = var0.uvt;

      for (int var15 = 0; var15 < 6; var15++) {
         int[] var16 = FN[var15];
         int var17 = ci(var9 + var16[0], var10 + var16[1], var11 + var16[2]);
         int var18 = var7[var17] & 0xFFFF;
         if (!Blocks.opaque[var18] && (var5 != 15 || var18 != 15) && !(Blocks.isLeaves(var5) && hiddenLeafFace(var7, var9, var10, var11, var15, var18))) {
            if (Color312.isStainedGlass(var5) || var5 == Blocks313.ICE) {
               // Update 3.12: stained glass is translucent (blended pass); the faces between two panes of the same kind are not drawn
               // (Update 3.13: ice is the same, a little less see-through)
               if (var18 != var5) {
                  int tile = Blocks.tiles[var5][var15];
                  quad(var0, var15, var2, var3, var4, var17, var12, tile, 1.0F, 1.0F, 1.0F, var5 == Blocks313.ICE ? 0.84F : Color312.GLASS_ALPHA, var6, 0.0F, var14);
                  emit(var0, var0.t, tile, flipFor(var0));
               }

               continue;
            }

            if (!legacy && var5 == Redstone319.SLIME_BLOCK) {
               if (var18 != var5) {
                  int tile = Blocks.tiles[var5][var15];
                  quad(var0, var15, var2, var3, var4, var17, var12, tile, 1.0F, 1.0F, 1.0F, 0.78F, var6, 0.0F, var14);
                  emit(var0, var0.t, tile, flipFor(var0));
               }

               continue;
            }

            boolean var19 = Blocks.tint[var5] == 1;
            float var20 = 1.0F;
            float var21 = 1.0F;
            float var22 = 1.0F;
            int var23 = Blocks.tiles[var5][var15];
            if (Blocks.tint[var5] == 2 || var19 && var15 == 0) {
               var20 = (var13 >> 16 & 0xFF) / 255.0F;
               var21 = (var13 >> 8 & 0xFF) / 255.0F;
               var22 = (var13 & 0xFF) / 255.0F;
            }

            // Update 3.19: every block is a little lighter or darker than its neighbours, and the top of ground blocks is turned by a random quarter turn
            float jit = 1.0F;
            if (variation && !legacy) {
               int hh = hash3(var0.ox + var2, var0.oy + var3, var0.oz + var4);
               jit = 1.0F + ((hh & 0xFFFF) / 32767.5F - 1.0F) * ampOf(var23);
               var0.rot = turnsOf(var23, var15, hh);
            }

            if (!legacy && var5 >= 1900 && var5 <= 2008) {
               var0.rot = Redstone319.faceRot(var5, var15);
            }

            quad(var0, var15, var2, var3, var4, var17, var12, var23, var20 * jit, var21 * jit, var22 * jit, 1.0F, var6, 0.0F, var14);
            if (!legacy && Blocks.emit[var5] > 0) {
               float selfBl = lu(Blocks.emit[var5]);
               for (int k = 0; k < 4; k++) {
                  if (var0.ql[k] < selfBl) {
                     var0.ql[k] = selfBl;
                  }
               }
            }

            emit(var0, var0.o, var23, flipFor(var0));
            var0.rot = 0;
            if (var19 && var15 >= 2) {
               float var24 = (var13 >> 16 & 0xFF) / 255.0F;
               float var25 = (var13 >> 8 & 0xFF) / 255.0F;
               float var26 = (var13 & 0xFF) / 255.0F;
               quad(var0, var15, var2, var3, var4, var17, var12, 2, var24 * jit, var25 * jit, var26 * jit, 1.0F, var6, 0.002F, var14);
               emit(var0, var0.o, 2, flipFor(var0));
            }
         }
      }

      if (!legacy && var5 == Redstone319.SLIME_BLOCK) {
         Shapes.emitBox(var0, var2, var3, var4, var5, new double[]{0.1875, 0.1875, 0.1875, 0.8125, 0.8125, 0.8125, 0.0}, 0, var0.o, 1.0F);
      }

      if (bushyLeaves && !legacy && Blocks.isLeaves(var5) && !enclosed(var7, var9, var10, var11)) {
         bushyFronds(var0, var2, var3, var4, var9, var10, var11, var13);
      }
   }

   static void bushyFronds(Mesher.Scratch s, int x, int y, int z, int px, int py, int pz, int tint) {
      int maxSky = 0;
      int maxBlk = 0;
      int[] fSky = s.fSky;
      int[] fBlk = s.fBlk;
      boolean[] fOpen = s.fOpen;
      for (int f = 0; f < 6; f++) {
         fSky[f] = 0;
         fBlk[f] = 0;
         fOpen[f] = false;
         int nb = ci(px + FN[f][0], py + FN[f][1], pz + FN[f][2]);
         int id = s.B[nb] & 0xFFFF;
         if (!Blocks.opaque[id]) {
            int lv = s.L[nb] & 255;
            int sl = lv >> 4 & 15;
            int bl = lv & 15;
            fOpen[f] = true;
            fSky[f] = sl;
            fBlk[f] = bl;
            if (sl > maxSky) {
               maxSky = sl;
            }

            if (bl > maxBlk) {
               maxBlk = bl;
            }
         }
      }

      int hh = hash3(s.ox + x, s.oy + y, s.oz + z);
      float jit = variation ? 1.0F + ((hh & 0xFFFF) / 32767.5F - 1.0F) * AMP[TexPaint319.BUSHY_FROND] : 1.0F;
      float cr = (tint >> 16 & 0xFF) / 255.0F * jit;
      float cg = (tint >> 8 & 0xFF) / 255.0F * jit;
      float cb = (tint & 0xFF) / 255.0F * jit;
      float cx = x + 0.5F + (((hh >>> 4) & 15) / 7.5F - 1.0F) * 0.08F;
      float cy = y + 0.5F + (((hh >>> 8) & 15) / 7.5F - 1.0F) * 0.07F;
      float cz = z + 0.5F + (((hh >>> 12) & 15) / 7.5F - 1.0F) * 0.08F;
      float ang = (((hh >>> 16) & 31) / 15.5F - 1.0F) * 0.34F;
      float r = 1.06F;
      float hy = 0.88F;
      s.rot = 0;
      s.ao[0] = s.ao[1] = s.ao[2] = s.ao[3] = 1.0F;
      for (int p = 0; p < 3; p++) {
         if (p < 2) {
            double a = (p == 0 ? 0.7853982 : 2.3561945) + ang;
            float rx = (float)Math.cos(a) * r;
            float rz = (float)Math.sin(a) * r;
            float tilt = (p == 0 ? 1.0F : -1.0F) * (((hh >>> 21) & 7) - 3.5F) * 0.045F;
            s.qx[0] = cx - rx - tilt;
            s.qy[0] = cy - hy;
            s.qz[0] = cz - rz + tilt;
            s.qx[1] = cx + rx - tilt;
            s.qy[1] = cy - hy;
            s.qz[1] = cz + rz + tilt;
            s.qx[2] = cx + rx + tilt;
            s.qy[2] = cy + hy;
            s.qz[2] = cz + rz - tilt;
            s.qx[3] = cx - rx + tilt;
            s.qy[3] = cy + hy;
            s.qz[3] = cz - rz - tilt;
         } else {
            // 3rd tilted canopy crown plane pitching across the leaf block for lush 3D volume from above and below
            double a = 1.5707963 + ang * 0.8;
            float rx = (float)Math.cos(a) * 1.02F;
            float rz = (float)Math.sin(a) * 1.02F;
            float px2 = -rz * 0.62F;
            float pz2 = rx * 0.62F;
            float py2 = 0.68F;
            s.qx[0] = cx - rx - px2;
            s.qy[0] = cy - py2;
            s.qz[0] = cz - rz - pz2;
            s.qx[1] = cx + rx - px2;
            s.qy[1] = cy - py2;
            s.qz[1] = cz + rz - pz2;
            s.qx[2] = cx + rx + px2;
            s.qy[2] = cy + py2;
            s.qz[2] = cz + rz + pz2;
            s.qx[3] = cx - rx + px2;
            s.qy[3] = cy + py2;
            s.qz[3] = cz - rz + pz2;
         }

         s.qu[0] = 0.0F;
         s.qv[0] = 1.0F;
         s.qu[1] = 1.0F;
         s.qv[1] = 1.0F;
         s.qu[2] = 1.0F;
         s.qv[2] = 0.0F;
         s.qu[3] = 0.0F;
         s.qv[3] = 0.0F;
         for (int k = 0; k < 4; k++) {
            int fy = s.qy[k] >= y + 0.5F ? 0 : 1;
            int fz = s.qz[k] >= z + 0.5F ? 3 : 2;
            int fx = s.qx[k] >= x + 0.5F ? 4 : 5;
            float sumSky = maxSky;
            float sumBlk = maxBlk;
            int cnt = 1;
            int occ = 0;
            int[] faces = {fy, fz, fx};
            for (int f : faces) {
               if (fOpen[f]) {
                  sumSky += fSky[f];
                  sumBlk += fBlk[f];
                  cnt++;
               } else {
                  occ++;
               }
            }

            float vSky = Math.max(maxSky * 0.85F, sumSky / cnt);
            float vBlk = Math.max(maxBlk * 0.85F, sumBlk / cnt);
            float ao = 1.0F - occ * 0.06F;
            float sh = (k < 2 ? 0.78F : 0.96F) * ao;
            s.qr[k] = cr * sh;
            s.qg[k] = cg * sh;
            s.qb[k] = cb * sh;
            s.qa[k] = 1.0F;
            // qm is sky light (vLm.x), ql is block light (vLm.y)
            s.qm[k] = lu(vSky);
            s.ql[k] = lu(vBlk);
         }

         emit(s, s.o, TexPaint319.BUSHY_FROND, false);
         emitBack(s, s.o, TexPaint319.BUSHY_FROND);
      }
   }

   /**
    * Update 3.19: variation between blocks. Brightness: every block of the natural kinds (grass, dirt, sand, gravel, stone, leaves, snow ...) gets
    * its own brightness, within a few percent (leaves: a little more), by its position - so a meadow no longer looks like one stamp repeated; the
    * top of ground blocks (and every face of leaves) is also turned by a random quarter turn. It only changes colours and texture coordinates:
    * the shapes, the light and the number of faces are the same. {@code -Dbc.novariation=true} switches it off (so does {@link #legacy}).
    */
   public static volatile boolean variation = !Boolean.getBoolean("bc.novariation");
   static final float[] AMP = new float[1024];

   static {
      AMP[Tex.GRASS_TOP] = 0.075F;
      AMP[Tex.DIRT] = 0.06F;
      AMP[Tex.STONE] = 0.05F;
      AMP[Tex.COBBLE] = 0.04F;
      AMP[Tex.SAND] = 0.045F;
      AMP[Tex.GRAVEL] = 0.065F;
      AMP[Tex.LEAVES] = 0.12F;
      AMP[Tex.SNOW] = 0.025F;
      AMP[Tex.SANDSTONE_SIDE] = 0.035F;
      AMP[Tex.SANDSTONE_TOP] = 0.035F;
      AMP[Tex.BEDROCK] = 0.05F;
      AMP[Tex.MOSSY] = 0.04F;
      AMP[Tex.COAL] = 0.03F;
      AMP[Tex.IRON] = 0.03F;
      AMP[Tex.GOLD] = 0.03F;
      AMP[Tex.DIAMOND] = 0.03F;
      AMP[Tex.REDSTONE] = 0.03F;
      AMP[Tex.LAPIS] = 0.03F;
      AMP[TexArt312.NETHERRACK] = 0.06F;
      AMP[TexArt312.SOUL_SAND] = 0.05F;
      AMP[TexArt312.END_STONE] = 0.04F;
      AMP[TexArt313.GRANITE] = 0.04F;
      AMP[TexArt313.DIORITE] = 0.04F;
      AMP[TexArt313.ANDESITE] = 0.04F;
      AMP[TexArt313.RED_SAND] = 0.045F;
      AMP[TexArt313.PODZOL_TOP] = 0.065F;
      AMP[TexArt313.MYCELIUM_TOP] = 0.065F;
      AMP[TexArt313.COARSE_DIRT] = 0.065F;
      AMP[TexArt313.LEAVES0] = 0.12F;
      AMP[TexArt313.LEAVES0 + 1] = 0.12F;
      AMP[TexArt313.LEAVES0 + 2] = 0.12F;
      AMP[TexArt312.QUARTZ_ORE] = 0.04F;
      AMP[Color312.T_EMERALD_ORE] = 0.03F;
      AMP[Color312.T_CLAY] = 0.045F;
      AMP[Color312.T_TERRACOTTA] = 0.035F;
      for (int i = 0; i < 16; i++) {
         AMP[Color312.T_POWDER0 + i] = 0.04F;
      }

      AMP[TexPaint319.BUSHY_FROND] = 0.12F;
   }

   static int hash3(int x, int y, int z) {
      int h = x * 0x27D4EB2D + y * 0x165667B1 + z * 0x9E3779B1;
      h ^= h >>> 15;
      h *= 0x85EBCA6B;
      h ^= h >>> 13;
      h *= 0xC2B2AE35;
      h ^= h >>> 16;
      return h;
   }

   /** How much the brightness of a block with this face tile may vary (0.075 = +-7.5 %). */
   static float ampOf(int tile) {
      return tile >= 0 && tile < AMP.length ? AMP[tile] : 0.0F;
   }

   /** The quarter turns of the texture of face {@code face} of a block with this tile: the tops of the ground kinds, every face of leaves. */
   static int turnsOf(int tile, int face, int hh) {
      switch (tile) {
         case Tex.LEAVES:
         case TexArt313.LEAVES0:
         case TexArt313.LEAVES0 + 1:
         case TexArt313.LEAVES0 + 2:
            return (hh >>> 18) + face * 3 & 3;
         case Tex.GRASS_TOP:
         case Tex.DIRT:
         case Tex.STONE:
         case Tex.SAND:
         case Tex.GRAVEL:
         case Tex.SNOW:
         case Tex.COBBLE:
         case TexArt312.NETHERRACK:
         case TexArt312.SOUL_SAND:
         case TexArt312.END_STONE:
         case TexArt313.RED_SAND:
         case TexArt313.PODZOL_TOP:
         case TexArt313.MYCELIUM_TOP:
         case TexArt313.COARSE_DIRT:
         case Color312.T_CLAY:
            return face == 0 ? hh >>> 20 & 3 : 0;
         default:
            return 0;
      }
   }

   static boolean flipFor(Mesher.Scratch var0) {
      return var0.ao[0] + var0.ao[2] < var0.ao[1] + var0.ao[3];
   }

   static void quad(
      Mesher.Scratch var0,
      int var1,
      int var2,
      int var3,
      int var4,
      int var5,
      int var6,
      int var7,
      float var8,
      float var9,
      float var10,
      float var11,
      boolean var12,
      float var13,
      float[] var14
   ) {
      short[] var15 = var0.B;
      byte[] var16 = var0.L;
      int[] var17 = FN[var1];
      int var18 = var2 + 1 + var17[0];
      int var19 = var3 + 1 + var17[1];
      int var20 = var4 + 1 + var17[2];
      int var21 = var16[var5] & 255;
      if (Blocks.opaque[var15[var5] & 0xFFFF]) {
         var21 = var16[var6] & 255;
      }

      int var22 = PLANE[var1][0];
      int var23 = PLANE[var1][1];

      for (int var24 = 0; var24 < 4; var24++) {
         int[] var25 = FC[var1][var24];
         float var26 = 1.0F;
         float var27;
         float var28;
         if (var12) {
            int var29 = var25[var22] * 2 - 1;
            int var30 = var25[var23] * 2 - 1;
            int[] var31 = new int[]{var18, var19, var20};
            int[] var32 = new int[]{var18, var19, var20};
            int[] var33 = new int[]{var18, var19, var20};
            var31[var22] += var29;
            var32[var23] += var30;
            var33[var22] += var29;
            var33[var23] += var30;
            int var34 = ci(var31[0], var31[1], var31[2]);
            int var35 = ci(var32[0], var32[1], var32[2]);
            int var36 = ci(var33[0], var33[1], var33[2]);
            boolean var37 = Blocks.opaque[var15[var34] & 0xFFFF];
            boolean var38 = Blocks.opaque[var15[var35] & 0xFFFF];
            boolean var39 = Blocks.opaque[var15[var36] & 0xFFFF];
            int var40 = var37 && var38 ? 3 : (var37 ? 1 : 0) + (var38 ? 1 : 0) + (var39 ? 1 : 0);
            var26 = AOF[var40];
            int var41 = var37 ? var21 : var16[var34] & 0xFF;
            int var42 = var38 ? var21 : var16[var35] & 0xFF;
            int var43 = var37 && var38 ? var21 : (var39 ? var21 : var16[var36] & 0xFF);
            var27 = ((var21 >> 4 & 15) + (var41 >> 4 & 15) + (var42 >> 4 & 15) + (var43 >> 4 & 15)) * 0.25F;
            var28 = ((var21 & 15) + (var41 & 15) + (var42 & 15) + (var43 & 15)) * 0.25F;
         } else {
            var27 = var21 >> 4 & 15;
            var28 = var21 & 15;
         }

         var0.ao[var24] = var26;
         float var44 = SHADE[var1] * var26;
         float var45 = var25[0];
         float var46 = var25[1];
         float var47 = var25[2];
         uvFor(var1, var45, var46, var47, var14);
         var0.qx[var24] = var2 + var45 + var17[0] * var13;
         var0.qy[var24] = var3 + var46 + var17[1] * var13;
         var0.qz[var24] = var4 + var47 + var17[2] * var13;
         var0.qu[var24] = var14[0];
         var0.qv[var24] = var14[1];
         var0.qr[var24] = var44 * var8;
         var0.qg[var24] = var44 * var9;
         var0.qb[var24] = var44 * var10;
         var0.qa[var24] = var11;
         var0.qm[var24] = lu(var27);
         var0.ql[var24] = lu(var28);
      }
   }

   static void cross(Mesher.Scratch var0, Chunk var1, int var2, int var3, int var4, int var5) {
      int var6 = ci(var2 + 1, var3 + 1, var4 + 1);
      int var7 = var0.L[var6] & 255;
      float var8 = var7 >> 4 & 15;
      float var9 = var7 & 15;
      int var10 = tintColor(var1, var5, var2, var4);
      float var11 = (var10 >> 16 & 0xFF) / 255.0F;
      float var12 = (var10 >> 8 & 0xFF) / 255.0F;
      float var13 = (var10 & 0xFF) / 255.0F;
      int var14 = var1.cx * 16 + var2;
      int var15 = var1.cz * 16 + var4;
      int var16 = var14 * 3129871 ^ var15 * 116129781;
      var16 = var16 * var16 * 42317861 + var16 * 11;
      float var17 = ((var16 >> 16 & 15) / 15.0F - 0.5F) * 0.3F;
      float var18 = ((var16 >> 20 & 15) / 15.0F - 0.5F) * 0.3F;
      float var19 = 0.146F;
      float var20 = 0.854F;
      float[][][] var21 = new float[][][]{
         {{var19, 0.0F, var19}, {var20, 0.0F, var20}, {var20, 1.0F, var20}, {var19, 1.0F, var19}},
         {{var19, 0.0F, var20}, {var20, 0.0F, var19}, {var20, 1.0F, var19}, {var19, 1.0F, var20}}
      };
      float[] var22 = new float[]{0.0F, 1.0F, 1.0F, 0.0F};
      float[] var23 = new float[]{1.0F, 1.0F, 0.0F, 0.0F};

      for (float[][] var27 : var21) {
         for (int var28 = 0; var28 < 4; var28++) {
            var0.qx[var28] = var2 + var27[var28][0] + var17;
            var0.qy[var28] = var3 + var27[var28][1];
            var0.qz[var28] = var4 + var27[var28][2] + var18;
            var0.qu[var28] = var22[var28];
            var0.qv[var28] = var23[var28];
            var0.qr[var28] = 0.95F * var11;
            var0.qg[var28] = 0.95F * var12;
            var0.qb[var28] = 0.95F * var13;
            var0.qa[var28] = 1.0F;
            var0.qm[var28] = lu(var8);
            var0.ql[var28] = lu(var9);
         }

         var0.ao[0] = var0.ao[1] = var0.ao[2] = var0.ao[3] = 1.0F;
         emit(var0, var0.o, Blocks.tiles[var5][0], false);
         emitBack(var0, var0.o, Blocks.tiles[var5][0]);
      }
   }

   // ------------------------------------------------------------------ Update 3.9: fire that clings to blocks (Minecraft 1.12's fire models)
   static final int[][] FIRE_SIDES = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

   /**
    * Fire like in Minecraft: standing on a solid block (or on something flammable) it is the familiar X of two crossed flame planes.
    * With nothing under it, it hangs on the flammable blocks around it instead: a flame plane covers every side face that touches a
    * flammable neighbour, and one hangs from a flammable ceiling. (If no neighbour qualifies - e.g. for the moment before the fire
    * is put out - the X is drawn.)
    */
   static void fire(Mesher.Scratch var0, Chunk var1, int x, int y, int z) {
      int below = var0.B[ci(x + 1, y, z + 1)] & 0xFFFF;
      boolean floor = Fire.solidTop(below) || Fire.flammability(below) > 0;
      int drawn = 0;
      if (!floor) {
         int lightIdx = ci(x + 1, y + 1, z + 1);
         int light = var0.L[lightIdx] & 255;
         float sky = light >> 4 & 15;
         float blk = light & 15;
         for (int[] d : FIRE_SIDES) {
            if (Fire.flammability(var0.B[ci(x + 1 + d[0], y + 1, z + 1 + d[1])] & 0xFFFF) > 0) {
               fireWall(var0, x, y, z, d[0], d[1], sky, blk);
               drawn++;
            }
         }

         if (Fire.flammability(var0.B[ci(x + 1, y + 2, z + 1)] & 0xFFFF) > 0) {
            fireCeiling(var0, x, y, z, sky, blk);
            drawn++;
         }
      }

      if (floor || drawn == 0) {
         cross(var0, var1, x, y, z, Blocks.FIRE);
      }
   }

   /** One quad of flames just in front of the face of the neighbour at (dx, dz), leaning a little away from it. */
   static void fireWall(Mesher.Scratch var0, int x, int y, int z, int dx, int dz, float sky, float blk) {
      float near = 0.02F;
      float lean = 0.12F;
      // along-the-wall axis spans 0..1; the plane stands near the neighbour's face and its top leans towards the cell's middle
      for (int v = 0; v < 4; v++) {
         boolean top = v >= 2;
         boolean second = v == 1 || v == 2;
         float along = second ? 1.0F : 0.0F;
         float across = (dx != 0 ? (dx > 0 ? 1.0F - near : near) : (dz > 0 ? 1.0F - near : near)) - (top ? Math.signum(dx != 0 ? dx : dz) * lean : 0.0F);
         var0.qx[v] = x + (dx != 0 ? across : along);
         var0.qy[v] = y + (top ? 1.0F : 0.0F);
         var0.qz[v] = z + (dz != 0 ? across : along);
         var0.qu[v] = second ? 1.0F : 0.0F;
         var0.qv[v] = top ? 0.0F : 1.0F;
         var0.qr[v] = var0.qg[v] = var0.qb[v] = 1.0F;
         var0.qa[v] = 1.0F;
         var0.qm[v] = lu(sky);
         var0.ql[v] = lu(blk);
      }

      var0.ao[0] = var0.ao[1] = var0.ao[2] = var0.ao[3] = 1.0F;
      emit(var0, var0.o, Blocks.tiles[Blocks.FIRE][0], false);
      emitBack(var0, var0.o, Blocks.tiles[Blocks.FIRE][0]);
   }

   /** Flames hanging from a flammable ceiling: two crossed planes in the upper part of the cell. */
   static void fireCeiling(Mesher.Scratch var0, int x, int y, int z, float sky, float blk) {
      for (int plane = 0; plane < 2; plane++) {
         for (int v = 0; v < 4; v++) {
            boolean top = v >= 2;
            boolean second = v == 1 || v == 2;
            float along = second ? 0.9F : 0.1F;
            var0.qx[v] = x + (plane == 0 ? along : 0.5F);
            var0.qy[v] = y + (top ? 1.0F : 0.3F);
            var0.qz[v] = z + (plane == 0 ? 0.5F : along);
            var0.qu[v] = second ? 1.0F : 0.0F;
            var0.qv[v] = top ? 0.0F : 1.0F;
            var0.qr[v] = var0.qg[v] = var0.qb[v] = 1.0F;
            var0.qa[v] = 1.0F;
            var0.qm[v] = lu(sky);
            var0.ql[v] = lu(blk);
         }

         var0.ao[0] = var0.ao[1] = var0.ao[2] = var0.ao[3] = 1.0F;
         emit(var0, var0.o, Blocks.tiles[Blocks.FIRE][0], false);
         emitBack(var0, var0.o, Blocks.tiles[Blocks.FIRE][0]);
      }
   }

   static void crop(Mesher.Scratch var0, Chunk var1, int var2, int var3, int var4, int var5) {
      int var6 = ci(var2 + 1, var3 + 1, var4 + 1);
      int var7 = var0.L[var6] & 255;
      float var8 = var7 >> 4 & 15;
      float var9 = var7 & 15;
      float var10 = 0.25F;
      float var11 = 0.75F;
      float[][][] var12 = new float[][][]{
         {{0.0F, 0.0F, var10}, {1.0F, 0.0F, var10}, {1.0F, 1.0F, var10}, {0.0F, 1.0F, var10}},
         {{0.0F, 0.0F, var11}, {1.0F, 0.0F, var11}, {1.0F, 1.0F, var11}, {0.0F, 1.0F, var11}},
         {{var10, 0.0F, 0.0F}, {var10, 0.0F, 1.0F}, {var10, 1.0F, 1.0F}, {var10, 1.0F, 0.0F}},
         {{var11, 0.0F, 0.0F}, {var11, 0.0F, 1.0F}, {var11, 1.0F, 1.0F}, {var11, 1.0F, 0.0F}}
      };
      float[] var13 = new float[]{0.0F, 1.0F, 1.0F, 0.0F};
      float[] var14 = new float[]{1.0F, 1.0F, 0.0F, 0.0F};

      for (float[][] var18 : var12) {
         for (int var19 = 0; var19 < 4; var19++) {
            var0.qx[var19] = var2 + var18[var19][0];
            var0.qy[var19] = var3 + var18[var19][1] - 0.0625F;
            var0.qz[var19] = var4 + var18[var19][2];
            var0.qu[var19] = var13[var19];
            var0.qv[var19] = var14[var19];
            var0.qr[var19] = var0.qg[var19] = var0.qb[var19] = 0.95F;
            var0.qa[var19] = 1.0F;
            var0.qm[var19] = lu(var8);
            var0.ql[var19] = lu(var9);
         }

         var0.ao[0] = var0.ao[1] = var0.ao[2] = var0.ao[3] = 1.0F;
         emit(var0, var0.o, Blocks.tiles[var5][0], false);
         emitBack(var0, var0.o, Blocks.tiles[var5][0]);
      }
   }

   /** Update 3.12: the film of a nether portal - one plane through the middle of the cell, seen from both sides, blended (translucent buffer). */
   static void portal(Mesher.Scratch s, int x, int y, int z, int id) {
      int light = s.L[ci(x + 1, y + 1, z + 1)] & 255;
      float sky = light >> 4 & 15;
      float blk = Math.max(light & 15, Blocks.emit[id]);
      boolean alongX = id == Blocks.PORTAL_X;
      int tile = Blocks.tiles[id][0];
      for (int v = 0; v < 4; v++) {
         float u = v == 1 || v == 2 ? 1.0F : 0.0F;
         float h = v >= 2 ? 1.0F : 0.0F;
         s.qx[v] = x + (alongX ? u : 0.5F);
         s.qy[v] = y + h;
         s.qz[v] = z + (alongX ? 0.5F : u);
         s.qu[v] = u;
         s.qv[v] = 1.0F - h;
         s.qr[v] = s.qg[v] = s.qb[v] = 1.0F;
         s.qa[v] = 0.9F;
         s.qm[v] = lu(sky);
         s.ql[v] = lu(blk);
      }

      s.ao[0] = s.ao[1] = s.ao[2] = s.ao[3] = 1.0F;
      emit(s, s.t, tile, false);
      emitBack(s, s.t, tile);
   }

   /** Update 3.13: vines: a thin leafy plane in front of each wall the vine clings to (north 1, east 2, south 4, west 8). */
   static void vine(Mesher.Scratch s, Chunk c, int x, int y, int z, int id) {
      int mask = id - Blocks313.VINE0;
      int light = s.L[ci(x + 1, y + 1, z + 1)] & 255;
      float sky = light >> 4 & 15;
      float blk = light & 15;
      int tint = tintColor(c, id, x, z);
      float r = (tint >> 16 & 0xFF) / 255.0F;
      float g = (tint >> 8 & 0xFF) / 255.0F;
      float b = (tint & 0xFF) / 255.0F;
      int tile = Blocks.tiles[id][0];
      float e = 0.04F;
      for (int side = 0; side < 4; side++) {
         if ((mask & 1 << side) == 0) {
            continue;
         }

         for (int v = 0; v < 4; v++) {
            float u = v == 1 || v == 2 ? 1.0F : 0.0F;
            float h = v >= 2 ? 1.0F : 0.0F;
            float px;
            float pz;
            switch (side) {
               case 0:
                  px = u;
                  pz = e;
                  break;
               case 1:
                  px = 1.0F - e;
                  pz = u;
                  break;
               case 2:
                  px = 1.0F - u;
                  pz = 1.0F - e;
                  break;
               default:
                  px = e;
                  pz = 1.0F - u;
                  break;
            }

            s.qx[v] = x + px;
            s.qy[v] = y + h;
            s.qz[v] = z + pz;
            s.qu[v] = u;
            s.qv[v] = 1.0F - h;
            s.qr[v] = 0.92F * r;
            s.qg[v] = 0.92F * g;
            s.qb[v] = 0.92F * b;
            s.qa[v] = 1.0F;
            s.qm[v] = lu(sky);
            s.ql[v] = lu(blk);
         }

         s.ao[0] = s.ao[1] = s.ao[2] = s.ao[3] = 1.0F;
         emit(s, s.o, tile, false);
         emitBack(s, s.o, tile);
      }
   }

   static void box(Mesher.Scratch var0, Chunk var1, int var2, int var3, int var4, int var5) {
      boxBB(var0, var1, var2, var3, var4, var5, Blocks.box[var5], null);
   }

   static void stair(Mesher.Scratch var0, Chunk var1, int var2, int var3, int var4, int var5) {
      int var6 = Blocks.stairFacing(var5);
      boolean upsideDown = Blocks.isTopStair(var5);
      double[][] var7 = new double[][]{{0.0, 0.0, 0.0, 1.0, 0.5, 0.5}, {0.0, 0.5, 0.0, 1.0, 1.0, 0.5}, {0.0, 0.0, 0.5, 1.0, 0.5, 1.0}};
      int[][][] var8 = new int[][][]{{{0, 0, 1}, {0, 1, 0}}, {{0, -1, 0}}, {{0, 0, -1}}};
      if (upsideDown) {
         // Update 3.10: the same three boxes mirrored top to bottom (and the neighbours they hide each other's faces against)
         for (double[] box : var7) {
            double lo = 1.0 - box[4];
            box[4] = 1.0 - box[1];
            box[1] = lo;
         }

         for (int[][] hidden : var8) {
            for (int[] off : hidden) {
               off[1] = -off[1];
            }
         }
      }

      for (int var9 = 0; var9 < 3; var9++) {
         double[] var10 = var7[var9];
         double[] var11 = new double[6];
         double[][] var12 = new double[][]{{var10[0], var10[2]}, {var10[3], var10[5]}};
         double[] var13 = new double[2];
         double[] var14 = new double[2];

         for (int var15 = 0; var15 < 2; var15++) {
            double var16 = var12[var15][0];
            double var18 = var12[var15][1];
            switch (var6) {
               case 0:
                  var13[var15] = var16;
                  var14[var15] = var18;
                  break;
               case 1:
                  var13[var15] = 1.0 - var16;
                  var14[var15] = 1.0 - var18;
                  break;
               case 2:
                  var13[var15] = 1.0 - var18;
                  var14[var15] = var16;
                  break;
               default:
                  var13[var15] = var18;
                  var14[var15] = 1.0 - var16;
            }
         }

         var11[0] = Math.min(var13[0], var13[1]);
         var11[3] = Math.max(var13[0], var13[1]);
         var11[2] = Math.min(var14[0], var14[1]);
         var11[5] = Math.max(var14[0], var14[1]);
         var11[1] = var10[1];
         var11[4] = var10[4];
         int[][] var20 = new int[var8[var9].length][];

         for (int var21 = 0; var21 < var20.length; var21++) {
            int var17 = var8[var9][var21][0];
            int var22 = var8[var9][var21][1];
            int var19 = var8[var9][var21][2];
            switch (var6) {
               case 0:
                  var20[var21] = new int[]{var17, var22, var19};
                  break;
               case 1:
                  var20[var21] = new int[]{-var17, var22, -var19};
                  break;
               case 2:
                  var20[var21] = new int[]{-var19, var22, var17};
                  break;
               default:
                  var20[var21] = new int[]{var19, var22, -var17};
            }
         }

         boxBB(var0, var1, var2, var3, var4, var5, var11, var20);
      }
   }

   static boolean fenceLink(Mesher.Scratch var0, int var1, int var2, int var3, int dir) {
      int var4 = var0.B[ci(var1, var2, var3)] & 0xFFFF;
      return Blocks.isFence(var4) || Blocks.opaque[var4] || Shapes.fenceLinks(var4, dir);
   }

   static void fence(Mesher.Scratch var0, Chunk var1, int var2, int var3, int var4, int var5) {
      boxBB(var0, var1, var2, var3, var4, var5, new double[]{0.375, 0.0, 0.375, 0.625, 1.0, 0.625}, null);
      int var6 = var2 + 1;
      int var7 = var3 + 1;
      int var8 = var4 + 1;
      boolean[] var9 = new boolean[]{
         fenceLink(var0, var6, var7, var8 - 1, 0),
         fenceLink(var0, var6, var7, var8 + 1, 1),
         fenceLink(var0, var6 + 1, var7, var8, 2),
         fenceLink(var0, var6 - 1, var7, var8, 3)
      };
      double[][] var10 = new double[][]{
         {0.4375, 0.0, 0.0, 0.5625, 0.0, 0.375},
         {0.4375, 0.0, 0.625, 0.5625, 0.0, 1.0},
         {0.625, 0.0, 0.4375, 1.0, 0.0, 0.5625},
         {0.0, 0.0, 0.4375, 0.375, 0.0, 0.5625}
      };
      double[][] var11 = new double[][]{{0.375, 0.5625}, {0.75, 0.9375}};

      for (int var12 = 0; var12 < 4; var12++) {
         if (var9[var12]) {
            for (double[] var16 : var11) {
               double[] var17 = (double[])var10[var12].clone();
               var17[1] = var16[0];
               var17[4] = var16[1];
               boxBB(var0, var1, var2, var3, var4, var5, var17, null);
            }
         }
      }
   }

   static void wallTorchLegacy(Mesher.Scratch var0, Chunk var1, int var2, int var3, int var4, int var5) {
      int var6 = var5 - 89;
      float var7 = var6 == 2 ? -1.0F : (var6 == 3 ? 1.0F : 0.0F);
      float var8 = var6 == 0 ? 1.0F : (var6 == 1 ? -1.0F : 0.0F);
      int var9 = ci(var2 + 1, var3 + 1, var4 + 1);
      int var10 = var0.L[var9] & 255;
      float var11 = var10 >> 4 & 15;
      float var12 = var10 & 15;
      double[] var13 = new double[]{0.4375, 0.0, 0.4375, 0.5625, 0.625, 0.5625};
      float[] var14 = (float[])UVT.get();
      int[] var15 = new int[]{0, 2, 3, 4, 5};

      for (int var19 : var15) {
         for (int var20 = 0; var20 < 4; var20++) {
            int[] var21 = FC[var19][var20];
            float var22 = (float)(var21[0] == 0 ? var13[0] : var13[3]);
            float var23 = (float)(var21[1] == 0 ? var13[1] : var13[4]);
            float var24 = (float)(var21[2] == 0 ? var13[2] : var13[5]);
            uvFor(var19, var22, var23, var24, var14);
            float var25 = var23 * 0.55F;
            var0.qx[var20] = var2 + 0.5F + (var22 - 0.5F) - var7 * 0.34F + var7 * var25;
            var0.qy[var20] = var3 + 0.2F + var23;
            var0.qz[var20] = var4 + 0.5F + (var24 - 0.5F) - var8 * 0.34F + var8 * var25;
            var0.qu[var20] = var14[0];
            var0.qv[var20] = var14[1];
            var0.qr[var20] = var0.qg[var20] = var0.qb[var20] = 1.0F;
            var0.qa[var20] = 1.0F;
            var0.qm[var20] = lu(var11);
            var0.ql[var20] = lu(var12);
         }

         var0.ao[0] = var0.ao[1] = var0.ao[2] = var0.ao[3] = 1.0F;
         emit(var0, var0.o, Blocks.tiles[var5][var19], false);
      }
   }


   static void wallTorch(Mesher.Scratch var0, Chunk var1, int var2, int var3, int var4, int var5) {
      if(legacy){wallTorchLegacy(var0,var1,var2,var3,var4,var5);return;}
      int var6 = var5 - 89;
      float var7 = var6 == 2 ? -1.0F : (var6 == 3 ? 1.0F : 0.0F);
      float var8 = var6 == 0 ? 1.0F : (var6 == 1 ? -1.0F : 0.0F);
      int var9 = ci(var2 + 1, var3 + 1, var4 + 1);
      int var10 = var0.L[var9] & 255;
      float var11 = var10 >> 4 & 15;
      float var12 = var10 & 15;
      float[] uv=new float[2],xyz=new float[3];
      for(double[] part:Shapes.modelBoxes(Blocks.TORCH))for(int face=0;face<6;face++) {
         for(int i=0;i<4;i++) {
            Shapes.corner(part,face,i,xyz,uv);float lean=xyz[1]*.55F;
            var0.qx[i]=var2+xyz[0]-var7*.34F+var7*lean;
            var0.qy[i]=var3+.2F+xyz[1];
            var0.qz[i]=var4+xyz[2]-var8*.34F+var8*lean;
            var0.qu[i]=uv[0];var0.qv[i]=uv[1];
            var0.qr[i]=var0.qg[i]=var0.qb[i]=var0.qa[i]=var0.ao[i]=1;
            var0.qm[i]=lu(var11);var0.ql[i]=lu(Math.max(var12,Blocks.emit[var5]));
         }
         int tile=(int)(face==0?part[9]:face==1?part[10]:part[8]);
         emit(var0,var0.o,tile,false);
      }
   }

   /** Stairs and slabs: not opaque, yet they block light completely (so their own cell and the cells of neighbouring ones are pitch dark). */
   static boolean isPartial(int id) {
      return Blocks.opacity[id] == 15 && !Blocks.opaque[id];
   }

   /**
    * Update 3.9: like Minecraft's "use neighbour brightness" for slabs and stairs: the light of such a block is the best light around it.
    * Without this the faces of an upside-down stair or of a slab that look into the ground or into another stair were black.
    */
   static int neighbourLight(Mesher.Scratch var0, int x, int y, int z) {
      int sky = 0;
      int blk = 0;
      for (int f = 0; f < 6; f++) {
         int q = ci(x + FN[f][0], y + FN[f][1], z + FN[f][2]);
         int id = var0.B[q] & 0xFFFF;
         if (!Blocks.opaque[id] && !isPartial(id)) {
            int l = var0.L[q] & 255;
            sky = Math.max(sky, l >> 4 & 15);
            blk = Math.max(blk, l & 15);
         }
      }

      return sky << 4 | blk;
   }

   static void boxBB(Mesher.Scratch var0, Chunk var1, int var2, int var3, int var4, int var5, double[] var6, int[][] var7) {
      int var8 = var2 + 1;
      int var9 = var3 + 1;
      int var10 = var4 + 1;
      int var11 = ci(var8, var9, var10);
      int var12 = var0.L[var11] & 255;
      boolean partial = isPartial(var5);
      if (partial) {
         var12 = neighbourLight(var0, var8, var9, var10);
      }

      float[] var13 = (float[])UVT.get();

      for (int var14 = 0; var14 < 6; var14++) {
         int[] var15 = FN[var14];
         if (var7 != null) {
            boolean var16 = false;

            for (int[] var20 : var7) {
               if (var20[0] == var15[0] && var20[1] == var15[1] && var20[2] == var15[2]) {
                  var16 = true;
               }
            }

            if (var16) {
               continue;
            }
         }

         int var31 = var14 / 2 == 0 ? 1 : (var14 / 2 == 1 ? 2 : 0);
         // faces 2 and 3 are -z and +z (FN order), so "the positive side" is not simply the even faces: with the old test a thin door lost its
         // visible big face whenever an opaque block stood against that side (Update 3.11)
         boolean var40 = legacy ? var14 % 2 == 0 : FN[var14][var31] > 0;
         double var33 = var40 ? var6[3 + var31] : var6[var31];
         boolean var32 = var40 ? var33 >= 1.0 : var33 <= 0.0;
         if (var32) {
            int var34 = var0.B[ci(var8 + var15[0], var9 + var15[1], var10 + var15[2])] & 0xFFFF;
            if (Blocks.opaque[var34]) {
               continue;
            }
         }

         int var35 = var12;
         int var21 = ci(var8 + var15[0], var9 + var15[1], var10 + var15[2]);
         if (!Blocks.opaque[var0.B[var21] & 0xFFFF] && !isPartial(var0.B[var21] & 0xFFFF)) {
            var35 = var0.L[var21] & 255;
         }

         if (var5 == 32) {
            var35 = var12;
         }

         float var22 = var35 >> 4 & 15;
         float var23 = var35 & 15;
         boolean var24 = false;
         if (Blocks.isDoor(var5)) {
            int var25 = var6[3] - var6[0] < var6[5] - var6[2] ? 4 : 2;
            var24 = var14 / 2 * 2 != var25;
         }

         for (int var36 = 0; var36 < 4; var36++) {
            int[] var26 = FC[var14][var36];
            float var27 = (float)(var26[0] == 0 ? var6[0] : var6[3]);
            float var28 = (float)(var26[1] == 0 ? var6[1] : var6[4]);
            float var29 = (float)(var26[2] == 0 ? var6[2] : var6[5]);
            uvFor(var14, var27, var28, var29, var13);
            if (var24) {
               var13[0] = 0.3F + 0.4F * (var13[0] * 7.0F % 1.0F);
               var13[1] = 0.3F + 0.4F * (var13[1] * 7.0F % 1.0F);
            }

            var0.qx[var36] = var2 + var27;
            var0.qy[var36] = var3 + var28;
            var0.qz[var36] = var4 + var29;
            var0.qu[var36] = var13[0];
            var0.qv[var36] = var13[1];
            float var30 = var5 == 32 ? 1.0F : SHADE[var14];
            var0.qr[var36] = var30;
            var0.qg[var36] = var30;
            var0.qb[var36] = var30;
            var0.qa[var36] = 1.0F;
            var0.qm[var36] = lu(var22);
            var0.ql[var36] = lu(var23);
         }

         var0.ao[0] = var0.ao[1] = var0.ao[2] = var0.ao[3] = 1.0F;
         emit(var0, var0.o, var24 ? 10 : Blocks.tiles[var5][var14], false);
      }
   }

   static float cornerHeight(Mesher.Scratch var0, int var1, int var2, int var3, int var4, int var5, int var6) {
      float var7 = 0.0F;
      int var8 = 0;

      for (int var9 = var5 - 1; var9 <= var5; var9++) {
         for (int var10 = var4 - 1; var10 <= var4; var10++) {
            int var11 = var0.B[ci(var1 + var10, var2 + 1, var3 + var9)] & 0xFFFF;
            if (Blocks.sameFluid(var11, var6)) {
               return 1.0F;
            }

            int var12 = var0.B[ci(var1 + var10, var2, var3 + var9)] & 0xFFFF;
            if (Blocks.sameFluid(var12, var6)) {
               float var13 = (8 - Blocks.fluidLevel(var12)) / 9.0F;
               var7 += var13;
               var8++;
            } else if (!Blocks.solid[var12]) {
               var8++;
            }
         }
      }

      return var8 == 0 ? 0.0F : var7 / var8;
   }

   static void fluid(Mesher.Scratch var0, Chunk var1, int var2, int var3, int var4, int var5) {
      int var6 = var2 + 1;
      int var7 = var3 + 1;
      int var8 = var4 + 1;
      boolean var9 = Blocks.isWater(var5);
      Mesher.FB var10 = var9 ? var0.t : var0.o;
      int var11 = ci(var6, var7, var8);
      float[] var12 = (float[])UVT.get();
      float[][] var13 = new float[2][2];

      for (int var14 = 0; var14 < 2; var14++) {
         for (int var15 = 0; var15 < 2; var15++) {
            var13[var14][var15] = cornerHeight(var0, var6, var7, var8, var15, var14, var5);
         }
      }

      int var26 = var9 ? 23 : 24;
      // Update 3.13: the water of a swamp is greenish, a warm sea turquoise, a frozen one pale (Chunk.waterColor); lava stays as it is
      int wc = var9 ? var1.waterColor[var4 * 16 + var2] : 0xFFFFFF;
      float wr = (wc >> 16 & 0xFF) / 255.0F;
      float wg = (wc >> 8 & 0xFF) / 255.0F;
      float wb = (wc & 0xFF) / 255.0F;

      for (int var27 = 0; var27 < 6; var27++) {
         int[] var16 = FN[var27];
         int var17 = ci(var6 + var16[0], var7 + var16[1], var8 + var16[2]);
         int var18 = var0.B[var17] & 0xFFFF;
         if (!Blocks.opaque[var18] && !Blocks.sameFluid(var18, var5)) {
            int var19 = var0.L[var17] & 255;
            if (!var9) {
               var19 = var0.L[var11] & 255;
            }

            float var20 = var19 >> 4 & 15;
            float var21 = var19 & 15;

            for (int var22 = 0; var22 < 4; var22++) {
               int[] var23 = FC[var27][var22];
               float var24 = var23[1] == 1 ? var13[var23[2]][var23[0]] : 0.0F;
               uvFor(var27, var23[0], var24, var23[2], var12);
               if (var27 < 2) {
                  var12[0] = var23[0];
                  var12[1] = var23[2];
               }

               var0.qx[var22] = var2 + var23[0];
               var0.qy[var22] = var3 + var24;
               var0.qz[var22] = var4 + var23[2];
               var0.qu[var22] = var12[0];
               var0.qv[var22] = var12[1];
               float var25 = var9 ? SHADE[var27] : 0.8F + 0.2F * SHADE[var27];
               var0.qr[var22] = var25 * wr;
               var0.qg[var22] = var25 * wg;
               var0.qb[var22] = var25 * wb;
               var0.qa[var22] = var9 ? 0.72F : 1.0F;
               var0.qm[var22] = lu(var20);
               var0.ql[var22] = lu(var21);
            }

            var0.ao[0] = var0.ao[1] = var0.ao[2] = var0.ao[3] = 1.0F;
            emit(var0, var10, var26, false);
            if (var27 == 0) {
               emitBack(var0, var10, var26); // the surface is also seen from below (from under water)
            }
         }
      }
   }

   static final class FB {
      float[] a = new float[32768];
      int n;

      void clear() {
         this.n = 0;
      }

      void v(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, float var10, float var11) {
         if (this.n + 11 > this.a.length) {
            this.a = Arrays.copyOf(this.a, this.a.length * 2);
         }

         this.a[this.n++] = var1;
         this.a[this.n++] = var2;
         this.a[this.n++] = var3;
         this.a[this.n++] = var4;
         this.a[this.n++] = var5;
         this.a[this.n++] = var6;
         this.a[this.n++] = var7;
         this.a[this.n++] = var8;
         this.a[this.n++] = var9;
         this.a[this.n++] = var10;
         this.a[this.n++] = var11;
      }
   }

   public static final class Result {
      public FloatBuffer opaque;
      public FloatBuffer trans;
      public int nO;
      public int nT;
      /** Update 3.18: which faces of the section are connected through open blocks (CaveCull.compute). */
      public long conn = CaveCull.ALL;
   }

   static final class Scratch {
      final short[] B = new short[5832];
      final byte[] L = new byte[5832];
      final Mesher.FB o = new Mesher.FB();
      final Mesher.FB t = new Mesher.FB();
      final float[] qx = new float[4];
      final float[] qy = new float[4];
      final float[] qz = new float[4];
      final float[] qu = new float[4];
      final float[] qv = new float[4];
      final float[] qr = new float[4];
      final float[] qg = new float[4];
      final float[] qb = new float[4];
      final float[] qa = new float[4];
      final float[] ql = new float[4];
      final float[] qm = new float[4];
      final float[] ao = new float[4];
      final float[] uvt = new float[2];
      final Chunk[] chunks9 = new Chunk[9];
      final int[] fSky = new int[6];
      final int[] fBlk = new int[6];
      final boolean[] fOpen = new boolean[6];
      /** Update 3.19: the world position of the section's corner (for the variation between blocks) and the quarter turns of the next quad's texture. */
      int ox;
      int oy;
      int oz;
      int rot;
   }
}
