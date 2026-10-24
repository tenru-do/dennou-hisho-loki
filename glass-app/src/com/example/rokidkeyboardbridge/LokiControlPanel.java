package com.example.rokidkeyboardbridge;
import android.app.*;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.*;
import android.widget.*;
/** Small black overlay: navigation and conversation keep running behind it. */
final class LokiControlPanel {
 interface Actions {void brightness(int percent);void systemBrightness();default int currentBrightness(){return -1;}}
 static AlertDialog show(Activity activity,int initial,Actions actions){
  LinearLayout root=new LinearLayout(activity);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(12,8,12,8);root.setBackgroundColor(Color.BLACK);
  TextView status=new TextView(activity);status.setTextColor(Color.WHITE);status.setTextSize(12);status.setText("明るさ");root.addView(status);
  LinearLayout light=new LinearLayout(activity);root.addView(light);
  Button dimmer=button(activity,light,"暗く"),brighter=button(activity,light,"明るく");
  TextView level=new TextView(activity);level.setTextColor(Color.WHITE);level.setTextSize(12);root.addView(level);
  SeekBar slider=new SeekBar(activity);slider.setMax(95);slider.setProgress(QuickControlPolicy.brightness(initial)-5);root.addView(slider);
  TextView pad=new TextView(activity);pad.setTextColor(Color.WHITE);pad.setTextSize(12);pad.setPadding(8,10,8,10);pad.setText("上下スワイプ：↑明るく　↓暗く");pad.setFocusable(true);pad.setFocusableInTouchMode(true);root.addView(pad);
  AlertDialog dialog=new LokiAlertDialogBuilder(activity).setView(root).setNegativeButton("閉じる",null).setNeutralButton("本体の値を確認",null).create();
  int[] percent={Math.max(0,Math.min(100,initial))};boolean[] syncing={false};
  Runnable label=()->level.setText("本体の明るさ："+percent[0]+"%");label.run();
  java.util.function.IntConsumer refresh=value->{percent[0]=Math.max(0,Math.min(100,value));syncing[0]=true;slider.setProgress(Math.max(0,percent[0]-5));syncing[0]=false;label.run();};
  java.util.function.IntConsumer adjust=value->{try{actions.brightness(QuickControlPolicy.brightness(value));int actual=actions.currentBrightness();refresh.accept(actual<0?QuickControlPolicy.brightness(value):actual);}catch(Exception error){int actual=actions.currentBrightness();if(actual>=0)refresh.accept(actual);level.setText(error.getMessage());}};
  android.database.ContentObserver observer=new android.database.ContentObserver(new android.os.Handler(android.os.Looper.getMainLooper())){public void onChange(boolean changed){int actual=actions.currentBrightness();if(actual>=0)refresh.accept(actual);}};
  activity.getContentResolver().registerContentObserver(android.provider.Settings.System.getUriFor(android.provider.Settings.System.SCREEN_BRIGHTNESS),false,observer);
  dialog.setOnDismissListener(d->activity.getContentResolver().unregisterContentObserver(observer));
  java.util.function.Consumer<String> operate=action->{
   if(action.equals("brighter")){adjust.accept(percent[0]+10);return;}if(action.equals("dimmer")){adjust.accept(percent[0]-10);return;}
  };
  dimmer.setOnClickListener(v->operate.accept("dimmer"));brighter.setOnClickListener(v->operate.accept("brighter"));
  slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar bar,int progress,boolean user){if(user&&!syncing[0])adjust.accept(progress+5);}public void onStartTrackingTouch(SeekBar bar){}public void onStopTrackingTouch(SeekBar bar){}});
  float[] down={0,0};
  pad.setOnTouchListener((v,event)->{if(event.getAction()==MotionEvent.ACTION_DOWN){down[0]=event.getX();down[1]=event.getY();v.requestFocus();return true;}if(event.getAction()==MotionEvent.ACTION_UP){String action=QuickControlPolicy.swipe(event.getX()-down[0],event.getY()-down[1],24*activity.getResources().getDisplayMetrics().density);if(!action.isEmpty())operate.accept(action);return true;}return true;});
  // Some glasses report temple swipes as DPAD events rather than touch coordinates.
  pad.setOnKeyListener((v,key,event)->{if(key==KeyEvent.KEYCODE_DPAD_CENTER||key==KeyEvent.KEYCODE_ENTER){if(event.getAction()==KeyEvent.ACTION_UP&&!event.isCanceled())dimmer.requestFocus();return true;}String action=key==KeyEvent.KEYCODE_DPAD_UP?"brighter":key==KeyEvent.KEYCODE_DPAD_DOWN?"dimmer":"";if(action.isEmpty())return false;if(event.getAction()==KeyEvent.ACTION_UP&&!event.isCanceled())operate.accept(action);return true;});
  dialog.setOnShowListener(d->{Window window=dialog.getWindow();window.setBackgroundDrawable(new ColorDrawable(Color.BLACK));window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);window.setGravity(Gravity.BOTTOM);window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.WRAP_CONTENT);
   WindowManager.LayoutParams attrs=window.getAttributes();attrs.screenBrightness=-1;window.setAttributes(attrs);
   Button reset=dialog.getButton(AlertDialog.BUTTON_NEUTRAL);NavigationChoiceDialog.styleButton(activity,reset);reset.setTextSize(12);reset.setOnClickListener(v->{actions.systemBrightness();WindowManager.LayoutParams p=window.getAttributes();p.screenBrightness=-1;window.setAttributes(p);int actual=actions.currentBrightness();if(actual>=0)refresh.accept(actual);});NavigationChoiceDialog.styleButton(activity,dialog.getButton(AlertDialog.BUTTON_NEGATIVE));pad.requestFocus();
  });dialog.show();return dialog;
 }
 private static Button button(Activity activity,LinearLayout row,String label){Button b=new LokiActionButton(activity);b.setText(label);NavigationChoiceDialog.styleButton(activity,b);b.setTextSize(13);b.setPadding(4,0,4,0);b.setIncludeFontPadding(false);b.setGravity(Gravity.CENTER);row.addView(b,new LinearLayout.LayoutParams(0,Math.round(32*activity.getResources().getDisplayMetrics().density),1));return b;}
}
