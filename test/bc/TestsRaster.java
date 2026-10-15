package bc;

import java.nio.file.*;
import java.util.*;
import static bc.GameTests.*;

public final class TestsRaster {
   static final class Rig implements RasterVM.IO {
      final RasterVM.Cpu c=new RasterVM.Cpu();final int[] mem=new int[4096],code=RasterPrograms.falling();int keys,held,frames;
      public int input(int p){if(p==0){int k=keys;keys=0;return k;}return p==1?held:p==2?c.width:p==3?c.height:0;}
      public void present(RasterVM.Cpu c){frames++;}
      void tick(int key){keys|=key;int before=frames;for(int n=0;n<8&&frames==before;n++)RasterVM.step(c,code,mem,this);check(c.fault.isEmpty(),"ROM fault: "+c.fault);check(frames>before,"ROM yields a displayed frame within eight bounded slices");}
      int occupied(){int n=0;for(int i=0;i<200;i++)if(mem[i]!=0)n++;return n;}
      void piece(int type,int rotation,int x){c.r[10]=x;c.r[11]=0;c.r[12]=rotation;c.r[13]=type;c.r[15]=type+1;c.r[26]=RasterPrograms.SHAPES[type*4+rotation];}
   }
   static void ticks(World w,int n){for(int i=0;i<n;i++)w.tick();}
   public static void main(String[] args){run();System.out.println("Raster: "+passed+" passed, "+failed+" failed");System.exit(failed==0?0:1);}
   static void run(){
      test("raster: all tetrominoes, real ROM startup, pause, movement, hard drop, restart, game over",()->{
         for(int mask:RasterPrograms.SHAPES)eq(4,Integer.bitCount(mask),"four occupied cells in each rotation");
         Rig r=new Rig();r.tick(0);eq(1,r.c.out[3],"starts paused");eq(0,r.occupied(),"empty board");
         r.tick(64);eq(0,r.c.out[3],"Enter starts");r.tick(32);int y=r.c.r[11];for(int i=0;i<50;i++)r.tick(0);eq(y,r.c.r[11],"pause stops gravity");
         r.tick(32);for(int i=0;i<20;i++)r.tick(0);check(r.c.r[11]>y,"gravity advances");
         for(int i=0;i<20;i++)r.tick(1);check(r.c.r[10]>=-2,"left boundary enforced");for(int i=0;i<30;i++)r.tick(2);check(r.c.r[10]<=9,"right boundary enforced");
         r.tick(16);eq(4,r.occupied(),"hard drop stamps exactly four cells");
         r.tick(64);eq(0,r.occupied(),"restart clears RAM board");for(int i=0;i<50&&r.c.out[4]==0;i++)r.tick(16);eq(1,r.c.out[4],"stacking blocks causes game over");
         int score=r.c.out[0];r.tick(16);eq(score,r.c.out[0],"game over ignores drops");r.tick(64);eq(0,r.c.out[4],"restart clears game-over state");
      });
      test("raster: row compaction, four-line clear, rotation and soft drop are ROM behavior",()->{
         Rig r=new Rig();r.tick(0);r.tick(64);r.piece(1,0,3);
         for(int x=0;x<10;x++)if(x!=4&&x!=5)r.mem[190+x]=3;
         r.tick(16);eq(1,r.c.out[1],"one completed line");eq(2,r.occupied(),"upper half of square drops into cleared row");check(r.mem[194]!=0&&r.mem[195]!=0,"row shift preserves coordinates");check(r.c.out[0]>=100,"line score");
         r.tick(64);r.piece(0,1,4);for(int y=16;y<20;y++)for(int x=0;x<10;x++)if(x!=5)r.mem[y*10+x]=4;
         r.tick(16);eq(4,r.c.out[1],"four rows cleared in one lock");eq(0,r.occupied(),"all four rows compacted");
         r.tick(64);r.piece(2,0,3);int mask=r.c.r[26];for(int i=0;i<4;i++)r.tick(4);eq(mask,r.c.r[26],"four rotations return to original");
         int y=r.c.r[11];r.held=8;r.tick(0);eq(y+1,r.c.r[11],"held down soft-drops every tick");
      });
      test("raster: bounded general-purpose VM, arithmetic, call stack, invalid addresses and programs",()->{
         Rig io=new Rig();RasterVM.Cpu c=new RasterVM.Cpu();int[] mem=new int[4096];
         int[] code=RasterVM.assemble("LI 0 7\nLI 1 5\nADD 0 1\nCALL store\nHALT\nstore:\nLI 2 123\nST 2 0\nLD 3 2\nRET\n");RasterVM.step(c,code,mem,io);eq(12,mem[123],"general integer arithmetic");eq(12,c.r[3],"load follows store");eq("HALT",c.fault,"halt is visible");
         for(String source:new String[]{"LI 0 -1\nLD 1 0","LI 32 0","RET","CALL 0","JMP -1","LI 0 1\nLI 1 0\nDIV 0 1","LI 4 4096\nLI 5 10\nLI 6 20\nHIT"}){c.reset();RasterVM.step(c,RasterVM.assemble(source),mem,io);check(!c.fault.isEmpty(),"unsafe program traps: "+source);}
         c.reset();RasterVM.step(c,RasterVM.assemble("JMP 0"),mem,io);eq(0,c.pc,"infinite loop yields at bounded instruction budget");
         boolean rejected=false;try{RasterVM.assemble("a:\na:\nHALT");}catch(IllegalArgumentException e){rejected=true;}check(rejected,"duplicate labels rejected");
      });
      test("raster: registered, craftable, canonical blocks with no ID overlap",()->{
         ArrayList<Integer> ids=new ArrayList<>();RasterComputer.creative(ids);eq(11,ids.size(),"eleven hardware items");
         for(int id:ids){check(Blocks.name[id]!=null,"registered name");check(Shapes.modelBoxes(id)!=null,"world and inventory model");eq(Creative.UTILITY,Creative.tabOf(id),"utility tab");eq(id,Blocks.pickItem(id),"canonical pick");check(!Circuit.handles(id),"old computer namespace untouched");}
         for(int f=0;f<4;f++){eq(RasterComputer.PIXEL,Blocks.pickItem(RasterComputer.PIXEL+4+f),"powered driver pick");eq(15,RasterComputer.lampPower(RasterComputer.PIXEL+4+f,Circuit.DX[f],0,Circuit.DZ[f]),"driver only powers forward");eq(0,RasterComputer.lampPower(RasterComputer.PIXEL+4+f,-Circuit.DX[f],0,-Circuit.DZ[f]),"no backwards power");}
      });
      test("raster: physical network, panel expansion, real lamp power, cable break and save/resume",()->{
         Game g=newGame(1);World w=g.world;RasterArcade.install(w,-8,72,0);ticks(w,8);
         long cpu=World.pack(-8,72,6),key=World.pack(1,82,30);RasterComputer.Network net=RasterComputer.network(w,cpu);
         eq("",net.error,"complete physical network");eq(28,net.screens.size(),"4x7 connected panels");eq(448,net.lamps.size(),"16x28 pixel drivers");eq(16,net.width,"native pixel width");eq(28,net.height,"native pixel height");
         RasterVM.Cpu c=w.raster.cpus.get(cpu);eq("",c.fault,"running ROM");eq(1,c.out[3],"world starts paused");check(!w.raster.pixels.isEmpty(),"screen output stored");
         for(int y=0;y<28;y++)for(int x=0;x<16;x++){int v=c.frame[y*64+x]&15;eq(v==0?0:15,RedstoneLogic.input(w,x,99-y,0,true),"real forward redstone pixel");eq(v==0?Redstone319.REDSTONE_LAMP:Redstone319.REDSTONE_LAMP_LIT,w.getBlock(x,99-y,0),"ordinary lamp matches framebuffer");}
         for(int h=0;h<7;h++)w.setState(-4,82+h,0,RasterComputer.PANEL+1);ticks(w,1);net=RasterComputer.network(w,cpu);eq(20,net.width,"adding a column grows canvas by four pixels");
         RasterComputer.input(w,key,0,64);ticks(w,1);int pc=c.pc,score=c.out[0];w.setBlock(-7,72,5,0);ticks(w,30);eq(pc,c.pc,"cutting bus stops processor");eq(score,c.out[0],"no hidden game keeps executing");w.setBlock(-7,72,5,RasterComputer.CABLE);ticks(w,1);check(RasterComputer.network(w,cpu).error.isEmpty(),"repair reconnects hardware");
         RasterComputer.input(w,key,0,16);ticks(w,2);
         Path dir=Files.createTempDirectory("raster-save-");setField(w,"dir",dir.toFile());w.saveEnabled=true;RasterComputer.save(w);World restored=new World(1234,dir.toFile());restored.saveEnabled=false;
         for(Chunk old:w.chunks.values()){Chunk copy=new Chunk(old.cx,old.cz);System.arraycopy(old.blocks,0,copy.blocks,0,old.blocks.length);copy.computeHeightmap();restored.chunks.put(World.key(copy.cx,copy.cz),copy);RasterComputer.loaded(restored,copy);}
         RasterVM.Cpu cc=restored.raster.cpus.get(cpu);eq(c.pc,cc.pc,"saved PC");check(Arrays.equals(c.r,cc.r),"saved registers");check(Arrays.equals(c.frame,cc.frame),"saved framebuffer");check(w.raster.pixels.equals(restored.raster.pixels),"saved panel pixels");
         for(int i=0;i<30;i++){RasterComputer.tick(w);RasterComputer.tick(restored);eq(c.pc,cc.pc,"resumed instruction stream");check(Arrays.equals(c.r,cc.r),"resumed registers");check(Arrays.equals(c.frame,cc.frame),"resumed graphics");}
         g.target=hit(1,82,30,0,1,0);RasterComputer.use(g,1,82,30,RasterComputer.KEYS+1);check(RasterComputer.captured(g),"keypad captures controls");g.onKey(264,1,0);eq(8,w.raster.keys.get(key)[0],"keydown held input");g.onKey(264,0,0);eq(0,w.raster.keys.get(key)[0],"keyup releases input");g.onKey(256,1,0);check(!RasterComputer.captured(g),"Escape restores normal game controls");
         w.shutdown();restored.shutdown();
      });
      test("raster: general redstone IO, alternate ROM, duplicate hardware and real network packets",()->{
         Game g=newGame(1);World w=g.world;int y=90;
         int[] parts={RasterComputer.CPU+1|4096,RasterComputer.CABLE,RasterComputer.ROM,RasterComputer.RAM,RasterComputer.GPU,RasterComputer.KEYS,RasterComputer.KEYS};
         for(int i=0;i<parts.length;i++)w.setState(-i,y,0,parts[i]);long cp=World.pack(0,y,0),rom=World.pack(-2,y,0);
         w.raster.roms.put(rom,RasterVM.assemble("IN 0 16\nOUT 6 0\nIN 1 0\nOUT 0 1\nPRESENT\nWAIT 1\nJMP 0\n"));
         w.setState(0,y,-1,Redstone319.REDSTONE_BLOCK);w.setState(0,y,1,Redstone319.REDSTONE_LAMP);ticks(w,6);
         eq(15,w.raster.cpus.get(cp).out[6],"ROM reads north redstone input");eq(Redstone319.REDSTONE_LAMP_LIT,w.getBlock(0,y,1),"OUT6 powers ordinary lamp");
         RasterComputer.input(w,World.pack(-5,y,0),0,1);RasterComputer.input(w,World.pack(-6,y,0),0,2);ticks(w,1);eq(3,w.raster.cpus.get(cp).out[0],"all connected keypads are read, not arbitrary one");
         w.setState(0,y,-1,0);ticks(w,6);eq(Redstone319.REDSTONE_LAMP,w.getBlock(0,y,1),"output falls and lamp obeys delay");
         w.setState(-7,y,0,RasterComputer.ROM);ticks(w,1);check(!RasterComputer.network(w,cp).error.isEmpty(),"duplicate ROM faults bus");w.setState(-7,y,0,0);
         w.setState(0,y,-1,Redstone319.REDSTONE_BLOCK);ticks(w,6);w.setState(0,y,0,RasterComputer.CPU+1);
         setField(g,"root",Files.createTempDirectory("raster-import-").toFile());g.target=hit(-2,y,0,0,1,0);g.runCommand("/rasterload Pattern.asm");ticks(w,6);
         eq(0,w.raster.cpus.get(cp).out[6],"import resets stopped CPU output");eq(Redstone319.REDSTONE_LAMP,w.getBlock(0,y,1),"import notifies real redstone when output falls");check(Arrays.equals(RasterPrograms.testPattern(),w.raster.roms.get(rom)),"actual import installs alternate program");
         int[] imported=w.raster.roms.get(rom);g.runCommand("/rasterload ../bad.asm");check(imported==w.raster.roms.get(rom),"path traversal refused without modifying ROM");
         java.lang.reflect.Field inboxField=Net319.class.getDeclaredField("inbox");inboxField.setAccessible(true);
         @SuppressWarnings("unchecked") Queue<String> inbox=(Queue<String>)inboxField.get(null);
         java.lang.reflect.Field client=Net319.class.getDeclaredField("clientConnected"),host=Net319.class.getDeclaredField("hosting");client.setAccessible(true);host.setAccessible(true);boolean oc=client.getBoolean(null),oh=host.getBoolean(null);
         try {
            client.setBoolean(null,true);host.setBoolean(null,false);int pc=w.raster.cpus.get(cp).pc;RasterComputer.tick(w);eq(pc,w.raster.cpus.get(cp).pc,"client never executes ROM");
            long panel=World.pack(8,y,8);w.setState(8,y,8,RasterComputer.PANEL);inbox.add(Net319.rasterPacket(w,0,panel,"fedcba9876543210"));Net319.processInbox(w,g);eq(0xfedcba9876543210L,w.raster.pixels.get(panel).longValue(),"all 16 pixel nibbles survive real RASTER parser");
            RasterVM.Cpu status=new RasterVM.Cpu();status.out[0]=12345;status.fault="test fault";inbox.add(Net319.rasterPacket(w,1,cp,RasterComputer.status(status)));Net319.processInbox(w,g);eq(12345,w.raster.cpus.get(cp).out[0],"network status");eq("test fault",w.raster.cpus.get(cp).fault,"network fault text");
            host.setBoolean(null,true);client.setBoolean(null,false);long key=World.pack(-5,y,0);inbox.add("RASTERKEY 0 -5 90 0 8 64");Net319.processInbox(w,g);eq(8,w.raster.keys.get(key)[0],"host receives held key");eq(64,w.raster.keys.get(key)[1],"host receives event");
            inbox.add(Net319.rasterPacket(w,0,panel,"1111111111111111"));Net319.processInbox(w,g);eq(0xfedcba9876543210L,w.raster.pixels.get(panel).longValue(),"host rejects client framebuffer spoof");
         } finally {client.setBoolean(null,oc);host.setBoolean(null,oh);Net319.clearWorldState();inbox.clear();w.shutdown();}
      });
      test("raster: screen grouping and pixel orientation on all four sides",()->{
         Game g=newGame(1);World w=g.world;
         for(int f=0;f<4;f++){
            RasterComputer.Network n=new RasterComputer.Network();RasterVM.Cpu c=new RasterVM.Cpu();c.width=8;c.height=8;
            for(int row=0;row<2;row++)for(int col=0;col<2;col++){int x=f*6+col*RasterComputer.RX[f],z=8+col*RasterComputer.RZ[f],y=90-row;w.setState(x,y,z,RasterComputer.PANEL+f);n.screens.add(World.pack(x,y,z));}
            c.frame[0]=1;c.frame[7]=2;c.frame[7*64]=3;c.frame[7*64+7]=4;RasterComputer.present(w,n,c);eq(1,RasterComputer.groups(w,n.screens).size(),"one coplanar wall");
            eq(1,(int)(w.raster.pixels.get(n.screens.get(0))&15),"top-left pixel facing "+f);eq(2,(int)(w.raster.pixels.get(n.screens.get(1))>>>12&15),"top-right facing "+f);eq(3,(int)(w.raster.pixels.get(n.screens.get(2))>>>48&15),"bottom-left facing "+f);eq(4,(int)(w.raster.pixels.get(n.screens.get(3))>>>60&15),"bottom-right facing "+f);
         }w.shutdown();
      });
   }
}
