package bc;

import static bc.GameTests.check;
import static bc.GameTests.eq;
import static bc.GameTests.test;

import java.io.File;
import java.nio.file.Files;
import java.util.Properties;

/**
 * V6.34: caves are ONE 3D volume that intersects the terrain (no drilled shafts),
 * and every underground structure is part of the same cave air field.
 */
final class Tests634 {
   private Tests634() {
   }

   static void run() {
      test("6.34: there is no aperture, funnel, shaft or link tunnel left", Tests634::noShafts);
      test("6.34: the cave volume leaves the world solid and opens naturally", Tests634::density);
      test("6.34: entrances are sloped cave walls, never vertical holes", Tests634::entrances);
      test("6.34: all six structures share the cave air and really build", Tests634::structures);
      test("6.34: old saves keep their own cave version", Tests634::oldSaves);
      test("6.34: version, notes, preview and release wiring point at 6.34", Tests634::wiring);
   }

   static String src(String p) throws Exception {
      return new String(Files.readAllBytes(new File(p).toPath()), "UTF-8");
   }

   static WorldSettings settings(int cv) {
      WorldSettings st = new WorldSettings();
      st.terrainVer = 3;
      st.caveVer = cv;
      st.caveStructures = true;
      st.caveLayers = true;
      st.caveScale = 1.0F;
      st.caveDensity = 1.0F;
      st.biomeBorders = 1;
      st.aquatic = true;
      return st;
   }

   static final long SEED = 20261107L;

   static WorldSettings settingsNoStructures() {
      WorldSettings st = settings(7);
      st.caveStructures = false;
      return st;
   }

   /** Every one of the rejected V6.28/V6.29/V6.32 entrance mechanisms must be absent. */
   static void noShafts() throws Exception {
      String c7 = src("src/bc/Caves7.java");
      String s7 = src("src/bc/CaveStructures7.java");
      String t = src("src/bc/TerraGen.java");
      for (String bad : new String[]{"CaveCliffs.Mouth", "surfaceHole(", "floorAt(",
         "aperture.noise", "crater.noise", "linkToCave", "shaft(this.seed"}) {
         check(!c7.contains(bad) && !s7.contains(bad),
            "V6.34 has no " + bad + " entrance/link machinery");
      }
      check(c7.contains("int cause(int x, int y, int z, int surfaceH)")
         && c7.contains("cause(x, y, z, surfaceH) != 0"),
         "one carver decides every block, including the surface");
      check(c7.contains("this.structures.air(x, y, z, surfaceH)"),
         "structure air is evaluated by that same carver");
      check(!c7.contains("surfaceH - 12") && !c7.contains("surfaceH - 10"),
         "there is no intact crust that would need a shaft");
      check(t.contains("cc7") && t.contains("carve7(wx, y, wz, h, allowSurface)"),
         "the generator calls the V6.34 carver");
      check(t.contains("!cc7") && t.contains("caveVer >= 7 ? 0.60"),
         "V6.34 switches off the old sinkholes and turns ravine cracks almost off");
      check(src("src/bc/Structs.java").contains("caveVer < 7")
         && src("src/bc/Structs.java").contains("caveMouthNear")
         && src("src/bc/Village314.java").contains("!Structs.caveMouthNear(gen, x, z)"),
         "legacy sealed underground structures are off and villages avoid cave mouths");
      check(t.contains("this.st.caveVer < 7"),
         "the old isolated surface stronghold is off for new worlds");
   }

   /** The world must stay intact, and openings must not be 1-block vertical shafts. */
   static void density() throws Exception {
      TerraGen tg = new TerraGen(SEED, settings(7));
      Caves7 c = tg.caves.deep7;
      int n = 96, ax = -600, az = -408;
      long carved = 0, total = 0, near = 0, nearTotal = 0, holes = 0, shaft = 0, linked = 0;
      for (int z = 0; z < n; z++) {
         for (int x = 0; x < n; x++) {
            int wx = ax + x, wz = az + z;
            Gen.Col col = tg.column(wx, wz);
            if (col.h < 64) continue;
            boolean allow = Caves7.landBiome(col.biome, col.h);
            boolean open = c.carve(wx, col.h, wz, col.h, allow) != Caves7.SOLID;
            for (int y = 6; y <= col.h; y++) {
               int cv = c.carve(wx, y, wz, col.h, allow);
               total++;
               if (cv != Caves7.SOLID) carved++;
               if (y >= col.h - 20) {
                  nearTotal++;
                  if (cv != Caves7.SOLID) near++;
               }
            }
            if (!open) continue;
            holes++;
            boolean lateral = false, deep = false;
            for (int d = 1; d <= 10 && !lateral; d++) {
               int yy = col.h - d;
               for (int q = 1; q <= 4 && !lateral; q++) {
                  for (int s = -1; s <= 1; s += 2) {
                     int nx = wx + s * q, nz = wz;
                     Gen.Col nn = tg.column(nx, nz);
                     if (yy <= nn.h && Caves7.landBiome(nn.biome, nn.h)
                        && c.carve(nx, yy, nz, nn.h, true) != Caves7.SOLID) lateral = true;
                     nz = wz + s * q;
                     nn = tg.column(wx, nz);
                     if (yy <= nn.h && Caves7.landBiome(nn.biome, nn.h)
                        && c.carve(wx, yy, nz, nn.h, true) != Caves7.SOLID) lateral = true;
                  }
               }
            }
            for (int d = 2; d <= 14; d++) {
               if (c.carve(wx, col.h - d, wz, col.h, allow) != Caves7.SOLID) deep = true;
            }
            if (!lateral) shaft++;
            if (deep) linked++;
         }
      }
      double pc = 100.0 * carved / total, pn = 100.0 * near / nearTotal;
      System.out.println("        V6.34 density: carved=" + String.format("%.2f", pc)
         + "% near-surface=" + String.format("%.2f", pn) + "% openings=" + holes
         + "/" + (n * n) + " shaftLike=" + shaft + " linked=" + linked);
      check(pc > 4.0 && pc < 14.0, "the underground is real but the world is solid (" + pc + "%)");
      check(pn < 9.0, "the top 20 blocks are not swiss cheese (" + pn + "%)");
      check(holes > n * n * 0.005 && holes < n * n * 0.05,
         "cave mouths exist but are not everywhere (" + holes + ")");
      check(shaft <= 2, "almost no opening is a lone vertical shaft (" + shaft + ")");
      check(linked > holes / 2, "most mouths lead into the cave network (" + linked + "/" + holes + ")");
   }

