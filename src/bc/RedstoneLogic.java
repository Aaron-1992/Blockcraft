package bc;

import java.util.*;

/** Java-style signal routing and game-tick scheduling. All transient state belongs to its World. */
final class RedstoneLogic {
   static final int COMPARATOR = 2600; // 64 states: output 0..15 x four facings; mode is a saved property bit.
   static final int[] DX = {0, 0, 1, -1}, DZ = {-1, 1, 0, 0};
   static final int[][] SIX = {{0,-1,0},{0,1,0},{0,0,-1},{0,0,1},{1,0,0},{-1,0,0}};
   static final int REPEAT = 1, TORCH = 2, OBSERVE_ON = 3, OBSERVE_OFF = 4, LAMP = 5, COMPARE = 6;
   record Event(long due, long order, long pos, int oldId, int kind, int value) {}
   static final class State {
      long sequence;
      boolean notesSaveBlocked;final Map<Long,Long> noteHits=new HashMap<>();
      boolean updating;
      final PriorityQueue<Event> queue = new PriorityQueue<>(Comparator.comparingLong(Event::due).thenComparingLong(Event::order));
      final Map<Long, Event> pending = new HashMap<>();
      final Map<Long, Integer> buttons = new HashMap<>(), notes = new HashMap<>();
      final Set<Long> opened = new HashSet<>(), plates = new HashSet<>(), sensors = new HashSet<>(), comparators = new HashSet<>();
      final Map<Long, ArrayDeque<Long>> torchChanges = new HashMap<>();
      final Map<Long, Long> burnout = new HashMap<>(), extendedAt = new HashMap<>();
      /** Update 6.18: reused for the periodic sensor/comparator scans (they used to copy both sets every two ticks). */
      final ArrayList<Long> scan = new ArrayList<>();
      void clear() { noteHits.clear();notesSaveBlocked=false; queue.clear(); pending.clear(); buttons.clear(); notes.clear(); opened.clear(); plates.clear(); sensors.clear(); comparators.clear(); torchChanges.clear(); burnout.clear(); extendedAt.clear(); scan.clear(); }
   }
   private RedstoneLogic() {}
   static boolean comparator(int id) { return id >= COMPARATOR && id < COMPARATOR + 64; }
   static int facing(int id) { return comparator(id) ? (id - COMPARATOR) & 3 : Redstone319.repeaterFacing(id); }
   static int delay(World w, int x, int y, int z) { return 2 * (1 + ((w.getState(x,y,z) >> 12) & 3)); }
   static boolean conductor(int id) { return Blocks.opaque[id] && !Blocks.isFluid(id); }

   static void register() {
      for (int power = 0; power < 16; power++) for (int f = 0; f < 4; f++) {
         int id = COMPARATOR + power * 4 + f;
         Blocks.def(id, "Redstone Comparator", 0, false, Redstone319.T_REPEATER_OFF);
         Blocks.opacity[id] = 0; Blocks.solid[id] = true; Blocks.drop[id] = COMPARATOR;
         Blocks.coll[id] = Blocks.box[id] = new double[]{0,0,0,1,.125,1};
         int cap = power > 0 ? UtilityModels.RED_HEAD : UtilityModels.DEAD_HEAD;
         Shapes.model(id, Shapes.turnAll(new double[][]{
            Shapes.tiles(Shapes.px(0,0,0,16,2,16), UtilityModels.REPEATER_EDGE, Redstone319.T_REPEATER_OFF, UtilityModels.REPEATER_EDGE),
            Shapes.tiles(Shapes.px(3,2,10,5,6,12), cap, cap, cap),
            Shapes.tiles(Shapes.px(11,2,10,13,6,12), cap, cap, cap),
            Shapes.tiles(Shapes.px(7,2,2,9,5,4), UtilityModels.RED_HEAD, UtilityModels.RED_HEAD, UtilityModels.RED_HEAD)
         }, f == 0 ? 0 : f == 1 ? 2 : f == 2 ? 3 : 1));
      }
   }

