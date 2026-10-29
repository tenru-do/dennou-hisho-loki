package com.example.rokidkeyboardbridge;
final class TopicProgressLabel {
 static String label(String phase,String reason){
  if("CAPTURING".equals(phase))return "TOPIC：音・画像の取得待ち";
  if("GENERATING".equals(phase))return "TOPIC：AIのコメントを作成中";
  if("QUEUED".equals(phase))return "TOPIC：コメントをグラスへ転送中";
  if("ERROR".equals(phase))return "TOPIC：処理失敗・接続と権限を確認";
  if("WAITING".equals(phase)&&"BRIDGE_STALE".equals(reason))return "TOPIC：グラスとの接続待ち";
  if("WAITING".equals(phase)&&"COOLDOWN".equals(reason))return "TOPIC：通信の再開待ち";
  if("WAITING".equals(phase)&&reason.matches("BACKGROUND|MENU|NAVIGATION|CONVERSATION|CAPTURING|BUSY"))return "TOPIC："+blocked(reason);
  if("WAITING".equals(phase)&&"QUEUE_BUSY".equals(reason))return "TOPIC：依頼の転送待ち";
  return "";
 }
 static String blocked(String reason){return "BACKGROUND".equals(reason)?"画面復帰待ち":"MENU".equals(reason)?"メニューを閉じると再開":"NAVIGATION".equals(reason)?"ナビを優先中":"CONVERSATION".equals(reason)?"会話の終了待ち":"CAPTURING".equals(reason)?"前の取得処理待ち":"別の処理の終了待ち";}
 private TopicProgressLabel(){}
}
