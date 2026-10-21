package com.example.rokidgeminisecretary;
import android.content.*;import android.view.*;import android.widget.*;import org.json.*;
final class DisplayPreferences {
 static JSONObject checked(JSONObject v)throws Exception{int size=MaintenancePolicy.size(v.getInt("size"));String shortcut=v.getString("shortcut");if(!shortcut.matches("status|search|notes|tasks"))throw new IllegalArgumentException("ショートカットを選んでください");return new JSONObject().put("size",size).put("contrast",v.getBoolean("contrast")).put("shortcut",shortcut);}
 static JSONObject read(Context c)throws Exception{SharedPreferences p=c.getSharedPreferences("maintenance_display",0);return checked(new JSONObject().put("size",p.getInt("size",20)).put("contrast",p.getBoolean("contrast",false)).put("shortcut",p.getString("shortcut","status")));}
 static void save(Context c,JSONObject v)throws Exception{v=checked(v);if(!c.getSharedPreferences("maintenance_display",0).edit().putInt("size",v.getInt("size")).putBoolean("contrast",v.getBoolean("contrast")).putString("shortcut",v.getString("shortcut")).commit())throw new java.io.IOException("表示設定を保存できません");}
 static void apply(Context c,View view){try{JSONObject p=read(c);if(view instanceof TextView){TextView t=(TextView)view;t.setTextSize(p.getInt("size"));t.setMinHeight((int)(48*c.getResources().getDisplayMetrics().density));if(p.getBoolean("contrast")){t.setTextColor(android.graphics.Color.WHITE);t.setBackgroundColor(android.graphics.Color.BLACK);}}if(view instanceof ViewGroup){ViewGroup g=(ViewGroup)view;for(int i=0;i<g.getChildCount();i++)apply(c,g.getChildAt(i));}}catch(Exception ignored){}}
 private DisplayPreferences(){}
}
