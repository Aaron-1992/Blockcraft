package com.blockcraft.pe;

/**
 * Pocket Edition's 1.12-style World of Color block catalogue.
 * Color order is shared by every family: white, orange, magenta, light blue,
 * yellow, lime, pink, gray, light gray, cyan, purple, blue, brown, green, red, black.
 */
public final class PeColors {
   private PeColors() {
   }

   public static final String[] NAMES = {
      "White", "Orange", "Magenta", "Light Blue", "Yellow", "Lime", "Pink", "Gray",
      "Light Gray", "Cyan", "Purple", "Blue", "Brown", "Green", "Red", "Black"
   };

   // Desktop Color312 / Tex palette, kept as separate material palettes instead of flat tints.
   public static final int[] WOOL_RGB = {
      0xE9ECEC, 0xF07613, 0xBD44B3, 0x3AAFD9,
      0xF8C527, 0x70B919, 0xED8DAC, 0x3E4447,
      0x8E8E86, 0x158991, 0x792AAC, 0x35399D,
      0x724728, 0x546D1B, 0xA02722, 0x141519
   };

   public static final int[] CONCRETE_RGB = {
      0xCFD5D6, 0xE06100, 0xA9309F, 0x2489C7,
      0xF1AF15, 0x5EA818, 0xD5658E, 0x36393D,
      0x7D7D73, 0x157788, 0x64209C, 0x2D2F8F,
      0x603C20, 0x495B24, 0x8E2121, 0x080A0F
   };

   public static final int[] TERRACOTTA_RGB = {
      0xD1B2A1, 0xA15325, 0x95576C, 0x716C89,
      0xBA8524, 0x677535, 0xA14E4E, 0x392A23,
      0x876B62, 0x575B5B, 0x764656, 0x4A3B5B,
      0x4D3324, 0x4C532A, 0x8F3D2E, 0x251610
   };

   public static final int[] WOOL_BLOCKS = {
      (PeWorld.B_WOOL_WHITE & 0xFF), (PeWorld.B_WOOL_ORANGE & 0xFF), (PeWorld.B_WOOL_MAGENTA & 0xFF), (PeWorld.B_WOOL_LIGHT_BLUE & 0xFF),
      (PeWorld.B_WOOL_YELLOW & 0xFF), (PeWorld.B_WOOL_LIME & 0xFF), (PeWorld.B_WOOL_PINK & 0xFF), (PeWorld.B_WOOL_GRAY & 0xFF),
      (PeWorld.B_WOOL_LIGHT_GRAY & 0xFF), (PeWorld.B_WOOL_CYAN & 0xFF), (PeWorld.B_WOOL_PURPLE & 0xFF), (PeWorld.B_WOOL_BLUE & 0xFF),
      (PeWorld.B_WOOL_BROWN & 0xFF), (PeWorld.B_WOOL_GREEN & 0xFF), (PeWorld.B_WOOL_RED & 0xFF), (PeWorld.B_WOOL_BLACK & 0xFF)
   };

   public static final int[] CONCRETE_BLOCKS = {
      (PeWorld.B_CONCRETE_WHITE & 0xFF), (PeWorld.B_CONCRETE_ORANGE & 0xFF), (PeWorld.B_CONCRETE_MAGENTA & 0xFF), (PeWorld.B_CONCRETE_LIGHT_BLUE & 0xFF),
      (PeWorld.B_CONCRETE_YELLOW & 0xFF), (PeWorld.B_CONCRETE_LIME & 0xFF), (PeWorld.B_CONCRETE_PINK & 0xFF), (PeWorld.B_CONCRETE_GRAY & 0xFF),
      (PeWorld.B_CONCRETE_LIGHT_GRAY & 0xFF), (PeWorld.B_CONCRETE_CYAN & 0xFF), (PeWorld.B_CONCRETE_PURPLE & 0xFF), (PeWorld.B_CONCRETE_BLUE & 0xFF),
      (PeWorld.B_CONCRETE_BROWN & 0xFF), (PeWorld.B_CONCRETE_GREEN & 0xFF), (PeWorld.B_CONCRETE_RED & 0xFF), (PeWorld.B_CONCRETE_BLACK & 0xFF)
   };

