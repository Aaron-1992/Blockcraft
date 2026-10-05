package bc;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Update 3.18: a software view of the player's body ({@link PlayerModel#build} through a {@link PlayerModel.Sink} that keeps its own matrices),
 * so that the walking, swimming, climbing ... can be looked at without a GPU. It draws the skin the game starts with (or one given as a PNG).
 *
 * <pre>
 *   java bc.PlayerView out.png walk [tile] [azimuth] [skin.png]      one stride in eight frames (also: run sneak idle air swim tread climb fly
 *                                                                    swing eat bow land hurt hold)
 * </pre>
 */
public final class PlayerView implements PlayerModel.Sink {
   private final MobView v;
   private final int[] skin;
   private double[] m = {1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0};
   private final java.util.ArrayDeque<double[]> stack = new java.util.ArrayDeque<>();
   private final double[] p = new double[3];
   private final double[][] sc = new double[4][3];
   private final float[][] vt = new float[4][5];

   PlayerView(MobView v, int[] skin) {
      this.v = v;
      this.skin = skin;
   }

   void reset(double atX, double atZ) {
      this.m = new double[]{1, 0, 0, atX, 0, 1, 0, 0, 0, 0, 1, atZ};
      this.stack.clear();
   }

   @Override
   public void push() {
      this.stack.push(this.m.clone());
   }

   @Override
   public void pop() {
      this.m = this.stack.pop();
   }

   @Override
   public void translate(float x, float y, float z) {
      this.m = Rig.mul(this.m, new double[]{1, 0, 0, x, 0, 1, 0, y, 0, 0, 1, z});
   }

   @Override
   public void rotate(float deg, float x, float y, float z) {
      if (deg == 0.0F) {
         return;
      }

      double[] r = x != 0 ? Rig.rotX(deg * Math.signum(x)) : y != 0 ? Rig.rotY(deg * Math.signum(y)) : Rig.rotZ(deg * Math.signum(z));
      this.m = Rig.mul(this.m, r);
   }

   @Override
   public void scale(float x, float y, float z) {
      this.m = Rig.mul(this.m, Rig.scaleM(x, y, z));
   }

   @Override
   public void box(float x0, float y0, float z0, float x1, float y1, float z1, float[] r, float bright) {
      float k = 1.0F / Skins.SIZE;
      for (int f = 0; f < 6; f++) {
         float s = bright * PlayerModel.SHADE[f];
         this.v.mulR = this.v.mulG = this.v.mulB = s;
         float u0 = r[f * 4] * k;
         float v0 = r[f * 4 + 1] * k;
         float u1 = r[f * 4 + 2] * k;
         float v1 = r[f * 4 + 3] * k;
         float[][] c; // the corners in the order of PlayerModel.box
         switch (f) {
            case 0:
               c = new float[][]{{x0, y1, z0, u0, v0}, {x1, y1, z0, u1, v0}, {x1, y1, z1, u1, v1}, {x0, y1, z1, u0, v1}};
               break;
            case 1:
               c = new float[][]{{x0, y0, z1, u0, v0}, {x1, y0, z1, u1, v0}, {x1, y0, z0, u1, v1}, {x0, y0, z0, u0, v1}};
               break;
            case 2:
               c = new float[][]{{x0, y0, z0, u0, v1}, {x0, y0, z1, u1, v1}, {x0, y1, z1, u1, v0}, {x0, y1, z0, u0, v0}};
               break;
            case 3:
               c = new float[][]{{x0, y0, z1, u0, v1}, {x1, y0, z1, u1, v1}, {x1, y1, z1, u1, v0}, {x0, y1, z1, u0, v0}};
               break;
            case 4:
               c = new float[][]{{x1, y0, z1, u0, v1}, {x1, y0, z0, u1, v1}, {x1, y1, z0, u1, v0}, {x1, y1, z1, u0, v0}};
               break;
            default:
               c = new float[][]{{x1, y0, z0, u0, v1}, {x0, y0, z0, u1, v1}, {x0, y1, z0, u1, v0}, {x1, y1, z0, u0, v0}};
               break;
         }

         for (int i = 0; i < 4; i++) {
            Rig.apply(this.m, c[i][0], c[i][1], c[i][2], this.p);
            this.v.project(this.p[0], this.p[1], this.p[2], this.sc[i]);
            this.vt[i][3] = c[i][3];
            this.vt[i][4] = c[i][4];
         }

         this.v.tri(this.sc[0], this.vt[0], this.sc[1], this.vt[1], this.sc[2], this.vt[2], this.skin, Skins.SIZE, Skins.SIZE);
         this.v.tri(this.sc[0], this.vt[0], this.sc[2], this.vt[2], this.sc[3], this.vt[3], this.skin, Skins.SIZE, Skins.SIZE);
      }
   }

