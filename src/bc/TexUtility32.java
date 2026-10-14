package bc;

/** Decorations and utility review. Component cells 760..780 prevent full block
 * pictures being stretched over handles, caps and tiny mechanical parts. */
final class TexUtility32 {
   private TexUtility32(){}
   static boolean handles(int t){return t>=760&&t<=780||t==30||t==36||t>=39&&t<=43||t>=180&&t<=182||t==212||t==213||t==220||t==223||t==224||t>=231&&t<=234||t==237||t==238||t>=265&&t<=267||t==269||t==279||t==641||t==644||t==645||t==646||t==659||t==662||t==663||t==664||t==669||t==671||t>=716&&t<=731;}
   static int[] paint(int t){
      PixelArt32 a=new PixelArt32();
      if(t>=760&&t<=780)return component(t);
      if(t>=716&&t<=731)return banner(t-716);
      if(t==36||t==646||t==662)return torch(t);
      if(t==39||t==40)return furnace(t==40,0);
      if(t==237||t==238)return pages(t==238);
      if(t==279)return eye();
      switch(t){
         case 30: glowstone(a);break;
         case 41:case 42: chest(a,t==41);break;
         case 43: cage(a);break;
         case 180:case 181:case 182: tnt(a,t);break;
         case 212:case 213: hay(a,t==213);break;
         case 220: books(a);break;
         case 223:case 224: pumpkin(a,t==224);break;
         case 231:case 232:case 233:case 234: cake(a,t);break;
         case 265:case 266:case 267: portal(a,t);break;
         case 269: shell(a);break;
         case 641: cloth(a);break;
         case 644:case 645: lamp(a,t==645);break;
         case 659: lantern(a);break;
         case 663:case 664: repeater(a,t==664);break;
         case 669: sensor(a,false);break;
         case 671: observer(a);break;
         default:throw new IllegalArgumentException("Unknown utility "+t);
      }
      return a.p;
   }
   private static int shade(int c,int d){return PixelArt32.shade(c,d);}
   private static float h(int x,int y,int s){return TexPixel32.hash(x,y,s);}
   private static void base(PixelArt32 a,int t){System.arraycopy(TexMaterials32.base(t),0,a.p,0,1024);}
   private static void material(PixelArt32 a,int c,int seed){a.material(0,0,32,32,c,seed,3);}
   private static int[] component(int t){
      PixelArt32 a=new PixelArt32();
      switch(t){
         case 760: material(a,0xE6AC4A,t);a.rect(9,9,14,14,0xFFE7A2);break;
         case 761:case 778:material(a,0x85613B,t);for(int x=2;x<32;x+=7)a.line(x,0,x,31,0xA17D4D);break;
         case 762: material(a,0xB04A2F,t);a.rect(7,3,18,18,0xEE9255);a.line(10,4,20,4,0xFFD398);break;
         case 763: material(a,0x65423A,t);a.rect(9,6,14,14,0x7B5142);break;
         case 764: base(a,Tex.PLANKS);a.rect(0,0,32,2,0x684E32);a.rect(0,29,32,3,0x614B32);a.rect(0,0,2,32,0x715339);a.rect(30,0,2,32,0x715339);break;
         case 765:case 766:base(a,Tex.STONE);a.outline(0,0,32,32,0x555B53);if(t==765){a.line(3,8,28,8,0x747A71);a.line(3,24,28,24,0x656D65);}else{a.outline(4,4,24,24,0x737A71);a.line(6,6,25,6,0xA0A396);}break;
         case 767:sensor(a,true);break;
         case 768:material(a,0x78A88B,t);a.poly(0x304C3E,1,16,3,22,12,20,23,16,29,12,23,10,12);a.line(16,8,16,23,0x121F23);a.rect(9,7,3,3,0xC7DABB);break;
         case 769:material(a,0x426E5A,t);a.line(0,5,31,5,0x85AE8D);break;
         case 770: material(a,0x7E8C86,t);a.line(0,5,31,5,0xB8C4B7);a.line(0,25,31,25,0x45544E);break;
         case 771: material(a,0x858D83,t);break;
         case 772: material(a,0xA58753,t);for(int y=4;y<32;y+=6)a.line(0,y,31,y,0x8B7047);break;
         case 773:shell(a);break;
         case 774:material(a,0xD59839,t);a.poly(0xF7DA83,t,3,28,5,14,12,2,15,14,24,4,29,20,27,31);a.poly(0xFFF0B3,t,10,31,12,20,19,14,23,30);break;
         case 775:case 776:material(a,0x72796F,t);a.outline(9,9,14,14,0x47554C);a.rect(12,12,8,8,t==776?0xF09D62:0x623F36);break;
         case 777:material(a,0xA3A59A,t);a.line(0,3,31,3,0xD0CCB7);a.line(0,27,31,27,0x707970);break;
         case 779:material(a,0xC5C5C5,t);break;
         case 780:material(a,0x818679,t);a.outline(1,1,30,30,0x586155);a.line(3,3,28,3,0xABB09B);break;
      }return a.p;
   }
   private static void glowstone(PixelArt32 a){
      int[] pigments={0xE7BE73,0xF4D68D,0xCF9C51,0xFBE3A5,0xDBAB60};
      float[] sx=new float[24],sy=new float[24];
      for(int i=0;i<24;i++){sx[i]=h(i,0,30)*32;sy[i]=h(i,1,30)*32;}
      for(int y=0;y<32;y++)for(int x=0;x<32;x++){
         float first=1000,second=1000;int cell=0;
         for(int i=0;i<24;i++){
            float dx=Math.abs(x-sx[i]),dy=Math.abs(y-sy[i]);dx=Math.min(dx,32-dx);dy=Math.min(dy,32-dy);
            float d=Math.max(dx,dy)+.45F*Math.min(dx,dy);
            if(d<first){second=first;first=d;cell=i;}else if(d<second)second=d;
         }
         float edge=second-first;
         int color=edge<.55F?0x795330:edge<1.1F?0xAC773B:pigments[cell%pigments.length];
         a.set(x,y,shade(color,PixelArt32.grain(x,y,30,3)));
      }
   }
   private static int[] torch(int t){PixelArt32 a=new PixelArt32();a.material(14,14,4,18,0x815B35,36,3);a.line(14,16,14,30,0xB38B50);a.rect(13,14,6,2,0x4F4030);
      if(t==662){a.material(12,10,8,5,0x735044,662,3);a.line(14,10,18,10,0x997455);}
      else{a.poly(t==646?0xAF442B:0xBD722C,36,11,14,12,8,15,3,16,9,20,6,21,12,19,15);a.poly(t==646?0xEE8B54:0xF6C969,37,13,14,14,9,17,7,19,12,18,15);a.rect(15,11,2,3,t==646?0xFFBB79:0xFFF0AA);}return a.p;}
   static int[] furnace(boolean lit,int phase){PixelArt32 a=new PixelArt32();base(a,Tex.STONE);a.outline(0,0,32,32,0x596158);a.material(4,4,24,7,0x303A33,39,2);a.line(5,11,26,11,0xB0B1A0);for(int x=8;x<27;x+=7)a.line(x,5,x,9,0x616F60);a.material(4,17,24,12,0x242B27,40,2);a.line(4,29,27,29,0xA4A893);
      if(lit)for(int x=6;x<26;x++){int height=3+(int)(h(x/2,phase%8,401)*7);for(int y=28-height;y<28;y++)a.set(x,y,y>24?0xEAC475:0xC37A3D);if(x%3==0)a.set(x,27,0xFFE3A2);}return a.p;}
   private static void chest(PixelArt32 a,boolean top){base(a,Tex.PLANKS);a.rect(0,0,32,2,0x725234);a.rect(0,30,32,2,0x594632);a.rect(0,0,2,32,0x6D5236);a.rect(30,0,2,32,0x6D5236);
      if(!top){a.line(0,11,31,11,0x473C2B);a.line(2,12,29,12,0xC4A172);a.material(13,9,6,10,0x7C8880,42,2);a.line(14,10,17,10,0xC6CFBB);a.rect(15,13,2,3,0x344A40);}else{a.line(8,3,8,28,0x8C7047);a.line(24,3,24,28,0x8C7047);}}
   private static void cage(PixelArt32 a){for(int x=0;x<32;x+=8){a.rect(x,0,2,32,0x3E504E);a.line(x,0,x,31,0x758C7F);}for(int y=0;y<32;y+=8){a.rect(0,y,32,2,0x394B49);a.line(0,y,31,y,0x6D8477);}a.outline(0,0,32,32,0x83948A);for(int y=2;y<32;y+=8)for(int x=2;x<32;x+=8){a.line(x,y,x+5,y+5,0x4B645C);a.set(x,y,0xB1BC9F);}}
   private static void tnt(PixelArt32 a,int t){material(a,0xB6553C,t);for(int x=0;x<32;x+=8){a.line(x,0,x,31,0x7D3D2F);a.line(x+1,0,x+1,31,0xD07B54);}
      if(t==180){a.material(0,11,32,11,0xDFD5B9,180,2);String[] glyph={"11111 10001 11111","00100 11001 00100","00100 10101 00100","00100 10101 00100","00100 10011 00100","00100 10001 00100","00100 10001 00100"};for(int y=0;y<7;y++)for(int x=0;x<17;x++)if(glyph[y].charAt(x)=='1')a.set(7+x,13+y,0x493D30);}
      else{for(int y=3;y<32;y+=8)for(int x=3;x<32;x+=8){a.ellipse(x-2,y-2,6,6,0xD18458,181,false);a.rect(x,y,2,2,0x784333);}if(t==181){a.rect(12,12,8,8,0xC9B48B);a.line(16,15,20,9,0x55432B);a.line(20,9,25,10,0x55432B);a.line(16,14,19,9,0x927A4D);}}}
   private static void hay(PixelArt32 a,boolean top){material(a,0xB39B46,212);for(int i=0;i<130;i++){int x=(int)(h(i,0,213)*32),y=(int)(h(i,1,213)*32),c=i%3==0?0xE0C66D:i%3==1?0xC6AE55:0x8E7935;if(top){a.line(x,y,x+1,y+1,c);a.set(x+1,y,0xA38D40);}else a.line(x,y,x,y+3+i%7,c);}if(!top)for(int y:new int[]{7,24}){a.rect(0,y,32,2,0x926047);a.line(0,y,31,y,0xB27E51);}}
   private static void books(PixelArt32 a){base(a,Tex.PLANKS);int[] col={0x805046,0x4D7180,0x6D8051,0xAC874E,0x795C7E,0xB8AF8F};for(int row=0;row<2;row++){int y=2+row*15;a.rect(2,y,28,12,0x423C2E);int x=3;for(int i=0;i<7;i++){int w=i%3==0?4:3,hh=9+i%3;a.material(x,y+12-hh,w,hh,col[(i+row)%6],220+i,2);a.line(x,y+12-hh,x,y+11,shade(col[(i+row)%6],13));a.line(x+1,y+4,x+w-1,y+4,0xC5B792);a.set(x+1,y+8,0xD8C4A0);a.line(x+1,y+10,x+w-1,y+10,0x524935);x+=w+1;if(x>28)break;}a.rect(0,y+12,32,2,0xA48756);a.line(0,y+14,31,y+14,0x685234);}}
   private static void pumpkin(PixelArt32 a,boolean lit){System.arraycopy(TexColorNature32.paint(222),0,a.p,0,1024);int c=lit?0xF6D184:0x3B2B1C;a.poly(c,223,4,9,12,6,11,14,6,13);a.poly(c,224,20,6,28,9,26,13,21,14);a.poly(c,225,15,13,18,17,13,17);a.poly(c,226,4,20,9,22,12,19,15,23,18,20,21,23,28,19,26,28,22,27,19,25,16,28,12,25,8,27,5,25);a.rect(9,23,2,3,0x84501E);a.rect(21,22,2,3,0x84501E);}
   private static void cake(PixelArt32 a,int t){material(a,t==231?0xE5D7BF:0xB08A59,t);if(t==231){for(int[] p:new int[][]{{4,5},{17,3},{25,10},{10,15},{21,23},{5,26}}){a.poly(0xAE4134,231,p[0],p[1],p[0]+3,p[1]-1,p[0]+4,p[1]+2,p[0]+1,p[1]+3);a.set(p[0]+1,p[1],0xDB7760);}a.outline(0,0,32,32,0xCEBEA4);}else if(t!=233){for(int y=0;y<32;y++)for(int x=0;x<32;x++){if(y<19+(int)(h(x/3,0,232)*3))a.set(x,y,0xE4D4B7);else if(y==26)a.set(x,y,0x8D4B35);else if(y==27)a.set(x,y,0xDDC29B);else if(h(x,y,233)>.91)a.set(x,y,0x8E6840);}}}
   private static void portal(PixelArt32 a,int t){material(a,0x637863,t);for(int y=0;y<32;y+=8)a.line(0,y,31,y,0x425B4B);a.outline(0,0,32,32,0xA9AF87);if(t!=267){a.rect(8,8,16,16,0x243C34);a.outline(6,6,20,20,0xA5B083);for(int x=3;x<30;x+=7){a.line(x,2,x+2,4,0xCDD0A2);a.line(x,28,x+2,26,0xBDD09F);}if(t==266){a.ellipse(9,9,14,14,0x8BAC87,266,false);a.poly(0x203830,267,16,10,19,15,16,22,13,16);}}}
   private static void shell(PixelArt32 a){material(a,0x28232F,773);for(int y=1;y<32;y+=4)for(int x=1+(y/4%2)*3;x<32;x+=6){a.line(x,y,x+3,y,0x423346);a.line(x+3,y,x+4,y+2,0x1B1B27);if(h(x,y,269)>.6)a.set(x+1,y+1,0x65516C);}}
   private static void cloth(PixelArt32 a){for(int y=0;y<32;y++)for(int x=0;x<32;x++){int d=(int)(h(x,y,641)*5)-2+(x%8==2?5:x%8==6?-6:0);a.set(x,y,shade(0xD5D5D5,d));}a.line(1,0,1,31,0xB8B8B8);a.line(30,0,30,31,0xB8B8B8);a.line(0,30,31,30,0xBDBDBD);}
   private static void lamp(PixelArt32 a,boolean lit){material(a,0x68513A,644);for(int y=3;y<30;y+=9)for(int x=3;x<30;x+=9){a.material(x,y,7,7,lit?0xE6BB70:0x937449,645+x+y,3);a.line(x,y,x+6,y,lit?0xFFE2A0:0xB59867);}a.outline(0,0,32,32,0x423F30);a.outline(1,1,30,30,0x96815B);}
   private static void lantern(PixelArt32 a){material(a,0x699A8D,659);a.material(4,4,24,24,0xDCE9C7,660,2);a.outline(3,3,26,26,0xA8C6AC);a.poly(0xF2F1D5,661,8,8,22,6,25,18,18,25,7,22);a.line(9,9,20,8,0xFFFFFF);for(int x:new int[]{1,29})for(int y:new int[]{1,29})a.rect(x,y,2,2,0x416F66);}
   private static void repeater(PixelArt32 a,boolean on){material(a,0xB7B4A4,663);a.outline(0,0,32,32,0x777F70);a.line(15,5,15,27,on?0xC65D39:0x703E2D);a.line(16,5,16,27,on?0xE48D51:0x8B4A33);a.rect(8,14,16,2,0x7A7867);for(int x:new int[]{4,26}){a.rect(x,4,2,2,0x707B71);a.rect(x,26,2,2,0x707B71);}}
   private static void sensor(PixelArt32 a,boolean on){material(a,0xAA8855,669);for(int y=3;y<30;y+=9)for(int x=3;x<30;x+=9){a.material(x,y,8,8,on?0xA7C0BA:0x465B68,669,2);a.line(x,y,x+7,y,on?0xE0DEAC:0x849AA1);}a.rect(13,28,6,2,on?0xDCAA59:0x574B3A);}
   private static void observer(PixelArt32 a){base(a,Tex.STONE);a.rect(4,7,9,7,0x34403A);a.rect(19,7,9,7,0x34403A);a.rect(6,8,5,3,0xA4A99C);a.rect(21,8,5,3,0xA4A99C);a.rect(7,22,18,2,0x505E52);a.line(7,24,24,24,0xACAD9C);}
   private static int[] eye(){PixelArt32 a=new PixelArt32();a.poly(0x304F4B,279,2,16,8,7,16,3,24,7,30,16,24,25,16,29,8,25);a.poly(0x80A47D,280,4,16,10,9,16,6,23,10,27,16,22,23,16,26,9,22);a.ellipse(10,7,13,19,0x4B9180,281,false);a.poly(0x182E2D,282,16,8,19,14,18,22,16,25,14,21,13,14);a.rect(10,9,3,3,0xD2E5B3);return a.p;}
   private static int[] pages(boolean book){PixelArt32 a=new PixelArt32();if(book){a.poly(0x64462F,238,5,7,21,3,28,8,28,25,11,30,5,26);a.poly(0x8B5541,239,5,7,21,3,26,7,10,12);a.poly(0xB17D53,240,5,7,10,12,11,30,5,26);a.poly(0xD7CEB2,241,11,13,26,9,26,23,12,27);for(int y=15;y<26;y+=3)a.line(13,y,25,y-4,0xB2AB92);a.rect(19,9,2,9,0x9B4938);}else{a.poly(0xBCB79F,237,6,5,23,5,28,10,27,28,6,28);a.poly(0xE4DDC4,238,7,4,22,4,26,10,25,26,7,26);a.poly(0xC9C4AC,239,22,4,27,10,22,10);a.line(10,17,21,17,0xCDC7AC);a.line(10,20,19,20,0xCDC7AC);}return a.p;}
   private static int[] banner(int color){PixelArt32 a=new PixelArt32();int c=PixelArt32.wool(color);a.line(9,3,9,29,0xA1804E);a.line(10,3,10,29,0x6D5335);a.rect(7,3,20,2,0xA68450);a.poly(c,716,11,5,26,5,25,24,22,25,18,24,11,25);a.line(12,6,24,6,shade(c,14));a.line(19,8,20,22,shade(c,-15));a.line(12,24,17,23,shade(c,-12));a.line(6,30,15,30,0x80643D);return a.p;}
}
