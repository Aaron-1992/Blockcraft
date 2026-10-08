package bc;

import static bc.GameTests.GROUND;
import static bc.GameTests.check;
import static bc.GameTests.eq;
import static bc.GameTests.newGame;
import static bc.GameTests.test;

import java.io.File;
import java.util.zip.GZIPOutputStream;

/**
 * Headless tests of Update 3.12 (dimensions, villages, 1.12 content): the foundation - 12 bit block ids, chunk files, the 32 x 32 tile
 * atlas - and then the new features as they are added.   python3 tools/runtests.py unit
 */
final class Tests312 {
   static void run() {
      foundation();
      nether();
      netherMobs();
      fortress();
      stronghold();
      end();
      villages();
      colors();
      parrots();
      advancements();
      pearls();
      netherRules();
   }

   static void deleteAll(File f) {
      File[] kids = f.listFiles();
      if (kids != null) {
         for (File k : kids) {
            deleteAll(k);
         }
      }

      f.delete();
   }

   // ------------------------------------------------------------------ foundation
   static void foundation() {
      test("ids: 0..255 and 1024..4095 are blocks, 256..1023 are items, every table is big enough", () -> {
         check(Blocks.isBlockId(0) && Blocks.isBlockId(255) && Blocks.isBlockId(1024) && Blocks.isBlockId(4095), "block id ranges");
         check(!Blocks.isBlockId(-1) && !Blocks.isBlockId(256) && !Blocks.isBlockId(1023) && !Blocks.isBlockId(4096), "outside the block ranges");
         check(Items.isItem(256) && Items.isItem(1023) && !Items.isItem(255) && !Items.isItem(1024), "item id range");
         eq(Blocks.MAX, Blocks.name.length, "names");
         eq(Blocks.MAX, Blocks.tiles.length, "tiles");
         eq(Blocks.MAX, Blocks.box.length, "boxes");
         eq(Blocks.MAX, Blocks.collMulti.length, "multi collision boxes");
         check(Items.name(1024).equals(Blocks.name[1024]), "a block id is named by Blocks, not by the item table");
         for (int id = 1; id < Blocks.COUNT; id++) {
            check(Blocks.isBlockId(id), "the old block " + id + " is a block id");
         }
      });

      test("chunk blocks: a short per block, ids up to 4095 survive set / get / heightmap", () -> {
         Chunk c = new Chunk(0, 0);
         c.set(1, 70, 2, 1500);
         c.set(3, 71, 4, 4095);
         c.set(5, 72, 6, 255);
         eq(1500, c.get(1, 70, 2), "1500");
         eq(4095, c.get(3, 71, 4), "4095");
         eq(255, c.get(5, 72, 6), "255 is not negative");
         eq(0, c.get(0, 0, 0), "air");
      });

      test("chunk files: files of 3.5 - 3.11 still load; ids above 255 are kept in an extension behind the old bytes", () -> {
         short[] a = new short[65536];
         for (int i = 0; i < a.length; i++) {
            a[i] = (short)(i % 7 == 0 ? 0 : i % 200);
         }

         java.io.ByteArrayOutputStream plain = new java.io.ByteArrayOutputStream();
         Chunk.writeBlocks(new java.io.DataOutputStream(plain), a);
         eq(65536, plain.size(), "no id above 255: the file is exactly the old format");
         short[] back = new short[65536];
         Chunk.readBlocks(new java.io.DataInputStream(new java.io.ByteArrayInputStream(plain.toByteArray())), back);
         check(java.util.Arrays.equals(a, back), "old format round trip");
         // an old file written by 3.5 - 3.11: 65536 bytes and nothing else
         byte[] old = new byte[65536];
         for (int i = 0; i < old.length; i++) {
            old[i] = (byte)(i * 31);
         }

         short[] fromOld = new short[65536];
         Chunk.readBlocks(new java.io.DataInputStream(new java.io.ByteArrayInputStream(old)), fromOld);
         boolean same = true;
         for (int i = 0; i < old.length; i++) {
            same &= fromOld[i] == (old[i] & 255);
         }

         check(same, "bytes of an old file are ids 0..255 (no sign extension)");
         // ids above 255
         a[1] = 1024;
         a[2] = 1500;
         a[65535] = 4095;
         a[777] = 256;
         java.io.ByteArrayOutputStream ext = new java.io.ByteArrayOutputStream();
         Chunk.writeBlocks(new java.io.DataOutputStream(ext), a);
         eq(65536 + 4 + 65536, ext.size(), "extension: magic + one byte per block");
         byte[] raw = ext.toByteArray();
         eq((byte)1500, raw[2], "the first 65536 bytes are the low 8 bits (an old game reads those)");
         short[] b2 = new short[65536];
         Chunk.readBlocks(new java.io.DataInputStream(new java.io.ByteArrayInputStream(raw)), b2);
         check(java.util.Arrays.equals(a, b2), "round trip with extended ids");
         // unknown trailer: ignored, the low bytes are still read
         byte[] junk = new byte[65536 + 6];
         junk[5] = 9;
         junk[65536] = 1;
         short[] b3 = new short[65536];
         Chunk.readBlocks(new java.io.DataInputStream(new java.io.ByteArrayInputStream(junk)), b3);
         eq(9, (int)b3[5], "unknown trailer: blocks still read");
      });

      test("world files: a saved chunk with extended ids comes back through World, and a 3.11 chunk file loads", () -> {
         File dir = java.nio.file.Files.createTempDirectory("ext").toFile();
         try {
            World w = new World(5L, dir);
            Chunk c = new Chunk(4, -3);
            c.set(2, 80, 3, 1500);
            c.set(2, 81, 3, 77);
            w.chunks.put(World.key(4, -3), c);
            w.saveChunk(c);
            w.shutdown();
            File f = new File(dir, "c.4.-3.gz");
            check(f.exists() && f.length() > 0, "the file was written");
            World w2 = new World(5L, dir);
            long t0 = System.currentTimeMillis();
            while (w2.chunks.get(World.key(4, -3)) == null && System.currentTimeMillis() - t0 < 30000) {
               w2.updateChunks(4, -3, 1, 64);
               Thread.sleep(5);
            }

            Chunk r = w2.chunks.get(World.key(4, -3));
            check(r != null, "chunk loaded");
            eq(1500, r.get(2, 80, 3), "the extended id came back");
            eq(77, r.get(2, 81, 3), "and the normal one");
            w2.shutdown();
            // a file in the 3.5 - 3.11 format
            File old = new File(dir, "c.7.7.gz");
            try (java.io.DataOutputStream o = new java.io.DataOutputStream(new GZIPOutputStream(new java.io.FileOutputStream(old)))) {
               byte[] bytes = new byte[65536];
               bytes[Chunk.idx(1, 40, 1)] = (byte)Blocks.STONE;
               bytes[Chunk.idx(1, 41, 1)] = (byte)200; // above 127: must not turn negative
               o.write(bytes);
            }

            World w3 = new World(5L, dir);
            t0 = System.currentTimeMillis();
            while (w3.chunks.get(World.key(7, 7)) == null && System.currentTimeMillis() - t0 < 30000) {
               w3.updateChunks(7, 7, 1, 64);
               Thread.sleep(5);
            }

            Chunk o3 = w3.chunks.get(World.key(7, 7));
            check(o3 != null, "old chunk loaded");
            eq(Blocks.STONE, o3.get(1, 40, 1), "stone");
            eq(200, o3.get(1, 41, 1), "200");
            w3.shutdown();
         } finally {
            deleteAll(dir);
         }
      });

      test("atlas: 32 x 32 tiles of 16 x 16 pixels, every tile has a place and old tiles did not move in meaning", () -> {
         eq(32, Tex.COLS, "columns");
         eq(512, Tex.PX, "pixels per side");
         eq(Tex.PX * Tex.PX, Tex.atlasPixels.length, "pixel array");
         eq(0, Tex.tx(0), "tile 0 x");
         eq(0, Tex.ty(0), "tile 0 y");
         eq(31 * 16, Tex.tx(31), "tile 31 x");
         eq(0, Tex.ty(31), "tile 31 y");
         eq(0, Tex.tx(32), "tile 32 x");
         eq(16, Tex.ty(32), "tile 32 y");
         eq(31 * 16, Tex.tx(1023), "last tile x");
         eq(31 * 16, Tex.ty(1023), "last tile y");
         for (int t = 0; t < 1024; t++) {
            check(Math.abs(Tex.tu(t) * Tex.PX - Tex.tx(t)) < 1e-3 && Math.abs(Tex.tv(t) * Tex.PX - Tex.ty(t)) < 1e-3, "tile " + t + " uv matches its pixels");
         }

         // every tile used by a block or an item is inside the atlas
         for (int id = 1; id < Blocks.MAX; id++) {
            if (Blocks.isBlockId(id) && Blocks.tiles[id] != null) {
               for (int f = 0; f < 6; f++) {
                  check(Blocks.tiles[id][f] >= 0 && Blocks.tiles[id][f] < Tex.COLS * Tex.COLS, "block " + id + " face " + f + " tile in the atlas");
               }
            }
         }

         // the water / lava / fire tiles that are animated are where the shaders and the animation expect them
         check(Tex.tx(23) + 16 <= Tex.PX && Tex.ty(235) + 16 <= Tex.PX, "animated tiles inside the atlas");
         // blit puts a tile where tx / ty say
         int[] px = new int[256];
         java.util.Arrays.fill(px, 0xFF123456);
         Tex.blit(1000, px);
         eq(0xFF123456, Tex.atlasPixels[Tex.ty(1000) * Tex.PX + Tex.tx(1000)], "first pixel of tile 1000");
         eq(0xFF123456, Tex.atlasPixels[(Tex.ty(1000) + 15) * Tex.PX + Tex.tx(1000) + 15], "last pixel of tile 1000");
         java.util.Arrays.fill(Tex.atlasPixels, Tex.ty(1000) * Tex.PX + Tex.tx(1000), Tex.ty(1000) * Tex.PX + Tex.tx(1000) + 16, 0);
      });

      test("meshing: a block with an id above 1023 is drawn, lit and walked on like any other (a probe block defined by the test)", () -> {
         int id = 3999;
         String oldName = Blocks.name[id];
         Blocks.def(id, "Probe", 1.0F, true, 3);
         try {
            Game g = newGame(0);
            g.world.setBlock(5, GROUND + 1, 5, id);
            eq(id, g.world.getBlock(5, GROUND + 1, 5), "stored and read back");
            Chunk c = g.world.chunk(0, 0);
            Mesher.Result r = Mesher.build(g.world, c, (GROUND + 1) >> 4, false);
            check(r.nO >= 24, "a cube has six faces: " + r.nO + " vertices");
            if (r.opaque != null) {
               org.lwjgl.system.MemoryUtil.memFree(r.opaque);
            }

            if (r.trans != null) {
               org.lwjgl.system.MemoryUtil.memFree(r.trans);
            }

            check(Blocks.solid[g.world.getBlock(5, GROUND + 1, 5)], "solid");
            g.world.setBlock(5, GROUND + 1, 5, 0);
            eq(0, g.world.getBlock(5, GROUND + 1, 5), "removed");
         } finally {
            Blocks.name[id] = oldName;
            Blocks.def(id, "Unknown", 1.0F, false, 0);
            Blocks.solid[id] = false;
            Blocks.coll[id] = null;
            Blocks.opacity[id] = 0;
            Blocks.drop[id] = id;
         }
      });
   }

   // ------------------------------------------------------------------ the Nether
   /** Loads the chunks around (cx, cz) the way the game does and waits for them (up to 60 s). */
   static void load(World w, int cx, int cz, int radius) throws Exception {
      long t0 = System.currentTimeMillis();
      while (!loaded(w, cx, cz, radius) && System.currentTimeMillis() - t0 < 30000) {
         w.updateChunks(cx, cz, radius, 64);
         Thread.sleep(2);
      }
   }

   /** The world only asks for the chunks inside a circle (not the corners of the square): are all of them there and lit? */
   static boolean loaded(World w, int cx, int cz, int radius) {
      int lim = (radius + 1) * (radius + 1) + 2;
      for (int dx = -radius - 1; dx <= radius + 1; dx++) {
         for (int dz = -radius - 1; dz <= radius + 1; dz++) {
            if (dx * dx + dz * dz <= lim) {
               Chunk c = w.chunk(cx + dx, cz + dz);
               if (c == null || !c.lit) {
                  return false;
               }
            }
         }
      }

      return true;
   }

   /** An obsidian frame (corners left out, like Minecraft's): interior w x h from (x, y, z) along x (axis 0) or z (axis 1). */
   static void frame(World w, int x, int y, int z, int axis, int fw, int fh) {
      int dx = axis == 0 ? 1 : 0;
      int dz = 1 - dx;
      for (int i = 0; i < fw; i++) {
         w.setBlock(x + dx * i, y - 1, z + dz * i, Blocks.OBSIDIAN);
         w.setBlock(x + dx * i, y + fh, z + dz * i, Blocks.OBSIDIAN);
      }

      for (int j = 0; j < fh; j++) {
         w.setBlock(x - dx, y + j, z - dz, Blocks.OBSIDIAN);
         w.setBlock(x + dx * fw, y + j, z + dz * fw, Blocks.OBSIDIAN);
      }
   }

   static int count(World w, int x0, int y0, int z0, int x1, int y1, int z1, int id) {
      int n = 0;
      for (int x = x0; x <= x1; x++) {
         for (int y = y0; y <= y1; y++) {
            for (int z = z0; z <= z1; z++) {
               if (w.getBlock(x, y, z) == id) {
                  n++;
               }
            }
         }
      }

      return n;
   }

