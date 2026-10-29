package com.example.rokidgeminisecretary;
import android.content.*;import org.json.*;
public final class HealthGeminiSettingsDeviceChecks {
 public static int run(Context c)throws Exception{int n=0;SharedPreferences p=c.getSharedPreferences("phone_secretary",0);p.edit().remove("health_gemini_mode").commit();if(HealthGeminiSettings.mode(c)!=0)throw new AssertionError("default off");n++;
  for(int mode=0;mode<=2;mode++){JSONObject state=HealthGeminiSettings.save(c,new JSONObject().put("mode",mode));if(state.getInt("mode")!=mode||state.getBoolean("automatic")!=(mode==2)||state.getInt("minimumIntervalMinutes")!=60)throw new AssertionError("mode");n++;}
  for(Object invalid:new Object[]{-1,3,"1",true,1.5}){boolean rejected=false;try{HealthGeminiSettings.save(c,new JSONObject().put("mode",invalid));}catch(IllegalArgumentException expected){rejected=true;}if(!rejected)throw new AssertionError("invalid accepted");n++;}
  HealthGeminiSettings.save(c,new JSONObject().put("mode",0));return n;
 }
}
