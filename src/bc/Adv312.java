package bc;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Update 3.12: advancements (Minecraft 1.12). A tree in five tabs, a toast in the corner when one is made, a screen on the L key
 * with the tree of every tab, and a file in the world folder that remembers them. Most are made by what you carry or where you are
 * (checked twice a second, {@link #poll}); a few by something you do ({@link #award} is called from the places where it happens).
 */
final class Adv312 {
   static final int TASK = 0;
   static final int GOAL = 1;
   static final int CHALLENGE = 2;
   static final String[] TABS = {"Minecraft", "Nether", "The End", "Adventure", "Husbandry"};
   /** What the tab buttons show. */
   static final int[] TAB_ICON = {Blocks.GRASS, Blocks.NETHERRACK, Blocks.END_STONE, Items.toolId(2, Items.C_SWORD), Blocks.HAY_BALE};
   /** The colour of the floor of each tab's tree. */
   static final int[] TAB_BG = {0xFF2A2218, 0xFF361414, 0xFF1F1A2C, 0xFF1C2A22, 0xFF383018};

   static final class A {
      final String id;
      final String title;
      final String desc;
      final int icon;
      final String parent;
      final int tab;
      final int col;
      final int row;
      final int frame;

      A(String id, String title, String desc, int icon, String parent, int tab, int col, int row, int frame) {
         this.id = id;
         this.title = title;
         this.desc = desc;
         this.icon = icon;
         this.parent = parent;
         this.tab = tab;
         this.col = col;
         this.row = row;
         this.frame = frame;
      }
   }

   static final List<A> LIST = new ArrayList<>();
   static final HashMap<String, A> BY_ID = new HashMap<>();

   private static void a(String id, String title, String desc, int icon, String parent, int tab, int col, int row, int frame) {
      A adv = new A(id, title, desc, icon, parent, tab, col, row, frame);
      LIST.add(adv);
      BY_ID.put(id, adv);
   }

   static {
      // ---- Minecraft: the story
      a("story/root", "Minecraft", "The heart and story of the game", Blocks.GRASS, null, 0, 0, 2, TASK);
      a("story/mine_stone", "Stone Age", "Mine stone with your new pickaxe", Blocks.COBBLE, "story/root", 0, 1, 2, TASK);
      a("story/upgrade_tools", "Getting an Upgrade", "Construct a better pickaxe", Items.toolId(1, Items.C_PICK), "story/mine_stone", 0, 2, 1, TASK);
      a("story/smelt_iron", "Acquire Hardware", "Smelt an iron ingot", Items.IRON_INGOT, "story/mine_stone", 0, 2, 3, TASK);
      a("story/obtain_armor", "Suit Up", "Protect yourself with a piece of iron armor", Items.armorId(1, 1), "story/smelt_iron", 0, 3, 4, TASK);
      a("story/iron_tools", "Isn't It Iron Pick", "Upgrade your pickaxe", Items.toolId(2, Items.C_PICK), "story/smelt_iron", 0, 3, 3, TASK);
      a("story/lava_bucket", "Hot Stuff", "Fill a bucket with lava", Items.LAVA_BUCKET, "story/smelt_iron", 0, 3, 2, TASK);
      a("story/mine_diamond", "Diamonds!", "Acquire diamonds", Items.DIAMOND, "story/iron_tools", 0, 4, 3, TASK);
      a("story/diamond_armor", "Cover Me with Diamonds", "Diamond armor saves lives", Items.armorId(3, 1), "story/mine_diamond", 0, 5, 3, GOAL);
      a("story/form_obsidian", "Ice Bucket Challenge", "Obtain a block of obsidian", Blocks.OBSIDIAN, "story/lava_bucket", 0, 4, 2, TASK);
      a("story/enter_the_nether", "We Need to Go Deeper", "Build, light and enter a Nether Portal", Blocks.NETHERRACK, "story/form_obsidian", 0, 5, 2, TASK);
      a("story/follow_ender_eye", "Eye Spy", "Follow an Eye of Ender", Items.EYE_OF_ENDER, "story/enter_the_nether", 0, 6, 2, TASK);
      a("story/enter_the_end", "The End?", "Enter the End", Blocks.END_STONE, "story/follow_ender_eye", 0, 7, 2, TASK);
      a("story/concrete", "Setting Concrete", "Watch concrete powder set next to water", Blocks.CONCRETE0 + 3, "story/root", 0, 1, 0, TASK);
      // ---- the Nether
      a("nether/root", "Nether", "Bring summer clothes", Blocks.NETHERRACK, null, 1, 0, 1, TASK);
      a("nether/obtain_blaze_rod", "Into Fire", "Relieve a Blaze of its rod", Items.BLAZE_ROD, "nether/root", 1, 1, 0, TASK);
      a("nether/fortress", "A Terrible Fortress", "Break your way into a Nether Fortress", Blocks.NETHER_BRICKS, "nether/root", 1, 1, 1, TASK);
      a("nether/ghast", "Return to Sender", "Kill a Ghast", Items.GHAST_TEAR, "nether/root", 1, 1, 2, GOAL);
      // ---- the End
      a("end/root", "The End", "Or the beginning?", Blocks.END_STONE, null, 2, 0, 1, TASK);
      a("end/pearl", "Spooky Pearl", "Get an Ender Pearl", Items.ENDER_PEARL, "end/root", 2, 1, 0, TASK);
      a("end/kill_dragon", "Free the End", "Good luck", Items.EYE_OF_ENDER, "end/root", 2, 1, 1, CHALLENGE);
      a("end/dragon_egg", "The Next Generation", "Hold the Dragon Egg", Blocks.DRAGON_EGG, "end/kill_dragon", 2, 2, 1, GOAL);
      // ---- Adventure
      a("adventure/root", "Adventure", "Adventure, exploration and combat", Items.toolId(2, Items.C_SWORD), null, 3, 0, 2, TASK);
      a("adventure/kill_a_mob", "Monster Hunter", "Kill any hostile monster", Items.toolId(1, Items.C_SWORD), "adventure/root", 3, 1, 0, TASK);
      a("adventure/sleep_in_bed", "Sweet Dreams", "Sleep in a bed to change your respawn point", Items.BED, "adventure/root", 3, 1, 1, TASK);
      a("adventure/trade", "What a Deal!", "Successfully trade with a Villager", Items.EMERALD, "adventure/root", 3, 1, 2, TASK);
      a("adventure/village", "A Peaceful Place", "Find a village", Items.DOOR, "adventure/root", 3, 1, 3, TASK);
      a("adventure/stronghold", "Buried Castle", "Find a stronghold deep underground", Blocks.STONEBRICK, "adventure/root", 3, 1, 4, GOAL);
      // ---- Husbandry
      a("husbandry/root", "Husbandry", "The world is full of friends and food", Blocks.HAY_BALE, null, 4, 0, 1, TASK);
      a("husbandry/plant_seed", "A Seedy Place", "Plant a seed and watch it grow", Items.SEEDS, "husbandry/root", 4, 1, 0, TASK);
      a("husbandry/breed_an_animal", "The Parrots and the Bats", "Breed two animals together", Items.WHEAT, "husbandry/root", 4, 1, 1, TASK);
      a("husbandry/bake_bread", "Bake Bread", "Turn wheat into bread", Items.BREAD, "husbandry/root", 4, 1, 2, TASK);
      a("husbandry/dye_sheep", "Color Me Impressed", "Dye a sheep", Color312.dyeItem(14), "husbandry/root", 4, 1, 3, TASK);
   }

   /** The advancements made in this world. */
   final LinkedHashSet<String> done = new LinkedHashSet<>();
   private final ArrayDeque<A> queue = new ArrayDeque<>();
   /** The advancement whose toast is showing, and for how long it already does. */
   A shown;
   int shownTicks;
   /** The tab and the advancement under the mouse on the screen. */
   int tab;
   static final int TOAST_TICKS = 110;

   boolean has(String id) {
      return this.done.contains(id);
   }

   int count() {
      return this.done.size();
   }

   int count(int tabNo) {
      int n = 0;
      for (A adv : LIST) {
         if (adv.tab == tabNo && this.done.contains(adv.id)) {
            n++;
         }
      }

      return n;
   }

   static int total(int tabNo) {
      int n = 0;
      for (A adv : LIST) {
         if (adv.tab == tabNo) {
            n++;
         }
      }

      return n;
   }

   /** Makes an advancement (nothing happens when it was made before, or the name is unknown). True when it was new. */
   boolean award(Game g, String id) {
      A adv = BY_ID.get(id);
      if (adv == null || !this.done.add(id)) {
         return false;
      }

      this.queue.add(adv);
      this.save(g == null ? null : g.rootDir);
      return true;
   }

   void revokeAll(Game g) {
      this.done.clear();
      this.queue.clear();
      this.shown = null;
      this.save(g == null ? null : g.rootDir);
   }

   /** Called every game tick: runs the toast. */
   void tick() {
      if (this.shown != null) {
         this.shownTicks++;
         if (this.shownTicks >= TOAST_TICKS) {
            this.shown = null;
         }
      }

      if (this.shown == null && !this.queue.isEmpty()) {
         this.shown = this.queue.poll();
         this.shownTicks = 0;
      }
   }

   // ------------------------------------------------------------------ what you carry and where you are
   private static boolean has(Game g, int id) {
      for (Stack s : g.inv) {
         if (s != null && s.id == id) {
            return true;
         }
      }

      for (Stack s : g.armor) {
         if (s != null && s.id == id) {
            return true;
         }
      }

      return false;
   }

   private static boolean hasArmor(Game g, int material) {
      for (Stack[] list : new Stack[][]{g.inv, g.armor}) {
         for (Stack s : list) {
            if (s != null && Items.isArmor(s.id) && Items.armorMat(s.id) == material) {
               return true;
            }
         }
      }

      return false;
   }

   /** Twice a second: the advancements that come from what you hold or where you stand. */
   void poll(Game g) {
      if (g.player == null || g.world == null || g.player.dead) {
         return;
      }

      this.award(g, "story/root");
      this.award(g, "adventure/root");
      this.award(g, "husbandry/root");
      if (has(g, Blocks.COBBLE)) {
         this.award(g, "story/mine_stone");
      }

      if (has(g, Items.toolId(1, Items.C_PICK))) {
         this.award(g, "story/upgrade_tools");
      }

      if (has(g, Items.IRON_INGOT)) {
         this.award(g, "story/smelt_iron");
      }

      if (hasArmor(g, 1)) {
         this.award(g, "story/obtain_armor");
      }

      if (has(g, Items.toolId(2, Items.C_PICK))) {
         this.award(g, "story/iron_tools");
      }

      if (has(g, Items.DIAMOND)) {
         this.award(g, "story/mine_diamond");
      }

      if (hasArmor(g, 3)) {
         this.award(g, "story/diamond_armor");
      }

      if (has(g, Items.LAVA_BUCKET)) {
         this.award(g, "story/lava_bucket");
      }

      if (has(g, Blocks.OBSIDIAN)) {
         this.award(g, "story/form_obsidian");
      }

      if (has(g, Items.BLAZE_ROD)) {
         this.award(g, "nether/obtain_blaze_rod");
      }

      if (has(g, Items.ENDER_PEARL)) {
         this.award(g, "end/pearl");
      }

      if (has(g, Blocks.DRAGON_EGG)) {
         this.award(g, "end/dragon_egg");
      }

      if (has(g, Items.BREAD)) {
         this.award(g, "husbandry/bake_bread");
      }

      Player p = g.player;
      World w = g.world;
      if (w.dim == Dim.NETHER) {
         this.award(g, "story/enter_the_nether");
         this.award(g, "nether/root");
         int below = w.getBlock((int)Math.floor(p.x), (int)Math.floor(p.y - 0.2), (int)Math.floor(p.z));
         if (below == Blocks.NETHER_BRICKS || below == Blocks.NETHER_BRICK_FENCE) {
            this.award(g, "nether/fortress");
         }
      } else if (w.dim == Dim.END) {
         this.award(g, "story/enter_the_end");
         this.award(g, "story/follow_ender_eye");
         this.award(g, "end/root");
         if (w.fight != null && w.fight.defeated) {
            this.award(g, "end/kill_dragon");
         }
      } else if (w.dim == Dim.OVERWORLD) {
         int rx = Math.floorDiv((int)Math.floor(p.x), 320);
         int rz = Math.floorDiv((int)Math.floor(p.z), 320);
         if (!this.has("adventure/village")) {
            for (int dx = -1; dx <= 1; dx++) {
               for (int dz = -1; dz <= 1; dz++) {
                  Structs.Village v = w.gen.structs.village(rx + dx, rz + dz);
                  if (v != null && v.valid && Math.hypot(p.x - v.cx, p.z - v.cz) < 44.0) {
                     this.award(g, "adventure/village");
                  }
               }
            }
         }

         if (!this.has("adventure/stronghold") && p.y < 60.0 && Gen.extras) {
            Stronghold s = Stronghold.nearest(w.seed, p.x, p.z);
            if (s != null && Math.abs(s.cx - p.x) < 140.0 && Math.abs(s.cz - p.z) < 140.0) {
               Stronghold.Plan plan = s.plan();
               if (p.x >= plan.minX && p.x <= plan.maxX + 1 && p.z >= plan.minZ && p.z <= plan.maxZ + 1 && p.y >= plan.oy - 1 && p.y <= plan.oy + 9) {
                  this.award(g, "adventure/stronghold");
               }
            }
         }
      }
   }

   /** The player killed a mob. */
   void killed(Game g, Mobs.Mob m) {
      if (Mobs.hostile(m.type)) {
         this.award(g, "adventure/kill_a_mob");
      }

      if (m.type == Mobs.GHAST) {
         this.award(g, "nether/ghast");
      }
   }

   // ------------------------------------------------------------------ the file
   static File file(File dir) {
      return dir == null ? null : new File(dir, "advancements.dat");
   }

   void save(File dir) {
      File f = file(dir);
      if (f == null) {
         return;
      }

      try {
         StringBuilder sb = new StringBuilder();
         for (String id : this.done) {
            sb.append(id).append('\n');
         }

         Files.write(f.toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
      } catch (Exception e) {
         System.out.println("could not save the advancements: " + e);
      }
   }

   void load(File dir) {
      this.done.clear();
      this.queue.clear();
      this.shown = null;
      File f = file(dir);
      if (f == null || !f.isFile()) {
         return;
      }

      try {
         for (String line : Files.readAllLines(f.toPath(), StandardCharsets.UTF_8)) {
            String id = line.trim();
            if (BY_ID.containsKey(id)) {
               this.done.add(id);
            }
         }
      } catch (Exception e) {
         System.out.println("could not read the advancements: " + e);
      }
   }

   // ------------------------------------------------------------------ the toast in the corner
   static int headerColor(int frame) {
      return frame == CHALLENGE ? 0xFFFF55FF : 0xFFFFFF55;
   }

   static String header(int frame) {
      return frame == CHALLENGE ? "Challenge Complete!" : (frame == GOAL ? "Goal Reached!" : "Advancement Made!");
   }

   static int frameColor(int frame, boolean done) {
      if (!done) {
         return 0xFF5A5A5A;
      }

      return frame == CHALLENGE ? 0xFFC060F0 : (frame == GOAL ? 0xFFF2B84A : 0xFFE9D27C);
   }

   void drawToast(Game g) {
      A adv = this.shown;
      if (adv == null) {
         return;
      }

      Gui gui = g.gui;
      float w = 164.0F;
      float h = 32.0F;
      float slide = 0.0F;
      float t = this.shownTicks + g.partial;
      if (t < 8.0F) {
         float k = 1.0F - t / 8.0F;
         slide = (w + 8.0F) * k * k;
      } else if (t > TOAST_TICKS - 8.0F) {
         float k = (t - (TOAST_TICKS - 8.0F)) / 8.0F;
         slide = (w + 8.0F) * k * k;
      }

      float x = gui.w - w - 4.0F + slide;
      float y = 4.0F;
      gui.rect(x - 1.0F, y - 1.0F, w + 2.0F, h + 2.0F, frameColor(adv.frame, true));
      gui.rect(x, y, w, h, 0xF0161616);
      gui.rect(x + 5.0F, y + 5.0F, 22.0F, 22.0F, 0xFF2A2A2A);
      gui.icon(adv.icon, x + 8.0F, y + 8.0F, 16.0F);
      gui.text(header(adv.frame), x + 32.0F, y + 6.0F, headerColor(adv.frame), true);
      gui.text(Game.clipText(adv.title, w - 38.0F), x + 32.0F, y + 18.0F, 0xFFFFFFFF, true);
   }

   // ------------------------------------------------------------------ the screen
   private static final float CELL_W = 30.0F;
   private static final float CELL_H = 27.0F;

   void drawScreen(Game g) {
      Gui gui = g.gui;
      float cw = gui.w;
      float ch = gui.h;
      float pw = Math.min(cw - 10.0F, 290.0F);
      float ph = Math.min(ch - 10.0F, 178.0F);
      float px = (float)Math.floor((cw - pw) / 2.0F);
      float py = (float)Math.floor((ch - ph) / 2.0F);
      gui.rect(0.0F, 0.0F, cw, ch, 0x99000000);
      // the tabs
      float tabW = 28.0F;
      float tabH = 28.0F;
      A hover = null;
      String hoverTab = null;
      float hoverTabX = 0.0F;
      for (int i = 0; i < TABS.length; i++) {
         float tx = px + 4.0F + i * (tabW + 2.0F);
         boolean on = i == this.tab;
         gui.rect(tx - 1.0F, py - tabH + 3.0F, tabW + 2.0F, tabH + (on ? 3.0F : 1.0F), 0xFF000000);
         gui.rect(tx, py - tabH + 4.0F, tabW, tabH + (on ? 3.0F : 0.0F), on ? 0xFF8B8B8B : 0xFF555555);
         gui.icon(TAB_ICON[i], tx + 6.0F, py - tabH + 10.0F, 16.0F);
         final int idx = i;
         g.hitbox(tx, py - tabH + 4.0F, tabW, tabH, () -> this.tab = idx);
         if (g.hover(tx, py - tabH + 4.0F, tabW, tabH)) {
            hoverTab = TABS[i] + "  " + this.count(i) + "/" + total(i);
            hoverTabX = tx;
         }
      }

      // the window
      gui.rect(px - 2.0F, py - 2.0F, pw + 4.0F, ph + 4.0F, 0xFF000000);
      gui.rect(px - 1.0F, py - 1.0F, pw + 2.0F, ph + 2.0F, 0xFF8B8B8B);
      gui.rect(px, py, pw, ph, TAB_BG[this.tab]);
      float topBar = 18.0F;
      gui.rect(px, py, pw, topBar, 0x99000000);
      gui.text(TABS[this.tab], px + 6.0F, py + 5.0F, 0xFFFFFFFF, true);
      String cnt = this.count(this.tab) + " / " + total(this.tab) + "    All: " + this.count() + " / " + LIST.size();
      gui.text(cnt, px + pw - Gui.textWidth(cnt) - 6.0F, py + 5.0F, 0xFFC8C8C8, true);
      // the floor of the tree: the blocks the tab is named after, dark
      float ox = px + 12.0F;
      float oy = py + topBar + 10.0F;
      // the lines between the nodes, then the nodes
      for (A adv : LIST) {
         if (adv.tab != this.tab || adv.parent == null) {
            continue;
         }

         A par = BY_ID.get(adv.parent);
         float x0 = ox + par.col * CELL_W + 24.0F;
         float y0 = oy + par.row * CELL_H + 12.0F;
         float x1 = ox + adv.col * CELL_W;
         float y1 = oy + adv.row * CELL_H + 12.0F;
         float mid = x1 - 3.0F - (CELL_W - 24.0F) / 2.0F + 3.0F;
         int lc = this.done.contains(adv.id) ? 0xFFEDEDED : 0xFF6A6A6A;
         gui.rect(x0, y0 - 1.0F, mid - x0 + 1.0F, 2.0F, lc);
         gui.rect(mid - 1.0F, Math.min(y0, y1) - 1.0F, 2.0F, Math.abs(y1 - y0) + 2.0F, lc);
         gui.rect(mid, y1 - 1.0F, x1 - mid, 2.0F, lc);
      }

      for (A adv : LIST) {
         if (adv.tab != this.tab) {
            continue;
         }

         float nx = ox + adv.col * CELL_W;
         float ny = oy + adv.row * CELL_H;
         boolean ok = this.done.contains(adv.id);
         gui.rect(nx - 1.0F, ny - 1.0F, 26.0F, 26.0F, 0xFF000000);
         gui.rect(nx, ny, 24.0F, 24.0F, frameColor(adv.frame, ok));
         gui.rect(nx + 2.0F, ny + 2.0F, 20.0F, 20.0F, ok ? 0xFF3C3C3C : 0xFF1E1E1E);
         gui.icon(adv.icon, nx + 4.0F, ny + 4.0F, 16.0F);
         if (!ok) {
            gui.rect(nx + 2.0F, ny + 2.0F, 20.0F, 20.0F, 0xB0101010);
         }

         if (g.hover(nx, ny, 24.0F, 24.0F)) {
            hover = adv;
         }
      }

      g.button("Done", px + pw - 60.0F, py + ph + 6.0F, 60.0F, 16.0F, true, () -> g.setScreen(Game.S.PLAY));
      gui.text("L or Esc closes", px, py + ph + 10.0F, 0xFF909090, true);
      if (hover != null) {
         this.tooltip(g, hover);
      } else if (hoverTab != null) {
         float tw = Gui.textWidth(hoverTab);
         gui.rect(hoverTabX - 1.0F, py + 2.0F, tw + 8.0F, 14.0F, 0xF0100010);
         gui.text(hoverTab, hoverTabX + 3.0F, py + 5.0F, 0xFFFFFFFF, true);
      }
   }

   private void tooltip(Game g, A adv) {
      Gui gui = g.gui;
      boolean ok = this.done.contains(adv.id);
      List<String> lines = new ArrayList<>(Game.wrapText(adv.desc, 150.0F));
      float w = Gui.textWidth(adv.title);
      for (String s : lines) {
         w = Math.max(w, Gui.textWidth(s));
      }

      String state = ok ? "Done" : "Not done yet";
      w = Math.max(w, Gui.textWidth(state));
      float h = 8.0F + 10.0F * (lines.size() + 2);
      float mx = (float)(g.mouseX / gui.scale);
      float my = (float)(g.mouseY / gui.scale);
      float x = mx + 10.0F;
      float y = my - 6.0F;
      if (x + w + 10.0F > gui.w) {
         x = mx - w - 16.0F;
      }

      if (y + h > gui.h) {
         y = gui.h - h - 2.0F;
      }

      gui.rect(x - 1.0F, y - 1.0F, w + 8.0F, h + 2.0F, frameColor(adv.frame, ok));
      gui.rect(x, y, w + 6.0F, h, 0xF0100010);
      gui.text(adv.title, x + 3.0F, y + 4.0F, ok ? headerColor(adv.frame) : 0xFFFFFFFF, true);
      for (int i = 0; i < lines.size(); i++) {
         gui.text(lines.get(i), x + 3.0F, y + 14.0F + i * 10.0F, 0xFFB8B8B8, true);
      }

      gui.text(state, x + 3.0F, y + 14.0F + lines.size() * 10.0F, ok ? 0xFF55FF55 : 0xFF909090, true);
   }
}
