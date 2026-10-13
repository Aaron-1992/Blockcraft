package com.blockcraft.pe;

import android.content.Context;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Blockcraft PE (Pocket Edition) v1.1 — Infinite World Single-Player Engine:
 * - True Infinite Streaming World: 16x128x16 chunks generated deterministically at ANY (cx, cz) coordinate
 *   around the player using a lock-free O(1) 16x16 active spatial ring buffer + persistent modified chunk storage
 * - Multi-Biome Overworld Generation (Large Biomes Recommended, Default Biomes, Superflat)
 *   with Caves, Ore Veins, Oak/Spruce/Birch Forests, Flower Meadows, Desert Cacti, Snowy Peaks & Explorer Cabins
 * - Per-section non-air tracking (sectionNonAir[0..7]) so empty sky sections skip VBO meshing in O(1)
 * - Zero AI bots / mobs for pure, lag-free 120Hz single-player exploration, mining, crafting & building
 */
public final class PeWorld {
   // Screen States
   public static final int SCREEN_TITLE = 0;
   public static final int SCREEN_WORLD_SELECT = 1;
   public static final int SCREEN_CREATE_WORLD = 2;
   public static final int SCREEN_PLAY = 3;
   public static final int SCREEN_PAUSE = 4;
   public static final int SCREEN_INVENTORY = 5;
   public static final int SCREEN_SETTINGS = 6;

   // Single-Player Game Modes
   public static final int MODE_SURVIVAL = 0;
   public static final int MODE_CREATIVE = 1;
   public static final String[] MODE_NAMES = new String[]{
      "Survival",
      "Creative"
   };

   // World Generation Types
   public static final int WTYPE_LARGE_BIOMES = 0; // Default & Recommended!
   public static final int WTYPE_DEFAULT = 1;
   public static final int WTYPE_SUPERFLAT = 2;
   public static final String[] WTYPE_NAMES = new String[]{
      "Large Biomes (Recommended)",
      "Default Biomes",
      "Superflat World"
   };

   public static final long[] SEED_PRESETS = new long[]{
      0L,          // Random Seed
      884219551L,  // Emerald Peaks & Valleys
      409172634L,  // Village & Forest River
      771902388L   // Desert Oasis & Mesa
   };
   public static final String[] SEED_NAMES = new String[]{
      "Random Seed",
      "Mountain Peaks",
      "Village Valley",
      "Desert Oasis"
   };

   // Block IDs (0..63)
   public static final byte B_AIR = 0;
   public static final byte B_STONE = 1;
   public static final byte B_GRASS = 2;
   public static final byte B_DIRT = 3;
   public static final byte B_COBBLE = 4;
   public static final byte B_PLANKS_OAK = 5;
   public static final byte B_BEDROCK = 6;
   public static final byte B_WATER = 7;
   public static final byte B_LAVA = 8;
   public static final byte B_SAND = 9;
   public static final byte B_GRAVEL = 10;
   public static final byte B_GOLD_ORE = 11;
   public static final byte B_IRON_ORE = 12;
   public static final byte B_COAL_ORE = 13;
   public static final byte B_LOG_OAK = 14;
   public static final byte B_LEAVES_OAK = 15;
   public static final byte B_GLASS = 16;
   public static final byte B_SANDSTONE = 17;
   public static final byte B_WOOL_WHITE = 18;
   public static final byte B_WOOL_RED = 19;
   public static final byte B_WOOL_BLUE = 20;
   public static final byte B_WOOL_GREEN = 21;
   public static final byte B_WOOL_YELLOW = 22;
   public static final byte B_GOLD_BLOCK = 23;
   public static final byte B_IRON_BLOCK = 24;
   public static final byte B_BRICKS = 25;
   public static final byte B_TNT = 26;
   public static final byte B_BOOKSHELF = 27;
   public static final byte B_MOSSY_COBBLE = 28;
   public static final byte B_OBSIDIAN = 29;
   public static final byte B_TORCH = 30;
   public static final byte B_CHEST = 31;
   public static final byte B_DIAMOND_ORE = 32;
   public static final byte B_DIAMOND_BLOCK = 33;
   public static final byte B_CRAFTING_TABLE = 34;
   public static final byte B_FURNACE = 35;
   public static final byte B_REDSTONE_ORE = 36;
   public static final byte B_SNOW = 37;
   public static final byte B_ICE = 38;
   public static final byte B_NETHERRACK = 39;
   public static final byte B_SOUL_SAND = 40;
   public static final byte B_GLOWSTONE = 41;
   public static final byte B_STONEBRICK = 42;
   public static final byte B_NETHER_BRICKS = 43;
   public static final byte B_END_STONE = 44;
   public static final byte B_EMERALD_BLOCK = 45;
   public static final byte B_QUARTZ_BLOCK = 46;
   public static final byte B_SLIME_BLOCK = 47;
   public static final byte B_SEA_LANTERN = 48;
   public static final byte B_PRISMARINE = 49;
   public static final byte B_REDSTONE_BLOCK = 50;
   public static final byte B_COAL_BLOCK = 51;
   public static final byte B_PLANKS_SPRUCE = 52;
   public static final byte B_PLANKS_BIRCH = 53;
   public static final byte B_LOG_SPRUCE = 54;
   public static final byte B_PACKED_ICE = 55;
   public static final byte B_BED_RED = 56;
   public static final byte B_RED_SANDSTONE = 57;
   public static final byte B_TERRACOTTA = 58;
   public static final byte B_CONCRETE_GRAY = 59;
   public static final byte B_CONCRETE_YELLOW = 60;
   public static final byte B_CRACKED_STONEBRICK = 61;
   public static final byte B_PURPUR = 62;
   public static final byte B_BARS = 63;

   // Plant & Foliage Sub-Blocks
   public static final byte B_LEAVES_SPRUCE = 64;
   public static final byte B_CACTUS = 65;
   public static final byte B_TALL_GRASS = 66;
   public static final byte B_FLOWER_RED = 67;
   public static final byte B_FLOWER_YELLOW = 68;

   // Item IDs (98+)
   public static final int I_SWORD_WOOD = 98;
   public static final int I_PICK_WOOD = 99;
   public static final int I_SWORD_IRON = 100;
   public static final int I_SWORD_DIAMOND = 101;
   public static final int I_PICK_IRON = 102;
   public static final int I_PICK_DIAMOND = 103;
   public static final int I_SHOVEL_DIAMOND = 104;
   public static final int I_AXE_IRON = 105;
   public static final int I_BOW = 106;
   public static final int I_ARROW = 107;
   public static final int I_GOLDEN_APPLE = 108;
   public static final int I_STEAK = 109;
   public static final int I_ENDER_PEARL = 110;

   // Infinite Streaming Chunk Ring-Buffer Constants
   // 16x16 active chunk ring buffer (256 chunk slots covering up to 256x256 blocks around the player at any (cx, cz))
   public static final int ACTIVE_GRID = 16;
   public static final int ACTIVE_MASK = 15;
   public static final int ACTIVE_SLOTS = ACTIVE_GRID * ACTIVE_GRID; // 256
   public static final int CHUNK_BYTES = 16 * 16 * 128;              // 32,768 bytes per chunk
   public static final int WORLD_H = 128;
   public static final int SECTIONS_Y = WORLD_H / 16;                // 8 sections of 16x16x16
   public static final int Y_SEA = 60;
   public static final int Y_DECK = 64;

   private static final int SAVE_MAGIC = 0x42435045; // "BCPE"
   private static final int SAVE_VERSION_INFINITE = 2;

   /**
    * Represents one 16x128x16 chunk column in the active 16x16 spatial ring buffer.
    * Block index = ((lz << 11) | (lx << 7) | wy), requiring zero multiplications.
    */
   public static final class PeChunk {
      public int cx = Integer.MIN_VALUE;
      public int cz = Integer.MIN_VALUE;
      public boolean loaded = false;
      public boolean modified = false;
      public int maxY = 64;
      public final byte[] blocks = new byte[CHUNK_BYTES];
      public final int[] sectionNonAir = new int[SECTIONS_Y];
      public final boolean[] sectionDirty = new boolean[SECTIONS_Y];

      public void clear() {
         Arrays.fill(this.blocks, B_AIR);
         Arrays.fill(this.sectionNonAir, 0);
         Arrays.fill(this.sectionDirty, true);
         this.maxY = 0;
         this.modified = false;
      }

      public void recomputeSectionCounts() {
         Arrays.fill(this.sectionNonAir, 0);
         int highest = 0;
         for (int lz = 0; lz < 16; lz++) {
            int zBase = lz << 11;
            for (int lx = 0; lx < 16; lx++) {
               int colBase = zBase | (lx << 7);
               for (int y = 0; y < WORLD_H; y++) {
                  if (this.blocks[colBase | y] != B_AIR) {
                     this.sectionNonAir[y >> 4]++;
                     if (y > highest) highest = y;
                  }
               }
            }
         }
         this.maxY = highest;
      }
   }

