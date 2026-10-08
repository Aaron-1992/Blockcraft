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

   private Net319() {
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
         hosting = true;
         selfId = 1;
         selfName = "Host";
      } catch (IOException e) {
         return "§cCould not host on port " + port + ": " + e.getMessage();
      }

      startDiscoveryResponder(activePort);
      Thread acceptThread = new Thread(() -> acceptLoop(w), "bc-net-accept");
      acceptThread.setDaemon(true);
      acceptThread.start();
      return "Hosting P2P/LAN world on port " + activePort + " (others can /join or click Join LAN)";
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
            long seed = w != null ? w.seed : 1L;
            int dim = w != null ? w.dim : 0;
            long dayTime = w != null ? w.dayTime : 1000L;
            c.send("ASSIGN " + cid + " " + seed + " " + dim + " " + dayTime);
            for (Map.Entry<Long, Integer> e : edits.entrySet()) {
               long k = e.getKey();
               c.send("BLOCK " + unpackX(k) + " " + unpackY(k) + " " + unpackZ(k) + " " + e.getValue());
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
            } else if (line.startsWith("POSE ") || line.startsWith("BLOCK ") || line.startsWith("CHAT ")) {
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
         String t = target.trim();
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
         selfName = "Peer" + (int)(System.currentTimeMillis() % 900 + 100);
         c.send("HELLO 0 " + selfName);
         Thread rt = new Thread(() -> readLoopClient(c), "bc-net-reader");
         rt.setDaemon(true);
         rt.start();
         return "Connected to " + host + ":" + port + " as " + selfName;
      } catch (IOException e) {
         clientConnected = false;
         return "§cCould not connect to " + host + ":" + port + " (" + e.getMessage() + ")";
      }
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
                     long dayTime = Long.parseLong(p[4]);
                     if (w != null) {
                        w.dayTime = dayTime;
                     }

                     if (clientConn != null) {
                        clientConn.send("HELLO " + selfId + " " + selfName);
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
         GL11.glPopMatrix();
      }

      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
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
