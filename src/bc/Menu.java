package bc;

import java.util.ArrayList;

final class Menu {
   static final int PLAYER = 0;
   static final int CRAFT3 = 1;
   static final int FURNACE = 2;
   static final int CHEST = 3;
   static final int CREATIVE = 4;
   /** Update 3.12: a villager's trades (profession in {@link #profession}). */
   static final int TRADE = 5;
   static final int NORMAL = 0;
   static final int OUTPUT = 1;
   static final int ARMOR = 2;
   static final int FURN_OUT = 3;
   static final int FURN_FUEL = 4;
   static final int VIRTUAL = 5;
   final Game g;
   final int kind;
   TileEntity tile;
   final ArrayList<Menu.Slot> slots = new ArrayList();
   float w = 176.0F;
   float h = 166.0F;
   String title = "";
   Stack[] grid;
   int gw;
   int gh;
   final Stack[] out = new Stack[1];
   int page;
   int[] palette;
   final ArrayList<Menu.Slot> paletteSlots = new ArrayList();
   int firstInvSlot;
   int invSlotCount;
   int profession;
   Trades.Trade[] trades = new Trades.Trade[0];
   /** Creative: the tab (Creative.BUILDING ...) and the text typed in the search tab. */
   int tab;
   final StringBuilder search = new StringBuilder();
   /** The tab under the mouse in the last frame (for its tooltip). */
   int hoverTab = -1;
   /** When the menu was opened: the letter of the key that opened it (E) must not end up in the search field. */
   final long openedAt = System.currentTimeMillis();

   Menu(Game var1, int var2, TileEntity var3) {
      this(var1, var2, var3, 0);
   }

   Menu(Game var1, int var2, TileEntity var3, int profession) {
      this.g = var1;
      this.kind = var2;
      this.tile = var3;
      this.profession = profession;
      this.build();
   }

   private Menu.Slot add(Stack[] var1, int var2, float var3, float var4, int var5) {
      Menu.Slot var6 = new Menu.Slot();
      var6.arr = var1;
      var6.idx = var2;
      var6.x = var3;
      var6.y = var4;
      var6.type = var5;
      this.slots.add(var6);
      return var6;
   }

   private void playerInv(float var1, float var2, float var3) {
      this.firstInvSlot = this.slots.size();

      for (int var4 = 0; var4 < 3; var4++) {
         for (int var5 = 0; var5 < 9; var5++) {
            this.add(this.g.inv, 9 + var4 * 9 + var5, var3 + var5 * 18, var1 + var4 * 18, 0);
         }
      }

      for (int var6 = 0; var6 < 9; var6++) {
         this.add(this.g.inv, var6, var3 + var6 * 18, var2, 0);
      }

      this.invSlotCount = 36;
   }

