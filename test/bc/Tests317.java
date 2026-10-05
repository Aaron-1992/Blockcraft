package bc;

import static bc.GameTests.GROUND;
import static bc.GameTests.check;
import static bc.GameTests.eq;
import static bc.GameTests.newGame;
import static bc.GameTests.test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Headless tests of Update 3.17 (jointed mob models, skins and the animation of every mob). Nothing here needs OpenGL: a clip only fills a
 * {@link Pose}, and the pose, the bones and the painted skin are plain Java.   python3 tools/runtests.py unit
 */
final class Tests317 {
   static void run() {
      rigs();
      clips();
      hooks();
   }

   // ------------------------------------------------------------------ states a mob can be in
   /** A broad set of states of one mob: standing, walking, attacking, hurt, dying, angry, fleeing, a baby, in the air ... */
   static ArrayList<Anim317.MState> states(int type) {
      ArrayList<Anim317.MState> list = new ArrayList<>();
      for (int seed = 0; seed < 3; seed++) {
         for (int k = 0; k < 16; k++) {
            Anim317.MState s = new Anim317.MState();
            s.seed = seed * 17 + 3;
            s.t = 37 + k * 53.3F + seed * 11;
            s.walk = k * 0.9F;
            s.color = k % 6;
            switch (k) {
               case 1:
                  s.amp = 1.0F;
                  break;
               case 2:
                  s.amp = 0.4F;
                  s.headYaw = 40.0F;
                  s.headPitch = 20.0F;
                  break;
               case 3:
                  s.amp = 1.0F;
                  s.target = true;
                  s.headYaw = -35.0F;
                  break;
               case 4:
                  s.attackT = 7.0F;
                  s.attack = 0.9F;
                  s.target = true;
                  break;
               case 5:
                  s.hurt = 0.7F;
                  break;
               case 6:
                  s.dead = true;
                  s.death = 0.5F;
                  s.deathT = 10.0F;
                  break;
               case 7:
                  s.angry = true;
                  s.target = true;
                  s.cd = 12.0F;
                  s.fuse = 9.0F;
                  s.swell = 0.6F;
                  break;
               case 8:
                  s.flee = true;
                  s.amp = 1.0F;
                  break;
               case 9:
                  s.baby = true;
                  s.sheared = true;
                  s.love = true;
                  break;
               case 10:
                  s.onGround = false;
                  s.air = 0.7F;
                  s.vy = 0.3F;
                  s.phase = Mobs312.P_FLY;
                  break;
               case 11:
                  s.phase = Mobs312.P_SHOULDER;
                  s.size = 2.0F;
                  s.inWater = true;
                  s.graze = 0.8F;
                  break;
               case 12:
                  s.phase = Dragon.PERCH;
                  s.charged = true;
                  s.sitting = true;
                  break;
               case 13:
                  s.phase = Dragon.LAND;
                  s.walk = 3.0F;
                  s.amp = 0.6F;
                  break;
               case 14:
                  s.dead = true;
                  s.death = 1.0F;
                  s.deathT = 199.0F;
                  break;
               case 15:
                  s.phase = Mobs312.P_LAND;
                  s.moving = true;
                  s.amp = 0.5F;
                  break;
               default:
                  break;
            }

            list.add(s);
         }
      }

      return list;
   }

   static boolean finite(float v) {
      return !Float.isNaN(v) && !Float.isInfinite(v);
   }

