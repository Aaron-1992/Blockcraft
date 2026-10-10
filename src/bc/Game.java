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
import org.lwjgl.glfw.GLFWDropCallback;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.system.MemoryUtil;

public final class Game {
   public static final String VERSION_STR = "4.1";
   public static final String TITLE_STR = "Blockcraft V" + VERSION_STR;
   public Options opt;
   public World world;
   public Player player;
   public Renderer rend;
   Gui gui = new Gui();
   /** Update 3.13: the widgets and state of the menu screens (see Screens). */
   final Ui ui = new Ui(this);
   /** Update 3.13: the turning world behind the title and the menus (null while a real world is open). */
   Panorama pano;
   long window;
   int winW = 1280;
   int winH = 720;
   int fbW = 1280;
   int fbH = 720;
   File root;
   Game.S screen = Game.S.TITLE;
   Game.S optionsReturn = Game.S.TITLE;
   Game.S mpReturn = Game.S.TITLE;
   final Stack[] inv = new Stack[36];
   final Stack[] armor = new Stack[4];
   Stack cursor;
   final Mobs mobs = new Mobs(this);
   Mob spectatingMob;
   boolean spectatorNightVision;
   /** Update 3.12: the advancements of the world that is open. */
   final Adv312 adv = new Adv312();
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
   /** Update 3.13: the new world was made with "Bonus Chest": the chest is put next to the player once the spawn is known. */
   boolean bonusPending;
   // ---- Update 3.12: dimensions ----
   /** The world's folder: the Overworld's chunks live in it, the Nether's in DIM-1 and the End's in DIM1. */
   File rootDir;
   /** The fire / weather / disaster settings of the Overworld, kept while the player is in another dimension (a new World object has its own). */
   Properties overworldSettings = new Properties();
   /** Ticks the player has been standing in a portal, and: the player came out of a portal and has not left it yet. */
   int portalTime;
   boolean portalLock;
   /** While another dimension loads: where the player comes out ({x, z}); and how (0 through a portal, 1 at the spawn point, 2 in the End). */
   double[] arrivalAt;
   int arrivalKind;
   int[] spawnXZ = new int[]{0, 0};
   int[] bedPos = null;
   double[] spawnPos = new double[]{0.0, 80.0, 0.0};
   final StringBuilder chat = new StringBuilder();
   long chatOpenedAt;
   static String clipboardFallback = "";
   final ArrayList<String> chatHistory = new ArrayList();
   int chatHistoryPos = -1;
   String chatDraft = "";
   final ArrayList<String> addrHistory = new ArrayList(Arrays.asList("127.0.0.1:25565", "lan"));
   int addrHistoryPos = -1;
   final ArrayList<String> seedHistory = new ArrayList();
   int seedHistoryPos = -1;
   final ArrayList<String> chatLog = new ArrayList();
   final ArrayList<Long> chatTimes = new ArrayList();
   final ArrayList<Game.Particle> particles = new ArrayList();
   /**
    * Update 3.9: the equip animation. The item in the hand lowers and the new one rises when you switch slots or the item in the slot changes;
    * the hand keeps drawing {@link #shownId} (which lags behind the selected item) until it is out of sight. Rates like Minecraft's: 0.4 per tick.
    */
   /** Update 3.10: camera shake 0..1.5 (earthquakes, a tree hitting the ground, a meteor); {@link #addShake} raises it, it fades by itself. */
   float shake;
   float pshake;
   float equip = 1.0F;
   float pEquip = 1.0F;
   /** Update 3.11: the arm lags behind the camera when you turn (Minecraft's renderArmPitch / renderArmYaw: it follows at half the distance every tick). */
   /** -Dbc.handswing=0.35 / -Dbc.handeat=0.5 freeze the hand in the middle of a swing / of eating (screenshot scenes). */
   static final String HAND_SWING = System.getProperty("bc.handswing");
   static final String HAND_EAT = System.getProperty("bc.handeat");
   float armPitch;
   float armYaw;
   float pArmPitch;
   float pArmYaw;
   int shownSel = -1;
   int shownId = -1;
   /** Update 3.9: skins, the camera view (0 first person, 1 behind the player, 2 in front of the player) and the body's animation state. */
   Skins skins;
   int view;
   boolean poseInit;
   float pBodyYaw;
   float bodyYaw;
   float walkPhase;
   float pWalkPhase;
   float walkAmp;
   float pWalkAmp;
   /**
    * Update 3.18: how much of the time the body of the player is sneaking, sprinting, in the air, swimming, treading water, climbing, flying (0..1,
    * smoothed from tick to tick), how hard it has just landed and how long ago it was hit; and the previous tick's values (the frames between are interpolated).
    */
   final float[] animK = new float[9];
   final float[] pAnimK = new float[9];
   static final int K_SNEAK = 0;
   static final int K_SPRINT = 1;
   static final int K_AIR = 2;
   static final int K_SWIM = 3;
   static final int K_TREAD = 4;
   static final int K_CLIMB = 5;
   static final int K_FLY = 6;
   static final int K_LAND = 7;
   static final int K_HURT = 8;
   int airTicks;
   boolean lastGround = true;
   double lastVy;
   /** -Dbc.posestate=swim=1,eat=0.5,... holds the body of the player in exactly this state (screenshot scenes): sneak, sprint, air, swim, tread, climb, fly, land, hurt, vy, eat, bow, swing, phase, amp. */
   static final String POSE_STATE = System.getProperty("bc.posestate");
   /** Update 3.11: Minecraft's view bobbing numbers (distanceWalkedModified, cameraYaw): the first-person hand bobs with every step. */
   float bobDist;
   float pBobDist;
   float bobAmp;
   float pBobAmp;
   int skinSel = -1;
   int skinScroll;
   String skinField = "";
   boolean skinFocus;
   String skinMsg = "";
   Game.S skinReturn = Game.S.TITLE;
   Game.S packsReturn = Game.S.TITLE;
   Game.S modsReturn = Game.S.TITLE;
   volatile boolean skinBrowsing;
   /** the game was fullscreen when the file dialog opened: go back to it once the dialog is gone */
   volatile boolean skinRefull;
   final java.util.concurrent.ConcurrentLinkedQueue<File> skinDrops = new java.util.concurrent.ConcurrentLinkedQueue<>();
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
   /** The tab of the creative inventory that was open last (it opens there again). */
   int creativeTab;
   /** How far the loading screen is (0..1): the chunks around the player, then the meshes. */
   float loadFrac;
   ArrayList<Game.Btn> btns = new ArrayList();
   ArrayList<Game.Btn> activeBtns = new ArrayList();
   int dragging = -1;
   int autoFrames;
   int autoShots;
   long autoNetStart;
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

      // the skin screen's "Import skin file..." opens a native file dialog (AWT) on Windows
      System.setProperty("java.awt.headless", System.getProperty("os.name", "").startsWith("Windows") ? "false" : "true");
      this.opt = new Options(this.root);
      this.opt.load();
      this.skins = new Skins(this.root);
      if (System.getProperty("bc.rd") != null) {
         this.opt.renderDistance = Integer.getInteger("bc.rd");
      }

      if (System.getProperty("bc.leaves") != null) {
         this.opt.leaves = Math.max(0, Math.min(2, Integer.getInteger("bc.leaves")));
      }

      if (System.getProperty("bc.bushy") != null) {
         this.opt.bushyLeaves = Boolean.getBoolean("bc.bushy");
      }

      Mesher.leafMode = this.opt.leaves;
      Mesher.bushyLeaves = this.opt.bushyLeaves;
      if (System.getProperty("bc.guiscale") != null) {
         this.opt.guiScale = Integer.getInteger("bc.guiscale"); // test hook: force the GUI scale (screens must cope with narrow windows)
      }

      if (System.getProperty("bc.win") != null) {
         String[] wh = System.getProperty("bc.win").split("x"); // test hook: window size, e.g. 1030x720
         this.winW = this.fbW = Integer.parseInt(wh[0]);
         this.winH = this.fbH = Integer.parseInt(wh[1]);
      }

      if (System.getProperty("bc.waterq") != null) {
         this.opt.waterQ = Integer.getInteger("bc.waterq"); // 1 = no reflections of the terrain in the water (cheaper)
      }

      if (System.getProperty("bc.lod") != null) {
         this.opt.lodDistance = Integer.getInteger("bc.lod");
      }

      if (System.getProperty("bc.gamma") != null) {
         this.opt.gamma = Float.parseFloat(System.getProperty("bc.gamma")); // test hook: 1 = "Bright" (caves can be photographed)
      }

      if (System.getProperty("bc.scale") != null) {
         this.opt.renderScale = Math.max(50, Math.min(100, Integer.getInteger("bc.scale"))); // test hook: the render scale in percent
      }

      if (System.getProperty("bc.shaders") != null) {
         this.opt.shaders = Boolean.getBoolean("bc.shaders");
      }

      if (System.getProperty("bc.look") != null) {
         this.opt.look = Look.clamp(Integer.getInteger("bc.look")); // test hook: the colour grading of the shader pack (0 Classic, 1 Vivid, 2 Cinematic, 3 Dreamy)
      }

      if (System.getProperty("bc.fx") != null) {
         this.opt.fx = Math.max(0, Math.min(2, Integer.getInteger("bc.fx"))); // test hook: the effects level (0 LOW, 1 MEDIUM, 2 HIGH)
      }

      if ("off".equals(System.getProperty("bc.clouds"))) {
         this.opt.clouds = false; // screenshots of the terrain from above the cloud layer
      }

      this.init();

