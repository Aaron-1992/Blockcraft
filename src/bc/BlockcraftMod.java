package bc;

/**
 * Public entrypoint interface for Blockcraft Java (.jar) mods.
 * External JARs placed in {@code mods/} can register an implementation via
 * {@code META-INF/services/bc.BlockcraftMod}, {@code Mod-Class} in {@code META-INF/MANIFEST.MF},
 * or {@code mainClass} in {@code mod.properties}.
 */
public interface BlockcraftMod {
   String id();

   String name();

   default String version() {
      return "1.0.0";
   }

   default String author() {
      return "Community";
   }

   default String description() {
      return "";
   }

   default void onInit(ModLoader.Context ctx) {
   }

   default void onEnable(Game game) {
   }

   default void onDisable(Game game) {
   }

   default void onWorldTick(World world) {
   }

   default void onGameTick(Game game) {
   }

   default boolean onCommand(Game game, String cmd, String[] args) {
      return false;
   }

   default void onBlockPlaced(World world, int x, int y, int z, int id) {
   }

   default void onBlockBroken(World world, int x, int y, int z, int oldId) {
   }

   /** Optional hook allowing a total-conversion mod (such as Infdev 2010) to provide its own terrain generator. */
   default Gen createWorldGen(int dim, long seed, WorldSettings settings) {
      return null;
   }

   /** Returns true if this mod converts the game into Minecraft Infdev (June 2010) mode. */
   default boolean totalConversionInfdev() {
      return false;
   }

   /** Optional top-left HUD watermark text (e.g. "Minecraft Infdev"). */
   default String hudWatermark() {
      return null;
   }

   /** Returns true to use the classic pre-Beta 1.8 HUD (hearts + armor only, no hunger bar, instant-heal food). */
   default boolean classicHudNoHunger() {
      return false;
   }

   /** Filters which block/item IDs appear in the inventory palette when this mod is active. */
   default boolean allowPaletteItem(int id) {
      return true;
   }
}
