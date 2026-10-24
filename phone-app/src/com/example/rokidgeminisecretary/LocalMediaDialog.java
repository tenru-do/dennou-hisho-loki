package com.example.rokidgeminisecretary;
import android.app.*;import android.content.*;import android.provider.Settings;import org.json.*;
/** Also available on Android tablets running the phone application. */
final class LocalMediaDialog {
 static void show(Activity owner){new Thread(()->{try{JSONObject result=LocalMediaControl.list(owner);owner.runOnUiThread(()->{
  if("permission".equals(result.optString("status"))){new AlertDialog.Builder(owner).setTitle("音楽・ラジオ操作").setMessage(result.optString("message")).setPositiveButton("通知へのアクセス設定",(d,w)->{try{owner.startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));}catch(Exception e){}}).setNegativeButton("閉じる",null).show();return;}
  JSONArray sessions=result.optJSONArray("sessions");if(sessions==null||sessions.length()==0){new AlertDialog.Builder(owner).setTitle("この端末の音楽・ラジオ").setMessage(result.optString("message")).setPositiveButton("更新",(d,w)->show(owner)).setNegativeButton("閉じる",null).show();return;}
  String[] labels=new String[sessions.length()];for(int i=0;i<labels.length;i++){JSONObject s=sessions.optJSONObject(i);labels[i]=s.optString("app")+(s.optBoolean("seek")?"":"（30秒操作非対応）");}
  new AlertDialog.Builder(owner).setTitle("この端末の再生アプリ").setItems(labels,(d,w)->controls(owner,sessions.optJSONObject(w))).setNegativeButton("閉じる",null).show();
 });}catch(Exception e){owner.runOnUiThread(()->new AlertDialog.Builder(owner).setMessage("再生状態を取得できません。設定は変更していません。").setPositiveButton("閉じる",null).show());}},"media-list").start();}
 static void controls(Activity owner,JSONObject session){if(!session.optBoolean("seek")){new AlertDialog.Builder(owner).setTitle(session.optString("app")).setMessage(session.optString("reason")).setPositiveButton("戻る",(d,w)->show(owner)).show();return;}
  new AlertDialog.Builder(owner).setTitle(session.optString("app")+"・この端末").setItems(new String[]{"30秒戻す","30秒先へ"},(d,w)->{
   new Thread(()->{String message;try{JSONObject request=new JSONObject().put("requestId",java.util.UUID.randomUUID().toString()).put("session",session.getString("id")).put("delta",w==0?-30000:30000).put("createdAt",System.currentTimeMillis());message=LocalMediaControl.execute(owner,request).optString("message");}catch(Exception e){message="操作結果を確認できません。";}final String text=message;owner.runOnUiThread(()->new AlertDialog.Builder(owner).setTitle("音楽・ラジオ操作").setMessage(text).setPositiveButton("続ける",(x,y)->show(owner)).setNegativeButton("閉じる",null).show());},"media-seek").start();
  }).setNegativeButton("戻る",(d,w)->show(owner)).show();
 }
 private LocalMediaDialog(){}
}
