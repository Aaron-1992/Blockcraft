package bc;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

public class Gui {
   public int scale = 2;
   public float w;
   public float h;
   static final int[] CODES = new int[]{
      0, 170, 43520, 43690, 11141120, 11141290, 16755200, 11184810, 5592405, 5592575, 5635925, 5636095, 16733525, 16733695, 16777045, 16777215
   };
   static final String[] HEART = new String[]{
      ".........", ".XX...XX.", "XXXX.XXXX", "XXXXXXXXX", "XXXXXXXXX", ".XXXXXXX.", "..XXXXX..", "...XXX...", "....X...."
   };
   static final String[] HUNGER = new String[]{
      ".....XXX.", "....XXXXX", "...XXXXXX", "..XXXXXX.", ".XXXXXX..", "XXXXXX...", "XXXXX....", "XXXX.....", "XX......."
   };
   static final String[] ARMOR_I = new String[]{
      ".XX...XX.", "XXXX.XXXX", "XXXXXXXXX", "XXXXXXXXX", "XXXXXXXXX", ".XXXXXXX.", ".XXXXXXX.", "..XXXXX..", "...XXX..."
   };

   public void begin(int var1, int var2, int var3) {
      this.scale = var3;
      this.w = (float)var1 / var3;
      this.h = (float)var2 / var3;
      GL11.glMatrixMode(5889);
      GL11.glLoadIdentity();
      GL11.glOrtho(0.0, this.w, this.h, 0.0, -1000.0, 1000.0);
      GL11.glMatrixMode(5888);
      GL11.glLoadIdentity();
      GL11.glDisable(2929);
      GL11.glDisable(2912);
      GL11.glDisable(3008);
      GL11.glDisable(2884);
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      GL13.glActiveTexture(33985);
      GL11.glDisable(3553);
      GL13.glActiveTexture(33984);
      GL11.glDisable(3553);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
   }

   static void color(int var0) {
      GL11.glColor4f((var0 >> 16 & 0xFF) / 255.0F, (var0 >> 8 & 0xFF) / 255.0F, (var0 & 0xFF) / 255.0F, (var0 >>> 24 & 0xFF) / 255.0F);
   }

   public void rect(float var1, float var2, float var3, float var4, int var5) {
      GL11.glDisable(3553);
      color(var5);
      GL11.glBegin(7);
      GL11.glVertex2f(var1, var2);
      GL11.glVertex2f(var1 + var3, var2);
      GL11.glVertex2f(var1 + var3, var2 + var4);
      GL11.glVertex2f(var1, var2 + var4);
      GL11.glEnd();
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
   }

   public void gradient(float var1, float var2, float var3, float var4, int var5, int var6) {
      GL11.glDisable(3553);
      GL11.glBegin(7);
      color(var5);
      GL11.glVertex2f(var1, var2);
      GL11.glVertex2f(var1 + var3, var2);
      color(var6);
      GL11.glVertex2f(var1 + var3, var2 + var4);
      GL11.glVertex2f(var1, var2 + var4);
      GL11.glEnd();
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
   }

   public void tex(int var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, int var10) {
      GL11.glEnable(3553);
      GL11.glBindTexture(3553, var1);
      color(var10);
      GL11.glBegin(7);
      GL11.glTexCoord2f(var6, var7);
      GL11.glVertex2f(var2, var3);
      GL11.glTexCoord2f(var8, var7);
      GL11.glVertex2f(var2 + var4, var3);
      GL11.glTexCoord2f(var8, var9);
      GL11.glVertex2f(var2 + var4, var3 + var5);
      GL11.glTexCoord2f(var6, var9);
      GL11.glVertex2f(var2, var3 + var5);
      GL11.glEnd();
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
   }

   public void dirtBackground(float var1) {
      int var2 = 0xFF000000 | (int)(var1 * 255.0F) << 16 | (int)(var1 * 255.0F) << 8 | (int)(var1 * 255.0F);
      float var3 = 32.0F;
      float var6 = Tex.tu(1);
      float var7 = Tex.tv(1);
      float var8 = var6 + Tex.TS;
      float var9 = var7 + Tex.TS;
      GL11.glEnable(3553);
      GL11.glBindTexture(3553, Tex.atlas);
      color(var2);
      GL11.glBegin(7);

      for (float var10 = 0.0F; var10 < this.h; var10 += var3) {
         for (float var11 = 0.0F; var11 < this.w; var11 += var3) {
            GL11.glTexCoord2f(var6, var7);
            GL11.glVertex2f(var11, var10);
            GL11.glTexCoord2f(var8, var7);
            GL11.glVertex2f(var11 + var3, var10);
            GL11.glTexCoord2f(var8, var9);
            GL11.glVertex2f(var11 + var3, var10 + var3);
            GL11.glTexCoord2f(var6, var9);
            GL11.glVertex2f(var11, var10 + var3);
         }
      }

      GL11.glEnd();
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
   }

