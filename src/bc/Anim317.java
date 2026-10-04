package bc;

/**
 * Update 3.17: the clips that move the bones of a {@link Rig}. A clip is a plain function of the state of a mob ({@link MState}) - the phase
 * and strength of its walk, where it looks, how long ago it was hit, how far it is in dying ... - and writes into a {@link Pose}; there is
 * no OpenGL in here, so the tests can check every clip for finite, bounded, smooth numbers and the viewer can draw them.
 */
final class Anim317 {
   private Anim317() {
   }

   /** What a clip needs to know about a mob. Angles in degrees, times in ticks. */
   static final class MState {
      /** Age of the mob in ticks, with the fraction of the frame. */
      float t;
      /** The phase of the walk (the stride is sin(walk * 1.6)) and its strength 0..1. */
      float walk;
      float amp;
      /** Where the head looks, relative to the body: yaw (positive = to the right of the mob) and pitch (positive = down). */
      float headYaw;
      float headPitch;
      boolean onGround = true;
      boolean inWater;
      float vy;
      /** 1 right after a hit, down to 0 (about half a second). */
      float hurt;
      /** 1 at the start of an attack, down to 0; and the ticks that are left of it (6 to 10 at the start). */
      float attack;
      float attackT;
      /** 0 while alive, up to 1 while dying (20 ticks); and the ticks since it died. */
      float death;
      float deathT;
      boolean dead;
      boolean moving;
      /** The cooldown of an attack in ticks (a skeleton draws its bow while it runs down). */
      float cd;
      /** Ticks until a ghast shoots (20 down to 1 while it charges; 0 otherwise). */
      float fuse;
      boolean target;
      boolean angry;
      boolean baby;
      boolean flee;
      boolean love;
      /** 0..1: the head is down grazing. */
      float graze;
      /** 0..1: a creeper's fuse. */
      float swell;
      boolean charged;
      boolean sheared;
      int color;
      int variant;
      int phase;
      /** A number that is different for every mob (for the random moments of its life). */
      int seed;
      /** 0..1: how far the legs are spread while the mob is in the air. */
      float air;
      /** The mob's own size (magma cubes 1..3). */
      float size = 1.0F;
      boolean sitting;
   }

   static float smooth(float a, float b, float x) {
      float t = Math.max(0.0F, Math.min(1.0F, (x - a) / (b - a)));
      return t * t * (3.0F - 2.0F * t);
   }

   static float sin(double a) {
      return (float)Math.sin(a);
   }

   static float cos(double a) {
      return (float)Math.cos(a);
   }

   /** Fills the pose (reset before) for the mob type of the rig. */
   static void pose(Rig r, MState s, Pose p) {
      switch (r.type) {
         case Mobs.PIG:
            quad(r, s, p, 34.0F, 0.45F, 1.0F, 0);
            break;
         case Mobs.COW:
            quad(r, s, p, 30.0F, 0.35F, 0.8F, 1);
            break;
         case Mobs.SHEEP:
            quad(r, s, p, 32.0F, 0.4F, 0.9F, 1);
            if (s.sheared) {
               p.hide |= 1 << Rigs317.G_WOOL;
            }

            break;
         case Mobs.CHICKEN:
            chicken(r, s, p);
            break;
         case Mobs.ZOMBIE:
            AnimM317.zombie(r, s, p, false);
            break;
         case Mobs.SKELETON:
            AnimM317.skeleton(r, s, p);
            break;
         case Mobs.CREEPER:
            AnimM317.creeper(r, s, p);
            break;
         case Mobs.SPIDER:
            AnimM317.spider(r, s, p);
            break;
         case Mobs.ZOMBIE_PIGMAN:
            AnimW317.pigman(r, s, p);
            break;
         case Mobs.GHAST:
            AnimW317.ghast(r, s, p);
            break;
         case Mobs.BLAZE:
            AnimW317.blaze(r, s, p);
            break;
         case Mobs.MAGMA_CUBE:
            AnimW317.magma(r, s, p);
            break;
         case Mobs.ENDERMAN:
            AnimW317.enderman(r, s, p);
            break;
         case Mobs.VILLAGER:
            AnimF317.villager(r, s, p);
            break;
         case Mobs.IRON_GOLEM:
            AnimF317.golem(r, s, p);
            break;
         case Mobs.PARROT:
            AnimF317.parrot(r, s, p);
            break;
         default:
            break;
      }

      if (s.hurt > 0.0F && s.death <= 0.0F) {
         p.rootPitch += -14.0F * sin(s.hurt * Math.PI);
      }

      if (s.baby) {
         p.scale(r.idx("head"), 1.35F, 1.35F, 1.35F);
      }

      if (s.death > 0.0F && p.rootRoll == 0.0F && p.rootPivot == 0.0F && p.rootY == 0.0F) {
         deathRoll(s, p, 0.3F);
      }

      blink(r, s, p);
   }

