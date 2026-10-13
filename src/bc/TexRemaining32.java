package bc;

/** Native 32px art for the atlas families not redrawn in 6.3.
 * No source-image recolouring, scaling, relief filter or interpolated noise.
 * Geometry, pigment palettes and one-pixel details are authored in this canvas.
 */
final class TexRemaining32 {
   private TexRemaining32() {}
   static final int N = 32;
   private static final int WOOD = 0xA98A58, IRON = 0xBBC4C4, GREEN = 0x628244;
   private static final int[] WOODS = {0xA98A58,0xCEBB83,0x765839,0xA47650,0xB66B3C,0x533B26,0xBBC4C4};
   private static final int[] TIERS = {0xA48A58,0x868C87,0xC5CDCE,0xDBC064,0x64C6C7};
   static boolean animated(int t) { return t == 23 || t == 24 || t == 235 || t == 263; }
   static boolean item(int t) {
      return t >= 65 && t <= 88 || t >= 105 && t <= 124 || t >= 135 && t <= 150
         || t >= 190 && t <= 196 || t >= 198 && t <= 202 || t >= 214 && t <= 218 || t >= 236 && t <= 245
         || t >= 272 && t <= 295 || t >= 590 && t <= 595 || t == 660
         || t >= 672 && t <= 679 || t >= 688 && t <= 695 || t >= 700 && t <= 731 || t == 742;
   }
   static boolean handles(int t) {
      if (t == 77) return false; // the approved animated BowSprite owns this cell
      if (item(t) || animated(t)) return true;
      return t == 2 || t == 9 || t == 11 || t == 19 || t == 21 || t == 22 || t >= 25 && t <= 30
         || t == 34 || t >= 36 && t <= 43 || t == 160 || t == 161 || t >= 170 && t <= 185
         || t >= 203 && t <= 213 || t >= 219 && t <= 234 || t >= 256 && t <= 269
         || t >= 400 && t <= 464 || t >= 466 && t <= 488 || t >= 524 && t <= 532
         || t >= 550 && t <= 554 || t >= 556 && t <= 567 || t >= 570 && t <= 587
         || t >= 600 && t <= 618 || t >= 620 && t <= 626 || t >= 630 && t <= 659
         || t >= 661 && t <= 671 || t >= 680 && t <= 687;
   }
   static int c(int rgb) { return 0xFF000000 | rgb; }
   static int shade(int rgb, int d) { return TexPixel32.shade(c(rgb), d); }
   static int tone(int x, int y, int seed, int step) {
      return ((int)(TexPixel32.hash(x / 3, y / 2, seed) * 5) - 2) * step;
   }
   static int wool(int i) { int[] p = Tex.WOOL[Math.floorMod(i,16)]; return p[0] << 16 | p[1] << 8 | p[2]; }
   static int edge(int x, int y) { return Math.min(Math.min(x,31-x),Math.min(y,31-y)); }
   static int brick(int base, int x, int y, int height) {
      int xx = (x + y / height % 2 * 8) % 16, yy = y % height;
      return shade(base, xx == 0 || yy == 0 ? -31 : xx == 1 || yy == 1 ? 11 : tone(x,y,base,4));
   }
   static int[] paint(int t) {
      if (animated(t)) return frame(t,0);
      if (item(t)) return icon(t);
      if (t == 2 || t == 9 || t >= 550 && t <= 552 || t == 642) return foliage(t);
      if (t >= 25 && t <= 27 || t >= 171 && t <= 178 || t >= 204 && t <= 211 || t == 219
         || t >= 227 && t <= 230 || t == 553 || t == 554 || t >= 556 && t <= 583) return plant(t);
      Canvas p = new Canvas();
      for (int y=0;y<N;y++) for (int x=0;x<N;x++) p.p[y*N+x] = material(t,x,y);
      // Distinct functional faces, rather than a generic noise treatment.
      switch(t) {
         case 36: case 646: case 662:
            p.clear(); p.rect(14,12,4,20,0x88643D); p.rect(14,12,1,20,0xB49460);
            p.rect(12,7,8,9,t==662?0x6C4337:t==646?0xAA3024:0xD98631);
            p.rect(14,8,4,5,t==662?0x8C5548:t==646?0xF17950:0xFFE0A1); break;
         case 37:
            p.rect(3,3,26,26,0x665034); p.rect(4,4,24,24,0xBB9A69);
            for(int a=0;a<4;a++) { p.rect(4+a*8,4,1,24,0x6C5136); p.rect(4,4+a*8,24,1,0x6C5136); } break;
         case 38:
            p.rect(3,3,26,3,0xC0A173); p.rect(3,25,26,4,0x705036);
            p.line(8,9,8,21,0x64503B,3); p.rect(5,8,7,3,IRON);
            p.line(20,8,20,22,0x705039,3); p.rect(18,7,6,7,IRON); p.rect(22,10,4,4,0x778581); break;
         case 39: case 40:
            p.rect(3,4,26,8,0x454D4D); p.rect(5,6,22,3,0x262C2D);
            p.rect(3,17,26,12,0x424746); p.rect(5,19,22,8,0x242B2C);
            if(t==40) for(int a=0;a<6;a++) { p.rect(5+a*4,22-a%3,3,5+a%3,0xCB6430); p.rect(6+a*4,25,2,2,0xF5CA69); } break;
         case 41: case 42:
            p.border(0,0,32,32,0x654C32); p.border(2,2,28,28,0xC1A06A);
            if(t==42) { p.rect(0,9,32,2,0x674C32); p.rect(13,7,6,10,0x555B58); p.rect(14,8,4,7,0xC8C9B5); } break;
         case 43: case 600:
            p.clear();
            for(int x=2;x<32;x+=8) { p.rect(x,0,2,32,0x667674); p.rect(x,0,1,32,0xA2ACA4); }
            p.rect(0,5,32,2,0x7D8C83); p.rect(0,25,32,2,0x606F69);
            if(t==43) { p.border(0,0,32,32,0x3E4C48); p.rect(14,14,4,4,0xD99347); } break;
         case 160: case 161:
            p.border(0,0,32,32,0x705139); p.border(2,2,28,28,0xBA996B);
            if(t==161) { p.rect(6,5,8,14,-1); p.rect(18,5,8,14,-1); } // explicit clear windows below
            else { p.border(5,6,10,20,0x745339); p.border(18,6,9,20,0x745339); }
            p.rect(26,22,3,4,0xC3BB85);
            if(t==161) { p.erase(6,5,8,14); p.erase(18,5,8,14); } break;
         case 179:
            p.clear(); p.rect(5,0,3,32,0x84673F); p.rect(24,0,3,32,0x84673F);
            for(int y=3;y<32;y+=8) {p.rect(5,y,22,3,0xAA8B58);p.rect(5,y,22,1,0xCEAD78);} break;
         case 180:
            p.rect(0,11,32,11,0xE4DCC5); p.textTNT(5,13,0x4E4032); break;
         case 181: p.rect(13,12,6,7,0xC5B28E);p.line(16,12,20,6,0x514635,2);break;
         case 220:
            p.rect(1,3,30,11,0x584833); p.rect(1,18,30,11,0x584833);
            int[] books={0x9A5951,0x6C8566,0xC4A16E,0x657D91,0xB38154,0x847297};
            for(int r=0;r<2;r++) for(int i=0;i<6;i++) { int x=2+i*5,y=4+r*15+(i%3);p.rect(x,y,4,10-i%3,books[(i+r)%6]);p.rect(x,y+2,3,1,0xD3BC8A); } break;
         case 223: case 224:
            int glow=t==224?0xF8D58A:0x65462C;
            p.rect(5,9,7,6,glow);p.rect(20,9,7,6,glow);p.rect(9,21,14,4,glow);p.rect(13,18,6,5,glow);break;
         case 265: case 266:
            p.border(0,0,32,32,0xD5C89B);p.border(4,4,24,24,0x576E50);p.rect(8,8,16,16,0x253F38);
            if(t==266){p.ellipse(10,11,12,10,0x7FAD7B);p.rect(15,12,3,8,0x233D36);}break;
         case 644: case 645:
            p.border(0,0,32,32,0x6B5440);p.border(3,3,26,26,0xB89A67);
            p.line(3,3,28,28,0x776143,2);p.line(28,3,3,28,0x776143,2);break;
         case 663: case 664:
            p.rect(0,0,32,3,0x817D70);p.rect(0,29,32,3,0x817D70);
            p.rect(14,3,4,26,t==664?0xC85E41:0x864736);p.rect(6,14,20,3,0x6B6558);
            p.rect(6,14,2,3,0xD9CFB4);p.rect(24,14,2,3,0xD9CFB4);break;
         case 665:
            p.rect(0,0,32,7,0x7D817A);p.rect(0,25,32,7,0x626D68);p.rect(7,7,18,18,0xBDAD87);
            p.rect(13,7,6,18,0x8E7754);break;
         case 666: case 667:
            p.border(0,0,32,32,0x7C8580);p.border(2,2,28,28,0xC1C3AC);
            for(int x=4;x<32;x+=8)p.rect(x,4,1,24,0x7A6545);
            if(t==667)for(int y=4;y<29;y+=4) for(int x=4;x<29;x+=4)if(TexPixel32.hash(x,y,19)>.25)p.rect(x,y,4,3,0x88A85C);break;
         case 668: p.border(2,2,28,28,0x586560);p.rect(8,8,16,16,0x899189);p.border(11,11,10,10,0x525E59);break;
         case 669:
            p.border(0,0,32,32,0x957C51);
            for(int y=3;y<30;y+=9)for(int x=3;x<30;x+=9){p.rect(x,y,8,8,0x737977);p.rect(x,y,8,2,0xB4BDB1);}break;
         case 670:
            p.border(1,1,30,30,0x66503A);for(int y=5;y<28;y+=4)for(int x=5;x<28;x+=4)p.rect(x,y,2,2,0x5A4430);break;
         case 671:
            p.rect(4,7,8,7,0x424E49);p.rect(20,7,8,7,0x424E49);p.rect(8,22,16,3,0x59685F);break;
      }
      if(t>=620&&t<=626) {
         p.border(0,0,32,32,0x77634B);p.border(2,2,28,28,0xB1A580);
         for(int y=8;y<26;y+=10)for(int x=6;x<28;x+=12)p.erase(x,y,8,4);
         p.rect(2,7,3,5,0x707B72);p.rect(2,22,3,5,0x707B72);
      }
      if(t==647||t==661) {p.clear(); int col=t==661?0xDE684C:0x823D30;p.rect(14,0,4,32,col);p.rect(0,14,32,4,col);p.rect(15,0,1,32,t==661?0xF89560:0xA05138);}
      if(t>=601&&t<=618) {p.clear();int col=t==601?0xCBDBDB:t==618?0x8D9D96:wool(t-602);p.rect(14,0,4,32,col);p.rect(0,14,32,4,col);p.rect(14,0,1,32,shade(col,15)&0xFFFFFF);p.rect(0,14,32,1,shade(col,15)&0xFFFFFF);}
      return p.p;
   }

