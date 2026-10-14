package bc;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Arrays;
import javax.imageio.ImageIO;

/** Actual texels at nearest-neighbour scale, not a generated concept image. */
public final class BuildingBlockReviewPreview {
   static final int[] TILES={522,650,44,656,651,653,655,261,262,684,685,487,257,264,485,486};
   static final String[] LABELS={"Andesite","Polished andesite","Mossy cobble","Mossy stone bricks",
      "Chiseled sandstone","Chiseled red sandstone","Chiseled stone","Quartz sides","Quartz ends",
      "Purpur pillar ends","Purpur pillar sides","Lapis block","Soul sand","End stone","Emerald block","Coal block"};
   static int[] pixels(int t){return t==77?BowSprite.pixels(0,false):TexPixel32.tile(t,TexOverhaulV57.readTile(t),TexRemaining32.animated(t));}
   public static void main(String[] args)throws Exception {
      ResourcePacks.select(ResourcePacks.ID_DEFAULT,null);Tex.initPixels();
      if(args[0].equals("baseline")) {
         BufferedImage atlas=new BufferedImage(1024,1024,BufferedImage.TYPE_INT_ARGB);
         try(PrintWriter out=new PrintWriter(args[1],"UTF-8")) {
            out.println("# V6.5 published atlas: Arrays.hashCode of all 1024 native tile arrays. Do not regenerate from revised art.");
            for(int t=0;t<1024;t++){int[] p=pixels(t);out.println(t+"="+Arrays.hashCode(p));atlas.setRGB(t%32*32,t/32*32,32,32,p,0,32);}
         }
         ImageIO.write(atlas,"png",new File(args[2]));return;
      }
      BufferedImage before=ImageIO.read(new File(args[1]));
      BufferedImage out=new BufferedImage(1040,680,BufferedImage.TYPE_INT_RGB);
      Graphics2D g=out.createGraphics();g.setColor(new Color(0x202722));g.fillRect(0,0,1040,680);
      g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
      g.setFont(new Font("SansSerif",Font.BOLD,20));g.setColor(new Color(0xE5DFCA));g.drawString("BUILDING BLOCK REVIEW — V6.5 / WORKING REDRAW",18,29);
      for(int i=0;i<TILES.length;i++){
         int t=TILES[i],x=18+i%4*258,y=50+i/4*153;
         g.setFont(new Font("SansSerif",Font.PLAIN,14));g.setColor(new Color(0xE5DFCA));g.drawString(LABELS[i],x,y+14);
         BufferedImage tile=new BufferedImage(32,32,BufferedImage.TYPE_INT_ARGB);tile.setRGB(0,0,32,32,pixels(t),0,32);
         g.drawImage(before.getSubimage(t%32*32,t/32*32,32,32),x,y+22,96,96,null);
         g.drawImage(tile,x+119,y+22,96,96,null);
         g.setColor(new Color(0xA8B0A1));g.drawString("V6.5",x,y+135);g.drawString("Working redraw",x+119,y+135);
      }
      g.dispose();ImageIO.write(out,"png",new File(args[2]));
   }
}
