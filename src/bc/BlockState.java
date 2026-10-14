package bc;

/** Block IDs remain 12-bit; the existing chunk high-byte trailer now retains property bits too.
 * Old readers ignore the property nibble, new readers preserve old worlds without migration. */
final class BlockState {
   static final int ID_MASK = 4095;
   static final int WATERLOGGED = 0x1000;
   static final int FALLING = 0x2000;
   private BlockState() {}

   static boolean waterloggable(int id) {
      return Blocks.isSlab(id) || Blocks.isStair(id) || Blocks.isFence(id)
         || Shapes.isThin(id) || Shapes.isWall(id) || Shapes.isTrap(id) || Blocks.isLadder(id)
         || id==Blocks.CHEST || id==Blocks.CHEST_LOOT;
   }
   static boolean wet(int state) {
      int id = state & ID_MASK;
      return Aquatic.plant(id) || waterloggable(id) && (state & WATERLOGGED) != 0;
   }
   static int transition(int old, int next) {
      if (next < 0 || next > 65535) return -1;
      if ((next & ~ID_MASK) != 0) return next;
      int before = old & ID_MASK;
      if (wet(old) && next == 0) return Blocks.WATER;
      if (waterloggable(next) && (wet(old) || before == Blocks.WATER)) return next | WATERLOGGED;
      if (Redstone319.isRepeater(before) && Redstone319.isRepeater(next)
         || (before==Redstone319.DAYLIGHT_SENSOR||before==Redstone319.DAYLIGHT_SENSOR_ON)&&(next==Redstone319.DAYLIGHT_SENSOR||next==Redstone319.DAYLIGHT_SENSOR_ON)
         || RedstoneLogic.comparator(before) && RedstoneLogic.comparator(next)) return next | (old & 0xF000);
      return next;
   }
}
