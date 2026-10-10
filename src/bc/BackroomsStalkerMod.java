package bc;

import java.util.Random;
import org.lwjgl.opengl.GL11;

/**
 * The Backrooms & Stalker Horror Total-Conversion Mod (ID: {@code backrooms-stalker}).
 * <p>
 * Combines:
 * <ul>
 *   <li><b>Infinite Multi-Zone Level 0 Backrooms Worldgen</b> ({@link BackroomsGen}) where you always spawn
 *       <i>inside</i> the labyrinth at {@code Y = 65}, plus {@code /backrooms} noclip pocket labyrinth</li>
 *   <li><b>Automatic VHS Analog Horror Shader, Fog & Dynamic Lighting</b>: buzzing 60 Hz yellow-ochre fog,
 *       blood-crimson blackout fog, camcorder flashlight beam, and chromatic aberration / scanline shader</li>
 *   <li><b>Custom 3D Stalker Entity ("The Bacteria / Wire-Stalker")</b>: 3.3-block-tall articulated 3D horror
 *       entity with glowing pinprick crimson eyes, unhinging toothed jaw, 6 writhing dorsal tendrils, and
 *       4-phase stalking / freeze / blackout / chase AI</li>
 * </ul>
 */
public class BackroomsStalkerMod implements BlockcraftMod {
   public static final int STALKER_TAG = 0xB4C700;

   public static final int PHASE_STALK = 0;
   public static final int PHASE_FROZEN = 1;
   public static final int PHASE_AMBUSH = 2;
   public static final int PHASE_CHASE = 3;

   /** Pocket Backrooms elevation when entered via /backrooms in a normal Overworld. */
   public static final int POCKET_Y = 200;
   public static final int POCKET_FLOOR_Y = POCKET_Y;
   public static final int POCKET_RADIUS = 20;

   private static boolean active = false;
   private static BackroomsStalkerMod instance;

   private final Random rng = new Random();
   private Game lastGame;
   private boolean worldGenEnabled = true;
   private boolean inPocketBackrooms = false;
   private double returnX;
   private double returnY = -1.0;
   private double returnZ;

   // Saved graphics settings restored when the mod is disabled
   private boolean savedSettingsValid = false;
   private boolean savedShaders;
   private boolean savedSmooth;
   private int savedFx;
   private boolean savedClouds;
   private boolean savedWeather;
   private boolean savedDisasters;
   private float savedGamma;
   private int savedLook;

   // Stalker Entity & Horror State
   private static Mobs.Mob stalkerMob;
   private static int stalkerPhase = PHASE_STALK;
   private static int stareTicks = 0;
   private static int chaseTicks = 0;
   private static int stalkerCooldown = 60;
   private static int sanity = 100;
   private static int heartbeatBpm = 72;
   private static float panicLevel = 0.0F;
   private static float jumpscareFlash = 0.0F;
   private static float lastDistToPlayer = 99.0F;

   // Fluorescent light blackout ring buffer
   private static int blackoutTicks = 0;
   private final int[] flickeredLightsX = new int[48];
   private final int[] flickeredLightsY = new int[48];
   private final int[] flickeredLightsZ = new int[48];
   private int flickeredCount = 0;

   public BackroomsStalkerMod() {
      instance = this;
   }

   public static boolean isActive() {
      return active;
   }

   public static boolean isStalkerMob(Mobs.Mob m) {
      return m != null && m.tag == STALKER_TAG;
   }

   public static Mobs.Mob getStalker() {
      return stalkerMob != null && !stalkerMob.dead ? stalkerMob : null;
   }

   public static int getPhase() {
      return stalkerPhase;
   }

   public static float getPanicLevel() {
      return panicLevel;
   }

   public static boolean isBlackout() {
      return blackoutTicks > 0;
   }

   /**
    * Dynamic Backrooms fog color (RGB in 0..1):
    * - Normal Level 0 state: Sickly 60 Hz buzzing fluorescent yellow-ochre mist (0.64, 0.57, 0.27)
    * - Blackout / Chase state: Pulses into deep blood-crimson horror fog (0.20, 0.02, 0.025)
    */
   public static float[] getFogColor(float timeSec) {
      float hum = 1.0F + 0.025F * (float)Math.sin(timeSec * 376.99F) + 0.015F * (float)Math.sin(timeSec * 19.0F);
      float baseR = 0.64F * hum;
      float baseG = 0.56F * hum;
      float baseB = 0.26F * hum;

      float darkMix = Math.min(1.0F, panicLevel * 0.85F + (blackoutTicks > 0 ? 0.65F : 0.0F));
      float pulse = 0.5F + 0.5F * (float)Math.sin(timeSec * 9.5F);
      float horrorR = 0.14F + 0.14F * pulse * panicLevel;
      float horrorG = 0.018F;
      float horrorB = 0.022F;

      float r = baseR + (horrorR - baseR) * darkMix;
      float g = baseG + (horrorG - baseG) * darkMix;
      float b = baseB + (horrorB - baseB) * darkMix;
      return new float[]{r, g, b};
   }

   /** Near fog plane in blocks — tightens claustrophobically during chases & blackouts. */
   public static float getFogNear() {
      if (blackoutTicks > 0) {
         return 0.6F;
      }
      return Math.max(0.8F, 4.2F - panicLevel * 3.2F);
   }

   /** Far fog plane in blocks — shrouds distant corridors in eerie yellow/crimson haze. */
   public static float getFogFar() {
      if (blackoutTicks > 0) {
         return 9.5F;
      }
      return Math.max(10.5F, 24.0F - panicLevel * 12.0F);
   }

   @Override
   public String id() {
      return "backrooms-stalker";
   }

   @Override
   public String name() {
      return "The Backrooms & Stalker";
   }

   @Override
   public String version() {
      return "2.0.0";
   }

   @Override
   public String description() {
      return "VHS Analog Horror Shader, 6-Zone Level 0 Backrooms WorldGen & Custom 3D Stalker Entity (/backrooms, /stalker).";
   }

   @Override
   public boolean totalConversionInfdev() {
      return true;
   }

   private void applyHorrorSettings(Game g) {
      if (g == null || g.opt == null) {
         return;
      }
      if (!this.savedSettingsValid) {
         this.savedShaders = g.opt.shaders;
         this.savedSmooth = g.opt.smooth;
         this.savedFx = g.opt.fx;
         this.savedClouds = g.opt.clouds;
         this.savedWeather = g.opt.weather;
         this.savedDisasters = g.opt.disasters;
         this.savedGamma = g.opt.gamma;
         this.savedLook = g.opt.look;
         this.savedSettingsValid = true;
      }
      // Enable full VHS Horror Shaders + Smooth Lighting + SSAO/Bloom + Moody Gamma
      g.opt.shaders = true;
      g.opt.smooth = true;
      g.opt.fx = 2;
      g.opt.clouds = false;
      g.opt.weather = false;
      g.opt.disasters = false;
      g.opt.gamma = -0.18F;
      g.opt.look = Look.CINEMATIC;
   }

