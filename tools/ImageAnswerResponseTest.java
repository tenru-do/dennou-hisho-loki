package com.example.rokidkeyboardbridge;
import org.json.*;
public final class ImageAnswerResponseTest {
 static int checks;static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
 static JSONObject response(String reason,JSONArray parts)throws Exception{return new JSONObject().put("finishReason",reason).put("content",new JSONObject().put("parts",parts));}
 public static void main(String[] args)throws Exception {
  JSONObject partial=response("MAX_TOKENS",new JSONArray().put(new JSONObject().put("thought",true).put("text","private reasoning"))
   .put(new JSONObject().put("text","予約日は10月")));
  String text=ImageAnswerResponse.visibleText(partial);
  check(text.equals("予約日は10月"),"thoughts never displayed or spoken");
  check(!ImageAnswerResponse.complete("MAX_TOKENS",text),"nonempty truncated answer not complete");
  check(ImageAnswerResponse.retry("MAX_TOKENS",false),"token truncation retries once");
  check(!ImageAnswerResponse.retry("MAX_TOKENS",true),"bounded regeneration");
  for(String reason:new String[]{"SAFETY","RECITATION","OTHER","CONTINUATION",""}){
   check(!ImageAnswerResponse.retry(reason,false),"no bypass/retry for "+reason);
   check(!ImageAnswerResponse.complete(reason,text),"no false completion for "+reason);
  }
  check(!ImageAnswerResponse.complete("STOP"," "),"empty STOP is not a usable answer");
  check(ImageAnswerResponse.complete("STOP","読み取れた予約日時です。"),"only normal nonempty finish succeeds");
  check(ImageAnswerResponse.visibleText(null).isEmpty(),"missing candidate handled");
  check(ImageAnswerResponse.visibleText(response("STOP",new JSONArray().put(new JSONObject().put("text","一行目")).put(new JSONObject().put("text","二行目")))).equals("一行目\n二行目"),"multiple visible parts retained");
  check(new ImageAnswerResponse.Incomplete("MAX_TOKENS",text).display().contains(text),"partial result retained with explicit warning");
  check(new ImageAnswerResponse.Incomplete("EMPTY","").display().contains("もう一度"),"no silent empty failure");
  System.out.println("ImageAnswerResponse PASS checks="+checks);
 }
}
