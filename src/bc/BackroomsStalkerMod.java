package bc;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * External JAR mod ({@code mods/backrooms-stalker.jar}) implementing the Level 0 Backrooms
 * dimension, pocket labyrinth builder ({@code /backrooms}), fluorescent light blackouts,
 * and the corner-stalking Stalker entity ({@code /stalker}).
 */
public final class BackroomsStalkerMod implements BlockcraftMod {
   public static final int POCKET_FLOOR_Y = 200;
   public static final int POCKET_CEIL_Y = 206;

   private boolean worldGenEnabled = true;
   private boolean inPocketBackrooms = false;
   private double returnX = 0.5;
   private double returnY = 70.0;
   private double returnZ = 0.5;
   private boolean hasReturnPos = false;

   private Mobs.Mob stalkerMob;
   private int sanity = 100;
   private int stareTicks = 0;
   private int blackoutTicks = 0;
   private long lastWhisperTick = -1000L;
   private final List<int[]> flickeredLights = new ArrayList<>();

   private static final String[] WHISPERS = new String[]{
      "§8§o*The 60 Hz fluorescent hum buzzes inside your skull...*",
      "§7§o*Wet footsteps echo down the yellow corridor behind you...*",
      "§c§o*Something tall just ducked behind the corner.*",
      "§8§o*The damp carpet squelches underfoot. You have been here before.*",
      "§4§o*DON'T TURN AROUND.*"
   };

   @Override
   public String id() {
      return "backrooms-stalker";
   }

   @Override
   public String name() {
      return "The Backrooms & Stalker (Level 0)";
   }

   @Override
   public String version() {
      return "1.2.0";
   }

   @Override
   public String author() {
      return "Backrooms Team";
   }

   @Override
   public String description() {
      return "Endless Level 0 yellow wallpaper labyrinth, buzzing fluorescent blackouts, /backrooms noclip, and the Stalker entity (/stalker)!";
   }

   @Override
   public boolean totalConversionInfdev() {
      // Pre-installed in mods/backrooms-stalker.jar; starts disabled until toggled on via Mods screen, Blockcraft-mods.bat, /backrooms, or /stalker
      return true;
   }

   @Override
   public String hudWatermark() {
      String state = (stalkerMob != null && !stalkerMob.dead) ? "STALKER NEAR" : "HUM 60Hz";
      return "Backrooms Level 0 [" + state + " | Sanity " + sanity + "%]";
   }

   @Override
   public void onInit(ModLoader.Context ctx) {
      // Almond Water Ration: Water Bucket + Apple -> 2 Bread
      ctx.addShapelessRecipe(Items.BREAD, 2, Items.WATER_BUCKET, Items.APPLE);
      // Mono-Yellow Wallpaper: 2 Sand + 2 Planks -> 4 Sandstone
      ctx.addShapelessRecipe(Blocks.SANDSTONE, 4, Blocks.SAND, Blocks.SAND, Blocks.PLANKS, Blocks.PLANKS);
      // Fluorescent Ceiling Fixture: 1 Glass + 2 Torches -> 1 Glowstone
      ctx.addShapelessRecipe(Blocks.GLOWSTONE, 1, Blocks.GLASS, Blocks.TORCH, Blocks.TORCH);
   }

   @Override
   public void onEnable(Game g) {
      sanity = 100;
      if (g != null && g.world != null) {
         g.chatMsg("§e[Backrooms Stalker] §7Enabled! Type §f/backrooms§7 to noclip into Level 0, or §f/stalker§7 to summon the Stalker.");
      }
   }

   @Override
   public void onDisable(Game g) {
      if (g != null && g.world != null) {
         restoreFlickeredLights(g);
      }
      if (stalkerMob != null) {
         stalkerMob.dead = true;
         stalkerMob = null;
      }
   }

   @Override
   public Gen createWorldGen(int dim, long seed, WorldSettings settings) {
      if (worldGenEnabled && dim == Dim.OVERWORLD && (settings == null || (!settings.isVoidMode() && settings.type != WorldSettings.FLAT))) {
         return new BackroomsGen(seed, settings != null ? settings : new WorldSettings());
      }
      return null;
   }

