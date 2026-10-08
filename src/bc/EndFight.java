package bc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

/**
 * Update 3.12: the fight against the Ender Dragon, for one End. It knows which crystals are still standing and whether the dragon has
 * been beaten already (saved in the End's folder), puts the dragon and the crystals into the world when you arrive, lets the dragon's
 * breath linger as clouds and - when the dragon is dead - opens the exit portal in the middle of the island and lays the dragon egg.
 */
final class EndFight {
   static final int CRYSTALS = 10;
   static final int DRAGON_HP = 200;
   final World w;
   /** The dragon has been killed (the exit portal exists). */
   boolean defeated;
   /** The egg has been laid once; killing the dragon again does not give another one. */
   boolean egg;
   /** Bit i set: the crystal on pillar i is gone. */
   int gone;
   /** The dragon's health when the End was left (0 = unknown, a new dragon has all of it). */
   int dragonHp;
   /** The lingering breath of the dragon: x, y, z, ticks left. */
   final ArrayList<double[]> clouds = new ArrayList<>();
   /** Ticks since the portal opened (the banner shows for a while). */
   int victory;

   EndFight(World w) {
      this.w = w;
      this.load();
   }

   private File file() {
      return this.w.dir == null ? null : new File(this.w.dir, "fight.properties");
   }

   private void load() {
      File f = this.file();
      if (f == null || !f.exists()) {
         return;
      }

      try (FileInputStream in = new FileInputStream(f)) {
         Properties p = new Properties();
         p.load(in);
         this.defeated = Boolean.parseBoolean(p.getProperty("defeated", "false"));
         this.egg = Boolean.parseBoolean(p.getProperty("egg", "false"));
         this.gone = Integer.parseInt(p.getProperty("crystalsGone", "0"));
         this.dragonHp = Integer.parseInt(p.getProperty("dragonHp", "0"));
      } catch (IOException | RuntimeException e) {
         e.printStackTrace();
      }
   }

   void save(Game g) {
      Mobs.Mob d = this.dragon(g.mobs);
      if (d != null) {
         this.dragonHp = Math.max(1, d.health);
      }

      File f = this.file();
      if (f == null) {
         return;
      }

      Properties p = new Properties();
      p.setProperty("defeated", this.defeated + "");
      p.setProperty("egg", this.egg + "");
      p.setProperty("crystalsGone", this.gone + "");
      p.setProperty("dragonHp", this.dragonHp + "");
      try {
         f.getParentFile().mkdirs();
         try (FileOutputStream out = new FileOutputStream(f)) {
            p.store(out, "Blockcraft: the fight in the End");
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   Mobs.Mob dragon(Mobs ms) {
      for (Mobs.Mob m : ms.list) {
         if (m.type == Mobs.ENDER_DRAGON && !m.dead) {
            return m;
         }
      }

      return null;
   }

   int crystalsStanding(Mobs ms) {
      int n = 0;
      for (Mobs.Mob m : ms.list) {
         if (m.type == Mobs.END_CRYSTAL && !m.dead) {
            n++;
         }
      }

      return n;
   }

   /** You have arrived in the End: the crystals and the dragon take their places (unless the dragon was beaten before). */
   void enter(Game g) {
      if (this.defeated) {
         this.openPortal(false);
         return;
      }

      boolean[] have = new boolean[CRYSTALS];
      boolean dragon = false;
      for (Mobs.Mob m : g.mobs.list) {
         if (m.type == Mobs.END_CRYSTAL && !m.dead && m.tag >= 0 && m.tag < CRYSTALS) {
            have[m.tag] = true;
         }

         if (m.type == Mobs.ENDER_DRAGON) {
            dragon = true;
         }
      }

      for (int i = 0; i < CRYSTALS; i++) {
         if (!have[i] && (this.gone & 1 << i) == 0) {
            int[] p = EndGen.PILLARS[i];
            Mobs.Mob c = g.mobs.spawn(Mobs.END_CRYSTAL, p[0] + 0.5, p[2] + 1.0, p[1] + 0.5);
            c.tag = i;
         }
      }

      if (!dragon) {
         Mobs.Mob d = g.mobs.spawn(Mobs.ENDER_DRAGON, 0.5, 84.0, 0.5);
         if (this.dragonHp > 0) {
            d.health = Math.min(DRAGON_HP, this.dragonHp);
         }
      }
   }

   void crystalDestroyed(int tag) {
      if (tag >= 0 && tag < CRYSTALS) {
         this.gone |= 1 << tag;
      }
   }

   /** The dragon is dead: from now on the End stays quiet. */
   void dragonKilled() {
      this.defeated = true;
      this.dragonHp = 0;
   }

   /** The exit portal in the middle of the island (and the egg on top of the bedrock pillar the first time). */
   void openPortal(boolean layEgg) {
      for (int[] c : EndGen.exitPortalCells()) {
         if (this.w.isLoaded(c[0], c[2])) {
            this.w.setBlock(c[0], c[1], c[2], Blocks.END_PORTAL);
         }
      }

      if (layEgg && !this.egg && this.w.isLoaded(0, 0)) {
         this.w.setBlock(0, EndGen.TOP + 5, 0, Blocks.DRAGON_EGG);
         this.egg = true;
      }

      this.victory = 1;
   }

   /** A pool of dragon's breath: purple smoke that hurts whoever stands in it. */
   void addCloud(double x, double y, double z) {
      if (this.clouds.size() > 24) {
         this.clouds.remove(0);
      }

      this.clouds.add(new double[]{x, y, z, 240.0});
   }

   void tick(Game g) {
      if (this.victory > 0 && this.victory < 100000) {
         this.victory++;
      }

      for (int i = this.clouds.size() - 1; i >= 0; i--) {
         double[] c = this.clouds.get(i);
         c[3]--;
         if (c[3] <= 0) {
            this.clouds.remove(i);
            continue;
         }

         if (g.rend != null && g.rnd.nextInt(2) == 0) {
            g.portalParticle(c[0] + (g.rnd.nextDouble() - 0.5) * 5.0, c[1] + g.rnd.nextDouble() * 1.5, c[2] + (g.rnd.nextDouble() - 0.5) * 5.0, 0.8F, 0.2F, 1.0F);
         }

         Player p = g.player;
         if (!p.dead && !p.spectator && !p.creative && Math.abs(p.x - c[0]) < 2.6 && Math.abs(p.z - c[2]) < 2.6 && p.y > c[1] - 1.0 && p.y < c[1] + 2.5 && ((int)c[3]) % 10 == 0) {
            g.damagePlayer(1, c[0], c[2]);
         }
      }
   }
}