   private static final double[][][] REPEATER_MODELS=new double[32][][];
   /** Saved settings also change the real world model; no texture repaint is needed. */
   static double[][] model(int id,int state) {
      if(Redstone319.isRepeater(id)) {
         int setting=(state>>12)&3, f=facing(id), on=Redstone319.isRepeaterOn(id)?1:0;
         int key=setting*8+on*4+f;
         // A tiny immutable geometry cache, safe for asynchronous section meshing.
         synchronized(REPEATER_MODELS) {
            if(REPEATER_MODELS[key]==null) {
               double[][] base=Shapes.modelBoxes(Redstone319.REPEATER_OFF+on*4);
               double[][] copy=new double[base.length][];
               for(int i=0;i<base.length;i++)copy[i]=base[i].clone();
               double shift=(setting*2-6)/16.0;
               for(int i=3;i<copy.length;i++){copy[i][2]+=shift;copy[i][5]+=shift;}
               REPEATER_MODELS[key]=Shapes.turnAll(copy,Shapes.turns(0,f));
            }
            return REPEATER_MODELS[key];
         }
      }
      double[][] base=Shapes.modelBoxes(id),copy=new double[base.length][];
      for(int i=0;i<base.length;i++)copy[i]=base[i];
      copy[3]=base[3].clone();int tile=(state&0x1000)!=0?UtilityModels.RED_HEAD:UtilityModels.DEAD_HEAD;
      copy[3][8]=copy[3][9]=copy[3][10]=tile;return copy;
   }

