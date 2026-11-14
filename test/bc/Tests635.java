package bc;

import static bc.GameTests.check;
import static bc.GameTests.eq;
import static bc.GameTests.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.Properties;

/**
 * V6.35: a 384-block-deep world, caves that get crazier with depth, twelve
 * structures inside the same cave air, and every menu rebuilt as the Atlas UI.
 */
final class Tests635 {
   private Tests635() {
   }

   static void run() {
      test("6.35: the world is 384 blocks tall and old saves still load", Tests635::height);
      test("6.35: deep terrain really reaches above y=255", Tests635::terrain);
      test("6.35: caves get crazier the deeper you go", Tests635::gradient);
      test("6.35: twelve deep structures share the cave air", Tests635::structures);
      test("6.35: the classic Screens UI is restored after rejected menu redraws", Tests635::menus);
      test("6.35: old saves keep their depth and cave version", Tests635::oldSaves);
      test("6.35: version, notes, previews and release wiring point at 6.35", Tests635::wiring);
   }

   static String src(String p) throws Exception {
      return new String(Files.readAllBytes(new File(p).toPath()), "UTF-8");
   }

   static final long SEED = 20261107L;

   static WorldSettings settings(int depth, int ver) {
      WorldSettings st = new WorldSettings();
      st.terrainVer = 3;
      st.caveVer = ver;
      st.depth = depth;
      st.caveStructures = true;
      st.caveLayers = true;
      st.biomeBorders = 1;
      st.aquatic = true;
      return st;
   }

   static void height() throws Exception {
      eq(384, Chunk.H, "the deep world is 384 blocks tall");
      eq(24, Chunk.SECTIONS, "24 sixteen-block sections");
      eq(98304, Chunk.SIZE, "the chunk array holds 384 layers");
      Chunk c = new Chunk(2, -3);
      eq(Chunk.SIZE, c.blocks.length, "block array is really tall");
      eq(Chunk.SIZE, c.light.length, "light array is really tall");
      eq(Chunk.SECTIONS, c.sections.length, "all 24 sections exist");
      c.set(5, 100, 7, Blocks.STONE);
      c.set(5, 300, 7, Blocks.DIAMOND_ORE);
      c.set(5, 383, 7, Blocks.OBSIDIAN);
      eq(Blocks.DIAMOND_ORE, c.get(5, 300, 7), "a block can be placed above y=255");
      eq(Blocks.OBSIDIAN, c.get(5, 383, 7), "the very top layer is addressable");
      c.computeHeightmap();
      eq(383, c.hm[7 * 16 + 5], "the heightmap reaches the top layer");

      ByteArrayOutputStream raw = new ByteArrayOutputStream();
      Chunk.writeBlocks(new DataOutputStream(raw), c.blocks);
      short[] back = new short[Chunk.SIZE];
      Chunk.readBlocks(new DataInputStream(new ByteArrayInputStream(raw.toByteArray())), back);
      eq(Blocks.DIAMOND_ORE, back[Chunk.idx(5, 300, 7)] & 0xFFFF, "y=300 survives a save/load");
      eq(Blocks.OBSIDIAN, back[Chunk.idx(5, 383, 7)] & 0xFFFF, "y=383 survives a save/load");
      short[] classic = new short[65536];
      Chunk.readBlocks(new DataInputStream(new ByteArrayInputStream(raw.toByteArray())), classic);
      eq(1, classic[Chunk.idx(5, 100, 7)] & 0xFFFF, "a classic-sized reader ignores the deep trailer");
      byte[] legacy = new byte[65536];
      legacy[99] = 7;
      short[] fromOld = new short[Chunk.SIZE];
      Chunk.readBlocks(new DataInputStream(new ByteArrayInputStream(legacy)), fromOld);
      eq(7, fromOld[99] & 0xFFFF, "a pre-3.12 file still reads");

      long k = World.packH(-1234, 383, 5678);
      eq(-1234, World.uxH(k), "x survives the 9-bit-y queue packing");
      eq(383, World.uyH(k), "y=383 fits in the light queue packing");
      eq(5678, World.uzH(k), "z survives the 9-bit-y queue packing");
      long savedKey = World.pack(-1234, 200, 5678);
      eq(-1234, World.ux(savedKey), "the persisted key format is unchanged (x)");
      eq(200, World.uy(savedKey), "the persisted key format is unchanged (y)");
      eq(5678, World.uz(savedKey), "the persisted key format is unchanged (z)");
      eq(63, Gen.BASE_SEA, "classic sea level is unchanged");
      WorldSettings deep = settings(128, 8);
      Gen.applySea(deep);
      eq(191, Gen.SEA, "a deep world lifts sea level to 191");
      eq(128, Gen.depth(), "the depth lift is reported correctly");
      Gen.applySea(settings(0, 1));
      eq(63, Gen.SEA, "a classic world restores sea level");
   }

