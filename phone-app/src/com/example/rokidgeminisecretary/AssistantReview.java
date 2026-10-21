package com.example.rokidgeminisecretary;
import org.json.*;
/** Private on-demand review. Facts and unknowns stay separate; never auto-retries. */
final class AssistantReview {
 static JSONObject read(MainActivity c)throws Exception {
  JSONObject state=ConversationWorkspace.snapshot(c),out=new JSONObject();boolean fresh=state.optBoolean("connected");
  StringBuilder diagnosis=new StringBuilder(fresh?"最近受信したグラスの状態":"グラスの現在状態は未確認。以下は最後の受信内容です");
  JSONObject recognition=state.optJSONObject("recognition");
  diagnosis.append("\n\n認識文：").append(recognition==null?"未取得":recognition.optString("text","未取得"));
  if(recognition!=null)diagnosis.append("\n認識元：").append(recognition.optString("source")).append(" / ").append(recognition.optString("phase")).append("\n認識日時：").append(new java.util.Date(recognition.optLong("at")));
  diagnosis.append("\n\n参照設定：").append(state.optString("referenceMode","未取得")).append("\n").append(ConversationReferentPreview.label(state.optJSONObject("referent")));
  JSONObject progress=state.optJSONObject("conversation");if(progress!=null)diagnosis.append("\n回答処理：").append(ConversationDeliveryState.label(progress,fresh));
  JSONObject trace=state.optJSONObject("trace");if(trace!=null&&trace.optLong("at")>0){boolean same=progress!=null&&!trace.optString("request").isEmpty()&&trace.optString("request").equals(progress.optString("id"));diagnosis.append("\n\n最後の通常回答試行（").append(new java.util.Date(trace.optLong("at"))).append(same?"・表示中の依頼IDと一致":"・表示中の依頼との対応は未確認").append("）：").append(trace.optString("phase")).append("\n検索：").append(trace.optBoolean("searchObserved")?"プロバイダー応答に検索の記録あり":"実行記録は未確認（検索していないとは断定しません）").append(" / 検索語数 ").append(trace.optInt("queries")).append(" / 出典数 ").append(trace.optInt("sources")).append("\n試行時の参照設定：").append(trace.optString("reference")).append(" / 組み立て済み文脈 ").append(trace.optInt("contextChars")).append("文字");}
  JSONObject capture=state.optJSONObject("capture");if(capture!=null)diagnosis.append("\n\n").append(fresh?"最近受信した稼働状態：":"最後に受信した稼働状態（現在は未確認）：").append("\n会議録音サービス：").append(capture.optBoolean("meetingService")?"稼働":"停止状態").append(" / KEEP撮影サービス：").append(capture.optBoolean("keepService")?"稼働":"停止状態").append("\n画像生成の結果待ち：").append(capture.optBoolean("imagePending")?"あり":"なし").append(" / 動画生成の結果待ち：").append(capture.optBoolean("videoPending")?"あり":"なし").append("\n最後の負荷判定：").append(capture.optLong("resourceAt")<=0?"未計測":capture.optString("resource")+" / "+new java.util.Date(capture.optLong("resourceAt"))).append("\nサービス稼働は保存の完全性や外部処理の成功を保証しません。");
  diagnosis.append("\n\n実際に回答へ採用された履歴・回答が誤った原因は、この受信状態だけでは確定できません。認識文が違う場合は入力を訂正し、参照先が違う場合は会話の参照設定を確認してください。失敗理由がある場合は依頼・転送の状態を確認してください。自動再送はしません。");
  out.put("diagnosis",diagnosis.toString()).put("operations",OperationOverview.read(c)).put("workspace",state);
  JSONArray cards=PersonalNotes.list(c);out.put("notes",cards);
  JSONArray inbox=SecretaryInbox.list(c).getJSONArray("items"),review=new JSONArray();long now=System.currentTimeMillis();
  for(int i=0;i<inbox.length();i++){JSONObject row=inbox.getJSONObject(i);if(!row.optString("state").equals("pending"))continue;JSONArray similar=new JSONArray();for(int j=0;j<inbox.length();j++){JSONObject other=inbox.getJSONObject(j);if(i!=j&&other.optString("state").equals("pending")&&RecordSearchPolicy.normalized(row.optString("title")).equals(RecordSearchPolicy.normalized(other.optString("title"))))similar.put(other.optString("id"));}row.put("similar",similar).put("old",now-row.optLong("createdAt")>=30L*86400000);review.put(row);}
  return out.put("inboxReview",review).put("continuationDraft",continuation(c));
 }
 static String continuation(MainActivity c){StringBuilder draft=new StringBuilder("話題：\n決定したこと（確認して記入）：\n未解決・次に話すこと：\n\n直近の会話の原文抜粋（自動要約ではありません）：\n");try{JSONArray rows=c.localRecordConversations();java.util.ArrayList<JSONObject> recent=new java.util.ArrayList<>();for(int i=0;i<rows.length();i++)recent.add(rows.getJSONObject(i));recent.sort((a,b)->Long.compare(b.optLong("time"),a.optLong("time")));for(int i=0;i<Math.min(4,recent.size());i++){JSONObject row=recent.get(i);String text=row.optString("message");draft.append(row.optString("kind")).append("：").append(text.length()>350?text.substring(0,350)+"…":text).append("\n");}}catch(Exception e){draft.append("会話記録を取得できませんでした。必要な内容を記入してください。");}return draft.toString();}
 private AssistantReview(){}
}
