package bc;

/**
 * Update 3.13: the BLOCKCRAFT logo of the title screen. Every lit pixel of the letters is a real block of the game's own atlas: letters built of stone,
 * with grass growing on every surface that faces up. (When the textures change, the logo changes with them.)
 */
final class Logo {
   private Logo() {
   }

   static final String WORD = "BLOCKCRAFT";
   static final int GW = 5;
   static final int GH = 7;
   static final String[][] GLYPH = new String[128][];

   static {
      GLYPH['B'] = new String[]{"XXXX.", "X...X", "X...X", "XXXX.", "X...X", "X...X", "XXXX."};
      GLYPH['L'] = new String[]{"X....", "X....", "X....", "X....", "X....", "X....", "XXXXX"};
      GLYPH['O'] = new String[]{".XXX.", "X...X", "X...X", "X...X", "X...X", "X...X", ".XXX."};
      GLYPH['C'] = new String[]{".XXX.", "X...X", "X....", "X....", "X....", "X...X", ".XXX."};
      GLYPH['K'] = new String[]{"X...X", "X..X.", "X.X..", "XX...", "X.X..", "X..X.", "X...X"};
      GLYPH['R'] = new String[]{"XXXX.", "X...X", "X...X", "XXXX.", "X.X..", "X..X.", "X...X"};
      GLYPH['A'] = new String[]{".XXX.", "X...X", "X...X", "XXXXX", "X...X", "X...X", "X...X"};
      GLYPH['F'] = new String[]{"XXXXX", "X....", "X....", "XXXX.", "X....", "X....", "X...."};
      GLYPH['T'] = new String[]{"XXXXX", "..X..", "..X..", "..X..", "..X..", "..X..", "..X.."};
   }

   /** The logo as a grid of cells (true = a block). */
   static boolean[][] cells() {
      int n = WORD.length();
      boolean[][] grid = new boolean[GH][n * (GW + 1) - 1];
      for (int l = 0; l < n; l++) {
         String[] g = GLYPH[WORD.charAt(l)];
         for (int j = 0; j < GH; j++) {
            for (int i = 0; i < GW; i++) {
               grid[j][l * (GW + 1) + i] = g[j].charAt(i) == 'X';
            }
         }
      }

      return grid;
   }

   static float width(float cell) {
      return (WORD.length() * (GW + 1) - 1) * cell;
   }

   static float height(float cell) {
      return GH * cell;
   }

   /** The biggest cell size (in GUI units) that makes the logo at most maxW wide. */
   static float fit(float maxW, float maxCell) {
      float c = maxW / (WORD.length() * (GW + 1) - 1);
      return Math.max(2.0F, Math.min(maxCell, (float)Math.floor(c * 2.0F) / 2.0F));
   }

   /** Draws the logo centred on cx, its top at y, and returns its bottom. */
   static float draw(Gui gui, float cx, float y, float cell, double time) {
      boolean[][] grid = cells();
      int cols = grid[0].length;
      float x0 = cx - cols * cell / 2.0F;
      int stone = Blocks.tiles[Blocks.STONE][3];
      int cobble = Blocks.tiles[Blocks.COBBLE][3];
      int grass = Blocks.tiles[Blocks.GRASS][3];
      int[] tintRgb = Gui.tintFor(Blocks.GRASS);
      int green = tintRgb[0] << 16 | tintRgb[1] << 8 | tintRgb[2];
      float o = Math.max(0.5F, cell / 8.0F);
      // the shadow on the ground, the dark outline
      for (int j = 0; j < GH; j++) {
         for (int i = 0; i < cols; i++) {
            if (grid[j][i]) {
               gui.rect(x0 + i * cell + cell * 0.35F, y + j * cell + cell * 0.55F, cell, cell, 0x50000000);
            }
         }
      }

      for (int j = 0; j < GH; j++) {
         for (int i = 0; i < cols; i++) {
            if (grid[j][i]) {
               gui.rect(x0 + i * cell - o, y + j * cell - o, cell + 2 * o, cell + 2 * o, 0xFF0C0C10);
            }
         }
      }

      // the blocks
      double sweep = (time * 14.0) % 140.0 - 40.0; // a band of light that runs across the letters every ten seconds
      for (int j = 0; j < GH; j++) {
         for (int i = 0; i < cols; i++) {
            if (!grid[j][i]) {
               continue;
            }

            boolean open = j == 0 || !grid[j - 1][i];
            int tile = open ? grass : ((i * 7 + j * 13) % 5 == 0 ? cobble : stone);
            float px = x0 + i * cell;
            float py = y + j * cell;
            gui.atlasTile(tile, px, py, cell);
            if (open) {
               gui.atlasTile(2, px, py, cell, green); // the grass fringe on top of the dirt
            }
            float shade = 0.06F * j; // a little darker towards the bottom
            if (shade > 0.0F) {
               gui.rect(px, py, cell, cell, (int)(shade * 255.0F) << 24);
            }

            if (j + 1 >= GH || !grid[j + 1][i]) {
               gui.rect(px, py + cell - Math.max(1.0F, cell * 0.12F), cell, Math.max(1.0F, cell * 0.12F), 0x38000000);
            }

            if (i + 1 >= cols || !grid[j][i + 1]) {
               gui.rect(px + cell - Math.max(1.0F, cell * 0.12F), py, Math.max(1.0F, cell * 0.12F), cell, 0x28000000);
            }

            double d = Math.abs((i * 1.0 + j * 2.2) - sweep);
            if (d < 5.0) {
               gui.rect(px, py, cell, cell, (int)((1.0 - d / 5.0) * 70.0) << 24 | 0xFFFFFF);
            }
         }
      }

      return y + GH * cell;
   }
}
