package com.blockcraft.pe;

import android.content.Context;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.zip.GZIPInputStream;

/**
 * Blockcraft PE (Pocket Edition) World, Block/Item Registry, Terrain & Minigame Map Generator,
 * Physics Collision, Raycasting, Projectiles, Explosions, and Dynamic AI Bots.
 */
public final class PeWorld {
   // Game Modes
   public static final int MODE_SURVIVAL = 0;
   public static final int MODE_CREATIVE = 1;
   public static final int MODE_BATTLE = 2;
   public static final int MODE_TUMBLE = 3;
   public static final int MODE_BEDWARS = 4;
   public static final int MODE_CS2 = 5;
   public static final int MODE_END_FIGHT = 6;
   public static final int MODE_CASTLE_SIEGE = 7;
   public static final String[] MODE_NAMES = new String[]{
      "Survival World",
      "Creative World",
      "Battle Brawl",
      "Tumble Spleef",
      "Bedwars 4-Team",
      "CS2 Tactical",
      "End Bed Fight",
      "Castle Siege"
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
   public static final int I_PISTOL = 111;
   public static final int I_MACHINE_GUN = 112;
   public static final int I_SNIPER = 113;
   public static final int I_TNT_GRENADE = 114;
   public static final int I_C4_BOMB = 115;
   public static final int I_SIEGE_CANNON = 116;
   public static final int I_MORTAR = 117;
   public static final int I_SNOWBALL = 118;

   // World Dimensions: 10x10 chunks (160x160 horizontal: wx, wz in -80..+79), Y in 0..127
   public static final int CHUNK_GRID = 10;
   public static final int HALF_CHUNKS = 5;
   public static final int WORLD_W = CHUNK_GRID * 16; // 160
   public static final int HALF_W = WORLD_W / 2;      // 80
   public static final int WORLD_H = 128;
   public static final int SECTIONS_Y = WORLD_H / 16; // 8
   public static final int Y_DECK = 64;

   public final byte[] blocks = new byte[WORLD_W * WORLD_W * WORLD_H];
   public final boolean[][] sectionDirty = new boolean[CHUNK_GRID * CHUNK_GRID][SECTIONS_Y];

   public int mode = MODE_SURVIVAL;
   public int mapId = 0;
   public boolean cs2NoBombs = false;
   public long seed = 1337L;
   public long tickCount = 0L;
   public float dayTime = 0.22F; // 0..1 day/night cycle

   // Player State
   public double px = 0.5;
   public double py = Y_DECK + 2.0;
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

   // Hitmarker & Match State
   public int hitmarkerTicks = 0;
   public boolean hitmarkerCrit = false;
   public int kills = 0;
   public int battleGraceTicks = 0;
   public boolean cs2BombPlanted = false;
   public int cs2BombTimer = 0;
   public int siegeZoneA = 0;
   public int siegeZoneB = 0;
   public String bannerMessage = "";
   public int bannerTicks = 0;

   // Entities: Mobs / AI Bots & Projectiles
   public static final class PeMob {
      public int id;
      public int type; // 0=Bot/Fighter, 1=Zombie, 2=Skeleton, 3=Creeper, 4=Pig, 5=Sheep
      public int team; // -1=FFA, 0=Red/CT/Def, 1=Blue/T/Atk, 2=Green, 3=Yellow
      public int archetype; // 0=Rusher, 1=Looter, 2=Sniper, 3=Flanker
      public int roamIdx;
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
      public int type; // 0=Arrow/Bullet, 1=TNT Grenade, 2=Snowball/Pearl, 3=Siege Shell, 4=Mortar
      public int sourceTeam; // -2 = Player, -1 = Bot FFA, 0..3 = Team
      public double x, y, z;
      public double vx, vy, vz;
      public int fuse = 60;
      public int damage = 6;
      public boolean dead = false;
   }

   public final ArrayList<PeMob> mobs = new ArrayList<>();
   public final ArrayList<PeProjectile> projectiles = new ArrayList<>();
   private static byte[] cachedConsoleArenas = null;
   private final Random rng = new Random();

   // Map Names per mode
   public static final String[] BATTLE_MAP_NAMES = new String[]{
      "Cove", "Crucible", "Cavern", "Temple", "Lair", "Castle", "Frontier", "Shrunk"
   };
   public static final String[] CS2_MAP_NAMES = new String[]{
      "Dust II", "Mirage", "Inferno", "Nuke", "Overpass", "Train", "Vertigo", "Anubis"
   };
   public static final String[] SIEGE_MAP_NAMES = new String[]{
      "Ironkeep", "Helm's Deep", "Dragonstone", "Frosthold", "Sandstorm", "Blackrock", "Skyhaven", "Dreadfort"
   };

   public static final int[][][] BATTLE_ROAM_WAYPOINTS = new int[][][]{
      {{-32, 74, -6}, {31, 69, -23}, {-28, 80, 27}, {25, 82, 25}, {13, 81, -25}, {-14, 83, -18}, {16, 80, 33}, {-2, 68, 37}},
      {{-26, 70, -21}, {33, 68, 28}, {28, 57, -28}, {-29, 57, 30}, {1, 60, -38}, {0, 54, 32}, {-14, 54, -14}, {26, 65, -20}},
      {{-27, 78, 16}, {25, 77, -21}, {30, 83, 0}, {1, 76, -31}, {9, 85, 27}, {1, 76, 35}, {-19, 79, -16}, {28, 77, 18}},
      {{-25, 77, -4}, {16, 69, 24}, {-37, 68, -36}, {14, 72, -35}, {30, 74, 0}, {-37, 74, 17}, {-13, 64, 32}, {26, 70, -20}},
      {{-32, 82, -6}, {29, 71, 25}, {25, 71, -27}, {21, 81, -25}, {0, 68, -30}, {30, 63, 0}, {-35, 63, 2}, {-18, 70, -10}},
      {{-19, 65, 22}, {21, 65, -35}, {-41, 77, -12}, {22, 70, 37}, {-1, 75, -30}, {-42, 64, 0}, {31, 63, 0}, {-13, 69, -26}},
      {{-16, 67, -27}, {28, 70, -8}, {24, 79, -23}, {-38, 81, -58}, {50, 68, 0}, {-51, 65, 1}, {-16, 76, 16}, {26, 67, 22}},
      {{-25, 65, 18}, {34, 68, 0}, {25, 78, -33}, {40, 86, -14}, {33, 61, -33}, {-17, 60, -32}, {-3, 67, 28}, {29, 63, 29}}
   };

   private static final byte[] CONSOLE_PALETTE_MAP = new byte[]{
      B_AIR, B_CHEST, B_WATER, B_LAVA, B_GRASS, B_GRASS, B_GRASS, B_DIRT,
      B_DIRT, B_SAND, B_SAND, B_GRAVEL, B_SANDSTONE, B_RED_SANDSTONE, B_MOSSY_COBBLE, B_COBBLE,
      B_MOSSY_COBBLE, B_STONEBRICK, B_NETHER_BRICKS, B_NETHERRACK, B_SOUL_SAND, B_OBSIDIAN, B_GLOWSTONE, B_SEA_LANTERN,
      B_PRISMARINE, B_QUARTZ_BLOCK, B_PURPUR, B_QUARTZ_BLOCK, B_GOLD_BLOCK, B_DIAMOND_BLOCK, B_EMERALD_BLOCK, B_IRON_BLOCK,
      B_COAL_BLOCK, B_WOOL_RED, B_TERRACOTTA, B_WOOL_YELLOW, B_SNOW, B_PRISMARINE, B_TERRACOTTA, B_PACKED_ICE,
      B_ICE, B_SLIME_BLOCK, B_GLASS, B_BOOKSHELF, B_BRICKS, B_PLANKS_SPRUCE, B_PLANKS_SPRUCE, B_PLANKS_OAK,
      B_PLANKS_BIRCH, B_PLANKS_OAK, B_LOG_OAK, B_LOG_SPRUCE, B_LOG_OAK, B_LEAVES_OAK, B_BARS, B_PLANKS_OAK,
      B_TORCH, B_COAL_ORE, B_IRON_ORE, B_GOLD_ORE, B_DIAMOND_ORE, B_REDSTONE_ORE, B_TNT, B_STONE
   };

   public String currentMapName() {
      int m = Math.floorMod(this.mapId, 8);
      if (this.mode == MODE_CS2) {
         return CS2_MAP_NAMES[m];
      }
      if (this.mode == MODE_CASTLE_SIEGE) {
         return SIEGE_MAP_NAMES[m];
      }
      if (this.mode == MODE_SURVIVAL || this.mode == MODE_CREATIVE) {
         return "Overworld Biomes";
      }
      return BATTLE_MAP_NAMES[m];
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
      // face: 0=+Y(top), 1=-Y(bottom), 2=-Z(north), 3=+Z(south), 4=-X(west), 5=+X(east)
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
         case I_PISTOL: return PeTextures.T_ITEM_PISTOL;
         case I_MACHINE_GUN: return PeTextures.T_ITEM_MACHINE_GUN;
         case I_SNIPER: return PeTextures.T_ITEM_SNIPER;
         case I_TNT_GRENADE: return PeTextures.T_ITEM_TNT_GRENADE;
         case I_C4_BOMB: return PeTextures.T_ITEM_C4_BOMB;
         case I_SIEGE_CANNON: return PeTextures.T_ITEM_SIEGE_CANNON;
         case I_MORTAR: return PeTextures.T_ITEM_MORTAR;
         case I_SNOWBALL: return PeTextures.T_ITEM_SNOWBALL;
         default: return PeTextures.T_ITEM_SWORD_IRON;
      }
   }