   /**
    * The test hook behind {@code -Dbc.still=1 -Dbc.mobpose=key=value,...}: puts a summoned mob into a state of its AI for a screenshot. Keys: target, angry,
    * attack (ticks left of the swing), cd, swell (0..30), fuse, hurt (ticks left), dead (ticks since death), air (the mob is jumping, vy), walk, amp.
    */
   static void testPose(Mobs.Mob m, String spec) {
      for (String kv : spec.split(",")) {
         String[] p = kv.trim().split("=");
         if (p[0].isEmpty()) {
            continue;
         }

         float v = p.length > 1 ? Float.parseFloat(p[1]) : 1.0F;
         switch (p[0]) {
            case "target":
               m.hasTarget = v > 0;
               break;
            case "angry":
               m.angry = v > 0 ? 600 : 0;
               break;
            case "attack":
               m.attackAnim = (int)v;
               break;
            case "cd":
               m.cd = (int)v;
               break;
            case "swell":
               m.swell = m.pswell = (int)v;
               break;
            case "fuse":
               m.fuse = (int)v;
               break;
            case "hurt":
               m.hurtTime = (int)v;
               break;
            case "dead":
               m.dead = true;
               m.deathTime = (int)v;
               break;
            case "air":
               m.onGround = false;
               m.vy = v;
               break;
            case "walk":
               m.walk = m.pwalk = v;
               break;
            case "amp":
               m.walkAmp = m.pwalkAmp = v;
               break;
            default:
               break;
         }
      }
   }

   // ------------------------------------------------------------------ small things every mob does
   /** A random moment: 1 right after it started, falling to 0 over {@code len} ticks, once in about {@code every} ticks per mob and channel. */
   static float moment(MState s, int channel, float every, float len) {
      float per = every * (0.7F + 0.6F * RigPaint.hash(s.seed, channel, 5, 1));
      float ph = (s.t + RigPaint.hash(s.seed, channel, 9, 2) * per) % per;
      return ph < len ? 1.0F - ph / len : 0.0F;
   }

   /** The eyelids: closed for a few ticks every 2 to 6 seconds (and shut while the mob is dying). */
   static void blink(Rig r, MState s, Pose p) {
      float c = moment(s, 1, 90.0F, 7.0F);
      float closed = c > 0.0F ? 1.0F - Math.abs(c * 2.0F - 1.0F) : 0.0F;
      closed = Math.max(closed, Math.min(1.0F, s.death * 6.0F));
      int l = r.idx("lidL");
      int rr = r.idx("lidR");
      if (l < 0) {
         return;
      }

      if (closed <= 0.02F) {
         p.hide |= 1 << Rigs317.G_LID;
      } else {
         p.scale(l, 1.0F, closed, 1.0F);
         p.scale(rr, 1.0F, closed, 1.0F);
      }
   }

