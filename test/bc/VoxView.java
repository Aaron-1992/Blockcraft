package bc;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import javax.imageio.ImageIO;

/**
 * Development tool (no GL needed): a small software ray caster that draws the terrain of a generator from a camera position, so that terrain,
 * plants and structures can be looked at without starting the game.
 * java ... bc.VoxView out.png seed type x y z yaw pitch [viewDistance] [width] [height] [fov]
 * yaw 0 looks along +z, 90 along -x (like the game); y < 0 puts the camera 2 blocks above the ground.
 */
public final class VoxView {
   final Gen gen;
   final HashMap<Long, Chunk> chunks = new HashMap<>();
   Chunk last;
   int lastCx = Integer.MIN_VALUE;
   int lastCz = Integer.MIN_VALUE;
   final int[] top;
   final int[] side;

   VoxView(Gen gen) {
      this.gen = gen;
      this.top = LodMap.averages(true);
      this.side = LodMap.averages(false);
   }

   Chunk chunk(int cx, int cz) {
      if (cx == this.lastCx && cz == this.lastCz) {
         return this.last;
      }

      long k = (long)cx << 32 ^ cz & 0xFFFFFFFFL;
      Chunk c = this.chunks.get(k);
      if (c == null) {
         c = this.gen.generate(cx, cz);
         this.chunks.put(k, c);
      }

      this.last = c;
      this.lastCx = cx;
      this.lastCz = cz;
      return c;
   }

   int block(int x, int y, int z) {
      if (y < 0) {
         return 6;
      }

      if (y > 255) {
         return 0;
      }

      return this.chunk(x >> 4, z >> 4).blocks[Chunk.idx(x & 15, y, z & 15)] & 0xFFFF;
   }

   static int clamp(int v) {
      return v < 0 ? 0 : (v > 255 ? 255 : v);
   }

   static int hash(int x, int y, int z) {
      int h = x * 73856093 ^ y * 19349663 ^ z * 83492791;
      h ^= h >>> 13;
      h *= 0x5bd1e995;
      return h ^ h >>> 15;
   }

   public static void main(String[] a) throws Exception {
      Tex.initPixels();
      long seed = Long.parseLong(a[1]);
      WorldSettings st = new WorldSettings();
      st.type = Integer.parseInt(a[2]);
      if (a[2].startsWith("9")) {
         st.type = WorldSettings.FLAT;
      }

      Gen g = Dim.makeGen(Dim.OVERWORLD, seed, st);
      VoxView v = new VoxView(g);
      double cx = Double.parseDouble(a[3]);
      double cy = Double.parseDouble(a[4]);
      double cz = Double.parseDouble(a[5]);
      double yaw = Math.toRadians(Double.parseDouble(a[6]));
      double pitch = Math.toRadians(Double.parseDouble(a[7]));
      int dist = a.length > 8 ? Integer.parseInt(a[8]) : 160;
      int w = a.length > 9 ? Integer.parseInt(a[9]) : 960;
      int h = a.length > 10 ? Integer.parseInt(a[10]) : 540;
      double fov = Math.toRadians(a.length > 11 ? Double.parseDouble(a[11]) : 70.0);
      if (cy < 0) {
         int y = 255;
         while (y > 0 && v.block((int)Math.floor(cx), y, (int)Math.floor(cz)) == 0) {
            y--;
         }

         cy = y + 1.0 - cy;
      }

      BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
      double fwdX = -Math.sin(yaw) * Math.cos(pitch);
      double fwdY = -Math.sin(pitch);
      double fwdZ = Math.cos(yaw) * Math.cos(pitch);
      double rightX = -Math.cos(yaw);
      double rightZ = -Math.sin(yaw);
      double upX = fwdY * rightZ * 0 + Math.sin(yaw) * Math.sin(pitch);
      double upY = Math.cos(pitch);
      double upZ = -Math.cos(yaw) * Math.sin(pitch);
      double tanH = Math.tan(fov / 2.0);
      double aspect = (double)w / h;
      long t0 = System.nanoTime();
      for (int py = 0; py < h; py++) {
         for (int px = 0; px < w; px++) {
            double sx = ((px + 0.5) / w * 2.0 - 1.0) * tanH * aspect;
            double sy = (1.0 - (py + 0.5) / h * 2.0) * tanH;
            double dx = fwdX + rightX * sx + upX * sy;
            double dy = fwdY + upY * sy;
            double dz = fwdZ + rightZ * sx + upZ * sy;
            double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
            img.setRGB(px, py, v.trace(cx, cy, cz, dx / len, dy / len, dz / len, dist));
         }
      }

      ImageIO.write(img, "png", new File(a[0]));
      System.out.printf("%dx%d in %d ms, %d chunks%n", w, h, (System.nanoTime() - t0) / 1000000, v.chunks.size());
   }

   int sky(double dy) {
      double t = Math.max(0.0, Math.min(1.0, dy * 1.6 + 0.12));
      int r = (int)(190 + (96 - 190) * t);
      int g = (int)(214 + (156 - 214) * t);
      int b = (int)(250 + (232 - 250) * t);
      return r << 16 | g << 8 | b;
   }

