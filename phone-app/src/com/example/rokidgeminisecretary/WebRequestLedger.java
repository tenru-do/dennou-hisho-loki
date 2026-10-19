package com.example.rokidgeminisecretary;
import org.json.*;
/** Retain unresolved receipt ownership; never evict a request that may still execute. */
final class WebRequestLedger {
 static JSONArray append(JSONArray old,JSONObject record)throws Exception{
  if(!record.optBoolean("tracked")||!TrackedWebRequest.validId(record.optString("key")))throw new IllegalArgumentException("Invalid ledger record");
  JSONArray next=new JSONArray(old.toString());
  for(int i=0;i<next.length();i++)if(record.getString("key").equals(next.getJSONObject(i).optString("key")))throw new IllegalArgumentException("Duplicate ledger ID");
  while(next.length()>=100){
   int remove=-1;for(int i=0;i<next.length();i++){JSONObject row=next.getJSONObject(i);if(!row.optBoolean("tracked")||TrackedWebRequest.terminal(row.optString("requestState"))){remove=i;break;}}
   if(remove<0)return null;
   JSONArray kept=new JSONArray();for(int i=0;i<next.length();i++)if(i!=remove)kept.put(next.getJSONObject(i));next=kept;
  }
  next.put(record);return next;
 }
 private WebRequestLedger(){}
}
