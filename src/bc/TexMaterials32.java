package bc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/** A single material vocabulary for the construction-block cohesion pass.
 * Native 32px throughout: 1px joints/bevels, 2–3px grain clusters, bounded contrast.
 * Furniture uses the actual approved timber; machinery uses the actual approved
 * stone. This paints structure from materials, not a noise pass over old pictures.
 */
final class TexMaterials32 {
   private TexMaterials32() {}
   static final int SIZE=32, JOINT=1;
   static final int STONE=0x898D85, IRON=0xB4BFBB, QUARTZ=0xD6D1BF;
   static final int NETHER=0x38272D, NETHER_MORTAR=0x201C20;
   private static final Map<Integer,int[]> BASES=new HashMap<>();

   static boolean handles(int t) {
      return t==21||t==22||t==30||t==34||t>=37&&t<=43||t==160||t==161||t==179
         ||t==170||t==203||t==212||t==213||t==220
         ||t>=256&&t<=262||t==264||t==269
         ||t>=400&&t<=464||t>=482&&t<=488
         ||t>=524&&t<=528||t==531||t==532||t==585||t==586||t==587
         ||t>=620&&t<=626||t>=643&&t<=645||t>=648&&t<=657||t==659
         ||t>=663&&t<=671||t>=680&&t<=687;
   }
   static float hash(int x,int y,int seed){return TexPixel32.hash(x,y,seed);}
   static int rgba(int c){return 0xFF000000|c;}
   static int shift(int c,int d){return TexPixel32.shade(rgba(c),d);}
   static int grain(int x,int y,int seed,int step) {
      float v=.7F*hash(Math.floorMod(x,32)/3,Math.floorMod(y,32)/2,seed)
         +.3F*hash(Math.floorMod(x,32),Math.floorMod(y,32),seed+1);
      return ((int)(v*5)-2)*step;
   }
   static synchronized int[] base(int t) {
      int[] result=BASES.get(t);
      if(result==null){int[] mask=new int[256];Arrays.fill(mask,0xFF808080);result=TexPixel32.tile(t,mask,false);BASES.put(t,result);}
      return result.clone();
   }
   static int[] timber(int species){return base(species==0?Tex.PLANKS:TexArt313.PLANKS0+species-1);}

   static int[] paint(int t) {
      if(!handles(t))return null;
      int[] pixels=new int[1024];
      for(int y=0;y<32;y++)for(int x=0;x<32;x++)pixels[y*32+x]=surface(t,x,y);
      Face f=new Face(pixels);
      switch(t){
         case 37: case 38: craft(f,t==37);break;
         case 39: case 40: furnace(f,t==40);break;
         case 41: case 42: chest(f,t==41);break;
         case 43: cage(f);break;
         case 160: case 161: door(f,t==161);break;
         case 179: ladder(f);break;
         case 220: bookshelf(f);break;
         case 644: case 645: lamp(f,t==645);break;
         case 651: case 653: case 655: carve(f);break;
         case 657: crack(f);break;
         case 663: case 664: repeater(f,t==664);break;
         case 665: case 666: case 667: case 668: piston(f,t);break;
         case 669: daylight(f);break;
         case 670: noteBlock(f);break;
         case 671: observer(f);break;
      }
      if(t>=620&&t<=626)trapdoor(f,t);
      return f.p;
   }

