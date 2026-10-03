package bc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public final class Options {
   public int renderDistance = 8;
   public int fov = 70;
   public float sensitivity = 0.5F;
   public float gamma = 0.0F;
   public boolean smooth = true;
   /** Update 3.9: leaf faces inside the foliage - 0 fancy (all), 1 smart (default), 2 fast (none between leaves). */
   public int leaves = 1;
   public boolean clouds = true;
   public boolean vsync = true;
   public int guiScale = 0;
   public boolean fullscreen = false;
   public int lodDistance = 64;
   public boolean shaders = true;
   /** Update 3.8: rain, snow, thunderstorms and storm clouds (off = fair-weather clouds only). */
   public boolean weather = true;
   /** Update 3.10: natural disasters (tornadoes, earthquakes, meteors); /disaster on|off switches them per world as well. */
   public boolean disasters = true;
   /** Update 3.9: the chosen skin (a file name in the skins folder, or Skins.DEFAULT). */
   public String skin = Skins.DEFAULT;
   final File file;

   public Options(File var1) {
      this.file = new File(var1, "options.txt");
   }

   public void load() {
      if (this.file.exists()) {
         try {
            FileInputStream var1 = new FileInputStream(this.file);

            try {
               Properties var2 = new Properties();
               var2.load(var1);
               this.renderDistance = Integer.parseInt(var2.getProperty("renderDistance", "8"));
               this.fov = Integer.parseInt(var2.getProperty("fov", "70"));
               this.sensitivity = Float.parseFloat(var2.getProperty("sensitivity", "0.5"));
               this.gamma = Float.parseFloat(var2.getProperty("gamma", "0"));
               this.smooth = Boolean.parseBoolean(var2.getProperty("smooth", "true"));
               this.leaves = Math.max(0, Math.min(2, Integer.parseInt(var2.getProperty("leaves", "1"))));
               this.clouds = Boolean.parseBoolean(var2.getProperty("clouds", "true"));
               this.vsync = Boolean.parseBoolean(var2.getProperty("vsync", "true"));
               this.guiScale = Integer.parseInt(var2.getProperty("guiScale", "0"));
               this.fullscreen = Boolean.parseBoolean(var2.getProperty("fullscreen", "false"));
               this.lodDistance = Integer.parseInt(var2.getProperty("lodDistance", "64"));
               this.shaders = Boolean.parseBoolean(var2.getProperty("shaders", "true"));
               this.weather = Boolean.parseBoolean(var2.getProperty("weather", "true"));
               this.disasters = Boolean.parseBoolean(var2.getProperty("disasters", "true"));
               this.skin = var2.getProperty("skin", Skins.DEFAULT);
            } catch (Throwable var5) {
               try {
                  var1.close();
               } catch (Throwable var4) {
                  var5.addSuppressed(var4);
               }

               throw var5;
            }

            var1.close();
         } catch (Exception var6) {
            System.err.println("options load failed: " + var6);
         }
      }
   }

   public void save() {
      Properties var1 = new Properties();
      var1.setProperty("renderDistance", this.renderDistance + "");
      var1.setProperty("fov", this.fov + "");
      var1.setProperty("sensitivity", this.sensitivity + "");
      var1.setProperty("gamma", this.gamma + "");
      var1.setProperty("smooth", this.smooth + "");
      var1.setProperty("leaves", this.leaves + "");
      var1.setProperty("clouds", this.clouds + "");
      var1.setProperty("vsync", this.vsync + "");
      var1.setProperty("guiScale", this.guiScale + "");
      var1.setProperty("fullscreen", this.fullscreen + "");
      var1.setProperty("lodDistance", this.lodDistance + "");
      var1.setProperty("shaders", this.shaders + "");
      var1.setProperty("weather", this.weather + "");
      var1.setProperty("disasters", this.disasters + "");
      var1.setProperty("skin", this.skin);

      try {
         FileOutputStream var2 = new FileOutputStream(this.file);

         try {
            var1.store(var2, "Blockcraft options");
         } catch (Throwable var6) {
            try {
               var2.close();
            } catch (Throwable var5) {
               var6.addSuppressed(var5);
            }

            throw var6;
         }

         var2.close();
      } catch (IOException var7) {
         System.err.println("options save failed: " + var7);
      }
   }
}