   private void restoreHorrorSettings(Game g) {
      if (g == null || g.opt == null || !this.savedSettingsValid) {
         return;
      }
      g.opt.shaders = this.savedShaders;
      g.opt.smooth = this.savedSmooth;
      g.opt.fx = this.savedFx;
      g.opt.clouds = this.savedClouds;
      g.opt.weather = this.savedWeather;
      g.opt.disasters = this.savedDisasters;
      g.opt.gamma = this.savedGamma;
      g.opt.look = this.savedLook;
      this.savedSettingsValid = false;
   }

   /** Ensures the player is actually inside the Backrooms floor (not standing on top of the bedrock roof). */
   public void ensurePlayerInsideBackrooms(Game g) {
      if (g == null || g.world == null || g.player == null) {
         return;
      }
      if (g.world.gen instanceof BackroomsGen) {
         int fy = BackroomsGen.FLOOR_Y;
         if (g.player.y > BackroomsGen.CEIL_Y - 0.5 || g.player.y < fy - 2.0) {
            int px = (int)Math.floor(g.player.x);
            int pz = (int)Math.floor(g.player.z);
            // Carve a guaranteed 3x3 breathing room at (px, pz) inside FLOOR_Y+1..CEIL_Y-1
            for (int dx = -1; dx <= 1; dx++) {
               for (int dz = -1; dz <= 1; dz++) {
                  g.world.setBlock(px + dx, fy, pz + dz, Blocks.WOOL0 + 4);
                  for (int y = fy + 1; y < BackroomsGen.CEIL_Y; y++) {
                     int cur = g.world.getBlock(px + dx, y, pz + dz);
                     if (cur != Blocks.AIR && cur != Blocks.CHEST_LOOT) {
                        g.world.setBlock(px + dx, y, pz + dz, Blocks.AIR);
                     }
                  }
                  g.world.setBlock(px + dx, BackroomsGen.CEIL_Y, pz + dz, (dx == 0 && dz == 0) ? Blocks.GLOWSTONE : Blocks.SANDSTONE);
                  g.world.setBlock(px + dx, BackroomsGen.CEIL_Y + 1, pz + dz, Blocks.BEDROCK);
               }
            }
            g.player.setPos(px + 0.5, fy + 1.0, pz + 0.5);
            g.player.vx = 0.0;
            g.player.vy = 0.0;
            g.player.vz = 0.0;
            g.needSpawn = false;
         }
      }
   }

   @Override
   public void onEnable(Game g) {
      this.lastGame = g;
      active = true;
      sanity = 100;
      heartbeatBpm = 75;
      panicLevel = 0.1F;
      stalkerCooldown = 40;
      applyHorrorSettings(g);

      if (g != null && g.world != null && g.player != null) {
         if (!(g.world.gen instanceof BackroomsGen) && g.world.dim == Dim.OVERWORLD) {
            g.world.gen = new BackroomsGen(g.world.seed, g.world.settings);
         }
         if (g.world.gen instanceof BackroomsGen) {
            ensurePlayerInsideBackrooms(g);
         } else if (!this.inPocketBackrooms) {
            enterBackrooms(g);
         }
      }
      if (g != null) {
         g.say("§e[Backrooms] §7VHS Shader, Yellow Fog & 3D Stalker Entity active! (§f/backrooms§7, §f/stalker§7)");
      }
   }

   @Override
   public void onDisable(Game g) {
      active = false;
      panicLevel = 0.0F;
      jumpscareFlash = 0.0F;
      Game target = g != null ? g : this.lastGame;
      restoreFlickeredLights(target);
      despawnStalker(target);
      restoreHorrorSettings(target);
      if (target != null) {
         target.say("§c[Backrooms] §7Disabled. Normal vision and world settings restored.");
      }
   }

   @Override
   public void onInit(ModLoader.Context ctx) {
      if (ctx == null) {
         return;
      }
      // Craft 3 Damp Yellow Wallpaper (Sandstone) + 1 Torch -> 2 Fluorescent Ceiling Lights (Glowstone)
      ctx.addShapedRecipe(
         Blocks.GLOWSTONE,
         2,
         new String[]{"WW", "WT"},
         'W', Blocks.SANDSTONE,
         'T', Blocks.TORCH
      );
      // Craft 2 Rotten Flesh + 1 Sugar Cane -> 2 Almond Water Rations (Apples)
      ctx.addShapelessRecipe(
         Items.APPLE,
         2,
         Items.ROTTEN_FLESH,
         Items.ROTTEN_FLESH,
         Blocks.SUGAR_CANE
      );
   }

   @Override
   public Gen createWorldGen(int dim, long seed, WorldSettings settings) {
      if (this.worldGenEnabled && dim == Dim.OVERWORLD && (settings == null || !settings.isMinigame())) {
         return new BackroomsGen(seed, settings);
      }
      return null;
   }

   @Override
   public String hudWatermark() {
      return null; // Custom VHS Camcorder HUD overlay is drawn in drawHudOverlay(g)
   }

   @Override
   public boolean onCommand(Game g, String cmd, String[] args) {
      if (cmd == null) {
         return false;
      }
      String c = cmd.toLowerCase();
      if (c.equals("/backrooms")) {
         active = true;
         applyHorrorSettings(g);
         if (args != null && args.length >= 1) {
            String sub = args[0].toLowerCase();
            if (sub.equals("leave") || sub.equals("exit") || sub.equals("escape")) {
               leaveBackrooms(g);
               return true;
            }
            if (sub.equals("worldgen")) {
               this.worldGenEnabled = !this.worldGenEnabled;
               g.say("§e[Backrooms] §7Infinite Backrooms worldgen for new worlds: " + (this.worldGenEnabled ? "§aON" : "§cOFF"));
               return true;
            }
         }
         if (g.world != null && g.world.gen instanceof BackroomsGen) {
            ensurePlayerInsideBackrooms(g);
            spawnStalkerNearPlayer(g, true);
            g.addShake(0.55F);
            g.toast("Level 0 — The Backrooms", "60 Hz fluorescent hum... Something is in the halls.");
         } else if (this.inPocketBackrooms) {
            leaveBackrooms(g);
         } else {
            enterBackrooms(g);
         }
         return true;
      }

      if (c.equals("/stalker")) {
         active = true;
         applyHorrorSettings(g);
         if (args != null && args.length >= 1) {
            String sub = args[0].toLowerCase();
            if (sub.equals("calm") || sub.equals("despawn") || sub.equals("off")) {
               restoreFlickeredLights(g);
               despawnStalker(g);
               sanity = 100;
               panicLevel = 0.05F;
               g.say("§a[Stalker] §7The footsteps fade away into the yellow halls.");
               return true;
            }
            if (sub.equals("chase") || sub.equals("hunt") || sub.equals("attack")) {
               if (stalkerMob == null || stalkerMob.dead) {
                  spawnStalkerNearPlayer(g, false);
               }
               stalkerPhase = PHASE_CHASE;
               chaseTicks = 240;
               panicLevel = 1.0F;
               triggerLightBlackout(g, 80);
               g.addShake(0.9F);
               g.say("§4[Stalker] §c§lIT FOUND YOU. RUN!");
               return true;
            }
            if (sub.equals("blackout") || sub.equals("flicker")) {
               triggerLightBlackout(g, 80);
               g.addShake(0.45F);
               g.say("§8[Stalker] §7The fluorescent tubes overhead buzz loudly and short out...");
               return true;
            }
         }
         spawnStalkerNearPlayer(g, true);
         return true;
      }

      return false;
   }

