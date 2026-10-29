package com.example.rokidkeyboardbridge;
import android.content.*;import org.json.*;import java.lang.reflect.*;import java.time.*;import java.io.*;
public final class HealthConversationDeviceChecks {
 static int n;static void ok(boolean b){if(!b)throw new AssertionError("health context "+n);n++;}
 static void field(Object o,String key,Object value)throws Exception{Field f=o.getClass().getDeclaredField(key);f.setAccessible(true);f.set(o,value);}
 static Method method(Class<?> c,String key,Class<?>...args)throws Exception{Method m=c.getDeclaredMethod(key,args);m.setAccessible(true);return m;}
 public static int run(Context c)throws Exception{n=0;long now=System.currentTimeMillis();ZoneId z=ZoneId.systemDefault();String day=Instant.ofEpochMilli(now).atZone(z).toLocalDate().toString();
  StepsExerciseAdvice.Sample steps=new StepsExerciseAdvice.Sample("歩数 1234",now,"Health Connect");
  JSONObject energy=new JSONObject().put("balanceKcal",550).put("observedAt",now).put("date",day).put("source","HEALBE_SCREEN").put("raw","do not send raw text");
  String query="今日は疲れているので休みたいです。どうしたらいい？";
  String block=HealthConversationContext.block(1,now,steps,energy,query,0,now,z);ok(block.contains("1234")&&block.contains("550"));ok(block.contains("provisional")&&block.contains("measurementTimeUnknown"));ok(!block.contains("do not send raw text"));
  ok(HealthConversationContext.block(0,now,steps,energy,query,0,now,z).isEmpty());ok(HealthConversationContext.block(1,now-60001,steps,energy,query,0,now,z).isEmpty());
  ok(HealthConversationContext.block(1,now,steps,energy,"東京駅の歴史を短く教えて",now,now,z).isEmpty());
  ok(!HealthConversationContext.block(1,now,steps,energy,"じゃあ",now,now,z).isEmpty());ok(HealthConversationContext.block(1,now,steps,energy,"じゃあ",now-600001,now,z).isEmpty());
  String missing=HealthConversationContext.block(1,now,new StepsExerciseAdvice.Sample("歩数 7777",now-300001,"Health Connect"),new JSONObject(energy.toString()).put("observedAt",now-900001),query,0,now,z);ok(!missing.contains("7777")&&!missing.contains("550"));ok(missing.contains("availability"));
  ok(HealthConversationContext.block(1,now,new StepsExerciseAdvice.Sample("歩数 20",now,"Step Counter / bridge"),null,query,0,now,z).contains("\"partial\":true"));
  JSONObject sdk=new JSONObject(energy.toString()).put("source","HEALBE_SDK").put("measuredAt",now-1000);String sdkBlock=HealthConversationContext.block(1,now,steps,sdk,query,0,now,z);ok(sdkBlock.contains("measuredAt")&&sdkBlock.contains("\"measurementTimeUnknown\":false"));ok(!HealthConversationContext.block(1,now,steps,new JSONObject(sdk.toString()).put("measuredAt",now-900001),query,0,now,z).contains("balanceKcal"));
  JSONObject req=new JSONObject().put("systemInstruction",new JSONObject().put("parts",new JSONArray().put(new JSONObject().put("text","unchanged rule"))));
  HealthConversationContext.apply(req,block);HealthConversationContext.apply(req,block);ok(req.getJSONObject("systemInstruction").getJSONArray("parts").length()==2);
  HealthConversationContext.apply(req,"");ok(req.getJSONObject("systemInstruction").getJSONArray("parts").length()==1);
  MainActivity owner=new MainActivity();method(ContextWrapper.class,"attachBaseContext",Context.class).invoke(owner,c);field(owner,"sharedMemoryManager",new SharedMemoryManager(c,()->"{}"));
  Field b=MainActivity.class.getDeclaredField("diagnosticGeminiBudget");b.setAccessible(true);((java.util.concurrent.atomic.AtomicInteger)b.get(owner)).set(0);
  field(owner,"healthGeminiMode",1);field(owner,"healthGeminiSettingsAt",now);field(owner,"exerciseSteps",steps);field(owner,"exerciseEnergy",energy);field(owner,"activeGeminiPrompt",query);
  String compact=(String)method(MainActivity.class,"buildGeminiPromptCompact",String.class,boolean.class).invoke(owner,query,true);
  JSONObject base=new JSONObject().put("contents",new JSONArray().put(new JSONObject().put("role","user").put("parts",new JSONArray().put(new JSONObject().put("text",compact))))).put("generationConfig",new JSONObject().put("maxOutputTokens",3072).put("thinkingConfig",new JSONObject().put("thinkingBudget",512)));
  Method build=method(MainActivity.class,"buildConversationRequest",JSONObject.class,Integer.class,String.class);
  JSONObject full=(JSONObject)build.invoke(owner,base,null,null);ok(full.toString().contains(HealthConversationContext.PREFIX));ok(full.toString().contains(LokiPersonaPolicy.INSTRUCTION));ok(!base.has("systemInstruction"));ok(full.getJSONArray("contents").toString().equals(base.getJSONArray("contents").toString()));
  try(FileOutputStream out=new FileOutputStream(new File(c.getFilesDir(),"provider-health-fresh.json"))){out.write(full.toString().getBytes("UTF-8"));}
  field(owner,"exerciseSteps",new StepsExerciseAdvice.Sample("歩数 7777",now-86400000,"Health Connect"));field(owner,"exerciseEnergy",new JSONObject(energy.toString()).put("date","2000-01-01"));field(owner,"activeGeminiPrompt","歩数が取れないけれど、今日は休みたいです。");
  String staleCompact=(String)method(MainActivity.class,"buildGeminiPromptCompact",String.class,boolean.class).invoke(owner,"歩数が取れないけれど、今日は休みたいです。",true);
  JSONObject staleBase=new JSONObject(base.toString()).put("contents",new JSONArray().put(new JSONObject().put("role","user").put("parts",new JSONArray().put(new JSONObject().put("text",staleCompact)))));
  JSONObject stale=(JSONObject)build.invoke(owner,staleBase,null,null);ok(stale.toString().contains("availability")&&!stale.toString().contains("7777"));
  try(FileOutputStream out=new FileOutputStream(new File(c.getFilesDir(),"provider-health-missing.json"))){out.write(stale.toString().getBytes("UTF-8"));}
  field(owner,"activeGeminiPrompt","東京駅の歴史を短く教えて");JSONObject unrelated=(JSONObject)build.invoke(owner,base,null,null);ok(!unrelated.toString().contains(HealthConversationContext.PREFIX));
  field(owner,"healthGeminiMode",0);field(owner,"activeGeminiPrompt",query);ok(!((JSONObject)build.invoke(owner,base,null,null)).toString().contains(HealthConversationContext.PREFIX));
  ok(((java.util.concurrent.atomic.AtomicInteger)b.get(owner)).get()==0);return n;
 }
}