   /** Absolute-Y profile + land/ocean rules + the same noise field above and below a mouth. */
   static void entrances() throws Exception {
      check(Caves7.profile(44) > 0.98 && Caves7.profile(5) == 0.0 && Caves7.profile(110) == 0.0,
         "density follows absolute world Y, not depth below the local surface");
      check(Caves7.profile(75) < Caves7.profile(44),
         "caves thin out towards high terrain, so mountains stay solid");
      check(Caves7.landBiome(Biomes.PLAINS, 70) && !Caves7.landBiome(Biomes.OCEAN, 70),
         "dry land opens, oceans stay sealed");
      TerraGen tg = new TerraGen(SEED, settings(7));
      Caves7 c = tg.caves.deep7;
      int mouths = 0, sameField = 0, noiseAtSurface = 0, straightDown = 0;
      int mask = Caves7.C_WORM | Caves7.C_CHEESE | Caves7.C_HALL;
      for (int z = -40; z < 40; z++) {
         for (int x = 0; x < 160; x++) {
            Gen.Col col = tg.column(x, z + 120);
            if (!Caves7.landBiome(col.biome, col.h)) continue;
            int top = c.cause(x, col.h, z + 120, col.h);
            if (top == 0) continue;
            mouths++;
            if ((top & mask) != 0) noiseAtSurface++;
            int wanted = top & mask;
            boolean direct = false, nearby = false;
            // Tunnels are SHEARED in Y, so a sloping mouth moves sideways as it
            // descends. Look for the very same noise layer in a small cone below.
            for (int d = 2; d <= 12 && !nearby; d += 2) {
               if ((c.cause(x, col.h - d, z + 120, col.h) & wanted) != 0) direct = true;
               for (int dz = -6; dz <= 6 && !nearby; dz++) {
                  for (int dx = -6; dx <= 6; dx++) {
                     if (dx * dx + dz * dz > 36) continue;
                     if ((c.cause(x + dx, col.h - d, z + 120 + dz, col.h) & wanted) != 0) {
                        nearby = true;
                        break;
                     }
                  }
               }
            }
            if (direct) straightDown++;
            if (nearby) sameField++;
         }
      }
      System.out.println("        mouths=" + mouths + " volume-continues=" + sameField
         + " also-straight-down=" + straightDown);
      check(mouths > 10, "the sample really has cave mouths (" + mouths + ")");
      check(noiseAtSurface == mouths, "every mouth is the cave volume itself, never a built aperture");
      check(sameField >= mouths * 9 / 10,
         "the same cave field continues below the opening (" + sameField + "/" + mouths + ")");
      check(straightDown < mouths,
         "most mouths lean sideways instead of being straight vertical holes");
      check(!c.surfaceOpen(0, 0, 40, false), "ocean floors never open at the surface");
      boolean sealed = true;
      for (int y = 37; y <= 40; y++) {
         if (c.carve(0, y, 0, 40, false) != Caves7.SOLID) sealed = false;
      }
      check(sealed, "ocean floors keep their seal with no drilled hole");
   }

