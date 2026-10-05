package bc;

/**
 * Update 3.19: the "Look" of the shader pack - how the finished picture is graded: colour strength, contrast, the tint of the shadows and of the
 * highlights, the dark corners, a little film grain, and how strongly bright things glow. It is the game's answer to "the shaders look the same as
 * before": one click in Options > Graphics changes the whole picture. Options.look picks the entry; the post-processing shader
 * (ShaderSrc.COMP_FS) gets the numbers from {@link Shaders#endScene}.
 *
 * <p>Classic is exactly the grading that Updates 3.11 - 3.18 had, so nobody is forced to see a different picture.
 */
final class Look {
   private Look() {
   }

   static final int CLASSIC = 0;
   static final int VIVID = 1;
   static final int CINEMATIC = 2;
   static final int DREAMY = 3;
   /** What a fresh options file gets. */
   static final int DEFAULT = VIVID;

   static final String[] NAME = {"Classic", "Vivid", "Cinematic", "Dreamy"};
   static final String[] COLOR = {"\u00a77", "\u00a7a", "\u00a76", "\u00a7d"};
   static final String[] INFO = {
      "The look of Updates 3.11 to 3.18.",
      "Rich colours, warm sunlight, cool shadows, a clear glow around lava and torches.",
      "Film look: teal shadows, orange highlights, strong dark corners and a little grain.",
      "Soft and bright: a strong glow and gentle contrast, like a sunny dream."
   };

   // saturation, S-curve amount, exposure, vignette, bloom multiplier, film grain, black lift
   static final float[][] A = {
      {1.10F, 0.20F, 1.03F, 0.35F, 1.00F, 0.000F, 0.000F},
      {1.30F, 0.30F, 1.05F, 0.30F, 1.25F, 0.000F, 0.000F},
      {0.95F, 0.55F, 1.00F, 0.62F, 1.35F, 0.030F, 0.010F},
      {1.18F, 0.05F, 1.10F, 0.20F, 2.40F, 0.000F, 0.020F}
   };
   /** Colour factors for the dark parts and for the bright parts of the picture (1, 1, 1 = untouched). */
   static final float[][] SHADOW = {
      {1.00F, 1.00F, 1.00F},
      {0.93F, 0.98F, 1.07F},
      {0.78F, 0.97F, 1.12F},
      {1.06F, 0.98F, 1.10F}
   };
   static final float[][] HIGH = {
      {1.00F, 1.00F, 1.00F},
      {1.06F, 1.02F, 0.95F},
      {1.14F, 1.01F, 0.82F},
      {1.06F, 1.03F, 0.97F}
   };

   static int clamp(int look) {
      return Math.max(0, Math.min(NAME.length - 1, look));
   }

   static String name(int look) {
      return NAME[clamp(look)];
   }

   static String label(int look) {
      return COLOR[clamp(look)] + NAME[clamp(look)];
   }

   static String info(int look) {
      return INFO[clamp(look)];
   }

   /** The next look in the list (the button of the options screen). */
   static int next(int look) {
      return (clamp(look) + 1) % NAME.length;
   }
}
