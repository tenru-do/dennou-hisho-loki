package com.example.rokidkeyboardbridge;
public final class FullBodyLivingPoseTest {
 public static void main(String[]args){long checks=0;float minMotion=100;
  for(int pose=0;pose<168;pose++){
   float[]f=FullBodyRealisticFeatures.ALL[pose];float face=(f[15]+Math.max(f[16]*.25f,f[18])+4)/f[1];
   for(int kind=0;kind<=3;kind++){float[]v=new float[(FullBodyBreathPolicy.COLUMNS+1)*(FullBodyBreathPolicy.ROWS+1)*2];
   float[]old=new float[v.length];float peak=0;int c=FullBodyBreathPolicy.COLUMNS,r=FullBodyBreathPolicy.ROWS;
   for(long t=0;t<=19400;t+=66){
    FullBodyBreathPolicy.resting(v,240,601,t,face);
    FullBodyLivingPosePolicy.apply(v,240,601,t,face,kind);
    for(int row=0;row<=r;row++)for(int col=0;col<=c;col++){
     int a=(row*(c+1)+col)*2;float x=240*col/(float)c,y=601*row/(float)r;
     if(!Float.isFinite(v[a])||!Float.isFinite(v[a+1]))throw new AssertionError("Nonfinite");
     if(row/(float)r<=face||row==r)if(Math.abs(v[a]-x)>.001||Math.abs(v[a+1]-y)>.001)throw new AssertionError("Head/floor drift pose "+pose+" row "+row);
     if(t>0&&(Math.abs(v[a]-old[a])>1.5||Math.abs(v[a+1]-old[a+1])>1.5))throw new AssertionError("Temporal jump");
     if(row/(float)r>face+.02&&row/(float)r<face+.22)peak=Math.max(peak,Math.abs(v[a]-x)+Math.abs(v[a+1]-y));
     if(row<r&&col<c){int b=a+2,d=a+(c+1)*2,e=d+2;
      float area=(v[b]-v[a])*(v[d+1]-v[a+1])-(v[b+1]-v[a+1])*(v[d]-v[a]);
      float area2=(v[e]-v[d])*(v[e+1]-v[b+1])-(v[e+1]-v[d+1])*(v[e]-v[b]);
      if(Math.min(area,area2)<(240f/c)*(601f/r)*.3f)throw new AssertionError("Fold pose "+pose);
     }checks++;
    }System.arraycopy(v,0,old,0,v.length);
   }
   if(peak<2)throw new AssertionError("Weak shoulder movement "+pose+" peak "+peak);minMotion=Math.min(minMotion,peak);}
  }
  if(FullBodyLivingPosePolicy.profile(2,FullBodyRealisticFeatures.ALL[2])!=3)throw new AssertionError("Arms-folded chest breathing absent");
  if(FullBodyLivingPosePolicy.profile(1,FullBodyRealisticFeatures.ALL[1])!=2)throw new AssertionError("Chin profile absent");
  System.out.println("PASS "+checks+" regional movement/mesh samples; all 168 poses upper body peak >= "+minMotion+"px; face/floor fixed");
 }
}
