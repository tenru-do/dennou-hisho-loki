package com.example.rokidkeyboardbridge;
import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.util.*;
import java.lang.reflect.*;

public final class NavigationRepairInstrumentation extends Instrumentation {
    int checks;
    void check(boolean value,String name){checks++;if(!value)throw new AssertionError(name);}
    Button find(View view,String text){if(view instanceof Button&&((Button)view).getText().toString().replace("✓ ","").equals(text))return (Button)view;
        if(view instanceof android.view.ViewGroup){android.view.ViewGroup g=(android.view.ViewGroup)view;for(int i=0;i<g.getChildCount();i++){Button b=find(g.getChildAt(i),text);if(b!=null)return b;}}return null;}
    @Override public void onCreate(Bundle b){super.onCreate(b);start();}
    @Override public void onStart(){Bundle result=new Bundle();try {
        Activity activity=startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        String live="not checked";
        try{Method m=MainActivity.class.getDeclaredMethod("fetchPhoneEndpointJson",String.class);m.setAccessible(true);
            JSONObject wire=new JSONObject((String)m.invoke(activity,"navigation_places"));
            check(wire.optJSONArray("places")!=null,"live wire list");
            live="live phone places="+SavedNavigationPlace.parse(wire).size();
        }catch(Exception unavailable){live="live endpoint unavailable: "+unavailable.getClass().getSimpleName();}
        List<SavedNavigationPlace> places=SavedNavigationPlace.parse(new JSONObject("{\"places\":[{\"name\":\"立川駅\",\"query\":\"35.697,139.413\"},{\"name\":\"新宿駅\",\"latitude\":35.69,\"longitude\":139.7}]}"));
        check(places.size()==2,"mock reception");final Throwable[] error={null};
        runOnMainSync(()->{try {
            TextView input=new TextView(activity),feedback=new TextView(activity);int[] selected={0};
            String[] names={places.get(0).name,places.get(1).name};
            for(int code:new int[]{KeyEvent.KEYCODE_DPAD_CENTER,KeyEvent.KEYCODE_ENTER,KeyEvent.KEYCODE_BUTTON_SELECT,KeyEvent.KEYCODE_NUMPAD_ENTER}) {
                AlertDialog dialog=NavigationChoiceDialog.show(activity,"登録地点",names,0,(d,i)->{selected[0]++;MainActivity.bindNavigationPlace(input,feedback,places.get(i));});
                Button button=find(dialog.getWindow().getDecorView(),names[1]);check(button!=null,"item exists");
                check(button.isFocusable()&&button.isClickable(),"selectable item");button.requestFocus();int before=selected[0];
                dialog.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,code));
                dialog.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,code));
                check(selected[0]==before+1,"one selection per key "+code);
                check(input.getText().toString().equals("新宿駅")&&input.getTag().equals("35.69,139.7"),"name and coordinates bound");
            }
            AlertDialog dialog=NavigationChoiceDialog.show(activity,"登録地点",names,0,(d,i)->{selected[0]++;MainActivity.bindNavigationPlace(input,feedback,places.get(i));});
            View decor=dialog.getWindow().getDecorView();decor.measure(View.MeasureSpec.makeMeasureSpec(480,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(320,View.MeasureSpec.EXACTLY));decor.layout(0,0,480,320);
            Bitmap bitmap=Bitmap.createBitmap(480,320,Bitmap.Config.ARGB_8888);decor.draw(new Canvas(bitmap));
            try(FileOutputStream out=new FileOutputStream(new File(getTargetContext().getExternalFilesDir(null),"saved-location-selection.png"))){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}bitmap.recycle();
            Button button=find(decor,names[0]);int before=selected[0];long now=android.os.SystemClock.uptimeMillis();
            MotionEvent down=MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,10,10,0),up=MotionEvent.obtain(now,now+20,MotionEvent.ACTION_UP,10,10,0);
            button.dispatchTouchEvent(down);button.dispatchTouchEvent(up);down.recycle();up.recycle();
            check(selected[0]==before+1,"first tap activates without focus-only step");
            check(input.getText().toString().equals("立川駅"),"first tap binds name");
            check(input.getTag().equals("35.697,139.413"),"tap binds coordinates");
            Field mf=MainActivity.class.getDeclaredField("navigationMap");mf.setAccessible(true);View map=(View)mf.get(activity);
            Field lat=map.getClass().getDeclaredField("latitude"),lng=map.getClass().getDeclaredField("longitude");lat.setAccessible(true);lng.setAccessible(true);lat.set(map,35.0);lng.set(map,139.0);
            Method route=map.getClass().getDeclaredMethod("setRoute",String.class);route.setAccessible(true);
            route.invoke(map,"[[35,139],[35,139.002],[35.003,139.002],[35.003,139.004],[35.008,139.01]]");
            map.measure(View.MeasureSpec.makeMeasureSpec(234,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(148,View.MeasureSpec.EXACTLY));map.layout(0,0,234,148);
            Method overview=map.getClass().getDeclaredMethod("setOverview",boolean.class);overview.setAccessible(true);overview.invoke(map,true);
            bitmap=Bitmap.createBitmap(234,148,Bitmap.Config.ARGB_8888);map.draw(new Canvas(bitmap));
            try(FileOutputStream out=new FileOutputStream(new File(getTargetContext().getExternalFilesDir(null),"walking-transit-shape.png"))){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}bitmap.recycle();
        }catch(Throwable e){error[0]=e;}});
        if(error[0]!=null)throw new RuntimeException(error[0]);
        result.putString("stream","Navigation selection PASS checks="+checks+"; "+live+"\n");finish(Activity.RESULT_OK,result);
    }catch(Throwable e){result.putString("stream","FAIL "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);}}
}
