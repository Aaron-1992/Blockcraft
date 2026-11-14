package bc;

import static bc.GameTests.check;
import static bc.GameTests.eq;
import static bc.GameTests.test;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Properties;

/** V6.32: the world is solid again - Minecraft-density caves and Minecraft-style structures. */
final class Tests632 {
   private Tests632() {
   }

   static void run() {
      test("6.32: caves leave the world intact instead of hollowing it out", Tests632::density);
      test("6.32: rare natural entrances still connect to a real cave", Tests632::entrances);
      test("6.32: the six underground structures are Minecraft-style and really build", Tests632::structures);
      test("6.32: version, notes and release wiring point at 6.32", Tests632::wiring);
   }

   static String src(String p) throws Exception {
      return new String(Files.readAllBytes(new File(p).toPath()), "UTF-8");
   }

   static WorldSettings settings(int cv) {
      WorldSettings st = new WorldSettings();
      st.terrainVer = 3;
      st.caveVer = cv;
      st.biomeBorders = 1;
      st.caveScale = 1.0F;
      st.caveDensity = 1.0F;
      st.aquatic = true;
      return st;
   }

   static final class Chunks {
      final TerraGen tg;
      final HashMap<Long, Chunk> cache = new HashMap<>();

      Chunks(TerraGen tg) { this.tg = tg; }

      int get(int x, int y, int z) {
         int cx = Math.floorDiv(x, 16), cz = Math.floorDiv(z, 16);
         long k = ((long)cx << 32) ^ (cz & 0xFFFFFFFFL);
         Chunk c = cache.get(k);
         if (c == null) { c = tg.generate(cx, cz); cache.put(k, c); }
         return c.get(x - cx * 16, y, z - cz * 16) & BlockState.ID_MASK;
      }
   }

   static double[] measure(int cv, long seed, int ox, int oz, int n) {
      TerraGen tg = new TerraGen(seed, settings(cv));
      Chunks ch = new Chunks(tg);
      long total = 0, carved = 0, near = 0, nearCarved = 0;
      int holes = 0, linked = 0;
      for (int x = ox; x < ox + n; x++) {
         for (int z = oz; z < oz + n; z++) {
            int h = tg.column(x, z).h;
            if (ch.get(x, h, z) == 0) {
               holes++;
               int y = h;
               while (y > 8 && ch.get(x, y - 1, z) == 0) y--;
               if (h - y >= 10) linked++;
            }
            for (int y = 8; y <= h; y++) {
               int id = ch.get(x, y, z);
               boolean c = id == 0 || id == Blocks.WATER || id == Blocks.LAVA;
               total++;
               if (c) carved++;
               if (y >= h - 20) {
                  near++;
                  if (c) nearCarved++;
               }
            }
         }
      }
      return new double[]{100.0 * carved / total, 100.0 * nearCarved / near, holes, linked};
   }

   static void density() throws Exception {
      long seed = 20261107L;
      int ox = -600, oz = -408, n = 96;
      double[] v6 = measure(6, seed, ox, oz, n);
      double[] v5 = measure(5, seed, ox, oz, n);
      System.out.println("        V6.32 density: carved=" + String.format("%.2f", v6[0])
         + "% near-surface=" + String.format("%.2f", v6[1]) + "% holes=" + (int)v6[2]);
      check(v6[0] < 9.0, "the new carver hollows less than 9% of the world (" + v6[0] + "%)");
      check(v6[1] < 5.0, "the top 20 blocks stay almost entirely solid (" + v6[1] + "%)");
      check(v6[2] < n * n * 0.02, "surface openings are rare (" + (int)v6[2] + " of " + (n * n) + ")");
      check(v5[1] > 12.0 && v6[1] < v5[1] / 3.0,
         "V6.31 carved " + String.format("%.1f", v5[1]) + "% near the surface; V6.32 fixes it");
      check(v5[0] > v6[0] * 2.0, "the total carved volume is much lower than V6.31");
      check(v6[3] >= v6[2] * 9 / 10, "almost every opening leads into a cave (" + (int)v6[3] + "/" + (int)v6[2] + ")");
      Properties p = new Properties();
      settings(6).save(p);
      eq(6, WorldSettings.load(p).caveVer, "cave version 6 round-trips");
      p.setProperty("cavever", "99");
      eq(8, WorldSettings.load(p).caveVer, "future cave versions clamp to 8");
      Properties legacy = new Properties();
      legacy.setProperty("gentype", "0");
      eq(1, WorldSettings.load(legacy).caveVer, "old saves keep their V6.21 caves");
      String t = src("src/bc/TerraGen.java");
      check(t.contains("cc6 ") && t.contains("carve6(wx, y, wz, h, allowSurface)"),
         "the generator uses the V6.32 carver");
      check(t.contains("cc5 ") && t.contains("carve5(wx, y, wz, h, allowSurface)"),
         "V6.31 worlds keep their old carver");
   }