   private static int material(int t,int x,int y) {
      int g=tone(x,y,t,5), e=edge(x,y);
      if(t>=400&&t<=448) {
         int base=t<416?Color312.CONCRETE[t-400]:t<432?Color312.CONCRETE[t-416]:t==432?Color312.PLAIN_TERRA:Color312.TERRA[t-433];
         int d=t<416?tone(x,y,t,2):t<432?tone(x,y,t,7):tone(x,y,t,3);
         if(t>=416&&t<432 && TexPixel32.hash(x,y,t)>.92)d+=10;
         return shade(base,d);
      }
      if(t>=449&&t<=464) {
         int i=t-449, xx=x, yy=y; for(int a=0;a<(i%4);a++){int z=xx;xx=31-yy;yy=z;}
         int d=Math.abs(xx-15)+Math.abs(yy-15), motif=i/4;
         boolean ink=motif==0?d%16<4:motif==1?(xx+yy/2)%16<4:motif==2?(xx/4+yy/4)%4==0:Math.max(Math.abs(xx-15),Math.abs(yy-15))%10<3;
         return c(e==0?Color312.TERRA[i]:ink?wool(i):(d%8<2?0xB8A885:0xDBD1B1));
      }
      if(t==19||t>=466&&t<=481) {
         int base=t==19?0xBED5D9:wool(t-466); boolean rim=e==0,shine=(x+y==12||x+y==17||x+y==42)&&e>3;
         int alpha=rim?210:shine?100:t==19?0:48;
         return alpha==0?0:alpha<<24 | (shade(base,shine?20:0)&0xFFFFFF);
      }
      if(t>=482&&t<=487||t==643) {
         int[] colors={0xC5CCCA,0xE0C568,0x6AC6C3,0x56AD7F,0x353B3B,0x426BA3};
         int base=t==643?0xB74B3C:colors[t-482];
         int d=e==0?-25:e==1?16:t==486?g:(x+y<25?7:x+y>42?-8:0);
         if(t==487&&TexPixel32.hash(x/2,y/2,70)>.96)return c(0xAC9A68);
         return shade(base,d);
      }
      if(t==488||t==258) {
         int b=t==488?0x80827D:0x804840;int cx=x%8-4,cy=y%8-4;
         if(Math.abs(cx)+Math.abs(cy)<3&&TexPixel32.hash(x/8,y/8,t)>.4)return shade(t==488?0x58BA87:0xDAD2BC,cx+cy<0?18:-14);
         return shade(b,g);
      }
      if(t>=630&&t<=637) {
         int a=new int[]{2,0,1,3}[(t-(t<634?630:634))%4],xx=x,yy=y;
         for(int i=0;i<a;i++){int z=xx;xx=31-yy;yy=z;}
         return shade(0xDADADA,((xx+yy)&1)==0?5:yy==12&&t<634?-18:-3);
      }
      if(t==183||t==184||t==185||t>=638&&t<=641) {
         if((t==185||t==638)&&(y<14||y>19))return 0; // blanket side UV occupies these rows
         if(t==183&&y<10||t==639||t==640)return shade(0xE9E9E9,e<2?-12:tone(x,y,41,2));
         return shade(t>=638?0xDCDCDC:0xAA5550,((x+y)&1)==0?4:-4);
      }
      if(t>=620&&t<=626||t==160||t==161||t==37||t==38||t==41||t==42||t==220||t==666||t==667||t==670) {
         int base=t>=620&&t<=626?WOODS[t-620]:WOOD;
         return shade(base,y%8==0?-18:tone(x,y,76,4));
      }
      if(t==524||t==526||t==528||t==525||t==527||t==587) {
         int b=t==524?0x786047:t==526?0x94838E:0x896447;
         if(t==525||t==527||t==587){int cap=5+(int)(TexPixel32.hash(x/3,0,t)*4);if(y<cap)b=t==525?0x786047:t==527?0x94838E:0xEAF0F4;}
         if(t==524&&(x+2*y)%13<2)return shade(0xA27B4D,g);
         if(t==528&&TexPixel32.hash(x/3,y/2,t)>.8)b=0x97897A;
         return shade(b,g);
      }
      if(t==529||t==530) {
         int base=t==529?0x98B9C8:0x9BB2C6;
         int d=(x+y)%23<2?21:(x+y)%23==2?8:tone(x/2,y/2,t,3);
         return (t==529?0xB8000000:0xFF000000)|(shade(base,d)&0xFFFFFF);
      }
      if(t==21||t==22||t==531||t==532||t>=651&&t<=654) {
         boolean red=t==531||t==532||t==653||t==654;int b=red?0xB67B4F:0xD3C59C;
         if(t==651||t==653){int d=Math.max(Math.abs(x-15),Math.abs(y-15));return shade(b,d==10||d==5?-27:g/2);}
         boolean side=t==21||t==532;
         return shade(b,side&&(y==7||y==23)?-20:side&&(y==8||y==24)?10:g/2);
      }
      if(t>=648&&t<=650) {int[] b={0x9D786B,0xBABBB5,0x898B86};return shade(b[t-648],e==0?-18:e==1?12:g/2);}
      if(t>=655&&t<=657||t==259||t==681||t==682||t==683) {
         int base=t==259?0x67444A:t==681?0x78A79C:t==682?0x4A756B:t==683?0xAE90A5:0x898D85;
         if(t==655){int d=Math.max(Math.abs(x-15),Math.abs(y-15));return shade(base,d==11||d==6?-32:g);}
         if(t==656&&TexPixel32.hash(x/3,y/3,t)>.64)base=0x687B41;
         if(t==657&&(x+y/3)%19==0)return shade(base,-38);
         return brick(base,x,y,t==259?6:8);
      }
      if(t==170||t==203)return shade(t==203?0x65503B:0x896447,(x%8<2?-21:x%8==2?10:0)+g/2);
      if(t==28)return shade(0x648249,x%8<2?-19:x%8==2?17:(x%8==5&&y%9==3?35:g/2));
      if(t==29)return shade(0x89A064,e<3?-27:(x==15||y==15)?-10:g/2);
      if(t==11)return shade(0x606662,tone(x/2,y,t,13)+(y%7==0?-15:0));
      if(t==34||t==269)return shade(t==34?0x353747:0x383544,(x+y/2)%9==0?8:g/2);
      if(t==30||t==260) return cellular(t,x,y);
      if(t==256)return shade(0x874F48,tone(x,y,t,8)+(y%7==0?-12:0));
      if(t==257) {int xx=x%16,yy=y%16;boolean pit=(xx==4||xx==10)&&yy>=5&&yy<8||yy==11&&xx>4&&xx<11;return shade(0x79614E,pit?-33:g);}
      if(t==261||t==262||t==686||t==687) {int b=t<263?0xDCD8C7:0xD6D0B6;return shade(b,e==0?-15:t==261||t==687?x%8==0?-12:g/2:g/2);}
      if(t==264)return shade(0xCECA96,TexPixel32.hash(x/3,y/2,t)>.87?-24:g);
      if(t==265||t==266||t==267)return brick(0x819472,x,y,8);
      if(t==268)return TexPixel32.hash(x,y,42)>.98?c(0x6B948D):c(0x152A2D);
      if(t==584)return (x%12>3&&x%12<8&&y%13>4&&y%13<9)?shade(0xDDD4C0,g):shade(0xB35045,g);
      if(t==585)return shade(0x9A826A,g);
      if(t==586)return shade(0xCFC7B4,x%7==0?-15:g/2);
      if(t==212||t==213)return shade(0xBB9E58,t==212&&(y==7||y==8||y==23||y==24)?-41:(x%3==0?-12:8));
      if(t>=221&&t<=226){boolean melon=t>=225;int d=x%8==0?-21:x%8==1?10:g/2;
         if(t==221||t==225){int r=Math.max(Math.abs(x-15),Math.abs(y-15));if(r<3)return c(0x687348);d=r%6==0?-17:g/2;}
         return shade(melon?0x7C9950:0xBB8548,d);}
      if(t>=180&&t<=182)return shade(0xB86045,x%8==0?-25:x%8==1?12:g/2);
      if(t>=231&&t<=234){if(t==231)return shade(0xE3D6BF,(x%11<2&&y%9<3)?-65:g/2);if(t==233)return shade(0xAA8054,g);return y<10?shade(0xE3D6BF,g/2):shade(0xB18D62,g);}
      if(t==644||t==645)return shade(t==644?0x83714F:0xE5CF91,g);
      if(t==658)return (0xA8000000|(shade(0x86B46A,e<3?-17:g/2)&0xFFFFFF));
      if(t==659)return shade(e<3?0x75A99E:0xD6E6CD,e==3?12:g/2);
      if(t==680)return shade(0x80A89B,tone(x,y,t,9));
      if(t==684||t==685)return shade(0xB19AAC,t==685?(x%8==0?-18:x%8==1?-8:x%8==2?11:0):e%6==0?-16:g/2);
      // Machine housings have a quiet stone/iron grain behind their authored face details.
      return shade(0x91978D,g/2);
   }