   static void terrain() throws Exception {
      WorldSettings st = settings(128, 8);
      Gen.applySea(st);
      TerraGen tg = new TerraGen(SEED, st);
      eq(191, tg.sea(), "the generator uses the lifted sea level");
      int min = 999, max = -1, above255 = 0;
      for (int z = -1800; z < 1800; z += 37) {
         for (int x = -1800; x < 1800; x += 37) {
            int h = tg.column(x, z).h;
            min = Math.min(min, h);
            max = Math.max(max, h);
            if (h > 255) above255++;
         }
      }
      System.out.println("        deep terrain: " + min + ".." + max + ", " + above255 + " columns above y=255");
      check(min < 191, "oceans and valleys sit below the lifted sea level");
      check(max > 255, "mountains really use the new upper half of the 384-block world");
      check(above255 > 10, "the high layers are not empty (" + above255 + ")");
      Chunk ch = tg.generate(3, -2);
      int h = tg.column(3 * 16 + 2, -2 * 16 + 2).h;
      eq(0, ch.get(2, Math.min(383, h + 5), 2), "the new high layers stay air above the terrain");
      check(src("src/bc/TerraGen.java").contains("o.genH = (int)clamp"),
         "climate and biome rules use the unshifted generation height");
      check(src("src/bc/TerraGen.java").contains("o.h = o.genH + this.st.depth"),
         "only the final block height is lifted");
      Gen.applySea(settings(0, 1));
   }

   static void gradient() throws Exception {
      WorldSettings st = settings(128, 8);
      Gen.applySea(st);
      TerraGen tg = new TerraGen(SEED, st);
      Caves8 c = tg.caves.deep8;
      check(Caves8.crazy(20, 191) > Caves8.crazy(120, 191),
         "the depth knob increases towards bedrock");
      check(Caves8.crazy(215, 191) == 0.0 && Caves8.crazy(191, 191) < 0.15,
         "there is no depth bonus at or above the surface");
      int n = 48;
      int[] count = new int[5];
      int[] ys = {20, 60, 100, 150, 185};
      for (int j = 0; j < ys.length; j++) {
         for (int z = 0; z < n; z++) {
            for (int x = 0; x < n; x++) {
               int wx = -600 + x, wz = -408 + z;
               Gen.Col col = tg.column(wx, wz);
               if (ys[j] > col.h) continue;
               if (c.carve(wx, ys[j], wz, col.h, Caves8.landBiome(col.biome, col.h, 191)) != Caves8.SOLID) count[j]++;
            }
         }
      }
      System.out.print("        carved per layer y=20/60/100/150/185: ");
      for (int v : count) System.out.print(v + " ");
      System.out.println();
      check(count[0] > count[2] * 2, "the abyss is far more carved than the middle");
      check(count[2] > count[4], "the middle is more carved than the daylight layer");
      check(count[4] < n * n / 4, "the surface layer stays mostly solid");
      int openings = 0, land = 0, shafts = 0;
      for (int z = 0; z < 96; z++) {
         for (int x = 0; x < 96; x++) {
            int wx = -600 + x, wz = -408 + z;
            Gen.Col col = tg.column(wx, wz);
            if (!Caves8.landBiome(col.biome, col.h, 191)) continue;
            land++;
            if (c.carve(wx, col.h, wz, col.h, true) == Caves8.SOLID) continue;
            openings++;
            boolean lateral = false;
            for (int d = 1; d <= 8 && !lateral; d++) {
               for (int q = 1; q <= 3 && !lateral; q++) {
                  for (int s = -1; s <= 1; s += 2) {
                     int nx = wx + s * q, nz = wz;
                     Gen.Col nn = tg.column(nx, nz);
                     if (col.h - d <= nn.h && c.carve(nx, col.h - d, nz, nn.h, true) != Caves8.SOLID) lateral = true;
                     nz = wz + s * q;
                     nn = tg.column(wx, nz);
                     if (col.h - d <= nn.h && c.carve(wx, col.h - d, nz, nn.h, true) != Caves8.SOLID) lateral = true;
                  }
               }
            }
            if (!lateral) shafts++;
         }
      }
      check(openings > 0 && openings < land / 4, "natural cave mouths exist but the ground is intact (" + openings + "/" + land + ")");
      check(shafts <= openings / 10, "almost no mouth is a lone vertical shaft (" + shafts + ")");
      check(Caves8.landBiome(Biomes.PLAINS, 200, 191) && !Caves8.landBiome(Biomes.OCEAN, 200, 191),
         "dry land opens, ocean floors stay sealed");
      Gen.applySea(settings(0, 1));
   }