   private void enterBackrooms(Game g) {
      if (g == null || g.world == null || g.player == null) {
         return;
      }
      this.returnX = g.player.x;
      this.returnY = g.player.y;
      this.returnZ = g.player.z;
      this.inPocketBackrooms = true;

      int cx = (int)Math.floor(g.player.x);
      int cz = (int)Math.floor(g.player.z);
      buildPocketBackrooms(g.world, cx, POCKET_Y, cz, POCKET_RADIUS);

      g.player.setPos(cx + 0.5, POCKET_Y + 1.05, cz + 0.5);
      g.player.vx = 0.0;
      g.player.vy = 0.0;
      g.player.vz = 0.0;
      sanity = 100;
      stalkerCooldown = 25;
      g.addShake(0.6F);
      g.toast("Noclipped out of reality!", "Welcome to Level 0. Type /backrooms leave to escape.");
      g.say("§e[Backrooms] §7You noclipped into Level 0! Type §f/backrooms leave§7 to return.");
   }

   private void leaveBackrooms(Game g) {
      if (g == null || g.player == null) {
         return;
      }
      restoreFlickeredLights(g);
      despawnStalker(g);
      this.inPocketBackrooms = false;
      if (this.returnY > 0.0) {
         g.player.setPos(this.returnX, this.returnY + 0.5, this.returnZ);
      } else {
         g.player.setPos(0.5, BackroomsGen.FLOOR_Y + 1.0, 0.5);
      }
      g.player.vx = 0.0;
      g.player.vy = 0.0;
      g.player.vz = 0.0;
      sanity = 100;
      panicLevel = 0.05F;
      g.say("§a[Backrooms] §7You clipped back into reality.");
   }

   /** Builds a 41x41 enclosed Level 0 Backrooms complex around (cx, baseY, cz). */
   private void buildPocketBackrooms(World w, int cx, int baseY, int cz, int radius) {
      int ceilY = baseY + 6;
      for (int dx = -radius; dx <= radius; dx++) {
         for (int dz = -radius; dz <= radius; dz++) {
            int wx = cx + dx;
            int wz = cz + dz;
            boolean outerBoundary = Math.abs(dx) == radius || Math.abs(dz) == radius;

            w.setBlock(wx, baseY - 1, wz, Blocks.BEDROCK);
            w.setBlock(wx, baseY, wz, Blocks.WOOL0 + 4);
            for (int y = baseY + 1; y < ceilY; y++) {
               w.setBlock(wx, y, wz, Blocks.AIR);
            }

            boolean light = (Math.floorMod(wx, 6) == 2 || Math.floorMod(wx, 6) == 3) && Math.floorMod(wz, 6) == 3;
            w.setBlock(wx, ceilY, wz, light ? Blocks.GLOWSTONE : Blocks.SANDSTONE);
            w.setBlock(wx, ceilY + 1, wz, Blocks.BEDROCK);

            if (outerBoundary) {
               for (int y = baseY + 1; y < ceilY; y++) {
                  w.setBlock(wx, y, wz, Blocks.BEDROCK);
               }
               continue;
            }

            if (Math.abs(dx) <= 3 && Math.abs(dz) <= 3) {
               continue;
            }

            int rx = Math.floorMod(dx, 7);
            int rz = Math.floorMod(dz, 7);
            long h = (wx * 73428767L) ^ (wz * 912931L) ^ w.seed;
            boolean wall = false;
            boolean door = false;
            if (rx == 0 && (h & 3L) != 0L) {
               wall = true;
               if (rz == 3 || rz == 4) {
                  door = true;
               }
            }
            if (rz == 0 && ((h >>> 2) & 3L) != 0L) {
               wall = true;
               if (rx == 3 || rx == 4) {
                  door = true;
               }
            }
            if (rx == 0 && rz == 0) {
               wall = true;
               door = false;
            }

            if (wall) {
               for (int y = baseY + 1; y < ceilY; y++) {
                  if (door && y <= baseY + 4) {
                     w.setBlock(wx, y, wz, Blocks.AIR);
                  } else {
                     w.setBlock(wx, y, wz, y == baseY + 1 ? Blocks.PLANKS : Blocks.SANDSTONE);
                  }
               }
            }
         }
      }
      w.setBlock(cx + 3, baseY + 1, cz, Blocks.CHEST_LOOT);
   }

   @Override
   public void onGameTick(Game g) {
      if (g == null || g.world == null || g.player == null || g.mobs == null) {
         return;
      }
      this.lastGame = g;
      active = true;

      if (!this.savedSettingsValid) {
         applyHorrorSettings(g);
      }

      // Always keep the player inside the Backrooms (never stranded on top of the bedrock roof)
      if (g.world.gen instanceof BackroomsGen) {
         ensurePlayerInsideBackrooms(g);
      }

      if (jumpscareFlash > 0.01F) {
         jumpscareFlash *= 0.86F;
      } else {
         jumpscareFlash = 0.0F;
      }

      if (blackoutTicks > 0) {
         blackoutTicks--;
         if (blackoutTicks == 0) {
            restoreFlickeredLights(g);
         }
      }

      boolean inBackroomsNow = this.inPocketBackrooms || (g.world.gen instanceof BackroomsGen);

      // Clean up dead reference
      if (stalkerMob != null && (stalkerMob.dead || !g.mobs.list.contains(stalkerMob))) {
         stalkerMob = null;
         stalkerPhase = PHASE_STALK;
         stalkerCooldown = 100;
      }

      // Automatically spawn the Stalker inside the Backrooms
      if (stalkerMob == null) {
         if (stalkerCooldown > 0) {
            stalkerCooldown--;
         } else if (inBackroomsNow || this.rng.nextInt(160) == 0) {
            spawnStalkerNearPlayer(g, false);
            stalkerCooldown = 140;
         }
      } else {
         tickStalkerAtmosphere(g);
      }
   }

