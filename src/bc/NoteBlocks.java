package bc;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Material-selected world note instruments. MidiPlayback reuses this same synthesized sound bank. */
final class NoteBlocks {
   static final String[] NAMES={"harp","bass","snare","hat","basedrum","flute","bell","guitar","chime","xylophone","iron_xylophone","cow_bell","didgeridoo","bit","banjo","pling"};
   static final int[] LOW={54,30,54,54,54,66,78,42,78,78,54,66,30,54,54,54};
   static final int[] MATERIAL={Blocks.DIRT,Blocks.PLANKS,Blocks.SAND,Blocks.GLASS,Blocks.STONE,Blocks.CLAY,Blocks.GOLD_BLOCK,Blocks.WOOL0,Blocks313.PACKED_ICE,Redstone319.BONE_BLOCK,Blocks.IRON_BLOCK,Blocks.SOUL_SAND,Blocks.PUMPKIN,Blocks.EMERALD_BLOCK,Blocks.HAY_BALE,Blocks.GLOWSTONE};
   static int instrument(int id){
      if(id==Blocks.CLAY)return 5;if(id==Blocks.GOLD_BLOCK)return 6;if(id>=Blocks.WOOL0&&id<Blocks.WOOL0+16)return 7;
      if(id==Blocks313.PACKED_ICE)return 8;if(id==Redstone319.BONE_BLOCK)return 9;if(id==Blocks.IRON_BLOCK)return 10;
      if(id==Blocks.SOUL_SAND)return 11;if(id==Blocks.PUMPKIN||id==Blocks.CARVED_PUMPKIN||id==Blocks.JACK_O_LANTERN)return 12;
      if(id==Blocks.EMERALD_BLOCK)return 13;if(id==Blocks.HAY_BALE)return 14;if(id==Blocks.GLOWSTONE)return 15;
      if(id==Blocks.SAND||id==Blocks.GRAVEL||id==Blocks313.RED_SAND||id>=Blocks.POWDER0&&id<Blocks.POWDER0+16)return 2;
      if(id==Blocks.GLASS||id>=Blocks.GLASS0&&id<Blocks.GLASS0+16)return 3;
      if(id>0&&Blocks.mineClass[id]==Items.C_AXE)return 1;
      if(id>0&&Blocks.mineClass[id]==Items.C_PICK)return 4;
      return 0;
   }
   static int pitch(World w,int x,int y,int z){return w.redstone.notes.getOrDefault(World.pack(x,y,z),0);}
   static void tune(World w,int x,int y,int z,int pitch){
      if(pitch<0||pitch>24)throw new IllegalArgumentException("Note pitch must be 0..24");
      if(w.getBlock(x,y,z)!=Redstone319.NOTE_BLOCK)throw new IllegalArgumentException("Not a note block");
      w.redstone.notes.put(World.pack(x,y,z),pitch);
   }
   static void play(World w,int x,int y,int z){
      if(w==null||w.getBlock(x,y,z)!=Redstone319.NOTE_BLOCK||w.getBlock(x,y+1,z)!=0)return;
      w.redstone.noteHits.put(World.pack(x,y,z),w.tickCount);
      SoundEngine.playNoteBlock(w,x,y,z,pitch(w,x,y,z));
      Net319.rasterBroadcast(w,4,World.pack(x,y,z),instrument(w.getBlock(x,y-1,z))+","+pitch(w,x,y,z));
      if(w.game!=null&&w.game.particles!=null)w.game.spawnBreakParticles(x,y+1,z,Redstone319.REDSTONE_TORCH,4);
   }
   static void receive(World w,long key,String data){try{
      String[] p=data.split(",");if(p.length!=2||w.getBlock(World.ux(key),World.uy(key),World.uz(key))!=Redstone319.NOTE_BLOCK)return;
      int instrument=Integer.parseInt(p[0]),pitch=Integer.parseInt(p[1]);if(instrument<0||instrument>=NAMES.length||pitch<0||pitch>24)return;
      SoundEngine.play3D(w,"note_"+NAMES[instrument],World.ux(key)+.5,World.uy(key)+.5,World.uz(key)+.5,1,(float)Math.pow(2,(pitch-12)/12.0));
   }catch(RuntimeException ignored){}}
   static short[] samples(int type,int rate){
      double duration=type==3?.16:type==2||type==4?.35:1.1;short[] out=new short[(int)(rate*duration)];
      double f=440*Math.pow(2,(LOW[type]+12-69)/12.0),lp=0;Random rng=new Random(611+type);
      for(int i=0;i<out.length;i++){
         double t=i/(double)rate,p=2*Math.PI*f*t,noise=rng.nextDouble()*2-1;lp+=.12*(noise-lp);
         double v=switch(type){
            case 1 -> Math.sin(p)*.8+Math.sin(2*p)*.2;
            case 2 -> noise*.72+Math.sin(2*Math.PI*185*t)*.28;
            case 3 -> (noise-lp)*.65;
            case 4 -> Math.sin(2*Math.PI*(55*t+1.5*(1-Math.exp(-35*t))));
            case 5 -> Math.sin(p+Math.sin(t*32)*.035)+.08*Math.sin(2*p);
            case 6 -> .6*Math.sin(p)+.25*Math.sin(2.76*p)+.15*Math.sin(5.4*p);
            case 7 -> .65*Math.sin(p)+.25*Math.sin(2*p)*Math.exp(-7*t)+.1*Math.sin(3*p);
            case 8 -> .6*Math.sin(p)+.25*Math.sin(2*p)+.15*Math.sin(4.01*p);
            case 9 -> .7*Math.sin(p)+.2*Math.sin(3*p)+.1*Math.sin(7*p);
            case 10 -> .5*Math.sin(p)+.3*Math.sin(2.76*p)+.2*Math.sin(5.4*p);
            case 11 -> .55*Math.sin(p)+.45*Math.sin(1.48*p);
            case 12 -> .65*Math.sin(p)+.25*Math.sin(3*p)+.1*Math.sin(5*p);
            case 13 -> .75*Math.sin(p)+.18*Math.sin(3*p)+.07*Math.sin(5*p);
            case 14 -> .55*Math.sin(p)+.3*Math.sin(2*p)+.15*Math.sin(4*p);
            case 15 -> .6*Math.sin(p)+.3*Math.sin(2*p)+.1*Math.sin(6*p);
            default -> .65*Math.sin(p)+.25*Math.sin(2*p)*Math.exp(-10*t)+.1*Math.sin(3*p);
         };
         double decay=type==3?35:type==2?18:type==4?14:type==9||type==14?9:type==5||type==12?3:5;
         double env=Math.min(1,i/(rate*.003))*Math.exp(-decay*t)*Math.min(1,(out.length-1-i)/(rate*.025));
         out[i]=(short)(Math.max(-1,Math.min(1,v))*env*18000);
      }return out;
   }
   static void changed(World w,int x,int y,int z,int old,int next){if(old==Redstone319.NOTE_BLOCK&&next!=old){long k=World.pack(x,y,z);w.redstone.notes.remove(k);w.redstone.noteHits.remove(k);w.redstone.opened.remove(Redstone319.pack(x,y,z));}}
   static void save(World w){
      if(w.dir==null||!w.saveEnabled||w.redstone.notesSaveBlocked)return;
      try{Properties p=new Properties();p.setProperty("version","1");for(var e:w.redstone.notes.entrySet())p.setProperty(Long.toString(e.getKey()),Integer.toString(e.getValue()));
         Path tmp=w.dir.toPath().resolve("notes.properties.tmp");try(OutputStream out=Files.newOutputStream(tmp)){p.store(out,"Note Block tuning (0=F# base, 24=two octaves)");}Files.move(tmp,w.dir.toPath().resolve("notes.properties"),StandardCopyOption.REPLACE_EXISTING);
      }catch(IOException e){System.err.println("Note tuning not saved: "+e);}
   }
   static void load(World w){
      if(w.dir==null)return;Path f=w.dir.toPath().resolve("notes.properties");if(!Files.exists(f))return;
      try{if(Files.size(f)>1024*1024)throw new IOException("Note map too large");Properties p=new Properties();try(InputStream in=Files.newInputStream(f)){p.load(in);}if(!"1".equals(p.getProperty("version")))throw new IOException("Note map version");Map<Long,Integer> valid=new HashMap<>();
         for(String k:p.stringPropertyNames())if(!k.equals("version")){int v=Integer.parseInt(p.getProperty(k));long key=Long.parseLong(k);if(v<0||v>24)throw new IOException("Note pitch");valid.put(key,v);}w.redstone.notes.putAll(valid);
      }catch(IOException|RuntimeException e){w.redstone.notesSaveBlocked=true;System.err.println("Preserving corrupt note tuning: "+e);}
   }
}
