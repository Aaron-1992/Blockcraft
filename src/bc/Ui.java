package bc;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.lwjgl.glfw.GLFW;

/**
 * Update 3.13: the building blocks of the menu screens - panels, buttons that explain themselves (the help line at the bottom shows what the widget under
 * the mouse does), tabs, text fields, scrolling lists - and the little state they keep between frames. The screens themselves are in {@link Screens}.
 * Everything is drawn through {@link Gui}, so the screens can also be drawn without OpenGL (test/bc/UiShots).
 */
final class Ui {
   static final int TEXT = 0xFFE8E8E8;
   static final int DIM = 0xFF9A9AA6;
   static final int FAINT = 0xFF70707C;
   static final int GOLD = 0xFFFFD84A;
   static final int ACCENT = 0xFF7FD04E;
   static final int PANEL = 0xC40E1014;
   static final int PANEL_EDGE = 0xFF000000;

   final Game g;
   /** The help text of the widget the mouse is over in this frame (set while drawing, shown by {@link #drawHint}). */
   String hint;
   /** Which text field has the keyboard (Create New World: 0 = name, 1 = seed). */
   int focus;
   /** The options tab that is open. */
   int optTab;
   /** Scroll offsets of the lists (one element arrays, so that the wheel can change them). */
   final float[] optScroll = new float[1];
   final float[] worldScroll = new float[1];
   /** The scrolling area the mouse wheel acts on in this frame (set while drawing), the offset it changes, the largest offset and the step of one notch. */
   float[] wheelArea;
   float[] wheelHolder;
   float wheelMax;
   float wheelStep;
   /** The seed that "random" means on the Create New World screen (shown in the preview, used when the seed field is empty). */
   long randomSeed = new java.util.Random().nextLong();
   WorldPreview preview;
   /** Press time of the last click on a list row (a double click starts the world). */
   long lastClick;
   String lastClickId = "";
   /** A short message at the bottom of the screen and when it was set. */
   String toast;
   long toastAt;

   Ui(Game g) {
      this.g = g;
   }

   // ------------------------------------------------------------------ frame
   void begin() {
      this.hint = null;
      this.wheelArea = null;
      this.wheelHolder = null;
   }

   boolean over(float x, float y, float w, float h) {
      return this.g.hover(x, y, w, h);
   }

   /** The mouse wheel: scrolls the list under the mouse. Returns true when it was used. */
   boolean onWheel(double dy) {
      if (this.wheelArea == null || this.wheelHolder == null) {
         return false;
      }

      float mx = (float)(this.g.mouseX / this.g.gui.scale);
      float my = (float)(this.g.mouseY / this.g.gui.scale);
      float[] a = this.wheelArea;
      if (mx < a[0] || my < a[1] || mx > a[0] + a[2] || my > a[1] + a[3]) {
         return false;
      }

      this.wheelHolder[0] = Math.max(0.0F, Math.min(this.wheelMax, this.wheelHolder[0] - (float)Math.signum(dy) * this.wheelStep));
      return true;
   }

   // ------------------------------------------------------------------ background
   /** The dimmed view behind a menu: the turning world when there is one, else the dirt of the old title screen. */
   void backdrop(float dim) {
      Gui gui = this.g.gui;
      if (this.g.pano != null && this.g.world != null && this.g.screen != Game.S.LOADING) {
         float fade = this.g.pano.fade();
         if (fade > 0.0F) {
            gui.rect(0, 0, gui.w, gui.h, (int)(fade * 255.0F) << 24);
         }

         if (dim > 0.0F) {
            gui.rect(0, 0, gui.w, gui.h, (int)(dim * 255.0F) << 24);
         }
      } else if (this.g.world != null && this.g.screen != Game.S.LOADING && this.g.screen != Game.S.TITLE) {
         gui.rect(0, 0, gui.w, gui.h, 0x90000000); // the paused game behind the menu
      } else {
         gui.dirtBackground(0.28F);
         gui.gradient(0, 0, gui.w, gui.h, 0x30000000, 0x90000000);
      }
   }

