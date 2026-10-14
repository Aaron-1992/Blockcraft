package bc;

import java.util.ArrayList;

/** Aquatic additions use new IDs/texture cells; none of the existing artwork is repainted. */
final class Aquatic {
   static final int SEAGRASS=2700, KELP=2701, CORAL=2702, FAN=2707, DEAD=2712;
   static final int TILE=781, TILE_END=793;
   private static final String[] COLORS={"Tube","Brain","Bubble","Fire","Horn"};
   private static final int[] PAL={0x365FC5,0xD47EAA,0x9559BA,0xBD4545,0xC6AF4E};
   private Aquatic() {}
   static boolean plant(int id){return id==SEAGRASS||id==KELP||id>=FAN&&id<FAN+5;}
   static boolean liveCoral(int id){return id>=CORAL&&id<CORAL+5;}
   static boolean tile(int t){return t>=TILE&&t<=TILE_END;}
   static void register(){
      for(int id=SEAGRASS;id<=DEAD;id++) {
         String name=id==SEAGRASS?"Seagrass":id==KELP?"Kelp":id==DEAD?"Dead Coral Block":COLORS[(id-(id<FAN?CORAL:FAN))%5]+(id<FAN?" Coral Block":" Coral Fan");
         int texture=id==DEAD?TILE_END:TILE+id-SEAGRASS;
         Blocks.def(id,name,plant(id)?0:.7F,!plant(id),texture);
         if(plant(id)) {
            Blocks.kind[id]=1;Blocks.solid[id]=false;Blocks.opacity[id]=3;
            Blocks.coll[id]=null;Blocks.cutout[id]=true;Blocks.replaceable[id]=true;
            Blocks.box[id]=new double[]{.1,0,.1,.9,id==KELP?1:.8,.9};
         }
      }
   }
   static void creative(ArrayList<Integer> list){for(int id=SEAGRASS;id<=DEAD;id++)list.add(id);}
   static void update(World w,int x,int y,int z,int id){
      if(plant(id)) {
         int below=w.getBlock(x,y-1,z);
         if(!Blocks.solid[below] && !(id==KELP&&below==KELP)) {
            w.setBlock(x,y,z,0);w.dropItem(x+.5,y+.25,z+.5,id,1);
         }
      } else if(liveCoral(id)) {
         boolean wet=false;
         for(int[] d:RedstoneLogic.SIX)wet|=WaterPhysics.contains(w,x+d[0],y+d[1],z+d[2]);
         if(!wet)w.setBlock(x,y,z,DEAD);
      }
   }
   static int ocean(double temperature,int height){
      boolean deep=height<45;
      if(temperature<.22)return deep?Biomes.DEEP_FROZEN_OCEAN:Biomes.FROZEN_OCEAN;
      if(temperature<.40)return deep?Biomes.DEEP_COLD_OCEAN:Biomes.COLD_OCEAN;
      if(temperature>.70)return Biomes.WARM_OCEAN;
      if(temperature>.58)return deep?Biomes.DEEP_LUKEWARM_OCEAN:Biomes.LUKEWARM_OCEAN;
      return deep?Biomes.DEEP_OCEAN:Biomes.OCEAN;
   }
   static long hash(long seed,int x,int z){
      long h=seed+x*341873128712L+z*132897987541L;h=(h^(h>>>33))*0xff51afd7ed558ccdL;
      h=(h^(h>>>33))*0xc4ceb9fe1a85ec53L;return h^(h>>>33);
   }
   /** Every column samples world coordinates, including reef and iceberg shapes crossing chunk seams. */
   static void decorate(TerraGen gen,Chunk ch,int[] heights,int[] biomes){
      for(int z=0;z<16;z++)for(int x=0;x<16;x++) {
         int i=z*16+x,h=heights[i],biome=biomes[i],wx=ch.cx*16+x,wz=ch.cz*16+z;
         if(h>=61||(!Biomes.ocean(biome)&&!Biomes.river(biome)))continue;
         long rnd=hash(gen.seed,wx,wz);
         if(biome==Biomes.WARM_OCEAN) {
            // Low branched colonies, clustered rather than a uniform random carpet.
            int gx=Math.floorDiv(wx,24),gz=Math.floorDiv(wz,24);
            for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++) {
               long root=hash(gen.seed+9041,gx+dx,gz+dz);
               int cx=(gx+dx)*24+4+(int)Math.floorMod(root,16),cz=(gz+dz)*24+4+(int)Math.floorMod(root>>>12,16);
               int dist=(wx-cx)*(wx-cx)+(wz-cz)*(wz-cz),radius=3+(int)Math.floorMod(root>>>24,4);
               if(dist<radius*radius && h>44 && ((rnd>>>5)&7)!=0) {
                  int kind=(int)Math.floorMod(root>>>30,5),height=1+(int)((radius*radius-dist)/(double)(radius*radius)*3);
                  for(int y=h+1;y<=Math.min(59,h+height);y++)if(ch.get(x,y,z)==Blocks.WATER)ch.set(x,y,z,CORAL+kind);
                  int top=Math.min(59,h+height)+1;
                  if(ch.get(x,top,z)==Blocks.WATER)ch.set(x,top,z,FAN+kind);
               }
            }
         } else if(!Biomes.snowy(biome)&&Biomes.ocean(biome) && (rnd&15)==0) {
            int len=2+(int)Math.floorMod(rnd>>>8,16);
            for(int y=h+1;y<=Math.min(61,h+len);y++)if(ch.get(x,y,z)==Blocks.WATER)ch.set(x,y,z,KELP);else break;
         }
         if((rnd&7)<2 && ch.get(x,h+1,z)==Blocks.WATER)ch.set(x,h+1,z,SEAGRASS);
         if(biome==Biomes.FROZEN_OCEAN||biome==Biomes.DEEP_FROZEN_OCEAN) {
            int gx=Math.floorDiv(wx,48),gz=Math.floorDiv(wz,48);
            for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++) {
               long root=hash(gen.seed+641,gx+dx,gz+dz);
               int cx=(gx+dx)*48+10+(int)Math.floorMod(root,28),cz=(gz+dz)*48+10+(int)Math.floorMod(root>>>16,28);
               double dist=Math.sqrt((wx-cx)*(wx-cx)+(wz-cz)*(wz-cz));
               double radius=6+Math.floorMod(root>>>32,7),q=1-dist/radius;
               if(q<=0)continue;
               int top=63+(int)(q*(8+Math.floorMod(root>>>40,10)));
               for(int y=Math.max(h+1,58-(int)(q*5));y<=top;y++)if(ch.get(x,y,z)==0||Blocks.isWater(ch.get(x,y,z))||ch.get(x,y,z)==Blocks313.ICE)
                  ch.set(x,y,z,y==top?Blocks.SNOW:Blocks313.PACKED_ICE);
            }
         }
      }
   }
   static int[] paint(int tile){
      PixelArt32 a=new PixelArt32();int t=tile-TILE;
      if(t==0||t==1) {
         for(int i=0;i<7;i++) {
            int x=3+i*4,top=t==1?0:8+(int)(TexPixel32.hash(i,0,51)*12);
            a.line(16,31,x,top,0x365A30);a.line(17,31,x+1,top,0x73904B);
            if(t==1)for(int y=4;y<31;y+=7){a.line(x,y,x-3,y-3,0x63813D);a.line(x+1,y,x+4,y-2,0x99AC5F);}
         }
      } else if(t>=7&&t<=11) {
         int col=PAL[t-7];
         for(int i=0;i<9;i++) {int x=2+i*3;a.line(16,31,x,5+Math.abs(i-4)*2,col);a.line(x,8+Math.abs(i-4)*2,x-2,4+Math.abs(i-4)*2,col);}
         a.line(15,31,16,10,0xCBD7AA);
      } else {
         int col=t==12?0x8A8272:PAL[t-2];
         for(int y=0;y<32;y++)for(int x=0;x<32;x++) {
            int cell=(int)(TexPixel32.hash(x/3,y/3,t+10)*4),d=cell*7-12;
            if(x%4==1&&y%4==1)d-=35;
            a.p[y*32+x]=TexPixel32.shade(0xFF000000|col,d);
         }
      }
      return a.p;
   }
   static void initPixels(){for(int t=TILE;t<=TILE_END;t++)Tex.blit(t,ResourcePacks.resize(paint(t),16));}
}
