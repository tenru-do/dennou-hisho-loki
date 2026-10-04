package com.example.rokidkeyboardbridge;
import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.lang.reflect.*;

/** Exercises the real destination dialog and its existing key listeners. */
public final class NavigationFocusInstrumentation extends Instrumentation {
    int checks;
    boolean regressionOnly;
    void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    Button find(View v,String text){if(v instanceof Button&&((Button)v).getText().toString().replace("✓ ","").equals(text))return (Button)v;
        if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){Button b=find(((ViewGroup)v).getChildAt(i),text);if(b!=null)return b;}return null;}
    void move(AlertDialog dialog,View from,int key,View expected){
        long until=SystemClock.elapsedRealtime()+5000;boolean[] ready={false};
        while(SystemClock.elapsedRealtime()<until){runOnMainSync(()->ready[0]=dialog.getWindow().getDecorView().hasWindowFocus());if(ready[0])break;SystemClock.sleep(30);}
        check(ready[0],"dialog owns input focus");
        runOnMainSync(()->{check(from.requestFocus(),"source accepts focus");});SystemClock.sleep(50);sendKeyDownUpSync(key);SystemClock.sleep(50);
        Throwable[] error={null};runOnMainSync(()->{try{
            View actual=dialog.getWindow().getDecorView().findFocus();
            check(actual==expected,"key="+key+" expected="+((Button)expected).getText()+" actual="+(actual instanceof Button?((Button)actual).getText():actual)+" windowFocus="+dialog.getWindow().getDecorView().hasWindowFocus());
        }catch(Throwable e){error[0]=e;}});if(error[0]!=null)throw new RuntimeException(error[0]);
    }
    void sticky(View work,View saved,int key){
        runOnMainSync(()->work.requestFocus());SystemClock.sleep(50);long down=SystemClock.uptimeMillis();
        for(int repeat=0;repeat<6;repeat++){
            sendKeySync(new KeyEvent(down,SystemClock.uptimeMillis(),KeyEvent.ACTION_DOWN,key,repeat));SystemClock.sleep(30);
            runOnMainSync(()->check(saved.hasFocus(),"forward gesture/repeat stops at saved key="+key));
        }
        sendKeySync(new KeyEvent(down,SystemClock.uptimeMillis(),KeyEvent.ACTION_UP,key,0));SystemClock.sleep(30);
        runOnMainSync(()->check(saved.hasFocus(),"forward release stays at saved"));
    }
    @Override public void onCreate(Bundle b){super.onCreate(b);regressionOnly=b!=null&&"true".equals(b.getString("regression_only"));start();}
    @Override public void onStart(){Bundle result=new Bundle();try{
        MainActivity activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        Method show=MainActivity.class.getDeclaredMethod("showNavigationDestinationSelection");show.setAccessible(true);
        Field choice=MainActivity.class.getDeclaredField("navigationChoiceDialog");choice.setAccessible(true);
        Throwable[] setupError={null};
        runOnMainSync(()->{try{
            Method controls=MainActivity.class.getDeclaredMethod("showControlsTemporarily");controls.setAccessible(true);controls.invoke(activity);
            Method settings=MainActivity.class.getDeclaredMethod("showDisplaySettings");settings.setAccessible(true);
            AlertDialog set=(AlertDialog)settings.invoke(activity);check(set!=null,"SET created");
            check(((ColorDrawable)set.getWindow().getDecorView().getBackground()).getColor()==Color.BLACK,"SET pure black RGB 0,0,0");set.dismiss();
        }catch(Throwable e){setupError[0]=e;}});
        if(setupError[0]!=null)throw new RuntimeException(setupError[0]);
        if(regressionOnly){result.putString("stream","Pure black regression PASS checks="+checks+"\n");finish(Activity.RESULT_OK,result);return;}
        for(int selectKey:new int[]{KeyEvent.KEYCODE_DPAD_CENTER,KeyEvent.KEYCODE_ENTER}){
            AlertDialog[] parent={null};Throwable[] error={null};Button[] buttons=new Button[4];
            runOnMainSync(()->{try{parent[0]=(AlertDialog)show.invoke(activity);}catch(Throwable e){error[0]=e;}});SystemClock.sleep(50);
            if(error[0]!=null)throw new RuntimeException(error[0]);
            runOnMainSync(()->{try{
                View decor=parent[0].getWindow().getDecorView();Button home=find(decor,"自宅"),work=find(decor,"勤務先"),saved=find(decor,"登録地点"),mode=find(decor,"徒歩");
                buttons[0]=home;buttons[1]=work;buttons[2]=saved;buttons[3]=mode;
                check(home!=null&&work!=null&&saved!=null&&mode!=null,"buttons retained");
                check(saved.isFocusable()&&saved.isClickable(),"saved selectable");
                check(home.focusSearch(View.FOCUS_RIGHT)==work,"home right search");
                check(work.focusSearch(View.FOCUS_RIGHT)==saved,"work right search");
                check(home.focusSearch(View.FOCUS_FORWARD)==work,"home forward search");
                check(work.focusSearch(View.FOCUS_FORWARD)==saved,"work forward search");
                check(saved.focusSearch(View.FOCUS_FORWARD)==mode,"saved forward search");
                check(saved.focusSearch(View.FOCUS_LEFT)==work,"saved left search");
                check(work.focusSearch(View.FOCUS_LEFT)==home,"work left search");
            }catch(Throwable e){error[0]=e;}});
            if(error[0]!=null)throw new RuntimeException(error[0]);
            for(int i=0;i<5;i++){
                move(parent[0],buttons[0],KeyEvent.KEYCODE_DPAD_RIGHT,buttons[1]);move(parent[0],buttons[1],KeyEvent.KEYCODE_DPAD_RIGHT,buttons[2]);
                move(parent[0],buttons[2],KeyEvent.KEYCODE_DPAD_LEFT,buttons[1]);move(parent[0],buttons[1],KeyEvent.KEYCODE_DPAD_LEFT,buttons[0]);
            }
            move(parent[0],buttons[2],KeyEvent.KEYCODE_DPAD_DOWN,buttons[3]);move(parent[0],buttons[3],KeyEvent.KEYCODE_DPAD_UP,buttons[2]);
            move(parent[0],buttons[0],KeyEvent.KEYCODE_TAB,buttons[1]);
            move(parent[0],buttons[1],KeyEvent.KEYCODE_TAB,buttons[2]);
            move(parent[0],buttons[2],KeyEvent.KEYCODE_TAB,buttons[3]);
            move(parent[0],buttons[2],KeyEvent.KEYCODE_DPAD_RIGHT,buttons[3]);move(parent[0],buttons[3],KeyEvent.KEYCODE_DPAD_UP,buttons[2]);
            sticky(buttons[1],buttons[2],KeyEvent.KEYCODE_DPAD_RIGHT);
            sticky(buttons[1],buttons[2],KeyEvent.KEYCODE_TAB);
            sendKeyDownUpSync(selectKey);SystemClock.sleep(50);
            AlertDialog list=null;long until=SystemClock.elapsedRealtime()+10000;
            while(SystemClock.elapsedRealtime()<until){list=(AlertDialog)choice.get(activity);if(list!=null&&list.isShowing())break;Thread.sleep(50);}
            check(list!=null&&list.isShowing(),"saved confirm opens live list key="+selectKey);
            final AlertDialog shown=list;runOnMainSync(()->{shown.dismiss();parent[0].dismiss();});
        }
        result.putString("stream","Navigation focus PASS checks="+checks+"; left/right cycles, down/up, DPAD_CENTER/ENTER live list\n");finish(Activity.RESULT_OK,result);
    }catch(Throwable e){result.putString("stream","FAIL "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);}}
}
