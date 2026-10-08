package bc;

import java.util.Random;

/** Update 3.13: the Superflat world type: a flat plain of one of four kinds (grass, desert, snow, sandstone) with ores below and villages on top. */
public final class FlatGen extends Gen {
   /** The y of the top block. */
   public static final int LEVEL = 65;
   final int preset;

   public FlatGen(long seed, WorldSettings st) {
      super(seed);
      this.settings = st;
      this.preset = st.flat;
   }

   int biome() {
      return this.preset == 1 || this.preset == 3 ? Biomes.DESERT : (this.preset == 2 ? Biomes.ICE_PLAINS : Biomes.PLAINS);
   }

   @Override
   public Gen.Col column(int x, int z) {
      Gen.Col c = new Gen.Col();
      c.h = LEVEL;
      c.biome = this.biome();
      c.t = this.preset == 1 || this.preset == 3 ? 0.9 : (this.preset == 2 ? 0.05 : 0.5);
      c.hu = this.preset == 1 || this.preset == 3 ? 0.1 : 0.5;
      return c;
   }

   @Override
   public Chunk generate(int cx, int cz) {
      Chunk ch = new Chunk(cx, cz);
      short[] b = ch.blocks;
      int biome = this.biome();
      int top;
      int fill;
      int fd;
      switch (this.preset) {
         case 1:
            top = Blocks.SAND;
            fill = Blocks.SAND;
            fd = 4;
            break;
         case 2:
            top = Blocks.GRASS;
            fill = Blocks.DIRT;
            fd = 3;
            break;
         case 3:
            top = Blocks.SANDSTONE;
            fill = Blocks.SANDSTONE;
            fd = 9;
            break;
         default:
            top = Blocks.GRASS;
            fill = Blocks.DIRT;
            fd = 3;
            break;
      }

      int grass = grassColorFor(this.column(0, 0).t, this.column(0, 0).hu);
      int foliage = foliageColorFor(this.column(0, 0).t, this.column(0, 0).hu);
      int[] hs = new int[256];
      int[] bm = new int[256];
      for (int z = 0; z < 16; z++) {
         for (int x = 0; x < 16; x++) {
            int i = z * 16 + x;
            hs[i] = LEVEL;
            bm[i] = biome;
            ch.biome[i] = (byte)biome;
            ch.grassColor[i] = grass;
            ch.foliageColor[i] = foliage;
            for (int y = 0; y <= LEVEL; y++) {
               int id = 1;
               int d = LEVEL - y;
               if (y == 0) {
                  id = 6;
               } else if (d == 0) {
                  id = top;
               } else if (d <= fd) {
                  id = fill;
               }

               b[Chunk.idx(x, y, z)] = (short)id;
            }

            if (this.preset == 2) {
               b[Chunk.idx(x, LEVEL + 1, z)] = (short)Blocks313.SNOW_LAYER;
            }
         }
      }

      Random vr = new Random(this.seed ^ (cx * 7919L + cz * 104729L) * 1000003L);
      this.vein(ch, vr, 3, 6, 20, 0, 60);
      this.vein(ch, vr, 8, 4, 20, 0, 60);
      this.vein(ch, vr, 19, 20, 14, 0, 60);
      this.vein(ch, vr, 20, 20, 8, 0, 60);
      this.vein(ch, vr, 21, 2, 8, 0, 32);
      this.vein(ch, vr, 23, 8, 7, 0, 16);
      this.vein(ch, vr, 22, 2, 7, 0, 16);
      this.vein(ch, vr, 24, 1, 6, 0, 30);
      if (this.settings.structures) {
         this.structs.apply(ch);
         if (extras) {
            Stronghold.apply(this.seed, ch);
         }
      }

      ch.computeHeightmap();
      return ch;
   }

   @Override
   public int[] findSpawn() {
      return new int[]{0, LEVEL + 1, 0};
   }
}