   /** Same dimensions/joint treatment as the approved stone bricks, not new outlines per block. */
   static int ashlar(int base,int mortar,int x,int y,int seed) {
      int row=y/8,xx=(x+(row&1)*8)%16,yy=y%8;
      if(xx==0||yy==0)return rgba(mortar);
      int tone=grain(x,y,seed,5)+((int)(hash((x+(row&1)*8)/16,row,639)*3)-1)*8;
      if(xx==1||yy==1)tone+=9;
      if(xx==15||yy==7)tone-=9;
      return shift(base,tone);
   }
   private static int surface(int t,int x,int y) {
      int g=grain(x,y,701+t,5),edge=Math.min(Math.min(x,31-x),Math.min(y,31-y));
      if(t==259)return ashlar(NETHER,NETHER_MORTAR,x,y,35);
      if(t==681)return ashlar(0x759A91,0x496960,x,y,35);
      if(t==682)return ashlar(0x46685E,0x314A43,x,y,35);
      if(t==683)return ashlar(0xA58C9B,0x756370,x,y,35);
      if(t==655||t==656||t==657) {
         int c=ashlar(STONE,0x565C53,x,y,35);
         if(t==656&&hash(x/3,y/2,478)>.65F&&(y%8>5||(x+y)%13<3))return shift(0x687B41,g);
         return c;
      }
      if(t==21||t==22||t==531||t==532||t>=651&&t<=654) {
         int c=t==531||t==532||t==653||t==654?0xB77C51:0xD1C29B;
         int d=grain(x,y,706,4);
         boolean banded=t==21||t==532;
         if(banded&&(y==7||y==23))d-=17;
         if(banded&&(y==8||y==24))d+=7;
         // Fine, discontinuous sediment planes rather than broad plastic rectangles.
         if(y%8==3&&hash(x/4,y/8,80)>.7F)d-=7;
         return shift(c,d);
      }
      if(t>=648&&t<=650) {
         int[] colors={0x9D786B,0xBABBB5,0x898B86};
         int d=grain(x,y,701+520+t-648,6);
         return shift(colors[t-648],edge==0?d-15:edge==1?d+7:d);
      }
      if(t>=400&&t<=448) {
         int c=t<416?Color312.CONCRETE[t-400]:t<432?Color312.CONCRETE[t-416]:t==432?Color312.PLAIN_TERRA:Color312.TERRA[t-433];
         int d=grain(x,y,t, t<416?3:t<432?7:4);
         if(t>=416&&t<432&&hash(x/2,y/2,802)>.91F)d+=9;
         return shift(c,d);
      }
      if(t>=449&&t<=464) {
         // Inlaid fired-clay tessellation; pigment and grout share the same earthen palette.
         int i=t-449,xx=x,yy=y;
         for(int k=0;k<i%4;k++){int old=xx;xx=31-yy;yy=old;}
         int d=Math.abs(xx-15)+Math.abs(yy-15);
         boolean line=i/4==0?d%16<3:i/4==1?(xx+yy/2)%16<3:i/4==2?(xx/8+yy/8)%2==0:Math.max(Math.abs(xx-15),Math.abs(yy-15))%10<3;
         int c=line?Color312.TERRA[i]:0xCEC1A1;
         return shift(c,grain(x,y,t,3)+(edge==0?-12:0));
      }
      if(t>=482&&t<=487||t==643) {
         int[] col={IRON,0xD8BB67,0x63B3B3,0x54A17B,0x353B3A,0x46688F};
         int c=t==643?0xA54C3E:col[t-482];
         int d=grain(x,y,t,t==486?6:3);
         if(t!=486){if(edge==0)d-=18;else if(x==1||y==1)d+=10;else if(x==30||y==30)d-=7;}
         if(t==487&&hash(x/2,y/2,t)>.96F)return shift(0x9B9875,d);
         // Small mineral cleavage planes, not a flat gradient taking half the face.
         if((t==484||t==485||t==643)&&(x+y)%17==0&&hash(x/4,y/4,t)>.5)d-=8;
         return shift(c,d);
      }
      if(t==488||t==258){
         int xx=x%8-4,yy=y%8-4;
         if(Math.abs(xx)+Math.abs(yy)<=3&&hash(x/8,y/8,t+77)>.42F)
            return shift(t==488?0x65AE84:QUARTZ,xx+yy<0?18:xx+yy>1?-18:0);
         return shift(t==488?0x80827D:0x794E47,grain(x,y,704,7));
      }
      if(t==256)return shift(0x794E47,grain(x,y,t,8)+(hash(x/4,y/2,47)>.85?-8:0));
      if(t==257){int xx=x%16,yy=y%16;boolean hollow=(xx==4||xx==10)&&yy>=5&&yy<7||yy==11&&xx>5&&xx<10;return shift(0x75614F,hollow?-25:g);}
      if(t==264)return shift(0xCBC593,grain(x,y,701,7)+(hash(x/3,y/2,964)>.86?-15:0));
      if(t==30||t==260||t==34||t==269||t==680)return fractureMaterial(t,x,y);
      if(t==261||t==262||t==686||t==687){
         int c=t<263?QUARTZ:0xD3CBB1;
         int d=grain(x,y,703,3);
         if((t==261||t==687)&&x%8==0)d-=11;
         if((t==261||t==687)&&x%8==1)d+=5;
         if(t==686&&Math.max(Math.abs(x-15),Math.abs(y-15))%7==0)d-=12;
         return shift(c,d);
      }
      if(t==684||t==685)return shift(0xAA929F,g/2+(t==685?(x%8==0?-15:x%8==1?7:0):Math.max(Math.abs(x-15),Math.abs(y-15))%7==0?-13:0));
      if(t>=524&&t<=528||t==587){
         int c=t==524?0x785A3D:t==526?0x94838E:0x896447;
         if(t==525||t==527||t==587)if(y<5+(int)(hash(x/3,0,91)*6))c=t==525?0x785A3D:t==527?0x94838E:0xEAF0F4;
         int d=grain(x,y,702,9);
         if(t==528&&hash(x/2,y/2,820)>.85F)c=0x998A77;
         if(t==524&&(x+2*y)%13<2&&hash(x/3,y/3,28)>.35F)d+=11;
         return shift(c,d);
      }
      if(t==170||t==203)return shift(t==170?0x896447:0x64503C,grain(x,y,702,9)+(x%8==0?-22:x%8==1?8:0));
      if(t==212||t==213)return shift(0xBDA465,(t==212&&(y==7||y==23)?-38:x%3==0?-13:grain(x,y,28,4)));
      if(t==585)return shift(0x98816A,g);
      if(t==586)return shift(0xCFC7B4,(x%7==0?-14:0)+grain(x,y,t,4));
      if(t==659)return shift(edge<2?0x7AA49A:0xD4DCC2,grain(x,y,700,3)+(edge==2?8:0));
      // Face layouts below completely supply the construction blocks.
      return shift(STONE,grain(x,y,704,5));
   }

