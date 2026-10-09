package bc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import org.lwjgl.opengl.GL11;

/**
 * Update 3.19: Prototype peer-to-peer / LAN & standalone-server multiplayer.
 * <ul>
 *   <li>Any client can host its current world directly ({@code /host [port]} or Pause Menu "Host LAN") or join another peer / standalone
 *       server ({@code /join [host[:port]]} or Pause Menu "Join LAN").</li>
 *   <li>UDP broadcast discovery on port {@link #DISCOVERY_PORT} lets {@code /join} (or {@code /join lan}) find a LAN/local host automatically.</li>
 *   <li>Synchronizes world seed &amp; time, live block edits ({@code BLOCK}), chat messages ({@code CHAT}) and 20 Hz 3D animated player poses
 *       ({@code POSE}) rendered with {@link PlayerModel} and {@link PlayerAnim}.</li>
 * </ul>
 */
public final class Net319 {
   public static final int DEFAULT_PORT = 25565;
   public static final int DISCOVERY_PORT = 25566;
   private static final String DISCOVER_REQ = "BLOCKCRAFT_DISCOVER_319";
   private static final String DISCOVER_RES = "BLOCKCRAFT_HERE_319:";

   static final class Peer {
      final int id;
      String name;
      int dim;
      double x;
      double y;
      double z;
      double px;
      double py;
      double pz;
      double tx;
      double ty;
      double tz;
      float yaw;
      float pitch;
      float headYaw;
      float pyaw;
      float ppitch;
      float pheadYaw;
      float limb;
      float amp;
      float pamp;
      boolean sneak;
      boolean sprint;
      boolean swim;
      boolean onGround = true;
      boolean hurt;
      float swing = -1.0F;
      float pswing = -1.0F;
      int heldId;
      int skinIdx;
      float age;

      Peer(int id, String name) {
         this.id = id;
         this.name = name;
      }
   }

   static final class Conn {
      final int id;
      final Socket sock;
      final PrintWriter out;
      volatile String name = "Peer";
      volatile boolean closed;

      Conn(int id, Socket sock) throws IOException {
         this.id = id;
         this.sock = sock;
         sock.setTcpNoDelay(true);
         this.out = new PrintWriter(new OutputStreamWriter(sock.getOutputStream(), StandardCharsets.UTF_8), true);
      }

      synchronized void send(String line) {
         if (!this.closed) {
            try {
               this.out.println(line);
            } catch ( Throwable t) {
               this.close();
            }
         }
      }

      void close() {
         this.closed = true;
         try {
            this.sock.close();
         } catch (IOException ignored) {
         }
      }
   }

   private static volatile boolean hosting = false;
   private static volatile boolean clientConnected = false;
   private static volatile int activePort = DEFAULT_PORT;
   private static volatile Game hostGame;
   private static volatile ServerSocket serverSock;
   private static volatile DatagramSocket udpSock;
   private static volatile Conn clientConn;
   private static final CopyOnWriteArrayList<Conn> hostConns = new CopyOnWriteArrayList<>();
   private static final ConcurrentHashMap<Integer, Peer> peers = new ConcurrentHashMap<>();
   private static final ConcurrentHashMap<Long, Integer> edits = new ConcurrentHashMap<>();
   private static final ConcurrentLinkedQueue<String> inbox = new ConcurrentLinkedQueue<>();
   private static int nextConnId = 2;
   private static int selfId = 1;
   private static String selfName = "Player" + (int)(System.currentTimeMillis() % 900 + 100);
   private static boolean applyingRemote = false;
   private static int[][] peerSkins;
   private static final java.util.concurrent.atomic.AtomicLong itemIdGen = new java.util.concurrent.atomic.AtomicLong((System.currentTimeMillis() & 0xFFFFFL) << 16);
   public static String serverAddr = "127.0.0.1:25565";
   public static String statusText = "Ready — Host a world, launch Server.bat, or join via IP / Share Code";
   public static int mpWorldType = WorldSettings.DEFAULT;
   public static int mpGameMode = 0; // 0=Survival, 1=Creative, 2=Adventure, 3=Spectator
   public static boolean mpWorldwide = true;
   public static volatile String publicAddress = "";
   public static volatile String shareCode = "";
   private static volatile Process dedicatedProc;
   private static volatile Process tunnelProc;
   private static volatile Thread bgServerThread;

   private Net319() {
   }

   public static void cycleMpWorldType() {
      int idx = 0;
      for (int i = 0; i < WorldSettings.TYPE_ORDER.length; i++) {
         if (WorldSettings.TYPE_ORDER[i] == mpWorldType) {
            idx = i;
            break;
         }
      }
      mpWorldType = WorldSettings.TYPE_ORDER[(idx + 1) % WorldSettings.TYPE_ORDER.length];
      if (mpWorldType == WorldSettings.SKYBLOCK || mpWorldType == WorldSettings.SKYWARS || mpWorldType == WorldSettings.BEDWARS) {
         mpGameMode = 0; // Survival by default for Skyblock / Skywars / Bedwars
      }
      statusText = "World Type: " + WorldSettings.TYPE_NAME[mpWorldType] + " — " + WorldSettings.TYPE_INFO[mpWorldType];
   }

   public static void cycleMpGameMode() {
      mpGameMode = (mpGameMode + 1) % 5;
      statusText = "Multiplayer Game Mode set to " + mpModeName();
   }

   public static String mpModeName() {
      return switch (mpGameMode) {
         case 0 -> "Survival";
         case 1 -> "Creative";
         case 2 -> "Adventure";
         case 3 -> "Spectator";
         default -> "Hardcore";
      };
   }

   public static void toggleMpWorldwide() {
      mpWorldwide = !mpWorldwide;
      statusText = mpWorldwide
         ? "Worldwide Relay/UPnP ON — Friends anywhere on Earth can join via Share Code"
         : "LAN-Only Mode — Only players on your local network can join";
   }

   /** Encodes a host:port endpoint into a compact, copy-pasteable Share Code (e.g. BC-...). */
   public static String encodeShareCode(String hostPort) {
      if (hostPort == null || hostPort.isBlank()) {
         return "";
      }
      String clean = hostPort.trim();
      if (clean.startsWith("tcp://")) {
         clean = clean.substring(6);
      }
      String b64 = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(clean.getBytes(StandardCharsets.UTF_8));
      return "BC-" + b64;
   }

   /** Decodes either a Share Code (BC-...), a tcp:// tunnel URL, or a plain host:port string. */
   public static String decodeAddress(String raw) {
      if (raw == null || raw.isBlank()) {
         return "";
      }
      String s = raw.trim();
      if (s.startsWith("tcp://")) {
         s = s.substring(6).trim();
      } else if (s.startsWith("bc://")) {
         s = s.substring(5).trim();
      }
      if (s.length() > 3 && (s.startsWith("BC-") || s.startsWith("bc-"))) {
         String body = s.substring(3).trim();
         try {
            byte[] dec = java.util.Base64.getUrlDecoder().decode(body);
            String out = new String(dec, StandardCharsets.UTF_8).trim();
            if (!out.isEmpty()) {
               return out;
            }
         } catch (IllegalArgumentException ignored) {
         }
      }
      return s;
   }