   public final PeChunk[] chunkGrid = new PeChunk[ACTIVE_SLOTS];
   // Stores player-modified chunks at ANY (cx, cz) across the infinite world
   private final HashMap<Long, byte[]> savedChunks = new HashMap<Long, byte[]>();

   //Configurable active chunk streaming radius around the player (4=9x9, 5=11x11, 6=13x13 chunks)
   public int viewRadiusChunks = 5;
   private int lastStreamCenterCx = Integer.MIN_VALUE;
   private int lastStreamCenterCz = Integer.MIN_VALUE;

   // Current Screen & World Slot
   public int screen = SCREEN_TITLE;
   public int previousScreen = SCREEN_TITLE;
   public int currentSlot = 0; // 0, 1, 2
   public int createSlot = 0;
   public int createMode = MODE_SURVIVAL;
   public int createWorldType = WTYPE_LARGE_BIOMES;
   public int createSeedIdx = 0;

   // Saved World Metadata Cache for Slots 0..2
   public final boolean[] slotExists = new boolean[3];
   public final String[] slotSummary = new String[3];

   public int mode = MODE_SURVIVAL;
   public int worldType = WTYPE_LARGE_BIOMES;
   public long seed = 884219551L;
   public long tickCount = 0L;
   public float dayTime = 0.22F;

   // Player State (Supports arbitrary infinite coordinates!)
   public double px = 0.5;
   public double py = 72.0;
   public double pz = 0.5;
   public double vx = 0.0;
   public double vy = 0.0;
   public double vz = 0.0;
   public float yaw = 0.0F;
   public float pitch = 10.0F;
   public boolean onGround = false;
   public boolean flying = false;
   public boolean sneaking = false;
   public boolean sprinting = false;
   public boolean autoJump = true;
   public int health = 20;
   public int hunger = 20;
   public int hurtTime = 0;
   public int invulnTicks = 0;
   public float swingAnim = -1.0F;
   public float walkDist = 0.0F;
   public int selectedSlot = 0;
   public final int[] hotbarId = new int[9];
   public final int[] hotbarCount = new int[9];

   // Mining state
   public int mineX = 0, mineY = -1, mineZ = 0;
   public float mineProgress = 0.0F;

   // HUD Banner State
   public String bannerMessage = "";
   public int bannerTicks = 0;

   // Crafting Recipes for Single-Player Survival
   public static final class CraftRecipe {
      public final String label;
      public final int outId;
      public final int outCount;
      public final int costId;
      public final int costCount;

      public CraftRecipe(String label, int outId, int outCount, int costId, int costCount) {
         this.label = label;
         this.outId = outId;
         this.outCount = outCount;
         this.costId = costId;
         this.costCount = costCount;
      }
   }

   public static final CraftRecipe[] RECIPES = new CraftRecipe[]{
      new CraftRecipe("Oak Planks x4 (1 Log)", B_PLANKS_OAK, 4, B_LOG_OAK, 1),
      new CraftRecipe("Spruce Planks x4 (1 Spruce)", B_PLANKS_SPRUCE, 4, B_LOG_SPRUCE, 1),
      new CraftRecipe("Crafting Table (4 Planks)", B_CRAFTING_TABLE, 1, B_PLANKS_OAK, 4),
      new CraftRecipe("Furnace (8 Cobble)", B_FURNACE, 1, B_COBBLE, 8),
      new CraftRecipe("Storage Chest (8 Planks)", B_CHEST, 1, B_PLANKS_OAK, 8),
      new CraftRecipe("Torches x4 (1 Coal)", B_TORCH, 4, B_COAL_ORE, 1),
      new CraftRecipe("Stone Bricks x4 (4 Stone)", B_STONEBRICK, 4, B_STONE, 4),
      new CraftRecipe("Glass x4 (4 Sand)", B_GLASS, 4, B_SAND, 4),
      new CraftRecipe("Sandstone x2 (2 Sand)", B_SANDSTONE, 2, B_SAND, 2),
      new CraftRecipe("Wood Pickaxe (3 Planks)", I_PICK_WOOD, 1, B_PLANKS_OAK, 3),
      new CraftRecipe("Wood Sword (2 Planks)", I_SWORD_WOOD, 1, B_PLANKS_OAK, 2),
      new CraftRecipe("Iron Pickaxe (3 Iron Ore)", I_PICK_IRON, 1, B_IRON_ORE, 3),
      new CraftRecipe("Iron Sword (2 Iron Ore)", I_SWORD_IRON, 1, B_IRON_ORE, 2),
      new CraftRecipe("Iron Axe (3 Iron Ore)", I_AXE_IRON, 1, B_IRON_ORE, 3),
      new CraftRecipe("Diamond Pick (3 Diamond)", I_PICK_DIAMOND, 1, B_DIAMOND_ORE, 3),
      new CraftRecipe("Diamond Sword (2 Diamond)", I_SWORD_DIAMOND, 1, B_DIAMOND_ORE, 2),
      new CraftRecipe("Golden Apple (2 Gold Ore)", I_GOLDEN_APPLE, 1, B_GOLD_ORE, 2),
      new CraftRecipe("Bed (3 Planks)", B_BED_RED, 1, B_PLANKS_OAK, 3)
   };

   public PeWorld() {
      for (int i = 0; i < ACTIVE_SLOTS; i++) {
         this.chunkGrid[i] = new PeChunk();
      }
   }

   public static int slotIndex(int cx, int cz) {
      return (cx & ACTIVE_MASK) | ((cz & ACTIVE_MASK) << 4);
   }

   public static long chunkKey(int cx, int cz) {
      return (((long)cx) << 32) ^ (cz & 0xFFFFFFFFL);
   }

   // ====================================================================
   //  Persistent Save Directory & Infinite World Save/Load (.bcpe files)
   // ====================================================================
   public static File getSavesDir(Context context) {
      File base = null;
      if (context != null) {
         try {
            base = context.getFilesDir();
         } catch (Throwable ignored) {
         }
      }
      if (base == null) {
         base = new File(System.getProperty("java.io.tmpdir", "/tmp"), "blockcraft-pe-saves");
      }
      File dir = new File(base, "worlds");
      if (!dir.exists()) {
         dir.mkdirs();
      }
      return dir;
   }

   public void refreshSaveSlots(Context context) {
      File dir = getSavesDir(context);
      for (int s = 0; s < 3; s++) {
         File f = new File(dir, "world_" + s + ".bcpe");
         File meta = new File(dir, "world_" + s + ".meta");
         this.slotExists[s] = f.exists() && f.length() > 32;
         if (this.slotExists[s] && meta.exists()) {
            try {
               DataInputStream in = new DataInputStream(new FileInputStream(meta));
               String summary = in.readUTF();
               in.close();
               this.slotSummary[s] = summary;
            } catch (Exception e) {
               this.slotSummary[s] = "Saved Infinite World (Tap to Play)";
            }
         } else if (this.slotExists[s]) {
            this.slotSummary[s] = "Saved Infinite World (Tap to Play)";
         } else {
            this.slotSummary[s] = "Empty Slot  -  Tap to Create Infinite World";
         }
      }
   }

   public void deleteWorldSlot(Context context, int slot) {
      if (slot < 0 || slot >= 3) return;
      File dir = getSavesDir(context);
      new File(dir, "world_" + slot + ".bcpe").delete();
      new File(dir, "world_" + slot + ".meta").delete();
      refreshSaveSlots(context);
   }

   private void flushModifiedActiveChunksToMap() {
      for (int i = 0; i < ACTIVE_SLOTS; i++) {
         PeChunk c = this.chunkGrid[i];
         if (c.loaded && c.modified) {
            byte[] copy = new byte[CHUNK_BYTES];
            System.arraycopy(c.blocks, 0, copy, 0, CHUNK_BYTES);
            this.savedChunks.put(chunkKey(c.cx, c.cz), copy);
         }
      }
   }

   public boolean saveWorld(Context context) {
      if (this.screen == SCREEN_TITLE || this.screen == SCREEN_WORLD_SELECT || this.screen == SCREEN_CREATE_WORLD) {
         return false;
      }
      flushModifiedActiveChunksToMap();

      int slot = Math.max(0, Math.min(2, this.currentSlot));
      File dir = getSavesDir(context);
      File tmpFile = new File(dir, "world_" + slot + ".bcpe.tmp");
      File dstFile = new File(dir, "world_" + slot + ".bcpe");
      File metaFile = new File(dir, "world_" + slot + ".meta");
      try {
         DataOutputStream out = new DataOutputStream(
            new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(tmpFile)))
         );
         out.writeInt(SAVE_MAGIC);
         out.writeInt(SAVE_VERSION_INFINITE);
         out.writeInt(this.mode);
         out.writeInt(this.worldType);
         out.writeLong(this.seed);
         out.writeLong(this.tickCount);
         out.writeFloat(this.dayTime);
         out.writeDouble(this.px);
         out.writeDouble(this.py);
         out.writeDouble(this.pz);
         out.writeFloat(this.yaw);
         out.writeFloat(this.pitch);
         out.writeBoolean(this.flying);
         out.writeInt(this.health);
         out.writeInt(this.hunger);
         out.writeInt(this.selectedSlot);
         for (int i = 0; i < 9; i++) {
            out.writeInt(this.hotbarId[i]);
            out.writeInt(this.hotbarCount[i]);
         }
         out.writeInt(this.savedChunks.size());
         for (Map.Entry<Long, byte[]> entry : this.savedChunks.entrySet()) {
            long key = entry.getKey();
            int cx = (int)(key >> 32);
            int cz = (int)key;
            out.writeInt(cx);
            out.writeInt(cz);
            out.write(entry.getValue(), 0, CHUNK_BYTES);
         }
         out.flush();
         out.close();