   private static int cellular(int t,int x,int y) {
      float px=x/8.0F,py=y/8.0F,first=100,second=100,variation=0;
      for(int yy=(int)py-1;yy<=(int)py+1;yy++)for(int xx=(int)px-1;xx<=(int)px+1;xx++){
         int wx=Math.floorMod(xx,4),wy=Math.floorMod(yy,4);
         float dx=px-xx-.2F-TexPixel32.hash(wx,wy,30)*.6F;
         float dy=py-yy-.2F-TexPixel32.hash(wx,wy,31)*.6F;
         float d=dx*dx+dy*dy;
         if(d<first){second=first;first=d;variation=TexPixel32.hash(wx,wy,32);}else if(d<second)second=d;
      }
      boolean seam=second-first<.17F;
      int base=t==30?(seam?0x9B744A:0xDBC486):(seam?0xE29145:0x634A40);
      return shade(base,((int)(variation*4)-2)*6+tone(x,y,t,3));
   }

   private static int[] foliage(int t) {
      Canvas p=new Canvas();
      if(t==2) {for(int y=0;y<12;y++)for(int x=0;x<32;x++)if(y<5+(int)(TexPixel32.hash(x/3,0,91)*6))p.p[y*32+x]=shade(0xA6A6A6,tone(x,y,901,7));return p.p;}
      // Biome tint is applied by the mesher. Keep foliage neutral, never bake green twice.
      for(int y=0;y<32;y++)for(int x=0;x<32;x++) {
         int xx=(x+y/8*3)%8, yy=y%8, d=Math.abs(xx-3)+Math.abs(yy-3);
         if(d<=5 || t!=642&&TexPixel32.hash(x/3,y/3,t)>.35)
            p.p[y*32+x]=shade(0xB0B0B0,d==0?15:d==1?7:xx>3?-20:tone(x,y,t,6));
      }
      return p.p;
   }

