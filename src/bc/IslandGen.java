package bc;

/** Finite, biome-rich mainland surrounded by ocean; terrain details remain those of TerraGen. */
public final class IslandGen extends TerraGen {
   private final int widthChunks;
   private final double radiusBlocks;

   public IslandGen(long seed, WorldSettings settings) {
      super(seed, settings);
      this.widthChunks = chunksFor(settings.type);
      this.radiusBlocks = this.widthChunks * 8.0;
   }

   public static int chunksFor(int type) {
      switch (type) {
         case WorldSettings.ISLAND_SMALL: return 256;
         case WorldSettings.ISLAND_MEDIUM: return 512;
         case WorldSettings.ISLAND_BIG: return 1028;
         default: throw new IllegalArgumentException("Not an island world type: " + type);
      }
   }

   public int widthChunks() { return this.widthChunks; }

   @Override
   void sample(int x, int z, Cl o) {
      super.sample(x, z, o);
      double coast = this.nBlend.fbm(x / 480.0 + 8.0, z / 480.0 - 17.0, 3);
      double detail = this.nTerrace.fbm(x / 155.0 - 6.0, z / 155.0 + 11.0, 2);
      double radius = this.radiusBlocks * (1.0 + coast * 0.045 + detail * 0.012);
      double distance = Math.sqrt((double)x * x + (double)z * z) / radius;
      double land = smooth(1.025, 0.82, distance);
      double seabed = 43.0 + 3.0 * this.nD2.noise2(x / 185.0, z / 185.0);
      o.genH = (int)Math.round(seabed + (o.genH - seabed) * land);
      o.genH = (int)clamp(o.genH, 4.0, 245.0);
      o.h = o.genH + this.st.depth;
      o.tEff = o.t - Math.max(0.0, o.genH - 82.0) * 0.0035;
      if (land < 0.025) o.c = -0.35;
      o.biome = pick(o);
      if (land > 0.04 && land < 0.98 && o.genH >= 60 && o.genH <= 66) {
         o.biome = o.tEff < 0.2 ? Biomes.SNOWY_BEACH : Biomes.BEACH;
      }
      o.altBiome = -1;
      o.blend = 0.0;
   }
}
