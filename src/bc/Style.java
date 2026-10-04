package bc;

/**
 * Update 3.14: what a building is made of. A template ({@link Tpl}) only says "wall", "roof stairs", "planks", "path" ...; the style of the
 * village (oak for plains, sandstone for deserts, acacia, spruce, spruce under snow - the palettes of {@link Biomes#villagePalette}) says
 * which blocks those are. A variant (0..3, chosen per building) changes the roof and the colours a little so that no two houses of a
 * village look alike.
 */
final class Style {
   static final int OAK = 0;
   static final int DESERT = 1;
   static final int SAVANNA = 2;
   static final int SPRUCE = 3;
   static final int SNOW = 4;
   static final String[] NAME = {"oak", "desert", "savanna", "spruce", "snow"};
   private static final Style[][] CACHE = new Style[5][4];

   final int kind;
   final int variant;
   /** The wood of the logs, fences, gates and trapdoors: 0 oak, 1 birch, 2 spruce, 3 jungle, 4 acacia, 5 dark oak. */
   int wood;
   int planks;
   /** The main wall block, the foundation, the floor, the full roof block (ridges, flat roofs). */
   int wall;
   int base;
   int floor;
   int roofBlock;
   /** The stair and slab family of the roof: 0 oak, 1 cobblestone, 2 stone brick, 3 brick, 4 sandstone. */
   int roofMat;
   int path;
   int path2;
   int fence;
   /** The three trim blocks of tokens 1, 2 and 3. */
   int[] acc = new int[3];
   /** The two wool colours (0 white ... 15 black, the order of the wool) of awnings, carpets and banners. */
   int[] wool = new int[2];
   /** The colours the beds of this village come in. */
   int[] beds;
   boolean snow;

   private Style(int kind, int variant) {
      this.kind = kind;
      this.variant = variant;
   }

   static int terracotta(int colour) {
      return 1083 + colour;
   }

   static Style of(int kind, int variant) {
      kind = Math.max(0, Math.min(4, kind));
      variant &= 3;
      Style s = CACHE[kind][variant];
      if (s == null) {
         s = build(kind, variant);
         CACHE[kind][variant] = s;
      }

      return s;
   }

   private static Style build(int kind, int v) {
      Style s = new Style(kind, v);
      switch (kind) {
         case DESERT:
            s.wood = 4;
            s.planks = Blocks313.plank(4);
            s.wall = Blocks.SANDSTONE;
            s.base = Blocks.SANDSTONE;
            s.floor = Blocks.SANDSTONE;
            s.roofBlock = Blocks.SANDSTONE;
            s.roofMat = 4;
            s.path = Blocks.SAND;
            s.path2 = Blocks.SANDSTONE;
            s.fence = Shapes.FENCE0 + 3;
            s.acc = new int[]{terracotta(1), terracotta(0), terracotta(v == 1 ? 11 : 4)};
            s.wool = new int[]{v == 0 ? 1 : (v == 1 ? 0 : (v == 2 ? 4 : 14)), v == 2 ? 11 : 0};
            s.beds = new int[]{0, 1, 4, 14};
            break;
         case SAVANNA:
            s.wood = 4;
            s.planks = Blocks313.plank(4);
            s.wall = Blocks313.plank(4);
            s.base = v == 1 ? Blocks.SANDSTONE : Blocks.COBBLE;
            s.floor = Blocks313.plank(4);
            s.roofBlock = Blocks313.plank(4);
            s.roofMat = v == 3 ? 3 : 1;
            s.path = Blocks313.COARSE_DIRT;
            s.path2 = Blocks.GRAVEL;
            s.fence = Shapes.FENCE0 + 3;
            s.acc = new int[]{Blocks.COBBLE, terracotta(1), Blocks313.plank(5)};
            s.wool = new int[]{v == 0 ? 1 : (v == 1 ? 14 : (v == 2 ? 4 : 11)), 0};
            s.beds = new int[]{1, 4, 14, 0};
            break;
         case SPRUCE:
            s.wood = 2;
            s.planks = Blocks313.plank(2);
            s.wall = Blocks313.plank(2);
            s.base = Blocks.COBBLE;
            s.floor = Blocks313.plank(2);
            s.roofBlock = Blocks313.plank(2);
            s.roofMat = v == 1 ? 0 : (v == 3 ? 2 : 1);
            s.path = Blocks.GRAVEL;
            s.path2 = Blocks313.COARSE_DIRT;
            s.fence = Shapes.FENCE0 + 1;
            s.acc = new int[]{Blocks.COBBLE, Blocks.MOSSY_COBBLE, Blocks313.plank(5)};
            s.wool = new int[]{v == 0 ? 12 : (v == 1 ? 13 : (v == 2 ? 14 : 7)), 0};
            s.beds = new int[]{12, 13, 14, 0, 11};
            break;
         case SNOW:
            s.wood = 2;
            s.planks = Blocks313.plank(2);
            s.wall = Blocks313.plank(2);
            s.base = Blocks.COBBLE;
            s.floor = Blocks313.plank(2);
            s.roofBlock = Blocks313.plank(2);
            s.roofMat = v == 2 ? 0 : 1;
            s.path = Blocks.GRAVEL;
            s.path2 = Blocks.COBBLE;
            s.fence = Shapes.FENCE0 + 1;
            s.acc = new int[]{Blocks.COBBLE, Blocks313.plank(1), Blocks313.plank(5)};
            s.wool = new int[]{v == 0 ? 14 : (v == 1 ? 11 : (v == 2 ? 0 : 3)), 0};
            s.beds = new int[]{14, 11, 0, 3};
            s.snow = true;
            break;
         default:
            s.wood = 0;
            s.planks = Blocks.PLANKS;
            s.wall = Blocks.PLANKS;
            s.base = Blocks.COBBLE;
            s.floor = Blocks.PLANKS;
            s.roofBlock = Blocks.PLANKS;
            s.roofMat = v == 1 ? 1 : (v == 3 ? 3 : 0);
            s.path = Blocks.GRAVEL;
            s.path2 = Blocks.COBBLE;
            s.fence = Blocks.FENCE;
            s.acc = new int[]{Blocks.COBBLE, Blocks313.plank(1), Blocks313.plank(5)};
            s.wool = new int[]{v == 0 ? 14 : (v == 1 ? 11 : (v == 2 ? 4 : 0)), 0};
            s.beds = new int[]{14, 0, 11, 13, 4};
            break;
      }

      return s;
   }

