package bc;

import bc.Gen.Col;
import bc.Structs.Bld;
import bc.Structs.Path;
import bc.Structs.Village;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;

public final class Lod {
   static final int N = 32;
   static final int MIN = 32;
   static final int MAXS = 1024;
   /** Update 6.19: LOD detail bias factors, indexed by Options.lodDetail. */
   static final double[] DETAIL_FACTOR = new double[]{0.70, 1.0, 1.35};

   static double detailFactor(int lodDetail) {
      return DETAIL_FACTOR[Options.clamp(lodDetail, 0, 2)];
   }

   /** Update 6.19: tiles beyond this distance are fully fogged and are not visited or drawn. */
   static int fogFarBlocks(int maxDist, Options lo) {
      return lo == null ? maxDist : (int)Math.max(64.0F, lo.lodFogEnd * maxDist);
   }
   /** V6.26: frames over which a freshly built LOD tile fades in instead of popping. */
   static final int FADE_FRAMES = 18;
   static final float LAND_SHIFT = 2.5F;
   static final float WATER_SHIFT = 0.2F;
   final Gen gen;
   /** Update 3.11: what the world really looks like where it was changed (null: the generator only). */
   final LodMap map;
   final AtomicLong epoch = new AtomicLong();
   final Renderer rend;
   final ConcurrentHashMap<Long, Lod.Tile> tiles = new ConcurrentHashMap();
   final ExecutorService pool;
   final ConcurrentLinkedQueue<Lod.Tile> uploads = new ConcurrentLinkedQueue();
   final AtomicInteger inflight = new AtomicInteger();
   long frame;
   int whiteTex;
   public int drawn;
   public int built;
   volatile boolean dead;
   double cx;
   double cz;
   double cy;
   int maxDist;
   int innerHalf;
   ArrayList<Lod.Tile> drawList = new ArrayList();
   final ArrayList<Lod.Tile> pendingVisible = new ArrayList<>();
   final ArrayList<Lod.Tile> pendingBehind = new ArrayList<>();
   final int maxInflight;
   /** Update 6.19: reused instead of allocating a root-tile list, int[2] per root, a comparator and a colour array every frame. */
   final ArrayList<int[]> rootTiles = new ArrayList<>();
   int rootUsed;
   final float[] texEnvColor = new float[4];
   final Comparator<int[]> rootOrder = (a, b) -> Double.compare(this.nearest(a[0], a[1], 1024), this.nearest(b[0], b[1], 1024));
   final Comparator<Lod.Tile> tileOrder = (a, b) -> Double.compare(
      this.nearest(a.x0, a.z0, a.size), this.nearest(b.x0, b.z0, b.size));
   /** Update 6.19: LOD tiles beyond this distance are fully faded into fog and are not visited at all. */
   int fogFarBlocks = Integer.MAX_VALUE;

   public Lod(Renderer var1, Gen var2) {
      this(var1, var2, null);
   }

   public Lod(Renderer var1, Gen var2, LodMap map) {
      this.rend = var1;
      this.gen = var2;
      this.map = map;
      if (map != null) {
         map.listener = this::invalidate;
      }

      int cores = Math.max(1, Runtime.getRuntime().availableProcessors());
      Options lo = var1 == null || var1.g == null ? null : var1.g.opt;
      int var3 = lo != null && lo.lodWorkers > 0 ? Math.min(4, lo.lodWorkers) : Math.max(1, Math.min(2, (cores + 3) / 4));
      this.maxInflight = var3 * 4;
      this.pool = Executors.newFixedThreadPool(var3, var0 -> {
         Thread var1x = new Thread(var0, "lod-gen");
         var1x.setDaemon(true);
         var1x.setPriority(Thread.NORM_PRIORITY - 2);
         return var1x;
      });
   }

   void ensureTex() {
      if (this.whiteTex == 0) {
         this.whiteTex = GL11.glGenTextures();
         GL11.glBindTexture(3553, this.whiteTex);
         ByteBuffer var1 = BufferUtils.createByteBuffer(4);
         var1.put((byte)-1).put((byte)-1).put((byte)-1).put((byte)-1).flip();
         GL11.glTexParameteri(3553, 10241, 9728);
         GL11.glTexParameteri(3553, 10240, 9728);
         GL11.glTexImage2D(3553, 0, 6408, 1, 1, 0, 6408, 5121, var1);
      }
   }

   static long key(int var0, int var1, int var2) {
      return (var1 / var0 & 4194303L) << 40 | (var2 / var0 & 4194303L) << 18 | Integer.numberOfTrailingZeros(var0) & 31;
   }

   Lod.Tile tile(int var1, int var2, int var3) {
      long var4 = key(var1, var2, var3);
      Lod.Tile var6 = (Lod.Tile)this.tiles.get(var4);
      if (var6 == null) {
         var6 = new Lod.Tile(var1, var2, var3);
         Lod.Tile var7 = (Lod.Tile)this.tiles.putIfAbsent(var4, var6);
         if (var7 != null) {
            var6 = var7;
         }
      }

      return var6;
   }

   void request(Lod.Tile var1) {
      if (var1.state == 0 && this.inflight.get() < this.maxInflight) {
         var1.state = 1;
         this.inflight.incrementAndGet();
         try {
            this.pool.execute(() -> {
               try {
                  if (!this.dead && !var1.retired) {
                     if (this.maxDist > 0 && this.nearest(var1.x0, var1.z0, var1.size) > this.maxDist + 512.0) {
                        var1.state = 0;
                        return;
                     }
                     this.build(var1);
                  }
               } catch (Throwable var6) {
                  System.err.println("[lod] build failed at " + var1.x0 + "," + var1.z0 + " / " + var1.size + ": " + var6);
                  var6.printStackTrace();
                  var1.state = 4;
               } finally {
                  this.inflight.decrementAndGet();
               }
            });
         } catch (RuntimeException rejected) {
            // The pool was shut down (world change / quit). Do not leave the tile stuck
            // in state 1 with a leaked inflight slot, or all LOD requests stop forever.
            var1.state = 0;
            this.inflight.decrementAndGet();
         }
      }
   }

   static float hash(int var0, int var1) {
      int var2 = var0 * 374761393 + var1 * 668265263;
      var2 = (var2 ^ var2 >>> 13) * 1274126177;
      return ((var2 ^ var2 >>> 16) & 65535) / 65535.0F;
   }

   static int pack(int var0, float var1, float var2) {
      float var3 = var1 * (1.0F + var2);
      int var4 = Math.min(255, (int)((var0 >> 16 & 0xFF) * var3));
      int var5 = Math.min(255, (int)((var0 >> 8 & 0xFF) * var3));
      int var6 = Math.min(255, (int)((var0 & 0xFF) * var3));
      return var4 | var5 << 8 | var6 << 16 | 0xFF000000;
   }

   static int code(int var0, int var1) {
      return var0 & 16777215 | var1 << 24;
   }

   static final ThreadLocal<ByteBuffer> SCRATCH = ThreadLocal.withInitial(
      () -> ByteBuffer.allocateDirect(786432).order(ByteOrder.nativeOrder())
   );

