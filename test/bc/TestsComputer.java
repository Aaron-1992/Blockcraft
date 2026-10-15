package bc;

import java.io.*;
import java.util.*;
import static bc.GameTests.*;

/** Tests the actual assembled block machine as well as the reusable components. */
public final class TestsComputer {
   static final int OX=-15,OZ=-22,Y=100;
   static void advance(World w,int n){for(int i=0;i<n;i++)Circuit.tick(w);}
   static void put(World w,int x,int y,int z,int type,int val,int face){w.setState(x,y,z,Circuit.id(type,val,face));}
   static int val(World w,int x,int y,int z){return Circuit.value(w.getBlock(x,y,z));}
   static int val(World w,CircuitComputer.Chip c){return val(w,OX+c.x(),Y,OZ+c.z());}
   static void use(Game g,CircuitComputer.Chip c){Circuit.use(g,OX+c.x(),Y,OZ+c.z(),g.world.getBlock(OX+c.x(),Y,OZ+c.z()));}
   static void step(Game g){g.player.sneaking=true;use(g,CircuitComputer.CLOCK);g.player.sneaking=false;advance(g.world,52);}
   static void program(World w,int... words){for(int a=0;a<16;a++){int word=a<words.length?words[a]:0xF0;put(w,OX+25,Y,OZ+10+a,Circuit.CONSTANT,word>>4,0);put(w,OX+27,Y,OZ+10+a,Circuit.CONSTANT,word&15,0);}}
   static Game machine() throws Exception {
      Game g=newGame(1);CircuitComputer.Plan p=CircuitComputer.plan(OX,Y,OZ);
      for(var e:p.blocks.entrySet()){long k=e.getKey();g.world.setState(World.ux(k),World.uy(k),World.uz(k),e.getValue());}
      advance(g.world,12);return g;
   }
   public static void main(String[] args){run();System.out.println("Computer/facing: "+passed+" passed, "+failed+" failed");System.exit(failed==0?0:1);}
   static void run(){
      test("facing: real placement, furnace transitions, waterlogged chest buckets and saved properties",()->{
         Game g=newGame(1);World w=g.world;
         int[] yaw={0,180,90,270};
         for(int id:new int[]{Blocks.CHEST,Blocks.CHEST_LOOT,Blocks.FURNACE,Blocks.CRAFTING_TABLE})for(int angle:yaw){
            w.setState(2,GROUND+1,2,0);g.player.yaw=angle;g.target=top(2,2);hold(g,id,1);g.placeBlock();
            int state=w.getState(2,GROUND+1,2),f=Shapes.playerFacing(g.player)^1;
            eq(id,w.getBlock(2,GROUND+1,2),"placement keeps inventory ID");eq(f,Facing.direction(state),"front faces player "+id+" yaw "+angle);
            eq(Blocks.tiles[id][2],Facing.tile(state,Facing.worldFace(2,Shapes.turns(0,f))),"front texture rotates");
            if(id==Blocks.FURNACE){w.setBlock(2,GROUND+1,2,Blocks.FURNACE_LIT);eq(f,Facing.direction(w.getState(2,GROUND+1,2)),"lighting keeps facing");w.setBlock(2,GROUND+1,2,Blocks.FURNACE);eq(state,w.getState(2,GROUND+1,2),"cooldown keeps facing");}
            if(id==Blocks.CHEST||id==Blocks.CHEST_LOOT){check(WaterPhysics.fill(w,2,GROUND+1,2),"fill oriented chest");check(BlockState.wet(w.getState(2,GROUND+1,2)),"waterlogged");eq(f,Facing.direction(w.getState(2,GROUND+1,2)),"fill preserves facing");check(WaterPhysics.drain(w,2,GROUND+1,2),"drain chest");eq(state,w.getState(2,GROUND+1,2),"drain preserves facing");}
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();Chunk.writeBlocks(new DataOutputStream(bytes),w.chunkAt(2,2).blocks);
            short[] saved=new short[65536];Chunk.readBlocks(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())),saved);
            eq(state,saved[Chunk.idx(2,GROUND+1,2)]&65535,"facing survives chunk codec");
         }
         eq(0,Facing.direction(Blocks.CHEST),"legacy north-facing saves need no migration");w.shutdown();
      });
      test("facing: all animated chest vertices rotate AFTER the hinge, without collapsed UVs",()->{
         for(double angle:new double[]{0,.5,1.45})for(int f=0;f<4;f++){
            double[][] base=UtilityModels.chest(angle),oriented=Facing.chest(Blocks.CHEST|(f<<13),angle);
            for(int i=0;i<base.length;i++)for(int face=0;face<6;face++)for(int corner=0;corner<4;corner++){
               float[] a=new float[3],b=new float[3],ua=new float[2],ub=new float[2];
               Shapes.corner(base[i],face,corner,a,ua);Shapes.corner(oriented[i],face,corner,b,ub);
               for(int j=0;j<Shapes.turns(0,f);j++){float x=a[0];a[0]=1-a[2];a[2]=x;}
               check(Arrays.equals(a,b),"rigid post-hinge rotation");check(Arrays.equals(ua,ub),"rotating lid keeps original UVs");
            }
         }
      });
      test("computer: ID allocation, palette, canonical picks and exhaustive four-bit ALU",()->{
         for(int k=0;k<Circuit.TYPES;k++)for(int f=0;f<4;f++)for(int v=0;v<16;v++){
            int id=Circuit.id(k,v,f);eq(k,Circuit.kind(id),"type");eq(f,Circuit.face(id),"facing");eq(v,Circuit.value(id),"nibble");
            eq(Circuit.id(k,0,0),Blocks.pickItem(id),"canonical item");check(Shapes.modelBoxes(id)!=null,"actual model");check(Blocks.name[id]!=null,"registered");
         }
         for(int a=0;a<16;a++)for(int b=0;b<16;b++)for(int op=0;op<16;op++){
            int expected=switch(op){case 0->b;case 1->(a+b)%16;case 2->(a-b+16)%16;case 3->a^b;case 4->a&b;case 5->a|b;default->a;};
            eq(expected,Circuit.alu(a,b,op),"ALU truth table");
            for(int acc:new int[]{0,1,15})eq(op==15?a:op==7||op==8&&acc==0?b:(a+1)%16,Circuit.nextAddress(a,b,op,acc),"branch truth table");
         }
      });
      test("computer: directional ports, three-dimensional unattenuated nets, conflicts and source removal",()->{
         Game g=newGame(1);World w=g.world;
         for(int f=0;f<4;f++){
            put(w,0,80,0,Circuit.CONSTANT,13,f);
            for(int d=0;d<4;d++)eq(d==f?13:-1,Circuit.output(w,0,80,0,Circuit.DX[d],80,Circuit.DZ[d]),"only front drives");
         }
         put(w,0,80,0,Circuit.CONSTANT,13,2);
         for(int x=1;x<=20;x++)w.setState(x,80,0,Circuit.CABLE);
         for(int y=81;y<=85;y++)w.setState(20,y,0,Circuit.CABLE);
         advance(w,2);eq(13,val(w,20,85,0),"no attenuation over 25 cells/vertical bend");
         put(w,21,85,0,Circuit.CONSTANT,4,3);advance(w,1);check((w.getState(20,85,0)&0x8000)!=0,"conflicting source flags entire net");eq(0,val(w,1,80,0),"fault drives safe zero");
         w.setState(21,85,0,0);w.setState(0,80,0,0);advance(w,1);eq(0,val(w,20,85,0),"removed driver cannot latch bus charge");
         put(w,0,80,0,Circuit.CONSTANT,7,2);w.setState(10,80,0,0);advance(w,1);eq(7,val(w,9,80,0),"source side after wire cut");eq(0,val(w,11,80,0),"disconnected side after wire cut");w.shutdown();
      });
      test("computer: synchronous registers, reset priority and rotated ROM access",()->{
         Game g=newGame(1);World w=g.world;
         for(int f=0;f<4;f++){
            w.circuit.clear();for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++)w.setState(x,80,z,0);
            put(w,0,80,0,Circuit.REGISTER,0,f);int back=f^1,left=Circuit.left(f),right=Circuit.right(f);
            put(w,Circuit.DX[back],80,Circuit.DZ[back],Circuit.CONSTANT,9,f);
            put(w,Circuit.DX[left],80,Circuit.DZ[left],Circuit.CONSTANT,0,left^1);
            advance(w,1);eq(0,val(w,0,80,0),"low clock holds");
            put(w,Circuit.DX[left],80,Circuit.DZ[left],Circuit.CONSTANT,15,left^1);advance(w,1);eq(9,val(w,0,80,0),"rising edge samples");
            put(w,Circuit.DX[back],80,Circuit.DZ[back],Circuit.CONSTANT,3,f);advance(w,3);eq(9,val(w,0,80,0),"high clock does not continuously sample");
            put(w,Circuit.DX[right],80,Circuit.DZ[right],Circuit.CONSTANT,15,right^1);advance(w,1);eq(0,val(w,0,80,0),"reset wins over data/clock");
            put(w,10,100,10,Circuit.ROM,0,f);int r=Circuit.right(f),a=7;
            int x=10+Circuit.DX[r]*3-Circuit.DX[f]*(2+a),z=10+Circuit.DZ[r]*3-Circuit.DZ[f]*(2+a);
            put(w,x,100,z,Circuit.CONSTANT,6,0);put(w,x+Circuit.DX[r]*2,100,z+Circuit.DZ[r]*2,Circuit.CONSTANT,11,0);
            eq(0x6B,Circuit.romWord(w,10,100,10,f,a),"rotated physical ROM bank");
         }w.shutdown();
      });
      test("computer: physical demo has 12 isolated buses and executes LOAD/OUT/ADD/JMP with real lamps",()->{
         Game g=machine();World w=g.world;eq(12,w.circuit.nets.size(),"all intended nets remain isolated");
         for(long k:w.circuit.cables)check((w.getState(World.ux(k),World.uy(k),World.uz(k))&0x8000)==0,"no cable contention");
         int[][] expected={{1,1,0},{2,1,1},{3,2,1},{1,2,1},{2,2,2}};
         for(int[] e:expected){step(g);eq(e[0],val(w,CircuitComputer.PC),"instruction PC");eq(e[1],val(w,CircuitComputer.ACC),"instruction accumulator");eq(e[2],val(w,CircuitComputer.OUT),"OUT register");eq(e[2],val(w,CircuitComputer.DISPLAY),"real output display");}
         for(int i=0;i<4;i++){var c=CircuitComputer.BITS[i];tick(w,6);eq((2&(8>>i))!=0?Redstone319.REDSTONE_LAMP_LIT:Redstone319.REDSTONE_LAMP,w.getBlock(OX+c.x(),Y,OZ+c.z()-1),"binary redstone lamp "+i);}
         use(g,CircuitComputer.RESET);advance(w,18);eq(0,val(w,CircuitComputer.PC),"reset PC");eq(0,val(w,CircuitComputer.ACC),"reset accumulator");eq(0,val(w,CircuitComputer.OUT),"reset output");
         // Sever the ALU's actual output wire. LOAD 1 must now load zero, not a scripted expected answer.
         w.setState(OX+38,Y,OZ+7,0);advance(w,8);step(g);eq(0,val(w,CircuitComputer.ACC),"broken physical data wire changes computation");w.shutdown();
      });
      test("computer: edited physical ROM executes conditional loop, logic operations and HALT",()->{
         Game g=machine();World w=g.world;
         program(w,0x03,0x21,0x84,0x71,0x0A,0x3F,0x43,0x58,0x60,0xF0);advance(w,10);
         int[] pcs={1,2,3,1,2,3,1,2,4,5,6,7,8,9};int[] acc={3,2,2,2,1,1,1,0,0,10,5,1,9,9};
         for(int i=0;i<pcs.length;i++){step(g);eq(pcs[i],val(w,CircuitComputer.PC),"edited program PC "+i);eq(acc[i],val(w,CircuitComputer.ACC),"edited program accumulator "+i);}
         eq(9,val(w,CircuitComputer.OUT),"program outputs nine");step(g);step(g);eq(9,val(w,CircuitComputer.PC),"HALT blocks clock/step");
         // Chunk round trip recovers nodes/cables and register values, not a hidden VM program/state.
         Game loaded=newGame(1);for(Chunk c:w.chunks.values()){
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();Chunk.writeBlocks(new DataOutputStream(bytes),c.blocks);
            Chunk restored=loaded.world.chunk(c.cx,c.cz);Chunk.readBlocks(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())),restored.blocks);
            Circuit.loaded(loaded.world,restored);
         }
         advance(loaded.world,60);eq(9,val(loaded.world,CircuitComputer.ACC),"saved accumulator");eq(9,val(loaded.world,CircuitComputer.PC),"saved halted PC");eq(9,val(loaded.world,CircuitComputer.OUT),"saved output");
         eq(12,loaded.world.circuit.nets.size(),"saved physical wiring recovered");loaded.world.shutdown();w.shutdown();
      });
      test("computer: Run/Stop is world-local; automatic counter wraps at 4 bits",()->{
         Game g=machine();World w=g.world;g.player.sneaking=false;use(g,CircuitComputer.CLOCK);
         Set<Integer> seen=new HashSet<>();for(int i=0;i<2300;i++){advance(w,1);seen.add(val(w,CircuitComputer.OUT));}
         eq(16,seen.size(),"automatic machine emits all 16 nibbles");
         g.player.sneaking=false;use(g,CircuitComputer.CLOCK);advance(w,8);int pc=val(w,CircuitComputer.PC),acc=val(w,CircuitComputer.ACC);advance(w,100);eq(pc,val(w,CircuitComputer.PC),"Stop holds PC");eq(acc,val(w,CircuitComputer.ACC),"Stop holds accumulator");
         Game other=newGame(1);eq(0,other.world.circuit.nodes.size(),"no circuit state leaks into another world");other.world.shutdown();w.shutdown();
      });
      test("computer: partial unload pauses registers; reload settles before resuming",()->{
         Game g=machine();World w=g.world;for(int i=0;i<5;i++)step(g);eq(2,val(w,CircuitComputer.ACC),"before unload");use(g,CircuitComputer.CLOCK);
         Chunk missing=w.chunks.remove(World.key(-1,-1));missing.unloaded=true;FastChunkCache.clear();
         advance(w,160);eq(2,val(w,CircuitComputer.ACC),"incomplete circuit cannot corrupt accumulator");eq(0,val(w,CircuitComputer.CLOCK),"clock paused at unloaded wire boundary");
         missing.unloaded=false;w.chunks.put(World.key(-1,-1),missing);FastChunkCache.clear();Circuit.loaded(w,missing);advance(w,70);
         eq(3,val(w,CircuitComputer.ACC),"restored machine resumes next ADD, not a reset program");w.shutdown();
      });
      test("computer/facing: full raw properties and Reset commands traverse multiplayer BLOCK parsing",()->{
         Game g=newGame(1);World w=g.world;
         java.lang.reflect.Field inboxField=Net319.class.getDeclaredField("inbox");inboxField.setAccessible(true);
         @SuppressWarnings("unchecked") java.util.Queue<String> inbox=(java.util.Queue<String>)inboxField.get(null);
         java.lang.reflect.Field client=Net319.class.getDeclaredField("clientConnected");client.setAccessible(true);
         boolean original=client.getBoolean(null);
         try {
            int chest=Blocks.CHEST|Facing.MASK|BlockState.WATERLOGGED;
            inbox.add("BLOCK 2 80 2 "+chest+" 0");Net319.processInbox(w,g);eq(chest,w.getState(2,80,2),"water/facing survive actual text protocol");
            put(w,5,80,5,Circuit.BUTTON,0,2);Circuit.use(g,5,80,5,w.getBlock(5,80,5));int pulse=w.getState(5,80,5);
            check((pulse>>>12)>0,"button request lives in transmitted state, not client-local timer");
            inbox.add("BLOCK 10 80 10 "+pulse+" 0");Net319.processInbox(w,g);eq(pulse,w.getState(10,80,10),"host receives remote reset request");
            client.setBoolean(null,true);advance(w,12);eq(pulse,w.getState(10,80,10),"remote client does not simulate authoritative controls");
            client.setBoolean(null,false);advance(w,1);eq(15,val(w,10,80,10),"host emits reset pulse");advance(w,12);eq(0,val(w,10,80,10),"host releases reset pulse");
         } finally {client.setBoolean(null,original);Net319.clearWorldState();inbox.clear();w.shutdown();}
      });
      test("facing: actual furnace cooking retains facing and inventory",()->{
         Game g=newGame(1);World w=g.world;w.setState(2,70,2,Blocks.FURNACE|Facing.MASK);
         TileEntity t=w.getOrCreateTile(2,70,2);t.slots[0]=new Stack(Blocks.IRON_ORE,1);t.slots[1]=new Stack(Items.COAL,1);
         tick(w,5);eq(Blocks.FURNACE_LIT,w.getBlock(2,70,2),"real ignition");eq(3,Facing.direction(w.getState(2,70,2)),"facing after ignition");
         tick(w,205);eq(Items.IRON_INGOT,t.slots[2].id,"cooking still works");eq(3,Facing.direction(w.getState(2,70,2)),"facing after smelting");
         t.burn=1;tick(w,2);eq(Blocks.FURNACE,w.getBlock(2,70,2),"real extinguish");eq(3,Facing.direction(w.getState(2,70,2)),"facing after extinguish");w.shutdown();
      });
      test("computer: ordinary World.tick drives the entire machine, dust adapter and lamps",()->{
         Game g=machine();World w=g.world;use(g,CircuitComputer.CLOCK);tick(w,180);
         eq(1,val(w,CircuitComputer.PC),"world-tick PC after four instructions");eq(2,val(w,CircuitComputer.ACC),"world-tick accumulator");eq(1,val(w,CircuitComputer.OUT),"world-tick output");
         for(int i=0;i<4;i++){var c=CircuitComputer.BITS[i];eq(i==3?Redstone319.REDSTONE_LAMP_LIT:Redstone319.REDSTONE_LAMP,w.getBlock(OX+c.x(),Y,OZ+c.z()-1),"lamps update without a test-only propagate call");}
         put(w,0,80,0,Circuit.INPUT,0,2);w.setState(-1,80,0,Redstone319.REDSTONE_BLOCK);w.setState(1,80,0,Circuit.CABLE);
         w.setState(2,79,0,Blocks.STONE);w.setState(2,80,0,Redstone319.REDSTONE_WIRE0);w.setState(3,80,0,Redstone319.REDSTONE_LAMP);tick(w,12);
         eq(15,val(w,1,80,0),"ordinary redstone enters adapter");eq(Redstone319.REDSTONE_LAMP_LIT,w.getBlock(3,80,0),"bus drives ordinary dust and lamp");
         w.setState(-1,80,0,0);tick(w,12);eq(0,val(w,1,80,0),"adapter releases without feedback");eq(Redstone319.REDSTONE_LAMP,w.getBlock(3,80,0),"ordinary lamp releases");w.shutdown();
      });
      test("renderer: moving to an elevated computer cannot hide LOD behind out-of-range old meshes",()->{
         for(double x:new double[]{-16.1,-.1,0,8,16,40})for(double z:new double[]{-32,-.1,0,58})for(int d:new int[]{2,5,8}){
            int cx=(int)Math.floor(x/16),cz=(int)Math.floor(z/16);
            check(Renderer.realChunkInRange(cx+d+1,cz,x,z,d),"draw ring boundary covered");
            check(!Renderer.realChunkInRange(cx+d+2,cz,x,z,d),"old mesh outside ring must expose LOD");
            check(!Renderer.realChunkInRange(cx+d+1,cz+d+1,x,z,d),"diagonal outside circular ring must expose LOD");
         }
      });
      test("computer: example builder uses actual command, preserves ground and rejects invalid height/mode",()->{
         Game g=newGame(1);g.runCommand("/computer");check(g.world.circuit.nodes.size()>40,"command builds hardware");eq(Blocks.GRASS,g.world.getBlock(8,GROUND,8),"existing terrain untouched");check(g.player.flying,"safe flying viewpoint");g.world.shutdown();
         Game denied=newGame(0);denied.runCommand("/computer");eq(0,denied.world.circuit.nodes.size(),"survival command rejected");denied.player.setMode(1);denied.player.setPos(8,250,8);denied.runCommand("/computer");eq(0,denied.world.circuit.nodes.size(),"height rejection is all-or-nothing");denied.world.shutdown();
      });
   }
}
