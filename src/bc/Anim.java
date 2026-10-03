package bc;

/**
 * Update 3.11: the secondary motion that makes the mobs (and later the player) feel alive - what Minecraft packs like "Fresh Animations" add to
 * the vanilla walk cycle: the body bounces and rolls with every step, breathes when it stands, a hit throws it back, animals put their
 * heads down and chew, the head nods while walking, babies have bigger heads, the legs spread in the air. Plain functions of a mob's state and
 * the time, no OpenGL (Tests311 checks that they are smooth and bounded), used by {@link MobRender} and {@link PlayerModel}.
 */
final class Anim {
   private Anim() {
   }

   static float smooth(float a, float b, float x) {
      float t = Math.max(0.0F, Math.min(1.0F, (x - a) / (b - a)));
      return t * t * (3.0F - 2.0F * t);
   }

   /** Pigs, cows and sheep. */
   static boolean animal(int type) {
      return type == Mobs.PIG || type == Mobs.COW || type == Mobs.SHEEP;
   }

   /** How high the body is lifted by the step (in blocks): twice per stride; creepers and spiders glide. */
   static float bounce(int type, float walk, float amp, boolean onGround) {
      if (!onGround || type == Mobs.CREEPER || type == Mobs.SPIDER) {
         return 0.0F;
      }

      float k = type == Mobs.CHICKEN ? 0.05F : (type == Mobs.ZOMBIE || type == Mobs.SKELETON ? 0.04F : 0.03F);
      return Math.abs((float)Math.sin(walk * 1.6F)) * amp * k;
   }

   /** The body leans from side to side with the stride (degrees about the forward axis). */
   static float roll(int type, float walk, float amp) {
      if (type == Mobs.SPIDER) {
         return 0.0F;
      }

      float k = type == Mobs.ZOMBIE || type == Mobs.SKELETON ? 3.0F : (type == Mobs.CHICKEN ? 5.0F : 2.2F);
      return (float)Math.sin(walk * 1.6F) * amp * k;
   }

   /** The chest rises and falls: a scale of the height around 1, a little less when the mob is walking. */
   static float breathe(Mobs.Mob m, float partial) {
      float calm = 1.0F - Math.min(1.0F, m.walkAmp * 2.0F);
      return 1.0F + (0.010F + 0.006F * calm) * (float)Math.sin((m.age + partial) * 0.11F + m.type * 1.7F);
   }

   /** A hit throws the body back for a moment (degrees about the side axis, negative = backwards). */
   static float hurtTilt(Mobs.Mob m, float partial) {
      if (m.hurtTime <= 0) {
         return 0.0F;
      }

      float t = Math.max(0.0F, Math.min(1.0F, (m.hurtTime - partial) / 10.0F));
      return -14.0F * (float)Math.sin(t * Math.PI);
   }

   /**
    * 0..1: how far the head is down while the animal grazes. Every animal does it for about 3 seconds every 20 seconds at its own pace, only when
    * it stands still and is neither hunted nor in love.
    */
   /** -Dbc.graze=1 holds every animal in the middle of grazing (screenshot scenes). */
   static final String FORCE_GRAZE = System.getProperty("bc.graze");

   static float graze(Mobs.Mob m, float partial) {
      if (FORCE_GRAZE != null && animal(m.type) && !m.dead) {
         return Float.parseFloat(FORCE_GRAZE);
      }

      if (!animal(m.type) || m.dead || m.hasTarget || m.moving || m.walkAmp > 0.05F || m.flee > 0 || m.love > 0 || m.babyAge > 0 && m.type == Mobs.SHEEP) {
         return 0.0F;
      }

      int seed = (m.type * 7919 + (int)Math.floor(m.x * 13.0) * 31 + (int)Math.floor(m.z * 17.0) * 17) & 0x3FF;
      float ph = ((m.age + partial) + seed) % 400.0F;
      return smooth(0.0F, 14.0F, ph) * (1.0F - smooth(62.0F, 78.0F, ph));
   }

   /** Head pitch added to what the AI asks for (degrees, positive = down): the nod of the walk, the grazing, the chewing. */
   static float headPitch(Mobs.Mob m, float partial, float walk, float amp) {
      float nod = m.type == Mobs.CHICKEN ? 0.0F : (float)Math.sin(walk * 3.2F) * amp * (animal(m.type) ? 4.0F : 2.5F);
      float g = graze(m, partial);
      float chew = (float)Math.sin((m.age + partial) * 0.9F) * 6.0F * g;
      return nod + g * 58.0F + chew;
   }

   /** The chicken pecks the air with its head when it walks: how far the head is pushed forward (blocks). */
   static float peck(Mobs.Mob m, float walk, float amp) {
      return m.type == Mobs.CHICKEN ? (float)Math.sin(walk * 3.2F) * amp * 0.05F : 0.0F;
   }

   /** Legs in the air: 0 on the ground, up to 1 when jumping or falling (they spread: the front ones forward, the back ones back). */
   static float air(Mobs.Mob m) {
      return m.onGround || m.inWater ? 0.0F : Math.max(0.0F, Math.min(1.0F, Math.abs((float)m.vy) * 4.0F + 0.35F));
   }

   /**
    * The pose of a leg in the air, degrees about the side axis (positive swings a hanging leg backwards, the model faces +z): the front legs
    * (pz > 0) reach forward, the back legs back. Only for the four-legged mobs.
    */
   static float spread(Mobs.Mob m, float legZ) {
      if (!animal(m.type) && m.type != Mobs.CREEPER) {
         return 0.0F;
      }

      float a = air(m);
      return a == 0.0F ? 0.0F : a * 24.0F * (legZ > 0.0F ? -1.0F : 1.0F);
   }

   /** A baby's head is bigger in proportion (Minecraft's baby animals). */
   static float babyHead(Mobs.Mob m) {
      return m.babyAge > 0 ? 1.35F : 1.0F;
   }
}
