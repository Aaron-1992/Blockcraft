package bc;

import bc.World.Hit;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public final class Mobs {
   public static final int PIG = 0;
   public static final int COW = 1;
   public static final int SHEEP = 2;
   public static final int CHICKEN = 3;
   public static final int ZOMBIE = 4;
   public static final int SKELETON = 5;
   public static final int CREEPER = 6;
   public static final int SPIDER = 7;
   // ---- Update 3.12 ----
   public static final int ZOMBIE_PIGMAN = 8;
   public static final int GHAST = 9;
   public static final int BLAZE = 10;
   public static final int MAGMA_CUBE = 11;
   public static final int ENDERMAN = 12;
   public static final int ENDER_DRAGON = 13;
   public static final int END_CRYSTAL = 14;
   /** The villages (villager, iron golem) and the parrots. */
   public static final int VILLAGER = 15;
   public static final int IRON_GOLEM = 16;
   public static final int PARROT = 17;
   public static final int N = 18;
   public static final String[] NAME = new String[]{
      "pig", "cow", "sheep", "chicken", "zombie", "skeleton", "creeper", "spider", "pigman", "ghast", "blaze", "magmacube", "enderman", "enderdragon", "endcrystal", "villager", "irongolem", "parrot"
   };
   static final int[] HP = new int[]{10, 10, 8, 4, 20, 20, 20, 16, 20, 10, 20, 16, 40, 200, 1, 20, 100, 6};
   /** Lights a creeper's fuse (flint and steel). False for anything else, or if it is already lit. */
   public boolean ignite(Mobs.Mob m) {
      if (m == null || m.dead || m.type != CREEPER || m.ignited) {
         return false;
      }

      m.ignited = true;
      Net319.onClientInteractMob(m, "IGNITE", 0);
      return true;
   }

   /**
    * Jump speed of a mob that walks into something. A mob's tick applies gravity BEFORE it moves, so it needs 0.42 / 0.98 + 0.08 to move
    * up by Minecraft's 0.42 in the first tick and reach the same height (1.25 blocks, enough for one block but not for two). With the old
    * value 0.42 the first tick only moved 0.33 and the jump peaked at 0.83 blocks: mobs hopped against every full block without ever
    * getting on top of it.
    */
   static final double JUMP = 0.42 / 0.98 + 0.08;
   static final float[] W = new float[]{0.9F, 0.9F, 0.9F, 0.4F, 0.6F, 0.6F, 0.6F, 1.4F, 0.6F, 4.0F, 0.6F, 0.52F, 0.6F, 8.0F, 1.6F, 0.6F, 1.4F, 0.5F};
   static final float[] H = new float[]{0.9F, 1.4F, 1.3F, 0.7F, 1.95F, 1.99F, 1.7F, 0.9F, 1.95F, 4.0F, 1.8F, 0.52F, 2.9F, 4.0F, 1.6F, 1.95F, 2.7F, 0.9F};
   static final float[] SPD = new float[]{0.1F, 0.1F, 0.1F, 0.1F, 0.13F, 0.125F, 0.125F, 0.17F, 0.12F, 0.05F, 0.06F, 0.1F, 0.16F, 0.5F, 0.0F, 0.1F, 0.1F, 0.12F};
   static final int[] SHEEP_COLORS = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 8, 8, 7, 15, 12, 6};
   final Game g;
   public final ArrayList<Mobs.Mob> list = new ArrayList();
   public final ArrayList<Mobs.Arrow> arrows = new ArrayList();
   /** Update 3.12: what ghasts and blazes shoot. */
   public final ArrayList<Mobs312.Fireball> fireballs = new ArrayList();
   /** Update 3.12: the vertical speed a flying mob (ghast, blaze, dragon, parrot) is working towards this tick. */
   double flyVy;
   final Random rnd = new Random();
   int timer;
   final double[] res = new double[3];
   final HashMap<Long, Integer> spawnerCd = new HashMap();

   public static boolean hostile(int var0) {
      return var0 >= 4 && !Mobs312.peaceful(var0);
   }

   public Mobs(Game var1) {
      this.g = var1;
      RigDraw.warm(); // Update 3.17: paint the mob skins in the background while the title screen is up
   }

   public void clear() {
      this.list.clear();
      this.arrows.clear();
      this.fireballs.clear();
   }

   public int count(boolean var1) {
      int var2 = 0;

      for (Mobs.Mob var4 : this.list) {
         if (!var4.dead && hostile(var4.type) == var1) {
            var2++;
         }
      }

      return var2;
   }

   static float daylightAt(long var0) {
      double var2 = var0 % 24000L;
      double var4 = Math.cos((Math.PI * 2) * (var2 - 6000.0) / 24000.0);
      return (float)Math.max(0.0, Math.min(1.0, 0.5 + var4 * 1.3));
   }

   public Mobs.Mob findByNetId(int netId) {
      if (netId <= 0) {
         return null;
      }
      for (Mobs.Mob m : this.list) {
         if (m != null && m.netId == netId) {
            return m;
         }
      }
      return null;
   }

   public Mobs.Mob spawn(int var1, double var2, double var4, double var6) {
      Mobs.Mob var8 = new Mobs.Mob();
      var8.netId = Net319.nextMobId();
      var8.type = var1;
      var8.health = HP[var1];
      var8.setPos(var2, var4, var6);
      var8.tx = var2;
      var8.ty = var4;
      var8.tz = var6;
      var8.yaw = var8.pyaw = this.rnd.nextFloat() * 360.0F;
      var8.dirYaw = var8.yaw;
      var8.headYaw = var8.pheadYaw = var8.yaw;
      var8.px = var2;
      var8.py = var4;
      var8.pz = var6;
      var8.wander = this.rnd.nextInt(60);
      var8.cd = 40;
      if (var1 == 2) {
         var8.color = SHEEP_COLORS[this.rnd.nextInt(SHEEP_COLORS.length)];
      }

      Mobs312.born(var8, this.rnd);

      this.list.add(var8);
      return var8;
   }

   public void tick() {
      World var1 = this.g.world;
      Player var2 = this.g.player;
      if (var1 != null && var2 != null) {
         float var3 = daylightAt(var1.dayTime);
         boolean remoteClientOnly = Net319.isClientConnected() && !Net319.isHosting();

         for (int var4 = this.list.size() - 1; var4 >= 0; var4--) {
            Mobs.Mob var5 = (Mobs.Mob)this.list.get(var4);
            if (var5.still) {
               // Update 3.17 test hook (bc.still): a mob that stands still for the screenshot, only its animation clock runs
               var5.age++;
               if (var5.dead) {
                  var5.deathTime = Math.min(var5.deathTime + 1, 19);
               }

               var5.pyaw = var5.yaw;
               var5.pheadYaw = var5.headYaw;
               var5.pheadPitch = var5.headPitch;
               var5.pwalk = var5.walk;
               var5.pwalkAmp = var5.walkAmp;
               var5.pswell = var5.swell;
            } else if (var5.dead) {
               var5.deathTime++;
               Mobs312.tickDying(this, var5, var1);
               if (var5.deathTime >= (var5.type == ENDER_DRAGON ? Dragon.DEATH_TICKS : 20)) {
                  if (SkyGen.isMinigameBot(var5)) {
                     SkyGen.onBotKilled(this.g, var5);
                  }
                  this.list.remove(var4);
                  Mobs312.removed(this, var5, var1);
               }
            } else if (remoteClientOnly && var5.remoteSynced) {
               var5.px = var5.x;
               var5.py = var5.y;
               var5.pz = var5.z;
               var5.pyaw = var5.yaw;
               var5.pheadYaw = var5.headYaw;
               var5.pheadPitch = var5.headPitch;
               var5.pwalk = var5.walk;
               var5.pwalkAmp = var5.walkAmp;
               var5.pswell = var5.swell;
               var5.age++;
               if (var5.hurtTime > 0) {
                  var5.hurtTime--;
               }
               if (var5.invuln > 0) {
                  var5.invuln--;
               }
               if (var5.attackAnim > 0) {
                  var5.attackAnim--;
               }
               double nx = var5.x + (var5.tx - var5.x) * 0.5;
               double ny = var5.y + (var5.ty - var5.y) * 0.5;
               double nz = var5.z + (var5.tz - var5.z) * 0.5;
               var5.setPos(nx, ny, nz);
               double dx = var5.x - var5.px;
               double dz = var5.z - var5.pz;
               float step = (float)Math.min(0.6, Math.hypot(dx, dz));
               if (SkyGen.isMinigameBot(var5)) {
                  var5.walkAmp += (Math.min(1.15F, step * 8.5F) - var5.walkAmp) * 0.48F;
                  var5.walk += step * 4.5F;
               } else {
                  var5.walkAmp += (Math.min(1.0F, step * 4.5F) - var5.walkAmp) * 0.45F;
                  var5.walk += step * 2.5F;
               }
               var5.moving = var5.moving || step > 0.01F;
            } else {
               double var6 = var5.x - var2.x;
               double var8 = var5.z - var2.z;
               double var10 = var6 * var6 + var8 * var8;
               boolean isBot = SkyGen.isMinigameBot(var5);
               if (isBot || Mobs312.persistent(var5.type) || Mobs312.keepsForever(var5.type) || BackroomsStalkerMod.isStalkerMob(var5) || var5.tamed || !(var10 > 16384.0) && (!hostile(var5.type) || !(var10 > 1600.0) || var5.age <= 600 || this.rnd.nextInt(600) != 0)) {
                  if (isBot || var1.isLoaded((int)Math.floor(var5.x), (int)Math.floor(var5.z))) {
                     this.tickMob(var5, var1, var2, var3);
                     double voidY = isBot ? (SkyGen.Y_DECK - 20.0) : -20.0;
                     if (var5.y < voidY) {
                        if (isBot) {
                           SkyGen.onBotKilled(this.g, var5);
                        }
                        this.list.remove(var4);
                     }
                  }
               } else {
                  this.list.remove(var4);
               }
            }
         }

         for (int var12 = this.arrows.size() - 1; var12 >= 0; var12--) {
            if (this.tickArrow((Mobs.Arrow)this.arrows.get(var12), var1, var2)) {
               this.arrows.remove(var12);
            }
         }

         this.timer++;
         Mobs312.tickFireballs(this, var1, var2);
         if (!remoteClientOnly || !Net319.hostHasMobSync) {
            if (this.timer % 20 == 3) {
               this.tickSpawners(var1, var2);
            }

            if (this.timer % 20 == 0 && (var1.settings == null || !var1.settings.isMinigame())) {
               if (var1.dim == Dim.OVERWORLD) {
                  this.spawnHostile(var1, var2, var3);
               } else {
                  Mobs312.spawnElsewhere(this, var1, var2);
               }
            }

            if (this.timer % 40 == 7 && var1.dim == Dim.OVERWORLD && (var1.settings == null || !var1.settings.isMinigame())) {
               this.spawnPassive(var1, var2);
               Mobs312.spawnVillagers(this, var1, var2);
               Mobs312.spawnParrots(this, var1, var2);
            }
         }

         if (Net319.isHosting() && this.timer % 2 == 0) {
            Net319.broadcastMobs(var1, this);
         }
      }
   }

   boolean targetable(Player var1) {
      return !var1.dead && (var1.gameMode == 0 || var1.gameMode == 2);
   }

   static float angleTo(double var0, double var2) {
      return (float)Math.toDegrees(Math.atan2(-var0, var2));
   }

   static float wrap(float var0) {
      while (var0 > 180.0F) {
         var0 -= 360.0F;
      }

      while (var0 < -180.0F) {
         var0 += 360.0F;
      }

      return var0;
   }

   void tickMob(Mobs.Mob var1, World var2, Player var3, float var4) {
      var1.px = var1.x;
      var1.py = var1.y;
      var1.pz = var1.z;
      var1.pyaw = var1.yaw;
      var1.pheadYaw = var1.headYaw;
      var1.pheadPitch = var1.headPitch;
      var1.pwalk = var1.walk;
      var1.pwalkAmp = var1.walkAmp;
      var1.pswell = var1.swell;
      var1.age++;
      if (var1.love > 0) {
         var1.love--;
      }

      if (var1.breedCd > 0) {
         var1.breedCd--;
      }

      if (var1.babyAge > 0 && --var1.babyAge == 0) {
         var1.setPos(var1.x, var1.y, var1.z);
      }

      if (var1.sheared && --var1.woolTimer <= 0) {
         var1.sheared = false;
      }
      if (var1.hurtTime > 0) {
         var1.hurtTime--;
      }

      if (var1.invuln > 0) {
         var1.invuln--;
      }

      if (var1.attackAnim > 0) {
         var1.attackAnim--;
      }

      if (var1.angry > 0) {
         var1.angry--;
      }

      // Update 3.12: the dragon and the End crystals do not walk, they have their own tick
      if (Mobs312.tickSpecial(this, var1, var2, var3)) {
         return;
      }

      int var5 = (int)Math.floor(var1.x);
      int var6 = (int)Math.floor(var1.z);
      var1.inWater = Blocks.isWater(var2.getBlock(var5, (int)Math.floor(var1.y + 0.3), var6));
      boolean var7 = Blocks.isLava(var2.getBlock(var5, (int)Math.floor(var1.y + 0.1), var6)) && !Mobs312.fireproof(var1.type);
      if (var7 && var1.age % 10 == 0) {
         this.hurtMob(var1, 4, var1.x, var1.z, 0.0, false);
      }

      if (var7) {
         var1.burn = Math.max(var1.burn, 160);
      }

      // Update 3.7: standing in a fire sets a creature alight
      if (!Mobs312.fireproof(var1.type) && (Blocks.isFire(var2.getBlock(var5, (int)Math.floor(var1.y + 0.1), var6)) || Blocks.isFire(var2.getBlock(var5, (int)Math.floor(var1.y + 0.9), var6)))) {
         var1.burn = Math.max(var1.burn, 160);
         if (var1.age % 10 == 0) {
            this.hurtMob(var1, 1, var1.x, var1.z, 0.0, false);
         }
      }

      if (var1.burn > 0 && var1.age % 3 == 0 && !var1.dead) {
         this.g.flameParticle(var1.x + (this.rnd.nextDouble() - 0.5) * W[var1.type], var1.y + this.rnd.nextDouble() * H[var1.type], var1.z + (this.rnd.nextDouble() - 0.5) * W[var1.type], this.rnd.nextInt(3) == 0);
      }

      // chickens lay an egg every 5-10 minutes
      if (var1.type == 3 && var1.babyAge == 0 && !var1.dead) {
         if (var1.eggTimer <= 0) {
            var1.eggTimer = 6000 + this.rnd.nextInt(6000);
         } else if (--var1.eggTimer == 0) {
            this.drop(var2, var1, Items.EGG, 1);
         }
      }

      if (!var1.dead) {
         if ((var1.type == 4 || var1.type == 5)
            && (var2.settings == null || !var2.settings.isMinigame())
            && (var1.teamTag == null || !var1.teamTag.startsWith("Team:"))
            && var4 > 0.7
            && !var1.inWater
            && var2.getSky(var5, (int)Math.floor(var1.y + 1.5), var6) >= 15
            && !var2.weather.rainingAt(var5, (int)Math.floor(var1.y + 1.5), var6)) {
            var1.burn = 40;
         }

         if (var1.burn > 0) {
            var1.burn--;
            if (var1.inWater) {
               var1.burn = 0;
            }

            if (var1.burn % 20 == 0 && var1.burn > 0 || var1.burn == 39) {
               this.hurtMob(var1, 1, var1.x, var1.z, 0.0, false);
            }

            if (var1.dead) {
               return;
            }
         }

         double var8 = 0.0;
         double var10 = 0.0;
         boolean var12 = false;
         var1.hasTarget = false;
         this.flyVy = 0.0;
         double var13 = SPD[var1.type];
         boolean var15 = hostile(var1.type) && this.targetable(var3);
         double var16 = var3.x - var1.x;
         double var18 = var3.z - var1.z;
         double var20 = var3.y + 0.9 - (var1.y + H[var1.type] * 0.5);
         double var22 = Math.sqrt(var16 * var16 + var18 * var18);
         double var24 = Math.sqrt(var22 * var22 + var20 * var20);
         float var26 = var1.yaw;
         float var27 = 0.0F;
         if (SkyGen.isMinigameBot(var1)) {
            SkyGen.BotIntent bi = SkyGen.tickBotAI(this, var1, var2, var3);
            if (bi != null) {
               var12 = bi.moving;
               var1.moving = bi.moving;
               var13 *= bi.speedMul;
               var26 = bi.headYaw;
               var27 = bi.headPitch;
            } else {
               var1.moving = false;
            }
         } else switch (var1.type) {
            case 4:
               if (var15 && var24 < 24.0) {
                  var1.hasTarget = true;
                  var1.dirYaw = angleTo(var16, var18);
                  var26 = var1.dirYaw;
                  var27 = (float)(-Math.toDegrees(Math.atan2(var20, var22)));
                  if (var22 > 0.8) {
                     var12 = true;
                  }

                  if (var22 < 1.3 && Math.abs(var20) < 1.8 && var1.cd <= 0) {
                     var1.cd = 20;
                     var1.attackAnim = 8;
                     this.g.damagePlayer(3, var1.x, var1.z);
                  }
               } else {
                  this.wanderAI(var1);
               }

               if (var1.cd > 0) {
                  var1.cd--;
               }

               if (!var1.hasTarget) {
                  var12 = var1.moving;
               }

               if (!var1.hasTarget) {
                  var13 *= 0.6;
               }
               break;
            case 5:
               if (var15 && var24 < 20.0) {
                  var1.hasTarget = true;
                  var1.dirYaw = angleTo(var16, var18);
                  var26 = var1.dirYaw;
                  var27 = (float)(-Math.toDegrees(Math.atan2(var20, var22)));
                  boolean var40 = this.hasLOS(var2, var1.x, var1.y + 1.5, var1.z, var3.x, var3.y + 1.0, var3.z);
                  if (var1.age % 60 == 0) {
                     var1.strafeDir = -var1.strafeDir;
                  }

                  if (!var40 || var22 > 9.0) {
                     var12 = true;
                  } else if (var22 < 4.0) {
                     var12 = true;
                     var1.dirYaw += 180.0F;
                  } else {
                     var12 = true;
                     var1.dirYaw = var1.dirYaw + 90 * var1.strafeDir;
                     var13 *= 0.6;
                  }

                  if (var40 && var24 < 16.0 && var1.cd <= 0) {
                     var1.cd = 40;
                     this.shoot(var1, var3);
                  }
               } else {
                  this.wanderAI(var1);
                  var12 = var1.moving;
                  var13 *= 0.6;
               }

               if (var1.cd > 0) {
                  var1.cd--;
               }
               break;
            case 6:
               if (var1.ignited) {
                  // Update 3.9: lit with flint and steel - it stops, hisses and goes off after the normal fuse, whatever the distance
                  var1.fuse++;
                  var12 = false;
                  if (var15) {
                     var1.dirYaw = angleTo(var16, var18);
                     var26 = var1.dirYaw;
                     var27 = (float)(-Math.toDegrees(Math.atan2(var20, var22)));
                  }
               } else if (var15 && var24 < 18.0) {
                  var1.hasTarget = true;
                  var1.dirYaw = angleTo(var16, var18);
                  var26 = var1.dirYaw;
                  var27 = (float)(-Math.toDegrees(Math.atan2(var20, var22)));
                  if (!(var24 < 3.0) && (var1.fuse <= 0 || !(var24 < 7.0))) {
                     if (var1.fuse > 0) {
                        var1.fuse--;
                     }

                     var12 = true;
                  } else {
                     var1.fuse++;
                     var12 = false;
                  }
               } else {
                  if (var1.fuse > 0) {
                     var1.fuse--;
                  }

                  this.wanderAI(var1);
                  var12 = var1.moving;
                  var13 *= 0.6;
               }

               var1.swell = var1.fuse;
               if (var1.fuse >= 30) {
                  this.explode(var1);
                  return;
               }
               break;
            case 7:
               boolean var28 = (var4 < 0.5 || var1.angry > 0) && var15;
               if (var28 && var24 < 20.0) {
                  var1.hasTarget = true;
                  var1.dirYaw = angleTo(var16, var18);
                  var26 = var1.dirYaw;
                  var27 = (float)(-Math.toDegrees(Math.atan2(var20, var22)));
                  if (var22 > 1.2) {
                     var12 = true;
                  }

                  if (var1.onGround && var22 > 2.0 && var22 < 5.5 && this.rnd.nextInt(10) == 0 && Math.abs(var20) < 2.0) {
                     var1.vy = 0.4;
                     var1.vx = Math.cos(Math.toRadians(var1.dirYaw + 90.0F)) * 0.35;
                     var1.vz = Math.sin(Math.toRadians(var1.dirYaw + 90.0F)) * 0.35;
                  }

                  if (var22 < 1.7 && Math.abs(var20) < 1.5 && var1.cd <= 0) {
                     var1.cd = 20;
                     var1.attackAnim = 8;
                     this.g.damagePlayer(2, var1.x, var1.z);
                  }
               } else {
                  this.wanderAI(var1);
                  var12 = var1.moving;
                  var13 *= 0.6;
               }

               if (var1.cd > 0) {
                  var1.cd--;
               }
               break;
            case 8:
            case 9:
            case 10:
            case 11:
            case 12:
            case 15:
            case 16:
            case 17:
               // Update 3.12: the mobs of the Nether, the Enderman, the villagers and the iron golems, the parrots (Mobs312)
               Mobs312.Intent intent312 = Mobs312.think(this, var1, var2, var3, var4);
               var12 = intent312.moving;
               var13 *= intent312.speed;
               if (!Float.isNaN(intent312.headYaw)) {
                  var26 = intent312.headYaw;
               }

               if (!Float.isNaN(intent312.headPitch)) {
                  var27 = intent312.headPitch;
               }
               break;
            default:
               Mobs.Mob mate = var1.flee <= 0 && var1.love > 0 ? this.findMate(var1) : null;
               if (var1.flee > 0) {
                  var1.flee--;
                  var1.dirYaw = angleTo(var1.x - var1.fleeX, var1.z - var1.fleeZ);
                  var12 = true;
                  var13 *= 1.9;
               } else if (mate != null) {
                  double var48 = mate.x - var1.x;
                  double var49 = mate.z - var1.z;
                  double var50 = Math.sqrt(var48 * var48 + var49 * var49);
                  var1.dirYaw = angleTo(var48, var49);
                  var26 = var1.dirYaw;
                  var12 = var50 > 0.9;
                  var13 *= 1.4;
                  if (var50 < 1.7 && Math.abs(mate.y - var1.y) < 1.5 && mate.love > 0) {
                     this.breed(var1, mate);
                  }
               } else if (var1.love <= 0 && this.tempted(var1, var3)) {
                  // Update 3.7: animals follow a player who holds their favourite food
                  var1.dirYaw = angleTo(var16, var18);
                  var26 = var1.dirYaw;
                  var27 = (float)(-Math.toDegrees(Math.atan2(var20, var22))) * 0.5F;
                  var12 = var22 > 2.2;
                  var13 *= 1.15;
               } else {
                  this.wanderAI(var1);
                  var12 = var1.moving;
                  if (!var12 && this.nearPlayer(var1, var3, 6.0) && this.rnd.nextInt(4) != 0) {
                     var26 = angleTo(var16, var18);
                     var27 = (float)(-Math.toDegrees(Math.atan2(var20, var22))) * 0.5F;
                  }
               }
         }

         if (var12) {
            double var41 = Math.toRadians(var1.dirYaw);
            var8 = -Math.sin(var41) * var13;
            var10 = Math.cos(var41) * var13;
            float targetBodyYaw = SkyGen.isMinigameBot(var1) && var1.hasTarget
               ? var26 + Math.max(-38.0F, Math.min(38.0F, wrap(var1.dirYaw - var26)))
               : var1.dirYaw;
            float var30 = wrap(targetBodyYaw - var1.yaw);
            float maxTurn = SkyGen.isMinigameBot(var1) ? 32.0F : 25.0F;
            var1.yaw = var1.yaw + Math.max(-maxTurn, Math.min(maxTurn, var30));
         }

         if (var1.hasTarget && !var12) {
            float maxIdleTurn = SkyGen.isMinigameBot(var1) ? 24.0F : 10.0F;
            float targetIdleYaw = SkyGen.isMinigameBot(var1) ? var26 : var1.dirYaw;
            var1.yaw = var1.yaw + Math.max(-maxIdleTurn, Math.min(maxIdleTurn, wrap(targetIdleYaw - var1.yaw)));
         }

         float var42 = wrap(var26 - var1.yaw);
         var42 = Math.max(-55.0F, Math.min(55.0F, var42));
         float headLerp = SkyGen.isMinigameBot(var1) ? 0.58F : 0.4F;
         var1.headYaw = var1.headYaw + (var1.yaw + var42 - var1.headYaw) * headLerp;
         var1.headPitch = var1.headPitch + (var27 - var1.headPitch) * headLerp;
         if (var12 && !var1.hasTarget && var1.flee <= 0 && !Mobs312.flies(var1.type) && !SkyGen.isMinigameBot(var1)) {
            double var29 = var1.x + var8 * 8.0;
            double var31 = var1.z + var10 * 8.0;
            int var33 = (int)Math.floor(var1.y);
            boolean var34 = false;

            for (int var35 = 0; var35 < 4; var35++) {
               int var36 = var2.getBlock((int)Math.floor(var29), var33 - var35, (int)Math.floor(var31));
               if (Blocks.isLava(var36)) {
                  break;
               }

               if (Blocks.coll[var36] != null && var36 != 0 || Blocks.isWater(var36)) {
                  var34 = true;
                  break;
               }
            }

            if (!var34) {
               var10 = 0.0;
               var8 = 0.0;
               var12 = false;
               var1.moving = false;
               var1.wander = 0;
            }
         }

         double var44 = var1.onGround ? 0.4 : 0.06;
         var1.vx = var1.vx + (var8 - var1.vx) * var44;
         var1.vz = var1.vz + (var10 - var1.vz) * var44;
         if (Mobs312.flies(var1.type)) {
            var1.vy += (this.flyVy - var1.vy) * 0.15;
         } else {
            var1.vy -= 0.08;
            var1.vy *= 0.98;
         }

         if (var1.inWater) {
            var1.vy *= 0.8;
            var1.vy += 0.045;
            var1.vx *= 0.8;
            var1.vz *= 0.8;
         }

         if (var1.type == 3 && var1.vy < -0.1) {
            var1.vy = -0.1;
         }

         double var45 = var1.vx;
         double var46 = var1.vz;
         double var47 = var1.vy;
         Phys.move(var2, var1.bb, var1.vx, var1.vy, var1.vz, this.res);
         var1.onGround = var47 < 0.0 && this.res[1] > var47 + 1.0E-9;
         boolean var37 = Math.abs(this.res[0] - var45) > 1.0E-6 || Math.abs(this.res[2] - var46) > 1.0E-6;
         if (var1.onGround || this.res[1] != var47) {
            var1.vy = 0.0;
         }

         if (Math.abs(this.res[0] - var45) > 1.0E-6) {
            var1.vx = 0.0;
         }

         if (Math.abs(this.res[2] - var46) > 1.0E-6) {
            var1.vz = 0.0;
         }

         var1.x = (var1.bb[0] + var1.bb[3]) / 2.0;
         var1.y = var1.bb[1];
         var1.z = (var1.bb[2] + var1.bb[5]) / 2.0;
         if (var37 && var12 && var1.onGround) {
            var1.vy = JUMP;
         }

         double var38 = Math.hypot(var1.x - var1.px, var1.z - var1.pz);
         if (SkyGen.isMinigameBot(var1)) {
            var1.walkAmp = var1.walkAmp + (Math.min(1.15F, (float)(var38 * 9.5)) - var1.walkAmp) * 0.48F;
            var1.walk += (float)var38 * 4.8F;
         } else {
            var1.walkAmp = var1.walkAmp + (Math.min(1.0F, (float)(var38 * 8.0)) - var1.walkAmp) * 0.4F;
            var1.walk += (float)var38 * 4.0F;
         }
      }
   }

   /** The player holds the food this animal likes and is within 9 blocks: it walks over to have a look. */
   boolean tempted(Mobs.Mob m, Player p) {
      return !p.dead && !p.spectator && isBreedFood(m.type, this.g.heldId()) && this.nearPlayer(m, p, 9.0);
   }

   boolean nearPlayer(Mobs.Mob var1, Player var2, double var3) {
      double var5 = var1.x - var2.x;
      double var7 = var1.z - var2.z;
      return var5 * var5 + var7 * var7 < var3 * var3;
   }

   void wanderAI(Mobs.Mob var1) {
      if (--var1.wander <= 0) {
         if (this.rnd.nextInt(3) == 0) {
            var1.moving = false;
            var1.wander = 40 + this.rnd.nextInt(80);
         } else {
            var1.moving = true;
            var1.dirYaw = this.rnd.nextFloat() * 360.0F;
            var1.wander = 30 + this.rnd.nextInt(60);
         }
      }
   }

   boolean hasLOS(World var1, double var2, double var4, double var6, double var8, double var10, double var12) {
      double var14 = var8 - var2;
      double var16 = var10 - var4;
      double var18 = var12 - var6;
      double var20 = Math.sqrt(var14 * var14 + var16 * var16 + var18 * var18);
      if (var20 < 0.001) {
         return true;
      }

      Hit var22 = var1.raycast(var2, var4, var6, var14 / var20, var16 / var20, var18 / var20, var20);
      return var22 == null;
   }

   void shoot(Mobs.Mob var1, Player var2) {
      this.shootAtPos(var1, var2.x, var2.y + 1.2, var2.z);
   }

   void shootAtPos(Mobs.Mob var1, double tx, double ty, double tz) {
      Mobs.Arrow var3 = new Mobs.Arrow();
      var3.x = var1.x;
      var3.y = var1.y + 1.5;
      var3.z = var1.z;
      double var4 = tx - var3.x;
      double var6 = ty - var3.y;
      double var8 = tz - var3.z;
      double var10 = Math.sqrt(var4 * var4 + var8 * var8);
      var6 += var10 * 0.12;
      double var12 = Math.sqrt(var4 * var4 + var6 * var6 + var8 * var8);
      double var14 = 1.6;
      var3.vx = var4 / var12 * var14 + this.rnd.nextGaussian() * 0.04;
      var3.vy = var6 / var12 * var14 + this.rnd.nextGaussian() * 0.04;
      var3.vz = var8 / var12 * var14 + this.rnd.nextGaussian() * 0.04;
      var3.damage = 3 + this.rnd.nextInt(2);
      var3.px = var3.x;
      var3.py = var3.y;
      var3.pz = var3.z;
      this.arrows.add(var3);
      var1.attackAnim = 6;
      Net319.onSpawnArrow(this.g != null ? this.g.world : null, var3);
   }

   public void shootFromPlayer(double var1, double var3, double var5, double var7, double var9, double var11, float var13) {
      this.shootGunArrow(var1, var3, var5, var7, var9, var11, var13, Math.max(1, Math.round(var13 * 2.0F)), true);
   }

   public void shootGunArrow(double x, double y, double z, double dx, double dy, double dz, float speed, int damage, boolean fromPlayer) {
      Mobs.Arrow a = new Mobs.Arrow();
      a.x = x;
      a.y = y;
      a.z = z;
      a.px = x;
      a.py = y;
      a.pz = z;
      a.vx = dx * speed;
      a.vy = dy * speed;
      a.vz = dz * speed;
      a.fromPlayer = fromPlayer;
      a.damage = Math.max(1, damage);
      this.arrows.add(a);
      Net319.onSpawnArrow(this.g != null ? this.g.world : null, a);
   }

   boolean tickArrow(Mobs.Arrow var1, World var2, Player var3) {
      var1.px = var1.x;
      var1.py = var1.y;
      var1.pz = var1.z;
      var1.life++;
      if (var1.stuck) {
         return var1.life > 1200;
      }

      if (var1.life > 1200) {
         return true;
      }

      for (int var4 = 0; var4 < 4; var4++) {
         var1.x = var1.x + var1.vx / 4.0;
         var1.y = var1.y + var1.vy / 4.0;
         var1.z = var1.z + var1.vz / 4.0;
         int var5 = var2.getBlock((int)Math.floor(var1.x), (int)Math.floor(var1.y), (int)Math.floor(var1.z));
         if (var5 != 0 && Blocks.coll[var5] != null) {
            var1.stuck = true;
            var1.life = 1140;
            var1.vx = var1.vy = var1.vz = 0.0;
            return false;
         }

         if (var1.fromPlayer) {
            for (Mobs.Mob var7 : this.list) {
               if (!var7.dead
                  && var1.x > var7.bb[0] - 0.1
                  && var1.x < var7.bb[3] + 0.1
                  && var1.y > var7.bb[1] - 0.1
                  && var1.y < var7.bb[4] + 0.1
                  && var1.z > var7.bb[2] - 0.1
                  && var1.z < var7.bb[5] + 0.1) {
                  if (!var1.remote) {
                     if (var2.settings != null && var2.settings.type == WorldSettings.MURDER && SkyGen.isMinigameBot(var7)) {
                        SkyGen.onMurderArrowHitBot(this.g, var7, (int)var1.damage, var1.x - var1.vx, var1.z - var1.vz);
                     } else {
                        this.hurtMob(var7, (int)var1.damage, var1.x - var1.vx, var1.z - var1.vz, 0.4, true);
                     }
                  }
                  return true;
               }
            }
            if (!var1.remote && Net319.isActive()) {
               if (Net319.tryArrowHitPeer(this.g, var1.x, var1.y, var1.z, (int)var1.damage, var1.x - var1.vx, var1.z - var1.vz)) {
                  return true;
               }
            }
         } else {
            if (this.targetable(var3)
               && var1.x > var3.bb[0]
               && var1.x < var3.bb[3]
               && var1.y > var3.bb[1]
               && var1.y < var3.bb[4]
               && var1.z > var3.bb[2]
               && var1.z < var3.bb[5]) {
               this.g.damagePlayer((int)var1.damage, var1.x - var1.vx, var1.z - var1.vz);
               return true;
            }
            if (var1.life > 3 && var2.settings != null && var2.settings.isMinigame()) {
               for (Mobs.Mob var7 : this.list) {
                  if (!var7.dead
                     && var1.x > var7.bb[0] - 0.1
                     && var1.x < var7.bb[3] + 0.1
                     && var1.y > var7.bb[1] - 0.1
                     && var1.y < var7.bb[4] + 0.1
                     && var1.z > var7.bb[2] - 0.1
                     && var1.z < var7.bb[5] + 0.1) {
                     if (!var1.remote) {
                        this.hurtMob(var7, (int)var1.damage, var1.x - var1.vx, var1.z - var1.vz, 0.35, false);
                     }
                     return true;
                  }
               }
               if (!var1.remote && Net319.isActive()) {
                  if (Net319.tryArrowHitPeer(this.g, var1.x, var1.y, var1.z, (int)var1.damage, var1.x - var1.vx, var1.z - var1.vz)) {
                     return true;
                  }
               }
            }
         }
      }

      var1.vy -= 0.05;
      var1.vx *= 0.99;
      var1.vy *= 0.99;
      var1.vz *= 0.99;
      return false;
   }

   /** What an animal likes to be fed: pigs carrots/potatoes, cows and sheep wheat, chickens seeds. */
   public static boolean isBreedFood(int type, int item) {
      switch (type) {
         case 0:
            return item == 336 || item == 337;
         case 1:
         case 2:
            return item == 286;
         case 3:
            return item == 285;
         default:
            return false;
      }
   }

   /**
    * Right-click with an item: returns true if the animal accepted it as food. A grown-up that is not on cooldown
    * goes into love mode (30 s); a baby grows up faster.
    */
   public boolean feed(Mobs.Mob m, int item) {
      if (m.dead || hostile(m.type) || !isBreedFood(m.type, item)) {
         return false;
      }

      if (m.babyAge > 0) {
         m.babyAge = Math.max(1, m.babyAge - 2400);
         return true;
      }

      if (m.love > 0 || m.breedCd > 0) {
         return false;
      }

      m.love = 600;
      Net319.onClientInteractMob(m, "FEED", item);
      return true;
   }

   /** Another grown-up of the same kind that is also in love, within 10 blocks. */
   Mobs.Mob findMate(Mobs.Mob m) {
      Mobs.Mob best = null;
      double bestD = 100.0;

      for (Mobs.Mob o : this.list) {
         if (o != m && !o.dead && o.type == m.type && o.love > 0 && o.babyAge == 0) {
            double dx = o.x - m.x;
            double dz = o.z - m.z;
            double d = dx * dx + dz * dz;
            if (d < bestD) {
               bestD = d;
               best = o;
            }
         }
      }

      return best;
   }

   /** Two animals in love meet: a baby appears between them and both go on a 5 minute cooldown. */
   void breed(Mobs.Mob a, Mobs.Mob b) {
      a.love = 0;
      b.love = 0;
      a.breedCd = 6000;
      b.breedCd = 6000;
      Mobs.Mob baby = this.spawnBaby(a.type, (a.x + b.x) / 2.0, Math.max(a.y, b.y), (a.z + b.z) / 2.0);
      if (a.type == 2) {
         baby.color = this.rnd.nextBoolean() ? a.color : b.color;
      }

      this.g.loveParticles(a);
      this.g.loveParticles(b);
      this.g.adv.award(this.g, "husbandry/breed_an_animal"); // Update 3.12
   }

   public Mobs.Mob spawnBaby(int type, double x, double y, double z) {
      Mobs.Mob baby = this.spawn(type, x, y, z);
      baby.babyAge = 24000;
      baby.setPos(x, y, z);
      return baby;
   }

   /** Shears on a grown-up, woolly sheep: 1-3 wool of its colour; the wool regrows after 4-8 minutes. */
   public boolean shear(Mobs.Mob m) {
      if (m.type != 2 || m.sheared || m.babyAge > 0 || m.dead) {
         return false;
      }

      m.sheared = true;
      m.woolTimer = 4800 + this.rnd.nextInt(4800);
      if (!Net319.isClientConnected() || Net319.isHosting()) {
         this.g.world.dropItem(m.x, m.y + 0.6, m.z, 34 + m.color, 1 + this.rnd.nextInt(3));
      } else {
         Net319.onClientInteractMob(m, "SHEAR", 0);
      }
      return true;
   }

   public boolean hurtMob(Mobs.Mob var1, int var2, double var3, double var5, double var7, boolean var9) {
      if (!var1.dead && var1.invuln <= 0) {
         if (var9 && Net319.isClientConnected() && !Net319.isHosting() && var1.remoteSynced) {
            var1.hurtTime = 10;
            var1.invuln = 8;
            Net319.onClientHitMob(var1, var2, var3, var5, var7);
            return true;
         }
         var1.health -= var2;
         var1.hurtTime = 10;
         var1.invuln = 10;
         double var10 = var1.x - var3;
         double var12 = var1.z - var5;
         double var14 = Math.sqrt(var10 * var10 + var12 * var12);
         if (var7 > 0.0 && var14 > 1.0E-4) {
            var1.vx = var10 / var14 * var7;
            var1.vz = var12 / var14 * var7;
            if (var1.onGround) {
               var1.vy = 0.36;
            }
         }

         if (!hostile(var1.type)) {
            var1.flee = 60;
            var1.fleeX = var3;
            var1.fleeZ = var5;
         } else if (var9) {
            var1.angry = 400;
            Mobs312.angerNeighbours(this, var1);
         }

         Mobs312.hurt(this, var1, var2, var9);

         if (var1.health <= 0) {
            this.kill(var1);
            if (var9) {
               this.g.adv.killed(this.g, var1); // Update 3.12: advancements
               if (this.g.world != null && this.g.world.settings != null && this.g.world.settings.isMinigame() && hostile(var1.type)) {
                  SkyGen.onArenaKill(this.g, SkyGen.botDisplayName(var1));
               }
            }
         }

         return true;
      } else {
         return false;
      }
   }

   void kill(Mobs.Mob var1) {
      var1.dead = true;
      var1.deathTime = 0;
      if (SkyGen.isMinigameBot(var1)) {
         SkyGen.onBotKilled(this.g, var1);
      }
      World var2 = this.g.world;
      boolean var3 = var1.burn > 0;
      if (var1.babyAge > 0) {
         return;
      }

      switch (var1.type) {
         case 0:
            this.drop(var2, var1, var3 ? 273 : 272, 1 + this.rnd.nextInt(3));
            break;
         case 1:
            this.drop(var2, var1, 261, this.rnd.nextInt(3));
            this.drop(var2, var1, var3 ? 275 : 274, 1 + this.rnd.nextInt(3));
            break;
         case 2:
            if (!var1.sheared) {
               this.drop(var2, var1, 34 + var1.color, 1);
            }

            this.drop(var2, var1, var3 ? 279 : 278, 1 + this.rnd.nextInt(2));
            break;
         case 3:
            this.drop(var2, var1, 262, this.rnd.nextInt(3));
            this.drop(var2, var1, var3 ? 277 : 276, 1);
            break;
         case 4:
            this.drop(var2, var1, 280, this.rnd.nextInt(3));
            if (this.rnd.nextInt(40) == 0) {
               this.drop(var2, var1, this.rnd.nextBoolean() ? 336 : 337, 1);
            }
            break;
         case 5:
            this.drop(var2, var1, 264, this.rnd.nextInt(3));
            this.drop(var2, var1, 267, this.rnd.nextInt(3));
            break;
         case 6:
            this.drop(var2, var1, 265, this.rnd.nextInt(3));
            break;
         case 7:
            this.drop(var2, var1, 263, this.rnd.nextInt(3));
            break;
         default:
            Mobs312.kill(this, var1, var2);
      }
   }

   void drop(World var1, Mobs.Mob var2, int var3, int var4) {
      if (var4 > 0) {
         var1.dropItem(var2.x, var2.y + 0.4, var2.z, var3, var4);
      }
   }

   void explode(Mobs.Mob var1) {
      this.blast(var1.x, var1.y + 0.8, var1.z, var1.charged ? 6 : 3, var1);
      var1.dead = true;
      var1.deathTime = 20;
   }

   void blast(double var1, double var3, double var5, int var7, Mobs.Mob var8) {
      World var9 = this.g.world;
      Player var10 = this.g.player;
      SoundEngine.play3DMoving(var9, "tnt_explode", var1, var3, var5, 0.0, 0.0, 0.0, 1.55F, 0.90F + this.rnd.nextFloat() * 0.12F, true);
      int var11 = (int)Math.floor(var1);
      int var12 = (int)Math.floor(var3);
      int var13 = (int)Math.floor(var5);

      for (int var14 = -var7; var14 <= var7; var14++) {
         for (int var15 = -var7; var15 <= var7; var15++) {
            for (int var16 = -var7; var16 <= var7; var16++) {
               double var17 = Math.sqrt(var14 * var14 + var15 * var15 + var16 * var16);
               if (!(var17 > var7 + 0.3) && (!(var17 > var7 - 1) || !(this.rnd.nextFloat() < 0.35 + (var17 - (var7 - 1)) * 0.3))) {
                  int var19 = var9.getBlock(var11 + var14, var12 + var15, var13 + var16);
                  if (var19 != 0 && Blocks.breakable(var19) && var19 != 26 && !Blocks.isFluid(var19)) {
                     if (var19 == 132) {
                        var9.setBlock(var11 + var14, var12 + var15, var13 + var16, 0);
                        var9.primeTnt(var11 + var14, var12 + var15, var13 + var16, 8 + this.rnd.nextInt(20));
                     } else {
                        if (var19 == 69 || var19 == 70 || var19 == 67 || var19 == 68) {
                           var9.removeTile(var11 + var14, var12 + var15, var13 + var16, true);
                        } else if (this.rnd.nextInt(4) == 0 && Blocks.drop[var19] != 0) {
                           var9.dropItem(var11 + var14 + 0.5, var12 + var15 + 0.5, var13 + var16 + 0.5, Blocks.drop[var19], 1);
                        }

                        var9.setBlock(var11 + var14, var12 + var15, var13 + var16, 0);
                     }
                  }
               }
            }
         }
      }

      this.g.explosionParticles(var1, var3, var5);
      Net319.onExplosion(var9, var1, var3, var5, var7);
      double var25 = 2 * var7 + 1;
      if (this.targetable(var10)) {
         double var26 = var10.x - var1;
         double var18 = var10.y + 0.9 - var3;
         double var20 = var10.z - var5;
         double var22 = Math.sqrt(var26 * var26 + var18 * var18 + var20 * var20);
         if (var22 < var25) {
            int var24 = (int)(1.0 + 18.0 * Math.pow(1.0 - var22 / var25, 1.2));
            var10.invuln = 0;
            this.g.damagePlayer(var24, var1, var5);
         }
      }

      for (Mobs.Mob var28 : this.list) {
         if (var28 != var8 && !var28.dead) {
            double var29 = Math.sqrt((var28.x - var1) * (var28.x - var1) + (var28.z - var5) * (var28.z - var5));
            if (var29 < var25 - 1.0) {
               this.hurtMob(var28, (int)(1.0 + 14.0 * (1.0 - var29 / (var25 - 1.0))), var1, var5, 0.8, false);
            }
         }
      }
   }

   public Mobs.Mob pick(double var1, double var3, double var5, double var7, double var9, double var11, double var13, double[] var15) {
      Mobs.Mob var16 = null;
      double var17 = var13;

      for (Mobs.Mob var20 : this.list) {
         if (!var20.dead && !Mobs312.onShoulder(var20)) {
            double[] var21 = var20.bb;
            double var22 = 0.0;
            double var24 = var17;
            double[] var26 = new double[]{var1, var3, var5};
            double[] var27 = new double[]{var7, var9, var11};
            double[] var28 = new double[]{var21[0] - 0.1, var21[1] - 0.05, var21[2] - 0.1};
            double[] var29 = new double[]{var21[3] + 0.1, var21[4] + 0.05, var21[5] + 0.1};
            boolean var30 = true;

            for (int var31 = 0; var31 < 3 && var30; var31++) {
               if (Math.abs(var27[var31]) < 1.0E-9) {
                  if (var26[var31] < var28[var31] || var26[var31] > var29[var31]) {
                     var30 = false;
                  }
               } else {
                  double var32 = (var28[var31] - var26[var31]) / var27[var31];
                  double var34 = (var29[var31] - var26[var31]) / var27[var31];
                  if (var32 > var34) {
                     double var36 = var32;
                     var32 = var34;
                     var34 = var36;
                  }

                  var22 = Math.max(var22, var32);
                  var24 = Math.min(var24, var34);
                  if (var22 > var24) {
                     var30 = false;
                  }
               }
            }

            if (var30 && var22 < var17) {
               var17 = var22;
               var16 = var20;
            }
         }
      }

      if (var16 != null) {
         var15[0] = var17;
      }

      return var16;
   }

   void tickSpawners(World var1, Player var2) {
      int var3 = (int)Math.floor(var2.x);
      int var4 = (int)Math.floor(var2.y);
      int var5 = (int)Math.floor(var2.z);

      for (int var6 = var3 - 13; var6 <= var3 + 13; var6++) {
         for (int var7 = var5 - 13; var7 <= var5 + 13; var7++) {
            if (var1.isLoaded(var6, var7)) {
               for (int var8 = Math.max(1, var4 - 8); var8 <= var4 + 8; var8++) {
                  if (var1.getBlock(var6, var8, var7) == 71) {
                     long var9 = var6 * 73856093L ^ var8 * 19349663L ^ var7 * 83492791L;
                     int var11 = (Integer)this.spawnerCd.getOrDefault(var9, 40);
                     if (var11 > 0) {
                        this.spawnerCd.put(var9, var11 - 20);
                     } else {
                        this.spawnerCd.put(var9, 200 + this.rnd.nextInt(400));
                        int var12 = 0;

                        for (Mobs.Mob var14 : this.list) {
                           if (!var14.dead
                              && hostile(var14.type)
                              && Math.abs(var14.x - var6) < 8.0
                              && Math.abs(var14.z - var7) < 8.0
                              && Math.abs(var14.y - var8) < 4.0) {
                              var12++;
                           }
                        }

                        if (var12 < 6) {
                           int var22 = (int)(Math.abs(var9) % 3L);
                           var22 = var22 == 0 ? 4 : (var22 == 1 ? 5 : 7);
                           if (var1.dim == Dim.NETHER) {
                              var22 = BLAZE; // the spawners of a nether fortress make blazes
                           }
                           int var24 = 1 + this.rnd.nextInt(3);

                           for (int var15 = 0; var15 < var24; var15++) {
                              double var16 = var6 + 0.5 + this.rnd.nextInt(7) - 3.0;
                              double var18 = var7 + 0.5 + this.rnd.nextInt(7) - 3.0;
                              int var20 = var8;
                              if (var1.getBlock((int)Math.floor(var16), var20, (int)Math.floor(var18)) == 0
                                 && var1.getBlock((int)Math.floor(var16), var20 + 1, (int)Math.floor(var18)) == 0) {
                                 Mobs.Mob var21 = this.spawn(var22, var16, var20, var18);
                                 if (Phys.collides(var1, var21.bb)) {
                                    this.list.remove(var21);
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   void spawnHostile(World var1, Player var2, float var3) {
      if (var1 != null && var1.gen instanceof BackroomsGen) {
         return;
      }
      if (!var2.dead) {
         if (var2.gameMode == 3) {
         }

         if (this.count(true) < 40) {
            for (int var4 = 0; var4 < 8; var4++) {
               double var5 = this.rnd.nextDouble() * Math.PI * 2.0;
               double var7 = 24.0 + this.rnd.nextDouble() * 22.0;
               int var9 = (int)Math.floor(var2.x + Math.cos(var5) * var7);
               int var10 = (int)Math.floor(var2.z + Math.sin(var5) * var7);
               if (var1.isLoaded(var9, var10)) {
                  int var11 = (int)var2.y + this.rnd.nextInt(34) - 22;
                  var11 = Math.max(2, Math.min(200, var11));
                  int var12 = -1;

                  for (int var13 = 0; var13 < 20; var13++) {
                     int var14 = var11 - var13;
                     if (var14 < 1) {
                        break;
                     }

                     int var15 = var1.getBlock(var9, var14 - 1, var10);
                     if (Blocks.coll[var15] != null
                        && var15 != 0
                        && !Blocks.isFluid(var15)
                        && var1.getBlock(var9, var14, var10) == 0
                        && var1.getBlock(var9, var14 + 1, var10) == 0) {
                        var12 = var14;
                        break;
                     }
                  }

                  if (var12 >= 0) {
                     int var26 = var1.getBlock(var9, var12 - 1, var10);
                     if (Blocks.solid[var26] && !Blocks.isLeaves(var26) && var26 != 28) {
                        int var27 = var1.getSky(var9, var12, var10) - (int)((1.0F - var3) * 11.0F);
                        int var28 = var1.getBlk(var9, var12, var10);
                        if (Math.max(var27, var28) <= 7) {
                           int var16 = this.rnd.nextInt(100);
                           int var17 = var16 < 38 ? 4 : (var16 < 66 ? 5 : (var16 < 92 ? 6 : 7));
                           if (Gen.extras && var16 >= 97) {
                              var17 = ENDERMAN; // Update 3.12: three in a hundred are tall, dark and teleporting
                           }

                           int var18 = var17 != 4 && var17 != 5 ? 1 : 1 + this.rnd.nextInt(2);

                           for (int var19 = 0; var19 < var18; var19++) {
                              double var20 = var9 + 0.5 + (var19 > 0 ? this.rnd.nextInt(3) - 1 : 0) * 0.8;
                              double var22 = var10 + 0.5 + (var19 > 0 ? this.rnd.nextInt(3) - 1 : 0) * 0.8;
                              Mobs.Mob var24 = this.spawn(var17, var20, var12, var22);
                              if (Phys.collides(var1, var24.bb)) {
                                 this.list.remove(var24);
                              }
                           }

                           return;
                        }
                     }
                  }
               }
            }
         }
      }
   }

   void spawnPassive(World var1, Player var2) {
      if (var1 != null && var1.gen instanceof BackroomsGen) {
         return;
      }
      if (this.count(false) < 28) {
         for (int var3 = 0; var3 < 6; var3++) {
            double var4 = this.rnd.nextDouble() * Math.PI * 2.0;
            double var6 = 14.0 + this.rnd.nextDouble() * 40.0;
            int var8 = (int)Math.floor(var2.x + Math.cos(var4) * var6);
            int var9 = (int)Math.floor(var2.z + Math.sin(var4) * var6);
            if (var1.isLoaded(var8, var9)) {
               int var10 = -1;

               for (int var11 = 200; var11 > 40; var11--) {
                  int var12 = var1.getBlock(var8, var11, var9);
                  if (var12 != 0 && (!Blocks.replaceable[var12] || Blocks.isFluid(var12))) {
                     if (var12 == 2 && var1.getBlock(var8, var11 + 1, var9) == 0 || var12 == 2 && var1.getBlock(var8, var11 + 1, var9) == 29) {
                        var10 = var11 + 1;
                     }
                     break;
                  }
               }

               if (var10 >= 0 && var1.getSky(var8, var10, var9) >= 12) {
                  int var19 = this.rnd.nextInt(100);
                  int var20 = var19 < 25 ? 0 : (var19 < 50 ? 1 : (var19 < 80 ? 2 : 3));
                  int var13 = 2 + this.rnd.nextInt(3);

                  for (int var14 = 0; var14 < var13; var14++) {
                     int var15 = var8 + this.rnd.nextInt(5) - 2;
                     int var16 = var9 + this.rnd.nextInt(5) - 2;
                     if (var1.isLoaded(var15, var16)) {
                        int var17 = -1;

                        for (int var18 = var10 + 3; var18 > var10 - 4; var18--) {
                           if (var1.getBlock(var15, var18, var16) == 0 && var1.getBlock(var15, var18 - 1, var16) == 2) {
                              var17 = var18;
                              break;
                           }
                        }

                        if (var17 >= 0) {
                           Mobs.Mob var21 = this.spawn(var20, var15 + 0.5, var17, var16 + 0.5);
                           if (Phys.collides(var1, var21.bb)) {
                              this.list.remove(var21);
                           }
                        }
                     }
                  }

                  return;
               }
            }
         }
      }
   }

   public void save(File var1) {
      if (var1 == null) {
         return;
      }

      try {
         PrintWriter var2 = new PrintWriter(new FileWriter(new File(var1, "mobs.dat")));

         try {
            for (Mobs.Mob var4 : this.list) {
               if (!var4.dead && (Mobs312.keepsForever(var4.type) || var4.tamed || !hostile(var4.type) && Math.abs(var4.x - this.g.player.x) < 120.0 && Math.abs(var4.z - this.g.player.z) < 120.0)) {
                  var2.println(var4.type + " " + var4.x + " " + var4.y + " " + var4.z + " " + var4.health + " " + var4.color + " " + var4.babyAge + " " + (var4.sheared ? 1 : 0) + " " + var4.woolTimer + " " + var4.breedCd + " " + (var4.tamed ? 1 : 0));
               }
            }
         } catch (Throwable var6) {
            try {
               var2.close();
            } catch (Throwable var5) {
               var6.addSuppressed(var5);
            }

            throw var6;
         }

         var2.close();
      } catch (IOException var7) {
         var7.printStackTrace();
      }
   }

   public void load(File var1) {
      if (var1 == null) {
         return;
      }

      File var2 = new File(var1, "mobs.dat");
      if (var2.exists()) {
         try {
            BufferedReader var3 = new BufferedReader(new FileReader(var2));

            String var4;
            try {
               while ((var4 = var3.readLine()) != null) {
                  String[] var5 = var4.trim().split(" ");
                  if (var5.length >= 6) {
                     Mobs.Mob var6 = this.spawn(
                        Integer.parseInt(var5[0]), Double.parseDouble(var5[1]), Double.parseDouble(var5[2]), Double.parseDouble(var5[3])
                     );
                     var6.health = Integer.parseInt(var5[4]);
                     var6.color = Integer.parseInt(var5[5]);
                     if (var5.length >= 10) {
                        var6.babyAge = Integer.parseInt(var5[6]);
                        var6.sheared = Integer.parseInt(var5[7]) != 0;
                        var6.woolTimer = Integer.parseInt(var5[8]);
                        var6.breedCd = Integer.parseInt(var5[9]);
                        var6.setPos(var6.x, var6.y, var6.z);
                     }

                     if (var5.length >= 11) {
                        var6.tamed = Integer.parseInt(var5[10]) != 0;
                     }
                  }
               }
            } catch (Throwable var8) {
               try {
                  var3.close();
               } catch (Throwable var7) {
                  var8.addSuppressed(var7);
               }

               throw var8;
            }

            var3.close();
         } catch (Exception var9) {
            var9.printStackTrace();
         }
      }
   }

   public static final class Arrow {
      public double x;
      public double y;
      public double z;
      public double vx;
      public double vy;
      public double vz;
      public double px;
      public double py;
      public double pz;
      public boolean fromPlayer;
      public boolean remote;
      public boolean stuck;
      public int life;
      public float damage;
   }

   public static final class Mob {
      private static int seedCounter;
      /** Update 3.17: a number of its own for the little random things the animation does (blinking, ear flicks ...). */
      public final int seed = (seedCounter++ * 40503 + 17) & 0x7FFFFFFF;
      public int netId;
      public boolean remoteSynced;
      public int type;
      public int health;
      public int hurtTime;
      public int invuln;
      public int deathTime;
      public int age;
      public int wander;
      public int fuse;
      public int cd;
      public int flee;
      public int burn;
      /** Update 3.17 test hook (bc.still): the mob does not think or move, its clips still play. */
      public boolean still;
      public int color;
      public int angry;
      public int attackAnim;
      /** Update 3.12: a free number for the mob's own mind (an End crystal: which pillar it sits on) and the phase of a boss. */
      public int tag;
      public String teamTag;
      public int phase;
      public int phaseT;
      public int tpCd;
      public double hx;
      public double hy;
      public double hz;
      public double tx;
      public double ty;
      public double tz;
      public float aux;
      public float rad;
      
      public int strafeDir = 1;
      public double x;
      public double y;
      public double z;
      public double vx;
      public double vy;
      public double vz;
      public double px;
      public double py;
      public double pz;
      public float yaw;
      public float pyaw;
      public float headYaw;
      public float pheadYaw;
      public float headPitch;
      public float pheadPitch;
      public float walk;
      public float pwalk;
      public float walkAmp;
      public float pwalkAmp;
      public float dirYaw;
      public boolean onGround;
      public boolean inWater;
      public boolean dead;
      public boolean moving;
      public boolean hasTarget;
      public double fleeX;
      public double fleeZ;
      public final double[] bb = new double[6];
      public int swell;
      public int pswell;
      /** Update 3.8: a creeper that was struck by lightning explodes with twice the power and glows blue. */
      public boolean charged;
      /** Update 3.9: set alight with flint and steel (creepers only): the fuse runs down by itself */
      public boolean ignited;
      /** Ticks until this animal is grown up (0 = adult). Babies are half size and cannot breed. */
      public int babyAge;
      /** > 0: in love (was fed), looks for a partner of the same kind. */
      public int love;
      /** > 0: cannot breed yet. */
      public int breedCd;
      public boolean sheared;
      /** Update 3.12: a parrot that has been tamed (it stays with you, sits on your shoulder, and is never lost). */
      public boolean tamed;
      /** Ticks until a sheared sheep has regrown its wool. */
      public int woolTimer;
      /** Ticks until a (grown-up) chicken lays its next egg; 0 = not started yet. */
      public int eggTimer;
      /** Update 3.12: magma cubes: 1 small .. 3 large (the size is also the scale of the model and of the box). */
      public int size;
      public float scale = 1.0F;
      /** Update 3.12: a little data of a mob that has more than a type (the profession of a villager, the colour of a parrot ...). */
      public int variant;

      public boolean isBaby() {
         return this.babyAge > 0;
      }

      void setPos(double var1, double var3, double var5) {
         this.x = var1;
         this.y = var3;
         this.z = var5;
         float var8 = (this.babyAge > 0 ? 0.5F : 1.0F) * this.scale;
         float var7 = Mobs.W[this.type] * var8 / 2.0F;
         this.bb[0] = this.x - var7;
         this.bb[1] = this.y;
         this.bb[2] = this.z - var7;
         this.bb[3] = this.x + var7;
         this.bb[4] = this.y + Mobs.H[this.type] * var8;
         this.bb[5] = this.z + var7;
      }
   }
}