   public static final int[] TERRACOTTA_BLOCKS = {
      (PeWorld.B_TERRACOTTA_WHITE & 0xFF), (PeWorld.B_TERRACOTTA_ORANGE & 0xFF), (PeWorld.B_TERRACOTTA_MAGENTA & 0xFF), (PeWorld.B_TERRACOTTA_LIGHT_BLUE & 0xFF),
      (PeWorld.B_TERRACOTTA_YELLOW & 0xFF), (PeWorld.B_TERRACOTTA_LIME & 0xFF), (PeWorld.B_TERRACOTTA_PINK & 0xFF), (PeWorld.B_TERRACOTTA_GRAY & 0xFF),
      (PeWorld.B_TERRACOTTA_LIGHT_GRAY & 0xFF), (PeWorld.B_TERRACOTTA_CYAN & 0xFF), (PeWorld.B_TERRACOTTA_PURPLE & 0xFF), (PeWorld.B_TERRACOTTA_BLUE & 0xFF),
      (PeWorld.B_TERRACOTTA_BROWN & 0xFF), (PeWorld.B_TERRACOTTA_GREEN & 0xFF), (PeWorld.B_TERRACOTTA_RED & 0xFF), (PeWorld.B_TERRACOTTA_BLACK & 0xFF)
   };

   public static final int[] POWDER_BLOCKS = {
      (PeWorld.B_CONCRETE_POWDER_WHITE & 0xFF), (PeWorld.B_CONCRETE_POWDER_ORANGE & 0xFF), (PeWorld.B_CONCRETE_POWDER_MAGENTA & 0xFF), (PeWorld.B_CONCRETE_POWDER_LIGHT_BLUE & 0xFF),
      (PeWorld.B_CONCRETE_POWDER_YELLOW & 0xFF), (PeWorld.B_CONCRETE_POWDER_LIME & 0xFF), (PeWorld.B_CONCRETE_POWDER_PINK & 0xFF), (PeWorld.B_CONCRETE_POWDER_GRAY & 0xFF),
      (PeWorld.B_CONCRETE_POWDER_LIGHT_GRAY & 0xFF), (PeWorld.B_CONCRETE_POWDER_CYAN & 0xFF), (PeWorld.B_CONCRETE_POWDER_PURPLE & 0xFF), (PeWorld.B_CONCRETE_POWDER_BLUE & 0xFF),
      (PeWorld.B_CONCRETE_POWDER_BROWN & 0xFF), (PeWorld.B_CONCRETE_POWDER_GREEN & 0xFF), (PeWorld.B_CONCRETE_POWDER_RED & 0xFF), (PeWorld.B_CONCRETE_POWDER_BLACK & 0xFF)
   };

   public static final int[] STAINED_GLASS_BLOCKS = {
      (PeWorld.B_STAINED_GLASS_WHITE & 0xFF), (PeWorld.B_STAINED_GLASS_ORANGE & 0xFF), (PeWorld.B_STAINED_GLASS_MAGENTA & 0xFF), (PeWorld.B_STAINED_GLASS_LIGHT_BLUE & 0xFF),
      (PeWorld.B_STAINED_GLASS_YELLOW & 0xFF), (PeWorld.B_STAINED_GLASS_LIME & 0xFF), (PeWorld.B_STAINED_GLASS_PINK & 0xFF), (PeWorld.B_STAINED_GLASS_GRAY & 0xFF),
      (PeWorld.B_STAINED_GLASS_LIGHT_GRAY & 0xFF), (PeWorld.B_STAINED_GLASS_CYAN & 0xFF), (PeWorld.B_STAINED_GLASS_PURPLE & 0xFF), (PeWorld.B_STAINED_GLASS_BLUE & 0xFF),
      (PeWorld.B_STAINED_GLASS_BROWN & 0xFF), (PeWorld.B_STAINED_GLASS_GREEN & 0xFF), (PeWorld.B_STAINED_GLASS_RED & 0xFF), (PeWorld.B_STAINED_GLASS_BLACK & 0xFF)
   };

   public static final int[] GLAZED_BLOCKS = {
      (PeWorld.B_GLAZED_TERRACOTTA_WHITE & 0xFF), (PeWorld.B_GLAZED_TERRACOTTA_ORANGE & 0xFF), (PeWorld.B_GLAZED_TERRACOTTA_MAGENTA & 0xFF), (PeWorld.B_GLAZED_TERRACOTTA_LIGHT_BLUE & 0xFF),
      (PeWorld.B_GLAZED_TERRACOTTA_YELLOW & 0xFF), (PeWorld.B_GLAZED_TERRACOTTA_LIME & 0xFF), (PeWorld.B_GLAZED_TERRACOTTA_PINK & 0xFF), (PeWorld.B_GLAZED_TERRACOTTA_GRAY & 0xFF),
      (PeWorld.B_GLAZED_TERRACOTTA_LIGHT_GRAY & 0xFF), (PeWorld.B_GLAZED_TERRACOTTA_CYAN & 0xFF), (PeWorld.B_GLAZED_TERRACOTTA_PURPLE & 0xFF), (PeWorld.B_GLAZED_TERRACOTTA_BLUE & 0xFF),
      (PeWorld.B_GLAZED_TERRACOTTA_BROWN & 0xFF), (PeWorld.B_GLAZED_TERRACOTTA_GREEN & 0xFF), (PeWorld.B_GLAZED_TERRACOTTA_RED & 0xFF), (PeWorld.B_GLAZED_TERRACOTTA_BLACK & 0xFF)
   };