   int trace(double ox, double oy, double oz, double dx, double dy, double dz, int maxDist) {
      int ix = (int)Math.floor(ox);
      int iy = (int)Math.floor(oy);
      int iz = (int)Math.floor(oz);
      int stepX = dx > 0 ? 1 : -1;
      int stepY = dy > 0 ? 1 : -1;
      int stepZ = dz > 0 ? 1 : -1;
      double tdx = dx == 0 ? 1e30 : Math.abs(1.0 / dx);
      double tdy = dy == 0 ? 1e30 : Math.abs(1.0 / dy);
      double tdz = dz == 0 ? 1e30 : Math.abs(1.0 / dz);
      double tx = dx == 0 ? 1e30 : (dx > 0 ? ix + 1 - ox : ox - ix) * tdx;
      double ty = dy == 0 ? 1e30 : (dy > 0 ? iy + 1 - oy : oy - iy) * tdy;
      double tz = dz == 0 ? 1e30 : (dz > 0 ? iz + 1 - oz : oz - iz) * tdz;
      double t = 0;
      int face = 1;
      double water = 0;
      int waterTint = 0xFFFFFF;
      int skyC = this.sky(dy);
      while (t < maxDist) {
         int id = iy < 0 ? 6 : (iy > 255 ? 0 : this.block(ix, iy, iz));
         if (iy > 255 && dy > 0 || iy < -1) {
            break;
         }

         if (id != 0) {
            int kind = Blocks.kind[id];
            if (Blocks.isWater(id)) {
               if (water == 0) {
                  Chunk c = this.chunk(ix >> 4, iz >> 4);
                  waterTint = c.waterColor[(iz & 15) * 16 + (ix & 15)];
               }

               water += 1.0;
            } else if (kind == 1 || kind == 9 || !Blocks.solid[id] && id != Blocks313.LILY_PAD) {
               // plants and thin things: a little colour, no blocking
               if (kind == 1 && (hash(ix, iy, iz) & 3) == 0 && t < 60) {
                  // sparse sprite impression
                  int tc = this.top[id];
                  Chunk c = this.chunk(ix >> 4, iz >> 4);
                  if (Blocks.tint[id] == 1) {
                     int gc = c.grassColor[(iz & 15) * 16 + (ix & 15)];
                     tc = tint(tc, gc);
                  }

                  return this.finish(tc, t, maxDist, water, waterTint, skyC, 0.85);
               }
            } else {
               Chunk c = this.chunk(ix >> 4, iz >> 4);
               int col;
               boolean topFace = face == 0;
               col = topFace ? this.top[id] : this.side[id];
               int lx = ix & 15;
               int lz = iz & 15;
               if (id == Blocks.GRASS) {
                  col = topFace ? tint(this.top[id], c.grassColor[lz * 16 + lx]) : this.side[id];
               } else if (Blocks.tint[id] == 1 && topFace) {
                  col = tint(col, c.grassColor[lz * 16 + lx]);
               } else if (Blocks.tint[id] == 2) {
                  int fc = id == 13 ? 8431445 : (id == 14 ? 6396257 : c.foliageColor[lz * 16 + lx]);
                  col = tint(this.top[id], fc);
               }

               double sh = face == 0 ? 1.0 : (face == 1 ? 0.5 : (face == 2 || face == 3 ? 0.78 : 0.64));
               if (Blocks.isLeaves(id)) {
                  sh *= 0.8 + (hash(ix, iy, iz) & 15) / 15.0 * 0.3;
               } else {
                  sh *= 0.94 + (hash(ix, iy, iz) & 7) / 7.0 * 0.12;
               }

               col = scale(col, sh);
               return this.finish(col, t, maxDist, water, waterTint, skyC, 1.0);
            }
         }

         if (tx < ty && tx < tz) {
            ix += stepX;
            t = tx;
            tx += tdx;
            face = stepX > 0 ? 5 : 4;
         } else if (ty < tz) {
            iy += stepY;
            t = ty;
            ty += tdy;
            face = stepY > 0 ? 1 : 0;
         } else {
            iz += stepZ;
            t = tz;
            tz += tdz;
            face = stepZ > 0 ? 3 : 2;
         }
      }

      // the sky; water in front of it (looking down a river to the horizon) is ignored
      return skyC;
   }

   int finish(int col, double t, int maxDist, double water, int waterTint, int skyC, double a) {
      if (water > 0) {
         int wc = scale(tint(0x3A68D0, waterTint), 1.0);
         double k = 1.0 - Math.exp(-0.16 * water);
         col = mix(col, wc, Math.min(0.92, 0.35 + k * 0.6));
      }

      double fog = 1.0 - Math.exp(-Math.pow(t / (maxDist * 0.75), 2.2));
      return mix(col, skyC, Math.max(0.0, Math.min(1.0, fog)) * 0.95);
   }

   static int tint(int c, int t) {
      int r = (c >> 16 & 255) * (t >> 16 & 255) / 255;
      int g = (c >> 8 & 255) * (t >> 8 & 255) / 255;
      int b = (c & 255) * (t & 255) / 255;
      return r << 16 | g << 8 | b;
   }

   static int scale(int c, double f) {
      return clamp((int)((c >> 16 & 255) * f)) << 16 | clamp((int)((c >> 8 & 255) * f)) << 8 | clamp((int)((c & 255) * f));
   }

   static int mix(int a, int b, double t) {
      int r = (int)((a >> 16 & 255) * (1 - t) + (b >> 16 & 255) * t);
      int g = (int)((a >> 8 & 255) * (1 - t) + (b >> 8 & 255) * t);
      int bl = (int)((a & 255) * (1 - t) + (b & 255) * t);
      return r << 16 | g << 8 | bl;
   }
}
