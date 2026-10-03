package bc;

import static bc.GameTests.GROUND;
import static bc.GameTests.check;
import static bc.GameTests.eq;
import static bc.GameTests.newGame;
import static bc.GameTests.test;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Headless tests of Update 3.9: skins (import, safety, formats, slim detection, Mojang answers), the player model's texture layout,
 * the body animation state, the third-person camera and the options.   python3 tools/runtests.py unit
 */
final class Tests39 {
   static void run() {
      names();
      files();
      formats();
      slim();
      mojang();
      layout();
      defaults();
      pose();
      camera();
      options();
      polish();
      steps();
      creepers();
      fireLook();
   }

   static File tmpDir() throws IOException {
      File d = Files.createTempDirectory("bc-skins").toFile();
      d.deleteOnExit();
      return d;
   }

   static File writePng(File dir, String name, int w, int h, java.util.function.IntBinaryOperator color) throws IOException {
      BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
      for (int y = 0; y < h; y++) {
         for (int x = 0; x < w; x++) {
            img.setRGB(x, y, color.applyAsInt(x, y));
         }
      }

      File f = new File(dir, name);
      ImageIO.write(img, "png", f);
      return f;
   }

   // ------------------------------------------------------------------ names (path traversal, odd characters)
   static void names() {
      test("skin file names are made safe: no paths, no odd characters, never empty, always .png", () -> {
         eq("evil.png", Skins.safeName("../../evil.png"), "parent directories");
         eq("y_z.png", Skins.safeName("C:\\x\\y z.png"), "Windows path and a space");
         eq("skin.png", Skins.safeName("???"), "nothing usable left");
         eq("skin.png", Skins.safeName(""), "empty");
         eq("skin.png", Skins.safeName(null), "null");
         eq("Steve_2.png", Skins.safeName("Steve.2"), "a dot");
         eq("a-b_c.png", Skins.safeName("a-b_c.PNG"), "extension in capitals");
         check(Skins.safeName("x".repeat(200)).length() == 44, "long names are cut to 40 characters + .png");
         check(!Skins.safeName("con:/\\*?\"<>|.png").contains("/") && !Skins.safeName("a/b").contains("/"), "no slashes");
      });
   }

   // ------------------------------------------------------------------ importing
   static void files() {
      test("importing: a skin is copied into the skins folder, listed, loaded; junk is refused", () -> {
         File root = tmpDir();
         Skins sk = new Skins(root);
         check(sk.list().isEmpty(), "starts empty");
         eq(List.of(Skins.DEFAULT), sk.choices(), "the default is always offered");
         File src = writePng(tmpDir(), "my skin.png", 64, 64, (x, y) -> 0xFF000000 | x * 4 << 16 | y * 4 << 8 | 77);
         String name = sk.importFile(src);
         eq("my_skin.png", name, "name made safe");
         eq(List.of("my_skin.png"), sk.list(), "listed");
         int[] px = sk.pixels(name);
         eq(4096, px.length, "64x64");
         eq(0xFF000000 | 10 * 4 << 16 | 20 * 4 << 8 | 77, px[20 * 64 + 10], "pixel (10,20) survived");
         String again = sk.importFile(src);
         eq("my_skin-2.png", again, "the same file name twice does not overwrite");
         eq(2, sk.list().size(), "two files");
         // junk
         File txt = new File(tmpDir(), "notes.png");
         Files.write(txt.toPath(), "this is not an image".getBytes(StandardCharsets.UTF_8));
         boolean threw = false;
         try {
            sk.importFile(txt);
         } catch (IOException e) {
            threw = true;
         }
         check(threw, "text with a .png name is refused");
         File wrong = writePng(tmpDir(), "wide.png", 50, 50, (x, y) -> -1);
         threw = false;
         try {
            sk.importFile(wrong);
         } catch (IOException e) {
            threw = true;
            check(e.getMessage().contains("64x64"), "the message says what a skin is: " + e.getMessage());
         }
         check(threw, "a 50x50 picture is refused");
         File missing = new File(tmpDir(), "nothing.png");
         threw = false;
         try {
            sk.importFile(missing);
         } catch (IOException e) {
            threw = true;
         }
         check(threw, "a missing file is refused");
         // an unknown / broken name falls back to the default skin, never crashes
         check(sk.pixels("ghost.png") == Skins.defaultPixels(), "an unknown skin shows the default");
         check(sk.pixels("../../etc/passwd") == Skins.defaultPixels(), "a path in the name cannot leave the folder");
         // a file already in the folder is not copied onto itself
         File inFolder = writePng(sk.dir, "inplace.png", 64, 64, (x, y) -> 0xFF112233);
         eq("inplace.png", sk.importFile(inFolder), "already in the folder: kept");
      });
   }