   public static final String[] FAMILY_NAMES = {
      "WOOL", "CONCRETE", "STAINED TERRACOTTA", "CONCRETE POWDER", "STAINED GLASS", "GLAZED TERRACOTTA"
   };

   /** Six block families plus the craftable 16-color dye collection shown in the mobile tab. */
   public static final String[] SELECTOR_NAMES = {
      "WOOL", "CONCRETE", "STAINED TERRACOTTA", "CONCRETE POWDER", "STAINED GLASS", "GLAZED TERRACOTTA", "DYES"
   };

   public static final int[] DYE_ITEMS = {
      PeWorld.I_DYE_WHITE, PeWorld.I_DYE_ORANGE, PeWorld.I_DYE_MAGENTA, PeWorld.I_DYE_LIGHT_BLUE,
      PeWorld.I_DYE_YELLOW, PeWorld.I_DYE_LIME, PeWorld.I_DYE_PINK, PeWorld.I_DYE_GRAY,
      PeWorld.I_DYE_LIGHT_GRAY, PeWorld.I_DYE_CYAN, PeWorld.I_DYE_PURPLE, PeWorld.I_DYE_BLUE,
      PeWorld.I_DYE_BROWN, PeWorld.I_DYE_GREEN, PeWorld.I_DYE_RED, PeWorld.I_DYE_BLACK
   };

   public static int dyeColorForItem(int itemId) {
      return itemId >= PeWorld.I_DYE_WHITE && itemId <= PeWorld.I_DYE_BLACK
         ? itemId - PeWorld.I_DYE_WHITE : -1;
   }

   public static final int[][] FAMILY_BLOCKS = {
      WOOL_BLOCKS, CONCRETE_BLOCKS, TERRACOTTA_BLOCKS, POWDER_BLOCKS, STAINED_GLASS_BLOCKS, GLAZED_BLOCKS
   };

   public static final int[][] FAMILY_TILES = {
      {
         PeTextures.T_WOOL_WHITE, PeTextures.T_WOOL_ORANGE, PeTextures.T_WOOL_MAGENTA, PeTextures.T_WOOL_LIGHT_BLUE,
         PeTextures.T_WOOL_YELLOW, PeTextures.T_WOOL_LIME, PeTextures.T_WOOL_PINK, PeTextures.T_WOOL_GRAY,
         PeTextures.T_WOOL_LIGHT_GRAY, PeTextures.T_WOOL_CYAN, PeTextures.T_WOOL_PURPLE, PeTextures.T_WOOL_BLUE,
         PeTextures.T_WOOL_BROWN, PeTextures.T_WOOL_GREEN, PeTextures.T_WOOL_RED, PeTextures.T_WOOL_BLACK
      },
      {
         PeTextures.T_CONCRETE_WHITE, PeTextures.T_CONCRETE_ORANGE, PeTextures.T_CONCRETE_MAGENTA, PeTextures.T_CONCRETE_LIGHT_BLUE,
         PeTextures.T_CONCRETE_YELLOW, PeTextures.T_CONCRETE_LIME, PeTextures.T_CONCRETE_PINK, PeTextures.T_CONCRETE_GRAY,
         PeTextures.T_CONCRETE_LIGHT_GRAY, PeTextures.T_CONCRETE_CYAN, PeTextures.T_CONCRETE_PURPLE, PeTextures.T_CONCRETE_BLUE,
         PeTextures.T_CONCRETE_BROWN, PeTextures.T_CONCRETE_GREEN, PeTextures.T_CONCRETE_RED, PeTextures.T_CONCRETE_BLACK
      },
      {
         PeTextures.T_TERRACOTTA_WHITE, PeTextures.T_TERRACOTTA_ORANGE, PeTextures.T_TERRACOTTA_MAGENTA, PeTextures.T_TERRACOTTA_LIGHT_BLUE,
         PeTextures.T_TERRACOTTA_YELLOW, PeTextures.T_TERRACOTTA_LIME, PeTextures.T_TERRACOTTA_PINK, PeTextures.T_TERRACOTTA_GRAY,
         PeTextures.T_TERRACOTTA_LIGHT_GRAY, PeTextures.T_TERRACOTTA_CYAN, PeTextures.T_TERRACOTTA_PURPLE, PeTextures.T_TERRACOTTA_BLUE,
         PeTextures.T_TERRACOTTA_BROWN, PeTextures.T_TERRACOTTA_GREEN, PeTextures.T_TERRACOTTA_RED, PeTextures.T_TERRACOTTA_BLACK
      },
      {
         PeTextures.T_POWDER_WHITE, PeTextures.T_POWDER_ORANGE, PeTextures.T_POWDER_MAGENTA, PeTextures.T_POWDER_LIGHT_BLUE,
         PeTextures.T_POWDER_YELLOW, PeTextures.T_POWDER_LIME, PeTextures.T_POWDER_PINK, PeTextures.T_POWDER_GRAY,
         PeTextures.T_POWDER_LIGHT_GRAY, PeTextures.T_POWDER_CYAN, PeTextures.T_POWDER_PURPLE, PeTextures.T_POWDER_BLUE,
         PeTextures.T_POWDER_BROWN, PeTextures.T_POWDER_GREEN, PeTextures.T_POWDER_RED, PeTextures.T_POWDER_BLACK
      },
      {
         PeTextures.T_STAINED_GLASS_WHITE, PeTextures.T_STAINED_GLASS_ORANGE, PeTextures.T_STAINED_GLASS_MAGENTA, PeTextures.T_STAINED_GLASS_LIGHT_BLUE,
         PeTextures.T_STAINED_GLASS_YELLOW, PeTextures.T_STAINED_GLASS_LIME, PeTextures.T_STAINED_GLASS_PINK, PeTextures.T_STAINED_GLASS_GRAY,
         PeTextures.T_STAINED_GLASS_LIGHT_GRAY, PeTextures.T_STAINED_GLASS_CYAN, PeTextures.T_STAINED_GLASS_PURPLE, PeTextures.T_STAINED_GLASS_BLUE,
         PeTextures.T_STAINED_GLASS_BROWN, PeTextures.T_STAINED_GLASS_GREEN, PeTextures.T_STAINED_GLASS_RED, PeTextures.T_STAINED_GLASS_BLACK
      },
      {
         PeTextures.T_GLAZED_WHITE, PeTextures.T_GLAZED_ORANGE, PeTextures.T_GLAZED_MAGENTA, PeTextures.T_GLAZED_LIGHT_BLUE,
         PeTextures.T_GLAZED_YELLOW, PeTextures.T_GLAZED_LIME, PeTextures.T_GLAZED_PINK, PeTextures.T_GLAZED_GRAY,
         PeTextures.T_GLAZED_LIGHT_GRAY, PeTextures.T_GLAZED_CYAN, PeTextures.T_GLAZED_PURPLE, PeTextures.T_GLAZED_BLUE,
         PeTextures.T_GLAZED_BROWN, PeTextures.T_GLAZED_GREEN, PeTextures.T_GLAZED_RED, PeTextures.T_GLAZED_BLACK
      }
   };