   @Override
   public void onGameTick(Game g) {
      if (g == null || g.world == null || g.player == null) {
         return;
      }
      long tick = g.world.tickCount;

      // Restore flickered fluorescent lights when blackout timer expires
      if (blackoutTicks > 0) {
         blackoutTicks--;
         if (blackoutTicks == 0) {
            restoreFlickeredLights(g);
         }
      }

      // Update sanity every 4 seconds based on light level and Stalker proximity
      if (tick % 80L == 0L) {
         int px = (int)Math.floor(g.player.x);
         int py = (int)Math.floor(g.player.y);
         int pz = (int)Math.floor(g.player.z);
         int light = Math.max(g.world.getBlk(px, py, pz), g.world.getSky(px, py, pz));
         if (light < 6 || (stalkerMob != null && !stalkerMob.dead)) {
            sanity = Math.max(5, sanity - 2);
         } else {
            sanity = Math.min(100, sanity + 3);
         }
      }

      // Maintain and tick the Stalker entity
      if (stalkerMob != null && stalkerMob.dead) {
         stalkerMob = null;
      }

      // Auto-spawn Stalker inside the Backrooms if not currently present
      boolean insideBackrooms = inPocketBackrooms || (g.world.gen instanceof BackroomsGen);
      if (insideBackrooms && stalkerMob == null && tick % 200L == 100L && g.mobs != null) {
         spawnStalkerNearPlayer(g, 15.0);
      }

      if (stalkerMob != null && !stalkerMob.dead) {
         tickStalkerAi(g, tick);
      }
   }

   private void tickStalkerAi(Game g, long tick) {
      Player p = g.player;
      double dx = stalkerMob.x - p.x;
      double dy = (stalkerMob.y + 1.4) - (p.y + 1.62);
      double dz = stalkerMob.z - p.z;
      double dist = Math.sqrt(dx * dx + dz * dz);

      // If the Stalker falls too far behind (> 38 blocks), relocate it into a nearby corridor behind the player
      if (dist > 38.0 || Math.abs(stalkerMob.y - p.y) > 12.0) {
         relocateStalkerBehindPlayer(g, 14.0);
         return;
      }

      // Check if player is looking directly at the Stalker
      double yawRad = Math.toRadians(p.yaw);
      double pitchRad = Math.toRadians(p.pitch);
      double lookX = Math.sin(yawRad) * Math.cos(pitchRad);
      double lookY = -Math.sin(pitchRad);
      double lookZ = -Math.cos(yawRad) * Math.cos(pitchRad);
      double len3 = Math.max(0.001, Math.sqrt(dx * dx + dy * dy + dz * dz));
      double dot = (dx * lookX + dy * lookY + dz * lookZ) / len3;

      // Always face the player
      stalkerMob.yaw = (float)Math.toDegrees(Math.atan2(-dx, -dz));

      if (dot > 0.72 && dist < 24.0) {
         // Player is staring right at the Stalker: freeze like a Weeping Angel / shadow figure
         stalkerMob.vx = 0.0;
         stalkerMob.vz = 0.0;
         stareTicks++;
         if (stareTicks == 25 && tick - lastWhisperTick > 120L) {
            lastWhisperTick = tick;
            int idx = (int)Math.floorMod(tick / 17L, WHISPERS.length);
            g.chatMsg(WHISPERS[idx]);
         }
         // After staring for 2.5 seconds, trigger a blackout and vanish behind the player!
         if (stareTicks > 50) {
            stareTicks = 0;
            triggerBlackout(g, 55);
            relocateStalkerBehindPlayer(g, 11.0);
         }
      } else {
         stareTicks = Math.max(0, stareTicks - 1);
         // Player is looking away: silently stalk closer along the floor
         if (dist > 2.2) {
            double stepSpeed = blackoutTicks > 0 ? 0.24 : 0.15;
            stalkerMob.vx = (-dx / Math.max(0.1, dist)) * stepSpeed;
            stalkerMob.vz = (-dz / Math.max(0.1, dist)) * stepSpeed;
         }
         // Occasional ambient light flicker when the Stalker gets close
         if (dist < 8.5 && blackoutTicks == 0 && tick % 160L == 0L) {
            triggerBlackout(g, 40);
            if (tick - lastWhisperTick > 100L) {
               lastWhisperTick = tick;
               g.chatMsg("§c§o*The fluorescent lights flicker violently... It is right behind you.*");
            }
         }
      }
   }

   public Mobs.Mob spawnStalkerNearPlayer(Game g, double distance) {
      if (g == null || g.mobs == null || g.player == null || g.world == null) {
         return null;
      }
      if (stalkerMob != null && !stalkerMob.dead) {
         stalkerMob.dead = true;
      }
      double pos[] = findCorridorSpotBehindPlayer(g, distance);
      Mobs.Mob m = g.mobs.spawn(Mobs.ENDERMAN, pos[0], pos[1], pos[2]);
      if (m != null) {
         m.health = 60;
         stalkerMob = m;
      }
      return m;
   }