   // ------------------------------------------------------------------ the models
   static void rigs() {
      test("3.17 rigs: every one of the 18 mobs has a jointed model with a painted skin of a sensible size", () -> {
         eq(18, Mobs.N, "eighteen kinds of mob");
         for (int t = 0; t < Mobs.N; t++) {
            String n = Mobs.NAME[t];
            Rig r = Rigs317.build(t);
            check(r != null, n + " has a rig");
            check(r.size() >= 7, n + " has joints: " + r.size());
            check(r.tw >= 64 && r.tw <= 512 && r.th >= 32 && r.th <= 1024, n + " skin " + r.tw + "x" + r.th);
            check(r.pix.length == r.tw * r.th, n + " skin size");
            int solid = 0;
            for (int c : r.pix) {
               if ((c >>> 24) >= 128) {
                  solid++;
               }
            }

            check(solid > r.pix.length / 8, n + ": most of the skin is painted (" + solid + " of " + r.pix.length + ")");
            // the model has to fit its hit box: arrows and the crosshair test the box, not the model
            float[] b = r.bounds();
            float scale = r.scale / 16.0F;
            float top = b[4] * scale;
            check(top >= Mobs.H[t] * 0.6F && top <= Mobs.H[t] * 1.3F, n + ": model top " + top + " blocks, hit box " + Mobs.H[t]);
            check(b[1] * scale > -3.3F && b[1] * scale < Mobs.H[t], n + ": the model starts at " + b[1] * scale);
            check(r.height() > 0.0F, n + " has a height");
         }
      });

      test("3.17 rigs: the skins are twice as fine as the old 16 pixel skins (2 pixels to a unit), the parrot three, the ghast one for its huge faces", () -> {
         eq(2, Rigs317.build(Mobs.PIG).ts, "pig");
         eq(2, Rigs317.build(Mobs.ZOMBIE).ts, "zombie");
         eq(2, Rigs317.build(Mobs.ENDER_DRAGON).ts, "dragon");
         eq(3, Rigs317.build(Mobs.PARROT).ts, "parrot");
         eq(1, Rigs317.build(Mobs.GHAST).ts, "ghast");
      });

      test("3.17 rigs: the groups - sheep wool, parrot feathers and villager robes are tinted by the renderer, the eyelids and the clothes can be hidden", () -> {
         Rig sheep = Rigs317.build(Mobs.SHEEP);
         eq(Rig.TINT_WOOL, sheep.groupTint[Rigs317.G_WOOL], "wool tint");
         Rig parrot = Rigs317.build(Mobs.PARROT);
         eq(Rig.TINT_FEATHER, parrot.groupTint[Rigs317.G_FEATHER], "feather tint");
         Rig villager = Rigs317.build(Mobs.VILLAGER);
         eq(Rig.TINT_ROBE, villager.groupTint[Rigs317.G_ROBE], "robe tint");
         for (int t = 0; t < Mobs.N; t++) {
            Rig r = Rigs317.build(t);
            for (Rig.Bone b : r.bones) {
               for (Rig.Cube c : b.cubes) {
                  check(c.group >= 0 && c.group < Rig.GROUPS, Mobs.NAME[t] + "/" + b.name + ": group " + c.group);
               }
            }
         }
      });

      test("3.17 rigs: the paint is deterministic - building a rig twice gives the same skin (the skins are built on a background thread)", () -> {
         for (int t : new int[]{Mobs.PIG, Mobs.ENDERMAN, Mobs.VILLAGER, Mobs.ENDER_DRAGON}) {
            Rig a = Rigs317.build(t);
            Rig b = Rigs317.build(t);
            check(java.util.Arrays.equals(a.pix, b.pix), Mobs.NAME[t] + " skin is the same twice");
         }
      });

      test("3.17 rigs: the background warm-up paints every rig and RigDraw takes them from there", () -> {
         RigDraw.warm();
         long end = System.currentTimeMillis() + 60000L;
         while (RigDraw.BUILT.size() < Mobs.N && System.currentTimeMillis() < end) {
            Thread.sleep(50);
         }

         // RigDraw.rig() removes what it hands out (it needs OpenGL, so nothing in a headless test asks): all 18 are waiting
         for (int t = 0; t < Mobs.N; t++) {
            check(RigDraw.BUILT.containsKey(t) || RigDraw.warmedAndTaken(t), Mobs.NAME[t] + " was painted in the background");
         }
      });
   }