   static void formats() {
      test("formats: old 64x32 skins get mirrored left limbs, HD skins are scaled down, an HD legacy skin works too", () -> {
         File dir = tmpDir();
         // legacy 64x32: give every pixel of the right leg and right arm a unique colour
         File old = writePng(dir, "old.png", 64, 32, (x, y) -> 0xFF000000 | (x & 63) << 16 | (y & 63) << 8 | 9);
         int[] px = Skins.load(old);
         eq(4096, px.length, "converted to 64x64");
         // right leg front face is (4,20)-(8,32); the left leg's front face is the mirror image at (20,52)
         for (int y = 0; y < 12; y++) {
            for (int x = 0; x < 4; x++) {
               eq(px[(20 + y) * 64 + 4 + (3 - x)], px[(52 + y) * 64 + 20 + x], "left leg front is the mirrored right leg front (" + x + "," + y + ")");
            }
         }
         // the side faces swap: the left leg's RIGHT face is the mirror of the right leg's LEFT face
         for (int y = 0; y < 12; y++) {
            for (int x = 0; x < 4; x++) {
               eq(px[(20 + y) * 64 + 8 + (3 - x)], px[(52 + y) * 64 + 16 + x], "left leg right face = mirrored right leg left face");
               eq(px[(20 + y) * 64 + 0 + (3 - x)], px[(52 + y) * 64 + 24 + x], "left leg left face = mirrored right leg right face");
            }
         }
         // the same for the arm (40,16) -> (32,48): front faces
         for (int y = 0; y < 12; y++) {
            for (int x = 0; x < 4; x++) {
               eq(px[(20 + y) * 64 + 44 + (3 - x)], px[(52 + y) * 64 + 36 + x], "left arm front is the mirrored right arm front");
            }
         }
         // HD: 128x128 where every 2x2 block has the colour of its (x, y) in the 64x64 grid
         File hd = writePng(dir, "hd.png", 128, 128, (x, y) -> 0xFF000000 | (x / 2) << 16 | (y / 2) << 8 | 5);
         int[] hp = Skins.load(hd);
         eq(0xFF000000 | 17 << 16 | 40 << 8 | 5, hp[40 * 64 + 17], "HD skin scaled down to 64x64");
         File hdLegacy = writePng(dir, "hdold.png", 128, 64, (x, y) -> 0xFF202020);
         eq(4096, Skins.load(hdLegacy).length, "128x64 (HD legacy) works");
      });
   }

   static void slim() {
      test("slim (Alex) arms are detected from the unused pixel column; the choice is remembered", () -> {
         int[] classic = Skins.defaultSkin();
         check(!Skins.looksSlim(classic), "the default skin has classic arms");
         int[] demo = Skins.demoSkin();
         check(Skins.looksSlim(demo), "the demo skin has slim arms");
         int[] oneBack = demo.clone();
         oneBack[25 * 64 + 54] = 0xFF123456;
         check(!Skins.looksSlim(oneBack), "one opaque pixel in the right arm's 4th column makes it classic");
         int[] oneLeft = demo.clone();
         oneLeft[60 * 64 + 47] = 0xFF123456;
         check(!Skins.looksSlim(oneLeft), "and in the left arm's");
         File root = tmpDir();
         Skins sk = new Skins(root);
         File f = writePng(sk.dir, "alex.png", 64, 64, (x, y) -> (x == 54 || x == 55) && y >= 20 && y < 32 || (x == 46 || x == 47) && y >= 52 ? 0 : 0xFF808080);
         check(sk.slim("alex.png"), "auto-detected as slim");
         sk.setSlim("alex.png", false);
         check(!sk.slim("alex.png"), "set by hand to classic");
         Skins again = new Skins(root);
         check(!again.slim("alex.png"), "the choice is stored in skins.properties");
         check(!again.slim(Skins.DEFAULT) && !again.slim(null), "the default skin is classic");
      });
   }

   static void mojang() {
      test("Mojang answers: account id and skin link / model are read from the JSON; bad input is rejected", () -> {
         eq("069a79f444e94726a5befca90e38aaf5", Skins.profileId("{\"id\":\"069a79f444e94726a5befca90e38aaf5\",\"name\":\"Notch\"}"), "profile id");
         eq(null, Skins.profileId("{\"error\":\"not found\"}"), "no id");
         String tex = "{\"timestamp\":1,\"profileId\":\"x\",\"textures\":{\"SKIN\":{\"url\":\"http://textures.minecraft.net/texture/abc123\",\"metadata\":{\"model\":\"slim\"}}}}";
         String b64 = Base64.getEncoder().encodeToString(tex.getBytes(StandardCharsets.UTF_8));
         String json = "{\"id\":\"069a79f444e94726a5befca90e38aaf5\",\"name\":\"Notch\",\"properties\":[{\"name\":\"textures\",\"value\":\"" + b64 + "\"}]}";
         String[] info = Skins.textureInfo(json);
         check(info != null, "found");
         eq("http://textures.minecraft.net/texture/abc123", info[0], "url");
         eq("slim", info[1], "model");
         String classic = "{\"textures\":{\"SKIN\":{\"url\":\"https://textures.minecraft.net/texture/zzz\"}}}";
         String[] c = Skins.textureInfo("{\"properties\":[{\"name\":\"textures\",\"value\":\"" + Base64.getEncoder().encodeToString(classic.getBytes(StandardCharsets.UTF_8)) + "\"}]}");
         eq("default", c[1], "no metadata = the classic model");
         eq(null, Skins.textureInfo("{\"properties\":[]}"), "no textures property");
         eq(null, Skins.textureInfo("{\"properties\":[{\"name\":\"textures\",\"value\":\"!!!notbase64\"}]}"), "garbage");
         eq(null, Skins.textureInfo("{\"properties\":[{\"name\":\"textures\",\"value\":\"" + Base64.getEncoder().encodeToString("{\"textures\":{}}".getBytes(StandardCharsets.UTF_8)) + "\"}]}"), "an account without a custom skin");
         // input checks (no network is used for these)
         Skins sk = new Skins(tmpDir());
         sk.importUser("bad name!");
         check(!sk.busy && sk.status.contains("1-16"), "a bad username is refused with a hint: " + sk.status);
         sk.importUser("");
         check(!sk.busy, "an empty username is refused");
         sk.importUrl("file:///etc/passwd");
         check(!sk.busy && sk.status.contains("link"), "only http(s) links: " + sk.status);
         File path = writePng(tmpDir(), "pasted.png", 64, 64, (x, y) -> 0xFF445566);
         eq(path, sk.importFromText("\"" + path.getPath() + "\""), "a pasted path (in quotes, as Windows copies it) is recognised");
         eq(null, sk.importFromText("   "), "an empty clipboard does nothing");
         check(sk.status.contains("empty"), "and says so");
      });
   }

