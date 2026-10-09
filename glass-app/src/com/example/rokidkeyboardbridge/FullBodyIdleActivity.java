package com.example.rokidkeyboardbridge;
/** Quiet visual-only activities selected by current posture. Speech/listening preempt these in the view. */
final class FullBodyIdleActivity {
 static boolean automaticPenEnabled=false;
 static final long PERIOD_MS=300000;
 static int kind(long elapsed){long rare=Math.max(0,elapsed)%600000L;if(rare>=465000&&rare<471700)return 2;long t=Math.max(0,elapsed)%PERIOD_MS;return t>=15000&&t<45000?0:t>=105000&&t<117000?1:-1;}
 static int kind(long elapsed,boolean seated){long time=Math.max(0,elapsed)%PERIOD_MS;if(time>=60000&&time<90000)return seated?5:4;int activity=kind(elapsed);if(activity==2&&!automaticPenEnabled)return -1;return seated?activity:activity==2?3:-1;}
 static int firstFrame(int kind){return kind==4?264:kind==5?270:kind==3?246:222+kind*6;}
 static int[] frames(int kind){int n=firstFrame(kind);return new int[]{n,n+1,n+2,n+3,n+4,n+5};}
 static boolean isFrame(int pose){return pose>=222&&pose<240||pose>=246&&pose<252||pose>=264&&pose<276;}
 static final int[] PLANNER={0,1,2,3,2,3,4,3,5,1,0};
 static final long[] PLANNER_MS={1400,1700,2400,4200,2200,4300,2400,4300,2000,1600,3500};
 static final int[] TABLET={0,1,2,3,4,3,2,4,5,4,2,3,4,1,0};
 static final long[] TABLET_MS={1000,3000,180,500,1500,400,180,1400,600,1400,180,500,1500,2000,600};
 static final long[] COFFEE_MS={1200,600,550,1500,600,1600};
 static final long[] PEN_MS={900,400,1000,1200,1400,1800};
 static int frame(int kind,long elapsed){
  if(kind==4||kind==5){long t=Math.max(0,elapsed%PERIOD_MS-60000);for(int i=0;i<PLANNER_MS.length;i++){if(t<PLANNER_MS[i])return PLANNER[i];t-=PLANNER_MS[i];}return 0;}
  if(kind==2||kind==3){long t=Math.max(0,elapsed%600000L-465000L);for(int i=0;i<PEN_MS.length;i++){if(t<PEN_MS[i])return i;t-=PEN_MS[i];}return 5;}
  long[] durations=kind==0?TABLET_MS:COFFEE_MS;long total=0;for(long d:durations)total+=d;
  long t=Math.max(0,elapsed%PERIOD_MS-(kind==0?15000:105000))%total;
  for(int i=0;i<durations.length;i++){if(t<durations[i])return kind==0?TABLET[i]:i;t-=durations[i];}return 0;
 }
}