   static void nether() {
      test("Nether blocks and items: registered, textured, with the right behaviour", () -> {
         int[] ids = new int[]{Blocks.NETHERRACK, Blocks.SOUL_SAND, Blocks.QUARTZ_ORE, Blocks.NETHER_BRICKS, Blocks.MAGMA, Blocks.QUARTZ_BLOCK, Blocks.NETHER_BRICK_FENCE,
            Blocks.PORTAL_X, Blocks.PORTAL_Z, Blocks.END_STONE, Blocks.END_FRAME, Blocks.END_FRAME_EYE, Blocks.END_PORTAL, Blocks.DRAGON_EGG};
         for (int id : ids) {
            check(!"Unknown".equals(Blocks.name[id]) && Blocks.name[id] != null, "block " + id + " has a name");
            for (int f = 0; f < 6; f++) {
               int t = Blocks.tiles[id][f];
               check(t >= 256 && t < 1024, Blocks.name[id] + " face " + f + " uses a new tile (" + t + ")");
            }
         }

         check(Blocks.opaque[Blocks.NETHERRACK] && Blocks.solid[Blocks.NETHERRACK] && Blocks.hardness[Blocks.NETHERRACK] > 0, "netherrack is a solid block");
         check(Blocks.emit[Blocks.MAGMA] > 0 && Blocks.emit[Blocks.PORTAL_X] == 11, "magma and the portal glow");
         check(!Blocks.solid[Blocks.PORTAL_X] && Blocks.coll[Blocks.PORTAL_X] == null && Blocks.kind[Blocks.PORTAL_Z] == Blocks.K_PORTAL, "the film cannot be bumped into");
         check(Blocks.isPortal(Blocks.PORTAL_X) && Blocks.isPortal(Blocks.PORTAL_Z) && !Blocks.isPortal(Blocks.OBSIDIAN), "isPortal");
         check(!Blocks.targetable(Blocks.PORTAL_X), "you reach through the film");
         check(Blocks.isFence(Blocks.NETHER_BRICK_FENCE) && Blocks.isFence(Blocks.FENCE) && !Blocks.isFence(Blocks.STONE), "fences");
         eq(Items.QUARTZ, Blocks.drop[Blocks.QUARTZ_ORE], "quartz ore drops quartz");
         java.util.HashSet<Integer> tiles = new java.util.HashSet<>();
         for (int id = Items.QUARTZ; id <= Items.EMERALD; id++) {
            check(Items.name[id] != null, "item " + id + " has a name");
            check(tiles.add(Items.tile[id]) && Items.tile[id] >= 272 && Items.tile[id] < 1024, "item " + id + " has its own tile");
            int[] sp = TexArt312.sprite(id);
            int n = 0;
            for (int px : sp) {
               if (px >>> 24 != 0) {
                  n++;
               }
            }

            check(n > 20 && n < 250, Items.name[id] + " has a picture (" + n + " pixels)");
            check(Items.isItem(id), id + " is an item id");
         }

         eq(Stack.class, Recipes.find(new Stack[]{new Stack(Items.QUARTZ, 1), new Stack(Items.QUARTZ, 1), new Stack(Items.QUARTZ, 1), new Stack(Items.QUARTZ, 1)}, 2, 2) == null ? null : Stack.class, "4 quartz make a block");
         eq(Blocks.QUARTZ_BLOCK, Recipes.find(new Stack[]{new Stack(Items.QUARTZ, 1), new Stack(Items.QUARTZ, 1), new Stack(Items.QUARTZ, 1), new Stack(Items.QUARTZ, 1)}, 2, 2).id, "quartz block");
         eq(Items.BLAZE_POWDER, Recipes.find(new Stack[]{new Stack(Items.BLAZE_ROD, 1)}, 1, 1).id, "blaze powder");
         eq(2, Recipes.find(new Stack[]{new Stack(Items.BLAZE_ROD, 1)}, 1, 1).n, "two of it");
         eq(Items.EYE_OF_ENDER, Recipes.find(new Stack[]{new Stack(Items.ENDER_PEARL, 1), new Stack(Items.BLAZE_POWDER, 1)}, 2, 1).id, "eye of ender");
         eq(Items.NETHER_BRICK, Recipes.smeltResult(new Stack(Blocks.NETHERRACK, 1)).id, "netherrack smelts to a nether brick");
         eq(Items.QUARTZ, Recipes.smeltResult(new Stack(Blocks.QUARTZ_ORE, 1)).id, "quartz ore smelts to quartz");
         check(Recipes.fuelTime(new Stack(Items.BLAZE_ROD, 1)) >= 2000, "a blaze rod burns for a long time");
      });

      test("Nether terrain: bedrock sheets, lava sea, rock, quartz, glowstone, the same chunk every time, different seeds differ", () -> {
         NetherGen g = new NetherGen(4242L);
         int rock = 0;
         int air = 0;
         int lavaBelow = 0;
         int lavaAbove = 0;
         int quartz = 0;
         int glow = 0;
         int soul = 0;
         int stand = 0;
         for (int cz = -2; cz <= 1; cz++) {
            for (int cx = -2; cx <= 1; cx++) {
               Chunk c = g.generate(cx, cz);
               Chunk again = g.generate(cx, cz);
               check(java.util.Arrays.equals(c.blocks, again.blocks), "chunk " + cx + "," + cz + " is the same when generated again");
               for (int z = 0; z < 16; z++) {
                  for (int x = 0; x < 16; x++) {
                     eq(Blocks.BEDROCK, c.get(x, 0, z), "floor");
                     eq(Blocks.BEDROCK, c.get(x, 127, z), "ceiling");
                     eq(127, (int)c.hm[z * 16 + x], "heightmap");
                     boolean place = false;
                     for (int y = 1; y < 127; y++) {
                        int id = c.get(x, y, z);
                        if (id == Blocks.NETHERRACK) {
                           rock++;
                        } else if (id == 0) {
                           air++;
                        } else if (Blocks.isLava(id)) {
                           if (y <= 32) {
                              lavaBelow++;
                           } else {
                              lavaAbove++;
                           }
                        } else if (id == Blocks.QUARTZ_ORE) {
                           quartz++;
                        } else if (id == Blocks.GLOWSTONE) {
                           glow++;
                        } else if (id == Blocks.SOUL_SAND) {
                           soul++;
                        }

                        if (!place && y > 34 && y < 118 && id != 0 && Blocks.solid[id] && !Blocks.isLava(id) && c.get(x, y + 1, z) == 0 && c.get(x, y + 2, z) == 0 && c.get(x, y + 3, z) == 0) {
                           place = true;
                        }
                     }

                     if (place) {
                        stand++;
                     }
                  }
               }
            }
         }

         int total = rock + air + lavaBelow;
         check(rock > total / 6 && rock < total * 3 / 4, "rock is a good part of the Nether (" + rock * 100 / total + "%)");
         check(air > total / 5, "there is plenty of open cave (" + air * 100 / total + "%)");
         check(lavaBelow > 2000, "a lava sea (" + lavaBelow + " blocks)");
         eq(0, lavaAbove, "lava only in the sea (nothing generated above it)");
         check(quartz > 20, "quartz ore (" + quartz + ")");
         check(glow > 5, "glowstone (" + glow + ")");
         check(stand > 16 * 16 * 16 / 3, "a third of the columns have a place to stand (" + stand + " of 4096)");
         Chunk other = new NetherGen(4243L).generate(0, 0);
         check(!java.util.Arrays.equals(g.generate(0, 0).blocks, other.blocks), "another seed makes another Nether");
         int[] floor = g.findFloor(0, 0);
         check(floor == null || floor[1] > 32, "findFloor is above the lava sea");
      });

      test("Nether world: no sky light, no weather, light from lava and glowstone, the clock is the world's", () -> {
         World w = new World(31L, null, Dim.NETHER);
         try {
            eq(Dim.NETHER, w.dim, "dimension");
            check(!w.weather.enabled && !w.disasters.enabled, "no weather, no disasters");
            for (int cz = 0; cz <= 1; cz++) {
               for (int cx = 0; cx <= 1; cx++) {
                  Chunk c = w.gen.generate(cx, cz);
                  w.chunks.put(World.key(cx, cz), c);
               }
            }

            for (Chunk c : new java.util.ArrayList<>(w.chunks.values())) {
               w.lightChunk(c);
            }

            int skyMax = 0;
            int lit = 0;
            int lavaAir = 0;
            for (int x = 0; x < 32; x++) {
               for (int z = 0; z < 32; z++) {
                  for (int y = 1; y < 127; y++) {
                     skyMax = Math.max(skyMax, w.getSky(x, y, z));
                     if (w.getBlock(x, y, z) == 0 && w.getBlk(x, y, z) > 0) {
                        lit++;
                     }

                     if (w.getBlock(x, y, z) == 0 && Blocks.isLava(w.getBlock(x, y - 1, z)) && w.getBlk(x, y, z) >= 13) {
                        lavaAir++;
                     }
                  }
               }
            }

            eq(0, skyMax, "no sky light anywhere");
            check(lit > 1000, "block light spreads (" + lit + " lit cells)");
            check(lavaAir > 50, "air over the lava sea is bright (" + lavaAir + ")");
            w.tick();
            w.tick();
            eq(2L, w.tickCount, "it ticks");
         } finally {
            w.shutdown();
         }
      });

      test("nether portal frames: lit with fire they come alive, broken they go, wrong frames do nothing", () -> {
         Game g = newGame(0);
         World w = g.world;
         int y = GROUND + 1;
         // 1: the classic 2 x 3 (4 x 5 with the frame) along x
         frame(w, 4, y, 4, 0, 2, 3);
         w.setBlock(4, y, 4, Blocks.FIRE);
         eq(6, count(w, 4, y, 4, 5, y + 2, 4, Blocks.PORTAL_X), "six film blocks");
         eq(0, count(w, 4, y, 4, 5, y + 2, 4, Blocks.FIRE), "no fire left inside");
         check(Portals.intact(w, 4, y, 4) && Portals.intact(w, 5, y + 2, 4), "the film is intact");
         // the box of the player touches it
         double[] bb = new double[]{4.7, y, 3.8, 5.3, y + 1.8, 4.4};
         check(Portals.touching(w, bb) != null, "standing in the film");
         double[] away = new double[]{4.7, y, 2.0, 5.3, y + 1.8, 2.6};
         check(Portals.touching(w, away) == null, "standing next to it");
         // breaking a corner of the frame (a side column): the film fades, block update after block update
         w.setBlock(3, y + 1, 4, 0);
         for (int i = 0; i < 12; i++) {
            w.tick();
         }

         eq(0, count(w, 4, y, 4, 5, y + 2, 4, Blocks.PORTAL_X), "the film is gone");
         // 2: along z, bigger (3 x 4 interior)
         frame(w, 10, y, 6, 1, 3, 4);
         w.setBlock(10, y, 7, Blocks.FIRE);
         eq(12, count(w, 10, y, 6, 10, y + 3, 8, Blocks.PORTAL_Z), "twelve film blocks for 3 x 4");
         // 3: fire next to a frame that is missing its top
         frame(w, 16, y, 4, 0, 2, 3);
         w.setBlock(17, y + 3, 4, 0);
         w.setBlock(16, y, 4, Blocks.FIRE);
         eq(0, count(w, 16, y, 4, 17, y + 2, 4, Blocks.PORTAL_X), "an open frame stays dark");
         // 4: something in the way inside
         frame(w, 22, y, 4, 0, 2, 3);
         w.setBlock(23, y + 1, 4, Blocks.STONE);
         w.setBlock(22, y, 4, Blocks.FIRE);
         eq(0, count(w, 22, y, 4, 23, y + 2, 4, Blocks.PORTAL_X), "a stone in the frame: no portal");
         // 5: too small (interior 1 x 3, 2 x 2)
         frame(w, 28, y, 4, 0, 1, 3);
         w.setBlock(28, y, 4, Blocks.FIRE);
         eq(0, count(w, 28, y, 4, 28, y + 2, 4, Blocks.PORTAL_X), "1 wide is too narrow");
         frame(w, 32, y, 4, 0, 2, 2);
         w.setBlock(32, y, 4, Blocks.FIRE);
         eq(0, count(w, 32, y, 4, 33, y + 1, 4, Blocks.PORTAL_X), "2 high is too low");
         // 6: fire somewhere else makes nothing
         w.setBlock(40, y, 40, Blocks.FIRE);
         eq(Blocks.FIRE, w.getBlock(40, y, 40), "ordinary fire is ordinary fire");
         // 7: a block put into the film closes the portal
         frame(w, 4, y, 12, 0, 2, 3);
         w.setBlock(4, y, 12, Blocks.FIRE);
         eq(6, count(w, 4, y, 12, 5, y + 2, 12, Blocks.PORTAL_X), "again");
         w.setBlock(5, y + 1, 12, Blocks.STONE);
         for (int i = 0; i < 12; i++) {
            w.tick();
         }

         eq(0, count(w, 4, y, 12, 5, y + 2, 12, Blocks.PORTAL_X), "a block in the film closes it");
      });

      test("nether portal travel: Overworld to Nether at 1/8, back at 8 times, the same portal is used, whatever you left behind is saved", () -> {
         File dir = java.nio.file.Files.createTempDirectory("nether").toFile();
         Game g = null;
         try {
            g = newGame(1);
            g.world.shutdown();
            World ow = new World(1234L, dir);
            ow.saveEnabled = true;
            for (int cz = -6; cz <= 6; cz++) {
               for (int cx = -6; cx <= 6; cx++) {
                  Chunk c = new Chunk(cx, cz);
                  for (int x = 0; x < 16; x++) {
                     for (int z = 0; z < 16; z++) {
                        c.set(x, 0, z, Blocks.BEDROCK);
                        for (int y = 1; y < GROUND; y++) {
                           c.set(x, y, z, Blocks.STONE);
                        }

                        c.set(x, GROUND, z, Blocks.GRASS);
                     }
                  }

                  c.computeHeightmap();
                  c.modified = true;
                  ow.chunks.put(World.key(cx, cz), c);
               }
            }

            for (Chunk c : new java.util.ArrayList<>(ow.chunks.values())) {
               ow.lightChunk(c);
            }

            ow.game = g;
            g.world = ow;
            g.rootDir = dir;
            g.screen = Game.S.PLAY;
            int y = GROUND + 1;
            frame(ow, 64, y, 64, 0, 2, 3);
            ow.setBlock(64, y, 64, Blocks.FIRE);
            eq(6, count(ow, 64, y, 64, 65, y + 2, 64, Blocks.PORTAL_X), "an Overworld portal at 64, 64");
            g.player.setMode(0);
            g.player.setPos(65.0, y, 64.5);
            // standing in it for four seconds
            for (int i = 0; i < Portals.WAIT - 1; i++) {
               Portals.tickPlayer(g);
            }

            eq(Dim.OVERWORLD, g.world.dim, "not yet");
            check(g.portalTime > 0, "the haze grows");
            Portals.tickPlayer(g);
            eq(Dim.NETHER, g.world.dim, "now in the Nether");
            eq(Game.S.LOADING, g.screen, "the loading screen is up");
            check(g.arrivalAt != null && Math.abs(g.arrivalAt[0] - 65.0 / 8.0) < 0.01 && Math.abs(g.arrivalAt[1] - 64.5 / 8.0) < 0.01, "the way in is at 1/8");
            // the game loads chunks around the player until the screen goes away; this does the same
            load(g.world, (int)Math.floor(g.player.x) >> 4, (int)Math.floor(g.player.z) >> 4, 2);
            g.finishArrival();
            g.screen = Game.S.PLAY;
            World nw = g.world;
            check(Portals.touching(nw, g.player.bb) != null, "the player stands in the film of the Nether portal");
            check(g.portalLock, "and may not be sent back at once");
            int[] cell = Portals.touching(nw, g.player.bb);
            int ny = cell[1];
            check(nw.getBlock(cell[0], ny - 1, cell[2]) == Blocks.OBSIDIAN, "a floor of obsidian under it");
            check(count(nw, cell[0] - 3, ny, cell[2] - 3, cell[0] + 3, ny + 2, cell[2] + 3, Blocks.PORTAL_X) == 6 || count(nw, cell[0] - 3, ny, cell[2] - 3, cell[0] + 3, ny + 2, cell[2] + 3, Blocks.PORTAL_Z) == 6, "a complete portal");
            check(!Phys.collides(nw, g.player.bb), "not stuck in anything");
            for (int i = 0; i < 200; i++) {
               Portals.tickPlayer(g);
            }

            eq(Dim.NETHER, g.world.dim, "no bouncing back while you stand in it");
            // walk out and in again
            g.player.setPos(g.player.x + 4.0, g.player.y, g.player.z + 4.0);
            Portals.tickPlayer(g);
            check(!g.portalLock, "free again");
            g.player.setPos(cell[0] + 0.5, ny, cell[2] + 0.5);
            double nx = cell[0] + 0.5;
            double nz = cell[2] + 0.5;
            for (int i = 0; i < Portals.WAIT + 1 && g.world.dim == Dim.NETHER; i++) {
               Portals.tickPlayer(g);
            }

            eq(Dim.OVERWORLD, g.world.dim, "back in the Overworld");
            check(g.arrivalAt != null && Math.abs(g.arrivalAt[0] - nx * 8.0) < 0.01, "8 times as far");
            load(g.world, (int)Math.floor(g.player.x) >> 4, (int)Math.floor(g.player.z) >> 4, 2);
            g.finishArrival();
            g.screen = Game.S.PLAY;
            // the portal that was built in the Overworld is the one used (it was saved and loaded again), not a new one
            check(Portals.touching(g.world, g.player.bb) != null, "standing in a portal film");
            check(Math.abs(g.player.x - 65.0) < 3.0 && Math.abs(g.player.z - 64.5) < 3.0, "the same portal as before (" + g.player.x + ", " + g.player.z + ")");
            eq(6, count(g.world, 64, y, 64, 65, y + 2, 64, Blocks.PORTAL_X), "and it is still there");
            // what is saved: the Overworld's chunks in the world folder, the Nether's in DIM-1, and which dimension the player is in
            g.player.setMode(0);
            g.switchDimension(Dim.NETHER, 10.0, 10.0, 0);
            load(g.world, 0, 0, 2);
            g.finishArrival();
            g.screen = Game.S.PLAY;
            g.world.setBlock(2, 70, 2, Blocks.MAGMA);
            g.saveWorld();
            java.util.Properties pr = new java.util.Properties();
            try (java.io.FileInputStream in = new java.io.FileInputStream(new File(dir, "level.properties"))) {
               pr.load(in);
            }

            eq("-1", pr.getProperty("dim"), "level.properties says the player is in the Nether");
            check(new File(dir, "DIM-1").isDirectory() && new File(dir, "DIM-1").list().length > 0, "the Nether has its own folder with chunks");
            check(new File(dir, "c.4.4.gz").exists() || new File(dir, "c.0.0.gz").exists(), "the Overworld chunks are in the world folder");
            // died in the Nether: respawn at home, in the Overworld
            g.player.dead = true;
            g.respawn();
            eq(Dim.OVERWORLD, g.world.dim, "respawn goes home");
            load(g.world, (int)Math.floor(g.player.x) >> 4, (int)Math.floor(g.player.z) >> 4, 2);
            g.finishArrival();
            check(!g.player.dead && g.player.health == 20, "alive again");
            // the block that was put into the Nether is still there when we go back
            g.screen = Game.S.PLAY;
            g.switchDimension(Dim.NETHER, 10.0, 10.0, 0);
            load(g.world, 0, 0, 2);
            eq(Blocks.MAGMA, g.world.getBlock(2, 70, 2), "the Nether remembers what was built in it");
            // the /dim command does the same trip without waiting in a portal
            g.finishArrival();
            g.screen = Game.S.PLAY;
            double px = g.player.x;
            g.runCommand("/dim overworld");
            eq(Dim.OVERWORLD, g.world.dim, "/dim overworld");
            check(g.arrivalAt != null && Math.abs(g.arrivalAt[0] - px * 8.0) < 0.01, "/dim goes to 8 times the place");
            load(g.world, (int)Math.floor(g.player.x) >> 4, (int)Math.floor(g.player.z) >> 4, 2);
            g.finishArrival();
            g.screen = Game.S.PLAY;
            g.runCommand("/dim overworld");
            eq(Dim.OVERWORLD, g.world.dim, "already there: nothing happens");
            eq(Game.S.PLAY, g.screen, "and the screen stays");
         } finally {
            if (g != null && g.world != null) {
               g.world.shutdown();
            }

            deleteAll(dir);
         }
      });
   }

   // ------------------------------------------------------------------ the mobs of the Nether
   static void netherMobs() {
      test("mob table: the new kinds are in every table, hostile / peaceful are told apart", () -> {
         eq(18, Mobs.N, "eighteen mobs (the parrots are the last ones)");
         for (int t = 0; t < Mobs.NAME.length; t++) {
            check(Mobs.NAME[t] != null && Mobs.HP[t] > 0 && Mobs.W[t] > 0 && Mobs.H[t] > 0 && (Mobs.SPD[t] > 0 || t == Mobs.END_CRYSTAL), "mob " + t + " has name, health, size and speed");
         }

         eq(18, Mobs.NAME.length, "a name for every mob");
         eq(18, Mobs.HP.length, "health");
         eq(18, Mobs.W.length, "width");
         eq(18, Mobs.H.length, "height");
         eq(18, Mobs.SPD.length, "speed");
         eq("enderman", Mobs.NAME[Mobs.ENDERMAN], "ids and names agree");
         eq("endcrystal", Mobs.NAME[Mobs.END_CRYSTAL], "ids and names agree (crystal)");
         eq("parrot", Mobs.NAME[Mobs.PARROT], "ids and names agree (parrot)");
         check(Mobs.hostile(Mobs.ENDERMAN) && Mobs.hostile(Mobs.ENDER_DRAGON), "the End's monsters are hostile");

         check(!Mobs.hostile(Mobs.PIG) && !Mobs.hostile(Mobs.CHICKEN), "animals are not hostile");
         check(Mobs.hostile(Mobs.ZOMBIE) && Mobs.hostile(Mobs.ZOMBIE_PIGMAN) && Mobs.hostile(Mobs.GHAST) && Mobs.hostile(Mobs.BLAZE) && Mobs.hostile(Mobs.MAGMA_CUBE), "the monsters are");
         check(!Mobs.hostile(Mobs.VILLAGER) && !Mobs.hostile(Mobs.IRON_GOLEM) && !Mobs.hostile(Mobs.PARROT), "villagers, golems and parrots are not");
         check(Mobs312.fireproof(Mobs.BLAZE) && Mobs312.fireproof(Mobs.ZOMBIE_PIGMAN) && !Mobs312.fireproof(Mobs.ZOMBIE), "fire resistance");
         check(Mobs312.flies(Mobs.GHAST) && Mobs312.flies(Mobs.BLAZE) && !Mobs312.flies(Mobs.PIG), "who floats");
         // the models: every box fits into its texture, every part has a kind the renderer knows
         MobRender.Model[] models = new MobRender.Model[Mobs.N];
         MobModels312.build(models);
         for (int t = Mobs.ZOMBIE_PIGMAN; t < Mobs.N; t++) {
            MobRender.Model m = models[t];
            check(m != null && m.parts.size() > 0, Mobs.NAME[t] + " has a model");
            for (MobRender.Part p : m.parts) {
               check(p.kind >= 1 && p.kind <= 19, Mobs.NAME[t] + ": part kind " + p.kind);
               if (!p.shared) {
                  check(p.u + 2 * (p.d + p.w) <= m.tw || p.u >= 0, Mobs.NAME[t] + ": box in the texture");
                  check(p.v + p.d + p.h <= m.th, Mobs.NAME[t] + ": box in the texture (height " + (p.v + p.d + p.h) + ")");
                  check(p.u + 2 * p.d + 2 * p.w <= m.tw, Mobs.NAME[t] + ": box in the texture (width " + (p.u + 2 * p.d + 2 * p.w) + ")");
               }
            }
         }

         eq(4.0F, models[Mobs.GHAST].scale, "the ghast is drawn four times as big");
         eq(12, models[Mobs.BLAZE].parts.stream().filter(p -> p.kind == 12).count() > 0 ? 12 : 0, "twelve rods");
         check(MobModels312.pixel(models[Mobs.MAGMA_CUBE], models[Mobs.MAGMA_CUBE].parts.get(0), 3, 4, 0) != null, "the magma cube has its own colours");
      });

      test("zombie pigmen: calm until hit, then they all come; fire does nothing to them; what they drop", () -> {
         Game g = newGame(0);
         g.mobs.rnd.setSeed(20123L + 678);
         Mobs.Mob near = g.mobs.spawn(Mobs.ZOMBIE_PIGMAN, 14.5, GROUND + 1, 8.5);
         Mobs.Mob mate = g.mobs.spawn(Mobs.ZOMBIE_PIGMAN, 20.5, GROUND + 1, 12.5);
         Mobs.Mob far = g.mobs.spawn(Mobs.ZOMBIE_PIGMAN, 8.5, GROUND + 1, -29.5);
         for (int i = 0; i < 160; i++) {
            g.mobs.tick();
         }

         eq(20, g.player.health, "calm pigmen do not attack");
         check(!near.hasTarget && near.angry == 0, "and do not hunt");
         // the player hits one
         g.mobs.hurtMob(near, 1, g.player.x, g.player.z, 0.4, true);
         check(near.angry > 0 && mate.angry > 0, "the one that was hit and the one next to it are angry");
         eq(0, far.angry, "the one 50 blocks away does not care");
         int hp = g.player.health;
         for (int i = 0; i < 400 && g.player.health == hp; i++) {
            g.mobs.tick();
         }

         check(g.player.health < hp, "an angry pigman hurts the player (health " + g.player.health + ")");
         // lava does nothing to a pigman
         World w = g.world;
         for (int x = 30; x <= 32; x++) {
            for (int z = 30; z <= 32; z++) {
               w.setBlock(x, GROUND, z, Blocks.LAVA);
            }
         }

         Mobs.Mob swimmer = g.mobs.spawn(Mobs.ZOMBIE_PIGMAN, 31.5, GROUND, 31.5);
         int before = swimmer.health;
         for (int i = 0; i < 100; i++) {
            g.mobs.tick();
         }

         eq(before, swimmer.health, "a pigman bathes in lava");
         eq(0, swimmer.burn, "and does not burn");
         // drops
         java.util.HashSet<Integer> seen = new java.util.HashSet<>();
         for (int i = 0; i < 300; i++) {
            Mobs.Mob d = g.mobs.spawn(Mobs.ZOMBIE_PIGMAN, 40.5, GROUND + 1, 40.5);
            g.mobs.hurtMob(d, 100, 0, 0, 0, false);
         }

         for (World.Item it : w.items) {
            seen.add(it.id);
         }

         check(seen.contains(Items.GOLD_NUGGET) && seen.contains(Items.ROTTEN_FLESH), "gold nuggets and rotten flesh: " + seen);
         for (int id : seen) {
            check(id == Items.GOLD_NUGGET || id == Items.ROTTEN_FLESH || id == Items.GOLD_INGOT, "nothing else drops (" + id + ")");
         }
      });

      test("ghasts float and throw fireballs that blow holes and hurt; blazes hover and shoot bursts that set you alight", () -> {
         Game g = newGame(0);
         g.mobs.rnd.setSeed(20123L + 732);
         World w = g.world;
         Mobs.Mob ghast = g.mobs.spawn(Mobs.GHAST, 8.5, GROUND + 12, 34.5);
         check(ghast.bb[4] - ghast.bb[1] > 3.9, "a ghast is four blocks high");
         int fired = 0;
         int hurt = 0;
         double minY = 999;
         for (int i = 0; i < 600; i++) {
            g.mobs.tick();
            minY = Math.min(minY, ghast.y);
            fired = Math.max(fired, g.mobs.fireballs.size());
            if (g.player.health < 20 && hurt == 0) {
               hurt = i;
            }
         }

         check(fired > 0, "the ghast threw a fireball");
         check(minY > GROUND + 1.5, "and did not fall to the ground (lowest " + minY + ")");
         check(g.player.health < 20 || count(w, -8, GROUND - 3, 8, 24, GROUND, 40, 0) > 0 || w.getBlock(8, GROUND + 1, 8) != 0 || g.mobs.fireballs.size() > 0, "the fireball landed somewhere");
         // a fireball against the ground: a hole and a fire
         Mobs312.Fireball fb = new Mobs312.Fireball();
         fb.x = 30.5;
         fb.y = GROUND + 0.6;
         fb.z = 30.5;
         fb.big = true;
         w.setBlock(30, GROUND, 30, Blocks.STONE);
         w.setBlock(31, GROUND, 30, Blocks.STONE);
         w.setBlock(30, GROUND, 31, Blocks.OBSIDIAN);
         int stoneBefore = count(w, 27, GROUND - 2, 27, 33, GROUND + 2, 33, Blocks.STONE) + count(w, 27, GROUND - 2, 27, 33, GROUND + 2, 33, Blocks.DIRT);
         for (int k = 0; k < 6; k++) {
            Mobs312.explode(g.mobs, w, g.player, fb);
         }

         int stoneAfter = count(w, 27, GROUND - 2, 27, 33, GROUND + 2, 33, Blocks.STONE) + count(w, 27, GROUND - 2, 27, 33, GROUND + 2, 33, Blocks.DIRT);
         check(stoneAfter < stoneBefore, "a hole in the ground (" + stoneBefore + " -> " + stoneAfter + ")");
         eq(Blocks.OBSIDIAN, w.getBlock(30, GROUND, 31), "obsidian stands");
         check(count(w, 26, GROUND - 1, 26, 34, GROUND + 3, 34, Blocks.FIRE) > 0, "and there are fires");
         // a blaze
         Game g2 = newGame(0);
         g2.mobs.rnd.setSeed(20123L + 770);
         Mobs.Mob blaze = g2.mobs.spawn(Mobs.BLAZE, 8.5, GROUND + 3, 22.5);
         int shots = 0;
         boolean burning = false;
         for (int i = 0; i < 500; i++) {
            g2.mobs.tick();
            shots = Math.max(shots, g2.mobs.fireballs.size());
            burning |= g2.player.burn > 0;
         }

         check(shots > 0, "the blaze shot");
         check(burning || g2.player.health < 20, "and the shots hurt (health " + g2.player.health + ", burning " + burning + ")");
         check(blaze.y > GROUND + 0.5, "it hovers");
         // blaze rods
         Game g3 = newGame(0);
         g3.mobs.rnd.setSeed(20123L + 784);
         Mobs.Mob b = g3.mobs.spawn(Mobs.BLAZE, 30.5, GROUND + 3, 30.5);
         java.util.HashSet<Integer> drops = new java.util.HashSet<>();
         for (int i = 0; i < 200; i++) {
            Mobs.Mob d = g3.mobs.spawn(Mobs.BLAZE, 40.5, GROUND + 1, 40.5);
            g3.mobs.hurtMob(d, 100, 0, 0, 0, false);
         }

         for (World.Item it : g3.world.items) {
            drops.add(it.id);
         }

         check(drops.contains(Items.BLAZE_ROD) && drops.size() == 1, "blazes drop blaze rods: " + drops);
         eq(Items.BLAZE_ROD, b.type == Mobs.BLAZE ? Items.BLAZE_ROD : 0, "a blaze");
         // ghast drops
         Game g4 = newGame(0);
         g4.mobs.rnd.setSeed(20123L + 799);
         java.util.HashSet<Integer> gd = new java.util.HashSet<>();
         for (int i = 0; i < 200; i++) {
            Mobs.Mob d = g4.mobs.spawn(Mobs.GHAST, 40.5, GROUND + 6, 40.5);
            g4.mobs.hurtMob(d, 100, 0, 0, 0, false);
         }

         for (World.Item it : g4.world.items) {
            gd.add(it.id);
         }

         check(gd.contains(Items.GHAST_TEAR) && gd.contains(Items.GUNPOWDER) && gd.size() == 2, "ghasts drop tears and gunpowder: " + gd);
      });

      test("magma cubes: three sizes, they hop at you, they burn nothing of theirs, and split into smaller ones when they die", () -> {
         Game g = newGame(0);
         g.mobs.rnd.setSeed(20123L + 814);
         Mobs.Mob big = g.mobs.spawn(Mobs.MAGMA_CUBE, 14.5, GROUND + 1, 8.5);
         eq(3, big.size, "a new one is large");
         eq(16, big.health, "16 health");
         check(big.bb[4] - big.bb[1] > 1.4 && big.bb[3] - big.bb[0] > 1.4, "a large one is a block and a half wide");
         Mobs.Mob small = g.mobs.spawn(Mobs.MAGMA_CUBE, 40.5, GROUND + 1, 40.5);
         Mobs312.setSize(small, 1);
         check(small.bb[3] - small.bb[0] < 0.6 && small.health == 2, "a small one is half a block");
         double y0 = big.y;
         double maxY = y0;
         int hp = g.player.health;
         for (int i = 0; i < 400; i++) {
            g.mobs.tick();
            maxY = Math.max(maxY, big.y);
         }

         check(maxY > y0 + 0.5, "it hops (highest " + (maxY - y0) + ")");
         check(g.player.health < hp, "and its touch hurts (health " + g.player.health + ")");
         int before = g.mobs.list.size();
         g.mobs.hurtMob(big, 100, 0, 0, 0, false);
         int mediums = 0;
         for (Mobs.Mob m : g.mobs.list) {
            if (m.type == Mobs.MAGMA_CUBE && m.size == 2 && !m.dead) {
               mediums++;
            }
         }

         check(mediums >= 2 && mediums <= 3, "a large one splits into 2 or 3 medium ones (" + mediums + ")");
         Mobs.Mob med = null;
         for (Mobs.Mob m : g.mobs.list) {
            if (m.type == Mobs.MAGMA_CUBE && m.size == 2 && !m.dead) {
               med = m;
            }
         }

         g.mobs.hurtMob(med, 100, 0, 0, 0, false);
         boolean smalls = false;
         for (Mobs.Mob m : g.mobs.list) {
            smalls |= m.type == Mobs.MAGMA_CUBE && m.size == 1 && !m.dead && m != small;
         }

         check(smalls, "a medium one splits into small ones");
         int cubes = 0;
         g.mobs.hurtMob(small, 100, 0, 0, 0, false);
         for (Mobs.Mob m : g.mobs.list) {
            if (m.type == Mobs.MAGMA_CUBE && m.size == 1 && m.dead && m == small) {
               cubes++;
            }
         }

         eq(1, cubes, "a small one just dies");
      });

      test("the Nether fills with its own mobs, standing on something, never inside a wall", () -> {
         Game g = newGame(0);
         g.mobs.rnd.setSeed(20123L + 868);
         World ow = g.world;
         ow.shutdown();
         World w = new World(77L, null, Dim.NETHER);
         g.world = w;
         w.game = g;
         int[] fl = null;
         load(w, 0, 0, 3);
         NetherGen ng = (NetherGen)w.gen;
         for (int x = -8; x < 8 && fl == null; x++) {
            fl = ng.findFloor(x * 3, 0);
         }

         check(fl != null, "a floor to stand on in the middle");
         g.player.setPos(fl[0] + 0.5, fl[1], fl[2] + 0.5);
         java.util.HashSet<Integer> kinds = new java.util.HashSet<>();
         for (int i = 0; i < 400; i++) {
            Mobs312.spawnNether(g.mobs, w, g.player);
            if (g.mobs.count(true) > 20) {
               break;
            }
         }

         for (Mobs.Mob m : g.mobs.list) {
            kinds.add(m.type);
            check(m.type == Mobs.ZOMBIE_PIGMAN || m.type == Mobs.GHAST || m.type == Mobs.MAGMA_CUBE || m.type == Mobs.BLAZE || m.type == Mobs.SKELETON, "only nether mobs (" + Mobs.NAME[m.type] + ")");
            check(!Phys.collides(w, m.bb), Mobs.NAME[m.type] + " is not inside a block");
            double dx = m.x - g.player.x;
            double dz = m.z - g.player.z;
            check(Math.sqrt(dx * dx + dz * dz) >= 19.0, "not on top of the player");
         }

         check(g.mobs.list.size() > 4, "some mobs came (" + g.mobs.list.size() + ")");
         check(kinds.contains(Mobs.ZOMBIE_PIGMAN), "pigmen are the commonest: " + kinds);
         // and each tick of the world's mob logic runs without trouble in the Nether
         for (int i = 0; i < 100; i++) {
            g.mobs.tick();
         }

         w.shutdown();
      });
   }