   public static String getItemName(int id) {
      switch (id) {
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
         case B_SANDSTONE: return "Sandstone";
         case B_GLASS: return "Glass";
         case B_LOG_OAK: return "Oak Log";
         case B_LEAVES_OAK: return "Oak Leaves";
         case B_WOOL_RED: return "Red Wool";
         case B_WOOL_BLUE: return "Blue Wool";
         case B_WOOL_GREEN: return "Green Wool";
         case B_WOOL_YELLOW: return "Yellow Wool";
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
         case B_TNT: return "TNT Explosive";
         case B_SLIME_BLOCK: return "Slime Jump Block";
         case B_BED_RED: return "Explosive Bed";
         case B_CRAFTING_TABLE: return "Crafting Table";
         case B_CHEST: return "Supply Chest";
         case I_SWORD_IRON: return "Iron Sword";
         case I_SWORD_DIAMOND: return "Diamond Sword";
         case I_PICK_IRON: return "Iron Pickaxe";
         case I_PICK_DIAMOND: return "Diamond Pickaxe";
         case I_SHOVEL_DIAMOND: return "Tumble Shovel";
         case I_AXE_IRON: return "Battle Axe";
         case I_BOW: return "Tactical Bow";
         case I_ARROW: return "Arrows / Ammo";
         case I_GOLDEN_APPLE: return "Golden Apple";
         case I_STEAK: return "Cooked Steak";
         case I_ENDER_PEARL: return "Ender Pearl";
         case I_PISTOL: return "CS2 Tactical Pistol";
         case I_MACHINE_GUN: return "CS2 Assault SMG";
         case I_SNIPER: return "AWP Sniper Rifle";
         case I_TNT_GRENADE: return "TNT Frag Grenade";
         case I_C4_BOMB: return "C4 Explosive";
         case I_SIEGE_CANNON: return "Siege Cannon";
         case I_MORTAR: return "Mortar Launcher";
         case I_SNOWBALL: return "Spleef Snowball";
         default: return "Block";
      }
   }

   public void generateWorld(Context context, int newMode, int newMapId) {
      this.mode = newMode;
      this.mapId = Math.floorMod(newMapId, 8);
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
      this.cs2BombPlanted = false;
      this.cs2BombTimer = 0;
      this.siegeZoneA = 0;
      this.siegeZoneB = 0;
      this.battleGraceTicks = (newMode == MODE_BATTLE) ? 100 : 0;

      switch (newMode) {
         case MODE_SURVIVAL:
         case MODE_CREATIVE:
            generateOverworldBiomes();
            break;
         case MODE_BATTLE:
            generateBattleArena(context);
            break;
         case MODE_TUMBLE:
            generateTumbleArena();
            break;
         case MODE_BEDWARS:
            generateBedwarsArena();
            break;
         case MODE_CS2:
            generateCs2Arena();
            break;
         case MODE_END_FIGHT:
            generateEndFightArena();
            break;
         case MODE_CASTLE_SIEGE:
            generateCastleSiegeArena();
            break;
      }
      equipModeLoadout();
      markAllDirty();
      showBanner(MODE_NAMES[newMode] + " - " + currentMapName());
   }

   public void showBanner(String msg) {
      this.bannerMessage = msg;
      this.bannerTicks = 110;
   }

   private void equipModeLoadout() {
      Arrays.fill(this.hotbarId, 0);
      Arrays.fill(this.hotbarCount, 0);
      this.selectedSlot = 0;
      switch (this.mode) {
         case MODE_SURVIVAL:
            setSlot(0, I_SWORD_IRON, 1);
            setSlot(1, I_PICK_IRON, 1);
            setSlot(2, I_AXE_IRON, 1);
            setSlot(3, B_TORCH, 32);
            setSlot(4, B_PLANKS_OAK, 32);
            setSlot(5, B_COBBLE, 32);
            setSlot(6, I_BOW, 1);
            setSlot(7, I_ARROW, 24);
            setSlot(8, I_STEAK, 8);
            break;
         case MODE_CREATIVE:
            setSlot(0, B_STONEBRICK, 64);
            setSlot(1, B_PLANKS_OAK, 64);
            setSlot(2, B_BRICKS, 64);
            setSlot(3, B_GLASS, 64);
            setSlot(4, B_GOLD_BLOCK, 64);
            setSlot(5, B_DIAMOND_BLOCK, 64);
            setSlot(6, B_SEA_LANTERN, 64);
            setSlot(7, B_TNT, 64);
            setSlot(8, I_SWORD_DIAMOND, 1);
            break;
         case MODE_BATTLE:
            setSlot(0, I_SWORD_IRON, 1);
            setSlot(1, I_BOW, 1);
            setSlot(2, I_GOLDEN_APPLE, 2);
            setSlot(3, I_TNT_GRENADE, 3);
            setSlot(4, B_PLANKS_OAK, 24);
            setSlot(5, I_ENDER_PEARL, 2);
            setSlot(6, I_STEAK, 6);
            setSlot(7, I_ARROW, 24);
            break;
         case MODE_TUMBLE:
            setSlot(0, I_SHOVEL_DIAMOND, 1);
            setSlot(1, I_SNOWBALL, 48);
            setSlot(2, I_TNT_GRENADE, 2);
            setSlot(3, I_GOLDEN_APPLE, 1);
            break;
         case MODE_BEDWARS:
            setSlot(0, I_SWORD_IRON, 1);
            setSlot(1, B_WOOL_RED, 64);
            setSlot(2, I_PICK_IRON, 1);
            setSlot(3, B_END_STONE, 24);
            setSlot(4, I_TNT_GRENADE, 3);
            setSlot(5, I_GOLDEN_APPLE, 2);
            setSlot(6, I_BOW, 1);
            setSlot(7, I_ARROW, 16);
            break;
         case MODE_CS2:
            setSlot(0, I_MACHINE_GUN, 1);
            setSlot(1, I_SNIPER, 1);
            setSlot(2, I_PISTOL, 1);
            setSlot(3, I_TNT_GRENADE, 3);
            if (!this.cs2NoBombs) {
               setSlot(4, I_C4_BOMB, 1);
            }
            setSlot(5, I_GOLDEN_APPLE, 2);
            setSlot(6, B_STONEBRICK, 16);
            setSlot(7, I_ARROW, 64);
            break;
         case MODE_END_FIGHT:
            setSlot(0, I_SWORD_DIAMOND, 1);
            setSlot(1, B_BED_RED, 32);
            setSlot(2, B_END_STONE, 32);
            setSlot(3, B_OBSIDIAN, 12);
            setSlot(4, I_ENDER_PEARL, 4);
            setSlot(5, I_GOLDEN_APPLE, 3);
            setSlot(6, I_PICK_DIAMOND, 1);
            this.selectedSlot = 1; // Select Explosive Beds immediately!
            break;
         case MODE_CASTLE_SIEGE:
            setSlot(0, I_SIEGE_CANNON, 1);
            setSlot(1, I_MORTAR, 1);
            setSlot(2, I_SWORD_DIAMOND, 1);
            setSlot(3, I_TNT_GRENADE, 4);
            setSlot(4, I_GOLDEN_APPLE, 3);
            setSlot(5, B_STONEBRICK, 24);
            setSlot(6, I_ARROW, 48);
            break;
      }
   }

