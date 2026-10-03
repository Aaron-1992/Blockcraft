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
}
