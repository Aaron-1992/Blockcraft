package bc;

import static bc.GameTests.check;
import static bc.GameTests.eq;
import static bc.GameTests.test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Headless tests of the graphics work of Update 3.19: the Looks (colour grading), the new options, the shader sources (no OpenGL here, so they are
 * checked for what a typo would break: balanced braces, matching #ifdef/#endif, every uniform the game sets is declared in the program) and the
 * table of glowing tiles.   python3 tools/runtests.py unit
 */
final class TestsGfx {
   static void run() {
      looks();
      options();
      sources();
      emissive();
      variation();
   }

   // ------------------------------------------------------------------ variation between blocks (Mesher)
   /** The opaque vertices (11 floats each: x y z u v r g b a m l) of the section with the ground of the flat test world, built with the given switches. */
   static float[] meshOf(Game g, boolean variation, boolean legacy) {
      boolean v0 = Mesher.variation;
      boolean l0 = Mesher.legacy;
      Mesher.variation = variation;
      Mesher.legacy = legacy;
      try {
         java.util.Arrays.fill(g.world.chunk(0, 0).grassColor, 0x79C05A);   // the flat test world has no biome colours: the grass would be black
         Mesher.Result r = Mesher.build(g.world, g.world.chunk(0, 0), GameTests.GROUND >> 4, true);
         float[] a = new float[r.nO * 11];
         if (r.opaque != null) {
            r.opaque.get(0, a);
            org.lwjgl.system.MemoryUtil.memFree(r.opaque);
         }

         if (r.trans != null) {
            org.lwjgl.system.MemoryUtil.memFree(r.trans);
         }

         return a;
      } finally {
         Mesher.variation = v0;
         Mesher.legacy = l0;
      }
   }

   static void variation() {
      test("variation: the blocks of a meadow differ a little in brightness and their tops are turned - shapes, light and face count stay", () -> {
         Game g = GameTests.newGame(0);
         float[] base = meshOf(g, false, false);
         float[] var = meshOf(g, true, false);
         check(base.length > 11 * 4 * 100, "the ground has many faces: " + base.length / 11 + " vertices");
         eq(base.length, var.length, "the same number of vertices");
         int n = base.length / 11;
         int differ = 0;
         int checked = 0;
         for (int i = 0; i < n; i++) {
            for (int k = 0; k < 3; k++) {
               check(base[i * 11 + k] == var[i * 11 + k], "positions are the same");
            }

            for (int k = 8; k < 11; k++) {
               check(base[i * 11 + k] == var[i * 11 + k], "alpha, material and light are the same (index " + k + ")");
            }

            float b = base[i * 11 + 5];
            if (b > 0.05F) {
               float ratio = var[i * 11 + 5] / b;
               check(ratio > 0.85F && ratio < 1.15F, "brightness stays within 15 %: " + ratio);
               checked++;
               if (Math.abs(ratio - 1.0F) > 0.004F) {
                  differ++;
               }

               // red, green and blue change by the same factor
               for (int k = 6; k <= 7; k++) {
                  if (base[i * 11 + k] > 0.05F) {
                     float rg = var[i * 11 + k] / base[i * 11 + k];
                     check(Math.abs(rg - ratio) < 0.003F, "the colour keeps its hue: " + rg + " vs " + ratio);
                  }
               }
            }
         }

         check(checked > 100 && differ > checked / 3, "many vertices got another brightness: " + differ + " of " + checked);
         // one quad = one factor
         int sameQuad = 0;
         for (int q = 0; q + 3 < n; q += 4) {
            float b0 = base[q * 11 + 5];
            if (b0 > 0.05F) {
               float r0 = var[q * 11 + 5] / b0;
               boolean same = true;
               for (int k = 1; k < 4; k++) {
                  float bk = base[(q + k) * 11 + 5];
                  if (bk > 0.05F && Math.abs(var[(q + k) * 11 + 5] / bk - r0) > 0.003F) {
                     same = false;
                  }
               }

               if (same) {
                  sameQuad++;
               }
            }
         }

         check(sameQuad > 100, "the four corners of a quad are scaled alike: " + sameQuad);
         // the tops of the grass: the texture is turned by a quarter turn in some quads, always inside its tile
         int top = 0;
         int turned = 0;
         for (int q = 0; q + 3 < n; q += 4) {
            boolean flat = true;
            float y0 = base[q * 11 + 1];
            for (int k = 1; k < 4; k++) {
               flat &= base[(q + k) * 11 + 1] == y0;
            }

            if (!flat || Math.abs(y0 - (GameTests.GROUND + 1 - ((GameTests.GROUND >> 4) << 4))) > 0.001F) {
               continue;
            }

            top++;
            boolean sameOrder = true;
            java.util.List<String> a = new java.util.ArrayList<>();
            java.util.List<String> b = new java.util.ArrayList<>();
            for (int k = 0; k < 4; k++) {
               a.add(base[(q + k) * 11 + 3] + "," + base[(q + k) * 11 + 4]);
               b.add(var[(q + k) * 11 + 3] + "," + var[(q + k) * 11 + 4]);
               sameOrder &= base[(q + k) * 11 + 3] == var[(q + k) * 11 + 3] && base[(q + k) * 11 + 4] == var[(q + k) * 11 + 4];
            }

            java.util.List<String> sa = new java.util.ArrayList<>(a);
            java.util.List<String> sb = new java.util.ArrayList<>(b);
            java.util.Collections.sort(sa);
            java.util.Collections.sort(sb);
            check(sa.equals(sb), "a turned top uses the same four texture corners");
            if (!sameOrder) {
               turned++;
            }
         }

         check(top >= 100, "the flat ground has 16 x 16 grass tops in the section: " + top);
         check(turned > top / 4 && turned < top, "some, not all tops are turned: " + turned + " of " + top);
      });
      test("variation: Update 3.5 compatibility mode (legacy) and -Dbc.novariation give the old picture", () -> {
         Game g = GameTests.newGame(0);
         float[] legacyOn = meshOf(g, true, true);
         float[] legacyOff = meshOf(g, false, true);
         check(java.util.Arrays.equals(legacyOn, legacyOff), "legacy meshes ignore the variation");
         float[] a = meshOf(g, false, false);
         float[] b = meshOf(g, false, false);
         check(java.util.Arrays.equals(a, b), "building the same section twice gives the same mesh");
         float[] c = meshOf(g, true, false);
         float[] d = meshOf(g, true, false);
         check(java.util.Arrays.equals(c, d), "the variation depends only on the position (a section rebuilt later looks the same)");
      });
      test("variation: the brightness table only holds natural kinds and sane amounts", () -> {
         for (int t = 0; t < Mesher.AMP.length; t++) {
            check(Mesher.AMP[t] >= 0.0F && Mesher.AMP[t] <= 0.13F, "tile " + t + " varies by " + Mesher.AMP[t]);
         }

         check(Mesher.ampOf(Tex.GRASS_TOP) > 0.0F && Mesher.ampOf(Tex.LEAVES) > Mesher.ampOf(Tex.GRASS_TOP), "grass varies, leaves more");
         eq(0.0F, Mesher.ampOf(Tex.PLANKS), "planks are made by hand: no variation");
         eq(0.0F, Mesher.ampOf(Tex.GLOWSTONE), "light sources keep their brightness");
         eq(0.0F, Mesher.ampOf(Tex.LAVA), "lava too");
         eq(0.0F, Mesher.ampOf(-5), "a bad tile number");
         eq(0.0F, Mesher.ampOf(100000), "a tile number far outside the table");
         for (int i = 0; i < 2000; i++) {
            int h = Mesher.hash3(i * 7 - 1000, i % 13, i * 3);
            float v = (h & 0xFFFF) / 32767.5F - 1.0F;
            check(v >= -1.0001F && v <= 1.0001F, "hash value in range");
         }
      });
   }

   // ------------------------------------------------------------------ the looks
   static void looks() {
      test("looks: every look has a name, a description and complete, sensible numbers", () -> {
         int n = Look.NAME.length;
         check(n >= 4, "at least four looks");
         eq(n, Look.COLOR.length, "colour codes");
         eq(n, Look.INFO.length, "descriptions");
         eq(n, Look.A.length, "grading numbers");
         eq(n, Look.SHADOW.length, "shadow tints");
         eq(n, Look.HIGH.length, "highlight tints");
         Set<String> names = new HashSet<>();
         for (int i = 0; i < n; i++) {
            check(names.add(Look.NAME[i]), "name " + Look.NAME[i] + " is unique");
            check(!Look.INFO[i].isEmpty() && Look.INFO[i].length() < 120, "description of " + Look.NAME[i] + " fits the tooltip");
            float[] a = Look.A[i];
            eq(7, a.length, "numbers of " + Look.NAME[i]);
            float[][] range = {{0.5F, 1.6F}, {0.0F, 0.8F}, {0.8F, 1.25F}, {0.0F, 0.8F}, {0.5F, 3.0F}, {0.0F, 0.06F}, {0.0F, 0.05F}};
            for (int k = 0; k < 7; k++) {
               check(Float.isFinite(a[k]) && a[k] >= range[k][0] && a[k] <= range[k][1], Look.NAME[i] + " number " + k + " = " + a[k]);
            }

            for (int k = 0; k < 3; k++) {
               check(Look.SHADOW[i][k] >= 0.6F && Look.SHADOW[i][k] <= 1.25F, Look.NAME[i] + " shadow tint " + k);
               check(Look.HIGH[i][k] >= 0.6F && Look.HIGH[i][k] <= 1.25F, Look.NAME[i] + " highlight tint " + k);
            }
         }
      });
      test("looks: Classic is the grading of Updates 3.11 - 3.18 (nobody is forced to a new picture)", () -> {
         eq("Classic", Look.NAME[Look.CLASSIC], "first look");
         float[] a = Look.A[Look.CLASSIC];
         check(a[0] == 1.10F && a[1] == 0.20F && a[2] == 1.03F && a[3] == 0.35F, "saturation, curve, exposure, vignette");
         check(a[4] == 1.0F && a[5] == 0.0F && a[6] == 0.0F, "no extra glow, no grain, no lift");
         for (int k = 0; k < 3; k++) {
            check(Look.SHADOW[Look.CLASSIC][k] == 1.0F && Look.HIGH[Look.CLASSIC][k] == 1.0F, "no tint " + k);
         }
      });
      test("looks: next() visits every look and comes back, clamp() keeps a bad number in the list", () -> {
         int at = Look.CLASSIC;
         Set<Integer> seen = new HashSet<>();
         for (int i = 0; i < Look.NAME.length; i++) {
            check(seen.add(at), "look " + at + " visited once");
            at = Look.next(at);
         }

         eq(Look.CLASSIC, at, "round trip");
         eq(0, Look.clamp(-7), "below");
         eq(Look.NAME.length - 1, Look.clamp(99), "above");
         check(Look.clamp(Look.DEFAULT) == Look.DEFAULT, "the default is a real look");
         check(Look.label(1).contains(Look.name(1)), "label contains the name");
      });
   }

   // ------------------------------------------------------------------ the options
   static void options() {
      test("options: look and effects level are saved, read back and clamped; an old file gets the defaults", () -> {
         File dir = new File(System.getProperty("java.io.tmpdir"), "bc-gfx-opt-" + System.nanoTime());
         dir.mkdirs();
         try {
            Options o = new Options(dir);
            eq(Look.DEFAULT, o.look, "default look");
            eq(1, o.fx, "default effects level");
            o.look = Look.CINEMATIC;
            o.fx = 2;
            o.save();
            Options r = new Options(dir);
            r.load();
            eq(Look.CINEMATIC, r.look, "look after the round trip");
            eq(2, r.fx, "effects level after the round trip");
            Files.write(new File(dir, "options.txt").toPath(), "look=99\nfx=-4\n".getBytes(StandardCharsets.UTF_8));
            Options c = new Options(dir);
            c.load();
            eq(Look.NAME.length - 1, c.look, "look is clamped");
            eq(0, c.fx, "effects level is clamped");
            Files.write(new File(dir, "options.txt").toPath(), "fov=80\n".getBytes(StandardCharsets.UTF_8));
            Options old = new Options(dir);
            old.load();
            eq(Look.DEFAULT, old.look, "old file: default look");
            eq(1, old.fx, "old file: default effects level");
            eq(80, old.fov, "old file: the other settings are still read");
         } finally {
            Tests311.deleteAll(dir);
         }
      });
   }

   // ------------------------------------------------------------------ the shader sources
   static final Pattern UNIFORM = Pattern.compile("uniform\\s+\\w+\\s+(\\w+)\\s*(\\[\\s*\\d+\\s*\\])?\\s*;");

   static Set<String> uniforms(String src) {
      Set<String> out = new HashSet<>();
      Matcher m = UNIFORM.matcher(src);
      while (m.find()) {
         out.add(m.group(1));
      }

      return out;
   }

   /** What a typo would break before any driver sees it: the version line first, balanced () {} [], every #ifdef closed, no uniform declared twice. */
   static void lint(String name, String src) {
      check(src.startsWith("#version 120\n"), name + ": the version line comes first");
      int[] depth = new int[3];
      for (String line : src.split("\n")) {
         int cut = line.indexOf("//");
         String code = cut >= 0 ? line.substring(0, cut) : line;
         for (char ch : code.toCharArray()) {
            switch (ch) {
               case '(' -> depth[0]++;
               case ')' -> depth[0]--;
               case '{' -> depth[1]++;
               case '}' -> depth[1]--;
               case '[' -> depth[2]++;
               case ']' -> depth[2]--;
               default -> {
               }
            }

            check(depth[0] >= 0 && depth[1] >= 0 && depth[2] >= 0, name + ": a bracket closes that was never opened: " + line.trim());
         }
      }

      eq(0, depth[0], name + ": parentheses balance");
      eq(0, depth[1], name + ": braces balance");
      eq(0, depth[2], name + ": square brackets balance");
      int opens = 0;
      int closes = 0;
      Set<String> seen = new HashSet<>();
      Matcher m = UNIFORM.matcher(src);
      while (m.find()) {
         check(seen.add(m.group(1)), name + ": uniform " + m.group(1) + " is declared once");
      }

      for (String line : src.split("\n")) {
         String t = line.trim();
         if (t.startsWith("#if")) {
            opens++;
         } else if (t.startsWith("#endif")) {
            closes++;
         }
      }

      eq(opens, closes, name + ": every #ifdef has its #endif");
      check(!src.contains("\t"), name + ": no tabs");
   }

   /** The uniform names that the code of Shaders.java hands to the given program ({@code this.u1f(this.pComp, "name", ..)}, {@code this.loc(this.pComp, "name[0]")}). */
   static Set<String> names(String javaSrc, String prog) {
      Set<String> out = new HashSet<>();
      Matcher m = Pattern.compile("(?:u1f|u1i|loc)\\(" + Pattern.quote(prog) + ",\\s*\"(\\w+)(?:\\[0\\])?\"").matcher(javaSrc);
      while (m.find()) {
         out.add(m.group(1));
      }

      return out;
   }

   /** The text of the method that starts with {@code signature}, up to its closing brace. */
   static String body(String javaSrc, String signature) {
      int at = javaSrc.indexOf(signature);
      check(at >= 0, "found " + signature);
      int open = javaSrc.indexOf('{', at);
      int depth = 0;
      for (int i = open; i < javaSrc.length(); i++) {
         char ch = javaSrc.charAt(i);
         if (ch == '{') {
            depth++;
         } else if (ch == '}' && --depth == 0) {
            return javaSrc.substring(at, i + 1);
         }
      }

      throw new IllegalStateException("no end of " + signature);
   }

   static void sources() {
      test("shader sources: the programs are well formed and the effects programs are the plain ones plus '#define FX'", () -> {
         lint("WORLD_VS", ShaderSrc.WORLD_VS);
         lint("WORLD_FS", ShaderSrc.WORLD_FS);
         lint("WATER_FS", ShaderSrc.WATER_FS);
         lint("WORLD_VS_FX", ShaderSrc.WORLD_VS_FX);
         lint("WORLD_FS_FX", ShaderSrc.WORLD_FS_FX);
         lint("WATER_FS_FX", ShaderSrc.WATER_FS_FX);
         lint("LOD_FS", ShaderSrc.LOD_FS);
         lint("LOD_FS_FX", ShaderSrc.LOD_FS_FX);
         lint("AO_FS", ShaderSrc.AO_FS);
         lint("AOBLUR_FS", ShaderSrc.AOBLUR_FS);
         lint("COMP_FS", ShaderSrc.COMP_FS);
         lint("CLOUD_FS", ShaderSrc.CLOUD_FS);
         lint("CLOUD_SHADOW_FS", ShaderSrc.CLOUD_SHADOW_FS);
         lint("BRIGHT_FS", Shaders.BRIGHT_FS);
         lint("BLUR_FS", Shaders.BLUR_FS);
         String def = "#define FX 1\n";
         eq(ShaderSrc.WORLD_FS.length() + def.length(), ShaderSrc.WORLD_FS_FX.length(), "terrain program: only the define is added");
         eq(ShaderSrc.WORLD_VS.length() + def.length(), ShaderSrc.WORLD_VS_FX.length(), "vertex program: only the define is added");
         eq(ShaderSrc.WATER_FS.length() + def.length(), ShaderSrc.WATER_FS_FX.length(), "water program: only the define is added");
         eq(ShaderSrc.LOD_FS.length() + def.length(), ShaderSrc.LOD_FS_FX.length(), "LOD program: only the define is added");
         check(ShaderSrc.LOD_FS.contains("#ifdef FX") && !ShaderSrc.LOD_FS.contains("#define FX"), "the cloud shadows of the LOD sit behind #ifdef FX");
         check(ShaderSrc.WORLD_FS_FX.startsWith("#version 120\n" + def), "define right after the version line");
         check(!ShaderSrc.WORLD_FS.contains("#define FX"), "the plain program has no define");
         check(ShaderSrc.WORLD_FS.contains("#ifdef FX") && ShaderSrc.WORLD_VS.contains("#ifdef FX"), "the new code sits behind #ifdef FX");
         check(ShaderSrc.WATER_FS_FX.contains("vEmis") && ShaderSrc.WATER_FS_FX.contains("WATER") == ShaderSrc.WATER_FS.contains("WATER"), "the water program carries the glow mask too");
         // the water program is spliced out of the terrain program: the markers that it needs must still be there
         check(ShaderSrc.WORLD_FS.contains("varying float vDepth;") && ShaderSrc.WORLD_FS.contains("  if (vWater > 0.5) {") && ShaderSrc.WORLD_FS.contains("  float f = clamp((fogRange.y - vDepth)"), "water splice markers");
      });
      test("shader sources: every uniform that the game sets is declared in the program that gets it", () -> {
         String java;
         try {
            java = new String(Files.readAllBytes(new File("src/bc/Shaders.java").toPath()), StandardCharsets.UTF_8);
         } catch (java.io.IOException e) {
            throw new RuntimeException(e);
         }

         Set<String> comp = uniforms(ShaderSrc.COMP_FS);
         Set<String> got = names(java, "this.pComp");
         check(got.size() >= 12, "the composite program gets its uniforms: " + got);
         for (String u : got) {
            check(comp.contains(u), "COMP_FS declares " + u);
         }

         Set<String> bright = uniforms(Shaders.BRIGHT_FS);
         for (String u : names(java, "this.pBright")) {
            check(bright.contains(u), "BRIGHT_FS declares " + u);
         }

         Set<String> world = uniforms(ShaderSrc.WORLD_VS_FX);
         world.addAll(uniforms(ShaderSrc.WORLD_FS_FX));
         Set<String> prog = names(body(java, "void useWorldProg("), "prog");
         check(prog.size() >= 25, "the terrain program gets its uniforms: " + prog.size());
         for (String u : prog) {
            check(world.contains(u), "the terrain program declares " + u);
         }

         Set<String> shadow = uniforms(ShaderSrc.CLOUD_SHADOW_FS);
         Set<String> sgot = names(java, "this.pCShadow");
         sgot.addAll(names(body(java, "void weatherUniforms("), "prog"));
         check(sgot.size() >= 12, "the cloud shadow program gets its uniforms: " + sgot);
         for (String u : sgot) {
            check(shadow.contains(u), "CLOUD_SHADOW_FS declares " + u);
         }

         Set<String> cloud = uniforms(ShaderSrc.CLOUD_FS);
         for (String u : names(body(java, "void weatherUniforms("), "prog")) {
            check(cloud.contains(u), "CLOUD_FS declares " + u);
         }

         check(prog.contains("cloudShadow") && prog.contains("csP") && prog.contains("wet"), "cloud shadows and wetness are handed to the terrain program");
         Set<String> water = uniforms(ShaderSrc.WORLD_VS_FX);
         water.addAll(uniforms(ShaderSrc.WATER_FS_FX));
         Set<String> wgot = names(body(java, "public void useWater("), "wp");
         check(wgot.size() >= 5, "the water program gets its uniforms: " + wgot);
         for (String u : wgot) {
            check(water.contains(u), "the water program declares " + u);
         }

         Set<String> lod = uniforms(ShaderSrc.LOD_VS);
         lod.addAll(uniforms(ShaderSrc.LOD_FS_FX));
         Set<String> lgot = names(body(java, "public void useLod("), "lp");
         check(lgot.size() >= 12, "the LOD program gets its uniforms: " + lgot);
         for (String u : lgot) {
            check(lod.contains(u), "the LOD program declares " + u);
         }

         check(lgot.contains("cloudShadow") && lgot.contains("csP"), "the cloud shadows are handed to the LOD program");
         Set<String> ao = uniforms(ShaderSrc.AO_FS);
         for (String u : names(java, "this.pAo")) {
            check(ao.contains(u), "AO_FS declares " + u);
         }

         Set<String> aob = uniforms(ShaderSrc.AOBLUR_FS);
         for (String u : names(java, "this.pAoBlur")) {
            check(aob.contains(u), "AOBLUR_FS declares " + u);
         }

         check(world.contains("emT") && prog.contains("emT"), "the glowing tiles are handed to the terrain program");
      });
   }

   // ------------------------------------------------------------------ the glowing tiles
   static void emissive() {
      test("glow: the tiles of lava, glowstone, torches and fire glow - cobblestone and the pumpkin's skin do not", () -> {
         float[] e = Shaders.computeEmissive();
         eq(32, e.length, "sixteen (tile, strength) pairs");
         Set<Integer> tiles = new HashSet<>();
         int n = 0;
         for (int i = 0; i < 16; i++) {
            if (e[i * 2 + 1] > 0.0F) {
               n++;
               check(tiles.add((int)e[i * 2]), "tile " + (int)e[i * 2] + " is listed once");
               check(e[i * 2] >= 0 && e[i * 2] < Tex.COLS * Tex.COLS, "tile is inside the atlas");
            }
         }

         check(n >= 5, "at least lava, glowstone, torch, fire, lit furnace: " + n);
         check(tiles.contains(Blocks.tiles[Blocks.LAVA][0]), "lava");
         check(tiles.contains(Blocks.tiles[Blocks.GLOWSTONE][0]), "glowstone");
         check(tiles.contains(Blocks.tiles[Blocks.TORCH][0]), "torch");
         check(tiles.contains(Blocks.tiles[Blocks.FIRE][0]), "fire");
         check(!tiles.contains(Tex.COBBLE) && !tiles.contains(Tex.STONE) && !tiles.contains(Tex.DIRT), "cobblestone, stone, dirt");
         // small light sources glow strongly, a lava lake less (its glow would cover half the screen)
         float lava = 0.0F;
         float torch = 0.0F;
         for (int i = 0; i < 16; i++) {
            check(e[i * 2 + 1] >= 0.0F && e[i * 2 + 1] <= 1.0F, "strength between 0 and 1");
            if ((int)e[i * 2] == Blocks.tiles[Blocks.LAVA][0] && e[i * 2 + 1] > 0.0F) {
               lava = e[i * 2 + 1];
            }

            if ((int)e[i * 2] == Blocks.tiles[Blocks.TORCH][0] && e[i * 2 + 1] > 0.0F) {
               torch = e[i * 2 + 1];
            }
         }

         check(lava > 0.0F && torch > lava, "a torch glows more than lava: " + torch + " vs " + lava);
         // Powered wire and observer indicators use visual emission without block-light propagation.
         for (int id = 1; id < Blocks.MAX; id++) {
            if (Blocks.name[id] != null && Blocks.emit[id] == 0 && !Redstone319.observerOn(id)
               && !(Redstone319.isWire(id)&&id>Redstone319.REDSTONE_WIRE0)) {
               for (int f = 0; f < 6; f++) {
                  check(!tiles.contains(Blocks.tiles[id][f]), Blocks.name[id] + " (" + id + ") shares a glowing tile");
               }
            }
         }
      });
   }
}