   private void setSlot(int slot, int id, int count) {
      this.hotbarId[slot] = id;
      this.hotbarCount[slot] = count;
   }

   private void generateOverworldBiomes() {
      for (int wz = -HALF_W; wz < HALF_W; wz++) {
         for (int wx = -HALF_W; wx < HALF_W; wx++) {
            double n1 = Math.sin(wx * 0.045 + 0.7) * Math.cos(wz * 0.045 + 1.1);
            double n2 = Math.sin((wx + wz) * 0.09) * 0.5 + Math.cos((wx - wz) * 0.11) * 0.5;
            double mountain = Math.max(0.0, Math.sin(wx * 0.025 - 1.2) * Math.sin(wz * 0.025 + 0.8));
            int h = (int)Math.round(62.0 + n1 * 6.5 + n2 * 3.0 + mountain * 16.0);
            boolean isDesert = (wx > 18 && wz < -10);
            boolean isSnow = (wx < -20 && wz < -12);

            setFast(wx, 0, wz, B_BEDROCK);
            for (int y = 1; y <= h; y++) {
               byte b;
               if (y == h) {
                  if (h <= 60) b = B_SAND;
                  else if (isDesert) b = B_SAND;
                  else if (isSnow || h >= 76) b = B_SNOW;
                  else b = B_GRASS;
               } else if (y >= h - 3) {
                  b = isDesert ? B_SANDSTONE : B_DIRT;
               } else {
                  int hash = (wx * 734287 + y * 912931 + wz * 439829) & 255;
                  if (y <= 16 && hash == 0) b = B_DIAMOND_ORE;
                  else if (y <= 28 && hash <= 2) b = B_GOLD_ORE;
                  else if (y <= 48 && hash <= 6) b = B_IRON_ORE;
                  else if (hash <= 11) b = B_COAL_ORE;
                  else b = B_STONE;
               }
               setFast(wx, y, wz, b);
            }
            for (int y = h + 1; y <= 60; y++) {
               setFast(wx, y, wz, isSnow && y == 60 ? B_ICE : B_WATER);
            }
         }
      }
      // Trees & Village Cabin near spawn
      for (int wz = -65; wz <= 65; wz += 9) {
         for (int wx = -65; wx <= 65; wx += 9) {
            if (Math.hypot(wx, wz) < 12.0) continue;
            int tx = wx + ((wz * 3) & 3);
            int tz = wz + ((wx * 5) & 3);
            int ty = topSolidY(tx, tz);
            if (ty > 61 && ty < 75 && getBlock(tx, ty, tz) == B_GRASS) {
               placeTree(tx, ty + 1, tz, (tx + tz) % 3 == 0 ? B_LOG_SPRUCE : B_LOG_OAK);
            }
         }
      }
      buildVillageHouse(14, topSolidY(14, 14), 14);
      int spawnY = topSolidY(0, 0) + 1;
      this.px = 0.5;
      this.py = spawnY + 0.1;
      this.pz = 0.5;

      // Spawn Overworld Mobs
      String[] mobNames = new String[]{"Zombie", "Skeleton", "Creeper", "Ranger", "Scout", "Knight"};
      for (int i = 0; i < 6; i++) {
         double ang = i * (Math.PI / 3.0);
         int mx = (int)Math.round(Math.cos(ang) * 20.0);
         int mz = (int)Math.round(Math.sin(ang) * 20.0);
         int my = topSolidY(mx, mz) + 1;
         spawnMob(1 + (i % 3), -1, i % 4, mobNames[i], mx + 0.5, my, mz + 0.5, I_SWORD_IRON);
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
            setFast(ox + dx, oy, oz + dz, B_COBBLE);
            boolean wall = Math.abs(dx) == 3 || Math.abs(dz) == 3;
            boolean door = dz == -3 && dx == 0;
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
      setFast(ox - 2, oy + 1, oz + 2, B_CRAFTING_TABLE);
      setFast(ox + 2, oy + 1, oz + 2, B_CHEST);
      setFast(ox, oy + 3, oz, B_TORCH);
   }

   private static byte[] loadConsoleArenas(Context context) {
      if (cachedConsoleArenas != null) {
         return cachedConsoleArenas;
      }
      int totalBytes = 8 * 160 * 160 * 96;
      byte[] buf = new byte[totalBytes];
      try {
         InputStream raw = null;
         if (context != null) {
            try {
               raw = context.getAssets().open("maps/console_arenas.bin");
            } catch (Exception ignored) {
            }
         }
         if (raw == null) {
            raw = PeWorld.class.getResourceAsStream("/maps/console_arenas.bin");
         }
         if (raw != null) {
            try (GZIPInputStream gz = new GZIPInputStream(new BufferedInputStream(raw))) {
               int off = 0;
               while (off < totalBytes) {
                  int r = gz.read(buf, off, totalBytes - off);
                  if (r <= 0) break;
                  off += r;
               }
            }
            cachedConsoleArenas = buf;
            return buf;
         }
      } catch (Exception ignored) {
      }
      return null;
   }

   private void generateBattleArena(Context context) {
      byte[] data = loadConsoleArenas(context);
      int m = Math.floorMod(this.mapId, 8);
      int yOff = (m == 2) ? 3 : ((m == 4) ? 5 : 2);
      if (data != null) {
         int mapBase = m * (160 * 160 * 96);
         for (int wz = -80; wz < 80; wz++) {
            for (int wx = -80; wx < 80; wx++) {
               int colBase = mapBase + ((wz + 80) * 160 + (wx + 80)) * 96;
               for (int dy = -24; dy <= 55; dy++) {
                  int bidx = data[colBase + (dy + 24)] & 0x3F;
                  if (bidx == 0) continue;
                  int wy = Y_DECK + yOff + dy;
                  if (wy >= 1 && wy < WORLD_H) {
                     setFast(wx, wy, wz, CONSOLE_PALETTE_MAP[bidx]);
                  }
               }
            }
         }
      } else {
         // Procedural fallback if asset stream not present
         for (int wz = -48; wz <= 48; wz++) {
            for (int wx = -48; wx <= 48; wx++) {
               setFast(wx, Y_DECK - 1, wz, B_STONE);
               setFast(wx, Y_DECK, wz, ((wx + wz) & 3) == 0 ? B_STONEBRICK : B_GRASS);
            }
         }
      }
      // Central Cornucopia Pedestal & 4 Center Chests
      for (int dz = -6; dz <= 6; dz++) {
         for (int dx = -6; dx <= 6; dx++) {
            if (Math.hypot(dx, dz) <= 6.2) {
               if (getBlock(dx, Y_DECK, dz) == B_AIR) {
                  setFast(dx, Y_DECK, dz, B_STONEBRICK);
               }
               setFast(dx, Y_DECK + 1, dz, B_AIR);
               setFast(dx, Y_DECK + 2, dz, B_AIR);
            }
         }
      }
      setFast(0, Y_DECK, 0, B_DIAMOND_BLOCK);
      setFast(-2, Y_DECK + 1, 0, B_CHEST);
      setFast(2, Y_DECK + 1, 0, B_CHEST);
      setFast(0, Y_DECK + 1, -2, B_CHEST);
      setFast(0, Y_DECK + 1, 2, B_CHEST);

      this.px = 5.5;
      this.py = Y_DECK + 1.1;
      this.pz = 0.5;
      this.yaw = 90.0F;

      String[] names = new String[]{"Vortex", "Blaze", "Shadow", "Nova", "Striker", "Titan", "valkyrie"};
      int[][] spawns = new int[][]{{4, 4}, {0, 5}, {-4, 4}, {-5, 0}, {-4, -4}, {0, -5}, {4, -4}};
      for (int i = 0; i < spawns.length; i++) {
         int sx = spawns[i][0];
         int sz = spawns[i][1];
         setFast(sx, Y_DECK, sz, B_GOLD_BLOCK);
         PeMob bot = spawnMob(0, -1, i % 4, names[i], sx + 0.5, Y_DECK + 1.1, sz + 0.5, (i % 3 == 0) ? I_BOW : I_SWORD_IRON);
         bot.roamIdx = (i + 1) & 7;
      }
   }

   private void generateTumbleArena() {
      for (int wz = -22; wz <= 22; wz++) {
         for (int wx = -22; wx <= 22; wx++) {
            double d = Math.hypot(wx, wz);
            if (d <= 22.0) {
               setFast(wx, Y_DECK - 15, wz, B_BEDROCK);
               if (d <= 20.0) {
                  setFast(wx, Y_DECK - 14, wz, B_LAVA);
               } else {
                  for (int y = Y_DECK - 14; y <= Y_DECK + 5; y++) {
                     setFast(wx, y, wz, B_STONEBRICK);
                  }
               }
               if (d <= 18.0) {
                  setFast(wx, Y_DECK, wz, ((wx + wz) & 1) == 0 ? B_SNOW : B_PACKED_ICE);
                  setFast(wx, Y_DECK - 5, wz, ((wx + wz) & 1) == 0 ? B_SANDSTONE : B_TERRACOTTA);
                  setFast(wx, Y_DECK - 10, wz, ((wx + wz) & 1) == 0 ? B_QUARTZ_BLOCK : B_PRISMARINE);
               }
            }
         }
      }
      this.px = 0.5;
      this.py = Y_DECK + 1.1;
      this.pz = -12.5;
      this.yaw = 0.0F;
      String[] names = new String[]{"Frost", "Glacier", "Avalanche", "Blizzard", "Icicle"};
      for (int i = 0; i < 5; i++) {
         double ang = i * (2.0 * Math.PI / 5.0);
         spawnMob(0, -1, i % 4, names[i], Math.cos(ang) * 11.0, Y_DECK + 1.1, Math.sin(ang) * 11.0, I_SHOVEL_DIAMOND);
      }
   }

   private void generateBedwarsArena() {
      // Center Emerald Island
      for (int wz = -9; wz <= 9; wz++) {
         for (int wx = -9; wx <= 9; wx++) {
            if (Math.hypot(wx, wz) <= 9.2) {
               setFast(wx, Y_DECK - 1, wz, B_STONE);
               setFast(wx, Y_DECK, wz, (wx == 0 && wz == 0) ? B_EMERALD_BLOCK : B_STONEBRICK);
            }
         }
      }
      int[][] bases = new int[][]{{0, -34}, {0, 34}, {-34, 0}, {34, 0}};
      byte[] wools = new byte[]{B_WOOL_RED, B_WOOL_BLUE, B_WOOL_GREEN, B_WOOL_YELLOW};
      String[] tNames = new String[]{"Red", "Blue", "Green", "Yellow"};
      for (int t = 0; t < 4; t++) {
         int bx = bases[t][0];
         int bz = bases[t][1];
         for (int dz = -5; dz <= 5; dz++) {
            for (int dx = -5; dx <= 5; dx++) {
               setFast(bx + dx, Y_DECK - 1, bz + dz, B_STONEBRICK);
               setFast(bx + dx, Y_DECK, bz + dz, (Math.abs(dx) == 5 || Math.abs(dz) == 5) ? wools[t] : B_PLANKS_OAK);
            }
         }
         // Bridges toward center
         int dirX = Integer.compare(0, bx);
         int dirZ = Integer.compare(0, bz);
         for (int step = 6; step <= 25; step++) {
            for (int w = -1; w <= 1; w++) {
               int rx = bx + dirX * step + (dirX == 0 ? w : 0);
               int rz = bz + dirZ * step + (dirZ == 0 ? w : 0);
               setFast(rx, Y_DECK, rz, w == 0 ? B_PLANKS_OAK : wools[t]);
            }
         }
         setFast(bx + dirX * 3, Y_DECK + 1, bz + dirZ * 3, B_BED_RED);
         setFast(bx - dirX * 3, Y_DECK, bz - dirZ * 3, B_GOLD_BLOCK);
         if (t > 0) {
            spawnMob(0, t, t % 4, tNames[t] + "Knight", bx + 0.5, Y_DECK + 1.1, bz + 0.5, I_SWORD_IRON);
            spawnMob(0, t, (t + 1) % 4, tNames[t] + "Raider", bx + 1.5, Y_DECK + 1.1, bz + 0.5, I_SWORD_DIAMOND);
         }
      }
      this.px = 0.5;
      this.py = Y_DECK + 1.1;
      this.pz = -34.5;
      this.yaw = 0.0F;
   }

   private void generateCs2Arena() {
      int m = Math.floorMod(this.mapId, 8);
      byte floorA;
      switch (m) {
         case 0:
         case 7:
            floorA = B_SANDSTONE;
            break;
         case 1:
            floorA = B_TERRACOTTA;
            break;
         case 2:
            floorA = B_COBBLE;
            break;
         case 3:
            floorA = B_CONCRETE_GRAY;
            break;
         case 6:
            floorA = B_QUARTZ_BLOCK;
            break;
         default:
            floorA = B_STONEBRICK;
            break;
      }
      byte wallA;
      switch (m) {
         case 0:
         case 7:
            wallA = B_SANDSTONE;
            break;
         case 1:
            wallA = B_RED_SANDSTONE;
            break;
         case 2:
         case 5:
            wallA = B_BRICKS;
            break;
         case 3:
         case 6:
            wallA = B_IRON_BLOCK;
            break;
         default:
            wallA = B_STONEBRICK;
            break;
      }
      for (int wz = -32; wz <= 32; wz++) {
         for (int wx = -34; wx <= 34; wx++) {
            int ax = Math.abs(wx);
            int az = Math.abs(wz);
            setFast(wx, Y_DECK - 1, wz, B_BEDROCK);
            setFast(wx, Y_DECK, wz, ((wx + wz) & 3) == 0 ? B_STONE : floorA);
            if (ax == 34 || az == 32) {
               for (int y = Y_DECK + 1; y <= Y_DECK + 5; y++) {
                  setFast(wx, y, wz, wallA);
               }
               continue;
            }
            // 3-Lane Divider Buildings with Mid & Side Connectors
            if (ax >= 8 && ax <= 13 && ((wz >= -11 && wz <= -4) || (wz >= 4 && wz <= 11))) {
               for (int y = Y_DECK + 1; y <= Y_DECK + 4; y++) {
                  setFast(wx, y, wz, wallA);
               }
            }
            // Tactical Cover Crates
            if ((ax == 4 && az == 6) || (ax == 20 && az == 8) || (ax == 26 && az == 14)) {
               setFast(wx, Y_DECK + 1, wz, B_PLANKS_OAK);
               setFast(wx, Y_DECK + 2, wz, B_PLANKS_OAK);
            }
         }
      }
      // Bomb Site A (-22, -12) & Bomb Site B (22, -12)
      int[][] sites = new int[][]{{-22, -12}, {22, -12}};
      for (int[] s : sites) {
         for (int dz = -2; dz <= 2; dz++) {
            for (int dx = -2; dx <= 2; dx++) {
               setFast(s[0] + dx, Y_DECK, s[1] + dz, (dx == 0 && dz == 0) ? B_GOLD_BLOCK : B_REDSTONE_BLOCK);
            }
         }
      }
      this.px = 0.5;
      this.py = Y_DECK + 1.1;
      this.pz = -24.5;
      this.yaw = 0.0F;
      for (int i = 0; i < 4; i++) {
         spawnMob(0, 1, i, "Terrorist-" + (i + 1), -12.0 + i * 8.0, Y_DECK + 1.1, 24.5, (i == 1) ? I_SNIPER : I_MACHINE_GUN);
      }
      for (int i = 0; i < 2; i++) {
         spawnMob(0, 0, i, "CT-Ally-" + (i + 1), -6.0 + i * 12.0, Y_DECK + 1.1, -24.5, I_MACHINE_GUN);
      }
   }

   private void generateEndFightArena() {
      for (int wz = -28; wz <= 28; wz++) {
         for (int wx = -28; wx <= 28; wx++) {
            double d = Math.hypot(wx, wz);
            if (d <= 20.0 || (Math.abs(wx) <= 2 && Math.abs(wz) <= 26) || (Math.abs(wz) <= 2 && Math.abs(wx) <= 26)) {
               setFast(wx, Y_DECK - 1, wz, B_END_STONE);
               setFast(wx, Y_DECK, wz, ((wx + wz) & 3) == 0 ? B_PURPUR : B_END_STONE);
            }
         }
      }
      for (int p = 0; p < 6; p++) {
         double ang = p * (Math.PI / 3.0);
         int ox = (int)Math.round(Math.cos(ang) * 13.0);
         int oz = (int)Math.round(Math.sin(ang) * 13.0);
         for (int y = Y_DECK + 1; y <= Y_DECK + 6; y++) {
            setFast(ox, y, oz, y == Y_DECK + 6 ? B_SEA_LANTERN : B_OBSIDIAN);
         }
      }
      this.px = 0.5;
      this.py = Y_DECK + 1.1;
      this.pz = -12.5;
      this.yaw = 0.0F;
      String[] names = new String[]{"EnderLord", "VoidStalker", "BedBomber", "DragonKnight", "NullStriker"};
      for (int i = 0; i < 5; i++) {
         double ang = i * (2.0 * Math.PI / 5.0);
         spawnMob(0, -1, i % 4, names[i], Math.cos(ang) * 10.0, Y_DECK + 1.1, Math.sin(ang) * 10.0, B_BED_RED);
      }
   }

   private void generateCastleSiegeArena() {
      int m = Math.floorMod(this.mapId, 8);
      byte fortWall;
      switch (m) {
         case 1:
            fortWall = B_COBBLE;
            break;
         case 2:
            fortWall = B_OBSIDIAN;
            break;
         case 3:
            fortWall = B_PACKED_ICE;
            break;
         case 4:
            fortWall = B_SANDSTONE;
            break;
         case 5:
            fortWall = B_NETHER_BRICKS;
            break;
         case 6:
            fortWall = B_QUARTZ_BLOCK;
            break;
         default:
            fortWall = B_STONEBRICK;
            break;
      }
      byte moat = (m == 2 || m == 5) ? B_LAVA : ((m == 3) ? B_ICE : B_WATER);
      for (int wz = -30; wz <= 30; wz++) {
         for (int wx = -30; wx <= 30; wx++) {
            int ax = Math.abs(wx);
            setFast(wx, Y_DECK - 1, wz, B_BEDROCK);
            boolean inMoat = wz >= 9 && wz <= 11 && ax > 3 && !(ax >= 12 && ax <= 16);
            setFast(wx, Y_DECK, wz, inMoat ? moat : (wz <= 6 ? B_STONEBRICK : B_GRASS));

            // Fortress Curtain Wall & 3 Destructible Gates at z = 6
            if (wz == 6 && ax <= 24) {
               boolean gate = ax <= 3 || (ax >= 12 && ax <= 16);
               for (int y = Y_DECK + 1; y <= Y_DECK + 5; y++) {
                  setFast(wx, y, wz, gate ? B_CRACKED_STONEBRICK : fortWall);
               }
            }
            if ((wz == -28 || ax == 24) && wz <= 6) {
               for (int y = Y_DECK + 1; y <= Y_DECK + 6; y++) {
                  setFast(wx, y, wz, fortWall);
               }
            }
         }
      }
      // Control Zone A (-10, -8) & Control Zone B (10, -18)
      int[][] zones = new int[][]{{-10, -8}, {10, -18}};
      for (int z = 0; z < 2; z++) {
         for (int dz = -2; dz <= 2; dz++) {
            for (int dx = -2; dx <= 2; dx++) {
               setFast(zones[z][0] + dx, Y_DECK, zones[z][1] + dz, (dx == 0 && dz == 0) ? B_GOLD_BLOCK : (z == 0 ? B_WOOL_BLUE : B_WOOL_YELLOW));
            }
         }
      }
      this.px = 0.5;
      this.py = Y_DECK + 1.1;
      this.pz = 22.5;
      this.yaw = 180.0F;
      for (int i = 0; i < 4; i++) {
         spawnMob(0, 0, i, "Defender-" + (i + 1), -12.0 + i * 8.0, Y_DECK + 1.1, -10.5, I_SWORD_DIAMOND);
      }
      for (int i = 0; i < 2; i++) {
         spawnMob(0, 1, i, "SiegeAlly-" + (i + 1), -8.0 + i * 16.0, Y_DECK + 1.1, 20.5, I_SIEGE_CANNON);
      }
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
         if (isSolid(getBlock(wx, y, wz))) {
            return y;
         }
      }
      return Y_DECK;
   }