   /**
    * Builds the mesh of a tile: 32 x 32 cells, one column each (the surface of the cell's middle), walls down to lower neighbours, trees and
    * houses as boxes. Update 3.11: where the world has a {@link LodMap.Sum summary} of the chunk (it was changed) the cell shows what is really
    * there; elsewhere it is still the generator. Every quad is counter-clockwise seen from the outside (back faces are culled).
    */
   void build(Lod.Tile t) {
      final int cs = t.size / 32; // blocks per cell
      final float sink = Math.min(2.5F, 0.4F + cs * 0.35F); // the land is sunk a little: it hides under the real terrain
      final int w = 34;
      t.builtEpoch = this.epoch.get();
      int[] topY = new int[w * w]; // the plane of the cell's surface: y of its top block + 1
      int[] rgb = new int[w * w];
      boolean[] wet = new boolean[w * w];
      int[] terrain = new int[w * w]; // height of the terrain (for the wall colours of mountains)
      int[] biome = new int[w * w];
      int[] cliff = new int[w * w]; // colour of the wall under a real surface (0 = the biome rules)
      List<Village> villages = t.size <= 512 ? this.villagesFor(t.x0 - 8, t.z0 - 8, t.x0 + t.size + 8, t.z0 + t.size + 8) : null;

      for (int cz = 0; cz < w; cz++) {
         for (int cx = 0; cx < w; cx++) {
            int x = t.x0 + (cx - 1) * cs + cs / 2;
            int z = t.z0 + (cz - 1) * cs + cs / 2;
            int i = cz * w + cx;
            LodMap.Sum sum = this.map == null ? null : this.map.get(x >> 4, z >> 4);
            if (sum != null) {
               int k = (z & 15) * 16 + (x & 15);
               wet[i] = sum.depth[k] > 0;
               topY[i] = sum.top[k];
               rgb[i] = sum.rgb[k];
               cliff[i] = sum.side[k];
               terrain[i] = sum.top[k] - 1;
               biome[i] = 2;
               continue;
            }

            Col col = this.gen.column(x, z);
            terrain[i] = col.h;
            biome[i] = col.biome;
            boolean sea = col.h < 63;
            int c;
            if (sea) {
               float d = Math.min(1.0F, (63 - col.h) / 30.0F);
               c = (int)(58.0F * (1.0F - d * 0.6F)) << 16 | (int)(104.0F * (1.0F - d * 0.5F)) << 8 | (int)(208.0F * (1.0F - d * 0.4F));
               if (col.biome > 6) {
                  c = Biomes.lodWater(col.biome, c);
               }
            } else {
               c = Biomes.lodColor(col.biome, col.h, col.t, col.hu);
            }

            if (!sea && cs <= 8 && villages != null) {
               for (Village v : villages) {
                  if (!v.placed.isEmpty()) {
                     int pathCol = v.style == Style.DESERT ? 14471328 : (v.style == Style.SAVANNA ? 7820603 : (v.style >= Style.SPRUCE ? 7229496 : 8748922));
                     for (Village314.Street st : v.streets) {
                        if (x >= st.x0 && x <= st.x1 && z >= st.z0 && z <= st.z1) {
                           c = pathCol;
                           break;
                        }
                     }
                  } else {
                     for (Path p : v.paths) {
                        if (x >= p.x0 && x <= p.x1 && z >= p.z0 && z <= p.z1) {
                           c = v.desert ? 14471328 : 8748922;
                        }
                     }
                  }
               }
            }

            wet[i] = sea;
            topY[i] = sea ? 63 : col.h + 1;
            rgb[i] = c;
         }
      }

      ByteBuffer bb = SCRATCH.get();
      bb.clear();
      int[] n = new int[]{0};
      int[][] dirs = new int[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
      for (int cz = 1; cz <= 32; cz++) {
         for (int cx = 1; cx <= 32; cx++) {
            int i = cz * w + cx;
            float y = topY[i] - (wet[i] ? 0.2F : sink);
            float x0 = (cx - 1) * cs;
            float x1 = x0 + cs;
            float z0 = (cz - 1) * cs;
            float z1 = z0 + cs;
            float jitter = (hash(t.x0 / cs + cx, t.z0 / cs + cz) - 0.5F) * 0.12F;
            int base = rgb[i];
            if (cs >= 2 && !wet[i] && cliff[i] == 0) {
               float dhx = (terrain[i + 1] - terrain[i - 1]) / (float)Math.max(2, cs * 2);
               float dhz = (terrain[i + w] - terrain[i - w]) / (float)Math.max(2, cs * 2);
               float slope = Math.max(-0.11F, Math.min(0.11F, (dhx * 0.45F - dhz * 0.35F) * 0.10F));
               float elev = Math.max(-0.04F, Math.min(0.08F, (terrain[i] - 68) * 0.0012F));
               jitter += slope + elev;
               if (terrain[i] <= 64 && (wet[i - 1] || wet[i + 1] || wet[i - w] || wet[i + w]) && biome[i] != Biomes.ICE_PLAINS && biome[i] != Biomes.ICE_SPIKES) {
                  base = Gen.lerpColor(base, 0xD9CF9E, 0.38);
               } else if (terrain[i] >= 112 && terrain[i] <= 122 && biome[i] == Biomes.MOUNTAINS) {
                  base = Gen.lerpColor(base, 0xEEF4F8, (terrain[i] - 111) / 12.0);
               }
            }
            int topCode = code(pack(base, 1.0F, jitter), wet[i] ? 180 : 255);
            bb = ensure(bb);
            float e = 0.04F * Math.max(1, cs / 8); // a sliver of overlap at the tile's edge: no crack between tiles
            float ex0 = cx == 1 ? x0 - e : x0;
            float ex1 = cx == 32 ? x1 + e : x1;
            float ez0 = cz == 1 ? z0 - e : z0;
            float ez1 = cz == 32 ? z1 + e : z1;
            quadN(bb, n, 0, 1, 0, ex0, y, ez0, ex1, y, ez0, ex1, y, ez1, ex0, y, ez1, topCode);
            for (int d = 0; d < 4; d++) {
               int ni = (cz + dirs[d][1]) * w + cx + dirs[d][0];
               float ny = topY[ni] - (wet[ni] ? 0.2F : sink);
               boolean edge = d == 0 && cx == 1 || d == 1 && cx == 32 || d == 2 && cz == 1 || d == 3 && cz == 32;
               if (!wet[i] || !wet[ni]) {
                  float low;
                  if (ny < y) {
                     low = edge ? ny - Math.min(3.5F, 1.0F + cs * 0.35F) : ny;
                  } else {
                     if (!edge || ny > y + 0.5F) {
                        continue;
                     }

                     low = y - Math.min(3.0F, 0.8F + cs * 0.3F);
                  }

                  int wall;
                  float drop = y - ny;
                  int bm = biome[i];
                  if (wet[i]) {
                     wall = base;
                  } else if (cliff[i] != 0) {
                     wall = drop <= 1.5F ? shade(base, 0.82F) : cliff[i];
                  } else if (bm == Biomes.BADLANDS) {
                     int hy = terrain[i];
                     wall = drop <= 1.5F ? shade(base, 0.84F) : (hy % 7 == 0 ? 0xD1B2A1 : (hy % 5 == 0 ? 0xA15325 : (hy % 3 == 0 ? 0xBA8524 : 0x985F45)));
                  } else if (bm == Biomes.DESERT) {
                     wall = drop <= 2.0F ? shade(base, 0.85F) : 0xD8CB9B;
                  } else if (bm == Biomes.ICE_SPIKES || bm == Biomes.FROZEN_PEAKS) {
                     wall = drop <= 2.0F ? shade(base, 0.86F) : 0x8EB4E6;
                  } else if (bm == 2 || bm == 3 || (bm == 6 && terrain[i] <= 98) || (bm >= Biomes.BIRCH_FOREST && bm <= Biomes.JUNGLE) || bm == Biomes.MEADOW || bm == Biomes.FLOWER_FOREST) {
                     wall = drop <= 1.5F ? shade(base, 0.82F) : (drop <= 5.0F ? 7033395 : 8026746);
                  } else {
                     wall = shade(base, 0.82F);
                  }

                  float face = d < 2 ? 0.72F : 0.84F;
                  int wallCode = code(pack(wall, face, jitter), d == 0 ? 210 : (d == 1 ? 195 : (d == 2 ? 240 : 225)));
                  bb = ensure(bb);
                  switch (d) {
                     case 0:
                        quadN(bb, n, -1, 0, 0, x0, y, z1, x0, y, z0, x0, low, z0, x0, low, z1, wallCode);
                        break;
                     case 1:
                        quadN(bb, n, 1, 0, 0, x1, y, z0, x1, y, z1, x1, low, z1, x1, low, z0, wallCode);
                        break;
                     case 2:
                        quadN(bb, n, 0, 0, -1, x0, y, z0, x1, y, z0, x1, low, z0, x0, low, z0, wallCode);
                        break;
                     default:
                        quadN(bb, n, 0, 0, 1, x1, y, z1, x0, y, z1, x0, low, z1, x1, low, z1, wallCode);
                  }
               }
            }
         }
      }

      Lod.Mb mb = new Lod.Mb();
      mb.bb = bb;
      mb.n = n[0];

      try {
         this.trees(t, mb, villages, sink, terrain, biome, rgb);
         if (t.size <= 512 && villages != null) {
            this.buildings(t, mb, villages);
         }
      } catch (Throwable ex) {
         ex.printStackTrace();
      }

      if (mb.bb != bb) {
         SCRATCH.set(mb.bb);
      }

      bb = mb.bb;
      bb.flip();
      int bytes = bb.limit();
      // Each vertex is 3 floats + 1 packed int = 16 bytes. Use the buffer's real
      // length, not the counter: if a builder ever wrote more bytes than it counted,
      // allocating from the counter would throw BufferOverflowException.
      int verts = bytes / 16;
      ByteBuffer compact = ByteBuffer.allocateDirect(Math.max(16, bytes)).order(ByteOrder.nativeOrder());
      compact.put(bb).flip();
      t.data = compact;
      t.vcount = verts;
      t.state = 2;
      if (!t.retired) this.uploads.add(t);
   }

   List<Village> villagesFor(int var1, int var2, int var3, int var4) {
      int var5 = 320;
      ArrayList var6 = new ArrayList();
      int var7 = Math.floorDiv(var1 - 100, var5);
      int var8 = Math.floorDiv(var3 + 100, var5);
      int var9 = Math.floorDiv(var2 - 100, var5);
      int var10 = Math.floorDiv(var4 + 100, var5);

      for (int var11 = var7; var11 <= var8; var11++) {
         for (int var12 = var9; var12 <= var10; var12++) {
            Village var13 = this.gen.structs.village(var11, var12);
            if (var13.valid && var13.maxX + 2 >= var1 && var13.minX - 2 <= var3 && var13.maxZ + 2 >= var2 && var13.minZ - 2 <= var4) {
               var6.add(var13);
            }
         }
      }

      return var6;
   }

   static int shade(int var0, float var1) {
      return (int)((var0 >> 16 & 0xFF) * var1) << 16 | (int)((var0 >> 8 & 0xFF) * var1) << 8 | (int)((var0 & 0xFF) * var1);
   }

   static void box(Lod.Mb var0, float var1, float var2, float var3, float var4, float var5, float var6, int var7, float var8) {
      if (var5 <= var2) {
         return;
      }
      if (var0.bb.remaining() < 448) {
         ByteBuffer var9 = ByteBuffer.allocateDirect(var0.bb.capacity() * 2).order(ByteOrder.nativeOrder());
         var0.bb.flip();
         var9.put(var0.bb);
         var0.bb = var9;
      }

      ByteBuffer var16 = var0.bb;
      int[] var10 = new int[]{0};
      int var11 = pack(var7, 1.0F, var8);
      int var12 = code(pack(var7, 0.82F, var8), 240);
      int var13 = code(pack(var7, 0.68F, var8), 210);
      int var14 = code(var12, 225);
      int var15 = code(var13, 195);
      quadN(var16, var10, 0, 1, 0, var1, var5, var3, var4, var5, var3, var4, var5, var6, var1, var5, var6, var11);
      quadN(var16, var10, 0, 0, -1, var1, var5, var3, var4, var5, var3, var4, var2, var3, var1, var2, var3, var12);
      quadN(var16, var10, 0, 0, 1, var4, var5, var6, var1, var5, var6, var1, var2, var6, var4, var2, var6, var14);
      quadN(var16, var10, -1, 0, 0, var1, var5, var6, var1, var5, var3, var1, var2, var3, var1, var2, var6, var13);
      quadN(var16, var10, 1, 0, 0, var4, var5, var3, var4, var5, var6, var4, var2, var6, var4, var2, var3, var15);
      var0.n = var0.n + var10[0];
   }

   /** Emits only the 4 outer vertical walls of a box when its top face is completely covered by a wider/equal tier above. */
   static void boxSides(Lod.Mb var0, float var1, float var2, float var3, float var4, float var5, float var6, int var7, float var8) {
      if (var5 <= var2) {
         return;
      }
      if (var0.bb.remaining() < 384) {
         ByteBuffer var9 = ByteBuffer.allocateDirect(var0.bb.capacity() * 2).order(ByteOrder.nativeOrder());
         var0.bb.flip();
         var9.put(var0.bb);
         var0.bb = var9;
      }

      ByteBuffer var16 = var0.bb;
      int[] var10 = new int[]{0};
      int var12 = code(pack(var7, 0.82F, var8), 240);
      int var13 = code(pack(var7, 0.68F, var8), 210);
      int var14 = code(var12, 225);
      int var15 = code(var13, 195);
      quadN(var16, var10, 0, 0, -1, var1, var5, var3, var4, var5, var3, var4, var2, var3, var1, var2, var3, var12);
      quadN(var16, var10, 0, 0, 1, var4, var5, var6, var1, var5, var6, var1, var2, var6, var4, var2, var6, var14);
      quadN(var16, var10, -1, 0, 0, var1, var5, var6, var1, var5, var3, var1, var2, var3, var1, var2, var6, var13);
      quadN(var16, var10, 1, 0, 0, var4, var5, var3, var4, var5, var6, var4, var2, var6, var4, var2, var3, var15);
      var0.n = var0.n + var10[0];
   }

   void trees(Lod.Tile var1, Lod.Mb var2, List<Village> var3, float var4) {
      this.trees(var1, var2, var3, var4, null, null, null);
   }

   /** Colours for the shared tree silhouettes (species-consistent with the real blocks). */
   static int lodLeafColour(int sp, Col col, int cellRgb) {
      switch (sp) {
         case TerraTrees.SP_BIRCH: return shade(8431445, 0.72F);
         case TerraTrees.SP_SPRUCE: return shade(6396257, 0.62F);
         case TerraTrees.SP_JUNGLE: return shade(0x36A022, 0.72F);
         case TerraTrees.SP_ACACIA: return shade(0x889232, 0.72F);
         case TerraTrees.SP_DARK_OAK: return shade(0x3C6E24, 0.72F);
         case TerraTrees.SP_SWAMP: return shade(0x6A7039, 0.70F);
         case TerraTrees.SP_BUSH: return shade(0x5AA655, 0.70F);
         default: return col != null ? Gen.foliageColorFor(col.t, col.hu) : shade(cellRgb, 0.92F);
      }
   }

   static int lodTrunkColour(int sp) {
      switch (sp) {
         case TerraTrees.SP_BIRCH: return 13816512;
         case TerraTrees.SP_SPRUCE: return 3877398;
         case TerraTrees.SP_JUNGLE: return 0x54441B;
         case TerraTrees.SP_ACACIA: return 0x676056;
         case TerraTrees.SP_DARK_OAK: return 0x3B2B17;
         default: return 7034162;
      }
   }

   /**
    * Draw one distant tree from the MEASURED silhouette of the exact species/variant/
    * size tier the world generator used at this coordinate. Bands follow the real crown
    * profile, so umbrellas stay flat, conifers taper and giants are actually taller.
    */
   static void lodTree(Lod.Mb mb, float x, float gy, float z, TerraTrees.LodShape sh,
                       float scale, float pad, boolean drawTrunk, int leafCol, int trunkCol,
                       float jitter, int bands) {
      float bottom = gy + sh.bottom * scale;
      float top = gy + sh.top * scale;
      float hw = sh.wide ? 0.95F : 0.5F;
      if (drawTrunk) {
         boxSides(mb, x - hw, gy - 1.0F, z - hw, x + hw, gy + sh.trunk * scale, z + hw, trunkCol, 0.0F);
      }
      int n = Math.max(3, Math.min(6, bands));
      for (int k = 0; k < n; k++) {
         float f0 = (float)k / n;
         float f1 = (float)(k + 1) / n;
         // A square LOD box of half-width r projects ~1.4x wider than a round crown
         // of radius r; trim the measured radius so the distant outline matches.
         float r = Math.max(0.65F, sh.band((f0 + f1) * 0.5F) * scale * 0.82F + pad);
         float y0 = bottom + (top - bottom) * f0;
         float y1 = bottom + (top - bottom) * f1;
         if (y1 <= y0) y1 = y0 + 0.6F;
         box(mb, x - r, y0, z - r, x + r, y1, z + r, shade(leafCol, 0.86F + 0.24F * f0), jitter);
      }
   }

   void trees(Lod.Tile var1, Lod.Mb var2, List<Village> var3, float var4, int[] gridH, int[] gridBiome, int[] gridRgb) {
      int x0c = Math.floorDiv(var1.x0, 16);
      int x1c = Math.floorDiv(var1.x0 + var1.size - 1, 16);
      int z0c = Math.floorDiv(var1.z0, 16);
      int z1c = Math.floorDiv(var1.z0 + var1.size - 1, 16);
      // Keep the old LOD work budget, but thin chunks by a hash instead of a fixed stride:
      // regular strides leave visible empty stripes in distant forests.
      int chunkStep = var1.size >= 1024 ? 4 : (var1.size == 512 ? 2 : 1);
      float chunkKeep = 1.0F / (chunkStep * chunkStep);
      int treeCap = var1.size >= 512 ? 260 : (var1.size == 256 ? 220 : 160);
      int drawn = 0;
      int bands = var1.size <= 64 ? 6 : (var1.size <= 256 ? 5 : (var1.size <= 512 ? 4 : 3));
      boolean drawTrunk = var1.size <= 128;
      float canopyPad = var1.size >= 1024 ? 1.55F : (var1.size == 512 ? 0.95F : (var1.size == 256 ? 0.45F : 0.0F));
      int cs = Math.max(1, var1.size / 32);
      TerraGen tg = this.gen instanceof TerraGen ? (TerraGen)this.gen : null;
      float treeScale = tg != null ? tg.treeScale : 1.0F;
      boolean giants = tg != null ? tg.giantTrees : true;

      List<Landmarks319.Site> landmarks = null;
      if (var1.size <= 512 && Gen.extras && this.gen.settings.structVer >= 2 && this.gen.settings.terra()) {
         int lrx0 = Math.floorDiv(var1.x0 - 16, Landmarks319.REG);
         int lrx1 = Math.floorDiv(var1.x0 + var1.size + 16, Landmarks319.REG);
         int lrz0 = Math.floorDiv(var1.z0 - 16, Landmarks319.REG);
         int lrz1 = Math.floorDiv(var1.z0 + var1.size + 16, Landmarks319.REG);
         for (int lrx = lrx0; lrx <= lrx1; lrx++) {
            for (int lrz = lrz0; lrz <= lrz1; lrz++) {
               Landmarks319.Site lsite = Landmarks319.plan(this.gen.structs, lrx, lrz);
               if (lsite != null) {
                  if (landmarks == null) landmarks = new ArrayList<>(4);
                  landmarks.add(lsite);
               }
            }
         }
      }

      for (int cx = x0c; cx <= x1c && drawn < treeCap; cx++) {
         for (int cz = z0c; cz <= z1c && drawn < treeCap; cz++) {
            if (chunkKeep < 1.0F && hash(cx, cz) >= chunkKeep) continue;
            // The very same dice the chunk generator rolls: LOD trees now sit exactly
            // where the real ones are instead of being an unrelated decorative forest.
            Random tr = new Random(TerraGen.treeAttemptSeed(this.gen.seed, cx, cz));
            for (int i = 0; i < TerraGen.TREE_ATTEMPTS; i++) {
               int lx = tr.nextInt(16);
               int lz = tr.nextInt(16);
               float p = tr.nextFloat();
               int kind = tr.nextInt(1000);
               long ts = tr.nextLong();
               int wx = cx * 16 + lx;
               int wz = cz * 16 + lz;
               if (p >= 0.63F || wx < var1.x0 || wx >= var1.x0 + var1.size
                  || wz < var1.z0 || wz >= var1.z0 + var1.size) continue;

               int ground;
               int bm;
               int cellRgb = 0x598E34;
               Col col = null;
               if (gridH != null && gridBiome != null) {
                  int gx = 1 + Math.max(0, Math.min(31, (wx - var1.x0) / cs));
                  int gz = 1 + Math.max(0, Math.min(31, (wz - var1.z0) / cs));
                  int gi = gz * 34 + gx;
                  ground = gridH[gi];
                  bm = gridBiome[gi];
                  if (gridRgb != null) cellRgb = gridRgb[gi];
               } else {
                  col = this.gen.column(wx, wz);
                  ground = col.h;
                  bm = col.biome;
               }
               if (ground < 64 || ground > 235 || (var1.size < 64 && this.gen.entrance(wx, wz))) continue;

               if (this.map != null && var1.size <= 256) {
                  // Where the chunk was changed only draw the tree if its trunk is still there.
                  LodMap.Sum sum = this.map.get(wx >> 4, wz >> 4);
                  if (sum != null) {
                     int k = (wz & 15) * 16 + (wx & 15);
                     if (sum.trunk[k] < 3 || sum.depth[k] > 0) continue;
                     ground = sum.top[k] - 1;
                     col = this.gen.column(wx, wz);
                     bm = col.biome;
                  }
               }

               double dens = tg != null ? tg.treeDensityAt(wx, wz) : 1.0;
               int plan = TerraGen.treePlan(bm, p, kind, dens);
               if (plan < 0) {
                  // Deserts have no tree plan; keep their sparse cactus silhouette.
                  if ((bm == Biomes.DESERT || bm == Biomes.BADLANDS) && p < 0.05 && var1.size <= 256) {
                     float px = wx - var1.x0 + 0.5F;
                     float pz = wz - var1.z0 + 0.5F;
                     float gy = ground + 1.0F;
                     box(var2, px - 0.45F, gy - 1.0F, pz - 0.45F, px + 0.45F, gy + 3.0F, pz + 0.45F,
                        0x4A7828, (hash(wx, wz) - 0.5F) * 0.14F);
                  }
                  continue;
               }

               boolean clear = true;
               for (Village vg : var3 == null ? Collections.<Village>emptyList() : var3) {
                  if (!vg.placed.isEmpty()) {
                     for (Village314.Placed pl : vg.placed) {
                        if (wx >= pl.x0 - 3 && wx <= pl.x0 + pl.fw + 2 && wz >= pl.z0 - 3 && wz <= pl.z0 + pl.fd + 2) {
                           clear = false; break;
                        }
                     }
                     if (clear) {
                        for (Village314.Street st : vg.streets) {
                           if (wx >= st.x0 - 2 && wx <= st.x1 + 2 && wz >= st.z0 - 2 && wz <= st.z1 + 2) {
                              clear = false; break;
                           }
                        }
                     }
                  } else {
                     for (Bld bld : vg.blds) {
                        if (wx >= bld.bx0 - 3 && wx <= bld.bx1 + 3 && wz >= bld.bz0 - 3 && wz <= bld.bz1 + 3) {
                           clear = false; break;
                        }
                     }
                  }
                  if (!clear) break;
               }
               if (clear && landmarks != null) {
                  for (int li = 0; li < landmarks.size(); li++) {
                     Landmarks319.Site site = landmarks.get(li);
                     if (wx >= site.x0 - 3 && wx <= site.x0 + site.w + 2 && wz >= site.z0 - 3 && wz <= site.z0 + site.d + 2) {
                        clear = false; break;
                     }
                  }
               }
               if (!clear) continue;

               float px = wx - var1.x0 + 0.5F;
               float pz = wz - var1.z0 + 0.5F;
               float gy = ground + 1.0F;
               float jitter = (hash(wx, wz) - 0.5F) * 0.14F;

               if (plan == TerraGen.P_MUSH_RED || plan == TerraGen.P_MUSH_BROWN) {
                  int mh = 5 + (int)Math.floorMod(ts >>> 11, 3L);
                  drawn++;
                  int capCol = plan == TerraGen.P_MUSH_RED ? 0xB82824 : 0x8A6444;
                  if (drawTrunk) boxSides(var2, px - 0.5F, gy - 1.0F, pz - 0.5F, px + 0.5F, gy + mh - 1.0F, pz + 0.5F, 0xCFC8B8, 0.0F);
                  float mr = 2.5F + canopyPad * 0.6F;
                  box(var2, px - mr, gy + mh - 1.0F, pz - mr, px + mr, gy + mh + 1.2F, pz + mr, capCol, jitter);
                  continue;
               }
               if (plan == TerraGen.P_ICE_SPIKE) {
                  drawn++;
                  int sh2 = 8 + (int)Math.floorMod(ts >>> 13, 3L) * 3;
                  float sr = 1.5F + canopyPad * 0.4F;
                  box(var2, px - sr, gy - 1.0F, pz - sr, px + sr, gy + sh2 * 0.55F, pz + sr, 0x8EB4E6, jitter);
                  box(var2, px - sr * 0.5F, gy + sh2 * 0.55F, pz - sr * 0.5F, px + sr * 0.5F, gy + sh2, pz + sr * 0.5F, 0xA8CEFA, jitter);
                  continue;
               }

               int sp = TerraGen.treeSpecies(plan);
               int variant = TerraGen.treeVariant(plan, ts);
               int tier = TerraGen.treeSizeTier(plan, ts, giants);
               TerraTrees.LodShape shape = TerraTrees.lodShape(sp, variant, tier);
               int leafCol = lodLeafColour(sp, col, cellRgb);
               lodTree(var2, px, gy, pz, shape, treeScale, canopyPad, drawTrunk,
                       leafCol, lodTrunkColour(sp), jitter, bands);
               drawn++;
            }
         }
      }
   }

   static int spR(int var0, int var1) {
      if (var0 < 0) {
         return 0;
      }

      int var2 = Math.min(var1, (var0 + 1) / 2);
      if (var0 % 2 == 0 && var0 > 1) {
         var2--;
      }

      return Math.max(0, var2);
   }

   static void canopy(Lod.Mb var0, float var1, float var2, float var3, float var4, float var5, float var6, int var7, float var8, int var9) {
      int step = Math.max(2, var9);
      int var10 = (int)Math.ceil(var5);
      int var11 = -var10;

      while (var11 < var10) {
         float var12 = var11 + 0.5F * step;
         float var13 = var12 / var5;
         float var14 = 1.0F - var13 * var13;
         if (!(var14 <= 0.05F)) {
            float var15 = (float)Math.sqrt(var14);
            float var16 = (float)Math.floor(var4 * var15) + 0.5F;
            float var17 = (float)Math.floor(var6 * var15) + 0.5F;
            float var18 = 0.84F + 0.22F * (var11 + var10) / (2.0F * var10);
            int nextY = var11 + step;
            boolean topCovered = false;
            if (nextY < var10) {
               float nextMid = (nextY + 0.5F * step) / var5;
               float nextSq = 1.0F - nextMid * nextMid;
               if (nextSq > 0.05F) {
                  float nextS = (float)Math.sqrt(nextSq);
                  float nextX = (float)Math.floor(var4 * nextS) + 0.5F;
                  float nextZ = (float)Math.floor(var6 * nextS) + 0.5F;
                  topCovered = nextX >= var16 && nextZ >= var17;
               }
            }
            if (topCovered) {
               boxSides(var0, var1 - var16, var2 + var11, var3 - var17, var1 + var16, var2 + var11 + step, var3 + var17, shade(var7, var18), var8);
            } else {
               box(var0, var1 - var16, var2 + var11, var3 - var17, var1 + var16, var2 + var11 + step, var3 + var17, shade(var7, var18), var8);
            }
         }

         var11 += step;
      }
   }

   void buildings(Lod.Tile var1, Lod.Mb var2, List<Village> var3) {
      float var4 = var1.x0;
      float var5 = var1.z0;

      for (Village var7 : var3) {
         if (!var7.placed.isEmpty()) {
            for (Village314.Placed p : var7.placed) {
               int x1 = p.x0 + p.fw - 1;
               int z1 = p.z0 + p.fd - 1;
               if (x1 >= var1.x0 - 4 && p.x0 <= var1.x0 + var1.size + 4 && z1 >= var1.z0 - 4 && p.z0 <= var1.z0 + var1.size + 4) {
                  if (this.map != null && this.map.get((p.x0 + x1) / 2 >> 4, (p.z0 + z1) / 2 >> 4) != null) {
                     continue;
                  }
                  renderPlacedBuilding(var2, var4, var5, p);
               }
            }
         } else {
            for (Bld var9 : var7.blds) {
               if (var9.bx1 >= var1.x0 - 4 && var9.bx0 <= var1.x0 + var1.size + 4 && var9.bz1 >= var1.z0 - 4 && var9.bz0 <= var1.z0 + var1.size + 4) {
                  if (this.map != null && this.map.get((var9.bx0 + var9.bx1) / 2 >> 4, (var9.bz0 + var9.bz1) / 2 >> 4) != null) {
                     continue;
                  }
                  int wallCol = var9.type == 2 ? 0x7C7C7C : (var9.desert ? 0xDDD59C : 0x9C7E4E);
                  int roofCol = var9.type == 2 ? 0x686868 : (var9.desert ? 0xD2C38B : 0x7E6238);
                  float bx0 = var9.bx0 - var4;
                  float bz0 = var9.bz0 - var5;
                  float bx1 = var9.bx1 + 1 - var4;
                  float bz1 = var9.bz1 + 1 - var5;
                  boxSides(var2, bx0, var9.y0 - 4, bz0, bx1, var9.y0 + var9.wallH + 1, bz1, wallCol, 0.02F);
                  box(var2, bx0 - 0.5F, var9.y0 + var9.wallH + 1, bz0 - 0.5F, bx1 + 0.5F, var9.y0 + var9.wallH + 3, bz1 + 0.5F, roofCol, 0.03F);
               }
            }
         }
      }

      if (Gen.extras && this.gen.settings.structVer >= 2 && this.gen.settings.terra()) {
         int rx0 = Math.floorDiv(var1.x0 - 16, Landmarks319.REG);
         int rx1 = Math.floorDiv(var1.x0 + var1.size + 16, Landmarks319.REG);
         int rz0 = Math.floorDiv(var1.z0 - 16, Landmarks319.REG);
         int rz1 = Math.floorDiv(var1.z0 + var1.size + 16, Landmarks319.REG);
         for (int rx = rx0; rx <= rx1; rx++) {
            for (int rz = rz0; rz <= rz1; rz++) {
               Landmarks319.Site s = Landmarks319.plan(this.gen.structs, rx, rz);
               if (s != null && s.x0 + s.w >= var1.x0 - 4 && s.x0 <= var1.x0 + var1.size + 4 && s.z0 + s.d >= var1.z0 - 4 && s.z0 <= var1.z0 + var1.size + 4) {
                  if (this.map != null && this.map.get((s.x0 + s.w / 2) >> 4, (s.z0 + s.d / 2) >> 4) != null) {
                     continue;
                  }
                  renderLandmark(var2, var4, var5, s);
               }
            }
         }
      }
   }

   private static void renderPlacedBuilding(Lod.Mb mb, float ox, float oz, Village314.Placed p) {
      Style st = p.st;
      Tpl t = p.tpl;
      String name = t.name;
      float bx0 = p.x0 - ox;
      float bz0 = p.z0 - oz;
      float bx1 = p.x0 + p.fw - ox;
      float bz1 = p.z0 + p.fd - oz;
      float y0 = p.y0;

      int baseCol;
      int wallCol;
      int roofCol;
      int logCol;
      int woolCol = 0xE2DDD2;
      switch (st.kind) {
         case Style.DESERT -> {
            baseCol = 0xD8C994;
            wallCol = 0xDFD2A2;
            roofCol = 0xCBB77B;
            logCol = 0xC4B076;
         }
         case Style.SPRUCE -> {
            baseCol = 0x7A7A7A;
            wallCol = 0x6F5134;
            roofCol = 0x4E3824;
            logCol = 0x3D2A1A;
         }
         case Style.SAVANNA -> {
            baseCol = 0x7A7A7A;
            wallCol = 0xAC5D33;
            roofCol = 0x99502B;
            logCol = 0x686259;
            woolCol = 0xD87F33;
         }
         default -> {
            baseCol = 0x7D7D7D;
            wallCol = 0xA68553;
            roofCol = 0x8C6E42;
            logCol = 0x675132;
         }
      }

      if (name.startsWith("farm")) {
         box(mb, bx0, y0 - 4, bz0, bx1, y0 + 1, bz1, logCol, 0.02F);
         box(mb, bx0 + 1, y0 + 1, bz0 + 1, bx1 - 1, y0 + 1.8F, bz1 - 1, 0x7DC242, 0.03F);
         return;
      }
      if (name.equals("pen")) {
         box(mb, bx0, y0 - 4, bz0, bx1, y0 + 1, bz1, baseCol, 0.02F);
         box(mb, bx0, y0 + 1, bz0, bx1, y0 + 2, bz0 + 1, logCol, 0.02F);
         box(mb, bx0, y0 + 1, bz1 - 1, bx1, y0 + 2, bz1, logCol, 0.02F);
         box(mb, bx0, y0 + 1, bz0, bx0 + 1, y0 + 2, bz1, logCol, 0.02F);
         box(mb, bx1 - 1, y0 + 1, bz0, bx1, y0 + 2, bz1, logCol, 0.02F);
         return;
      }
      if (name.equals("stall")) {
         box(mb, bx0, y0 - 4, bz0, bx1, y0 + 1, bz1, baseCol, 0.02F);
         box(mb, bx0 + 0.5F, y0 + 1, bz0 + 0.5F, bx1 - 0.5F, y0 + 2, bz1 - 0.5F, logCol, 0.02F);
         box(mb, bx0, y0 + 3, bz0, bx1, y0 + 4, bz1, woolCol, 0.03F);
         return;
      }
      if (name.equals("well") || name.equals("gazebo")) {
         box(mb, bx0, y0 - 4, bz0, bx1, y0 + 1, bz1, baseCol, 0.02F);
         box(mb, bx0 + 1, y0 + 1, bz0 + 1, bx1 - 1, y0 + 2, bz1 - 1, baseCol, 0.02F);
         boxSides(mb, bx0 + 1, y0 + 2, bz0 + 1, bx0 + 2, y0 + 5, bz0 + 2, logCol, 0.0F);
         boxSides(mb, bx1 - 2, y0 + 2, bz0 + 1, bx1 - 1, y0 + 5, bz0 + 2, logCol, 0.0F);
         boxSides(mb, bx0 + 1, y0 + 2, bz1 - 2, bx0 + 2, y0 + 5, bz1 - 1, logCol, 0.0F);
         boxSides(mb, bx1 - 2, y0 + 2, bz1 - 2, bx1 - 1, y0 + 5, bz1 - 1, logCol, 0.0F);
         box(mb, bx0 + 0.5F, y0 + 5, bz0 + 0.5F, bx1 - 0.5F, y0 + 6, bz1 - 0.5F, roofCol, 0.03F);
         box(mb, bx0 + 1.5F, y0 + 6, bz0 + 1.5F, bx1 - 1.5F, y0 + 7, bz1 - 1.5F, roofCol, 0.03F);
         return;
      }

      // Houses, shops, church, tower, windmill, inn, bakery
      float wx0 = bx0 + 1.0F;
      float wz0 = bz0 + 1.0F;
      float wx1 = bx1 - 1.0F;
      float wz1 = bz1 - 1.0F;
      int totalH = t.height();
      int wallH = Math.max(4, Math.min(totalH - 3, 8));
      boxSides(mb, wx0, y0 - 4, wz0, wx1, y0 + 2, wz1, baseCol, 0.02F);
      boxSides(mb, wx0, y0 + 2, wz0, wx1, y0 + wallH, wz1, wallCol, 0.02F);

      if (st.kind == Style.DESERT) {
         // Desert flat crenellated roof with terracotta trim
         box(mb, wx0 - 0.3F, y0 + wallH, wz0 - 0.3F, wx1 + 0.3F, y0 + wallH + 1.2F, wz1 + 0.3F, roofCol, 0.03F);
      } else {
         int roofSteps = Math.max(2, Math.min(4, (Math.min(p.fw, p.fd) + 1) / 2));
         boolean alongZ = (p.q & 1) == 0;
         for (int s = 0; s < roofSteps; s++) {
            float rx0 = alongZ ? wx0 - 0.5F + s : wx0 - 0.5F;
            float rx1 = alongZ ? wx1 + 0.5F - s : wx1 + 0.5F;
            float rz0 = alongZ ? wz0 - 0.5F : wz0 - 0.5F + s;
            float rz1 = alongZ ? wz1 + 0.5F : wz1 + 0.5F - s;
            if (rx1 > rx0 && rz1 > rz0) {
               box(mb, rx0, y0 + wallH + s, rz0, rx1, y0 + wallH + s + 1, rz1, roofCol, 0.03F);
            }
         }
      }

      if (name.equals("church") || name.equals("tower") || name.equals("windmill")) {
         float tx0 = (wx0 + wx1) * 0.5F - 2.0F;
         float tz0 = (wz0 + wz1) * 0.5F - 2.0F;
         box(mb, tx0, y0 + wallH, tz0, tx0 + 4.0F, y0 + totalH - 1, tz0 + 4.0F, name.equals("church") ? baseCol : wallCol, 0.02F);
         box(mb, tx0 - 0.5F, y0 + totalH - 1, tz0 - 0.5F, tx0 + 4.5F, y0 + totalH + 0.5F, tz0 + 4.5F, roofCol, 0.03F);
      }
   }

   private static void renderLandmark(Lod.Mb mb, float ox, float oz, Landmarks319.Site s) {
      float x0 = s.x0 - ox;
      float z0 = s.z0 - oz;
      float x1 = s.x0 + s.w - ox;
      float z1 = s.z0 + s.d - oz;
      float y0 = s.y0;
      switch (s.kind) {
         case Landmarks319.K_PYRAMID -> {
            int sand = 0xDDD59C;
            int terra = 0xA15325;
            box(mb, x0, y0 - 4, z0, x1, y0 + 1, z1, sand, 0.02F);
            for (int step = 0; step < 7; step++) {
               box(mb, x0 + step, y0 + 1 + step, z0 + step, x1 - step, y0 + 2 + step, z1 - step, step == 2 || step == 5 ? terra : sand, 0.02F);
            }
            box(mb, x0, y0 + 1, z0, x0 + 4, y0 + 9, z0 + 4, sand, 0.02F);
            box(mb, x1 - 4, y0 + 1, z0, x1, y0 + 9, z0 + 4, sand, 0.02F);
         }
         case Landmarks319.K_JUNGLE -> {
            box(mb, x0, y0 - 4, z0, x1, y0 + 5, z1, 0x627456, 0.03F);
            box(mb, x0 + 2, y0 + 5, z0 + 2, x1 - 2, y0 + 9, z1 - 2, 0x72786E, 0.03F);
         }
         case Landmarks319.K_WITCH -> {
            box(mb, x0 + 1, y0 - 4, z0 + 1, x0 + 2, y0 + 3, z0 + 2, 0x665132, 0.0F);
            box(mb, x1 - 2, y0 - 4, z0 + 1, x1 - 1, y0 + 3, z0 + 2, 0x665132, 0.0F);
            box(mb, x0 + 1, y0 - 4, z1 - 2, x0 + 2, y0 + 3, z1 - 1, 0x665132, 0.0F);
            box(mb, x1 - 2, y0 - 4, z1 - 2, x1 - 1, y0 + 3, z1 - 1, 0x665132, 0.0F);
            box(mb, x0 + 1, y0 + 2, z0 + 1, x1 - 1, y0 + 6, z1 - 1, 0x6E5034, 0.02F);
            box(mb, x0, y0 + 6, z0, x1, y0 + 7.5F, z1, 0x3E2912, 0.03F);
         }
         case Landmarks319.K_IGLOO -> {
            box(mb, x0 + 1, y0 - 3, z0 + 2, x1 - 1, y0 + 4, z1 - 1, 0xF2F7F8, 0.02F);
            box(mb, x0 + 2, y0 + 4, z0 + 3, x1 - 2, y0 + 5, z1 - 2, 0xE6F0F2, 0.02F);
         }
         case Landmarks319.K_OUTPOST -> {
            box(mb, x0 + 1, y0 - 4, z0 + 1, x1 - 1, y0 + 7, z1 - 1, 0x76767A, 0.02F);
            box(mb, x0, y0 + 7, z0, x1, y0 + 11, z1, 0x4C3319, 0.02F);
            box(mb, x0 - 0.5F, y0 + 11, z0 - 0.5F, x1 + 0.5F, y0 + 13, z1 + 0.5F, 0x3B2713, 0.03F);
         }
         default -> {
         }
      }
   }

   static ByteBuffer ensure(ByteBuffer var0) {
      if (var0.remaining() >= 128) {
         return var0;
      }

      ByteBuffer var1 = ByteBuffer.allocateDirect(var0.capacity() * 2).order(ByteOrder.nativeOrder());
      var0.flip();
      var1.put(var0);
      return var1;
   }

   static void v(ByteBuffer var0, float var1, float var2, float var3, int var4) {
      var0.putFloat(var1).putFloat(var2).putFloat(var3).putInt(var4);
   }

   /** A quad whose corners are given in any order; they are written counter-clockwise seen from the side the normal (nx, ny, nz) points to. */
   static void quadN(ByteBuffer b, int[] n, float nx, float ny, float nz, float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, int col) {
      float ax = x1 - x0;
      float ay = y1 - y0;
      float az = z1 - z0;
      float bx = x2 - x0;
      float by = y2 - y0;
      float bz = z2 - z0;
      float dot = (ay * bz - az * by) * nx + (az * bx - ax * bz) * ny + (ax * by - ay * bx) * nz;
      if (dot >= 0.0F) {
         quad(b, n, x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3, col);
      } else {
         quad(b, n, x0, y0, z0, x3, y3, z3, x2, y2, z2, x1, y1, z1, col);
      }
   }

   static void quad(
      ByteBuffer var0,
      int[] var1,
      float var2,
      float var3,
      float var4,
      float var5,
      float var6,
      float var7,
      float var8,
      float var9,
      float var10,
      float var11,
      float var12,
      float var13,
      int var14
   ) {
      v(var0, var2, var3, var4, var14);
      v(var0, var5, var6, var7, var14);
      v(var0, var8, var9, var10, var14);
      v(var0, var11, var12, var13, var14);
      var1[0] += 4;
   }

   void uploadPending(int maxUploads) {
      int var1 = 0;
      long start = System.nanoTime();

      Lod.Tile var2;
      while (var1 < maxUploads && (var1 == 0 || System.nanoTime() - start < 6000000L) && (var2 = (Lod.Tile)this.uploads.poll()) != null) {
         var1++;
         ByteBuffer data = var2.data;
         var2.data = null;
         if (this.dead || var2.retired) {
            continue;                 // a tile evicted while its build was in flight
         } else if (var2.vcount == 0 || data == null) {
            var2.state = 4;
         } else {
            try {
               int vbo = GL15.glGenBuffers();
               GL15.glBindBuffer(34962, vbo);
               GL15.glBufferData(34962, data, 35044);
               GL15.glBindBuffer(34962, 0);
               var2.vbo = vbo;
               var2.count = var2.vcount;
               var2.state = 3;
               var2.readyFrame = this.frame;   // V6.26: start the fade-in clock
               var2.builtFrame = this.frame;
               this.built++;
            } catch (Throwable glError) {
               // One bad VBO must not take the whole frame (and the game) down.
               GL15.glBindBuffer(34962, 0);
               System.err.println("[lod] upload failed at " + var2.x0 + "," + var2.z0 + ": " + glError);
               var2.state = 4;
            }
         }
      }
   }

   static boolean ready(Lod.Tile var0) {
      return var0.state == 3 || var0.state == 4;
   }

   void addDrawOnce(Lod.Tile t) {
      this.addDrawOnce(t, 1.0F);
   }

   void addDrawOnce(Lod.Tile t, float fade) {
      Lod.Tile d = this.refresh(t);
      if (d.lastDrawFrame != this.frame) {
         d.lastDrawFrame = this.frame;
         d.fade = fade;
         this.drawList.add(d);
      } else if (fade > d.fade) {
         d.fade = fade;
      }
   }

   /** V6.26: smooth 0..1 opacity for a tile that has just finished uploading. */
   static float fadeIn(long frame, long readyFrame) {
      if (readyFrame <= 0L) return 1.0F;
      float f = (frame - readyFrame) / (float)FADE_FRAMES;
      if (f >= 1.0F) return 1.0F;
      if (f <= 0.0F) return 0.0F;
      return f * f * (3.0F - 2.0F * f);
   }

   /** V6.26: fade the outermost ring out so culling at the horizon never shows a hard edge. */
   static float distanceFade(double dist, double far) {
      // Full opacity everywhere except the outermost 10% of the LOD horizon: a short
      // render distance must not dim nearby tiles.
      double start = far * 0.90;
      if (dist <= start) return 1.0F;
      return (float)Math.max(0.0, Math.min(1.0, (far - dist) / Math.max(1.0, far - start)));
   }

   void drawFallback(int x0, int z0, int size, float fade) {
      if (size > 32) {
         int half = size >> 1;
         boolean anyChild = false;
         for (int dx = 0; dx <= half; dx += half) {
            for (int dz = 0; dz <= half; dz += half) {
               Lod.Tile ch = (Lod.Tile)this.tiles.get(key(half, x0 + dx, z0 + dz));
               if (ch != null && ready(ch)) {
                  ch.lastUse = this.frame;
                  this.addDrawOnce(ch, fade);
                  anyChild = true;
               }
            }
         }
         if (anyChild) {
            return;
         }
      }
      for (int pSize = size * 2; pSize <= 1024; pSize *= 2) {
         int px0 = Math.floorDiv(x0, pSize) * pSize;
         int pz0 = Math.floorDiv(z0, pSize) * pSize;
         Lod.Tile anc = (Lod.Tile)this.tiles.get(key(pSize, px0, pz0));
         if (anc != null && ready(anc) && this.nearest(px0, pz0, pSize) >= this.innerHalf * 0.7) {
            anc.lastUse = this.frame;
            this.addDrawOnce(anc, fade);
            return;
         }
      }
   }

   public void render(double var1, double var3, double var5, int var7, int var8, float var9) {
      this.ensureTex();
      this.frame++;
      this.cx = var1;
      this.cz = var5;
      this.cy = var3;
      this.maxDist = var7 * 16;
      this.innerHalf = Math.max(0, (var8 - 1) * 16);
      Options lo = this.rend == null || this.rend.g == null ? null : this.rend.g.opt;
      this.fogFarBlocks = fogFarBlocks(this.maxDist, lo);
      this.uploadPending(lo == null ? 8 : Math.max(1, lo.lodMaxUploads));
      this.drawList.clear();
      this.pendingVisible.clear();
      this.pendingBehind.clear();
      int var10 = (int)Math.floor(var1);
      int var11 = (int)Math.floor(var5);
      int var12 = Math.floorDiv(var10 - this.fogFarBlocks, 1024);
      int var13 = Math.floorDiv(var10 + this.fogFarBlocks, 1024);
      int var14 = Math.floorDiv(var11 - this.fogFarBlocks, 1024);
      int var15 = Math.floorDiv(var11 + this.fogFarBlocks, 1024);
      this.rootUsed = 0;
      for (int var17 = var12; var17 <= var13; var17++) {
         for (int var18 = var14; var18 <= var15; var18++) {
            int[] r;
            if (this.rootUsed < this.rootTiles.size()) {
               r = this.rootTiles.get(this.rootUsed);
            } else {
               r = new int[2];
               this.rootTiles.add(r);
            }
            r[0] = var17 * 1024;
            r[1] = var18 * 1024;
            this.rootUsed++;
         }
      }

      this.rootTiles.subList(0, this.rootUsed).sort(this.rootOrder);
      for (int i = 0; i < this.rootUsed; i++) {
         int[] r = this.rootTiles.get(i);
         this.visit(r[0], r[1], 1024);
      }

      int budget = lo == null ? 4 : Math.max(1, lo.lodMaxRequests);
      if (!this.pendingVisible.isEmpty() && this.inflight.get() < this.maxInflight) {
         this.pendingVisible.sort(this.tileOrder);
         for (int i = 0; i < this.pendingVisible.size() && i < budget && this.inflight.get() < this.maxInflight; i++) {
            this.request(this.pendingVisible.get(i));
         }
         budget -= Math.min(budget, this.pendingVisible.size());
      }
      if (budget > 0 && !this.pendingBehind.isEmpty() && this.inflight.get() < this.maxInflight) {
         this.pendingBehind.sort(this.tileOrder);
         for (int i = 0; i < this.pendingBehind.size() && i < budget && this.inflight.get() < this.maxInflight; i++) {
            this.request(this.pendingBehind.get(i));
         }
      }

      GL13.glActiveTexture(33984);
      GL11.glEnable(3553);
      GL11.glBindTexture(3553, this.whiteTex);
      GL11.glTexEnvi(8960, 8704, 34160);
      GL11.glTexEnvi(8960, 34161, 8448);
      GL11.glTexEnvi(8960, 34176, 34168);
      GL11.glTexEnvi(8960, 34177, 34166);
      GL11.glTexEnvi(8960, 34162, 7681);
      GL11.glTexEnvi(8960, 34184, 34168);
      // Update 3.8: the same brightness as the real terrain at full sky light (the lightmap's value for sky 15 is skyLightFactor, which
      // also contains the weather's dimming): no seam between near terrain and LOD at night or in the rain
      float var21 = Math.max(0.06F, Math.min(1.0F, var9));
      this.texEnvColor[0] = this.texEnvColor[1] = this.texEnvColor[2] = var21;
      this.texEnvColor[3] = 1.0F;
      GL11.glTexEnvfv(8960, 8705, this.texEnvColor);
      GL11.glDisable(3008);
      // V6.26: constant-alpha blending gives each tile its own fade-in opacity without
      // touching the vertex colours (works with and without the LOD shader). At fade=1
      // the result is byte-for-byte the old opaque rendering.
      GL11.glEnable(3042);
      GL11.glBlendFunc(32771, 32772); // GL_CONSTANT_ALPHA, GL_ONE_MINUS_CONSTANT_ALPHA
      GL14.glBlendColor(0.0F, 0.0F, 0.0F, 1.0F);
      GL11.glEnableClientState(32884);
      GL11.glEnableClientState(32886);
      GL11.glEnable(32823);
      GL11.glPolygonOffset(1.0F, 8.0F);
      boolean cull = Renderer.cullFaces();
      if (cull) {
         GL11.glEnable(2884); // Update 3.11: every LOD quad is counter-clockwise seen from outside; the ground is see-through from below
      }

      this.drawn = 0;

      int drawCap = lo != null && lo.lodMaxDraws > 0 ? lo.lodMaxDraws : Integer.MAX_VALUE;
      for (Lod.Tile var19 : this.drawList) {
         if (this.drawn >= drawCap) break;
         if (var19.retired || var19.state != 3 || var19.vbo == 0 || var19.count <= 0) continue;
         float fade = var19.fade;
         if (fade <= 0.002F) continue;
         boolean pushed = false;
         try {
            GL14.glBlendColor(0.0F, 0.0F, 0.0F, Math.min(1.0F, fade));
            // A semi-transparent tile must not write depth and hide the tiles behind it.
            boolean fading = fade < 0.998F;
            if (fading) GL11.glDepthMask(false);
            GL15.glBindBuffer(34962, var19.vbo);
            GL11.glVertexPointer(3, 5126, 16, 0L);
            GL11.glColorPointer(4, 5121, 16, 12L);
            GL11.glPushMatrix();
            pushed = true;
            GL11.glTranslated(var19.x0 - var1, -var3, var19.z0 - var5);
            GL11.glDrawArrays(7, 0, var19.count);
            GL11.glPopMatrix();
            pushed = false;
            if (fading) GL11.glDepthMask(true);
            this.drawn++;
            Perf.lodVerts += var19.count;
            int lvl = Math.min(7, Integer.numberOfTrailingZeros(Math.max(32, var19.size) >> 5));
            Perf.lodSizeVerts[lvl] += var19.count;
            Perf.lodSizeTiles[lvl]++;
         } catch (Throwable tileError) {
            // A stale/deleted VBO or a driver error must not kill the whole game:
            // retire just this tile and let it rebuild on a later frame.
            System.err.println("[lod] draw failed at " + var19.x0 + "," + var19.z0 + ": " + tileError);
            if (pushed) { try { GL11.glPopMatrix(); } catch (Throwable ignored) { } }
            GL11.glDepthMask(true);
            var19.state = 4;
            var19.vbo = 0;
         }
      }

      if (cull) {
         GL11.glDisable(2884);
      }

      GL11.glBlendFunc(770, 771);   // restore the game's normal blending
      GL11.glDisable(3042);
      GL14.glBlendColor(0.0F, 0.0F, 0.0F, 1.0F);
      GL11.glDisable(32823);
      GL15.glBindBuffer(34962, 0);
      GL11.glDisableClientState(32886);
      GL11.glDisableClientState(32884);
      GL11.glTexEnvi(8960, 8704, 8448);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      if (this.frame % 120L == 0L) {
         this.evict();
      }
   }

   double nearest(int var1, int var2, int var3) {
      double var4 = Math.max(Math.max(var1 - this.cx, this.cx - (var1 + var3)), 0.0);
      double var6 = Math.max(Math.max(var2 - this.cz, this.cz - (var2 + var3)), 0.0);
      return Math.sqrt(var4 * var4 + var6 * var6);
   }

   boolean insideInner(int var1, int var2, int var3) {
      double var4 = Math.max(Math.abs(var1 - this.cx), Math.abs(var1 + var3 - this.cx));
      double var6 = Math.max(Math.abs(var2 - this.cz), Math.abs(var2 + var3 - this.cz));
      double var8 = this.innerHalf + 24;
      return var4 * var4 + var6 * var6 <= var8 * var8;
   }

   boolean covered(int var1, int var2) {
      return this.coveredBox(var1, var2, 32);
   }

   static boolean coverageCacheReusable(boolean covered, long cachedView, long currentView, long cachedMeshEpoch, long currentMeshEpoch) {
      return !covered || cachedView == currentView && cachedMeshEpoch == currentMeshEpoch;
   }

   boolean coveredBox(int var1, int var2, int size) {
      Lod.Tile var3 = (Lod.Tile)this.tiles.get(key(size, var1, var2));
      World world = this.rend == null || this.rend.g == null ? null : this.rend.g.world;
      long meshEpoch = world == null ? 0L : world.meshCoverageEpoch.get();
      long viewKey = this.rend == null ? 0L : this.rend.meshVisibilityKey;
      if (var3 != null) {
         var3.lastUse = this.frame;
         if (var3.coverFrame != 0L && this.frame - var3.coverFrame < 15L
            && coverageCacheReusable(var3.covered, var3.coverVisibilityKey, viewKey, var3.coverMeshEpoch, meshEpoch)) {
            return var3.covered;
         }
      }

      boolean covered = true;
      int cx0 = var1 >> 4;
      int cx1 = (var1 + size - 1) >> 4;
      int cz0 = var2 >> 4;
      int cz1 = (var2 + size - 1) >> 4;

      for (int cx = cx0; covered && cx <= cx1; cx++) {
         for (int cz = cz0; covered && cz <= cz1; cz++) {
            if (this.rend != null && !this.rend.chunkMeshed(cx, cz)) {
               covered = false;
            }
         }
      }

      if (var3 != null) {
         var3.coverFrame = this.frame;
         var3.covered = covered;
         var3.coverVisibilityKey = viewKey;
         var3.coverMeshEpoch = meshEpoch;
      }
      return covered;
   }

   /**
    * Computes the distance out to which a tile of {@code tileSize} splits into its 4 higher-detail children.
    * Scales dynamically with both chunk render distance ({@code innerHalf}) and LOD horizon ({@code maxDist}) so
    * high-detail 1x1-block (size 32) and 2x2-block (size 64) LOD rings reach much further out.
    */
   double splitDistance(int tileSize) {
      Options lo = this.rend == null || this.rend.g == null ? null : this.rend.g.opt;
      double detail = lo == null ? 1.0 : detailFactor(lo.lodDetail);
      double rdScale = Math.max(1.0, (this.innerHalf + 16.0) / 96.0);
      double lodScale = Math.max(1.0, this.maxDist / 768.0);
      double scale = Math.max(rdScale, rdScale * 0.65 + lodScale * 0.35);
      if (tileSize == 64) {
         // 64 -> 32 (highest-detail 1x1 block LOD): reaches far beyond real chunks and scales with render distance
         return Math.max(this.innerHalf + Math.max(176.0, this.innerHalf * 1.45), 64.0 * 3.1 * scale) * detail;
      }
      if (tileSize == 128) {
         // 128 -> 64 (high-detail 2x2 block LOD with full trees, trunks, and village roofs)
         return Math.max(this.innerHalf + Math.max(288.0, this.innerHalf * 2.1), 128.0 * 2.6 * scale) * detail;
      }
      return Math.max(this.innerHalf + tileSize * 1.2, tileSize * 2.15 * scale) * detail;
   }

   void visit(int var1, int var2, int var3) {
      double var4 = this.nearest(var1, var2, var3);
      if (!(var4 > this.fogFarBlocks)) {
         if (var3 == 32 && this.coveredBox(var1, var2, 32)) {
            return;
         }
         if (var3 <= 128 && this.insideInner(var1, var2, var3) && this.coveredBox(var1, var2, var3)) {
            return;
         }
         boolean inFrustum = this.rend.visible(
            (float)(var1 + var3 / 2 - this.cx),
            (float)(100.0 - this.cy),
            (float)(var2 + var3 / 2 - this.cz),
            var3 * 0.75F + 40.0F
         );
         // V6.26: a 6% hysteresis band around the split distance stops a tile from
         // flipping between its own mesh and its four children on every small camera
         // movement (a classic source of LOD "pops").
         boolean var6 = false;
         if (var3 > 32) {
            double sd = this.splitDistance(var3);
            Lod.Tile node = (Lod.Tile)this.tiles.get(key(var3, var1, var2));
            boolean wasSplit = node != null && node.lastSplit && this.frame - node.splitFrame < 30L;
            var6 = var4 < sd * (wasSplit ? 1.06 : 0.94);
            if (node != null) {
               node.lastSplit = var6;
               node.splitFrame = this.frame;
            }
         }
         if (!var6) {
            Lod.Tile var16 = this.tile(var3, var1, var2);
            var16.lastUse = this.frame;
            if (ready(var16)) {
               if (inFrustum) {
                  float fi = fadeIn(this.frame, var16.readyFrame) * distanceFade(var4, this.fogFarBlocks);
                  this.addDrawOnce(var16, fi);
               }
            } else if (inFrustum) {
               this.pendingVisible.add(var16);
               this.drawFallback(var1, var2, var3, 1.0F);
            } else {
               this.pendingBehind.add(var16);
            }
         } else {
            int var7 = var3 >> 1;
            int sx = this.cx >= var1 + var7 ? var7 : 0;
            int sz = this.cz >= var2 + var7 ? var7 : 0;
            this.visit(var1 + sx, var2 + sz, var7);
            this.visit(var1 + (var7 - sx), var2 + sz, var7);
            this.visit(var1 + sx, var2 + (var7 - sz), var7);
            this.visit(var1 + (var7 - sx), var2 + (var7 - sz), var7);
         }
      }
   }

   void evict() {
      Iterator var1 = this.tiles.entrySet().iterator();

      while (var1.hasNext()) {
         Lod.Tile var2 = (Lod.Tile)((Entry)var1.next()).getValue();
         boolean replIdle = var2.replacement == null || var2.replacement.state == 0 || var2.replacement.state == 3 || var2.replacement.state == 4;
         double dist = this.nearest(var2.x0, var2.z0, var2.size);
         long idle = this.frame - var2.lastUse;
         boolean outOfRange = dist > this.maxDist + 256.0 && idle > 600L;
         boolean staleSplit = idle > 1800L;
         if ((outOfRange || staleSplit) && (var2.state == 3 || var2.state == 4 || var2.state == 0) && replIdle) {
            if (var2.vbo != 0) {
               GL15.glDeleteBuffers(var2.vbo);
            }

            if (var2.replacement != null && var2.replacement.vbo != 0) {
               GL15.glDeleteBuffers(var2.replacement.vbo);
            }

            var2.retired = true;
            if (var2.replacement != null) var2.replacement.retired = true;
            var1.remove();
         }
      }
   }

   /** Marks all cached LOD tiles outdated so they rebuild cleanly (e.g. after switching Resource Packs). */
   public void invalidateAll() {
      long e = this.epoch.incrementAndGet();
      for (Lod.Tile t : this.tiles.values()) {
         t.dirtyEpoch = e;
         t.builtFrame = 0L;
      }
   }

   public void dispose() {
      this.dead = true;
      this.pool.shutdownNow();

      for (Lod.Tile var2 : this.tiles.values()) {
         if (var2.vbo != 0) {
            GL15.glDeleteBuffers(var2.vbo);
         }

         if (var2.replacement != null && var2.replacement.vbo != 0) {
            GL15.glDeleteBuffers(var2.replacement.vbo);
         }
      }

      for (Lod.Tile t : this.tiles.values()) t.retired = true;
      this.tiles.clear();
      this.uploads.clear();
      if (this.map != null && this.map.listener != null) {
         this.map.listener = null;
      }
   }

   // ------------------------------------------------------------------ Update 3.11: tiles follow the world
   /** The summary of chunk (cx, cz) changed: every tile that contains it is outdated and is rebuilt when it is next drawn (not more often than every few seconds). */
   void invalidate(int cx, int cz) {
      long e = this.epoch.incrementAndGet();
      for (int size = 32; size <= 1024; size *= 2) {
         int x0 = Math.floorDiv(cx * 16, size) * size;
         int z0 = Math.floorDiv(cz * 16, size) * size;
         Lod.Tile t = (Lod.Tile)this.tiles.get(key(size, x0, z0));
         if (t != null) {
            t.dirtyEpoch = e;
         }
      }
   }

   /** Frames a tile of this size is left alone after it was built before it is rebuilt for a change (the big ones hardly ever change visibly). */
   static long minAge(int size) {
      return (long)(Math.min(1800, 90L * (size / 32)) * AGE);
   }

   /** -Dbc.lodage=0.05 shortens the rest an outdated tile gets (screenshot scenes only run a couple of hundred frames). */
   static final double AGE = Double.parseDouble(System.getProperty("bc.lodage", "1"));

   /**
    * An outdated tile keeps being drawn while its replacement is built; when that is ready it takes the old one's place in one step, so
    * nothing flickers or is missing in between.
    */
   Lod.Tile refresh(Lod.Tile t) {
      Lod.Tile r = t.replacement;
      if (r != null) {
         if (ready(r)) {
            this.tiles.put(key(t.size, t.x0, t.z0), r);
            t.replacement = null;
            if (t.vbo != 0) {
               GL15.glDeleteBuffers(t.vbo);
               t.vbo = 0;
            }

            t.state = 5;
            t.retired = true;
            r.lastUse = this.frame;
            return r;
         }

         if (r.state == 0) {
            this.request(r);
         }

         return t;
      }

      if (t.dirtyEpoch > t.builtEpoch && this.frame - t.builtFrame >= minAge(t.size) && this.inflight.get() < 6) {
         Lod.Tile n = new Lod.Tile(t.size, t.x0, t.z0);
         n.lastUse = this.frame;
         t.replacement = n;
         this.request(n);
      }

      return t;
   }

   static final class Mb {
      ByteBuffer bb;
      int n;
   }

   static final class Tile {
      final int size;
      final int x0;
      final int z0;
      volatile int state;
      int vbo;
      int count;
      volatile ByteBuffer data;
      volatile int vcount;
      long lastUse;
      long lastDrawFrame;
      long coverFrame;
      boolean covered;
      long coverVisibilityKey;
      long coverMeshEpoch;
      /** Update 3.11: the epoch (see Lod.invalidate) the tile was built at / last found outdated at, the frame it was uploaded, its replacement. */
      volatile long builtEpoch;
      volatile long dirtyEpoch;
      long builtFrame;
      volatile Lod.Tile replacement;
      /** V6.26: frame the tile finished uploading, its per-frame opacity, and split hysteresis. */
      long readyFrame;
      float fade = 1.0F;
      volatile boolean retired;
      boolean lastSplit;
      long splitFrame;

      Tile(int var1, int var2, int var3) {
         this.size = var1;
         this.x0 = var2;
         this.z0 = var3;
      }
   }
}