   private static int[] plant(int t) {
      Canvas p=new Canvas();
      if(t>=570&&t<=580&&(t&1)==0) {
         int col=t<=572?0xA9A9A9:0x67864A;
         p.line(16,31,16,0,col,2);
         for(int y=7;y<31;y+=8){p.line(16,y,5,y-6,col,2);p.line(16,y+2,27,y-4,col,2);}
         return p.p;
      }
      if(t==219){for(int i=0;i<3;i++){int x=5+i*10;p.rect(x,0,3,32,0x75964E);p.rect(x,0,1,32,0xB0BA6C);for(int y=5;y<32;y+=9){p.rect(x,y,3,1,0x526F3B);p.line(x+1,y,x+6,y-5,0x8EA953,1);}}return p.p;}
      if(t==582){p.ellipse(2,6,28,21,0x59794B);p.ellipse(5,7,21,15,0x769451);p.line(16,16,30,28,0x385B39,3);p.erase(22,22,10,10);return p.p;}
      if(t==583){for(int x=4;x<32;x+=10){p.line(x,0,x-2,31,0x969696,1);for(int y=2;y<32;y+=7){p.ellipse(x-3,y,7,5,0xB4B4B4);p.line(x-2,y+3,x+3,y+1,0x858585,1);}}return p.p;}
      if(t==566||t==567){p.rect(14,16,5,15,0xC5B49A);p.ellipse(5,8,23,15,t==566?0x9C8062:0xB35244);p.rect(5,19,23,3,0xD4BA91);if(t==567){p.rect(9,12,3,3,0xE5D6B7);p.rect(20,15,4,3,0xE5D6B7);}return p.p;}
      if(t>=171&&t<=178||t>=204&&t<=211||t>=227&&t<=230) {
         boolean wheat=t<179,stem=t>=227;
         int stage=wheat?t-171:stem?t-227:(t-204)%4, h=wheat?5+stage*3:5+stage*6;
         for(int i=0;i<(stem?1:5);i++) {int x=stem?16:3+i*6,y=31-h+(i%2)*2;int col=wheat&&stage>=5?0xB6A25B:0x78924B;
            p.line(x,31,x,y,col,1);p.line(x,y+6,x-3,y+3,col,1);p.line(x,y+9,x+3,y+5,col,1);
            if(wheat&&stage>3)for(int k=0;k<4;k++)p.rect(x-(k%2)*2,y+k*2,3,2,k%2==0?0xC9B570:0xA69550);
            if(!wheat&&!stem&&stage==3)p.ellipse(x-2,27,5,4,t<208?0xCB873F:0xB9A066);
         }return p.p;
      }
      if(t==554){p.line(16,31,15,8,0x967852,2);p.line(16,24,5,14,0x967852,2);p.line(15,19,24,7,0xAA8656,2);return p.p;}
      boolean grass=t==25||t==553||t>=570&&t<=573||t==574;
      if(grass){for(int i=0;i<7;i++){int x=4+i*4,h=9+(int)(TexPixel32.hash(i,0,t)*19);p.line(16,31,x,31-h,0xA9A9A9,1);p.line(16,30,x+1,32-h,0xC0C0C0,1);if(t==553||t==572||t==573)for(int y=31-h;y<28;y+=4)p.line(x,y,x+(x<16?-4:4),y-2,0x9A9A9A,1);}return p.p;}
      int[] petal={0xB5D8E1,0xAD8DB0,0xDEDCC5,0xBF6656,0xCF9353,0xE8E2CD,0xD1A0AD,0xE4E1CE,0x739ABD,0xE9E5D1};
      int col=t==26?0xC2594C:t==27?0xD6BE64:t>=556&&t<=565?petal[t-556]:t==575?0xDAC36B:t<578?0xB397B4:t<580?0xB8544B:0xD4A3AE;
      p.line(16,31,15,13,0x67864A,2);p.line(15,25,7,20,0x83A15B,2);p.line(16,22,23,18,0x83A15B,2);
      if(t>=576){for(int i=0;i<3;i++)p.flower(10+i*6,7+(i%2)*8,col);}
      else {p.flower(15,t==575?10:11,col);if(t>=559&&t<=562)p.rect(11,7,9,7,col);}
      return p.p;
   }

