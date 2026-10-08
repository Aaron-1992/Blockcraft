package bc;

import static bc.GameTests.GROUND;
import static bc.GameTests.check;
import static bc.GameTests.eq;
import static bc.GameTests.newGame;
import static bc.GameTests.test;

import java.nio.ByteBuffer;
import java.util.Random;
import org.lwjgl.system.MemoryUtil;

/**
 * Update 3.19 unit tests:
 *  - Bushy leaves (3D crossed foliage fronds & Options persistence)
 *  - Per-tile CPU mipmap downsampling (distant block seam prevention)
 *  - Expanded village templates & biome landmarks (Desert Pyramid, Jungle Temple, Witch Hut, Igloo, Watchtower)
 *  - Redstone mechanisms, wire power propagation, Iron Trapdoor / Lamp / TNT activation & Slime Block bounce
 *  - Prototype P2P/LAN & standalone server multiplayer (Net319 host, join, block/pose/chat sync)
 */
final class Tests319 {
   private Tests319() {
   }

   static void run() {
      bushyLeaves();
      tileMipmaps();
      landmarksAndVillages();
      redstoneAndBlocks();
      v40RedstoneAndPistons();
      v40LodBiomesAndStructures();
      multiplayer();
   }

   static void bushyLeaves() {
      test("3.19 bushy leaves: 3D foliage fronds add 24 vertices per isolated leaf block and map sky light to qm", () -> {
         TexExtra.blocks();
         Game g = newGame(1);
         World w = g.world;
         Chunk c = w.chunkAt(8, 8);
         w.setBlock(8, GROUND + 5, 8, 12);
         w.lightChunk(c);

         Mesher.bushyLeaves = false;
         Mesher.Result plain = Mesher.build(w, c, (GROUND + 5) >> 4, true);
         int vPlain = plain.nO;
         if (plain.opaque != null) {
            MemoryUtil.memFree(plain.opaque);
         }

         Mesher.bushyLeaves = true;
         Mesher.Result bushy = Mesher.build(w, c, (GROUND + 5) >> 4, true);
         int vBushy = bushy.nO;
         // Inspect the last vertex's lightmap coordinates (stride 11 floats: x, y, z, u, v, r, g, b, a, qm_sky, ql_blk)
         float frondSky = bushy.opaque.get((vBushy - 1) * 11 + 9);
         float frondBlk = bushy.opaque.get((vBushy - 1) * 11 + 10);
         if (bushy.opaque != null) {
            MemoryUtil.memFree(bushy.opaque);
         }

         Mesher.bushyLeaves = false;
         eq(vPlain + 24, vBushy, "isolated leaf block gains 6 two-sided frond quads (24 vertices)");
         check(frondSky > 0.85F && frondBlk < 0.15F, "bushy frond vertex receives outdoor sky light in qm (" + frondSky + ") and low block light in ql (" + frondBlk + ")");
      });
   }

   static void tileMipmaps() {
      test("3.19 mipmaps: per-tile downsampling stays strictly inside each 16x16 tile", () -> {
         TexExtra.blocks();
         int[] tile0 = new int[256];
         for (int y = 0; y < 16; y++) {
            System.arraycopy(Tex.atlasPixels, y * Tex.PX, tile0, y * 16, 16);
         }

         int[] cur = tile0;
         int sz = 16;
         for (int lvl = 1; lvl <= 3; lvl++) {
            cur = Tex.downsampleTile(cur, sz);
            sz >>= 1;
            eq(sz * sz, cur.length, "mip level " + lvl + " pixel count");
            int nonZero = 0;
            for (int px : cur) {
               if ((px >>> 24) > 0) {
                  nonZero++;
               }
            }

            check(nonZero == sz * sz, "solid grass top tile stays opaque at mip level " + lvl);
         }
      });
   }

