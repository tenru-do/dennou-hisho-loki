package com.example.rokidkeyboardbridge;
import android.content.*;import android.os.*;
final class ResourceLoad {static volatile String status="通常負荷";static long lastOptional=-1;static volatile long checkedAt;
 static int level(Context c){checkedAt=System.currentTimeMillis();if(!c.getSharedPreferences("device_tuning",0).getBoolean("adaptive",true)){status="負荷の自動調整OFF（既存の録音保護は維持）";return 0;}int battery=-1,temp=-1,thermal=0;try{Intent b=c.registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));if(b!=null){int scale=b.getIntExtra("scale",0);if(scale>0)battery=b.getIntExtra("level",-1)*100/scale;temp=b.getIntExtra("temperature",-1);}if(Build.VERSION.SDK_INT>=29)thermal=((PowerManager)c.getSystemService(Context.POWER_SERVICE)).getCurrentThermalStatus();}catch(Exception ignored){}int result=ResourceLoadPolicy.withStorage(ResourceLoadPolicy.level(battery,temp,thermal),c.getFilesDir().getUsableSpace());status=result>0&&c.getFilesDir().getUsableSpace()<300000000?"空き容量低下：録音の保存を優先し任意の撮影を抑制":ResourceLoadPolicy.label(result);return result;}
 static synchronized String optional(Context c){int value=level(c);if(value>=2)return status;if(value==1){long now=SystemClock.elapsedRealtime();if(lastOptional>=0&&now-lastOptional<60000)return status;lastOptional=now;}return "";}
 private ResourceLoad(){}
}
