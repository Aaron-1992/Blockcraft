package bc;

import java.io.File;
import java.io.PrintWriter;

public final class Main {
   public static void main(String[] var0) {
      try {
         new Game().run();
      } catch (Throwable var9) {
         Throwable var1 = var9;

         try {
            File var2 = new File(System.getProperty("user.home"), ".blockcraft112");
            var2.mkdirs();
            PrintWriter var3 = new PrintWriter(new File(var2, "crash.log"));

            try {
               var1.printStackTrace(var3);
            } catch (Throwable var7) {
               try {
                  var3.close();
               } catch (Throwable var6) {
                  var7.addSuppressed(var6);
               }

               throw var7;
            }

            var3.close();
         } catch (Exception var8) {
         }

         var9.printStackTrace();
         System.exit(1);
      }
   }
}
