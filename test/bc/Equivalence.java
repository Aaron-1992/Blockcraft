package bc;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.nio.FloatBuffer;
import java.util.zip.CRC32;
import org.lwjgl.system.MemoryUtil;

/**
 * Deterministic "fingerprint" of the game's pure-logic subsystems (no OpenGL needed).
 *
 * Run it twice - once with the classes of an OLD jar on the classpath, once with freshly built classes - and
 * diff the two outputs. Identical output = the subsystems that were exercised behave identically.
 * It was used to prove that the source recovered (decompiled) from the Update 3.5 jar behaves exactly like
 * the original. Because it is written in package bc it can see package-private members.
 *
 *   java -cp test-classes:OLD.jar bc.Equivalence [section...]      sections: statics gen world phys recipes tick
 *   java -cp test-classes:build/classes:lwjgl-bundle.jar bc.Equivalence
 */
public final class Equivalence {
   static void p(String s) {
      System.out.println(s);
   }

   // ---------- generic deterministic hashing ----------
   static long mix(long a, long b) {
      long r = (a ^ (b + 0x9E3779B97F4A7C15L + (a << 6) + (a >>> 2))) * 0xC2B2AE3D27D4EB4FL;
      return r ^ (r >>> 29);
   }

   static long h(Object o) {
      return h(o, 0);
   }

   static long h(Object o, int depth) {
      if (o == null) {
         return 17;
      }
      if (depth > 6) {
         return 99;
      }
      Class<?> c = o.getClass();
      if (c == byte[].class) {
         CRC32 crc = new CRC32();
         crc.update((byte[]) o);
         return crc.getValue() ^ ((byte[]) o).length;
      }
      if (c.isArray()) {
         long r = 1469598103934665603L;
         int n = Array.getLength(o);
         for (int i = 0; i < n; i++) {
            r = mix(r, h(Array.get(o, i), depth + 1));
         }
         return mix(r, n);
      }
      if (o instanceof Float f) {
         return Float.floatToRawIntBits(f);
      }
      if (o instanceof Double d) {
         return Double.doubleToRawLongBits(d);
      }
      if (o instanceof Number || o instanceof Boolean || o instanceof Character) {
         return o.hashCode();
      }
      if (o instanceof String s) {
         return s.hashCode();
      }
      if (o instanceof Enum<?> e) {
         return e.name().hashCode();
      }
      if (o instanceof Collection<?> col) {
         long r = 7;
         for (Object x : col) {
            r = mix(r, h(x, depth + 1));
         }
         return mix(r, col.size());
      }
      if (o instanceof Map<?, ?> m) {
         TreeMap<String, Long> sorted = new TreeMap<>();
         for (Map.Entry<?, ?> e : m.entrySet()) {
            sorted.put(String.valueOf(e.getKey()), h(e.getValue(), depth + 1));
         }
         long r = 11;
         for (Map.Entry<String, Long> e : sorted.entrySet()) {
            r = mix(mix(r, e.getKey().hashCode()), e.getValue());
         }
         return r;
      }
      if (c.getName().startsWith("bc.")) {
         long r = c.getSimpleName().hashCode();
         Field[] fs = c.getDeclaredFields();
         Arrays.sort(fs, (a, b) -> a.getName().compareTo(b.getName()));
         for (Field f : fs) {
            if (Modifier.isStatic(f.getModifiers()) || f.isSynthetic()) {
               continue;
            }
            try {
               f.setAccessible(true);
               r = mix(r, h(f.get(o), depth + 1));
            } catch (Throwable t) {
               r = mix(r, 5);
            }
         }
         return r;
      }
      return c.getName().hashCode();
   }

   static String hex(long v) {
      return Long.toHexString(v);
   }

