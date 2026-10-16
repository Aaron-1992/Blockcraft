package bc;

import java.io.*;
import java.util.*;

/** An inspectable, ordinary-block example factory, not a hidden machine simulator. */
final class KineticWorkshop {
   static Map<Long,Integer> plan(int x,int y,int z){
      Map<Long,Integer> p=new LinkedHashMap<>();for(int dx=-2;dx<=19;dx++)for(int dz=-3;dz<=5;dz++)p.put(World.pack(x+dx,y-1,z+dz),Blocks313.ANDESITE);
      int[] row={Kinetics.MOTOR,Kinetics.SHAFT,Kinetics.LARGE,Kinetics.CLUTCH,Kinetics.SHIFT,Kinetics.SPEED,Kinetics.STRESS,Kinetics.MILL};
      for(int i=0;i<row.length;i++)p.put(World.pack(x+i,y,z),Kinetics.id(row[i],0)|(i==0?3<<12:0));
      p.put(World.pack(x+2,y+1,z+1),Kinetics.id(Kinetics.COG,0));p.put(World.pack(x+3,y+1,z+1),Kinetics.id(Kinetics.SHAFT,0));p.put(World.pack(x+4,y+1,z+1),Kinetics.id(Kinetics.PRESS,0));
      p.put(World.pack(x+3,y,z-1),Redstone319.LEVER_OFF);p.put(World.pack(x+4,y,z-1),Redstone319.LEVER_OFF);
      p.put(World.pack(x+11,y,z),Kinetics.id(Kinetics.WHEEL,0));p.put(World.pack(x+12,y,z),Kinetics.id(Kinetics.SHAFT,0));p.put(World.pack(x+13,y,z),Kinetics.id(Kinetics.MILL,0));
      p.put(World.pack(x+11,y,z+1),Blocks.WATER);p.put(World.pack(x+10,y,z+1),Blocks.STONEBRICK);p.put(World.pack(x+12,y,z+1),Blocks.STONEBRICK);p.put(World.pack(x+11,y,z+2),Blocks.STONEBRICK);
      p.put(World.pack(x+17,y,z),Kinetics.id(Kinetics.GEARBOX,1));
      return p;
   }
   static void build(Game g)throws IOException{
      if(g.world==null||g.player==null||!g.player.creative||Net319.isClientConnected()&&!Net319.isHosting())throw new IOException("Native workshop requires Creative host.");
      if(g.world.kinetics.saveBlocked)throw new IOException("Repair/back up kinetics.dat before building.");
      int x=(int)Math.floor(g.player.x)+3,y=(int)Math.floor(g.player.y)+3,z=(int)Math.floor(g.player.z)+3;
      if(y<2||y>250)throw new IOException("Need more vertical room for the workshop.");Map<Long,Integer> plan=plan(x,y,z);
      for(int xx=x-3;xx<=x+20;xx++)for(int zz=z-4;zz<=z+6;zz++){if(!g.world.isLoaded(xx,zz))throw new IOException("Load the workshop area first.");for(int yy=y-1;yy<=y+3;yy++)if(g.world.getBlock(xx,yy,zz)!=0)throw new IOException("Workshop needs clear air; move higher. Nothing was overwritten.");}
      for(var e:plan.entrySet()){long p=e.getKey();g.world.setState(World.ux(p),World.uy(p),World.uz(p),e.getValue());}
      KineticMachines.get(g.world,World.pack(x+4,y+1,z+1)).tile.slots[0]=new Stack(Items.IRON_INGOT,8);
      KineticMachines.get(g.world,World.pack(x+7,y,z)).tile.slots[0]=new Stack(Items.WHEAT,8);
      KineticMachines.get(g.world,World.pack(x+13,y,z)).tile.slots[0]=new Stack(Items.WHEAT,8);
      Kinetics.solve(g.world);g.player.setPos(x+8,y+7,z-10);g.player.yaw=0;g.player.pitch=28;g.player.flying=true;g.say("Native Create foundation workshop placed. Press: 2:1 gearing; levers: clutch/gearshift. Right-click machines for inventory. Full Create is NOT finished.");
   }
   static long started;static int originX,originY,originZ;static boolean done;
   static void smoke(Game g,int frame){try{
      if(frame==80){build(g);originX=(int)(g.player.x-8);originY=(int)(g.player.y-7);originZ=(int)(g.player.z+10);started=g.world.tickCount;}
      if(frame>=280&&!done&&g.world.tickCount-started>=130){
         long press=World.pack(originX+4,originY+1,originZ+1),mill=World.pack(originX+7,originY,originZ);
         Stack a=KineticMachines.get(g.world,press).tile.slots[2],b=KineticMachines.get(g.world,mill).tile.slots[2];
         if(a==null||a.id!=Kinetics.IRON_SHEET||b==null||b.id!=Kinetics.FLOUR)throw new IOException("Native workshop did not process real items");
         if(Math.abs(Kinetics.reading(g.world,press).rpm())!=128)throw new IOException("Native 2:1 gear ratio failed");
         System.out.println("[kinetics] press="+a.n+" iron sheets at 128 RPM; mill="+b.n+" flour; ordinary connected blocks");done=true;
         if("machine".equals(System.getProperty("bc.kinetics")))KineticMachines.open(g,press);
      }
      if(done)g.autoShotNow=true;
   }catch(Exception e){throw new RuntimeException("Kinetic workshop smoke failed",e);}}
}
