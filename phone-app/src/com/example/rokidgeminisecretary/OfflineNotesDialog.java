package com.example.rokidgeminisecretary;
import android.app.*;
import android.widget.*;
import org.json.*;
/** Restoring a note only fills a draft; sending is a separate user action. */
final class OfflineNotesDialog {
 static void show(Activity owner,java.util.function.Supplier<String> currentDraft,java.util.function.BooleanSupplier attachments,java.util.function.BooleanSupplier busy,java.util.function.Consumer<String> restore){
  final String draft=currentDraft.get();
  LinearLayout body=new LinearLayout(owner);body.setOrientation(1);body.setPadding(20,12,20,12);
  TextView help=new TextView(owner);help.setText("この端末だけに暗号化保存します。再接続しても自動送信しません。画像・動画は保存しません。録音と保留案件は各一覧から確認してください。");body.addView(help);
  Button save=new Button(owner);save.setText("現在の入力を端末内メモに保存");body.addView(save);
  save.setOnClickListener(v->{try{String blocked=DraftRestorePolicy.reason(draft,currentDraft.get(),attachments.getAsBoolean(),busy.getAsBoolean());if(!blocked.isEmpty())throw new java.io.IOException(blocked);OfflineNotes.add(owner,draft);Toast.makeText(owner,"本文を保存しました。送信していません",1).show();save.setEnabled(false);}catch(Exception e){Toast.makeText(owner,"保存できません："+e.getMessage(),1).show();}});
  try{JSONArray rows=OfflineNotes.read(owner);for(int i=rows.length()-1;i>=0;i--){JSONObject row=rows.getJSONObject(i);String text=row.getString("text"),id=row.getString("id");Button note=new Button(owner);note.setAllCaps(false);note.setText(new java.text.SimpleDateFormat("M/d HH:mm",java.util.Locale.JAPAN).format(new java.util.Date(row.getLong("at")))+"  "+text.substring(0,Math.min(70,text.length())));body.addView(note);note.setOnClickListener(v->new AlertDialog.Builder(owner).setTitle("端末内メモ（未送信）").setMessage(text).setPositiveButton("入力欄へ戻す",(d,w)->{final String expected=currentDraft.get();new AlertDialog.Builder(owner).setMessage("現在の入力をこのメモに置き換えます。送信はしません。").setPositiveButton("置き換える",(a,b)->{String blocked=DraftRestorePolicy.reason(expected,currentDraft.get(),attachments.getAsBoolean(),busy.getAsBoolean());if(!blocked.isEmpty()){Toast.makeText(owner,blocked,1).show();return;}restore.accept(text);}).setNegativeButton("戻る",null).show();}).setNeutralButton("削除",(d,w)->new AlertDialog.Builder(owner).setMessage("この端末内のメモを削除しますか？").setPositiveButton("削除",(a,b)->{try{OfflineNotes.remove(owner,id);note.setVisibility(android.view.View.GONE);}catch(Exception e){Toast.makeText(owner,"削除できませんでした",1).show();}}).setNegativeButton("戻る",null).show()).setNegativeButton("閉じる",null).show());}}
  catch(Exception corrupt){TextView error=new TextView(owner);error.setText("保存済みメモを復号できません。既存データは上書きしていません。");body.addView(error);save.setEnabled(false);}
  ScrollView scroll=new ScrollView(owner);scroll.addView(body);new AlertDialog.Builder(owner).setTitle("端末内メモ・通信待ち").setView(scroll).setPositiveButton("閉じる",null).show();
 }
 private OfflineNotesDialog(){}
}
