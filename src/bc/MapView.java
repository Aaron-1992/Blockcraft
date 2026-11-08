package bc;

/**
 * Update 6.20: a live minimap in the corner of the HUD and a big, pannable world map on the M key.
 *
 * Both pictures are ordinary {@link Gui.Image}s, so they use exactly the same drawing path in the
 * real OpenGL GUI and in the headless software raster. Colours come from the biome palette of
 * {@link WorldPreview}; loaded chunks supply their real height map, biome and top block, and the
 * generator fills in everything that has not been loaded yet. The big map is filled a few thousand
 * pixels per frame instead of in one burst, so opening it cannot produce a frame spike.
 */
final class MapView {
   static final int MINI = 96;
   static final int BIG = 256;
   static final int[] SIZES = {64, 96, 128};
   /** Blocks per pixel of the full map: 512 .. 16384 blocks across. */
   static final int[] BIG_STEPS = {2, 4, 8, 16, 32, 64};
   static final int UNKNOWN = 0xFF121722;

   final Gui.Image mini = new Gui.Image(MINI, MINI);
   Gui.Image big;
   /** Last full-map rectangle, for click-to-centre. */
   float lastX, lastY, lastSize, lastStep;
   double lastCenterX, lastCenterZ;
   private World lastWorld;
   private int frame;
   private int lastMiniFrame = -1000;
   private int bigCursor;
   private boolean bigDone;
   double bigX, bigZ;
   private int bigStep;
   private boolean bigReady;

   // ------------------------------------------------------------------ lifecycle
   static int step(Game g) {
      int i = g.opt == null ? 2 : Options.clamp(g.opt.bigMapZoom, 0, BIG_STEPS.length - 1);
      return BIG_STEPS[i];
   }

   void open(Game g) {
      if (g.world == null || g.player == null) return;
      if (big == null) big = new Gui.Image(BIG, BIG);
      center(g);
   }

   void center(Game g) {
      if (g.player == null) return;
      bigX = g.player.x;
      bigZ = g.player.z;
      invalidateBig(g);
   }

   void zoom(Game g, int dir) {
      if (g.opt == null) return;
      g.opt.bigMapZoom = Options.clamp(g.opt.bigMapZoom + dir, 0, BIG_STEPS.length - 1);
      invalidateBig(g);
      g.opt.save();
   }

   void pan(Game g, double dx, double dz) {
      bigX += dx;
      bigZ += dz;
      invalidateBig(g);
   }

   private void invalidateBig(Game g) {
      bigStep = step(g);
      bigCursor = 0;
      bigDone = false;
      bigReady = big != null;
      if (big != null) {
         java.util.Arrays.fill(big.px, UNKNOWN);
         big.dirty = true;
      }
   }

   /** True when a click lands on the drawn map picture (buttons are below it). */
   boolean hit(float mx, float my) {
      return lastSize > 0.0F && mx >= lastX && my >= lastY && mx <= lastX + lastSize && my <= lastY + lastSize;
   }

   void click(Game g, float mx, float my) {
      if (lastSize <= 0.0F || lastStep <= 0.0F) return;
      bigX = lastCenterX + (mx - lastX - lastSize * 0.5F) / lastSize * BIG * lastStep;
      bigZ = lastCenterZ + (my - lastY - lastSize * 0.5F) / lastSize * BIG * lastStep;
      invalidateBig(g);
   }

   /** Called once a frame from {@link Game#update(double)}; does nothing while both maps are hidden. */
   void update(Game g) {
      if (g.world == null || g.player == null || g.opt == null) return;
      if (lastWorld != g.world) {
         lastWorld = g.world;
         lastMiniFrame = -1000;
         bigReady = false;
         if (big != null) invalidateBig(g);
      }
      frame++;
      if (g.screen == Game.S.MAP) {
         if (big == null) big = new Gui.Image(BIG, BIG);
         if (!bigReady || bigStep != step(g)) invalidateBig(g);
         refreshBig(g, bigDone ? 256 : 1024);
      }
      boolean hud = !g.hideGui && g.pano == null && (g.screen == Game.S.PLAY || g.screen == Game.S.CHAT);
      if (hud && g.opt.minimap) {
         int period = g.opt.minimapRotate ? 1 : Math.max(1, Options.clamp(g.opt.mapRefresh, 1, 20));
         if (frame - lastMiniFrame >= period) {
            refreshMini(g);
            lastMiniFrame = frame;
         }
      }
   }

