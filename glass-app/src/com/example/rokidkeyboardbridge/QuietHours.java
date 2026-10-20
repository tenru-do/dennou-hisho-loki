package com.example.rokidkeyboardbridge;
import android.content.*;import org.json.*;
final class QuietHours {
 static synchronized JSONObject read(Context c){try{JSONObject value=new JSONObject(c.getSharedPreferences("quiet_hours",0).getString("config","{\"enabled\":false,\"second\":false,\"start\":1320,\"end\":420,\"start2\":540,\"end2\":1080,\"allow\":\"\",\"revision\":0}"));return QuietHoursPolicy.checked(value).put("revision",Math.max(0,value.optLong("revision")));}catch(Exception e){try{return new JSONObject().put("enabled",true).put("second",false).put("start",0).put("end",0).put("start2",0).put("end2",0).put("allow","").put("revision",0);}catch(Exception impossible){return new JSONObject();}}}
 static synchronized void save(Context c,JSONObject input,long revision)throws Exception{JSONObject old=read(c);if(revision!=old.optLong("revision"))throw new IllegalArgumentException("静かな時間が更新されています。開き直してください");JSONObject next=QuietHoursPolicy.checked(input).put("revision",revision+1);if(!c.getSharedPreferences("quiet_hours",0).edit().putString("config",next.toString()).commit())throw new java.io.IOException("設定を保存できません");}
 static boolean muted(Context c,String kind){java.util.Calendar clock=java.util.Calendar.getInstance();return QuietHoursPolicy.muted(read(c),clock.get(java.util.Calendar.HOUR_OF_DAY)*60+clock.get(java.util.Calendar.MINUTE),kind);}
 private QuietHours(){}
}