   // ------------------------------------------------------------------ four legs: pig, cow, sheep
   /**
    * The trot of a four legged animal: diagonal legs together, knees that bend while a leg swings forward, the body rising twice per stride,
    * the head nodding against it, ears and tail lagging behind. {@code hipDeg} is how far the hips swing, {@code bob} the lift of the body in
    * model units and {@code lag} how loose the ears are; {@code tailMode} 0 is a curly tail that wags, 1 one that hangs and swishes.
    */
   static void quad(Rig r, MState s, Pose p, float hipDeg, float bob, float lag, int tailMode) {
      float phi = s.walk * 1.6F;
      float a = s.amp;
      float calm = 1.0F - Math.min(1.0F, a * 2.0F);
      int body = r.idx("body");
      int head = r.idx("head");
      String[] names = {"FL", "FR", "BL", "BR"};
      float[] ph = {0.0F, 3.1416F, 3.1416F, 0.0F};
      for (int i = 0; i < 4; i++) {
         int hip = r.idx("leg" + names[i]);
         int knee = r.idx("shin" + names[i]);
         float th = phi + ph[i];
         float swing = -hipDeg * a * sin(th) * (i < 2 ? 1.0F : 0.9F);
         float flex = 46.0F * a * Math.max(0.0F, cos(th)) * (i < 2 ? 0.9F : 1.1F);
         float reach = s.air * 24.0F * (i < 2 ? -1.0F : 1.0F);
         p.rot(hip, swing + reach, 0.0F, 0.0F);
         p.rot(knee, flex + s.air * 18.0F, 0.0F, 0.0F);
      }

      float breath = sin(s.t * 0.11F + s.seed % 17) * 0.012F;
      p.scale(body, 1.0F, 1.0F + breath * (1.0F + calm), 1.0F);
      p.move(body, 0.0F, bob * a * (0.5F + 0.5F * cos(2.0F * phi)), 0.0F);
      p.rot(body, sin(2.0F * phi + 0.4F) * 1.6F * a - s.graze * 4.0F, sin(phi) * 2.4F * a, sin(phi) * 1.8F * a);
      float nod = -sin(2.0F * phi + 0.3F) * 3.0F * a;
      float look = Math.max(-50.0F, Math.min(50.0F, s.headYaw));
      p.rot(head, nod + s.headPitch + s.graze * 58.0F + sin(s.t * 0.9F) * 5.0F * s.graze - 10.0F * hurtK(s), -look - sin(phi) * 2.4F * a, 0.0F);
      float idleLook = calm * sin(s.t * 0.021F + s.seed) * 9.0F;
      p.rot(head, 0.0F, idleLook, 0.0F);
      int earL = r.idx("earL");
      int earR = r.idx("earR");
      float flick = moment(s, 2, 140.0F, 9.0F);
      float flick2 = moment(s, 3, 170.0F, 9.0F);
      p.rot(earL, sin(2.0F * phi - 0.8F) * 9.0F * a * lag - 22.0F * flick * calm, 0.0F, sin(phi) * 6.0F * a * lag - 10.0F * flick * calm);
      p.rot(earR, sin(2.0F * phi - 1.4F) * 9.0F * a * lag - 22.0F * flick2 * calm, 0.0F, -sin(phi) * 6.0F * a * lag + 10.0F * flick2 * calm);
      for (int i = 1; i <= 3; i++) {
         int t = r.idx("tail" + i);
         if (tailMode == 0) {
            float wag = sin(phi * 1.0F - i * 0.55F) * (9.0F + i * 6.0F) * a + sin(s.t * 0.13F - i * 0.7F + s.seed) * 6.0F * calm;
            p.rot(t, sin(s.t * 0.17F - i) * 4.0F, wag, 0.0F);
         } else {
            float swish = sin(s.t * 0.09F + s.seed * 0.7F) * 7.0F * (0.4F + calm) + sin(phi - i * 0.8F) * 14.0F * a;
            float tf = moment(s, 4, 160.0F, 12.0F);
            p.rot(t, sin(2.0F * phi - i * 0.9F) * 6.0F * a + tf * 14.0F * sin(s.t * 0.9F), 0.0F, swish * (i == 1 ? 1.0F : 1.6F) + tf * 18.0F);
         }
      }

      int snout = r.idx("snout");
      p.rot(snout, sin(s.t * 0.55F) * 3.0F * (calm * 0.6F + s.graze), 0.0F, 0.0F);
   }

