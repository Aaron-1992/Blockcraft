package bc;

/** Colors/nature review, native 32px. Only user-reopened texture families belong
 * here. Terrain grains and bark accents are authored at ONE texel, never scaled
 * masks. Glaze pigments are flat inlaid designs, not embossed shapes/noise. */
final class TexColorNature32 {
   private TexColorNature32() {}
   static boolean log(int t){return t==7||t==8||t==31||t==32||t==33||t>=246&&t<=248||t>=540&&t<=548;}
   static boolean wasProtected(int t){return t==1||t==5||t==523||log(t)||t>=12&&t<=17||t>=48&&t<=63;}
   static boolean handles(int t){return wasProtected(t)||t>=416&&t<=464||t>=524&&t<=525||t==528||t==258||t==488||t>=221&&t<=226;}
   static int[] paint(int t) {
      if(!handles(t))return null;
      if(t>=48&&t<=63)return wool(t-48);
      if(t>=416&&t<=431)return granular(Color312.CONCRETE[t-416],t,true);
      if(t>=432&&t<=448)return clay(t==432?Color312.PLAIN_TERRA:Color312.TERRA[t-433]);
      if(t>=449&&t<=464)return TexGlazedReview32.paint(t-449);
      if(t==5||t==523)return granular(t==5?0xD9C698:0xBF783B,71,false);
      if(t==1||t==528)return soil(t==528);
      if(t==524||t==525)return podzol(t==525);
      if(log(t))return timber(t);
      if(t>=12&&t<=17||t==258||t==488)return ore(t);
      return gourd(t);
   }
   private static float h(int x,int y,int s){return TexPixel32.hash(x,y,s);}
   private static int shade(int c,int d){return PixelArt32.shade(c,d);}
   private static int mix(int a,int b,int parts){return (((a>>16&255)*(8-parts)+(b>>16&255)*parts)/8)<<16|(((a>>8&255)*(8-parts)+(b>>8&255)*parts)/8)<<8|((a&255)*(8-parts)+(b&255)*parts)/8;}
   private static void dot(PixelArt32 a,int x,int y,int c){a.set(Math.floorMod(x,32),Math.floorMod(y,32),c);}

   private static int[] wool(int dye) {
      PixelArt32 a=new PixelArt32();int c=PixelArt32.wool(dye);
      for(int y=0;y<32;y++)for(int x=0;x<32;x++)a.set(x,y,shade(c,(int)(h(x,y,481)*5)-2));
      // Short hooked fibres, staggered and rotated, not a checkerboard weave.
      for(int row=0;row<8;row++)for(int col=0;col<8;col++) {
         int x=col*4+(row&1)+(int)(h(col,row,482)*2),y=row*4+(int)(h(col,row,483)*2);
         int rot=(int)(h(col,row,484)*4)%4;
         int[][] curl={{0,0},{1,-1},{2,-1},{3,0},{2,1},{1,1}};
         for(int k=0;k<curl.length;k++) {
            int dx=curl[k][0],dy=curl[k][1];
            for(int r=0;r<rot;r++){int old=dx;dx=-dy;dy=old;}
            dot(a,x+dx,y+dy,shade(c,k<3?9:k==3?3:-11));
         }
         if(h(col,row,485)>.5)dot(a,x-1,y+2,shade(c,-5));
      }
      return a.p;
   }
   private static int[] granular(int base,int seed,boolean powder) {
      PixelArt32 a=new PixelArt32();int c=powder?mix(base,0xCBB999,1):base;
      for(int y=0;y<32;y++)for(int x=0;x<32;x++) {
         float r=h(x,y,seed);int d=r<.16F?-12:r<.36F?-5:r>.86F?11:r>.67F?5:0;
         // Low, discontinuous wind ripples beneath independent 1px grains.
         int drift=(y+(int)(2*Math.sin(x*Math.PI/16)))&15;
         if(!powder&&drift==4)d-=3;
         if(powder&&h(x,y,seed+1)>.965F)d-=6;
         a.set(x,y,shade(c,d));
      }
      return a.p;
   }
   private static int[] clay(int c) {
      PixelArt32 a=new PixelArt32();
      for(int y=0;y<32;y++)for(int x=0;x<32;x++) {
         // Quiet kiln clouds and curved compression marks, rather than loose grit.
         double cloud=Math.sin((x+y*.4)*Math.PI/16)+.6*Math.cos((y-x*.35)*Math.PI/16);
         int d=(int)Math.floor(cloud*4);
         int band=(y+(int)(2*Math.sin(x*Math.PI/16))+32)%16;
         if(band==10&&x>4&&x<25)d-=4;
         d+=(int)(h(x,y,432)*3)-1;
         a.set(x,y,shade(c,d));
      }
      return a.p;
   }
   private static int[] soil(boolean coarse) {
      PixelArt32 a=new PixelArt32();
      int[] p=coarse?new int[]{0x674A34,0x77543A,0x846044,0x936E4C,0xA17C56}:new int[]{0x715039,0x7A583E,0x846045,0x8D694A,0x987351};
      for(int y=0;y<32;y++)for(int x=0;x<32;x++) {
         float r=h(x,y,81);int k=r<.13F?0:r<.34F?1:r>.94F?4:r>.75F?3:2;
         a.set(x,y,p[k]);
         // Fine aggregate only: no grey 2x2 or 3x3 pebble stamps.
         if(coarse&&h(x,y,82)>.96F)a.set(x,y,0xB18B64);
      }
      return a.p;
   }
   private static int[] podzol(boolean side) {
      PixelArt32 a=new PixelArt32();int[] dirt=soil(false);
      for(int y=0;y<32;y++)for(int x=0;x<32;x++)a.set(x,y,shade(0x695034,(int)(h(x,y,524)*17)-8));
      int[] litter={0xA87943,0x896139,0xB18953,0x4C3D2B,0x7E7848};
      for(int i=0;i<95;i++) {
         int x=(int)(h(i,0,525)*32),y=(int)(h(i,1,525)*32),c=litter[i%5];
         for(int k=0;k<2+i%3;k++)dot(a,x+k,y+(i%3==0?k:-(k/2)),c);
         if(i%7==0)dot(a,x+1,y+1,shade(c,-9));
      }
      if(side)for(int y=0;y<32;y++)for(int x=0;x<32;x++) {
         int depth=8+(int)(h(x/2,0,526)*4);
         if(y>=depth)a.p[y*32+x]=dirt[y*32+x];
         if(y==depth)a.p[y*32+x]=shade(dirt[y*32+x],-14);
      }
      return a.p;
   }

