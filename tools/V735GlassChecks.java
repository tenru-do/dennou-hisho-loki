package com.example.rokidkeyboardbridge;
import android.content.*;import org.json.*;import java.io.*;import java.lang.reflect.*;import java.nio.charset.StandardCharsets;import java.text.SimpleDateFormat;import java.util.*;
public final class V735GlassChecks {
 static int n;static void ok(boolean b,String why){n++;if(!b)throw new AssertionError(why);}
 static Method method(String name,Class<?>...args)throws Exception{Method m=MainActivity.class.getDeclaredMethod(name,args);m.setAccessible(true);return m;}
 static void field(MainActivity a,String name,Object value)throws Exception{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);f.set(a,value);}
 public static int run(Context c)throws Exception{
  n=0;JSONObject capability;try{capability=KeepExposureSupport.read(c);}catch(Exception e){capability=new JSONObject().put("known",false).put("errorType",e.getClass().getSimpleName());}try(FileOutputStream file=new FileOutputStream(new File(c.getFilesDir(),"exposure-capability.json"))){file.write(capability.toString().getBytes(StandardCharsets.UTF_8));}
  for(float step:new float[]{.1f,.33333334f,.5f,1f})for(int mode=-1;mode<=1;mode++){int value=KeepExposurePolicy.compensation(mode,step,-6,6);ok(value>=-6&&value<=6,"hardware EV bounds");ok(mode==0?value==0:mode<0?value<0:value>0,"EV direction");}
  ok(KeepExposurePolicy.compensation(1,0,0,0)==0,"no exposure support");ok(KeepExposurePolicy.compensation(-1,Float.NaN,-4,4)==0,"invalid capability safe");
  ok(CalendarReadContext.name("カレンダーの「合成家族」を見ればわかるよ").equals("合成家族"),"named calendar reference");ok(CalendarConversationPolicy.lookup("予定が見えなくなった？"),"read failure followup lookup");ok(CalendarReadContext.related("明日は合成点滴のみか？"),"dated appointment enrichment");ok(!CalendarReadContext.related("点滴の副作用について教えて"),"general medical question does not retrieve private schedule");
  Calendar tomorrow=Calendar.getInstance();tomorrow.add(Calendar.DAY_OF_YEAR,1);String date=new SimpleDateFormat("yyyy-MM-dd",Locale.ROOT).format(tomorrow.getTime());
  JSONArray events=new JSONArray().put(new JSONObject().put("calendar","合成家族").put("title","合成点滴の予約").put("allDay",true).put("begin",tomorrow.getTimeInMillis()).put("end",tomorrow.getTimeInMillis()+3600000))
   .put(new JSONObject().put("calendar","合成勤務").put("title","架空の会議").put("allDay",false).put("begin",tomorrow.getTimeInMillis()).put("end",tomorrow.getTimeInMillis()+1800000));
  String raw=new JSONObject().put("ok",true).put("timezone",TimeZone.getDefault().getID()).put("date",date).put("startDate",date).put("endDate",date).put("events",events).toString();
  JSONObject scoped=new JSONObject(CalendarReadContext.format(raw,"明日","合成家族"));ok(scoped.getJSONArray("events").length()==1&&!scoped.toString().contains("架空の会議"),"named owner filtering");
  MainActivity a=new MainActivity();Method attach=ContextWrapper.class.getDeclaredMethod("attachBaseContext",Context.class);attach.setAccessible(true);attach.invoke(a,c);
  field(a,"sharedMemoryManager",new SharedMemoryManager(c,()->"{}"));Field budget=MainActivity.class.getDeclaredField("diagnosticGeminiBudget");budget.setAccessible(true);((java.util.concurrent.atomic.AtomicInteger)budget.get(a)).set(0);int[] offset={0};field(a,"diagnosticCalendarSource",(CalendarReadContext.Source)(days,count)->{offset[0]=days;return raw;});
  c.getSharedPreferences("gemini_settings",0).edit().clear().commit();
  method("buildCalendarReadContext",String.class).invoke(a,"明日は合成点滴のみか？");ok(offset[0]==1,"dated appointment reads tomorrow");
  String[] questions={"カレンダーの「合成家族」を見ればわかるよ","明日の予定は？"};
  for(int i=0;i<questions.length;i++){
   String q=questions[i];field(a,"activeGeminiPrompt",q);String prompt=(String)method("buildGeminiPromptCompact",String.class,boolean.class).invoke(a,q,true);
   ok(prompt.contains("<calendar_read_context>")&&prompt.contains("合成家族")&&prompt.contains(date),"actual prompt carries date and calendar ownership");ok(offset[0]==1,"reference keeps tomorrow date");
   int start=prompt.indexOf("<calendar_read_context>")+"<calendar_read_context>".length(),end=prompt.indexOf("</calendar_read_context>");JSONObject block=new JSONObject(prompt.substring(start,end).trim());ok(block.optString("status").equals("ok"),"retrieved evidence success");ok(block.getJSONArray("events").length()==(i==0?1:2),"scoped vs general schedule separation");
   JSONObject input=new JSONObject().put("contents",new JSONArray().put(new JSONObject().put("role","user").put("parts",new JSONArray().put(new JSONObject().put("text",prompt)))));
   JSONObject body=(JSONObject)method("buildConversationRequest",JSONObject.class,Integer.class,String.class).invoke(a,input,null,null);body.remove("tools");body.remove("toolConfig");
   try(FileOutputStream out=new FileOutputStream(new File(c.getFilesDir(),"calendar-provider-"+i+".json"))){out.write(body.toString().getBytes(StandardCharsets.UTF_8));}
  }
  JSONObject missing=new JSONObject(CalendarReadContext.format(raw,"明日","存在しない合成カレンダー"));ok(missing.getJSONArray("events").length()==0&&missing.has("note"),"missing owner never falls through to other person");
  field(a,"activeGeminiPrompt","明日の予定は？");String listing=a.requestGeminiWithRetry("no-provider-key","");ok(listing.contains("[合成家族]")&&listing.contains("[合成勤務]"),"actual final general listing retains owner labels without model rewrite");ok(CalendarReadContext.listOnly("明日の予定は？")&&!CalendarReadContext.listOnly("明日の予定を変更して")&&!CalendarReadContext.listOnly("明日は合成点滴のみか"),"deterministic listing does not capture writes or conversational followups");
  android.widget.LinearLayout panel=new android.widget.LinearLayout(c);android.widget.Button read=new android.widget.Button(c){public boolean isShown(){return true;}},cam=new android.widget.Button(c){public boolean isShown(){return true;}};
  read.setFocusableInTouchMode(true);cam.setFocusableInTouchMode(true);panel.addView(cam);panel.addView(read);field(a,"buttonPanel",panel);field(a,"imageReadButton",read);field(a,"zoomButton",cam);int[] clicks={0};read.setOnClickListener(v->clicks[0]++);cam.setOnClickListener(v->clicks[0]++);
  method("focusSafeDefault").invoke(a);ok(read.isFocused()&&!cam.isFocused(),"READ wins over first-position CAM on recovery");method("focusSafeDefault").invoke(a);ok(clicks[0]==0&&read.isFocused(),"repeated recovery focus never opens camera");read.setEnabled(false);method("focusSafeDefault").invoke(a);ok(cam.isFocused(),"disabled READ still permits safe visible fallback");
  return n;
 }
}
