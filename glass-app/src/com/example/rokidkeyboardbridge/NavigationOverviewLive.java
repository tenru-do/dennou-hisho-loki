package com.example.rokidkeyboardbridge;
import org.json.JSONObject;
/** A live receipt is independent from the immutable whole-route summary and geometry cache. */
final class NavigationOverviewLive {
 final String instruction,detail,distance,duration,arrival,state;final long received,positionAge,packetAge;
 NavigationOverviewLive(JSONObject j,long now,long elapsed){received=elapsed;long location=j.optLong("locationTime"),packet=j.optLong("time");positionAge=location>0&&location<=now?now-location:-1;packetAge=packet>0&&packet<=now?now-packet:-1;instruction=text(j,"instruction");detail=text(j,"detail");distance=text(j,"totalRemainingDistance");duration=text(j,"totalRemainingDuration");String eta=text(j,"routeArrival");arrival=eta.isEmpty()?text(j,"arrival"):eta;state=j.optBoolean("guidancePaused")?"案内は一時停止中":j.optBoolean("arrivalPending")?"到着の確認待ち":j.optBoolean("routeRefreshing")?"経路を更新中":!j.optBoolean("routeReady")?"経路の確認待ち":"案内を受信済み";}
 static String text(JSONObject j,String key){String s=j.optString(key,"").trim();return s.substring(0,Math.min(s.length(),500));}
 static String age(long ms){return ms<0?"未取得":ms<120000?ms/1000+"秒前":ms/60000+"分前";}
 String render(long elapsed,boolean failed){long passed=elapsed>=received?elapsed-received:-1;boolean old=failed||passed<0||passed>15000||packetAge<0||packetAge+passed>90000;long fix=positionAge<0||passed<0?-1:positionAge+passed;String status=old?"通信・案内の更新待ち（前回の情報）":state;if(!old&&(fix<0||fix>90000))status="位置情報の更新待ち / "+state;StringBuilder out=new StringBuilder(status).append("\n受信 ").append(age(passed)).append(" / 現在地 ").append(age(fix));if(!instruction.isEmpty())out.append(old?"\n前回の案内：":"\n現在の案内：").append(instruction);if(!detail.isEmpty())out.append("\n").append(detail);out.append(old||fix<0||fix>90000?"\n参考の残り：":"\n残り：").append(duration.isEmpty()?"未取得":duration).append(" / ").append(distance.isEmpty()?"未取得":distance).append("\n到着予定：").append(arrival.isEmpty()?"未取得":arrival);return out.toString();}
}