   /** Animated native texels: time changes the bands, never the spatial resolution. */
   static int[] frame(int t,float phase) {
      Canvas p=new Canvas(); double a=(phase%16)*Math.PI/8;
      int[] flame={0xF8D779,0xE8AD4B,0xD97B31,0xB7522B};
      int[] palette=t==23?new int[]{0x345F91,0x3D709C,0x487DA4,0x558CAB,0x6C9EB7}:t==24?new int[]{0xA64D2D,0xBE6633,0xD58139,0xE59D48,0xF0BF62}:new int[]{0x58447F,0x68508D,0x795E9E,0x8A72AE,0xA18BBE};
      for(int y=0;y<32;y++)for(int x=0;x<32;x++){
         int c0;
         if(t==235){double height=17+7*Math.sin(x*.55+a)+4*Math.sin(x*.22-a*2);if(31-y>height)continue;
            int band=(int)((31-y)/Math.max(1,height)*4);c0=c(flame[Math.min(3,band)]);
         }else{double v=Math.sin(x*Math.PI/8+a)+Math.sin(y*Math.PI/8-a)+Math.sin((x+y)*Math.PI/16+a);
            int band=Math.max(0,Math.min(4,(int)((v+3)*5/6)));

            c0=(t==23?0xB0000000:t==263?0xD0000000:0xFF000000)|palette[band];
         }p.p[y*32+x]=c0;
      }return p.p;
   }