   // ------------------------------------------------------------------ picture data
   /** The colour of one block column, from live chunk data when it is loaded, otherwise from the generator. */
   static int colorAt(World w, Gen gen, int bx, int bz) {
      Chunk c = w == null ? null : w.chunk(bx >> 4, bz >> 4);
      int unloadedAlt = -1;
      float unloadedBlend = 0.0F;
      Gen.Col col = null;
      int h, biome, top = 0;
      float relief = 0.0F;
      if (c != null && c.lit) {
         int ix = bx & 15;
         int iz = bz & 15;
         int idx = iz * 16 + ix;
         h = c.hm[idx];
         biome = c.biome[idx] & 255;
         if (h >= 0 && h < 256) top = c.get(ix, h, iz);
         int hE = c.hm[iz * 16 + Math.min(15, ix + 1)];
         int hS = c.hm[Math.min(15, iz + 1) * 16 + ix];
         relief = (hE - hS) * 0.02F;
      } else {
         if (gen == null) return UNKNOWN;
         try {
            col = gen.column(bx, bz);
         } catch (RuntimeException e) {
            return UNKNOWN;
         }
         h = col.h;
         biome = col.biome;
      }

      int base = WorldPreview.color(biome);
      // Unloaded columns blend toward the neighbouring biome across a border band; loaded
      // columns already have their real (dithered) surface colour from the chunk.
      if (c == null || !c.lit) {
         if (unloadedAlt >= 0 && unloadedBlend > 0.0F) {
            base = WorldPreview.mix(base, WorldPreview.color(unloadedAlt), unloadedBlend * 0.45F);
         }
      }

      if (Blocks.isWater(top)) {
         base = 0x2F55B0;
      } else if (top == Blocks.SAND || top == Blocks.SANDSTONE) {
         base = 0xE2D69C;
      } else if (top == Blocks.SNOW || (h > 150 + Gen.depth() && biome != Biomes.DESERT && biome != Biomes.BADLANDS)) {
         base = 0xEAF3F7;
      } else if (top == Blocks.STONE) {
         base = 0x8E8E8E;
      }

      float shade;
      if (h < Gen.SEA) {
         shade = 1.0F - Math.min(0.45F, (Gen.SEA - h) / 70.0F);
      } else {
         shade = Math.max(0.62F, Math.min(1.35F, 0.92F + (h - Gen.SEA) / 220.0F + relief));
      }
      int d = ((bx ^ bz) & 1) == 0 ? 3 : -3;
      int r = byteOf((base >> 16 & 255) * shade + d);
      int gr = byteOf((base >> 8 & 255) * shade + d);
      int b = byteOf((base & 255) * shade + d);
      return 0xFF000000 | r << 16 | gr << 8 | b;
   }

   private static int byteOf(float v) {
      return v < 0.0F ? 0 : v > 255.0F ? 255 : (int)(v + 0.5F);
   }

   private void refreshMini(Game g) {
      World w = g.world;
      int radius = Options.clamp(g.opt.minimapRadius, 32, 512);
      int step = Math.max(1, 2 * radius / MINI);
      double px = g.player.x;
      double pz = g.player.z;
      boolean rotate = g.opt.minimapRotate;
      double rad = Math.toRadians(g.player.yaw);
      double fx = -Math.sin(rad), fz = Math.cos(rad);
      double rx = Math.cos(rad), rz = Math.sin(rad);
      for (int j = 0; j < MINI; j++) {
         for (int i = 0; i < MINI; i++) {
            double sx = (i - MINI / 2) * step;
            double sy = (j - MINI / 2) * step;
            double dx = rotate ? sx * rx - sy * fx : sx;
            double dz = rotate ? sx * rz - sy * fz : sy;
            int bx = (int)Math.floor(px + dx);
            int bz = (int)Math.floor(pz + dz);
            mini.px[j * MINI + i] = colorAt(w, w.gen, bx, bz);
         }
      }
      mini.dirty = true;
   }

