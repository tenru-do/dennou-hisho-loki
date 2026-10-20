package com.example.rokidgeminisecretary;
import android.app.*;import android.widget.*;import org.json.*;
final class DepartureDialog {
 static JSONObject read(MainActivity owner)throws Exception{JSONObject workspace=ConversationWorkspace.snapshot(owner);return new JSONObject().put("phone",owner.localDeparture()).put("glass",workspace.optJSONObject("departure")==null?new JSONObject():workspace.getJSONObject("departure")).put("connected",workspace.optBoolean("connected")).put("receivedAt",workspace.optLong("receivedAt"));}
 static void show(MainActivity owner){TextView text=new TextView(owner);text.setPadding(20,12,20,12);text.setText("端末内の状態を確認中…");ScrollView scroll=new ScrollView(owner);scroll.addView(text);AlertDialog dialog=new AlertDialog.Builder(owner).setTitle("出発前チェック").setView(scroll).setPositiveButton("閉じる",null).create();dialog.show();new Thread(()->{String result;try{JSONObject d=read(owner);result="スマホ\n"+DeviceReadiness.describe(d.getJSONObject("phone"),true)+"\n\nスマホ↔グラス："+(d.getBoolean("connected")?"最近の受信あり":"未確認・接続を確認してください")+"\n\nグラス\n"+DeviceReadiness.describe(d.getJSONObject("glass"),d.getBoolean("connected"))+"\n電池30%・空き300MBは注意表示の目安です。";}catch(Exception e){result="状態を確認できません。設定・記録は変更していません";}final String value=result;owner.runOnUiThread(()->{if(dialog.isShowing())text.setText(value);});},"DepartureChecklist").start();}
 private DepartureDialog(){}
}