   private void build() {
      switch (this.kind) {
         case 0:
            this.title = "Crafting";
            this.gw = this.gh = 2;
            this.grid = new Stack[4];
            int var6 = 0;

            while (var6 < 4) {
               Menu.Slot var8 = this.add(this.g.armor, var6, 8.0F, 8 + var6 * 18, 2);
               var8.armorPiece = var6++;
            }

            for (int var7 = 0; var7 < 4; var7++) {
               this.add(this.grid, var7, 98 + var7 % 2 * 18, 18 + var7 / 2 * 18, 0);
            }

            this.add(this.out, 0, 154.0F, 28.0F, 1);
            this.playerInv(84.0F, 142.0F, 8.0F);
            break;
         case 1:
            this.title = "Crafting";
            this.gw = this.gh = 3;
            this.grid = new Stack[9];

            for (int var5 = 0; var5 < 9; var5++) {
               this.add(this.grid, var5, 30 + var5 % 3 * 18, 17 + var5 / 3 * 18, 0);
            }

            this.add(this.out, 0, 124.0F, 35.0F, 1);
            this.playerInv(84.0F, 142.0F, 8.0F);
            break;
         case 2:
            this.title = "Furnace";
            this.add(this.tile.slots, 0, 56.0F, 17.0F, 0);
            this.add(this.tile.slots, 1, 56.0F, 53.0F, 4);
            this.add(this.tile.slots, 2, 116.0F, 35.0F, 3);
            this.playerInv(84.0F, 142.0F, 8.0F);
            break;
         case 3:
            this.title = "Chest";

            for (int var4 = 0; var4 < 27; var4++) {
               this.add(this.tile.slots, var4, 8 + var4 % 9 * 18, 18 + var4 / 9 * 18, 0);
            }

            this.playerInv(84.0F, 142.0F, 8.0F);
            break;
         case 5:
            // Update 3.12: two columns of three trades: [price] [price] -> [goods]; a click on the goods buys
            this.title = Trades.name(this.profession);
            this.trades = Trades.offers(this.profession);
            for (int i = 0; i < this.trades.length; i++) {
               Trades.Trade t = this.trades[i];
               float x0 = i < 3 ? 8.0F : 92.0F;
               float y0 = 18.0F + i % 3 * 18;
               Stack[] a = new Stack[]{new Stack(t.a, t.an)};
               Stack[] b = new Stack[]{t.bn > 0 ? new Stack(t.b, t.bn) : null};
               Stack[] o = new Stack[]{new Stack(t.out, t.outN)};
               this.add(a, 0, x0, y0, 6);
               this.add(b, 0, x0 + 18.0F, y0, 6);
               this.add(o, 0, x0 + 54.0F, y0, 7).armorPiece = i;
            }

            this.playerInv(84.0F, 142.0F, 8.0F);
            break;
         default:
            this.title = "Creative";
            this.w = 195.0F;
            this.h = 136.0F;
            this.tab = Math.max(0, Math.min(Creative.SEARCH, g.creativeTab));
            this.palette = this.tab == Creative.SEARCH ? Creative.search("") : Creative.tab(this.tab);
            this.firstInvSlot = this.slots.size();

            for (int var1 = 0; var1 < 9; var1++) {
               this.add(this.g.inv, var1, 9 + var1 * 18, 112.0F, 0);
            }

            this.invSlotCount = 9;

            for (int var3 = 0; var3 < 45; var3++) {
               Menu.Slot var2 = this.add(null, var3, 9 + var3 % 9 * 18, 18 + var3 / 9 * 18, 5);
               this.paletteSlots.add(var2);
            }
      }

      this.changed();
   }

   int pages() {
      return this.palette == null ? 1 : Math.max(1, (this.palette.length + 44) / 45);
   }

   /** Creative: switches to a tab (the search tab shows everything that matches the text typed). */
   void setTab(int t) {
      this.tab = Math.max(0, Math.min(Creative.SEARCH, t));
      if (this.tab != Creative.SEARCH) {
         this.g.creativeTab = this.tab; // (the search tab is not remembered: E must close the inventory when it opens)
      }

      this.page = 0;
      this.refill();
   }

   void refill() {
      this.palette = this.tab == Creative.SEARCH ? Creative.search(this.search.toString()) : Creative.tab(this.tab);
      this.page = Math.max(0, Math.min(this.pages() - 1, this.page));
   }

   static final ArrayList<String> searchHistory = new ArrayList<>();
   int searchHistPos = -1;

   /** Creative, search tab: a typed character. */
   void typed(char c) {
      if (this.kind == 4 && this.tab == Creative.SEARCH && c >= ' ' && c < 127 && this.search.length() < 24) {
         this.search.append(c);
         Game.recordHistory(searchHistory, this.search.toString());
         this.searchHistPos = searchHistory.size();
         this.page = 0;
         this.refill();
      }
   }

   void pasteSearch(String s) {
      if (this.kind == 4 && this.tab == Creative.SEARCH && s != null && !s.isEmpty()) {
         this.search.append(s);
         Game.recordHistory(searchHistory, this.search.toString());
         this.searchHistPos = searchHistory.size();
         this.page = 0;
         this.refill();
      }
   }

   void clearSearch() {
      if (this.kind == 4 && this.tab == Creative.SEARCH && this.search.length() > 0) {
         this.search.setLength(0);
         this.page = 0;
         this.refill();
      }
   }

