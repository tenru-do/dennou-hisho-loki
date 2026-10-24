package com.example.rokidgeminisecretary;
import android.content.Context;import org.json.*;import java.util.concurrent.atomic.AtomicLong;
public class RouteDeadlineTest {
 static int n;static void ok(boolean b){n++;if(!b)throw new AssertionError("deadline assertion "+n);}
 public static int run(Context c,String path)throws Exception{n=0;AtomicLong clock=new AtomicLong(100);NavigationRouteDeadline d=new NavigationRouteDeadline(clock::get);ok(!d.expired());clock.set(75099);ok(!d.expired());clock.set(75100);ok(d.expired());NavigationRouteDeadline.enter(d);boolean thrown=false;try{NavigationRouteDeadline.check();}catch(java.io.InterruptedIOException e){thrown=true;}ok(thrown);NavigationRouteDeadline.leave();NavigationRouteDeadline.check();clock.set(99);ok(d.expired());
 for(String mode:new String[]{"walking","bicycling","driving","transit"})for(int i=0;i<100;i++){
  JSONObject fresh=new JSONObject().put("routeStatus","fetching").put("routeRefreshing",true).put("mode",mode);NavigationRouteDeadline.present(fresh,true,false);ok(fresh.optString("routeStatus").equals("fetching"));
  JSONObject orphan=new JSONObject().put("routeStatus","fetching").put("journey",new JSONObject()).put("routeReady",false);NavigationRouteDeadline.present(orphan,false,false);ok(orphan.optString("routeStatus").equals("request_failed"));ok(!orphan.has("journey"));ok(!orphan.optBoolean("routeRefreshing"));
  JSONObject timeout=new JSONObject().put("routeStatus","ready").put("routeReady",true).put("totalRemainingDistance","old");NavigationRouteDeadline.present(timeout,true,true);ok(timeout.optString("routeError").equals("google_route_timeout"));ok(!timeout.optBoolean("routeReady"));ok(timeout.optString("totalRemainingDistance").isEmpty());
  JSONObject healthy=new JSONObject().put("routeStatus","ready").put("routeReady",true);NavigationRouteDeadline.present(healthy,false,false);ok(healthy.optBoolean("routeReady"));
 }
 return n;}
}