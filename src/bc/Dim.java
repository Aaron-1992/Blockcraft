package bc;

import java.io.File;

/**
 * Update 3.12: the three dimensions. Every dimension is its own {@link World} (own chunk folder, own tile entities, own mobs file);
 * only one of them is loaded at a time, the game swaps them when you walk through a portal.
 */
public final class Dim {
   public static final int OVERWORLD = 0;
   public static final int NETHER = -1;
   public static final int END = 1;

   private Dim() {
   }

   public static boolean valid(int d) {
      return d == OVERWORLD || d == NETHER || d == END;
   }

   public static String name(int d) {
      return d == NETHER ? "The Nether" : (d == END ? "The End" : "The Overworld");
   }

   /** Name of the sub folder of the world folder that holds the chunks of a dimension ("" = the world folder itself). */
   public static String dirName(int d) {
      return d == NETHER ? "DIM-1" : (d == END ? "DIM1" : "");
   }

   public static File dir(File root, int d) {
      return root == null || d == OVERWORLD ? root : new File(root, dirName(d));
   }

   /** Only the Overworld has a sky: sun, moon, stars, clouds, weather, sky light, the distant terrain (LOD). */
   public static boolean hasSky(int d) {
      return d == OVERWORLD;
   }

   /** Light that is always there (0..1): the lightmap never goes darker than this. The Nether glows a little, the End a bit more. */
   public static float ambient(int d) {
      return d == NETHER ? 0.27F : (d == END ? 0.46F : 0.0F);
   }

   /**
    * How much of {@link #ambient} the green and the blue channel of the lightmap get: the Nether's glow is warm (red), the End's is
    * neutral, so that end stone stays the pale cream it is (it looked brown with the warm tint).
    */
   public static float[] ambientTint(int d) {
      return d == END ? new float[]{1.0F, 1.04F} : new float[]{0.8F, 0.78F};
   }

   /** The colour of the fog / the sky behind everything. */
   public static float[] fog(int d) {
      return d == NETHER ? new float[]{0.20F, 0.035F, 0.03F} : (d == END ? new float[]{0.075F, 0.045F, 0.10F} : new float[]{0.76F, 0.85F, 1.0F});
   }

   /** How many blocks a block in {@code from} is in {@code to} (the Nether is 8 times smaller). */
   public static double scale(int from, int to) {
      if (from == OVERWORLD && to == NETHER) {
         return 1.0 / 8.0;
      }

      return from == NETHER && to == OVERWORLD ? 8.0 : 1.0;
   }

   /** A dimension from what a player typed: nether / end / overworld (or -1 / 1 / 0). Returns Integer.MIN_VALUE if it is none of them. */
   public static int parse(String s) {
      if (s == null) {
         return Integer.MIN_VALUE;
      }

      switch (s.trim().toLowerCase()) {
         case "nether":
         case "-1":
         case "hell":
            return NETHER;
         case "end":
         case "the_end":
         case "1":
            return END;
         case "overworld":
         case "over":
         case "0":
         case "normal":
            return OVERWORLD;
         default:
            return Integer.MIN_VALUE;
      }
   }

   static Gen makeGen(int d, long seed) {
      return makeGen(d, seed, WorldSettings.classic());
   }

   /** Update 3.13: the Overworld generator depends on what the world was created with (the classic one for old worlds). */
   static Gen makeGen(int d, long seed, WorldSettings st) {
      if (d == NETHER) {
         return new NetherGen(seed);
      }

      if (d == END) {
         return new EndGen(seed);
      }

      Gen modGen = ModLoader.createModWorldGen(d, seed, st);
      if (modGen != null) {
         return modGen;
      }

      Gen g;
      if (st == null || st.type == WorldSettings.CLASSIC) {
         g = new Gen(seed);
      } else if (st.type == WorldSettings.FLAT) {
         g = new FlatGen(seed, st);
      } else if (st.isVoidMode()) {
         g = new SkyGen(seed, st);
      } else {
         g = new TerraGen(seed, st);
      }

      if (st != null) {
         g.settings = st;
      }

      return g;
   }
}