   private static final int[] BARK={0x705132,0x503C2B,0xD6D4C4,0x75613E,0x777165,0x49372A};
   private static final int[] HEART={0xC3A06B,0xAA8854,0xD6C69D,0xAE845C,0xBE7543,0x775333};
   private static int[] timber(int t) {
      int species=t==31||t==32||t==248?1:t==33||t==247?2:t>=540&&t<=548?(t-540)%3+3:0;
      boolean end=t==8||t==32||t>=543&&t<=545,turned=t>=246&&t<=248||t>=546&&t<=548;
      int[] src=end?endGrain(species):bark(species);
      if(!turned)return src;
      int[] out=new int[1024];for(int y=0;y<32;y++)for(int x=0;x<32;x++)out[y*32+x]=src[x*32+y];return out;
   }
   private static int[] bark(int species) {
      PixelArt32 a=new PixelArt32();int base=BARK[species];
      for(int y=0;y<32;y++)for(int x=0;x<32;x++)a.set(x,y,shade(base,(int)(h(x,y,710+species)*7)-3));
      if(species==2) {
         // Paper birch: narrow lenticels, tapered tears and peeling cream strips.
         for(int i=0;i<23;i++) {
            int x=(int)(h(i,0,720)*32),y=(int)(h(i,1,720)*32),w=2+i%5;
            for(int k=0;k<w;k++) {
               dot(a,x+k,y, i%4==0?0x726F61:0x494A43);
               if(k>0&&k<w-1&&i%3==0)dot(a,x+k,y+1,0x5B5B51);
               if(i%2==0)dot(a,x+k,y-1,0xEBE7D8);
            }
            if(i%5==0){dot(a,x,y-1,0xB0AD9E);dot(a,x-1,y-2,0xB0AD9E);}
         }
         // Two tapered bark tears with thin curled paper above exposed dark bark.
         a.line(9,8,17,8,0xEBE7D8);a.line(10,9,17,9,0x55564D);a.line(12,10,16,10,0x464940);
         a.line(18,20,27,20,0xB2B1A1);a.line(20,21,27,21,0x484A42);a.line(22,22,25,22,0x5A5B51);
         for(int y=3;y<32;y+=11)for(int x=0;x<32;x++)if(h(x/6,y,722)>.4)dot(a,x,y+(x/9)%2,0xBCBCAB);
      } else {
         // Narrow, wandering furrows with broken native-scale edge highlights.
         int count=species==1?10:species==5?7:8;
         for(int i=0;i<count;i++)for(int y=0;y<32;y++) {
            int x=(int)(i*32F/count)+(int)Math.round(Math.sin(y*Math.PI/16+i*1.7)*(species==3?2:1));
            dot(a,x,y,shade(base,-22));
            if((y+i*3)%13<9)dot(a,x+1,y,shade(base,13));
            if(species==5&&(y+i)%7<4)dot(a,x-1,y,shade(base,-10));
            if(species==4&&y%9<2){dot(a,x+2,y,shade(base,-11));dot(a,x+3,y,shade(base,-9));}
         }
         // Small asymmetric knot scars. These are bark features, not scaled pixels.
         for(int i=0;i<3;i++) {
            int x=4+(int)(h(i,0,732+species)*23),y=3+(int)(h(i,1,732+species)*25);
            a.line(x,y-2,x-1,y,shade(base,-19));a.line(x-1,y,x,y+3,shade(base,-19));
            a.line(x+1,y-1,x+2,y+1,shade(base,9));
         }
      }
      return a.p;
   }
   private static int[] endGrain(int species) {
      int[] p=bark(species);int c=HEART[species];
      for(int y=2;y<30;y++)for(int x=2;x<30;x++) {
         double dx=x-14.5,dy=(y-16.5)*.92,r=Math.hypot(dx,dy)+.45*Math.sin(Math.atan2(dy,dx)*5);
         int ring=(int)Math.floor(r)%4,d=ring==0?-16:ring==1?5:0;
         d+=(int)(h(x,y,743)*5)-2;
         if(x>=15&&x<=16&&y>22)d-=14;
         p[y*32+x]=shade(c,d);
      }
      return p;
   }

