package bc;

import java.nio.file.*;
import static bc.GameTests.*;

/** Real widgets/input, anchored previews, and atomic preflight regression tests. */
final class TestsMidiGui {
   public static void main(String[] args){run();System.out.println("MIDI GUI: "+passed+" passed, "+failed+" failed");System.exit(failed==0?0:1);}
   static Game setup()throws Exception{
      Game g=newGame(1);Path root=Files.createTempDirectory("midi-gui");setField(g,"root",root.toFile());g.opt=new Options(root.resolve("options").toFile());
      g.world.setBlock(-3,61,-22,MidiCompiler.BLOCK);MidiCompiler.use(g,-3,61,-22,MidiCompiler.BLOCK);return g;
   }
   static GuiRaster draw(Game g,int w,int h)throws Exception{
      GuiRaster r=new GuiRaster(w*2,h*2);g.gui=r;g.fbW=g.winW=w*2;g.fbH=g.winH=h*2;r.begin(w*2,h*2,2);g.activeBtnsSwap();g.midiScreen.draw();g.activeBtns=g.btns;
      for(Game.Btn b:g.btns){check(b.x>=0&&b.y>=0&&b.x+b.w<=w&&b.y+b.h<=h,"widget inside window");if(b.label!=null)check(Gui.textWidth(b.label)+4<=b.w,"label fits: "+b.label);}
      for(int i=0;i<g.btns.size();i++)for(int j=i+1;j<g.btns.size();j++)check(!TestsUi.overlap(g.btns.get(i),g.btns.get(j)),"widgets do not overlap");
      check(r.textExtent[0]>=0&&r.textExtent[1]>=0&&r.textExtent[2]<=w&&r.textExtent[3]<=h,"text inside window");return r;
   }
   static void run(){
      test("midi GUI: direct Unicode/spaced file picker, real buttons, anchor and physical playback",()->{
         Game g=setup();World w=g.world;MidiScreen s=g.midiScreen;Path folder=g.root.toPath().resolve("Downloads from elsewhere");Files.createDirectories(folder);Path file=folder.resolve("DSi Shop sample ü.MIDI");Files.write(file,MidiCompiler.demo());Files.writeString(folder.resolve("ignore.txt"),"not music");
         s.browse(folder);eq(1,s.entries.size(),"only MIDI files displayed");draw(g,640,360);MidiScreen.click(g,"[MIDI]");check(!s.browsing&&s.ready(),"file click automatically previews");eq(file,s.file,"direct path retained");check(s.plan.y>=63,"ground compiler automatically lifted");eq(0,w.getBlock(s.plan.x,s.plan.y,s.plan.z),"preview is read-only");
         g.target=null;Files.delete(file);draw(g,640,360);MidiScreen.click(g,"Build circuit");check(s.built,"build uses captured compiler and preview, not crosshair or file reread");eq(Redstone319.STONE_BUTTON,w.getBlock(s.plan.x,s.plan.y,s.plan.z),"real button placed");
         w.setBlock(-3,61,-22,0);TestsMidi.ticks(w,4);Redstone319.use(g,s.plan.x,s.plan.y,s.plan.z,Redstone319.STONE_BUTTON);TestsMidi.ticks(w,110);eq(8,w.redstone.noteHits.size(),"all notes played without compiler/file");
         draw(g,640,360);MidiScreen.click(g,"Go to button");eq(Game.S.PLAY,g.screen,"button navigation returns to game");check(g.midiScreen==null,"session released on close");double yaw=Math.toRadians(g.player.yaw),pitch=Math.toRadians(g.player.pitch);var aimed=w.raycast(g.player.x,g.player.y+g.player.eye,g.player.z,-Math.sin(yaw)*Math.cos(pitch),-Math.sin(pitch),Math.cos(yaw)*Math.cos(pitch),5);check(aimed!=null&&aimed.x==s.plan.x&&aimed.y==s.plan.y&&aimed.z==s.plan.z,"go-to aims within reach of real button");w.shutdown();
      });
      test("midi GUI: stale fields, blocked space, removed compiler, numeric errors and permissions",()->{
         Game g=setup();World w=g.world;MidiScreen s=g.midiScreen;Path file=g.root.toPath().resolve("test.mid");Files.write(file,MidiCompiler.demo());s.select(file);check(s.ready(),"valid preview");
         draw(g,400,300); // focus first actual field using mouse input
         g.mouseX=44;g.mouseY=110;g.onMouse(0,1);g.onMouse(0,0);g.onChar('1');check(s.plan==null&&!s.ready(),"typing invalidates old preview");g.onKey(259,1,0);eq("0",s.start,"backspace routed to field");g.onKey(257,1,0);check(s.ready(),"enter previews");
         MidiCompiler.Plan p=s.plan;w.setBlock(p.x+1,p.y+2,p.z,Blocks.STONE);s.build();check(!s.built&&s.error.contains("occupied"),"fresh preflight detects changed world");eq(0,w.getBlock(p.x,p.y,p.z),"blocked build is atomic");s.preview();check(s.ready()&&s.plan.y>p.y,"explicit re-preview safely raises above obstruction");
         w.setBlock(-3,61,-22,0);s.build();check(s.error.contains("removed"),"removed anchor rejected");w.setBlock(-3,61,-22,MidiCompiler.BLOCK);s.preview();g.player.creative=false;s.build();check(s.error.contains("Creative"),"permission rechecked at build");g.player.creative=true;
         s.start="";s.preview();check(s.error.contains("numbers"),"actionable numeric error");s.start="0";s.length="0";s.preview();check(!s.ready(),"zero section refused");s.length="5";s.raise=false;s.preview();check(s.error.contains("occupied"),"fixed ground-level build explains clearance");
         s.browse(file.resolveSibling("missing folder"));check(!s.error.isEmpty()&&s.plan==null,"unreadable directory is recoverable and clears preview");s.drives();check(!s.entries.isEmpty(),"filesystem roots available");g.onKey(256,1,0);eq(Game.S.PLAY,g.screen,"escape closes");w.shutdown();
      });
      test("midi GUI: bounded files and MIDI-specific chat errors",()->{
         Game g=setup();MidiScreen s=g.midiScreen;Path file=g.root.toPath().resolve("bad.mid");Files.write(file,new byte[MidiCompiler.MAX_BYTES+1]);s.select(file);check(s.error.contains("1 MiB"),"bounded file reader");Files.write(file,"this is not a MIDI file".getBytes());s.select(file);check(s.error.contains("standard MIDI"),"invalid format diagnostic");
         g.setScreen(Game.S.PLAY);g.runCommand("/midibuild");check(g.chatLog.get(g.chatLog.size()-1).contains("No successful MIDI preview"),"command error no longer hidden");g.world.shutdown();
      });
      test("midi GUI: responsive browser/preview/error/success layouts and pagination",()->{
         Game g=setup();MidiScreen s=g.midiScreen;Path folder=g.root.toPath().resolve("many files");Files.createDirectories(folder);for(int i=0;i<30;i++)Files.write(folder.resolve(String.format("Song %02d.mid",i)),MidiCompiler.demo());
         for(int[] size:TestsUi.SIZES){s.browse(folder);draw(g,size[0],size[1]);g.onScroll(-1);check(s.page==1,"wheel changes page");s.select(folder.resolve("Song 00.mid"));draw(g,size[0],size[1]);s.error="Build cancelled: occupied/reserved space at 123,456,789. Preview again with build height above terrain, or move the compiler.";draw(g,size[0],size[1]);}
         s.preview();s.build();draw(g,320,240);Files.createDirectories(Path.of("build/midi-gui-shots"));draw(g,640,360).save("build/midi-gui-shots/built.png");s.browse(folder);draw(g,640,360).save("build/midi-gui-shots/browser.png");draw(g,320,240).save("build/midi-gui-shots/small.png");g.world.shutdown();
      });
   }
}
