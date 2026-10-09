package bc;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Headless dedicated server entrypoint ({@code java -cp blockcraft.jar bc.ServerMain [port] [options]}).
 * <p>Supports world creation settings ({@code --type default|skyblock|skywars|bedwars|flat|amplified|large|classic},
 * {@code --seed <seed>}, {@code --mode survival|creative|adventure|spectator}, {@code --worldwide} / {@code --lan})
 * as well as zero-port-forward worldwide connectivity via UPnP + SSH reverse tunnel &amp; Share Codes.
 */
public final class ServerMain {
   private ServerMain() {
   }

   public static void main(String[] args) throws Exception {
      int port = Net319.DEFAULT_PORT;
      long seed = 12345L;
      int worldType = WorldSettings.DEFAULT;
      int gameMode = 0; // Survival
      boolean structures = true;
      boolean worldwide = true;

      for (int i = 0; i < args.length; i++) {
         String a = args[i].trim();
         if ((a.equalsIgnoreCase("--type") || a.equalsIgnoreCase("-t")) && i + 1 < args.length) {
            worldType = WorldSettings.parseType(args[++i]);
         } else if ((a.equalsIgnoreCase("--seed") || a.equalsIgnoreCase("-s")) && i + 1 < args.length) {
            seed = parseSeed(args[++i]);
         } else if ((a.equalsIgnoreCase("--mode") || a.equalsIgnoreCase("-m")) && i + 1 < args.length) {
            gameMode = parseMode(args[++i]);
         } else if (a.equalsIgnoreCase("--no-structures")) {
            structures = false;
         } else if (a.equalsIgnoreCase("--worldwide") || a.equalsIgnoreCase("--tunnel")) {
            worldwide = true;
         } else if (a.equalsIgnoreCase("--lan")) {
            worldwide = false;
         } else if (i == 0 && a.matches("\\d+")) {
            port = Integer.parseInt(a);
         } else if (i == 1 && !a.startsWith("-")) {
            seed = parseSeed(a);
         } else if (i == 2 && !a.startsWith("-")) {
            worldType = WorldSettings.parseType(a);
         }
      }

      int _b = Blocks.STONE;
      int _i = Items.STICK;
      int _r = Recipes.list.size();
      if (_b + _i + _r < 0) {
         return;
      }
      ModLoader.init();

      Net319.mpWorldType = worldType;
      Net319.mpGameMode = gameMode;
      Net319.mpWorldwide = worldwide;

      WorldSettings ws = new WorldSettings();
      ws.type = worldType;
      ws.structures = structures;

      File dir = null;
      if (ws.isMinigame()) {
         SkyGen.matchState = SkyGen.STATE_WAITING;
         SkyGen.resetMatchState();
         Game.deleteDir(new File("saves/server-skywars"));
         Game.deleteDir(new File("saves/server-bedwars"));
      } else {
         dir = new File("saves/server-" + WorldSettings.TYPE_NAME[worldType].split("\\s+")[0].toLowerCase(Locale.ROOT));
         dir.mkdirs();
      }
      World w = new World(seed, dir, Dim.OVERWORLD, ws);
      if (ws.isMinigame()) {
         w.saveEnabled = false;
      }
      int[] sp = w.gen.findSpawn();
      w.getBlock(sp[0], sp[1], sp[2]); // Warm spawn chunk
      if (ws.isVoidMode()) {
         for (int cz = -3; cz <= 3; cz++) {
            for (int cx = -3; cx <= 3; cx++) {
               Chunk ch = w.gen.generate(cx, cz);
               w.chunks.put(World.key(cx, cz), ch);
            }
         }
         for (Chunk ch : new java.util.ArrayList<>(w.chunks.values())) {
            w.lightChunk(ch);
         }
      }

      String msg = Net319.startHost(null, w, port);
      System.out.println("================================================================");
      System.out.println(" Blockcraft V4.1 Dedicated Server");
      System.out.println("----------------------------------------------------------------");
      System.out.println(" " + msg);
      System.out.println(" World Type : " + WorldSettings.TYPE_NAME[worldType] + " (" + WorldSettings.TYPE_INFO[worldType] + ")");
      System.out.println(" Game Mode  : " + Net319.mpModeName() + " | Seed: " + seed);
      System.out.println(" LAN IP     : " + Net319.localLanIp() + ":" + port);
      System.out.println(" Share Code : " + Net319.shareCode + " (paste into Multiplayer -> Server Address)");
      if (worldwide) {
         System.out.println(" Worldwide  : Opening UPnP + Public TCP Tunnel (no port forwarding needed)...");
      }
      System.out.println(" Commands   : status, share, say <msg>, type <skyblock|skywars|bedwars|default>, stop");
      System.out.println("================================================================");

      Thread console = new Thread(() -> {
         try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
               line = line.trim();
               if (line.equalsIgnoreCase("stop") || line.equalsIgnoreCase("quit") || line.equalsIgnoreCase("exit")) {
                  Net319.disconnect(null);
                  w.shutdown();
                  System.exit(0);
               } else if (line.equalsIgnoreCase("status") || line.equalsIgnoreCase("share")) {
                  System.out.println("Peers connected : " + Net319.peerCount());
                  System.out.println("Public Endpoint : " + Net319.publicAddress);
                  System.out.println("Share Code      : " + Net319.shareCode);
               } else if (line.equalsIgnoreCase("start")) {
                  SkyGen.startMatch(null, true);
                  System.out.println("Match started!");
               } else if (line.toLowerCase(Locale.ROOT).startsWith("say ")) {
                  Net319.sendChat(null, line.substring(4).trim());
               }
            }
         } catch (Exception ignored) {
         }
      }, "bc-server-console");
      console.setDaemon(true);
      console.start();

      String lastPrintedTunnel = "";
      while (Net319.isHosting()) {
         Thread.sleep(50L);
         w.tickCount++;
         w.dayTime = (w.dayTime + 1L) % 24000L;
         w.updateChunks(0, 0, 4, 32);
         SkyGen.tickMode(null, w);
         Net319.processInbox(w, null);
         if (!Net319.publicAddress.equals(lastPrintedTunnel) && !Net319.publicAddress.startsWith(Net319.localLanIp())) {
            lastPrintedTunnel = Net319.publicAddress;
            System.out.println("[Worldwide] Public Endpoint Ready: " + Net319.publicAddress);
            System.out.println("[Worldwide] Copy Share Code      : " + Net319.shareCode);
         }
      }
   }

   private static long parseSeed(String s) {
      if (s == null || s.isBlank()) {
         return System.currentTimeMillis();
      }
      try {
         return Long.parseLong(s.trim());
      } catch (NumberFormatException e) {
         return s.trim().hashCode();
      }
   }

   private static int parseMode(String s) {
      if (s == null) {
         return 0;
      }
      return switch (s.trim().toLowerCase(Locale.ROOT)) {
         case "1", "creative", "c" -> 1;
         case "2", "adventure", "a" -> 2;
         case "3", "spectator", "sp" -> 3;
         case "4", "hardcore", "hc", "h" -> 4;
         default -> 0;
      };
   }
}
