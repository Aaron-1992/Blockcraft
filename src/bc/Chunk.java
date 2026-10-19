package bc;

public final class Chunk {
   public static final int H = 256;
   public final int cx;
   public final int cz;
   /** Update 3.12: block ids are 12 bits wide (0..4095), so the array holds shorts (Update 3.5 - 3.11 used one byte per block). */
   public final short[] blocks = new short[65536];
   public final byte[] light = new byte[65536];
   public final int[] hm = new int[256];
   public final int[] grassColor = new int[256];
   public final int[] foliageColor = new int[256];
   public final byte[] biome = new byte[256];
   /** Update 3.13: the tint of the water surface in every column (0xFFFFFF = the plain blue of the picture). */
   public final int[] waterColor = new int[256];
   public volatile boolean modified;
   /** Update 3.11: tick of the last block change since the LOD summary was made (0 = the summary is up to date). */
   public volatile long lodDirty;
   /** True once every section has been meshed at least once; demand meshing may intentionally leave sealed sections unbuilt. */
   public volatile boolean everMeshed;
   /** True after the first section has been processed; lets border updates avoid re-dirtying every hidden section. */
   public volatile boolean hasMeshed;
   public volatile boolean lit;
   public volatile boolean lighting;
   public volatile boolean unloaded;
   public final Chunk.Section[] sections = new Chunk.Section[16];

   public Chunk(int var1, int var2) {
      this.cx = var1;
      this.cz = var2;
      java.util.Arrays.fill(this.waterColor, 0xFFFFFF);

      for (int var3 = 0; var3 < 16; var3++) {
         this.sections[var3] = new Chunk.Section();
      }
   }

   /** Marks one uploaded section; hasMeshed tracks the first upload and all 16 set everMeshed. */
   public void markMeshed(int sy) {
      this.sections[sy].meshed = true;
      this.hasMeshed = true;
      if (!this.everMeshed) {
         boolean all = true;
         for (Chunk.Section sec : this.sections) {
            all &= sec.meshed;
         }

         this.everMeshed = all;
      }
   }

   /** Marks the extension of a chunk file that holds block ids above 255 (see {@link #writeBlocks}). */
   static final int EXT_MAGIC = 0x42435831; // "BCX1"

   /**
    * Update 3.12: the block part of a chunk file. Files of Update 3.5 - 3.11 hold the 65536 block ids as bytes; this format keeps that part
    * unchanged (the low 8 bits of every id, so old files stay readable and an old game still reads the blocks it knows) and appends the
    * high 4 bits as a second array behind a magic number - only when some id is above 255.
    */
   static void writeBlocks(java.io.DataOutputStream out, short[] blocks) throws java.io.IOException {
      byte[] lo = new byte[blocks.length];
      byte[] hi = null;
      for (int i = 0; i < blocks.length; i++) {
         int id = blocks[i] & 0xFFFF;
         lo[i] = (byte)id;
         if (id > 255) {
            if (hi == null) {
               hi = new byte[blocks.length];
            }

            hi[i] = (byte)(id >> 8);
         }
      }

      out.write(lo);
      if (hi != null) {
         out.writeInt(EXT_MAGIC);
         out.write(hi);
      }
   }

   /** The counterpart of {@link #writeBlocks}: reads the blocks of a chunk file written by any version since 3.5 into {@code dst} (65536 ids). */
   static void readBlocks(java.io.DataInputStream in, short[] dst) throws java.io.IOException {
      byte[] lo = new byte[dst.length];
      in.readFully(lo);
      for (int i = 0; i < dst.length; i++) {
         dst[i] = (short)(lo[i] & 255);
      }

      int b0 = in.read();
      if (b0 < 0) {
         return; // a file from before Update 3.12
      }

      int magic = b0 << 24 | in.readUnsignedByte() << 16 | in.readUnsignedByte() << 8 | in.readUnsignedByte();
      if (magic != EXT_MAGIC) {
         return; // unknown trailer: ignore it
      }

      byte[] hi = new byte[dst.length];
      in.readFully(hi);
      for (int i = 0; i < dst.length; i++) {
         if (hi[i] != 0) {
            dst[i] = (short)((hi[i] & 255) << 8 | lo[i] & 255);
         }
      }
   }

   public static int idx(int var0, int var1, int var2) {
      return var1 << 8 | var2 << 4 | var0;
   }

   public int get(int var1, int var2, int var3) {
      return this.blocks[idx(var1, var2, var3)] & BlockState.ID_MASK;
   }

   public void set(int var1, int var2, int var3, int var4) {
      int index = idx(var1, var2, var3);
      short old = this.blocks[index];
      short next = (short)var4;
      if (old == next) return;
      this.blocks[index] = next;
      Chunk.Section section = this.sections[var2 >> 4];
      section.conn = CaveCull.ALL;
      section.connKnown = false;
      int count = this.sections[var2 >> 4].nonAirCount;
      if (count >= 0) {
         if (old == 0) count++;
         if (next == 0) count--;
         this.sections[var2 >> 4].nonAirCount = count;
      }
   }