   /**
    * Called directly from {@link Mobs312} during {@code Mobs.tick()} so the Stalker uses the engine's
    * native movement, collision, stride animation, and smooth 60 FPS pose interpolation.
    */
   public static void stepStalkerIntent(
      Mobs ms,
      Mobs.Mob m,
      World w,
      Player p,
      double dx,
      double dz,
      double dy,
      double d2,
      double d3,
      Mobs312.Intent in
   ) {
      stalkerMob = m;
      m.health = Math.max(m.health, 80);
      m.burn = 0;
      if (m.cd > 0) {
         m.cd--;
      }

      boolean watched = isPlayerLookingAtStalker(ms, w, p, m, d3);
      m.dirYaw = Mobs.angleTo(dx, dz);
      in.headYaw = m.dirYaw;
      in.headPitch = (float)Math.toDegrees(-Math.atan2(dy, Math.max(0.01, d2)));

      if (stalkerPhase == PHASE_CHASE) {
         m.angry = 400;
         m.hasTarget = true;
         in.moving = d2 > 1.15;
         in.speed = 2.05; // Terrifyingly fast sprint down corridors
         if (d2 < 2.15 && Math.abs(dy) < 2.8 && m.cd <= 0) {
            m.cd = 18;
            m.attackAnim = 10;
            jumpscareFlash = 1.0F;
            ms.g.addShake(1.15F);
            ms.g.damagePlayer(6, m.x, m.z);
         }
      } else if (watched && d3 < 30.0) {
         // Weeping-Angel freeze while stared at: locks eyes with you and twitches its neck
         stalkerPhase = PHASE_FROZEN;
         m.angry = 200;
         in.moving = false;
         in.speed = 0.0;
      } else {
         // Corner-stalking phase: creeps closer whenever you aren't looking directly at it
         stalkerPhase = PHASE_STALK;
         m.angry = 0;
         if (d2 > 5.0) {
            in.moving = true;
            in.speed = d2 > 16.0 ? 1.45 : 0.95;
         } else {
            in.moving = false;
            in.speed = 0.0;
         }
      }
   }

   private static boolean isPlayerLookingAtStalker(Mobs ms, World w, Player p, Mobs.Mob m, double d3) {
      if (d3 < 0.8) {
         return true;
      }
      double ex = p.x;
      double ey = p.y + p.eye;
      double ez = p.z;
      double yaw = Math.toRadians(p.yaw);
      double pitch = Math.toRadians(p.pitch);
      double lx = -Math.sin(yaw) * Math.cos(pitch);
      double ly = -Math.sin(pitch);
      double lz = Math.cos(yaw) * Math.cos(pitch);

      double hx = m.x - ex;
      double hy = (m.y + 2.3) - ey;
      double hz = m.z - ez;
      double dist = Math.sqrt(hx * hx + hy * hy + hz * hz);
      double dot = (lx * hx + ly * hy + lz * hz) / Math.max(0.001, dist);
      if (dot < 0.72) {
         return false;
      }
      return ms.hasLOS(w, ex, ey, ez, m.x, m.y + 2.3, m.z);
   }

   private void tickStalkerAtmosphere(Game g) {
      Player p = g.player;
      double dx = p.x - stalkerMob.x;
      double dy = p.y - stalkerMob.y;
      double dz = p.z - stalkerMob.z;
      double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
      lastDistToPlayer = (float)dist;

      // Relocate closer if the player outran the Stalker by > 38 blocks
      if (dist > 38.0) {
         relocateStalkerBehindPlayer(g, 13.0, 20.0);
         return;
      }

      boolean watched = isPlayerLookingAtStalker(g.mobs, g.world, p, stalkerMob, dist);

      // Update continuous panic level (0..1) and heart rate BPM for VHS shader & HUD
      float targetPanic = 0.08F;
      if (stalkerPhase == PHASE_CHASE) {
         targetPanic = 1.0F;
      } else if (watched) {
         targetPanic = Math.min(1.0F, 0.55F + (float)stareTicks / 40.0F);
      } else if (dist < 18.0) {
         targetPanic = Math.max(0.15F, (float)((18.0 - dist) / 18.0) * 0.75F);
      }
      if (blackoutTicks > 0) {
         targetPanic = Math.max(targetPanic, 0.82F);
      }
      panicLevel += (targetPanic - panicLevel) * 0.12F;
      heartbeatBpm = (int)(70 + panicLevel * 116.0F + (g.tickCount % 7 == 0 ? 3 : 0));

      // Emit dark shadow/crimson static particles around the Stalker
      if (g.rend != null && (g.tickCount & 1) == 0) {
         g.portalParticle(
            stalkerMob.x + (this.rng.nextDouble() - 0.5) * 0.9,
            stalkerMob.y + 0.4 + this.rng.nextDouble() * 2.7,
            stalkerMob.z + (this.rng.nextDouble() - 0.5) * 0.9,
            0.85F, 0.06F, 0.08F
         );
      }

      if (stalkerPhase == PHASE_CHASE) {
         chaseTicks--;
         if (dist < 12.0 && g.tickCount % 4 == 0) {
            g.addShake(Math.min(0.65F, (float)((14.0 - dist) * 0.05)));
         }
         if (g.tickCount % 30 == 0) {
            sanity = Math.max(0, sanity - 4);
         }
         // Break line of sight or survive the chase timer -> Stalker vanishes into the dark
         if (chaseTicks <= 0) {
            triggerLightBlackout(g, 45);
            relocateStalkerBehindPlayer(g, 15.0, 22.0);
            stalkerPhase = PHASE_STALK;
         }
         return;
      }

      if (watched && dist < 28.0) {
         stareTicks++;
         g.addShake(Math.min(0.45F, 0.06F + stareTicks * 0.01F));
         if (stareTicks % 8 == 0) {
            sanity = Math.max(0, sanity - 3);
         }
         if (stareTicks == 8) {
            g.toast("DON'T STARE AT IT", "Look away before the lights go out!");
         }
         if (stareTicks > 34) {
            stareTicks = 0;
            triggerLightBlackout(g, 70);
            jumpscareFlash = 0.85F;
            g.addShake(0.95F);
            if (this.rng.nextBoolean() || dist < 11.0) {
               stalkerPhase = PHASE_CHASE;
               chaseTicks = 180;
               g.say("§4[Stalker] §c§lTHE LIGHTS BURST. IT IS HUNTING YOU!");
            } else {
               relocateStalkerBehindPlayer(g, 5.5, 9.5);
               g.say("§8[Stalker] §cIt vanished in the blackout... check behind you.");
            }
         }
      } else {
         if (stareTicks > 0) {
            stareTicks--;
         }
         if (g.tickCount % 80 == 0 && sanity < 100) {
            sanity = Math.min(100, sanity + 1);
         }
         // Ambush if the player backs too close into the Stalker
         if (dist < 5.2) {
            stalkerPhase = PHASE_CHASE;
            chaseTicks = 160;
            triggerLightBlackout(g, 60);
            jumpscareFlash = 0.9F;
            g.addShake(0.85F);
            g.toast("IT'S RIGHT BEHIND YOU!", "RUN!");
         } else if (g.tickCount % 160 == 0 && this.rng.nextInt(3) == 0) {
            triggerLightBlackout(g, 35);
         }
      }
   }

