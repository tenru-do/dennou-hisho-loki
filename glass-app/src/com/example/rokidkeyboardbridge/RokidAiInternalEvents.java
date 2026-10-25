package com.example.rokidkeyboardbridge;
import android.content.*;import android.os.*;
/** Signature protection also covers dynamic receivers on Android 12L and older. */
public final class RokidAiInternalEvents {
 public static final String PERMISSION="com.example.rokidkeyboardbridge.permission.INTERNAL_EVENTS";
 public static void register(Context c,BroadcastReceiver receiver,IntentFilter filter){
  if(Build.VERSION.SDK_INT>=33)c.registerReceiver(receiver,filter,PERMISSION,null,Context.RECEIVER_NOT_EXPORTED);
  else c.registerReceiver(receiver,filter,PERMISSION,null);
 }
 private RokidAiInternalEvents(){}
}