   // ------------------------------------------------------------------ nether fortresses
   static void fortress() {
      test("Nether fortress: regions, shape, spawner and chests are where the plan says, every part can be walked to, chunks are repeatable", () -> {
         for (long seed : new long[]{12345L, 777L, 99L}) {
            NetherGen gen = new NetherGen(seed);
            int present = 0;
            NetherFortress.Plan best = null;
            for (int rz = -2; rz <= 2; rz++) {
               for (int rx = -2; rx <= 2; rx++) {
                  NetherFortress.Plan p = gen.fortress.plan(rx, rz);
                  check(p == gen.fortress.plan(rx, rz), "the plan is made once");
                  if (!p.present) {
                     continue;
                  }

                  present++;
                  check(p.minX >= rx * 256 && p.maxX < rx * 256 + 256 && p.minZ >= rz * 256 && p.maxZ < rz * 256 + 256, "a fortress stays inside its region " + rx + "," + rz + ": x " + p.minX + ".." + p.maxX + " z " + p.minZ + ".." + p.maxZ);
                  check(p.maxX - p.minX < 2 * NetherFortress.REACH + 12 && p.maxZ - p.minZ < 2 * NetherFortress.REACH + 12, "not too big");
                  check(p.oy >= 52 && p.oy < 64 && p.rooms >= 2 && p.hasThrone && !p.spawners.isEmpty() && !p.chests.isEmpty(), "every fortress has rooms, a throne room and chests");
                  if (best == null && p.maxX - p.minX > 100) {
                     best = p;
                  }
               }
            }

            check(present >= 8 && present <= 22, "about 70% of the regions have a fortress (" + present + " of 25), seed " + seed);
            check(best != null, "a big fortress to look at, seed " + seed);
            if (best == null) {
               continue;
            }

            // build every chunk of it
            int cx0 = Math.floorDiv(best.minX, 16) - 1;
            int cx1 = Math.floorDiv(best.maxX, 16) + 1;
            int cz0 = Math.floorDiv(best.minZ, 16) - 1;
            int cz1 = Math.floorDiv(best.maxZ, 16) + 1;
            Chunk[][] cs = new Chunk[cz1 - cz0 + 1][cx1 - cx0 + 1];
            int bricks = 0;
            for (int cz = cz0; cz <= cz1; cz++) {
               for (int cx = cx0; cx <= cx1; cx++) {
                  Chunk c = gen.generate(cx, cz);
                  cs[cz - cz0][cx - cx0] = c;
                  for (short b : c.blocks) {
                     if (b == Blocks.NETHER_BRICKS) {
                        bricks++;
                     }
                  }
               }
            }

            check(bricks > 1500, "a fortress of nether bricks: " + bricks);
            // the same chunk again is identical
            Chunk again = gen.generate(cx0 + 3, cz0 + 3);
            check(java.util.Arrays.equals(again.blocks, cs[3][3].blocks), "a chunk is generated the same every time");
            // what stands where
            for (int[] s : best.spawners) {
               eq(Blocks.SPAWNER, at(cs, cx0, cz0, s[0], s[1], s[2]), "the blaze spawner is there");
            }

            for (int[] ch : best.chests) {
               eq(Blocks.CHEST_LOOT, at(cs, cx0, cz0, ch[0], ch[1], ch[2]), "a loot chest is there");
            }

            // walk through it: breadth first through open air from the middle of the first hall
            eq(0, at(cs, cx0, cz0, best.ox, best.oy + 1, best.oz), "the middle of the central hall is open");
            Reach walk = new Reach(cs, cx0, cz0, best.minX, best.maxX, best.minZ, best.maxZ, best.oy, 12, best.ox, best.oy + 1, best.oz);
            for (int[] s : best.spawners) {
               check(walk.can(s[0], s[1] + 1, s[2]), "the cell above the spawner at " + s[0] + "," + s[2] + " can be reached on foot (seed " + seed + ")");
            }

            for (int[] ch : best.chests) {
               check(walk.can(ch[0], ch[1] + 1, ch[2]), "the cell above the chest at " + ch[0] + "," + ch[2] + " can be reached on foot (seed " + seed + ")");
            }

            int qt = walk.count;
            check(qt > 1500, "a big connected space: " + qt + " cells");
         }
      });

      test("Nether fortress: the spawner in the throne room makes blazes, guards stand on the bricks", () -> {
         Game g = newGame(0);
         g.mobs.rnd.setSeed(20123L + 991);
         g.world.shutdown();
         World w = new World(12345L, null, Dim.NETHER);
         g.world = w;
         w.game = g;
         NetherFortress.Plan best = null;
         for (int rx = -2; rx <= 2 && best == null; rx++) {
            NetherFortress.Plan p = ((NetherGen)w.gen).fortress.plan(rx, 0);
            if (p.present && !p.spawners.isEmpty()) {
               best = p;
            }
         }

         check(best != null, "a fortress with a spawner");
         int[] sp = best.spawners.get(0);
         int cx = Math.floorDiv(sp[0], 16);
         int cz = Math.floorDiv(sp[2], 16);
         load(w, cx, cz, 2);
         eq(Blocks.SPAWNER, w.getBlock(sp[0], sp[1], sp[2]), "the spawner is in the loaded world");
         g.player.setPos(sp[0] + 0.5, sp[1] + 1, sp[2] + 3.5);
         int blazes = 0;
         for (int i = 0; i < 40 && blazes == 0; i++) {
            g.mobs.tickSpawners(w, g.player);
            for (Mobs.Mob m : g.mobs.list) {
               if (m.type == Mobs.BLAZE) {
                  blazes++;
               }
            }
         }

         check(blazes > 0, "the spawner made a blaze");
         // guards: spawn attempts around the player in the fortress give blazes / skeletons when the floor is nether brick
         g.mobs.rnd.setSeed(4242L);
         int guards = 0;
         int onBricks = 0;
         for (int batch = 0; batch < 80 && guards == 0; batch++) {
            g.mobs.list.clear();
            Mobs312.spawnNether(g.mobs, w, g.player);
            for (Mobs.Mob m : g.mobs.list) {
               if (w.getBlock((int)Math.floor(m.x), (int)Math.floor(m.y) - 1, (int)Math.floor(m.z)) == Blocks.NETHER_BRICKS) {
                  onBricks++;
                  check(m.type == Mobs.SKELETON || m.type == Mobs.BLAZE || m.type == Mobs.ZOMBIE_PIGMAN, "only guards on the bricks, not a " + Mobs.NAME[m.type]);
                  if (m.type != Mobs.ZOMBIE_PIGMAN) {
                     guards++;
                  }
               }
            }
         }

         check(guards > 0, "guards turned up on the bricks (" + onBricks + " mobs stood on bricks)");
         w.shutdown();
      });

      test("Nether fortress: the chest holds nether loot", () -> {
         World w = new World(12345L, null, Dim.NETHER);
         load(w, 0, 0, 1);
         w.setBlock(0, 60, 0, Blocks.CHEST_LOOT);
         TileEntity t = w.getOrCreateTile(0, 60, 0);
         int n = 0;
         for (Stack s : t.slots) {
            if (s != null) {
               n++;
            }
         }

         check(n >= 3, "the chest has things in it: " + n);
         check(w.getBlock(0, 60, 0) == Blocks.CHEST, "opening makes it an ordinary chest");
         w.shutdown();
      });
   }

   /** The block at world position (x, y, z) in the grid of chunks cs, which starts at chunk (cx0, cz0). */
   static int at(Chunk[][] cs, int cx0, int cz0, int x, int y, int z) {
      int cx = Math.floorDiv(x, 16) - cx0;
      int cz = Math.floorDiv(z, 16) - cz0;
      if (cx < 0 || cz < 0 || cz >= cs.length || cx >= cs[0].length) {
         return Blocks.BEDROCK;
      }

      return cs[cz][cx].get(Math.floorMod(x, 16), y, Math.floorMod(z, 16));
   }

   /** Breadth first search through open air (id 0) inside a box of chunks: which cells can be walked to from a start cell? */
   static final class Reach {
      final boolean[] seen;
      final int minX;
      final int minZ;
      final int w;
      final int d;
      final int hgt;
      final int base;
      int count;

      Reach(Chunk[][] cs, int cx0, int cz0, int minX, int maxX, int minZ, int maxZ, int base, int hgt, int sx, int sy, int sz) {
         this.minX = minX;
         this.minZ = minZ;
         this.w = maxX - minX + 3;
         this.d = maxZ - minZ + 3;
         this.hgt = hgt;
         this.base = base;
         this.seen = new boolean[w * d * hgt];
         int[] queue = new int[w * d * hgt];
         int qh = 0;
         int qt = 0;
         int start = ((sx - minX + 1) * d + (sz - minZ + 1)) * hgt + (sy - base);
         queue[qt++] = start;
         seen[start] = true;
         int[][] nb = {{1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}, {0, 1, 0}, {0, -1, 0}};
         while (qh < qt) {
            int cur = queue[qh++];
            int yy = cur % hgt;
            int zz = (cur / hgt) % d;
            int xx = cur / hgt / d;
            for (int[] n : nb) {
               int nx = xx + n[0];
               int ny = yy + n[1];
               int nz = zz + n[2];
               if (nx < 0 || nz < 0 || ny < 0 || nx >= w || nz >= d || ny >= hgt) {
                  continue;
               }

               int key = (nx * d + nz) * hgt + ny;
               if (seen[key] || at(cs, cx0, cz0, nx + minX - 1, base + ny, nz + minZ - 1) != 0) {
                  continue;
               }

               seen[key] = true;
               queue[qt++] = key;
            }
         }

         this.count = qt;
      }

      boolean can(int x, int y, int z) {
         int ix = x - minX + 1;
         int iz = z - minZ + 1;
         int iy = y - base;
         return ix >= 0 && iz >= 0 && iy >= 0 && ix < w && iz < d && iy < hgt && seen[(ix * d + iz) * hgt + iy];
      }
   }

   // ------------------------------------------------------------------ strongholds
   static void stronghold() {
      test("strongholds: three rings around the middle, the same list every time, an eye of ender finds the nearest", () -> {
         Stronghold[] a = Stronghold.all(12345L);
         eq(19, a.length, "3 + 6 + 10 strongholds");
         Stronghold[] b = Stronghold.all(12345L);
         for (int i = 0; i < a.length; i++) {
            eq(a[i].cx, b[i].cx, "same x");
            eq(a[i].cz, b[i].cz, "same z");
            double dist = Math.sqrt((double)a[i].cx * a[i].cx + (double)a[i].cz * a[i].cz);
            if (i < 3) {
               check(dist > 630 && dist < 970, "ring 1 stronghold " + i + " is " + dist + " blocks out");
            } else if (i < 9) {
               check(dist > 1490 && dist < 2110, "ring 2 stronghold " + i + " is " + dist + " blocks out");
            } else {
               check(dist > 2790 && dist < 3410, "ring 3 stronghold " + i + " is " + dist + " blocks out");
            }
         }

         Stronghold other = Stronghold.all(99L)[0];
         check(other.cx != a[0].cx || other.cz != a[0].cz, "another seed, other places");
         Stronghold n = Stronghold.nearest(12345L, a[4].cx + 10.0, a[4].cz - 10.0);
         eq(4, n.index, "the nearest one to a place right next to #4");
      });

      test("stronghold: portal room with 12 frames around a lava pit, a spawner, a library, chests - and everything can be walked to", () -> {
         for (long seed : new long[]{12345L, 31L}) {
            Gen gen = new Gen(seed);
            for (int which = 0; which < 3; which++) {
               Stronghold sh = Stronghold.all(seed)[which];
               Stronghold.Plan p = sh.plan();
               check(p.oy >= 20 && p.oy < 32 && p.rooms >= 5 && p.hasLibrary && !p.chests.isEmpty(), "rooms, a library and chests");
               check(p.maxX - p.minX <= 2 * Stronghold.REACH + 12 && p.maxZ - p.minZ <= 2 * Stronghold.REACH + 12, "not too big");
               int cx0 = Math.floorDiv(p.minX, 16) - 1;
               int cx1 = Math.floorDiv(p.maxX, 16) + 1;
               int cz0 = Math.floorDiv(p.minZ, 16) - 1;
               int cz1 = Math.floorDiv(p.maxZ, 16) + 1;
               Chunk[][] cs = new Chunk[cz1 - cz0 + 1][cx1 - cx0 + 1];
               int bricks = 0;
               for (int cz = cz0; cz <= cz1; cz++) {
                  for (int cx = cx0; cx <= cx1; cx++) {
                     Chunk c = gen.generate(cx, cz);
                     cs[cz - cz0][cx - cx0] = c;
                     for (short blk : c.blocks) {
                        if (blk == Blocks.STONEBRICK) {
                           bricks++;
                        }
                     }
                  }
               }

               check(bricks > 1500, "a stronghold of stone bricks: " + bricks);
               int eyes = 0;
               eq(12, p.frames.size(), "twelve frames");
               for (int[] f : p.frames) {
                  int id = at(cs, cx0, cz0, f[0], f[1], f[2]);
                  check(id == Blocks.END_FRAME || id == Blocks.END_FRAME_EYE, "a frame at " + f[0] + "," + f[2] + " (" + id + ")");
                  if (id == Blocks.END_FRAME_EYE) {
                     eyes++;
                  }
               }

               check(eyes < 7, "some frames hold an eye but never all: " + eyes);
               for (int x = -1; x <= 1; x++) {
                  for (int z = -1; z <= 1; z++) {
                     eq(0, at(cs, cx0, cz0, p.ox + x, p.oy, p.oz + z), "the pit is open above the lava");
                     eq(Blocks.LAVA, at(cs, cx0, cz0, p.ox + x, p.oy - 1, p.oz + z), "lava in the pit");
                  }
               }

               int[] sp = p.spawners.get(0);
               eq(Blocks.SPAWNER, at(cs, cx0, cz0, sp[0], sp[1], sp[2]), "the spawner");
               int books = 0;
               for (int dz = 0; dz < cs.length; dz++) {
                  for (int dx = 0; dx < cs[0].length; dx++) {
                     for (short blk : cs[dz][dx].blocks) {
                        if (blk == Blocks.BOOKSHELF) {
                           books++;
                        }
                     }
                  }
               }

               check(books >= 20, "a library: " + books + " bookshelves");
               for (int[] ch : p.chests) {
                  eq(Blocks.CHEST_LOOT, at(cs, cx0, cz0, ch[0], ch[1], ch[2]), "a loot chest");
               }

               // from the portal room to everything
               eq(0, at(cs, cx0, cz0, p.ox + 4, p.oy + 1, p.oz), "the portal room is open");
               Reach walk = new Reach(cs, cx0, cz0, p.minX, p.maxX, p.minZ, p.maxZ, p.oy, 12, p.ox + 4, p.oy + 1, p.oz);
               for (int[] f : p.frames) {
                  check(walk.can(f[0], f[1] + 1, f[2]), "the cell above the frame at " + f[0] + "," + f[2] + " can be reached on foot (seed " + seed + ")");
               }

               for (int[] ch : p.chests) {
                  check(walk.can(ch[0], ch[1] + 1, ch[2]), "the cell above the chest at " + ch[0] + "," + ch[2] + " can be reached on foot (seed " + seed + ", stronghold " + which + ")");
               }

               check(walk.can(sp[0], sp[1] + 1, sp[2]), "the cell above the spawner can be reached");
               check(walk.count > 1500, "a big connected space: " + walk.count + " cells");
               // the same chunk twice is the same
               Chunk again = gen.generate(cx0 + 2, cz0 + 2);
               check(java.util.Arrays.equals(again.blocks, cs[2][2].blocks), "a chunk is generated the same every time");
            }
         }
      });
   }

   // ------------------------------------------------------------------ the End
   static Game endGame(long seed, File dir) throws Exception {
      Game g = newGame(0);
      g.mobs.rnd.setSeed(seed);
      g.world.shutdown();
      World w = new World(seed, dir, Dim.END);
      w.saveEnabled = dir != null;
      g.world = w;
      w.game = g;
      g.screen = Game.S.PLAY;
      load(w, 0, 0, 3);
      g.player.setMode(0);
      g.player.setPos(EndGen.PLATFORM_X + 0.5, EndGen.PLATFORM_Y + 1.0, EndGen.PLATFORM_Z + 0.5);
      return g;
   }

