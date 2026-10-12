package com.blockcraft.pe;

import android.content.Context;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Blockcraft PE (Pocket Edition) Single-Player World Engine:
 * - Multi-Biome Overworld Generation (Large Biomes Recommended, Default Biomes, Superflat)
 *   with Caves, Ore Veins, Trees, Desert Cacti, Snowy Peaks, Water Lakes & Village Cabins
 * - Persistent GZIP World Saving & Loading across 3 Save Slots (world_0..2.bcpe)
 * - Right-Handed Camera, Raycasting & AABB Physics Collision (with optional Auto-Jump)
 * - Survival Mining, Item Collection, Crafting & Creative Building
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

   // Item IDs (100+)
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

   // World Dimensions: 10x10 chunks (160x160 horizontal: wx, wz in -80..+79), Y in 0..127
   public static final int CHUNK_GRID = 10;
   public static final int HALF_CHUNKS = 5;
   public static final int WORLD_W = CHUNK_GRID * 16; // 160
   public static final int HALF_W = WORLD_W / 2;      // 80
   public static final int WORLD_H = 128;
   public static final int SECTIONS_Y = WORLD_H / 16; // 8
   public static final int Y_SEA = 60;
   public static final int Y_DECK = 64;

   private static final int SAVE_MAGIC = 0x42435045; // "BCPE"
   private static final int SAVE_VERSION = 1;

   public final byte[] blocks = new byte[WORLD_W * WORLD_W * WORLD_H];
   public final boolean[][] sectionDirty = new boolean[CHUNK_GRID * CHUNK_GRID][SECTIONS_Y];

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
   public float dayTime = 0.22F; // 0..1 day/night cycle

   // Player State
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

   // Feedback & HUD State
   public int hitmarkerTicks = 0;
   public boolean hitmarkerCrit = false;
   public int kills = 0;
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
      new CraftRecipe("Torches x4 (1 Coal + 1 Plank)", B_TORCH, 4, B_COAL_ORE, 1),
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

   // Entities: Overworld Mobs & Projectiles
   public static final class PeMob {
      public int id;
      public int type; // 1=Zombie, 2=Skeleton, 3=Creeper, 4=Villager/Guard
      public int team = -1;
      public String name;
      public double x, y, z;
      public double vx, vy, vz;
      public float yaw, pitch;
      public float walkPhase;
      public int health = 20;
      public int maxHealth = 20;
      public int hurtTime = 0;
      public int attackCd = 0;
      public int heldItem = I_SWORD_IRON;
      public boolean onGround = false;
      public boolean dead = false;
   }

   public static final class PeProjectile {
      public int type; // 0=Arrow
      public int sourceTeam; // -2=Player, -1=Mob
      public double x, y, z;
      public double vx, vy, vz;
      public int fuse = 60;
      public int damage = 6;
      public boolean dead = false;
   }

   public final ArrayList<PeMob> mobs = new ArrayList<PeMob>();
   public final ArrayList<PeProjectile> projectiles = new ArrayList<PeProjectile>();
   private final Random rng = new Random(1337L);

   // ====================================================================
   //  Persistent Save Directory & World Save/Load (GZIP .bcpe files)
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
         this.slotExists[s] = f.exists() && f.length() > 64;
         if (this.slotExists[s] && meta.exists()) {
            try {
               DataInputStream in = new DataInputStream(new FileInputStream(meta));
               String summary = in.readUTF();
               in.close();
               this.slotSummary[s] = summary;
            } catch (Exception e) {
               this.slotSummary[s] = "Saved World (Tap to Load)";
            }
         } else if (this.slotExists[s]) {
            this.slotSummary[s] = "Saved World (Tap to Load)";
         } else {
            this.slotSummary[s] = "Empty Slot - Tap to Create New World";
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

   public boolean saveWorld(Context context) {
      if (this.screen == SCREEN_TITLE || this.screen == SCREEN_WORLD_SELECT || this.screen == SCREEN_CREATE_WORLD) {
         return false;
      }
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
         out.writeInt(SAVE_VERSION);
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
         out.write(this.blocks, 0, this.blocks.length);
         out.flush();
         out.close();

         if (dstFile.exists()) dstFile.delete();
         tmpFile.renameTo(dstFile);

         int dayNum = (int)(this.tickCount / 24000L) + 1;
         String wTypeShort = this.worldType == WTYPE_LARGE_BIOMES ? "Large Biomes" : (this.worldType == WTYPE_SUPERFLAT ? "Superflat" : "Default");
         String summary = MODE_NAMES[this.mode] + " | " + wTypeShort + " | Day " + dayNum;
         DataOutputStream mOut = new DataOutputStream(new FileOutputStream(metaFile));
         mOut.writeUTF(summary);
         mOut.close();

         refreshSaveSlots(context);
         showBanner("World Saved (" + "World " + (slot + 1) + ")");
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
         if (magic != SAVE_MAGIC || ver != SAVE_VERSION) {
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
         in.readFully(this.blocks);
         in.close();

         this.vx = this.vy = this.vz = 0.0;
         this.mineY = -1;
         this.mineProgress = 0.0F;
         this.mobs.clear();
         this.projectiles.clear();
         spawnOverworldMobs();
         markAllDirty();
         this.screen = SCREEN_PLAY;
         showBanner("Loaded World " + (slot + 1) + " (" + MODE_NAMES[this.mode] + ")");
         return true;
      } catch (Exception e) {
         return false;
      }
   }

   public void createNewSinglePlayerWorld(Context context, int slot, int newMode, int newWorldType, long customSeed) {
      this.currentSlot = Math.max(0, Math.min(2, slot));
      this.mode = newMode;
      this.worldType = newWorldType;
      this.seed = (customSeed != 0L) ? customSeed : (System.currentTimeMillis() ^ 0x5DEECE66DL);
      this.rng.setSeed(this.seed);

      Arrays.fill(this.blocks, B_AIR);
      this.mobs.clear();
      this.projectiles.clear();
      this.kills = 0;
      this.health = 20;
      this.hunger = 20;
      this.hurtTime = 0;
      this.invulnTicks = 40;
      this.flying = false;
      this.tickCount = 0L;
      this.dayTime = 0.20F;

      if (this.worldType == WTYPE_SUPERFLAT) {
         generateSuperflatWorld();
      } else {
         generateOverworldBiomes();
      }
      equipDefaultLoadout();
      markAllDirty();
      this.screen = SCREEN_PLAY;
      saveWorld(context);
      showBanner("Created World " + (this.currentSlot + 1) + " - " + WTYPE_NAMES[this.worldType]);
   }

   // Backwards-compatible helper for tests/preview
   public void generateWorld(Context context, int newMode, int worldTypeOrMap) {
      createNewSinglePlayerWorld(context, this.currentSlot, newMode <= 1 ? newMode : MODE_SURVIVAL, worldTypeOrMap % 3, 884219551L + worldTypeOrMap * 997L);
   }

   public String currentWorldSummary() {
      return "World " + (this.currentSlot + 1) + " (" + MODE_NAMES[this.mode] + ")";
   }

   public void showBanner(String msg) {
      this.bannerMessage = msg;
      this.bannerTicks = 100;
   }

   private void equipDefaultLoadout() {
      Arrays.fill(this.hotbarId, 0);
      Arrays.fill(this.hotbarCount, 0);
      this.selectedSlot = 0;
      if (this.mode == MODE_CREATIVE) {
         setSlot(0, B_GRASS, 64);
         setSlot(1, B_STONEBRICK, 64);
         setSlot(2, B_PLANKS_OAK, 64);
         setSlot(3, B_BRICKS, 64);
         setSlot(4, B_GLASS, 64);
         setSlot(5, B_TORCH, 64);
         setSlot(6, B_GLOWSTONE, 64);
         setSlot(7, B_GOLD_BLOCK, 64);
         setSlot(8, I_SWORD_DIAMOND, 1);
      } else {
         // Survival starter tools & blocks
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
      // Stack into existing matching slot first
      for (int i = 0; i < 9; i++) {
         if (this.hotbarId[i] == id && this.hotbarCount[i] < 64) {
            this.hotbarCount[i] = Math.min(64, this.hotbarCount[i] + count);
            return;
         }
      }
      // Empty slot
      for (int i = 0; i < 9; i++) {
         if (this.hotbarId[i] == 0 || this.hotbarCount[i] <= 0) {
            this.hotbarId[i] = id;
            this.hotbarCount[i] = count;
            return;
         }
      }
      // Replace currently selected slot if full
      this.hotbarId[this.selectedSlot] = id;
      this.hotbarCount[this.selectedSlot] = count;
   }

   // ====================================================================
   //  Single-Player Overworld Terrain & Biome Generator
   // ====================================================================
   private void generateSuperflatWorld() {
      for (int wz = -HALF_W; wz < HALF_W; wz++) {
         for (int wx = -HALF_W; wx < HALF_W; wx++) {
            setFast(wx, 0, wz, B_BEDROCK);
            for (int y = 1; y <= 59; y++) {
               setFast(wx, y, wz, B_STONE);
            }
            for (int y = 60; y <= 62; y++) {
               setFast(wx, y, wz, B_DIRT);
            }
            setFast(wx, 63, wz, B_GRASS);
         }
      }
      buildVillageHouse(12, 63, -12);
      this.px = 0.5;
      this.py = 64.1;
      this.pz = 0.5;
      this.yaw = 0.0F;
      this.pitch = 8.0F;
      spawnOverworldMobs();
   }

   private void generateOverworldBiomes() {
      double biomeFreq = (this.worldType == WTYPE_LARGE_BIOMES) ? 0.018 : 0.036;
      double sOffX = ((this.seed & 0xFFFFL) - 32768.0) * 0.01;
      double sOffZ = (((this.seed >>> 16) & 0xFFFFL) - 32768.0) * 0.01;

      for (int wz = -HALF_W; wz < HALF_W; wz++) {
         for (int wx = -HALF_W; wx < HALF_W; wx++) {
            double bx = (wx + sOffX) * biomeFreq;
            double bz = (wz + sOffZ) * biomeFreq;
            double temp = Math.sin(bx + 0.5) * Math.cos(bz - 0.3);
            double humid = Math.cos(bx * 0.8 - 1.1) * Math.sin(bz * 0.8 + 0.7);

            // Biomes: 0=Plains/Forest, 1=Desert, 2=Snowy Taiga, 3=Mountain Hills
            int biome = 0;
            if (temp > 0.38 && humid < 0.1) biome = 1;      // Desert
            else if (temp < -0.36) biome = 2;               // Snowy Taiga
            else if (humid > 0.42) biome = 3;               // Extreme Hills

            double n1 = Math.sin((wx + sOffX) * 0.045) * Math.cos((wz + sOffZ) * 0.045);
            double n2 = Math.sin((wx + wz) * 0.09) * 0.5 + Math.cos((wx - wz) * 0.11) * 0.5;
            double mtn = Math.max(0.0, Math.sin((wx - sOffX) * 0.028) * Math.sin((wz - sOffZ) * 0.028));
            double hillScale = (biome == 3) ? 18.0 : 10.0;
            int h = (int)Math.round(62.0 + n1 * 5.5 + n2 * 2.5 + mtn * hillScale);

            // Keep spawn area (r < 10) flat and welcoming
            double spawnDist = Math.hypot(wx, wz);
            if (spawnDist < 12.0) {
               double blend = spawnDist / 12.0;
               h = (int)Math.round(64.0 * (1.0 - blend) + h * blend);
            }

            setFast(wx, 0, wz, B_BEDROCK);
            for (int y = 1; y <= h; y++) {
               // Underground 3D Cave tunnels (below Y=50)
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
                  else if (biome == 2 || h >= 76) b = B_SNOW;
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
               setFast(wx, y, wz, b);
            }
            for (int y = h + 1; y <= Y_SEA; y++) {
               setFast(wx, y, wz, (biome == 2 && y == Y_SEA) ? B_ICE : B_WATER);
            }
         }
      }

      // Trees & Desert Cacti across the Overworld
      for (int wz = -68; wz <= 68; wz += 8) {
         for (int wx = -68; wx <= 68; wx += 8) {
            if (Math.hypot(wx, wz) < 11.0 || Math.hypot(wx - 14, wz + 14) < 9.0) continue;
            int tx = wx + ((wz * 3) & 3);
            int tz = wz + ((wx * 5) & 3);
            int ty = topSolidY(tx, tz);
            byte top = getBlock(tx, ty, tz);
            if (ty > Y_SEA + 1 && ty < 76 && (top == B_GRASS || top == B_SNOW)) {
               byte log = (top == B_SNOW || ((tx + tz) & 3) == 0) ? B_LOG_SPRUCE : B_LOG_OAK;
               placeTree(tx, ty + 1, tz, log);
            } else if (ty > Y_SEA + 1 && top == B_SAND && ((tx ^ tz) & 3) == 0) {
               for (int ch = 1; ch <= 3; ch++) {
                  setFast(tx, ty + ch, tz, B_WOOL_GREEN);
               }
            }
         }
      }

      // Build a furnished Starter Village Cabin near spawn at (14, -14)
      int houseY = topSolidY(14, -14);
      if (houseY < Y_SEA + 1) houseY = Y_SEA + 1;
      buildVillageHouse(14, houseY, -14);

      int spawnY = topSolidY(0, 0) + 1;
      this.px = 0.5;
      this.py = spawnY + 0.05;
      this.pz = 0.5;
      this.yaw = 0.0F; // Looking toward -Z (where the Village Cabin at z=-14 is visible!)
      this.pitch = 6.0F;

      spawnOverworldMobs();
   }

   private void spawnOverworldMobs() {
      this.mobs.clear();
      String[] mobNames = new String[]{"Zombie", "Skeleton", "Creeper", "Zombie", "Skeleton", "Creeper"};
      for (int i = 0; i < 6; i++) {
         double ang = i * (Math.PI / 3.0) + 0.4;
         int mx = (int)Math.round(Math.cos(ang) * 24.0);
         int mz = (int)Math.round(Math.sin(ang) * 24.0);
         int my = topSolidY(mx, mz) + 1;
         PeMob m = new PeMob();
         m.id = i + 1;
         m.type = 1 + (i % 3);
         m.team = -1;
         m.name = mobNames[i];
         m.x = mx + 0.5;
         m.y = my;
         m.z = mz + 0.5;
         m.health = 20;
         m.maxHealth = 20;
         this.mobs.add(m);
      }
   }

   private void placeTree(int x, int y, int z, byte logId) {
      for (int dy = 0; dy < 5; dy++) {
         setFast(x, y + dy, z, logId);
      }
      for (int dy = 3; dy <= 6; dy++) {
         int r = (dy >= 5) ? 1 : 2;
         for (int dz = -r; dz <= r; dz++) {
            for (int dx = -r; dx <= r; dx++) {
               if (dx == 0 && dz == 0 && dy < 5) continue;
               if (Math.abs(dx) == 2 && Math.abs(dz) == 2 && dy == 4) continue;
               setFast(x + dx, y + dy, z + dz, B_LEAVES_OAK);
            }
         }
      }
   }

   private void buildVillageHouse(int ox, int oy, int oz) {
      for (int dz = -3; dz <= 3; dz++) {
         for (int dx = -3; dx <= 3; dx++) {
            for (int y = oy - 3; y <= oy; y++) {
               setFast(ox + dx, y, oz + dz, B_COBBLE);
            }
            boolean wall = Math.abs(dx) == 3 || Math.abs(dz) == 3;
            boolean door = (dz == 3 && dx == 0);
            for (int dy = 1; dy <= 4; dy++) {
               if (wall && !(door && dy <= 2)) {
                  boolean corner = Math.abs(dx) == 3 && Math.abs(dz) == 3;
                  boolean win = dy == 2 && (dx == 0 || dz == 0) && !door;
                  setFast(ox + dx, oy + dy, oz + dz, corner ? B_LOG_OAK : (win ? B_GLASS : B_PLANKS_OAK));
               } else {
                  setFast(ox + dx, oy + dy, oz + dz, B_AIR);
               }
            }
            setFast(ox + dx, oy + 5, oz + dz, B_PLANKS_SPRUCE);
         }
      }
      setFast(ox - 2, oy + 1, oz - 2, B_CRAFTING_TABLE);
      setFast(ox, oy + 1, oz - 2, B_FURNACE);
      setFast(ox + 2, oy + 1, oz - 2, B_CHEST);
      setFast(ox - 2, oy + 1, oz + 1, B_BED_RED);
      setFast(ox, oy + 3, oz, B_TORCH);
   }

   private void setFast(int wx, int wy, int wz, byte id) {
      int ix = wx + HALF_W;
      int iz = wz + HALF_W;
      if (ix >= 0 && ix < WORLD_W && iz >= 0 && iz < WORLD_W && wy >= 0 && wy < WORLD_H) {
         this.blocks[(iz * WORLD_W + ix) * WORLD_H + wy] = id;
      }
   }

   public int topSolidY(int wx, int wz) {
      for (int y = WORLD_H - 2; y >= 1; y--) {
         byte b = getBlock(wx, y, wz);
         if (isSolid(b) && b != B_LEAVES_OAK) {
            return y;
         }
      }
      return Y_DECK;
   }

   public void markAllDirty() {
      for (int i = 0; i < sectionDirty.length; i++) {
         Arrays.fill(sectionDirty[i], true);
      }
   }

   public byte getBlock(int wx, int wy, int wz) {
      int ix = wx + HALF_W;
      int iz = wz + HALF_W;
      if (ix < 0 || ix >= WORLD_W || iz < 0 || iz >= WORLD_W || wy < 0 || wy >= WORLD_H) {
         return B_AIR;
      }
      return this.blocks[(iz * WORLD_W + ix) * WORLD_H + wy];
   }

   public void setBlock(int wx, int wy, int wz, byte id) {
      int ix = wx + HALF_W;
      int iz = wz + HALF_W;
      if (ix < 0 || ix >= WORLD_W || iz < 0 || iz >= WORLD_W || wy < 0 || wy >= WORLD_H) {
         return;
      }
      int idx = (iz * WORLD_W + ix) * WORLD_H + wy;
      if (this.blocks[idx] == id) {
         return;
      }
      this.blocks[idx] = id;
      int cx = ix >> 4;
      int cz = iz >> 4;
      int sy = wy >> 4;
      this.sectionDirty[cz * CHUNK_GRID + cx][sy] = true;
      if ((ix & 15) == 0 && cx > 0) this.sectionDirty[cz * CHUNK_GRID + (cx - 1)][sy] = true;
      if ((ix & 15) == 15 && cx + 1 < CHUNK_GRID) this.sectionDirty[cz * CHUNK_GRID + (cx + 1)][sy] = true;
      if ((iz & 15) == 0 && cz > 0) this.sectionDirty[(cz - 1) * CHUNK_GRID + cx][sy] = true;
      if ((iz & 15) == 15 && cz + 1 < CHUNK_GRID) this.sectionDirty[(cz + 1) * CHUNK_GRID + cx][sy] = true;
      if ((wy & 15) == 0 && sy > 0) this.sectionDirty[cz * CHUNK_GRID + cx][sy - 1] = true;
      if ((wy & 15) == 15 && sy + 1 < SECTIONS_Y) this.sectionDirty[cz * CHUNK_GRID + cx][sy + 1] = true;
   }

   public static boolean isSolid(byte id) {
      return id != B_AIR && id != B_WATER && id != B_LAVA && id != B_TORCH;
   }

   public static boolean isOpaque(byte id) {
      return id != B_AIR && id != B_WATER && id != B_LAVA && id != B_GLASS && id != B_LEAVES_OAK
         && id != B_ICE && id != B_SLIME_BLOCK && id != B_TORCH && id != B_BARS && id != B_BED_RED;
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
         case B_SNOW: return PeTextures.T_SNOW;
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
         default: return PeTextures.T_STONE;
      }
   }

   public static int getItemTile(int id) {
      if (id > 0 && id < 64) {
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
         case B_WOOL_WHITE: return "White Wool";
         case B_WOOL_RED: return "Red Wool";
         case B_WOOL_BLUE: return "Blue Wool";
         case B_WOOL_GREEN: return "Green Wool";
         case B_WOOL_YELLOW: return "Yellow Wool";
         case B_GOLD_ORE: return "Gold Ore";
         case B_IRON_ORE: return "Iron Ore";
         case B_COAL_ORE: return "Coal";
         case B_DIAMOND_ORE: return "Diamond";
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
         byte b = getBlock(bx, by, bz);
         if (b != B_AIR && b != B_WATER && b != B_LAVA) {
            return new int[]{bx, by, bz, prevX, prevY, prevZ, b & 0xFF};
         }
         prevX = bx;
         prevY = by;
         prevZ = bz;
      }
      return null;
   }

   public PeMob raycastMob(double maxReach) {
      double ex = this.px;
      double ey = this.py + 1.62;
      double ez = this.pz;
      double yawRad = Math.toRadians(this.yaw);
      double pitchRad = Math.toRadians(this.pitch);
      double cosP = Math.cos(pitchRad);
      double dx = Math.sin(yawRad) * cosP;
      double dy = -Math.sin(pitchRad);
      double dz = -Math.cos(yawRad) * cosP;

      PeMob best = null;
      double bestD = maxReach;
      for (PeMob m : this.mobs) {
         if (m == null || m.dead) continue;
         double mx = m.x - ex;
         double my = (m.y + 0.9) - ey;
         double mz = m.z - ez;
         double proj = mx * dx + my * dy + mz * dz;
         if (proj > 0.0 && proj < bestD) {
            double perpSq = (mx * mx + my * my + mz * mz) - proj * proj;
            if (perpSq <= 0.85) {
               bestD = proj;
               best = m;
            }
         }
      }
      return best;
   }

   // Dedicated Attack / Break tap action
   public void triggerAttackAction(PeAudio audio) {
      this.swingAnim = 0.0F;
      int held = this.hotbarId[this.selectedSlot];

      // Melee Attack on Targeted Mob
      PeMob hitMob = raycastMob(3.8);
      if (hitMob != null) {
         boolean isCrit = !this.onGround && this.vy < -0.05;
         int dmg = (held == I_SWORD_DIAMOND ? 8 : (held == I_SWORD_IRON || held == I_AXE_IRON ? 6 : (held == I_SWORD_WOOD ? 5 : 3))) + (isCrit ? 2 : 0);
         damageMob(hitMob, dmg, this.px, this.pz, true, audio);
         return;
      }

      // In Creative Mode, tapping MINE/HIT breaks the targeted block immediately
      if (this.mode == MODE_CREATIVE) {
         int[] hit = raycastBlock(5.5);
         if (hit != null && hit[6] != B_BEDROCK) {
            setBlock(hit[0], hit[1], hit[2], B_AIR);
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

      // 1. Check if tapping a Crafting Table, Chest, or Bed in the world
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
            this.dayTime = 0.22F; // Morning!
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

      // 3. Shoot Bow if holding Bow
      if (held == I_BOW) {
         firePlayerProjectile(0, 2.1, 7, 60);
         if (audio != null) audio.playSound("bow_shoot", 0.85F, 1.1F);
         return;
      }

      // 4. Place Block at targeted block face
      if (hit != null && held > 0 && held < 64) {
         int pxPlace = hit[3], pyPlace = hit[4], pzPlace = hit[5];
         boolean overlapsPlayer = Math.abs((pxPlace + 0.5) - this.px) < 0.75
            && Math.abs((pzPlace + 0.5) - this.pz) < 0.75
            && pyPlace >= Math.floor(this.py) && pyPlace <= Math.floor(this.py + 1.7);
         if (!overlapsPlayer && getBlock(pxPlace, pyPlace, pzPlace) == B_AIR) {
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
      if (held > 0 && held < 64) {
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

   private void firePlayerProjectile(int type, double speed, int dmg, int fuse) {
      double yawRad = Math.toRadians(this.yaw);
      double pitchRad = Math.toRadians(this.pitch);
      double cosP = Math.cos(pitchRad);
      double dx = Math.sin(yawRad) * cosP;
      double dy = -Math.sin(pitchRad);
      double dz = -Math.cos(yawRad) * cosP;
      PeProjectile p = new PeProjectile();
      p.type = type;
      p.sourceTeam = -2;
      p.x = this.px + dx * 0.6;
      p.y = this.py + 1.52 + dy * 0.4;
      p.z = this.pz + dz * 0.6;
      p.vx = dx * speed;
      p.vy = dy * speed + 0.04;
      p.vz = dz * speed;
      p.damage = dmg;
      p.fuse = fuse;
      this.projectiles.add(p);
   }

   public void damageMob(PeMob m, int dmg, double srcX, double srcZ, boolean fromPlayer, PeAudio audio) {
      if (m == null || m.dead || m.hurtTime > 0) return;
      m.health -= dmg;
      m.hurtTime = 8;
      double dx = m.x - srcX;
      double dz = m.z - srcZ;
      double d = Math.max(0.1, Math.hypot(dx, dz));
      m.vx += (dx / d) * 0.42;
      m.vy = 0.34;
      m.vz += (dz / d) * 0.42;
      m.onGround = false;
      if (fromPlayer) {
         this.hitmarkerTicks = 10;
         this.hitmarkerCrit = m.health <= 0 || (!this.onGround && this.vy < -0.05);
      }
      if (audio != null) audio.playSound("hit_1", 0.85F, 1.0F);
      if (m.health <= 0) {
         m.dead = true;
         if (fromPlayer) {
            this.kills++;
            giveItem(I_STEAK, 1);
            showBanner("Defeated " + m.name + "! (+1 Steak)");
         }
      }
   }

   public void tick(PeTouchController touch, PeAudio audio) {
      if (this.screen != SCREEN_PLAY) {
         return;
      }
      this.tickCount++;
      if (this.hurtTime > 0) this.hurtTime--;
      if (this.invulnTicks > 0) this.invulnTicks--;
      if (this.hitmarkerTicks > 0) this.hitmarkerTicks--;
      if (this.bannerTicks > 0) this.bannerTicks--;
      if (this.swingAnim >= 0.0F) {
         this.swingAnim += 0.16F;
         if (this.swingAnim >= 1.0F) this.swingAnim = -1.0F;
      }
      this.dayTime = (this.dayTime + 0.00008F) % 1.0F;

      // 1. Player Movement & Right-Handed Camera Basis:
      //    Forward = ( +sin(yaw), -cos(yaw) )
      //    Right   = ( +cos(yaw), +sin(yaw) )
      double moveFwd = touch != null ? touch.moveForward : 0.0;
      double moveStrafe = touch != null ? touch.moveStrafe : 0.0;
      boolean wantJump = touch != null && touch.jumpHeld;
      boolean wantDescend = touch != null && touch.sneakHeld;

      double speed = this.flying ? 0.34 : (this.sneaking ? 0.10 : (this.sprinting ? 0.24 : 0.185));
      double yawRad = Math.toRadians(this.yaw);
      double sinY = Math.sin(yawRad);
      double cosY = Math.cos(yawRad);
      double wishX = (sinY * moveFwd + cosY * moveStrafe) * speed;
      double wishZ = (-cosY * moveFwd + sinY * moveStrafe) * speed;

      this.vx = this.vx * 0.50 + wishX * 0.50;
      this.vz = this.vz * 0.50 + wishZ * 0.50;

      byte inBlock = getBlock((int)Math.floor(this.px), (int)Math.floor(this.py + 0.4), (int)Math.floor(this.pz));
      boolean inWater = (inBlock == B_WATER);

      if (this.flying) {
         this.vy = wantJump ? 0.24 : (wantDescend ? -0.24 : 0.0);
      } else if (inWater) {
         this.vy = wantJump ? 0.14 : Math.max(-0.18, this.vy - 0.025);
      } else {
         if (wantJump && this.onGround) {
            this.vy = 0.42;
            this.onGround = false;
         }
         this.vy = Math.max(-0.78, this.vy - 0.08);
      }

      moveEntityWithCollision(true, null);

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
               float rate = (this.mode == MODE_CREATIVE) ? 0.55F : (0.065F * toolMult);
               this.mineProgress += rate;
               this.swingAnim = (this.tickCount % 6) / 6.0F;
               if (this.mineProgress >= 1.0F) {
                  byte broken = getBlock(this.mineX, this.mineY, this.mineZ);
                  setBlock(this.mineX, this.mineY, this.mineZ, B_AIR);
                  if (this.mode == MODE_SURVIVAL && broken != B_AIR) {
                     byte drop = (broken == B_STONE) ? B_COBBLE : ((broken == B_GRASS) ? B_DIRT : broken);
                     giveItem(drop, 1);
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

      // 3. Tick Projectiles
      for (int i = this.projectiles.size() - 1; i >= 0; i--) {
         PeProjectile p = this.projectiles.get(i);
         p.x += p.vx;
         p.y += p.vy;
         p.z += p.vz;
         p.vy -= 0.012;
         p.fuse--;
         int bx = (int)Math.floor(p.x);
         int by = (int)Math.floor(p.y);
         int bz = (int)Math.floor(p.z);
         byte hitB = getBlock(bx, by, bz);

         for (PeMob m : this.mobs) {
            if (m == null || m.dead) continue;
            if (Math.hypot(m.x - p.x, m.z - p.z) <= 0.85 && p.y >= m.y && p.y <= m.y + 1.9) {
               damageMob(m, p.damage, p.x - p.vx, p.z - p.vz, p.sourceTeam == -2, audio);
               p.dead = true;
               break;
            }
         }
         if (!p.dead && (isSolid(hitB) || p.fuse <= 0)) {
            p.dead = true;
         }
         if (p.dead) {
            this.projectiles.remove(i);
         }
      }

      // 4. Tick Overworld Mobs
      tickOverworldMobs(audio);
   }

   private void tickOverworldMobs(PeAudio audio) {
      for (PeMob m : this.mobs) {
         if (m == null || m.dead) continue;
         if (m.hurtTime > 0) m.hurtTime--;
         if (m.attackCd > 0) m.attackCd--;

         double dx = this.px - m.x;
         double dz = this.pz - m.z;
         double dist = Math.hypot(dx, dz);

         if (this.mode == MODE_SURVIVAL && dist < 16.0 && dist > 0.7) {
            m.yaw = (float)Math.toDegrees(Math.atan2(dx, -dz));
            double spd = 0.095;
            m.vx = (dx / dist) * spd;
            m.vz = (dz / dist) * spd;
            m.walkPhase += 0.25F;
         } else {
            // Peaceful wander
            if (((this.tickCount + m.id * 19) % 80) == 0) {
               m.yaw += (this.rng.nextFloat() - 0.5F) * 90.0F;
            }
            double rad = Math.toRadians(m.yaw);
            m.vx = Math.sin(rad) * 0.04;
            m.vz = -Math.cos(rad) * 0.04;
            m.walkPhase += 0.12F;
         }
         m.vy = Math.max(-0.75, m.vy - 0.08);
         moveEntityWithCollision(false, m);

         if (this.mode == MODE_SURVIVAL && dist <= 1.85 && Math.abs(this.py - m.y) <= 2.0 && m.attackCd <= 0) {
            m.attackCd = 24;
            if (this.invulnTicks <= 0) {
               this.health = Math.max(1, this.health - 2);
               this.hurtTime = 8;
               this.invulnTicks = 16;
               if (audio != null) audio.playSound("player_hurt", 0.8F, 1.0F);
            }
         }
      }
   }

   private void moveEntityWithCollision(boolean isPlayer, PeMob mob) {
      double x = isPlayer ? this.px : mob.x;
      double y = isPlayer ? this.py : mob.y;
      double z = isPlayer ? this.pz : mob.z;
      double vx = isPlayer ? this.vx : mob.vx;
      double vy = isPlayer ? this.vy : mob.vy;
      double vz = isPlayer ? this.vz : mob.vz;

      // Y collision
      double ny = y + vy;
      int bx = (int)Math.floor(x);
      int bz = (int)Math.floor(z);
      boolean grounded = false;
      if (vy <= 0.0 && isSolid(getBlock(bx, (int)Math.floor(ny), bz))) {
         ny = Math.floor(ny) + 1.0;
         vy = 0.0;
         grounded = true;
      } else if (vy > 0.0 && isSolid(getBlock(bx, (int)Math.floor(ny + 1.75), bz))) {
         vy = 0.0;
      }

      // X & Z collision + Optional Auto Step-Jump (Pocket Edition auto-jump)
      double nx = x + vx;
      double nz = z + vz;
      int nbx = (int)Math.floor(nx);
      int nbz = (int)Math.floor(nz);
      int footY = (int)Math.floor(ny + 0.1);
      if (isSolid(getBlock(nbx, footY, nbz)) || isSolid(getBlock(nbx, footY + 1, nbz))) {
         boolean allowAutoJump = !isPlayer || (this.autoJump && !this.sneaking && !this.flying);
         if (grounded && allowAutoJump
            && isSolid(getBlock(nbx, footY, nbz))
            && !isSolid(getBlock(nbx, footY + 1, nbz))
            && !isSolid(getBlock(nbx, footY + 2, nbz))) {
            vy = 0.42;
            grounded = false;
         } else {
            if (isSolid(getBlock(nbx, footY, bz)) || isSolid(getBlock(nbx, footY + 1, bz))) nx = x;
            if (isSolid(getBlock(bx, footY, nbz)) || isSolid(getBlock(bx, footY + 1, nbz))) nz = z;
         }
      }

      if (isPlayer) {
         if (Math.hypot(nx - this.px, nz - this.pz) > 0.001 && grounded) {
            this.walkDist += (float)Math.hypot(nx - this.px, nz - this.pz);
         }
         this.px = nx;
         this.py = ny;
         this.pz = nz;
         this.vy = vy;
         this.onGround = grounded;
      } else {
         mob.x = nx;
         mob.y = ny;
         mob.z = nz;
         mob.vy = vy;
         mob.onGround = grounded;
      }
   }
}