   private void spawnStalkerNearPlayer(Game g, boolean announce) {
      if (g == null || g.world == null || g.player == null || g.mobs == null) {
         return;
      }
      despawnStalker(g);
      double[] pos = findStalkerSpot(g, 9.0, 17.0);
      stalkerMob = g.mobs.spawn(Mobs.ENDERMAN, pos[0], pos[1], pos[2]);
      if (stalkerMob != null) {
         stalkerMob.tag = STALKER_TAG;
         stalkerMob.health = 100;
         stalkerMob.scale = 1.0F;
         stalkerPhase = PHASE_STALK;
         stareTicks = 0;
         chaseTicks = 0;
         if (announce) {
            triggerLightBlackout(g, 40);
            g.addShake(0.45F);
            g.toast("Something entered the halls...", "A 3.3m silhouette is watching from the corner.");
            g.say("§4[Stalker] §7You hear wet, clicking footsteps echoing down the corridor...");
         }
      }
   }

   private void relocateStalkerBehindPlayer(Game g, double minDist, double maxDist) {
      if (stalkerMob == null || g == null) {
         return;
      }
      double[] pos = findStalkerSpot(g, minDist, maxDist);
      stalkerMob.setPos(pos[0], pos[1], pos[2]);
      stalkerMob.px = pos[0];
      stalkerMob.py = pos[1];
      stalkerMob.pz = pos[2];
      stalkerMob.vx = 0.0;
      stalkerMob.vy = 0.0;
      stalkerMob.vz = 0.0;
   }

   private double[] findStalkerSpot(Game g, double minDist, double maxDist) {
      Player p = g.player;
      World w = g.world;
      double yawRad = Math.toRadians(p.yaw);
      // Forward look is (-sin(yaw), +cos(yaw)), so behind the player is (+sin(yaw), -cos(yaw))
      double behindAngle = Math.atan2(-Math.cos(yawRad), Math.sin(yawRad));

      int targetFloorY = (w.gen instanceof BackroomsGen)
         ? BackroomsGen.FLOOR_Y
         : (this.inPocketBackrooms ? POCKET_Y : (int)Math.floor(p.y) - 1);

      for (int attempt = 0; attempt < 28; attempt++) {
         double spread = (this.rng.nextDouble() - 0.5) * Math.PI * 1.35;
         double ang = behindAngle + spread;
         double d = minDist + this.rng.nextDouble() * (maxDist - minDist);
         int bx = (int)Math.floor(p.x + Math.cos(ang) * d);
         int bz = (int)Math.floor(p.z + Math.sin(ang) * d);

         if ((w.gen instanceof BackroomsGen) || this.inPocketBackrooms) {
            if (w.getBlock(bx, targetFloorY + 1, bz) == Blocks.AIR
               && w.getBlock(bx, targetFloorY + 2, bz) == Blocks.AIR
               && w.getBlock(bx, targetFloorY + 3, bz) == Blocks.AIR) {
               return new double[]{bx + 0.5, targetFloorY + 1.0, bz + 0.5};
            }
         } else {
            for (int y = (int)Math.floor(p.y) + 6; y >= Math.max(1, (int)Math.floor(p.y) - 8); y--) {
               if (Blocks.solid[w.getBlock(bx, y, bz)]
                  && w.getBlock(bx, y + 1, bz) == Blocks.AIR
                  && w.getBlock(bx, y + 2, bz) == Blocks.AIR
                  && w.getBlock(bx, y + 3, bz) == Blocks.AIR) {
                  return new double[]{bx + 0.5, y + 1.0, bz + 0.5};
               }
            }
         }
      }
      double fallbackX = p.x + Math.sin(yawRad) * minDist;
      double fallbackZ = p.z - Math.cos(yawRad) * minDist;
      return new double[]{fallbackX, targetFloorY + 1.0, fallbackZ};
   }

   private void triggerLightBlackout(Game g, int durationTicks) {
      if (g == null || g.world == null || g.player == null) {
         return;
      }
      restoreFlickeredLights(g);
      int px = (int)Math.floor(g.player.x);
      int py = (int)Math.floor(g.player.y);
      int pz = (int)Math.floor(g.player.z);

      for (int dx = -12; dx <= 12 && this.flickeredCount < this.flickeredLightsX.length; dx++) {
         for (int dz = -12; dz <= 12 && this.flickeredCount < this.flickeredLightsX.length; dz++) {
            for (int dy = -2; dy <= 7 && this.flickeredCount < this.flickeredLightsX.length; dy++) {
               int wx = px + dx;
               int wy = py + dy;
               int wz = pz + dz;
               if (g.world.getBlock(wx, wy, wz) == Blocks.GLOWSTONE) {
                  this.flickeredLightsX[this.flickeredCount] = wx;
                  this.flickeredLightsY[this.flickeredCount] = wy;
                  this.flickeredLightsZ[this.flickeredCount] = wz;
                  this.flickeredCount++;
                  g.world.setBlock(wx, wy, wz, Blocks.GLASS);
               }
            }
         }
      }
      blackoutTicks = durationTicks;
   }

   private void restoreFlickeredLights(Game g) {
      if (g != null && g.world != null) {
         for (int i = 0; i < this.flickeredCount; i++) {
            int wx = this.flickeredLightsX[i];
            int wy = this.flickeredLightsY[i];
            int wz = this.flickeredLightsZ[i];
            if (g.world.getBlock(wx, wy, wz) == Blocks.GLASS) {
               g.world.setBlock(wx, wy, wz, Blocks.GLOWSTONE);
            }
         }
      }
      this.flickeredCount = 0;
      blackoutTicks = 0;
   }

   private void despawnStalker(Game g) {
      if (stalkerMob != null) {
         stalkerMob.dead = true;
         if (g != null && g.mobs != null) {
            g.mobs.list.remove(stalkerMob);
         }
         stalkerMob = null;
      }
      stalkerPhase = PHASE_STALK;
      stareTicks = 0;
      chaseTicks = 0;
   }

   // ====================================================================================================
   // CUSTOM 3D HORROR ENTITY RENDERER: "THE BACTERIA / WIRE-STALKER"
   // ====================================================================================================