   private void refreshBig(Game g, int budget) {
      if (big == null) return;
      World w = g.world;
      Gen gen = w.gen;
      int n = BIG * BIG;
      for (int k = 0; k < budget; k++) {
         int idx = bigCursor;
         int i = idx % BIG;
         int j = idx / BIG;
         int bx = (int)Math.floor(bigX + (i - BIG / 2) * bigStep);
         int bz = (int)Math.floor(bigZ + (j - BIG / 2) * bigStep);
         big.px[idx] = colorAt(w, gen, bx, bz);
         bigCursor++;
         if (bigCursor >= n) {
            bigCursor = 0;
            bigDone = true;
         }
      }
      big.dirty = true;
   }

   // ------------------------------------------------------------------ markers
   /** A filled triangle pointing along (ux,uy), drawn as one-GUI-unit squares so the software raster sees it too. */
   private static void marker(Gui gui, float cx, float cy, double ux, double uy, float r, int outline, int fill) {
      triangle(gui, cx, cy, ux, uy, r + 1.4F, outline);
      triangle(gui, cx, cy, ux, uy, r, fill);
   }

   private static void triangle(Gui gui, float cx, float cy, double ux, double uy, float r, int color) {
      double vx = -uy, vy = ux;
      double ax = cx + ux * r * 1.5, ay = cy + uy * r * 1.5;
      double bx = cx - ux * r * 0.8 + vx * r * 0.85, by = cy - uy * r * 0.8 + vy * r * 0.85;
      double dx = cx - ux * r * 0.8 - vx * r * 0.85, dy = cy - uy * r * 0.8 - vy * r * 0.85;
      double x0 = Math.min(ax, Math.min(bx, dx)), x1 = Math.max(ax, Math.max(bx, dx));
      double y0 = Math.min(ay, Math.min(by, dy)), y1 = Math.max(ay, Math.max(by, dy));
      double den = (by - dy) * (ax - dx) + (dx - bx) * (ay - dy);
      if (Math.abs(den) < 1.0E-6) return;
      for (float y = (float)y0; y <= y1; y += 1.0F) {
         for (float x = (float)x0; x <= x1; x += 1.0F) {
            double a = ((by - dy) * (x - dx) + (dx - bx) * (y - dy)) / den;
            double b = ((dy - ay) * (x - dx) + (ax - dx) * (y - dy)) / den;
            double c = 1.0 - a - b;
            if (a >= -0.02 && b >= -0.02 && c >= -0.02) gui.rect(x, y, 1.05F, 1.05F, color);
         }
      }
   }

   /** Direction the player arrow points on the picture (north-up vs rotating). */
   private static void arrow(double[] out, Game g, boolean rotate) {
      if (rotate) {
         out[0] = 0.0;
         out[1] = -1.0;
      } else {
         double rad = Math.toRadians(g.player.yaw);
         out[0] = -Math.sin(rad);
         out[1] = Math.cos(rad);
      }
   }

   // ------------------------------------------------------------------ HUD minimap
   /** Update 6.22: four corners for the HUD minimap. */
   static final String[] POS_NAME = {"Top Right", "Top Left", "Bottom Right", "Bottom Left"};
   /** Height of the advancement toast (Adv312.drawToast draws a 32px box at y=4). */
   static final float TOAST_H = 38.0F;

   /**
    * Update 6.22: where the minimap goes, and where it slides to while an advancement toast is
    * showing. The toast is in the top-right corner, so a top-right minimap moves down below it;
    * other corners are already clear. Pure so the headless screen tests can assert the behaviour.
    */
   static float[] miniOrigin(float gw, float gh, float size, int pos, boolean toast, boolean autoMove, float[] out) {
      int p = Options.clamp(pos, 0, 3);
      float x = p == 0 || p == 2 ? gw - size - 6.0F : 6.0F;
      float y = p == 0 || p == 1 ? 6.0F : gh - size - 24.0F;
      if (toast && autoMove && p == 0) {
         y += TOAST_H;
         if (y + size + 14.0F > gh) {
            y = Math.max(6.0F, gh - size - 24.0F);
         }
      }

      out[0] = x;
      out[1] = y;
      return out;
   }

