package bc;

/** Headless-testable viewmodel light sampling. The map already includes time, torch colour, gamma and dimension ambient. */
final class HandLighting {
   private HandLighting() {}
   static int sample(int[] lightmap, float sky, float block) {
      int s = Math.max(0, Math.min(15, (int)(sky * 16)));
      int b = Math.max(0, Math.min(15, (int)(block * 16)));
      return lightmap[b * 16 + s];
   }
   static float fallback(float brightness, float gamma) {
      return Tex.gamma(Math.max(0, Math.min(1, brightness)), Math.max(0, Math.min(1, gamma)));
   }
}
