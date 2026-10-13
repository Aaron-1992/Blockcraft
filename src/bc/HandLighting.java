package bc;

/** Headless-testable viewmodel light sampling. The map already includes time, torch colour, gamma and dimension ambient. */
final class HandLighting {
   private HandLighting() {}
   static int sample(int[] lightmap, float sky, float block) {
      int s = Math.max(0, Math.min(15, (int)(sky * 16)));
      int b = Math.max(0, Math.min(15, (int)(block * 16)));
      return lightmap[b * 16 + s];
   }
   /** Nearby shader lights are camera-relative. Use distance only, never a fake viewmodel normal.
    * Max with the baked map avoids counting an already propagated torch twice. */
   static int withNearby(int base, float[] lights, float[] colors) {
      float r = 0, g = 0, b = 0;
      for (int i = 0; i + 3 < Math.min(lights.length, colors.length); i += 4) {
         float radius = lights[i + 3];
         if (radius <= 0) continue;
         float distance = (float)Math.sqrt(lights[i] * lights[i] + lights[i + 1] * lights[i + 1] + lights[i + 2] * lights[i + 2]);
         float attenuation = Math.max(0, 1 - distance / radius);
         attenuation *= attenuation * 0.8F;
         r += colors[i] * attenuation;
         g += colors[i + 1] * attenuation;
         b += colors[i + 2] * attenuation;
      }
      return 0xFF000000 | Math.max(base >> 16 & 255, Tex.clamp(Math.round(r * 255))) << 16
         | Math.max(base >> 8 & 255, Tex.clamp(Math.round(g * 255))) << 8
         | Math.max(base & 255, Tex.clamp(Math.round(b * 255)));
   }
   static float fallback(float brightness, float gamma) {
      return Tex.gamma(Math.max(0, Math.min(1, brightness)), Math.max(0, Math.min(1, gamma)));
   }
}
