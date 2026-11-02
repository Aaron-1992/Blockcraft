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
   /** Update 3.19: 3D crossed foliage fronds protruding from tree canopy leaf blocks. */
   public boolean bushyLeaves = true;
   /** Update 6.18: the switch that opens the Advanced Video tab in the in-game options. */
   public boolean debugVideo = false;
   /** Update 6.18: title-screen landscape. 0 = Classic, 1 = Coral Coast, 2 = Snowy Peaks, 3 = Cycle. */
   public int panorama = 0;
   /** Update 6.18: shadow-map edge length index into Screens.SHADOW_RES = {512, 1024, 2048}. */
   public int shadowRes = 2;
   /** Update 6.18: cloud render target divisor index into Screens.CLOUD_RES = {1, 2, 4}. */
   public int cloudRes = 1;
   /** Update 6.18: cloud raymarch step index into Screens.CLOUD_STEPS = {16, 32, 48, 64}. */
   public int cloudSteps = 2;
   /** Update 6.18: bloom quality: 0 = off, 1 = low (one blur round), 2 = full. */
   public int bloom = 2;
   /** Update 6.18: minimum frames between shadow-map / cloud-shadow rebuilds after terrain changed. */
   public int shadowInterval = 8;
   /** Update 6.18: rebuild the shadow map at least this often even when nothing changed. */
   public int shadowMaxAge = 150;
   /** Update 6.18: hard cap on live particles, so a fire or a huge explosion cannot stall a frame. */
   public int particleBudget = 500;
   /** Update 6.18: rebuild the atlas mipmaps while animated tiles change. Off saves a texture upload every fourth tick. */
   public boolean animatedMips = true;
   /** Update 6.18: 0 = automatic, otherwise the exact number of chunk generation threads. */
   public int genThreads = 0;
   /** Update 6.18: 0 = automatic, otherwise the exact number of mesh build threads. */
   public int meshWorkers = 0;
   /** Update 6.18: budget for loaded chunks, in megabytes of raw block data. */
   public int chunkCacheMb = 128;
   /** Update 6.18: maximum chunks unloaded in one frame (spikes come from unloading hundreds at once). */
   public int unloadBudget = 2;
   /** Update 6.18: 0 = no timed save, otherwise a save every N game ticks, spread over frames. */
   public int autosaveTicks = 1200;
   /** Update 6.18: write chunks on a low-priority thread instead of stopping the render thread. */
   public boolean asyncSaves = true;
   /** Update 6.19: moonlight casts real terrain/entity shadows at night. */
   public boolean moonShadows = true;
   /** Update 6.19: strength of moonlight (0.15..1.0); lower is a darker night with softer shadows. */
   public float moonBrightness = 0.45F;
   /** Update 6.19: mobs (and the third-person player) cast shadows into the sun/moon shadow map. */
   public boolean entityShadows = true;
   /** Update 6.19: the first-person hand uses the shader's sun/moon/fog/dynamic lighting, not a flat lightmap colour. */
   public boolean handLighting = true;
   /** Update 6.19: how quickly distant LOD fades into the fog (0.4 sharp/fast .. 2.0 gentle/slow). */
   public float lodFalloff = 1.0F;
   /** Update 6.19: fraction of the LOD horizon where the fade starts. */
   public float lodFogStart = 0.35F;
   /** Update 6.19: fraction of the LOD horizon where LOD is fully hidden by fog. */
   public float lodFogEnd = 1.0F;
   /** Update 6.19: LOD quadtree detail bias: 0 coarse/fastest, 1 normal, 2 fine. */
   public int lodDetail = 1;
   /** Update 6.19: maximum LOD vertex buffers uploaded to the GPU in one frame (spike control). */
   public int lodMaxUploads = 8;
   /** Update 6.19: maximum new LOD builds requested in one frame (spike control). */
   public int lodMaxRequests = 4;
   /** Update 6.19: maximum LOD tiles drawn in one frame; 0 = unlimited. */
   public int lodMaxDraws = 0;
   /** Update 6.19: LOD build threads, 0 = automatic. */
   public int lodWorkers = 0;
   /** Update 6.19: new worlds get the bigger cave systems and taller mountains. Existing saves keep their generator. */
   public boolean newTerrain = true;
   /** Update 6.19: cave size multiplier for new worlds (1.0 = old caves). */
   public float caveScale = 1.6F;
   /** Update 6.19: how much of the underground is hollowed out in new worlds. */
   public float caveDensity = 1.3F;
   /** Update 6.19: mountain height multiplier for new worlds. */
   public float mountainScale = 1.5F;
   /** Update 6.19: ravine width/depth multiplier for new worlds. */
   public float ravineScale = 1.3F;
   /** Active resource pack ID ("Default", "Classic", "Vibrant", "Retro8Bit", or a custom pack in resourcepacks/). */
   public String resourcePack = ResourcePacks.ID_DEFAULT;
   public float soundVolume = 1.0F;
   public float musicVolume = 0.75F;
   public boolean realAcoustics = true;
   /** False = random Minecraft music intervals; True = plays music continuously back-to-back. */
   public boolean constantMusic = false;
   final File file;

   public Options(File var1) {
      this.file = new File(var1, "options.txt");
   }

   /** Keeps a hand-edited or older options.txt value inside a slider's range. */
   public static int clamp(int value, int lo, int hi) {
      return value < lo ? lo : value > hi ? hi : value;
   }

   /** Keeps a hand-edited float inside a slider's range. */
   public static float clampf(float value, float lo, float hi) {
      return value < lo ? lo : value > hi ? hi : value;
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
               this.bushyLeaves = Boolean.parseBoolean(var2.getProperty("bushyLeaves", "true"));
               this.debugVideo = Boolean.parseBoolean(var2.getProperty("debugVideo", "false"));
               this.panorama = clamp(Integer.parseInt(var2.getProperty("panorama", "0")), 0, 3);
               this.shadowRes = clamp(Integer.parseInt(var2.getProperty("shadowRes", "2")), 0, 2);
               this.cloudRes = clamp(Integer.parseInt(var2.getProperty("cloudRes", "1")), 0, 2);
               this.cloudSteps = clamp(Integer.parseInt(var2.getProperty("cloudSteps", "2")), 0, 3);
               this.bloom = clamp(Integer.parseInt(var2.getProperty("bloom", "2")), 0, 2);
               this.shadowInterval = clamp(Integer.parseInt(var2.getProperty("shadowInterval", "8")), 1, 16);
               this.shadowMaxAge = clamp(Integer.parseInt(var2.getProperty("shadowMaxAge", "150")), 30, 1200);
               this.particleBudget = clamp(Integer.parseInt(var2.getProperty("particleBudget", "500")), 0, 2000);
               this.animatedMips = Boolean.parseBoolean(var2.getProperty("animatedMips", "true"));
               this.genThreads = clamp(Integer.parseInt(var2.getProperty("genThreads", "0")), 0, 8);
               this.meshWorkers = clamp(Integer.parseInt(var2.getProperty("meshWorkers", "0")), 0, 4);
               this.chunkCacheMb = clamp(Integer.parseInt(var2.getProperty("chunkCacheMb", "128")), 32, 2048);
               this.unloadBudget = clamp(Integer.parseInt(var2.getProperty("unloadBudget", "2")), 1, 32);
               this.autosaveTicks = clamp(Integer.parseInt(var2.getProperty("autosaveTicks", "1200")), 0, 7200);
               this.asyncSaves = Boolean.parseBoolean(var2.getProperty("asyncSaves", "true"));
               this.moonShadows = Boolean.parseBoolean(var2.getProperty("moonShadows", "true"));
               this.moonBrightness = clampf(Float.parseFloat(var2.getProperty("moonBrightness", "0.45")), 0.15F, 1.0F);
               this.entityShadows = Boolean.parseBoolean(var2.getProperty("entityShadows", "true"));
               this.handLighting = Boolean.parseBoolean(var2.getProperty("handLighting", "true"));
               this.lodFalloff = clampf(Float.parseFloat(var2.getProperty("lodFalloff", "1.0")), 0.4F, 2.0F);
               this.lodFogStart = clampf(Float.parseFloat(var2.getProperty("lodFogStart", "0.35")), 0.0F, 0.95F);
               this.lodFogEnd = clampf(Float.parseFloat(var2.getProperty("lodFogEnd", "1.0")), this.lodFogStart + 0.05F, 1.0F);
               this.lodDetail = clamp(Integer.parseInt(var2.getProperty("lodDetail", "1")), 0, 2);
               this.lodMaxUploads = clamp(Integer.parseInt(var2.getProperty("lodMaxUploads", "8")), 1, 64);
               this.lodMaxRequests = clamp(Integer.parseInt(var2.getProperty("lodMaxRequests", "4")), 1, 32);
               this.lodMaxDraws = clamp(Integer.parseInt(var2.getProperty("lodMaxDraws", "0")), 0, 4096);
               this.lodWorkers = clamp(Integer.parseInt(var2.getProperty("lodWorkers", "0")), 0, 4);
               this.newTerrain = Boolean.parseBoolean(var2.getProperty("newTerrain", "true"));
               this.caveScale = clampf(Float.parseFloat(var2.getProperty("caveScale", "1.6")), 0.6F, 3.0F);
               this.caveDensity = clampf(Float.parseFloat(var2.getProperty("caveDensity", "1.3")), 0.4F, 2.5F);
               this.mountainScale = clampf(Float.parseFloat(var2.getProperty("mountainScale", "1.5")), 0.5F, 2.5F);
               this.ravineScale = clampf(Float.parseFloat(var2.getProperty("ravineScale", "1.3")), 0.4F, 2.5F);
               this.resourcePack = var2.getProperty("resourcePack", ResourcePacks.ID_DEFAULT);
               this.soundVolume = Math.max(0.0F, Math.min(1.0F, Float.parseFloat(var2.getProperty("soundVolume", "1.0"))));
               this.musicVolume = Math.max(0.0F, Math.min(1.0F, Float.parseFloat(var2.getProperty("musicVolume", "0.75"))));
               this.realAcoustics = Boolean.parseBoolean(var2.getProperty("realAcoustics", "true"));
               this.constantMusic = Boolean.parseBoolean(var2.getProperty("constantMusic", "false"));
               ResourcePacks.setActiveId(this.resourcePack);
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
      var1.setProperty("bushyLeaves", this.bushyLeaves + "");
      var1.setProperty("debugVideo", this.debugVideo + "");
      var1.setProperty("panorama", this.panorama + "");
      var1.setProperty("shadowRes", this.shadowRes + "");
      var1.setProperty("cloudRes", this.cloudRes + "");
      var1.setProperty("cloudSteps", this.cloudSteps + "");
      var1.setProperty("bloom", this.bloom + "");
      var1.setProperty("shadowInterval", this.shadowInterval + "");
      var1.setProperty("shadowMaxAge", this.shadowMaxAge + "");
      var1.setProperty("particleBudget", this.particleBudget + "");
      var1.setProperty("animatedMips", this.animatedMips + "");
      var1.setProperty("genThreads", this.genThreads + "");
      var1.setProperty("meshWorkers", this.meshWorkers + "");
      var1.setProperty("chunkCacheMb", this.chunkCacheMb + "");
      var1.setProperty("unloadBudget", this.unloadBudget + "");
      var1.setProperty("autosaveTicks", this.autosaveTicks + "");
      var1.setProperty("asyncSaves", this.asyncSaves + "");
      var1.setProperty("moonShadows", this.moonShadows + "");
      var1.setProperty("moonBrightness", this.moonBrightness + "");
      var1.setProperty("entityShadows", this.entityShadows + "");
      var1.setProperty("handLighting", this.handLighting + "");
      var1.setProperty("lodFalloff", this.lodFalloff + "");
      var1.setProperty("lodFogStart", this.lodFogStart + "");
      var1.setProperty("lodFogEnd", this.lodFogEnd + "");
      var1.setProperty("lodDetail", this.lodDetail + "");
      var1.setProperty("lodMaxUploads", this.lodMaxUploads + "");
      var1.setProperty("lodMaxRequests", this.lodMaxRequests + "");
      var1.setProperty("lodMaxDraws", this.lodMaxDraws + "");
      var1.setProperty("lodWorkers", this.lodWorkers + "");
      var1.setProperty("newTerrain", this.newTerrain + "");
      var1.setProperty("caveScale", this.caveScale + "");
      var1.setProperty("caveDensity", this.caveDensity + "");
      var1.setProperty("mountainScale", this.mountainScale + "");
      var1.setProperty("ravineScale", this.ravineScale + "");
      var1.setProperty("resourcePack", this.resourcePack != null ? this.resourcePack : ResourcePacks.ID_DEFAULT);
      var1.setProperty("soundVolume", this.soundVolume + "");
      var1.setProperty("musicVolume", this.musicVolume + "");
      var1.setProperty("realAcoustics", this.realAcoustics + "");
      var1.setProperty("constantMusic", this.constantMusic + "");

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
