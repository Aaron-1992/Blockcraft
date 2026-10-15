package bc;

import java.io.IOException;
import java.util.*;

/** Only constructs blocks and installs an ordinary ROM. No game logic runs in this builder. */
final class RasterArcade {
   static CircuitComputer.Plan plan(int ox,int y,int oz){
      CircuitComputer.Plan p=new CircuitComputer.Plan();
      for(int x=-4;x<=27;x++)for(int z=-3;z<=35;z++)p.put(ox+x,y-1,oz+z,Blocks.STONEBRICK);
      for(int x=9;x<=17;x++)for(int z=28;z<=34;z++)p.put(ox+x,y+9,oz+z,Blocks.GLASS);
      for(int x=0;x<4;x++)for(int h=0;h<7;h++)p.put(ox+x,y+10+h,oz,RasterComputer.PANEL+1);
      for(int x=0;x<16;x++)for(int h=0;h<28;h++){
         p.put(ox+8+x,y+h,oz,Redstone319.REDSTONE_LAMP);
         p.put(ox+8+x,y+h,oz-1,RasterComputer.PIXEL+1);
      }
      for(int x=0;x<=9;x++)p.put(ox+x,y,oz+5,RasterComputer.CABLE);
      for(int z=0;z<=5;z++)p.put(ox,y,oz+z,RasterComputer.CABLE);
      for(int h=1;h<10;h++)p.put(ox,y+h,oz,RasterComputer.CABLE);
      for(int z=-1;z<=5;z++)p.put(ox+7,y,oz+z,RasterComputer.CABLE);
      for(int z=5;z<=30;z++)p.put(ox+9,y,oz+z,RasterComputer.CABLE);
      for(int h=1;h<=9;h++)p.put(ox+9,y+h,oz+30,RasterComputer.CABLE);
      p.put(ox+9,y+10,oz+30,RasterComputer.KEYS+1);
      int[] modules={RasterComputer.CPU|4096,RasterComputer.ROM,RasterComputer.RAM,RasterComputer.GPU,RasterComputer.LAMPS};
      for(int i=0;i<modules.length;i++)p.put(ox+i*2,y,oz+6,modules[i]+1);
      // Stairs to the observation deck, plus lit floor trim. No decoration carries data.
      for(int i=0;i<10;i++)for(int x=6;x<=8;x++)p.put(ox+x,y+i-1,oz+19+i,Blocks.STONEBRICK);
      for(int x=-4;x<=27;x++)for(int z:new int[]{-3,35})p.put(ox+x,y-1,oz+z,Blocks.GLOWSTONE);
      return p;
   }
   static void install(World w,int ox,int y,int oz){for(var e:plan(ox,y,oz).blocks.entrySet()){long k=e.getKey();w.setState(World.ux(k),World.uy(k),World.uz(k),e.getValue());}w.raster.roms.put(World.pack(ox+2,y,oz+6),RasterPrograms.falling());}
   static void build(Game g)throws IOException{
      if(!g.player.creative){g.say("/arcade requires Creative mode; the individual raster blocks can also be crafted.");return;}
      if(Net319.isClientConnected()&&!Net319.isHosting()){g.say("Ask the host to build the arcade.");return;}
      World w=g.world;int ox=(int)Math.floor(g.player.x)-12,oz=(int)Math.floor(g.player.z)-16,top=(int)Math.floor(g.player.y);
      for(int x=ox-5;x<=ox+28;x++)for(int z=oz-4;z<=oz+36;z++){Chunk c=w.chunkAt(x,z);if(c==null){g.say("Let nearby chunks load (view distance 5+) and try /arcade again.");return;}top=Math.max(top,c.hm[(z&15)*16+(x&15)]);}
      int y=top+3;if(y>225){g.say("Not enough room overhead. Try lower terrain.");return;}
      for(long k:plan(ox,y,oz).blocks.keySet())if(w.getBlock(World.ux(k),World.uy(k),World.uz(k))!=0){g.say("Cancelled: arcade would replace existing blocks.");return;}
      install(w,ox,y,oz);export(g);g.player.setPos(ox+12.5,y+10,oz+31.5);g.player.yaw=180;g.player.pitch=-6;g.player.flying=false;
      g.say("BLOCK DROP: right-click the keypad on your left, then Enter to start. Escape leaves controls.");
      g.say("4x7 display panels = 16x28 pixels; 448 real lamps mirror the same ROM program. See Raster-guide.txt.");
   }
   static void smoke(Game g,int frame){
      if(frame==80){g.runCommand("/arcade");if(g.world.raster.nodes.size()<450)throw new IllegalStateException("Arcade fixture did not build");}
      if(frame==140){
         for(long key:new ArrayList<>(g.world.raster.nodes))if(RasterComputer.kindSafe(g.world,key)==5){RasterComputer.use(g,World.ux(key),World.uy(key),World.uz(key),RasterComputer.KEYS+1);g.onKey(257,1,0);break;}
      }
      if(frame==180||frame==240||frame==300)g.onKey(32,1,0);
      if(frame==210)g.onKey(263,1,0);if(frame==211)g.onKey(263,0,0);
      if(frame==340){g.onKey(80,1,0);if("panel".equals(System.getProperty("bc.arcade"))){RasterComputer.Network n=RasterComputer.network(g.world,g.rasterFocus.key);int[] b=RasterComputer.bounds(g.world,n.screens);long p=n.screens.get(0);g.player.setPos((b[0]+b[1]+1)/2.0,b[2]+1.9,World.uz(p)+10);g.player.yaw=180;g.player.pitch=0;g.player.flying=true;}}
      if(frame>140&&frame%60==0){RasterComputer.Network n=RasterComputer.network(g.world,g.rasterFocus.key);RasterVM.Cpu c=g.world.raster.cpus.get(n.cpu);if(!c.fault.isEmpty()||!n.error.isEmpty())throw new IllegalStateException("Arcade smoke: "+n.error+" "+c.fault);System.out.println("[arcade-smoke] frame="+frame+" panels="+n.screens.size()+" lamps="+n.lamps.size()+" score="+c.out[0]+" lines="+c.out[1]);}
   }
   static void export(Game g)throws IOException{RasterComputer.exportExamples(g);}
}