   void stepSearchHistory(int dir) {
      if (this.kind == 4 && this.tab == Creative.SEARCH && !searchHistory.isEmpty()) {
         if (this.searchHistPos < 0 || this.searchHistPos > searchHistory.size()) {
            this.searchHistPos = searchHistory.size();
         }
         int next = Math.max(0, Math.min(searchHistory.size() - 1, this.searchHistPos + dir));
         this.searchHistPos = next;
         this.search.setLength(0);
         this.search.append(searchHistory.get(next));
         this.page = 0;
         this.refill();
      }
   }

   void backspace() {
      if (this.kind == 4 && this.tab == Creative.SEARCH && this.search.length() > 0) {
         this.search.setLength(this.search.length() - 1);
         this.page = 0;
         this.refill();
      }
   }

   /** The number keys 1-9 over a slot: that slot and hotbar slot n swap places (creative: the item goes into the hotbar slot). */
   void hotbarKey(int n, float mx, float my, float ox, float oy) {
      Menu.Slot s = this.at(mx, my, ox, oy);
      if (s == null || n < 0 || n > 8) {
         return;
      }

      if (s.type == 5) {
         int idx = this.page * 45 + s.idx;
         if (idx < this.palette.length) {
            this.g.inv[n] = new Stack(this.palette[idx], Items.maxStack(this.palette[idx]));
         }

         return;
      }

      if (s.type != 0 || s.arr == this.g.inv && s.idx == n) {
         return;
      }

      Stack a = s.arr[s.idx];
      Stack b = this.g.inv[n];
      if (b != null && !this.accepts(s, b)) {
         return;
      }

      s.arr[s.idx] = b;
      this.g.inv[n] = a;
      this.changed();
   }

   /** The Q key over a slot: drops one item of it (Ctrl: the whole stack). */
   void dropKey(float mx, float my, float ox, float oy, boolean all) {
      Menu.Slot s = this.at(mx, my, ox, oy);
      if (s == null || s.type != 0 && s.type != 4 || s.arr == null) {
         return;
      }

      Stack st = s.arr[s.idx];
      if (st == null) {
         return;
      }

      int cnt = all ? st.n : 1;
      this.g.dropFromPlayer(st.id, cnt, st.dmg);
      st.n -= cnt;
      if (st.n <= 0) {
         s.arr[s.idx] = null;
      }

      this.changed();
   }

   /** The width of one tab (creative). */
   static final float TAB_W = 24.0F;
   static final float TAB_H = 22.0F;

   /** Creative: which tab is at the mouse (panel at ox, oy), or -1. */
   int tabAt(float mx, float my, float ox, float oy) {
      if (this.kind != 4 || my < oy - TAB_H - 1.0F || my >= oy + 2.0F) {
         return -1;
      }

      int i = (int)Math.floor((mx - ox) / TAB_W);
      return i >= 0 && i < Creative.TABS && mx >= ox ? i : -1;
   }

   void changed() {
      if (this.grid != null) {
         this.out[0] = Recipes.find(this.grid, this.gw, this.gh);
      }
      if (this.tile != null && this.g != null && this.g.world != null) {
         Net319.onTileChanged(this.g.world, this.tile);
      }
   }

   boolean accepts(Menu.Slot var1, Stack var2) {
      if (var2 == null) {
         return true;
      }

      switch (var1.type) {
         case 1:
         case 3:
            return false;
         case 2:
            return Items.isArmor(var2.id) && Items.armorSlot(var2.id) == var1.armorPiece;
         case 4:
            return Recipes.fuelTime(var2) > 0;
         default:
            return true;
      }
   }

   int limit(Menu.Slot var1, Stack var2) {
      return var1.type == 2 ? 1 : var2.max();
   }

   Menu.Slot at(float var1, float var2, float var3, float var4) {
      for (Menu.Slot var6 : this.slots) {
         if (var6.type != 5 || this.page * 45 + var6.idx < this.palette.length) {
            float var7 = var3 + var6.x;
            float var8 = var4 + var6.y;
            if (var1 >= var7 && var1 < var7 + 18.0F && var2 >= var8 && var2 < var8 + 18.0F) {
               return var6;
            }
         }
      }

      return null;
   }