   // ------------------------------------------------------------------ panels and text
   void panel(float x, float y, float w, float h) {
      Gui gui = this.g.gui;
      gui.rect(x - 1, y - 1, w + 2, h + 2, PANEL_EDGE);
      gui.rect(x, y, w, h, PANEL);
      gui.rect(x, y, w, 1, 0x28FFFFFF);
   }

   void title(String s, float y) {
      Gui gui = this.g.gui;
      gui.centered(s, gui.w / 2.0F, y, 0xFFFFFFFF, true, 1.0F);
   }

   void label(String s, float x, float y, int color) {
      this.g.gui.text(s, x, y, color, true);
   }

   /** Text in a box: wraps to the width, returns the y below the last line. */
   float paragraph(String s, float x, float y, float w, int color, int maxLines) {
      List<String> lines = Game.wrapText(s, w);
      for (int i = 0; i < lines.size() && i < maxLines; i++) {
         String line = lines.get(i);
         if (i == maxLines - 1 && lines.size() > maxLines) {
            line = Game.clipText(line + " ...", w);
         }

         this.g.gui.text(line, x, y + i * 10.0F, color, true);
      }

      return y + Math.min(maxLines, lines.size()) * 10.0F;
   }

   // ------------------------------------------------------------------ widgets
   void button(String label, float x, float y, float w, float h, boolean enabled, String hint, Runnable action) {
      this.g.button(label, x, y, w, h, enabled, action);
      this.g.btns.get(this.g.btns.size() - 1).label = label;
      if (hint != null && this.over(x, y, w, h)) {
         this.hint = hint;
      }
   }

   /** "Name: ON" with the value in colour. */
   void toggle(String name, boolean on, float x, float y, float w, float h, String hint, Runnable action) {
      this.button(name + ": " + (on ? "\u00a7aON" : "\u00a7cOFF"), x, y, w, h, true, hint, action);
   }

   void slider(String label, float x, float y, float w, float h, float value, String hint, Consumer<Float> set) {
      this.g.slider(label, x, y, w, h, value, set);
      this.g.btns.get(this.g.btns.size() - 1).label = label;
      if (hint != null && this.over(x, y, w, h)) {
         this.hint = hint;
      }
   }

   /** A row of tabs; the one at index sel is open. */
   void tabs(String[] names, int sel, float x, float y, float w, float h, Consumer<Integer> pick) {
      Gui gui = this.g.gui;
      float tw = w / names.length;
      for (int i = 0; i < names.length; i++) {
         float tx = x + i * tw;
         boolean on = i == sel;
         boolean hov = this.over(tx, y, tw, h);
         gui.rect(tx, y, tw, h, 0xFF000000);
         int top = on ? 0xFF8E93A3 : (hov ? 0xFF5C6070 : 0xFF3E414C);
         int bot = on ? 0xFF6E7383 : (hov ? 0xFF484B58 : 0xFF2E3039);
         gui.gradient(tx + 1, y + 1, tw - 2, h - 1, top, bot);
         if (on) {
            gui.rect(tx + 1, y + 1, tw - 2, 1, 0x60FFFFFF);
            gui.rect(tx + 1, y + h - 2, tw - 2, 2, ACCENT);
         }

         gui.centered(names[i], tx + tw / 2.0F, y + (h - 8.0F) / 2.0F - (on ? 1 : 0), on ? 0xFFFFFFFF : (hov ? 0xFFFFFFA0 : 0xFFB8B8C4), true, 1.0F);
         final int idx = i;
         this.g.hitbox(tx, y, tw, h, () -> pick.accept(idx));
         this.g.btns.get(this.g.btns.size() - 1).label = names[i];
      }
   }

