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
   /** Update 3.11: the first-person hand bobs with every step (Minecraft's view bobbing); /bobbing on|off. */
   public boolean bobbing = true;
   /** Update 3.13: moving the mouse up looks down. */
   public boolean invertY = false;
   public boolean shaders = true;
   /** Update 3.11: water with shaders - 1 = depth colour, refraction, foam; 2 = also the terrain mirrored in it (the "FANCY" setting, default). */
   public int waterQ = 2;
   /** Update 3.8: rain, snow, thunderstorms and storm clouds (off = fair-weather clouds only). */
   public boolean weather = true;
   /** Update 3.10: natural disasters (tornadoes, earthquakes, meteors); /disaster on|off switches them per world as well. */
   public boolean disasters = true;
   /** Update 3.9: the chosen skin (a file name in the skins folder, or Skins.DEFAULT). */
   public String skin = Skins.DEFAULT;
   /** Update 3.18: ask Windows to draw the game with the high performance graphics chip (the real graphics card, not the one inside the processor); see {@link GpuPref}. */
   public boolean gpuHigh = true;
   /** Update 3.18: the 3D world is drawn at this percentage of the window's size and stretched (50..100; needs the shaders: the menus and the text stay sharp). */
   public int renderScale = 100;
   /** Update 3.18: frame rate limit (0 = none: the screen's refresh rate with VSync); lets a fast graphics card rest instead of drawing 400 pictures a second. */
   public int maxFps = 0;
   /** Update 3.19: the colour grading of the shader pack (see {@link Look}). */
   public int look = Look.DEFAULT;
   /** Update 3.19: how many of the new shader effects are on - 0 = LOW (none: the picture of 3.18), 1 = MEDIUM (glowing lava and torches, cloud shadows, wet ground in rain), 2 = HIGH (also soft contact shadows in corners). */
   public int fx = 1;
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
               this.bobbing = Boolean.parseBoolean(var2.getProperty("bobbing", "true"));
               this.invertY = Boolean.parseBoolean(var2.getProperty("invertY", "false"));
               this.shaders = Boolean.parseBoolean(var2.getProperty("shaders", "true"));
               this.waterQ = Math.max(1, Math.min(2, Integer.parseInt(var2.getProperty("waterQ", "2"))));
               this.weather = Boolean.parseBoolean(var2.getProperty("weather", "true"));
               this.disasters = Boolean.parseBoolean(var2.getProperty("disasters", "true"));
               this.skin = var2.getProperty("skin", Skins.DEFAULT);
               this.gpuHigh = Boolean.parseBoolean(var2.getProperty("gpuHigh", "true"));
               this.renderScale = Math.max(50, Math.min(100, Integer.parseInt(var2.getProperty("renderScale", "100"))));
               this.maxFps = Math.max(0, Math.min(1000, Integer.parseInt(var2.getProperty("maxFps", "0"))));
               this.look = Look.clamp(Integer.parseInt(var2.getProperty("look", Look.DEFAULT + "")));
               this.fx = Math.max(0, Math.min(2, Integer.parseInt(var2.getProperty("fx", "1"))));
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
      var1.setProperty("bobbing", this.bobbing + "");
      var1.setProperty("invertY", this.invertY + "");
      var1.setProperty("shaders", this.shaders + "");
      var1.setProperty("waterQ", this.waterQ + "");
      var1.setProperty("weather", this.weather + "");
      var1.setProperty("disasters", this.disasters + "");
      var1.setProperty("skin", this.skin);
      var1.setProperty("gpuHigh", this.gpuHigh + "");
      var1.setProperty("renderScale", this.renderScale + "");
      var1.setProperty("maxFps", this.maxFps + "");
      var1.setProperty("look", this.look + "");
      var1.setProperty("fx", this.fx + "");

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