   Stack stackIn(Menu.Slot var1) {
      if (var1.type == 5) {
         int var2 = this.page * 45 + var1.idx;
         return var2 < this.palette.length ? new Stack(this.palette[var2], 1) : null;
      } else {
         return var1.arr[var1.idx];
      }
   }

   void click(Menu.Slot var1, int var2, boolean var3) {
      Stack var4 = this.g.cursor;
      if (var1.type == 6) {
         return;
      }

      if (var1.type == 7) {
         if (var1.armorPiece >= 0 && var1.armorPiece < this.trades.length) {
            Trades.buy(this.g, this.trades[var1.armorPiece]);
         }

         return;
      }

      if (var1.type == 5) {
         int var8 = this.page * 45 + var1.idx;
         if (var4 != null) {
            this.g.cursor = null;
         } else {
            if (var8 < this.palette.length) {
               int var11 = this.palette[var8];
               Stack var7 = new Stack(var11, var2 == 0 ? Items.maxStack(var11) : 1);
               if (var3) {
                  this.g.addItem(new Stack(var11, Items.maxStack(var11)));
                  return;
               }

               this.g.cursor = var7;
            }
         }
      } else if (var1.type == 1 || var1.type == 3) {
         this.takeOutput(var1, var3);
      } else if (var3) {
         this.quickMove(var1);
         this.changed();
      } else {
         Stack var5 = var1.arr[var1.idx];
         if (var2 == 0) {
            if (var4 == null) {
               this.g.cursor = var5;
               var1.arr[var1.idx] = null;
            } else if (this.accepts(var1, var4)) {
               if (var5 == null) {
                  int var6 = Math.min(var4.n, this.limit(var1, var4));
                  var1.arr[var1.idx] = new Stack(var4.id, var6, var4.dmg);
                  var4.n -= var6;
                  if (var4.n <= 0) {
                     this.g.cursor = null;
                  }
               } else if (var5.same(var4)) {
                  int var9 = Math.min(this.limit(var1, var5) - var5.n, var4.n);
                  var5.n += var9;
                  var4.n -= var9;
                  if (var4.n <= 0) {
                     this.g.cursor = null;
                  }
               } else if (var4.n <= this.limit(var1, var4)) {
                  var1.arr[var1.idx] = var4;
                  this.g.cursor = var5;
               }
            }
         } else if (var4 == null) {
            if (var5 != null) {
               int var10 = (var5.n + 1) / 2;
               this.g.cursor = new Stack(var5.id, var10, var5.dmg);
               var5.n -= var10;
               if (var5.n <= 0) {
                  var1.arr[var1.idx] = null;
               }
            }
         } else if (this.accepts(var1, var4)) {
            if (var5 == null) {
               var1.arr[var1.idx] = new Stack(var4.id, 1, var4.dmg);
               var4.n--;
               if (var4.n <= 0) {
                  this.g.cursor = null;
               }
            } else if (var5.same(var4) && var5.n < this.limit(var1, var5)) {
               var5.n++;
               var4.n--;
               if (var4.n <= 0) {
                  this.g.cursor = null;
               }
            }
         }

         this.changed();
      }
   }

   void takeOutput(Menu.Slot var1, boolean var2) {
      if (var1.type == 3) {
         Stack var7 = var1.arr[var1.idx];
         if (var7 != null) {
            if (var2) {
               Stack var8 = this.moveToPlayer(var7);
               var1.arr[var1.idx] = var8;
            } else {
               if (this.g.cursor == null) {
                  this.g.cursor = var7;
                  var1.arr[var1.idx] = null;
               } else if (this.g.cursor.same(var7) && this.g.cursor.n + var7.n <= var7.max()) {
                  this.g.cursor.n = this.g.cursor.n + var7.n;
                  var1.arr[var1.idx] = null;
               }
            }
            if (this.tile != null && this.g != null && this.g.world != null) {
               Net319.onTileChanged(this.g.world, this.tile);
            }
         }
      } else {
         for (int var3 = 0; var3 < 64; var3++) {
            Stack var4 = this.out[0];
            if (var4 == null) {
               return;
            }

            if (var2) {
               Stack var5 = var4.copy();
               Stack var6 = this.moveToPlayerSim(var5);
               if (var6 != null) {
                  return;
               }

               this.moveToPlayer(var4.copy());
            } else if (this.g.cursor == null) {
               this.g.cursor = var4.copy();
            } else {
               if (!this.g.cursor.same(var4) || this.g.cursor.n + var4.n > var4.max()) {
                  return;
               }

               this.g.cursor.n = this.g.cursor.n + var4.n;
            }

            for (int var9 = 0; var9 < this.grid.length; var9++) {
               if (this.grid[var9] != null) {
                  int left = Recipes.leftover(this.grid[var9].id);
                  this.grid[var9].n--;
                  if (this.grid[var9].n <= 0) {
                     // a used-up milk bucket leaves its empty bucket in the slot (Update 3.7)
                     this.grid[var9] = left != 0 ? new Stack(left, 1) : null;
                  } else if (left != 0 && !this.g.addItem(new Stack(left, 1))) {
                     this.g.dropFromPlayer(left, 1);
                  }
               }
            }

            this.changed();
            if (!var2) {
               return;
            }
         }
      }
   }