   public static int textWidth(String var0) {
      int var1 = 0;

      for (int var2 = 0; var2 < var0.length(); var2++) {
         char var3 = var0.charAt(var2);
         if (var3 == 167) {
            var2++;
         } else {
            int var4 = var3 - ' ';
            if (var4 < 0 || var4 >= 96) {
               var4 = 0;
            }

            var1 += Tex.fontW[var4];
         }
      }

      return var1;
   }

   protected void rawText(String var1, float var2, float var3, int var4, float var5) {
      int var6 = Tex.fontCellW;
      int var7 = Tex.fontCellH;
      int var8 = Tex.fontCols;
      float var9 = var8 * var6;
      float var10 = 6 * var7;
      GL11.glEnable(3553);
      GL11.glBindTexture(3553, Tex.fontTex);
      int var11 = var4;
      GL11.glBegin(7);
      color(var11);
      float var12 = var2;

      for (int var13 = 0; var13 < var1.length(); var13++) {
         char var14 = var1.charAt(var13);
         if (var14 == 167 && var13 + 1 < var1.length()) {
            int var23 = Character.digit(var1.charAt(var13 + 1), 16);
            if (var23 >= 0) {
               var11 = var4 & 0xFF000000 | CODES[var23];
               color(var11);
            }

            var13++;
         } else {
            int var15 = var14 - ' ';
            if (var15 < 0 || var15 >= 96) {
               var15 = 0;
            }

            int var16 = var15 % var8 * var6;
            int var17 = var15 / var8 * var7;
            float var18 = var16 / var9;
            float var19 = var17 / var10;
            float var20 = (var16 + var6) / var9;
            float var21 = (var17 + var7) / var10;
            GL11.glTexCoord2f(var18, var19);
            GL11.glVertex2f(var12, var3);
            GL11.glTexCoord2f(var20, var19);
            GL11.glVertex2f(var12 + var6 * var5, var3);
            GL11.glTexCoord2f(var20, var21);
            GL11.glVertex2f(var12 + var6 * var5, var3 + var7 * var5);
            GL11.glTexCoord2f(var18, var21);
            GL11.glVertex2f(var12, var3 + var7 * var5);
            var12 += Tex.fontW[var15] * var5;
         }
      }

      GL11.glEnd();
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
   }

   public int text(String var1, float var2, float var3, int var4, boolean var5) {
      return this.text(var1, var2, var3, var4, var5, 1.0F);
   }

   public int text(String var1, float var2, float var3, int var4, boolean var5, float var6) {
      if (var4 >>> 24 == 0) {
         var4 |= -16777216;
      }

      if (var5) {
         String var7 = var1;
         int var8 = var4 >> 2 & 4144959 | var4 & 0xFF000000;
         this.rawText(stripCodes(var7), var2 + var6, var3 + var6, var8, var6);
      }

      this.rawText(var1, var2, var3, var4, var6);
      return (int)(textWidth(var1) * var6);
   }

   static String stripCodes(String var0) {
      StringBuilder var1 = new StringBuilder();

      for (int var2 = 0; var2 < var0.length(); var2++) {
         if (var0.charAt(var2) == 167) {
            var2++;
         } else {
            var1.append(var0.charAt(var2));
         }
      }

      return var1.toString();
   }

   public void centered(String var1, float var2, float var3, int var4, boolean var5, float var6) {
      this.text(var1, var2 - textWidth(var1) * var6 / 2.0F, var3, var4, var5, var6);
   }

