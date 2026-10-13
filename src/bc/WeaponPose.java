package bc;

/** Shared, headless-testable weapon coordinates: +X muzzle, +Y sights/upper limb, +Z side. */
final class WeaponPose {
   static final float FIRST_PERSON_YAW = 96.0F; // +X -> -Z (away from the camera), slightly inward
   private WeaponPose() {}
   static float clamp(float f) { return Math.max(0, Math.min(1, f)); }

   static int cooldown(int id) {
      if (id == Redstone319.PISTOL) return 7;
      if (id == Redstone319.SNIPER_RIFLE) return 22;
      if (id == Redstone319.SIEGE_CANNON) return 20;
      if (id == Redstone319.MORTAR_STRIKE) return 24;
      return 3;
   }

   static float recoil(int id, int ticks, float partial) {
      if (ticks <= 0) return 0;
      float elapsed = Math.max(0, cooldown(id) - ticks + partial);
      float duration = Math.min(5, cooldown(id));
      return (float)Math.exp(-elapsed * 1.1F) * clamp(1 - elapsed / duration);
   }

   static float gripX(int id) {
      return id == Redstone319.SIEGE_CANNON || id == Redstone319.MORTAR_STRIKE ? -0.27F : -0.17F;
   }
   static float gripY(int id) { return -0.20F; }
   static float muzzleX(int id) {
      if (id == Redstone319.SNIPER_RIFLE) return 0.77F;
      if (id == Redstone319.MACHINE_GUN) return 0.60F;
      if (id == Redstone319.SIEGE_CANNON || id == Redstone319.MORTAR_STRIKE) return 0.59F;
      return 0.46F;
   }
   static float muzzleY(int id) {
      if (id == Redstone319.SNIPER_RIFLE || id == Redstone319.SIEGE_CANNON || id == Redstone319.MORTAR_STRIKE) return 0.08F;
      return id == Redstone319.MACHINE_GUN ? 0.055F : 0.10F;
   }

   static float bowDraw(int ticks, float partial) {
      return ticks <= 0 ? 0 : clamp((ticks + clamp(partial)) / 20.0F);
   }
   static float releaseDraw(float draw, int ticks, float partial) {
      float t = clamp((ticks - partial) / 4.0F);
      return clamp(draw) * t * t * t;
   }
}