   // ------------------------------------------------------------------ chicken
   /** A hopping run on two thin legs: the head pecks forward with every step, the wings flap when it falls, it looks around in jerks. */
   static void chicken(Rig r, MState s, Pose p) {
      float phi = s.walk * 1.6F;
      float a = s.amp;
      float calm = 1.0F - Math.min(1.0F, a * 2.0F);
      int body = r.idx("body");
      int head = r.idx("head");
      for (int i = 0; i < 2; i++) {
         float sg = i == 0 ? 1.0F : -1.0F;
         int leg = r.idx(i == 0 ? "legL" : "legR");
         int foot = r.idx(i == 0 ? "footL" : "footR");
         float th = phi + (i == 0 ? 0.0F : 3.1416F);
         p.rot(leg, -44.0F * a * sin(th) + (s.onGround ? 0.0F : 28.0F * sg), 0.0F, 0.0F);
         p.rot(foot, 18.0F * a * Math.max(0.0F, cos(th)) + 44.0F * a * sin(th) * 0.0F - (s.onGround ? 0.0F : 20.0F), 0.0F, 0.0F);
      }

      p.move(body, 0.0F, 0.9F * a * Math.abs(sin(phi)), 0.0F);
      p.rot(body, -5.0F * a + sin(s.t * 0.11F + s.seed) * 0.8F, sin(phi) * 4.0F * a, sin(phi) * 6.0F * a);
      p.scale(body, 1.0F, 1.0F + sin(s.t * 0.13F + s.seed) * 0.02F * (1.0F + calm), 1.0F);
      float peck = sin(phi * 2.0F + 1.2F);
      float look = 0.0F;
      float ev = moment(s, 6, 70.0F, 22.0F);
      if (ev > 0.0F) {
         float dir = RigPaint.hash(s.seed, (int)(s.t / 70.0F), 3, 4) > 0.5F ? 1.0F : -1.0F;
         look = dir * 34.0F * smooth(1.0F, 0.85F, ev) * smooth(0.0F, 0.15F, ev);
      }

      float tilt = ev > 0.0F ? 12.0F * sin(s.t * 1.7F) * smooth(0.0F, 0.2F, ev) : 0.0F;
      float pecking = s.graze * (50.0F + 22.0F * sin(s.t * 0.55F));
      p.move(head, 0.0F, 0.0F, peck * 0.85F * a);
      p.rot(head, peck * 9.0F * a + pecking + s.headPitch - 8.0F * hurtK(s) * 1.0F, -Math.max(-60.0F, Math.min(60.0F, s.headYaw)) + look, tilt);
      int jaw = r.idx("jaw");
      float cluck = moment(s, 7, 150.0F, 10.0F);
      p.rot(jaw, 26.0F * cluck * Math.abs(sin(s.t * 1.1F)) + s.attack * 10.0F, 0.0F, 0.0F);
      float flap = s.onGround ? 0.0F : 1.0F;
      float panic = s.flee ? 0.5F : 0.0F;
      float ft = s.t * 1.15F;
      for (int i = 0; i < 2; i++) {
         float sg = i == 0 ? 1.0F : -1.0F;
         int w = r.idx(i == 0 ? "wingL" : "wingR");
         float up = flap * (46.0F * sin(ft) + 42.0F) + panic * a * (22.0F * sin(ft * 1.3F) + 16.0F);
         float shake = moment(s, 8, 210.0F, 14.0F) * calm * sin(s.t * 1.9F) * 12.0F;
         p.rot(w, 0.0F, flap * 8.0F * sin(ft + 1.0F) * 0.0F, -sg * (up + shake));
      }

      int tail = r.idx("tail1");
      p.rot(tail, sin(phi * 2.0F + 0.6F) * 6.0F * a + sin(s.t * 0.19F) * 2.5F, sin(phi) * 7.0F * a + sin(s.t * 0.11F + s.seed) * 4.0F * calm, 0.0F);
      if (!s.onGround) {
         p.rot(body, 10.0F, 0.0F, 0.0F);
      }
   }

   static float hurtK(MState s) {
      return s.hurt <= 0.0F ? 0.0F : sin(s.hurt * Math.PI);
   }

   /** The old fall: a dead mob rolls over onto its side and lies there. */
   static void deathRoll(MState s, Pose p, float pivotHeight) {
      if (s.death > 0.0F) {
         float k = Math.min(1.0F, s.death);
         p.rootPivot = pivotHeight * k;
         p.rootRoll = 90.0F * k;
      }
   }
}
