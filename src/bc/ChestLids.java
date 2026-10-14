package bc;

import java.util.concurrent.ConcurrentHashMap;

/** Client-side lid interpolation. No block IDs, inventories or save data change.
 * Immutable entries are published to mesher workers; cleared on world changes. */
final class ChestLids {
   private static final ConcurrentHashMap<String,Float> angles=new ConcurrentHashMap<>();
   private static World world;
   private static double accumulator;
   private ChestLids(){}
   private static String key(int x,int y,int z){return x+","+y+","+z;}
   static float angle(int x,int y,int z){return angles.getOrDefault(key(x,y,z),0F);}
   static float next(float angle,boolean open,float dt){float goal=open?1.45F:0;float step=Math.min(.1F,Math.max(0,dt))*4;return angle<goal?Math.min(goal,angle+step):Math.max(goal,angle-step);}
   static void update(Game g,double dt) {
      if(world!=g.world){angles.clear();world=g.world;accumulator=0;}
      if(world==null)return;
      accumulator+=dt;if(accumulator<.05)return;float elapsed=(float)Math.min(.1,accumulator);accumulator=0;
      TileEntity tile=g.menu!=null&&g.menu.kind==3?g.menu.tile:null;
      String active=tile==null?null:key(tile.x,tile.y,tile.z);
      if(active!=null)angles.putIfAbsent(active,0F);
      for(String k:angles.keySet()) {
         float old=angles.getOrDefault(k,0F),value=next(old,k.equals(active),elapsed);
         String[] xyz=k.split(",");int x=Integer.parseInt(xyz[0]),y=Integer.parseInt(xyz[1]),z=Integer.parseInt(xyz[2]);
         int id=world.getBlock(x,y,z);
         if(id!=Blocks.CHEST&&id!=Blocks.CHEST_LOOT)value=0;
         if(value==0)angles.remove(k);else angles.put(k,value);
         if(value!=old&&g.rend!=null)g.meshNow(x,y,z);
      }
   }
}