   static void end() {
      test("the End: one island in the void, the same in every world; ten obsidian pillars with bedrock on top; the fountain; the platform", () -> {
         EndGen a = new EndGen(1L);
         EndGen b = new EndGen(987654321L);
         check(java.util.Arrays.equals(a.generate(0, 0).blocks, b.generate(0, 0).blocks), "the same island whatever the seed");
         check(java.util.Arrays.equals(a.generate(-2, 1).blocks, b.generate(-2, 1).blocks), "the same island whatever the seed (a rim chunk)");
         int solid = 0;
         for (short blk : a.generate(9, 9).blocks) {
            if (blk != 0) {
               solid++;
            }
         }

         eq(0, solid, "nothing out at 150 blocks: the void");
         for (int deg = 0; deg < 360; deg += 15) {
            int x = (int)Math.round(Math.cos(Math.toRadians(deg)) * 44.0);
            int z = (int)Math.round(Math.sin(Math.toRadians(deg)) * 44.0);
            check(EndGen.groundTop(x, z) >= 56, "ground under the pillar ring at " + deg + " degrees: " + EndGen.groundTop(x, z));
            check(EndGen.groundTop((int)Math.round(Math.cos(Math.toRadians(deg)) * 75.0), (int)Math.round(Math.sin(Math.toRadians(deg)) * 75.0)) < 0, "the island ends before 75 blocks");
            check(EndGen.groundBottom(x, z) < EndGen.groundTop(x, z) - 2, "the island is thick here");
         }

         World w = new World(5L, null, Dim.END);
         load(w, 0, 0, 3);
         load(w, 3, 0, 2);
         eq(Blocks.END_STONE, w.getBlock(8, EndGen.TOP, 0), "end stone on top of the island");
         eq(Blocks.END_STONE, w.getBlock(8, EndGen.TOP - 5, 0), "end stone below the top");
         eq(0, w.getBlock(8, EndGen.TOP + 1, 0), "air above");
         java.util.TreeSet<Integer> heights = new java.util.TreeSet<>();
         for (int[] p : EndGen.PILLARS) {
            heights.add(p[2]);
            load(w, p[0] >> 4, p[1] >> 4, 1);
            eq(Blocks.BEDROCK, w.getBlock(p[0], p[2], p[1]), "bedrock on top of the pillar at " + p[0] + "," + p[1]);
            eq(Blocks.OBSIDIAN, w.getBlock(p[0], p[2] - 1, p[1]), "obsidian below it");
            eq(Blocks.OBSIDIAN, w.getBlock(p[0], p[2] - 12, p[1]), "obsidian further down");
            eq(0, w.getBlock(p[0], p[2] + 1, p[1]), "free air for the crystal");
            eq(Blocks.OBSIDIAN, w.getBlock(p[0] + p[3] - 1, p[2] - 1, p[1]), "as wide as it says");
            eq(0, w.getBlock(p[0] + p[3] + 2, p[2] - 1, p[1]), "and not wider");
         }

         eq(10, heights.size(), "ten different heights");
         eq(76, (int)heights.first(), "the lowest is 76");
         eq(103, (int)heights.last(), "the highest is 103");
         // the fountain
         eq(Blocks.BEDROCK, w.getBlock(0, EndGen.TOP, 0), "fountain floor");
         eq(Blocks.BEDROCK, w.getBlock(0, EndGen.TOP + 4, 0), "the bedrock pillar in the middle is four high");
         eq(0, w.getBlock(0, EndGen.TOP + 5, 0), "with the place for the egg on top");
         eq(Blocks.BEDROCK, w.getBlock(3, EndGen.TOP + 1, 0), "the rim");
         eq(Blocks.TORCH, w.getBlock(3, EndGen.TOP + 2, 0), "torches on the rim");
         eq(0, w.getBlock(1, EndGen.TOP + 1, 1), "the dish is empty where the portal will be");
         eq(Blocks.BEDROCK, w.getBlock(1, EndGen.TOP, 1), "and has a bedrock floor");
         for (int[] c : EndGen.exitPortalCells()) {
            eq(0, w.getBlock(c[0], c[1], c[2]), "a free cell for the exit portal");
         }

         // the platform you arrive on
         load(w, EndGen.PLATFORM_X >> 4, 0, 2);
         for (int dz = -2; dz <= 2; dz++) {
            for (int dx = -2; dx <= 2; dx++) {
               eq(Blocks.OBSIDIAN, w.getBlock(EndGen.PLATFORM_X + dx, EndGen.PLATFORM_Y, EndGen.PLATFORM_Z + dz), "platform obsidian at " + dx + "," + dz);
               eq(0, w.getBlock(EndGen.PLATFORM_X + dx, EndGen.PLATFORM_Y + 2, EndGen.PLATFORM_Z + dz), "room above the platform");
            }
         }

         check(w.getBlock(EndGen.PLATFORM_X, EndGen.PLATFORM_Y + 1, EndGen.PLATFORM_Z) == 0, "the place you arrive in is free");
         check(!w.gen.getClass().equals(Gen.class), "the End has a generator of its own");
         check(!Dim.hasSky(Dim.END) && Dim.ambient(Dim.END) > 0.1F, "no sky, but not pitch black");
         w.shutdown();
      });

      test("End portal frames: an eye in every one of the twelve and the pit fills with portal; not before, not with something in the pit", () -> {
         Game g = newGame(0);
         int y = GROUND + 1;
         int cx = 8;
         int cz = 8;
         for (int[] r : EndPortals.RING) {
            g.world.setBlock(cx + r[0], y, cz + r[1], Blocks.END_FRAME);
         }

         g.inv[0] = new Stack(Items.EYE_OF_ENDER, 12);
         g.sel = 0;
         g.player.setMode(0);
         for (int i = 0; i < 11; i++) {
            int[] r = EndPortals.RING[i];
            check(EndPortals.insertEye(g, cx + r[0], y, cz + r[1]), "eye " + i + " goes in");
            eq(Blocks.END_FRAME_EYE, g.world.getBlock(cx + r[0], y, cz + r[1]), "the frame has its eye");
            eq(0, count(g.world, cx - 1, y, cz - 1, cx + 1, y, cz + 1, Blocks.END_PORTAL), "no portal yet");
         }

         eq(1, g.countItem(Items.EYE_OF_ENDER), "eleven eyes used up");
         check(EndPortals.insertEye(g, cx + EndPortals.RING[0][0], y, cz + EndPortals.RING[0][1]), "a frame that has an eye takes no second one");
         eq(1, g.countItem(Items.EYE_OF_ENDER), "and nothing is used up");
         // the real thing: aim at the last frame and use it
         int[] last = EndPortals.RING[11];
         World.Hit h = new World.Hit();
         h.x = cx + last[0];
         h.y = y;
         h.z = cz + last[1];
         h.ny = 1;
         g.target = h;
         check(g.useBlock(), "right click on the last frame");
         eq(9, count(g.world, cx - 1, y, cz - 1, cx + 1, y, cz + 1, Blocks.END_PORTAL), "the pit is full of portal");
         eq(0, g.countItem(Items.EYE_OF_ENDER), "twelve eyes are gone");
         check(Blocks.END_PORTAL < Blocks.MAX && !Blocks.solid[Blocks.END_PORTAL], "you can walk into it");
         check(EndPortals.touching(g.world, new double[]{cx + 0.2, y, cz + 0.2, cx + 0.8, y + 1.8, cz + 0.8}) != null, "standing in it is noticed");
         check(EndPortals.touching(g.world, new double[]{cx + 5.2, y, cz + 5.2, cx + 5.8, y + 1.8, cz + 5.8}) == null, "standing beside it is not");
         // a ring with a block in the pit
         int cx2 = 24;
         for (int[] r : EndPortals.RING) {
            g.world.setBlock(cx2 + r[0], y, cz + r[1], Blocks.END_FRAME_EYE);
         }

         g.world.setBlock(cx2, y, cz, Blocks.STONE);
         eq(null, EndPortals.tryOpen(g.world, cx2 + 2, y, cz), "something in the pit: no portal");
         g.world.setBlock(cx2, y, cz, 0);
         int[] opened = EndPortals.tryOpen(g.world, cx2 + 2, y, cz);
         check(opened != null && opened[0] == cx2 && opened[1] == cz, "free again: it opens");
         // creative mode: the eye is not used up
         int cx3 = 8;
         int cz3 = -16;
         for (int[] r : EndPortals.RING) {
            g.world.setBlock(cx3 + r[0], y, cz3 + r[1], Blocks.END_FRAME);
         }

         g.player.setMode(1);
         g.inv[0] = new Stack(Items.EYE_OF_ENDER, 1);
         for (int[] r : EndPortals.RING) {
            EndPortals.insertEye(g, cx3 + r[0], y, cz3 + r[1]);
         }

         eq(9, count(g.world, cx3 - 1, y, cz3 - 1, cx3 + 1, y, cz3 + 1, Blocks.END_PORTAL), "creative: portal as well");
         eq(1, g.countItem(Items.EYE_OF_ENDER), "and the single eye is still there");
         check(Blocks.hardness[Blocks.END_FRAME] < 0.0F && Blocks.hardness[Blocks.END_FRAME_EYE] < 0.0F, "frames cannot be broken");
      });

      test("an Eye of Ender thrown into the air flies towards the nearest stronghold, rises, hovers, then drops - or breaks", () -> {
         Game g = newGame(0);
         g.player.setMode(0);
         g.player.setPos(8.5, GROUND + 1, 8.5);
         g.player.yaw = 0.0F;
         g.inv[0] = new Stack(Items.EYE_OF_ENDER, 40);
         g.sel = 0;
         g.world.dayTime = 1000;
         Stronghold s = Stronghold.nearest(g.world.seed, 8.5, 8.5);
         EndPortals.rnd.setSeed(11L);
         check(g.useSpecial(g.inv[0]), "the eye is thrown");
         eq(39, g.countItem(Items.EYE_OF_ENDER), "one is used up");
         World.Item it = null;
         for (World.Item i : g.world.items) {
            if (i.eye) {
               it = i;
            }
         }

         check(it != null, "an eye is in the air");
         double want = Math.atan2(s.cz - it.lz, s.cx - it.lx);
         double got = Math.atan2(it.az - it.lz, it.ax - it.lx);
         check(Math.abs(want - got) < 0.01, "it flies towards the stronghold at " + s.cx + "," + s.cz);
         check(Math.abs(Math.hypot(it.ax - it.lx, it.az - it.lz) - 12.0) < 0.01, "twelve blocks at a time");
         check(it.delay > 1000, "nobody can pick it up in the air");
         double top = it.y;
         for (int i = 0; i < 40 && it.eye; i++) {
            it.eyeT += 0;
            EndPortals.tickEye(g.world, it);
            top = Math.max(top, it.y);
         }

         check(top > it.ly + 5.0, "it rises about six blocks (" + (top - it.ly) + ")");
         for (int i = 0; i < 40 && it.eye; i++) {
            EndPortals.tickEye(g.world, it);
         }

         check(!it.eye, "the flight is over after about three seconds");
         check(it.dead || it.delay <= 10, "it either broke or is an item that can be picked up again");
         // a lot of throws: some of them break, most do not
         int broke = 0;
         int kept = 0;
         for (int n = 0; n < 40; n++) {
            World.Item e = g.world.dropItem(8.5, GROUND + 5.0, 8.5, Items.EYE_OF_ENDER, 1, 0);
            e.eye = true;
            e.lx = e.x;
            e.ly = e.y;
            e.lz = e.z;
            e.ax = e.x + 5.0;
            e.az = e.z;
            for (int i = 0; i < 70 && e.eye; i++) {
               EndPortals.tickEye(g.world, e);
            }

            if (e.dead) {
               broke++;
            } else {
               kept++;
            }
         }

         check(broke > 0 && kept > 20, "about one in five breaks: " + broke + " of 40");
         // in the Nether it just goes up and comes down again
         Game n = newGame(0);
         n.world.shutdown();
         World nw = new World(3L, null, Dim.NETHER);
         n.world = nw;
         nw.game = n;
         n.player.setMode(0);
         n.player.setPos(2.5, 70.0, 2.5);
         n.inv[0] = new Stack(Items.EYE_OF_ENDER, 3);
         n.sel = 0;
         check(n.useSpecial(n.inv[0]), "thrown in the Nether");
         World.Item ne = nw.items.get(0);
         check(Math.abs(ne.ax - ne.lx) < 0.01 && Math.abs(ne.az - ne.lz) < 0.01, "straight up: there are no strongholds here");
         nw.shutdown();
      });

      test("End portal travel: the Overworld portal leads to the platform, the exit leads home; /dim end; falling out of the world", () -> {
         File dir = java.nio.file.Files.createTempDirectory("endtrip").toFile();
         Game g = null;
         try {
            g = newGame(1);
            g.world.shutdown();
            World ow = new World(4321L, dir);
            ow.saveEnabled = true;
            for (int cz = -6; cz <= 6; cz++) {
               for (int cx = -6; cx <= 6; cx++) {
                  Chunk c = new Chunk(cx, cz);
                  for (int x = 0; x < 16; x++) {
                     for (int z = 0; z < 16; z++) {
                        c.set(x, 0, z, Blocks.BEDROCK);
                        for (int y = 1; y < GROUND; y++) {
                           c.set(x, y, z, Blocks.STONE);
                        }

                        c.set(x, GROUND, z, Blocks.GRASS);
                     }
                  }

                  c.computeHeightmap();
                  c.modified = true;
                  ow.chunks.put(World.key(cx, cz), c);
               }
            }

            for (Chunk c : new java.util.ArrayList<>(ow.chunks.values())) {
               ow.lightChunk(c);
            }

            ow.game = g;
            g.world = ow;
            g.rootDir = dir;
            g.screen = Game.S.PLAY;
            g.spawnXZ = new int[]{12, 12};
            int y = GROUND + 1;
            for (int dz = -1; dz <= 1; dz++) {
               for (int dx = -1; dx <= 1; dx++) {
                  ow.setBlock(30 + dx, y, 30 + dz, Blocks.END_PORTAL);
               }
            }

            g.player.setMode(0);
            g.player.setPos(30.5, y, 30.5);
            Portals.tickPlayer(g);
            eq(Dim.END, g.world.dim, "in the End at once");
            eq(Game.S.LOADING, g.screen, "behind the loading screen");
            check(g.arrivalKind == 2 && Math.abs(g.arrivalAt[0] - (EndGen.PLATFORM_X + 0.5)) < 0.01 && Math.abs(g.arrivalAt[2] - (EndGen.PLATFORM_Y + 1.0)) < 0.01, "the way in is the platform");
            load(g.world, EndGen.PLATFORM_X >> 4, 0, 2);
            g.finishArrival();
            g.screen = Game.S.PLAY;
            check(Math.abs(g.player.x - (EndGen.PLATFORM_X + 0.5)) < 0.01 && Math.abs(g.player.y - 64.0) < 0.01, "standing on the platform");
            check(!Phys.collides(g.world, g.player.bb), "not stuck in anything");
            check(g.world.fight != null, "the End has a fight");
            int crystals = 0;
            int dragons = 0;
            for (Mobs.Mob m : g.mobs.list) {
               if (m.type == Mobs.END_CRYSTAL) {
                  crystals++;
               }

               if (m.type == Mobs.ENDER_DRAGON) {
                  dragons++;
                  eq(200, m.health, "a dragon with all of its health");
               }
            }

            eq(10, crystals, "ten crystals");
            eq(1, dragons, "and one dragon");
            for (int i = 0; i < 100; i++) {
               Portals.tickPlayer(g);
            }

            eq(Dim.END, g.world.dim, "standing next to nothing: still there");
            // a second arrival does not bring a second dragon
            g.world.fight.enter(g);
            int again = 0;
            for (Mobs.Mob m : g.mobs.list) {
               if (m.type == Mobs.ENDER_DRAGON || m.type == Mobs.END_CRYSTAL) {
                  again++;
               }
            }

            eq(11, again, "no doubles");
            // falling into the void
            g.player.setPos(g.player.x, -20.0, g.player.z);
            g.voidCheck();
            check(g.player.dead, "the void kills");
            g.respawn();
            eq(Dim.OVERWORLD, g.world.dim, "dead in the End: home in the Overworld");
            load(g.world, 0, 0, 2);
            g.finishArrival();
            g.screen = Game.S.PLAY;
            check(Math.abs(g.player.x - 12.5) < 1.5 && Math.abs(g.player.z - 12.5) < 1.5, "at the world spawn (" + g.player.x + ", " + g.player.z + ")");
            // /dim end and /dim overworld
            g.runCommand("/dim end");
            eq(Dim.END, g.world.dim, "/dim end");
            check(g.arrivalKind == 2, "arrives on the platform");
            load(g.world, EndGen.PLATFORM_X >> 4, 0, 2);
            g.finishArrival();
            g.screen = Game.S.PLAY;
            g.runCommand("/dim overworld");
            eq(Dim.OVERWORLD, g.world.dim, "/dim overworld");
            check(g.arrivalKind == 1 && Math.abs(g.arrivalAt[0] - 12.5) < 0.01, "and home, not to a portal");
            // what was saved
            check(new File(dir, "DIM1").isDirectory() && new File(dir, "DIM1").list().length > 0, "the End has its own folder with chunks");
            check(new File(new File(dir, "DIM1"), "fight.properties").exists(), "and remembers the fight");
         } finally {
            if (g != null && g.world != null) {
               g.world.shutdown();
            }

            deleteAll(dir);
         }
      });

      test("endermen: calm until you look them in the face or hit them, then they blink and hit hard; water hurts them; pearls", () -> {
         Game g = newGame(0);
         g.mobs.rnd.setSeed(555L);
         g.player.setMode(0);
         g.player.setPos(8.5, GROUND + 1, 8.5);
         Mobs.Mob e = g.mobs.spawn(Mobs.ENDERMAN, 17.5, GROUND + 1, 8.5);
         // looking away
         g.player.yaw = 90.0F;
         g.player.pitch = 0.0F;
         for (int i = 0; i < 80; i++) {
            g.mobs.tick();
            g.player.health = 20;
         }

         eq(0, e.angry, "calm while you look somewhere else");
         e.setPos(17.5, GROUND + 1, 8.5);
         // looking at it: yaw towards +x is -90
         double dx = e.x - g.player.x;
         double dy = e.y + 2.55 - (g.player.y + g.player.eye);
         double d2 = Math.sqrt(dx * dx + (e.z - g.player.z) * (e.z - g.player.z));
         g.player.yaw = Mobs.angleTo(dx, e.z - g.player.z);
         g.player.pitch = (float)(-Math.toDegrees(Math.atan2(dy, d2)));
         check(Mobs312.lookedAt(g.mobs, g.world, g.player, e), "the head is in the middle of the view");
         g.player.pitch += 40.0F;
         check(!Mobs312.lookedAt(g.mobs, g.world, g.player, e), "looking 40 degrees too low: not looked at");
         g.player.pitch -= 40.0F;
         g.world.setBlock(12, GROUND + 1, 8, Blocks.STONE);
         g.world.setBlock(12, GROUND + 2, 8, Blocks.STONE);
         g.world.setBlock(12, GROUND + 3, 8, Blocks.STONE);
         g.world.setBlock(12, GROUND + 4, 8, Blocks.STONE);
         check(!Mobs312.lookedAt(g.mobs, g.world, g.player, e), "a wall in between: not looked at");
         for (int y = GROUND + 1; y <= GROUND + 4; y++) {
            g.world.setBlock(12, y, 8, 0);
         }

         int hp = g.player.health;
         boolean angry = false;
         for (int i = 0; i < 400 && g.player.health == hp; i++) {
            g.mobs.tick();
            angry |= e.angry > 0;
         }

         check(angry, "it noticed");
         check(g.player.health < hp, "and it hit: " + g.player.health);
         // it blinks about when it is hurt
         g.player.health = 20;
         g.player.dead = false;
         Mobs.Mob b = g.mobs.spawn(Mobs.ENDERMAN, 20.5, GROUND + 1, 20.5);
         int jumps = 0;
         double lx = b.x;
         double lz = b.z;
         for (int i = 0; i < 40; i++) {
            b.invuln = 0;
            g.mobs.hurtMob(b, 1, 8.5, 8.5, 0.0, true);
            if (Math.hypot(b.x - lx, b.z - lz) > 2.0) {
               jumps++;
            }

            lx = b.x;
            lz = b.z;
         }

         check(jumps >= 3 && jumps <= 35, "it teleports when hit now and then: " + jumps + " of 40");
         check(b.angry > 0, "and is angry");
         check(!Phys.collides(g.world, b.bb), "never inside a block");
         // water
         for (int x = 28; x <= 34; x++) {
            for (int z = 28; z <= 34; z++) {
               g.world.setBlock(x, GROUND + 1, z, Blocks.WATER);
            }
         }

         Mobs.Mob swimmer = g.mobs.spawn(Mobs.ENDERMAN, 31.5, GROUND + 1, 31.5);
         int before = swimmer.health;
         for (int i = 0; i < 60; i++) {
            g.mobs.tick();
            g.player.health = 20;
         }

         check(swimmer.health < before || swimmer.dead, "water hurts an enderman (" + swimmer.health + " of " + before + ")");
         // pearls
         int pearls = 0;
         for (int i = 0; i < 30; i++) {
            Mobs.Mob d = g.mobs.spawn(Mobs.ENDERMAN, 40.5, GROUND + 1, 40.5);
            d.health = 1;
            g.mobs.hurtMob(d, 5, 40.0, 40.0, 0.0, false);
         }

         for (World.Item it : g.world.items) {
            if (it.id == Items.ENDER_PEARL) {
               pearls += it.count;
            }
         }

         check(pearls > 5 && pearls < 40, "about one pearl for every second one: " + pearls);
      });

      test("End crystals blow up when hit and hurt a dragon that is close; the dragon heals while one stands", () -> {
         Game g = endGame(2024L, null);
         g.world.fight.enter(g);
         Mobs.Mob dragon = g.world.fight.dragon(g.mobs);
         check(dragon != null, "a dragon");
         Mobs.Mob crystal = null;
         for (Mobs.Mob m : g.mobs.list) {
            if (m.type == Mobs.END_CRYSTAL && m.tag == 0) {
               crystal = m;
            }
         }

         check(crystal != null, "the crystal on pillar 0");
         int[] p = EndGen.PILLARS[0];
         check(Math.abs(crystal.x - (p[0] + 0.5)) < 0.01 && Math.abs(crystal.y - (p[2] + 1.0)) < 0.01, "it stands on the bedrock on top of the pillar");
         check(Mobs.hostile(Mobs.END_CRYSTAL) && Mobs312.persistent(Mobs.END_CRYSTAL) && Mobs312.persistent(Mobs.ENDER_DRAGON), "never despawn");
         // it heals while crystals stand
         g.player.setPos(EndGen.PLATFORM_X + 0.5, EndGen.PLATFORM_Y + 1.0, EndGen.PLATFORM_Z + 0.5);
         dragon.health = 100;
         for (int i = 0; i < 100; i++) {
            g.mobs.tick();
            g.player.health = 20;
         }

         check(dragon.health >= 108 && dragon.health <= 112, "+1 every half second: " + dragon.health);
         // a crystal is destroyed by any hit, and explodes
         crystal.health = 1;
         dragon.x = p[0] + 3.0;
         dragon.y = p[2];
         dragon.z = p[1] + 3.0;
         dragon.setPos(dragon.x, dragon.y, dragon.z);
         dragon.invuln = 0;
         int hpBefore = dragon.health;
         g.mobs.hurtMob(crystal, 1, 0.0, 0.0, 0.0, true);
         check(crystal.dead, "a single hit is enough");
         check((g.world.fight.gone & 1) != 0, "the fight remembers it");
         check(dragon.health <= hpBefore - 8, "the dragon next to it was hurt by the blast: " + hpBefore + " -> " + dragon.health);
         eq(Blocks.BEDROCK, g.world.getBlock(p[0], p[2], p[1]), "the bedrock under it is still there");
         eq(Blocks.OBSIDIAN, g.world.getBlock(p[0], p[2] - 1, p[1]), "and the pillar");
         for (int i = 0; i < 25; i++) {
            g.mobs.tick();
            g.player.health = 20;
         }

         int left = 0;
         for (Mobs.Mob m : g.mobs.list) {
            if (m.type == Mobs.END_CRYSTAL && !m.dead) {
               left++;
            }
         }

         eq(9, left, "nine left");
         check(!g.mobs.list.contains(crystal), "and the exploded one is gone from the list at once");
         // no crystals: no healing
         for (Mobs.Mob m : new java.util.ArrayList<>(g.mobs.list)) {
            if (m.type == Mobs.END_CRYSTAL && !m.dead) {
               m.health = 1;
               g.mobs.hurtMob(m, 1, 0.0, 0.0, 0.0, true);
            }
         }

         eq(1023, g.world.fight.gone, "all ten are gone");
         dragon.health = 60;
         for (int i = 0; i < 100; i++) {
            g.mobs.tick();
            g.player.health = 20;
         }

         check(dragon.health == 60, "it stays as it is: " + dragon.health);
         g.world.shutdown();
      });

      test("the Ender Dragon: circles, swoops with its breath, rams, lands on the fountain, takes off again; killing it opens the exit portal and lays the egg", () -> {
         File dir = java.nio.file.Files.createTempDirectory("dragon").toFile();
         Game g = null;
         try {
            g = endGame(99L, dir);
            load(g.world, 0, 0, 3);
            g.world.fight.enter(g);
            Mobs.Mob d = g.world.fight.dragon(g.mobs);
            d.health = 200;
            boolean[] phases = new boolean[5];
            boolean breath = false;
            double maxR = 0;
            double minY = 999;
            double maxY = 0;
            for (int i = 0; i < 4500; i++) {
               g.mobs.tick();
               g.player.health = 20;
               g.player.invuln = 0;
               phases[d.phase] = true;
               if (d.phase == Dragon.PERCH) {
                  check(Math.abs(d.x - 0.5) < 1.7 && Math.abs(d.z - 0.5) < 1.7 && Math.abs(d.y - (EndGen.TOP + 2.0)) < 1.7, "perched on the fountain (" + d.x + ", " + d.y + ", " + d.z + ")");
               } else if (d.phase == Dragon.CIRCLE) {
                  maxR = Math.max(maxR, Math.hypot(d.x, d.z));
                  minY = Math.min(minY, d.y);
                  maxY = Math.max(maxY, d.y);
               }

               for (Mobs312.Fireball f : g.mobs.fireballs) {
                  breath |= f.dragon;
               }

               if (d.dead) {
                  break;
               }
            }

            for (int i = 0; i < 5; i++) {
               check(phases[i], "the dragon was in phase " + i);
            }

            check(breath, "it breathed a purple fireball");
            check(maxR > 20.0 && maxR < 90.0, "circling at 20-90 blocks from the middle (after a swoop it spirals back in): " + maxR);
            check(minY > 60.0 && maxY < 110.0, "between 60 and 110 high: " + minY + " .. " + maxY);
            // it rams
            g.mobs.fireballs.clear();
            d.phase = Dragon.CIRCLE;
            d.phaseT = 0;
            g.player.setPos(d.x, d.y + 1.0, d.z);
            g.player.invuln = 0;
            g.player.health = 20;
            d.cd = 0;
            g.mobs.tick();
            check(g.player.health < 20, "it hurts whoever is inside it: " + g.player.health);
            // purple clouds hurt
            g.world.fight.addCloud(20.5, 70.0, 20.5);
            g.player.setPos(20.5, 70.0, 20.5);
            g.player.health = 20;
            g.player.invuln = 0;
            g.player.setMode(0);
            for (int i = 0; i < 25; i++) {
               g.world.fight.tick(g);
               g.player.invuln = 0;
            }

            check(g.player.health <= 18, "standing in the breath hurts: " + g.player.health);
            g.world.fight.clouds.clear();
            // it dies
            g.player.setPos(EndGen.PLATFORM_X + 0.5, EndGen.PLATFORM_Y + 1.0, EndGen.PLATFORM_Z + 0.5);
            g.player.health = 20;
            d.invuln = 0;
            g.mobs.hurtMob(d, 500, 0.0, 0.0, 0.0, true);
            check(d.dead, "dead");
            check(g.world.fight.defeated, "the fight is won");
            eq(0, count(g.world, -1, EndGen.TOP + 1, -1, 1, EndGen.TOP + 1, 1, Blocks.END_PORTAL), "the portal is not there yet while it dies");
            for (int i = 0; i < Dragon.DEATH_TICKS - 2; i++) {
               g.mobs.tick();
               g.player.health = 20;
            }

            check(g.mobs.list.contains(d), "it takes its time to die");
            for (int i = 0; i < 4; i++) {
               g.mobs.tick();
            }

            check(!g.mobs.list.contains(d), "and then it is gone");
            eq(8, count(g.world, -1, EndGen.TOP + 1, -1, 1, EndGen.TOP + 1, 1, Blocks.END_PORTAL), "the exit portal rings the bedrock pillar");
            eq(Blocks.DRAGON_EGG, g.world.getBlock(0, EndGen.TOP + 5, 0), "the egg sits on the pillar");
            check(g.toastTicks > 0 && g.toastTitle.contains("Dragon"), "a message on the screen");
            // standing in the exit: home
            g.spawnXZ = new int[]{12, 12};
            g.player.setPos(1.5, EndGen.TOP + 1.0, 0.5);
            check(EndPortals.touching(g.world, g.player.bb) != null, "in the exit portal");
            Portals.tickPlayer(g);
            eq(Dim.OVERWORLD, g.world.dim, "the exit leads home");
            // the End remembers: no dragon next time, the portal is there
            World again = new World(99L, dir, Dim.END);
            check(again.fight.defeated && again.fight.egg, "saved: the dragon is dead, the egg has been laid");
            again.shutdown();
         } finally {
            if (g != null && g.world != null) {
               g.world.shutdown();
            }

            deleteAll(dir);
         }
      });
   }

