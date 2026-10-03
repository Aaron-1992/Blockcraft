package bc;

/** Update 3.12: the End (placeholder, filled in later). */
final class EndGen extends Gen {
   EndGen(long seed) {
      super(seed ^ 0x454E44L);
   }

   @Override
   public Chunk generate(int cx, int cz) {
      Chunk c = new Chunk(cx, cz);
      c.computeHeightmap();
      return c;
   }
}
