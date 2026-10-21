package com.example.rokidgeminisecretary;
import android.content.*;import android.app.*;import org.json.*;
/** Optional silent local notification. Never asks for a permission or starts an activity by itself. */
final class TransferResultNotice {
 static final int NOTICE_ID=7261;
 static void clear(Context c){try{NotificationManager n=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);if(n!=null)n.cancel(NOTICE_ID);}catch(Exception ignored){}}
 static boolean enabled(Context c){return c.getSharedPreferences("transfer_result_notices",0).getBoolean("enabled",false);}
 static boolean available(Context c){try{NotificationManager n=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);return n!=null&&n.areNotificationsEnabled();}catch(Exception e){return false;}}
 static void set(Context c,boolean enabled)throws Exception{if(!c.getSharedPreferences("transfer_result_notices",0).edit().putBoolean("enabled",enabled).commit())throw new java.io.IOException("通知設定を保存できません");if(!enabled)clear(c);}
 static void show(Context c,JSONObject receipt){if(!enabled(c)||!available(c))return;try{String phase=receipt.optString("remotePhase");if(!ExperiencePolicy.terminal(phase))return;NotificationManager manager=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);NotificationChannel channel=new NotificationChannel("loki-transfer-results","転送予約の結果",NotificationManager.IMPORTANCE_LOW);channel.setSound(null,null);channel.enableVibration(false);manager.createNotificationChannel(channel);Intent intent=new Intent(c,MainActivity.class).putExtra("openTransferReservation",true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);PendingIntent open=PendingIntent.getActivity(c,7260,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);Notification n=new Notification.Builder(c,"loki-transfer-results").setSmallIcon(android.R.drawable.stat_sys_upload_done).setContentTitle("ロキ："+ExperiencePolicy.transferLabel(phase)).setContentText("保存確認 "+receipt.optLong("confirmed")+" / "+receipt.optLong("expected")+"件。詳細を確認してください").setContentIntent(open).setAutoCancel(true).setVisibility(Notification.VISIBILITY_PRIVATE).build();manager.notify(NOTICE_ID,n);}catch(Exception ignored){}}
 private TransferResultNotice(){}
}
