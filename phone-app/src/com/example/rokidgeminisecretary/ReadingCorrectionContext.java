package com.example.rokidgeminisecretary;
import android.content.Context;import org.json.*;import java.io.*;
final class ReadingCorrectionContext {
 static JSONObject prepare(Context c,String id,int revision)throws Exception{JSONObject row=ReadingHistory.get(c,id);if(row.getInt("revision")!=revision)throw new IOException("訂正履歴が更新されています。開き直してください");JSONArray changes=row.getJSONArray("corrections");if(changes.length()==0)throw new IOException("訂正履歴がありません");StringBuilder b=new StringBuilder("保存した画像読み取りについて、以下は私が確認した訂正です。原文は推測を含む参照資料です。訂正と区別してください。予定はまだ登録せず、日時と登録先を確認してください。\n元の質問：").append(row.optString("question")).append("\n元の読み取り：").append(row.getString("answer")).append("\n確認した訂正（時系列。矛盾がある場合は質問してください）：");for(int i=0;i<changes.length();i++){JSONObject change=changes.getJSONObject(i);b.append("\n").append(change.getString("field")).append("：").append(change.optString("before")).append(" → ").append(change.getString("after"));}b.append("\nこの訂正を踏まえて：");if(b.length()>2400)throw new IOException("原文と訂正が長いため入力上限を超えます。必要な項目をメモにまとめてください");return new JSONObject().put("text",b.toString()).put("revision",revision).put("id",id);}
 private ReadingCorrectionContext(){}
}
