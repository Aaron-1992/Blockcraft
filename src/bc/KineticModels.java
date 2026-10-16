package bc;

import java.util.*;

/** Original wood/iron/andesite machinery. No upstream Create artwork is redistributed. */
final class KineticModels {
   static double[] box(double x,double y,double z,double xx,double yy,double zz,int tile){return Shapes.local(Shapes.tiles(Shapes.px(x,y,z,xx,yy,zz),tile,tile,tile));}
   static double[] rotate(double[] box,int axis,double angle){double[] p=Arrays.copyOf(box,23);p[17]=p[18]=1;p[20]=0;p[21]=angle;p[22]=axis;return p;}
   static double[][] model(int id,double phase,double progress){
      int k=Kinetics.kind(id),a=Kinetics.axis(id);List<double[]> p=new ArrayList<>();
      int wood=Tex.PLANKS,iron=UtilityModels.METAL,stone=TexArt313.ANDESITE;
      if(k!=Kinetics.GEARBOX)p.add(rotate(box(0,6,6,16,10,10,iron),a,phase));
      if(k==Kinetics.COG||k==Kinetics.LARGE||k==Kinetics.WHEEL){
         double radius=k==Kinetics.COG?6.5:k==Kinetics.LARGE?12.5:7;int teeth=k==Kinetics.COG?8:k==Kinetics.LARGE?16:12;
         for(int i=0;i<teeth;i++)p.add(rotate(box(5.5,8+radius-2,6,10.5,8+radius+2,10,wood),a,phase+i*Math.PI*2/teeth));
         for(int i=0;i<4;i++)p.add(rotate(box(6,8,6.5,10,8+radius,9.5,wood),a,phase+i*Math.PI/2));
         p.add(rotate(box(5,4,4,11,12,12,iron),a,phase));
      }else if(k==Kinetics.PRESS){
         p.add(box(0,0,0,16,2,16,stone));p.add(box(1,2,12,4,14,15,iron));p.add(box(12,2,12,15,14,15,iron));p.add(box(0,10,1,16,15,15,stone));
         double down=progress>0?Math.sin(progress*Math.PI)*3:0;p.add(box(6,6-down,6,10,12-down,10,iron));p.add(box(3,5-down,3,13,7-down,12,iron));
      }else if(k==Kinetics.MILL){
         p.add(box(0,0,0,16,3,16,wood));p.add(box(1,3,1,15,11,15,stone));p.add(rotate(box(5,2,2,11,14,14,stone),a,phase));
      }else if(k!=Kinetics.SHAFT){
         p.add(box(2,2,2,14,14,14,k==Kinetics.MOTOR?iron:stone));
         if(k==Kinetics.GEARBOX)for(int axis=0;axis<3;axis++)if(axis!=a)p.add(rotate(box(0,6,6,16,10,10,iron),axis,phase));
         if(k==Kinetics.SPEED||k==Kinetics.STRESS){p.add(box(3,14,3,13,15,13,iron));p.add(rotate(box(15,7.5,3,15.4,8.5,8.5,Tex.PLANKS),1,Math.PI*(-.75+1.5*progress)));}
         else{p.add(box(1,3,1,3,13,15,wood));p.add(box(13,3,1,15,13,15,wood));}
      }
      return p.toArray(new double[0][]);
   }
   static double[][] world(Mesher.Scratch s,int x,int y,int z,int id){long k=World.pack(s.ox+x,s.oy+y,s.oz+z);double angle=s.world==null?0:s.world.kinetics.angles.getOrDefault(k,0D);double progress=s.world==null?0:KineticMachines.progress(s.world,k);if(s.world!=null&&(Kinetics.kind(id)==Kinetics.SPEED||Kinetics.kind(id)==Kinetics.STRESS)){Kinetics.Reading r=Kinetics.reading(s.world,k);progress=Kinetics.kind(id)==Kinetics.SPEED?Math.abs(r.rpm())/256:r.capacity()==0?0:Math.min(1,r.stress()/r.capacity());}return model(id,angle,progress);}
}
