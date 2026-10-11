package com.example.rokidkeyboardbridge;
import android.content.*;
import android.media.browse.MediaBrowser;
import android.media.session.*;
import android.os.*;
import android.view.Gravity;
import android.widget.Button;
/** Direct HUD toggle. Observe the remote session instead of maintaining a guessed ON/OFF flag. */
final class MusicToggleButton extends Button {
 interface Feedback {void show(String text,boolean ok);}
 private final Handler main=new Handler(Looper.getMainLooper());
 private final Feedback feedback;
 private MediaBrowser browser;private MediaController controller;
 private boolean busy,attached;private int expected=-1;
 private final Runnable timeout=this::timedOut;
 private void timedOut(){busy=false;expected=-1;render();feedback.show("音楽の操作結果を確認できませんでした。",false);}
 private final MediaController.Callback observer=new MediaController.Callback(){
  @Override public void onPlaybackStateChanged(PlaybackState state){
   if(busy&&state!=null&&((expected==PlaybackState.STATE_PLAYING&&playing(state))||(expected==PlaybackState.STATE_PAUSED&&state.getState()==PlaybackState.STATE_PAUSED))){busy=false;expected=-1;main.removeCallbacks(timeout);feedback.show(playing(state)?"音楽を再開しました。":"音楽を一時停止しました。",true);}
   render();
  }
  @Override public void onSessionDestroyed(){disconnect();render();}
 };
 MusicToggleButton(Context context,Feedback feedback){
  super(context);this.feedback=feedback;NavigationChoiceDialog.styleButton(context,this);
  HudButtonStyle.apply(this);
  setOnClickListener(v->toggle());render();
 }
 static boolean playing(PlaybackState state){return state!=null&&(state.getState()==PlaybackState.STATE_PLAYING||state.getState()==PlaybackState.STATE_BUFFERING||state.getState()==PlaybackState.STATE_FAST_FORWARDING||state.getState()==PlaybackState.STATE_REWINDING||state.getState()==PlaybackState.STATE_CONNECTING);}
 @Override protected void onAttachedToWindow(){super.onAttachedToWindow();attached=true;connect();}
 @Override protected void onDetachedFromWindow(){attached=false;disconnect();super.onDetachedFromWindow();}
 private void disconnect(){main.removeCallbacks(timeout);busy=false;expected=-1;if(controller!=null){controller.unregisterCallback(observer);controller=null;}if(browser!=null){browser.disconnect();browser=null;}}
 private void connect(){
  if(!attached||browser!=null)return;
  browser=new MediaBrowser(getContext(),new ComponentName("com.android.bluetooth","com.android.bluetooth.avrcpcontroller.BluetoothMediaBrowserService"),new MediaBrowser.ConnectionCallback(){
   @Override public void onConnected(){try {controller=new MediaController(getContext(),browser.getSessionToken());controller.registerCallback(observer,main);render();}catch(Exception e){disconnect();render();}}
   @Override public void onConnectionFailed(){disconnect();render();}
   @Override public void onConnectionSuspended(){disconnect();render();}
  },null);
  try{browser.connect();}catch(Exception e){disconnect();render();}
 }
 private void render(){
  PlaybackState state=controller==null?null:controller.getPlaybackState();boolean available=state!=null&&state.getState()!=PlaybackState.STATE_NONE&&state.getState()!=PlaybackState.STATE_ERROR;
  setText(busy?"…":!available?"MUSIC":playing(state)?"PAUSE":"PLAY");
  setContentDescription(busy?"音楽を操作中":!available?"音楽：Bluetooth接続を確認":playing(state)?"音楽再生中：押すと一時停止":"音楽停止中：押すと再開");setEnabled(!busy);
 }
 private void toggle(){
  if(busy)return;PlaybackState state=controller==null?null:controller.getPlaybackState();
  if(state==null||state.getState()==PlaybackState.STATE_NONE||state.getState()==PlaybackState.STATE_ERROR){feedback.show("Bluetoothの音楽再生元を確認してください。",false);disconnect();connect();return;}
  boolean pause=playing(state);long required=pause?PlaybackState.ACTION_PAUSE:PlaybackState.ACTION_PLAY;
  if((state.getActions()&required)==0){feedback.show("再生元がこの音楽操作に対応していません。",false);return;}
  busy=true;expected=pause?PlaybackState.STATE_PAUSED:PlaybackState.STATE_PLAYING;render();main.postDelayed(timeout,3500);
  try {if(pause)controller.getTransportControls().pause();else controller.getTransportControls().play();}
  catch(Exception e){main.removeCallbacks(timeout);busy=false;expected=-1;render();feedback.show("音楽の操作を送れませんでした。",false);}
 }
}