   Stack moveToPlayerSim(Stack var1) {
      Stack[] var2 = new Stack[36];

      for (int var3 = 0; var3 < 36; var3++) {
         var2[var3] = this.g.inv[var3] == null ? null : this.g.inv[var3].copy();
      }

      return insert(var2, var1.copy(), 0, 36, false);
   }

   Stack moveToPlayer(Stack var1) {
      Stack var2 = insert(this.g.inv, var1, 9, 36, true);
      if (var2 != null) {
         var2 = insert(this.g.inv, var2, 0, 9, true);
      }

      return var2;
   }

   static Stack insert(Stack[] var0, Stack var1, int var2, int var3, boolean var4) {
      if (var1 == null) {
         return null;
      }

      for (int var5 = var2; var5 < var3 && var1.n > 0; var5++) {
         Stack var6 = var0[var5];
         if (var6 != null && var6.same(var1) && var6.n < var6.max()) {
            int var7 = Math.min(var6.max() - var6.n, var1.n);
            var6.n += var7;
            var1.n -= var7;
         }
      }

      for (int var8 = var2; var8 < var3 && var1.n > 0; var8++) {
         if (var0[var8] == null) {
            int var9 = Math.min(var1.max(), var1.n);
            var0[var8] = new Stack(var1.id, var9, var1.dmg);
            var1.n -= var9;
         }
      }

      return var1.n > 0 ? var1 : null;
   }

   boolean isPlayerSlot(Menu.Slot var1) {
      return var1.arr == this.g.inv;
   }

   void quickMove(Menu.Slot var1) {
      Stack var2 = var1.arr[var1.idx];
      if (var2 != null) {
         if (this.isPlayerSlot(var1)) {
            Stack var3 = var2;
            switch (this.kind) {
               case 0:
                  if (Items.isArmor(var2.id) && this.g.armor[Items.armorSlot(var2.id)] == null) {
                     this.g.armor[Items.armorSlot(var2.id)] = var2;
                     var3 = null;
                     break;
                  }
               case 1:
               default:
                  if (var1.idx < 9) {
                     var3 = insert(this.g.inv, var2, 9, 36, true);
                  } else {
                     var3 = insert(this.g.inv, var2, 0, 9, true);
                  }
                  break;
               case 2:
                  if (Recipes.fuelTime(var2) > 0 && this.tile.slots[1] == null && Recipes.smeltResult(var2) == null) {
                     var3 = insert(this.tile.slots, var2, 1, 2, true);
                  } else if (Recipes.smeltResult(var2) != null) {
                     var3 = insert(this.tile.slots, var2, 0, 1, true);
                  } else if (Recipes.fuelTime(var2) > 0) {
                     var3 = insert(this.tile.slots, var2, 1, 2, true);
                  }
                  break;
               case 3:
                  var3 = insert(this.tile.slots, var2, 0, 27, true);
            }

            var1.arr[var1.idx] = var3;
         } else {
            Stack var4 = this.moveToPlayer(var2);
            var1.arr[var1.idx] = var4;
         }
      }
   }

