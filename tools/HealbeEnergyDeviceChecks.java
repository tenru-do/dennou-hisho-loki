package com.example.rokidgeminisecretary;
import android.content.*;import org.json.*;
public final class HealbeEnergyDeviceChecks {
 static int n;static void ok(boolean b){if(!b)throw new AssertionError("energy parse/store "+n);n++;}
 public static int run(Context c)throws Exception{n=0;
  for(String s:new String[]{"今日\nエネルギーバランス\n+550\nkcal","Energy balance: +550 kcal","エネルギーバランス ＋550 kcal"})ok(HealbeEnergyText.balance(s)==550);
  ok(HealbeEnergyText.balance("エネルギーバランス -1,250 kcal")==-1250);
  ok(HealbeEnergyText.balance("エネルギーバランス 0 kcal")==0);
  ok(HealbeEnergyText.balance("エネルギーバランス\n1,234 歩、0.1 km\n+550\nkcal")==550);
  ok(HealbeEnergyText.todayScreen("今日, 木曜日, 10/29\nエネルギーバランス +550 kcal"));
  ok(HealbeEnergyText.balance("エネルギーバランス\n摂取カロリー\n550 kcal")==null);
  for(String s:new String[]{"摂取カロリー 550 kcal","Active calories 550 kcal","エネルギーバランス +550 steps","エネルギーバランス +99999 kcal","エネルギーバランス +500 kcal\nエネルギーバランス -200 kcal","エネルギーバランス\n摂取\n550 kcal"})ok(HealbeEnergyText.balance(s)==null);
  ok(HealbeEnergyText.todayScreen("今日\nエネルギーバランス +500 kcal"));ok(!HealbeEnergyText.todayScreen("昨日\nエネルギーバランス +500 kcal"));ok(!HealbeEnergyText.todayScreen("エネルギーバランス +500 kcal"));
  ReviewStore.write(c,"healbe-energy",new JSONObject());long now=System.currentTimeMillis();
  HealbeEnergyStore.observe(c,"今日\nエネルギーバランス +550 kcal","HEALBE_SCREEN",now);
  JSONObject saved=HealbeEnergyStore.snapshot(c);ok(saved.optInt("balanceKcal")==550);ok(saved.optLong("observedAt")==now);ok(saved.optBoolean("provisional"));
  for(String source:new String[]{"manual","ACTIVE_CALORIES","STALE_HEALBE"})HealbeEnergyStore.observe(c,"今日\nエネルギーバランス +1000 kcal",source,now);
  HealbeEnergyStore.observe(c,"昨日\nエネルギーバランス +1000 kcal","HEALBE_NOTIFICATION",now);
  HealbeEnergyStore.observe(c,"今日\nエネルギーバランス +1000 kcal","HEALBE_SCREEN",now-300001);
  ok(HealbeEnergyStore.snapshot(c).optInt("balanceKcal")==550);
  HealbeEnergyStore.observe(c,"エネルギーバランス +700 kcal","HEALBE_NOTIFICATION",now);ok(HealbeEnergyStore.snapshot(c).optInt("balanceKcal")==700);
  ReviewStore.write(c,"healbe-energy",new JSONObject());return n;
 }
}
