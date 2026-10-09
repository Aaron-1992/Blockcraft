package bc;

/**
 * Authentic Minecraft Infdev (June 2010) terrain generator used by {@link InfdevMod}.
 * Features:
 * <ul>
 *   <li>Single-biome radioactive neon-green grass &amp; foliage ({@code 0xFF63D43E})</li>
 *   <li>3D/2D Perlin cliff overhangs, floating islands, and gravel/sand beaches</li>
 *   <li>Iconic solid Infdev Brick Pyramids (including one at X=48, Z=48 near spawn)</li>
 *   <li>Classic Oak trees, Dandelions, Roses, and classic Coal/Iron/Gold/Diamond ores</li>
 * </ul>
 */
public final class InfdevGen extends Gen {
   public static final int INFDEV_GREEN = 0xFF63D43E;
   public static final int PYRAMID_X = 48;
   public static final int PYRAMID_Z = 48;
   public static final int PYRAMID_HALF = 16;

   public InfdevGen(long seed, WorldSettings st) {
      super(seed);
      this.settings = st;
   }

   @Override
   public Gen.Col column(int x, int z) {
      Gen.Col c = new Gen.Col();
      c.t = 0.75;
      c.hu = 0.85;
      c.biome = Biomes.PLAINS;
      double cont = this.nCont.fbm(x * 0.0045, z * 0.0045, 4);
      double ridge = Math.abs(this.nMount.fbm(x * 0.009, z * 0.009, 3));
      double det = this.nDetail.fbm(x * 0.025, z * 0.025, 3);
      int h = (int)Math.round(64.0 + cont * 22.0 + ridge * ridge * 38.0 + det * 7.0);
      int pyrH = pyramidTopAt(x, z);
      if (pyrH > h) {
         h = pyrH;
      }
      c.h = Math.max(12, Math.min(126, h));
      return c;
   }

   /** Returns the top Y of an Infdev Brick Pyramid at (x, z), or -1 if outside any pyramid footprint. */
   public static int pyramidTopAt(int x, int z) {
      // Iconic pyramid near spawn at (48, 48), plus periodic pyramids every 512 blocks
      int dx = Math.abs(x - PYRAMID_X);
      int dz = Math.abs(z - PYRAMID_Z);
      if (dx <= PYRAMID_HALF && dz <= PYRAMID_HALF) {
         int dist = Math.max(dx, dz);
         return 64 + (PYRAMID_HALF - dist);
      }
      int cellX = Math.floorMod(x - 256, 512) - 256;
      int cellZ = Math.floorMod(z - 256, 512) - 256;
      if (Math.abs(cellX) <= PYRAMID_HALF && Math.abs(cellZ) <= PYRAMID_HALF && (Math.abs(x) > 128 || Math.abs(z) > 128)) {
         int dist = Math.max(Math.abs(cellX), Math.abs(cellZ));
         return 68 + (PYRAMID_HALF - dist);
      }
      return -1;
   }

   @Override
   public int[] findSpawn() {
      Gen.Col c = column(0, 0);
      return new int[]{0, Math.max(SEA + 2, c.h + 1), 0};
   }

