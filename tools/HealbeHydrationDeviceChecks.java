package com.example.rokidgeminisecretary;
import android.content.*;import org.json.*;import java.util.*;
public final class HealbeHydrationDeviceChecks {
 static int n;static void ok(boolean b){n++;if(!b)throw new AssertionError("HEALBE "+n);}
 public static int run(Context c)throws Exception{n=0;String pkg="com.healbe.healbegobe";
  for(String s:new String[]{"水分補給してください","水を飲みましょう","水分が不足しています","Drink water","Time to drink","low hydration"})ok(HealbeHydrationAlert.matches(pkg,s));
  for(String s:new String[]{"同期中","エネルギーバランス +100 kcal","hydration normal","電池残量"})ok(!HealbeHydrationAlert.matches(pkg,s));
  ok(!HealbeHydrationAlert.matches("other.app","水分補給"));
  long now=System.currentTimeMillis();ok(!HealbeHydrationAlert.fresh(now+1,now));ok(!HealbeHydrationAlert.fresh(now-300001,now));ok(HealbeHydrationAlert.fresh(now-300000,now));
  SharedPreferences p=c.getSharedPreferences("phone_secretary",0),h=c.getSharedPreferences("healbe_alert_delivery",0);p.edit().remove("pending_command_queue").remove("pending_command").commit();h.edit().clear().commit();
  ok(MainActivity.queueBridgeCommand(c,"動作確認の質問",true));
  HealbeHydrationAlert.forward(c,now,pkg,"水分補給テスト","実際の警告ではありません。");
  JSONArray q=new JSONArray(p.getString("pending_command_queue","[]"));ok(q.length()==2);ok(q.getString(0).equals("動作確認の質問"));String id=CodexNoticeQueue.id(q.getString(1));ok(id.matches("[a-f0-9]{64}"));
  HealbeHydrationAlert.forward(c,now+1,pkg,"水分補給テスト","実際の警告ではありません。");ok(new JSONArray(p.getString("pending_command_queue","[]")).length()==2);
  HealbeHydrationAlert.forward(c,now-600000,pkg,"水分補給テスト","古い記録");ok(new JSONArray(p.getString("pending_command_queue","[]")).length()==2);
  ArrayList<String> list=new ArrayList<>();list.add(q.getString(0));list.add(q.getString(1));ok(CodexNoticeQueue.removeMatching(list,id));ok(list.size()==1&&list.get(0).equals("動作確認の質問"));
  String codex="__CODEX_NOTIFY__:"+new JSONObject().put("id",id).put("kind","completed").put("issuedAt",now).put("message","合成通知");ok(CodexNoticeQueue.id(codex).equals(id));
  p.edit().remove("pending_command_queue").remove("pending_command").commit();h.edit().clear().commit();return n;
 }
}
