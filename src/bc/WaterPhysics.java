package bc;

/** Source/flowing/falling water, nearest-drop routing, waterlogged sources and bubble columns. */
final class WaterPhysics {
   static final int BUBBLE_UP=0x4000, BUBBLE_DOWN=0x8000;
   private static final int[] DX={0,0,1,-1}, DZ={-1,1,0,0};
   private WaterPhysics() {}
   static boolean containsState(int state) { return Blocks.isWater(state&BlockState.ID_MASK)||BlockState.wet(state); }
   static boolean contains(World w,int x,int y,int z) { return containsState(w.getState(x,y,z)); }
   static boolean source(int state) { return BlockState.wet(state)||(state&BlockState.ID_MASK)==Blocks.WATER; }
   static int level(int state) { return source(state)||(state&BlockState.FALLING)!=0?0:Blocks.fluidLevel(state&BlockState.ID_MASK); }
   static double height(World w,int x,int y,int z) {
      int state=w.getState(x,y,z);
      if(!containsState(state))return 0;
      return (state&BlockState.FALLING)!=0||contains(w,x,y+1,z)?1:(8-level(state))/9.0;
   }
   static boolean submerged(World w,double x,double y,double z) {
      int bx=(int)Math.floor(x),by=(int)Math.floor(y),bz=(int)Math.floor(z);
      return contains(w,bx,by,bz)&&y<by+height(w,bx,by,bz);
   }
   static boolean passable(World w,int x,int y,int z) {
      if(y<0||y>255||!w.isLoaded(x,z))return false;
      int id=w.getBlock(x,y,z);
      return id==0||Blocks.isWater(id)||Blocks.isLava(id)||Blocks.replaceable[id]||Redstone319.isFloorPart(id)||Blocks.kind[id]==1||Blocks.kind[id]==Blocks.K_WTORCH;
   }
   static boolean drain(World w,int x,int y,int z) {
      int state=w.getState(x,y,z),id=state&BlockState.ID_MASK;
      if(!source(state))return false;
      if(Aquatic.plant(id)) { w.setState(x,y,z,0); w.dropItem(x+.5,y+.3,z+.5,Blocks.drop[id],1); }
      else w.setState(x,y,z,BlockState.wet(state)?state&~BlockState.WATERLOGGED:0);
      return true;
   }
   static boolean fill(World w,int x,int y,int z) {
      int id=w.getBlock(x,y,z);
      if(w.dim==Dim.NETHER)return true; // bucket empties, water evaporates
      if(BlockState.waterloggable(id))return w.setState(x,y,z,w.getState(x,y,z)|BlockState.WATERLOGGED);
      if(id==0||Blocks.replaceable[id]||Blocks.isWater(id))return w.setState(x,y,z,Blocks.WATER);
      return false;
   }
   private static boolean flow(World w,int x,int y,int z,int value,boolean falling) {
      if(!passable(w,x,y,z))return false;
      int old=w.getState(x,y,z),id=old&BlockState.ID_MASK;
      if(source(old))return false;
      if(Blocks.isLava(id))return w.setState(x,y,z,Blocks.fluidLevel(id)==0?Blocks.OBSIDIAN:w.cobbleOrSkyblockOre());
      if(Blocks.isWater(id)&&!falling&&(old&BlockState.FALLING)!=0)return false;
      if(Blocks.isWater(id)&&((old&BlockState.FALLING)!=0)==falling&&Blocks.fluidLevel(id)<=value)return false;
      if(id!=0&&!Blocks.isFluid(id)&&Blocks.drop[id]>0)w.dropItem(x+.5,y+.3,z+.5,Blocks.drop[id],1);
      return w.setState(x,y,z,Blocks.WATER+value+(falling?BlockState.FALLING:0));
   }
   /** Up to four horizontal steps: choose routes that reach a downward opening first. */
   private static int slope(World w,int x,int y,int z,int depth,int cameFrom) {
      if(passable(w,x,y-1,z)&&!source(w.getState(x,y-1,z)))return depth;
      if(depth>=4)return 1000;
      int best=1000;
      for(int d=0;d<4;d++)if(d!=cameFrom) {
         int nx=x+DX[d],nz=z+DZ[d];
         if(passable(w,nx,y,nz)&&!source(w.getState(nx,y,nz)))best=Math.min(best,slope(w,nx,y,nz,depth+1,d^1));
      }
      return best;
   }
   static void update(World w,int x,int y,int z) {
      int state=w.getState(x,y,z),id=state&BlockState.ID_MASK;
      if(!containsState(state))return;
      boolean src=source(state),fall=(state&BlockState.FALLING)!=0;
      if(!src) {
         int above=w.getState(x,y+1,z),best=8,sources=0;
         if(containsState(above)) { best=1;fall=true; }
         else {
            fall=false;
            for(int d=0;d<4;d++) {
               int nb=w.getState(x+DX[d],y,z+DZ[d]);
               if(containsState(nb)){if(source(nb))sources++;best=Math.min(best,level(nb)+1);}
            }
            int below=w.getState(x,y-1,z);
            if(sources>=2&&(Blocks.solid[below&BlockState.ID_MASK]||source(below))) { best=0;src=true; }
         }
         if(best>7) { w.setState(x,y,z,0);return; }
         int desired=Blocks.WATER+best+(fall?BlockState.FALLING:0);
         if(desired!=state){w.setState(x,y,z,desired);state=desired;id=desired&BlockState.ID_MASK;}
      }
      if(id==Blocks.WATER) {
         int below=w.getState(x,y-1,z),bid=below&BlockState.ID_MASK;
         int bubble=bid==Blocks.SOUL_SAND?BUBBLE_UP:bid==Blocks.MAGMA?BUBBLE_DOWN:
            (bid==Blocks.WATER?below&(BUBBLE_UP|BUBBLE_DOWN):0);
         int want=Blocks.WATER|bubble;
         if(want!=state){w.setState(x,y,z,want);state=want;}
      }
      int below=w.getState(x,y-1,z);
      boolean bottomClosed=Blocks.isSlab(id)&&!Blocks.isTopSlab(id)
         || Shapes.isTrap(id)&&!Shapes.trapOpen(id)&&(id-Shapes.TRAP0)%16<8;
      boolean down=!bottomClosed&&passable(w,x,y-1,z)&&!source(below);
      if(down){flow(w,x,y-1,z,1,true);if(!src)return;}
      int next=src||fall?1:level(state)+1;
      if(next>7)return;
      int[] costs=new int[4];int best=1000;
      for(int d=0;d<4;d++) {
         int nx=x+DX[d],nz=z+DZ[d];
         costs[d]=passable(w,nx,y,nz)&&!source(w.getState(nx,y,nz))?slope(w,nx,y,nz,0,d^1):2000;
         best=Math.min(best,costs[d]);
      }
      for(int d=0;d<4;d++)if(costs[d]==best)flow(w,x+DX[d],y,z+DZ[d],next,false);
   }
   /** Unit current pointing down the fluid-level gradient. Falling columns also pull down. */
   static double[] current(World w,int x,int y,int z) {
      int state=w.getState(x,y,z); if(!containsState(state))return new double[3];
      double vx=0,vz=0;int here=level(state);
      for(int d=0;d<4;d++) {
         int next=w.getState(x+DX[d],y,z+DZ[d]);int delta=0;
         if(containsState(next))delta=level(next)-here;
         else if(passable(w,x+DX[d],y,z+DZ[d])&&contains(w,x+DX[d],y-1,z+DZ[d]))delta=8-here;
         vx+=DX[d]*delta;vz+=DZ[d]*delta;
      }
      double vy=(state&BlockState.FALLING)!=0?-6:0, n=Math.sqrt(vx*vx+vy*vy+vz*vz);
      return n==0?new double[3]:new double[]{vx/n,vy/n,vz/n};
   }
}