   static void entrances() throws Exception {
      TerraGen tg = new TerraGen(20261107L, settings(6));
      Caves6 c = tg.caves.deep6;
      check(Caves6.landBiome(Biomes.PLAINS, 70) && !Caves6.landBiome(Biomes.OCEAN, 70),
         "only dry land can open");
      int open = 0, matching = 0;
      for (int x = -600; x < -504; x++) {
         for (int z = -408; z < -312; z++) {
            Gen.Col col = tg.column(x, z);
            boolean allow = Caves6.landBiome(col.biome, col.h);
            boolean direct = c.carve(x, col.h, z, col.h, allow) != Caves6.SOLID;
            boolean pred = c.surfaceOpen(x, z, col.h);
            if (direct) open++;
            if (direct == pred) matching++;
         }
      }
      check(open > 0, "the noise gate really opens some entrances (" + open + ")");
      eq(96 * 96, matching, "the tree/spawn exclusion predicate matches the carver everywhere");
      String c6 = src("src/bc/Caves6.java");
      check(c6.contains("depth >= 12") && c6.contains("caveBelow(x, z, surfaceH, k)"),
         "an intact crust and a column-constant cave link keep shafts continuous");
      check(c6.contains("aperture.noise2") && c6.contains("crater.noise2"),
         "entrances are noise-shaped, not a repeated template");
      String t = src("src/bc/TerraGen.java");
      check(t.contains("cc3 && !cc5 && !cc6 && !cc7 && !cc8") && t.contains("!cc6 && !cc7 && !cc8 && h >= 64"),
         "no built cave mouths or stone rims are used for V6.32");
   }

   static int count(Chunk c, int id) {
      int n = 0;
      for (int x = 0; x < 16; x++) {
         for (int z = 0; z < 16; z++) {
            for (int y = 6; y < 80; y++) {
               if ((c.get(x, y, z) & BlockState.ID_MASK) == id) n++;
            }
         }
      }
      return n;
   }

   static void structures() throws Exception {
      eq(6, CaveStructures6.NAME.length, "six Minecraft-style structure types");
      String names = String.join("|", CaveStructures6.NAME);
      for (String want : new String[]{"Mineshaft", "Dungeon", "Stronghold", "Amethyst Geode", "Ancient City", "Ruined Portal"}) {
         check(names.contains(want), want + " is implemented");
      }
      CaveStructures6.Plan a = CaveStructures6.plan(12345L, 3, -2);
      CaveStructures6.Plan b = CaveStructures6.plan(12345L, 3, -2);
      check(a.type == b.type && a.x == b.x && a.z == b.z && a.y == b.y, "plans are deterministic");
      int[] seen = new int[6];
      int[][] site = new int[6][];
      for (int rx = -10; rx <= 10; rx++) {
         for (int rz = -10; rz <= 10; rz++) {
            CaveStructures6.Plan p = CaveStructures6.plan(20261107L, rx, rz);
            if (p.valid) {
               seen[p.type]++;
               if (site[p.type] == null) site[p.type] = new int[]{p.x, p.z};
            }
         }
      }
      for (int n : seen) check(n > 0, "every structure type occurs (" + java.util.Arrays.toString(seen) + ")");

      TerraGen tg = new TerraGen(20261107L, settings(6));
      int built = 0;
      for (int t = 0; t < 6; t++) {
         int[] s = site[t];
         Chunk c = tg.generate(Math.floorDiv(s[0], 16), Math.floorDiv(s[1], 16));
         int blocks = count(c, Blocks.CHEST_LOOT) + count(c, Blocks.SPAWNER) + count(c, Blocks.PLANKS)
            + count(c, Blocks.STONEBRICK) + count(c, Blocks.OBSIDIAN) + count(c, Blocks.GLASS0 + 10)
            + count(c, Blocks.TORCH) + count(c, Redstone319.DARK_PRISMARINE);
         check(blocks > 0, CaveStructures6.NAME[t] + " really builds blocks (" + blocks + ")");
         built += blocks;
      }
      check(built > 100, "the structures are substantial (" + built + " blocks)");
      String s6 = src("src/bc/CaveStructures6.java");
      check(s6.contains("linkToCave") && s6.contains("carve(px, yy, pz, h, false)"),
         "structures connect into the cave network");
      check(s6.contains("Blocks.CHEST_LOOT || id == Blocks.SPAWNER"),
         "link tunnels never erase the loot");
      String w = src("src/bc/CaveWorks.java");
      check(w.contains("this.structures6.apply(ch)") && w.contains("this.deep6.finish(ch)"),
         "V6.32 worlds use the new structures and decorations");
   }

   static void wiring() throws Exception {
      eq("6.38", Game.VERSION_STR, "the game version is 6.37");
      eq("6.38", src("ci/release-trigger.txt").trim(), "the release workflow publishes 6.37");
      check(src("README.md").contains("V6.32"), "README describes V6.32");
      check(src("package/README.txt").contains("WHAT'S NEW IN V6.32"), "the packaged readme describes V6.32");
      check(new File("docs/releases/v6.32.md").isFile(), "V6.32 release notes exist");
      check(new File("docs/screenshots/v6.32-caves.png").isFile(), "the cave preview exists");
      check(src("src/bc/Screens.java").contains("g.opt.caveCliffs ? 8 : 1"), "new worlds use caveVer 8");
      check(src("src/bc/Trailer.java").contains("ws.caveVer = 8"), "the trailer world uses the new caves");
      check(src("tools/build.py").contains("\"Caves6\"")
         && src("tools/verify_release.py").contains("\"Caves6\""),
         "the build receipt and verifier hash the new carver");
      check(src("docs/FILE-LAYOUT.md").contains("Caves6"), "the layout doc lists the new classes");
   }
}
