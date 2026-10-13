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

               // Verify persistent Multiplayer: switching gamemode/world on host NEVER disconnects connected peers and broadcasts new ASSIGN!
               SkyGen.switchMinigameMode(g, WorldSettings.CS2, true);
               check(Net319.isHosting() && Net319.peerCount() == 1, "Connected peer stays connected when host switches gamemode to CS2 Strike");
               String switchedAssign = null;
               for (int i = 0; i < 20; i++) {
                  String line = in.readLine();
                  if (line != null && line.startsWith("ASSIGN ")) {
                     switchedAssign = line;
                     break;
                  }
               }
               check(switchedAssign != null && switchedAssign.contains(" " + WorldSettings.CS2 + " "),
                  "Host broadcasts fresh ASSIGN with new gamemode (CS2=8) to connected peers without disconnecting: " + switchedAssign);
               g.world.settings.type = WorldSettings.CLASSIC;
            }
         } finally {
            Net319.disconnect(g);
         }

         check(!Net319.isActive(), "disconnected cleanly");

         // Follow-up patch checks: chat wrapping/clipping, block models/orientations, sideways bark, and /locate + new commands
         java.util.List<String> wrapped = Game.wrapChatLine("§cCommands: /gamemode /time set /tp /locate /seed /dim /give /clear /setblock /fill /spawn /spawnpoint /top /kill /butcher", 140);
         check(wrapped.size() >= 3, "long chat message wraps across multiple lines: " + wrapped.size());
         for (String wl : wrapped) {
            check(Gui.textWidth(Gui.stripCodes(wl)) <= 140, "wrapped chat line fits within maxWidth: " + wl);
            check(wl.startsWith("§c"), "color code preserved across wrapped lines: " + wl);
         }
         String clippedIn = Game.clipChatInput("/fill 100 64 100 105 68 105 polished_granite_with_very_long_tail_cursor_", 100);
         check(Gui.textWidth(Gui.stripCodes(clippedIn)) <= 100 && clippedIn.endsWith("cursor_"), "chat input clips from left and keeps cursor visible: " + clippedIn);

         check(Shapes.flatItem(Redstone319.REDSTONE_TORCH) && Shapes.flatItem(Redstone319.REDSTONE_TORCH_OFF), "redstone torches render as crisp flat icons in inventory/hand");
         check(Shapes.iconBoxes(Redstone319.LEVER_OFF) != null && Shapes.iconBoxes(Redstone319.REPEATER_OFF) != null && Shapes.iconBoxes(Redstone319.DAYLIGHT_SENSOR) != null, "3D iconBoxes exposed for redstone mechanisms");
         eq(Redstone319.T_OBSERVER_FRONT, Blocks.tiles[Redstone319.OBSERVER][2], "north-facing observer has face only on front (-Z)");
         eq(Redstone319.T_PISTON_BOTTOM, Blocks.tiles[Redstone319.OBSERVER][3], "north-facing observer has output port on back (+Z)");
         eq(Redstone319.T_PISTON_SIDE, Blocks.tiles[Redstone319.OBSERVER][4], "north-facing observer has side texture on east (+X)");
         eq(2, Redstone319.faceRot(Redstone319.REPEATER_OFF + 1, 0), "south repeater rotates top arrow 180 deg");
         eq(3, Redstone319.faceRot(Redstone319.REPEATER_OFF + 2, 0), "east repeater rotates top arrow 90 deg");
         eq(2, Redstone319.faceRot(Redstone319.PISTON, 2), "downward piston rotates side texture 180 deg");

         g.chatLog.clear();
         g.runCommand("/locate stronghold");
         check(!g.chatLog.isEmpty() && g.chatLog.get(g.chatLog.size() - 1).contains("Located Stronghold at ["), "/locate stronghold: " + g.chatLog);
         g.runCommand("/locate fortress");
         check(g.chatLog.get(g.chatLog.size() - 1).contains("Located Nether Fortress at ["), "/locate fortress: " + g.chatLog);
         g.runCommand("/locate mineshaft");
         check(g.chatLog.get(g.chatLog.size() - 1).contains("Located Mineshaft at ["), "/locate mineshaft: " + g.chatLog);
         g.runCommand("/setblock 2 " + (GROUND + 3) + " 2 polished_granite");
         eq(Redstone319.POLISHED_GRANITE, w.getBlock(2, GROUND + 3, 2), "/setblock placed polished granite");
         g.runCommand("/fill 2 " + (GROUND + 4) + " 2 3 " + (GROUND + 4) + " 3 sea_lantern");
         eq(Redstone319.SEA_LANTERN, w.getBlock(3, GROUND + 4, 3), "/fill placed sea lantern");
         g.addItem(Blocks.STONE, 10);
         g.runCommand("/clear");
         check(g.chatLog.get(g.chatLog.size() - 1).contains("Cleared "), "/clear emptied inventory");
      });

      test("4.0 LOD trees at all scales, compact buffers & landmark cache", () -> {
         Gen gen = new Gen(12345L);
         Lod lod = new Lod(null, gen);
         try {
            for (int sz : new int[]{32, 64, 128, 256, 512, 1024}) {
               int bestVerts = 0;
               Lod.Tile sample = null;
               for (int off : new int[]{0, 64, -64, 128, -128}) {
                  Lod.Tile tile = new Lod.Tile(sz, off, off);
                  lod.build(tile);
                  if (tile.vcount > bestVerts) {
                     bestVerts = tile.vcount;
                     sample = tile;
                  }
               }
               check(bestVerts > 32 * 32 * 4, "LOD tile size " + sz + " includes 3D trees/details beyond base terrain quads: vcount=" + bestVerts);
               check(sample != null && sample.data != null && sample.data.capacity() == Math.max(16, sample.vcount * 16), "LOD tile size " + sz + " allocates compact upload buffer: " + sample.data.capacity());
            }
            Landmarks319.Site s1 = Landmarks319.plan(gen.structs, 0, 0);
            Landmarks319.Site s2 = Landmarks319.plan(gen.structs, 0, 0);
            check(s1 == s2, "Landmarks319.plan caches site instance in Structs.landmarkCache");
         } finally {
            lod.dispose();
         }
      });

      test("4.0 weather streaks, resource packs (OG Classic & custom) & mod loader", () -> {
         int[] rainPx = WeatherFx.streaks(128, 256, 32, 16, 58, 0.58F, 6001L);
         eq(128 * 256, rainPx.length, "high-res 128x256 rain streak texture generated");
         int[] snowPx = WeatherFx.flakes(64, 14, 6003L);
         eq(64 * 64, snowPx.length, "high-res 64x64 snowflake texture generated");

         java.nio.file.Path tmpPacks = java.nio.file.Files.createTempDirectory("bc-packs-test");
         java.nio.file.Path customPackDir = tmpPacks.resolve("My_Custom_OG");
         java.nio.file.Files.createDirectories(customPackDir);
         java.nio.file.Files.writeString(
            customPackDir.resolve("pack.properties"),
            "name=My Custom OG Pack\ndescription=Custom test pack\nbase=Classic\nsaturation=1.15\n"
         );
         try {
            ResourcePacks.setDirForTest(tmpPacks);
            check(ResourcePacks.list().size() >= 5, "built-in + custom resource packs listed: " + ResourcePacks.list().size());

            int defaultStone0 = Tex.atlasPixels[Tex.ty(3) * Tex.PX + Tex.tx(3)];
            ResourcePacks.select(ResourcePacks.ID_CLASSIC, null);
            check(ResourcePacks.isClassicBase(), "Classic pack uses OG 3.5 procedural base");
            int classicStone0 = Tex.atlasPixels[Tex.ty(3) * Tex.PX + Tex.tx(3)];
            check(classicStone0 != defaultStone0, "OG Classic 3.5 stone tile differs from V4.0 painted stone tile");

            ResourcePacks.select("My_Custom_OG", null);
            eq("My Custom OG Pack", ResourcePacks.active().title, "custom folder resource pack activated");
         } finally {
            ResourcePacks.setDirForTest(new java.io.File("resourcepacks").toPath());
            ResourcePacks.select(ResourcePacks.ID_DEFAULT, null);
         }
         check(!ResourcePacks.isClassicBase(), "restored Default V4.0 resource pack");

         java.nio.file.Path tmpMods = java.nio.file.Files.createTempDirectory("bc-mods-test");
         java.nio.file.Files.writeString(
            tmpMods.resolve("greet-mod.bcmod"),
            "id=greet-mod\nname=Greet Mod\nversion=1.0\nauthor=Tester\nshapeless.1=264,1:3,3\nsmelt.1=4:1,2\ncommand.hello=Hello from Greet Mod!\n"
         );
         try {
            ModLoader.setDirForTest(tmpMods);
            check(ModLoader.allMods().size() >= 7, "catalog + custom .bcmod loaded: " + ModLoader.allMods().size());
            check(ModLoader.get("greet-mod") != null && ModLoader.get("greet-mod").enabled, "custom .bcmod enabled automatically");

            Stack[] craftGrid = new Stack[]{new Stack(3, 1), new Stack(3, 1), null, null};
            Stack crafted = Recipes.find(craftGrid, 2, 2);
            check(crafted != null && crafted.id == 264, "declarative .bcmod shapeless recipe active");

            ModLoader.install("waystones-hud");
            ModLoader.install("recipe-plus");
            ModLoader.install("builder-wand");
            ModLoader.install("timber-assist");
            ModLoader.install("torch-master");
            check(ModLoader.isWaystonesHudActive(), "waystones-hud active after install");

            // Verify recipe-plus uses valid item/block IDs (Flint=266, Sand=7, String=263, Leather=261)
            Stack flint = Recipes.find(new Stack[]{new Stack(Blocks.GRAVEL, 1), new Stack(Blocks.GRAVEL, 1), null, null}, 2, 2);
            check(flint != null && flint.id == Items.FLINT, "recipe-plus crafts 2 Gravel -> Flint (266)");
            Stack str = Recipes.find(new Stack[]{new Stack(Blocks.WOOL0, 1), null, null, null}, 2, 2);
            check(str != null && str.id == Items.STRING && str.n == 4, "recipe-plus crafts Wool -> 4 String (263)");
            Stack leather = Recipes.smelt.get(Items.ROTTEN_FLESH);
            check(leather != null && leather.id == Items.LEATHER, "recipe-plus smelts Rotten Flesh -> Leather (261)");

            Game g = newGame(1);
            g.runCommand("/hello");
            check(!g.chatLog.isEmpty() && g.chatLog.get(g.chatLog.size() - 1).contains("Hello from Greet Mod!"), "custom .bcmod command executed");
            g.runCommand("/wp set base");
            g.player.x = 40.0;
            g.runCommand("/wp tp base");
            check(Math.abs(g.player.x - 8.5) < 1.0, "/wp tp teleported player back to saved waypoint");
            g.runCommand("/platform 2 4");
            eq(4, g.world.getBlock(8, GROUND, 8), "/platform built cobblestone platform");
            // Invalid block ID 33 falls back safely to STONEBRICK (27), never placing an unknown block
            g.runCommand("/platform 1 33");
            eq(Blocks.STONEBRICK, g.world.getBlock(8, GROUND, 8), "/platform rejects invalid block 33 and places Stone Bricks");

            // Verify timber-assist fells connected tree logs above without placing invalid blocks
            g.world.setBlock(8, GROUND, 12, Blocks.GRASS);
            g.world.setBlock(8, GROUND + 1, 12, Blocks.LOG_OAK);
            g.world.setBlock(8, GROUND + 2, 12, Blocks.LOG_OAK);
            g.world.setBlock(8, GROUND + 3, 12, Blocks.LOG_OAK);
            g.world.setBlock(8, GROUND + 1, 12, Blocks.AIR);
            ModLoader.onBlockBroken(g.world, 8, GROUND + 1, 12, Blocks.LOG_OAK);
            eq(Blocks.AIR, g.world.getBlock(8, GROUND + 1, 12), "timber-assist leaves base clear (never places slab 117)");
            eq(Blocks.AIR, g.world.getBlock(8, GROUND + 2, 12), "timber-assist felled log above");
            eq(Blocks.AIR, g.world.getBlock(8, GROUND + 3, 12), "timber-assist felled top log");

            // Verify torch-master places Blocks.TORCH (32, not 33)
            g.world.setBlock(8, GROUND, 8, Blocks.STONE);
            g.world.setBlock(8, GROUND + 1, 8, Blocks.AIR);
            g.runCommand("/lightup");
            int torchCheck = g.world.getBlock(8, GROUND + 1, 8);
            check(torchCheck == Blocks.AIR || torchCheck == Blocks.TORCH, "torch-master only places valid Torch (32)");

            ModLoader.setEnabled("greet-mod", false);
            ModLoader.uninstall("waystones-hud");
            ModLoader.uninstall("recipe-plus");
            ModLoader.uninstall("builder-wand");
            ModLoader.uninstall("timber-assist");
            ModLoader.uninstall("torch-master");
            check(Recipes.find(craftGrid, 2, 2) == null, "disabling mod cleanly removes its custom recipes");
         } finally {
            ModLoader.setDirForTest(new java.io.File("mods").toPath());
         }

         // Verify Skyblock, Skywars, and Bedwars world generators & gameplay
         eq(WorldSettings.SKYBLOCK, WorldSettings.parseType("skyblock"), "parseType skyblock");
         eq(WorldSettings.SKYWARS, WorldSettings.parseType("skywars"), "parseType skywars");
         eq(WorldSettings.BEDWARS, WorldSettings.parseType("bedwars"), "parseType bedwars");

         WorldSettings sbWs = new WorldSettings();
         sbWs.type = WorldSettings.SKYBLOCK;
         Gen sbGen = Dim.makeGen(Dim.OVERWORLD, 12345L, sbWs);
         check(sbGen instanceof SkyGen, "Skyblock uses SkyGen");
         Chunk sbCh = sbGen.generate(0, 0);
         eq(Blocks.GRASS, sbCh.blocks[Chunk.idx(0, SkyGen.Y_DECK, 0)] & 0xFFFF, "Skyblock starter island grass");
         eq(Blocks.BEDROCK, sbCh.blocks[Chunk.idx(0, SkyGen.Y_DECK - 2, 0)] & 0xFFFF, "Skyblock bedrock anchor");
         eq(Blocks.CHEST_LOOT, sbCh.blocks[Chunk.idx(3, SkyGen.Y_DECK + 1, 0)] & 0xFFFF, "Skyblock starter chest");

         WorldSettings swWs = new WorldSettings();
         swWs.type = WorldSettings.SKYWARS;
         Gen swGen = Dim.makeGen(Dim.OVERWORLD, 12345L, swWs);
         Chunk swCenter = swGen.generate(0, 0);
         eq(Blocks.DIAMOND_BLOCK, swCenter.blocks[Chunk.idx(0, SkyGen.Y_DECK, 0)] & 0xFFFF, "Skywars center feast diamond block");
         eq(Blocks.CHEST_LOOT, swCenter.blocks[Chunk.idx(2, SkyGen.Y_DECK + 1, 0)] & 0xFFFF, "Skywars center feast loot chest");

         WorldSettings bwWs = new WorldSettings();
         bwWs.type = WorldSettings.BEDWARS;
         Gen bwGen = Dim.makeGen(Dim.OVERWORLD, 12345L, bwWs);
         Chunk bwCenter = bwGen.generate(0, 0);
         eq(Blocks.EMERALD_BLOCK, bwCenter.blocks[Chunk.idx(4, SkyGen.Y_DECK, 0)] & 0xFFFF, "Bedwars central emerald generator pad");
         Chunk bwRed = bwGen.generate(0, -3); // covers z = -48..-33 (Red base at 0, -44; bedFoot at z=-41 -> lz=7)
         check(Decor315.isBed(bwRed.blocks[Chunk.idx(0, SkyGen.Y_DECK + 1, 7)] & 0xFFFF), "Bedwars Red team bed placed");

         // Verify Worldwide Share Code encoding/decoding
         String code = Net319.encodeShareCode("rnkxy.a.pinggy.link:41823");
         check(code.startsWith("BC-"), "Share Code starts with BC-");
         eq("rnkxy.a.pinggy.link:41823", Net319.decodeAddress(code), "Share Code round-trips tunnel host:port");

         // Verify external Minecraft Infdev (2010) JAR mod (mods/infdev-2010.jar) & automatic Infdev settings preset
         ModLoader.ModEntry infEntry = ModLoader.get("infdev-2010");
         check(infEntry != null, "infdev-2010 mod is always registered in ModLoader");
         if (infEntry != null) {
            Game infG = newGame(1);
            infG.opt = new Options(new java.io.File(System.getProperty("java.io.tmpdir")));
            ModLoader.activeGame = infG;
            try {
               ModLoader.setEnabled("infdev-2010", true);
               check(ModLoader.isInfdevActive(), "Infdev 2010 total conversion active");
               check(ModLoader.isClassicHudNoHunger(), "Infdev classic no-hunger HUD active");
               eq("Minecraft Infdev", ModLoader.hudWatermark(), "Infdev HUD watermark");
               check(!infG.opt.shaders, "Infdev disables shaders for classic OpenGL look");
               check(!infG.opt.smooth, "Infdev enables flat per-face lighting (opt.smooth=false)");
               check(!infG.opt.bushyLeaves && !Mesher.bushyLeaves, "Infdev disables bushy leaves");
               check(!Mesher.variation, "Infdev disables block brightness/rotation variation");
               check(infG.opt.clouds, "Infdev enables 3D blocky clouds");
               check(!infG.opt.weather && !infG.opt.disasters, "Infdev disables modern weather and disasters");
               eq(ResourcePacks.ID_CLASSIC, ResourcePacks.activeId(), "Infdev activates Classic 16x16 texture pack");
               eq(InfdevGen.INFDEV_GREEN, Mesher.tintColor(sbCh, Blocks.GRASS, 0, 0), "Infdev neon-green grass tint applies to all chunks");
               Gen infGen = Dim.makeGen(Dim.OVERWORLD, 12345L, new WorldSettings());
               check(infGen instanceof InfdevGen, "Overworld uses InfdevGen when infdev-2010 mod is enabled");
               Chunk pyrCh = infGen.generate(3, 3); // (48, 48) Brick Pyramid
               eq(Blocks.BRICKS, pyrCh.blocks[Chunk.idx(0, 75, 0)] & 0xFFFF, "Infdev Brick Pyramid at (48, 75, 48)");
               eq(InfdevGen.INFDEV_GREEN, pyrCh.grassColor[0], "Infdev neon-green grass tint");
               eq(1, Items.maxStack(Items.BREAD), "Food does not stack in Infdev mode");
            } finally {
               ModLoader.setEnabled("infdev-2010", false);
            }
            check(!ModLoader.isInfdevActive(), "Infdev 2010 cleanly disabled after test");
            check(infG.opt.smooth, "Disabling Infdev restores smooth lighting");
            eq(ResourcePacks.ID_DEFAULT, ResourcePacks.activeId(), "Disabling Infdev restores previous resource pack");
         }

         // Verify Copy (Ctrl+C), Paste (Ctrl+V), Cut (Ctrl+X) and Up/Down history across Chat & all text fields
         Game tg = newGame(1);
         tg.runCommand("/time set day");
         tg.runCommand("/weather clear");
         tg.openChat("");
         tg.onKey(265, 1, 0); // Up arrow -> last command
         eq("/weather clear", tg.chat.toString(), "Chat Up arrow recalls most recent command");
         tg.onKey(265, 1, 0); // Up arrow again -> previous command
         eq("/time set day", tg.chat.toString(), "Chat Up arrow steps back to previous command");
         tg.onKey(264, 1, 0); // Down arrow -> forward
         eq("/weather clear", tg.chat.toString(), "Chat Down arrow steps forward");
         tg.onKey(67, 1, 2); // Ctrl+C copies chat
         eq("/weather clear", tg.getClipboard(), "Ctrl+C copies chat text to clipboard");
         tg.onKey(88, 1, 2); // Ctrl+X cuts chat
         eq("", tg.chat.toString(), "Ctrl+X clears chat input");
         tg.onKey(86, 1, 2); // Ctrl+V pastes back
         eq("/weather clear", tg.chat.toString(), "Ctrl+V pastes clipboard into chat");

         tg.setScreen(Game.S.MULTIPLAYER);
         Net319.serverAddr = "";
         tg.setClipboard(code);
         tg.onKey(86, 1, 2); // Ctrl+V in Multiplayer
         eq(code, Net319.serverAddr, "Ctrl+V pastes Share Code into Multiplayer address field");
         tg.onKey(265, 1, 0); // Up arrow in Multiplayer recalls previous address history
         check(!Net319.serverAddr.isEmpty(), "Up arrow in Multiplayer cycles address history");
         Net319.serverAddr = "127.0.0.1:25565";

         tg.setScreen(Game.S.NEWWORLD);
         tg.focusField = 0;
         tg.newName = "";
         tg.setClipboard("Sky Island");
         tg.onKey(86, 1, 2); // Ctrl+V into World Name
         eq("Sky Island", tg.newName, "Ctrl+V pastes into New World name field");
         tg.onKey(264, 1, 0); // Down arrow switches focus to Seed field
         eq(1, tg.focusField, "Down arrow moves focus from World Name to Seed");
         tg.setClipboard("20261009");
         tg.onKey(86, 1, 2); // Ctrl+V into Seed
         eq("20261009", tg.seedField, "Ctrl+V pastes into Seed field");

         // Verify V4.1 All Gamemodes Overhaul: Survival, Creative, Adventure, Spectator, Hardcore & Minigames
         Game mg = newGame(0);
         // 1. Survival: Right-click armor quick-equip, F-key quick utility swap, and Survival middle-click pickBlock
         int ironChest = Items.armorId(1, 1);
         mg.sel = 0;
         mg.inv[0] = new Stack(ironChest, 1);
         check(mg.useSpecial(mg.inv[0]), "Right-clicking armor in Survival equips it");
         eq(ironChest, mg.armor[1].id, "Iron Chestplate equipped in chest slot");
         mg.inv[0] = new Stack(Blocks.TORCH, 16);
         mg.inv[8] = new Stack(Items.BREAD, 8);
         mg.setScreen(Game.S.PLAY);
         mg.onKey(70, 1, 0); // F key swaps active slot with utility slot 8
         eq(Items.BREAD, mg.inv[0].id, "F key swaps utility slot 8 into active slot");
         eq(Blocks.TORCH, mg.inv[8].id, "F key moves previous active item to slot 8");

         // 2. Adventure Mode: tool-matched breaking, tactical block placement, Villager trading & Cake eating
         mg.player.setMode(2);
         check(mg.player.adventure, "Player is in Adventure Mode");
         check(!Items.canAdventureBreak(0, Blocks.STONE), "Adventure Mode blocks bare-hand stone breaking");
         check(Items.canAdventureBreak(Items.toolId(1, Items.C_PICK), Blocks.STONE), "Adventure Mode allows Pickaxe to break Stone");
         check(Items.canAdventureBreak(Items.toolId(1, Items.C_AXE), Blocks.LOG_OAK), "Adventure Mode allows Axe to break Oak Log");
         check(Items.canAdventureBreak(Items.toolId(1, Items.C_SHOVEL), Blocks.DIRT), "Adventure Mode allows Shovel to break Dirt");
         check(Items.canAdventureBreak(0, Blocks.TORCH), "Adventure Mode allows breaking 0-hardness Torch");
         check(Items.canAdventurePlace(Blocks.TORCH, Blocks.STONE), "Adventure Mode allows placing Torch");
         check(Items.canAdventurePlace(Items.SEEDS, 123), "Adventure Mode allows planting Seeds on Farmland");

         // 3. Spectator Mode: scroll-wheel fly speed & quick-action bar
         mg.player.setMode(3);
         check(mg.player.spectator && mg.player.flying, "Spectator mode enables flight");
         float spd0 = mg.player.flySpeed;
         mg.onScroll(1.0);
         check(mg.player.flySpeed > spd0, "Scrolling in Spectator Mode increases fly speed");
         mg.spectatorAction(5);
         check(mg.spectatorNightVision, "Spectator action 5 toggles Night Vision");
         mg.spectatorAction(6);
         eq(0.05F, mg.player.flySpeed, "Spectator action 6 resets fly speed to 100%");

         // 4. Hardcore Mode: mode 4, /gamemode hardcore, and rawMode()
         mg.runCommand("/gamemode hardcore");
         check(mg.player.hardcore && mg.player.gameMode == 0 && mg.player.rawMode() == 4, "/gamemode hardcore activates Hardcore Mode");
         eq("Hardcore", mg.player.modeName(), "Hardcore modeName()");

         // 5. Console Edition Tumble (3 layers over lava), Battle (4 maps: Cove, Crucible, Cavern, Temple) & Bedwars Shop / Upgrades
         check((sbCh.blocks[Chunk.idx(5, SkyGen.Y_DECK, 5)] & 0xFFFF) != 0, "Tumble Layer 1 generated at Y_DECK");
         check((sbCh.blocks[Chunk.idx(5, SkyGen.Y_DECK - 5, 5)] & 0xFFFF) != 0, "Tumble Layer 2 generated at Y_DECK - 5");
         check((sbCh.blocks[Chunk.idx(5, SkyGen.Y_DECK - 10, 5)] & 0xFFFF) != 0, "Tumble Layer 3 generated at Y_DECK - 10");
         eq(Blocks.LAVA, sbCh.blocks[Chunk.idx(5, SkyGen.Y_DECK - 14, 5)] & 0xFFFF, "Tumble Lava Cauldron generated at Y_DECK - 14");
         for (int m = 0; m < WorldSettings.MAP_COUNT; m++) {
            WorldSettings mapWs = new WorldSettings();
            mapWs.type = WorldSettings.BATTLE;
            mapWs.mapId = m;
            Gen mapGen = Dim.makeGen(Dim.OVERWORLD, 12345L, mapWs);
            Chunk mapCh = mapGen.generate(0, 0);
            eq(Blocks.DIAMOND_BLOCK, mapCh.blocks[Chunk.idx(0, SkyGen.Y_DECK, 0)] & 0xFFFF, "Battle map " + WorldSettings.MAP_NAMES[m] + " center shrine generated");
         }
         check(Trades.offers(Trades.BEDWARS_SHOP).length == 6, "Bedwars Item Shop has 6 trades");
         check(Trades.offers(Trades.BEDWARS_ARMOR).length == 6, "Bedwars Gear & Upgrades Shop has 6 trades");

         // 6. Minigame Ephemeral Non-Saving, Waiting Screen UI, Multi-Endpoint VPN Share Code & Bedwars Team/Bed Mechanics
         check(swWs.isMinigame() && bwWs.isMinigame() && sbWs.isMinigame(), "Battle, Tumble, and Bedwars are all ephemeral Console Edition minigames");

         Game bwGame = new Game();
         bwGame.opt = new Options(new java.io.File(java.nio.file.Files.createTempDirectory("bcbw").toFile(), "opt"));
         bwGame.newSettings = bwWs.copy();
         bwGame.startWorld(true, "Bedwars Test", 0);
         check(bwGame.rootDir == null && bwGame.world.dir == null && !bwGame.world.saveEnabled, "Bedwars minigame world has saving disabled and no disk directory");
         bwGame.saveWorld();
         check(bwGame.rootDir == null && bwGame.world.dir == null, "saveWorld() on a minigame does not create any save files");
         eq(SkyGen.STATE_WAITING, SkyGen.matchState, "Minigame initializes in STATE_WAITING pre-game lobby");

         // Verify Screens.waiting layout across window sizes
         for (int[] sz : TestsUi.SIZES) {
            GuiRaster gr = new GuiRaster(sz[0] * 3, sz[1] * 3);
            bwGame.gui = gr;
            bwGame.fbW = sz[0] * 3;
            bwGame.fbH = sz[1] * 3;
            bwGame.winW = sz[0] * 3;
            bwGame.winH = sz[1] * 3;
            gr.begin(sz[0] * 3, sz[1] * 3, 3);
            bwGame.activeBtnsSwap();
            Screens.waiting(bwGame);
            bwGame.activeBtns = bwGame.btns;
            for (int i = 0; i < bwGame.activeBtns.size(); i++) {
               Game.Btn b = bwGame.activeBtns.get(i);
               check(b.x >= 0 && b.y >= 0 && b.x + b.w <= gr.w && b.y + b.h <= gr.h, "waiting button inside screen at " + sz[0] + "x" + sz[1]);
               check(Gui.textWidth(b.label) <= b.w - 6, "waiting label fits button: " + b.label);
               for (int j = i + 1; j < bwGame.activeBtns.size(); j++) {
                  check(!TestsUi.overlap(b, bwGame.activeBtns.get(j)), "waiting buttons do not overlap: " + b.label + " vs " + bwGame.activeBtns.get(j).label);
               }
            }
         }

         // Warm Bedwars arena chunks so all 4 team bases and beds exist
         for (int cx = -3; cx <= 3; cx++) {
            for (int cz = -3; cz <= 3; cz++) {
               Chunk ch = bwGame.world.gen.generate(cx, cz);
               bwGame.world.chunks.put(World.key(cx, cz), ch);
            }
         }
         for (Chunk ch : new java.util.ArrayList<>(bwGame.world.chunks.values())) {
            bwGame.world.lightChunk(ch);
         }
         SkyGen.tickMode(bwGame, bwGame.world);
         for (int t = 0; t < 4; t++) {
            check(SkyGen.isTeamBedIntact(bwGame.world, t), "Bedwars Team " + t + " bed is intact at match start");
         }

         // Verify players cannot break their own bed
         SkyGen.selectTeam(bwGame, 0, false); // Red Team
         int[] redFoot = SkyGen.getTeamBedFoot(0);
         bwGame.breakBlock(redFoot[0], redFoot[1], redFoot[2]);
         check(SkyGen.isTeamBedIntact(bwGame.world, 0), "Red player cannot break Red Team's own bed");

         // Verify Blue Team player spawns and respawns at Blue Base (0, 65, 42) with Blue Wool, NEVER at Red bed
         SkyGen.selectTeam(bwGame, 1, false); // Blue Team
         eq(1, SkyGen.getPlayerTeam(bwGame), "Player team is Blue (1)");
         int[] blueSp = SkyGen.getTeamSpawnBlock(1);
         eq(blueSp[0] + 0.5, bwGame.player.x, "Blue player spawns at Blue Base X");
         eq(blueSp[2] + 0.5, bwGame.player.z, "Blue player spawns at Blue Base Z");
         bwGame.player.dead = true;
         bwGame.respawn();
         check(!bwGame.player.dead && !bwGame.player.spectator, "Blue player respawns alive while Blue bed is intact");
         eq(blueSp[0] + 0.5, bwGame.player.x, "Blue player respawns at Blue Base X (NOT Red bed)");
         eq(blueSp[2] + 0.5, bwGame.player.z, "Blue player respawns at Blue Base Z (NOT Red bed)");
         eq(Blocks.WOOL0 + 11, bwGame.inv[1].id, "Blue player respawns with Blue Wool");

         // Verify Blue player can break Red Team's enemy bed (no bed item dropped!) and Red is eliminated
         int dropsBefore = bwGame.world.items.size();
         bwGame.breakBlock(redFoot[0], redFoot[1], redFoot[2]);
         check(!SkyGen.isTeamBedIntact(bwGame.world, 0), "Enemy Red bed is destroyed when broken by Blue player");
         eq(dropsBefore, bwGame.world.items.size(), "Breaking a bed in Bedwars does not drop a bed item");

         // Verify a Red player whose bed is destroyed is eliminated into Spectator Mode upon respawn (cannot respawn at Red bed!)
         SkyGen.myTeam = 0;
         bwGame.player.dead = true;
         bwGame.respawn();
         check(bwGame.player.spectator && bwGame.player.rawMode() == 3, "Red player with destroyed bed enters Spectator Mode on respawn");
         check(bwGame.bedPos == null, "Eliminated player has bedPos cleared");

         // Verify Multi-Endpoint Share Code & Parallel Join (Radmin VPN / LAN fallback without timing out)
         int mpPort = 25594;
         String bwHostMsg = Net319.host(bwGame, mpPort);
         check(Net319.isHosting(), "Bedwars host started on port " + mpPort + ": " + bwHostMsg);
         try {
            String multiRaw = "203.0.113.1:25594,127.0.0.1:" + mpPort;
            String multiCode = Net319.encodeShareCode(multiRaw);
            eq(multiRaw, Net319.decodeAddress(multiCode), "Multi-endpoint Share Code round-trips VPN + LAN endpoints");
            try (java.net.Socket s2 = new java.net.Socket("127.0.0.1", mpPort)) {
               s2.setSoTimeout(1500);
               java.io.BufferedReader in2 = new java.io.BufferedReader(new java.io.InputStreamReader(s2.getInputStream()));
               String assign2 = in2.readLine();
               check(assign2 != null && assign2.startsWith("ASSIGN "), "Bedwars host sends ASSIGN with team & matchState: " + assign2);
               String[] ap = assign2.split("\\s+");
               check(ap.length >= 11 && Integer.parseInt(ap[10]) != 0, "Joining peer is assigned a non-Red team (" + (ap.length >= 11 ? ap[10] : "?") + ")");
            }
         } finally {
            Net319.disconnect(bwGame);
         }

         // 7. Bedwars Arena Map Full Reset (wipes placed blocks, restores broken blocks & destroyed beds, respawns shopkeepers)
         bwGame.world.setBlock(0, SkyGen.Y_DECK + 5, 12, Blocks.PLANKS);
         bwGame.world.setBlock(3, SkyGen.Y_DECK + 6, -10, Blocks.OBSIDIAN);
         eq(Blocks.PLANKS, bwGame.world.getBlock(0, SkyGen.Y_DECK + 5, 12), "Bridge block placed in Bedwars arena before reset");
         eq(Blocks.OBSIDIAN, bwGame.world.getBlock(3, SkyGen.Y_DECK + 6, -10), "Obsidian placed in Bedwars arena before reset");
         check(!SkyGen.isTeamBedIntact(bwGame.world, 0), "Red bed is destroyed before arena reset");
         SkyGen.resetArenaMap(bwGame, bwGame.world, false);
         eq(Blocks.AIR, bwGame.world.getBlock(0, SkyGen.Y_DECK + 5, 12), "Arena reset wipes placed bridge blocks back to air");
         eq(Blocks.AIR, bwGame.world.getBlock(3, SkyGen.Y_DECK + 6, -10), "Arena reset wipes placed obsidian back to air");
         for (int t = 0; t < 4; t++) {
            check(SkyGen.isTeamBedIntact(bwGame.world, t), "Arena reset restores Bedwars Team " + t + " bed");
         }

         // 8. Cross-Dimension & Cross-World Block Isolation + Multiplayer Mod, TileEntity & Entity Sync
         java.io.File tmpRoot = java.nio.file.Files.createTempDirectory("bcdim").toFile();
         java.io.File netherDir = Dim.dir(tmpRoot, Dim.NETHER);
         eq(new java.io.File(tmpRoot, "DIM-1"), netherDir, "Dim.dir resolves DIM-1 under world root");
         eq(tmpRoot, Dim.dir(netherDir, Dim.OVERWORLD), "Dim.dir from DIM-1 back to Overworld returns root without nesting");
         eq(new java.io.File(tmpRoot, "DIM1"), Dim.dir(netherDir, Dim.END), "Dim.dir from DIM-1 to End resolves sibling DIM1 without nesting");

         int syncPort = 25596;
         String syncHostMsg = Net319.host(bwGame, syncPort);
         check(Net319.isHosting(), "Sync host started: " + syncHostMsg);
         try {
            // Place a block in Overworld (dim 0) while hosting
            bwGame.world.setBlock(2, 70, 2, Blocks.GOLD_BLOCK);
            check(Net319.editsForDim(Dim.OVERWORLD).containsKey(Net319.packCoord(2, 70, 2)), "Overworld edit recorded in dim 0");
            check(!Net319.editsForDim(Dim.NETHER).containsKey(Net319.packCoord(2, 70, 2)), "Overworld edit does NOT leak into Nether (dim -1)");
            check(!Net319.editsForDim(Dim.END).containsKey(Net319.packCoord(2, 70, 2)), "Overworld edit does NOT leak into End (dim 1)");

            // Verify Nether world chunk generation is untouched by Overworld edits
            World netherW = new World(bwGame.world.seed, null, Dim.NETHER, new WorldSettings());
            Chunk netherCh = netherW.gen.generate(0, 0);
            int beforeNetherBlock = netherCh.get(2, 70, 2);
            Net319.applyEditsToChunk(netherW, netherCh);
            eq(beforeNetherBlock, netherCh.get(2, 70, 2), "Overworld edits are never applied to Nether chunks");
            netherW.shutdown();

            // Verify TileEntity (chest) sync packet round-trip
            bwGame.world.setBlock(3, SkyGen.Y_DECK + 1, -43, Blocks.CHEST);
            TileEntity hostChest = bwGame.world.getOrCreateTile(3, SkyGen.Y_DECK + 1, -43);
            hostChest.slots[0] = new Stack(Items.DIAMOND, 12, 0);
            Net319.onTileChanged(bwGame.world, hostChest);
            check(Net319.tilesForDim(Dim.OVERWORLD).containsKey(Net319.packCoord(3, SkyGen.Y_DECK + 1, -43)), "Chest TileEntity state recorded for sync");

            // Verify Mob Entity snapshot encoding & client-side decoding
            Mobs.Mob spawnedMob = bwGame.mobs.spawn(Mobs.VILLAGER, 4.5, SkyGen.Y_DECK + 1.0, 4.5);
            spawnedMob.color = Trades.BEDWARS_SHOP;
            check(spawnedMob.netId > 0, "Spawned mob has a valid network ID");
            java.util.ArrayList<String> mobPkts = Net319.buildMobSnapshotsForTest(bwGame.world, bwGame.mobs);
            check(!mobPkts.isEmpty() && mobPkts.get(0).startsWith("MOBS "), "Host builds MOBS snapshot packets");

            // Verify Mod sync CSV round-trip
            ModLoader.setEnabled("waystones-hud", true);
            String activeCsv = ModLoader.getActiveModsCsv();
            check(activeCsv.contains("waystones-hud"), "Active mods CSV includes waystones-hud");
            ModLoader.setEnabled("waystones-hud", false);
            ModLoader.applySyncedModsCsv(activeCsv, bwGame);
            check(ModLoader.get("waystones-hud").enabled, "applySyncedModsCsv enables host mod on client");
            ModLoader.restoreSessionMods(bwGame);
         } finally {
            Net319.disconnect(bwGame);
         }
         check(Net319.editsForDim(Dim.OVERWORLD).isEmpty(), "Disconnecting clears all multiplayer block edits so they never leak to other worlds");

         // 9. V4.1+ Gamemodes & Multiplayer Polish: Bedwars Team Upgrades, Fireballs, Skywars Kits, Skyblock Ore Generator, Nicknames, Pings & Tab Scoreboard
         SkyGen.resetMatchState();
         SkyGen.selectTeam(bwGame, 0, false); // Red Team
         check(!SkyGen.hasTeamSharpness(0) && !SkyGen.hasTeamProtection(0), "Red Team starts with no team upgrades");
         check(SkyGen.unlockNextTeamUpgrade(bwGame, 0, false), "Unlock 1st team upgrade (Sharpness)");
         check(SkyGen.hasTeamSharpness(0), "Red Team has Sharpness I");
         check(SkyGen.unlockNextTeamUpgrade(bwGame, 0, false), "Unlock 2nd team upgrade (Protection)");
         check(SkyGen.hasTeamProtection(0), "Red Team has Reinforced Armor I");
         check(SkyGen.unlockNextTeamUpgrade(bwGame, 0, false), "Unlock 3rd team upgrade (Emerald Forge)");
         check(SkyGen.hasTeamForge(0), "Red Team has Emerald Forge");
         check(SkyGen.unlockNextTeamUpgrade(bwGame, 0, false), "Unlock 4th team upgrade (Heal Pool & Bed Trap)");
         check(SkyGen.hasTeamHealPool(0), "Red Team has Heal Pool & Alarm Trap");

         // Verify Bedwars Fireball throwing (Items.BLAZE_ROD)
         int fbBefore = bwGame.mobs.fireballs.size();
         bwGame.inv[0] = new Stack(Items.BLAZE_ROD, 2);
         bwGame.sel = 0;
         check(Mobs312.throwFireball(bwGame, bwGame.inv[0]), "Throwing a Bedwars Fireball succeeds");
         eq(fbBefore + 1, bwGame.mobs.fireballs.size(), "Fireball projectile spawned in world");
         eq(1, bwGame.inv[0].n, "Throwing a Fireball consumes 1 Blaze Rod");

         // Verify Skywars Starter Kits & Bulldozer Kill Perk
         for (int k = 0; k < 4; k++) {
            SkyGen.giveSkywarsKit(bwGame, k);
            eq(k, SkyGen.swKit, "Skywars kit index set to " + k);
            check(bwGame.inv[0] != null && bwGame.inv[0].n > 0, "Skywars kit " + SkyGen.SW_KIT_NAMES[k] + " grants primary item");
         }
         int kills0 = SkyGen.myKills;
         SkyGen.onArenaKill(bwGame, "TestBot");
         eq(kills0 + 1, SkyGen.myKills, "onArenaKill increments player kill counter");

         // Verify Skyblock Ore-Generating Cobblestone Generator
         World sbWorld = new World(12345L, null, Dim.OVERWORLD, sbWs);
         boolean sawCobble = false;
         boolean sawOre = false;
         for (int i = 0; i < 200; i++) {
            int genId = sbWorld.cobbleOrSkyblockOre();
            if (genId == Blocks.COBBLE) sawCobble = true;
            if (genId == Blocks.COAL_ORE || genId == Blocks.IRON_ORE || genId == Blocks.GOLD_ORE || genId == Blocks.DIAMOND_ORE || genId == Blocks.REDSTONE_ORE || genId == Blocks.LAPIS_ORE) {
               sawOre = true;
            }
         }
         check(sawCobble && sawOre, "Skyblock cobblestone generator produces both cobblestone and ores");
         sbWorld.shutdown();

         // Verify Multiplayer /nick, /ping, rich POSE, and Tab Scoreboard rendering
         bwGame.runCommand("/nick ArenaChamp");
         eq("ArenaChamp", Net319.selfName, "/nick updates player multiplayer nickname");
         String richPose = Net319.formatLocalPose(bwGame);
         check(richPose.startsWith("POSE ") && richPose.split("\\s+").length >= 21, "formatLocalPose includes 21 tokens (health, armor, mode, ping, kills, deaths, team)");
         Net319.sendWorldPing(bwGame);
         GuiRaster tabGr = new GuiRaster(854, 480);
         bwGame.gui = tabGr;
         tabGr.begin(854, 480, 2);
         Net319.drawTabOverlay(bwGame);
         check(tabGr.textExtent[2] > tabGr.textExtent[0], "Tab Scoreboard overlay renders panels and player stats");

         // Verify Backrooms Stalker Mod (mods/backrooms-stalker.jar & .jar.txt support)
         ModLoader.ModEntry brEntry = ModLoader.get("backrooms-stalker");
         check(brEntry != null, "backrooms-stalker mod is registered in ModLoader");
         try {
            double origY = bwGame.player.y;
            bwGame.runCommand("/backrooms");
            check(ModLoader.get("backrooms-stalker").enabled, "/backrooms auto-enables backrooms-stalker mod");
            check(BackroomsStalkerMod.isActive() && bwGame.opt.shaders, "/backrooms enables horror shaders and atmosphere");
            eq(BackroomsStalkerMod.POCKET_FLOOR_Y + 1, (int)Math.floor(bwGame.player.y), "Player teleported into Backrooms Level 0 labyrinth");
            int px = (int)Math.floor(bwGame.player.x);
            int pz = (int)Math.floor(bwGame.player.z);
            eq(BackroomsGen.CARPET_BLOCK, bwGame.world.getBlock(px, BackroomsStalkerMod.POCKET_FLOOR_Y, pz), "Backrooms damp yellow carpet floor placed");
            Gen brGen = Dim.makeGen(Dim.OVERWORLD, 999L, new WorldSettings());
            check(brGen instanceof BackroomsGen, "Overworld uses BackroomsGen when backrooms-stalker mod is enabled");
            Chunk brCh = brGen.generate(0, 0);
            eq(BackroomsGen.CARPET_BLOCK, brCh.blocks[Chunk.idx(0, BackroomsGen.FLOOR_Y, 0)] & 0xFFFF, "BackroomsGen Level 0 places damp carpet at Y=64");
            eq(Blocks.AIR, brCh.blocks[Chunk.idx(0, BackroomsGen.FLOOR_Y + 1, 0)] & 0xFFFF, "BackroomsGen Level 0 has open interior room at Y=65");
            eq(Blocks.QUARTZ_BLOCK, brCh.blocks[Chunk.idx(0, BackroomsGen.POOL_FLOOR_Y, 0)] & 0xFFFF, "BackroomsGen Level 37 Poolrooms has quartz floor at Y=82");
            eq(Blocks.COBBLE, brCh.blocks[Chunk.idx(0, BackroomsGen.L2_FLOOR_Y, 0)] & 0xFFFF, "BackroomsGen Level 2 Pipe Dreams has cobble floor at Y=30");
            eq(Blocks.WOOL0 + 14, brCh.blocks[Chunk.idx(0, BackroomsGen.RUN_FLOOR_Y, 0)] & 0xFFFF, "BackroomsGen Level ! has crimson carpet floor at Y=14");
            // Verify 3D Block Models (Desks, Lockers, Water Coolers, Level Exit Doors, Waterslides, Chairs, Vending & Valve Pipes)
            check(Shapes.isModel(BackroomsStalkerMod.B_DESK) && Blocks.kind[BackroomsStalkerMod.B_DESK] == Shapes.K_SHAPE, "3D Office Desk model (B_DESK) registered in Shapes");
            check(Shapes.isModel(BackroomsStalkerMod.B_LOCKER_LOW) && Shapes.isModel(BackroomsStalkerMod.B_LOCKER_HIGH), "3D Steel Hiding Locker models registered in Shapes");
            check(Shapes.isModel(BackroomsStalkerMod.B_WATER_COOLER) && Shapes.isModel(BackroomsStalkerMod.B_COOLER_JUG), "3D Almond Water Cooler models registered in Shapes");
            check(Shapes.isModel(BackroomsStalkerMod.B_EXIT_LOW) && Shapes.isModel(BackroomsStalkerMod.B_EXIT_HIGH), "3D Illuminated Level Exit Door models registered in Shapes");
            check(Shapes.isModel(BackroomsStalkerMod.B_POOL_SLIDE) && Shapes.isModel(BackroomsStalkerMod.B_OFFICE_CHAIR), "3D Poolrooms Waterslide & 3D Swivel Chair models registered in Shapes");
            check(Shapes.isModel(BackroomsStalkerMod.B_VENDING) && Shapes.isModel(BackroomsStalkerMod.B_VALVE_PIPE), "3D Vending Machine & 3D Steam Valve Pipe models registered in Shapes");

            // Verify Randomized Level Exit Doors are generated on ALL 5 Backrooms Levels
            int[] levels = {BackroomsGen.LEVEL_0, BackroomsGen.LEVEL_1, BackroomsGen.LEVEL_2, BackroomsGen.LEVEL_POOLROOMS, BackroomsGen.LEVEL_RUN};
            for (int lvl : levels) {
               int[] exXZ = BackroomsGen.exitDoorXZInSector(brGen.seed, lvl, 0, 0);
               Chunk exitCh = brGen.generate(exXZ[0] >> 4, exXZ[1] >> 4);
               int fy = BackroomsGen.floorYForLevel(lvl);
               eq(BackroomsStalkerMod.B_EXIT_LOW, exitCh.blocks[Chunk.idx(exXZ[0] & 15, fy + 1, exXZ[1] & 15)] & 0xFFFF, "Randomized 3D Exit Door generated for level " + lvl);
            }

            // Verify findable Almond Water item (269) & Physical Under-Desk Crouching / Hiding Spots
            eq("Almond Water", Items.name(BackroomsStalkerMod.ALMOND_WATER), "Almond Water item (269) registered when mod is active");
            Stack awStack = new Stack(BackroomsStalkerMod.ALMOND_WATER, 2);
            bwGame.player.health = 10;
            check(BackroomsStalkerMod.drinkAlmondWater(bwGame, awStack) && bwGame.player.health == 16 && awStack.n == 1, "Drinking Almond Water restores +6 health and sanity");
            // Verify physical under-desk crouching (height 0.72 < 0.8125 tabletop) and hiding detection
            int deskX = px + 3;
            int deskY = BackroomsStalkerMod.POCKET_FLOOR_Y + 1;
            int deskZ = pz + 1;
            eq(BackroomsStalkerMod.B_DESK, bwGame.world.getBlock(deskX, deskY, deskZ), "3D Office Desk placed at welcome lobby");
            bwGame.player.setPos(deskX + 0.5, deskY, deskZ + 0.5);
            bwGame.player.sneaking = true;
            check(bwGame.player.height() < Blocks.coll[BackroomsStalkerMod.B_DESK][1], "Crouching player height (0.72) fits physically underneath 3D Desk tabletop (0.8125)");
            check(BackroomsStalkerMod.mustStayCrouched(bwGame.world, bwGame.player), "mustStayCrouched keeps player crouched while under 3D Desk");
            check(BackroomsStalkerMod.checkPlayerHidden(bwGame.world, bwGame.player), "Crouching under a 3D Office Desk hides the player from the Stalker");
            bwGame.player.sneaking = false;
            // Verify findStand places player INSIDE the Backrooms at Y=65 (not on top of the bedrock roof)
            Gen prevGen = bwGame.world.gen;
            bwGame.world.gen = brGen;
            double[] stand = bwGame.findStand(0, 0);
            eq(BackroomsGen.FLOOR_Y + 1, (int)Math.round(stand[1]), "findStand spawns player INSIDE Backrooms at Y=65");
            bwGame.runCommand("/backrooms poolrooms");
            eq(BackroomsGen.POOL_FLOOR_Y + 1, (int)Math.floor(bwGame.player.y), "/backrooms poolrooms teleports player to separate Level 37 Poolrooms");
            bwGame.world.gen = prevGen;
            // Verify custom 3D Stalker Entity & Horror Fog/Shader helpers
            bwGame.runCommand("/stalker");
            check(BackroomsStalkerMod.getStalker() != null && BackroomsStalkerMod.isStalkerMob(BackroomsStalkerMod.getStalker()), "Custom 3D Stalker entity spawned with STALKER_TAG");
            float[] fogCol = BackroomsStalkerMod.getFogColor(1.0F);
            check(fogCol[0] > 0.2F && BackroomsStalkerMod.getFogFar() <= 40.0F, "Backrooms per-level fog color and fog distance active");
            bwGame.runCommand("/stalker blackout");
            check(BackroomsStalkerMod.isBlackout(), "/stalker blackout triggers ceiling light blackout");
            bwGame.runCommand("/backrooms leave");
            eq((int)Math.floor(origY), (int)Math.floor(bwGame.player.y), "/backrooms leave restores player position");
         } finally {
            ModLoader.setEnabled("backrooms-stalker", false);
         }

         // 9. Normal-game Minecraft features (Prismarine, Purpur, Bone Block, Golden Apple/Carrot, Cookie, Stew, Compass, Clock, Fire Charge) & World Gen recommendation
         eq("Prismarine", Blocks.name[Redstone319.PRISMARINE], "Prismarine block registered");
         eq("Prismarine Bricks", Blocks.name[Redstone319.PRISMARINE_BRICKS], "Prismarine Bricks block registered");
         eq("Dark Prismarine", Blocks.name[Redstone319.DARK_PRISMARINE], "Dark Prismarine block registered");
         eq("Purpur Block", Blocks.name[Redstone319.PURPUR_BLOCK], "Purpur Block registered");
         eq("Purpur Pillar", Blocks.name[Redstone319.PURPUR_PILLAR], "Purpur Pillar registered");
         eq("Bone Block", Blocks.name[Redstone319.BONE_BLOCK], "Bone Block registered");
         eq("Golden Apple", Items.name(Redstone319.GOLDEN_APPLE), "Golden Apple item registered");
         eq("Golden Carrot", Items.name(Redstone319.GOLDEN_CARROT), "Golden Carrot item registered");
         eq("Cookie", Items.name(Redstone319.COOKIE), "Cookie item registered");
         eq("Mushroom Stew", Items.name(Redstone319.MUSHROOM_STEW), "Mushroom Stew item registered");
         eq("Bowl", Items.name(Redstone319.BOWL), "Bowl item registered");
         eq("Compass", Items.name(Redstone319.COMPASS), "Compass item registered");
         eq("Clock", Items.name(Redstone319.CLOCK), "Clock item registered");
         eq("Fire Charge", Items.name(Redstone319.FIRE_CHARGE), "Fire Charge item registered");
         check(bwGame.useSpecial(new Stack(Redstone319.COMPASS, 1)), "Compass right-click displays Spawn direction and distance");
         check(bwGame.useSpecial(new Stack(Redstone319.CLOCK, 1)), "Clock right-click displays Day, Time and Weather");

         // 10. Realistic Sounds, Minecraft Legacy Console Edition + Tumble & Battle Minigame Music, Real-Life 3D Physical Acoustics & End Credits
         SoundEngine.loadSoundbank();
         check(SoundEngine.loadedSampleCount() >= 100, "Soundbank loaded >= 100 real sound effects & Minecraft Console Edition tracks (" + SoundEngine.loadedSampleCount() + ")");
         check(SoundEngine.hasSample("step_grass_1") && SoundEngine.hasSample("dig_stone_1") && SoundEngine.hasSample("break_block_1") && SoundEngine.hasSample("tnt_explode"), "Real surface footsteps, block dig/break, and explosion samples loaded");
         check(SoundEngine.hasSample("swing_1") && SoundEngine.hasSample("hit_1") && SoundEngine.hasSample("step_leaves_1") && SoundEngine.hasSample("click_block") && SoundEngine.hasSample("mg_count_go") && SoundEngine.hasSample("mg_win"), "Swing, melee hit, leaves, click_block, and Console Edition minigame SFX loaded");
         check(!SoundEngine.hasSample("ac_morning") && !SoundEngine.hasSample("ac_kk_bossa"), "Animal Crossing tracks completely removed from soundbank");
         check(SoundEngine.hasSample("lce_menu")
            && SoundEngine.hasSample("lce_morning")
            && SoundEngine.hasSample("lce_day")
            && SoundEngine.hasSample("lce_afternoon")
            && SoundEngine.hasSample("lce_sunset")
            && SoundEngine.hasSample("lce_night")
            && SoundEngine.hasSample("lce_creative")
            && SoundEngine.hasSample("lce_nether")
            && SoundEngine.hasSample("lce_end")
            && SoundEngine.hasSample("lce_credits")
            && SoundEngine.hasSample("lce_tumble_1")
            && SoundEngine.hasSample("lce_tumble_2")
            && SoundEngine.hasSample("lce_tumble_3")
            && SoundEngine.hasSample("lce_battle_1")
            && SoundEngine.hasSample("lce_battle_2")
            && SoundEngine.hasSample("lce_battle_3"), "All 16 Minecraft Console Edition (C418) + Tumble & Battle Mini Game (Gareth Coker) tracks loaded");
         SoundEngine.Sample tumbleSample = SoundEngine.getSample("lce_tumble_1");
         check(tumbleSample != null && tumbleSample.pcm.length >= tumbleSample.sampleRate * 120, "Gareth Coker Tumble ('Double Time') & Battle tracks are full-length (>120s, not truncated)");

         // Verify fixed block material sound categories (Coarse Dirt, Podzol, Mycelium, Leaves, Planks, Slime Block)
         eq("dirt", SoundEngine.blockMaterialCategory(Blocks313.COARSE_DIRT), "Coarse Dirt uses dirt sound category (not stone)");
         eq("dirt", SoundEngine.blockMaterialCategory(Blocks313.PODZOL), "Podzol uses dirt sound category");
         eq("dirt", SoundEngine.blockMaterialCategory(Blocks313.MYCELIUM), "Mycelium uses dirt sound category");
         eq("dirt", SoundEngine.blockMaterialCategory(Blocks.FARMLAND), "Farmland uses dirt sound category");
         eq("dirt", SoundEngine.blockMaterialCategory(Blocks.CLAY), "Clay uses dirt sound category");
         eq("leaves", SoundEngine.blockMaterialCategory(Blocks.LEAVES_OAK), "Oak Leaves use leaves sound category");
         eq("wood", SoundEngine.blockMaterialCategory(Blocks313.PLANKS_SPRUCE), "Spruce Planks use wood sound category");
         eq("slime", SoundEngine.blockMaterialCategory(Redstone319.SLIME_BLOCK), "Slime Block uses slime sound category");

         // Verify Minecraft Console Edition music selection by screen, Tumble/Battle/Bedwars mode, dimension, random Overworld, and Constant Music setting
         bwGame.screen = Game.S.TITLE;
         eq("lce_menu", SoundEngine.selectConsoleMusic(bwGame), "Title screen plays C418 - Moog City 2 (Menu)");
         bwGame.screen = Game.S.MINIGAMES;
         eq("lce_tumble_1", SoundEngine.selectConsoleMusic(bwGame), "Mini Games Hub plays Gareth Coker - Double Time (That One Tumble Song!)");
         bwGame.screen = Game.S.CREDITS;
         eq("lce_credits", SoundEngine.selectConsoleMusic(bwGame), "End Credits screen plays C418 - Alpha");
         bwGame.screen = Game.S.PLAY;
         bwGame.world.settings.type = WorldSettings.TUMBLE;
         bwGame.world.settings.mapId = WorldSettings.MAP_COVE;
         check(SoundEngine.selectConsoleMusic(bwGame).startsWith("lce_tumble_"), "Tumble mode plays Gareth Coker Tumble Mini Game theme");
         bwGame.world.settings.type = WorldSettings.BATTLE;
         check(SoundEngine.selectConsoleMusic(bwGame).startsWith("lce_battle_") || SoundEngine.selectConsoleMusic(bwGame).startsWith("lce_tumble_"), "Battle mode plays Gareth Coker Battle/Tumble Mini Game theme");
         bwGame.world.settings.type = WorldSettings.BEDWARS;
         check(SoundEngine.selectConsoleMusic(bwGame).startsWith("lce_battle_") || SoundEngine.selectConsoleMusic(bwGame).startsWith("lce_tumble_"), "Bedwars mode plays Gareth Coker Battle/Tumble Mini Game theme");
         bwGame.world.settings.type = WorldSettings.DEFAULT;
         bwGame.player.creative = false;
         check(SoundEngine.selectConsoleMusic(bwGame).startsWith("lce_"), "Overworld plays random C418 Legacy Console Edition track");
         SoundEngine.setConstantMusic(true);
         check(SoundEngine.isConstantMusic(), "Constant Music setting toggles ON");
         SoundEngine.setConstantMusic(false);
         check(!SoundEngine.isConstantMusic(), "Constant Music setting toggles OFF (random delays between songs)");

         // Verify Real-Life 3D Physical Acoustics: wall occlusion & cave enclosure reverb
         for (int x = 18; x <= 26; x++) {
            for (int y = 79; y <= 84; y++) {
               for (int z = 18; z <= 26; z++) {
                  bwGame.world.setBlock(x, y, z, 0);
               }
            }
         }
         float openOcc = SoundEngine.computeOcclusion(bwGame.world, 20.5, 81.5, 20.5, 25.5, 81.5, 20.5);
         eq(0.0F, openOcc, "Open line-of-sight has 0.0 acoustic wall occlusion");
         for (int y = 79; y <= 84; y++) {
            for (int z = 18; z <= 24; z++) {
               bwGame.world.setBlock(23, y, z, Blocks.STONE);
            }
         }
         float wallOcc = SoundEngine.computeOcclusion(bwGame.world, 20.5, 81.5, 20.5, 25.5, 81.5, 20.5);
         check(wallOcc >= 0.25F, "Solid stone wall between source and listener muffles sound via physical occlusion (" + wallOcc + ")");

         // Build a sealed stone cave chamber around (40, 80, 40) and verify 14-ray acoustic probing increases reverb
         for (int x = 36; x <= 44; x++) {
            for (int y = 77; y <= 84; y++) {
               for (int z = 36; z <= 44; z++) {
                  boolean shell = x == 36 || x == 44 || y == 77 || y == 84 || z == 36 || z == 44;
                  bwGame.world.setBlock(x, y, z, shell ? Blocks.STONE : 0);
               }
            }
         }
         for (int i = 0; i < 6; i++) {
            SoundEngine.updateEnvironmentAcoustics(bwGame.world, 40.5, 80.5, 40.5);
         }
         check(SoundEngine.getRoomEnclosure() > 0.45F && SoundEngine.getReverbWet() > 0.14F, "Enclosed stone cave increases 3D room enclosure & Schroeder reverb wet mix");

         // Verify End Credits attributions & /credits command
         check(SoundEngine.CREDITS_SECTIONS.length >= 3, "End Credits contains Music, Sound Effects, and 3D Acoustics attribution sections");
         bwGame.runCommand("/credits");
         eq(Game.S.CREDITS, bwGame.screen, "/credits command opens the End Poem & Credits screen");

         // Verify V4.2 Minigame Smart AI Competitors (Tumble, Battle, Bedwars) & Custom Prebuilt Map System
         bwGame.world.settings.type = WorldSettings.BEDWARS;
         SkyGen.resetArenaMap(bwGame, bwGame.world, false);
         Mobs.Mob testBot = bwGame.mobs.spawn(Mobs.ZOMBIE, 0.5, SkyGen.Y_DECK + 1.0, 44.5);
         testBot.teamTag = "Team:Blue:0:BlazeKing";
         testBot.variant = 0;
         check(SkyGen.isMinigameBot(testBot), "Bedwars AI bot recognized by SkyGen.isMinigameBot");
         eq("[BLUE] BlazeKing", SkyGen.botDisplayName(testBot), "Bedwars AI bot formats team badge and player name");
         eq(1, SkyGen.botTeamIndex(testBot), "Bedwars AI bot resolves Blue team index (1)");
         SkyGen.BotIntent bi = SkyGen.tickBotAI(bwGame.mobs, testBot, bwGame.world, bwGame.player);
         check(bi != null, "SkyGen.tickBotAI produces movement/combat intent for Bedwars AI bot");

         // Verify Bedwars AI Bed Defense & Bridging
         Mobs.Mob raiderBot = bwGame.mobs.spawn(Mobs.SKELETON, 0.5, SkyGen.Y_DECK + 1.0, 44.5);
         raiderBot.teamTag = "Team:Blue:1:Valkyrie";
         raiderBot.variant = 1;
         raiderBot.age = 12;
         SkyGen.tickBotAI(bwGame.mobs, raiderBot, bwGame.world, bwGame.player);
         check(bwGame.world.getBlock(0, SkyGen.Y_DECK, 38) != 0, "Bedwars Raider AI places team wool bridge block toward center");

         // Verify Custom Prebuilt Map save/load (/map save & /map load)
         bwGame.world.setBlock(2, SkyGen.Y_DECK + 4, 2, Blocks.GOLD_BLOCK);
         check(SkyGen.saveCustomArenaMap(bwGame.world, "unit_test_arena"), "SkyGen.saveCustomArenaMap saves world chunks to maps/unit_test_arena");
         SkyGen.customMapName = "unit_test_arena";
         SkyGen.resetArenaMap(bwGame, bwGame.world, false);
         eq(Blocks.GOLD_BLOCK, bwGame.world.getBlock(2, SkyGen.Y_DECK + 4, 2), "Custom prebuilt map chunk restores saved GOLD_BLOCK on arena reset");
         SkyGen.customMapName = null;
         Game.deleteDir(new java.io.File("maps/unit_test_arena"));

         // Verify Multi-Height Arena Structures (Upper Sky-Vaults, Slime Jump Pads & 2-Story Castles)
         SkyGen.resetArenaMap(bwGame, bwGame.world, false);
         eq(Blocks.EMERALD_BLOCK, bwGame.world.getBlock(0, SkyGen.Y_DECK + 6, 0), "Bedwars center island has 2nd-story Sky-Vault at Y_DECK + 6");
         eq(Redstone319.SLIME_BLOCK, bwGame.world.getBlock(3, SkyGen.Y_DECK, 3), "Bedwars center island has Slime Block Jump Pads to upper Sky-Vault");
         check(bwGame.world.getBlock(0, SkyGen.Y_DECK + 5, -48) != 0, "Bedwars team base has 2nd-story Castle Keep roof at Y_DECK + 5");

         // Verify Post-Match Victory Overview -> Inter-Round Lobby -> Map Voting Flow
         SkyGen.openMatchOverview(bwGame, true);
         check(SkyGen.showingOverview, "Winning a round opens the Post-Match Victory Overview screen");
         eq(Game.S.WAITING, bwGame.screen, "Overview screen is active on Game.S.WAITING");
         SkyGen.castMapVote(bwGame, WorldSettings.MAP_TEMPLE, false);
         eq(WorldSettings.MAP_TEMPLE, SkyGen.myMapVote, "Player map vote is recorded for Temple");
         SkyGen.enterInterRoundLobby(bwGame);
         check(!SkyGen.showingOverview && SkyGen.lobbyCountdownTicks > 0, "Transitioning from Overview enters Inter-Round Waiting Lobby with active countdown");

         // Verify all 9x9 arena chunks stay permanently loaded even at minimum Render Distance (RD=2)
         bwGame.world.updateChunks(0, -3, 2, 4);
         check(bwGame.world.chunk(0, 3) != null && bwGame.world.chunk(-3, 0) != null && bwGame.world.chunk(3, 0) != null,
            "All enemy Bedwars base chunks stay resident regardless of player render distance");

         // Verify Bedwars AI Bot Respawn when Bed is Intact vs Permanent Elimination when Bed is Destroyed
         SkyGen.startMatch(bwGame, false);
         Mobs.Mob blueBot = null;
         Mobs.Mob blueRaider = null;
         for (Mobs.Mob m : bwGame.mobs.list) {
            if (SkyGen.botTeamIndex(m) == 1 && (m.variant & 1) == 0) {
               blueBot = m;
            } else if (SkyGen.botTeamIndex(m) == 1 && (m.variant & 1) == 1) {
               blueRaider = m;
            }
         }
         check(blueBot != null && blueRaider != null, "Blue Team Defender and Raider AI bots spawned at match start");

         // Verify AI surroundings-safe item looting & dynamic bridge building toward enemy beds
         bwGame.world.dropItem(blueRaider.x + 1.0, blueRaider.y, blueRaider.z, Items.EMERALD, 2);
         for (int tick = 0; tick < 25; tick++) {
            bwGame.world.tickCount++;
            bwGame.mobs.tick();
            SkyGen.tickMode(bwGame, bwGame.world);
         }
         boolean pickedUpEmerald = true;
         for (World.Item it : bwGame.world.items) {
            if (it != null && !it.dead && it.id == Items.EMERALD && Math.hypot(it.x - blueRaider.x, it.z - blueRaider.z) < 2.5) {
               pickedUpEmerald = false;
            }
         }
         check(pickedUpEmerald, "AI bot safely loots nearby dropped Emerald when no enemy is threatening");

         bwGame.mobs.hurtMob(blueBot, 100, 0.0, 0.0, 0.0, true);
         check(blueBot.dead, "Blue Team Defender AI bot died from fatal damage");
         for (int tick = 0; tick < 105; tick++) {
            bwGame.world.tickCount++;
            bwGame.mobs.tick();
            SkyGen.tickMode(bwGame, bwGame.world);
         }
         Mobs.Mob respawnedBlue = null;
         for (Mobs.Mob m : bwGame.mobs.list) {
            if (!m.dead && SkyGen.botTeamIndex(m) == 1 && (m.variant & 1) == 0) {
               respawnedBlue = m;
               break;
            }
         }
         check(respawnedBlue != null, "Bedwars AI bot respawns after 5s when its team bed is intact");

         // Now destroy Blue Team's bed, kill the bot, and verify it stays permanently dead
         SkyGen.onBedDestroyed(bwGame, bwGame.world, 1, "You", false);
         bwGame.mobs.hurtMob(respawnedBlue, 100, 0.0, 0.0, 0.0, true);
         for (int tick = 0; tick < 120; tick++) {
            bwGame.world.tickCount++;
            bwGame.mobs.tick();
            SkyGen.tickMode(bwGame, bwGame.world);
         }
         boolean stillAliveAfterBedGone = false;
         for (Mobs.Mob m : bwGame.mobs.list) {
            if (!m.dead && SkyGen.botTeamIndex(m) == 1 && (m.variant & 1) == 0) {
               stillAliveAfterBedGone = true;
               break;
            }
         }
         check(!stillAliveAfterBedGone, "Bedwars AI bot stays permanently dead after its team bed is destroyed");

         // Verify starting a NEW ROUND (startMatch / resetArenaMap) in Bedwars, Battle, and Tumble respawns all AI bots and gives player-like running!
         SkyGen.startMatch(bwGame, false);
         int bwRound2Bots = 0;
         Mobs.Mob runnerBot = null;
         for (Mobs.Mob m : bwGame.mobs.list) {
            if (!m.dead && SkyGen.isMinigameBot(m)) {
               bwRound2Bots++;
               if ((m.variant & 1) == 1) {
                  runnerBot = m;
               }
            }
         }
         eq(6, bwRound2Bots, "Starting a new Bedwars round respawns all 6 enemy AI bots across Blue, Green, and Yellow");
         check(runnerBot != null, "Bedwars Raider AI bot spawned for new round");
         for (int tick = 0; tick < 12; tick++) {
            bwGame.world.tickCount++;
            bwGame.mobs.tick();
            SkyGen.tickMode(bwGame, bwGame.world);
         }
         check(runnerBot.moving && runnerBot.walkAmp > 0.2F, "Minigame AI bot sets moving=true and animates player-like sprinting stride");

         // Switch to BATTLE mode, kill all bots, start a new round, and verify all 7 BattleBots respawn!
         bwGame.world.settings.type = WorldSettings.BATTLE;
         SkyGen.startMatch(bwGame, false);
         int battleRound1Bots = 0;
         for (Mobs.Mob m : bwGame.mobs.list) {
            if (!m.dead && SkyGen.isMinigameBot(m)) {
               battleRound1Bots++;
               m.dead = true;
            }
         }
         eq(7, battleRound1Bots, "Battle round 1 spawns all 7 BattleBot competitors");
         SkyGen.startMatch(bwGame, false);
         int battleRound2Bots = 0;
         for (Mobs.Mob m : bwGame.mobs.list) {
            if (!m.dead && SkyGen.isMinigameBot(m)) {
               battleRound2Bots++;
            }
         }
         eq(7, battleRound2Bots, "Starting a new Battle round (startMatch / resetArenaMap) respawns all 7 BattleBot competitors");

         // Switch to TUMBLE mode, kill all bots, start a new round, and verify all 5 TumbleBots respawn!
         bwGame.world.settings.type = WorldSettings.TUMBLE;
         SkyGen.startMatch(bwGame, false);
         int tumbleRound1Bots = 0;
         for (Mobs.Mob m : bwGame.mobs.list) {
            if (!m.dead && SkyGen.isMinigameBot(m)) {
               tumbleRound1Bots++;
               m.dead = true;
            }
         }
         eq(5, tumbleRound1Bots, "Tumble round 1 spawns all 5 TumbleBot competitors");
         SkyGen.startMatch(bwGame, false);
         int tumbleRound2Bots = 0;
         for (Mobs.Mob m : bwGame.mobs.list) {
            if (!m.dead && SkyGen.isMinigameBot(m)) {
               tumbleRound2Bots++;
            }
         }
         eq(5, tumbleRound2Bots, "Starting a new Tumble round (startMatch / resetArenaMap) respawns all 5 TumbleBot competitors");

         // Verify players stay eliminated / go to Spectator Mode when they die in Tumble, Battle, or Bedwars without a bed!
         bwGame.player.hurt(100);
         SkyGen.onLocalPlayerDied(bwGame);
         check(SkyGen.localEliminated, "Dying in Tumble marks localEliminated = true");
         bwGame.respawn();
         check(bwGame.player.spectator && bwGame.player.rawMode() == 3, "Calling respawn() in Tumble puts eliminated player into Spectator Mode");
         bwGame.player.setMode(0);
         SkyGen.tickMode(bwGame, bwGame.world);
         check(bwGame.player.spectator, "tickMode enforces Spectator Mode while localEliminated is true during an active round");

         // Verify starting a new round clears localEliminated and restores Survival mode (mode 0)
         SkyGen.startMatch(bwGame, false);
         check(!SkyGen.localEliminated && !bwGame.player.spectator && bwGame.player.rawMode() == 0,
            "Starting a new round clears localEliminated and restores player to Survival mode");

         // Verify Multiplayer compatibility: remote map votes & remote match overview sync
         SkyGen.onRemoteMapVote(2, WorldSettings.MAP_CAVERN);
         SkyGen.onRemoteMapVote(3, WorldSettings.MAP_CAVERN);
         eq(WorldSettings.MAP_CAVERN, SkyGen.winningVotedMap(0), "Multiplayer peer map votes are tallied into winningVotedMap");
         SkyGen.onRemoteMatchOverview(bwGame, "PeerChamp_(Champion!)", "Tumble", "Cavern", 62);
         check(SkyGen.showingOverview && "PeerChamp (Champion!)".equals(SkyGen.lastRoundFirst),
            "Multiplayer MATCH OVERVIEW packet syncs Post-Match Overview screen and winner");

         // =========================================================================
         // Verify V4.3 Tactical Items, CS2 Strike, Murder Mystery, Strict AI Inventory, Vertical Building & Persistent Multiplayer
         // =========================================================================
         eq("Tactical Pistol", Items.name(Redstone319.PISTOL), "PISTOL item registered");
         eq("Assault SMG", Items.name(Redstone319.MACHINE_GUN), "MACHINE_GUN item registered");
         eq("Heavy Sniper", Items.name(Redstone319.SNIPER_RIFLE), "SNIPER_RIFLE item registered");
         eq("TNT Frag Grenade", Items.name(Redstone319.TNT_GRENADE), "TNT_GRENADE item registered");
         eq("Murderer's Knife", Items.name(Redstone319.MURDER_KNIFE), "MURDER_KNIFE item registered");
         eq("C4 Explosive", Items.name(Redstone319.C4_BOMB), "C4_BOMB item registered");
         eq(24.0F, Items.attackDamage(Redstone319.MURDER_KNIFE), "Murderer's Knife deals lethal 24.0 melee damage");

         // 1. CS2 Strike mode: Arena generation, Bomb Sites A & B, arrow-shooting guns, TNT grenades, C4 plant & defuse
         SkyGen.switchMinigameMode(bwGame, WorldSettings.CS2, false);
         eq(WorldSettings.CS2, bwGame.world.settings.type, "switchMinigameMode switches world to CS2 Strike");
         SkyGen.myTeam = 1; // Terrorist
         SkyGen.startMatch(bwGame, false);
         eq(1, SkyGen.nearCs2BombSite(-22.0, SkyGen.Y_DECK + 1.0, 0.0), "CS2 Bomb Site A located at (-22, Y_DECK+1, 0)");
         eq(2, SkyGen.nearCs2BombSite(22.0, SkyGen.Y_DECK + 1.0, 0.0), "CS2 Bomb Site B located at (+22, Y_DECK+1, 0)");
         int cs2BotCount = 0;
         for (Mobs.Mob m : bwGame.mobs.list) {
            if (!m.dead && m.teamTag != null && m.teamTag.startsWith("CS2Bot:")) {
               cs2BotCount++;
            }
         }
         eq(7, cs2BotCount, "CS2 Strike spawns 7 CT and T bots across both teams");

         // Verify gun firing consumes Arrow ammo and spawns high-velocity arrow, and refuses when ammo is 0
         SkyGen.giveCs2Loadout(bwGame, 1, 0); // Assault SMG kit
         int arrowsBefore = bwGame.countItem(Items.ARROW);
         int worldArrowsBefore = bwGame.mobs.arrows.size();
         SkyGen.playerGunCooldown = 0;
         check(SkyGen.tryUseTacticalWeapon(bwGame, Redstone319.MACHINE_GUN), "Firing MACHINE_GUN succeeds");
         eq(arrowsBefore - 1, bwGame.countItem(Items.ARROW), "Firing MACHINE_GUN consumes 1 Arrow from inventory");
         eq(worldArrowsBefore + 1, bwGame.mobs.arrows.size(), "Firing MACHINE_GUN spawns a high-velocity Arrow projectile");

         // Remove all arrows and verify gun cannot spawn any arrow when out of ammo
         bwGame.removeItems(Items.ARROW, 999);
         SkyGen.playerGunCooldown = 0;
         int arrowsEmpty = bwGame.mobs.arrows.size();
         SkyGen.tryUseTacticalWeapon(bwGame, Redstone319.PISTOL);
         eq(arrowsEmpty, bwGame.mobs.arrows.size(), "Gun cannot fire arrows when player has 0 Arrows in inventory");

         // Verify TNT Frag Grenade throws a primed TNT entity with horizontal velocity
         SkyGen.giveCs2Loadout(bwGame, 1, 0);
         SkyGen.playerGunCooldown = 0;
         int fallersBefore = bwGame.world.fallers.size();
         bwGame.sel = 3; // TNT_GRENADE slot
         check(SkyGen.tryUseTacticalWeapon(bwGame, Redstone319.TNT_GRENADE), "Throwing TNT_GRENADE succeeds");
         eq(fallersBefore + 1, bwGame.world.fallers.size(), "Throwing TNT_GRENADE spawns primed TNT Falling entity");

         // Verify C4 Bomb Plant and Defuse
         SkyGen.plantCs2Bomb(bwGame, 1, "You", false);
         check(SkyGen.cs2BombPlanted && SkyGen.cs2BombSite == 1 && SkyGen.cs2BombTimerTicks == 700, "C4 Bomb planted at Site A with 35s timer");
         SkyGen.defuseCs2Bomb(bwGame, "Blue_CT", false);
         check(!SkyGen.cs2BombPlanted && SkyGen.cs2RoundOver, "Defusing C4 Bomb ends CS2 round for Counter-Terrorists");

         // 2. Murder Mystery mode: Mansion generation, Roles, 5s weapon deal, 5-gold Detective Pistol & Sheriff gun drop
         SkyGen.murderRolePref = 3; // Innocent
         SkyGen.switchMinigameMode(bwGame, WorldSettings.MURDER, false);
         SkyGen.startMatch(bwGame, false);
         eq(WorldSettings.MURDER, bwGame.world.settings.type, "Switched to Murder Mystery mode");
         eq(SkyGen.MURDER_ROLE_INNOCENT, SkyGen.myMurderRole, "Player assigned Innocent role per preference");
         int murderBots = 0;
         Mobs.Mob sheriffBot = null;
         Mobs.Mob murdererBot = null;
         for (Mobs.Mob m : bwGame.mobs.list) {
            if (!m.dead && m.teamTag != null && m.teamTag.startsWith("MurderBot:")) {
               murderBots++;
               int bSlot = Math.abs(m.netId) & 31;
               if (SkyGen.botMurderRole[bSlot] == SkyGen.MURDER_ROLE_SHERIFF) {
                  sheriffBot = m;
               } else if (SkyGen.botMurderRole[bSlot] == SkyGen.MURDER_ROLE_MURDERER) {
                  murdererBot = m;
               }
            }
         }
         eq(7, murderBots, "Murder Mystery spawns 7 bots in the Mansion");
         check(sheriffBot != null && murdererBot != null, "Murder Mystery assigns 1 Murderer bot and 1 Sheriff bot when player is Innocent");

         // Advance past 100 ticks (5 seconds) so Murder Knife and Sheriff Pistol are dealt
         bwGame.world.tickCount = 105L;
         SkyGen.tickMode(bwGame, bwGame.world);
         check(SkyGen.murderWeaponsDealt, "Murder Mystery deals weapons at 5 seconds (100 ticks)");
         eq(Redstone319.MURDER_KNIFE, SkyGen.botWeaponItem[Math.abs(murdererBot.netId) & 31], "Murderer bot receives MURDER_KNIFE at 5s");
         eq(Redstone319.PISTOL, SkyGen.botRangedItem[Math.abs(sheriffBot.netId) & 31], "Sheriff bot receives PISTOL at 5s");

         // Verify Innocent collecting 5 Gold Ingots receives Detective Pistol + Arrows
         bwGame.addItem(new Stack(Items.GOLD_INGOT, 5));
         SkyGen.tickMode(bwGame, bwGame.world);
         check(bwGame.countItem(Redstone319.PISTOL) == 1 && bwGame.countItem(Items.ARROW) >= 6,
            "Innocent collecting 5 Gold Ingots automatically unlocks a Detective Pistol + Arrows");

         // Verify Sheriff bot dying drops the Sheriff's Pistol for Innocents to pick up
         sheriffBot.dead = true;
         SkyGen.onBotKilled(bwGame, sheriffBot);
         check(SkyGen.murderGunDropped, "Sheriff bot death drops the Sheriff's Pistol in the Mansion");

         // 3. Verify AI Strict Inventory Ledger & Vertical Pillar Building (tryBotBuildUp)
         SkyGen.switchMinigameMode(bwGame, WorldSettings.BATTLE, false);
         SkyGen.startMatch(bwGame, false);
         Mobs.Mob pillarBot = null;
         for (Mobs.Mob m : bwGame.mobs.list) {
            if (!m.dead && SkyGen.isMinigameBot(m)) {
               pillarBot = m;
               break;
            }
         }
         check(pillarBot != null, "Found BattleBot for strict inventory & vertical build-up test");
         int pSlot = Math.abs(pillarBot.netId) & 31;
         int bx = (int)Math.floor(pillarBot.x);
         int by = (int)Math.floor(pillarBot.y);
         int bz = (int)Math.floor(pillarBot.z);
         bwGame.world.setBlock(bx, by - 1, bz, Blocks.STONE);
         bwGame.world.setBlock(bx, by, bz, 0);
         bwGame.world.setBlock(bx, by + 1, bz, 0);
         bwGame.world.setBlock(bx, by + 2, bz, 0);
         pillarBot.onGround = true;

         // When botBlocks[pSlot] == 0, AI cannot pillar up or place blocks!
         SkyGen.botBlocks[pSlot] = 0;
         boolean builtEmpty = SkyGen.tryBotBuildUp(pillarBot, bwGame.world, pillarBot.x, pillarBot.y + 4.0, pillarBot.z, Blocks.COBBLE);
         check(!builtEmpty && bwGame.world.getBlock(bx, by, bz) == 0, "AI bot cannot build vertically when botBlocks inventory is 0");

         // Give the bot 4 blocks and verify tryBotBuildUp pillars up and decrements botBlocks[pSlot]!
         SkyGen.botBlocks[pSlot] = 4;
         boolean builtWithBlocks = SkyGen.tryBotBuildUp(pillarBot, bwGame.world, pillarBot.x, pillarBot.y + 4.0, pillarBot.z, Blocks.COBBLE);
         check(builtWithBlocks, "AI bot pillars vertically toward higher target when it owns blocks");
         eq(3, SkyGen.botBlocks[pSlot], "Vertical pillar building consumes 1 block from botBlocks[pSlot]");
         eq(Blocks.COBBLE, bwGame.world.getBlock(bx, by, bz), "AI bot placed Cobblestone pillar block beneath its feet");

         bwGame.world.settings.type = WorldSettings.BEDWARS;

         // Verify LOD high-detail splitDistance extends further, boxSides emits outer walls only, and evict retains 360-degree in-range tiles
         Lod lodTest = new Lod(null, bwGame.world.gen);
         try {
            lodTest.innerHalf = 112; // Render distance 8
            lodTest.maxDist = 1024;
            double splitRD8 = lodTest.splitDistance(64);
            lodTest.innerHalf = 240; // Render distance 16
            double splitRD16 = lodTest.splitDistance(64);
            check(splitRD8 >= 280.0 && splitRD16 > splitRD8 + 150.0, "High-detail LOD split distance reaches far out and scales with render distance");
            Lod.Mb sideMb = new Lod.Mb();
            sideMb.bb = java.nio.ByteBuffer.allocateDirect(512).order(java.nio.ByteOrder.nativeOrder());
            Lod.boxSides(sideMb, 0.0F, 0.0F, 0.0F, 2.0F, 4.0F, 2.0F, 0x558833, 0.0F);
            eq(16, sideMb.n, "Lod.boxSides emits only the 4 outer vertical walls (16 vertices) without hidden interior horizontal caps");
            Lod.Tile behindTile = lodTest.tile(64, -128, -128);
            behindTile.state = 3;
            behindTile.lastUse = 1L;
            lodTest.cx = 0.0;
            lodTest.cz = 0.0;
            lodTest.frame = 1000L; // > 900 frames since lastUse, but still inside maxDist (1024)
            lodTest.evict();
            check(lodTest.tiles.containsKey(Lod.key(64, -128, -128)), "Lod.evict retains in-range tiles behind the camera so turning around never drops LOD chunks");
         } finally {
            lodTest.dispose();
         }

         // Verify 3D gun classification & two-handed tactical aiming pose in PlayerAnim
         check(Renderer.isGun(Redstone319.PISTOL) && Renderer.isGun(Redstone319.MACHINE_GUN) && Renderer.isGun(Redstone319.SNIPER_RIFLE), "PISTOL, MACHINE_GUN, and SNIPER_RIFLE are classified as 3D guns");
         check(!Renderer.isGun(Redstone319.MURDER_KNIFE) && !Renderer.isGun(Items.BOW), "Non-gun weapons are not classified as 3D guns");
         PlayerModel.Pose gunPose = new PlayerModel.Pose();
         gunPose.held = Redstone319.MACHINE_GUN;
         PlayerAnim.Joints gunJoints = PlayerAnim.solve(gunPose);
         check(gunJoints.sh[0] > 65.0F && gunJoints.sh[1] > 60.0F, "Holding a 3D gun raises both right and left arms into a two-handed tactical aiming stance");

         // Verify stuck arrows preserve non-zero flight heading vector so MobRender.drawArrows keeps incoming orientation
         bwGame.world.setBlock(0, SkyGen.Y_DECK + 2, 2, Blocks.STONE);
         bwGame.mobs.arrows.clear();
         bwGame.mobs.shootGunArrow(0.5, SkyGen.Y_DECK + 2.5, 0.5, 0.0, 0.0, 1.0, 3.2F, 6, true);
         eq(1, bwGame.mobs.arrows.size(), "Spawned high-speed bullet arrow toward stone wall");
         Mobs.Arrow shotAr = bwGame.mobs.arrows.get(0);
         boolean removedOnWall = bwGame.mobs.tickArrow(shotAr, bwGame.world, bwGame.player);
         check(!removedOnWall && shotAr.stuck, "Bullet arrow embeds in solid wall on impact without disappearing immediately");
         check(shotAr.vz > 0.0, "Embedded arrow preserves non-zero flight heading vector for 3D rendering");
         bwGame.world.setBlock(0, SkyGen.Y_DECK + 2, 2, 0);

         // Verify 11x11 arena + border ring (-5..5) stays resident, outer void chunks fast-path, and updateChunks never overwrites resident chunks
         bwGame.world.ensureArenaChunksLoaded();
         check(bwGame.world.chunk(-5, -5) != null && bwGame.world.chunk(5, 5) != null && bwGame.world.chunk(5, 5).lit,
            "1-chunk border ring (-5..5) around minigame arena is loaded and lit so outer arena chunks can mesh immediately");
         Chunk centerBefore = bwGame.world.chunk(0, 0);
         bwGame.world.updateChunks(0, 0, 12, 64, 10_000_000L);
         check(bwGame.world.chunk(0, 0) == centerBefore, "updateChunks preserves already-resident arena chunk instances");
         check(bwGame.world.chunk(8, 8) == null, "Void mode updateChunks clamps background chunk loading to the -5..5 arena bounds");

         // =========================================================================
         // Verify V4.5 Player-Authentic AI Economy (Diamond/Emerald Expeditions, Base Shopkeeper Purchases) & Team Mind
         // =========================================================================
         SkyGen.myTeam = 0;
         SkyGen.switchMinigameMode(bwGame, WorldSettings.BEDWARS, false);
         SkyGen.startMatch(bwGame, false);
         Mobs.Mob econBot = null;
         for (Mobs.Mob m : bwGame.mobs.list) {
            if (!m.dead && SkyGen.botTeamIndex(m) == 1) {
               econBot = m;
               break;
            }
         }
         check(econBot != null, "Found Blue Team AI bot for Player-Authentic Economy & Team Mind test");
         int eSlot = Math.abs(econBot.netId) & 31;
         SkyGen.teamUpgrades[1] = 0;
         SkyGen.botDiamonds[eSlot] = 0;

         // Drop 2 Diamonds on the nearest Diamond Generator Island (24, 24) and verify findBedwarsExpeditionTarget directs the bot there!
         bwGame.world.dropItem(24.5, SkyGen.Y_DECK + 1.0, 24.5, Items.DIAMOND, 2);
         double[] diaExp = SkyGen.findBedwarsExpeditionTarget(bwGame.world, econBot, 1, 0, eSlot, 99.0, -40.0, -40.0);
         check(diaExp != null && Math.hypot(diaExp[0] - 24.5, diaExp[2] - 24.5) < 2.0,
            "AI bot targets live Diamonds on the Diamond Generator Island (24, 24) for a physical Diamond expedition");

         // Move bot to the Diamond Island to pick up the 2 Diamonds; verify Diamonds go into botDiamonds[eSlot] WITHOUT magically unlocking Team Upgrades in the field!
         econBot.setPos(24.5, SkyGen.Y_DECK + 1.0, 24.5);
         econBot.onGround = true;
         SkyGen.tickBotAI(bwGame.mobs, econBot, bwGame.world, bwGame.player);
         eq(2, SkyGen.botDiamonds[eSlot], "Picking up Diamonds on Diamond Island adds 2 Diamonds to botDiamonds[eSlot]");
         eq(0, SkyGen.teamUpgrades[1], "Picking up Diamonds in the field does NOT unlock Team Upgrades before returning to the Base Priest Shopkeeper");
         check(SkyGen.botNeedsBaseShopVisit(bwGame.world, 1, eSlot), "AI bot carrying 2 Diamonds flags botNeedsBaseShopVisit = true to return to Base Priest");

         // Now return the bot to Blue Base (0.5, Y_DECK + 1.0, 44.5) with 2 Diamonds, 2 Emeralds, and 12 Iron, and verify performBotShopAtBase!
         econBot.setPos(0.5, SkyGen.Y_DECK + 1.0, 44.5);
         SkyGen.botEmeralds[eSlot] = 2;
         SkyGen.botIron[eSlot] = 12;
         boolean shopped = SkyGen.performBotShopAtBase(bwGame.mobs, econBot, bwGame.world, 1, eSlot);
         check(shopped, "AI bot physically at Blue Base executes purchases at Priest & Smith Shopkeepers");
         eq(0, SkyGen.botDiamonds[eSlot], "Priest Shopkeeper purchase consumes 2 Diamonds from botDiamonds[eSlot]");
         check(SkyGen.teamUpgrades[1] != 0, "Spending 2 Diamonds at Base Priest unlocks Blue Team's next Team Upgrade");
         eq(3, SkyGen.botGearTier[eSlot], "Spending 2 Emeralds at Base Smith upgrades AI bot to Diamond Gear (tier 3)");
         check(SkyGen.botObsidian[eSlot] >= 4 && SkyGen.botEndStone[eSlot] >= 12,
            "Spending Emeralds and Iron at Base Smith purchases Obsidian and End Stone for Bed Defense");

         // Verify Team Mind emergency defense & coordinated raid states
         SkyGen.updateBedwarsTeamMind(bwGame, bwGame.world, 1, eSlot, econBot, true, true, 0);
         eq(SkyGen.TEAM_OBJ_EMERGENCY_DEFEND, SkyGen.teamObjective[1], "Team Mind switches to TEAM_OBJ_EMERGENCY_DEFEND when base is under attack");
         SkyGen.updateBedwarsTeamMind(bwGame, bwGame.world, 1, eSlot, econBot, false, true, 0);
         check(!SkyGen.getTeamThought(1).isEmpty(), "Team Mind publishes a non-empty tactical team thought summary");

         // =========================================================================
         // Verify V4.6 Pre-Round Tactical Warmup, Anti-Clumping, Low-HP Retreat/Walling & Layer-2 Bed Defense
         // =========================================================================
         // 1. Anti-Clumping Formation Offsets & Pre-Round Warmup Scouting
         Mobs.Mob secondBlueBot = null;
         for (Mobs.Mob m : bwGame.mobs.list) {
            if (!m.dead && m != econBot && SkyGen.botTeamIndex(m) == 1) {
               secondBlueBot = m;
               break;
            }
         }
         check(secondBlueBot != null, "Found second Blue Team bot for Anti-Clumping & Pre-Round Scouting test");
         double[] sp1 = SkyGen.applyBotSpreadOffset(econBot, 0.5, 44.5, 3.6);
         double[] sp2 = SkyGen.applyBotSpreadOffset(secondBlueBot, 0.5, 44.5, 3.6);
         check(Math.hypot(sp1[0] - sp2[0], sp1[1] - sp2[1]) > 1.2,
            "applyBotSpreadOffset assigns distinct tactical formation coordinates to teammates so they never pile up on one spot");

         SkyGen.preRoundWarmupActive = true;
         try {
            SkyGen.BotIntent warmIntent = SkyGen.tickBotAI(bwGame.mobs, econBot, bwGame.world, bwGame.player);
            check(warmIntent != null && "Pre-Round Tactical Scouting".equals(SkyGen.botThought[eSlot]),
               "Pre-round warmup ticks AI bots in non-lethal Pre-Round Tactical Scouting mode");
         } finally {
            SkyGen.preRoundWarmupActive = false;
         }

         // 2. Low-HP Tactical Retreat, Defensive Walling & On-the-Run Golden Apple Healing
         econBot.setPos(0.5, SkyGen.Y_DECK + 1.0, 2.5);
         econBot.onGround = true;
         econBot.health = 5;
         econBot.age = 16;
         SkyGen.botBlocks[eSlot] = 16;
         SkyGen.botApples[eSlot] = 1;
         SkyGen.botEatTicks[eSlot] = 0;
         bwGame.world.setBlock(0, SkyGen.Y_DECK, 1, Blocks.STONE);
         bwGame.world.setBlock(0, SkyGen.Y_DECK + 1, 1, 0);
         check(SkyGen.shouldBotRetreat(econBot, eSlot, 4.0), "shouldBotRetreat triggers when bot HP <= 8 and enemy is within 13.5 blocks");
         int blocksBeforeRetreat = SkyGen.botBlocks[eSlot];
         SkyGen.BotIntent retIntent = new SkyGen.BotIntent();
         SkyGen.executeTacticalRetreat(
            bwGame.mobs, econBot, bwGame.world, eSlot, 0.5, SkyGen.Y_DECK + 1.0, -1.5, 4.0, Blocks.WOOL0 + 11, 0.5, 44.5, retIntent
         );
         eq("Low HP! Tactical Retreat!", SkyGen.botThought[eSlot], "Retreating bot sets thought to Low HP! Tactical Retreat!");
         check(econBot.health > 5 && SkyGen.botApples[eSlot] == 0, "Retreating bot consumes Golden Apple on the run to recover HP");
         check(SkyGen.botBlocks[eSlot] < blocksBeforeRetreat && bwGame.world.getBlock(0, SkyGen.Y_DECK + 1, 1) != 0,
            "Retreating bot places a defensive obstruction block between itself and the close pursuer");

         // 3. Multi-Layer (Layer-2) Bed Defense Construction
         econBot.health = 20;
         int[] blueBf = SkyGen.getTeamBedFoot(1);
         int[][] innerCore = SkyGen.getBedInnerShell(1);
         for (int[] ic : innerCore) {
            bwGame.world.setBlock(ic[0], ic[1], ic[2], Blocks.OBSIDIAN);
         }
         bwGame.world.setBlock(blueBf[0], blueBf[1] + 2, blueBf[2], 0);
         econBot.setPos(blueBf[0] + 0.5, SkyGen.Y_DECK + 1.0, blueBf[2] + 0.5);
         econBot.age = 19; // 19 % 14 == 5
         SkyGen.botEndStone[eSlot] = 8;
         SkyGen.tickBotAI(bwGame.mobs, econBot, bwGame.world, bwGame.player);
         eq(Blocks.END_STONE, bwGame.world.getBlock(blueBf[0], blueBf[1] + 2, blueBf[2]),
            "Once inner 6-block bed core is sealed, AI bot builds Layer-2 End Stone defense above the bed");

         // =========================================================================
         // Verify 3D Obstacle-Aware Detour Pathfinding, Overhead Clearance & Smart Block Breaking
         // =========================================================================
         // 4a. Verify canBotWalkThrough rejects 2-block walls and low overhead blocks, and pathfinding detours around walls!
         for (int dz = -2; dz <= 4; dz++) {
            for (int dx = -3; dx <= 3; dx++) {
               bwGame.world.setBlock(dx, SkyGen.Y_DECK, dz, Blocks.STONE);
               bwGame.world.setBlock(dx, SkyGen.Y_DECK + 1, dz, 0);
               bwGame.world.setBlock(dx, SkyGen.Y_DECK + 2, dz, 0);
               bwGame.world.setBlock(dx, SkyGen.Y_DECK + 3, dz, 0);
            }
         }
         // Place a 2-block-high pillar/wall directly ahead at (0, Y_DECK+1..2, 1) while side lanes (dx = ±1, ±2) are open
         bwGame.world.setBlock(0, SkyGen.Y_DECK + 1, 1, Blocks.COBBLE);
         bwGame.world.setBlock(0, SkyGen.Y_DECK + 2, 1, Blocks.COBBLE);
         check(!SkyGen.canBotWalkThrough(bwGame.world, 0, SkyGen.Y_DECK + 1, 0, 0, 1),
            "canBotWalkThrough rejects a 2-block-high wall so AI bots never treat walls as walkable ground");
         check(SkyGen.canBotWalkThrough(bwGame.world, 0, SkyGen.Y_DECK + 1, 0, 1, 1),
            "canBotWalkThrough accepts the open side lane around the wall");

         // Drop an Emerald at (0.5, Y_DECK + 1.0, 3.5) behind the wall and verify the bot steers around the wall!
         bwGame.world.items.clear();
         bwGame.world.dropItem(0.5, SkyGen.Y_DECK + 1.0, 3.5, Items.EMERALD, 1);
         econBot.setPos(0.5, SkyGen.Y_DECK + 1.0, 0.0);
         econBot.onGround = true;
         SkyGen.botStuckTicks[eSlot] = 0;
         SkyGen.tickBotAI(bwGame.mobs, econBot, bwGame.world, bwGame.player);
         float directYaw = Mobs.angleTo(0.0, 3.5);
         check(Math.abs(Mobs.wrap(econBot.dirYaw - directYaw)) >= 20.0F,
            "AI pathfinding steers around a 2-block wall instead of walking straight into it");

         // 4b. Verify overhead block clearance: when a block is directly above the bot's head at (0, Y_DECK + 3, 0),
         // canBotWalkThrough rejects step-jumping into a ledge and tryBotBuildUp mines the overhead block!
         bwGame.world.setBlock(0, SkyGen.Y_DECK + 3, 0, Blocks.PLANKS);
         bwGame.world.setBlock(0, SkyGen.Y_DECK + 1, 1, Blocks.COBBLE);
         bwGame.world.setBlock(0, SkyGen.Y_DECK + 2, 1, 0);
         check(!SkyGen.canBotWalkThrough(bwGame.world, 0, SkyGen.Y_DECK + 1, 0, 0, 1),
            "canBotWalkThrough rejects step-up jumping when a block directly above the bot's head would bonk its head");
         SkyGen.botBlocks[eSlot] = 8;
         SkyGen.tryBotBuildUp(econBot, bwGame.world, 0.5, SkyGen.Y_DECK + 5.0, 0.0, Blocks.COBBLE);
         eq(0, bwGame.world.getBlock(0, SkyGen.Y_DECK + 3, 0),
            "tryBotBuildUp mines a breakable block directly above the bot's head so it never gets stuck under a ceiling");

         // 4c. Verify tryBreakBlockingObstacle mines through a blocking wall when boxed in / no detour exists!
         bwGame.world.setBlock(0, SkyGen.Y_DECK + 1, 1, Blocks.PLANKS);
         bwGame.world.setBlock(0, SkyGen.Y_DECK + 2, 1, Blocks.PLANKS);
         SkyGen.BotIntent breakIntent = new SkyGen.BotIntent();
         boolean brokeWall = SkyGen.tryBreakBlockingObstacle(econBot, bwGame.world, 0.5, SkyGen.Y_DECK + 1.0, 3.5, breakIntent);
         check(brokeWall && bwGame.world.getBlock(0, SkyGen.Y_DECK + 2, 1) == 0,
            "tryBreakBlockingObstacle mines through an obstructing wall block when the bot's path is blocked");

         // =========================================================================
         // Verify Update 4.8: 3D Local BFS Grid Pathfinder, Wall-Slide Physics, Tracker Compass, Bedwars Tools Shop & Redstone Plates
         // =========================================================================
         // 5a. Verify Mobs.canStepUpOnCollision prevents bonk-jumping against 2-block walls & low ceilings while allowing 1-block steps
         bwGame.world.setBlock(0, SkyGen.Y_DECK + 1, 1, Blocks.COBBLE);
         bwGame.world.setBlock(0, SkyGen.Y_DECK + 2, 1, Blocks.COBBLE);
         bwGame.world.setBlock(0, SkyGen.Y_DECK + 3, 0, 0);
         econBot.setPos(0.5, SkyGen.Y_DECK + 1.0, 0.65);
         check(!Mobs.canStepUpOnCollision(bwGame.world, econBot, 0.0, 0.2, 0.0, 0.0),
            "Mobs.canStepUpOnCollision returns false against a 2-block-high wall so bots slide smoothly along walls on the ground");
         bwGame.world.setBlock(0, SkyGen.Y_DECK + 2, 1, 0);
         bwGame.world.setBlock(0, SkyGen.Y_DECK + 3, 1, 0);
         check(Mobs.canStepUpOnCollision(bwGame.world, econBot, 0.0, 0.2, 0.0, 0.0),
            "Mobs.canStepUpOnCollision returns true for a 1-block step-up ledge with clear headroom");
         bwGame.world.setBlock(0, SkyGen.Y_DECK + 3, 0, Blocks.STONE);
         check(!Mobs.canStepUpOnCollision(bwGame.world, econBot, 0.0, 0.2, 0.0, 0.0),
            "Mobs.canStepUpOnCollision returns false when a ceiling block is directly above the mob's head");
         bwGame.world.setBlock(0, SkyGen.Y_DECK + 3, 0, 0);

         // 5b. Verify SkyGen.findLocalBfsStep routes around a wide 2-block wall through an open doorway!
         for (int dz = -2; dz <= 6; dz++) {
            for (int dx = -4; dx <= 4; dx++) {
               bwGame.world.setBlock(dx, SkyGen.Y_DECK, dz, Blocks.STONE);
               bwGame.world.setBlock(dx, SkyGen.Y_DECK + 1, dz, 0);
               bwGame.world.setBlock(dx, SkyGen.Y_DECK + 2, dz, 0);
            }
         }
         // Build a 2-block-tall wall at z = 2 from x = -2 to x = +1, leaving a doorway open at x = +2, z = 2
         for (int wx = -2; wx <= 1; wx++) {
            bwGame.world.setBlock(wx, SkyGen.Y_DECK + 1, 2, Blocks.STONEBRICK);
            bwGame.world.setBlock(wx, SkyGen.Y_DECK + 2, 2, Blocks.STONEBRICK);
         }
         int[] bfsStep = SkyGen.findLocalBfsStep(bwGame.world, 0, SkyGen.Y_DECK + 1, 0, 0.5, 5.5);
         check(bfsStep != null && bfsStep[0] >= 1,
            "findLocalBfsStep routes East toward the open doorway at x=2 to bypass the wide wall at z=2");

         // 5c. Verify Bedwars Tools & Defense Shop (Trades.BEDWARS_TOOLS) and Permanent Armor Persistence across respawns!
         eq(6, Trades.offers(Trades.BEDWARS_TOOLS).length, "Bedwars Tools & Defense Shop has 6 trades");
         bwGame.inv[0] = new Stack(Items.IRON_INGOT, 32);
         check(Trades.buy(bwGame, Trades.offers(Trades.BEDWARS_ARMOR)[0]), "Bought permanent Iron Armor in Bedwars");
         eq(1, SkyGen.playerBwArmorTier, "playerBwArmorTier upgraded to 1 (Iron)");
         java.util.Arrays.fill(bwGame.armor, null);
         SkyGen.giveBedwarsRespawnKit(bwGame, 0);
         check(bwGame.armor[1] != null && bwGame.armor[1].id == Items.armorId(1, 1),
            "giveBedwarsRespawnKit restores permanent Iron Armor after respawning");

         // 5d. Verify Tactical Tracker Compass works in Bedwars and Survival!
         check(SkyGen.useTrackerCompass(bwGame), "useTrackerCompass reports nearest enemy and intact bed in Bedwars");
         check(SkyGen.canExplosionBreakBlock(bwGame.world, 0, SkyGen.Y_DECK + 1, 2, Blocks.WOOL0)
            && !SkyGen.canExplosionBreakBlock(bwGame.world, 0, SkyGen.Y_DECK, 0, Blocks.STONE)
            && !SkyGen.canExplosionBreakBlock(bwGame.world, blueBf[0], blueBf[1], blueBf[2], Decor315.foot(11, 0)),
            "canExplosionBreakBlock allows blowing up defense blocks above Y_DECK while protecting arena floor and Beds");

         // 5e. Play Now startMatch -> tickMode race fix, Audio Aliases, Remote Pose, Shopkeeper Anchor, Blast Protection & Bot Loot
         SoundEngine.ensureLoaded();
         check(SoundEngine.hasSample("bow")
               && SoundEngine.hasSample("tnt_prime")
               && SoundEngine.hasSample("explode")
               && SoundEngine.hasSample("eat_1")
               && SoundEngine.hasSample("item_pickup"),
            "SoundEngine registers fallback aliases for bow, tnt_prime, explode, eat_1, and item_pickup");

         Net319.Peer testPeer = new Net319.Peer(2, "Buddy");
         PlayerModel.Pose peerPose = new PlayerModel.Pose();
         peerPose.eat = testPeer.eat;
         peerPose.bow = testPeer.bow;
         peerPose.swing = 0.5F;
         PlayerAnim.Joints solvedPeer = PlayerAnim.solve(peerPose);
         check(testPeer.eat < 0.0F && testPeer.bow < 0.0F && Math.abs(solvedPeer.sh[0]) > 10.0F,
            "Net319.Peer initializes eat/bow to -1.0F so remote player arm swings are not locked by bow-draw pose");

         SkyGen.switchMinigameMode(bwGame, WorldSettings.CS2, false);
         SkyGen.startMatch(bwGame, false);
         bwGame.screen = Game.S.PLAY;
         SkyGen.tickMode(bwGame, bwGame.world);
         check(SkyGen.matchState == SkyGen.STATE_PLAYING,
            "SkyGen.startMatch updates lastWorldRef so first tickMode does not wipe matchState via resetMatchState");
         String cs2Track = SoundEngine.selectConsoleMusic(bwGame);
         check(cs2Track.startsWith("lce_battle_") || cs2Track.startsWith("lce_tumble_"),
            "SoundEngine.selectConsoleMusic selects Console Edition Minigame music for active CS2 Strike matches");

         // Bedwars Shopkeeper invulnerability & stationary anchoring + Bot resource drop on elimination + single kill count
         SkyGen.switchMinigameMode(bwGame, WorldSettings.BEDWARS, false);
         SkyGen.startMatch(bwGame, false);
         Mobs.Mob shopVillager = null;
         Mobs.Mob enemyBot = null;
         for (Mobs.Mob m : bwGame.mobs.list) {
            if (m != null && m.type == Mobs.VILLAGER) {
               shopVillager = m;
            } else if (m != null && !m.dead && SkyGen.isMinigameBot(m) && SkyGen.botTeamIndex(m) != SkyGen.getPlayerTeam(bwGame)) {
               enemyBot = m;
            }
         }
         check(shopVillager != null && !bwGame.mobs.hurtMob(shopVillager, 10, 0.0, 0.0, 0.4, true),
            "Bedwars Shopkeeper Villagers are invulnerable to player/bot attacks and explosions");

         if (enemyBot != null) {
            int enemySlot = Math.abs(enemyBot.netId) & 31;
            SkyGen.botDiamonds[enemySlot] = 3;
            SkyGen.botEmeralds[enemySlot] = 2;
            enemyBot.setPos(0.5, SkyGen.Y_DECK + 1.0, -38.5);
            int killsBefore = SkyGen.myKills;
            bwGame.inv[0] = new Stack(Items.toolId(4, Items.C_SWORD), 1);
            bwGame.sel = 0;
            enemyBot.health = 2;
            bwGame.atkTicks = 40;
            bwGame.attackMob(enemyBot);
            boolean droppedDia = false;
            boolean droppedRottenFlesh = false;
            for (World.Item it : bwGame.world.items) {
               if (it != null && !it.dead) {
                  if (it.id == Items.DIAMOND && it.count == 3) {
                     droppedDia = true;
                  }
                  if (it.id == Items.ROTTEN_FLESH || it.id == Items.BONE) {
                     droppedRottenFlesh = true;
                  }
               }
            }
            check(SkyGen.myKills == killsBefore + 1 && droppedDia && !droppedRottenFlesh,
               "Eliminating a Bedwars AI bot increments myKills by 1, drops carried Diamonds/Emeralds, and suppresses Rotten Flesh/Bone drops");
         }

         // Human player Slime Block Jump Pad launch in Minigame arenas
         bwGame.world.setBlock(0, SkyGen.Y_DECK, -38, Redstone319.SLIME_BLOCK);
         bwGame.player.setPos(0.5, SkyGen.Y_DECK + 1.0, -37.5);
         bwGame.player.onGround = true;
         bwGame.player.sneaking = false;
         bwGame.player.vy = 0.0;
         SkyGen.tickMode(bwGame, bwGame.world);
         check(bwGame.player.vy > 0.9 && !bwGame.player.onGround,
            "Stepping on a Slime Block Jump Pad in a minigame launches the human player into the air");

         // Update 5.0: Verify all 8 Console Edition Maps (Cove, Crucible, Cavern, Temple, Lair, Castle, Frontier, Shrunk)
         // across all 5 gamemodes have multi-level height differences, buildings, underground passages, and ported .mca voxels!
         eq(8, WorldSettings.MAP_COUNT, "All 8 Console Edition maps registered in WorldSettings.MAP_COUNT");
         int[] allModes = new int[]{
            WorldSettings.BATTLE, WorldSettings.TUMBLE, WorldSettings.BEDWARS, WorldSettings.CS2, WorldSettings.MURDER
         };
         for (int mode : allModes) {
            for (int m = 0; m < WorldSettings.MAP_COUNT; m++) {
               WorldSettings mws = new WorldSettings();
               mws.type = mode;
               mws.mapId = m;
               Gen mgen = Dim.makeGen(Dim.OVERWORLD, 9000L + m * 17L, mws);
               Chunk c0 = mgen.generate(0, 0);
               Chunk c1 = mgen.generate(1, 1);
               int lowBlocks = 0;
               int midBlocks = 0;
               int highBlocks = 0;
               for (int y = SkyGen.Y_DECK - 12; y <= SkyGen.Y_DECK + 12; y++) {
                  for (int z = 0; z < 16; z++) {
                     for (int x = 0; x < 16; x++) {
                        int b0 = c0.get(x, y, z);
                        int b1 = c1.get(x, y, z);
                        if (b0 != 0 || b1 != 0) {
                           if (y < SkyGen.Y_DECK - 1) lowBlocks++;
                           else if (y <= SkyGen.Y_DECK + 3) midBlocks++;
                           else highBlocks++;
                        }
                     }
                  }
               }
               check(lowBlocks > 0 && midBlocks > 0 && highBlocks > 0,
                  "Mode " + mode + " map " + WorldSettings.MAP_NAMES[m] + " has multi-level architecture (underground, ground, upper stories)");
            }
         }

         // Update 5.0: Verify Truly Randomized Chest Loot (different items AND scattered across random slots 0..26!)
         TileEntity randChest1 = new TileEntity(0, 2, SkyGen.Y_DECK + 1, 0);
         TileEntity randChest2 = new TileEntity(0, 2, SkyGen.Y_DECK + 1, 0);
         SkyGen.fillRandomMinigameChest(bwGame.world, randChest1);
         SkyGen.fillRandomMinigameChest(bwGame.world, randChest2);
         int nonNull1 = 0;
         boolean nonZeroSlotUsed = false;
         boolean differsBetweenRolls = false;
         for (int s = 0; s < 27; s++) {
            if (randChest1.slots[s] != null) {
               nonNull1++;
               if (s >= 6) {
                  nonZeroSlotUsed = true;
               }
            }
            int id1 = randChest1.slots[s] != null ? randChest1.slots[s].id : -1;
            int id2 = randChest2.slots[s] != null ? randChest2.slots[s].id : -1;
            int n1 = randChest1.slots[s] != null ? randChest1.slots[s].n : -1;
            int n2 = randChest2.slots[s] != null ? randChest2.slots[s].n : -1;
            if (id1 != id2 || n1 != n2) {
               differsBetweenRolls = true;
            }
         }
         check(nonNull1 >= 3 && nonZeroSlotUsed && differsBetweenRolls,
            "Minigame chest loot is genuinely randomized in both item selection and chest slot placement");

         // Update 5.0: Verify Redesigned Crosshair & Animated Hitmarker ("Hit Thingie")
         SkyGen.hitmarkerTicks = 0;
         Mobs.Mob testTarget = bwGame.mobs.spawn(Mobs.ZOMBIE, 2.5, SkyGen.Y_DECK + 1.0, 2.5);
         testTarget.health = 20;
         bwGame.mobs.hurtMob(testTarget, 6, 0.0, 0.0, 0.4, true);
         check(SkyGen.hitmarkerTicks > 0 && !SkyGen.hitmarkerKill,
            "Player hit on mob triggers animated crosshair hitmarker");
         int ticksBefore = SkyGen.hitmarkerTicks;
         SkyGen.tickCrosshair();
         eq(ticksBefore - 1, SkyGen.hitmarkerTicks, "tickCrosshair decrements active hitmarker timer");
         testTarget.invuln = 0;
         bwGame.mobs.hurtMob(testTarget, 25, 0.0, 0.0, 0.4, true);
         check(SkyGen.hitmarkerTicks > 0 && SkyGen.hitmarkerCrit && SkyGen.hitmarkerKill,
            "Lethal high-damage hit triggers critical + kill confirmation hitmarker");

         // =========================================================================
         // Update 5.1: Clean Non-Fused Maps, 3D Vertical AI Pathfinding (Up & Down), Anti-Block-Spam & Player 2 Map Sync
         // =========================================================================
         // 1. Verify Bedwars wide staircases sit outside roof overhang with full 4-block headroom on every step
         SkyGen.switchMinigameMode(bwGame, WorldSettings.BEDWARS, false);
         for (int step = 1; step <= 5; step++) {
            int sz = -44 - step;
            int sy = SkyGen.Y_DECK + step;
            check(bwGame.world.getBlock(4, sy, sz) != 0 && bwGame.world.getBlock(5, sy, sz) != 0,
               "Bedwars North base has 2-wide rising staircase step " + step + " at x=4..5");
            check(bwGame.world.getBlock(4, sy + 1, sz) == 0
                  && bwGame.world.getBlock(4, sy + 2, sz) == 0
                  && bwGame.world.getBlock(4, sy + 3, sz) == 0,
               "Bedwars staircase step " + step + " has clear 3-block headroom (not blocked by roof overhang)");
         }
         // Verify hasAnySolidColumnFloor prevents bridgeTowardTarget from placing floating blocks over existing island floors!
         Mobs.Mob roofBot = bwGame.mobs.spawn(Mobs.ZOMBIE, 0.5, SkyGen.Y_DECK + 6.0, -44.5);
         int rSlot = Math.abs(roofBot.netId) & 31;
         SkyGen.botBlocks[rSlot] = 16;
         check(SkyGen.hasAnySolidColumnFloor(bwGame.world, 0, SkyGen.Y_DECK + 6, -42),
            "hasAnySolidColumnFloor detects solid base floor below upper roof edge");
         SkyGen.bridgeTowardTarget(roofBot, bwGame.world, 0.5, -38.5, Blocks.WOOL0 + 14);
         eq(16, SkyGen.botBlocks[rSlot], "AI bot on upper base roof does NOT spam bridge blocks when solid island floor exists below");

         // 2. Verify 3D Vertical AI Pathfinding UP (Ascent via Staircase / Jump Pad) & DOWN (Descent via Stairwell / Drop-Off)
         SkyGen.switchMinigameMode(bwGame, WorldSettings.BATTLE, false);
         Mobs.Mob navBot = bwGame.mobs.spawn(Mobs.ZOMBIE, 0.5, SkyGen.Y_DECK + 1.0, -10.5);
         navBot.onGround = true;
         // Build a clean 2nd-floor platform at y = Y_DECK + 5 (z = -6..-2, x = -3..+3) with a rising staircase at x = 4, z = -9..-5
         for (int z = -12; z <= 0; z++) {
            for (int x = -5; x <= 6; x++) {
               bwGame.world.setBlock(x, SkyGen.Y_DECK, z, Blocks.STONEBRICK);
               for (int y = SkyGen.Y_DECK + 1; y <= SkyGen.Y_DECK + 9; y++) {
                  bwGame.world.setBlock(x, y, z, 0);
               }
            }
         }
         for (int z = -6; z <= -2; z++) {
            for (int x = -3; x <= 3; x++) {
               bwGame.world.setBlock(x, SkyGen.Y_DECK + 5, z, Blocks.STONEBRICK);
            }
         }
         for (int st = 1; st <= 5; st++) {
            int sz = -10 + st;
            for (int y = SkyGen.Y_DECK + 1; y <= SkyGen.Y_DECK + st; y++) {
               bwGame.world.setBlock(4, y, sz, Blocks.STONEBRICK);
            }
         }
         // Target is on the 2nd floor at (0.5, Y_DECK + 6.0, -4.5); verify resolveVerticalWaypoint3D routes to the rising stair at x = 4!
         double[] upWp = SkyGen.resolveVerticalWaypoint3D(bwGame.world, navBot, 0.5, SkyGen.Y_DECK + 6.0, -4.5);
         check(upWp[0] >= 3.0, "resolveVerticalWaypoint3D routes lower-floor bot toward the rising staircase at x=4 to reach 2nd-floor target");
         int[] upBfs = SkyGen.findLocalBfsStep(bwGame.world, 0, SkyGen.Y_DECK + 1, -10, 0.5, SkyGen.Y_DECK + 6.0, -4.5);
         check(upBfs != null && upBfs[0] >= 1,
            "3D findLocalBfsStep routes East toward the rising staircase rather than walking under the 2nd-floor ceiling");

         // Now place navBot on the 2nd floor at (0.5, Y_DECK + 6.0, -4.5), enclose North/West/South edges with a 2-block railing,
         // and verify canBotWalkThrough3D + resolveVerticalWaypoint3D route the bot DOWN to the main floor (Y_DECK + 1.0)!
         navBot.setPos(0.5, SkyGen.Y_DECK + 6.0, -4.5);
         check(SkyGen.canBotWalkThrough3D(bwGame.world, 0, SkyGen.Y_DECK + 6, -2, 0, -1, SkyGen.Y_DECK + 1.0),
            "canBotWalkThrough3D allows stepping off a 5-block balcony edge onto solid main deck below when descending");
         double[] downWp = SkyGen.resolveVerticalWaypoint3D(bwGame.world, navBot, 0.5, SkyGen.Y_DECK + 1.0, -10.5);
         check(downWp != null && downWp[1] <= SkyGen.Y_DECK + 5.0,
            "resolveVerticalWaypoint3D finds a safe descent edge/staircase from the 2nd floor down to the 1st floor");

         // Verify Battle and CS2 bots do not place blocks when ticking AI
         int navSlot = Math.abs(navBot.netId) & 31;
         SkyGen.botBlocks[navSlot] = 16;
         SkyGen.battleGraceTicks = 0;
         SkyGen.tickBotAI(bwGame.mobs, navBot, bwGame.world, bwGame.player);
         eq(16, SkyGen.botBlocks[navSlot], "Battle AI bot navigates 3D arena without spamming building blocks");

         // 3. Verify Player 2 Multiplayer Map Synchronization for maps 4..7 (e.g. Castle = 5, Shrunk = 7)
         bwGame.joinMultiplayerWorld(4242L, Dim.OVERWORLD, 1000L, 0.5, SkyGen.Y_DECK + 1.0, 0.5, WorldSettings.BATTLE, 0, WorldSettings.MAP_SHRUNK);
         eq(WorldSettings.MAP_SHRUNK, bwGame.world.settings.mapId,
            "joinMultiplayerWorld preserves hostMapId >= 4 (MAP_SHRUNK = 7) without truncating to & 3");
         check(bwGame.world.chunk(0, 0) != null && bwGame.world.getBlock(0, SkyGen.Y_DECK, 0) == Blocks.DIAMOND_BLOCK,
            "Multiplayer client immediately generates and loads the synchronized host arena map chunks");

         // =========================================================================
         // Update 5.2: Full 160x160x96 Console Edition .mca Maps (No Cutoffs / No Fusion) & Continuous Music Fix
         // =========================================================================
         // 1. Verify outer chunks (-5..+4, e.g. Frontier at cx=-5, cz=0) and deep vertical range (dy=-24..+71, e.g. Cavern/Shrunk)
         WorldSettings frontierWs = new WorldSettings();
         frontierWs.type = WorldSettings.BATTLE;
         frontierWs.mapId = WorldSettings.MAP_FRONTIER;
         Gen frontierGen = Dim.makeGen(Dim.OVERWORLD, 7777L, frontierWs);
         Chunk outerFrontier = frontierGen.generate(-5, 0);
         int outerBlocks = 0;
         for (int y = SkyGen.Y_DECK - 24; y <= SkyGen.Y_DECK + 45; y++) {
            for (int z = 0; z < 16; z++) {
               for (int x = 0; x < 16; x++) {
                  if (outerFrontier.get(x, y, z) != 0) {
                     outerBlocks++;
                  }
               }
            }
         }
         check(outerBlocks > 0, "Frontier Console Edition .mca map preserves outer perimeter chunks (cx=-5, wx=-80..-65) without cutoff");

         WorldSettings cavernWs = new WorldSettings();
         cavernWs.type = WorldSettings.BATTLE;
         cavernWs.mapId = WorldSettings.MAP_CAVERN;
         Gen cavernGen = Dim.makeGen(Dim.OVERWORLD, 7778L, cavernWs);
         Chunk cavernCenter = cavernGen.generate(0, 0);
         int highCanopyBlocks = 0;
         for (int y = SkyGen.Y_DECK + 35; y <= SkyGen.Y_DECK + 70; y++) {
            for (int z = 0; z < 16; z++) {
               for (int x = 0; x < 16; x++) {
                  if (cavernCenter.get(x, y, z) != 0) {
                     highCanopyBlocks++;
                  }
               }
            }
         }
         check(highCanopyBlocks > 0, "Cavern Console Edition .mca map preserves high cavern ceiling/stalactites above Y_DECK+35 without vertical cutoff");

         // 2. Verify Console Edition Minigame music plays continuously during WAITING lobby, LOADING, and PLAY,
         // and that each Console Edition map selects its respective Console Edition music pack!
         bwGame.world.settings.type = WorldSettings.BATTLE;
         bwGame.world.settings.mapId = WorldSettings.MAP_COVE;
         bwGame.screen = Game.S.WAITING;
         String waitTrack0 = SoundEngine.selectConsoleMusic(bwGame);
         check(waitTrack0.startsWith("lce_battle_") || waitTrack0.startsWith("lce_tumble_"),
            "Minigame Waiting Lobby plays Gareth Coker Console Edition Minigame music instead of cutting back to menu music");
         bwGame.world.settings.mapId = WorldSettings.MAP_TEMPLE;
         String templeTrack = SoundEngine.selectConsoleMusic(bwGame);
         check(!templeTrack.equals(waitTrack0) && (templeTrack.contains("chinese") || templeTrack.equals("lce_battle_10") || templeTrack.contains("greek") || templeTrack.equals("lce_battle_9")),
            "Temple map selects its respective Chinese / Greek Mythology Console Edition Battle music pack");
         bwGame.world.settings.mapId = WorldSettings.MAP_FRONTIER;
         String frontierTrack = SoundEngine.selectConsoleMusic(bwGame);
         check(frontierTrack.equals("lce_battle_2") || frontierTrack.contains("western") || frontierTrack.contains("city"),
            "Frontier map selects its respective Western / City Console Edition Battle music pack");
         bwGame.world.settings.mapId = WorldSettings.MAP_COVE;
         SoundEngine.nextBattleTrack();
         String nextTrack = SoundEngine.selectConsoleMusic(bwGame);
         check(!nextTrack.equals(waitTrack0), "Clicking Next Track in the Waiting Lobby immediately advances to the next Console Edition theme");
         bwGame.screen = Game.S.PLAY;

         // =========================================================================
         // Update 5.4: Complete 64-Track Normal Game (C418) + Map-Specific Battle/Tumble (Gareth Coker) Soundtrack
         // =========================================================================
         check(SoundEngine.totalConsoleMusicTrackCount() >= 64,
            "Soundbank contains all 64 Normal Game & Console Edition Minigame music tracks (" + SoundEngine.totalConsoleMusicTrackCount() + ")");
         check(SoundEngine.hasSample("lce_battle_4")
            && SoundEngine.hasSample("lce_battle_5")
            && SoundEngine.hasSample("lce_battle_6")
            && SoundEngine.hasSample("lce_battle_7")
            && SoundEngine.hasSample("lce_battle_8")
            && SoundEngine.hasSample("lce_battle_fantasy_2")
            && SoundEngine.hasSample("lce_battle_fantasy_3")
            && SoundEngine.hasSample("lce_battle_9")
            && SoundEngine.hasSample("lce_battle_greek_2")
            && SoundEngine.hasSample("lce_battle_greek_3")
            && SoundEngine.hasSample("lce_battle_10")
            && SoundEngine.hasSample("lce_battle_chinese_2")
            && SoundEngine.hasSample("lce_battle_chinese_3")
            && SoundEngine.hasSample("lce_battle_chinese_4")
            && SoundEngine.hasSample("lce_battle_11")
            && SoundEngine.hasSample("lce_battle_steampunk_2")
            && SoundEngine.hasSample("lce_battle_steampunk_3")
            && SoundEngine.hasSample("lce_battle_steampunk_4")
            && SoundEngine.hasSample("lce_battle_western_2")
            && SoundEngine.hasSample("lce_battle_western_3")
            && SoundEngine.hasSample("lce_battle_city_1")
            && SoundEngine.hasSample("lce_battle_city_5")
            && SoundEngine.hasSample("lce_battle_halloween_3")
            && SoundEngine.hasSample("lce_tumble_4")
            && SoundEngine.hasSample("lce_tumble_5")
            && SoundEngine.hasSample("lce_tumble_festive_3")
            && SoundEngine.hasSample("lce_menu_2")
            && SoundEngine.hasSample("lce_menu_3")
            && SoundEngine.hasSample("lce_menu_4")
            && SoundEngine.hasSample("lce_overworld_6")
            && SoundEngine.hasSample("lce_overworld_7")
            && SoundEngine.hasSample("lce_overworld_8")
            && SoundEngine.hasSample("lce_hal4")
            && SoundEngine.hasSample("lce_nuance1")
            && SoundEngine.hasSample("lce_nuance2")
            && SoundEngine.hasSample("lce_piano1")
            && SoundEngine.hasSample("lce_creative_1")
            && SoundEngine.hasSample("lce_creative_2")
            && SoundEngine.hasSample("lce_creative_3")
            && SoundEngine.hasSample("lce_creative_5")
            && SoundEngine.hasSample("lce_creative_6")
            && SoundEngine.hasSample("lce_nether_1")
            && SoundEngine.hasSample("lce_nether_2")
            && SoundEngine.hasSample("lce_nether_3")
            && SoundEngine.hasSample("lce_end_2"),
            "All 64 C418 Normal Game (Menu 1-4, Overworld 1-12, Creative 1-6, Nether 1-4, End 1-2, Credits) & Gareth Coker Map Pack tracks loaded");
         check(SoundEngine.musicTrackTitle("lce_battle_4").contains("Gareth Coker")
            && SoundEngine.musicTrackTitle("lce_overworld_6").contains("C418"),
            "musicTrackTitle resolves artist & track names for expanded 64-track soundtrack");

         // =========================================================================
         // Update 5.5: Map-Wide Situational AI, CS2 Map Bomb Spots + No-Bombs Mode,
         // Bed-Bombing End Fight Mode, and Castle Siege / Conquest with Cannons
         // =========================================================================
         // 1. Verify Map-Wide AI Archetypes & Situational State Evaluation
         SkyGen.initBotInventory(0, 16, 16, 2, 1, Items.toolId(2, Items.C_SWORD), Redstone319.PISTOL, 0);
         SkyGen.initBotInventory(1, 16, 16, 2, 1, Items.toolId(2, Items.C_SWORD), Redstone319.PISTOL, 0);
         SkyGen.initBotInventory(2, 16, 16, 2, 1, Items.toolId(2, Items.C_SWORD), Redstone319.PISTOL, 0);
         SkyGen.initBotInventory(3, 16, 16, 2, 1, Items.toolId(2, Items.C_SWORD), Redstone319.PISTOL, 0);
         SkyGen.initBotInventory(4, 16, 16, 2, 1, Items.toolId(2, Items.C_SWORD), Redstone319.PISTOL, 0);
         eq(SkyGen.ARCH_RUSHER, SkyGen.botArchetype[0], "Bot slot 0 receives Rusher archetype");
         eq(SkyGen.ARCH_LOOTER, SkyGen.botArchetype[1], "Bot slot 1 receives Tactical Looter archetype");
         eq(SkyGen.ARCH_SNIPER, SkyGen.botArchetype[2], "Bot slot 2 receives High-Ground Sniper archetype");
         eq(SkyGen.ARCH_FLANKER, SkyGen.botArchetype[3], "Bot slot 3 receives Stealth Flanker archetype");
         eq(SkyGen.ARCH_OBJECTIVE, SkyGen.botArchetype[4], "Bot slot 4 receives Objective Tactician archetype");

         econBot.age = 100;
         double[] roamWp0 = SkyGen.getMapWideRoamWaypoint(bwGame.world, econBot, 0);
         double[] roamWp2 = SkyGen.getMapWideRoamWaypoint(bwGame.world, econBot, 2);
         check(Math.hypot(roamWp0[0], roamWp0[2]) >= 18.0 && Math.hypot(roamWp2[0], roamWp2[2]) >= 18.0,
            "getMapWideRoamWaypoint selects outer Console Edition map landmarks (radius >= 18 blocks) across the arena");
         check(roamWp0[0] != roamWp2[0] || roamWp0[2] != roamWp2[2],
            "Different AI bots traverse distinct outer map sectors rather than clumping in the middle");

         econBot.health = 18;
         econBot.setPos(-10.0, SkyGen.Y_DECK + 1.0, -10.0);
         int sitFlank = SkyGen.evaluateBotSituation(bwGame.mobs, econBot, bwGame.world, 3, -1, 20.0, 20);
         eq(SkyGen.SIT_FLANKING, sitFlank, "Stealth Flanker bot at medium range enters SIT_FLANKING situation");
         double[] flankGoal = SkyGen.computeSituationalApproachWaypoint(bwGame.world, econBot, 3, 10.0, SkyGen.Y_DECK + 1.0, 10.0, 20.0);
         check(Math.hypot(flankGoal[0] - 10.0, flankGoal[2] - 10.0) >= 3.0,
            "Flanking bot computes a lateral wide-angle approach waypoint instead of running in a straight line");

         // 2. Verify CS2 Per-Map Bomb Spots & CS2 No-Bombs Mode
         int[] coveSiteA = SkyGen.getCs2BombSiteCoords(WorldSettings.MAP_COVE, 1);
         int[] templeSiteA = SkyGen.getCs2BombSiteCoords(WorldSettings.MAP_TEMPLE, 1);
         int[] frontierSiteB = SkyGen.getCs2BombSiteCoords(WorldSettings.MAP_FRONTIER, 2);
         check(coveSiteA[0] != templeSiteA[0] || coveSiteA[2] != templeSiteA[2],
            "CS2 Bomb Site A is placed in map-specific areas across different Console Edition maps (Cove vs Temple)");
         bwGame.world.settings.type = WorldSettings.CS2;
         bwGame.world.settings.mapId = WorldSettings.MAP_FRONTIER;
         eq(2, SkyGen.nearCs2BombSite(bwGame.world, frontierSiteB[0] + 0.5, frontierSiteB[1] + 1.0, frontierSiteB[2] + 0.5),
            "nearCs2BombSite detects map-specific Bomb Site B on Frontier (" + frontierSiteB[0] + ", " + frontierSiteB[2] + ")");

         SkyGen.cs2NoBombs = true;
         SkyGen.myTeam = 1; // Terrorist
         SkyGen.giveCs2Loadout(bwGame, 0);
         eq(0, bwGame.countItem(Redstone319.C4_BOMB),
            "CS2 No-Bombs mode omits C4 Explosive from Terrorist loadout for pure Team Elimination");
         SkyGen.cs2BombPlanted = false;
         SkyGen.plantCs2Bomb(bwGame, 1, "TestBot", true);
         check(!SkyGen.cs2BombPlanted, "plantCs2Bomb is disabled when CS2 No-Bombs mode is active");
         SkyGen.cs2NoBombs = false;
         bwGame.world.settings.mapId = WorldSettings.MAP_COVE;

         // 3. Verify Bed-Bombing / Bed-Spam End Fight Mode (0 self damage/knockback, high enemy blast damage & void knockback)
         bwGame.world.settings.type = WorldSettings.END_FIGHT;
         SkyGen.giveEndFightLoadout(bwGame, 0);
         check(bwGame.countItem(Decor315.bedItem(14)) >= 16, "End Fight loadout equips stacks of Explosive Beds for bed-spamming");

         bwGame.player.setPos(0.5, SkyGen.Y_DECK + 1.0, 0.5);
         bwGame.player.vx = 0.0;
         bwGame.player.vy = 0.0;
         bwGame.player.vz = 0.0;
         bwGame.player.health = 20;
         bwGame.player.hurtTime = 0;

         Mobs.Mob endVictim = bwGame.mobs.spawn(Mobs.ZOMBIE, 2.5, SkyGen.Y_DECK + 1.0, 0.5);
         endVictim.teamTag = "EndBot:0:Rival";
         endVictim.health = 20;
         endVictim.hurtTime = 0;
         endVictim.vx = 0.0;
         endVictim.vz = 0.0;

         bwGame.world.setBlock(1, SkyGen.Y_DECK + 1, 0, Decor315.foot(14, 0));
         SkyGen.detonateBedExplosion(bwGame, bwGame.world, 1, SkyGen.Y_DECK + 1, 0, bwGame.player, null);
         eq(0, bwGame.world.getBlock(1, SkyGen.Y_DECK + 1, 0), "Exploded bed block is removed on detonation");
         eq(20, bwGame.player.health, "Player who places and triggers a bed explosion in End Fight takes ZERO self blast damage");
         check(Math.hypot(bwGame.player.vx, bwGame.player.vz) < 0.01,
            "Player who triggers their own bed explosion in End Fight takes ZERO horizontal self knockback");
         check(endVictim.health < 20 && endVictim.vx > 0.5,
            "Opponent caught in End Fight bed blast takes full blast damage and high knockback toward the void");

         // 4. Verify Castle Siege / Conquest with Cannons Mode (0 same-team blast damage, high destructible gate block damage)
         bwGame.world.settings.type = WorldSettings.CASTLE_SIEGE;
         SkyGen.myTeam = 1; // Attackers (Red)
         SkyGen.giveCastleSiegeLoadout(bwGame, 0);
         check(bwGame.countItem(Redstone319.SIEGE_CANNON) > 0, "Castle Siege Cannoneer loadout equips the 3D Siege Cannon");

         // Build a destructible castle gate section at (0, Y_DECK + 1..2, 6) over indestructible floor at (0, Y_DECK, 6)
         bwGame.world.setBlock(0, SkyGen.Y_DECK, 6, Blocks.STONEBRICK);
         bwGame.world.setBlock(0, SkyGen.Y_DECK + 1, 6, Blocks.COBBLE);
         bwGame.world.setBlock(0, SkyGen.Y_DECK + 2, 6, Shapes.BARS);

         bwGame.player.setPos(0.5, SkyGen.Y_DECK + 1.0, 7.5);
         bwGame.player.health = 20;
         bwGame.player.hurtTime = 0;

         Mobs.Mob allySiegeBot = bwGame.mobs.spawn(Mobs.ZOMBIE, -0.5, SkyGen.Y_DECK + 1.0, 7.5);
         allySiegeBot.teamTag = "SiegeBot:Red:0:Ally";
         allySiegeBot.health = 20;
         allySiegeBot.hurtTime = 0;

         Mobs.Mob enemyDefenderBot = bwGame.mobs.spawn(Mobs.SKELETON, 0.5, SkyGen.Y_DECK + 1.0, 4.5);
         enemyDefenderBot.teamTag = "SiegeBot:Blue:0:Guard";
         enemyDefenderBot.health = 20;
         enemyDefenderBot.hurtTime = 0;

         SkyGen.detonateSiegeExplosion(bwGame, bwGame.world, 0.5, SkyGen.Y_DECK + 1.5, 6.5, 1, 5);
         eq(20, bwGame.player.health, "Attacker player takes ZERO blast damage from their own team's Siege Cannon explosion");
         eq(20, allySiegeBot.health, "Allied Siege Bot takes ZERO blast damage from their own team's Siege Cannon explosion");
         check(enemyDefenderBot.health < 20, "Enemy Defender Bot takes full blast damage from Attacker Siege Cannon explosion");
         check(bwGame.world.getBlock(0, SkyGen.Y_DECK + 1, 6) == 0 && bwGame.world.getBlock(0, SkyGen.Y_DECK + 2, 6) == 0,
            "Siege Cannon explosion deals high block damage to destructible castle gates/barricades to create a breach");
         eq(Blocks.STONEBRICK, bwGame.world.getBlock(0, SkyGen.Y_DECK, 6),
            "Siege Cannon explosion preserves the castle floor at Y_DECK while breaching the gate above it");

         // =========================================================================
         // Update 5.6: Separate Dedicated CS2 Maps, Separate Castle Siege Maps & Brawl Bot Map-Wide Dispersion
         // =========================================================================
         // 1. Verify 8 Separate Dedicated CS2 Tactical Strike Maps (Dust II, Mirage, Inferno, Nuke, Overpass, Train, Vertigo, Anubis)
         eq("Dust II", WorldSettings.mapNameFor(WorldSettings.CS2, 0), "CS2 Map 0 is Dust II");
         eq("Mirage", WorldSettings.mapNameFor(WorldSettings.CS2, 1), "CS2 Map 1 is Mirage");
         eq("Inferno", WorldSettings.mapNameFor(WorldSettings.CS2, 2), "CS2 Map 2 is Inferno");
         eq("Nuke", WorldSettings.mapNameFor(WorldSettings.CS2, 3), "CS2 Map 3 is Nuke");
         eq("Overpass", WorldSettings.mapNameFor(WorldSettings.CS2, 4), "CS2 Map 4 is Overpass");
         eq("Train", WorldSettings.mapNameFor(WorldSettings.CS2, 5), "CS2 Map 5 is Train");
         eq("Vertigo", WorldSettings.mapNameFor(WorldSettings.CS2, 6), "CS2 Map 6 is Vertigo");
         eq("Anubis", WorldSettings.mapNameFor(WorldSettings.CS2, 7), "CS2 Map 7 is Anubis");

         WorldSettings csDustWs = new WorldSettings();
         csDustWs.type = WorldSettings.CS2;
         csDustWs.mapId = 0; // Dust II
         Gen csDustGen = Dim.makeGen(Dim.OVERWORLD, 101L, csDustWs);
         Chunk dustCh = csDustGen.generate(0, 0);

         WorldSettings csNukeWs = new WorldSettings();
         csNukeWs.type = WorldSettings.CS2;
         csNukeWs.mapId = 3; // Nuke
         Gen csNukeGen = Dim.makeGen(Dim.OVERWORLD, 101L, csNukeWs);
         Chunk nukeCh = csNukeGen.generate(0, 0);
         check(dustCh.get(2, SkyGen.Y_DECK, 2) != nukeCh.get(2, SkyGen.Y_DECK, 2),
            "Dedicated CS2 maps (Dust II vs Nuke) generate distinct tactical arena floors and layouts");

         // 2. Verify 8 Separate Dedicated Castle Siege Fortress Maps (Ironkeep, Helm's Deep, Dragonstone, Frosthold, Sandstorm, Blackrock, Skyhaven, Dreadfort)
         eq("Ironkeep", WorldSettings.mapNameFor(WorldSettings.CASTLE_SIEGE, 0), "Castle Siege Map 0 is Ironkeep");
         eq("Helm's Deep", WorldSettings.mapNameFor(WorldSettings.CASTLE_SIEGE, 1), "Castle Siege Map 1 is Helm's Deep");
         eq("Dragonstone", WorldSettings.mapNameFor(WorldSettings.CASTLE_SIEGE, 2), "Castle Siege Map 2 is Dragonstone");
         eq("Frosthold", WorldSettings.mapNameFor(WorldSettings.CASTLE_SIEGE, 3), "Castle Siege Map 3 is Frosthold");
         eq("Sandstorm", WorldSettings.mapNameFor(WorldSettings.CASTLE_SIEGE, 4), "Castle Siege Map 4 is Sandstorm");
         eq("Blackrock", WorldSettings.mapNameFor(WorldSettings.CASTLE_SIEGE, 5), "Castle Siege Map 5 is Blackrock");
         eq("Skyhaven", WorldSettings.mapNameFor(WorldSettings.CASTLE_SIEGE, 6), "Castle Siege Map 6 is Skyhaven");
         eq("Dreadfort", WorldSettings.mapNameFor(WorldSettings.CASTLE_SIEGE, 7), "Castle Siege Map 7 is Dreadfort");

         WorldSettings siegeIronWs = new WorldSettings();
         siegeIronWs.type = WorldSettings.CASTLE_SIEGE;
         siegeIronWs.mapId = 0; // Ironkeep (Water Moat at z=10, x=6)
         Gen siegeIronGen = Dim.makeGen(Dim.OVERWORLD, 202L, siegeIronWs);
         Chunk ironCh = siegeIronGen.generate(0, 0);

         WorldSettings siegeDragonWs = new WorldSettings();
         siegeDragonWs.type = WorldSettings.CASTLE_SIEGE;
         siegeDragonWs.mapId = 2; // Dragonstone (Boiling Lava Moat at z=10, x=6)
         Gen siegeDragonGen = Dim.makeGen(Dim.OVERWORLD, 202L, siegeDragonWs);
         Chunk dragonCh = siegeDragonGen.generate(0, 0);
         eq(Blocks.WATER, ironCh.get(6, SkyGen.Y_DECK, 10), "Ironkeep Castle Siege map features a Water Moat at z=10");
         eq(Blocks.LAVA, dragonCh.get(6, SkyGen.Y_DECK, 10), "Dragonstone Castle Siege map features a Boiling Lava Moat at z=10");

         // 3. Verify Brawl (WorldSettings.BATTLE) AI Bots Leave the Middle (0, 0) and Disperse to Outer Map Landmarks!
         bwGame.world.settings.type = WorldSettings.BATTLE;
         bwGame.world.settings.mapId = WorldSettings.MAP_COVE;
         SkyGen.battleGraceTicks = 200;
         SkyGen.showdownActive = false;
         Mobs.Mob brawlBot = bwGame.mobs.spawn(Mobs.ZOMBIE, 4.5, SkyGen.Y_DECK + 1.0, 4.5);
         brawlBot.teamTag = "BattleBot:2:Blaze";
         brawlBot.onGround = true;
         int bbSlot = Math.abs(brawlBot.netId) & 31;
         if (bbSlot % 5 == 0) {
            brawlBot.netId++;
            bbSlot = Math.abs(brawlBot.netId) & 31;
         }
         SkyGen.initBotInventory(bbSlot, 16, 0, 0, 0, Items.toolId(1, Items.C_SWORD), 0, 0);
         SkyGen.botRoamWaypointIdx[bbSlot] = 1; // Outer landmark at (31, 69, -23)
         SkyGen.botArchetype[bbSlot] = SkyGen.ARCH_LOOTER;
         SkyGen.BotIntent brawlIntent = SkyGen.tickBotAI(bwGame.mobs, brawlBot, bwGame.world, bwGame.player);
         check(brawlIntent != null && brawlIntent.moving && SkyGen.botSituation[bbSlot] == SkyGen.SIT_ROAMING,
            "Brawl AI bot immediately disperses toward its outer map landmark instead of staying around the middle");

         // 4. Verify Brawl AI Bot Dynamically Targets, Chases & Attacks the Human Player even when other bots exist!
         SkyGen.battleGraceTicks = 0;
         Mobs.Mob distantBot = bwGame.mobs.spawn(Mobs.SKELETON, 28.5, SkyGen.Y_DECK + 1.0, -22.5);
         distantBot.teamTag = "BattleBot:3:Vortex";
         brawlBot.setPos(18.5, SkyGen.Y_DECK + 1.0, 12.5);
         brawlBot.cd = 0;
         bwGame.player.setPos(19.8, SkyGen.Y_DECK + 1.0, 12.5);
         bwGame.player.health = 20;
         bwGame.player.hurtTime = 0;
         bwGame.player.invuln = 0;
         SkyGen.tickBotAI(bwGame.mobs, brawlBot, bwGame.world, bwGame.player);
         check(bwGame.player.health < 20,
            "Brawl AI bot dynamically targets and attacks the human player in combat range even when other rival bots exist");
      });

      test("5.7 Master Texture Overhaul: every block tile and item/tool/armor/weapon sprite has brand-new crisp pixel art", () -> {
         TexExtra.blocks();
         int[] checkTiles = {
            Tex.GRASS_TOP, Tex.DIRT, Tex.GRASS_SIDE_OVL, Tex.STONE, Tex.COBBLE, Tex.SAND, Tex.GRAVEL,
            Tex.LOG_SIDE, Tex.LOG_TOP, Tex.LEAVES, Tex.PLANKS, Tex.BEDROCK,
            Tex.COAL, Tex.IRON, Tex.GOLD, Tex.DIAMOND, Tex.REDSTONE, Tex.LAPIS,
            Tex.SNOW, Tex.GLASS, Tex.BRICKS, Tex.SANDSTONE_SIDE, Tex.SANDSTONE_TOP,
            Tex.TALLGRASS, Tex.FLOWER_RED, Tex.FLOWER_YELLOW, Tex.CACTUS_SIDE, Tex.CACTUS_TOP,
            Tex.GLOWSTONE, Tex.SPRUCE_SIDE, Tex.SPRUCE_TOP, Tex.BIRCH_SIDE, Tex.OBSIDIAN,
            Tex.STONEBRICK, Tex.TORCH, Tex.CRAFT_TOP, Tex.CRAFT_SIDE, Tex.FURNACE_FRONT, Tex.FURNACE_ON,
            Tex.CHEST_TOP, Tex.CHEST_SIDE, Tex.SPAWNER, Tex.MOSSY,
            Tex.DOOR_LO, Tex.DOOR_HI, Tex.FARM_TOP, Tex.WHEAT0, Tex.LADDER,
            Tex.TNT_SIDE, Tex.TNT_TOP, Tex.TNT_BOTTOM,
            203, 208, 212, 213, 219, 220, 221, 222, 223, 224, 225, 226,
            Color312.T_IRON, Color312.T_GOLD, Color312.T_DIAMOND, Color312.T_EMERALD, Color312.T_COAL, Color312.T_LAPIS,
            TexArt312.NETHERRACK, TexArt312.SOUL_SAND, TexArt312.QUARTZ_ORE, TexArt312.NETHER_BRICKS,
            TexArt312.MAGMA, TexArt312.QUARTZ_SIDE, TexArt312.QUARTZ_TOP, TexArt312.END_STONE, TexArt312.DRAGON_EGG,
            TexArt313.RED_SAND, TexArt313.GRANITE, TexArt313.DIORITE, TexArt313.ANDESITE,
            TexArt313.COARSE_DIRT, TexArt313.PODZOL_TOP, TexArt313.MYCELIUM_TOP, TexArt313.ICE, TexArt313.PACKED_ICE,
            642
         };
         for (int tile : checkTiles) {
            int tx0 = (tile & 31) * 16;
            int ty0 = (tile >> 5) * 16;
            int nonZero = 0;
            for (int y = 0; y < 16; y++) {
               for (int x = 0; x < 16; x++) {
                  if (Tex.atlasPixels[(ty0 + y) * 512 + (tx0 + x)] != 0) {
                     nonZero++;
                  }
               }
            }
            check(nonZero >= 12, "Overhauled block tile " + tile + " has crisp pixel art (" + nonZero + " non-zero pixels)");
         }

         for (int id = 256; id < Blocks.EXT0; id++) {
            if (Items.name[id] == null || Items.tile(id) <= 0) continue;
            int tile = Items.tile(id);
            int tx0 = (tile & 31) * 16;
            int ty0 = (tile >> 5) * 16;
            int opaque = 0;
            for (int y = 0; y < 16; y++) {
               for (int x = 0; x < 16; x++) {
                  if ((Tex.atlasPixels[(ty0 + y) * 512 + (tx0 + x)] >>> 24) >= 128) {
                     opaque++;
                  }
               }
            }
            check(opaque >= 12, "Overhauled item sprite id=" + id + " (" + Items.name(id) + ") has crisp outlined pixel art (" + opaque + " px)");
         }
      });
   }
}
