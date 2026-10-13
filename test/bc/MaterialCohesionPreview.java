package bc;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;

/** Actual atlas art, contiguous repeats at exactly the same nearest-neighbour scale. */
public final class MaterialCohesionPreview {
   static final int[][] ROWS={
      {4,35,657,655,39,10,38,42,220,259,681,683},
      {4,35,656,3,40,10,37,41,670,259,680,685},
      {21,22,531,532,261,262,648,649,650,482,484,659}
   };
   public static void main(String[] args)throws Exception {
      ResourcePacks.select(ResourcePacks.ID_DEFAULT,null);Tex.initPixels();
      BufferedImage image=new BufferedImage(1200,830,BufferedImage.TYPE_INT_RGB);
      Graphics2D g=image.createGraphics();
      g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
      g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g.setColor(new Color(0x1C2421));g.fillRect(0,0,1200,830);
      label(g,"MATERIAL COHESION / NATIVE 32px",24,37,24,0xE4E0D2);
      label(g,"Same scale, same lighting, touching tiles. Approved cobble, stone bricks and oak are unchanged.",24,66,15,0xB4C0B0);
      label(g,"6.4  /  BEFORE",24,100,17,0xD0B588);panel(g,24,114,false);
      label(g,"WORKING CONSTRUCTION PASS  /  AFTER",24,450,17,0xD0B588);panel(g,24,464,true);
      label(g,"Actual game texels — no perspective scaling, image smoothing, or concept-art rendering. Not a released pack.",24,804,14,0xB4C0B0);
      g.dispose();File out=new File(args[0]);out.getParentFile().mkdirs();ImageIO.write(image,"png",out);
      int count=0;for(int t=0;t<1024;t++)if(TexMaterials32.handles(t)){boolean populated=false;for(int c:TexOverhaulV57.readTile(t))populated|=(c>>>24)!=0;if(populated)count++;}
      System.out.println("Cohesion pass: "+count+" populated construction/material cells. Wrote "+out);
   }
   static void label(Graphics2D g,String text,int x,int y,int size,int color){g.setFont(new Font("SansSerif",Font.PLAIN,size));g.setColor(new Color(color));g.drawString(text,x,y);}
   static void panel(Graphics2D g,int x0,int y0,boolean revised){
      for(int y=0;y<ROWS.length;y++)for(int x=0;x<ROWS[y].length;x++){
         int t=ROWS[y][x];int[] pixels=TexRemaining32.handles(t)?(revised?TexRemaining32.paint(t):TexRemaining32.previous(t)):TexPixel32.tile(t,TexOverhaulV57.readTile(t),false);
         BufferedImage tile=new BufferedImage(32,32,BufferedImage.TYPE_INT_ARGB);tile.setRGB(0,0,32,32,pixels,0,32);
         g.drawImage(tile,x0+x*96,y0+y*96,96,96,null);
      }
   }
}