   // ------------------------------------------------------------------ the clips
   static void clips() {
      test("3.17 clips: every bone a clip asks for exists in its rig (a typo would silently animate nothing)", () -> {
         Set<String> missing = new HashSet<>();
         Rig.missingNames = missing;
         try {
            for (int t = 0; t < Mobs.N; t++) {
               Rig r = Rigs317.build(t);
               Pose p = r.pose();
               for (Anim317.MState s : states(t)) {
                  p.reset();
                  Anim317.pose(r, s, p);
               }
            }
         } finally {
            Rig.missingNames = null;
         }

         // the generic parts of a clip (blinking, a baby's big head) look for what a rig may not have
         missing.removeIf(n -> n.endsWith(":lidL") || n.endsWith(":lidR") || n.endsWith(":head"));
         // the shared four-legged clip also looks for a snout and a longer tail, and the zombie clip for a jaw: optional parts, not every model has them
         missing.removeIf(n -> n.equals("1:tail3") || n.equals("1:snout") || n.equals("2:tail2") || n.equals("2:tail3") || n.equals("2:snout") || n.equals("4:jaw"));
         check(missing.isEmpty(), "bones that a clip asks for and the rig does not have: " + missing);
      });

      test("3.17 clips: every pose is finite and sane for every mob in sixteen different states", () -> {
         for (int t = 0; t < Mobs.N; t++) {
            Rig r = Rigs317.build(t);
            Pose p = r.pose();
            for (Anim317.MState s : states(t)) {
               p.reset();
               Anim317.pose(r, s, p);
               String where = Mobs.NAME[t] + " t=" + s.t + " walk=" + s.walk + " dead=" + s.dead + " phase=" + s.phase;
               for (int i = 0; i < r.size(); i++) {
                  check(finite(p.rx[i]) && finite(p.ry[i]) && finite(p.rz[i]), where + ": rotation of " + r.bones.get(i).name);
                  check(Math.abs(p.rx[i]) < 400 && Math.abs(p.ry[i]) < 400 && Math.abs(p.rz[i]) < 400, where + ": " + r.bones.get(i).name + " turns by " + p.rx[i] + "," + p.ry[i] + "," + p.rz[i]);
                  check(finite(p.ox[i]) && finite(p.oy[i]) && finite(p.oz[i]) && Math.abs(p.ox[i]) < 60 && Math.abs(p.oy[i]) < 60 && Math.abs(p.oz[i]) < 60, where + ": offset of " + r.bones.get(i).name);
                  check(p.sx[i] > 0.02F && p.sy[i] > 0.02F && p.sz[i] > 0.02F && p.sx[i] < 5 && p.sy[i] < 5 && p.sz[i] < 5, where + ": scale of " + r.bones.get(i).name + " " + p.sx[i] + "," + p.sy[i] + "," + p.sz[i]);
               }

               check(finite(p.rootY) && finite(p.rootPitch) && finite(p.rootRoll) && finite(p.rootYaw) && Math.abs(p.rootY) < 3, where + ": root");
               check(p.rootScaleX > 0 && p.rootScaleY > 0 && p.rootScaleZ > 0, where + ": root scale");
               check((p.hide & 1) == 0, where + ": group 0 is never hidden");
               check(p.tintR > 0 && p.tintG > 0 && p.tintB > 0 && p.tintR < 4 && p.tintG < 4 && p.tintB < 4, where + ": tint");
               double[][] wm = r.world(p);
               for (double[] m : wm) {
                  for (double v : m) {
                     check(!Double.isNaN(v) && !Double.isInfinite(v) && Math.abs(v) < 5000, where + ": bone matrix");
                  }
               }
            }
         }
      });

      test("3.17 clips: no pops - the pose of every mob changes smoothly while it stands about, walks, attacks and dies", () -> {
         for (int t = 0; t < Mobs.N; t++) {
            Rig r = Rigs317.build(t);
            Pose p = r.pose();
            float[] prev = null;
            // standing about: 600 ticks in steps of a fiftieth of a tick, the random moments (looks, blinks, preening) included
            for (int rep = 0; rep < 2; rep++) {
               prev = null;
               for (int i = 0; i <= 12000; i++) {
                  Anim317.MState s = new Anim317.MState();
                  s.seed = 5 + rep * 29;
                  s.t = 10 + i * 0.05F;
                  if (rep == 1) {
                     s.phase = Mobs312.P_LAND;
                  }

                  prev = step(r, p, s, prev, Mobs.NAME[t] + " standing t=" + s.t, 0.05F, 150.0F);
               }
            }

            // walking: the walk phase and the clock run together
            prev = null;
            for (int i = 0; i <= 1200; i++) {
               Anim317.MState s = new Anim317.MState();
               s.seed = 7;
               s.walk = 2 + i * 0.01F;
               s.t = 50 + i * 0.0125F;
               s.amp = 1.0F;
               s.onGround = t != Mobs.PARROT;
               prev = step(r, p, s, prev, Mobs.NAME[t] + " walking walk=" + s.walk, 0.0125F, 150.0F);
            }

            // an attack and a death, tick by tick in a hundredth
            prev = null;
            for (int i = 0; i <= 1000; i++) {
               Anim317.MState s = new Anim317.MState();
               s.seed = 9;
               s.t = 80;
               s.target = true;
               s.attackT = 10.0F - i * 0.01F;
               s.attack = Math.min(1.0F, s.attackT / 8.0F);
               prev = step(r, p, s, prev, Mobs.NAME[t] + " attack " + s.attackT, 0.01F, 400.0F);
            }

            prev = null;
            for (int i = 0; i <= 2000; i++) {
               Anim317.MState s = new Anim317.MState();
               s.seed = 11;
               s.t = 80 + i * 0.01F;
               s.dead = true;
               s.deathT = i * 0.01F;
               s.death = Math.min(1.0F, s.deathT / (t == Mobs.ENDER_DRAGON ? Dragon.DEATH_TICKS : 20.0F));
               prev = step(r, p, s, prev, Mobs.NAME[t] + " death " + s.deathT, 0.01F, 400.0F);
            }
         }
      });

      test("3.17 clips: the walk cycle closes - one stride later the legs are where they were", () -> {
         for (int t : new int[]{Mobs.PIG, Mobs.COW, Mobs.SHEEP, Mobs.CHICKEN, Mobs.ZOMBIE, Mobs.SKELETON, Mobs.CREEPER, Mobs.SPIDER, Mobs.ZOMBIE_PIGMAN, Mobs.ENDERMAN, Mobs.VILLAGER, Mobs.IRON_GOLEM}) {
            // a spider's eight legs step a quarter faster, a creeper's four a little faster than the rest
            float period = (float)(Math.PI * 2.0 / (t == Mobs.SPIDER ? 2.0 : t == Mobs.CREEPER ? 1.84 : 1.6));
            Rig r = Rigs317.build(t);
            Pose p = r.pose();
            for (int k = 0; k < 8; k++) {
               Anim317.MState a = new Anim317.MState();
               a.seed = 3;
               a.t = 70;
               a.amp = 1.0F;
               a.walk = k * 0.5F;
               p.reset();
               Anim317.pose(r, a, p);
               float[] before = snapshot(r, p);
               Anim317.MState b = new Anim317.MState();
               b.seed = 3;
               b.t = 70;
               b.amp = 1.0F;
               b.walk = k * 0.5F + period;
               p.reset();
               Anim317.pose(r, b, p);
               float[] after = snapshot(r, p);
               for (int i = 0; i < before.length; i++) {
                  if (Float.isNaN(before[i]) || Float.isNaN(after[i])) {
                     continue; // hidden bones
                  }

                  check(Math.abs(before[i] - after[i]) < 0.02F, Mobs.NAME[t] + ": the stride is not periodic (value " + i + ": " + before[i] + " vs " + after[i] + ")");
               }
            }
         }
      });

      test("3.17 clips: a walking mob moves its legs, a standing one does not", () -> {
         for (int t : new int[]{Mobs.PIG, Mobs.COW, Mobs.SHEEP, Mobs.CHICKEN, Mobs.ZOMBIE, Mobs.SKELETON, Mobs.SPIDER, Mobs.ZOMBIE_PIGMAN, Mobs.ENDERMAN, Mobs.VILLAGER, Mobs.IRON_GOLEM}) {
            Rig r = Rigs317.build(t);
            Pose p = r.pose();
            float[] lo = {1e9F, 1e9F, 1e9F};
            float[] hi = {-1e9F, -1e9F, -1e9F};
            int leg = -1;
            for (Rig.Bone b : r.bones) {
               if (b.name.startsWith("leg") && leg < 0) {
                  leg = b.index;
               }
            }

            check(leg >= 0, Mobs.NAME[t] + " has a leg");
            for (int i = 0; i < 40; i++) {
               Anim317.MState s = new Anim317.MState();
               s.seed = 3;
               s.t = 70;
               s.amp = 1.0F;
               s.walk = i * 0.1F;
               p.reset();
               Anim317.pose(r, s, p);
               float[] v = {p.rx[leg], p.ry[leg], p.rz[leg]};
               for (int k = 0; k < 3; k++) {
                  lo[k] = Math.min(lo[k], v[k]);
                  hi[k] = Math.max(hi[k], v[k]);
               }
            }

            float swing = Math.max(hi[0] - lo[0], Math.max(hi[1] - lo[1], hi[2] - lo[2]));
            check(swing > 15.0F, Mobs.NAME[t] + ": the leg swings " + swing + " degrees");
            Anim317.MState idle = new Anim317.MState();
            idle.seed = 3;
            idle.t = 0; // (a spider lifts a leg now and then, but not at the start of its clock)
            p.reset();
            Anim317.pose(r, idle, p);
            check(Math.abs(p.rx[leg]) < 3.0F && Math.abs(p.ry[leg]) < 3.0F && Math.abs(p.rz[leg]) < 3.0F, Mobs.NAME[t] + ": a standing mob keeps its leg still: " + p.rx[leg] + " " + p.ry[leg] + " " + p.rz[leg]);
         }
      });

      test("3.17 clips: clothes by profession - a farmer wears the hat, a librarian the glasses, a priest the cap, a blacksmith and a butcher their aprons, a nitwit nothing", () -> {
         Rig r = Rigs317.build(Mobs.VILLAGER);
         Pose p = r.pose();
         int[] wears = {Folk317.V_HAT, Folk317.V_GLASSES, Folk317.V_CAP, Folk317.V_SMITH, Folk317.V_BUTCHER, -1};
         int all = 1 << Folk317.V_HAT | 1 << Folk317.V_GLASSES | 1 << Folk317.V_CAP | 1 << Folk317.V_SMITH | 1 << Folk317.V_BUTCHER;
         for (int color = 0; color < 6; color++) {
            Anim317.MState s = new Anim317.MState();
            s.color = color;
            p.reset();
            Anim317.pose(r, s, p);
            int shown = all & ~p.hide;
            eq(wears[color] < 0 ? 0 : 1 << wears[color], shown, "profession " + color + " (" + Trades.NAMES[color] + ") shows the right clothes");
            check((p.hide >> Rigs317.G_ROBE & 1) == 0, "the robe is always worn");
         }
      });

      test("3.17 clips: a sheared sheep loses its wool, a lamb has a bigger head, creepers swell, ghasts open their mouths", () -> {
         Rig sheep = Rigs317.build(Mobs.SHEEP);
         Pose p = sheep.pose();
         Anim317.MState s = new Anim317.MState();
         p.reset();
         Anim317.pose(sheep, s, p);
         eq(0, p.hide >> Rigs317.G_WOOL & 1, "wool shown");
         s.sheared = true;
         p.reset();
         Anim317.pose(sheep, s, p);
         eq(1, p.hide >> Rigs317.G_WOOL & 1, "wool hidden after shearing");
         Rig pig = Rigs317.build(Mobs.PIG);
         p = pig.pose();
         Anim317.MState b = new Anim317.MState();
         b.baby = true;
         p.reset();
         Anim317.pose(pig, b, p);
         check(p.sx[pig.idx("head")] > 1.2F, "a piglet's head is out of proportion");
         Rig creeper = Rigs317.build(Mobs.CREEPER);
         p = creeper.pose();
         float lo = 1e9F;
         float hi = -1e9F;
         for (int i = 0; i <= 10; i++) {
            Anim317.MState c = new Anim317.MState();
            c.swell = i / 10.0F;
            c.target = true;
            p.reset();
            Anim317.pose(creeper, c, p);
            float w = p.rootScaleX;
            lo = Math.min(lo, w);
            hi = Math.max(hi, w);
         }

         check(hi > lo * 1.15F, "a creeper swells before it blows: " + lo + " to " + hi);
      });

      test("3.17 clips: the dragon flaps (a wave runs along the wing), folds its wings on the perch and whitens while it dies", () -> {
         Rig d = Rigs317.build(Mobs.ENDER_DRAGON);
         Pose p = d.pose();
         int wing = d.idx("wingL");
         int tip = d.idx("wingTipL");
         float lo = 1e9F;
         float hi = -1e9F;
         boolean lag = false;
         for (int i = 0; i < 60; i++) {
            Anim317.MState s = new Anim317.MState();
            s.walk = i * 0.4F;
            s.onGround = false;
            p.reset();
            Anim317.pose(d, s, p);
            lo = Math.min(lo, p.rz[wing]);
            hi = Math.max(hi, p.rz[wing]);
            lag |= Math.abs(p.rz[wing] - p.rz[tip]) > 6.0F;
         }

         check(hi - lo > 50.0F, "wing beat " + (hi - lo));
         check(lag, "the tip of the wing lags behind");
         Anim317.MState perch = new Anim317.MState();
         perch.phase = Dragon.PERCH;
         int mid = d.idx("wingMidL"); // Update 3.18: the wing has an elbow; it is the elbow that folds the wing back along the body
         int folded = 0;
         float minMid = 1e9F;
         for (int t = 0; t < 1200; t += 6) {
            perch.t = t;
            p.reset();
            Anim317.pose(d, perch, p);
            minMid = Math.min(minMid, p.ry[mid]);
            if (p.ry[wing] > 25.0F && p.ry[mid] > 55.0F && p.ry[tip] > 25.0F) {
               folded++;
            }
         }

         check(folded > 140, "folded back on the perch (200 samples; now and then it stretches its wings): " + folded);
         check(minMid > 40.0F, "the elbow stays bent even while it stretches: " + minMid);
         Anim317.MState dying = new Anim317.MState();
         dying.dead = true;
         dying.deathT = 150;
         dying.death = 0.75F;
         p.reset();
         Anim317.pose(d, dying, p);
         check(p.tintR > 1.3F, "whitens: " + p.tintR);
         check(p.rootY != 0.0F, "shakes");
      });
   }

