package bc;

public final class Player {
   public double x;
   public double y;
   public double z;
   public double px;
   public double py;
   public double pz;
   public double vx;
   public double vy;
   public double vz;
   public float yaw;
   public float pitch;
   public float pyaw;
   public float ppitch;
   public boolean onGround;
   public boolean flying;
   public boolean sprinting;
   public boolean sneaking;
   public boolean inWater;
   public boolean inLava;
   /** ticks left on fire (Update 3.7): 1 damage per second, put out by water */
   public int burn;
   public boolean eyeInWater;
   public boolean collidedH;
   public boolean creative = true;
   public boolean spectator;
   public boolean adventure;
   public boolean hardcore;
   public int gameMode = 1;
   public int food = 20;
   public float sat = 5.0F;
   public float exh;
   public int foodTimer;
   public int starveTimer;
   public float fovMod = 1.0F;
   public float fovModPrev = 1.0F;
   public boolean hungerBlocked;
   public float sprintDist;
   public int health = 20;
   public int air = 300;
   public float fallDistance;
   public int hurtTime;
   public int invuln;
   public int jumpDelay;
   public int regenTimer;
   public boolean dead;
   public float eye = 1.62F;
   public float peye = 1.62F;
   public final double[] bb = new double[6];
   private final double[] res = new double[3];
   public float flySpeed = 0.05F;
   public static final double W = 0.6;

   public void setPos(double var1, double var3, double var5) {
      this.x = this.px = var1;
      this.y = this.py = var3;
      this.z = this.pz = var5;
      this.vx = this.vy = this.vz = 0.0;
      this.updateBox();
   }

   public void setMode(int var1) {
      this.hardcore = var1 == 4;
      this.gameMode = var1 == 4 ? 0 : var1;
      this.creative = var1 == 1 || var1 == 3;
      this.spectator = var1 == 3;
      this.adventure = var1 == 2;
      if (this.spectator) {
         this.flying = true;
      } else if (var1 != 1) {
         this.flying = false;
      }

      if (var1 == 1 || var1 == 3) {
         this.food = 20;
         this.sat = 5.0F;
         this.burn = 0;
      }
   }

   public int rawMode() {
      return this.hardcore ? 4 : this.gameMode;
   }

   public String modeName() {
      if (this.hardcore) {
         return "Hardcore";
      }
      return switch (this.gameMode) {
         case 0 -> "Survival";
         case 1 -> "Creative";
         case 2 -> "Adventure";
         case 3 -> "Spectator";
         default -> "Survival";
      };
   }

   public void addExhaustion(float var1) {
      if (!this.creative) {
         if (this.adventure) {
         }

         this.exh += var1;
      }
   }

   public double height() {
      return this.sneaking ? 1.65 : 1.8;
   }

   void updateBox() {
      this.bb[0] = this.x - 0.3;
      this.bb[1] = this.y;
      this.bb[2] = this.z - 0.3;
      this.bb[3] = this.x + 0.3;
      this.bb[4] = this.y + this.height();
      this.bb[5] = this.z + 0.3;
   }