   // ------------------------------------------------------------------ the model's texture layout
   static void layout() {
      test("player model: the UV rectangles are Minecraft's skin layout (head, body, arms classic and slim, legs, second layer)", () -> {
         int[][] head = PlayerModel.faceRects(0, 0, 8, 8, 8);
         check(java.util.Arrays.equals(new int[]{8, 8, 16, 16}, head[3]), "head front is (8,8)-(16,16)");
         check(java.util.Arrays.equals(new int[]{8, 0, 16, 8}, head[0]), "head top");
         check(java.util.Arrays.equals(new int[]{16, 0, 24, 8}, head[1]), "head bottom");
         check(java.util.Arrays.equals(new int[]{0, 8, 8, 16}, head[2]), "head right");
         check(java.util.Arrays.equals(new int[]{16, 8, 24, 16}, head[4]), "head left");
         check(java.util.Arrays.equals(new int[]{24, 8, 32, 16}, head[5]), "head back");
         check(java.util.Arrays.equals(new int[]{40, 8, 48, 16}, PlayerModel.faceRects(32, 0, 8, 8, 8)[3]), "hat front (second layer)");
         check(java.util.Arrays.equals(new int[]{20, 20, 28, 32}, PlayerModel.faceRects(16, 16, 8, 12, 4)[3]), "body front is (20,20)-(28,32)");
         check(java.util.Arrays.equals(new int[]{44, 20, 48, 32}, PlayerModel.faceRects(40, 16, 4, 12, 4)[3]), "classic right arm front");
         check(java.util.Arrays.equals(new int[]{44, 20, 47, 32}, PlayerModel.faceRects(40, 16, 3, 12, 4)[3]), "slim right arm front is 3 wide");
         check(java.util.Arrays.equals(new int[]{51, 20, 54, 32}, PlayerModel.faceRects(40, 16, 3, 12, 4)[5]), "slim right arm back ends at x=54: columns 54-55 are unused");
         check(java.util.Arrays.equals(new int[]{52, 20, 56, 32}, PlayerModel.faceRects(40, 16, 4, 12, 4)[5]), "classic right arm back uses columns 54-55");
         check(java.util.Arrays.equals(new int[]{4, 20, 8, 32}, PlayerModel.faceRects(0, 16, 4, 12, 4)[3]), "right leg front");
         check(java.util.Arrays.equals(new int[]{20, 52, 24, 64}, PlayerModel.faceRects(16, 48, 4, 12, 4)[3]), "left leg front");
         // every layer of every part lies inside the 64x64 skin, base layers never overlap each other
         int[][] boxes = {{0, 0, 8, 8, 8}, {32, 0, 8, 8, 8}, {16, 16, 8, 12, 4}, {16, 32, 8, 12, 4}, {40, 16, 4, 12, 4}, {40, 32, 4, 12, 4}, {32, 48, 4, 12, 4}, {48, 48, 4, 12, 4},
            {0, 16, 4, 12, 4}, {0, 32, 4, 12, 4}, {16, 48, 4, 12, 4}, {0, 48, 4, 12, 4}, {40, 16, 3, 12, 4}, {32, 48, 3, 12, 4}};
         for (int[] b : boxes) {
            for (int[] r : PlayerModel.faceRects(b[0], b[1], b[2], b[3], b[4])) {
               check(r[0] >= 0 && r[1] >= 0 && r[2] <= 64 && r[3] <= 64 && r[2] > r[0] && r[3] > r[1], "inside the skin: box at " + b[0] + "," + b[1]);
            }
         }
         int[] owner = new int[4096];
         int[][] base = {{0, 0, 8, 8, 8}, {16, 16, 8, 12, 4}, {40, 16, 4, 12, 4}, {32, 48, 4, 12, 4}, {0, 16, 4, 12, 4}, {16, 48, 4, 12, 4}};
         for (int i = 0; i < base.length; i++) {
            for (int[] r : PlayerModel.faceRects(base[i][0], base[i][1], base[i][2], base[i][3], base[i][4])) {
               for (int y = r[1]; y < r[3]; y++) {
                  for (int x = r[0]; x < r[2]; x++) {
                     check(owner[y * 64 + x] == 0, "no two base faces share a pixel (" + x + "," + y + ")");
                     owner[y * 64 + x] = i + 1;
                  }
               }
            }
         }
      });
   }