   private static int[] icon(int t) {
      Canvas p=new Canvas();
      if(t>=105&&t<=124||t>=198&&t<=202) {
         int tier=t>=198?t-198:(t-105)/4, kind=t>=198?4:(t-105)%4, metal=TIERS[tier];
         p.line(7,26,21,12,0x72543B,4);p.line(7,25,21,11,0xBC965D,1);
         if(kind==0) { // sword: a restrained two-facet blade and a brass/wood grip
            p.poly(new int[]{13,16,25,3,29,2,28,7,19,19},shade(metal,-32)&0xFFFFFF);
            p.poly(new int[]{14,16,25,4,28,3,18,18},metal);
            p.line(15,15,26,4,shade(metal,27)&0xFFFFFF,1);
            p.line(10,14,20,24,0x8B7850,3);p.line(10,14,20,24,0xC4AF73,1);
         } else if(kind==1) {
            p.poly(new int[]{5,7,13,3,22,5,28,12,29,18,25,15,20,10,12,8,5,10},metal);
            p.line(7,6,13,4,shade(metal,24)&0xFFFFFF,1);p.line(14,4,22,6,shade(metal,24)&0xFFFFFF,1);
         } else if(kind==2) {
            p.poly(new int[]{10,5,17,3,24,10,16,18,9,16,7,12},metal);
            p.line(9,8,10,14,shade(metal,27)&0xFFFFFF,2);p.line(12,5,17,4,shade(metal,27)&0xFFFFFF,1);
         } else if(kind==3) {
            p.poly(new int[]{20,3,27,4,29,11,21,18,15,12},metal);
            p.line(20,5,26,5,shade(metal,27)&0xFFFFFF,1);p.line(19,7,16,11,shade(metal,27)&0xFFFFFF,1);
         } else {p.line(8,7,23,7,metal,4);p.line(23,7,26,12,metal,3);p.line(9,5,23,5,shade(metal,24)&0xFFFFFF,1);}
         return p.p;
      }
      if(t>=135&&t<=150) {
         int mat=(t-135)/4,slot=(t-135)%4,col=mat==0?0xA47751:TIERS[mat+1];
         if(slot==0){p.poly(new int[]{5,22,5,10,10,5,22,5,27,10,27,22,22,22,22,15,10,15,10,22},col);p.line(9,8,22,8,shade(col,22)&0xFFFFFF,2);}
         if(slot==1){p.poly(new int[]{3,6,11,4,13,9,19,9,21,4,29,6,29,15,24,16,24,28,8,28,8,16,3,15},col);p.line(11,12,11,25,shade(col,22)&0xFFFFFF,2);p.line(21,12,21,25,shade(col,-20)&0xFFFFFF,2);}
         if(slot==2){p.poly(new int[]{7,4,25,4,25,28,19,28,18,15,14,15,13,28,7,28},col);p.rect(8,5,16,3,shade(col,-20)&0xFFFFFF);p.line(9,10,9,25,shade(col,22)&0xFFFFFF,1);}
         if(slot==3){p.rect(7,5,7,20,col);p.rect(19,5,7,20,col);p.rect(3,22,11,6,col);p.rect(19,22,11,6,col);p.rect(4,22,8,2,shade(col,22)&0xFFFFFF);p.rect(20,22,9,2,shade(col,22)&0xFFFFFF);}
         return p.p;
      }
      if(t>=281&&t<=295||t==74||t==217||t==236||t==275||t==660){int col=t>=281&&t<=295?wool(t-280):t==74?0x747A74:t==275?0xDDA54E:t==660?0xB24C3E:0xDCDACA;
         p.poly(new int[]{4,26,8,19,15,15,20,18,27,25,22,28,10,29},col);for(int i=0;i<12;i++){int x=5+(int)(TexPixel32.hash(i,1,t)*21),y=15+(int)(TexPixel32.hash(i,2,t)*12);p.rect(x,y,2,2,shade(col,i%2==0?19:-22)&0xFFFFFF);}return p.p;}
      if(t==88||t>=590&&t<=595){int col=t==88?WOOD:WOODS[t-590];p.rect(7,3,18,27,col);p.border(7,3,18,27,0x796044);p.erase(11,7,4,7);p.erase(18,7,3,7);p.border(11,18,10,8,0x796044);p.rect(21,17,2,3,0xD1C49A);return p.p;}
      if(t>=700&&t<=731){int col=wool((t-700)%16);
         if(t<716){p.poly(new int[]{3,15,18,8,29,14,29,24,13,30,3,24},0x947048);p.poly(new int[]{3,14,18,7,29,13,14,21},col);p.poly(new int[]{3,14,10,11,20,16,14,20},0xDED9C6);p.rect(4,23,3,7,0x806443);p.rect(26,23,3,6,0x806443);}
         else{p.rect(7,4,21,19,col);p.rect(7,4,2,25,WOOD);p.rect(9,5,17,1,shade(col,18)&0xFFFFFF);p.rect(14,29,13,2,WOOD);p.rect(19,23,2,7,WOOD);}return p.p;}
      if(t>=688&&t<=695){weaponIcon(p,t);return p.p;}
      switch(t) {
         case 65: case 274:
            p.line(6,27,25,5,t==65?0x77553A:0xB07C31,4);p.line(6,26,25,4,t==65?0xBD9861:0xEDD07D,1);break;
         case 66: case 75: case 272:
            int shard=t==66?0x4B5350:t==75?0x6F7E81:0xDAD6C2;
            p.poly(new int[]{5,23,10,12,23,4,27,12,24,24,13,29},shard);
            p.poly(new int[]{10,12,23,5,18,21,8,24},shade(shard,24)&0xFFFFFF);p.line(19,14,16,25,shade(shard,-20)&0xFFFFFF,1);break;
         case 67: case 68: case 273:
            int metal=t==67?0xBCC8C7:t==68?0xD9B957:0x735259;
            p.poly(new int[]{4,19,9,10,24,8,29,17,24,23,8,25},shade(metal,-24)&0xFFFFFF);
            p.poly(new int[]{5,18,10,11,23,9,27,16,22,20,8,22},metal);p.line(11,11,22,10,shade(metal,30)&0xFFFFFF,2);break;
         case 69: case 280:
            int gem=t==69?0x71C6CD:0x5FB88B;
            p.poly(new int[]{5,12,11,5,22,5,28,12,18,28,14,28},shade(gem,-22)&0xFFFFFF);
            p.poly(new int[]{6,12,12,6,21,6,27,12,17,23},gem);p.poly(new int[]{12,6,17,6,12,13,7,12},shade(gem,35)&0xFFFFFF);p.line(12,14,16,25,shade(gem,19)&0xFFFFFF,1);break;
         case 70:
            p.poly(new int[]{4,7,10,3,16,8,23,3,29,8,24,14,27,24,20,29,15,25,8,29,4,23,8,14},0xAD8056);
            p.line(11,10,10,22,0xC49A68,2);p.line(22,12,23,21,0x8B6243,2);break;
         case 71:
            p.poly(new int[]{5,28,8,14,23,4,28,4,25,17,16,25},0xDDD9C7);p.line(5,28,26,5,0x989F91,1);p.line(10,19,14,11,0xECE8D6,2);break;
         case 72:
            p.line(8,27,5,19,0xD5D0BB,2);p.line(5,19,16,7,0xD5D0BB,2);p.line(16,7,25,9,0xD5D0BB,2);p.line(25,9,27,16,0xD5D0BB,2);p.line(27,16,12,25,0xD5D0BB,2);p.line(12,25,8,18,0xF0EBD5,1);break;
         case 73:
            p.line(8,25,24,8,0xC5C3AF,5);p.line(8,24,23,8,0xE6E1CC,2);p.ellipse(4,22,9,8,0xE6E1CC);p.ellipse(21,3,8,9,0xE6E1CC);break;
         case 76:
            p.line(6,27,26,6,0x967E58,2);p.poly(new int[]{19,6,29,3,26,14},0xBAC6BE);p.line(4,21,10,27,0xD9D7C2,3);break;
         case 78: case 672:
            int apple=t==78?0xBE6150:0xD4B862;
            p.ellipse(5,9,13,19,apple);p.ellipse(14,9,13,19,apple);p.rect(11,23,10,6,apple);p.rect(14,5,3,8,0x775D3D);p.ellipse(18,5,7,4,GREEN);p.ellipse(8,12,5,7,shade(apple,24)&0xFFFFFF);break;
         case 79: case 80: case 81: case 82: case 83: case 84: case 85: case 86: case 87:
            int[] meat={0xCE9B89,0xB89260,0xB96C61,0x987454,0xD5B7A1,0xBA965E,0xBE7B6B,0x9F7F5C,0x9B8561};
            int col=meat[t-79];p.poly(new int[]{4,16,8,8,17,5,27,11,28,22,19,28,9,26},col);p.line(8,18,15,12,shade(col,22)&0xFFFFFF,3);p.line(19,23,25,17,shade(col,-22)&0xFFFFFF,2);
            if(t==83||t==84){p.line(6,24,3,29,0xE1D7BC,3);}break;
         case 190: case 191: case 192: case 239:
            p.poly(new int[]{4,9,28,9,25,26,20,29,11,29,7,25},0x7C8F8C);p.poly(new int[]{7,11,25,11,22,25,12,26},0xB6C5BC);p.line(8,13,11,24,0xDFE0CE,2);
            p.ellipse(4,5,25,9,0xD4D8C8);p.ellipse(7,7,19,5,t==190?0x667D76:t==191?0x568DA7:t==192?0xDC9A4A:0xE9E1CC);break;
         case 193: case 241: case 242:
            int seed=t==193?0x8BA965:t==241?0xC5BA87:0x665B48;for(int i=0;i<6;i++)p.ellipse(5+(i%3)*8,7+(i/3)*13+(i%2)*3,5,8,seed);break;
         case 194:
            for(int i=0;i<4;i++){p.line(10+i*3,29,8+i*5,5+i%2*3,0xAC9450,1);for(int y=7;y<19;y+=3)p.rect(7+i*5,y+i%2*3,4,2,0xD1B872);}p.line(8,24,24,24,0x816239,2);break;
         case 244:
            p.poly(new int[]{3,17,20,4,29,19,25,28,7,28},0xB6925E);
            p.poly(new int[]{4,17,20,6,27,19,24,23,8,23},0xC99455);
            p.line(4,18,9,23,0xDFC28B,2);p.line(9,24,24,24,0xDFC28B,2);
            p.line(13,17,21,17,0xA97743,1);break;
         case 195: case 215: case 216:
            int bread=t==195?0xC3A16A:t==215?0xB4A070:t==216?0xAF8B5C:0xBB9965;
            p.ellipse(3,9,27,19,bread);p.ellipse(6,9,21,12,shade(bread,15)&0xFFFFFF);
            if(t==195||t==244){for(int x=10;x<27;x+=7)p.line(x,10,x-3,18,0xE0C694,2);}else{p.rect(10,15,2,2,0x796641);p.rect(22,18,2,2,0x796641);}break;
         case 196:
            p.poly(new int[]{5,23,7,14,13,13,17,21,14,27,8,28},0x778A86);
            p.poly(new int[]{19,5,28,7,28,22,21,27,16,24,17,20,22,21,24,17,23,10,19,9},0xBFCBC0);
            p.line(20,6,27,8,0xE1DFCA,1);break;
         case 218:
            p.ellipse(3,18,11,11,0x667874);p.erase(6,21,5,5);p.ellipse(19,18,11,11,0x667874);p.erase(22,21,5,5);p.line(10,22,24,4,IRON,3);p.line(23,23,9,4,IRON,3);p.rect(15,15,3,3,0xE1DCC3);break;
         case 214: case 673:
            p.poly(new int[]{4,28,11,9,23,12,20,20},t==214?0xC78D49:0xD2BA64);p.line(7,23,13,12,t==214?0xDFAD69:0xE5CF87,2);p.line(19,12,17,3,GREEN,2);p.line(19,12,28,5,GREEN,2);break;
         case 237: case 238:
            p.poly(new int[]{6,5,25,7,28,27,8,29,3,24},t==238?0x956D50:0xC6C9B5);p.poly(new int[]{7,5,24,7,26,24,8,26},0xE3DFCA);p.line(11,12,21,13,0x9FA38F,1);p.line(11,16,23,17,0x9FA38F,1);if(t==238){p.rect(5,7,3,18,0x8D5D48);p.rect(22,8,3,12,0xAF6E53);}break;
         case 240: case 276: case 277:
            int egg=t==240?0xDAD2B6:t==276?0xB4CDD0:0xD0B460;p.ellipse(8,7,17,23,egg);p.ellipse(12,3,9,12,egg);p.ellipse(10,9,5,10,shade(egg,21)&0xFFFFFF);break;
         case 243:
            p.poly(new int[]{3,10,28,10,25,23,16,28,6,23},0x7F9E64);p.poly(new int[]{5,10,26,10,23,21,16,25,8,21},0xC17A65);p.rect(11,15,2,3,0x665144);p.rect(19,17,2,3,0x665144);break;
         case 245:
            p.poly(new int[]{4,14,16,7,28,14,28,24,16,30,4,23},0xB59B72);p.poly(new int[]{4,14,16,7,28,14,16,21},0xE6DAC0);p.rect(12,12,3,2,0xB36C57);p.rect(21,14,3,2,0xB36C57);break;
         case 278: case 279: case 679:
            int orb=t==278?0x549C92:t==279?0x83AA70:0xC58C4A;p.ellipse(4,4,25,25,shade(orb,-24)&0xFFFFFF);p.ellipse(5,5,21,21,orb);p.ellipse(8,7,8,5,shade(orb,34)&0xFFFFFF);if(t==279){p.ellipse(11,9,11,15,0xB4CE91);p.rect(15,10,3,13,0x3D6350);}break;
         case 674:
            p.ellipse(4,4,25,25,0xC3A16A);for(int i=0;i<7;i++)p.rect(8+(i*7)%17,8+(i*11)%17,3,3,0x806446);break;
         case 675: case 676:
            p.poly(new int[]{3,13,29,13,25,25,19,29,12,29,6,25},0x957953);p.ellipse(3,9,27,12,0xB79A6E);p.ellipse(6,11,21,8,t==675?0xA07C52:0x776042);if(t==675){p.rect(11,13,4,3,0xD1B78B);p.rect(20,12,3,3,0x84915A);}break;
         case 677: case 678:
            p.ellipse(3,3,27,27,t==677?0x7D918E:0xBDA768);p.ellipse(6,6,21,21,t==677?0xCED4C2:0x658DA2);p.line(16,17,21,8,t==677?0xB66151:0xDDC77B,2);p.line(16,17,11,24,0x546A62,2);p.rect(15,15,3,3,0xEEE7CD);break;
         case 742:
            p.rect(12,3,9,4,0x9A907A);p.rect(11,7,11,5,0xC0C9B1);p.rect(7,12,19,17,0xB8C7AE);p.rect(9,17,15,9,0xDED7AB);p.rect(12,19,8,4,0x8B9E76);p.rect(9,13,2,13,0xD8E0C4);break;
         default: throw new IllegalArgumentException("Missing native icon painter for tile " + t);
      }
      return p.p;
   }

