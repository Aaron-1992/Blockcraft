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

   static void sources() {
      test("shader sources: the programs are well formed and the effects programs are the plain ones plus '#define FX'", () -> {
         lint("WORLD_VS", ShaderSrc.WORLD_VS);
         lint("WORLD_FS", ShaderSrc.WORLD_FS);
         lint("WATER_FS", ShaderSrc.WATER_FS);
         lint("WORLD_VS_FX", ShaderSrc.WORLD_VS_FX);
         lint("WORLD_FS_FX", ShaderSrc.WORLD_FS_FX);
         lint("WATER_FS_FX", ShaderSrc.WATER_FS_FX);
         lint("COMP_FS", ShaderSrc.COMP_FS);
         lint("CLOUD_FS", ShaderSrc.CLOUD_FS);
         lint("BRIGHT_FS", Shaders.BRIGHT_FS);
         lint("BLUR_FS", Shaders.BLUR_FS);
         String def = "#define FX 1\n";
         eq(ShaderSrc.WORLD_FS.length() + def.length(), ShaderSrc.WORLD_FS_FX.length(), "terrain program: only the define is added");
         eq(ShaderSrc.WORLD_VS.length() + def.length(), ShaderSrc.WORLD_VS_FX.length(), "vertex program: only the define is added");
         eq(ShaderSrc.WATER_FS.length() + def.length(), ShaderSrc.WATER_FS_FX.length(), "water program: only the define is added");
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
         Set<String> prog = names(java, "prog");
         check(prog.size() >= 20, "the terrain program gets its uniforms: " + prog.size());
         for (String u : prog) {
            check(world.contains(u), "the terrain program declares " + u);
         }

         Set<String> water = uniforms(ShaderSrc.WORLD_VS_FX);
         water.addAll(uniforms(ShaderSrc.WATER_FS_FX));
         for (String u : names(java, "this.pWater")) {
            check(water.contains(u), "the water program declares " + u);
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
         // whatever is listed is used by nothing that gives no light
         for (int id = 1; id < Blocks.MAX; id++) {
            if (Blocks.name[id] != null && Blocks.emit[id] < 12) {
               for (int f = 0; f < 6; f++) {
                  check(!tiles.contains(Blocks.tiles[id][f]), Blocks.name[id] + " (" + id + ") shares a glowing tile");
               }
            }
         }
      });
   }
}