   private static void stalkerBox(
      float x0,
      float y0,
      float z0,
      float x1,
      float y1,
      float z1,
      int rgb,
      float shadeScale
   ) {
      float r = ((rgb >> 16) & 0xFF) / 255.0F * shadeScale;
      float g = ((rgb >> 8) & 0xFF) / 255.0F * shadeScale;
      float b = (rgb & 0xFF) / 255.0F * shadeScale;

      GL11.glBegin(GL11.GL_QUADS);
      // Top (+Y)
      GL11.glColor3f(Math.min(1.0F, r * 1.12F), Math.min(1.0F, g * 1.12F), Math.min(1.0F, b * 1.12F));
      GL11.glVertex3f(x0, y1, z0);
      GL11.glVertex3f(x1, y1, z0);
      GL11.glVertex3f(x1, y1, z1);
      GL11.glVertex3f(x0, y1, z1);
      // Bottom (-Y)
      GL11.glColor3f(r * 0.55F, g * 0.55F, b * 0.55F);
      GL11.glVertex3f(x0, y0, z1);
      GL11.glVertex3f(x1, y0, z1);
      GL11.glVertex3f(x1, y0, z0);
      GL11.glVertex3f(x0, y0, z0);
      // Front (+Z)
      GL11.glColor3f(r, g, b);
      GL11.glVertex3f(x0, y0, z1);
      GL11.glVertex3f(x0, y1, z1);
      GL11.glVertex3f(x1, y1, z1);
      GL11.glVertex3f(x1, y0, z1);
      // Back (-Z)
      GL11.glColor3f(r * 0.82F, g * 0.82F, b * 0.82F);
      GL11.glVertex3f(x1, y0, z0);
      GL11.glVertex3f(x1, y1, z0);
      GL11.glVertex3f(x0, y1, z0);
      GL11.glVertex3f(x0, y0, z0);
      // Right (+X)
      GL11.glColor3f(r * 0.72F, g * 0.72F, b * 0.72F);
      GL11.glVertex3f(x1, y0, z1);
      GL11.glVertex3f(x1, y1, z1);
      GL11.glVertex3f(x1, y1, z0);
      GL11.glVertex3f(x1, y0, z0);
      // Left (-X)
      GL11.glColor3f(r * 0.72F, g * 0.72F, b * 0.72F);
      GL11.glVertex3f(x0, y0, z0);
      GL11.glVertex3f(x0, y1, z0);
      GL11.glVertex3f(x0, y1, z1);
      GL11.glVertex3f(x0, y0, z1);
      GL11.glEnd();
   }