   static void defaults() {
      test("default skin: drawn in code, opaque on every base face, transparent second layer, a face on the head", () -> {
         int[] px = Skins.defaultPixels();
         eq(4096, px.length, "64x64");
         check(px == Skins.defaultPixels(), "built once");
         int[][] base = {{0, 0, 8, 8, 8}, {16, 16, 8, 12, 4}, {40, 16, 4, 12, 4}, {32, 48, 4, 12, 4}, {0, 16, 4, 12, 4}, {16, 48, 4, 12, 4}};
         for (int[] b : base) {
            for (int[] r : PlayerModel.faceRects(b[0], b[1], b[2], b[3], b[4])) {
               for (int y = r[1]; y < r[3]; y++) {
                  for (int x = r[0]; x < r[2]; x++) {
                     check(px[y * 64 + x] >>> 24 == 255, "opaque at " + x + "," + y);
                  }
               }
            }
         }
         for (int[] b : new int[][]{{32, 0, 8, 8, 8}, {16, 32, 8, 12, 4}, {40, 32, 4, 12, 4}, {48, 48, 4, 12, 4}, {0, 32, 4, 12, 4}, {0, 48, 4, 12, 4}}) {
            for (int[] r : PlayerModel.faceRects(b[0], b[1], b[2], b[3], b[4])) {
               for (int y = r[1]; y < r[3]; y++) {
                  for (int x = r[0]; x < r[2]; x++) {
                     check(px[y * 64 + x] >>> 24 == 0, "second layer transparent at " + x + "," + y);
                  }
               }
            }
         }
         // the face: white eyes on the front of the head (row 12), a mouth (row 14)
         check(px[12 * 64 + 9] == 0xFFFFFFFF && px[12 * 64 + 14] == 0xFFFFFFFF, "eyes");
         check(px[14 * 64 + 11] != px[10 * 64 + 11], "a mouth");
         check(java.util.Arrays.equals(px, Skins.defaultSkin()), "drawing it again gives the same pixels");
      });
   }

   // ------------------------------------------------------------------ pose, camera, options
   static void pose() {
      test("body animation: legs start swinging when you walk, stop when you stand; the head may turn 50 degrees before the body follows", () -> {
         Game g = newGame(1);
         Player p = g.player;
         g.tickPose();
         eq(0.0F, g.walkAmp, "standing: no swing");
         for (int i = 0; i < 20; i++) {
            p.px = p.x;
            p.pz = p.z;
            p.x += 0.2;
            g.tickPose();
         }
         check(g.walkAmp > 0.6F, "walking: swing amplitude " + g.walkAmp);
         check(g.walkPhase > 5.0F, "the phase advances: " + g.walkPhase);
         PlayerModel.Pose pose = g.pose(0.5F);
         check(pose.amp > 0.5F, "pose carries the amplitude");
         for (int i = 0; i < 40; i++) {
            p.px = p.x;
            p.pz = p.z;
            g.tickPose();
         }
         check(g.walkAmp < 0.05F, "standing again: the swing dies down (" + g.walkAmp + ")");
         // head and body: turn the camera 120 degrees while standing
         p.yaw = 0;
         g.bodyYaw = g.pBodyYaw = 0;
         p.yaw = 120;
         p.px = p.x;
         p.pz = p.z;
         g.tickPose();
         check(Math.abs(g.bodyYaw - 70.0F) < 0.01F, "the body only follows the part beyond 50 degrees: " + g.bodyYaw);
         PlayerModel.Pose q = g.pose(1.0F);
         check(Math.abs(q.headYaw - 50.0F) < 0.01F, "so the head is turned 50 degrees: " + q.headYaw);
         // walking turns the body to where you look
         for (int i = 0; i < 10; i++) {
            p.px = p.x;
            p.pz = p.z;
            p.x += 0.2;
            g.tickPose();
         }
         check(Math.abs(Mobs.wrap(p.yaw - g.bodyYaw)) < 2.0F, "walking: the body faces the way you look (" + g.bodyYaw + ")");
         // sneaking and swinging reach the pose
         p.sneaking = true;
         g.swinging = true;
         g.swing = 0.4F;
         g.swingPrev = 0.2F;
         PlayerModel.Pose s = g.pose(0.5F);
         check(s.sneak, "sneak");
         check(Math.abs(s.swing - 0.3F) < 1e-5F, "swing is interpolated: " + s.swing);
         p.flying = true;
         check(!g.pose(0.0F).sneak, "no sneak pose while flying");
         g.swinging = false;
         check(g.pose(0.0F).swing < 0, "no swing when not hitting");
      });
   }

   static void camera() {
      test("third-person camera: 4 blocks behind or in front along the line of sight, stopped by walls, never inside a block", () -> {
         Game g = newGame(1);
         World w = g.world;
         double ex = 8.5;
         double ey = GROUND + 2.62;
         double ez = 8.5;
         // free space, looking along +z (yaw 0), level: behind = -z
         double[] c = Renderer.thirdPerson(w, ex, ey, ez, 0, 0, 1);
         check(Math.abs(c[0] - ex) < 1e-6 && Math.abs(c[2] - (ez - 4.0)) < 1e-6 && Math.abs(c[1] - ey) < 1e-6, "behind: 4 blocks back, same height");
         eq(0.0, c[3], "same yaw");
         double[] f = Renderer.thirdPerson(w, ex, ey, ez, 0, 0, 2);
         check(Math.abs(f[2] - (ez + 4.0)) < 1e-6, "in front: 4 blocks ahead");
         eq(180.0, f[3], "looking back at the player: yaw turned around");
         check(Math.abs(f[4]) < 1e-9, "level");
         double[] fu = Renderer.thirdPerson(w, ex, ey, ez, 0, 30, 2);
         eq(-30.0, fu[4], "pitch mirrored in front");
         // looking down 45 degrees: behind-and-above
         double[] up = Renderer.thirdPerson(w, ex, ey, ez, 0, 45, 1);
         check(up[1] > ey + 2.0, "looking down: the camera goes up and back (" + up[1] + ")");
         // a wall 2 blocks behind
         for (int y = GROUND + 1; y < GROUND + 6; y++) {
            for (int x = 4; x < 14; x++) {
               w.setBlock(x, y, 6, Blocks.STONE);
            }
         }
         double[] wall = Renderer.thirdPerson(w, ex, ey, ez, 0, 0, 1);
         check(wall[2] > 7.0 - 1e-6 && wall[2] < 8.5 - 0.2, "the camera stops in front of the wall: z=" + wall[2]);
         check(!Blocks.solid[w.getBlock((int)Math.floor(wall[0]), (int)Math.floor(wall[1]), (int)Math.floor(wall[2]))], "and is not inside a block");
         // standing with the back to the ground: looking straight up, behind = down: stops above the floor
         double[] down = Renderer.thirdPerson(w, ex, ey, ez, 0, -90, 1);
         check(down[1] > GROUND + 1.0, "looking up: the camera does not go through the floor (" + down[1] + ")");
         // in the first-person view the renderer does not call this at all (view 0 is handled by the caller)
         eq(0, g.view, "first person is the default");
      });
   }

