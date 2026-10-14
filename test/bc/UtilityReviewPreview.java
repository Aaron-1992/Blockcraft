package bc;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import javax.imageio.ImageIO;

/** Review evidence from actual native tile dispatch. Not concept art. */
public final class UtilityReviewPreview {
   public static void main(String[] args)throws Exception {
      ResourcePacks.select(ResourcePacks.ID_DEFAULT,null);Tex.initPixels();
      for(int id:Creative.tab(Creative.TOOLS))System.out.println(id+" : "+Items.tile(id)+" : "+Items.name(id));
      BufferedImage before=ImageIO.read(new File(args[0]));
      ArrayList<Integer> utility=new ArrayList<>(),tools=new ArrayList<>();
      for(int t=0;t<1024;t++){
         if(TexUtility32.handles(t))utility.add(t);
         if(TexTools32.handles(t))tools.add(t);
      }
      sheet(before,utility,args[1]+"-utility.png","DECORATIONS / UTILITY / MATERIALS — UNRELEASED REVIEW");
      sheet(before,tools,args[1]+"-tools.png","TOOLS & COMBAT — UNRELEASED REVIEW");
      GuiRaster r=new GuiRaster(1080,440);r.useNativeAtlas();r.fill(0xFF252B29);r.begin(1080,440,1);
      int[] ids={Blocks.CHEST,Blocks.FURNACE,Blocks.FURNACE_LIT,Blocks.TORCH,Blocks.DRAGON_EGG,Blocks.END_FRAME_EYE,
         Redstone319.LEVER_OFF,Redstone319.LEVER_ON,Redstone319.REPEATER_OFF,Redstone319.REDSTONE_TORCH,
         Redstone319.STONE_BUTTON,Redstone319.WOOD_BUTTON,Redstone319.DAYLIGHT_SENSOR,Redstone319.DAYLIGHT_SENSOR_ON,
         Redstone319.OBSERVER,Redstone319.OBSERVER_VERTICAL,Decor315.BANNER_ITEM0+14,Decor315.BANNER_ITEM0+11};
      for(int i=0;i<ids.length;i++)r.icon(ids[i],20+i%6*176,12+i/6*142,110);
      r.save(args[1]+"-icons.png");
   }
   private static String label(int t){
      for(int id:Creative.tab(Creative.TOOLS))if(Items.tile(id)==t)return Items.name(id);
      return "Atlas "+t;
   }
   private static void sheet(BufferedImage before,ArrayList<Integer> tiles,String file,String title)throws Exception {
      int w=1200,h=52+(tiles.size()+4)/5*136;
      BufferedImage out=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);Graphics2D g=out.createGraphics();
      g.setColor(new Color(0x252B29));g.fillRect(0,0,w,h);g.setColor(new Color(0xECE5D3));g.setFont(new Font("SansSerif",Font.BOLD,18));g.drawString(title,16,27);
      g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
      for(int i=0;i<tiles.size();i++){
         int t=tiles.get(i),x=16+i%5*238,y=52+i/5*136;
         g.setColor(new Color(0xECE5D3));g.setFont(new Font("SansSerif",Font.PLAIN,13));g.drawString(label(t)+" ["+t+"]",x,y);
         BufferedImage current=new BufferedImage(32,32,BufferedImage.TYPE_INT_ARGB);current.setRGB(0,0,32,32,BuildingBlockReviewPreview.pixels(t),0,32);
         g.drawImage(before.getSubimage(t%32*32,t/32*32,32,32),x,y+7,96,96,null);g.drawImage(current,x+114,y+7,96,96,null);
         g.setColor(new Color(0xADB8AC));g.drawString(t>=760?"New component":"Previous review",x,y+120);g.drawString("Working redraw",x+114,y+120);
      }
      g.dispose();ImageIO.write(out,"png",new File(file));
   }
}
