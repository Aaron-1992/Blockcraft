package bc;

import bc.Chunk.Section;
import bc.Menu.Slot;
import bc.Mobs.Mob;
import bc.World.Hit;
import bc.World.Item;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;
import java.util.Random;
import java.util.function.Consumer;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.system.MemoryUtil;

public final class Game {
   public static final String TITLE_STR = "Blockcraft 1.12 - Update 3.8";
   /** "3.8": the version without the words, for the title screen and the F3 screen. */
   public static final String VERSION_STR = TITLE_STR.substring(TITLE_STR.indexOf("Update ") + 7);
   public Options opt;
   public World world;
   public Player player;
   public Renderer rend;
   final Gui gui = new Gui();
   long window;
   int winW = 1280;
   int winH = 720;
   int fbW = 1280;
   int fbH = 720;
   File root;
   Game.S screen = Game.S.TITLE;
   Game.S optionsReturn = Game.S.TITLE;
   final Stack[] inv = new Stack[36];
   final Stack[] armor = new Stack[4];
   Stack cursor;
   final Mobs mobs = new Mobs(this);
   Mob targetMob;
   double targetMobT;
   Menu menu;
   int eatTicks;
   int atkTicks = 20;
   int leftClickCounter;
   boolean leftPressed;
   boolean rightPressed;
   int bowCharge;
   boolean deathHandled;
   int sel = 0;
   float nameTimer;
   public Hit target;
   float breakProg;
   int breakX;
   int breakY;
   int breakZ;
   int breakDelay;
   int placeDelay;
   float swing;
   float swingPrev;
   boolean swinging;
   boolean leftDown;
   boolean rightDown;
   boolean hideGui;
   boolean debug;
   float partial;
   double acc;
   double mouseX;
   double mouseY;
   double lastMX;
   double lastMY;
   boolean grabbed;
   boolean firstMouse = true;
   long lastWPress;
   long lastSpacePress;
   boolean sprintToggle;
   int tickCount;
   int fps;
   int frames;
   double fpsTimer;
   String seedField = "";
   int newMode = 1;
   boolean needSpawn;
   int[] spawnXZ = new int[]{0, 0};
   int[] bedPos = null;
   double[] spawnPos = new double[]{0.0, 80.0, 0.0};
   final StringBuilder chat = new StringBuilder();
   long chatOpenedAt;
   final ArrayList<String> chatLog = new ArrayList();
   final ArrayList<Long> chatTimes = new ArrayList();
   final ArrayList<Game.Particle> particles = new ArrayList();
   final Random rnd = new Random();
   String splash;
   static final String[] SPLASHES = new String[]{
      "Also try voxels!",
      "Now with caves!",
      "100% blocky!",
      "Java powered!",
      "Mind the creepers!",
      "Made of cubes!",
      "1.12 style!",
      "Infinite worlds!",
      "Pickaxe not included!",
      "Hello Dornbirn!",
      "Stay a while and dig!",
      "Torches are great!"
   };
   String loadingMsg = "";
   ArrayList<Game.Btn> btns = new ArrayList();
   ArrayList<Game.Btn> activeBtns = new ArrayList();
   int dragging = -1;
   int autoFrames;
   int autoShots;
   boolean autoShotNow;
   String worldName = "world1";
   String newName = "";
   int focusField = 1;
   int worldScroll = 0;
   boolean confirmDelete = false;
   ArrayList<Game.WInfo> worldList = new ArrayList();
   String worldDisplayName = "World";
   public float zoomAmt;
   public float zoomFactor = 4.0F;
   long zoomLast = System.nanoTime();
   long lastWorldClick = 0L;
   static final String[] DIRS = new String[]{
      "south (Towards positive Z)", "west (Towards negative X)", "north (Towards negative Z)", "east (Towards positive X)"
   };

   public void run() throws Exception {
      this.root = new File(System.getProperty("user.home"), ".blockcraft112");
      this.root.mkdirs();

      try {
         PrintStream var1 = new PrintStream(new FileOutputStream(new File(this.root, "latest.log")), true);
         System.setOut(var1);
         System.setErr(var1);
      } catch (Exception var6) {
      }

      System.setProperty("java.awt.headless", "true");
      this.opt = new Options(this.root);
      this.opt.load();
      if (System.getProperty("bc.rd") != null) {
         this.opt.renderDistance = Integer.getInteger("bc.rd");
      }

      if (System.getProperty("bc.lod") != null) {
         this.opt.lodDistance = Integer.getInteger("bc.lod");
      }

      if (System.getProperty("bc.shaders") != null) {
         this.opt.shaders = Boolean.getBoolean("bc.shaders");
      }

      this.init();

      try {
         this.loop();
      } finally {
         this.shutdown();
      }
   }

   void init() {
      GLFWErrorCallback.createPrint(System.err).set();
      if (!GLFW.glfwInit()) {
         throw new IllegalStateException("Unable to initialize GLFW");
      }

      GLFW.glfwDefaultWindowHints();
      GLFW.glfwWindowHint(131076, 0);
      GLFW.glfwWindowHint(131075, 1);
      GLFW.glfwWindowHint(135173, 24);
      this.window = GLFW.glfwCreateWindow(this.winW, this.winH, TITLE_STR, 0L, 0L);
      if (this.window == 0L) {
         throw new RuntimeException("Failed to create the GLFW window (OpenGL 2.1+ required)");
      }

      GLFWVidMode var1 = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());
      if (var1 != null) {
         GLFW.glfwSetWindowPos(this.window, (var1.width() - this.winW) / 2, (var1.height() - this.winH) / 2);
      }

      GLFW.glfwMakeContextCurrent(this.window);
      GLFW.glfwSwapInterval(this.opt.vsync ? 1 : 0);
      GL.createCapabilities();
      GLFW.glfwShowWindow(this.window);
      System.out.println("GL: " + GL11.glGetString(7938) + " / " + GL11.glGetString(7937));
      GLFW.glfwSetKeyCallback(this.window, (var1x, var3x, var4, var5, var6) -> this.onKey(var3x, var5, var6));
      GLFW.glfwSetCharCallback(this.window, (var1x, var3x) -> this.onChar(var3x));
      GLFW.glfwSetMouseButtonCallback(this.window, (var1x, var3x, var4, var5) -> this.onMouse(var3x, var4));
      GLFW.glfwSetScrollCallback(this.window, (var1x, var3x, var5) -> this.onScroll(var5));
      GLFW.glfwSetCursorPosCallback(this.window, (var1x, var3x, var5) -> this.onCursor(var3x, var5));
      GLFW.glfwSetFramebufferSizeCallback(this.window, (var1x, var3x, var4) -> {
         this.fbW = Math.max(1, var3x);
         this.fbH = Math.max(1, var4);
      });
      GLFW.glfwSetWindowSizeCallback(this.window, (var1x, var3x, var4) -> {
         this.winW = Math.max(1, var3x);
         this.winH = Math.max(1, var4);
      });
      GLFW.glfwSetWindowFocusCallback(this.window, (var1x, var3x) -> {
         if (!var3x && this.screen == Game.S.PLAY) {
            this.setScreen(Game.S.PAUSE);
         }
      });
      int[] var2 = new int[1];
      int[] var3 = new int[1];
      GLFW.glfwGetFramebufferSize(this.window, var2, var3);
      this.fbW = Math.max(1, var2[0]);
      this.fbH = Math.max(1, var3[0]);
      GLFW.glfwGetWindowSize(this.window, var2, var3);
      this.winW = Math.max(1, var2[0]);
      this.winH = Math.max(1, var3[0]);
      Tex.init();
      this.rend = new Renderer(this);
      this.rend.sh = new Shaders();
      this.rend.sh.init();
      this.splash = SPLASHES[this.rnd.nextInt(SPLASHES.length)];
      if (this.opt.fullscreen) {
         this.setFullscreen(true);
      }

