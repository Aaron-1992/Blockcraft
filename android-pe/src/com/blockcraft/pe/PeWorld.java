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
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Blockcraft PE (Pocket Edition) v1.11 — World Generation, Structures & Streaming:
 * - Seed-stable Perlin terrain, coarse cached temperature/humidity biomes, per-chunk heightmaps,
 *   3D caves and water-filled river valleys
 * - Fixed-seed starter village plus bounded biome-selected structures in deterministic 6x6 chunk regions
 * - Budgeted ring-buffer streaming and section meshing keep world warm-up and frame work bounded
 * - High-refresh movement interpolation, AABB physics, touch controls, crafting, crops and block interaction
 */
public final class PeWorld {
   public static final String APP_VERSION = "1.11";

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
   public static final int WTYPE_ALL_BLOCKS = 3;
   public static final String[] WTYPE_NAMES = new String[]{
      "Large Biomes (Recommended)",
      "Default Biomes",
      "Superflat World",
      "All Blocks Gallery"
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

   // Stable legacy block IDs (0..88); World of Color variants continue at 89..177, followed by Lapis Ore at 178.
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

   // Plant, Foliage & Update 3.5+ Sub-Blocks (64..84)
   public static final byte B_LEAVES_SPRUCE = 64;
   public static final byte B_CACTUS = 65;
   public static final byte B_TALL_GRASS = 66;
   public static final byte B_FLOWER_RED = 67;
   public static final byte B_FLOWER_YELLOW = 68;
   public static final byte B_LOG_BIRCH = 69;
   public static final byte B_LEAVES_BIRCH = 70;
   public static final byte B_SLAB_OAK = 71;
   public static final byte B_SLAB_COBBLE = 72;
   public static final byte B_SLAB_STONEBRICK = 73;
   public static final byte B_FENCE_OAK = 74;
   public static final byte B_DOOR_OAK = 75;
   public static final byte B_LADDER = 76;
   public static final byte B_FARMLAND = 77;
   public static final byte B_WHEAT = 78;
   public static final byte B_SUGAR_CANE = 79;
   public static final byte B_PUMPKIN = 80;
   public static final byte B_JACK_O_LANTERN = 81;
   public static final byte B_MELON = 82;
   public static final byte B_HAY_BALE = 83;
   public static final byte B_MOSSY_STONEBRICK = 84;
   public static final byte B_STAIR_OAK = 85;
   public static final byte B_STAIR_COBBLE = 86;
   public static final byte B_STAIR_STONEBRICK = 87;
   public static final byte B_DOOR_OAK_OPEN = 88;
   // Desktop 1.12-style World of Color. The legacy block IDs 1..88 are stable;
   // these new IDs are unsigned bytes, so all block access must mask with 0xFF.
   public static final byte B_WOOL_ORANGE = 89;
   public static final byte B_WOOL_MAGENTA = 90;
   public static final byte B_WOOL_LIGHT_BLUE = 91;
   public static final byte B_WOOL_LIME = 92;
   public static final byte B_WOOL_PINK = 93;
   public static final byte B_WOOL_GRAY = 94;
   public static final byte B_WOOL_LIGHT_GRAY = 95;
   public static final byte B_WOOL_CYAN = 96;
   public static final byte B_WOOL_PURPLE = 97;
   public static final byte B_WOOL_BROWN = 98;
   public static final byte B_WOOL_BLACK = 99;

   public static final byte B_CONCRETE_WHITE = 100;
   public static final byte B_CONCRETE_ORANGE = 101;
   public static final byte B_CONCRETE_MAGENTA = 102;
   public static final byte B_CONCRETE_LIGHT_BLUE = 103;
   public static final byte B_CONCRETE_LIME = 104;
   public static final byte B_CONCRETE_PINK = 105;
   public static final byte B_CONCRETE_LIGHT_GRAY = 106;
   public static final byte B_CONCRETE_CYAN = 107;
   public static final byte B_CONCRETE_PURPLE = 108;
   public static final byte B_CONCRETE_BLUE = 109;
   public static final byte B_CONCRETE_BROWN = 110;
   public static final byte B_CONCRETE_GREEN = 111;
   public static final byte B_CONCRETE_RED = 112;
   public static final byte B_CONCRETE_BLACK = 113;

   public static final byte B_TERRACOTTA_WHITE = 114;
   public static final byte B_TERRACOTTA_ORANGE = 115;
   public static final byte B_TERRACOTTA_MAGENTA = 116;
   public static final byte B_TERRACOTTA_LIGHT_BLUE = 117;
   public static final byte B_TERRACOTTA_YELLOW = 118;
   public static final byte B_TERRACOTTA_LIME = 119;
   public static final byte B_TERRACOTTA_PINK = 120;
   public static final byte B_TERRACOTTA_GRAY = 121;
   public static final byte B_TERRACOTTA_LIGHT_GRAY = 122;
   public static final byte B_TERRACOTTA_CYAN = 123;
   public static final byte B_TERRACOTTA_PURPLE = 124;
   public static final byte B_TERRACOTTA_BLUE = 125;
   public static final byte B_TERRACOTTA_BROWN = 126;
   public static final byte B_TERRACOTTA_GREEN = 127;
   public static final byte B_TERRACOTTA_RED = (byte)128;
   public static final byte B_TERRACOTTA_BLACK = (byte)129;

   public static final byte B_STAINED_GLASS_WHITE = (byte)130;
   public static final byte B_STAINED_GLASS_ORANGE = (byte)131;
   public static final byte B_STAINED_GLASS_MAGENTA = (byte)132;
   public static final byte B_STAINED_GLASS_LIGHT_BLUE = (byte)133;
   public static final byte B_STAINED_GLASS_YELLOW = (byte)134;
   public static final byte B_STAINED_GLASS_LIME = (byte)135;
   public static final byte B_STAINED_GLASS_PINK = (byte)136;
   public static final byte B_STAINED_GLASS_GRAY = (byte)137;
   public static final byte B_STAINED_GLASS_LIGHT_GRAY = (byte)138;
   public static final byte B_STAINED_GLASS_CYAN = (byte)139;
   public static final byte B_STAINED_GLASS_PURPLE = (byte)140;
   public static final byte B_STAINED_GLASS_BLUE = (byte)141;
   public static final byte B_STAINED_GLASS_BROWN = (byte)142;
   public static final byte B_STAINED_GLASS_GREEN = (byte)143;
   public static final byte B_STAINED_GLASS_RED = (byte)144;
   public static final byte B_STAINED_GLASS_BLACK = (byte)145;

   public static final byte B_CONCRETE_POWDER_WHITE = (byte)146;
   public static final byte B_CONCRETE_POWDER_ORANGE = (byte)147;
   public static final byte B_CONCRETE_POWDER_MAGENTA = (byte)148;
   public static final byte B_CONCRETE_POWDER_LIGHT_BLUE = (byte)149;
   public static final byte B_CONCRETE_POWDER_YELLOW = (byte)150;
   public static final byte B_CONCRETE_POWDER_LIME = (byte)151;
   public static final byte B_CONCRETE_POWDER_PINK = (byte)152;
   public static final byte B_CONCRETE_POWDER_GRAY = (byte)153;
   public static final byte B_CONCRETE_POWDER_LIGHT_GRAY = (byte)154;
   public static final byte B_CONCRETE_POWDER_CYAN = (byte)155;
   public static final byte B_CONCRETE_POWDER_PURPLE = (byte)156;
   public static final byte B_CONCRETE_POWDER_BLUE = (byte)157;
   public static final byte B_CONCRETE_POWDER_BROWN = (byte)158;
   public static final byte B_CONCRETE_POWDER_GREEN = (byte)159;
   public static final byte B_CONCRETE_POWDER_RED = (byte)160;
   public static final byte B_CONCRETE_POWDER_BLACK = (byte)161;

   public static final byte B_GLAZED_TERRACOTTA_WHITE = (byte)162;
   public static final byte B_GLAZED_TERRACOTTA_ORANGE = (byte)163;
   public static final byte B_GLAZED_TERRACOTTA_MAGENTA = (byte)164;
   public static final byte B_GLAZED_TERRACOTTA_LIGHT_BLUE = (byte)165;
   public static final byte B_GLAZED_TERRACOTTA_YELLOW = (byte)166;
   public static final byte B_GLAZED_TERRACOTTA_LIME = (byte)167;
   public static final byte B_GLAZED_TERRACOTTA_PINK = (byte)168;
   public static final byte B_GLAZED_TERRACOTTA_GRAY = (byte)169;
   public static final byte B_GLAZED_TERRACOTTA_LIGHT_GRAY = (byte)170;
   public static final byte B_GLAZED_TERRACOTTA_CYAN = (byte)171;
   public static final byte B_GLAZED_TERRACOTTA_PURPLE = (byte)172;
   public static final byte B_GLAZED_TERRACOTTA_BLUE = (byte)173;
   public static final byte B_GLAZED_TERRACOTTA_BROWN = (byte)174;
   public static final byte B_GLAZED_TERRACOTTA_GREEN = (byte)175;
   public static final byte B_GLAZED_TERRACOTTA_RED = (byte)176;
   public static final byte B_GLAZED_TERRACOTTA_BLACK = (byte)177;
   public static final byte B_LAPIS_ORE = (byte)178;
   public static final int MAX_BLOCK_ID = 178;

   // Item IDs moved out of the unsigned-byte block range. Version 1.8 saves are migrated on load.
   public static final int I_SWORD_WOOD = 256;
   public static final int I_PICK_WOOD = 257;
   public static final int I_SWORD_IRON = 258;
   public static final int I_SWORD_DIAMOND = 259;
   public static final int I_PICK_IRON = 260;
   public static final int I_PICK_DIAMOND = 261;
   public static final int I_SHOVEL_DIAMOND = 262;
   public static final int I_AXE_IRON = 263;
   public static final int I_BOW = 264;
   public static final int I_ARROW = 265;
   public static final int I_GOLDEN_APPLE = 266;
   public static final int I_STEAK = 267;
   public static final int I_ENDER_PEARL = 268;
   public static final int I_IRON_INGOT = 269;
   public static final int I_GOLD_INGOT = 270;
   public static final int I_DIAMOND = 271;
   public static final int I_COAL = 272;
   public static final int I_BREAD = 273;
   public static final int I_DYE_WHITE = 274;
   public static final int I_DYE_ORANGE = 275;
   public static final int I_DYE_MAGENTA = 276;
   public static final int I_DYE_LIGHT_BLUE = 277;
   public static final int I_DYE_YELLOW = 278;
   public static final int I_DYE_LIME = 279;
   public static final int I_DYE_PINK = 280;
   public static final int I_DYE_GRAY = 281;
   public static final int I_DYE_LIGHT_GRAY = 282;
   public static final int I_DYE_CYAN = 283;
   public static final int I_DYE_PURPLE = 284;
   public static final int I_DYE_BLUE = 285;
   public static final int I_DYE_BROWN = 286;
   public static final int I_DYE_GREEN = 287;
   public static final int I_DYE_RED = 288;
   public static final int I_DYE_BLACK = 289;

   public static int dyeItem(int color) {
      return I_DYE_WHITE + Math.max(0, Math.min(15, color));
   }

   // Infinite Streaming Chunk Ring-Buffer Constants
   public static final int ACTIVE_GRID = 16;
   public static final int ACTIVE_MASK = 15;
   public static final int ACTIVE_SLOTS = ACTIVE_GRID * ACTIVE_GRID; // 256
   public static final int CHUNK_BYTES = 16 * 16 * 128;              // 32,768 bytes per chunk
   public static final int WORLD_H = 128;
   public static final int SECTIONS_Y = WORLD_H / 16;                // 8 sections of 16x16x16
   public static final int Y_SEA = 60;
   public static final int Y_DECK = 64;

   public static final byte STRUCTURE_NONE = 0;
   public static final byte STRUCTURE_STARTER_COTTAGE = 1;
   public static final byte STRUCTURE_STARTER_FARM = 2;
   public static final byte STRUCTURE_STARTER_BLACKSMITH = 3;
   public static final byte STRUCTURE_DESERT_PYRAMID = 4;
   public static final byte STRUCTURE_WATCHTOWER = 5;
   public static final byte STRUCTURE_EXPLORER_CABIN = 6;
   public static final byte STRUCTURE_SNOWY_CABIN = 7;
   public static final byte STRUCTURE_MEADOW_RUIN = 8;

   private static final int STRUCTURE_REGION_CHUNKS = 6;
   private static final int SAVE_MAGIC = 0x42435045; // "BCPE"
   private static final int SAVE_VERSION_INFINITE = 2;
   private static final int SAVE_VERSION_CURRENT = 3;

   /**
    * PC Update 3.5 Perlin Noise Generator (ported from src/bc/Noise.java).
    * Pure integer/polynomial operations — zero Math.sin/Math.cos calls!
    */
   public static final class PerlinNoise {
      private final int[] p = new int[512];

      public PerlinNoise(long seed) {
         Random r = new Random(seed);
         int[] perm = new int[256];
         for (int i = 0; i < 256; i++) perm[i] = i;
         for (int i = 255; i > 0; i--) {
            int j = r.nextInt(i + 1);
            int tmp = perm[i];
            perm[i] = perm[j];
            perm[j] = tmp;
         }
         for (int i = 0; i < 512; i++) {
            this.p[i] = perm[i & 255];
         }
      }

      private static double fade(double t) {
         return t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
      }

      private static double lerp(double t, double a, double b) {
         return a + t * (b - a);
      }

      private static double grad(int h, double x, double y, double z) {
         h &= 15;
         double u = h < 8 ? x : y;
         double v = h < 4 ? y : (h != 12 && h != 14 ? z : x);
         return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
      }

      public double noise3(double x, double y, double z) {
         int X = (int)Math.floor(x);
         int Y = (int)Math.floor(y);
         int Z = (int)Math.floor(z);
         x -= X;
         y -= Y;
         z -= Z;
         X &= 255;
         Y &= 255;
         Z &= 255;
         double u = fade(x);
         double v = fade(y);
         double w = fade(z);
         int A = this.p[X] + Y;
         int AA = this.p[A] + Z;
         int AB = this.p[A + 1] + Z;
         int B = this.p[X + 1] + Y;
         int BA = this.p[B] + Z;
         int BB = this.p[B + 1] + Z;
         return lerp(w,
            lerp(v,
               lerp(u, grad(this.p[AA], x, y, z), grad(this.p[BA], x - 1.0, y, z)),
               lerp(u, grad(this.p[AB], x, y - 1.0, z), grad(this.p[BB], x - 1.0, y - 1.0, z))
            ),
            lerp(v,
               lerp(u, grad(this.p[AA + 1], x, y, z - 1.0), grad(this.p[BA + 1], x - 1.0, y, z - 1.0)),
               lerp(u, grad(this.p[AB + 1], x, y - 1.0, z - 1.0), grad(this.p[BB + 1], x - 1.0, y - 1.0, z - 1.0))
            )
         );
      }

      public double noise2(double x, double z) {
         return noise3(x, 137.31, z);
      }

      public double fbm2(double x, double z, int octaves) {
         double sum = 0.0;
         double amp = 1.0;
         double freq = 1.0;
         double norm = 0.0;
         for (int i = 0; i < octaves; i++) {
            sum += noise3(x * freq, 137.31 + i * 17.7, z * freq) * amp;
            norm += amp;
            amp *= 0.5;
            freq *= 2.0;
         }
         return (sum / norm) * 1.45;
      }
   }

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
      /** 16x16 heightmap plus a coarse 4x4 seed-derived biome cache; neither is serialized. */
      public final short[] surfaceY = new short[16 * 16];
      public final byte[] biomeMap = new byte[4 * 4];
      public byte structureType = STRUCTURE_NONE;

      public PeChunk() {
         Arrays.fill(this.biomeMap, (byte)-1);
      }

      public void clear() {
         Arrays.fill(this.blocks, B_AIR);
         Arrays.fill(this.sectionNonAir, 0);
         Arrays.fill(this.sectionDirty, true);
         Arrays.fill(this.surfaceY, (short)0);
         Arrays.fill(this.biomeMap, (byte)-1);
         this.structureType = STRUCTURE_NONE;
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

   /**
    * PC Update 3.10 Animated 3D Falling Tree:
    * Topples smoothly from 0..90 degrees around the cut log pivot (pivotX, pivotY, pivotZ)
    * at high frame rates (via getAngleDegrees(partial)) before crashing onto the ground and placing its
    * fallen trunk logs and canopy leaves!
    */
   public static final class FallingTree {
      public final int pivotX;
      public final int pivotY;
      public final int pivotZ;
      public final int dx;
      public final int dz;
      public final int nLogs;
      public final int nTotal;
      public final int[] offX;
      public final int[] offY;
      public final int[] offZ;
      public final byte[] blockId;
      public final int rotTicks;
      public int age = 0;
      public boolean done = false;

      public FallingTree(int pivotX, int pivotY, int pivotZ, int dx, int dz,
                         int nLogs, int nTotal, int[] offX, int[] offY, int[] offZ, byte[] blockId) {
         this.pivotX = pivotX;
         this.pivotY = pivotY;
         this.pivotZ = pivotZ;
         this.dx = dx;
         this.dz = dz;
         this.nLogs = nLogs;
         this.nTotal = nTotal;
         this.offX = offX;
         this.offY = offY;
         this.offZ = offZ;
         this.blockId = blockId;
         this.rotTicks = 22;
      }

      public float getAngleDegrees(float partial) {
         float t = Math.max(0.0F, Math.min(1.0F, (this.age + partial) / (float)this.rotTicks));
         return 90.0F * t * t;
      }
   }

   public final PeChunk[] chunkGrid = new PeChunk[ACTIVE_SLOTS];
   private final HashMap<Long, byte[]> savedChunks = new HashMap<Long, byte[]>();
   public final ArrayList<FallingTree> fallingTrees = new ArrayList<FallingTree>();

   public int viewRadiusChunks = 5;
   public boolean bushyLeaves = true;

   // Current Screen & World Slot
   public int screen = SCREEN_TITLE;
   public int previousScreen = SCREEN_TITLE;
   public int currentSlot = 0; // 0, 1, 2
   public int createSlot = 0;
   public int createMode = MODE_SURVIVAL;
   public int createWorldType = WTYPE_LARGE_BIOMES;
   public int createSeedIdx = 0;
   public int invCategory = 0; // 0 = Backpack, 1 = Building, 2 = Nature, 3 = Tools, 4 = World of Color
   public int colorFamily = 0;
   public int selectedColor = 14;
   public int selectedInvSlot = -1;

   // Saved World Metadata Cache for Slots 0..2
   public final boolean[] slotExists = new boolean[3];
   public final String[] slotSummary = new String[3];

   public int mode = MODE_SURVIVAL;
   public int worldType = WTYPE_LARGE_BIOMES;
   public long seed = 884219551L;
   private long noiseSeed = 0L;
   private PerlinNoise nCont;
   private PerlinNoise nMount;
   private PerlinNoise nDetail;
   private PerlinNoise nTemp;
   private PerlinNoise nHum;
   private PerlinNoise nCave1;
   private PerlinNoise nCave2;
   private PerlinNoise nRiver;

   public long tickCount = 0L;
   public float dayTime = 0.22F;

   // Player State + High-Refresh Sub-Tick Interpolation State
   public double px = 0.5;
   public double py = 72.0;
   public double pz = 0.5;
   public double prevPx = 0.5;
   public double prevPy = 72.0;
   public double prevPz = 0.5;
   public double spawnX = 0.5;
   public double spawnY = 66.0;
   public double spawnZ = 0.5;
   public double vx = 0.0;
   public double vy = 0.0;
   public double vz = 0.0;
   public float yaw = 0.0F;
   public float pitch = 10.0F;
   public float eyeHeight = 1.62F;
   public float prevEyeHeight = 1.62F;
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
   public float prevSwingAnim = -1.0F;
   public float walkDist = 0.0F;
   public float prevWalkDist = 0.0F;
   public int selectedSlot = 0;
   public final int[] hotbarId = new int[9];
   public final int[] hotbarCount = new int[9];
   public final int[] invId = new int[27];
   public final int[] invCount = new int[27];

   // Mining state
   public int mineX = 0, mineY = -1, mineZ = 0;
   public float mineProgress = 0.0F;

   // HUD Banner State
   public String bannerMessage = "";
   public int bannerTicks = 0;

   // 5-Tab Inventory & Creative Palette (the final tab filters all six color families)
   public static final String[] INV_TAB_NAMES = new String[]{
      "BACKPACK", "BUILD", "NATURE", "TOOLS", "COLORS"
   };

   public static final int[][] CREATIVE_TABS = new int[][]{
      // Tab 0: Building Blocks, Slabs & Stairs
      {
         B_GRASS, B_DIRT, B_STONE, B_COBBLE, B_MOSSY_COBBLE, B_STONEBRICK,
         B_MOSSY_STONEBRICK, B_BRICKS, B_PLANKS_OAK, B_PLANKS_SPRUCE, B_PLANKS_BIRCH, B_LOG_OAK,
         B_LOG_SPRUCE, B_LOG_BIRCH, B_SLAB_OAK, B_SLAB_COBBLE, B_STAIR_OAK, B_STAIR_COBBLE,
         B_STAIR_STONEBRICK, B_SAND, B_SANDSTONE, B_GLASS, B_QUARTZ_BLOCK, B_OBSIDIAN
      },
      // Tab 1: Nature, Foliage, Crops & Utility
      {
         B_LEAVES_OAK, B_LEAVES_SPRUCE, B_LEAVES_BIRCH, B_FLOWER_RED, B_FLOWER_YELLOW, B_TALL_GRASS,
         B_SUGAR_CANE, B_WHEAT, B_CACTUS, B_PUMPKIN, B_JACK_O_LANTERN, B_MELON,
         B_HAY_BALE, B_TORCH, B_GLOWSTONE, B_SEA_LANTERN, B_CRAFTING_TABLE, B_FURNACE,
         B_CHEST, B_BOOKSHELF, B_BED_RED, B_FENCE_OAK, B_DOOR_OAK, B_LADDER
      },
      // Tab 2: Tools, Weapons, Food, Ores & Gems
      {
         I_PICK_DIAMOND, I_SWORD_DIAMOND, I_SHOVEL_DIAMOND, I_PICK_IRON, I_SWORD_IRON, I_AXE_IRON,
         I_PICK_WOOD, I_SWORD_WOOD, I_GOLDEN_APPLE, I_STEAK, I_BREAD, B_TNT,
         B_GOLD_BLOCK, B_IRON_BLOCK, B_DIAMOND_BLOCK, B_EMERALD_BLOCK, B_REDSTONE_BLOCK, B_COAL_BLOCK,
         B_SNOW, B_ICE, B_SLIME_BLOCK, B_OBSIDIAN, B_END_STONE, (B_LAPIS_ORE & 0xFF)
      },
      // Tab 3: Six complete 16-color material families (filtered by colorFamily)
      PeColors.ALL_BLOCKS
   };

   // Crafting & Furnace Smelting Recipes for Single-Player Survival (3 Tabs x 8 Recipes = 24 Recipes)
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
      // Category 0 (0..7): Wood, Stone, Slabs & Stairs
      new CraftRecipe("Oak Planks x4 (1 Oak Log)", B_PLANKS_OAK, 4, B_LOG_OAK, 1),
      new CraftRecipe("Spruce Planks x4 (1 Spruce)", B_PLANKS_SPRUCE, 4, B_LOG_SPRUCE, 1),
      new CraftRecipe("Birch Planks x4 (1 Birch)", B_PLANKS_BIRCH, 4, B_LOG_BIRCH, 1),
      new CraftRecipe("Oak Slabs x6 (3 Planks)", B_SLAB_OAK, 6, B_PLANKS_OAK, 3),
      new CraftRecipe("Oak Stairs x4 (4 Planks)", B_STAIR_OAK, 4, B_PLANKS_OAK, 4),
      new CraftRecipe("Cobble Stairs x4 (4 Cobble)", B_STAIR_COBBLE, 4, B_COBBLE, 4),
      new CraftRecipe("Stone Bricks x4 (4 Stone)", B_STONEBRICK, 4, B_STONE, 4),
      new CraftRecipe("Glass x4 (Smelt 4 Sand)", B_GLASS, 4, B_SAND, 4),

      // Category 1 (8..15): Utility, Decor, Lighting & Farming
      new CraftRecipe("Crafting Table (4 Planks)", B_CRAFTING_TABLE, 1, B_PLANKS_OAK, 4),
      new CraftRecipe("Furnace (8 Cobblestone)", B_FURNACE, 1, B_COBBLE, 8),
      new CraftRecipe("Storage Chest (8 Planks)", B_CHEST, 1, B_PLANKS_OAK, 8),
      new CraftRecipe("Torches x4 (1 Coal)", B_TORCH, 4, B_COAL_ORE, 1),
      new CraftRecipe("Oak Fence x4 (4 Planks)", B_FENCE_OAK, 4, B_PLANKS_OAK, 4),
      new CraftRecipe("Oak Door x2 (6 Planks)", B_DOOR_OAK, 2, B_PLANKS_OAK, 6),
      new CraftRecipe("Ladders x4 (4 Planks)", B_LADDER, 4, B_PLANKS_OAK, 4),
      new CraftRecipe("Cozy Bed (3 Planks)", B_BED_RED, 1, B_PLANKS_OAK, 3),

      // Category 2 (16..23): Tools, Weapons, Smelting & Food
      new CraftRecipe("Wood Pickaxe (3 Planks)", I_PICK_WOOD, 1, B_PLANKS_OAK, 3),
      new CraftRecipe("Wood Sword (2 Planks)", I_SWORD_WOOD, 1, B_PLANKS_OAK, 2),
      new CraftRecipe("Iron Pickaxe (3 Iron Ore)", I_PICK_IRON, 1, B_IRON_ORE, 3),
      new CraftRecipe("Iron Sword (2 Iron Ore)", I_SWORD_IRON, 1, B_IRON_ORE, 2),
      new CraftRecipe("Iron Axe (3 Iron Ore)", I_AXE_IRON, 1, B_IRON_ORE, 3),
      new CraftRecipe("Diamond Pick (3 Diamond)", I_PICK_DIAMOND, 1, B_DIAMOND_ORE, 3),
      new CraftRecipe("Diamond Sword (2 Diamond)", I_SWORD_DIAMOND, 1, B_DIAMOND_ORE, 2),
      new CraftRecipe("Golden Apple (2 Gold Ore)", I_GOLDEN_APPLE, 1, B_GOLD_ORE, 2)
   };

   public PeWorld() {
      for (int i = 0; i < ACTIVE_SLOTS; i++) {
         this.chunkGrid[i] = new PeChunk();
      }
      ensureNoiseInitialized();
   }

   private void ensureNoiseInitialized() {
      if (this.nCont != null && this.noiseSeed == this.seed) return;
      this.noiseSeed = this.seed;
      Random r = new Random(this.seed);
      this.nCont = new PerlinNoise(r.nextLong());
      this.nMount = new PerlinNoise(r.nextLong());
      this.nDetail = new PerlinNoise(r.nextLong());
      this.nTemp = new PerlinNoise(r.nextLong());
      this.nHum = new PerlinNoise(r.nextLong());
      this.nCave1 = new PerlinNoise(r.nextLong());
      this.nCave2 = new PerlinNoise(r.nextLong());
      this.nRiver = new PerlinNoise(r.nextLong());
   }

   public void syncInterpolationState() {
      this.prevPx = this.px;
      this.prevPy = this.py;
      this.prevPz = this.pz;
      this.prevWalkDist = this.walkDist;
      this.prevEyeHeight = this.eyeHeight;
      this.prevSwingAnim = this.swingAnim;
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
         } else if (s == 2) {
            this.slotSummary[s] = "All Blocks & Items Gallery  -  Tap to Open Showcase!";
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
      finishAllFallingTrees();
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
         out.writeInt(SAVE_VERSION_CURRENT);
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
         // Save 27-slot player backpack after chunks (backwards-compatible trailer)
         out.writeInt(0x42414731); // "BAG1"
         for (int i = 0; i < 27; i++) {
            out.writeInt(this.invId[i]);
            out.writeInt(this.invCount[i]);
         }
         out.flush();
         out.close();

         if (dstFile.exists()) dstFile.delete();
         tmpFile.renameTo(dstFile);

         int dayNum = (int)(this.tickCount / 24000L) + 1;
         String wTypeShort = this.worldType == WTYPE_ALL_BLOCKS ? "All Blocks Gallery"
            : (this.worldType == WTYPE_LARGE_BIOMES ? "Large Biomes" : (this.worldType == WTYPE_SUPERFLAT ? "Superflat" : "Default"));
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

   /** Version 1.8 stored non-block items in 98..115, which is now part of the expanded block catalogue. */
   public static int migrateLegacyItemId(int id) {
      return id >= 98 && id <= 115 ? I_SWORD_WOOD + (id - 98) : id;
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
         if (magic != SAVE_MAGIC || (ver < 1 || ver > SAVE_VERSION_CURRENT)) {
            in.close();
            return false;
         }
         this.currentSlot = slot;
         this.mode = in.readInt();
         this.worldType = in.readInt();
         this.seed = in.readLong();
         ensureNoiseInitialized();
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
            int savedId = in.readInt();
            this.hotbarId[i] = ver < SAVE_VERSION_CURRENT ? migrateLegacyItemId(savedId) : savedId;
            this.hotbarCount[i] = in.readInt();
         }

         Arrays.fill(this.invId, 0);
         Arrays.fill(this.invCount, 0);
         this.fallingTrees.clear();
         resetActiveChunks();
         this.savedChunks.clear();

         if (ver >= SAVE_VERSION_INFINITE) {
            int count = in.readInt();
            for (int i = 0; i < count; i++) {
               int cx = in.readInt();
               int cz = in.readInt();
               byte[] data = new byte[CHUNK_BYTES];
               in.readFully(data);
               this.savedChunks.put(chunkKey(cx, cz), data);
            }
            try {
               int bagMagic = in.readInt();
               if (bagMagic == 0x42414731) {
                  for (int i = 0; i < 27; i++) {
                     int savedId = in.readInt();
                     this.invId[i] = ver < SAVE_VERSION_CURRENT ? migrateLegacyItemId(savedId) : savedId;
                     this.invCount[i] = in.readInt();
                  }
               }
            } catch (Exception ignored) {
            }
         }
         in.close();

         this.vx = this.vy = this.vz = 0.0;
         this.mineY = -1;
         this.mineProgress = 0.0F;
         this.eyeHeight = 1.62F;
         syncInterpolationState();
         ensureChunksAroundPlayer(81); // Warm the inner 9x9; the outer view ring streams during play.
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
   }

   public void createNewSinglePlayerWorld(Context context, int slot, int newMode, int newWorldType, long customSeed) {
      this.currentSlot = Math.max(0, Math.min(2, slot));
      this.mode = newMode;
      this.worldType = newWorldType;
      this.seed = (customSeed != 0L) ? customSeed : (System.currentTimeMillis() ^ 0x5DEECE66DL);
      ensureNoiseInitialized();

      this.fallingTrees.clear();
      resetActiveChunks();
      this.savedChunks.clear();
      this.health = 20;
      this.hunger = 20;
      this.hurtTime = 0;
      this.invulnTicks = 40;
      this.flying = false;
      this.sneaking = false;
      this.tickCount = 0L;
      this.dayTime = 0.22F;

      this.px = 0.5;
      this.pz = 0.5;
      ensureChunksAroundPlayer(81); // Avoid blocking on the full 11x11 view radius.

      int sy = topSolidY(0, 0) + 1;
      this.px = 0.5;
      this.py = sy;
      this.pz = 0.5;
      this.spawnX = this.px;
      this.spawnY = this.py;
      this.spawnZ = this.pz;
      this.vx = this.vy = this.vz = 0.0;
      this.yaw = 0.0F;
      this.pitch = 6.0F;
      this.onGround = true;
      this.eyeHeight = 1.62F;
      this.walkDist = 0.0F;
      syncInterpolationState();

      equipDefaultLoadout();
      this.screen = SCREEN_PLAY;
      saveWorld(context);
      showBanner("Created Infinite World " + (this.currentSlot + 1) + " - " + WTYPE_NAMES[this.worldType]);
   }

   /** Initializes the deterministic title-screen preview without touching any save slot. */
   public void generateWorld(Context context, int newMode, int worldTypeOrMap) {
      this.currentSlot = 0;
      this.mode = newMode <= 1 ? newMode : MODE_SURVIVAL;
      this.worldType = worldTypeOrMap % 3;
      this.seed = 884219551L + worldTypeOrMap * 997L;
      ensureNoiseInitialized();
      resetActiveChunks();
      this.savedChunks.clear();
      this.fallingTrees.clear();
      this.tickCount = 0L;
      this.dayTime = 0.22F;
      this.px = 0.5;
      this.py = 72.0;
      this.pz = 0.5;
      this.vx = this.vy = this.vz = 0.0;
      this.yaw = 0.0F;
      this.pitch = 6.0F;
      this.health = 20;
      this.hunger = 20;
      this.flying = false;
      this.sneaking = false;
      this.mineY = -1;
      this.mineProgress = 0.0F;
      this.screen = SCREEN_TITLE;
      ensureChunksAroundPlayer(1);
      this.py = topSolidY(0, 0) + 1.0;
      this.spawnX = this.px;
      this.spawnY = this.py;
      this.spawnZ = this.pz;
      this.onGround = true;
      equipDefaultLoadout();
      syncInterpolationState();
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
      Arrays.fill(this.invId, 0);
      Arrays.fill(this.invCount, 0);
      this.selectedSlot = 0;
      this.selectedInvSlot = -1;
      if (this.worldType == WTYPE_ALL_BLOCKS) {
         setSlot(0, I_BOW, 1);
         setSlot(1, I_PICK_DIAMOND, 1);
         setSlot(2, I_SWORD_DIAMOND, 1);
         setSlot(3, I_AXE_IRON, 1);
         setSlot(4, I_SHOVEL_DIAMOND, 1);
         setSlot(5, I_GOLDEN_APPLE, 16);
         setSlot(6, I_STEAK, 16);
         setSlot(7, I_DIAMOND, 64);
         setSlot(8, I_ENDER_PEARL, 16);
         int[] allItemsAndBlocks = {
            I_SWORD_WOOD, I_SWORD_IRON, I_PICK_WOOD, I_PICK_IRON,
            I_ARROW, I_IRON_INGOT, I_GOLD_INGOT, I_COAL, I_BREAD,
            B_GRASS, B_STONE, B_COBBLE, B_PLANKS_OAK, B_LOG_OAK, B_LEAVES_OAK,
            B_CRAFTING_TABLE, B_FURNACE, B_CHEST, B_TORCH, B_TNT, B_BED_RED,
            B_DOOR_OAK, B_JACK_O_LANTERN, B_DIAMOND_BLOCK, B_EMERALD_BLOCK, B_GOLD_BLOCK, B_OBSIDIAN
         };
         for (int i = 0; i < 27 && i < allItemsAndBlocks.length; i++) {
            int id = allItemsAndBlocks[i];
            this.invId[i] = id;
            this.invCount[i] = (id >= I_SWORD_WOOD && id <= I_BOW) ? 1 : 64;
         }
         return;
      }
      if (this.mode == MODE_CREATIVE) {
         setSlot(0, I_PICK_DIAMOND, 1);
         setSlot(1, I_SWORD_DIAMOND, 1);
         setSlot(2, B_GRASS, 64);
         setSlot(3, B_STONEBRICK, 64);
         setSlot(4, B_PLANKS_OAK, 64);
         setSlot(5, B_LOG_OAK, 64);
         setSlot(6, B_STAIR_OAK, 64);
         setSlot(7, B_TORCH, 64);
         setSlot(8, B_TNT, 64);
         this.invId[0] = B_SLAB_OAK;        this.invCount[0] = 64;
         this.invId[1] = B_FENCE_OAK;       this.invCount[1] = 64;
         this.invId[2] = B_DOOR_OAK;        this.invCount[2] = 64;
         this.invId[3] = B_GLASS;           this.invCount[3] = 64;
         this.invId[4] = B_JACK_O_LANTERN;  this.invCount[4] = 64;
         this.invId[5] = B_BED_RED;         this.invCount[5] = 64;
         this.invId[6] = I_AXE_IRON;        this.invCount[6] = 1;
         this.invId[7] = I_SHOVEL_DIAMOND;  this.invCount[7] = 1;
         this.invId[8] = I_ENDER_PEARL;     this.invCount[8] = 16;
      } else {
         setSlot(0, I_PICK_WOOD, 1);
         setSlot(1, I_SWORD_WOOD, 1);
         setSlot(2, I_AXE_IRON, 1);
         setSlot(3, B_TORCH, 16);
         setSlot(4, B_PLANKS_OAK, 16);
         setSlot(5, B_COBBLE, 16);
         setSlot(6, B_CRAFTING_TABLE, 1);
         setSlot(7, I_STEAK, 6);
         this.invId[0] = B_LOG_OAK;         this.invCount[0] = 8;
         this.invId[1] = B_SAND;            this.invCount[1] = 12;
         this.invId[2] = B_DOOR_OAK;        this.invCount[2] = 2;
         this.invId[3] = B_BED_RED;         this.invCount[3] = 1;
         this.invId[4] = I_BREAD;           this.invCount[4] = 4;
         this.invId[5] = I_ENDER_PEARL;     this.invCount[5] = 2;
         this.invId[6] = B_WOOL_WHITE;      this.invCount[6] = 4;
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

   private static boolean matchesCraftCost(int slotItem, int costId) {
      if (slotItem == costId) return true;
      if (costId == B_COAL_ORE && slotItem == I_COAL) return true;
      if (costId == B_IRON_ORE && slotItem == I_IRON_INGOT) return true;
      if (costId == B_GOLD_ORE && slotItem == I_GOLD_INGOT) return true;
      if (costId == B_DIAMOND_ORE && slotItem == I_DIAMOND) return true;
      if (costId == B_PLANKS_OAK && (slotItem == B_PLANKS_SPRUCE || slotItem == B_PLANKS_BIRCH)) return true;
      return false;
   }

   public int countItemInHotbar(int id) {
      int total = 0;
      for (int i = 0; i < 9; i++) {
         if (matchesCraftCost(this.hotbarId[i], id) && this.hotbarCount[i] > 0) {
            total += this.hotbarCount[i];
         }
      }
      for (int i = 0; i < 27; i++) {
         if (matchesCraftCost(this.invId[i], id) && this.invCount[i] > 0) {
            total += this.invCount[i];
         }
      }
      return total;
   }

   private void consumeItemFromHotbar(int id, int count) {
      int rem = count;
      for (int i = 0; i < 9 && rem > 0; i++) {
         if (matchesCraftCost(this.hotbarId[i], id) && this.hotbarCount[i] > 0) {
            int take = Math.min(rem, this.hotbarCount[i]);
            this.hotbarCount[i] -= take;
            rem -= take;
            if (this.hotbarCount[i] <= 0) {
               this.hotbarId[i] = 0;
               this.hotbarCount[i] = 0;
            }
         }
      }
      for (int i = 0; i < 27 && rem > 0; i++) {
         if (matchesCraftCost(this.invId[i], id) && this.invCount[i] > 0) {
            int take = Math.min(rem, this.invCount[i]);
            this.invCount[i] -= take;
            rem -= take;
            if (this.invCount[i] <= 0) {
               this.invId[i] = 0;
               this.invCount[i] = 0;
            }
         }
      }
   }

   private static final int[][] DYE_MIXES = {
      {1, 14, 4}, {2, 10, 6}, {3, 11, 0}, {5, 13, 0}, {6, 14, 0},
      {7, 15, 0}, {8, 7, 0}, {9, 11, 13}, {10, 11, 14}, {12, 14, 13}
   };

   private static int directDyeSource(int color) {
      switch (color) {
         case 0: return B_SNOW;
         case 4: return B_FLOWER_YELLOW;
         case 13: return B_CACTUS;
         case 14: return B_FLOWER_RED;
         case 15: return I_COAL;
         default: return 0;
      }
   }

   /** Craft one gathered base dye or two mixed dyes using the desktop World-of-Color combinations. */
   public boolean tryCraftDye(int color, PeAudio audio) {
      if (color < 0 || color >= 16) return false;
      int source = directDyeSource(color);
      if (source > 0) {
         if (countItemInHotbar(source) < 1) {
            showBanner("Need 1x " + getItemName(source) + " to make " + PeColors.NAMES[color] + " Dye");
            if (audio != null) audio.playSound("click", 0.7F, 0.75F);
            return false;
         }
         consumeItemFromHotbar(source, 1);
         giveItem(dyeItem(color), 1);
         showBanner("Made " + PeColors.NAMES[color] + " Dye");
         if (audio != null) audio.playSound("levelup", 0.85F, 1.05F);
         return true;
      }

      if (color == 11) {
         showBanner("Mine Lapis Ore for Blue Dye");
         if (audio != null) audio.playSound("click", 0.7F, 0.75F);
         return false;
      }
      for (int[] mix : DYE_MIXES) {
         if (mix[0] != color) continue;
         int first = dyeItem(mix[1]);
         int second = dyeItem(mix[2]);
         if (countItemInHotbar(first) < 1 || countItemInHotbar(second) < 1) {
            showBanner("Mix 1x " + getItemName(first) + " + 1x " + getItemName(second));
            if (audio != null) audio.playSound("click", 0.7F, 0.75F);
            return false;
         }
         consumeItemFromHotbar(first, 1);
         consumeItemFromHotbar(second, 1);
         giveItem(dyeItem(color), 2);
         showBanner("Mixed 2x " + PeColors.NAMES[color] + " Dye");
         if (audio != null) audio.playSound("levelup", 0.85F, 1.05F);
         return true;
      }
      showBanner("Gather the matching flower or material first");
      return false;
   }

   private boolean craftColorRecipe(int outputId, int outputCount, int[] ingredientIds, int[] ingredientCounts,
                                    PeAudio audio) {
      for (int i = 0; i < ingredientIds.length; i++) {
         int have = countItemInHotbar(ingredientIds[i]);
         if (have < ingredientCounts[i]) {
            showBanner("Need " + ingredientCounts[i] + "x " + getItemName(ingredientIds[i]) + " (have " + have + ")");
            if (audio != null) audio.playSound("click", 0.7F, 0.75F);
            return false;
         }
      }
      for (int i = 0; i < ingredientIds.length; i++) {
         consumeItemFromHotbar(ingredientIds[i], ingredientCounts[i]);
      }
      giveItem(outputId, outputCount);
      showBanner("Crafted " + outputCount + "x " + getItemName(outputId));
      if (audio != null) audio.playSound("levelup", 0.85F, 1.1F);
      return true;
   }

   private int countAnyItems(int[] itemIds) {
      int total = 0;
      for (int itemId : itemIds) total += countItemInHotbar(itemId);
      return total;
   }

   private void consumeAnyItems(int[] itemIds, int count) {
      int remaining = count;
      for (int itemId : itemIds) {
         int take = Math.min(remaining, countItemInHotbar(itemId));
         if (take > 0) consumeItemFromHotbar(itemId, take);
         remaining -= take;
         if (remaining <= 0) break;
      }
   }

   private static int[] withBaseMaterial(int base, int[] colored) {
      int[] all = new int[colored.length + 1];
      all[0] = base;
      System.arraycopy(colored, 0, all, 1, colored.length);
      return all;
   }

   private boolean craftColorMaterialRecipe(int outputId, int outputCount, int[] materialIds,
                                            int materialCount, int color, PeAudio audio) {
      int dye = dyeItem(color);
      int dyeCount = countItemInHotbar(dye);
      if (dyeCount < 1) {
         showBanner("Need 1x " + getItemName(dye) + " to color these blocks");
         if (audio != null) audio.playSound("click", 0.7F, 0.75F);
         return false;
      }
      int materialHave = countAnyItems(materialIds);
      if (materialHave < materialCount) {
         showBanner("Need " + materialCount + "x material blocks (have " + materialHave + ")");
         if (audio != null) audio.playSound("click", 0.7F, 0.75F);
         return false;
      }
      consumeAnyItems(materialIds, materialCount);
      consumeItemFromHotbar(dye, 1);
      giveItem(outputId, outputCount);
      showBanner("Crafted " + outputCount + "x " + getItemName(outputId));
      if (audio != null) audio.playSound("levelup", 0.85F, 1.1F);
      return true;
   }

   private boolean craftDyedWool(int color, PeAudio audio) {
      int outputId = PeColors.WOOL_BLOCKS[color];
      for (int woolId : PeColors.WOOL_BLOCKS) {
         if (woolId == outputId || countItemInHotbar(woolId) < 1) continue;
         int dye = dyeItem(color);
         if (countItemInHotbar(dye) < 1) {
            showBanner("Need 1x " + getItemName(dye) + " to color wool");
            if (audio != null) audio.playSound("click", 0.7F, 0.75F);
            return false;
         }
         consumeItemFromHotbar(woolId, 1);
         consumeItemFromHotbar(dye, 1);
         giveItem(outputId, 1);
         showBanner("Crafted " + getItemName(outputId));
         if (audio != null) audio.playSound("levelup", 0.85F, 1.1F);
         return true;
      }
      showBanner("Need 1x wool of a different color");
      if (audio != null) audio.playSound("click", 0.7F, 0.75F);
      return false;
   }

   /** One-tap survival recipes for the selected World-of-Color material and color. */
   public boolean tryCraftColorBlock(int family, int color, PeAudio audio) {
      if (family < 0 || family >= 6 || color < 0 || color >= 16) return false;
      if (this.mode == MODE_CREATIVE) {
         giveItem(PeColors.FAMILY_BLOCKS[family][color], 64);
         showBanner("Equipped " + getItemName(PeColors.FAMILY_BLOCKS[family][color]));
         return true;
      }
      switch (family) {
         case 0:
            return craftDyedWool(color, audio);
         case 1:
            showBanner("Make Concrete Powder, then set it beside water");
            return false;
         case 2:
            return craftColorMaterialRecipe(PeColors.TERRACOTTA_BLOCKS[color], 8,
               withBaseMaterial(B_TERRACOTTA, PeColors.TERRACOTTA_BLOCKS), 8, color, audio);
         case 3:
            return craftColorRecipe(PeColors.POWDER_BLOCKS[color], 8,
               new int[]{B_SAND, B_GRAVEL, dyeItem(color)}, new int[]{4, 4, 1}, audio);
         case 4:
            return craftColorMaterialRecipe(PeColors.STAINED_GLASS_BLOCKS[color], 8,
               withBaseMaterial(B_GLASS, PeColors.STAINED_GLASS_BLOCKS), 8, color, audio);
         case 5:
            return craftColorRecipe(PeColors.GLAZED_BLOCKS[color], 1,
               new int[]{PeColors.TERRACOTTA_BLOCKS[color]}, new int[]{1}, audio);
         default:
            return false;
      }
   }

   public static boolean isToolOrWeapon(int id) {
      return id >= I_SWORD_WOOD && id <= I_BOW;
   }

   public void giveItem(int id, int count) {
      if (id <= 0 || count <= 0) return;
      int maxStack = isToolOrWeapon(id) ? 1 : 64;
      int rem = count;

      // 1. Stack into matching Hotbar slots
      if (maxStack > 1) {
         for (int i = 0; i < 9 && rem > 0; i++) {
            if (this.hotbarId[i] == id && this.hotbarCount[i] > 0 && this.hotbarCount[i] < maxStack) {
               int add = Math.min(rem, maxStack - this.hotbarCount[i]);
               this.hotbarCount[i] += add;
               rem -= add;
            }
         }
         // 2. Stack into matching Backpack slots
         for (int i = 0; i < 27 && rem > 0; i++) {
            if (this.invId[i] == id && this.invCount[i] > 0 && this.invCount[i] < maxStack) {
               int add = Math.min(rem, maxStack - this.invCount[i]);
               this.invCount[i] += add;
               rem -= add;
            }
         }
      }

      // 3. Place into empty Hotbar slots
      for (int i = 0; i < 9 && rem > 0; i++) {
         if (this.hotbarId[i] == 0 || this.hotbarCount[i] <= 0) {
            int put = Math.min(rem, maxStack);
            this.hotbarId[i] = id;
            this.hotbarCount[i] = put;
            rem -= put;
         }
      }

      // 4. Place into empty 27-slot Backpack slots (never overwriting held tools!)
      for (int i = 0; i < 27 && rem > 0; i++) {
         if (this.invId[i] == 0 || this.invCount[i] <= 0) {
            int put = Math.min(rem, maxStack);
            this.invId[i] = id;
            this.invCount[i] = put;
            rem -= put;
         }
      }
   }

   public void swapBackpackAndHotbar(int invSlot, int hotbarSlot) {
      if (invSlot < 0 || invSlot >= 27 || hotbarSlot < 0 || hotbarSlot >= 9) return;
      int tmpId = this.hotbarId[hotbarSlot];
      int tmpCnt = this.hotbarCount[hotbarSlot];
      this.hotbarId[hotbarSlot] = this.invId[invSlot];
      this.hotbarCount[hotbarSlot] = this.invCount[invSlot];
      this.invId[invSlot] = tmpId;
      this.invCount[invSlot] = tmpCnt;
   }

   // ====================================================================
   //  Infinite World Streaming & Deterministic Perlin Chunk Generation
   // ====================================================================
   public void ensureChunksAroundPlayer(int maxNewChunks) {
      if (maxNewChunks <= 0) return;
      ensureNoiseInitialized();
      int pCx = ((int)Math.floor(this.px)) >> 4;
      int pCz = ((int)Math.floor(this.pz)) >> 4;
      int rad = Math.max(3, Math.min(7, this.viewRadiusChunks));
      int generated = 0;

      for (int ring = 0; ring <= rad && generated < maxNewChunks; ring++) {
         if (ring == 0) {
            generated += ensureChunkLoaded(pCx, pCz);
            continue;
         }
         // Visit only the perimeter. The previous square scan discarded most of its
         // candidates with a distance test, which was needlessly expensive each tick.
         for (int dx = -ring; dx <= ring && generated < maxNewChunks; dx++) {
            generated += ensureChunkLoaded(pCx + dx, pCz - ring);
            if (generated < maxNewChunks) {
               generated += ensureChunkLoaded(pCx + dx, pCz + ring);
            }
         }
         for (int dz = -ring + 1; dz < ring && generated < maxNewChunks; dz++) {
            generated += ensureChunkLoaded(pCx - ring, pCz + dz);
            if (generated < maxNewChunks) {
               generated += ensureChunkLoaded(pCx + ring, pCz + dz);
            }
         }
      }
   }

   private int ensureChunkLoaded(int cx, int cz) {
      PeChunk slot = this.chunkGrid[slotIndex(cx, cz)];
      if (slot.loaded && slot.cx == cx && slot.cz == cz) return 0;
      loadOrGenerateChunkIntoSlot(slot, cx, cz);
      return 1;
   }

   private PeChunk getOrLoadChunk(int cx, int cz) {
      PeChunk c = this.chunkGrid[slotIndex(cx, cz)];
      if (!c.loaded || c.cx != cx || c.cz != cz) {
         loadOrGenerateChunkIntoSlot(c, cx, cz);
      }
      return c;
   }

   private void loadOrGenerateChunkIntoSlot(PeChunk slot, int cx, int cz) {
      ensureNoiseInitialized();
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
         Arrays.fill(slot.surfaceY, (short)0);
         Arrays.fill(slot.biomeMap, (byte)-1);
         slot.structureType = STRUCTURE_NONE;
         System.arraycopy(saved, 0, slot.blocks, 0, CHUNK_BYTES);
         slot.modified = true;
         slot.recomputeSectionCounts();
         Arrays.fill(slot.sectionDirty, true);
      } else {
         slot.clear();
         if (this.worldType == WTYPE_ALL_BLOCKS) {
            generateAllBlocksGalleryChunk(slot, cx, cz);
         } else if (this.worldType == WTYPE_SUPERFLAT) {
            generateSuperflatChunk(slot, cx, cz);
         } else {
            generateOverworldChunk(slot, cx, cz);
         }
         Arrays.fill(slot.sectionDirty, true);
      }

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
         if (isSolid(b) && !isLeaves(b)) {
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

   static int structureSiteChunkX(int regionX, int regionZ, long seed) {
      int hash = chunkHash(regionX, regionZ, seed + 77L);
      return regionX * STRUCTURE_REGION_CHUNKS + 1 + (hash & 3);
   }

   static int structureSiteChunkZ(int regionX, int regionZ, long seed) {
      int hash = chunkHash(regionX, regionZ, seed + 77L);
      return regionZ * STRUCTURE_REGION_CHUNKS + 1 + ((hash >>> 4) & 3);
   }

   private static int floorDivideByStructureRegion(int chunk) {
      int region = chunk / STRUCTURE_REGION_CHUNKS;
      return chunk < 0 && chunk % STRUCTURE_REGION_CHUNKS != 0 ? region - 1 : region;
   }

   private static boolean isStructureSiteChunk(int cx, int cz, long seed) {
      int regionX = floorDivideByStructureRegion(cx);
      int regionZ = floorDivideByStructureRegion(cz);
      return cx == structureSiteChunkX(regionX, regionZ, seed)
         && cz == structureSiteChunkZ(regionX, regionZ, seed);
   }

   private static int biomeCellIndex(int localX, int localZ) {
      return (((localZ & 15) >> 2) << 2) | ((localX & 15) >> 2);
   }

   public int getBiomeAt(int wx, int wz) {
      if (this.worldType == WTYPE_SUPERFLAT) return 0;
      int cx = wx >> 4;
      int cz = wz >> 4;
      int localX = wx & 15;
      int localZ = wz & 15;
      int biomeCell = biomeCellIndex(localX, localZ);
      PeChunk chunk = this.chunkGrid[slotIndex(cx, cz)];
      boolean cacheOwned = chunk.loaded && chunk.cx == cx && chunk.cz == cz;
      if (cacheOwned) {
         int cached = chunk.biomeMap[biomeCell];
         if (cached >= 0 && cached <= 4) return cached;
      }

      // Biome boundaries are broad; use globally aligned 4x4-block samples to avoid
      // evaluating three multi-octave climate fields for every single terrain column.
      int sampleX = (wx & ~3) + 2;
      int sampleZ = (wz & ~3) + 2;
      ensureNoiseInitialized();
      double scale = (this.worldType == WTYPE_LARGE_BIOMES) ? 640.0 : 320.0;
      double temp = this.nTemp.fbm2(sampleX / scale + 500.0, sampleZ / scale, 2) * 0.5 + 0.5;
      double hum = this.nHum.fbm2(sampleX / scale - 300.0, sampleZ / scale + 120.0, 2) * 0.5 + 0.5;
      double mtn = this.nMount.fbm2(sampleX / 320.0 + 77.0, sampleZ / 320.0 - 33.0, 2);
      int biome = mtn > 0.34 ? 3 : (temp > 0.62 && hum < 0.45 ? 1
         : (temp < 0.30 ? 2 : (temp > 0.48 && hum > 0.54 ? 4 : 0)));
      if (cacheOwned) chunk.biomeMap[biomeCell] = (byte)biome;
      return biome;
   }

   public String getBiomeName(int wx, int wz) {
      if (this.worldType == WTYPE_ALL_BLOCKS) return "All Blocks Gallery";
      switch (getBiomeAt(wx, wz)) {
         case 1: return "Golden Desert";
         case 2: return "Snowy Taiga";
         case 3: return "Mountain Peaks";
         case 4: return "Birch Flower Meadow";
         default: return this.worldType == WTYPE_SUPERFLAT ? "Superflat Plains" : "Emerald Forest";
      }
   }

   private void generateAllBlocksGalleryChunk(PeChunk c, int cx, int cz) {
      int wx0 = cx << 4;
      int wz0 = cz << 4;
      for (int lz = 0; lz < 16; lz++) {
         int wz = wz0 + lz;
         for (int lx = 0; lx < 16; lx++) {
            int wx = wx0 + lx;
            setChunkLocal(c, lx, 0, lz, B_BEDROCK);
            for (int y = 1; y <= 61; y++) {
               setChunkLocal(c, lx, y, lz, B_STONE);
            }
            setChunkLocal(c, lx, 62, lz, B_STONEBRICK);
            if (wx >= -18 && wx <= 18 && wz >= -30 && wz <= 10) {
               boolean light = Math.floorMod(wx, 4) == 0 && Math.floorMod(wz, 3) == 0;
               boolean aisle = ((wx + wz) & 1) == 0;
               setChunkLocal(c, lx, 63, lz, light ? B_GLOWSTONE : (aisle ? B_QUARTZ_BLOCK : B_STONEBRICK));
            } else {
               setChunkLocal(c, lx, 63, lz, B_GRASS);
            }
         }
      }

      // Lay out every block ID in 15 columns, including the complete World of Color set.
      for (int id = 1; id <= MAX_BLOCK_ID; id++) {
         int idx = id - 1;
         int col = idx % 15;
         int row = idx / 15;
         int bx = -14 + col * 2;
         int bz = -4 - row * 3;
         if ((bx >> 4) == cx && (bz >> 4) == cz) {
            int lx = bx & 15;
            int lz = bz & 15;
            byte b = (byte)id;
            if (b == B_TALL_GRASS || b == B_FLOWER_RED || b == B_FLOWER_YELLOW || b == B_SUGAR_CANE) {
               setChunkLocal(c, lx, 63, lz, B_GRASS);
            } else if (b == B_WHEAT) {
               setChunkLocal(c, lx, 63, lz, B_FARMLAND);
            } else if (b == B_CACTUS) {
               setChunkLocal(c, lx, 63, lz, B_SAND);
            }
            setChunkLocal(c, lx, 64, lz, b);
            if (isOpaque(b)) {
               setChunkLocal(c, lx, 67, lz, b);
            }
         }
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
         buildVillageCottageInChunk(c, 8, 63, 8);
      } else if (cx == 1 && cz == -1) {
         buildVillageFarmInChunk(c, 8, 63, 8);
      }
   }

   private void generateOverworldChunk(PeChunk c, int cx, int cz) {
      int wx0 = cx << 4;
      int wz0 = cz << 4;
      c.structureType = STRUCTURE_NONE;

      for (int lz = 0; lz < 16; lz++) {
         int wz = wz0 + lz;
         for (int lx = 0; lx < 16; lx++) {
            int wx = wx0 + lx;
            int column = (lz << 4) | lx;
            int biomeCell = biomeCellIndex(lx, lz);
            int biome = getBiomeAt(wx, wz);
            c.biomeMap[biomeCell] = (byte)biome;

            double cont = this.nCont.fbm2(wx / 260.0, wz / 260.0, 3);
            double detail = this.nDetail.fbm2(wx / 56.0, wz / 56.0, 3);
            double ridge = Math.max(0.0, this.nMount.fbm2(wx / 140.0 + 31.0, wz / 140.0 - 19.0, 2));
            double hillAmp = (biome == 3) ? 28.0 : (biome == 1 ? 7.0 : 12.0);
            int h = (int)Math.round(62.5 + cont * 7.5 + detail * 4.5 + ridge * hillAmp);

            // Carve broad, shallow river valleys before flattening the protected spawn area.
            double river = Math.abs(this.nRiver.noise2(wx / 156.0 + 402.0, wz / 156.0 - 173.0));
            if (river < 0.027 && h > Y_SEA + 2 && h < 78) {
               double blend = (0.027 - river) / 0.027;
               blend = blend * blend * (3.0 - 2.0 * blend);
               h = (int)Math.round(h + (Y_SEA - 1 - h) * blend);
            }

            // Avoid a square-root per column except in the small spawn falloff.
            int spawnDx = wx - 4;
            int spawnDz = wz + 6;
            int spawnDistSq = spawnDx * spawnDx + spawnDz * spawnDz;
            if (spawnDistSq < 22 * 22) {
               double spawnDist = Math.sqrt(spawnDistSq);
               double blend = Math.max(0.0, (spawnDist - 10.0) / 12.0);
               h = (int)Math.round(64.0 * (1.0 - blend) + h * blend);
            }
            h = Math.max(8, Math.min(112, h));
            c.surfaceY[column] = (short)h;

            setChunkLocal(c, lx, 0, lz, B_BEDROCK);
            for (int y = 1; y <= h; y++) {
               // 3D cave noise is costly; keep caves out of the protected spawn area.
               if (y > 5 && y < h - 4 && spawnDistSq > 18 * 18 && (y & 1) == 0) {
                  double c1 = this.nCave1.noise3(wx * 0.045, y * 0.06, wz * 0.045);
                  if (c1 * c1 < 0.0055) {
                     double c2 = this.nCave2.noise3(wx * 0.045 + 19.3, y * 0.06 + 7.1, wz * 0.045 - 23.7);
                     if (c1 * c1 + c2 * c2 < 0.0052) continue;
                  }
               }

               byte b;
               if (y == h) {
                  if (h <= Y_SEA + 1) b = B_SAND;
                  else if (biome == 1) b = B_SAND;
                  else if (biome == 2 || h >= 80) b = B_SNOW;
                  else b = B_GRASS;
               } else if (y >= h - 3) {
                  b = (biome == 1 || h <= Y_SEA + 1) ? B_SANDSTONE : B_DIRT;
               } else {
                  int hash = ((wx * 734287) ^ (y * 912931) ^ (wz * 439829) ^ (int)this.seed) & 255;
                  if (y <= 16 && hash <= 1) b = B_DIAMOND_ORE;
                  else if (y <= 28 && hash <= 4) b = B_GOLD_ORE;
                  else if (y <= 24 && hash == 5) b = B_REDSTONE_ORE;
                  else if (y <= 32 && hash <= 7) b = B_LAPIS_ORE;
                  else if (y <= 54 && hash <= 11) b = B_IRON_ORE;
                  else if (hash <= 19) b = B_COAL_ORE;
                  else if (hash == 20) b = B_GRAVEL;
                  else b = B_STONE;
               }
               setChunkLocal(c, lx, y, lz, b);
            }
            for (int y = h + 1; y <= Y_SEA; y++) {
               setChunkLocal(c, lx, y, lz, (biome == 2 && y == Y_SEA) ? B_ICE : B_WATER);
            }
            int clayHash = (wx * 734287) ^ (wz * 439829) ^ (int)this.seed;
            if (h < Y_SEA && h >= Y_SEA - 5 && spawnDistSq > 28 * 28 && (clayHash & 31) == 0) {
               setChunkLocal(c, lx, h, lz, B_TERRACOTTA);
            }
         }
      }

      // The fixed spawn village is deliberately stable across seeds and releases.
      if (cx == 0 && cz == -1) {
         c.structureType = STRUCTURE_STARTER_COTTAGE;
         buildVillageCottageInChunk(c, 8, c.surfaceY[(8 << 4) | 8], 8);
         return;
      }
      if (cx == 1 && cz == -1) {
         c.structureType = STRUCTURE_STARTER_FARM;
         buildVillageFarmInChunk(c, 8, c.surfaceY[(8 << 4) | 8], 8);
         return;
      }
      if (cx == -1 && cz == -1) {
         c.structureType = STRUCTURE_STARTER_BLACKSMITH;
         buildBlacksmithInChunk(c, 8, c.surfaceY[(8 << 4) | 8], 8);
         return;
      }

      // One deterministic, biome-selected landmark per 6x6 chunk region. Sites
      // have bounded flat pads so buildings never spill into a neighbour chunk.
      if ((Math.abs((long)cx) > 1 || Math.abs((long)cz) > 1)
            && isStructureSiteChunk(cx, cz, this.seed)) {
         int oy = c.surfaceY[(8 << 4) | 8];
         int biome = c.biomeMap[biomeCellIndex(8, 8)] & 0xFF;
         if (biome == 1 && hasStructurePad(c, 8, 8, 5, 2, 5)) {
            c.structureType = STRUCTURE_DESERT_PYRAMID;
            buildDesertPyramidInChunk(c, 8, oy, 8);
            return;
         }
         if (biome == 3 && hasStructurePad(c, 8, 8, 2, 7, 7)) {
            c.structureType = STRUCTURE_WATCHTOWER;
            buildWatchtowerInChunk(c, 8, oy, 8);
            return;
         }
         if (biome == 2 && hasStructurePad(c, 8, 8, 4, 3, 6)) {
            c.structureType = STRUCTURE_SNOWY_CABIN;
            buildSnowyCabinInChunk(c, 8, oy, 8);
            return;
         }
         if (biome == 4 && hasStructurePad(c, 8, 8, 3, 4, 5)) {
            c.structureType = STRUCTURE_MEADOW_RUIN;
            buildMeadowRuinInChunk(c, 8, oy, 8);
            return;
         }
         if (hasStructurePad(c, 8, 8, 4, 3, 6)) {
            c.structureType = STRUCTURE_EXPLORER_CABIN;
            buildVillageCottageInChunk(c, 8, oy, 8);
            return;
         }
      }

      // Deterministic Oak, Birch & Spruce Trees + Desert Cacti.
      for (int gz = 0; gz < 2; gz++) {
         for (int gx = 0; gx < 2; gx++) {
            int cellH = chunkHash(cx * 2 + gx, cz * 2 + gz, this.seed);
            int lx = 3 + gx * 6 + (cellH & 3);
            int lz = 3 + gz * 6 + ((cellH >>> 2) & 3);
            int wx = wx0 + lx;
            int wz = wz0 + lz;
            if (Math.hypot(wx, wz) < 9.0) continue;

            int column = (lz << 4) | lx;
            int ty = c.surfaceY[column];
            int biome = c.biomeMap[biomeCellIndex(lx, lz)] & 0xFF;
            byte top = c.blocks[(lz << 11) | (lx << 7) | ty];
            if (ty <= Y_SEA + 1 || ty >= 79) continue;

            if ((top == B_GRASS || top == B_SNOW) && (cellH % 3 != 0)) {
               boolean spruce = (top == B_SNOW) || (biome == 2) || ((cellH & 7) == 0);
               boolean birch = !spruce && ((biome == 4 && (cellH & 1) == 0) || ((cellH & 7) == 3));
               byte logId = spruce ? B_LOG_SPRUCE : (birch ? B_LOG_BIRCH : B_LOG_OAK);
               byte leafId = spruce ? B_LEAVES_SPRUCE : (birch ? B_LEAVES_BIRCH : B_LEAVES_OAK);
               placeTreeInChunk(c, lx, ty + 1, lz, logId, leafId, spruce);
            } else if (top == B_SAND && biome == 1 && (cellH & 3) == 0) {
               for (int ch = 1; ch <= 3; ch++) {
                  setChunkLocal(c, lx, ty + ch, lz, B_CACTUS);
               }
            }
         }
      }

      // Shoreline reeds and deterministic biome-aware ground cover.
      for (int lz = 1; lz < 15; lz++) {
         for (int lx = 1; lx < 15; lx++) {
            int column = (lz << 4) | lx;
            int ty = c.surfaceY[column];
            int colBase = (lz << 11) | (lx << 7);
            byte ground = c.blocks[colBase | ty];
            if (c.blocks[colBase | (ty + 1)] != B_AIR) continue;

            int fh = chunkHash(wx0 + lx, wz0 + lz, this.seed + 313L) & 127;
            if ((ty == Y_SEA || ty == Y_SEA + 1) && (ground == B_SAND || ground == B_GRASS) && fh <= 10) {
               boolean nearWater = c.blocks[((lz - 1) << 11) | (lx << 7) | Y_SEA] == B_WATER
                  || c.blocks[((lz + 1) << 11) | (lx << 7) | Y_SEA] == B_WATER
                  || c.blocks[(lz << 11) | ((lx - 1) << 7) | Y_SEA] == B_WATER
                  || c.blocks[(lz << 11) | ((lx + 1) << 7) | Y_SEA] == B_WATER;
               if (nearWater) {
                  setChunkLocal(c, lx, ty + 1, lz, B_SUGAR_CANE);
                  setChunkLocal(c, lx, ty + 2, lz, B_SUGAR_CANE);
                  if ((fh & 1) == 0) setChunkLocal(c, lx, ty + 3, lz, B_SUGAR_CANE);
                  continue;
               }
            }

            if (ty <= Y_SEA + 1 || ty >= 77 || ground != B_GRASS) continue;
            int biome = c.biomeMap[biomeCellIndex(lx, lz)] & 0xFF;
            if (fh == 0) {
               setChunkLocal(c, lx, ty + 1, lz, (biome == 4) ? B_MELON : B_PUMPKIN);
            } else if (fh <= 2 || (biome == 4 && fh <= 7)) {
               setChunkLocal(c, lx, ty + 1, lz, B_FLOWER_RED);
            } else if (fh <= 4 || (biome == 4 && fh <= 13)) {
               setChunkLocal(c, lx, ty + 1, lz, B_FLOWER_YELLOW);
            } else if (fh <= 16) {
               setChunkLocal(c, lx, ty + 1, lz, B_TALL_GRASS);
            }
         }
      }
   }

   private static boolean hasStructurePad(PeChunk c, int ox, int oz, int radius, int maxVariation, int heightClearance) {
      int minY = Integer.MAX_VALUE;
      int maxY = Integer.MIN_VALUE;
      for (int dz = -radius; dz <= radius; dz++) {
         for (int dx = -radius; dx <= radius; dx++) {
            int lx = ox + dx;
            int lz = oz + dz;
            if ((lx & ~15) != 0 || (lz & ~15) != 0) return false;
            int y = c.surfaceY[(lz << 4) | lx];
            if (y <= Y_SEA + 1 || y + heightClearance >= WORLD_H - 1) return false;
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
         }
      }
      return maxY - minY <= maxVariation;
   }

   private static void placeTreeInChunk(PeChunk c, int lx, int y, int lz, byte logId, byte leafId, boolean spruce) {
      int trunkH = spruce ? 6 : 5;
      for (int dy = 2; dy <= trunkH + 1; dy++) {
         int r = spruce ? ((dy == trunkH + 1) ? 0 : ((dy & 1) == 0 ? 2 : 1)) : ((dy >= trunkH) ? 1 : 2);
         if (!spruce && dy == 2) continue;
         for (int dz = -r; dz <= r; dz++) {
            for (int dx = -r; dx <= r; dx++) {
               if (r == 2 && Math.abs(dx) == 2 && Math.abs(dz) == 2 && (!spruce || dy > 2)) continue;
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
      for (int dy = 0; dy < trunkH; dy++) {
         setChunkLocal(c, lx, y + dy, lz, logId);
      }
   }

   private static void buildVillageCottageInChunk(PeChunk c, int ox, int oy, int oz) {
      for (int dz = -3; dz <= 4; dz++) {
         for (int dx = -3; dx <= 3; dx++) {
            int lx = ox + dx;
            int lz = oz + dz;
            if ((lx & ~15) != 0 || (lz & ~15) != 0) continue;
            if (dz == 4) {
               // Front cobblestone path & porch step
               for (int y = oy - 3; y <= oy; y++) {
                  setChunkLocal(c, lx, y, lz, Math.abs(dx) <= 1 ? B_COBBLE : B_GRASS);
               }
               if (dx == -2) {
                  setChunkLocal(c, lx, oy + 1, lz, B_FENCE_OAK);
                  setChunkLocal(c, lx, oy + 2, lz, B_FENCE_OAK);
                  setChunkLocal(c, lx, oy + 3, lz, B_GLOWSTONE);
               }
               continue;
            }
            for (int y = oy - 3; y <= oy; y++) {
               setChunkLocal(c, lx, y, lz, B_COBBLE);
            }
            boolean wall = Math.abs(dx) == 3 || Math.abs(dz) == 3;
            boolean door = (dz == 3 && dx == 0);
            for (int dy = 1; dy <= 4; dy++) {
               if (door && dy <= 2) {
                  setChunkLocal(c, lx, oy + dy, lz, dy == 1 ? B_DOOR_OAK : B_AIR);
               } else if (wall) {
                  boolean corner = Math.abs(dx) == 3 && Math.abs(dz) == 3;
                  boolean win = dy == 2 && (dx == 0 || dz == 0) && !door;
                  setChunkLocal(c, lx, oy + dy, lz, corner ? B_LOG_OAK : (win ? B_GLASS : B_PLANKS_OAK));
               } else {
                  setChunkLocal(c, lx, oy + dy, lz, B_AIR);
               }
            }
            // Pitched Oak Stair & Spruce Plank roof
            setChunkLocal(c, lx, oy + 5, lz, (Math.abs(dx) == 3 || Math.abs(dz) == 3) ? B_STAIR_OAK : B_PLANKS_SPRUCE);
            if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) {
               setChunkLocal(c, lx, oy + 6, lz, B_SLAB_OAK);
            }
         }
      }
      setChunkLocal(c, ox - 2, oy + 1, oz - 2, B_CRAFTING_TABLE);
      setChunkLocal(c, ox, oy + 1, oz - 2, B_FURNACE);
      setChunkLocal(c, ox + 2, oy + 1, oz - 2, B_CHEST);
      setChunkLocal(c, ox + 2, oy + 1, oz, B_BOOKSHELF);
      setChunkLocal(c, ox + 2, oy + 2, oz, B_BOOKSHELF);
      setChunkLocal(c, ox - 2, oy + 1, oz + 1, B_BED_RED);
      setChunkLocal(c, ox, oy + 3, oz, B_TORCH);
   }

   private static void buildVillageFarmInChunk(PeChunk c, int ox, int oy, int oz) {
      for (int dz = -4; dz <= 4; dz++) {
         for (int dx = -4; dx <= 4; dx++) {
            int lx = ox + dx;
            int lz = oz + dz;
            if ((lx & ~15) != 0 || (lz & ~15) != 0) continue;
            for (int y = oy - 3; y < oy; y++) {
               setChunkLocal(c, lx, y, lz, B_DIRT);
            }
            for (int dy = 1; dy <= 5; dy++) {
               setChunkLocal(c, lx, oy + dy, lz, B_AIR);
            }
            boolean border = Math.abs(dx) == 4 || Math.abs(dz) == 4;
            if (border) {
               setChunkLocal(c, lx, oy, lz, B_LOG_OAK);
               if (!(dz == 4 && dx == 0)) {
                  setChunkLocal(c, lx, oy + 1, lz, B_FENCE_OAK);
               }
            } else if (dx == 0) {
               setChunkLocal(c, lx, oy, lz, B_WATER);
            } else {
               setChunkLocal(c, lx, oy, lz, B_FARMLAND);
               setChunkLocal(c, lx, oy + 1, lz, (dx > 0 && dz >= 2) ? B_PUMPKIN : B_WHEAT);
            }
         }
      }
      setChunkLocal(c, ox - 3, oy + 1, oz - 3, B_HAY_BALE);
      setChunkLocal(c, ox - 3, oy + 2, oz - 3, B_JACK_O_LANTERN);
   }

   private static void buildBlacksmithInChunk(PeChunk c, int ox, int oy, int oz) {
      for (int dz = -3; dz <= 3; dz++) {
         for (int dx = -3; dx <= 3; dx++) {
            int lx = ox + dx;
            int lz = oz + dz;
            if ((lx & ~15) != 0 || (lz & ~15) != 0) continue;
            for (int y = oy - 3; y <= oy; y++) {
               setChunkLocal(c, lx, y, lz, B_STONEBRICK);
            }
            boolean wall = Math.abs(dx) == 3 || Math.abs(dz) == 3;
            boolean openFront = (dz == 3 && dx >= -1 && dx <= 1);
            for (int dy = 1; dy <= 4; dy++) {
               if (wall && !(openFront && dy <= 2)) {
                  setChunkLocal(c, lx, oy + dy, lz, (dy == 2 && !openFront) ? B_BARS : B_COBBLE);
               } else {
                  setChunkLocal(c, lx, oy + dy, lz, B_AIR);
               }
            }
            setChunkLocal(c, lx, oy + 5, lz, B_SLAB_STONEBRICK);
         }
      }
      setChunkLocal(c, ox - 2, oy + 1, oz - 2, B_FURNACE);
      setChunkLocal(c, ox - 1, oy + 1, oz - 2, B_LAVA);
      setChunkLocal(c, ox, oy + 1, oz - 2, B_STONEBRICK);
      setChunkLocal(c, ox + 2, oy + 1, oz - 2, B_CHEST);
      setChunkLocal(c, ox + 2, oy + 1, oz + 1, B_IRON_BLOCK);
      setChunkLocal(c, ox, oy + 3, oz, B_TORCH);
   }

   private static void buildDesertPyramidInChunk(PeChunk c, int ox, int oy, int oz) {
      for (int layer = 0; layer <= 5; layer++) {
         int r = 5 - layer;
         for (int dz = -r; dz <= r; dz++) {
            for (int dx = -r; dx <= r; dx++) {
               int lx = ox + dx;
               int lz = oz + dz;
               if ((lx & ~15) != 0 || (lz & ~15) != 0) continue;
               if (layer == 0) {
                  for (int y = oy - 3; y <= oy; y++) {
                     setChunkLocal(c, lx, y, lz, B_SANDSTONE);
                  }
               }
               boolean shell = Math.abs(dx) == r || Math.abs(dz) == r || layer == 5;
               boolean doorway = layer <= 2 && dz == r && dx == 0;
               if (doorway) {
                  setChunkLocal(c, lx, oy + layer, lz, B_AIR);
               } else if (shell) {
                  byte b = (layer == 2 && (Math.abs(dx) == 2 || Math.abs(dz) == 2)) ? B_RED_SANDSTONE : B_SANDSTONE;
                  setChunkLocal(c, lx, oy + layer, lz, b);
               } else if (layer >= 1) {
                  setChunkLocal(c, lx, oy + layer, lz, B_AIR);
               }
            }
         }
      }
      setChunkLocal(c, ox, oy + 1, oz, B_GOLD_BLOCK);
      setChunkLocal(c, ox - 1, oy + 1, oz - 1, B_CHEST);
      setChunkLocal(c, ox + 1, oy + 1, oz - 1, B_TORCH);
   }

   private static void buildWatchtowerInChunk(PeChunk c, int ox, int oy, int oz) {
      for (int dz = -2; dz <= 2; dz++) {
         for (int dx = -2; dx <= 2; dx++) {
            int lx = ox + dx;
            int lz = oz + dz;
            if ((lx & ~15) != 0 || (lz & ~15) != 0) continue;
            for (int y = oy - 3; y <= oy; y++) {
               setChunkLocal(c, lx, y, lz, B_STONEBRICK);
            }
            boolean wall = Math.abs(dx) == 2 || Math.abs(dz) == 2;
            for (int dy = 1; dy <= 7; dy++) {
               if (dz == 2 && dx == 0 && dy <= 2) {
                  setChunkLocal(c, lx, oy + dy, lz, B_AIR);
               } else if (dy == 7) {
                  if (wall && ((dx + dz) & 1) == 0) {
                     setChunkLocal(c, lx, oy + dy, lz, B_STONEBRICK);
                  } else {
                     setChunkLocal(c, lx, oy + dy, lz, B_AIR);
                  }
               } else if (dy == 6) {
                  setChunkLocal(c, lx, oy + dy, lz, B_PLANKS_OAK);
               } else if (wall) {
                  byte brick = ((dy + dx + dz) % 3 == 0) ? B_MOSSY_STONEBRICK : B_STONEBRICK;
                  setChunkLocal(c, lx, oy + dy, lz, brick);
               } else {
                  setChunkLocal(c, lx, oy + dy, lz, B_AIR);
               }
            }
         }
      }
      for (int dy = 1; dy <= 6; dy++) {
         setChunkLocal(c, ox, oy + dy, oz - 1, B_LADDER);
      }
      setChunkLocal(c, ox + 1, oy + 1, oz - 1, B_CHEST);
      setChunkLocal(c, ox, oy + 7, oz, B_TORCH);
   }

   private static void buildSnowyCabinInChunk(PeChunk c, int ox, int oy, int oz) {
      for (int dz = -4; dz <= 4; dz++) {
         for (int dx = -4; dx <= 4; dx++) {
            int lx = ox + dx;
            int lz = oz + dz;
            if ((lx & ~15) != 0 || (lz & ~15) != 0) continue;
            for (int y = oy - 3; y <= oy; y++) {
               setChunkLocal(c, lx, y, lz, (Math.abs(dx) == 4 || Math.abs(dz) == 4) ? B_COBBLE : B_PLANKS_SPRUCE);
            }
            boolean wall = Math.abs(dx) == 3 || Math.abs(dz) == 3;
            boolean corner = Math.abs(dx) == 3 && Math.abs(dz) == 3;
            boolean doorway = dz == 3 && dx == 0;
            for (int dy = 1; dy <= 4; dy++) {
               if (doorway && dy <= 2) {
                  setChunkLocal(c, lx, oy + dy, lz, dy == 1 ? B_DOOR_OAK : B_AIR);
               } else if (wall) {
                  boolean window = dy == 2 && ((Math.abs(dx) == 3 && dz == 0) || (Math.abs(dz) == 3 && dx != 0));
                  setChunkLocal(c, lx, oy + dy, lz,
                     corner ? B_LOG_SPRUCE : (window ? B_GLASS : B_PLANKS_SPRUCE));
               } else {
                  setChunkLocal(c, lx, oy + dy, lz, B_AIR);
               }
            }
         }
      }
      // Compact pitched roof, with a snow edge and an exposed ridge.
      for (int layer = 0; layer <= 3; layer++) {
         int xRadius = 3 - layer;
         int y = oy + 5 + layer;
         for (int dz = -4; dz <= 4; dz++) {
            for (int dx = -xRadius; dx <= xRadius; dx++) {
               byte roof = (layer == 0 && (Math.abs(dx) == 3 || Math.abs(dz) == 4)) ? B_SNOW : B_PLANKS_SPRUCE;
               setChunkLocal(c, ox + dx, y, oz + dz, roof);
            }
         }
      }
      setChunkLocal(c, ox - 2, oy + 1, oz - 2, B_BED_RED);
      setChunkLocal(c, ox + 2, oy + 1, oz - 2, B_CHEST);
      setChunkLocal(c, ox - 2, oy + 1, oz + 1, B_CRAFTING_TABLE);
      setChunkLocal(c, ox + 2, oy + 1, oz + 1, B_FURNACE);
      setChunkLocal(c, ox, oy + 3, oz, B_TORCH);
   }

   private static void buildMeadowRuinInChunk(PeChunk c, int ox, int oy, int oz) {
      for (int dz = -3; dz <= 3; dz++) {
         for (int dx = -3; dx <= 3; dx++) {
            int lx = ox + dx;
            int lz = oz + dz;
            if ((lx & ~15) != 0 || (lz & ~15) != 0) continue;
            for (int y = oy - 3; y <= oy; y++) {
               setChunkLocal(c, lx, y, lz, B_STONEBRICK);
            }
            boolean perimeter = Math.abs(dx) == 3 || Math.abs(dz) == 3;
            if (!perimeter) continue;
            int wallHeight = 1 + (Math.abs(dx * 5 + dz * 3) % 4);
            boolean cornerPillar = Math.abs(dx) == 3 && Math.abs(dz) == 3;
            if (cornerPillar) wallHeight = 5;
            for (int dy = 1; dy <= wallHeight; dy++) {
               int pattern = Math.abs(dx * 17 + dz * 31 + dy * 13) % 7;
               byte block = pattern == 0 ? B_MOSSY_STONEBRICK
                  : (pattern <= 2 ? B_CRACKED_STONEBRICK : B_STONEBRICK);
               setChunkLocal(c, lx, oy + dy, lz, block);
            }
         }
      }
      // A small broken arch and a weathered marker stone give the ruin a readable silhouette.
      setChunkLocal(c, ox - 1, oy + 4, oz + 3, B_STONEBRICK);
      setChunkLocal(c, ox + 1, oy + 4, oz + 3, B_STONEBRICK);
      setChunkLocal(c, ox, oy + 1, oz, B_MOSSY_STONEBRICK);
      setChunkLocal(c, ox, oy + 2, oz, B_CHEST);
      setChunkLocal(c, ox, oy + 1, oz - 1, B_TORCH);
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

   public static boolean isLeaves(byte id) {
      return id == B_LEAVES_OAK || id == B_LEAVES_SPRUCE || id == B_LEAVES_BIRCH;
   }

   public static boolean isLog(byte id) {
      return id == B_LOG_OAK || id == B_LOG_SPRUCE || id == B_LOG_BIRCH;
   }

   public static boolean isSlab(byte id) {
      return id == B_SLAB_OAK || id == B_SLAB_COBBLE || id == B_SLAB_STONEBRICK;
   }

   public static boolean isStair(byte id) {
      return id == B_STAIR_OAK || id == B_STAIR_COBBLE || id == B_STAIR_STONEBRICK;
   }

   public static boolean isCrossPlant(byte id) {
      return id == B_TORCH || id == B_TALL_GRASS || id == B_FLOWER_RED || id == B_FLOWER_YELLOW
         || id == B_WHEAT || id == B_SUGAR_CANE;
   }

   public static boolean isSolid(byte id) {
      return id != B_AIR && id != B_WATER && id != B_LAVA && !isCrossPlant(id)
         && id != B_LADDER && id != B_DOOR_OAK_OPEN;
   }

   public static double getBlockHeight(byte id) {
      if (!isSolid(id)) return 0.0;
      if (isSlab(id) || isStair(id)) return 0.5;
      if (id == B_BED_RED) return 0.5625;
      if (id == B_FARMLAND) return 0.9375;
      if (id == B_DOOR_OAK) return 2.0;
      return 1.0;
   }

   public static boolean isGlassBlock(byte id) {
      return id == B_GLASS || PeColors.isStainedGlass(id & 0xFF);
   }

   public static boolean isOpaque(byte id) {
      int blockId = id & 0xFF;
      return blockId != B_AIR && blockId != B_WATER && blockId != B_LAVA && blockId != B_GLASS
         && !PeColors.isStainedGlass(blockId)
         && !isLeaves(id) && !isSlab(id) && !isStair(id) && blockId != B_FARMLAND
         && blockId != B_ICE && blockId != B_SLIME_BLOCK && !isCrossPlant(id)
         && blockId != B_BARS && blockId != B_BED_RED && blockId != B_FENCE_OAK
         && blockId != B_DOOR_OAK && blockId != B_DOOR_OAK_OPEN && blockId != B_LADDER && blockId != B_CHEST && blockId != B_CACTUS;
   }

   public static boolean isLightEmitter(byte id) {
      return id == B_TORCH || id == B_GLOWSTONE || id == B_SEA_LANTERN
         || id == B_LAVA || id == B_REDSTONE_BLOCK || id == B_JACK_O_LANTERN;
   }

   public static int getFaceTile(byte id, int face) {
      int coloredTile = PeColors.tileForBlock(id & 0xFF);
      if (coloredTile >= 0) return coloredTile;
      switch (id & 0xFF) {
         case B_STONE: return PeTextures.T_STONE;
         case B_GRASS: return face == 0 ? PeTextures.T_GRASS_TOP : (face == 1 ? PeTextures.T_DIRT : PeTextures.T_GRASS_SIDE);
         case B_DIRT: return PeTextures.T_DIRT;
         case B_COBBLE:
         case B_SLAB_COBBLE:
         case B_STAIR_COBBLE: return PeTextures.T_COBBLE;
         case B_PLANKS_OAK:
         case B_SLAB_OAK:
         case B_STAIR_OAK:
         case B_FENCE_OAK: return PeTextures.T_PLANKS_OAK;
         case B_BEDROCK: return PeTextures.T_BEDROCK;
         case B_WATER: return PeTextures.T_WATER;
         case B_LAVA: return PeTextures.T_LAVA;
         case B_SAND: return PeTextures.T_SAND;
         case B_GRAVEL: return PeTextures.T_GRAVEL;
         case B_GOLD_ORE: return PeTextures.T_GOLD_ORE;
         case B_IRON_ORE: return PeTextures.T_IRON_ORE;
         case B_COAL_ORE: return PeTextures.T_COAL_ORE;
         case B_LOG_OAK: return (face <= 1) ? PeTextures.T_LOG_OAK_TOP : PeTextures.T_LOG_OAK_SIDE;
         case B_LOG_SPRUCE: return (face <= 1) ? PeTextures.T_LOG_OAK_TOP : PeTextures.T_LOG_SPRUCE;
         case B_LOG_BIRCH: return (face <= 1) ? PeTextures.T_LOG_OAK_TOP : PeTextures.T_LOG_BIRCH_SIDE;
         case B_LEAVES_OAK: return PeTextures.T_LEAVES_OAK;
         case B_LEAVES_SPRUCE: return PeTextures.T_LEAVES_SPRUCE;
         case B_LEAVES_BIRCH: return PeTextures.T_LEAVES_BIRCH;
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
         case B_CHEST: return face <= 1 ? PeTextures.T_CHEST_TOP : (face == 3 ? PeTextures.T_CHEST_FRONT : PeTextures.T_CHEST_SIDE);
         case B_DIAMOND_ORE: return PeTextures.T_DIAMOND_ORE;
         case (B_LAPIS_ORE & 0xFF): return PeTextures.T_LAPIS_ORE;
         case B_DIAMOND_BLOCK: return PeTextures.T_DIAMOND_BLOCK;
         case B_CRAFTING_TABLE: return face == 0 ? PeTextures.T_CRAFT_TOP : (face == 1 ? PeTextures.T_PLANKS_OAK : PeTextures.T_CRAFT_SIDE);
         case B_FURNACE: return face == 3 ? PeTextures.T_FURNACE_FRONT : PeTextures.T_FURNACE_SIDE;
         case B_REDSTONE_ORE: return PeTextures.T_REDSTONE_ORE;
         case B_SNOW: return face == 0 ? PeTextures.T_SNOW : (face == 1 ? PeTextures.T_DIRT : PeTextures.T_SNOW_SIDE);
         case B_ICE: return PeTextures.T_ICE;
         case B_NETHERRACK: return PeTextures.T_NETHERRACK;
         case B_SOUL_SAND: return PeTextures.T_SOUL_SAND;
         case B_GLOWSTONE: return PeTextures.T_GLOWSTONE;
         case B_STONEBRICK:
         case B_SLAB_STONEBRICK:
         case B_STAIR_STONEBRICK: return PeTextures.T_STONEBRICK;
         case B_MOSSY_STONEBRICK: return PeTextures.T_MOSSY_STONEBRICK;
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
         case B_PACKED_ICE: return PeTextures.T_PACKED_ICE;
         case B_BED_RED: return face == 0 ? PeTextures.T_BED_RED : PeTextures.T_PLANKS_OAK;
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
         case B_DOOR_OAK:
         case B_DOOR_OAK_OPEN: return PeTextures.T_DOOR_TOP;
         case B_LADDER: return PeTextures.T_LADDER;
         case B_FARMLAND: return face == 0 ? PeTextures.T_FARM_WET : PeTextures.T_DIRT;
         case B_WHEAT: return PeTextures.T_WHEAT;
         case B_SUGAR_CANE: return PeTextures.T_SUGAR_CANE;
         case B_PUMPKIN: return face <= 1 ? PeTextures.T_PUMPKIN_TOP : (face == 3 ? PeTextures.T_PUMPKIN_FACE : PeTextures.T_PUMPKIN_SIDE);
         case B_JACK_O_LANTERN: return face <= 1 ? PeTextures.T_PUMPKIN_TOP : (face == 3 ? PeTextures.T_JACK_O_LANTERN : PeTextures.T_PUMPKIN_SIDE);
         case B_MELON: return face <= 1 ? PeTextures.T_MELON_TOP : PeTextures.T_MELON_SIDE;
         case B_HAY_BALE: return face <= 1 ? PeTextures.T_HAY_TOP : PeTextures.T_HAY_SIDE;
         default: return PeTextures.T_STONE;
      }
   }

   public static int getItemTile(int id) {
      if (id < 0) id &= 0xFF;
      int dyeColor = PeColors.dyeColorForItem(id);
      if (dyeColor >= 0) return PeColors.FAMILY_TILES[0][dyeColor];
      if (id == B_GRASS) return PeTextures.T_GRASS_SIDE;
      if (id == B_SNOW) return PeTextures.T_SNOW_SIDE;
      if (id == B_BED_RED) return PeTextures.T_BED_RED;
      if (id > 0 && id <= MAX_BLOCK_ID) {
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
         case I_IRON_INGOT: return PeTextures.T_ITEM_IRON_INGOT;
         case I_GOLD_INGOT: return PeTextures.T_ITEM_GOLD_INGOT;
         case I_DIAMOND: return PeTextures.T_ITEM_DIAMOND;
         case I_COAL: return PeTextures.T_ITEM_COAL;
         case I_BREAD: return PeTextures.T_ITEM_BREAD;
         default: return PeTextures.T_ITEM_PICK_WOOD;
      }
   }

   public static String getItemName(int id) {
      if (id < 0) id &= 0xFF;
      int dyeColor = PeColors.dyeColorForItem(id);
      if (dyeColor >= 0) return PeColors.NAMES[dyeColor] + " Dye";
      String colorName = PeColors.nameForBlock(id);
      if (colorName != null) return colorName;
      switch (id) {
         case 0: return "Empty Hand";
         case B_GRASS: return "Grass Block";
         case B_STONE: return "Stone";
         case B_DIRT: return "Dirt";
         case B_COBBLE: return "Cobblestone";
         case B_MOSSY_COBBLE: return "Mossy Cobblestone";
         case B_PLANKS_OAK: return "Oak Planks";
         case B_PLANKS_SPRUCE: return "Spruce Planks";
         case B_PLANKS_BIRCH: return "Birch Planks";
         case B_SLAB_OAK: return "Oak Slab";
         case B_SLAB_COBBLE: return "Cobblestone Slab";
         case B_SLAB_STONEBRICK: return "Stone Brick Slab";
         case B_STAIR_OAK: return "Oak Stairs";
         case B_STAIR_COBBLE: return "Cobblestone Stairs";
         case B_STAIR_STONEBRICK: return "Stone Brick Stairs";
         case B_FENCE_OAK: return "Oak Fence";
         case B_DOOR_OAK: return "Oak Door";
         case B_DOOR_OAK_OPEN: return "Oak Door (Open)";
         case B_LADDER: return "Ladder";
         case B_BRICKS: return "Bricks";
         case B_STONEBRICK: return "Stone Bricks";
         case B_MOSSY_STONEBRICK: return "Mossy Stone Bricks";
         case B_CRACKED_STONEBRICK: return "Cracked Stone Bricks";
         case B_SAND: return "Sand";
         case B_GRAVEL: return "Gravel";
         case B_SANDSTONE: return "Sandstone";
         case B_RED_SANDSTONE: return "Red Sandstone";
         case B_GLASS: return "Glass";
         case B_LOG_OAK: return "Oak Log";
         case B_LOG_SPRUCE: return "Spruce Log";
         case B_LOG_BIRCH: return "Birch Log";
         case B_LEAVES_OAK: return "Oak Leaves";
         case B_LEAVES_SPRUCE: return "Spruce Leaves";
         case B_LEAVES_BIRCH: return "Birch Leaves";
         case B_WOOL_WHITE: return "White Wool";
         case B_WOOL_RED: return "Red Wool";
         case B_WOOL_BLUE: return "Blue Wool";
         case B_WOOL_GREEN: return "Green Wool";
         case B_WOOL_YELLOW: return "Yellow Wool";
         case B_GOLD_ORE: return "Gold Ore";
         case B_IRON_ORE: return "Iron Ore";
         case B_COAL_ORE: return "Coal Ore";
         case B_DIAMOND_ORE: return "Diamond Ore";
         case (B_LAPIS_ORE & 0xFF): return "Lapis Lazuli Ore";
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
         case B_FARMLAND: return "Farmland";
         case B_WHEAT: return "Ripe Wheat";
         case B_SUGAR_CANE: return "Sugar Cane";
         case B_PUMPKIN: return "Pumpkin";
         case B_JACK_O_LANTERN: return "Jack o'Lantern";
         case B_MELON: return "Melon";
         case B_HAY_BALE: return "Hay Bale";
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
         case I_BREAD: return "Fresh Bread";
         case I_ENDER_PEARL: return "Ender Pearl";
         case I_IRON_INGOT: return "Iron Ingot";
         case I_GOLD_INGOT: return "Gold Ingot";
         case I_DIAMOND: return "Diamond";
         case I_COAL: return "Coal";
         default: return "Block";
      }
   }

   // ====================================================================
   //  Right-Handed Camera Forward Vector & Raycasting
   // ====================================================================
   public int[] raycastBlock(double maxReach) {
      double ex = this.px;
      double ey = this.py + this.eyeHeight;
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
      for (double t = 0.2; t <= maxReach; t += 0.05) {
         int bx = (int)Math.floor(ex + dx * t);
         int by = (int)Math.floor(ey + dy * t);
         int bz = (int)Math.floor(ez + dz * t);
         byte b = getBlockFast(bx, by, bz);
         if (b != B_AIR && b != B_WATER && b != B_LAVA) {
            return new int[]{bx, by, bz, prevX, prevY, prevZ, b & 0xFF};
         }
         // 2-block tall Door hit check on upper half (by - 1)
         if (by > 1) {
            byte below = getBlockFast(bx, by - 1, bz);
            if (below == B_DOOR_OAK || below == B_DOOR_OAK_OPEN) {
               return new int[]{bx, by - 1, bz, prevX, prevY, prevZ, below & 0xFF};
            }
         }
         prevX = bx;
         prevY = by;
         prevZ = bz;
      }
      return null;
   }

   public void settleGravityColumnAbove(int wx, int wy, int wz) {
      for (int checkY = wy + 1; checkY < WORLD_H - 1; checkY++) {
         byte above = getBlockFast(wx, checkY, wz);
         boolean powder = PeColors.isPowder(above & 0xFF);
         if (above == B_SAND || above == B_GRAVEL || powder) {
            int destY = checkY;
            while (destY > 1) {
               byte below = getBlockFast(wx, destY - 1, wz);
               if (below == B_AIR || (powder && (below == B_WATER || below == B_LAVA || below == B_TALL_GRASS))) {
                  destY--;
               } else {
                  break;
               }
            }
            if (destY != checkY) {
               setBlock(wx, checkY, wz, B_AIR);
               setBlock(wx, destY, wz, above);
            }
         } else {
            break;
         }
      }
   }

   private boolean concretePowderTouchesWater(int x, int y, int z) {
      return getBlockFast(x + 1, y, z) == B_WATER || getBlockFast(x - 1, y, z) == B_WATER
         || getBlockFast(x, y, z + 1) == B_WATER || getBlockFast(x, y, z - 1) == B_WATER
         || getBlockFast(x, y + 1, z) == B_WATER;
   }

   /** One World-of-Color powder update: water hardens it; otherwise it falls through empty/fluid cells. */
   public void tickConcretePowderAt(int x, int y, int z) {
      if (y <= 1 || y >= WORLD_H - 1) return;
      byte powder = getBlockFast(x, y, z);
      int powderId = powder & 0xFF;
      if (!PeColors.isPowder(powderId)) return;
      if (concretePowderTouchesWater(x, y, z)) {
         setBlock(x, y, z, (byte)PeColors.hardenedBlock(powderId));
         return;
      }
      byte below = getBlockFast(x, y - 1, z);
      if (below == B_AIR || below == B_WATER || below == B_LAVA || below == B_TALL_GRASS) {
         setBlock(x, y, z, B_AIR);
         setBlock(x, y - 1, z, powder);
      }
   }

   private void tickConcretePowderNearPlayer() {
      int centerX = (int)Math.floor(this.px);
      int centerY = (int)Math.floor(this.py);
      int centerZ = (int)Math.floor(this.pz);
      // Staggered local simulation keeps mobile costs bounded and makes nearby powder react promptly.
      for (int dy = -4; dy <= 4; dy++) {
         int y = centerY + dy;
         if (y <= 1 || y >= WORLD_H - 1) continue;
         for (int dz = -4; dz <= 4; dz++) {
            for (int dx = -4; dx <= 4; dx++) {
               tickConcretePowderAt(centerX + dx, y, centerZ + dz);
            }
         }
      }
   }

   /**
    * PC Update 3.10 Animated 3D Tree Felling:
    * When chopping the base/trunk log of a natural tree, collects the trunk logs and canopy leaves,
    * removes them from the static chunk grid, and launches an animated 3D FallingTree that topples
    * smoothly from 0..90 degrees at high frame rates before crashing onto the ground and placing the fallen trunk!
    */
   public boolean fellTreeIfNatural(int x, int y, int z, byte logType) {
      int leavesAround = 0;
      for (int dy = 1; dy <= 7; dy++) {
         for (int dz = -2; dz <= 2; dz++) {
            for (int dx = -2; dx <= 2; dx++) {
               if (isLeaves(getBlockFast(x + dx, y + dy, z + dz))) {
                  leavesAround++;
               }
            }
         }
      }
      if (leavesAround < 4) return false;

      int[] tmpOx = new int[192];
      int[] tmpOy = new int[192];
      int[] tmpOz = new int[192];
      byte[] tmpId = new byte[192];
      int count = 0;

      // 1. Collect vertical trunk logs above the cut
      int trunkTop = 0;
      for (int dy = 1; dy <= 7; dy++) {
         byte above = getBlockFast(x, y + dy, z);
         if (isLog(above)) {
            tmpOx[count] = 0;
            tmpOy[count] = dy;
            tmpOz[count] = 0;
            tmpId[count] = above;
            count++;
            trunkTop = dy;
         } else {
            break;
         }
      }
      if (count == 0) return false;
      int nLogs = count;

      // 2. Collect canopy leaves attached to the trunk
      for (int dy = 1; dy <= trunkTop + 2; dy++) {
         for (int dz = -2; dz <= 2; dz++) {
            for (int dx = -2; dx <= 2; dx++) {
               if (count >= tmpId.length) break;
               byte b = getBlockFast(x + dx, y + dy, z + dz);
               if (isLeaves(b)) {
                  tmpOx[count] = dx;
                  tmpOy[count] = dy;
                  tmpOz[count] = dz;
                  tmpId[count] = b;
                  count++;
               }
            }
         }
      }

      // 3. Remove the collected logs & leaves from the static chunk grid while the tree is falling in 3D
      for (int i = count - 1; i >= 0; i--) {
         setBlock(x + tmpOx[i], y + tmpOy[i], z + tmpOz[i], B_AIR);
      }

      // 4. Choose fall direction pointing away from the player
      double awayX = (x + 0.5) - this.px;
      double awayZ = (z + 0.5) - this.pz;
      int fallDx = 1;
      int fallDz = 0;
      if (Math.abs(awayX) >= Math.abs(awayZ)) {
         fallDx = (awayX >= 0.0) ? 1 : -1;
         fallDz = 0;
      } else {
         fallDx = 0;
         fallDz = (awayZ >= 0.0) ? 1 : -1;
      }

      int[] ox = Arrays.copyOf(tmpOx, count);
      int[] oy = Arrays.copyOf(tmpOy, count);
      int[] oz = Arrays.copyOf(tmpOz, count);
      byte[] ids = Arrays.copyOf(tmpId, count);

      FallingTree tree = new FallingTree(x, y, z, fallDx, fallDz, nLogs, count, ox, oy, oz, ids);
      this.fallingTrees.add(tree);
      showBanner("TIMBER! Tree Falling!");
      return true;
   }

   public void tickFallingTrees(PeAudio audio) {
      if (this.fallingTrees.isEmpty()) return;
      for (int i = 0; i < this.fallingTrees.size(); i++) {
         FallingTree f = this.fallingTrees.get(i);
         if (f.done) continue;
         f.age++;
         if (f.age >= f.rotTicks) {
            landFallingTree(f, audio);
         }
      }
      for (int i = this.fallingTrees.size() - 1; i >= 0; i--) {
         if (this.fallingTrees.get(i).done) {
            this.fallingTrees.remove(i);
         }
      }
   }

   public void finishAllFallingTrees() {
      if (this.fallingTrees.isEmpty()) return;
      for (int i = 0; i < this.fallingTrees.size(); i++) {
         FallingTree f = this.fallingTrees.get(i);
         if (!f.done) {
            landFallingTree(f, null);
         }
      }
      this.fallingTrees.clear();
   }

   private boolean isReplaceableByFallingTree(byte b) {
      return b == B_AIR || isLeaves(b) || isCrossPlant(b) || b == B_SNOW;
   }

   private void landFallingTree(FallingTree f, PeAudio audio) {
      if (f.done) return;
      f.done = true;

      // 1. Place the fallen horizontal trunk logs on the ground along (f.dx, f.dz)
      for (int i = 0; i < f.nLogs; i++) {
         int along = f.offX[i] * f.dx + f.offZ[i] * f.dz;
         int side = -f.offX[i] * f.dz + f.offZ[i] * f.dx;
         int rx = f.pivotX + f.offY[i] * f.dx - side * f.dz;
         int ry = f.pivotY - along;
         int rz = f.pivotZ + f.offY[i] * f.dz + side * f.dx;

         // Drop down until resting on solid terrain
         while (ry > 2 && isReplaceableByFallingTree(getBlockFast(rx, ry - 1, rz))) {
            ry--;
         }
         // Step up at most 2 blocks if the terrain rises along the fall direction
         for (int up = 0; up <= 2; up++) {
            if (isReplaceableByFallingTree(getBlockFast(rx, ry + up, rz))) {
               setBlock(rx, ry + up, rz, f.blockId[i]);
               break;
            }
         }
      }

      // 2. Place canopy leaves around the fallen trunk where there is air
      for (int i = f.nLogs; i < f.nTotal; i++) {
         int along = f.offX[i] * f.dx + f.offZ[i] * f.dz;
         int side = -f.offX[i] * f.dz + f.offZ[i] * f.dx;
         int rx = f.pivotX + f.offY[i] * f.dx - side * f.dz;
         int ry = f.pivotY - along;
         int rz = f.pivotZ + f.offY[i] * f.dz + side * f.dx;

         int groundY = topSolidY(rx, rz);
         int targetY = Math.max(groundY + 1, Math.min(groundY + 3, ry));
         if (targetY > 1 && targetY < WORLD_H - 1 && getBlockFast(rx, targetY, rz) == B_AIR) {
            setBlock(rx, targetY, rz, f.blockId[i]);
         }
      }

      if (this.mode == MODE_SURVIVAL && f.nLogs > 0) {
         giveItem(f.blockId[0] & 0xFF, Math.min(2, f.nLogs));
      }
      if (audio != null) {
         audio.playSound("explode", 0.65F, 1.35F);
      }
   }

   private void awardSurvivalBlockDrop(byte broken) {
      if (this.mode != MODE_SURVIVAL || broken == B_AIR) return;
      if (broken == B_TALL_GRASS) return;
      if (broken == B_LAPIS_ORE) {
         giveItem(I_DYE_BLUE, 4 + (int)(this.tickCount & 3L));
         showBanner("Found Blue Dye from Lapis Ore");
         return;
      }
      if (isLeaves(broken)) {
         if ((this.tickCount & 3L) == 0L) {
            giveItem(B_PLANKS_OAK, 1);
         } else if ((this.tickCount & 7L) == 1L) {
            giveItem(I_GOLDEN_APPLE, 1);
            showBanner("Found a Golden Apple in the leaves!");
         }
         return;
      }
      int drop;
      if (broken == B_STONE) drop = B_COBBLE;
      else if (broken == B_GRASS || broken == B_FARMLAND) drop = B_DIRT;
      else if (broken == B_COAL_ORE) drop = I_COAL;
      else if (broken == B_DIAMOND_ORE) drop = I_DIAMOND;
      else if (broken == B_WHEAT) drop = I_BREAD;
      else if (broken == B_MELON) drop = I_STEAK;
      else if (broken == B_DOOR_OAK_OPEN) drop = B_DOOR_OAK;
      else drop = broken & 0xFF;
      giveItem(drop, 1);
   }

   // Dedicated Break / Mine tap action
   public void triggerAttackAction(PeAudio audio) {
      this.swingAnim = 0.0F;
      int[] hit = raycastBlock(5.5);
      if (hit != null && hit[6] != B_BEDROCK) {
         byte target = (byte)hit[6];
         if (this.mode == MODE_CREATIVE || isCrossPlant(target)) {
            setBlock(hit[0], hit[1], hit[2], B_AIR);
            settleGravityColumnAbove(hit[0], hit[1], hit[2]);
            if (isLog(target)) {
               fellTreeIfNatural(hit[0], hit[1], hit[2], target);
            }
            awardSurvivalBlockDrop(target);
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

      // 1. Check if tapping a Crafting Table, Furnace, Chest, Door, TNT, or Bed in the world
      int[] hit = raycastBlock(5.0);
      if (hit != null && !this.sneaking) {
         byte targetBlock = (byte)hit[6];
         if (targetBlock == B_CRAFTING_TABLE || targetBlock == B_FURNACE || targetBlock == B_CHEST) {
            this.previousScreen = SCREEN_PLAY;
            this.invCategory = (targetBlock == B_CHEST) ? 0 : 1;
            this.screen = SCREEN_INVENTORY;
            if (audio != null) audio.playSound("click", 0.85F, 1.1F);
            return;
         }
         if (targetBlock == B_BED_RED) {
            this.dayTime = 0.22F;
            this.health = 20;
            this.spawnX = hit[0] + 0.5;
            this.spawnY = hit[1] + 1.0;
            this.spawnZ = hit[2] + 0.5;
            showBanner("Slept through the night! Spawn point set.");
            if (audio != null) audio.playSound("levelup", 0.85F, 1.0F);
            return;
         }
         if (targetBlock == B_DOOR_OAK || targetBlock == B_DOOR_OAK_OPEN) {
            setBlock(hit[0], hit[1], hit[2], targetBlock == B_DOOR_OAK ? B_DOOR_OAK_OPEN : B_DOOR_OAK);
            if (audio != null) audio.playSound("click_block", 0.85F, targetBlock == B_DOOR_OAK ? 1.15F : 0.9F);
            return;
         }
         if (targetBlock == B_TNT) {
            detonateTnt(hit[0], hit[1], hit[2], audio);
            return;
         }
      }

      // 2. Eat Food if holding Golden Apple, Steak, or Bread
      if (held == I_GOLDEN_APPLE || held == I_STEAK || held == I_BREAD) {
         if (this.health < 20 || this.hunger < 20) {
            int heal = (held == I_GOLDEN_APPLE) ? 8 : (held == I_STEAK ? 6 : 5);
            this.health = Math.min(20, this.health + heal);
            this.hunger = Math.min(20, this.hunger + 6);
            consumeSelectedSlot(1);
            showBanner("Ate " + getItemName(held) + " (+" + (heal / 2) + " Hearts)");
            if (audio != null) audio.playSound("eat_bite", 0.85F, 1.0F);
         } else {
            showBanner("Health & Hunger are already full!");
         }
         return;
      }

      // 3. Throw Ender Pearl to Teleport!
      if (held == I_ENDER_PEARL) {
         int[] farHit = raycastBlock(18.0);
         if (farHit != null) {
            this.px = farHit[3] + 0.5;
            this.py = Math.max(farHit[4], topSolidY(farHit[3], farHit[5]) + 1.0);
            this.pz = farHit[5] + 0.5;
            this.vx = this.vy = this.vz = 0.0;
            syncInterpolationState();
            consumeSelectedSlot(1);
            showBanner("Teleported with Ender Pearl!");
            if (audio != null) audio.playSound("levelup", 0.9F, 1.25F);
         }
         return;
      }

      // 4. Fire Bow!
      if (held == I_BOW) {
         int[] bowHit = raycastBlock(24.0);
         if (bowHit != null) {
            byte hitBlock = (byte)bowHit[6];
            if (hitBlock == B_TNT) {
               detonateTnt(bowHit[0], bowHit[1], bowHit[2], audio);
            } else if (hitBlock == B_GLASS || isLeaves(hitBlock) || isCrossPlant(hitBlock)) {
               setBlock(bowHit[0], bowHit[1], bowHit[2], B_AIR);
               awardSurvivalBlockDrop(hitBlock);
            }
         }
         if (audio != null) audio.playSound("swing_1", 0.85F, 1.35F);
         return;
      }

      // 5. Place Block at targeted block face
      if (hit != null && held > 0 && held <= MAX_BLOCK_ID) {
         int pxPlace = hit[3], pyPlace = hit[4], pzPlace = hit[5];
         byte heldByte = (byte)held;
         boolean overlapsPlayer = isSolid(heldByte)
            && Math.abs((pxPlace + 0.5) - this.px) < 0.76
            && Math.abs((pzPlace + 0.5) - this.pz) < 0.76
            && (pyPlace + getBlockHeight(heldByte)) > this.py + 0.05
            && pyPlace < this.py + 1.75;
         byte existing = getBlock(pxPlace, pyPlace, pzPlace);
         if (!overlapsPlayer && (existing == B_AIR || existing == B_WATER || existing == B_TALL_GRASS)) {
            int finalY = pyPlace;
            if (heldByte == B_SAND || heldByte == B_GRAVEL) {
               while (finalY > 1 && getBlockFast(pxPlace, finalY - 1, pzPlace) == B_AIR) {
                  finalY--;
               }
            }
            setBlock(pxPlace, finalY, pzPlace, heldByte);
            if (PeColors.isPowder(held & 0xFF)) tickConcretePowderAt(pxPlace, finalY, pzPlace);
            if (this.mode != MODE_CREATIVE) {
               consumeSelectedSlot(1);
            }
            if (audio != null) audio.playSound("click_block", 0.8F, 1.05F);
         }
      }
   }

   public void detonateTnt(int tx, int ty, int tz, PeAudio audio) {
      setBlock(tx, ty, tz, B_AIR);
      for (int dy = -2; dy <= 2; dy++) {
         for (int dz = -2; dz <= 2; dz++) {
            for (int dx = -2; dx <= 2; dx++) {
               if (dx * dx + dy * dy + dz * dz <= 6) {
                  byte b = getBlockFast(tx + dx, ty + dy, tz + dz);
                  if (b != B_AIR && b != B_BEDROCK && b != B_OBSIDIAN) {
                     setBlock(tx + dx, ty + dy, tz + dz, B_AIR);
                  }
               }
            }
         }
      }
      showBanner("BOOOOM! TNT Detonated!");
      if (audio != null) audio.playSound("explode", 1.0F, 0.9F);
   }

   // Legacy alias for tests
   public void triggerPrimaryAction(PeAudio audio) {
      int held = this.hotbarId[this.selectedSlot];
      if (held > 0 && held <= MAX_BLOCK_ID) {
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
      // Snapshot previous tick state for high-refresh sub-tick camera & view-bobbing interpolation!
      this.prevPx = this.px;
      this.prevPy = this.py;
      this.prevPz = this.pz;
      this.prevWalkDist = this.walkDist;
      this.prevEyeHeight = this.eyeHeight;
      this.prevSwingAnim = this.swingAnim;

      this.tickCount++;
      if ((this.tickCount & 7L) == 0L) tickConcretePowderNearPlayer();
      if (this.hurtTime > 0) this.hurtTime--;
      if (this.invulnTicks > 0) this.invulnTicks--;
      if (this.bannerTicks > 0) this.bannerTicks--;
      if (this.swingAnim >= 0.0F) {
         this.swingAnim += 0.16F;
         if (this.swingAnim >= 1.0F) this.swingAnim = -1.0F;
      }
      this.dayTime = (this.dayTime + 0.00008F) % 1.0F;

      // Advance animated 3D falling trees (0..90 degrees topple + landing on ground)
      tickFallingTrees(audio);

      // Smoothly interpolate eye height when sneaking vs standing
      float targetEye = (this.sneaking && !this.flying) ? 1.46F : 1.62F;
      this.eyeHeight += (targetEye - this.eyeHeight) * 0.45F;

      // Stream at most 1 new chunk per tick while moving for zero frame hitches at high frame rates
      ensureChunksAroundPlayer(1);

      // 1. Player Movement & Right-Handed Camera Basis:
      //    Forward = ( +sin(yaw), -cos(yaw) )
      //    Right   = ( +cos(yaw), +sin(yaw) )
      double moveFwd = touch != null ? touch.moveForward : 0.0;
      double moveStrafe = touch != null ? touch.moveStrafe : 0.0;
      boolean wantJump = touch != null && touch.jumpHeld;
      boolean wantDescend = touch != null && touch.sneakHeld;

      this.sprinting = (moveFwd > 0.72 && !this.sneaking);
      double speed = this.flying ? 0.36 : (this.sneaking ? 0.10 : (this.sprinting ? 0.232 : 0.182));
      double yawRad = Math.toRadians(this.yaw);
      double sinY = Math.sin(yawRad);
      double cosY = Math.cos(yawRad);
      double wishX = (sinY * moveFwd + cosY * moveStrafe) * speed;
      double wishZ = (-cosY * moveFwd + sinY * moveStrafe) * speed;

      // Smooth horizontal velocity damping
      this.vx = this.vx * 0.42 + wishX * 0.58;
      this.vz = this.vz * 0.42 + wishZ * 0.58;
      if (Math.abs(this.vx) < 0.0005) this.vx = 0.0;
      if (Math.abs(this.vz) < 0.0005) this.vz = 0.0;

      int bx = (int)Math.floor(this.px);
      int byFoot = (int)Math.floor(this.py + 0.2);
      int byBody = (int)Math.floor(this.py + 0.9);
      int bz = (int)Math.floor(this.pz);
      byte inFoot = getBlockFast(bx, byFoot, bz);
      byte inBody = getBlockFast(bx, byBody, bz);
      boolean inWater = (inFoot == B_WATER || inBody == B_WATER);
      boolean onLadder = (inFoot == B_LADDER || inBody == B_LADDER);

      if (this.flying) {
         this.vy = wantJump ? 0.25 : (wantDescend ? -0.25 : 0.0);
      } else if (onLadder) {
         if (wantJump || moveFwd > 0.25) {
            this.vy = 0.17;
         } else if (wantDescend) {
            this.vy = 0.0;
         } else {
            this.vy = -0.12;
         }
      } else if (inWater) {
         this.vy = wantJump ? 0.15 : Math.max(-0.16, this.vy - 0.025);
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
         this.px = this.spawnX;
         this.py = Math.max(this.spawnY, topSolidY((int)Math.floor(this.spawnX), (int)Math.floor(this.spawnZ)) + 1.0);
         this.pz = this.spawnZ;
         this.vx = this.vy = this.vz = 0.0;
         this.health = 20;
         syncInterpolationState();
         showBanner("Respawned at Spawn Point!");
      }

      // 2. Hold-to-Mine Block Progression (ONLY when miningHeld is explicitly active!)
      if (touch != null && touch.miningHeld) {
         int[] hit = raycastBlock(5.0);
         if (hit != null && hit[6] != B_BEDROCK) {
            if (hit[0] == this.mineX && hit[1] == this.mineY && hit[2] == this.mineZ) {
               int held = this.hotbarId[this.selectedSlot];
               float toolMult = (held == I_PICK_DIAMOND || held == I_SHOVEL_DIAMOND) ? 2.4F
                  : ((held == I_PICK_IRON || held == I_AXE_IRON) ? 1.8F : (held == I_PICK_WOOD ? 1.35F : 1.0F));
               float rate = (this.mode == MODE_CREATIVE) ? 0.55F : (0.072F * toolMult);
               this.mineProgress += rate;
               this.swingAnim = (this.tickCount % 6) / 6.0F;
               if (this.mineProgress >= 1.0F) {
                  byte broken = getBlock(this.mineX, this.mineY, this.mineZ);
                  setBlock(this.mineX, this.mineY, this.mineZ, B_AIR);
                  settleGravityColumnAbove(this.mineX, this.mineY, this.mineZ);
                  if (isLog(broken)) {
                     fellTreeIfNatural(this.mineX, this.mineY, this.mineZ, broken);
                  }
                  awardSurvivalBlockDrop(broken);
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

   /**
    * Checks whether the player AABB [x - 0.28, x + 0.28] x [y, y + 1.76] x [z - 0.28, z + 0.28]
    * intersects any solid block (accounting for 0.5-height slabs and beds).
    */
   private boolean collidesAABB(double x, double y, double z) {
      double r = 0.28;
      int minX = (int)Math.floor(x - r);
      int maxX = (int)Math.floor(x + r);
      int minY = (int)Math.floor(y + 0.001);
      int maxY = (int)Math.floor(y + 1.76);
      int minZ = (int)Math.floor(z - r);
      int maxZ = (int)Math.floor(z + r);

      for (int by = minY; by <= maxY; by++) {
         for (int bz = minZ; bz <= maxZ; bz++) {
            for (int bx = minX; bx <= maxX; bx++) {
               byte id = getBlockFast(bx, by, bz);
               double bh = getBlockHeight(id);
               if (bh > 0.0 && y < by + bh && y + 1.76 > by) {
                  return true;
               }
            }
         }
      }
      return false;
   }

   private double findSupportingFloorY(double x, double y, double z) {
      double r = 0.28;
      int minX = (int)Math.floor(x - r);
      int maxX = (int)Math.floor(x + r);
      int minZ = (int)Math.floor(z - r);
      int maxZ = (int)Math.floor(z + r);
      int baseBy = (int)Math.floor(y);
      double bestTop = -1.0;

      for (int by = baseBy - 1; by <= baseBy + 1; by++) {
         for (int bz = minZ; bz <= maxZ; bz++) {
            for (int bx = minX; bx <= maxX; bx++) {
               byte id = getBlockFast(bx, by, bz);
               double bh = getBlockHeight(id);
               if (bh > 0.0) {
                  double top = by + bh;
                  if (top <= y + 0.58 && top > bestTop) {
                     bestTop = top;
                  }
               }
            }
         }
      }
      return bestTop;
   }

   /**
    * Smooth Independent-Axis AABB Box Collision with:
    * - Automatic 0.5625-block step-up onto Slabs & Beds (zero vertical bounce!)
    * - Optional 1.0-block Pocket Edition Auto-Jump onto full blocks
    * - Smooth wall sliding along X and Z independently
    */
   private void movePlayerWithCollision() {
      double x = this.px;
      double y = this.py;
      double z = this.pz;

      // 1. Resolve X-axis movement independently
      double nx = x + this.vx;
      if (this.vx != 0.0 && collidesAABB(nx, y, z)) {
         if (this.onGround && !collidesAABB(nx, y + 0.5625, z)) {
            double floorTop = findSupportingFloorY(nx, y + 0.5625, z);
            if (floorTop > y && floorTop - y <= 0.58) {
               y = floorTop;
            }
         } else if (this.onGround && this.autoJump && !this.sneaking && !this.flying && !collidesAABB(nx, y + 1.05, z)) {
            this.vy = 0.42;
            this.onGround = false;
            nx = x;
         } else {
            nx = x;
            this.vx = 0.0;
         }
      }

      // 2. Resolve Z-axis movement independently
      double nz = z + this.vz;
      if (this.vz != 0.0 && collidesAABB(nx, y, nz)) {
         if (this.onGround && !collidesAABB(nx, y + 0.5625, nz)) {
            double floorTop = findSupportingFloorY(nx, y + 0.5625, nz);
            if (floorTop > y && floorTop - y <= 0.58) {
               y = floorTop;
            }
         } else if (this.onGround && this.autoJump && !this.sneaking && !this.flying && !collidesAABB(nx, y + 1.05, nz)) {
            this.vy = 0.42;
            this.onGround = false;
            nz = z;
         } else {
            nz = z;
            this.vz = 0.0;
         }
      }

      // 3. Sneak edge protection (prevent walking off ledges while sneaking on ground)
      if (this.sneaking && this.onGround && !this.flying) {
         if (!collidesAABB(nx, y - 0.58, z)) {
            nx = x;
            this.vx = 0.0;
         }
         if (!collidesAABB(nx, y - 0.58, nz)) {
            nz = z;
            this.vz = 0.0;
         }
      }

      // 4. Resolve Y-axis movement
      double ny = y + this.vy;
      boolean grounded = false;
      if (this.vy <= 0.0) {
         if (collidesAABB(nx, ny, nz)) {
            double floorTop = findSupportingFloorY(nx, y, nz);
            if (floorTop > 0.0) {
               ny = floorTop;
            } else {
               ny = Math.floor(ny + 0.05) + 1.0;
            }
            this.vy = 0.0;
            grounded = true;
         }
      } else {
         if (collidesAABB(nx, ny, nz)) {
            ny = y;
            this.vy = 0.0;
         }
      }

      double movedHoriz = Math.hypot(nx - this.px, nz - this.pz);
      if (movedHoriz > 0.0005 && grounded) {
         this.walkDist += (float)movedHoriz;
      }
      this.px = nx;
      this.py = ny;
      this.pz = nz;
      this.onGround = grounded;
   }
}