   public void button(float var1, float var2, float var3, float var4, String var5, boolean var6, boolean var7) {
      int var8 = !var7 ? -11908534 : (var6 ? -8485187 : -9408400);
      this.rect(var1, var2, var3, var4, -16777216);
      this.rect(var1 + 1.0F, var2 + 1.0F, var3 - 2.0F, var4 - 2.0F, var8);
      int var9 = !var7 ? -10855846 : (var6 ? -5195536 : -6250336);
      int var10 = !var7 ? -12961222 : (var6 ? -11906416 : -12303292);
      this.rect(var1 + 1.0F, var2 + 1.0F, var3 - 2.0F, 1.0F, var9);
      this.rect(var1 + 1.0F, var2 + 1.0F, 1.0F, var4 - 2.0F, var9);
      this.rect(var1 + 1.0F, var2 + var4 - 2.0F, var3 - 2.0F, 1.0F, var10);
      this.rect(var1 + var3 - 2.0F, var2 + 1.0F, 1.0F, var4 - 2.0F, var10);
      int var11 = !var7 ? -6710887 : (var6 ? -96 : -1);
      this.centered(var5, var1 + var3 / 2.0F, var2 + (var4 - 8.0F) / 2.0F, var11, true, 1.0F);
   }

   /** Update 3.13: a track that fills up to the handle (so the setting can be read at a glance); the text sits on top. */
   public void slider(float var1, float var2, float var3, float var4, String var5, float var6, boolean var7) {
      this.rect(var1, var2, var3, var4, -16777216);
      this.rect(var1 + 1.0F, var2 + 1.0F, var3 - 2.0F, var4 - 2.0F, -13750738);
      float var8 = var1 + 1.0F + (var3 - 8.0F) * var6;
      float fill = var8 - (var1 + 1.0F);
      if (fill > 1.0F) {
         this.gradient(var1 + 1.0F, var2 + 1.0F, fill, var4 - 2.0F, var7 ? -10646691 : -11700672, var7 ? -12624064 : -13489606);
      }

      this.rect(var1 + 1.0F, var2 + 1.0F, var3 - 2.0F, 1.0F, 553648127);
      this.rect(var8 + 1.5F, var2 + 1.0F, 3.0F, var4 - 2.0F, -16777216);
      this.rect(var8 + 2.0F, var2 + 2.0F, 2.0F, var4 - 4.0F, var7 ? -5195536 : -1);
      this.centered(var5, var1 + var3 / 2.0F, var2 + (var4 - 8.0F) / 2.0F, -1, true, 1.0F);
   }

   /** A picture made of pixels that the program computes (the world preview): uploaded to the graphics card when it changes. */
   public static final class Image {
      public final int w;
      public final int h;
      public final int[] px;
      public boolean dirty = true;
      int tex;

      public Image(int w, int h) {
         this.w = w;
         this.h = h;
         this.px = new int[w * h];
      }
   }

   /** Draws the picture into the rectangle (nearest-neighbour: every sample stays a sharp little square). */
   public void image(Image img, float x, float y, float w, float h) {
      if (img.tex == 0) {
         img.tex = Tex.upload(img.w, img.h, img.px, false, false);
         img.dirty = false;
      } else if (img.dirty) {
         GL11.glBindTexture(3553, img.tex);
         java.nio.ByteBuffer buf = Tex.toBuffer(img.px);
         GL11.glPixelStorei(3317, 1);
         GL11.glTexSubImage2D(3553, 0, 0, 0, img.w, img.h, 6408, 5121, buf);
         org.lwjgl.system.MemoryUtil.memFree(buf);
         img.dirty = false;
      }

      this.tex(img.tex, x, y, w, h, 0.0F, 0.0F, 1.0F, 1.0F, -1);
   }

   /** A sunken rectangle with the inventory's slot colours (the box behind the player model). */
   public void inset(float x, float y, float w, float h) {
      this.rect(x, y, w, h, -1);
      this.rect(x, y, w - 1.0F, h - 1.0F, -13158601);
      this.rect(x + 1.0F, y + 1.0F, w - 2.0F, h - 2.0F, -7631989);
   }

   public void slot(float var1, float var2, float var3) {
      this.rect(var1, var2, var3, var3, -13158601);
      this.rect(var1, var2, var3 - 1.0F, 1.0F, -13158601);
      this.rect(var1 + var3 - 1.0F, var2, 1.0F, var3, -1);
      this.rect(var1, var2 + var3 - 1.0F, var3, 1.0F, -1);
      this.rect(var1 + 1.0F, var2 + 1.0F, var3 - 2.0F, var3 - 2.0F, -7631989);
      this.rect(var1, var2, var3 - 1.0F, 1.0F, -13158601);
      this.rect(var1, var2, 1.0F, var3 - 1.0F, -13158601);
   }