   /** Poses a state, compares it with the one before and returns the pose: a rotation may not jump by more than {@code maxRate} degrees per tick. */
   static float[] step(Rig r, Pose p, Anim317.MState s, float[] prev, String where, float dt, float maxRate) {
      p.reset();
      Anim317.pose(r, s, p);
      float[] now = snapshot(r, p);
      if (prev != null) {
         for (int i = 0; i < now.length; i++) {
            if (Float.isNaN(now[i]) || Float.isNaN(prev[i])) {
               continue; // a bone that is hidden in one of the two pictures
            }

            float d = Math.abs(now[i] - prev[i]);
            if (i < 3 * p.rx.length) {
               d = Math.abs((now[i] - prev[i] + 540.0F) % 360.0F - 180.0F); // a rotation: 359 and 1 degree are two degrees apart
            }

            if (d > maxRate * dt + 1.0E-4F) {
               check(false, where + ": value " + i + " (" + valueName(r, i) + ") jumps by " + d + " in " + dt + " ticks");
            }
         }
      }

      return now;
   }

   static String valueName(Rig r, int i) {
      int bones = r.size();
      if (i < bones * 9) {
         String[] k = {"rx", "ry", "rz", "ox", "oy", "oz", "sx", "sy", "sz"};
         return r.bones.get(i % bones).name + "." + k[i / bones];
      }

      return "root " + (i - bones * 9);
   }