   /** Directional connection, shared by signal routing and the dust model. */
   static boolean connects(int id, int dir) {
      if(RasterComputer.handles(id))return RasterComputer.kind(id)==1||RasterComputer.kind(id)==8&&(RasterComputer.face(id)>>1)==(dir>>1);
      if(Circuit.handles(id))return Circuit.cable(id)||(Circuit.face(id)>>1)==(dir>>1);
      if (Redstone319.isRepeater(id) || comparator(id)) return (facing(id) >> 1) == (dir >> 1);
      if (Redstone319.isObserver(id)) return Redstone319.observerFacing(id) == dir;
      return Redstone319.isWire(id) || Redstone319.sourcePower(id) > 0
         || id >= Redstone319.REDSTONE_TORCH && id <= Redstone319.REDSTONE_TORCH_OFF
         || id == Redstone319.DAYLIGHT_SENSOR;
   }
   static boolean wireLink(World w, int x, int y, int z, int dir) {
      int nx=x+DX[dir], nz=z+DZ[dir], id=w.getBlock(nx,y,nz);
      return connects(id,dir) || conductor(id) && !conductor(w.getBlock(x,y+1,z)) && Redstone319.isWire(w.getBlock(nx,y+1,nz))
         || !conductor(id) && Redstone319.isWire(w.getBlock(nx,y-1,nz));
   }
   static boolean wirePoints(World w, int x, int y, int z, int dir) {
      boolean a=wireLink(w,x,y,z,0), b=wireLink(w,x,y,z,1), c=wireLink(w,x,y,z,2), d=wireLink(w,x,y,z,3);
      if (!(a||b||c||d)) return true; // isolated dot powers horizontal neighbours
      if ((dir>>1)==0 && !c&&!d) return true;
      if ((dir>>1)==1 && !a&&!b) return true;
      return dir==0?a:dir==1?b:dir==2?c:d;
   }
   static int direct(World w, int x, int y, int z, int tx, int ty, int tz, boolean dust, boolean strong) {
      int id=w.getBlock(x,y,z), dx=tx-x,dy=ty-y,dz=tz-z;
      if(RasterComputer.handles(id)){
         if(strong)return 0;if(RasterComputer.kind(id)==1){RasterVM.Cpu cpu=w.raster.cpus.get(World.pack(x,y,z));int f=RasterComputer.face(id);return cpu!=null&&dy==0&&dx==Circuit.DX[f]&&dz==Circuit.DZ[f]?cpu.out[6]&15:0;}
         return RasterComputer.lampPower(id,dx,dy,dz);
      }
      if(Circuit.handles(id))return strong?0:Math.max(0,Circuit.output(w,x,y,z,tx,ty,tz));
      if (Redstone319.isWire(id)) {
         if (!dust || dy>0) return 0;
         if (dy<0) return id-Redstone319.REDSTONE_WIRE0;
         int f=dz<0?0:dz>0?1:dx>0?2:3;
         return wirePoints(w,x,y,z,f)?id-Redstone319.REDSTONE_WIRE0:0;
      }
      if (Redstone319.isRepeater(id) || comparator(id)) {
         int f=facing(id);
         return dy==0 && dx==DX[f] && dz==DZ[f] ? (comparator(id)?(id-COMPARATOR)/4:Redstone319.isRepeaterOn(id)?15:0) : 0;
      }
      if (Redstone319.isObserver(id)) return Redstone319.observerFeeds(id,x,y,z,tx,ty,tz)?15:0;
      if (id==Redstone319.REDSTONE_TORCH) return (strong?dy>0:dy>=0)?15:0;
      if (id==Redstone319.REDSTONE_BLOCK) return strong?0:15;
      if (id==Redstone319.DAYLIGHT_SENSOR || id==Redstone319.DAYLIGHT_SENSOR_ON) return strong?0:daylight(w,x,y,z);
      int p=Redstone319.sourcePower(id);
      return !strong || dy<0 ? p : 0; // currently available switches/plates attach to the floor
   }
   static int signal(World w,int x,int y,int z,int tx,int ty,int tz,boolean dust) {
      int out=direct(w,x,y,z,tx,ty,tz,dust,false);
      if (conductor(w.getBlock(x,y,z))) for (int[] d:SIX)
         out=Math.max(out,direct(w,x+d[0],y+d[1],z+d[2],x,y,z,dust,true));
      return out;
   }
   static int input(World w,int x,int y,int z,boolean dust) {
      int power=0;
      for(int[] d:SIX)power=Math.max(power,signal(w,x+d[0],y+d[1],z+d[2],x,y,z,dust));
      return power;
   }
   static int rear(World w,int x,int y,int z,int f) { return signal(w,x-DX[f],y,z-DZ[f],x,y,z,true); }
   static boolean locked(World w,int x,int y,int z,int f) {
      for(int side=0;side<4;side++)if((side>>1)!=(f>>1)) {
         int nx=x+DX[side],nz=z+DZ[side],id=w.getBlock(nx,y,nz);
         if ((Redstone319.isRepeater(id)||comparator(id)) && direct(w,nx,y,nz,x,y,z,true,false)>0)return true;
      }
      return false;
   }
   static int fullness(TileEntity tile) {
      if(tile==null || tile.slots.length==0)return 0;
      double total=0; boolean any=false;
      for(Stack s:tile.slots)if(s!=null&&s.n>0){total+=Math.min(1.0,(double)s.n/s.max());any=true;}
      return any?1+(int)Math.floor(total/tile.slots.length*14):0;
   }
   static int comparison(World w,int x,int y,int z) {
      int id=w.getBlock(x,y,z),f=facing(id),rx=x-DX[f],rz=z-DZ[f];
      int rear=rear(w,x,y,z,f), side=0;
      TileEntity tile=w.tileAt(rx,y,rz);
      if(tile==null && conductor(w.getBlock(rx,y,rz)) && rear<15)tile=w.tileAt(rx-DX[f],y,rz-DZ[f]);
      if(tile!=null)rear=fullness(tile);
      for(int d=0;d<4;d++)if((d>>1)!=(f>>1))side=Math.max(side,direct(w,x+DX[d],y,z+DZ[d],x,y,z,true,false));
      return (w.getState(x,y,z)&0x1000)!=0?Math.max(0,rear-side):(rear>=side?rear:0);
   }
   static int daylight(World w,int x,int y,int z) {
      if(w.dim!=Dim.OVERWORLD)return 0;
      double angle=((w.dayTime%24000L)-6000)*Math.PI/12000.0;
      int value=(int)Math.round(Math.max(0,Math.cos(angle))*w.getSky(x,y+1,z));
      if(w.weather.rainingAt(x,y+1,z))value=Math.max(0,value-3);
      value=Math.max(0,Math.min(15,value));
      return (w.getState(x,y,z)&0x1000)!=0?15-value:value;
   }
   static void schedule(World w,int x,int y,int z,int kind,int value,int delay) {
      long key=World.pack(x,y,z);
      if(w.redstone.pending.containsKey(key))return;
      Event e=new Event(w.tickCount+delay,w.redstone.sequence++,key,w.getBlock(x,y,z),kind,value);
      w.redstone.pending.put(key,e);w.redstone.queue.add(e);
   }
   static void evaluate(World w,int x,int y,int z,int id) {
      if(Redstone319.isRepeater(id)) {
         if(!locked(w,x,y,z,facing(id)) && (rear(w,x,y,z,facing(id))>0)!=Redstone319.isRepeaterOn(id))
            schedule(w,x,y,z,REPEAT,rear(w,x,y,z,facing(id))>0?1:0,delay(w,x,y,z));
      } else if(id==Redstone319.REDSTONE_TORCH||id==Redstone319.REDSTONE_TORCH_OFF) {
         long key=World.pack(x,y,z), until=w.redstone.burnout.getOrDefault(key,0L);
         boolean on=input(w,x,y-1,z,true)==0 && w.tickCount>=until;
         if(on!=(id==Redstone319.REDSTONE_TORCH))schedule(w,x,y,z,TORCH,0,2);
      } else if(comparator(id)) {
         w.redstone.comparators.add(World.pack(x,y,z));
         if(comparison(w,x,y,z)!=(id-COMPARATOR)/4)schedule(w,x,y,z,COMPARE,0,2);
      } else if(id==Redstone319.REDSTONE_LAMP_LIT && input(w,x,y,z,true)==0)schedule(w,x,y,z,LAMP,0,4);
   }
   static void changed(World w,int x,int y,int z) {
      int id=w.getBlock(x,y,z);long key=World.pack(x,y,z);
      if(id==Redstone319.DAYLIGHT_SENSOR||id==Redstone319.DAYLIGHT_SENSOR_ON)w.redstone.sensors.add(key);else w.redstone.sensors.remove(key);
      if(comparator(id))w.redstone.comparators.add(key);else w.redstone.comparators.remove(key);
      Event e=w.redstone.pending.get(key);
      if(e!=null && id!=e.oldId())w.redstone.pending.remove(key);
      for(int f=0;f<6;f++) {
         int ox=x-Redstone319.observerDx(f),oy=y-Redstone319.observerDy(f),oz=z-Redstone319.observerDz(f);
         if(oy>=0&&oy<256 && w.getBlock(ox,oy,oz)==Redstone319.observerId(f,false))schedule(w,ox,oy,oz,OBSERVE_ON,0,2);
      }
   }
   static void loaded(World w,Chunk c) {
      for(int i=0;i<c.blocks.length;i++) {
         int id=c.blocks[i]&BlockState.ID_MASK;
         if(WaterPhysics.containsState(c.blocks[i]&65535) && (id!=Blocks.WATER||(c.blocks[i]&0xF000)!=0))
            w.scheduleFluid(c.cx*16+(i&15),i>>8,c.cz*16+((i>>4)&15),5);
         if(Redstone319.isWire(id))w.scheduleBlock(c.cx*16+(i&15),i>>8,c.cz*16+((i>>4)&15));
         if(comparator(id)||Redstone319.isRepeater(id)||id==Redstone319.REDSTONE_TORCH||id==Redstone319.REDSTONE_TORCH_OFF
            ||id==Redstone319.DAYLIGHT_SENSOR||id==Redstone319.DAYLIGHT_SENSOR_ON||Redstone319.observerOn(id)
            ||id==Redstone319.REDSTONE_LAMP_LIT||id==Redstone319.STONE_BUTTON_ON||id==Redstone319.WOOD_BUTTON_ON
            ||id==Redstone319.STONE_PLATE_ON||id==Redstone319.WOOD_PLATE_ON) {
            int x=c.cx*16+(i&15),y=i>>8,z=c.cz*16+((i>>4)&15);
            long key=World.pack(x,y,z);
            if(id==Redstone319.DAYLIGHT_SENSOR||id==Redstone319.DAYLIGHT_SENSOR_ON)w.redstone.sensors.add(key);
            if(comparator(id))w.redstone.comparators.add(key);
            Redstone319.recover(w,x,y,z,id);
            evaluate(w,x,y,z,id);w.scheduleBlock(x,y,z);
            if(Redstone319.observerOn(id))schedule(w,x,y,z,OBSERVE_OFF,0,2);
         }
      }
   }
   /**
    * Update 6.18: when a chunk is unloaded, every scheduled event and cached component state inside it must go
    * with it. Otherwise a long exploration leaves thousands of dead timers, ghosts that fire when the chunk is
    * loaded again and a redstone state that grows for the whole session.
    */
   static boolean inChunk(long key, Chunk c) {
      return (World.ux(key) >> 4) == c.cx && (World.uz(key) >> 4) == c.cz;
   }

