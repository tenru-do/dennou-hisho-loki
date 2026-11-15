package com.example.rokidgeminisecretary;
import android.content.*;import org.json.*;import java.io.*;import java.util.*;
final class ReconnectReview {
 static long last=-1;static boolean ever=false;
 static final long RECENT_MS=120000;
 static SharedPreferences prefs(Context c){return c.getSharedPreferences("loki-reconnect-review",0);}
 static boolean gap(long before,long now){return before<0||now<before||now-before>=15000;}
 static boolean recent(long created,long now){return created>0&&now>=created&&now-created<RECENT_MS;}
 static HashSet<String> ids(JSONArray rows)throws Exception{HashSet<String> out=new HashSet<>();for(int i=0;i<rows.length();i++)out.add(rows.getString(i));return out;}
 static JSONArray array(Set<String> ids){JSONArray out=new JSONArray();for(String id:ids)out.put(id);return out;}
 static void contact(Context c,long now,JSONArray order)throws Exception{synchronized(MainActivity.class){
  boolean returned=gap(last,now);HashSet<String> current=new HashSet<>();for(int i=0;i<order.length();i++)current.add(order.getJSONObject(i).getString("id"));
  HashSet<String> held=ids(new JSONArray(prefs(c).getString("held","[]"))),approved=ids(new JSONArray(prefs(c).getString("approved","[]")));held.retainAll(current);approved.retainAll(current);held.removeAll(approved);
  if(returned){HashMap<String,Long> created=new HashMap<>();JSONArray ledger=new JSONArray(c.getSharedPreferences("loki_web_console",0).getString("outbox","[]"));for(int i=0;i<ledger.length();i++){JSONObject row=ledger.getJSONObject(i);created.put(row.optString("key"),row.optLong("time"));}long wall=System.currentTimeMillis();for(String id:current)if(!approved.contains(id)&&!recent(created.containsKey(id)?created.get(id):0L,wall))held.add(id);}
  String heldJson=array(held).toString(),approvedJson=array(approved).toString();if(returned||!heldJson.equals(prefs(c).getString("held","[]"))||!approvedJson.equals(prefs(c).getString("approved","[]"))){if(!prefs(c).edit().putString("held",heldJson).putString("approved",approvedJson).putLong("returnedAt",returned&&(ever||!held.isEmpty())?System.currentTimeMillis():prefs(c).getLong("returnedAt",0)).commit())throw new IOException("接続復帰の確認待ちを保存できません");}last=now;ever=true;
 }}
 static boolean held(Context c,String id)throws Exception{synchronized(MainActivity.class){return ids(new JSONArray(prefs(c).getString("held","[]"))).contains(id);}}
 // Legacy commands still acknowledge the queue head. Only tracked IDs may pass held rows.
 static int dispatchIndex(Context c,ArrayList<String> queue)throws Exception{return dispatchIndex(c,queue,false);}
 static int dispatchIndex(Context c,ArrayList<String> queue,boolean noticeAck)throws Exception{synchronized(MainActivity.class){for(int i=0;i<queue.size();i++){String wire=queue.get(i);if(noticeAck&&!CodexNoticeQueue.id(wire).isEmpty())return i;if(wire.startsWith(TrackedWebRequest.PREFIX)){if(!held(c,TrackedWebRequest.parse(wire).getString("id")))return i;}else if(i==0)return i;}return -1;}}
 static JSONObject status(Context c)throws Exception{synchronized(MainActivity.class){JSONArray current=MainActivity.queuedWebOrder(c),held=new JSONArray();for(int i=0;i<current.length();i++){String id=current.getJSONObject(i).getString("id");if(held(c,id))held.put(id);}return new JSONObject().put("held",held).put("returnedAt",prefs(c).getLong("returnedAt",0));}}
 static JSONObject approve(Context c,String id)throws Exception{synchronized(LokiWebConsole.class){synchronized(MainActivity.class){if(!TrackedWebRequest.validId(id)||!MainActivity.hasQueuedWebCommand(c,id)||!held(c,id))throw new IOException("対象が受理・取消・変更されています。更新してください");HashSet<String> held=ids(new JSONArray(prefs(c).getString("held","[]"))),approved=ids(new JSONArray(prefs(c).getString("approved","[]")));held.remove(id);approved.add(id);if(!prefs(c).edit().putString("held",array(held).toString()).putString("approved",array(approved).toString()).commit())throw new IOException("確認結果を保存できません");CommandNotice.changed();return new JSONObject().put("ok",true);}}}
 static JSONObject approveBatch(Context c,JSONArray keys)throws Exception{synchronized(LokiWebConsole.class){synchronized(MainActivity.class){
  if(keys.length()<1||keys.length()>100)throw new IOException("1〜100件の送信待ちを選んでください");
  LinkedHashSet<String> selected=new LinkedHashSet<>();JSONArray all=ConversationContinuation.ledger(c);
  for(int i=0;i<keys.length();i++){String id=keys.getString(i);if(!TrackedWebRequest.validId(id)||!selected.add(id))throw new IOException("選択IDを確認してください");JSONObject row=null;for(int j=0;j<all.length();j++)if(id.equals(all.getJSONObject(j).optString("key")))row=all.getJSONObject(j);if(row==null||!row.optBoolean("tracked")||!row.optString("requestState").equals("QUEUED")||!MainActivity.hasQueuedWebCommand(c,id))throw new IOException("選んだ依頼が受信・変更されました。更新して選び直してください");}
  HashSet<String> held=ids(new JSONArray(prefs(c).getString("held","[]"))),approved=ids(new JSONArray(prefs(c).getString("approved","[]")));int resumed=0;for(String id:selected){if(held.remove(id))resumed++;approved.add(id);}
  if(!prefs(c).edit().putString("held",array(held).toString()).putString("approved",array(approved).toString()).commit())throw new IOException("まとめて再開の確認を保存できません");
  CommandNotice.changed();return new JSONObject().put("ok",true).put("selected",selected.size()).put("resumed",resumed).put("newRequests",0);
 }}}
 private ReconnectReview(){}
}
