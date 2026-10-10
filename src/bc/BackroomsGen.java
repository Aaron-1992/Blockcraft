package bc;

/**
 * Infinite Level 0 Backrooms terrain generator used by {@link BackroomsStalkerMod}.
 * Generates an endless, sealed indoor labyrinth of mono-yellow wallpaper walls,
 * damp yellow carpet floors, pillar halls, dark maintenance corridors, and
 * buzzing fluorescent ceiling lights.
 */
public final class BackroomsGen extends Gen {
   public static final int FLOOR_Y = 64;
   public static final int CEIL_Y = 70;
   public static final int CARPET_BLOCK = Blocks.WOOL0 + 4; // Damp yellow carpet
   public static final int WALL_BASE = Blocks.PLANKS;       // Baseboard trim
   public static final int WALL_UPPER = Blocks.SANDSTONE;   // Mono-yellow wallpaper

   public BackroomsGen(long seed, WorldSettings st) {
      super(seed);
      this.settings = st;
   }

   @Override
   public Gen.Col column(int x, int z) {
      Gen.Col c = new Gen.Col();
      c.t = 0.6;
      c.hu = 0.3;
      c.biome = Biomes.DESERT;
      c.h = CEIL_Y + 1;
      return c;
   }

   @Override
   public int[] findSpawn() {
      return new int[]{0, FLOOR_Y + 1, 0};
   }

   /** Returns true if (wx, wz) should be a wall column at height y (FLOOR_Y+1 .. CEIL_Y-1). */
   public static boolean isWall(long seed, int wx, int y, int wz) {
      // Keep a 7x7 starting room around (0, 0) open
      if (Math.abs(wx) <= 3 && Math.abs(wz) <= 3) {
         return false;
      }
      int cellX = Math.floorDiv(wx, 8);
      int cellZ = Math.floorDiv(wz, 8);
      int lx = Math.floorMod(wx, 8);
      int lz = Math.floorMod(wz, 8);
      int roomHash = mixHash(seed, cellX, cellZ);
      int roomType = Math.floorMod(roomHash, 5);

      // Pillar Hall (repeating 1x1 or 2x2 columns)
      if (roomType == 0) {
         return (lx == 3 || lx == 4) && (lz == 3 || lz == 4);
      }

      boolean eastWall = (lx == 7) && ((roomHash & 1) != 0);
      boolean southWall = (lz == 7) && ((roomHash & 2) != 0);
      // Sub-divider walls inside segmented office rooms
      if (roomType == 1 && lx == 4 && (roomHash & 4) != 0) {
         eastWall = true;
      }
      if (roomType == 2 && lz == 4 && (roomHash & 8) != 0) {
         southWall = true;
      }

      if (!eastWall && !southWall) {
         return false;
      }

      // Carve 2-wide, 3-tall doorways so rooms never become completely sealed boxes
      int dy = y - (FLOOR_Y + 1);
      if (dy < 3) {
         int doorPos = 2 + Math.floorMod(roomHash >> 4, 4);
         if (eastWall && (lz == doorPos || lz == doorPos + 1)) {
            return false;
         }
         if (southWall && (lx == doorPos || lx == doorPos + 1)) {
            return false;
         }
      }
      return true;
   }

   public static boolean isDarkSector(long seed, int wx, int wz) {
      int sx = Math.floorDiv(wx, 32);
      int sz = Math.floorDiv(wz, 32);
      if (sx == 0 && sz == 0) {
         return false;
      }
      return Math.floorMod(mixHash(seed ^ 0x5DEECE66DL, sx, sz), 6) == 0;
   }

   private static int mixHash(long seed, int x, int z) {
      long h = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (z * 0xBF58476D1CE4E5B9L);
      h ^= (h >>> 30);
      h *= 0xBF58476D1CE4E5B9L;
      h ^= (h >>> 27);
      return (int)h;
   }

   @Override
   public Chunk generate(int cx, int cz) {
      Chunk ch = new Chunk(cx, cz);
      for (int i = 0; i < 256; i++) {
         ch.biome[i] = (byte)Biomes.DESERT;
         ch.grassColor[i] = 0xC8B568;
         ch.foliageColor[i] = 0xB8A458;
      }

      for (int lz = 0; lz < 16; lz++) {
         int wz = cz * 16 + lz;
         for (int lx = 0; lx < 16; lx++) {
            int wx = cx * 16 + lx;
            boolean dark = isDarkSector(this.seed, wx, wz);

            ch.blocks[Chunk.idx(lx, 0, lz)] = Blocks.BEDROCK;
            for (int y = 1; y < FLOOR_Y - 1; y++) {
               ch.blocks[Chunk.idx(lx, y, lz)] = Blocks.STONE;
            }
            ch.blocks[Chunk.idx(lx, FLOOR_Y - 1, lz)] = Blocks.BEDROCK;
            // Damp carpet floor (or stonebrick in dark maintenance sectors)
            ch.blocks[Chunk.idx(lx, FLOOR_Y, lz)] = (short)(dark ? Blocks.STONEBRICK : CARPET_BLOCK);

            for (int y = FLOOR_Y + 1; y < CEIL_Y; y++) {
               if (isWall(this.seed, wx, y, wz)) {
                  int wallBlk = dark
                     ? Blocks.COBBLE
                     : (y == FLOOR_Y + 1 ? WALL_BASE : WALL_UPPER);
                  ch.blocks[Chunk.idx(lx, y, lz)] = (short)wallBlk;
               }
            }

            // Ceiling with recessed fluorescent Glowstone panels every 6 blocks
            boolean lightFixture = (Math.floorMod(wx, 6) == 2 || Math.floorMod(wx, 6) == 3)
               && (Math.floorMod(wz, 6) == 2);
            if (lightFixture) {
               ch.blocks[Chunk.idx(lx, CEIL_Y, lz)] = (short)(dark ? Blocks.GLASS : Blocks.GLOWSTONE);
            } else {
               ch.blocks[Chunk.idx(lx, CEIL_Y, lz)] = (short)(dark ? Blocks.STONEBRICK : WALL_UPPER);
            }
            ch.blocks[Chunk.idx(lx, CEIL_Y + 1, lz)] = Blocks.BEDROCK;
         }
      }
      return ch;
   }
}
