package bc;

/**
 * Update 3.13: the biome table of the new world generator ({@link TerraGen}). The ids 0..6 are the seven biomes of the old generator
 * (Update 3.5 - 3.12) and keep their meaning, so old worlds, the LOD and everything that asks "is this a desert?" keep working;
 * 7.. are new.
 */
public final class Biomes {
   private Biomes() {
   }

   public static final int OCEAN = 0;
   public static final int BEACH = 1;
   public static final int PLAINS = 2;
   public static final int FOREST = 3;
   public static final int DESERT = 4;
   public static final int ICE_PLAINS = 5;
   public static final int MOUNTAINS = 6;
   public static final int DEEP_OCEAN = 7;
   public static final int FROZEN_OCEAN = 8;
   public static final int RIVER = 9;
   public static final int FROZEN_RIVER = 10;
   public static final int BIRCH_FOREST = 11;
   public static final int DARK_FOREST = 12;
   public static final int SNOWY_TAIGA = 13;
   public static final int TAIGA = 14;
   public static final int MEGA_TAIGA = 15;
   public static final int SWAMP = 16;
   public static final int SAVANNA = 17;
   public static final int JUNGLE = 18;
   public static final int BADLANDS = 19;
   public static final int MUSHROOM = 20;
   public static final int ICE_SPIKES = 21;
   public static final int STONE_SHORE = 22;
   public static final int MEADOW = 23;
   public static final int FROZEN_PEAKS = 24;
   public static final int SNOWY_BEACH = 25;
   public static final int FLOWER_FOREST = 26;
   public static final int WARM_OCEAN = 27;
   public static final int COLD_OCEAN=28, LUKEWARM_OCEAN=29, DEEP_COLD_OCEAN=30, DEEP_LUKEWARM_OCEAN=31, DEEP_FROZEN_OCEAN=32;
   public static final int N = 33;

   public static final String[] NAME = new String[]{
      "Ocean", "Beach", "Plains", "Forest", "Desert", "Ice Plains", "Extreme Hills", "Deep Ocean", "Frozen Ocean", "River", "Frozen River", "Birch Forest",
      "Dark Forest", "Snowy Taiga", "Taiga", "Giant Taiga", "Swamp", "Savanna", "Jungle", "Badlands", "Mushroom Fields", "Ice Spikes", "Stone Shore",
      "Meadow", "Frozen Peaks", "Snowy Beach", "Flower Forest", "Warm Ocean", "Cold Ocean", "Lukewarm Ocean", "Deep Cold Ocean", "Deep Lukewarm Ocean", "Deep Frozen Ocean"
   };

   public static String name(int b) {
      return b >= 0 && b < N ? NAME[b] : "Unknown";
   }

   public static boolean ocean(int b) {
      return b == OCEAN || b == DEEP_OCEAN || b == FROZEN_OCEAN || b == WARM_OCEAN || b>=COLD_OCEAN&&b<=DEEP_FROZEN_OCEAN;
   }

   public static boolean river(int b) {
      return b == RIVER || b == FROZEN_RIVER;
   }

   /** The water of the biome freezes at the top and the ground carries snow. */
   public static boolean snowy(int b) {
      return b == DEEP_FROZEN_OCEAN || b == ICE_PLAINS || b == FROZEN_OCEAN || b == FROZEN_RIVER || b == SNOWY_TAIGA || b == ICE_SPIKES || b == FROZEN_PEAKS || b == SNOWY_BEACH;
   }

   /** No rain, no snow: deserts and badlands. */
   public static boolean dry(int b) {
      return b == DESERT || b == BADLANDS;
   }

   /** What falls from the sky at a place: -1 nothing (dry), 1 rain, 2 snow. (Mountains snow above y 112, as in Update 3.8.) */
   public static int precipitation(int b, int h) {
      if (dry(b)) {
         return -1;
      }

      return snowy(b) || b == MOUNTAINS && h >= 112 || b == MEADOW && h >= 118 ? 2 : 1;
   }

   /** Biomes a new world may start in (the spawn search takes the first of these it meets, spiralling out from 0,0). */
   public static boolean spawnable(int b) {
      return b == PLAINS || b == FOREST || b == BIRCH_FOREST || b == FLOWER_FOREST || b == TAIGA || b == SAVANNA || b == DESERT || b == ICE_PLAINS || b == MEADOW
         || b == SNOWY_TAIGA;
   }

   /** A village fits here (the palette is picked by {@link #villagePalette}). */
   public static boolean village(int b) {
      return b == PLAINS || b == DESERT || b == SAVANNA || b == TAIGA || b == SNOWY_TAIGA || b == ICE_PLAINS || b == MEADOW;
   }

   /** 0 oak (plains, meadow), 1 sandstone (desert), 2 acacia (savanna), 3 spruce (taiga), 4 snow and spruce (snowy plains / taiga). */
   public static int villagePalette(int b) {
      switch (b) {
         case DESERT:
            return 1;
         case SAVANNA:
            return 2;
         case TAIGA:
            return 3;
         case SNOWY_TAIGA:
         case ICE_PLAINS:
            return 4;
         default:
            return 0;
      }
   }

