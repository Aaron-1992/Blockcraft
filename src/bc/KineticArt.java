package bc;

/** Original 32px material/item pixels for native machinery; no Create asset files. */
final class KineticArt {
   static final int BASE=1000,COUNT=13;
   static final int CASING=1005,AXLE=1006,TIMBER=1007,BRASS=1008,RUBBER=1009,PRESS=1010,MILL=1011,DIAL=1012;
   static boolean tile(int t){return t>=BASE&&t<BASE+COUNT;}
   static int[] paint(int tile){if(tile>=CASING)return component(tile);int type=tile-BASE;int[] p=new int[1024];for(int y=0;y<32;y++)for(int x=0;x<32;x++){
      boolean on;int color;
      if(type==4){on=(Math.abs(x+y-31)<3&&x>=8&&x<25)||(x>=19&&x<29&&y>=3&&y<13&&!(x>=23&&y<=8));color=0xffc8a05b;if(on&&(x+y)%4==0)color=0xffe8ce8a;}
      else if(type==3){on=y>=9&&y<=27&&x>=7+(y>23?2:0)&&x<=24-(y>23?2:0);color=y<14?0xffa98552:0xffd8c399;if(on&&x%6==0&&y>14)color=0xffb6a179;}
      else if(type==0){on=x>=5&&x<=27&&y>=10&&y<=24&&x+y>=17&&x+y<49;color=y<15?0xffbac6b5:y>21?0xff58695c:0xff879988;}
      else{on=x>=5&&x<=27&&y>=7&&y<=25&&x+y>=17&&x+y<49;color=type==1?0xffaebbc1:0xffd8ac50;if(on&&(x==6||y==8||x+y==18))color=type==1?0xffe2ebec:0xfff8df8a;if(on&&(y==24||x==26))color=type==1?0xff647780:0xff856028;}
      p[y*32+x]=on?color:0;
   }return p;}
   /** Purpose-made machine surfaces, never borrowed full-block/plank textures. */
   static int[] component(int t){
      PixelArt32 a=new PixelArt32();int c=t==TIMBER?0x88623f:t==BRASS?0xb89a58:t==RUBBER?0x343a3b:t==MILL?0x777d73:t==DIAL?0xd9cfaa:0x647879;
      a.material(0,0,32,32,c,t,2);
      if(t==TIMBER){for(int y=3;y<32;y+=5){a.line(0,y,31,y,0x705039);a.line(4,y+1,23,y+1,0x9c774e);}}
      else if(t==RUBBER){a.line(0,2,31,2,0x616967);a.line(0,29,31,29,0x20292a);for(int x=2;x<32;x+=4){a.rect(x,5,2,1,0x8c8168);a.rect(x,26,2,1,0x8c8168);}}
      else if(t==MILL){for(int i=0;i<32;i+=8){a.line(i,0,31,31-i,0x545f59);a.line(0,i,31-i,31,0x969c8e);}}
      else if(t==AXLE){for(int y=1;y<32;y+=8){a.line(0,y,31,y,0x9baaaa);a.line(0,y+5,31,y+5,0x3e5255);}}
      else if(t==DIAL){a.outline(0,0,32,32,0x665339);for(int x=4;x<30;x+=5)a.line(x,4,x,8,0x484b43);}
      else{a.outline(0,0,32,32,0x33494c);a.outline(2,2,28,28,t==BRASS?0xe1c57d:0xa4b2ac);for(int x:new int[]{4,26})for(int y:new int[]{4,26}){a.rect(x,y,2,2,0x263b40);a.set(x,y,0xe2c480);}if(t==PRESS)for(int x=0;x<32;x+=8)a.line(x,24,x+6,30,0xc6a557);}
      return a.p;
   }
   static void low(){for(int t=BASE;t<BASE+COUNT;t++){int[] high=paint(t);for(int y=0;y<16;y++)for(int x=0;x<16;x++)Tex.atlasPixels[(Tex.ty(t)+y)*Tex.PX+Tex.tx(t)+x]=high[(y*2)*32+x*2];}}
}