   private static int[] ore(int t) {
      int[] host=t==258?TexMaterials32.paint(256):TexMaterials32.base(Tex.STONE);
      PixelArt32 a=new PixelArt32();System.arraycopy(host,0,a.p,0,1024);
      int c=t==12?0x343535:t==13?0xB58D72:t==14?0xCBA64D:t==15?0x5AB9BD:t==16?0xA74332:t==17?0x3F5894:t==488?0x369267:0xD7CCB1;
      boolean[] seam=new boolean[1024];
      int[][] starts={{3,4},{17,2},{25,10},{10,13},{3,25},{19,24},{27,29}};
      for(int i=0;i<starts.length;i++) {
         int x=starts[i][0],y=starts[i][1],dir=(i+t)%2==0?1:-1;
         for(int k=0;k<7+i%3;k++) {
            x=Math.floorMod(x+dir,32);y=(y+(h(i,k,t)>.5?1:0))&31;
            seam[y*32+x]=true;
            if(k>0&&k<7)seam[((y+1)&31)*32+x]=true;
            if(k>1&&k<5)seam[((y+2)&31)*32+x]=true;
            if(k==2||k==4)seam[((y-1)&31)*32+((x+dir)&31)]=true;
         }
      }
      for(int y=0;y<32;y++)for(int x=0;x<32;x++) {
         int i=y*32+x;
         if(seam[i]) {
            int d=(int)(h(x,y,t+91)*4)*6-12;
            if(y>0&&!seam[i-32])d-=7; // mineral sits inside a fracture, not on a raised rim
            a.p[i]=shade(c,d);
         } else if(y>0&&seam[i-32]&&h(x,y,753)>.45)a.p[i]=shade(host[i],-9);
      }
      return a.p;
   }

   private static int[] gourd(int t) {
      PixelArt32 a=new PixelArt32();boolean melon=t>=225,top=t==221||t==225;
      for(int y=0;y<32;y++)for(int x=0;x<32;x++) {
         int c,d;
         if(melon) {
            int wave=(int)Math.round(Math.sin(y*Math.PI/16)*1.5+Math.sin(y*Math.PI/8+x/7.)*.5);
            int band=Math.floorMod((top?y:x)+wave,9);
            c=band<2?0x486D2C:band==2?0x648537:band==8?0x769944:0x9AB650;
            d=(int)(h(x,y,225)*9)-4;
            if(h(x,y,226)>.92)c=band<3?0x789645:0xB0C15B;
         } else {
            double angle=Math.atan2(y-15.5,x-15.5);
            int rib=top?(int)Math.round((angle+Math.PI)*6):x+(int)Math.round(Math.sin(y*Math.PI/16+x/8.)*1.1);
            int band=Math.floorMod(rib,7);
            c=band==0?0x9B5722:band==1?0xB46924:band==6?0xD18A35:0xC77A29;
            if(top){double r=Math.hypot(x-15.5,y-15.5),ridge=Math.abs(Math.sin(angle*4+r*.07))*r/4;
               c=ridge<.45?0xAD6526:ridge<1?0xBA7128:ridge<1.4?0xD38A36:0xC77A29;}
            d=(int)(h(x,y,221)*9)-4;
         }
         a.set(x,y,shade(c,d));
      }
      if(top) {
         if(!melon){a.poly(0x756239,221,12,12,18,11,21,15,18,19,13,20,11,16);a.line(14,13,17,16,0x97834D);a.line(17,12,18,16,0x514D2D);}
         else{a.line(14,14,17,17,0x7D9442);a.set(17,15,0xB5C573);}
      }
      if(t==223||t==224) {
         int shadow=t==224?0xDDA54A:0x593A20,inside=t==224?0xF5D078:0x352B20;
         a.poly(shadow,223,5,8,12,10,12,16,5,14);a.poly(shadow,224,20,10,27,8,27,14,20,16);
         a.rect(6,11,5,3,inside);a.rect(21,11,5,3,inside);
         a.poly(shadow,225,5,21,10,21,10,24,14,24,14,21,18,21,18,24,22,24,22,21,27,21,25,28,7,28);
         a.line(8,26,24,26,inside);
      }
      return a.p;
   }
}
