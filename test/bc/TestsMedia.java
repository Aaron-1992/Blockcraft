package bc;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import static bc.GameTests.*;

public final class TestsMedia {
   static void ticks(World w,int n){for(int i=0;i<n;i++)w.tick();}
   static RasterClip original()throws Exception{return RasterClip.read(Files.readAllBytes(Path.of("res/media/shadow-signal.bcv")));}
   static double energy(float[] a){double e=0;for(float x:a){check(Float.isFinite(x),"finite sample");e+=x*x;}return e;}
   public static void main(String[] args){run();System.out.println("Media/audio: "+passed+" passed, "+failed+" failed");System.exit(failed==0?0:1);}
   static void run(){
      test("media: original bounded cartridge format, nonempty moving frames and soundtrack",()->{
         RasterClip c=original();eq(64,c.width,"width");eq(48,c.height,"height");eq(640,c.frames,"32 seconds at 20fps");eq(1411200,c.pcm.length,"sample-exact movie length");
         int peak=0;long sum=0;for(short s:c.pcm){peak=Math.max(peak,Math.abs((int)s));sum+=(long)s*s;}check(peak>10000&&peak<30000,"audible with headroom");check(sum>1_000_000_000L,"actual audio energy");
         Set<Integer> hashes=new HashSet<>();for(int f=0;f<c.frames;f++){int h=1;for(int y=0;y<48;y++)for(int x=0;x<64;x++){int v=c.pixel(f,x,y);check(v==0||v==15,"monochrome pixel");h=h*31+v;}hashes.add(h);}check(hashes.size()>400,"hundreds of distinct frames, not a still picture");
         for(byte[] bad:new byte[][]{new byte[0],Arrays.copyOf(c.encoded,50),new byte[]{1,2,3}}){boolean reject=false;try{RasterClip.read(bad);}catch(IOException e){reject=true;}check(reject,"malformed/truncated cartridge rejected");}
         ByteArrayOutputStream b=new ByteArrayOutputStream();try(DataOutputStream out=new DataOutputStream(new GZIPOutputStream(b))){out.writeInt(0x42435631);out.writeInt(1000000);out.writeInt(64);out.writeInt(20);out.writeInt(640);out.writeInt(44100);out.writeInt(1411200);}boolean reject=false;try{RasterClip.read(b.toByteArray());}catch(IOException e){reject=true;}check(reject,"dimensions checked before allocation");
      });
      test("audio: synth envelopes, stereo mixer, frame lock, seek, pause, volume and distance",()->{
         Game g=newGame(1);World w=g.world;g.player.setPos(8.5,80,8.5);RasterAudio.listener(g);long key=World.pack(10,81,10);RasterClip c=original();
         for(int v=0;v<4;v++){short[] a=RasterAudio.synth(69,100,v);eq(4410,a.length,"tone length");eq(0,(int)a[0],"soft attack");eq(0,(int)a[a.length-1],"soft release");check(Arrays.stream(toInts(a)).anyMatch(x->x!=0),"non-silent oscillator");}
         float[] l=new float[2205],r=new float[2205];RasterAudio.video(w,key,c,120,.7F);eq(2205,RasterAudio.mix(l,r,2205,1),"one frame of PCM");check(energy(l)>0&&energy(r)>0,"audible stereo output");check(!Arrays.equals(l,r),"spatial panning");
         Arrays.fill(l,0);Arrays.fill(r,0);eq(0,RasterAudio.mix(l,r,2205,1),"cannot run ahead of displayed frame");eq(0.0,energy(l),"stall holds silence instead of drifting");
         RasterAudio.video(w,key,c,121,.7F);RasterAudio.mix(l,r,512,1);RasterAudio.Stream stream=RasterAudio.streams.get(new RasterAudio.Key(w,key));eq(121*2205+512,stream.pos,"sequential frame continues at sample clock");
         RasterAudio.video(w,key,c,20,.7F);RasterAudio.mix(l,r,512,1);eq(20*2205+512,stream.pos,"backward seek resynchronizes audio");
         RasterAudio.video(w,key,c,300,.7F);RasterAudio.mix(l,r,512,1);eq(300*2205+512,stream.pos,"forward seek resynchronizes audio");
         Arrays.fill(l,0);Arrays.fill(r,0);RasterAudio.mix(l,r,512,0);eq(0.0,energy(l),"master sound volume respected");
         RasterAudio.video(w,key,c,300,0);check(RasterAudio.streams.isEmpty(),"mute removes stream immediately");
         RasterAudio.tone(w,key,72,80,0x901,.7F);check(!RasterAudio.tones.isEmpty(),"ROM tone enqueued");Arrays.fill(l,0);Arrays.fill(r,0);RasterAudio.mix(l,r,512,1);check(energy(l)>0,"tone goes through real mixer");
         RasterAudio.video(w,key,c,301,.7F);g.screen=Game.S.PAUSE;RasterAudio.listener(g);check(RasterAudio.streams.isEmpty()&&RasterAudio.tones.isEmpty(),"pause/menu releases foreground audio");
         g.screen=Game.S.PLAY;g.player.setPos(200,80,200);RasterAudio.listener(g);RasterAudio.video(w,key,c,301,.7F);check(RasterAudio.streams.isEmpty(),"out-of-range speaker inaudible");RasterAudio.listener(null);check(!RasterAudio.playing&&RasterAudio.listenerWorld==null,"null listener preserves SoundEngine null-safe contract");w.shutdown();
      });
      test("audio: continuous 512-sample mixing across 2205-sample frame boundaries",()->{
         Game g=newGame(1);World w=g.world;g.player.setPos(8.5,80,8.5);RasterAudio.listener(g);RasterAudio.stopWorld(w);long key=World.pack(10,81,10);RasterClip clip=original();int mixed=0;
         for(int i=0;i<600;i++){RasterAudio.video(w,key,clip,i*512/2205,.7F);float[] l=new float[512],r=new float[512];mixed+=RasterAudio.mix(l,r,512,1);RasterAudio.Stream stream=RasterAudio.streams.get(new RasterAudio.Key(w,key));check(stream.pos<=stream.cursor.end(),"no audio ahead of submitted frame");}
         check(600*512-mixed<=512,"at most one device block of initial slack, not gaps at every frame");w.shutdown();
      });
      test("audio: batched simulation ticks keep continuous PCM at a 75ms renderer cadence",()->{
         Game g=newGame(1);World w=g.world;g.player.setPos(8.5,80,8.5);RasterAudio.listener(g);RasterAudio.stopWorld(w);long key=World.pack(10,81,10);RasterClip clip=original();int last=0;
         for(int i=0;i<600;i++){int frame=(i*512/3308)*3308/2205;RasterAudio.video(w,key,clip,frame,.7F);RasterAudio.mix(new float[512],new float[512],512,1);int pos=RasterAudio.streams.get(new RasterAudio.Key(w,key)).pos;check(pos-last>=0&&pos-last<=512,"two ticks in one render update do not skip PCM (block "+i+", frame "+frame+", delta "+(pos-last)+")");last=pos;}
         w.shutdown();
      });
      test("media: physical ROM playback, seek, mute, cable cut, restart and native save mapping",()->{
         Game g=newGame(1);World w=g.world;ShadowMachine.install(w,0,72,0);g.player.setPos(5.5,77,46.5);RasterAudio.listener(g);ticks(w,2);
         long cp=World.pack(-20,72,6),key=World.pack(2,77,45),cart=World.pack(-10,72,6);RasterComputer.Network n=RasterComputer.network(w,cp);RasterVM.Cpu c=w.raster.cpus.get(cp);
         eq("",n.error,"connected media circuit");eq(192,n.screens.size(),"64x48 native panels");eq(768,n.lamps.size(),"32x24 actual lamp drivers");eq(2,RasterComputer.lampScale(w,n),"2x lamp sampling");eq(1,c.out[3],"starts paused");eq(0,c.out[0],"first frame");
         RasterComputer.input(w,key,0,64);ticks(w,1);check(!RasterAudio.streams.isEmpty(),"speaker receives synchronized PCM");ticks(w,20);eq(20,c.out[0],"one video frame per game tick");
         RasterComputer.input(w,key,0,32);ticks(w,1);int paused=c.out[0];check(RasterAudio.streams.isEmpty(),"program pause stops soundtrack");ticks(w,20);eq(paused,c.out[0],"video pauses too");
         RasterComputer.input(w,key,0,2);ticks(w,1);eq(paused+100,c.out[0],"seek forward five seconds while paused");RasterComputer.input(w,key,0,1);ticks(w,1);eq(paused,c.out[0],"seek backwards");
         RasterComputer.input(w,key,0,32);ticks(w,1);RasterComputer.use(g,2,77,45,RasterComputer.KEYS+1);g.onKey(77,1,0);ticks(w,1);check(RasterAudio.streams.isEmpty(),"M mutes connected speakers without pausing film");g.onKey(77,1,0);ticks(w,1);check(!RasterAudio.streams.isEmpty(),"M resumes current audio phase");
         int pc=c.pc;w.setState(-19,72,5,0);ticks(w,1);eq(pc,c.pc,"cut required bus freezes processor");check(RasterAudio.streams.isEmpty(),"cut required bus stops sound");w.setState(-19,72,5,RasterComputer.CABLE);ticks(w,1);check(!RasterAudio.streams.isEmpty(),"repair resumes sound");
         w.setState(-10,72,6,0);pc=c.pc;ticks(w,1);eq(pc,c.pc,"missing cartridge stops playback");check(RasterAudio.streams.isEmpty(),"missing cartridge silences playback");w.setState(-10,72,6,RasterComputer.CARTRIDGE+1);RasterMedia.install(w,cart,RasterMedia.BUILTIN);ticks(w,1);
         RasterComputer.input(w,key,0,64);ticks(w,1);eq(0,c.out[0],"Enter restarts video and audio at zero");
         Path dir=Files.createTempDirectory("media-save-");setField(w,"dir",dir.toFile());w.saveEnabled=true;RasterComputer.save(w);RasterMedia.save(w);World restored=new World(1234,dir.toFile());eq(RasterMedia.BUILTIN,restored.raster.media.slots.get(cart),"cartridge mapping persists");eq(c.pc,restored.raster.cpus.get(cp).pc,"ROM playback position persists");
         RasterClip clip=original();RasterComputer.input(w,key,0,32);ticks(w,6);int frame=c.out[0];for(int y=0;y<24;y++)for(int x=0;x<32;x++)eq(clip.pixel(frame,x*2,y*2)==0?Redstone319.REDSTONE_LAMP:Redstone319.REDSTONE_LAMP_LIT,w.getBlock(x,95-y,0),"real downsampled lamp frame");
         w.shutdown();restored.shutdown();
      });
      test("media: legacy ROM safety and generic sound opcodes, not hidden event detection",()->{
         int[] legacy=RasterVM.assemble(Files.readString(Path.of("test/fixtures/FallingBlocks-v69.asm")));check(Arrays.equals(legacy,RasterVM.assemble(RasterPrograms.fallingSource(false))),"exact old V6.9 program recognized");
         final int[] sounds={0};RasterVM.Cpu cpu=new RasterVM.Cpu();int[] mem=new int[4096];RasterVM.IO io=new RasterVM.IO(){public int input(int port){return port==0?64:0;}public void present(RasterVM.Cpu c){}public void tone(int n,int ms,int v){sounds[0]++;}};
         RasterVM.step(cpu,RasterPrograms.falling(),mem,io);RasterVM.step(cpu,RasterPrograms.falling(),mem,io);check(sounds[0]>0,"actual game ROM emits TONE");
         Game g=newGame(1);World w=g.world;RasterArcade.install(w,-8,72,0);long cp=World.pack(-8,72,6),rom=World.pack(-6,72,6);w.raster.roms.put(rom,legacy);g.target=hit(-8,72,6,0,1,0);ShadowMachine.upgrade(g);check(Arrays.equals(RasterPrograms.falling(),w.raster.roms.get(rom)),"old world gets sound ROM");eq(1,w.raster.cpus.get(cp).pc==0?1:0,"upgrade resets instead of corrupting running PC");
         int[] custom=RasterVM.assemble("HALT");w.raster.roms.put(rom,custom);ShadowMachine.upgrade(g);check(custom==w.raster.roms.get(rom),"custom ROM is never overwritten");w.shutdown();
      });
      test("media: actual audio packets, late frame sync, custom import and corrupt-map protection",()->{
         Game g=newGame(1);World w=g.world;g.player.setPos(8.5,80,8.5);RasterAudio.listener(g);long speaker=World.pack(10,81,10);w.setState(10,81,10,RasterComputer.SPEAKER);
         java.lang.reflect.Field inboxField=Net319.class.getDeclaredField("inbox"),host=Net319.class.getDeclaredField("hosting"),client=Net319.class.getDeclaredField("clientConnected");inboxField.setAccessible(true);host.setAccessible(true);client.setAccessible(true);
         @SuppressWarnings("unchecked") Queue<String> inbox=(Queue<String>)inboxField.get(null);boolean oh=host.getBoolean(null),oc=client.getBoolean(null);
         try {
            host.setBoolean(null,false);client.setBoolean(null,true);inbox.add(Net319.rasterPacket(w,2,speaker,"72,100,2305,0.7"));Net319.processInbox(w,g);check(!RasterAudio.tones.isEmpty(),"actual network tone received");
            inbox.add(Net319.rasterPacket(w,3,speaker,"shadow-signal,400,0.7"));Net319.processInbox(w,g);float[] l=new float[512],r=new float[512];RasterAudio.mix(l,r,512,1);eq(400*2205+512,RasterAudio.streams.get(new RasterAudio.Key(w,speaker)).pos,"late join starts at host frame, not song beginning");
            inbox.add(Net319.rasterPacket(w,3,speaker,"shadow-signal,400,0"));Net319.processInbox(w,g);check(RasterAudio.streams.isEmpty(),"network pause/mute");
            host.setBoolean(null,true);client.setBoolean(null,false);RasterAudio.stopWorld(w);inbox.add(Net319.rasterPacket(w,2,speaker,"72,100,2305,0.7"));Net319.processInbox(w,g);check(RasterAudio.tones.isEmpty(),"host rejects forged client audio");
         }finally{host.setBoolean(null,oh);client.setBoolean(null,oc);inbox.clear();Net319.clearWorldState();}
         Path root=Files.createTempDirectory("media-import-"),save=Files.createTempDirectory("media-native-");setField(g,"root",root.toFile());setField(w,"dir",save.toFile());w.saveEnabled=true;Files.createDirectories(root.resolve("media"));
         RasterClip clip=original();Files.write(root.resolve("media/custom.bcv"),clip.encoded);w.setState(8,80,9,RasterComputer.CARTRIDGE);g.target=hit(8,80,9,0,1,0);RasterMedia.loadProgram(g,"custom.bcv");long slot=World.pack(8,80,9);eq(clip.hash,w.raster.media.slots.get(slot),"import stores content hash");check(Files.exists(save.resolve("media/"+clip.hash+".bcv")),"media bytes stored with native world");
         World restored=new World(1,save.toFile());eq(clip.hash,restored.raster.media.slots.get(slot),"imported reference reloads");eq(clip.hash,RasterMedia.clip(restored,clip.hash).hash,"imported bytes reload");
         boolean refused=false;try{RasterMedia.loadProgram(g,"../outside.bcv");}catch(IOException e){refused=true;}check(refused,"path traversal refused");Files.write(root.resolve("media/bad.bcv"),new byte[]{1,2});refused=false;try{RasterMedia.loadProgram(g,"bad.bcv");}catch(IOException e){refused=true;}check(refused,"invalid file refused");eq(clip.hash,w.raster.media.slots.get(slot),"bad import leaves valid cartridge untouched");
         Path bad=Files.createTempDirectory("media-corrupt-");byte[] bytes="version=999\n".getBytes();Files.write(bad.resolve("media.properties"),bytes);World corrupt=new World(2,bad.toFile());check(corrupt.raster.media.saveBlocked,"corruption blocks overwriting original");RasterMedia.save(corrupt);check(Arrays.equals(bytes,Files.readAllBytes(bad.resolve("media.properties"))),"corrupt map preserved for recovery");w.shutdown();restored.shutdown();corrupt.shutdown();
      });
      test("media: pixel geometry merging preserves every addressable pixel",()->{
         eq(2,RasterComputer.panelModel(1,0).length,"uniform panel uses one face rectangle plus case");Random r=new Random(610);
         for(int i=0;i<200;i++){long bits=r.nextLong();double[][] boxes=RasterComputer.panelModel(1,bits);for(int y=0;y<4;y++)for(int x=0;x<4;x++){double px=(x+.5)/4,py=1-(y+.5)/4;int hits=0;for(int j=1;j<boxes.length;j++){double[] b=boxes[j];if(px>=b[0]&&px<b[3]&&py>=b[1]&&py<b[4]){hits++;eq(RasterComputer.PALETTE[(int)(bits>>>((y*4+x)*4))&15],(int)b[7],"merged pixel color");}}eq(1,hits,"pixel covered exactly once");}}
      });
   }
   static int[] toInts(short[] a){int[] b=new int[a.length];for(int i=0;i<a.length;i++)b[i]=a[i];return b;}
}
