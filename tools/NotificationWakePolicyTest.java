package com.example.rokidkeyboardbridge;
public final class NotificationWakePolicyTest {
 static int n;static void ok(boolean b,String m){n++;if(!b)throw new AssertionError(m);}
 public static void main(String[]args){for(int mask=0;mask<32;mask++){boolean fg=(mask&1)!=0,on=(mask&2)!=0,latest=(mask&4)!=0,same=(mask&8)!=0,busy=(mask&16)!=0;int mode=NotificationWakePolicy.restoreMode(fg,on);ok(mode==(!on?2:!fg?1:0),"capture previous display/task state");ok(NotificationWakePolicy.ready(fg,on)==(fg&&on),"do not acknowledge before display ready");for(int m=-1;m<5;m++)ok(NotificationWakePolicy.restore(m,latest,same,busy)==((m==1||m==2)&&latest&&same&&!busy),"never hide user interaction, new content or real work");}System.out.println("PASS "+n+" wake admission and display restoration checks");}
}