   /** Rebuilds section occupancy once after a generator, disk load, or direct bulk edit. */
   public void recomputeSectionOccupancy() {
      for (int sy = 0; sy < 16; sy++) {
         int count = 0;
         int start = sy << 12;
         int end = start + 4096;
         for (int i = start; i < end; i++) {
            if (this.blocks[i] != 0) count++;
         }
         this.sections[sy].nonAirCount = count;
         this.sections[sy].conn = CaveCull.ALL;
         this.sections[sy].connKnown = false;
      }
   }

   public int sky(int var1, int var2, int var3) {
      return this.light[idx(var1, var2, var3)] >> 4 & 15;
   }

   public int blk(int var1, int var2, int var3) {
      return this.light[idx(var1, var2, var3)] & 15;
   }

   public void setSky(int var1, int var2, int var3, int var4) {
      int var5 = idx(var1, var2, var3);
      this.light[var5] = (byte)(this.light[var5] & 15 | var4 << 4);
   }

   public void setBlk(int var1, int var2, int var3, int var4) {
      int var5 = idx(var1, var2, var3);
      this.light[var5] = (byte)(this.light[var5] & 240 | var4);
   }

   public boolean isSectionEmpty(int sy) {
      int count = this.sections[sy].nonAirCount;
      if (count >= 0) return count == 0;
      int base = sy << 12;
      int end = base + 4096;
      short[] b = this.blocks;
      for (int i = base; i < end; i++) {
         if (b[i] != 0) return false;
      }
      return true;
   }

   public boolean isChunkEmpty() {
      boolean allKnown = true;
      for (Section section : this.sections) {
         int count = section.nonAirCount;
         if (count < 0) {
            allKnown = false;
         } else if (count != 0) {
            return false;
         }
      }
      if (allKnown) return true;
      short[] b = this.blocks;
      for (int i = 0; i < b.length; i++) {
         if (b[i] != 0) return false;
      }
      return true;
   }

   /**
    * Returns true if every block in section {@code sy} and its 6 adjacent boundary planes is opaque,
    * so the section emits 0 vertices and has CaveCull connectivity 0L.
    */
   public boolean isSectionFullyEnclosedOpaque(World w, int sy) {
      if (sy >= 15 || w == null) {
         return false;
      }
      boolean[] op = Blocks.opaque;
      short[] b = this.blocks;
      int base = sy << 12;
      int end = base + 4096;
      for (int i = base; i < end; i++) {
         if (!op[b[i] & BlockState.ID_MASK]) {
            return false;
         }
      }
      // Check plane immediately above (y = (sy << 4) + 16)
      int topBase = (sy + 1) << 12;
      for (int i = topBase; i < topBase + 256; i++) {
         if (!op[b[i] & BlockState.ID_MASK]) {
            return false;
         }
      }
      // Check plane immediately below (if sy > 0; sy == 0 has solid bedrock below in Mesher)
      if (sy > 0) {
         int botBase = (sy << 12) - 256;
         for (int i = botBase; i < botBase + 256; i++) {
            if (!op[b[i] & BlockState.ID_MASK]) {
               return false;
            }
         }
      }
      Chunk cW = w.chunk(this.cx - 1, this.cz);
      Chunk cE = w.chunk(this.cx + 1, this.cz);
      Chunk cN = w.chunk(this.cx, this.cz - 1);
      Chunk cS = w.chunk(this.cx, this.cz + 1);
      if (cW == null || cE == null || cN == null || cS == null) {
         return false;
      }
      short[] bW = cW.blocks;
      short[] bE = cE.blocks;
      short[] bN = cN.blocks;
      short[] bS = cS.blocks;
      int y0 = sy << 4;
      int y1 = y0 + 16;
      for (int y = y0; y < y1; y++) {
         int yOff = y << 8;
         for (int k = 0; k < 16; k++) {
            if (!op[bW[yOff | (k << 4) | 15] & BlockState.ID_MASK]
               || !op[bE[yOff | (k << 4)] & BlockState.ID_MASK]
               || !op[bN[yOff | (15 << 4) | k] & BlockState.ID_MASK]
               || !op[bS[yOff | k] & BlockState.ID_MASK]) {
               return false;
            }
         }
      }
      return true;
   }

   public void computeHeightmap() {
      for (int var1 = 0; var1 < 16; var1++) {
         for (int var2 = 0; var2 < 16; var2++) {
            int var3 = 255;

            while (var3 > 0 && Blocks.opacity[this.blocks[idx(var2, var3, var1)] & BlockState.ID_MASK] == 0) {
               var3--;
            }

            this.hm[var1 * 16 + var2] = var3;
         }
      }
   }

   public static final class Section {
      public int vboO;
      public int vboT;
      public int nO;
      public int nT;
      public volatile boolean dirty = true;
      public volatile boolean inFlight;
      public int version;
      /** Non-air blocks in this section, or -1 until a generated/loaded chunk has been indexed. */
      public volatile int nonAirCount = -1;
      public boolean empty = true;
      /** Update 3.11: has been meshed (and uploaded) at least once. */
      public boolean meshed;
      /** Update 3.18: which faces of the section are connected through open blocks (see CaveCull). */
      public long conn = CaveCull.ALL;
      /** Connectivity is computed from block data as soon as a chunk is generated; unknown sections stay conservatively open. */
      public volatile boolean connKnown;
   }
}
