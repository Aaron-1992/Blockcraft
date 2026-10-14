package bc;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;

/** Review actual raster output at identical nearest-neighbour scale. */
public final class ColorNatureReviewPreview {
   public static void main(String[] args)throws Exception {
      ResourcePacks.select(ResourcePacks.ID_DEFAULT,null);Tex.initPixels();
      BufferedImage old=ImageIO.read(new File(args[0]));
      sheet(old,args[1]+"-colors.png","FIBRES / POWDER / FIRED CLAY",new int[]{48,49,54,63,416,417,429,431,432,433,438,448});
      sheet(old,args[1]+"-glazed.png","SIXTEEN GLAZED CERAMIC DESIGNS",new int[]{449,450,451,452,453,454,455,456,457,458,459,460,461,462,463,464});
      sheet(old,args[1]+"-earth.png","SOILS / SANDS / PUMPKIN & MELON",new int[]{1,5,523,524,525,528,221,222,223,224,225,226});
      sheet(old,args[1]+"-logs-ores.png","NATIVE-SCALE BARK / EMBEDDED MINERALS",new int[]{7,8,31,32,33,247,540,543,541,544,542,545,12,13,14,15,16,17,258,488});
   }
   static String name(int t){
      for(int b=1;b<Blocks.MAX;b++)if(Blocks.name[b]!=null&&Blocks.tiles[b]!=null)for(int tile:Blocks.tiles[b])if(tile==t)return Blocks.name[b]+" / "+t;
      return "Cell "+t;
   }
   static void sheet(BufferedImage old,String path,String title,int[] tiles)throws Exception {
      int rows=(tiles.length+3)/4;
      BufferedImage out=new BufferedImage(1040,rows*153+66,BufferedImage.TYPE_INT_RGB);
      Graphics2D g=out.createGraphics();g.setColor(new Color(0x202722));g.fillRect(0,0,out.getWidth(),out.getHeight());
      g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
      g.setFont(new Font("SansSerif",Font.BOLD,20));g.setColor(new Color(0xE5DFCA));g.drawString(title,18,29);
      for(int i=0;i<tiles.length;i++) {
         int t=tiles[i],x=18+i%4*258,y=50+i/4*153;
         g.setColor(new Color(0xE5DFCA));g.setFont(new Font("SansSerif",Font.PLAIN,13));g.drawString(name(t),x,y+14);
         BufferedImage tile=new BufferedImage(32,32,BufferedImage.TYPE_INT_ARGB);
         tile.setRGB(0,0,32,32,TexRemaining32.paint(t),0,32);
         g.drawImage(old.getSubimage(t%32*32,t/32*32,32,32),x,y+22,96,96,null);
         g.drawImage(tile,x+119,y+22,96,96,null);
         g.setColor(new Color(0xA8B0A1));g.drawString("V6.5",x,y+135);g.drawString("Working redraw",x+119,y+135);
      }
      g.dispose();ImageIO.write(out,"png",new File(path));
   }
}
