package com.example.rokidkeyboardbridge;
import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.ColorDrawable;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

public final class CoreRecoveryInstrumentation extends Instrumentation {
    int checks;
    void check(boolean ok,String name){checks++;if(!ok)throw new AssertionError(name);}
    Button find(View v,String text){
        if(v instanceof Button && ((Button)v).getText().toString().replace("✓ ","").equals(text))return (Button)v;
        if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){Button b=find(((ViewGroup)v).getChildAt(i),text);if(b!=null)return b;}
        return null;
    }
    Method method(String name)throws Exception{Method m=MainActivity.class.getDeclaredMethod(name);m.setAccessible(true);return m;}
    void awaitLayout(AlertDialog dialog) throws Exception {
        long until=SystemClock.elapsedRealtime()+5000;boolean[] ready={false};
        while(SystemClock.elapsedRealtime()<until){runOnMainSync(()->{View v=dialog.getWindow().getDecorView();ready[0]=v.getWidth()>0&&v.getHeight()>0;});if(ready[0])return;SystemClock.sleep(30);}
        throw new AssertionError("dialog layout timeout");
    }
    void awaitRelease(Semaphore resource,String label) throws Exception {
        boolean acquired=resource.tryAcquire(10,TimeUnit.SECONDS);
        try {check(acquired,label+" can be reacquired after close");} finally {if(acquired)resource.release();}
    }
    JSONObject capture(MainActivity activity,JSONObject request) throws Exception {
        CountDownLatch done=new CountDownLatch(1);JSONObject[] value={null};Throwable[] error={null};
        java.util.concurrent.atomic.AtomicBoolean cancelled=new java.util.concurrent.atomic.AtomicBoolean();
        Thread worker=new Thread(()->{try{value[0]=TopicObservationCapture.capture(activity,request,cancelled::get);}catch(Throwable e){error[0]=e;}finally{done.countDown();}},"test-capture");worker.start();
        if(!done.await(25,TimeUnit.SECONDS)){cancelled.set(true);worker.interrupt();if(!done.await(10,TimeUnit.SECONDS))throw new AssertionError("capture close timeout");throw new AssertionError("capture timeout");}
        if(error[0]!=null)throw new RuntimeException(error[0]);return value[0];
    }
    TextView destination(View v){if(v instanceof TextView&&v.getTag() instanceof String)return (TextView)v;
        if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){TextView t=destination(((ViewGroup)v).getChildAt(i));if(t!=null)return t;}return null;}
    @Override public void onCreate(Bundle b){super.onCreate(b);start();}
    @Override public void onStart(){Bundle result=new Bundle();try{
        MainActivity activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        Throwable[] error={null}; AlertDialog[] parent={null}, settings={null};
        runOnMainSync(()->{try{method("showControlsTemporarily").invoke(activity);settings[0]=(AlertDialog)method("showDisplaySettings").invoke(activity);}catch(Throwable t){error[0]=t;}});
        if(error[0]!=null)throw new RuntimeException(error[0]);
        awaitLayout(settings[0]);
        runOnMainSync(()->{try{
            AlertDialog set=settings[0];
            check(((ColorDrawable)set.getWindow().getDecorView().getBackground()).getColor()==Color.BLACK,"SET decor RGB 0,0,0");
            Bitmap image=Bitmap.createBitmap(set.getWindow().getDecorView().getWidth(),set.getWindow().getDecorView().getHeight(),Bitmap.Config.ARGB_8888);
            set.getWindow().getDecorView().draw(new Canvas(image));
            check(image.getPixel(2,2)==Color.BLACK,"SET rendered background RGB 0,0,0"); image.recycle();set.dismiss();
            parent[0]=(AlertDialog)method("showNavigationDestinationSelection").invoke(activity);
        }catch(Throwable t){error[0]=t;}});
        if(error[0]!=null)throw new RuntimeException(error[0]);
        awaitLayout(parent[0]);
        runOnMainSync(()->{try{
            Button favorites=find(parent[0].getWindow().getDecorView(),"登録地点");check(favorites!=null,"actual favorites button");
            long now=SystemClock.uptimeMillis();
            MotionEvent down=MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,10,10,0),up=MotionEvent.obtain(now,now+20,MotionEvent.ACTION_UP,10,10,0);
            favorites.dispatchTouchEvent(down);favorites.dispatchTouchEvent(up);down.recycle();up.recycle();
        }catch(Throwable t){error[0]=t;}});
        if(error[0]!=null)throw new RuntimeException(error[0]);
        Field choice=MainActivity.class.getDeclaredField("navigationChoiceDialog");choice.setAccessible(true);
        AlertDialog list=null;long until=SystemClock.elapsedRealtime()+10000;
        while(SystemClock.elapsedRealtime()<until){list=(AlertDialog)choice.get(activity);if(list!=null&&list.isShowing())break;Thread.sleep(50);}
        check(list!=null&&list.isShowing(),"tap opens live favorites list");
        final AlertDialog selectedList=list;
        runOnMainSync(()->{try{
            View focus=selectedList.getWindow().getDecorView().findFocus();check(focus instanceof Button,"first place focused");
            selectedList.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_ENTER));
            selectedList.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_ENTER));
            check(!selectedList.isShowing(),"ENTER selects actual place");
            TextView destination=destination(parent[0].getWindow().getDecorView());
            check(destination!=null&&!destination.getText().toString().equals("目的地を選んでください"),"actual name and coordinate binding");
            parent[0].dismiss();
            check((Integer)method("getVoiceAudioSourceIndex").invoke(activity)==0,"VOICE_RECOGNITION default despite stale setting");
        }catch(Throwable t){error[0]=t;}});
        if(error[0]!=null)throw new RuntimeException(error[0]);
        Method pause=MainActivity.class.getDeclaredMethod("pauseAmbientForUserAction",long.class);pause.setAccessible(true);
        Field observation=MainActivity.class.getDeclaredField("radioObservationActive");observation.setAccessible(true);
        boolean previous=observation.getBoolean(activity);
        runOnMainSync(()->{try{observation.setBoolean(activity,true);pause.invoke(activity,120000L);method("stopAmbientCapture").invoke(activity);}catch(Throwable t){error[0]=t;}});
        if(error[0]!=null)throw new RuntimeException(error[0]);
        StringBuilder observations=new StringBuilder();
        try {
        awaitRelease(CaptureLease.MICROPHONE,"initial microphone");awaitRelease(CaptureLease.CAMERA,"initial camera");
        for(String mode:new String[]{"vision","sound","both","vision"}){
            JSONObject request=new JSONObject().put("requestId","test-"+mode).put("revision",1).put("mode",mode);
            JSONObject captured=capture(activity,request);
            if(!mode.equals("sound"))check(captured.has("image")&&!captured.optBoolean("imageUnavailable"),"real camera "+mode);
            if(!mode.equals("vision"))check(captured.has("audio")&&!captured.optBoolean("audioUnavailable"),"real microphone "+mode);
            awaitRelease(CaptureLease.MICROPHONE,"microphone after "+mode);awaitRelease(CaptureLease.CAMERA,"camera after "+mode);
            observations.append(mode).append(" image=").append(captured.has("image")).append(" audio=").append(captured.has("audio")).append("; ");
        }
        } finally {runOnMainSync(()->{try{observation.setBoolean(activity,previous);}catch(Exception e){throw new RuntimeException(e);}});}
        result.putString("stream","Core recovery device PASS checks="+checks+"; "+observations+"\n");finish(Activity.RESULT_OK,result);
    }catch(Throwable t){result.putString("stream","FAIL "+android.util.Log.getStackTraceString(t));finish(Activity.RESULT_CANCELED,result);}}
}