   // ---------- 1. static tables ----------
   static void statics() throws Exception {
      String[] names = {"Blocks", "Items", "Recipes", "Mobs", "Structs", "Phys", "Gen", "Chunk", "World", "Noise", "Player", "Stack", "TileEntity", "TexExtra", "ShaderSrc"};
      for (String n : names) {
         Class<?> c;
         try {
            c = Class.forName("bc." + n);
         } catch (Throwable t) {
            p("statics " + n + " LOAD-FAIL " + t);
            continue;
         }
         Field[] fs = c.getDeclaredFields();
         Arrays.sort(fs, (a, b) -> a.getName().compareTo(b.getName()));
         for (Field f : fs) {
            if (!Modifier.isStatic(f.getModifiers()) || f.isSynthetic()) {
               continue;
            }
            try {
               f.setAccessible(true);
               p("statics " + n + "." + f.getName() + " = " + hex(h(f.get(null))));
            } catch (Throwable t) {
               p("statics " + n + "." + f.getName() + " ERR " + t.getClass().getSimpleName());
            }
         }
      }
   }

   /**
    * The blocks of a chunk as the byte array they were up to Update 3.11 (low 8 bits of every id). Read through Chunk.get() so the same
    * test classes also run against the old 3.5 jar, where Chunk.blocks is a byte[] and not a short[].
    */
   /** V6.35: Chunk.light is taller now; equivalence still checks the classic 0..255 layers byte-for-byte. */
   static byte[] lightBytes(Chunk c) {
      byte[] b = new byte[65536];
      System.arraycopy(c.light, 0, b, 0, 65536);
      return b;
   }

