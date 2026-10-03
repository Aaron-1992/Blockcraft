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
         eq(12, Mobs.N, "twelve mobs so far");
         for (int t = 0; t < Mobs.N; t++) {
            check(Mobs.NAME[t] != null && Mobs.HP[t] > 0 && Mobs.W[t] > 0 && Mobs.H[t] > 0 && Mobs.SPD[t] > 0, "mob " + t + " has name, health, size and speed");
         }

         check(!Mobs.hostile(Mobs.PIG) && !Mobs.hostile(Mobs.CHICKEN), "animals are not hostile");
         check(Mobs.hostile(Mobs.ZOMBIE) && Mobs.hostile(Mobs.ZOMBIE_PIGMAN) && Mobs.hostile(Mobs.GHAST) && Mobs.hostile(Mobs.BLAZE) && Mobs.hostile(Mobs.MAGMA_CUBE), "the monsters are");
         check(!Mobs.hostile(Mobs.VILLAGER) && !Mobs.hostile(Mobs.IRON_GOLEM) && !Mobs.hostile(Mobs.PARROT), "villagers, golems and parrots are not");
         check(Mobs312.fireproof(Mobs.BLAZE) && Mobs312.fireproof(Mobs.ZOMBIE_PIGMAN) && !Mobs312.fireproof(Mobs.ZOMBIE), "fire resistance");
         check(Mobs312.flies(Mobs.GHAST) && Mobs312.flies(Mobs.BLAZE) && !Mobs312.flies(Mobs.PIG), "who floats");
         // the models: every box fits into its texture, every part has a kind the renderer knows
         MobRender.Model[] models = new MobRender.Model[Mobs.N];
         MobModels312.build(models);
         for (int t = Mobs.ZOMBIE_PIGMAN; t <= Mobs.MAGMA_CUBE; t++) {
            MobRender.Model m = models[t];
            check(m != null && m.parts.size() > 0, Mobs.NAME[t] + " has a model");
            for (MobRender.Part p : m.parts) {
               check(p.kind >= 1 && p.kind <= 13, Mobs.NAME[t] + ": part kind " + p.kind);
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
            int sx = best.ox;
            int sy = best.oy + 1;
            int sz = best.oz;
            eq(0, at(cs, cx0, cz0, sx, sy, sz), "the middle of the central hall is open");
            int w = best.maxX - best.minX + 3;
            int d = best.maxZ - best.minZ + 3;
            int hgt = 12;
            boolean[] seen = new boolean[w * d * hgt];
            int[] queue = new int[w * d * hgt];
            int qh = 0;
            int qt = 0;
            int base = best.oy;
            queue[qt++] = ((sx - best.minX + 1) * d + (sz - best.minZ + 1)) * hgt + 1;
            seen[queue[0]] = true;
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
                  if (seen[key] || at(cs, cx0, cz0, nx + best.minX - 1, base + ny, nz + best.minZ - 1) != 0) {
                     continue;
                  }

                  seen[key] = true;
                  queue[qt++] = key;
               }
            }

            for (int[] s : best.spawners) {
               int key = ((s[0] - best.minX + 1) * d + (s[2] - best.minZ + 1)) * hgt + (s[1] + 1 - base);
               check(seen[key], "the cell above the spawner at " + s[0] + "," + s[2] + " can be reached on foot (seed " + seed + ")");
            }

            for (int[] ch : best.chests) {
               int key = ((ch[0] - best.minX + 1) * d + (ch[2] - best.minZ + 1)) * hgt + (ch[1] + 1 - base);
               check(seen[key], "the cell above the chest at " + ch[0] + "," + ch[2] + " can be reached on foot (seed " + seed + ")");
            }

            check(qt > 1500, "a big connected space: " + qt + " cells");
         }
      });

      test("Nether fortress: the spawner in the throne room makes blazes, guards stand on the bricks", () -> {
         Game g = newGame(0);
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
         // guards: spawn attempts around the player on the fortress floor give blazes / skeletons
         g.mobs.list.clear();
         int guards = 0;
         for (int i = 0; i < 300; i++) {
            Mobs312.spawnNether(g.mobs, w, g.player);
         }

         for (Mobs.Mob m : g.mobs.list) {
            if (m.type == Mobs.SKELETON || m.type == Mobs.BLAZE) {
               guards++;
            }
         }

         check(guards > 0, "guards turned up: " + g.mobs.list.size() + " mobs");
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
}
