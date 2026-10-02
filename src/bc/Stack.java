package bc;

public final class Stack {
   public int id;
   public int n;
   public int dmg;

   public Stack(int var1, int var2) {
      this.id = var1;
      this.n = var2;
   }

   public Stack(int var1, int var2, int var3) {
      this(var1, var2);
      this.dmg = var3;
   }

   public Stack copy() {
      return new Stack(this.id, this.n, this.dmg);
   }

   public int max() {
      return Items.maxStack(this.id);
   }

   public boolean same(Stack var1) {
      return var1 != null && var1.id == this.id && var1.dmg == this.dmg;
   }

   public static boolean same(Stack var0, Stack var1) {
      return var0 != null && var0.same(var1);
   }

   public static int idOf(Stack var0) {
      return var0 == null ? 0 : var0.id;
   }
}
