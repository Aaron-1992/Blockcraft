package bc;

/** Sixteen individually drawn ceramic designs, inspired by the decorative
 * vocabulary of the 1.12 color set, not copied game assets. Flat pigment edges
 * are rasterized at 32px; no noisy bevel applied to each printed shape. */
final class TexGlazedReview32 {
   private TexGlazedReview32() {}
   private static final int[][] PAL={
      {0xE8DDC5,0x437D9C,0x90B4BF,0xD2A03F},
      {0xD88438,0x354D70,0xE6C788,0xA94D2D},
      {0xBA619C,0xE9D9D7,0x803B76,0xD8A0BE},
      {0x7CB6C9,0xE2DDD0,0x345879,0xBD923D},
      {0xDFB846,0x297D91,0xEBDB9E,0x8E692E},
      {0x93B84D,0xE5D79B,0x3E7866,0x617D33},
      {0xDEA0AE,0xAC4660,0xEEDDCB,0x687C50},
      {0x565C5D,0x26383B,0xC7C9B9,0x8BA6A2},
      {0xBDBEB0,0xE6E0C9,0x697E80,0xA77C69},
      {0x338A92,0x173F5D,0x9EC6BE,0x587F89},
      {0x885891,0x4E366A,0xD3B4C7,0xAB7CAA},
      {0x36538B,0xDFDACC,0x7899B4,0x22365C},
      {0x876044,0xC4A36D,0x3D6876,0x503D30},
      {0x657B3B,0xB8B66B,0x304F39,0x94A451},
      {0xAC503C,0xE1C8A0,0x663D36,0xC7844A},
      {0x292F35,0xBC613D,0xDDAA57,0x536D76}
   };
   static int[] paint(int dye) {
      PixelArt32 a=new PixelArt32();int[] p=PAL[dye];a.rect(0,0,32,32,p[0]);
      switch(dye) {
         case 0: // Blue cornflower, warm centre, corner leaf sprays.
            petals(a,p[1],16,16,13,5,4);
            petals(a,p[2],16,16,9,3,4);
            diamond(a,16,16,4,p[3]);
            ink(a,p[1],0,0,10,0,4,4,0,10);ink(a,p[1],32,32,22,32,28,28,32,22);
            break;
         case 1: // Four hooked navy fans on orange, cream inner feather.
            for(int r=0;r<4;r++){
               rotated(a,p[1],r,0,0,15,0,15,5,7,5,7,11,2,16,0,16);
               rotated(a,p[2],r,9,7,15,7,15,12,11,16,8,13);
            }
            diamond(a,16,16,3,p[3]);break;
         case 2: // The directional magenta motif reads as arrows, not generic diamonds.
            ink(a,p[1],3,11,16,11,16,5,29,16,16,27,16,21,3,21);
            ink(a,p[2],3,0,11,0,17,6,10,6);ink(a,p[2],3,32,11,32,17,26,10,26);
            a.rect(5,14,13,4,p[3]);ink(a,p[3],19,10,25,16,19,22);break;
         case 3: // Quarter compass/diamond fans meet along adjoining edges.
            for(int r=0;r<4;r++){
               rotated(a,p[2],r,0,0,14,0,0,14);
               rotated(a,p[1],r,0,0,8,0,0,8);
               rotated(a,p[1],r,16,3,21,11,16,15,11,11);
            }
            diamond(a,16,16,4,p[3]);break;
         case 4: // Cyan-gold floral ceramic, with split petals.
            for(int r=0;r<4;r++){
               rotated(a,p[1],r,12,3,20,3,23,8,18,15,14,15,9,8);
               rotated(a,p[2],r,15,5,17,5,17,12,15,12);
            }
            diamond(a,16,16,4,p[3]);
            for(int x:new int[]{1,29})for(int y:new int[]{1,29})diamond(a,x,y,3,p[2]);break;
         case 5: // Linked lime-and-cream stepped scroll.
            band(a,p[1],3,0,3,20,12,20,12,9,24,9,24,29,32,29);
            band(a,p[2],0,27,8,27,8,31);
            ink(a,p[3],17,15,21,15,21,25,17,29);a.rect(15,0,10,3,p[1]);break;
         case 6: // Rose petals with cream separations and olive corner leaves.
            for(int r=0;r<4;r++){
               rotated(a,p[1],r,15,15,7,13,5,8,8,4,13,6,17,13);
               rotated(a,p[2],r,15,14,10,11,9,7,13,9);
               rotated(a,p[3],r,0,0,7,1,3,5,0,8);
            }
            diamond(a,16,16,3,p[2]);break;
         case 7: // Interwoven charcoal ribbons and pale folded corners.
            for(int r=0;r<4;r++){
               rotated(a,p[1],r,0,7,9,7,9,0,13,0,13,11,4,11,4,22,0,22);
               rotated(a,p[2],r,17,0,22,0,27,5,27,12,23,12,23,7);
               rotated(a,p[3],r,14,13,19,13,19,18,14,18);
            }break;
         case 8: // Eight-point stone/ivory star with contrasting inlay at its tips.
            star(a,16,16,14,7,p[2]);star(a,16,16,10,5,p[1]);diamond(a,16,16,3,p[3]);
            for(int r=0;r<4;r++)rotated(a,p[3],r,0,0,7,0,0,7);break;
         case 9: // Deep cyan key-wave: two interleaved directional bands.
            band(a,p[1],0,5,23,5,23,20,10,20,10,13,17,13);
            band(a,p[2],32,28,3,28,3,11,6,8);
            a.rect(28,0,4,20,p[3]);a.rect(13,25,11,2,p[0]);break;
         case 10: // Violet iris with a cross-shaped pale centre.
            for(int r=0;r<4;r++){
               rotated(a,p[1],r,16,16,5,13,1,6,6,1,13,5);
               rotated(a,p[2],r,16,16,8,11,6,5,12,8);
               rotated(a,p[3],r,15,0,19,0,19,9,16,12,13,9);
            }
            diamond(a,16,16,3,p[1]);break;
         case 11: // Cobalt ceramic sprays, curved pointed leaves rather than a solid cross.
            for(int r=0;r<4;r++){
               rotated(a,p[1],r,16,16,10,12,8,5,11,1,15,6);
               rotated(a,p[2],r,16,16,21,10,29,9,26,14,20,17);
               rotated(a,p[3],r,0,0,7,0,3,4,0,7);
            }
            diamond(a,16,16,3,p[1]);break;
         case 12: // Ochre stepped spirals with blue ceramic corner inlays.
            band(a,p[1],2,0,2,23,24,23,24,7,10,7,10,15,17,15);
            band(a,p[3],0,29,30,29,30,0);
            ink(a,p[2],0,0,0,9,9,0);ink(a,p[2],32,32,22,32,32,22);break;
         case 13: // Olive vine with ochre leaves, winding between tiles.
            band(a,p[2],0,26,7,26,12,21,12,12,20,6,32,6);
            ink(a,p[1],5,25,2,17,3,12,9,16,10,21);
            ink(a,p[3],13,17,19,12,25,13,20,19,13,21);
            ink(a,p[1],20,7,19,0,26,0,25,5);
            ink(a,p[3],0,0,9,2,12,8,4,6);
            diamond(a,26,26,4,p[1]);break;
         case 14: // Terracotta pinwheel, broken cream chevrons and warm red field.
            for(int r=0;r<4;r++){
               rotated(a,p[1],r,0,0,17,0,17,5,6,5,16,15,12,19,0,7);
               rotated(a,p[2],r,21,7,27,7,27,21,23,17,23,11);
               rotated(a,p[3],r,9,7,17,7,17,12,14,12);
            }
            diamond(a,16,16,3,p[2]);break;
         case 15: // Black, rust and amber angular rosette.
            star(a,16,16,16,9,p[1]);star(a,16,16,12,6,p[2]);
            star(a,16,16,8,4,p[0]);diamond(a,16,16,3,p[3]);
            for(int r=0;r<4;r++)rotated(a,p[3],r,0,0,9,0,6,3,3,3,3,6,0,9);break;
      }
      // Uniform, quiet glaze sheen; no fake relief around printed pigment edges.
      for(int y=0;y<32;y++)for(int x=0;x<32;x++) {
         int d=(x+y<14?2:x+y>51?-2:0);
         a.p[y*32+x]=PixelArt32.shade(a.p[y*32+x],d);
      }
      return a.p;
   }
   private static void ink(PixelArt32 a,int color,int... xy) {
      PixelArt32 mask=new PixelArt32();mask.poly(1,0,xy);
      for(int i=0;i<1024;i++)if(mask.p[i]!=0)a.p[i]=PixelArt32.c(color);
   }
   private static void rotated(PixelArt32 a,int c,int rotation,int... xy) {
      for(int i=0;i<xy.length;i+=2)for(int r=0;r<rotation;r++){int x=xy[i];xy[i]=32-xy[i+1];xy[i+1]=x;}
      ink(a,c,xy);
   }
   private static void band(PixelArt32 a,int c,int... xy) {
      for(int i=2;i<xy.length;i+=2)for(int d=0;d<3;d++) {
         a.line(xy[i-2]+d-1,xy[i-1],xy[i]+d-1,xy[i+1],c);
         a.line(xy[i-2],xy[i-1]+d-1,xy[i],xy[i+1]+d-1,c);
      }
   }
   private static void diamond(PixelArt32 a,int x,int y,int r,int c){ink(a,c,x,y-r,x+r,y,x,y+r,x-r,y);}
   private static void petals(PixelArt32 a,int c,int x,int y,int reach,int width,int count) {
      for(int k=0;k<count;k++)rotated(a,c,k,x,y,x-width,y-reach+3,x,y-reach,x+width,y-reach+3);
   }
   private static void star(PixelArt32 a,int x,int y,int outer,int inner,int c) {
      int[] xy=new int[32];
      for(int k=0;k<16;k++){double v=k*Math.PI/8;int r=k%2==0?outer:inner;xy[k*2]=x+(int)Math.round(Math.cos(v)*r);xy[k*2+1]=y+(int)Math.round(Math.sin(v)*r);}
      ink(a,c,xy);
   }
}
