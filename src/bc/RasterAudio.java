package bc;

import java.util.*;
import java.util.concurrent.*;

/** Frame-locked speaker mixer. Runs inside SoundEngine, not on a separate unsynchronized music player.
 * The audio thread cannot advance beyond the last committed video frame. A stalled simulation holds
 * sound instead of letting the soundtrack run ahead. No JavaSound device is needed for mix() tests. */
final class RasterAudio {
   record Key(World world,long speaker){}
   record Cursor(short[] pcm,int start,int end,float left,float right,long received,long worldTick){}
   static final class Stream {volatile Cursor cursor;int pos=-1,lastStart=-1,ramp;short[] lastPcm;float smooth;boolean starved;}
   static final class Tone {final Key key;final short[] pcm;final float left,right;final long created=System.nanoTime();int pos;Tone(Key k,short[] p,float l,float r){key=k;pcm=p;left=l;right=r;}}
   static final Map<Key,Stream> streams=new ConcurrentHashMap<>();
   static final ConcurrentLinkedQueue<Tone> tones=new ConcurrentLinkedQueue<>();
   private static final Map<Integer,short[]> cache=new LinkedHashMap<>(128,.75F,true){protected boolean removeEldestEntry(Map.Entry<Integer,short[]> e){return size()>128;}};
   static volatile World listenerWorld;static volatile double listenerX,listenerY,listenerZ;static volatile float yaw;
   static volatile boolean playing;static volatile float duck=1;
   static void listener(Game g){
      if(g==null){streams.clear();tones.clear();listenerWorld=null;playing=false;return;}
      World w=g.world;boolean active=w!=null&&g.player!=null&&(g.screen==Game.S.PLAY||g.screen==Game.S.INV||g.screen==Game.S.CHAT);
      if(listenerWorld!=w||!active){streams.clear();tones.clear();}listenerWorld=w;playing=active;
      if(g.player!=null){listenerX=g.player.x;listenerY=g.player.y+g.player.eye;listenerZ=g.player.z;yaw=g.player.yaw;}
   }
   static float[] gains(World w,long key,float volume){
      if(w!=listenerWorld||!playing||volume<=0)return new float[]{0,0};
      double dx=World.ux(key)+.5-listenerX,dy=World.uy(key)+.5-listenerY,dz=World.uz(key)+.5-listenerZ,d=Math.sqrt(dx*dx+dy*dy+dz*dz);
      if(d>=48)return new float[]{0,0};double angle=Math.toRadians(yaw),side=(-Math.cos(angle)*dx-Math.sin(angle)*dz)/Math.max(1,d),pan=Math.max(-1,Math.min(1,side));
      float gain=(float)(volume*Math.min(1,8/Math.max(1,d))*(1-d/48));
      return new float[]{gain*(float)Math.sqrt((1-pan)*.5),gain*(float)Math.sqrt((1+pan)*.5)};
   }
   static void video(World w,long speaker,RasterClip clip,int frame,float volume){
      Key key=new Key(w,speaker);float[] g=gains(w,speaker,volume);
      if(g[0]+g[1]<.001F){streams.remove(key);return;}if(!streams.containsKey(key)&&streams.size()>=8)return;
      Stream s=streams.computeIfAbsent(key,k->new Stream());int start=frame*RasterClip.SAMPLES_PER_FRAME;
      s.cursor=new Cursor(clip.pcm,start,start+RasterClip.SAMPLES_PER_FRAME,g[0],g[1],System.nanoTime(),w.tickCount);
   }
   static void stop(World w,long speaker){Key k=new Key(w,speaker);streams.remove(k);tones.removeIf(t->t.key.equals(k));}
   static void stopWorld(World w){streams.keySet().removeIf(k->k.world==w);tones.removeIf(t->t.key.world==w);}
   static void endTick(World w){streams.entrySet().removeIf(e->e.getKey().world==w&&e.getValue().cursor.worldTick!=w.tickCount);}
   static synchronized short[] synth(int note,int ms,int instrument){
      note=Math.max(24,Math.min(108,note));ms=Math.max(20,Math.min(1000,ms));instrument&=3;int key=(note<<13)|(ms<<2)|instrument;short[] hit=cache.get(key);if(hit!=null)return hit;
      int n=ms*44100/1000;short[] pcm=new short[n];double freq=440*Math.pow(2,(note-69)/12.0);int rnd=note*31337+ms;
      for(int i=0;i<n;i++){double p=i*freq/44100,s;switch(instrument){case 1->s=2/Math.PI*Math.asin(Math.sin(2*Math.PI*p));case 2->s=Math.sin(2*Math.PI*p)+.22*Math.sin(6*Math.PI*p);case 3->{rnd^=rnd<<13;rnd^=rnd>>>17;rnd^=rnd<<5;s=(rnd&65535)/32768.0-1;}default->s=Math.sin(2*Math.PI*p);}double env=Math.min(1,i/160.0)*Math.min(1,(n-1-i)/300.0)*Math.exp(-2.3*i/n);pcm[i]=(short)(s*env*11000);}
      cache.put(key,pcm);return pcm;
   }
   static void tone(World w,long speaker,int note,int duration,int spec,float volume){
      float[] g=gains(w,speaker,volume*((spec>>>8)&15)/15F);if(g[0]+g[1]<.001F)return;
      while(tones.size()>=32)tones.poll();tones.add(new Tone(new Key(w,speaker),synth(note,duration,spec&3),g[0],g[1]));
   }
   /** Add to existing stereo buffers. The returned count helps verify that real PCM was mixed. */
   static int mix(float[] left,float[] right,int count,float effectsVolume){
      int mixed=0;boolean audible=false;long now=System.nanoTime();
      for(var e:streams.entrySet()){
         Stream s=e.getValue();Cursor c=s.cursor;if(c==null)continue;
         if(now-c.received>250_000_000L||e.getKey().world!=listenerWorld||!playing){streams.remove(e.getKey(),s);continue;}
         // Allow one frame plus one device block of slack: 512-sample device blocks do not divide 2205-sample video frames.
         // Resetting to every new frame would discard tails and cause a 20 Hz audio glitch.
         // Seeks/loops still reset immediately, and a stalled producer can never be run past.
         if(s.lastPcm!=c.pcm||s.pos<c.start-RasterClip.SAMPLES_PER_FRAME-512||s.pos>c.end||c.start<s.lastStart){s.pos=c.start;s.smooth=0;s.ramp=64;}
         else if(s.starved&&s.pos<c.end)s.ramp=64;
         s.starved=false;
         s.lastStart=c.start;s.lastPcm=c.pcm;
         for(int i=0;i<count&&s.pos<c.end&&s.pos<c.pcm.length;i++){
            float sample=c.pcm[s.pos]/32768F;float fade=Math.min(1,(c.end-s.pos)/64F)*(s.ramp>0?(64-s.ramp--)/64F:1);s.smooth+=.65F*(sample-s.smooth);
            left[i]+=s.smooth*fade*c.left*effectsVolume;right[i]+=s.smooth*fade*c.right*effectsVolume;s.pos++;mixed++;audible|=effectsVolume>.001F;
         }
         s.starved=s.pos>=c.end;
      }
      for(Tone t:tones){
         if(now-t.created>1_500_000_000L||t.key.world!=listenerWorld||!playing){tones.remove(t);continue;}
         for(int i=0;i<count&&t.pos<t.pcm.length;i++){float sample=t.pcm[t.pos++]/32768F;left[i]+=sample*t.left*effectsVolume;right[i]+=sample*t.right*effectsVolume;mixed++;}
         if(t.pos>=t.pcm.length)tones.remove(t);
      }
      duck+=( (audible?.08F:1F)-duck)*.12F;return mixed;
   }
}