   private static void weaponIcon(Canvas p,int t) {
      if(t==691){p.ellipse(8,9,18,21,0x6B7A58);p.rect(13,3,8,7,0x7E8A76);p.border(17,2,10,8,0xADBAA1);p.rect(10,19,13,2,0x475D49);return;}
      if(t==692){p.line(5,28,15,18,0x6C624B,4);p.poly(new int[]{13,17,26,3,29,3,28,10,18,21},0xCAD4C8);p.line(16,16,27,5,0xE6E6CF,1);return;}
      if(t==693){p.rect(5,8,23,21,0x788065);p.rect(9,11,14,8,0x464F44);p.rect(11,13,10,3,0xA8B887);p.line(7,9,15,3,0xB77454,2);for(int x=10;x<24;x+=5)p.rect(x,23,3,3,0xB8B28D);return;}
      if(t==694||t==695){p.line(9,23,23,t==695?5:12,0x576F66,8);p.line(9,21,23,t==695?3:10,0x9DAC96,2);p.line(13,23,7,29,0x6C6751,3);p.line(15,23,26,29,0x6C6751,3);if(t==694){p.ellipse(4,20,10,10,0x7E7252);p.ellipse(17,20,10,10,0x7E7252);}return;}
      p.rect(4,11,t==688?21:25,6,0x566A63);p.rect(5,11,t==688?20:24,2,0xA1AF9E);p.poly(new int[]{9,16,16,16,13,28,7,26},0x8A7554);
      p.rect(20,14,10,3,0x718279);if(t!=688){p.rect(1,13,7,8,0x8C7D5D);p.rect(17,17,4,8,0x65776B);}if(t==690){p.rect(15,6,12,4,0x74877A);p.rect(18,9,2,3,0x4C625C);}
   }

