package com.example.rokidkeyboardbridge;
import java.lang.reflect.*;import java.util.concurrent.atomic.*;
public final class RestLocalReplyDeviceChecks {
 public static int run()throws Exception{int n=0;MainActivity owner=new MainActivity();Field budget=MainActivity.class.getDeclaredField("diagnosticGeminiBudget");budget.setAccessible(true);((AtomicInteger)budget.get(owner)).set(0);Method create=MainActivity.class.getDeclaredMethod("createConversationAnswer",String.class,String.class,boolean.class,String.class);create.setAccessible(true);
 String[] q={"ロキ、くつろいで","ねそべって","ねそべらないな","ねそべらないで"};String[] expected={"くつろぐ姿勢に切り替えます。","寝姿に切り替えます。","失礼しました。寝姿への切り替えをやり直します。","寝姿をやめて、座る姿勢にします。"};for(int i=0;i<q.length;i++){String local=RestPoseIntent.localReply(q[i],false);if(!local.equals(expected[i]))throw new AssertionError(q[i]);n++;String answer=(String)create.invoke(owner,"","",false,local);if(!answer.equals(expected[i]))throw new AssertionError("provider-free answer");n++;if(((AtomicInteger)budget.get(owner)).get()!=0)throw new AssertionError("provider budget changed");n++;}
 for(String text:new String[]{"彼女がねそべってと言った","この画像を説明して","私はねそべっていい？","寝そべる方法を教えて"}){if(!RestPoseIntent.localReply(text,false).isEmpty())throw new AssertionError("unrelated question intercepted");n++;}return n;}
}
