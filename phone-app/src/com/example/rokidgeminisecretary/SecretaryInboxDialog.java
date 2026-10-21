package com.example.rokidgeminisecretary;
import android.app.AlertDialog;
import android.widget.Toast;
import org.json.*;
final class SecretaryInboxDialog {
 private final MainActivity owner;
 SecretaryInboxDialog(MainActivity owner){this.owner=owner;}
 void show(){show(false);}
 private void show(boolean includeClosed){new Thread(()->{try{JSONArray source=SecretaryInbox.list(owner).getJSONArray("items"),all=new JSONArray();for(int i=0;i<source.length();i++){JSONObject item=source.getJSONObject(i);if(includeClosed||SecretaryInboxPresentation.active(item))all.put(item);}String[] labels=new String[all.length()];
  for(int i=0;i<all.length();i++){JSONObject j=all.getJSONObject(i);labels[i]="["+j.optString("stateLabel","要確認")+"] "+j.optString("title")+(j.optString("date_text").isEmpty()?"":"\n日程："+j.optString("date_text"));}
  owner.runOnUiThread(()->new AlertDialog.Builder(owner).setTitle((includeClosed?"全案件":"未完了・結果要確認")+"（"+labels.length+"件）").setItems(labels,(d,n)->open(all.optJSONObject(n).optString("id"))).setPositiveButton(includeClosed?"未完了へ":"すべて",(d,n)->show(!includeClosed)).setNegativeButton("閉じる",null).show());
 }catch(Exception e){error("受信箱を読み込めませんでした。元データは保持しています。");}},"LokiInboxList").start();}
 void open(String id){new Thread(()->{try{JSONObject j=SecretaryInbox.get(owner,id).getJSONObject("item");
  owner.runOnUiThread(()->{AlertDialog.Builder b=new AlertDialog.Builder(owner).setTitle("引き継ぎカード")
   .setMessage(SecretaryInbox.display(j));
   b.setNegativeButton("閉じる",null).setNeutralButton("整理",(d,n)->new AlertDialog.Builder(owner).setTitle("案件の整理").setItems("pending".equals(j.optString("state"))?new String[]{"保留を取り消す","この案件を削除","手動で完了にする"}:new String[]{"この案件を削除"},(q,k)->new AlertDialog.Builder(owner).setTitle(k==2?"予定登録とは別に手動完了にしますか？":k==0&&"pending".equals(j.optString("state"))?"保留を取り消しますか？":"案件を削除しますか？").setMessage("登録済みのカレンダー予定は削除しません。").setNegativeButton("やめる",null).setPositiveButton("実行",(v,z)->change(j,k==2?"complete_manual":k==0&&"pending".equals(j.optString("state"))?"cancel":"delete")).show()).show());
   if("pending".equals(j.optString("state")))b.setPositiveButton("会話欄で確認する",(d,n)->owner.composeInboxResume(id));
   b.show();});
 }catch(Exception e){error("案件を開けませんでした。");}},"LokiInboxRead").start();}
 private void change(JSONObject j,String action){new Thread(()->{try{SecretaryInbox.action(owner,new JSONObject().put("id",j.getString("id")).put("revision",j.getInt("revision")).put("action",action));owner.runOnUiThread(()->{owner.refreshInboxSummaryNow();show();});}catch(Exception e){error(e.getMessage());}},"LokiInboxChange").start();}
 private void error(String message){owner.runOnUiThread(()->Toast.makeText(owner,message,Toast.LENGTH_LONG).show());}
}