   static int hash(int x, int y, int z, int salt) {
      int h = x * 73856093 ^ y * 19349663 ^ z * 83492791 ^ salt * 0x9E3779B1;
      h ^= h >>> 13;
      h *= 0x5bd1e995;
      return (h ^ h >>> 15) & 0x7FFFFFFF;
   }

   int upright() {
      return Blocks.WOOD_LOG[this.wood];
   }

   /**
    * The block for a template token; -1 leaves the cell alone. dirW is the world direction of oriented tokens (0 north, 1 south, 2 east, 3 west),
    * q the quarter turns the template was rotated by, salt a number that is the same for everything of one building.
    */
   int resolve(char t, int dirW, int q, int wx, int wy, int wz, int salt) {
      switch (t) {
         case '.':
            return 0;
         case '#':
            return this.wall;
         case 'P':
            return this.planks;
         case 'E':
            return this.floor;
         case 'C':
            return this.base;
         case 'c':
            return Blocks.MOSSY_COBBLE;
         case 'S':
            return Blocks.STONEBRICK;
         case 'K':
            return Blocks.BRICKS;
         case 'L':
            return this.upright();
         case 'X':
            return Blocks.sideLog(this.upright(), (q & 1) == 0 ? 0 : 1);
         case 'Z':
            return Blocks.sideLog(this.upright(), (q & 1) == 0 ? 1 : 0);
         case 'R':
            return this.roofBlock;
         case '_':
            return 113 + this.roofMat;
         case '-':
            return Blocks.SLAB_TOP0 + this.roofMat;
         case '^':
         case 'v':
         case '<':
         case '>':
            return 93 + this.roofMat * 4 + dirW;
         case 'G':
            return Shapes.PANE0;
         case 'g':
            return Blocks.GLASS;
         case 'F':
            return this.fence;
         case 'f':
            return Shapes.gate(this.wood, false, dirW);
         case 'T':
            return Blocks.TORCH;
         case '*':
            return dirW < 0 ? Blocks.TORCH : 89 + dirW;
         case '$':
            return Blocks.GLOWSTONE;
         case '!':
            return 73 + dirW + ((hash(wx, wy, wz, salt) & 7) == 0 ? 4 : 0);
         case '|':
            return 81 + dirW + ((hash(wx, wy - 1, wz, salt) & 7) == 0 ? 4 : 0);
         case 'H':
            return dirW < 0 ? 0 : 119 + dirW;
         case 'b':
            return Decor315.foot(this.beds[(hash(wx, wy, wz, salt) >> 3) % this.beds.length], dirW);
         case 'B': {
            int fx = wx - Tpl.DX[dirW];
            int fz = wz - Tpl.DZ[dirW];
            return Decor315.head(this.beds[(hash(fx, wy, fz, salt) >> 3) % this.beds.length], dirW);
         }
         case 'k':
            return Blocks.CRAFTING_TABLE;
         case 'u':
            return Blocks.FURNACE;
         case 'x':
            return Blocks.CHEST_LOOT;
         case 'q':
            return Blocks.BOOKSHELF;
         case 'h':
            return Blocks.HAY_BALE;
         case 'w':
            return 34 + this.wool[0];
         case 'W':
            return 34 + this.wool[1];
         case '+':
            return Shapes.CARPET0 + this.wool[0];
         case ':':
            return Shapes.CARPET0 + this.wool[1];
         case 'o':
            return Blocks.WATER;
         case 'a':
            return Blocks.FARMLAND_WET;
         case 'r': {
            int crop = (salt >> 2) % 3;
            int base = crop == 0 ? Blocks.WHEAT : (crop == 1 ? Blocks.CARROT0 : Blocks.POTATO0);
            int stage = 3 + hash(wx, wy, wz, salt) % 5;
            return base + stage;
         }
         case 'y':
            return Blocks.DIRT;
         case 'm':
            return this.path;
         case 'n':
            return Blocks313.COARSE_DIRT;
         case '1':
            return this.acc[0];
         case '2':
            return this.acc[1];
         case '3':
            return this.acc[2];
         case 'O':
            return Blocks.PUMPKIN;
         case 'z':
            return this.kind == DESERT || this.kind == SNOW ? -1 : Blocks313.FLOWER0 + 1 + hash(wx, 0, wz, salt) % 8;
         default:
            return -1;
      }
   }
}
