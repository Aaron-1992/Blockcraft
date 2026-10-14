package bc;

import java.io.*;
import java.util.*;

/** Reproducible coverage ledger, including model-only and mod texture cells. */
public final class TextureAudit {
   public static void main(String[] args) throws Exception {
      ResourcePacks.select(ResourcePacks.ID_DEFAULT, null);
      Tex.initPixels();
      if(args.length>0 && args[0].equals("gallery")) {
         int[] blocks=AllBlocksGen.getShowcaseBlocks();
         for(int i=0;i<blocks.length;i++)System.out.println(Blocks.name[blocks[i]]+"\t"+(-27+i%28*2)+",66,"+(4+i/28*3));
         return;
      }
      String[] names = new String[1024];
      for (int id = 1; id < Blocks.MAX; id++) if (Blocks.name[id] != null && Blocks.tiles[id] != null)
         for (int t : Blocks.tiles[id]) if (t >= 0 && t < 1024 && names[t] == null) names[t] = Blocks.name[id];
      for (int id = 256; id < Items.name.length; id++) if (Items.name[id] != null) names[Items.tile[id]] = Items.name[id];
      for(Class<?> cl:new Class<?>[]{Tex.class,TexArt313.class,TexArt315.class,TexPaint319.class})
         for(java.lang.reflect.Field f:cl.getDeclaredFields())if(!java.lang.reflect.Modifier.isPrivate(f.getModifiers())&&f.getType()==int.class&&java.lang.reflect.Modifier.isStatic(f.getModifiers())&&java.lang.reflect.Modifier.isFinal(f.getModifiers())) {
            int t=f.getInt(null);if(t>=0&&t<1024&&names[t]==null)names[t]=f.getName();
         }
      Properties approved=new Properties();
      try(InputStream in=new FileInputStream("test/pixel32-approved.properties")){approved.load(in);}
      PrintWriter out=args.length>0?new PrintWriter(args[0],"UTF-8"):new PrintWriter(System.out);
      out.println("tile\tlabel\tartist\tchanged_texels_vs_6.4\tunique_colors");
      int kept=0,redrawn=0;
      for (int t = 0; t < 1024; t++) {
         int[] src = TexOverhaulV57.readTile(t); boolean visible=false;
         for (int c : src) if ((c >>> 24) != 0) visible=true;
         if(!visible)continue;
         boolean keep=approved.containsKey(Integer.toString(t));
         if(!keep&&t!=Tex.WHITE&&!TexRemaining32.handles(t))throw new AssertionError("Uncovered tile "+t);
         int[] art=t==Items.tile[Items.BOW]?BowSprite.pixels(0,false):TexPixel32.tile(t,src,TexRemaining32.animated(t));
         int[] before=keep||t==Tex.WHITE?art:TexRemaining32.previous(t);
         int changed=0;Set<Integer> colors=new HashSet<>();
         for(int y=0;y<32;y++)for(int x=0;x<32;x++){int c=art[y*32+x];if(!keep&&c!=before[y*32+x])changed++;colors.add(c);}
         out.println(t+"\t"+(names[t]==null?"Model-only face / icon":names[t])+"\t"+(keep?"approved-6.3":t==Tex.WHITE?"utility-white":TexColorNature32.handles(t)?"colors-nature-review-unreleased":TexBuildingReview32.handles(t)?"building-review-unreleased":TexMaterials32.handles(t)?"material-6.5":TexRemaining32.item(t)?"item-6.5":"nature-6.5")+"\t"+changed+"\t"+colors.size());
         if(keep)kept++;else if(t!=Tex.WHITE)redrawn++;
      }
      out.flush();if(args.length>0)out.close();
      System.err.println("Coverage: "+kept+" approved unchanged, "+redrawn+" newly painted, 1 utility white. No uncovered populated cells.");
   }
}
