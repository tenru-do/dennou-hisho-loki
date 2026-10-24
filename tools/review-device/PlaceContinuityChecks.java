package com.example.rokidkeyboardbridge;
import org.json.*;import android.content.*;import android.os.*;import java.lang.reflect.*;import java.util.concurrent.*;
public final class PlaceContinuityChecks {
 static int n;static void ok(boolean b){n++;if(!b)throw new AssertionError("place continuity "+n);}static JSONObject turn(String user,String answer)throws Exception{return new JSONObject().put("time",99900L).put("user",user).put("assistant",answer);}
 public static int run(Context c)throws Exception{n=0;long now=100000,last=99900;
 JSONArray history=new JSONArray().put(turn("ぶんぶく茶釜の舞台って？","群馬県館林市の茂林寺です。\n\n情報元は\n検索の題名は立川駅 https://example.invalid/")).put(turn("栃木じゃなかったっけ？","茂林寺は群馬県館林市です。"));
 ok(ConversationPlaceContinuity.resolve("近くに沼ある？",history,last,now).equals("茂林寺の近くに沼ある？"));
 history.put(turn("近くに沼ある？","立川市の昭和記念公園をご案内します。"));ok(ConversationPlaceContinuity.resolve("違うよ茂林寺の話",history,last,now).equals("茂林寺の近くに沼ある？"));history.put(turn("違うよ茂林寺の話","どの点が違いますか？"));ok(ConversationPlaceContinuity.resolve("茂林寺の話だよ",history,last,now).equals("茂林寺の近くに沼ある？"));
 ok(ConversationPlaceContinuity.resolve("茂林寺の近くに沼があるか？って聞いてんだよ",history,last,now).equals("茂林寺の近くに沼があるか？って聞いてんだよ"));
 JSONArray station=new JSONArray().put(turn("東京駅の住所は？","東京駅は東京都千代田区です。"));ok(ConversationPlaceContinuity.resolve("周辺にカフェある？",station,last,now).equals("東京駅の周辺にカフェある？"));
 JSONArray latin=new JSONArray().put(turn("ピュアブレッドはどこにある？","上尾市にあるジムです。"));ok(ConversationPlaceContinuity.resolve("近くに駐車場ある？",latin,last,now).startsWith("ピュアブレッドの近く"));
 for(String text:new String[]{"今いる場所の近くに沼ある？","現在地の近くにカフェある？","ここから近くの沼は？","自宅の近くに駅ある？","新しい話題。近くに沼ある？","立川駅の近くに沼ある？","立川市の近くに沼ある？","東京駅と上野駅の近くにカフェある？","ここ近くに沼ある？","入力欄の近くにボタンある？","画像の近くに何かある？"})ok(ConversationPlaceContinuity.resolve(text,station,last,now).equals(text));
 ok(ConversationPlaceContinuity.resolve("近くに沼ある？",station,0,now).equals("近くに沼ある？"));ok(ConversationPlaceContinuity.resolve("近くに沼ある？",station,now+1,now).equals("近くに沼ある？"));ok(ConversationPlaceContinuity.resolve("近くに沼ある？",station,last,now+7200001).equals("近くに沼ある？"));
 ok(ConversationPlaceContinuity.resolve("近くに沼ある？",new JSONArray(),last,now).equals("近くに沼ある？"));
 JSONArray ambiguous=new JSONArray().put(turn("この二つを比べて","東京駅と上野駅です。"));ok(ConversationPlaceContinuity.resolve("近くにカフェある？",ambiguous,last,now).equals("近くにカフェある？"));
 String check=ConversationPlaceContinuity.resolve("栃木じゃなかったっけ？",new JSONArray().put(history.get(0)),last,now);ok(check.contains("茂林寺"));ok(check.contains("所在地を再確認"));ok(QueryPolicy.search(check));
 ok(QueryPolicy.search("茂林寺の近くに沼ある？"));ok(!ConversationPlaceContinuity.spatial("ファイルの近くに設定画面がある"));
 JSONArray reset=new JSONArray().put(station.get(0)).put(turn("新しい話題です","何でしょうか。"));ok(ConversationPlaceContinuity.resolve("近くに沼ある？",reset,last,now).equals("近くに沼ある？"));
 ok(ConversationPlaceContinuity.resolve("違うよ茂林寺の話",new JSONArray(),last,now).equals("違うよ茂林寺の話"));
 JSONArray ambiguousCity=new JSONArray().put(turn("この二つを比べて","東京都の東京駅と上野駅です。"));ok(ConversationPlaceContinuity.resolve("近くにカフェある？",ambiguousCity,last,now).equals("近くにカフェある？"));
 ok(ConversationPlaceContinuity.resolve("近くにカフェある？",station,7999900,8000000).equals("近くにカフェある？"));JSONArray future=new JSONArray().put(turn("東京駅の住所は？","東京駅です。").put("time",100001));ok(ConversationPlaceContinuity.resolve("近くにカフェある？",future,last,now).equals("近くにカフェある？"));
 JSONArray action=new JSONArray().put(turn("近くのカフェを予約して","予約内容を確認してください。"));ok(ConversationPlaceContinuity.resolve("違うよ茂林寺の話",action,last,now).equals("違うよ茂林寺の話"));ok(ConversationPlaceContinuity.resolve("近くのカフェを予約して",station,last,now).equals("近くのカフェを予約して"));ok(ConversationPlaceContinuity.resolve("曹洞宗じゃなかったっけ？",station,last,now).equals("曹洞宗じゃなかったっけ？"));ok(ConversationPlaceContinuity.resolve("予約じゃなかったっけ？",station,last,now).equals("予約じゃなかったっけ？"));
 // Original messages are never overwritten by internal resolution.
 ok(history.getJSONObject(2).getString("user").equals("近くに沼ある？"));pipeline(c,history);return n;
 }

