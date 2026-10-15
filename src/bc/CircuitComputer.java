package bc;

import java.util.*;

/** A physical 4-bit accumulator computer. This class only builds blocks; it never executes instructions.
 * Alter or break a wire/chip/ROM cell and the machine's behavior changes through Circuit's ordinary rules. */
final class CircuitComputer {
   static final int WIDTH=47,DEPTH=61;
   record Chip(int x,int z,int type){}
   record Port(Chip chip,int direction){}
   static final Chip PC=new Chip(8,8,Circuit.REGISTER), ROM=new Chip(22,8,Circuit.ROM),
      ALU=new Chip(38,8,Circuit.ALU), ACC=new Chip(38,20,Circuit.REGISTER),
      NEXT=new Chip(8,20,Circuit.NEXT), OUT=new Chip(38,32,Circuit.REGISTER),
      EQOUT=new Chip(8,32,Circuit.EQUAL), EQHALT=new Chip(20,32,Circuit.EQUAL),
      WRITE=new Chip(8,44,Circuit.AND), CLOCK=new Chip(20,44,Circuit.CLOCK),
      RESET=new Chip(32,44,Circuit.BUTTON), DISPLAY=new Chip(38,44,Circuit.DISPLAY);
   static final Chip[] BITS={new Chip(8,54,Circuit.BIT),new Chip(18,54,Circuit.BIT),new Chip(28,54,Circuit.BIT),new Chip(38,54,Circuit.BIT)};
   private CircuitComputer(){}
   static Port p(Chip c,int direction){return new Port(c,direction);}
   static List<List<Port>> nets(){
      return List.of(
         List.of(p(PC,0),p(ROM,1),p(NEXT,1)),
         List.of(p(ROM,0),p(ALU,3),p(NEXT,3)),
         List.of(p(ROM,3),p(ALU,2),p(NEXT,2),p(EQOUT,1),p(EQHALT,1)),
         List.of(p(ALU,0),p(ACC,1)),
         List.of(p(ACC,0),p(ALU,1),p(NEXT,4),p(OUT,1)),
         List.of(p(NEXT,0),p(PC,1)),
         List.of(p(CLOCK,0),p(PC,3),p(ACC,3),p(WRITE,1)),
         List.of(p(EQOUT,0),p(WRITE,3)),
         List.of(p(WRITE,0),p(OUT,3)),
         List.of(p(EQHALT,0),p(CLOCK,1)),
         List.of(p(RESET,0),p(PC,2),p(ACC,2),p(OUT,2),p(CLOCK,2)),
         List.of(p(OUT,0),p(DISPLAY,1),p(BITS[0],1),p(BITS[1],1),p(BITS[2],1),p(BITS[3],1))
      );
   }
   static int cx(Port p){return p.chip.x+(p.direction<4?Circuit.DIR[p.direction][0]*3:0);}
   static int cz(Port p){return p.chip.z+(p.direction<4?Circuit.DIR[p.direction][2]*3:0);}
   static final class Plan {
      final Map<Long,Integer> blocks=new LinkedHashMap<>();
      final Map<Long,Integer> wireNet=new HashMap<>();
      void put(int x,int y,int z,int state){blocks.put(World.pack(x,y,z),state);}
      void wire(int x,int y,int z,int net){
         long k=World.pack(x,y,z);Integer old=wireNet.putIfAbsent(k,net);
         if(old!=null&&old!=net)throw new IllegalStateException("Cable crossing "+x+","+y+","+z);
         put(x,y,z,Circuit.CABLE);
      }
   }
   static Plan plan(int ox,int y,int oz){
      Plan plan=new Plan();List<List<Port>> nets=nets();
      int[][] owner=new int[WIDTH][DEPTH];for(int[] col:owner)Arrays.fill(col,-1);
      for(int n=0;n<nets.size();n++)for(Port p:nets.get(n)){
         int x=cx(p),z=cz(p);if(owner[x][z]>=0&&owner[x][z]!=n)throw new IllegalStateException("Shared port");owner[x][z]=n;
      }
      for(int x=0;x<WIDTH;x++)for(int z=0;z<DEPTH;z++)plan.put(ox+x,y-1,oz+z,Blocks.GLASS);
      for(int n=0;n<nets.size();n++){
         int layer=y-4-n*2;
         Set<Integer> tree=new HashSet<>();
         for(Port p:nets.get(n)){
            int x=cx(p),z=cz(p);
            if(p.direction<4)for(int d=1;d<=3;d++)plan.wire(ox+p.chip.x+Circuit.DIR[p.direction][0]*d,y,oz+p.chip.z+Circuit.DIR[p.direction][2]*d,n);
            for(int yy=p.direction<4?y:y-1;yy>=layer;yy--)plan.wire(ox+x,yy,oz+z,n);
            int start=z*WIDTH+x;
            if(tree.isEmpty()){tree.add(start);continue;}
            int[] prev=new int[WIDTH*DEPTH];Arrays.fill(prev,-1);prev[start]=start;
            ArrayDeque<Integer> q=new ArrayDeque<>();q.add(start);int end=-1;
            while(!q.isEmpty()){
               int a=q.remove();if(tree.contains(a)){end=a;break;}
               int ax=a%WIDTH,az=a/WIDTH;
               for(int d=0;d<4;d++){
                  int nx=ax+Circuit.DX[d],nz=az+Circuit.DZ[d];
                  if(nx<1||nz<1||nx>=WIDTH-1||nz>=DEPTH-1)continue;
                  int next=nz*WIDTH+nx;if(prev[next]>=0)continue;
                  boolean blocked=false;
                  // Keep each cable at least a block away from other nets' vertical trunks.
                  for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)if(Math.abs(dx)+Math.abs(dz)<=1){
                     int who=owner[nx+dx][nz+dz];if(who>=0&&who!=n)blocked=true;
                  }
                  if(!blocked){prev[next]=a;q.add(next);}
               }
            }
            if(end<0)throw new IllegalStateException("Unroutable bus "+n);
            for(int a=end;;a=prev[a]){tree.add(a);plan.wire(ox+a%WIDTH,layer,oz+a/WIDTH,n);if(a==start)break;}
         }
      }
      for(Chip c:new Chip[]{PC,ROM,ALU,ACC,NEXT,OUT,EQOUT,EQHALT,WRITE,CLOCK,RESET,DISPLAY}){
         int meta=c==EQOUT?6:c==EQHALT?15:0;plan.put(ox+c.x,y,oz+c.z,Circuit.id(c.type,0,0)|(meta<<12));
      }
      for(int i=0;i<4;i++){
         Chip c=BITS[i];plan.put(ox+c.x,y,oz+c.z,Circuit.id(Circuit.BIT,0,0)|((8>>i)<<12));
         plan.put(ox+c.x,y,oz+c.z-1,Redstone319.REDSTONE_LAMP);
      }
      int[] program={0x01,0x60,0x11,0x71}; // LOAD 1; OUT; ADD 1; JMP 1. Remaining words HALT.
      for(int a=0;a<16;a++){
         int word=a<program.length?program[a]:0xF0;
         plan.put(ox+ROM.x+3,y,oz+ROM.z+2+a,Circuit.id(Circuit.CONSTANT,word>>4,0));
         plan.put(ox+ROM.x+5,y,oz+ROM.z+2+a,Circuit.id(Circuit.CONSTANT,word&15,0));
      }
      // Detect even adjacent (not merely overlapping) nets before placing a single block.
      for(Map.Entry<Long,Integer> e:plan.wireNet.entrySet()){
         long k=e.getKey();for(int[] d:Circuit.DIR){Integer other=plan.wireNet.get(World.pack(World.ux(k)+d[0],World.uy(k)+d[1],World.uz(k)+d[2]));
            if(other!=null&&!other.equals(e.getValue()))throw new IllegalStateException("Adjacent short between buses "+e.getValue()+" and "+other);
         }
      }
      return plan;
   }
   /** Actual-game capture hook: invokes the public command and its real Run control. */
   static void smoke(Game g,int frame){
      if(frame==80){
         g.runCommand("/computer");
         if(g.world.circuit.nodes.size()<40)throw new IllegalStateException("Computer smoke fixture failed to build");
         for(long k:new ArrayList<>(g.world.circuit.nodes))if(Circuit.kind(g.world.getBlock(World.ux(k),World.uy(k),World.uz(k)))==Circuit.CLOCK){
            int x=World.ux(k),y=World.uy(k),z=World.uz(k);
            g.player.sneaking=false;Circuit.use(g,x,y,z,g.world.getBlock(x,y,z));
            boolean close="close".equals(System.getProperty("bc.computer"));
            g.player.setPos(x+(close?6:3),y+(close?8:32),z+(close?16:36));g.player.yaw=180;g.player.pitch=close?25:35;
         }
      }
      if(frame>80&&frame%60==0){
         StringBuilder values=new StringBuilder();
         for(long k:g.world.circuit.nodes){int id=g.world.getBlock(World.ux(k),World.uy(k),World.uz(k));
            if(Circuit.kind(id)==Circuit.DISPLAY||Circuit.kind(id)==Circuit.REGISTER)values.append(" ").append(Circuit.NAMES[Circuit.kind(id)]).append("@").append(World.ux(k)).append(",").append(World.uz(k)).append("=").append(Circuit.value(id));
         }
         System.out.println("[computer-smoke] frame="+frame+values);
      }
   }
   static void build(Game g){
      if(!g.player.creative){g.say("/computer needs Creative mode. The individual components can also be crafted.");return;}
      if(Net319.isClientConnected()&&!Net319.isHosting()){g.say("Ask the host to build the demonstration computer.");return;}
      World w=g.world;int ox=(int)Math.floor(g.player.x)-23,oz=(int)Math.floor(g.player.z)-30;
      int top=(int)Math.floor(g.player.y);
      for(int x=ox-1;x<=ox+WIDTH;x++)for(int z=oz-1;z<=oz+DEPTH;z++){
         Chunk c=w.chunkAt(x,z);if(c==null){g.say("Let nearby chunks load (view distance 5+) and try /computer again.");return;}
         top=Math.max(top,c.hm[(z&15)*16+(x&15)]);
      }
      int y=top+34;if(y>247){g.say("Not enough room overhead. Try /computer over lower terrain.");return;}
      Plan plan=plan(ox,y,oz);
      for(long k:plan.blocks.keySet())if(w.getBlock(World.ux(k),World.uy(k),World.uz(k))!=0){g.say("Construction cancelled: the computer would replace existing blocks.");return;}
      for(Map.Entry<Long,Integer> e:plan.blocks.entrySet()){long k=e.getKey();w.setState(World.ux(k),World.uy(k),World.uz(k),e.getValue());}
      g.player.setPos(ox+23,y+22,oz+70);g.player.yaw=180;g.player.pitch=32;g.player.flying=true;
      g.say("Built a real 4-bit circuit computer at "+ox+", "+y+", "+oz+". Demo counts 1..F,0.");
      g.say("Clock (orange chip): right-click RUN/STOP; empty-hand sneak/right-click STEP. Reset button beside it.");
      g.say("ROM's two columns are opcode / operand; right-click a cell to edit. See Computer-guide.txt.");
   }
}
