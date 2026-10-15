package bc;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Media cartridges hold data, while an ordinary ROM controls playback and seeking. */
final class RasterMedia {
   static final String BUILTIN="shadow-signal";
   static final Map<String,RasterClip> cache=Collections.synchronizedMap(new LinkedHashMap<>(8,.75F,true){protected boolean removeEldestEntry(Map.Entry<String,RasterClip> e){return size()>8;}});
   static final class State {final Map<Long,String> slots=new HashMap<>();final Map<String,RasterClip> imported=new HashMap<>();int tones;boolean saveBlocked;final Set<String> missing=new HashSet<>();}
   static RasterClip clip(World w,String name)throws IOException{
      RasterClip c=w.raster.media.imported.get(name);if(c!=null)return c;c=cache.get(name);if(c!=null)return c;
      byte[] bytes;
      if(name.equals(BUILTIN)){
         try(InputStream in=RasterMedia.class.getResourceAsStream("/media/shadow-signal.bcv")){bytes=in==null?Files.readAllBytes(Path.of("res/media/shadow-signal.bcv")):in.readNBytes(RasterClip.MAX_BYTES+1);}
      }else{
         if(!name.matches("[a-f0-9]{64}"))throw new IOException("Invalid cartridge reference");
         Path file=w.dir==null?null:w.dir.toPath().resolve("media").resolve(name+".bcv");
         if(file==null||!Files.isRegularFile(file)){if(w.game==null)throw new IOException("Missing media file");file=w.game.root.toPath().resolve("media").resolve(name+".bcv");}
         if(Files.size(file)>RasterClip.MAX_BYTES)throw new IOException("Media file too large");bytes=Files.readAllBytes(file);
      }
      c=RasterClip.read(bytes);if(!name.equals(BUILTIN)&&!name.equals(c.hash))throw new IOException("Media hash mismatch");cache.put(name,c);return c;
   }
   static RasterClip cartridge(World w,RasterComputer.Network n){if(n.cartridges.size()!=1)return null;String name=w.raster.media.slots.get(n.cartridges.get(0));if(name==null)return null;try{return clip(w,name);}catch(IOException e){return null;}}
   static String reference(World w,RasterComputer.Network n){return n.cartridges.size()==1?w.raster.media.slots.get(n.cartridges.get(0)):null;}
   static int info(World w,RasterComputer.Network n,int port){RasterClip c=cartridge(w,n);if(c==null)return 0;return switch(port){case 32->c.width;case 33->c.height;case 34->c.frames;case 35->RasterClip.FPS;default->0;};}
   static boolean requiresMedia(int[] code){for(int i=0;i<code.length;i+=4)if(code[i]==34)return true;return false;}
   static float volume(World w,long k){int raw=w.getState(World.ux(k),World.uy(k),World.uz(k));return (raw&4096)!=0?0:(raw&8192)!=0?.3F:.7F;}
   static void tone(World w,RasterComputer.Network n,int note,int ms,int spec){
      if(w.raster.media.tones++>=32)return;
      for(long key:n.speakers.stream().sorted().limit(8).toList()){
         float v=volume(w,key);if(v<=0)continue;
         String data=Math.max(24,Math.min(108,note))+","+Math.max(20,Math.min(1000,ms))+","+(spec&0xf03)+","+v;
         receive(w,2,key,data);Net319.rasterBroadcast(w,2,key,data);
      }
   }
   static void video(World w,RasterComputer.Network n,RasterVM.Cpu cpu,int frame,int flags){
      RasterClip c=cartridge(w,n);if(c==null)throw new IllegalArgumentException("Connect one loaded Media Cartridge");c.draw(cpu,frame);
      RasterComputer.present(w,n,cpu);
      for(long key:n.speakers.stream().sorted().limit(8).toList()){
         float v=(flags&1)!=0?volume(w,key):0;String data=reference(w,n)+","+frame+","+v;
         receive(w,3,key,data);Net319.rasterBroadcast(w,3,key,data);
      }
   }
   static void receive(World w,int type,long key,String data){
      if(RasterComputer.kindSafe(w,key)!=9)return;
      try{String[] p=data.split(",");if(type==2&&p.length==4)RasterAudio.tone(w,key,Integer.parseInt(p[0]),Integer.parseInt(p[1]),Integer.parseInt(p[2]),finiteVolume(p[3]));
         else if(type==3&&p.length==3){int frame=Integer.parseInt(p[1]);float v=finiteVolume(p[2]);if(v==0){RasterAudio.stop(w,key);return;}RasterClip c=clip(w,p[0]);if(frame<0||frame>=c.frames)return;RasterAudio.video(w,key,c,frame,v);}
      }catch(IOException e){if(w.game!=null&&w.raster.media.missing.size()<8&&w.raster.media.missing.add(data.split(",")[0]))w.game.say("Media audio unavailable: install the matching .bcv file in .blockcraft112/media. Built-in Shadow Signal needs no download.");}catch(RuntimeException ignored){}
   }
   static float finiteVolume(String v){float f=Float.parseFloat(v);return Float.isFinite(f)?Math.max(0,Math.min(1,f)):0;}
   static void begin(World w){w.raster.media.tones=0;}
   static void end(World w){RasterAudio.endTick(w);}
   static void install(World w,long cartridge,String name){w.raster.media.slots.put(cartridge,name);}
   static void loadProgram(Game g,String name)throws IOException{
      if(!g.player.creative||Net319.isClientConnected()&&!Net319.isHosting()){g.say("Only the Creative host imports media.");return;}
      if(g.target==null||RasterComputer.kindSafe(g.world,World.pack(g.target.x,g.target.y,g.target.z))!=10){g.say("Look at a Media Cartridge first.");return;}
      long key=World.pack(g.target.x,g.target.y,g.target.z);
      if(name.equalsIgnoreCase("shadow-signal")){clip(g.world,BUILTIN);install(g.world,key,BUILTIN);g.say("Loaded original Shadow Signal cartridge (32 seconds).");return;}
      if(!name.matches("[A-Za-z0-9_-]+\\.bcv"))throw new IOException("Use a simple .bcv filename");
      Path base=g.root.toPath().resolve("media");Files.createDirectories(base);base=base.toRealPath();Path file=base.resolve(name).toRealPath();if(!file.startsWith(base)||Files.size(file)>RasterClip.MAX_BYTES)throw new IOException("Invalid media file");
      RasterClip c=RasterClip.read(Files.readAllBytes(file));if(g.world.raster.media.imported.size()>=8&&!g.world.raster.media.imported.containsKey(c.hash))throw new IOException("Eight imported clips per loaded world maximum");
      g.world.raster.media.imported.put(c.hash,c);install(g.world,key,c.hash);Files.write(base.resolve(c.hash+".bcv"),c.encoded);save(g.world);g.say("Loaded "+c.width+"x"+c.height+", "+c.frames+" frames. Back up the entire world/media folder too.");
   }
   static void save(World w){if(w.dir==null||!w.saveEnabled||w.raster.media.saveBlocked)return;try{
      Path base=w.dir.toPath();Properties p=new Properties();p.setProperty("version","1");
      for(var e:w.raster.media.slots.entrySet()){p.setProperty(Long.toString(e.getKey()),e.getValue());if(!e.getValue().equals(BUILTIN)){RasterClip c=clip(w,e.getValue());Path dir=base.resolve("media");Files.createDirectories(dir);Path f=dir.resolve(c.hash+".bcv");if(!Files.exists(f))Files.write(f,c.encoded);}}
      Path temp=base.resolve("media.properties.tmp");try(OutputStream out=Files.newOutputStream(temp)){p.store(out,"Raster media cartridges");}Files.move(temp,base.resolve("media.properties"),StandardCopyOption.REPLACE_EXISTING);
   }catch(IOException e){System.err.println("Media not saved: "+e);}}
   static void load(World w){if(w.dir==null)return;Path file=w.dir.toPath().resolve("media.properties");if(!Files.exists(file))return;try{
      if(Files.size(file)>1024*1024)throw new IOException("Media map too large");Properties p=new Properties();try(InputStream in=Files.newInputStream(file)){p.load(in);}if(!"1".equals(p.getProperty("version")))throw new IOException("Media version");Map<Long,String> loaded=new HashMap<>();
      for(String k:p.stringPropertyNames())if(!k.equals("version")){String v=p.getProperty(k);if(!v.equals(BUILTIN)&&!v.matches("[a-f0-9]{64}"))throw new IOException("Invalid media reference");loaded.put(Long.parseLong(k),v);}w.raster.media.slots.putAll(loaded);
   }catch(IOException|RuntimeException e){w.raster.media.saveBlocked=true;System.err.println("Media map not loaded; preserving original file: "+e);}}
}