   static void unloaded(World w, Chunk c) {
      State s = w.redstone;
      s.queue.removeIf(e -> inChunk(e.pos(), c));
      s.pending.keySet().removeIf(k -> inChunk(k, c));
      s.buttons.keySet().removeIf(k -> inChunk(k, c));
      s.notes.keySet().removeIf(k -> inChunk(k, c));
      s.noteHits.keySet().removeIf(k -> inChunk(k, c));
      s.opened.removeIf(k -> inChunk(k, c));
      s.plates.removeIf(k -> inChunk(k, c));
      s.sensors.removeIf(k -> inChunk(k, c));
      s.comparators.removeIf(k -> inChunk(k, c));
      s.torchChanges.keySet().removeIf(k -> inChunk(k, c));
      s.burnout.keySet().removeIf(k -> inChunk(k, c));
      s.extendedAt.keySet().removeIf(k -> inChunk(k, c));
   }

   /**
    * Update 6.18: pending repeater/observer/torch deadlines and torch burnout used to vanish on quit, so a
    * machine could come back in a different state. They are now stored in level.properties as relative times.
    */
   static void save(World w, java.util.Properties p) {
      StringBuilder events = new StringBuilder();
      int n = 0;
      for (Event e : w.redstone.pending.values()) {
         if (n++ >= 2048) break;
         if (events.length() > 0) events.append(';');
         events.append(World.ux(e.pos())).append(',').append(World.uy(e.pos())).append(',').append(World.uz(e.pos()))
            .append(',').append(e.kind()).append(',').append(e.value()).append(',')
            .append(Math.max(1L, e.due() - w.tickCount)).append(',').append(e.oldId());
      }
      StringBuilder burnout = new StringBuilder();
      n = 0;
      for (java.util.Map.Entry<Long, Long> e : w.redstone.burnout.entrySet()) {
         if (n++ >= 2048) break;
         if (burnout.length() > 0) burnout.append(';');
         burnout.append(World.ux(e.getKey())).append(',').append(World.uy(e.getKey())).append(',')
            .append(World.uz(e.getKey())).append(',').append(Math.max(1L, e.getValue() - w.tickCount));
      }
      p.setProperty("redstone.events", events.toString());
      p.setProperty("redstone.burnout", burnout.toString());
   }