   /** The colour a water surface gets in this biome (multiplied onto the water picture): white = the plain blue of the picture. */
   public static int waterTint(int b) {
      switch (b) {
         case SWAMP:
            return 0x7FA46A;
         case LUKEWARM_OCEAN:
         case DEEP_LUKEWARM_OCEAN:
            return 0xA4DEEE;
         case COLD_OCEAN:
         case DEEP_COLD_OCEAN:
            return 0xA0BCEF;
         case WARM_OCEAN:
            return 0x9BF2F0;
         case DEEP_FROZEN_OCEAN:
         case FROZEN_OCEAN:
         case FROZEN_RIVER:
            return 0xB8C8F4;
         case DEEP_OCEAN:
            return 0xC4C8FF;
         case MUSHROOM:
            return 0xE0C8F8;
         default:
            return 0xFFFFFF;
      }
   }

   /** Underwater haze, separate from the texture tint: warmer seas are clearer and greener. */
   static int waterFog(int biome) {
      return switch(biome) {
         case WARM_OCEAN -> 0x176B80;
         case LUKEWARM_OCEAN,DEEP_LUKEWARM_OCEAN -> 0x15536B;
         case COLD_OCEAN,DEEP_COLD_OCEAN,FROZEN_OCEAN,DEEP_FROZEN_OCEAN,FROZEN_RIVER -> 0x183C62;
         case SWAMP -> 0x354333;
         default -> 0x0D2961;
      };
   }

   /** The colour of the distant water of a sea or river (the plain blue c is tinted by the biome). */
   public static int lodWater(int biome, int c) {
      int t = waterTint(biome);
      if (biome == FROZEN_OCEAN || biome == DEEP_FROZEN_OCEAN || biome == FROZEN_RIVER) {
         return Lod.shade(0xC8DCF2, 0.9F);
      }

      return (int)((c >> 16 & 255) * (t >> 16 & 255) / 255.0F) << 16 | (int)((c >> 8 & 255) * (t >> 8 & 255) / 255.0F) << 8 | (int)((c & 255) * (t & 255) / 255.0F);
   }

   /** The colour the distant terrain (LOD) paints a column that is not under water (biomes 0..6 as in Update 3.12). */
   public static int lodColor(int biome, int h, double t, double hu) {
      switch (biome) {
         case BEACH:
            return 14735012;
         case DESERT:
            return 14931876;
         case ICE_PLAINS:
            return 15594485;
         case MOUNTAINS:
            if (h > 125) {
               return 15791350;
            } else if (h > 98) {
               return 9079434;
            }

            return Lod.shade(Gen.grassColorFor(t, hu), 0.8F);
         case FOREST:
            int g = Gen.grassColorFor(t, hu);
            return (int)((g >> 16 & 0xFF) * 0.72F) << 16 | (int)((g >> 8 & 0xFF) * 0.76F) << 8 | (int)((g & 0xFF) * 0.72F);
         case BIRCH_FOREST:
         case FLOWER_FOREST:
         case DARK_FOREST:
         case JUNGLE:
         case TAIGA:
         case MEGA_TAIGA: {
            int gg = Gen.grassColorFor(t, hu);
            float f = biome == DARK_FOREST ? 0.52F : (biome == JUNGLE ? 0.6F : (biome == TAIGA || biome == MEGA_TAIGA ? 0.62F : 0.74F));
            return (int)((gg >> 16 & 0xFF) * f) << 16 | (int)((gg >> 8 & 0xFF) * f) << 8 | (int)((gg & 0xFF) * f);
         }
         case SNOWY_TAIGA:
            return 0xDCE6EC;
         case ICE_SPIKES:
            return 0xE6F0FF;
         case FROZEN_PEAKS:
            return h > 150 ? 0xF4F8FC : 0xB9C4CC;
         case SNOWY_BEACH:
            return 0xF0EFE6;
         case BADLANDS:
            return h > 90 ? 0xB4694A : (h > 76 ? 0xC68B5C : 0xD9A066);
         case MUSHROOM:
            return 0x8B7B91;
         case STONE_SHORE:
            return 0x8E8E8E;
         case SAVANNA: {
            int gs = Gen.grassColorFor(Math.max(t, 0.82), Math.min(hu, 0.2));
            return Lod.shade(gs, 0.82F);
         }
         case SWAMP: {
            return 0x4F5E33;
         }
         case MEADOW:
            return Lod.shade(Gen.grassColorFor(Math.min(t, 0.5), Math.max(hu, 0.5)), 0.85F);
         case RIVER:
         case FROZEN_RIVER:
            return Lod.shade(Gen.grassColorFor(t, hu), 0.8F);
         case PLAINS:
         default:
            return Lod.shade(Gen.grassColorFor(t, hu), 0.8F);
      }
   }
}
