package com.example.rokidkeyboardbridge;
import org.json.*;
/** Finish status is authoritative; nonempty partial text is not proof of completion. */
final class ImageAnswerResponse {
 static final int INITIAL_TOKENS=4096,RETRY_TOKENS=8192;
 static String visibleText(JSONObject candidate)throws JSONException {
  JSONObject content=candidate==null?null:candidate.optJSONObject("content");JSONArray parts=content==null?null:content.optJSONArray("parts");
  StringBuilder text=new StringBuilder();
  if(parts!=null)for(int i=0;i<parts.length();i++){JSONObject part=parts.optJSONObject(i);if(part==null||part.optBoolean("thought"))continue;
   String value=part.optString("text","");if(value.isEmpty())continue;if(text.length()>0)text.append('\n');text.append(value);}
  return text.toString();
 }
 static boolean complete(String reason,String text){return "STOP".equals(reason)&&text!=null&&!text.trim().isEmpty();}
 static boolean retry(String reason,boolean retried){return !retried&&"MAX_TOKENS".equals(reason);}
 static final class Incomplete extends Exception {
  final String partial,reason;
  Incomplete(String reason,String partial){super("Image answer incomplete");this.reason=reason;this.partial=partial==null?"":partial;}
  String display(){String message="画像解析の回答を最後まで取得できませんでした。以下は途中の結果です。内容を確定情報として扱わず、必要な項目を指定してもう一度質問してください。";
   return partial.isEmpty()?"画像解析の回答を最後まで取得できませんでした。必要な項目を指定して、画像をもう一度送ってください。":message+"\n\n"+partial;}
 }
}
