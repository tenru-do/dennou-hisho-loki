package com.example.rokidkeyboardbridge;
import android.content.*;import android.os.*;import android.widget.*;import java.lang.reflect.*;
public final class NotificationExpiryDeviceChecks {
 static int n;static void ok(boolean b,String m){n++;if(!b)throw new AssertionError(m);}
 static void field(MainActivity a,String name,Object value)throws Exception{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);f.set(a,value);}
 static boolean active(MainActivity a)throws Exception{Field f=MainActivity.class.getDeclaredField("conversationActive");f.setAccessible(true);return f.getBoolean(a);}
 static Runnable callback(MainActivity a,int ticket,String text,long age)throws Exception{
  Class<?> type=Class.forName("com.example.rokidkeyboardbridge.MainActivity$103");Constructor<?> c=type.getDeclaredConstructors()[0];c.setAccessible(true);Class<?>[] types=c.getParameterTypes();Object[] args=new Object[types.length];
  for(int i=0;i<types.length;i++){if(types[i]==MainActivity.class)args[i]=a;else if(types[i]==int.class)args[i]=ticket;else if(types[i]==String.class)args[i]=text;else if(types[i]==long.class)args[i]=SystemClock.elapsedRealtime()-age;else throw new AssertionError("unexpected callback argument");}return (Runnable)c.newInstance(args);
 }
 public static int run(Context context)throws Exception{
  MainActivity a=new MainActivity();Method attach=ContextWrapper.class.getDeclaredMethod("attachBaseContext",Context.class);attach.setAccessible(true);attach.invoke(a,context);Field window=android.app.Activity.class.getDeclaredField("mWindow");window.setAccessible(true);window.set(a,new android.app.Dialog(context).getWindow());TextView answer=new TextView(context);field(a,"answer",answer);field(a,"assistantNoticeSequence",5);field(a,"requestGeneration",999);Method blocked=MainActivity.class.getDeclaredMethod("codexNotificationBlocked");blocked.setAccessible(true);field(a,"activityForeground",false);ok(!((Boolean)blocked.invoke(a)),"background alone no longer defers notification");field(a,"voiceRecording",true);ok(((Boolean)blocked.invoke(a)),"recording still defers wake");field(a,"voiceRecording",false);
  field(a,"conversationActive",true);answer.setText("");Runnable first=callback(a,5,"CODEX\n\nsynthetic",20000);first.run();ok(!active(a),"actual callback clears empty surface despite unrelated request generation");
  field(a,"conversationActive",true);answer.setText("CODEX\n\nsynthetic");callback(a,5,"CODEX\n\nsynthetic",20000).run();ok(!active(a),"actual notification owner expires");
  field(a,"conversationActive",true);answer.setText("different synthetic answer");callback(a,5,"CODEX\n\nsynthetic",20000).run();ok(active(a),"different answer retained");
  answer.setText("");callback(a,4,"CODEX\n\nsynthetic",20000).run();ok(active(a),"old notification cannot clear newer state");
  field(a,"voiceRecording",true);Runnable busy=callback(a,5,"CODEX\n\nsynthetic",20000);busy.run();ok(active(a),"recording never interrupted");Field h=MainActivity.class.getDeclaredField("handler");h.setAccessible(true);((Handler)h.get(a)).removeCallbacks(busy);field(a,"voiceRecording",false);
  busy.run();ok(!active(a),"cleanup resumes after active work ends");((Handler)h.get(a)).removeCallbacks(busy);
  field(a,"conversationActive",true);field(a,"geminiRequestActive",true);Runnable ai=callback(a,5,"CODEX\n\nsynthetic",20000);ai.run();ok(active(a),"new AI request protected");((Handler)h.get(a)).removeCallbacks(ai);field(a,"geminiRequestActive",false);
  ((Handler)h.get(a)).removeCallbacksAndMessages(null);return n;
 }
}
