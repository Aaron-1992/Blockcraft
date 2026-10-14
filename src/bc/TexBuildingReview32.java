package bc;

/** Targeted follow-up to the user's V6.5 building-tab review. Only the listed
 * materials change. Every feature is rasterized on the same native 32px grid. */
final class TexBuildingReview32 {
   private TexBuildingReview32() {}

   static boolean handles(int t) {
      switch(t) {
         case 44: case 257: case 261: case 262: case 264: case 485: case 486:
         case 487: case 522: case 650: case 651: case 653: case 655: case 656:
         case 684: case 685: return true;
         default: return false;
      }
   }
   static int[] paint(int t) {
      if(!handles(t))return null;
      PixelArt32 a=new PixelArt32();
      switch(t) {
         case 522: case 650: andesite(a,t==650);break;
         case 44: case 656: moss(a,t==656);break;
         case 651: case 653: case 655: carved(a,t);break;
         case 261: case 262: quartz(a,t==262);break;
         case 684: case 685: purpur(a,t==684);break;
         case 487: lapis(a);break;
         case 257: souls(a);break;
         case 264: endStone(a);break;
         case 485: emerald(a);break;
         case 486: coal(a);break;
      }
      return a.p;
   }
   private static float h(int x,int y,int seed){return PixelArt32.h(x,y,seed);}
   private static int shade(int c,int d){return PixelArt32.shade(c,d);}
   private static void ground(PixelArt32 a,int color,int seed,int step) {
      a.material(0,0,32,32,color,seed,step);
   }
   /** Porphyritic rock: small angular feldspar laths in a cooler volcanic matrix.
    * The polished variant keeps the same crystals, not a generic grey panel. */
   private static void andesite(PixelArt32 a,boolean polished) {
      ground(a,0x777D7B,812,4);
      for(int i=0;i<55;i++) {
         int x=(int)(h(i,0,810)*32),y=(int)(h(i,1,810)*32);
         int w=1+i%3,ht=1+i%2,c=i%4==0?0x535E5C:i%3==0?0x989F99:0xA3A9A2;
         for(int oy=-32;oy<=32;oy+=32)for(int ox=-32;ox<=32;ox+=32) {
            int xx=x+ox,yy=y+oy;
            int[] shape={0,0,w,0,w+1,ht,1,ht+1};
            for(int j=0;j<shape.length;j+=2) {
               int dx=shape[j],dy=shape[j+1];
               for(int r=0;r<i%4;r++){int old=dx;dx=-dy;dy=old;}
               shape[j]=xx+dx;shape[j+1]=yy+dy;
            }
            a.poly(c,811+i,shape);
         }
      }
      if(polished)for(int y=0;y<32;y++)for(int x=0;x<32;x++) {
         int c=a.p[y*32+x],r=c>>16&255,g=c>>8&255,b=c&255;
         int e=Math.min(Math.min(x,31-x),Math.min(y,31-y));
         int d=e==0?-13:x==1||y==1?8:0;
         a.set(x,y,shade(((r*3+135)/4)<<16|((g*3+141)/4)<<8|(b*3+138)/4,d));
      }
   }
   /** Fine frayed growth following joints, not a hash of oversized square stamps. */
   private static void moss(PixelArt32 a,boolean bricks) {
      int[] stone=TexMaterials32.base(bricks?Tex.STONEBRICK:Tex.COBBLE);
      System.arraycopy(stone,0,a.p,0,1024);
      int[] growth=new int[1024];
      for(int i=0;i<22;i++) {
         int x=(int)(h(i,0,442)*32),y=(int)(h(i,1,442)*32);
         // Root near a recess in the actual underlying masonry.
         int best=255,rootX=x,rootY=y;
         for(int dy=-2;dy<=2;dy++)for(int dx=-2;dx<=2;dx++) {
            int xx=(x+dx)&31,yy=(y+dy)&31,v=stone[yy*32+xx]&255;
            if(v<best){best=v;rootX=xx;rootY=yy;}
         }
         x=rootX;y=rootY;growth[y*32+x]=1;
         for(int j=0;j<7+i%5;j++) {
            x=(x+(h(i,j,443)>.5F?1:-1))&31;
            y=(y+(j%3==0?-1:0))&31;
            growth[y*32+x]=2;
            if(h(i,j,445)>.35F)growth[((y+1)&31)*32+x]=1;
            if(j%3==1)growth[((y-1)&31)*32+((x+1)&31)]=3;
         }
      }
      for(int y=0;y<32;y++)for(int x=0;x<32;x++) {
         int n=growth[y*32+x];if(n==0)continue;
         int c=n==1?0x4F6540:n==2?0x657B48:0x8D9A5A;
         // Recesses stay darker, helping moss sit on the stone instead of float.
         int d=(stone[y*32+x]&255)<90?-10:0;
         a.set(x,y,shade(c,d+(h(x,y,447)>.7?4:0)));
      }
   }
   /** Carve a native binary mask into the substrate with one-pixel incised rims. */
   private static void incise(PixelArt32 a,PixelArt32 mask) {
      for(int y=0;y<32;y++)for(int x=0;x<32;x++) {
         int i=y*32+x;
         if(mask.p[i]!=0) a.p[i]=shade(a.p[i],-36);
         else if(y>0&&mask.p[i-32]!=0||x>0&&mask.p[i-1]!=0) a.p[i]=shade(a.p[i],13);
      }
   }
   private static void carved(PixelArt32 a,int t) {
      ground(a,t==651?0xCEC099:t==653?0xB47A50:TexMaterials32.STONE,900+t,3);
      PixelArt32 m=new PixelArt32();
      if(t==651) {
         // Winged scarab relief, with a cut lintel and separate lower inscription.
         m.rect(0,3,32,1,1);m.rect(0,27,32,1,1);
         m.poly(1,1,14,9,17,9,20,13,18,21,15,24,12,21,11,13);
         for(int side:new int[]{-1,1}) {
            m.line(15+side*3,12,15+side*11,8,1);
            m.line(15+side*3,15,15+side*10,13,1);
            m.line(15+side*3,18,15+side*8,18,1);
            m.line(15+side*2,9,15+side*4,6,1);
         }
         m.rect(7,29,3,1,1);m.rect(13,29,5,1,1);m.rect(22,29,3,1,1);
      } else if(t==653) {
         // Rising sun above asymmetric stepped dunes; not the sandstone motif recolored.
         m.poly(1,1,12,10,14,7,18,7,21,10,21,14,17,17,13,16,11,13);
         m.line(16,2,16,4,1);m.line(6,5,9,8,1);m.line(25,5,23,8,1);
         m.line(4,12,8,12,1);m.line(25,12,28,12,1);
         int[] line={2,23,7,23,11,19,16,24,23,20,29,20};path(m,line,1);
         path(m,new int[]{2,27,8,27,12,24,17,28,24,24,29,24},1);
         m.rect(0,30,32,1,1);
      } else {
         // Watcher/avian relief: brows, eye hollows, beak and separate feathers.
         // No brick courses, enclosed central ring or reused sandstone glyph.
         m.rect(0,2,32,1,1);m.rect(0,29,32,1,1);
         path(m,new int[]{4,9,10,6,14,9},1);
         path(m,new int[]{18,9,22,6,28,9},1);
         m.poly(1,1,7,11,12,12,12,16,8,15);
         m.poly(1,2,20,12,25,11,24,15,20,16);
         m.poly(1,3,16,13,19,18,16,23,13,18);
         path(m,new int[]{4,17,6,22,11,26},1);
         path(m,new int[]{28,17,26,22,21,26},1);
         path(m,new int[]{8,19,10,22,13,24},1);
         path(m,new int[]{24,19,22,22,19,24},1);
      }
      incise(a,m);
   }
   private static void path(PixelArt32 a,int[] points,int color) {
      for(int i=2;i<points.length;i+=2)a.line(points[i-2],points[i-1],points[i],points[i+1],color);
   }
   /** Milky cut quartz: a continuous pale mineral face, not longitudinal bone striations. */
   private static void quartz(PixelArt32 a,boolean end) {
      ground(a,0xE6E3DA,261,2);
      for(int i=0;i<5;i++) {
         int x=i*8-5,y=(int)(h(i,0,262)*28);
         for(int k=0;k<9;k++) {
            int xx=Math.floorMod(x+k,32),yy=Math.floorMod(y+k/3+(end?3:0),32);
            a.set(xx,yy,0xD7D8D0);
            if(k>3&&k<7)a.set(xx,(yy+1)&31,0xDDDCD3);
         }
      }
      for(int x=0;x<32;x++){a.set(x,0,0xF1EEE5);a.set(x,31,0xCDCEC6);}
      for(int y=1;y<31;y++){a.set(0,y,0xEDEAE1);a.set(31,y,0xD5D5CC);}
   }
   /** A fluted column with a chamfered cross-section, rather than bone-colored stripes/rings. */
   private static void purpur(PixelArt32 a,boolean end) {
      ground(a,0xAA909F,684,2);
      if(!end) {
         int[] flute={-9,-22,-29,-21,-8,4,11,6,0,0};
         for(int y=0;y<32;y++)for(int x=0;x<32;x++) {
            int k=Math.floorMod(x-1,10),d=flute[k];
            a.set(x,y,shade(0xAE93A4,d+TexMaterials32.grain(x,y,685,2)));
         }
      } else {
         a.poly(0xC1A7B6,684,0,0,32,0,25,7,7,7);
         a.poly(0x8A6F82,685,32,0,32,32,25,25,25,7);
         a.poly(0x95798D,686,0,32,32,32,25,25,7,25);
         a.poly(0xB99BAB,687,0,0,7,7,7,25,0,32);
         a.poly(0xAE93A4,688,7,7,25,7,25,25,7,25);
         // The column channels terminate as shallow scallops on the cut end.
         for(int x:new int[]{5,15,25}){a.line(x,0,x,3,0x85697E);a.line(x+1,0,x+1,2,0xD0B4C3);}
         for(int y:new int[]{5,15,25}){a.line(29,y,31,y,0x85697E);a.line(0,y,3,y,0xC8ADBB);}
      }
   }
   /** Deep ultramarine with broken blue mineral veins. No gold/yellow dot stamps. */
   private static void lapis(PixelArt32 a) {
      ground(a,0x304C8B,487,3);
      a.poly(0x334F8D,487,0,0,12,0,17,5,12,11,4,13,0,9);
      a.poly(0x2A457F,488,15,13,23,10,31,13,31,23,25,29,16,25,13,19);
      int[][] veins={{-2,8,4,6,9,7,14,4,19,5,23,2,31,3},
         {0,23,6,25,11,21,17,22,22,19,28,20,33,17}};
      for(int[] q:veins){path(a,q,0x243B72);int[] high=q.clone();for(int i=1;i<high.length;i+=2)high[i]--;for(int i=2;i<high.length;i+=4)a.line(high[i-2],high[i-1],high[i],high[i+1],0x405D95);}
      path(a,new int[]{9,7,9,11,14,14,18,14},0x3A5893);
      path(a,new int[]{22,19,24,24,29,27},0x3A5893);
   }
   private static void souls(PixelArt32 a) {
      ground(a,0x79614D,257,5);
      soul(a,1,1,11,15);soul(a,18,4,12,17);soul(a,6,20,11,13);
   }
   /** Eroded brow, paired eye hollows and stretched mouths pressed into the sand. */
   private static void soul(PixelArt32 a,int x,int y,int w,int ht) {
      a.poly(0x68513F,257,x+2,y,x+w-3,y,x+w,y+4,x+w-2,y+ht-4,x+w/2,y+ht,x+2,y+ht-3,x,y+5);
      a.line(x+2,y,x+w-4,y,0x9A8061);
      a.line(x,y+5,x+2,y+ht-4,0x927357);
      a.poly(0x392F29,258,x+2,y+4,x+5,y+3,x+5,y+6,x+2,y+7);
      a.poly(0x40312A,259,x+w-5,y+3,x+w-2,y+5,x+w-3,y+7,x+w-5,y+6);
      a.poly(0x3D3028,260,x+4,y+8,x+w-4,y+7,x+w-3,y+ht-5,x+w/2,y+ht-2,x+3,y+ht-5);
      a.set(x+5,y+7,0xAA8764);
      a.line(x+2,y+ht-3,x+w/2,y+ht,0x8A6C50);
   }
   /** Shallow interlocking calcite plates, with no dark round sponge pores. */
   private static void endStone(PixelArt32 a) {
      ground(a,0xC9C796,264,3);
      int[][] plates={{-4,2,9,6},{10,-3,12,7},{25,0,10,8},{3,12,11,7},{19,10,12,8},{-3,23,10,7},{10,23,13,8},{25,23,10,7}};
      for(int[] q:plates)for(int oy=-32;oy<=32;oy+=32)for(int ox=-32;ox<=32;ox+=32) {
         int x=q[0]+ox,y=q[1]+oy,w=q[2],ht=q[3];
         a.poly(0xD4D0A3,264,x,y+2,x+3,y,x+w-2,y+1,x+w,y+4,x+w-4,y+ht,x+1,y+ht-1);
         a.line(x+3,y,x+w-3,y+1,0xDDD8AD);
         a.line(x+w-4,y+ht,x+w-1,y+ht-2,0xB7B98A);
      }
   }
   /** Emerald-cut gemstone with a polished table and explicitly lit facets. */
   private static void emerald(PixelArt32 a) {
      ground(a,0x195C3D,485,1);
      a.poly(0x55B47A,1,1,1,30,1,24,7,8,7);
      a.poly(0x379660,2,1,1,8,7,8,24,1,30);
      a.poly(0x206A48,3,30,1,31,30,24,24,24,7);
      a.poly(0x124D36,4,1,30,8,24,24,24,31,30);
      a.poly(0x288452,5,8,7,24,7,24,24,8,24);
      a.line(8,7,23,7,0x82CAA0);a.line(8,8,8,23,0x4EA86E);
      a.line(11,10,17,10,0x38935F);a.line(11,11,14,11,0x38935F);
      a.line(25,6,28,3,0x76C193);a.line(3,28,6,25,0x3A9464);
   }
   /** Anthracite cleavage: irregular near-black facets with sparse hard sheen. */
   private static void coal(PixelArt32 a) {
      ground(a,0x232426,486,2);
      int[][] shards={{-2,2,12,9},{15,-3,10,11},{28,8,10,9},{7,13,13,10},{-4,22,12,9},{21,23,12,10},{8,28,12,8}};
      for(int i=0;i<shards.length;i++)for(int oy=-32;oy<=32;oy+=32)for(int ox=-32;ox<=32;ox+=32) {
         int[] q=shards[i];int x=q[0]+ox,y=q[1]+oy,w=q[2],ht=q[3];
         a.poly(i%2==0?0x303134:0x292A2C,486+i,x,y+3,x+3,y,x+w,y+1,x+w-2,y+ht-2,x+4,y+ht);
         a.line(x+3,y,x+w-2,y+1,0x484A4D);
         a.line(x+4,y+ht,x+w-2,y+ht-2,0x141518);
         if(i%2==0)a.line(x+4,y+2,x+7,y+3,0x3B3D40);
      }
   }
}