   static final String[] ARROW = {
      "..........X.....", "..........XX....", "..........XXX...", "..........XXXX..", "XXXXXXXXXXXXXXX.", "XXXXXXXXXXXXXXXX", "XXXXXXXXXXXXXXX.", "..........XXXX..",
      "..........XXX...", "..........XX....", "..........X....."
   };

   /** A pixel arrow pointing right (from the crafting grid to the result), 16 x 11 units, the top left corner at x, y. */
   public void arrow(float x, float y, int argb) {
      this.arrow(x, y, argb, 0.0F, 0);
   }

   /** The same arrow, filled with another colour from the left up to the fraction (the progress of the furnace). */
   public void arrow(float x, float y, int argb, float fraction, int fillArgb) {
      int cut = Math.round(16.0F * Math.max(0.0F, Math.min(1.0F, fraction)));
      for (int r = 0; r < ARROW.length; r++) {
         int c = 0;
         while (c < 16) {
            if (ARROW[r].charAt(c) == 'X') {
               int e = c;
               while (e < 16 && ARROW[r].charAt(e) == 'X') {
                  e++;
               }

               int mid = Math.max(c, Math.min(e, cut));
               if (mid > c) {
                  this.rect(x + c, y + r, mid - c, 1.0F, fillArgb);
               }

               if (e > mid) {
                  this.rect(x + mid, y + r, e - mid, 1.0F, argb);
               }

               c = e;
            } else {
               c++;
            }
         }
      }
   }

   static final String[] FLAME = {
      ".......X......", "......XX......", "......XXX.....", ".....XXXXX.X..", "..X..XXXXXXX..", "..XX.XXXXXXXX.", "..XXXXXXXXXXX.", ".XXXXXXXXXXXXX", ".XXXXXXXXXXXXX", ".XXXXXXXXXXXXX",
      ".XXXXXXXXXXXXX", "..XXXXXXXXXXX.", "...XXXXXXXXX..", ".....XXXXX...."
   };

   /** The flame of the furnace, 14 x 14 units: the lower part (lit = 0..1 of its height) glows, the rest is grey. */
   public void flame(float x, float y, float lit) {
      int from = 14 - Math.round(14.0F * Math.max(0.0F, Math.min(1.0F, lit)));
      for (int r = 0; r < 14; r++) {
         for (int c = 0; c < 14; c++) {
            if (FLAME[r].charAt(c) == 'X') {
               int col;
               if (r < from) {
                  col = -6710887;
               } else {
                  boolean core = r >= 8 && c >= 4 && c <= 9 && r <= 12 || r >= 5 && c >= 5 && c <= 8 && r <= 7;
                  col = core ? -12224 : -32768;
               }

               this.rect(x + c, y + r, 1.0F, 1.0F, col);
            }
         }
      }
   }

   public void panel(float var1, float var2, float var3, float var4) {
      this.rect(var1, var2, var3, var4, -16777216);
      this.rect(var1 + 1.0F, var2 + 1.0F, var3 - 2.0F, var4 - 2.0F, -3750202);
      this.rect(var1 + 1.0F, var2 + 1.0F, var3 - 3.0F, 1.0F, -1);
      this.rect(var1 + 1.0F, var2 + 1.0F, 1.0F, var4 - 3.0F, -1);
      this.rect(var1 + 2.0F, var2 + var4 - 2.0F, var3 - 3.0F, 1.0F, -11184811);
      this.rect(var1 + var3 - 2.0F, var2 + 2.0F, 1.0F, var4 - 3.0F, -11184811);
   }

   public void heart(float var1, float var2, int var3) {
      for (int var4 = 0; var4 < 9; var4++) {
         for (int var5 = 0; var5 < 9; var5++) {
            char var6 = HEART[var4].charAt(var5);
            if (var6 != '.') {
               boolean var8 = isEdge(var4, var5);
               int var7;
               if (var8) {
                  var7 = -16777216;
               } else if (var3 == 0) {
                  var7 = -14013910;
               } else if (var3 == 1 && var5 >= 5) {
                  var7 = -14013910;
               } else {
                  var7 = var4 < 3 && var5 <= 2 && var5 >= 1 && var4 > 1 ? -30584 : (var4 > 4 ? -3665904 : -1564640);
               }

               this.rect(var1 + var5, var2 + var4, 1.0F, 1.0F, var7);
            }
         }
      }
   }

