package bc;

import java.util.*;

/** Persistent visible cargo. Straight horizontal belts transport real stacks; no container screens. */
final class KineticTransport {
   static boolean carrier(int id){return Kinetics.handles(id)&&(Kinetics.kind(id)==Kinetics.BELT||Kinetics.kind(id)==Kinetics.DEPOT);}
   static int dx(int id){return Kinetics.axis(id)==2?-1:0;}
   static int dz(int id){return Kinetics.axis(id)==0?1:0;}
   static boolean insert(World w,long p,Stack stack){
      if(w.kinetics.saveBlocked||stack==null||stack.n<1||stack.n>stack.max())return false;KineticMachines.Machine m=KineticMachines.get(w,p);Stack old=m.tile.slots[0];
      if(old!=null&&(old.id!=stack.id||old.dmg!=stack.dmg||old.n+stack.n>old.max()))return false;
      if(old==null){m.tile.slots[0]=new Stack(stack.id,stack.n,stack.dmg);m.progress=carrier(Kinetics.block(w,p))?3200:0;}else old.n+=stack.n;return true;
   }
   static void use(Game g,long p){
      if(g.world.kinetics.saveBlocked)return;
      KineticMachines.Machine m=KineticMachines.get(g.world,p);Stack held=g.inv[g.sel];
      if(held==null){int slot=m.tile.slots[2]!=null?2:0;Stack s=m.tile.slots[slot];if(s!=null){g.inv[g.sel]=s;m.tile.slots[slot]=null;}return;}
      if(m.kind==Kinetics.PRESS)return; // Presses work on the visible depot/belt below, never a menu.
      if(m.kind==Kinetics.MILL&&KineticMachines.output(m.kind,held.id)==0)return;
      int count=g.player.sneaking?held.n:1;
      if(insert(g.world,p,new Stack(held.id,count,held.dmg))){held.n-=count;if(held.n==0)g.inv[g.sel]=null;}
   }
   static boolean capture(World w,World.Item item){
      if(item.dead||item.eye||item.vy>0||w.kinetics.saveBlocked||Net319.isClientConnected()&&!Net319.isHosting())return false;
      int x=(int)Math.floor(item.x),y=(int)Math.floor(item.y),z=(int)Math.floor(item.z);
      for(int yy=y;yy>=0&&yy>=y-1;yy--){int id=w.getBlock(x,yy,z);if(!Kinetics.handles(id))continue;int kind=Kinetics.kind(id);
         if(!carrier(id)&&kind!=Kinetics.MILL)continue;
         double top=yy+(kind==Kinetics.MILL?1:.75);if(item.y<top-.2||item.y>top+.35)continue;
         if(kind==Kinetics.MILL&&KineticMachines.output(kind,item.id)==0)return false;
         if(insert(w,World.pack(x,yy,z),new Stack(item.id,item.count,item.dmg))){item.dead=true;return true;}
      }return false;
   }
   static void tick(World w){
      for(var m:w.kinetics.machines.values())m.transportVelocity=0;
      Set<Long> held=new HashSet<>(),arrived=new HashSet<>();
      // Stamp complete batches on visible carriers. A stopped/blocked press keeps the batch in place.
      for(long p:w.kinetics.nodes){int id=Kinetics.block(w,p);if(Kinetics.kind(id)!=Kinetics.PRESS||World.uy(p)<2)continue;
         long below=World.pack(World.ux(p),World.uy(p)-2,World.uz(p));if(!carrier(Kinetics.block(w,below)))continue;
         KineticMachines.Machine press=KineticMachines.get(w,p),cargo=KineticMachines.get(w,below);Stack in=cargo.tile.slots[0];if(press.tile.slots[0]!=null)continue;
         int output=in==null?0:KineticMachines.output(Kinetics.PRESS,in.id);if(output==0){if(press.tile.slots[0]==null)press.progress=0;continue;}
         double rpm=Math.abs(Kinetics.reading(w,p).rpm());if(rpm==0&&press.progress==0)continue;
         // Stop at the centre of the belt, rather than grabbing an item at its leading edge.
         if(Kinetics.kind(Kinetics.block(w,below))==Kinetics.BELT&&Math.abs(cargo.progress-3200)>640)continue;
         cargo.progress=3200;held.add(below);
         if(press.inputId!=in.id){press.inputId=in.id;press.progress=0;}
         press.progress+=(int)Math.round(rpm);
         if(press.progress>=6400){in.id=output;press.progress=0;}
      }
      // One hop maximum per tick, independent of hash iteration order.
      for(long p:new TreeSet<>(w.kinetics.nodes)){
         int id=Kinetics.block(w,p);if(Kinetics.kind(id)!=Kinetics.BELT||held.contains(p)||arrived.contains(p))continue;
         KineticMachines.Machine m=KineticMachines.get(w,p);Stack cargo=m.tile.slots[0];if(cargo==null)continue;
         int advance=(int)Math.round(Kinetics.reading(w,p).rpm()*5);if(advance==0)continue;
         int next=m.progress+advance;if(next>=0&&next<6400){m.progress=next;m.transportVelocity=advance;continue;}
         int sign=advance>0?1:-1,x=World.ux(p)+dx(id)*sign,z=World.uz(p)+dz(id)*sign,y=World.uy(p);
         if(!w.isLoaded(x,z))continue;long to=World.pack(x,y,z);int target=w.getBlock(x,y,z);
         if(carrier(target)){
            if(Kinetics.kind(target)==Kinetics.BELT&&Kinetics.axis(target)!=Kinetics.axis(id))continue;
            KineticMachines.Machine dest=KineticMachines.get(w,to);if(dest.tile.slots[0]!=null)continue;
            dest.tile.slots[0]=cargo;dest.progress=Kinetics.kind(target)==Kinetics.DEPOT?3200:advance>0?0:6399;
            m.tile.slots[0]=null;m.progress=3200;arrived.add(to);
         }else if(Blocks.replaceable[target]){
            World.Item dropped=w.dropItem(x+.5,y+.85,z+.5,cargo.id,cargo.n,cargo.dmg);dropped.vx=dx(id)*sign*.1;dropped.vz=dz(id)*sign*.1;dropped.vy=0;
            m.tile.slots[0]=null;m.progress=3200;
         }
      }
   }
}
