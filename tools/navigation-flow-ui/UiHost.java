package com.example.lokinavflowui;
public final class UiHost extends android.app.Activity {
 public static volatile android.app.Activity current;public static volatile Runnable onDisposed;public static volatile int resumes,pauses;
 public void onCreate(android.os.Bundle b){super.onCreate(b);current=this;android.widget.TextView text=new android.widget.TextView(this);text.setText("ロキの隔離ナビ操作検証\n合成データのみ・通信権限なし");setContentView(text);getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);}
 protected void onResume(){super.onResume();resumes++;}protected void onPause(){pauses++;super.onPause();}
 protected void onDestroy(){Runnable callback=onDisposed;onDisposed=null;if(callback!=null)callback.run();if(current==this)current=null;super.onDestroy();}
}
