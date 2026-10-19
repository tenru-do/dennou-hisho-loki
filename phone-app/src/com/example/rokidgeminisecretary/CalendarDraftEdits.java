package com.example.rokidgeminisecretary;
import org.json.*;import java.time.*;
/** Manual edits to an existing candidate only. This class never registers an event. */
final class CalendarDraftEdits {
 static JSONObject apply(JSONObject original,JSONObject edit)throws Exception{
  JSONObject next=new JSONObject(original.toString());String name=edit.getString("calendar_name").trim();if(name.isEmpty()||name.length()>120)throw new IllegalArgumentException("登録先を確認してください");JSONArray before=original.getJSONArray("events"),events=edit.getJSONArray("events");if(events.length()!=before.length()||events.length()<1||events.length()>10)throw new IllegalArgumentException("予定の件数が変わっています");JSONArray out=new JSONArray();
  for(int i=0;i<events.length();i++){JSONObject patch=events.getJSONObject(i),event=new JSONObject(before.getJSONObject(i).toString());String title=patch.getString("title").trim(),start=patch.getString("start").trim(),end=patch.getString("end").trim(),location=patch.optString("location");boolean day=patch.getBoolean("all_day");if(title.isEmpty()||title.length()>200||location.length()>500)throw new IllegalArgumentException("予定名・場所を確認してください");long a,b;if(day){a=LocalDate.parse(start).toEpochDay()*86400000L;b=LocalDate.parse(end).toEpochDay()*86400000L;}else{a=OffsetDateTime.parse(start).toInstant().toEpochMilli();b=OffsetDateTime.parse(end).toInstant().toEpochMilli();}if(b<=a||b-a>366L*86400000)throw new IllegalArgumentException("開始・終了日時を確認してください");event.put("title",title).put("start",start).put("end",end).put("all_day",day).put("location",location);out.put(event);}
  return next.put("calendar_name",name).put("events",out);
 }
 static JSONObject publicCandidate(JSONObject original)throws Exception{JSONObject clean=new JSONObject().put("calendar_name",original.optString("calendar_name"));JSONArray events=new JSONArray(),source=original.optJSONArray("events");if(source!=null)for(int i=0;i<Math.min(10,source.length());i++){JSONObject e=source.getJSONObject(i),out=new JSONObject();for(String key:new String[]{"title","start","end","location"})out.put(key,e.optString(key));out.put("all_day",e.optBoolean("all_day"));events.put(out);}return clean.put("events",events);}
 private CalendarDraftEdits(){}
}