   void drawMini(Game g) {
      Gui gui = g.gui;
      Options o = g.opt;
      if (o == null || !o.minimap || g.hideGui || g.pano != null || g.world == null || g.player == null) return;
      if (g.screen != Game.S.PLAY && g.screen != Game.S.CHAT) return;
      float size = SIZES[Options.clamp(o.minimapSize, 0, SIZES.length - 1)];
      boolean toast = g.adv != null && g.adv.shown != null;
      float[] at = miniOrigin(gui.w, gui.h, size, o.minimapPos, toast, o.minimapAutoMove, new float[2]);
      float x = at[0];
      float y = at[1];
      gui.rect(x - 2.0F, y - 2.0F, size + 4.0F, size + 4.0F, 0xD00A0E16);
      gui.rect(x - 1.0F, y - 1.0F, size + 2.0F, size + 2.0F, 0xFF7C8BA8);
      gui.image(mini, x, y, size, size);
      gui.rect(x, y, size, 1.0F, 0x60FFFFFF);
      gui.text("N", x + 2.0F, y + 2.0F, 0xFFFFFFFF, true);
      double[] dir = new double[2];
      arrow(dir, g, o.minimapRotate);
      double rad = Math.toRadians(g.player.yaw);
      double rx = Math.cos(rad), rz = Math.sin(rad);
      double fx = -Math.sin(rad), fz = Math.cos(rad);
      int radius = Options.clamp(o.minimapRadius, 32, 512);
      int step = Math.max(1, 2 * radius / MINI);
      double scale = size / (MINI * (double)step);
      if (o.minimapMobs) {
         int n = 0;
         for (Mobs.Mob m : g.mobs.list) {
            if (m == null || m.dead || n++ >= 64) continue;
            double dx = m.x - g.player.x, dz = m.z - g.player.z;
            double sx = o.minimapRotate ? dx * rx + dz * rz : dx;
            double sy = o.minimapRotate ? -(dx * fx + dz * fz) : dz;
            float ux = (float)(x + size * 0.5 + sx * scale);
            float uy = (float)(y + size * 0.5 + sy * scale);
            if (ux >= x && ux <= x + size - 2.0F && uy >= y && uy <= y + size - 2.0F) {
               int col = Mobs.hostile(m.type) ? 0xFFFF5A50 : 0xFF8FE060;
               gui.rect(ux - 1.0F, uy - 1.0F, 3.0F, 3.0F, col);
            }
         }
      }
      marker(gui, x + size * 0.5F, y + size * 0.5F, dir[0], dir[1], 5.0F, 0xFF101018, 0xFFFFFFFF);
      String pos = "X " + (int)Math.floor(g.player.x) + "  Z " + (int)Math.floor(g.player.z);
      gui.text(Game.clipText(pos, size + 20.0F), x - 1.0F, y + size + 3.0F, 0xFFE8EEF8, true);
   }

