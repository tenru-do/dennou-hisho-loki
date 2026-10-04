package com.example.rokidgeminisecretary;
import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.lang.reflect.*;

/** UI validation only: no route calculation, API call or phase fault injection. */
public final class NavigationUiBindingInstrumentation extends Instrumentation {
    int checks;void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
    Field field(String s)throws Exception{Field f=MainActivity.class.getDeclaredField(s);f.setAccessible(true);return f;}
    Button button(View v,String text){if(v instanceof Button&&text.equals(((Button)v).getText().toString()))return (Button)v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){Button b=button(((ViewGroup)v).getChildAt(i),text);if(b!=null)return b;}return null;}
    @Override public void onCreate(Bundle b){super.onCreate(b);start();}
    @Override public void onStart(){Bundle result=new Bundle();try{
        MainActivity activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        JSONArray items=FavoriteDestinations.all(activity);JSONObject found=null;
        for(int i=0;i<items.length();i++)if(items.getJSONObject(i).optString("name").contains("多摩総合医療センター"))found=items.getJSONObject(i);
        check(found!=null,"actual registered hospital exists");final JSONObject place=found;
        Method validate=MainActivity.class.getDeclaredMethod("prepareNavigationUiSelection");validate.setAccessible(true);
        EditText input=(EditText)field("navigationDestinationInput").get(activity);String previous=input.getText().toString();Object previousTag=input.getTag();CharSequence previousError=input.getError();
        Throwable[] error={null};
        try {runOnMainSync(()->{try{
            String kind=place.optString("kind","favorite");
            if(kind.equals("home")||kind.equals("work")){
                Button select=button(activity.getWindow().getDecorView(),kind.equals("work")?"勤務先":"自宅");check(select!=null,"real preset selector exists");select.performClick();
            }else{input.setText(place.getString("query"));input.setTag(place);}
            check(input.getTag()!=null,"selected destination object retained");check(input.getText().length()>0,"selection cannot become empty");
            check((Boolean)validate.invoke(activity),"hospital UI validation passes");
            String query=place.getString("query"),name=place.getString("name");
            if(query.matches("[-+0-9.\\s]+,[-+0-9.\\s]+")){
                check(input.getText().toString().equals(name),"selected name retained for start");
                String[] expected=query.split(","),actual=GoogleNavigationClient.selectedDestinationCoordinates(name).split(",");
                check(actual.length==2 && Double.parseDouble(expected[0].trim())==Double.parseDouble(actual[0])&&Double.parseDouble(expected[1].trim())==Double.parseDouble(actual[1]),"selected coordinates retained for start");
            }else check(input.getText().toString().equals(query),"saved address reaches unchanged start input");
            input.setText("");
            Button start=button(activity.getWindow().getDecorView(),"MAPナビ開始");check(start!=null&&start.isEnabled(),"actual MAP listener reachable");start.performClick();
            check(input.getText().length()==0,"empty input cannot reuse old destination");check("目的地が設定されていません".contentEquals(input.getError()),"empty validation reports visible error and toast");
            check(!(Boolean)validate.invoke(activity),"empty validation stops before route method");
            input.setText("91,139");input.setTag(new JSONObject().put("name","UI座標検証").put("query","91,139"));
            check(!(Boolean)validate.invoke(activity),"invalid coordinate rejected in UI");check(input.getError()!=null,"invalid coordinate error visible");
            input.setText("UI検証用の別目的地");input.setTag(place);
            check((Boolean)validate.invoke(activity),"manual nonempty input accepted");check(input.getTag()==null,"stale selection object discarded");check(input.getText().toString().equals("UI検証用の別目的地"),"manual input not overwritten by saved place");
        }catch(Throwable e){error[0]=e;}});if(error[0]!=null)throw new RuntimeException(error[0]);}
        finally {runOnMainSync(()->{input.setText(previous);input.setTag(previousTag);input.setError(previousError);});}
        result.putString("stream","Navigation UI binding PASS checks="+checks+"; actual hospital/name/coordinates/empty MAP click/invalid coordinate/manual input; no route injection\n");finish(Activity.RESULT_OK,result);
    }catch(Throwable e){result.putString("stream","FAIL "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);}}
}
