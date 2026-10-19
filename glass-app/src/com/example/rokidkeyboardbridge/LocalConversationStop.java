package com.example.rokidkeyboardbridge;
/** Typed local stop: fixed public wording, never provider response or user content. */
class LocalConversationStop extends java.io.IOException {
 private final String code;
 LocalConversationStop(String code){super(message(code));this.code=code;}
 String code(){return code;}
 static String summary(String code,long at){if(!"REFERENCE_CHANGED".equals(code)||at<=0)return "";return "直近の端末内停止：画像参照の更新・失効\n"+new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm",java.util.Locale.JAPAN).format(new java.util.Date(at))+"\n自動再送していません。参照を選び直してください。";}
 static String message(String code){if(!"REFERENCE_CHANGED".equals(code))throw new IllegalArgumentException("Unknown local stop");return "画像参照が更新・失効したため処理を停止しました。参照を選び直してください（自動再送なし）";}
}
