package bc;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Update 3.17: a skeleton of bones that carry cuboids - the models of the new mobs. Pure data and maths, no OpenGL (so the tests and the
 * software viewer {@code MobView} use exactly what the game draws).
 *
 * <ul>
 * <li>Everything is measured in model units, 1/16 of a block; the model stands on y = 0 and looks towards +z (like the old models).</li>
 * <li>A {@link Bone} has a pivot (given in absolute rest coordinates, stored relative to its parent) and a rest rotation; a pose then adds
 * an offset, a rotation and a scale per bone (see {@link Pose}). Rotations are applied like Minecraft does: x first, then y, then z.</li>
 * <li>A {@link Cube} is a box in the bone's own frame. Every cube gets a net of its own in the skin (the same layout as Minecraft's boxes:
 * top, bottom, right, front, left, back), packed automatically. The skin has {@code ts} texels per model unit (2 for the new mobs).</li>
 * <li>The skin is painted by a {@link Shader} that is asked for the colour of every texel together with the position of that texel on the
 * model in the rest pose - so a pattern (the patches of a cow, the stripes of a tiger) runs on over the edges of the boxes.</li>
 * </ul>
 */
final class Rig {
   /** Face numbers: top, bottom, right (+x), front (+z), left (-x), back (-z). */
   static final int TOP = 0;
   static final int BOTTOM = 1;
   static final int RIGHT = 2;
   static final int FRONT = 3;
   static final int LEFT = 4;
   static final int BACK = 5;
   /** Number of groups of boxes a pose can hide (bit n of {@code Pose.hide} hides group n; group 0 is always drawn). */
   static final int GROUPS = 16;
   /** What a tinted bone is multiplied with: see {@code RigDraw.tintColor}. */
   static final int TINT_WOOL = 1;
   static final int TINT_ROBE = 2;
   static final int TINT_FEATHER = 3;
   /** What the baked light does to a face (the old models used the same numbers). */
   static final float[] FACE_K = {1.08F, 0.70F, 0.86F, 0.94F, 0.86F, 0.94F};

   /** Gives the colour of a texel (0xRRGGBB or 0xFFRRGGBB; {@link RigPaint#CLEAR} = see-through), or 0 to leave it to the next layer. */
   interface Shader {
      int rgb(Ctx c);
   }

   /** What a shader knows about the texel it paints. */
   static final class Ctx {
      Rig rig;
      Bone bone;
      Cube cube;
      int face;
      /** The texel in the skin. */
      int tx;
      int ty;
      /** Position of the texel's centre on the model in the rest pose (model units), and the face's normal there. */
      float x;
      float y;
      float z;
      float nx;
      float ny;
      float nz;
      /** 0..1 across the face, from its top left corner as it is seen from outside. */
      float fu;
      float fv;
      /** Position in the face in model units from its top left corner (for hand placed details). */
      float uu;
      float vv;
      /** Distance to the nearest edge of the face in texels (0 = on the edge). */
      int edge;
   }

   static final class Cube {
      final Bone bone;
      /** From the pivot of the bone, in model units. */
      final float x;
      final float y;
      final float z;
      final float w;
      final float h;
      final float d;
      final float inflate;
      /** The net in the skin, in texels. */
      int u;
      int v;
      /** Size of the faces in texels. */
      final int wt;
      final int ht;
      final int dt;
      /** Part of a group that a pose can hide (sheared wool, a profession's hat ...); 0 = always there. */
      int group;
      /** What the box is ("belt", "hem" ...): a free name for the painting shader (Update 3.18), null = nothing in particular. */
      String role;
      private float[] quads;

      Cube(Bone bone, float x, float y, float z, float w, float h, float d, float inflate, int ts) {
         this.bone = bone;
         this.x = x;
         this.y = y;
         this.z = z;
         this.w = w;
         this.h = h;
         this.d = d;
         this.inflate = inflate;
         this.wt = Math.max(1, Math.round(w * ts));
         this.ht = Math.max(1, Math.round(h * ts));
         this.dt = Math.max(1, Math.round(d * ts));
      }

