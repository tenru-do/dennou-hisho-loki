package com.example.rokidkeyboardbridge;
import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.lang.reflect.*;

/** Runs on Android against the production formatter/view and MainActivity layout. */
public final class OverviewInstrumentation extends Instrumentation {
    int checks;
    void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
    Object field(Activity a,String name)throws Exception{Field f=a.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(a);}
    void set(Activity a,String name,Object value)throws Exception{Field f=a.getClass().getDeclaredField(name);f.setAccessible(true);f.set(a,value);}
    void layout(Activity a)throws Exception{Method m=a.getClass().getDeclaredMethod("updateNavigationCommentLayout");m.setAccessible(true);m.invoke(a);}
    JSONObject fixture(String mode)throws Exception {
        JSONArray rides=new JSONArray().put(new JSONObject().put("departureStop","立川駅").put("lineName","中央線快速").put("arrivalStop","新宿駅"))
            .put(new JSONObject().put("departureStop","新宿駅").put("lineName","山手線").put("arrivalStop","渋谷駅"));
        return new JSONObject().put("journey",new JSONObject().put("requestedMode",mode).put("finalDestination","渋谷駅")
            .put("overviewSummary",new JSONObject().put("durationSeconds",2520).put("distanceMeters",18400)
            .put("arrivalTime","2026-10-03T13:35:00Z").put("transitStops",rides).put("roads",new JSONArray().put("甲州街道").put("中央道"))));
    }
    void save(View view,String name)throws Exception {
        view.measure(View.MeasureSpec.makeMeasureSpec(234,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));
        view.layout(0,0,234,view.getMeasuredHeight());
        Bitmap bitmap=Bitmap.createBitmap(234,view.getHeight(),Bitmap.Config.ARGB_8888);
        view.draw(new Canvas(bitmap));check(bitmap.getPixel(0,0)==Color.BLACK,"pure black");
        for(int i=0;i<((LinearLayout)view).getChildCount();i++) {
            TextView row=(TextView)((LinearLayout)view).getChildAt(i);
            check(row.getRight()<=234,"row bounds");
            android.text.Layout l=row.getLayout();
            for(int j=0;j<l.getLineCount();j++)check(l.getLineWidth(j)<=row.getWidth()+1,"wrapped text bounds");
        }
        try(FileOutputStream out=new FileOutputStream(new File(getTargetContext().getExternalFilesDir(null),name))){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}
        bitmap.recycle();
    }
    void saveRoot(View root,String name)throws Exception {
        root.measure(View.MeasureSpec.makeMeasureSpec(480,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(320,View.MeasureSpec.EXACTLY));
        root.layout(0,0,480,320);
        Bitmap bitmap=Bitmap.createBitmap(480,320,Bitmap.Config.ARGB_8888);root.draw(new Canvas(bitmap));
        try(FileOutputStream out=new FileOutputStream(new File(getTargetContext().getExternalFilesDir(null),name))){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}
        bitmap.recycle();
    }
    @Override public void onCreate(Bundle b){super.onCreate(b);start();}
    @Override public void onStart(){Bundle result=new Bundle();try {
        NavigationOverviewSummary transit=NavigationOverviewSummary.from(fixture("transit"));
        check(transit.duration.equals("約42分"),"whole duration");check(transit.arrival.equals("22:35"),"JST ETA");check(transit.distance.equals("18.4km"),"whole distance");
        check(transit.detail.contains("乗換: 1回")&&transit.detail.contains("中央線快速")&&transit.detail.contains("山手線"),"all lines");
        NavigationOverviewSummary driving=NavigationOverviewSummary.from(fixture("driving"));check(driving.detail.equals("甲州街道 / 中央道 経由"),"driving roads");
        JSONObject absent=fixture("transit");absent.getJSONObject("journey").remove("overviewSummary");absent.put("totalRemainingDuration","3分");
        check(NavigationOverviewSummary.from(absent).duration.equals("未取得"),"do not mix remaining values");
        final Throwable[] error=new Throwable[1];
        Activity activity=startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        android.content.SharedPreferences prefs=activity.getSharedPreferences("navigation_display",0);
        boolean oldOverview=prefs.getBoolean("overview",false),oldLarge=prefs.getBoolean("large_map",false);
        try {runOnMainSync(()->{try {
            NavigationOverviewView view=new NavigationOverviewView(getTargetContext());view.bind(transit);save(view,"overview-transit.png");
            view.bind(driving);save(view,"overview-driving.png");
            NavigationOverviewSummary longTrip=new NavigationOverviewSummary("長い目的地名称を持つ登録地点", "約142分","23:59","118.4km","利用路線・乗換",transit.detail+"\n東京駅 → とても長い路線名称の特別快速線 → 長い駅名の終着駅");
            view.bind(longTrip);save(view,"overview-long.png");
            set(activity,"mapNavigationActive",true);set(activity,"navigationOverviewSummary",transit);
            ((View)field(activity,"navigationPanel")).setVisibility(View.VISIBLE);
            ((TextView)field(activity,"answer")).setText("直近の案内コメント");
            prefs.edit().putBoolean("overview",true).putBoolean("large_map",true).commit();layout(activity);
            View map=(View)field(activity,"navigationMap");map.setVisibility(View.VISIBLE);
            Field lat=map.getClass().getDeclaredField("latitude"),lng=map.getClass().getDeclaredField("longitude");
            lat.setAccessible(true);lng.setAccessible(true);lat.set(map,35.7);lng.set(map,139.4);
            Method route=map.getClass().getDeclaredMethod("setRoute",String.class);route.setAccessible(true);
            route.invoke(map,"[[35.7,139.4],[35.69,139.7],[35.65,139.71]]");
            Method overview=map.getClass().getDeclaredMethod("setOverview",boolean.class);overview.setAccessible(true);overview.invoke(map,true);
            View summary=(View)field(activity,"navigationOverviewScroll");View answer=(View)field(activity,"answerScroll");
            check(summary.getVisibility()==View.VISIBLE&&answer.getVisibility()==View.GONE,"overview replaces comments");
            android.widget.FrameLayout.LayoutParams p=(android.widget.FrameLayout.LayoutParams)summary.getLayoutParams();
            int width=((View)field(activity,"hudRoot")).getWidth();if(width<=0)width=getTargetContext().getResources().getDisplayMetrics().widthPixels;
            check(p.width==(width-12)/2&&p.leftMargin+p.width<=width/2,"left half bounds");
            saveRoot((View)field(activity,"hudRoot"),"overview-layout.png");
            android.graphics.Rect left=new android.graphics.Rect(),right=new android.graphics.Rect();
            summary.getGlobalVisibleRect(left);map.getGlobalVisibleRect(right);
            check(left.right<=right.left,"summary does not overlap map");
            prefs.edit().putBoolean("overview",false).commit();layout(activity);
            check(summary.getVisibility()==View.GONE,"follow hides summary");
            check(((View)field(activity,"navigationGuidanceColumn")).getVisibility()==View.VISIBLE,"follow guidance restored");
            check(((TextView)field(activity,"answer")).getText().toString().equals("直近の案内コメント"),"comment preserved");
            ((TextView)field(activity,"navigationHud")).setText("200m先を右折");
            ((View)field(activity,"navigationHud")).setVisibility(View.VISIBLE);
            saveRoot((View)field(activity,"hudRoot"),"overview-follow-layout.png");
        }catch(Throwable e){error[0]=e;}});
        if(error[0]!=null)throw new RuntimeException(error[0]);
        }finally{prefs.edit().putBoolean("overview",oldOverview).putBoolean("large_map",oldLarge).commit();}
        result.putString("stream","Overview PASS checks="+checks+"; transit/driving/long-text render; overview/follow switch\n");finish(Activity.RESULT_OK,result);
    }catch(Throwable e){result.putString("stream","FAIL "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);}}
}