   // ------------------------------------------------------------------ villages
   static Structs.Village firstVillage(Gen gen, int minFarms) {
      for (int rx = -2; rx <= 2; rx++) {
         for (int rz = -2; rz <= 2; rz++) {
            Structs.Village v = gen.structs.village(rx, rz);
            if (v.valid) {
               int farms = 0;
               for (Structs.Bld b : v.blds) {
                  if (b.type == 3) {
                     farms++;
                  }
               }

               if (farms >= minFarms) {
                  return v;
               }
            }
         }
      }

      return null;
   }

   static void villages() {
      test("villages: farms with crops and water, a smithy, lamp posts - and none of it when the extras are off (the old villages stay as they were)", () -> {
         boolean was = Gen.extras;
         try {
            Gen.extras = true;
            Gen gen = new Gen(12345L);
            Structs.Village v = firstVillage(gen, 2);
            check(v != null, "a village with farms");
            int farms = 0;
            int smithies = 0;
            for (Structs.Bld b : v.blds) {
               if (b.type == 3) {
                  farms++;
                  check(b.w == 9 && b.d == 7 && b.y0 >= 64, "a farm of 9 x 7 on dry land");
                  for (Structs.Bld o : v.blds) {
                     if (o != b) {
                        check(!(b.bx1 + 2 >= o.bx0 && b.bx0 - 2 <= o.bx1 && b.bz1 + 2 >= o.bz0 && b.bz0 - 2 <= o.bz1), "farms do not touch other buildings");
                     }
                  }
               } else if (b.type == 4) {
                  smithies++;
               }
            }

            check(farms >= 2 && farms <= 4, "two to four farms: " + farms);
            eq(1, smithies, "one smithy");
            check(v.lamps.size() >= 8, "lamp posts: " + v.lamps.size());
            // the same village without the extras
            Gen.extras = false;
            Gen strict = new Gen(12345L);
            Structs.Village old = strict.structs.village(v.cx >= 0 ? v.cx / 320 : -1 - (-v.cx - 1) / 320, v.cz >= 0 ? v.cz / 320 : -1 - (-v.cz - 1) / 320);
            check(old.valid && old.cx == v.cx && old.cz == v.cz, "the same village in the same place");
            eq(0, old.lamps.size(), "no lamps in the old villages");
            int oldBuildings = 0;
            for (Structs.Bld b : old.blds) {
               check(b.type != 3 && b.type != 4, "no new building types");
               oldBuildings++;
            }

            check(oldBuildings + farms == v.blds.size(), "the new village has all the old buildings and the farms: " + oldBuildings + " + " + farms + " = " + v.blds.size());
            for (int i = 0; i < old.blds.size(); i++) {
               Structs.Bld a = old.blds.get(i);
               Structs.Bld b = v.blds.get(i);
               check(a.x0 == b.x0 && a.z0 == b.z0 && a.w == b.w && a.d == b.d && a.dir == b.dir && a.y0 == b.y0, "building " + i + " is where it was");
            }
         } finally {
            Gen.extras = was;
         }
      });

      test("a farm in the generated world: wet farmland, crops, a channel of water, a fence with a gate, torches; the lamp posts", () -> {
         boolean was = Gen.extras;
         try {
            Gen.extras = true;
            Gen gen = new Gen(12345L);
            Structs.Village v = firstVillage(gen, 1);
            Structs.Bld f = null;
            for (Structs.Bld b : v.blds) {
               if (b.type == 3) {
                  f = b;
               }
            }

            int cx0 = Math.floorDiv(f.bx0 - 2, 16);
            int cx1 = Math.floorDiv(f.bx1 + 2, 16);
            int cz0 = Math.floorDiv(f.bz0 - 2, 16);
            int cz1 = Math.floorDiv(f.bz1 + 2, 16);
            Chunk[][] cs = new Chunk[cz1 - cz0 + 1][cx1 - cx0 + 1];
            for (int cz = cz0; cz <= cz1; cz++) {
               for (int cx = cx0; cx <= cx1; cx++) {
                  cs[cz - cz0][cx - cx0] = gen.generate(cx, cz);
               }
            }

            int wet = 0;
            int crops = 0;
            int ripe = 0;
            int water = 0;
            int fence = 0;
            int torches = 0;
            for (int x = f.bx0; x <= f.bx1; x++) {
               for (int z = f.bz0; z <= f.bz1; z++) {
                  int ground = at(cs, cx0, cz0, x, f.y0, z);
                  if (ground == Blocks.FARMLAND_WET) {
                     wet++;
                  } else if (ground == Blocks.WATER) {
                     water++;
                  }

                  int above = at(cs, cx0, cz0, x, f.y0 + 1, z);
                  if (above >= Blocks.WHEAT && above <= Blocks.POTATO0 + 7) {
                     crops++;
                     if (above == Blocks.WHEAT + 7 || above == Blocks.CARROT0 + 7 || above == Blocks.POTATO0 + 7) {
                        ripe++;
                     }
                  } else if (above == Blocks.FENCE) {
                     fence++;
                  }

                  if (at(cs, cx0, cz0, x, f.y0 + 3, z) == Blocks.TORCH) {
                     torches++;
                  }
               }
            }

            check(wet >= 20, "wet farmland: " + wet);
            check(crops >= 20, "crops: " + crops);
            check(ripe >= 1, "some of them ripe: " + ripe);
            check(water >= 5, "a channel of water: " + water);
            check(fence >= 14, "a fence: " + fence);
            eq(4, torches, "a torch on every corner post");
            int lampsFound = 0;
            for (int[] lamp : v.lamps) {
               Chunk c = null;
               int lcx = Math.floorDiv(lamp[0], 16);
               int lcz = Math.floorDiv(lamp[1], 16);
               c = gen.generate(lcx, lcz);
               for (int y = 120; y > 50; y--) {
                  int id = c.get(lamp[0] - lcx * 16, y, lamp[1] - lcz * 16);
                  if (id == Blocks.TORCH && c.get(lamp[0] - lcx * 16, y - 1, lamp[1] - lcz * 16) == Blocks.FENCE && c.get(lamp[0] - lcx * 16, y - 3, lamp[1] - lcz * 16) == Blocks.FENCE) {
                     lampsFound++;
                     break;
                  }
               }

               if (lampsFound >= 3) {
                  break;
               }
            }

            check(lampsFound >= 3, "lamp posts of three fences and a torch: " + lampsFound);
         } finally {
            Gen.extras = was;
         }
      });

      test("villagers and iron golems appear in a village once (and are kept), a farmer is hit or a zombie comes near: they run; the golem fights", () -> {
         File dir = java.nio.file.Files.createTempDirectory("village").toFile();
         boolean was = Gen.extras;
         Game g = null;
         try {
            Gen.extras = true;
            g = newGame(0);
            g.world.shutdown();
            World w = new World(12345L, dir);
            w.saveEnabled = true;
            g.world = w;
            w.game = g;
            g.rootDir = dir;
            g.screen = Game.S.PLAY;
            g.mobs.rnd.setSeed(42L);
            Structs.Village v = firstVillage(w.gen, 1);
            load(w, v.cx >> 4, v.cz >> 4, 3);
            g.player.setMode(0);
            g.player.setPos(v.cx + 30.5, v.cy + 3, v.cz + 0.5);
            Mobs312.spawnVillagers(g.mobs, w, g.player);
            int villagers = 0;
            int golems = 0;
            for (Mobs.Mob m : g.mobs.list) {
               if (m.type == Mobs.VILLAGER) {
                  villagers++;
                  check(m.color >= 0 && m.color <= 5, "a profession");
                  check(Math.hypot(m.x - v.cx, m.z - v.cz) < 25.0, "inside the village");
                  check(!Phys.collides(w, m.bb), "not inside a block");
               } else if (m.type == Mobs.IRON_GOLEM) {
                  golems++;
               }
            }

            check(villagers >= 2 && villagers <= 6, "two to six villagers: " + villagers);
            check(golems <= 1, "at most one golem: " + golems);
            check(w.populated.size() == 1, "the village is marked");
            int before = g.mobs.list.size();
            Mobs312.spawnVillagers(g.mobs, w, g.player);
            eq(before, g.mobs.list.size(), "only once");
            // far away: they are not lost
            g.player.setPos(v.cx + 400.0, v.cy + 3, v.cz);
            for (int i = 0; i < 5; i++) {
               g.mobs.tick();
            }

            int still = 0;
            for (Mobs.Mob m : g.mobs.list) {
               if (m.type == Mobs.VILLAGER || m.type == Mobs.IRON_GOLEM) {
                  still++;
               }
            }

            eq(villagers + golems, still, "300 blocks away they are still in the list");
            g.mobs.save(w.dir);
            w.saveAll();
            check(new File(dir, "villages.dat").exists(), "villages.dat");
            g.mobs.clear();
            g.mobs.load(w.dir);
            int loaded = 0;
            for (Mobs.Mob m : g.mobs.list) {
               if (m.type == Mobs.VILLAGER) {
                  loaded++;
               }
            }

            eq(villagers, loaded, "saved and loaded again - however far you were");
            World again = new World(12345L, dir);
            check(again.populated.size() == 1, "the village is remembered as populated");
            again.shutdown();
            // behaviour: a zombie nearby makes a villager run away
            g.player.setPos(v.cx + 0.5, v.cy + 3, v.cz + 30.5);
            g.mobs.clear();
            load(w, v.cx >> 4, v.cz >> 4, 3);
            double gy = v.cy + 1;
            int sx = v.cx + 40;
            int sz = v.cz - 40;
            for (int x = sx - 6; x <= sx + 6; x++) {
               for (int z = sz - 6; z <= sz + 6; z++) {
                  for (int y = v.cy + 1; y <= v.cy + 8; y++) {
                     w.setBlock(x, y, z, 0);
                  }

                  w.setBlock(x, v.cy, z, Blocks.STONE);
               }
            }

            Mobs.Mob vil = g.mobs.spawn(Mobs.VILLAGER, sx + 0.5, gy, sz + 0.5);
            vil.hx = sx;
            vil.hz = sz;
            vil.hy = 1.0;
            Mobs.Mob z = g.mobs.spawn(Mobs.ZOMBIE, sx + 4.5, gy, sz + 0.5);
            double d0 = Math.hypot(vil.x - z.x, vil.z - z.z);
            g.player.setPos(sx + 0.5, gy, sz + 20.5);
            for (int i = 0; i < 40; i++) {
               g.mobs.tick();
               z.x = sx + 4.5;
               z.z = sz + 0.5;
               z.setPos(z.x, z.y, z.z);
            }

            check(Math.hypot(vil.x - z.x, vil.z - z.z) > d0 + 2.0 || Math.hypot(vil.x - sx, vil.z - sz) > 4.0, "the villager ran away from the zombie");
            g.mobs.list.remove(z);
            // a golem breaks a zombie
            Mobs.Mob gol = g.mobs.spawn(Mobs.IRON_GOLEM, sx - 3.5, gy, sz + 0.5);
            gol.hx = sx;
            gol.hz = sz;
            gol.hy = 1.0;
            Mobs.Mob zom = g.mobs.spawn(Mobs.ZOMBIE, sx + 3.5, gy, sz + 0.5);
            for (int i = 0; i < 200 && !zom.dead; i++) {
               g.mobs.tick();
               g.player.health = 20;
            }

            check(zom.dead, "the golem killed the zombie");
            check(gol.health == 100, "and was not hurt");
            // hurt a villager: it runs
            Mobs.Mob victim = g.mobs.spawn(Mobs.VILLAGER, sx + 0.5, gy, sz - 4.5);
            victim.hx = sx;
            victim.hz = sz;
            victim.hy = 1.0;
            g.mobs.hurtMob(victim, 1, sx + 0.5, sz - 2.5, 0.0, true);
            check(victim.flee > 0 && victim.angry == 0, "it runs, it does not fight back");
            check(!Mobs.hostile(Mobs.VILLAGER) && !Mobs.hostile(Mobs.IRON_GOLEM), "neither is a monster");
         } finally {
            Gen.extras = was;
            if (g != null && g.world != null) {
               g.world.shutdown();
            }

            deleteAll(dir);
         }
      });

      test("trading: right click on a villager opens its trades; click the goods to buy; the price comes out of the inventory; emeralds are the money", () -> {
         Game g = newGame(0);
         g.player.setMode(0);
         Mobs.Mob v = g.mobs.spawn(Mobs.VILLAGER, 10.5, GROUND + 1, 10.5);
         v.color = Trades.FARMER;
         check(g.useOnMob(v, null), "an empty hand is enough to talk to a villager");
         eq(Game.S.INV, g.screen, "the trade screen is open");
         eq(Menu.TRADE, g.menu.kind, "it is the trade menu");
         eq("Farmer", g.menu.title, "titled by the profession");
         eq(6, g.menu.trades.length, "six trades");
         eq(18 + 36, g.menu.slots.size(), "three slots a trade and the inventory");
         Menu.Slot buy = null;
         for (Menu.Slot s : g.menu.slots) {
            if (s.type == 7 && s.armorPiece == 0) {
               buy = s;
            }
         }

         check(buy != null && g.menu.stackIn(buy).id == Items.EMERALD, "the first trade gives an emerald");
         g.menu.click(buy, 0, false);
         eq(0, g.countItem(Items.EMERALD), "nothing to pay with: nothing happens");
         g.inv[0] = new Stack(Items.WHEAT, 40);
         g.menu.click(buy, 0, false);
         eq(22, g.countItem(Items.WHEAT), "18 wheat paid");
         eq(1, g.countItem(Items.EMERALD), "an emerald in return");
         g.menu.click(buy, 0, false);
         eq(4, g.countItem(Items.WHEAT), "again");
         eq(2, g.countItem(Items.EMERALD), "two emeralds");
         g.menu.click(buy, 0, false);
         eq(4, g.countItem(Items.WHEAT), "not enough wheat for a third");
         // buying with emeralds
         Menu.Slot bread = null;
         for (Menu.Slot s : g.menu.slots) {
            if (s.type == 7 && s.armorPiece == 3) {
               bread = s;
            }
         }

         g.menu.click(bread, 0, false);
         eq(1, g.countItem(Items.EMERALD), "an emerald for bread");
         eq(4, g.countItem(Items.BREAD), "four loaves");
         g.menu.close();
         // every profession has trades, the nitwit has none, and every trade is possible
         for (int p = 0; p < 5; p++) {
            Trades.Trade[] t = Trades.offers(p);
            check(t.length >= 4 && t.length <= 6, Trades.name(p) + " has " + t.length + " trades");
            for (Trades.Trade tr : t) {
               check(tr.an > 0 && tr.outN > 0 && Items.name(tr.out) != null && Items.name(tr.a) != null, Trades.name(p) + ": a complete trade");
               check(tr.a == Items.EMERALD || tr.out == Items.EMERALD, Trades.name(p) + ": emeralds on one side");
            }
         }

         eq(0, Trades.offers(Trades.NITWIT).length, "the nitwit has nothing to trade");
         // the smith sells iron tools for emeralds
         g.inv[0] = new Stack(Items.EMERALD, 20);
         Menu m = new Menu(g, Menu.TRADE, null, Trades.SMITH);
         Menu.Slot sword = null;
         for (Menu.Slot s : m.slots) {
            if (s.type == 7 && s.armorPiece == 3) {
               sword = s;
            }
         }

         int e0 = g.countItem(Items.EMERALD);
         m.click(sword, 0, false);
         eq(e0 - 7, g.countItem(Items.EMERALD), "an iron sword costs seven");
         check(g.countItem(Items.toolId(2, Items.C_SWORD)) == 1 && Items.name(Items.toolId(2, Items.C_SWORD)).equals("Iron Sword"), "and there it is: an iron sword");
         // creative: free
         g.player.setMode(1);
         int emeralds = g.countItem(Items.EMERALD);
         m.click(sword, 0, false);
         eq(emeralds, g.countItem(Items.EMERALD), "creative players pay nothing");
         // the priest sells an ender pearl
         boolean pearl = false;
         for (Trades.Trade tr : Trades.offers(Trades.PRIEST)) {
            pearl |= tr.out == Items.ENDER_PEARL;
         }

         check(pearl, "the priest sells ender pearls: the way to the End is open without a single enderman");
      });
   }