   static void load(World w, java.util.Properties p) {
      String events = p.getProperty("redstone.events", "");
      if (!events.isEmpty()) {
         for (String part : events.split(";")) {
            String[] f = part.split(",");
            if (f.length != 7) continue;
            try {
               restore(w, Integer.parseInt(f[0]), Integer.parseInt(f[1]), Integer.parseInt(f[2]),
                  Integer.parseInt(f[3]), Integer.parseInt(f[4]), Integer.parseInt(f[5]), Integer.parseInt(f[6]));
            } catch (NumberFormatException ignored) {
               // a hand-edited or older level.properties entry: ignore it, the circuit just starts idle
            }
         }
      }
      String burnout = p.getProperty("redstone.burnout", "");
      if (!burnout.isEmpty()) {
         for (String part : burnout.split(";")) {
            String[] f = part.split(",");
            if (f.length != 4) continue;
            try {
               long key = World.pack(Integer.parseInt(f[0]), Integer.parseInt(f[1]), Integer.parseInt(f[2]));
               w.redstone.burnout.put(key, w.tickCount + Math.max(1L, Long.parseLong(f[3])));
            } catch (NumberFormatException ignored) {
            }
         }
      }
   }

   private static void restore(World w, int x, int y, int z, int kind, int value, int delay, int oldId) {
      long key = World.pack(x, y, z);
      if (w.redstone.pending.containsKey(key)) return;
      Event e = new Event(w.tickCount + Math.max(1, delay), w.redstone.sequence++, key, oldId, kind, value);
      w.redstone.pending.put(key, e);
      w.redstone.queue.add(e);
   }

