package bc;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * Update 3.20: the wire of the prototype multiplayer. A host (a game that opened its world to the network) and its guests talk over one TCP connection each; the
 * host's world is the truth. Every packet is {@code int length, byte type, payload}. A {@link Conn} writes from a thread of its own (a slow network never stalls
 * the game) and reads from another (packets arrive on that thread: the game takes them from a queue).
 *
 * Packets (all numbers big endian, strings as UTF):
 * <pre>
 * guest -> host   HELLO  int protocol, String name
 *                 POS    double x, y, z, float yaw, pitch, headYaw, byte flags, byte renderDistance, float walkPhase, walkAmp
 *                 BLOCK  int x, y, z, short id          the guest changed a block
 *                 CHAT   String text
 * host -> guest   WELCOME int protocol, long seed, byte dim, String worldSettings, double spawnX, spawnY, spawnZ, int yourId, long dayTime, byte gameMode, String hostName
 *                 CHUNK  int cx, cz, int length, byte[] data (see {@link #encodeChunk})
 *                 BLOCK  int x, y, z, short id          a block changed in the host's world
 *                 PLAYERS byte n, n * (int id, String name, double x, y, z, float yaw, pitch, headYaw, byte flags, float walkPhase, walkAmp)
 *                 LEAVE  int id
 *                 CHAT   String text
 *                 TIME   long dayTime, byte weatherState
 *                 KICK   String reason
 * </pre>
 */
final class Net {
   static final int PROTOCOL = 1;
   static final int PORT = 25566;
   /** The biggest packet we accept (a chunk is a few kilobytes deflated; this is only a guard against garbage). */
   static final int MAX_PACKET = 1 << 20;

   static final int C_HELLO = 1;
   static final int C_POS = 2;
   static final int C_BLOCK = 3;
   static final int C_CHAT = 4;

   static final int S_WELCOME = 20;
   static final int S_CHUNK = 21;
   static final int S_BLOCK = 22;
   static final int S_PLAYERS = 23;
   static final int S_LEAVE = 24;
   static final int S_CHAT = 25;
   static final int S_TIME = 26;
   static final int S_KICK = 27;

   /** Flags in a POS packet / a player of the PLAYERS packet. */
   static final int F_SNEAK = 1;
   static final int F_SPRINT = 2;
   static final int F_FLY = 4;
   static final int F_SWIM = 8;
   static final int F_AIR = 16;

   private Net() {
   }

   /** A packet that has been read. */
   static final class Packet {
      final int type;
      final byte[] data;

      Packet(int type, byte[] data) {
         this.type = type;
         this.data = data;
      }

      DataInputStream in() {
         return new DataInputStream(new ByteArrayInputStream(this.data));
      }
   }

   /** A packet being written. */
   static final class Out {
      final int type;
      final ByteArrayOutputStream bytes = new ByteArrayOutputStream(64);
      final DataOutputStream out = new DataOutputStream(this.bytes);

      Out(int type) {
         this.type = type;
      }

      Out i(int v) {
         try {
            this.out.writeInt(v);
         } catch (IOException e) {
            throw new IllegalStateException(e);
         }

         return this;
      }

      Out l(long v) {
         try {
            this.out.writeLong(v);
         } catch (IOException e) {
            throw new IllegalStateException(e);
         }

         return this;
      }

      Out s(int v) {
         try {
            this.out.writeShort(v);
         } catch (IOException e) {
            throw new IllegalStateException(e);
         }

         return this;
      }

      Out b(int v) {
         try {
            this.out.writeByte(v);
         } catch (IOException e) {
            throw new IllegalStateException(e);
         }

         return this;
      }

      Out f(float v) {
         try {
            this.out.writeFloat(v);
         } catch (IOException e) {
            throw new IllegalStateException(e);
         }

         return this;
      }

      Out d(double v) {
         try {
            this.out.writeDouble(v);
         } catch (IOException e) {
            throw new IllegalStateException(e);
         }

         return this;
      }

      Out str(String v) {
         try {
            this.out.writeUTF(v == null ? "" : (v.length() > 4000 ? v.substring(0, 4000) : v));
         } catch (IOException e) {
            throw new IllegalStateException(e);
         }

         return this;
      }

      Out raw(byte[] v) {
         try {
            this.out.write(v);
         } catch (IOException e) {
            throw new IllegalStateException(e);
         }

         return this;
      }

      /** The bytes that go on the wire: length, type, payload. */
      byte[] frame() {
         byte[] body = this.bytes.toByteArray();
         ByteArrayOutputStream f = new ByteArrayOutputStream(body.length + 5);
         DataOutputStream d = new DataOutputStream(f);
         try {
            d.writeInt(body.length + 1);
            d.writeByte(this.type);
            d.write(body);
         } catch (IOException e) {
            throw new IllegalStateException(e);
         }

         return f.toByteArray();
      }
   }

   /** One end of a connection. */
   static final class Conn {
      final Socket socket;
      private final DataInputStream in;
      private final DataOutputStream out;
      private final LinkedBlockingQueue<byte[]> outQ = new LinkedBlockingQueue<>();
      private final Consumer<Packet> onPacket;
      private final Runnable onClose;
      volatile boolean closed;
      /** Bytes sent and received, for the debug screen. */
      volatile long sent;
      volatile long received;

      Conn(Socket socket, Consumer<Packet> onPacket, Runnable onClose) throws IOException {
         this.socket = socket;
         this.onPacket = onPacket;
         this.onClose = onClose;
         socket.setTcpNoDelay(true);
         this.in = new DataInputStream(new java.io.BufferedInputStream(socket.getInputStream(), 1 << 15));
         this.out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream(), 1 << 15));
      }

      /** Starts the two threads. */
      void start(String name) {
         Thread r = new Thread(this::readLoop, name + "-read");
         r.setDaemon(true);
         r.start();
         Thread w = new Thread(this::writeLoop, name + "-write");
         w.setDaemon(true);
         w.start();
      }

      /** Queues a packet; returns at once. */
      void send(Out p) {
         if (!this.closed) {
            this.outQ.add(p.frame());
         }
      }

      void sendRaw(byte[] frame) {
         if (!this.closed) {
            this.outQ.add(frame);
         }
      }

      /** How many packets wait to be written (a guest that cannot keep up gets no more chunks). */
      int backlog() {
         return this.outQ.size();
      }

      private void readLoop() {
         try {
            while (!this.closed) {
               int len = this.in.readInt();
               if (len < 1 || len > MAX_PACKET) {
                  throw new IOException("bad packet length " + len);
               }

               int type = this.in.readUnsignedByte();
               byte[] data = new byte[len - 1];
               this.in.readFully(data);
               this.received += len + 4;
               this.onPacket.accept(new Packet(type, data));
            }
         } catch (IOException | RuntimeException e) {
            // the other side went away (or sent garbage)
         } finally {
            this.close();
         }
      }

      private void writeLoop() {
         try {
            while (!this.closed) {
               byte[] f = this.outQ.take();
               if (f.length == 0) {
                  break; // the closing marker
               }

               this.out.write(f);
               this.sent += f.length;
               if (this.outQ.isEmpty()) {
                  this.out.flush();
               }
            }

            this.out.flush();
         } catch (IOException | InterruptedException e) {
            // closed
         } finally {
            this.close();
         }
      }

      void close() {
         if (this.closed) {
            return;
         }

         this.closed = true;
         this.outQ.add(new byte[0]);
         try {
            this.socket.close();
         } catch (IOException e) {
            // already gone
         }

         if (this.onClose != null) {
            this.onClose.run();
         }
      }
   }

   // ------------------------------------------------------------------ chunks
   /**
    * A chunk as it goes over the wire (deflated): the biome of every column, the three colour maps (grass, foliage, water), then the blocks in the format of a
    * chunk file ({@link Chunk#writeBlocks} - last, because its reader looks for an optional trailer). The light is not sent: the guest works it out itself.
    */
   static byte[] encodeChunk(Chunk c) {
      try {
         ByteArrayOutputStream bo = new ByteArrayOutputStream(1 << 14);
         Deflater def = new Deflater(1);
         try (DataOutputStream d = new DataOutputStream(new DeflaterOutputStream(bo, def, 1 << 14))) {
            d.write(c.biome);
            for (int v : c.grassColor) {
               d.writeInt(v);
            }

            for (int v : c.foliageColor) {
               d.writeInt(v);
            }

            for (int v : c.waterColor) {
               d.writeInt(v);
            }

            Chunk.writeBlocks(d, c.blocks);
         } finally {
            def.end();
         }

         return bo.toByteArray();
      } catch (IOException e) {
         throw new IllegalStateException(e);
      }
   }

   /** The counterpart of {@link #encodeChunk}: a chunk with its blocks, biomes and colours and its heightmap, not yet lit. */
   static Chunk decodeChunk(int cx, int cz, byte[] data) throws IOException {
      Chunk c = new Chunk(cx, cz);
      try (DataInputStream d = new DataInputStream(new InflaterInputStream(new ByteArrayInputStream(data)))) {
         d.readFully(c.biome);
         for (int i = 0; i < 256; i++) {
            c.grassColor[i] = d.readInt();
         }

         for (int i = 0; i < 256; i++) {
            c.foliageColor[i] = d.readInt();
         }

         for (int i = 0; i < 256; i++) {
            c.waterColor[i] = d.readInt();
         }

         Chunk.readBlocks(d, c.blocks);
      }

      c.computeHeightmap();
      return c;
   }
}