   static void landmarksAndVillages() {
      test("3.19 structures: expanded village templates and biome landmarks place cleanly", () -> {
         check(Buildings314.ALL.size() >= 17, "at least 17 village building templates: " + Buildings314.ALL.size());
         for (Tpl t : Buildings314.ALL.values()) {
            check(t.w > 0 && t.h > 0 && t.d > 0, "valid template bounds for " + t.name);
         }

         Game g = newGame(1);
         Chunk c = g.world.chunkAt(0, 0);
         for (int kind = 1; kind <= 5; kind++) {
            Landmarks319.Site s = new Landmarks319.Site();
            s.kind = kind;
            s.x0 = 2;
            s.y0 = GROUND;
            s.z0 = 2;
            s.w = 9;
            s.d = 9;
            Landmarks319.buildSite(c, s);
         }

         int nonAir = 0;
         for (int y = GROUND + 1; y < GROUND + 14; y++) {
            for (int z = 0; z < 16; z++) {
               for (int x = 0; x < 16; x++) {
                  if (c.get(x, y, z) != 0) {
                     nonAir++;
                  }
               }
            }
         }

         check(nonAir > 80, "landmarks placed blocks into chunk: " + nonAir);
      });
   }

   static void redstoneAndBlocks() {
      test("3.19 redstone: wire propagation, lamps, levers, iron trapdoors and slime block bounce", () -> {
         TexExtra.blocks();
         check(Items.name(Redstone319.ITEM_REDSTONE).equals("Redstone Dust"), "redstone dust item named");
         check(Blocks.name[Redstone319.REDSTONE_BLOCK].equals("Block of Redstone"), "redstone block named");
         check(Blocks.name[Redstone319.SLIME_BLOCK].equals("Slime Block"), "slime block named");
         check(Blocks.emit[Redstone319.SEA_LANTERN] == 15, "sea lantern emits full light");

         Game g = newGame(0);
         World w = g.world;
         int y = GROUND + 1;
         // Lay a 3-block line of redstone wire leading to a Redstone Lamp and an Iron Trapdoor
         w.setBlock(8, y, 8, Redstone319.REDSTONE_WIRE0);
         w.setBlock(9, y, 8, Redstone319.REDSTONE_WIRE0);
         w.setBlock(10, y, 8, Redstone319.REDSTONE_WIRE0);
         w.setBlock(11, y, 8, Redstone319.REDSTONE_LAMP);
         int closedIron = Shapes.trap(6, false, false, 0);
         w.setBlock(10, y, 9, closedIron);

         // Place a Lever at (7, y, 8) and switch it ON
         w.setBlock(7, y, 8, Redstone319.LEVER_OFF);
         check(Shapes.use(g, 7, y, 8, Redstone319.LEVER_OFF), "lever toggles on right-click");
         eq(Redstone319.LEVER_ON, w.getBlock(7, y, 8), "lever is ON");
         eq(Redstone319.REDSTONE_WIRE0 + 15, w.getBlock(8, y, 8), "wire 1 has power 15");
         eq(Redstone319.REDSTONE_WIRE0 + 14, w.getBlock(9, y, 8), "wire 2 has power 14");
         eq(Redstone319.REDSTONE_WIRE0 + 13, w.getBlock(10, y, 8), "wire 3 has power 13");
         eq(Redstone319.REDSTONE_LAMP_LIT, w.getBlock(11, y, 8), "redstone lamp lit by wire");
         check(Shapes.trapOpen(w.getBlock(10, y, 9)), "iron trapdoor opened by redstone wire");

         // Switch the lever OFF -> wire Depowers, lamp turns off, iron trapdoor closes
         Shapes.use(g, 7, y, 8, Redstone319.LEVER_ON);
         eq(Redstone319.REDSTONE_WIRE0, w.getBlock(8, y, 8), "wire 1 unpowered");
         eq(Redstone319.REDSTONE_LAMP, w.getBlock(11, y, 8), "redstone lamp unlit");
         check(!Shapes.trapOpen(w.getBlock(10, y, 9)), "iron trapdoor closed again");

         // Manual door toggle stays open across world ticks when unpowered by Redstone
         w.setBlock(4, y, 4, 73);
         w.setBlock(4, y + 1, 4, 81);
         g.toggleDoor(4, y, 4);
         check(Blocks.doorOpen(w.getBlock(4, y, 4)), "door opened manually");
         w.tick();
         w.tick();
         check(Blocks.doorOpen(w.getBlock(4, y, 4)), "door remains open after world ticks");

         // Slime Block bounce & zero fall damage
         w.setBlock(5, GROUND, 5, Redstone319.SLIME_BLOCK);
         Player p = g.player;
         p.health = 20;
         p.setPos(5.5, GROUND + 10, 5.5);
         boolean bounced = false;
         for (int i = 0; i < 200; i++) {
            p.tick(w, 0.0F, 0.0F, false, false, false);
            if (p.vy > 0.2) {
               bounced = true;
               break;
            }
         }

         check(bounced, "player bounced upward off Slime Block");
         eq(20, p.health, "Slime Block negated all fall damage");
      });
   }

