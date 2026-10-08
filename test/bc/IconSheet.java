package bc;

/**
 * Dev tool: draws the inventory icons of a range of blocks / items (the real Gui.icon code, in software), 16 to a row.
 *   java ... bc.IconSheet out.png first last [size]
 */
public final class IconSheet {
   public static void main(String[] a) throws Exception {
      int first = Integer.parseInt(a[1]);
      int last = Integer.parseInt(a[2]);
      int size = a.length > 3 ? Integer.parseInt(a[3]) : 24;
      java.util.ArrayList<Integer> ids = new java.util.ArrayList<>();
      for (int id = first; id <= last; id++) {
         if (Blocks.isBlockId(id) && Blocks.name[id] != null && !Blocks.name[id].startsWith("Unknown") || Items.isItem(id) && Items.name(id) != null) {
            ids.add(id);
         }
      }

      int cols = 16;
      int rows = (ids.size() + cols - 1) / cols;
      int pad = 6;
      int scale = 3;
      int w = cols * (size + pad) + pad;
      int h = rows * (size + pad) + pad;
      GuiRaster r = new GuiRaster(w * scale, h * scale);
      r.fill(0xFF8B8B8B);
      r.begin(w * scale, h * scale, scale);
      for (int i = 0; i < ids.size(); i++) {
         int x = pad + i % cols * (size + pad);
         int y = pad + i / cols * (size + pad);
         r.icon(ids.get(i), x, y, size);
      }

      r.save(a[0]);
      System.out.println("wrote " + a[0] + " with " + ids.size() + " icons");
   }
}
