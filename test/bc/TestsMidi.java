package bc;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.sound.midi.*;
import static bc.GameTests.*;

public final class TestsMidi {
   public static void main(String[] a){run();System.out.println("MIDI/notes: "+passed+" passed, "+failed+" failed");System.exit(failed==0?0:1);}
   static void ticks(World w,int n){for(int i=0;i<n;i++){w.tick();Redstone319.tick(w.game);}}
   static byte[] encode(Sequence s)throws Exception{ByteArrayOutputStream o=new ByteArrayOutputStream();MidiSystem.write(s,1,o);return o.toByteArray();}
   static void note(Track t,int channel,int key,int velocity,long time)throws Exception{ShortMessage m=new ShortMessage();m.setMessage(ShortMessage.NOTE_ON,channel,key,velocity);t.add(new MidiEvent(m,time));}
   static void tempo(Track t,int value,long time)throws Exception{MetaMessage m=new MetaMessage();m.setMessage(0x51,new byte[]{(byte)(value>>16),(byte)(value>>8),(byte)value},3);t.add(new MidiEvent(m,time));}
   static void run(){
      test("notes: sixteen material instruments and bounded original PCM",()->{
         Set<Integer> hashes=new HashSet<>();for(int i=0;i<16;i++){eq(i,NoteBlocks.instrument(NoteBlocks.MATERIAL[i]),"material instrument "+i);short[] a=NoteBlocks.samples(i,22050);eq(0,(int)a[0],"attack starts at zero");eq(0,(int)a[a.length-1],"release ends at zero");long energy=0;for(short v:a){energy+=(long)v*v;check(Math.abs((int)v)<=18000,"PCM headroom");}check(energy>100000,"non-silent instrument");hashes.add(Arrays.hashCode(a));SoundEngine.Sample bank=SoundEngine.getSample("note_"+NoteBlocks.NAMES[i]);check(bank!=null&&Arrays.equals(bank.pcm,NoteBlocks.samples(i,bank.sampleRate)),"actual sound bank contains selected instrument");}eq(16,hashes.size(),"sixteen different waveforms");
      });
      test("midi: merged track tempo map, program changes, percussion and excerpts",()->{
         Sequence seq=new Sequence(Sequence.PPQ,480);Track control=seq.createTrack(),music=seq.createTrack();tempo(control,500000,0);tempo(control,1000000,480);
         ShortMessage program=new ShortMessage();program.setMessage(ShortMessage.PROGRAM_CHANGE,0,32,0);music.add(new MidiEvent(program,0));note(music,0,40,100,0);note(music,0,40,0,200);note(music,0,43,100,480);note(music,9,38,100,960);
         MidiCompiler.Song s=MidiCompiler.parse(encode(seq),0,120);eq(3,s.notes().size(),"velocity-zero note on is off");eq(1,s.notes().get(0).instrument(),"GM bass");eq(5,s.notes().get(1).at(),"first half second");eq(15,s.notes().get(2).at(),"tempo change across other track");eq(2,s.notes().get(2).instrument(),"channel ten snare");
         s=MidiCompiler.parse(encode(seq),.5,1.1);eq(2,s.notes().size(),"explicit window");eq(0,s.notes().get(0).at(),"relative window clock");eq(1,s.outside(),"outside selection reported");
         Sequence smpte=new Sequence(Sequence.SMPTE_25,40);note(smpte.createTrack(),0,66,100,1000);eq(10,MidiCompiler.parse(encode(smpte),0,120).notes().get(0).at(),"SMPTE one second");
         boolean bad=false;try{MidiCompiler.parse(new byte[]{1,2,3},0,120);}catch(Exception e){bad=true;}check(bad,"bad MIDI rejected");
         bad=false;try{MidiCompiler.parse(MidiCompiler.demo(),Double.NaN,120);}catch(Exception e){bad=true;}check(bad,"NaN range rejected");
         Sequence dense=new Sequence(Sequence.PPQ,480);Track track=dense.createTrack();for(int i=0;i<7;i++)note(track,0,60+i,100,0);bad=false;try{MidiCompiler.parse(encode(dense),0,120);}catch(IOException e){bad=true;}check(bad,"no silent chord truncation");
      });
      test("midi: file preview/confirmation, path safety, piston tuning and corrupt map",()->{
         Game g=newGame(1);World w=g.world;Path root=Files.createTempDirectory("midi-files");setField(g,"root",root.toFile());MidiCompiler.exportDemo(g);byte[] original=Files.readAllBytes(root.resolve("midi/Workshop.mid"));MidiCompiler.exportDemo(g);check(Arrays.equals(original,Files.readAllBytes(root.resolve("midi/Workshop.mid"))),"demo never overwrites file");
         w.setBlock(-3,74,-22,MidiCompiler.BLOCK);g.target=hit(-3,74,-22,0,1,0);MidiCompiler.command(g,new String[]{"midi","Workshop.mid"});check(MidiCompiler.pending.containsKey(w),"valid file preview");eq(0,w.getBlock(0,75,-20),"preview does not place anything");MidiCompiler.command(g,new String[]{"midibuild"});eq(Redstone319.STONE_BUTTON,w.getBlock(0,75,-20),"explicit confirmation builds");
         boolean refused=false;try{MidiCompiler.command(g,new String[]{"midi","../other.mid"});}catch(IOException e){refused=true;}check(refused,"path traversal rejected");check(!MidiCompiler.pending.containsKey(w),"failed preview clears old confirmation");
         w.setBlock(8,65,8,Redstone319.PISTON+4);w.setBlock(9,65,8,Redstone319.NOTE_BLOCK);NoteBlocks.tune(w,9,65,8,24);w.setBlock(7,65,8,Redstone319.REDSTONE_BLOCK);Redstone319.propagate(w,7,65,8);eq(Redstone319.NOTE_BLOCK,w.getBlock(10,65,8),"piston moved note");eq(24,NoteBlocks.pitch(w,10,65,8),"piston preserved pitch");check(!w.redstone.notes.containsKey(World.pack(9,65,8)),"no ghost note left behind");
         w.redstone.opened.add(Redstone319.pack(10,65,8));w.setBlock(10,65,8,0);check(!w.redstone.opened.contains(Redstone319.pack(10,65,8)),"power latch cleared using redstone position encoding");
         Path dir=Files.createTempDirectory("bad-note-map");byte[] broken="version=1\n123=99\n".getBytes();Files.write(dir.resolve("notes.properties"),broken);World bad=new World(1,dir.toFile());check(bad.redstone.notesSaveBlocked,"corruption prevents overwrite");NoteBlocks.save(bad);check(Arrays.equals(broken,Files.readAllBytes(dir.resolve("notes.properties"))),"corrupt tuning retained");bad.shutdown();w.shutdown();
      });
      test("notes: actual host-authoritative audio packets and malformed events",()->{
         Game g=newGame(1);World w=g.world;w.setBlock(8,65,8,Redstone319.NOTE_BLOCK);long key=World.pack(8,65,8);
         java.lang.reflect.Field inboxField=Net319.class.getDeclaredField("inbox"),host=Net319.class.getDeclaredField("hosting"),client=Net319.class.getDeclaredField("clientConnected");inboxField.setAccessible(true);host.setAccessible(true);client.setAccessible(true);
         @SuppressWarnings("unchecked") Queue<String> inbox=(Queue<String>)inboxField.get(null);boolean oh=host.getBoolean(null),oc=client.getBoolean(null);
         try{host.setBoolean(null,false);client.setBoolean(null,true);inbox.add(Net319.rasterPacket(w,4,key,"6,12"));Net319.processInbox(w,g);eq("note_bell",SoundEngine.lastPlayedSound(),"client plays host-selected instrument");
            NoteBlocks.receive(w,key,"999,12");eq("note_bell",SoundEngine.lastPlayedSound(),"invalid instrument ignored");
            host.setBoolean(null,true);client.setBoolean(null,false);inbox.add(Net319.rasterPacket(w,4,key,"5,12"));Net319.processInbox(w,g);eq("note_bell",SoundEngine.lastPlayedSound(),"host ignores forged client note event");
         }finally{host.setBoolean(null,oh);client.setBoolean(null,oc);inbox.clear();Net319.clearWorldState();w.shutdown();}
      });
      test("midi: actual dust/repeaters play all notes at compiled times without compiler",()->{
         Game g=newGame(1);World w=g.world;MidiCompiler.Plan p=MidiCompiler.plan(MidiCompiler.parse(MidiCompiler.demo(),0,120),0,75,-20);MidiCompiler.build(w,p);ticks(w,4);eq(0,w.redstone.noteHits.size(),"build is initially silent");
         long start=w.tickCount;Redstone319.use(g,p.x,p.y,p.z,Redstone319.STONE_BUTTON);ticks(w,110);
         eq(p.pitches.size(),w.redstone.noteHits.size(),"all eight real note blocks triggered");
         for(var e:p.times.entrySet())eq((long)e.getValue(),w.redstone.noteHits.getOrDefault(e.getKey(),-1000L)-start,"real onset tick "+World.uz(e.getKey()));
         w.redstone.noteHits.clear();start=w.tickCount;Redstone319.use(g,p.x,p.y,p.z,Redstone319.STONE_BUTTON);ticks(w,110);eq(8,w.redstone.noteHits.size(),"button replay, not one-shot hidden program");
         w.redstone.noteHits.clear();w.setBlock(p.x,p.y,p.z+1,0);Redstone319.use(g,p.x,p.y,p.z,Redstone319.STONE_BUTTON);ticks(w,110);eq(0,w.redstone.noteHits.size(),"broken repeater genuinely stops playback");w.shutdown();
      });
      test("midi: chord isolation, reserved-air preflight and durable tuning",()->{
         Game g=newGame(1);World w=g.world;List<MidiCompiler.Note> notes=new ArrayList<>();for(int t=0;t<3;t++)for(int i=0;i<6;i++)notes.add(new MidiCompiler.Note(t,2+i,i));
         MidiCompiler.Plan p=MidiCompiler.plan(new MidiCompiler.Song(notes,0,0,0),0,75,0);
         w.setBlock(2,76,3,Blocks.STONE);boolean refused=false;try{MidiCompiler.build(w,p);}catch(IOException e){refused=true;}check(refused,"reserved air obstruction refuses entire build");eq(0,w.getBlock(p.x,p.y,p.z),"no partial button on failure");w.setBlock(2,76,3,0);
         MidiCompiler.build(w,p);ticks(w,4);long start=w.tickCount;Redstone319.use(g,p.x,p.y,p.z,Redstone319.STONE_BUTTON);ticks(w,40);eq(18,w.redstone.noteHits.size(),"six-note chords all powered");for(var e:p.times.entrySet())eq((long)e.getValue(),w.redstone.noteHits.getOrDefault(e.getKey(),-1000L)-start,"isolated chord timings");
         Path dir=Files.createTempDirectory("note-save");setField(w,"dir",dir.toFile());w.saveEnabled=true;NoteBlocks.save(w);World reloaded=new World(1,dir.toFile());eq(w.redstone.notes,reloaded.redstone.notes,"all pitches survive world reload");
         long key=p.pitches.keySet().iterator().next();w.setBlock(World.ux(key),World.uy(key)+1,World.uz(key),Blocks.STONE);w.redstone.noteHits.remove(key);NoteBlocks.play(w,World.ux(key),World.uy(key),World.uz(key));check(!w.redstone.noteHits.containsKey(key),"blocked top silences note");w.setBlock(World.ux(key),World.uy(key),World.uz(key),0);check(!w.redstone.notes.containsKey(key),"breaking clears old tuning");
         w.shutdown();reloaded.shutdown();
      });
   }
}