   static void structures() throws Exception {
      eq(12, CaveStructures8.NAME.length, "twelve underground structure types");
      CaveStructures8.Plan a = CaveStructures8.plan(SEED, 3, -2, 0);
      CaveStructures8.Plan b = CaveStructures8.plan(SEED, 3, -2, 0);
      check(a.x == b.x && a.y == b.y && a.type == b.type, "plans are deterministic across chunk seams");
      int[] sites = new int[12];
      int total = 0, abyss = 0;
      for (int rz = -8; rz <= 8; rz++) {
         for (int rx = -8; rx <= 8; rx++) {
            for (int slot = 0; slot < 2; slot++) {
               CaveStructures8.Plan p = CaveStructures8.plan(SEED, rx, rz, slot);
               if (!p.valid) continue;
               sites[p.type]++;
               total++;
               if (slot == 1) abyss++;
            }
         }
      }
      for (int t = 0; t < 12; t++) check(sites[t] > 0, CaveStructures8.NAME[t] + " has sites");
      check(abyss > total / 4, "there are extra structure sites in the abyss (" + abyss + "/" + total + ")");

      WorldSettings on = settings(128, 8), off = settings(128, 8);
      off.caveStructures = false;
      Gen.applySea(on);
      TerraGen ta = new TerraGen(SEED, on), tb = new TerraGen(SEED, off);
      int overlap = 0, built = 0;
      for (int rz = -2; rz <= 2; rz++) {
         for (int rx = -2; rx <= 2; rx++) {
            for (int slot = 0; slot < 2; slot++) {
               CaveStructures8.Plan p = CaveStructures8.plan(SEED, rx, rz, slot);
               if (!p.valid) continue;
               int h = ta.column(p.x, p.z).h;
               int y = Math.max(10, Math.min(p.y, h - 14));
               for (int dx = -10; dx <= 10; dx += 2) {
                  for (int dz = -10; dz <= 10; dz += 2) {
                     int cause = ta.caves.deep8.cause(p.x + dx, y, p.z + dz, h);
                     if ((cause & Caves8.C_STRUCT) == 0) continue;
                     if ((cause & (Caves8.C_WORM | Caves8.C_CHEESE | Caves8.C_HALL | Caves8.C_MEGA | Caves8.C_MAZE)) != 0) overlap++;
                  }
               }
               int cx = Math.floorDiv(p.x, 16), cz = Math.floorDiv(p.z, 16);
               Chunk c1 = ta.generate(cx, cz), c2 = tb.generate(cx, cz);
               for (int z = 0; z < 16; z += 2) {
                  for (int x = 0; x < 16; x += 2) {
                     for (int yy = Math.max(8, y - 4); yy <= Math.min(Chunk.H - 1, y + 8); yy++) {
                        if ((c1.get(x, yy, z) & BlockState.ID_MASK) != (c2.get(x, yy, z) & BlockState.ID_MASK)) built++;
                     }
                  }
               }
            }
         }
      }
      System.out.println("        cave/structure intersections=" + overlap + " structure blocks=" + built);
      check(overlap > 0, "natural caves cut through the structures' own carved air");
      check(built > 100, "the structures really place blocks into the world (" + built + ")");
      Gen.applySea(settings(0, 1));
   }

