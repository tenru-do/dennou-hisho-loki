package com.example.rokidkeyboardbridge;
/** Optional camera work yields to recording storage and memory; microphone ownership is unchanged. */
final class MeetingCameraPolicy {
 static final long MB=1024L*1024;
 static long interval(int pending,long freeHeap){return pending>8||freeHeap<48*MB?60000L:30000L;}
 static String reason(int mode,boolean stopping,int pending,boolean lowMemory,long freeHeap,long now,long last){
  if(mode==0||mode==3)return "AUDIO_ONLY";if(stopping)return "STOPPING";if(pending>16)return "AUDIO_BACKLOG";if(lowMemory)return "LOW_MEMORY";if(freeHeap<32*MB)return "HEAP_RESERVE";
  if(now<0||last>0&&now<last)return "CLOCK";if(last>0&&now-last<interval(pending,freeHeap))return interval(pending,freeHeap)==60000?"INTERVAL60":"INTERVAL30";return "";
 }
 static String label(String code){switch(code){
  case "RESOURCE_LOAD":return "発熱・低電池：録音優先で画像判定を抑制";
  case "READY":return "取得を許可";case "KEEP_WAIT":return "KEEP撮影・保存を優先";case "NO_FRAME":return "KEEPの保存写真を準備中";case "STALE":return "保存写真が15秒より古いため待機";case "RATE":return "保存写真の再利用は30秒間隔";case "SOURCE_SIZE":return "保存写真のサイズ上限で待機";case "MISSING":return "保存写真を確認できず待機";case "READ_ERROR":return "保存写真の読み出しを完了できず待機";case "CANCELLED":return "ユーザー操作・終了を優先";
  case "AUDIO_ONLY":return "録音・音声のみのモード";case "STOPPING":return "会議録音の終了処理を優先";case "AUDIO_BACKLOG":return "録音の保存待ちを優先";case "LOW_MEMORY":return "端末のメモリ不足で画像を待機";case "HEAP_RESERVE":return "録音用メモリの余裕を確保";case "CLOCK":return "取得時刻を確認できず待機";case "INTERVAL60":return "録音負荷を優先・撮影は60秒間隔";case "INTERVAL30":return "会議中の撮影は30秒間隔";default:return "画像判定の要求はまだありません";}}
 private MeetingCameraPolicy(){}
}