   public PeMob spawnMob(int type, int team, int arch, String name, double x, double y, double z, int held) {
      PeMob m = new PeMob();
      m.id = this.mobs.size() + 1;
      m.type = type;
      m.team = team;
      m.archetype = arch;
      m.roamIdx = m.id & 7;
      m.name = name;
      m.x = x;
      m.y = y;
      m.z = z;
      m.heldItem = held;
      this.mobs.add(m);
      return m;
   }

   // Raycast from camera eye along look direction up to maxReach
   public int[] raycastBlock(double maxReach) {
      double ex = this.px;
      double ey = this.py + (this.sneaking ? 1.50 : 1.62);
      double ez = this.pz;
      double yawRad = Math.toRadians(this.yaw);
      double pitchRad = Math.toRadians(this.pitch);
      double cosP = Math.cos(pitchRad);
      double dx = -Math.sin(yawRad) * cosP;
      double dy = -Math.sin(pitchRad);
      double dz = Math.cos(yawRad) * cosP;

      int prevX = (int)Math.floor(ex);
      int prevY = (int)Math.floor(ey);
      int prevZ = (int)Math.floor(ez);
      for (double t = 0.2; t <= maxReach; t += 0.08) {
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
      double dx = -Math.sin(yawRad) * cosP;
      double dy = -Math.sin(pitchRad);
      double dz = Math.cos(yawRad) * cosP;

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
            if (perpSq <= 0.75) {
               bestD = proj;
               best = m;
            }
         }
      }
      return best;
   }

   public void triggerPrimaryAction(PeAudio audio) {
      this.swingAnim = 0.0F;
      int held = this.hotbarId[this.selectedSlot];

      // 1. Ranged / Tactical Weapons & Consumables
      if (held == I_PISTOL || held == I_MACHINE_GUN || held == I_SNIPER || held == I_BOW) {
         firePlayerProjectile(0, held == I_SNIPER ? 2.8 : 2.1, held == I_SNIPER ? 14 : (held == I_PISTOL ? 6 : 5), 60);
         if (audio != null) audio.playSound("bow_shoot", 0.85F, held == I_MACHINE_GUN ? 1.45F : 1.15F);
         return;
      }
      if (held == I_SIEGE_CANNON || held == I_MORTAR) {
         firePlayerProjectile(held == I_MORTAR ? 4 : 3, held == I_MORTAR ? 1.25 : 1.65, 16, 40);
         if (audio != null) audio.playSound("tnt_explode", 0.9F, 1.15F);
         return;
      }
      if (held == I_TNT_GRENADE) {
         firePlayerProjectile(1, 0.95, 12, 45);
         consumeSelectedSlot(1);
         if (audio != null) audio.playSound("tnt_ignite", 0.85F, 1.1F);
         return;
      }
      if (held == I_SNOWBALL || held == I_ENDER_PEARL) {
         firePlayerProjectile(2, 1.55, 2, 50);
         if (held == I_ENDER_PEARL) consumeSelectedSlot(1);
         if (audio != null) audio.playSound("bow_shoot", 0.7F, 1.4F);
         return;
      }
      if (held == I_GOLDEN_APPLE || held == I_STEAK) {
         if (this.health < 20 || this.hunger < 20) {
            this.health = Math.min(20, this.health + (held == I_GOLDEN_APPLE ? 8 : 5));
            this.hunger = Math.min(20, this.hunger + 6);
            consumeSelectedSlot(1);
            if (audio != null) audio.playSound("eat_bite", 0.8F, 1.0F);
         }
         return;
      }
      if (held == I_C4_BOMB && this.mode == MODE_CS2 && !this.cs2NoBombs && !this.cs2BombPlanted) {
         if (Math.hypot(this.px - (-22), this.pz - (-12)) <= 5.0 || Math.hypot(this.px - 22, this.pz - (-12)) <= 5.0) {
            this.cs2BombPlanted = true;
            this.cs2BombTimer = 700;
            showBanner("C4 BOMB PLANTED! (35s to Detonation!)");
            if (audio != null) audio.playSound("tnt_ignite", 1.0F, 0.95F);
            return;
         }
      }

      // 2. Melee Attack on Targeted Mob
      PeMob hitMob = raycastMob(3.6);
      if (hitMob != null) {
         boolean isFriendly = (this.mode == MODE_CS2 && hitMob.team == 0)
            || (this.mode == MODE_CASTLE_SIEGE && hitMob.team == 1)
            || (this.mode == MODE_BEDWARS && hitMob.team == 0);
         if (!isFriendly) {
            boolean isCrit = !this.onGround && this.vy < -0.05;
            int dmg = (held == I_SWORD_DIAMOND ? 8 : (held == I_SWORD_IRON || held == I_AXE_IRON ? 6 : 3)) + (isCrit ? 2 : 0);
            damageMob(hitMob, dmg, this.px, this.pz, true, audio);
            return;
         }
      }

      // 3. Place Block / Detonate End-Fight Bed / Break Tumble Layer Block
      int[] hit = raycastBlock(5.0);
      if (hit != null) {
         int bx = hit[0], by = hit[1], bz = hit[2];
         int pxPlace = hit[3], pyPlace = hit[4], pzPlace = hit[5];
         byte targetBlock = (byte)hit[6];

         // Tumble Shovel instant layer break!
         if (this.mode == MODE_TUMBLE && held == I_SHOVEL_DIAMOND && targetBlock != B_BEDROCK && targetBlock != B_STONEBRICK) {
            setBlock(bx, by, bz, B_AIR);
            if (audio != null) audio.playSound("dig_stone", 0.8F, 1.15F);
            return;
         }
         // End Fight Bed-Bombing: placing or tapping a Bed immediately detonates a zero-self-damage blast!
         if (this.mode == MODE_END_FIGHT && (held == B_BED_RED || targetBlock == B_BED_RED)) {
            if (targetBlock != B_BED_RED) {
               consumeSelectedSlot(1);
            } else {
               setBlock(bx, by, bz, B_AIR);
            }
            detonateExplosion(pxPlace + 0.5, pyPlace + 0.5, pzPlace + 0.5, 4.5, -2, true, audio);
            return;
         }
         // Place block if holding a block (1..63) and not overlapping player AABB
         if (held > 0 && held < 64) {
            boolean overlapsPlayer = Math.abs((pxPlace + 0.5) - this.px) < 0.75
               && Math.abs((pzPlace + 0.5) - this.pz) < 0.75
               && pyPlace >= Math.floor(this.py) && pyPlace <= Math.floor(this.py + 1.7);
            if (!overlapsPlayer && getBlock(pxPlace, pyPlace, pzPlace) == B_AIR) {
               setBlock(pxPlace, pyPlace, pzPlace, (byte)held);
               if (this.mode != MODE_CREATIVE) {
                  consumeSelectedSlot(1);
               }
               if (audio != null) audio.playSound("click_block", 0.75F, 1.05F);
            }
         }
      } else if (audio != null) {
         audio.playSound("swing_1", 0.6F, 1.0F);
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
      double dx = -Math.sin(yawRad) * cosP;
      double dy = -Math.sin(pitchRad);
      double dz = Math.cos(yawRad) * cosP;
      PeProjectile p = new PeProjectile();
      p.type = type;
      p.sourceTeam = -2; // Player
      p.x = this.px + dx * 0.6;
      p.y = this.py + 1.52 + dy * 0.4;
      p.z = this.pz + dz * 0.6;
      p.vx = dx * speed;
      p.vy = dy * speed + (type == 1 || type == 4 ? 0.22 : 0.04);
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
            showBanner("Eliminated " + m.name + "! (Kills: " + this.kills + ")");
         }
      }
   }

   public void detonateExplosion(double ex, double ey, double ez, double radius, int sourceTeam, boolean endBedBlast, PeAudio audio) {
      if (audio != null) audio.playSound("tnt_explode", 1.0F, 0.95F);
      int r = (int)Math.ceil(radius);
      int cx = (int)Math.floor(ex);
      int cy = (int)Math.floor(ey);
      int cz = (int)Math.floor(ez);
      for (int dz = -r; dz <= r; dz++) {
         for (int dy = -r; dy <= r; dy++) {
            for (int dx = -r; dx <= r; dx++) {
               if (dx * dx + dy * dy + dz * dz <= radius * radius) {
                  int bx = cx + dx, by = cy + dy, bz = cz + dz;
                  byte b = getBlock(bx, by, bz);
                  if (b == B_AIR || b == B_BEDROCK || b == B_OBSIDIAN) continue;
                  if (this.mode == MODE_CASTLE_SIEGE && by <= Y_DECK) continue;
                  if (this.mode == MODE_BATTLE || this.mode == MODE_CS2) continue;
                  setBlock(bx, by, bz, B_AIR);
               }
            }
         }
      }
      // Entity blast damage & knockback (Zero self-damage in End Fight or Castle Siege friendly fire!)
      for (PeMob m : this.mobs) {
         if (m == null || m.dead) continue;
         if (this.mode == MODE_CASTLE_SIEGE && sourceTeam == -2 && m.team == 1) continue;
         if (this.mode == MODE_CASTLE_SIEGE && sourceTeam >= 0 && m.team == sourceTeam) continue;
         double d = Math.sqrt((m.x - ex) * (m.x - ex) + (m.y - ey) * (m.y - ey) + (m.z - ez) * (m.z - ez));
         if (d <= radius + 1.5) {
            double f = 1.0 - (d / (radius + 1.5));
            int dmg = Math.max(4, (int)Math.round(16.0 * f));
            damageMob(m, dmg, ex, ez, sourceTeam == -2, null);
            if (endBedBlast) {
               double hd = Math.max(0.1, Math.hypot(m.x - ex, m.z - ez));
               m.vx = ((m.x - ex) / hd) * 1.45;
               m.vz = ((m.z - ez) / hd) * 1.45;
               m.vy = 0.72;
            }
         }
      }
      // Player blast check (Zero self-damage if sourceTeam == -2 in End Fight or Castle Siege!)
      boolean immuneSelf = (sourceTeam == -2) && (endBedBlast || this.mode == MODE_END_FIGHT || this.mode == MODE_CASTLE_SIEGE);
      if (!immuneSelf && this.mode != MODE_CREATIVE) {
         double d = Math.sqrt((this.px - ex) * (this.px - ex) + (this.py - ey) * (this.py - ey) + (this.pz - ez) * (this.pz - ez));
         if (d <= radius + 1.2 && this.invulnTicks <= 0) {
            int dmg = Math.max(2, (int)Math.round(12.0 * (1.0 - d / (radius + 1.2))));
            this.health = Math.max(1, this.health - dmg);
            this.hurtTime = 10;
            this.invulnTicks = 15;
         }
      }
   }

   public void tick(PeTouchController touch, PeAudio audio) {
      this.tickCount++;
      if (this.hurtTime > 0) this.hurtTime--;
      if (this.invulnTicks > 0) this.invulnTicks--;
      if (this.hitmarkerTicks > 0) this.hitmarkerTicks--;
      if (this.bannerTicks > 0) this.bannerTicks--;
      if (this.battleGraceTicks > 0) this.battleGraceTicks--;
      if (this.swingAnim >= 0.0F) {
         this.swingAnim += 0.16F;
         if (this.swingAnim >= 1.0F) this.swingAnim = -1.0F;
      }
      if (this.mode == MODE_SURVIVAL || this.mode == MODE_CREATIVE) {
         this.dayTime = (this.dayTime + 0.00012F) % 1.0F;
      }

      // 1. Player Movement & Physics AABB Collision
      double moveFwd = touch != null ? touch.moveForward : 0.0;
      double moveStrafe = touch != null ? touch.moveStrafe : 0.0;
      boolean wantJump = touch != null && touch.jumpHeld;

      double speed = this.flying ? 0.34 : (this.sneaking ? 0.10 : (this.sprinting ? 0.25 : 0.19));
      double yawRad = Math.toRadians(this.yaw);
      double sinY = Math.sin(yawRad);
      double cosY = Math.cos(yawRad);
      double wishX = (-sinY * moveFwd + cosY * moveStrafe) * speed;
      double wishZ = (cosY * moveFwd + sinY * moveStrafe) * speed;

      this.vx = this.vx * 0.55 + wishX * 0.45;
      this.vz = this.vz * 0.55 + wishZ * 0.45;

      if (this.flying) {
         this.vy = wantJump ? 0.24 : (this.sneaking ? -0.24 : 0.0);
      } else {
         if (wantJump && this.onGround) {
            this.vy = 0.42;
            this.onGround = false;
         }
         this.vy = Math.max(-0.78, this.vy - 0.08);
      }

      moveEntityWithCollision(true, null);

      // Slime Block Jump Pad bounce!
      byte under = getBlock((int)Math.floor(this.px), (int)Math.floor(this.py - 0.2), (int)Math.floor(this.pz));
      if (under == B_SLIME_BLOCK && this.onGround) {
         this.vy = 0.82;
         this.onGround = false;
         if (audio != null) audio.playSound("slime_bounce", 0.85F, 1.0F);
      }

      // Void / Lava Respawn Protection
      if (this.py < 4.0 || (this.mode == MODE_TUMBLE && this.py <= Y_DECK - 13.0)) {
         this.px = 0.5;
         this.py = topSolidY(0, 0) + 1.5;
         this.pz = 0.5;
         this.vx = this.vy = this.vz = 0.0;
         this.health = 20;
         showBanner("Respawned at Safe Spawn!");
      }

      // 2. Hold-to-Mine Block Progression
      if (touch != null && touch.miningHeld) {
         int[] hit = raycastBlock(5.0);
         if (hit != null && hit[6] != B_BEDROCK) {
            if (hit[0] == this.mineX && hit[1] == this.mineY && hit[2] == this.mineZ) {
               float rate = (this.mode == MODE_CREATIVE || this.mode == MODE_TUMBLE) ? 0.50F : 0.075F;
               this.mineProgress += rate;
               this.swingAnim = (this.tickCount % 6) / 6.0F;
               if (this.mineProgress >= 1.0F) {
                  byte broken = getBlock(this.mineX, this.mineY, this.mineZ);
                  setBlock(this.mineX, this.mineY, this.mineZ, B_AIR);
                  if (this.mode == MODE_SURVIVAL && broken != B_AIR) {
                     givePickedUpBlock(broken);
                  }
                  if (audio != null) audio.playSound("dig_stone", 0.85F, 1.0F);
                  this.mineProgress = 0.0F;
               }
            } else {
               this.mineX = hit[0];
               this.mineY = hit[1];
               this.mineZ = hit[2];
               this.mineProgress = 0.08F;
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
         if (p.type == 1 || p.type == 3 || p.type == 4) {
            p.vy -= 0.045;
         } else {
            p.vy -= 0.012;
         }
         p.fuse--;
         int bx = (int)Math.floor(p.x);
         int by = (int)Math.floor(p.y);
         int bz = (int)Math.floor(p.z);
         byte hitB = getBlock(bx, by, bz);

         // Check Mob Hit
         for (PeMob m : this.mobs) {
            if (m == null || m.dead) continue;
            if (p.sourceTeam == -2 && ((this.mode == MODE_CS2 && m.team == 0) || (this.mode == MODE_CASTLE_SIEGE && m.team == 1))) continue;
            if (Math.hypot(m.x - p.x, m.z - p.z) <= 0.85 && p.y >= m.y && p.y <= m.y + 1.9) {
               if (p.type == 1 || p.type == 3 || p.type == 4) {
                  detonateExplosion(p.x, p.y, p.z, p.type == 1 ? 3.2 : 4.2, p.sourceTeam, false, audio);
               } else {
                  damageMob(m, p.damage, p.x - p.vx, p.z - p.vz, p.sourceTeam == -2, audio);
               }
               p.dead = true;
               break;
            }
         }

         if (!p.dead && (isSolid(hitB) || p.fuse <= 0)) {
            if (p.type == 1 || p.type == 3 || p.type == 4) {
               detonateExplosion(p.x, p.y, p.z, p.type == 1 ? 3.2 : 4.2, p.sourceTeam, false, audio);
            } else if (p.type == 2 && this.mode == MODE_TUMBLE && hitB != B_BEDROCK && hitB != B_STONEBRICK) {
               setBlock(bx, by, bz, B_AIR);
            }
            p.dead = true;
         }
         if (p.dead) {
            this.projectiles.remove(i);
         }
      }

      // 4. Tick Dynamic Mobs & Minigame AI Bots
      tickDynamicMobs(audio);

      // 5. Mode-specific objectives (CS2 Bomb Timer & Castle Siege Capture Zones)
      if (this.mode == MODE_CS2 && this.cs2BombPlanted && this.cs2BombTimer > 0) {
         this.cs2BombTimer--;
         if (this.cs2BombTimer == 0) {
            detonateExplosion(-22, Y_DECK + 1, -12, 6.0, 1, false, audio);
            this.cs2BombPlanted = false;
            showBanner("C4 DETONATED! New Round Starting...");
         }
      }
      if (this.mode == MODE_CASTLE_SIEGE) {
         if (Math.hypot(this.px - (-10), this.pz - (-8)) <= 3.5 && this.siegeZoneA < 100) {
            this.siegeZoneA = Math.min(100, this.siegeZoneA + 1);
            if (this.siegeZoneA == 100) showBanner("ZONE A CAPTURED! Advance to Throne Keep Zone B!");
         }
         if (Math.hypot(this.px - 10, this.pz - (-18)) <= 3.5 && this.siegeZoneB < 100) {
            this.siegeZoneB = Math.min(100, this.siegeZoneB + 1);
            if (this.siegeZoneB == 100) showBanner("VICTORY! Both Castle Siege Control Zones Captured!");
         }
      }
   }

   private void givePickedUpBlock(byte id) {
      for (int i = 0; i < 9; i++) {
         if (this.hotbarId[i] == id && this.hotbarCount[i] < 64) {
            this.hotbarCount[i]++;
            return;
         }
      }
      for (int i = 0; i < 9; i++) {
         if (this.hotbarId[i] == 0 || this.hotbarCount[i] <= 0) {
            this.hotbarId[i] = id;
            this.hotbarCount[i] = 1;
            return;
         }
      }
   }

   private void tickDynamicMobs(PeAudio audio) {
      int mId = Math.floorMod(this.mapId, 8);
      for (PeMob m : this.mobs) {
         if (m == null || m.dead) continue;
         if (m.hurtTime > 0) m.hurtTime--;
         if (m.attackCd > 0) m.attackCd--;

         // Select target (Player or rival bot)
         double targX = this.px;
         double targY = this.py;
         double targZ = this.pz;
         boolean targetIsPlayer = true;
         boolean playerIsAlly = (this.mode == MODE_CS2 && m.team == 0)
            || (this.mode == MODE_CASTLE_SIEGE && m.team == 1)
            || (this.mode == MODE_BEDWARS && m.team == 0);
         double bestD = playerIsAlly ? 999.0 : Math.hypot(this.px - m.x, this.pz - m.z);

         for (PeMob other : this.mobs) {
            if (other == null || other == m || other.dead) continue;
            if (m.team >= 0 && other.team == m.team) continue;
            double d = Math.hypot(other.x - m.x, other.z - m.z);
            if (d < bestD - 2.0) {
               bestD = d;
               targX = other.x;
               targY = other.y;
               targZ = other.z;
               targetIsPlayer = false;
            }
         }

         // In Battle Brawl: during Grace Period OR when no target is within 18m (or near center < 13m), disperse to outer map landmarks!
         double moveX = targX;
         double moveZ = targZ;
         if (this.mode == MODE_BATTLE && (this.battleGraceTicks > 0 || bestD > 18.0 || (Math.hypot(m.x, m.z) < 13.0 && bestD > 4.0 && m.archetype != 0))) {
            int[] wp = BATTLE_ROAM_WAYPOINTS[mId][m.roamIdx & 7];
            moveX = wp[0] + 0.5;
            moveZ = wp[2] + 0.5;
            if (Math.hypot(m.x - moveX, m.z - moveZ) <= 4.0) {
               m.roamIdx = (m.roamIdx + 1) & 7;
            }
         } else if (bestD < 5.0 && bestD > 2.0) {
            // Dynamic PvP Strafe-Weave around opponent!
            double baseAng = Math.atan2(targZ - m.z, targX - m.x);
            double side = (((this.tickCount / 20) + m.id) & 1) == 0 ? 0.55 : -0.55;
            moveX = m.x + Math.cos(baseAng + side) * 3.0;
            moveZ = m.z + Math.sin(baseAng + side) * 3.0;
         }

         double dx = moveX - m.x;
         double dz = moveZ - m.z;
         double dist = Math.hypot(dx, dz);
         m.yaw = (float)Math.toDegrees(Math.atan2(-dx, dz));
         if (dist > 0.6) {
            double spd = (this.mode == MODE_SURVIVAL && m.type > 0) ? 0.11 : 0.165;
            m.vx = (dx / dist) * spd;
            m.vz = (dz / dist) * spd;
            m.walkPhase += 0.32F;
         } else {
            m.vx *= 0.5;
            m.vz *= 0.5;
         }
         m.vy = Math.max(-0.75, m.vy - 0.08);
         moveEntityWithCollision(false, m);

         // Dynamic Combat Attack (Melee Swing / Bow / Gun / End Bed Blast)
         if (this.battleGraceTicks <= 0 && bestD <= 2.65 && Math.abs(targY - m.y) <= 2.6 && m.attackCd <= 0) {
            m.attackCd = 15;
            if (this.mode == MODE_END_FIGHT) {
               detonateExplosion((m.x + targX) * 0.5, m.y + 0.5, (m.z + targZ) * 0.5, 4.0, m.id, true, audio);
            } else if (targetIsPlayer && this.invulnTicks <= 0 && this.mode != MODE_CREATIVE) {
               this.health = Math.max(1, this.health - 3);
               this.hurtTime = 8;
               this.invulnTicks = 12;
               if (audio != null) audio.playSound("player_hurt", 0.8F, 1.0F);
            } else if (!targetIsPlayer) {
               for (PeMob o : this.mobs) {
                  if (o != null && o != m && !o.dead && Math.hypot(o.x - targX, o.z - targZ) < 1.0) {
                     damageMob(o, 4, m.x, m.z, false, null);
                     break;
                  }
               }
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

      // X & Z collision + Auto Step-Jump (Pocket Edition auto-jump when walking into a 1-block step!)
      double nx = x + vx;
      double nz = z + vz;
      int nbx = (int)Math.floor(nx);
      int nbz = (int)Math.floor(nz);
      int footY = (int)Math.floor(ny + 0.1);
      if (isSolid(getBlock(nbx, footY, nbz)) || isSolid(getBlock(nbx, footY + 1, nbz))) {
         // Auto-jump if 1-block step has clear headroom at footY+1 and footY+2!
         if (grounded
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