   static boolean isEdge(int var0, int var1) {
      for (int var2 = -1; var2 <= 1; var2++) {
         for (int var3 = -1; var3 <= 1; var3++) {
            int var4 = var0 + var2;
            int var5 = var1 + var3;
            if (Math.abs(var2) + Math.abs(var3) == 1) {
               if (var4 < 0 || var4 > 8 || var5 < 0 || var5 > 8) {
                  return true;
               }

               if (HEART[var4].charAt(var5) == '.') {
                  return true;
               }
            }
         }
      }

      return false;
   }

   public void bubble(float var1, float var2) {
      for (int var3 = 0; var3 < 9; var3++) {
         for (int var4 = 0; var4 < 9; var4++) {
            double var5 = Math.hypot(var4 - 4, var3 - 4);
            if (!(var5 > 4.3)) {
               int var7 = var5 > 3.3 ? -13996344 : -6369025;
               if (var4 == 3 && var3 == 3) {
                  var7 = -1;
               }

               this.rect(var1 + var4, var2 + var3, 1.0F, 1.0F, var7);
            }
         }
      }
   }

   static int[] tintFor(int var0) {
      if (Blocks.tint[var0] == 1) {
         return new int[]{143, 189, 85};
      }

      if (Blocks.tint[var0] == 2) {
         if (var0 == 14) {
            return new int[]{97, 153, 97};
         } else {
            return var0 == 13 ? new int[]{128, 167, 85} : new int[]{110, 160, 48};
         }
      } else {
         return null;
      }
   }

   protected void v(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, float var10) {
      GL11.glColor4f(var5, var6, var7, var8);
      GL11.glTexCoord2f(var9 + var3 * Tex.TS, var10 + var4 * Tex.TS);
      GL11.glVertex2f(var1, var2);
   }

   private void face(int var1, float[] var2, float[] var3, float var4, int[] var5, boolean var6) {
      float var7 = Tex.tu(var1);
      float var8 = Tex.tv(var1);
      float var9 = var4;
      float var10 = var4;
      float var11 = var4;
      if (var5 != null && var6) {
         var9 *= var5[0] / 255.0F;
         var10 *= var5[1] / 255.0F;
         var11 *= var5[2] / 255.0F;
      }

      float[] var12 = new float[]{0.0F, 1.0F, 1.0F, 0.0F};
      float[] var13 = new float[]{0.0F, 0.0F, 1.0F, 1.0F};

      for (int var14 = 0; var14 < 4; var14++) {
         this.v(var2[var14], var3[var14], var12[var14], var13[var14], var9, var10, var11, 1.0F, var7, var8);
      }
   }

   public void itemIcon(int var1, float var2, float var3, float var4) {
      this.atlasTile(Items.tile[var1], var2, var3, var4);
   }

   /** One picture of the atlas (alpha-tested) as a square; a hook so that the menu screens can be drawn without OpenGL (the UI preview of the tests). */
   protected void atlasTile(int tile, float x, float y, float size) {
      this.atlasTile(tile, x, y, size, 0xFFFFFF);
   }

   /** The same, multiplied with a colour (the grass of the title logo is green because of it). */
   protected void atlasTile(int tile, float x, float y, float size, int rgb) {
      float var6 = Tex.tu(tile);
      float var7 = Tex.tv(tile);
      GL11.glEnable(3553);
      GL11.glBindTexture(3553, Tex.atlas);
      GL11.glEnable(3008);
      GL11.glAlphaFunc(516, 0.1F);
      GL11.glColor4f((rgb >> 16 & 0xFF) / 255.0F, (rgb >> 8 & 0xFF) / 255.0F, (rgb & 0xFF) / 255.0F, 1.0F);
      GL11.glBegin(7);
      GL11.glTexCoord2f(var6, var7);
      GL11.glVertex2f(x, y);
      GL11.glTexCoord2f(var6 + Tex.TS, var7);
      GL11.glVertex2f(x + size, y);
      GL11.glTexCoord2f(var6 + Tex.TS, var7 + Tex.TS);
      GL11.glVertex2f(x + size, y + size);
      GL11.glTexCoord2f(var6, var7 + Tex.TS);
      GL11.glVertex2f(x, y + size);
      GL11.glEnd();
      GL11.glDisable(3008);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
   }

