package bc;

import java.io.File;
import java.io.PrintWriter;

public final class Main {
   public static void main(String[] var0) {
      try {
         if (var0.length == 1 && "--build-info".equals(var0[0])) {
            java.util.Properties receipt = new java.util.Properties();
            try (java.io.InputStream in = Main.class.getResourceAsStream("/bc-build.properties")) {
               if (in == null) throw new IllegalStateException("Build receipt missing: rebuild this executable.");
               receipt.load(in);
            }
            if (!Game.VERSION_STR.equals(receipt.getProperty("version"))) {
               throw new IllegalStateException("Build receipt and compiled game versions disagree.");
            }
            System.out.println("version=" + Game.VERSION_STR);
            System.out.println("source.sha256=" + receipt.getProperty("source.sha256"));
            System.out.println("default.texture.tileSize=" + TexPixel32.SIZE);
            System.out.println("gun.firstPersonYaw=" + WeaponPose.FIRST_PERSON_YAW);
            System.out.println("bow.model=pixel-sprite");
            System.out.println("hand.lighting=single-lightmap");
            return; // No window, GPU, native libraries, options or saved-world writes.
         }
         if (var0.length >= 1 && "--export-textures".equals(var0[0])) {
            if (var0.length > 2) throw new IllegalArgumentException("Usage: --export-textures [new-folder]");
            TextureEditor.export(java.nio.file.Path.of(var0.length == 2 ? var0[1] : "resourcepacks/MyTextures"));
            return;
         }
         new Game().run();
         // everything is saved and closed; do not let a lingering AWT thread (the skin file dialog) keep the process alive
         System.exit(0);
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
