package bc;

import java.nio.file.*;
import java.util.*;
import javax.sound.midi.*;
import static bc.GameTests.*;

final class TestsMidiPlayback {
   static byte[] longMidi()throws Exception{Sequence seq=new Sequence(Sequence.PPQ,480);Track t=seq.createTrack();for(int i=0;i<600;i++)TestsMidi.note(t,0,54+i%25,100,i*480L);return TestsMidi.encode(seq);}
   static float[] render(Object owner,MidiCompiler.Song song,int count,int chunk){
      MidiPlayback.play(owner,song,true);float[] all=new float[count];int at=0;
      while(at<count){int n=Math.min(chunk,count-at);float[] l=new float[n],r=new float[n];MidiPlayback.mix(l,r,n,1);System.arraycopy(l,0,all,at,n);check(Arrays.equals(l,r),"local stereo is centered");at+=n;}return all;
   }
   static void run(){
      test("MIDI listening: full song bypasses physical duration/note/rail limits with bounded parsing",()->{
         byte[] bytes=longMidi();MidiCompiler.Song full=MidiCompiler.playback(bytes,0,0,true);eq(600,full.notes().size(),"whole file, not first five seconds or 512 notes");eq(2995,full.notes().get(599).at(),"five-minute timeline retained");eq(0,full.outside(),"nothing silently discarded");
         Object owner=new Object();try{MidiPlayback.play(owner,full,true);float[] l=new float[512],r=new float[512];long frames=0;while(MidiPlayback.active(owner)){Arrays.fill(l,0);Arrays.fill(r,0);frames+=MidiPlayback.mix(l,r,512,1);}eq(600,MidiPlayback.triggered(owner),"entire five-minute song reaches the last note");check(frames>299L*44100,"sample clock advances beyond old 120s cap");}finally{MidiPlayback.stop(owner);}
         boolean refused=false;try{MidiCompiler.plan(full,0,75,0);}catch(Exception e){refused=true;}check(refused,"physical rail remains limited");refused=false;try{MidiCompiler.parse(bytes,0,300);}catch(Exception e){refused=true;}check(refused,"build duration remains limited");
         MidiCompiler.Song section=MidiCompiler.playback(bytes,200,10,false);eq(20,section.notes().size(),"play section uses fields even beyond build duration");eq(0,section.notes().get(0).at(),"section onset relative to selected start");
         Sequence seq=new Sequence(Sequence.PPQ,480);Track t=seq.createTrack();TestsMidi.note(t,0,60,100,3601L*960);refused=false;try{MidiCompiler.playback(TestsMidi.encode(seq),0,0,true);}catch(Exception e){refused=true;}check(refused,"over-hour full file refused, not truncated");
         seq=new Sequence(Sequence.PPQ,480);t=seq.createTrack();for(int ch=0;ch<2;ch++){ShortMessage m=new ShortMessage();m.setMessage(ShortMessage.PROGRAM_CHANGE,ch,ch==0?0:32,0);t.add(new MidiEvent(m,0));for(int n=0;n<16;n++)TestsMidi.note(t,ch,ch==0?54+n:30+n,100,0);}
         eq(32,MidiCompiler.playback(TestsMidi.encode(seq),0,0,true).notes().size(),"audio is not restricted to six physical lanes");TestsMidi.note(t,0,78,100,0);refused=false;try{MidiCompiler.playback(TestsMidi.encode(seq),0,0,true);}catch(Exception e){refused=e.getMessage().contains("32");}check(refused,"bounded 32-note chord, no silent voice drops");
      });
      test("MIDI listening: exact note bank, pitch range, sample clock and finite centered PCM",()->{
         Object owner=new Object();Set<Integer> waveforms=new HashSet<>();
         try{
            for(int instrument=0;instrument<16;instrument++)for(int pitch:new int[]{0,12,24}){
               MidiCompiler.Note n=new MidiCompiler.Note(0,pitch,instrument);SoundEngine.Sample sample=SoundEngine.getSample("note_"+NoteBlocks.NAMES[instrument]);MidiPlayback.Voice voice=new MidiPlayback.Voice(n,sample);
               check(voice.sample==sample,"uses actual shared note-block bank");check(Math.abs(voice.step-sample.sampleRate/(double)44100*Math.pow(2,(pitch-12)/12.0))<1e-12,"same two-octave pitch multiplier");
               float[] pcm=render(owner,new MidiCompiler.Song(List.of(n),0,0,0),12000,512);double energy=0;for(int i=0;i<pcm.length;i++){check(Float.isFinite(pcm[i])&&Math.abs(pcm[i])<=.85,"bounded PCM");if(i<=4410)eq(0F,pcm[i],"100ms lead-in at exact sample");energy+=pcm[i]*pcm[i];}check(energy>0,"audible strike");if(pitch==12)waveforms.add(Arrays.hashCode(pcm));
            }
            eq(16,waveforms.size(),"all sixteen distinct note timbres");
            MidiCompiler.Song song=MidiCompiler.parse(MidiCompiler.demo(),0,120);float[] a=render(owner,song,44100*6,512),b=render(owner,song,44100*6,137);check(Arrays.equals(a,b),"block-size-independent sample-accurate clock");check(!MidiPlayback.active(owner),"ends after final sample tail");check(MidiPlayback.status(owner).startsWith("Finished"),"finished state reported");
            MidiPlayback.play(owner,song,true);float[] l=new float[10000],r=new float[10000];MidiPlayback.mix(l,r,l.length,0);check(Arrays.equals(l,new float[l.length]),"effects volume mutes MIDI");MidiPlayback.stop(owner);eq(0,MidiPlayback.mix(l,r,100,1),"stop removes notes and tails");eq(1F,MidiPlayback.musicGain(),"background music restored");
         }finally{MidiPlayback.stop(owner);}
      });
      test("MIDI listening: real GUI buttons work despite impossible build, without world mutation",()->{
         Game g=TestsMidiGui.setup();MidiScreen s=g.midiScreen;Path file=g.root.toPath().resolve("Long song.MIDI");Files.write(file,longMidi());s.select(file);s.length="300";s.preview();check(!s.ready(),"physical preview refused");
         long tick=g.world.tickCount;double x=g.player.x,y=g.player.y,z=g.player.z;int tunes=g.world.redstone.notes.size();
         TestsMidiGui.draw(g,640,360);MidiScreen.click(g,"Play full");check(MidiPlayback.active(s)&&s.playError.isEmpty(),"whole song plays without build readiness");check(MidiPlayback.status(s).contains("600 notes"),"whole-file count reported");
         float[] l=new float[10000],r=new float[10000];MidiPlayback.mix(l,r,l.length,1);check(!Arrays.equals(l,new float[l.length]),"audio advances while world is paused");eq(tick,g.world.tickCount,"no simulation tick required");eq(x,g.player.x,"no movement x");eq(y,g.player.y,"no movement y");eq(z,g.player.z,"no movement z");eq(tunes,g.world.redstone.notes.size(),"no blocks/tuning created");eq(0,g.world.redstone.noteHits.size(),"not fake block playback");
         for(int[] size:TestsUi.SIZES)TestsMidiGui.draw(g,size[0],size[1]);TestsMidiGui.draw(g,640,360);Files.createDirectories(Path.of("build/midi-player-shots"));((GuiRaster)g.gui).save("build/midi-player-shots/playing.png");
         MidiScreen.click(g,"Stop");check(!MidiPlayback.active(s),"real Stop widget");s.start="200";s.length="10";TestsMidiGui.draw(g,640,360);MidiScreen.click(g,"Play section");check(MidiPlayback.status(s).contains("20 notes"),"real section widget");
         s.focus=1;g.onChar('1');check(!MidiPlayback.active(s),"editing stops stale audio");s.play(true);Files.delete(file);check(MidiPlayback.active(s),"playing song owns its parsed snapshot");g.onKey(256,1,0);check(!MidiPlayback.active(s),"Escape stops all audio");
         MidiScreen.open(g,World.pack(-3,61,-22));s=g.midiScreen;Files.write(file,MidiCompiler.demo());s.select(file);s.play(true);g.world.setBlock(-3,61,-22,0);s.checkPlayback();check(!MidiPlayback.active(s)&&s.playError.contains("removed"),"removed compiler stops session");g.setScreen(Game.S.PLAY);g.world.shutdown();
      });
   }
}
