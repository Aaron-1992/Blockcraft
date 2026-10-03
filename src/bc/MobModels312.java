package bc;

import bc.MobRender.Model;
import bc.MobRender.Part;

/**
 * Update 3.12: the models of the new mobs (boxes with painted textures, like the old ones in {@link MobRender}) and the details that
 * are painted on their faces. Sizes are in 1/16 blocks; {@code Model.scale} blows a model up (the ghast is a 16 unit cube drawn 4 times as big).
 */
final class MobModels312 {
   private MobModels312() {
   }

   static void build(Model[] models) {
      models[Mobs.ZOMBIE_PIGMAN] = pigman();
      models[Mobs.GHAST] = ghast();
      models[Mobs.BLAZE] = blaze();
      models[Mobs.MAGMA_CUBE] = magma();
   }

   /** A zombie's build, with pink skin and brown rags. */
   private static Model pigman() {
      Model m = new Model();
      m.type = Mobs.ZOMBIE_PIGMAN;
      int skin = 0xDB8E8E;
      m.add(1, skin, 8, 8, 8, -4.0F, 0.0F, -4.0F, 0.0F, 24.0F, 0.0F);
      m.add(2, 0x8E5B3E, 8, 12, 4, -4.0F, -12.0F, -2.0F, 0.0F, 24.0F, 0.0F);
      Part arm = m.add(5, skin, 4, 12, 4, -2.0F, -10.0F, -2.0F, 6.0F, 22.0F, 0.0F);
      m.addLike(arm, 6, -6.0F, 22.0F, 0.0F);
      Part leg = m.add(3, 0x6B4B33, 4, 12, 4, -2.0F, -12.0F, -2.0F, 2.0F, 12.0F, 0.0F);
      m.addLike(leg, 4, -2.0F, 12.0F, 0.0F);
      return m;
   }

   /** A white cube, four blocks wide, with a sad face and nine tentacles. */
   private static Model ghast() {
      Model m = new Model();
      m.type = Mobs.GHAST;
      m.scale = 4.0F;
      m.add(2, 0xF4F4F4, 16, 16, 16, -8.0F, -8.0F, -8.0F, 0.0F, 8.0F, 0.0F);
      int[] len = new int[]{11, 7, 12, 9, 13, 8, 10, 12, 8};
      for (int i = 0; i < 9; i++) {
         float x = (i % 3 - 1) * 5.0F;
         float z = (i / 3 - 1) * 5.0F;
         Part t = m.add(13, 0xE8E8E8, 2, len[i], 2, -1.0F, -len[i], -1.0F, x, 0.5F, z);
         t.ry = i * 40.0F;
      }

      return m;
   }

   /** A head floating above three rings of rods that circle round it. */
   private static Model blaze() {
      Model m = new Model();
      m.type = Mobs.BLAZE;
      m.add(1, 0xF0B828, 8, 8, 8, -4.0F, -4.0F, -4.0F, 0.0F, 24.0F, 0.0F);
      float[] radius = new float[]{9.0F, 7.0F, 5.0F};
      float[] height = new float[]{14.0F, 8.0F, 2.0F};
      for (int ring = 0; ring < 3; ring++) {
         for (int k = 0; k < 4; k++) {
            double a = k * Math.PI / 2.0 + ring * 0.5;
            Part rod = m.add(12, 0xF0A818, 2, 8, 2, -1.0F, 0.0F, -1.0F, (float)(Math.cos(a) * radius[ring]), height[ring], (float)(Math.sin(a) * radius[ring]));
            rod.ry = ring * 20.0F;
         }
      }

      return m;
   }

   /** A cube of cooling lava: dark rock with glowing cracks and two angry eyes. */
   private static Model magma() {
      Model m = new Model();
      m.type = Mobs.MAGMA_CUBE;
      m.add(2, 0x4A1E10, 8, 8, 8, -4.0F, 0.0F, -4.0F, 0.0F, 0.0F, 0.0F);
      return m;
   }

   /** The colour of one pixel of a part (null: leave it to the standard shading) as a {colour, light factor} pair. */
   static float[] pixel(Model model, Part part, int x, int y, int faceIdx) {
      switch (model.type) {
         case Mobs.GHAST:
            return new float[]{part.color, 0.93F + MobRender.hash(x, y, 91) * 0.1F};
         case Mobs.BLAZE:
            return new float[]{part.color, part.kind == 12 ? 0.8F + (y % 3 == 0 ? 0.35F : 0.0F) : 0.92F + MobRender.hash(x, y, 33) * 0.14F};
         case Mobs.MAGMA_CUBE:
            float crack = MobRender.vnoise(x * 0.5F, y * 0.5F, 21);
            int c = crack > 0.66F ? 0xF0A010 : (crack > 0.52F ? 0xC8480C : 0x4A1E10);
            return new float[]{c, 0.9F + MobRender.hash(x, y, 5) * 0.2F};
         default:
            return null;
      }
   }

   /** The faces: eyes, mouths. */
   static void details(Model model) {
      switch (model.type) {
         case Mobs.ZOMBIE_PIGMAN: {
            int[] f = MobRender.face(model.parts.get(0), 3);
            for (int px : new int[]{1, 2, 5, 6}) {
               MobRender.put(f[0] + px, f[1] + 3, 0x3A1E10);
            }

            MobRender.put(f[0] + 3, f[1] + 4, 0x9C5E5E);
            MobRender.put(f[0] + 4, f[1] + 4, 0x9C5E5E);
            for (int px = 2; px < 6; px++) {
               MobRender.put(f[0] + px, f[1] + 6, 0x6A3030);
            }

            break;
         }
         case Mobs.GHAST: {
            int[] f = MobRender.face(model.parts.get(0), 3);
            // two shut eyes (long dark slits) and a small mouth
            for (int px = 2; px < 6; px++) {
               MobRender.put(f[0] + px, f[1] + 7, 0x3C3C3C);
               MobRender.put(f[0] + 8 + px, f[1] + 7, 0x3C3C3C);
            }

            MobRender.put(f[0] + 2, f[1] + 8, 0x777777);
            MobRender.put(f[0] + 11, f[1] + 8, 0x777777);
            MobRender.put(f[0] + 7, f[1] + 11, 0x3C3C3C);
            MobRender.put(f[0] + 8, f[1] + 11, 0x3C3C3C);
            MobRender.put(f[0] + 7, f[1] + 12, 0x3C3C3C);
            MobRender.put(f[0] + 8, f[1] + 12, 0x3C3C3C);
            break;
         }
         case Mobs.BLAZE: {
            int[] f = MobRender.face(model.parts.get(0), 3);
            for (int px : new int[]{1, 2, 5, 6}) {
               MobRender.put(f[0] + px, f[1] + 3, 0x4A2A08);
            }

            MobRender.put(f[0] + 3, f[1] + 5, 0x6A3A08);
            MobRender.put(f[0] + 4, f[1] + 5, 0x6A3A08);
            MobRender.put(f[0] + 3, f[1] + 6, 0x6A3A08);
            MobRender.put(f[0] + 4, f[1] + 6, 0x6A3A08);
            break;
         }
         case Mobs.MAGMA_CUBE: {
            int[] f = MobRender.face(model.parts.get(0), 3);
            for (int px : new int[]{1, 2, 5, 6}) {
               MobRender.put(f[0] + px, f[1] + 3, 0xFFC020);
               MobRender.put(f[0] + px, f[1] + 4, 0xE06010);
            }

            for (int px = 2; px < 6; px++) {
               MobRender.put(f[0] + px, f[1] + 6, 0x240C08);
            }

            break;
         }
         default:
            break;
      }
   }
}
