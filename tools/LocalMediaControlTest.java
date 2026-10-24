package com.example.rokidgeminisecretary;
import android.content.Context;import android.media.session.PlaybackState;import android.os.SystemClock;import org.json.*;import java.util.*;
/** Synthetic sessions only; never accesses or controls installed media applications. */
public final class LocalMediaControlTest {
 static int count;
 static void check(boolean value,String message){if(!value)throw new AssertionError(message);count++;}
 static PlaybackState state(long position,long actions,long updated,int mode,float speed){return new PlaybackState.Builder().setActions(actions).setState(mode,position,speed,updated).build();}
 static final class Fake implements LocalMediaControl.Session {
  PlaybackState state;String identity="track-one";long duration=120000;int calls;boolean ignore,change;
  Fake(){state=LocalMediaControlTest.state(60000,PlaybackState.ACTION_SEEK_TO,SystemClock.elapsedRealtime()-50,PlaybackState.STATE_PAUSED,0);}
  public String app(){return "Synthetic test player";}public String identity(){return identity;}public PlaybackState state(){return state;}public long duration(){return duration;}
  public void seek(long position){calls++;if(change){identity="track-two";return;}if(!ignore)state=LocalMediaControlTest.state(position,PlaybackState.ACTION_SEEK_TO,Math.max(SystemClock.elapsedRealtime(),state.getLastPositionUpdateTime()+1),PlaybackState.STATE_PAUSED,0);}
 }
 static JSONObject list(Fake fake)throws Exception{return LocalMediaControl.listSessions(Arrays.asList(fake),SystemClock.elapsedRealtime()).getJSONArray("sessions").getJSONObject(0);}
 static JSONObject request(JSONObject row,long delta)throws Exception{return new JSONObject().put("requestId",UUID.randomUUID().toString()).put("session",row.getString("id")).put("createdAt",System.currentTimeMillis()).put("delta",delta);}
 public static int run(Context context,String unused)throws Exception {
  context.getSharedPreferences("media_control_receipts",0).edit().clear().commit();count=0;long now=SystemClock.elapsedRealtime();
  check(LocalMediaControl.seekable(state(60000,256,now-50,3,1),120000,now),"playing supported");
  check(!LocalMediaControl.seekable(state(60000,0,now,3,1),120000,now),"unseekable live");
  check(!LocalMediaControl.seekable(state(60000,256,now-61000,3,1),120000,now),"projected beyond track end");
  check(LocalMediaControl.seekable(state(60000,256,now-31000,3,1),120000,now),"normal long-lived playing timestamp");
  check(!LocalMediaControl.seekable(state(-1,256,now,2,0),120000,now),"unknown position");
  check(!LocalMediaControl.seekable(state(60000,256,now,2,0),0,now),"unknown duration live");
  check(!LocalMediaControl.seekable(state(60000,256,now,6,1),120000,now),"buffering");
  check(!LocalMediaControl.seekable(state(60000,256,now,3,1.5f),120000,now),"unknown speed projection");
  check(LocalMediaControl.target(10000,-30000,120000)==0,"clamp beginning");
  check(LocalMediaControl.target(110000,30000,120000)==120000,"clamp ending");
  check(LocalMediaControl.target(Long.MAX_VALUE-5,30000,Long.MAX_VALUE)==Long.MAX_VALUE,"no overflow");
  Fake fake=new Fake();JSONObject row=list(fake),input=request(row,30000);JSONObject output=LocalMediaControl.execute(context,input);
  check(output.optBoolean("ok")&&output.optString("status").equals("confirmed")&&fake.state.getPosition()==90000,"forward confirmed");
  check(LocalMediaControl.execute(context,input).optBoolean("ok")&&fake.calls==1,"lost response retry at most once");
  check(LocalMediaControl.execute(context,request(row,-30000)).optBoolean("ok")&&fake.state.getPosition()==60000,"back confirmed");
  JSONObject changed=request(row,30000);fake.identity="another-track";check(LocalMediaControl.execute(context,changed).optString("status").equals("changed")&&fake.calls==2,"changed track cannot seek");
  fake=new Fake();fake.ignore=true;row=list(fake);input=request(row,30000);output=LocalMediaControl.execute(context,input);
  check(!output.optBoolean("ok")&&output.optString("status").equals("unconfirmed"),"ignored command is not success");
  check(!LocalMediaControl.execute(context,input).optBoolean("ok")&&fake.calls==1,"unconfirmed retry cannot move twice");
  fake=new Fake();fake.change=true;row=list(fake);output=LocalMediaControl.execute(context,request(row,30000));check(output.optString("status").equals("changed"),"track changes during command");
  fake=new Fake();row=list(fake);input=request(row,30000).put("createdAt",System.currentTimeMillis()-31000);check(LocalMediaControl.execute(context,input).optString("status").equals("invalid")&&fake.calls==0,"expired network command");
  input=request(row,30000).put("delta",1);check(LocalMediaControl.execute(context,input).optString("status").equals("invalid")&&fake.calls==0,"reject arbitrary offset");
  fake.state=state(0,256,SystemClock.elapsedRealtime()-50,2,0);check(LocalMediaControl.execute(context,request(row,-30000)).optString("status").equals("limit")&&fake.calls==0,"at beginning no send");
  fake=new Fake();fake.duration=65000;row=list(fake);output=LocalMediaControl.execute(context,request(row,30000));check(output.optBoolean("ok")&&output.optLong("actualDelta")==5000,"end clamp reports actual 5 seconds");
  check(!row.toString().contains("track-one"),"no media titles exported");
  return count;
 }
}