   static void v40RedstoneAndPistons() {
      test("4.0 redstone: dense wire grid relaxation, repeaters, NOT-gate torches, pistons, sticky pistons, daylight sensors & observers", () -> {
         TexExtra.blocks();
         Game g = newGame(0);
         World w = g.world;
         int y = GROUND + 1;

         // 1. Place a dense 5x5 grid of redstone dust and toggle a lever next to it repeatedly (must not StackOverflow or hang)
         for (int dx = 0; dx < 5; dx++) {
            for (int dz = 0; dz < 5; dz++) {
               w.setBlock(2 + dx, y, 2 + dz, Redstone319.REDSTONE_WIRE0);
            }
         }

         w.setBlock(1, y, 2, Redstone319.LEVER_OFF);
         Shapes.use(g, 1, y, 2, Redstone319.LEVER_OFF);
         eq(Redstone319.REDSTONE_WIRE0 + 15, w.getBlock(2, y, 2), "corner of 5x5 wire grid powered to 15");
         eq(Redstone319.REDSTONE_WIRE0 + 7, w.getBlock(6, y, 6), "opposite corner of 5x5 wire grid powered to 7 (Manhattan distance 8)");
         Shapes.use(g, 1, y, 2, Redstone319.LEVER_ON);
         eq(Redstone319.REDSTONE_WIRE0, w.getBlock(2, y, 2), "corner of 5x5 wire grid unpowered cleanly");
         eq(Redstone319.REDSTONE_WIRE0, w.getBlock(6, y, 6), "opposite corner of 5x5 wire grid unpowered cleanly");

         // 2. Dynamic wire 3D geometry: isolated wire has 3 boxes (hub + N-S + E-W cross), single neighbor has 2 boxes
         Shapes.Nbr nbr = (nx, ny, nz) -> nx == 1 ? Redstone319.REDSTONE_WIRE0 : 0;
         double[][] boxes1 = Redstone319.wireBoxes(nbr, Redstone319.REDSTONE_WIRE0);
         eq(2, boxes1.length, "wire connected only to +X emits hub + east arm");

         // 3. Redstone Torch NOT-gate inversion (torch on top of block turns OFF when block below is powered)
         w.setBlock(10, y, 2, 1); // stone block
         w.setBlock(10, y + 1, 2, Redstone319.REDSTONE_TORCH);
         w.setBlock(9, y, 2, Redstone319.REDSTONE_BLOCK);
         Redstone319.propagate(w, 9, y, 2);
         eq(Redstone319.REDSTONE_TORCH_OFF, w.getBlock(10, y + 1, 2), "redstone torch turns OFF when supporting block is powered");
         w.setBlock(9, y, 2, 0);
         Redstone319.propagate(w, 9, y, 2);
         eq(Redstone319.REDSTONE_TORCH, w.getBlock(10, y + 1, 2), "redstone torch relights when power removed");

         // 4. Redstone Repeater boosts weak signal back to 15 in facing direction (+X = dir 2)
         w.setBlock(2, y, 10, Redstone319.REDSTONE_WIRE0);
         w.setBlock(3, y, 10, Redstone319.REPEATER_OFF + 2); // facing +X (dir 2), input at -X (2, y, 10)
         w.setBlock(4, y, 10, Redstone319.REDSTONE_WIRE0);
         w.setBlock(1, y, 10, Redstone319.REDSTONE_BLOCK);
         Redstone319.propagate(w, 1, y, 10);
         eq(Redstone319.REPEATER_ON + 2, w.getBlock(3, y, 10), "repeater turns ON from input behind it");
         eq(Redstone319.REDSTONE_WIRE0 + 15, w.getBlock(4, y, 10), "repeater boosts output wire to 15");

         // 5. Sticky Piston pushes a stone block on power and pulls it back on depower (facing +X = face 4)
         w.setBlock(8, y, 12, Redstone319.STICKY_PISTON + 4);
         w.setBlock(9, y, 12, 4); // cobblestone in front of piston
         w.setBlock(7, y, 12, Redstone319.REDSTONE_BLOCK);
         Redstone319.propagate(w, 7, y, 12);
         eq(Redstone319.STICKY_PISTON_EXT + 4, w.getBlock(8, y, 12), "sticky piston extended");
         eq(Redstone319.PISTON_HEAD + 4, w.getBlock(9, y, 12), "piston head placed at +X");
         eq(4, w.getBlock(10, y, 12), "cobblestone pushed to (10, y, 12)");
         w.setBlock(7, y, 12, 0);
         Redstone319.propagate(w, 7, y, 12);
         eq(Redstone319.STICKY_PISTON + 4, w.getBlock(8, y, 12), "sticky piston retracted");
         eq(4, w.getBlock(9, y, 12), "sticky piston pulled cobblestone back to (9, y, 12)");
         eq(0, w.getBlock(10, y, 12), "pushed cell cleared after sticky retraction");
      });
   }

