package com.example.rokidkeyboardbridge;
/** Explicit exercise artwork selection. No inferred health state or model input. */
final class RecoveryExercisePolicy {
 static final String[] ASSETS={"recovery-20261114/standing-01.png","recovery-20261114/standing-02.png","recovery-20261114/standing-03.png","recovery-20261114/standing-04.png","recovery-20261114/standing-05.png","recovery-20261114/standing-06.png","recovery-20261114/standing-07.png","recovery-20261114/standing-08.png","recovery-20261114/seated-01.png","recovery-20261114/seated-02.png","recovery-20261114/seated-03.png","recovery-20261114/seated-04.png","recovery-20261114/seated-05.png","recovery-20261114/seated-06.png","recovery-20261114/seated-07.png","recovery-20261114/seated-08.png"};
 static final float[][] GEOMETRY={{1024f,1536f,522.5f,62.232155f,0f,0.177083f},{1024f,1536f,515.5f,53.043515f,0f,0.108073f},{1024f,1536f,532.0f,53.46118f,0f,0.143229f},{1024f,1536f,521.5f,52.208184f,0f,0.152344f},{1024f,1536f,496.5f,53.043515f,0f,0.141927f},{1024f,1536f,478.5f,61.396824f,0f,0.145833f},{1024f,1536f,511.5f,55.549507f,0f,0.151042f},{1024f,1536f,513.5f,53.043515f,0f,0.138021f},{1024f,1536f,517.5f,73.926788f,0f,0.179036f},{1024f,1536f,545.0f,65.991144f,0f,0.161458f},{1024f,1536f,526.0f,64.320482f,0f,0.169922f},{1024f,1536f,522.5f,68.079472f,0f,0.175781f},{1024f,1536f,501.5f,73.091457f,0f,0.179036f},{1024f,1536f,525.5f,77.268112f,0f,0.1875f},{1024f,1536f,505.5f,71.420795f,0f,0.173828f},{1024f,1536f,520.0f,72.673792f,0f,0.178385f}};
 static final int COUNT=20;
 static boolean valid(int i){return i>=0&&i<COUNT;}
 static int posture(int i){return i<8?0:i<16?1:2;}
 static String label(int i){return i<8?"立位 "+(i+1):i<16?"座位 "+(i-7):"既存の寝姿 "+(i-15);}
 static int oldRest(int i){return 3+Math.max(0,Math.min(3,i-16));}
 static String asset(int i){if(!valid(i))throw new IllegalArgumentException("pose");return i<16?ASSETS[i]:ApprovedQuietPosePolicy.ASSETS[oldRest(i)];}
 static float[] box(int i,int w,int h){if(!valid(i))throw new IllegalArgumentException("pose");if(i>=16)return ApprovedQuietPosePolicy.fit(oldRest(i),w,h,1,1);float[] a=GEOMETRY[i];return CharacterViewport.fit(w,h,a[0],a[1],a[2],a[3],a[4]);}
 static float faceBottom(int i){if(i<16)return GEOMETRY[i][5];float[] f=ApprovedQuietPosePolicy.FACE[oldRest(i)];return (f[15]+f[16])/f[1];}
 static void mesh(float[] v,int w,int h,float face,long elapsed,int strength){
  long period=strength==2?1350:3000;float pulse=(float)((1-Math.cos(Math.max(0,elapsed)%period*2*Math.PI/period))*.5);
  float start=Math.min(.8f,face+.035f),end=Math.min(.9f,start+.24f);int at=0;
  for(int row=0;row<=32;row++){float y=row/32f,band=y<=start||y>=end?0:(float)Math.pow(Math.sin(Math.PI*(y-start)/(end-start)),2);
   for(int col=0;col<=16;col++){float x=col/16f,edge=(float)Math.sin(Math.PI*x);v[at++]=x*w;v[at++]=y*h-(strength==0?0:h*(strength==2?.0045f:.0012f)*pulse*band*edge);}
  }
 }
 private RecoveryExercisePolicy(){}
}
