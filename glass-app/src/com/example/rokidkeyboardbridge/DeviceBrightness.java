package com.example.rokidkeyboardbridge;
import android.app.Activity;
import android.provider.Settings;
/** The device setting is authoritative. Never persists a second application level. */
final class DeviceBrightness {
 static int read(Activity a){return Math.round(Settings.System.getInt(a.getContentResolver(),Settings.System.SCREEN_BRIGHTNESS,128)*100f/255f);}
 static int write(Activity a,int percent){
  if(!Settings.System.canWrite(a))throw new SecurityException("本体の明るさ変更を許可してください");
  int value=Math.round(QuickControlPolicy.brightness(percent)*255f/100f);
  if(!Settings.System.putInt(a.getContentResolver(),Settings.System.SCREEN_BRIGHTNESS,value))throw new IllegalStateException("本体の明るさを変更できませんでした");
  // Match a deliberate manual adjustment in the system settings UI.
  if(!Settings.System.putInt(a.getContentResolver(),Settings.System.SCREEN_BRIGHTNESS_MODE,Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL))throw new IllegalStateException("本体の自動明るさを切り替えられませんでした");
  if(Settings.System.getInt(a.getContentResolver(),Settings.System.SCREEN_BRIGHTNESS,-1)!=value)throw new IllegalStateException("本体側で明るさが更新されませんでした");
  return read(a);
 }
}