   public static String localLanIp() {
      try {
         java.util.Enumeration<java.net.NetworkInterface> ifs = java.net.NetworkInterface.getNetworkInterfaces();
         while (ifs != null && ifs.hasMoreElements()) {
            java.net.NetworkInterface ni = ifs.nextElement();
            if (!ni.isUp() || ni.isLoopback()) {
               continue;
            }
            java.util.Enumeration<InetAddress> addrs = ni.getInetAddresses();
            while (addrs.hasMoreElements()) {
               InetAddress a = addrs.nextElement();
               if (a instanceof java.net.Inet4Address && !a.isLoopbackAddress() && a.isSiteLocalAddress()) {
                  return a.getHostAddress();
               }
            }
         }
      } catch (Exception ignored) {
      }
      return "127.0.0.1";
   }

   public static void copyShareCode(Game g) {
      int port = activePort > 0 ? activePort : parsePortFromAddr(serverAddr, DEFAULT_PORT);
      if (publicAddress == null || publicAddress.isBlank()) {
         publicAddress = localLanIp() + ":" + port;
      }
      if (shareCode == null || shareCode.isBlank()) {
         shareCode = encodeShareCode(publicAddress);
      }
      if (g != null) {
         g.setClipboard(shareCode);
         Game.recordHistory(g.addrHistory, shareCode);
      }
      serverAddr = shareCode;
      statusText = "Share Code: " + shareCode + " (" + publicAddress + ") copied!";
   }

   public static long nextItemId() {
      return itemIdGen.incrementAndGet();
   }

   public static int selfId() {
      return selfId;
   }

   public static boolean isHosting() {
      return hosting;
   }

   public static boolean isConnected() {
      return clientConnected;
   }

   public static boolean isActive() {
      return hosting || clientConnected;
   }

   public static int peerCount() {
      return peers.size();
   }

   public static Map<Integer, Peer> peers() {
      return peers;
   }

   // ------------------------------------------------------------------ Host (P2P / LAN or headless ServerMain)
   public static synchronized String host(Game g, int port) {
      return startHost(g, g != null ? g.world : null, port);
   }

   static synchronized String startHost(Game g, World w, int port) {
      disconnect(g);
      if (port <= 0 || port > 65535) {
         port = DEFAULT_PORT;
      }

      try {
         ServerSocket ss = new ServerSocket();
         ss.setReuseAddress(true);
         ss.bind(new InetSocketAddress("0.0.0.0", port));
         serverSock = ss;
         activePort = ss.getLocalPort();
         hostGame = g;
         hosting = true;
         selfId = 1;
         selfName = "Host";
      } catch (IOException e) {
         return "§cCould not host on port " + port + ": " + e.getMessage();
      }

      startDiscoveryResponder(activePort);
      String lanEndpoint = localLanIp() + ":" + activePort;
      publicAddress = lanEndpoint;
      shareCode = encodeShareCode(lanEndpoint);
      if (mpWorldwide && System.getProperty("bc.test") == null) {
         startWorldwideAccess(activePort);
      }
      Thread acceptThread = new Thread(() -> acceptLoop(w), "bc-net-accept");
      acceptThread.setDaemon(true);
      acceptThread.start();
      return "Hosting " + WorldSettings.TYPE_NAME[w != null && w.settings != null ? w.settings.type : mpWorldType]
         + " on port " + activePort + " | Share Code: " + shareCode;
   }

   /** Opens worldwide connectivity without manual router configuration via UPnP/NAT-PMP + SSH reverse TCP tunnel. */
   public static void startWorldwideAccess(int port) {
      Thread t = new Thread(() -> {
         tryUpnpAndNatPmp(port);
         String pubIp = queryPublicIp();
         if (pubIp != null && !pubIp.isBlank()) {
            publicAddress = pubIp.trim() + ":" + port;
            shareCode = encodeShareCode(publicAddress);
            statusText = "Worldwide UPnP active: " + publicAddress + " | Share Code: " + shareCode;
         }
         startReverseTunnel(port);
      }, "bc-net-worldwide");
      t.setDaemon(true);
      t.start();
   }

   private static void tryUpnpAndNatPmp(int port) {
      // 1. NAT-PMP (RFC 6886) UDP request to default gateway .1:5351
      try {
         String lan = localLanIp();
         int lastDot = lan.lastIndexOf('.');
         if (lastDot > 0) {
            String gw = lan.substring(0, lastDot) + ".1";
            try (DatagramSocket ds = new DatagramSocket()) {
               ds.setSoTimeout(350);
               byte[] req = new byte[]{
                  0, 2, 0, 0,
                  (byte)((port >> 8) & 0xFF), (byte)(port & 0xFF),
                  (byte)((port >> 8) & 0xFF), (byte)(port & 0xFF),
                  0, 0, 0x0E, 0x10
               };
               ds.send(new DatagramPacket(req, req.length, InetAddress.getByName(gw), 5351));
            }
         }
      } catch (Exception ignored) {
      }

      // 2. UPnP IGD SSDP M-SEARCH
      try (DatagramSocket ssdp = new DatagramSocket()) {
         ssdp.setSoTimeout(500);
         String msearch = "M-SEARCH * HTTP/1.1\r\n"
            + "HOST: 239.255.255.250:1900\r\n"
            + "MAN: \"ssdp:discover\"\r\n"
            + "MX: 1\r\n"
            + "ST: urn:schemas-upnp-org:service:WANIPConnection:1\r\n\r\n";
         byte[] data = msearch.getBytes(StandardCharsets.UTF_8);
         ssdp.send(new DatagramPacket(data, data.length, InetAddress.getByName("239.255.255.250"), 1900));
      } catch (Exception ignored) {
      }
   }