   /** A stand-in for the item in the hand: a stick along the way a tool lies in the hand (the real item model needs OpenGL). */
   @Override
   public void held(int id, boolean slim, float bright) {
      this.push();
      this.rotate(180.0F, 1, 0, 0);
      this.rotate(-90.0F, 1, 0, 0);
      this.rotate(180.0F, 0, 1, 0);
      this.translate(1.0F, 2.0F, -10.0F);
      this.translate(0.0F, 4.0F, 0.5F);
      this.rotate(-90.0F, 0, 1, 0);
      this.rotate(55.0F, 0, 0, 1);
      this.scale(0.85F * 16.0F, 0.85F * 16.0F, 0.85F * 16.0F);
      float[] stick = new float[24];
      for (int i = 0; i < 6; i++) {
         stick[i * 4] = 56;
         stick[i * 4 + 1] = 0;
         stick[i * 4 + 2] = 57;
         stick[i * 4 + 3] = 1;
      }

      // the blade of a sword, from the hand to the tip
      this.box(0.0F, 0.0F, -0.04F, 1.0F, 0.12F, 0.04F, stick, bright);
      this.pop();
   }

   static PlayerModel.Pose state(String name, int i, int n) {
      PlayerModel.Pose p = new PlayerModel.Pose();
      float k = i / (float)n;
      p.age = 100 + i * 3;
      switch (name) {
         case "walk":
            p.limb = k * (float)(Math.PI * 2 / 1.6);
            p.amp = 0.86F;
            break;
         case "run":
            p.limb = k * (float)(Math.PI * 2 / 1.6);
            p.amp = 1.0F;
            p.sprint = true;
            p.sprintK = 1.0F;
            break;
         case "sneak":
            p.limb = k * (float)(Math.PI * 2 / 1.6);
            p.amp = 0.26F;
            p.sneak = true;
            p.sneakK = 1.0F;
            break;
         case "idle":
            p.age = 20 + i * 24;
            break;
         case "hold":
            p.age = 20 + i * 24;
            p.held = 276;
            break;
         case "air":
            // a jump: up, the top, down, the landing
            p.airK = i < 6 ? 1.0F : 0.0F;
            p.vy = 0.42F - i * 0.12F;
            p.limb = 0.4F;
            p.amp = 0.8F;
            p.land = i == 6 ? 0.9F : i == 7 ? 0.5F : 0.0F;
            break;
         case "land":
            p.land = 1.0F - k;
            break;
         case "swim":
            p.swim = 1.0F;
            p.limb = k * (float)(Math.PI * 2 / (1.6 * 0.6));
            p.amp = 0.5F;
            break;
         case "tread":
            p.tread = 1.0F;
            p.age = 100 + i * 4;
            break;
         case "climb":
            p.climb = 1.0F;
            p.limb = k * (float)(Math.PI * 2 / 1.6);
            break;
         case "fly":
            p.fly = 1.0F;
            p.amp = 0.3F * Math.min(1.0F, i / 3.0F);
            p.age = 100 + i * 6;
            break;
         case "swing":
            p.swing = k;
            p.held = 276;
            break;
         case "eat":
            p.eat = 0.1F + k * 0.8F;
            p.held = 260;
            break;
         case "bow":
            p.bow = k * 1.4F;
            p.held = 268;
            break;
         case "hurt":
            p.hurt = 1.0F - k;
            break;
         default:
            break;
      }

      return p;
   }

   public static void main(String[] a) throws Exception {
      String out = a.length > 0 ? a[0] : "build/playerview.png";
      String what = a.length > 1 ? a[1] : "walk";
      int tile = a.length > 2 ? Integer.parseInt(a[2]) : 260;
      double az = a.length > 3 ? Double.parseDouble(a[3]) : 90;
      int[] skin = a.length > 4 ? Skins.fromImage(ImageIO.read(new File(a[4]))) : Skins.defaultPixels();
      int n = 8;
      BufferedImage sheet = new BufferedImage(tile * 4, tile * 2, BufferedImage.TYPE_INT_ARGB);
      for (int i = 0; i < n; i++) {
         MobView v = new MobView(tile, tile);
         v.clear(0xFFCFE4F5, 0xFFA9C8E4);
         v.camera(0.0, 0.88, 0.0, az, 8, 5.0, 30);
         v.groundY = 0.0;
         v.ground(7, 1.2);
         PlayerView pv = new PlayerView(v, skin);
         PlayerModel.Pose p = state(what, i, n);
         PlayerAnim.Joints j = PlayerAnim.solve(p);
         pv.reset(0, 0);
         PlayerModel.build(j, p, false, 1.0F, pv);
         MobView.blit(sheet, v, i % 4 * tile, i / 4 * tile);
      }

      ImageIO.write(sheet, "png", new File(out));
      System.out.println("wrote " + out);
   }
}
