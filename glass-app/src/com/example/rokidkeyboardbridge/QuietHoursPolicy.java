package com.example.rokidkeyboardbridge;
import org.json.*;
final class QuietHoursPolicy {
 static boolean inside(int minute,int start,int end){if(minute<0||minute>=1440||start<0||start>=1440||end<0||end>=1440)return false;return start==end||start<end&&minute>=start&&minute<end||start>end&&(minute>=start||minute<end);}
 static int minute(String s){if(s==null||!s.matches("(?:[01][0-9]|2[0-3]):[0-5][0-9]"))throw new IllegalArgumentException("時刻はHH:mmで入力してください");return Integer.parseInt(s.substring(0,2))*60+Integer.parseInt(s.substring(3));}
 static boolean muted(JSONObject config,int minute,String kind){if(!config.optBoolean("enabled"))return false;boolean active=inside(minute,config.optInt("start",1320),config.optInt("end",420))||config.optBoolean("second")&&inside(minute,config.optInt("start2",540),config.optInt("end2",1080));if(!active)return false;String allowed=config.optString("allow");return !java.util.Arrays.asList(allowed.split(",")).contains(kind);}
 static JSONObject checked(JSONObject v)throws Exception{JSONObject out=new JSONObject().put("enabled",v.optBoolean("enabled")).put("second",v.optBoolean("second"));for(String key:new String[]{"start","end","start2","end2"}){int n=v.getInt(key);if(n<0||n>=1440)throw new IllegalArgumentException("時刻を確認してください");out.put(key,n);}String allow=v.optString("allow");if(!allow.matches("(?:error|schedule|codex)(?:,(?:error|schedule|codex))*|"))throw new IllegalArgumentException("例外通知を確認してください");return out.put("allow",allow);}
 private QuietHoursPolicy(){}
}
