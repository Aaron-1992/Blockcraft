package bc;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

/** V6.10 native saves. Reloads the actual chunks, CPU state and cartridge map before checking them. */
public final class BuildSoundWorlds {
   static int checks;
   static void check(boolean yes,String text){if(!yes)throw new AssertionError(text);checks++;}
   static WorldSettings settings(){WorldSettings s=new WorldSettings();s.type=WorldSettings.FLAT;s.flat=0;s.structures=false;s.bonusChest=false;return s;}
   static World fresh(Path path,WorldSettings s){World w=new World(6100610L,path.toFile(),0,s);w.weather.enabled=false;w.disasters.enabled=false;w.fire.spread=false;w.trees.enabled=false;w.dayTime=6000;return w;}
   static void ticks(World w,int count){for(int i=0;i<count;i++)w.tick();}
   static void build(Path path,boolean movie)throws Exception{
      if(Files.exists(path.resolve("level.properties")))throw new IOException("Refusing to replace "+path);
      WorldSettings settings=settings();World w=fresh(path,settings);int radius=movie?4:3;
      for(int z=-radius;z<=radius;z++)for(int x=-radius;x<=radius;x++){Chunk c=w.gen.generate(x,z);w.chunks.put(World.key(x,z),c);w.lightChunk(c);}
      if(movie)ShadowMachine.install(w,0,72,0);else RasterArcade.install(w,-8,72,0);ticks(w,12);
      long cp=World.pack(movie?-20:-8,72,6);RasterVM.Cpu cpu=w.raster.cpus.get(cp);check(cpu!=null&&cpu.fault.isEmpty(),"ROM booted");check(cpu.out[3]==1,"starts paused");
      Properties p=new Properties();settings.save(p);p.setProperty("name",movie?"Shadow Signal - Original Silhouette Cinema":"Block Drop - Sound Edition");p.setProperty("seed","6100610");p.setProperty("dim","0");
      p.setProperty("x",movie?"5.5":"4.5");p.setProperty("y",movie?"77":"82");p.setProperty("z",movie?"46.5":"31.5");p.setProperty("yaw",movie?"192":"186.3");p.setProperty("pitch",movie?"-8":"-4.3");
      p.setProperty("mode","1");p.setProperty("creative","true");p.setProperty("flying","false");p.setProperty("health","20");p.setProperty("food","20");p.setProperty("sat","5");p.setProperty("time","6000");p.setProperty("spawnx",movie?"5":"4");p.setProperty("spawnz",movie?"46":"31");p.setProperty("sel","0");p.setProperty("firespread","false");p.setProperty("treefall","false");w.weather.save(p);w.disasters.save(p);
      StringBuilder inv=new StringBuilder("0:0:0;");ArrayList<Integer> ids=new ArrayList<>();RasterComputer.creative(ids);for(int id:ids)inv.append(id).append(":64:0;");inv.append(Redstone319.REDSTONE_LAMP).append(":64:0;");p.setProperty("inv",inv.toString());p.setProperty("armor","");
      try(OutputStream out=Files.newOutputStream(path.resolve("level.properties"))){p.store(out,"Blockcraft V6.10 sound machines");}
      for(Chunk c:w.chunks.values()){c.modified=true;w.saveChunk(c);}w.saveAll();w.shutdown();
      verify(path,movie);
   }
   static void verify(Path path,boolean movie)throws Exception{
      Properties p=new Properties();try(InputStream in=Files.newInputStream(path.resolve("level.properties"))){p.load(in);}
      int radius=movie?4:3;long cp=World.pack(movie?-20:-8,72,6);
      Map<Path,byte[]> before=new HashMap<>();try(var files=Files.list(path)){for(Path f:files.filter(Files::isRegularFile).toList())before.put(f,Files.readAllBytes(f));}
      World v=fresh(path,WorldSettings.load(p));v.saveEnabled=false;
      for(int z=-radius;z<=radius;z++)for(int x=-radius;x<=radius;x++){Chunk c=new Chunk(x,z);try(DataInputStream in=new DataInputStream(new GZIPInputStream(Files.newInputStream(path.resolve("c."+x+"."+z+".gz"))))){Chunk.readBlocks(in,c.blocks);}c.computeHeightmap();v.chunks.put(World.key(x,z),c);v.lightChunk(c);}
      for(Chunk c:v.chunks.values()){RasterComputer.loaded(v,c);RedstoneLogic.loaded(v,c);}ticks(v,6);
      Game g=new Game();g.world=v;v.game=g;g.player=new Player();g.player.setMode(1);g.player.setPos(Double.parseDouble(p.getProperty("x")),Double.parseDouble(p.getProperty("y")),Double.parseDouble(p.getProperty("z")));g.screen=Game.S.PLAY;RasterAudio.listener(g);
      RasterComputer.Network n=RasterComputer.network(v,cp);RasterVM.Cpu cpu=v.raster.cpus.get(cp);check(n.error.isEmpty(),"reloaded physical network");check(cpu.out[3]==1&&cpu.fault.isEmpty(),"saved CPU paused without fault");check(n.speakers.size()==(movie?2:1),"speaker hardware saved");check(Arrays.equals(v.raster.roms.get(n.rom),movie?RasterVM.assemble(ShadowMachine.source()):RasterPrograms.falling()),"native ROM matches the released editable source");
      if(movie){check(n.screens.size()==192&&n.lamps.size()==768,"64x48 panels and 32x24 lamps");check(RasterMedia.cartridge(v,n)!=null,"saved cartridge reference loads actual clip");}else check(n.screens.size()==28&&n.lamps.size()==448,"Block Drop screens");
      long key=World.pack(movie?2:1,movie?77:82,movie?45:30);RasterComputer.input(v,key,0,64);ticks(v,1);check(cpu.out[3]==0,"Enter starts");
      if(movie){
         double energy=0;for(int frame=0;frame<20;frame++){float[] l=new float[2205],r=new float[2205];RasterAudio.mix(l,r,2205,1);for(float f:l)energy+=f*f;ticks(v,1);}check(energy>1,"real speaker mixer emits original PCM");check(cpu.out[0]==20,"synchronized frame clock");
         RasterComputer.input(v,key,0,32);ticks(v,1);check(RasterAudio.streams.isEmpty(),"pause silences soundtrack");int pos=cpu.out[0];RasterComputer.input(v,key,0,2);ticks(v,6);check(cpu.out[0]==pos+100,"seek while paused");
         RasterClip clip=RasterMedia.cartridge(v,n);for(int y=0;y<48;y++)for(int x=0;x<64;x++){int wanted=clip.pixel(cpu.out[0],x,y);long tile=World.pack(-20+x/4,90-y/4,0);int value=(int)(v.raster.pixels.getOrDefault(tile,0L)>>>(((y%4)*4+x%4)*4))&15;check(wanted==value,"native panel pixel");}
         for(int y=0;y<24;y++)for(int x=0;x<32;x++)check(v.getBlock(x,95-y,0)==(clip.pixel(cpu.out[0],x*2,y*2)==0?Redstone319.REDSTONE_LAMP:Redstone319.REDSTONE_LAMP_LIT),"native lamp pixel");
      }else{
         RasterComputer.input(v,key,0,16);ticks(v,2);int cells=0;for(int i=0;i<200;i++)if(v.raster.rams.get(n.ram)[i]!=0)cells++;check(cells==4,"real hard drop");check(!RasterAudio.tones.isEmpty(),"ROM emitted real sound event");
      }
      int pc=cpu.pc;v.setBlock(movie?-19:-7,72,5,0);ticks(v,2);check(cpu.pc==pc,"cut required bus stops CPU");check(RasterAudio.streams.isEmpty(),"cut bus stops streaming audio");v.shutdown();
      for(var e:before.entrySet())check(Arrays.equals(e.getValue(),Files.readAllBytes(e.getKey())),"verification did not mutate distributed save");
      System.out.println("Verified "+path.getFileName()+": "+((radius*2+1)*(radius*2+1))+" native chunks, "+n.speakers.size()+" speaker(s).");
   }
   public static void main(String[] args)throws Exception{
      if(!Game.VERSION_STR.equals("6.10"))throw new IOException("Compile against V6.10");Path bundle=Path.of(args[0]);Files.createDirectories(bundle);
      if(args.length>1&&args[1].equals("--verify")){verify(bundle.resolve("Shadow_Signal_V610"),true);verify(bundle.resolve("Block_Drop_Sound_V610"),false);}
      else {build(bundle.resolve("Shadow_Signal_V610"),true);build(bundle.resolve("Block_Drop_Sound_V610"),false);
      Files.writeString(bundle.resolve("FallingBlocks-Sound.asm"),RasterPrograms.fallingSource());Files.writeString(bundle.resolve("MediaPlayer.asm"),ShadowMachine.source());}
      System.out.println("PASS "+checks+" native-save checks, including actual PCM mixing. Original animation/music, not the Bad Apple recording.");
   }
}
