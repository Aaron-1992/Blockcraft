package bc;

import java.util.*;

/** Original component-built machinery with purpose-painted surfaces and uniform texture density. */
final class KineticModels {
   static double[] box(double x,double y,double z,double xx,double yy,double zz,int tile){
      double[] b=Arrays.copyOf(Shapes.local(Shapes.tiles(Shapes.px(x,y,z,xx,yy,zz),tile,tile,tile)),24);
      b[17]=b[18]=1;b[23]=1;return b;
   }
   static double[] rotate(double[] box,int axis,double angle){double[] p=Arrays.copyOf(box,24);p[17]=p[18]=1;p[20]=0;p[21]=angle;p[22]=axis;return p;}
   static double[][] model(int id,double phase,double progress){
      int k=Kinetics.kind(id),a=Kinetics.axis(id);List<double[]> p=new ArrayList<>();
      int wood=KineticArt.TIMBER,iron=KineticArt.AXLE,stone=KineticArt.CASING,brass=KineticArt.BRASS;
      if(k==Kinetics.BELT){
         p.add(box(0,2,1,2,10,15,stone));p.add(box(14,2,1,16,10,15,stone));
         p.add(box(2,4,0,14,11,16,KineticArt.RUBBER));
         for(int i=0;i<8;i++){double z=(i*2+phase*8/Math.PI)%16;if(z<0)z+=16;
            p.add(box(2,11,z,14,12,Math.min(16,z+1),KineticArt.RUBBER));}
         p.add(rotate(box(0,5,5,16,9,9,iron),0,phase));
         for(double[] b:p)b[19]=a==2?1:0;
         return p.toArray(new double[0][]);
      }
      if(k==Kinetics.DEPOT){
         p.add(box(3,0,3,13,3,13,stone));p.add(box(5,3,5,11,9,11,stone));
         p.add(box(1,9,1,15,11,15,brass));p.add(box(2,11,2,14,12,14,KineticArt.PRESS));
         return p.toArray(new double[0][]);
      }
      if(k!=Kinetics.GEARBOX)p.add(rotate(box(0,6,6,16,10,10,iron),a,phase));
      if(k==Kinetics.COG||k==Kinetics.LARGE||k==Kinetics.WHEEL){
         double radius=k==Kinetics.COG?6.5:k==Kinetics.LARGE?12.5:11;int teeth=k==Kinetics.COG?8:k==Kinetics.LARGE?16:8;
         for(int i=0;i<teeth;i++){
            double angle=phase+i*Math.PI*2/teeth;
            p.add(rotate(box(5.5,8+radius-2,6,10.5,8+radius+2,10,wood),a,angle));
            if(k==Kinetics.WHEEL)p.add(rotate(box(2,8+radius-2,5,14,8+radius,11,wood),a,angle));
         }
         for(int i=0;i<4;i++)p.add(rotate(box(6,8,6.5,10,8+radius,9.5,wood),a,phase+i*Math.PI/2));
         p.add(rotate(box(5,4,4,11,12,12,brass),a,phase));
      }else if(k==Kinetics.PRESS){
         p.add(box(1,4,1,15,15,15,stone));p.add(box(0,13,0,16,16,16,KineticArt.PRESS));
         // The body stays above the work; the ram extends down toward the depot/belt.
         double down=progress>0?Math.sin(progress*Math.PI)*14:0;
         p.add(box(6,-4-down,6,10,5-down,10,iron));p.add(box(3,-6-down,3,13,-3-down,13,KineticArt.PRESS));
      }else if(k==Kinetics.MILL){
         p.add(box(1,0,1,15,4,15,stone));
         p.add(box(0,3,0,3,15,16,wood));p.add(box(13,3,0,16,15,16,wood));
         p.add(box(3,3,0,13,15,3,wood));p.add(box(3,3,13,13,15,16,wood));
         p.add(rotate(box(3,5,3,13,11,13,KineticArt.MILL),1,phase));
      }else if(k!=Kinetics.SHAFT){
         p.add(box(2,2,2,14,14,14,stone));
         if(k==Kinetics.GEARBOX)for(int axis=0;axis<3;axis++)if(axis!=a)p.add(rotate(box(0,6,6,16,10,10,iron),axis,phase));
         if(k==Kinetics.SPEED||k==Kinetics.STRESS){p.add(box(3,14,3,13,15,13,KineticArt.DIAL));p.add(rotate(box(15,7.5,3,15.4,8.5,8.5,brass),1,Math.PI*(-.75+1.5*progress)));}
         else{p.add(box(1,3,1,3,13,15,brass));p.add(box(13,3,1,15,13,15,brass));}
      }
      return p.toArray(new double[0][]);
   }
}
