package bc;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** In-game local file picker. A preview owns an anchored plan parsed from a bounded file snapshot. */
final class MidiScreen {
   final Game g; final World world; final long anchor;
   Path directory,file; List<Path> entries=List.of(); boolean roots,browsing=true,raise=true,built;
   int page,rows=8,focus; String start="0",length="5",error="",notice="",playError="";
   MidiCompiler.Song song; MidiCompiler.Plan plan;
   static void open(Game g,long anchor)throws IOException{
      MidiCompiler.validate(g,g.world,anchor);MidiCompiler.pending.remove(g.world);MidiScreen screen=new MidiScreen(g,anchor);
      g.setScreen(Game.S.MIDI);g.midiScreen=screen;
   }
   MidiScreen(Game g,long anchor){this.g=g;world=g.world;this.anchor=anchor;Path downloads=home().resolve("Downloads");browse(Files.isDirectory(downloads)?downloads:home());}
   Path folder()throws IOException{Path p=g.root.toPath().resolve("midi");Files.createDirectories(p);return p;}
   static Path home(){return Path.of(System.getProperty("user.home","." )).toAbsolutePath();}
   static boolean midi(Path p){String n=p.getFileName()==null?"":p.getFileName().toString().toLowerCase(Locale.ROOT);return n.endsWith(".mid")||n.endsWith(".midi");}
   static String text(String s){return s.replaceAll("[\\p{Cntrl}§]"," ");}
   void invalidate(){MidiPlayback.stop(this);playError="";plan=null;song=null;built=false;notice="";error="";}
   void browse(Path path){
      invalidate();browsing=true;focus=0;page=0;roots=false;directory=path.toAbsolutePath().normalize();
      try(var stream=Files.list(directory)){
         entries=stream.filter(p->Files.isDirectory(p)||midi(p)).limit(10001).sorted(Comparator.<Path,Boolean>comparing(p->!Files.isDirectory(p)).thenComparing(p->p.getFileName().toString(),String.CASE_INSENSITIVE_ORDER)).toList();
         if(entries.size()>10000){entries=entries.subList(0,10000);error="Showing the first 10,000 entries. Choose a smaller folder.";}
      }catch(Exception e){entries=List.of();error=MidiCompiler.explain(e);}
   }
   void drives(){invalidate();browsing=true;focus=0;page=0;roots=true;directory=null;ArrayList<Path> p=new ArrayList<>();FileSystems.getDefault().getRootDirectories().forEach(p::add);entries=p;}
   void up(){if(directory==null||directory.getParent()==null)drives();else browse(directory.getParent());}
   void select(Path path){if(Files.isDirectory(path)){browse(path);return;}file=path.toAbsolutePath();browsing=false;focus=0;preview();}
   void preview(){
      invalidate();
      try{
         MidiCompiler.validate(g,world,anchor);if(file==null)throw new IOException("Choose a MIDI file first.");
         double from=Double.parseDouble(start),seconds=Double.parseDouble(length);
         song=MidiCompiler.parse(MidiCompiler.readMidi(file),from,seconds);
         plan=MidiCompiler.layout(world,song,anchor,raise);MidiCompiler.preflight(world,plan);
         notice="Ready. Build only places blocks in empty space; nothing is overwritten.";
      }catch(Exception e){error=MidiCompiler.explain(e);}
   }
   void play(boolean whole){
      MidiPlayback.stop(this);playError="";
      try{
         MidiCompiler.validate(g,world,anchor);
         // Listening never calls layout/preflight: even a rejected physical build can be heard.
         MidiCompiler.Song audio=MidiCompiler.playback(MidiCompiler.readMidi(file),whole?0:Double.parseDouble(start),whole?3600:Double.parseDouble(length),whole);
         MidiPlayback.play(this,audio,whole);
      }catch(Exception e){playError=MidiCompiler.explain(e);}
   }
   void checkPlayback(){
      if(MidiPlayback.active(this))try{MidiCompiler.validate(g,world,anchor);}catch(Exception e){MidiPlayback.stop(this);playError=MidiCompiler.explain(e);}
   }
   boolean ready(){return plan!=null&&error.isEmpty()&&!built;}
   void build(){
      if(!ready())return;
      MidiPlayback.stop(this);
      try{MidiCompiler.validate(g,world,anchor);MidiCompiler.build(world,plan);built=true;notice="Built! Press the stone button to play. The compiler and MIDI file are no longer needed.";g.say("MIDI built: "+plan.pitches.size()+" notes. Start button at "+plan.x+", "+plan.y+", "+plan.z+".");}
      catch(Exception e){error=MidiCompiler.explain(e);}
   }
   void goToButton(){
      if(!built||plan==null)return;
      if(g.world!=world||g.player==null||!g.player.creative||Net319.isClientConnected()&&!Net319.isHosting()){error="Return to this world as a Creative host first.";return;}
      g.player.setPos(plan.x+1.5,plan.y+2,plan.z-1.5);g.player.flying=true;g.player.vx=g.player.vy=g.player.vz=0;g.player.yaw=26.565F;g.player.pitch=(float)Math.toDegrees(Math.atan2(2+g.player.eye-.1,Math.sqrt(5)));g.setScreen(Game.S.PLAY);
   }
   void character(int cp){if(browsing||focus==0)return;if(cp>='0'&&cp<='9'||cp=='.'){String s=focus==1?start:length;if(s.length()<10){if(focus==1)start+=(char)cp;else length+=(char)cp;invalidate();}}}
   void key(int key,int action){
      if(action!=1&&action!=2)return;
      if(key==256){g.setScreen(Game.S.PLAY);return;}
      if(browsing){if(key==259)up();if(key==266)scroll(1);if(key==267)scroll(-1);return;}
      if(key==258){focus=focus==1?2:1;return;}
      if(key==257||key==335){focus=0;preview();return;}
      if(key==259&&focus!=0){String s=focus==1?start:length;if(!s.isEmpty()){if(focus==1)start=s.substring(0,s.length()-1);else length=s.substring(0,s.length()-1);invalidate();}}
   }
   void scroll(double amount){if(browsing&&amount!=0)page=Math.max(0,Math.min(pages()-1,page+(amount<0?1:-1)));}
   int pages(){return Math.max(1,(entries.size()+rows-1)/rows);}
   void button(int x,int y,int w,String label,Runnable action){g.ui.button(Game.clipText(label,w-10),x,y,w,20,action!=null,null,action);}
   void draw(){
      Ui u=g.ui;u.begin();u.backdrop(.7F);u.panel(8,8,g.gui.w-16,g.gui.h-16);u.title("MIDI BUILD COMPILER",18);
      if(browsing)drawBrowser();else drawPreview();
   }
   void drawBrowser(){
      Ui u=g.ui;int w=(int)g.gui.w,h=(int)g.gui.h,x=20,gap=4,bw=(w-40-gap*3)/4;
      button(x,36,bw,"Home",()->browse(home()));button(x+(bw+gap),36,bw,"Downloads",()->browse(home().resolve("Downloads")));
      button(x+2*(bw+gap),36,bw,"MIDI folder",()->{try{browse(folder());}catch(Exception e){error=MidiCompiler.explain(e);}});button(x+3*(bw+gap),36,bw,"Drives",this::drives);
      u.label(Game.clipText(text(directory==null?"Select a drive or filesystem root":directory.toString()),w-40),x,62,Ui.TEXT);
      rows=Math.max(1,(h-132)/20);page=Math.min(page,pages()-1);
      for(int i=0;i<rows;i++){int n=page*rows+i;if(n>=entries.size())break;Path p=entries.get(n);String name=p.getFileName()==null?p.toString():p.getFileName().toString();button(x,78+i*20,w-40,Game.clipText((Files.isDirectory(p)?"[Folder]  ":"[MIDI]  ")+text(name),w-56),()->select(p));}
      if(entries.isEmpty())u.label("No MIDI files or folders here.",x,80,Ui.TEXT);
      u.label(Game.clipText(text(error.isEmpty()?"Click a folder or MIDI file. Scroll to change pages. ("+(page+1)+" / "+pages()+")":error),w-40),x,h-46,error.isEmpty()?0xffb8d3df:0xffffa090);
      button(x,h-32,bw,"Up",this::up);button(x+bw+gap,h-32,bw,"< Previous",page>0?()->scroll(1):null);button(x+2*(bw+gap),h-32,bw,"Next >",page+1<pages()?()->scroll(-1):null);button(x+3*(bw+gap),h-32,bw,"Cancel",()->g.setScreen(Game.S.PLAY));
   }
   void drawPreview(){
      Ui u=g.ui;int w=(int)g.gui.w,h=(int)g.gui.h,x=20,gap=6,col=(w-40-2*gap)/3;
      u.label(Game.clipText(text(file==null?"No MIDI selected":file.getFileName().toString()),w-40),x,36,Ui.TEXT);
      field(x,52,col,"Start: ",start,1);field(x+col+gap,52,col,"Length: ",length,2);button(x+2*(col+gap),52,col,"Preview",this::preview);
      button(x,76,col*2+gap,"Build height: "+(raise?"above terrain":"at compiler"),()->{raise=!raise;focus=0;invalidate();});button(x+2*(col+gap),76,col,"Choose file",()->browse(file==null?home():file.getParent()));
      boolean compact=h<300;boolean diagram=w>=540&&h>=320;int content=diagram?w-198:w-40;
      u.label(Game.clipText("Section defaults to 5s. Play full ignores it.",w-40),x,102,0xffb8d3df);
      int y=116;
      if(song!=null){
         u.label(Game.clipText(song.notes().size()+" notes / "+song.notes().stream().map(MidiCompiler.Note::instrument).distinct().count()+" instruments / selected "+length+"s",content),x,y,Ui.TEXT);y+=12;
         if(!compact){u.label(Game.clipText(song.outside()+" outside section / "+song.folded()+" octave-folded / "+song.merged()+" merged",content),x,y,Ui.TEXT);y+=12;}
      }
      if(plan!=null){u.label(Game.clipText("Footprint 16 x "+(plan.length+2)+" x 5 / "+plan.repeaters+" repeaters",content),x,y,Ui.TEXT);y+=12;if(!compact){u.label(Game.clipText("Start button: "+plan.x+", "+plan.y+", "+plan.z,content),x,y,Ui.TEXT);y+=12;}}
      String message=!playError.isEmpty()?"Audio: "+playError:MidiPlayback.active(this)?"Listening locally with note-block sounds. No blocks are placed. Stop or close this menu to end playback.":!error.isEmpty()?"Build: "+error:!notice.isEmpty()?notice:"Choose start and length, then Preview. Length must be positive, up to 120s. Rail limit: 256 blocks.";
      u.paragraph(text(message),x,y+4,content,playError.isEmpty()&&(error.isEmpty()||MidiPlayback.active(this))?0xffc4e1cb:0xffffa090,Math.max(1,(h-90-y)/10));
      if(diagram&&plan!=null)map(w-162,112,140,Math.min(150,h-200));
      u.label(Game.clipText(g.opt!=null&&g.opt.soundVolume<=.001F&&MidiPlayback.active(this)?"Muted: raise Options > Sounds volume.":MidiPlayback.status(this),w-40),x,h-74,0xff9dd9eb);
      button(x,h-60,col,"Play full",file!=null?()->play(true):null);
      button(x+col+gap,h-60,col,"Play section",file!=null?()->play(false):null);
      button(x+2*(col+gap),h-60,col,"Stop",MidiPlayback.active(this)?()->MidiPlayback.stop(this):null);
      button(x,h-32,col,built?"Built": "Build circuit",ready()?this::build:null);
      button(x+col+gap,h-32,col,"Go to button",built?this::goToButton:null);button(x+2*(col+gap),h-32,col,"Close",()->g.setScreen(Game.S.PLAY));
   }
   void field(int x,int y,int w,String label,String value,int id){g.ui.field(x,y,w,20,Game.clipText(label+value+(focus==id?"_":"")+" s",w-12),"",focus==id,()->focus=id);}
   void map(int x,int y,int w,int h){
      g.ui.panel(x,y,w,h);g.ui.label("TOP VIEW / +Z DOWN",x+8,y+8,Ui.TEXT);float scale=Math.min((w-20)/16f,(h-30)/(float)(plan.length+2));float left=x+(w-16*scale)/2,top=y+24;
      for(var entry:plan.blocks.entrySet()){long k=entry.getKey();if(World.uy(k)!=plan.y)continue;int id=entry.getValue()&4095;int color=id==Redstone319.NOTE_BLOCK?0xffeeb65d:id>=Redstone319.REPEATER_OFF&&id<Redstone319.REPEATER_OFF+8?0xffff6050:id>=Redstone319.REDSTONE_WIRE0&&id<Redstone319.REDSTONE_WIRE0+16?0xffa23730:0xff6b7880;g.gui.rect(left+(World.ux(k)-plan.x+1)*scale,top+(World.uz(k)-plan.z+1)*scale,Math.max(1,scale),Math.max(1,scale),color);}
   }
   /** Native screenshot automation: real right-click, picker selection and Build widget. */
   static boolean smokeOpened,smokeBuilt;static long smokeAnchor;static int smokeAudioStep;
   static void smoke(Game g,int frame){
      try{
         if(!smokeOpened&&frame>=240&&g.world!=null&&g.player!=null){
            int x=(int)Math.floor(g.player.x),z=(int)Math.floor(g.player.z),y=Math.min(220,Math.max(80,(int)g.player.y+8));for(int cx=(x>>4)-1;cx<=(x>>4)+4;cx++)for(int cz=(z>>4)-1;cz<=(z>>4)+4;cz++)g.world.ensureChunkLoaded(cx,cz);
            for(int xx=x;xx<x+20;xx++)for(int zz=z;zz<z+60;zz++)for(int yy=y-3;yy<y+15;yy++)g.world.setBlock(xx,yy,zz,0);
            g.world.setBlock(x,y,z,MidiCompiler.BLOCK);g.player.creative=true;smokeAnchor=World.pack(x,y,z);
            MidiCompiler.exportDemo(g);MidiCompiler.use(g,x,y,z,MidiCompiler.BLOCK);g.midiScreen.browse(g.midiScreen.folder());smokeOpened=true;
         }
         if(smokeBuilt&&frame>=255)g.autoShotNow=true;
         if(smokeOpened&&!smokeBuilt&&frame>=245&&g.midiScreen!=null){
            if(g.midiScreen.browsing){click(g,"Workshop.mid");}
            else if("listen".equals(System.getProperty("bc.midigui"))){
               MidiScreen screen=g.midiScreen;
               if(smokeAudioStep==0){
                  click(g,"Play full");if(!MidiPlayback.active(screen))throw new IOException(screen.playError);
                  double energy=0;float[] l=new float[512],r=new float[512];
                  while(MidiPlayback.active(screen)){Arrays.fill(l,0);Arrays.fill(r,0);MidiPlayback.mix(l,r,512,1);for(int i=0;i<l.length;i++){if(l[i]!=r[i]||!Float.isFinite(l[i]))throw new IOException("Invalid stereo MIDI PCM");energy+=l[i]*l[i];}}
                  if(MidiPlayback.triggered(screen)!=8||energy<1)throw new IOException("MIDI audio missing notes or PCM");
                  click(g,"Play full");smokeAudioStep=1;
               }else if(smokeAudioStep==1){click(g,"Stop");if(MidiPlayback.active(screen))throw new IOException("Stop failed");smokeAudioStep=2;}
               else{
                  click(g,"Play full");float[] l=new float[44100],r=new float[44100];MidiPlayback.mix(l,r,l.length,1);
                  if(!MidiPlayback.active(screen)||!g.world.redstone.notes.isEmpty())throw new IOException("Listening created blocks or failed to restart");
                  smokeBuilt=true;System.out.println("[midi-listen] full song=8 notes; centered finite PCM; Stop/Restart OK; zero placed note blocks");
               }
            }
            else{click(g,"Build circuit");if(!g.midiScreen.built)throw new IOException(g.midiScreen.error);smokeBuilt=true;System.out.println("[midi-gui] picker+preview+build OK notes="+g.midiScreen.plan.pitches.size()+" anchor="+smokeAnchor);}
         }
      }catch(Exception e){throw new RuntimeException("MIDI GUI smoke failed",e);}
   }
   static void click(Game g,String label){for(Game.Btn b:g.activeBtns)if(b.label!=null&&b.label.contains(label)&&b.action!=null){g.mouseX=(b.x+2)*g.gui.scale;g.mouseY=(b.y+2)*g.gui.scale;g.onMouse(0,1);g.onMouse(0,0);return;}throw new IllegalStateException("Missing GUI button: "+label);}
}
