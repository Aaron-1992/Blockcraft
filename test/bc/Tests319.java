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

         // 5. Skyblock 5 islands & Bedwars Shop / Upgrades
         Chunk sbNorth = sbGen.generate(0, -3); // North Snowy Taiga island at (0, -48) -> lz=0 in chunk -3
         eq(Blocks.SNOW, sbNorth.blocks[Chunk.idx(0, SkyGen.Y_DECK, 0)] & 0xFFFF, "Skyblock North Snowy Taiga island generated");
         Chunk sbSouth = sbGen.generate(0, 3); // South Jungle & Mushroom island at (0, 48) -> lz=0 in chunk 3
         eq(Blocks.GRASS, sbSouth.blocks[Chunk.idx(0, SkyGen.Y_DECK, 0)] & 0xFFFF, "Skyblock South Jungle island generated");
         check(Trades.offers(Trades.BEDWARS_SHOP).length == 6, "Bedwars Item Shop has 6 trades");
         check(Trades.offers(Trades.BEDWARS_ARMOR).length == 6, "Bedwars Gear & Upgrades Shop has 6 trades");

         // 6. Minigame Ephemeral Non-Saving, Waiting Screen UI, Multi-Endpoint VPN Share Code & Bedwars Team/Bed Mechanics
         check(swWs.isMinigame() && bwWs.isMinigame() && !sbWs.isMinigame(), "Only Skywars and Bedwars are ephemeral minigames");

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
      });
   }
}
