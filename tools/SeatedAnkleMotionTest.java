package com.example.rokidkeyboardbridge;
public final class SeatedAnkleMotionTest {
 static int checks;static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
 static float[] grid(){float[] m=new float[17*33*2];for(int y=0;y<=32;y++)for(int x=0;x<=16;x++){int i=(y*17+x)*2;m[i]=x*362f/16;m[i+1]=y*543f/32;}return m;}
 public static void main(String[] a){
  SeatedAnkleMotion clock=new SeatedAnkleMotion();check(clock.update(0,true)==0,"start");check(clock.update(100,true)==100,"advance");clock.update(200,false);check(clock.update(50000,false)==100,"pause");check(clock.update(60000,true)==100,"no catchup");check(clock.update(60100,true)==200,"resume");check(clock.update(1,true)==200,"clock reversal");clock.reset();check(clock.update(100,true)==0,"reset");
  for(float[] joints:SeatedLegWardrobe.JOINTS)for(int kind=0;kind<3;kind++){
   float[] before=grid(),m=before.clone();SeatedAnkleMotion.apply(m,16,32,362,543,joints,kind*12000+1000,1);int changed=0;
   for(int i=0;i<m.length;i+=2){check(Float.isFinite(m[i])&&Float.isFinite(m[i+1]),"finite");check(Math.abs(m[i]-before[i])<=2.51&&Math.abs(m[i+1]-before[i+1])<=2.31,"bounded local movement");if(before[i+1]<330)check(m[i]==before[i]&&m[i+1]==before[i+1],"head torso knees rigid");if(m[i]!=before[i]||m[i+1]!=before[i+1])changed++;}
   check(changed>0,"each ankle gesture changes local pixels");float[] held=before.clone();SeatedAnkleMotion.apply(held,16,32,362,543,joints,kind*12000+1000,0);check(java.util.Arrays.equals(before,held),"zero gain unchanged");
  }
  System.out.println("PASS "+checks+" ankle motion, fixed torso and pause checks");
 }
}
