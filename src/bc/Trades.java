package bc;

/**
 * Update 3.12: what villagers buy and sell. Every profession has its own list; the emerald is the money. A trade can be made any number
 * of times (no restocking to wait for): click the result and the price is taken from your inventory.
 */
final class Trades {
   static final int FARMER = 0;
   static final int LIBRARIAN = 1;
   static final int PRIEST = 2;
   static final int SMITH = 3;
   static final int BUTCHER = 4;
   static final int NITWIT = 5;
   static final String[] NAMES = {"Farmer", "Librarian", "Priest", "Blacksmith", "Butcher", "Nitwit"};
   /** The robe colours of the professions (RGB). */
   static final int[] ROBE = {0x7A5230, 0xEDEDED, 0x8B3FA8, 0x3A3A3A, 0xC8C8C8, 0x4E9A3E};

   private Trades() {
   }

   static final class Trade {
      final int a;
      final int an;
      final int b;
      final int bn;
      final int out;
      final int outN;

      Trade(int a, int an, int b, int bn, int out, int outN) {
         this.a = a;
         this.an = an;
         this.b = b;
         this.bn = bn;
         this.out = out;
         this.outN = outN;
      }
   }

   private static Trade t(int a, int an, int out, int outN) {
      return new Trade(a, an, 0, 0, out, outN);
   }

   static String name(int profession) {
      return NAMES[Math.max(0, Math.min(NAMES.length - 1, profession))];
   }

   /** Up to six trades of a profession. */
   static Trade[] offers(int profession) {
      int e = Items.EMERALD;
      switch (profession) {
         case FARMER:
            return new Trade[]{t(Items.WHEAT, 18, e, 1), t(Items.POTATO, 15, e, 1), t(Items.CARROT, 15, e, 1), t(e, 1, Items.BREAD, 4), t(e, 1, Items.APPLE, 4), t(e, 3, Items.CAKE, 1)};
         case LIBRARIAN:
            return new Trade[]{t(Items.PAPER, 24, e, 1), t(Items.BOOK, 4, e, 1), t(e, 3, Blocks.BOOKSHELF, 1), t(e, 1, Items.PAPER, 6), t(e, 5, Blocks.GLASS, 8), t(e, 8, Items.BOOK, 3)};
         case PRIEST:
            return new Trade[]{t(Items.ROTTEN_FLESH, 32, e, 1), t(Items.GOLD_INGOT, 3, e, 1), t(e, 1, Items.BONE_MEAL, 4), t(e, 4, Blocks.GLOWSTONE, 2), t(e, 5, Items.ENDER_PEARL, 1), t(e, 3, Items.BLAZE_POWDER, 2)};
         case SMITH:
            return new Trade[]{t(Items.COAL, 16, e, 1), t(Items.IRON_INGOT, 4, e, 1), t(e, 5, Items.toolId(2, Items.C_AXE), 1), t(e, 7, Items.toolId(2, Items.C_SWORD), 1), t(e, 9, Items.armorId(1, 1), 1), t(e, 13, Items.toolId(4, Items.C_PICK), 1)};
         case BUTCHER:
            return new Trade[]{t(Items.RAW_PORK, 14, e, 1), t(Items.RAW_CHICKEN, 14, e, 1), t(Items.RAW_BEEF, 10, e, 1), t(e, 1, Items.COOKED_PORK, 5), t(e, 1, Items.COOKED_CHICKEN, 4), t(e, 2, Items.STEAK, 4)};
         default:
            return new Trade[0];
      }
   }

   static boolean has(Game g, Trade t) {
      return g.player.creative || g.countItem(t.a) >= t.an && (t.bn == 0 || g.countItem(t.b) >= t.bn);
   }

   /** Makes the trade: the price comes out of the inventory, the goods go in (what does not fit falls to the floor). True if it was made. */
   static boolean buy(Game g, Trade t) {
      if (!has(g, t)) {
         g.say("You need " + t.an + " x " + Items.name(t.a));
         return false;
      }

      if (!g.player.creative) {
         g.removeItems(t.a, t.an);
         if (t.bn > 0) {
            g.removeItems(t.b, t.bn);
         }
      }

      Stack s = new Stack(t.out, t.outN);
      g.addItem(s);
      if (s.n > 0) {
         g.dropFromPlayer(s.id, s.n, s.dmg);
      }

      g.adv.award(g, "adventure/trade");
      return true;
   }
}