   /**
    * Renders the custom 3.3-block-tall articulated 3D Backrooms Stalker Entity:
    * - Hunched skeletal ribcage & contorted spine
    * - Twitching crooked skull with unhinging toothed jaw
    * - Emissive blood-crimson & white pinprick eyes that glow through the fog
    * - 6 writhing dorsal tendrils/wires sprouting from its back
    * - Elongated multi-jointed arms with 3-pronged razor claws
    */
   public static void drawStalkerEntity(
      Mobs.Mob m,
      World w,
      Renderer rend,
      float pt,
      double x,
      double y,
      double z
   ) {
      float bodyYaw = MobRender.lerpAngle(m.pyaw, m.yaw, pt);
      float headYaw = MobRender.lerpAngle(m.pheadYaw, m.headYaw, pt) - bodyYaw;
      float headPitch = m.pheadPitch + (m.headPitch - m.pheadPitch) * pt;
      float walk = m.pwalk + (m.walk - m.pwalk) * pt;
      float walkAmp = m.pwalkAmp + (m.walkAmp - m.pwalkAmp) * pt;
      float time = m.age + pt;
      float bright = Math.max(0.35F, rend.brightnessAt(w, x, y + 1.5, z));

      int VoidBlack = 0x0A090E;
      int BoneDark = 0x1A161E;
      int CrimsonFlesh = 0x4A090E;
      int ToothPale = 0xDFD6BE;
      int ClawCrimson = 0x8A0D14;

      if (m.hurtTime > 0) {
         VoidBlack = 0x5A0E12;
         BoneDark = 0x7A181E;
      }

      boolean texWasOn = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
      GL11.glDisable(GL11.GL_TEXTURE_2D);
      GL11.glPushMatrix();
      GL11.glTranslated(x - rend.camX, y - rend.camY, z - rend.camZ);
      GL11.glRotatef(-bodyYaw, 0.0F, 1.0F, 0.0F);

      // Slight eerie hovering / convulsing bob
      float twitch = (stalkerPhase == PHASE_FROZEN || stalkerPhase == PHASE_CHASE)
         ? (float)Math.sin(time * 2.9F) * 1.8F + (float)Math.sin(time * 17.3F) * 2.6F
         : (float)Math.sin(time * 0.15F) * 0.8F;

      // ---- 1. SPIDERY DIGITIGRADE LEGS (0.0m .. 1.45m) ----
      float legSwing = (float)Math.sin(walk * 0.7F) * 38.0F * walkAmp;
      for (int side = -1; side <= 1; side += 2) {
         float sSwing = side * legSwing;
         GL11.glPushMatrix();
         GL11.glTranslatef(side * 0.19F, 1.45F, 0.0F);
         GL11.glRotatef(sSwing - 8.0F, 1.0F, 0.0F, 0.0F);
         GL11.glRotatef(side * 3.0F, 0.0F, 0.0F, 1.0F);
         // Upper femur
         stalkerBox(-0.06F, -0.75F, -0.06F, 0.06F, 0.0F, 0.06F, VoidBlack, bright);
         // Knee joint & lower shin angled backward
         GL11.glTranslatef(0.0F, -0.72F, 0.0F);
         GL11.glRotatef(18.0F - sSwing * 0.35F, 1.0F, 0.0F, 0.0F);
         stalkerBox(-0.048F, -0.76F, -0.048F, 0.048F, 0.02F, 0.048F, BoneDark, bright);
         // Spidery taloned foot
         stalkerBox(-0.07F, -0.76F, -0.05F, 0.07F, -0.70F, 0.22F, VoidBlack, bright);
         GL11.glPopMatrix();
      }

      // ---- 2. CONTORTED SKELETAL TORSO, PELVIS & RIBCAGE (1.40m .. 2.55m) ----
      GL11.glPushMatrix();
      GL11.glTranslatef(0.0F, 1.42F, 0.0F);
      float hunch = stalkerPhase == PHASE_CHASE ? 22.0F : 12.0F;
      GL11.glRotatef(hunch + (float)Math.sin(time * 0.12F) * 2.5F, 1.0F, 0.0F, 0.0F);
      GL11.glRotatef(twitch * 0.4F, 0.0F, 0.0F, 1.0F);

      // Pelvis & central crooked spine column
      stalkerBox(-0.22F, -0.05F, -0.11F, 0.22F, 0.14F, 0.11F, VoidBlack, bright);
      stalkerBox(-0.07F, 0.10F, -0.09F, 0.07F, 1.15F, 0.07F, VoidBlack, bright);
      // Glowing crimson inner heart/core inside the ribcage
      stalkerBox(-0.11F, 0.45F, -0.05F, 0.11F, 0.88F, 0.09F, CrimsonFlesh, 1.0F);

      // 5 pairs of skeletal ribs wrapping around the chest cavity
      float breath = (float)Math.sin(time * 0.22F) * 0.015F;
      for (int r = 0; r < 5; r++) {
         float ry = 0.28F + r * 0.16F;
         float rw = 0.23F + (r >= 2 ? 0.05F : 0.0F) + breath;
         stalkerBox(-rw, ry, -0.13F, rw, ry + 0.065F, 0.14F, BoneDark, bright);
      }

      // Broad, jagged shoulder girdle
      stalkerBox(-0.38F, 1.02F, -0.14F, 0.38F, 1.16F, 0.14F, VoidBlack, bright);

      // ---- 3. SIX WRITHING DORSAL TENDRILS (sprouting from shoulder blades) ----
      for (int t = 0; t < 6; t++) {
         int side = (t & 1) == 0 ? -1 : 1;
         int row = t / 2;
         GL11.glPushMatrix();
         GL11.glTranslatef(side * (0.14F + row * 0.06F), 0.55F + row * 0.22F, -0.11F);
         float wave1 = (float)Math.sin(time * 0.24F + t * 1.3F) * 28.0F;
         float wave2 = (float)Math.cos(time * 0.19F + t * 0.9F) * 24.0F;
         GL11.glRotatef(side * (28.0F + row * 12.0F) + wave1, 0.0F, 1.0F, 0.0F);
         GL11.glRotatef(-20.0F + wave2, 1.0F, 0.0F, 0.0F);
         stalkerBox(-0.03F, -0.03F, -0.48F, 0.03F, 0.03F, 0.0F, VoidBlack, bright);
         GL11.glTranslatef(0.0F, 0.0F, -0.45F);
         GL11.glRotatef(wave1 * 0.8F, 0.0F, 1.0F, 0.0F);
         GL11.glRotatef(wave2 * 0.8F, 1.0F, 0.0F, 0.0F);
         stalkerBox(-0.02F, -0.02F, -0.46F, 0.02F, 0.02F, 0.0F, ClawCrimson, 0.95F);
         GL11.glPopMatrix();
      }

      // ---- 4. ELONGATED MULTI-JOINTED ARMS & 3-PRONGED RAZOR CLAWS ----
      float armSwing = (float)Math.cos(walk * 0.7F) * 32.0F * walkAmp;
      for (int side = -1; side <= 1; side += 2) {
         GL11.glPushMatrix();
         GL11.glTranslatef(side * 0.36F, 1.08F, 0.02F);
         float reachPitch;
         if (stalkerPhase == PHASE_CHASE) {
            // Arms raised forward reaching for the player's throat!
            reachPitch = 78.0F + side * armSwing * 0.6F + (float)Math.sin(time * 0.9F + side) * 12.0F;
            if (m.attackAnim > 0) {
               reachPitch += (float)Math.sin((10 - m.attackAnim + pt) * 0.5F) * 45.0F;
            }
         } else if (stalkerPhase == PHASE_FROZEN) {
            reachPitch = 28.0F + twitch * 1.5F;
         } else {
            reachPitch = 10.0F - side * armSwing;
         }
         GL11.glRotatef(reachPitch, 1.0F, 0.0F, 0.0F);
         GL11.glRotatef(side * (8.0F + (float)Math.sin(time * 0.14F) * 4.0F), 0.0F, 0.0F, 1.0F);

         // Gaunt upper arm
         stalkerBox(-0.055F, -0.78F, -0.055F, 0.055F, 0.04F, 0.055F, VoidBlack, bright);
         // Elbow joint & long forearm
         GL11.glTranslatef(0.0F, -0.75F, 0.0F);
         GL11.glRotatef(26.0F + (stalkerPhase == PHASE_CHASE ? 20.0F : 0.0F), 1.0F, 0.0F, 0.0F);
         stalkerBox(-0.045F, -0.82F, -0.045F, 0.045F, 0.02F, 0.045F, BoneDark, bright);

         // 3 long blood-stained razor claws at the end of each hand
         for (int c = -1; c <= 1; c++) {
            stalkerBox(c * 0.035F - 0.012F, -1.08F, -0.02F, c * 0.035F + 0.012F, -0.80F, 0.04F, ClawCrimson, 0.95F);
         }
         GL11.glPopMatrix();
      }

      // ---- 5. TWITCHING CROOKED NECK, SKULL, UNHINGING JAW & GLOWING EYES ----
      GL11.glPushMatrix();
      GL11.glTranslatef(0.0F, 1.15F, 0.04F);
      // Long crooked neck
      stalkerBox(-0.06F, -0.02F, -0.06F, 0.06F, 0.24F, 0.06F, BoneDark, bright);
      GL11.glTranslatef(0.0F, 0.22F, 0.02F);

      GL11.glRotatef(headYaw, 0.0F, 1.0F, 0.0F);
      GL11.glRotatef(headPitch, 1.0F, 0.0F, 0.0F);

      // Iconic horror head tilt: snaps sideways at 34 degrees when stared at or chasing!
      float headTilt;
      if (stalkerPhase == PHASE_FROZEN) {
         headTilt = 36.0F + twitch * 3.2F;
      } else if (stalkerPhase == PHASE_CHASE) {
         headTilt = (float)Math.sin(time * 1.7F) * 26.0F + twitch * 2.5F;
      } else {
         headTilt = 14.0F * (float)Math.sin(time * 0.09F);
      }
      GL11.glRotatef(headTilt, 0.0F, 0.0F, 1.0F);

      // Elongated gaunt upper skull
      stalkerBox(-0.21F, 0.04F, -0.22F, 0.21F, 0.52F, 0.22F, VoidBlack, bright);
      // Brow ridge & cheekbones
      stalkerBox(-0.23F, 0.26F, 0.16F, 0.23F, 0.35F, 0.25F, BoneDark, bright);
      // Horn-like wire protrusions rising from the crown of the skull
      stalkerBox(-0.18F, 0.50F, -0.08F, -0.11F, 0.78F, -0.01F, VoidBlack, bright);
      stalkerBox(0.11F, 0.50F, -0.08F, 0.18F, 0.84F, -0.01F, VoidBlack, bright);

      // Upper row of jagged teeth
      for (int t = -3; t <= 3; t += 2) {
         stalkerBox(t * 0.045F - 0.018F, -0.03F, 0.17F, t * 0.045F + 0.018F, 0.05F, 0.21F, ToothPale, 0.95F);
      }

      // Unhinging Lower Jaw (drops wide open when watched or chasing!)
      float jawDrop = stalkerPhase == PHASE_CHASE
         ? 0.26F + 0.06F * (float)Math.sin(time * 1.4F)
         : (stalkerPhase == PHASE_FROZEN ? 0.18F : 0.04F);
      GL11.glPushMatrix();
      GL11.glTranslatef(0.0F, 0.04F - jawDrop, 0.0F);
      GL11.glRotatef(jawDrop * 85.0F, 1.0F, 0.0F, 0.0F);
      stalkerBox(-0.19F, -0.14F, -0.18F, 0.19F, 0.0F, 0.23F, BoneDark, bright);
      // Lower jagged teeth
      for (int t = -3; t <= 3; t += 2) {
         stalkerBox(t * 0.045F - 0.018F, 0.0F, 0.17F, t * 0.045F + 0.018F, 0.075F, 0.21F, ToothPale, 0.95F);
      }
      GL11.glPopMatrix();

      // ---- 6. EMISSIVE GLOWING EYES & CRIMSON MAW (pierces fog & darkness!) ----
      boolean fogWasOn = GL11.glIsEnabled(GL11.GL_FOG);
      GL11.glDisable(GL11.GL_FOG);

      int EyeCrimson = 0xFF1A1E;
      int EyeCoreWhite = 0xFFF8EC;
      stalkerBox(-0.155F, 0.20F, 0.215F, -0.055F, 0.275F, 0.245F, EyeCrimson, 1.0F);
      stalkerBox(0.055F, 0.20F, 0.215F, 0.155F, 0.275F, 0.245F, EyeCrimson, 1.0F);
      // White-hot pinprick pupils
      stalkerBox(-0.118F, 0.222F, 0.232F, -0.088F, 0.252F, 0.256F, EyeCoreWhite, 1.0F);
      stalkerBox(0.088F, 0.222F, 0.232F, 0.118F, 0.252F, 0.256F, EyeCoreWhite, 1.0F);

      // Glowing crimson throat inside the unhinged mouth when hunting
      if (jawDrop > 0.08F) {
         stalkerBox(-0.14F, 0.04F - jawDrop * 0.7F, 0.12F, 0.14F, 0.06F, 0.19F, 0xD80E16, 1.0F);
      }

      if (fogWasOn) {
         GL11.glEnable(GL11.GL_FOG);
      }

      GL11.glPopMatrix(); // End head
      GL11.glPopMatrix(); // End torso
      GL11.glPopMatrix(); // End entity

      if (texWasOn) {
         GL11.glEnable(GL11.GL_TEXTURE_2D);
      }
   }

