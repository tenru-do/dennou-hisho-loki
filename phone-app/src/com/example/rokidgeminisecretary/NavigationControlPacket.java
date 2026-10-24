package com.example.rokidgeminisecretary;
import org.json.*;
final class NavigationControlPacket {
 static JSONObject hold(JSONObject packet,String message,boolean paused)throws Exception{JSONObject out=packet==null?new JSONObject():new JSONObject(packet.toString());out.put("navigationTrouble","").put("etaChange","").put("navigationComplete",false).put("guidancePaused",paused).put("routeRefreshing",true).put("recoveryMessage",message).put("instruction",paused?"ナビ一時停止中":"目的地付近です");out.remove("completionId");for(String field:new String[]{"nextDistance","afterNextInstruction","afterNextDistance","afterNextDuration"})out.put(field,"");return out;}
 private NavigationControlPacket(){}
}
