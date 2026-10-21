package com.example.rokidgeminisecretary;
import org.json.*;import java.util.*;
/** Pure policies: unknown is not a successful check; no operation starts from a status read. */
final class MaintenancePolicy {
 static String transferReason(boolean connected,boolean charging,boolean wifi,boolean needCharge,boolean needWifi,long now,long expiry){if(now>expiry)return "期限切れです。再予約してください";if(!connected)return "グラスの新しい接続状態を待っています";if(needCharge&&!charging)return "スマホの充電を待っています";if(needWifi&&!wifi)return "スマホのWi-Fi接続を待っています";return "";}
 static String phase(String raw){if(raw.matches("COMPLETED|completed|RETURNED"))return "完了";if(raw.matches("QUEUED|queued|PENDING|pending"))return "待機";if(raw.matches("RUNNING|running|submitting|downloading|saving"))return "処理中";if(raw.matches("STOP_REQUESTED|STOPPED|paused"))return "中止・保留";return "要確認";}
 static String next(String phase){return phase.equals("完了")?"保存した結果を確認":phase.equals("待機")?"接続・実行条件を確認":phase.equals("処理中")?"進捗を更新。再送せず待つ":"詳細を開いて結果・中止状態を確認";}
 static int size(int v){if(v!=16&&v!=20&&v!=24)throw new IllegalArgumentException("文字サイズを選んでください");return v;}
 static boolean selected(JSONArray a,String key){for(int i=0;i<a.length();i++)if(key.equals(a.optString(i)))return true;return false;}
 static JSONArray taskCandidates(String summary)throws Exception{JSONArray out=new JSONArray();HashSet<String> seen=new HashSet<>();String section="";for(String raw:summary.split("[\\r\\n]+")){String line=raw.trim();if(line.isEmpty())continue;if(line.length()<80&&(line.startsWith("#")||line.endsWith("：")||line.endsWith(":")))section=line;boolean relevant=(line+" "+section).matches(".*(?:決定事項|決定した|合意|担当|期限|TODO|タスク|アクション|次の作業|確認する|要確認).*" );if(!relevant||line.equals(section)||!seen.add(line))continue;out.put(new JSONObject().put("index",out.length()).put("text",line.substring(0,Math.min(800,line.length()))).put("section",section));if(out.length()==20)break;}return out;}
 private MaintenancePolicy(){}
}