   static void menus() throws Exception {
      String game = src("src/bc/Game.java");
      for (String screen : new String[]{"title", "worlds", "multiplayer", "newWorld", "minigames",
         "singleplayer", "mpHub", "mpCreate", "loading", "waiting", "pause", "more",
         "resourcePacks", "mods", "credits", "options"}) {
         check(game.contains("Screens." + screen + "(this)"), "classic Screens is routed for " + screen);
      }
      check(!game.contains("Menus635.title(this)") && !game.contains("Menus635.options(this)"),
         "the rejected V6.35 menu is not active");
      String screens = src("src/bc/Screens.java");
      check(screens.contains("ui.preview.update") && screens.contains("gui.image(ui.preview.img"),
         "the live world map preview remains on Create New World");
      check(screens.contains("Season") && screens.contains("Climate HUD"),
         "the restored classic settings include climate controls");
      String scenes = src("test/bc/Scenes.java");
      check(scenes.contains("Screens.title(g)") && scenes.contains("Screens.options(g)"),
         "headless scene coverage draws the restored classic screens");
   }

   static void oldSaves() throws Exception {
      WorldSettings st = settings(128, 8);
      Properties p = new Properties();
      st.save(p);
      eq(128, WorldSettings.load(p).depth, "the deep-world depth round-trips");
      eq(8, WorldSettings.load(p).caveVer, "caveVer 8 round-trips");
      eq(191, WorldSettings.load(p).sea(), "the saved world reports its lifted sea level");
      p.setProperty("worlddepth", "999");
      eq(128, WorldSettings.load(p).depth, "the depth is clamped");
      p.setProperty("cavever", "999");
      eq(8, WorldSettings.load(p).caveVer, "future cave versions clamp to 8");
      Properties legacy = new Properties();
      legacy.setProperty("gentype", "0");
      eq(0, WorldSettings.load(legacy).depth, "pre-settings saves keep the classic height");
      eq(1, WorldSettings.load(legacy).caveVer, "pre-settings saves keep V6.21 caves");
   }

   static void wiring() throws Exception {
      eq("6.38", Game.VERSION_STR, "the game version is 6.37");
      eq("6.38", src("ci/release-trigger.txt").trim(), "the release workflow publishes 6.37");
      check(src("README.md").contains("V6.35"), "README describes V6.35");
      check(src("package/README.txt").contains("WHAT'S NEW IN V6.35"), "the packaged readme describes V6.35");
      check(new File("docs/releases/v6.35.md").isFile(), "V6.35 release notes exist");
      check(new File("docs/screenshots/v6.35-menus.png").isFile(), "the Atlas menu preview exists");
      check(new File("docs/screenshots/v6.35-caves.png").isFile(), "the deep-cave preview exists");
      check(src("tools/build.py").contains("\"Caves8\"") && src("tools/verify_release.py").contains("\"Caves8\""),
         "the build receipt and verifier hash the new carver");
      check(src("tools/build.py").contains("\"Menus635\"") && src("tools/verify_release.py").contains("\"Menus635\""),
         "the build receipt and verifier hash the new menus");
      check(src("src/bc/Trailer.java").contains("ws.caveVer = 8") && src("src/bc/Trailer.java").contains("ws.depth = 128"),
         "the trailer world is deep too");
      check(src("docs/FILE-LAYOUT.md").contains("Menus635"), "the layout doc lists the new UI");
      check(src("tools/make_release.py").contains("if version == \"6.35\""), "the GitHub release notes describe 6.35");
   }
}