   static void options() {
      test("the chosen skin is saved with the options and survives a restart", () -> {
         File root = tmpDir();
         Options o = new Options(root);
         eq(Skins.DEFAULT, o.skin, "default choice");
         o.skin = "my_skin.png";
         o.save();
         Options r = new Options(root);
         r.load();
         eq("my_skin.png", r.skin, "loaded again");
         Options old = new Options(tmpDir());
         old.load();
         eq(Skins.DEFAULT, old.skin, "options files from before 3.9 have no skin entry");
         // Game: wearing a missing skin shows the default; a real one is returned with its slim flag
         Game g = newGame(1);
         g.skins = new Skins(root);
         g.opt = r;
         check(g.skinPixels() == Skins.defaultPixels(), "a missing file falls back to the default skin");
         File f = writePng(g.skins.dir, "my_skin.png", 64, 64, (x, y) -> 0xFF778899);
         check(g.skinPixels() != Skins.defaultPixels() && g.skinPixels()[100] == 0xFF778899, "the real skin is used");
         check(!g.skinSlim(), "classic arms");
         // dropping / importing through the game wears the skin
         File src = writePng(tmpDir(), "dropped one.png", 64, 64, (x, y) -> 0xFFAA5500);
         g.importSkin(src);
         eq("dropped_one.png", g.opt.skin, "an imported skin is worn at once");
         check(g.skinMsg.startsWith("Imported"), g.skinMsg);
         File junk = new File(tmpDir(), "junk.png");
         Files.write(junk.toPath(), new byte[]{1, 2, 3});
         g.importSkin(junk);
         eq("dropped_one.png", g.opt.skin, "a bad file does not change the skin");
         check(g.skinMsg.startsWith("Not a usable skin"), g.skinMsg);
      });
   }

   // ------------------------------------------------------------------ the polish after the first 3.9 build
   static void polish() {
      test("account ids: 32 hex digits or the dashed form are understood (and nothing else); the session answer gives the account's own name", () -> {
         eq("069a79f444e94726a5befca90e38aaf5", Skins.uuidOf("069a79f444e94726a5befca90e38aaf5"), "plain");
         eq("069a79f444e94726a5befca90e38aaf5", Skins.uuidOf("  069A79F4-44E9-4726-A5BE-FCA90E38AAF5 "), "dashed, upper case, padded");
         eq(null, Skins.uuidOf("069a79f444e94726a5befca90e38aaf"), "31 digits");
         eq(null, Skins.uuidOf("069a79f444e94726a5befca90e38aafZ"), "not hex");
         eq(null, Skins.uuidOf("Notch"), "a name is not an id");
         eq(null, Skins.uuidOf(null), "null");
         eq("jeb_", Skins.profileName("{\"id\":\"853c80ef3c3749fdaa49938b674adae6\",\"name\":\"jeb_\",\"properties\":[{\"name\":\"textures\",\"value\":\"AAAA\"}]}"), "name from the session answer");
         eq(null, Skins.profileName("{\"properties\":[{\"name\":\"textures\",\"value\":\"AAAA\"}]}"), "no account name -> null (not 'textures')");
         Skins sk = new Skins(tmpDir());
         sk.importUser("bad name!");
         check(sk.status.contains("1-16") && !sk.busy, "a name with a space is still refused: " + sk.status);
      });
      test("the Skins screen adapts to narrow windows: the hint wraps, labels fall back to shorter ones, long lines are clipped", () -> {
         // the font's glyph widths normally come from res/font.bin through OpenGL: read them directly so the widths are real here
         try (java.io.DataInputStream in = new java.io.DataInputStream(new java.io.FileInputStream("res/font.bin"))) {
            for (int i = 0; i < 4; i++) {
               in.readUnsignedByte();
            }

            for (int i = 0; i < 96; i++) {
               Tex.fontW[i] = in.readUnsignedByte();
            }
         }

         check(Gui.textWidth("Hello world") > 30, "the font widths are loaded: " + Gui.textWidth("Hello world"));
         String tip = "Drag a skin .png onto this window to wear it. Or copy a file path, link or username, then press Ctrl+V.";
         int previous = 0;
         for (float w : new float[]{700, 430, 343, 300, 240}) {
            List<String> lines = Game.wrapText(tip, w - 12);
            for (String line : lines) {
               check(Gui.textWidth(line) <= w - 12, "a line fits a window " + w + " units wide: " + Gui.textWidth(line) + " '" + line + "'");
            }

            eq(tip, String.join(" ", lines), "no word is lost when wrapping to " + w);
            check(lines.size() >= previous, "a narrower window never gives fewer lines (" + lines.size() + " at " + w + ")");
            previous = lines.size();
         }

         check(Game.wrapText(tip, 700 - 12).size() <= 2, "a wide window needs at most two lines");
         eq(1, Game.wrapText("Supercalifragilisticexpialidocious", 20).size(), "a single long word keeps its own line");
         eq("Import skin file...", Game.fitLabel(500, "Import skin file...", "Import file...", "File..."), "wide button: the full label");
         eq("Import file...", Game.fitLabel(Gui.textWidth("Import file...") + 1, "Import skin file...", "Import file...", "File..."), "narrower: the shorter label");
         eq("File...", Game.fitLabel(5, "Import skin file...", "Import file...", "File..."), "very narrow: the last one");
         String longLine = "Could not import: the server answered 429 and then something else that is far too long to show";
         String clipped = Game.clipText(longLine, 300.0F);
         check(Gui.textWidth(clipped) <= 300, "clipped width " + Gui.textWidth(clipped));
         check(clipped.endsWith("~") && clipped.startsWith("Could not"), "clipped: " + clipped);
         eq("short", Game.clipText("short", 300.0F), "short lines stay as they are");
      });
      test("the pose carries the item in the right hand (0 = empty hand); the body model draws it in the fist", () -> {
         Game g = newGame(1);
         g.inv[g.sel] = null;
         eq(0, g.pose(0.0F).held, "empty hand");
         g.inv[g.sel] = new Stack(Items.BOW, 1);
         eq(Items.BOW, g.pose(0.0F).held, "bow");
         g.inv[g.sel] = new Stack(Blocks.STONE, 12);
         eq(Blocks.STONE, g.pose(0.0F).held, "a block");
      });
      test("a skin dialog leaves nothing behind: the dialog flags start clear and the fullscreen return flag is only set by the dialog", () -> {
         Game g = newGame(1);
         check(!g.skinBrowsing, "not browsing");
         check(!g.skinRefull, "no pending return to fullscreen");
         check(g.skinDrops.isEmpty(), "no pending drops");
      });
   }

