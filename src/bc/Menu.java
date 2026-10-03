package bc;

import java.util.ArrayList;

final class Menu {
   static final int PLAYER = 0;
   static final int CRAFT3 = 1;
   static final int FURNACE = 2;
   static final int CHEST = 3;
   static final int CREATIVE = 4;
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

   Menu(Game var1, int var2, TileEntity var3) {
      this.g = var1;
      this.kind = var2;
      this.tile = var3;
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
         default:
            this.title = "Creative";
            this.w = 195.0F;
            this.h = 136.0F;
            this.palette = Blocks.creativeList();
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
      return this.palette == null ? 1 : (this.palette.length + 44) / 45;
   }

   void changed() {
      if (this.grid != null) {
         this.out[0] = Recipes.find(this.grid, this.gw, this.gh);
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

   void draw(Gui var1, float var2, float var3, float var4, float var5) {
      var1.panel(var4, var5, this.w, this.h);
      if (this.kind == 4) {
         var1.text(this.title, var4 + 8.0F, var5 + 6.0F, -12566464, false);
      } else if (this.kind == 0) {
         var1.text(this.title, var4 + 97.0F, var5 + 6.0F, -12566464, false);
         var1.text("Inventory", var4 + 8.0F, var5 + 72.0F, -12566464, false);
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
         PlayerModel.drawLooking(this.g.skinPixels(), this.g.skinSlim(), var4 + 62.0F, var5 + 75.0F, 30.0F, var2, var3, (float)(System.nanoTime() / 1.0E9 % 100000.0));
         var1.rect(var4 + 136.0F, var5 + 33.0F, 12.0F, 3.0F, -7631989);
         var1.rect(var4 + 144.0F, var5 + 30.0F, 3.0F, 9.0F, -7631989);

         for (int var9 = 0; var9 < 4; var9++) {
            if (this.g.armor[var9] == null) {
               float var13 = var4 + 8.0F + 1.0F;
               float var8 = var5 + 8.0F + var9 * 18 + 1.0F;
               var1.rect(var13 + 4.0F, var8 + 4.0F, 8.0F, 8.0F, 805306368);
            }
         }
      } else if (this.kind == 1) {
         var1.rect(var4 + 90.0F, var5 + 41.0F, 22.0F, 3.0F, -7631989);
         var1.rect(var4 + 104.0F, var5 + 37.0F, 3.0F, 11.0F, -7631989);
      } else if (this.kind == 2) {
         float var10 = this.tile.cook / 200.0F;
         var1.rect(var4 + 79.0F, var5 + 35.0F, 24.0F, 16.0F, -7631989);
         var1.rect(var4 + 79.0F, var5 + 35.0F, 24.0F * var10, 16.0F, -13730510);
         if (this.tile.burnMax > 0 && this.tile.burn > 0) {
            int var14 = (int)(14.0F * this.tile.burn / this.tile.burnMax);
            var1.rect(var4 + 57.0F, var5 + 37.0F + (14 - var14), 14.0F, var14 + 1, -30208);
         } else {
            var1.rect(var4 + 57.0F, var5 + 37.0F, 14.0F, 14.0F, -9737365);
         }
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