   // ------------------------------------------------------------------ parrots
   static void parrots() {
      test("parrots: id 17 is alive (N = 18), six hit points, five colours, a model of eleven boxes with a tinted body", () -> {
         eq(18, Mobs.N, "eighteen kinds of mob");
         eq("parrot", Mobs.NAME[Mobs.PARROT], "its name");
         eq(6, Mobs.HP[Mobs.PARROT], "six hit points");
         check(Mobs312.flies(Mobs.PARROT) && Mobs312.peaceful(Mobs.PARROT) && !Mobs.hostile(Mobs.PARROT), "it flies and is peaceful");
         MobRender.Model[] models = new MobRender.Model[Mobs.N];
         MobModels312.build(models);
         check(models[Mobs.PARROT] != null && models[Mobs.PARROT].parts.size() == 11, "a model with eleven parts");
         int tinted = 0;
         for (MobRender.Part p : models[Mobs.PARROT].parts) {
            if (p.color == MobModels312.PARROT_MARK) {
               tinted++;
            }
         }

         check(tinted >= 5, "feathers are tinted: " + tinted);
         eq(Mobs312.PARROT_COLORS, MobModels312.PARROT_RGB.length, "a colour for every kind");
         Game g = newGame(0);
         g.mobs.timer = 1;
         boolean[] seen = new boolean[5];
         for (int i = 0; i < 60; i++) {
            Mobs.Mob m = g.mobs.spawn(Mobs.PARROT, 8.5, GROUND + 3, 8.5);
            check(m.color >= 0 && m.color < 5, "a colour from 0 to 4");
            seen[m.color] = true;
            eq(6, m.health, "health");
            check(!m.tamed, "wild");
            g.mobs.list.remove(m);
         }

         for (int c = 0; c < 5; c++) {
            check(seen[c], "colour " + c + " turns up");
         }
      });

      test("parrots: a wild one flutters about in the air, never inside a block, sits down now and then and takes off again", () -> {
         Game g = newGame(0);
         g.mobs.timer = 1;
         g.mobs.rnd.setSeed(1L); // an unseeded generator made this test fail now and then (149 blocks flown in one CI run, one parrot out of 30 left the 5 x 5 chunks of the test world)
         g.player.setPos(30.5, GROUND + 1, 30.5);
         // a few trees to fly round: columns of logs with leaves on top
         for (int k = 0; k < 6; k++) {
            int x = 10 + k * 3;
            int z = 4 + k % 2 * 5;
            for (int y = 1; y <= 4; y++) {
               g.world.setBlock(x, GROUND + y, z, Blocks.LOG_OAK);
            }

            for (int dx = -1; dx <= 1; dx++) {
               for (int dz = -1; dz <= 1; dz++) {
                  g.world.setBlock(x + dx, GROUND + 5, z + dz, Blocks.LEAVES_OAK);
               }
            }
         }

         Mobs.Mob m = g.mobs.spawn(Mobs.PARROT, 12.5, GROUND + 4, 6.5);
         double travelled = 0.0;
         int landed = 0;
         int tookOff = 0;
         int last = Mobs312.P_FLY;
         double minY = 999;
         double maxY = -999;
         boolean inside = false;
         for (int i = 0; i < 6000; i++) {
            g.mobs.tickMob(m, g.world, g.player, 1.0f);
            travelled += Math.hypot(m.x - m.px, m.z - m.pz);
            minY = Math.min(minY, m.y);
            maxY = Math.max(maxY, m.y);
            inside |= Phys.collides(g.world, m.bb);
            if (m.phase == Mobs312.P_LAND && last != Mobs312.P_LAND) {
               landed++;
            } else if (m.phase == Mobs312.P_FLY && last == Mobs312.P_LAND) {
               tookOff++;
            }

            last = m.phase;
         }

         check(!inside, "never inside a block");
         check(travelled > 150.0, "it flew about: " + (int)travelled + " blocks in 5 minutes");
         check(minY >= GROUND + 0.9 && maxY < GROUND + 30, "between the ground and the sky: " + minY + " .. " + maxY);
         check(landed >= 2, "it sat down: " + landed + " times");
         check(tookOff >= 1, "and took off again: " + tookOff);
         check(!m.dead && m.health == 6, "alive and well");
      });

      test("parrots: seeds tame one in three (a seed is used up each time), then it stays with you, follows you and comes to you when you are far away", () -> {
         Game g = newGame(0);
         g.mobs.timer = 1;
         Mobs.Mob m = g.mobs.spawn(Mobs.PARROT, 12.5, GROUND + 2, 12.5);
         g.mobs.rnd.setSeed(7L);
         GameTests.hold(g, Items.SEEDS, 30);
         int used = 0;
         while (!m.tamed && used < 30) {
            check(g.useOnMob(m, g.inv[g.sel]), "a seed is taken");
            used++;
         }

         check(m.tamed && used >= 1 && used <= 15, "tamed after " + used + " seeds");
         eq(30 - used, g.inv[g.sel].n, "every seed was used");
         check(g.adv.has("husbandry/tame_an_animal"), "Best Friends Forever");
         int before = g.inv[g.sel].n;
         check(!g.useOnMob(m, g.inv[g.sel]) || !m.tamed, "(a tame parrot takes no more seeds)");
         eq(before, g.inv[g.sel].n, "so none are used");
         // a creative player keeps the seeds
         Game c = newGame(1);
         c.mobs.timer = 1;
         Mobs.Mob cm = c.mobs.spawn(Mobs.PARROT, 12.5, GROUND + 2, 12.5);
         c.mobs.rnd.setSeed(3L);
         GameTests.hold(c, Items.MELON_SEEDS, 5);
         for (int i = 0; i < 30 && !cm.tamed; i++) {
            c.useOnMob(cm, c.inv[c.sel]);
         }

         check(cm.tamed && c.inv[c.sel].n == 5, "creative: tamed, seeds kept");
         // following: 15 blocks away it comes closer and stays about
         g.player.setPos(8.5, GROUND + 1, 8.5);
         m.setPos(23.5, GROUND + 4, 8.5);
         m.px = m.x;
         m.pz = m.z;
         m.phase = Mobs312.P_FLY;
         double closest = 99;
         for (int i = 0; i < 300; i++) {
            g.mobs.tickMob(m, g.world, g.player, 1.0f);
            closest = Math.min(closest, Math.hypot(m.x - g.player.x, m.z - g.player.z));
         }

         check(closest < 6.0, "it came to me: " + closest);
         check(Math.hypot(m.x - g.player.x, m.z - g.player.z) < 9.0, "and stayed: " + Math.hypot(m.x - g.player.x, m.z - g.player.z));
         // far away: it is brought to the player at once
         m.setPos(g.player.x + 40.0, GROUND + 5, g.player.z);
         g.mobs.tickMob(m, g.world, g.player, 1.0f);
         check(Math.hypot(m.x - g.player.x, m.z - g.player.z) < 4.0, "40 blocks away: here again: " + Math.hypot(m.x - g.player.x, m.z - g.player.z));
         // a wild one is not tamed by an empty hand and does not sit on the shoulder
         Mobs.Mob wild = g.mobs.spawn(Mobs.PARROT, 14.5, GROUND + 2, 14.5);
         g.inv[g.sel] = null;
         check(!g.useOnMob(wild, null), "an empty hand does nothing to a wild parrot");
         check(wild.phase != Mobs312.P_SHOULDER, "it stays where it is");
      });

      test("parrots: a tame one sits on your shoulder (two fit), is not seen in first person, cannot be hit by you, and flies away when you sneak and click, when you are hurt and when you swim", () -> {
         Game g = newGame(0);
         g.mobs.timer = 1;
         Mobs.Mob a = g.mobs.spawn(Mobs.PARROT, 9.5, GROUND + 2, 8.5);
         Mobs.Mob b = g.mobs.spawn(Mobs.PARROT, 7.5, GROUND + 2, 8.5);
         Mobs.Mob c = g.mobs.spawn(Mobs.PARROT, 8.5, GROUND + 2, 10.5);
         a.tamed = true;
         b.tamed = true;
         c.tamed = true;
         g.inv[g.sel] = null;
         g.player.yaw = 0.0F;
         check(g.useOnMob(a, null), "the first goes up");
         check(g.useOnMob(b, null), "the second goes up");
         check(!g.useOnMob(c, null), "a third has no place");
         check(Mobs312.onShoulder(a) && Mobs312.onShoulder(b) && !Mobs312.onShoulder(c), "two on the shoulders");
         check(a.aux * b.aux < 0, "one on each side");
         for (int i = 0; i < 3; i++) {
            g.mobs.tickMob(a, g.world, g.player, 1.0f);
            g.mobs.tickMob(b, g.world, g.player, 1.0f);
         }

         for (Mobs.Mob m : new Mobs.Mob[]{a, b}) {
            double d = Math.hypot(m.x - g.player.x, m.z - g.player.z);
            check(Math.abs(d - 0.36) < 0.02, "on the shoulder: " + d + " blocks from the middle");
            check(Math.abs(m.y - (g.player.y + 1.4)) < 0.01, "at shoulder height: " + (m.y - g.player.y));
         }

         // the right one is on the right: yaw 0 looks +z, the right hand is on the -x side
         Mobs.Mob right = a.aux > 0 ? a : b;
         Mobs.Mob left = a.aux > 0 ? b : a;
         check(right.x < g.player.x - 0.3 && left.x > g.player.x + 0.3, "left and right: " + right.x + " / " + left.x);
         // turn east (yaw -90): the right shoulder is on the south side (+z)
         g.player.yaw = -90.0F;
         g.mobs.tickMob(right, g.world, g.player, 1.0f);
         check(right.z > g.player.z + 0.3 && Math.abs(right.x - g.player.x) < 0.05, "turned east: the right shoulder is on the south side");
         // you cannot aim at them
         g.player.yaw = 0.0F;
         g.mobs.tickMob(left, g.world, g.player, 1.0f);
         double[] t = new double[1];
         Mobs.Mob picked = g.mobs.pick(left.x, left.y + 0.4, left.z - 3.0, 0.0, 0.0, 1.0, 6.0, t);
         check(picked != left, "a parrot on a shoulder is not picked");
         // sneak + right click with an empty hand: the last one flies off
         g.player.sneaking = true;
         check(Mobs312.shoulderOff(g), "sneaking: one flies off");
         check(!Mobs312.onShoulder(b) || !Mobs312.onShoulder(a), "only one is left");
         g.player.sneaking = false;
         // hurt: the other falls off
         Mobs.Mob stay = Mobs312.onShoulder(a) ? a : b;
         g.mobs.tickMob(stay, g.world, g.player, 1.0f);
         check(Mobs312.onShoulder(stay), "still there");
         g.player.health -= 3;
         g.mobs.tickMob(stay, g.world, g.player, 1.0f);
         check(!Mobs312.onShoulder(stay), "you were hurt: it flies up");
         check(!Mobs312.shoulderOff(g), "none left on a shoulder");
         // swimming
         g.useOnMob(stay, null);
         check(Mobs312.onShoulder(stay), "back again");
         g.player.inWater = true;
         g.mobs.tickMob(stay, g.world, g.player, 1.0f);
         check(!Mobs312.onShoulder(stay), "in the water it flies off");
         g.player.inWater = false;
         // a sneaking player does not pick one up
         g.player.sneaking = true;
         check(!g.useOnMob(stay, null), "sneaking: it does not go on the shoulder");
         g.player.sneaking = false;
         // dying: all fall
         g.useOnMob(stay, null);
         g.player.dead = true;
         g.mobs.tickMob(stay, g.world, g.player, 1.0f);
         check(!Mobs312.onShoulder(stay), "dead players carry nothing");
      });

      test("parrots: tamed ones are saved and are never lost; wild ones leave with distance; a killed one drops feathers; they appear in forests only", () -> {
         File dir = java.nio.file.Files.createTempDirectory("parrot").toFile();
         try {
            Game g = newGame(0);
            g.mobs.timer = 1;
            Mobs.Mob a = g.mobs.spawn(Mobs.PARROT, 12.5, GROUND + 2, 12.5);
            a.tamed = true;
            a.color = 3;
            Mobs.Mob w = g.mobs.spawn(Mobs.PARROT, 14.5, GROUND + 2, 12.5);
            w.color = 1;
            g.mobs.save(dir);
            g.mobs.list.clear();
            g.mobs.load(dir);
            int tame = 0;
            int wild = 0;
            for (Mobs.Mob m : g.mobs.list) {
               if (m.type == Mobs.PARROT) {
                  if (m.tamed) {
                     tame++;
                     eq(3, m.color, "the colour of the tame one");
                  } else {
                     wild++;
                     eq(1, m.color, "the colour of the wild one");
                  }
               }
            }

            check(tame == 1 && wild == 1, "both are back: tame " + tame + " wild " + wild);
            // far away
            for (Mobs.Mob m : g.mobs.list) {
               m.x += 300.0;
               m.setPos(m.x, m.y, m.z);
            }

            g.mobs.tick();
            int left = 0;
            boolean tameLeft = false;
            for (Mobs.Mob m : g.mobs.list) {
               if (m.type == Mobs.PARROT) {
                  left++;
                  tameLeft |= m.tamed;
               }
            }

            check(left == 1 && tameLeft, "300 blocks away only the tame one is kept: " + left);
            // feathers
            g.mobs.list.clear();
            g.world.items.clear();
            Mobs.Mob k = g.mobs.spawn(Mobs.PARROT, 12.5, GROUND + 1, 12.5);
            g.mobs.hurtMob(k, 50, 11.0, 12.5, 0.4, true);
            int feathers = GameTests.countItems(g.world, Items.FEATHER);
            check(k.dead && feathers >= 1 && feathers <= 2, "feathers: " + feathers);
            // forests
            g.mobs.list.clear();
            g.mobs.rnd.setSeed(11L);
            int spawned = 0;
            for (int i = 0; i < 400; i++) {
               Mobs312.spawnParrots(g.mobs, g.world, g.player);
            }

            for (Mobs.Mob m : g.mobs.list) {
               if (m.type == Mobs.PARROT) {
                  spawned++;
               }
            }

            eq(0, spawned, "no parrots on a plain (biome 0 in the test world)");
            for (Chunk ch : g.world.chunks.values()) {
               java.util.Arrays.fill(ch.biome, (byte)3);
            }

            for (int i = 0; i < 400; i++) {
               Mobs312.spawnParrots(g.mobs, g.world, g.player);
            }

            for (Mobs.Mob m : g.mobs.list) {
               if (m.type == Mobs.PARROT) {
                  spawned++;
                  check(!Phys.collides(g.world, m.bb), "not inside a block");
               }
            }

            check(spawned >= 1 && spawned <= 4, "in a forest they come, but few: " + spawned);
         } finally {
            deleteAll(dir);
         }
      });
   }

   // ------------------------------------------------------------------ advancements
   static Game gameIn(int dim) throws Exception {
      Game g = newGame(0);
      g.world.shutdown();
      World w = new World(5L, null, dim);
      g.world = w;
      w.game = g;
      g.screen = Game.S.PLAY;
      load(w, 0, 0, 2);
      g.player.setMode(0);
      g.player.setPos(0.5, 80.0, 0.5);
      return g;
   }