   /** A text field. The caret blinks while it has the keyboard; text that is too long shows its end. */
   void field(float x, float y, float w, float h, String text, String placeholder, boolean focused, Runnable click) {
      Gui gui = this.g.gui;
      gui.rect(x, y, w, h, focused ? 0xFFFFFFFF : (this.over(x, y, w, h) ? 0xFFB0B0B0 : 0xFF8A8A92));
      gui.rect(x + 1, y + 1, w - 2, h - 2, 0xFF000000);
      String shown = text;
      float max = w - 10;
      while (shown.length() > 1 && Gui.textWidth(shown) > max) {
         shown = shown.substring(1);
      }

      boolean caret = focused && (long)(this.time() * 2.0) % 2L == 0L;
      if (text.isEmpty() && !focused) {
         gui.text(placeholder, x + 5, y + (h - 8.0F) / 2.0F, FAINT, true);
      } else {
         gui.text(shown + (caret ? "_" : ""), x + 5, y + (h - 8.0F) / 2.0F, 0xFFFFFFFF, true);
      }

      this.g.hitbox(x, y, w, h, click);
   }

   double time() {
      return this.g.window != 0L ? GLFW.glfwGetTime() : 0.0;
   }

   /** The help line: a small box that explains the widget the mouse is over (or says what the screen is about). */
   void drawHint(float x, float y, float w, float h, String fallback) {
      String s = this.hint != null ? this.hint : fallback;
      if (s == null) {
         return;
      }

      Gui gui = this.g.gui;
      gui.rect(x - 1, y - 1, w + 2, h + 2, 0xFF000000);
      gui.rect(x, y, w, h, 0xB0181A20);
      int lines = Math.max(1, (int)((h - 4) / 10.0F));
      this.paragraph(s, x + 5, y + 4, w - 10, this.hint != null ? 0xFFFFFFFF : DIM, lines);
   }

   // ------------------------------------------------------------------ scrolling
   /** Starts a scrolling area: everything drawn until {@link #endScroll} is cut off outside it, and the wheel scrolls it. */
   void beginScroll(float x, float y, float w, float h, float[] holder, float content, float step) {
      float max = Math.max(0.0F, content - h);
      holder[0] = Math.max(0.0F, Math.min(max, holder[0]));
      this.wheelArea = new float[]{x, y, w, h};
      this.wheelHolder = holder;
      this.wheelMax = max;
      this.wheelStep = step;
      this.g.gui.clip(x, y, w, h);
   }

   void endScroll(float x, float y, float w, float h, float content) {
      this.g.gui.unclip();
      float max = Math.max(0.0F, content - h);
      if (max > 0.0F && this.wheelHolder != null) {
         Gui gui = this.g.gui;
         float bar = Math.max(12.0F, h * h / content);
         float pos = y + (h - bar) * this.wheelHolder[0] / max;
         gui.rect(x + w + 3, y, 4, h, 0xFF000000);
         gui.rect(x + w + 4, y + 1, 2, h - 2, 0xFF2A2C34);
         gui.rect(x + w + 3, pos, 4, bar, 0xFF000000);
         gui.rect(x + w + 4, pos + 1, 2, bar - 2, 0xFFB4B8C8);
      }
   }

   /** Is a row at this y inside the visible part of the scrolling area? (Rows that are cut off must not take clicks.) */
   static boolean visible(float y, float h, float areaY, float areaH) {
      return y + h > areaY && y < areaY + areaH;
   }

   // ------------------------------------------------------------------ messages
   void say(String s) {
      this.toast = s;
      this.toastAt = System.currentTimeMillis();
   }

   void drawToast() {
      if (this.toast != null && System.currentTimeMillis() - this.toastAt < 3000L) {
         Gui gui = this.g.gui;
         float tw = Gui.textWidth(this.toast) + 12;
         gui.rect(gui.w / 2.0F - tw / 2.0F, gui.h - 22, tw, 14, 0xC0000000);
         gui.centered(this.toast, gui.w / 2.0F, gui.h - 19, 0xFFFFE070, true, 1.0F);
      }
   }

   static List<String> list(String... s) {
      return new ArrayList<>(java.util.Arrays.asList(s));
   }
}
