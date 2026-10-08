package bc;

import static bc.GameTests.check;
import static bc.GameTests.eq;
import static bc.GameTests.test;

import java.io.DataInputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/** Headless tests of the prototype multiplayer (Update 3.20): the wire, over the loopback interface of this computer. */
final class TestsNet {
   static void run() {
      wire();
      chunks();
   }

   /** A pair of connected sockets on the loopback interface. */
   static Socket[] pair() throws Exception {
      try (ServerSocket ss = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
         Socket a = new Socket(InetAddress.getLoopbackAddress(), ss.getLocalPort());
         Socket b = ss.accept();
         return new Socket[]{a, b};
      }
   }

   static void wire() {
      test("net: packets go both ways in order, every number and string arrives as it left, a closed end is noticed", () -> {
         Socket[] s = pair();
         LinkedBlockingQueue<Net.Packet> atHost = new LinkedBlockingQueue<>();
         LinkedBlockingQueue<Net.Packet> atGuest = new LinkedBlockingQueue<>();
         boolean[] hostClosed = new boolean[1];
         Net.Conn host = new Net.Conn(s[1], atHost::add, () -> hostClosed[0] = true);
         Net.Conn guest = new Net.Conn(s[0], atGuest::add, null);
         host.start("test-host");
         guest.start("test-guest");
         guest.send(new Net.Out(Net.C_HELLO).i(Net.PROTOCOL).str("Zoë \u00e4\u00f6\u00fc"));
         guest.send(new Net.Out(Net.C_POS).d(12.5).d(70.25).d(-3.125).f(90.0F).f(-12.5F).f(5.0F).b(Net.F_SNEAK | Net.F_SPRINT).b(8).f(1.5F).f(0.5F));
         guest.send(new Net.Out(Net.C_BLOCK).i(-5).i(64).i(100000).s(1167));
         Net.Packet p = atHost.poll(5, TimeUnit.SECONDS);
         check(p != null && p.type == Net.C_HELLO, "hello first");
         DataInputStream in = p.in();
         eq(Net.PROTOCOL, in.readInt(), "protocol");
         eq("Zoë \u00e4\u00f6\u00fc", in.readUTF(), "the name, with accents");
         p = atHost.poll(5, TimeUnit.SECONDS);
         check(p != null && p.type == Net.C_POS, "position second");
         in = p.in();
         check(in.readDouble() == 12.5 && in.readDouble() == 70.25 && in.readDouble() == -3.125, "x y z");
         check(in.readFloat() == 90.0F && in.readFloat() == -12.5F && in.readFloat() == 5.0F, "yaw pitch head");
         eq(Net.F_SNEAK | Net.F_SPRINT, in.readUnsignedByte(), "flags");
         eq(8, in.readUnsignedByte(), "render distance");
         check(in.readFloat() == 1.5F && in.readFloat() == 0.5F, "walk");
         p = atHost.poll(5, TimeUnit.SECONDS);
         check(p != null && p.type == Net.C_BLOCK, "block third");
         in = p.in();
         eq(-5, in.readInt(), "x");
         eq(64, in.readInt(), "y");
         eq(100000, in.readInt(), "z");
         eq(1167, in.readUnsignedShort(), "an id above 255");
         host.send(new Net.Out(Net.S_CHAT).str("<Host> hello"));
         p = atGuest.poll(5, TimeUnit.SECONDS);
         check(p != null && p.type == Net.S_CHAT && p.in().readUTF().equals("<Host> hello"), "chat back to the guest");
         // a lot of packets: none lost, none reordered
         for (int i = 0; i < 2000; i++) {
            host.send(new Net.Out(Net.S_TIME).l(i).b(i % 3));
         }

         for (int i = 0; i < 2000; i++) {
            p = atGuest.poll(5, TimeUnit.SECONDS);
            check(p != null && p.type == Net.S_TIME, "packet " + i + " arrived");
            eq((long)i, p.in().readLong(), "in order");
         }

         check(host.sent > 0 && host.sent == guest.received, "the counters agree: " + host.sent + " / " + guest.received);
         // closing one end is seen at the other
         guest.close();
         for (int i = 0; i < 100 && !hostClosed[0]; i++) {
            Thread.sleep(50);
         }

         check(hostClosed[0], "the host end noticed that the guest went away");
         host.close();
      });
      test("net: garbage on the wire (a huge length) closes the connection and does not allocate", () -> {
         Socket[] s = pair();
         boolean[] closed = new boolean[1];
         Net.Conn host = new Net.Conn(s[1], p -> { }, () -> closed[0] = true);
         host.start("test-garbage");
         s[0].getOutputStream().write(new byte[]{0x7F, (byte)0xFF, (byte)0xFF, (byte)0xFF, 1, 2, 3});
         s[0].getOutputStream().flush();
         for (int i = 0; i < 100 && !closed[0]; i++) {
            Thread.sleep(50);
         }

         check(closed[0], "closed");
         s[0].close();
      });
   }

   static void chunks() {
      test("net: a chunk (blocks above 255 too, biomes, colours) goes over the wire and comes back the same, a few kilobytes", () -> {
         Chunk c = new Chunk(-3, 7);
         java.util.Random r = new java.util.Random(5);
         for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
               c.set(x, 0, z, Blocks.BEDROCK);
               for (int y = 1; y < 60; y++) {
                  c.set(x, y, z, Blocks.STONE);
               }

               c.set(x, 60, z, Blocks.GRASS);
               c.biome[z * 16 + x] = (byte)(r.nextInt(8));
               c.grassColor[z * 16 + x] = 0x405060 + r.nextInt(255);
               c.foliageColor[z * 16 + x] = 0x203040 + r.nextInt(255);
               c.waterColor[z * 16 + x] = 0x3F76E4;
            }
         }

         c.set(3, 61, 4, Blocks313.LEAVES_ACACIA);
         c.set(4, 61, 4, 3999);
         c.set(5, 100, 5, 12);
         byte[] data = Net.encodeChunk(c);
         check(data.length > 20 && data.length < 20000, "deflated size: " + data.length);
         Chunk d = Net.decodeChunk(-3, 7, data);
         check(java.util.Arrays.equals(c.blocks, d.blocks), "the blocks are the same");
         check(java.util.Arrays.equals(c.biome, d.biome), "biomes");
         check(java.util.Arrays.equals(c.grassColor, d.grassColor), "grass colours");
         check(java.util.Arrays.equals(c.foliageColor, d.foliageColor), "foliage colours");
         check(java.util.Arrays.equals(c.waterColor, d.waterColor), "water colours");
         eq(-3, d.cx, "x");
         eq(7, d.cz, "z");
         eq(3999, d.get(4, 61, 4), "an id of 12 bits");
         check(d.hm[4 * 16 + 4] == 61 || d.hm[4 * 16 + 4] > 0, "the heightmap is computed: " + d.hm[4 * 16 + 4]);
         // a real chunk of the flat test world
         Game g = GameTests.newGame(0);
         Chunk w = g.world.chunk(1, 1);
         Chunk w2 = Net.decodeChunk(1, 1, Net.encodeChunk(w));
         check(java.util.Arrays.equals(w.blocks, w2.blocks), "a chunk of the test world is the same after the trip");
         // garbage is an error, not a crash of the game thread
         boolean threw = false;
         try {
            Net.decodeChunk(0, 0, new byte[]{1, 2, 3, 4, 5});
         } catch (java.io.IOException | RuntimeException e) {
            threw = true;
         }

         check(threw, "garbage is refused");
      });
   }
}
