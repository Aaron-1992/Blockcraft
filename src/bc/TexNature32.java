package bc;

/** Remaining native world art: botanic structure, transparent materials, textiles
 * and animated surfaces. Pigments and feature scale follow the construction set.
 */
final class TexNature32 {
   private TexNature32(){}
   static int[] paint(int t){
      if(TexRemaining32.animated(t))return frame(t,0);
      if(t==2)return grassEdge();
      if(t==9||t>=550&&t<=552||t==642)return foliage(t);
      if(t==25||t==553||t==554||t>=171&&t<=178||t>=204&&t<=211||t>=227&&t<=230||t==219)return crop(t);
      if(t==26||t==27||t>=556&&t<=567)return flower(t);
      if(t>=570&&t<=581)return tall(t);
      if(t==582||t==583)return creeper(t);
      if(t==19||t>=466&&t<=481||t==529||t==530||t==658)return translucent(t);
      if(t>=601&&t<=618||t==600)return bars(t);
      if(t==183||t==184||t==185||t>=630&&t<=641)return cloth(t);
      PixelArt32 a=new PixelArt32();
      switch(t){
         case 11: bedrock(a);break;
         case 28:case 29: cactus(a,t==29);break;
         case 36:case 646:case 662: torch(a,t);break;
         case 180:case 181:case 182: explosives(a,t);break;
         case 221:case 222:case 223:case 224:case 225:case 226: gourd(a,t);break;
         case 231:case 232:case 233:case 234: cake(a,t);break;
         case 265:case 266:case 267:case 268: endFrame(a,t);break;
         case 584: mushroomBlock(a);break;
         case 647:case 661: wire(a,t==661);break;
         default:throw new IllegalArgumentException("Unpainted world cell "+t);
      }
      return a.p;
   }
   private static int[] grassEdge(){PixelArt32 a=new PixelArt32();int[] top=TexMaterials32.base(Tex.GRASS_TOP);for(int x=0;x<32;x++){int h=4+(int)(PixelArt32.h(x/3,0,91)*6);for(int y=0;y<h;y++)a.set(x,y,top[y*32+x]);if(h<10&&x%3==1)a.set(x,h,PixelArt32.shade(top[h*32+x],-9));}return a.p;}
   private static int[] foliage(int t){
      PixelArt32 a=new PixelArt32();boolean frond=t==642;
      if(!frond)for(int y=0;y<32;y++)for(int x=0;x<32;x++)a.set(x,y,PixelArt32.shade(0x777777,PixelArt32.grain(x,y,32+t,5)));
      // Small overlapping pointed leaves and fine veins, never circular stamps.
      for(int i=0;i<(frond?26:50);i++){
         int x=(int)(PixelArt32.h(i,0,811+t)*32),y=(int)(PixelArt32.h(i,1,811+t)*32);
         int dx=(i%2==0?1:-1)*(5+(i%3)),dy=i%3==0?-5:4;
         int grey=141+(i%4)*12,col=grey<<16|grey<<8|grey;
         for(int oy=-32;oy<=32;oy+=32)for(int ox=-32;ox<=32;ox+=32)a.leaf(x+ox,y+oy,dx,dy,col,i+12);
      }
      if(!frond)for(int y=0;y<32;y++)for(int x=0;x<32;x++){
         int c=a.p[y*32+x]&255;if(c<125&&PixelArt32.h(x/2,y/2,923+t)>.89F)a.p[y*32+x]=0;
      }
      return a.p;
   }
   private static int[] crop(int t){
      PixelArt32 a=new PixelArt32();
      if(t==554){a.stroke(16,31,15,11,0x94754F,2);a.line(15,24,6,15,0xAB8C60);a.line(6,15,7,9,0x826842);a.line(16,19,24,10,0xAB8C60);a.line(24,10,23,5,0x826842);a.line(15,15,11,7,0xA88855);return a.p;}
      if(t==219){for(int i=0;i<3;i++){int x=4+i*11;a.material(x,0,3,32,0x779458,73+i,3);a.line(x,0,x,31,0xA2B071);for(int y=5+i%2*3;y<32;y+=9){a.line(x,y,x+2,y,0x506C3C);a.leaf(x+2,y-1,5,-5,0x7B9855,83+i);}}return a.p;}
      if(t==25||t==553){for(int i=0;i<9;i++){int tip=3+(int)(PixelArt32.h(i,0,t)*17),x=2+i*3;a.stroke(14+i%4,31,x,tip,0xA9A9A9,1+(i%2));if(t==553)for(int y=tip+3;y<27;y+=4){int xx=x+(16-x)*(y-tip)/Math.max(1,31-tip);a.leaf(xx,y,(x<16?-1:1)*(3+(27-y)/5),-2,0xABABAB,i+y);}}return a.p;}
      boolean wheat=t>=171&&t<=178,stem=t>=227;
      int stage=wheat?t-171:stem?t-227:(t-204)%4;
      int height=wheat?5+stage*3:5+stage*6;
      for(int i=0;i<(stem?1:5);i++){
         int x=stem?15:3+i*6,top=31-height+i%2;
         int col=wheat&&stage>=5?0xB6A063:0x7C944F;
         a.line(x,31,x,top,col);a.line(x+1,31,x+1,top+3,PixelArt32.shade(col,-14));
         if(wheat&&stage>3)for(int k=0;k<4;k++){a.leaf(x,top+3+k*3,-3,-3,0xC2AA6A,k+i);a.leaf(x,top+3+k*3,3,-4,0xA99150,k+i+17);}
         else{a.leaf(x,top+height/2,-4,-3,PixelArt32.shade(col,8),i);a.leaf(x,top+height/2+3,5,-4,col,i+1);}
         if(!wheat&&!stem&&stage==3)a.ellipse(x-2,27,5,4,t<208?0xBE8748:0xB6A273,72+i,true);
      }return a.p;
   }
   static void stem(PixelArt32 a,int offset){a.line(15,31-offset,15,12-offset,0x56713D);a.line(16,31-offset,16,12-offset,0x8B9D60);a.leaf(15,25-offset,-9,-5,0x71894B,71);a.leaf(16,22-offset,9,-6,0x849A57,72);}
   private static int flowerColor(int t){int[] colors={0x82A4B2,0xA783AC,0xD4D4BA,0xB65545,0xC48949,0xDDD7C3,0xC88F9F,0xDFDCC3,0x698CAF,0xDADCC7};return t==26?0xB4513F:t==27?0xCBB268:colors[Math.min(9,Math.max(0,t-556))];}
   private static int[] flower(int t){
      PixelArt32 a=new PixelArt32();
      if(t==566||t==567){a.poly(0xC0B295,51,14,29,14,15,18,14,21,29);a.line(15,18,15,28,0xDFCEAA);a.poly(t==566?0x9C8163:0xAB5144,52,4,18,7,12,12,8,20,8,26,12,29,19,24,22,8,22);a.line(6,20,26,20,0xD0BA98);if(t==567){a.poly(0xDBD1B8,53,11,10,15,10,15,13,11,14);a.poly(0xDBD1B8,54,22,14,25,14,26,17,22,18);}return a.p;}
      int col=flowerColor(t);stem(a,0);
      if(t>=559&&t<=562){a.poly(PixelArt32.shade(col,-18),61,10,6,14,9,17,5,20,9,24,5,23,13,19,17,15,17,11,13);a.poly(col,62,13,9,17,7,21,10,20,14,16,15,13,12);a.line(17,9,17,14,PixelArt32.shade(col,15));}
      else if(t==565){a.line(16,15,20,6,0x73884D);for(int y=6;y<17;y+=5){a.poly(col,y,17,y,21,y,23,y+4,17,y+4,16,y+2);a.line(17,y+4,22,y+4,0xB2BBA2);}}
      else if(t==563||t==558||t==27){for(int i=0;i<8;i++){double an=i*Math.PI/4;int dx=(int)Math.round(Math.cos(an)*6),dy=(int)Math.round(Math.sin(an)*6);a.leaf(16,11,dx,dy,col,70+i);}a.ellipse(13,8,6,6,t==27?0x9D8645:0xC1A456,85,true);}
      else if(t==556){a.poly(col,70,8,6,13,5,17,9,22,5,25,8,23,12,18,12,18,16,13,16,14,11,9,10);a.rect(15,10,3,3,0xD6C595);}
      else{a.poly(PixelArt32.shade(col,-18),71,8,9,11,5,15,6,18,4,23,6,25,11,22,15,17,17,12,14,9,13);a.poly(col,72,10,8,14,6,17,10,14,13,10,11);a.poly(PixelArt32.shade(col,7),73,18,6,22,7,23,10,19,13,16,11);a.rect(15,12,4,3,0x5E5132);a.rect(16,12,2,1,0xBFA06A);}
      return a.p;
   }
   private static int[] tall(int t){
      PixelArt32 a=new PixelArt32();int kind=(t-570)/2,off=(t&1)==1?0:32;
      if(kind<2){for(int i=0;i<10;i++){int x=2+i*3,top=3+(int)(PixelArt32.h(i,0,kind+800)*24);a.stroke(14+i%5,63-off,x,top-off,0xB1B1B1,1+i%2);if(kind==1)for(int y=top+4;y<58;y+=5){int xx=x+(16-x)*(y-top)/Math.max(1,63-top);a.leaf(xx,y-off,(x<16?-1:1)*(3+(58-y)/9),-3,0xA5A5A5,88+y);}}return a.p;}
      a.stroke(15,63-off,15,12-off,0x738B4D,2);
      for(int y=28;y<63;y+=10){a.leaf(16,y-off,-11,-6,0x6C8748,y);a.leaf(16,y+4-off,10,-7,0x879B58,y+1);}
      if(kind==2){int center=13-off;for(int i=0;i<12;i++){double an=i*Math.PI/6;int dx=(int)(Math.cos(an)*10),dy=(int)(Math.sin(an)*10);a.leaf(16,center,dx,dy,0xCEB35E,90+i);}a.ellipse(10,7-off,12,12,0x79613A,92,true);for(int y=9-off;y<18-off;y+=3)for(int x=12;x<21;x+=3)a.set(x,y,0xB69550);}
      else{
         int col=kind==3?0xA185A6:kind==4?0xAE5042:0xC092A3;
         int[][] blooms={{9,9},{17,5},{23,11},{13,16},{20,21},{7,23},{25,26},{15,28}};
         for(int i=0;i<blooms.length;i++){int x=blooms[i][0],y=blooms[i][1]-off;
            a.poly(col,100+i,x-3,y,x-1,y-3,x+3,y-2,x+4,y+1,x+1,y+4,x-2,y+3);
            a.set(x,y,PixelArt32.shade(col,21));}
      }
      return a.p;
   }
   private static int[] creeper(int t){PixelArt32 a=new PixelArt32();
      if(t==582){a.poly(0x657F49,118,2,15,5,9,14,5,23,7,29,13,30,20,23,26,14,28,7,24,3,21);a.poly(0x7E944F,119,5,14,9,9,17,7,24,11,26,17,20,23,13,24,7,20);a.line(15,17,6,13,0x4E6C3C);a.line(15,17,20,9,0x4E6C3C);a.line(15,17,24,22,0x4E6C3C);a.poly(0x4D683A,120,16,17,27,26,22,28);a.clear(26,25,6,7);}
      else{for(int x=4;x<32;x+=11){a.line(x,0,x-2,31,0x858585);for(int y=3;y<32;y+=7){a.leaf(x,y,-4,-3,0xAAAAAA,x+y);a.leaf(x,y+2,5,-4,0xB6B6B6,x+y+1);}}}
      return a.p;
   }
   private static int[] translucent(int t){PixelArt32 a=new PixelArt32();
      boolean ice=t==529||t==530,slime=t==658;int color=ice?0x9FB9C7:slime?0x88A865:t==19?0xB7C7C5:PixelArt32.wool(t-466);
      for(int y=0;y<32;y++)for(int x=0;x<32;x++){
         int e=Math.min(Math.min(x,31-x),Math.min(y,31-y));int alpha,delta;
         if(ice){alpha=t==530?255:172;int seam=(x+y/2)%23;delta=PixelArt32.grain(x,y,529,3)+(seam==0?12:seam==1?5:0);}
         else if(slime){alpha=166;delta=PixelArt32.grain(x,y,658,4)+(e==0?-14:e==1?6:e<4?-5:0);}
         else{boolean shine=e>3&&((x+y==12&&x<11)||(x+y==17&&x<13)||(x+y==43&&x>22));alpha=e==0?190:shine?84:t==19?0:42;delta=e==0?-8:shine?13:0;}
         a.p[y*32+x]=alpha==0?0:alpha<<24|(PixelArt32.shade(color,delta)&0xFFFFFF);
      }return a.p;
   }
   private static int[] bars(int t){PixelArt32 a=new PixelArt32();
      if(t==600){for(int x=2;x<32;x+=8){a.material(x,0,2,32,0x75867C,600+x,3);a.line(x,0,x,31,0xA6B4A7);}a.material(0,6,32,2,0x849185,623,2);a.material(0,25,32,2,0x75867C,624,2);}
      else{int c=t==601?0xBCCFCE:t==618?0x8A9C92:PixelArt32.wool(t-602);a.material(14,0,4,32,c,640,2);a.material(0,14,32,4,c,640,2);a.line(14,0,14,31,PixelArt32.shade(c,11));a.line(0,14,31,14,PixelArt32.shade(c,11));}
      return a.p;
   }
   private static int[] cloth(int t){PixelArt32 a=new PixelArt32();
      int facing=t>=630&&t<=637?new int[]{2,0,1,3}[(t-(t<634?630:634))%4]:0;
      for(int y=0;y<32;y++)for(int x=0;x<32;x++){
         if((t==185||t==638)&&(y<14||y>19))continue;
         int xx=x,yy=y;for(int k=0;k<facing;k++){int z=xx;xx=31-yy;yy=z;}
         boolean pillow=t==639||t==640||t==183&&yy<10;
         int base=t>=630?0xDCDCDC:pillow?0xE4E0D0:0xA55247;
         int d=PixelArt32.grain(xx,yy,641,2)+((xx+yy)%2==0?3:-3);
         if(xx==1||xx==30||yy==1||yy==30)d-=9;
         if((t==184||t>=630&&t<=633)&&yy==12)d-=14;
         if(pillow)d+=xx<3||xx>28||yy<2||yy>29?-8:2;
         a.set(x,y,PixelArt32.shade(base,d));
      }return a.p;
   }
   private static void bedrock(PixelArt32 a){for(int y=0;y<32;y++)for(int x=0;x<32;x++){int band=(int)(PixelArt32.h(x/5,y/4,11)*5);int d=(band-2)*10+PixelArt32.grain(x,y,912,5);if(y%4==0&&band>2)d-=10;a.set(x,y,PixelArt32.shade(0x5E655F,d));}}
   private static void cactus(PixelArt32 a,boolean top){for(int y=0;y<32;y++)for(int x=0;x<32;x++){int e=Math.min(Math.min(x,31-x),Math.min(y,31-y));int d=PixelArt32.grain(x,y,929,4)+(top?(e<3?-22:Math.max(Math.abs(x-15),Math.abs(y-15))%6==0?-9:0):x%8==0?-20:x%8==1?8:0);a.set(x,y,PixelArt32.shade(top?0x90A16A:0x67824C,d));if(!top&&x%8==5&&y%9==3){a.set(x,y,0xBEC195);a.set(x,y+1,0x455F3B);}}}
   private static void torch(PixelArt32 a,int t){a.material(14,11,4,21,0x8B6A43,36,4);a.line(14,13,14,30,0xB79762);a.material(12,7,8,8,t==662?0x775445:t==646?0xA84B39:0xC79149,37,3);a.rect(12,14,8,2,0x5D4A33);if(t!=662){a.poly(t==646?0xD77B55:0xF2D78E,41,14,13,13,9,16,5,17,9,20,8,19,13);a.rect(15,10,3,3,t==646?0xF5B67B:0xFAE4B4);}else a.line(14,7,18,7,0x9D7160);}
   private static void explosives(PixelArt32 a,int t){for(int y=0;y<32;y++)for(int x=0;x<32;x++){int d=PixelArt32.grain(x,y,180,4)+(x%8==0?-21:x%8==1?8:0);a.set(x,y,PixelArt32.shade(0xA95A45,d));}if(t==180){a.material(0,11,32,11,0xD8CFB9,184,2);String[] q={"11101010111","01001110010","01001010010","01001011010","01001010010"};for(int y=0;y<5;y++)for(int x=0;x<11;x++)if(q[y].charAt(x)=='1')a.rect(5+x*2,14+y,2,1,0x514233);}else{for(int x=4;x<32;x+=8)a.ellipse(x-2,10,5,5,0x734C39,x,false);if(t==181){a.material(13,13,5,5,0xC0AF89,182,2);a.line(15,13,20,6,0x584B34);a.line(16,13,21,6,0x9D865D);}}}
   private static void gourd(PixelArt32 a,int t){boolean melon=t>=225,top=t==221||t==225;for(int y=0;y<32;y++)for(int x=0;x<32;x++){int band=top?Math.max(Math.abs(x-15),Math.abs(y-15)):x;int d=PixelArt32.grain(x,y,221,4)+(band%8==0?-21:band%8==1?8:0);a.set(x,y,PixelArt32.shade(melon?0x81994F:0xBC8A4A,d));}if(top){a.poly(0x596C3F,221,12,12,17,11,20,14,18,19,14,20,11,16);a.line(14,13,15,17,0x8E9A5D);}if(t==223||t==224){int c=t==224?0xE1C185:0x62452E;a.poly(c,222,5,8,12,11,12,15,5,15);a.poly(c,223,20,11,27,8,27,15,20,15);a.poly(c,224,8,21,13,23,16,20,20,23,25,20,24,27,10,27);}}
   private static void cake(PixelArt32 a,int t){for(int y=0;y<32;y++)for(int x=0;x<32;x++){boolean icing=t==231||t!=233&&y<9+(int)(PixelArt32.h(x/3,0,231)*4);int c=icing?0xDED3BA:0xAD8654,d=PixelArt32.grain(x,y,233,4);if(t==231&&PixelArt32.h(x/2,y/2,232)>.97)c=0xA75642;if(!icing&&PixelArt32.h(x/2,y/2,234)>.9)d-=12;a.set(x,y,PixelArt32.shade(c,d));}}
   private static void endFrame(PixelArt32 a,int t){if(t==268){for(int y=0;y<32;y++)for(int x=0;x<32;x++){a.set(x,y,PixelArt32.h(x,y,268)>.986?0x7F9B8D:0x152526);}return;}for(int y=0;y<32;y++)for(int x=0;x<32;x++)a.set(x,y,TexMaterials32.ashlar(0x7C8A6A,0x536448,x,y,35));if(t==265||t==266){a.material(0,0,32,2,0xC6C095,265,4);a.material(0,30,32,2,0xC6C095,265,4);a.material(0,0,2,32,0xC6C095,265,4);a.material(30,0,2,32,0xC6C095,265,4);a.material(7,7,18,18,0x273E34,266,3);a.outline(6,6,20,20,0xA0AA7E);if(t==266){a.ellipse(9,9,15,14,0x80A06D,267,true);a.poly(0x253F31,269,16,10,19,14,18,20,15,22,14,16);}}}
   private static void mushroomBlock(PixelArt32 a){for(int y=0;y<32;y++)for(int x=0;x<32;x++)a.set(x,y,PixelArt32.shade(0xAB5749,PixelArt32.grain(x,y,584,6)));for(int[] q:new int[][]{{6,7},{21,4},{17,22},{3,27},{29,17}})a.poly(0xD8CDBC,585,q[0]-2,q[1],q[0],q[1]-3,q[0]+4,q[1]-2,q[0]+4,q[1]+2,q[0]+1,q[1]+4,q[0]-2,q[1]+2);}
   private static void wire(PixelArt32 a,boolean lit){int c=lit?0xB95C42:0x773A2C;a.material(14,0,4,32,c,647,4);a.material(0,14,32,4,c,647,4);for(int y=3;y<32;y+=6){a.set(13,y,PixelArt32.shade(c,-10));a.set(18,y+2,c);a.set(y,13,c);}a.line(15,0,15,31,PixelArt32.shade(c,lit?19:9));}

