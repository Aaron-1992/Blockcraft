package bc;

import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import javax.imageio.ImageIO;

/**
 * Development tool: a {@link Gui} that paints into a picture instead of OpenGL, so that the menu screens (title, options, inventory, ...) can be looked at
 * without a window: every screen is drawn by the real code of the game, only the pixels are made here. Text uses the real font, block and item icons the
 * real atlas (Tex.initPixels). Rotation is ignored (the splash text of the title is drawn straight).
 */
public class GuiRaster extends Gui {
   public final int W;
   public final int H;
   public final int[] px;
   final ArrayList<float[]> stack = new ArrayList<>();
   float tx;
   float ty;
   float sc = 1.0F;
   float[] clip;
   static byte[] fontAlpha;
   static int fontPxW;
   static int fontCols;
   static int fontRows;
   static boolean atlasReady;
   final float[] qx = new float[4];
   final float[] qy = new float[4];
   final float[] qu = new float[4];
   final float[] qv = new float[4];
   float qr;
   float qg;
   float qb;
   float qa;
   int qn;

   public GuiRaster(int w, int h) {
      this.W = w;
      this.H = h;
      this.px = new int[w * h];
      loadResources();
   }

   static synchronized void loadResources() {
      if (!atlasReady) {
         Tex.initPixels();
         atlasReady = true;
      }

      if (fontAlpha == null) {
         try (DataInputStream in = new DataInputStream(new FileInputStream(new File("res/font.bin")))) {
            fontCols = in.readUnsignedByte();
            fontRows = in.readUnsignedByte();
            Tex.fontCellW = in.readUnsignedByte();
            Tex.fontCellH = in.readUnsignedByte();
            Tex.fontCols = fontCols;
            for (int i = 0; i < 96; i++) {
               Tex.fontW[i] = in.readUnsignedByte();
            }

            fontPxW = fontCols * Tex.fontCellW;
            fontAlpha = new byte[fontPxW * fontRows * Tex.fontCellH];
            in.readFully(fontAlpha);
         } catch (Exception e) {
            throw new RuntimeException(e);
         }
      }
   }

   public void fill(int argb) {
      java.util.Arrays.fill(this.px, argb);
   }

   @Override
   public void begin(int fbW, int fbH, int s) {
      this.scale = s;
      this.w = (float)fbW / s;
      this.h = (float)fbH / s;
      this.stack.clear();
      this.tx = 0;
      this.ty = 0;
      this.sc = 1.0F;
      this.clip = null;
   }

   // ---------------------------------------------------------------- pixels
   void blend(int x, int y, int argb) {
      if (x < 0 || y < 0 || x >= this.W || y >= this.H) {
         return;
      }

      if (this.clip != null && (x < this.clip[0] || y < this.clip[1] || x >= this.clip[2] || y >= this.clip[3])) {
         return;
      }

      int a = argb >>> 24;
      if (a == 0) {
         return;
      }

      if (a == 255) {
         this.px[y * this.W + x] = argb | 0xFF000000;
         return;
      }

      int d = this.px[y * this.W + x];
      int r = ((argb >> 16 & 255) * a + (d >> 16 & 255) * (255 - a)) / 255;
      int g = ((argb >> 8 & 255) * a + (d >> 8 & 255) * (255 - a)) / 255;
      int b = ((argb & 255) * a + (d & 255) * (255 - a)) / 255;
      this.px[y * this.W + x] = 0xFF000000 | r << 16 | g << 8 | b;
   }

   float dx(float x) {
      return (x * this.sc + this.tx) * this.scale;
   }

   float dy(float y) {
      return (y * this.sc + this.ty) * this.scale;
   }

   @Override
   public void rect(float x, float y, float w, float h, int argb) {
      int x0 = Math.round(this.dx(x));
      int y0 = Math.round(this.dy(y));
      int x1 = Math.round(this.dx(x + w));
      int y1 = Math.round(this.dy(y + h));
      for (int j = Math.max(0, y0); j < Math.min(this.H, y1); j++) {
         for (int i = Math.max(0, x0); i < Math.min(this.W, x1); i++) {
            this.blend(i, j, argb);
         }
      }
   }

   @Override
   public void gradient(float x, float y, float w, float h, int top, int bottom) {
      int x0 = Math.round(this.dx(x));
      int y0 = Math.round(this.dy(y));
      int x1 = Math.round(this.dx(x + w));
      int y1 = Math.round(this.dy(y + h));
      for (int j = Math.max(0, y0); j < Math.min(this.H, y1); j++) {
         float t = y1 - y0 <= 1 ? 0 : (j - y0) / (float)(y1 - y0 - 1);
         int c = 0;
         for (int sh = 0; sh <= 24; sh += 8) {
            int a = top >> sh & 255;
            int b = bottom >> sh & 255;
            c |= Math.max(0, Math.min(255, Math.round(a + (b - a) * t))) << sh;
         }

         for (int i = Math.max(0, x0); i < Math.min(this.W, x1); i++) {
            this.blend(i, j, c);
         }
      }
   }

