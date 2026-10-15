package bc;

import java.io.IOException;
import java.util.*;

/** Builds a media machine; its editable ROM (not this builder) advances the cartridge. */
final class ShadowMachine {
   static String source(){return """
      ; Generic 20fps cartridge player. r10 frame, r11 length, r13 loops, r19 pause.
      LI 0 0x4d50
      OUT 7 0
      LI 10 0
      LI 13 0
      LI 19 1
      loop:
      IN 11 34
      IN 12 35
      IN 21 0
      MOV 0 21
      LI 31 64
      AND 0 31
      JZ 0 pause
      LI 10 0
      LI 13 0
      LI 19 0
      pause:
      MOV 0 21
      LI 31 48
      AND 0 31
      JZ 0 left
      LI 31 1
      XOR 19 31
      left:
      MOV 0 21
      LI 31 1
      AND 0 31
      JZ 0 right
      LI 31 100
      SUB 10 31
      LI 31 0
      JLT 31 10 right
      LI 10 0
      right:
      MOV 0 21
      LI 31 2
      AND 0 31
      JZ 0 bound
      LI 31 100
      ADD 10 31
      bound:
      JLT 10 11 draw
      MOV 10 11
      LI 31 1
      SUB 10 31
      draw:
      OUT 0 10
      OUT 1 11
      OUT 2 12
      OUT 3 19
      OUT 4 13
      LI 0 1
      SUB 0 19
      VIDEO 10 0
      JNZ 19 wait
      LI 31 1
      ADD 10 31
      JLT 10 11 wait
      LI 10 0
      ADD 13 31
      wait:
      WAIT 1
      JMP loop
      """;}
   static CircuitComputer.Plan plan(int ox,int y,int oz){
      CircuitComputer.Plan p=new CircuitComputer.Plan();
      for(int x=-23;x<=34;x++)for(int z=-3;z<=50;z++)p.put(ox+x,y-1,oz+z,Blocks.STONEBRICK);
      for(int x=0;x<=11;x++)for(int z=43;z<=49;z++)p.put(ox+x,y+4,oz+z,Blocks.GLASS);
      for(int x=0;x<16;x++)for(int h=0;h<12;h++)p.put(ox-20+x,y+7+h,oz,RasterComputer.PANEL+1);
      for(int x=0;x<32;x++)for(int h=0;h<24;h++){p.put(ox+x,y+h,oz,Redstone319.REDSTONE_LAMP);p.put(ox+x,y+h,oz-1,RasterComputer.PIXEL+1);}
      for(int x=-20;x<=2;x++)p.put(ox+x,y,oz+5,RasterComputer.CABLE);
      for(int z=0;z<=5;z++)p.put(ox-20,y,oz+z,RasterComputer.CABLE);
      for(int h=1;h<7;h++)p.put(ox-20,y+h,oz,RasterComputer.CABLE);
      for(int z=-1;z<=5;z++)p.put(ox-1,y,oz+z,RasterComputer.CABLE);
      for(int z=5;z<=45;z++)p.put(ox+2,y,oz+z,RasterComputer.CABLE);
      for(int h=1;h<=4;h++)p.put(ox+2,y+h,oz+45,RasterComputer.CABLE);
      p.put(ox+2,y+5,oz+45,RasterComputer.KEYS+1);p.put(ox+1,y+5,oz+45,RasterComputer.SPEAKER+1);
      p.put(ox-21,y+10,oz,RasterComputer.SPEAKER+1);
      int[] modules={RasterComputer.CPU|4096,RasterComputer.ROM,RasterComputer.RAM,RasterComputer.GPU,RasterComputer.LAMPS|4096,RasterComputer.CARTRIDGE};
      for(int i=0;i<modules.length;i++)p.put(ox-20+i*2,y,oz+6,modules[i]+1);
      for(int x=-23;x<=34;x++)for(int z:new int[]{-3,50})p.put(ox+x,y-1,oz+z,Blocks.GLOWSTONE);
      for(int i=0;i<5;i++)for(int x=12;x<=14;x++)p.put(ox+x,y+i-1,oz+39+i,Blocks.STONEBRICK);
      return p;
   }
   static void install(World w,int ox,int y,int oz){
      for(var e:plan(ox,y,oz).blocks.entrySet()){long k=e.getKey();w.setState(World.ux(k),World.uy(k),World.uz(k),e.getValue());}
      w.raster.roms.put(World.pack(ox-18,y,oz+6),RasterVM.assemble(source()));RasterMedia.install(w,World.pack(ox-10,y,oz+6),RasterMedia.BUILTIN);
   }
   static void build(Game g)throws IOException{
      if(!g.player.creative||Net319.isClientConnected()&&!Net319.isHosting()){g.say("/shadow requires a Creative host.");return;}
      World w=g.world;int ox=(int)Math.floor(g.player.x),oz=(int)Math.floor(g.player.z)-24,top=(int)Math.floor(g.player.y);
      for(int x=ox-24;x<=ox+35;x++)for(int z=oz-4;z<=oz+51;z++){Chunk c=w.chunkAt(x,z);if(c==null){g.say("Let nearby chunks load (view distance 5+) and retry /shadow.");return;}top=Math.max(top,c.hm[(z&15)*16+(x&15)]);}
      int y=top+3;if(y>230){g.say("Not enough overhead space.");return;}
      for(long k:plan(ox,y,oz).blocks.keySet())if(w.getBlock(World.ux(k),World.uy(k),World.uz(k))!=0){g.say("Cancelled: existing blocks would be replaced.");return;}
      RasterMedia.clip(w,RasterMedia.BUILTIN);install(w,ox,y,oz);RasterComputer.exportExamples(g);
      g.player.setPos(ox+5.5,y+5,oz+46.5);g.player.yaw=192;g.player.pitch=-8;g.player.flying=false;
      g.say("SHADOW SIGNAL: original silhouette film + synthesized score, not Bad Apple. Right-click left keypad, then Enter.");
      g.say("Space/P pauses; arrows seek 5s; Enter restarts; M mutes; Escape leaves controls. 64x48 panels + 32x24 lamps.");
   }
   static void upgrade(Game g){
      if(!g.player.creative||Net319.isClientConnected()&&!Net319.isHosting()||g.target==null){g.say("Creative host: look at an existing Block Drop component first.");return;}
      World w=g.world;RasterComputer.Network n=RasterComputer.network(w,World.pack(g.target.x,g.target.y,g.target.z));
      if(n==null||n.nc!=1||n.nr!=1||n.inputs.isEmpty()){g.say("Look at a complete Block Drop circuit.");return;}int[] old=w.raster.roms.get(n.rom);
      if(!Arrays.equals(old,RasterVM.assemble(RasterPrograms.fallingSource(false)))&&!Arrays.equals(old,RasterPrograms.falling())){g.say("Refusing to overwrite a custom ROM. Import FallingBlocks-Sound.asm yourself if intended.");return;}
      if(n.speakers.isEmpty()){
         long key=n.inputs.get(0);boolean placed=false;for(int[] d:Circuit.DIR){int x=World.ux(key)+d[0],y=World.uy(key)+d[1],z=World.uz(key)+d[2];if(y>=0&&y<256&&w.isLoaded(x,z)&&w.getBlock(x,y,z)==0){w.setState(x,y,z,RasterComputer.SPEAKER+1);placed=true;break;}}
         if(!placed){g.say("Make one air space next to the keypad for a speaker, then retry.");return;}
      }
      w.raster.roms.put(n.rom,RasterPrograms.falling());w.raster.cpus.computeIfAbsent(n.cpu,k->new RasterVM.Cpu()).reset();
      w.setState(World.ux(n.cpu),World.uy(n.cpu),World.uz(n.cpu),w.getState(World.ux(n.cpu),World.uy(n.cpu),World.uz(n.cpu))|4096);
      g.say("Installed sound-enabled ROM and speaker. Game reset; press Enter at the keypad. Existing custom ROMs are never replaced.");
   }
   static void smoke(Game g,int frame){
      if(frame==80){g.runCommand("/shadow");if(g.world.raster.nodes.size()<900)throw new IllegalStateException("Shadow machine failed to build");}
      if(frame==140){for(long k:g.world.raster.nodes)if(RasterComputer.kindSafe(g.world,k)==5){RasterComputer.use(g,World.ux(k),World.uy(k),World.uz(k),RasterComputer.KEYS+1);g.onKey(257,1,0);break;}}
      if(frame==180){g.onKey(262,1,0);g.onKey(262,0,0);}
      if(frame==220&&"panel".equals(System.getProperty("bc.shadow"))){RasterComputer.Network n=RasterComputer.network(g.world,g.rasterFocus.key);int[] b=RasterComputer.bounds(g.world,n.screens);long k=n.screens.get(0);g.player.setPos((b[0]+b[1]+1)/2.0,(b[2]+b[3]+1)/2.0-1.6,World.uz(k)+18);g.player.yaw=180;g.player.pitch=0;g.player.flying=true;}
      if(frame>140&&frame%60==0){RasterComputer.Network n=RasterComputer.network(g.world,g.rasterFocus.key);RasterVM.Cpu c=g.world.raster.cpus.get(n.cpu);if(!n.error.isEmpty()||!c.fault.isEmpty())throw new IllegalStateException(n.error+" "+c.fault);System.out.println("[shadow-smoke] panels="+n.screens.size()+" lamps="+n.lamps.size()+" frame="+c.out[0]+" speakers="+n.speakers.size());}
   }
}
