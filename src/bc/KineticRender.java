package bc;

import org.lwjgl.opengl.GL11;

/** Frame-interpolated rendering independent of chunk rebuild frequency. */
final class KineticRender {
   static double angle(World w,long p,float partial){
      double rpm=Kinetics.reading(w,p).rpm();return w.kinetics.angles.getOrDefault(p,0D)+rpm*Math.PI/600*(partial-1);
   }
   static double progress(World w,long p,float partial){
      int kind=Kinetics.kind(Kinetics.block(w,p));Kinetics.Reading r=Kinetics.reading(w,p);
      if(kind==Kinetics.SPEED)return Math.abs(r.rpm())/256;
      if(kind==Kinetics.STRESS)return r.capacity()==0?0:Math.min(1,r.stress()/r.capacity());
      KineticMachines.Machine m=w.kinetics.machines.get(p);
      return m==null||m.progress==0?0:Math.min(.9999,(m.progress+Math.abs(r.rpm())*partial)/6400.0);
   }
   static void draw(Renderer r,float partial){
      World w=r.g.world;GL11.glEnable(GL11.GL_TEXTURE_2D);GL11.glBindTexture(GL11.GL_TEXTURE_2D,Tex.atlas);
      GL11.glEnable(GL11.GL_ALPHA_TEST);GL11.glAlphaFunc(GL11.GL_GREATER,.3f);
      for(long p:w.kinetics.nodes){int id=Kinetics.block(w,p);if(!Kinetics.handles(id))continue;
         double x=World.ux(p)-r.camX,y=World.uy(p)-r.camY,z=World.uz(p)-r.camZ;
         if(x*x+y*y+z*z>9216||!r.visible((float)x+.5f,(float)y+.5f,(float)z+.5f,2))continue;
         float bright=r.brightnessAt(w,World.ux(p)+.5,World.uy(p)+.5,World.uz(p)+.5);
         GL11.glPushMatrix();GL11.glTranslated(x,y,z);boxes(KineticModels.model(id,angle(w,p,partial),progress(w,p,partial)),bright);GL11.glPopMatrix();
         KineticMachines.Machine m=w.kinetics.machines.get(p);if(m==null)continue;
         Stack cargo=m.tile.slots[0]!=null?m.tile.slots[0]:m.tile.slots[2];if(cargo==null||m.kind==Kinetics.PRESS)continue;
         double offset=m.kind==Kinetics.BELT?m.progress/6400.0-.5:0;
         if(m.kind==Kinetics.BELT)offset=Math.max(-.5,Math.min(.5,offset+m.transportVelocity*partial/6400));
         GL11.glPushMatrix();GL11.glTranslated(x+.5+KineticTransport.dx(id)*offset,y+(m.kind==Kinetics.MILL?1.05:.86),z+.5+KineticTransport.dz(id)*offset);
         GL11.glScalef(.35f,.35f,.35f);
         if(Items.isItem(cargo.id))Renderer.drawItemModel(cargo.id,bright);else Renderer.drawBlockModel(cargo.id,bright);
         GL11.glPopMatrix();
      }
      GL11.glColor4f(1,1,1,1);
   }
   static void boxes(double[][] boxes,float bright){
      float[] xyz=new float[3],uv=new float[2];GL11.glBegin(GL11.GL_QUADS);
      for(double[] b:boxes)for(int f=0;f<6;f++){
         if(((int)b[6]&(1<<f))!=0)continue;int t=(int)b[f==0?9:f==1?10:8];float shade=bright*Mesher.SHADE[f];GL11.glColor3f(shade,shade,shade);
         for(int v=0;v<4;v++){Shapes.corner(b,f,v,xyz,uv);GL11.glTexCoord2f(Tex.tu(t)+uv[0]*Tex.TS,Tex.tv(t)+uv[1]*Tex.TS);GL11.glVertex3f(xyz[0],xyz[1],xyz[2]);}
      }GL11.glEnd();
   }
}
