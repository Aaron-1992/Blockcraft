package bc;

/** Original 32px material/item pixels for native machinery; no Create asset files. */
final class KineticArt {
   static final int BASE=1000,COUNT=5;
   static boolean tile(int t){return t>=BASE&&t<BASE+COUNT;}
   static int[] paint(int tile){int type=tile-BASE;int[] p=new int[1024];for(int y=0;y<32;y++)for(int x=0;x<32;x++){
      boolean on;int color;
      if(type==4){on=(Math.abs(x+y-31)<3&&x>=8&&x<25)||(x>=19&&x<29&&y>=3&&y<13&&!(x>=23&&y<=8));color=0xffc8a05b;if(on&&(x+y)%4==0)color=0xffe8ce8a;}
      else if(type==3){on=y>=9&&y<=27&&x>=7+(y>23?2:0)&&x<=24-(y>23?2:0);color=y<14?0xffa98552:0xffd8c399;if(on&&x%6==0&&y>14)color=0xffb6a179;}
      else if(type==0){on=x>=5&&x<=27&&y>=10&&y<=24&&x+y>=17&&x+y<49;color=y<15?0xffbac6b5:y>21?0xff58695c:0xff879988;}
      else{on=x>=5&&x<=27&&y>=7&&y<=25&&x+y>=17&&x+y<49;color=type==1?0xffaebbc1:0xffd8ac50;if(on&&(x==6||y==8||x+y==18))color=type==1?0xffe2ebec:0xfff8df8a;if(on&&(y==24||x==26))color=type==1?0xff647780:0xff856028;}
      p[y*32+x]=on?color:0;
   }return p;}
   static void low(){for(int t=BASE;t<BASE+COUNT;t++){int[] high=paint(t);for(int y=0;y<16;y++)for(int x=0;x<16;x++)Tex.atlasPixels[(Tex.ty(t)+y)*Tex.PX+Tex.tx(t)+x]=high[(y*2)*32+x*2];}}
}