   private void relocateStalkerBehindPlayer(Game g, double distance) {
      if (stalkerMob == null || g == null || g.player == null) {
         return;
      }
      double[] pos = findCorridorSpotBehindPlayer(g, distance);
      stalkerMob.x = pos[0];
      stalkerMob.y = pos[1];
      stalkerMob.z = pos[2];
      stalkerMob.vx = 0.0;
      stalkerMob.vy = 0.0;
      stalkerMob.vz = 0.0;
   }

   private double[] findCorridorSpotBehindPlayer(Game g, double dist) {
      Player p = g.player;
      double yawRad = Math.toRadians(p.yaw);
      double backX = -Math.sin(yawRad);
      double backZ = Math.cos(yawRad);
      int baseY = (int)Math.floor(p.y);

      for (int attempt = 0; attempt < 8; attempt++) {
         double ang = yawRad + Math.PI + (attempt - 3.5) * 0.45;
         int tx = (int)Math.floor(p.x + Math.sin(ang) * dist);
         int tz = (int)Math.floor(p.z - Math.cos(ang) * dist);
         if (g.world.getBlock(tx, baseY, tz) == Blocks.AIR
            && g.world.getBlock(tx, baseY + 1, tz) == Blocks.AIR
            && g.world.getBlock(tx, baseY + 2, tz) == Blocks.AIR) {
            return new double[]{tx + 0.5, baseY, tz + 0.5};
         }
      }
      int fx = (int)Math.floor(p.x + backX * 6.0);
      int fz = (int)Math.floor(p.z + backZ * 6.0);
      return new double[]{fx + 0.5, baseY, fz + 0.5};
   }

   /** Flickers nearby Glowstone ceiling lights off for {@code durationTicks} ticks. */
   public int triggerBlackout(Game g, int durationTicks) {
      if (g == null || g.world == null || g.player == null) {
         return 0;
      }
      restoreFlickeredLights(g);
      int px = (int)Math.floor(g.player.x);
      int py = (int)Math.floor(g.player.y);
      int pz = (int)Math.floor(g.player.z);

      for (int dz = -14; dz <= 14; dz++) {
         for (int dx = -14; dx <= 14; dx++) {
            for (int dy = -2; dy <= 8; dy++) {
               int bx = px + dx;
               int by = py + dy;
               int bz = pz + dz;
               if (by > 1 && by < 255 && g.world.getBlock(bx, by, bz) == Blocks.GLOWSTONE) {
                  g.world.setBlock(bx, by, bz, Blocks.GLASS);
                  flickeredLights.add(new int[]{bx, by, bz});
               }
            }
         }
      }
      if (!flickeredLights.isEmpty()) {
         blackoutTicks = Math.max(20, durationTicks);
         g.remeshAll();
      }
      return flickeredLights.size();
   }

   public void restoreFlickeredLights(Game g) {
      if (flickeredLights.isEmpty() || g == null || g.world == null) {
         flickeredLights.clear();
         blackoutTicks = 0;
         return;
      }
      for (int[] pos : flickeredLights) {
         if (g.world.getBlock(pos[0], pos[1], pos[2]) == Blocks.GLASS) {
            g.world.setBlock(pos[0], pos[1], pos[2], Blocks.GLOWSTONE);
         }
      }
      flickeredLights.clear();
      blackoutTicks = 0;
      g.remeshAll();
   }

