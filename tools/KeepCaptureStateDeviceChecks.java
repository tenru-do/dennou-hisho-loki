package com.example.rokidkeyboardbridge;
import android.content.*;import org.json.*;import java.lang.reflect.*;
public final class KeepCaptureStateDeviceChecks {
 static int n;static void ok(boolean b,String why){if(!b)throw new AssertionError(why);n++;}
 static void set(Object o,String field,Object value)throws Exception{Field f=o.getClass().getDeclaredField(field);f.setAccessible(true);f.set(o,value);}
 public static int run(Context c)throws Exception{n=0;PhotoRecorderService previous=PhotoRecorderService.active;try{
  PhotoRecorderService.active=null;KeepCaptureState.record(c,"FAILED","LOW_SPACE",1000,0,0,900000000);
  JSONObject failure=KeepCaptureState.snapshot(c);ok(failure.optString("phase").equals("FAILED"),"failure survives inactive service");ok(failure.optString("reason").equals("LOW_SPACE"),"low space reason retained");ok(failure.optInt("saved")==0,"no invented photos");
  KeepCaptureState.record(c,"RUNNING","",1000,2,1500,1200000000);JSONObject interrupted=KeepCaptureState.snapshot(c);ok(interrupted.optString("phase").equals("INTERRUPTED"),"process loss not reported as still recording");ok(interrupted.optInt("saved")==2,"last successful count retained");
  PhotoRecorderService live=new PhotoRecorderService();Method attach=ContextWrapper.class.getDeclaredMethod("attachBaseContext",Context.class);attach.setAccessible(true);attach.invoke(live,c);set(live,"savedCount",12);set(live,"startedAt",1000L);set(live,"lastSavedAt",3000L);PhotoRecorderService.active=live;
  JSONObject current=KeepCaptureState.snapshot(c);ok(current.optInt("saved")==12,"live counts do not lag behind first persisted frame");ok(current.optString("phase").equals("RUNNING"),"saved live recording state");ok(current.optLong("lastSavedAt")==3000,"last successful save time");
  set(live,"savedCount",0);ok(KeepCaptureState.snapshot(c).optString("phase").equals("STARTING"),"start does not claim capture success");
  PhotoRecorderService.active=null;KeepCaptureState.record(c,"STOPPED","USER_STOP",1000,12,3000,1200000000);ok(KeepCaptureState.snapshot(c).optString("reason").equals("USER_STOP"),"manual stop distinct from failure");
 }finally{PhotoRecorderService.active=previous;c.getSharedPreferences("keep_capture_state",0).edit().clear().commit();}return n;}
}
