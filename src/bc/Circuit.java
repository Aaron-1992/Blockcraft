package bc;

import java.util.*;

/** Buildable, synchronous four-bit logic. There is no CPU object: every result is produced
 * by the placed components and their actual cable nets. Registers sample together, on edges. */
final class Circuit {
   static final int BASE=2800, TYPES=16, CABLE=3840;
   static final int CONSTANT=0, ALU=1, REGISTER=2, ROM=3, NEXT=4, EQUAL=5,
      AND=6, NOT=7, CLOCK=8, BUTTON=9, DISPLAY=10, BIT=11, ADD=12, INPUT=13, XOR=14, MUX=15;
   static final int PERIOD=40;
   static final String[] NAMES={"Hex Constant","4-bit ALU","4-bit Register","16-word ROM Reader","Program Counter Selector",
      "Equality Gate","4-bit AND Gate","4-bit NOT Gate","Computer Clock","Computer Reset Button","Hex Display",
      "Bit Output","4-bit Adder","Redstone Bus Input","4-bit XOR Gate","4-bit Multiplexer"};
   static final String[] OPS={"LOAD","ADD","SUB","XOR","AND","OR","OUT","JMP","JZ","NOP","NOP","NOP","NOP","NOP","NOP","HALT"};
   static final int[] DX={0,0,1,-1}, DZ={-1,1,0,0};
   static final int[][] DIR={{0,0,-1},{0,0,1},{1,0,0},{-1,0,0},{0,-1,0},{0,1,0}};
   private Circuit(){}
   static final class State {
      final Set<Long> nodes=new HashSet<>(), cables=new HashSet<>();
      final Map<Long,Integer> timers=new HashMap<>();
      final List<List<Long>> nets=new ArrayList<>();
      boolean dirty=true;
      int warmup;
      void clear(){nodes.clear();cables.clear();timers.clear();nets.clear();dirty=true;warmup=0;}
   }
   static boolean node(int id){return id>=BASE&&id<BASE+TYPES*64;}
   static boolean cable(int id){return id>=CABLE&&id<CABLE+16;}
   static boolean handles(int id){return node(id)||cable(id);}
   static int kind(int id){return (id-BASE)/64;}
   static int face(int id){return (id-BASE)&3;}
   static int value(int id){return cable(id)?id-CABLE:((id-BASE)>>2)&15;}
   static int id(int kind,int value,int face){return BASE+kind*64+(value&15)*4+(face&3);}
   static int pick(int id){return cable(id)?CABLE:Circuit.id(kind(id),0,0);}
   static int left(int f){return f==0?3:f==1?2:f==2?0:1;}
   static int right(int f){return left(f)^1;}
   static int alu(int a,int b,int op){return switch(op){case 0->b;case 1->(a+b)&15;case 2->(a-b)&15;case 3->a^b;case 4->a&b;case 5->a|b;default->a;};}
   static int nextAddress(int pc,int operand,int op,int acc){return op==15?pc:op==7||op==8&&acc==0?operand:(pc+1)&15;}