   /** Everything a pose says, as one array: rotations, offsets (a unit counts as 2 degrees), scales (a hundredth counts as a degree), the root. */
   static float[] snapshot(Rig r, Pose p) {
      int n = p.rx.length;
      float[] a = new float[n * 9 + 8];
      for (int i = 0; i < n; i++) {
         // a bone whose boxes are all hidden (the eyelids while the eyes are open) may do what it likes
         boolean shown = r.bones.get(i).cubes.isEmpty();
         for (Rig.Cube c : r.bones.get(i).cubes) {
            shown |= c.group == 0 || (p.hide >> c.group & 1) == 0;
         }

         if (!shown) {
            for (int k = 0; k < 9; k++) {
               a[k * n + i] = Float.NaN;
            }

            continue;
         }

         a[i] = p.rx[i];
         a[n + i] = p.ry[i];
         a[2 * n + i] = p.rz[i];
         a[3 * n + i] = p.ox[i] * 2.0F;
         a[4 * n + i] = p.oy[i] * 2.0F;
         a[5 * n + i] = p.oz[i] * 2.0F;
         a[6 * n + i] = (p.sx[i] - 1.0F) * 100.0F;
         a[7 * n + i] = (p.sy[i] - 1.0F) * 100.0F;
         a[8 * n + i] = (p.sz[i] - 1.0F) * 100.0F;
      }

      a[n * 9] = p.rootY * 32.0F;
      a[n * 9 + 1] = p.rootPitch;
      a[n * 9 + 2] = p.rootRoll;
      a[n * 9 + 3] = p.rootYaw;
      a[n * 9 + 4] = (p.rootScaleX - 1.0F) * 100.0F;
      a[n * 9 + 5] = (p.rootScaleY - 1.0F) * 100.0F;
      a[n * 9 + 6] = (p.rootScaleZ - 1.0F) * 100.0F;
      a[n * 9 + 7] = p.hide;
      return a;
   }

