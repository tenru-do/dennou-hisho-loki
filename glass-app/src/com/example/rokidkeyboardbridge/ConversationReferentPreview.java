package com.example.rokidkeyboardbridge;
import org.json.*;
/** Display-only candidates, not a resolved target or permission for side effects. */
final class ConversationReferentPreview {
 static JSONObject build(JSONArray history,String request,long now)throws Exception{
  JSONArray candidates=new JSONArray();boolean continuation=request!=null&&request.matches("(?s).*(それ|その件|さっき|この前|前の|続き|年間では|月間では).*?");
  if(continuation)for(int i=history.length()-1;i>=0&&candidates.length()<2;i--){JSONObject row=history.optJSONObject(i);if(row==null)continue;long at=row.optLong("time"),age=now-at;if(at<=0||age<0||age>7200000)continue;String user=row.optString("user").trim();if(user.isEmpty()||user.equals(request))continue;candidates.put(new JSONObject().put("text",user.substring(0,Math.min(100,user.length()))).put("at",at));}
  return new JSONObject().put("state",candidates.length()>1?"AMBIGUOUS":candidates.length()==1?"CANDIDATE":"NONE").put("candidates",candidates).put("at",now);
 }
 static JSONObject safe(JSONObject value)throws Exception{
  JSONArray rows=new JSONArray(),input=value.optJSONArray("candidates");if(input!=null)for(int i=0;i<Math.min(2,input.length());i++){JSONObject row=input.optJSONObject(i);if(row==null)continue;String text=row.optString("text");rows.put(new JSONObject().put("text",text.substring(0,Math.min(100,text.length()))).put("at",row.optLong("at")));}
  return new JSONObject().put("at",value.optLong("at")).put("state",rows.length()>1?"AMBIGUOUS":rows.length()==1?"CANDIDATE":"NONE").put("candidates",rows);
 }
 static String label(JSONObject value){
  if(value==null)return "";JSONArray rows=value.optJSONArray("candidates");if(rows==null||rows.length()==0)return "";StringBuilder text=new StringBuilder(rows.length()>1?"参照候補が複数あります（未確定）":"参照候補（未確定）");for(int i=0;i<Math.min(2,rows.length());i++)text.append("\n・").append(rows.optJSONObject(i).optString("text"));return text.append("\n違う場合は対象の名前を添えて訂正してください。予定登録などの同意には使いません。").toString();
 }
 private ConversationReferentPreview(){}
}
