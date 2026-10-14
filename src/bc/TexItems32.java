package bc;

/** Material-led item art. All recipes are native 32px, including their silhouettes.
 * Tools share their material colors with mineral blocks; wood and textiles share
 * the construction palette. No enlarged legacy sprite is used as a stencil.
 */
final class TexItems32 {
   private TexItems32(){}
   static final int[] TIERS={PixelArt32.OAK,PixelArt32.STONE,PixelArt32.IRON,PixelArt32.GOLD,PixelArt32.DIAMOND};
   static int[] paint(int t){
      PixelArt32 a=new PixelArt32();
      if(t>=105&&t<=124||t>=198&&t<=202)return tool(t);
      if(t>=135&&t<=150)return armor(t);
      if(t>=700&&t<=731)return decoration(t);
      if(t==88||t>=590&&t<=595){door(a,t);return a.p;}
      if(t>=281&&t<=295||t==74||t==217||t==236||t==275||t==660){powder(a,t);return a.p;}
      if(t>=688&&t<=695){weapon(a,t);return a.p;}
      switch(t){
         case 65:case 274: rod(a,t==65?0x977647:0xD6A958);break;
         case 66:case 75:case 272: mineral(a,t);break;
         case 67:case 68:case 273: ingot(a,t==67?PixelArt32.IRON:t==68?PixelArt32.GOLD:TexMaterials32.NETHER);break;
         case 69:case 280: gem(a,t==69?PixelArt32.DIAMOND:0x54A17B,t==280);break;
         case 70: hide(a);break;
         case 71: feather(a);break;
         case 72: thread(a);break;
         case 73: bone(a);break;
         case 76: arrow(a);break;
         case 78:case 672: apple(a,t==672);break;
         case 79:case 80:case 81:case 82:case 83:case 84:case 85:case 86:case 87: meat(a,t);break;
         case 190:case 191:case 192:case 239: bucket(a,t);break;
         case 193:case 241:case 242: seeds(a,t);break;
         case 194: wheat(a);break;
         case 195: bread(a);break;
         case 196: striker(a);break;
         case 214:case 673: carrot(a,t==673);break;
         case 215:case 216: potato(a,t==216);break;
         case 218: shears(a);break;
         case 237:case 238: pages(a,t==238);break;
         case 240: egg(a);break;
         case 243: melon(a);break;
         case 244: pie(a);break;
         case 245: cake(a);break;
         case 276: tear(a);break;
         case 277: nugget(a);break;
         case 278:case 279:case 679: orb(a,t);break;
         case 674: cookie(a);break;
         case 675:case 676: bowl(a,t==675);break;
         case 677:case 678: dial(a,t==678);break;
         case 742: bottle(a);break;
         default:throw new IllegalArgumentException("Unpainted item cell "+t);
      }
      return a.p;
   }
   private static void handle(PixelArt32 a){
      a.poly(0x725435,10,3,27,19,10,23,13,7,30);
      a.line(5,27,20,11,0xB79A64);a.line(7,28,21,13,0x896B42);
      for(int k=0;k<3;k++)a.line(6+k*2,26-k*2,8+k*2,28-k*2,0x624A33);
   }
   static int[] tool(int t){
      PixelArt32 a=new PixelArt32();int tier=t>=198?t-198:(t-105)/4,kind=t>=198?4:(t-105)%4,c=TIERS[tier];handle(a);
      if(kind==0){
         a.poly(PixelArt32.shade(c,-26),40,10,18,25,3,29,2,28,7,15,22);
         a.poly(c,41,11,18,26,3,28,3,15,20);
         a.line(13,16,26,3,PixelArt32.shade(c,30));a.line(16,18,27,7,PixelArt32.shade(c,-15));
         a.poly(PixelArt32.shade(c,-12),44,7,14,9,13,21,24,19,26,17,24,7,16);
         a.line(8,14,19,24,PixelArt32.shade(c,16));a.material(3,27,3,3,c,22,3);
      }else if(kind==1){
         a.poly(PixelArt32.shade(c,-18),42,3,8,7,5,13,3,19,4,24,7,27,11,29,17,28,21,25,16,22,12,18,10,13,8,8,8,4,10);
         a.poly(c,43,5,7,13,4,19,5,24,8,27,13,27,16,22,10,17,8,11,7);
         a.line(7,6,13,4,PixelArt32.shade(c,27));a.line(14,4,19,5,PixelArt32.shade(c,16));a.line(24,9,26,12,PixelArt32.shade(c,12));
         a.rect(19,9,2,2,0x776F53);
      }else if(kind==2){
         a.poly(PixelArt32.shade(c,-20),38,5,4,12,2,19,5,25,11,23,14,19,11,15,12,15,17,11,21,6,20,2,15,2,9);
         a.poly(c,39,5,5,12,3,18,6,22,10,20,11,15,10,13,12,13,16,10,19,7,18,4,14,4,9);
         a.line(3,9,3,14,PixelArt32.shade(c,28));a.line(4,16,8,19,PixelArt32.shade(c,18));a.line(15,5,20,9,PixelArt32.shade(c,-12));
      }else if(kind==3){
         a.poly(PixelArt32.shade(c,-19),35,19,2,27,3,30,10,26,16,20,19,14,13,15,8);
         a.poly(c,36,20,3,26,4,28,10,24,15,20,17,16,12,17,8);
         a.line(20,4,25,5,PixelArt32.shade(c,27));a.line(18,7,17,11,PixelArt32.shade(c,20));a.line(22,14,26,11,PixelArt32.shade(c,-14));
      }else{
         a.poly(PixelArt32.shade(c,-22),50,4,6,14,3,22,5,27,12,26,16,21,11,13,9,5,10);
         a.poly(c,51,5,6,14,4,22,6,24,10,21,9,13,7,5,8);a.line(7,5,14,4,PixelArt32.shade(c,23));
      }
      return a.p;
   }
   static int[] armor(int t){
      PixelArt32 a=new PixelArt32();int mat=(t-135)/4,kind=(t-135)%4,c=mat==0?PixelArt32.LEATHER:TIERS[mat+1],dark=PixelArt32.shade(c,-25),light=PixelArt32.shade(c,18);
      if(kind==0){
         a.poly(dark,71,4,23,4,12,7,7,12,4,22,4,27,8,29,14,27,24,22,25,22,16,11,15,10,24);
         a.poly(c,72,5,15,6,11,10,6,21,5,26,9,27,15,22,13,11,13);
         a.line(11,6,20,6,light);a.material(6,16,3,7,c,73,3);a.material(24,16,3,7,c,74,3);a.line(15,5,15,12,PixelArt32.shade(c,8));
      }else if(kind==1){
         a.poly(dark,80,3,6,10,3,14,7,18,7,22,3,29,6,30,14,25,17,25,28,8,29,7,17,2,14);
         a.poly(c,81,8,7,12,10,20,10,23,6,25,15,23,26,10,27,9,16);
         a.poly(PixelArt32.shade(c,8),82,3,7,8,5,8,14,3,13);a.poly(c,83,24,5,28,7,29,13,25,15);
         a.line(11,13,11,23,light);a.line(22,13,21,24,dark);a.line(15,12,16,24,PixelArt32.shade(c,-10));
      }else if(kind==2){
         a.poly(dark,90,6,3,26,3,25,29,19,29,17,16,14,16,12,29,6,28);
         a.poly(c,91,7,4,24,4,24,12,19,15,12,14,7,12);a.material(7,13,5,15,c,92,3);a.material(20,13,4,15,c,93,3);
         a.line(8,14,8,26,light);a.line(21,16,21,26,light);a.material(7,5,17,2,mat==0?0x614A34:dark,94,2);a.rect(14,5,3,2,0xC2B184);
      }else{
         a.poly(dark,100,6,5,14,5,14,28,3,29,2,24,5,21);a.poly(dark,101,19,5,26,5,27,21,30,24,29,29,18,28);
         a.material(7,6,5,17,c,102,3);a.material(20,6,4,17,c,103,3);a.poly(c,104,5,22,13,22,13,26,3,27,3,25);a.poly(c,105,19,22,27,22,29,26,19,26);
         a.line(7,7,10,7,light);a.line(20,7,23,7,light);a.line(4,27,12,27,dark);a.line(19,27,28,27,dark);
      }
      if(mat==0)for(int y=10;y<24;y+=4){a.set(kind==3?9:11,y,0xCCAC78);}
      return a.p;
   }
   private static void rod(PixelArt32 a,int c){a.poly(PixelArt32.shade(c,-18),10,4,27,23,4,27,3,28,7,8,30);a.line(6,26,25,4,PixelArt32.shade(c,19));a.line(8,27,26,7,c);}
   private static void mineral(PixelArt32 a,int t){int c=t==66?0x404743:t==75?0x64706F:0xD6D1BF;
      a.poly(PixelArt32.shade(c,-12),12,4,22,7,13,14,7,22,4,28,10,26,22,20,28,10,29);
      a.poly(c,13,7,20,9,13,20,6,23,8,18,19,10,26);a.poly(PixelArt32.shade(c,13),14,12,13,21,6,21,12,17,19);
      a.line(20,16,23,22,PixelArt32.shade(c,-20));a.line(9,23,12,18,PixelArt32.shade(c,14));}
   private static void ingot(PixelArt32 a,int c){
      a.poly(PixelArt32.shade(c,-27),20,3,18,8,10,24,7,29,13,28,21,9,26,3,23);
      a.poly(c,21,4,18,9,11,23,8,27,14,22,19,8,22);
      a.poly(PixelArt32.shade(c,-13),22,8,22,22,19,27,15,26,20,9,25);
      a.line(10,11,22,9,PixelArt32.shade(c,24));a.line(5,18,8,21,PixelArt32.shade(c,15));}
   private static void gem(PixelArt32 a,int c,boolean emerald){
      if(emerald){a.poly(PixelArt32.shade(c,-23),24,8,5,23,5,28,11,25,24,19,29,9,25,4,13);a.poly(c,25,9,6,21,6,24,12,22,22,18,26,10,22,7,13);a.line(10,7,20,7,PixelArt32.shade(c,24));a.line(9,9,8,15,PixelArt32.shade(c,29));a.line(21,13,20,22,PixelArt32.shade(c,-17));}
      else{a.poly(PixelArt32.shade(c,-24),26,4,11,10,4,22,4,28,11,25,17,16,29,7,18);a.poly(PixelArt32.shade(c,15),27,10,5,21,5,24,10,9,10);a.poly(c,28,10,11,23,11,16,27);a.poly(PixelArt32.shade(c,-10),29,5,11,9,11,15,26);a.line(10,11,15,25,PixelArt32.shade(c,24));a.line(6,11,9,7,PixelArt32.shade(c,31));}
   }
   private static void hide(PixelArt32 a){a.poly(0x765138,30,5,4,11,4,15,8,21,4,27,5,29,11,25,15,28,24,22,28,16,25,10,29,4,25,7,15,3,10);a.poly(PixelArt32.LEATHER,31,7,6,11,6,15,11,22,6,26,7,27,11,22,15,25,23,21,25,16,22,10,26,7,23,10,15,6,10);a.line(12,12,11,21,0x78563A);a.line(20,13,22,21,0xC19865);}
   private static void feather(PixelArt32 a){a.poly(0xD6D1BA,33,5,28,6,20,9,13,18,5,27,2,28,7,24,17,14,25);for(int i=0;i<7;i++){int x=8+i*2,y=22-i*2;a.line(x,y,x+5,y-1,i%2==0?0xB1B7A4:0xE4DFCA);}a.line(4,29,26,4,0x868D7D);a.line(5,29,27,4,0xB4B69F);}
   private static void thread(PixelArt32 a){int[] q={7,27,4,23,5,17,10,10,19,6,25,8,28,14,26,21,18,26,11,24,9,20,13,16,20,13,22,16,19,21,13,23};for(int i=0;i<q.length-2;i+=2){a.line(q[i],q[i+1],q[i+2],q[i+3],0xC0B99D);a.line(q[i],q[i+1]-1,q[i+2],q[i+3]-1,0xE0D7BD);}}
   private static void bone(PixelArt32 a){a.poly(0xC1BDA5,36,5,25,21,8,26,11,10,29);a.line(8,24,23,9,0xE5DFC6);a.ellipse(3,22,7,7,0xDDD6BC,37,true);a.ellipse(8,24,6,6,0xC9C6AA,38,true);a.ellipse(21,3,7,7,0xDDD6BC,39,true);a.ellipse(24,8,6,6,0xC9C6AA,40,true);}
   private static void arrow(PixelArt32 a){a.stroke(5,27,25,7,0x977D51,2);a.poly(PixelArt32.IRON,42,19,6,29,2,27,13,24,9);a.line(21,6,28,3,0xDBE1D0);a.poly(0xD7D1B7,43,3,21,5,18,12,25,10,29,8,25);a.line(4,21,10,27,0x878D7A);}
   private static void powder(PixelArt32 a,int t){int c=t>=281&&t<=295?PixelArt32.wool(t-280):t==74?0x606961:t==275?0xC59343:t==660?0xA54C3E:0xD4D1B9;
      a.poly(PixelArt32.shade(c,-18),43,3,25,8,18,14,14,19,16,22,21,28,24,27,28,16,30,7,28);
      for(int i=0;i<21;i++){int x=5+(int)(PixelArt32.h(i,0,t)*21),y=17+(int)(PixelArt32.h(i,1,t)*11);a.poly(PixelArt32.shade(c,(i%3-1)*9),t+i,x-1,y,x+1,y-2,x+3,y,x+2,y+2,x,y+2);}
      a.set(6,14,PixelArt32.shade(c,12));a.set(23,16,c);}
   private static void apple(PixelArt32 a,boolean gold){int c=gold?0xCAB05D:0xAD4936;
      a.poly(PixelArt32.shade(c,-23),51,4,14,7,9,12,7,16,10,21,7,26,10,28,16,27,22,23,28,18,29,15,27,11,29,6,25,4,20);
      a.poly(c,52,6,14,9,10,13,10,16,12,21,10,25,13,26,19,23,25,19,27,15,25,11,26,7,22);
      a.stroke(16,11,17,4,0x765638,2);a.leaf(19,7,7,-3,0x6D8349,54);a.line(9,13,8,17,PixelArt32.shade(c,31));a.line(9,18,9,20,PixelArt32.shade(c,16));}
   private static void meat(PixelArt32 a,int t){boolean cooked=t==80||t==82||t==84||t==86;int c=cooked?0x986846:t==87?0x927255:t==79?0xC0917D:0xAD6454;
      if(t==83||t==84){a.stroke(7,25,3,29,PixelArt32.BONE,3);a.ellipse(2,26,5,5,PixelArt32.BONE,59,true);a.poly(PixelArt32.shade(c,-22),60,7,23,9,14,16,7,22,6,28,10,29,17,25,24,18,28,12,27);a.ellipse(11,9,15,16,c,61,true);a.line(14,13,21,10,PixelArt32.shade(c,22));return;}
      a.poly(0xC2A47D,62,3,18,5,12,10,8,20,5,27,9,29,14,27,20,23,23,21,27,15,29,10,26,5,26,3,22);
      a.poly(c,63,5,17,7,13,12,10,19,8,25,10,27,14,25,19,21,21,19,25,14,27,10,24,6,24,5,21);
      a.line(8,18,17,12,PixelArt32.shade(c,-24));a.line(11,23,22,15,PixelArt32.shade(c,-21));a.line(9,15,15,11,PixelArt32.shade(c,19));
      if(!cooked){a.line(20,10,18,15,0xD1B197);a.line(18,15,22,17,0xD1B197);}
      if(t==85||t==86){a.ellipse(18,9,6,6,PixelArt32.BONE,64,true);a.ellipse(20,11,2,2,0x907154,65,false);}}
   private static void bucket(PixelArt32 a,int t){a.poly(0x6C807C,66,3,9,29,9,26,25,22,29,10,29,6,25);a.poly(PixelArt32.IRON,67,6,11,26,11,23,25,20,27,11,27,8,23);a.line(8,14,10,23,0xD4DDCC);a.line(23,14,21,24,0x899D94);a.ellipse(3,5,27,10,0xBAC5B9,68,false);a.ellipse(6,7,21,6,t==190?0x526962:t==191?0x557E98:t==192?0xCC9145:0xE1D8BF,69,false);a.line(7,6,24,6,0xDEE3D0);}
   private static void seeds(PixelArt32 a,int t){int c=t==193?0x879C53:t==241?0xC1B17C:0x65553D;int[][] q={{5,12},{13,7},{21,10},{8,23},{20,23}};for(int i=0;i<q.length;i++){int x=q[i][0],y=q[i][1];a.poly(c,70+i,x-1,y,x,y-3,x+3,y-4,x+4,y-1,x+1,y+4);a.line(x,y,x+2,y-2,PixelArt32.shade(c,20));}}
   private static void wheat(PixelArt32 a){for(int i=0;i<4;i++){int x=8+i*5,top=4+i%2*3;a.line(12+i*2,29,x,top,0x8C773D);for(int y=top;y<19;y+=4){a.leaf(x,y+3,-4,-3,0xC4A960,i+72);a.leaf(x,y+3,4,-4,0xB39550,i+82);}}a.stroke(9,24,25,21,0x80603D,2);}
   private static void bread(PixelArt32 a){a.poly(0x8C6239,90,2,21,3,16,6,12,11,9,19,7,25,9,29,13,30,19,27,24,20,27,10,28,5,26);a.poly(0xB99455,91,4,18,7,13,12,10,20,9,25,11,28,15,27,20,22,23,15,25,8,25,4,23);a.poly(0x896236,92,8,14,10,12,14,21,11,23,10,19);a.poly(0x896236,93,15,10,17,10,21,18,19,21,18,17);a.line(9,13,12,20,0xDFC18A);a.line(16,10,19,17,0xDFC18A);a.line(22,11,25,16,0xCBB071);}
   private static void striker(PixelArt32 a){mineral(a,75);a.clear(14,0,18,32);a.poly(0x71837B,100,18,5,27,7,29,20,24,27,17,28,14,24,16,20,20,23,24,21,25,15,23,10,18,10);a.line(19,6,26,8,0xCDD5C6);a.line(25,11,27,17,0xA6B7AE);}
   private static void shears(PixelArt32 a){a.ellipse(3,20,10,10,0x78877C,101,false);a.clear(6,23,4,4);a.ellipse(20,20,10,10,0x78877C,102,false);a.clear(23,23,4,4);a.poly(PixelArt32.IRON,103,8,21,23,3,26,3,24,10,12,24);a.poly(0x93A49E,104,23,23,7,4,5,3,7,10,20,26);a.line(11,18,24,4,0xD0D9C8);a.rect(15,15,3,3,0x61756C);a.set(15,15,0xCDD5C1);}
   private static void carrot(PixelArt32 a,boolean golden){int c=golden?0xC7AE5F:0xC08744;a.poly(PixelArt32.shade(c,-19),110,3,29,8,16,12,10,22,12,24,16,16,24);a.poly(c,111,5,27,10,16,14,11,20,13,20,17,14,23);a.line(8,23,11,16,PixelArt32.shade(c,19));a.line(13,16,17,18,PixelArt32.shade(c,-14));a.leaf(18,13,0,-10,0x667E43,112);a.leaf(19,12,9,-7,0x849250,113);}
   private static void potato(PixelArt32 a,boolean baked){int c=baked?0xA18151:0xB4A075;a.poly(PixelArt32.shade(c,-18),115,3,18,5,12,11,8,19,7,26,11,29,18,26,25,19,28,10,27,5,23);a.ellipse(5,10,21,16,c,116,true);a.line(9,17,11,17,0x7C6546);a.set(20,13,0x88744F);a.line(21,21,23,20,0x88744F);if(baked){a.poly(0xD4C39A,117,11,11,18,9,22,13,21,20,16,24,12,21);a.line(13,13,19,11,0xEEE0B3);}}
   private static void pages(PixelArt32 a,boolean book){if(book){a.poly(0x715238,120,3,8,23,3,29,9,28,25,10,30,3,25);a.poly(0xA27B51,121,5,7,23,4,27,8,10,13);}a.poly(0xB8B79E,122,7,8,23,5,27,9,27,25,10,28,6,24);a.poly(0xDBD5BA,123,8,7,22,5,25,8,24,23,10,26);a.line(12,13,21,11,0x9EA189);a.line(12,17,22,15,0x9EA189);a.line(12,20,18,19,0x9EA189);if(book){a.stroke(5,9,8,26,0x8A6141,3);a.material(20,6,3,10,0xA25E46,124,3);}}
   private static void egg(PixelArt32 a){a.poly(0xBDB69A,130,8,27,5,21,6,15,10,7,14,3,19,4,24,11,27,19,25,26,19,29,12,29);a.ellipse(8,8,16,20,0xD9D1B3,131,true);a.line(11,11,12,8,0xEFE1BF);}
   private static void melon(PixelArt32 a){a.poly(0x657F45,140,2,10,29,10,28,19,24,25,17,29,9,27,4,22);a.poly(0xA8AE68,141,4,11,27,11,25,20,20,25,15,26,9,23,6,19);a.poly(0xBE7460,142,5,11,26,11,24,18,19,23,15,24,10,21,7,17);for(int[] q:new int[][]{{11,15},{18,17},{22,13},{15,20}}){a.line(q[0],q[1],q[0],q[1]+2,0x654C39);}}
   private static void pie(PixelArt32 a){a.poly(0x8B623D,150,3,17,19,4,29,17,27,25,8,29,3,25);a.poly(0xC39556,151,4,17,19,6,27,17,23,23,8,26);a.line(4,18,8,26,0xDDC18A);a.line(8,26,24,23,0xDDC18A);a.line(13,16,19,15,0xA57841);a.line(12,20,20,18,0xA57841);}
   private static void cake(PixelArt32 a){a.poly(0xA98254,160,3,13,17,6,29,13,29,24,16,30,3,23);a.poly(0xC7A87A,161,4,15,15,22,15,28,4,22);a.poly(0xDFD5BD,162,3,13,17,6,29,13,16,21);a.line(5,17,13,22,0xE5D5B1);a.line(18,22,27,17,0xDDD0AD);a.rect(11,12,2,2,0xA95542);a.rect(20,13,2,2,0xA95542);}
   private static void tear(PixelArt32 a){a.poly(0x9AB3B3,170,16,2,19,9,25,17,26,23,22,28,15,30,9,27,6,22,8,16,13,9);a.poly(0xD1DDD4,171,15,8,16,17,13,24,9,21,10,16);}
   private static void nugget(PixelArt32 a){a.poly(0xAD9146,172,5,18,10,11,19,8,25,12,28,21,22,27,13,28,6,24);a.poly(PixelArt32.GOLD,173,8,17,12,12,18,10,24,14,24,20,18,24,11,23);a.line(12,13,18,11,0xEBDD9A);a.line(20,15,22,18,0xBEA153);}
   private static void orb(PixelArt32 a,int t){int c=t==278?0x437F73:t==279?0x6E935D:0xA66C35;a.ellipse(3,3,27,27,PixelArt32.shade(c,-23),180,true);a.ellipse(5,5,22,22,c,181,true);a.poly(PixelArt32.shade(c,22),182,8,9,12,6,18,6,18,8,13,9,10,13,7,13);if(t==279){a.ellipse(10,7,13,18,0xA8B97C,183,true);a.poly(0x293F30,184,16,8,19,13,18,20,15,25,14,16);}if(t==679){a.poly(0xE2B869,185,7,15,11,10,13,16,18,8,23,11,25,17,21,15,18,21,12,18);}}
   private static void cookie(PixelArt32 a){a.poly(0xA27F4C,190,3,15,6,8,13,4,21,5,28,11,29,19,23,27,15,29,7,26,3,21);a.ellipse(6,6,22,21,0xBD9D65,191,true);for(int[] q:new int[][]{{10,10},{20,9},{13,18},{23,18},{9,24},{20,25}})a.poly(0x694B32,192,q[0]-1,q[1],q[0]+1,q[1]-1,q[0]+2,q[1]+1,q[0],q[1]+3);}
   private static void bowl(PixelArt32 a,boolean stew){a.poly(0x735434,200,3,13,29,13,26,23,20,29,12,29,6,24);a.poly(PixelArt32.OAK,201,5,15,27,15,23,23,19,26,12,26,8,22);a.ellipse(3,8,27,12,0xAD8C57,202,false);a.ellipse(6,10,21,8,stew?0x987348:0x634C30,203,false);if(stew){a.material(10,12,4,3,0xCFB289,204,2);a.material(20,12,3,2,0x7F8C51,205,2);a.rect(16,15,2,2,0xA9643F);}}
   private static void dial(PixelArt32 a,boolean clock){int c=clock?PixelArt32.GOLD:PixelArt32.IRON;if(clock){a.material(14,0,5,4,c,209,2);a.line(14,0,18,0,PixelArt32.shade(c,16));}else{a.outline(13,0,7,6,c);}a.ellipse(3,3,27,27,PixelArt32.shade(c,-21),210,false);a.ellipse(4,4,24,24,c,211,false);a.ellipse(7,7,18,18,clock?0x607F8C:0xC2C7B4,212,false);for(int i=0;i<4;i++){int x=i==1?24:i==3?6:15,y=i==0?6:i==2?24:15;a.rect(x,y,2,2,0x57645B);}a.poly(clock?0xD9C17C:0xA54D3D,213,15,17,21,8,19,17);a.poly(0x566A61,214,16,15,10,24,13,16);a.set(16,16,0xE7DFC4);}
   private static void bottle(PixelArt32 a){a.material(12,3,9,4,0xA0967C,220,3);a.poly(0x95AA96,221,11,7,22,7,22,11,26,15,26,28,23,30,8,30,6,27,6,16,11,11);a.material(8,17,16,10,0xD4CAA1,222,3);a.rect(11,20,9,4,0x7C926B);a.line(9,14,8,26,0xD3DFBD);a.line(23,16,23,27,0x819882);}
   private static void door(PixelArt32 a,int t){int species=t==88?0:t-590;a.wood(7,2,18,28,Math.min(5,species),true);a.outline(7,2,18,28,0x715536);a.clear(11,6,4,8);a.clear(18,6,3,8);a.outline(10,17,12,10,0x795F3C);a.rect(21,16,2,3,PixelArt32.IRON);}
   private static int[] decoration(int t){PixelArt32 a=new PixelArt32();int col=PixelArt32.wool((t-700)%16);
      if(t<716){a.poly(0x705134,230,2,16,18,8,30,14,29,21,14,28,2,22);a.poly(col,231,2,15,18,7,30,13,14,23);a.poly(0xD6D1BF,232,2,15,10,11,22,17,14,22);a.wood(3,21,3,8,0,true);a.wood(26,19,3,8,0,true);a.line(15,24,27,18,PixelArt32.shade(col,-22));}
      else{a.stroke(8,3,8,29,0x937345,2);a.poly(col,233,10,4,28,4,27,23,22,24,16,22,10,24);a.line(11,5,26,5,PixelArt32.shade(col,15));a.line(18,7,19,20,PixelArt32.shade(col,-12));a.wood(15,29,12,2,0,false);a.stroke(20,24,20,29,0x8C6C42,2);}
      return a.p;}
   private static void weapon(PixelArt32 a,int t){
      if(t==692){a.poly(0x594D38,241,2,27,13,16,17,20,7,31);a.line(5,26,12,19,0xA58D5F);a.poly(0x819894,242,12,16,25,2,29,2,28,11,18,22);a.poly(PixelArt32.IRON,243,13,16,27,3,26,10,17,19);a.line(14,16,27,3,0xD4DFCF);return;}
      if(t==691){a.poly(0x435442,244,8,14,11,9,22,9,27,15,26,24,21,29,12,29,7,24);a.ellipse(9,11,16,16,0x768561,245,true);for(int y=15;y<27;y+=5)a.line(9,y,24,y,0x45573E);a.material(13,4,9,6,0x879381,246,3);a.outline(19,2,9,8,PixelArt32.IRON);return;}
      if(t==693){a.material(5,9,23,20,0x6F7959,250,4);a.outline(5,9,23,20,0x3D4E3A);a.material(8,12,15,8,0x384F3E,251,2);a.rect(10,14,10,3,0xB1BA82);a.line(8,9,15,3,0x9D5742);a.line(15,3,24,7,0xAA9355);for(int x=9;x<25;x+=5)a.rect(x,24,3,3,0xBBC4A5);return;}
      if(t==694||t==695){a.poly(0x4B6457,254,6,23,21,5,28,8,14,27);a.poly(0x859885,255,9,21,22,6,25,8,12,24);a.poly(0x2C4238,256,21,5,24,3,29,7,27,10);a.stroke(14,23,6,30,0x7E7152,3);a.stroke(14,23,28,30,0x7E7152,3);if(t==694){a.ellipse(3,21,11,11,0x725F40,257,false);a.ellipse(18,21,11,11,0x725F40,258,false);a.ellipse(6,24,5,5,0xA99565,259,false);}return;}
      // Distinct receiver, stock, grip, muzzle and sights; this is the inventory icon,
      // not the already-correct first/third-person gun geometry or pose.
      a.poly(0x435952,261,5,12,9,8,21,8,23,11,30,11,30,15,21,15,18,18,7,18);
      a.material(8,9,14,4,0x85988C,262,3);a.line(10,9,20,9,0xC0CBB8);a.rect(28,11,3,4,0x2D443B);
      a.poly(0x8D7450,263,8,17,15,17,13,28,7,27,6,24);a.line(8,20,7,24,0xB0996D);
      if(t!=688){a.poly(0x88754F,264,1,12,7,12,7,20,2,21);a.material(17,17,4,8,0x596E5B,265,3);a.material(23,12,5,3,0x758B77,266,2);}
      if(t==690){a.material(13,4,14,4,0x597165,267,2);a.rect(16,8,2,3,0x3A5347);a.rect(24,3,3,5,0x334D40);}else a.rect(18,6,3,2,0x33483C);
   }
}
