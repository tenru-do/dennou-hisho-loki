package com.example.rokidkeyboardbridge;
import org.json.*;
/** No coordinates retained. Static route age and live position age are distinct. */
final class NavigationFreshness {
 static JSONObject safe(JSONObject j)throws Exception{return new JSONObject().put("locationTime",Math.max(0,j.optLong("locationTime"))).put("routeTime",Math.max(0,j.optLong("routeTime"))).put("routeReady",j.optBoolean("routeReady")).put("active",j.optBoolean("navigationActive"));}
 static boolean linkFresh(long received,long elapsed){return received>=0&&elapsed>=received&&elapsed-received<=15000;}
 static String age(long at,long now){if(at<=0)return "未取得";if(now<at)return "時刻不整合";long seconds=(now-at)/1000;return seconds<120?seconds+"秒前":Math.min(1440,seconds/60)+"分前";}
 static String label(JSONObject j,long received,long now,long elapsed){if(!j.optBoolean("active"))return "";boolean link=linkFresh(received,elapsed);long location=j.optLong("locationTime");String status=!link?"通信更新待ち・前回の案内":location<=0||now<location||now-location>90000?"位置更新待ち":"位置受信済み";return status+" / 現在地 "+age(location,now)+" / 経路 "+(j.optBoolean("routeReady")?age(j.optLong("routeTime"),now):"未取得・確認中");}
 private NavigationFreshness(){}
}