   // ------------------------------------------------------------------ full map screen
   static void screen(Game g) {
      Ui ui = g.ui;
      Gui gui = g.gui;
      MapView map = g.map;
      Options o = g.opt;
      ui.begin();
      gui.rect(0.0F, 0.0F, gui.w, gui.h, 0xE0070A12);
      float panelW = Math.min(430.0F, gui.w - 16.0F);
      float panelH = Math.min(430.0F, gui.h - 16.0F);
      float px = (gui.w - panelW) * 0.5F;
      float py = (gui.h - panelH) * 0.5F;
      ui.panel(px, py, panelW, panelH);
      ui.title("WORLD MAP", py + 4.0F);
      if (map.big == null) map.big = new Gui.Image(BIG, BIG);
      map.invalidateIfStale(g);
      int step = step(g);
      float footY = py + panelH - 24.0F;
      float hintY = footY - 20.0F;
      float b2 = hintY - 22.0F;
      float b1 = b2 - 22.0F;
      float mapTop = py + 28.0F;
      float mapSize = Math.max(48.0F, Math.min(panelW - 20.0F, b1 - mapTop - 16.0F));
      float mx = (gui.w - mapSize) * 0.5F;
      float my = mapTop;
      map.lastX = mx;
      map.lastY = my;
      map.lastSize = mapSize;
      map.lastStep = step;
      map.lastCenterX = map.bigX;
      map.lastCenterZ = map.bigZ;

      gui.rect(mx - 2.0F, my - 2.0F, mapSize + 4.0F, mapSize + 4.0F, 0xFF6B7B96);
      gui.image(map.big, mx, my, mapSize, mapSize);
      if (o.mapGrid) {
         for (int k = 1; k < 4; k++) {
            float t = mapSize * k / 4.0F;
            gui.rect(mx + t, my, 1.0F, mapSize, 0x30FFFFFF);
            gui.rect(mx, my + t, mapSize, 1.0F, 0x30FFFFFF);
         }
      }
      gui.text("N", mx + 3.0F, my + 3.0F, 0xFFFFFFFF, true);
      double[] dir = new double[2];
      arrow(dir, g, false);
      double toU = mapSize / (BIG * (double)step);
      if (o.minimapMobs) {
         int n = 0;
         for (Mobs.Mob m : g.mobs.list) {
            if (m == null || m.dead || n++ >= 96) continue;
            float ux = (float)(mx + mapSize * 0.5 + (m.x - map.bigX) * toU);
            float uy = (float)(my + mapSize * 0.5 + (m.z - map.bigZ) * toU);
            if (ux >= mx && ux <= mx + mapSize - 2.0F && uy >= my && uy <= my + mapSize - 2.0F) {
               gui.rect(ux - 2.0F, uy - 2.0F, 5.0F, 5.0F, 0x60000000);
               gui.rect(ux - 1.0F, uy - 1.0F, 3.0F, 3.0F, Mobs.hostile(m.type) ? 0xFFFF5A50 : 0xFF8FE060);
            }
         }
      }
      float ux = (float)(mx + mapSize * 0.5 + (g.player.x - map.bigX) * toU);
      float uy = (float)(my + mapSize * 0.5 + (g.player.z - map.bigZ) * toU);
      if (ux >= mx && ux <= mx + mapSize && uy >= my && uy <= my + mapSize) {
         marker(gui, ux, uy, dir[0], dir[1], 6.0F, 0xFF101018, 0xFFFFE060);
      }
      String info = "X " + (int)Math.floor(g.player.x) + "  Z " + (int)Math.floor(g.player.z)
         + "   scale " + step + " m/px   centre " + (int)Math.floor(map.bigX) + ", " + (int)Math.floor(map.bigZ);
      gui.centered(Game.clipText(info, panelW - 12.0F), gui.w * 0.5F, my + mapSize + 2.0F, 0xFFC4D0E4, true, 1.0F);

      float gap = 4.0F;
      float bw = (panelW - 8.0F - gap * 2.0F) / 3.0F;
      ui.button("Zoom In", px + 4.0F, b1, bw, 20.0F, true, "One step closer: half as many blocks per pixel.", () -> map.zoom(g, -1));
      ui.button("Zoom Out", px + 4.0F + bw + gap, b1, bw, 20.0F, true, "One step further out: twice as many blocks per pixel.", () -> map.zoom(g, 1));
      ui.button("Center", px + 4.0F + 2.0F * (bw + gap), b1, bw, 20.0F, true, "Put the player back in the middle of the map.", () -> map.center(g));
      ui.toggle("Grid", o.mapGrid, px + 4.0F, b2, bw, 20.0F, "Coordinate grid lines over the map.", () -> o.mapGrid = !o.mapGrid);
      ui.toggle("Mobs", o.minimapMobs, px + 4.0F + bw + gap, b2, bw, 20.0F, "Show nearby creatures as dots.", () -> o.minimapMobs = !o.minimapMobs);
      ui.button("Done", px + 4.0F + 2.0F * (bw + gap), b2, bw, 20.0F, true, "Close the map (M or Esc).", () -> g.setScreen(Game.S.PLAY));
      ui.drawHint(px + 4.0F, hintY, panelW - 8.0F, 18.0F,
         "M / Esc close  -  WASD or arrows pan  -  wheel or +/- zoom  -  click the map to centre there.");
      ui.drawToast();
   }

   private void invalidateIfStale(Game g) {
      if (!bigReady || bigStep != step(g)) invalidateBig(g);
   }
}