   // ------------------------------------------------------------------ the game side
   static void hooks() {
      test("3.17 RigDraw.fill: what a clip knows is read from the mob, interpolated between two ticks", () -> {
         Game g = newGame(0);
         Mobs.Mob m = g.mobs.spawn(Mobs.ZOMBIE, 8.5, GROUND + 1, 8.5);
         m.age = 100;
         m.pwalk = 2.0F;
         m.walk = 3.0F;
         m.pwalkAmp = 0.0F;
         m.walkAmp = 1.0F;
         m.hasTarget = true;
         m.attackAnim = 6;
         m.hurtTime = 5;
         m.color = 3;
         Anim317.MState s = new Anim317.MState();
         RigDraw.fill(s, m, 0.5F);
         check(Math.abs(s.t - 100.5F) < 1.0E-4F, "the clock is the age plus the part of the tick: " + s.t);
         check(Math.abs(s.walk - 2.5F) < 1.0E-4F && Math.abs(s.amp - 0.5F) < 1.0E-4F, "walk " + s.walk + " amp " + s.amp);
         check(s.target && !s.dead && s.attackT > 5.0F && s.attackT < 6.0F && s.hurt > 0.4F && s.hurt < 0.5F, "target, attack and hurt: " + s.attackT + " " + s.hurt);
         eq(3, s.color, "colour");
         m.dead = true;
         m.deathTime = 10;
         RigDraw.fill(s, m, 0.5F);
         check(s.dead && Math.abs(s.death - 0.525F) < 1.0E-3F && Math.abs(s.deathT - 10.5F) < 1.0E-4F, "dying: " + s.death + " " + s.deathT);
      });

      test("3.17 hooks: -Dbc.still keeps a mob where it was put (only its animation clock runs), -Dbc.mobpose puts it into a state of its AI", () -> {
         Game g = newGame(0);
         g.player.setPos(30.5, GROUND + 1, 30.5);
         Mobs.Mob m = g.mobs.spawn(Mobs.ZOMBIE, 8.5, GROUND + 1, 8.5);
         m.still = true;
         Anim317.testPose(m, "target=1,attack=5,cd=9,swell=20,fuse=7,hurt=6,angry=1,walk=2.5,amp=0.5");
         check(m.hasTarget && m.attackAnim == 5 && m.cd == 9 && m.swell == 20 && m.fuse == 7 && m.hurtTime == 6 && m.angry > 0, "state set");
         check(m.walk == 2.5F && m.walkAmp == 0.5F, "walk set");
         double x = m.x;
         double y = m.y;
         double z = m.z;
         int age = m.age;
         for (int i = 0; i < 80; i++) {
            g.mobs.tick();
         }

         check(m.x == x && m.y == y && m.z == z, "it stood still");
         eq(age + 80, m.age, "its clock ran");
         eq(5, m.attackAnim, "its AI did not run");
         Mobs.Mob d = g.mobs.spawn(Mobs.PIG, 10.5, GROUND + 1, 10.5);
         d.still = true;
         Anim317.testPose(d, "dead=8");
         for (int i = 0; i < 60; i++) {
            g.mobs.tick();
         }

         check(g.mobs.list.contains(d) && d.dead && d.deathTime == 19, "a still dead mob lies there in the last picture of its death: " + d.deathTime);
         Mobs.Mob w = g.mobs.spawn(Mobs.COW, 12.5, GROUND + 1, 12.5);
         Anim317.testPose(w, "bogus=3,,x");
         check(!w.dead && !w.hasTarget, "unknown keys are ignored");
      });
   }
}
