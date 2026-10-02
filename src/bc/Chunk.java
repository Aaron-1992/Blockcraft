package bc;

public final class Chunk {
   public static final int H = 256;
   public final int cx;
   public final int cz;
   public final byte[] blocks = new byte[65536];
   public final byte[] light = new byte[65536];
   public final int[] hm = new int[256];
   public final int[] grassColor = new int[256];
   public final int[] foliageColor = new int[256];
   public final byte[] biome = new byte[256];
   public volatile boolean modified;
   public boolean lit;
   public volatile boolean unloaded;
   public final Chunk.Section[] sections = new Chunk.Section[16];

   public Chunk(int var1, int var2) {
      this.cx = var1;
      this.cz = var2;

      for (int var3 = 0; var3 < 16; var3++) {
         this.sections[var3] = new Chunk.Section();
      }
   }

   public static int idx(int var0, int var1, int var2) {
      return var1 << 8 | var2 << 4 | var0;
   }

   public int get(int var1, int var2, int var3) {
      return this.blocks[idx(var1, var2, var3)] & 0xFF;
   }

   public void set(int var1, int var2, int var3, int var4) {
      this.blocks[idx(var1, var2, var3)] = (byte)var4;
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

            while (var3 > 0 && Blocks.opacity[this.blocks[idx(var2, var3, var1)] & 0xFF] == 0) {
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
      public boolean dirty = true;
      public boolean inFlight;
      public int version;
      public boolean empty = true;
   }
}