      /** The other boxes that were made together with this one (see {@link Bone#round}): they all move to the group. */
      java.util.List<Cube> mates;

      Cube group(int g) {
         this.group = g;
         if (this.mates != null) {
            for (Cube c : this.mates) {
               c.group = g;
            }
         }

         return this;
      }

      /** Names the box (and the boxes made together with it, see {@link Bone#round}) for the painting shader. */
      Cube role(String name) {
         this.role = name;
         if (this.mates != null) {
            for (Cube c : this.mates) {
               c.role = name;
            }
         }

         return this;
      }

      /** The rectangle of a face in the skin: {x, y, width, height} in texels. */
      int[] face(int f) {
         switch (f) {
            case TOP:
               return new int[]{this.u + this.dt, this.v, this.wt, this.dt};
            case BOTTOM:
               return new int[]{this.u + this.dt + this.wt, this.v, this.wt, this.dt};
            case RIGHT:
               return new int[]{this.u, this.v + this.dt, this.dt, this.ht};
            case FRONT:
               return new int[]{this.u + this.dt, this.v + this.dt, this.wt, this.ht};
            case LEFT:
               return new int[]{this.u + this.dt + this.wt, this.v + this.dt, this.dt, this.ht};
            default:
               return new int[]{this.u + 2 * this.dt + this.wt, this.v + this.dt, this.wt, this.ht};
         }
      }

      /** The corners of a face in the bone's frame, top left first, seen from outside: TL, TR, BR, BL (model units). */
      float[][] corners(int f) {
         float x0 = this.x - this.inflate;
         float y0 = this.y - this.inflate;
         float z0 = this.z - this.inflate;
         float x1 = this.x + this.w + this.inflate;
         float y1 = this.y + this.h + this.inflate;
         float z1 = this.z + this.d + this.inflate;
         switch (f) {
            case TOP:
               return new float[][]{{x0, y1, z0}, {x1, y1, z0}, {x1, y1, z1}, {x0, y1, z1}};
            case BOTTOM:
               return new float[][]{{x0, y0, z1}, {x1, y0, z1}, {x1, y0, z0}, {x0, y0, z0}};
            case RIGHT:
               return new float[][]{{x1, y1, z1}, {x1, y1, z0}, {x1, y0, z0}, {x1, y0, z1}};
            case FRONT:
               return new float[][]{{x0, y1, z1}, {x1, y1, z1}, {x1, y0, z1}, {x0, y0, z1}};
            case LEFT:
               return new float[][]{{x0, y1, z0}, {x0, y1, z1}, {x0, y0, z1}, {x0, y0, z0}};
            default:
               return new float[][]{{x1, y1, z0}, {x0, y1, z0}, {x0, y0, z0}, {x1, y0, z0}};
         }
      }

      static final float[][] NORMALS = {{0, 1, 0}, {0, -1, 0}, {1, 0, 0}, {0, 0, 1}, {-1, 0, 0}, {0, 0, -1}};

      /** 6 faces x 4 corners x (x, y, z in blocks, u, v) in the bone's frame - what goes into a display list. */
      float[] quads() {
         if (this.quads == null) {
            Rig r = this.bone.rig;
            float[] q = new float[6 * 4 * 5];
            int n = 0;
            for (int f = 0; f < 6; f++) {
               float[][] c = this.corners(f);
               int[] t = this.face(f);
               float u0 = t[0] / (float)r.tw;
               float v0 = t[1] / (float)r.th;
               float u1 = (t[0] + t[2]) / (float)r.tw;
               float v1 = (t[1] + t[3]) / (float)r.th;
               float[] us = {u0, u1, u1, u0};
               float[] vs = {v0, v0, v1, v1};
               for (int k = 0; k < 4; k++) {
                  q[n++] = c[k][0] / 16.0F;
                  q[n++] = c[k][1] / 16.0F;
                  q[n++] = c[k][2] / 16.0F;
                  q[n++] = us[k];
                  q[n++] = vs[k];
               }
            }

            this.quads = q;
         }

         return this.quads;
      }
   }