   static void advancements() {
      test("advancements: the tree is sound (one root per tab, parents in the same tab, no two on one spot, every icon exists)", () -> {
         eq(Adv312.LIST.size(), Adv312.BY_ID.size(), "every id once");
         check(Adv312.LIST.size() >= 28, "a decent number of advancements: " + Adv312.LIST.size());
         int[] roots = new int[Adv312.TABS.length];
         java.util.HashSet<String> spots = new java.util.HashSet<>();
         java.util.HashSet<String> titles = new java.util.HashSet<>();
         for (Adv312.A a : Adv312.LIST) {
            check(titles.add(a.title), "title unique: " + a.title);
            check(a.tab >= 0 && a.tab < Adv312.TABS.length, "tab of " + a.id);
            check(spots.add(a.tab + ":" + a.col + ":" + a.row), a.id + " has a spot of its own");
            check(a.col >= 0 && a.col <= 7 && a.row >= 0 && a.row <= 4, a.id + " fits the screen: " + a.col + "," + a.row);
            check(a.id.startsWith(new String[]{"story/", "nether/", "end/", "adventure/", "husbandry/"}[a.tab]), a.id + " is named after its tab");
            check(a.icon > 0 && (Blocks.isBlockId(a.icon) ? Blocks.name[a.icon] != null : Items.name(a.icon) != null), a.id + " has an icon");
            check(a.title.length() <= 28 && a.desc.length() <= 60, a.id + " has short texts");
            if (a.parent == null) {
               roots[a.tab]++;
            } else {
               Adv312.A p = Adv312.BY_ID.get(a.parent);
               check(p != null && p.tab == a.tab, a.id + " has a parent in its tab");
               check(Adv312.LIST.indexOf(p) < Adv312.LIST.indexOf(a), a.id + " comes after its parent");
               check(p.col < a.col, a.id + " lies right of its parent");
            }
         }

         for (int t = 0; t < roots.length; t++) {
            eq(1, roots[t], "one root in tab " + t);
         }
      });

      test("advancements: made by what you carry, where you are and what you do; each only once; remembered in the world folder", () -> {
         File dir = java.nio.file.Files.createTempDirectory("adv").toFile();
         try {
            Game g = newGame(0);
            g.rootDir = dir;
            g.adv.poll(g);
            check(g.adv.has("story/root") && g.adv.has("adventure/root") && g.adv.has("husbandry/root"), "the roots come by themselves");
            check(!g.adv.has("story/mine_stone") && !g.adv.has("story/mine_diamond"), "and nothing else");
            eq(3, g.adv.count(), "three so far");
            g.adv.tick();
            check(g.adv.shown == null, "the roots come without a toast");
            // what you carry
            g.inv[0] = new Stack(Blocks.COBBLE, 3);
            g.inv[1] = new Stack(Items.IRON_INGOT, 1);
            g.inv[2] = new Stack(Items.toolId(1, Items.C_PICK), 1);
            g.adv.poll(g);
            check(g.adv.has("story/mine_stone") && g.adv.has("story/smelt_iron") && g.adv.has("story/upgrade_tools"), "cobblestone, an iron ingot, a stone pickaxe");
            check(!g.adv.has("story/iron_tools"), "an iron pickaxe is something else");
            g.inv[3] = new Stack(Items.toolId(2, Items.C_PICK), 1);
            g.inv[4] = new Stack(Items.DIAMOND, 2);
            g.inv[5] = new Stack(Items.LAVA_BUCKET, 1);
            g.inv[6] = new Stack(Blocks.OBSIDIAN, 4);
            g.inv[7] = new Stack(Items.BLAZE_ROD, 1);
            g.inv[8] = new Stack(Items.ENDER_PEARL, 1);
            g.inv[9] = new Stack(Items.BREAD, 1);
            g.inv[10] = new Stack(Blocks.DRAGON_EGG, 1);
            g.adv.poll(g);
            for (String id : new String[]{"story/iron_tools", "story/mine_diamond", "story/lava_bucket", "story/form_obsidian", "nether/obtain_blaze_rod", "end/pearl", "husbandry/bake_bread", "end/dragon_egg"}) {
               check(g.adv.has(id), id);
            }

            // armour: worn counts, iron and diamond are different
            check(!g.adv.has("story/obtain_armor") && !g.adv.has("story/diamond_armor"), "no armour yet");
            g.armor[1] = new Stack(Items.armorId(1, 1), 1);
            g.adv.poll(g);
            check(g.adv.has("story/obtain_armor") && !g.adv.has("story/diamond_armor"), "an iron chestplate is Suit Up");
            g.armor[3] = new Stack(Items.armorId(3, 3), 1);
            g.adv.poll(g);
            check(g.adv.has("story/diamond_armor"), "diamond boots are Cover Me with Diamonds");
            // new ones come once
            int before = g.adv.count();
            check(!g.adv.award(g, "story/mine_stone"), "a second time: nothing");
            check(!g.adv.award(g, "no/such"), "an unknown name: nothing");
            eq(before, g.adv.count(), "the count did not change");
            // the file
            File f = Adv312.file(dir);
            check(f.isFile(), "saved at once");
            Adv312 again = new Adv312();
            again.load(dir);
            check(again.done.equals(g.adv.done), "a new game reads the same advancements back");
            java.nio.file.Files.write(f.toPath(), "story/root\nnot/an/advancement\n\nend/root\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            again.load(dir);
            eq(2, again.count(), "unknown names in the file are ignored");
            again.load(new File(dir, "nothing-here"));
            eq(0, again.count(), "no file: no advancements");
            g.adv.revokeAll(g);
            eq(0, g.adv.count(), "revoked");
            again.load(dir);
            eq(0, again.count(), "and the file says so");
         } finally {
            deleteAll(dir);
         }
      });

      test("advancements: the Nether, the End, a village and a stronghold, and what you kill", () -> {
         Game n = gameIn(Dim.NETHER);
         n.adv.poll(n);
         check(n.adv.has("story/enter_the_nether") && n.adv.has("nether/root"), "entering the Nether");
         check(!n.adv.has("nether/fortress"), "but not a fortress yet");
         n.world.setBlock(3, 60, 3, Blocks.NETHER_BRICKS);
         n.player.setPos(3.5, 61.0, 3.5);
         n.adv.poll(n);
         check(n.adv.has("nether/fortress"), "standing on nether bricks");
         n.mobs.timer = 1;
         Mobs.Mob ghast = n.mobs.spawn(Mobs.GHAST, 8.5, 62, 8.5);
         Mobs.Mob zombie = n.mobs.spawn(Mobs.ZOMBIE, 9.5, 61, 8.5);
         Mobs.Mob cow = n.mobs.spawn(Mobs.COW, 10.5, 61, 8.5);
         n.adv.killed(n, cow);
         check(!n.adv.has("adventure/kill_a_mob"), "a cow is no monster");
         n.adv.killed(n, zombie);
         check(n.adv.has("adventure/kill_a_mob"), "a zombie is");
         check(!n.adv.has("nether/ghast"), "no ghast yet");
         n.adv.killed(n, ghast);
         check(n.adv.has("nether/ghast"), "a ghast");
         // hitting a monster to death does it (the real path)
         Game h = newGame(0);
         h.mobs.timer = 1;
         Mobs.Mob z = h.mobs.spawn(Mobs.ZOMBIE, 12.5, GROUND + 1, 12.5);
         h.mobs.hurtMob(z, 100, 11.0, 12.5, 0.4, true);
         check(z.dead && h.adv.has("adventure/kill_a_mob"), "killing a zombie by hand");
         Game e = gameIn(Dim.END);
         e.adv.poll(e);
         check(e.adv.has("story/enter_the_end") && e.adv.has("end/root") && e.adv.has("story/follow_ender_eye"), "entering the End");
         check(!e.adv.has("end/kill_dragon"), "the dragon is alive");
         e.world.fight.defeated = true;
         e.adv.poll(e);
         check(e.adv.has("end/kill_dragon"), "Free the End when the dragon is dead");
         // a village: stand in it
         Game v = newGame(0);
         Structs.Village vil = firstVillage(v.world.gen, 1);
         check(vil != null, "a village near the origin");
         v.player.setPos(vil.cx + 0.5, 90.0, vil.cz + 0.5);
         v.adv.poll(v);
         check(v.adv.has("adventure/village"), "A Peaceful Place");
         Game far = newGame(0);
         far.player.setPos(vil.cx + 100.5, 90.0, vil.cz + 100.5);
         far.adv.poll(far);
         check(!far.adv.has("adventure/village"), "not 140 blocks away");
         // a stronghold
         Stronghold sh = Stronghold.all(v.world.seed)[0];
         Stronghold.Plan plan = sh.plan();
         Game s = newGame(0);
         s.player.setPos(sh.cx + 0.5, plan.oy + 2.0, sh.cz + 0.5);
         s.adv.poll(s);
         check(s.adv.has("adventure/stronghold"), "Buried Castle");
         Game above = newGame(0);
         above.player.setPos(sh.cx + 0.5, 70.0, sh.cz + 0.5);
         above.adv.poll(above);
         check(!above.adv.has("adventure/stronghold"), "not on the surface above it");
      });

      test("advancements: trading, breeding, planting, sleeping-free ones, dyeing, setting concrete and the eye of ender are made where they happen", () -> {
         Game g = newGame(0);
         g.mobs.timer = 1;
         // a trade
         g.inv[0] = new Stack(Items.EMERALD, 5);
         check(Trades.buy(g, Trades.offers(Trades.SMITH)[2]), "bought");
         check(g.adv.has("adventure/trade"), "What a Deal!");
         // breeding
         Mobs.Mob a = g.mobs.spawn(Mobs.COW, 12.5, GROUND + 1, 12.5);
         Mobs.Mob b = g.mobs.spawn(Mobs.COW, 13.5, GROUND + 1, 12.5);
         g.mobs.breed(a, b);
         check(g.adv.has("husbandry/breed_an_animal"), "two cows have a calf");
         // planting
         g.world.setBlock(10, GROUND, 10, Blocks.FARMLAND);
         GameTests.hold(g, Items.SEEDS, 3);
         g.target = GameTests.top(10, 10);
         check(g.placeSeeds(g.inv[g.sel]), "planted");
         check(g.adv.has("husbandry/plant_seed"), "A Seedy Place");
         // a sheep gets a dye
         Mobs.Mob sheep = g.mobs.spawn(Mobs.SHEEP, 14.5, GROUND + 1, 14.5);
         sheep.color = 0;
         GameTests.hold(g, Color312.dyeItem(4), 1);
         check(g.useOnMob(sheep, g.inv[g.sel]) && g.adv.has("husbandry/dye_sheep"), "Color Me Impressed");
         // concrete powder sets
         check(!g.adv.has("story/concrete"), "not yet");
         g.world.setBlock(20, GROUND + 1, 20, Blocks.WATER);
         g.world.setBlock(21, GROUND + 1, 20, Blocks.POWDER0);
         for (int i = 0; i < 20; i++) {
            g.world.tick();
         }

         check(g.adv.has("story/concrete"), "Setting Concrete");
         // the eye of ender is thrown
         check(!g.adv.has("story/follow_ender_eye"), "no eye thrown yet");
         GameTests.hold(g, Items.EYE_OF_ENDER, 2);
         g.useSpecial(g.inv[g.sel]);
         check(g.adv.has("story/follow_ender_eye"), "Eye Spy");
      });

      test("advancements: the toast waits its turn, the commands grant and take back, the screen opens on L", () -> {
         Game g = newGame(0);
         g.adv.award(g, "story/mine_stone");
         g.adv.award(g, "story/smelt_iron");
         g.adv.award(g, "end/kill_dragon");
         check(g.adv.shown == null, "nothing shows before the first tick");
         g.adv.tick();
         eq("story/mine_stone", g.adv.shown.id, "the first one shows");
         for (int i = 0; i < Adv312.TOAST_TICKS; i++) {
            g.adv.tick();
         }

         eq("story/smelt_iron", g.adv.shown.id, "then the second");
         for (int i = 0; i < Adv312.TOAST_TICKS; i++) {
            g.adv.tick();
         }

         eq("end/kill_dragon", g.adv.shown.id, "then the third");
         eq(Adv312.CHALLENGE, g.adv.shown.frame, "a challenge");
         check(Adv312.header(Adv312.CHALLENGE).startsWith("Challenge") && Adv312.header(Adv312.TASK).startsWith("Advancement"), "headers");
         for (int i = 0; i < Adv312.TOAST_TICKS + 2; i++) {
            g.adv.tick();
         }

         check(g.adv.shown == null, "and the toast is gone");
         g.runCommand("/advancement grant all");
         eq(Adv312.LIST.size(), g.adv.count(), "/advancement grant all");
         g.runCommand("/advancement revoke all");
         eq(0, g.adv.count(), "/advancement revoke all");
         g.runCommand("/advancement grant story/mine_stone");
         check(g.adv.has("story/mine_stone") && g.adv.count() == 1, "/advancement grant <id>");
         g.runCommand("/advancement grant nonsense");
         eq(1, g.adv.count(), "a name that does not exist grants nothing");
         // per tab counts
         eq(1, g.adv.count(0), "one in the first tab");
         eq(0, g.adv.count(1), "none in the Nether tab");
         int sum = 0;
         for (int t = 0; t < Adv312.TABS.length; t++) {
            sum += Adv312.total(t);
         }

         eq(Adv312.LIST.size(), sum, "the tabs add up");
         // the L key
         g.screen = Game.S.PLAY;
         g.onKey(76, 1, 0);
         eq(Game.S.ADV, g.screen, "L opens the screen");
         g.onKey(76, 1, 0);
         eq(Game.S.PLAY, g.screen, "L closes it");
         g.onKey(76, 1, 0);
         g.onKey(256, 1, 0);
         eq(Game.S.PLAY, g.screen, "so does Escape");
      });
   }

   // ------------------------------------------------------------------ the World of Color
   static Stack craft(int w, int h, Stack... cells) {
      Stack[] g = new Stack[w * h];
      for (int i = 0; i < cells.length; i++) {
         g[i] = cells[i];
      }

      return Recipes.find(g, w, h);
   }

   static Stack s(int id) {
      return new Stack(id, 1);
   }

   static void colors() {
      test("colours: 16 colours of concrete, powder, terracotta, glazed terracotta and stained glass, 15 dyes, 6 blocks of metal and gem, emerald ore - registered, named, tiled", () -> {
         for (int c = 0; c < 16; c++) {
            int[] ids = {Blocks.CONCRETE0 + c, Blocks.POWDER0 + c, Blocks.STAINED0 + c, Blocks.GLAZED0 + c, Blocks.GLASS0 + c};
            for (int id : ids) {
               check(Blocks.isBlockId(id) && Blocks.name[id] != null && Blocks.name[id].startsWith(Color312.NAME[c]), "name of block " + id + ": " + Blocks.name[id]);
               check(Blocks.tiles[id][0] >= 400 && Blocks.tiles[id][0] < Tex.COLS * Tex.COLS, "tile of block " + id);
               check(Blocks.solid[id], "solid " + id);
            }

            check(!Blocks.opaque[Blocks.GLASS0 + c] && Blocks.opacity[Blocks.GLASS0 + c] == 0, "stained glass lets the light through");
            eq(0, Blocks.drop[Blocks.GLASS0 + c], "stained glass drops nothing");
            check(Blocks.opaque[Blocks.CONCRETE0 + c] && Blocks.opaque[Blocks.GLAZED0 + c], "concrete and terracotta are opaque");
         }

         for (int c = 1; c < 16; c++) {
            int id = Items.DYE1 + c - 1;
            check(Items.isItem(id) && Items.name[id] != null && Items.name[id].equals(Color312.NAME[c] + " Dye"), "dye " + c + ": " + Items.name[id]);
            eq(TexArt312.ITEM0 + id - Items.QUARTZ, Items.tile[id], "tile of dye " + c);
            check(TexArt312.sprite(id) != null && TexArt312.sprite(id).length == 256, "a picture for dye " + c);
            eq(c, Color312.dyeColor(id), "colour of dye " + c);
            eq(id, Color312.dyeItem(c), "item of colour " + c);
         }

         eq(0, Color312.dyeColor(Items.BONE_MEAL), "bone meal is the white dye");
         eq(-1, Color312.dyeColor(Items.COAL), "coal is no dye");
         eq(-1, Color312.dyeColor(Items.DYE1 + 15), "one past the last dye");
         // all tiles are distinct from one another and in the atlas
         java.util.HashSet<Integer> tiles = new java.util.HashSet<>();
         for (int id = Blocks.CONCRETE0; id <= Blocks.EMERALD_ORE; id++) {
            if (Blocks.name[id] != null) {
               check(tiles.add(Blocks.tiles[id][0]), "block " + id + " has a tile of its own");
            }
         }

         for (int id : new int[]{Blocks.TERRACOTTA, Blocks.CLAY, Blocks.IRON_BLOCK, Blocks.GOLD_BLOCK, Blocks.DIAMOND_BLOCK, Blocks.EMERALD_BLOCK, Blocks.COAL_BLOCK, Blocks.LAPIS_BLOCK, Blocks.EMERALD_ORE}) {
            check(Blocks.name[id] != null, "block " + id + " is named");
         }

         eq(Items.EMERALD, Blocks.drop[Blocks.EMERALD_ORE], "emerald ore drops emeralds");
         // the creative inventory offers all of them
         java.util.HashSet<Integer> offered = new java.util.HashSet<>();
         for (int id : Blocks.creativeList()) {
            offered.add(id);
         }

         for (int c = 0; c < 16; c++) {
            check(offered.contains(Blocks.CONCRETE0 + c) && offered.contains(Blocks.POWDER0 + c) && offered.contains(Blocks.STAINED0 + c)
               && offered.contains(Blocks.GLAZED0 + c) && offered.contains(Blocks.GLASS0 + c), "creative: colour " + c);
         }

         for (int c = 1; c < 16; c++) {
            check(offered.contains(Items.DYE1 + c - 1), "creative: dye " + c);
         }

         check(offered.contains(Blocks.CLAY) && offered.contains(Blocks.TERRACOTTA) && offered.contains(Blocks.EMERALD_ORE) && offered.contains(Blocks.LAPIS_BLOCK), "creative: the rest");
      });

      test("colours: the tiles have the colours they are named after", () -> {
         Color312.tiles();
         for (int c = 0; c < 16; c++) {
            int[] want = {Color312.CONCRETE[c] >> 16 & 255, Color312.CONCRETE[c] >> 8 & 255, Color312.CONCRETE[c] & 255};
            int[] got = avg(Color312.T_CONCRETE0 + c);
            for (int k = 0; k < 3; k++) {
               check(Math.abs(want[k] - got[k]) <= 6, "concrete " + Color312.NAME[c] + " channel " + k + ": want " + want[k] + " got " + got[k]);
            }

            int[] pow = avg(Color312.T_POWDER0 + c);
            check(Math.abs(pow[0] - got[0]) + Math.abs(pow[1] - got[1]) + Math.abs(pow[2] - got[2]) > 6 || c == 0, "powder differs from concrete: " + Color312.NAME[c]);
            int[] ter = avg(Color312.T_STAINED0 + c);
            int[] want2 = {Color312.TERRA[c] >> 16 & 255, Color312.TERRA[c] >> 8 & 255, Color312.TERRA[c] & 255};
            for (int k = 0; k < 3; k++) {
               check(Math.abs(want2[k] - ter[k]) <= 8, "terracotta " + Color312.NAME[c] + " channel " + k);
            }

            // every pixel of every tile is opaque (the translucency of glass comes from the mesher)
            for (int tile : new int[]{Color312.T_CONCRETE0 + c, Color312.T_POWDER0 + c, Color312.T_STAINED0 + c, Color312.T_GLAZED0 + c, Color312.T_GLASS0 + c}) {
               int x0 = Tex.tx(tile);
               int y0 = Tex.ty(tile);
               for (int y = 0; y < 16; y++) {
                  for (int x = 0; x < 16; x++) {
                     eq(255, Tex.atlasPixels[(y0 + y) * Tex.PX + x0 + x] >>> 24, "tile " + tile + " is opaque at " + x + "," + y);
                  }
               }
            }
         }

         // the 16 glazed patterns are all different from each other
         java.util.HashSet<String> seen = new java.util.HashSet<>();
         for (int c = 0; c < 16; c++) {
            check(seen.add(java.util.Arrays.toString(Color312.glazed(c))), "glazed pattern " + c + " is unique");
         }

         // a tile must have been painted where the dye sprite goes (the game blits every item that has a picture)
         int[] dye = Color312.dyeSprite(Items.DYE1);
         int solid = 0;
         for (int px : dye) {
            if (px >>> 24 != 0) {
               solid++;
            }
         }

         check(solid >= 40 && solid <= 200, "the dye sprite is a small heap: " + solid + " pixels");
      });

      test("colours: dye recipes (wool, terracotta, glass, concrete powder), mixing, sources of dye, metal blocks, the furnace", () -> {
         // wool
         Stack r = craft(2, 2, s(Blocks.WOOL0), s(Color312.dyeItem(14)));
         check(r != null && r.id == Blocks.WOOL0 + 14 && r.n == 1, "white wool + red dye = red wool");
         r = craft(3, 3, null, null, null, null, s(Blocks.WOOL0 + 3), null, null, s(Color312.dyeItem(5)), null);
         check(r != null && r.id == Blocks.WOOL0 + 5, "anywhere on the grid: light blue wool + lime dye = lime wool");
         r = craft(2, 2, s(Blocks.WOOL0), s(Items.BONE_MEAL));
         check(r == null, "white wool + bone meal (white dye): nothing happens");
         check(craft(2, 2, s(Blocks.WOOL0), s(Blocks.WOOL0 + 1)) == null, "two wools make nothing");
         check(craft(3, 3, s(Blocks.WOOL0), s(Color312.dyeItem(2)), s(Color312.dyeItem(3))) == null, "wool + two dyes make nothing");
         // terracotta: eight of any colour and a dye
         Stack[] ring = new Stack[9];
         for (int i = 0; i < 9; i++) {
            ring[i] = s(Blocks.TERRACOTTA);
         }

         ring[4] = s(Color312.dyeItem(11));
         r = Recipes.find(ring, 3, 3);
         check(r != null && r.id == Blocks.STAINED0 + 11 && r.n == 8, "8 terracotta + blue dye = 8 blue terracotta");
         ring[0] = s(Blocks.STAINED0 + 2);
         r = Recipes.find(ring, 3, 3);
         check(r != null && r.id == Blocks.STAINED0 + 11 && r.n == 8, "stained terracotta can be dyed again");
         // glass
         for (int i = 0; i < 9; i++) {
            ring[i] = s(Blocks.GLASS);
         }

         ring[4] = s(Color312.dyeItem(1));
         r = Recipes.find(ring, 3, 3);
         check(r != null && r.id == Blocks.GLASS0 + 1 && r.n == 8, "8 glass + orange dye = 8 orange stained glass");
         // concrete powder
         for (int i = 0; i < 9; i++) {
            ring[i] = s(i % 2 == 0 ? 7 : 8);
         }

         ring[4] = s(Color312.dyeItem(13));
         eq(5, craft(3, 3, ring[0], ring[1], ring[2], ring[3], null, ring[5], ring[6], ring[7], ring[8]) == null ? 5 : 6, "only 8 around a dye: 4 + 4 needed");
         for (int i = 0; i < 9; i++) {
            ring[i] = s(i < 4 ? 7 : 8);
         }

         ring[8] = s(Color312.dyeItem(13));
         r = Recipes.find(ring, 3, 3);
         check(r != null && r.id == Blocks.POWDER0 + 13 && r.n == 8, "4 sand + 4 gravel + green dye = 8 green concrete powder");
         ring[7] = s(7);
         check(Recipes.find(ring, 3, 3) == null, "5 sand + 3 gravel + a dye: nothing");
         // mixing
         r = craft(2, 2, s(Color312.dyeItem(14)), s(Color312.dyeItem(4)));
         check(r != null && r.id == Color312.dyeItem(1) && r.n == 2, "red + yellow = 2 orange");
         r = craft(2, 2, s(Color312.dyeItem(11)), s(Items.BONE_MEAL));
         check(r != null && r.id == Color312.dyeItem(3) && r.n == 2, "blue + bone meal = 2 light blue");
         r = craft(2, 2, s(Color312.dyeItem(11)), s(Color312.dyeItem(13)));
         check(r != null && r.id == Color312.dyeItem(9) && r.n == 2, "blue + green = 2 cyan");
         r = craft(2, 2, s(Color312.dyeItem(7)), s(Items.BONE_MEAL));
         check(r != null && r.id == Color312.dyeItem(8), "gray + white = light gray");
         // sources
         r = craft(1, 1, s(30));
         check(r != null && r.id == Color312.dyeItem(14) && r.n == 1, "poppy = red dye");
         r = craft(1, 1, s(31));
         check(r != null && r.id == Color312.dyeItem(4), "dandelion = yellow dye");
         r = craft(1, 1, s(Items.COAL));
         check(r != null && r.id == Color312.dyeItem(15), "coal = black dye");
         check(Recipes.smeltResult(s(28)) != null && Recipes.smeltResult(s(28)).id == Color312.dyeItem(13), "a cactus smelts to green dye");
         // metal blocks, and back
         Stack[] nine = new Stack[9];
         for (int i = 0; i < 9; i++) {
            nine[i] = s(Items.IRON_INGOT);
         }

         r = Recipes.find(nine, 3, 3);
         check(r != null && r.id == Blocks.IRON_BLOCK && r.n == 1, "9 iron ingots = a block of iron");
         r = craft(1, 1, s(Blocks.IRON_BLOCK));
         check(r != null && r.id == Items.IRON_INGOT && r.n == 9, "a block of iron = 9 ingots");
         for (int i = 0; i < 9; i++) {
            nine[i] = s(Color312.dyeItem(11));
         }

         r = Recipes.find(nine, 3, 3);
         check(r != null && r.id == Blocks.LAPIS_BLOCK, "9 blue dye = a lapis block");
         for (int i = 0; i < 9; i++) {
            nine[i] = s(Items.EMERALD);
         }

         r = Recipes.find(nine, 3, 3);
         check(r != null && r.id == Blocks.EMERALD_BLOCK, "9 emeralds = a block of emerald");
         // the furnace
         check(Recipes.smeltResult(s(Blocks.CLAY)).id == Blocks.TERRACOTTA, "clay smelts to terracotta");
         for (int c = 0; c < 16; c++) {
            check(Recipes.smeltResult(s(Blocks.STAINED0 + c)).id == Blocks.GLAZED0 + c, "stained terracotta smelts to glazed: " + c);
         }

         // the old recipes did not change
         r = craft(2, 2, s(5), s(5), s(5), s(5));
         check(r != null && r.id == 66, "4 planks still make a crafting table");
         r = craft(1, 1, s(Items.BONE));
         check(r != null && r.id == Items.BONE_MEAL, "a bone still makes bone meal");
      });

      test("concrete powder falls like sand and sets to concrete next to water, on its side, on top and after falling into water; plain sand does not", () -> {
         Game g = newGame(0);
         World w = g.world;
         int y = GROUND + 1;
         // 1. falls
         w.setBlock(5, y + 4, 5, Blocks.POWDER0 + 3);
         for (int i = 0; i < 60; i++) {
            w.tick();
         }

         eq(0, w.getBlock(5, y + 4, 5), "the powder left its place");
         eq(Blocks.POWDER0 + 3, w.getBlock(5, y, 5), "and lies on the ground, still powder");
         // 2. water arrives next to it: sets
         w.setBlock(6, y, 5, Blocks.WATER);
         for (int i = 0; i < 40; i++) {
            w.tick();
         }

         eq(Blocks.CONCRETE0 + 3, w.getBlock(5, y, 5), "water at its side: light blue concrete");
         // 3. placed next to water: sets at once (after one update)
         w.setBlock(10, y, 10, Blocks.WATER);
         w.setBlock(10, y, 11, Blocks.POWDER0 + 12);
         for (int i = 0; i < 10; i++) {
            w.tick();
         }

         eq(Blocks.CONCRETE0 + 12, w.getBlock(10, y, 11), "placed next to water: brown concrete");
         // 4. water above
         w.setBlock(12, y + 1, 12, Blocks.WATER);
         w.setBlock(12, y, 12, Blocks.POWDER0 + 15);
         for (int i = 0; i < 20; i++) {
            w.tick();
         }

         eq(Blocks.CONCRETE0 + 15, w.getBlock(12, y, 12), "water on top: black concrete");
         // 5. falls into a pool and sets
         for (int x = 14; x <= 16; x++) {
            for (int z = 14; z <= 16; z++) {
               w.setBlock(x, y, z, Blocks.WATER);
            }
         }

         w.setBlock(15, y + 6, 15, Blocks.POWDER0 + 1);
         for (int i = 0; i < 120; i++) {
            w.tick();
         }

         int concrete = count(w, 14, y - 1, 14, 16, y + 1, 16, Blocks.CONCRETE0 + 1);
         eq(1, concrete, "the powder that fell into the pool is orange concrete now");
         eq(0, count(w, 14, y - 1, 14, 16, y + 8, 16, Blocks.POWDER0 + 1), "and nothing is left of the powder");
         // 6. no water: it stays powder for as long as you like
         w.setBlock(-28, y, -28, Blocks.POWDER0 + 5);
         for (int i = 0; i < 400; i++) {
            w.tick();
         }

         eq(Blocks.POWDER0 + 5, w.getBlock(-28, y, -28), "dry powder stays powder");
         // 7. hard concrete does not fall
         w.setBlock(3, y + 5, 3, Blocks.CONCRETE0);
         for (int i = 0; i < 30; i++) {
            w.tick();
         }

         eq(Blocks.CONCRETE0, w.getBlock(3, y + 5, 3), "concrete hangs in the air like stone");
      });

      test("lapis ore gives 4 - 8 blue dye, emerald ore an emerald, stained glass nothing; a sheep takes a dye (and keeps it when it is sheared)", () -> {
         Game g = newGame(0);
         g.world.tick();
         int y = GROUND + 1;
         GameTests.hold(g, Items.toolId(2, Items.C_PICK), 1);
         int most = 0;
         int least = 99;
         for (int i = 0; i < 60; i++) {
            g.world.items.clear();
            g.world.setBlock(10, y, 10, 24);
            g.breakBlock(10, y, 10);
            int n = GameTests.countItems(g.world, Color312.dyeItem(11));
            most = Math.max(most, n);
            least = Math.min(least, n);
         }

         check(least >= 4 && most <= 8 && most > least, "lapis ore: " + least + ".." + most + " blue dye");
         g.world.items.clear();
         g.world.setBlock(10, y, 10, Blocks.EMERALD_ORE);
         g.breakBlock(10, y, 10);
         eq(1, GameTests.countItems(g.world, Items.EMERALD), "emerald ore: an emerald (with an iron pickaxe)");
         g.world.items.clear();
         g.world.setBlock(10, y, 10, Blocks.EMERALD_ORE);
         GameTests.hold(g, 0, 0);
         g.inv[g.sel] = null;
         g.breakBlock(10, y, 10);
         eq(0, GameTests.countItems(g.world, Items.EMERALD), "emerald ore: nothing by hand");
         g.world.items.clear();
         g.world.setBlock(10, y, 10, Blocks.GLASS0 + 4);
         g.breakBlock(10, y, 10);
         eq(0, g.world.items.size(), "stained glass drops nothing");
         // the sheep
         g.mobs.timer = 1;
         Mobs.Mob sheep = g.mobs.spawn(Mobs.SHEEP, 12.5, y, 12.5);
         sheep.color = 0;
         GameTests.hold(g, Color312.dyeItem(14), 3);
         check(g.useOnMob(sheep, g.inv[g.sel]), "the dye is used on the sheep");
         eq(14, sheep.color, "it is red now");
         eq(2, g.inv[g.sel].n, "one dye used");
         check(!g.useOnMob(sheep, g.inv[g.sel]), "a red sheep takes no red dye");
         eq(2, g.inv[g.sel].n, "so nothing is used");
         GameTests.hold(g, Items.BONE_MEAL, 1);
         check(g.useOnMob(sheep, g.inv[g.sel]) && sheep.color == 0, "bone meal is the white dye");
         GameTests.hold(g, Color312.dyeItem(9), 1);
         g.useOnMob(sheep, g.inv[g.sel]);
         eq(null, g.inv[g.sel], "the last dye is gone from the hand");
         eq(9, sheep.color, "cyan");
         GameTests.hold(g, Items.SHEARS, 1);
         g.world.items.clear();
         check(g.useOnMob(sheep, g.inv[g.sel]), "shears");
         check(GameTests.countItems(g.world, Blocks.WOOL0 + 9) >= 1, "cyan wool");
         // creative: the dye is not used up
         Game c = newGame(1);
         c.mobs.timer = 1;
         Mobs.Mob s2 = c.mobs.spawn(Mobs.SHEEP, 12.5, y, 12.5);
         GameTests.hold(c, Color312.dyeItem(2), 5);
         s2.color = 0;
         check(c.useOnMob(s2, c.inv[c.sel]) && s2.color == 2, "dyed in creative");
         eq(5, c.inv[c.sel].n, "creative keeps the dye");
         // every colour has a fleece
         for (int k = 0; k < 16; k++) {
            check(MobRender.SHEEP_RGB[k] != 0, "a fleece colour for " + k);
         }
      });

      test("emerald ore sits in the stone under mountains (and only there), clay in the beds of shallow water - decided by a stream of its own", () -> {
         int[] heights = new int[256];
         int[] biomes = new int[256];
         java.util.Arrays.fill(heights, 70);
         java.util.Arrays.fill(biomes, 6);
         int total = 0;
         for (int k = 0; k < 20; k++) {
            Chunk c = new Chunk(k, 4);
            for (int x = 0; x < 16; x++) {
               for (int z = 0; z < 16; z++) {
                  for (int y = 1; y <= 70; y++) {
                     c.set(x, y, z, 1);
                  }
               }
            }

            Color312.decorate(777L, c, heights, biomes);
            int n = 0;
            for (int x = 0; x < 16; x++) {
               for (int z = 0; z < 16; z++) {
                  for (int y = 0; y < 100; y++) {
                     if (c.get(x, y, z) == Blocks.EMERALD_ORE) {
                        n++;
                        check(y >= 4 && y < 32, "emerald ore deep down: y=" + y);
                     }
                  }
               }
            }

            check(n >= 1 && n <= 6, "a few emerald ores per mountain chunk: " + n);
            total += n;
            // the same chunk again: the same ore (no randomness from outside)
            Chunk again = new Chunk(k, 4);
            for (int x = 0; x < 16; x++) {
               for (int z = 0; z < 16; z++) {
                  for (int y = 1; y <= 70; y++) {
                     again.set(x, y, z, 1);
                  }
               }
            }

            Color312.decorate(777L, again, heights, biomes);
            check(java.util.Arrays.equals(c.blocks, again.blocks), "the same chunk twice: the same blocks");
            // other biomes: none
            Chunk plain = new Chunk(k, 4);
            for (int y = 1; y <= 70; y++) {
               for (int x = 0; x < 16; x++) {
                  for (int z = 0; z < 16; z++) {
                     plain.set(x, y, z, 1);
                  }
               }
            }

            java.util.Arrays.fill(biomes, 2);
            Color312.decorate(777L, plain, heights, biomes);
            java.util.Arrays.fill(biomes, 6);
            boolean any = false;
            for (int i = 0; i < plain.blocks.length; i++) {
               any |= plain.blocks[i] == Blocks.EMERALD_ORE;
            }

            check(!any, "no emerald ore outside the mountains");
         }

         check(total >= 40, "emerald ore in the end: " + total);
         // clay: a lake of depth 6 over sand
         int claySeen = 0;
         int[] bed = new int[256];
         java.util.Arrays.fill(bed, 56);
         java.util.Arrays.fill(biomes, 0);
         for (int k = 0; k < 60; k++) {
            Chunk c = new Chunk(k, -7);
            for (int x = 0; x < 16; x++) {
               for (int z = 0; z < 16; z++) {
                  for (int y = 1; y <= 56; y++) {
                     c.set(x, y, z, y > 53 ? 7 : 1);
                  }

                  for (int y = 57; y < 63; y++) {
                     c.set(x, y, z, Blocks.WATER);
                  }
               }
            }

            Color312.decorate(777L, c, bed, biomes);
            int n = 0;
            for (int x = 0; x < 16; x++) {
               for (int z = 0; z < 16; z++) {
                  for (int y = 0; y < 100; y++) {
                     if (c.get(x, y, z) == Blocks.CLAY) {
                        n++;
                        check(y >= 55 && y <= 56, "clay in the top layers of the bed: y=" + y);
                        check(c.get(x, y + 1, z) == Blocks.CLAY || Blocks.isWater(c.get(x, y + 1, z)), "water (or more clay) above the clay");
                     }
                  }
               }
            }

            check(n == 0 || n >= 6, "a patch, not a single block: " + n);
            claySeen += n > 0 ? 1 : 0;
         }

         check(claySeen >= 5 && claySeen <= 30, "about a quarter of the chunks with a lake bed get a patch: " + claySeen + " of 60");
         // dry land gets no clay
         Chunk dry = new Chunk(1, 1);
         int[] hills = new int[256];
         java.util.Arrays.fill(hills, 80);
         for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
               for (int y = 1; y <= 80; y++) {
                  dry.set(x, y, z, y > 77 ? 3 : 1);
               }
            }
         }

         for (int k = 0; k < 40; k++) {
            Chunk c = new Chunk(k, 9);
            for (int x = 0; x < 16; x++) {
               for (int z = 0; z < 16; z++) {
                  for (int y = 1; y <= 80; y++) {
                     c.set(x, y, z, y > 77 ? 3 : 1);
                  }
               }
            }

            Color312.decorate(777L, c, hills, biomes);
            boolean any = false;
            for (int i = 0; i < c.blocks.length; i++) {
               any |= c.blocks[i] == Blocks.CLAY;
            }

            check(!any, "no clay on dry land");
         }
      });

