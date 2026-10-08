package bc;

/**
 * Update 3.17: what a clip does to the bones of a {@link Rig} in one frame: an offset (model units), a rotation (degrees) and a scale per
 * bone, and bones that are not drawn. One Pose is reused for every mob that is drawn, so {@link #reset} has to be called first.
 */
final class Pose {
   final float[] ox;
   final float[] oy;
   final float[] oz;
   final float[] rx;
   final float[] ry;
   final float[] rz;
   final float[] sx;
   final float[] sy;
   final float[] sz;
   /** Hidden groups (see Rig.Cube.group): bit n hides group n. */
   int hide;
   /** The whole model: a lift (blocks), a rotation about the point (0, rootPivot, 0) (degrees) and a scale - for falling over, bouncing. */
   float rootY;
   float rootPivot;
   float rootPitch;
   float rootRoll;
   float rootYaw;
   float rootScaleX = 1.0F;
   float rootScaleY = 1.0F;
   float rootScaleZ = 1.0F;
   /** Added to the colour that the renderer picks (1 = unchanged) and a flash that is added to the picture (0 = none). */
   float tintR = 1.0F;
   float tintG = 1.0F;
   float tintB = 1.0F;

   Pose(int bones) {
      this.ox = new float[bones];
      this.oy = new float[bones];
      this.oz = new float[bones];
      this.rx = new float[bones];
      this.ry = new float[bones];
      this.rz = new float[bones];
      this.sx = new float[bones];
      this.sy = new float[bones];
      this.sz = new float[bones];
      this.reset();
   }

   void reset() {
      java.util.Arrays.fill(this.ox, 0.0F);
      java.util.Arrays.fill(this.oy, 0.0F);
      java.util.Arrays.fill(this.oz, 0.0F);
      java.util.Arrays.fill(this.rx, 0.0F);
      java.util.Arrays.fill(this.ry, 0.0F);
      java.util.Arrays.fill(this.rz, 0.0F);
      java.util.Arrays.fill(this.sx, 1.0F);
      java.util.Arrays.fill(this.sy, 1.0F);
      java.util.Arrays.fill(this.sz, 1.0F);
      this.hide = 0;
      this.rootY = 0.0F;
      this.rootPivot = 0.0F;
      this.rootPitch = 0.0F;
      this.rootRoll = 0.0F;
      this.rootYaw = 0.0F;
      this.rootScaleX = 1.0F;
      this.rootScaleY = 1.0F;
      this.rootScaleZ = 1.0F;
      this.tintR = 1.0F;
      this.tintG = 1.0F;
      this.tintB = 1.0F;
   }

   /** The matrix of the whole model (3x4, model units): lift, then the rotation about the pivot and the scale. */
   double[] root() {
      double pv = this.rootPivot * 16.0;
      double[] m = {1, 0, 0, 0, 0, 1, 0, this.rootY * 16.0 + pv, 0, 0, 1, 0};
      if (this.rootRoll != 0.0F) {
         m = Rig.mul(m, Rig.rotZ(this.rootRoll));
      }

      if (this.rootPitch != 0.0F) {
         m = Rig.mul(m, Rig.rotX(this.rootPitch));
      }

      if (this.rootYaw != 0.0F) {
         m = Rig.mul(m, Rig.rotY(this.rootYaw));
      }

      if (this.rootScaleX != 1.0F || this.rootScaleY != 1.0F || this.rootScaleZ != 1.0F) {
         m = Rig.mul(m, Rig.scaleM(this.rootScaleX, this.rootScaleY, this.rootScaleZ));
      }

      return Rig.mul(m, new double[]{1, 0, 0, 0, 0, 1, 0, -pv, 0, 0, 1, 0});
   }

   /** Rotation of one bone (a no-op for a bone that does not exist: index -1). */
   void rot(int b, float x, float y, float z) {
      if (b >= 0) {
         this.rx[b] += x;
         this.ry[b] += y;
         this.rz[b] += z;
      }
   }

   void move(int b, float x, float y, float z) {
      if (b >= 0) {
         this.ox[b] += x;
         this.oy[b] += y;
         this.oz[b] += z;
      }
   }

   void scale(int b, float x, float y, float z) {
      if (b >= 0) {
         this.sx[b] *= x;
         this.sy[b] *= y;
         this.sz[b] *= z;
      }
   }
}