   private static int fractureMaterial(int t,int x,int y) {
      // Same irregular stone scale as approved cobble: 4 cells across a 32px tile.
      float px=x/8F,py=y/8F,a=100,b=100,v=0;
      for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++){
         int cx=(int)px+dx,cy=(int)py+dy,wx=Math.floorMod(cx,4),wy=Math.floorMod(cy,4);
         float sx=cx+.2F+hash(wx,wy,11)*.6F,sy=cy+.2F+hash(wx,wy,12)*.6F;
         float d=(px-sx)*(px-sx)+(py-sy)*(py-sy);
         if(d<a){b=a;a=d;v=hash(wx,wy,13);}else if(d<b)b=d;
      }
      float edge=b-a;
      int c=t==30?0xCCB477:t==260?0x665046:t==680?0x799E94:0x363540;
      int d=((int)(v*5)-2)*5+grain(x,y,287,4);
      if(edge<.12F){if(t==260)return shift(0xDD914A,d);d-=t==34||t==269?9:23;}
      else if(edge<.27F)d+=t==34||t==269?4:8;
      return shift(c,d);
   }

   private static void crack(Face f) {
      int[][] strokes={{11,1,10,2,10,3,9,4,9,5,8,6},{9,4,8,3,7,3},
         {17,9,17,10,16,11,16,12,15,13,14,14},{16,12,18,12,19,13},
         {30,20,29,20,28,21,27,21,27,22},{12,25,12,26,13,27,13,28}};
      for(int[] q:strokes)for(int i=0;i<q.length;i+=2){int at=q[i+1]*32+q[i];f.p[at]=TexPixel32.shade(f.p[at],-30);}
   }
   private static void carve(Face f) {
      f.groove(5,5,22,22);f.groove(10,10,12,12);
      // Deliberate 1px carving; the stone beneath is not replaced by a flat panel.
   }
   private static void craft(Face f,boolean top) {
      f.fill(timber(0));
      f.frame(0,0,32,32,-30);f.frame(1,1,30,30,9);
      if(top){for(int p=4;p<30;p+=8){f.cut(p,4,p,28,-27);f.cut(4,p,28,p,-27);f.cut(p+1,4,p+1,28,8);} }
      else{
         f.wood(0,2,3,28,3,0);f.wood(0,2,26,28,4,0);
         // Recessed hanging tools with proper blade/handle proportions.
         f.line(8,11,8,23,0x61492F);f.line(9,11,9,23,0xB59663);
         f.metal(5,8,9,4);f.line(5,8,13,8,0xD0D8C9);
         f.line(21,9,21,24,0x725435);f.line(22,9,22,24,0xB59663);
         f.metal(18,8,6,7);f.metal(24,10,3,4);
      }
      f.rivet(2,2);f.rivet(29,2);f.rivet(2,29);f.rivet(29,29);
   }
   private static void chest(Face f,boolean top) {
      f.fill(timber(0));f.wood(0,0,0,3,32,1);f.wood(0,29,0,3,32,1);
      f.cut(3,0,3,31,9);f.cut(28,0,28,31,-18);
      if(!top){f.cut(0,9,31,9,-36);f.cut(0,10,31,10,-18);f.cut(3,11,28,11,9);
         f.metal(13,7,7,11);f.line(14,8,18,8,0xD4DACE);f.line(16,11,16,14,0x455450);}
      else{f.wood(0,0,0,32,3,0);f.wood(0,0,29,32,3,0);}
      f.cut(0,30,31,30,-24);f.rivet(1,5);f.rivet(30,5);f.rivet(1,26);f.rivet(30,26);
   }
   private static void furnace(Face f,boolean lit) {
      f.fill(base(Tex.STONEBRICK));
      // Masonry survives around openings; no unrelated smooth grey housing.
      f.stone(2,3,28,8);f.recess(5,6,9,3);f.recess(18,6,9,3);
      f.stone(2,15,28,15);
      for(int y=17;y<29;y++)for(int x=5;x<27;x++){
         if(y==17&&(x<8||x>23)||y==18&&(x<6||x>25))continue;
         f.set(x,y,shift(0x252A26,grain(x,y,71,3)));
      }
      if(lit)for(int x=7;x<26;x++){
         int h=2+(int)(hash(x/2,0,40)*7);
         for(int y=28-h;y<28;y++)f.set(x,y,shift(y<25?0xBB6D33:0xE9BA61,grain(x,y,51,4)));
      }
      for(int x=7;x<27;x+=4)f.line(x,26,x,29,0x5A6258);
      f.cut(3,30,28,30,9);
   }
   private static void bookshelf(Face f) {
      f.fill(timber(0));
      int[] books={0x855A47,0x617458,0xB29A68,0x596E7E,0xA06F46,0x78667D};
      for(int r=0;r<2;r++){
         int y=3+r*15;f.recess(2,y,28,11);
         for(int i=0;i<6;i++){int x=3+i*4,h=8+i%3;
            f.patch(x,y+11-h,3,h,books[(i+r)%6],3);f.line(x,y+13-h,x+2,y+13-h,0xBBAC82);}
         f.wood(0,0,y+11,32,2,0);f.cut(0,y+13,31,y+13,-19);
      }
      f.wood(0,0,0,2,32,1);f.wood(0,30,0,2,32,1);
   }
   private static void door(Face f,boolean upper) {
      f.fill(timber(0));
      f.wood(0,0,0,4,32,1);f.wood(0,28,0,4,32,1);
      f.cut(4,0,4,31,7);f.cut(27,0,27,31,-20);
      for(int x:new int[]{6,18}){
         f.groove(x,4,8,22);
         if(upper)f.erase(x+1,5,6,13);
      }
      f.metal(25,21,3,5);f.line(25,22,25,23,0xD2D7C6);
      f.rivet(1,6);f.rivet(1,25);
   }
   private static void trapdoor(Face f,int t) {
      int[] species={0,2,1,3,4,5};
      if(t==626){for(int y=0;y<32;y++)for(int x=0;x<32;x++)f.set(x,y,shift(IRON,grain(x,y,51,4)));}
      else f.fill(timber(species[t-620]));
      f.frame(0,0,32,32,-27);f.frame(1,1,30,30,9);
      for(int y:new int[]{8,20})for(int x:new int[]{6,18}){f.groove(x-1,y-1,10,6);f.erase(x,y,8,4);}
      f.metal(2,5,2,6);f.metal(2,23,2,6);
   }
   private static void ladder(Face f) {
      Arrays.fill(f.p,0);
      f.wood(0,5,0,3,32,1);f.wood(0,24,0,3,32,1);
      for(int y=3;y<32;y+=8){f.wood(0,5,y,22,3,0);f.cut(5,y,26,y,8);f.rivet(6,y+1);f.rivet(25,y+1);}
   }
   private static void cage(Face f){Arrays.fill(f.p,0);for(int x=2;x<32;x+=8)f.metal(x,0,2,32);f.metal(0,5,32,2);f.metal(0,25,32,2);f.line(2,0,2,31,0xBBC6BB);}
   private static void lamp(Face f,boolean lit){
      f.fill(base(Tex.PLANKS));
      for(int y=3;y<29;y++)for(int x=3;x<29;x++)f.set(x,y,shift(lit?0xD8BF83:0x827353,grain(x,y,30,5)));
      f.frame(0,0,32,32,-25);f.frame(2,2,28,28,-23);
      f.line(3,3,28,28,0x6A5942);f.line(28,3,3,28,0x6A5942);
      f.rivet(1,1);f.rivet(30,1);f.rivet(1,30);f.rivet(30,30);
   }
   private static void repeater(Face f,boolean on){
      f.fill(base(Tex.STONE));f.frame(0,0,32,32,-19);
      f.patch(14,3,4,26,on?0xAE5744:0x713E31,3);
      f.wood(0,6,14,20,3,0);f.metal(6,14,2,3);f.metal(24,14,2,3);
   }
   private static void piston(Face f,int t){
      if(t==666||t==667){
         f.fill(timber(0));f.metal(0,0,32,2);f.metal(0,30,32,2);f.metal(0,0,2,32);f.metal(30,0,2,32);
         if(t==667)for(int y=4;y<28;y++)for(int x=4;x<28;x++)if(hash(x/3,y/2,478)>.36F)f.set(x,y,shift(0x809657,grain(x,y,91,5)));
      }else{
         f.fill(base(Tex.STONE));f.frame(1,1,30,30,-18);
         if(t==665){f.wood(0,6,6,20,20,0);f.metal(13,6,6,20);}
         else{f.stone(7,7,18,18);f.groove(10,10,12,12);}
      }
      f.rivet(3,3);f.rivet(28,3);f.rivet(3,28);f.rivet(28,28);
   }
   private static void daylight(Face f){
      f.fill(timber(0));f.frame(0,0,32,32,-26);
      for(int y=3;y<30;y+=9)for(int x=3;x<30;x+=9){f.patch(x,y,8,8,0x727E79,3);f.cut(x,y,x+7,y,15);f.cut(x+7,y,x+7,y+7,-14);}
   }
   private static void noteBlock(Face f){f.fill(timber(0));f.groove(2,2,28,28);for(int y=5;y<28;y+=4)for(int x=5;x<28;x+=4)f.patch(x,y,2,2,0x514330,2);}
   private static void observer(Face f){f.fill(base(Tex.STONE));f.frame(0,0,32,32,-15);f.recess(4,7,8,7);f.recess(20,7,8,7);f.recess(8,22,16,3);}

   private static final class Face {
      final int[] p;
      Face(int[] p){this.p=p;}
      void fill(int[] src){System.arraycopy(src,0,p,0,1024);}
      void set(int x,int y,int c){if(x>=0&&x<32&&y>=0&&y<32)p[y*32+x]=rgba(c);}
      void patch(int x,int y,int w,int h,int c,int step){for(int yy=y;yy<y+h;yy++)for(int xx=x;xx<x+w;xx++)set(xx,yy,shift(c,grain(xx,yy,47,step)));}
      void stone(int x,int y,int w,int h){int[] src=base(Tex.STONE);for(int yy=y;yy<y+h;yy++)for(int xx=x;xx<x+w;xx++)set(xx,yy,src[(yy&31)*32+(xx&31)]);frame(x,y,w,h,-13);cut(x+1,y+1,x+w-2,y+1,8);}
      void wood(int species,int x,int y,int w,int h,int turned){int[] src=timber(species);for(int yy=y;yy<y+h;yy++)for(int xx=x;xx<x+w;xx++)set(xx,yy,src[turned==0?(yy&31)*32+(xx&31):(xx&31)*32+(yy&31)]);}
      void metal(int x,int y,int w,int h){patch(x,y,w,h,IRON,3);frame(x,y,w,h,-19);cut(x,y,x+w-1,y,10);}
      void erase(int x,int y,int w,int h){for(int yy=y;yy<y+h;yy++)for(int xx=x;xx<x+w;xx++)if(xx>=0&&xx<32&&yy>=0&&yy<32)p[yy*32+xx]=0;}
      void line(int x,int y,int x1,int y1,int c){walk(x,y,x1,y1,c,false);}
      void cut(int x,int y,int x1,int y1,int d){walk(x,y,x1,y1,d,true);}
      void walk(int x,int y,int x1,int y1,int c,boolean shade){int dx=Math.abs(x1-x),dy=-Math.abs(y1-y),sx=x<x1?1:-1,sy=y<y1?1:-1,e=dx+dy;while(true){if(x>=0&&x<32&&y>=0&&y<32){if(shade)p[y*32+x]=TexPixel32.shade(p[y*32+x],c);else set(x,y,c);}if(x==x1&&y==y1)break;int e2=e*2;if(e2>=dy){e+=dy;x+=sx;}if(e2<=dx){e+=dx;y+=sy;}}}
      void frame(int x,int y,int w,int h,int d){cut(x,y,x+w-1,y,d);cut(x,y+h-1,x+w-1,y+h-1,d);if(h>2){cut(x,y+1,x,y+h-2,d);cut(x+w-1,y+1,x+w-1,y+h-2,d);}}
      void groove(int x,int y,int w,int h){frame(x,y,w,h,-22);cut(x+1,y+1,x+w-2,y+1,7);cut(x+1,y+1,x+1,y+h-2,7);}
      void recess(int x,int y,int w,int h){patch(x,y,w,h,0x303831,2);line(x,y+h,x+w-1,y+h,0x9BA294);}
      void rivet(int x,int y){set(x,y,0x53605A);set(x,y-1,0xBAC5B9);}
   }
}
