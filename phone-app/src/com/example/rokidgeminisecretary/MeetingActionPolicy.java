package com.example.rokidgeminisecretary;
import org.json.*;
/** Local candidates, never inferred dates, assignees, or calendar mutations. */
final class MeetingActionPolicy {
 static JSONArray candidates(String summary)throws Exception{JSONArray out=new JSONArray();java.util.HashSet<String> seen=new java.util.HashSet<>();String section="";for(String raw:summary.split("[\\r\\n]+")){String line=raw.trim();if(line.isEmpty())continue;if(line.length()<60&&(line.endsWith(":" )||line.endsWith("：")||line.startsWith("#")||line.startsWith("**")&&line.endsWith("**")))section=line;boolean relevant=line.matches(".*(?:TODO|ToDo|タスク|次の作業|次のアクション|確認する|要確認|確認事項|未決|期限|担当|宿題|検討事項|アクションアイテム).*" )||section.matches(".*(?:TODO|タスク|未決|次の作業|アクション).*" );if(!relevant||line.equals(section)||!seen.add(line))continue;if(line.length()>800)line=line.substring(0,800);out.put(new JSONObject().put("index",out.length()).put("text",line).put("section",section));if(out.length()==12)break;}return out;}
 private MeetingActionPolicy(){}
}