      try {
         this.loop();
      } finally {
         this.shutdown();
      }
   }

   /** Update 3.18: what the driver calls the chip that draws (GL_RENDERER), e.g. "NVIDIA GeForce RTX 3060 Laptop GPU/PCIe/SSE2". */
   String gpuName = "";

   void init() {
      // Update 3.18: ask Windows for the real graphics card before the first window exists (see GpuPref)
      boolean gpuChanged = GpuPref.apply(this.opt);
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
      this.gpuName = String.valueOf(GL11.glGetString(7937));
      Perf.init();
      System.out.println("GL: " + GL11.glGetString(7938) + " / " + this.gpuName);
      // (-Dbc.gpuforce=1, a test hook: behave as if the first process had got the small chip - the Windows smoke test proves with it that the second start works)
      boolean gpuForce = Boolean.getBoolean("bc.gpuforce");
      boolean wantDedicated = gpuChanged || gpuForce || this.opt.gpuHigh && GpuPref.windows() && !"off".equals(System.getProperty("bc.gpu"));
      if (wantDedicated && GpuPref.retryOf() == null && (gpuForce || GpuPref.isIntegrated(this.gpuName))) {
         // the first process got the integrated processor GPU (e.g. started as javaw.exe before Optimus/PowerXpress saw blockcraft-gpu.exe): relaunch once
         System.out.println("GPU: " + this.gpuName + " is part of the processor; starting again with the high performance preference");
         if (GpuPref.relaunch(this.gpuName)) {
            GLFW.glfwDestroyWindow(this.window);
            GLFW.glfwTerminate();
            System.exit(0);
         }
      }

      if (GpuPref.retryOf() != null) {
         System.out.println("GPU: second start; the first one drew with " + GpuPref.retryOf());
      }

      GLFW.glfwShowWindow(this.window);
      GLFW.glfwSetDropCallback(this.window, (win, count, names) -> {
         for (int i = 0; i < count; i++) {
            this.skinDrops.add(new File(GLFWDropCallback.getName(names, i)));
         }
      });
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
         if (!var3x && this.screen == Game.S.PLAY && !Net319.isActive()) {
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

      if (System.getProperty("bc.auto") != null && (System.getProperty("bc.title") == null || System.getProperty("bc.world") != null)) {
         this.startWorld(true, System.getProperty("bc.seed", "12345"), Integer.parseInt(System.getProperty("bc.mode", "1")));
      } else if (System.getProperty("bc.auto") == null || "on".equals(System.getProperty("bc.pano"))) {
         Panorama.start(this); // Update 3.13: the title screen turns over a real landscape
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
      this.autoBiome();
   }

   /**
    * bc.disaster=tornado:dx,dz[,size[,speed]] | quake:dx,dz[,magnitude[,life]] | meteor:dx,dz[,flight] - starts a disaster relative to the player
    * (the camera) for screenshots: the tornado is fully grown and, with speed 0 (the default), stands still; -Dbc.dfreeze=N stops everything N ticks later.
    */
   void autoDisaster(String specs) {
      for (String spec : specs.split(";")) {
         this.autoDisaster1(spec);
      }
   }

   void autoDisaster1(String spec) {
      Disasters d = this.world.disasters;
      String[] kv = spec.split(":", 2);
      String[] a = kv.length > 1 ? kv[1].split(",") : new String[0];
      double dx = a.length > 0 ? Double.parseDouble(a[0]) : 0.0;
      double dz = a.length > 1 ? Double.parseDouble(a[1]) : 40.0;
      double x = this.player.x + dx;
      double z = this.player.z + dz;
      d.frozenAt = d.clock;
      switch (kv[0]) {
         case "tornado":
            Disasters.Tornado t = d.spawnTornado(x, z, 1.0, 0.0, a.length > 2 ? Float.parseFloat(a[2]) : 1.1F, true);
            t.speed = a.length > 3 ? Double.parseDouble(a[3]) : 0.0;
            t.life = 99999;
            break;
         case "quake":
            d.startQuake(x, z, a.length > 2 ? Float.parseFloat(a[2]) : 2.5F, a.length > 3 ? Integer.parseInt(a[3]) : 400);
            break;
         case "meteor":
            d.spawnMeteor(x, z, 0.6, 1.0, a.length > 2 ? Integer.parseInt(a[2]) : 60, 1.6F);
            break;
         default:
            System.out.println("[disasters] bc.disaster: unknown kind " + kv[0]);
      }
   }

   /** bc.biome=<id>: every loaded chunk gets this biome (re-applied while chunks are still loading in). */
   void autoBiome() {
      String biome = System.getProperty("bc.biome");
      if (biome != null) {
         for (Chunk c : this.world.chunks.values()) {
            Arrays.fill(c.biome, (byte)Integer.parseInt(biome));
         }
      }
   }

   /** bc.skin=demo | <file name>: a skin for screenshots ("demo" is a built-in slim-armed test skin that is stored in the skins folder). */
   void autoSkin(String name) {
      if (name.equals("demo")) {
         try {
            java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(64, 64, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            img.setRGB(0, 0, 64, 64, Skins.demoSkin(), 0, 64);
            javax.imageio.ImageIO.write(img, "png", new File(this.skins.dir, "demo.png"));
            this.skins.setSlim("demo.png", true);
            name = "demo.png";
         } catch (Exception e) {
            System.err.println("bc.skin=demo: " + e);
         }
      }

      this.opt.skin = name;
   }

   long autoT0;

   void autoTest() {
      String var1 = System.getProperty("bc.title");
      if (this.autoT0 == 0L) {
         this.autoT0 = System.nanoTime();
      }

      if (var1 != null) {
         if (this.autoFrames == 0 && System.getProperty("bc.skin") != null) {
            this.autoSkin(System.getProperty("bc.skin"));
         }

         if (++this.autoFrames == 40) {
            this.setScreen(Game.S.valueOf(var1));
            this.ui.optTab = Math.max(0, Math.min(3, Integer.getInteger("bc.opttab", 0)));
         }

         String netUser = System.getProperty("bc.skinuser");
         if (netUser != null && this.skins != null) {
            // import a real account's skin over the network, then take the picture (at most 90 s later)
            if (this.autoFrames == 42) {
               this.autoNetStart = System.nanoTime();
               this.skins.importUser(netUser);
            }

            if (this.autoFrames > 42 && !this.skins.busy && this.autoFrames >= 60 && !this.autoShotNow) {
               System.out.println("[skins] import of " + netUser + ": " + this.skins.status);
               this.autoShotNow = true;
            } else if (this.autoFrames > 42 && this.skins.busy && System.nanoTime() - this.autoNetStart > 90_000_000_000L && !this.autoShotNow) {
               System.out.println("[skins] import of " + netUser + " still busy after 90 s: " + this.skins.status);
               this.autoShotNow = true;
            }
         } else if (this.autoFrames >= 60 && !this.autoShotNow && (this.pano == null || this.pano.ready() || System.nanoTime() - this.autoT0 > 150_000_000_000L)) {
            this.autoShotNow = true; // (with the panorama behind the menu: wait until it has faded in, 150 s at most)
         }

         if (this.autoFrames == 50 && System.getProperty("bc.skinbrowse") != null) {
            this.browseSkin();
         }
      } else if (this.screen == Game.S.PAUSE && System.getProperty("bc.pause") != null) {
         // the pause menu is up: give it a few frames to draw, then take the picture
         if (++this.autoFrames >= 108) {
            this.autoShotNow = true;
         }
      } else if (this.screen == Game.S.PLAY || this.screen == Game.S.INV || this.screen == Game.S.ADV) {
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

            if (System.getProperty("bc.dim") != null && var2 != null) {
               // Update 3.12 test hook: start in another dimension (bc.dim=-1 Nether, 1 End); bc.cam is then a position in THAT dimension
               String[] dc = var2.split(",");
               this.switchDimension(Integer.getInteger("bc.dim"), Double.parseDouble(dc[0]), Double.parseDouble(dc[1]), Double.parseDouble(dc[2]), 2);
            }

            if ("off".equals(System.getProperty("bc.fire"))) {
               this.world.fire.spread = false; // screenshots: fires neither burn out nor spread
            }

            if (System.getProperty("bc.weather") != null || System.getProperty("bc.wtime") != null || System.getProperty("bc.biome") != null) {
               this.autoWeather();
            }

            if (System.getProperty("bc.view") != null) {
               this.view = Integer.parseInt(System.getProperty("bc.view"));
            }

            if (System.getProperty("bc.skin") != null) {
               this.autoSkin(System.getProperty("bc.skin"));
            }
         }

         if (this.autoFrames % 15 == 0 && System.getProperty("bc.biome") != null) {
            this.autoBiome();
         }

         if (this.autoFrames >= Integer.getInteger("bc.shot", 100) - 4 && System.getProperty("bc.bolt") != null) {
            // a fresh lightning bolt every frame so the screenshot catches the blinding first stroke
            String[] b = System.getProperty("bc.bolt").split(",");
            this.world.weather.bolts.clear();
            this.world.weather.strike(this.player.x + Double.parseDouble(b[0]), this.player.z + Double.parseDouble(b[1]), false);
         }

         if (System.getProperty("bc.mouse") != null) {
            String[] mp = System.getProperty("bc.mouse").split(","); // the mouse position in framebuffer pixels (the player model in the inventory looks at it)
            this.mouseX = Double.parseDouble(mp[0]);
            this.mouseY = Double.parseDouble(mp[1]);
         }

         if (this.autoFrames == 12 && System.getProperty("bc.bodyyaw") != null) {
            // the body turned away from the camera by this many degrees (third-person screenshots from the side)
            this.poseInit = true;
            this.bodyYaw = this.pBodyYaw = this.player.yaw + Float.parseFloat(System.getProperty("bc.bodyyaw"));
         }

         if (this.autoFrames == 12 && System.getProperty("bc.emptyhand") != null) {
            this.inv[this.sel] = null; // screenshots of the arm in the first-person view
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

         if (this.autoFrames == 68 && System.getProperty("bc.fill") != null) {
            // bc.fill=x0,y0,z0,x1,y1,z1,id;...  fills boxes (before bc.set: room for a scene)
            for (String box : System.getProperty("bc.fill").split(";")) {
               String[] f = box.split(",");
               int[] n = new int[7];
               for (int i = 0; i < 7; i++) {
                  n[i] = Integer.parseInt(f[i]);
               }

               for (int x = Math.min(n[0], n[3]); x <= Math.max(n[0], n[3]); x++) {
                  for (int y = Math.min(n[1], n[4]); y <= Math.max(n[1], n[4]); y++) {
                     for (int z = Math.min(n[2], n[5]); z <= Math.max(n[2], n[5]); z++) {
                        this.world.setBlock(x, y, z, n[6]);
                     }
                  }
               }
            }
         }

         if (this.autoFrames == 75 && System.getProperty("bc.disaster") != null) {
            this.autoDisaster(System.getProperty("bc.disaster"));
         }

         if (this.autoFrames == 80 && System.getProperty("bc.fell") != null) {
            // bc.fell=x,y,z,dx,dz: cut the log at (x, y, z); the tree above it falls towards (dx, dz)
            String[] f = System.getProperty("bc.fell").split(",");
            int fx = Integer.parseInt(f[0]);
            int fy = Integer.parseInt(f[1]);
            int fz = Integer.parseInt(f[2]);
            this.world.setBlock(fx, fy, fz, 0);
            Trees.Plan plan = this.world.trees.collect(fx, fy, fz);
            if (plan == null || !this.world.trees.start(plan, Double.parseDouble(f[3]) * 10.0, Double.parseDouble(f[4]) * 10.0, null)) {
               System.out.println("[trees] bc.fell: no tree falls at " + fx + "," + fy + "," + fz);
            }
         }

         if (this.autoFrames >= 12 && System.getProperty("bc.walkphase") != null) {
            // bc.walkphase=phase,amplitude: every mob caught in the middle of a stride (screenshot scenes)
            String[] wp = System.getProperty("bc.walkphase").split(",");
            for (Mobs.Mob mm : this.mobs.list) {
               mm.walk = mm.pwalk = Float.parseFloat(wp[0]);
               mm.walkAmp = mm.pwalkAmp = Float.parseFloat(wp[1]);
            }
         }

         if (this.autoFrames >= 12 && System.getProperty("bc.posewalk") != null) {
            // bc.posewalk=phase,amplitude,sprint(0/1): the player's body caught in the middle of a stride (third-person scenes)
            String[] pw = System.getProperty("bc.posewalk").split(",");
            this.walkPhase = this.pWalkPhase = Float.parseFloat(pw[0]);
            this.walkAmp = this.pWalkAmp = Float.parseFloat(pw[1]);
            this.player.sprinting = pw.length > 2 && pw[2].equals("1");
         }

         if (System.getProperty("bc.tp") != null) {
            // bc.tp=frame:x,y,z - teleports the player at that frame (fly away from a scene so its chunks unload and the LOD has to show them)
            String[] tp = System.getProperty("bc.tp").split("[:,]");
            if (this.autoFrames == Integer.parseInt(tp[0])) {
               this.player.setPos(Double.parseDouble(tp[1]), Double.parseDouble(tp[2]), Double.parseDouble(tp[3]));
            }
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
            float summonYaw = Float.parseFloat(System.getProperty("bc.summonYaw", "150")); // how the mobs are turned, relative to the camera (180 = facing it)

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

               for (int var21 = 0; var21 < Mobs.N; var21++) {
                  if (Mobs.NAME[var21].equals(mobName)) {
                     double var8 = (var20 - (var14.length - 1) / 2.0) * 1.6;
                     double mx = this.player.x - Math.sin(var17) * summonDist + Math.cos(var17) * var8;
                     double my = this.player.y + summonDy;
                     double mz = this.player.z + Math.cos(var17) * summonDist + Math.sin(var17) * var8;
                     Mob spawned = babyMob ? this.mobs.spawnBaby(var21, mx, my, mz) : this.mobs.spawn(var21, mx, my, mz);
                     spawned.sheared = shearedMob;
                     if (System.getProperty("bc.still") != null) {
                        spawned.still = true; // Update 3.17: the screenshot mobs stand where they were put (see Anim317.testPose)
                        Anim317.testPose(spawned, System.getProperty("bc.mobpose", ""));
                     }

                     if (spawned.type == Mobs.VILLAGER) {
                        spawned.color = var20 % 6; // every villager a different profession
                     }

                     if (spawned.type == Mobs.PARROT) {
                        spawned.color = var20 % Mobs312.PARROT_COLORS; // every parrot another colour
                        if ("land".equals(System.getProperty("bc.parrot"))) {
                           spawned.phase = Mobs312.P_LAND; // sitting still for the picture
                           spawned.phaseT = 1000000;
                        } else if ("shoulder".equals(System.getProperty("bc.parrot")) && var20 < 2) {
                           spawned.tamed = true;
                           spawned.phase = Mobs312.P_SHOULDER;
                           spawned.aux = var20 == 0 ? 1.0F : -1.0F;
                           spawned.tx = this.player.health;
                        }
                     }
                     spawned.yaw = this.player.yaw + summonYaw;
                  }
               }
            }
         }

         if (this.autoFrames == 62 && System.getProperty("bc.dragon") != null) {
            // Update 3.12 test hook: the dragon sits on the fountain (bc.dragon=perch) for a screenshot
            for (Mob dm : this.mobs.list) {
               if (dm.type == Mobs.ENDER_DRAGON) {
                  dm.hx = 0.5;
                  dm.hz = 0.5;
                  dm.hy = 84.0;
                  if ("perch".equals(System.getProperty("bc.dragon"))) {
                     dm.phase = Dragon.PERCH;
                     dm.phaseT = -10000000;
                     dm.yaw = dm.pyaw = dm.headYaw = dm.pheadYaw = -90.0F;
                     Dragon.setPos(dm, 0.5, EndGen.TOP + 2.0, 0.5);
                     dm.px = dm.x;
                     dm.py = dm.y;
                     dm.pz = dm.z;
                  }
               }
            }
         }

         if (this.autoFrames == 100 && System.getProperty("bc.pause") != null) {
            this.setScreen(Game.S.PAUSE);
         }

         if (this.autoFrames == 100 && System.getProperty("bc.inv") != null) {
            String var15 = System.getProperty("bc.inv");
            if (var15.equals("craft")) {
               this.openMenu(1, null);
            } else if (var15.equals("player")) {
               this.openMenu(0, null);
            } else if (var15.equals("trade")) {
               this.menu = new Menu(this, Menu.TRADE, null, Integer.getInteger("bc.prof", 3));
               this.addItem(new Stack(Items.EMERALD, 24));
               this.addItem(new Stack(Items.IRON_INGOT, 16));
               this.setScreen(Game.S.INV);
            } else if (var15.equals("furnace")) {
               this.openMenu(2, new TileEntity(1, 0, 0, 0));
            } else if (var15.equals("chest")) {
               this.openMenu(3, new TileEntity(0, 0, 0, 0));
            } else {
               this.openMenu(this.player.creative ? 4 : 0, null);
            }

            if (this.menu != null) {
               if (this.menu.kind == 4) {
                  // screenshot hooks of Update 3.13: bc.tab = the creative tab, bc.search = the text typed into the search tab
                  this.menu.setTab(Integer.getInteger("bc.tab", this.menu.tab));
                  String typedText = System.getProperty("bc.search", "");
                  for (int i = 0; i < typedText.length(); i++) {
                     this.menu.typed(typedText.charAt(i));
                  }
               }

               this.menu.page = Math.min(this.menu.pages() - 1, Integer.getInteger("bc.page", 0));
            }
         }

         if (this.autoFrames == 100 && System.getProperty("bc.adv") != null) {
            // screenshot hook of Update 3.12: bc.adv=screen opens the advancements (bc.advdone = how many of them are made, bc.advtab = the tab), bc.adv=toast shows a toast
            int made = Integer.getInteger("bc.advdone", 12);
            for (int i = 0; i < made && i < Adv312.LIST.size(); i++) {
               this.adv.award(this, Adv312.LIST.get(i).id);
            }

            if (System.getProperty("bc.adv").equals("toast")) {
               this.adv.award(this, System.getProperty("bc.advid", "end/kill_dragon"));
            } else {
               this.adv.tab = Integer.getInteger("bc.advtab", 0);
               this.setScreen(Game.S.ADV);
            }
         }

         if (PerfBench.enabled()) {
            // Update 3.18: -Dbc.bench=frames - the benchmark runs on its own schedule and takes the last picture when it is done
            if (PerfBench.step(this)) {
               this.autoShotNow = true;
            }

            return;
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
                     var7.seed = (long)d(var8, "seed", 0.0);
                     try {
                        var7.seed = Long.parseLong(var8.getProperty("seed", "0").trim());
                     } catch (NumberFormatException seedEx) {
                        // keep the rounded value
                     }

                     var7.settings = WorldSettings.load(var8);
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

               if (var7.settings != null && var7.settings.isMinigame()) {
                  deleteDir(var5);
                  continue;
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

      if (System.getProperty("bc.fakeworlds") != null && this.worldList.isEmpty()) {
         // screenshot hook: a list of worlds that are not on the disk (the world list screen, with its maps)
         String[][] fake = {
            {"Survival Island", "0", "12345", "1"}, {"Creative Castle", "1", "-987654321", "1"}, {"Old World from Update 3.5", "0", "424242", "0"},
            {"Flat Test", "1", "7", "2"}, {"Mountain Home", "0", "99", "4"}
         };
         long now = System.currentTimeMillis();
         for (int i = 0; i < fake.length; i++) {
            Game.WInfo w = new Game.WInfo();
            w.dir = fake[i][0];
            w.name = fake[i][0];
            w.mode = Integer.parseInt(fake[i][1]);
            w.seed = Long.parseLong(fake[i][2]);
            w.settings = new WorldSettings();
            w.settings.type = Integer.parseInt(fake[i][3]);
            w.mod = now - (i * i * 7L + 1L) * 3600_000L;
            this.worldList.add(w);
         }

         this.worldName = fake[1][0];
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

   /** Update 3.13: the settings the Create New World screen is building, and those of the world that is open. */
   WorldSettings newSettings = new WorldSettings();
   WorldSettings worldSettings = WorldSettings.classic();

   void startWorld(boolean var1, String var2, int var3) {
      Panorama.stop(this); // Update 3.13: the turning world behind the menus makes room for the real one
      Properties var4 = new Properties();
      String var5 = this.newName;
      if (var1) {
         this.worldName = this.uniqueDir(this.newName.trim().isEmpty() ? "New World" : this.newName);
         var5 = this.newName.trim().isEmpty() ? "New World" : this.newName.trim();
      }

      File var8 = this.worldDir();
      long var6;
      if (var1) {
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

      int startDim = var1 ? Dim.OVERWORLD : (int)d(var4, "dim", 0.0);
      if (!Dim.valid(startDim)) {
         startDim = Dim.OVERWORLD;
      }

      this.overworldSettings = new Properties();
      this.overworldSettings.putAll(var4);
      this.portalTime = 0;
      this.portalLock = false;
      this.arrivalAt = null;
      WorldSettings ws;
      if (var1) {
         ws = this.newSettings.copy();
         if (System.getProperty("bc.auto") != null) {
            // the screenshot and test scenes were made on the classic terrain: they keep it unless a scene asks for another type
            ws.type = Integer.parseInt(System.getProperty("bc.gen", "0"));
            ws.flat = Integer.parseInt(System.getProperty("bc.flat", "0"));
            ws.structures = !"false".equals(System.getProperty("bc.structs", "true"));
            ws.bonusChest = "true".equals(System.getProperty("bc.bonus", "false"));
         }
      } else {
         ws = WorldSettings.load(var4);
      }

      boolean ephemeralMinigame = ws.isMinigame();
      if (ephemeralMinigame) {
         deleteDir(var8);
         this.rootDir = null;
         SkyGen.myTeam = 0;
         SkyGen.matchState = SkyGen.STATE_WAITING;
         SkyGen.resetMatchState();
      } else {
         if (var1) {
            deleteDir(var8);
         }
         var8.mkdirs();
         this.rootDir = var8;
      }

      this.worldSettings = ws;
      Net319.clearWorldState();
      this.bonusPending = var1 && ws.bonusChest && startDim == Dim.OVERWORLD && !ephemeralMinigame;
      this.world = new World(var6, ephemeralMinigame ? null : Dim.dir(var8, startDim), startDim, ws);
      if (ephemeralMinigame) {
         this.world.saveEnabled = false;
      }
      this.world.game = this;
      if (this.rend != null) {
         this.rend.setLod(this.world);
         this.world.unloadHook = this.rend::onUnload;
      }
      this.mobs.clear();
      this.targetMob = null;
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
      this.sayShaderProblems();
      this.bedPos = null;
      if (var1 || ephemeralMinigame) {
         this.player.setMode(var3);
         this.world.dayTime = 1000L;
         this.world.weather.startFair();
         int[] var16 = this.world.gen.findSpawn();
         this.spawnXZ = new int[]{var16[0], var16[2]};
         double startY = (this.world.gen instanceof BackroomsGen) ? (BackroomsGen.FLOOR_Y + 1.0) : 200.0;
         this.player.setPos(var16[0] + 0.5, startY, var16[2] + 0.5);
         this.needSpawn = true;
         if (var3 == 1) {
            int[] var10 = new int[]{1, 4, 5, 3, 15, 32, 9, 25, 7};

            for (int var11 = 0; var11 < var10.length; var11++) {
               this.inv[var11] = new Stack(var10[var11], 64);
            }
         } else if (ws.type == WorldSettings.BEDWARS) {
            SkyGen.giveBedwarsLoadout(this, SkyGen.myTeam);
            this.bedPos = SkyGen.getTeamBedFoot(SkyGen.myTeam);
            this.player.yaw = SkyGen.getTeamYaw(SkyGen.myTeam);
         } else if (ws.type == WorldSettings.SKYWARS) {
            this.player.yaw = SkyGen.getSkywarsYaw(0);
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
         if (this.world.dim == Dim.OVERWORLD) {
            this.restoreFireSetting(var4);
         }

         this.spawnXZ = new int[]{(int)d(var4, "spawnx", 0.0), (int)d(var4, "spawnz", 0.0)};
         this.bedPos = var4.getProperty("bedy") == null ? null : new int[]{(int)d(var4, "bedx", 0.0), (int)d(var4, "bedy", 0.0), (int)d(var4, "bedz", 0.0)};
         this.sel = (int)d(var4, "sel", 0.0);
         parseStacks(var4.getProperty("inv", ""), this.inv);
         parseStacks(var4.getProperty("armor", ""), this.armor);
         this.mobs.load(this.world.dir);
         this.needSpawn = false;
      }

      this.adv.load(this.rootDir); // Update 3.12: advancements (a new world starts with none)
      this.player.updateBox();
      this.setScreen(Game.S.LOADING);
   }

   /** Update 3.19: synchronizes a multiplayer client into the host's world seed, dimension, time of day, and position near the host. */
   void joinMultiplayerWorld(long seed, int dim, long dayTime, double sx, double sy, double sz) {
      joinMultiplayerWorld(seed, dim, dayTime, sx, sy, sz, WorldSettings.DEFAULT, 1);
   }

   void joinMultiplayerWorld(long seed, int dim, long dayTime, double sx, double sy, double sz, int worldType, int gameMode) {
      int targetDim = Dim.valid(dim) ? dim : Dim.OVERWORLD;
      int wType = Math.max(0, Math.min(WorldSettings.TYPES - 1, worldType));
      int gMode = Math.max(0, Math.min(4, gameMode));
      if (this.world == null
         || this.world.seed != seed
         || this.world.dim != targetDim
         || this.world.settings == null
         || this.world.settings.type != wType) {
         Panorama.stop(this);

         if (this.world != null) {
            for (Chunk c : this.world.chunks.values()) {
               c.unloaded = true;
               if (this.rend != null) {
                  this.rend.onUnload(c);
               }
            }

            this.world.chunks.clear();
            this.world.shutdown();
         }

         WorldSettings ws = new WorldSettings();
         ws.type = wType;
         this.worldSettings = ws;
         this.rootDir = null;
         this.bonusPending = false;
         this.worldDisplayName = WorldSettings.TYPE_NAME[wType] + " (Multiplayer)";
         this.world = new World(seed, null, targetDim, ws);
         this.world.saveEnabled = false;
         this.world.game = this;
         if (this.rend != null) {
            this.rend.setLod(this.world);
            this.world.unloadHook = this.rend::onUnload;
         }

         this.mobs.clear();
         this.targetMob = null;
         if (this.player == null) {
            this.player = new Player();
         }
         this.player.setMode(gMode);
         Arrays.fill(this.inv, null);
         Arrays.fill(this.armor, null);
         if (gMode == 1) {
            int[] startItems = new int[]{1, 4, 5, 3, 15, 32, 9, 25, 7};
            for (int i = 0; i < startItems.length; i++) {
               this.inv[i] = new Stack(startItems[i], 64);
            }
         } else if (wType == WorldSettings.BEDWARS) {
            int myTeam = SkyGen.getPlayerTeam(this);
            SkyGen.giveBedwarsLoadout(this, myTeam);
         }

         this.cursor = null;
         this.menu = null;
         this.eatTicks = 0;
         this.bowCharge = 0;
         this.deathHandled = false;
         this.particles.clear();
      }

      this.world.dayTime = dayTime;
      this.needSpawn = false;
      if (wType == WorldSettings.BEDWARS) {
         int myTeam = SkyGen.getPlayerTeam(this);
         int[] sp = SkyGen.getTeamSpawnBlock(myTeam);
         this.spawnXZ = new int[]{sp[0], sp[2]};
         this.bedPos = SkyGen.getTeamBedFoot(myTeam);
         this.player.setPos(sp[0] + 0.5, SkyGen.Y_DECK + 1.0, sp[2] + 0.5);
         this.player.yaw = SkyGen.getTeamYaw(myTeam);
      } else {
         this.bedPos = null;
         this.spawnXZ = new int[]{(int)Math.floor(sx), (int)Math.floor(sz)};
         double px = wsIsVoid(wType) ? sx : sx + 1.6;
         double py = Math.max(4.0, sy);
         double pz = sz;
         this.player.setPos(px, py, pz);
         this.player.yaw = wType == WorldSettings.SKYWARS ? (float)Math.toDegrees(Math.atan2(sx, -sz)) : -90.0F;
      }
      this.player.pitch = 0.0F;
      this.player.updateBox();
      if (this.screen != Game.S.PLAY && this.screen != Game.S.LOADING && this.screen != Game.S.WAITING) {
         this.setScreen(Game.S.LOADING);
      }
   }

   private static boolean wsIsVoid(int t) {
      return t == WorldSettings.SKYBLOCK || t == WorldSettings.SKYWARS || t == WorldSettings.BEDWARS;
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
      if (this.world.dim != Dim.OVERWORLD) {
         // somewhere else: the settings of the Overworld were stored when the player left it
         if (p != this.overworldSettings) {
            for (String k : this.overworldSettings.stringPropertyNames()) {
               p.setProperty(k, this.overworldSettings.getProperty(k));
            }
         }

         return;
      }

      p.setProperty("firespread", this.world.fire.spread + "");
      p.setProperty("treefall", this.world.trees.enabled + "");
      this.world.disasters.save(p);
      this.world.weather.save(p);
   }

   void restoreFireSetting(Properties p) {
      this.world.fire.spread = !"false".equals(p.getProperty("firespread"));
      this.world.trees.enabled = !"false".equals(p.getProperty("treefall"));
      this.world.disasters.load(p);
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
      if (this.world != null
         && this.pano == null
         && this.world.saveEnabled
         && (this.world.settings == null || !this.world.settings.isMinigame())) {
         this.world.saveAll();
         this.mobs.save(this.world.dir);
         if (this.world.fight != null) {
            this.world.fight.save(this);
         }

         this.adv.save(this.rootDir);

         if (this.rootDir == null && this.world.dir == null) {
            return; // a world that only lives in memory (tests)
         }

         Properties var1 = new Properties();
         var1.setProperty("seed", this.world.seed + "");
         var1.setProperty("dim", this.world.dim + "");
         var1.setProperty("name", this.worldDisplayName);
         var1.setProperty("x", this.player.x + "");
         var1.setProperty("y", this.player.y + "");
         var1.setProperty("z", this.player.z + "");
         var1.setProperty("yaw", this.player.yaw + "");
         var1.setProperty("pitch", this.player.pitch + "");
         var1.setProperty("health", Math.max(1, this.player.health) + "");
         var1.setProperty("creative", this.player.creative + "");
         var1.setProperty("mode", this.player.rawMode() + "");
         var1.setProperty("food", this.player.food + "");
         var1.setProperty("sat", this.player.sat + "");
         var1.setProperty("flying", this.player.flying + "");
         var1.setProperty("time", this.world.dayTime + "");
         this.worldSettings.save(var1);
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
            FileOutputStream var2 = new FileOutputStream(new File(this.rootDir != null ? this.rootDir : this.world.dir, "level.properties"));

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

   /**
    * Update 3.12: moves the player to another dimension. The world that is left is saved and closed, the new one is opened (it is its own
    * World object with its own chunks, tiles and mobs), and the loading screen is shown until the chunks around (x, z) are ready; then
    * {@link #finishArrival} puts the player where they belong.
    */
   void switchDimension(int dim, double x, double z, int kind) {
      this.switchDimension(dim, x, Double.NaN, z, kind);
   }

   /** Like the above; kind 2 puts the player at exactly (x, y, z). */
   void switchDimension(int dim, double x, double y, double z, int kind) {
      if (this.world == null || !Dim.valid(dim)) {
         return;
      }

      World old = this.world;
      if (old.dim == Dim.OVERWORLD) {
         this.storeFireSetting(this.overworldSettings);
      }

      this.saveWorld();
      for (Chunk c : old.chunks.values()) {
         c.unloaded = true;
         if (this.rend != null) {
            this.rend.onUnload(c);
         }
      }

      old.chunks.clear();
      old.shutdown();
      World nw = new World(old.seed, Dim.dir(this.rootDir != null ? this.rootDir : old.dir, dim), dim, this.worldSettings);
      nw.game = this;
      nw.saveEnabled = old.saveEnabled;
      nw.dayTime = old.dayTime;
      this.world = nw;
      if (this.rend != null) {
         nw.unloadHook = this.rend::onUnload;
         this.rend.setLod(nw);
      }

      this.mobs.clear();
      this.mobs.load(nw.dir);
      if (dim == Dim.OVERWORLD) {
         this.restoreFireSetting(this.overworldSettings);
      }

      this.target = null;
      this.targetMob = null;
      this.particles.clear();
      this.portalTime = 0;
      this.player.setPos(x, dim == Dim.NETHER ? 70.0 : (dim == Dim.END ? 70.0 : 200.0), z);
      this.arrivalAt = new double[]{x, z, y};
      this.arrivalKind = kind;
      this.setScreen(Game.S.LOADING);
   }

   /** The chunks around the arrival point are loaded: use or build the portal there, and put the player in it. */
   void finishArrival() {
      double[] at = this.arrivalAt;
      this.arrivalAt = null;
      double[] pos = null;
      if (this.arrivalKind == 0) {
         pos = Portals.arrive(this.world, at[0], at[1]);
         this.portalLock = true;
      } else if (this.arrivalKind == 2) {
         pos = new double[]{at[0], Double.isNaN(at[2]) ? 70.0 : at[2], at[1]};
      } else {
         pos = this.findStand((int)Math.floor(at[0]), (int)Math.floor(at[1]));
      }

      if (pos == null) {
         pos = new double[]{at[0], 100.0, at[1]};
      }

      this.player.setPos(pos[0], pos[1], pos[2]);
      this.player.fallDistance = 0.0F;
      this.player.updateBox();
      if (this.world.fight != null) {
         this.world.fight.enter(this);
      }
   }

   /** Where you wake up in the Overworld after the End or the Nether: next to your bed, else at the world spawn (x, z). */
   double[] homeXZ() {
      int[] h = this.bedPos != null ? new int[]{this.bedPos[0], this.bedPos[2]} : this.spawnXZ;
      return new double[]{h[0] + 0.5, h[1] + 0.5};
   }

   /** Update 3.12: a coloured spark that drifts about (teleporting endermen, portals, eyes of ender, the dragon's breath). */
   void portalParticle(double x, double y, double z, float r, float g, float b) {
      Game.Particle p = new Game.Particle();
      p.x = (float)x;
      p.y = (float)y;
      p.z = (float)z;
      p.vx = (this.rnd.nextFloat() - 0.5F) * 0.05F;
      p.vy = (this.rnd.nextFloat() - 0.3F) * 0.04F;
      p.vz = (this.rnd.nextFloat() - 0.5F) * 0.05F;
      p.u = (Tex.tx(64) + 1) / (float)Tex.PX;
      p.v = (Tex.ty(64) + 1) / (float)Tex.PX;
      p.shade = 1.0F;
      p.grav = 0.0F;
      p.size = 0.07F + this.rnd.nextFloat() * 0.06F;
      p.life = 16 + this.rnd.nextInt(14);
      p.r = r;
      p.g = g * (0.8F + this.rnd.nextFloat() * 0.4F);
      p.b = b;
      this.particles.add(p);
   }

   /** Update 3.12: a message in big letters at the top of the screen for a few seconds (the dragon is dead, ...). */
   void toast(String title, String text) {
      this.toastTitle = title;
      this.toastText = text;
      this.toastTicks = 260;
   }

   String toastTitle = "";
   String toastText = "";
   int toastTicks;

   /** The boss bar of the Ender Dragon and the toast, drawn on top of the hotbar's layer. */
   void drawBossBar() {
      if (this.world != null && this.world.fight != null && !this.player.spectator) {
         Mobs.Mob d = this.world.fight.dragon(this.mobs);
         if (d != null) {
            float x = (float)Math.floor((this.gui.w - 182.0F) / 2.0F);
            float frac = Math.max(0.0F, Math.min(1.0F, d.health / (float)EndFight.DRAGON_HP));
            this.gui.centered("Ender Dragon", this.gui.w / 2.0F, 6.0F, 0xFFFFFF, true, 1.0F);
            this.gui.rect(x - 1.0F, 17.0F, 184.0F, 9.0F, 0xFF000000);
            this.gui.rect(x, 18.0F, 182.0F, 7.0F, 0xFF3A1250);
            this.gui.rect(x, 18.0F, 182.0F * frac, 7.0F, 0xFFC040F0);
            this.gui.rect(x, 18.0F, 182.0F * frac, 2.0F, 0xFFE8A0FF);
         }
      }

      if (this.toastTicks > 0) {
         float fade = Math.min(1.0F, this.toastTicks / 30.0F);
         int a = (int)(255.0F * fade) << 24;
         this.gui.centered(this.toastTitle, this.gui.w / 2.0F, this.gui.h * 0.2F, 0xFFFF80 | a, true, 2.0F);
         this.gui.centered(this.toastText, this.gui.w / 2.0F, this.gui.h * 0.2F + 24.0F, 0xFFFFFF | a, true, 1.0F);
      }
   }

   void quitToTitle() {
      Net319.disconnect(this);
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
      Panorama.start(this);
   }

   double[] findStand(int var1, int var2) {
      if (this.world != null && this.world.gen instanceof BackroomsGen) {
         int fy = BackroomsGen.FLOOR_Y;
         for (int r = 0; r <= 10; r++) {
            for (int dx = -r; dx <= r; dx++) {
               for (int dz = -r; dz <= r; dz++) {
                  if (Math.max(Math.abs(dx), Math.abs(dz)) == r) {
                     int bx = var1 + dx;
                     int bz = var2 + dz;
                     if (this.world.isLoaded(bx, bz)) {
                        if (this.world.getBlock(bx, fy + 1, bz) == Blocks.AIR
                           && this.world.getBlock(bx, fy + 2, bz) == Blocks.AIR) {
                           return new double[]{bx + 0.5, fy + 1.0, bz + 0.5};
                        }
                     }
                  }
               }
            }
         }
         return new double[]{var1 + 0.5, fy + 1.0, var2 + 0.5};
      }

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
                           if (Blocks.solid[var9] && !Blocks.isLeaves(var9) && !Blocks.isAnyLog(var9) && var9 != 28) {
                              return new double[]{var6 + 0.5, var8 + 1, var7 + 0.5};
                           }

                           if (!Blocks.isFluid(var9) && Blocks.coll[var9] == null && !Blocks.isLeaves(var9)) {
                              continue; // Update 3.13: grass, flowers, ferns and a cover of snow do not stop you: look for the ground below them
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
      if (this.world != null && this.world.settings != null && this.world.settings.type == WorldSettings.BEDWARS) {
         int myTeam = SkyGen.getPlayerTeam(this);
         boolean bedIntact = SkyGen.isTeamBedIntact(this.world, myTeam);
         this.player.dead = false;
         this.player.burn = 0;
         this.player.health = 20;
         this.player.food = 20;
         this.player.sat = 5.0F;
         this.player.exh = 0.0F;
         this.player.air = 300;
         this.player.fallDistance = 0.0F;
         this.player.invuln = 40;
         this.deathHandled = false;
         if (bedIntact) {
            int[] sp = SkyGen.getTeamSpawnBlock(myTeam);
            this.spawnXZ = new int[]{sp[0], sp[2]};
            this.bedPos = SkyGen.getTeamBedFoot(myTeam);
            this.player.setPos(sp[0] + 0.5, SkyGen.Y_DECK + 1.0, sp[2] + 0.5);
            this.player.yaw = SkyGen.getTeamYaw(myTeam);
            this.player.pitch = 0.0F;
            this.player.updateBox();
            SkyGen.giveBedwarsRespawnKit(this, myTeam);
            this.say("\u00a7a[Bedwars] Respawned at " + SkyGen.BEDWARS_CODES[myTeam] + SkyGen.BEDWARS_NAMES[myTeam] + " Base\u00a7a!");
         } else {
            this.bedPos = null;
            this.player.setMode(3);
            this.player.setPos(0.5, SkyGen.Y_DECK + 16.0, 0.5);
            this.player.updateBox();
            this.say("\u00a7c\u00a7l[Bedwars] ELIMINATED! \u00a77Your bed was destroyed — spectating match...");
         }
         return;
      }

      if (this.world != null && this.world.settings != null && this.world.settings.type == WorldSettings.SKYWARS) {
         int myTeam = SkyGen.getPlayerTeam(this);
         int[] sp = SkyGen.getTeamSpawnBlock(myTeam);
         this.player.dead = false;
         this.player.burn = 0;
         this.player.health = 20;
         this.player.food = 20;
         this.player.sat = 5.0F;
         this.player.exh = 0.0F;
         this.player.air = 300;
         this.player.fallDistance = 0.0F;
         this.player.invuln = 40;
         this.deathHandled = false;
         this.spawnXZ = new int[]{sp[0], sp[2]};
         this.player.setPos(sp[0] + 0.5, SkyGen.Y_DECK + 1.0, sp[2] + 0.5);
         this.player.yaw = SkyGen.getTeamYaw(myTeam);
         this.player.pitch = 0.0F;
         this.player.updateBox();
         SkyGen.giveSkywarsKit(this, SkyGen.swKit);
         this.say("§a[Skywars] Respawned at Spawn Island #" + (myTeam + 1) + " with Kit: §e" + SkyGen.SW_KIT_NAMES[SkyGen.swKit] + "§a!");
         return;
      }

      double[] var1 = null;
      if (this.world.dim != Dim.OVERWORLD) {
         // Update 3.12: whoever dies in the Nether or in the End wakes up at home in the Overworld
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
         double[] home = this.homeXZ();
         this.switchDimension(Dim.OVERWORLD, home[0], home[1], 1);
         return;
      }

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
      if (var1 == Game.S.LOADING && var2 != Game.S.LOADING) {
         this.loadFrac = 0.0F;
      }

      if (var1 == Game.S.WORLDS && var2 != Game.S.WORLDS) {
         this.scanWorlds();
      }

      boolean var3 = var1 == Game.S.PLAY;
      if (var3 != this.grabbed) {
         this.grabbed = var3;
         if (this.window != 0L) { // (tests have no window)
            GLFW.glfwSetInputMode(this.window, 208897, var3 ? 212995 : 212993);
            if (var3 && GLFW.glfwRawMouseMotionSupported()) {
               GLFW.glfwSetInputMode(this.window, 208901, 1);
            }
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
         this.player.pitch = (float)(this.player.pitch + (this.opt.invertY ? -var7 : var7) * var10 * 0.15);
         if (this.player.pitch > 90.0F) {
            this.player.pitch = 90.0F;
         }

         if (this.player.pitch < -90.0F) {
            this.player.pitch = -90.0F;
         }
      }
   }

   void onScroll(double var1) {
      if (this.ui.onWheel(var1)) {
         return;
      }

      if (this.screen == Game.S.SKINS) {
         this.skinScroll = Math.max(0, this.skinScroll - (int)Math.signum(var1));
      } else if (this.screen == Game.S.WORLDS) {
         this.worldScroll = Math.max(0, Math.min(Math.max(0, this.worldList.size() - 5), this.worldScroll - (int)Math.signum(var1)));
      } else {
         if (this.screen == Game.S.PLAY && this.key(67)) {
            if (var1 != 0.0) {
               this.zoomFactor = Math.max(1.5F, Math.min(12.0F, this.zoomFactor * (var1 > 0.0 ? 1.25F : 0.8F)));
            }
         } else if (this.screen == Game.S.PLAY && (this.player.spectator || (this.player.creative && this.player.flying && (this.key(341) || this.key(345))))) {
            if (var1 != 0.0) {
               this.player.flySpeed = Math.max(0.01F, Math.min(0.25F, this.player.flySpeed + (float)Math.signum(var1) * 0.01F));
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
      } else if (this.screen == Game.S.SKINS) {
         if (this.skinFocus && this.skinField.length() < 16 && (var1 >= 48 && var1 <= 57 || var1 >= 65 && var1 <= 90 || var1 >= 97 && var1 <= 122 || var1 == 95)) {
            this.skinField = this.skinField + (char)var1;
         }
      } else if (this.screen == Game.S.NEWWORLD) {
         if (var1 >= 32 && var1 < 127) {
            if (this.focusField == 0 && this.newName.length() < 24) {
               this.newName = this.newName + (char)var1;
            } else if (this.focusField == 1 && this.seedField.length() < 30) {
               this.seedField = this.seedField + (char)var1;
            }
         }
      } else if (this.screen == Game.S.MULTIPLAYER) {
         if (var1 >= 32 && var1 < 127 && Net319.serverAddr.length() < 64) {
            Net319.serverAddr = Net319.serverAddr + (char)var1;
         }
      } else if (this.screen == Game.S.INV && this.menu != null && this.menu.kind == 4 && this.menu.tab == Creative.SEARCH) {
         if (System.currentTimeMillis() - this.menu.openedAt > 100L) {
            this.menu.typed((char)var1);
         }
      } else if (this.screen == Game.S.PLAY && var1 == 47) {
         this.openChat("/");
      }
   }

   public String getClipboard() {
      if (this.window != 0L) {
         try {
            String s = GLFW.glfwGetClipboardString(this.window);
            if (s != null) {
               clipboardFallback = s;
               return s;
            }
         } catch (Throwable ignored) {
         }
      }
      return clipboardFallback != null ? clipboardFallback : "";
   }

   public void setClipboard(String s) {
      String val = s == null ? "" : s;
      clipboardFallback = val;
      if (this.window != 0L) {
         try {
            GLFW.glfwSetClipboardString(this.window, val);
         } catch (Throwable ignored) {
         }
      }
   }

   static String sanitizePaste(String raw, int maxLen) {
      if (raw == null || raw.isEmpty() || maxLen <= 0) {
         return "";
      }
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < raw.length() && sb.length() < maxLen; i++) {
         char c = raw.charAt(i);
         if (c >= 32 && c < 127) {
            sb.append(c);
         }
      }
      return sb.toString();
   }

   static String deleteLastWord(String s) {
      if (s == null || s.isEmpty()) {
         return "";
      }
      int i = s.length() - 1;
      while (i >= 0 && s.charAt(i) == ' ') {
         i--;
      }
      while (i >= 0 && s.charAt(i) != ' ') {
         i--;
      }
      return s.substring(0, i + 1);
   }

   static void recordHistory(ArrayList<String> hist, String val) {
      if (val == null || val.trim().isEmpty()) {
         return;
      }
      String t = val.trim();
      if (hist.isEmpty() || !hist.get(hist.size() - 1).equals(t)) {
         hist.add(t);
      }
   }

   void openChat(String var1) {
      this.setScreen(Game.S.CHAT);
      this.chat.setLength(0);
      this.chat.append(var1);
      this.chatDraft = var1;
      this.chatHistoryPos = this.chatHistory.size();
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
                           case 70:
                              if (!this.player.spectator && this.sel != 8) {
                                 Stack tmp = this.inv[this.sel];
                                 this.inv[this.sel] = this.inv[8];
                                 this.inv[8] = tmp;
                                 this.nameTimer = 40.0F;
                                 this.equip = 0.0F;
                              }

                              return;
                           case 71:
                              Net319.sendWorldPing(this);
                              return;
                           case 76:
                              this.setScreen(Game.S.ADV); // Update 3.12: the advancements
                              return;
                           case 78:
                              if (this.key(292) || (var3 & 2) != 0) {
                                 int nextM = this.player.spectator ? 1 : 3;
                                 this.spectatingMob = null;
                                 this.player.setMode(nextM);
                                 this.say("Set own game mode to " + this.player.modeName() + " Mode");
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
                              if ((var3 & 1) != 0) {
                                 Perf.toggleShow(new File(this.root, "perf.log")); // Update 3.18: Shift+F3 = the frame timer, Ctrl+F3 = exact
                              } else if ((var3 & 2) != 0) {
                                 Perf.toggleExact();
                              } else {
                                 this.debug = !this.debug;
                              }

                              return;
                           case 293:
                              if (this.key(292) || (var3 & 2) != 0) {
                                 int nextM = (this.player.rawMode() + 1) % 5;
                                 this.spectatingMob = null;
                                 this.player.setMode(nextM);
                                 this.say("Set own game mode to " + this.player.modeName() + " Mode");
                              }

                              return;
                           case 294:
                              this.view = (this.view + 1) % 3;
                              this.say("Camera: " + new String[]{"first person", "behind you", "in front of you"}[this.view]);
                              return;
                           default:
                              if (var1 >= 49 && var1 <= 57) {
                                 if (this.player.spectator) {
                                    this.spectatorAction(var1 - 48);
                                 } else {
                                    this.sel = var1 - 49;
                                    this.nameTimer = 40.0F;
                                 }
                              }
                        }
                     }

                     return;
                  }
               case SKINS:
                  this.skinKey(var1, var2, var3);
                  return;
               case WORLDS:
                  if (var1 == 256 && var2 == 1) {
                     this.setScreen(Game.S.TITLE);
                  }

                  return;
               case MULTIPLAYER: {
                  boolean ctrl = (var3 & 10) != 0;
                  if (ctrl && var1 == 67 && var2 == 1) {
                     this.setClipboard(Net319.serverAddr);
                  } else if (ctrl && var1 == 88 && var2 == 1) {
                     this.setClipboard(Net319.serverAddr);
                     Net319.serverAddr = "";
                  } else if (ctrl && var1 == 86) {
                     String paste = sanitizePaste(this.getClipboard(), 64 - Net319.serverAddr.length());
                     Net319.serverAddr = Net319.serverAddr + paste;
                  } else if ((var1 == 259 || var1 == 261) && Net319.serverAddr.length() > 0) {
                     Net319.serverAddr = ctrl
                        ? deleteLastWord(Net319.serverAddr)
                        : Net319.serverAddr.substring(0, Net319.serverAddr.length() - 1);
                  } else if (var1 == 265 && !this.addrHistory.isEmpty()) {
                     if (this.addrHistoryPos < 0 || this.addrHistoryPos > this.addrHistory.size()) {
                        this.addrHistoryPos = this.addrHistory.size();
                     }
                     if (this.addrHistoryPos > 0) {
                        this.addrHistoryPos--;
                        Net319.serverAddr = this.addrHistory.get(this.addrHistoryPos);
                     }
                  } else if (var1 == 264 && !this.addrHistory.isEmpty()) {
                     if (this.addrHistoryPos >= 0 && this.addrHistoryPos < this.addrHistory.size() - 1) {
                        this.addrHistoryPos++;
                        Net319.serverAddr = this.addrHistory.get(this.addrHistoryPos);
                     }
                  } else if (var1 == 256 && var2 == 1) {
                     this.setScreen(this.mpReturn != null ? this.mpReturn : Game.S.TITLE);
                  } else if ((var1 == 257 || var1 == 335) && var2 == 1) {
                     recordHistory(this.addrHistory, Net319.serverAddr);
                     this.addrHistoryPos = this.addrHistory.size();
                     Net319.joinFromMenu(this, Net319.serverAddr);
                  }

                  return;
               }
               case NEWWORLD: {
                  boolean ctrl = (var3 & 10) != 0;
                  if (ctrl && var1 == 67 && var2 == 1) {
                     this.setClipboard(this.focusField == 0 ? this.newName : this.seedField);
                     return;
                  }
                  if (ctrl && var1 == 88 && var2 == 1) {
                     this.setClipboard(this.focusField == 0 ? this.newName : this.seedField);
                     if (this.focusField == 0) {
                        this.newName = "";
                     } else {
                        this.seedField = "";
                     }
                     return;
                  }
                  if (ctrl && var1 == 86) {
                     if (this.focusField == 0) {
                        this.newName = this.newName + sanitizePaste(this.getClipboard(), 24 - this.newName.length());
                     } else {
                        this.seedField = this.seedField + sanitizePaste(this.getClipboard(), 30 - this.seedField.length());
                     }
                     return;
                  }
                  if (var1 == 259 || var1 == 261) {
                     if (this.focusField == 0 && this.newName.length() > 0) {
                        this.newName = ctrl ? deleteLastWord(this.newName) : this.newName.substring(0, this.newName.length() - 1);
                     } else if (this.focusField == 1 && this.seedField.length() > 0) {
                        this.seedField = ctrl ? deleteLastWord(this.seedField) : this.seedField.substring(0, this.seedField.length() - 1);
                     }
                  }

                  if (var1 == 265 && var2 == 1) {
                     if (this.focusField == 1 && !this.seedHistory.isEmpty() && this.seedField.isEmpty()) {
                        this.seedField = this.seedHistory.get(this.seedHistory.size() - 1);
                     } else {
                        this.focusField = 0;
                     }
                  } else if (var1 == 264 && var2 == 1) {
                     this.focusField = 1;
                  }

                  if (var1 == 258 && var2 == 1) {
                     this.focusField = 1 - this.focusField;
                  }

                  if (var1 == 256 && var2 == 1) {
                     this.scanWorlds();
                     this.setScreen(this.worldList.isEmpty() ? Game.S.TITLE : Game.S.WORLDS);
                  }

                  if (var1 == 257 && var2 == 1) {
                     recordHistory(this.seedHistory, this.seedField);
                     Screens.createWorld(this);
                  }

                  return;
               }
               case LOADING:
                  return;
               case WAITING: {
                  if (var1 == 256 && var2 == 1) {
                     this.quitToTitle();
                  } else if (var1 == 257 && var2 == 1) {
                     SkyGen.startMatch(this, true);
                  }
                  return;
               }
               case CHAT: {
                  boolean ctrl = (var3 & 10) != 0;
                  if (var1 == 256 && var2 == 1) {
                     this.setScreen(Game.S.PLAY);
                  } else if (ctrl && var1 == 67 && var2 == 1) {
                     this.setClipboard(this.chat.toString());
                  } else if (ctrl && var1 == 88 && var2 == 1) {
                     this.setClipboard(this.chat.toString());
                     this.chat.setLength(0);
                  } else if (ctrl && var1 == 86) {
                     String paste = sanitizePaste(this.getClipboard(), 100 - this.chat.length());
                     this.chat.append(paste);
                  } else if ((var1 == 259 || var1 == 261) && this.chat.length() > 0) {
                     if (ctrl) {
                        String rest = deleteLastWord(this.chat.toString());
                        this.chat.setLength(0);
                        this.chat.append(rest);
                     } else {
                        this.chat.setLength(this.chat.length() - 1);
                     }
                  } else if (var1 == 265 && !this.chatHistory.isEmpty()) {
                     if (this.chatHistoryPos < 0 || this.chatHistoryPos >= this.chatHistory.size()) {
                        this.chatDraft = this.chat.toString();
                        this.chatHistoryPos = this.chatHistory.size();
                     }
                     if (this.chatHistoryPos > 0) {
                        this.chatHistoryPos--;
                        this.chat.setLength(0);
                        this.chat.append(this.chatHistory.get(this.chatHistoryPos));
                     }
                  } else if (var1 == 264 && !this.chatHistory.isEmpty()) {
                     if (this.chatHistoryPos >= 0 && this.chatHistoryPos < this.chatHistory.size() - 1) {
                        this.chatHistoryPos++;
                        this.chat.setLength(0);
                        this.chat.append(this.chatHistory.get(this.chatHistoryPos));
                     } else if (this.chatHistoryPos == this.chatHistory.size() - 1) {
                        this.chatHistoryPos = this.chatHistory.size();
                        this.chat.setLength(0);
                        this.chat.append(this.chatDraft);
                     }
                  } else if ((var1 == 257 || var1 == 335) && var2 == 1) {
                     String var4 = this.chat.toString().trim();
                     this.setScreen(Game.S.PLAY);
                     if (!var4.isEmpty()) {
                        recordHistory(this.chatHistory, var4);
                        this.chatHistoryPos = this.chatHistory.size();
                        this.runCommand(var4);
                     }
                  }

                  return;
               }
               case INV:
                  if (this.menu != null && this.menu.kind == 4 && this.menu.tab == Creative.SEARCH) {
                     // the search tab: letters are for the search field, only Esc closes
                     boolean ctrl = (var3 & 10) != 0;
                     if (ctrl && var1 == 67 && var2 == 1) {
                        this.setClipboard(this.menu.search.toString());
                     } else if (ctrl && var1 == 88 && var2 == 1) {
                        this.setClipboard(this.menu.search.toString());
                        this.menu.clearSearch();
                     } else if (ctrl && var1 == 86) {
                        this.menu.pasteSearch(sanitizePaste(this.getClipboard(), 24 - this.menu.search.length()));
                     } else if (var1 == 265) {
                        this.menu.stepSearchHistory(-1);
                     } else if (var1 == 264) {
                        this.menu.stepSearchHistory(1);
                     } else if (var2 != 0 && (var1 == 259 || var1 == 261)) {
                        this.menu.backspace();
                     } else if (var2 == 1 && var1 == 256) {
                        this.setScreen(Game.S.PLAY);
                     }

                     return;
                  }

                  if (var2 == 1 && (var1 == 256 || var1 == 69)) {
                     this.setScreen(Game.S.PLAY);
                  } else if (var2 == 1 && this.menu != null && var1 >= 49 && var1 <= 57) {
                     this.menu.hotbarKey(var1 - 49, (float)(this.mouseX / this.gui.scale), (float)(this.mouseY / this.gui.scale), this.menuOx(), this.menuOy());
                  } else if (var2 == 1 && this.menu != null && var1 == 81) {
                     this.menu.dropKey((float)(this.mouseX / this.gui.scale), (float)(this.mouseY / this.gui.scale), this.menuOx(), this.menuOy(), (var3 & 2) != 0);
                  }

                  return;
               case PAUSE:
                  if (var2 == 1 && var1 == 256) {
                     this.setScreen(Game.S.PLAY);
                  }

                  return;
               case ADV:
                  if (var2 == 1 && (var1 == 256 || var1 == 76)) {
                     this.setScreen(Game.S.PLAY);
                  }

                  return;
               case PACKS:
                  if (var2 == 1 && var1 == 256) {
                     this.setScreen(this.packsReturn != null ? this.packsReturn : Game.S.TITLE);
                  }

                  return;
               case MODS:
                  if (var2 == 1 && var1 == 256) {
                     this.setScreen(this.modsReturn != null ? this.modsReturn : Game.S.TITLE);
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
      recordHistory(this.chatHistory, var1);
      this.chatHistoryPos = this.chatHistory.size();
      if (!var1.startsWith("/")) {
         this.say("<" + Net319.selfName + "> " + var1);
         Net319.sendChat(this, var1);
      } else {
         if (Net319.onNetCommand(this, var1)) {
            return;
         }
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
                     } else if (var16.equals("4") || var16.startsWith("h")) {
                        var19 = 4;
                     }
                  } else {
                     var19 = 0;
                  }

                  if (var19 < 0) {
                     this.say("§cUsage: /gamemode <survival|creative|adventure|spectator|hardcore>");
                  } else {
                     this.spectatingMob = null;
                     this.player.setMode(var19);
                     String[] var23 = new String[]{"Survival", "Creative", "Adventure", "Spectator", "Hardcore"};
                     this.say("Set own game mode to " + var23[var19] + " Mode");
                  }
                  break;
               case "time":
                  if (var2.length >= 3 && var2[1].equalsIgnoreCase("set")) {
                     String var15 = var2[2].toLowerCase();
                     long var18;
                     switch (var15) {
                        case "day":
                           var18 = 1000L;
                           break;
                        case "noon":
                           var18 = 6000L;
                           break;
                        case "sunset":
                        case "dusk":
                           var18 = 12000L;
                           break;
                        case "night":
                           var18 = 13000L;
                           break;
                        case "midnight":
                           var18 = 18000L;
                           break;
                        case "sunrise":
                        case "dawn":
                           var18 = 23000L;
                           break;
                        default:
                           var18 = Long.parseLong(var15);
                     }

                     this.world.dayTime = var18;
                     this.say("Set the time to " + var18);
                  } else if (var2.length >= 3 && var2[1].equalsIgnoreCase("add")) {
                     long delta = Long.parseLong(var2[2]);
                     this.world.dayTime = Math.max(0L, this.world.dayTime + delta);
                     this.say("Added " + delta + " to time (now " + this.world.dayTime + ")");
                  } else if (var2.length == 1 || var2[1].equalsIgnoreCase("query")) {
                     this.say("Current time is " + this.world.dayTime + " (day " + (this.world.dayTime / 24000L) + ")");
                  } else {
                     this.say("§cUsage: /time <set|add|query> <day|noon|sunset|night|midnight|sunrise|ticks>");
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
               case "advancement":
               case "adv":
                  String advSub = var2.length > 1 ? var2[1].toLowerCase() : "list";
                  if (advSub.equals("grant") && var2.length > 2) {
                     if (var2[2].equals("all") || var2[2].equals("everything")) {
                        for (Adv312.A a : Adv312.LIST) {
                           this.adv.award(this, a.id);
                        }

                        this.say("Granted every advancement");
                     } else if (Adv312.BY_ID.containsKey(var2[2])) {
                        this.adv.award(this, var2[2]);
                        this.say("Granted " + var2[2]);
                     } else {
                        this.say("§cNo advancement called " + var2[2]);
                     }
                  } else if (advSub.equals("revoke") && var2.length > 2 && var2[2].equals("all")) {
                     this.adv.revokeAll(this);
                     this.say("Took back every advancement");
                  } else {
                     this.say("Advancements: " + this.adv.count() + " of " + Adv312.LIST.size() + " made (press L).  /advancement grant <id|all>   /advancement revoke all");
                  }
                  break;
               case "dim":
               case "dimension":
                  int dn = var2.length > 1 ? Dim.parse(var2[1]) : Integer.MIN_VALUE;
                  if (dn == Integer.MIN_VALUE) {
                     this.say("§cUsage: /dim <overworld|nether|end>   (you are in " + Dim.name(this.world.dim) + ")");
                  } else if (dn == this.world.dim) {
                     this.say("You are already in " + Dim.name(dn));
                  } else if (dn == Dim.END) {
                     this.say("Travelling to " + Dim.name(dn) + " ...");
                     this.switchDimension(Dim.END, EndGen.PLATFORM_X + 0.5, EndGen.PLATFORM_Y + 1.0, EndGen.PLATFORM_Z + 0.5, 2);
                  } else if (this.world.dim == Dim.END) {
                     double[] home = this.homeXZ();
                     this.say("Travelling to " + Dim.name(dn) + " ...");
                     this.switchDimension(Dim.OVERWORLD, home[0], home[1], 1);
                  } else {
                     double dscale = Dim.scale(this.world.dim, dn);
                     this.say("Travelling to " + Dim.name(dn) + " ...");
                     this.switchDimension(dn, this.player.x * dscale, this.player.z * dscale, 0);
                  }
                  break;
               case "summon":
                  int var13 = -1;
                  String var17 = var2.length > 1 ? var2[1].toLowerCase() : "";

                  for (int var20 = 0; var20 < Mobs.N; var20++) {
                     if (Mobs.NAME[var20].equals(var17)) {
                        var13 = var20;
                     }
                  }

                  if (var13 < 0) {
                     this.say("§cUsage: /summon <" + String.join("|", java.util.Arrays.copyOf(Mobs.NAME, Mobs.N)) + "> [count]");
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
                  if (var2.length > 1 && (var2[1].equalsIgnoreCase("mobs") || var2[1].equalsIgnoreCase("all") || var2[1].equalsIgnoreCase("@e"))) {
                     int removed = this.mobs.list.size();
                     this.mobs.list.clear();
                     this.say("Removed " + removed + " mob(s)");
                  } else {
                     this.player.health = 0;
                     this.player.dead = true;
                  }
                  break;
               case "butcher":
               case "killall":
                  int butchered = this.mobs.list.size();
                  this.mobs.list.clear();
                  this.say("Removed " + butchered + " mob(s)");
                  break;
               case "clear":
                  String clearFilter = var2.length > 1 ? var2[1].toLowerCase().replace('_', ' ') : "";
                  int cleared = 0;
                  for (int si = 0; si < this.inv.length; si++) {
                     Stack st = this.inv[si];
                     if (st != null && st.n > 0 && (clearFilter.isEmpty() || Items.name(st.id).toLowerCase().contains(clearFilter))) {
                        cleared += st.n;
                        this.inv[si] = null;
                     }
                  }
                  this.say("Cleared " + cleared + " item(s) from inventory");
                  break;
               case "spawn":
               case "home":
                  double[] hxz = this.homeXZ();
                  int hx = (int)Math.floor(hxz[0]);
                  int hz = (int)Math.floor(hxz[1]);
                  double[] stand = this.findStand(hx, hz);
                  this.player.setPos(stand[0], stand[1], stand[2]);
                  this.player.fallDistance = 0.0F;
                  this.say("Teleported to spawn [" + (int)Math.floor(stand[0]) + ", " + (int)Math.floor(stand[1]) + ", " + (int)Math.floor(stand[2]) + "]");
                  break;
               case "spawnpoint":
               case "setspawn":
                  int spx = var2.length >= 4 ? (int)Math.floor(rel(var2[1], this.player.x)) : (int)Math.floor(this.player.x);
                  int spy = var2.length >= 4 ? (int)Math.floor(rel(var2[2], this.player.y)) : (int)Math.floor(this.player.y);
                  int spz = var2.length >= 4 ? (int)Math.floor(rel(var2[3], this.player.z)) : (int)Math.floor(this.player.z);
                  this.spawnXZ = new int[]{spx, spz};
                  this.say("Set world spawn point to [" + spx + ", " + spy + ", " + spz + "]");
                  break;
               case "top":
                  int tx = (int)Math.floor(this.player.x);
                  int tz = (int)Math.floor(this.player.z);
                  double[] tStand = this.findStand(tx, tz);
                  this.player.setPos(tStand[0], tStand[1], tStand[2]);
                  this.player.fallDistance = 0.0F;
                  this.say("Teleported to surface [" + tx + ", " + (int)Math.floor(tStand[1]) + ", " + tz + "]");
                  break;
               case "setblock":
                  if (var2.length < 5) {
                     this.say("§cUsage: /setblock <x> <y> <z> <block>");
                  } else {
                     int sbx = (int)Math.floor(rel(var2[1], this.player.x));
                     int sby = (int)Math.floor(rel(var2[2], this.player.y));
                     int sbz = (int)Math.floor(rel(var2[3], this.player.z));
                     int bid = resolveBlockArg(var2[4]);
                     if (bid < 0) {
                        this.say("§cUnknown block: " + var2[4]);
                     } else {
                        this.world.setBlock(sbx, sby, sbz, bid);
                        if (Redstone319.handlesUpdate(bid)) {
                           Redstone319.propagate(this.world, sbx, sby, sbz);
                        }
                        this.meshNow(sbx, sby, sbz);
                        this.say("Set block at [" + sbx + ", " + sby + ", " + sbz + "] to [" + (bid == 0 ? "Air" : Items.name(bid)) + "]");
                     }
                  }
                  break;
               case "fill":
                  if (var2.length < 8) {
                     this.say("§cUsage: /fill <x1> <y1> <z1> <x2> <y2> <z2> <block>");
                  } else {
                     int fx1 = (int)Math.floor(rel(var2[1], this.player.x));
                     int fy1 = (int)Math.floor(rel(var2[2], this.player.y));
                     int fz1 = (int)Math.floor(rel(var2[3], this.player.z));
                     int fx2 = (int)Math.floor(rel(var2[4], this.player.x));
                     int fy2 = (int)Math.floor(rel(var2[5], this.player.y));
                     int fz2 = (int)Math.floor(rel(var2[6], this.player.z));
                     int x0 = Math.min(fx1, fx2);
                     int x1 = Math.max(fx1, fx2);
                     int y0 = Math.max(0, Math.min(fy1, fy2));
                     int y1 = Math.min(255, Math.max(fy1, fy2));
                     int z0 = Math.min(fz1, fz2);
                     int z1 = Math.max(fz1, fz2);
                     long vol = (long)(x1 - x0 + 1) * (y1 - y0 + 1) * (z1 - z0 + 1);
                     int bid = resolveBlockArg(var2[7]);
                     if (bid < 0) {
                        this.say("§cUnknown block: " + var2[7]);
                     } else if (vol > 4096L) {
                        this.say("§cToo many blocks in the specified area (" + vol + " > 4096)");
                     } else {
                        int count = 0;
                        for (int yy = y0; yy <= y1; yy++) {
                           for (int zz = z0; zz <= z1; zz++) {
                              for (int xx = x0; xx <= x1; xx++) {
                                 if (this.world.setBlock(xx, yy, zz, bid)) {
                                    count++;
                                 }
                              }
                           }
                        }
                        this.meshNow(x0, y0, z0);
                        this.meshNow(x1, y1, z1);
                        this.say("Filled " + count + " block(s) with [" + (bid == 0 ? "Air" : Items.name(bid)) + "]");
                     }
                  }
                  break;
               case "locate":
               case "find":
               case "locatebiome":
                  this.locateCommand(var2);
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
               case "disaster":
               case "disasters":
                  this.world.disasters.command(this, var2);
                  break;
               case "treefall":
                  if (var2.length > 1 && (var2[1].equalsIgnoreCase("on") || var2[1].equalsIgnoreCase("off"))) {
                     this.world.trees.enabled = var2[1].equalsIgnoreCase("on");
                  }

                  this.say("Falling trees are " + (this.world.trees.enabled ? "on" : "off") + "  (/treefall on|off)");
                  break;
               case "bobbing":
                  if (var2.length > 1 && (var2[1].equalsIgnoreCase("on") || var2[1].equalsIgnoreCase("off"))) {
                     this.opt.bobbing = var2[1].equalsIgnoreCase("on");
                     this.opt.save();
                  }

                  this.say("The hand bobs when you walk: " + (this.opt.bobbing ? "on" : "off") + "  (/bobbing on|off)");
                  break;
               case "host":
                  int hPort = var2.length > 1 ? Integer.parseInt(var2[1]) : Net319.DEFAULT_PORT;
                  this.say(Net319.host(this, hPort));
                  break;
               case "join":
               case "connect":
                  String target = var2.length > 1 ? var2[1] : "lan";
                  this.say(Net319.join(this, target));
                  break;
               case "disconnect":
               case "leave":
                  Net319.disconnect(this);
                  this.say("Disconnected from multiplayer");
                  break;
               case "mp":
               case "peers":
                  this.say(
                     Net319.isHosting()
                        ? "Hosting LAN world (" + Net319.peerCount() + " peer(s) connected)"
                        : (Net319.isConnected() ? "Connected to multiplayer (" + Net319.peerCount() + " peer(s))" : "Multiplayer inactive (/host [port] or /join [host:port])")
                  );
                  break;
               case "pack":
               case "resourcepack":
                  if (var2.length < 2) {
                     StringBuilder sb = new StringBuilder("Resource Packs (active: " + ResourcePacks.activeId() + "): ");
                     boolean firstP = true;
                     for (ResourcePacks.Pack p : ResourcePacks.list()) {
                        if (!firstP) {
                           sb.append(", ");
                        }
                        sb.append(p.id);
                        firstP = false;
                     }
                     this.say(sb.toString());
                  } else {
                     ResourcePacks.select(var2[1], this);
                     this.say("Activated Resource Pack: " + ResourcePacks.active().title);
                  }
                  break;
               case "mods":
                  ModLoader.onCommand(this, "/mods", var2);
                  break;
               case "sb":
               case "challenges":
               case "sw":
               case "bw":
               case "shop":
               case "upgrades":
                  SkyGen.onModeCommand(this, "/" + var2[0].toLowerCase(), var2);
                  break;
               case "help":
                  this.say("Commands: /gamemode /time set /tp /locate /seed /dim /give /clear /setblock /fill /spawn /spawnpoint /top /kill /butcher /heal /fly /summon /firespread /weather /treefall /disaster /bobbing /pack /mods /sb /sw /bw /shop /host /join /disconnect");
                  break;
               default:
                  if (!SkyGen.onModeCommand(this, "/" + var2[0].toLowerCase(), var2) && !ModLoader.onCommand(this, "/" + var2[0].toLowerCase(), var2)) {
                     this.say("§cUnknown command. Try /help");
                  }
            }
         } catch (Exception var12) {
            this.say("§cInvalid arguments");
         }
      }
   }

   static int resolveBlockArg(String raw) {
      String q = raw.toLowerCase().trim();
      if (q.equals("air") || q.equals("0")) {
         return 0;
      }
      try {
         int id = Integer.parseInt(q);
         if (id > 0 && id < Blocks.MAX && Blocks.name[id] != null && !Items.isItem(id)) {
            return id;
         }
      } catch (NumberFormatException ignored) {
      }
      String norm = q.replace('_', ' ');
      int partial = -1;
      for (int id : Blocks.creativeList()) {
         if (!Items.isItem(id)) {
            String nm = Items.name(id).toLowerCase();
            if (nm.equals(norm)) {
               return id;
            }
            if (partial < 0 && nm.contains(norm)) {
               partial = id;
            }
         }
      }
      return partial;
   }

   void locateCommand(String[] args) {
      if (args.length < 2) {
         this.say("§cUsage: /locate <village|stronghold|fortress|mineshaft|pyramid|temple|witch_hut|igloo|outpost|biome_name>");
         return;
      }
      boolean forceBiome = args[0].equalsIgnoreCase("locatebiome") || (args.length >= 3 && args[1].equalsIgnoreCase("biome"));
      String target = (args.length >= 3 && args[1].equalsIgnoreCase("biome") ? args[2] : args[1]).toLowerCase().trim();
      int px = (int)Math.floor(this.player.x);
      int pz = (int)Math.floor(this.player.z);

      if (!forceBiome) {
         switch (target) {
            case "village":
            case "villages": {
               int r0x = Math.floorDiv(px, 320);
               int r0z = Math.floorDiv(pz, 320);
               Structs.Village best = null;
               double bestD2 = Double.MAX_VALUE;
               for (int r = 0; r <= 18; r++) {
                  for (int dz = -r; dz <= r; dz++) {
                     for (int dx = -r; dx <= r; dx++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                           continue;
                        }
                        Structs.Village v = this.world.gen.structs.village(r0x + dx, r0z + dz);
                        if (v != null && v.valid) {
                           double d2 = (double)(v.cx - px) * (v.cx - px) + (double)(v.cz - pz) * (v.cz - pz);
                           if (d2 < bestD2) {
                              bestD2 = d2;
                              best = v;
                           }
                        }
                     }
                  }
                  if (best != null && r >= 2) {
                     break;
                  }
               }
               if (best != null) {
                  int dist = (int)Math.round(Math.sqrt(bestD2));
                  this.say("Located Village at [" + best.cx + ", " + best.cy + ", " + best.cz + "] (" + dist + " blocks away)");
               } else {
                  this.say("§cCould not find a Village nearby");
               }
               return;
            }
            case "stronghold":
            case "portal": {
               Stronghold sh = Stronghold.nearest(this.world.seed, px, pz);
               if (sh != null) {
                  int dist = (int)Math.round(Math.hypot(sh.cx - px, sh.cz - pz));
                  int sy = sh.plan() != null ? sh.plan().oy : 28;
                  this.say("Located Stronghold at [" + sh.cx + ", " + sy + ", " + sh.cz + "] (" + dist + " blocks away)");
               } else {
                  this.say("§cCould not find a Stronghold");
               }
               return;
            }
            case "fortress":
            case "nether_fortress": {
               NetherFortress nf = new NetherFortress(this.world.seed);
               int r0x = Math.floorDiv(px, NetherFortress.REG);
               int r0z = Math.floorDiv(pz, NetherFortress.REG);
               NetherFortress.Plan best = null;
               double bestD2 = Double.MAX_VALUE;
               for (int r = 0; r <= 12; r++) {
                  for (int dz = -r; dz <= r; dz++) {
                     for (int dx = -r; dx <= r; dx++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                           continue;
                        }
                        NetherFortress.Plan p = nf.plan(r0x + dx, r0z + dz);
                        if (p != null && p.present) {
                           double d2 = (double)(p.ox - px) * (p.ox - px) + (double)(p.oz - pz) * (p.oz - pz);
                           if (d2 < bestD2) {
                              bestD2 = d2;
                              best = p;
                           }
                        }
                     }
                  }
                  if (best != null && r >= 1) {
                     break;
                  }
               }
               if (best != null) {
                  int dist = (int)Math.round(Math.sqrt(bestD2));
                  this.say("Located Nether Fortress at [" + best.ox + ", " + (best.oy + 1) + ", " + best.oz + "] (" + dist + " blocks away)");
               } else {
                  this.say("§cCould not find a Nether Fortress nearby");
               }
               return;
            }
            case "mineshaft":
            case "shaft": {
               int r0x = Math.floorDiv(px, 128);
               int r0z = Math.floorDiv(pz, 128);
               int bestX = 0;
               int bestY = 0;
               int bestZ = 0;
               boolean foundShaft = false;
               double bestD2 = Double.MAX_VALUE;
               for (int r = 0; r <= 16; r++) {
                  for (int dz = -r; dz <= r; dz++) {
                     for (int dx = -r; dx <= r; dx++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                           continue;
                        }
                        Structs.Shaft sh = this.world.gen.structs.shaft(r0x + dx, r0z + dz);
                        if (sh != null && sh.valid && !sh.cs.isEmpty()) {
                           Structs.Corr c0 = (Structs.Corr)sh.cs.get(0);
                           int sx = (c0.x0 + c0.x1) / 2;
                           int sz = (c0.z0 + c0.z1) / 2;
                           double d2 = (double)(sx - px) * (sx - px) + (double)(sz - pz) * (sz - pz);
                           if (d2 < bestD2) {
                              bestD2 = d2;
                              bestX = sx;
                              bestY = c0.y + 1;
                              bestZ = sz;
                              foundShaft = true;
                           }
                        }
                     }
                  }
                  if (foundShaft && r >= 1) {
                     break;
                  }
               }
               if (foundShaft) {
                  int dist = (int)Math.round(Math.sqrt(bestD2));
                  this.say("Located Mineshaft at [" + bestX + ", " + bestY + ", " + bestZ + "] (" + dist + " blocks away)");
               } else {
                  this.say("§cCould not find a Mineshaft nearby");
               }
               return;
            }
            case "pyramid":
            case "desert_pyramid":
            case "desert_temple":
            case "temple":
            case "jungle_temple":
            case "witch_hut":
            case "swamp_hut":
            case "hut":
            case "igloo":
            case "outpost":
            case "watchtower":
            case "tower":
            case "landmark":
            case "structure": {
               int wantKind = 0;
               String label = "Landmark";
               if (target.contains("pyramid") || target.equals("desert_temple")) {
                  wantKind = Landmarks319.K_PYRAMID;
                  label = "Desert Pyramid";
               } else if (target.contains("temple")) {
                  wantKind = Landmarks319.K_JUNGLE;
                  label = "Jungle Temple";
               } else if (target.contains("witch") || target.contains("hut")) {
                  wantKind = Landmarks319.K_WITCH;
                  label = "Witch Hut";
               } else if (target.equals("igloo")) {
                  wantKind = Landmarks319.K_IGLOO;
                  label = "Igloo";
               } else if (target.equals("outpost") || target.equals("watchtower") || target.equals("tower")) {
                  wantKind = Landmarks319.K_OUTPOST;
                  label = "Ruined Watchtower";
               }
               int r0x = Math.floorDiv(px, Landmarks319.REG);
               int r0z = Math.floorDiv(pz, Landmarks319.REG);
               Landmarks319.Site best = null;
               double bestD2 = Double.MAX_VALUE;
               for (int r = 0; r <= 24; r++) {
                  for (int dz = -r; dz <= r; dz++) {
                     for (int dx = -r; dx <= r; dx++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                           continue;
                        }
                        Landmarks319.Site s = Landmarks319.plan(this.world.gen.structs, r0x + dx, r0z + dz);
                        if (s != null && (wantKind == 0 || s.kind == wantKind)) {
                           int cx = s.x0 + s.w / 2;
                           int cz = s.z0 + s.d / 2;
                           double d2 = (double)(cx - px) * (cx - px) + (double)(cz - pz) * (cz - pz);
                           if (d2 < bestD2) {
                              bestD2 = d2;
                              best = s;
                           }
                        }
                     }
                  }
                  if (best != null && r >= 2) {
                     break;
                  }
               }
               if (best != null) {
                  int cx = best.x0 + best.w / 2;
                  int cy = best.y0 + 1;
                  int cz = best.z0 + best.d / 2;
                  int dist = (int)Math.round(Math.sqrt(bestD2));
                  String[] kindNames = {"Landmark", "Desert Pyramid", "Jungle Temple", "Witch Hut", "Igloo", "Ruined Watchtower"};
                  String foundLabel = best.kind >= 1 && best.kind < kindNames.length ? kindNames[best.kind] : label;
                  this.say("Located " + foundLabel + " at [" + cx + ", " + cy + ", " + cz + "] (" + dist + " blocks away)");
               } else {
                  this.say("§cCould not find " + label + " nearby");
               }
               return;
            }
            default:
               break;
         }
      }

      // Biome search
      String normB = target.replace('_', ' ');
      if (normB.equals("mesa")) {
         normB = "badlands";
      } else if (normB.equals("mountains") || normB.equals("hills")) {
         normB = "extreme hills";
      }
      int wantBiome = -1;
      for (int b = 0; b < Biomes.N; b++) {
         String bn = Biomes.NAME[b].toLowerCase();
         if (bn.equals(normB) || (wantBiome < 0 && bn.contains(normB))) {
            wantBiome = b;
            if (bn.equals(normB)) {
               break;
            }
         }
      }
      if (wantBiome < 0) {
         this.say("§cUnknown structure or biome '" + target + "'. Try: village, stronghold, fortress, mineshaft, pyramid, temple, witch_hut, igloo, outpost, or a biome name.");
         return;
      }

      int step = 32;
      for (int r = 0; r <= 150; r++) {
         for (int dz = -r; dz <= r; dz++) {
            for (int dx = -r; dx <= r; dx++) {
               if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                  continue;
               }
               int wx = px + dx * step;
               int wz = pz + dz * step;
               Gen.Col col = this.world.gen.column(wx, wz);
               if (col.biome == wantBiome) {
                  int dist = (int)Math.round(Math.hypot(wx - px, wz - pz));
                  this.say("Located biome " + Biomes.name(wantBiome) + " at [" + wx + ", " + (col.h + 1) + ", " + wz + "] (" + dist + " blocks away)");
                  return;
               }
            }
         }
      }
      this.say("§cCould not find biome " + Biomes.name(wantBiome) + " within 4800 blocks");
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
         Net319.onDropItem(this.world, var14);
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
      float tabs = this.menu.kind == 4 ? Menu.TAB_H + 2.0F : 0.0F; // (the tabs of the creative inventory stick out above the panel)
      return (float)Math.floor((this.gui.h - this.menu.h - tabs) / 2.0F + tabs);
   }

   void menuClick(float var1, float var2, int var3) {
      if (this.menu != null) {
         float var4 = this.menuOx();
         float var5 = this.menuOy();
         int tabHit = this.menu.tabAt(var1, var2, var4, var5);
         if (tabHit >= 0) {
            if (var3 == 0) {
               this.menu.setTab(tabHit);
            }

            return;
         }

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
      if (this.player.spectator) {
         if (this.targetMob != null) {
            this.spectatingMob = this.targetMob;
            this.say("\u00a7b[Spectator] Spectating " + Mobs.NAME[this.targetMob.type] + " (press Shift to exit POV)");
         } else if (this.target != null) {
            this.player.setPos(this.target.x + 0.5, this.target.y + 1.0, this.target.z + 0.5);
         }
         return;
      }
      if (this.target != null) {
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

               if (this.player.creative) {
                  this.inv[this.sel] = new Stack(var1, 64);
                  this.nameTimer = 40.0F;
               } else {
                  // V4.1 Survival & Adventure Pick-Block: swap matching stack from main inventory (9..35) into active hotbar slot
                  for (int i = 9; i < 36; i++) {
                     if (this.inv[i] != null && this.inv[i].id == var1) {
                        Stack tmp = this.inv[this.sel];
                        this.inv[this.sel] = this.inv[i];
                        this.inv[i] = tmp;
                        this.nameTimer = 40.0F;
                        this.equip = 0.0F;
                        return;
                     }
                  }
               }
            }
         }
      }
   }

   void spectatorAction(int action) {
      if (this.world == null || this.player == null) {
         return;
      }
      switch (action) {
         case 1 -> {
            this.spectatingMob = null;
            double[] st = this.findStand(this.spawnXZ[0], this.spawnXZ[1]);
            double sy = st != null ? st[1] : 80.0;
            this.player.setPos(this.spawnXZ[0] + 0.5, sy, this.spawnXZ[1] + 0.5);
            this.say("\u00a7b[Spectator] Teleported to World Spawn (" + this.spawnXZ[0] + ", " + (int)sy + ", " + this.spawnXZ[1] + ")");
         }
         case 2 -> {
            Mob best = null;
            double bestD = Double.MAX_VALUE;
            for (Mob m : this.mobs.list) {
               if (!m.dead && m != this.spectatingMob) {
                  double d = Math.hypot(m.x - this.player.x, m.z - this.player.z);
                  if (d < bestD) {
                     bestD = d;
                     best = m;
                  }
               }
            }
            if (best != null) {
               this.spectatingMob = best;
               this.player.setPos(best.x, best.y, best.z);
               this.say("\u00a7b[Spectator] Spectating " + Mobs.NAME[best.type] + " (HP: " + best.health + ", press Shift to exit POV)");
            } else {
               this.say("\u00a77[Spectator] No mobs nearby to spectate.");
            }
         }
         case 3 -> {
            this.spectatingMob = null;
            if (this.world.settings != null && this.world.settings.isVoidMode()) {
               this.player.setPos(0.5, SkyGen.Y_DECK + 4.0, 0.5);
               this.say("\u00a7b[Spectator] Teleported to Center Island (0, " + (SkyGen.Y_DECK + 4) + ", 0)");
            } else {
               this.locateCommand(new String[]{"locate", "village"});
            }
         }
         case 4 -> {
            this.spectatingMob = null;
            int bx = (int)Math.floor(this.player.x);
            int bz = (int)Math.floor(this.player.z);
            double[] st = this.findStand(bx, bz);
            double sy = st != null ? st[1] : 90.0;
            this.player.setPos(bx + 0.5, sy, bz + 0.5);
            this.say("\u00a7b[Spectator] Teleported to Surface (" + bx + ", " + (int)sy + ", " + bz + ")");
         }
         case 5 -> {
            this.spectatorNightVision = !this.spectatorNightVision;
            this.say("\u00a7b[Spectator] Night Vision: " + (this.spectatorNightVision ? "\u00a7aON" : "\u00a7cOFF"));
         }
         case 6 -> {
            if (this.spectatingMob != null) {
               this.spectatingMob = null;
               this.say("\u00a7b[Spectator] Exited Mob POV.");
            } else if (this.world.settings != null && this.world.settings.type == WorldSettings.BEDWARS) {
               int idx = (int)((this.tickCount / 5) % 4);
               int[] b = SkyGen.BEDWARS_BASES[idx];
               this.player.setPos(b[0] + 0.5, SkyGen.Y_DECK + 3.0, b[1] + 0.5);
               this.say("\u00a7b[Spectator] Teleported to " + SkyGen.BEDWARS_NAMES[idx] + " Base");
            } else if (this.world.settings != null && this.world.settings.type == WorldSettings.SKYWARS) {
               int idx = (int)((this.tickCount / 5) % SkyGen.SKYWARS_SPAWNS.length);
               int[] sp = SkyGen.SKYWARS_SPAWNS[idx];
               this.player.setPos(sp[0] + 0.5, SkyGen.Y_DECK + 3.0, sp[1] + 0.5);
               this.say("\u00a7b[Spectator] Teleported to Skywars Island #" + (idx + 1));
            } else {
               this.player.flySpeed = 0.05F;
               this.say("\u00a7b[Spectator] Reset Fly Speed to 100%.");
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

   static final String[] ARMOR_PLACE = {"Head", "Chest", "Legs", "Feet"};
   static final String[] TOOL_USE = {"", "Mines stone and ores", "Chops wood fast", "Digs dirt, sand and gravel", "Hits harder than a fist"};

   void tooltip(Stack var1, float var2, float var3) {
      this.tooltipLines(this.tooltipText(var1), var2, var3);
   }

   /** The lines of the tooltip of an item: name, kind and what is worth knowing (durability, damage, armor, food, fuel, what it smelts into). */
   ArrayList<String> tooltipText(Stack var1) {
      ArrayList<String> var4 = new ArrayList<>();
      int id = var1.id;
      var4.add(Items.name(id));
      var4.add("§7" + Creative.kind(id));
      int var6 = Items.maxDamage(id);
      if (var6 > 0) {
         int left = var6 - var1.dmg;
         var4.add((left * 4 < var6 ? "§c" : "§7") + "Durability: " + left + " / " + var6);
      }

      if (Items.isArmor(id)) {
         var4.add("§9+" + Items.armorPoints(id) + " Armor  §7(" + ARMOR_PLACE[Items.armorSlot(id)] + ")");
      }

      if (Items.isTool(id)) {
         var4.add("§9" + String.format("%.1f", new Object[]{Items.attackDamage(id)}) + " Attack Damage");
         var4.add("§7" + TOOL_USE[Items.toolClass(id)]);
      } else if (Items.isHoe(id)) {
         var4.add("§7Turns dirt into farmland");
      }

      if (Items.isFood(id)) {
         var4.add("§6Restores " + Items.foodPts[id] / 2.0F + " hunger");
      }

      int fuel = Recipes.fuelTime(new Stack(id, 1));
      if (fuel > 0) {
         int items = fuel / 200;
         var4.add("§eFuel: smelts " + (items >= 1 ? items + (items == 1 ? " item" : " items") : "less than one item"));
      }

      Stack smelt = Recipes.smeltResult(new Stack(id, 1));
      if (smelt != null) {
         var4.add("§aSmelts into " + Items.name(smelt.id));
      }

      int max = Items.maxStack(id);
      if (max != 64) {
         var4.add("§7Stacks to " + max);
      }

      if (this.debug) {
         var4.add("§7id " + id + (var1.dmg != 0 ? ", damage " + var1.dmg : ""));
      }

      return var4;
   }

   /** A box with some lines of text next to the mouse. */
   void tooltipLines(java.util.List<String> var4, float var2, float var3) {
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
      if (var13 + var10 + 3.0F > this.gui.h) {
         var13 = Math.max(3.0F, this.gui.h - var10 - 3.0F); // (near the bottom of the window the box goes up)
      }

      var12 = Math.max(3.0F, var12);
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
         Perf.frame(); // Update 3.18: the frame timer (see Perf)
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

         Perf.mark(Perf.SWAP);
         GLFW.glfwSwapBuffers(this.window);
         this.limitFps(var3);
         this.frames++;
         double var7 = GLFW.glfwGetTime();
         if (var7 - this.fpsTimer >= 1.0) {
            this.fps = this.frames;
            this.frames = 0;
            this.fpsTimer = var7;
         }
      }
   }

   /**
    * Update 3.18: the frame rate limit: the one in the options, 60 in the menus (the turning world behind the title does not need 300 pictures a
    * second), 30 while the window is in the background, 10 while it is minimised. 0 = no limit (VSync alone decides).
    */
   int fpsCap() {
      if (this.window == 0L || System.getProperty("bc.auto") != null) {
         return 0;
      }

      if (GLFW.glfwGetWindowAttrib(this.window, GLFW.GLFW_ICONIFIED) != 0) {
         return 10;
      }

      int cap = this.opt.maxFps;
      boolean menu = this.screen == Game.S.TITLE || this.screen == Game.S.WORLDS || this.screen == Game.S.NEWWORLD || this.screen == Game.S.MULTIPLAYER
         || this.screen == Game.S.OPTIONS || this.screen == Game.S.SKINS || this.screen == Game.S.PAUSE;
      if (menu) {
         cap = cap > 0 ? Math.min(cap, 60) : 60;
      }

      if (GLFW.glfwGetWindowAttrib(this.window, GLFW.GLFW_FOCUSED) == 0) {
         cap = cap > 0 ? Math.min(cap, 30) : 30;
      }

      return cap;
   }

   /** How much longer than asked a sleep took, on average (ns): a coarse timer (15 ms on an old Windows) makes the limiter wait busily instead of oversleeping. */
   private long sleepOvershoot = 1_000_000L;

   /** Waits until the frame that began at {@code frameStart} has lasted as long as the limit says (sleeps while there is time to spare, waits busily for the rest). */
   void limitFps(long frameStart) {
      int cap = this.fpsCap();
      if (cap <= 0) {
         return;
      }

      long end = frameStart + 1_000_000_000L / cap;
      while (true) {
         long left = end - System.nanoTime();
         if (left <= 0L) {
            return;
         }

         long spare = left - this.sleepOvershoot - 500_000L;
         if (spare > 500_000L) {
            long t0 = System.nanoTime();
            java.util.concurrent.locks.LockSupport.parkNanos(spare);
            long over = Math.max(0L, System.nanoTime() - t0 - spare);
            this.sleepOvershoot = Math.min(20_000_000L, (this.sleepOvershoot * 7 + over) / 8);
         } else {
            Thread.onSpinWait();
         }
      }
   }

   void update(double var1) {
      File dropped;
      while (this.skins != null && (dropped = this.skinDrops.poll()) != null) {
         this.importSkin(dropped);
      }

      if (this.skinRefull) {
         this.skinRefull = false;
         this.setFullscreen(true);
      }

      if (this.skins != null && this.skins.imported != null) {
         this.opt.skin = this.skins.imported;
         this.opt.save();
         this.skinSel = -1;
         this.skinMsg = "Imported " + this.skins.imported;
         this.skins.imported = null;
      }

      if (this.pano != null) {
         this.pano.update(var1); // Update 3.13: the turning world behind the title and the menus
      }

      if (this.world == null && Net319.isActive()) {
         Net319.tick(this);
      }

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
            if (Net319.isActive()) {
               Net319.tick(this);
            }

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
            this.loadFrac = Math.min(1.0F, var18 / (float)var16) * 0.92F + (var19 ? 0.08F : 0.0F);
            if (var19 && this.arrivalAt != null) {
               // Update 3.12: the new dimension is there: put the player in the portal (this may build one, which the next frames mesh before play goes on)
               this.finishArrival();
               var19 = false;
            }

            if (var19) {
               if (this.needSpawn) {
                  double[] var21 = this.findStand(this.spawnXZ[0], this.spawnXZ[1]);
                  if (var21 != null) {
                     this.spawnPos = var21;
                     this.spawnXZ = new int[]{(int)Math.floor(var21[0]), (int)Math.floor(var21[2])};
                     this.player.setPos(var21[0], var21[1], var21[2]);
                     this.needSpawn = false;
                     if (this.bonusPending) {
                        this.bonusPending = false;
                        Bonus.place(this.world, (int)Math.floor(var21[0]), (int)Math.floor(var21[1]), (int)Math.floor(var21[2]), this.world.seed);
                     }
                  }
               }

               if (!this.needSpawn) {
                  if (Phys.collides(this.world, this.player.bb)) {
                     this.player.setPos(this.player.x, this.player.y + 1.0, this.player.z);
                  }

                  if (this.world.settings != null
                     && this.world.settings.isMinigame()
                     && SkyGen.matchState == SkyGen.STATE_WAITING
                     && this.window != 0L
                     && System.getProperty("bc.auto") == null) {
                     this.setScreen(Game.S.WAITING);
                  } else {
                     this.setScreen(Game.S.PLAY);
                  }
               }
            }
         } else {
            boolean var6 = this.screen == Game.S.PLAY
               || this.screen == Game.S.WAITING
               || this.screen == Game.S.INV
               || this.screen == Game.S.CHAT
               || this.screen == Game.S.DEAD
               || Net319.isActive();
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

            if (this.pano == null) {
               this.partial = (float)this.acc;
            }

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
      return this.window != 0L && GLFW.glfwGetKey(this.window, var1) == 1;
   }

   /** Update 3.9: the walking / turning state of the body that the third-person view and the skin preview draw. */
   void tickPose() {
      Player pl = this.player;
      if (!this.poseInit) {
         this.bodyYaw = this.pBodyYaw = pl.yaw;
         this.poseInit = true;
      }

      double dx = pl.x - pl.px;
      double dz = pl.z - pl.pz;
      float speed = (float)Math.sqrt(dx * dx + dz * dz);
      this.pWalkPhase = this.walkPhase;
      this.pWalkAmp = this.walkAmp;
      this.pBodyYaw = this.bodyYaw;
      this.pBobDist = this.bobDist;
      this.pBobAmp = this.bobAmp;
      this.bobAmp += ((pl.onGround && !pl.flying && !pl.dead ? Math.min(0.1F, speed) : 0.0F) - this.bobAmp) * 0.4F;
      this.bobDist += speed * 0.6F;
      float target = pl.flying ? Math.min(0.3F, speed * 1.5F) : Math.min(1.0F, speed * 4.0F);
      this.walkAmp += (target - this.walkAmp) * 0.4F;
      this.walkPhase += speed * 2.45F * (pl.sprinting ? 1.2F : 1.0F);
      // Update 3.18: the other ways of moving
      System.arraycopy(this.animK, 0, this.pAnimK, 0, this.animK.length);
      boolean wet = pl.inWater && !pl.onGround && !pl.flying && !pl.dead;
      boolean ladder = !pl.flying && !pl.onGround && !pl.dead && this.world != null && pl.onLadder(this.world);
      this.airTicks = pl.onGround || pl.flying || wet || ladder ? 0 : this.airTicks + 1;
      if (ladder) {
         this.walkPhase += (float)Math.abs(pl.y - pl.py) * 3.2F; // the hands and knees go up and down with the climb
      }

      float[] k = this.animK;
      k[K_SNEAK] += ((pl.sneaking && !pl.flying && !wet ? 1.0F : 0.0F) - k[K_SNEAK]) * 0.35F;
      k[K_SPRINT] += ((pl.sprinting && !pl.flying && !pl.sneaking ? 1.0F : 0.0F) - k[K_SPRINT]) * 0.3F;
      k[K_AIR] += ((this.airTicks > 3 ? 1.0F : 0.0F) - k[K_AIR]) * 0.5F;
      k[K_SWIM] += ((wet && speed > 0.03F ? 1.0F : 0.0F) - k[K_SWIM]) * 0.2F;
      k[K_TREAD] += ((wet ? 1.0F : 0.0F) - k[K_TREAD]) * 0.2F;
      k[K_CLIMB] += ((ladder ? 1.0F : 0.0F) - k[K_CLIMB]) * 0.35F;
      k[K_FLY] += ((pl.flying ? 1.0F : 0.0F) - k[K_FLY]) * 0.15F;
      k[K_LAND] *= 0.8F;
      if (pl.onGround && !this.lastGround && this.lastVy < -0.35) {
         k[K_LAND] = (float)Math.min(1.0, -this.lastVy * 1.5); // a hard landing: the knees give way
      }

      k[K_HURT] = pl.hurtTime > 0 ? Math.min(1.0F, pl.hurtTime / 10.0F) : 0.0F;
      this.lastGround = pl.onGround;
      this.lastVy = pl.vy;
      float diff = Mobs.wrap(pl.yaw - this.bodyYaw);
      if (speed > 0.03F || this.swinging) {
         this.bodyYaw += diff * 0.5F; // walking or hitting: the body turns to where you look
      } else if (Math.abs(diff) > 50.0F) {
         this.bodyYaw = pl.yaw - Math.signum(diff) * 50.0F; // standing: the head turns up to 50 degrees before the body follows
      }
   }

   /** The pose of the player's body for this frame (interpolated between ticks). */
   PlayerModel.Pose pose(float partial) {
      PlayerModel.Pose p = new PlayerModel.Pose();
      Player pl = this.player;
      p.bodyYaw = MobRender.lerpAngle(this.pBodyYaw, this.bodyYaw, partial);
      p.headYaw = Math.max(-70.0F, Math.min(70.0F, Mobs.wrap(pl.yaw - p.bodyYaw)));
      p.headPitch = pl.pitch;
      p.limb = this.pWalkPhase + (this.walkPhase - this.pWalkPhase) * partial;
      p.amp = this.pWalkAmp + (this.walkAmp - this.pWalkAmp) * partial;
      p.swing = this.swinging ? this.swingPrev + (this.swing - this.swingPrev) * partial : -1.0F;
      p.sneak = pl.sneaking && !pl.flying;
      p.sprint = pl.sprinting && !pl.flying && !pl.sneaking;
      p.air = !pl.onGround && !pl.flying && !pl.eyeInWater;
      p.age = this.tickCount + partial;
      Stack held = this.inv[this.sel];
      p.held = held == null ? 0 : held.id;
      // Update 3.18
      p.sneakK = this.animK(K_SNEAK, partial);
      p.sprintK = this.animK(K_SPRINT, partial);
      p.airK = this.animK(K_AIR, partial);
      p.swim = this.animK(K_SWIM, partial);
      p.tread = this.animK(K_TREAD, partial);
      p.climb = this.animK(K_CLIMB, partial);
      p.fly = this.animK(K_FLY, partial);
      p.land = this.animK(K_LAND, partial);
      p.hurt = this.animK(K_HURT, partial);
      p.vy = (float)pl.vy;
      p.eat = this.eatTicks > 0 ? Math.min(1.0F, (this.eatTicks + partial) / 32.0F) : -1.0F;
      p.bow = this.bowCharge > 0 ? this.bowCharge / 20.0F : -1.0F;
      if (POSE_STATE != null) {
         // the state is exactly what the property names: everything else is switched off (a creative player in a test scene is flying)
         p.sneakK = p.sprintK = p.airK = p.swim = p.tread = p.climb = p.fly = p.land = p.hurt = p.vy = 0.0F;
         p.sneak = p.sprint = p.air = false;
         p.eat = p.bow = -1.0F;
         for (String kv : POSE_STATE.split(",")) {
            String[] e = kv.trim().split("=");
            float v = e.length > 1 ? Float.parseFloat(e[1]) : 1.0F;
            switch (e[0]) {
               case "sneak": p.sneakK = v; p.sneak = v > 0.5F; break;
               case "sprint": p.sprintK = v; p.sprint = v > 0.5F; break;
               case "air": p.airK = v; break;
               case "swim": p.swim = v; break;
               case "tread": p.tread = v; break;
               case "climb": p.climb = v; break;
               case "fly": p.fly = v; break;
               case "land": p.land = v; break;
               case "hurt": p.hurt = v; break;
               case "vy": p.vy = v; break;
               case "eat": p.eat = v; break;
               case "bow": p.bow = v; break;
               case "swing": p.swing = v; break;
               case "phase": p.limb = v; break;
               case "amp": p.amp = v; break;
               default: break;
            }
         }
      }

      return p;
   }

   private float animK(int i, float partial) {
      return this.pAnimK[i] + (this.animK[i] - this.pAnimK[i]) * partial;
   }

   /** The 64x64 pixels of the skin the player has chosen (never null). */
   int[] skinPixels() {
      return this.skins == null ? Skins.defaultPixels() : this.skins.pixels(this.opt.skin);
   }

   boolean skinSlim() {
      return this.skins != null && this.skins.slim(this.opt.skin);
   }

   /** A skin file arrived (dropped on the window, from the dialog or the clipboard): import it and wear it. */
   void importSkin(File f) {
      try {
         String name = this.skins.importFile(f);
         this.opt.skin = name;
         this.opt.save();
         this.skinSel = -1;
         this.skinMsg = "Imported " + name;
         if (this.world != null && this.screen != Game.S.SKINS) {
            this.say("Skin imported: " + name + "  (change it under Game menu > Skins)");
         }
      } catch (Exception e) {
         this.skinMsg = "Not a usable skin: " + e.getMessage();
         if (this.world != null && this.screen != Game.S.SKINS) {
            this.say("§cNot a usable skin: " + e.getMessage());
         }
      }
   }

   void tickEquip() {
      Stack cur = this.inv[this.sel];
      int curId = cur == null ? 0 : cur.id;
      this.pEquip = this.equip;
      float target = this.sel == this.shownSel && curId == this.shownId ? 1.0F : 0.0F;
      this.equip += Math.max(-0.4F, Math.min(0.4F, target - this.equip));
      if (this.equip < 0.1F) {
         this.shownSel = this.sel;
         this.shownId = curId;
      }
   }

   void tickArm() {
      Player pl = this.player;
      if (pl == null) {
         return;
      }

      if (Math.abs(pl.yaw - this.armYaw) > 180.0F || Math.abs(pl.pitch - this.armPitch) > 120.0F) {
         this.armYaw = pl.yaw; // teleported / a new world: no swing across half the screen
         this.armPitch = pl.pitch;
      }

      this.pArmPitch = this.armPitch;
      this.pArmYaw = this.armYaw;
      this.armPitch += (pl.pitch - this.armPitch) * 0.5F;
      this.armYaw += (pl.yaw - this.armYaw) * 0.5F;
   }

   void addShake(float amount) {
      if (amount > this.shake) {
         this.shake = Math.min(1.5F, amount);
      }
   }

   void tick() {
      this.tickCount++;
      this.pshake = this.shake;
      this.shake = this.shake < 0.004F ? 0.0F : this.shake * 0.9F;
      this.tickEquip();
      this.tickArm();
      this.world.tick();
      this.world.fire.particles(this);
      this.Tex_tick();
      if (this.screen != Game.S.PAUSE && this.screen != Game.S.WAITING) {
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

      if (!this.player.dead && this.screen != Game.S.WAITING) {
         this.player.tick(this.world, var2, var3, var4, var5, var6);
         this.tickPose();
      }

      Redstone319.tick(this);
      Net319.tick(this);
      SkyGen.tickMode(this, this.world);
      ModLoader.onGameTick(this);
      this.adv.tick();
      if (this.tickCount % 10 == 0) {
         this.adv.poll(this);
      }

      if (this.player.dead && this.screen != Game.S.DEAD) {
         if (!this.deathHandled) {
            this.deathHandled = true;
            SkyGen.myDeaths++;
            if (!this.player.creative) {
               for (int var7 = 0; var7 < 36; var7++) {
                  if (this.inv[var7] != null) {
                     this.world.dropItem(this.player.x, this.player.y + 0.5, this.player.z, this.inv[var7].id, this.inv[var7].n, this.inv[var7].dmg);
                     this.inv[var7] = null;
                  }
               }

               boolean keepBwArmor = this.world != null && this.world.settings != null && this.world.settings.type == WorldSettings.BEDWARS;
               if (!keepBwArmor) {
                  for (int var12 = 0; var12 < 4; var12++) {
                     if (this.armor[var12] != null) {
                        this.world.dropItem(this.player.x, this.player.y + 0.5, this.player.z, this.armor[var12].id, this.armor[var12].n, this.armor[var12].dmg);
                        this.armor[var12] = null;
                     }
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
         if (var8.life0 == 0) {
            var8.life0 = var8.life;
         }

         var8.life--;
         if (var8.life <= 0) {
            var13.remove();
         } else {
            var8.vy -= var8.grav;
            if (var8.sway != 0.0F) {
               float age = var8.life0 - var8.life;
               var8.vx += (float)Math.cos(var8.phase + age * 0.22F) * var8.sway;
               var8.vz += (float)Math.sin(var8.phase * 1.7F + age * 0.17F) * var8.sway;
            }

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

      Fx316.tick(this); // Update 3.16: falling leaves, fireflies, embers, torch flames, dust, splashes, bubbles
      if (!var1) {
         this.leftDown = false;
      }

      Portals.tickPlayer(this);
      if (this.toastTicks > 0) {
         this.toastTicks--;
      }

      if (this.world.fight != null && this.screen != Game.S.PAUSE) {
         this.world.fight.tick(this);
      }

      if (this.player.spectator && this.spectatingMob != null) {
         if (this.spectatingMob.dead || this.player.sneaking) {
            this.spectatingMob = null;
         } else {
            this.player.setPos(this.spectatingMob.x, this.spectatingMob.y, this.spectatingMob.z);
            this.player.yaw = this.spectatingMob.yaw;
         }
      }

      this.voidCheck();
   }

   /** Update 3.12: the End has no floor under the island - whoever falls out of the world is gone. */
   void voidCheck() {
      if (this.world.dim == Dim.END && this.player.y < -12.0 && !this.player.dead && !this.player.spectator) {
         this.player.health = 0;
         this.player.dead = true;
      }
   }

   void Tex_tick() {
   }

   void updateTarget() {
      double var1 = Math.toRadians(this.player.yaw);
      double var3 = Math.toRadians(this.player.pitch);
      double var5 = -Math.sin(var1) * Math.cos(var3);
      double var7 = -Math.sin(var3);
      double var9 = Math.cos(var1) * Math.cos(var3);
      double var11 = this.player.spectator ? 8.0 : (this.player.creative ? 5.0 : 4.5);
      this.target = this.world.raycast(this.player.x, this.player.y + this.player.eye, this.player.z, var5, var7, var9, var11);
      this.targetMob = null;
      double[] var13 = new double[1];
      double var14 = this.player.x;
      double var16 = this.player.y + this.player.eye;
      double var18 = this.player.z;
      Mob var20 = this.mobs.pick(var14, var16, var18, var5, var7, var9, this.player.spectator ? 8.0 : (this.player.creative ? 5.0 : 3.0), var13);
      if (var20 != null && (this.target == null || var13[0] < this.target.t)) {
         this.targetMob = var20;
         this.targetMobT = var13[0];
         this.target = null;
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
         if (var1 && this.targetMob != null) {
            this.spectatingMob = this.targetMob;
            this.say("\u00a7b[Spectator] Spectating " + Mobs.NAME[this.targetMob.type] + " (press Shift to exit POV)");
         } else if (var2) {
            if (this.spectatingMob != null) {
               this.spectatingMob = null;
               this.say("\u00a7b[Spectator] Returned to free-flight camera.");
            } else if (this.target != null) {
               int b = this.world.getBlock(this.target.x, this.target.y, this.target.z);
               if (b == 69 || b == 70 || b == 67 || b == 68) {
                  TileEntity te = this.world.getOrCreateTile(this.target.x, this.target.y, this.target.z);
                  if (te != null) {
                     this.openMenu((b == 67 || b == 68) ? 2 : 3, te);
                  }
               }
            }
         }
      } else {
         Stack var3 = this.held();
         int var4 = this.heldId();
         if (this.leftDown && !this.swinging) {
            this.swinging = true;
            this.swing = 1.0E-4F;
         }

         boolean var5 = false;
         if (var1 && Net319.tryAttackPeer(this)) {
            var5 = true;
            this.breakProg = 0.0F;
         } else if (this.targetMob != null) {
            if (var1) {
               this.attackMob(this.targetMob);
            }

            var5 = true;
            this.breakProg = 0.0F;
         }

         if (!var5 && this.leftDown && this.target != null) {
            int var6 = this.world.getBlock(this.target.x, this.target.y, this.target.z);
            if (!Blocks.breakable(var6) || (this.player.adventure && !Items.canAdventureBreak(var4, var6))) {
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
         if (!var19 && var2 && var3 == null && this.player.sneaking && Mobs312.shoulderOff(this)) {
            var19 = true; // Update 3.12: sneak + right click with an empty hand sends the parrot on your shoulder away
         }

         if (var19) {
            this.swinging = true;
            this.swing = 1.0E-4F;
         }

         if (!var19 && var2 && this.target != null && (!this.player.sneaking || var3 == null)) {
            var19 = this.useBlock();
            if (var19) {
               this.swinging = true;
               this.swing = 1.0E-4F;
            }
         }

         if (var19) {
            this.rightDown = false;
         } else if (var2 && var3 != null && this.useSpecial(var3)) {
            this.rightDown = false;
            this.swinging = true;
            this.swing = 1.0E-4F;
         } else if (this.rightDown
            && var3 != null
            && Items.isFood(var3.id)
            && !this.player.creative
            && (ModLoader.isClassicHudNoHunger() ? this.player.health < 20 : this.player.food < 20)) {
            if (ModLoader.isClassicHudNoHunger()) {
               // Classic Minecraft Infdev: food heals hearts immediately on right-click with no eating delay
               this.eatTicks = 0;
               this.rightDown = false;
               this.player.health = Math.min(20, this.player.health + Items.foodPts[var3.id]);
               var3.n--;
               if (var3.n <= 0) {
                  this.inv[this.sel] = null;
               }
            } else {
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
            }
         } else {
            this.eatTicks = 0;
            if (var4 != 268) {
               this.bowCharge = 0;
               if (this.rightDown
                  && this.placeDelay <= 0
                  && this.target != null
                  && (!this.player.adventure || Items.canAdventurePlace(var4, this.world.getBlock(this.target.x, this.target.y, this.target.z)))
                  && this.placeBlock()) {
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
      if (BackroomsStalkerMod.isActive() && BackroomsStalkerMod.onUseBlock(this, this.target.x, this.target.y, this.target.z, var1)) {
         return true;
      }
      if (Shapes.use(this, this.target.x, this.target.y, this.target.z, var1)) {
         return true;
      }

      if (Blocks.isDoor(var1)) {
         this.toggleDoor(this.target.x, this.target.y, this.target.z);
         return true;
      }

      if (Blocks.isBed(var1)) {
         if (this.world.settings != null && this.world.settings.type == WorldSettings.BEDWARS) {
            return false;
         }
         this.sleepAt(this.target.x, this.target.y, this.target.z);
         return true;
      }

      if (Blocks.isCake(var1)) {
         return this.eatCake();
      }

      if (var1 == Blocks.PUMPKIN && this.heldId() == Items.SHEARS && !this.player.adventure) {
         return this.carvePumpkin();
      }

      if ((var1 == Blocks.END_FRAME || var1 == Blocks.END_FRAME_EYE) && this.heldId() == Items.EYE_OF_ENDER) {
         return EndPortals.insertEye(this, this.target.x, this.target.y, this.target.z);
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
      if (BackroomsStalkerMod.isActive() && var4 == Blocks313.ICE && this.world.getBlock(var1, var2 - 1, var3) == Blocks.QUARTZ_BLOCK) {
         this.world.dropItem(var1 + 0.5, var2 + 0.3, var3 + 0.5, BackroomsStalkerMod.ALMOND_WATER, 2);
         return;
      }
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
               case 23:
                  var6 = Redstone319.ITEM_REDSTONE; // Update 3.19: redstone ore gives 4 - 5 redstone dust
                  var7 = 4 + this.rnd.nextInt(2);
                  break;
               case 24:
                  var6 = Color312.dyeItem(11); // Update 3.12: lapis lazuli ore gives 4 - 8 blue dye
                  var7 = 4 + this.rnd.nextInt(5);
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
      if (this.world != null && this.world.settings != null && this.world.settings.type == WorldSettings.BEDWARS) {
         if (var1.teamTag != null && var1.teamTag.equals("Team:" + SkyGen.BEDWARS_NAMES[SkyGen.getPlayerTeam(this)])) {
            this.say("§c[Bedwars] That is your own team's Defender!");
            return;
         }
      }
      int var2 = this.heldId();
      boolean var3 = Items.isTool(var2);
      float var4 = var3 ? Items.attackDamage(var2) : 1.0F;
      if (this.world != null && this.world.settings != null && this.world.settings.type == WorldSettings.BEDWARS && SkyGen.hasTeamSharpness(SkyGen.getPlayerTeam(this))) {
         var4 += 2.0F;
      }
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
      p.u = (Tex.tx(64) + 1) / (float)Tex.PX;
      p.v = (Tex.ty(64) + 1) / (float)Tex.PX;
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
      p.u = (Tex.tx(64) + 1) / (float)Tex.PX;
      p.v = (Tex.ty(64) + 1) / (float)Tex.PX;
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
         p.fade = 3; // Update 3.16: smoke thins out and shrinks
         p.alpha = 0.85F;
         p.sway = 0.0012F;
         p.phase = this.rnd.nextFloat() * 6.28F;
      } else {
         p.vy = 0.04F + this.rnd.nextFloat() * 0.03F;
         p.size = 0.07F + this.rnd.nextFloat() * 0.05F;
         p.life = 10 + this.rnd.nextInt(8);
         p.r = 1.0F;
         p.g = 0.45F + this.rnd.nextFloat() * 0.4F;
         p.b = 0.08F;
         p.glow = true; // Update 3.16: flames glow (additive) and die down
         p.fade = 3;
      }

      this.particles.add(p);
   }

   void explosionParticles(double var1, double var3, double var5) {
      int var7 = Blocks.tiles[4][2];
      int var8 = Tex.tx(var7);
      int var9 = Tex.ty(var7);

      for (int var10 = 0; var10 < 60; var10++) {
         Game.Particle var11 = new Game.Particle();
         var11.x = (float)var1 + (this.rnd.nextFloat() - 0.5F) * 2.0F;
         var11.y = (float)var3 + (this.rnd.nextFloat() - 0.5F) * 2.0F;
         var11.z = (float)var5 + (this.rnd.nextFloat() - 0.5F) * 2.0F;
         var11.vx = (this.rnd.nextFloat() - 0.5F) * 0.4F;
         var11.vy = this.rnd.nextFloat() * 0.3F;
         var11.vz = (this.rnd.nextFloat() - 0.5F) * 0.4F;
         var11.size = 0.15F + this.rnd.nextFloat() * 0.12F;
         var11.u = (var8 + this.rnd.nextInt(12)) / (float)Tex.PX;
         var11.v = (var9 + this.rnd.nextInt(12)) / (float)Tex.PX;
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
         boolean isBedwars = this.world.settings != null && this.world.settings.type == WorldSettings.BEDWARS;
         int bedTeam = isBedwars && Blocks.isBed(var4) ? SkyGen.getBedTeamAt(var1, var2, var3) : -1;
         if (isBedwars && bedTeam >= 0 && bedTeam == SkyGen.getPlayerTeam(this) && !this.player.creative) {
            this.say("\u00a7c[Bedwars] You cannot break your own team's bed!");
            return;
         }

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
            int var8 = Decor315.bedFacing(var4);
            int var9 = var8 == 2 ? 1 : (var8 == 3 ? -1 : 0);
            int var10 = var8 == 0 ? -1 : (var8 == 1 ? 1 : 0);
            int var11 = var7 ? var1 - var9 : var1 + var9;
            int var12 = var7 ? var3 - var10 : var3 + var10;
            this.world.setBlock(var11, var2, var12, 0);
            this.meshNow(var11, var2, var12);
            if (var7 && !this.player.creative && !isBedwars) {
               this.world.dropItem(var1 + 0.5, var2 + 0.3, var3 + 0.5, Decor315.bedItem(Decor315.bedColor(var4)), 1);
            }
         }

         this.world.setBlock(var1, var2, var3, 0);
         if (isBedwars && bedTeam >= 0) {
            SkyGen.onBedDestroyed(this, this.world, bedTeam, Net319.selfName, true);
         }
         if (var4 == Blocks313.ICE && !this.player.creative && this.world.getBlock(var1, var2 - 1, var3) != 0) {
            this.world.setBlock(var1, var2, var3, Blocks.WATER); // Update 3.13: broken ice melts to water
         }
         ModLoader.onBlockBroken(this.world, var1, var2, var3, var4);

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
            if (!isBedwars || !Blocks.isBed(var4)) {
               this.blockDrops(var1, var2, var3, var4, var5);
            }
            this.player.addExhaustion(0.025F);
            if (Blocks.hardness[var4] > 0.0F) {
               this.damageHeld(Items.isTool(var5) && Items.toolClass(var5) == 4 ? 2 : 1);
            }
         }

         this.meshNow(var1, var2, var3);
         if (Redstone319.handlesUpdate(var4)) {
            Redstone319.propagate(this.world, var1, var2, var3);
            this.meshNow(var1 + 1, var2, var3);
            this.meshNow(var1 - 1, var2, var3);
            this.meshNow(var1, var2, var3 + 1);
            this.meshNow(var1, var2, var3 - 1);
         }
         if (Blocks.isLog(var4)) {
            // Update 3.10: cutting through a trunk makes the tree above fall over
            this.world.trees.fell(var1, var2, var3, this.player.x, this.player.z);
         }
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
         this.adv.award(this, "husbandry/plant_seed");
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
   /** Update 3.12: right click on a villager opens its trades. */
   boolean openTrade(Mob v) {
      int prof = v.color;
      if (this.world != null && this.world.settings != null && this.world.settings.type == WorldSettings.BEDWARS) {
         prof = v.color == Trades.PRIEST ? Trades.BEDWARS_ARMOR : Trades.BEDWARS_SHOP;
      }
      this.menu = new Menu(this, Menu.TRADE, null, prof);
      this.setScreen(Game.S.INV);
      return true;
   }

   boolean useOnMob(Mob var1, Stack var2) {
      if (var1.type == Mobs.PARROT && !var1.dead && !this.player.spectator && Mobs312.useParrot(this, var1, var2)) {
         return true;
      }

      if (var1.type == Mobs.VILLAGER && !var1.dead && !this.player.spectator) {
         return this.openTrade(var1);
      }

      if (var2 == null || var1.dead || this.player.spectator) {
         return false;
      }

      if (var2.id == Items.SHEARS && this.mobs.shear(var1)) {
         if (!this.player.creative) {
            this.damageHeld(1);
         }

         return true;
      }

      int dyeColor = Color312.dyeColor(var2.id);
      if (dyeColor >= 0 && var1.type == Mobs.SHEEP && var1.color != dyeColor) {
         var1.color = dyeColor; // Update 3.12: dye a sheep (its next wool has the colour)
         this.adv.award(this, "husbandry/dye_sheep");
         if (!this.player.creative) {
            var2.n--;
            if (var2.n <= 0) {
               this.inv[this.sel] = null;
            }
         }

         return true;
      }

      if (var2.id == Items.FLINT_STEEL && this.mobs.ignite(var1)) {
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
         var3.u = (Tex.tx(64) + 1) / (float)Tex.PX;
         var3.v = (Tex.ty(64) + 1) / (float)Tex.PX;
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
      if (!Blocks.isCake(id) || this.player.food >= 20) {
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
            int var12 = Decor315.bedItemColor(var1.id);
            this.world.setBlock(var2, var3, var4, Decor315.foot(var12, var7));
            this.world.setBlock(var10, var3, var11, Decor315.head(var12, var7));
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
      if (this.world.settings != null && this.world.settings.type == WorldSettings.BEDWARS) {
         return;
      }
      if (this.world.dim != Dim.OVERWORLD) {
         // Update 3.12: a bed in the Nether or the End blows up (as in Minecraft)
         this.world.setBlock(var1, var2, var3, 0);
         this.mobs.blast(var1 + 0.5, var2 + 0.5, var3 + 0.5, 5, null);
         return;
      }

      int var4 = this.world.getBlock(var1, var2, var3);
      int var5 = Decor315.bedFacing(var4);
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
            if (!var14.dead && Mobs.hostile(var14.type) && Math.abs(var14.x - var1) < 8.0 && Math.abs(var14.z - var3) < 8.0 && Math.abs(var14.y - var2) < 5.0) {
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
         this.adv.award(this, "adventure/sleep_in_bed");
      }
   }

   boolean useSpecial(Stack var1) {
      if (this.player.spectator) {
         return false;
      }
      int var2 = var1.id;
      if (var2 == BackroomsStalkerMod.ALMOND_WATER && BackroomsStalkerMod.drinkAlmondWater(this, var1)) {
         return true;
      }
      if (Items.isArmor(var2)) {
         int slot = Items.armorSlot(var2);
         Stack prev = this.armor[slot];
         this.armor[slot] = new Stack(var1.id, 1, var1.dmg);
         if (var1.n > 1) {
            var1.n--;
            if (prev != null && !this.addItem(prev)) {
               this.dropFromPlayer(prev.id, prev.n, prev.dmg);
            }
         } else {
            this.inv[this.sel] = prev;
         }
         this.equip = 0.0F;
         return true;
      }
      double var3 = Math.toRadians(this.player.yaw);
      double var5 = Math.toRadians(this.player.pitch);
      double var7 = -Math.sin(var3) * Math.cos(var5);
      double var9 = -Math.sin(var5);
      double var11 = Math.cos(var3) * Math.cos(var5);
      double var13 = this.player.creative ? 5.0 : 4.5;
      if (var2 == 336 || var2 == 337) {
         return this.target != null && this.placeSeeds(var1);
      }

      if (var2 == 339) {
         return this.useBoneMeal(var1);
      }

      if (var2 == Items.ENDER_PEARL) {
         return !this.player.adventure && Mobs312.throwPearl(this, var1);
      }

      if (var2 == Items.BLAZE_ROD && (this.player.creative || (this.world != null && this.world.settings != null && this.world.settings.isMinigame()))) {
         return Mobs312.throwFireball(this, var1);
      }

      if (var2 == Items.EYE_OF_ENDER) {
         boolean thrown = EndPortals.throwEye(this, var1);
         if (thrown) {
            this.adv.award(this, "story/follow_ender_eye");
         }

         return thrown;
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

   /** Update 3.13: a lily pad goes on the surface of a still water block, whichever face of it you aim at. */
   boolean placeLily(Stack s) {
      double yaw = Math.toRadians(this.player.yaw);
      double pitch = Math.toRadians(this.player.pitch);
      this.world.rayFluids = true;
      Hit h = this.world.raycast(this.player.x, this.player.y + this.player.eye, this.player.z, -Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch),
         this.player.creative ? 5.0 : 4.5);
      this.world.rayFluids = false;
      if (h == null) {
         return false;
      }

      int below = this.world.getBlock(h.x, h.y, h.z);
      if (!Blocks.isWater(below) || Blocks.fluidLevel(below) != 0 || h.y + 1 > 255) {
         return false;
      }

      int at = this.world.getBlock(h.x, h.y + 1, h.z);
      if (at != 0 && !Blocks.replaceable[at]) {
         return false;
      }

      if (!this.world.setBlock(h.x, h.y + 1, h.z, Blocks313.LILY_PAD)) {
         return false;
      }

      this.meshNow(h.x, h.y + 1, h.z);
      if (!this.player.creative) {
         s.n--;
         if (s.n <= 0) {
            this.inv[this.sel] = null;
         }
      }

      return true;
   }

   /** Update 3.13: a vine clings to the side of a block: the cell next to the face you aim at gets a vine on its wall side. */
   boolean placeVine(Stack s) {
      if (this.target == null || this.target.ny != 0) {
         return false;
      }

      int x = this.target.x + this.target.nx;
      int y = this.target.y;
      int z = this.target.z + this.target.nz;
      int wall = this.world.getBlock(this.target.x, this.target.y, this.target.z);
      if (!Blocks.opaque[wall] || !Blocks.solid[wall]) {
         return false;
      }

      int bit = this.target.nz < 0 ? 4 : (this.target.nz > 0 ? 1 : (this.target.nx > 0 ? 8 : 2));
      int at = this.world.getBlock(x, y, z);
      int id;
      if (Blocks313.isVine(at)) {
         id = at | bit;
         id = Blocks313.VINE0 + (id - Blocks313.VINE0 | bit);
      } else if (at == 0 || Blocks.replaceable[at]) {
         id = Blocks313.VINE0 + bit;
      } else {
         return false;
      }

      if (!this.world.setBlock(x, y, z, id)) {
         return false;
      }

      this.meshNow(x, y, z);
      if (!this.player.creative) {
         s.n--;
         if (s.n <= 0) {
            this.inv[this.sel] = null;
         }
      }

      return true;
   }

   boolean placeBlock() {
      Stack var1 = this.inv[this.sel];
      if (var1 != null && var1.id == Blocks313.LILY_PAD) {
         return this.placeLily(var1);
      }

      if (var1 != null && Blocks313.isVine(var1.id)) {
         return this.placeVine(var1);
      }

      if (var1 != null && var1.id == 281) {
         return this.placeDoor(var1);
      }

      if (var1 != null && Decor315.isBedItem(var1.id)) {
         return this.placeBed(var1);
      }

      if (var1 != null && Decor315.isBannerItem(var1.id)) {
         return Decor315.placeBanner(this, var1);
      }

      if (var1 != null && Blocks.isBlockId(var1.id) && Shapes.placesSpecially(var1.id)) {
         return Shapes.place(this, var1);
      }

      if (var1 != null && var1.id == Items.CAKE) {
         return this.placeCake(var1);
      }

      if (var1 != null && Items.plantBlock(var1.id) != 0) {
         return this.placeSeeds(var1);
      }

      if (var1 != null && var1.id == Redstone319.ITEM_REDSTONE) {
         return Redstone319.placeWire(this, var1);
      }

      if (var1 != null && Blocks.isBlockId(var1.id)) {
         int var2 = var1.id;
         int var3 = this.target.x;
         int var4 = this.target.y;
         int var5 = this.target.z;
         int var6 = this.world.getBlock(var3, var4, var5);
         int slabMat = Blocks.isSlab(var2) ? Blocks.stairSlabMaterial(var2) : -1;
         // Update 3.10: a slab on the matching face of a slab of the same kind (the top of a bottom slab, the underside of a top slab) = the full block
         boolean mergeClicked = slabMat >= 0 && Blocks.isSlab(var6) && Blocks.stairSlabMaterial(var6) == slabMat
            && (Blocks.isTopSlab(var6) ? this.target.ny == -1 : this.target.ny == 1);
         if (mergeClicked) {
            if (!this.mergeSlabs(var3, var4, var5, slabMat)) {
               return false; // the player is standing where the full block would be
            }

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

            // Update 3.10: slabs and stairs go into the upper half of the cell when you click the underside of a block or the upper half of a side face
            boolean upper = !var7 && (this.target.ny == -1 || this.target.ny == 0 && this.target.fy > 0.5);
            if (var4 >= 0 && var4 <= 255) {
               int var11 = this.world.getBlock(var3, var4, var5);
               if (slabMat >= 0 && Blocks.isSlab(var11) && Blocks.stairSlabMaterial(var11) == slabMat) {
                  // the cell next to the clicked face already has such a slab (in either half): both together are the full block
                  if (!this.mergeSlabs(var3, var4, var5, slabMat)) {
                     return false;
                  }

                  if (!this.player.creative) {
                     var1.n--;
                     if (var1.n <= 0) {
                        this.inv[this.sel] = null;
                     }
                  }

                  return true;
               }

               if (!Blocks.replaceable[var11]) {
                  return false;
               }

               if (slabMat >= 0 && upper) {
                  var2 = Blocks.SLAB_TOP0 + slabMat;
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
                  if (upper) {
                     var2 = Blocks.STAIR_TOP0 + (var2 - 93);
                  }
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

               if (Blocks.isLog(var2) && var9 == 0 && (var8 != 0 || var10 != 0)) {
                  // Update 3.10: a log put against the side of a block lies down along that axis (like Minecraft's); the felled trees are made of them
                  var2 = Blocks.sideLog(var2, var8 != 0 ? 0 : 1);
               }

               boolean tallPlant = Blocks313.isDouble(var2) && !Blocks313.isDoubleUpper(var2);
               if (tallPlant) {
                  int above = var4 + 1 > 255 ? 1 : this.world.getBlock(var3, var4 + 1, var5);
                  if (above != 0 && !Blocks.replaceable[above]) {
                     return false;
                  }
               }

               if (!this.world.setBlock(var3, var4, var5, var2)) {
                  return false;
               }
               ModLoader.onBlockPlaced(this.world, var3, var4, var5, var2);

               if (var2 == Blocks.TNT && this.world.settings != null && this.world.settings.isMinigame()) {
                  this.world.setBlock(var3, var4, var5, 0);
                  this.world.primeTnt(var3, var4, var5, 55);
               }

               if (tallPlant) {
                  this.world.setBlock(var3, var4 + 1, var5, var2 + 1);
                  this.meshNow(var3, var4 + 1, var5);
               }

               if (Redstone319.handlesUpdate(var2) || var2 == Redstone319.DAYLIGHT_SENSOR) {
                  Redstone319.propagate(this.world, var3, var4, var5);
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

   /** Two single slabs of one material in one cell make the full block - unless the player is standing where it would be. */
   boolean mergeSlabs(int x, int y, int z, int mat) {
      double[] bb = this.player.bb;
      if (bb[3] > x && bb[0] < x + 1 && bb[4] > y && bb[1] < y + 1 && bb[5] > z && bb[2] < z + 1) {
         return false;
      }

      if (!this.world.setBlock(x, y, z, Blocks.STAIR_MAT[mat])) {
         return false;
      }

      this.meshNow(x, y, z);
      return true;
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
                  int prevCount = var2.count;
                  this.addItem(var9);
                  if (var9.n <= 0) {
                     var2.dead = true;
                     Net319.onPickupItem(var2);
                  } else if (var9.n < prevCount) {
                     var2.count = var9.n;
                     Net319.onPickupItem(var2);
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
      if (Blocks.kind[var4] == 1 || Blocks.kind[var4] == 7 || Blocks.isLadder(var4) || Blocks313.flatItem(var4)) {
         var6 = Blocks.tiles[var4][0];
      }

      int var7 = Tex.tx(var6);
      int var8 = Tex.ty(var6);
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
         var11.u = (var7 + this.rnd.nextInt(12)) / (float)Tex.PX;
         var11.v = (var8 + this.rnd.nextInt(12)) / (float)Tex.PX;
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

   /** The biggest GUI scale that still leaves 320x240 units for the menus (they are made for that much). */
   int guiScaleMax() {
      int var1 = Math.min(this.fbW / 320, this.fbH / 240);
      return Math.max(1, Math.min(6, var1));
   }

   int guiScaleAuto() {
      if (this.opt.guiScale > 0) {
         // (the test hook bc.guiscale may force a scale that is too big, to see how the screens cope)
         return System.getProperty("bc.guiscale") != null ? this.opt.guiScale : Math.min(this.opt.guiScale, this.guiScaleMax());
      }

      return this.guiScaleMax();
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
         this.rend.computeSky(this.pano != null ? this.pano.skyTime() : this.world.dayTime, this.partial);
         GL13.glActiveTexture(33984);
         Tex.animate(this.tickCount, this.partial);
         boolean var3 = this.rend.shadersOn();
         // Update 3.18: the render scale - with the shaders the world is drawn into the scene buffer, which can be smaller than the window
         int sceneW = this.fbW;
         int sceneH = this.fbH;
         if (var3 && this.opt.renderScale < 100) {
            sceneW = Math.max(64, Math.round(this.fbW * this.opt.renderScale / 100.0F));
            sceneH = Math.max(36, Math.round(this.fbH * this.opt.renderScale / 100.0F));
         }

         if (var3) {
            this.rend.sh.beginScene(sceneW, sceneH);
         }

         this.rend.renderWorld(this.partial, sceneW, sceneH);
         if (var3) {
            this.rend.sh.skyMask(sceneW, sceneH);
            this.rend.sh.aoPass(sceneW, sceneH);
            float uwTarget = this.player.eyeInWater && !this.player.spectator ? 1.0F : 0.0F;
            this.rend.sh.underwater += (uwTarget - this.rend.sh.underwater) * 0.12F;
            if (Math.abs(uwTarget - this.rend.sh.underwater) < 0.004F) {
               this.rend.sh.underwater = uwTarget;
            }

            Perf.mark(Perf.POST);
            this.rend.sh.look = this.opt.look;
            this.rend.sh.endScene(sceneW, sceneH, this.fbW, this.fbH, 0.45F);
         }

         Perf.mark(Perf.HAND);
         if (this.screenIsPlay() && !this.hideGui && this.screen != Game.S.DEAD && !this.player.spectator && this.view == 0) {
            float var4 = this.swingPrev + (this.swing - this.swingPrev) * this.partial;
            float var5 = this.rend.brightnessAt(this.world, this.player.x, this.player.y + 1.0, this.player.z);
            this.rend
               .renderHand(
                  this.partial,
                  this.shownId > 0 ? new Stack(this.shownId, 1) : null,
                  HAND_SWING != null ? Float.parseFloat(HAND_SWING) : (this.swinging ? var4 : 0.0F),
                  HAND_EAT != null ? Float.parseFloat(HAND_EAT) : (this.eatTicks > 0 ? (this.eatTicks + this.partial) / 32.0F : -1.0F),
                  var5,
                  this.fbW,
                  this.fbH,
                  this.pEquip + (this.equip - this.pEquip) * this.partial,
                  (this.player.pitch - (this.pArmPitch + (this.armPitch - this.pArmPitch) * this.partial)) * 0.1F,
                  (this.player.yaw - (this.pArmYaw + (this.armYaw - this.pArmYaw) * this.partial)) * 0.1F,
                  this.opt.bobbing ? -(this.pBobDist + (this.bobDist - this.pBobDist) * this.partial) : 0.0F,
                  this.opt.bobbing ? this.pBobAmp + (this.bobAmp - this.pBobAmp) * this.partial : 0.0F
               );
         }
      } else {
         GL11.glClearColor(0.0F, 0.0F, 0.0F, 1.0F);
         GL11.glClear(16640);
      }

      Perf.mark(Perf.GUI);
      this.gui.begin(this.fbW, this.fbH, var1);
      switch (this.screen) {
         case TITLE:
            Screens.title(this);
            break;
         case WORLDS:
            Screens.worlds(this);
            break;
         case MULTIPLAYER:
            Screens.multiplayer(this);
            break;
         case NEWWORLD:
            Screens.newWorld(this);
            break;
         case LOADING:
            Screens.loading(this);
            break;
         case WAITING:
            Screens.waiting(this);
            break;
         case CHAT:
         case INV:
         case PAUSE:
         default:
            this.drawHud();
            if (this.screen == Game.S.PAUSE) {
               Screens.pause(this);
            } else if (this.screen == Game.S.INV) {
               this.drawInventory();
            } else if (this.screen == Game.S.ADV) {
               this.adv.drawScreen(this);
            } else if (this.screen == Game.S.DEAD) {
               this.drawDead();
            }
            break;
         case SKINS:
            this.ui.backdrop(0.5F);
            this.drawSkins();
            break;
         case PACKS:
            Screens.resourcePacks(this);
            break;
         case MODS:
            Screens.mods(this);
            break;
         case OPTIONS:
            Screens.options(this);
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
      var8.label = var1;
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

   void hitbox(float var1, float var2, float var3, float var4, Runnable var5) {
      Game.Btn var6 = new Game.Btn();
      var6.x = var1;
      var6.y = var2;
      var6.w = var3;
      var6.h = var4;
      var6.action = var5;
      this.btns.add(var6);
   }

   void drawDead() {
      this.gui.gradient(0.0F, 0.0F, this.gui.w, this.gui.h, 1615855616, -1600126976);
      float var1 = this.gui.w / 2.0F;
      boolean hc = this.player != null && this.player.hardcore;
      boolean bwEliminated = this.world != null
         && this.world.settings != null
         && this.world.settings.type == WorldSettings.BEDWARS
         && !SkyGen.isTeamBedIntact(this.world, SkyGen.getPlayerTeam(this));
      this.gui.centered(hc ? "Game Over!" : (bwEliminated ? "ELIMINATED!" : "You died!"), var1, 40.0F, -1, true, 3.0F);
      long daysLived = this.world != null ? (this.world.dayTime / 24000L + 1L) : 1L;
      this.gui.centered("§7Day " + daysLived + "  |  Kills: §e" + SkyGen.myKills + "  §7|  Deaths: §c" + Math.max(1, SkyGen.myDeaths), var1, 63.0F, -1, true, 1.0F);
      if (hc) {
         this.gui.centered("\u00a7cYou cannot respawn in Hardcore Mode!", var1, 74.0F, 0xFFFF5555, true, 1.0F);
         this.button("Spectate World", var1 - 100.0F, this.gui.h / 2.0F - 10.0F, 200.0F, 20.0F, true, () -> {
            this.player.dead = false;
            this.player.burn = 0;
            this.player.health = 20;
            this.player.setMode(3);
            this.deathHandled = false;
            this.setScreen(Game.S.PLAY);
         });
      } else if (bwEliminated) {
         this.gui.centered("\u00a7cYour bed was destroyed — you cannot respawn!", var1, 74.0F, 0xFFFF5555, true, 1.0F);
         this.button("Spectate Match", var1 - 100.0F, this.gui.h / 2.0F - 10.0F, 200.0F, 20.0F, true, () -> {
            this.respawn();
            this.setScreen(Game.S.PLAY);
         });
      } else {
         this.button("Respawn", var1 - 100.0F, this.gui.h / 2.0F - 10.0F, 200.0F, 20.0F, true, () -> {
            this.respawn();
            this.setScreen(Game.S.PLAY);
         });
      }
      this.button("Title Screen", var1 - 100.0F, this.gui.h / 2.0F + 14.0F, 200.0F, 20.0F, true, this::quitToTitle);
   }

   String onOff(boolean var1) {
      return var1 ? "ON" : "OFF";
   }

   // ------------------------------------------------------------------ Update 3.9: the Skins screen
   /** Shortens a line with a trailing "~" until it is at most maxW GUI units wide. */
   static String clipText(String s, float maxW) {
      if (Gui.textWidth(s) <= maxW) {
         return s;
      }

      String t = s;
      while (t.length() > 1 && Gui.textWidth(t + "~") > maxW) {
         t = t.substring(0, t.length() - 1);
      }

      return t + "~";
   }

   /** Word-wraps a line of text to at most maxW GUI units per line (a single word that is too long keeps its own line). */
   static java.util.List<String> wrapText(String s, float maxW) {
      java.util.ArrayList<String> out = new java.util.ArrayList<>();
      String line = "";
      for (String word : s.split(" ")) {
         String attempt = line.isEmpty() ? word : line + " " + word;
         if (line.isEmpty() || Gui.textWidth(attempt) <= maxW) {
            line = attempt;
         } else {
            out.add(line);
            line = word;
         }
      }

      if (!line.isEmpty()) {
         out.add(line);
      }

      return out;
   }

   /** The first of the labels (longest first) that fits in maxW GUI units, or the last one. */
   static String fitLabel(float maxW, String... options) {
      for (String o : options) {
         if (Gui.textWidth(o) <= maxW) {
            return o;
         }
      }

      return options[options.length - 1];
   }

   /**
    * The Skins screen. The layout follows the window: widths are clamped to what is available (a narrow window or a big GUI scale
    * gives a narrower list and shorter button labels), the hint text wraps, and everything is stacked from the bottom up so nothing
    * overlaps on a short window.
    */
   void drawSkins() {
      float W = this.gui.w;
      float H = this.gui.h;
      float cx = W / 2.0F;
      this.gui.centered("Skins", cx, 7.0F, -1, true, 1.0F);
      java.util.List<String> names = this.skins.choices();
      int cur = Math.max(0, names.indexOf(this.opt.skin));
      if (this.skinSel < 0 || this.skinSel >= names.size()) {
         this.skinSel = cur;
      }

      // ---- horizontal layout: [list 4 scroll buttons 10 | right column]
      final float rw = Math.max(100.0F, Math.min(140.0F, W * 0.36F));
      final float lw = Math.max(110.0F, Math.min(182.0F, W - rw - 40.0F));
      float total = lw + 4.0F + 14.0F + 10.0F + rw;
      final float left = Math.max(3.0F, (W - total) / 2.0F);
      final float rx = left + lw + 4.0F + 14.0F + 10.0F;
      final float pcx = rx + rw / 2.0F;
      // ---- vertical layout, bottom up: Done, hint lines, status line, then the controls block above them
      float rowH = 15.0F;
      float top = 20.0F;
      float doneY = H - 26.0F;
      java.util.List<String> tip = H >= 200.0F
         ? wrapText("Drag a skin .png onto this window to wear it. Or copy a file path, link or username, then press Ctrl+V.", W - 12.0F)
         : new java.util.ArrayList<String>();
      float tipTop = doneY - 4.0F - tip.size() * 10.0F;
      float statusY = tipTop - 11.0F;
      int rows = Math.max(3, Math.min(8, (int)((statusY - 2.0F - top - 62.0F) / rowH)));
      this.skinScroll = Math.max(0, Math.min(Math.max(0, names.size() - rows), this.skinScroll));
      float listH = rows * rowH;
      this.gui.rect(left - 1.0F, top - 1.0F, lw + 2.0F, listH + 2.0F, -8355712);
      this.gui.rect(left, top, lw, listH, -1442840576);
      for (int i = 0; i < rows; i++) {
         int idx = this.skinScroll + i;
         if (idx >= names.size()) {
            break;
         }

         final String n = names.get(idx);
         float y = top + i * rowH;
         boolean wearing = n.equals(this.opt.skin);
         if (this.hover(left, y, lw, rowH)) {
            this.gui.rect(left, y, lw, rowH, 1090519039);
         }

         if (idx == this.skinSel) {
            this.gui.rect(left, y, lw, rowH, 1627389951);
         }

         String label = n.equals(Skins.DEFAULT) ? "Default skin" : n.substring(0, n.length() - 4);
         this.gui.text((wearing ? "> " : "  ") + clipText(label, lw - 14.0F), left + 3.0F, y + 4.0F, wearing ? -256 : -1, true);
         final int fi = idx;
         this.hitbox(left, y, lw, rowH, () -> {
            this.skinSel = fi;
            this.opt.skin = n;
            this.opt.save();
            this.skinMsg = "";
         });
      }

      this.button("^", left + lw + 4.0F, top, 14.0F, 14.0F, true, () -> this.skinScroll = Math.max(0, this.skinScroll - 1));
      this.button("v", left + lw + 4.0F, top + listH - 14.0F, 14.0F, 14.0F, true, () -> this.skinScroll++);
      // ---- the preview: your skin, slowly turning
      float feetY = top + listH - 16.0F;
      float scale = Math.min(60.0F, (listH - 26.0F) / 1.9F);
      this.gui.rect(rx, top - 1.0F, rw, listH + 2.0F, 1073741824);
      PlayerModel.drawPreview(this.skinPixels(), this.skinSlim(), pcx, feetY, scale, 25.0F + (float)(GLFW.glfwGetTime() * 28.0 % 360.0), (float)GLFW.glfwGetTime());
      String shown = this.opt.skin.equals(Skins.DEFAULT) ? "Default skin" : this.opt.skin;
      this.gui.centered(clipText(shown, rw), pcx, top + listH + 3.0F, -5592406, true, 1.0F);
      float by = top + listH + 14.0F;
      boolean custom = !this.opt.skin.equals(Skins.DEFAULT);
      String arms = this.skinSlim() ? "Slim" : "Classic";
      this.button(fitLabel(rw - 6.0F, "Arms: " + arms + (this.skinSlim() ? " (3 px)" : " (4 px)"), "Arms: " + arms, arms), rx, by, rw, 18.0F, custom, () -> {
         this.skins.setSlim(this.opt.skin, !this.skinSlim());
      });
      // ---- username / link / path field + buttons
      this.gui.text(fitLabel(lw, "Minecraft username:", "Username:"), left, by - 10.0F, -6250336, true);
      float fieldW = lw - 58.0F;
      this.gui.rect(left, by, fieldW, 18.0F, this.skinFocus ? -1 : -6250336);
      this.gui.rect(left + 1.0F, by + 1.0F, fieldW - 2.0F, 16.0F, -16777216);
      this.gui.text(this.skinField + (this.skinFocus && (int)(GLFW.glfwGetTime() * 2.0) % 2 == 0 ? "_" : ""), left + 4.0F, by + 5.0F, -1, true);
      this.hitbox(left, by, fieldW, 18.0F, () -> this.skinFocus = true);
      this.button("Import", left + lw - 54.0F, by, 54.0F, 18.0F, !this.skins.busy && !this.skinField.isEmpty(), () -> this.skins.importUser(this.skinField));
      this.button(fitLabel(lw - 6.0F, "Import skin file...", "Import file...", "File..."), left, by + 22.0F, lw, 18.0F, !this.skinBrowsing, this::browseSkin);
      this.button(fitLabel(rw - 6.0F, "Open skins folder", "Skins folder", "Folder"), rx, by + 22.0F, rw, 18.0F, true, this::openSkinsFolder);
      // ---- status, hint, Done
      String st = this.skins.busy || this.skins.status.length() > 0 && this.skinMsg.isEmpty() ? this.skins.status : this.skinMsg;
      if (!st.isEmpty()) {
         this.gui.centered(clipText(st, W - 8.0F), cx, statusY, st.startsWith("Imported") ? -8323200 : (st.startsWith("Could not") || st.startsWith("Not a") ? -43691 : -5592406), true, 1.0F);
      }

      for (int i = 0; i < tip.size(); i++) {
         this.gui.centered(tip.get(i), cx, tipTop + i * 10.0F, -8355712, true, 1.0F);
      }

      float doneW = Math.min(200.0F, W - 20.0F);
      this.button("Done", cx - doneW / 2.0F, doneY, doneW, 20.0F, true, () -> {
         this.opt.save();
         this.setScreen(this.skinReturn);
      });
   }

   void skinKey(int key, int action, int mods) {
      java.util.List<String> names = this.skins.choices();
      if (key == 256 && action == 1) {
         if (this.skinFocus) {
            this.skinFocus = false;
         } else {
            this.opt.save();
            this.setScreen(this.skinReturn);
         }
      } else if (key == 259 && !this.skinField.isEmpty() && this.skinFocus) {
         this.skinField = this.skinField.substring(0, this.skinField.length() - 1);
      } else if ((key == 257 || key == 335) && action == 1 && this.skinFocus && !this.skinField.isEmpty()) {
         this.skins.importUser(this.skinField);
      } else if (key == 258 && action == 1) {
         this.skinFocus = !this.skinFocus;
      } else if (key == 67 && action == 1 && (mods & 10) != 0 && this.skinFocus) {
         this.setClipboard(this.skinField);
      } else if (key == 88 && action == 1 && (mods & 10) != 0 && this.skinFocus) {
         this.setClipboard(this.skinField);
         this.skinField = "";
      } else if (key == 86 && action == 1 && (mods & 10) != 0) {
         String text = this.getClipboard();
         if (this.skinFocus && text != null && text.trim().matches("[A-Za-z0-9_]{1,16}")) {
            String clean = text.trim();
            this.skinField = (this.skinField + clean).substring(0, Math.min(16, this.skinField.length() + clean.length()));
         } else {
            File f = this.skins.importFromText(text);
            if (f != null) {
               this.importSkin(f);
            }
         }
      } else if ((key == 265 || key == 264) && !this.skinFocus) {
         int i = Math.max(0, Math.min(names.size() - 1, (this.skinSel < 0 ? 0 : this.skinSel) + (key == 264 ? 1 : -1)));
         this.skinSel = i;
         this.opt.skin = names.get(i);
         this.opt.save();
         this.skinScroll = Math.max(0, Math.min(i, this.skinScroll > i ? i : this.skinScroll));
      }
   }

   /**
    * The system's own file dialog (Windows), on a helper thread so the game keeps drawing; the chosen file arrives through skinDrops.
    * A fullscreen game window is "topmost" and would hide the dialog, so the game leaves fullscreen while the dialog is open (and
    * returns to it afterwards); the dialog is always disposed, otherwise AWT would keep the process alive after the game window closes.
    */
   void browseSkin() {
      if (this.skinBrowsing) {
         return;
      }

      this.skinBrowsing = true;
      final boolean wasFullscreen = this.opt.fullscreen;
      if (wasFullscreen) {
         this.setFullscreen(false);
      }

      Thread t = new Thread(() -> {
         java.awt.FileDialog fd = null;
         try {
            fd = new java.awt.FileDialog((java.awt.Frame)null, "Choose a Minecraft skin (a 64x64 PNG)", java.awt.FileDialog.LOAD);
            fd.setFile("*.png");
            fd.setFilenameFilter((d, n) -> n.toLowerCase().endsWith(".png"));
            System.out.println("[skins] file dialog opened");
            fd.setVisible(true);
            if (fd.getFile() != null) {
               this.skinDrops.add(new File(fd.getDirectory(), fd.getFile()));
            }

            System.out.println("[skins] file dialog closed" + (fd.getFile() != null ? " with a file" : ""));
         } catch (Throwable e) {
            System.err.println("[skins] no file dialog: " + e);
            this.skinMsg = "No file dialog here - drag the skin .png onto this window instead";
         } finally {
            if (fd != null) {
               try {
                  fd.dispose();
               } catch (Throwable ignored) {
               }
            }

            this.skinRefull = wasFullscreen;
            this.skinBrowsing = false;
         }
      }, "skin-browse");
      t.setDaemon(true);
      t.start();
   }

   void openSkinsFolder() {
      try {
         this.skins.dir.mkdirs();
         String os = System.getProperty("os.name", "").toLowerCase();
         String[] cmd = os.startsWith("windows") ? new String[]{"explorer", this.skins.dir.getAbsolutePath()} : new String[]{os.contains("mac") ? "open" : "xdg-open", this.skins.dir.getAbsolutePath()};
         new ProcessBuilder(cmd).start();
      } catch (Exception e) {
         this.skinMsg = "Skins folder: " + this.skins.dir.getAbsolutePath();
      }
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
         if ((this.screen == Game.S.PLAY || this.screen == Game.S.CHAT) && this.view == 0) {
            GL11.glBlendFunc(775, 769);
            float var1 = (float)Math.floor(this.gui.w / 2.0F);
            float var2 = (float)Math.floor(this.gui.h / 2.0F);
            this.gui.rect(var1 - 7.0F, var2 - 0.5F, 15.0F, 1.0F, -1);
            this.gui.rect(var1 - 0.5F, var2 - 7.0F, 1.0F, 6.5F, -1);
            this.gui.rect(var1 - 0.5F, var2 + 0.5F, 1.0F, 6.5F, -1);
            GL11.glBlendFunc(770, 771);
         }

         this.drawHotbar();
         if (this.player.spectator) {
            this.drawSpectatorHud();
         } else if (this.player.adventure && this.target != null && this.world != null && this.screen == Game.S.PLAY) {
            int tbid = this.world.getBlock(this.target.x, this.target.y, this.target.z);
            if (tbid > 0) {
               boolean canBrk = Items.canAdventureBreak(this.heldId(), tbid);
               String advHint = (canBrk ? "§a[Can Break: " : "§c[Need Tool: ") + Items.name(tbid) + "]";
               this.gui.centered(advHint, this.gui.w / 2.0F, this.gui.h - 48.0F, 0xFFFFFFFF, true, 1.0F);
            }
         }
         SkyGen.drawModeHud(this);
         this.drawBossBar();
         this.adv.drawToast(this);
         if (this.window != 0L && this.screen == Game.S.PLAY && GLFW.glfwGetKey(this.window, 258) == 1) {
            Net319.drawTabOverlay(this);
         }
         String wm = ModLoader.hudWatermark();
         if (wm != null && !this.debug) {
            this.gui.text(wm, 4.0F, 4.0F, 0xFFFFFFFF, true, 1.0F);
         }
         if (BackroomsStalkerMod.isActive()) {
            BackroomsStalkerMod.drawHudOverlay(this);
         }
         if (ModLoader.isWaystonesHudActive() && !this.debug && this.world != null) {
            this.drawWaystonesHud();
         }
      }

      if (this.debug) {
         this.drawDebug();
      }

      if (Perf.show) {
         this.drawPerf();
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

      if (this.portalTime > 0) {
         // Update 3.12: the purple haze of a nether portal grows while you stand in it
         float k = Math.min(1.0F, this.portalTime / 70.0F);
         int pulse = (int)(10.0F * Math.sin(this.tickCount * 0.4F));
         this.gui.rect(0.0F, 0.0F, this.gui.w, this.gui.h, Math.min(255, (int)(k * 150.0F) + pulse) << 24 | 0x7020C0);
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
         if (this.player.hardcore) {
            var1 = Math.max(1, (var1 * 3 + 1) / 2);
         }
         if (this.world != null && this.world.settings != null && this.world.settings.type == WorldSettings.BEDWARS && SkyGen.hasTeamProtection(SkyGen.getPlayerTeam(this))) {
            var1 = Math.max(1, Math.round(var1 * 0.8F));
         }
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
               float hx = var1 + var14 * 8;
               float hy = var12 + var8;
               this.gui.heart(hx, hy, 0);
               if (var7 > 0) {
                  this.gui.heart(hx, hy, var7);
                  if (this.player.hardcore) {
                     // Hardcore Mode heart inner marks
                     this.gui.rect(hx + 2.0F, hy + 3.0F, 1.0F, 2.0F, 0xFF280000);
                     if (var7 == 2) {
                        this.gui.rect(hx + 5.0F, hy + 3.0F, 1.0F, 2.0F, 0xFF280000);
                     }
                  }
               }
            }

            int var15 = this.armorPoints();
            boolean infdevHud = ModLoader.isClassicHudNoHunger();
            if (var15 > 0) {
               for (int var17 = 0; var17 < 10; var17++) {
                  float ax = infdevHud ? (var1 + 182.0F - 9.0F - var17 * 8) : (var1 + var17 * 8);
                  float ay = infdevHud ? var12 : (var12 - 10.0F);
                  this.gui.armorIcon(ax, ay, var15 - var17 * 2 >= 2 ? 2 : (var15 - var17 * 2 == 1 ? 1 : 0));
               }
            }

            if (!infdevHud) {
               for (int var18 = 0; var18 < 10; var18++) {
                  int var20 = this.player.food - var18 * 2;
                  int var22 = var20 >= 2 ? 2 : (var20 == 1 ? 1 : 0);
                  float var9 = this.player.sat <= 0.0F && this.player.food <= 6 && (this.tickCount / 3 + var18) % 6 == 0 ? 1.0F : 0.0F;
                  this.gui.hunger(var1 + 182.0F - 9.0F - var18 * 8, var12 + var9, 0);
                  if (var22 > 0) {
                     this.gui.hunger(var1 + 182.0F - 9.0F - var18 * 8, var12 + var9, var22);
                  }
               }
            }

            if (this.player.air < 300) {
               int var19 = (int)Math.ceil(this.player.air / 30.0);

               for (int var21 = 0; var21 < 10; var21++) {
                  if (var21 < var19) {
                     float bx = infdevHud ? (var1 + var21 * 8) : (var1 + 182.0F - 9.0F - var21 * 8);
                     this.gui.bubble(bx, var12 - 10.0F);
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

   void drawSpectatorHud() {
      float cx = this.gui.w / 2.0F;
      int pct = Math.round(this.player.flySpeed / 0.05F * 100.0F);
      String pov = this.spectatingMob != null && !this.spectatingMob.dead
         ? (" | POV: \u00a7e" + Mobs.NAME[this.spectatingMob.type] + " \u00a77(Shift to exit)")
         : (this.targetMob != null ? (" | Target: \u00a7e" + Mobs.NAME[this.targetMob.type] + " (" + this.targetMob.health + " HP)") : "");
      if (pov.isEmpty() && this.target != null && this.world != null) {
         int bid = this.world.getBlock(this.target.x, this.target.y, this.target.z);
         if (bid > 0) {
            pov = " | Looking at: \u00a7f" + Items.name(bid);
         }
      }
      String top = String.format(
         "\u00a7bSPECTATOR \u00a77| Speed: \u00a7f%d%% \u00a77(Scroll) | XYZ: \u00a7f%d, %d, %d\u00a77%s",
         pct,
         (int)Math.floor(this.player.x),
         (int)Math.floor(this.player.y),
         (int)Math.floor(this.player.z),
         pov
      );
      float tw = Gui.textWidth(Gui.stripCodes(top)) + 10.0F;
      this.gui.rect(cx - tw / 2.0F, 3.0F, tw, 12.0F, 0x90000000);
      this.gui.centered(top, cx, 5.0F, 0xFFFFFFFF, true, 1.0F);

      String bar = "\u00a7e[1]\u00a7f Spawn  \u00a7e[2]\u00a7f Mob POV  \u00a7e[3]\u00a7f Center/Village  \u00a7e[4]\u00a7f Surface  \u00a7e[5]\u00a7f NightVis  \u00a7e[6]\u00a7f Next/Reset";
      float bw = Gui.textWidth(Gui.stripCodes(bar)) + 12.0F;
      float by = this.gui.h - 16.0F;
      this.gui.rect(cx - bw / 2.0F, by - 2.0F, bw, 13.0F, 0xA010141C);
      this.gui.centered(bar, cx, by, 0xFFFFFFFF, true, 1.0F);
   }

   /** Update 3.18: the frame timer's panel (Shift+F3), below the F3 text when that is open too. */
   void drawPerf() {
      float y = this.debug ? 150.0F : 2.0F;
      for (String s : Perf.lines(this.gpuName)) {
         this.gui.rect(1.0F, y - 1.0F, Gui.textWidth(s) + 2, 10.0F, -1873784752);
         this.gui.text(s, 2.0F, y, -2039584, false);
         y += 10.0F;
      }
   }

   /** Update 3.18: is the shader pack running? (a graphics card whose driver cannot compile it falls back to the plain picture: no shadows, no water effects, no clouds - the reason is in latest.log) */
   String shaderStatus() {
      Shaders sh = this.rend == null ? null : this.rend.sh;
      if (sh == null) {
         return "Shaders: ?";
      }

      if (!sh.ok) {
         return "Shaders: UNAVAILABLE (" + (sh.error == null || sh.error.isEmpty() ? "see latest.log" : sh.error.replace('\n', ' ').substring(0, Math.min(60, sh.error.length()))) + ")";
      }

      if (!this.opt.shaders) {
         return "Shaders: off (options)";
      }

      return "Shaders: on" + (sh.waterOk ? ", water" : "") + (sh.cloudsOk ? ", clouds" : "") + (sh.entityOn ? ", mobs" : "");
   }

   /**
    * Update 3.19: why the shader pack is not running although the options ask for it (null: it runs, or it is switched off on purpose). Players wrote that
    * "nothing changed" after an update of the shaders: on a graphics chip whose driver cannot compile them the game fell back to the plain picture without a word.
    */
   String shaderProblem() {
      Shaders sh = this.rend == null ? null : this.rend.sh;
      if (sh == null || !this.opt.shaders || sh.ok) {
         return null;
      }

      String why = sh.error == null || sh.error.trim().isEmpty() ? "see latest.log" : sh.error.trim().replace('\n', ' ').replace('\r', ' ');
      return why.length() > 120 ? why.substring(0, 120) : why;
   }

   /** Update 3.19: the parts of the shader pack that this graphics chip could not start although the rest runs (empty = none). */
   String shaderPartial() {
      Shaders sh = this.rend == null ? null : this.rend.sh;
      if (sh == null || !this.opt.shaders || !sh.ok) {
         return "";
      }

      StringBuilder b = new StringBuilder();
      if (!sh.waterOk) {
         b.append("water, ");
      }

      if (!sh.cloudsOk) {
         b.append("clouds, ");
      }

      if (!sh.entityOk) {
         b.append("mobs, ");
      }

      if (!sh.lodOk) {
         b.append("far land, ");
      }

      if (!sh.fxOk) {
         b.append("glow and the new effects, ");
      }

      return b.length() == 0 ? "" : b.substring(0, b.length() - 2);
   }

   /** Update 3.19: tells the player in the chat, once per world, when the shader pack (or a part of it) is not running on this computer. */
   void sayShaderProblems() {
      String p = this.shaderProblem();
      if (p != null) {
         this.say("\u00a7cThe shader pack cannot run on this graphics chip, so the plain picture is used.");
         this.say("\u00a77" + clipText("Reason: " + p, 300.0F) + " (F3 shows it; more in latest.log)");
         return;
      }

      String part = this.shaderPartial();
      if (!part.isEmpty()) {
         this.say("\u00a7eSome shader effects are off on this graphics chip: " + part + ". (F3 shows the details)");
      }
   }

   void drawDebug() {
      int var1 = (int)Math.floor(this.player.x);
      int var2 = (int)Math.floor(this.player.y);
      int var3 = (int)Math.floor(this.player.z);
      Chunk var4 = this.world.chunkAt(var1, var3);
      String var5 = var4 == null ? "?" : Biomes.name(var4.biome[(var3 & 15) * 16 + (var1 & 15)]);
      int var6 = ((int)Math.floor(this.player.yaw / 90.0 + 0.5) % 4 + 4) % 4;
      float var7 = ((this.player.yaw + 180.0F) % 360.0F + 360.0F) % 360.0F - 180.0F;
      long var8 = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576L;
      String[] var10 = new String[]{
         "Blockcraft 1.12 Update " + VERSION_STR,
         this.fps + " fps, " + this.rend.visibleSections + " sections, " + this.rend.drawCalls + " draws   GPU: " + GpuPref.shortName(this.gpuName),
         "C: " + (this.world == null ? 0 : this.world.chunks.size()) + " chunks loaded, mesh queue: " + this.rend.inflight.get() + "   " + this.shaderStatus(),
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
            int maxDbgW = Math.max(60, (int)this.gui.w - 6);
            for (String dbgLine : wrapChatLine(var15, maxDbgW)) {
               float bw = Math.min(this.gui.w - 2.0F, Gui.textWidth(Gui.stripCodes(dbgLine)) + 2.0F);
               this.gui.rect(1.0F, var11 - 1.0F, bw, 10.0F, -1873784752);
               this.gui.text(dbgLine, 2.0F, var11, -2039584, false);
               var11 += 10.0F;
            }
         }
      }
   }

   static java.util.List<String> wrapChatLine(String msg, int maxWidth) {
      java.util.ArrayList<String> out = new java.util.ArrayList<>();
      if (msg == null) {
         out.add("");
         return out;
      }
      int limit = Math.max(24, maxWidth);
      if (Gui.textWidth(Gui.stripCodes(msg)) <= limit) {
         out.add(msg);
         return out;
      }

      String activeColor = "";
      StringBuilder cur = new StringBuilder();
      int i = 0;
      int n = msg.length();
      while (i < n) {
         if (msg.charAt(i) == ' ') {
            if (cur.length() > activeColor.length()) {
               String candidate = cur.toString() + " ";
               if (Gui.textWidth(Gui.stripCodes(candidate)) <= limit) {
                  cur.append(' ');
               } else {
                  out.add(cur.toString());
                  cur.setLength(0);
                  cur.append(activeColor);
               }
            }
            i++;
            continue;
         }

         int wStart = i;
         while (i < n && msg.charAt(i) != ' ') {
            i++;
         }
         String word = msg.substring(wStart, i);
         String nextColor = lastColorCode(word, activeColor);

         if (cur.length() > activeColor.length()) {
            String candidate = cur.toString() + word;
            if (Gui.textWidth(Gui.stripCodes(candidate)) <= limit) {
               cur.append(word);
               activeColor = nextColor;
               continue;
            }
            out.add(cur.toString());
            cur.setLength(0);
            cur.append(activeColor);
         }

         if (Gui.textWidth(Gui.stripCodes(cur.toString() + word)) <= limit) {
            cur.append(word);
            activeColor = nextColor;
         } else {
            // Split an overlong token character-by-character while preserving § codes
            int wi = 0;
            while (wi < word.length()) {
               char c = word.charAt(wi);
               if (c == 167 && wi + 1 < word.length()) {
                  String code = word.substring(wi, wi + 2);
                  cur.append(code);
                  activeColor = code.equalsIgnoreCase("§r") ? "" : code;
                  wi += 2;
                  continue;
               }
               String cand = cur.toString() + c;
               if (Gui.textWidth(Gui.stripCodes(cand)) > limit && cur.length() > activeColor.length()) {
                  out.add(cur.toString());
                  cur.setLength(0);
                  cur.append(activeColor);
               }
               cur.append(c);
               wi++;
            }
         }
      }

      if (cur.length() > activeColor.length() || out.isEmpty()) {
         out.add(cur.toString());
      }
      return out;
   }

   private static String lastColorCode(String s, String current) {
      String c = current;
      for (int i = 0; i + 1 < s.length(); i++) {
         if (s.charAt(i) == 167) {
            String code = s.substring(i, i + 2);
            c = code.equalsIgnoreCase("§r") ? "" : code;
            i++;
         }
      }
      return c;
   }

   static String clipChatInput(String text, int maxWidth) {
      if (text == null || text.isEmpty()) {
         return "";
      }
      int limit = Math.max(16, maxWidth);
      if (Gui.textWidth(Gui.stripCodes(text)) <= limit) {
         return text;
      }
      int start = 0;
      while (start < text.length() - 1 && Gui.textWidth(Gui.stripCodes(text.substring(start))) > limit) {
         start++;
      }
      return text.substring(start);
   }

   void drawChat() {
      long var1 = System.currentTimeMillis();
      float var3 = this.gui.h - 40.0F;
      int var4 = 0;
      int maxChatW = Math.max(60, (int)Math.min(this.gui.w - 10.0F, 320.0F));

      for (int var5 = this.chatLog.size() - 1; var5 >= 0 && var4 < 12 && var3 >= 6.0F; var5--) {
         long var6 = var1 - (Long)this.chatTimes.get(var5);
         if (var6 > 10000L && this.screen != Game.S.CHAT) {
            break;
         }

         float var8 = this.screen == Game.S.CHAT ? 1.0F : Math.max(0.0F, Math.min(1.0F, (float)(10000L - var6) / 2000.0F));
         String var9 = (String)this.chatLog.get(var5);
         java.util.List<String> wrapped = wrapChatLine(var9, maxChatW);
         for (int li = wrapped.size() - 1; li >= 0 && var4 < 12 && var3 >= 6.0F; li--) {
            String line = wrapped.get(li);
            float bw = Math.min(this.gui.w - 4.0F, Gui.textWidth(Gui.stripCodes(line)) + 4.0F);
            this.gui.rect(2.0F, var3 - 1.0F, bw, 10.0F, (int)(var8 * 100.0F) << 24);
            this.gui.text(line, 3.0F, var3, (int)(var8 * 255.0F) << 24 | 16777215, true);
            var3 -= 10.0F;
            var4++;
         }
      }

      if (this.screen == Game.S.CHAT) {
         this.gui.rect(2.0F, this.gui.h - 14.0F, this.gui.w - 4.0F, 12.0F, Integer.MIN_VALUE);
         String var10 = clipChatInput(this.chat + ((int)(GLFW.glfwGetTime() * 2.0) % 2 == 0 ? "_" : ""), Math.max(20, (int)this.gui.w - 12));
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
            if (var5 > 1) {
               String var6 = this.menu.page + 1 + "/" + var5;
               this.gui.text(var6, var3 + this.menu.w - 8.0F - Gui.textWidth(var6), var4 + 6.0F, -12566464, false);
               Menu var7 = this.menu;
               this.button("<", var3 + this.menu.w - 70.0F, var4 + 3.0F, 12.0F, 12.0F, this.menu.page > 0, () -> var7.page--);
               this.button(">", var3 + this.menu.w - 56.0F, var4 + 3.0F, 12.0F, 12.0F, this.menu.page < var5 - 1, () -> var7.page++);
            }
            this.button("X", var3 + this.menu.w - 42.0F, var4 + 3.0F, 12.0F, 12.0F, true, () -> {
               if (this.cursor != null) {
                  this.cursor = null;
               } else {
                  for (int i = 0; i < 9; i++) {
                     this.inv[i] = null;
                  }
               }
            });

            if (this.menu.palette.length == 0) {
               this.gui.centered("Nothing found", var3 + this.menu.w / 2.0F, var4 + 52.0F, -9408400, false, 1.0F);
            }

            this.gui.centered("Drop an item on the list to delete it.", this.gui.w / 2.0F, var4 + this.menu.h + 5.0F, -1426063361, true, 1.0F);
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
      /** The text on the widget (the tests check that it fits). */
      String label;
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
      /** Update 3.16: the life it started with (set by the first tick), for fading and shrinking. */
      int life0;
      /** Update 3.16: bit 0: fades out over the last part of its life (and in at the start), bit 1: shrinks. */
      int fade;
      /** Update 3.16: drawn additively (light: embers, fireflies, bubbles, flames) and not darkened by the place it is in. */
      boolean glow;
      /** Update 3.16: 1 = firefly (counted, so there are never too many). */
      int kind;
      /** Update 3.16: pulses (fireflies). */
      int twinkle;
      /** Update 3.16: sways from side to side (leaves, embers, bubbles): the strength of the push per tick and where in the sway it starts. */
      float sway;
      float phase;
      float alpha = 1.0F;
   }

   public void chatMsg(String msg) {
      this.say(msg);
   }

   void drawWaystonesHud() {
      int px = (int)Math.floor(this.player.x);
      int py = (int)Math.floor(this.player.y);
      int pz = (int)Math.floor(this.player.z);
      float yaw = ((this.player.yaw % 360.0F) + 360.0F) % 360.0F;
      String[] dirs = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
      String dir = dirs[Math.round(yaw / 45.0F) & 7];
      long day = this.world.dayTime / 24000L + 1L;
      String line1 = String.format("XYZ: %d, %d, %d  [%s]", px, py, pz, dir);
      String line2 = "Day " + day + "  |  " + this.fps + " FPS";
      float w = Math.max(Gui.textWidth(line1), Gui.textWidth(line2)) + 10.0F;
      float x = this.gui.w - w - 4.0F;
      float y = 4.0F;
      this.gui.rect(x, y, w, 24.0F, 0x9010141A);
      this.gui.text(line1, x + 5.0F, y + 3.0F, 0xFF8FE060, true);
      this.gui.text(line2, x + 5.0F, y + 13.0F, 0xFFD6D6E0, true);
   }

   enum S {
      TITLE,
      WORLDS,
      MULTIPLAYER,
      NEWWORLD,
      LOADING,
      WAITING,
      PLAY,
      PAUSE,
      INV,
      ADV,
      CHAT,
      OPTIONS,
      SKINS,
      PACKS,
      MODS,
      DEAD;
   }

   static final class WInfo {
      String dir;
      String name;
      long mod;
      int mode;
      long seed;
      WorldSettings settings = WorldSettings.classic();
      WorldPreview thumb;
   }
}