   void close() {
      if (this.g.cursor != null) {
         if (this.kind != 4 && !this.g.addItem(this.g.cursor)) {
            this.g.dropFromPlayer(this.g.cursor.id, this.g.cursor.n, this.g.cursor.dmg);
         }

         this.g.cursor = null;
      }

      if (this.grid != null) {
         for (int var1 = 0; var1 < this.grid.length; var1++) {
            if (this.grid[var1] != null) {
               if (!this.g.addItem(this.grid[var1])) {
                  this.g.dropFromPlayer(this.grid[var1].id, this.grid[var1].n, this.grid[var1].dmg);
               }

               this.grid[var1] = null;
            }
         }
      }
   }

   static final String[] MAGNIFIER = {
      "..XXXX.....", ".X....X....", "X......X...", "X......X...", "X......X...", ".X....X....", "..XXXXX....", ".......XX..", "........XX.", ".........XX"
   };

   /** Creative: the tabs above the panel (the picture of each, the selected one is joined to the panel). */
   void drawTabs(Gui gui, float mx, float my, float ox, float oy) {
      this.hoverTab = this.tabAt(mx, my, ox, oy);
      for (int pass = 0; pass < 2; pass++) {
         for (int i = 0; i < Creative.TABS; i++) {
            boolean sel = i == this.tab;
            if (sel != (pass == 1)) {
               continue; // the selected tab last, on top of its neighbours
            }

            float x = ox + i * TAB_W;
            float y = sel ? oy - TAB_H - 2.0F : oy - TAB_H;
            int fill = sel ? -3750202 : (i == this.hoverTab ? -5855578 : -7631989);
            gui.rect(x, y, TAB_W, oy - y, -16777216);
            gui.rect(x + 1.0F, y + 1.0F, TAB_W - 2.0F, oy + (sel ? 2.0F : 0.0F) - y - 1.0F, fill);
            gui.rect(x + 1.0F, y + 1.0F, TAB_W - 3.0F, 1.0F, -1);
            gui.rect(x + 1.0F, y + 1.0F, 1.0F, oy - y - 2.0F, -1);
            if (sel) {
               gui.rect(x + TAB_W - 2.0F, y + 2.0F, 1.0F, oy - y - 2.0F, -11184811);
            }

            float iy = y + (sel ? 5.0F : 4.0F);
            if (i == Creative.SEARCH) {
               for (int r = 0; r < MAGNIFIER.length; r++) {
                  for (int c = 0; c < MAGNIFIER[r].length(); c++) {
                     if (MAGNIFIER[r].charAt(c) == 'X') {
                        gui.rect(x + 6.0F + c, iy + 3.0F + r, 1.0F, 1.0F, r >= 7 ? -12566464 : -14013910);
                     }
                  }
               }
            } else {
               gui.icon(Creative.ICON[i], x + 4.0F, iy, 16.0F);
            }
         }
      }
   }

