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
}
