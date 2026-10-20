package com.example.rokidkeyboardbridge;
import org.json.*;
final class ImageReferenceHistory {
 static JSONArray add(JSONArray old,JSONObject value,long now)throws Exception{JSONArray result=new JSONArray().put(value);for(int i=0;i<old.length()&&result.length()<4;i++){JSONObject row=old.optJSONObject(i);if(row!=null&&ImageReferencePolicy.fresh(row.optLong("at"),now)&&row.optLong("at")!=value.optLong("at"))result.put(row);}return result;}
 static JSONObject previous(JSONArray rows,long current,long now)throws Exception{for(int i=0;i<rows.length();i++)if(rows.getJSONObject(i).optLong("at")==current){for(int j=i+1;j<rows.length();j++){JSONObject row=rows.getJSONObject(j);if(ImageReferencePolicy.fresh(row.optLong("at"),now))return new JSONObject(row.toString());}return null;}return null;}
 private ImageReferenceHistory(){}
}