   // ====================================================================================================
   // FOUND-FOOTAGE VHS CAMCORDER HUD OVERLAY
   // ====================================================================================================

   public static void drawHudOverlay(Game g) {
      if (!active || g == null || g.gui == null || g.debug) {
         return;
      }
      Gui gui = g.gui;
      float w = gui.w;
      float h = gui.h;

      // 1. VHS Static Interference Bars & Crimson Jumpscare Flash
      if (panicLevel > 0.18F || jumpscareFlash > 0.02F) {
         int barCount = (int)(2 + panicLevel * 7.0F);
         for (int i = 0; i < barCount; i++) {
            float by = (float)((g.tickCount * 19 + i * 73) % (int)Math.max(1.0F, h));
            float bh = 2.0F + (i % 3) * 2.5F;
            int alpha = Math.min(140, (int)(panicLevel * 95.0F));
            int col = (alpha << 24) | 0x00E8DFB0;
            gui.rect(0.0F, by, w, bh, col);
         }
         if (jumpscareFlash > 0.02F) {
            int flashA = Math.min(170, (int)(jumpscareFlash * 170.0F));
            gui.rect(0.0F, 0.0F, w, h, (flashA << 24) | 0x008A080C);
         }
      }

      // 2. Camcorder Viewfinder Frame Brackets
      int frameCol = 0x88E6DFB8;
      float m = 10.0F;
      float len = 18.0F;
      gui.rect(m, m, len, 1.5F, frameCol);
      gui.rect(m, m, 1.5F, len, frameCol);
      gui.rect(w - m - len, m, len, 1.5F, frameCol);
      gui.rect(w - m - 1.5F, m, 1.5F, len, frameCol);
      gui.rect(m, h - m - 1.5F, len, 1.5F, frameCol);
      gui.rect(m, h - m - len, 1.5F, len, frameCol);
      gui.rect(w - m - len, h - m - 1.5F, len, 1.5F, frameCol);
      gui.rect(w - m - 1.5F, h - m - len, 1.5F, len, frameCol);

      // 3. Top-Left Tape Header & Current Backrooms Zone
      String zoneName = "LEVEL 0 // THE LOBBY";
      if (g.world != null && g.player != null && g.world.gen instanceof BackroomsGen) {
         int z = BackroomsGen.zoneAt(g.world.seed, (int)Math.floor(g.player.x), (int)Math.floor(g.player.z));
         if (z == BackroomsGen.ZONE_PILLARS) {
            zoneName = "LEVEL 0 // PILLAR HALLS";
         } else if (z == BackroomsGen.ZONE_MAINTENANCE) {
            zoneName = "SECTOR 2 // MAINTENANCE TUNNELS";
         } else if (z == BackroomsGen.ZONE_RED_RUN) {
            zoneName = "§cLEVEL ! // RUN FOR YOUR LIFE";
         } else if (z == BackroomsGen.ZONE_POOLROOMS) {
            zoneName = "§bLEVEL 37 // THE POOLROOMS";
         } else if (z == BackroomsGen.ZONE_BLACKOUT) {
            zoneName = "§8SECTOR 0-B // BLACKOUT ZONE";
         }
      }
      boolean recBlink = ((g.tickCount / 12) & 1) == 0;
      String recStr = (recBlink ? "§c● REC " : "§8○ REC ") + "§e" + zoneName;
      gui.text(recStr, 16.0F, 15.0F, 0xFFFFFFFF, true, 1.0F);

      // 4. Sanity & Heart Rate Monitor
      String sanCol = sanity > 60 ? "§a" : (sanity > 30 ? "§e" : "§c");
      String bpmCol = heartbeatBpm > 135 ? "§c" : (heartbeatBpm > 100 ? "§e" : "§f");
      String status = "§7SANITY: " + sanCol + sanity + "%  §8|  §7BPM: " + bpmCol + heartbeatBpm + " ♥";
      gui.text(status, 16.0F, 27.0F, 0xFFFFFFFF, true, 1.0F);

      // 5. Entity Proximity & Phase Warning
      if (stalkerMob != null && !stalkerMob.dead) {
         String entityAlert = null;
         if (stalkerPhase == PHASE_CHASE) {
            entityAlert = "§4§l! ENTITY HUNTING (" + (int)lastDistToPlayer + "m) — RUN !";
         } else if (stalkerPhase == PHASE_FROZEN) {
            entityAlert = "§c! VISUAL CONTACT — DO NOT STARE !";
         } else if (blackoutTicks > 0) {
            entityAlert = "§8[POWER FAILURE — FLUORESCENT GRID OFFLINE]";
         } else if (lastDistToPlayer < 16.0F) {
            entityAlert = "§6[AUDIO ANOMALY DETECTED: " + (int)lastDistToPlayer + "m]";
         }
         if (entityAlert != null) {
            gui.centered(entityAlert, w / 2.0F, 18.0F, 0xFFFFFFFF, true, 1.0F);
         }
      }

      // 6. Top-Right VHS Tape Timecode
      int totalSec = g.tickCount / 20;
      int mm = (totalSec / 60) % 60;
      int ss = totalSec % 60;
      int ff = (g.tickCount % 20) * 3;
      String tc = String.format("§7TAPE 1996-08  %02d:%02d:%02d", mm, ss, ff);
      gui.text(tc, w - Gui.textWidth(Gui.stripCodes(tc)) - 18.0F, 15.0F, 0xFFFFFFFF, true, 1.0F);
   }
}
