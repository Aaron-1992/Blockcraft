package bc;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Connected, reusable raster hardware. Programs reside in ROM modules; work RAM is a separate block.
 * Display panels contain 4x4 pixels each. Rectangular walls join without seams, up to 64x64 pixels. */
final class RasterComputer {
   static final int PANEL=3920,CPU=3924,ROM=3928,RAM=3932,GPU=3936,KEYS=3940,LAMPS=3944,CABLE=3948,PIXEL=3952,SPEAKER=3960,CARTRIDGE=3964;
   static final int[] RX={-1,1,0,0},RZ={0,0,-1,1};
   static final int[] PALETTE={0x070C13,0x43D8EF,0xFFE26B,0xBA79EF,0x6484FF,0xFFA14F,0x75D85C,0xF16F83,0xDCE8EF,0xF04444,0x386091,0x3E724D,0x946544,0x685283,0xE8AF47,0xFFFFFF};
   static final String[] NAMES={"4x4 Display Panel","Raster Processor","Program ROM","4K Word RAM","Graphics Coprocessor","Game Keypad","Lamp Screen Adapter","Raster Data Cable","Lamp Pixel Driver","Raster Speaker","Media Cartridge"};
   static final class State {
      final RasterMedia.State media=new RasterMedia.State();
      final Set<Long> nodes=new HashSet<>();final List<Network> nets=new ArrayList<>();boolean dirty=true;
      final Map<Long,Long> pixels=new ConcurrentHashMap<>();
      final Map<Long,String> statuses=new ConcurrentHashMap<>();
      final Map<Long,RasterVM.Cpu> cpus=new HashMap<>();final Map<Long,int[]> roms=new HashMap<>(),rams=new HashMap<>();
      final Map<Long,int[]> keys=new HashMap<>();
      void clear(){media.slots.clear();media.imported.clear();media.missing.clear();media.tones=0;media.saveBlocked=false;nodes.clear();nets.clear();pixels.clear();statuses.clear();cpus.clear();roms.clear();rams.clear();keys.clear();dirty=true;}
   }
   static final class Network {final List<Long> nodes=new ArrayList<>(),screens=new ArrayList<>(),lamps=new ArrayList<>(),inputs=new ArrayList<>(),speakers=new ArrayList<>(),cartridges=new ArrayList<>();long cpu,rom,ram,key,adapter;int nc,nr,nm,ng,na;boolean complete=true;int width=0,height=0;String error="",mediaError="";}
   static final class Focus {final World w;final long key;int held;Focus(World w,long key){this.w=w;this.key=key;}}
   static boolean handles(int id){return id>=PANEL&&id<=CABLE||id>=PIXEL&&id<CARTRIDGE+4;}
   static int kind(int id){return id>=CARTRIDGE?10:id>=SPEAKER?9:id>=PIXEL?8:id==CABLE?7:(id-PANEL)/4;}
   static int face(int id){return (id-(id>=PIXEL?PIXEL:PANEL))&3;}
   static int pick(int id){int k=kind(id);return k>=9?SPEAKER+(k-9)*4:k==8?PIXEL:k==7?CABLE:PANEL+k*4;}
   static void creative(ArrayList<Integer> list){for(int k=0;k<7;k++)list.add(PANEL+k*4);list.add(CABLE);list.add(PIXEL);list.add(SPEAKER);list.add(CARTRIDGE);}
   static void recipes(){
      Recipes.shaped(CABLE,8,new String[]{" R ","GQG"," R "},'R',Redstone319.ITEM_REDSTONE,'G',Blocks.GLASS,'Q',Items.QUARTZ);
      int[] centers={Blocks.GLASS,Circuit.id(Circuit.ALU,0,0),Circuit.id(Circuit.ROM,0,0),Circuit.id(Circuit.REGISTER,0,0),Circuit.id(Circuit.DISPLAY,0,0),Redstone319.STONE_BUTTON,Redstone319.REDSTONE_LAMP};
      for(int k=0;k<7;k++)Recipes.shaped(PANEL+k*4,1,new String[]{" R ","QCQ"," S "},'R',CABLE,'Q',Items.QUARTZ,'C',centers[k],'S',Blocks.STONE);
      Recipes.shaped(PIXEL,8,new String[]{" R ","QGQ"," R "},'R',CABLE,'Q',Items.QUARTZ,'G',Redstone319.REDSTONE_LAMP);
   }
   static void mediaRecipes(){
      Recipes.shaped(SPEAKER,1,new String[]{"SRS","RQR","SRS"},'S',Blocks.STONE,'R',CABLE,'Q',Items.QUARTZ);
      Recipes.shaped(CARTRIDGE,1,new String[]{"GQG","QRQ","GQG"},'G',Blocks.GLASS,'Q',Items.QUARTZ,'R',ROM);
   }
   static int lampScale(World w,Network n){return n.na==0?1:1<<((w.getState(World.ux(n.adapter),World.uy(n.adapter),World.uz(n.adapter))>>>12)&3);}
   static void register(){
      for(int k=0;k<11;k++)for(int v=0;v<(k==8?2:1);v++)for(int f=0;f<(k==7?1:4);f++){
         int id=k>=9?SPEAKER+(k-9)*4+f:k==8?PIXEL+v*4+f:k==7?CABLE:PANEL+k*4+f;
         Blocks.def(id,NAMES[k],1,false,UtilityModels.METAL);Blocks.solid[id]=true;Blocks.opacity[id]=0;Blocks.drop[id]=pick(id);
         Shapes.model(id,k==0?panelModel(f,0):Shapes.turnAll(moduleModel(k,v),Shapes.turns(1,f)));
      }
   }
   static double[][] moduleModel(int kind,int value){
      if(kind==9)return new double[][]{Circuit.part(0,0,0,16,16,14,0x283C4A),Circuit.part(2,2,14,14,14,16,0x080E14),Circuit.part(5,2,15,11,14,16,0x607987),Circuit.part(2,5,15,14,11,16,0x607987),Circuit.part(6,6,15,10,10,16,0x131E28)};
      if(kind==10)return new double[][]{Circuit.part(0,0,0,16,12,16,0x7A435E),Circuit.part(3,12,3,13,16,12,0x18222C),Circuit.part(5,12,12,11,15,14,0xDEC8A1)};
      if(kind==7)return new double[][]{Circuit.part(0,6,6,16,10,10,0x37969D),Circuit.part(6,0,6,10,16,10,0x37969D),Circuit.part(6,6,0,10,10,16,0x37969D)};
      int[] colors={0,0x365975,0x72547B,0x3F7560,0x805F33,0x405E83,0x777044,0,0x3A6971};
      return new double[][]{Circuit.part(0,0,0,16,12,16,colors[kind]),Circuit.part(2,12,2,14,13,14,0x172637),Circuit.part(4,13,4,12,14,12,kind==8?(value==0?0x314047:0xFFD77D):0x71C5B3),Circuit.part(6,3,15,10,6,16,0xCC6250)};
   }
   static double[][] panelModel(int f,long pixels){
      // Merge equal adjacent pixels geometrically, never in the framebuffer. Large monochrome
      // video screens otherwise rebuild six hidden faces for every tiny pixel on every frame.
      ArrayList<double[]> boxes=new ArrayList<>();boxes.add(Circuit.part(0,0,0,16,16,15,0x172431));boolean[] used=new boolean[16];
      for(int y=0;y<4;y++)for(int x=0;x<4;x++)if(!used[y*4+x]){
         int color=(int)(pixels>>>((y*4+x)*4))&15,w=1,h=1;
         while(x+w<4&&!used[y*4+x+w]&&((pixels>>>((y*4+x+w)*4))&15)==color)w++;
         outer:while(y+h<4){for(int xx=0;xx<w;xx++)if(used[(y+h)*4+x+xx]||((pixels>>>(((y+h)*4+x+xx)*4))&15)!=color)break outer;h++;}
         for(int yy=0;yy<h;yy++)for(int xx=0;xx<w;xx++)used[(y+yy)*4+x+xx]=true;
         boxes.add(Circuit.part(x*4,(4-y-h)*4,15,(x+w)*4,(4-y)*4,16,PALETTE[color]));
      }
      return Shapes.turnAll(boxes.toArray(new double[0][]),Shapes.turns(1,f));
   }
   static double[][] model(Mesher.Scratch s,int x,int y,int z,int id){return panelModel(face(id),s.world==null?0:s.world.raster.pixels.getOrDefault(World.pack(s.ox+x,s.oy+y,s.oz+z),0L));}
   static void changed(World w,int x,int y,int z,int old,int next){
      if(!handles(old)&&!handles(next))return;State s=w.raster;long k=World.pack(x,y,z);
      if(handles(next))s.nodes.add(k);else s.nodes.remove(k);
      if(handles(next)&&kind(next)==6)s.dirty=true;
      if(handles(old)&&kind(old)==9)RasterAudio.stop(w,k);
      if(!handles(old)||!handles(next)||pick(old)!=pick(next)||face(old)!=face(next)){
         s.dirty=true;if(handles(old)){s.pixels.remove(k);s.statuses.remove(k);s.cpus.remove(k);s.roms.remove(k);s.rams.remove(k);s.keys.remove(k);s.media.slots.remove(k);}
      }
   }
   static void loaded(World w,Chunk c){w.raster.dirty=true;for(int i=0;i<c.blocks.length;i++)if(handles(c.blocks[i]&4095)){w.raster.nodes.add(World.pack(c.cx*16+(i&15),i>>8,c.cz*16+((i>>4)&15)));w.raster.dirty=true;}}
   static void dirty(World w,long key){Chunk c=w.chunkAt(World.ux(key),World.uz(key));if(c!=null)c.sections[World.uy(key)>>4].dirty=true;}
   static void rebuild(World w){
      State s=w.raster;s.nets.clear();Set<Long> unseen=new HashSet<>(s.nodes);
      while(!unseen.isEmpty()){
         long start=unseen.iterator().next();unseen.remove(start);Network n=new Network();ArrayDeque<Long> q=new ArrayDeque<>();q.add(start);
         while(!q.isEmpty()){
            long k=q.remove();int x=World.ux(k),y=World.uy(k),z=World.uz(k),id=w.getBlock(x,y,z);if(!handles(id))continue;n.nodes.add(k);
            switch(kind(id)){case 0->n.screens.add(k);case 1->{n.cpu=k;n.nc++;}case 2->{n.rom=k;n.nr++;}case 3->{n.ram=k;n.nm++;}case 4->n.ng++;case 5->{n.key=k;n.inputs.add(k);}case 6->{n.na++;n.adapter=k;}case 8->n.lamps.add(k);case 9->n.speakers.add(k);case 10->n.cartridges.add(k);}
            for(int[] d:Circuit.DIR){if(y+d[1]<0||y+d[1]>255)continue;long a=World.pack(x+d[0],y+d[1],z+d[2]);if(!w.isLoaded(x+d[0],z+d[2]))n.complete=false;if(unseen.remove(a))q.add(a);}
         }
         if(n.nodes.size()>4096)n.error="Network exceeds 4096 blocks";
         else if(n.nc!=1||n.nr!=1||n.nm!=1||n.ng!=1)n.error="Need exactly one CPU, ROM, RAM and GPU on the connected bus";
         else if(!n.complete)n.error="Load all chunks containing the circuit";
         // Resolution is the largest attached display wall. No panel = default 16x28 canvas for lamps.
         for(List<Long> group:groups(w,n.screens)){int[] b=bounds(w,group);n.width=Math.max(n.width,Math.min(64,(b[1]-b[0]+1)*4));n.height=Math.max(n.height,Math.min(64,(b[3]-b[2]+1)*4));}
         if(n.na>0)for(List<Long> group:groups(w,n.lamps)){int[] b=bounds(w,group);n.width=Math.max(n.width,Math.min(64,(b[1]-b[0]+1)*lampScale(w,n)));n.height=Math.max(n.height,Math.min(64,(b[3]-b[2]+1)*lampScale(w,n)));}
         if(n.width==0){n.width=16;n.height=28;}
         s.nets.add(n);
      }s.dirty=false;
   }
   static List<List<Long>> groups(World w,List<Long> nodes){
      Set<Long> todo=new HashSet<>(nodes);List<List<Long>> groups=new ArrayList<>();
      while(!todo.isEmpty()){
         long start=todo.iterator().next();todo.remove(start);int f=face(w.getBlock(World.ux(start),World.uy(start),World.uz(start)));List<Long> group=new ArrayList<>();ArrayDeque<Long> queue=new ArrayDeque<>();queue.add(start);
         while(!queue.isEmpty()){long p=queue.remove();group.add(p);int x=World.ux(p),y=World.uy(p),z=World.uz(p);
            for(int[] d:new int[][]{{RX[f],0,RZ[f]},{-RX[f],0,-RZ[f]},{0,1,0},{0,-1,0}}){if(y+d[1]<0||y+d[1]>255)continue;long k=World.pack(x+d[0],y+d[1],z+d[2]);if(todo.contains(k)&&face(w.getBlock(World.ux(k),World.uy(k),World.uz(k)))==f){todo.remove(k);queue.add(k);}}
         }groups.add(group);
      }return groups;
   }
   static int[] bounds(World w,List<Long> group){int min=1000000000,max=-min,lo=256,hi=-1,f=face(w.getBlock(World.ux(group.get(0)),World.uy(group.get(0)),World.uz(group.get(0))));for(long k:group){int x=World.ux(k)*RX[f]+World.uz(k)*RZ[f],y=World.uy(k);min=Math.min(min,x);max=Math.max(max,x);lo=Math.min(lo,y);hi=Math.max(hi,y);}return new int[]{min,max,lo,hi,f};}
   static void present(World w,Network n,RasterVM.Cpu cpu){
      for(List<Long> group:groups(w,n.screens)){
         int[] b=bounds(w,group);for(long k:group){int left=(World.ux(k)*RX[b[4]]+World.uz(k)*RZ[b[4]]-b[0])*4,top=(b[3]-World.uy(k))*4;long bits=0;
            for(int y=0;y<4;y++)for(int x=0;x<4;x++){int px=left+x,py=top+y,v=px<cpu.width&&py<cpu.height?cpu.frame[py*64+px]&15:0;bits|=(long)v<<((y*4+x)*4);}
            if(w.raster.pixels.getOrDefault(k,0L)!=bits){w.raster.pixels.put(k,bits);dirty(w,k);Net319.rasterBroadcast(w,0,k,Long.toHexString(bits));}
         }
      }
      if(n.na>0)for(List<Long> group:groups(w,n.lamps)){int[] b=bounds(w,group);for(long k:group){int px=(World.ux(k)*RX[b[4]]+World.uz(k)*RZ[b[4]]-b[0])*lampScale(w,n),py=(b[3]-World.uy(k))*lampScale(w,n),on=px<cpu.width&&py<cpu.height&&(cpu.frame[py*64+px]&15)!=0?1:0;w.setState(World.ux(k),World.uy(k),World.uz(k),PIXEL+on*4+b[4]);}}
      Net319.rasterBroadcast(w,1,n.cpu,status(cpu));
   }
   static String status(RasterVM.Cpu c){StringBuilder s=new StringBuilder();for(int n:c.out)s.append(n).append(',');return s+Base64.getEncoder().encodeToString(c.fault.getBytes(java.nio.charset.StandardCharsets.UTF_8));}
   static void receive(World w,int type,long k,String data){try{if(type==2||type==3){RasterMedia.receive(w,type,k,data);return;}if(type==0){w.raster.pixels.put(k,Long.parseUnsignedLong(data,16));dirty(w,k);}else{String[] p=data.split(",",-1);if(p.length!=9)return;RasterVM.Cpu c=w.raster.cpus.computeIfAbsent(k,a->new RasterVM.Cpu());for(int i=0;i<8;i++)c.out[i]=Integer.parseInt(p[i]);c.fault=new String(Base64.getDecoder().decode(p[8]),java.nio.charset.StandardCharsets.UTF_8);}}catch(RuntimeException ignored){}}
   static void input(World w,long k,int held,int events){if(kindSafe(w,k)!=5)return;int[] v=w.raster.keys.computeIfAbsent(k,a->new int[3]);v[0]=held&15;v[1]=(events&128)!=0?0:v[1]|events&127;v[2]=(int)w.tickCount;}
   static int kindSafe(World w,long k){int id=w.getBlock(World.ux(k),World.uy(k),World.uz(k));return handles(id)?kind(id):-1;}
   static Network network(World w,long key){if(w.raster.dirty)rebuild(w);for(Network n:w.raster.nets)if(n.nodes.contains(key))return n;return null;}
   static void tick(World w){
      if(Net319.isClientConnected()&&!Net319.isHosting())return;State s=w.raster;
      RasterMedia.begin(w);if(s.nodes.isEmpty()){RasterMedia.end(w);return;}if(s.nodes.removeIf(k->!w.isLoaded(World.ux(k),World.uz(k))))s.dirty=true;if(s.dirty)rebuild(w);
      for(Network n:s.nets){if(n.nc!=1)continue;RasterVM.Cpu c=s.cpus.computeIfAbsent(n.cpu,k->new RasterVM.Cpu());int raw=w.getState(World.ux(n.cpu),World.uy(n.cpu),World.uz(n.cpu));
         if((raw&8192)!=0){c.reset();Net319.rasterBroadcast(w,1,n.cpu,status(c));w.setState(World.ux(n.cpu),World.uy(n.cpu),World.uz(n.cpu),raw&~8192);}
         if(!n.error.isEmpty()||(raw&4096)==0)continue;
         int[] rom=s.roms.get(n.rom);if(rom==null||rom.length==0)continue;
         n.mediaError="";if(RasterMedia.requiresMedia(rom)&&RasterMedia.cartridge(w,n)==null){n.mediaError="Connect one loaded Media Cartridge";continue;}
         int[] toneBudget={0};int[] ram=s.rams.computeIfAbsent(n.ram,k->new int[4096]);c.width=n.width;c.height=n.height;
         RasterVM.step(c,rom,ram,new RasterVM.IO(){public int input(int port){if(port>=32&&port<=35)return RasterMedia.info(w,n,port);int held=0,events=0;if(port==0||port==1)for(long input:n.inputs){int[] keys=s.keys.computeIfAbsent(input,k->new int[3]);if((int)(w.tickCount-keys[2])<=40)held|=keys[0];events|=keys[1];if(port==0)keys[1]=0;}if(port>=16&&port<22){int[] d=Circuit.DIR[port-16];int x=World.ux(n.cpu),y=World.uy(n.cpu),z=World.uz(n.cpu);return RedstoneLogic.direct(w,x+d[0],y+d[1],z+d[2],x,y,z,true,false);}
            return switch(port){case 0->events;case 1->held;case 2->c.width;case 3->c.height;default->0;};}public void tone(int note,int ms,int voice){if(toneBudget[0]++<8)RasterMedia.tone(w,n,note,ms,voice);}public void video(RasterVM.Cpu cpu,int frame,int flags){RasterMedia.video(w,n,cpu,frame,flags);}
            public void present(RasterVM.Cpu cpu){RasterComputer.present(w,n,cpu);}public void output(int port,int value){if(port==6)w.notifyAround(World.ux(n.cpu),World.uy(n.cpu),World.uz(n.cpu));}});
         String status=status(c);if(!status.equals(s.statuses.get(n.cpu)))Net319.rasterBroadcast(w,1,n.cpu,status);
      }RasterMedia.end(w);
   }
   static int lampPower(int id,int dx,int dy,int dz){if(id<PIXEL+4||id>=PIXEL+8)return 0;int f=face(id);return dy==0&&dx==Circuit.DX[f]&&dz==Circuit.DZ[f]?15:0;}
   static boolean captured(Game g){return g.rasterFocus!=null&&g.world==g.rasterFocus.w&&kindSafe(g.world,g.rasterFocus.key)==5&&g.screen==Game.S.PLAY;}
   static void release(Game g){if(g.rasterFocus!=null){Net319.rasterKeys(g.rasterFocus.w,g.rasterFocus.key,0,128);g.rasterFocus=null;}}
   static boolean onKey(Game g,int key,int action){
      if(!captured(g))return false;if(key==256){release(g);return true;}if(key==291)return false;
      if(key==77&&action==1){Network n=network(g.world,g.rasterFocus.key);if(n!=null){boolean mute=n.speakers.stream().anyMatch(k->RasterMedia.volume(g.world,k)>0);for(long k:n.speakers){int x=World.ux(k),y=World.uy(k),z=World.uz(k),raw=g.world.getState(x,y,z);g.world.setState(x,y,z,mute?raw|4096:raw&~4096);}g.say(mute?"Raster speakers muted":"Raster speakers unmuted");}return true;}
      int bit=switch(key){case 263,65->1;case 262,68->2;case 265,87->4;case 264,83->8;case 32->16;case 80->32;case 82,257->64;default->0;};
      Focus f=g.rasterFocus;if((bit&11)!=0){if(action==0)f.held&=~bit;else f.held|=bit;}
      int events=action==1||action==2&&(bit==1||bit==2)?bit:0;Net319.rasterKeys(g.world,f.key,f.held,events);return true;
   }
   static boolean use(Game g,int x,int y,int z,int id){
      if(!handles(id))return false;World w=g.world;long key=World.pack(x,y,z);int k=kind(id);
      if(k==1){int raw=w.getState(x,y,z);w.setState(x,y,z,g.player.sneaking?raw|8192:raw^4096);g.say(g.player.sneaking?"Processor reset requested":"Processor Run/Stop toggled");}
      else if(k==0||k==5){Network n=network(w,key);if(n==null||n.nc!=1||n.inputs.isEmpty()){g.say("Connect this screen to one processor and a Game Keypad using Raster Data Cable.");return true;}release(g);g.rasterFocus=new Focus(w,k==5?key:n.key);g.leftDown=g.rightDown=g.leftPressed=g.rightPressed=false;
         List<Long> view=k==0?n.screens:n.lamps;if(!view.isEmpty()){double ax=0,ay=0,az=0;for(long pos:view){ax+=World.ux(pos)+.5;ay+=World.uy(pos)+.5;az+=World.uz(pos)+.5;}double dx=ax/view.size()-g.player.x,dy=ay/view.size()-g.player.y-g.player.eye,dz=az/view.size()-g.player.z;g.player.yaw=(float)Math.toDegrees(Math.atan2(-dx,dz));g.player.pitch=(float)-Math.toDegrees(Math.atan2(dy,Math.hypot(dx,dz)));}
         g.say(w.raster.cpus.get(n.cpu)!=null&&w.raster.cpus.get(n.cpu).out[7]==0x4d50?"MEDIA: Enter restart; Space/P play/pause; Left/Right seek; M mute; Esc leave.":"ARROWS/A-D move; UP rotate; DOWN soft drop; SPACE hard drop; P pause; ENTER/R restart; ESC leave controls.");}
      else if(k==9){int raw=w.getState(x,y,z);w.setState(x,y,z,g.player.sneaking?raw^8192:raw^4096);g.say("Speaker: "+Math.round(RasterMedia.volume(w,key)*100)+"% (sneak-click quiet/normal)");}
      else if(k==10){g.say("Cartridge: "+w.raster.media.slots.getOrDefault(key,"empty")+". /mediaload shadow-signal or /mediaload file.bcv");}
      else if(k==6){int raw=w.getState(x,y,z),scale=(((raw>>>12)&3)+1)&3;w.setState(x,y,z,id|(scale<<12));g.say("Lamp sampling: 1 lamp per "+(1<<scale)+" screen pixels");}
      else if(k==2){int[] code=w.raster.roms.get(key);g.say("ROM: "+(code==null?0:code.length/4)+" instructions. Host: look at ROM and /rasterload FallingBlocks.asm");}
      else g.say(NAMES[k]+". Connect modules with Raster Data Cable. Display panels: 4x4 pixels each, up to 64x64 per wall.");return true;
   }
   static void hud(Game g){if(!captured(g)){if(g.target!=null&&g.world!=null){int id=g.world.getBlock(g.target.x,g.target.y,g.target.z);if(handles(id))g.gui.centered(NAMES[kind(id)]+" - right-click to use",g.gui.w/2,g.gui.h-65,0xFFBDEADA,true,.8F);}return;}Network n=network(g.world,g.rasterFocus.key);if(n==null)return;RasterVM.Cpu c=g.world.raster.cpus.get(n.cpu);if(c==null)return;
      String state=!n.error.isEmpty()?n.error:!n.mediaError.isEmpty()?n.mediaError:!c.fault.isEmpty()?c.fault:c.out[4]!=0?"GAME OVER - Enter to restart":c.out[3]!=0?"PAUSED - Enter starts / P resumes":"PLAYING";
      if((g.world.getState(World.ux(n.cpu),World.uy(n.cpu),World.uz(n.cpu))&4096)==0)state="PROCESSOR STOPPED";
      if(n.cartridges.size()>0&&RasterMedia.cartridge(g.world,n)==null)state="MISSING MEDIA - load cartridge";
      String top=c.out[7]==0x4d50?"MEDIA | "+(c.out[0]/20)+" / "+(c.out[1]/20)+" seconds | "+(!n.error.isEmpty()?n.error:!n.mediaError.isEmpty()?n.mediaError:!c.fault.isEmpty()?c.fault:(g.world.getState(World.ux(n.cpu),World.uy(n.cpu),World.uz(n.cpu))&4096)==0?"STOPPED":c.out[3]!=0?"PAUSED":"PLAYING")+" | M: mute speakers":c.out[7]==0x4244?"BLOCK DROP | Score "+c.out[0]+" | Lines "+c.out[1]+" | Level "+c.out[2]+" | "+state:"RASTER | OUT "+Arrays.toString(c.out)+" | "+(!n.error.isEmpty()?n.error:!n.mediaError.isEmpty()?n.mediaError:c.fault);
      g.gui.centered(top,g.gui.w/2,12,0xFFFFFFFF,true,.8F);
      g.gui.centered(c.out[7]==0x4d50?"Space/P: play/pause | Left/Right: seek 5s | Enter: restart | M: mute | Esc: leave":"Arrows: move/rotate | Space: drop | P: pause | Enter: restart | M: mute | Esc: leave",g.gui.w/2,g.gui.h-60,0xFFBDEADA,true,.7F);
   }
   static void exportExamples(Game g)throws IOException{Path p=g.root.toPath().resolve("computer-programs");Files.createDirectories(p);Path f=p.resolve("FallingBlocks.asm");if(!Files.exists(f))Files.writeString(f,RasterPrograms.fallingSource());Path snd=p.resolve("FallingBlocks-Sound.asm");if(!Files.exists(snd))Files.writeString(snd,RasterPrograms.fallingSource());Path media=p.resolve("MediaPlayer.asm");if(!Files.exists(media))Files.writeString(media,ShadowMachine.source());Path t=p.resolve("Pattern.asm");if(!Files.exists(t))Files.writeString(t,"LI 0 1\nCLEAR 0\nPRESENT\nWAIT 20\nLI 0 2\nCLEAR 0\nPRESENT\nWAIT 20\nJMP 0\n");}
   static void loadProgram(Game g,String name)throws IOException{
      if(Net319.isClientConnected()&&!Net319.isHosting()){g.say("Only the host imports ROM programs.");return;}if(g.target==null||kindSafe(g.world,World.pack(g.target.x,g.target.y,g.target.z))!=2){g.say("Look at a Program ROM block first.");return;}
      if(!name.matches("[A-Za-z0-9_-]+\\.asm"))throw new IOException("Use a simple .asm filename");exportExamples(g);Path base=g.root.toPath().resolve("computer-programs").toRealPath(),file=base.resolve(name).toRealPath();if(!file.startsWith(base)||Files.size(file)>262144)throw new IOException("Invalid program file");int[] code=RasterVM.assemble(Files.readString(file));long k=World.pack(g.target.x,g.target.y,g.target.z);g.world.raster.roms.put(k,code);Network n=network(g.world,k);if(n!=null&&n.nc==1){RasterVM.Cpu cpu=g.world.raster.cpus.computeIfAbsent(n.cpu,p->new RasterVM.Cpu());cpu.reset();g.world.notifyAround(World.ux(n.cpu),World.uy(n.cpu),World.uz(n.cpu));Net319.rasterBroadcast(g.world,1,n.cpu,status(cpu));}g.say("Loaded "+code.length/4+" instructions from "+name);
   }
   static void save(World w){if(w.dir==null||!w.saveEnabled)return;Path file=w.dir.toPath().resolve("raster.dat"),tmp=file.resolveSibling("raster.dat.tmp");try(DataOutputStream out=new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(tmp)))){
      State s=w.raster;out.writeInt(0x52415331);writeArrays(out,s.roms);writeArrays(out,s.rams);out.writeInt(s.pixels.size());for(var e:s.pixels.entrySet()){out.writeLong(e.getKey());out.writeLong(e.getValue());}
      out.writeInt(s.cpus.size());for(var e:s.cpus.entrySet()){out.writeLong(e.getKey());RasterVM.Cpu c=e.getValue();out.writeInt(c.pc);out.writeInt(c.wait);out.writeInt(c.sp);out.writeInt(c.rng);out.writeUTF(c.fault);for(int v:c.r)out.writeInt(v);for(int v:c.stack)out.writeInt(v);for(int v:c.out)out.writeInt(v);out.write(c.frame);}
   }catch(IOException e){System.err.println("Cannot save raster computers: "+e);return;}try{Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(IOException e){try{Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING);}catch(IOException ex){System.err.println(ex);}}}
   static void writeArrays(DataOutputStream o,Map<Long,int[]> map)throws IOException{o.writeInt(map.size());for(var e:map.entrySet()){o.writeLong(e.getKey());o.writeInt(e.getValue().length);for(int v:e.getValue())o.writeInt(v);}}
   static int count(DataInputStream in,int max)throws IOException{int n=in.readInt();if(n<0||n>max)throw new IOException("Invalid raster data count");return n;}
   static void readArrays(DataInputStream in,Map<Long,int[]> map,int max,boolean ram)throws IOException{int n=count(in,4096);for(int i=0;i<n;i++){long k=in.readLong();int len=count(in,max);if(ram&&len!=4096||!ram&&len%4!=0)throw new IOException("Invalid memory length");int[] a=new int[len];for(int j=0;j<len;j++)a[j]=in.readInt();map.put(k,a);}}
   static void load(World w){if(w.dir==null)return;Path file=w.dir.toPath().resolve("raster.dat");if(!Files.exists(file))return;try{if(Files.size(file)>64*1024*1024)throw new IOException("Raster file too large");State s=new State();try(DataInputStream in=new DataInputStream(new BufferedInputStream(Files.newInputStream(file)))){
      if(in.readInt()!=0x52415331)throw new IOException("Raster file version");readArrays(in,s.roms,16384,false);readArrays(in,s.rams,4096,true);int n=count(in,65536);for(int i=0;i<n;i++)s.pixels.put(in.readLong(),in.readLong());n=count(in,4096);for(int i=0;i<n;i++){long k=in.readLong();RasterVM.Cpu c=new RasterVM.Cpu();c.pc=in.readInt();c.wait=in.readInt();c.sp=in.readInt();c.rng=in.readInt();c.fault=in.readUTF();if(c.sp<0||c.sp>32||c.wait<0||c.wait>1200)throw new IOException("CPU state");for(int j=0;j<32;j++)c.r[j]=in.readInt();for(int j=0;j<32;j++)c.stack[j]=in.readInt();for(int j=0;j<8;j++)c.out[j]=in.readInt();in.readFully(c.frame);s.cpus.put(k,c);}}
      w.raster.roms.putAll(s.roms);w.raster.rams.putAll(s.rams);w.raster.pixels.putAll(s.pixels);w.raster.cpus.putAll(s.cpus);for(var e:s.cpus.entrySet())w.raster.statuses.put(e.getKey(),status(e.getValue()));
   }catch(IOException|RuntimeException e){System.err.println("Raster data not loaded: "+e);}}
}
