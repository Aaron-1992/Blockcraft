package bc;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Native rotational network foundation. Reference geometry: Create 6.0.10 (MIT code;
 * see package/Create-code-license.txt). Not a Forge loader or a complete Create port. */
final class Kinetics {
   static final int BASE=3980,TYPES=12;
   static final int SHAFT=0,COG=1,LARGE=2,GEARBOX=3,MOTOR=4,WHEEL=5,CLUTCH=6,SHIFT=7,SPEED=8,STRESS=9,PRESS=10,MILL=11;
   static final String[] NAMES={"Shaft","Cogwheel","Large Cogwheel","Gearbox","Creative Motor","Water Wheel","Clutch","Gearshift","Speedometer","Stressometer","Mechanical Press","Millstone"};
   static final int[] SPEEDS={0,16,32,64,128,256,-16,-32,-64,-128,-256,8,-8,4,-4,1};
   static final int ALLOY=930,IRON_SHEET=931,GOLD_SHEET=932,FLOUR=933,WRENCH=934;
   static final int MAX_NODES=4096;
   record Reading(double rpm,double capacity,double stress,String fault){}
   static final class State {
      final Set<Long> nodes=new HashSet<>();final Map<Long,Reading> readings=new ConcurrentHashMap<>();
      final Map<Long,Double> angles=new ConcurrentHashMap<>();final Map<Long,KineticMachines.Machine> machines=new ConcurrentHashMap<>();
      boolean dirty=true,saveBlocked;
      void clear(){nodes.clear();readings.clear();angles.clear();machines.clear();dirty=true;saveBlocked=false;}
   }
   static boolean handles(int id){return id>=BASE&&id<BASE+TYPES*3;}
   static int kind(int id){return (id-BASE)/3;}
   static int axis(int id){return (id-BASE)%3;}
   static int id(int kind,int axis){return BASE+kind*3+axis;}
   static int pick(int id){return id(kind(id),0);}
   static boolean machine(int id){return handles(id)&&(kind(id)==PRESS||kind(id)==MILL);}
   static Reading reading(World w,long key){return w.kinetics.readings.getOrDefault(key,new Reading(0,0,0,"No power"));}
   static void register(){
      for(int k=0;k<TYPES;k++)for(int a=0;a<3;a++){
         int id=id(k,a);if(!"Unknown".equals(Blocks.name[id]))throw new IllegalStateException("Kinetic block ID collision: "+id);
         Blocks.def(id,NAMES[k],2,false,UtilityModels.METAL);Blocks.solid[id]=true;Blocks.opacity[id]=0;Blocks.drop[id]=pick(id);Blocks.mineClass[id]=Items.C_PICK;
         Blocks.coll[id]=Blocks.box[id]=new double[]{0,0,0,1,1,1};
         Shapes.model(id,KineticModels.model(id,0,0));
      }
   }
   static void items(){
      String[] names={"Andesite Alloy","Iron Sheet","Golden Sheet","Wheat Flour","Kinetic Wrench"};
      for(int i=0;i<names.length;i++){if(Items.name[ALLOY+i]!=null)throw new IllegalStateException("Kinetic item ID collision");Items.defTile(ALLOY+i,names[i],KineticArt.BASE+i);}
      Items.maxStack[WRENCH]=1;
   }
   static void creative(ArrayList<Integer> list){for(int k=0;k<TYPES;k++)list.add(id(k,0));for(int i=ALLOY;i<=WRENCH;i++)list.add(i);}
   static void recipes(){
      Recipes.shapeless(ALLOY,2,Blocks313.ANDESITE,Items.IRON_INGOT);
      Recipes.shaped(id(SHAFT,0),8,new String[]{"A","A"},'A',ALLOY);
      Recipes.shaped(id(COG,0),1,new String[]{" P ","PSP"," P "},'P',Blocks.PLANKS,'S',id(SHAFT,0));
      Recipes.shaped(id(LARGE,0),1,new String[]{"PPP","PCP","PPP"},'P',Blocks.PLANKS,'C',id(COG,0));
      Recipes.shaped(WRENCH,1,new String[]{"G G"," A "," S "},'G',Items.GOLD_INGOT,'A',ALLOY,'S',Items.STICK);
      for(int k:new int[]{GEARBOX,CLUTCH,SHIFT,SPEED,STRESS,PRESS,MILL,WHEEL})Recipes.shaped(id(k,0),1,new String[]{" S ","ACA"," B "},'S',id(SHAFT,0),'A',ALLOY,'C',k==PRESS?Blocks.IRON_BLOCK:k==MILL?Blocks.COBBLE:k==WHEEL?Blocks.PLANKS:k==GEARBOX?id(COG,0):Redstone319.ITEM_REDSTONE,'B',k==CLUTCH?Redstone319.LEVER_OFF:k==SHIFT?Redstone319.STONE_BUTTON:k==SPEED?Items.QUARTZ:k==STRESS?Blocks.GLASS:k==WHEEL?id(LARGE,0):Blocks313.ANDESITE);
      Recipes.smelt.put(FLOUR,new Stack(Items.BREAD,1));
   }
   static void changed(World w,int x,int y,int z,int old,int next){
      State s=w.kinetics;long key=World.pack(x,y,z);
      if(handles(next))s.nodes.add(key);else if(handles(old)){s.nodes.remove(key);s.readings.remove(key);s.angles.remove(key);}
      if(machine(old)&&(!machine(next)||kind(old)!=kind(next)))KineticMachines.removed(w,key);
      if(!s.nodes.isEmpty()||handles(old))s.dirty=true;
   }
   static void unloaded(World w,Chunk c){w.kinetics.nodes.removeIf(p->(World.ux(p)>>4)==c.cx&&(World.uz(p)>>4)==c.cz);w.kinetics.dirty=true;}
   static void loaded(World w,Chunk c){w.kinetics.machines.entrySet().removeIf(e->(World.ux(e.getKey())>>4)==c.cx&&(World.uz(e.getKey())>>4)==c.cz&&(!machine(block(w,e.getKey()))||kind(block(w,e.getKey()))!=e.getValue().kind));for(int i=0;i<c.blocks.length;i++)if(handles(c.blocks[i]&4095))w.kinetics.nodes.add(World.pack(c.cx*16+(i&15),i>>8,c.cz*16+((i>>4)&15)));w.kinetics.dirty=true;}
   static boolean powered(World w,long p){return RedstoneLogic.input(w,World.ux(p),World.uy(p),World.uz(p),true)>0;}
   static int block(World w,long p){return w.getBlock(World.ux(p),World.uy(p),World.uz(p));}
   static int raw(World w,long p){return w.getState(World.ux(p),World.uy(p),World.uz(p));}
   static int component(int x,int y,int z,int axis){return axis==0?x:axis==1?y:z;}
   static double port(World w,long p,int dx,int dy,int dz){
      int id=block(w,p),k=kind(id),a=axis(id),d=component(dx,dy,dz,a);
      if(Math.abs(dx)+Math.abs(dy)+Math.abs(dz)!=1)return 0;
      if(k==GEARBOX){if(d!=0)return 0;int first=a==0?1:0;int along=component(dx,dy,dz,first);return along!=0?along:-(dx+dy+dz);}
      if(d==0)return 0;
      if(k==CLUTCH&&powered(w,p))return 0;
      if(k==SHIFT&&d<0&&powered(w,p))return -1;
      return 1;
   }
   /** Reciprocal ratio: adjacent shafts, coplanar small gears, diagonal 2:1 gears, bevel gears. */
   static double ratio(World w,long from,long to){
      int x=World.ux(to)-World.ux(from),y=World.uy(to)-World.uy(from),z=World.uz(to)-World.uz(from);
      int a=block(w,from),b=block(w,to);if(!handles(a)||!handles(b))return 0;
      double p=port(w,from,x,y,z),q=port(w,to,-x,-y,-z);if(p!=0&&q!=0)return p/q;
      int ka=kind(a),kb=kind(b),aa=axis(a),ab=axis(b),m=Math.abs(x)+Math.abs(y)+Math.abs(z);
      if(ka==COG&&kb==COG&&aa==ab&&component(x,y,z,aa)==0&&m==1)return -1;
      if((ka==LARGE&&kb==COG||ka==COG&&kb==LARGE)&&aa==ab&&component(x,y,z,aa)==0&&m==2&&Math.max(Math.abs(x),Math.max(Math.abs(y),Math.abs(z)))==1)return ka==LARGE?-2:-.5;
      if(ka==LARGE&&kb==LARGE&&aa!=ab&&m==2&&Math.abs(component(x,y,z,aa))==1&&Math.abs(component(x,y,z,ab))==1)return component(x,y,z,aa)*component(x,y,z,ab)>0?1:-1;
      return 0;
   }
   static double source(World w,long p){
      int id=block(w,p),k=kind(id);if(k==MOTOR)return SPEEDS[(raw(w,p)>>>12)&15];
      if(k!=WHEEL||axis(id)==1)return 0;
      int x=World.ux(p),y=World.uy(p),z=World.uz(p),a=axis(id);
      // Native milestone rule: actual water in the rim plane drives a compact wheel. No water = no torque.
      for(int[] d:Circuit.DIR)if(component(d[0],d[1],d[2],a)==0&&Blocks.isWater(w.getBlock(x+d[0],y+d[1],z+d[2])))return (raw(w,p)&0x1000)!=0?-8:8;
      return 0;
   }
   static void solve(World w){
      State s=w.kinetics;Set<Long> unseen=new TreeSet<>();for(long p:s.nodes)if(w.isLoaded(World.ux(p),World.uz(p))&&handles(block(w,p)))unseen.add(p);
      s.readings.clear();if(unseen.size()>MAX_NODES){for(long p:unseen)s.readings.put(p,new Reading(0,0,0,"Safety limit: 4096 loaded kinetic blocks"));s.dirty=false;return;}
      while(!unseen.isEmpty()){
         long root=unseen.iterator().next();unseen.remove(root);Map<Long,Double> scale=new LinkedHashMap<>();scale.put(root,1D);ArrayDeque<Long> queue=new ArrayDeque<>();queue.add(root);String fault="";
         while(!queue.isEmpty()){
            long p=queue.remove();int x=World.ux(p),y=World.uy(p),z=World.uz(p),id=block(w,p);boolean cog=kind(id)==COG||kind(id)==LARGE;
            for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++)for(int dz=-1;dz<=1;dz++){
               int m=Math.abs(dx)+Math.abs(dy)+Math.abs(dz);if(m==0||m>2||(!cog&&m!=1)||y+dy<0||y+dy>255)continue;
               if(!w.isLoaded(x+dx,z+dz)){if(cog||port(w,p,dx,dy,dz)!=0)fault="Load the whole kinetic network";continue;}
               long to=World.pack(x+dx,y+dy,z+dz);double r=ratio(w,p,to);if(r==0)continue;double wanted=scale.get(p)*r;
               if(!Double.isFinite(wanted)||Math.abs(wanted)>1e12||Math.abs(wanted)<1e-12){fault="Unsafe gear ratio";continue;}
               Double prev=scale.get(to);if(prev!=null){if(Math.abs(prev-wanted)>1e-7*Math.max(1,Math.abs(prev)))fault="Conflicting gear loop";}
               else{scale.put(to,wanted);unseen.remove(to);queue.add(to);}
            }
         }
         Double rpm=null;for(var e:scale.entrySet()){double speed=source(w,e.getKey());if(speed==0)continue;double candidate=speed/e.getValue();if(rpm==null)rpm=candidate;else if(Math.abs(rpm-candidate)>1e-6)fault="Conflicting power sources";}
         double capacity=0,stress=0,base=rpm==null?0:rpm;
         for(var e:scale.entrySet()){
            int k=kind(block(w,e.getKey()));double speed=Math.abs(e.getValue()*base);
            if(speed>256.000001)fault="Overspeed: maximum 256 RPM";
            double generator=source(w,e.getKey());if(generator!=0)capacity+=Math.abs(generator)*(k==MOTOR?16384:32);
            stress+=speed*(k==PRESS?8:k==MILL?4:0);
         }
         if(stress>capacity+1e-6&&fault.isEmpty())fault="Overstressed";
         if(rpm==null&&fault.isEmpty())fault="No power";
         for(var e:scale.entrySet())s.readings.put(e.getKey(),new Reading(fault.isEmpty()?e.getValue()*base:0,capacity,stress,fault));
      }s.dirty=false;
   }
   static void tick(World w){
      State s=w.kinetics;if(s.nodes.isEmpty())return;
      if(s.dirty||w.tickCount%4==0)solve(w);
      for(var e:s.readings.entrySet())if(e.getValue().rpm!=0){double angle=s.angles.getOrDefault(e.getKey(),0D)+e.getValue().rpm*Math.PI/600;s.angles.put(e.getKey(),Math.IEEEremainder(angle,Math.PI*2));if(w.tickCount%2==0)RasterComputer.dirty(w,e.getKey());}
      if(!Net319.isClientConnected()||Net319.isHosting())KineticMachines.tick(w);
   }
   static boolean use(Game g,int x,int y,int z,int id){
      if(!handles(id))return false;
      if(Net319.isClientConnected()&&!Net319.isHosting()){g.say("Native kinetics: host edits only in this milestone.");return true;}
      long p=World.pack(x,y,z);Stack hand=g.inv[g.sel];
      if(hand!=null&&hand.id==WRENCH){g.world.setState(x,y,z,id(kind(id),(axis(id)+1)%3)|(g.world.getState(x,y,z)&0xF000));g.say(NAMES[kind(id)]+": axis "+"XYZ".charAt((axis(id)+1)%3));return true;}
      if(machine(id)){KineticMachines.open(g,p);return true;}
      int k=kind(id),meta=g.world.getState(x,y,z)>>>12;
      if(k==MOTOR){if(!g.player.creative){g.say("Creative Motor requires Creative mode.");return true;}g.world.setState(x,y,z,id|(((meta+(g.player.sneaking?15:1))&15)<<12));}
      else if(k==WHEEL)g.world.setState(x,y,z,id|((meta^1)<<12));
      solve(g.world);g.say(describe(g.world,p));return true;
   }
   static String describe(World w,long p){Reading r=reading(w,p);return String.format(Locale.ROOT,"%s: %.0f RPM | %.0f / %.0f SU%s",NAMES[kind(block(w,p))],r.rpm,r.stress,r.capacity,r.fault.isEmpty()?"":" | "+r.fault);}
   static void hud(Game g){if(g.target==null||g.world==null||g.screen!=Game.S.PLAY)return;long p=World.pack(g.target.x,g.target.y,g.target.z);if(!handles(block(g.world,p)))return;String line=describe(g.world,p);float scale=Math.min(1,(g.gui.w-24)/Gui.textWidth(line));g.gui.centered(line,g.gui.w/2,g.gui.h/2+26,0xffead6b1,true,scale);}
}