   static void register(){
      for(int type=0;type<TYPES;type++)for(int v=0;v<16;v++)for(int f=0;f<4;f++) {
         int id=id(type,v,f);
         Blocks.def(id,NAMES[type],1,false,UtilityModels.METAL);
         Blocks.solid[id]=true;Blocks.opacity[id]=0;Blocks.drop[id]=Circuit.id(type,0,0);
         Blocks.coll[id]=Blocks.box[id]=new double[]{0,0,0,1,.65,1};
         Shapes.model(id,Shapes.turnAll(model(type,v),Shapes.turns(0,f)));
      }
      for(int v=0;v<16;v++) {
         int id=CABLE+v;Blocks.def(id,"4-bit Bus Cable",.3F,false,UtilityModels.METAL);
         Blocks.solid[id]=true;Blocks.opacity[id]=0;Blocks.drop[id]=CABLE;
         Shapes.model(id,cableModel(v,false));
      }
   }
   static double[] part(double x,double y,double z,double xx,double yy,double zz,int color){
      double[] b=Shapes.tiles(Shapes.px(x,y,z,xx,yy,zz),Tex.WHITE,Tex.WHITE,Tex.WHITE);b[7]=color;return b;
   }
   private static final int[] DIGITS={0x3f,0x06,0x5b,0x4f,0x66,0x6d,0x7d,0x07,0x7f,0x6f,0x77,0x7c,0x39,0x5e,0x79,0x71};
   static double[][] model(int type,int v){
      ArrayList<double[]> p=new ArrayList<>();
      p.add(Shapes.tiles(Shapes.px(0,0,0,16,3,16),UtilityModels.REPEATER_EDGE,UtilityModels.METAL,UtilityModels.METAL));
      p.add(part(1,3,1,15,9,15,type==ROM?0x293663:type==REGISTER?0x3A5364:type==CLOCK?0x653B29:type==BUTTON?0x733938:type==DISPLAY?0x173642:0x294336));
      // Seven-segment hexadecimal readout and four binary LEDs. All values are real block states.
      int[][] seg={{5,3,11,4},{11,4,12,7},{11,8,12,11},{5,11,11,12},{4,8,5,11},{4,4,5,7},{5,7,11,8}};
      for(int i=0;i<7;i++){int[] q=seg[i];p.add(part(q[0],9,q[1],q[2],9.25,q[3],(DIGITS[v]&(1<<i))!=0?0x92EAC6:0x23372F));}
      for(int b=0;b<4;b++)p.add(part(2+b*3,9,13,4+b*3,9.3,14,(v&(8>>b))!=0?0xEBAF52:0x433B2A));
      p.add(part(6,3,0,10,5,1,0xCC6250)); // front = output; back = A/data, left = B/clock, right = mode/reset
      p.add(part(6,3,15,10,5,16,0x54B9D0));
      p.add(part(0,3,6,1,5,10,0xE2B863));p.add(part(15,3,6,16,5,10,0xB38CD9));
      return p.toArray(new double[0][]);
   }
   static double[][] cableModel(int v,boolean fault){
      int c=fault?0xF04444:v==0?0x45495B:0x646AAA+v*0x060300;
      return new double[][]{part(0,6,6,16,10,10,c),part(6,0,6,10,16,10,c),part(6,6,0,10,10,16,c)};
   }
   static double[][] cableBoxes(Shapes.Nbr n,int value,boolean fault){
      double[][] all=cableModel(value,fault);int color=(int)all[0][7];
      List<double[]> p=new ArrayList<>();p.add(part(6,6,6,10,10,10,color));
      for(int d=0;d<6;d++)if(handles(n.at(Circuit.DIR[d][0],Circuit.DIR[d][1],Circuit.DIR[d][2]))){
         int dx=DIR[d][0],dy=DIR[d][1],dz=DIR[d][2];
         p.add(part(dx<0?0:6,dy<0?0:6,dz<0?0:6,dx>0?16:10,dy>0?16:10,dz>0?16:10,color));
      }
      return p.toArray(new double[0][]);
   }
   static String[] hint(int raw){
      int id=raw&4095,v=value(id),meta=raw>>>12;
      if(cable(id))return new String[]{"4-bit Bus: "+Integer.toHexString(v).toUpperCase(),(raw&0x8000)!=0?"FAULT: conflicting drivers. Separate these nets.":"Signal 0-15; no attenuation. Input adapter accepts redstone."};
      int k=kind(id);
      String detail=switch(k){
         case CONSTANT -> "Right-click: +1 | empty-hand sneak/right-click: -1";
         case CLOCK -> ((meta&1)!=0?"RUNNING":"STOPPED")+" | Right-click: RUN/STOP | empty-hand sneak/right-click: STEP";
         case BUTTON -> "Right-click: RESET connected registers and clock";
         case EQUAL -> "Match "+Integer.toHexString(meta).toUpperCase()+" | Right-click: change comparison";
         case BIT -> "Mask "+Integer.toHexString(meta==0?1:meta).toUpperCase()+" | Right-click: next bit";
         case ROM -> "Instruction: "+OPS[meta]+" "+Integer.toHexString(v).toUpperCase()+" | Physical memory on reader's right";
         case REGISTER -> "Cyan: data | gold: rising clock | violet: reset | red: output";
         default -> "Cyan: A | gold: B | violet: mode | red: output. See Computer-guide.txt";
      };
      return new String[]{NAMES[k]+" = "+Integer.toHexString(v).toUpperCase()+" ("+v+")",detail};
   }
   static void hud(Game g){
      if(g.target==null||g.world==null||g.screen!=Game.S.PLAY)return;
      int raw=g.world.getState(g.target.x,g.target.y,g.target.z);if(!handles(raw&4095))return;
      String[] lines=hint(raw);float width=0;for(String line:lines)width=Math.max(width,Gui.textWidth(line));
      float scale=Math.min(1,(g.gui.w-24)/width),yy=g.gui.h/2+24;
      g.gui.rect((g.gui.w-width*scale)/2-5,yy-4,width*scale+10,25*scale,0xC0162028);
      for(String line:lines){g.gui.centered(line,g.gui.w/2,yy,0xFFD9F4EB,true,scale);yy+=11*scale;}
   }
   static void creative(ArrayList<Integer> list){list.add(CABLE);for(int k=0;k<TYPES;k++)list.add(id(k,0,0));}
   static void recipes(){
      Recipes.shaped(CABLE,8,new String[]{"GRG","GRG","GRG"},'G',Blocks.GLASS,'R',Redstone319.ITEM_REDSTONE);
      for(int k=0;k<TYPES;k++)Recipes.shaped(id(k,0,0),1,new String[]{" R ","RCR","SQS"},
         'R',Redstone319.ITEM_REDSTONE,'C',k==0?Blocks.STONE:id(k-1,0,0),'S',Blocks.STONE,'Q',Items.QUARTZ);
   }
   static void changed(World w,int x,int y,int z){
      long key=World.pack(x,y,z);int id=w.getBlock(x,y,z);State s=w.circuit;
      if(node(id)){s.nodes.add(key);}else if(s.nodes.remove(key)){s.timers.remove(key);}
      boolean old=s.cables.contains(key);
      if(cable(id))s.cables.add(key);else s.cables.remove(key);
      if(old!=cable(id))s.dirty=true;
   }
   static void loaded(World w,Chunk c){
      for(int i=0;i<c.blocks.length;i++)if(handles(c.blocks[i]&4095)) {
         int x=c.cx*16+(i&15),y=i>>8,z=c.cz*16+((i>>4)&15);changed(w,x,y,z);w.circuit.warmup=8;
         if(node(w.getBlock(x,y,z))&&kind(w.getBlock(x,y,z))==CLOCK)w.circuit.timers.put(World.pack(x,y,z),PERIOD);
      }
   }
   /** Negative means this face is NOT a driver; zero is a driven zero. */
   static int output(World w,int x,int y,int z,int tx,int ty,int tz){
      int raw=w.getState(x,y,z),id=raw&4095;
      if(cable(id))return (raw&0x8000)!=0?0:value(id);
      if(!node(id)||ty!=y)return -1;
      int f=face(id);
      if(tx==x+DX[f]&&tz==z+DZ[f])return value(id);
      if(kind(id)==ROM){int l=left(f);if(tx==x+DX[l]&&tz==z+DZ[l])return (raw>>>12)&15;}
      return -1;
   }
   static int input(World w,int x,int y,int z,int d){
      int nx=x+DIR[d][0],ny=y+DIR[d][1],nz=z+DIR[d][2];
      int p=output(w,nx,ny,nz,x,y,z);
      return p>=0?p:RedstoneLogic.signal(w,nx,ny,nz,x,y,z,true);
   }
   static int romWord(World w,int x,int y,int z,int f,int address){
      int r=right(f),ox=x+DX[r]*3-DX[f]*(2+(address&15)),oz=z+DZ[r]*3-DZ[f]*(2+(address&15));
      int op=w.getBlock(ox,y,oz),arg=w.getBlock(ox+DX[r]*2,y,oz+DZ[r]*2);
      if(!node(op)||kind(op)!=CONSTANT||!node(arg)||kind(arg)!=CONSTANT)return 0xF0; // missing memory halts
      return value(op)*16+value(arg);
   }
   static void rebuild(World w){
      State s=w.circuit;s.nets.clear();Set<Long> unseen=new HashSet<>(s.cables);
      while(!unseen.isEmpty()){
         long start=unseen.iterator().next();unseen.remove(start);
         List<Long> net=new ArrayList<>();ArrayDeque<Long> q=new ArrayDeque<>();q.add(start);
         while(!q.isEmpty()){
            long k=q.remove();net.add(k);int x=World.ux(k),y=World.uy(k),z=World.uz(k);
            for(int[] d:DIR)if(y+d[1]>=0&&y+d[1]<Chunk.H){long n=World.pack(x+d[0],y+d[1],z+d[2]);if(unseen.remove(n))q.add(n);}
         }
         s.nets.add(net);
      }
      s.dirty=false;
   }
   static void tick(World w){
      if(Net319.isClientConnected()&&!Net319.isHosting())return;
      State s=w.circuit;
      if(s.nodes.isEmpty()&&s.cables.isEmpty())return;
      if(s.nodes.removeIf(k->!w.isLoaded(World.ux(k),World.uz(k))))s.timers.keySet().retainAll(s.nodes);
      if(s.cables.removeIf(k->!w.isLoaded(World.ux(k),World.uz(k))))s.dirty=true;
      if(s.dirty)rebuild(w);
      boolean complete=true;
      for(long k:s.cables)for(int d=0;d<4;d++)if(!w.isLoaded(World.ux(k)+DX[d],World.uz(k)+DZ[d]))complete=false;
      if(!complete)s.warmup=8;
      boolean freeze=s.warmup>0;
      // Bus nets are resolved from component outputs, not from their own previous charge.
      Map<Long,Integer> writes=new HashMap<>();
      for(List<Long> net:s.nets){
         int driver=-1;boolean fault=false;
         for(long k:net){int x=World.ux(k),y=World.uy(k),z=World.uz(k);
            for(int[] d:DIR){int nx=x+d[0],ny=y+d[1],nz=z+d[2];
               if(!node(w.getBlock(nx,ny,nz)))continue;
               int out=output(w,nx,ny,nz,x,y,z);
               if(out>=0){if(driver>=0&&driver!=out)fault=true;driver=out;}
            }
         }
         int raw=CABLE+(fault||driver<0?0:driver)|(fault?0x8000:0);
         for(long k:net)if(w.getState(World.ux(k),World.uy(k),World.uz(k))!=raw)writes.put(k,raw);
      }
      apply(w,writes);writes.clear();
      // Read every register's old output before any component commits a new result.
      for(long k:new ArrayList<>(s.nodes)){
         int x=World.ux(k),y=World.uy(k),z=World.uz(k),raw=w.getState(x,y,z),id=raw&4095;
         if(!node(id))continue;
         int type=kind(id),f=face(id),v=value(id),meta=raw>>>12;
         int a=input(w,x,y,z,f^1),b=input(w,x,y,z,left(f)),c=input(w,x,y,z,right(f));
         int out=v,props=meta;
         switch(type){
            case CONSTANT -> {}
            case ALU -> out=alu(a,b,c);
            case REGISTER -> {out=freeze?v:c>0?0:b>0&&(meta&1)==0?a:v;props=b>0?1:0;}
            case ROM -> {int word=romWord(w,x,y,z,f,a);out=word&15;props=word>>4;}
            case NEXT -> out=nextAddress(a,b,c,input(w,x,y,z,4));
            case EQUAL -> out=a==meta?15:0;
            case AND -> out=a&b;
            case NOT -> out=(~a)&15;
            case XOR -> out=a^b;
            case ADD -> out=(a+b)&15;
            case MUX -> out=c>0?b:a;
            case DISPLAY,INPUT -> out=a;
            case BIT -> out=(a&(meta==0?1:meta))!=0?15:0;
            case BUTTON -> {out=meta>0?15:0;props=Math.max(0,meta-1);}
            case CLOCK -> {
               int rem=s.timers.getOrDefault(k,PERIOD);out=0;
               if(freeze)rem=PERIOD;
               else if(c>0){rem=PERIOD;props=meta&1;}
               else if(rem>0)rem--;
               else if(a==0&&(meta&3)!=0){out=15;rem=PERIOD;props=meta&1;}
               s.timers.put(k,rem);
            }
         }
         int next=id(type,out,f)|(props<<12);
         if(next!=raw)writes.put(k,next);
      }
      apply(w,writes);
      if(s.warmup>0)s.warmup--;
   }
   static void apply(World w,Map<Long,Integer> states){for(Map.Entry<Long,Integer> e:states.entrySet()){long k=e.getKey();w.setState(World.ux(k),World.uy(k),World.uz(k),e.getValue());}}
   static boolean use(Game g,int x,int y,int z,int id){
      if(!handles(id))return false;
      World w=g.world;int raw=w.getState(x,y,z),meta=raw>>>12;
      if(cable(id)){g.say((raw&0x8000)!=0?"BUS FAULT: conflicting drivers. Separate the cable nets.":"Bus: "+Integer.toHexString(value(id)).toUpperCase()+" (0-15)");return true;}
      int k=kind(id),v=value(id),f=face(id);
      if(k==CONSTANT)w.setState(x,y,z,id(k,(v+(g.player.sneaking?15:1))&15,f));
      else if(k==EQUAL)w.setState(x,y,z,id|(((meta+1)&15)<<12));
      else if(k==BIT)w.setState(x,y,z,id|((meta==0?2:meta>=8?1:meta*2)<<12));
      else if(k==CLOCK){w.setState(x,y,z,id|((g.player.sneaking?meta|2:(meta^1)&1)<<12));g.say(g.player.sneaking?"Computer: STEP queued":((meta&1)==0?"Computer: RUN":"Computer: STOP"));return true;}
      else if(k==BUTTON)w.setState(x,y,z,id(k,15,f)|(10<<12));
      else {g.say(NAMES[k]+": "+Integer.toHexString(v).toUpperCase()+". Cyan=back/data, gold=left/clock, violet=right/reset, red=output.");return true;}
      g.say(NAMES[k]+": "+(k==EQUAL?"match "+((meta+1)&15):value(w.getBlock(x,y,z))));return true;
   }
}