 static void set(Object o,String name,Object value)throws Exception{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);f.set(o,value);}
 static void pipeline(Context c,JSONArray history)throws Exception{
  history=new JSONArray(history.toString());for(int i=0;i<history.length();i++)history.getJSONObject(i).put("time",System.currentTimeMillis());
  android.content.SharedPreferences prefs=c.getSharedPreferences("gemini_settings",0);prefs.edit().clear().putString("conversation_history",history.toString()).putLong("last_conversation_at",System.currentTimeMillis()).commit();
  MainActivity[] owner={null};Throwable[] failure={null};CountDownLatch ready=new CountDownLatch(1);new Handler(Looper.getMainLooper()).post(()->{try{owner[0]=new MainActivity();Method attach=ContextWrapper.class.getDeclaredMethod("attachBaseContext",Context.class);attach.setAccessible(true);attach.invoke(owner[0],c);}catch(Throwable e){failure[0]=e;}finally{ready.countDown();}});if(!ready.await(5,TimeUnit.SECONDS)||failure[0]!=null)throw new AssertionError("Synthetic activity unavailable");
  MainActivity activity=owner[0];set(activity,"sharedMemoryManager",new SharedMemoryManager(c,()->"{}"));Field budget=MainActivity.class.getDeclaredField("diagnosticGeminiBudget");budget.setAccessible(true);((java.util.concurrent.atomic.AtomicInteger)budget.get(activity)).set(0);
  Method resolve=MainActivity.class.getDeclaredMethod("resolvePlaceConversationRequest",String.class);resolve.setAccessible(true);String resolved=(String)resolve.invoke(activity,"違うよ茂林寺の話");ok(resolved.equals("茂林寺の近くに沼ある？"));set(activity,"activeGeminiPrompt",resolved);
  Method build=MainActivity.class.getDeclaredMethod("buildGeminiPromptCompact",String.class,boolean.class);build.setAccessible(true);String prompt=(String)build.invoke(activity,resolved,true);String current=prompt.substring(prompt.indexOf("<current_request>"),prompt.indexOf("</current_request>"));ok(current.contains("茂林寺の近くに沼ある？"));ok(!current.contains("立川"));ok(prompt.contains("<reference_context>"));set(activity,"voiceLoopMode",true);String voicePrompt=(String)build.invoke(activity,resolved,true);ok(voicePrompt.contains("<voice_conversation_policy>")&&voicePrompt.contains("茂林寺の近くに沼ある？"));ok(prefs.getString("conversation_history","").equals(history.toString()));prefs.edit().clear().commit();
 }
}
