package bc;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

/** Creates and independently reloads the shipped native save using the production codec and simulator. */
public final class BuildRasterWorld {
   static int checks;
   static void check(boolean yes,String text){if(!yes)throw new AssertionError(text);checks++;}
   static World fresh(Path path,WorldSettings settings){World w=new World(690069L,path.toFile(),0,settings);w.weather.enabled=false;w.disasters.enabled=false;w.fire.spread=false;w.trees.enabled=false;w.dayTime=6000;return w;}
   static void ticks(World w,int n){for(int i=0;i<n;i++)w.tick();}
   public static void main(String[] args)throws Exception{
      Path path=Path.of(args[0]);if(Files.exists(path.resolve("level.properties")))throw new IOException("Refusing to overwrite an existing world");
      WorldSettings settings=new WorldSettings();settings.type=WorldSettings.FLAT;settings.flat=0;settings.structures=false;settings.bonusChest=false;
      World w=fresh(path,settings);for(int z=-3;z<=3;z++)for(int x=-3;x<=3;x++){Chunk c=w.gen.generate(x,z);w.chunks.put(World.key(x,z),c);w.lightChunk(c);}
      RasterArcade.install(w,-8,72,0);ticks(w,12);long cp=World.pack(-8,72,6);RasterComputer.Network n=RasterComputer.network(w,cp);
      check(n.error.isEmpty(),"complete connected hardware");check(n.screens.size()==28&&n.lamps.size()==448,"physical displays");check(w.raster.cpus.get(cp).out[3]==1,"starts paused");
      Properties p=new Properties();settings.save(p);p.setProperty("name","Block Drop - Lamp Arcade and Modular Displays");p.setProperty("seed","690069");p.setProperty("dim","0");
      p.setProperty("x","4.5");p.setProperty("y","82");p.setProperty("z","31.5");p.setProperty("yaw","186.3");p.setProperty("pitch","-4.3");
      p.setProperty("mode","1");p.setProperty("creative","true");p.setProperty("flying","false");p.setProperty("health","20");p.setProperty("food","20");p.setProperty("sat","5");p.setProperty("time","6000");p.setProperty("spawnx","4");p.setProperty("spawnz","31");p.setProperty("sel","0");p.setProperty("firespread","false");p.setProperty("treefall","false");w.weather.save(p);w.disasters.save(p);
      StringBuilder inv=new StringBuilder("0:0:0;");ArrayList<Integer> ids=new ArrayList<>();RasterComputer.creative(ids);for(int id:ids)inv.append(id).append(":64:0;");inv.append(Redstone319.REDSTONE_LAMP).append(":64:0;");p.setProperty("inv",inv.toString());p.setProperty("armor","");
      try(OutputStream out=Files.newOutputStream(path.resolve("level.properties"))){p.store(out,"Blockcraft V6.9 - actual programmable raster hardware");}
      for(Chunk c:w.chunks.values()){c.modified=true;w.saveChunk(c);}w.saveAll();w.shutdown();
      Files.writeString(path.getParent().resolve("FallingBlocks.asm"),RasterPrograms.fallingSource());
      // Reload from disk, not from the builder's maps. Verification never writes the shipped save.
      World v=fresh(path,WorldSettings.load(p));v.saveEnabled=false;
      for(int z=-3;z<=3;z++)for(int x=-3;x<=3;x++){Chunk c=new Chunk(x,z);try(DataInputStream in=new DataInputStream(new GZIPInputStream(Files.newInputStream(path.resolve("c."+x+"."+z+".gz"))))){Chunk.readBlocks(in,c.blocks);}c.computeHeightmap();v.chunks.put(World.key(x,z),c);v.lightChunk(c);}
      for(Chunk c:v.chunks.values()){RasterComputer.loaded(v,c);RedstoneLogic.loaded(v,c);}ticks(v,10);
      RasterVM.Cpu cpu=v.raster.cpus.get(cp);n=RasterComputer.network(v,cp);check(n.error.isEmpty(),"reloaded network");check(cpu.fault.isEmpty()&&cpu.out[3]==1,"saved CPU resumes paused without faults");check(Arrays.equals(v.raster.roms.get(n.rom),RasterPrograms.falling()),"saved ROM matches editable source");
      for(int y=0;y<28;y++)for(int x=0;x<16;x++){
         int value=cpu.frame[y*64+x]&15;check(v.getBlock(x,99-y,0)==(value==0?Redstone319.REDSTONE_LAMP:Redstone319.REDSTONE_LAMP_LIT),"real lamp wall matches saved framebuffer");
         long tile=World.pack(-8+x/4,88-y/4,0);int pixel=(int)(v.raster.pixels.getOrDefault(tile,0L)>>>(((y%4)*4+x%4)*4))&15;check(pixel==value,"modular screen matches saved framebuffer");
      }
      long key=World.pack(1,82,30);RasterComputer.input(v,key,0,64);ticks(v,1);check(cpu.out[3]==0,"Enter starts game");
      RasterComputer.input(v,key,0,16);ticks(v,2);int occupied=0;int[] ram=v.raster.rams.get(n.ram);for(int i=0;i<200;i++)if(ram[i]!=0)occupied++;check(occupied==4,"hard drop writes actual RAM");
      int pc=cpu.pc;v.setBlock(-7,72,5,0);ticks(v,40);check(cpu.pc==pc,"cut bus stops execution");v.setBlock(-7,72,5,RasterComputer.CABLE);ticks(v,2);check(RasterComputer.network(v,cp).error.isEmpty(),"repair reconnects processor");
      RasterComputer.input(v,key,0,64);ticks(v,1);check(cpu.out[0]==0&&cpu.out[1]==0,"restart resets score and lines");v.shutdown();
      System.out.println("PASS "+checks+" native-save checks: 49 chunks, 28 modular display panels, 448 redstone lamps, separate CPU/ROM/RAM/GPU/keypad, persistence, play and cut-wire behavior.");
      System.out.println("The verified save remains paused and unchanged on disk. Requires Blockcraft V6.9 or later.");
   }
}
