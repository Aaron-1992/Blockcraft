package bc;

public final class TileEntity {
   public static final int CHEST = 0;
   public static final int FURNACE = 1;
   public final int type;
   public final int x;
   public final int y;
   public final int z;
   public final Stack[] slots;
   public int burn;
   public int burnMax;
   public int cook;

   public TileEntity(int var1, int var2, int var3, int var4) {
      this.type = var1;
      this.x = var2;
      this.y = var3;
      this.z = var4;
      this.slots = new Stack[var1 == 0 ? 27 : 3];
   }

   public boolean tickFurnace(World var1) {
      boolean var2 = this.burn > 0;
      Stack var3 = this.slots[0];
      Stack var4 = Recipes.smeltResult(var3);
      boolean var5 = false;
      if (var4 != null) {
         Stack var6 = this.slots[2];
         var5 = var6 == null || var6.same(var4) && var6.n + var4.n <= var6.max();
      }

      if (this.burn > 0) {
         this.burn--;
      }

      if (var5 && this.burn == 0) {
         int var7 = Recipes.fuelTime(this.slots[1]);
         if (var7 > 0) {
            this.burn = this.burnMax = var7;
            this.slots[1].n--;
            if (this.slots[1].n <= 0) {
               this.slots[1] = null;
            }
         }
      }

      if (this.burn > 0 && var5) {
         if (++this.cook >= 200) {
            this.cook = 0;
            if (this.slots[2] == null) {
               this.slots[2] = var4;
            } else {
               this.slots[2].n = this.slots[2].n + var4.n;
            }

            this.slots[0].n--;
            if (this.slots[0].n <= 0) {
               this.slots[0] = null;
            }
         }
      } else if (this.cook > 0) {
         this.cook = Math.max(0, this.cook - 2);
      }

      return var2 != this.burn > 0;
   }
}