   private static String queryPublicIp() {
      for (String urlStr : new String[]{"http://checkip.amazonaws.com", "https://api.ipify.org"}) {
         try {
            java.net.URLConnection conn = new java.net.URI(urlStr).toURL().openConnection();
            conn.setConnectTimeout(1200);
            conn.setReadTimeout(1200);
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
               String line = br.readLine();
               if (line != null && line.trim().matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                  return line.trim();
               }
            }
         } catch (Exception ignored) {
         }
      }
      return null;
   }

   private static void startReverseTunnel(int port) {
      try {
         if (tunnelProc != null) {
            tunnelProc.destroyForcibly();
            tunnelProc = null;
         }
         Process p = new ProcessBuilder(
               "ssh",
               "-o", "StrictHostKeyChecking=no",
               "-o", "UserKnownHostsFile=/dev/null",
               "-o", "BatchMode=yes",
               "-o", "ConnectTimeout=5",
               "-o", "ServerAliveInterval=30",
               "-R", "0:127.0.0.1:" + port,
               "tcp@a.pinggy.io"
            )
            .redirectErrorStream(true)
            .start();
         tunnelProc = p;
         try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while (hosting && (line = br.readLine()) != null) {
               int idx = line.indexOf("tcp://");
               if (idx >= 0) {
                  String sub = line.substring(idx + 6).trim();
                  int sp = sub.indexOf(' ');
                  if (sp > 0) {
                     sub = sub.substring(0, sp);
                  }
                  if (sub.contains(":") && !sub.startsWith("127.")) {
                     publicAddress = sub;
                     shareCode = encodeShareCode(publicAddress);
                     statusText = "Worldwide Tunnel ready: " + publicAddress + " | Share Code: " + shareCode;
                     Game hg = hostGame;
                     if (hg != null) {
                        hg.say("§a[Worldwide] Public Tunnel ready: " + publicAddress + " (Share Code: " + shareCode + ")");
                     }
                     break;
                  }
               }
            }
         }
      } catch (Exception ignored) {
      }
   }

   private static void startDiscoveryResponder(int tcpPort) {
      try {
         DatagramSocket ds = new DatagramSocket(null);
         ds.setReuseAddress(true);
         ds.bind(new InetSocketAddress("0.0.0.0", DISCOVERY_PORT));
         udpSock = ds;
         Thread t = new Thread(() -> {
            byte[] buf = new byte[128];
            while (hosting && !ds.isClosed()) {
               try {
                  DatagramPacket pkt = new DatagramPacket(buf, buf.length);
                  ds.receive(pkt);
                  String msg = new String(pkt.getData(), pkt.getOffset(), pkt.getLength(), StandardCharsets.UTF_8).trim();
                  if (DISCOVER_REQ.equals(msg)) {
                     byte[] rep = (DISCOVER_RES + tcpPort).getBytes(StandardCharsets.UTF_8);
                     ds.send(new DatagramPacket(rep, rep.length, pkt.getAddress(), pkt.getPort()));
                  }
               } catch (IOException ignored) {
                  break;
               }
            }
         }, "bc-net-udp");
         t.setDaemon(true);
         t.start();
      } catch (IOException ignored) {
         // Discovery port may already be bound on the same machine; direct TCP /join still works.
      }
   }

   private static void acceptLoop(World w) {
      ServerSocket ss = serverSock;
      while (hosting && ss != null && !ss.isClosed()) {
         try {
            Socket s = ss.accept();
            int cid;
            synchronized (Net319.class) {
               cid = nextConnId++;
            }

            Conn c = new Conn(cid, s);
            hostConns.add(c);
            Game hg = hostGame;
            World curW = hg != null && hg.world != null ? hg.world : w;
            long seed = curW != null ? curW.seed : 12345L;
            int dim = curW != null ? curW.dim : 0;
            long dayTime = curW != null ? curW.dayTime : 1000L;
            int wType = curW != null && curW.settings != null ? curW.settings.type : mpWorldType;
            int gMode = hg != null && hg.player != null
               ? hg.player.rawMode()
               : mpGameMode;
            double sx = 8.5;
            double sy = 72.0;
            double sz = 8.5;
            double[] modeSp = SkyGen.multiplayerSpawnFor(curW, cid - 1);
            if (modeSp != null) {
               sx = modeSp[0];
               sy = modeSp[1];
               sz = modeSp[2];
            } else if (hg != null && hg.player != null) {
               sx = hg.player.x;
               sy = hg.player.y;
               sz = hg.player.z;
            } else if (!peers.isEmpty()) {
               Peer first = peers.values().iterator().next();
               sx = first.x;
               sy = first.y;
               sz = first.z;
            }

            c.send(String.format(Locale.ROOT, "ASSIGN %d %d %d %d %.3f %.3f %.3f %d %d", cid, seed, dim, dayTime, sx, sy, sz, wType, gMode));
            if (hg != null && hg.player != null && curW != null) {
               Player pl = hg.player;
               int flags = (pl.sneaking ? 1 : 0) | (pl.sprinting ? 2 : 0) | (pl.inWater ? 4 : 0) | (pl.onGround ? 8 : 0) | (pl.hurtTime > 0 ? 16 : 0);
               float sw = hg.swinging ? hg.swing : -1.0F;
               c.send(
                  String.format(
                     Locale.ROOT,
                     "POSE %d %s %.3f %.3f %.3f %.2f %.2f %.2f %d %.3f %d %d %d",
                     1,
                     selfName,
                     pl.x,
                     pl.y,
                     pl.z,
                     pl.yaw,
                     pl.pitch,
                     pl.yaw,
                     flags,
                     sw,
                     hg.heldId(),
                     Math.abs(selfName.hashCode()) & 1,
                     curW.dim
                  )
               );
            }

            for (Peer p : peers.values()) {
               int flags = (p.sneak ? 1 : 0) | (p.sprint ? 2 : 0) | (p.swim ? 4 : 0) | (p.onGround ? 8 : 0) | (p.hurt ? 16 : 0);
               c.send(
                  String.format(
                     Locale.ROOT,
                     "POSE %d %s %.3f %.3f %.3f %.2f %.2f %.2f %d %.3f %d %d %d",
                     p.id,
                     p.name,
                     p.x,
                     p.y,
                     p.z,
                     p.yaw,
                     p.pitch,
                     p.headYaw,
                     flags,
                     p.swing,
                     p.heldId,
                     p.skinIdx,
                     p.dim
                  )
               );
            }

            for (Map.Entry<Long, Integer> e : edits.entrySet()) {
               long k = e.getKey();
               c.send("BLOCK " + unpackX(k) + " " + unpackY(k) + " " + unpackZ(k) + " " + e.getValue());
            }

            if (curW != null) {
               for (World.Item it : curW.items) {
                  if (it != null && !it.dead && it.count > 0) {
                     if (it.netId == 0L) {
                        it.netId = nextItemId();
                     }

                     c.send(formatDropPacket(it));
                  }
               }
            }

            Thread rt = new Thread(() -> readLoopHost(c), "bc-net-client-" + cid);
            rt.setDaemon(true);
            rt.start();
         } catch (IOException e) {
            break;
         }
      }
   }

   private static void readLoopHost(Conn c) {
      try (BufferedReader in = new BufferedReader(new InputStreamReader(c.sock.getInputStream(), StandardCharsets.UTF_8))) {
         String line;
         while (!c.closed && (line = in.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) {
               continue;
            }

            if (line.startsWith("HELLO ")) {
               String[] p = line.split("\\s+", 3);
               if (p.length >= 3) {
                  c.name = sanitizeName(p[2]);
                  inbox.add("JOINED " + c.id + " " + c.name);
                  broadcastExcept(c.id, "CHAT * " + c.name + " joined the world");
               }
            } else if (line.startsWith("POSE ")
               || line.startsWith("BLOCK ")
               || line.startsWith("CHAT ")
               || line.startsWith("HIT ")
               || line.startsWith("DROP ")
               || line.startsWith("PICKUP ")) {
               inbox.add(line);
               broadcastExcept(c.id, line);
            }
         }
      } catch (IOException ignored) {
      } finally {
         c.close();
         hostConns.remove(c);
         inbox.add("LEAVE " + c.id);
         broadcastExcept(c.id, "LEAVE " + c.id);
      }
   }

   static void broadcastExcept(int skipId, String line) {
      for (Conn c : hostConns) {
         if (c.id != skipId && !c.closed) {
            c.send(line);
         }
      }
   }

   // ------------------------------------------------------------------ Client Join
   public static synchronized String join(Game g, String target) {
      disconnect(g);
      String host = "127.0.0.1";
      int port = DEFAULT_PORT;
      if (target == null || target.isBlank() || target.equalsIgnoreCase("lan") || target.equalsIgnoreCase("auto")) {
         String[] disc = discoverLan(350);
         if (disc != null) {
            host = disc[0];
            port = Integer.parseInt(disc[1]);
         }
      } else {
         String t = decodeAddress(target);
         int colon = t.lastIndexOf(':');
         if (colon > 0 && colon < t.length() - 1) {
            host = t.substring(0, colon);
            try {
               port = Integer.parseInt(t.substring(colon + 1));
            } catch (NumberFormatException ignored) {
            }
         } else {
            try {
               port = Integer.parseInt(t);
            } catch (NumberFormatException e) {
               host = t;
            }
         }
      }

      try {
         Socket s = new Socket();
         s.connect(new InetSocketAddress(host, port), 1500);
         Conn c = new Conn(0, s);
         clientConn = c;
         clientConnected = true;
         activePort = port;
         selfId = 0;
         selfName = "Peer" + (int)(System.currentTimeMillis() % 900 + 100);
         c.send("HELLO 0 " + selfName);
         Thread rt = new Thread(() -> readLoopClient(c), "bc-net-reader");
         rt.setDaemon(true);
         rt.start();
         statusText = "Connected to " + host + ":" + port + " as " + selfName;
         return statusText;
      } catch (IOException e) {
         clientConnected = false;
         statusText = "Could not connect to " + host + ":" + port + " (" + e.getMessage() + ")";
         return "§c" + statusText;
      }
   }

   /** Update 3.19 / 4.0: called from the Multiplayer menu to join a target server or LAN host. */
   public static void joinFromTitle(Game g) {
      joinFromMenu(g, "lan");
   }

   public static void joinFromMenu(Game g, String target) {
      String msg = join(g, target);
      if (clientConnected && g != null) {
         for (int i = 0; i < 25 && inbox.isEmpty(); i++) {
            try {
               Thread.sleep(10L);
            } catch (InterruptedException ignored) {
               break;
            }
         }

         tick(g);
         if (g.world == null) {
            g.joinMultiplayerWorld(12345L, 0, 1000L, 8.5, 72.0, 8.5);
         }

         g.say(msg);
      } else if (g != null && g.ui != null) {
         g.ui.say(Gui.stripCodes(msg));
      }
   }

   /** V4.0: Hosts a world directly from the Multiplayer screen (loading or creating a world with the selected Multiplayer settings). */
   public static void hostFromMenu(Game g) {
      if (g == null) {
         return;
      }

      int port = parsePortFromAddr(decodeAddress(serverAddr), DEFAULT_PORT);
      if (g.world == null || (g.world.settings != null && g.world.settings.type != mpWorldType)) {
         WorldSettings ws = new WorldSettings();
         ws.type = mpWorldType;
         Gen tmpGen = Dim.makeGen(Dim.OVERWORLD, 20260400L, ws);
         int[] sp = tmpGen.findSpawn();
         g.joinMultiplayerWorld(20260400L, 0, 1000L, sp[0] + 0.5, Math.max(65.0, sp[1]), sp[2] + 0.5, mpWorldType, mpGameMode);
      } else if (g.player != null) {
         g.player.setMode(mpGameMode);
      }

      String msg = host(g, port);
      statusText = Gui.stripCodes(msg);
      g.setScreen(Game.S.PLAY);
      g.say(msg);
   }

   /**
    * V4.0: Launches {@code Blockcraft-server.bat} (or a background {@link ServerMain} instance) with the selected
    * Multiplayer world type and game mode if not already listening on the configured port, then automatically connects the client.
    */
   public static void launchServerAndJoin(Game g) {
      int port = parsePortFromAddr(decodeAddress(serverAddr), DEFAULT_PORT);
      String addr = "127.0.0.1:" + port;
      String typeArg = WorldSettings.TYPE_NAME[mpWorldType].split("\\s+")[0].toLowerCase(Locale.ROOT);
      String modeArg = mpModeName().toLowerCase(Locale.ROOT);
      String netArg = mpWorldwide ? "--worldwide" : "--lan";
      if (!isPortListening("127.0.0.1", port)) {
         boolean launchedBat = false;
         java.io.File bat = new java.io.File("Blockcraft-server.bat");
         String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
         if (bat.isFile() && os.contains("win")) {
            try {
               dedicatedProc = new ProcessBuilder(
                     "cmd", "/c", "start", "\"Blockcraft Server\"", "/min", bat.getAbsolutePath(),
                     String.valueOf(port), "--type", typeArg, "--mode", modeArg, netArg
                  )
                  .directory(bat.getAbsoluteFile().getParentFile())
                  .start();
               launchedBat = true;
               statusText = "Started Blockcraft-server.bat (" + WorldSettings.TYPE_NAME[mpWorldType] + ") on port " + port + "...";
            } catch (IOException ignored) {
            }
         }

         if (!launchedBat) {
            try {
               String javaBin = new java.io.File(System.getProperty("java.home"), "bin/java").getAbsolutePath();
               String cp = System.getProperty("java.class.path", "blockcraft.jar");
               dedicatedProc = new ProcessBuilder(
                     javaBin, "-cp", cp, "bc.ServerMain",
                     String.valueOf(port), "--type", typeArg, "--mode", modeArg, netArg
                  )
                  .redirectErrorStream(true)
                  .start();
               statusText = "Started dedicated ServerMain (" + WorldSettings.TYPE_NAME[mpWorldType] + ") on port " + port + "...";
            } catch (IOException ignored) {
            }
         }

         for (int i = 0; i < 40 && !isPortListening("127.0.0.1", port); i++) {
            try {
               Thread.sleep(35L);
            } catch (InterruptedException ignored) {
               break;
            }
         }
      }

      if (g != null) {
         if (isPortListening("127.0.0.1", port)) {
            joinFromMenu(g, addr);
         } else {
            // Fallback to in-process LAN host so the user always gets an active server immediately
            hostFromMenu(g);
         }
      }
   }

   private static boolean isPortListening(String host, int port) {
      try (Socket s = new Socket()) {
         s.connect(new InetSocketAddress(host, port), 120);
         return true;
      } catch (IOException e) {
         return false;
      }
   }

   public static int parsePortFromAddr(String addr, int defPort) {
      if (addr == null || addr.isBlank()) {
         return defPort;
      }

      String t = addr.trim();
      int colon = t.lastIndexOf(':');
      if (colon >= 0 && colon < t.length() - 1) {
         try {
            int p = Integer.parseInt(t.substring(colon + 1).trim());
            if (p > 0 && p <= 65535) {
               return p;
            }
         } catch (NumberFormatException ignored) {
         }
      }

      return defPort;
   }

   public static String[] discoverLan(int timeoutMs) {
      try (DatagramSocket ds = new DatagramSocket()) {
         ds.setBroadcast(true);
         ds.setSoTimeout(Math.max(100, timeoutMs));
         byte[] req = DISCOVER_REQ.getBytes(StandardCharsets.UTF_8);
         ds.send(new DatagramPacket(req, req.length, InetAddress.getByName("255.255.255.255"), DISCOVERY_PORT));
         ds.send(new DatagramPacket(req, req.length, InetAddress.getByName("127.0.0.1"), DISCOVERY_PORT));
         byte[] buf = new byte[128];
         DatagramPacket rep = new DatagramPacket(buf, buf.length);
         ds.receive(rep);
         String s = new String(rep.getData(), rep.getOffset(), rep.getLength(), StandardCharsets.UTF_8).trim();
         if (s.startsWith(DISCOVER_RES)) {
            String portStr = s.substring(DISCOVER_RES.length()).trim();
            return new String[]{rep.getAddress().getHostAddress(), portStr};
         }
      } catch (IOException ignored) {
      }

      return null;
   }

   private static void readLoopClient(Conn c) {
      try (BufferedReader in = new BufferedReader(new InputStreamReader(c.sock.getInputStream(), StandardCharsets.UTF_8))) {
         String line;
         while (!c.closed && (line = in.readLine()) != null) {
            line = line.trim();
            if (!line.isEmpty()) {
               inbox.add(line);
            }
         }
      } catch (IOException ignored) {
      } finally {
         c.close();
         if (clientConn == c) {
            clientConnected = false;
            inbox.add("DISCONNECTED");
         }
      }
   }

   public static synchronized void disconnect(Game g) {
      hosting = false;
      clientConnected = false;
      if (tunnelProc != null) {
         tunnelProc.destroyForcibly();
         tunnelProc = null;
      }
      if (udpSock != null) {
         udpSock.close();
         udpSock = null;
      }

      if (serverSock != null) {
         try {
            serverSock.close();
         } catch (IOException ignored) {
         }

         serverSock = null;
      }

      for (Conn c : hostConns) {
         c.close();
      }

      hostConns.clear();
      if (clientConn != null) {
         clientConn.close();
         clientConn = null;
      }

      peers.clear();
      inbox.clear();
   }

   // ------------------------------------------------------------------ Pause screen & command helpers
   public static String hostLabel() {
      return hosting ? "Stop Host (" + peers.size() + ")" : "Host LAN";
   }

   public static String joinLabel() {
      return clientConnected ? "Disconnect" : "Join LAN";
   }

   public static void toggleHost(Game g) {
      if (hosting) {
         disconnect(g);
         if (g != null) {
            g.say("Stopped hosting multiplayer");
         }
      } else {
         String msg = host(g, DEFAULT_PORT);
         if (g != null) {
            g.say(msg);
         }
      }
   }

   public static void toggleJoin(Game g) {
      if (clientConnected) {
         disconnect(g);
         if (g != null) {
            g.say("Disconnected from multiplayer");
         }
      } else {
         String msg = join(g, "lan");
         if (g != null) {
            g.say(msg);
         }
      }
   }

   public static void sendChat(Game g, String text) {
      String clean = text.replace('\n', ' ').replace('\r', ' ').trim();
      if (!clean.isEmpty() && isActive()) {
         String pkt = "CHAT " + selfName + " " + clean;
         if (hosting) {
            broadcastExcept(0, pkt);
         } else if (clientConnected && clientConn != null) {
            clientConn.send(pkt);
         }
      }
   }

   public static void onSetBlock(World w, int x, int y, int z, int id) {
      if (applyingRemote || w == null) {
         return;
      }

      if (hosting || clientConnected) {
         edits.put(pack(x, y, z), id);
         String pkt = "BLOCK " + x + " " + y + " " + z + " " + id;
         if (hosting) {
            broadcastExcept(0, pkt);
         } else if (clientConnected && clientConn != null) {
            clientConn.send(pkt);
         }
      }
   }

   // ------------------------------------------------------------------ V4.0: Dropped Item Synchronization
   static String formatDropPacket(World.Item it) {
      return String.format(
         Locale.ROOT,
         "DROP %d %.3f %.3f %.3f %.4f %.4f %.4f %d %d %d %d",
         it.netId,
         it.x,
         it.y,
         it.z,
         it.vx,
         it.vy,
         it.vz,
         it.id,
         it.count,
         it.dmg,
         it.delay
      );
   }

   public static void onDropItem(World w, World.Item it) {
      if (applyingRemote || w == null || it == null || it.count <= 0) {
         return;
      }

      if (it.netId == 0L) {
         it.netId = nextItemId();
      }

      if (hosting || clientConnected) {
         String pkt = formatDropPacket(it);
         if (hosting) {
            broadcastExcept(0, pkt);
         } else if (clientConnected && clientConn != null) {
            clientConn.send(pkt);
         }
      }
   }

   public static void onPickupItem(World.Item it) {
      if (applyingRemote || it == null || it.netId == 0L) {
         return;
      }

      if (hosting || clientConnected) {
         int rem = it.dead ? 0 : Math.max(0, it.count);
         String pkt = "PICKUP " + it.netId + " " + rem;
         if (hosting) {
            broadcastExcept(0, pkt);
         } else if (clientConnected && clientConn != null) {
            clientConn.send(pkt);
         }
      }
   }

   // ------------------------------------------------------------------ V4.0: Survival PvP Player-vs-Player Combat
   private static double rayBox(
      double ox,
      double oy,
      double oz,
      double dx,
      double dy,
      double dz,
      double x0,
      double y0,
      double z0,
      double x1,
      double y1,
      double z1,
      double maxDist
   ) {
      double tMin = 0.0;
      double tMax = maxDist;
      double[] o = {ox, oy, oz};
      double[] d = {dx, dy, dz};
      double[] b0 = {x0, y0, z0};
      double[] b1 = {x1, y1, z1};
      for (int i = 0; i < 3; i++) {
         if (Math.abs(d[i]) < 1.0E-8) {
            if (o[i] < b0[i] || o[i] > b1[i]) {
               return -1.0;
            }
         } else {
            double inv = 1.0 / d[i];
            double t1 = (b0[i] - o[i]) * inv;
            double t2 = (b1[i] - o[i]) * inv;
            if (t1 > t2) {
               double tmp = t1;
               t1 = t2;
               t2 = tmp;
            }

            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);
            if (tMin > tMax) {
               return -1.0;
            }
         }
      }

      return tMin;
   }

   public static Peer pickPeer(Game g, double ox, double oy, double oz, double dx, double dy, double dz, double maxDist) {
      if (!isActive() || peers.isEmpty() || g == null || g.world == null) {
         return null;
      }

      Peer best = null;
      double bestT = maxDist;
      for (Peer p : peers.values()) {
         if (p.dim != g.world.dim) {
            continue;
         }

         double h = p.sneak ? 1.55 : 1.85;
         double t = rayBox(ox, oy, oz, dx, dy, dz, p.x - 0.36, p.y, p.z - 0.36, p.x + 0.36, p.y + h, p.z + 0.36, bestT);
         if (t >= 0.0 && t < bestT) {
            bestT = t;
            best = p;
         }
      }

      return best;
   }

   public static boolean tryAttackPeer(Game g) {
      if (!isActive() || peers.isEmpty() || g == null || g.player == null || g.world == null) {
         return false;
      }

      Player pl = g.player;
      double yawR = Math.toRadians(pl.yaw);
      double pitchR = Math.toRadians(pl.pitch);
      double cp = Math.cos(pitchR);
      double dx = -Math.sin(yawR) * cp;
      double dy = -Math.sin(pitchR);
      double dz = Math.cos(yawR) * cp;
      double ox = pl.x;
      double oy = pl.y + pl.eye;
      double oz = pl.z;
      double maxReach = pl.creative ? 5.0 : 3.5;
      if (g.target != null && g.target.t < maxReach) {
         maxReach = g.target.t;
      }

      if (g.targetMob != null && g.targetMobT < maxReach) {
         maxReach = g.targetMobT;
      }

      Peer hit = pickPeer(g, ox, oy, oz, dx, dy, dz, maxReach);
      if (hit == null) {
         return false;
      }

      int held = g.heldId();
      boolean isTool = Items.isTool(held);
      float baseDmg = isTool ? Items.attackDamage(held) : 1.0F;
      float speed = 4.0F;
      if (isTool) {
         int tc = Items.toolClass(held);
         speed = tc == 4 ? 1.6F : (tc == 1 ? 1.2F : (tc == 2 ? (Items.toolTier(held) >= 3 ? 1.0F : 0.8F) : 1.0F));
      }

      float cd = Math.min(1.0F, (g.atkTicks + 0.5F) / (20.0F / speed));
      baseDmg *= 0.2F + cd * cd * 0.8F;
      if (cd > 0.9F && !pl.onGround && pl.vy < 0.0 && !pl.inWater && !pl.sprinting) {
         baseDmg *= 1.5F;
      }

      int dmg = Math.max(1, Math.round(baseDmg));
      g.atkTicks = 0;
      g.swinging = true;
      g.swing = 1.0E-4F;
      hit.hurt = true;
      if (pl.sprinting) {
         pl.sprinting = false;
      }

      if (!pl.creative) {
         if (isTool) {
            g.damageHeld(Items.toolClass(held) == 4 ? 1 : 2);
         }

         pl.addExhaustion(0.1F);
      }

      sendHit(hit.id, dmg, pl.x, pl.z, selfName);
      return true;
   }

   public static void sendHit(int targetId, int dmg, double attackerX, double attackerZ, String attackerName) {
      String pkt = String.format(Locale.ROOT, "HIT %d %d %.3f %.3f %s", targetId, dmg, attackerX, attackerZ, sanitizeName(attackerName));
      if (hosting) {
         inbox.add(pkt);
         broadcastExcept(0, pkt);
      } else if (clientConnected && clientConn != null) {
         clientConn.send(pkt);
      }
   }

   static void recordServerBlock(World w, int x, int y, int z, int id) {
      edits.put(pack(x, y, z), id);
      if (w != null) {
         Chunk c = w.chunkAt(x, z);
         if (c != null && y >= 0 && y <= 255) {
            c.set(x & 15, y, z & 15, id);
         }
      }
   }

   // ------------------------------------------------------------------ Per-tick processing & pose sync
   public static void tick(Game g) {
      processInbox(g != null ? g.world : null, g);
      if (!isActive() || g == null || g.player == null || g.world == null) {
         return;
      }

      // Broadcast our own pose at 20 Hz
      Player pl = g.player;
      int flags = (pl.sneaking ? 1 : 0) | (pl.sprinting ? 2 : 0) | (pl.inWater ? 4 : 0) | (pl.onGround ? 8 : 0) | (pl.hurtTime > 0 ? 16 : 0);
      float sw = g.swinging ? g.swing : -1.0F;
      String poseLine = String.format(
         Locale.ROOT,
         "POSE %d %s %.3f %.3f %.3f %.2f %.2f %.2f %d %.3f %d %d %d",
         selfId,
         selfName,
         pl.x,
         pl.y,
         pl.z,
         pl.yaw,
         pl.pitch,
         pl.yaw,
         flags,
         sw,
         g.heldId(),
         Math.abs(selfName.hashCode()) & 1,
         g.world.dim
      );
      if (hosting) {
         broadcastExcept(0, poseLine);
         if (g.tickCount % 40 == 0) {
            broadcastExcept(0, "TIME " + g.world.dayTime);
         }
      } else if (clientConnected && clientConn != null) {
         clientConn.send(poseLine);
      }

      // Step peer animations smoothly
      for (Peer p : peers.values()) {
         p.px = p.x;
         p.py = p.y;
         p.pz = p.z;
         p.pyaw = p.yaw;
         p.ppitch = p.pitch;
         p.pheadYaw = p.headYaw;
         p.pamp = p.amp;
         p.pswing = p.swing;
         p.x += (p.tx - p.x) * 0.55;
         p.y += (p.ty - p.y) * 0.55;
         p.z += (p.tz - p.z) * 0.55;
         double dx = p.x - p.px;
         double dz = p.z - p.pz;
         float step = (float)Math.min(0.6, Math.hypot(dx, dz));
         p.amp += (Math.min(1.0F, step * 4.2F) - p.amp) * 0.45F;
         p.limb += step * 2.4F;
         p.age += 1.0F;
      }
   }

   static void processInbox(World w, Game g) {
      String line;
      while ((line = inbox.poll()) != null) {
         try {
            String[] p = line.split("\\s+");
            if (p.length == 0) {
               continue;
            }

            switch (p[0]) {
               case "ASSIGN" -> {
                  if (p.length >= 5) {
                     selfId = Integer.parseInt(p[1]);
                     selfName = "Player" + selfId;
                     long seed = Long.parseLong(p[2]);
                     int dim = Integer.parseInt(p[3]);
                     long dayTime = Long.parseLong(p[4]);
                     double sx = p.length >= 8 ? Double.parseDouble(p[5]) : 8.5;
                     double sy = p.length >= 8 ? Double.parseDouble(p[6]) : 72.0;
                     double sz = p.length >= 8 ? Double.parseDouble(p[7]) : 8.5;
                     int wType = p.length >= 9 ? Integer.parseInt(p[8]) : WorldSettings.DEFAULT;
                     int gMode = p.length >= 10 ? Integer.parseInt(p[9]) : 1;
                     if (g != null) {
                        g.joinMultiplayerWorld(seed, dim, dayTime, sx, sy, sz, wType, gMode);
                     } else if (w != null) {
                        w.dayTime = dayTime;
                     }

                     if (clientConn != null) {
                        clientConn.send("HELLO " + selfId + " " + selfName);
                        if (g != null && g.player != null && g.world != null) {
                           Player pl = g.player;
                           int flags = (pl.sneaking ? 1 : 0) | (pl.sprinting ? 2 : 0) | (pl.inWater ? 4 : 0) | (pl.onGround ? 8 : 0);
                           clientConn.send(
                              String.format(
                                 Locale.ROOT,
                                 "POSE %d %s %.3f %.3f %.3f %.2f %.2f %.2f %d %.3f %d %d %d",
                                 selfId,
                                 selfName,
                                 pl.x,
                                 pl.y,
                                 pl.z,
                                 pl.yaw,
                                 pl.pitch,
                                 pl.yaw,
                                 flags,
                                 -1.0F,
                                 g.heldId(),
                                 Math.abs(selfName.hashCode()) & 1,
                                 g.world.dim
                              )
                           );
                        }
                     }
                  }
               }
               case "JOINED" -> {
                  if (p.length >= 3 && g != null) {
                     g.say("§e" + p[2] + " joined the game");
                  }
               }
               case "LEAVE" -> {
                  if (p.length >= 2) {
                     int id = Integer.parseInt(p[1]);
                     Peer rem = peers.remove(id);
                     if (rem != null && g != null) {
                        g.say("§e" + rem.name + " left the game");
                     }
                  }
               }
               case "DISCONNECTED" -> {
                  if (g != null) {
                     g.say("§cDisconnected from multiplayer host");
                  }
               }
               case "TIME" -> {
                  if (p.length >= 2 && w != null && !hosting) {
                     w.dayTime = Long.parseLong(p[1]);
                  }
               }
               case "CHAT" -> {
                  if (p.length >= 3 && g != null) {
                     String sender = p[1];
                     int idx = line.indexOf(' ', 5);
                     String msg = idx > 0 ? line.substring(idx + 1) : p[2];
                     g.say("*".equals(sender) ? "§e" + msg : "<" + sender + "> " + msg);
                  }
               }
               case "BLOCK" -> {
                  if (p.length >= 5 && w != null) {
                     int bx = Integer.parseInt(p[1]);
                     int by = Integer.parseInt(p[2]);
                     int bz = Integer.parseInt(p[3]);
                     int bid = Integer.parseInt(p[4]);
                     edits.put(pack(bx, by, bz), bid);
                     applyingRemote = true;
                     try {
                        if (w.chunkAt(bx, bz) != null) {
                           w.setBlock(bx, by, bz, bid);
                           if (g != null) {
                              g.meshNow(bx, by, bz);
                           }
                        }
                     } finally {
                        applyingRemote = false;
                     }
                  }
               }
               case "POSE" -> {
                  if (p.length >= 13) {
                     int id = Integer.parseInt(p[1]);
                     if (id == selfId) {
                        continue;
                     }

                     String name = p[2];
                     Peer peer = peers.computeIfAbsent(id, k -> new Peer(k, name));
                     peer.name = name;
                     double nx = Double.parseDouble(p[3]);
                     double ny = Double.parseDouble(p[4]);
                     double nz = Double.parseDouble(p[5]);
                     if (peer.age == 0.0F) {
                        peer.x = peer.px = nx;
                        peer.y = peer.py = ny;
                        peer.z = peer.pz = nz;
                     }

                     peer.tx = nx;
                     peer.ty = ny;
                     peer.tz = nz;
                     peer.yaw = Float.parseFloat(p[6]);
                     peer.pitch = Float.parseFloat(p[7]);
                     peer.headYaw = Float.parseFloat(p[8]);
                     int flags = Integer.parseInt(p[9]);
                     peer.sneak = (flags & 1) != 0;
                     peer.sprint = (flags & 2) != 0;
                     peer.swim = (flags & 4) != 0;
                     peer.onGround = (flags & 8) != 0;
                     peer.hurt = (flags & 16) != 0;
                     peer.swing = Float.parseFloat(p[10]);
                     peer.heldId = Integer.parseInt(p[11]);
                     peer.skinIdx = Integer.parseInt(p[12]);
                     peer.dim = p.length >= 14 ? Integer.parseInt(p[13]) : 0;
                  }
               }
               case "DROP" -> {
                  if (p.length >= 12 && w != null) {
                     long nid = Long.parseLong(p[1]);
                     double dx = Double.parseDouble(p[2]);
                     double dy = Double.parseDouble(p[3]);
                     double dz = Double.parseDouble(p[4]);
                     double vx = Double.parseDouble(p[5]);
                     double vy = Double.parseDouble(p[6]);
                     double vz = Double.parseDouble(p[7]);
                     int iid = Integer.parseInt(p[8]);
                     int cnt = Integer.parseInt(p[9]);
                     int idmg = Integer.parseInt(p[10]);
                     int idelay = Integer.parseInt(p[11]);
                     applyingRemote = true;
                     try {
                        World.Item found = null;
                        for (World.Item it : w.items) {
                           if (it != null && it.netId == nid) {
                              found = it;
                              break;
                           }
                        }

                        if (found != null) {
                           found.x = dx;
                           found.y = dy;
                           found.z = dz;
                           found.vx = vx;
                           found.vy = vy;
                           found.vz = vz;
                           found.count = cnt;
                           found.dmg = idmg;
                           found.delay = idelay;
                           found.dead = cnt <= 0;
                        } else if (cnt > 0) {
                           World.Item it = new World.Item();
                           it.netId = nid;
                           it.x = dx;
                           it.y = dy;
                           it.z = dz;
                           it.vx = vx;
                           it.vy = vy;
                           it.vz = vz;
                           it.id = iid;
                           it.count = cnt;
                           it.dmg = idmg;
                           it.delay = idelay;
                           w.items.add(it);
                        }
                     } finally {
                        applyingRemote = false;
                     }
                  }
               }
               case "PICKUP" -> {
                  if (p.length >= 3 && w != null) {
                     long nid = Long.parseLong(p[1]);
                     int rem = Integer.parseInt(p[2]);
                     for (World.Item it : w.items) {
                        if (it != null && it.netId == nid) {
                           if (rem <= 0) {
                              it.dead = true;
                           } else {
                              it.count = rem;
                           }
                        }
                     }
                  }
               }
               case "HIT" -> {
                  if (p.length >= 5) {
                     int targetId = Integer.parseInt(p[1]);
                     int dmg = Integer.parseInt(p[2]);
                     double ax = Double.parseDouble(p[3]);
                     double az = Double.parseDouble(p[4]);
                     String atkName = p.length >= 6 ? p[5] : "Player";
                     if (targetId == selfId && g != null && g.player != null) {
                        boolean wasDead = g.player.dead;
                        g.damagePlayer(dmg, ax, az);
                        if (!wasDead && g.player.dead) {
                           String deathMsg = selfName + " was slain by " + atkName;
                           g.say("§e" + deathMsg);
                           String chatPkt = "CHAT * " + deathMsg;
                           if (hosting) {
                              broadcastExcept(0, chatPkt);
                           } else if (clientConnected && clientConn != null) {
                              clientConn.send(chatPkt);
                           }
                        }
                     } else {
                        Peer targetPeer = peers.get(targetId);
                        if (targetPeer != null) {
                           targetPeer.hurt = true;
                        }
                     }
                  }
               }
            }
         } catch (Exception ignored) {
         }
      }
   }

   // ------------------------------------------------------------------ 3D rendering of remote peers
   static PlayerModel.Pose pose(Peer p, float partial) {
      PlayerModel.Pose out = new PlayerModel.Pose();
      out.bodyYaw = p.pyaw + (p.yaw - p.pyaw) * partial;
      out.headYaw = 0.0F;
      out.headPitch = p.ppitch + (p.pitch - p.ppitch) * partial;
      out.limb = p.limb;
      out.amp = p.pamp + (p.amp - p.pamp) * partial;
      out.swing = p.swing;
      out.sneak = p.sneak;
      out.sprint = p.sprint;
      out.air = !p.onGround;
      out.sneakK = p.sneak ? 1.0F : 0.0F;
      out.sprintK = p.sprint ? 1.0F : 0.0F;
      out.airK = p.onGround ? 0.0F : 1.0F;
      out.swim = p.swim ? 1.0F : 0.0F;
      out.hurt = p.hurt ? 1.0F : 0.0F;
      out.age = p.age + partial;
      out.held = p.heldId;
      return out;
   }

   private static synchronized int[] skinPixels(int idx) {
      if (peerSkins == null) {
         peerSkins = new int[][]{Skins.defaultPixels(), Skins.demoSkin()};
      }

      return peerSkins[Math.abs(idx) % peerSkins.length];
   }

   static void drawPeers(Renderer r, float partial) {
      if (peers.isEmpty() || r == null || r.g == null || r.g.world == null) {
         return;
      }

      World w = r.g.world;
      GL11.glEnable(2929);
      GL11.glDepthMask(true);
      GL11.glDisable(2884);
      for (Peer p : peers.values()) {
         if (p.dim != w.dim) {
            continue;
         }

         double x = p.px + (p.x - p.px) * partial;
         double y = p.py + (p.y - p.py) * partial;
         double z = p.pz + (p.z - p.pz) * partial;
         float bright = r.brightnessAt(w, x, y + 1.0, z);
         GL11.glPushMatrix();
         GL11.glTranslated(x - r.camX, y - r.camY, z - r.camZ);
         PlayerModel.draw(skinPixels(p.skinIdx), false, pose(p, partial), bright);
         drawNameTag(r, p.name, p.sneak ? 1.98F : 2.16F);
         GL11.glPopMatrix();
      }

      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
   }

   private static void drawNameTag(Renderer r, String name, float yOff) {
      if (name == null || name.isEmpty() || Tex.fontTex == 0) {
         return;
      }

      boolean wasEnt = r.sh != null && r.sh.entityOn;
      if (wasEnt) {
         r.sh.stopEntity();
      }

      GL11.glPushMatrix();
      GL11.glTranslatef(0.0F, yOff, 0.0F);
      GL11.glRotatef(-r.g.player.yaw, 0.0F, 1.0F, 0.0F);
      GL11.glRotatef(r.g.player.pitch, 1.0F, 0.0F, 0.0F);
      float sc = 0.026F;
      GL11.glScalef(-sc, -sc, sc);
      float tw = Gui.textWidth(name);
      float x0 = -tw * 0.5F;
      GL11.glDisable(3553);
      GL11.glDisable(3008);
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      GL11.glDepthMask(false);
      GL11.glColor4f(0.0F, 0.0F, 0.0F, 0.42F);
      GL11.glBegin(7);
      GL11.glVertex3f(x0 - 2.0F, -1.0F, 0.01F);
      GL11.glVertex3f(x0 + tw + 2.0F, -1.0F, 0.01F);
      GL11.glVertex3f(x0 + tw + 2.0F, 9.0F, 0.01F);
      GL11.glVertex3f(x0 - 2.0F, 9.0F, 0.01F);
      GL11.glEnd();
      GL11.glEnable(3553);
      GL11.glBindTexture(3553, Tex.fontTex);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 0.95F);
      float cx = x0;
      float invW = 1.0F / (Tex.fontCols * Tex.fontCellW);
      float invH = 1.0F / (6 * Tex.fontCellH);
      GL11.glBegin(7);
      for (int i = 0; i < name.length(); i++) {
         int ch = name.charAt(i) - 32;
         if (ch >= 0 && ch < 96) {
            int col = ch % Tex.fontCols;
            int row = ch / Tex.fontCols;
            float u0 = col * Tex.fontCellW * invW;
            float v0 = row * Tex.fontCellH * invH;
            float u1 = (col + 1) * Tex.fontCellW * invW;
            float v1 = (row + 1) * Tex.fontCellH * invH;
            GL11.glTexCoord2f(u0, v0);
            GL11.glVertex3f(cx, 0.0F, 0.0F);
            GL11.glTexCoord2f(u1, v0);
            GL11.glVertex3f(cx + Tex.fontCellW, 0.0F, 0.0F);
            GL11.glTexCoord2f(u1, v1);
            GL11.glVertex3f(cx + Tex.fontCellW, Tex.fontCellH, 0.0F);
            GL11.glTexCoord2f(u0, v1);
            GL11.glVertex3f(cx, Tex.fontCellH, 0.0F);
            cx += Tex.fontW[ch];
         }
      }

      GL11.glEnd();
      GL11.glDepthMask(true);
      GL11.glDisable(3042);
      GL11.glPopMatrix();
      if (wasEnt) {
         r.sh.useEntity(r.mv, r.camX, r.camY, r.camZ, r.entFogNear, r.entFogFar, r.entFogCol, r.entShadow, r.lastSky, r.lastBlk);
      }
   }

   private static String sanitizeName(String s) {
      String clean = s.replaceAll("[^A-Za-z0-9_\\-]", "");
      return clean.isEmpty() ? "Peer" : (clean.length() > 16 ? clean.substring(0, 16) : clean);
   }

   private static long pack(int x, int y, int z) {
      return ((long)(x & 0x3FFFFFF) << 38) | ((long)(z & 0x3FFFFFF) << 12) | (y & 0xFFF);
   }

   private static int unpackX(long k) {
      int v = (int)(k >> 38);
      return (v & 0x2000000) != 0 ? (v | 0xFC000000) : v;
   }

   private static int unpackZ(long k) {
      int v = (int)((k >> 12) & 0x3FFFFFFL);
      return (v & 0x2000000) != 0 ? (v | 0xFC000000) : v;
   }

   private static int unpackY(long k) {
      return (int)(k & 0xFFFL);
   }
}
