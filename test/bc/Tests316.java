package bc;

import static bc.GameTests.GROUND;
import static bc.GameTests.check;
import static bc.GameTests.eq;
import static bc.GameTests.newGame;
import static bc.GameTests.test;

/**
 * Headless tests of Update 3.16 (the graphics overhaul: sky, light, post effects, particles; the texture overhaul).   python3 tools/runtests.py unit
 */
final class Tests316 {
   static void run() {
      sky();
      particles();
   }

   static void near(float expected, float actual, float eps, String what) {
      check(Math.abs(expected - actual) <= eps, what + ": expected " + expected + ", got " + actual);
   }

   // ------------------------------------------------------------------ particles
   static void particles() {
      test("particles: day and night are what the sky says", () -> {
         check(Fx316.day(0L) && Fx316.day(6000L) && Fx316.day(12000L) && !Fx316.day(15000L) && !Fx316.day(18000L) && Fx316.day(23900L), "day");
         check(!Fx316.night(6000L) && !Fx316.night(12500L) && Fx316.night(15000L) && Fx316.night(18000L) && !Fx316.night(23500L), "night");
         check(Fx316.day(24000L * 5) && Fx316.night(24000L * 5 + 17000L), "every day the same");
      });

      test("particles: leaves come off the trees (only where there is air below), drift down in the colour of the foliage and are never too many", () -> {
         Game g = newGame(0);
         Player p = g.player;
         for (int x = -10; x <= 30; x++) {
            for (int z = -10; z <= 30; z++) {
               g.world.setBlock(x, GROUND + 12, z, Blocks.LEAVES_OAK);
            }
         }

         // leaves with a block below them shed nothing: a second canopy lying on the floor
         for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
               g.world.setBlock(-25 + dx, GROUND + 1, 20 + dz, Blocks.LEAVES_OAK);
            }
         }

         for (int i = 0; i < 400; i++) {
            g.tickCount = i * 3;
            Fx316.leaves(g, g.world, p, g.rnd);
         }

         int n = 0;
         for (Game.Particle q : g.particles) {
            n++;
            check(q.fade == 1 && q.sway > 0.0F && q.grav > 0.0F && q.life >= 150 && q.size >= 0.04F && q.size <= 0.08F, "a leaf: fade " + q.fade + " sway " + q.sway);
            check(q.y < GROUND + 12.05F && q.y > GROUND + 11.0F, "it starts under the canopy: y = " + q.y);
            check(q.x > -10.5F && q.x < 31.5F && q.z > -10.5F && q.z < 31.5F, "under the canopy: " + q.x + " " + q.z);
            check(q.vy < 0.0F, "falling");
            check(q.g > q.b && q.g > 0.3F, "green: " + q.r + " " + q.g + " " + q.b);
            check(!q.glow, "a leaf does not glow");
         }

         check(n >= 10 && n <= 200, "a steady trickle of leaves: " + n);
         // the number of ticks the loop makes only matters every third tick
         Game h = newGame(0);
         h.tickCount = 1;
         Fx316.leaves(h, h.world, h.player, h.rnd);
         eq(0, h.particles.size(), "nothing on a tick that is not a multiple of three");
      });

      test("particles: fireflies only over grass, only at night, never more than 14, glowing, pulsing and wandering", () -> {
         Game g = newGame(0);
         g.world.dayTime = 16000L;
         for (int i = 0; i < 1200; i++) {
            g.tickCount = i * 4;
            Fx316.fireflies(g, g.world, g.player, g.rnd);
         }

         int n = Fx316.fireflies(g);
         check(n >= 6 && n <= 14, "up to fourteen: " + n);
         for (Game.Particle q : g.particles) {
            check(q.kind == 1 && q.glow && q.twinkle == 1 && q.fade == 1, "a firefly");
            check(q.y > GROUND + 1.7F && q.y < GROUND + 3.5F, "above the grass: " + q.y);
            check(Math.abs(q.x - 8.5F) < 14.0F && Math.abs(q.z - 8.5F) < 14.0F, "near the player");
            check(q.g > q.r && q.g > q.b, "yellow-green");
         }

         // not over a floor of stone
         Game h = newGame(0);
         for (int x = -8; x < 28; x++) {
            for (int z = -8; z < 28; z++) {
               h.world.setBlock(x, GROUND, z, Blocks.STONE);
            }
         }

         for (int i = 0; i < 600; i++) {
            h.tickCount = i * 4;
            Fx316.fireflies(h, h.world, h.player, h.rnd);
         }

         eq(0, Fx316.fireflies(h), "none over stone");
         // by day Fx316.tick does not make any
         Game d = newGame(0);
         d.world.dayTime = 6000L;
         for (int i = 0; i < 400; i++) {
            d.tickCount = i;
            Fx316.tick(d);
         }

         eq(0, Fx316.fireflies(d), "none by day");
      });

      test("particles: embers rise from lava, glowing, and die out", () -> {
         Game g = newGame(0);
         for (int x = 4; x <= 14; x++) {
            for (int z = 4; z <= 14; z++) {
               g.world.setBlock(x, GROUND, z, Blocks.LAVA);
            }
         }

         for (int i = 0; i < 400; i++) {
            g.tickCount = i;
            Fx316.embers(g, g.world, g.player, g.rnd);
         }

         int n = g.particles.size();
         check(n >= 15 && n <= 400, "embers: " + n);
         for (Game.Particle q : g.particles) {
            check(q.glow && q.vy > 0.0F && q.grav < 0.0F && q.fade == 3 && q.life <= 60, "an ember");
            check(q.y > GROUND + 1.0F && q.y < GROUND + 1.1F, "starts on the lava surface: " + q.y);
            check(q.r == 1.0F && q.g < 0.8F && q.b < 0.2F, "orange");
         }

         // a lake far below the player (the Nether): the embers come from there too
         Game far = newGame(0);
         far.player.setPos(8.5, GROUND + 30, 8.5);
         for (int x = -8; x <= 24; x++) {
            for (int z = -8; z <= 24; z++) {
               far.world.setBlock(x, GROUND, z, Blocks.LAVA);
            }
         }

         for (int i = 0; i < 100; i++) {
            Fx316.embers(far, far.world, far.player, far.rnd);
         }

         check(far.particles.size() >= 20, "embers over a lake 30 blocks down: " + far.particles.size());
         // no lava, no embers
         Game h = newGame(0);
         for (int i = 0; i < 400; i++) {
            Fx316.embers(h, h.world, h.player, h.rnd);
         }

         eq(0, h.particles.size(), "no embers without lava");
      });

      test("particles: a sprinting player kicks up dust from dirt, sand, gravel and snow, not from stone; walking does not", () -> {
         Game g = newGame(0);
         Player p = g.player;
         p.sprinting = true;
         p.onGround = true;
         p.vx = 0.2;
         p.vz = 0.1;
         p.setPos(8.5, GROUND + 1, 8.5);
         p.vx = 0.2;
         p.vz = 0.1;
         g.tickCount = 3;
         Fx316.sprintDust(g, g.world, p, g.rnd);
         eq(2, g.particles.size(), "two specks over grass");
         for (Game.Particle q : g.particles) {
            check(q.fade == 3 && q.life <= 16 && q.vy > 0.0F && q.size < 0.07F, "dust");
            check(q.y < GROUND + 1.2F && Math.abs(q.x - 8.5F) < 0.5F, "at the feet");
         }

         g.tickCount = 4;
         Fx316.sprintDust(g, g.world, p, g.rnd);
         eq(2, g.particles.size(), "only every third tick");
         p.sprinting = false;
         g.tickCount = 6;
         Fx316.sprintDust(g, g.world, p, g.rnd);
         eq(2, g.particles.size(), "not when walking");
         p.sprinting = true;
         g.world.setBlock(8, GROUND, 8, Blocks.STONE);
         Fx316.sprintDust(g, g.world, p, g.rnd);
         eq(2, g.particles.size(), "not on stone");
         for (int id : new int[]{Blocks.DIRT, Blocks.SAND, Blocks.GRAVEL, Blocks.SNOW}) {
            Game h = newGame(0);
            h.player.sprinting = true;
            h.player.onGround = true;
            h.player.setPos(8.5, GROUND + 1, 8.5);
            h.player.vx = 0.2;
            h.world.setBlock(8, GROUND, 8, id);
            h.tickCount = 3;
            Fx316.sprintDust(h, h.world, h.player, h.rnd);
            eq(2, h.particles.size(), "dust over block " + id);
         }
      });

      test("particles: jumping into water splashes, being under water makes bubbles, and nothing happens in spectator mode", () -> {
         Game g = newGame(0);
         Player p = g.player;
         Fx316.wasInWater = false;
         Fx316.lastVy = -0.55;
         p.inWater = true;
         p.vy = -0.3;
         Fx316.water(g, g.world, p, g.rnd);
         check(g.particles.size() >= 10 && g.particles.size() <= 30, "a splash: " + g.particles.size());
         for (Game.Particle q : g.particles) {
            check(!q.glow && q.vy > 0.1F && q.b == 1.0F, "a droplet");
         }

         int before = g.particles.size();
         Fx316.water(g, g.world, p, g.rnd);
         eq(before, g.particles.size(), "only the moment of entering splashes");
         p.eyeInWater = true;
         for (int i = 0; i < 300; i++) {
            Fx316.water(g, g.world, p, g.rnd);
         }

         int bubbles = 0;
         for (Game.Particle q : g.particles) {
            if (q.glow) {
               bubbles++;
               check(q.vy > 0.0F && q.grav < 0.0F && q.life >= 30, "a bubble rises");
            }
         }

         check(bubbles >= 8 && bubbles <= 60, "bubbles: " + bubbles);
         Game s = newGame(3);
         Fx316.wasInWater = false;
         Fx316.lastVy = -0.6;
         s.player.inWater = true;
         s.player.eyeInWater = true;
         for (int i = 0; i < 100; i++) {
            Fx316.water(s, s.world, s.player, s.rnd);
         }

         eq(0, s.particles.size(), "a spectator makes no splash");
      });

      test("particles: nothing is added while there are too many, and Fx316.tick copes with no world", () -> {
         Game g = newGame(0);
         for (int i = 0; i < Fx316.CEILING + 5; i++) {
            Game.Particle q = new Game.Particle();
            q.life = 100;
            g.particles.add(q);
         }

         g.world.dayTime = 16000L;
         for (int x = 4; x <= 14; x++) {
            for (int z = 4; z <= 14; z++) {
               g.world.setBlock(x, GROUND, z, Blocks.LAVA);
            }
         }

         int n = g.particles.size();
         for (int i = 0; i < 200; i++) {
            g.tickCount = i;
            Fx316.tick(g);
         }

         eq(n, g.particles.size(), "the ceiling holds");
         Game h = newGame(0);
         h.world = null;
         Fx316.tick(h);
         eq(0, h.particles.size(), "no world, no particles");
      });

      test("particles: flames glow and smoke fades (the old particles got the new fields)", () -> {
         Game g = newGame(0);
         g.flameParticle(1.0, 2.0, 3.0, false);
         g.flameParticle(1.0, 2.0, 3.0, true);
         Game.Particle flame = g.particles.get(0);
         Game.Particle smoke = g.particles.get(1);
         check(flame.glow && flame.fade == 3 && flame.r == 1.0F, "a flame glows");
         check(!smoke.glow && smoke.fade == 3 && smoke.alpha < 1.0F && smoke.sway > 0.0F, "smoke is soft");
         Game.Particle plain = new Game.Particle();
         check(!plain.glow && plain.fade == 0 && plain.alpha == 1.0F && plain.sway == 0.0F && plain.kind == 0 && plain.twinkle == 0, "a new particle behaves as before");
      });
   }

   // ------------------------------------------------------------------ the sky
   static void sky() {
      test("sky: the colours of a clear sky are the old ones by day and by night, the dusk is orange and soft", () -> {
         float[] o = new float[8];
         Sky.base(0.0F, o); // noon
         near(0.40F, o[0], 0.002F, "zenith r at noon");
         near(0.60F, o[1], 0.002F, "zenith g at noon");
         near(0.98F, o[2], 0.002F, "zenith b at noon");
         near(0.76F, o[3], 0.002F, "horizon r at noon");
         near(0.85F, o[4], 0.002F, "horizon g at noon");
         near(1.00F, o[5], 0.002F, "horizon b at noon");
         near(1.0F, o[6], 0.0001F, "daylight at noon");
         near(1.0F, o[7], 0.0001F, "the sun stands at the zenith");
         Sky.base((float)Math.PI, o); // midnight
         near(0.01F, o[0], 0.002F, "zenith r at midnight");
         near(0.04F, o[2], 0.002F, "zenith b at midnight");
         near(0.02F, o[3], 0.002F, "horizon r at midnight");
         near(0.0F, o[6], 0.0001F, "no daylight at midnight");
         Sky.base((float)(Math.PI / 2.0), o); // sunset
         check(o[3] > o[5] + 0.15F, "the horizon is orange at sunset: " + o[3] + " " + o[5]);
         check(o[6] > 0.45F && o[6] < 0.55F, "half the daylight at sunset: " + o[6]);
         // the dusk is longer and has no jumps: walk through the whole day
         float[] prev = new float[8];
         Sky.base(-1.0F, prev);
         float maxStep = 0.0F;
         for (int i = 0; i <= 2400; i++) {
            Sky.base(-1.0F + i * (float)(Math.PI * 2.0) / 2400.0F, o);
            for (int k = 0; k < 6; k++) {
               maxStep = Math.max(maxStep, Math.abs(o[k] - prev[k]));
               check(o[k] >= -0.0001F && o[k] <= 1.0001F, "colour channel " + k + " in range at step " + i + ": " + o[k]);
            }

            System.arraycopy(o, 0, prev, 0, 8);
         }

         check(maxStep < 0.01F, "no jumps in the colours over a day: biggest step " + maxStep);
      });

      test("sky: twilight, glow and the glow of the fog: the same numbers in the sky and in the terrain's shader", () -> {
         near(1.0F, Sky.twilight(0.0F), 1.0E-6F, "twilight on the horizon");
         near(0.0F, Sky.twilight(0.5F), 1.0E-6F, "no twilight at noon");
         near(0.0F, Sky.twilight(-0.9F), 1.0E-6F, "no twilight at midnight");
         near(0.28F, Sky.glowStrength(0.0F), 1.0E-5F, "the glow is strongest at dusk");
         near(0.08F, Sky.glowStrength(0.9F), 1.0E-5F, "and weak by day");
         near(0.0F, Sky.glowAmount(-0.2F, 1.0F), 1.0E-6F, "no glow long after the sun has set");
         near(1.0F, Sky.glowAmount(0.3F, 1.0F), 1.0E-6F, "full glow with the sun up");
         near(0.0F, Sky.glowAmount(0.5F, 0.0F), 1.0E-6F, "no glow when the weather hides the sun");
         float[] gc = new float[3];
         Sky.glowColor(0.0F, gc);
         near(0.54F, gc[1], 1.0E-5F, "orange green part at dusk");
         near(0.29F, gc[2], 1.0E-5F, "orange blue part at dusk");
         Sky.glowColor(0.9F, gc);
         near(0.80F, gc[1], 1.0E-5F, "pale green part by day");
         // the shaders spell out the same constants (one function in three programs)
         for (String src : new String[]{ShaderSrc.WORLD_FS, ShaderSrc.ENTITY_FS, ShaderSrc.LOD_FS}) {
            check(src.contains("(0.08 + 0.20*tw)") && src.contains("vec3(1.0, 0.80 - 0.26*tw, 0.55 - 0.26*tw)") && src.contains("smoothstep(0.0, 0.30, abs(sunDir.y))") && src.contains("* fogGlow"),
               "a shader has the glow formula of Sky");
            check(src.contains("uniform float fogGlow;"), "and declares its uniform");
         }
      });

      test("sky: in every direction the sky is the fog colour (plus the sun's glow) at and below the horizon, and has no seam just above it", () -> {
         float[] zen = {0.4F, 0.6F, 0.98F};
         float[] hor = {0.76F, 0.85F, 1.0F};
         float[] out = new float[3];
         float[] up = new float[3];
         for (int t = 0; t < 24000; t += 700) {
            float sa = (float)(Math.PI * 2.0 * (t - 6000) / 24000.0);
            float[] base = new float[8];
            Sky.base(sa, base);
            float[] z = {base[0], base[1], base[2]};
            float[] h = {base[3], base[4], base[5]};
            float[] s = {(float)-Math.sin(sa), (float)Math.cos(sa), 0.0F};
            for (int az = 0; az < 360; az += 20) {
               double a = Math.toRadians(az);
               float dx = (float)Math.cos(a);
               float dz = (float)Math.sin(a);
               Sky.color(out, dx, 0.0F, dz, z, h, s, 1.0F, 1.0F - base[6], 1.0F);
               // what the shader does for the fog at the horizon
               float cs = Math.max(dx * s[0] + dz * s[2], 0.0F);
               float tw = 1.0F - Sky.smooth(0.0F, 0.30F, Math.abs(s[1]));
               float glow = (0.08F + 0.20F * tw) * (float)Math.pow(cs, 5.0) * Sky.glowAmount(s[1], 1.0F);
               near(h[0] + glow * 1.0F, out[0], 0.003F, "horizon r matches the fog, tick " + t + " az " + az);
               near(h[1] + glow * (0.80F - 0.26F * tw), out[1], 0.003F, "horizon g matches the fog");
               near(h[2] + glow * (0.55F - 0.26F * tw), out[2], 0.003F, "horizon b matches the fog");
               // just above: no seam
               float dy = 0.004F;
               float len = (float)Math.sqrt(1.0F + dy * dy);
               Sky.color(up, dx / len, dy / len, dz / len, z, h, s, 1.0F, 1.0F - base[6], 1.0F);
               for (int k = 0; k < 3; k++) {
                  check(Math.abs(up[k] - out[k]) < 0.02F, "no seam at the horizon: tick " + t + " az " + az + " channel " + k + ": " + out[k] + " -> " + up[k]);
               }

               // never negative, never blown out
               for (float dyy : new float[]{0.05F, 0.2F, 0.5F, 0.9F, 1.0F}) {
                  float l = (float)Math.sqrt(1.0F + dyy * dyy);
                  Sky.color(up, dx / l, dyy / l, dz / l, z, h, s, 1.0F, 1.0F - base[6], 1.0F);
                  for (int k = 0; k < 3; k++) {
                     check(up[k] >= 0.0F && up[k] < 1.6F, "colour in range: " + up[k]);
                  }
               }
            }
         }
      });

      test("sky: at dusk the sky is orange on the sun's side and pink-violet on the other, never the same", () -> {
         float sa = (float)(Math.PI * 2.0 * (11800 - 6000) / 24000.0); // a little before sunset
         float[] base = new float[8];
         Sky.base(sa, base);
         float[] z = {base[0], base[1], base[2]};
         float[] h = {base[3], base[4], base[5]};
         float[] s = {(float)-Math.sin(sa), (float)Math.cos(sa), 0.0F};
         float[] towards = new float[3];
         float[] away = new float[3];
         float dy = 0.12F;
         float c = (float)Math.sqrt(1.0F - dy * dy);
         Sky.color(towards, s[0] / Math.max(1.0E-4F, (float)Math.hypot(s[0], s[2])) * c, dy, 0.0F, z, h, s, 1.0F, 0.2F, 1.0F);
         Sky.color(away, -s[0] / Math.max(1.0E-4F, (float)Math.hypot(s[0], s[2])) * c, dy, 0.0F, z, h, s, 1.0F, 0.2F, 1.0F);
         check(towards[0] > away[0] + 0.05F, "redder towards the sun: " + towards[0] + " against " + away[0]);
         check(away[2] > towards[2] - 0.02F, "bluer on the other side: " + away[2] + " against " + towards[2]);
         check(towards[0] - towards[2] > away[0] - away[2] + 0.1F, "warmer towards the sun");
      });

      test("sky: the moon has eight phases (full, waning, new, waxing), drawn as a pixel disc that is lit from the side its phase says", () -> {
         eq(0, Sky.moonPhase(0L), "day 0 has a full moon");
         eq(1, Sky.moonPhase(24000L), "day 1");
         eq(7, Sky.moonPhase(24000L * 7 + 5), "day 7");
         eq(0, Sky.moonPhase(24000L * 8), "and round again");
         near(1.0F, Sky.moonLit(0), 1.0E-6F, "full");
         near(0.5F, Sky.moonLit(2), 1.0E-6F, "half");
         near(0.0F, Sky.moonLit(4), 1.0E-6F, "new");
         near(0.5F, Sky.moonLit(6), 1.0E-6F, "half again");
         double[] bright = new double[8];
         double[] meanX = new double[8];
         for (int p = 0; p < 8; p++) {
            int[] px = Sky.moonPixels(p);
            eq(1024, px.length, "32 x 32");
            int disc = 0;
            int lit = 0;
            double sx = 0;
            for (int y = 0; y < 32; y++) {
               for (int x = 0; x < 32; x++) {
                  int v = px[y * 32 + x];
                  if (v >>> 24 != 0) {
                     disc++;
                     int g = v >> 8 & 255;
                     bright[p] += g;
                     if (g > 140) {
                        lit++;
                        sx += x;
                     }
                  }
               }
            }

            check(disc > 500 && disc < 800, "a round disc of about 650 pixels in phase " + p + ": " + disc);
            bright[p] /= disc;
            meanX[p] = lit > 0 ? sx / lit : 15.5;
            if (p == 0) {
               check(lit > disc * 0.9, "the full moon is lit all over: " + lit + " of " + disc);
            }

            if (p == 4) {
               check(lit == 0 && bright[p] < 30, "the new moon is dark: " + bright[p]);
            }
         }

         check(bright[0] > bright[1] && bright[1] > bright[2] && bright[2] > bright[3] && bright[3] > bright[4], "it wanes: " + java.util.Arrays.toString(bright));
         check(bright[4] < bright[5] && bright[5] < bright[6] && bright[6] < bright[7], "and waxes");
         check(meanX[2] < 15.5 - 3 && meanX[6] > 15.5 + 3, "the quarters are lit on opposite sides: " + meanX[2] + " and " + meanX[6]);
         check(meanX[1] < 15.5 && meanX[3] < 15.5 && meanX[5] > 15.5 && meanX[7] > 15.5, "waning on one side, waxing on the other");
      });

      test("sky: 1,800 stars of every brightness and tint on a sphere of radius 150, a band of them along a tilted great circle, and the glow / sun pictures", () -> {
         eq(1800, Sky.STARS, "the number of stars");
         int bright = 0;
         int dim = 0;
         double bandOff = 0;
         double[] normal = {0.0, Math.cos(Math.toRadians(62.0)), Math.sin(Math.toRadians(62.0))};
         // the band lies on a great circle: its normal, turned
         for (int i = 0; i < Sky.STARS; i++) {
            float x = Sky.STAR[i * 7];
            float y = Sky.STAR[i * 7 + 1];
            float z = Sky.STAR[i * 7 + 2];
            near(150.0F, (float)Math.sqrt(x * x + y * y + z * z), 0.01F, "on the sphere");
            float b = Sky.STAR[i * 7 + 3];
            check(b >= 0.14F && b <= 1.0F, "brightness in range: " + b);
            check(Sky.STAR[i * 7 + 4] >= 0.0F && Sky.STAR[i * 7 + 4] <= 1.0F, "tint in range");
            if (b > 0.8F) {
               bright++;
            }

            if (b < 0.5F) {
               dim++;
            }

            if (i >= 1300) {
               bandOff += Math.abs((x * normal[0] + y * normal[1] + z * normal[2]) / 150.0);
            }
         }

         check(bright > 20 && bright < 400, "a few bright stars: " + bright);
         check(dim > 800, "many dim ones: " + dim);
         check(bandOff / 500 < 0.15, "the band is thin: mean offset " + bandOff / 500);
         int[] sun = Sky.sunPixels();
         int[] glow = Sky.glowPixels();
         eq(1024, sun.length, "the sun is 32 x 32");
         eq(4096, glow.length, "the glow is 64 x 64");
         check(sun[16 * 32 + 16] >>> 24 == 255 && sun[0] >>> 24 == 0, "the sun is a square with transparent corners");
         check(glow[32 * 64 + 32] >>> 24 > 200 && glow[0] >>> 24 == 0 && glow[32 * 64 + 63] >>> 24 < 8, "the glow fades from the middle to nothing");
      });
   }
}
