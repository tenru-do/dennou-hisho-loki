package com.example.rokidgeminisecretary;
import android.content.*;import org.json.*;import java.io.*;
/** One explicit import request, independent of AI conversation delivery. Contains no photo data. */
final class PhotoImportRequests {
 static final String KEY="photo_import_request_v1";static final long TTL=600000;
 static JSONObject current(Context c){try{return new JSONObject(c.getSharedPreferences("phone_secretary",0).getString(KEY,"{}"));}catch(Exception invalid){return new JSONObject();}}
 static boolean valid(JSONObject request,long now){long at=request.optLong("createdAt"),end=request.optLong("expiresAt");return request.optString("id").matches("[a-f0-9-]{36}")&&request.optString("mode").matches("copy|move")&&at>0&&now>=at&&now<=end&&end-at==TTL;}
 static JSONObject peek(Context c,long now)throws Exception{JSONObject request=current(c),out=new JSONObject().put("ok",true);if(valid(request,now))out.put("request",request);return out;}
 static JSONObject accept(Context c,String id,long now)throws Exception{synchronized(MainActivity.class){JSONObject request=current(c);if(!id.equals(request.optString("id"))||!valid(request,now))return new JSONObject().put("ok",true).put("execute",false);if(!request.has("acceptedAt")){request.put("acceptedAt",now);if(!c.getSharedPreferences("phone_secretary",0).edit().putString(KEY,request.toString()).commit())throw new IOException("取り込みの受付を保存できません");}return new JSONObject().put("ok",true).put("execute",true).put("request",request);}}
 private PhotoImportRequests(){}
}
