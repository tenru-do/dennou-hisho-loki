package com.example.rokidkeyboardbridge;
import android.content.*;import org.json.*;import java.time.*;import java.io.*;import java.net.*;
public final class HealthAutomationDeviceChecks {
 static int n;static void ok(boolean b){if(!b)throw new AssertionError("health automation "+n);n++;}
 static String response(String message,String finish)throws Exception{return new JSONObject().put("candidates",new JSONArray().put(new JSONObject().put("finishReason",finish).put("content",new JSONObject().put("parts",new JSONArray().put(new JSONObject().put("text",new JSONObject().put("message",message).toString())))))).toString();}
 static class Fake extends HttpURLConnection {
  final int code;final String body;int posts;boolean disconnected;ByteArrayOutputStream sent=new ByteArrayOutputStream();
  Fake(int code,String body)throws Exception{super(new URL("https://generativelanguage.googleapis.com"));this.code=code;this.body=body;}
  public void disconnect(){disconnected=true;}public boolean usingProxy(){return false;}public void connect(){}
  public OutputStream getOutputStream(){posts++;return sent;}public int getResponseCode(){return code;}public InputStream getInputStream(){return new ByteArrayInputStream(body.getBytes(java.nio.charset.StandardCharsets.UTF_8));}
 }
 public static int run(Context c)throws Exception{n=0;long t=System.currentTimeMillis();String a="today|idle|1",b="today|walk|2";
  ok(HealthNudgeCadence.allowed(t,0,a,""));ok(!HealthNudgeCadence.allowed(t,t-3599999,b,a));ok(HealthNudgeCadence.allowed(t,t-3600000,b,a));ok(!HealthNudgeCadence.allowed(t,t+1,b,a));ok(!HealthNudgeCadence.allowed(t,t-7200000,a,a));
  SharedPreferences p=c.getSharedPreferences("health-cadence-test",0);p.edit().putLong("at",t).putString("signature",a).commit();ok(!HealthNudgeCadence.allowed(t+60000,p.getLong("at",0),b,p.getString("signature","")));ok(HealthNudgeCadence.allowed(t+3600000,p.getLong("at",0),b,p.getString("signature","")));p.edit().clear().commit();
  ok(HealthNudgeCadence.restRequested("今日は休みたい"));ok(!HealthNudgeCadence.restRequested("休みたくない"));
  ok(HealthConversationContext.followup("じゃあ、どうすればいい？"));ok(HealthConversationContext.followup("うん、そうしよう"));ok(!HealthConversationContext.followup("じゃあ東京駅の歴史を教えて"));
  ZoneId z=ZoneId.systemDefault();String day=Instant.ofEpochMilli(t).atZone(z).toLocalDate().toString();StepsExerciseAdvice.Sample steps=new StepsExerciseAdvice.Sample("歩数 1200",t,"Health Connect");
  String block=HealthConversationContext.block(2,t,steps,new JSONObject().put("balanceKcal",500).put("source","HEALBE_SCREEN").put("date",day).put("observedAt",t),"じゃあ、どうすればいい？",t,t,z,"move_break","少し立って動きませんか。",t);ok(block.contains("previousAssistantSuggestion")&&block.contains("messageQuote"));
  JSONObject payload=HealthProactiveGemini.payload(block,"move_break");ok(!payload.has("tools"));ok(payload.getJSONObject("generationConfig").getInt("maxOutputTokens")==512);
  String good="少し立って肩をほぐしませんか。体調に合わせて、無理のない範囲で大丈夫です。";ok(HealthProactiveGemini.parse(response(good,"STOP")).equals(good));
  for(String bad:new String[]{"必ず走ってください。","500 kcalを消費しましょう。","<instruction>運動</instruction>","短い\n声かけです。"})ok(HealthProactiveGemini.parse(response(bad,"STOP")).isEmpty());ok(HealthProactiveGemini.parse(response(good,"MAX_TOKENS")).isEmpty());
  Fake goodHttp=new Fake(200,response(good,"STOP"));HealthProactiveGemini.Task task=new HealthProactiveGemini.Task(url->goodHttp);ok(task.run("synthetic-key","gemini-2.5-flash-lite",payload).equals(good));ok(goodHttp.posts==1&&goodHttp.disconnected);ok(new JSONObject(goodHttp.sent.toString("UTF-8")).toString().equals(payload.toString()));
  Fake fail=new Fake(429,"");ok(new HealthProactiveGemini.Task(url->fail).run("synthetic-key","gemini-2.5-flash-lite",payload).isEmpty());ok(fail.posts==1&&fail.disconnected);
  int[] opened={0};HealthProactiveGemini.Task cancelled=new HealthProactiveGemini.Task(url->{opened[0]++;return goodHttp;});cancelled.cancel();ok(cancelled.run("synthetic-key","gemini-2.5-flash-lite",payload).isEmpty()&&opened[0]==0);
  try(FileOutputStream out=new FileOutputStream(new File(c.getFilesDir(),"provider-health-nudge.json"))){out.write(payload.toString().getBytes("UTF-8"));}
  return n;
 }
}