   @Override
   public void tex(int id, float x, float y, float w, float h, float u0, float v0, float u1, float v1, int argb) {
      this.rect(x, y, w, h, (argb & 0xFF000000) | 0x303030);
   }

   @Override
   public void image(Gui.Image img, float x, float y, float w, float h) {
      int x0 = Math.round(this.dx(x));
      int y0 = Math.round(this.dy(y));
      int x1 = Math.round(this.dx(x + w));
      int y1 = Math.round(this.dy(y + h));
      for (int j = Math.max(0, y0); j < Math.min(this.H, y1); j++) {
         for (int i = Math.max(0, x0); i < Math.min(this.W, x1); i++) {
            int sx = Math.min(img.w - 1, (i - x0) * img.w / Math.max(1, x1 - x0));
            int sy = Math.min(img.h - 1, (j - y0) * img.h / Math.max(1, y1 - y0));
            this.blend(i, j, img.px[sy * img.w + sx]);
         }
      }
   }

   @Override
   public void dirtBackground(float f) {
      int sh = (int)(f * 255.0F);
      for (float y = 0; y < this.h; y += 32) {
         for (float x = 0; x < this.w; x += 32) {
            this.tile(1, x, y, 32, 32, sh, sh, sh);
         }
      }
   }

   @Override
   protected void atlasTile(int tile, float x, float y, float size, int rgb) {
      this.tile(tile, x, y, size, size, rgb >> 16 & 255, rgb >> 8 & 255, rgb & 255);
   }

   void tile(int tile, float x, float y, float w, float h, int r, int g, int b) {
      int x0 = Math.round(this.dx(x));
      int y0 = Math.round(this.dy(y));
      int x1 = Math.round(this.dx(x + w));
      int y1 = Math.round(this.dy(y + h));
      float u0 = Tex.tu(tile) * Tex.PX;
      float v0 = Tex.tv(tile) * Tex.PX;
      int ts = Math.round(Tex.TS * Tex.PX);
      for (int j = Math.max(0, y0); j < Math.min(this.H, y1); j++) {
         for (int i = Math.max(0, x0); i < Math.min(this.W, x1); i++) {
            int sx = (int)(u0 + (i - x0 + 0.5F) * ts / Math.max(1, x1 - x0));
            int sy = (int)(v0 + (j - y0 + 0.5F) * ts / Math.max(1, y1 - y0));
            int c = Tex.atlasPixels[sy * Tex.PX + sx];
            int a = c >>> 24;
            if (a < 26) {
               continue;
            }

            int cr = (c >> 16 & 255) * r / 255;
            int cg = (c >> 8 & 255) * g / 255;
            int cb = (c & 255) * b / 255;
            this.blend(i, j, a << 24 | cr << 16 | cg << 8 | cb);
         }
      }
   }

   // ---------------------------------------------------------------- block icons: the faces come in as four vertices each
   @Override
   protected void quadsBegin() {
      this.qn = 0;
   }

   @Override
   protected void quadsEnd() {
      this.qn = 0;
   }

   @Override
   protected void v(float x, float y, float du, float dv, float r, float g, float b, float a, float tu0, float tv0) {
      this.qx[this.qn] = this.dx(x);
      this.qy[this.qn] = this.dy(y);
      this.qu[this.qn] = (tu0 + du * Tex.TS) * Tex.PX;
      this.qv[this.qn] = (tv0 + dv * Tex.TS) * Tex.PX;
      this.qr = r;
      this.qg = g;
      this.qb = b;
      this.qa = a;
      if (++this.qn == 4) {
         this.qn = 0;
         this.drawQuad();
      }
   }