   static final class Bone {
      final Rig rig;
      final String name;
      final int index;
      final Bone parent;
      final ArrayList<Bone> children = new ArrayList<>();
      /** Pivot relative to the parent's pivot, model units. */
      float px;
      float py;
      float pz;
      /** Rest rotation in degrees. */
      float rx;
      float ry;
      float rz;
      final ArrayList<Cube> cubes = new ArrayList<>();
      /** Drawn at full brightness whatever the light is (eyes, crystals, embers). */
      boolean glow;
      /** The pivot in the rest pose, absolute. */
      float ax;
      float ay;
      float az;
      /** What a clip may want to know: a free tag. */
      int tag;
      /** The 3x4 matrix of the rest pose: from the bone's frame to the model's. */
      double[] rest;
      /** The display lists (one per group of boxes), set by RigDraw. */
      int[] lists;

      Bone(Rig rig, String name, Bone parent, int index) {
         this.rig = rig;
         this.name = name;
         this.parent = parent;
         this.index = index;
      }

      Bone rot(float x, float y, float z) {
         this.rx = x;
         this.ry = y;
         this.rz = z;
         return this;
      }

      Bone glow() {
         this.glow = true;
         return this;
      }

      Bone tag(int t) {
         this.tag = t;
         return this;
      }

      /** A box from (x, y, z) to (x + w, y + h, z + d), measured from the pivot of this bone. */
      Cube box(float x, float y, float z, float w, float h, float d) {
         return this.box(x, y, z, w, h, d, 0.0F);
      }

      Cube box(float x, float y, float z, float w, float h, float d, float inflate) {
         Cube c = new Cube(this, x, y, z, w, h, d, inflate, this.rig.ts);
         this.rig.pack(c);
         this.cubes.add(c);
         return c;
      }

      /**
       * A box with bevelled long edges: three boxes (the middle one full size, the top and bottom one narrower by {@code b} on each side), so
       * the cross section is an octagon and the model is not a plain brick. Returns the middle box.
       */
      Cube round(float x, float y, float z, float w, float h, float d, float b) {
         Cube a = this.box(x + b, y + h - b, z, w - 2 * b, b, d);
         Cube c = this.box(x + b, y, z, w - 2 * b, b, d);
         Cube m = this.box(x, y + b * 0.999F, z, w, h - 2 * b * 0.999F, d);
         java.util.List<Cube> set = java.util.List.of(a, c, m);
         a.mates = set;
         c.mates = set;
         m.mates = set;
         return m;
      }

      /**
       * Like {@link #round} but bevelled on all four long edges and the two ends: a rounded block. Returns the box whose front and back
       * faces are flush with the block's (for painting a face on it); the box that has the flat sides is {@link #blobSide}.
       */
      Cube blob(float x, float y, float z, float w, float h, float d, float b) {
         Cube a = this.box(x + b, y + h - b, z + b, w - 2 * b, b, d - 2 * b);
         Cube c = this.box(x + b, y, z + b, w - 2 * b, b, d - 2 * b);
         Cube front = this.box(x + b, y + b * 0.999F, z, w - 2 * b, h - 2 * b * 0.999F, d);
         this.blobSide = this.box(x, y + b * 0.999F, z + b, w, h - 2 * b * 0.999F, d - 2 * b);
         java.util.List<Cube> set = java.util.List.of(a, c, front, this.blobSide);
         a.mates = set;
         c.mates = set;
         front.mates = set;
         this.blobSide.mates = set;
         return front;
      }

      Cube blobSide;

      /** A box given in absolute model coordinates (only for bones whose parents have no rest rotation). */
      Cube boxAt(float x, float y, float z, float w, float h, float d) {
         return this.box(x - this.ax, y - this.ay, z - this.az, w, h, d, 0.0F);
      }

      Cube boxAt(float x, float y, float z, float w, float h, float d, float inflate) {
         return this.box(x - this.ax, y - this.ay, z - this.az, w, h, d, inflate);
      }
   }

