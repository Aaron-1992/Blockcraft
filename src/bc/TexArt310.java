package bc;

/**
 * Update 3.10 art. Logs lying on their side (felled trees) need their bark turned by 90 degrees so the grain runs along the log
 * (the mesher maps the tile's v axis to the vertical on side faces, and to the log's own axis on the top face of a z log).
 * The turned copies are made from the same generators as the normal bark, so they always match.
 * Tiles 246..248 = turned bark of oak (tile 7), birch (33) and spruce (31). The next free tile is 249.
 */
final class TexArt310 {
   private TexArt310() {
   }

   static final int TURNED0 = 246;

   /** The bark of oak, birch and spruce logs, exactly as Tex.init paints tiles 7, 33 and 31. */
   static int[] bark(int type) {
      if (ResourcePacks.isClassicBase()) {
         return type == 0 ? Tex.tLogSide(6L, 102, 81, 50) : (type == 1 ? Tex.tBirchSide() : Tex.tLogSide(15L, 58, 39, 19));
      }
      return type == 0 ? TexPaint.oakSide() : (type == 1 ? TexPaint.birchSide() : TexPaint.spruceSide());
   }

   static void blocks() {
      for (int type = 0; type < 3; type++) {
         int[] src = bark(type);
         int[] dst = new int[256];
         for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
               dst[y * 16 + x] = src[x * 16 + y]; // transposed: the vertical grain becomes horizontal
            }
         }

         Tex.blit(TURNED0 + type, dst);
      }
   }
}
