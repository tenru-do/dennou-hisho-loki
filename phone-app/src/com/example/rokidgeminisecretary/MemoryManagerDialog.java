package com.example.rokidgeminisecretary;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Handler;
import android.view.View;
import android.widget.*;
import android.text.InputFilter;
import org.json.*;

/** Dedicated, scrollable manager for the glass-authoritative dynamic memory store. */
public final class MemoryManagerDialog {
    private final Activity activity;
    private final DynamicMemorySync sync;
    private final Handler handler = new Handler();
    private LinearLayout list;
    private TextView status;
    private String previous = "";
    public MemoryManagerDialog(Activity activity) { this.activity=activity; sync=new DynamicMemorySync(activity); }
    public void show() {
        LinearLayout panel=new LinearLayout(activity); panel.setOrientation(LinearLayout.VERTICAL);
        int pad=(int)(16*activity.getResources().getDisplayMetrics().density); panel.setPadding(pad,pad,pad,pad);
        panel.setMinimumHeight((int)(activity.getResources().getDisplayMetrics().heightPixels*0.60));
        status=new TextView(activity); panel.addView(status);
        Button refresh=new Button(activity); refresh.setText("グラスから再取得 / 同期を確認");
        refresh.setOnClickListener(v -> { try { sync.refresh(); render(); } catch(Exception e) { error(e); } }); panel.addView(refresh);
        ScrollView scroll=new ScrollView(activity); list=new LinearLayout(activity); list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list); panel.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        AlertDialog dialog=new AlertDialog.Builder(activity).setTitle("記憶マネージャー（メモ一覧）")
                .setView(panel).setPositiveButton("閉じる",null).create();
        Runnable tick=new Runnable() {
            long nextRefresh=android.os.SystemClock.elapsedRealtime()+15000;
            public void run() {
                if(activity.isDestroyed()) return;
                if(android.os.SystemClock.elapsedRealtime()>=nextRefresh) {
                    try { JSONObject state=sync.state(); if(!state.getBoolean("pending") && state.getString("requestId").isEmpty()) sync.refresh(); }
                    catch(Exception e) { error(e); }
                    nextRefresh=android.os.SystemClock.elapsedRealtime()+15000;
                }
                render(); handler.postDelayed(this,1000);
            }
        };
        dialog.setOnDismissListener(d -> handler.removeCallbacksAndMessages(null));
        dialog.show(); dialog.getWindow().setLayout(-1,(int)(activity.getResources().getDisplayMetrics().heightPixels*0.85));
        try { sync.refresh(); } catch(Exception e) { error(e); }
        tick.run();
    }
    private void render() {
        if(activity.isDestroyed()) { handler.removeCallbacksAndMessages(null); return; }
        try {
            JSONObject state=sync.state(); String serialized=state.toString(); if(serialized.equals(previous)) return; previous=serialized;
            long at=state.getLong("receivedAt"); boolean pending=state.getBoolean("pending");
            status.setText(state.getString("message") + (state.getString("requestId").isEmpty()? "" : "\n取得要求中（未接続時は待機）")
                    + (pending? "\n編集・削除の応答待ち" : "")
                    + (at==0? "" : "\n最終取得: " + android.text.format.DateFormat.format("yyyy-MM-dd HH:mm:ss",at)));
            list.removeAllViews(); JSONArray entries=state.getJSONArray("entries");
            if(entries.length()==0) { TextView empty=new TextView(activity); empty.setText(at==0? "取得後にメモを表示します" : "保存されているメモはありません"); list.addView(empty); }
            for(int i=0;i<entries.length();i++) {
                JSONObject item=entries.getJSONObject(i);
                TextView text=new TextView(activity); text.setText(item.getString("topic")+"\nID: "+item.getString("id")
                        +"\n日時: "+item.getString("timestamp")+"\n"+item.getString("content")); text.setTextIsSelectable(true); text.setPadding(0,16,0,4); list.addView(text);
                LinearLayout actions=new LinearLayout(activity);
                Button edit=new Button(activity); edit.setText("編集"); edit.setEnabled(!pending); edit.setOnClickListener(v -> edit(item));
                Button delete=new Button(activity); delete.setText("削除"); delete.setEnabled(!pending);
                delete.setOnClickListener(v -> new AlertDialog.Builder(activity).setTitle("このメモを削除しますか？")
                        .setMessage(item.optString("topic")+"\nグラスの記憶からも削除します。")
                        .setNegativeButton("キャンセル",null).setPositiveButton("削除",(d,w) -> queue(item,"delete","","")).show());
                actions.addView(edit,new LinearLayout.LayoutParams(0,-2,1)); actions.addView(delete,new LinearLayout.LayoutParams(0,-2,1)); list.addView(actions);
            }
        } catch(Exception e) { error(e); }
    }
    private void edit(JSONObject item) {
        LinearLayout panel=new LinearLayout(activity); panel.setOrientation(LinearLayout.VERTICAL);
        EditText topic=new EditText(activity); topic.setHint("件名"); topic.setSingleLine(); topic.setFilters(new InputFilter[]{new InputFilter.LengthFilter(200)}); topic.setText(item.optString("topic")); panel.addView(topic);
        EditText content=new EditText(activity); content.setHint("メモ本文"); content.setMinLines(3); content.setMaxLines(8); content.setFilters(new InputFilter[]{new InputFilter.LengthFilter(12000)}); content.setText(item.optString("content")); panel.addView(content);
        AlertDialog dialog=new AlertDialog.Builder(activity).setTitle("メモを編集").setView(panel).setNegativeButton("キャンセル",null).setPositiveButton("保存して同期",null).create();
        dialog.setOnShowListener(d -> dialog.getButton(-1).setOnClickListener(v -> {
            try { sync.queue(item,"edit",topic.getText().toString(),content.getText().toString()); dialog.dismiss(); render(); }
            catch(Exception e) { content.setError(e.getMessage()); }
        })); dialog.show();
    }
    private void queue(JSONObject item,String action,String topic,String content) {
        try { sync.queue(item,action,topic,content); render(); } catch(Exception e) { error(e); }
    }
    private void error(Exception e) { Toast.makeText(activity,e.getMessage(),Toast.LENGTH_LONG).show(); }
}