   /** Tiny aliased rasterizer. All coordinates are actual native texels, not scaled 16px art. */
   private static final class Canvas {
      final int[] p=new int[N*N];
      void clear(){java.util.Arrays.fill(p,0);}
      void set(int x,int y,int rgb){if(x>=0&&x<N&&y>=0&&y<N)p[y*N+x]=c(rgb);}
      void rect(int x,int y,int w,int h,int rgb){for(int yy=y;yy<y+h;yy++)for(int xx=x;xx<x+w;xx++)set(xx,yy,rgb);}
      void erase(int x,int y,int w,int h){for(int yy=Math.max(0,y);yy<Math.min(N,y+h);yy++)for(int xx=Math.max(0,x);xx<Math.min(N,x+w);xx++)p[yy*N+xx]=0;}
      void border(int x,int y,int w,int h,int rgb){rect(x,y,w,1,rgb);rect(x,y+h-1,w,1,rgb);rect(x,y,1,h,rgb);rect(x+w-1,y,1,h,rgb);}
      void line(int x,int y,int x1,int y1,int rgb,int width){int dx=Math.abs(x1-x),dy=-Math.abs(y1-y),sx=x<x1?1:-1,sy=y<y1?1:-1,err=dx+dy;while(true){rect(x-width/2,y-width/2,width,width,rgb);if(x==x1&&y==y1)break;int e=err*2;if(e>=dy){err+=dy;x+=sx;}if(e<=dx){err+=dx;y+=sy;}}}
      void ellipse(int x,int y,int w,int h,int rgb){for(int yy=y;yy<y+h;yy++)for(int xx=x;xx<x+w;xx++){double dx=(xx-x+.5)/w*2-1,dy=(yy-y+.5)/h*2-1;if(dx*dx+dy*dy<=1)set(xx,yy,rgb);}}
      void poly(int[] xy,int rgb){for(int y=0;y<N;y++)for(int x=0;x<N;x++){boolean in=false;for(int i=0,j=xy.length-2;i<xy.length;j=i,i+=2){if((xy[i+1]>y+.5)!=(xy[j+1]>y+.5)&&(x+.5)<(xy[j]-xy[i])*(y+.5-xy[i+1])/(xy[j+1]-xy[i+1])+xy[i])in=!in;}if(in)set(x,y,rgb);}}
      void flower(int x,int y,int col){ellipse(x-6,y-3,13,7,col);ellipse(x-3,y-6,7,13,col);rect(x-1,y-1,3,3,0xC6B072);rect(x-4,y-3,2,2,shade(col,20)&0xFFFFFF);}
      void textTNT(int x,int y,int rgb){String[] rows={"11101010111","01001110010","01001010010","01001011010","01001010010"};for(int yy=0;yy<5;yy++)for(int xx=0;xx<11;xx++)if(rows[yy].charAt(xx)=='1')rect(x+xx*2,y+yy,2,1,rgb);}
   }
}
