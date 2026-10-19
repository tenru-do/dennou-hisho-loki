package com.example.rokidgeminisecretary;
import org.json.*;
public class ConversationRuntimeSnapshotTest {
 static int n;static void ok(boolean b,String s){if(!b)throw new AssertionError(s);n++;}
 public static void main(String[]args)throws Exception{
  JSONObject j=ConversationRuntimeSnapshot.build("THINKING","回答の文脈を確認中",200000,199000,198000,0,2);
  ok(j.getBoolean("contactRecent")&&j.getBoolean("stateFresh"),"recent phone-clock contact");
  ok(j.getLong("stateAgeMs")==2000,"state age calculated on phone");
  ok(j.getString("message").contains("文脈"),"stage preserved");
  ok(j.getInt("queued")==2,"queue count only");
  ok(!j.has("commands")&&!j.has("request")&&!j.has("api_key"),"no command or key exposure");
  j=ConversationRuntimeSnapshot.build("READY","",200000,0,0,0,0);
  ok(!j.getBoolean("contactRecent")&&!j.getBoolean("stateFresh"),"restart cannot revive stale persisted READY");
  ok(j.getLong("stateAgeMs")==-1,"missing age unknown");
  j=ConversationRuntimeSnapshot.build("THINKING","",200000,184999,79999,0,0);
  ok(!j.getBoolean("contactRecent")&&!j.getBoolean("stateFresh"),"old heartbeat and stage stale");
  j=ConversationRuntimeSnapshot.build("READY","",200000,200001,200001,-100,-1);
  ok(!j.getBoolean("contactRecent")&&!j.getBoolean("stateFresh"),"future monotonic time rejected");
  ok(j.getLong("waitMs")==0&&j.getInt("queued")==0,"negative values bounded");
  j=ConversationRuntimeSnapshot.build(null,"x".repeat(300),200000,200000,200000,9999999,999);
  ok(j.getString("state").equals("UNKNOWN")&&j.getString("message").length()==80,"safe bounded defaults");
  ok(j.getLong("waitMs")==600000&&j.getInt("queued")==100,"upper bounds");
  System.out.println("PASS conversation runtime snapshot "+n+" checks");
 }
}
