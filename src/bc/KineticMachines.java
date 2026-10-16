package bc;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Host-owned processing inventories, saved separately with strict bounds and corruption preservation. */
final class KineticMachines {
   static final class Machine {
      final int kind;final TileEntity tile;int inputId;volatile int progress;volatile double transportVelocity;
      Machine(long p,int kind){this.kind=kind;tile=new TileEntity(1,World.ux(p),World.uy(p),World.uz(p));}
   }
   static int output(int kind,int input){return kind==Kinetics.PRESS?input==Items.IRON_INGOT?Kinetics.IRON_SHEET:input==Items.GOLD_INGOT?Kinetics.GOLD_SHEET:0:kind==Kinetics.MILL?input==Items.WHEAT?Kinetics.FLOUR:input==Blocks.COBBLE?Blocks.GRAVEL:input==Blocks.GRAVEL?Blocks.SAND:0:0;}
   static Machine get(World w,long p){return w.kinetics.machines.computeIfAbsent(p,k->new Machine(p,Kinetics.kind(Kinetics.block(w,p))));}
   static double progress(World w,long p){Machine m=w.kinetics.machines.get(p);return m==null?0:m.progress/6400.0;}
   static void open(Game g,long p){
      if(g.world.kinetics.saveBlocked){g.say("Back up/repair kinetics.dat before opening a machine; the corrupt file is preserved.");return;}
      g.openMenu(Menu.KINETIC,get(g.world,p).tile);
   }
   static void tick(World w){
      if(w.kinetics.saveBlocked)return;
      KineticTransport.tick(w);
      for(long p:w.kinetics.nodes){int block=Kinetics.block(w,p);if(!Kinetics.machine(block)||KineticTransport.carrier(block))continue;Machine m=get(w,p);if(m.kind==Kinetics.PRESS&&m.tile.slots[0]==null)continue;Stack input=m.tile.slots[0],out=m.tile.slots[2];int result=input==null?0:output(m.kind,input.id);
         if(input==null||m.inputId!=input.id){m.inputId=input==null?0:input.id;m.progress=0;}
         double rpm=Math.abs(Kinetics.reading(w,p).rpm());
         if(result==0||rpm==0||out!=null&&(out.id!=result||out.n>=out.max())){m.tile.cook=m.progress/32;continue;}
         m.progress+=Math.max(1,(int)Math.round(rpm));
         if(m.progress>=6400){m.progress-=6400;if(out==null)m.tile.slots[2]=new Stack(result,1);else out.n++;if(--input.n<=0)m.tile.slots[0]=null;}
         m.tile.cook=m.progress/32;
      }
   }
   static void removed(World w,long p){Machine m=w.kinetics.machines.remove(p);if(m==null)return;if(Net319.isClientConnected()&&!Net319.isHosting())return;for(Stack item:m.tile.slots)if(item!=null)w.dropItem(World.ux(p)+.5,World.uy(p)+.5,World.uz(p)+.5,item.id,item.n,item.dmg);
      if(w.game!=null&&w.game.menu!=null&&w.game.menu.kind==Menu.KINETIC&&w.game.menu.tile==m.tile)w.game.setScreen(Game.S.PLAY);
   }
   static void save(World w){
      if(w.dir==null||!w.saveEnabled||w.kinetics.saveBlocked)return;
      try{if(w.kinetics.machines.size()>16384)throw new IOException("Too many kinetic inventories");Path temp=w.dir.toPath().resolve("kinetics.dat.tmp");
         try(DataOutputStream out=new DataOutputStream(Files.newOutputStream(temp))){out.writeInt(0x42434B31);out.writeInt(w.kinetics.machines.size());for(var e:new TreeMap<>(w.kinetics.machines).entrySet()){Machine m=e.getValue();out.writeLong(e.getKey());out.writeByte(m.kind);out.writeInt(m.inputId);out.writeInt(m.progress);writeStack(out,m.tile.slots[0]);writeStack(out,m.tile.slots[2]);}}
         Files.move(temp,w.dir.toPath().resolve("kinetics.dat"),StandardCopyOption.REPLACE_EXISTING);
      }catch(IOException e){System.err.println("Kinetic inventories not saved: "+e);}
   }
   static void writeStack(DataOutputStream out,Stack s)throws IOException{out.writeInt(s==null?0:s.id);out.writeInt(s==null?0:s.n);out.writeInt(s==null?0:s.dmg);}
   static Stack readStack(DataInputStream in)throws IOException{int id=in.readInt(),n=in.readInt(),dmg=in.readInt();if(id==0&&n==0&&dmg==0)return null;if(id<=0||id>=Blocks.MAX||n<1||n>Items.maxStack(id)||dmg<0||dmg>65535||Items.name(id).equals("Unknown")||Items.name(id).startsWith("Item "))throw new IOException("Invalid kinetic inventory stack");return new Stack(id,n,dmg);}
   static void load(World w){
      if(w.dir==null)return;Path path=w.dir.toPath().resolve("kinetics.dat");if(!Files.exists(path))return;
      try{if(Files.size(path)>1024*1024)throw new IOException("Kinetic inventory file too large");Map<Long,Machine> valid=new HashMap<>();try(DataInputStream in=new DataInputStream(Files.newInputStream(path))){if(in.readInt()!=0x42434B31)throw new IOException("Kinetic inventory version");int count=in.readInt();if(count<0||count>16384)throw new IOException("Kinetic inventory count");for(int i=0;i<count;i++){long p=in.readLong();int kind=in.readUnsignedByte();if(kind!=Kinetics.PRESS&&kind!=Kinetics.MILL&&kind!=Kinetics.BELT&&kind!=Kinetics.DEPOT)throw new IOException("Unknown kinetic machine");Machine m=new Machine(p,kind);m.inputId=in.readInt();m.progress=in.readInt();if(m.progress<0||m.progress>=6400)throw new IOException("Kinetic progress");m.tile.slots[0]=readStack(in);m.tile.slots[2]=readStack(in);if(valid.put(p,m)!=null)throw new IOException("Duplicate kinetic inventory");}if(in.read()!=-1)throw new IOException("Trailing kinetic inventory data");}w.kinetics.machines.putAll(valid);
      }catch(IOException|RuntimeException e){w.kinetics.saveBlocked=true;System.err.println("Preserving corrupt kinetic inventory: "+e);}
   }
}
