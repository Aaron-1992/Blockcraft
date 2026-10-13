package bc;

import static bc.GameTests.check;
import static bc.GameTests.test;
import java.util.Arrays;
import java.util.HashSet;

/** CPU regressions for the HD atlas and shared first/third-person weapon math. */
final class TestsVisualRefresh {
   static void run() {
      test("smooth materials: all atlas cells, alpha masks, deterministic detail and mip chains", () -> {
         String old = ResourcePacks.activeId();
         try {
            ResourcePacks.select(ResourcePacks.ID_DEFAULT, null);
            Tex.initPixels();
            int[] hd = TexSmooth.atlas(Tex.atlasPixels);
            int width = Tex.COLS * TexSmooth.SIZE;
            check(hd.length == width * width, "2048x2048 default atlas");
            for (int tile = 0; tile < Tex.COLS * Tex.COLS; tile++) {
               int[] src = TexOverhaulV57.readTile(tile);
               int[] result = TexSmooth.tile(tile, src, tile == Tex.WATER || tile == Tex.LAVA || tile == 235 || tile == TexArt312.PORTAL);
               for (int y = 0; y < 64; y++) for (int x = 0; x < 64; x++) {
                  int c = result[y * 64 + x];
                  check((c >>> 24) == (src[y / 4 * 16 + x / 4] >>> 24), "same silhouette/opacity " + tile);
                  check(c == hd[((tile / Tex.COLS) * 64 + y) * width + (tile % Tex.COLS) * 64 + x], "tile isolation " + tile);
               }
            }
            int[] stone = TexSmooth.tile(Tex.STONE, TexOverhaulV57.readTile(Tex.STONE), false);
            check(Arrays.equals(stone, TexSmooth.tile(Tex.STONE, TexOverhaulV57.readTile(Tex.STONE), false)), "deterministic");
            HashSet<Integer> shades = new HashSet<>();
            int largestStep = 0;
            for (int y = 0; y < 64; y++) for (int x = 0; x < 64; x++) {
               shades.add(stone[y * 64 + x]);
               largestStep = Math.max(largestStep, Math.abs((stone[y * 64 + x] & 255) - (stone[y * 64 + (x + 1) % 64] & 255)));
            }
            check(shades.size() > 20 && largestStep < 20, "smooth mineral gradients, including the repeating edge");
            for (int size = 64; size > 2; size /= 2) {
               stone = Tex.downsampleTile(stone, size);
               check(stone.length == size * size / 4, "complete HD mip level");
               for (int c : stone) check((c >>> 24) == 255, "opaque stone stays opaque at distance");
            }
            ResourcePacks.select(ResourcePacks.ID_CLASSIC, null);
            check(!TexSmooth.enabled(), "classic retains its original 16px art");
            ResourcePacks.select(ResourcePacks.ID_V56_PAINTED, null);
            check(!TexSmooth.enabled(), "legacy painted pack is unchanged by HD reconstruction");
            ResourcePacks.select(ResourcePacks.ID_DEFAULT, null);
            check(TexSmooth.enabled(), "default enables HD again on reload");
         } finally { ResourcePacks.select(old, null); }
      });
      test("smooth materials: transparent neighbours cannot add black fringes or change white utility texel", () -> {
         int[] src = new int[256];
         src[8 * 16 + 8] = 0xFFCCAA88;
         int[] hd = TexSmooth.tile(700, src, true);
         for (int y = 32; y < 36; y++) for (int x = 32; x < 36; x++) {
            check(hd[y * 64 + x] == src[136], "alpha-normalised colour at silhouette");
         }
         Arrays.fill(src, -1);
         for (int c : TexSmooth.tile(Tex.WHITE, src, false)) check(c == -1, "white stays white");
         check(Math.abs(TexSmooth.noise(0, 1.7F, 4, 900) - TexSmooth.noise(4, 1.7F, 4, 900)) < 0.00001F, "periodic noise");
      });
      test("weapons: barrels face forward, forearm aim compensates elbow, recoil returns to rest", () -> {
         double yaw = Math.toRadians(WeaponPose.FIRST_PERSON_YAW);
         check(-Math.sin(yaw) < -0.99 && Math.cos(yaw) < 0, "muzzle points away from camera and slightly inward");
         for (int id : new int[]{Redstone319.PISTOL, Redstone319.MACHINE_GUN, Redstone319.SNIPER_RIFLE,
               Redstone319.SIEGE_CANNON, Redstone319.MORTAR_STRIKE}) {
            PlayerModel.Pose p = new PlayerModel.Pose();
            p.held = id;
            for (int pitch = -60; pitch <= 60; pitch += 10) {
               p.headPitch = pitch;
               p.swing = 0.4F; // firing must not apply the sword/mining swing
               PlayerAnim.Joints j = PlayerAnim.solve(p);
               check(Math.abs(j.sh[0] + j.el[0] + j.tPitch + j.pitch - (90 - pitch)) < 0.01F, "aim at pitch " + pitch);
            }
            check(WeaponPose.muzzleX(id) > 0 && WeaponPose.gripX(id) < 0, "muzzle in front of grip");
            check(WeaponPose.recoil(id, 0, 0) == 0, "rest");
            float previous = 1;
            for (float t = 0; t <= WeaponPose.cooldown(id); t += 0.05F) {
               float r = WeaponPose.recoil(id, WeaponPose.cooldown(id), t);
               check(r >= 0 && r <= previous + 0.00001F, "continuous recoil recovery");
               previous = r;
            }
         }
      });
      test("bow: limbs bend continuously, string stays attached and taut, arrow shares nock, release relaxes", () -> {
         float previous = 0;
         for (int ticks = 0; ticks <= 60; ticks++) for (int sub = 0; sub < 10; sub++) {
            float draw = WeaponPose.bowDraw(ticks, sub / 10.0F);
            check(draw >= previous && draw <= 1, "draw continuous and clamped");
            previous = draw;
            float dx = WeaponPose.tipX(draw) - WeaponPose.nockX(draw), y = WeaponPose.tipY(draw);
            check(Math.abs(dx * dx + y * y + WeaponPose.nockZ(draw) * WeaponPose.nockZ(draw) - 0.36F) < 0.00001F, "fixed string half-length");
            check(Math.abs(WeaponPose.limbX(1, draw) - WeaponPose.tipX(draw)) < 0.00001F, "string joins limb tip");
            check(WeaponPose.limbX(0, draw) == 0, "grip stays in palm");
         }
         check(WeaponPose.tipY(1) < WeaponPose.tipY(0) && WeaponPose.nockX(1) < WeaponPose.nockX(0), "limbs flex and nock retracts");
         check(WeaponPose.releaseDraw(1, 4, 0) == 1 && WeaponPose.releaseDraw(1, 0, 0) == 0, "release endpoints");
         for (int tick = 4; tick > 0; tick--) {
            check(Math.abs(WeaponPose.releaseDraw(1, tick, 1) - WeaponPose.releaseDraw(1, tick - 1, 0)) < 0.00001F, "release continuous across ticks");
         }
         Game g = GameTests.newGame(0);
         g.inv[g.sel] = new Stack(Items.BOW, 1);
         g.bowCharge = 10;
         check(g.pose(0.75F).bow > g.pose(0).bow, "third-person draw interpolates between ticks");
         check(g.bowVisualDraw(0.75F) == g.pose(0.75F).bow, "both views use same draw");
         g.bowCharge = 0;
         g.bowReleaseDraw = 1;
         g.bowReleaseTicks = 4;
         check(g.bowVisualDraw(0.5F) > 0 && g.pose(0.5F).bow < 0, "relaxing bow has no ghost nocked arrow");
         g.inv[g.sel] = new Stack(Items.ARROW, 1);
         check(g.bowVisualDraw(0) == 0, "switching items hides release");
      });
      test("bow: actual draw/release consumes one arrow, menus cancel without firing", () -> {
         Game g = GameTests.newGame(0);
         g.inv[g.sel] = new Stack(Items.BOW, 1);
         g.inv[10] = new Stack(Items.ARROW, 3);
         g.rightDown = true;
         for (int i = 0; i < 20; i++) g.interact();
         check(g.bowCharge == 20, "charges while held");
         g.rightDown = false;
         g.interact();
         check(g.bowCharge == 0 && g.bowReleaseTicks == 4, "starts spring-back on release");
         check(g.mobs.arrows.size() == 1 && g.countItem(Items.ARROW) == 2, "one projectile, one arrow used");
         g.rightDown = true;
         g.interact();
         g.setScreen(Game.S.PAUSE);
         check(g.bowCharge == 0 && g.bowReleaseTicks == 0 && g.mobs.arrows.size() == 1, "menu cancels without shooting");
      });
   }
}