   /** The face is a parallelogram: P = v0 + u * (v1 - v0) + v * (v3 - v0). */
   void drawQuad() {
      float ax = this.qx[1] - this.qx[0];
      float ay = this.qy[1] - this.qy[0];
      float bx = this.qx[3] - this.qx[0];
      float by = this.qy[3] - this.qy[0];
      float det = ax * by - ay * bx;
      if (Math.abs(det) < 1e-4F) {
         return;
      }

      float minX = Math.min(Math.min(this.qx[0], this.qx[1]), Math.min(this.qx[2], this.qx[3]));
      float maxX = Math.max(Math.max(this.qx[0], this.qx[1]), Math.max(this.qx[2], this.qx[3]));
      float minY = Math.min(Math.min(this.qy[0], this.qy[1]), Math.min(this.qy[2], this.qy[3]));
      float maxY = Math.max(Math.max(this.qy[0], this.qy[1]), Math.max(this.qy[2], this.qy[3]));
      float du = this.qu[1] - this.qu[0];
      float dvu = this.qu[3] - this.qu[0];
      float duv = this.qv[1] - this.qv[0];
      float dvv = this.qv[3] - this.qv[0];
      for (int j = (int)Math.floor(minY); j <= (int)Math.ceil(maxY); j++) {
         for (int i = (int)Math.floor(minX); i <= (int)Math.ceil(maxX); i++) {
            float pxx = i + 0.5F - this.qx[0];
            float pyy = j + 0.5F - this.qy[0];
            float u = (pxx * by - pyy * bx) / det;
            float v = (ax * pyy - ay * pxx) / det;
            if (u < 0 || v < 0 || u >= 1 || v >= 1) {
               continue;
            }

            int sx = (int)(this.qu[0] + u * du + v * dvu);
            int sy = (int)(this.qv[0] + u * duv + v * dvv);
            int c = Tex.atlasPixels[Math.max(0, Math.min(Tex.PX - 1, sy)) * Tex.PX + Math.max(0, Math.min(Tex.PX - 1, sx))];
            int a = c >>> 24;
            if (a < 26) {
               continue;
            }

            int cr = Math.min(255, Math.round((c >> 16 & 255) * this.qr));
            int cg = Math.min(255, Math.round((c >> 8 & 255) * this.qg));
            int cb = Math.min(255, Math.round((c & 255) * this.qb));
            this.blend(i, j, a << 24 | cr << 16 | cg << 8 | cb);
         }
      }
   }

   // ---------------------------------------------------------------- text
   @Override
   protected void rawText(String s, float x, float y, int argb, float size) {
      int cw = Tex.fontCellW;
      int ch = Tex.fontCellH;
      int color = argb;
      float cx = x;
      for (int n = 0; n < s.length(); n++) {
         char c = s.charAt(n);
         if (c == 167 && n + 1 < s.length()) {
            int d = Character.digit(s.charAt(n + 1), 16);
            if (d >= 0) {
               color = argb & 0xFF000000 | CODES[d];
            }

            n++;
            continue;
         }

         int gi = c - ' ';
         if (gi < 0 || gi >= 96) {
            gi = 0;
         }

         int gx = gi % fontCols * cw;
         int gy = gi / fontCols * ch;
         for (int j = 0; j < ch; j++) {
            for (int i = 0; i < cw; i++) {
               int a = fontAlpha[(gy + j) * fontPxW + gx + i] & 255;
               if (a == 0) {
                  continue;
               }

               int x0 = Math.round(this.dx(cx + i * size));
               int y0 = Math.round(this.dy(y + j * size));
               int x1 = Math.round(this.dx(cx + (i + 1) * size));
               int y1 = Math.round(this.dy(y + (j + 1) * size));
               int ca = (color >>> 24) * a / 255;
               int cc = ca << 24 | color & 0xFFFFFF;
               for (int yy = y0; yy < y1; yy++) {
                  for (int xx = x0; xx < x1; xx++) {
                     this.blend(xx, yy, cc);
                  }
               }
            }
         }

         cx += Tex.fontW[gi] * size;
      }
   }

   // ---------------------------------------------------------------- transforms (rotation is ignored), clipping
   @Override
   public void push() {
      this.stack.add(new float[]{this.tx, this.ty, this.sc});
   }

   @Override
   public void pop() {
      float[] t = this.stack.remove(this.stack.size() - 1);
      this.tx = t[0];
      this.ty = t[1];
      this.sc = t[2];
   }

   @Override
   public void translate(float x, float y) {
      this.tx += x * this.sc;
      this.ty += y * this.sc;
   }

   @Override
   public void rotate(float degrees) {
   }

   @Override
   public void scaleBy(float s) {
      this.sc *= s;
   }

   @Override
   public void clip(float x, float y, float w, float h) {
      this.clip = new float[]{Math.round(this.dx(x)), Math.round(this.dy(y)), Math.round(this.dx(x + w)), Math.round(this.dy(y + h))};
   }

   @Override
   public void unclip() {
      this.clip = null;
   }

   // ---------------------------------------------------------------- output
   public void drawImage(BufferedImage img, int x, int y, int w, int h) {
      for (int j = 0; j < h; j++) {
         for (int i = 0; i < w; i++) {
            int sx = i * img.getWidth() / w;
            int sy = j * img.getHeight() / h;
            if (x + i >= 0 && y + j >= 0 && x + i < this.W && y + j < this.H) {
               this.px[(y + j) * this.W + x + i] = img.getRGB(sx, sy) | 0xFF000000;
            }
         }
      }
   }

   public void save(String path) throws Exception {
      BufferedImage img = new BufferedImage(this.W, this.H, BufferedImage.TYPE_INT_RGB);
      img.setRGB(0, 0, this.W, this.H, this.px, 0, this.W);
      new File(path).getAbsoluteFile().getParentFile().mkdirs();
      ImageIO.write(img, "png", new File(path));
   }
}
