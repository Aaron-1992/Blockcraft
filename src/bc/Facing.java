package bc;

/** Placement-facing properties for furniture whose IDs also identify inventories/furnace state.
 * Bits 13..14 leave the chest's waterlogged bit (12) untouched. Zero is the historical north face. */
final class Facing {
   static final int MASK=0x6000;
   private static final int[] CW={0,1,4,5,3,2};
   private Facing(){}
   static int family(int id){return id==Blocks.CHEST||id==Blocks.CHEST_LOOT?1:id==Blocks.FURNACE||id==Blocks.FURNACE_LIT?2:id==Blocks.CRAFTING_TABLE?3:0;}
   static boolean handles(int id){return family(id)!=0;}
   static boolean same(int a,int b){return family(a)!=0&&family(a)==family(b);}
   static int direction(int state){return (state>>>13)&3;}
   static int placement(int id,Player player,int old){
      if(!handles(id))return id;
      int wet=BlockState.waterloggable(id)&&(BlockState.wet(old)||(old&4095)==Blocks.WATER)?BlockState.WATERLOGGED:0;
      return id|((Shapes.playerFacing(player)^1)<<13)|wet;
   }
   static int worldFace(int face,int turns){for(int i=0;i<(turns&3);i++)face=CW[face];return face;}
   static int tile(int state,int face){
      int id=state&4095;
      if(!handles(id))return Blocks.tiles[id][face];
      int turns=Shapes.turns(0,direction(state));
      return Blocks.tiles[id][worldFace(face,4-turns)];
   }
   /** Screenshot fixture uses actual mouse placement, tile entities and lit/unlit transitions. */
   static void smoke(Game g){
      for(int x=0;x<=18;x++)for(int z=0;z<=14;z++){
         g.world.setBlock(x,66,z,Blocks.STONE);
         for(int y=67;y<=79;y++)g.world.setState(x,y,z,0);
      }
      float yaw=g.player.yaw;Stack held=g.inv[g.sel];World.Hit target=g.target;
      int[] types={Blocks.CHEST,Blocks.FURNACE,Blocks.CRAFTING_TABLE};
      for(int row=0;row<3;row++)for(int i=0;i<4;i++){
         int x=2+i*4,z=3+row*4;g.player.yaw=i*90;g.inv[g.sel]=new Stack(types[row],1);
         g.target=new World.Hit();g.target.x=x;g.target.y=66;g.target.z=z;g.target.ny=1;g.placeBlock();
         if(row==1){TileEntity tile=g.world.getOrCreateTile(x,67,z);tile.burn=tile.burnMax=2400;g.world.setBlock(x,67,z,Blocks.FURNACE_LIT);}
      }
      g.player.yaw=yaw;g.inv[g.sel]=held;g.target=target;
   }
   static double[][] chest(int state,double angle){
      double[][] boxes=UtilityModels.chest(angle);
      int turns=Shapes.turns(0,direction(state));
      if(turns==0)return boxes;
      for(int i=0;i<boxes.length;i++){
         int length=boxes[i].length;
         boxes[i]=java.util.Arrays.copyOf(boxes[i],20);
         if(length<=18){boxes[i][17]=1;boxes[i][18]=1;}
         // Rotate AFTER hinging, around the block centre, so the lid/latch share the correct hinge axis.
         boxes[i][19]=turns;
      }
      return boxes;
   }
}
