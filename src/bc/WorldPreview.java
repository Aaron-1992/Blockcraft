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
      // Update 6.18: the five ocean biomes added with the aquatic update had no colour and rendered black.
      c[Biomes.COLD_OCEAN] = 0x3B6FC0;
      c[Biomes.LUKEWARM_OCEAN] = 0x3FC0C8;
      c[Biomes.DEEP_COLD_OCEAN] = 0x28508F;
      c[Biomes.DEEP_LUKEWARM_OCEAN] = 0x227F9E;
      c[Biomes.DEEP_FROZEN_OCEAN] = 0x86AEDD;
   }

   /** Never black: a biome added later still gets a visible colour on the map. */
   static int color(int id) {
      return id >= 0 && id < COL.length && COL[id] != 0 ? COL[id] : 0x6B8F3A;
   }

   final Gui.Image img;
   /** Update 6.18: how many samples of each biome the current preview shows (used for the Create New World legend). */
   final int[] counts = new int[Biomes.N];
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
      // Update 6.18: aquatic generation, structures and the map change what the map shows, so they change the key too.
      String k = seed + "/" + ws.type + "/" + ws.flat + "/" + ws.aquatic + "/" + ws.structures
         + "/" + ws.bonusChest + "/" + ws.mapId + "/" + ws.structVer + "/" + ws.terrainVer
         + "/" + ws.caveScale + "/" + ws.caveDensity + "/" + ws.mountainScale + "/" + ws.ravineScale
         + "/" + ws.caveStructures + "/" + ws.caveLayers + "/" + ws.caveLakes + "/" + ws.caveRichness;
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
      for (int i = 0; i < this.counts.length; i++) {
         this.counts[i] = 0;
      }
      for (int j = 0; j < this.h; j++) {
         for (int i = 0; i < this.w; i++) {
            Gen.Col col = gen.column(cx + (i - this.w / 2) * effStep, cz + (j - this.h / 2) * effStep);
            int b = col.biome >= 0 && col.biome < Biomes.N ? col.biome : 0;
            hs[j * this.w + i] = col.h;
            bs[j * this.w + i] = b;
            this.counts[b]++;
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
            int base = color(b);
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

   /** Names of the most common biomes in the preview, for the Create New World hint. */
   String legendText(int max) {
      int[] ids = this.topBiomes(max);
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < ids.length; i++) {
         if (i > 0) sb.append(", ");
         sb.append(Biomes.name(ids[i]));
      }
      return sb.length() == 0 ? "no land in view" : sb.toString();
   }

   /** Number of different biomes visible in the preview map. */
   int distinctBiomes() {
      int n = 0;
      for (int c : this.counts) {
         if (c > 0) n++;
      }
      return n;
   }

   boolean hasBiome(int id) {
      return id >= 0 && id < this.counts.length && this.counts[id] > 0;
   }

   /** Up to {@code max} biome ids, most common first, for a compact legend. */
   int[] topBiomes(int max) {
      int[] out = new int[Math.max(0, Math.min(max, Biomes.N))];
      int filled = 0;
      for (int slot = 0; slot < out.length; slot++) {
         int best = -1;
         for (int id = 0; id < Biomes.N; id++) {
            if (this.counts[id] <= 0) continue;
            boolean taken = false;
            for (int k = 0; k < filled; k++) {
               if (out[k] == id) taken = true;
            }
            if (!taken && (best < 0 || this.counts[id] > this.counts[best])) best = id;
         }
         if (best < 0) break;
         out[filled++] = best;
      }
      int[] trimmed = new int[filled];
      System.arraycopy(out, 0, trimmed, 0, filled);
      return trimmed;
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