   // ------------------------------------------------------------------ mobs and one-block steps (To do: "mobs not being able to jump up a block")
   /** Walls of the given height at x 14..24 (all z of the test world); the player stands on top of them far away. */
   static Game stepWorld(int height) throws Exception {
      Game g = newGame(0);
      for (int x = 14; x <= 24; x++) {
         for (int z = -30; z <= 45; z++) {
            for (int h = 1; h <= height; h++) {
               g.world.setBlock(x, GROUND + h, z, Blocks.STONE);
            }
         }
      }

      return g;
   }

   /** Drives the mobs toward the wall (animals flee from a point behind them, hostile mobs chase the player); true = it stood on top of the wall. */
   static boolean[] walkAtWall(Game g, int height, List<Mobs.Mob> mobs) {
      boolean[] up = new boolean[mobs.size()];
      for (int t = 0; t < 300; t++) {
         g.player.setPos(30.5, GROUND + height + 1, 12.5);
         for (int i = 0; i < mobs.size(); i++) {
            Mobs.Mob m = mobs.get(i);
            if (up[i]) {
               continue;
            }

            if (m.type < 4) {
               m.flee = 100;
               m.fleeX = 0;
               m.fleeZ = m.z;
            }

            g.mobs.tickMob(m, g.world, g.player, 1.0f);
            up[i] = m.onGround && m.y > GROUND + height + 0.9 && m.x > 13.0;
         }
      }

      return up;
   }

   static void steps() {
      test("mobs jump up one block: pigs, cows, sheep, chickens, babies and zombies get on top of a full block in their way", () -> {
         Game g = stepWorld(1);
         java.util.ArrayList<Mobs.Mob> mobs = new java.util.ArrayList<>();
         for (int type = 0; type <= 3; type++) {
            mobs.add(g.mobs.spawn(type, 10.5, GROUND + 1, 2.5 + type * 2));
         }

         mobs.add(g.mobs.spawn(Mobs.ZOMBIE, 10.5, GROUND + 1, 12.5));
         mobs.add(g.mobs.spawnBaby(Mobs.PIG, 10.5, GROUND + 1, 16.5));
         mobs.add(g.mobs.spawnBaby(Mobs.COW, 10.5, GROUND + 1, 18.5));
         boolean[] up = walkAtWall(g, 1, mobs);
         for (int i = 0; i < mobs.size(); i++) {
            Mobs.Mob m = mobs.get(i);
            check(up[i], Mobs.NAME[m.type] + (m.babyAge > 0 ? " (baby)" : "") + " got on top of the block: x=" + m.x + " y=" + m.y);
         }
      });
      test("... but not up two blocks (a jump reaches 1.25 blocks, like in Minecraft)", () -> {
         Game g = stepWorld(2);
         java.util.ArrayList<Mobs.Mob> mobs = new java.util.ArrayList<>();
         mobs.add(g.mobs.spawn(Mobs.PIG, 10.5, GROUND + 1, 4.5));
         mobs.add(g.mobs.spawn(Mobs.COW, 10.5, GROUND + 1, 8.5));
         mobs.add(g.mobs.spawn(Mobs.ZOMBIE, 10.5, GROUND + 1, 12.5));
         double[] peak = new double[mobs.size()];
         for (int t = 0; t < 300; t++) {
            g.player.setPos(30.5, GROUND + 3, 12.5);
            for (int i = 0; i < mobs.size(); i++) {
               Mobs.Mob m = mobs.get(i);
               if (m.type < 4) {
                  m.flee = 100;
                  m.fleeX = 0;
                  m.fleeZ = m.z;
               }

               g.mobs.tickMob(m, g.world, g.player, 1.0f);
               peak[i] = Math.max(peak[i], m.y - (GROUND + 1));
            }
         }

         for (int i = 0; i < mobs.size(); i++) {
            Mobs.Mob m = mobs.get(i);
            check(m.x < 14.0, Mobs.NAME[m.type] + " stays in front of a two-block wall: x=" + m.x);
            check(peak[i] > 1.15 && peak[i] < 1.35, Mobs.NAME[m.type] + " jumps 1.25 blocks high: " + peak[i]);
         }
      });
   }

