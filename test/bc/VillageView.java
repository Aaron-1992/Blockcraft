package bc;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import javax.imageio.ImageIO;

/**
 * Development tool (no GL needed): finds the villages of a seed and draws one of them with the software rasterizer of {@link MeshView}.
 * java ... bc.VillageView out.png seed type index yaw pitch distance [width] [height] [fov] [lookDx lookDz]
 * (type = WorldSettings type, 1 default; index = which village, nearest to 0,0 first; yaw 0 looks south)
 */
public final class VillageView {
   static ArrayList<Structs.Village> find(Gen g, int rr) {
      ArrayList<Structs.Village> list = new ArrayList<>();
      for (int rx = -rr; rx <= rr; rx++) {
         for (int rz = -rr; rz <= rr; rz++) {
            Structs.Village v = g.structs.village(rx, rz);
            if (v.valid) {
               list.add(v);
            }
         }
      }

      list.sort(Comparator.comparingDouble(v -> Math.hypot(v.cx, v.cz)));
      return list;
   }

   public static void main(String[] a) throws Exception {
      Tex.initPixels();
      long seed = Long.parseLong(a[1]);
      WorldSettings st = new WorldSettings();
      st.type = Integer.parseInt(a[2]);
      Gen.extras = true;
      World world = new World(seed, null, Dim.OVERWORLD, st);
      world.saveEnabled = false;
      Gen g = world.gen;
      ArrayList<Structs.Village> vs = find(g, 3);
      int index = Integer.parseInt(a[3]);
      for (int i = 0; i < Math.min(vs.size(), 8); i++) {
         Structs.Village v = vs.get(i);
         System.out.println("village " + i + " at " + v.cx + "," + v.cz + " y " + v.cy + " style " + (v.style < 0 ? "old" : Style.NAME[v.style]) + " buildings " + v.placed.size() + " streets " + v.streets.size());
      }

      Structs.Village v = vs.get(index);
      double yaw = Double.parseDouble(a[4]);
      double pitch = Double.parseDouble(a[5]);
      double dist = Double.parseDouble(a[6]);
      int w = a.length > 7 ? Integer.parseInt(a[7]) : 960;
      int h = a.length > 8 ? Integer.parseInt(a[8]) : 540;
      double fov = a.length > 9 ? Double.parseDouble(a[9]) : 60.0;
      double dx = a.length > 10 ? Double.parseDouble(a[10]) : 0.0;
      double dz = a.length > 11 ? Double.parseDouble(a[11]) : 0.0;
      int ccx = Math.floorDiv(v.cx, 16);
      int ccz = Math.floorDiv(v.cz, 16);
      int rad = (int)Math.ceil(dist * 0.5 / 16.0) + 1;
      rad = Math.min(rad, 7);
      for (int cz = ccz - rad; cz <= ccz + rad; cz++) {
         for (int cx = ccx - rad; cx <= ccx + rad; cx++) {
            Chunk c = g.generate(cx, cz);
            world.chunks.put(World.key(cx, cz), c);
         }
      }

      MeshView.relight(world);
      BufferedImage img = StructView.shot(world, v.cx + dx, v.cy + 3, v.cz + dz, yaw, pitch, dist, w, h, fov);
      ImageIO.write(img, "png", new File(a[0]));
      StringBuilder sb = new StringBuilder("buildings:");
      for (Village314.Placed p : v.placed) {
         sb.append(' ').append(p.tpl.name);
      }

      System.out.println(sb);
      for (Village314.Street s : v.streets) {
         System.out.println("street " + (s.alongX ? "x " : "z ") + s.x0 + "," + s.z0 + " .. " + s.x1 + "," + s.z1);
      }

      System.out.println("wrote " + a[0]);
   }
}