   static void tick(World w) {
      if(Net319.isClientConnected()&&!Net319.isHosting())return;
      // Update 6.18: a bounded number of events per tick. A huge circuit used to be able to run 65536 updates in
      // one frame; the rest simply wait for the next tick instead of producing a single enormous stutter.
      int budget=4096;
      while(!w.redstone.queue.isEmpty()&&w.redstone.queue.peek().due()<=w.tickCount&&budget-->0) {
         Event e=w.redstone.queue.remove();
         if(w.redstone.pending.get(e.pos())!=e)continue;
         w.redstone.pending.remove(e.pos());
         int x=World.ux(e.pos()),y=World.uy(e.pos()),z=World.uz(e.pos());
         if(!w.isLoaded(x,z))continue;
         int id=w.getBlock(x,y,z);if(id!=e.oldId())continue;
         int next=id;
         switch(e.kind()) {
            case REPEAT -> { if(!locked(w,x,y,z,facing(id)))next=(e.value()>0?Redstone319.REPEATER_ON:Redstone319.REPEATER_OFF)+facing(id); }
            case COMPARE -> next=COMPARATOR+facing(id)+comparison(w,x,y,z)*4;
            case OBSERVE_ON -> next=Redstone319.observerId(Redstone319.observerFacing(id),true);
            case OBSERVE_OFF -> next=Redstone319.observerId(Redstone319.observerFacing(id),false);
            case LAMP -> { if(input(w,x,y,z,true)==0)next=Redstone319.REDSTONE_LAMP; }
            case TORCH -> {
               boolean on=input(w,x,y-1,z,true)==0 && w.tickCount>=w.redstone.burnout.getOrDefault(e.pos(),0L);
               next=on?Redstone319.REDSTONE_TORCH:Redstone319.REDSTONE_TORCH_OFF;
               if(next==Redstone319.REDSTONE_TORCH_OFF && id==Redstone319.REDSTONE_TORCH) {
                  ArrayDeque<Long> changes=w.redstone.torchChanges.computeIfAbsent(e.pos(),k->new ArrayDeque<>());
                  while(!changes.isEmpty()&&changes.peek()<w.tickCount-60)changes.remove();
                  changes.add(w.tickCount);
                  if(changes.size()>=8)w.redstone.burnout.put(e.pos(),w.tickCount+160);
               }
            }
         }
         if(next!=id) { w.setBlock(x,y,z,next);Redstone319.propagate(w,x,y,z); }
         if(e.kind()==OBSERVE_ON)schedule(w,x,y,z,OBSERVE_OFF,0,2);
         if(e.kind()==TORCH && w.redstone.burnout.getOrDefault(e.pos(),0L)>w.tickCount)
            schedule(w,x,y,z,TORCH,0,(int)(w.redstone.burnout.get(e.pos())-w.tickCount));
         evaluate(w,x,y,z,w.getBlock(x,y,z));
      }
      if(w.tickCount%20==0) {
         w.redstone.scan.clear();
         w.redstone.scan.addAll(w.redstone.sensors);
         for(long k : w.redstone.scan) {
            int x=World.ux(k),y=World.uy(k),z=World.uz(k);
            if(w.isLoaded(x,z)) { w.setBlock(x,y,z,daylight(w,x,y,z)>0?Redstone319.DAYLIGHT_SENSOR_ON:Redstone319.DAYLIGHT_SENSOR);Redstone319.propagate(w,x,y,z); }
            else w.redstone.sensors.remove(k);
         }
      }
      if((w.tickCount&1)==0) {
         w.redstone.scan.clear();
         w.redstone.scan.addAll(w.redstone.comparators);
         for(long k : w.redstone.scan) {
            int x=World.ux(k),y=World.uy(k),z=World.uz(k);
            if(w.isLoaded(x,z))evaluate(w,x,y,z,w.getBlock(x,y,z));
            else w.redstone.comparators.remove(k);
         }
      }
   }
}