   public void tick(World var1, float var2, float var3, boolean var4, boolean var5, boolean var6) {
      this.px = this.x;
      this.py = this.y;
      this.pz = this.z;
      this.pyaw = this.yaw;
      this.ppitch = this.pitch;
      this.peye = this.eye;
      if (this.invuln > 0) {
         this.invuln--;
      }

      if (this.hurtTime > 0) {
         this.hurtTime--;
      }

      if (this.spectator) {
         this.tickSpectator(var2, var3, var4, var5, var6);
      } else {
         boolean var7 = this.sneaking;
         this.sneaking = var5 && !this.flying;
         if (this.sneaking != var7) {
            this.updateBox();
         }

         this.updateBox();
         this.inWater = this.fluidIn(var1, true);
         this.inLava = this.fluidIn(var1, false);
         this.eyeInWater = Blocks.isWater(var1.getBlock(fl(this.x), fl(this.y + this.eye), fl(this.z)));
         var2 *= 0.98F;
         var3 *= 0.98F;
         if (this.sneaking) {
            var2 *= 0.3F;
            var3 *= 0.3F;
         }

         this.hungerBlocked = !this.creative && this.food <= 6;
         if (var6 && var2 > 0.78400004F && !this.sneaking && !this.hungerBlocked) {
            this.sprinting = true;
         }

         if (var2 <= 0.0F || this.sneaking || this.collidedH || this.hungerBlocked) {
            this.sprinting = false;
         }

         if (this.creative && this.flying) {
         }

         if (this.jumpDelay > 0) {
            this.jumpDelay--;
         }

         double var8 = this.vy;
         if (this.flying) {
            if (var4) {
               this.vy = this.vy + this.flySpeed * 3.0F;
            }

            if (var5) {
               this.vy = this.vy - this.flySpeed * 3.0F;
            }

            var8 = this.vy;
         } else if (var4) {
            if (this.inWater || this.inLava) {
               this.vy += 0.04;
            } else if (this.onGround && this.jumpDelay == 0) {
               this.vy = 0.42;
               this.addExhaustion(this.sprinting ? 0.8F : 0.2F);
               if (this.sprinting) {
                  double var10 = Math.toRadians(this.yaw);
                  this.vx = this.vx - Math.sin(var10) * 0.2;
                  this.vz = this.vz + Math.cos(var10) * 0.2;
               }

               this.jumpDelay = 10;
            }
         } else {
            this.jumpDelay = 0;
         }

         boolean var15 = !this.flying && !this.creativeFlyingNoClip() && this.onLadder(var1);
         if (var15) {
            this.vx = Math.max(-0.15, Math.min(0.15, this.vx));
            this.vz = Math.max(-0.15, Math.min(0.15, this.vz));
            this.fallDistance = 0.0F;
            if (this.vy < -0.15) {
               this.vy = -0.15;
            }

            if (this.sneaking && this.vy < 0.0) {
               this.vy = 0.0;
            }
         }

         this.travel(var1, var3, var2);
         if (var15 && (this.collidedH || var4) && !this.sneaking) {
            this.vy = 0.2;
         }

         if (this.flying) {
            this.vy = var8 * 0.6;
            if (this.onGround && this.vy <= 0.0) {
               this.flying = false;
            }
         }

         float var11 = this.sneaking ? 1.54F : 1.62F;
         this.eye = this.eye + (var11 - this.eye) * 0.5F;
         if (!this.creative) {
            if (this.inLava && this.invuln == 0) {
               this.hurt(4);
            }

            if (this.inLava) {
               this.burn = 300;
            }

            int fireX = fl(this.x);
            int fireZ = fl(this.z);
            if (Blocks.isFire(var1.getBlock(fireX, fl(this.y + 0.1), fireZ)) || Blocks.isFire(var1.getBlock(fireX, fl(this.y + 0.9), fireZ))) {
               this.burn = Math.max(this.burn, 160);
               if (this.invuln == 0) {
                  this.hurt(1);
               }
            }

            // Update 3.12: a magma block burns the feet of whoever stands on it without sneaking (1 damage every half second)
            if (this.onGround && !this.sneaking && !this.flying && this.invuln == 0 && var1.getBlock(fireX, fl(this.y - 0.05), fireZ) == Blocks.MAGMA) {
               this.hurt(1);
            }

            if (this.burn > 0) {
               this.burn--;
               if (this.inWater) {
                  this.burn = 0;
               } else if (this.burn % 20 == 0) {
                  this.hurt(1);
               }
            }

            if (this.eyeInWater) {
               this.air--;
               if (this.air <= -20) {
                  this.air = 0;
                  this.hurt(2);
               }
            } else {
               this.air = Math.min(300, this.air + 4);
            }

            if (this.sprinting) {
               this.addExhaustion(0.1F * (float)Math.hypot(this.x - this.px, this.z - this.pz));
            } else if (this.inWater) {
               this.addExhaustion(0.015F * (float)Math.hypot(this.x - this.px, this.z - this.pz));
            }

            if (this.exh > 4.0F) {
               this.exh -= 4.0F;
               if (this.sat > 0.0F) {
                  this.sat = Math.max(0.0F, this.sat - 1.0F);
               } else {
                  this.food = Math.max(0, this.food - 1);
               }
            }

            if (this.food >= 20 && this.sat > 0.0F && this.health < 20 && this.health > 0) {
               if (++this.foodTimer >= 10) {
                  this.foodTimer = 0;
                  this.health++;
                  this.addExhaustion(Math.min(this.sat, 6.0F));
               }
            } else if (this.food >= 18 && this.health < 20 && this.health > 0) {
               if (++this.foodTimer >= 80) {
                  this.foodTimer = 0;
                  this.health++;
                  this.addExhaustion(6.0F);
               }
            } else if (this.food <= 0) {
               if (++this.foodTimer >= 80) {
                  this.foodTimer = 0;
                  if (this.health > 1) {
                     this.health--;
                     this.hurtTime = 10;
                  }
               }
            } else {
               this.foodTimer = 0;
            }
         } else {
            this.air = 300;
            this.burn = 0;
            if (this.creative) {
               this.food = 20;
               this.exh = 0.0F;
            }
         }

         float var12 = 1.0F;
         if (this.flying) {
            var12 *= 1.1F;
         }

         var12 *= ((this.sprinting ? 1.3F : 1.0F) + 1.0F) / 2.0F;
         this.fovModPrev = this.fovMod;
         this.fovMod = this.fovMod + (var12 - this.fovMod) * 0.5F;
         if (this.y < -64.0) {
            if (this.creative) {
               this.setPos(this.x, 200.0, this.z);
            } else {
               this.health = 0;
               this.dead = true;
            }
         }

         if (this.health <= 0) {
            this.dead = true;
         }
      }
   }

