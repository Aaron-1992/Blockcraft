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
      models[Mobs.ENDERMAN] = enderman();
      models[Mobs.ENDER_DRAGON] = dragon();
      models[Mobs.END_CRYSTAL] = crystal();
   }

   /** Tall, black, long arms and legs, a jaw that drops open when it is angry, purple eyes. */
   private static Model enderman() {
      Model m = new Model();
      m.type = Mobs.ENDERMAN;
      int c = 0x1A1A1A;
      m.add(1, c, 8, 6, 8, -4.0F, 0.0F, -4.0F, 0.0F, 42.0F, 0.0F);
      m.add(14, c, 8, 2, 8, -4.0F, -2.0F, -4.0F, 0.0F, 42.0F, 0.0F);
      m.add(2, c, 8, 12, 4, -4.0F, -12.0F, -2.0F, 0.0F, 40.0F, 0.0F);
      Part arm = m.add(5, c, 2, 30, 2, -1.0F, -30.0F, -1.0F, 5.0F, 38.0F, 0.0F);
      m.addLike(arm, 6, -5.0F, 38.0F, 0.0F);
      Part leg = m.add(3, c, 2, 28, 2, -1.0F, -28.0F, -1.0F, 2.0F, 28.0F, 0.0F);
      m.addLike(leg, 4, -2.0F, 28.0F, 0.0F);
      return m;
   }

   /** Black and enormous: a long body, a neck of three segments, a horned head with a jaw, two wings of two parts, a tail of seven. */
   private static Model dragon() {
      Model m = new Model();
      m.type = Mobs.ENDER_DRAGON;
      m.tw = 256;
      m.th = 256;
      int dark = 0x26242C;
      int skin = 0x3C2C4C;
      m.add(2, dark, 16, 12, 28, -8.0F, -6.0F, -14.0F, 0.0F, 34.0F, 0.0F);
      for (int i = 0; i < 3; i++) {
         m.add(2, dark, 8, 8, 10, -4.0F, -4.0F, 0.0F, 0.0F, 35.0F + 1.5F * i, 12.0F + 9.0F * i);
      }

      m.add(1, dark, 10, 8, 14, -5.0F, -4.0F, 0.0F, 0.0F, 40.0F, 40.0F);
      m.add(14, dark, 8, 3, 12, -4.0F, -3.0F, 0.0F, 0.0F, 38.5F, 41.0F);
      Part horn = m.add(2, 0xC8C0B0, 2, 4, 2, -1.0F, 0.0F, -1.0F, 3.5F, 44.0F, 42.0F);
      m.addLike(horn, 2, -3.5F, 44.0F, 42.0F);
      for (int side = 0; side < 2; side++) {
         float s = side == 0 ? 1.0F : -1.0F;
         float x0 = side == 0 ? 0.0F : -28.0F;
         float x1 = side == 0 ? 0.0F : -32.0F;
         m.add(15, dark, 28, 4, 6, x0, -2.0F, -3.0F, 8.0F * s, 38.0F, 6.0F);
         m.add(15, skin, 28, 1, 20, x0, -1.0F, -17.0F, 8.0F * s, 38.0F, 6.0F);
         m.add(16, dark, 32, 3, 5, x1, -1.5F, -2.5F, 8.0F * s, 38.0F, 6.0F);
         m.add(16, skin, 32, 1, 18, x1, -1.0F, -15.5F, 8.0F * s, 38.0F, 6.0F);
      }

      for (int i = 0; i < 7; i++) {
         int w = Math.max(3, 8 - i);
         Part seg = m.add(17, dark, w, w, 9, -w / 2.0F, -w / 2.0F, -9.0F, 0.0F, 34.0F, -14.0F - 9.0F * i);
         seg.rx = i;
      }

      Part fore = m.add(2, dark, 5, 14, 5, -2.5F, -14.0F, -2.5F, 5.0F, 28.0F, 8.0F);
      m.addLike(fore, 2, -5.0F, 28.0F, 8.0F);
      Part hind = m.add(2, dark, 7, 18, 7, -3.5F, -18.0F, -3.5F, 5.0F, 28.0F, -8.0F);
      m.addLike(hind, 2, -5.0F, 28.0F, -8.0F);
      return m;
   }

   /** A cube of glass (only its edges are drawn) round a glowing pink core. */
   private static Model crystal() {
      Model m = new Model();
      m.type = Mobs.END_CRYSTAL;
      m.scale = 1.4F;
      m.add(18, 0xE08CF0, 12, 12, 12, -6.0F, -6.0F, -6.0F, 0.0F, 9.0F, 0.0F);
      m.add(19, 0xFF40D0, 6, 6, 6, -3.0F, -3.0F, -3.0F, 0.0F, 9.0F, 0.0F);
      return m;
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
         case Mobs.ENDERMAN:
            return new float[]{part.color, 0.85F + MobRender.hash(x, y, 61) * 0.3F};
         case Mobs.ENDER_DRAGON:
            return new float[]{part.color, 0.8F + MobRender.vnoise(x * 0.35F, y * 0.35F, 71) * 0.45F};
         case Mobs.END_CRYSTAL:
            if (part.kind == 19) {
               float glow = MobRender.vnoise(x * 0.6F, y * 0.6F, 17);
               return new float[]{glow > 0.55F ? 0xFFB0F0 : 0xE030C0, 1.15F};
            }

            return new float[]{part.color, 1.1F};
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
         case Mobs.ENDERMAN: {
            // the purple eyes at the bottom of the face, and a glowing inside to the mouth
            int[] f = MobRender.face(model.parts.get(0), 3);
            for (int px : new int[]{0, 1, 2, 5, 6, 7}) {
               MobRender.put(f[0] + px, f[1] + 4, 0xF070FF);
               MobRender.put(f[0] + px, f[1] + 5, 0xB030E0);
            }

            int[] top = MobRender.face(model.parts.get(1), 0);
            for (int yy = 0; yy < top[3]; yy++) {
               for (int xx = 0; xx < top[2]; xx++) {
                  MobRender.put(top[0] + xx, top[1] + yy, 0xC050E8);
               }
            }

            int[] under = MobRender.face(model.parts.get(0), 1);
            for (int yy = 0; yy < under[3]; yy++) {
               for (int xx = 0; xx < under[2]; xx++) {
                  MobRender.put(under[0] + xx, under[1] + yy, 0xA030C8);
               }
            }

            break;
         }
         case Mobs.ENDER_DRAGON: {
            // magenta eyes on the sides of the head, a pale underside to the jaw, nostrils
            Part head = model.parts.get(4);
            int[] f = MobRender.face(head, 3);
            MobRender.put(f[0] + 2, f[1] + 3, 0xF050FF);
            MobRender.put(f[0] + 3, f[1] + 3, 0xF050FF);
            MobRender.put(f[0] + 6, f[1] + 3, 0xF050FF);
            MobRender.put(f[0] + 7, f[1] + 3, 0xF050FF);
            MobRender.put(f[0] + 3, f[1] + 6, 0x777080);
            MobRender.put(f[0] + 6, f[1] + 6, 0x777080);
            for (int face : new int[]{2, 4}) {
               int[] s = MobRender.face(head, face);
               MobRender.put(s[0] + 4, s[1] + 3, 0xF050FF);
               MobRender.put(s[0] + 5, s[1] + 3, 0xF050FF);
               MobRender.put(s[0] + 4, s[1] + 4, 0xC030E0);
               MobRender.put(s[0] + 5, s[1] + 4, 0xC030E0);
            }

            int[] jaw = MobRender.face(model.parts.get(5), 0);
            for (int yy = 0; yy < jaw[3]; yy++) {
               for (int xx = 0; xx < jaw[2]; xx++) {
                  MobRender.put(jaw[0] + xx, jaw[1] + yy, (xx + yy) % 3 == 0 ? 0xD8D0C0 : 0x8A5A9A);
               }
            }

            break;
         }
         case Mobs.END_CRYSTAL: {
            // glass: only the frame of every face is there, the rest is see-through
            Part frame = model.parts.get(0);
            for (int face = 0; face < 6; face++) {
               int[] f = MobRender.face(frame, face);
               for (int yy = 1; yy < f[3] - 1; yy++) {
                  for (int xx = 1; xx < f[2] - 1; xx++) {
                     MobRender.clear(f[0] + xx, f[1] + yy);
                  }
               }
            }

            break;
         }
         default:
            break;
      }
   }
}