   // ------------------------------------------------------------------ creepers can be lit (To do: "Make Creepers ignitable")
   static void creepers() {
      test("flint and steel lights a creeper's fuse: it stops, swells and goes off after 1.5 s even when the player is far away", () -> {
         Game g = newGame(0);
         g.inv[g.sel] = new Stack(Items.FLINT_STEEL, 1);
         Mobs.Mob far = g.mobs.spawn(Mobs.CREEPER, 40.5, GROUND + 1, 8.5); // 32 blocks away: it never noticed the player
         check(!far.ignited, "not lit yet");
         check(g.useOnMob(far, g.held()), "right-click with flint and steel lights it");
         check(far.ignited, "lit");
         eq(1, g.held().dmg, "uses one point of durability");
         check(!g.useOnMob(far, g.held()), "a creeper that is already lit cannot be lit again (and the steel is not used up)");
         eq(1, g.held().dmg, "still one point");
         double x0 = far.x;
         double z0 = far.z;
         int swellMax = 0;
         for (int t = 0; t < 29; t++) {
            g.mobs.tickMob(far, g.world, g.player, 1.0f);
            swellMax = Math.max(swellMax, far.swell);
            check(!far.dead, "still fizzing after " + (t + 1) + " ticks");
         }

         check(swellMax >= 28, "it swells while the fuse burns: " + swellMax);
         check(Math.hypot(far.x - x0, far.z - z0) < 0.5, "it stands still while lit");
         for (int t = 0; t < 4; t++) {
            g.mobs.tickMob(far, g.world, g.player, 1.0f);
         }

         check(far.dead, "and then it explodes");
         eq(Blocks.AIR, g.world.getBlock(40, GROUND, 8), "the blast leaves a crater in the ground it stood on");
      });
      test("only creepers can be lit; creative mode keeps the steel; a charged creeper still blasts twice as hard when lit", () -> {
         Game g = newGame(0);
         g.inv[g.sel] = new Stack(Items.FLINT_STEEL, 1);
         for (int type : new int[]{Mobs.PIG, Mobs.COW, Mobs.SHEEP, Mobs.CHICKEN, Mobs.ZOMBIE, Mobs.SKELETON, Mobs.SPIDER}) {
            Mobs.Mob m = g.mobs.spawn(type, 14.5, GROUND + 1, 8.5);
            check(!g.useOnMob(m, g.held()), Mobs.NAME[type] + " cannot be lit");
            check(!m.ignited, Mobs.NAME[type] + " is not ignited");
         }

         eq(0, g.held().dmg, "no durability used");
         Game c = newGame(1);
         c.inv[c.sel] = new Stack(Items.FLINT_STEEL, 1);
         Mobs.Mob cr = c.mobs.spawn(Mobs.CREEPER, 14.5, GROUND + 1, 8.5);
         check(c.useOnMob(cr, c.held()), "creative: lights it too");
         eq(0, c.held().dmg, "creative: no durability used");
         Game h = newGame(0);
         h.inv[h.sel] = new Stack(Items.STICK, 1);
         Mobs.Mob plain = h.mobs.spawn(Mobs.CREEPER, 14.5, GROUND + 1, 8.5);
         check(!h.useOnMob(plain, h.held()), "a stick does nothing");
         // a charged, lit creeper next to a block wall: radius 6, not 3
         Game w = newGame(0);
         w.inv[w.sel] = new Stack(Items.FLINT_STEEL, 1);
         for (int dx = -5; dx <= 5; dx += 5) {
            w.world.setBlock(30 + dx, GROUND + 1, 30, Blocks.STONE);
         }

         Mobs.Mob ch = w.mobs.spawn(Mobs.CREEPER, 30.5, GROUND + 1, 30.5);
         ch.charged = true;
         w.useOnMob(ch, w.held());
         for (int t = 0; t < 40 && !ch.dead; t++) {
            w.mobs.tickMob(ch, w.world, w.player, 1.0f);
         }

         check(ch.dead, "it blew up");
         eq(Blocks.AIR, w.world.getBlock(35, GROUND + 1, 30), "a stone block 5 blocks away is gone (charged blast)");
      });
   }

