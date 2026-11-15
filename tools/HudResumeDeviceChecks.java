package com.example.rokidkeyboardbridge;
import android.app.*;import android.content.*;import android.graphics.*;import android.view.*;import android.widget.*;import java.lang.reflect.*;import java.io.*;import java.util.*;
public final class HudResumeDeviceChecks{
 static int n;static void ok(boolean b,String s){n++;if(!b)throw new AssertionError(s);}
 static void set(Object owner,String name,Object value)throws Exception{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);f.set(owner,value);}
 static void call(Object owner,String name)throws Exception{Method m=MainActivity.class.getDeclaredMethod(name);m.setAccessible(true);m.invoke(owner);}
 static class Visible extends Button{Visible(Context c,String text){super(c);setText(text);setFocusable(true);setFocusableInTouchMode(true);}public boolean isShown(){return getVisibility()==VISIBLE;}}
 public static int run(Context c)throws Exception{MainActivity owner=new MainActivity();Method attach=ContextWrapper.class.getDeclaredMethod("attachBaseContext",Context.class);attach.setAccessible(true);attach.invoke(owner,c);
  LinearLayout panel=new LinearLayout(c);Visible topic=new Visible(c,"TOPIC"),read=new Visible(c,"READ"),settings=new Visible(c,"SET");panel.addView(topic,new LinearLayout.LayoutParams(160,80));panel.addView(read,new LinearLayout.LayoutParams(160,80));panel.addView(settings,new LinearLayout.LayoutParams(160,80));panel.measure(View.MeasureSpec.makeMeasureSpec(480,1073741824),View.MeasureSpec.makeMeasureSpec(80,1073741824));panel.layout(0,0,480,80);set(owner,"morningButton",topic);set(owner,"imageReadButton",read);set(owner,"settingsButton",settings);set(owner,"buttonPanel",panel);
  SharedPreferences prefs=c.getSharedPreferences("navigation_display",0);String[] keys={"full_body_mode","lower_half","upper_half","large_map","overview"};
  for(int bits=0;bits<32;bits++){
   SharedPreferences.Editor e=prefs.edit();for(int i=0;i<keys.length;i++)e.putBoolean(keys[i],(bits&(1<<i))!=0);e.commit();Map<String,?> before=new HashMap<>(prefs.getAll());
   for(int state=0;state<4;state++){set(owner,"ambientMode",state==1||state==2);set(owner,"ambientPausedForNavigation",state==2);set(owner,"pendingAmbientStart",state==3);call(owner,"updateAmbientButtonLabel");call(owner,"focusSafeDefault");ok(panel.getFocusedChild()==(state==0?read:topic),"actual READ/AMB focus");ok(topic.getText().toString().equals(new String[]{"TOPIC","AMB中","AMB休止","AMB準備"}[state]),"visible top-row state");}
   ok(before.equals(prefs.getAll()),"all saved display values retained");
  }
  set(owner,"ambientMode",true);set(owner,"pendingAmbientStart",false);topic.setVisibility(View.GONE);call(owner,"focusSafeDefault");ok(panel.getFocusedChild()==read,"hidden AMB falls back to READ");topic.setVisibility(View.VISIBLE);settings.requestFocus();call(owner,"updateAmbientButtonLabel");ok(panel.getFocusedChild()==settings,"state refresh never steals user's selection");
  Visible camera=new Visible(c,"CAM");panel.addView(camera,0,new LinearLayout.LayoutParams(120,80));set(owner,"zoomButton",camera);final int[] captures={0};camera.setOnClickListener(v->captures[0]++);read.setOnClickListener(v->captures[0]++);
  Method glance=MainActivity.class.getDeclaredMethod("restoreHeadGlanceSelection",boolean.class,boolean.class,boolean.class);glance.setAccessible(true);
  for(boolean full:new boolean[]{false,true})for(int state=0;state<3;state++){
   prefs.edit().putBoolean("full_body_mode",full).commit();set(owner,"ambientMode",state==1);set(owner,"pendingAmbientStart",state==2);camera.requestFocus();panel.setVisibility(View.INVISIBLE);
   glance.invoke(owner,false,true,false);ok(panel.getVisibility()==View.VISIBLE,"head wake restores button row");ok(panel.getFocusedChild()==(state==0?read:topic),"head-only wake replaces stale CAM with READ/AMB");ok(prefs.getBoolean("full_body_mode",!full)==full,"head wake retains face/full-body selection");
  }
  set(owner,"ambientMode",false);set(owner,"pendingAmbientStart",false);camera.requestFocus();glance.invoke(owner,true,true,true);ok(panel.getFocusedChild()==camera,"repeated visible tilt preserves manual selection");
  camera.requestFocus();glance.invoke(owner,true,true,false);ok(panel.getFocusedChild()==read,"new head gesture resets even when firmware still reports interactive HUD");
  camera.requestFocus();glance.invoke(owner,true,false,false);ok(panel.getFocusedChild()==read,"OS sleep even with visible HUD restores READ");
  class Draft extends EditText{Draft(Context c){super(c);}public boolean isShown(){return true;}}
  Draft draft=new Draft(c);draft.setText("synthetic unsent draft");set(owner,"input",draft);camera.requestFocus();glance.invoke(owner,false,false,false);ok(panel.getFocusedChild()==camera,"draft focus is not stolen");set(owner,"input",null);
  ok(captures[0]==0,"selecting default never captures or activates CAM/READ");Field hf=MainActivity.class.getDeclaredField("handler");hf.setAccessible(true);((android.os.Handler)hf.get(owner)).removeCallbacksAndMessages(null);
  Bitmap picture=Bitmap.createBitmap(480,80,Bitmap.Config.ARGB_8888);panel.draw(new Canvas(picture));try(FileOutputStream out=c.openFileOutput("hud-resume-buttons.png",0)){picture.compress(Bitmap.CompressFormat.PNG,100,out);}picture.recycle();
  ok(topic.getContentDescription().toString().contains("停止"),"AMB stop described");return n;}
}
