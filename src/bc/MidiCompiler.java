package bc;

import javax.sound.midi.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Offline MIDI-to-block compiler. Playback is exclusively ordinary redstone, even after removal. */
final class MidiCompiler {
   static final int BLOCK=3970,MAX_BYTES=1024*1024,MAX_LENGTH=256,MAX_NOTES=512;
   record Note(int at,int pitch,int instrument){}
   record Song(List<Note> notes,int folded,int merged,int outside){}
   record Entry(long tick,int track,int index,MidiMessage message){}
   record Pending(long anchor,Plan plan){}
   static final Map<World,Pending> pending=Collections.synchronizedMap(new WeakHashMap<>());
   static final class Plan {
      final Map<Long,Integer> blocks=new LinkedHashMap<>(),pitches=new LinkedHashMap<>();
      final Map<Long,Integer> times=new LinkedHashMap<>();
      final int x,y,z;int length,repeaters;
      Plan(int x,int y,int z){this.x=x;this.y=y;this.z=z;}
      void put(int dx,int dy,int dz,int state){blocks.put(World.pack(x+dx,y+dy,z+dz),state);}
      void support(int dx,int dz){put(dx,-2,dz,Blocks.STONE);put(dx,-1,dz,Blocks.STONE);}
   }
   static void register(){
      Blocks.def(BLOCK,"MIDI Build Compiler",0,true,Redstone319.T_NOTE_BLOCK);Blocks.hardness[BLOCK]=2;Blocks.solid[BLOCK]=true;Blocks.opacity[BLOCK]=15;Blocks.drop[BLOCK]=BLOCK;
      Shapes.model(BLOCK,new double[][]{Circuit.part(0,0,0,16,12,16,0x344752),Circuit.part(1,12,1,15,13,8,0x43D8EF),Circuit.part(1,12,9,15,14,15,0xE3E8DE),Circuit.part(3,14,9,4,15,12,0x111820),Circuit.part(6,14,9,7,15,12,0x111820),Circuit.part(10,14,9,11,15,12,0x111820),Circuit.part(13,14,9,14,15,12,0x111820)});
   }
   static void recipe(){Recipes.shaped(BLOCK,1,new String[]{"IRI","QNQ","IRI"},'I',Items.IRON_INGOT,'R',Redstone319.ITEM_REDSTONE,'Q',Items.QUARTZ,'N',Redstone319.NOTE_BLOCK);}
   static boolean use(Game g,int x,int y,int z,int id){
      if(id!=BLOCK)return false;
      g.say("MIDI compiler: put a .mid in .blockcraft112/midi, then /midi filename.mid. /midibuild confirms the preview.");
      g.say("/mididemo creates an original example file. Import requires Creative host. Generated builds need no compiler to play.");if(g.player.creative)g.openChat("/midi ");return true;
   }
   static int gm(int p,int key,int channel){
      if(channel==9)return key==35||key==36||key>=41&&key<=47&&key!=42&&key!=44&&key!=46?4:key==42||key==44||key==46||key>=49?3:2;
      if(p>=32&&p<40)return 1;if(p>=24&&p<32)return 7;if(p>=72&&p<80)return 5;
      if(p==9||p==14)return 6;if(p==10)return 8;if(p==11)return 10;if(p==12||p==13)return 9;
      if(p==105)return 14;if(p>=80&&p<88)return 13;if(p>=112)return 2;
      return 0;
   }
   static Song parse(byte[] data,double start,double duration)throws Exception{
      if(data.length>MAX_BYTES||data.length<14||!Double.isFinite(start)||start<0||!Double.isFinite(duration)||duration<=0||duration>120)throw new IOException("Use a MIDI under 1 MiB and a 0..120 second excerpt");
      MidiFileFormat format=MidiSystem.getMidiFileFormat(new ByteArrayInputStream(data));if(format.getType()!=0&&format.getType()!=1)throw new IOException("Only synchronous MIDI type 0/1 is supported");
      Sequence seq=MidiSystem.getSequence(new ByteArrayInputStream(data));if(seq.getResolution()<=0)throw new IOException("Invalid MIDI timebase");
      List<Entry> all=new ArrayList<>();Track[] tracks=seq.getTracks();for(int t=0;t<tracks.length;t++)for(int i=0;i<tracks[t].size();i++){if(all.size()>=100000)throw new IOException("Too many MIDI events");MidiEvent e=tracks[t].get(i);all.add(new Entry(e.getTick(),t,i,e.getMessage()));}
      all.sort(Comparator.comparingLong(Entry::tick).thenComparingInt(Entry::track).thenComparingInt(Entry::index));
      int tempo=500000,folded=0,outside=0,merged=0;long last=0;double seconds=0;int[] programs=new int[16];TreeMap<Integer,LinkedHashSet<Note>> chords=new TreeMap<>();
      for(Entry e:all){
         double step=seq.getDivisionType()==Sequence.PPQ?tempo/1e6/seq.getResolution():1.0/(seq.getDivisionType()==Sequence.SMPTE_30DROP?30000.0/1001:seq.getDivisionType())/seq.getResolution();seconds+=(e.tick-last)*step;last=e.tick;
         if(!Double.isFinite(seconds)||seconds>86400)throw new IOException("MIDI timeline too large");
         if(e.message instanceof MetaMessage m&&m.getType()==0x51&&seq.getDivisionType()==Sequence.PPQ){byte[] b=m.getData();if(b.length!=3)throw new IOException("Invalid tempo event");tempo=(b[0]&255)<<16|(b[1]&255)<<8|b[2]&255;if(tempo==0)throw new IOException("Zero MIDI tempo");}
         if(!(e.message instanceof ShortMessage m))continue;
         if(m.getCommand()==ShortMessage.PROGRAM_CHANGE){programs[m.getChannel()]=m.getData1();continue;}
         if(m.getCommand()!=ShortMessage.NOTE_ON||m.getData2()==0)continue;
         if(seconds<start||seconds>=start+duration){outside++;continue;}
         int instrument=gm(programs[m.getChannel()],m.getData1(),m.getChannel()),key=m.getData1(),p;
         if(m.getChannel()==9)p=12;else {int old=key;while(key<NoteBlocks.LOW[instrument])key+=12;while(key>NoteBlocks.LOW[instrument]+24)key-=12;p=key-NoteBlocks.LOW[instrument];if(key!=old)folded++;}
         int tick=(int)Math.round((seconds-start)*10);Note note=new Note(tick,p,instrument);Set<Note> chord=chords.computeIfAbsent(tick,k->new LinkedHashSet<>());if(!chord.add(note))merged++;
         if(chord.size()>6)throw new IOException("More than six distinct notes in a 100ms chord; simplify tracks or select an excerpt");
      }
      List<Note> notes=new ArrayList<>();for(var c:chords.values())notes.addAll(c);
      if(notes.isEmpty()||notes.size()>MAX_NOTES)throw new IOException("Choose an excerpt containing 1..512 notes");return new Song(List.copyOf(notes),folded,merged,outside);
   }
   static Plan plan(Song song,int x,int y,int z)throws IOException{
      if(y<3||y>253)throw new IOException("Need room above and below the circuit");Plan p=new Plan(x,y,z);p.support(0,0);p.put(0,0,0,Redstone319.STONE_BUTTON);
      TreeMap<Integer,List<Note>> chords=new TreeMap<>();for(Note n:song.notes)chords.computeIfAbsent(n.at,k->new ArrayList<>()).add(n);
      int cursor=1,last=-1;
      for(var e:chords.entrySet()){
         int delay=e.getKey()-last;if(delay<1)throw new IOException("Invalid note order");
         while(delay>0){int d=Math.min(4,delay);p.support(0,cursor);p.put(0,0,cursor,Redstone319.REPEATER_OFF+1|((d-1)<<12));p.repeaters++;cursor++;delay-=d;}
         int tap=cursor;p.support(0,tap+1);p.put(0,0,tap+1,Redstone319.REDSTONE_WIRE0);p.support(0,tap+2);p.put(0,0,tap+2,Redstone319.REDSTONE_WIRE0);
         for(int i=0;i<=e.getValue().size()*2;i++){p.support(i,tap);p.put(i,0,tap,Redstone319.REDSTONE_WIRE0);}
         for(int i=0;i<e.getValue().size();i++){Note n=e.getValue().get(i);int nx=2+i*2;p.support(nx,tap+1);p.put(nx,0,tap+1,Redstone319.REDSTONE_WIRE0);p.support(nx,tap+2);p.put(nx,-1,tap+2,NoteBlocks.MATERIAL[n.instrument]);p.put(nx,0,tap+2,Redstone319.NOTE_BLOCK);long k=World.pack(x+nx,y,z+tap+2);p.pitches.put(k,n.pitch);p.times.put(k,(n.at+1)*2);}
         cursor=tap+3;last=e.getKey();if(cursor>MAX_LENGTH)throw new IOException("Track exceeds 256 blocks; use /midi filename.mid startSeconds durationSeconds to select a shorter section");
      }p.length=cursor;return p;
   }
   static void preflight(World w,Plan p)throws IOException{
      if(w.redstone.notesSaveBlocked)throw new IOException("Back up/repair corrupt notes.properties before generating a tuned build");
      // Check even reserved air and an isolation border: don't overwrite blocks or join other wires.
      for(int x=p.x-1;x<=p.x+14;x++)for(int z=p.z-1;z<=p.z+p.length;z++){
         if(!w.isLoaded(x,z))throw new IOException("Area not loaded: track is "+p.length+" blocks south. Increase view distance / use a shorter excerpt");
         for(int y=p.y-2;y<=p.y+2;y++)if(w.getBlock(x,y,z)!=0)throw new IOException("Build cancelled: occupied/reserved space at "+x+","+y+","+z+". Place compiler higher in clear air");
      }
   }
   static void build(World w,Plan p)throws IOException{
      preflight(w,p);
      for(var e:p.blocks.entrySet()){long k=e.getKey();w.setState(World.ux(k),World.uy(k),World.uz(k),e.getValue());}
      for(var e:p.pitches.entrySet()){long k=e.getKey();NoteBlocks.tune(w,World.ux(k),World.uy(k),World.uz(k),e.getValue());}
   }
   static long target(Game g)throws IOException{
      if(g.world==null||g.player==null||!g.player.creative||Net319.isClientConnected()&&!Net319.isHosting())throw new IOException("Creative host only");
      if(g.target==null||g.world.getBlock(g.target.x,g.target.y,g.target.z)!=BLOCK)throw new IOException("Look at a MIDI Build Compiler first");return World.pack(g.target.x,g.target.y,g.target.z);
   }
   static void command(Game g,String[] args)throws Exception{
      long anchor=target(g);
      if(args[0].equalsIgnoreCase("midibuild")){
         Pending q=pending.get(g.world);if(q==null||q.anchor!=anchor)throw new IOException("Preview a MIDI on this compiler first with /midi filename.mid");
         build(g.world,q.plan);pending.remove(g.world);g.say("Built "+q.plan.pitches.size()+" tuned note blocks and "+q.plan.repeaters+" repeaters. Button: "+q.plan.x+","+q.plan.y+","+q.plan.z+". Follow the track to hear distant notes. Compiler may be removed.");return;
      }
      pending.remove(g.world);if(args.length<2)throw new IOException("/midi filename.mid [startSeconds durationSeconds]");
      String name=args[1];if(!name.matches("[A-Za-z0-9_-]+\\.(mid|midi)"))throw new IOException("Use a simple .mid/.midi filename in .blockcraft112/midi");
      Path dir=g.root.toPath().resolve("midi");Files.createDirectories(dir);dir=dir.toRealPath();Path f=dir.resolve(name).toRealPath();if(!f.startsWith(dir)||!Files.isRegularFile(f)||Files.size(f)>MAX_BYTES)throw new IOException("Invalid or oversized MIDI file");
      if(args.length!=2&&args.length!=4)throw new IOException("Use both startSeconds and durationSeconds, or neither");
      double start=args.length==4?Double.parseDouble(args[2]):0,duration=args.length==4?Double.parseDouble(args[3]):120;
      Song song=parse(Files.readAllBytes(f),start,duration);if(args.length==2&&song.outside>0)throw new IOException("MIDI exceeds 120 seconds. Select an explicit excerpt; nothing was truncated/built");
      Plan p=plan(song,World.ux(anchor)+3,World.uy(anchor)+1,World.uz(anchor)+2);pending.put(g.world,new Pending(anchor,p));
      g.say("Preview: "+song.notes.size()+" notes, "+p.repeaters+" repeaters, "+p.blocks.size()+" blocks. Clear footprint 16 x "+(p.length+2)+" x 5; extends SOUTH.");
      g.say("100ms timing; fixed 100ms lead-in; "+song.folded+" octave folds, "+song.merged+" merged duplicates, "+song.outside+" notes outside excerpt. Velocity/sustain/bends are not reproduced. /midibuild to confirm.");
   }
   static byte[] demo()throws Exception{
      Sequence s=new Sequence(Sequence.PPQ,480);Track t=s.createTrack();MetaMessage tempo=new MetaMessage();tempo.setMessage(0x51,new byte[]{7,(byte)161,32},3);t.add(new MidiEvent(tempo,0));
      int[] melody={66,69,73,71,68,66,63,66};int[] instruments={0,24,11,73};for(int i=0;i<melody.length;i++){
         if(i%2==0){ShortMessage program=new ShortMessage();program.setMessage(ShortMessage.PROGRAM_CHANGE,0,instruments[i/2],0);t.add(new MidiEvent(program,i*480));}
         ShortMessage on=new ShortMessage();on.setMessage(ShortMessage.NOTE_ON,0,melody[i],90);t.add(new MidiEvent(on,i*480));
         ShortMessage off=new ShortMessage();off.setMessage(ShortMessage.NOTE_OFF,0,melody[i],0);t.add(new MidiEvent(off,i*480+240));
      }ByteArrayOutputStream out=new ByteArrayOutputStream();MidiSystem.write(s,1,out);return out.toByteArray();
   }
   static void exportDemo(Game g)throws Exception{Path dir=g.root.toPath().resolve("midi");Files.createDirectories(dir);Path f=dir.resolve("Workshop.mid");if(!Files.exists(f))Files.write(f,demo(),StandardOpenOption.CREATE_NEW);g.say("Original Workshop.mid ready in .blockcraft112/midi. Look at MIDI Build Compiler and /midi Workshop.mid");}
   static long smokeStart;static boolean smokeChecked;
   static void smoke(Game g,int frame){try{
      if(frame==80){smokeChecked=false;int x=(int)g.player.x,y=(int)g.player.y+5,z=(int)g.player.z;g.world.setState(x,y,z,BLOCK);Plan p=plan(parse(demo(),0,120),x+3,y+1,z+2);build(g.world,p);g.player.setPos(x+18,y+12,z+24);g.player.yaw=90;g.player.pitch=30;g.player.flying=true;pending.put(g.world,new Pending(World.pack(x,y,z),p));}
      if(frame==120){Pending q=pending.get(g.world);g.world.setBlock(World.ux(q.anchor),World.uy(q.anchor),World.uz(q.anchor),0);}
      if(frame==140){smokeStart=g.world.tickCount;Plan p=pending.get(g.world).plan;Redstone319.use(g,p.x,p.y,p.z,Redstone319.STONE_BUTTON);}
      if(frame>=300&&!smokeChecked){if(g.world.tickCount-smokeStart<100){g.autoFrames=299;return;}smokeChecked=true;Plan p=pending.get(g.world).plan;if(g.world.redstone.noteHits.size()!=p.pitches.size())throw new IllegalStateException("MIDI redstone did not play every generated note");System.out.println("[midi-smoke] actual note triggers="+g.world.redstone.noteHits.size()+", repeaters="+p.repeaters+", compiler is not a player");}
   }catch(Exception e){throw new IllegalStateException(e);}}
}
