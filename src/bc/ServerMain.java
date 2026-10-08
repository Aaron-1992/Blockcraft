package bc;

/**
 * Update 3.19: Standalone PC multiplayer server entry point ({@code Blockcraft-server.bat} or {@code java -cp blockcraft.jar bc.ServerMain [port] [seed]}).
 * Starts a headless world host on TCP port 25565 (and UDP discovery port 25566) so any Blockcraft client can join via {@code /join} or
 * the Pause Menu "Join LAN / Local" button.
 */
public final class ServerMain {
   private ServerMain() {
   }

   public static void main(String[] args) throws Exception {
      int port = Net319.DEFAULT_PORT;
      long seed = 20260319L;
      if (args.length >= 1) {
         try {
            port = Integer.parseInt(args[0]);
         } catch (NumberFormatException ignored) {
         }
      }

      if (args.length >= 2) {
         try {
            seed = Long.parseLong(args[1]);
         } catch (NumberFormatException e) {
            seed = args[1].hashCode();
         }
      }

      System.out.println("=== Blockcraft V" + Game.VERSION_STR + " Standalone Server ===");
      World w = new World(seed, null);
      String status = Net319.startHost(null, w, port);
      System.out.println(status.replaceAll("§.", ""));
      System.out.println("Seed: " + seed + "  |  Join in-game with: /join 127.0.0.1:" + port + " (or click Join LAN in Pause Menu)");
      System.out.println("Press Ctrl+C to stop the server.");

      long tick = 0;
      int lastPeers = 0;
      while (Net319.isHosting()) {
         Thread.sleep(50L);
         tick++;
         w.dayTime = (w.dayTime + 1L) % 24000L;
         Net319.processInbox(w, null);
         if (tick % 40L == 0L) {
            Net319.broadcastExcept(0, "TIME " + w.dayTime);
         }

         int cur = Net319.peerCount();
         if (cur != lastPeers) {
            System.out.println("[Server] Connected players: " + cur);
            lastPeers = cur;
         }
      }
   }
}