   /** Starts / ends a batch of textured atlas quads (the faces of a block icon). */
   protected void quadsBegin() {
      GL11.glEnable(3553);
      GL11.glBindTexture(3553, Tex.atlas);
      GL11.glEnable(3008);
      GL11.glAlphaFunc(516, 0.1F);
      GL11.glBegin(7);
   }

   protected void quadsEnd() {
      GL11.glEnd();
      GL11.glDisable(3008);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
   }

   public void push() {
      GL11.glPushMatrix();
   }

   public void pop() {
      GL11.glPopMatrix();
   }

   public void translate(float x, float y) {
      GL11.glTranslatef(x, y, 0.0F);
   }

   public void rotate(float degrees) {
      GL11.glRotatef(degrees, 0.0F, 0.0F, 1.0F);
   }

   public void scaleBy(float s) {
      GL11.glScalef(s, s, 1.0F);
   }

   /** Everything is cut off outside this rectangle (GUI units) until {@link #unclip()}: the lists that scroll. */
   public void clip(float x, float y, float w, float h) {
      int s = this.scale;
      int fbH = Math.round(this.h * s);
      GL11.glEnable(3089);
      GL11.glScissor(Math.round(x * s), fbH - Math.round((y + h) * s), Math.max(0, Math.round(w * s)), Math.max(0, Math.round(h * s)));
   }

   public void unclip() {
      GL11.glDisable(3089);
   }

   public void hunger(float var1, float var2, int var3) {
      for (int var4 = 0; var4 < 9; var4++) {
         for (int var5 = 0; var5 < 9; var5++) {
            if (HUNGER[var4].charAt(var5) != '.') {
               boolean var6 = false;

               for (int[] var10 : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                  int var11 = var4 + var10[0];
                  int var12 = var5 + var10[1];
                  if (var11 < 0 || var11 > 8 || var12 < 0 || var12 > 8 || HUNGER[var11].charAt(var12) == '.') {
                     var6 = true;
                  }
               }

               int var13;
               if (var6) {
                  var13 = -16777216;
               } else if (var3 == 0) {
                  var13 = -12965350;
               } else if (var3 == 1 && var5 < 5) {
                  var13 = -12965350;
               } else {
                  var13 = var4 + var5 < 8 ? -3900870 : -6661592;
               }

               this.rect(var1 + var5, var2 + var4, 1.0F, 1.0F, var13);
            }
         }
      }
   }

   public void armorIcon(float var1, float var2, int var3) {
      for (int var4 = 0; var4 < 9; var4++) {
         for (int var5 = 0; var5 < 9; var5++) {
            if (ARMOR_I[var4].charAt(var5) != '.') {
               boolean var6 = false;

               for (int[] var10 : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                  int var11 = var4 + var10[0];
                  int var12 = var5 + var10[1];
                  if (var11 < 0 || var11 > 8 || var12 < 0 || var12 > 8 || ARMOR_I[var11].charAt(var12) == '.') {
                     var6 = true;
                  }
               }

               int var13;
               if (var6) {
                  var13 = -16777216;
               } else if (var3 == 0) {
                  var13 = -12566456;
               } else if (var3 == 1 && var5 >= 5) {
                  var13 = -12566456;
               } else {
                  var13 = var4 < 3 ? -1513232 : -4671288;
               }

               this.rect(var1 + var5, var2 + var4, 1.0F, 1.0F, var13);
            }
         }
      }
   }

   static float sx(float var0, float var1, float var2, float var3) {
      return var0 + var1 / 2.0F + (var2 - var3) * var1 / 2.0F;
   }

   static float sy(float var0, float var1, float var2, float var3, float var4) {
      return var0 + (var2 + var3) * var1 * 0.25F + (1.0F - var4) * var1 * 0.5F;
   }

   public static double[][] iconBoxes(int var0) {
      if (!Blocks.isBlockId(var0)) {
         return null;
      } else if (Blocks.isStair(var0)) {
         return new double[][]{{0.0, 0.0, 0.0, 1.0, 0.5, 1.0}, {0.0, 0.5, 0.0, 1.0, 1.0, 0.5}};
      } else if (Blocks.isSlab(var0)) {
         return new double[][]{{0.0, 0.0, 0.0, 1.0, 0.5, 1.0}};
      } else {
         return Blocks.isFence(var0)
            ? new double[][]{{0.375, 0.0, 0.375, 0.625, 1.0, 0.625}, {0.0, 0.62, 0.44, 1.0, 0.82, 0.56}, {0.0, 0.22, 0.44, 1.0, 0.42, 0.56}}
            : null;
      }
   }