   static byte[] blockBytes(Chunk c) {
      byte[] b = new byte[65536];
      for (int y = 0; y < 256; y++) {
         for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
               b[Chunk.idx(x, y, z)] = (byte)c.get(x, y, z);
            }
         }
      }

      return b;
   }

   // ---------- 2. raw terrain generation ----------
   static void gen() {
      long[] seeds = {1L, 12345L, 987654321L, -42L};
      for (long seed : seeds) {
         Gen g = new Gen(seed);
         int[] sp = g.findSpawn();
         p("gen seed=" + seed + " spawn = " + sp[0] + "," + sp[1]);
         long all = 0;
         for (int cz = -3; cz <= 3; cz++) {
            for (int cx = -3; cx <= 3; cx++) {
               Chunk c = g.generate(cx, cz);
               long v = mix(mix(h(blockBytes(c)), h(c.hm)), mix(mix(h(c.grassColor), h(c.foliageColor)), h(c.biome)));
               all = mix(all, v);
               if (cx == 0 && cz == 0 || cx == 3 && cz == -2) {
                  p("gen seed=" + seed + " chunk " + cx + "," + cz + " = " + hex(v));
               }
            }
         }
         p("gen seed=" + seed + " all = " + hex(all));
      }
   }

   // ---------- 3. world pipeline (threads + lighting) ----------
   static World newWorld(long seed) throws Exception {
      World w = new World(seed, null);
      w.saveEnabled = false;
      seedRnd(w, 4242);
      return w;
   }

   static void seedRnd(Object o, long seed) throws Exception {
      Field f = o.getClass().getDeclaredField("rnd");
      f.setAccessible(true);
      f.set(o, new Random(seed));
   }

   static void load(World w, int cx, int cz, int r) throws Exception {
      int last = -1;
      int stable = 0;
      for (int it = 0; it < 20000 && stable < 150; it++) {
         w.updateChunks(cx, cz, r, 64);
         Thread.sleep(2);
         int n = w.chunks.size();
         if (n == last) {
            stable++;
         } else {
            stable = 0;
            last = n;
         }
      }
   }

   static void world() throws Exception {
      for (long seed : new long[]{12345L, 31337L}) {
         World w = newWorld(seed);
         load(w, 0, 0, 3);
         List<Long> keys = new ArrayList<>(w.chunks.keySet());
         keys.sort(null);
         long blocks = 0;
         long light = 0;
         int lit = 0;
         for (long k : keys) {
            Chunk c = w.chunks.get(k);
            blocks = mix(blocks, mix(k, h(blockBytes(c))));
            light = mix(light, mix(k, h(lightBytes(c))));
            if (c.lit) {
               lit++;
            }
         }
         p("world seed=" + seed + " = chunks=" + keys.size() + " lit=" + lit + " blocks=" + hex(blocks) + " light=" + hex(light));
      }
   }

   // ---------- 4. collision physics ----------
   static void phys() throws Exception {
      World w = newWorld(777);
      Gen g = w.gen;
      for (int cz = -3; cz <= 3; cz++) {
         for (int cx = -3; cx <= 3; cx++) {
            w.chunks.put(World.key(cx, cz), g.generate(cx, cz));
         }
      }
      Random r = new Random(99);
      long acc = 0;
      double[] out = new double[3];
      int collided = 0;
      for (int i = 0; i < 6000; i++) {
         double x = r.nextDouble() * 80 - 40;
         double z = r.nextDouble() * 80 - 40;
         int cx = (int) Math.floor(x) >> 4;
         int cz = (int) Math.floor(z) >> 4;
         Chunk c = w.chunk(cx, cz);
         int gy = c.hm[((int) Math.floor(z) & 15) * 16 + ((int) Math.floor(x) & 15)];
         double y = gy + r.nextDouble() * 3 - 0.5;
         double hw = 0.3;
         double hh = 1.8;
         double[] box = {x - hw, y, z - hw, x + hw, y + hh, z + hw};
         if (Phys.collides(w, box)) {
            collided++;
         }
         double dx = r.nextDouble() * 2 - 1;
         double dy = r.nextDouble() * 4 - 2;
         double dz = r.nextDouble() * 2 - 1;
         Phys.move(w, box, dx, dy, dz, out);
         acc = mix(acc, h(box));
         acc = mix(acc, h(out));
      }
      // a big multi-box column to exercise the >21-box code path (the old byte-loop hazard)
      for (int y = 100; y < 130; y++) {
         w.setBlock(2, y, 2, Blocks.FENCE);
         w.setBlock(3, y, 2, Blocks.STAIR0);
      }
      double[] big = {1.6, 99.0, 1.6, 2.4, 132.0, 2.4};
      Phys.move(w, big, 0.0, -1.0, 0.0, out);
      Phys.move(w, big, 0.7, 0.0, 0.0, out);
      acc = mix(acc, h(big));
      p("phys = collided=" + collided + " acc=" + hex(acc) + " big=" + Arrays.toString(big));
   }

   // ---------- 5. crafting lookup ----------
   static void recipes() {
      Random r = new Random(5);
      long acc = 0;
      int hits = 0;
      int[] pool = new int[Items.class.getDeclaredFields().length > 0 ? 400 : 400];
      for (int i = 0; i < pool.length; i++) {
         pool[i] = i;
      }
      // (a) every recipe, laid out in a 3x3 and a 2x2 grid at random offsets
      for (int round = 0; round < 3000; round++) {
         int gw = round % 2 == 0 ? 3 : 2;
         Stack[] grid = new Stack[gw * gw];
         int n = 1 + r.nextInt(gw * gw);
         for (int k = 0; k < n; k++) {
            int id;
            switch (r.nextInt(4)) {
               case 0 -> id = 5; // planks
               case 1 -> id = 256; // stick
               case 2 -> id = 4 + r.nextInt(5);
               default -> id = r.nextInt(300);
            }
            grid[r.nextInt(grid.length)] = new Stack(id, 1);
         }
         Stack res = Recipes.find(grid, gw, gw);
         if (res != null) {
            hits++;
            acc = mix(acc, mix(res.id, res.n));
         } else {
            acc = mix(acc, 3);
         }
      }
      // (b) the real recipes
      int total = 0;
      for (Recipes.Recipe rc : Recipes.list) {
         total++;
         Stack[] g3 = new Stack[9];
         if (rc.shapeless) {
            for (int i = 0; i < rc.grid.length; i++) {
               g3[i] = new Stack(Math.max(rc.grid[i], 1), 1);
            }
         } else {
            for (int y = 0; y < rc.h; y++) {
               for (int x = 0; x < rc.w; x++) {
                  int want = rc.grid[y * rc.w + x];
                  if (want != 0) {
                     g3[(y + 1 - rc.h / 2) * 3 + x + (3 - rc.w) / 2 * 0] = new Stack(want > 0 ? want : 5, 1);
                  }
               }
            }
         }
         try {
            Stack res = Recipes.find(g3, 3, 3);
            acc = mix(acc, res == null ? 1 : mix(res.id, res.n));
         } catch (Throwable t) {
            acc = mix(acc, 2);
         }
      }
      p("recipes = random-hits=" + hits + " real-recipes=" + total + " acc=" + hex(acc));
   }

   // ---------- 6. simulation: fluids, falling blocks, TNT fuse, furnace-less ticking ----------
   static void tick() throws Exception {
      World w = newWorld(2024);
      Gen g = w.gen;
      for (int cz = -3; cz <= 3; cz++) {
         for (int cx = -3; cx <= 3; cx++) {
            Chunk c = g.generate(cx, cz);
            w.chunks.put(World.key(cx, cz), c);
         }
      }
      for (Chunk c : new ArrayList<>(w.chunks.values())) {
         w.lightChunk(c);
      }
      int gx = 4;
      int gz = 4;
      int top = w.chunk(0, 0).hm[gz * 16 + gx];
      int y0 = top + 1;
      // water source on a ledge + lava source + sand tower over air + a few TNT fuses + ladders/torches/doors
      for (int dx = -6; dx <= 6; dx++) {
         for (int dz = -6; dz <= 6; dz++) {
            w.setBlock(gx + dx, y0, gz + dz, 0);
            w.setBlock(gx + dx, y0 - 1, gz + dz, Blocks.STONE);
         }
      }
      w.setBlock(gx, y0 + 2, gz, Blocks.WATER);
      w.setBlock(gx + 4, y0 + 1, gz + 4, Blocks.LAVA);
      w.setBlock(gx - 4, y0 + 5, gz - 4, Blocks.SAND);
      w.setBlock(gx - 4, y0 + 6, gz - 4, Blocks.SAND);
      w.setBlock(gx - 4, y0 + 7, gz - 4, Blocks.GRAVEL);
      w.setBlock(gx + 3, y0, gz - 3, Blocks.LADDER);
      w.setBlock(gx + 3, y0 + 1, gz - 3, Blocks.TORCH);
      w.setBlock(gx - 3, y0, gz + 3, Blocks.WHEAT);
      w.primeTnt(gx + 2, y0 + 3, gz + 2, 40);
      w.primeTnt(gx - 2, y0 + 6, gz + 2, 70);
      for (int t = 0; t < 600; t++) {
         w.tick();
      }
      long acc = 0;
      for (int dx = -9; dx <= 9; dx++) {
         for (int dz = -9; dz <= 9; dz++) {
            for (int y = y0 - 3; y < y0 + 12; y++) {
               acc = mix(acc, w.getBlock(gx + dx, y, gz + dz));
            }
         }
      }
      p("tick = region=" + hex(acc) + " items=" + w.items.size() + " fallers=" + w.fallers.size() + " tick=" + w.tickCount);
   }


   // ---------- 7. CPU-side texture / text / icon generators (no GL involved) ----------
   static final String[] PURE = {
      "Tex:t", "Tex:argb", "Tex:clamp", "Tex:rgb", "TexExtra:", "Gui:textWidth", "Gui:stripCodes", "Gui:isEdge", "Gui:tintFor",
      "Gui:iconBoxes", "Gui:sx", "Gui:sy", "Lod:hash", "Lod:pack", "Lod:code", "Lod:shade", "Lod:spR", "MobRender:hash",
      "MobRender:vnoise", "MobRender:mul", "MobRender:lerpAngle", "Mesher:lu", "Mesher:ci", "Gen:clamp", "Gen:smooth"
   };

   static boolean wanted(String cls, String name) {
      for (String w : PURE) {
         int i = w.indexOf(':');
         if (w.substring(0, i).equals(cls) && name.startsWith(w.substring(i + 1))) {
            return true;
         }
      }
      return false;
   }

   static Object[] sampleArgs(Class<?>[] pt, int trial) {
      int[] ints = trial == 0 ? new int[]{16, 8, 3, 1, 2, 5, 9, 12, 4, 6} : trial == 1 ? new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9} : new int[]{255, 17, 33, 64, 101, 7, 250, 31, 90, 14};
      int ii = 0;
      Object[] a = new Object[pt.length];
      for (int i = 0; i < pt.length; i++) {
         Class<?> t = pt[i];
         if (t == int.class) {
            a[i] = ints[ii++ % ints.length];
         } else if (t == long.class) {
            a[i] = 1234567L + trial;
         } else if (t == float.class) {
            a[i] = 0.25F + trial * 0.37F;
         } else if (t == double.class) {
            a[i] = 0.5 + trial * 1.3;
         } else if (t == boolean.class) {
            a[i] = trial % 2 == 0;
         } else if (t == String.class) {
            a[i] = trial == 0 ? "Hello \u00a7cWorld \u00a7l!" : trial == 1 ? "" : "The quick brown fox 0123456789";
         } else if (t == int[].class) {
            a[i] = new int[]{0xFF336699, 0xFF99AA33, 0xFFAA3366, 0xFF223344};
         } else {
            return null;
         }
      }
      return a;
   }

   static void tex() throws Exception {
      for (String cn : new String[]{"Tex", "TexExtra", "Gui", "Lod", "MobRender", "Mesher", "Gen"}) {
         Class<?> c = Class.forName("bc." + cn);
         Method[] ms = c.getDeclaredMethods();
         Arrays.sort(ms, (x, y) -> (x.getName() + x.toGenericString()).compareTo(y.getName() + y.toGenericString()));
         for (Method m : ms) {
            if (!Modifier.isStatic(m.getModifiers()) || m.isSynthetic() || !wanted(cn, m.getName())) {
               continue;
            }
            for (int trial = 0; trial < 3; trial++) {
               Object[] args = sampleArgs(m.getParameterTypes(), trial);
               if (args == null) {
                  continue;
               }
               m.setAccessible(true);
               String label = "tex " + cn + "." + m.getName() + Arrays.toString(m.getParameterTypes()).replace("class ", "") + "#" + trial;
               try {
                  p(label + " = " + hex(h(m.invoke(null, args))));
               } catch (InvocationTargetException e) {
                  p(label + " threw " + e.getCause().getClass().getSimpleName());
               }
            }
         }
      }
      // sprites for every item id, and the full block tile builders
      long acc = 0;
      for (int id = 0; id < 520; id++) {
         try {
            acc = mix(acc, h(TexExtra.sprite(id)));
         } catch (Throwable t) {
            acc = mix(acc, t.getClass().getSimpleName().hashCode());
         }
      }
      p("tex sprites 0..519 = " + hex(acc));
      acc = 0;
      for (int id = 0; id < 520; id++) {
         try {
            acc = mix(acc, h(Gui.iconBoxes(id)));
         } catch (Throwable t) {
            acc = mix(acc, t.getClass().getSimpleName().hashCode());
         }
      }
      p("tex iconBoxes 0..519 = " + hex(acc));
      // NOTE: MobRender.build() is deliberately NOT called - it uploads textures (needs a GL context and aborts the JVM without one).
   }

   // ---------- 8. chunk meshing (vertex data) ----------
   /** Tiles per atlas row: 32 since Update 3.12, 16 before (the 3.5 jar has no Tex.COLS). */
   static int atlasCols() {
      try {
         return Class.forName("bc.Tex").getField("COLS").getInt(null);
      } catch (ReflectiveOperationException e) {
         return 16;
      }
   }

   static long floats(FloatBuffer fb) {
      if (fb == null) {
         return 0;
      }
      long r = 31;
      FloatBuffer d = fb.duplicate();
      if (d.remaining() % 11 == 0) {
         // 11 floats per vertex: x y z u v r g b a m l. The atlas got bigger in Update 3.12, so u / v are compared as "which tile, and where in
         // it" (to 2%), not as raw texture coordinates; everything else bit for bit.
         int cols = atlasCols();
         while (d.hasRemaining()) {
            for (int k = 0; k < 11; k++) {
               float f = d.get(d.position() + k);
               if (k == 3 || k == 4) {
                  continue;
               }
               r = mix(r, Float.floatToRawIntBits(f));
            }

            float u = d.get(d.position() + 3) * cols;
            float v = d.get(d.position() + 4) * cols;
            int tile = (int)Math.floor(v) * cols + (int)Math.floor(u);
            r = mix(r, mix(tile, Math.round((u - Math.floor(u)) * 50) * 64L + Math.round((v - Math.floor(v)) * 50)));
            d.position(d.position() + 11);
         }

         return mix(r, fb.limit());
      }

      while (d.hasRemaining()) {
         r = mix(r, Float.floatToRawIntBits(d.get()));
      }
      return mix(r, fb.limit());
   }

   static void mesh() throws Exception {
      // (a) natural terrain
      World w = newWorld(12345);
      load(w, 0, 0, 3);
      long acc = 0;
      int verts = 0;
      for (int cz = -1; cz <= 1; cz++) {
         for (int cx = -1; cx <= 1; cx++) {
            Chunk c = w.chunk(cx, cz);
            for (int sy = 0; sy < 16; sy++) {
               for (int flag = 0; flag < 2; flag++) {
                  Mesher.Result r = Mesher.build(w, c, sy, flag == 1);
                  acc = mix(acc, mix(floats(r.opaque), floats(r.trans)));
                  acc = mix(acc, mix(r.nO, r.nT));
                  verts += r.nO + r.nT;
                  if (r.opaque != null) {
                     MemoryUtil.memFree(r.opaque);
                  }
                  if (r.trans != null) {
                     MemoryUtil.memFree(r.trans);
                  }
               }
            }
         }
      }
      p("mesh terrain = verts=" + verts + " acc=" + hex(acc));
      // (b) showcase: every block id once, on a stone floor, in 4 chunks
      World s = newWorld(1);
      for (int cz = -1; cz <= 2; cz++) {
         for (int cx = -1; cx <= 2; cx++) {
            Chunk c = new Chunk(cx, cz);
            for (int x = 0; x < 16; x++) {
               for (int z = 0; z < 16; z++) {
                  c.set(x, 0, z, Blocks.BEDROCK);
                  for (int y = 1; y < 60; y++) {
                     c.set(x, y, z, Blocks.STONE);
                  }
               }
            }
            if (cx >= 0 && cx <= 1 && cz >= 0 && cz <= 1) {
               int base = 1 + (cz * 2 + cx) * 35;
               for (int k = 0; k < 35; k++) {
                  int id = base + k;
                  if (id < Blocks.COUNT) {
                     c.set(1 + (k % 7) * 2, 60, 1 + (k / 7) * 3, id);
                  }
               }
               // some neighbours so fences/stairs/water/crops link and light
               c.set(2, 60, 14, Blocks.FENCE);
               c.set(3, 60, 14, Blocks.FENCE);
               c.set(3, 61, 14, Blocks.TORCH);
               c.set(5, 60, 14, Blocks.WATER);
               c.set(6, 60, 14, Blocks.WATER);
               c.set(6, 61, 14, Blocks.WATER);
            }
            c.computeHeightmap();
            s.chunks.put(World.key(cx, cz), c);
         }
      }
      for (Chunk c : new ArrayList<>(s.chunks.values())) {
         s.lightChunk(c);
      }
      acc = 0;
      verts = 0;
      for (int cz = 0; cz <= 1; cz++) {
         for (int cx = 0; cx <= 1; cx++) {
            Chunk c = s.chunk(cx, cz);
            for (int sy = 3; sy <= 4; sy++) {
               for (int flag = 0; flag < 2; flag++) {
                  Mesher.Result r = Mesher.build(s, c, sy, flag == 1);
                  long v = mix(floats(r.opaque), floats(r.trans));
                  acc = mix(acc, v);
                  verts += r.nO + r.nT;
                  p("mesh showcase chunk " + cx + "," + cz + " sec " + sy + " flag " + flag + " nO=" + r.nO + " nT=" + r.nT + " = " + hex(v));
                  if (r.opaque != null) {
                     MemoryUtil.memFree(r.opaque);
                  }
                  if (r.trans != null) {
                     MemoryUtil.memFree(r.trans);
                  }
               }
            }
         }
      }
      p("mesh showcase = verts=" + verts + " acc=" + hex(acc));
   }

   public static void main(String[] args) throws Exception {
      List<String> sections = args.length == 0 ? List.of("statics", "gen", "world", "phys", "recipes", "tick", "tex", "mesh") : List.of(args);
      for (String s : sections) {
         switch (s) {
            case "statics" -> statics();
            case "gen" -> gen();
            case "world" -> world();
            case "phys" -> phys();
            case "recipes" -> recipes();
            case "tick" -> tick();
            case "tex" -> tex();
            case "mesh" -> mesh();
            default -> p("unknown section " + s);
         }
      }
      System.exit(0);
   }
}