   /** Periodic advected scalar field, quantized into five pigments. No image interpolation. */
   private static float noise(float x,float y,int cells,int seed){float fx=x*cells/32F,fy=y*cells/32F;int ix=(int)Math.floor(fx),iy=(int)Math.floor(fy);float u=fx-ix,v=fy-iy;u=u*u*(3-2*u);v=v*v*(3-2*v);float a=PixelArt32.h(Math.floorMod(ix,cells),Math.floorMod(iy,cells),seed),b=PixelArt32.h(Math.floorMod(ix+1,cells),Math.floorMod(iy,cells),seed),c=PixelArt32.h(Math.floorMod(ix,cells),Math.floorMod(iy+1,cells),seed),d=PixelArt32.h(Math.floorMod(ix+1,cells),Math.floorMod(iy+1,cells),seed);return (a+(b-a)*u)*(1-v)+(c+(d-c)*u)*v;}
   static int[] frame(int t,float phase){
      PixelArt32 a=new PixelArt32();float time=(phase%16+16)%16,shift=time*2;
      int[] water={0x365D79,0x426E87,0x4F7F92,0x60929E,0x78A5AD};
      int[] lava={0x75442E,0xA45D31,0xC4833D,0xDEAB52,0xE9C67A};
      int[] portal={0x3E354F,0x534362,0x6B557A,0x826A90,0x9B86A5};
      int[] flame={0xEBD09A,0xDDB567,0xCA8B42,0xAA6232,0x77452A};
      for(int y=0;y<32;y++)for(int x=0;x<32;x++){
         if(t==235){float field=noise(x,shift,8,735);int height=11+(int)(field*19);if(31-y>height)continue;int band=Math.min(4,(31-y)*5/Math.max(1,height));a.set(x,y,flame[band]);continue;}
         float v=noise(x+shift,y-shift,4,t)*.68F+noise(x+shift*2,y+shift,8,t+1)*.32F;
         if(t==263){double r=Math.hypot(x-15.5,y-15.5);v=(float)(v*.6+.4*(.5+.5*Math.sin(r*.65-time*Math.PI/4)));}
         int band=Math.max(0,Math.min(4,(int)(v*5)));
         int color=t==23?water[band]:t==24?lava[band]:portal[band];
         a.p[y*32+x]=(t==23?0xB0000000:t==263?0xD0000000:0xFF000000)|color;
      }return a.p;
   }
}