         if (dstFile.exists()) dstFile.delete();
         tmpFile.renameTo(dstFile);

         int dayNum = (int)(this.tickCount / 24000L) + 1;
         String wTypeShort = this.worldType == WTYPE_LARGE_BIOMES ? "Large Biomes" : (this.worldType == WTYPE_SUPERFLAT ? "Superflat" : "Default");
         String summary = MODE_NAMES[this.mode] + "  |  " + wTypeShort + "  |  Day " + dayNum;
         DataOutputStream mOut = new DataOutputStream(new FileOutputStream(metaFile));
         mOut.writeUTF(summary);
         mOut.close();

         refreshSaveSlots(context);
         showBanner("World Saved (World " + (slot + 1) + ")");
         return true;
      } catch (Exception e) {
         return false;
      }
   }

   public boolean loadWorld(Context context, int slot) {
      slot = Math.max(0, Math.min(2, slot));
      File dir = getSavesDir(context);
      File srcFile = new File(dir, "world_" + slot + ".bcpe");
      if (!srcFile.exists()) return false;
      try {
         DataInputStream in = new DataInputStream(
            new GZIPInputStream(new BufferedInputStream(new FileInputStream(srcFile)))
         );
         int magic = in.readInt();
         int ver = in.readInt();
         if (magic != SAVE_MAGIC || (ver != SAVE_VERSION_INFINITE && ver != 1)) {
            in.close();
            return false;
         }
         this.currentSlot = slot;
         this.mode = in.readInt();
         this.worldType = in.readInt();
         this.seed = in.readLong();
         this.tickCount = in.readLong();
         this.dayTime = in.readFloat();
         this.px = in.readDouble();
         this.py = in.readDouble();
         this.pz = in.readDouble();
         this.yaw = in.readFloat();
         this.pitch = in.readFloat();
         this.flying = in.readBoolean();
         this.health = in.readInt();
         this.hunger = in.readInt();
         this.selectedSlot = Math.max(0, Math.min(8, in.readInt()));
         for (int i = 0; i < 9; i++) {
            this.hotbarId[i] = in.readInt();
            this.hotbarCount[i] = in.readInt();
         }

         resetActiveChunks();
         this.savedChunks.clear();

         if (ver == SAVE_VERSION_INFINITE) {
            int count = in.readInt();
            for (int i = 0; i < count; i++) {
               int cx = in.readInt();
               int cz = in.readInt();
               byte[] data = new byte[CHUNK_BYTES];
               in.readFully(data);
               this.savedChunks.put(chunkKey(cx, cz), data);
            }
         }
         in.close();

         this.vx = this.vy = this.vz = 0.0;
         this.mineY = -1;
         this.mineProgress = 0.0F;
         ensureChunksAroundPlayer(256);
         this.screen = SCREEN_PLAY;
         showBanner("Loaded World " + (slot + 1) + " (" + MODE_NAMES[this.mode] + ")");
         return true;
      } catch (Exception e) {
         return false;
      }
   }

   private void resetActiveChunks() {
      for (int i = 0; i < ACTIVE_SLOTS; i++) {
         PeChunk c = this.chunkGrid[i];
         c.loaded = false;
         c.modified = false;
         c.cx = Integer.MIN_VALUE;
         c.cz = Integer.MIN_VALUE;
      }
      this.lastStreamCenterCx = Integer.MIN_VALUE;
      this.lastStreamCenterCz = Integer.MIN_VALUE;
   }

   public void createNewSinglePlayerWorld(Context context, int slot, int newMode, int newWorldType, long customSeed) {
      this.currentSlot = Math.max(0, Math.min(2, slot));
      this.mode = newMode;
      this.worldType = newWorldType;
      this.seed = (customSeed != 0L) ? customSeed : (System.currentTimeMillis() ^ 0x5DEECE66DL);

      resetActiveChunks();
      this.savedChunks.clear();
      this.health = 20;
      this.hunger = 20;
      this.hurtTime = 0;
      this.invulnTicks = 40;
      this.flying = false;
      this.tickCount = 0L;
      this.dayTime = 0.22F;

      this.px = 0.5;
      this.pz = 0.5;
      ensureChunksAroundPlayer(256);

      int spawnY = topSolidY(0, 0) + 1;
      this.px = 0.5;
      this.py = spawnY + 0.05;
      this.pz = 0.5;
      this.vx = this.vy = this.vz = 0.0;
      this.yaw = 0.0F;
      this.pitch = 6.0F;

      equipDefaultLoadout();
      this.screen = SCREEN_PLAY;
      saveWorld(context);
      showBanner("Created Infinite World " + (this.currentSlot + 1) + " - " + WTYPE_NAMES[this.worldType]);
   }

   public void generateWorld(Context context, int newMode, int worldTypeOrMap) {
      createNewSinglePlayerWorld(context, this.currentSlot, newMode <= 1 ? newMode : MODE_SURVIVAL,
         worldTypeOrMap % 3, 884219551L + worldTypeOrMap * 997L);
   }

   public String currentWorldSummary() {
      return "World " + (this.currentSlot + 1) + " (" + MODE_NAMES[this.mode] + ")";
   }

   public void showBanner(String msg) {
      this.bannerMessage = msg;
      this.bannerTicks = 90;
   }

   private void equipDefaultLoadout() {
      Arrays.fill(this.hotbarId, 0);
      Arrays.fill(this.hotbarCount, 0);
      this.selectedSlot = 0;
      if (this.mode == MODE_CREATIVE) {
         setSlot(0, B_GRASS, 64);
         setSlot(1, B_STONEBRICK, 64);
         setSlot(2, B_PLANKS_OAK, 64);
         setSlot(3, B_LOG_OAK, 64);
         setSlot(4, B_LEAVES_OAK, 64);
         setSlot(5, B_BRICKS, 64);
         setSlot(6, B_GLASS, 64);
         setSlot(7, B_TORCH, 64);
         setSlot(8, B_GLOWSTONE, 64);
      } else {
         setSlot(0, I_PICK_WOOD, 1);
         setSlot(1, I_SWORD_WOOD, 1);
         setSlot(2, B_TORCH, 16);
         setSlot(3, B_PLANKS_OAK, 16);
         setSlot(4, B_COBBLE, 16);
         setSlot(5, B_CRAFTING_TABLE, 1);
         setSlot(6, I_STEAK, 6);
      }
   }

   private void setSlot(int slot, int id, int count) {
      this.hotbarId[slot] = id;
      this.hotbarCount[slot] = count;
   }

   public boolean tryCraftRecipe(int recipeIdx, PeAudio audio) {
      if (recipeIdx < 0 || recipeIdx >= RECIPES.length) return false;
      CraftRecipe r = RECIPES[recipeIdx];
      if (this.mode == MODE_CREATIVE) {
         giveItem(r.outId, r.outCount);
         if (audio != null) audio.playSound("pop", 0.85F, 1.15F);
         showBanner("Equipped " + getItemName(r.outId));
         return true;
      }
      int have = countItemInHotbar(r.costId);
      if (have < r.costCount) {
         showBanner("Need " + r.costCount + "x " + getItemName(r.costId) + " to craft!");
         if (audio != null) audio.playSound("click", 0.7F, 0.75F);
         return false;
      }
      consumeItemFromHotbar(r.costId, r.costCount);
      giveItem(r.outId, r.outCount);
      if (audio != null) audio.playSound("levelup", 0.85F, 1.1F);
      showBanner("Crafted " + r.outCount + "x " + getItemName(r.outId) + "!");
      return true;
   }

   public int countItemInHotbar(int id) {
      int total = 0;
      for (int i = 0; i < 9; i++) {
         if (this.hotbarId[i] == id && this.hotbarCount[i] > 0) {
            total += this.hotbarCount[i];
         }
      }
      return total;
   }

   private void consumeItemFromHotbar(int id, int count) {
      int rem = count;
      for (int i = 0; i < 9 && rem > 0; i++) {
         if (this.hotbarId[i] == id && this.hotbarCount[i] > 0) {
            int take = Math.min(rem, this.hotbarCount[i]);
            this.hotbarCount[i] -= take;
            rem -= take;
            if (this.hotbarCount[i] <= 0) {
               this.hotbarId[i] = 0;
               this.hotbarCount[i] = 0;
            }
         }
      }
   }

   public void giveItem(int id, int count) {
      for (int i = 0; i < 9; i++) {
         if (this.hotbarId[i] == id && this.hotbarCount[i] < 64) {
            this.hotbarCount[i] = Math.min(64, this.hotbarCount[i] + count);
            return;
         }
      }
      for (int i = 0; i < 9; i++) {
         if (this.hotbarId[i] == 0 || this.hotbarCount[i] <= 0) {
            this.hotbarId[i] = id;
            this.hotbarCount[i] = count;
            return;
         }
      }
      this.hotbarId[this.selectedSlot] = id;
      this.hotbarCount[this.selectedSlot] = count;
   }

   // ====================================================================
   //  Infinite World Streaming & Deterministic Chunk Generation
   // ====================================================================
   public void ensureChunksAroundPlayer(int maxNewChunks) {
      int pCx = ((int)Math.floor(this.px)) >> 4;
      int pCz = ((int)Math.floor(this.pz)) >> 4;
      int rad = Math.max(3, Math.min(7, this.viewRadiusChunks));
      int generated = 0;

      for (int ring = 0; ring <= rad && generated < maxNewChunks; ring++) {
         for (int dz = -ring; dz <= ring && generated < maxNewChunks; dz++) {
            for (int dx = -ring; dx <= ring && generated < maxNewChunks; dx++) {
               if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) continue;
               int cx = pCx + dx;
               int cz = pCz + dz;
               PeChunk slot = this.chunkGrid[slotIndex(cx, cz)];
               if (!slot.loaded || slot.cx != cx || slot.cz != cz) {
                  loadOrGenerateChunkIntoSlot(slot, cx, cz);
                  generated++;
               }
            }
         }
      }
      this.lastStreamCenterCx = pCx;
      this.lastStreamCenterCz = pCz;
   }

   private PeChunk getOrLoadChunk(int cx, int cz) {
      PeChunk c = this.chunkGrid[slotIndex(cx, cz)];
      if (!c.loaded || c.cx != cx || c.cz != cz) {
         loadOrGenerateChunkIntoSlot(c, cx, cz);
      }
      return c;
   }

   private void loadOrGenerateChunkIntoSlot(PeChunk slot, int cx, int cz) {
      // Save old modified chunk before evicting
      if (slot.loaded && slot.modified && (slot.cx != cx || slot.cz != cz)) {
         byte[] copy = new byte[CHUNK_BYTES];
         System.arraycopy(slot.blocks, 0, copy, 0, CHUNK_BYTES);
         this.savedChunks.put(chunkKey(slot.cx, slot.cz), copy);
      }

      slot.cx = cx;
      slot.cz = cz;
      slot.loaded = true;

      byte[] saved = this.savedChunks.get(chunkKey(cx, cz));
      if (saved != null) {
         System.arraycopy(saved, 0, slot.blocks, 0, CHUNK_BYTES);
         slot.modified = true;
         slot.recomputeSectionCounts();
         Arrays.fill(slot.sectionDirty, true);
      } else {
         slot.clear();
         if (this.worldType == WTYPE_SUPERFLAT) {
            generateSuperflatChunk(slot, cx, cz);
         } else {
            generateOverworldChunk(slot, cx, cz);
         }
         Arrays.fill(slot.sectionDirty, true);
      }

      // Mark adjacent neighbor chunks dirty so cross-chunk faces remesh cleanly
      markChunkDirtyIfLoaded(cx - 1, cz);
      markChunkDirtyIfLoaded(cx + 1, cz);
      markChunkDirtyIfLoaded(cx, cz - 1);
      markChunkDirtyIfLoaded(cx, cz + 1);
   }

   private void markChunkDirtyIfLoaded(int cx, int cz) {
      PeChunk n = this.chunkGrid[slotIndex(cx, cz)];
      if (n.loaded && n.cx == cx && n.cz == cz) {
         Arrays.fill(n.sectionDirty, true);
      }
   }

   public void markAllDirty() {
      for (int i = 0; i < ACTIVE_SLOTS; i++) {
         Arrays.fill(this.chunkGrid[i].sectionDirty, true);
      }
   }

   private static void setChunkLocal(PeChunk c, int lx, int wy, int lz, byte id) {
      if ((lx & ~15) != 0 || (lz & ~15) != 0 || (wy & ~127) != 0) return;
      int idx = (lz << 11) | (lx << 7) | wy;
      byte prev = c.blocks[idx];
      if (prev == id) return;
      c.blocks[idx] = id;
      int sy = wy >> 4;
      if (prev == B_AIR && id != B_AIR) {
         c.sectionNonAir[sy]++;
         if (wy > c.maxY) c.maxY = wy;
      } else if (prev != B_AIR && id == B_AIR) {
         if (c.sectionNonAir[sy] > 0) c.sectionNonAir[sy]--;
      }
   }

   private static int topSolidLocal(PeChunk c, int lx, int lz) {
      int colBase = (lz << 11) | (lx << 7);
      for (int y = WORLD_H - 2; y >= 1; y--) {
         byte b = c.blocks[colBase | y];
         if (isSolid(b) && b != B_LEAVES_OAK && b != B_LEAVES_SPRUCE) {
            return y;
         }
      }
      return Y_DECK;
   }

   private static int chunkHash(int x, int z, long seed) {
      int h = x * 374761393 + z * 668265263 + (int)(seed ^ (seed >>> 32)) * 1442695041;
      h = (h ^ (h >>> 13)) * 1274126177;
      return (h ^ (h >>> 16)) & 0x7FFFFFFF;
   }

   public int getBiomeAt(int wx, int wz) {
      if (this.worldType == WTYPE_SUPERFLAT) return 0;
      double biomeFreq = (this.worldType == WTYPE_LARGE_BIOMES) ? 0.015 : 0.032;
      double sOffX = ((this.seed & 0xFFFFL) - 32768.0) * 0.01;
      double sOffZ = (((this.seed >>> 16) & 0xFFFFL) - 32768.0) * 0.01;
      double bx = (wx + sOffX) * biomeFreq;
      double bz = (wz + sOffZ) * biomeFreq;
      double temp = Math.sin(bx + 0.5) * Math.cos(bz - 0.3);
      double humid = Math.cos(bx * 0.8 - 1.1) * Math.sin(bz * 0.8 + 0.7);
      if (temp > 0.38 && humid < 0.12) return 1; // Golden Desert
      if (temp < -0.36) return 2;                // Snowy Taiga
      if (humid > 0.42) return 3;                // Mountain Peaks
      if (temp > 0.12 && humid > 0.15) return 4; // Flower Meadow
      return 0;                                  // Emerald Forest
   }

   public String getBiomeName(int wx, int wz) {
      switch (getBiomeAt(wx, wz)) {
         case 1: return "Golden Desert";
         case 2: return "Snowy Taiga";
         case 3: return "Mountain Peaks";
         case 4: return "Flower Meadow";
         default: return this.worldType == WTYPE_SUPERFLAT ? "Superflat Plains" : "Emerald Forest";
      }
   }

   private void generateSuperflatChunk(PeChunk c, int cx, int cz) {
      for (int lz = 0; lz < 16; lz++) {
         for (int lx = 0; lx < 16; lx++) {
            setChunkLocal(c, lx, 0, lz, B_BEDROCK);
            for (int y = 1; y <= 59; y++) {
               setChunkLocal(c, lx, y, lz, B_STONE);
            }
            for (int y = 60; y <= 62; y++) {
               setChunkLocal(c, lx, y, lz, B_DIRT);
            }
            setChunkLocal(c, lx, 63, lz, B_GRASS);
         }
      }
      if (cx == 0 && cz == -1) {
         buildCabinInChunk(c, 8, 63, 6);
      }
   }

   private void generateOverworldChunk(PeChunk c, int cx, int cz) {
      double sOffX = ((this.seed & 0xFFFFL) - 32768.0) * 0.01;
      double sOffZ = (((this.seed >>> 16) & 0xFFFFL) - 32768.0) * 0.01;
      int wx0 = cx << 4;
      int wz0 = cz << 4;

      int[][] surfaceY = new int[16][16];
      int[][] biomeMap = new int[16][16];

      for (int lz = 0; lz < 16; lz++) {
         int wz = wz0 + lz;
         for (int lx = 0; lx < 16; lx++) {
            int wx = wx0 + lx;
            int biome = getBiomeAt(wx, wz);
            biomeMap[lz][lx] = biome;

            double n1 = Math.sin((wx + sOffX) * 0.042) * Math.cos((wz + sOffZ) * 0.042);
            double n2 = Math.sin((wx + wz) * 0.085) * 0.5 + Math.cos((wx - wz) * 0.105) * 0.5;
            double mtn = Math.max(0.0, Math.sin((wx - sOffX) * 0.025) * Math.sin((wz - sOffZ) * 0.025));
            double hillScale = (biome == 3) ? 20.0 : 10.0;
            int h = (int)Math.round(62.0 + n1 * 5.5 + n2 * 2.5 + mtn * hillScale);

            // Keep spawn area around (0, 0) flat and welcoming
            double spawnDist = Math.hypot(wx, wz);
            if (spawnDist < 12.0) {
               double blend = spawnDist / 12.0;
               h = (int)Math.round(64.0 * (1.0 - blend) + h * blend);
            }
            surfaceY[lz][lx] = h;

            setChunkLocal(c, lx, 0, lz, B_BEDROCK);
            for (int y = 1; y <= h; y++) {
               // Underground 3D Cave tunnels
               if (y > 4 && y < h - 4 && spawnDist > 10.0) {
                  double cave = Math.sin(wx * 0.14 + y * 0.18) * Math.cos(wz * 0.14 - y * 0.15)
                     + Math.cos((wx - wz) * 0.11 + y * 0.22) * 0.5;
                  if (cave > 1.08) {
                     continue;
                  }
               }

               byte b;
               if (y == h) {
                  if (h <= Y_SEA) b = B_SAND;
                  else if (biome == 1) b = B_SAND;
                  else if (biome == 2 || h >= 77) b = B_SNOW;
                  else b = B_GRASS;
               } else if (y >= h - 3) {
                  b = (biome == 1) ? B_SANDSTONE : B_DIRT;
               } else {
                  int hash = ((wx * 734287) ^ (y * 912931) ^ (wz * 439829) ^ (int)this.seed) & 255;
                  if (y <= 16 && hash <= 1) b = B_DIAMOND_ORE;
                  else if (y <= 28 && hash <= 4) b = B_GOLD_ORE;
                  else if (y <= 24 && hash == 5) b = B_REDSTONE_ORE;
                  else if (y <= 52 && hash <= 11) b = B_IRON_ORE;
                  else if (hash <= 19) b = B_COAL_ORE;
                  else if (hash == 20) b = B_GRAVEL;
                  else b = B_STONE;
               }
               setChunkLocal(c, lx, y, lz, b);
            }
            for (int y = h + 1; y <= Y_SEA; y++) {
               setChunkLocal(c, lx, y, lz, (biome == 2 && y == Y_SEA) ? B_ICE : B_WATER);
            }
         }
      }

      // Starter Cabin at chunk (0, -1), or rare Explorer Cabins across the infinite world
      boolean hasCabin = (cx == 0 && cz == -1) || ((cx != 0 || cz != 0) && (chunkHash(cx, cz, this.seed + 77L) % 44 == 0));
      if (hasCabin) {
         int hy = surfaceY[8][8];
         if (hy > Y_SEA + 1 && hy < 76) {
            buildCabinInChunk(c, 8, hy, 8);
            return;
         }
      }

      // Deterministic Trees, Cacti, Flowers & Tall Grass inside chunk bounds (lx, lz in 2..13)
      for (int gz = 0; gz < 2; gz++) {
         for (int gx = 0; gx < 2; gx++) {
            int cellH = chunkHash(cx * 2 + gx, cz * 2 + gz, this.seed);
            int lx = 3 + gx * 6 + (cellH & 3);
            int lz = 3 + gz * 6 + ((cellH >>> 2) & 3);
            int wx = wx0 + lx;
            int wz = wz0 + lz;
            if (Math.hypot(wx, wz) < 8.5) continue;

            int ty = surfaceY[lz][lx];
            int biome = biomeMap[lz][lx];
            byte top = c.blocks[(lz << 11) | (lx << 7) | ty];
            if (ty <= Y_SEA + 1 || ty >= 76) continue;

            if ((top == B_GRASS || top == B_SNOW) && (cellH % 3 != 0)) {
               boolean spruce = (top == B_SNOW) || (biome == 2) || ((cellH & 7) == 0);
               placeTreeInChunk(c, lx, ty + 1, lz, spruce ? B_LOG_SPRUCE : B_LOG_OAK, spruce ? B_LEAVES_SPRUCE : B_LEAVES_OAK);
            } else if (top == B_SAND && (cellH & 3) == 0) {
               for (int ch = 1; ch <= 3; ch++) {
                  setChunkLocal(c, lx, ty + ch, lz, B_CACTUS);
               }
            }
         }
      }

      // Surface Flowers & Tall Grass tufts on open grass blocks
      for (int lz = 1; lz < 15; lz++) {
         for (int lx = 1; lx < 15; lx++) {
            int ty = surfaceY[lz][lx];
            if (ty <= Y_SEA + 1 || ty >= 75) continue;
            int colBase = (lz << 11) | (lx << 7);
            if (c.blocks[colBase | ty] == B_GRASS && c.blocks[colBase | (ty + 1)] == B_AIR) {
               int fh = chunkHash(wx0 + lx, wz0 + lz, this.seed + 313L) & 63;
               int biome = biomeMap[lz][lx];
               if (fh == 0 || (biome == 4 && fh <= 3)) {
                  setChunkLocal(c, lx, ty + 1, lz, B_FLOWER_RED);
               } else if (fh == 1 || (biome == 4 && fh <= 7)) {
                  setChunkLocal(c, lx, ty + 1, lz, B_FLOWER_YELLOW);
               } else if (fh <= 6) {
                  setChunkLocal(c, lx, ty + 1, lz, B_TALL_GRASS);
               }
            }
         }
      }
   }

   private static void placeTreeInChunk(PeChunk c, int lx, int y, int lz, byte logId, byte leafId) {
      for (int dy = 3; dy <= 6; dy++) {
         int r = (dy >= 5) ? 1 : 2;
         for (int dz = -r; dz <= r; dz++) {
            for (int dx = -r; dx <= r; dx++) {
               if (Math.abs(dx) == 2 && Math.abs(dz) == 2 && (dy == 3 || dy == 4)) continue;
               int nx = lx + dx;
               int nz = lz + dz;
               if ((nx & ~15) == 0 && (nz & ~15) == 0 && (y + dy) < WORLD_H - 1) {
                  int idx = (nz << 11) | (nx << 7) | (y + dy);
                  if (c.blocks[idx] == B_AIR) {
                     setChunkLocal(c, nx, y + dy, nz, leafId);
                  }
               }
            }
         }
      }
      for (int dy = 0; dy < 5; dy++) {
         setChunkLocal(c, lx, y + dy, lz, logId);
      }
   }

   private static void buildCabinInChunk(PeChunk c, int ox, int oy, int oz) {
      for (int dz = -3; dz <= 3; dz++) {
         for (int dx = -3; dx <= 3; dx++) {
            int lx = ox + dx;
            int lz = oz + dz;
            if ((lx & ~15) != 0 || (lz & ~15) != 0) continue;
            for (int y = oy - 3; y <= oy; y++) {
               setChunkLocal(c, lx, y, lz, B_COBBLE);
            }
            boolean wall = Math.abs(dx) == 3 || Math.abs(dz) == 3;
            boolean door = (dz == 3 && dx == 0);
            for (int dy = 1; dy <= 4; dy++) {
               if (wall && !(door && dy <= 2)) {
                  boolean corner = Math.abs(dx) == 3 && Math.abs(dz) == 3;
                  boolean win = dy == 2 && (dx == 0 || dz == 0) && !door;
                  setChunkLocal(c, lx, oy + dy, lz, corner ? B_LOG_OAK : (win ? B_GLASS : B_PLANKS_OAK));
               } else {
                  setChunkLocal(c, lx, oy + dy, lz, B_AIR);
               }
            }
            setChunkLocal(c, lx, oy + 5, lz, B_PLANKS_SPRUCE);
         }
      }
      setChunkLocal(c, ox - 2, oy + 1, oz - 2, B_CRAFTING_TABLE);
      setChunkLocal(c, ox, oy + 1, oz - 2, B_FURNACE);
      setChunkLocal(c, ox + 2, oy + 1, oz - 2, B_CHEST);
      setChunkLocal(c, ox - 2, oy + 1, oz + 1, B_BED_RED);
      setChunkLocal(c, ox, oy + 3, oz, B_TORCH);
   }

   // ====================================================================
   //  Fast O(1) Block Access Across Infinite Coordinates
   // ====================================================================
   public int topSolidY(int wx, int wz) {
      int cx = wx >> 4;
      int cz = wz >> 4;
      PeChunk c = getOrLoadChunk(cx, cz);
      return topSolidLocal(c, wx & 15, wz & 15);
   }

   /** Fast O(1) lookup in the active 16x16 chunk ring buffer (returns B_AIR if chunk is outside active ring). */
   public byte getBlockFast(int wx, int wy, int wz) {
      if ((wy & ~127) != 0) return B_AIR;
      int cx = wx >> 4;
      int cz = wz >> 4;
      PeChunk c = this.chunkGrid[(cx & ACTIVE_MASK) | ((cz & ACTIVE_MASK) << 4)];
      if (!c.loaded || c.cx != cx || c.cz != cz) {
         return B_AIR;
      }
      return c.blocks[((wz & 15) << 11) | ((wx & 15) << 7) | wy];
   }

   /** Full infinite-world block getter: automatically loads/generates chunk (cx, cz) if not yet in the active ring. */
   public byte getBlock(int wx, int wy, int wz) {
      if ((wy & ~127) != 0) return B_AIR;
      int cx = wx >> 4;
      int cz = wz >> 4;
      PeChunk c = this.chunkGrid[(cx & ACTIVE_MASK) | ((cz & ACTIVE_MASK) << 4)];
      if (!c.loaded || c.cx != cx || c.cz != cz) {
         c = getOrLoadChunk(cx, cz);
      }
      return c.blocks[((wz & 15) << 11) | ((wx & 15) << 7) | wy];
   }

   public void setBlock(int wx, int wy, int wz, byte id) {
      if ((wy & ~127) != 0) return;
      int cx = wx >> 4;
      int cz = wz >> 4;
      int lx = wx & 15;
      int lz = wz & 15;
      PeChunk c = getOrLoadChunk(cx, cz);
      int idx = (lz << 11) | (lx << 7) | wy;
      byte prev = c.blocks[idx];
      if (prev == id) return;

      c.blocks[idx] = id;
      c.modified = true;
      int sy = wy >> 4;
      if (prev == B_AIR && id != B_AIR) {
         c.sectionNonAir[sy]++;
         if (wy > c.maxY) c.maxY = wy;
      } else if (prev != B_AIR && id == B_AIR) {
         if (c.sectionNonAir[sy] > 0) c.sectionNonAir[sy]--;
      }

      c.sectionDirty[sy] = true;
      if ((wy & 15) == 0 && sy > 0) c.sectionDirty[sy - 1] = true;
      if ((wy & 15) == 15 && sy + 1 < SECTIONS_Y) c.sectionDirty[sy + 1] = true;

      if (lx == 0) {
         PeChunk w = this.chunkGrid[slotIndex(cx - 1, cz)];
         if (w.loaded && w.cx == cx - 1 && w.cz == cz) w.sectionDirty[sy] = true;
      } else if (lx == 15) {
         PeChunk e = this.chunkGrid[slotIndex(cx + 1, cz)];
         if (e.loaded && e.cx == cx + 1 && e.cz == cz) e.sectionDirty[sy] = true;
      }
      if (lz == 0) {
         PeChunk n = this.chunkGrid[slotIndex(cx, cz - 1)];
         if (n.loaded && n.cx == cx && n.cz == cz - 1) n.sectionDirty[sy] = true;
      } else if (lz == 15) {
         PeChunk s = this.chunkGrid[slotIndex(cx, cz + 1)];
         if (s.loaded && s.cx == cx && s.cz == cz + 1) s.sectionDirty[sy] = true;
      }
   }

   public static boolean isCrossPlant(byte id) {
      return id == B_TORCH || id == B_TALL_GRASS || id == B_FLOWER_RED || id == B_FLOWER_YELLOW;
   }

   public static boolean isSolid(byte id) {
      return id != B_AIR && id != B_WATER && id != B_LAVA && !isCrossPlant(id);
   }

   public static boolean isOpaque(byte id) {
      return id != B_AIR && id != B_WATER && id != B_LAVA && id != B_GLASS
         && id != B_LEAVES_OAK && id != B_LEAVES_SPRUCE
         && id != B_ICE && id != B_SLIME_BLOCK && !isCrossPlant(id)
         && id != B_BARS && id != B_BED_RED;
   }

   public static boolean isLightEmitter(byte id) {
      return id == B_TORCH || id == B_GLOWSTONE || id == B_SEA_LANTERN || id == B_LAVA || id == B_REDSTONE_BLOCK;
   }

   public static int getFaceTile(byte id, int face) {
      switch (id) {
         case B_STONE: return PeTextures.T_STONE;
         case B_GRASS: return face == 0 ? PeTextures.T_GRASS_TOP : (face == 1 ? PeTextures.T_DIRT : PeTextures.T_GRASS_SIDE);
         case B_DIRT: return PeTextures.T_DIRT;
         case B_COBBLE: return PeTextures.T_COBBLE;
         case B_PLANKS_OAK: return PeTextures.T_PLANKS_OAK;
         case B_BEDROCK: return PeTextures.T_BEDROCK;
         case B_WATER: return PeTextures.T_WATER;
         case B_LAVA: return PeTextures.T_LAVA;
         case B_SAND: return PeTextures.T_SAND;
         case B_GRAVEL: return PeTextures.T_GRAVEL;
         case B_GOLD_ORE: return PeTextures.T_GOLD_ORE;
         case B_IRON_ORE: return PeTextures.T_IRON_ORE;
         case B_COAL_ORE: return PeTextures.T_COAL_ORE;
         case B_LOG_OAK: return (face <= 1) ? PeTextures.T_LOG_OAK_TOP : PeTextures.T_LOG_OAK_SIDE;
         case B_LEAVES_OAK: return PeTextures.T_LEAVES_OAK;
         case B_LEAVES_SPRUCE: return PeTextures.T_LEAVES_SPRUCE;
         case B_GLASS: return PeTextures.T_GLASS;
         case B_SANDSTONE: return (face <= 1) ? PeTextures.T_SANDSTONE_TOP : PeTextures.T_SANDSTONE_SIDE;
         case B_WOOL_WHITE: return PeTextures.T_WOOL_WHITE;
         case B_WOOL_RED: return PeTextures.T_WOOL_RED;
         case B_WOOL_BLUE: return PeTextures.T_WOOL_BLUE;
         case B_WOOL_GREEN: return PeTextures.T_WOOL_GREEN;
         case B_WOOL_YELLOW: return PeTextures.T_WOOL_YELLOW;
         case B_GOLD_BLOCK: return PeTextures.T_GOLD_BLOCK;
         case B_IRON_BLOCK: return PeTextures.T_IRON_BLOCK;
         case B_BRICKS: return PeTextures.T_BRICKS;
         case B_TNT: return face == 0 ? PeTextures.T_TNT_TOP : (face == 1 ? PeTextures.T_TNT_BOTTOM : PeTextures.T_TNT_SIDE);
         case B_BOOKSHELF: return (face <= 1) ? PeTextures.T_PLANKS_OAK : PeTextures.T_BOOKSHELF;
         case B_MOSSY_COBBLE: return PeTextures.T_MOSSY_COBBLE;
         case B_OBSIDIAN: return PeTextures.T_OBSIDIAN;
         case B_TORCH: return PeTextures.T_TORCH;
         case B_CHEST: return face == 0 || face == 1 ? PeTextures.T_CHEST_TOP : (face == 3 ? PeTextures.T_CHEST_FRONT : PeTextures.T_CHEST_SIDE);
         case B_DIAMOND_ORE: return PeTextures.T_DIAMOND_ORE;
         case B_DIAMOND_BLOCK: return PeTextures.T_DIAMOND_BLOCK;
         case B_CRAFTING_TABLE: return face == 0 ? PeTextures.T_CRAFT_TOP : (face == 1 ? PeTextures.T_PLANKS_OAK : PeTextures.T_CRAFT_SIDE);
         case B_FURNACE: return face == 3 ? PeTextures.T_FURNACE_FRONT : PeTextures.T_FURNACE_SIDE;
         case B_REDSTONE_ORE: return PeTextures.T_REDSTONE_ORE;
         case B_SNOW: return face == 0 ? PeTextures.T_SNOW : (face == 1 ? PeTextures.T_DIRT : PeTextures.T_SNOW_SIDE);
         case B_ICE: return PeTextures.T_ICE;
         case B_NETHERRACK: return PeTextures.T_NETHERRACK;
         case B_SOUL_SAND: return PeTextures.T_SOUL_SAND;
         case B_GLOWSTONE: return PeTextures.T_GLOWSTONE;
         case B_STONEBRICK: return PeTextures.T_STONEBRICK;
         case B_NETHER_BRICKS: return PeTextures.T_NETHER_BRICKS;
         case B_END_STONE: return PeTextures.T_END_STONE;
         case B_EMERALD_BLOCK: return PeTextures.T_EMERALD_BLOCK;
         case B_QUARTZ_BLOCK: return PeTextures.T_QUARTZ_BLOCK;
         case B_SLIME_BLOCK: return PeTextures.T_SLIME_BLOCK;
         case B_SEA_LANTERN: return PeTextures.T_SEA_LANTERN;
         case B_PRISMARINE: return PeTextures.T_PRISMARINE;
         case B_REDSTONE_BLOCK: return PeTextures.T_REDSTONE_BLOCK;
         case B_COAL_BLOCK: return PeTextures.T_COAL_BLOCK;
         case B_PLANKS_SPRUCE: return PeTextures.T_PLANKS_SPRUCE;
         case B_PLANKS_BIRCH: return PeTextures.T_PLANKS_BIRCH;
         case B_LOG_SPRUCE: return (face <= 1) ? PeTextures.T_LOG_OAK_TOP : PeTextures.T_LOG_SPRUCE;
         case B_PACKED_ICE: return PeTextures.T_PACKED_ICE;
         case B_BED_RED: return PeTextures.T_BED_RED;
         case B_RED_SANDSTONE: return PeTextures.T_RED_SANDSTONE;
         case B_TERRACOTTA: return PeTextures.T_TERRACOTTA;
         case B_CONCRETE_GRAY: return PeTextures.T_CONCRETE_GRAY;
         case B_CONCRETE_YELLOW: return PeTextures.T_CONCRETE_YELLOW;
         case B_CRACKED_STONEBRICK: return PeTextures.T_CRACKED_STONEBRICK;
         case B_PURPUR: return PeTextures.T_PURPUR;
         case B_BARS: return PeTextures.T_BARS;
         case B_CACTUS: return (face <= 1) ? PeTextures.T_CACTUS_TOP : PeTextures.T_CACTUS_SIDE;
         case B_TALL_GRASS: return PeTextures.T_TALL_GRASS;
         case B_FLOWER_RED: return PeTextures.T_FLOWER_RED;
         case B_FLOWER_YELLOW: return PeTextures.T_FLOWER_YELLOW;
         default: return PeTextures.T_STONE;
      }
   }

   public static int getItemTile(int id) {
      if (id == B_GRASS) return PeTextures.T_GRASS_SIDE;
      if (id == B_SNOW) return PeTextures.T_SNOW_SIDE;
      if (id > 0 && id <= 68) {
         return getFaceTile((byte)id, 3);
      }
      switch (id) {
         case I_SWORD_WOOD: return PeTextures.T_ITEM_SWORD_WOOD;
         case I_PICK_WOOD: return PeTextures.T_ITEM_PICK_WOOD;
         case I_SWORD_IRON: return PeTextures.T_ITEM_SWORD_IRON;
         case I_SWORD_DIAMOND: return PeTextures.T_ITEM_SWORD_DIAMOND;
         case I_PICK_IRON: return PeTextures.T_ITEM_PICK_IRON;
         case I_PICK_DIAMOND: return PeTextures.T_ITEM_PICK_DIAMOND;
         case I_SHOVEL_DIAMOND: return PeTextures.T_ITEM_SHOVEL_DIAMOND;
         case I_AXE_IRON: return PeTextures.T_ITEM_AXE_IRON;
         case I_BOW: return PeTextures.T_ITEM_BOW;
         case I_ARROW: return PeTextures.T_ITEM_ARROW;
         case I_GOLDEN_APPLE: return PeTextures.T_ITEM_GOLDEN_APPLE;
         case I_STEAK: return PeTextures.T_ITEM_STEAK;
         case I_ENDER_PEARL: return PeTextures.T_ITEM_ENDER_PEARL;
         default: return PeTextures.T_ITEM_PICK_WOOD;
      }
   }

   public static String getItemName(int id) {
      switch (id) {
         case 0: return "Empty Hand";
         case B_GRASS: return "Grass Block";
         case B_STONE: return "Stone";
         case B_DIRT: return "Dirt";
         case B_COBBLE: return "Cobblestone";
         case B_PLANKS_OAK: return "Oak Planks";
         case B_PLANKS_SPRUCE: return "Spruce Planks";
         case B_PLANKS_BIRCH: return "Birch Planks";
         case B_BRICKS: return "Bricks";
         case B_STONEBRICK: return "Stone Bricks";
         case B_SAND: return "Sand";
         case B_GRAVEL: return "Gravel";
         case B_SANDSTONE: return "Sandstone";
         case B_GLASS: return "Glass";
         case B_LOG_OAK: return "Oak Log";
         case B_LOG_SPRUCE: return "Spruce Log";
         case B_LEAVES_OAK: return "Oak Leaves";
         case B_LEAVES_SPRUCE: return "Spruce Leaves";
         case B_WOOL_WHITE: return "White Wool";
         case B_WOOL_RED: return "Red Wool";
         case B_WOOL_BLUE: return "Blue Wool";
         case B_WOOL_GREEN: return "Green Wool";
         case B_WOOL_YELLOW: return "Yellow Wool";
         case B_GOLD_ORE: return "Gold Ore";
         case B_IRON_ORE: return "Iron Ore";
         case B_COAL_ORE: return "Coal Ore";
         case B_DIAMOND_ORE: return "Diamond Ore";
         case B_REDSTONE_ORE: return "Redstone Ore";
         case B_GOLD_BLOCK: return "Gold Block";
         case B_DIAMOND_BLOCK: return "Diamond Block";
         case B_EMERALD_BLOCK: return "Emerald Block";
         case B_IRON_BLOCK: return "Iron Block";
         case B_OBSIDIAN: return "Obsidian";
         case B_END_STONE: return "End Stone";
         case B_QUARTZ_BLOCK: return "Quartz Block";
         case B_GLOWSTONE: return "Glowstone";
         case B_SEA_LANTERN: return "Sea Lantern";
         case B_TORCH: return "Torch";
         case B_TNT: return "TNT";
         case B_SLIME_BLOCK: return "Slime Block";
         case B_BED_RED: return "Bed";
         case B_CRAFTING_TABLE: return "Crafting Table";
         case B_FURNACE: return "Furnace";
         case B_CHEST: return "Chest";
         case B_BOOKSHELF: return "Bookshelf";
         case B_SNOW: return "Snow Block";
         case B_ICE: return "Ice";
         case B_CACTUS: return "Cactus";
         case B_TALL_GRASS: return "Tall Grass";
         case B_FLOWER_RED: return "Red Poppy";
         case B_FLOWER_YELLOW: return "Dandelion";
         case I_SWORD_WOOD: return "Wooden Sword";
         case I_PICK_WOOD: return "Wooden Pickaxe";
         case I_SWORD_IRON: return "Iron Sword";
         case I_SWORD_DIAMOND: return "Diamond Sword";
         case I_PICK_IRON: return "Iron Pickaxe";
         case I_PICK_DIAMOND: return "Diamond Pickaxe";
         case I_SHOVEL_DIAMOND: return "Diamond Shovel";
         case I_AXE_IRON: return "Iron Axe";
         case I_BOW: return "Bow";
         case I_ARROW: return "Arrows";
         case I_GOLDEN_APPLE: return "Golden Apple";
         case I_STEAK: return "Cooked Steak";
         case I_ENDER_PEARL: return "Ender Pearl";
         default: return "Block";
      }
   }

   // ====================================================================
   //  Right-Handed Camera Forward Vector & Raycasting
   //  Forward = ( +sin(yaw)*cos(pitch), -sin(pitch), -cos(yaw)*cos(pitch) )
   //  Right   = ( +cos(yaw),             0,          +sin(yaw)            )
   // ====================================================================
   public int[] raycastBlock(double maxReach) {
      double ex = this.px;
      double ey = this.py + (this.sneaking && !this.flying ? 1.46 : 1.62);
      double ez = this.pz;
      double yawRad = Math.toRadians(this.yaw);
      double pitchRad = Math.toRadians(this.pitch);
      double cosP = Math.cos(pitchRad);
      double dx = Math.sin(yawRad) * cosP;
      double dy = -Math.sin(pitchRad);
      double dz = -Math.cos(yawRad) * cosP;

      int prevX = (int)Math.floor(ex);
      int prevY = (int)Math.floor(ey);
      int prevZ = (int)Math.floor(ez);
      for (double t = 0.2; t <= maxReach; t += 0.06) {
         int bx = (int)Math.floor(ex + dx * t);
         int by = (int)Math.floor(ey + dy * t);
         int bz = (int)Math.floor(ez + dz * t);
         byte b = getBlockFast(bx, by, bz);
         if (b != B_AIR && b != B_WATER && b != B_LAVA) {
            return new int[]{bx, by, bz, prevX, prevY, prevZ, b & 0xFF};
         }
         prevX = bx;
         prevY = by;
         prevZ = bz;
      }
      return null;
   }

   // Dedicated Break / Mine tap action
   public void triggerAttackAction(PeAudio audio) {
      this.swingAnim = 0.0F;
      // In Creative Mode (or on instant-break plants/torches), tapping MINE breaks immediately
      int[] hit = raycastBlock(5.5);
      if (hit != null && hit[6] != B_BEDROCK) {
         byte target = (byte)hit[6];
         if (this.mode == MODE_CREATIVE || isCrossPlant(target)) {
            setBlock(hit[0], hit[1], hit[2], B_AIR);
            if (this.mode == MODE_SURVIVAL && target == B_TORCH) {
               giveItem(B_TORCH, 1);
            }
            if (audio != null) audio.playSound("dig_stone", 0.8F, 1.0F);
            return;
         }
      }
      if (audio != null) {
         audio.playSound("swing_1", 0.6F, 1.0F);
      }
   }

   // Dedicated Place / Use / Interact tap action
   public void triggerPlaceOrUseAction(PeAudio audio) {
      this.swingAnim = 0.0F;
      int held = this.hotbarId[this.selectedSlot];

      // 1. Check if tapping a Crafting Table, Furnace, Chest, or Bed in the world
      int[] hit = raycastBlock(5.0);
      if (hit != null && !this.sneaking) {
         byte targetBlock = (byte)hit[6];
         if (targetBlock == B_CRAFTING_TABLE || targetBlock == B_FURNACE || targetBlock == B_CHEST) {
            this.previousScreen = SCREEN_PLAY;
            this.screen = SCREEN_INVENTORY;
            if (audio != null) audio.playSound("click", 0.85F, 1.1F);
            return;
         }
         if (targetBlock == B_BED_RED) {
            this.dayTime = 0.22F;
            this.health = 20;
            showBanner("You slept through the night! Spawn point set.");
            if (audio != null) audio.playSound("levelup", 0.85F, 1.0F);
            return;
         }
      }

      // 2. Eat Food if holding Golden Apple or Steak
      if (held == I_GOLDEN_APPLE || held == I_STEAK) {
         if (this.health < 20 || this.hunger < 20) {
            this.health = Math.min(20, this.health + (held == I_GOLDEN_APPLE ? 8 : 6));
            this.hunger = Math.min(20, this.hunger + 6);
            consumeSelectedSlot(1);
            showBanner("Ate " + getItemName(held) + " (+HP)");
            if (audio != null) audio.playSound("eat_bite", 0.85F, 1.0F);
         }
         return;
      }

      // 3. Place Block at targeted block face
      if (hit != null && held > 0 && held <= 68) {
         int pxPlace = hit[3], pyPlace = hit[4], pzPlace = hit[5];
         boolean overlapsPlayer = isSolid((byte)held)
            && Math.abs((pxPlace + 0.5) - this.px) < 0.75
            && Math.abs((pzPlace + 0.5) - this.pz) < 0.75
            && pyPlace >= Math.floor(this.py) && pyPlace <= Math.floor(this.py + 1.7);
         byte existing = getBlock(pxPlace, pyPlace, pzPlace);
         if (!overlapsPlayer && (existing == B_AIR || existing == B_WATER || existing == B_TALL_GRASS)) {
            setBlock(pxPlace, pyPlace, pzPlace, (byte)held);
            if (this.mode != MODE_CREATIVE) {
               consumeSelectedSlot(1);
            }
            if (audio != null) audio.playSound("click_block", 0.8F, 1.05F);
         }
      }
   }

   // Legacy alias for tests
   public void triggerPrimaryAction(PeAudio audio) {
      int held = this.hotbarId[this.selectedSlot];
      if (held > 0 && held <= 68) {
         triggerPlaceOrUseAction(audio);
      } else {
         triggerAttackAction(audio);
      }
   }

   private void consumeSelectedSlot(int n) {
      if (this.mode == MODE_CREATIVE) return;
      this.hotbarCount[this.selectedSlot] -= n;
      if (this.hotbarCount[this.selectedSlot] <= 0) {
         this.hotbarId[this.selectedSlot] = 0;
         this.hotbarCount[this.selectedSlot] = 0;
      }
   }

   public void tick(PeTouchController touch, PeAudio audio) {
      if (this.screen != SCREEN_PLAY) {
         return;
      }
      this.tickCount++;
      if (this.hurtTime > 0) this.hurtTime--;
      if (this.invulnTicks > 0) this.invulnTicks--;
      if (this.bannerTicks > 0) this.bannerTicks--;
      if (this.swingAnim >= 0.0F) {
         this.swingAnim += 0.16F;
         if (this.swingAnim >= 1.0F) this.swingAnim = -1.0F;
      }
      this.dayTime = (this.dayTime + 0.00008F) % 1.0F;

      // Stream infinite chunks around player (up to 2 new chunks per tick for zero stutter!)
      ensureChunksAroundPlayer(2);

      // 1. Player Movement & Right-Handed Camera Basis:
      //    Forward = ( +sin(yaw), -cos(yaw) )
      //    Right   = ( +cos(yaw), +sin(yaw) )
      double moveFwd = touch != null ? touch.moveForward : 0.0;
      double moveStrafe = touch != null ? touch.moveStrafe : 0.0;
      boolean wantJump = touch != null && touch.jumpHeld;
      boolean wantDescend = touch != null && touch.sneakHeld;

      this.sprinting = (moveFwd > 0.72 && !this.sneaking);
      double speed = this.flying ? 0.36 : (this.sneaking ? 0.10 : (this.sprinting ? 0.235 : 0.185));
      double yawRad = Math.toRadians(this.yaw);
      double sinY = Math.sin(yawRad);
      double cosY = Math.cos(yawRad);
      double wishX = (sinY * moveFwd + cosY * moveStrafe) * speed;
      double wishZ = (-cosY * moveFwd + sinY * moveStrafe) * speed;

      this.vx = this.vx * 0.50 + wishX * 0.50;
      this.vz = this.vz * 0.50 + wishZ * 0.50;

      byte inBlock = getBlockFast((int)Math.floor(this.px), (int)Math.floor(this.py + 0.4), (int)Math.floor(this.pz));
      boolean inWater = (inBlock == B_WATER);

      if (this.flying) {
         this.vy = wantJump ? 0.25 : (wantDescend ? -0.25 : 0.0);
      } else if (inWater) {
         this.vy = wantJump ? 0.14 : Math.max(-0.18, this.vy - 0.025);
      } else {
         if (wantJump && this.onGround) {
            this.vy = 0.42;
            this.onGround = false;
         }
         this.vy = Math.max(-0.78, this.vy - 0.08);
      }

      movePlayerWithCollision();

      // Void Respawn Protection
      if (this.py < 2.0) {
         this.px = 0.5;
         this.py = topSolidY(0, 0) + 1.5;
         this.pz = 0.5;
         this.vx = this.vy = this.vz = 0.0;
         this.health = 20;
         showBanner("Respawned at World Spawn!");
      }

      // 2. Hold-to-Mine Block Progression (ONLY when miningHeld is explicitly active!)
      if (touch != null && touch.miningHeld) {
         int[] hit = raycastBlock(5.0);
         if (hit != null && hit[6] != B_BEDROCK) {
            if (hit[0] == this.mineX && hit[1] == this.mineY && hit[2] == this.mineZ) {
               int held = this.hotbarId[this.selectedSlot];
               float toolMult = (held == I_PICK_DIAMOND || held == I_SHOVEL_DIAMOND) ? 2.4F
                  : ((held == I_PICK_IRON || held == I_AXE_IRON) ? 1.8F : (held == I_PICK_WOOD ? 1.35F : 1.0F));
               float rate = (this.mode == MODE_CREATIVE) ? 0.55F : (0.068F * toolMult);
               this.mineProgress += rate;
               this.swingAnim = (this.tickCount % 6) / 6.0F;
               if (this.mineProgress >= 1.0F) {
                  byte broken = getBlock(this.mineX, this.mineY, this.mineZ);
                  setBlock(this.mineX, this.mineY, this.mineZ, B_AIR);
                  if (this.mode == MODE_SURVIVAL && broken != B_AIR) {
                     byte drop = (broken == B_STONE) ? B_COBBLE : ((broken == B_GRASS) ? B_DIRT : broken);
                     if (drop != B_LEAVES_OAK && drop != B_LEAVES_SPRUCE && drop != B_TALL_GRASS) {
                        giveItem(drop, 1);
                     }
                  }
                  if (audio != null) audio.playSound("dig_stone", 0.85F, 1.0F);
                  this.mineProgress = 0.0F;
               }
            } else {
               this.mineX = hit[0];
               this.mineY = hit[1];
               this.mineZ = hit[2];
               this.mineProgress = 0.06F;
            }
         } else {
            this.mineY = -1;
            this.mineProgress = 0.0F;
         }
      } else {
         this.mineY = -1;
         this.mineProgress = 0.0F;
      }
   }

   private void movePlayerWithCollision() {
      double x = this.px;
      double y = this.py;
      double z = this.pz;

      // Y collision
      double ny = y + this.vy;
      int bx = (int)Math.floor(x);
      int bz = (int)Math.floor(z);
      boolean grounded = false;
      if (this.vy <= 0.0 && isSolid(getBlockFast(bx, (int)Math.floor(ny), bz))) {
         ny = Math.floor(ny) + 1.0;
         this.vy = 0.0;
         grounded = true;
      } else if (this.vy > 0.0 && isSolid(getBlockFast(bx, (int)Math.floor(ny + 1.75), bz))) {
         this.vy = 0.0;
      }

      // X & Z collision + Optional Auto Step-Jump (Pocket Edition auto-jump)
      double nx = x + this.vx;
      double nz = z + this.vz;
      int nbx = (int)Math.floor(nx);
      int nbz = (int)Math.floor(nz);
      int footY = (int)Math.floor(ny + 0.1);
      if (isSolid(getBlockFast(nbx, footY, nbz)) || isSolid(getBlockFast(nbx, footY + 1, nbz))) {
         boolean allowAutoJump = this.autoJump && !this.sneaking && !this.flying;
         if (grounded && allowAutoJump
            && isSolid(getBlockFast(nbx, footY, nbz))
            && !isSolid(getBlockFast(nbx, footY + 1, nbz))
            && !isSolid(getBlockFast(nbx, footY + 2, nbz))) {
            this.vy = 0.42;
            grounded = false;
         } else {
            if (isSolid(getBlockFast(nbx, footY, bz)) || isSolid(getBlockFast(nbx, footY + 1, bz))) nx = x;
            if (isSolid(getBlockFast(bx, footY, nbz)) || isSolid(getBlockFast(bx, footY + 1, nbz))) nz = z;
         }
      }

      if (Math.hypot(nx - this.px, nz - this.pz) > 0.001 && grounded) {
         this.walkDist += (float)Math.hypot(nx - this.px, nz - this.pz);
      }
      this.px = nx;
      this.py = ny;
      this.pz = nz;
      this.onGround = grounded;
   }
}
