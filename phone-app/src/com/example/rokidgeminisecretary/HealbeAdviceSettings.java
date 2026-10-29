package com.example.rokidgeminisecretary;
import android.app.*;import android.content.*;import android.provider.Settings;import org.json.*;
final class HealbeAdviceSettings {
 static void show(Activity c){
  JSONObject energy=HealbeSdkReader.preferred(c,HealbeEnergyStore.snapshot(c));long at=energy.optLong("observedAt");
  String status=at>0?"最後の取得："+new java.text.SimpleDateFormat("M/d H:mm",java.util.Locale.JAPAN).format(new java.util.Date(at))+"\n収支："+energy.optInt("balanceKcal")+" kcal（暫定）":"エネルギー収支は未取得です。";
  android.widget.LinearLayout box=new android.widget.LinearLayout(c);box.setOrientation(1);int pad=(int)(16*c.getResources().getDisplayMetrics().density);box.setPadding(pad,pad,pad,pad);android.widget.TextView text=new android.widget.TextView(c);
  text.setText(status+"\n\n画面を開かない取得は、公式SDKの補助アプリへ一度ログインして試せます。ログイン後に実データが取得できるか確認します。画面読み取りはHEALBEの今日の画面を表示したときだけの補助です。\n\n歩数と収支をグラスへ表示し、条件が合えば最短1時間おきに運動の声かけをします。Gemini連動オン時は新しい数値・取得時刻をGoogle Geminiへ送ります。元の画面や通知文は送りません。");box.addView(text);
  android.widget.Button direct=new android.widget.Button(c);direct.setText("HEALBEへ直接ログイン（取得を試す）");direct.setOnClickListener(v->HealbeSdkReader.open(c));box.addView(direct);
  android.widget.Button screen=new android.widget.Button(c);screen.setText("画面読み取りの許可設定");screen.setOnClickListener(v->c.startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));box.addView(screen);
  android.widget.Button gemini=new android.widget.Button(c);gemini.setText("Geminiとの会話連動");gemini.setOnClickListener(v->gemini(c));box.addView(gemini);android.widget.ScrollView scroll=new android.widget.ScrollView(c);scroll.addView(box);
  new AlertDialog.Builder(c).setTitle("歩数・HEALBEと運動の声かけ").setView(scroll).setNegativeButton("閉じる",null).show();
 }
 static void gemini(Activity c){
  android.widget.LinearLayout box=new android.widget.LinearLayout(c);box.setOrientation(1);int pad=(int)(16*c.getResources().getDisplayMetrics().density);box.setPadding(pad,pad,pad,pad);
  android.widget.TextView text=new android.widget.TextView(c);text.setText("関連する会話で、新しい歩数・HEALBEの暫定収支・取得時刻をGoogle Geminiへ送ります。元の画面や通知文は送りません。自発的な声かけの追加API利用は最短1時間おきです。設定は次のグラスとの同期で反映します。\n現在："+(HealthGeminiSettings.mode(c)==2?"会話＋声かけ":HealthGeminiSettings.mode(c)==1?"会話のみ":"オフ"));box.addView(text);
  AlertDialog dialog=new AlertDialog.Builder(c).setTitle("Geminiとの健康データ連動").setView(box).setNegativeButton("閉じる",null).create();
  String[] labels={"会話＋自発的な声かけをオン","会話への反映のみ","Gemini連動をオフ","保存した収支を消す"};int[] modes={2,1,0,-1};
  for(int i=0;i<labels.length;i++){final int mode=modes[i];android.widget.Button button=new android.widget.Button(c);button.setText(labels[i]);button.setOnClickListener(v->{try{if(mode<0)ReviewStore.write(c,"healbe-energy",new JSONObject());else HealthGeminiSettings.save(c,new JSONObject().put("mode",mode));dialog.dismiss();show(c);}catch(Exception failed){new AlertDialog.Builder(c).setMessage("設定を保存できませんでした。もう一度お試しください。").setPositiveButton("閉じる",null).show();}});box.addView(button);}dialog.show();
 }
}