   final int type;
   final int ts;
   int tw;
   int th;
   float scale = 1.0F;
   /** The skin, ARGB; null until {@link #finish}. */
   int[] pix;
   final ArrayList<Bone> bones = new ArrayList<>();
   private final HashMap<String, Bone> byName = new HashMap<>();
   private final ArrayList<Cube> cubes = new ArrayList<>();
   private int packX;
   private int packY;
   private int packRow;
   /** GL texture, 0 until the renderer uploads it. */
   int tex;
   private Pose pose;
   /** For every group of boxes: 0 = the skin as painted, else multiplied with a colour the renderer picks (fleece, robe, feathers ...). */
   final int[] groupTint = new int[GROUPS];

   Rig(int type, int ts, int texW) {
      this.type = type;
      this.ts = ts;
      this.tw = texW;
      this.th = 0;
      Bone root = new Bone(this, "root", null, 0);
      this.bones.add(root);
      this.byName.put("root", root);
   }

   Bone root() {
      return this.bones.get(0);
   }

   /** A pose to fill and draw with (one per rig, reused: whoever draws fills it, draws, and is done with it). */
   Pose pose() {
      if (this.pose == null) {
         this.pose = new Pose(this.bones.size());
      }

      return this.pose;
   }

   /** A bone whose pivot is at the absolute rest position (x, y, z). Its parent must have no rest rotation. */
   Bone bone(String name, Bone parent, float x, float y, float z) {
      Bone b = new Bone(this, name, parent, this.bones.size());
      b.ax = x;
      b.ay = y;
      b.az = z;
      b.px = x - parent.ax;
      b.py = y - parent.ay;
      b.pz = z - parent.az;
      parent.children.add(b);
      this.bones.add(b);
      if (this.byName.put(name, b) != null) {
         throw new IllegalArgumentException("two bones called " + name);
      }

      return b;
   }

   /** A bone whose pivot is given relative to the pivot of the parent (the parent may be rotated). */
   Bone boneRel(String name, Bone parent, float x, float y, float z) {
      Bone b = this.bone(name, parent, parent.ax + x, parent.ay + y, parent.az + z);
      return b;
   }

   Bone find(String name) {
      return this.byName.get(name);
   }

   /** Test hook: when this is not null, every name a clip asks for that the rig does not have is added as "type:name" (typos in a clip are otherwise silent). */
   static java.util.Set<String> missingNames;

   /** The index of a bone, or -1. */
   int idx(String name) {
      Bone b = this.byName.get(name);
      if (b == null && missingNames != null) {
         synchronized (missingNames) {
            missingNames.add(this.type + ":" + name);
         }
      }

      return b == null ? -1 : b.index;
   }

   int size() {
      return this.bones.size();
   }

   /** The box round the model in the rest pose (model units): {minX, minY, minZ, maxX, maxY, maxZ}. */
   float[] bounds() {
      double[] b = {1e9, 1e9, 1e9, -1e9, -1e9, -1e9};
      double[] p = new double[3];
      for (Cube c : this.cubes) {
         double[] m = c.bone.rest;
         if (m == null) {
            continue;
         }

         for (int f = 0; f < 6; f++) {
            for (float[] k : c.corners(f)) {
               apply(m, k[0], k[1], k[2], p);
               for (int i = 0; i < 3; i++) {
                  b[i] = Math.min(b[i], p[i]);
                  b[3 + i] = Math.max(b[3 + i], p[i]);
               }
            }
         }
      }

      return new float[]{(float)b[0], (float)b[1], (float)b[2], (float)b[3], (float)b[4], (float)b[5]};
   }

   /** How high the model is in the rest pose (model units): the top of the highest box. */
   float height() {
      return Math.max(0.0F, this.bounds()[4]);
   }

   ArrayList<Cube> cubes() {
      return this.cubes;
   }

   /** Puts the net of a cube in the next free place of the skin (rows from left to right). */
   private void pack(Cube c) {
      int nw = 2 * (c.dt + c.wt);
      int nh = c.dt + c.ht;
      if (this.packX + nw > this.tw) {
         this.packX = 0;
         this.packY += this.packRow;
         this.packRow = 0;
      }

      c.u = this.packX;
      c.v = this.packY;
      this.packX += nw;
      this.packRow = Math.max(this.packRow, nh);
      this.th = Math.max(this.th, this.packY + this.packRow);
      this.cubes.add(c);
   }

