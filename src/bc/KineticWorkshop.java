package bc;

import java.io.*;
import java.util.*;

/** Ordinary powered workshop: visible belt cargo, overhead pressing and a falling-water wheel. */
final class KineticWorkshop {
   static Map<Long,Integer> plan(int x,int y,int z){
      Map<Long,Integer> p=new LinkedHashMap<>();for(int dx=-2;dx<=19;dx++)for(int dz=-3;dz<=5;dz++)p.put(World.pack(x+dx,y-1,z+dz),Blocks313.ANDESITE);
      int[] row={Kinetics.MOTOR,Kinetics.SHAFT,Kinetics.LARGE,Kinetics.CLUTCH,Kinetics.SHIFT,Kinetics.SPEED,Kinetics.STRESS,Kinetics.MILL};
      for(int i=0;i<row.length;i++)p.put(World.pack(x+i,y+1,z),Kinetics.id(row[i],0)|(i==0?3<<12:0));
      p.put(World.pack(x+2,y+2,z+1),Kinetics.id(Kinetics.COG,0));p.put(World.pack(x+3,y+2,z+1),Kinetics.id(Kinetics.SHAFT,0));p.put(World.pack(x+4,y+2,z+1),Kinetics.id(Kinetics.PRESS,0));
      p.put(World.pack(x+3,y+1,z-1),Redstone319.LEVER_OFF);p.put(World.pack(x+4,y+1,z-1),Redstone319.LEVER_OFF);
      for(int dz=-2;dz<=3;dz++)p.put(World.pack(x+4,y,z+dz),Kinetics.id(Kinetics.BELT,0));
      p.put(World.pack(x+3,y,z-2),Kinetics.id(Kinetics.MOTOR,0)|(3<<12));p.put(World.pack(x+4,y,z+4),Kinetics.id(Kinetics.DEPOT,0));
      p.put(World.pack(x+11,y+1,z),Kinetics.id(Kinetics.WHEEL,0));p.put(World.pack(x+12,y+1,z),Kinetics.id(Kinetics.SHAFT,0));p.put(World.pack(x+13,y+1,z),Kinetics.id(Kinetics.MILL,0));
      // A real source above an open column: downward water on the +Z rim gives +X torque.
      for(int yy=y;yy<=y+3;yy++)p.put(World.pack(x+11,yy,z+1),Blocks.WATER|BlockState.FALLING);
      p.put(World.pack(x+11,y+4,z+1),Blocks.WATER);
      p.put(World.pack(x+11,y-1,z+1),0);
      for(int yy=y;yy<=y+4;yy++)for(int[] d:new int[][]{{-1,0},{1,0},{0,1}})p.put(World.pack(x+11+d[0],yy,z+1+d[1]),Blocks.GLASS);
      p.put(World.pack(x+11,y+4,z),Blocks.GLASS);
      p.put(World.pack(x+17,y,z),Kinetics.id(Kinetics.GEARBOX,1));
      return p;
   }
   static void build(Game g)throws IOException{
      if(g.world==null||g.player==null||!g.player.creative||Net319.isClientConnected()&&!Net319.isHosting())throw new IOException("Workshop requires Creative host.");
      if(g.world.kinetics.saveBlocked)throw new IOException("Repair/back up kinetics.dat before building.");
      int x=(int)Math.floor(g.player.x)+3,y=(int)Math.floor(g.player.y)+3,z=(int)Math.floor(g.player.z)+3;
      if(y<2||y>249)throw new IOException("Need more vertical room for the workshop.");Map<Long,Integer> plan=plan(x,y,z);
      for(int xx=x-3;xx<=x+20;xx++)for(int zz=z-4;zz<=z+6;zz++){if(!g.world.isLoaded(xx,zz))throw new IOException("Load the workshop area first.");for(int yy=y-1;yy<=y+5;yy++)if(g.world.getBlock(xx,yy,zz)!=0)throw new IOException("Workshop needs clear air; move higher. Nothing was overwritten.");}
      for(var e:plan.entrySet()){long p=e.getKey();g.world.setState(World.ux(p),World.uy(p),World.uz(p),e.getValue());}
      KineticTransport.insert(g.world,World.pack(x+4,y,z-1),new Stack(Items.IRON_INGOT,8));
      KineticMachines.get(g.world,World.pack(x+7,y+1,z)).tile.slots[0]=new Stack(Items.WHEAT,8);
      KineticMachines.get(g.world,World.pack(x+13,y+1,z)).tile.slots[0]=new Stack(Items.WHEAT,8);
      Kinetics.solve(g.world);g.player.setPos(x+22,y+10,z-14);g.player.yaw=40;g.player.pitch=25;g.player.flying=true;
   }
   static long started;static int originX,originY,originZ;static boolean done;
   static void smoke(Game g,int frame){try{
      if(frame==80){originX=(int)Math.floor(g.player.x)+3;originY=(int)Math.floor(g.player.y)+3;originZ=(int)Math.floor(g.player.z)+3;build(g);started=g.world.tickCount;}
      if(frame>=280&&!done&&g.world.tickCount-started>=210){
         long press=World.pack(originX+4,originY+2,originZ+1),depot=World.pack(originX+4,originY,originZ+4),mill=World.pack(originX+7,originY+1,originZ);
         Stack a=KineticMachines.get(g.world,depot).tile.slots[0],b=KineticMachines.get(g.world,mill).tile.slots[2];
         if(a==null||a.id!=Kinetics.IRON_SHEET||a.n!=8||b==null||b.id!=Kinetics.FLOUR)throw new IOException("Belt did not deliver eight stamped sheets onto depot");
         if(Math.abs(Kinetics.reading(g.world,press).rpm())!=128)throw new IOException("2:1 gear ratio failed");
         long wheel=World.pack(originX+11,originY+1,originZ);if(Kinetics.source(g.world,wheel)<=0)throw new IOException("Falling water did not drive wheel");
         System.out.println("[kinetics] press=8 iron sheets via belt to depot at 128 RPM; mill="+b.n+" flour; directional flowing wheel; no GUI");done=true;
         if("machine".equals(System.getProperty("bc.kinetics"))){g.player.setPos(originX+9,originY+5,originZ-5);g.player.yaw=35;g.player.pitch=20;}
      }
      if(frame>=Integer.getInteger("bc.shot",1200)-1&&!done)throw new IOException("Kinetic smoke timed out before real production");
      if(done)g.autoShotNow=true;
   }catch(Exception e){throw new RuntimeException("Kinetic workshop smoke failed",e);}}
}
