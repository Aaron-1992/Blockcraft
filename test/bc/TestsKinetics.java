package bc;

import java.nio.file.*;
import java.util.*;
import static bc.GameTests.*;

final class TestsKinetics {
   static long put(World w,int x,int y,int z,int kind,int axis,int meta){w.setState(x,y,z,Kinetics.id(kind,axis)|(meta<<12));return World.pack(x,y,z);}
   static void speed(World w,long p,double expected){check(Math.abs(Kinetics.reading(w,p).rpm()-expected)<.0001,"RPM "+expected+": "+Kinetics.describe(w,p));}
   static void run(){
      test("native kinetics: rotating model UVs, finite geometry and real machine GUI at every size",()->{
         for(int kind=0;kind<Kinetics.TYPES;kind++)for(int axis=0;axis<3;axis++)for(double phase:new double[]{0,.3,2})for(double[] box:KineticModels.model(Kinetics.id(kind,axis),phase,.5))for(int face=0;face<6;face++){
            Set<String> uvPairs=new HashSet<>();for(int vertex=0;vertex<4;vertex++){float[] xyz=new float[3],uv=new float[2];Shapes.corner(box,face,vertex,xyz,uv);for(float v:xyz)check(Float.isFinite(v)&&v>=-1.3&&v<=1.6,"bounded rotating vertex");for(float v:uv)check(Float.isFinite(v)&&v>=0&&v<=1,"no neighbouring-atlas bleed");uvPairs.add(uv[0]+","+uv[1]);}eq(4,uvPairs.size(),"noncollapsed texture coordinates on rotating faces");
         }
         Game g=newGame(1);World w=g.world;long p=put(w,0,75,0,Kinetics.PRESS,0,0);g.opt=new Options(Files.createTempFile("kinetic-options",".txt").toFile());KineticMachines.open(g,p);
         for(int[] size:TestsUi.SIZES){GuiRaster raster=new GuiRaster(size[0]*2,size[1]*2);g.gui=raster;g.fbW=g.winW=size[0]*2;g.fbH=g.winH=size[1]*2;raster.begin(g.fbW,g.fbH,2);g.activeBtnsSwap();g.menu.draw(raster,-100,-100,g.menuOx(),g.menuOy());float[] e=raster.textExtent;check(e[0]>=0&&e[1]>=0&&e[2]<=size[0]&&e[3]<=size[1],"machine GUI text within viewport");}
         g.setScreen(Game.S.PLAY);w.shutdown();
      });
      test("kinetic upgrade: purpose-painted surfaces, density and frame interpolation",()->{
         for(int t=KineticArt.CASING;t<KineticArt.BASE+KineticArt.COUNT;t++){
            Set<Integer> colors=new HashSet<>();for(int c:KineticArt.paint(t)){eq(255,c>>>24,"component pixels opaque");colors.add(c);}check(colors.size()>3,"dedicated component detail");
         }
         for(int kind=0;kind<Kinetics.TYPES;kind++)for(double[] b:KineticModels.model(Kinetics.id(kind,0),.23,.4)){
            check(b[8]>=KineticArt.CASING&&b[9]>=KineticArt.CASING&&b[10]>=KineticArt.CASING,"no stretched vanilla block skins");
            float[] xyz=new float[3],uv=new float[2];float min=1,max=0;
            for(int v=0;v<4;v++){Shapes.corner(b,0,v,xyz,uv);min=Math.min(min,uv[0]);max=Math.max(max,uv[0]);}
            check(Math.abs(max-min-(b[3]-b[0]))<.00001,"uniform texel density, not full tile stretched to face");
         }
         Game g=newGame(1);World w=g.world;long motor=put(w,0,75,0,Kinetics.MOTOR,0,3);Kinetics.tick(w);
         double a=KineticRender.angle(w,motor,.1f),b=KineticRender.angle(w,motor,.5f),c=KineticRender.angle(w,motor,.9f);
         check(a<b&&b<c,"different poses within one world tick");check(Math.abs((b-a)-(c-b))<.000001,"linear frame interpolation");
         put(w,0,75,0,Kinetics.MOTOR,0,0);Kinetics.solve(w);eq(KineticRender.angle(w,motor,0),KineticRender.angle(w,motor,1),"stopped shaft has no phantom animation");w.shutdown();
      });
      test("kinetic upgrade: real water-current torque and quiet interaction",()->{
         Game g=newGame(1);World w=g.world;long wheel=put(w,0,75,0,Kinetics.WHEEL,0,0);
         w.setBlock(0,75,1,Blocks.WATER);eq(0D,Kinetics.source(w,wheel),"still water cannot turn wheel");
         w.setState(0,75,1,Blocks.WATER|BlockState.FALLING);eq(8D,Kinetics.source(w,wheel),"falling water positive rim");
         w.setBlock(0,75,1,0);w.setState(0,75,-1,Blocks.WATER|BlockState.FALLING);eq(-8D,Kinetics.source(w,wheel),"opposite waterfall reverses");
         w.setState(0,75,1,Blocks.WATER|BlockState.FALLING);eq(0D,Kinetics.source(w,wheel),"opposing currents cancel");
         w.setBlock(0,75,-1,0);w.setBlock(0,75,1,0);
         w.setBlock(0,76,0,Blocks.WATER);w.setBlock(0,76,1,Blocks.WATER+1);check(Kinetics.source(w,wheel)>0,"horizontal current across upper rim");
         w.setBlock(0,76,1,0);w.setBlock(0,76,-1,Blocks.WATER+1);check(Kinetics.source(w,wheel)<0,"reversing gradient reverses wheel");
         int chats=g.chatLog.size(),raw=w.getState(0,75,0);g.inv[g.sel]=null;Kinetics.use(g,0,75,0,Kinetics.block(w,wheel));eq(raw,w.getState(0,75,0),"right click cannot reverse water");
         long motor=put(w,2,75,0,Kinetics.MOTOR,0,3);Kinetics.use(g,2,75,0,Kinetics.block(w,motor));g.inv[g.sel]=new Stack(Kinetics.WRENCH,1);Kinetics.use(g,2,75,0,Kinetics.block(w,motor));eq(chats,g.chatLog.size(),"motor/wrench/wheel no chat spam");w.shutdown();
      });
      test("kinetic upgrade: belt transport, visible pressing, hand insertion and persistence",()->{
         Game g=newGame(1);World w=g.world;
         put(w,-1,75,0,Kinetics.MOTOR,0,3);long belt=put(w,0,75,0,Kinetics.BELT,0,0);long next=put(w,0,75,1,Kinetics.BELT,0,0);long depot=put(w,0,75,2,Kinetics.DEPOT,0,0);
         KineticTransport.insert(w,belt,new Stack(Items.IRON_INGOT,8));Kinetics.solve(w);
         for(int t=0;t<50;t++)Kinetics.tick(w);eq(8,KineticMachines.get(w,depot).tile.slots[0].n,"actual belt transfers preserve batch");
         long press=put(w,0,77,2,Kinetics.PRESS,0,0);put(w,-1,77,2,Kinetics.MOTOR,0,3);Kinetics.solve(w);
         for(int t=0;t<100;t++)Kinetics.tick(w);eq(Kinetics.IRON_SHEET,KineticMachines.get(w,depot).tile.slots[0].id,"overhead press stamps visible depot batch");eq(8,KineticMachines.get(w,depot).tile.slots[0].n,"batch count conserved");
         int chats=g.chatLog.size();g.inv[g.sel]=null;Kinetics.use(g,0,75,2,Kinetics.block(w,depot));eq(8,g.inv[g.sel].n,"empty-hand extraction");check(g.menu==null,"no inventory UI opened");
         Kinetics.use(g,0,75,2,Kinetics.block(w,depot));eq(7,g.inv[g.sel].n,"inserting consumes one actual item");eq(chats,g.chatLog.size(),"interactions silent");
         World.Item drop=w.dropItem(.5,75.9,.5,Items.GOLD_INGOT,4,0);drop.vy=-.1;check(KineticTransport.capture(w,drop)&&drop.dead,"dropped stacks enter transport");
         KineticMachines.get(w,belt).progress=1234;
         Path folder=Files.createTempDirectory("kinetic-belt-save");setField(w,"dir",folder.toFile());w.saveEnabled=true;KineticMachines.save(w);
         World copy=new World(1,folder.toFile());eq(1234,copy.kinetics.machines.get(belt).progress,"belt position survives reload");eq(4,copy.kinetics.machines.get(belt).tile.slots[0].n,"cargo survives reload");copy.shutdown();
         put(w,-1,75,0,Kinetics.MOTOR,0,0);Kinetics.solve(w);for(int t=0;t<10;t++)Kinetics.tick(w);eq(1234,KineticMachines.get(w,belt).progress,"unpowered belt stops cargo");
         put(w,-1,75,0,Kinetics.MOTOR,0,8);Kinetics.solve(w);Kinetics.tick(w);check(KineticMachines.get(w,belt).progress<1234,"reversed motor moves cargo backward");
         w.setBlock(0,75,0,0);eq(4,w.items.stream().filter(it->!it.dead&&it.id==Items.GOLD_INGOT).mapToInt(it->it.count).sum(),"broken belt returns cargo once");w.shutdown();
      });
      test("native kinetics: registry, native sprites, recipes, oriented placement and wrench persistence",()->{
         Set<Integer> blocks=new HashSet<>();for(int k=0;k<Kinetics.TYPES;k++)for(int a=0;a<3;a++){int id=Kinetics.id(k,a);check(blocks.add(id)&&Blocks.isBlockId(id),"unique real block ID");eq(Kinetics.id(k,0),Blocks.pickItem(id),"canonical pick");check(Shapes.isModel(id),"world/held model");}
         eq(19,Creative.search("create").length,"all fourteen native blocks and five materials/tools searchable");
         for(int t=1000;t<=1004;t++){int[] art=KineticArt.paint(t);check(Arrays.equals(art,KineticArt.paint(t)),"deterministic new native item art");int ink=0;for(int c:art){check(c==0||c>>>24==255,"hard sprite edges, no invisible fringe");if(c!=0)ink++;}check(ink>40&&ink<700,"original readable silhouette");}
         Game g=newGame(1);World w=g.world;g.player.setPos(8,80,8);w.setBlock(0,74,0,Blocks.STONE);g.target=hit(0,74,0,0,1,0);Stack shaft=new Stack(Kinetics.id(Kinetics.SHAFT,0),1);check(Shapes.place(g,shaft),"real special placement");eq(1,Kinetics.axis(w.getBlock(0,75,0)),"floor placement Y axis");
         long motor=put(w,2,75,0,Kinetics.MOTOR,0,3);g.inv[g.sel]=new Stack(Kinetics.WRENCH,1);Kinetics.use(g,2,75,0,Kinetics.block(w,motor));eq(1,Kinetics.axis(Kinetics.block(w,motor)),"wrench rotates axis");eq(3,w.getState(2,75,0)>>>12,"wrench preserves motor setting");
         check(Recipes.find(new Stack[]{new Stack(Blocks313.ANDESITE,1),new Stack(Items.IRON_INGOT,1),null,null},2,2).id==Kinetics.ALLOY,"alloy crafting");w.shutdown();
      });
      test("native kinetics: all three axes, reciprocal gears, bevel geometry and redstone transmission",()->{
         for(int axis=0;axis<3;axis++){
            Game g=newGame(1);World w=g.world;int[] d={0,0,0};d[axis]=1;long motor=put(w,0,75,0,Kinetics.MOTOR,axis,3),shaft=put(w,d[0],75+d[1],d[2],Kinetics.SHAFT,axis,0);Kinetics.solve(w);speed(w,motor,64);speed(w,shaft,64);
            w.setBlock(d[0],75+d[1],d[2],0);Kinetics.solve(w);check(!w.kinetics.nodes.contains(shaft),"breaking removes node");w.shutdown();
         }
         Game g=newGame(1);World w=g.world;long motor=put(w,0,75,0,Kinetics.MOTOR,0,3),large=put(w,1,75,0,Kinetics.LARGE,0,0),small=put(w,1,76,1,Kinetics.COG,0,0),shaft=put(w,2,76,1,Kinetics.SHAFT,0,0);
         Kinetics.solve(w);speed(w,small,-128);speed(w,shaft,-128);eq(-2D,Kinetics.ratio(w,large,small),"large-to-small 2:1 diagonal");eq(1D,Kinetics.ratio(w,large,small)*Kinetics.ratio(w,small,large),"reciprocal gearing");
         long bevel=put(w,2,76,0,Kinetics.LARGE,1,0);eq(1D,Kinetics.ratio(w,large,bevel),"perpendicular bevel gears");w.setBlock(2,76,0,0);
         w.setBlock(2,76,1,0);long clutch=put(w,2,76,1,Kinetics.CLUTCH,0,0),end=put(w,3,76,1,Kinetics.SHAFT,0,0);Kinetics.solve(w);speed(w,end,-128);
         w.setBlock(2,77,1,Redstone319.REDSTONE_BLOCK);Kinetics.solve(w);speed(w,end,0);w.setBlock(2,77,1,0);Kinetics.solve(w);speed(w,end,-128);
         put(w,2,76,1,Kinetics.SHIFT,0,0);w.setBlock(2,77,1,Redstone319.REDSTONE_BLOCK);Kinetics.solve(w);speed(w,end,128);w.shutdown();
      });
      test("native kinetics: water capacity, ratio-weighted stress, conflicting sources and overspeed",()->{
         Game g=newGame(1);World w=g.world;long wheel=put(w,0,75,0,Kinetics.WHEEL,0,0);w.setState(0,75,1,Blocks.WATER|BlockState.FALLING);put(w,1,75,0,Kinetics.LARGE,0,0);put(w,1,76,1,Kinetics.COG,0,0);put(w,2,76,1,Kinetics.LARGE,0,0);put(w,2,77,2,Kinetics.COG,0,0);long press=put(w,3,77,2,Kinetics.PRESS,0,0);Kinetics.solve(w);speed(w,press,32);eq(256D,Kinetics.reading(w,wheel).capacity(),"water capacity");
         long extra=put(w,4,77,2,Kinetics.MILL,0,0);Kinetics.solve(w);speed(w,press,0);check(Kinetics.reading(w,press).fault().equals("Overstressed"),"real shared stress budget");w.setBlock(4,77,2,0);Kinetics.solve(w);speed(w,press,32);w.setBlock(0,75,1,0);Kinetics.solve(w);speed(w,press,0);w.shutdown();
         g=newGame(1);w=g.world;long a=put(w,0,75,0,Kinetics.MOTOR,0,3);put(w,1,75,0,Kinetics.SHAFT,0,0);put(w,2,75,0,Kinetics.MOTOR,0,4);Kinetics.solve(w);check(Kinetics.reading(w,a).fault().contains("Conflicting"),"incompatible motors stall safely");put(w,2,75,0,Kinetics.MOTOR,0,3);Kinetics.solve(w);speed(w,a,64);w.shutdown();
         g=newGame(1);w=g.world;a=put(w,0,75,0,Kinetics.MOTOR,0,3);for(int i=1;i<=3;i++){put(w,i,74+i,i-1,Kinetics.LARGE,0,0);put(w,i,75+i,i,Kinetics.COG,0,0);}Kinetics.solve(w);check(Kinetics.reading(w,a).fault().contains("Overspeed"),"256 RPM safety ceiling, not silent clipping");w.shutdown();
      });
      test("native kinetics: real processing, blocked output, GUI transfer and durable inventories",()->{
         Game g=newGame(1);World w=g.world;put(w,0,75,0,Kinetics.MOTOR,0,3);long press=put(w,1,75,0,Kinetics.PRESS,0,0);KineticMachines.Machine m=KineticMachines.get(w,press);m.tile.slots[0]=new Stack(Items.IRON_INGOT,3);
         for(int i=0;i<50;i++)w.tick();eq(3200,m.progress,"speed-dependent progress, not instant recipe");eq(3,m.tile.slots[0].n,"input not consumed early");
         Path folder=Files.createTempDirectory("kinetic-save");setField(w,"dir",folder.toFile());w.saveEnabled=true;KineticMachines.save(w);World restored=new World(1,folder.toFile());eq(3200,restored.kinetics.machines.get(press).progress,"in-flight progress survives reload");eq(3,restored.kinetics.machines.get(press).tile.slots[0].n,"inventory survives reload");restored.shutdown();
         for(int i=0;i<50;i++)w.tick();eq(1,m.tile.slots[2].n,"one iron sheet produced");eq(2,m.tile.slots[0].n,"exactly one ingot consumed");m.tile.slots[2].n=64;int before=m.progress;for(int i=0;i<120;i++)w.tick();eq(before,m.progress,"full output stops work");eq(2,m.tile.slots[0].n,"blocked output never eats input");
         m.tile.slots[2].n=1;KineticMachines.open(g,press);eq(Menu.KINETIC,g.menu.kind,"real machine menu");check(!g.menu.accepts(g.menu.slots.get(1),new Stack(Items.COAL,1)),"output is extract-only");
         g.menu.quickMove(g.menu.slots.get(1));check(m.tile.slots[2]==null,"real shift-click extracts output");
         w.setBlock(1,75,0,0);eq(Game.S.PLAY,g.screen,"breaking an open machine closes its menu");eq(2,w.items.stream().filter(it->it.id==Items.IRON_INGOT).mapToInt(it->it.count).sum(),"breaking returns remaining inventory exactly once");check(!w.kinetics.machines.containsKey(press),"no ghost machine");
         byte[] bad={1,2,3};Files.write(folder.resolve("kinetics.dat"),bad);World corrupt=new World(1,folder.toFile());check(corrupt.kinetics.saveBlocked,"corrupt inventory blocks processing/overwrite");KineticMachines.save(corrupt);check(Arrays.equals(bad,Files.readAllBytes(folder.resolve("kinetics.dat"))),"corrupt data preserved");corrupt.shutdown();w.shutdown();
      });
      test("native kinetics: chunk boundaries, world-local state and safe workshop",()->{
         Game g=newGame(1);World w=g.world;long motor=put(w,46,75,0,Kinetics.MOTOR,0,3);put(w,47,75,0,Kinetics.SHAFT,0,0);Kinetics.solve(w);check(Kinetics.reading(w,motor).fault().contains("Load"),"unloaded frontier stalls network");w.ensureChunkLoaded(3,0);Kinetics.solve(w);speed(w,motor,64);w.shutdown();
         g=newGame(1);w=g.world;g.player.setPos(0,75,0);KineticWorkshop.build(g);for(int i=0;i<210;i++)w.tick();long press=World.pack(7,78,7);KineticMachines.Machine m=w.kinetics.machines.get(press);check(m!=null&&m.tile.slots[0]!=null&&m.tile.slots[0].id==Kinetics.IRON_SHEET,"actual example world ticks process iron");
         Kinetics.State empty=new Kinetics.State();check(empty.nodes.isEmpty()&&empty.machines.isEmpty(),"independent worlds");w.shutdown();
      });
   }
}
