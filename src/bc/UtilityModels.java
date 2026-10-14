package bc;

import java.util.ArrayList;

/** Dedicated utility components. Existing block IDs and collision contracts stay
 * intact; the four new vertical-observer states avoid the Backrooms ID range. */
final class UtilityModels {
   static final int CAP=760,SHAFT=761,RED_HEAD=762,DEAD_HEAD=763,CHEST_SIDE=764,
      FURNACE_SIDE=765,FURNACE_TOP=766,DAY_ON=767,EYE_TOP=768,EYE_SIDE=769,
      METAL=770,BUTTON_STONE=771,BUTTON_WOOD=772,EGG=773,FLAME=774,
      OBS_BACK=775,OBS_BACK_ON=776,REPEATER_EDGE=777,ROD=778,CLOTH_EDGE=779,LEVER_BASE=780;
   private UtilityModels(){}
   static double[] part(double x,double y,double z,double xx,double yy,double zz,int tile){return Shapes.tiles(Shapes.px(x,y,z,xx,yy,zz),tile,tile,tile);}
   static double[] local(double[] p){return Shapes.local(p);}
   static double[] hinged(double[] p,double angle,double y,double z){double[] q=java.util.Arrays.copyOf(p,15);q[12]=angle;q[13]=y;q[14]=z;return q;}
   static boolean componentIcon(int id) {
      return id==Blocks.FURNACE||id==Blocks.FURNACE_LIT||id==Blocks.CHEST||id==Blocks.CHEST_LOOT||id==Blocks.DRAGON_EGG||id==Blocks.END_FRAME_EYE
         ||id==Redstone319.REDSTONE_TORCH||id==Redstone319.REDSTONE_TORCH_OFF
         ||id==Redstone319.LEVER_OFF||id==Redstone319.LEVER_ON||Redstone319.isRepeater(id)
         ||id>=Redstone319.STONE_BUTTON&&id<=Redstone319.WOOD_BUTTON_ON;
   }
   static void initPixels() {
      for(int tile=760;tile<=780;tile++) {
         int[] nativePixels=TexUtility32.paint(tile),low=new int[256];
         for(int y=0;y<16;y++)for(int x=0;x<16;x++)low[y*16+x]=nativePixels[y*64+x*2];
         Tex.blit(tile,low);
      }
   }
   static void register() {
      if(Boolean.getBoolean("bc.legacymesh"))return; // retain the strict 3.5 mesh fixture
      // Upright torch has a real cap, separate wood shaft and luminous head.
      Shapes.model(Blocks.TORCH,new double[][]{part(7,0,7,9,8,9,SHAFT),Shapes.tiles(Shapes.px(6.75,8,6.75,9.25,10,9.25),FLAME,CAP,SHAFT)});
      for(int id=Blocks.WALL_TORCH;id<Blocks.WALL_TORCH+4;id++)Blocks.tiles[id][0]=CAP;
      for(int id:new int[]{Blocks.CHEST,Blocks.CHEST_LOOT}){
         java.util.Arrays.fill(Blocks.tiles[id],CHEST_SIDE);Blocks.tiles[id][0]=41;Blocks.tiles[id][2]=42;
         Shapes.model(id,chest(0));
      }
      for(int id:new int[]{Blocks.FURNACE,Blocks.FURNACE_LIT}){
         java.util.Arrays.fill(Blocks.tiles[id],FURNACE_SIDE);Blocks.tiles[id][0]=FURNACE_TOP;Blocks.tiles[id][2]=id==Blocks.FURNACE?39:40;
      }
      // Transparent cage holes and inset egg geometry cannot occlude an entire
      // neighbouring cube face, or the floor beneath them becomes a sky hole.
      Blocks.opaque[Blocks.SPAWNER]=false;Blocks.opacity[Blocks.SPAWNER]=0;Blocks.cutout[Blocks.SPAWNER]=true;
      Blocks.opaque[Blocks.DRAGON_EGG]=false;Blocks.opacity[Blocks.DRAGON_EGG]=0;
      // Stepped ovoid shell, not an obsidian cube.
      java.util.Arrays.fill(Blocks.tiles[Blocks.DRAGON_EGG],EGG);
      Shapes.model(Blocks.DRAGON_EGG,new double[][]{
         part(5,0,5,11,2,11,EGG),part(3,2,3,13,5,13,EGG),part(2,5,2,14,9,14,EGG),
         part(3,9,3,13,12,13,EGG),part(4,12,4,12,14,12,EGG),part(6,14,6,10,16,10,EGG)});
      Blocks.box[Blocks.DRAGON_EGG]=new double[]{.125,0,.125,.875,1,.875};Blocks.coll[Blocks.DRAGON_EGG]=Blocks.box[Blocks.DRAGON_EGG];
      Shapes.model(Blocks.END_FRAME_EYE,new double[][]{
         Shapes.px(0,0,0,16,13,16),part(4,13,4,12,14,12,EYE_SIDE),
         Shapes.tiles(Shapes.px(5,14,5,11,16,11),EYE_SIDE,EYE_TOP,EYE_SIDE)});
      Blocks.tiles[Blocks.END_FRAME_EYE][0]=265; // socket stays beneath the raised eye
      Blocks.box[Blocks.END_FRAME_EYE]=new double[]{0,0,0,1,1,1};Blocks.coll[Blocks.END_FRAME_EYE]=Blocks.box[Blocks.END_FRAME_EYE];
      redstone();
   }
   static double[][] chest(double angle) {
      // Physical latch only: inheriting the old front texture on every hollow
      // wall stamped additional locks onto the inside of the opened chest.
      double[] lid=hinged(Shapes.tiles(Shapes.px(1,9,1,15,14,15),CHEST_SIDE,41,CHEST_SIDE),angle,9/16.,15/16.);
      return new double[][]{part(1,0,1,15,1,15,CHEST_SIDE),part(1,1,1,15,9,2,CHEST_SIDE),part(1,1,14,15,9,15,CHEST_SIDE),
         part(1,1,2,2,9,14,CHEST_SIDE),part(14,1,2,15,9,14,CHEST_SIDE),lid,
         hinged(part(7,8,0.5,9,11,1,METAL),angle,9/16.,15/16.)};
   }
   static double[][] torch(boolean lit){return new double[][]{part(7,0,7,9,7,9,SHAFT),local(part(6.75,7,6.75,9.25,10,9.25,lit?RED_HEAD:DEAD_HEAD))};}
   private static void redstone() {
      Shapes.model(Redstone319.REDSTONE_TORCH,torch(true));Shapes.model(Redstone319.REDSTONE_TORCH_OFF,torch(false));
      for(int on=0;on<2;on++) {
         int id=on==0?Redstone319.LEVER_OFF:Redstone319.LEVER_ON;
         double[] handle=hinged(part(7,3,7,9,10,9,SHAFT),(on==0?-.6:.6),3/16.,8/16.);
         Shapes.model(id,new double[][]{part(4,0,3,12,3,13,LEVER_BASE),part(6,2.5,6,10,4,10,METAL),handle});
         for(int f=0;f<4;f++) {
            int rep=(on==0?Redstone319.REPEATER_OFF:Redstone319.REPEATER_ON)+f;
            Shapes.model(rep,Shapes.turnAll(new double[][]{
               Shapes.tiles(Shapes.px(0,0,0,16,2,16),REPEATER_EDGE,on==0?663:664,REPEATER_EDGE),
               part(7,2,2,9,5,4,SHAFT),local(part(7,5,2,9,7,4,on==0?DEAD_HEAD:RED_HEAD)),
               part(7,2,10,9,5,12,SHAFT),local(part(7,5,10,9,7,12,on==0?DEAD_HEAD:RED_HEAD))},Shapes.turns(0,f)));
         }
      }
      for(int id:new int[]{Redstone319.STONE_BUTTON,Redstone319.STONE_BUTTON_ON,Redstone319.WOOD_BUTTON,Redstone319.WOOD_BUTTON_ON}) {
         int tile=id<Redstone319.WOOD_BUTTON?BUTTON_STONE:BUTTON_WOOD;
         double[][] boxes=Shapes.modelBoxes(id);
         for(double[] b:boxes){b[8]=b[9]=b[10]=tile;b[11]=0;}
         java.util.Arrays.fill(Blocks.tiles[id],tile);
      }
      double[][] day=Shapes.modelBoxes(Redstone319.DAYLIGHT_SENSOR_ON);
      double[][] copy=new double[day.length][];for(int i=0;i<copy.length;i++)copy[i]=day[i].clone();
      copy[1][9]=DAY_ON;Shapes.model(Redstone319.DAYLIGHT_SENSOR_ON,copy);
      Blocks.tiles[Redstone319.DAYLIGHT_SENSOR_ON][0]=DAY_ON;
      Blocks.emit[Redstone319.DAYLIGHT_SENSOR_ON]=0; // a powered detector is not a lamp
      for(int f=0;f<6;f++)for(int on=0;on<2;on++) {
         int id=Redstone319.observerId(f,on==1);
         if(f>=4){Blocks.def(id,"Observer",3,true,665);Blocks.mineClass[id]=1;Blocks.needTool[id]=true;Blocks.drop[id]=Redstone319.OBSERVER;}
         int front=f<4?2+f:f==4?1:0;
         java.util.Arrays.fill(Blocks.tiles[id],FURNACE_SIDE);Blocks.tiles[id][front]=671;Blocks.tiles[id][front^1]=on==1?OBS_BACK_ON:OBS_BACK;
         Blocks.emit[id]=0;
      }
   }
   static double[][] banner(int color,boolean wall,int facing) {
      ArrayList<double[]> parts=new ArrayList<>();int tint=Color312.fleece(color);
      if(wall)parts.add(part(0,13,13,16,14,15,ROD));
      else {parts.add(part(7.5,0,7,8.5,28,8,ROD));parts.add(part(0,28,6,16,29,7,ROD));}
      double y=wall?-11:4,z=wall?12:6;
      for(int i=0;i<4;i++) {
         // Adjacent folds overlap in depth: thin cloth must not have light-leaking slits.
         double fold=new double[]{0,.15,.05,.20}[i];
         double[] p=local(Shapes.tint(Shapes.tiles(Shapes.px(.5+i*3.75,y,z+fold,4.25+i*3.75,y+24,z+fold+.25),641,CLOTH_EDGE,CLOTH_EDGE),tint));
         p=java.util.Arrays.copyOf(p,19);p[15]=i/4.;p[16]=0;p[17]=(i+1)/4.;p[18]=1;parts.add(p);
      }
      return Shapes.turnAll(parts.toArray(new double[0][]),Shapes.turns(0,facing));
   }
}
