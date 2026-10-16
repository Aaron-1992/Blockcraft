package bc;

import java.util.*;

/** Local note-block listening, clocked by the audio device rather than world ticks/render FPS.
 * No MIDI synthesizer, pre-rendered whole-song PCM, blocks, positional sound or network packets.
 * All public operations synchronize with the audio mixer, so Stop discards every note/tail together. */
final class MidiPlayback {
   static final int RATE=SoundEngine.MIX_RATE,GRID=RATE/10;
   static final class Voice {
      final SoundEngine.Sample sample;final long onset;final double step;final long end;
      Voice(MidiCompiler.Note note,SoundEngine.Sample sample){this.sample=sample;onset=(note.at()+1L)*GRID;step=sample.sampleRate/(double)RATE*Math.pow(2,(note.pitch()-12)/12.0);end=onset+(long)Math.ceil(sample.pcm.length/step);}
      float at(long frame){int index=(int)((frame-onset)*step);return index>=0&&index<sample.pcm.length?sample.pcm[index]/32768F:0;}
   }
   static final class Session {
      final Object owner;final Voice[] notes;final ArrayList<Voice> ringing=new ArrayList<>();
      final boolean whole;final int folded,merged;final long end;long frame;int next;
      Session(Object owner,MidiCompiler.Song song,boolean whole){
         this.owner=Objects.requireNonNull(owner);this.whole=whole;folded=song.folded();merged=song.merged();
         SoundEngine.Sample[] bank=new SoundEngine.Sample[16];for(int i=0;i<16;i++){bank[i]=SoundEngine.getSample("note_"+NoteBlocks.NAMES[i]);if(bank[i]==null)throw new IllegalStateException("Missing note-block instrument: "+NoteBlocks.NAMES[i]);}
         notes=new Voice[song.notes().size()];long end=0;int i=0;for(MidiCompiler.Note note:song.notes()){Voice voice=new Voice(note,bank[note.instrument()]);notes[i++]=voice;end=Math.max(end,voice.end);}this.end=end;
      }
   }
   private static Session current;
   static void play(Object owner,MidiCompiler.Song song,boolean whole){
      // Resolve the bank outside the mixer lock: SoundEngine loading can start the audio thread.
      Session next=new Session(owner,song,whole);synchronized(MidiPlayback.class){current=next;}
   }
   static synchronized void stop(Object owner){if(current!=null&&current.owner==owner)current=null;}
   static synchronized boolean active(Object owner){return current!=null&&current.owner==owner&&current.frame<current.end;}
   static synchronized int triggered(Object owner){return current!=null&&current.owner==owner?current.next:0;}
   static synchronized float musicGain(){return current!=null&&current.frame<current.end?.08F:1F;}
   static String time(long frame){long seconds=frame/RATE;return String.format(Locale.ROOT,"%02d:%02d",seconds/60,seconds%60);}
   static synchronized String status(Object owner){
      Session s=current;if(s==null||s.owner!=owner)return "Local audio; closing this menu stops playback.";
      return (s.frame>=s.end?"Finished":s.whole?"Full song":"Section")+" "+time(s.frame)+" / "+time(s.end)+" | "+s.notes.length+" notes, "+s.folded+" folds, "+s.merged+" merged";
   }
   /** Mix bounded chunks of the actual note-block bank, including overlapping decays. */
   static synchronized int mix(float[] left,float[] right,int count,float volume){
      Session s=current;if(s==null)return 0;int rendered=0;float gain=Math.max(0,Math.min(1,volume));
      for(int i=0;i<count&&s.frame<s.end;i++,s.frame++){
         while(s.next<s.notes.length&&s.notes[s.next].onset<=s.frame)s.ringing.add(s.notes[s.next++]);
         double sample=0;for(int n=s.ringing.size()-1;n>=0;n--){Voice v=s.ringing.get(n);if(s.frame>=v.end)s.ringing.remove(n);else sample+=v.at(s.frame);}
         // Soft-limit dense overlaps while leaving room for UI/world effects in the shared mixer.
         float value=(float)Math.tanh(sample*.24)*.85F*gain;left[i]+=value;right[i]+=value;rendered++;
      }
      return rendered;
   }
}
