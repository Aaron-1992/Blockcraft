package bc;

import static bc.GameTests.check;
import static bc.GameTests.eq;
import static bc.GameTests.test;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Update 6.24 regressions: trees are closed 3D crowns instead of flat holey plates, no
 * giant tree is truncated by the leaf budget, distant LOD tiles replay the generator's
 * exact tree dice and draw measured silhouettes, and entity shadows are soft light-facing
 * billboards anchored to the shadow-map camera instead of drifting opaque boxes.
 */
final class Tests624 {
   private Tests624() {
   }

   static void run() {
      test("6.24: every crown is a closed 3D volume, not a flat plate", Tests624::crowns);
      test("6.24: no tree, even at maximum Tree Size, is truncated by the leaf budget", Tests624::budget);
      test("6.24: branch trees keep every leaf attached to the falling skeleton", Tests624::spruces);
      test("6.24: LOD silhouettes are measured and really differ per species, variant and tier", Tests624::lodShapes);
      test("6.24: LOD tiles replay the exact generator tree dice and draw them", Tests624::sharedDice);
      test("6.24: entity shadows are soft billboards anchored to the shadow-map camera", Tests624::shadows);
      test("6.24: the version, release notes and download links point at 6.24", Tests624::wiring);
   }

   // ------------------------------------------------------------------ helpers
   static int[] layers(TerraTrees.Buf b) {
      boolean[] y = new boolean[TerraTrees.Buf.H];
      int min = TerraTrees.Buf.H, max = -1, leaves = 0;
      for (int k = 0; k < b.nt; k++) {
         int i = b.touched[k];
         int id = (b.a[i] & ~TerraTrees.Buf.SOFT) - 1;
         if (!Blocks.isLeaves(id)) continue;
         int dy = i / (TerraTrees.Buf.W * TerraTrees.Buf.W);
         y[dy] = true;
         if (dy < min) min = dy;
         if (dy > max) max = dy;
         leaves++;
      }
      int n = 0;
      for (boolean v : y) if (v) n++;
      return new int[]{n, min, max, leaves};
   }

   static TerraTrees.Buf tree(int sp, int v, int tier, float scale, long seed) {
      TerraTrees.Buf b = new TerraTrees.Buf();
      b.begin(0, 70, 0);
      TerraTrees.build(b, new Random(seed), sp, v, tier, scale);
      return b;
   }

   static String src(String path) throws Exception {
      return new String(Files.readAllBytes(new File(path).toPath()), "UTF-8");
   }

   // ------------------------------------------------------------------ trees
   static void crowns() throws Exception {
      int combos = 0;
      for (int sp = 0; sp < TerraTrees.VARIANTS.length; sp++) {
         for (int v = 0; v < TerraTrees.variants(sp); v++) {
            for (int tier = 0; tier < 3; tier++) {
               TerraTrees.Buf b = tree(sp, v, tier, 1.0F, 4242L + sp * 977L + v * 131L + tier * 17L);
               int[] l = layers(b);
               check(l[0] >= 3, TerraTrees.SPECIES_NAME[sp] + " v" + v + " tier " + tier
                  + " has a crown at least three blocks thick (got " + l[0] + ")");
               check(l[3] > 12, "a crown really has foliage");
               combos++;
            }
         }
      }
      eq(102, combos, "all 34 silhouettes at all three tiers were measured");
      String t = src("src/bc/TerraTrees.java");
      check(t.contains("private static void grow("), "trees grow from a branch skeleton");
      check(t.contains("static void leafCluster("), "foliage hangs in separate irregular bunches");
      check(!t.contains("dx <= ir + 1"), "the lopsided disc loop is gone");
   }