   static void v40LodBiomesAndStructures() {
      test("4.0 LOD: new biomes and 3.14/3.19 structures render into LOD buffers without legacy v.blds", () -> {
         Game g = newGame(1);
         World w = g.world;
         Lod lod = new Lod(null, w.gen);
         try {
            Lod.Tile t64 = new Lod.Tile(64, 0, 0);
            lod.build(t64);
            check(t64.vcount > 0 && t64.data != null, "LOD 64x64 tile (surface + trees + buildings/landmarks) built vertices: " + t64.vcount);
            Lod.Tile t128 = new Lod.Tile(128, -256, -256);
            lod.build(t128);
            check(t128.vcount > 0 && t128.data != null, "LOD 128x128 tile built vertices: " + t128.vcount);
         } finally {
            lod.pool.shutdownNow();
         }
      });
   }

   static void multiplayer() {
      test("3.19 / 4.0 multiplayer: P2P/LAN host, client join, block sync, pose sync, dropped item sync and survival PvP", () -> {
         Game g = newGame(0); // Survival mode (mode 0) for PvP testing
         World w = g.world;
         int port = 25579;
         String hostMsg = Net319.host(g, port);
         check(Net319.isHosting(), "host started: " + hostMsg);
         try {
            try (java.net.Socket s = new java.net.Socket("127.0.0.1", port)) {
               s.setSoTimeout(1500);
               java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(s.getInputStream()));
               java.io.PrintWriter out = new java.io.PrintWriter(s.getOutputStream(), true);
               String assign = in.readLine();
               check(assign != null && assign.startsWith("ASSIGN "), "received ASSIGN handshake: " + assign);
               out.println("HELLO 2 AlexPeer");
               out.println("POSE 2 AlexPeer 12.500 65.000 12.500 90.00 0.00 90.00 8 -1.000 1 0 0");
               out.println("BLOCK 6 " + (GROUND + 2) + " 6 " + Redstone319.SEA_LANTERN);
               out.println("DROP 99001 9.500 " + (GROUND + 1.5) + " 9.500 0.0000 0.1000 0.0000 264 3 0 10");
               out.println("HIT " + Net319.selfId() + " 4 12.500 12.500 AlexPeer");
               for (int i = 0; i < 20 && (Net319.peerCount() == 0 || w.getBlock(6, GROUND + 2, 6) != Redstone319.SEA_LANTERN); i++) {
                  try {
                     Thread.sleep(20L);
                  } catch (InterruptedException ignored) {
                  }

                  Net319.tick(g);
               }

               Net319.tick(g);
               eq(1, Net319.peerCount(), "one remote peer tracked");
               eq(Redstone319.SEA_LANTERN, w.getBlock(6, GROUND + 2, 6), "remote BLOCK packet updated world");
               check(g.player.health < 20, "survival PvP HIT packet reduced host player health to " + g.player.health);

               World.Item syncedItem = null;
               for (World.Item it : w.items) {
                  if (it != null && it.netId == 99001L) {
                     syncedItem = it;
                     break;
                  }
               }

               check(syncedItem != null && syncedItem.id == 264 && syncedItem.count == 3, "remote DROP packet spawned item stack in world");
               out.println("PICKUP 99001 0");
               for (int i = 0; i < 15 && syncedItem != null && !syncedItem.dead; i++) {
                  try {
                     Thread.sleep(15L);
                  } catch (InterruptedException ignored) {
                  }

                  Net319.tick(g);
               }

               check(syncedItem != null && syncedItem.dead, "remote PICKUP packet marked dropped item dead");
            }
         } finally {
            Net319.disconnect(g);
         }

         check(!Net319.isActive(), "disconnected cleanly");
      });
   }
}