   // ------------------------------------------------------------------ fire clings to blocks (To do: "Display fire like in Minecraft when it is attached to a block")
   /** The vertices {x,y,z} that a fire adds to the mesh of chunk (0,0), given the blocks set up around it before. */
   static java.util.List<float[]> fireVertices(World w, Chunk c, int x, int y, int z) {
      java.util.HashMap<String, Integer> base = new java.util.HashMap<>();
      w.setBlock(x, y, z, 0);
      Mesher.Result b = Mesher.build(w, c, 3, false);
      for (int i = 0; i < b.nO; i++) {
         base.merge(b.opaque.get(i * 11) + "," + b.opaque.get(i * 11 + 1) + "," + b.opaque.get(i * 11 + 2), 1, Integer::sum);
      }

      w.setBlock(x, y, z, Blocks.FIRE);
      Mesher.Result r = Mesher.build(w, c, 3, false);
      java.util.ArrayList<float[]> out = new java.util.ArrayList<>();
      for (int i = 0; i < r.nO; i++) {
         float[] v = {r.opaque.get(i * 11), r.opaque.get(i * 11 + 1), r.opaque.get(i * 11 + 2)};
         String key = v[0] + "," + v[1] + "," + v[2];
         Integer n = base.get(key);
         if (n != null && n > 0) {
            base.put(key, n - 1);
         } else {
            out.add(v);
         }
      }

      for (Mesher.Result m : new Mesher.Result[]{r, b}) {
         if (m.opaque != null) {
            org.lwjgl.system.MemoryUtil.memFree(m.opaque);
         }

         if (m.trans != null) {
            org.lwjgl.system.MemoryUtil.memFree(m.trans);
         }
      }

      w.setBlock(x, y, z, 0);
      return out;
   }

   /** Since 3.11 every flame plane comes with its back (back-face culling): twice the vertices. */
   static int back() {
      return Mesher.legacy ? 1 : 2;
   }

   static void fireLook() {
      test("fire like in Minecraft: an X on solid ground, flames on the wall it hangs on, flames from a ceiling it hangs from", () -> {
         Game g = newGame(1);
         World w = g.world;
         Chunk c = w.chunk(0, 0);
         int y = GROUND + 3; // in the air: nothing solid below unless the test puts it there
         int ly = y - 48; // mesh coordinates are local to the 16-high section (3 = y 48..63)
         // 1) on the grass: the X (2 quads)
         eq(8 * back(), fireVertices(w, c, 5, GROUND + 1, 5).size(), "fire on grass: two crossed planes (each with its back)");
         // 2) hanging on a wooden wall, nothing below: one plane in front of the wall
         w.setBlock(6, y, 5, Blocks.PLANKS);
         java.util.List<float[]> wall = fireVertices(w, c, 5, y, 5);
         eq(4 * back(), wall.size(), "fire hanging on a wall east of it: one flame plane");
         for (float[] v : wall) {
            check(v[0] >= 5.80F && v[0] <= 5.99F, "the plane is just in front of the wall's face (x=6): " + v[0]);
            check(v[2] >= 5.0F && v[2] <= 6.0F && v[1] >= ly && v[1] <= ly + 1.0F, "and covers the face: " + v[1] + "/" + v[2]);
         }

         double lean = 0;
         for (float[] v : wall) {
            lean = Math.max(lean, 5.99 - v[0]);
         }

         check(lean > 0.05, "the flames lean away from the wall at the top: " + lean);
         // 3) a corner: two walls, two planes; plus a flammable ceiling: two more crossed planes
         w.setBlock(5, y, 6, Blocks.LOG_OAK);
         eq(8 * back(), fireVertices(w, c, 5, y, 5).size(), "two flammable neighbours: two planes");
         w.setBlock(5, y + 1, 5, Blocks.LEAVES_OAK);
         eq(16 * back(), fireVertices(w, c, 5, y, 5).size(), "plus a flammable ceiling: two crossed planes more");
         for (float[] v : fireVertices(w, c, 5, y, 5)) {
            check(v[1] >= ly, "nothing sticks out below the cell: " + v[1]);
         }

         // 4) a non-flammable neighbour does not get flames: stone beside the fire, nothing flammable, nothing below -> the plain X
         w.setBlock(6, y, 5, 0);
         w.setBlock(5, y, 6, 0);
         w.setBlock(5, y + 1, 5, 0);
         w.setBlock(4, y, 5, Blocks.STONE);
         eq(8 * back(), fireVertices(w, c, 5, y, 5).size(), "stone is not flammable: the X (nothing to hang on)");
         // 5) flammable below counts as ground: the X even with a wall next to it
         w.setBlock(4, y, 5, 0);
         w.setBlock(5, y - 1, 5, Blocks.PLANKS);
         w.setBlock(6, y, 5, Blocks.PLANKS);
         eq(8 * back(), fireVertices(w, c, 5, y, 5).size(), "on planks: the X, like Minecraft's default fire state");
         // 6) the other three directions get their plane too, inside their own cell
         w.setBlock(5, y - 1, 5, 0);
         w.setBlock(6, y, 5, 0);
         for (int[] d : new int[][]{{-1, 0}, {0, 1}, {0, -1}}) {
            w.setBlock(5 + d[0], y, 5 + d[1], Blocks.PLANKS);
            java.util.List<float[]> p = fireVertices(w, c, 5, y, 5);
            eq(4 * back(), p.size(), "one plane for the wall at " + d[0] + "," + d[1]);
            for (float[] v : p) {
               check(v[0] > 5.0F - 1e-3F && v[0] < 6.0F + 1e-3F && v[2] > 5.0F - 1e-3F && v[2] < 6.0F + 1e-3F, "inside its own cell: " + v[0] + "," + v[2]);
               double toWall = d[0] != 0 ? Math.abs(v[0] - (d[0] > 0 ? 6.0 : 5.0)) : Math.abs(v[2] - (d[1] > 0 ? 6.0 : 5.0));
               check(toWall < 0.2, "next to the wall: " + toWall);
            }

            w.setBlock(5 + d[0], y, 5 + d[1], 0);
         }
      });
   }
}