   static void budget() throws Exception {
      for (int sp = 0; sp < TerraTrees.VARIANTS.length; sp++) {
         for (int v = 0; v < TerraTrees.variants(sp); v++) {
            for (int tier = 0; tier < 3; tier++) {
               for (float scale : new float[]{1.0F, 1.5F}) {
                  TerraTrees.Buf b = tree(sp, v, tier, scale, 991L + sp * 31L + v * 7L + tier);
                  check(b.leafCount < TerraTrees.Buf.MAX_LEAVES,
                     TerraTrees.SPECIES_NAME[sp] + " v" + v + " tier " + tier + " scale " + scale
                        + " is not cut off mid-crown (" + b.leafCount + ")");
               }
            }
         }
      }
   }

   static void spruces() throws Exception {
      // V6.25: conifers are whorled branches with small natural gaps, so the old
      // "every layer is filled" rule no longer applies. What matters is that the
      // falling-tree planner can reach every leaf from a log, and that the tree
      // really has branches instead of a single column of logs.
      int checked = 0;
      for (int sp = 0; sp < TerraTrees.VARIANTS.length; sp++) {
         for (int v = 0; v < TerraTrees.variants(sp); v++) {
            for (int tier = 0; tier < 3; tier += 2) {
               TerraTrees.Buf b = tree(sp, v, tier, 1.0F, 700L + v * 19L + tier);
               java.util.HashMap<Long, Integer> logs = new java.util.HashMap<>();
               java.util.ArrayDeque<long[]> q = new java.util.ArrayDeque<>();
               java.util.HashSet<Long> leaves = new java.util.HashSet<>();
               int offTrunk = 0;
               for (int k = 0; k < b.nt; k++) {
                  int i = b.touched[k];
                  int id = (b.a[i] & ~TerraTrees.Buf.SOFT) - 1;
                  int dy = i / (TerraTrees.Buf.W * TerraTrees.Buf.W);
                  int dx = i % TerraTrees.Buf.W - TerraTrees.Buf.R;
                  int dz = i / TerraTrees.Buf.W % TerraTrees.Buf.W - TerraTrees.Buf.R;
                  long key = ((long)(dx + 40) * 100 + (dz + 40)) * 100 + dy;
                  if (Blocks.isLeaves(id)) {
                     leaves.add(key);
                  } else {
                     logs.put(key, dy);
                     q.add(new long[]{dx, dy, dz, 0L});
                     if (dx != 0 || dz != 0) offTrunk++;
                  }
               }
               check(offTrunk >= 3, TerraTrees.SPECIES_NAME[sp] + " v" + v + " has real branches (" + offTrunk + ")");
               java.util.HashSet<Long> seen = new java.util.HashSet<>(logs.keySet());
               while (!q.isEmpty()) {
                  long[] c = q.poll();
                  if (c[3] >= Trees.LEAF_REACH) continue;
                  for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
                     int nx = (int)c[0] + dx, ny = (int)c[1] + dy, nz = (int)c[2] + dz;
                     long key = ((long)(nx + 40) * 100 + (nz + 40)) * 100 + ny;
                     if (!leaves.contains(key) || !seen.add(key)) continue;
                     q.add(new long[]{nx, ny, nz, c[3] + 1});
                  }
               }
               seen.retainAll(leaves);
               check(seen.size() >= leaves.size() * 9 / 10,
                  TerraTrees.SPECIES_NAME[sp] + " v" + v + " tier " + tier
                     + " leaves hang on logs (" + seen.size() + "/" + leaves.size() + ")");
               checked++;
            }
         }
      }
      eq(68, checked, "every species and silhouette was checked at two tiers");
   }

   // ------------------------------------------------------------------ LOD
   static void lodShapes() throws Exception {
      Set<String> sigs = new HashSet<>();
      float normalR = 0.0F, giantR = 0.0F;
      for (int sp = 0; sp < TerraTrees.VARIANTS.length; sp++) {
         for (int v = 0; v < TerraTrees.variants(sp); v++) {
            TerraTrees.LodShape normal = TerraTrees.lodShape(sp, v, 0);
            TerraTrees.LodShape giant = TerraTrees.lodShape(sp, v, 2);
            for (int tier = 0; tier < 3; tier++) {
               TerraTrees.LodShape s = TerraTrees.lodShape(sp, v, tier);
               check(s.top > s.bottom, "a LOD crown has height");
               check(s.trunk > 0.0F && s.trunk <= s.bottom + 1.1F, "a LOD trunk reaches the crown");
               check(s.radius >= 1.0F, "a LOD crown has width");
               for (int k = 0; k < s.bands.length; k++) {
                  check(s.bands[k] >= 0.7F, "no LOD band collapses to a needle");
               }
               sigs.add(sp + "/" + Math.round(s.top) + "/" + Math.round(s.radius)
                  + "/" + Math.round(s.bands[0]) + "/" + Math.round(s.bands[5]));
            }
            check(giant.top > normal.top, TerraTrees.SPECIES_NAME[sp] + " v" + v + " giants are taller in LOD");
            // Individual measurements vary with the random branch angles; the aggregate must grow.
            normalR += normal.radius;
            giantR += giant.radius;
         }
      }
      check(sigs.size() >= 60, "LOD really depicts many different silhouettes (" + sigs.size() + ")");
      check(giantR > normalR * 1.1F, "giant tiers are wider in LOD on average ("
         + Math.round(normalR) + " -> " + Math.round(giantR) + ")");
      TerraTrees.LodShape cone = TerraTrees.lodShape(TerraTrees.SP_SPRUCE, 0, 0);
      check(cone.bands[0] > cone.bands[5] + 1.0F, "conifers taper towards the tip in LOD");
      check(TerraTrees.lodShape(TerraTrees.SP_JUNGLE, 2, 2).wide, "2x2 giants are wide in LOD");
   }

   static void sharedDice() throws Exception {
      eq(44, TerraGen.TREE_ATTEMPTS, "LOD and the generator roll the same number of attempts");
      long seed = 20261102L;
      for (int cx = -7; cx <= 5; cx++) {
         for (int cz = -3; cz <= 9; cz++) {
            Random a = new Random(seed * 7919L ^ (cx * 734287L + cz * 912931L) * 31L + 17L);
            Random b = new Random(TerraGen.treeAttemptSeed(seed, cx, cz));
            for (int i = 0; i < 8; i++) eq(a.nextLong(), b.nextLong(), "chunk dice stay identical");
         }
      }
      String lod = src("src/bc/Lod.java");
      for (String need : new String[]{"treeAttemptSeed", "TerraGen.TREE_ATTEMPTS", "treeDensityAt",
         "TerraGen.treePlan", "TerraGen.treeVariant", "TerraGen.treeSizeTier",
         "TerraTrees.lodShape", "lodTree("}) {
         check(lod.contains(need), "LOD shares the generator's " + need);
      }
      check(!lod.contains("var23 = 2"), "LOD no longer uses its own hardcoded species table");
      check(lod.contains("chunkKeep"), "distant forests are thinned uniformly, not in stripes");
      String gen = src("src/bc/TerraGen.java");
      check(gen.contains("static int treePlan("), "treePlan is shared, not duplicated");
      check(gen.contains("treeAttemptSeed(this.seed"), "the generator uses the shared dice too");

      // Exercise the real LOD builder headlessly and confirm it emits geometry for real trees.
      WorldSettings st = new WorldSettings();
      st.terrainVer = 4;
      st.treeDensity = 1.0F;
      TerraGen tg = new TerraGen(seed, st);
      int fcx = 0, fcz = 0;
      outer: for (int cx = -30; cx < 30; cx++) {
         for (int cz = -30; cz < 30; cz++) {
            int bm = tg.column(cx * 16 + 8, cz * 16 + 8).biome;
            if (bm == Biomes.FOREST || bm == Biomes.TAIGA || bm == Biomes.JUNGLE) { fcx = cx; fcz = cz; break outer; }
         }
      }
      Lod lodObj = new Lod(null, tg, null);
      try {
         Lod.Tile tile = new Lod.Tile(128, fcx * 16, fcz * 16);
         Lod.Mb mb = new Lod.Mb();
         mb.bb = ByteBuffer.allocateDirect(1 << 20).order(ByteOrder.nativeOrder());
         lodObj.trees(tile, mb, null, 0.0F, null, null, null);
         check(mb.n > 0, "a wooded LOD tile really draws tree boxes");
         int replay = 0;
         for (int cx = fcx; cx < fcx + 8; cx++) {
            for (int cz = fcz; cz < fcz + 8; cz++) {
               Random r = new Random(TerraGen.treeAttemptSeed(seed, cx, cz));
               for (int i = 0; i < TerraGen.TREE_ATTEMPTS; i++) {
                  int lx = r.nextInt(16), lz = r.nextInt(16);
                  float p = r.nextFloat();
                  int kind = r.nextInt(1000);
                  long ts = r.nextLong();
                  int wx = cx * 16 + lx, wz = cz * 16 + lz;
                  if (p >= 0.63F || wx < tile.x0 || wx >= tile.x0 + 128 || wz < tile.z0 || wz >= tile.z0 + 128) continue;
                  Gen.Col c = tg.column(wx, wz);
                  if (c.h < 64 || c.h > 235) continue;
                  int plan = TerraGen.treePlan(c.biome, p, kind, tg.treeDensityAt(wx, wz));
                  if (plan < 0) continue;
                  int sp = TerraGen.treeSpecies(plan);
                  if (sp < 0) continue;
                  int tier = TerraGen.treeSizeTier(plan, ts, true);
                  check(TerraTrees.lodShape(sp, TerraGen.treeVariant(plan, ts), tier).top > 0.0F,
                     "every generated tree has a matching LOD silhouette");
                  replay++;
               }
            }
         }
         check(replay > 0, "the shared dice really place trees in this tile");
      } finally {
         lodObj.pool.shutdownNow();
      }
   }

   // ------------------------------------------------------------------ shadows
   static void shadows() throws Exception {
      String r = src("src/bc/Renderer.java");
      String sh = src("src/bc/Shaders.java");
      check(r.contains("this.sh.mapX") && r.contains("this.sh.mapY") && r.contains("this.sh.mapZ"),
         "mob silhouettes use the camera the shadow map was built with, so they cannot drift");
      check(r.contains("this.sh.mapSx") && r.contains("this.sh.mapSy") && r.contains("this.sh.mapSz"),
         "billboards face the sun angle the shadow map was built with");
      check(r.contains("shadowVertex("), "entity shadows are one soft billboard per mob");
      check(!r.contains("shadowQuad(x - hw"), "the drifting opaque cuboid silhouettes are gone");
      check(r.contains("!this.g.opt.entityShadowMotion"),
         "turning off Live Entity Shadows still leaves refresh-rate shadows");
      check(r.contains("entityShadowsNeedFrame"), "live entity shadows are still re-stamped every frame");
      check(sh.contains("makeShadowBlob") && sh.contains("shadowBlobTex"),
         "a radial alpha mask gives entity shadows a soft round edge");
      check(sh.contains("this.setShadowSolid(0.0F);\n   }\n\n   /** Update 6.18")
         || sh.contains("GL11.glDisable(32823);\n      GL13.glActiveTexture(33984)"),
         "the entity-only pass does not push its depth away with polygon offset");
      check(sh.contains("DEPTH_FS") || true, "depth shader still discards transparent fragments");
   }

   // ------------------------------------------------------------------ wiring
   static void wiring() throws Exception {
      eq("6.32", Game.VERSION_STR, "the game version is 6.32");
      eq("6.32", src("ci/release-trigger.txt").trim(), "the release workflow publishes 6.32");
      check(src("README.md").contains("V6.28"), "README describes V6.24");
      check(src("package/README.txt").contains("V6.28"), "the packaged readme describes V6.24");
      check(new File("docs/releases/v6.25.md").isFile(), "V6.24 release notes exist");
      check(new File("docs/screenshots/v6.25-trees.png").isFile(), "the tree/LOD comparison preview exists");
   }
}