   public void hurt(int var1) {
      if (!this.creative && !this.dead && this.invuln <= 0) {
         this.addExhaustion(0.3F);
         this.health -= var1;
         this.invuln = 10;
         this.hurtTime = 10;
         if (this.health <= 0) {
            this.health = 0;
            this.dead = true;
         }
      }
   }

   static int fl(double var0) {
      return (int)Math.floor(var0);
   }

   boolean creativeFlyingNoClip() {
      return false;
   }

   boolean onLadder(World var1) {
      int var2 = fl(this.x);
      int var3 = fl(this.z);
      return Blocks.climbable(var1.getBlock(var2, fl(this.y + 0.05), var3)) || Blocks.climbable(var1.getBlock(var2, fl(this.y + 0.9), var3));
   }

   boolean fluidIn(World var1, boolean var2) {
      double var3 = var2 ? 0.001 : 0.1;
      int var5 = fl(this.bb[0] + var3);
      int var6 = fl(this.bb[3] - var3);
      int var7 = fl(this.bb[1] + 0.4);
      int var8 = fl(this.bb[4] - 0.4);
      int var9 = fl(this.bb[2] + var3);
      int var10 = fl(this.bb[5] - var3);

      for (int var11 = var7; var11 <= var8; var11++) {
         for (int var12 = var9; var12 <= var10; var12++) {
            for (int var13 = var5; var13 <= var6; var13++) {
               int var14 = var1.getBlock(var13, var11, var12);
               if (var2 ? Blocks.isWater(var14) : Blocks.isLava(var14)) {
                  double var15 = var11 + (Blocks.sameFluid(var1.getBlock(var13, var11 + 1, var12), var14) ? 1.0 : (8 - Blocks.fluidLevel(var14)) / 9.0);
                  if (this.bb[1] + 0.4 <= var15) {
                     return true;
                  }
               }
            }
         }
      }

      return false;
   }

   void moveFlying(float var1, float var2, float var3) {
      float var4 = var1 * var1 + var2 * var2;
      if (var4 >= 1.0E-4F) {
         var4 = (float)Math.sqrt(var4);
         if (var4 < 1.0F) {
            var4 = 1.0F;
         }

         var4 = var3 / var4;
         var1 *= var4;
         var2 *= var4;
         double var5 = Math.toRadians(this.yaw);
         float var7 = (float)Math.sin(var5);
         float var8 = (float)Math.cos(var5);
         this.vx += var1 * var8 - var2 * var7;
         this.vz += var2 * var8 + var1 * var7;
      }
   }