   /** Call when the bones and boxes are all there: sizes the skin, computes the rest matrices. */
   Rig finish() {
      this.th = (this.th + 7) / 8 * 8;
      this.pix = new int[this.tw * this.th];
      for (Bone b : this.bones) {
         b.rest = matrix(b.parent == null ? null : b.parent.rest, b.px, b.py, b.pz, b.rx, b.ry, b.rz);
      }

      for (Cube c : this.cubes) {
         c.quads = null;
      }

      return this;
   }

   // ------------------------------------------------------------------ maths
   /** parent * translate(px, py, pz) * Rz * Ry * Rx, as a row-major 3x4. */
   static double[] matrix(double[] parent, double px, double py, double pz, double rx, double ry, double rz) {
      double[] m = {1, 0, 0, px, 0, 1, 0, py, 0, 0, 1, pz};
      if (rz != 0.0) {
         m = mul(m, rotZ(rz));
      }

      if (ry != 0.0) {
         m = mul(m, rotY(ry));
      }

      if (rx != 0.0) {
         m = mul(m, rotX(rx));
      }

      return parent == null ? m : mul(parent, m);
   }

   static double[] rotX(double deg) {
      double a = Math.toRadians(deg);
      double c = Math.cos(a);
      double s = Math.sin(a);
      return new double[]{1, 0, 0, 0, 0, c, -s, 0, 0, s, c, 0};
   }

   static double[] rotY(double deg) {
      double a = Math.toRadians(deg);
      double c = Math.cos(a);
      double s = Math.sin(a);
      return new double[]{c, 0, s, 0, 0, 1, 0, 0, -s, 0, c, 0};
   }

   static double[] rotZ(double deg) {
      double a = Math.toRadians(deg);
      double c = Math.cos(a);
      double s = Math.sin(a);
      return new double[]{c, -s, 0, 0, s, c, 0, 0, 0, 0, 1, 0};
   }

   static double[] scaleM(double sx, double sy, double sz) {
      return new double[]{sx, 0, 0, 0, 0, sy, 0, 0, 0, 0, sz, 0};
   }

   /** a * b (both 3x4, the last row 0 0 0 1 left out). */
   static double[] mul(double[] a, double[] b) {
      double[] r = new double[12];
      for (int i = 0; i < 3; i++) {
         for (int j = 0; j < 3; j++) {
            r[i * 4 + j] = a[i * 4] * b[j] + a[i * 4 + 1] * b[4 + j] + a[i * 4 + 2] * b[8 + j];
         }

         r[i * 4 + 3] = a[i * 4] * b[3] + a[i * 4 + 1] * b[7] + a[i * 4 + 2] * b[11] + a[i * 4 + 3];
      }

      return r;
   }

   static void apply(double[] m, double x, double y, double z, double[] out) {
      out[0] = m[0] * x + m[1] * y + m[2] * z + m[3];
      out[1] = m[4] * x + m[5] * y + m[6] * z + m[7];
      out[2] = m[8] * x + m[9] * y + m[10] * z + m[11];
   }

   /** The matrices of all bones for a pose (model units; the scale of a pose is part of them): from the bone's frame to the model's. */
   double[][] world(Pose p) {
      double[][] w = new double[this.bones.size()][];
      for (Bone b : this.bones) {
         int i = b.index;
         double[] m = {1, 0, 0, b.px + p.ox[i], 0, 1, 0, b.py + p.oy[i], 0, 0, 1, b.pz + p.oz[i]};
         double rz = b.rz + p.rz[i];
         double ry = b.ry + p.ry[i];
         double rx = b.rx + p.rx[i];
         if (rz != 0.0) {
            m = mul(m, rotZ(rz));
         }

         if (ry != 0.0) {
            m = mul(m, rotY(ry));
         }

         if (rx != 0.0) {
            m = mul(m, rotX(rx));
         }

         if (p.sx[i] != 1.0F || p.sy[i] != 1.0F || p.sz[i] != 1.0F) {
            m = mul(m, scaleM(p.sx[i], p.sy[i], p.sz[i]));
         }

         w[i] = b.parent == null ? m : mul(w[b.parent.index], m);
      }

      return w;
   }

