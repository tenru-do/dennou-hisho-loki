package com.example.rokidgeminisecretary;
import android.app.*;
import android.content.*;
import android.os.*;
import android.widget.*;
import org.json.*;
import java.lang.reflect.*;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Uses the actual saved favorite and authenticated request; never prints the bridge credential. */
public final class DestinationBindingInstrumentation extends Instrumentation {
    int checks;void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
    Field field(String s)throws Exception{Field f=MainActivity.class.getDeclaredField(s);f.setAccessible(true);return f;}
    Object member(Object object,String name)throws Exception{Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}
    @Override public void onCreate(Bundle b){super.onCreate(b);start();}
    @Override public void onStart(){Bundle result=new Bundle();try{
        MainActivity activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        JSONArray places=FavoriteDestinations.all(activity);JSONObject chosen=null;
        for(int i=0;i<places.length();i++)if(places.getJSONObject(i).optString("name").contains("ドンキ"))chosen=places.getJSONObject(i);
        check(chosen!=null,"live Donki favorite exists");String name=chosen.getString("name"),query=chosen.getString("query");
        final boolean coordinateQuery=query.matches("[-+0-9.\\s]+,[-+0-9.\\s]+");
        Method prefs=MainActivity.class.getDeclaredMethod("getPreferences");prefs.setAccessible(true);
        String token=((SharedPreferences)prefs.invoke(activity)).getString("bridge_token","");
        String path="/navigation_start?destination="+android.net.Uri.encode(query)+"&name="+android.net.Uri.encode(name)+"&mode=walking&id="+java.util.UUID.randomUUID();
        try(Socket socket=new Socket("127.0.0.1",8765)){
            socket.setSoTimeout(10000);socket.getOutputStream().write(("GET "+path+" HTTP/1.1\r\nHost: localhost\r\nX-Roki-Token: "+token+"\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            BufferedReader reader=new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.UTF_8));
            check(reader.readLine().contains("200"),"actual start request accepted");
        }
        long until=SystemClock.elapsedRealtime()+25000;boolean[] ready={false};
        while(SystemClock.elapsedRealtime()<until){runOnMainSync(()->{try{ready[0]=name.equals(((TextView)field("navigationDestinationInput").get(activity)).getText().toString()) && !GoogleNavigationClient.selectedDestinationCoordinates(name).isEmpty() && name.equals(field("confirmedSharedDestination").get(null));}catch(Exception e){throw new RuntimeException(e);}});if(ready[0])break;SystemClock.sleep(50);}
        check(ready[0],"selected name reaches phone UI");
        String[] actual=GoogleNavigationClient.selectedDestinationCoordinates(name).split(",");
        check(actual.length==2 && Double.isFinite(Double.parseDouble(actual[0])) && Double.isFinite(Double.parseDouble(actual[1])),"coordinates cached under selected name");
        if(coordinateQuery){String[] expected=query.split(",");check(Double.parseDouble(expected[0].trim())==Double.parseDouble(actual[0]) && Double.parseDouble(expected[1].trim())==Double.parseDouble(actual[1]),"explicit coordinates preserved");}
        check(name.equals(field("confirmedSharedDestination").get(null)),"navigation final identity uses selected name");
        Method build=MainActivity.class.getDeclaredMethod("buildTransitJson");build.setAccessible(true);
        JSONObject snapshot=(JSONObject)build.invoke(activity);
        check(name.equals(snapshot.optString("destination")),"Overview/HUD payload uses selected name");
        SharedPreferences state=(SharedPreferences)prefs.invoke(activity);
        check(name.equals(state.getString("map_route_destination","")),"route acquisition targets selected name");
        until=SystemClock.elapsedRealtime()+20000;
        Object cache=null;
        while(SystemClock.elapsedRealtime()<until){cache=field("googleRouteCache").get(activity);if(cache!=null&&name.equals(member(cache,"destination")))break;SystemClock.sleep(100);}
        check(cache!=null && name.equals(member(cache,"destination")) && ((JSONArray)member(member(cache,"data"),"route")).length()>=2,"fresh Google road geometry acquired; error="+state.getString("map_route_error",""));
        snapshot=(JSONObject)build.invoke(activity);
        check(name.equals(snapshot.optString("destination")) && name.equals(snapshot.optString("routeDestination")),"final route and Overview destination agree");
        result.putString("stream","Destination binding PASS checks="+checks+"; actual saved="+name+"; name/coordinates/request/HUD identity agree\n");finish(Activity.RESULT_OK,result);
    }catch(Throwable t){result.putString("stream","FAIL "+android.util.Log.getStackTraceString(t));finish(Activity.RESULT_CANCELED,result);}}
}
