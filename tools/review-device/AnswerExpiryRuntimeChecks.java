package com.example.rokidkeyboardbridge;
import android.content.*;import android.os.*;import android.widget.*;import java.lang.reflect.*;import java.util.concurrent.*;
public final class AnswerExpiryRuntimeChecks {
 interface Task{void run()throws Exception;}
 static void ui(Task task)throws Exception{CountDownLatch done=new CountDownLatch(1);Throwable[] error={null};new Handler(Looper.getMainLooper()).post(()->{try{task.run();}catch(Throwable e){error[0]=e;}finally{done.countDown();}});if(!done.await(5,TimeUnit.SECONDS))throw new AssertionError("UI timeout");if(error[0]!=null)throw new AssertionError(error[0]);}
 static void field(Object o,String name,Object value)throws Exception{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);f.set(o,value);}
 public static int run(Context c)throws Exception{MainActivity[] a={null};TextView[] view={null};Method expire=MainActivity.class.getDeclaredMethod("scheduleAnswerClearAfterSpeech",int.class);expire.setAccessible(true);
 ui(()->{a[0]=new MainActivity();Method attach=ContextWrapper.class.getDeclaredMethod("attachBaseContext",Context.class);attach.setAccessible(true);attach.invoke(a[0],c);view[0]=new TextView(c);field(a[0],"answer",view[0]);field(a[0],"requestGeneration",31);field(a[0],"ttsGeneration",41);view[0].setText("synthetic completed answer");expire.invoke(a[0],41);});Thread.sleep(3400);ui(()->{if(view[0].length()!=0)throw new AssertionError("Completed answer did not clear");});
 ui(()->{view[0].setText("old synthetic answer");expire.invoke(a[0],41);view[0].setText("new synthetic answer");});Thread.sleep(3400);ui(()->{if(!view[0].getText().toString().equals("new synthetic answer"))throw new AssertionError("New answer cleared");});
 ui(()->{view[0].setText("same synthetic answer");expire.invoke(a[0],41);field(a[0],"requestGeneration",32);});Thread.sleep(3400);ui(()->{if(view[0].length()==0)throw new AssertionError("New request generation cleared");});return 3;
 }
}
