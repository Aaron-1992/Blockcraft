package bc;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Update 3.11: what the distant terrain (the LOD) shows for chunks that are not what the terrain generator made of them.
 *
 * <p>The LOD used to be a pure function of the generator, so a house, a pit or a burnt forest was only ever seen from close by. Now every chunk that
 * was changed (a chunk file exists / {@link Chunk#modified}) gets a {@link Sum summary} of its 256 columns - the height and colour of the surface (a
 * real roof, a real hole), the colour of the cliff under it, the depth of the water, the logs of a tree that stands there - and
 * {@link Lod#build} reads that instead of the generator wherever one exists. A summary is made when the chunk is saved (so it survives walking away)
 * and again two seconds after the last change while the chunk is loaded; all of them live in {@code lod.dat} next to the chunk files, and chunk files
 * from before this version (no summary yet) are summarised by a background thread when the world is opened.
 *
 * <p>No OpenGL in here: {@code Tests311} runs it headless.
 */
public final class LodMap {
   /** The 256 columns of one chunk, index z * 16 + x. Immutable once published (a change makes a new one). */
   public static final class Sum {
      /** The plane of the surface: y of its top block + 1 (for water: of the topmost water block + 1). */
      public final short[] top = new short[256];
      /** Water: how many water blocks lie over the bed; 0 = dry land. */
      public final byte[] depth = new byte[256];
      /** Upright logs of a tree that stands on this column (0 = no tree), counted up to 127. */
      public final byte[] trunk = new byte[256];
      /** Colour of the surface 0xRRGGBB (for water the colour of the water over its bed). */
      public final int[] rgb = new int[256];
      /** Colour of the cliff under the surface. */
      public final int[] side = new int[256];
      /** lastModified of the chunk file this was made from; Long.MAX_VALUE = made from the live chunk (not saved yet). */
      public volatile long stamp = Long.MAX_VALUE;
   }

   public interface Listener {
      void changed(int cx, int cz);
   }

   static final int MAGIC = 0x4C4F4431; // "LOD1"
   final ConcurrentHashMap<Long, Sum> sums = new ConcurrentHashMap<>();
   public volatile Listener listener;
   public volatile boolean dirtyFile;
   volatile boolean scanning;
   volatile boolean stop;

   public Sum get(int cx, int cz) {
      return this.sums.get(World.key(cx, cz));
   }

   public int size() {
      return this.sums.size();
   }

   public void put(int cx, int cz, Sum s) {
      this.sums.put(World.key(cx, cz), s);
      this.dirtyFile = true;
      Listener l = this.listener;
      if (l != null) {
         l.changed(cx, cz);
      }
   }

   /** Summarises the live chunk (its blocks as they are now). */
   public Sum update(Chunk c, Gen gen) {
      Sum s = summarize(c.blocks, c.grassColor, c.foliageColor, gen, c.cx, c.cz);
      this.put(c.cx, c.cz, s);
      return s;
   }

   /** The chunk was written to a file that carries this modification time. */
   public void stamp(int cx, int cz, long fileTime) {
      Sum s = this.get(cx, cz);
      if (s != null) {
         s.stamp = fileTime;
         this.dirtyFile = true;
      }
   }

   // ------------------------------------------------------------------ colours
   static volatile int[] topAvg;
   static volatile int[] sideAvg;

   static int avgTile(int tile) {
      int r = 0;
      int g = 0;
      int b = 0;
      int n = 0;
      int x0 = Tex.tx(tile);
      int y0 = Tex.ty(tile);
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            int p = Tex.atlasPixels[(y0 + y) * Tex.PX + x0 + x];
            if (p >>> 24 >= 128) {
               r += p >> 16 & 255;
               g += p >> 8 & 255;
               b += p & 255;
               n++;
            }
         }
      }

      return n == 0 ? -1 : (r / n << 16 | g / n << 8 | b / n);
   }

   /** The average colour of the top (or side) tile of every block, from the atlas; blocks without a tile get a grey. */
   static int[] averages(boolean top) {
      int[] a = top ? topAvg : sideAvg;
      if (a != null) {
         return a;
      }

      synchronized (LodMap.class) {
         a = top ? topAvg : sideAvg;
         if (a == null) {
            a = new int[Blocks.MAX];
            for (int id = 1; id < Blocks.MAX; id++) {
               int[] t = Blocks.tiles[id];
               int c = t == null ? -1 : avgTile(t[top ? 0 : Math.min(2, t.length - 1)]);
               a[id] = c < 0 ? 0x808080 : c;
            }

            if (top) {
               topAvg = a;
            } else {
               sideAvg = a;
            }
         }

         return a;
      }
   }

   static int shade(int c, float f) {
      return (int)((c >> 16 & 255) * f) << 16 | (int)((c >> 8 & 255) * f) << 8 | (int)((c & 255) * f);
   }

   static int waterColor(int depth) {
      float d = Math.min(1.0F, depth / 30.0F);
      return (int)(58.0F * (1.0F - d * 0.6F)) << 16 | (int)(104.0F * (1.0F - d * 0.5F)) << 8 | (int)(208.0F * (1.0F - d * 0.4F));
   }

   /** The colour the LOD paints a block seen from above (grass takes the colour of its biome like the real terrain does). */
   static int topColor(int id, int grass) {
      if (id == Blocks.GRASS || Blocks.tint[id] == 1) {
         return shade(grass, 0.8F);
      }

      if (Blocks.isLava(id)) {
         return 0xD2540C;
      }

      return shade(averages(true)[id], 0.92F);
   }

   // ------------------------------------------------------------------ the summary of a chunk
   /** True for what is not part of the ground when you look at a map: plants, torches, fire, ladders, anything you walk through. */
   static boolean skippable(int id) {
      return id == 0 || !Blocks.solid[id] && !Blocks.isWater(id) && !Blocks.isLava(id);
   }

   /**
    * @param blocks  the 65536 blocks of the chunk
    * @param grass   biome colours of the 256 columns (index z * 16 + x) or null: then the generator is asked
    * @param foliage the same for leaves (unused now: leaves are skipped) or null
    */
   public static Sum summarize(short[] blocks, int[] grass, int[] foliage, Gen gen, int cx, int cz) {
      Sum s = new Sum();
      for (int z = 0; z < 16; z++) {
         for (int x = 0; x < 16; x++) {
            int i = z * 16 + x;
            int y = 255;
            boolean leaves = false;
            int logs = 0;
            int id = 0;
            while (y > 0) {
               id = blocks[Chunk.idx(x, y, z)] & 0xFFFF;
               if (Blocks.isLeaves(id)) {
                  leaves = true; // the crown of a tree: not ground
               } else if (leaves && Blocks.isLog(id)) {
                  logs++; // its trunk (an upright log that has leaves above it)
               } else if (!skippable(id)) {
                  break;
               }

               y--;
            }

            if (Blocks.isWater(id)) {
               int bed = y;
               while (bed > 0 && Blocks.isWater(blocks[Chunk.idx(x, bed, z)] & 0xFFFF)) {
                  bed--;
               }

               int d = Math.max(1, Math.min(127, y - bed));
               s.top[i] = (short)(y + 1);
               s.depth[i] = (byte)d;
               s.rgb[i] = waterColor(d);
               int below = blocks[Chunk.idx(x, bed, z)] & 0xFFFF;
               s.side[i] = shade(averages(true)[below == 0 ? 3 : below], 0.8F);
            } else {
               int g = 0;
               if (id == Blocks.GRASS || Blocks.tint[id] == 1) {
                  g = grass != null ? grass[i] : tint(gen, cx * 16 + x, cz * 16 + z);
               }

               s.top[i] = (short)(y + 1);
               s.rgb[i] = topColor(id, g);
               int below = y >= 2 ? blocks[Chunk.idx(x, y - 2, z)] & 0xFFFF : id;
               if (below == 0 || skippable(below) || Blocks.isWater(below) || Blocks.isLeaves(below)) {
                  below = id;
               }

               s.side[i] = shade(averages(false)[below == Blocks.GRASS ? Blocks.DIRT : below], 0.9F);
            }

            s.trunk[i] = (byte)Math.min(127, logs);
         }
      }

      return s;
   }

   static int tint(Gen gen, int x, int z) {
      if (gen == null) {
         return 0x5B9A3A;
      }

      Gen.Col c = gen.column(x, z);
      return Gen.grassColorFor(c.t, c.hu);
   }

   // ------------------------------------------------------------------ file
   public void save(File dir) {
      if (dir == null || !this.dirtyFile) {
         return;
      }

      this.dirtyFile = false;
      File tmp = new File(dir, "lod.dat.tmp");
      try (DataOutputStream o = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(tmp))))) {
         o.writeInt(MAGIC);
         o.writeInt(this.sums.size());
         for (java.util.Map.Entry<Long, Sum> e : this.sums.entrySet()) {
            long k = e.getKey();
            Sum s = e.getValue();
            o.writeLong(k);
            o.writeLong(s.stamp);
            for (int i = 0; i < 256; i++) {
               o.writeShort(s.top[i]);
               o.writeByte(s.depth[i]);
               o.writeByte(s.trunk[i]);
               o.writeInt(s.rgb[i]);
               o.writeInt(s.side[i]);
            }
         }
      } catch (IOException e) {
         System.err.println("Failed to save lod.dat: " + e);
         this.dirtyFile = true;
         return;
      }

      File f = new File(dir, "lod.dat");
      if (f.exists() && !f.delete()) {
         System.err.println("Failed to replace lod.dat");
      }

      if (!tmp.renameTo(f)) {
         System.err.println("Failed to rename lod.dat.tmp");
      }
   }

   public void load(File dir) {
      File f = dir == null ? null : new File(dir, "lod.dat");
      if (f == null || !f.exists()) {
         return;
      }

      try (DataInputStream in = new DataInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(f))))) {
         if (in.readInt() != MAGIC) {
            return;
         }

         int n = in.readInt();
         for (int k = 0; k < n; k++) {
            long key = in.readLong();
            Sum s = new Sum();
            s.stamp = in.readLong();
            for (int i = 0; i < 256; i++) {
               s.top[i] = in.readShort();
               s.depth[i] = in.readByte();
               s.trunk[i] = in.readByte();
               s.rgb[i] = in.readInt();
               s.side[i] = in.readInt();
            }

            this.sums.put(key, s);
         }
      } catch (IOException | RuntimeException e) {
         System.err.println("Failed to read lod.dat: " + e);
      }
   }

   /**
    * Summarises the chunk files that have no (or an outdated) summary - worlds saved before 3.11, or a crash between the chunk file and lod.dat.
    * Runs on a background thread at low priority; {@code skip} says which chunks are loaded (they are summarised from live data).
    */
   public void scan(File dir, Gen gen, java.util.function.BiPredicate<Integer, Integer> skip) {
      if (dir == null || this.scanning) {
         return;
      }

      this.scanning = true;
      Thread t = new Thread(() -> {
         try {
            scanNow(dir, gen, skip, 2);
         } finally {
            this.scanning = false;
         }
      }, "lod-scan");
      t.setDaemon(true);
      t.setPriority(Thread.MIN_PRIORITY);
      t.start();
   }

   /** The body of {@link #scan}, on the calling thread; returns how many chunks were summarised. */
   int scanNow(File dir, Gen gen, java.util.function.BiPredicate<Integer, Integer> skip, long pauseMs) {
      File[] files = dir.listFiles((d, name) -> name.startsWith("c.") && name.endsWith(".gz"));
      int made = 0;
      if (files != null) {
         short[] blocks = new short[65536];
         for (File f : files) {
            if (this.stop) {
               break;
            }

            String[] p = f.getName().split("\\.");
            if (p.length != 4) {
               continue;
            }

            int cx;
            int cz;
            try {
               cx = Integer.parseInt(p[1]);
               cz = Integer.parseInt(p[2]);
            } catch (NumberFormatException e) {
               continue;
            }

            Sum have = this.get(cx, cz);
            long mod = f.lastModified();
            if (have != null && (have.stamp == Long.MAX_VALUE || mod <= have.stamp + 2000) || skip != null && skip.test(cx, cz)) {
               continue;
            }

            try (DataInputStream in = new DataInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(f))))) {
               Chunk.readBlocks(in, blocks);
            } catch (IOException | RuntimeException e) {
               continue;
            }

            Sum s = summarize(blocks, null, null, gen, cx, cz);
            s.stamp = mod;
            this.put(cx, cz, s);
            made++;
            if (pauseMs > 0) {
               try {
                  Thread.sleep(pauseMs);
               } catch (InterruptedException e) {
                  break;
               }
            }
         }
      }

      return made;
   }
}