      if (System.getProperty("bc.auto") != null && System.getProperty("bc.title") == null) {
         this.startWorld(true, System.getProperty("bc.seed", "12345"), Integer.parseInt(System.getProperty("bc.mode", "1")));
      }
   }

   /** Weather test hooks: bc.weather=clear|rain|thunder|cell:dx,dz,radius  bc.wtime=<weather ticks>  bc.biome=<biome id for all loaded chunks> */
   void autoWeather() {
      Weather w = this.world.weather;
      w.natural();
      w.calmUntil = 0L;
      w.enabled = true;
      String t = System.getProperty("bc.wtime");
      if (t != null) {
         w.ticks = Long.parseLong(t);
      }

      String m = System.getProperty("bc.weather");
      if (m != null) {
         if (m.equals("clear")) {
            w.force(1, 99999L);
            w.instant();
         } else if (m.equals("rain")) {
            w.force(2, 99999L);
            w.instant();
         } else if (m.equals("thunder")) {
            w.force(3, 99999L);
            w.instant();
         } else if (m.equals("calm")) {
            w.calm = true;
         } else if (m.startsWith("cell:")) {
            String[] c = m.substring(5).split(",");
            w.calm = true;
            w.placeStorm(this.player.x + Double.parseDouble(c[0]), this.player.z + Double.parseDouble(c[1]), Float.parseFloat(c[2]));
         }
      }

      w.noLightning = "off".equals(System.getProperty("bc.lightning"));
      String biome = System.getProperty("bc.biome");
      if (biome != null) {
         for (Chunk c : this.world.chunks.values()) {
            Arrays.fill(c.biome, (byte)Integer.parseInt(biome));
         }
      }
   }

   void autoTest() {
      String var1 = System.getProperty("bc.title");
      if (var1 != null) {
         if (++this.autoFrames == 40) {
            this.setScreen(Game.S.valueOf(var1));
         }

         if (this.autoFrames == 60) {
            this.autoShotNow = true;
         }
      } else if (this.screen == Game.S.PLAY || this.screen == Game.S.INV) {
         String var2 = System.getProperty("bc.cam");
         this.autoFrames++;
         if (this.autoFrames == 1) {
            if (var2 != null) {
               String[] var3 = var2.split(",");
               this.player.setPos(Double.parseDouble(var3[0]), Double.parseDouble(var3[1]), Double.parseDouble(var3[2]));
               this.player.yaw = Float.parseFloat(var3[3]);
               this.player.pitch = Float.parseFloat(var3[4]);
               this.player.flying = true;
            }

            String var10 = System.getProperty("bc.time");
            if (var10 != null) {
               this.world.dayTime = Long.parseLong(var10);
            }

            if (System.getProperty("bc.debug") != null) {
               this.debug = true;
            }

            if ("off".equals(System.getProperty("bc.fire"))) {
               this.world.fire.spread = false; // screenshots: fires neither burn out nor spread
            }

            if (System.getProperty("bc.weather") != null || System.getProperty("bc.wtime") != null || System.getProperty("bc.biome") != null) {
               this.autoWeather();
            }
         }

         if (this.autoFrames >= Integer.getInteger("bc.shot", 100) - 4 && System.getProperty("bc.bolt") != null) {
            // a fresh lightning bolt every frame so the screenshot catches the blinding first stroke
            String[] b = System.getProperty("bc.bolt").split(",");
            this.world.weather.bolts.clear();
            this.world.weather.strike(this.player.x + Double.parseDouble(b[0]), this.player.z + Double.parseDouble(b[1]), false);
         }

         if (this.autoFrames == 12 && System.getProperty("bc.hold") != null) {
            this.inv[this.sel] = new Stack(Integer.getInteger("bc.hold"), 64);
         }

         if (this.autoFrames == 10 && System.getProperty("bc.kit") != null) {
            this.addItem(5, 20);
            this.addItem(256, 10);
            this.addItem(4, 30);
            this.addItem(Items.toolId(2, 1), 1);
            this.addItem(Items.armorId(1, 1), 1);
            this.addItem(257, 5);
            this.addItem(273, 8);
            this.addItem(Items.toolId(4, 4), 1);
            this.addItem(281, 3);
            this.sel = Integer.getInteger("bc.sel", 0);
         }

         if (this.autoFrames == 70 && System.getProperty("bc.set") != null) {
            for (String var6 : System.getProperty("bc.set").split(";")) {
               String[] var7 = var6.split(",");
               this.world.setBlock(Integer.parseInt(var7[0]), Integer.parseInt(var7[1]), Integer.parseInt(var7[2]), Integer.parseInt(var7[3]));
            }
         }

         if (this.autoFrames == 80 && System.getProperty("bc.toggle") != null) {
            String[] var12 = System.getProperty("bc.toggle").split(",");
            this.toggleDoor(Integer.parseInt(var12[0]), Integer.parseInt(var12[1]), Integer.parseInt(var12[2]));
         }

         if (this.autoFrames == 80 && System.getProperty("bc.tnt") != null) {
            String[] var13 = System.getProperty("bc.tnt").split(",");
            int var16 = Integer.parseInt(var13[0]);
            int var18 = Integer.parseInt(var13[1]);
            int var19 = Integer.parseInt(var13[2]);
            this.world.setBlock(var16, var18, var19, 0);
            this.world.primeTnt(var16, var18, var19, 50);
         }

         if (this.autoFrames == 60 && System.getProperty("bc.kill") != null) {
            this.runCommand("/kill");
         }

         if (this.autoFrames == 60 && System.getProperty("bc.summon") != null) {
            // names: pig,cow,sheep,... ; prefix "baby:" for a baby, "sheared:" for a sheared sheep (test hooks)
            String[] var14 = System.getProperty("bc.summon").split(",");
            double var17 = Math.toRadians(this.player.yaw);
            double summonDist = Double.parseDouble(System.getProperty("bc.summonDist", "3.5"));
            double summonDy = Double.parseDouble(System.getProperty("bc.summonDy", "-3.0"));

            for (int var20 = 0; var20 < var14.length; var20++) {
               String mobName = var14[var20];
               boolean babyMob = mobName.startsWith("baby:");
               boolean shearedMob = mobName.startsWith("sheared:");
               if (babyMob) {
                  mobName = mobName.substring(5);
               }

               if (shearedMob) {
                  mobName = mobName.substring(8);
               }

               for (int var21 = 0; var21 < 8; var21++) {
                  if (Mobs.NAME[var21].equals(mobName)) {
                     double var8 = (var20 - (var14.length - 1) / 2.0) * 1.6;
                     double mx = this.player.x - Math.sin(var17) * summonDist + Math.cos(var17) * var8;
                     double my = this.player.y + summonDy;
                     double mz = this.player.z + Math.cos(var17) * summonDist + Math.sin(var17) * var8;
                     Mob spawned = babyMob ? this.mobs.spawnBaby(var21, mx, my, mz) : this.mobs.spawn(var21, mx, my, mz);
                     spawned.sheared = shearedMob;
                     spawned.yaw = this.player.yaw + 180.0F - 30.0F;
                  }
               }
            }
         }

         if (this.autoFrames == 100 && System.getProperty("bc.inv") != null) {
            String var15 = System.getProperty("bc.inv");
            if (var15.equals("craft")) {
               this.openMenu(1, null);
            } else if (var15.equals("player")) {
               this.openMenu(0, null);
            } else if (var15.equals("furnace")) {
               this.openMenu(2, new TileEntity(1, 0, 0, 0));
            } else if (var15.equals("chest")) {
               this.openMenu(3, new TileEntity(0, 0, 0, 0));
            } else {
               this.openMenu(this.player.creative ? 4 : 0, null);
            }

            if (this.menu != null) {
               this.menu.page = Math.min(this.menu.pages() - 1, Integer.getInteger("bc.page", 0));
            }
         }

         if (this.autoFrames == Integer.getInteger("bc.shot", 120)) {
            this.autoShotNow = true;
         }
      }
   }

   void shutdown() {
      try {
         if (this.world != null) {
            this.saveWorld();
         }
      } catch (Throwable var2) {
         var2.printStackTrace();
      }

      this.opt.save();
      if (this.rend != null) {
         this.rend.shutdown();
      }

      if (this.world != null) {
         this.world.shutdown();
      }

      GLFW.glfwDestroyWindow(this.window);
      GLFW.glfwTerminate();
   }

   void setFullscreen(boolean var1) {
      this.opt.fullscreen = var1;
      long var2 = GLFW.glfwGetPrimaryMonitor();
      GLFWVidMode var4 = GLFW.glfwGetVideoMode(var2);
      if (var1) {
         GLFW.glfwSetWindowMonitor(this.window, var2, 0, 0, var4.width(), var4.height(), var4.refreshRate());
      } else {
         GLFW.glfwSetWindowMonitor(this.window, 0L, (var4.width() - 1280) / 2, (var4.height() - 720) / 2, 1280, 720, 0);
      }

      GLFW.glfwSwapInterval(this.opt.vsync ? 1 : 0);
   }

   File worldDir() {
      return new File(this.root, "saves/" + this.worldName);
   }

   boolean hasSave() {
      return !this.worldList.isEmpty() || new File(this.worldDir(), "level.properties").exists();
   }

   void scanWorlds() {
      this.worldList.clear();
      File[] var1 = new File(this.root, "saves").listFiles();
      if (var1 != null) {
         for (File var5 : var1) {
            File var6 = new File(var5, "level.properties");
            if (var5.isDirectory() && var6.exists()) {
               Game.WInfo var7 = new Game.WInfo();
               var7.dir = var5.getName();
               var7.name = var5.getName();
               var7.mod = var6.lastModified();
               Properties var8 = new Properties();

               try {
                  FileInputStream var9 = new FileInputStream(var6);

                  try {
                     var8.load(var9);
                     if (var8.getProperty("name") != null) {
                        var7.name = var8.getProperty("name");
                     }

                     var7.mode = (int)d(var8, "mode", Boolean.parseBoolean(var8.getProperty("creative", "true")) ? 1.0 : 0.0);
                  } catch (Throwable var13) {
                     try {
                        var9.close();
                     } catch (Throwable var12) {
                        var13.addSuppressed(var12);
                     }

                     throw var13;
                  }

                  var9.close();
               } catch (Exception var14) {
               }

               this.worldList.add(var7);
            }
         }

         this.worldList.sort((var0, var1x) -> Long.compare(var1x.mod, var0.mod));
         boolean var15 = false;

         for (Game.WInfo var17 : this.worldList) {
            if (var17.dir.equals(this.worldName)) {
               var15 = true;
            }
         }

         if (!var15 && !this.worldList.isEmpty()) {
            this.worldName = ((Game.WInfo)this.worldList.get(0)).dir;
         }
      }
   }

   String uniqueDir(String var1) {
      String var2 = var1.trim().replaceAll("[^A-Za-z0-9 _-]", "").trim();
      if (var2.isEmpty()) {
         var2 = "New World";
      }

      String var3 = var2;
      int var4 = 2;

      while (new File(this.root, "saves/" + var3).exists()) {
         var3 = var2 + " (" + var4++ + ")";
      }

      return var3;
   }

   static void deleteDir(File var0) {
      File[] var1 = var0.listFiles();
      if (var1 != null) {
         for (File var5 : var1) {
            if (var5.isDirectory()) {
               deleteDir(var5);
            } else {
               var5.delete();
            }
         }
      }

      var0.delete();
   }

   void startWorld(boolean var1, String var2, int var3) {
      Properties var4 = new Properties();
      String var5 = this.newName;
      if (var1) {
         this.worldName = this.uniqueDir(this.newName.trim().isEmpty() ? "New World" : this.newName);
         var5 = this.newName.trim().isEmpty() ? "New World" : this.newName.trim();
      }

      File var8 = this.worldDir();
      long var6;
      if (var1) {
         deleteDir(var8);
         this.worldDisplayName = var5;
         if (var2 != null && !var2.trim().isEmpty()) {
            try {
               var6 = Long.parseLong(var2.trim());
            } catch (NumberFormatException var15) {
               var6 = var2.trim().hashCode();
            }
         } else {
            var6 = new Random().nextLong();
         }
      } else {
         try {
            FileInputStream var9 = new FileInputStream(new File(var8, "level.properties"));

            try {
               var4.load(var9);
            } catch (Throwable var13) {
               try {
                  var9.close();
               } catch (Throwable var12) {
                  var13.addSuppressed(var12);
               }

               throw var13;
            }

            var9.close();
         } catch (Exception var14) {
            var14.printStackTrace();
         }

         var6 = Long.parseLong(var4.getProperty("seed", "0"));
         this.worldDisplayName = var4.getProperty("name", this.worldName);
      }

      var8.mkdirs();
      this.world = new World(var6, var8);
      this.world.game = this;
      this.rend.setLod(this.world);
      this.mobs.clear();
      this.targetMob = null;
      this.world.unloadHook = this.rend::onUnload;
      this.player = new Player();
      Arrays.fill(this.inv, null);
      Arrays.fill(this.armor, null);
      this.cursor = null;
      this.menu = null;
      this.eatTicks = 0;
      this.bowCharge = 0;
      this.deathHandled = false;
      this.particles.clear();
      this.chatLog.clear();
      this.chatTimes.clear();
      this.bedPos = null;
      if (var1) {
         this.player.setMode(var3);
         this.world.dayTime = 1000L;
         this.world.weather.startFair();
         int[] var16 = this.world.gen.findSpawn();
         this.spawnXZ = new int[]{var16[0], var16[2]};
         this.player.setPos(var16[0] + 0.5, 200.0, var16[2] + 0.5);
         this.needSpawn = true;
         if (var3 == 1) {
            int[] var10 = new int[]{1, 4, 5, 3, 15, 32, 9, 25, 7};

            for (int var11 = 0; var11 < var10.length; var11++) {
               this.inv[var11] = new Stack(var10[var11], 64);
            }
         }

         this.sel = 0;
      } else {
         int var17 = (int)d(var4, "mode", Boolean.parseBoolean(var4.getProperty("creative", "true")) ? 1.0 : 0.0);
         this.player.setMode(var17);
         this.player.setPos(d(var4, "x", 0.5), d(var4, "y", 90.0), d(var4, "z", 0.5));
         this.player.yaw = (float)d(var4, "yaw", 0.0);
         this.player.pitch = (float)d(var4, "pitch", 0.0);
         this.player.health = (int)d(var4, "health", 20.0);
         this.player.food = (int)d(var4, "food", 20.0);
         this.player.sat = (float)d(var4, "sat", 5.0);
         this.player.flying = Boolean.parseBoolean(var4.getProperty("flying", "false")) || var17 == 3;
         this.world.dayTime = (long)d(var4, "time", 1000.0);
         this.restoreFireSetting(var4);
         this.spawnXZ = new int[]{(int)d(var4, "spawnx", 0.0), (int)d(var4, "spawnz", 0.0)};
         this.bedPos = var4.getProperty("bedy") == null ? null : new int[]{(int)d(var4, "bedx", 0.0), (int)d(var4, "bedy", 0.0), (int)d(var4, "bedz", 0.0)};
         this.sel = (int)d(var4, "sel", 0.0);
         parseStacks(var4.getProperty("inv", ""), this.inv);
         parseStacks(var4.getProperty("armor", ""), this.armor);
         this.mobs.load(var8);
         this.needSpawn = false;
      }

      this.player.updateBox();
      this.setScreen(Game.S.LOADING);
   }

   static void parseStacks(String var0, Stack[] var1) {
      String[] var2 = var0.split(";");

      for (int var3 = 0; var3 < var2.length && var3 < var1.length; var3++) {
         String[] var4 = var2[var3].split(":");

         try {
            if (var4.length >= 2 && Integer.parseInt(var4[0]) > 0) {
               var1[var3] = new Stack(Integer.parseInt(var4[0]), Integer.parseInt(var4[1]), var4.length > 2 ? Integer.parseInt(var4[2]) : 0);
            }
         } catch (NumberFormatException var6) {
         }
      }
   }

   static String stacksToString(Stack[] var0) {
      StringBuilder var1 = new StringBuilder();

      for (Stack var5 : var0) {
         if (var5 == null) {
            var1.append("0:0:0;");
         } else {
            var1.append(var5.id).append(':').append(var5.n).append(':').append(var5.dmg).append(';');
         }
      }

      return var1.toString();
   }

   /** /firespread is saved with the world (level.properties) so a world where fire was switched off stays safe. */
   void storeFireSetting(Properties p) {
      p.setProperty("firespread", this.world.fire.spread + "");
      this.world.weather.save(p);
   }

   void restoreFireSetting(Properties p) {
      this.world.fire.spread = !"false".equals(p.getProperty("firespread"));
      this.world.weather.load(p);
   }

   static double d(Properties var0, String var1, double var2) {
      try {
         return Double.parseDouble(var0.getProperty(var1, var2 + ""));
      } catch (Exception var5) {
         return var2;
      }
   }

   void saveWorld() {
      if (this.world != null) {
         this.world.saveAll();
         this.mobs.save(this.worldDir());
         Properties var1 = new Properties();
         var1.setProperty("seed", this.world.seed + "");
         var1.setProperty("name", this.worldDisplayName);
         var1.setProperty("x", this.player.x + "");
         var1.setProperty("y", this.player.y + "");
         var1.setProperty("z", this.player.z + "");
         var1.setProperty("yaw", this.player.yaw + "");
         var1.setProperty("pitch", this.player.pitch + "");
         var1.setProperty("health", Math.max(1, this.player.health) + "");
         var1.setProperty("creative", this.player.creative + "");
         var1.setProperty("mode", this.player.gameMode + "");
         var1.setProperty("food", this.player.food + "");
         var1.setProperty("sat", this.player.sat + "");
         var1.setProperty("flying", this.player.flying + "");
         var1.setProperty("time", this.world.dayTime + "");
         var1.setProperty("spawnx", this.spawnXZ[0] + "");
         var1.setProperty("spawnz", this.spawnXZ[1] + "");
         this.storeFireSetting(var1);
         if (this.bedPos != null) {
            var1.setProperty("bedx", this.bedPos[0] + "");
            var1.setProperty("bedy", this.bedPos[1] + "");
            var1.setProperty("bedz", this.bedPos[2] + "");
         }

         var1.setProperty("sel", this.sel + "");
         var1.setProperty("inv", stacksToString(this.inv));
         var1.setProperty("armor", stacksToString(this.armor));

         try {
            FileOutputStream var2 = new FileOutputStream(new File(this.world.dir, "level.properties"));

            try {
               var1.store(var2, "Blockcraft level");
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
            var7.printStackTrace();
         }
      }
   }

   void quitToTitle() {
      this.saveWorld();

      for (Chunk var2 : this.world.chunks.values()) {
         var2.unloaded = true;
         this.rend.onUnload(var2);
      }

      this.world.chunks.clear();
      this.world.shutdown();
      this.world = null;
      this.player = null;
      this.target = null;
      this.setScreen(Game.S.TITLE);
   }

   double[] findStand(int var1, int var2) {
      for (int var3 = 0; var3 <= 10; var3++) {
         for (int var4 = -var3; var4 <= var3; var4++) {
            for (int var5 = -var3; var5 <= var3; var5++) {
               if (Math.max(Math.abs(var4), Math.abs(var5)) == var3) {
                  int var6 = var1 + var4;
                  int var7 = var2 + var5;
                  if (this.world.isLoaded(var6, var7)) {
                     for (int var8 = 255; var8 > 0; var8--) {
                        int var9 = this.world.getBlock(var6, var8, var7);
                        if (var9 != 0) {
                           if (Blocks.solid[var9] && !Blocks.isLeaves(var9) && !Blocks.isLog(var9) && var9 != 28) {
                              return new double[]{var6 + 0.5, var8 + 1, var7 + 0.5};
                           }
                           break;
                        }
                     }
                  }
               }
            }
         }
      }

      return null;
   }

   void respawn() {
      double[] var1 = null;
      if (this.bedPos != null) {
         if (Blocks.isBed(this.world.getBlock(this.bedPos[0], this.bedPos[1], this.bedPos[2]))) {
            var1 = this.findStand(this.bedPos[0], this.bedPos[2]);
         } else {
            this.bedPos = null;
            this.say("Your home bed was missing or obstructed");
         }
      }

      if (var1 == null) {
         var1 = this.findStand(this.spawnXZ[0], this.spawnXZ[1]);
      }

      if (var1 == null) {
         var1 = new double[]{this.spawnXZ[0] + 0.5, 100.0, this.spawnXZ[1] + 0.5};
      }

      this.player.dead = false;
      this.player.burn = 0;
      this.player.health = 20;
      this.player.food = 20;
      this.player.sat = 5.0F;
      this.player.exh = 0.0F;
      this.player.air = 300;
      this.player.fallDistance = 0.0F;
      this.player.invuln = 0;
      this.deathHandled = false;
      this.player.setPos(var1[0], var1[1], var1[2]);
   }

   void setScreen(Game.S var1) {
      Game.S var2 = this.screen;
      this.screen = var1;
      if (var1 == Game.S.WORLDS && var2 != Game.S.WORLDS) {
         this.scanWorlds();
      }

      boolean var3 = var1 == Game.S.PLAY;
      if (var3 != this.grabbed) {
         this.grabbed = var3;
         GLFW.glfwSetInputMode(this.window, 208897, var3 ? 212995 : 212993);
         if (var3 && GLFW.glfwRawMouseMotionSupported()) {
            GLFW.glfwSetInputMode(this.window, 208901, 1);
         }

         this.firstMouse = true;
      }

      if (var2 == Game.S.INV && var1 != Game.S.INV) {
         this.closeMenu();
      }

      if (var1 == Game.S.PLAY) {
         this.leftDown = this.rightDown = false;
         this.breakProg = 0.0F;
      }

      this.btns.clear();
      this.activeBtns.clear();
      this.dragging = -1;
   }

   boolean screenIsPlay() {
      return this.screen == Game.S.PLAY || this.screen == Game.S.CHAT || this.screen == Game.S.INV || this.screen == Game.S.DEAD;
   }

   void onCursor(double var1, double var3) {
      this.mouseX = var1 * this.fbW / this.winW;
      this.mouseY = var3 * this.fbH / this.winH;
      if (this.grabbed && this.player != null) {
         if (this.firstMouse) {
            this.lastMX = var1;
            this.lastMY = var3;
            this.firstMouse = false;
            return;
         }

         double var5 = var1 - this.lastMX;
         double var7 = var3 - this.lastMY;
         this.lastMX = var1;
         this.lastMY = var3;
         float var9 = (this.opt.sensitivity * 0.6F + 0.2F) / this.zoomDiv();
         float var10 = var9 * var9 * var9 * 8.0F;
         this.player.yaw = (float)(this.player.yaw + var5 * var10 * 0.15);
         this.player.pitch = (float)(this.player.pitch + var7 * var10 * 0.15);
         if (this.player.pitch > 90.0F) {
            this.player.pitch = 90.0F;
         }

         if (this.player.pitch < -90.0F) {
            this.player.pitch = -90.0F;
         }
      }
   }

   void onScroll(double var1) {
      if (this.screen == Game.S.WORLDS) {
         this.worldScroll = Math.max(0, Math.min(Math.max(0, this.worldList.size() - 5), this.worldScroll - (int)Math.signum(var1)));
      } else {
         if (this.screen == Game.S.PLAY && this.key(67)) {
            if (var1 != 0.0) {
               this.zoomFactor = Math.max(1.5F, Math.min(12.0F, this.zoomFactor * (var1 > 0.0 ? 1.25F : 0.8F)));
            }
         } else if (this.screen == Game.S.PLAY) {
            if (var1 != 0.0) {
               this.sel = ((this.sel - (int)Math.signum(var1)) % 9 + 9) % 9;
               this.nameTimer = 40.0F;
               this.eatTicks = 0;
            }
         } else if (this.screen == Game.S.INV && this.menu != null && this.menu.kind == 4) {
            this.menu.page = Math.max(0, Math.min(this.menu.pages() - 1, this.menu.page - (int)Math.signum(var1)));
         }
      }
   }

   void onChar(int var1) {
      if (this.screen == Game.S.CHAT) {
         if (System.currentTimeMillis() - this.chatOpenedAt < 80L && (var1 == 116 || var1 == 84)) {
            return;
         }

         if (var1 >= 32 && var1 < 127 && this.chat.length() < 100) {
            this.chat.append((char)var1);
         }
      } else if (this.screen == Game.S.NEWWORLD) {
         if (var1 >= 32 && var1 < 127) {
            if (this.focusField == 0 && this.newName.length() < 24) {
               this.newName = this.newName + (char)var1;
            } else if (this.focusField == 1 && this.seedField.length() < 30) {
               this.seedField = this.seedField + (char)var1;
            }
         }
      } else if (this.screen == Game.S.PLAY && var1 == 47) {
         this.openChat("/");
      }
   }

   void openChat(String var1) {
      this.setScreen(Game.S.CHAT);
      this.chat.setLength(0);
      this.chat.append(var1);
      this.chatOpenedAt = System.currentTimeMillis();
   }

   void onKey(int var1, int var2, int var3) {
      if (var2 != 0) {
         if (var1 == 300 && var2 == 1) {
            this.setFullscreen(!this.opt.fullscreen);
         } else {
            switch (this.screen) {
               case TITLE:
               case PLAY:
               default:
                  if (this.screen != Game.S.PLAY) {
                     return;
                  } else {
                     if (var2 == 1) {
                        switch (var1) {
                           case 32:
                              long var7 = System.currentTimeMillis();
                              if (this.player.creative && !this.player.spectator && var7 - this.lastSpacePress < 300L) {
                                 this.player.flying = !this.player.flying;
                                 if (this.player.flying) {
                                    this.player.vy = 0.0;
                                 }

                                 this.lastSpacePress = 0L;
                              } else {
                                 this.lastSpacePress = var7;
                              }

                              return;
                           case 69:
                              if (!this.player.spectator) {
                                 this.openMenu(this.player.creative ? 4 : 0, null);
                              }

                              return;
                           case 81:
                              this.dropHeld((var3 & 2) != 0);
                              return;
                           case 84:
                              this.openChat("");
                              return;
                           case 87:
                              long var6 = System.currentTimeMillis();
                              if (var6 - this.lastWPress < 300L) {
                                 this.sprintToggle = true;
                              }

                              this.lastWPress = var6;
                              return;
                           case 256:
                              this.setScreen(Game.S.PAUSE);
                              return;
                           case 290:
                              this.hideGui = !this.hideGui;
                              return;
                           case 291:
                              this.screenshot();
                              return;
                           case 292:
                              this.debug = !this.debug;
                              return;
                           default:
                              if (var1 >= 49 && var1 <= 57) {
                                 this.sel = var1 - 49;
                                 this.nameTimer = 40.0F;
                              }
                        }
                     }

                     return;
                  }
               case WORLDS:
                  if (var1 == 256 && var2 == 1) {
                     this.setScreen(Game.S.TITLE);
                  }

                  return;
               case NEWWORLD:
                  if (var1 == 259) {
                     if (this.focusField == 0 && this.newName.length() > 0) {
                        this.newName = this.newName.substring(0, this.newName.length() - 1);
                     } else if (this.focusField == 1 && this.seedField.length() > 0) {
                        this.seedField = this.seedField.substring(0, this.seedField.length() - 1);
                     }
                  }

                  if (var1 == 258 && var2 == 1) {
                     this.focusField = 1 - this.focusField;
                  }

                  if (var1 == 256 && var2 == 1) {
                     this.scanWorlds();
                     this.setScreen(this.worldList.isEmpty() ? Game.S.TITLE : Game.S.WORLDS);
                  }

                  if (var1 == 257 && var2 == 1) {
                     this.startWorld(true, this.seedField, this.newMode);
                  }

                  return;
               case LOADING:
                  return;
               case CHAT:
                  if (var1 == 256 && var2 == 1) {
                     this.setScreen(Game.S.PLAY);
                  } else if (var1 == 259 && this.chat.length() > 0) {
                     this.chat.setLength(this.chat.length() - 1);
                  } else if ((var1 == 257 || var1 == 335) && var2 == 1) {
                     String var4 = this.chat.toString().trim();
                     this.setScreen(Game.S.PLAY);
                     if (!var4.isEmpty()) {
                        this.runCommand(var4);
                     }
                  }

                  return;
               case INV:
                  if (var2 == 1 && (var1 == 256 || var1 == 69)) {
                     this.setScreen(Game.S.PLAY);
                  }

                  return;
               case PAUSE:
                  if (var2 == 1 && var1 == 256) {
                     this.setScreen(Game.S.PLAY);
                  }

                  return;
               case OPTIONS:
                  if (var2 == 1 && var1 == 256) {
                     this.opt.save();
                     this.setScreen(this.optionsReturn);
                  }

                  return;
               case DEAD:
            }
         }
      }
   }

   void onMouse(int var1, int var2) {
      boolean var3 = var2 == 1;
      if (this.screen == Game.S.PLAY) {
         if (var1 == 0) {
            this.leftDown = var3;
            if (var3) {
               this.leftPressed = true;
            }
         } else if (var1 == 1) {
            this.rightDown = var3;
            if (var3) {
               this.placeDelay = 0;
               this.rightPressed = true;
            }
         } else if (var1 == 2 && var3) {
            this.pickBlock();
         }
      } else {
         if (var1 == 0 && !var3) {
            this.dragging = -1;
         }

         if (var3) {
            float var4 = (float)(this.mouseX / this.gui.scale);
            float var5 = (float)(this.mouseY / this.gui.scale);
            if (this.screen == Game.S.INV) {
               if (var1 == 0) {
                  for (int var8 = 0; var8 < this.activeBtns.size(); var8++) {
                     Game.Btn var9 = (Game.Btn)this.activeBtns.get(var8);
                     if (var4 >= var9.x && var4 <= var9.x + var9.w && var5 >= var9.y && var5 <= var9.y + var9.h && var9.action != null) {
                        var9.action.run();
                        return;
                     }
                  }
               }

               this.menuClick(var4, var5, var1);
            } else if (var1 == 0) {
               for (int var6 = 0; var6 < this.activeBtns.size(); var6++) {
                  Game.Btn var7 = (Game.Btn)this.activeBtns.get(var6);
                  if (var4 >= var7.x && var4 <= var7.x + var7.w && var5 >= var7.y && var5 <= var7.y + var7.h) {
                     if (var7.slider != null) {
                        this.dragging = var6;
                     } else if (var7.action != null) {
                        var7.action.run();
                     }
                     break;
                  }
               }
            }
         }
      }
   }

   void say(String var1) {
      this.chatLog.add(var1);
      this.chatTimes.add(System.currentTimeMillis());
   }

   /** /weather [clear|rain|thunder [seconds]] [storm [distance]] [natural] [on|off] [info] */
   void weatherCommand(String[] a) {
      Weather w = this.world.weather;
      String sub = a.length > 1 ? a[1].toLowerCase() : "info";
      long secs = a.length > 2 ? Long.parseLong(a[2]) : 360L;
      switch (sub) {
         case "clear":
            w.force(1, secs);
            this.say("Weather: clearing up for " + secs + " s");
            break;
         case "rain":
            w.force(2, secs);
            this.say("Weather: rain for " + secs + " s");
            break;
         case "thunder":
            w.force(3, a.length > 2 ? secs : 240L);
            this.say("Weather: thunderstorm for " + (a.length > 2 ? secs : 240L) + " s");
            break;
         case "storm":
            double dist = a.length > 2 ? Double.parseDouble(a[2]) : 900.0;
            w.summonStorm(this.player.x, this.player.z, Math.max(0.0, dist));
            this.say("A thunderstorm is brewing " + (int)dist + " blocks upwind and drifting your way");
            break;
         case "natural":
         case "reset":
            w.natural();
            this.say("Weather: natural again");
            break;
         case "off":
            w.enabled = false;
            this.say("Weather is off (only fair-weather clouds)");
            break;
         case "on":
            w.enabled = true;
            this.say("Weather is on");
            break;
         default:
            String near = w.nearestStorm(this.player.x, this.player.z);
            this.say("Weather here: " + w.describe(this.player.x, this.player.z) + (near == null ? "; no thunderstorm in sight" : "; nearest thunderstorm: " + near));
            this.say("/weather clear|rain|thunder [seconds]   /weather storm [distance]   /weather natural|on|off");
      }
   }

   void runCommand(String var1) {
      if (!var1.startsWith("/")) {
         this.say("<Player> " + var1);
      } else {
         String[] var2 = var1.substring(1).split("\\s+");

         try {
            switch (var2[0].toLowerCase()) {
               case "gamemode":
               case "gm":
                  String var16 = var2.length > 1 ? var2[1].toLowerCase() : "";
                  int var19 = -1;
                  if (!var16.equals("0") && (!var16.startsWith("s") || var16.startsWith("sp"))) {
                     if (var16.equals("1") || var16.startsWith("c")) {
                        var19 = 1;
                     } else if (var16.equals("2") || var16.startsWith("a")) {
                        var19 = 2;
                     } else if (var16.equals("3") || var16.startsWith("sp")) {
                        var19 = 3;
                     }
                  } else {
                     var19 = 0;
                  }

                  if (var19 < 0) {
                     this.say("§cUsage: /gamemode <survival|creative|adventure|spectator>");
                  } else {
                     this.player.setMode(var19);
                     String[] var23 = new String[]{"Survival", "Creative", "Adventure", "Spectator"};
                     this.say("Set own game mode to " + var23[var19] + " Mode");
                  }
                  break;
               case "time":
                  if (var2.length >= 3 && var2[1].equals("set")) {
                     String var15 = var2[2].toLowerCase();
                     long var18;
                     switch (var15) {
                        case "day":
                           var18 = 1000L;
                           break;
                        case "noon":
                           var18 = 6000L;
                           break;
                        case "night":
                           var18 = 13000L;
                           break;
                        case "midnight":
                           var18 = 18000L;
                           break;
                        default:
                           var18 = Long.parseLong(var15);
                     }

                     this.world.dayTime = var18;
                     this.say("Set the time to " + var18);
                  } else {
                     this.say("§cUsage: /time set <day|noon|night|midnight|ticks>");
                  }
                  break;
               case "tp":
                  if (var2.length >= 4) {
                     double var14 = rel(var2[1], this.player.x);
                     double var22 = rel(var2[2], this.player.y);
                     double var27 = rel(var2[3], this.player.z);
                     this.player.setPos(var14, var22, var27);
                     this.say(String.format("Teleported to %.1f, %.1f, %.1f", new Object[]{var14, var22, var27}));
                  } else {
                     this.say("§cUsage: /tp <x> <y> <z>");
                  }
                  break;
               case "seed":
                  this.say("Seed: [" + this.world.seed + "]");
                  break;
               case "summon":
                  int var13 = -1;
                  String var17 = var2.length > 1 ? var2[1].toLowerCase() : "";

                  for (int var20 = 0; var20 < 8; var20++) {
                     if (Mobs.NAME[var20].equals(var17)) {
                        var13 = var20;
                     }
                  }

                  if (var13 < 0) {
                     this.say("§cUsage: /summon <pig|cow|sheep|chicken|zombie|skeleton|creeper|spider> [count]");
                  } else {
                     int var21 = var2.length > 2 ? Math.max(1, Math.min(20, Integer.parseInt(var2[2]))) : 1;
                     double var25 = Math.toRadians(this.player.yaw);

                     for (int var29 = 0; var29 < var21; var29++) {
                        this.mobs
                           .spawn(
                              var13,
                              this.player.x - Math.sin(var25) * 3.0 + var29 % 3 * 0.7,
                              this.player.y + 0.1,
                              this.player.z + Math.cos(var25) * 3.0 + var29 / 3 * 0.7
                           );
                     }

                     this.say("Summoned " + var21 + " " + Mobs.NAME[var13]);
                  }
                  break;
               case "give":
                  if (var2.length < 2) {
                     this.say("§cUsage: /give <block name> [count]");
                  } else {
                     int var5 = var2.length > 2 ? Integer.parseInt(var2[2]) : 64;
                     String var6 = var2[1].toLowerCase().replace('_', ' ');
                     int var7 = -1;

                     for (int var11 : Blocks.creativeList()) {
                        if (Items.name(var11).toLowerCase().contains(var6)) {
                           var7 = var11;
                           break;
                        }
                     }

                     if (var7 < 0) {
                        this.say("§cUnknown item: " + var2[1]);
                     } else {
                        Stack var24 = new Stack(var7, var5);
                        this.addItem(var24);
                        this.say("Gave " + var5 + " [" + Items.name(var7) + "]");
                     }
                  }
                  break;
               case "kill":
                  this.player.health = 0;
                  this.player.dead = true;
                  break;
               case "heal":
                  this.player.health = 20;
                  this.player.food = 20;
                  this.player.sat = 5.0F;
                  this.say("Healed");
                  break;
               case "fly":
                  if (!this.player.creative) {
                     this.setCreativeFlight();
                  }

                  this.player.flying = !this.player.flying;
                  break;
               case "firespread":
                  if (var2.length > 1 && (var2[1].equalsIgnoreCase("on") || var2[1].equalsIgnoreCase("off"))) {
                     this.world.fire.spread = var2[1].equalsIgnoreCase("on");
                  }

                  this.say("Fire spread is " + (this.world.fire.spread ? "on" : "off") + "  (/firespread on|off)");
                  break;
               case "weather":
                  this.weatherCommand(var2);
                  break;
               case "help":
                  this.say("Commands: /gamemode /time set /tp /seed /give /kill /heal /fly /summon /firespread /weather");
                  break;
               default:
                  this.say("§cUnknown command. Try /help");
            }
         } catch (Exception var12) {
            this.say("§cInvalid arguments");
         }
      }
   }

   void setCreativeFlight() {
      this.say("Flying is only available in creative or spectator mode");
   }

   static double rel(String var0, double var1) {
      return var0.startsWith("~") ? var1 + (var0.length() > 1 ? Double.parseDouble(var0.substring(1)) : 0.0) : Double.parseDouble(var0);
   }

   Stack held() {
      return this.inv[this.sel];
   }

   int heldId() {
      return this.inv[this.sel] == null ? 0 : this.inv[this.sel].id;
   }

   boolean addItem(int var1, int var2) {
      return this.addItem(new Stack(var1, var2));
   }

   boolean addItem(Stack var1) {
      return var1 != null && var1.n > 0 ? Menu.insert(this.inv, var1, 0, 36, true) == null : true;
   }

   void dropHeld(boolean var1) {
      Stack var2 = this.inv[this.sel];
      if (var2 != null) {
         int var3 = var1 ? var2.n : 1;
         this.dropFromPlayer(var2.id, var3, var2.dmg);
         var2.n -= var3;
         if (var2.n <= 0) {
            this.inv[this.sel] = null;
         }
      }
   }

   void dropFromPlayer(int var1, int var2) {
      this.dropFromPlayer(var1, var2, 0);
   }

   void dropFromPlayer(int var1, int var2, int var3) {
      double var4 = Math.toRadians(this.player.yaw);
      double var6 = Math.toRadians(this.player.pitch);
      double var8 = this.player.x;
      double var10 = this.player.y + this.player.eye - 0.3;
      double var12 = this.player.z;
      Item var14 = this.world.dropItem(var8, var10, var12, var1, var2, var3);
      if (var14 != null) {
         var14.vx = -Math.sin(var4) * Math.cos(var6) * 0.3;
         var14.vz = Math.cos(var4) * Math.cos(var6) * 0.3;
         var14.vy = -Math.sin(var6) * 0.3 + 0.1;
         var14.delay = 40;
      }
   }

   void openMenu(int var1, TileEntity var2) {
      this.menu = new Menu(this, var1, var2);
      this.setScreen(Game.S.INV);
   }

   void closeMenu() {
      if (this.menu != null) {
         this.menu.close();
         this.menu = null;
      }
   }

   float menuOx() {
      return (float)Math.floor((this.gui.w - this.menu.w) / 2.0F);
   }

   float menuOy() {
      return (float)Math.floor((this.gui.h - this.menu.h) / 2.0F);
   }

   void menuClick(float var1, float var2, int var3) {
      if (this.menu != null) {
         float var4 = this.menuOx();
         float var5 = this.menuOy();
         Slot var6 = this.menu.at(var1, var2, var4, var5);
         boolean var7 = this.key(340) || this.key(344);
         if (var6 != null) {
            this.menu.click(var6, var3 == 0 ? 0 : 1, var7);
         } else {
            boolean var8 = var1 >= var4 && var1 < var4 + this.menu.w && var2 >= var5 && var2 < var5 + this.menu.h;
            if (!var8 && this.cursor != null) {
               if (this.menu.kind != 4) {
                  boolean var9 = var3 == 0;
                  this.dropFromPlayer(this.cursor.id, var9 ? this.cursor.n : 1, this.cursor.dmg);
                  if (!var9 && this.cursor.n > 1) {
                     this.cursor.n--;
                  } else {
                     this.cursor = null;
                  }
               } else {
                  this.cursor = null;
               }
            }
         }
      }
   }

   void pickBlock() {
      if (this.player.creative && !this.player.spectator && this.target != null) {
         int var1 = this.world.getBlock(this.target.x, this.target.y, this.target.z);
         if (var1 != 0) {
            if (var1 == 70) {
               var1 = 69;
            }

            if (var1 == 68) {
               var1 = 67;
            }

            var1 = Blocks.pickItem(var1);
            if (var1 != 0 && !Blocks.isFluid(var1)) {
               for (int var2 = 0; var2 < 9; var2++) {
                  if (this.inv[var2] != null && this.inv[var2].id == var1) {
                     this.sel = var2;
                     this.nameTimer = 40.0F;
                     return;
                  }
               }

               this.inv[this.sel] = new Stack(var1, 64);
               this.nameTimer = 40.0F;
            }
         }
      }
   }

   void drawStack(Stack var1, float var2, float var3) {
      if (var1 != null) {
         this.gui.icon(var1.id, var2, var3, 16.0F);
         if (var1.n > 1) {
            String var4 = var1.n + "";
            this.gui.text(var4, var2 + 17.0F - Gui.textWidth(var4), var3 + 8.0F, -1, true);
         }

         int var7 = Items.maxDamage(var1.id);
         if (var7 > 0 && var1.dmg > 0) {
            float var5 = 1.0F - (float)var1.dmg / var7;
            int var6 = var5 > 0.5F ? -11141291 : (var5 > 0.25F ? -171 : -43691);
            this.gui.rect(var2 + 2.0F, var3 + 13.0F, 13.0F, 2.0F, -16777216);
            this.gui.rect(var2 + 2.0F, var3 + 13.0F, Math.max(1, Math.round(13.0F * var5)), 1.0F, var6);
         }
      }
   }

   void tooltip(Stack var1, float var2, float var3) {
      ArrayList<String> var4 = new ArrayList<>();
      String var5 = Items.name(var1.id);
      var4.add(var5);
      int var6 = Items.maxDamage(var1.id);
      if (var6 > 0) {
         var4.add("§7Durability: " + (var6 - var1.dmg) + " / " + var6);
      }

      if (Items.isArmor(var1.id)) {
         var4.add("§9+" + Items.armorPoints(var1.id) + " Armor");
      }

      if (Items.isTool(var1.id)) {
         var4.add("§9" + String.format("%.1f", new Object[]{Items.attackDamage(var1.id)}) + " Attack Damage");
      }

      if (Items.isFood(var1.id)) {
         var4.add("§7Restores " + Items.foodPts[var1.id] / 2.0F + " hunger");
      }

      float var7 = 0.0F;

      for (String var9 : var4) {
         var7 = Math.max(var7, Gui.textWidth(Gui.stripCodes(var9)));
      }

      float var12 = var2 + 8.0F;
      float var13 = var3 - 6.0F;
      if (var12 + var7 + 8.0F > this.gui.w) {
         var12 = var2 - var7 - 14.0F;
      }

      float var10 = 4 + var4.size() * 10;
      this.gui.rect(var12 - 1.0F, var13 - 1.0F, var7 + 8.0F, var10 + 2.0F, -267386864);
      this.gui.rect(var12, var13, var7 + 6.0F, var10, -266338256);

      for (int var11 = 0; var11 < var4.size(); var11++) {
         this.gui.text((String)var4.get(var11), var12 + 3.0F, var13 + 3.0F + var11 * 10, -1, true);
      }
   }

   void loop() {
      long var1 = System.nanoTime();
      this.fpsTimer = GLFW.glfwGetTime();

      while (!GLFW.glfwWindowShouldClose(this.window)) {
         long var3 = System.nanoTime();
         double var5 = (var3 - var1) / 1.0E9;
         var1 = var3;
         if (var5 > 0.25) {
            var5 = 0.25;
         }

         GLFW.glfwPollEvents();
         this.update(var5);
         if (System.getProperty("bc.auto") != null) {
            this.autoTest();
         }

         this.render();
         if (this.autoShotNow) {
            this.screenshot();
            System.out.println("AUTO screenshot; chunks=" + (this.world == null ? 0 : this.world.chunks.size()) + " fps=" + this.fps);
            GLFW.glfwSetWindowShouldClose(this.window, true);
         }

         GLFW.glfwSwapBuffers(this.window);
         this.frames++;
         double var7 = GLFW.glfwGetTime();
         if (var7 - this.fpsTimer >= 1.0) {
            this.fps = this.frames;
            this.frames = 0;
            this.fpsTimer = var7;
         }
      }
   }

   void update(double var1) {
      if (this.world != null) {
         int var3 = (int)Math.floor(this.player.x) >> 4;
         int var4 = (int)Math.floor(this.player.z) >> 4;
         this.rend.camX = this.player.x;
         this.rend.camY = this.player.y + this.player.eye;
         this.rend.camZ = this.player.z;
         boolean var5 = this.screen == Game.S.LOADING;
         this.world.updateChunks(var3, var4, this.opt.renderDistance + 1, 256, var5 ? 30000000L : 4000000L);
         this.rend.scheduleMeshes(this.world, var5 ? 64 : 48);
         this.rend.uploadMeshes(256, var5 ? 20000000L : 4000000L);
         if (this.screen == Game.S.LOADING) {
            int var16 = 25;
            int var18 = this.world.loadedAround(var3, var4, 2);
            boolean var19 = var18 >= var16;
            if (var19) {
               for (int var20 = -1; var20 <= 1 && var19; var20++) {
                  for (int var10 = -1; var10 <= 1 && var19; var10++) {
                     Chunk var11 = this.world.chunk(var3 + var20, var4 + var10);
                     if (var11 == null) {
                        var19 = false;
                        break;
                     }

                     for (Section var15 : var11.sections) {
                        if (var15.dirty || var15.inFlight) {
                           var19 = false;
                        }
                     }
                  }
               }
            }

            this.loadingMsg = "Chunks: " + var18 + "/" + var16;
            if (var19) {
               if (this.needSpawn) {
                  double[] var21 = this.findStand(this.spawnXZ[0], this.spawnXZ[1]);
                  if (var21 != null) {
                     this.spawnPos = var21;
                     this.spawnXZ = new int[]{(int)Math.floor(var21[0]), (int)Math.floor(var21[2])};
                     this.player.setPos(var21[0], var21[1], var21[2]);
                     this.needSpawn = false;
                  }
               }

               if (!this.needSpawn) {
                  if (Phys.collides(this.world, this.player.bb)) {
                     this.player.setPos(this.player.x, this.player.y + 1.0, this.player.z);
                  }

                  this.setScreen(Game.S.PLAY);
               }
            }
         } else {
            boolean var6 = this.screen == Game.S.PLAY || this.screen == Game.S.INV || this.screen == Game.S.CHAT || this.screen == Game.S.DEAD;
            if (var6) {
               this.acc += var1 * 20.0;

               for (int var7 = 0; this.acc >= 1.0 && var7 < 5; var7++) {
                  this.tick();
                  this.acc--;
               }

               if (this.acc >= 1.0) {
                  this.acc = 0.0;
               }
            }

            this.partial = (float)this.acc;
            if (this.dragging >= 0 && this.dragging < this.activeBtns.size() && GLFW.glfwGetMouseButton(this.window, 0) == 1) {
               Game.Btn var17 = (Game.Btn)this.activeBtns.get(this.dragging);
               float var8 = (float)(this.mouseX / this.gui.scale);
               float var9 = (var8 - var17.x - 4.0F) / (var17.w - 8.0F);
               var17.slider.accept(Math.max(0.0F, Math.min(1.0F, var9)));
            }
         }
      }
   }

   boolean key(int var1) {
      return GLFW.glfwGetKey(this.window, var1) == 1;
   }

   void tick() {
      this.tickCount++;
      this.world.tick();
      this.world.fire.particles(this);
      this.Tex_tick();
      if (this.screen != Game.S.PAUSE) {
         this.mobs.tick();
      }

      boolean var1 = this.screen == Game.S.PLAY;
      float var2 = 0.0F;
      float var3 = 0.0F;
      boolean var4 = false;
      boolean var5 = false;
      boolean var6 = false;
      if (var1) {
         if (this.key(87)) {
            var2++;
         }

         if (this.key(83)) {
            var2--;
         }

         if (this.key(65)) {
            var3++;
         }

         if (this.key(68)) {
            var3--;
         }

         var4 = this.key(32);
         var5 = this.key(340) || this.key(344);
         var6 = this.key(341) || this.sprintToggle;
      }

      if (!this.key(87) || !var1) {
         this.sprintToggle = false;
      }

      if (!this.player.dead) {
         this.player.tick(this.world, var2, var3, var4, var5, var6);
      }

      if (this.player.dead && this.screen != Game.S.DEAD) {
         if (!this.deathHandled) {
            this.deathHandled = true;
            if (!this.player.creative) {
               for (int var7 = 0; var7 < 36; var7++) {
                  if (this.inv[var7] != null) {
                     this.world.dropItem(this.player.x, this.player.y + 0.5, this.player.z, this.inv[var7].id, this.inv[var7].n, this.inv[var7].dmg);
                     this.inv[var7] = null;
                  }
               }

               for (int var12 = 0; var12 < 4; var12++) {
                  if (this.armor[var12] != null) {
                     this.world.dropItem(this.player.x, this.player.y + 0.5, this.player.z, this.armor[var12].id, this.armor[var12].n, this.armor[var12].dmg);
                     this.armor[var12] = null;
                  }
               }
            }
         }

         this.setScreen(Game.S.DEAD);
      }

      this.swingPrev = this.swing;
      if (this.swinging) {
         this.swing += 0.25F;
         if (this.swing >= 1.0F) {
            this.swing = 0.0F;
            this.swingPrev = 0.0F;
            this.swinging = this.leftDown && this.screen == Game.S.PLAY;
         }
      }

      if (this.nameTimer > 0.0F) {
         this.nameTimer--;
      }

      this.updateTarget();
      if (this.screen == Game.S.PLAY) {
         this.interact();
      }

      this.pickupItems();
      Iterator var13 = this.particles.iterator();

      while (var13.hasNext()) {
         Game.Particle var8 = (Game.Particle)var13.next();
         var8.life--;
         if (var8.life <= 0) {
            var13.remove();
         } else {
            var8.vy -= var8.grav;
            float var9 = var8.x + var8.vx;
            float var10 = var8.y + var8.vy;
            float var11 = var8.z + var8.vz;
            if (Blocks.solid[this.world.getBlock((int)Math.floor(var9), (int)Math.floor(var8.y), (int)Math.floor(var8.z))]) {
               var8.vx = -var8.vx * 0.3F;
            }

            if (Blocks.solid[this.world.getBlock((int)Math.floor(var8.x), (int)Math.floor(var8.y), (int)Math.floor(var11))]) {
               var8.vz = -var8.vz * 0.3F;
            }

            if (Blocks.solid[this.world.getBlock((int)Math.floor(var8.x), (int)Math.floor(var10), (int)Math.floor(var8.z))]) {
               var8.vy = 0.0F;
               var8.vx *= 0.7F;
               var8.vz *= 0.7F;
            }

            var8.x = var8.x + var8.vx;
            var8.y = var8.y + var8.vy;
            var8.z = var8.z + var8.vz;
            var8.vx *= 0.98F;
            var8.vy *= 0.98F;
            var8.vz *= 0.98F;
         }
      }

      if (!var1) {
         this.leftDown = false;
      }
   }

   void Tex_tick() {
   }

   void updateTarget() {
      if (this.player.spectator) {
         this.target = null;
      } else {
         double var1 = Math.toRadians(this.player.yaw);
         double var3 = Math.toRadians(this.player.pitch);
         double var5 = -Math.sin(var1) * Math.cos(var3);
         double var7 = -Math.sin(var3);
         double var9 = Math.cos(var1) * Math.cos(var3);
         double var11 = this.player.creative ? 5.0 : 4.5;
         this.target = this.world.raycast(this.player.x, this.player.y + this.player.eye, this.player.z, var5, var7, var9, var11);
         this.targetMob = null;
         double[] var13 = new double[1];
         double var14 = this.player.x;
         double var16 = this.player.y + this.player.eye;
         double var18 = this.player.z;
         Mob var20 = this.mobs.pick(var14, var16, var18, var5, var7, var9, this.player.creative ? 5.0 : 3.0, var13);
         if (var20 != null && (this.target == null || var13[0] < this.target.t)) {
            this.targetMob = var20;
            this.targetMobT = var13[0];
            this.target = null;
         }
      }
   }

   int crackStage() {
      return !(this.breakProg <= 0.0F) && !this.player.creative ? Math.min(9, (int)(this.breakProg * 10.0F)) : -1;
   }

   void interact() {
      if (this.breakDelay > 0) {
         this.breakDelay--;
      }

      if (this.placeDelay > 0) {
         this.placeDelay--;
      }

      if (this.leftClickCounter > 0) {
         this.leftClickCounter--;
      }

      this.atkTicks++;
      boolean var1 = this.leftPressed;
      boolean var2 = this.rightPressed;
      this.leftPressed = this.rightPressed = false;
      if (this.player.spectator) {
         this.breakProg = 0.0F;
         this.eatTicks = 0;
      } else {
         Stack var3 = this.held();
         int var4 = this.heldId();
         if (this.leftDown && !this.swinging) {
            this.swinging = true;
            this.swing = 1.0E-4F;
         }

         boolean var5 = false;
         if (this.targetMob != null) {
            if (var1) {
               this.attackMob(this.targetMob);
            }

            var5 = true;
            this.breakProg = 0.0F;
         }

         if (!var5 && this.leftDown && this.target != null) {
            int var6 = this.world.getBlock(this.target.x, this.target.y, this.target.z);
            if (!Blocks.breakable(var6) || this.player.adventure) {
               this.breakProg = 0.0F;
            } else if (this.player.creative) {
               boolean var7 = Items.isTool(var4) && Items.toolClass(var4) == 4;
               if (this.breakDelay <= 0 && !var7) {
                  this.breakBlock(this.target.x, this.target.y, this.target.z);
                  this.breakDelay = 5;
               }
            } else {
               if (this.breakX != this.target.x || this.breakY != this.target.y || this.breakZ != this.target.z) {
                  this.breakProg = 0.0F;
                  this.breakX = this.target.x;
                  this.breakY = this.target.y;
                  this.breakZ = this.target.z;
               }

               int var20 = Items.breakTicks(var6, var4, this.player.onGround, this.player.inWater);
               this.breakProg += 1.0F / var20;
               if (this.rnd.nextInt(3) == 0) {
                  this.spawnBreakParticles(this.target.x, this.target.y, this.target.z, var6, 1);
               }

               if (this.breakProg >= 1.0F) {
                  this.breakBlock(this.target.x, this.target.y, this.target.z);
                  this.breakProg = 0.0F;
                  this.breakDelay = 5;
               }
            }
         } else if (!var5) {
            this.breakProg = 0.0F;
         }

         boolean var19 = var2 && this.targetMob != null && this.useOnMob(this.targetMob, var3);
         if (var19) {
            this.swinging = true;
            this.swing = 1.0E-4F;
         }

         if (!var19 && var2 && this.target != null && (!this.player.sneaking || var3 == null)) {
            var19 = this.useBlock();
         }

         if (var19) {
            this.rightDown = false;
         } else if (var2 && var3 != null && this.useSpecial(var3)) {
            this.rightDown = false;
            this.swinging = true;
            this.swing = 1.0E-4F;
         } else if (this.rightDown && var3 != null && Items.isFood(var3.id) && this.player.food < 20 && !this.player.creative) {
            this.eatTicks++;
            if (this.eatTicks >= 32) {
               this.eatTicks = 0;
               this.player.food = Math.min(20, this.player.food + Items.foodPts[var3.id]);
               this.player.sat = Math.min(this.player.food, this.player.sat + Items.foodSat[var3.id]);
               var3.n--;
               if (var3.n <= 0) {
                  this.inv[this.sel] = null;
               }
            }
         } else {
            this.eatTicks = 0;
            if (var4 != 268) {
               this.bowCharge = 0;
               if (this.rightDown && this.placeDelay <= 0 && this.target != null && !this.player.adventure && this.placeBlock()) {
                  this.placeDelay = 4;
                  this.swinging = true;
                  this.swing = 1.0E-4F;
               }
            } else {
               boolean var21 = this.player.creative || this.countItem(267) > 0;
               if (this.rightDown && var21) {
                  this.bowCharge = Math.min(this.bowCharge + 1, 60);
               } else {
                  if (this.bowCharge > 0) {
                     float var8 = this.bowCharge / 20.0F;
                     var8 = (var8 * var8 + var8 * 2.0F) / 3.0F;
                     if (var8 > 1.0F) {
                        var8 = 1.0F;
                     }

                     this.bowCharge = 0;
                     if (var8 >= 0.1F) {
                        double var9 = Math.toRadians(this.player.yaw);
                        double var11 = Math.toRadians(this.player.pitch);
                        double var13 = -Math.sin(var9) * Math.cos(var11);
                        double var15 = -Math.sin(var11);
                        double var17 = Math.cos(var9) * Math.cos(var11);
                        this.mobs
                           .shootFromPlayer(
                              this.player.x + var13 * 0.4,
                              this.player.y + this.player.eye - 0.1 + var15 * 0.4,
                              this.player.z + var17 * 0.4,
                              var13,
                              var15,
                              var17,
                              3.0F * var8
                           );
                        if (!this.player.creative) {
                           this.removeItems(267, 1);
                           this.damageHeld(1);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   /** Re-meshes the chunk sections around a changed block. No-op without a renderer (headless tests). */
   void meshNow(int x, int y, int z) {
      if (this.rend != null) {
         this.rend.meshNow(this.world, x, y, z);
      }
   }

   boolean useBlock() {
      int var1 = this.world.getBlock(this.target.x, this.target.y, this.target.z);
      if (Blocks.isDoor(var1)) {
         this.toggleDoor(this.target.x, this.target.y, this.target.z);
         return true;
      }

      if (Blocks.isBed(var1)) {
         this.sleepAt(this.target.x, this.target.y, this.target.z);
         return true;
      }

      if (Blocks.isCake(var1)) {
         return this.eatCake();
      }

      if (var1 == Blocks.PUMPKIN && this.heldId() == Items.SHEARS && !this.player.adventure) {
         return this.carvePumpkin();
      }

      switch (var1) {
         case 66:
            this.openMenu(1, null);
            return true;
         case 67:
         case 68:
            TileEntity var3 = this.world.getOrCreateTile(this.target.x, this.target.y, this.target.z);
            if (var3 == null) {
               return false;
            }

            this.openMenu(2, var3);
            return true;
         case 69:
         case 70:
            TileEntity var2 = this.world.getOrCreateTile(this.target.x, this.target.y, this.target.z);
            if (var2 == null) {
               return false;
            }

            this.openMenu(3, var2);
            return true;
         default:
            return false;
      }
   }

   void blockDrops(int var1, int var2, int var3, int var4, int var5) {
      if (Items.canHarvest(var5, var4)) {
         int var6 = Blocks.drop[var4];
         int var7 = 1;
         if (Blocks.isStem(var4)) {
            int stage = Blocks.stemStage(var4);

            for (int seed = 0; seed < 3; seed++) {
               if (this.rnd.nextInt(15) <= stage) {
                  this.world.dropItem(var1 + 0.5, var2 + 0.3, var3 + 0.5, Blocks.stemSeedItem(var4), 1);
               }
            }
         } else if (Blocks.isCrop(var4)) {
            if (Blocks.isRipe(var4)) {
               if (Blocks.cropBase(var4) == 124) {
                  this.world.dropItem(var1 + 0.5, var2 + 0.3, var3 + 0.5, 286, 1);
                  int var8 = this.rnd.nextInt(4);
                  if (var8 > 0) {
                     this.world.dropItem(var1 + 0.5, var2 + 0.3, var3 + 0.5, 285, var8);
                  }
               } else {
                  this.world.dropItem(var1 + 0.5, var2 + 0.3, var3 + 0.5, Blocks.cropSeedItem(var4), 1 + this.rnd.nextInt(4));
               }
            } else {
               this.world.dropItem(var1 + 0.5, var2 + 0.3, var3 + 0.5, Blocks.cropSeedItem(var4), 1);
            }
         } else {
            switch (var4) {
               case 8:
                  if (this.rnd.nextInt(10) == 0) {
                     var6 = 266;
                  }
                  break;
               case 19:
                  var6 = 257;
                  break;
               case 22:
                  var6 = 260;
                  break;
               case 29:
                  var6 = var5 == 340 ? 29 : (this.rnd.nextInt(8) == 0 ? 285 : 0);
                  break;
               case 160:
                  var7 = 3;
                  break;
               case 170:
                  var7 = 3 + this.rnd.nextInt(5);
                  break;
               default:
                  if (Blocks.isLeaves(var4)) {
                     var6 = 0;
                     if (var5 == 340) {
                        var6 = var4;
                     } else if (this.rnd.nextInt(20) == 0 && var4 == 12) {
                        var6 = 270;
                     } else if (this.rnd.nextInt(50) == 0) {
                        var6 = 256;
                     }
                  }
            }

            if (var6 != 0) {
               this.world.dropItem(var1 + 0.5, var2 + 0.3, var3 + 0.5, var6, var7);
            }
         }
      }
   }

   int countItem(int var1) {
      int var2 = 0;

      for (Stack var6 : this.inv) {
         if (var6 != null && var6.id == var1) {
            var2 += var6.n;
         }
      }

      return var2;
   }

   void removeItems(int var1, int var2) {
      for (int var3 = 0; var3 < 36 && var2 > 0; var3++) {
         if (this.inv[var3] != null && this.inv[var3].id == var1) {
            int var4 = Math.min(var2, this.inv[var3].n);
            this.inv[var3].n -= var4;
            var2 -= var4;
            if (this.inv[var3].n <= 0) {
               this.inv[var3] = null;
            }
         }
      }
   }

   void attackMob(Mob var1) {
      int var2 = this.heldId();
      boolean var3 = Items.isTool(var2);
      float var4 = var3 ? Items.attackDamage(var2) : 1.0F;
      float var5 = 4.0F;
      if (var3) {
         int var6 = Items.toolClass(var2);
         var5 = var6 == 4 ? 1.6F : (var6 == 1 ? 1.2F : (var6 == 2 ? (Items.toolTier(var2) >= 3 ? 1.0F : 0.8F) : 1.0F));
      }

      float var10 = Math.min(1.0F, (this.atkTicks + 0.5F) / (20.0F / var5));
      var4 *= 0.2F + var10 * var10 * 0.8F;
      if (var10 > 0.9F && !this.player.onGround && this.player.vy < 0.0 && !this.player.inWater && !this.player.sprinting) {
         var4 *= 1.5F;
      }

      boolean var7 = this.player.sprinting;
      this.atkTicks = 0;
      this.swinging = true;
      this.swing = 1.0E-4F;
      boolean var8 = this.mobs.hurtMob(var1, Math.max(1, Math.round(var4)), this.player.x, this.player.z, 0.4 + (var7 ? 0.5 : 0.0), true);
      if (var7) {
         this.player.sprinting = false;
      }

      if (var8 && !this.player.creative) {
         if (var3) {
            this.damageHeld(Items.toolClass(var2) == 4 ? 1 : 2);
         }

         this.player.addExhaustion(0.1F);
      }
   }

   /** Update 3.8: a droplet of rain bouncing off the ground (or a ring on water). */
   void splashParticle(double x, double y, double z, boolean water) {
      Game.Particle p = new Game.Particle();
      p.x = (float)x;
      p.y = (float)y;
      p.z = (float)z;
      p.u = 1.0F / 256.0F;
      p.v = 65.0F / 256.0F;
      p.shade = 1.0F;
      if (water) {
         p.vy = 0.0F;
         p.grav = 0.0F;
         p.size = 0.05F;
         p.life = 5 + this.rnd.nextInt(3);
         p.r = 0.78F;
         p.g = 0.88F;
         p.b = 1.0F;
      } else {
         p.vx = (this.rnd.nextFloat() - 0.5F) * 0.05F;
         p.vz = (this.rnd.nextFloat() - 0.5F) * 0.05F;
         p.vy = 0.07F + this.rnd.nextFloat() * 0.05F;
         p.grav = 0.05F;
         p.size = 0.025F + this.rnd.nextFloat() * 0.015F;
         p.life = 4 + this.rnd.nextInt(3);
         p.r = 0.7F;
         p.g = 0.82F;
         p.b = 1.0F;
      }

      this.particles.add(p);
   }

   /** One spark (or a puff of smoke) of a fire or a burning creature. */
   void flameParticle(double x, double y, double z, boolean smoke) {
      Game.Particle p = new Game.Particle();
      p.x = (float)x;
      p.y = (float)y;
      p.z = (float)z;
      p.vx = (this.rnd.nextFloat() - 0.5F) * 0.02F;
      p.vz = (this.rnd.nextFloat() - 0.5F) * 0.02F;
      p.u = 1.0F / 256.0F;
      p.v = 65.0F / 256.0F;
      p.shade = 1.0F;
      p.grav = -0.002F;
      if (smoke) {
         p.vy = 0.03F + this.rnd.nextFloat() * 0.02F;
         p.size = 0.08F + this.rnd.nextFloat() * 0.06F;
         p.life = 22 + this.rnd.nextInt(14);
         float g = 0.22F + this.rnd.nextFloat() * 0.2F;
         p.r = g;
         p.g = g;
         p.b = g;
      } else {
         p.vy = 0.04F + this.rnd.nextFloat() * 0.03F;
         p.size = 0.07F + this.rnd.nextFloat() * 0.05F;
         p.life = 10 + this.rnd.nextInt(8);
         p.r = 1.0F;
         p.g = 0.45F + this.rnd.nextFloat() * 0.4F;
         p.b = 0.08F;
      }

      this.particles.add(p);
   }

   void explosionParticles(double var1, double var3, double var5) {
      int var7 = Blocks.tiles[4][2];
      int var8 = var7 % 16 * 16;
      int var9 = var7 / 16 * 16;

      for (int var10 = 0; var10 < 60; var10++) {
         Game.Particle var11 = new Game.Particle();
         var11.x = (float)var1 + (this.rnd.nextFloat() - 0.5F) * 2.0F;
         var11.y = (float)var3 + (this.rnd.nextFloat() - 0.5F) * 2.0F;
         var11.z = (float)var5 + (this.rnd.nextFloat() - 0.5F) * 2.0F;
         var11.vx = (this.rnd.nextFloat() - 0.5F) * 0.4F;
         var11.vy = this.rnd.nextFloat() * 0.3F;
         var11.vz = (this.rnd.nextFloat() - 0.5F) * 0.4F;
         var11.size = 0.15F + this.rnd.nextFloat() * 0.12F;
         var11.u = (var8 + this.rnd.nextInt(12)) / 256.0F;
         var11.v = (var9 + this.rnd.nextInt(12)) / 256.0F;
         var11.life = 20 + this.rnd.nextInt(20);
         var11.shade = 1.0F;
         this.particles.add(var11);
      }
   }

   void damageHeld(int var1) {
      Stack var2 = this.held();
      if (var2 != null && Items.maxDamage(var2.id) != 0 && !this.player.creative) {
         var2.dmg += var1;
         if (var2.dmg >= Items.maxDamage(var2.id)) {
            this.inv[this.sel] = null;
         }
      }
   }

   void breakBlock(int var1, int var2, int var3) {
      int var4 = this.world.getBlock(var1, var2, var3);
      if (Blocks.breakable(var4)) {
         int var5 = this.heldId();
         this.spawnBreakParticles(var1, var2, var3, var4, 16);
         boolean var6 = var4 == 69 || var4 == 70 || var4 == 67 || var4 == 68;
         if (var4 == 70 && !this.player.creative) {
            this.world.getOrCreateTile(var1, var2, var3);
         }

         if (var6) {
            this.world.removeTile(var1, var2, var3, !this.player.creative);
         }

         if (Blocks.isBed(var4)) {
            boolean var7 = Blocks.isBedHead(var4);
            int var8 = var4 - 133 & 3;
            int var9 = var8 == 2 ? 1 : (var8 == 3 ? -1 : 0);
            int var10 = var8 == 0 ? -1 : (var8 == 1 ? 1 : 0);
            int var11 = var7 ? var1 - var9 : var1 + var9;
            int var12 = var7 ? var3 - var10 : var3 + var10;
            this.world.setBlock(var11, var2, var12, 0);
            this.meshNow(var11, var2, var12);
            if (var7 && !this.player.creative) {
               this.world.dropItem(var1 + 0.5, var2 + 0.3, var3 + 0.5, 289, 1);
            }
         }

         this.world.setBlock(var1, var2, var3, 0);
         if (Blocks.isDoor(var4)) {
            int var13 = Blocks.isDoorLo(var4) ? var2 + 1 : var2 - 1;
            if (Blocks.isDoor(this.world.getBlock(var1, var13, var3))) {
               this.world.setBlock(var1, var13, var3, 0);
               this.meshNow(var1, var13, var3);
            }

            if (Blocks.isDoorHi(var4)) {
               var4 = 73;
            }
         }

         if (!this.player.creative) {
            this.blockDrops(var1, var2, var3, var4, var5);
            this.player.addExhaustion(0.025F);
            if (Blocks.hardness[var4] > 0.0F) {
               this.damageHeld(Items.isTool(var5) && Items.toolClass(var5) == 4 ? 2 : 1);
            }
         }

         this.meshNow(var1, var2, var3);
      }
   }

   int doorSide(double var1, double var3) {
      if (Math.abs(var3) >= Math.abs(var1)) {
         return var3 > 0.0 ? 1 : 0;
      } else {
         return var1 > 0.0 ? 2 : 3;
      }
   }

   /** Plants wheat seeds / a carrot / a potato on the top face of the targeted farmland. */
   boolean placeSeeds(Stack var1) {
      int var3 = Items.plantBlock(var1.id);
      if (var3 != 0 && this.target.ny == 1 && Blocks.isFarmland(this.world.getBlock(this.target.x, this.target.y, this.target.z))) {
         int var2 = this.target.y + 1;
         if (!Blocks.replaceable[this.world.getBlock(this.target.x, var2, this.target.z)]) {
            return false;
         }

         this.world.setBlock(this.target.x, var2, this.target.z, var3);
         this.meshNow(this.target.x, var2, this.target.z);
         if (!this.player.creative) {
            var1.n--;
            if (var1.n <= 0) {
               this.inv[this.sel] = null;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   /**
    * Bone meal: instantly grows the targeted crop by 2-5 stages, or sprinkles tall grass and the odd flower around
    * a targeted grass block.
    */
   boolean useBoneMeal(Stack var1) {
      if (this.target == null || this.player.adventure) {
         return false;
      }

      int var2 = this.target.x;
      int var3 = this.target.y;
      int var4 = this.target.z;
      int var5 = this.world.getBlock(var2, var3, var4);
      if (Blocks.isFarmland(var5) && (Blocks.isCrop(this.world.getBlock(var2, var3 + 1, var4)) || Blocks.isStem(this.world.getBlock(var2, var3 + 1, var4)))) {
         var3++;
         var5 = this.world.getBlock(var2, var3, var4);
      }

      if (Blocks.isStem(var5) && Blocks.stemStage(var5) < 7) {
         int stage = Math.min(7, Blocks.stemStage(var5) + 2 + this.rnd.nextInt(4));
         this.world.setBlock(var2, var3, var4, var5 - Blocks.stemStage(var5) + stage);
         this.meshNow(var2, var3, var4);
      } else if (Blocks.isCrop(var5) && !Blocks.isRipe(var5)) {
         int var6 = Math.min(7, Blocks.cropStage(var5) + 2 + this.rnd.nextInt(4));
         this.world.setBlock(var2, var3, var4, Blocks.cropBase(var5) + var6);
         this.meshNow(var2, var3, var4);
      } else {
         if (var5 != 2 || this.target.ny != 1 || this.world.getBlock(var2, var3 + 1, var4) != 0) {
            return false;
         }

         for (int var7 = 0; var7 < 24; var7++) {
            int var8 = var2 + this.rnd.nextInt(7) - 3;
            int var9 = var4 + this.rnd.nextInt(7) - 3;
            int var10 = var3 + this.rnd.nextInt(3) - 1;
            if (this.world.getBlock(var8, var10, var9) == 2 && this.world.getBlock(var8, var10 + 1, var9) == 0) {
               this.world.setBlock(var8, var10 + 1, var9, this.rnd.nextInt(8) == 0 ? 30 + this.rnd.nextInt(2) : 29);
               this.meshNow(var8, var10 + 1, var9);
            }
         }
      }

      if (!this.player.creative) {
         var1.n--;
         if (var1.n <= 0) {
            this.inv[this.sel] = null;
         }
      }

      return true;
   }

   /** Right-click on a mob with an item in hand: shear a sheep, or feed an animal (breeding / growing up). */
   boolean useOnMob(Mob var1, Stack var2) {
      if (var2 == null || var1.dead || this.player.adventure || this.player.spectator) {
         return false;
      }

      if (var2.id == Items.SHEARS && this.mobs.shear(var1)) {
         if (!this.player.creative) {
            this.damageHeld(1);
         }

         return true;
      }

      if (var2.id == Items.BUCKET && var1.type == Mobs.COW && var1.babyAge == 0 && !this.player.creative) {
         var2.n--;
         if (var2.n <= 0) {
            this.inv[this.sel] = new Stack(Items.MILK_BUCKET, 1);
         } else if (!this.addItem(new Stack(Items.MILK_BUCKET, 1))) {
            this.dropFromPlayer(Items.MILK_BUCKET, 1);
         }

         return true;
      }

      if (this.mobs.feed(var1, var2.id)) {
         if (!this.player.creative) {
            var2.n--;
            if (var2.n <= 0) {
               this.inv[this.sel] = null;
            }
         }

         this.loveParticles(var1);
         return true;
      }

      return false;
   }

   /** A few red particles rising from the mob (breeding "hearts"). */
   void loveParticles(Mob var1) {
      for (int var2 = 0; var2 < 7; var2++) {
         Game.Particle var3 = new Game.Particle();
         var3.x = (float)(var1.x + (this.rnd.nextFloat() - 0.5F) * Mobs.W[var1.type]);
         var3.y = (float)(var1.y + Mobs.H[var1.type] * (0.6F + this.rnd.nextFloat() * 0.6F));
         var3.z = (float)(var1.z + (this.rnd.nextFloat() - 0.5F) * Mobs.W[var1.type]);
         var3.vx = (this.rnd.nextFloat() - 0.5F) * 0.05F;
         var3.vy = 0.05F + this.rnd.nextFloat() * 0.08F;
         var3.vz = (this.rnd.nextFloat() - 0.5F) * 0.05F;
         var3.size = 0.09F + this.rnd.nextFloat() * 0.04F;
         var3.u = 1.0F / 256.0F;
         var3.v = 65.0F / 256.0F;
         var3.life = 25 + this.rnd.nextInt(15);
         var3.shade = 1.0F;
         var3.r = 1.0F;
         var3.g = 0.25F;
         var3.b = 0.4F;
         this.particles.add(var3);
      }
   }

   /**
    * Which side a carved pumpkin / jack o'lantern shows its face on when it looks at the player:
    * 0 = north (-z), 1 = south (+z), 2 = east (+x), 3 = west (-x).
    */
   int carveFacing() {
      double yaw = Math.toRadians(this.player.yaw);
      double dx = -Math.sin(yaw);
      double dz = Math.cos(yaw);
      if (Math.abs(dz) >= Math.abs(dx)) {
         return dz > 0.0 ? 0 : 1;
      } else {
         return dx > 0.0 ? 3 : 2;
      }
   }

   /** Puts a cake on top of a solid block (or into the free cell next to the clicked face). */
   boolean placeCake(Stack var1) {
      int x = this.target.x;
      int y = this.target.y;
      int z = this.target.z;
      if (!Blocks.replaceable[this.world.getBlock(x, y, z)]) {
         x += this.target.nx;
         y += this.target.ny;
         z += this.target.nz;
      }

      if (y >= 1 && y <= 254 && Blocks.replaceable[this.world.getBlock(x, y, z)] && Blocks.solid[this.world.getBlock(x, y - 1, z)]) {
         double[] bb = this.player.bb;
         double[] c = Blocks.coll[Blocks.CAKE];
         if (bb[3] > x + c[0] && bb[0] < x + c[3] && bb[4] > y + c[1] && bb[1] < y + c[4] && bb[5] > z + c[2] && bb[2] < z + c[5]) {
            return false;
         }

         if (!this.world.setBlock(x, y, z, Blocks.CAKE)) {
            return false;
         }

         this.meshNow(x, y, z);
         if (!this.player.creative) {
            var1.n--;
            if (var1.n <= 0) {
               this.inv[this.sel] = null;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   /** Right-click on a cake: eat one slice (+2 hunger) if you are hungry; the last slice makes the cake disappear. */
   boolean eatCake() {
      int x = this.target.x;
      int y = this.target.y;
      int z = this.target.z;
      int id = this.world.getBlock(x, y, z);
      if (!Blocks.isCake(id) || this.player.food >= 20 || this.player.adventure) {
         return false;
      }

      this.player.food = Math.min(20, this.player.food + 2);
      this.player.sat = Math.min(this.player.food, this.player.sat + 0.4F);
      this.world.setBlock(x, y, z, id >= Blocks.CAKE + 6 ? 0 : id + 1);
      this.meshNow(x, y, z);
      return true;
   }

   /** Shears on a pumpkin: carve a face into the clicked side (or towards you if you clicked top / bottom), 4 seeds drop. */
   boolean carvePumpkin() {
      int x = this.target.x;
      int y = this.target.y;
      int z = this.target.z;
      int f;
      if (this.target.ny != 0) {
         f = this.carveFacing();
      } else {
         f = this.target.nz == -1 ? 0 : (this.target.nz == 1 ? 1 : (this.target.nx == 1 ? 2 : 3));
      }

      this.world.setBlock(x, y, z, Blocks.CARVED_PUMPKIN + f);
      this.meshNow(x, y, z);
      this.world.dropItem(x + 0.5 + this.target.nx * 0.65, y + 0.5 + this.target.ny * 0.65, z + 0.5 + this.target.nz * 0.65, Items.PUMPKIN_SEEDS, 4);
      this.damageHeld(1);
      return true;
   }

   boolean placeBed(Stack var1) {
      int var2 = this.target.x;
      int var3 = this.target.y;
      int var4 = this.target.z;
      if (!Blocks.replaceable[this.world.getBlock(var2, var3, var4)]) {
         var2 += this.target.nx;
         var3 += this.target.ny;
         var4 += this.target.nz;
      }

      if (var3 >= 1 && var3 <= 254) {
         double var5 = Math.toRadians(this.player.yaw);
         int var7 = this.doorSide(-Math.sin(var5), Math.cos(var5));
         int var8 = var7 == 2 ? 1 : (var7 == 3 ? -1 : 0);
         int var9 = var7 == 0 ? -1 : (var7 == 1 ? 1 : 0);
         int var10 = var2 + var8;
         int var11 = var4 + var9;
         if (!Blocks.replaceable[this.world.getBlock(var2, var3, var4)] || !Blocks.replaceable[this.world.getBlock(var10, var3, var11)]) {
            return false;
         }

         if (Blocks.solid[this.world.getBlock(var2, var3 - 1, var4)] && Blocks.solid[this.world.getBlock(var10, var3 - 1, var11)]) {
            this.world.setBlock(var2, var3, var4, 133 + var7);
            this.world.setBlock(var10, var3, var11, 137 + var7);
            this.meshNow(var2, var3, var4);
            this.meshNow(var10, var3, var11);
            if (!this.player.creative) {
               var1.n--;
               if (var1.n <= 0) {
                  this.inv[this.sel] = null;
               }
            }

            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   void sleepAt(int var1, int var2, int var3) {
      int var4 = this.world.getBlock(var1, var2, var3);
      int var5 = var4 - 133 & 3;
      int var6 = var5 == 2 ? 1 : (var5 == 3 ? -1 : 0);
      int var7 = var5 == 0 ? -1 : (var5 == 1 ? 1 : 0);
      int var8 = Blocks.isBedHead(var4) ? var1 - var6 : var1;
      int var9 = Blocks.isBedHead(var4) ? var3 - var7 : var3;
      long var10 = this.world.dayTime % 24000L;
      boolean var12 = var10 >= 12541L && var10 < 23459L || this.world.weather.thunderingAt(this.player.x, this.player.z);
      if (!var12) {
         this.bedPos = new int[]{var8, var2, var9};
         this.say("You can only sleep at night or in a thunderstorm (respawn point set)");
      } else {
         for (Mob var14 : this.mobs.list) {
            if (!var14.dead && var14.type >= 4 && Math.abs(var14.x - var1) < 8.0 && Math.abs(var14.z - var3) < 8.0 && Math.abs(var14.y - var2) < 5.0) {
               this.say("You may not rest now, there are monsters nearby");
               return;
            }
         }

         this.bedPos = new int[]{var8, var2, var9};
         this.world.dayTime = (this.world.dayTime / 24000L + 1L) * 24000L + 100L;
         this.world.weather.natural(); // sleeping through the night clears the sky: fair weather for the next few minutes
         this.world.weather.calmUntil = this.world.weather.ticks + 7200L;
         this.player.health = Math.max(this.player.health, Math.min(20, this.player.health + 4));
         this.say("Good morning! Respawn point set.");
      }
   }

   boolean useSpecial(Stack var1) {
      int var2 = var1.id;
      double var3 = Math.toRadians(this.player.yaw);
      double var5 = Math.toRadians(this.player.pitch);
      double var7 = -Math.sin(var3) * Math.cos(var5);
      double var9 = -Math.sin(var5);
      double var11 = Math.cos(var3) * Math.cos(var5);
      double var13 = this.player.creative ? 5.0 : 4.5;
      if (var2 == 336 || var2 == 337) {
         return this.target != null && !this.player.adventure && this.placeSeeds(var1);
      }

      if (var2 == 339) {
         return this.useBoneMeal(var1);
      }

      if (var2 == 282) {
         this.world.rayFluids = true;
         Hit var19 = this.world.raycast(this.player.x, this.player.y + this.player.eye, this.player.z, var7, var9, var11, var13);
         this.world.rayFluids = false;
         if (var19 == null) {
            return false;
         }

         int var21 = this.world.getBlock(var19.x, var19.y, var19.z);
         if (Blocks.isFluid(var21) && Blocks.fluidLevel(var21) == 0) {
            int var22 = Blocks.isWater(var21) ? 283 : 284;
            this.world.setBlock(var19.x, var19.y, var19.z, 0);
            this.meshNow(var19.x, var19.y, var19.z);
            var1.n--;
            if (var1.n <= 0) {
               this.inv[this.sel] = new Stack(var22, 1);
            } else {
               this.addItem(new Stack(var22, 1));
            }

            return true;
         } else {
            return false;
         }
      } else if (var2 != 283 && var2 != 284) {
         if (!Items.isHoe(var2)) {
            if (var2 != 288) {
               return false;
            }

            if (this.target != null && !this.player.adventure) {
               if (this.world.getBlock(this.target.x, this.target.y, this.target.z) != 132) {
                  // Update 3.7: light a fire in the free cell next to the clicked face
                  int fx = this.target.x + this.target.nx;
                  int fy = this.target.y + this.target.ny;
                  int fz = this.target.z + this.target.nz;
                  if (!this.world.fire.ignite(fx, fy, fz)) {
                     return false;
                  }

                  this.meshNow(fx, fy, fz);
                  this.damageHeld(1);
                  return true;
               }

               this.world.setBlock(this.target.x, this.target.y, this.target.z, 0);
               this.meshNow(this.target.x, this.target.y, this.target.z);
               this.world.primeTnt(this.target.x, this.target.y, this.target.z, 80);
               this.damageHeld(1);
               return true;
            } else {
               return false;
            }
         } else if (this.target != null && !this.player.adventure) {
            int var18 = this.world.getBlock(this.target.x, this.target.y, this.target.z);
            if ((var18 == 2 || var18 == 3) && this.target.ny >= 0) {
               int var20 = this.world.getBlock(this.target.x, this.target.y + 1, this.target.z);
               if (var20 != 0 && !Blocks.replaceable[var20]) {
                  return false;
               }

               if (var20 != 0) {
                  this.world.setBlock(this.target.x, this.target.y + 1, this.target.z, 0);
               }

               this.world.setBlock(this.target.x, this.target.y, this.target.z, 123);
               this.meshNow(this.target.x, this.target.y, this.target.z);
               this.meshNow(this.target.x, this.target.y + 1, this.target.z);
               this.damageHeld(1);
               return true;
            } else {
               return false;
            }
         } else {
            return false;
         }
      } else if (this.target != null && !this.player.adventure) {
         int var15 = this.target.x;
         int var16 = this.target.y;
         int var17 = this.target.z;
         if (!Blocks.replaceable[this.world.getBlock(var15, var16, var17)]) {
            var15 += this.target.nx;
            var16 += this.target.ny;
            var17 += this.target.nz;
         }

         if (var16 >= 0 && var16 <= 255 && Blocks.replaceable[this.world.getBlock(var15, var16, var17)]) {
            this.world.setBlock(var15, var16, var17, var2 == 283 ? 50 : 58);
            this.meshNow(var15, var16, var17);
            if (!this.player.creative) {
               this.inv[this.sel] = new Stack(282, 1);
            }

            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   boolean placeDoor(Stack var1) {
      int var2 = this.target.x;
      int var3 = this.target.y;
      int var4 = this.target.z;
      if (!Blocks.replaceable[this.world.getBlock(var2, var3, var4)]) {
         var2 += this.target.nx;
         var3 += this.target.ny;
         var4 += this.target.nz;
      }

      if (var3 >= 1 && var3 <= 253) {
         if (Blocks.replaceable[this.world.getBlock(var2, var3, var4)] && Blocks.replaceable[this.world.getBlock(var2, var3 + 1, var4)]) {
            if (Blocks.solid[this.world.getBlock(var2, var3 - 1, var4)] && !Blocks.isDoor(this.world.getBlock(var2, var3 - 1, var4))) {
               double var5 = Math.toRadians(this.player.yaw);
               int var7 = this.doorSide(-Math.sin(var5), Math.cos(var5));
               double[] var8 = Blocks.coll[73 + var7];
               double[] var9 = this.player.bb;

               for (int var10 = 0; var10 < 2; var10++) {
                  if (var9[3] > var2 + var8[0]
                     && var9[0] < var2 + var8[3]
                     && var9[4] > var3 + var10
                     && var9[1] < var3 + var10 + 1
                     && var9[5] > var4 + var8[2]
                     && var9[2] < var4 + var8[5]) {
                     return false;
                  }
               }

               this.world.setBlock(var2, var3, var4, 73 + var7);
               this.world.setBlock(var2, var3 + 1, var4, 81 + var7);
               this.meshNow(var2, var3, var4);
               this.meshNow(var2, var3 + 1, var4);
               if (!this.player.creative) {
                  var1.n--;
                  if (var1.n <= 0) {
                     this.inv[this.sel] = null;
                  }
               }

               return true;
            } else {
               return false;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   void toggleDoor(int var1, int var2, int var3) {
      int var4 = this.world.getBlock(var1, var2, var3);
      int var5 = Blocks.isDoorLo(var4) ? var2 : var2 - 1;
      int var6 = this.world.getBlock(var1, var5, var3);
      int var7 = this.world.getBlock(var1, var5 + 1, var3);
      if (Blocks.isDoorLo(var6) && Blocks.isDoorHi(var7)) {
         int var8 = Blocks.doorOpen(var6) ? -4 : 4;
         this.world.setBlock(var1, var5, var3, var6 + var8);
         this.world.setBlock(var1, var5 + 1, var3, var7 + var8);
         this.meshNow(var1, var5, var3);
         this.meshNow(var1, var5 + 1, var3);
      }
   }

   boolean placeBlock() {
      Stack var1 = this.inv[this.sel];
      if (var1 != null && var1.id == 281) {
         return this.placeDoor(var1);
      }

      if (var1 != null && var1.id == 289) {
         return this.placeBed(var1);
      }

      if (var1 != null && var1.id == Items.CAKE) {
         return this.placeCake(var1);
      }

      if (var1 != null && Items.plantBlock(var1.id) != 0) {
         return this.placeSeeds(var1);
      }

      if (var1 != null && var1.id < 256) {
         int var2 = var1.id;
         int var3 = this.target.x;
         int var4 = this.target.y;
         int var5 = this.target.z;
         int var6 = this.world.getBlock(var3, var4, var5);
         if (Blocks.isSlab(var2) && var6 == var2 && this.target.ny == 1) {
            this.world.setBlock(var3, var4, var5, Blocks.STAIR_MAT[var2 - 113]);
            this.meshNow(var3, var4, var5);
            if (!this.player.creative) {
               var1.n--;
               if (var1.n <= 0) {
                  this.inv[this.sel] = null;
               }
            }

            return true;
         } else {
            boolean var7 = Blocks.replaceable[var6];
            int var8 = var7 ? 0 : this.target.nx;
            int var9 = var7 ? 1 : this.target.ny;
            int var10 = var7 ? 0 : this.target.nz;
            if (!Blocks.replaceable[var6]) {
               var3 += this.target.nx;
               var4 += this.target.ny;
               var5 += this.target.nz;
            }

            if (var4 >= 0 && var4 <= 255) {
               int var11 = this.world.getBlock(var3, var4, var5);
               if (!Blocks.replaceable[var11]) {
                  return false;
               }

               if (Blocks.coll[var2] != null) {
                  double[] var12 = this.player.bb;
                  double[] var13 = Blocks.coll[var2];
                  if (var12[3] > var3 + var13[0]
                     && var12[0] < var3 + var13[3]
                     && var12[4] > var4 + var13[1]
                     && var12[1] < var4 + var13[4]
                     && var12[5] > var5 + var13[2]
                     && var12[2] < var5 + var13[5]) {
                     return false;
                  }
               }

               int var18 = this.world.getBlock(var3, var4 - 1, var5);
               if (var2 == 32) {
                  if (var9 == 1) {
                     if (!Blocks.solid[var18]) {
                        return false;
                     }
                  } else {
                     if (var9 == -1) {
                        return false;
                     }

                     int var19 = var3 - var8;
                     int var14 = var5 - var10;
                     if (!Blocks.solid[this.world.getBlock(var19, var4, var14)] || !Blocks.opaque[this.world.getBlock(var19, var4, var14)]) {
                        return false;
                     }

                     var2 = 89 + (var10 == 1 ? 0 : (var10 == -1 ? 1 : (var8 == 1 ? 3 : 2)));
                  }
               } else if (var2 == 119) {
                  if (var9 != 0) {
                     return false;
                  }

                  int var20 = var3 - var8;
                  int var22 = var5 - var10;
                  if (!Blocks.solid[this.world.getBlock(var20, var4, var22)] || !Blocks.opaque[this.world.getBlock(var20, var4, var22)]) {
                     return false;
                  }

                  var2 = 119 + (var10 == 1 ? 0 : (var10 == -1 ? 1 : (var8 == 1 ? 3 : 2)));
               } else if (Blocks.kind[var2] == 1 && !Blocks.solid[var18] && !(var2 == Blocks.SUGAR_CANE && var18 == Blocks.SUGAR_CANE)) {
                  return false;
               }

               if (Blocks.isStair(var2)) {
                  double var21 = -Math.sin(Math.toRadians(this.player.yaw));
                  double var15 = Math.cos(Math.toRadians(this.player.yaw));
                  int var17 = Math.abs(var15) >= Math.abs(var21) ? (var15 > 0.0 ? 1 : 0) : (var21 > 0.0 ? 2 : 3);
                  var2 = var2 - (var2 - 93 & 3) + var17;
               }

               if (Blocks.isCarvedPumpkin(var2) || Blocks.isJackOLantern(var2)) {
                  var2 = (Blocks.isCarvedPumpkin(var2) ? 162 : 166) + this.carveFacing();
               }

               if (var2 == Blocks.SUGAR_CANE && !this.world.canCaneStay(var3, var4, var5)) {
                  return false;
               }

               if (var2 == 28 && var18 != 7 && var18 != 28) {
                  return false;
               }

               if (!this.world.setBlock(var3, var4, var5, var2)) {
                  return false;
               }

               this.meshNow(var3, var4, var5);
               if (!this.player.creative) {
                  var1.n--;
                  if (var1.n <= 0) {
                     this.inv[this.sel] = null;
                  }
               }

               return true;
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   void pickupItems() {
      if (!this.player.dead && !this.player.spectator) {
         for (Item var2 : this.world.items) {
            if (!var2.dead && var2.delay <= 0) {
               double var3 = var2.x - this.player.x;
               double var5 = var2.z - this.player.z;
               double var7 = var2.y - (this.player.y + 0.5);
               if (var3 * var3 + var5 * var5 < 1.44 && Math.abs(var7) < 1.3) {
                  Stack var9 = new Stack(var2.id, var2.count, var2.dmg);
                  this.addItem(var9);
                  if (var9.n <= 0) {
                     var2.dead = true;
                  } else {
                     var2.count = var9.n;
                  }
               }
            }
         }
      }
   }

   void spawnBreakParticles(int var1, int var2, int var3, int var4, int var5) {
      int var6 = Blocks.tiles[var4][2];
      if (Blocks.kind[var4] == 1 || Blocks.kind[var4] == 7 || Blocks.isLadder(var4)) {
         var6 = Blocks.tiles[var4][0];
      }

      int var7 = var6 % 16 * 16;
      int var8 = var6 / 16 * 16;
      int[] var9 = Gui.tintFor(var4);

      for (int var10 = 0; var10 < var5; var10++) {
         Game.Particle var11 = new Game.Particle();
         var11.x = var1 + this.rnd.nextFloat();
         var11.y = var2 + this.rnd.nextFloat();
         var11.z = var3 + this.rnd.nextFloat();
         var11.vx = (this.rnd.nextFloat() - 0.5F) * 0.2F;
         var11.vy = this.rnd.nextFloat() * 0.2F;
         var11.vz = (this.rnd.nextFloat() - 0.5F) * 0.2F;
         var11.size = 0.07F + this.rnd.nextFloat() * 0.05F;
         var11.u = (var7 + this.rnd.nextInt(12)) / 256.0F;
         var11.v = (var8 + this.rnd.nextInt(12)) / 256.0F;
         var11.life = 15 + this.rnd.nextInt(20);
         var11.shade = 0.6F;
         if (var9 != null && Blocks.tint[var4] == 2) {
            var11.r = var9[0] / 255.0F;
            var11.g = var9[1] / 255.0F;
            var11.b = var9[2] / 255.0F;
         }

         this.particles.add(var11);
      }
   }

   int guiScaleAuto() {
      if (this.opt.guiScale > 0) {
         return this.opt.guiScale;
      }

      int var1 = Math.min(this.fbW / 320, this.fbH / 240);
      return Math.max(1, Math.min(6, var1));
   }

   void updateZoom() {
      long var1 = System.nanoTime();
      float var3 = Math.min(0.1F, (float)(var1 - this.zoomLast) / 1.0E9F);
      this.zoomLast = var1;
      boolean var4 = this.screen == Game.S.PLAY && (this.key(67) || Boolean.getBoolean("bc.zoom"));
      float var5 = var4 ? 1.0F : 0.0F;
      this.zoomAmt = this.zoomAmt + (var5 - this.zoomAmt) * Math.min(1.0F, var3 * 14.0F);
      if (Math.abs(var5 - this.zoomAmt) < 0.002F) {
         this.zoomAmt = var5;
      }
   }

   public float zoomDiv() {
      return 1.0F + (this.zoomFactor - 1.0F) * this.zoomAmt;
   }

   void render() {
      this.updateZoom();
      int var1 = this.guiScaleAuto();
      GL11.glViewport(0, 0, this.fbW, this.fbH);
      this.activeBtnsSwap();
      boolean var2 = this.world != null && this.screen != Game.S.LOADING;
      if (var2) {
         this.rend.computeSky(this.world.dayTime, this.partial);
         GL13.glActiveTexture(33984);
         Tex.animate(this.tickCount);
         boolean var3 = this.rend.shadersOn();
         if (var3) {
            this.rend.sh.beginScene(this.fbW, this.fbH);
         }

         this.rend.renderWorld(this.partial, this.fbW, this.fbH);
         if (var3) {
            this.rend.sh.skyMask(this.fbW, this.fbH);
         }

         if (this.screenIsPlay() && !this.hideGui && this.screen != Game.S.DEAD && !this.player.spectator) {
            float var4 = this.swingPrev + (this.swing - this.swingPrev) * this.partial;
            float var5 = this.rend.brightnessAt(this.world, this.player.x, this.player.y + 1.0, this.player.z);
            this.rend
               .renderHand(
                  this.partial,
                  this.inv[this.sel],
                  this.swinging ? var4 : 0.0F,
                  this.eatTicks > 0 ? (this.eatTicks + this.partial) / 32.0F : -1.0F,
                  var5,
                  this.fbW,
                  this.fbH
               );
         }

         if (var3) {
            this.rend.sh.endScene(this.fbW, this.fbH, 0.45F);
         }
      } else {
         GL11.glClearColor(0.0F, 0.0F, 0.0F, 1.0F);
         GL11.glClear(16640);
      }

      this.gui.begin(this.fbW, this.fbH, var1);
      switch (this.screen) {
         case TITLE:
            this.drawTitle();
            break;
         case WORLDS:
            this.drawWorlds();
            break;
         case NEWWORLD:
            this.drawNewWorld();
            break;
         case LOADING:
            this.drawLoading();
            break;
         case CHAT:
         case INV:
         case PAUSE:
         default:
            this.drawHud();
            if (this.screen == Game.S.PAUSE) {
               this.drawPause();
            } else if (this.screen == Game.S.INV) {
               this.drawInventory();
            } else if (this.screen == Game.S.DEAD) {
               this.drawDead();
            }
            break;
         case OPTIONS:
            if (this.world != null) {
               this.gui.rect(0.0F, 0.0F, this.gui.w, this.gui.h, -1879048192);
            } else {
               this.gui.dirtBackground(0.25F);
            }

            this.drawOptions();
      }

      this.activeBtns = this.btns;
   }

   void activeBtnsSwap() {
      this.btns = new ArrayList();
   }

   boolean hover(float var1, float var2, float var3, float var4) {
      float var5 = (float)(this.mouseX / this.gui.scale);
      float var6 = (float)(this.mouseY / this.gui.scale);
      return var5 >= var1 && var5 <= var1 + var3 && var6 >= var2 && var6 <= var2 + var4 && !this.grabbed;
   }

   void button(String var1, float var2, float var3, float var4, float var5, boolean var6, Runnable var7) {
      this.gui.button(var2, var3, var4, var5, var1, var6 && this.hover(var2, var3, var4, var5), var6);
      Game.Btn var8 = new Game.Btn();
      var8.x = var2;
      var8.y = var3;
      var8.w = var4;
      var8.h = var5;
      var8.action = var6 ? var7 : null;
      this.btns.add(var8);
   }

   void slider(String var1, float var2, float var3, float var4, float var5, float var6, Consumer<Float> var7) {
      this.gui.slider(var2, var3, var4, var5, var1, var6, this.hover(var2, var3, var4, var5) || this.dragging >= 0 && this.dragging == this.btns.size());
      Game.Btn var8 = new Game.Btn();
      var8.x = var2;
      var8.y = var3;
      var8.w = var4;
      var8.h = var5;
      var8.slider = var7;
      this.btns.add(var8);
   }

   void drawTitle() {
      this.gui.dirtBackground(0.3F);
      this.gui.gradient(0.0F, 0.0F, this.gui.w, this.gui.h, 1073741824, -1610612736);
      float var1 = this.gui.w / 2.0F;
      float var2 = Math.max(20.0F, this.gui.h * 0.14F);
      String var3 = "BLOCKCRAFT";
      float var4 = 4.0F;
      float var5 = Gui.textWidth(var3) * var4;
      this.gui.text(var3, var1 - var5 / 2.0F + 3.0F, var2 + 3.0F, -14013910, false, var4);
      this.gui.text(var3, var1 - var5 / 2.0F, var2, -1118482, false, var4);
      this.gui.text(var3, var1 - var5 / 2.0F + 1.0F, var2 + 1.0F, -5197648, false, var4);
      this.gui.text(var3, var1 - var5 / 2.0F, var2, -1, false, var4);
      this.gui.centered("JAVA EDITION  1.12  -  UPDATE " + VERSION_STR, var1, var2 + 8.0F * var4 + 4.0F, -5592406, true, 1.0F);
      GL11.glPushMatrix();
      GL11.glTranslatef(var1 + var5 / 2.0F - 20.0F, var2 + 8.0F * var4 + 4.0F, 0.0F);
      GL11.glRotatef(-20.0F, 0.0F, 0.0F, 1.0F);
      float var6 = 1.0F + 0.06F * (float)Math.sin(GLFW.glfwGetTime() * 6.0);
      GL11.glScalef(var6, var6, 1.0F);
      this.gui.centered(this.splash, 0.0F, 0.0F, -256, true, 1.0F);
      GL11.glPopMatrix();
      float var7 = 200.0F;
      float var8 = 20.0F;
      float var9 = Math.max(var2 + 8.0F * var4 + 40.0F, this.gui.h * 0.42F);
      this.button("Singleplayer", var1 - var7 / 2.0F, var9, var7, var8, true, () -> {
         this.scanWorlds();
         this.worldScroll = 0;
         this.confirmDelete = false;
         if (this.worldList.isEmpty()) {
            this.seedField = "";
            this.newName = "";
            this.focusField = 0;
            this.setScreen(Game.S.NEWWORLD);
         } else {
            this.setScreen(Game.S.WORLDS);
         }
      });
      this.button("Create New World", var1 - var7 / 2.0F, var9 + 24.0F, var7, var8, true, () -> {
         this.seedField = "";
         this.newName = "";
         this.focusField = 0;
         this.setScreen(Game.S.NEWWORLD);
      });
      this.button("Options...", var1 - var7 / 2.0F, var9 + 52.0F, 98.0F, var8, true, () -> {
         this.optionsReturn = Game.S.TITLE;
         this.setScreen(Game.S.OPTIONS);
      });
      this.button("Quit Game", var1 + 2.0F, var9 + 52.0F, 98.0F, var8, true, () -> GLFW.glfwSetWindowShouldClose(this.window, true));
      this.gui.text("Update " + VERSION_STR, 2.0F, this.gui.h - 10.0F, -1, true);
      String var10 = "Not affiliated with Mojang";
      this.gui.text(var10, this.gui.w - Gui.textWidth(var10) - 2.0F, this.gui.h - 10.0F, -1, true);
   }

   void hitbox(float var1, float var2, float var3, float var4, Runnable var5) {
      Game.Btn var6 = new Game.Btn();
      var6.x = var1;
      var6.y = var2;
      var6.w = var3;
      var6.h = var4;
      var6.action = var5;
      this.btns.add(var6);
   }

   void drawWorlds() {
      this.gui.dirtBackground(0.25F);
      float var1 = this.gui.w / 2.0F;
      this.gui.centered("Select World", var1, 14.0F, -1, true, 1.0F);
      float var2 = var1 - 130.0F;
      float var3 = 32.0F;
      float var4 = 260.0F;
      float var5 = 34.0F;
      int var6 = Math.max(2, Math.min(5, (int)((this.gui.h - 32.0F - 90.0F) / var5)));
      this.worldScroll = Math.max(0, Math.min(Math.max(0, this.worldList.size() - var6), this.worldScroll));
      this.gui.rect(var2 - 2.0F, var3 - 2.0F, var4 + 4.0F, var6 * var5 + 4.0F, -16777216);
      String[] var7 = new String[]{"Survival", "Creative", "Adventure", "Spectator"};

      for (int var8 = 0; var8 < var6; var8++) {
         int var9 = this.worldScroll + var8;
         if (var9 >= this.worldList.size()) {
            break;
         }

         Game.WInfo var10 = (Game.WInfo)this.worldList.get(var9);
         float var11 = var3 + var8 * var5;
         boolean var12 = var10.dir.equals(this.worldName);
         boolean var13 = this.hover(var2, var11, var4, var5 - 2.0F);
         this.gui.rect(var2, var11, var4, var5 - 2.0F, var12 ? -1 : (var13 ? -7303024 : -11513776));
         this.gui.rect(var2 + 1.0F, var11 + 1.0F, var4 - 2.0F, var5 - 4.0F, -14671840);
         this.gui.text(var10.name, var2 + 6.0F, var11 + 5.0F, -1, true);
         String var14 = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date(var10.mod));
         this.gui.text(var7[Math.max(0, Math.min(3, var10.mode))] + "   " + var14, var2 + 6.0F, var11 + 18.0F, -7303024, true);
         String var15 = var10.dir;
         this.hitbox(var2, var11, var4, var5 - 2.0F, () -> {
            if (var15.equals(this.worldName) && System.currentTimeMillis() - this.lastWorldClick < 400L) {
               this.startWorld(false, null, 1);
            }

            this.worldName = var15;
            this.confirmDelete = false;
            this.lastWorldClick = System.currentTimeMillis();
         });
      }

      if (this.worldList.size() > var6) {
         float var16 = var6 * var5;
         float var18 = var16 * var6 / this.worldList.size();
         float var19 = var3 + (var16 - var18) * this.worldScroll / Math.max(1, this.worldList.size() - var6);
         this.gui.rect(var2 + var4 + 4.0F, var3, 4.0F, var16, -13619152);
         this.gui.rect(var2 + var4 + 4.0F, var19, 4.0F, var18, -5197648);
      }

      float var17 = var3 + var6 * var5 + 10.0F;
      this.button("Play Selected World", var1 - 130.0F, var17, 260.0F, 20.0F, !this.worldList.isEmpty(), () -> this.startWorld(false, null, 1));
      this.button("Create New World", var1 - 130.0F, var17 + 24.0F, 127.0F, 20.0F, true, () -> {
         this.seedField = "";
         this.newName = "";
         this.focusField = 0;
         this.setScreen(Game.S.NEWWORLD);
      });
      this.button(this.confirmDelete ? "Really delete?" : "Delete", var1 + 3.0F, var17 + 24.0F, 127.0F, 20.0F, !this.worldList.isEmpty(), () -> {
         if (!this.confirmDelete) {
            this.confirmDelete = true;
         } else {
            deleteDir(new File(this.root, "saves/" + this.worldName));
            this.confirmDelete = false;
            this.scanWorlds();
            if (this.worldList.isEmpty()) {
               this.setScreen(Game.S.TITLE);
            }
         }
      });
      this.button("Cancel", var1 - 130.0F, var17 + 48.0F, 260.0F, 20.0F, true, () -> this.setScreen(Game.S.TITLE));
   }

   void drawNewWorld() {
      this.gui.dirtBackground(0.25F);
      float var1 = this.gui.w / 2.0F;
      this.gui.centered("Create New World", var1, 20.0F, -1, true, 1.0F);
      this.gui.text("World Name", var1 - 100.0F, 36.0F, -6250336, true);
      this.gui.rect(var1 - 100.0F, 47.0F, 200.0F, 20.0F, this.focusField == 0 ? -1 : -6250336);
      this.gui.rect(var1 - 99.0F, 48.0F, 198.0F, 18.0F, -16777216);
      this.gui
         .text(
            this.newName.isEmpty() && this.focusField != 0
               ? "New World"
               : this.newName + (this.focusField == 0 && (int)(GLFW.glfwGetTime() * 2.0) % 2 == 0 ? "_" : ""),
            var1 - 95.0F,
            53.0F,
            this.newName.isEmpty() ? -9408400 : -1,
            true
         );
      this.hitbox(var1 - 100.0F, 47.0F, 200.0F, 20.0F, () -> this.focusField = 0);
      this.gui.text("Seed for the World Generator", var1 - 100.0F, 74.0F, -6250336, true);
      this.gui.rect(var1 - 100.0F, 85.0F, 200.0F, 20.0F, this.focusField == 1 ? -1 : -6250336);
      this.gui.rect(var1 - 99.0F, 86.0F, 198.0F, 18.0F, -16777216);
      String var2 = this.seedField + (this.focusField == 1 && (int)(GLFW.glfwGetTime() * 2.0) % 2 == 0 ? "_" : "");
      this.gui.text(var2, var1 - 95.0F, 91.0F, -1, true);
      this.hitbox(var1 - 100.0F, 85.0F, 200.0F, 20.0F, () -> this.focusField = 1);
      this.gui.text("Blank = random seed  (Tab = next field)", var1 - 100.0F, 108.0F, -8355712, true);
      String[] var3 = new String[]{"Survival", "Creative", "Adventure", "Spectator"};
      String[] var4 = new String[]{
         "Gather, craft, eat. Hostile mobs.", "Unlimited blocks, flying (tap Space x2)", "Cannot break or place blocks.", "Fly through walls, invisible."
      };
      this.button("Game Mode: " + var3[this.newMode], var1 - 100.0F, 126.0F, 200.0F, 20.0F, true, () -> this.newMode = (this.newMode + 1) % 4);
      this.gui.text(var4[this.newMode], var1 - 100.0F, 149.0F, -8355712, true);
      this.button("Create New World", var1 - 100.0F, this.gui.h - 56.0F, 200.0F, 20.0F, true, () -> this.startWorld(true, this.seedField, this.newMode));
      this.button("Cancel", var1 - 100.0F, this.gui.h - 32.0F, 200.0F, 20.0F, true, () -> {
         this.scanWorlds();
         this.setScreen(this.worldList.isEmpty() ? Game.S.TITLE : Game.S.WORLDS);
      });
   }

   void drawLoading() {
      this.gui.dirtBackground(0.25F);
      float var1 = this.gui.w / 2.0F;
      float var2 = this.gui.h / 2.0F;
      this.gui.centered("Loading level", var1, var2 - 20.0F, -1, true, 1.5F);
      this.gui.centered("Building terrain... " + this.loadingMsg, var1, var2, -5592406, true, 1.0F);
      int var3 = (int)(GLFW.glfwGetTime() * 3.0) % 4;
      float var4 = 0.0F;
      if (this.world != null) {
         int var5 = (int)Math.floor(this.player.x) >> 4;
         int var6 = (int)Math.floor(this.player.z) >> 4;
         var4 = this.world.loadedAround(var5, var6, 2) / 25.0F;
      }

      this.gui.rect(var1 - 50.0F, var2 + 16.0F, 100.0F, 4.0F, -16777216);
      this.gui.rect(var1 - 50.0F, var2 + 16.0F, 100.0F * var4, 4.0F, -8323296);
   }

   void drawPause() {
      this.gui.gradient(0.0F, 0.0F, this.gui.w, this.gui.h, 1611665424, -1877995504);
      float var1 = this.gui.w / 2.0F;
      float var2 = this.gui.h / 4.0F;
      this.gui.centered("Game menu", var1, var2 - 16.0F, -1, true, 1.0F);
      this.button("Back to Game", var1 - 100.0F, var2, 200.0F, 20.0F, true, () -> this.setScreen(Game.S.PLAY));
      this.button("Options...", var1 - 100.0F, var2 + 28.0F, 200.0F, 20.0F, true, () -> {
         this.optionsReturn = Game.S.PAUSE;
         this.setScreen(Game.S.OPTIONS);
      });
      this.button("Save and Quit to Title", var1 - 100.0F, var2 + 56.0F, 200.0F, 20.0F, true, this::quitToTitle);
   }

   void drawDead() {
      this.gui.gradient(0.0F, 0.0F, this.gui.w, this.gui.h, 1615855616, -1600126976);
      float var1 = this.gui.w / 2.0F;
      this.gui.centered("You died!", var1, 40.0F, -1, true, 3.0F);
      this.button("Respawn", var1 - 100.0F, this.gui.h / 2.0F - 10.0F, 200.0F, 20.0F, true, () -> {
         this.respawn();
         this.setScreen(Game.S.PLAY);
      });
      this.button("Title Screen", var1 - 100.0F, this.gui.h / 2.0F + 14.0F, 200.0F, 20.0F, true, this::quitToTitle);
   }

   String onOff(boolean var1) {
      return var1 ? "ON" : "OFF";
   }

   void drawOptions() {
      float var1 = this.gui.w / 2.0F;
      float var2 = Math.max(24.0F, this.gui.h / 2.0F - 90.0F);
      this.gui.centered("Options", var1, 10.0F, -1, true, 1.0F);
      float var3 = var1 - 155.0F;
      float var4 = var1 + 5.0F;
      float var5 = 150.0F;
      this.slider(
         "Render Dist: " + this.opt.renderDistance,
         var3,
         var2,
         var5,
         20.0F,
         (this.opt.renderDistance - 2) / 30.0F,
         var1x -> this.opt.renderDistance = 2 + Math.round(var1x * 30.0F)
      );
      this.slider("FOV: " + this.opt.fov, var4, var2, var5, 20.0F, (this.opt.fov - 30) / 80.0F, var1x -> this.opt.fov = 30 + Math.round(var1x * 80.0F));
      this.slider(
         "Sensitivity: " + (int)(this.opt.sensitivity * 200.0F) + "%",
         var3,
         var2 + 24.0F,
         var5,
         20.0F,
         this.opt.sensitivity,
         var1x -> this.opt.sensitivity = var1x
      );
      this.slider(
         "Brightness: " + (this.opt.gamma <= 0.0F ? "Moody" : (this.opt.gamma >= 1.0F ? "Bright" : (int)(this.opt.gamma * 100.0F) + "%")),
         var4,
         var2 + 24.0F,
         var5,
         20.0F,
         this.opt.gamma,
         var1x -> this.opt.gamma = var1x
      );
      this.button("Smooth Lighting: " + this.onOff(this.opt.smooth), var3, var2 + 48.0F, var5, 20.0F, true, () -> {
         this.opt.smooth = !this.opt.smooth;
         this.remeshAll();
      });
      this.button("Clouds: " + this.onOff(this.opt.clouds), var4, var2 + 48.0F, var5, 20.0F, true, () -> this.opt.clouds = !this.opt.clouds);
      this.button("VSync: " + this.onOff(this.opt.vsync), var3, var2 + 72.0F, var5, 20.0F, true, () -> {
         this.opt.vsync = !this.opt.vsync;
         GLFW.glfwSwapInterval(this.opt.vsync ? 1 : 0);
      });
      this.button(
         "GUI Scale: " + (this.opt.guiScale == 0 ? "Auto" : this.opt.guiScale + ""),
         var4,
         var2 + 72.0F,
         var5,
         20.0F,
         true,
         () -> this.opt.guiScale = (this.opt.guiScale + 1) % 5
      );
      this.button("Fullscreen: " + this.onOff(this.opt.fullscreen), var3, var2 + 96.0F, var5, 20.0F, true, () -> this.setFullscreen(!this.opt.fullscreen));
      int[] var6 = new int[]{0, 32, 64, 128, 256};
      this.button("LOD Distance: " + (this.opt.lodDistance == 0 ? "OFF" : this.opt.lodDistance + " chunks"), var4, var2 + 96.0F, var5, 20.0F, true, () -> {
         int var2x = 0;

         for (int var3x = 0; var3x < var6.length; var3x++) {
            if (var6[var3x] == this.opt.lodDistance) {
               var2x = var3x;
            }
         }

         this.opt.lodDistance = var6[(var2x + 1) % var6.length];
      });
      this.button("Shaders: " + this.onOff(this.opt.shaders), var3, var2 + 120.0F, var5, 20.0F, true, () -> this.opt.shaders = !this.opt.shaders);
      this.button("Weather: " + this.onOff(this.opt.weather), var4, var2 + 120.0F, var5, 20.0F, true, () -> this.opt.weather = !this.opt.weather);
      this.button("Done", var1 - 100.0F, var2 + 148.0F, 200.0F, 20.0F, true, () -> {
         this.opt.save();
         this.setScreen(this.optionsReturn);
      });
   }

   void remeshAll() {
      if (this.world != null) {
         for (Chunk var2 : this.world.chunks.values()) {
            for (Section var6 : var2.sections) {
               var6.dirty = true;
            }
         }
      }
   }

   void drawHud() {
      if (!this.hideGui) {
         this.drawOverlays();
         if (this.screen == Game.S.PLAY || this.screen == Game.S.CHAT) {
            GL11.glBlendFunc(775, 769);
            float var1 = (float)Math.floor(this.gui.w / 2.0F);
            float var2 = (float)Math.floor(this.gui.h / 2.0F);
            this.gui.rect(var1 - 7.0F, var2 - 0.5F, 15.0F, 1.0F, -1);
            this.gui.rect(var1 - 0.5F, var2 - 7.0F, 1.0F, 6.5F, -1);
            this.gui.rect(var1 - 0.5F, var2 + 0.5F, 1.0F, 6.5F, -1);
            GL11.glBlendFunc(770, 771);
         }

         this.drawHotbar();
      }

      if (this.debug) {
         this.drawDebug();
      }

      this.drawChat();
   }

   void drawOverlays() {
      if (this.player.hurtTime > 0) {
         this.gui.rect(0.0F, 0.0F, this.gui.w, this.gui.h, (int)(this.player.hurtTime / 10.0F * 90.0F) << 24 | 0xFF0000);
      }

      if (this.player.eyeInWater) {
         this.gui.rect(0.0F, 0.0F, this.gui.w, this.gui.h, 1612726464);
      }

      if (this.player.burn > 0 && !this.player.creative) {
         // flames licking up the bottom of the screen
         float band = this.gui.h * 0.28F;
         int flick = this.tickCount / 2 % 3;
         this.gui.rect(0.0F, this.gui.h - band * 0.55F, this.gui.w, band * 0.55F, (96 + flick * 8) << 24 | 0xFF7A10);
         this.gui.rect(0.0F, this.gui.h - band, this.gui.w, band * 0.45F, (54 + flick * 6) << 24 | 0xFFA020);
         this.gui.rect(0.0F, this.gui.h - band * 0.25F, this.gui.w, band * 0.25F, 150 << 24 | 0xFFD040);
      }

      int var1 = this.world.getBlock((int)Math.floor(this.player.x), (int)Math.floor(this.player.y + this.player.eye), (int)Math.floor(this.player.z));
      if (Blocks.isLava(var1)) {
         this.gui.rect(0.0F, 0.0F, this.gui.w, this.gui.h, -1328525312);
      }
   }

   void damagePlayer(int var1, double var2, double var4) {
      if (!this.player.creative && !this.player.dead && this.player.invuln <= 0) {
         int var6 = this.armorPoints();
         float var7 = Math.min(20.0F, Math.max(var6 / 5.0F, var6 - var1 / 2.0F)) / 25.0F;
         int var8 = Math.max(1, Math.round(var1 * (1.0F - var7)));
         if (var6 == 0) {
            var8 = var1;
         }

         this.player.hurt(var8);
         double var9 = this.player.x - var2;
         double var11 = this.player.z - var4;
         double var13 = Math.sqrt(var9 * var9 + var11 * var11);
         if (var13 > 1.0E-4) {
            this.player.vx += var9 / var13 * 0.4;
            this.player.vz += var11 / var13 * 0.4;
            if (this.player.onGround) {
               this.player.vy = Math.max(this.player.vy, 0.36);
            }
         }

         for (int var15 = 0; var15 < 4; var15++) {
            Stack var16 = this.armor[var15];
            if (var16 != null) {
               var16.dmg = var16.dmg + Math.max(1, var1 / 4);
               if (var16.dmg >= Items.maxDamage(var16.id)) {
                  this.armor[var15] = null;
               }
            }
         }
      }
   }

   int armorPoints() {
      int var1 = 0;

      for (Stack var5 : this.armor) {
         if (var5 != null) {
            var1 += Items.armorPoints(var5.id);
         }
      }

      return var1;
   }

   void drawHotbar() {
      if (!this.player.spectator) {
         float var1 = (float)Math.floor((this.gui.w - 182.0F) / 2.0F);
         float var2 = this.gui.h - 22.0F;
         this.gui.rect(var1, var2, 182.0F, 22.0F, -16777216);
         this.gui.rect(var1 + 1.0F, var2 + 1.0F, 180.0F, 20.0F, -11184811);
         this.gui.rect(var1 + 1.0F, var2 + 1.0F, 180.0F, 1.0F, -8750470);

         for (int var3 = 0; var3 < 9; var3++) {
            float var4 = var1 + 3.0F + var3 * 20;
            float var5 = var2 + 3.0F;
            this.gui.rect(var4, var5, 16.0F, 16.0F, -13750738);
            this.gui.rect(var4 + 1.0F, var5 + 1.0F, 14.0F, 14.0F, -7631989);
            this.gui.rect(var4, var5, 16.0F, 1.0F, -13158601);
            this.gui.rect(var4, var5, 1.0F, 16.0F, -13158601);
            this.gui.rect(var4 + 15.0F, var5 + 1.0F, 1.0F, 15.0F, -1);
            this.gui.rect(var4 + 1.0F, var5 + 15.0F, 15.0F, 1.0F, -1);
            this.gui.rect(var4 + 1.0F, var5 + 1.0F, 14.0F, 14.0F, -7631989);
         }

         for (int var10 = 0; var10 < 9; var10++) {
            this.drawStack(this.inv[var10], var1 + 3.0F + var10 * 20, var2 + 3.0F);
         }

         float var11 = var1 + 3.0F + this.sel * 20 - 4.0F;
         this.gui.rect(var11, var2 - 1.0F, 24.0F, 2.0F, -1);
         this.gui.rect(var11, var2 + 21.0F, 24.0F, 2.0F, -1);
         this.gui.rect(var11, var2 - 1.0F, 2.0F, 24.0F, -1);
         this.gui.rect(var11 + 22.0F, var2 - 1.0F, 2.0F, 24.0F, -1);
         if (!this.player.creative) {
            float var12 = var2 - 11.0F;

            for (int var14 = 0; var14 < 10; var14++) {
               int var6 = this.player.health - var14 * 2;
               int var7 = var6 >= 2 ? 2 : (var6 == 1 ? 1 : 0);
               float var8 = this.player.health <= 4 && (this.tickCount / 2 + var14) % 5 == 0 ? 1.0F : 0.0F;
               this.gui.heart(var1 + var14 * 8, var12 + var8, 0);
               if (var7 > 0) {
                  this.gui.heart(var1 + var14 * 8, var12 + var8, var7);
               }
            }

            int var15 = this.armorPoints();
            if (var15 > 0) {
               for (int var17 = 0; var17 < 10; var17++) {
                  this.gui.armorIcon(var1 + var17 * 8, var12 - 10.0F, var15 - var17 * 2 >= 2 ? 2 : (var15 - var17 * 2 == 1 ? 1 : 0));
               }
            }

            for (int var18 = 0; var18 < 10; var18++) {
               int var20 = this.player.food - var18 * 2;
               int var22 = var20 >= 2 ? 2 : (var20 == 1 ? 1 : 0);
               float var9 = this.player.sat <= 0.0F && this.player.food <= 6 && (this.tickCount / 3 + var18) % 6 == 0 ? 1.0F : 0.0F;
               this.gui.hunger(var1 + 182.0F - 9.0F - var18 * 8, var12 + var9, 0);
               if (var22 > 0) {
                  this.gui.hunger(var1 + 182.0F - 9.0F - var18 * 8, var12 + var9, var22);
               }
            }

            if (this.player.air < 300) {
               int var19 = (int)Math.ceil(this.player.air / 30.0);

               for (int var21 = 0; var21 < 10; var21++) {
                  if (var21 < var19) {
                     this.gui.bubble(var1 + 182.0F - 9.0F - var21 * 8, var12 - 10.0F);
                  }
               }
            }
         }

         if (this.nameTimer > 0.0F && this.inv[this.sel] != null && this.screen == Game.S.PLAY) {
            String var13 = Items.name(this.inv[this.sel].id);
            int var16 = (int)Math.min(255.0F, this.nameTimer * 12.0F);
            this.gui.centered(var13, this.gui.w / 2.0F, var2 - (this.player.creative ? 16 : 38), var16 << 24 | 16777215, true, 1.0F);
         }
      }
   }

   void drawDebug() {
      int var1 = (int)Math.floor(this.player.x);
      int var2 = (int)Math.floor(this.player.y);
      int var3 = (int)Math.floor(this.player.z);
      Chunk var4 = this.world.chunkAt(var1, var3);
      String var5 = var4 == null ? "?" : Gen.BIOME_NAMES[var4.biome[(var3 & 15) * 16 + (var1 & 15)]];
      int var6 = ((int)Math.floor(this.player.yaw / 90.0 + 0.5) % 4 + 4) % 4;
      float var7 = ((this.player.yaw + 180.0F) % 360.0F + 360.0F) % 360.0F - 180.0F;
      long var8 = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576L;
      String[] var10 = new String[]{
         "Blockcraft 1.12 Update " + VERSION_STR,
         this.fps + " fps, " + this.rend.visibleSections + " sections, " + this.rend.drawCalls + " draws",
         "C: " + (this.world == null ? 0 : this.world.chunks.size()) + " chunks loaded, mesh queue: " + this.rend.inflight.get(),
         "Mem: "
            + var8
            + " MB   Items: "
            + this.world.items.size()
            + "   Mobs: "
            + this.mobs.count(false)
            + "p/"
            + this.mobs.count(true)
            + "h   Mode: "
            + this.player.gameMode
            + "  Food: "
            + this.player.food
            + "/"
            + String.format("%.1f", new Object[]{this.player.sat}),
         "",
         String.format("XYZ: %.3f / %.5f / %.3f", new Object[]{this.player.x, this.player.y, this.player.z}),
         String.format("Block: %d %d %d   Chunk: %d %d in %d %d", new Object[]{var1, var2, var3, var1 & 15, var3 & 15, var1 >> 4, var3 >> 4}),
         String.format("Facing: %s (%.1f / %.1f)", new Object[]{DIRS[var6], var7, this.player.pitch}),
         "Light: " + this.world.getSky(var1, var2, var3) + " sky, " + this.world.getBlk(var1, var2, var3) + " block",
         "Biome: " + var5,
         "Weather: " + this.world.weather.describe(this.player.x, this.player.z)
            + String.format("   cover %.2f rain %.2f storm %.2f   wet %.2f", new Object[]{this.rend.wfx.cover, this.rend.wfx.rain, this.rend.wfx.storm, this.world.weather.wet}),
         "Seed: " + this.world.seed + "   Time: " + this.world.dayTime % 24000L + "  Day " + this.world.dayTime / 24000L,
         this.target == null
            ? ""
            : "Looking at: "
               + this.target.x
               + " "
               + this.target.y
               + " "
               + this.target.z
               + " ["
               + Blocks.name[this.world.getBlock(this.target.x, this.target.y, this.target.z)]
               + "]"
      };
      float var11 = 2.0F;

      for (String var15 : var10) {
         if (var15.isEmpty()) {
            var11 += 5.0F;
         } else {
            this.gui.rect(1.0F, var11 - 1.0F, Gui.textWidth(var15) + 2, 10.0F, -1873784752);
            this.gui.text(var15, 2.0F, var11, -2039584, false);
            var11 += 10.0F;
         }
      }
   }

   void drawChat() {
      long var1 = System.currentTimeMillis();
      float var3 = this.gui.h - 40.0F;
      int var4 = 0;

      for (int var5 = this.chatLog.size() - 1; var5 >= 0 && var4 < 10; var5--) {
         long var6 = var1 - (Long)this.chatTimes.get(var5);
         if (var6 > 10000L && this.screen != Game.S.CHAT) {
            break;
         }

         float var8 = this.screen == Game.S.CHAT ? 1.0F : Math.max(0.0F, Math.min(1.0F, (float)(10000L - var6) / 2000.0F));
         String var9 = (String)this.chatLog.get(var5);
         this.gui.rect(2.0F, var3 - 1.0F, Gui.textWidth(Gui.stripCodes(var9)) + 3, 10.0F, (int)(var8 * 100.0F) << 24);
         this.gui.text(var9, 3.0F, var3, (int)(var8 * 255.0F) << 24 | 16777215, true);
         var3 -= 10.0F;
         var4++;
      }

      if (this.screen == Game.S.CHAT) {
         this.gui.rect(2.0F, this.gui.h - 14.0F, this.gui.w - 4.0F, 12.0F, Integer.MIN_VALUE);
         String var10 = this.chat + ((int)(GLFW.glfwGetTime() * 2.0) % 2 == 0 ? "_" : "");
         this.gui.text(var10, 4.0F, this.gui.h - 12.0F, -2039584, false);
      }
   }

   void drawInventory() {
      this.gui.rect(0.0F, 0.0F, this.gui.w, this.gui.h, -1877995504);
      if (this.menu != null) {
         float var1 = (float)(this.mouseX / this.gui.scale);
         float var2 = (float)(this.mouseY / this.gui.scale);
         float var3 = this.menuOx();
         float var4 = this.menuOy();
         this.menu.draw(this.gui, var1, var2, var3, var4);
         if (this.menu.kind == 4) {
            int var5 = this.menu.pages();
            String var6 = this.menu.page + 1 + "/" + var5;
            this.gui.text(var6, var3 + this.menu.w - 8.0F - Gui.textWidth(var6), var4 + 6.0F, -12566464, false);
            Menu var7 = this.menu;
            this.button("<", var3 + this.menu.w - 60.0F, var4 + 3.0F, 12.0F, 12.0F, this.menu.page > 0, () -> var7.page--);
            this.button(">", var3 + this.menu.w - 46.0F, var4 + 3.0F, 12.0F, 12.0F, this.menu.page < var5 - 1, () -> var7.page++);
         }
      }
   }

   void screenshot() {
      try {
         ByteBuffer var1 = MemoryUtil.memAlloc(this.fbW * this.fbH * 4);
         GL11.glReadPixels(0, 0, this.fbW, this.fbH, 6408, 5121, var1);
         byte[] var2 = new byte[(this.fbW * 3 + 1) * this.fbH];

         for (int var3 = 0; var3 < this.fbH; var3++) {
            int var4 = (this.fbH - 1 - var3) * this.fbW * 4;
            var2[var3 * (this.fbW * 3 + 1)] = 0;

            for (int var5 = 0; var5 < this.fbW; var5++) {
               int var6 = var3 * (this.fbW * 3 + 1) + 1 + var5 * 3;
               var2[var6] = var1.get(var4 + var5 * 4);
               var2[var6 + 1] = var1.get(var4 + var5 * 4 + 1);
               var2[var6 + 2] = var1.get(var4 + var5 * 4 + 2);
            }
         }

         MemoryUtil.memFree(var1);
         File var8 = new File(this.root, "screenshots");
         var8.mkdirs();
         File var9 = new File(var8, new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").format(new Date()) + ".png");
         writePng(var9, this.fbW, this.fbH, var2);
         this.say("Saved screenshot as " + var9.getName());
      } catch (Exception var7) {
         this.say("§cFailed to save screenshot");
      }
   }

   static void writePng(File var0, int var1, int var2, byte[] var3) throws IOException {
      DataOutputStream var4 = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(var0)));

      try {
         var4.write(new byte[]{-119, 80, 78, 71, 13, 10, 26, 10});
         ByteArrayOutputStream var5 = new ByteArrayOutputStream();
         DataOutputStream var6 = new DataOutputStream(var5);
         var6.writeInt(var1);
         var6.writeInt(var2);
         var6.writeByte(8);
         var6.writeByte(2);
         var6.writeByte(0);
         var6.writeByte(0);
         var6.writeByte(0);
         chunk(var4, "IHDR", var5.toByteArray());
         Deflater var7 = new Deflater();
         var7.setInput(var3);
         var7.finish();
         ByteArrayOutputStream var8 = new ByteArrayOutputStream();
         byte[] var9 = new byte[65536];

         while (!var7.finished()) {
            var8.write(var9, 0, var7.deflate(var9));
         }

         chunk(var4, "IDAT", var8.toByteArray());
         chunk(var4, "IEND", new byte[0]);
      } catch (Throwable var11) {
         try {
            var4.close();
         } catch (Throwable var10) {
            var11.addSuppressed(var10);
         }

         throw var11;
      }

      var4.close();
   }

   static void chunk(DataOutputStream var0, String var1, byte[] var2) throws IOException {
      var0.writeInt(var2.length);
      byte[] var3 = var1.getBytes("US-ASCII");
      var0.write(var3);
      var0.write(var2);
      CRC32 var4 = new CRC32();
      var4.update(var3);
      var4.update(var2);
      var0.writeInt((int)var4.getValue());
   }

   static final class Btn {
      float x;
      float y;
      float w;
      float h;
      Runnable action;
      Consumer<Float> slider;
   }

   public static final class Particle {
      public float x;
      public float y;
      public float z;
      public float vx;
      public float vy;
      public float vz;
      public float size;
      public float u;
      public float v;
      public float r = 1.0F;
      public float g = 1.0F;
      public float b = 1.0F;
      public float shade = 1.0F;
      /** downward acceleration per tick (smoke and sparks use a tiny or negative value so they rise) */
      public float grav = 0.04F;
      int life;
   }

   enum S {
      TITLE,
      WORLDS,
      NEWWORLD,
      LOADING,
      PLAY,
      PAUSE,
      INV,
      CHAT,
      OPTIONS,
      DEAD;
   }

   static final class WInfo {
      String dir;
      String name;
      long mod;
      int mode;
   }
}
