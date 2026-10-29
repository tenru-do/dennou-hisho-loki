package com.example.rokidgeminisecretary;
import android.content.*;import org.json.*;
final class HealthGeminiSettings {
 static int mode(Context c){return Math.max(0,Math.min(2,c.getSharedPreferences("phone_secretary",0).getInt("health_gemini_mode",0)));}
 static JSONObject read(Context c)throws JSONException{return new JSONObject().put("mode",mode(c)).put("context",mode(c)>0).put("automatic",mode(c)==2).put("minimumIntervalMinutes",60);}
 static JSONObject save(Context c,JSONObject input)throws Exception{
  Object raw=input.opt("mode");if(!(raw instanceof Integer)||((Integer)raw)<0||((Integer)raw)>2)throw new IllegalArgumentException("連動設定を確認してください");
  if(!c.getSharedPreferences("phone_secretary",0).edit().putInt("health_gemini_mode",(Integer)raw).commit())throw new java.io.IOException("設定を保存できません");return read(c);
 }
}
