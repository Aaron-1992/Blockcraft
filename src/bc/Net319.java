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
      boolean spectator;
      boolean eliminated;
      boolean fly;
      int team = -1;
      int health = 20;
      int armorPts = 0;
      int gameMode = 0;
      int pingMs = 1;
      int kills = 0;
      int deaths = 0;
      float swing = -1.0F;
      float pswing = -1.0F;
      float eat = 0.0F;
      float bow = 0.0F;
      int heldId;
      int skinIdx;
      float age;

      Peer(int id, String name) {
         this.id = id;
         this.name = name;
         this.team = Math.max(0, (id - 1) & 3);
      }
   }

   static final class WorldPing {
      final int senderId;
      final String senderName;
      final int team;
      final int dim;
      final double x;
      final double y;
      final double z;
      int ticks;

      WorldPing(int senderId, String senderName, int team, int dim, double x, double y, double z, int ticks) {
         this.senderId = senderId;
         this.senderName = senderName;
         this.team = team;
         this.dim = dim;
         this.x = x;
         this.y = y;
         this.z = z;
         this.ticks = ticks;
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
         sock.setKeepAlive(true);
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
   private static final CopyOnWriteArrayList<WorldPing> worldPings = new CopyOnWriteArrayList<>();
   private static final ConcurrentHashMap<Integer, ConcurrentHashMap<Long, Integer>> editsByDim = new ConcurrentHashMap<>();
   private static final ConcurrentHashMap<Integer, ConcurrentHashMap<Long, String>> tilesByDim = new ConcurrentHashMap<>();
   private static final ConcurrentLinkedQueue<String> inbox = new ConcurrentLinkedQueue<>();
   private static int nextConnId = 2;
   public static int selfId = 1;
   public static String selfName = "Player" + (int)(System.currentTimeMillis() % 900 + 100);
   public static String customNick = "";
   public static volatile int myPingMs = 1;
   private static int presetNickIdx = 0;
   private static final String[] PRESET_NICKS = new String[]{
      "Builder", "RedKnight", "SkyKing", "BedBreaker", "Explorer", "CreeperSlayer", "VoidWalker", "Architect"
   };
   private static boolean applyingRemote = false;
   public static volatile boolean hostHasMobSync = false;
   private static int[][] peerSkins;
   private static final java.util.concurrent.atomic.AtomicLong itemIdGen = new java.util.concurrent.atomic.AtomicLong((System.currentTimeMillis() & 0xFFFFFL) << 16);
   private static final java.util.concurrent.atomic.AtomicInteger mobIdGen = new java.util.concurrent.atomic.AtomicInteger(1);
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
      if (WorldSettings.isMinigame(mpWorldType)) {
         mpGameMode = 0; // Survival by default for all Minigames
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

   public static void cyclePresetNick() {
      String nick = PRESET_NICKS[presetNickIdx % PRESET_NICKS.length] + (10 + (int)(System.currentTimeMillis() % 89));
      presetNickIdx = (presetNickIdx + 1) % PRESET_NICKS.length;
      setNickname(null, nick);
      statusText = "Multiplayer Nickname set to " + selfName + " (or type /nick <name> in-game)";
   }

   public static void setNickname(Game g, String name) {
      String clean = sanitizeName(name == null ? "" : name.trim());
      if (clean.isEmpty()) {
         return;
      }
      String oldName = selfName;
      customNick = clean;
      selfName = clean;
      if (isActive() && !clean.equals(oldName)) {
         String pkt = "CHAT * " + oldName + " is now known as " + clean;
         if (hosting) {
            broadcastExcept(0, pkt);
         } else if (clientConnected && clientConn != null) {
            clientConn.send(pkt);
         }
      }
      if (g != null) {
         g.say("§a[Multiplayer] §fYour nickname is now §e" + selfName);
      }
   }

   public static String displayNick() {
      return customNick != null && !customNick.isEmpty() ? customNick : selfName;
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

   /**
    * Collects all reachable local &amp; virtual VPN IPv4 addresses (Radmin VPN 26.x.x.x, Hamachi 25.x.x.x,
    * Tailscale 100.64..127.x.x, ZeroTier, and standard LAN 192.168.x.x / 10.x.x.x), prioritizing gaming VPNs
    * and real Wi-Fi/Ethernet adapters over local hypervisor switches (WSL/Hyper-V/Docker/VirtualBox).
    */
   public static ArrayList<String> allLocalIps() {
      ArrayList<String> vpnIps = new ArrayList<>();
      ArrayList<String> lanIps = new ArrayList<>();
      ArrayList<String> fallbackIps = new ArrayList<>();
      try {
         java.util.Enumeration<java.net.NetworkInterface> ifs = java.net.NetworkInterface.getNetworkInterfaces();
         while (ifs != null && ifs.hasMoreElements()) {
            java.net.NetworkInterface ni = ifs.nextElement();
            if (!ni.isUp() || ni.isLoopback()) {
               continue;
            }
            String desc = (ni.getName() + " " + ni.getDisplayName()).toLowerCase(Locale.ROOT);
            boolean isHypervisor = desc.contains("vethernet")
               || desc.contains("wsl")
               || desc.contains("hyper-v")
               || desc.contains("docker")
               || desc.contains("vbox")
               || desc.contains("virtualbox")
               || desc.contains("vmware");
            boolean isGamingVpn = desc.contains("radmin")
               || desc.contains("hamachi")
               || desc.contains("tailscale")
               || desc.contains("zerotier")
               || desc.contains("wireguard");
            java.util.Enumeration<InetAddress> addrs = ni.getInetAddresses();
            while (addrs.hasMoreElements()) {
               InetAddress a = addrs.nextElement();
               if (!(a instanceof java.net.Inet4Address) || a.isLoopbackAddress() || a.isLinkLocalAddress()) {
                  continue;
               }
               byte[] raw = a.getAddress();
               int b0 = raw[0] & 0xFF;
               int b1 = raw[1] & 0xFF;
               String ip = a.getHostAddress();
               boolean vpnSubnet = (b0 == 26) // Radmin VPN (26.0.0.0/8)
                  || (b0 == 25) // LogMeIn Hamachi (25.0.0.0/8)
                  || (b0 == 100 && b1 >= 64 && b1 <= 127) // Tailscale CGNAT (100.64.0.0/10)
                  || isGamingVpn;
               if (vpnSubnet) {
                  if (!vpnIps.contains(ip)) {
                     vpnIps.add(ip);
                  }
               } else if (a.isSiteLocalAddress()) {
                  if (isHypervisor || ip.startsWith("192.168.56.")) {
                     if (!fallbackIps.contains(ip)) {
                        fallbackIps.add(ip);
                     }
                  } else if (!lanIps.contains(ip)) {
                     lanIps.add(ip);
                  }
               }
            }
         }
      } catch (Exception ignored) {
      }
      ArrayList<String> out = new ArrayList<>();
      out.addAll(vpnIps);
      out.addAll(lanIps);
      out.addAll(fallbackIps);
      if (out.isEmpty()) {
         out.add("127.0.0.1");
      }
      return out;
   }

   public static String localLanIp() {
      return allLocalIps().get(0);
   }

   public static void refreshShareEndpoints(int port, String verifiedExternal) {
      ArrayList<String> eps = new ArrayList<>();
      if (verifiedExternal != null && !verifiedExternal.isBlank()) {
         eps.add(verifiedExternal.trim());
      }
      for (String ip : allLocalIps()) {
         String ep = ip + ":" + port;
         if (!eps.contains(ep)) {
            eps.add(ep);
         }
      }
      publicAddress = eps.get(0);
      shareCode = encodeShareCode(String.join("|", eps));
   }

   public static void copyShareCode(Game g) {
      int port = activePort > 0 ? activePort : parsePortFromAddr(serverAddr, DEFAULT_PORT);
      if (shareCode == null || shareCode.isBlank()) {
         refreshShareEndpoints(port, null);
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

   public static int nextMobId() {
      return mobIdGen.getAndIncrement();
   }

   public static ConcurrentHashMap<Long, Integer> editsForDim(int dim) {
      return editsByDim.computeIfAbsent(dim, k -> new ConcurrentHashMap<>());
   }

   public static ConcurrentHashMap<Long, String> tilesForDim(int dim) {
      return tilesByDim.computeIfAbsent(dim, k -> new ConcurrentHashMap<>());
   }

   public static void clearWorldState() {
      editsByDim.clear();
      tilesByDim.clear();
      worldPings.clear();
      hostHasMobSync = false;
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

   public static boolean isClientConnected() {
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

   public static int aliveEnemyPeers(int myTeam) {
      int c = 0;
      for (Peer p : peers.values()) {
         if (!p.spectator && !p.eliminated && p.health > 0 && p.gameMode != 1 && p.gameMode != 3 && (myTeam < 0 || p.team != myTeam)) {
            c++;
         }
      }
      return c;
   }

   public static int aliveTeamPeers(int team) {
      int c = 0;
      for (Peer p : peers.values()) {
         if (!p.spectator && !p.eliminated && p.health > 0 && p.gameMode != 1 && p.gameMode != 3 && p.team == team) {
            c++;
         }
      }
      return c;
   }

   public static boolean isTeamOccupiedByPeer(int team) {
      for (Peer p : peers.values()) {
         if (p.team == team) {
            return true;
         }
      }
      return false;
   }

   public static void clearPeersEliminated() {
      for (Peer p : peers.values()) {
         p.eliminated = false;
         p.spectator = false;
      }
   }

   public static int pickAvailableTeam(int cid) {
      boolean[] taken = new boolean[4];
      taken[Math.abs(SkyGen.myTeam) & 3] = true;
      for (Peer p : peers.values()) {
         if (p.team >= 0 && p.team < 4) {
            taken[p.team] = true;
         }
      }
      for (int t = 0; t < 4; t++) {
         if (!taken[t]) {
            return t;
         }
      }
      return Math.max(0, (cid - 1) & 3);
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
         selfName = (customNick != null && !customNick.isEmpty()) ? customNick : "Host";
         myPingMs = 1;
      } catch (IOException e) {
         return "§cCould not host on port " + port + ": " + e.getMessage();
      }

      startDiscoveryResponder(activePort);
      refreshShareEndpoints(activePort, null);
      if (mpWorldwide && System.getProperty("bc.test") == null) {
         startWorldwideAccess(activePort);
      }
      Thread acceptThread = new Thread(() -> acceptLoop(w), "bc-net-accept");
      acceptThread.setDaemon(true);
      acceptThread.start();
      return "Hosting " + WorldSettings.TYPE_NAME[w != null && w.settings != null ? w.settings.type : mpWorldType]
         + " on " + publicAddress + " | Share Code: " + shareCode;
   }

   /** Opens worldwide connectivity without manual router configuration via UPnP/NAT-PMP + SSH reverse TCP tunnel. */
   public static void startWorldwideAccess(int port) {
      Thread t = new Thread(() -> {
         tryUpnpAndNatPmp(port);
         String pubIp = queryPublicIp();
         if (pubIp != null && !pubIp.isBlank() && isPortListening(pubIp.trim(), port, 900)) {
            refreshShareEndpoints(port, pubIp.trim() + ":" + port);
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
      String nullFile = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win") ? "NUL" : "/dev/null";
      for (String target : new String[]{"tcp@a.pinggy.io", "serveo.net"}) {
         if (!hosting) {
            return;
         }
         try {
            if (tunnelProc != null) {
               tunnelProc.destroyForcibly();
               tunnelProc = null;
            }
            Process p = new ProcessBuilder(
                  "ssh",
                  "-T",
                  "-o", "StrictHostKeyChecking=no",
                  "-o", "UserKnownHostsFile=" + nullFile,
                  "-o", "ConnectTimeout=6",
                  "-o", "ServerAliveInterval=25",
                  "-R", "0:127.0.0.1:" + port,
                  target
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
                        refreshShareEndpoints(port, sub);
                        statusText = "Worldwide Tunnel ready: " + publicAddress + " | Share Code: " + shareCode;
                        Game hg = hostGame;
                        if (hg != null) {
                           hg.say("§a[Worldwide] Public Tunnel ready: " + publicAddress + " (Share Code: " + shareCode + ")");
                        }
                        return;
                     }
                  }
               }
            }
         } catch (Exception ignored) {
         }
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
            int gMode = (wType == WorldSettings.BEDWARS || wType == WorldSettings.SKYWARS || wType == WorldSettings.SKYBLOCK)
               ? 0
               : (hg != null && hg.player != null ? hg.player.rawMode() : mpGameMode);
            int assignedTeam = pickAvailableTeam(cid);
            double sx = 8.5;
            double sy = 72.0;
            double sz = 8.5;
            double[] modeSp = SkyGen.multiplayerSpawnFor(curW, wType == WorldSettings.BEDWARS ? assignedTeam : (cid - 1));
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

            int mapId = curW != null && curW.settings != null ? curW.settings.mapId : 0;
            c.send(
               String.format(
                  Locale.ROOT,
                  "ASSIGN %d %d %d %d %.3f %.3f %.3f %d %d %d %d %d %d %d %d",
                  cid,
                  seed,
                  dim,
                  dayTime,
                  sx,
                  sy,
                  sz,
                  wType,
                  gMode,
                  assignedTeam,
                  SkyGen.matchState,
                  mapId,
                  SkyGen.swKit,
                  SkyGen.tumbleWeaponMode,
                  SkyGen.soloBots ? 1 : 0
               )
            );
            c.send("TEAM 1 " + SkyGen.myTeam);
            if (curW != null && wType == WorldSettings.BEDWARS) {
               for (int t = 0; t < 4; t++) {
                  if (!SkyGen.isTeamBedIntact(curW, t)) {
                     c.send("BED " + t + " Server");
                  }
                  if (SkyGen.teamUpgrades[t] != 0) {
                     c.send("UPG " + t + " " + SkyGen.teamUpgrades[t] + " Server");
                  }
               }
            }
            if (hg != null && hg.player != null && curW != null) {
               Player pl = hg.player;
               int flags = (pl.sneaking ? 1 : 0)
                  | (pl.sprinting ? 2 : 0)
                  | (pl.inWater ? 4 : 0)
                  | (pl.onGround ? 8 : 0)
                  | (pl.hurtTime > 0 ? 16 : 0)
                  | ((pl.dead || pl.spectator) ? 32 : 0);
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
               c.send("TEAM " + p.id + " " + p.team);
               int flags = (p.sneak ? 1 : 0)
                  | (p.sprint ? 2 : 0)
                  | (p.swim ? 4 : 0)
                  | (p.onGround ? 8 : 0)
                  | (p.hurt ? 16 : 0)
                  | (p.spectator ? 32 : 0);
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

            c.send("MODS " + ModLoader.getActiveModsCsv());
            if (curW != null && curW.weather != null) {
               c.send(
                  "ENV "
                     + curW.dayTime
                     + " "
                     + curW.weather.ticks
                     + " "
                     + curW.weather.forceMode
                     + " "
                     + curW.weather.forceEnd
                     + " "
                     + (curW.weather.enabled ? 1 : 0)
               );
            }

            for (Map.Entry<Integer, ConcurrentHashMap<Long, Integer>> de : editsByDim.entrySet()) {
               int d = de.getKey();
               for (Map.Entry<Long, Integer> e : de.getValue().entrySet()) {
                  long k = e.getKey();
                  c.send("BLOCK " + unpackX(k) + " " + unpackY(k) + " " + unpackZ(k) + " " + e.getValue() + " " + d);
               }
            }

            for (Map.Entry<Integer, ConcurrentHashMap<Long, String>> td : tilesByDim.entrySet()) {
               for (String tilePkt : td.getValue().values()) {
                  c.send(tilePkt);
               }
            }
            if (curW != null && !curW.tiles.isEmpty()) {
               for (TileEntity te : new ArrayList<TileEntity>(curW.tiles.values())) {
                  c.send(formatTilePacket(curW.dim, te));
               }
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

            if (hg != null && hg.mobs != null && curW != null) {
               sendMobsToConn(c, curW, hg.mobs);
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

            if (line.equals("PING") || line.startsWith("PING ")) {
               c.send(line.length() > 5 ? ("PONG " + line.substring(5).trim()) : "PONG");
            } else if (line.startsWith("HELLO ")) {
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
               || line.startsWith("KILL ")
               || line.startsWith("DROP ")
               || line.startsWith("PICKUP ")
               || line.startsWith("BED ")
               || line.startsWith("UPG ")
               || line.startsWith("TEAM ")
               || line.startsWith("MATCH ")
               || line.startsWith("ELIM ")
               || line.startsWith("TILE ")
               || line.startsWith("MOB_HIT ")
               || line.startsWith("MOB_ACT ")
               || line.startsWith("ARROW ")
               || line.startsWith("FBALL ")
               || line.startsWith("WPING ")
               || line.startsWith("TNT ")
               || line.startsWith("EXPLODE ")
               || line.startsWith("MODS ")
               || line.startsWith("ENV ")) {
               inbox.add(line);
               if (!line.startsWith("MOB_HIT ") && !line.startsWith("MOB_ACT ")) {
                  broadcastExcept(c.id, line);
               }
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
   private static String[] parseHostPort(String ep) {
      String h = "127.0.0.1";
      int p = DEFAULT_PORT;
      String t = ep == null ? "" : ep.trim();
      if (t.startsWith("tcp://")) {
         t = t.substring(6).trim();
      }
      int colon = t.lastIndexOf(':');
      if (colon > 0 && colon < t.length() - 1) {
         h = t.substring(0, colon).trim();
         try {
            p = Integer.parseInt(t.substring(colon + 1).trim());
         } catch (NumberFormatException ignored) {
         }
      } else if (!t.isEmpty()) {
         try {
            p = Integer.parseInt(t);
         } catch (NumberFormatException e) {
            h = t;
         }
      }
      return new String[]{h, Integer.toString(p)};
   }

   public static synchronized String join(Game g, String target) {
      disconnect(g);
      ArrayList<String[]> candidates = new ArrayList<>();
      if (target == null || target.isBlank() || target.equalsIgnoreCase("lan") || target.equalsIgnoreCase("auto")) {
         String[] disc = discoverLan(900);
         if (disc != null) {
            candidates.add(disc);
         } else {
            candidates.add(new String[]{"127.0.0.1", Integer.toString(DEFAULT_PORT)});
         }
      } else {
         String decoded = decodeAddress(target);
         for (String part : decoded.split("[|,;]+")) {
            if (!part.isBlank()) {
               candidates.add(parseHostPort(part));
            }
         }
         if (candidates.isEmpty()) {
            candidates.add(new String[]{"127.0.0.1", Integer.toString(DEFAULT_PORT)});
         }
      }

      // Race all candidate endpoints (Tunnel, Radmin VPN, Tailscale, Hamachi, LAN) with a generous 5500ms timeout
      Socket connectedSock = null;
      String connectedHost = candidates.get(0)[0];
      int connectedPort = Integer.parseInt(candidates.get(0)[1]);
      IOException lastErr = null;
      int timeoutMs = System.getProperty("bc.test") != null ? 1200 : 5500;

      if (candidates.size() == 1) {
         try {
            Socket s = new Socket();
            s.connect(new InetSocketAddress(connectedHost, connectedPort), timeoutMs);
            connectedSock = s;
         } catch (IOException e) {
            lastErr = e;
         }
      } else {
         final Object lock = new Object();
         final Socket[] winner = new Socket[1];
         final String[] winAddr = new String[2];
         final IOException[] errRef = new IOException[1];
         final int[] remaining = new int[]{candidates.size()};
         for (String[] cand : candidates) {
            final String ch = cand[0];
            final int cp = Integer.parseInt(cand[1]);
            Thread racer = new Thread(() -> {
               Socket s = new Socket();
               try {
                  s.connect(new InetSocketAddress(ch, cp), timeoutMs);
                  synchronized (lock) {
                     if (winner[0] == null) {
                        winner[0] = s;
                        winAddr[0] = ch;
                        winAddr[1] = Integer.toString(cp);
                        lock.notifyAll();
                        return;
                     }
                  }
                  s.close();
               } catch (IOException e) {
                  try {
                     s.close();
                  } catch (IOException ignored) {
                  }
                  synchronized (lock) {
                     errRef[0] = e;
                     remaining[0]--;
                     if (remaining[0] <= 0) {
                        lock.notifyAll();
                     }
                  }
               }
            }, "bc-net-racer");
            racer.setDaemon(true);
            racer.start();
         }
         synchronized (lock) {
            long deadline = System.currentTimeMillis() + timeoutMs + 200L;
            while (winner[0] == null && remaining[0] > 0) {
               long wait = deadline - System.currentTimeMillis();
               if (wait <= 0L) {
                  break;
               }
               try {
                  lock.wait(wait);
               } catch (InterruptedException ignored) {
                  break;
               }
            }
            if (winner[0] != null) {
               connectedSock = winner[0];
               connectedHost = winAddr[0];
               connectedPort = Integer.parseInt(winAddr[1]);
            } else {
               lastErr = errRef[0];
            }
         }
      }

      // Automatic LAN / Radmin VPN broadcast discovery fallback if direct target timed out
      if (connectedSock == null && System.getProperty("bc.test") == null) {
         String[] disc = discoverLan(800);
         if (disc != null) {
            try {
               Socket s = new Socket();
               int dp = Integer.parseInt(disc[1]);
               s.connect(new InetSocketAddress(disc[0], dp), 2500);
               connectedSock = s;
               connectedHost = disc[0];
               connectedPort = dp;
            } catch (IOException e) {
               lastErr = e;
            }
         }
      }

      if (connectedSock != null) {
         try {
            Conn c = new Conn(0, connectedSock);
            clientConn = c;
            clientConnected = true;
            activePort = connectedPort;
            selfId = 0;
            selfName = (customNick != null && !customNick.isEmpty()) ? customNick : ("Peer" + (int)(System.currentTimeMillis() % 900 + 100));
            c.send("HELLO 0 " + selfName);
            Thread rt = new Thread(() -> readLoopClient(c), "bc-net-reader");
            rt.setDaemon(true);
            rt.start();
            statusText = "Connected to " + connectedHost + ":" + connectedPort + " as " + selfName;
            return statusText;
         } catch (IOException e) {
            lastErr = e;
         }
      }

      clientConnected = false;
      String errMsg = lastErr != null ? lastErr.getMessage() : "Connection timed out";
      statusText = "Could not connect to " + connectedHost + ":" + connectedPort + " (" + errMsg + ")";
      return "§c" + statusText;
   }

   /** Update 3.19 / 4.0: called from the Multiplayer menu to join a target server or LAN host. */
   public static void joinFromTitle(Game g) {
      joinFromMenu(g, "lan");
   }

   public static void joinFromMenu(Game g, String target) {
      String msg = join(g, target);
      if (clientConnected && g != null) {
         for (int i = 0; i < 180 && inbox.isEmpty(); i++) {
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
      WorldSettings ws = new WorldSettings();
      ws.type = mpWorldType;
      SkyGen.myTeam = 0;
      if (ws.isMinigame()) {
         SkyGen.matchState = SkyGen.STATE_WAITING;
         SkyGen.resetMatchState();
      }
      if (g.world == null || ws.isMinigame() || (g.world.settings != null && g.world.settings.type != mpWorldType)) {
         long seed = ws.isMinigame() ? (System.currentTimeMillis() & 0x7FFFFFFFL) : 20260400L;
         Gen tmpGen = Dim.makeGen(Dim.OVERWORLD, seed, ws);
         int[] sp = tmpGen.findSpawn();
         g.joinMultiplayerWorld(seed, 0, 1000L, sp[0] + 0.5, Math.max(65.0, sp[1]), sp[2] + 0.5, mpWorldType, mpGameMode);
      } else if (g.player != null) {
         g.player.setMode(mpGameMode);
      }

      String msg = host(g, port);
      statusText = Gui.stripCodes(msg);
      if (g.screen != Game.S.LOADING && g.screen != Game.S.WAITING) {
         g.setScreen(ws.isMinigame() ? Game.S.WAITING : Game.S.PLAY);
      }
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
      return isPortListening(host, port, 120);
   }

   private static boolean isPortListening(String host, int port, int timeoutMs) {
      try (Socket s = new Socket()) {
         s.connect(new InetSocketAddress(host, port), Math.max(80, timeoutMs));
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
      int pipe = t.indexOf('|');
      if (pipe > 0) {
         t = t.substring(0, pipe).trim();
      }
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
         ds.setSoTimeout(Math.max(150, timeoutMs));
         byte[] req = DISCOVER_REQ.getBytes(StandardCharsets.UTF_8);
         ds.send(new DatagramPacket(req, req.length, InetAddress.getByName("127.0.0.1"), DISCOVERY_PORT));
         ds.send(new DatagramPacket(req, req.length, InetAddress.getByName("255.255.255.255"), DISCOVERY_PORT));
         try {
            java.util.Enumeration<java.net.NetworkInterface> ifs = java.net.NetworkInterface.getNetworkInterfaces();
            while (ifs != null && ifs.hasMoreElements()) {
               java.net.NetworkInterface ni = ifs.nextElement();
               if (!ni.isUp() || ni.isLoopback()) {
                  continue;
               }
               for (java.net.InterfaceAddress ia : ni.getInterfaceAddresses()) {
                  InetAddress bcast = ia.getBroadcast();
                  if (bcast != null) {
                     try {
                        ds.send(new DatagramPacket(req, req.length, bcast, DISCOVERY_PORT));
                     } catch (IOException ignored) {
                     }
                  }
                  InetAddress addr = ia.getAddress();
                  if (addr instanceof java.net.Inet4Address) {
                     byte[] raw = addr.getAddress();
                     int b0 = raw[0] & 0xFF;
                     if (b0 == 26 || b0 == 25) {
                        // Radmin VPN (26.255.255.255) & Hamachi (25.255.255.255) broadcast
                        try {
                           ds.send(new DatagramPacket(req, req.length, InetAddress.getByName(b0 + ".255.255.255"), DISCOVERY_PORT));
                        } catch (IOException ignored) {
                        }
                     }
                  }
               }
            }
         } catch (Exception ignored) {
         }
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
      clearWorldState();
      ModLoader.restoreSessionMods(g);
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
      if (applyingRemote || w == null || !isActive()) {
         return;
      }
      if (clientConnected && !hosting && w.inPassiveTick) {
         return;
      }

      if (hosting || clientConnected) {
         editsForDim(w.dim).put(pack(x, y, z), id);
         String pkt = "BLOCK " + x + " " + y + " " + z + " " + id + " " + w.dim;
         if (hosting) {
            broadcastExcept(0, pkt);
         } else if (clientConnected && clientConn != null) {
            clientConn.send(pkt);
         }
      }
   }

   public static void applyEditsToChunk(World w, Chunk ch) {
      if (w == null || ch == null || !isActive() || (!clientConnected && w.dir != null)) {
         return;
      }
      ConcurrentHashMap<Long, Integer> dimEdits = editsByDim.get(w.dim);
      if (dimEdits == null || dimEdits.isEmpty()) {
         return;
      }
      boolean changed = false;
      for (Map.Entry<Long, Integer> e : dimEdits.entrySet()) {
         long k = e.getKey();
         int bx = unpackX(k);
         int bz = unpackZ(k);
         if ((bx >> 4) == ch.cx && (bz >> 4) == ch.cz) {
            int by = unpackY(k);
            if (by >= 0 && by <= 255) {
               ch.set(bx & 15, by, bz & 15, e.getValue());
               changed = true;
            }
         }
      }
      if (changed) {
         ch.computeHeightmap();
      }
   }

   public static void sendModsSync() {
      if (!hosting) {
         return;
      }
      broadcastExcept(0, "MODS " + ModLoader.getActiveModsCsv());
   }

   static String formatTilePacket(int dim, TileEntity te) {
      StringBuilder sb = new StringBuilder();
      sb.append("TILE ").append(dim).append(' ')
         .append(te.x).append(' ').append(te.y).append(' ').append(te.z).append(' ')
         .append(te.type).append(' ').append(te.burn).append(' ').append(te.burnMax).append(' ').append(te.cook).append(' ');
      if (te.slots == null || te.slots.length == 0) {
         sb.append('-');
      } else {
         boolean any = false;
         for (int i = 0; i < te.slots.length; i++) {
            Stack s = te.slots[i];
            if (s != null && s.id > 0 && s.n > 0) {
               if (any) {
                  sb.append(',');
               }
               sb.append(i).append(':').append(s.id).append(':').append(s.n).append(':').append(s.dmg);
               any = true;
            }
         }
         if (!any) {
            sb.append('-');
         }
      }
      return sb.toString();
   }

   public static void onTileChanged(World w, TileEntity te) {
      if (applyingRemote || w == null || te == null || !isActive()) {
         return;
      }
      String pkt = formatTilePacket(w.dim, te);
      tilesForDim(w.dim).put(pack(te.x, te.y, te.z), pkt);
      if (hosting) {
         broadcastExcept(0, pkt);
      } else if (clientConnected && clientConn != null) {
         clientConn.send(pkt);
      }
   }

   public static void applyPendingTile(World w, TileEntity te) {
      if (w == null || te == null || !isActive()) {
         return;
      }
      ConcurrentHashMap<Long, String> map = tilesByDim.get(w.dim);
      if (map == null) {
         return;
      }
      String pkt = map.get(pack(te.x, te.y, te.z));
      if (pkt != null) {
         applyTilePacketToEntity(te, pkt.split("\\s+"));
      }
   }

   private static void applyTilePacketToEntity(TileEntity te, String[] p) {
      if (te == null || p.length < 10) {
         return;
      }
      te.burn = Integer.parseInt(p[6]);
      te.burnMax = Integer.parseInt(p[7]);
      te.cook = Integer.parseInt(p[8]);
      if (te.slots != null) {
         java.util.Arrays.fill(te.slots, null);
         String raw = p[9];
         if (!"-".equals(raw)) {
            for (String entry : raw.split(",")) {
               String[] kv = entry.split(":");
               if (kv.length >= 4) {
                  int slot = Integer.parseInt(kv[0]);
                  int id = Integer.parseInt(kv[1]);
                  int n = Integer.parseInt(kv[2]);
                  int dmg = Integer.parseInt(kv[3]);
                  if (slot >= 0 && slot < te.slots.length && id > 0 && n > 0) {
                     te.slots[slot] = new Stack(id, n, dmg);
                  }
               }
            }
         }
      }
   }

   public static void onPrimeTnt(World w, int x, int y, int z, int fuse) {
      if (applyingRemote || w == null || !isActive() || (clientConnected && !hosting && w.inPassiveTick)) {
         return;
      }
      String pkt = "TNT " + w.dim + " " + x + " " + y + " " + z + " " + fuse;
      if (hosting) {
         broadcastExcept(0, pkt);
      } else if (clientConnected && clientConn != null) {
         clientConn.send(pkt);
      }
   }

   public static void onExplosion(World w, double x, double y, double z, int radius) {
      if (applyingRemote || w == null || !hosting) {
         return;
      }
      String pkt = String.format(Locale.ROOT, "EXPLODE %d %.2f %.2f %.2f %d", w.dim, x, y, z, radius);
      broadcastExcept(0, pkt);
   }

   public static void onSpawnArrow(World w, Mobs.Arrow a) {
      if (applyingRemote || w == null || a == null || a.remote || !isActive()) {
         return;
      }
      String pkt = String.format(
         Locale.ROOT,
         "ARROW %d %d %.3f %.3f %.3f %.4f %.4f %.4f %.1f %d",
         selfId,
         w.dim,
         a.x,
         a.y,
         a.z,
         a.vx,
         a.vy,
         a.vz,
         a.damage,
         a.fromPlayer ? 1 : 0
      );
      if (hosting) {
         broadcastExcept(0, pkt);
      } else if (clientConnected && clientConn != null) {
         clientConn.send(pkt);
      }
   }

   public static boolean tryArrowHitPeer(Game g, double x, double y, double z, int dmg, double ax, double az) {
      if (!isActive() || peers.isEmpty() || g == null || g.world == null) {
         return false;
      }
      int wType = g.world.settings != null ? g.world.settings.type : WorldSettings.DEFAULT;
      int myTeam = SkyGen.getPlayerTeam(g);
      for (Peer p : peers.values()) {
         if (p.dim != g.world.dim || p.spectator) {
            continue;
         }
         if (wType == WorldSettings.BEDWARS && p.team == myTeam) {
            continue;
         }
         if (wType == WorldSettings.CS2 && (p.team & 1) == (myTeam & 1)) {
            continue;
         }
         double h = p.sneak ? 1.55 : 1.85;
         if (x >= p.x - 0.42 && x <= p.x + 0.42 && y >= p.y - 0.1 && y <= p.y + h + 0.1 && z >= p.z - 0.42 && z <= p.z + 0.42) {
            sendHit(p.id, Math.max(1, dmg), ax, az, selfName);
            return true;
         }
      }
      return false;
   }

   /**
    * Keeps the host's server socket and all connected peer connections alive when switching worlds or gamemodes,
    * immediately syncing the new world seed, dimension, gamemode, map, and spawn coordinates to all connected players!
    */
   public static void onHostWorldSwitched(Game g) {
      if (!hosting || g == null || g.world == null) {
         return;
      }
      hostGame = g;
      clearWorldState();
      clearPeersEliminated();
      World curW = g.world;
      long seed = curW.seed;
      int dim = curW.dim;
      long dayTime = curW.dayTime;
      int wType = curW.settings != null ? curW.settings.type : mpWorldType;
      int gMode = WorldSettings.isMinigame(wType) ? 0 : (g.player != null ? g.player.rawMode() : mpGameMode);
      int mapId = curW.settings != null ? curW.settings.mapId : 0;

      for (Conn c : hostConns) {
         if (c == null || c.closed) {
            continue;
         }
         Peer peer = peers.get(c.id);
         int assignedTeam = peer != null && peer.team >= 0 ? peer.team : Math.max(0, (c.id - 1) & 3);
         double sx = g.player != null ? g.player.x : 8.5;
         double sy = g.player != null ? g.player.y : 72.0;
         double sz = g.player != null ? g.player.z : 8.5;
         double[] modeSp = SkyGen.multiplayerSpawnFor(
            curW,
            (wType == WorldSettings.BEDWARS || wType == WorldSettings.CS2) ? assignedTeam : (c.id - 1)
         );
         if (modeSp != null) {
            sx = modeSp[0];
            sy = modeSp[1];
            sz = modeSp[2];
         }
         c.send(
            String.format(
               Locale.ROOT,
               "ASSIGN %d %d %d %d %.3f %.3f %.3f %d %d %d %d %d %d %d %d",
               c.id,
               seed,
               dim,
               dayTime,
               sx,
               sy,
               sz,
               wType,
               gMode,
               assignedTeam,
               SkyGen.matchState,
               mapId,
               SkyGen.swKit,
               SkyGen.tumbleWeaponMode,
               SkyGen.soloBots ? 1 : 0
            )
         );
         c.send("TEAM 1 " + SkyGen.myTeam);
      }
      broadcastExcept(0, "CHAT * Host switched server to " + WorldSettings.TYPE_NAME[wType] + "!");
   }

   public static void onClientHitMob(Mobs.Mob m, int dmg, double ax, double az, double kb) {
      if (m == null || m.netId <= 0 || !clientConnected || hosting || clientConn == null) {
         return;
      }
      clientConn.send(String.format(Locale.ROOT, "MOB_HIT %d %d %.3f %.3f %.3f", m.netId, dmg, ax, az, kb));
   }

   public static void onClientInteractMob(Mobs.Mob m, String action, int arg) {
      if (m == null || m.netId <= 0 || !clientConnected || hosting || clientConn == null) {
         return;
      }
      clientConn.send("MOB_ACT " + m.netId + " " + action + " " + arg);
   }

   private static String encodeMobToken(Mobs.Mob m) {
      int flags = (m.sheared ? 1 : 0)
         | (m.tamed ? 2 : 0)
         | (m.charged ? 4 : 0)
         | (m.ignited ? 8 : 0)
         | (m.hurtTime > 0 ? 16 : 0)
         | (m.attackAnim > 0 ? 32 : 0)
         | (m.dead ? 64 : 0)
         | SkyGen.getBotNetFlags(m);
      String tagToken = (m.teamTag == null || m.teamTag.isEmpty())
         ? "-"
         : m.teamTag.replace(':', '~').replace(' ', '_');
      return String.format(
         Locale.ROOT,
         "%d:%d:%.2f:%.2f:%.2f:%.1f:%.1f:%.1f:%d:%d:%d:%d:%d:%d:%d:%s:%d",
         m.netId,
         m.type,
         m.x,
         m.y,
         m.z,
         m.yaw,
         m.headYaw,
         m.headPitch,
         m.health,
         m.color,
         m.babyAge,
         flags,
         m.swell,
         m.size,
         m.variant,
         tagToken,
         SkyGen.getBotGearTier(m)
      );
   }

   private static void sendMobsToConn(Conn c, World w, Mobs mobs) {
      if (c == null || w == null || mobs == null) {
         return;
      }
      for (String pkt : buildMobSnapshots(w, mobs)) {
         c.send(pkt);
      }
   }

   public static void broadcastMobs(World w, Mobs mobs) {
      if (!hosting || hostConns.isEmpty() || w == null || mobs == null) {
         return;
      }
      for (String pkt : buildMobSnapshots(w, mobs)) {
         broadcastExcept(0, pkt);
      }
   }

   private static ArrayList<String> buildMobSnapshots(World w, Mobs mobs) {
      ArrayList<String> out = new ArrayList<>();
      ArrayList<Mobs.Mob> snap = new ArrayList<>(mobs.list);
      int total = snap.size();
      if (total == 0) {
         out.add("MOBS " + w.dim + " 1 0");
         return out;
      }
      int chunkSize = 16;
      int chunks = (total + chunkSize - 1) / chunkSize;
      for (int c = 0; c < chunks; c++) {
         int start = c * chunkSize;
         int end = Math.min(total, start + chunkSize);
         StringBuilder sb = new StringBuilder();
         sb.append("MOBS ").append(w.dim).append(' ').append(c == chunks - 1 ? 1 : 0).append(' ').append(c == 0 ? 1 : 0);
         for (int i = start; i < end; i++) {
            Mobs.Mob m = snap.get(i);
            if (m != null) {
               if (m.netId <= 0) {
                  m.netId = nextMobId();
               }
               sb.append(' ').append(encodeMobToken(m));
            }
         }
         out.add(sb.toString());
      }
      return out;
   }

   public static void sendBedDestroyed(int team, String breaker) {
      if (!isActive()) {
         return;
      }
      String pkt = "BED " + team + " " + sanitizeName(breaker == null ? selfName : breaker);
      if (hosting) {
         broadcastExcept(0, pkt);
      } else if (clientConnected && clientConn != null) {
         clientConn.send(pkt);
      }
   }

   public static void sendTeamSelect(int team) {
      if (!isActive()) {
         return;
      }
      String pkt = "TEAM " + selfId + " " + (Math.abs(team) & 3);
      if (hosting) {
         broadcastExcept(0, pkt);
      } else if (clientConnected && clientConn != null) {
         clientConn.send(pkt);
      }
   }

   public static void sendMatchControl(String action) {
      if (!isActive() || action == null || action.isBlank()) {
         return;
      }
      String pkt = "MATCH " + action.trim();
      if (hosting) {
         broadcastExcept(0, pkt);
      } else if (clientConnected && clientConn != null) {
         clientConn.send(pkt);
      }
   }

   public static void sendEliminated(int team) {
      if (!isActive()) {
         return;
      }
      String pkt = "ELIM " + selfId + " " + sanitizeName(selfName) + " " + (Math.abs(team) & 3);
      if (hosting) {
         broadcastExcept(0, pkt);
      } else if (clientConnected && clientConn != null) {
         clientConn.send(pkt);
      }
   }

   public static void sendTeamUpgrade(int team, int mask, String buyer) {
      if (!isActive()) {
         return;
      }
      String pkt = "UPG " + (Math.abs(team) & 3) + " " + mask + " " + sanitizeName(buyer == null ? selfName : buyer);
      if (hosting) {
         broadcastExcept(0, pkt);
      } else if (clientConnected && clientConn != null) {
         clientConn.send(pkt);
      }
   }

   public static void sendFireball(double x, double y, double z, double vx, double vy, double vz) {
      if (!isActive()) {
         return;
      }
      int dim = hostGame != null && hostGame.world != null ? hostGame.world.dim : 0;
      String pkt = String.format(Locale.ROOT, "FBALL %d %d %.3f %.3f %.3f %.4f %.4f %.4f", selfId, dim, x, y, z, vx, vy, vz);
      if (hosting) {
         broadcastExcept(0, pkt);
      } else if (clientConnected && clientConn != null) {
         clientConn.send(pkt);
      }
   }

   public static boolean enemyPeerNear(World w, int myTeam, double x, double y, double z, double radius) {
      if (w == null || peers.isEmpty()) {
         return false;
      }
      double r2 = radius * radius;
      for (Peer p : peers.values()) {
         if (p.dim == w.dim && !p.spectator && p.team != myTeam) {
            double dx = p.x - x;
            double dy = p.y - y;
            double dz = p.z - z;
            if (dx * dx + dy * dy + dz * dz <= r2) {
               return true;
            }
         }
      }
      return false;
   }

   public static void sendWorldPing(Game g) {
      if (g == null || g.player == null || g.world == null) {
         return;
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
      World.Hit hit = g.world.raycast(ox, oy, oz, dx, dy, dz, 64.0);
      double px;
      double py;
      double pz;
      if (hit != null) {
         px = hit.x + 0.5;
         py = hit.y + 1.0;
         pz = hit.z + 0.5;
      } else {
         px = ox + dx * 16.0;
         py = oy + dy * 16.0;
         pz = oz + dz * 16.0;
      }
      int team = SkyGen.getPlayerTeam(g);
      worldPings.removeIf(wp -> wp.senderId == selfId);
      worldPings.add(new WorldPing(selfId, selfName, team, g.world.dim, px, py, pz, 180));
      g.say(String.format(Locale.ROOT, "§b[Ping] §f%s marked §e(%d, %d, %d)", selfName, (int)Math.floor(px), (int)Math.floor(py), (int)Math.floor(pz)));
      if (isActive()) {
         String pkt = String.format(Locale.ROOT, "WPING %d %s %d %d %.2f %.2f %.2f", selfId, selfName, team, g.world.dim, px, py, pz);
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
      if (applyingRemote || w == null || it == null || it.count <= 0 || !isActive()) {
         return;
      }
      if (clientConnected && !hosting && w.inPassiveTick) {
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
         if (p.dim != g.world.dim || p.spectator) {
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
      if (g.world.settings != null && g.world.settings.type == WorldSettings.BEDWARS && hit.team == SkyGen.getPlayerTeam(g)) {
         g.say("§c[Bedwars] You cannot attack your own teammate!");
         return false;
      }

      int held = g.heldId();
      boolean isTool = Items.isTool(held);
      float baseDmg = isTool ? Items.attackDamage(held) : 1.0F;
      if (g.world.settings != null && g.world.settings.type == WorldSettings.BEDWARS && SkyGen.hasTeamSharpness(SkyGen.getPlayerTeam(g))) {
         baseDmg += 2.0F;
      }
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

   private static final java.util.Set<Integer> seenMobIds = ConcurrentHashMap.newKeySet();

   static void recordServerBlock(World w, int x, int y, int z, int id) {
      int dim = w != null ? w.dim : 0;
      editsForDim(dim).put(pack(x, y, z), id);
      if (w != null) {
         Chunk c = w.chunkAt(x, z);
         if (c != null && y >= 0 && y <= 255) {
            c.set(x & 15, y, z & 15, id);
         }
      }
   }

   static String formatLocalPose(Game g) {
      Player pl = g.player;
      int flags = (pl.sneaking ? 1 : 0)
         | (pl.sprinting ? 2 : 0)
         | (pl.inWater ? 4 : 0)
         | (pl.onGround ? 8 : 0)
         | (pl.hurtTime > 0 ? 16 : 0)
         | ((pl.dead || pl.spectator || SkyGen.localEliminated) ? 32 : 0)
         | (pl.flying ? 64 : 0)
         | (g.eatTicks > 0 ? 128 : 0)
         | (g.bowCharge > 0 ? 256 : 0)
         | (SkyGen.localEliminated ? 512 : 0);
      float sw = g.swinging ? g.swing : -1.0F;
      return String.format(
         Locale.ROOT,
         "POSE %d %s %.3f %.3f %.3f %.2f %.2f %.2f %d %.3f %d %d %d %d %d %d %d %d %d %d",
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
         g.world.dim,
         pl.health,
         g.armorPoints(),
         pl.rawMode(),
         myPingMs,
         SkyGen.myKills,
         SkyGen.myDeaths,
         SkyGen.getPlayerTeam(g)
      );
   }

   // ------------------------------------------------------------------ Per-tick processing & pose sync
   public static void tick(Game g) {
      for (int i = worldPings.size() - 1; i >= 0; i--) {
         WorldPing wp = worldPings.get(i);
         if (--wp.ticks <= 0) {
            worldPings.remove(i);
         }
      }
      processInbox(g != null ? g.world : null, g);
      if (!isActive() || g == null || g.player == null || g.world == null) {
         return;
      }

      // Broadcast our own pose at 20 Hz
      String poseLine = formatLocalPose(g);
      if (hosting) {
         myPingMs = 1;
         broadcastExcept(0, poseLine);
         if (g.tickCount % 40 == 0) {
            broadcastExcept(0, "TIME " + g.world.dayTime);
            if (g.world.weather != null) {
               broadcastExcept(
                  0,
                  "ENV "
                     + g.world.dayTime
                     + " "
                     + g.world.weather.ticks
                     + " "
                     + g.world.weather.forceMode
                     + " "
                     + g.world.weather.forceEnd
                     + " "
                     + (g.world.weather.enabled ? 1 : 0)
               );
            }
         }
      } else if (clientConnected && clientConn != null) {
         clientConn.send(poseLine);
         if (g.tickCount % 40 == 0) {
            clientConn.send("PING " + System.currentTimeMillis());
         }
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
         if (g != null && g.world != null) {
            w = g.world;
         }
         try {
            String[] p = line.split("\\s+");
            if (p.length == 0) {
               continue;
            }

            switch (p[0]) {
               case "ASSIGN" -> {
                  if (p.length >= 5) {
                     clearWorldState();
                     selfId = Integer.parseInt(p[1]);
                     selfName = (customNick != null && !customNick.isEmpty()) ? customNick : ("Player" + selfId);
                     long seed = Long.parseLong(p[2]);
                     int dim = Integer.parseInt(p[3]);
                     long dayTime = Long.parseLong(p[4]);
                     double sx = p.length >= 8 ? Double.parseDouble(p[5]) : 8.5;
                     double sy = p.length >= 8 ? Double.parseDouble(p[6]) : 72.0;
                     double sz = p.length >= 8 ? Double.parseDouble(p[7]) : 8.5;
                     int wType = p.length >= 9 ? Integer.parseInt(p[8]) : WorldSettings.DEFAULT;
                     int gMode = p.length >= 10 ? Integer.parseInt(p[9]) : 1;
                     int assignedTeam = p.length >= 11 ? Integer.parseInt(p[10]) : Math.max(0, (selfId - 1) & 3);
                     int hostMatchState = p.length >= 12 ? Integer.parseInt(p[11]) : SkyGen.STATE_PLAYING;
                     int hostMapId = p.length >= 13 ? Integer.parseInt(p[12]) : 0;
                     if (p.length >= 14) {
                        SkyGen.swKit = Math.max(0, Math.min(3, Integer.parseInt(p[13])));
                     }
                     if (p.length >= 15) {
                        SkyGen.tumbleWeaponMode = Math.max(0, Math.min(2, Integer.parseInt(p[14])));
                     }
                     if (p.length >= 16) {
                        SkyGen.soloBots = Integer.parseInt(p[15]) != 0;
                     }
                     SkyGen.myTeam = Math.abs(assignedTeam) & 3;
                     SkyGen.matchState = hostMatchState;
                     if (g != null) {
                        g.joinMultiplayerWorld(seed, dim, dayTime, sx, sy, sz, wType, gMode);
                        w = g.world;
                        if (w != null && w.settings != null && w.settings.mapId != hostMapId) {
                           w.settings.mapId = hostMapId;
                           SkyGen.resetArenaMap(g, w, false);
                        }
                     } else if (w != null) {
                        w.dayTime = dayTime;
                     }

                     if (clientConn != null) {
                        clientConn.send("HELLO " + selfId + " " + selfName);
                        clientConn.send("TEAM " + selfId + " " + SkyGen.myTeam);
                        if (g != null && g.player != null && g.world != null) {
                           clientConn.send(formatLocalPose(g));
                        }
                     }
                  }
               }
               case "PONG" -> {
                  if (p.length >= 2) {
                     long sent = Long.parseLong(p[1]);
                     myPingMs = (int)Math.max(1L, Math.min(999L, System.currentTimeMillis() - sent));
                  }
               }
               case "UPG" -> {
                  if (p.length >= 3) {
                     int team = Integer.parseInt(p[1]);
                     int mask = Integer.parseInt(p[2]);
                     String buyer = p.length >= 4 ? p[3] : "Teammate";
                     SkyGen.setTeamUpgradeMask(g, team, mask, buyer);
                  }
               }
               case "FBALL" -> {
                  if (p.length >= 9 && w != null && g != null && g.mobs != null) {
                     int sender = Integer.parseInt(p[1]);
                     int fdim = Integer.parseInt(p[2]);
                     if (sender != selfId && w.dim == fdim) {
                        Mobs312.Fireball fb = new Mobs312.Fireball();
                        fb.big = true;
                        fb.fromPlayer = true;
                        fb.remote = true;
                        fb.x = fb.px = fb.lx = Double.parseDouble(p[3]);
                        fb.y = fb.py = fb.ly = Double.parseDouble(p[4]);
                        fb.z = fb.pz = fb.lz = Double.parseDouble(p[5]);
                        fb.vx = Double.parseDouble(p[6]);
                        fb.vy = Double.parseDouble(p[7]);
                        fb.vz = Double.parseDouble(p[8]);
                        g.mobs.fireballs.add(fb);
                     }
                  }
               }
               case "WPING" -> {
                  if (p.length >= 8 && g != null) {
                     int sid = Integer.parseInt(p[1]);
                     if (sid != selfId) {
                        String sname = p[2];
                        int steam = Integer.parseInt(p[3]);
                        int sdim = Integer.parseInt(p[4]);
                        double px = Double.parseDouble(p[5]);
                        double py = Double.parseDouble(p[6]);
                        double pz = Double.parseDouble(p[7]);
                        worldPings.removeIf(wp -> wp.senderId == sid);
                        worldPings.add(new WorldPing(sid, sname, steam, sdim, px, py, pz, 180));
                        g.say(String.format(Locale.ROOT, "§b[Ping] §f%s marked §e(%d, %d, %d)", sname, (int)Math.floor(px), (int)Math.floor(py), (int)Math.floor(pz)));
                     }
                  }
               }
               case "KILL" -> {
                  if (p.length >= 3 && g != null) {
                     String killer = p[1];
                     String victim = p[2];
                     if (killer.equalsIgnoreCase(selfName)) {
                        SkyGen.onArenaKill(g, victim);
                     }
                  }
               }
               case "MODS" -> {
                  String csv = p.length >= 2 ? p[1] : "-";
                  ModLoader.applySyncedModsCsv(csv, g);
               }
               case "ENV" -> {
                  if (p.length >= 6 && w != null && !hosting) {
                     w.dayTime = Long.parseLong(p[1]);
                     if (w.weather != null) {
                        w.weather.ticks = Long.parseLong(p[2]);
                        w.weather.forceMode = Integer.parseInt(p[3]);
                        w.weather.forceEnd = Long.parseLong(p[4]);
                        w.weather.enabled = Integer.parseInt(p[5]) != 0;
                     }
                  }
               }
               case "BED" -> {
                  if (p.length >= 2) {
                     int team = Integer.parseInt(p[1]);
                     String breaker = p.length >= 3 ? p[2] : "";
                     SkyGen.onBedDestroyed(g, w, team, breaker, false);
                  }
               }
               case "TEAM" -> {
                  if (p.length >= 3) {
                     int pid = Integer.parseInt(p[1]);
                     int team = Math.abs(Integer.parseInt(p[2])) & 3;
                     if (pid != selfId) {
                        Peer peer = peers.computeIfAbsent(pid, k -> new Peer(k, "Player" + k));
                        peer.team = team;
                     }
                  }
               }
               case "ELIM" -> {
                  if (p.length >= 3) {
                     int pid = Integer.parseInt(p[1]);
                     String pname = p[2];
                     int pteam = p.length >= 4 ? (Math.abs(Integer.parseInt(p[3])) & 3) : 0;
                     if (pid != selfId) {
                        Peer peer = peers.computeIfAbsent(pid, k -> new Peer(k, pname));
                        peer.eliminated = true;
                        peer.spectator = true;
                        if (g != null && w != null && w.settings != null) {
                           if (w.settings.type == WorldSettings.BEDWARS) {
                              g.say("§e§l[FINAL KILL] " + SkyGen.BEDWARS_CODES[pteam] + pname + " §7was permanently eliminated!");
                           } else {
                              g.say("§c[Eliminated] §e" + pname + " §7is out for this round and is now spectating!");
                           }
                        }
                     }
                  }
               }
               case "MATCH" -> {
                  if (p.length >= 2 && g != null) {
                     String act = p[1].toUpperCase(Locale.ROOT);
                     if ("START".equals(act)) {
                        clearPeersEliminated();
                        if (p.length >= 3 && w != null && w.settings != null) {
                           w.settings.mapId = Math.abs(Integer.parseInt(p[2])) & 3;
                        }
                        if (p.length >= 4) {
                           SkyGen.swKit = Math.max(0, Math.min(3, Integer.parseInt(p[3])));
                        }
                        if (p.length >= 5) {
                           SkyGen.tumbleWeaponMode = Math.max(0, Math.min(2, Integer.parseInt(p[4])));
                        }
                        if (p.length >= 6) {
                           SkyGen.soloBots = Integer.parseInt(p[5]) != 0;
                        }
                        SkyGen.startMatch(g, false);
                     } else if ("RESET".equals(act)) {
                        clearPeersEliminated();
                        if (p.length >= 3 && w != null && w.settings != null) {
                           w.settings.mapId = Math.abs(Integer.parseInt(p[2])) & 3;
                        }
                        SkyGen.resetArenaMap(g, w, false);
                     } else if ("OVERVIEW".equals(act)) {
                        String champ = p.length >= 3 ? p[2] : "Champion";
                        String modeName = p.length >= 4 ? p[3] : "Battle";
                        String mapName = p.length >= 5 ? p[4] : "Cove";
                        int durSec = p.length >= 6 ? Integer.parseInt(p[5]) : 45;
                        SkyGen.onRemoteMatchOverview(g, champ, modeName, mapName, durSec);
                     } else if ("WAIT".equals(act) || "LOBBY".equals(act)) {
                        clearPeersEliminated();
                        if (p.length >= 3 && w != null && w.settings != null) {
                           w.settings.mapId = Math.abs(Integer.parseInt(p[2])) & 3;
                           SkyGen.resetArenaMap(g, w, false);
                        }
                        SkyGen.showingOverview = false;
                        SkyGen.matchState = SkyGen.STATE_WAITING;
                        SkyGen.lobbyCountdownTicks = 240;
                        g.setScreen(Game.S.WAITING);
                     } else if ("VOTE".equals(act) && p.length >= 4) {
                        int voterId = Integer.parseInt(p[2]);
                        int votedMap = Integer.parseInt(p[3]);
                        if (voterId != selfId) {
                           SkyGen.onRemoteMapVote(voterId, votedMap);
                        }
                     } else if ("MODE".equals(act) && p.length >= 3) {
                        int newMode = Integer.parseInt(p[2]);
                        int newMap = p.length >= 4 ? (Math.abs(Integer.parseInt(p[3])) & 3) : 0;
                        if (w != null && w.settings != null) {
                           w.settings.mapId = newMap;
                        }
                        SkyGen.switchMinigameMode(g, newMode, false);
                     } else if ("BOMB_PLANT".equals(act) && p.length >= 3) {
                        int site = Integer.parseInt(p[2]) & 1;
                        String planter = p.length >= 4 ? p[3] : "Terrorist";
                        SkyGen.cs2BombPlanted = true;
                        SkyGen.cs2BombSite = site;
                        SkyGen.cs2BombTimerTicks = 800;
                        SkyGen.cs2DefuseTicks = 0;
                        SoundEngine.playMinigameSound("mg_showdown");
                        g.say("\u00a7c\u00a7l[CS2 Strike] BOMB HAS BEEN PLANTED AT SITE " + (site == 0 ? "A" : "B") + " \u00a77by \u00a7e" + planter + "\u00a77!");
                     } else if ("BOMB_DEFUSE".equals(act)) {
                        String defuser = p.length >= 3 ? p[2] : "Counter-Terrorist";
                        SkyGen.cs2BombPlanted = false;
                        SkyGen.cs2RoundOver = true;
                        g.say("\u00a7b\u00a7l[CS2 Strike] BOMB HAS BEEN DEFUSED \u00a77by \u00a7a" + defuser + "\u00a77! Counter-Terrorists Win!");
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
                  if (p.length >= 5) {
                     int bx = Integer.parseInt(p[1]);
                     int by = Integer.parseInt(p[2]);
                     int bz = Integer.parseInt(p[3]);
                     int bid = Integer.parseInt(p[4]);
                     int bdim = p.length >= 6 ? Integer.parseInt(p[5]) : 0;
                     editsForDim(bdim).put(pack(bx, by, bz), bid);
                     if (w != null && w.dim == bdim) {
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
               }
               case "TILE" -> {
                  if (p.length >= 10) {
                     int tdim = Integer.parseInt(p[1]);
                     int tx = Integer.parseInt(p[2]);
                     int ty = Integer.parseInt(p[3]);
                     int tz = Integer.parseInt(p[4]);
                     tilesForDim(tdim).put(pack(tx, ty, tz), line);
                     if (w != null && w.dim == tdim) {
                        applyingRemote = true;
                        try {
                           TileEntity te = w.getOrCreateTile(tx, ty, tz);
                           applyTilePacketToEntity(te, p);
                        } finally {
                           applyingRemote = false;
                        }
                     }
                  }
               }
               case "MOBS" -> {
                  if (p.length >= 4 && !hosting) {
                     hostHasMobSync = true;
                     int mdim = Integer.parseInt(p[1]);
                     boolean isLast = Integer.parseInt(p[2]) != 0;
                     boolean isFirst = Integer.parseInt(p[3]) != 0;
                     if (w != null && g != null && g.mobs != null && w.dim == mdim) {
                        if (isFirst) {
                           seenMobIds.clear();
                        }
                        for (int i = 4; i < p.length; i++) {
                           String[] f = p[i].split(":");
                           if (f.length >= 15) {
                              int nid = Integer.parseInt(f[0]);
                              int mtype = Integer.parseInt(f[1]);
                              double mx = Double.parseDouble(f[2]);
                              double my = Double.parseDouble(f[3]);
                              double mz = Double.parseDouble(f[4]);
                              float myaw = Float.parseFloat(f[5]);
                              float mhyaw = Float.parseFloat(f[6]);
                              float mhpitch = Float.parseFloat(f[7]);
                              int mhp = Integer.parseInt(f[8]);
                              int mcol = Integer.parseInt(f[9]);
                              int mbaby = Integer.parseInt(f[10]);
                              int mflags = Integer.parseInt(f[11]);
                              int mswell = Integer.parseInt(f[12]);
                              int msize = Integer.parseInt(f[13]);
                              int mvar = Integer.parseInt(f[14]);
                              seenMobIds.add(nid);
                              Mobs.Mob m = g.mobs.findByNetId(nid);
                              if (m == null) {
                                 m = new Mobs.Mob();
                                 m.type = mtype;
                                 m.netId = nid;
                                 m.remoteSynced = true;
                                 m.setPos(mx, my, mz);
                                 m.px = mx;
                                 m.py = my;
                                 m.pz = mz;
                                 m.tx = mx;
                                 m.ty = my;
                                 m.tz = mz;
                                 m.yaw = m.pyaw = myaw;
                                 m.headYaw = m.pheadYaw = mhyaw;
                                 m.headPitch = m.pheadPitch = mhpitch;
                                 g.mobs.list.add(m);
                              }
                              m.type = mtype;
                              m.remoteSynced = true;
                              if (Math.hypot(mx - m.x, mz - m.z) > 6.0 || Math.abs(my - m.y) > 6.0) {
                                 m.setPos(mx, my, mz);
                                 m.px = mx;
                                 m.py = my;
                                 m.pz = mz;
                              }
                              m.tx = mx;
                              m.ty = my;
                              m.tz = mz;
                              m.yaw = myaw;
                              m.headYaw = mhyaw;
                              m.headPitch = mhpitch;
                              m.health = mhp;
                              m.color = mcol;
                              m.babyAge = mbaby;
                              m.sheared = (mflags & 1) != 0;
                              m.tamed = (mflags & 2) != 0;
                              m.charged = (mflags & 4) != 0;
                              m.ignited = (mflags & 8) != 0;
                              if ((mflags & 16) != 0 && m.hurtTime == 0) {
                                 m.hurtTime = 6;
                              }
                              if ((mflags & 32) != 0 && m.attackAnim == 0) {
                                 m.attackAnim = 6;
                              }
                              m.dead = (mflags & 64) != 0;
                              m.swell = mswell;
                              m.size = msize;
                              m.variant = mvar;
                              if (f.length >= 16) {
                                 m.teamTag = "-".equals(f[15]) ? null : f[15].replace('~', ':').replace('_', ' ');
                              }
                              int gTier = f.length >= 17 ? Integer.parseInt(f[16]) : 0;
                              SkyGen.applyRemoteBotState(m, mflags, gTier);
                           }
                        }
                        if (isLast) {
                           g.mobs.list.removeIf(m -> m == null || !seenMobIds.contains(m.netId));
                        }
                     }
                  }
               }
               case "MOB_HIT" -> {
                  if (p.length >= 6 && hosting && w != null && g != null && g.mobs != null) {
                     int nid = Integer.parseInt(p[1]);
                     int dmg = Integer.parseInt(p[2]);
                     double ax = Double.parseDouble(p[3]);
                     double az = Double.parseDouble(p[4]);
                     double kb = Double.parseDouble(p[5]);
                     Mobs.Mob m = g.mobs.findByNetId(nid);
                     if (m != null) {
                        g.mobs.hurtMob(m, dmg, ax, az, kb, true);
                        broadcastMobs(w, g.mobs);
                     }
                  }
               }
               case "MOB_ACT" -> {
                  if (p.length >= 4 && hosting && w != null && g != null && g.mobs != null) {
                     int nid = Integer.parseInt(p[1]);
                     String act = p[2];
                     int arg = Integer.parseInt(p[3]);
                     Mobs.Mob m = g.mobs.findByNetId(nid);
                     if (m != null) {
                        applyingRemote = true;
                        try {
                           if ("IGNITE".equals(act)) {
                              g.mobs.ignite(m);
                           } else if ("SHEAR".equals(act)) {
                              g.mobs.shear(m);
                           } else if ("FEED".equals(act)) {
                              g.mobs.feed(m, arg);
                           }
                        } finally {
                           applyingRemote = false;
                        }
                        broadcastMobs(w, g.mobs);
                     }
                  }
               }
               case "ARROW" -> {
                  if (p.length >= 11 && w != null && g != null && g.mobs != null) {
                     int sender = Integer.parseInt(p[1]);
                     int adim = Integer.parseInt(p[2]);
                     if (sender != selfId && w.dim == adim) {
                        Mobs.Arrow a = new Mobs.Arrow();
                        a.remote = true;
                        a.x = a.px = Double.parseDouble(p[3]);
                        a.y = a.py = Double.parseDouble(p[4]);
                        a.z = a.pz = Double.parseDouble(p[5]);
                        a.vx = Double.parseDouble(p[6]);
                        a.vy = Double.parseDouble(p[7]);
                        a.vz = Double.parseDouble(p[8]);
                        a.damage = Float.parseFloat(p[9]);
                        a.fromPlayer = Integer.parseInt(p[10]) != 0;
                        g.mobs.arrows.add(a);
                     }
                  }
               }
               case "TNT" -> {
                  if (p.length >= 6 && w != null) {
                     int tdim = Integer.parseInt(p[1]);
                     int tx = Integer.parseInt(p[2]);
                     int ty = Integer.parseInt(p[3]);
                     int tz = Integer.parseInt(p[4]);
                     int tfuse = Integer.parseInt(p[5]);
                     if (w.dim == tdim) {
                        applyingRemote = true;
                        try {
                           w.primeTnt(tx, ty, tz, tfuse);
                        } finally {
                           applyingRemote = false;
                        }
                     }
                  }
               }
               case "EXPLODE" -> {
                  if (p.length >= 6 && w != null && g != null && !hosting) {
                     int edim = Integer.parseInt(p[1]);
                     double ex = Double.parseDouble(p[2]);
                     double ey = Double.parseDouble(p[3]);
                     double ez = Double.parseDouble(p[4]);
                     if (w.dim == edim) {
                        g.explosionParticles(ex, ey, ez);
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
                     peer.spectator = (flags & 32) != 0;
                     peer.fly = (flags & 64) != 0;
                     peer.eat = (flags & 128) != 0 ? 0.5F : 0.0F;
                     peer.bow = (flags & 256) != 0 ? 0.85F : 0.0F;
                     peer.eliminated = (flags & 512) != 0;
                     peer.swing = Float.parseFloat(p[10]);
                     peer.heldId = Integer.parseInt(p[11]);
                     peer.skinIdx = Integer.parseInt(p[12]);
                     peer.dim = p.length >= 14 ? Integer.parseInt(p[13]) : 0;
                     if (p.length >= 15) {
                        peer.health = Integer.parseInt(p[14]);
                     }
                     if (p.length >= 16) {
                        peer.armorPts = Integer.parseInt(p[15]);
                     }
                     if (p.length >= 17) {
                        peer.gameMode = Integer.parseInt(p[16]);
                     }
                     if (p.length >= 18) {
                        peer.pingMs = Integer.parseInt(p[17]);
                     }
                     if (p.length >= 19) {
                        peer.kills = Integer.parseInt(p[18]);
                     }
                     if (p.length >= 20) {
                        peer.deaths = Integer.parseInt(p[19]);
                     }
                     if (p.length >= 21) {
                        peer.team = Math.abs(Integer.parseInt(p[20])) & 3;
                     }
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
                           SkyGen.myDeaths++;
                           String deathMsg = selfName + " was slain by " + atkName;
                           g.say("§e" + deathMsg);
                           String chatPkt = "CHAT * " + deathMsg;
                           String killPkt = "KILL " + sanitizeName(atkName) + " " + sanitizeName(selfName);
                           if (hosting) {
                              inbox.add(killPkt);
                              broadcastExcept(0, chatPkt);
                              broadcastExcept(0, killPkt);
                           } else if (clientConnected && clientConn != null) {
                              clientConn.send(chatPkt);
                              clientConn.send(killPkt);
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

   private static float wrapDeg(float a) {
      while (a > 180.0F) {
         a -= 360.0F;
      }
      while (a < -180.0F) {
         a += 360.0F;
      }
      return a;
   }

   // ------------------------------------------------------------------ 3D rendering of remote peers & tactical pings
   static PlayerModel.Pose pose(Peer p, float partial) {
      PlayerModel.Pose out = new PlayerModel.Pose();
      out.bodyYaw = p.pyaw + wrapDeg(p.yaw - p.pyaw) * partial;
      float hy = p.pheadYaw + wrapDeg(p.headYaw - p.pheadYaw) * partial;
      out.headYaw = Math.max(-55.0F, Math.min(55.0F, wrapDeg(hy - out.bodyYaw)));
      out.headPitch = p.ppitch + (p.pitch - p.ppitch) * partial;
      out.limb = p.limb;
      out.amp = p.pamp + (p.amp - p.pamp) * partial;
      out.swing = p.swing;
      out.eat = p.eat;
      out.bow = p.bow;
      out.sneak = p.sneak;
      out.sprint = p.sprint;
      out.fly = p.fly ? 1.0F : 0.0F;
      out.air = !p.onGround && !p.fly;
      out.sneakK = p.sneak ? 1.0F : 0.0F;
      out.sprintK = p.sprint ? 1.0F : 0.0F;
      out.airK = (p.onGround || p.fly) ? 0.0F : 1.0F;
      out.swim = p.swim ? 1.0F : 0.0F;
      out.hurt = p.hurt ? 1.0F : 0.0F;
      out.age = p.age + partial;
      out.held = p.heldId;
      return out;
   }

   static synchronized int[] skinPixels(int idx) {
      if (peerSkins == null) {
         peerSkins = new int[][]{Skins.defaultPixels(), Skins.demoSkin()};
      }

      return peerSkins[Math.abs(idx) % peerSkins.length];
   }

   static void drawPeers(Renderer r, float partial) {
      if (r == null || r.g == null || r.g.world == null) {
         return;
      }
      if (peers.isEmpty() && worldPings.isEmpty()) {
         return;
      }

      World w = r.g.world;
      boolean isMinigame = w.settings != null && w.settings.isMinigame();
      GL11.glEnable(2929);
      GL11.glDepthMask(true);
      GL11.glDisable(2884);
      for (Peer p : peers.values()) {
         if (p.dim != w.dim || (p.spectator && (r.g.player == null || !r.g.player.spectator))) {
            continue;
         }

         double x = p.px + (p.x - p.px) * partial;
         double y = p.py + (p.y - p.py) * partial;
         double z = p.pz + (p.z - p.pz) * partial;
         float bright = r.brightnessAt(w, x, y + 1.0, z);
         GL11.glPushMatrix();
         GL11.glTranslated(x - r.camX, y - r.camY, z - r.camZ);
         PlayerModel.draw(skinPixels(p.skinIdx), false, pose(p, partial), bright);
         String tag = p.name;
         int col = 0xFFFFFF;
         if (isMinigame && p.team >= 0 && p.team < 4) {
            String[] badge = new String[]{"[RED] ", "[BLU] ", "[GRN] ", "[YEL] "};
            int[] rgb = new int[]{0xFF5555, 0x55AAFF, 0x55FF55, 0xFFFF55};
            tag = badge[p.team] + p.name;
            col = rgb[p.team];
         }
         if (p.gameMode != 1 && p.gameMode != 3) {
            tag = tag + " (" + Math.max(0, p.health) + "HP)";
         }
         drawNameTag(r, tag, p.sneak ? 1.98F : 2.16F, col, p.gameMode != 1 && p.gameMode != 3 ? (p.health / 20.0F) : -1.0F);
         GL11.glPopMatrix();
      }

      for (WorldPing wp : worldPings) {
         if (wp.dim != w.dim) {
            continue;
         }
         drawWorldPingBeacon(r, wp);
      }

      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
   }

   private static void drawWorldPingBeacon(Renderer r, WorldPing wp) {
      boolean wasEnt = r.sh != null && r.sh.entityOn;
      if (wasEnt) {
         r.sh.stopEntity();
      }
      int[] rgb = new int[]{0xFF5555, 0x55AAFF, 0x55FF55, 0xFFFF55};
      int c = wp.team >= 0 && wp.team < 4 ? rgb[wp.team] : 0x55FFFF;
      float cr = ((c >> 16) & 0xFF) / 255.0F;
      float cg = ((c >> 8) & 0xFF) / 255.0F;
      float cb = (c & 0xFF) / 255.0F;
      GL11.glPushMatrix();
      GL11.glTranslated(wp.x - r.camX, wp.y - r.camY, wp.z - r.camZ);
      GL11.glDisable(3553);
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      GL11.glDepthMask(false);
      float w = 0.14F;
      float h = 14.0F;
      GL11.glColor4f(cr, cg, cb, 0.48F);
      GL11.glBegin(7);
      GL11.glVertex3f(-w, 0.0F, -w);
      GL11.glVertex3f(w, 0.0F, -w);
      GL11.glVertex3f(w, h, -w);
      GL11.glVertex3f(-w, h, -w);
      GL11.glVertex3f(-w, 0.0F, w);
      GL11.glVertex3f(w, 0.0F, w);
      GL11.glVertex3f(w, h, w);
      GL11.glVertex3f(-w, h, w);
      GL11.glVertex3f(-w, 0.0F, -w);
      GL11.glVertex3f(-w, 0.0F, w);
      GL11.glVertex3f(-w, h, w);
      GL11.glVertex3f(-w, h, -w);
      GL11.glVertex3f(w, 0.0F, -w);
      GL11.glVertex3f(w, 0.0F, w);
      GL11.glVertex3f(w, h, w);
      GL11.glVertex3f(w, h, -w);
      GL11.glEnd();
      GL11.glDepthMask(true);
      GL11.glDisable(3042);
      GL11.glEnable(3553);
      int dist = (int)Math.round(Math.sqrt((wp.x - r.camX) * (wp.x - r.camX) + (wp.y - r.camY) * (wp.y - r.camY) + (wp.z - r.camZ) * (wp.z - r.camZ)));
      drawNameTag(r, "[! " + wp.senderName + " " + dist + "m]", 2.4F, c, -1.0F);
      GL11.glPopMatrix();
      if (wasEnt) {
         r.sh.useEntity(r.mv, r.camX, r.camY, r.camZ, r.entFogNear, r.entFogFar, r.entFogCol, r.entShadow, r.lastSky, r.lastBlk);
      }
   }

   static void drawNameTag(Renderer r, String name, float yOff, int rgbColor, float hpFrac) {
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
      float boxH = hpFrac >= 0.0F ? 13.0F : 9.0F;
      GL11.glColor4f(0.0F, 0.0F, 0.0F, 0.48F);
      GL11.glBegin(7);
      GL11.glVertex3f(x0 - 2.0F, -1.0F, 0.01F);
      GL11.glVertex3f(x0 + tw + 2.0F, -1.0F, 0.01F);
      GL11.glVertex3f(x0 + tw + 2.0F, boxH, 0.01F);
      GL11.glVertex3f(x0 - 2.0F, boxH, 0.01F);
      GL11.glEnd();
      if (hpFrac >= 0.0F) {
         float f = Math.max(0.0F, Math.min(1.0F, hpFrac));
         float barW = Math.max(24.0F, tw);
         float bx0 = -barW * 0.5F;
         GL11.glColor4f(0.2F, 0.05F, 0.05F, 0.8F);
         GL11.glBegin(7);
         GL11.glVertex3f(bx0, 9.5F, 0.005F);
         GL11.glVertex3f(bx0 + barW, 9.5F, 0.005F);
         GL11.glVertex3f(bx0 + barW, 11.8F, 0.005F);
         GL11.glVertex3f(bx0, 11.8F, 0.005F);
         GL11.glEnd();
         GL11.glColor4f(f > 0.5F ? 0.2F : 0.95F, f > 0.25F ? 0.9F : 0.2F, 0.2F, 0.92F);
         GL11.glBegin(7);
         GL11.glVertex3f(bx0, 9.5F, 0.0F);
         GL11.glVertex3f(bx0 + barW * f, 9.5F, 0.0F);
         GL11.glVertex3f(bx0 + barW * f, 11.8F, 0.0F);
         GL11.glVertex3f(bx0, 11.8F, 0.0F);
         GL11.glEnd();
      }
      GL11.glEnable(3553);
      GL11.glBindTexture(3553, Tex.fontTex);
      float cr = ((rgbColor >> 16) & 0xFF) / 255.0F;
      float cg = ((rgbColor >> 8) & 0xFF) / 255.0F;
      float cb = (rgbColor & 0xFF) / 255.0F;
      GL11.glColor4f(cr, cg, cb, 0.96F);
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

   // ------------------------------------------------------------------ V4.1: Multiplayer Commands & Tab Scoreboard Overlay
   public static boolean onNetCommand(Game g, String cmd) {
      if (g == null || cmd == null) {
         return false;
      }
      String[] parts = cmd.trim().split("\\s+", 3);
      if (parts.length == 0) {
         return false;
      }
      String root = parts[0].toLowerCase(Locale.ROOT);
      if ("/nick".equals(root) || "/name".equals(root)) {
         if (parts.length >= 2 && !parts[1].isBlank()) {
            setNickname(g, parts[1]);
         } else {
            cyclePresetNick();
            g.say("§a[Multiplayer] §fYour nickname is now §e" + selfName + " §7(Usage: /nick <name>)");
         }
         return true;
      }
      if ("/ping".equals(root)) {
         sendWorldPing(g);
         g.say("§a[Network] §fLatency: §e" + myPingMs + " ms §7| Role: " + (hosting ? "Host" : (clientConnected ? "Client" : "Local")));
         return true;
      }
      if ("/list".equals(root) || "/players".equals(root) || "/who".equals(root)) {
         StringBuilder sb = new StringBuilder();
         sb.append("§a[Players (").append(1 + peers.size()).append(")] §f").append(selfName).append(" (You, ").append(myPingMs).append("ms)");
         for (Peer p : peers.values()) {
            sb.append(", §e").append(p.name).append(" §7(").append(p.health).append("HP, ").append(p.pingMs).append("ms)");
         }
         g.say(sb.toString());
         return true;
      }
      if ("/tp".equals(root) && parts.length == 2) {
         String targetName = parts[1].trim();
         for (Peer p : peers.values()) {
            if (p.name.equalsIgnoreCase(targetName)) {
               if (g.world != null && p.dim != g.world.dim) {
                  g.switchDimension(p.dim, p.x, p.z, 1);
               }
               if (g.player != null) {
                  g.player.setPos(p.x, p.y, p.z);
                  g.player.vx = g.player.vy = g.player.vz = 0.0;
                  g.player.fallDistance = 0.0F;
               }
               g.say("§aTeleported to " + p.name + " (" + (int)p.x + ", " + (int)p.y + ", " + (int)p.z + ")");
               return true;
            }
         }
      }
      if (("/msg".equals(root) || "/w".equals(root) || "/tell".equals(root)) && parts.length >= 3) {
         String targetName = parts[1].trim();
         String text = parts[2].trim();
         g.say("§d[You -> " + targetName + "] §f" + text);
         sendChat(g, "§d[Whisper to " + targetName + "] §f" + text);
         return true;
      }
      return false;
   }

   private static String shortModeBadge(int rawMode) {
      return switch (rawMode) {
         case 1 -> "§bCREA";
         case 2 -> "§eADV";
         case 3 -> "§7SPEC";
         case 4 -> "§cHARD";
         default -> "§aSURV";
      };
   }

   /** Draws the Multiplayer &amp; Minigame Tab Scoreboard overlay when the player holds TAB in-game. */
   public static void drawTabOverlay(Game g) {
      if (g == null || g.gui == null || g.world == null || g.player == null) {
         return;
      }
      Gui gui = g.gui;
      World w = g.world;
      int wType = w.settings != null ? w.settings.type : WorldSettings.DEFAULT;
      boolean isBedwars = wType == WorldSettings.BEDWARS;
      boolean isSkywars = wType == WorldSettings.SKYWARS;
      boolean isMinigame = w.settings != null && w.settings.isMinigame();

      int rows = 1 + peers.size();
      int extraBots = 0;
      if (isMinigame && g.mobs != null) {
         for (Mobs.Mob m : g.mobs.list) {
            if (m != null && !m.dead && m.teamTag != null && m.teamTag.startsWith("Team:")) {
               extraBots++;
            }
         }
      }
      int totalRows = Math.min(10, rows + extraBots);
      float boxW = 316.0F;
      float headerH = isBedwars ? 38.0F : 26.0F;
      float boxH = headerH + 14.0F + totalRows * 12.0F + 14.0F;
      float x0 = (gui.w - boxW) * 0.5F;
      float y0 = 18.0F;

      gui.rect(x0, y0, boxW, boxH, -1056964608);
      gui.rect(x0, y0, boxW, 14.0F, -1040187392);

      String title = isBedwars
         ? "§c§lBEDWARS §fARENA SCOREBOARD"
         : (isSkywars
            ? "§6§lBATTLE §fARENA SCOREBOARD"
            : (wType == WorldSettings.TUMBLE ? "§b§lTUMBLE §fARENA SCOREBOARD" : "§a§lBLOCKCRAFT §fMULTIPLAYER SCOREBOARD"));
      String roleStr = hosting
         ? ("§aHosting (" + (1 + peers.size()) + " online)")
         : (clientConnected ? ("§bConnected (" + myPingMs + "ms)") : "§7Local Session");
      gui.text(title + "  §7— " + roleStr, x0 + 6.0F, y0 + 3.0F, -1, true);

      String sub = "§7World: §f" + WorldSettings.TYPE_NAME[wType]
         + "  §7| Mode: " + shortModeBadge(g.player.rawMode())
         + (shareCode != null && !shareCode.isBlank() ? ("  §7| Code: §b" + shareCode) : "");
      gui.text(sub, x0 + 6.0F, y0 + 15.0F, -1, true);

      if (isBedwars) {
         StringBuilder bw = new StringBuilder("§7Teams: ");
         for (int t = 0; t < 4; t++) {
            boolean bed = SkyGen.isTeamBedIntact(w, t);
            int upg = SkyGen.teamUpgrades[t];
            String uStr = (upg != 0)
               ? ("+" + ((upg & 1) != 0 ? "S" : "") + ((upg & 2) != 0 ? "P" : "") + ((upg & 4) != 0 ? "F" : "") + ((upg & 8) != 0 ? "H" : ""))
               : "";
            bw.append(SkyGen.BEDWARS_CODES[t])
               .append(SkyGen.BEDWARS_NAMES[t])
               .append(bed ? "§a[BED]" : "§c[X]")
               .append("§b")
               .append(uStr)
               .append("  ");
         }
         gui.text(bw.toString(), x0 + 6.0F, y0 + 26.0F, -1, true);
      }

      float colY = y0 + headerH;
      gui.rect(x0 + 4.0F, colY, boxW - 8.0F, 12.0F, 1073741824);
      gui.text("§ePlayer", x0 + 8.0F, colY + 2.0F, -1, true);
      gui.text("§eMode", x0 + 118.0F, colY + 2.0F, -1, true);
      gui.text("§eHP / Armor", x0 + 162.0F, colY + 2.0F, -1, true);
      gui.text("§eK / D", x0 + 230.0F, colY + 2.0F, -1, true);
      gui.text("§ePing", x0 + 274.0F, colY + 2.0F, -1, true);

      float ry = colY + 13.0F;
      int myTeam = SkyGen.getPlayerTeam(g);
      String myPrefix = isMinigame ? (SkyGen.BEDWARS_CODES[myTeam] + "[" + SkyGen.BEDWARS_NAMES[myTeam].substring(0, 3).toUpperCase(Locale.ROOT) + "] §f") : "§f";
      gui.text(myPrefix + selfName + " §7(You)", x0 + 8.0F, ry, -1, true);
      gui.text(shortModeBadge(g.player.rawMode()), x0 + 118.0F, ry, -1, true);
      gui.text("§c" + g.player.health + "HP §7/ §9" + g.armorPoints() + "AP", x0 + 162.0F, ry, -1, true);
      gui.text("§a" + SkyGen.myKills + " §7/ §c" + SkyGen.myDeaths, x0 + 230.0F, ry, -1, true);
      gui.text((myPingMs < 60 ? "§a" : (myPingMs < 150 ? "§e" : "§c")) + myPingMs + "ms", x0 + 274.0F, ry, -1, true);
      ry += 12.0F;

      int drawn = 1;
      for (Peer p : peers.values()) {
         if (drawn >= totalRows) {
            break;
         }
         int pt = Math.abs(p.team) & 3;
         String pfx = isMinigame ? (SkyGen.BEDWARS_CODES[pt] + "[" + SkyGen.BEDWARS_NAMES[pt].substring(0, 3).toUpperCase(Locale.ROOT) + "] §f") : "§f";
         gui.text(pfx + p.name, x0 + 8.0F, ry, -1, true);
         gui.text(shortModeBadge(p.gameMode), x0 + 118.0F, ry, -1, true);
         gui.text("§c" + p.health + "HP §7/ §9" + p.armorPts + "AP", x0 + 162.0F, ry, -1, true);
         gui.text("§a" + p.kills + " §7/ §c" + p.deaths, x0 + 230.0F, ry, -1, true);
         gui.text((p.pingMs < 60 ? "§a" : (p.pingMs < 150 ? "§e" : "§c")) + p.pingMs + "ms", x0 + 274.0F, ry, -1, true);
         ry += 12.0F;
         drawn++;
      }

      if (isMinigame && g.mobs != null) {
         for (Mobs.Mob m : g.mobs.list) {
            if (drawn >= totalRows) {
               break;
            }
            if (m != null && !m.dead && m.teamTag != null && m.teamTag.startsWith("Team:")) {
               String tName = m.teamTag.substring(5);
               gui.text("§7[BOT] §f" + tName + " Defender", x0 + 8.0F, ry, -1, true);
               gui.text("§eAI", x0 + 118.0F, ry, -1, true);
               gui.text("§c" + m.health + "HP", x0 + 162.0F, ry, -1, true);
               gui.text("§7-", x0 + 230.0F, ry, -1, true);
               gui.text("§aAI", x0 + 274.0F, ry, -1, true);
               ry += 12.0F;
               drawn++;
            }
         }
      }

      gui.text("§7Tip: Press §eG§7 to place a 3D Tactical Ping Marker  |  §e/nick <name>§7  |  §e/tp <player>", x0 + 8.0F, y0 + boxH - 11.0F, -1, true);
   }

   private static String sanitizeName(String s) {
      String clean = s.replaceAll("[^A-Za-z0-9_\\-]", "");
      return clean.isEmpty() ? "Peer" : (clean.length() > 16 ? clean.substring(0, 16) : clean);
   }

   static long packCoord(int x, int y, int z) {
      return pack(x, y, z);
   }

   static ArrayList<String> buildMobSnapshotsForTest(World w, Mobs mobs) {
      return buildMobSnapshots(w, mobs);
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