   void travel(World var1, float var2, float var3) {
      if (this.inWater && !this.flying) {
         double var8 = this.y;
         this.moveFlying(var2, var3, 0.02F);
         this.move(var1, this.vx, this.vy, this.vz);
         this.vx *= 0.8;
         this.vy *= 0.8;
         this.vz *= 0.8;
         this.vy -= 0.02;
         if (this.collidedH && this.freeAt(var1, this.vx, this.vy + 0.6 - this.y + var8, this.vz)) {
            this.vy = 0.3;
         }
      } else if (this.inLava && !this.flying) {
         double var7 = this.y;
         this.moveFlying(var2, var3, 0.02F);
         this.move(var1, this.vx, this.vy, this.vz);
         this.vx *= 0.5;
         this.vy *= 0.5;
         this.vz *= 0.5;
         this.vy -= 0.02;
         if (this.collidedH && this.freeAt(var1, this.vx, this.vy + 0.6 - this.y + var7, this.vz)) {
            this.vy = 0.3;
         }
      } else {
         float var4 = 0.91F;
         if (this.onGround) {
            var4 = 0.546F;
         }

         float var5 = 0.16277136F / (var4 * var4 * var4);
         float var6;
         if (this.flying) {
            var6 = this.flySpeed * (this.sprinting ? 2.0F : 1.0F);
         } else if (this.onGround) {
            var6 = (this.sprinting ? 0.13F : 0.1F) * var5;
         } else {
            var6 = this.sprinting ? 0.026F : 0.02F;
         }

         this.moveFlying(var2, var3, var6);
         this.move(var1, this.vx, this.vy, this.vz);
         if (!this.flying) {
            this.vy -= 0.08;
         }

         this.vy *= 0.98;
         this.vx *= var4;
         this.vz *= var4;
         // Update 3.12: soul sand drags (Minecraft: motion x 0.4 on every tick spent on it)
         if (this.onGround && !this.flying && var1.getBlock(fl(this.x), fl(this.y - 0.05), fl(this.z)) == Blocks.SOUL_SAND) {
            this.vx *= 0.4;
            this.vz *= 0.4;
         }
      }
   }

   boolean freeAt(World var1, double var2, double var4, double var6) {
      double[] var8 = new double[]{this.bb[0] + var2, this.bb[1] + var4, this.bb[2] + var6, this.bb[3] + var2, this.bb[4] + var4, this.bb[5] + var6};
      return !Phys.collides(var1, var8) && !this.fluidBox(var1, var8);
   }

