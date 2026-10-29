package com.example.rokidgeminisecretary;
import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.*;
/** Passive, package-scoped text only. Does not click, launch, record or send to AI. */
public final class HealbeEnergyAccessibilityService extends AccessibilityService {
 private long readAt;
 private final android.os.Handler handler=new android.os.Handler(android.os.Looper.getMainLooper());
 private final Runnable readTask=this::readActive;
 @Override public void onAccessibilityEvent(AccessibilityEvent event){
  if(event==null||!"com.healbe.healbegobe".contentEquals(event.getPackageName()==null?"":event.getPackageName()))return;
  long elapsed=android.os.SystemClock.elapsedRealtime();handler.removeCallbacks(readTask);
  if(elapsed-readAt<3000){handler.postDelayed(readTask,3000-(elapsed-readAt)+100);return;}
  readActive();
 }
 private void readActive(){
  readAt=android.os.SystemClock.elapsedRealtime();
  AccessibilityNodeInfo root=getRootInActiveWindow();if(root==null)return;
  try{if(!"com.healbe.healbegobe".contentEquals(root.getPackageName()==null?"":root.getPackageName()))return;
   StringBuilder text=new StringBuilder();append(root,text,new int[]{0});
   HealbeEnergyStore.observe(this,text.toString(),"HEALBE_SCREEN",System.currentTimeMillis());
  }finally{root.recycle();}
 }
 private void append(AccessibilityNodeInfo node,StringBuilder text,int[] count){
  if(++count[0]>250||text.length()>14000)return;
  CharSequence value=node.getText();if(value==null||value.length()==0)value=node.getContentDescription();
  if(value!=null&&value.length()<1000)text.append(value).append('\n');
  for(int i=0;i<node.getChildCount()&&count[0]<250;i++){AccessibilityNodeInfo child=node.getChild(i);if(child!=null)try{append(child,text,count);}finally{child.recycle();}}
 }
 @Override public void onInterrupt(){}
 @Override public void onDestroy(){handler.removeCallbacks(readTask);super.onDestroy();}
}
