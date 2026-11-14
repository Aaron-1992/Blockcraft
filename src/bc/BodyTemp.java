package bc;

/** Player-specific air temperature, wind chill, wetness and thermal stress. */
final class BodyTemp {
   float airC = 18.0F;
   float feelsC = 18.0F;
   float wetness;
   float coldStress;
   float heatStress;
   float movementFactor = 1.0F;
   float windPushX;
   float windPushZ;
   boolean sheltered;
   String status = "Comfortable";
   private long lastSample = Long.MIN_VALUE;
   private long stressTicks;
   private final double[] windTmp = new double[2];

   void reset() {
      airC = feelsC = 18.0F;
      wetness = coldStress = heatStress = 0.0F;
      movementFactor = 1.0F;
      windPushX = windPushZ = 0.0F;
      sheltered = false;
      status = "Comfortable";
      lastSample = Long.MIN_VALUE;
      stressTicks = 0L;
   }

   /** Sample every second; interpolate the display and effects each game tick. */
   void tick(Game g) {
      if (g == null || g.world == null || g.player == null || g.world.dim != Dim.OVERWORLD) {
         reset();
         return;
      }
      World w = g.world;
      Player p = g.player;
      long now = w.tickCount;
      if (this.lastSample == Long.MIN_VALUE || now - this.lastSample >= 20L) {
         this.lastSample = now;
         this.sample(g);
      }
      float targetCold = clamp((10.0F - this.feelsC) / 30.0F, 0.0F, 1.0F);
      float targetHeat = clamp((this.feelsC - 31.0F) / 24.0F, 0.0F, 1.0F);
      this.coldStress += (targetCold - this.coldStress) * 0.025F;
      this.heatStress += (targetHeat - this.heatStress) * 0.025F;
      float stress = Math.max(this.coldStress, this.heatStress);
      this.movementFactor = p.creative || p.flying ? 1.0F : 1.0F - 0.16F * stress;
      if (stress > 0.20F) {
         if (++this.stressTicks >= 600L) {
            this.stressTicks = 0L;
            p.addExhaustion(0.12F * stress);
         }
      } else {
         this.stressTicks = 0L;
      }
      if (this.coldStress > 0.25F) this.status = "Cold - seek shelter or warmth";
      else if (this.heatStress > 0.25F) this.status = "Overheated - find shade or water";
      else if (this.wetness > 0.65F) this.status = "Wet - wind chill increased";
      else this.status = this.sheltered ? "Sheltered" : "Comfortable";
   }

   private void sample(Game g) {
      World w = g.world;
      Player p = g.player;
      int x = (int)Math.floor(p.x);
      int y = (int)Math.floor(p.y + p.eye);
      int z = (int)Math.floor(p.z);
      Gen.Col col = w.gen.column(x, z);
      int surface = col.h;
      Chunk c = w.chunkAt(x, z);
      if (c != null && c.lit) {
         surface = c.hm[(z & 15) * 16 + (x & 15)];
      }
      int biome = col.biome;
      if (c != null) biome = c.biome[(z & 15) * 16 + (x & 15)] & 255;

      double climate = col.t;
      float base = -5.0F + (float)(clamp((float)climate, 0.0F, 1.0F) * 39.0F);
      base += biomeBias(biome);
      base -= Math.max(-64.0F, p.y - 64.0F) * 0.0065F; // normal atmospheric lapse rate
      base += (float)Math.cos((w.dayTime % Seasons.DAY - 6000L) * (Math.PI * 2.0 / Seasons.DAY)) * 3.0F;
      base += Seasons.bias(w.dayTime, w.settings.seasonDays, w.settings.seasons);

      boolean roof = false;
      for (int yy = y + 1; yy <= y + 12 && yy < Chunk.H; yy++) {
         int id = w.getBlock(x, yy, z);
         if (Blocks.opaque[id]) {
            roof = true;
            break;
         }
      }
      this.sheltered = roof;
      float rain = w.weather.snapAt(0.0F).rain(x, z);
      float wetTarget = !roof && rain > 0.20F ? Math.min(1.0F, rain * 1.3F) : 0.0F;
      this.wetness += (wetTarget - this.wetness) * (wetTarget > this.wetness ? 0.025F : 0.004F);
      float dampCooling = this.wetness * (1.4F + 2.8F * (float)w.wind.speed / 10.0F);
      if (p.inWater || p.eyeInWater) {
         base -= 8.0F;
         this.wetness = Math.max(this.wetness, 0.8F);
      }
      if (!roof) base -= rain * 1.5F;
      if (nearHeat(w, x, y, z)) base += 4.0F;
      this.airC = base;

      double[] local = w.wind.at(p.x, p.y, p.z, this.windTmp);
      float kmh = (float)(Math.sqrt(local[0] * local[0] + local[1] * local[1]) * 3.6);
      // Breezes affect wind chill and scenery, not player movement. Only exposed players in near-hurricane gusts get a small nudge.
      float exposure = roof ? 0.0F : 1.0F;
      float speed = kmh / 3.6F;
      this.windPushX = windPush((float)local[0], speed, exposure);
      this.windPushZ = windPush((float)local[1], speed, exposure);
      float chill = base;
      if (!roof && base <= 10.0F && kmh > 4.8F) {
         double v16 = Math.pow(kmh, 0.16);
         chill = (float)(13.12 + 0.6215 * base - 11.37 * v16 + 0.3965 * base * v16);
      }
      this.feelsC = chill - dampCooling;
   }

   /** Wind starts affecting horizontal motion only above 18 m/s (~65 km/h), reaching full strength at 25 m/s. */
   static float windPush(float component, float speed, float exposure) {
      if (speed <= 18.0F || exposure <= 0.0F) return 0.0F;
      float strength = clamp((speed - 18.0F) / 7.0F, 0.0F, 1.0F);
      return component * 0.00030F * strength * clamp(exposure, 0.0F, 1.0F);
   }

   private static float biomeBias(int biome) {
      if (biome == Biomes.DESERT) return 12.0F;
      if (biome == Biomes.BADLANDS || biome == Biomes.SAVANNA) return 9.0F;
      if (biome == Biomes.WARM_OCEAN || biome == Biomes.LUKEWARM_OCEAN) return 4.0F;
      if (biome == Biomes.FROZEN_OCEAN || biome == Biomes.DEEP_FROZEN_OCEAN || biome == Biomes.FROZEN_RIVER
         || biome == Biomes.SNOWY_TAIGA || biome == Biomes.ICE_PLAINS || biome == Biomes.ICE_SPIKES
         || biome == Biomes.FROZEN_PEAKS || biome == Biomes.SNOWY_BEACH) return -12.0F;
      if (biome == Biomes.TAIGA || biome == Biomes.MEGA_TAIGA || biome == Biomes.COLD_OCEAN
         || biome == Biomes.DEEP_COLD_OCEAN) return -5.0F;
      return 0.0F;
   }

   private static boolean nearHeat(World w, int x, int y, int z) {
      for (int dy = -2; dy <= 2; dy++) {
         for (int dz = -2; dz <= 2; dz++) {
            for (int dx = -2; dx <= 2; dx++) {
               int id = w.getBlock(x + dx, y + dy, z + dz);
               if (id == Blocks.FIRE || id == Blocks.LAVA || id == Blocks.GLOWSTONE || id == Blocks.MAGMA) return true;
            }
         }
      }
      return false;
   }

   private static float clamp(float v, float lo, float hi) {
      return Math.max(lo, Math.min(hi, v));
   }
}
