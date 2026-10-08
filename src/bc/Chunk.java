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
   /** Update 3.11: set once the chunk's sections have all been meshed at least once - from then on the LOD under it stays hidden. */
   public volatile boolean everMeshed;
   public boolean lit;
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

   /** Update 3.11: section sy has been meshed and uploaded; once all 16 have, the chunk counts as covered for the LOD (see Renderer.chunkMeshed). */
   public void markMeshed(int sy) {
      this.sections[sy].meshed = true;
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
            dst[i] = (short)((hi[i] & 15) << 8 | lo[i] & 255);
         }
      }
   }

   public static int idx(int var0, int var1, int var2) {
      return var1 << 8 | var2 << 4 | var0;
   }

   public int get(int var1, int var2, int var3) {
      return this.blocks[idx(var1, var2, var3)] & 0xFFFF;
   }

   public void set(int var1, int var2, int var3, int var4) {
      this.blocks[idx(var1, var2, var3)] = (short)var4;
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

   public void computeHeightmap() {
      for (int var1 = 0; var1 < 16; var1++) {
         for (int var2 = 0; var2 < 16; var2++) {
            int var3 = 255;

            while (var3 > 0 && Blocks.opacity[this.blocks[idx(var2, var3, var1)] & 0xFFFF] == 0) {
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
      /** Update 3.19: the bushy extra layer of leaves: this many vertices stored after the {@link #nO} ones in the same buffer (drawn near the camera only). */
      public int nX;
      public int nT;
      public boolean dirty = true;
      public boolean inFlight;
      public int version;
      public boolean empty = true;
      /** Update 3.11: has been meshed (and uploaded) at least once. */
      public boolean meshed;
      /** Update 3.18: which faces of the section are connected through open blocks (see CaveCull); open on every side until the section has been meshed. */
      public long conn = CaveCull.ALL;
   }
}
