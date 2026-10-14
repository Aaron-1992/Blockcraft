package bc;

/** Native-texel construction tools shared by plants, objects and item sprites.
 * No rescaling, antialiasing, image-wide noise/emboss or interpolated RGB gradients.
 * Shape, material grain and restrained edge accents are drawn together at 32px.
 */
final class PixelArt32 {
   final int[] p=new int[1024];
   static final int OAK=0xA98A58, IRON=0xB4BFBB, GOLD=0xD8BB67, DIAMOND=0x63B3B3,
      STONE=0x898D85, LEATHER=0x9B704B, LEAF=0x69854C, BONE=0xD6D1BA;
   static int c(int rgb){return 0xFF000000|rgb;}
   static int shade(int rgb,int d){return TexPixel32.shade(c(rgb),d);}
   static float h(int x,int y,int seed){return TexPixel32.hash(x,y,seed);}
   static int grain(int x,int y,int seed,int step){return TexMaterials32.grain(x,y,seed,step);}
   void set(int x,int y,int color){if(x>=0&&x<32&&y>=0&&y<32)p[y*32+x]=c(color);}
   void clear(int x,int y,int w,int h){for(int yy=Math.max(0,y);yy<Math.min(32,y+h);yy++)for(int xx=Math.max(0,x);xx<Math.min(32,x+w);xx++)p[yy*32+xx]=0;}
   void rect(int x,int y,int w,int h,int color){for(int yy=y;yy<y+h;yy++)for(int xx=x;xx<x+w;xx++)set(xx,yy,color);}
   void material(int x,int y,int w,int h,int color,int seed,int step){for(int yy=y;yy<y+h;yy++)for(int xx=x;xx<x+w;xx++)set(xx,yy,shade(color,grain(xx,yy,seed,step)));}
   void line(int x,int y,int xx,int yy,int color){int dx=Math.abs(xx-x),dy=-Math.abs(yy-y),sx=x<xx?1:-1,sy=y<yy?1:-1,e=dx+dy;while(true){set(x,y,color);if(x==xx&&y==yy)break;int e2=e*2;if(e2>=dy){e+=dy;x+=sx;}if(e2<=dx){e+=dx;y+=sy;}}}
   void stroke(int x,int y,int xx,int yy,int color,int width){for(int i=0;i<width;i++)line(x+i,y,xx+i,yy,shade(color,(i==0?10:i==width-1?-14:0)));}
   void outline(int x,int y,int w,int h,int color){line(x,y,x+w-1,y,color);line(x,y+h-1,x+w-1,y+h-1,color);line(x,y,x,y+h-1,color);line(x+w-1,y,x+w-1,y+h-1,color);}
   void ellipse(int x,int y,int w,int ht,int color,int seed,boolean round){
      boolean[] mask=new boolean[1024];
      for(int yy=Math.max(0,y);yy<Math.min(32,y+ht);yy++)for(int xx=Math.max(0,x);xx<Math.min(32,x+w);xx++){
         double dx=(xx-x+.5)*2/w-1,dy=(yy-y+.5)*2/ht-1;mask[yy*32+xx]=dx*dx+dy*dy<1;
      }
      surface(mask,color,seed,round,x,y,w,ht);
   }
   void poly(int color,int seed,int... xy){
      boolean[] mask=new boolean[1024];int minx=32,miny=32,maxx=0,maxy=0;
      for(int i=0;i<xy.length;i+=2){minx=Math.min(minx,xy[i]);miny=Math.min(miny,xy[i+1]);maxx=Math.max(maxx,xy[i]);maxy=Math.max(maxy,xy[i+1]);}
      for(int y=Math.max(0,miny);y<=Math.min(31,maxy);y++)for(int x=Math.max(0,minx);x<=Math.min(31,maxx);x++){
         boolean in=false;
         for(int i=0,j=xy.length-2;i<xy.length;j=i,i+=2)if((xy[i+1]>y+.5)!=(xy[j+1]>y+.5)&&x+.5<(xy[j]-xy[i])*(y+.5-xy[i+1])/(xy[j+1]-xy[i+1])+xy[i])in=!in;
         mask[y*32+x]=in;
      }
      surface(mask,color,seed,false,minx,miny,maxx-minx+1,maxy-miny+1);
   }
   private void surface(boolean[] mask,int col,int seed,boolean round,int x0,int y0,int w,int ht){
      for(int y=0;y<32;y++)for(int x=0;x<32;x++)if(mask[y*32+x]){
         int d=grain(x,y,seed,3);
         boolean top=y==0||!mask[(y-1)*32+x],left=x==0||!mask[y*32+x-1];
         boolean bottom=y==31||!mask[(y+1)*32+x],right=x==31||!mask[y*32+x+1];
         if(top||left)d+=10;if(bottom||right)d-=17;
         if(round)d+=((x-x0)*2+y-y0<w?9:(x-x0)*2+y-y0>w+ht/2?-9:0);
         set(x,y,shade(col,d));
      }
   }
   void wood(int x,int y,int w,int ht,int species,boolean vertical){
      int[] timber=TexMaterials32.timber(species);
      for(int yy=y;yy<y+ht;yy++)for(int xx=x;xx<x+w;xx++)set(xx,yy,timber[vertical?(xx&31)*32+(yy&31):(yy&31)*32+(xx&31)]);
   }
   void leaf(int x,int y,int dx,int dy,int color,int seed){
      poly(color,seed,x,y,x+dx/2-dy/3,y+dy/2+dx/3,x+dx,y+dy,x+dx/2+dy/3,y+dy/2-dx/3);
      line(x,y,x+dx,y+dy,shade(color,-13));
   }
   static int wool(int i){int[] w=Tex.WOOL[Math.floorMod(i,16)];return w[0]<<16|w[1]<<8|w[2];}
}