   /** Plans are deterministic, all six build, and natural caves cut through their air. */
   static void structures() throws Exception {
      eq(6, CaveStructures7.NAME.length, "six integrated structure types");
      CaveStructures7.Plan a = CaveStructures7.plan(SEED, 3, -2);
      CaveStructures7.Plan b = CaveStructures7.plan(SEED, 3, -2);
      check(a.x == b.x && a.z == b.z && a.y == b.y && a.type == b.type,
         "structure plans are deterministic across chunk seams");
      TerraGen on = new TerraGen(SEED, settings(7));
      TerraGen off = new TerraGen(SEED, settingsNoStructures());
      Caves7 c = on.caves.deep7;
      int[] overlap = new int[6], air = new int[6], built = new int[6], sites = new int[6];
      for (int rz = -10; rz <= 10; rz++) {
         for (int rx = -10; rx <= 10; rx++) {
            CaveStructures7.Plan p = c.structures.plan(SEED, rx, rz);
            if (!p.valid || sites[p.type] >= 2) continue;
            sites[p.type]++;
            int h = on.column(p.x, p.z).h;
            int y = Math.max(12, Math.min(p.y, h - 14));
            for (int dx = -12; dx <= 12; dx++) {
               for (int dz = -12; dz <= 12; dz++) {
                  for (int dy = -6; dy <= 8; dy++) {
                     int wx = p.x + dx, wz = p.z + dz, wy = y + dy;
                     if (wy < 8 || wy > h - 6) continue;
                     int cause = c.cause(wx, wy, wz, h);
                     if ((cause & Caves7.C_STRUCT) == 0) continue;
                     air[p.type]++;
                     if ((cause & (Caves7.C_WORM | Caves7.C_CHEESE | Caves7.C_HALL)) != 0) overlap[p.type]++;
                  }
               }
            }
            // Real generated chunks, with and without the integrated structures.
            int cx = Math.floorDiv(p.x, 16), cz = Math.floorDiv(p.z, 16);
            Chunk with = on.generate(cx, cz), without = off.generate(cx, cz);
            int diff = 0;
            for (int lz = 0; lz < 16; lz++) {
               for (int lx = 0; lx < 16; lx++) {
                  for (int ly = Math.max(6, y - 8); ly <= Math.min(79, y + 9); ly++) {
                     if ((with.get(lx, ly, lz) & BlockState.ID_MASK)
                        != (without.get(lx, ly, lz) & BlockState.ID_MASK)) diff++;
                  }
               }
            }
            built[p.type] += diff;
         }
      }
      int overlappingTypes = 0;
      for (int t = 0; t < 6; t++) {
         check(sites[t] >= 1, CaveStructures7.NAME[t] + " plans exist");
         check(built[t] > 50, CaveStructures7.NAME[t] + " really builds blocks (" + built[t] + ")");
         check(air[t] > 0, CaveStructures7.NAME[t] + " carves air into the world");
         if (overlap[t] > 0) overlappingTypes++;
         System.out.println("        " + CaveStructures7.NAME[t] + ": air=" + air[t]
            + " cave-overlap=" + overlap[t] + " built=" + built[t]);
      }
      check(overlappingTypes >= 4,
         "natural caves intersect at least four structure types (" + overlappingTypes + ")");
   }

   static void oldSaves() throws Exception {
      Properties p = new Properties();
      settings(7).save(p);
      eq(7, WorldSettings.load(p).caveVer, "cave version 7 round-trips");
      p.setProperty("cavever", "99");
      eq(8, WorldSettings.load(p).caveVer, "future cave versions clamp to 8");
      for (int v = 1; v <= 6; v++) {
         Properties old = new Properties();
         settings(v).save(old);
         eq(v, WorldSettings.load(old).caveVer, "a V" + v + " save keeps its own carver");
      }
      Properties legacy = new Properties();
      legacy.setProperty("gentype", "0");
      eq(1, WorldSettings.load(legacy).caveVer, "pre-settings saves keep V6.21 caves");
      check(src("src/bc/Screens.java").contains("g.opt.caveCliffs ? 8 : 1"),
         "only Create New World selects caveVer 8");
   }

   static void wiring() throws Exception {
      eq("6.38", Game.VERSION_STR, "the game version is 6.37");
      eq("6.38", src("ci/release-trigger.txt").trim(), "the release workflow publishes 6.37");
      check(src("README.md").contains("V6.34"), "README describes V6.34");
      check(src("package/README.txt").contains("WHAT'S NEW IN V6.34"),
         "the packaged readme describes V6.34");
      check(new File("docs/releases/v6.34.md").isFile(), "V6.34 release notes exist");
      check(new File("docs/screenshots/v6.34-caves.png").isFile(), "the cave preview exists");
      check(src("src/bc/Trailer.java").contains("ws.caveVer = 8"), "the trailer uses the new caves");
      check(src("tools/build.py").contains("\"Caves7\"")
         && src("tools/verify_release.py").contains("\"Caves7\""),
         "the build receipt and verifier hash the new carver and structures");
      check(src("tools/build.py").contains("\"CaveStructures7\"")
         && src("tools/verify_release.py").contains("\"CaveStructures7\""),
         "the build receipt and verifier hash the integrated structures");
      check(src("docs/FILE-LAYOUT.md").contains("Caves7"), "the layout doc lists the new classes");
      check(src("tools/make_release.py").contains("if version == \"6.34\""),
         "the GitHub release notes describe 6.34");
   }
}
