package bc;

/** Development tool: where does the title panorama stand, and what does it look like? java ... bc.PanoView outdir [w h] -> pano-<yaw>.png (software ray caster) */
public final class PanoView {
   public static void main(String[] a) throws Exception {
      String out = a[0];
      int w = a.length > 2 ? Integer.parseInt(a[1]) : 640;
      int h = a.length > 2 ? Integer.parseInt(a[2]) : 360;
      WorldSettings ws = new WorldSettings();
      Gen gen = Dim.makeGen(Dim.OVERWORLD, Panorama.SEED, ws);
      long t0 = System.nanoTime();
      double[] v = Panorama.pick(gen);
      System.out.println("pick: " + (System.nanoTime() - t0) / 1000000 + " ms  score=" + v[0] + " x=" + v[1] + " eyeY=" + v[2] + " z=" + v[3] + " yaw0=" + v[4]);
      java.util.List<double[]> all = Panorama.candidates(gen);
      for (int k = 0; k < Math.min(a.length > 3 ? Integer.parseInt(a[3]) : 4, all.size()); k++) {
         double[] c = all.get(k);
         System.out.println("#" + k + " score=" + c[0] + " x=" + c[1] + " eyeY=" + c[2] + " z=" + c[3] + " yaw0=" + c[4]);
         VoxView.main(new String[]{out + "/cand-" + k + ".png", Long.toString(Panorama.SEED), "1", Double.toString(c[1]), Double.toString(c[2]), Double.toString(c[3]),
            Double.toString(c[4]), Double.toString(Panorama.PITCH), "220", Integer.toString(w), Integer.toString(h), "75"});
      }
   }
}
