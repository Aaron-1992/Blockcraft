package bc;

import java.io.*;
import java.util.*;
import static bc.GameTests.*;

/** Executable contracts for Java-style circuits and aquatic mechanics (20 game ticks / second). */
public final class TestsAquaticRedstone {
   static void advance(World w,int ticks){for(int i=0;i<ticks;i++)w.tick();}
   public static void main(String[] args){run();System.out.println("Aquatic/redstone: "+passed+" passed, "+failed+" failed");System.exit(failed==0?0:1);}
   static void run(){
      test("aquatic: saved properties, old chunks, waterlogged slabs and bucket extraction",()->{
         Game g=newGame(1);World w=g.world;int x=8,y=GROUND+1,z=8;
         w.setBlock(x,y,z,Blocks.WATER);w.setBlock(x,y,z,Blocks.SLAB0);
         eq(Blocks.SLAB0,w.getBlock(x,y,z),"waterlogging does not change block identity");
         check(BlockState.wet(w.getState(x,y,z)),"source retained on underwater slab placement");
         check(WaterPhysics.contains(w,x,y,z),"waterlogged block counts as water");
         int expected=Blocks.SLAB0|BlockState.WATERLOGGED;
         ByteArrayOutputStream bytes=new ByteArrayOutputStream();
         Chunk.writeBlocks(new DataOutputStream(bytes),w.chunkAt(x,z).blocks);
         short[] decoded=new short[65536];Chunk.readBlocks(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())),decoded);
         eq(expected,decoded[Chunk.idx(x,y,z)]&65535,"property nibble survives chunk save/load");
         check(WaterPhysics.drain(w,x,y,z),"bucket drains only source water");
         eq(Blocks.SLAB0,w.getState(x,y,z),"draining leaves dry slab intact");
         check(WaterPhysics.fill(w,x,y,z),"bucket waterlogs existing slab");
         w.setBlock(x,y,z,0);eq(Blocks.WATER,w.getBlock(x,y,z),"breaking waterlogged slab leaves water");
         w.setState(x,y,z,Blocks.WATER+3);check(!WaterPhysics.drain(w,x,y,z),"flowing water cannot be bucketed");
         check(!WaterPhysics.submerged(w,x+.5,y+.99,z+.5),"eyes above a shallow water surface can breathe");
         check(WaterPhysics.submerged(w,x+.5,y+.1,z+.5),"eyes below surface submerged");
         byte[] legacy=new byte[65536];legacy[99]=1;Chunk.readBlocks(new DataInputStream(new ByteArrayInputStream(legacy)),decoded);
         eq(1,decoded[99]&65535,"pre-extension saves still readable");
         w.shutdown();
      });
      test("aquatic: seven-block reach, source renewal, nearest drain and falling reset",()->{
         Game g=newGame(1);World w=g.world;int y=GROUND+1;
         w.setBlock(0,y,0,Blocks.WATER);advance(w,55);
         for(int x=0;x<=7;x++)eq(Blocks.WATER+x,w.getBlock(x,y,0),"water horizontal level "+x);
         eq(0,w.getBlock(8,y,0),"source cannot spread eight blocks horizontally");
         w.setState(0,y,0,0);advance(w,70);
         eq(0,w.getBlock(7,y,0),"flow recedes after source removal");
         for(int x=10;x<14;x++)for(int z=10;z<13;z++)w.setBlock(x,y,z,Blocks.STONE);
         w.setBlock(10,y,11,Blocks.WATER);w.setBlock(12,y,11,Blocks.WATER);w.setBlock(11,y,11,Blocks.WATER+1);
         advance(w,5);eq(Blocks.WATER,w.getBlock(11,y,11),"two adjacent sources renew over solid ground");
         // A separate high platform drains east, so other directions must remain dry.
         int high=y+7;
         for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++)w.setBlock(x,high-1,z,Blocks.STONE);
         w.setBlock(2,high-1,0,0);w.setBlock(0,high,0,Blocks.WATER);advance(w,5);
         check(Blocks.isWater(w.getBlock(1,high,0)),"water chooses route toward nearby drop");
         eq(0,w.getBlock(-1,high,0),"nearest-drop routing avoids opposite side");
         advance(w,45);
         check((w.getState(2,high-2,0)&BlockState.FALLING)!=0,"vertical stream has falling state");
         w.shutdown();
      });
      test("aquatic: bubble columns and sprint swimming collision pose",()->{
         Game g=newGame(0);World w=g.world;int y=GROUND+1;
         for(int x=5;x<=11;x++)for(int z=5;z<=11;z++)for(int yy=y;yy<y+5;yy++)w.setBlock(x,yy,z,Blocks.WATER);
         w.setBlock(8,y-1,8,Blocks.SOUL_SAND);advance(w,25);
         check((w.getState(8,y+3,8)&WaterPhysics.BUBBLE_UP)!=0,"soul sand bubbles propagate through source column");
         Player p=g.player;p.flying=false;p.setPos(8.5,y+1,8.5);p.tick(w,1,0,false,false,true);
         check(p.swimming,"sprint forward underwater starts swimming");check(p.height()==.6,"swimming uses 0.6-block collision height");
         check(p.vy>0,"bubble column lifts swimmer");
         w.setBlock(8,y-1,8,Blocks.MAGMA);advance(w,25);
         check((w.getState(8,y+3,8)&WaterPhysics.BUBBLE_DOWN)!=0,"magma reverses column direction");
         p.setPos(20.5,y,20.5);p.tick(w,0,0,false,false,false);check(!p.swimming&&p.height()==1.8,"exiting water restores standing pose with clearance");
         w.shutdown();
      });
      test("aquatic: biome bands, versioned generation and deterministic vegetation",()->{
         eq(Biomes.WARM_OCEAN,Aquatic.ocean(.8,50),"warm shallow sea");
         eq(Biomes.LUKEWARM_OCEAN,Aquatic.ocean(.65,50),"lukewarm sea");
         eq(Biomes.DEEP_LUKEWARM_OCEAN,Aquatic.ocean(.65,30),"deep lukewarm sea");
         eq(Biomes.COLD_OCEAN,Aquatic.ocean(.3,50),"cold sea");
         eq(Biomes.DEEP_FROZEN_OCEAN,Aquatic.ocean(.1,30),"deep frozen sea");
         Properties old=new Properties();old.setProperty("gentype","1");check(!WorldSettings.load(old).aquatic,"old saves retain old generation even in new chunks");
         WorldSettings st=new WorldSettings();Properties saved=new Properties();st.save(saved);check(WorldSettings.load(saved).aquatic,"new world's aquatic setting is saved");
         TerraGen gen=new TerraGen(72349,st);Chunk c=new Chunk(-1,0),other=new Chunk(-1,0);
         int[] heights=new int[256],biomes=new int[256];Arrays.fill(heights,48);Arrays.fill(biomes,Biomes.WARM_OCEAN);
         for(Chunk ch:new Chunk[]{c,other})for(int z=0;z<16;z++)for(int x=0;x<16;x++){ch.set(x,48,z,Blocks.SAND);for(int y=49;y<63;y++)ch.set(x,y,z,Blocks.WATER);}
         Aquatic.decorate(gen,c,heights,biomes);Aquatic.decorate(gen,other,heights,biomes);
         check(Arrays.equals(c.blocks,other.blocks),"decoration independent of chunk generation order");
         int plants=0;for(short state:c.blocks)if(Aquatic.plant(state&4095)||Aquatic.liveCoral(state&4095))plants++;
         check(plants>0,"underwater features are actually placed");
         for(int t=781;t<=793;t++){int[] art=Aquatic.paint(t);check(art.length==1024&&Arrays.equals(art,Aquatic.paint(t)),"new native 32px canvas "+t);}
      });
      test("redstone: saved repeater settings, delayed rise/fall, locking and rear-only input",()->{
         Game g=newGame(1);World w=g.world;int y=GROUND+1;
         w.setBlock(8,y,8,Redstone319.REPEATER_OFF+2);w.setState(8,y,8,(Redstone319.REPEATER_OFF+2)|(3<<12));
         w.setBlock(7,y,8,Redstone319.LEVER_ON);Redstone319.propagate(w,7,y,8);
         advance(w,7);eq(Redstone319.REPEATER_OFF+2,w.getBlock(8,y,8),"four-tick repeater waits eight game ticks");
         advance(w,1);eq(Redstone319.REPEATER_ON+2,w.getBlock(8,y,8),"repeater rises at scheduled tick");
         eq(8,RedstoneLogic.delay(w,8,y,8),"power state changes retain saved delay");
         w.setBlock(7,y,8,0);Redstone319.propagate(w,7,y,8);advance(w,7);
         eq(Redstone319.REPEATER_ON+2,w.getBlock(8,y,8),"falling edge also delayed");advance(w,1);
         eq(Redstone319.REPEATER_OFF+2,w.getBlock(8,y,8),"falling edge at full delay");
         w.setBlock(8,y,7,Redstone319.REPEATER_ON+1); // side repeater faces +Z into target
         w.setBlock(8,y,6,Redstone319.REDSTONE_BLOCK);
         w.setBlock(7,y,8,Redstone319.LEVER_ON);Redstone319.propagate(w,7,y,8);advance(w,10);
         eq(Redstone319.REPEATER_OFF+2,w.getBlock(8,y,8),"powered side repeater locks an OFF state");
         w.setBlock(8,y,7,0);Redstone319.propagate(w,8,y,7);advance(w,8);
         eq(Redstone319.REPEATER_ON+2,w.getBlock(8,y,8),"unlock allows rear input through");
         w.shutdown();
      });
      test("redstone: observers delay then pulse exactly two game ticks; timers do not leak worlds",()->{
         Game g=newGame(1);World w=g.world;int y=GROUND+3;
         w.setBlock(8,y,8,Redstone319.OBSERVER+2);w.setBlock(9,y,8,Blocks.STONE);
         eq(Redstone319.OBSERVER+2,w.getBlock(8,y,8),"observer not instantaneous");advance(w,1);
         eq(Redstone319.OBSERVER+2,w.getBlock(8,y,8),"one game tick still OFF");advance(w,1);
         eq(Redstone319.OBSERVER_ON+2,w.getBlock(8,y,8),"rising edge after one redstone tick");
         check(Redstone319.isPowered(w,7,y,8)&&!Redstone319.isPowered(w,9,y,8),"rear output only");advance(w,1);
         eq(Redstone319.OBSERVER_ON+2,w.getBlock(8,y,8),"pulse lasts second game tick");advance(w,1);
         eq(Redstone319.OBSERVER+2,w.getBlock(8,y,8),"pulse then ends");
         Game other=newGame(1);check(other.world.redstone.pending.isEmpty(),"fresh world has no previous timers");
         w.shutdown();other.world.shutdown();
      });
      test("redstone: long dust graph clears, source attenuation and no power through glass",()->{
         Game g=newGame(1);World w=g.world;int y=GROUND+1;
         for(int x=-20;x<=20;x++)w.setBlock(x,y,0,Redstone319.REDSTONE_WIRE0+7);
         w.setBlock(-21,y,0,Redstone319.REDSTONE_BLOCK);Redstone319.propagate(w,-21,y,0);
         for(int i=0;i<16;i++)eq(Math.max(0,15-i),Redstone319.powerAt(w,-20+i,y,0),"one power lost per dust edge "+i);
         eq(0,Redstone319.powerAt(w,20,y,0),"entire graph cleared, not just a truncated scan radius");
         w.setBlock(-21,y,0,0);Redstone319.propagate(w,-21,y,0);
         for(int x=-20;x<=20;x++)eq(0,Redstone319.powerAt(w,x,y,0),"no stale charge after source removal "+x);
         w.setBlock(7,y,8,Redstone319.REPEATER_ON+2);w.setBlock(6,y,8,Redstone319.REDSTONE_BLOCK);
         w.setBlock(8,y,8,Blocks.STONE);w.setBlock(9,y,8,Redstone319.REDSTONE_WIRE0);Redstone319.propagate(w,7,y,8);
         eq(15,Redstone319.powerAt(w,9,y,8),"repeater strongly powers dust through one solid block");
         w.setBlock(8,y,8,Blocks.GLASS);Redstone319.propagate(w,8,y,8);
         eq(0,Redstone319.powerAt(w,9,y,8),"glass does not conduct signal");w.shutdown();
      });
      test("redstone: comparator analog inventory output, compare/subtract and two-game-tick delay",()->{
         Game g=newGame(1);World w=g.world;int y=GROUND+1;
         w.setBlock(7,y,8,Blocks.CHEST);TileEntity chest=w.getOrCreateTile(7,y,8);
         chest.slots[0]=new Stack(Blocks.STONE,64);
         w.setBlock(8,y,8,RedstoneLogic.COMPARATOR+2);Redstone319.propagate(w,8,y,8);
         eq(0,Redstone319.powerAt(w,8,y,8),"comparator initially unpowered");advance(w,2);
         eq(1,Redstone319.powerAt(w,8,y,8),"one full slot in a 27-slot chest gives signal 1");
         Arrays.fill(chest.slots,new Stack(Blocks.STONE,64));advance(w,4);
         eq(15,Redstone319.powerAt(w,8,y,8),"full chest gives signal 15");
         w.setBlock(8,y,7,Redstone319.REDSTONE_BLOCK);
         Redstone319.use(g,8,y,8,w.getBlock(8,y,8));advance(w,2);
         eq(0,Redstone319.powerAt(w,8,y,8),"subtract mode: 15 minus 15 is zero");
         check((w.getState(8,y,8)&0x1000)!=0,"subtract mode stored in chunk state");
         Redstone319.use(g,8,y,8,w.getBlock(8,y,8));advance(w,2);
         eq(15,Redstone319.powerAt(w,8,y,8),"compare mode passes equal rear and side inputs");w.shutdown();
      });
      test("redstone: delayed torch inversion and four-game-tick lamp persistence",()->{
         Game g=newGame(1);World w=g.world;int y=GROUND+1;
         w.setBlock(8,y,8,Blocks.STONE);w.setBlock(8,y+1,8,Redstone319.REDSTONE_TORCH);
         w.setBlock(7,y,8,Redstone319.REDSTONE_BLOCK);Redstone319.propagate(w,7,y,8);
         advance(w,1);eq(Redstone319.REDSTONE_TORCH,w.getBlock(8,y+1,8),"torch waits one redstone tick");advance(w,1);
         eq(Redstone319.REDSTONE_TORCH_OFF,w.getBlock(8,y+1,8),"torch inverts after two game ticks");
         w.setBlock(12,y,8,Redstone319.REDSTONE_LAMP);w.setBlock(11,y,8,Redstone319.REDSTONE_BLOCK);Redstone319.propagate(w,11,y,8);
         eq(Redstone319.REDSTONE_LAMP_LIT,w.getBlock(12,y,8),"lamp lights immediately");
         w.setBlock(11,y,8,0);Redstone319.propagate(w,11,y,8);advance(w,3);
         eq(Redstone319.REDSTONE_LAMP_LIT,w.getBlock(12,y,8),"lamp persists for four game ticks");advance(w,1);
         eq(Redstone319.REDSTONE_LAMP,w.getBlock(12,y,8),"lamp turns off on fourth tick");w.shutdown();
      });
      test("aquatic: only valid shapes waterlog, fire protection and water washes components away",()->{
         Game g=newGame(1);World w=g.world;int y=GROUND+1;
         for(int id:new int[]{Blocks.FENCE,Shapes.FENCE0,Shapes.BARS,Shapes.PANE0,Shapes.WALL0,Shapes.TRAP0,Blocks.CHEST,Blocks.LADDER}) {
            w.setState(8,y,8,id);check(WaterPhysics.fill(w,8,y,8),"bucket waterlogs shape "+id);
            check(BlockState.wet(w.getState(8,y,8)),"wet property "+id);
         }
         check(!BlockState.waterloggable(Blocks.TORCH),"torches wash away, never waterlog");
         w.setState(8,y,8,Blocks.SLAB0|BlockState.WATERLOGGED);
         for(int i=0;i<40;i++)w.fire.tryCatch(8,y,8,1,0);
         eq(Blocks.SLAB0,w.getBlock(8,y,8),"wet timber does not burn");
         w.setBlock(2,y,2,Redstone319.REDSTONE_WIRE0);w.setBlock(1,y,2,Blocks.WATER);advance(w,5);
         check(Blocks.isWater(w.getBlock(2,y,2)),"water removes dust rather than flowing around it");
         check(w.items.stream().anyMatch(item->item.id==Redstone319.ITEM_REDSTONE),"washed dust drops its item");
         w.shutdown();
      });
      test("state: all sixteen property nibbles round-trip in the backwards-readable chunk extension",()->{
         short[] states=new short[65536],decoded=new short[65536];
         for(int i=0;i<states.length;i++)states[i]=(short)i;
         ByteArrayOutputStream b=new ByteArrayOutputStream();Chunk.writeBlocks(new DataOutputStream(b),states);
         Chunk.readBlocks(new DataInputStream(new ByteArrayInputStream(b.toByteArray())),decoded);
         check(Arrays.equals(states,decoded),"entire unsigned 16-bit range, including sign bit");
      });
      test("redstone: connected consumers beyond a local radius update on both edges",()->{
         Game g=newGame(1);World w=g.world;int y=GROUND+1;
         for(int x=0;x<29;x++)w.setBlock(x,y,0,x==14?Redstone319.REPEATER_OFF+2:Redstone319.REDSTONE_WIRE0);
         w.setBlock(29,y,0,Redstone319.REDSTONE_LAMP);w.setBlock(-1,y,0,Redstone319.LEVER_ON);Redstone319.propagate(w,-1,y,0);advance(w,2);
         eq(Redstone319.REDSTONE_LAMP_LIT,w.getBlock(29,y,0),"distant consumer after repeater delay");
         w.setBlock(-1,y,0,Redstone319.LEVER_OFF);Redstone319.propagate(w,-1,y,0);advance(w,6);
         eq(Redstone319.REDSTONE_LAMP,w.getBlock(29,y,0),"repeater falling delay plus lamp persistence");w.shutdown();
      });
      test("redstone: pistons validate the complete 12-block assembly and retain waterlogged states",()->{
         for(int count:new int[]{12,13}) {
            Game g=newGame(1);World w=g.world;int y=GROUND+3;
            w.setBlock(4,y,8,Redstone319.PISTON+4);for(int i=0;i<count;i++)w.setBlock(5+i,y,8,Blocks.STONE);
            w.setBlock(3,y,8,Redstone319.REDSTONE_BLOCK);Redstone319.propagate(w,3,y,8);
            eq(count==12?Redstone319.PISTON_EXT+4:Redstone319.PISTON+4,w.getBlock(4,y,8),"push limit "+count);
            eq(count==12?Blocks.STONE:0,w.getBlock(5+count,y,8),"all-or-nothing movement "+count);w.shutdown();
         }
         Game g=newGame(1);World w=g.world;int y=GROUND+3;
         w.setBlock(8,y,8,Redstone319.STICKY_PISTON+4);w.setBlock(9,y,8,Redstone319.SLIME_BLOCK);
         w.setState(9,y,9,Blocks.SLAB0|BlockState.WATERLOGGED);w.setBlock(9,y,7,Blocks.GLAZED0);
         w.setBlock(7,y,8,Redstone319.REDSTONE_BLOCK);Redstone319.propagate(w,7,y,8);
         eq(Blocks.SLAB0|BlockState.WATERLOGGED,w.getState(10,y,9),"slime moves side payload with saved property intact");
         eq(Blocks.GLAZED0,w.getBlock(9,y,7),"glazed terracotta does not stick to slime");
         advance(w,3);w.setBlock(7,y,8,0);Redstone319.propagate(w,7,y,8);
         eq(Redstone319.SLIME_BLOCK,w.getBlock(9,y,8),"normal sticky retraction pulls assembly");
         eq(Blocks.SLAB0|BlockState.WATERLOGGED,w.getState(9,y,9),"attached state pulled back");w.shutdown();
      });
      test("redstone: sticky short-pulse block dropping, quasi-connectivity and blocked movement",()->{
         Game g=newGame(1);World w=g.world;int y=GROUND+3;
         w.setBlock(8,y,8,Redstone319.STICKY_PISTON+4);w.setBlock(9,y,8,Blocks.STONE);
         w.setBlock(7,y,8,Redstone319.REDSTONE_BLOCK);Redstone319.propagate(w,7,y,8);
         advance(w,1);w.setBlock(7,y,8,0);Redstone319.propagate(w,7,y,8);
         eq(0,w.getBlock(9,y,8),"one-game-tick sticky pulse does not pull");eq(Blocks.STONE,w.getBlock(10,y,8),"short pulse leaves block out");
         w.setBlock(9,y,8,Blocks.OBSIDIAN);w.setBlock(7,y+1,8,Redstone319.REDSTONE_BLOCK);Redstone319.propagate(w,8,y,8);
         eq(Redstone319.STICKY_PISTON+4,w.getBlock(8,y,8),"obsidian prevents extension atomically");
         w.setBlock(9,y,8,0);Redstone319.propagate(w,8,y,8);
         eq(Redstone319.STICKY_PISTON_EXT+4,w.getBlock(8,y,8),"power adjacent to space above piston activates on update");w.shutdown();
      });
      test("redstone: torch burnout/recovery, daylight inversion and button durations",()->{
         Game g=newGame(1);World w=g.world;int y=GROUND+1;
         w.setBlock(8,y,8,Blocks.STONE);w.setBlock(8,y+1,8,Redstone319.REDSTONE_TORCH);
         for(int n=0;n<8;n++) {
            w.setBlock(7,y,8,Redstone319.REDSTONE_BLOCK);Redstone319.propagate(w,7,y,8);advance(w,2);
            w.setBlock(7,y,8,0);Redstone319.propagate(w,7,y,8);advance(w,2);
         }
         eq(Redstone319.REDSTONE_TORCH_OFF,w.getBlock(8,y+1,8),"eight quick inversions burn torch out");advance(w,160);
         eq(Redstone319.REDSTONE_TORCH,w.getBlock(8,y+1,8),"burned-out torch recovers");
         w.setBlock(14,y,8,Redstone319.DAYLIGHT_SENSOR);w.dayTime=6000;
         eq(15,RedstoneLogic.daylight(w,14,y,8),"noon skylight analog strength");
         Redstone319.use(g,14,y,8,w.getBlock(14,y,8));eq(0,RedstoneLogic.daylight(w,14,y,8),"inverted at noon");
         w.dayTime=18000;eq(15,RedstoneLogic.daylight(w,14,y,8),"inverted at midnight");advance(w,20);
         check((w.getState(14,y,8)&0x1000)!=0,"sensor polling does not discard inversion");
         for(int id:new int[]{Redstone319.STONE_BUTTON,Redstone319.WOOD_BUTTON}) {
            int duration=id==Redstone319.STONE_BUTTON?20:30;w.setBlock(2,y,2,id);Redstone319.use(g,2,y,2,id);
            for(int i=0;i<duration-1;i++)Redstone319.tick(g);
            eq(id+1,w.getBlock(2,y,2),"button still active before deadline "+duration);Redstone319.tick(g);
            eq(id,w.getBlock(2,y,2),"button releases at deadline "+duration);
         }w.shutdown();
      });
      test("network: unsigned state packets, exact dry replacement and generator gate on world entry",()->{
         Game g=newGame(1);World w=g.world;
         try {
            Net319.host(g,25589);
            try(java.net.Socket socket=new java.net.Socket("127.0.0.1",25589)) {
               socket.setSoTimeout(2000);
               BufferedReader in=new BufferedReader(new InputStreamReader(socket.getInputStream()));
               PrintWriter out=new PrintWriter(socket.getOutputStream(),true);
               String[] assign=in.readLine().split(" ");eq(17,assign.length,"ASSIGN includes generator version flag");eq("0",assign[16],"old host sends legacy terrain gate");
               int raw=Blocks.WATER|WaterPhysics.BUBBLE_DOWN;
               out.println("BLOCK 8 65 8 "+raw+" 0");
               long end=System.currentTimeMillis()+2000;
               while(w.getState(8,65,8)!=raw&&System.currentTimeMillis()<end){Thread.sleep(10);Net319.tick(g);}
               eq(raw,w.getState(8,65,8),"network carries unsigned bit 15");
               w.setState(9,65,8,Blocks.SLAB0|BlockState.WATERLOGGED);out.println("BLOCK 9 65 8 "+Blocks.SLAB0+" 0");end=System.currentTimeMillis()+2000;
               while(w.getState(9,65,8)!=Blocks.SLAB0&&System.currentTimeMillis()<end){Thread.sleep(10);Net319.tick(g);}
               eq(Blocks.SLAB0,w.getState(9,65,8),"authoritative dry state is not re-waterlogged locally");
            }
         }finally{Net319.disconnect(g);w.shutdown();}
         g.joinMultiplayerWorld(6739,0,6000,8,70,8,WorldSettings.DEFAULT,1,0,false);
         check(!g.world.settings.aquatic&&g.world.gen instanceof TerraGen&&!((TerraGen)g.world.gen).st.aquatic,"gate selected before generator construction");g.world.shutdown();
      });
      test("redstone: load recovery does not fake observer changes; settings alter model geometry",()->{
         Game g=newGame(1);World w=g.world;int y=GROUND+1;Chunk c=w.chunkAt(8,8);
         c.set(8,y,8,Redstone319.STONE_BUTTON_ON);c.set(9,y,8,Redstone319.OBSERVER+3);
         c.set(12,y,8,Redstone319.REDSTONE_LAMP_LIT);RedstoneLogic.loaded(w,c);
         check(!w.redstone.pending.containsKey(World.pack(9,y,8)),"loading neighbour is not a block-state transition");
         for(int i=0;i<20;i++)Redstone319.tick(g);
         eq(Redstone319.STONE_BUTTON,w.getBlock(8,y,8),"pressed button does not stick forever after load");
         advance(w,4);eq(Redstone319.REDSTONE_LAMP,w.getBlock(12,y,8),"unpowered saved lamp resumes off timer");
         double[][] shortDelay=RedstoneLogic.model(Redstone319.REPEATER_OFF,Redstone319.REPEATER_OFF);
         double[][] longDelay=RedstoneLogic.model(Redstone319.REPEATER_OFF,Redstone319.REPEATER_OFF|0x3000);
         check(shortDelay[3][2]!=longDelay[3][2],"movable torch shows selected repeater delay");
         double[][] compare=RedstoneLogic.model(RedstoneLogic.COMPARATOR,RedstoneLogic.COMPARATOR);
         double[][] subtract=RedstoneLogic.model(RedstoneLogic.COMPARATOR,RedstoneLogic.COMPARATOR|0x1000);
         check(compare[3][9]!=subtract[3][9],"front comparator indicator shows subtract mode");w.shutdown();
      });
   }
}