   boolean fluidBox(World var1, double[] var2) {
      for (int var3 = fl(var2[1]); var3 <= fl(var2[4]); var3++) {
         for (int var4 = fl(var2[2]); var4 <= fl(var2[5]); var4++) {
            for (int var5 = fl(var2[0]); var5 <= fl(var2[3]); var5++) {
               if (Blocks.isFluid(var1.getBlock(var5, var3, var4))) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   boolean canFallAt(World var1, double var2, double var4) {
      double[] var6 = new double[]{this.bb[0] + var2, this.bb[1] - 0.6, this.bb[2] + var4, this.bb[3] + var2, this.bb[4] - 0.6, this.bb[5] + var4};
      var6[4] = this.bb[1] + 0.0;
      var6[1] = this.bb[1] - 0.6;
      return !Phys.collides(var1, var6);
   }

   void move(World var1, double var2, double var4, double var6) {
      boolean var8 = this.onGround;
      if (this.sneaking && this.onGround && !this.flying) {
         double var9 = 0.05;

         while (var2 != 0.0 && this.canFallAt(var1, var2, 0.0)) {
            if (var2 < var9 && var2 >= -var9) {
               var2 = 0.0;
            } else if (var2 > 0.0) {
               var2 -= var9;
            } else {
               var2 += var9;
            }
         }

         while (var6 != 0.0 && this.canFallAt(var1, 0.0, var6)) {
            if (var6 < var9 && var6 >= -var9) {
               var6 = 0.0;
            } else if (var6 > 0.0) {
               var6 -= var9;
            } else {
               var6 += var9;
            }
         }

         while (var2 != 0.0 && var6 != 0.0 && this.canFallAt(var1, var2, var6)) {
            if (var2 < var9 && var2 >= -var9) {
               var2 = 0.0;
            } else if (var2 > 0.0) {
               var2 -= var9;
            } else {
               var2 += var9;
            }

            if (var6 < var9 && var6 >= -var9) {
               var6 = 0.0;
            } else if (var6 > 0.0) {
               var6 -= var9;
            } else {
               var6 += var9;
            }
         }
      }

      double[] var28 = (double[])this.bb.clone();
      Phys.move(var1, this.bb, var2, var4, var6, this.res);
      double var10 = this.res[0];
      double var12 = this.res[1];
      double var14 = this.res[2];
      boolean var16 = var12 != var4;
      boolean var17 = var16 && var4 < 0.0;
      boolean var18 = var10 != var2 || var14 != var6;
      if ((var8 || var17) && var18 && !this.flying) {
         double[] var19 = (double[])this.bb.clone();
         System.arraycopy(var28, 0, this.bb, 0, 6);
         Phys.move(var1, this.bb, 0.0, 0.6, 0.0, this.res);
         double var20 = this.res[1];
         Phys.move(var1, this.bb, var2, 0.0, var6, this.res);
         double var22 = this.res[0];
         double var24 = this.res[2];
         Phys.move(var1, this.bb, 0.0, -var20, 0.0, this.res);
         double var26 = this.res[1];
         if (var22 * var22 + var24 * var24 > var10 * var10 + var14 * var14 + 1.0E-9) {
            var10 = var22;
            var14 = var24;
            var12 = var20 + var26;
            var17 = true;
            var16 = true;
         } else {
            System.arraycopy(var19, 0, this.bb, 0, 6);
         }
      }

      this.collidedH = var10 != var2 || var14 != var6;
      this.x = (this.bb[0] + this.bb[3]) / 2.0;
      this.y = this.bb[1];
      this.z = (this.bb[2] + this.bb[5]) / 2.0;
      this.onGround = var17;
      if (var10 != var2) {
         this.vx = 0.0;
      }

      if (var14 != var6) {
         this.vz = 0.0;
      }

      if (var16 && (var4 < 0.0 || var12 != var4)) {
         this.vy = 0.0;
      }

      if (this.inWater || this.inLava) {
         this.fallDistance = 0.0F;
      }

      if (this.onGround) {
         int landX = fl(this.x);
         int landY = fl(this.y - 0.001);
         int landZ = fl(this.z);
         int below = var1.getBlock(landX, landY, landZ);
         if (below == Redstone319.SLIME_BLOCK && !this.sneaking) {
            if (this.fallDistance > 1.0F && !this.flying) {
               this.vy = Math.min(1.15, Math.sqrt(this.fallDistance) * 0.28);
               this.onGround = false;
            }

            this.fallDistance = 0.0F;
         } else {
            if (this.fallDistance > 0.5F && !this.flying) {
               var1.trample(landX, landY, landZ, this.fallDistance);
            }

            if (this.fallDistance > 3.0F && !this.creative && !this.flying) {
               int var29 = (int)Math.ceil(this.fallDistance - 3.0F);
               if (below == Blocks.HAY_BALE) {
                  var29 = (int)Math.ceil(var29 * 0.2F);
               }

               if (var29 > 0) {
                  this.invuln = 0;
                  this.hurt(var29);
               }
            }

            this.fallDistance = 0.0F;
         }
      } else if (var12 < 0.0) {
         this.fallDistance -= (float)var12;
      }

      if (this.flying) {
         this.fallDistance = 0.0F;
      }
   }

   void tickSpectator(float var1, float var2, boolean var3, boolean var4, boolean var5) {
      this.sneaking = false;
      this.flying = true;
      this.sprinting = var5 && var1 > 0.0F;
      this.inWater = this.inLava = this.eyeInWater = false;
      if (var3) {
         this.vy = this.vy + this.flySpeed * 3.0F;
      }

      if (var4) {
         this.vy = this.vy - this.flySpeed * 3.0F;
      }

      this.moveFlying(var2 * 0.98F, var1 * 0.98F, this.flySpeed * (this.sprinting ? 2.2F : 1.4F));
      this.x = this.x + this.vx;
      this.y = this.y + this.vy;
      this.z = this.z + this.vz;
      this.vx *= 0.82;
      this.vz *= 0.82;
      this.vy *= 0.6;
      this.onGround = false;
      this.fallDistance = 0.0F;
      this.collidedH = false;
      this.updateBox();
      this.eye = 1.62F;
      this.peye = 1.62F;
      this.air = 300;
      this.fovModPrev = this.fovMod;
      this.fovMod = this.fovMod + ((this.sprinting ? 1.15F : 1.0F) * 1.1F - this.fovMod) * 0.5F;
      if (this.y < -64.0) {
         this.y = 120.0;
      }

      if (this.y > 400.0) {
         this.y = 400.0;
      }
   }

   public float ifov(float var1) {
      return this.fovModPrev + (this.fovMod - this.fovModPrev) * var1;
   }

   public double ix(float var1) {
      return this.px + (this.x - this.px) * var1;
   }

   public double iy(float var1) {
      return this.py + (this.y - this.py) * var1;
   }

   public double iz(float var1) {
      return this.pz + (this.z - this.pz) * var1;
   }

   public float ieye(float var1) {
      return this.peye + (this.eye - this.peye) * var1;
   }
}
