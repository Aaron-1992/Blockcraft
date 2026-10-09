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
      int team = -1;
      float swing = -1.0F;
      float pswing = -1.0F;
      int heldId;
      int skinIdx;
      float age;

      Peer(int id, String name) {
         this.id = id;
         this.name = name;
         this.team = Math.max(0, (id - 1) & 3);
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
   private static final ConcurrentHashMap<Long, Integer> edits = new ConcurrentHashMap<>();
   private static final ConcurrentLinkedQueue<String> inbox = new ConcurrentLinkedQueue<>();
   private static int nextConnId = 2;
   public static int selfId = 1;
   public static String selfName = "Player" + (int)(System.currentTimeMillis() % 900 + 100);
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

   public static int aliveEnemyPeers(int myTeam) {
      int c = 0;
      for (Peer p : peers.values()) {
         if (!p.spectator && (myTeam < 0 || p.team != myTeam)) {
            c++;
         }
      }
      return c;
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
         selfName = "Host";
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

            c.send(
               String.format(
                  Locale.ROOT,
                  "ASSIGN %d %d %d %d %.3f %.3f %.3f %d %d %d %d",
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
                  SkyGen.matchState
               )
            );
            c.send("TEAM 1 " + SkyGen.myTeam);
            if (curW != null && wType == WorldSettings.BEDWARS) {
               for (int t = 0; t < 4; t++) {
                  if (!SkyGen.isTeamBedIntact(curW, t)) {
                     c.send("BED " + t + " Server");
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

            if (line.equals("PING")) {
               c.send("PONG");
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
               || line.startsWith("DROP ")
               || line.startsWith("PICKUP ")
               || line.startsWith("BED ")
               || line.startsWith("TEAM ")
               || line.startsWith("MATCH ")) {
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
            selfName = "Peer" + (int)(System.currentTimeMillis() % 900 + 100);
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

   public static void applyEditsToChunk(Chunk ch) {
      if (ch == null || edits.isEmpty() || !isActive()) {
         return;
      }
      boolean changed = false;
      for (Map.Entry<Long, Integer> e : edits.entrySet()) {
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
      if (!isActive()) {
         return;
      }
      String pkt = "MATCH " + action;
      if (hosting) {
         broadcastExcept(0, pkt);
      } else if (clientConnected && clientConn != null) {
         clientConn.send(pkt);
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
      int flags = (pl.sneaking ? 1 : 0)
         | (pl.sprinting ? 2 : 0)
         | (pl.inWater ? 4 : 0)
         | (pl.onGround ? 8 : 0)
         | (pl.hurtTime > 0 ? 16 : 0)
         | ((pl.dead || pl.spectator) ? 32 : 0);
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
         if (g.tickCount % 40 == 0) {
            clientConn.send("PING");
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
                     int assignedTeam = p.length >= 11 ? Integer.parseInt(p[10]) : Math.max(0, (selfId - 1) & 3);
                     int hostMatchState = p.length >= 12 ? Integer.parseInt(p[11]) : SkyGen.STATE_PLAYING;
                     SkyGen.myTeam = Math.abs(assignedTeam) & 3;
                     SkyGen.matchState = hostMatchState;
                     if (g != null) {
                        g.joinMultiplayerWorld(seed, dim, dayTime, sx, sy, sz, wType, gMode);
                     } else if (w != null) {
                        w.dayTime = dayTime;
                     }

                     if (clientConn != null) {
                        clientConn.send("HELLO " + selfId + " " + selfName);
                        clientConn.send("TEAM " + selfId + " " + SkyGen.myTeam);
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
               case "MATCH" -> {
                  if (p.length >= 2 && g != null) {
                     String act = p[1].toUpperCase(Locale.ROOT);
                     if ("START".equals(act)) {
                        SkyGen.startMatch(g, false);
                     } else if ("WAIT".equals(act) || "LOBBY".equals(act)) {
                        SkyGen.matchState = SkyGen.STATE_WAITING;
                        g.setScreen(Game.S.WAITING);
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
                     peer.spectator = (flags & 32) != 0;
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
