package bc;

/** Utility-review item silhouettes. Native 32px; deliberately leaves the accepted
 * flat animated bow, shears and flint-and-steel alone. Gun poses are not touched. */
final class TexTools32 {
   private TexTools32(){}
   static boolean handles(int t){return t>=105&&t<=124||t>=198&&t<=202||t>=135&&t<=150
      ||t==76||t>=190&&t<=192||t==239||t==677||t==678||t==679||t>=688&&t<=693;}
   static int[] paint(int t){
      if(t>=105&&t<=124||t>=198&&t<=202)return tool(t);
      if(t>=135&&t<=150)return armor(t);
      PixelArt32 a=new PixelArt32();
      if(t>=190&&t<=192||t==239)bucket(a,t);
      else if(t==677||t==678)dial(a,t==678);
      else if(t==76){
         a.stroke(5,27,24,8,0x977C51,2);
         a.poly(0xD0D5C4,76,21,7,28,3,26,11,24,10,22,12);
         a.line(23,7,27,4,0xEEF0DC);
         a.poly(0xC7C9B9,77,4,21,9,22,10,27,5,28,3,30,3,26);
         a.line(4,26,8,23,0x858E7E);
      }else if(t==679){
         a.poly(0x393B35,680,7,23,6,15,12,8,15,3,18,10,25,6,23,16,28,15,25,25,18,29,11,28);
         a.poly(0xAD542D,681,9,22,10,15,15,10,16,18,22,13,24,23,18,27,12,26);
         a.poly(0xEBA348,682,13,24,12,20,16,14,18,22,21,18,22,24,17,26);
         a.line(15,22,17,24,0xFFE4A2);
      }else weapon(a,t);
      return a.p;
   }
   private static int light(int c){return PixelArt32.shade(c,25);}
   private static int dark(int c){return PixelArt32.shade(c,-27);}
   static int[] tool(int t){
      PixelArt32 a=new PixelArt32();int tier=t>=198?t-198:(t-105)/4,kind=t>=198?4:(t-105)%4,c=TexItems32.TIERS[tier];
      a.stroke(5,28,22,10,0x86633D,2);a.line(5,27,20,11,0xB09562);
      for(int k=0;k<3;k++)a.line(6+k*2,26-k*2,7+k*2,27-k*2,0x66492F);
      if(kind==0){
         a.poly(c,105,12,18,24,4,28,2,27,7,15,21);
         a.line(13,18,26,4,light(c));a.line(15,19,26,7,dark(c));
         a.stroke(9,15,18,24,dark(c),2);a.line(9,15,18,24,light(c));
         a.line(4,28,6,30,c);
      }else if(kind==1){
         a.poly(c,106,4,8,10,4,18,4,24,8,27,14,28,18,25,14,21,10,17,8,10,7);
         a.line(6,7,10,5,light(c));a.line(10,5,17,5,light(c));a.line(18,5,23,8,light(c));
         a.line(24,10,26,14,dark(c));a.set(19,8,0x675C47);
      }else if(kind==2){
         a.poly(c,107,10,4,17,4,23,9,21,12,17,10,16,15,11,18,7,15,7,10);
         a.line(8,10,8,14,light(c));a.line(9,15,11,16,light(c));a.line(12,16,15,14,light(c));
         a.line(12,5,17,5,PixelArt32.shade(c,12));a.line(17,6,21,9,dark(c));
      }else if(kind==3){
         a.poly(c,108,23,3,27,5,28,9,24,15,20,17,17,14,17,10);
         a.line(23,4,26,6,light(c));a.line(19,8,18,11,light(c));
         a.line(21,15,25,11,dark(c));a.line(20,13,25,7,PixelArt32.shade(c,8));
      }else{
         a.poly(c,109,5,7,13,4,19,5,25,10,26,14,23,12,18,8,12,7,5,9);
         a.line(6,7,13,5,light(c));a.line(14,5,19,6,light(c));a.line(23,10,25,13,dark(c));
      }
      return a.p;
   }
   static int[] armor(int t){
      PixelArt32 a=new PixelArt32();int mat=(t-135)/4,k=(t-135)%4,c=mat==0?PixelArt32.LEATHER:TexItems32.TIERS[mat+1];
      if(k==0){
         a.poly(c,t,7,21,7,12,10,8,15,6,21,7,25,11,25,22,21,24,21,16,11,16,11,23);
         a.line(10,10,14,8,light(c));a.line(15,8,20,9,light(c));a.line(22,11,23,14,dark(c));
         a.line(8,15,23,15,dark(c));a.line(8,16,8,21,light(c));a.set(22,20,light(c));
      }else if(k==1){
         a.poly(c,t,5,9,10,6,13,10,19,10,22,6,27,9,26,15,22,16,22,27,10,27,10,16,6,15);
         a.line(7,9,10,8,light(c));a.line(22,8,25,10,light(c));
         a.line(12,12,12,23,light(c));a.line(20,13,20,24,dark(c));
         a.line(13,24,19,24,PixelArt32.shade(c,-12));a.line(16,12,16,23,PixelArt32.shade(c,-10));
      }else if(k==2){
         a.poly(c,t,9,5,23,5,23,27,18,27,17,15,15,15,14,27,9,27);
         a.rect(9,6,14,2,dark(c));a.rect(15,6,3,2,0xC6B78F);
         a.line(10,11,10,25,light(c));a.line(19,14,20,25,light(c));
         a.line(10,20,13,20,dark(c));a.line(19,20,22,20,dark(c));
      }else{
         a.poly(c,t,8,7,13,7,13,27,5,27,4,24,7,21);
         a.poly(c,t+1,19,7,24,7,24,21,28,24,27,27,19,27);
         a.line(8,8,11,8,light(c));a.line(20,8,23,8,light(c));
         a.line(8,10,8,20,light(c));a.line(20,10,20,20,light(c));
         a.line(5,26,12,26,dark(c));a.line(20,26,27,26,dark(c));
         a.line(8,20,12,20,dark(c));a.line(20,20,23,20,dark(c));
      }
      if(mat==0){for(int y=12;y<24;y+=3){int x=k==0?9:k==1?11:k==2?10:9;if((a.p[y*32+x]>>>24)!=0)a.set(x,y,0xC49E69);}}
      return a.p;
   }
   private static void bucket(PixelArt32 a,int t){
      a.line(8,12,10,6,0x697A77);a.line(10,6,20,4,0xBEC9BF);a.line(20,4,25,10,0x768883);
      a.poly(0x7D918B,t,7,12,25,12,23,27,12,28,9,24);
      a.poly(0xB0BDB2,t+1,9,14,23,14,21,25,13,26,11,23);
      a.ellipse(7,9,19,7,0xCAD1BD,t+2,false);
      a.ellipse(9,11,15,4,t==190?0x465C56:t==191?0x416F93:t==192?0xDD8737:0xDFDFCA,t+3,false);
      a.line(10,16,12,23,0xD4DBCA);a.line(21,16,20,24,0x7B9389);
      if(t==192){a.line(12,12,16,12,0xFBD37B);a.set(20,13,0xF7BA57);}
   }
   private static void dial(PixelArt32 a,boolean clock){
      int c=clock?PixelArt32.GOLD:PixelArt32.IRON;
      a.outline(14,3,5,4,dark(c));a.ellipse(6,6,21,23,dark(c),677,false);
      a.ellipse(7,7,19,20,c,678,false);a.ellipse(10,10,13,14,clock?0x556D7A:0xD2D1B9,679,false);
      for(int[] q:new int[][]{{16,10},{22,16},{16,23},{10,16}})a.set(q[0],q[1],clock?0xD6BA7D:0x69796C);
      if(clock){a.line(16,17,16,12,0xE8D39B);a.line(16,17,20,19,0xE8D39B);}
      else{a.poly(0xB34837,680,15,17,19,11,18,18);a.poly(0x586F69,681,15,16,18,17,13,23);}
      a.set(16,17,0xE7D8AC);
   }
   private static void weapon(PixelArt32 a,int t){
      int steel=0x81938D,deep=0x354942;
      if(t==692){
         a.stroke(5,28,13,19,0x756044,3);a.line(6,26,11,21,0xAA9164);
         a.poly(steel,t,12,18,25,4,28,3,26,10,16,21);a.line(13,18,26,5,0xCED9CB);
         a.line(10,17,17,24,deep);return;
      }
      if(t==691){
         a.poly(0x687B53,t,9,17,13,12,21,12,25,18,24,25,20,28,13,28,9,24);
         a.line(11,19,23,19,0x3E5037);a.line(11,24,23,24,0x3E5037);a.line(16,14,16,27,0x879363);
         a.rect(13,9,7,4,steel);a.outline(19,5,6,6,0xA6B39F);a.line(14,8,23,14,0xC0CAB7);return;
      }
      if(t==693){
         a.material(8,11,17,16,0x717E59,t,3);a.outline(8,11,17,16,deep);
         a.rect(10,14,13,6,0x334D40);a.rect(12,16,8,2,0xBAD094);
         a.line(10,11,14,5,0xAE5940);a.line(14,5,22,9,0xC9AA64);
         for(int x=12;x<23;x+=4)a.rect(x,23,2,2,0xB2BD9A);return;
      }
      // Deliberately distinct pistol / SMG / long rifle silhouettes, not one recoloured body.
      if(t==688){
         a.poly(deep,t,7,10,26,10,28,12,27,15,17,15,14,19,12,27,7,26,9,17);
         a.material(8,11,18,3,steel,t,2);a.poly(0x806B4E,t,9,17,14,18,11,25,8,24);
         a.line(17,16,17,19,steel);a.line(14,20,17,19,steel);a.rect(24,8,2,2,deep);
      }else{
         a.rect(8,13,20,4,deep);a.rect(9,13,12,2,steel);a.rect(27,14,4,2,0x566D65);
         a.poly(0x806B4E,t,2,15,8,14,9,18,3,20);a.poly(0x556B5B,t,12,17,15,17,13,25,10,24);
         if(t==689){a.rect(19,17,3,7,0x4B6355);a.rect(19,11,2,2,deep);a.line(15,19,18,19,steel);}
         else{a.rect(14,8,12,3,0x56726A);a.rect(16,11,2,2,steel);a.rect(24,7,3,5,deep);a.rect(22,16,7,2,0x8F8060);}
      }
   }
}