   @Override
   public Chunk generate(int cx, int cz) {
      Chunk ch = new Chunk(cx, cz);
      for (int i = 0; i < 256; i++) {
         ch.biome[i] = (byte)Biomes.PLAINS;
         ch.grassColor[i] = INFDEV_GREEN;
         ch.foliageColor[i] = INFDEV_GREEN;
      }

      for (int lz = 0; lz < 16; lz++) {
         int wz = cz * 16 + lz;
         for (int lx = 0; lx < 16; lx++) {
            int wx = cx * 16 + lx;
            Gen.Col col = column(wx, wz);
            int surf = col.h;
            int pyrTop = pyramidTopAt(wx, wz);

            ch.blocks[Chunk.idx(lx, 0, lz)] = Blocks.BEDROCK;
            for (int y = 1; y <= Math.max(surf, SEA); y++) {
               if (pyrTop > 0 && y <= pyrTop && y >= 48) {
                  ch.blocks[Chunk.idx(lx, y, lz)] = Blocks.BRICKS;
                  continue;
               }
               if (y <= surf) {
                  // 3D Infdev cliff overhang / arch carving
                  if (y > SEA + 4 && y < surf - 1) {
                     double n3 = this.nCave1.noise(wx * 0.045, y * 0.055, wz * 0.045);
                     if (n3 > 0.52) {
                        continue;
                     }
                  }
                  if (y == surf) {
                     if (surf <= SEA + 1) {
                        // Classic Infdev gravel & sand beaches
                        double bNoise = this.nDetail.noise(wx * 0.05, 0.0, wz * 0.05);
                        ch.blocks[Chunk.idx(lx, y, lz)] = (short)(bNoise > 0.1 ? Blocks.GRAVEL : Blocks.SAND);
                     } else {
                        ch.blocks[Chunk.idx(lx, y, lz)] = Blocks.GRASS;
                     }
                  } else if (y >= surf - 3) {
                     ch.blocks[Chunk.idx(lx, y, lz)] = (short)(surf <= SEA + 1 ? Blocks.SAND : Blocks.DIRT);
                  } else {
                     ch.blocks[Chunk.idx(lx, y, lz)] = Blocks.STONE;
                  }
               } else if (y <= SEA) {
                  ch.blocks[Chunk.idx(lx, y, lz)] = Blocks.WATER;
               }
            }

            // Classic Infdev ores
            int oreHash = (wx * 73428767 ^ wz * 912931 ^ (int)this.seed);
            int oreY = 8 + Math.floorMod(oreHash, 42);
            if (oreY < surf - 4 && ch.blocks[Chunk.idx(lx, oreY, lz)] == Blocks.STONE) {
               int kind = Math.floorMod(oreHash >> 8, 20);
               if (kind == 0 && oreY < 16) {
                  ch.blocks[Chunk.idx(lx, oreY, lz)] = Blocks.DIAMOND_ORE;
               } else if (kind <= 2 && oreY < 32) {
                  ch.blocks[Chunk.idx(lx, oreY, lz)] = Blocks.GOLD_ORE;
               } else if (kind <= 7) {
                  ch.blocks[Chunk.idx(lx, oreY, lz)] = Blocks.IRON_ORE;
               } else if (kind <= 13) {
                  ch.blocks[Chunk.idx(lx, oreY, lz)] = Blocks.COAL_ORE;
               }
            }

            // Classic Dandelions & Red Roses
            if (pyrTop < 0 && surf > SEA + 2 && ch.blocks[Chunk.idx(lx, surf, lz)] == Blocks.GRASS) {
               int fh = (wx * 31337 ^ wz * 65521) & 255;
               if (fh == 1) {
                  ch.blocks[Chunk.idx(lx, surf + 1, lz)] = Blocks.FLOWER_YELLOW;
               } else if (fh == 2) {
                  ch.blocks[Chunk.idx(lx, surf + 1, lz)] = Blocks.FLOWER_RED;
               }
            }
         }
      }

      // Classic Infdev Oak Trees (only inside safe chunk bounds 2..13)
      for (int t = 0; t < 3; t++) {
         int h = (cx * 341873128 + cz * 132897987 + t * 982451653) ^ (int)this.seed;
         int lx = 3 + Math.floorMod(h, 10);
         int lz = 3 + Math.floorMod(h >> 5, 10);
         int wx = cx * 16 + lx;
         int wz = cz * 16 + lz;
         if (pyramidTopAt(wx, wz) > 0) {
            continue;
         }
         int sy = 0;
         for (int y = 126; y > SEA + 1; y--) {
            int id = ch.blocks[Chunk.idx(lx, y, lz)] & 0xFFFF;
            if (id != 0) {
               if (id == Blocks.GRASS) {
                  sy = y;
               }
               break;
            }
         }
         if (sy > SEA + 1 && sy < 115) {
            placeInfdevOak(ch, lx, sy + 1, lz);
         }
      }

      // Recompute heightmap
      for (int z = 0; z < 16; z++) {
         for (int x = 0; x < 16; x++) {
            int top = 0;
            for (int y = 255; y >= 0; y--) {
               int id = ch.blocks[Chunk.idx(x, y, z)] & 0xFFFF;
               if (Blocks.opacity[id] > 0) {
                  top = y;
                  break;
               }
            }
            ch.hm[z * 16 + x] = top;
         }
      }
      return ch;
   }

   private static void placeInfdevOak(Chunk ch, int lx, int y, int lz) {
      for (int dz = -2; dz <= 2; dz++) {
         for (int dx = -2; dx <= 2; dx++) {
            if (Math.abs(dx) == 2 && Math.abs(dz) == 2) {
               continue;
            }
            ch.blocks[Chunk.idx(lx + dx, y + 2, lz + dz)] = Blocks.LEAVES_OAK;
            ch.blocks[Chunk.idx(lx + dx, y + 3, lz + dz)] = Blocks.LEAVES_OAK;
         }
      }
      for (int dz = -1; dz <= 1; dz++) {
         for (int dx = -1; dx <= 1; dx++) {
            if (Math.abs(dx) + Math.abs(dz) <= 1) {
               ch.blocks[Chunk.idx(lx + dx, y + 4, lz + dz)] = Blocks.LEAVES_OAK;
            }
         }
      }
      for (int dy = 0; dy < 4; dy++) {
         ch.blocks[Chunk.idx(lx, y + dy, lz)] = Blocks.LOG_OAK;
      }
   }
}