   public static final int[] ALL_BLOCKS = new int[6 * 16];

   static {
      for (int family = 0; family < FAMILY_BLOCKS.length; family++) {
         System.arraycopy(FAMILY_BLOCKS[family], 0, ALL_BLOCKS, family * 16, 16);
      }
   }

   public static int familyForBlock(int blockId) {
      int id = blockId & 0xFF;
      for (int family = 0; family < FAMILY_BLOCKS.length; family++) {
         for (int color = 0; color < 16; color++) {
            if ((FAMILY_BLOCKS[family][color] & 0xFF) == id) return family;
         }
      }
      return -1;
   }

   public static int colorForBlock(int blockId) {
      int id = blockId & 0xFF;
      for (int family = 0; family < FAMILY_BLOCKS.length; family++) {
         for (int color = 0; color < 16; color++) {
            if ((FAMILY_BLOCKS[family][color] & 0xFF) == id) return color;
         }
      }
      return -1;
   }

   public static int tileForBlock(int blockId) {
      int id = blockId & 0xFF;
      for (int family = 0; family < FAMILY_BLOCKS.length; family++) {
         for (int color = 0; color < 16; color++) {
            if ((FAMILY_BLOCKS[family][color] & 0xFF) == id) return FAMILY_TILES[family][color];
         }
      }
      return -1;
   }

   public static String nameForBlock(int blockId) {
      int family = familyForBlock(blockId);
      int color = colorForBlock(blockId);
      if (family < 0 || color < 0) return null;
      switch (family) {
         case 0: return NAMES[color] + " Wool";
         case 1: return NAMES[color] + " Concrete";
         case 2: return NAMES[color] + " Terracotta";
         case 3: return NAMES[color] + " Concrete Powder";
         case 4: return NAMES[color] + " Stained Glass";
         default: return NAMES[color] + " Glazed Terracotta";
      }
   }

   public static boolean isPowder(int blockId) {
      return familyForBlock(blockId) == 3;
   }

   public static boolean isStainedGlass(int blockId) {
      return familyForBlock(blockId) == 4;
   }

   public static int hardenedBlock(int powderId) {
      int color = colorForBlock(powderId);
      return color < 0 ? -1 : CONCRETE_BLOCKS[color] & 0xFF;
   }
}
