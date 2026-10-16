package bc;

import java.util.*;
import static bc.GameTests.*;

/** Independent scope allowlist, behavior and shared model contracts for this review. */
final class TestsUtilityReview {
   // Intentionally independent of painter handles(): expanding a painter must not
   // silently authorize unrelated changes or overwrite either earlier review.
   static boolean reopened(int t) {
      return t>=760&&t<=780||t>=39&&t<=43||t>=180&&t<=182||t>=231&&t<=234
         ||t>=265&&t<=267||t>=716&&t<=731||t>=105&&t<=124||t>=135&&t<=150
         ||t>=198&&t<=202||t>=190&&t<=192||t>=688&&t<=693
         ||Arrays.binarySearch(new int[]{30,36,76,77,212,213,220,223,224,237,238,239,269,279,641,
            644,645,646,659,662,663,664,669,671,677,678,679},t)>=0;
   }
   static void run() {
      test("utility review: exact 129-cell scope preserves earlier reviews and explicit item exceptions",()->{
         ResourcePacks.select(ResourcePacks.ID_DEFAULT,null);Tex.initPixels();
         Properties before=new Properties();
         try(java.io.InputStream in=new java.io.FileInputStream("test/pixel32-color-nature.properties")){before.load(in);}
         int changed=0;
         for(int t=0;t<1024;t++) {
            if(Aquatic.tile(t)||KineticArt.tile(t))continue; // Five newly allocated sprites; historical art remains guarded.
            int[] art=BuildingBlockReviewPreview.pixels(t);
            boolean differs=Arrays.hashCode(art)!=Integer.parseInt(before.getProperty(""+t));
            check(differs==reopened(t),"exact requested review scope "+t);
            if(differs){changed++;check(Arrays.equals(art,TexRemaining32.paint(t)),"native reviewed dispatch "+t);}
         }
         eq(129,changed,"108 existing cells and 21 new components");
         for(int t:new int[]{196,218,4,10,35,534,535,536,537,538})
            check(Arrays.hashCode(BuildingBlockReviewPreview.pixels(t))==Integer.parseInt(before.getProperty(""+t)),"explicit exceptions and approved references "+t);
         check(Arrays.hashCode(BowSprite.previousPixels(0,false))==Integer.parseInt(before.getProperty("77")),"honest pre-review bow reference");
         for(int id:Creative.tab(Creative.TOOLS)) {
            int t=Items.tile(id);
            check(t==196||t==218||TexTools32.handles(t),"no omitted creative Tools/Combat item "+Items.name(id));
         }
      });
      test("utility review: native tools have lighter silhouettes, readable TNT and visible cake icing",()->{
         // Comparing painter silhouettes is supplemental; the immutable full-atlas guard is above.
         for(int t=105;t<=124;t++) {
            int current=0,previous=0;
            for(int c:TexTools32.paint(t))if(c>>>24!=0)current++;
            for(int c:TexItems32.tool(t))if(c>>>24!=0)previous++;
            check(current<previous*.85,"material tool silhouette at least 15% slimmer "+t);
         }
         int[] tnt=TexUtility32.paint(180);
         String[] n={"10001","11001","10101","10101","10011","10001","10001"};
         for(int y=0;y<7;y++)for(int x=0;x<5;x++)
            check((tnt[(13+y)*32+13+x]==0xFF493D30)==(n[y].charAt(x)=='1'),"N diagonal, not M "+x+","+y);
         int[] side=TexUtility32.paint(232);
         for(int x=3;x<29;x++)check(side[16*32+x]==0xFFE4D4B7,"icing at actual half-height cake UV "+x);
         check(!Arrays.equals(TexUtility32.paint(212),TexUtility32.paint(213)),"hay cross section not stalk side");
         check(!Arrays.equals(TexUtility32.paint(669),TexUtility32.paint(767)),"sensor states visibly distinct");
         check(!Arrays.equals(TexUtility32.furnace(true,0),TexUtility32.furnace(true,3)),"furnace fire animates");
         for(String pack:new String[]{ResourcePacks.ID_CLASSIC,ResourcePacks.ID_DEFAULT}){
            ResourcePacks.select(pack,null);Tex.initPixels();
            for(int t=760;t<=780;t++)for(int c:TexOverhaulV57.readTile(t))check(c>>>24==255,"component has CPU16 fallback "+t);
         }
      });
      test("utility review: all six observer orientations pulse only on their watching face and power only their output",()->{
         for(int f=0;f<6;f++) {
            Game g=newGame(1);World w=g.world;int x=8,y=72,z=8;
            int dx=Redstone319.observerDx(f),dy=Redstone319.observerDy(f),dz=Redstone319.observerDz(f);
            int off=Redstone319.observerId(f,false),on=Redstone319.observerId(f,true);
            w.setBlock(x,y,z,off);
            // A side change cannot be mistaken for the watched face.
            int sx=dy==0?0:1,sy=dy==0?1:0;
            w.setBlock(x+sx,y+sy,z,Blocks.STONE);
            eq(off,w.getBlock(x,y,z),"ignore side change "+f);
            w.setBlock(x+dx,y+dy,z+dz,Blocks.STONE);
            TestsAquaticRedstone.advance(w,2);
            eq(on,w.getBlock(x,y,z),"watch change pulses "+f);
            eq(0,Redstone319.sourcePower(on),"not an omnidirectional source");
            eq(15,Redstone319.powerAt(w,x,y,z),"pulse power 15");
            for(int[] d:Mesher.FN)check(Redstone319.isPowered(w,x+d[0],y+d[1],z+d[2])
               ==(d[0]==-dx&&d[1]==-dy&&d[2]==-dz),"output only, facing "+f+" neighbour "+Arrays.toString(d));
            TestsAquaticRedstone.advance(w,2);
            eq(off,w.getBlock(x,y,z),"pulse resets preserving orientation "+f);
            check(!Redstone319.isPowered(w,x-dx,y-dy,z-dz),"output goes low again");
            for(int id:new int[]{off,on}) {
               eq(Redstone319.OBSERVER,Blocks.pickItem(id),"pick canonical observer");
               eq(Redstone319.OBSERVER,Redstone319.pickItem(id),"survival drop canonical observer");
               int front=f<4?f+2:f==4?1:0;
               eq(671,Blocks.tiles[id][front],"watching face at physical orientation");
               eq(id==on?776:775,Blocks.tiles[id][front^1],"indicator opposite watching face");
            }
         }
         Game g=newGame(1);g.player.pitch=70;
         eq(2030,Redstone319.orientForPlace(g,Redstone319.OBSERVER,8,72,8),"look down places down-facing");
         g.player.pitch=-70;
         eq(2031,Redstone319.orientForPlace(g,Redstone319.OBSERVER,8,72,8),"look up places up-facing");
      });
      test("utility review: stacked observers safely re-arm timers during reset",()->{
         Game g=newGame(1);World w=g.world;
         for(int y=72;y<77;y++)w.setBlock(8,y,8,Redstone319.OBSERVER_VERTICAL);
         w.setBlock(8,71,8,Blocks.STONE);
         eq(Redstone319.OBSERVER_VERTICAL,w.getBlock(8,72,8),"observer chain begins with scheduled delay");
         TestsAquaticRedstone.advance(w,2);
         eq(Redstone319.OBSERVER_VERTICAL_ON,w.getBlock(8,72,8),"first observer pulses after delay");
         TestsAquaticRedstone.advance(w,80);
         for(int y=72;y<77;y++)eq(Redstone319.OBSERVER_VERTICAL,w.getBlock(8,y,8),"finite chain settles off");
      });
      test("utility review: chest motion invalidates meshes, closes smoothly, resets on world change and keeps its inventory",()->{
         Game g=newGame(1);int x=8,y=72,z=8;g.world.setBlock(x,y,z,Blocks.CHEST);
         TileEntity tile=new TileEntity(TileEntity.CHEST,x,y,z);tile.slots[0]=new Stack(Items.IRON_INGOT,7);
         g.menu=new Menu(g,Menu.CHEST,tile);
         Chunk.Section section=g.world.chunkAt(x,z).sections[y>>4];section.dirty=false;
         ChestLids.update(g,.05);float first=ChestLids.angle(x,y,z);
         check(first>0&&first<1.45,"partial rather than instantaneous opening");
         check(section.dirty,"lid motion invalidates section without changing block ID");
         for(int i=0;i<20;i++)ChestLids.update(g,.05);
         check(Math.abs(ChestLids.angle(x,y,z)-1.45)<.001,"fully open");
         eq(7,tile.slots[0].n,"animation never consumes or replaces inventory");
         double[][] open=UtilityModels.chest(1.45),closed=UtilityModels.chest(0);
         float[] xyz=new float[3],uv=new float[2];
         Shapes.corner(open[5],0,0,xyz,uv);
         check(xyz[1]>1,"actual raised lid geometry, shared with held and GUI renderers");
         float u=uv[0],v=uv[1];Shapes.corner(closed[5],0,0,xyz,uv);
         check(u==uv[0]&&v==uv[1],"lid texture follows hinge without sliding");
         g.menu=null;ChestLids.update(g,.05);
         check(ChestLids.angle(x,y,z)>0&&ChestLids.angle(x,y,z)<1.45,"partial closing");
         for(int i=0;i<20;i++)ChestLids.update(g,.05);
         check(ChestLids.angle(x,y,z)==0,"closes all the way");
         g.menu=new Menu(g,Menu.CHEST,tile);ChestLids.update(g,.1);
         Game other=newGame(1);ChestLids.update(other,.05);
         check(ChestLids.angle(x,y,z)==0,"no animation leaks between worlds");
         eq(Blocks.CHEST,g.world.getBlock(x,y,z),"no new chest save IDs");
      });
      test("utility review: inset egg and cage holes keep the supporting floor visible",()->{
         Game g=newGame(1);int x=8,y=72,z=8;g.world.setBlock(x,y-1,z,Blocks.STONE);
         for(int id:new int[]{Blocks.DRAGON_EGG,Blocks.SPAWNER}) {
            check(!Blocks.opaque[id],"partial/holed model cannot hide adjacent full faces");
            boolean floor=false;
            for(float[][] q:Tests311.quads(g.world,g.world.chunkAt(x,z),x,y,z,id)) {
               float minx=2,maxx=-1;boolean level=true;
               for(float[] v:q){minx=Math.min(minx,v[0]);maxx=Math.max(maxx,v[0]);level&=Math.abs(v[1])<.001;}
               if(level&&minx<.001&&maxx>.999&&Tests311.normal(q)[1]>0)floor=true;
            }
            check(floor,"actual mesh includes full support top below "+id);
         }
         double[][] chest=UtilityModels.chest(1.45);
         for(int i=0;i<6;i++)eq(UtilityModels.CHEST_SIDE,(int)chest[i][8],"hollow walls do not repeat old painted latch");
         eq(UtilityModels.METAL,(int)chest[6][8],"one actual hinged latch");
      });
      test("utility review: banner pick and native UV strips, raised eye, egg shell and selective component emission",()->{
         for(int color=0;color<16;color++)for(int f=0;f<4;f++)for(int base:new int[]{Decor315.STAND0,Decor315.WALL0})
            eq(Decor315.BANNER_ITEM0+color,Blocks.pickItem(base+color*4+f),"pick retains banner colour, not wool");
         double[][] banner=UtilityModels.banner(14,false,0);float[] xyz=new float[3],uv=new float[2];
         double last=0;int strips=0;
         for(double[] b:banner)if(b.length>18){check(b[15]==last&&b[17]-b[15]==.25,"four consecutive UV strips");last=b[17];strips++;Shapes.corner(b,3,0,xyz,uv);check(uv[0]>=b[15]&&uv[0]<=b[17],"strip UV stays in its band");}
         eq(4,strips,"folded fabric, not cube");
         for(int i=2;i<banner.length-1;i++)check(banner[i][5]>=banner[i+1][2]&&banner[i+1][5]>=banner[i][2],"no light-leaking gap between cloth strips");
         double[][] eye=Shapes.modelBoxes(Blocks.END_FRAME_EYE);
         check(eye.length>=3&&eye[2][1]>=14/16.&&eye[2][4]==1,"eye above socket, not painted flat");
         double[][] egg=Shapes.modelBoxes(Blocks.DRAGON_EGG);
         check(egg.length>=6&&egg[0][3]-egg[0][0]<egg[2][3]-egg[2][0]&&egg[5][3]-egg[5][0]<egg[2][3]-egg[2][0],"egg narrows at both ends");
         Set<Integer> emissive=new HashSet<>();float[] e=Shaders.computeEmissive();for(int i=0;i<e.length;i+=2)if(e[i+1]>0)emissive.add((int)e[i]);
         for(int t:new int[]{761,763,764,765,766,767,770,771,772,773,775,777,778,779,780,647,643,16})check(!emissive.contains(t),"non-emissive body/material "+t);
         for(int t:new int[]{760,762,774,776})check(emissive.contains(t),"dedicated glowing component "+t);
         eq(0,Blocks.emit[Redstone319.DAYLIGHT_SENSOR_ON],"powered sensor is not a light source");
         for(int id=Blocks.WALL_TORCH;id<Blocks.WALL_TORCH+4;id++)eq(UtilityModels.CAP,Blocks.tiles[id][0],"correct wall torch cap");
      });
   }
}