   // ------------------------------------------------------------------ the skin
   /** Paints the whole skin with a shader, then bakes the light of the faces (top bright, bottom dark) and darkens the rims of the faces a little. */
   Rig paint(Shader shader, float rim) {
      Ctx c = new Ctx();
      c.rig = this;
      double[] pt = new double[3];
      for (Cube cube : this.cubes) {
         Bone b = cube.bone;
         c.bone = b;
         c.cube = cube;
         double[] m = b.rest;
         for (int f = 0; f < 6; f++) {
            int[] t = cube.face(f);
            float[][] k = cube.corners(f);
            float[] n = Cube.NORMALS[f];
            c.face = f;
            double nxr = m[0] * n[0] + m[1] * n[1] + m[2] * n[2];
            double nyr = m[4] * n[0] + m[5] * n[1] + m[6] * n[2];
            double nzr = m[8] * n[0] + m[9] * n[1] + m[10] * n[2];
            c.nx = (float)nxr;
            c.ny = (float)nyr;
            c.nz = (float)nzr;
            float faceW = f == TOP || f == BOTTOM || f == FRONT || f == BACK ? cube.w : cube.d;
            float faceH = f == TOP || f == BOTTOM ? cube.d : cube.h;
            for (int j = 0; j < t[3]; j++) {
               for (int i = 0; i < t[2]; i++) {
                  float a = (i + 0.5F) / t[2];
                  float v = (j + 0.5F) / t[3];
                  float lx = k[0][0] + (k[1][0] - k[0][0]) * a + (k[3][0] - k[0][0]) * v;
                  float ly = k[0][1] + (k[1][1] - k[0][1]) * a + (k[3][1] - k[0][1]) * v;
                  float lz = k[0][2] + (k[1][2] - k[0][2]) * a + (k[3][2] - k[0][2]) * v;
                  apply(m, lx, ly, lz, pt);
                  c.x = (float)pt[0];
                  c.y = (float)pt[1];
                  c.z = (float)pt[2];
                  c.fu = a;
                  c.fv = v;
                  c.uu = a * faceW;
                  c.vv = v * faceH;
                  c.tx = t[0] + i;
                  c.ty = t[1] + j;
                  c.edge = Math.min(Math.min(i, t[2] - 1 - i), Math.min(j, t[3] - 1 - j));
                  int rgb = shader.rgb(c);
                  if (rgb == 0) {
                     continue;
                  }

                  if (rgb >>> 24 == 0) {
                     rgb |= 0xFF000000;
                  }

                  if (rgb >>> 24 >= 128) {
                     float shade = FACE_K[f] * (c.edge == 0 ? 1.0F - rim : 1.0F);
                     rgb = RigPaint.mul(rgb, shade);
                  }

                  this.pix[c.ty * this.tw + c.tx] = rgb;
               }
            }
         }
      }

      return this;
   }

   /** Overwrites one texel (a hand painted detail); (i, j) counts in texels from the top left corner of the face. */
   void put(Cube cube, int face, int i, int j, int argb) {
      int[] t = cube.face(face);
      if (i >= 0 && j >= 0 && i < t[2] && j < t[3]) {
         this.pix[(t[1] + j) * this.tw + t[0] + i] = argb;
      }
   }

   /** A rectangle of texels on a face, in model units from the top left corner of the face as it is seen from outside. */
   void patch(Cube cube, int face, float u, float v, float w, float h, int argb) {
      int[] t = cube.face(face);
      float faceW = face == TOP || face == BOTTOM || face == FRONT || face == BACK ? cube.w : cube.d;
      float faceH = face == TOP || face == BOTTOM ? cube.d : cube.h;
      float sx = t[2] / faceW;
      float sy = t[3] / faceH;
      int x0 = Math.round(u * sx);
      int y0 = Math.round(v * sy);
      int x1 = Math.round((u + w) * sx);
      int y1 = Math.round((v + h) * sy);
      for (int j = y0; j < y1; j++) {
         for (int i = x0; i < x1; i++) {
            this.put(cube, face, i, j, argb);
         }
      }
   }
}
