package bc;

/**
 * Update 3.13: a small map of what a world will look like (seed + world type), drawn from the generator's columns: the Create New World screen shows it
 * while you type the seed, and the world list shows one for every saved world. Computing it costs a few milliseconds (a column is about 3 microseconds).
 */
final class WorldPreview {
   /** Map colours of the biomes (index = biome id). */
   static final int[] COL = new int[Biomes.N];

   static {
      int[] c = COL;
      c[Biomes.OCEAN] = 0x2F55B0;
      c[Biomes.BEACH] = 0xE2D69C;
      c[Biomes.PLAINS] = 0x8CBD50;
      c[Biomes.FOREST] = 0x3E8030;
      c[Biomes.DESERT] = 0xDEC77E;
      c[Biomes.ICE_PLAINS] = 0xEAF3F7;
      c[Biomes.MOUNTAINS] = 0x8E8E8E;
      c[Biomes.DEEP_OCEAN] = 0x203F8C;
      c[Biomes.FROZEN_OCEAN] = 0xA9C6EC;
      c[Biomes.RIVER] = 0x4A80DC;
      c[Biomes.FROZEN_RIVER] = 0xBED3F4;
      c[Biomes.BIRCH_FOREST] = 0x70A650;
      c[Biomes.DARK_FOREST] = 0x2E5A26;
      c[Biomes.SNOWY_TAIGA] = 0xD2E2E2;
      c[Biomes.TAIGA] = 0x4D7F5C;
      c[Biomes.MEGA_TAIGA] = 0x62804C;
      c[Biomes.SWAMP] = 0x4E6C42;
      c[Biomes.SAVANNA] = 0xB9AB55;
      c[Biomes.JUNGLE] = 0x2B8E1E;
      c[Biomes.BADLANDS] = 0xCB7236;
      c[Biomes.MUSHROOM] = 0x8E6C9E;
      c[Biomes.ICE_SPIKES] = 0xD9F4FF;
      c[Biomes.STONE_SHORE] = 0x7C7C76;
      c[Biomes.MEADOW] = 0x80C460;
      c[Biomes.FROZEN_PEAKS] = 0xF4F8FA;
      c[Biomes.SNOWY_BEACH] = 0xF0F0E2;
      c[Biomes.FLOWER_FOREST] = 0x86B85E;
      c[Biomes.WARM_OCEAN] = 0x38AECC;
   }

   final Gui.Image img;
   final int w;
   final int h;
   /** Blocks between two samples. */
   final int step;
   final boolean marker;
   String key = "";
   int spawnX;
   int spawnZ;

   WorldPreview(int w, int h, int step, boolean marker) {
      this.w = w;
      this.h = h;
      this.step = step;
      this.marker = marker;
      this.img = new Gui.Image(w, h);
   }

   /** Makes the map again when the seed or the settings are not those of the last call. Returns true when it did. */
   boolean update(long seed, WorldSettings ws) {
      String k = seed + "/" + ws.type + "/" + ws.flat;
      if (k.equals(this.key)) {
         return false;
      }

      this.key = k;
      this.render(Dim.makeGen(Dim.OVERWORLD, seed, ws));
      return true;
   }

   void render(Gen gen) {
      int cx = 0;
      int cz = 0;
      boolean voidMap = gen instanceof SkyGen;
      try {
         int[] sp = gen.findSpawn();
         cx = voidMap ? 0 : sp[0];
         cz = voidMap ? 0 : sp[2];
      } catch (RuntimeException e) {
         // the map is centred on 0,0 then
      }

      this.spawnX = cx;
      this.spawnZ = cz;
      int effStep = voidMap ? 2 : this.step;
      int[] hs = new int[this.w * this.h];
      int[] bs = new int[this.w * this.h];
      for (int j = 0; j < this.h; j++) {
         for (int i = 0; i < this.w; i++) {
            Gen.Col col = gen.column(cx + (i - this.w / 2) * effStep, cz + (j - this.h / 2) * effStep);
            hs[j * this.w + i] = col.h;
            bs[j * this.w + i] = col.biome;
         }
      }

      for (int j = 0; j < this.h; j++) {
         for (int i = 0; i < this.w; i++) {
            int idx = j * this.w + i;
            int hgt = hs[idx];
            if (voidMap && hgt <= 1) {
               this.img.px[idx] = 0xFF141828;
               continue;
            }
            int b = bs[idx];
            int base = COL[Math.max(0, Math.min(Biomes.N - 1, b))];
            float shade;
            if (hgt < Gen.SEA) {
               // water: the deeper, the darker
               shade = 1.0F - Math.min(0.45F, (Gen.SEA - hgt) / 70.0F);
            } else {
               int hw = hs[j * this.w + Math.max(0, i - 1)];
               int he = hs[j * this.w + Math.min(this.w - 1, i + 1)];
               int hn = hs[Math.max(0, j - 1) * this.w + i];
               int hsouth = hs[Math.min(this.h - 1, j + 1) * this.w + i];
               shade = 1.0F + (hw - he) * 0.03F + (hn - hsouth) * 0.03F;
               shade = Math.max(0.62F, Math.min(1.35F, shade));
               if (hgt > 150 && b != Biomes.DESERT && b != Biomes.BADLANDS) {
                  base = mix(base, 0xF4F8FA, Math.min(1.0F, (hgt - 150) / 40.0F)); // snow caps
               }
            }

            int r = Math.min(255, (int)((base >> 16 & 255) * shade));
            int gr = Math.min(255, (int)((base >> 8 & 255) * shade));
            int bl = Math.min(255, (int)((base & 255) * shade));
            this.img.px[idx] = 0xFF000000 | r << 16 | gr << 8 | bl;
         }
      }

      if (this.marker) {
         int mx = this.w / 2;
         int mz = this.h / 2;
         for (int d = -2; d <= 2; d++) {
            this.put(mx + d, mz, 0xFFFFFFFF);
            this.put(mx, mz + d, 0xFFFFFFFF);
         }

         this.put(mx, mz, 0xFFE03030);
      }

      this.img.dirty = true;
   }

   void put(int x, int y, int argb) {
      if (x >= 0 && y >= 0 && x < this.w && y < this.h) {
         this.img.px[y * this.w + x] = argb;
      }
   }

   static int mix(int a, int b, float t) {
      int r = (int)((a >> 16 & 255) * (1 - t) + (b >> 16 & 255) * t);
      int g = (int)((a >> 8 & 255) * (1 - t) + (b >> 8 & 255) * t);
      int bl = (int)((a & 255) * (1 - t) + (b & 255) * t);
      return r << 16 | g << 8 | bl;
   }
}
