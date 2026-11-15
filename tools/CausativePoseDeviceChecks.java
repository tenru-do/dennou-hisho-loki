package com.example.rokidkeyboardbridge;
import android.content.*;import java.lang.reflect.*;import java.util.concurrent.atomic.*;
/** New source in a permission-free separate UID; API budget zero and no device secrets. */
public final class CausativePoseDeviceChecks {
 static int n;static void ok(boolean b,String m){n++;if(!b)throw new AssertionError(m);}
 public static int run(Context c)throws Exception{
  n=0;MainActivity owner=new MainActivity();Field budget=MainActivity.class.getDeclaredField("diagnosticGeminiBudget");budget.setAccessible(true);((AtomicInteger)budget.get(owner)).set(0);Method create=MainActivity.class.getDeclaredMethod("createConversationAnswer",String.class,String.class,boolean.class,String.class);create.setAccessible(true);
  FullBodyMascotView view=new FullBodyMascotView(c);view.layout(0,0,240,640);Field embodied=FullBodyMascotView.class.getDeclaredField("embodied");embodied.setAccessible(true);
  try{for(String text:new String[]{"ロキを寝かせる","こちらがロキを寝かせる","私がロキをそっと寝かせる","あなたを横たえてあげる","ロキを寝そべらせて"}){
   ok(RestPoseIntent.kind(text)==RestPoseIntent.LIE,"causative kind "+text);ok(AvatarActionDirector.posture(text)==AvatarActionDirector.LIE,"director lying");ok(AvatarPostureContext.pose(text).equals("LYING"),"context agrees");ok(RestPoseIntent.systemRule(text).contains("要求姿勢はLYING"),"complete added system rule agrees");
   String reply=RestPoseIntent.localReply(text,false);ok(!reply.isEmpty(),"provider-free reply");ok(!view.localVisualReply(text,false).isEmpty(),"actual view local dispatch");view.setUserContext("座って");view.setUserContext(text);EmbodiedScenePolicy pose=(EmbodiedScenePolicy)embodied.get(view);ok(pose.correctedRest(),"actual view enters approved lying path");view.setMode(2);view.setMode(0);ok(pose.correctedRest(),"answer start/end preserves lying");
   String answer=(String)create.invoke(owner,"",text,false,reply);ok(answer.equals(reply),"real answer method returns local result");ok(((AtomicInteger)budget.get(owner)).get()==0,"provider budget untouched");
  }
  for(String text:new String[]{"彼女を寝かせる","ロキを寝かせる方法を教えて","ロキを寝かせるという例文","私は横になる"})ok(RestPoseIntent.localReply(text,false).isEmpty(),"non-command stays conversation");
  }finally{view.releaseInspection();}return n;
 }
}