   void draw(Gui var1, float var2, float var3, float var4, float var5) {
      var1.panel(var4, var5, this.w, this.h);
      if (this.kind == 4) {
         this.drawTabs(var1, var2, var3, var4, var5);
         if (this.tab == Creative.SEARCH) {
            // the search field (what is typed shows the items whose names contain it)
            float fx = var4 + 8.0F;
            float fy = var5 + 4.0F;
            var1.rect(fx - 1.0F, fy - 1.0F, 114.0F, 13.0F, -6250336);
            var1.rect(fx, fy, 112.0F, 11.0F, -16777216);
            String typed = this.search.toString();
            if (typed.isEmpty()) {
               var1.text("Type to search...", fx + 3.0F, fy + 2.0F, -9211021, false);
            } else {
               var1.text(Game.clipText(typed, 104.0F), fx + 3.0F, fy + 2.0F, -1, false);
            }

            if (System.currentTimeMillis() / 500L % 2L == 0L) {
               var1.rect(fx + 3.0F + Math.min(104, Gui.textWidth(typed)), fy + 9.0F, 5.0F, 1.0F, -1);
            }
         } else {
            var1.text(Creative.NAME[this.tab], var4 + 8.0F, var5 + 6.0F, -12566464, false);
         }
      } else if (this.kind == 0) {
         // (no "Inventory" label here: it used to sit between the armor slots and the crafting grid, where the player is drawn now - Minecraft has none either)
         var1.text(this.title, var4 + 97.0F, var5 + 6.0F, -12566464, false);
      } else {
         var1.text(this.title, var4 + 8.0F, var5 + 6.0F, -12566464, false);
         var1.text("Inventory", var4 + 8.0F, var5 + 72.0F, -12566464, false);
      }

      for (Menu.Slot var7 : this.slots) {
         if (var7.type != 5 || this.page * 45 + var7.idx < this.palette.length) {
            var1.slot(var4 + var7.x, var5 + var7.y, 18.0F);
         }
      }

      if (this.kind == 0) {
         // Update 3.9: you, in your skin, looking at the mouse
         var1.inset(var4 + 29.0F, var5 + 8.0F, 66.0F, 72.0F);
         if (this.g.window != 0L) { // (the menu tests draw without a window)
            PlayerModel.drawLooking(this.g.skinPixels(), this.g.skinSlim(), var4 + 62.0F, var5 + 75.0F, 30.0F, var2, var3, (float)(System.nanoTime() / 1.0E9 % 100000.0));
         }
         var1.arrow(var4 + 136.0F, var5 + 32.0F, -7631989);

         for (int var9 = 0; var9 < 4; var9++) {
            if (this.g.armor[var9] == null) {
               // a faded picture of what goes in (helmet, chestplate, leggings, boots)
               float var13 = var4 + 8.0F + 1.0F;
               float var8 = var5 + 8.0F + var9 * 18 + 1.0F;
               var1.icon(324 + var9, var13, var8, 16.0F);
               var1.rect(var13, var8, 16.0F, 16.0F, -1333032053);
            }
         }
      } else if (this.kind == 1) {
         var1.arrow(var4 + 94.0F, var5 + 39.0F, -7631989);
      } else if (this.kind == 5) {
         for (int i = 0; i < this.trades.length; i++) {
            float x0 = i < 3 ? 8.0F : 92.0F;
            float y0 = 18.0F + i % 3 * 18;
            boolean ok = Trades.has(this.g, this.trades[i]);
            var1.rect(var4 + x0 + 38.0F, var5 + y0 + 7.0F, 12.0F, 3.0F, ok ? -13730510 : -7631989);
            var1.rect(var4 + x0 + 46.0F, var5 + y0 + 4.0F, 3.0F, 9.0F, ok ? -13730510 : -7631989);
         }

         if (this.trades.length == 0) {
            var1.text("Hrmmm.", var4 + 60.0F, var5 + 40.0F, -12566464, false);
         }
      } else if (this.kind == 2) {
         float var10 = this.tile.cook / 200.0F;
         var1.arrow(var4 + 84.0F, var5 + 37.0F, -7631989, var10, -13730510);
         var1.flame(var4 + 57.0F, var5 + 36.0F, this.tile.burnMax > 0 && this.tile.burn > 0 ? (float)this.tile.burn / this.tile.burnMax : 0.0F);
      }

      for (Menu.Slot var15 : this.slots) {
         Stack var17 = this.stackIn(var15);
         if (var17 != null && (var15.type != 5 || this.page * 45 + var15.idx < this.palette.length)) {
            this.g.drawStack(var17, var4 + var15.x + 1.0F, var5 + var15.y + 1.0F);
         }
      }

      Menu.Slot var12 = this.at(var2, var3, var4, var5);
      if (var12 != null) {
         var1.rect(var4 + var12.x + 1.0F, var5 + var12.y + 1.0F, 16.0F, 16.0F, -2130706433);
      }

      if (this.g.cursor != null) {
         this.g.drawStack(this.g.cursor, var2 - 8.0F, var3 - 8.0F);
      } else if (var12 != null) {
         Stack var16 = this.stackIn(var12);
         if (var16 != null) {
            this.g.tooltip(var16, var2, var3);
         }
      } else if (this.kind == 4 && this.hoverTab >= 0) {
         this.g.tooltipLines(java.util.Arrays.asList(Creative.NAME[this.hoverTab]), var2, var3);
      }
   }

   static final class Slot {
      Stack[] arr;
      int idx;
      int type;
      float x;
      float y;
      int armorPiece;
   }
}
