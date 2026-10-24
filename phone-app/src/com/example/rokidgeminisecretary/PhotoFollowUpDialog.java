package com.example.rokidgeminisecretary;
import android.app.*;import android.widget.*;import org.json.*;import java.text.DateFormat;import java.util.Date;
/** Select a source first; preview in the existing composer, never auto-send. */
final class PhotoFollowUpDialog {
 interface Selection{void use(String id);}
 static void show(Activity host,Selection selection){
  TextView status=new TextView(host);status.setPadding(24,20,24,20);status.setText("写真付きの送信控えを確認中…");
  LinearLayout body=new LinearLayout(host);body.setOrientation(1);body.addView(status);ScrollView scroll=new ScrollView(host);scroll.addView(body);
  AlertDialog dialog=new AlertDialog.Builder(host).setTitle("送った写真に追加質問").setView(scroll).setNegativeButton("閉じる",null).create();dialog.show();
  new Thread(()->{try{JSONArray rows=PhotoFollowUp.list(host);host.runOnUiThread(()->{if(!dialog.isShowing()||host.isDestroyed())return;
   status.setText(rows.length()==0?"使える写真の控えはありません。写真を添付して一度送信してください。控えは24時間・直近10件です。":"写真を選ぶと、元の質問ではなく今の入力を残して添付します。まだ送信しません。控えは24時間・直近10件です。");
   for(int i=0;i<rows.length();i++){JSONObject row=rows.optJSONObject(i);Button use=new Button(host);use.setAllCaps(false);use.setText(DateFormat.getDateTimeInstance().format(new Date(row.optLong("at")))+" ／ 写真"+row.optInt("imageCount")+"件\n"+row.optString("message"));use.setOnClickListener(v->{dialog.dismiss();selection.use(row.optString("id"));});body.addView(use);}
  });}catch(Exception e){host.runOnUiThread(()->{if(dialog.isShowing())status.setText("控えを開けません。写真を選び直してください");});}},"LokiPhotoFollowUpList").start();
 }
 private PhotoFollowUpDialog(){}
}
