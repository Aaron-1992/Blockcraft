package bc;

import bc.Gen.Col;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public final class Structs {
   final Gen gen;
   final long seed;
   static final int VILLAGE_REGION = 20;
   static final int MINE_REGION = 8;
   static Chunk C;
   final ConcurrentHashMap<Long, Structs.Village> villageCache = new ConcurrentHashMap();
   final ConcurrentHashMap<Long, Structs.Shaft> shaftCache = new ConcurrentHashMap();

   public Structs(Gen var1, long var2) {
      this.gen = var1;
      this.seed = var2;
   }

   static void set(Chunk var0, int var1, int var2, int var3, int var4) {
      int var5 = var1 - var0.cx * 16;
      int var6 = var3 - var0.cz * 16;
      if (var5 >= 0 && var5 <= 15 && var6 >= 0 && var6 <= 15 && var2 >= 0 && var2 <= 255) {
         var0.blocks[Chunk.idx(var5, var2, var6)] = (short)var4;
      }
   }

   static int get(Chunk var0, int var1, int var2, int var3) {
      int var4 = var1 - var0.cx * 16;
      int var5 = var3 - var0.cz * 16;
      return var4 >= 0 && var4 <= 15 && var5 >= 0 && var5 <= 15 && var2 >= 0 && var2 <= 255 ? var0.blocks[Chunk.idx(var4, var2, var5)] & 0xFFFF : -1;
   }

   static boolean overlaps(Chunk var0, int var1, int var2, int var3, int var4) {
      return var3 >= var0.cx * 16 && var1 <= var0.cx * 16 + 15 && var4 >= var0.cz * 16 && var2 <= var0.cz * 16 + 15;
   }

   static void box(Chunk var0, int var1, int var2, int var3, int var4, int var5, int var6, int var7) {
      int var8 = Math.max(var1, var0.cx * 16);
      int var9 = Math.min(var4, var0.cx * 16 + 15);
      int var10 = Math.max(var3, var0.cz * 16);
      int var11 = Math.min(var6, var0.cz * 16 + 15);

      for (int var12 = Math.max(0, var2); var12 <= Math.min(255, var5); var12++) {
         for (int var13 = var10; var13 <= var11; var13++) {
            for (int var14 = var8; var14 <= var9; var14++) {
               var0.blocks[Chunk.idx(var14 - var0.cx * 16, var12, var13 - var0.cz * 16)] = (short)var7;
            }
         }
      }
   }

   static boolean soft(int var0) {
      return var0 == 0 || var0 == 29 || Blocks.isLeaves(var0) || Blocks.isFluid(var0) || Blocks.coll[var0] == null;
   }

   public void apply(Chunk var1) {
      try {
         this.villages(var1);
         Landmarks319.apply(this, var1);
         this.dungeons(var1);
         this.mineshafts(var1);
      } catch (RuntimeException var3) {
         var3.printStackTrace();
      }
   }

   Structs.Village village(int var1, int var2) {
      long var3 = (long)var1 << 32 ^ var2 & 4294967295L;
      Structs.Village var5 = (Structs.Village)this.villageCache.get(var3);
      if (var5 != null) {
         return var5;
      }

      var5 = this.planVillage(var1, var2);
      this.villageCache.put(var3, var5);
      return var5;
   }

   Structs.Village planVillage(int var1, int var2) {
      if (Gen.extras && this.gen.settings.structVer >= 2 && this.gen.settings.terra()) {
         return Village314.plan(this, var1, var2); // Update 3.14: streets, plots, buildings from templates, a palette per biome
      }

      Structs.Village var3 = new Structs.Village();
      Random var4 = new Random(this.seed * 6364136223846793005L + var1 * 1442695040888963407L + var2 * 987654321L + 12345L);
      if (var4.nextFloat() > 0.7F) {
         return var3;
      }

      int var5 = 320;
      int var6 = 0;
      int var7 = 0;
      int var8 = 0;
      boolean var9 = false;

      for (int var10 = 0; var10 < 12 && !var9; var10++) {
         var6 = var1 * var5 + 70 + var4.nextInt(var5 - 140);
         var7 = var2 * var5 + 70 + var4.nextInt(var5 - 140);
         Col var11 = this.gen.column(var6, var7);
         if (Biomes.village(var11.biome) && var11.h >= 65 && var11.h <= 90) {
            boolean var12 = true;

            for (int[] var16 : new int[][]{{12, 0}, {-12, 0}, {0, 12}, {0, -12}}) {
               Col var17 = this.gen.column(var6 + var16[0], var7 + var16[1]);
               if (Math.abs(var17.h - var11.h) > 5 || var17.h < 64) {
                  var12 = false;
               }
            }

            if (var12) {
               var9 = true;
               var8 = var11.h;
               var3.desert = var11.biome == 4;
            }
         }
      }

      if (!var9) {
         return var3;
      }

      var3.valid = true;
      var3.cx = var6;
      var3.cz = var7;
      var3.cy = var8;
      Structs.Bld var34 = new Structs.Bld();
      var34.type = 1;
      var34.x0 = var6 - 1;
      var34.z0 = var7 - 1;
      var34.w = 3;
      var34.d = 3;
      var34.y0 = var8;
      var34.desert = var3.desert;
      finish(var34);
      var3.blds.add(var34);
      int var35 = 6 + var4.nextInt(5);
      boolean var36 = false;

      for (int var37 = 0; var37 < var35 * 3 && var3.blds.size() < var35 + 1; var37++) {
         double var41 = var4.nextDouble() * Math.PI * 2.0;
         double var46 = 9.0 + var4.nextDouble() * 26.0;
         int var18 = var6 + (int)Math.round(Math.cos(var41) * var46);
         int var19 = var7 + (int)Math.round(Math.sin(var41) * var46);
         Structs.Bld var20 = new Structs.Bld();
         boolean var21 = var3.blds.size() == 3;
         var20.type = var21 ? 2 : 0;
         var20.desert = var3.desert;
         var20.w = var21 ? 5 : (var4.nextBoolean() ? 5 : 7);
         var20.d = var21 ? 9 : (var4.nextBoolean() ? 5 : 7);
         var20.wallH = var21 ? 6 : 3;
         int var22 = var6 - var18;
         int var23 = var7 - var19;
         if (Math.abs(var22) > Math.abs(var23)) {
            var20.dir = var22 > 0 ? 3 : 2;
         } else {
            var20.dir = var23 > 0 ? 1 : 0;
         }

         int var24 = var20.dir < 2 ? var20.w : var20.d;
         int var25 = var20.dir < 2 ? var20.d : var20.w;
         var20.x0 = var18 - var24 / 2;
         var20.z0 = var19 - var25 / 2;
         finish(var20);
         boolean var26 = false;

         for (Structs.Bld var28 : var3.blds) {
            if (var20.bx1 + 3 >= var28.bx0 && var20.bx0 - 3 <= var28.bx1 && var20.bz1 + 3 >= var28.bz0 && var20.bz0 - 3 <= var28.bz1) {
               var26 = true;
            }
         }

         if (!var26) {
            Col var49 = this.gen.column(var18, var19);
            if (var49.h >= 64 && Math.abs(var49.h - var8) <= 6) {
               int[][] var50 = new int[][]{{var20.bx0, var20.bz0}, {var20.bx1, var20.bz0}, {var20.bx0, var20.bz1}, {var20.bx1, var20.bz1}};

               for (int[] var32 : var50) {
                  Col var33 = this.gen.column(var32[0], var32[1]);
                  if (var33.h < 64 || Math.abs(var33.h - var49.h) > 3) {
                     var26 = true;
                  }
               }

               if (!var26) {
                  var20.y0 = var49.h;
                  if (!var21 && !var36) {
                     var20.chest = true;
                     var36 = true;
                  }

                  var3.blds.add(var20);
               }
            }
         }
      }

      for (Structs.Bld var42 : var3.blds) {
         if (var42.type != 1) {
            int[] var45 = worldPos(var42, var42.w / 2, -1);
            Structs.Path var47 = new Structs.Path();
            Structs.Path var48 = new Structs.Path();
            var47.x0 = Math.min(var6, var45[0]);
            var47.x1 = Math.max(var6, var45[0]);
            var47.z0 = var47.z1 = var7 + 2;
            var48.x0 = var48.x1 = var45[0];
            var48.z0 = Math.min(var7 + 2, var45[1]);
            var48.z1 = Math.max(var7 + 2, var45[1]);
            var3.paths.add(var47);
            var3.paths.add(var48);
         }
      }

      var3.minX = var3.minZ = Integer.MAX_VALUE;
      var3.maxX = var3.maxZ = Integer.MIN_VALUE;

      for (Structs.Bld var43 : var3.blds) {
         var3.minX = Math.min(var3.minX, var43.bx0);
         var3.minZ = Math.min(var3.minZ, var43.bz0);
         var3.maxX = Math.max(var3.maxX, var43.bx1);
         var3.maxZ = Math.max(var3.maxZ, var43.bz1);
      }

      for (Structs.Path var44 : var3.paths) {
         var3.minX = Math.min(var3.minX, var44.x0);
         var3.minZ = Math.min(var3.minZ, var44.z0);
         var3.maxX = Math.max(var3.maxX, var44.x1);
         var3.maxZ = Math.max(var3.maxZ, var44.z1);
      }

      if (Gen.extras) {
         this.upgrade(var3);
      }

      return var3;
   }

   /**
    * Update 3.12: what the villages got on top of the old ones: a smithy (one of the houses with furnaces and a chest), two to four farms
    * with crops and a water channel, lamp posts beside the paths. Uses a random stream of its own: the buildings that were planned
    * before are exactly where they were.
    */
   void upgrade(Structs.Village v) {
      Random r = new Random(this.seed * 7046029254386353131L + v.cx * 31L + v.cz * 1000003L + 4242L);
      // the smithy
      for (Structs.Bld b : v.blds) {
         if (b.type == 0 && !b.chest && b.w >= 5) {
            b.type = 4;
            break;
         }
      }

      // the farms
      int want = 2 + r.nextInt(3);
      int farms = 0;
      for (int tries = 0; tries < 40 && farms < want; tries++) {
         double ang = r.nextDouble() * Math.PI * 2.0;
         double dist = 14.0 + r.nextDouble() * 30.0;
         int fx = v.cx + (int)Math.round(Math.cos(ang) * dist);
         int fz = v.cz + (int)Math.round(Math.sin(ang) * dist);
         Structs.Bld f = new Structs.Bld();
         f.type = 3;
         f.desert = v.desert;
         f.w = 9;
         f.d = 7;
         f.wallH = 1;
         int dx = v.cx - fx;
         int dz = v.cz - fz;
         if (Math.abs(dx) > Math.abs(dz)) {
            f.dir = dx > 0 ? 3 : 2;
         } else {
            f.dir = dz > 0 ? 1 : 0;
         }

         int ex = f.dir < 2 ? f.w : f.d;
         int ez = f.dir < 2 ? f.d : f.w;
         f.x0 = fx - ex / 2;
         f.z0 = fz - ez / 2;
         finish(f);
         boolean clash = false;
         for (Structs.Bld o : v.blds) {
            if (f.bx1 + 3 >= o.bx0 && f.bx0 - 3 <= o.bx1 && f.bz1 + 3 >= o.bz0 && f.bz0 - 3 <= o.bz1) {
               clash = true;
            }
         }

         if (clash) {
            continue;
         }

         Col mid = this.gen.column(fx, fz);
         if (mid.h < 64 || Math.abs(mid.h - v.cy) > 4) {
            continue;
         }

         int[][] corners = new int[][]{{f.bx0, f.bz0}, {f.bx1, f.bz0}, {f.bx0, f.bz1}, {f.bx1, f.bz1}};
         for (int[] c : corners) {
            Col cc = this.gen.column(c[0], c[1]);
            if (cc.h < 64 || Math.abs(cc.h - mid.h) > 1) {
               clash = true;
            }
         }

         if (clash) {
            continue;
         }

         f.y0 = mid.h;
         v.blds.add(f);
         farms++;
         // a path from the middle of the village to the gate of the farm
         int[] gate = worldPos(f, f.w / 2, -1);
         Structs.Path p1 = new Structs.Path();
         Structs.Path p2 = new Structs.Path();
         p1.x0 = Math.min(v.cx, gate[0]);
         p1.x1 = Math.max(v.cx, gate[0]);
         p1.z0 = p1.z1 = v.cz + 2;
         p2.x0 = p2.x1 = gate[0];
         p2.z0 = Math.min(v.cz + 2, gate[1]);
         p2.z1 = Math.max(v.cz + 2, gate[1]);
         v.paths.add(p1);
         v.paths.add(p2);
      }

      // lamp posts beside the paths, one every eight blocks
      for (Structs.Path p : v.paths) {
         boolean alongX = p.z0 == p.z1;
         for (int k = alongX ? p.x0 + 4 : p.z0 + 4; k <= (alongX ? p.x1 : p.z1) - 2; k += 8) {
            int[] lamp = alongX ? new int[]{k, p.z0 + 1} : new int[]{p.x0 + 1, k};
            boolean near = false;
            for (int[] o : v.lamps) {
               if (Math.abs(o[0] - lamp[0]) < 7 && Math.abs(o[1] - lamp[1]) < 7) {
                  near = true;
               }
            }

            if (!near) {
               v.lamps.add(lamp);
            }
         }
      }

      for (Structs.Bld b : v.blds) {
         v.minX = Math.min(v.minX, b.bx0);
         v.minZ = Math.min(v.minZ, b.bz0);
         v.maxX = Math.max(v.maxX, b.bx1);
         v.maxZ = Math.max(v.maxZ, b.bz1);
      }

      for (Structs.Path p : v.paths) {
         v.minX = Math.min(v.minX, p.x0);
         v.minZ = Math.min(v.minZ, p.z0);
         v.maxX = Math.max(v.maxX, p.x1);
         v.maxZ = Math.max(v.maxZ, p.z1);
      }
   }

   static void finish(Structs.Bld var0) {
      int var1;
      int var2;
      if (var0.type == 1) {
         var1 = var0.w;
         var2 = var0.d;
      } else if (var0.dir < 2) {
         var1 = var0.w;
         var2 = var0.d;
      } else {
         var1 = var0.d;
         var2 = var0.w;
      }

      var0.bx0 = var0.x0;
      var0.bz0 = var0.z0;
      var0.bx1 = var0.x0 + var1 - 1;
      var0.bz1 = var0.z0 + var2 - 1;
   }

   static int[] worldPos(Structs.Bld var0, int var1, int var2) {
      switch (var0.dir) {
         case 0:
            return new int[]{var0.x0 + var1, var0.z0 + var2};
         case 1:
            return new int[]{var0.x0 + var1, var0.z0 + var0.d - 1 - var2};
         case 2:
            return new int[]{var0.x0 + var2, var0.z0 + var1};
         default:
            return new int[]{var0.x0 + var0.d - 1 - var2, var0.z0 + var1};
      }
   }

   void villages(Chunk var1) {
      int var2 = 320;
      int var3 = Math.floorDiv(var1.cx * 16 - 90, var2);
      int var4 = Math.floorDiv(var1.cx * 16 + 16 + 90, var2);
      int var5 = Math.floorDiv(var1.cz * 16 - 90, var2);
      int var6 = Math.floorDiv(var1.cz * 16 + 16 + 90, var2);

      for (int var7 = var3; var7 <= var4; var7++) {
         for (int var8 = var5; var8 <= var6; var8++) {
            Structs.Village var9 = this.village(var7, var8);
            if (var9.valid && var9.style >= 0 && overlaps(var1, var9.minX - 1, var9.minZ - 1, var9.maxX + 1, var9.maxZ + 1)) {
               Village314.apply(this, var1, var9);
            } else if (var9.valid && overlaps(var1, var9.minX - 1, var9.minZ - 1, var9.maxX + 1, var9.maxZ + 1)) {
               for (Structs.Path var11 : var9.paths) {
                  if (overlaps(var1, var11.x0, var11.z0, var11.x1, var11.z1)) {
                     this.drawPath(var1, var11, var9);
                  }
               }

               for (Structs.Bld var13 : var9.blds) {
                  if (overlaps(var1, var13.bx0 - 1, var13.bz0 - 1, var13.bx1 + 2, var13.bz1 + 2)) {
                     if (var13.type == 1) {
                        this.well(var1, var13);
                     } else if (var13.type == 3) {
                        this.farm(var1, var13);
                     } else {
                        this.house(var1, var13, var9);
                     }
                  }
               }

               for (int[] lamp : var9.lamps) {
                  if (overlaps(var1, lamp[0], lamp[1], lamp[0], lamp[1])) {
                     this.lamp(var1, lamp[0], lamp[1], var9);
                  }
               }
            }
         }
      }
   }

   void drawPath(Chunk var1, Structs.Path var2, Structs.Village var3) {
      int var4 = Math.max(var2.x0, var1.cx * 16);
      int var5 = Math.min(var2.x1, var1.cx * 16 + 15);
      int var6 = Math.max(var2.z0, var1.cz * 16);
      int var7 = Math.min(var2.z1, var1.cz * 16 + 15);
      int var8 = var3.desert ? 17 : 8;

      for (int var9 = var6; var9 <= var7; var9++) {
         for (int var10 = var4; var10 <= var5; var10++) {
            int var11 = 130;

            while (var11 > 40 && soft(get(var1, var10, var11, var9)) && !Blocks.isFluid(get(var1, var10, var11, var9))) {
               var11--;
            }

            int var12 = get(var1, var10, var11, var9);
            if (var12 == 2 || var12 == 3 || var12 == 7) {
               set(var1, var10, var11, var9, var8);

               for (int var13 = 1; var13 < 4; var13++) {
                  int var14 = get(var1, var10, var11 + var13, var9);
                  if (var14 == 29 || var14 == 30 || var14 == 31 || Blocks.isLeaves(var14)) {
                     set(var1, var10, var11 + var13, var9, 0);
                  }
               }
            }
         }
      }
   }

   void foundation(Chunk var1, int var2, int var3, int var4, int var5, int var6) {
      int var7 = var4 - 1;

      for (int var8 = 0; var8 < 8 && var7 > 1; var7--) {
         int var9 = get(var1, var2, var7, var3);
         if (var9 < 0) {
            return;
         }

         if (!soft(var9)) {
            break;
         }

         set(var1, var2, var7, var3, var5);
         var8++;
      }

      for (int var10 = var4 + 1; var10 <= var4 + var6; var10++) {
         if (get(var1, var2, var10, var3) > 0) {
            set(var1, var2, var10, var3, 0);
         }
      }
   }

   void well(Chunk var1, Structs.Bld var2) {
      int var3 = var2.y0;
      int var4 = var2.desert ? 17 : 4;

      for (int var5 = -1; var5 <= 3; var5++) {
         for (int var6 = -1; var6 <= 3; var6++) {
            int var7 = var2.x0 + var6;
            int var8 = var2.z0 + var5;
            if (overlaps(var1, var7, var8, var7, var8)) {
               boolean var9 = var6 >= 0 && var6 < 3 && var5 >= 0 && var5 < 3;
               if (var9) {
                  this.foundation(var1, var7, var8, var3, var4, 8);
                  set(var1, var7, var3, var8, var4);
                  boolean var10 = var6 == 1 && var5 == 1;
                  if (var10) {
                     set(var1, var7, var3, var8, 50);
                     set(var1, var7, var3 - 1, var8, 50);
                     set(var1, var7, var3 - 2, var8, 4);
                  } else {
                     set(var1, var7, var3 + 1, var8, 0);
                  }

                  boolean var11 = (var6 == 0 || var6 == 2) && (var5 == 0 || var5 == 2);
                  if (!var11) {
                     if (!var10) {
                        set(var1, var7, var3 + 4, var8, 5);
                     }
                  } else {
                     for (int var12 = 1; var12 <= 3; var12++) {
                        set(var1, var7, var3 + var12, var8, 118);
                     }

                     set(var1, var7, var3 + 4, var8, 5);
                  }
               }
            }
         }
      }

      set(var1, var2.x0 + 1, var3 + 4, var2.z0 + 1, 5);
      set(var1, var2.x0 + 1, var3 + 5, var2.z0 + 1, 113);
   }

   /** Update 3.12: a lamp post - three fences and a torch - on the ground beside a path. */
   void lamp(Chunk c, int x, int z, Structs.Village v) {
      int y = 130;
      while (y > 40 && soft(get(c, x, y, z)) && !Blocks.isFluid(get(c, x, y, z))) {
         y--;
      }

      int ground = get(c, x, y, z);
      if (ground == 2 || ground == 3 || ground == 7 || ground == 8 || ground == 17) {
         set(c, x, y + 1, z, 118);
         set(c, x, y + 2, z, 118);
         set(c, x, y + 3, z, 118);
         set(c, x, y + 4, z, 32);
      }
   }

   /**
    * Update 3.12: a farm - 9 x 7 blocks, a fence round it with a gate in front, a channel of water down the middle and four rows of
    * wheat, carrots or potatoes on wet farmland (some of it ripe), a torch on every corner post.
    */
   void farm(Chunk c, Structs.Bld f) {
      int y = f.y0;
      Random r = new Random(f.x0 * 31L + f.z0 * 17L + f.y0 * 7L + 5L);
      int crop = r.nextInt(3);
      int base = crop == 0 ? Blocks.WHEAT : (crop == 1 ? Blocks.CARROT0 : Blocks.POTATO0);
      int other = r.nextBoolean() ? Blocks.WHEAT : Blocks.CARROT0;
      for (int j = -1; j <= f.d; j++) {
         for (int i = -1; i <= f.w; i++) {
            int[] p = worldPos(f, i, j);
            if (!overlaps(c, p[0], p[1], p[0], p[1])) {
               continue;
            }

            boolean inside = i >= 0 && i < f.w && j >= 0 && j < f.d;
            if (!inside) {
               for (int h = 1; h <= 3; h++) {
                  int id = get(c, p[0], y + h, p[1]);
                  if (id > 0 && (id == 29 || id == 30 || id == 31 || Blocks.isLeaves(id))) {
                     set(c, p[0], y + h, p[1], 0);
                  }
               }

               continue;
            }

            this.foundation(c, p[0], p[1], y, 3, 5);
            boolean edge = i == 0 || i == f.w - 1 || j == 0 || j == f.d - 1;
            boolean gate = j == 0 && i == f.w / 2;
            if (edge) {
               set(c, p[0], y, p[1], 3);
               if (!gate) {
                  set(c, p[0], y + 1, p[1], 118);
                  boolean post = (i == 0 || i == f.w - 1) && (j == 0 || j == f.d - 1);
                  if (post) {
                     set(c, p[0], y + 2, p[1], 118);
                     set(c, p[0], y + 3, p[1], 32);
                  }
               }
            } else if (j == f.d / 2) {
               set(c, p[0], y, p[1], 50);
            } else {
               set(c, p[0], y, p[1], Blocks.FARMLAND_WET);
               int stage = 3 + r.nextInt(5);
               int kind = j < f.d / 2 ? base : (r.nextInt(4) == 0 ? other : base);
               set(c, p[0], y + 1, p[1], kind + stage);
            }
         }
      }
   }

   static int doorId(Structs.Bld var0, int var1, boolean var2, Random var3) {
      int[] var4 = worldPos(var0, var1, 0);
      int[] var5 = worldPos(var0, var1, -1);
      int var6 = var5[0] - var4[0];
      int var7 = var5[1] - var4[1];
      int var8 = var7 < 0 ? 0 : (var7 > 0 ? 1 : (var6 > 0 ? 2 : 3));
      Random var9 = new Random(var0.x0 * 7919L + var0.z0 * 104729L + var0.y0);
      int var10 = var9.nextInt(4) == 0 ? 4 : 0;
      return (var2 ? 81 : 73) + var8 + var10;
   }

   void house(Chunk var1, Structs.Bld var2, Structs.Village var3) {
      int var4 = var2.y0;
      boolean var5 = var2.desert;
      boolean var6 = var2.type == 2;
      int var7 = var6 ? 27 : (var5 ? 17 : 5);
      int var8 = var6 ? 4 : (var5 ? 17 : 9);
      int var9 = var5 ? 17 : 4;
      if (var6) {
         int var10000 = 4;
      } else {
         int var56 = var5 ? 17 : 5;
      }

      int var11 = var2.w;
      int var12 = var2.d;
      int var13 = var2.wallH;
      Random var14 = new Random(var2.x0 * 31L + var2.z0 * 17L + var2.y0);

      for (int var15 = -1; var15 <= var11; var15++) {
         for (int var16 = -1; var16 <= var12; var16++) {
            int[] var17 = worldPos(var2, var15, var16);
            if (overlaps(var1, var17[0], var17[1], var17[0], var17[1])) {
               boolean var18 = var15 >= 0 && var15 < var11 && var16 >= 0 && var16 < var12;
               if (var18) {
                  this.foundation(var1, var17[0], var17[1], var4, var9, var13 + 8);
                  set(var1, var17[0], var4, var17[1], var6 ? 27 : (var5 ? 17 : 5));
                  boolean var32 = var15 == 0 || var15 == var11 - 1;
                  boolean var34 = var16 == 0 || var16 == var12 - 1;

                  for (int var21 = 1; var21 <= var13; var21++) {
                     int var22 = 0;
                     if (var32 && var34) {
                        var22 = var8;
                     } else if (var32 || var34) {
                        var22 = var7;
                        int var23 = var6 ? 3 : 2;
                        if (var21 == var23 || var6 && var21 == var23 + 1) {
                           if (var32 && var16 % 3 == 1 && var16 > 0 && var16 < var12 - 1) {
                              var22 = 15;
                           }

                           if (var34 && var16 == var12 - 1 && var15 == var11 / 2) {
                              var22 = 15;
                           }

                           if (!var6 && var34 && var16 == 0 && var15 != var11 / 2 && (var15 == 1 || var15 == var11 - 2) && var21 == 2) {
                              var22 = 15;
                           }
                        }

                        if (var16 == 0 && var15 == var11 / 2 && var21 <= 2) {
                           var22 = doorId(var2, var15, var21 == 2, var14);
                        }
                     }

                     set(var1, var17[0], var4 + var21, var17[1], var22);
                  }
               } else {
                  for (int var19 = 1; var19 <= 4; var19++) {
                     int var20 = get(var1, var17[0], var4 + var19, var17[1]);
                     if (var20 > 0 && (var20 == 29 || Blocks.isLeaves(var20) || var20 == 30 || var20 == 31)) {
                        set(var1, var17[0], var4 + var19, var17[1], 0);
                     }
                  }
               }
            }
         }
      }

      int var28 = var6 ? 1 : (var5 ? 4 : (var14.nextInt(4) == 0 ? 3 : 0));
      int var29 = Blocks.STAIR_MAT[var28];
      int[] var30 = worldPos(var2, 0, 0);
      int[] var31 = worldPos(var2, 1, 0);
      int var33 = faceOf(var31[0] - var30[0], var31[1] - var30[1]);
      int var35 = faceOf(var30[0] - var31[0], var30[1] - var31[1]);
      int var36 = (var11 + 1) / 2;

      for (int var37 = 0; var37 < var36; var37++) {
         for (int var43 = var37; var43 <= var11 - 1 - var37; var43++) {
            for (int var24 = -1; var24 <= var12; var24++) {
               int[] var25 = worldPos(var2, var43, var24);
               if (overlaps(var1, var25[0], var25[1], var25[0], var25[1])) {
                  int var26;
                  if (var43 == var37 && var43 < var11 - 1 - var37) {
                     var26 = 93 + var28 * 4 + var33;
                  } else if (var43 == var11 - 1 - var37 && var43 > var37) {
                     var26 = 93 + var28 * 4 + var35;
                  } else if (var43 == var37) {
                     var26 = var29;
                  } else if (var37 == 0) {
                     var26 = var29;
                  } else {
                     if (var24 != 0 && var24 != var12 - 1) {
                        continue;
                     }

                     var26 = var7;
                  }

                  set(var1, var25[0], var4 + var13 + 1 + var37, var25[1], var26);
               }
            }
         }
      }

      if ((var11 & 1) == 1) {
         for (int var38 = -1; var38 <= var12; var38++) {
            int[] var44 = worldPos(var2, var36 - 1, var38);
            if (overlaps(var1, var44[0], var44[1], var44[0], var44[1])) {
               set(var1, var44[0], var4 + var13 + 1 + var36, var44[1], 113 + var28);
            }
         }
      }

      if (var2.type == 4) {
         // Update 3.12: the smithy - two furnaces in a row and a chest at the far corner
         int[] f1 = worldPos(var2, 1, var12 - 2);
         int[] f2 = worldPos(var2, 2, var12 - 2);
         int[] ch = worldPos(var2, var11 - 2, var12 - 2);
         set(var1, f1[0], var4 + 1, f1[1], 67);
         set(var1, f2[0], var4 + 1, f2[1], 67);
         set(var1, ch[0], var4 + 1, ch[1], 70);
      } else if (var2.chest) {
         int[] var39 = worldPos(var2, var11 - 2, var12 - 2);
         set(var1, var39[0], var4 + 1, var39[1], 70);
         set(var1, var39[0], var4 + 2, var39[1], 0);
         int[] var45 = worldPos(var2, 1, var12 - 2);
         set(var1, var45[0], var4 + 1, var45[1], 66);
      } else if (!var6) {
         int[] var40 = worldPos(var2, 1, var12 - 2);
         int var46 = var14.nextInt(3);
         set(var1, var40[0], var4 + 1, var40[1], var46 == 0 ? 66 : (var46 == 1 ? 67 : 70));
         set(var1, var40[0], var4 + 2, var40[1], 0);
      }

      if (var6) {
         int[] var41 = worldPos(var2, var11 / 2, var12 - 2);
         set(var1, var41[0], var4 + 1, var41[1], 25);

         for (int var47 = 1; var47 < var11 - 1; var47++) {
            int[] var51 = worldPos(var2, var47, var12 - 1);

            for (int var53 = var13 + 1; var53 <= var13 + 4; var53++) {
               set(var1, var51[0], var4 + var53, var51[1], 27);
            }
         }

         int[] var48 = worldPos(var2, var11 / 2, var12 - 1);
         set(var1, var48[0], var4 + var13 + 5, var48[1], 32);
      }

      this.wtorch(var1, var2, 1, var12 - 2, 1, var12 - 1, var4 + 2);
      this.wtorch(var1, var2, var11 - 2, var12 - 2, var11 - 2, var12 - 1, var4 + 2);
      if (var11 >= 5) {
         this.wtorch(var1, var2, var11 / 2 - 1, -1, var11 / 2 - 1, 0, var4 + 2);
         this.wtorch(var1, var2, var11 / 2 + 1, -1, var11 / 2 + 1, 0, var4 + 2);
      }

      int[] var42 = worldPos(var2, var11 / 2 + 2 > var11 - 1 ? var11 / 2 - 2 : var11 / 2 + 2, -2);
      if (overlaps(var1, var42[0], var42[1], var42[0], var42[1])) {
         int var49 = var4;
         set(var1, var42[0], var49 + 1, var42[1], 118);
         set(var1, var42[0], var49 + 2, var42[1], 118);
         set(var1, var42[0], var49 + 3, var42[1], 32);
      }

      if (!var6 && var14.nextInt(3) == 0) {
         for (int var50 = 0; var50 < var11; var50++) {
            for (int var52 = var12 + 1; var52 <= var12 + 2; var52++) {
               boolean var54 = var50 == 0 || var50 == var11 - 1 || var52 == var12 + 2;
               int[] var55 = worldPos(var2, var50, var52);
               if (overlaps(var1, var55[0], var55[1], var55[0], var55[1])) {
                  int var27 = get(var1, var55[0], var4 - 1, var55[1]);
                  if (var27 == 2 || var27 == 3) {
                     if (var54) {
                        set(var1, var55[0], var4, var55[1], 118);
                     } else if (!var5) {
                        set(var1, var55[0], var4, var55[1], (var50 + var52) % 2 == 0 ? 30 : 31);
                     }
                  }
               }
            }
         }
      }
   }

   static int faceOf(int var0, int var1) {
      return var1 < 0 ? 0 : (var1 > 0 ? 1 : (var0 > 0 ? 2 : 3));
   }

   void wtorch(Chunk var1, Structs.Bld var2, int var3, int var4, int var5, int var6, int var7) {
      int[] var8 = worldPos(var2, var3, var4);
      int[] var9 = worldPos(var2, var5, var6);
      if (overlaps(var1, var8[0], var8[1], var8[0], var8[1])) {
         int var10 = get(var1, var9[0], var7, var9[1]);
         if (var10 > 0 && var10 < Blocks.MAX && Blocks.solid[var10] && Blocks.opaque[var10]) {
            if (get(var1, var8[0], var7, var8[1]) <= 0) {
               set(var1, var8[0], var7, var8[1], 89 + faceOf(var9[0] - var8[0], var9[1] - var8[1]));
            }
         }
      }
   }

   void dungeons(Chunk var1) {
      Random var2 = new Random(this.seed * 2862933555777941757L + var1.cx * 3037000493L + var1.cz * 1103515245L + 99L);
      if (var2.nextInt(14) == 0) {
         int var3 = 5 + 2 * var2.nextInt(2);
         int var4 = 5 + 2 * var2.nextInt(2);
         int var5 = 1 + var2.nextInt(16 - var3 - 1);
         int var6 = 1 + var2.nextInt(16 - var4 - 1);
         int var7 = 50;
         int var8 = 12 + var2.nextInt(var7 - 12);
         int var9 = 0;
         int var10 = 0;
         int var11 = 0;

         for (int var12 = var8 - 1; var12 <= var8 + 4; var12++) {
            for (int var13 = var6 - 1; var13 <= var6 + var4; var13++) {
               for (int var14 = var5 - 1; var14 <= var5 + var3; var14++) {
                  int var15 = var1.get(var14, var12, var13);
                  var10++;
                  if (var15 != 0 && !Blocks.isFluid(var15)) {
                     var9++;
                  }

                  if (Blocks.isFluid(var15)) {
                     var11 += 5;
                  }
               }
            }
         }

         if (!(var9 < var10 * 0.75) && var11 <= 0) {
            int var19 = var1.cx * 16;
            int var20 = var1.cz * 16;

            for (int var21 = var8 - 1; var21 <= var8 + 4; var21++) {
               for (int var24 = var6 - 1; var24 <= var6 + var4; var24++) {
                  for (int var16 = var5 - 1; var16 <= var5 + var3; var16++) {
                     boolean var17 = var16 < var5 || var16 >= var5 + var3 || var24 < var6 || var24 >= var6 + var4 || var21 < var8 || var21 > var8 + 3;
                     int var18;
                     if (var17) {
                        var18 = (byte)(var2.nextInt(4) == 0 ? 4 : 72);
                        if (var21 > var8 && var1.get(var16, var21, var24) == 0) {
                           continue;
                        }
                     } else {
                        var18 = 0;
                     }

                     var1.set(var16, var21, var24, var18);
                  }
               }
            }

            for (int var22 = var6; var22 < var6 + var4; var22++) {
               for (int var25 = var5; var25 < var5 + var3; var25++) {
                  var1.set(var25, var8 - 1, var22, var2.nextInt(3) == 0 ? 4 : 72);
               }
            }

            var1.set(var5 + var3 / 2, var8, var6 + var4 / 2, 71);
            int var23 = 0;

            for (int var26 = 0; var26 < 20 && var23 < 2; var26++) {
               int var27 = var2.nextInt(4);
               int var28;
               int var29;
               if (var27 < 2) {
                  var28 = var5 + var2.nextInt(var3);
                  var29 = var27 == 0 ? var6 : var6 + var4 - 1;
               } else {
                  var29 = var6 + var2.nextInt(var4);
                  var28 = var27 == 2 ? var5 : var5 + var3 - 1;
               }

               if (var1.get(var28, var8, var29) == 0) {
                  var1.set(var28, var8, var29, 70);
                  var23++;
               }
            }
         }
      }
   }

   Structs.Shaft shaft(int var1, int var2) {
      long var3 = (long)var1 << 32 ^ var2 & 4294967295L;
      Structs.Shaft var5 = (Structs.Shaft)this.shaftCache.get(var3);
      if (var5 != null) {
         return var5;
      }

      var5 = this.planShaft(var1, var2);
      this.shaftCache.put(var3, var5);
      return var5;
   }

   Structs.Shaft planShaft(int var1, int var2) {
      Structs.Shaft var3 = new Structs.Shaft();
      Random var4 = new Random(this.seed * 1181783497276652981L + var1 * 7046029254386353131L + var2 * 3935559000370003845L + 5L);
      if (var4.nextFloat() > 0.45F) {
         return var3;
      }

      var3.valid = true;
      int var5 = 128;
      int var6 = var1 * var5 + 36 + var4.nextInt(var5 - 72);
      int var7 = var2 * var5 + 36 + var4.nextInt(var5 - 72);
      int var8 = 22 + var4.nextInt(22);
      addCorr(var3, var6 - 20 - var4.nextInt(30), var7, var6 + 20 + var4.nextInt(30), var7, var8, true);
      addCorr(var3, var6, var7 - 20 - var4.nextInt(30), var6, var7 + 20 + var4.nextInt(30), var8, false);
      int var9 = var3.cs.size();

      for (int var10 = 0; var10 < var9; var10++) {
         Structs.Corr var11 = (Structs.Corr)var3.cs.get(var10);
         int var12 = var11.alongX ? var11.x1 - var11.x0 : var11.z1 - var11.z0;

         for (int var13 = 8; var13 < var12 - 4; var13 += 8 + var4.nextInt(8)) {
            if (var4.nextInt(3) != 0) {
               int var14 = 10 + var4.nextInt(18);
               int var15 = var4.nextBoolean() ? 1 : -1;
               if (var11.alongX) {
                  int var16 = var11.x0 + var13;
                  addCorr(
                     var3,
                     var16,
                     var15 > 0 ? var11.z0 : var11.z0 - var14,
                     var16,
                     var15 > 0 ? var11.z0 + var14 : var11.z0,
                     var8 + (var4.nextInt(5) == 0 ? 0 : 0),
                     false
                  );
               } else {
                  int var19 = var11.z0 + var13;
                  addCorr(var3, var15 > 0 ? var11.x0 : var11.x0 - var14, var19, var15 > 0 ? var11.x0 + var14 : var11.x0, var19, var8, true);
               }
            }
         }
      }

      var3.minX = var3.minZ = Integer.MAX_VALUE;
      var3.maxX = var3.maxZ = Integer.MIN_VALUE;

      for (Structs.Corr var18 : var3.cs) {
         var3.minX = Math.min(var3.minX, var18.x0 - 2);
         var3.minZ = Math.min(var3.minZ, var18.z0 - 2);
         var3.maxX = Math.max(var3.maxX, var18.x1 + 2);
         var3.maxZ = Math.max(var3.maxZ, var18.z1 + 2);
      }

      return var3;
   }

   static void addCorr(Structs.Shaft var0, int var1, int var2, int var3, int var4, int var5, boolean var6) {
      Structs.Corr var7 = new Structs.Corr();
      var7.x0 = Math.min(var1, var3);
      var7.x1 = Math.max(var1, var3);
      var7.z0 = Math.min(var2, var4);
      var7.z1 = Math.max(var2, var4);
      var7.y = var5;
      var7.alongX = var6;
      var0.cs.add(var7);
   }

   void mineshafts(Chunk var1) {
      int var2 = 128;
      int var3 = Math.floorDiv(var1.cx * 16 - 90, var2);
      int var4 = Math.floorDiv(var1.cx * 16 + 16 + 90, var2);
      int var5 = Math.floorDiv(var1.cz * 16 - 90, var2);
      int var6 = Math.floorDiv(var1.cz * 16 + 16 + 90, var2);

      for (int var7 = var3; var7 <= var4; var7++) {
         for (int var8 = var5; var8 <= var6; var8++) {
            Structs.Shaft var9 = this.shaft(var7, var8);
            if (var9.valid && overlaps(var1, var9.minX, var9.minZ, var9.maxX, var9.maxZ)) {
               for (Structs.Corr var11 : var9.cs) {
                  int var12 = var11.alongX ? var11.x0 : var11.x0 - 1;
                  int var13 = var11.alongX ? var11.x1 : var11.x1 + 1;
                  int var14 = var11.alongX ? var11.z0 - 1 : var11.z0;
                  int var15 = var11.alongX ? var11.z1 + 1 : var11.z1 + 1;
                  if (var11.alongX) {
                     var14 = var11.z0 - 1;
                     var15 = var11.z0 + 1;
                  } else {
                     var12 = var11.x0 - 1;
                     var13 = var11.x0 + 1;
                  }

                  if (overlaps(var1, var12, var14, var13, var15)) {
                     this.carve(var1, var11, var12, var14, var13, var15);
                  }
               }
            }
         }
      }
   }

   void carve(Chunk var1, Structs.Corr var2, int var3, int var4, int var5, int var6) {
      int var7 = var2.y;
      int var8 = Math.max(var3, var1.cx * 16);
      int var9 = Math.min(var5, var1.cx * 16 + 15);
      int var10 = Math.max(var4, var1.cz * 16);
      int var11 = Math.min(var6, var1.cz * 16 + 15);

      for (int var12 = var10; var12 <= var11; var12++) {
         for (int var13 = var8; var13 <= var9; var13++) {
            for (int var14 = 0; var14 < 3; var14++) {
               int var15 = get(var1, var13, var7 + var14, var12);
               if (var15 != 6 && !Blocks.isFluid(var15)) {
                  set(var1, var13, var7 + var14, var12, 0);
               }
            }

            int var25 = get(var1, var13, var7 - 1, var12);
            if (var25 == 0) {
               set(var1, var13, var7 - 1, var12, 5);
            }
         }
      }

      int var22 = var2.alongX ? var2.x1 - var2.x0 : var2.z1 - var2.z0;

      for (int var23 = 2; var23 < var22 - 1; var23 += 4) {
         int var26 = var2.alongX ? var2.x0 + var23 : var2.x0;
         int var28 = var2.alongX ? var2.z0 : var2.z0 + var23;
         int var16 = var2.alongX ? 0 : 1;
         int var17 = var2.alongX ? 1 : 0;

         for (int var18 = -1; var18 <= 1; var18 += 2) {
            int var19 = var26 + var16 * var18;
            int var20 = var28 + var17 * var18;
            if (get(var1, var19, var7, var20) != 0) {
            }

            if (overlaps(var1, var19, var20, var19, var20)) {
               set(var1, var19, var7, var20, 9);
               set(var1, var19, var7 + 1, var20, 9);
            }
         }

         for (int var31 = -1; var31 <= 1; var31++) {
            int var33 = var26 + var16 * var31;
            int var35 = var28 + var17 * var31;
            if (overlaps(var1, var33, var35, var33, var35)) {
               set(var1, var33, var7 + 2, var35, 5);
            }
         }

         if (var23 / 4 % 3 == 1) {
            int var32 = var26 + var16;
            int var34 = var28 + var17;
            if (overlaps(var1, var32, var34, var32, var34)) {
               set(var1, var32, var7 + 2, var34, 5);
            }

            int var36 = var26 - var16;
            int var21 = var28 - var17;
            if (overlaps(var1, var36, var21, var36, var21)) {
               set(var1, var36, var7 + 1, var21, 32);
            }
         }
      }

      Random var24 = new Random(var2.x0 * 91L + var2.z0 * 37L + var2.y);
      if (var22 > 12 && var24.nextInt(3) == 0) {
         int var27 = 4 + var24.nextInt(var22 - 8);
         int var29 = var2.alongX ? var2.x0 + var27 : var2.x0 + 1;
         int var30 = var2.alongX ? var2.z0 + 1 : var2.z0 + var27;
         if (overlaps(var1, var29, var30, var29, var30) && get(var1, var29, var7, var30) == 0) {
            set(var1, var29, var7, var30, 70);
         }
      }
   }

   static final class Bld {
      int type;
      int x0;
      int z0;
      int w;
      int d;
      int y0;
      int dir;
      int wallH;
      int bx0;
      int bz0;
      int bx1;
      int bz1;
      boolean chest;
      boolean desert;
   }

   static final class Corr {
      int x0;
      int z0;
      int x1;
      int z1;
      int y;
      boolean alongX;
   }

   static final class Path {
      int x0;
      int z0;
      int x1;
      int z1;
   }

   static final class Shaft {
      boolean valid;
      ArrayList<Structs.Corr> cs = new ArrayList();
      int minX;
      int minZ;
      int maxX;
      int maxZ;
   }

   static final class Village {
      boolean valid;
      int cx;
      int cz;
      int cy;
      boolean desert;
      ArrayList<Structs.Bld> blds = new ArrayList();
      ArrayList<Structs.Path> paths = new ArrayList();
      /** Update 3.12: lamp posts {x, z} beside the paths. */
      ArrayList<int[]> lamps = new ArrayList();
      /** Update 3.14: the village of the new generator (-1: one of the old kind): its palette ({@link Style}), the buildings, streets and the places where villagers start {x, z, y}. */
      int style = -1;
      ArrayList<Village314.Placed> placed = new ArrayList();
      ArrayList<Village314.Street> streets = new ArrayList();
      ArrayList<int[]> spawn = new ArrayList();
      int minX;
      int minZ;
      int maxX;
      int maxZ;
   }
}