      test("stained glass is drawn translucent (blended pass), hides the faces between panes of one kind and shows the faces between two kinds", () -> {
         Game g = newGame(0);
         int y = GROUND + 1;
         Chunk c = g.world.chunk(0, 0);
         g.world.setBlock(5, y, 5, Blocks.GLASS0 + 2);
         Mesher.Result one = Mesher.build(g.world, c, y >> 4, false);
         int nT1 = one.nT;
         int nO1 = one.nO;
         free(one);
         g.world.setBlock(6, y, 5, Blocks.GLASS0 + 2);
         Mesher.Result two = Mesher.build(g.world, c, y >> 4, false);
         int nT2 = two.nT;
         free(two);
         g.world.setBlock(6, y, 5, Blocks.GLASS0 + 9);
         Mesher.Result mixed = Mesher.build(g.world, c, y >> 4, false);
         int nTm = mixed.nT;
         free(mixed);
         // the same section without the glass
         g.world.setBlock(5, y, 5, 0);
         g.world.setBlock(6, y, 5, 0);
         Mesher.Result none = Mesher.build(g.world, c, y >> 4, false);
         int nO0 = none.nO;
         free(none);
         eq(5, nT1 / 4, "one pane on the ground: five faces in the blended buffer");
         eq(8, nT2 / 4, "two panes of one kind: the faces between them are not drawn");
         eq(10, nTm / 4, "two kinds: the faces between them are drawn");
         eq(nO0, nO1, "glass adds nothing to the opaque buffer");
      });
   }

   static int[] avg(int tile) {
      int x0 = Tex.tx(tile);
      int y0 = Tex.ty(tile);
      long r = 0;
      long g = 0;
      long b = 0;
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            int p = Tex.atlasPixels[(y0 + y) * Tex.PX + x0 + x];
            r += p >> 16 & 255;
            g += p >> 8 & 255;
            b += p & 255;
         }
      }

      return new int[]{(int)(r / 256), (int)(g / 256), (int)(b / 256)};
   }

   // ------------------------------------------------------------------ small rules of the Nether
   static void netherRules() {
      test("rules: fire on netherrack never burns out (on stone it does); soul sand drags; a magma block burns your feet", () -> {
         Game g = newGame(0);
         g.world.fire.rnd.setSeed(21);
         g.world.setBlock(20, GROUND + 1, 20, Blocks.NETHERRACK);
         g.world.setBlock(30, GROUND + 1, 20, Blocks.STONE);
         check(g.world.fire.ignite(20, GROUND + 2, 20), "fire on netherrack");
         check(g.world.fire.ignite(30, GROUND + 2, 20), "fire on stone");
         int gone = Tests37.tickUntil(g.world, 6000, () -> g.world.getBlock(30, GROUND + 2, 20) != Blocks.FIRE);
         check(gone >= 0, "the fire on stone burned out");
         for (int i = 0; i < 6000; i++) {
            g.world.tick();
         }

         eq(Blocks.FIRE, g.world.getBlock(20, GROUND + 2, 20), "twelve thousand ticks later the fire on netherrack still burns");
         eq(Blocks.NETHERRACK, g.world.getBlock(20, GROUND + 1, 20), "and the netherrack is still there");

         // soul sand: the same walk covers far less ground
         double[] dist = new double[2];
         for (int k = 0; k < 2; k++) {
            Game h = newGame(0);
            for (int x = 0; x < 30; x++) {
               for (int z = 0; z < 30; z++) {
                  h.world.setBlock(x, GROUND, z, k == 0 ? Blocks.STONE : Blocks.SOUL_SAND);
               }
            }

            h.player.setPos(8.5, GROUND + 1, 8.5);
            h.player.yaw = 0.0F;
            for (int i = 0; i < 40; i++) {
               h.player.tick(h.world, 0.0f, 1.0f, false, false, false);
            }

            dist[k] = Math.hypot(h.player.x - 8.5, h.player.z - 8.5);
         }

         check(dist[0] > 3.0, "walks on stone: " + dist[0]);
         check(dist[1] < dist[0] * 0.6 && dist[1] > 0.3, "soul sand drags: " + dist[1] + " against " + dist[0]);

         // magma: standing costs health, sneaking does not, creative does not
         int[] hp = new int[3];
         for (int k = 0; k < 3; k++) {
            Game h = newGame(k == 2 ? 1 : 0);
            h.world.setBlock(8, GROUND, 8, Blocks.MAGMA);
            h.player.setPos(8.5, GROUND + 1, 8.5);
            h.player.food = 10; // no natural regeneration to hide the damage
            h.player.sat = 0.0F;
            for (int i = 0; i < 60; i++) {
               h.player.tick(h.world, 0.0f, 0.0f, false, k == 1, false);
            }

            hp[k] = h.player.health;
         }

         check(hp[0] <= 17 && hp[0] >= 13, "standing on magma: health " + hp[0]);
         eq(20, hp[1], "sneaking on magma is safe");
         eq(20, hp[2], "creative is safe");
      });
   }

   // ------------------------------------------------------------------ ender pearls
   static void pearls() {
      test("pearls: a right click throws one (not in adventure mode, one a second), creative keeps its stack", () -> {
         Game g = newGame(0);
         g.inv[g.sel] = new Stack(Items.ENDER_PEARL, 3);
         check(g.useSpecial(g.inv[g.sel]), "the first throw works");
         eq(2, g.inv[g.sel].n, "one pearl less");
         eq(1, g.mobs.fireballs.size(), "one pearl in the air");
         check(g.mobs.fireballs.get(0).pearl, "it is a pearl");
         check(!g.useSpecial(g.inv[g.sel]), "a second throw straight away is refused");
         eq(2, g.inv[g.sel].n, "nothing was used up");
         g.mobs.fireballs.clear();
         g.player.setMode(2);
         check(!g.useSpecial(g.inv[g.sel]), "no throwing in adventure mode");
         g.player.setMode(1);
         check(g.useSpecial(g.inv[g.sel]), "creative throws");
         eq(2, g.inv[g.sel].n, "creative keeps the stack");
         g.player.setMode(3);
         g.mobs.fireballs.clear();
         check(!g.useSpecial(g.inv[g.sel]), "spectators cannot throw");
      });

      test("pearls: it arcs down and you land where it did - 5 damage, no armour help, no fall damage, never inside a block", () -> {
         Game g = newGame(0);
         g.player.yaw = 0.0F;
         g.player.pitch = 0.0F;
         g.inv[g.sel] = new Stack(Items.ENDER_PEARL, 4);
         check(g.useSpecial(g.inv[g.sel]), "thrown");
         double startY = g.mobs.fireballs.get(0).y;
         int ticks = 0;
         double topY = startY;
         while (!g.mobs.fireballs.isEmpty() && ticks < 200) {
            Mobs312.tickFireballs(g.mobs, g.world, g.player);
            if (!g.mobs.fireballs.isEmpty()) {
               topY = Math.max(topY, g.mobs.fireballs.get(0).y);
            }

            ticks++;
         }

         check(ticks > 5 && ticks < 40, "it flew for a few ticks: " + ticks);
         check(topY <= startY + 0.01, "thrown level it only falls");
         check(g.player.z > 18.0 && g.player.z < 30.0, "you arrived ahead: z = " + g.player.z);
         check(Math.abs(g.player.x - 8.5) < 1.0, "straight ahead: x = " + g.player.x);
         check(g.player.y >= GROUND + 0.99 && g.player.y < GROUND + 2.5, "on the ground, not in it: y = " + g.player.y);
         eq(15, g.player.health, "5 damage");
         eq(0.0F, g.player.fallDistance, "no fall damage to come");
         g.player.updateBox();
         check(!Phys.collides(g.world, g.player.bb), "not inside a block");
      });

      test("pearls: a wall stops it, you stand in front of the wall; creative takes no damage; straight up comes back down to where you were", () -> {
         Game g = newGame(1);
         for (int y = GROUND + 1; y <= GROUND + 18; y++) {
            for (int x = -4; x <= 20; x++) {
               g.world.setBlock(x, y, 30, Blocks.STONE);
            }
         }

         g.player.yaw = 0.0F;
         g.player.pitch = -8.0F;
         g.inv[g.sel] = new Stack(Items.ENDER_PEARL, 2);
         check(g.useSpecial(g.inv[g.sel]), "thrown");
         for (int i = 0; i < 100 && !g.mobs.fireballs.isEmpty(); i++) {
            Mobs312.tickFireballs(g.mobs, g.world, g.player);
         }

         check(g.player.z > 26.5 && g.player.z <= 29.71, "in front of the wall: z = " + g.player.z);
         eq(20, g.player.health, "creative: no damage");
         g.player.updateBox();
         check(!Phys.collides(g.world, g.player.bb), "not inside the wall");

         g.player.setMode(0);
         g.player.setPos(8.5, GROUND + 1, 8.5);
         g.player.pitch = -90.0F;
         g.player.invuln = 5;
         check(g.useSpecial(g.inv[g.sel]), "thrown straight up");
         for (int i = 0; i < 400 && !g.mobs.fireballs.isEmpty(); i++) {
            Mobs312.tickFireballs(g.mobs, g.world, g.player);
         }

         check(Math.hypot(g.player.x - 8.5, g.player.z - 8.5) < 2.5, "back near where you were: " + g.player.x + " " + g.player.z);
         eq(15, g.player.health, "5 damage even straight after being hurt");
      });

      test("pearls: out of the loaded world or into the void nothing happens; a dead player is not moved; no spot, no teleport", () -> {
         Game g = newGame(0);
         g.player.yaw = 0.0F;
         g.player.pitch = 85.0F;
         g.player.setPos(8.5, GROUND + 1, 8.5);
         // take the ground out of a wide column right below you (down to the bedrock): the pearl falls out of the world
         for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -4; dz <= 12; dz++) {
               for (int y = 0; y <= GROUND; y++) {
                  g.world.setBlock(8 + dx, y, 8 + dz, 0);
               }
            }
         }

         g.inv[g.sel] = new Stack(Items.ENDER_PEARL, 2);
         check(g.useSpecial(g.inv[g.sel]), "thrown into the hole");
         for (int i = 0; i < 200 && !g.mobs.fireballs.isEmpty(); i++) {
            Mobs312.tickFireballs(g.mobs, g.world, g.player);
         }

         eq(20, g.player.health, "no damage when the pearl fell out of the world");
         check(Math.abs(g.player.x - 8.5) < 0.01 && Math.abs(g.player.z - 8.5) < 0.01, "you stayed where you were");
         // a point inside solid rock has no spot
         for (int x = 28; x <= 40; x++) {
            for (int y = GROUND - 12; y <= GROUND - 3; y++) {
               for (int z = 28; z <= 40; z++) {
                  g.world.setBlock(x, y, z, Blocks.STONE);
               }
            }
         }

         check(Mobs312.pearlSpot(g.world, 34.5, GROUND - 7.5, 34.5) == null, "deep inside rock there is no spot");
         check(Mobs312.pearlSpot(g.world, 34.5, GROUND + 1.2, 34.5) != null, "above the ground there is");
         g.mobs.fireballs.clear();
         Mobs312.Fireball f = new Mobs312.Fireball();
         f.pearl = true;
         f.lx = 20.5;
         f.ly = GROUND + 1.5;
         f.lz = 20.5;
         g.player.dead = true;
         Mobs312.explode(g.mobs, g.world, g.player, f);
         check(Math.abs(g.player.x - 8.5) < 0.01, "a dead player stays");
         g.player.dead = false;
         Mobs312.explode(g.mobs, g.world, g.player, f);
         check(Math.abs(g.player.x - 20.5) < 0.7 && Math.abs(g.player.z - 20.5) < 0.7, "a live one arrives");
      });
   }

   static void free(Mesher.Result r) {
      if (r.opaque != null) {
         org.lwjgl.system.MemoryUtil.memFree(r.opaque);
      }

      if (r.trans != null) {
         org.lwjgl.system.MemoryUtil.memFree(r.trans);
      }
   }
}