   public void icon(int var1, float var2, float var3, float var4) {
      if (var1 != 0) {
         if (Items.isItem(var1)) {
            this.itemIcon(var1, var2, var3, var4);
         } else {
            int var5 = Blocks.kind[var1];
            this.quadsBegin();
            if (var5 == 1 || var5 == 7 || Blocks.isLadder(var1) || var1 == 32 || Blocks.isWallTorch(var1) || Blocks313.flatItem(var1)) {
               int[] var24 = tintFor(var1);
               float[] var26 = new float[]{var2, var2 + var4, var2 + var4, var2};
               float[] var28 = new float[]{var3, var3, var3 + var4, var3 + var4};
               this.face(Blocks.tiles[var1][0], var26, var28, 1.0F, var24, true);
            } else if (iconBoxes(var1) != null) {
               int[] var6 = tintFor(var1);

               for (double[] var10 : iconBoxes(var1)) {
                  float var11 = (float)var10[0];
                  float var12 = (float)var10[3];
                  float var13 = (float)var10[2];
                  float var14 = (float)var10[5];
                  float var15 = (float)var10[1];
                  float var16 = (float)var10[4];
                  float[] var17 = new float[]{
                     sx(var2, var4, var11, var13), sx(var2, var4, var12, var13), sx(var2, var4, var12, var14), sx(var2, var4, var11, var14)
                  };
                  float[] var18 = new float[]{
                     sy(var3, var4, var11, var13, var16),
                     sy(var3, var4, var12, var13, var16),
                     sy(var3, var4, var12, var14, var16),
                     sy(var3, var4, var11, var14, var16)
                  };
                  this.face(Blocks.tiles[var1][0], var17, var18, 1.0F, var6, false);
                  float[] var19 = new float[]{
                     sx(var2, var4, var11, var14), sx(var2, var4, var12, var14), sx(var2, var4, var12, var14), sx(var2, var4, var11, var14)
                  };
                  float[] var20 = new float[]{
                     sy(var3, var4, var11, var14, var16),
                     sy(var3, var4, var12, var14, var16),
                     sy(var3, var4, var12, var14, var15),
                     sy(var3, var4, var11, var14, var15)
                  };
                  this.face(Blocks.tiles[var1][3], var19, var20, 0.78F, var6, false);
                  float[] var21 = new float[]{
                     sx(var2, var4, var12, var14), sx(var2, var4, var12, var13), sx(var2, var4, var12, var13), sx(var2, var4, var12, var14)
                  };
                  float[] var22 = new float[]{
                     sy(var3, var4, var12, var14, var16),
                     sy(var3, var4, var12, var13, var16),
                     sy(var3, var4, var12, var13, var15),
                     sy(var3, var4, var12, var14, var15)
                  };
                  this.face(Blocks.tiles[var1][4], var21, var22, 0.58F, var6, false);
               }
            } else {
               int[] var23 = tintFor(var1);
               float var25 = var2 + var4 / 2.0F;
               float var27 = var3 + var4 * 0.5F;
               float[] var29 = new float[]{var25, var2 + var4, var25, var2};
               float[] var30 = new float[]{var3, var3 + var4 * 0.25F, var27, var3 + var4 * 0.25F};
               this.face(Blocks.tiles[var1][0], var29, var30, 1.0F, var23, Blocks.tint[var1] != 0);
               float[] var31 = new float[]{var2, var25, var25, var2};
               float[] var32 = new float[]{var3 + var4 * 0.25F, var27, var3 + var4, var3 + var4 * 0.75F};
               this.face(Blocks.tiles[var1][3], var31, var32, 0.78F, var23, Blocks.tint[var1] == 2);
               float[] var33 = new float[]{var25, var2 + var4, var2 + var4, var25};
               float[] var34 = new float[]{var27, var3 + var4 * 0.25F, var3 + var4 * 0.75F, var3 + var4};
               this.face(Blocks.tiles[var1][4], var33, var34, 0.58F, var23, Blocks.tint[var1] == 2);
               if (var1 == 2) {
                  int var35 = 2;
                  this.face(var35, var31, var32, 0.78F, var23, true);
                  this.face(var35, var33, var34, 0.58F, var23, true);
               }
            }

            this.quadsEnd();
         }
      }
   }
}