   /** Builds a sealed Level 0 Backrooms Labyrinth around (cx, POCKET_FLOOR_Y, cz) and teleports the player inside. */
   public void enterPocketBackrooms(Game g) {
      if (g == null || g.world == null || g.player == null) {
         return;
      }
      if (!inPocketBackrooms) {
         returnX = g.player.x;
         returnY = g.player.y;
         returnZ = g.player.z;
         hasReturnPos = true;
      }
      int cx = (int)Math.floor(g.player.x);
      int cz = (int)Math.floor(g.player.z);
      int rad = 16;
      long seed = g.world.seed ^ 0xBACC0005L;

      for (int dz = -rad; dz <= rad; dz++) {
         for (int dx = -rad; dx <= rad; dx++) {
            int wx = cx + dx;
            int wz = cz + dz;
            boolean border = (Math.abs(dx) == rad || Math.abs(dz) == rad);

            // Subfloor and damp yellow carpet
            g.world.setBlock(wx, POCKET_FLOOR_Y - 1, wz, Blocks.BEDROCK);
            g.world.setBlock(wx, POCKET_FLOOR_Y, wz, BackroomsGen.CARPET_BLOCK);

            for (int y = POCKET_FLOOR_Y + 1; y < POCKET_CEIL_Y; y++) {
               boolean wall = border || (Math.abs(dx) > 2 || Math.abs(dz) > 2)
                  && BackroomsGen.isWall(seed, dx, y - POCKET_FLOOR_Y + BackroomsGen.FLOOR_Y, dz);
               if (wall) {
                  int blk = (y == POCKET_FLOOR_Y + 1) ? BackroomsGen.WALL_BASE : BackroomsGen.WALL_UPPER;
                  g.world.setBlock(wx, y, wz, blk);
               } else {
                  g.world.setBlock(wx, y, wz, Blocks.AIR);
               }
            }

            boolean light = !border
               && (Math.floorMod(dx, 6) == 2 || Math.floorMod(dx, 6) == 3)
               && (Math.floorMod(dz, 6) == 2);
            g.world.setBlock(wx, POCKET_CEIL_Y, wz, light ? Blocks.GLOWSTONE : BackroomsGen.WALL_UPPER);
            g.world.setBlock(wx, POCKET_CEIL_Y + 1, wz, Blocks.BEDROCK);
         }
      }

      inPocketBackrooms = true;
      g.remeshAll();
      g.player.setPos(cx + 0.5, POCKET_FLOOR_Y + 1.0, cz + 0.5);
      g.player.vx = g.player.vy = g.player.vz = 0.0;
      g.player.fallDistance = 0.0F;
      spawnStalkerNearPlayer(g, 11.0);
      g.chatMsg("§e[Backrooms] §7You noclipped out of reality into §eLevel 0§7. Type §f/backrooms leave§7 to escape.");
   }

   public void leavePocketBackrooms(Game g) {
      if (g == null || g.player == null) {
         return;
      }
      restoreFlickeredLights(g);
      if (stalkerMob != null) {
         stalkerMob.dead = true;
         stalkerMob = null;
      }
      inPocketBackrooms = false;
      if (hasReturnPos) {
         g.player.setPos(returnX, returnY, returnZ);
      } else {
         int sy = 70;
         if (g.world != null) {
            int bx = (int)Math.floor(g.player.x);
            int bz = (int)Math.floor(g.player.z);
            for (int y = 160; y >= 4; y--) {
               if (Blocks.solid[g.world.getBlock(bx, y, bz)]) {
                  sy = y + 1;
                  break;
               }
            }
         }
         g.player.setPos(g.player.x, Math.max(65, sy), g.player.z);
      }
      g.player.vx = g.player.vy = g.player.vz = 0.0;
      g.player.fallDistance = 0.0F;
      g.chatMsg("§a[Backrooms] §7You escaped back to reality.");
   }

   @Override
   public boolean onCommand(Game g, String cmd, String[] args) {
      String c = cmd.toLowerCase(Locale.ROOT);
      if (c.equals("/backrooms")) {
         String sub = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "enter";
         if (sub.equals("leave") || sub.equals("exit") || sub.equals("escape")) {
            leavePocketBackrooms(g);
            return true;
         }
         if (sub.equals("worldgen")) {
            worldGenEnabled = args.length >= 3
               ? (args[2].equalsIgnoreCase("on") || args[2].equalsIgnoreCase("true"))
               : !worldGenEnabled;
            g.chatMsg("§e[Backrooms] §7Infinite Level 0 World Generation is now " + (worldGenEnabled ? "§aON" : "§cOFF"));
            return true;
         }
         enterPocketBackrooms(g);
         return true;
      }
      if (c.equals("/stalker")) {
         String sub = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "spawn";
         if (sub.equals("calm") || sub.equals("clear") || sub.equals("despawn") || sub.equals("off")) {
            restoreFlickeredLights(g);
            if (stalkerMob != null) {
               stalkerMob.dead = true;
               stalkerMob = null;
            }
            sanity = 100;
            g.chatMsg("§a[Stalker] §7The Stalker vanished from the corridors and the lights stabilized.");
            return true;
         }
         if (sub.equals("blackout") || sub.equals("flicker")) {
            int n = triggerBlackout(g, 60);
            g.chatMsg("§8§o[Stalker] " + n + " fluorescent lights shorted out...");
            return true;
         }
         Mobs.Mob m = spawnStalkerNearPlayer(g, 10.0);
         if (m != null) {
            g.chatMsg("§c§o[Stalker] Something tall and dark is stalking you from the shadows...");
         }
         return true;
      }
      return false;
   }
}
