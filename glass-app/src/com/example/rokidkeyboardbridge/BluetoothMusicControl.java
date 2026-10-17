package com.example.rokidkeyboardbridge;
import android.content.*;
import android.media.browse.MediaBrowser;
import android.media.session.*;
import android.media.MediaMetadata;
import android.os.*;
/** Connect to the existing Bluetooth media session; pairing/source selection stays with OS. */
final class BluetoothMusicControl {
 interface Callback {void done(boolean handled,String message,String title);}
 static void execute(Context context,MusicCommand command,Callback callback){
  Handler handler=new Handler(Looper.getMainLooper());
  handler.post(()-> {
   final MediaBrowser[] browser=new MediaBrowser[1];final boolean[] finished={false};
   class Finish {void done(boolean ok,String message,String title){if(finished[0])return;finished[0]=true;if(browser[0]!=null)browser[0].disconnect();callback.done(ok,message,title);}}
   Finish finish=new Finish();
   browser[0]=new MediaBrowser(context,new ComponentName("com.android.bluetooth","com.android.bluetooth.avrcpcontroller.BluetoothMediaBrowserService"),new MediaBrowser.ConnectionCallback(){
    @Override public void onConnected(){try {
     MediaController controller=new MediaController(context,browser[0].getSessionToken());PlaybackState state=controller.getPlaybackState();
     MediaMetadata metadata=controller.getMetadata();String title=metadata==null?"":metadata.getString(MediaMetadata.METADATA_KEY_TITLE);if(title==null)title="";
     if(state==null||state.getState()==PlaybackState.STATE_NONE||state.getState()==PlaybackState.STATE_ERROR){finish.done(false,"Bluetoothの再生元が見つかりません。","");return;}
     MediaController.TransportControls controls=controller.getTransportControls();long actions=state.getActions();
     if(command.action.equals("seek")){
      if((actions&PlaybackState.ACTION_SEEK_TO)==0){finish.done(false,"再生元が秒数指定の移動に対応していません。",title);return;}
      long position=state.getPosition();
      if(position<0){finish.done(false,"再生位置を取得できないため移動できません。",title);return;}
      if(state.getState()==PlaybackState.STATE_PLAYING&&state.getLastPositionUpdateTime()>0){
       long age=SystemClock.elapsedRealtime()-state.getLastPositionUpdateTime();
       if(age<0||age>30000){finish.done(false,"再生位置が古いため移動できません。",title);return;}
       position+=Math.round(age*state.getPlaybackSpeed());
      }
      long duration=metadata==null?0:metadata.getLong(MediaMetadata.METADATA_KEY_DURATION);
      controls.seekTo(MusicSeekPolicy.target(position,command.delta,duration));
      finish.done(true,command.delta>0?"30秒先への操作を送りました。":"30秒戻す操作を送りました。",title);return;
     }else if(command.action.equals("pause")){
      if((actions&PlaybackState.ACTION_PAUSE)==0){finish.done(false,"一時停止に対応していません。",title);return;}controls.pause();
     }else {if((actions&PlaybackState.ACTION_PLAY)==0){finish.done(false,"再生に対応していません。",title);return;}controls.play();}
     finish.done(true,"Bluetoothの再生元へ"+(command.action.equals("pause")?"一時停止":"再開")+"の操作を送りました。",title);
    }catch(Exception e){finish.done(false,"Bluetoothの音楽操作に接続できませんでした。","");}}
    @Override public void onConnectionFailed(){finish.done(false,"Bluetoothの音楽操作に接続できませんでした。","");}
    @Override public void onConnectionSuspended(){finish.done(false,"Bluetoothの接続が切れました。","");}
   },null);
   try {browser[0].connect();handler.postDelayed(()->finish.done(false,"Bluetoothの音楽操作が応答しませんでした。",""),2500);}catch(Exception e){finish.done(false,"Bluetoothの音楽操作を開始できません。","");}
  });
 }
}
