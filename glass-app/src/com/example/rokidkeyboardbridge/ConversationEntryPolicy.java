package com.example.rokidkeyboardbridge;
/** Only protects local control shortcuts. Natural-language meaning remains the model's job. */
final class ConversationEntryPolicy {
 static boolean discussion(String text){
  String s=text==null?"":text.trim();
  return s.length()>180||s.matches("(?s).*[「」『』“”\\\"].*")
   ||s.matches("(?s).*(しない|止めない|消さない|やめない|かどうか|か教えて|か迷|考えて|しなくて|しないよう|しない場合|したら|するとき|する時|したとき|すれば|するなら|した場合|って言|と言|という|方法|仕組み|誤動作|なぜ|なんで|できる[？?]|できない[？?]).*");
 }
 static boolean localRead(String text){
  if(discussion(text))return false;
  String s=text==null?"":text.replaceAll("[\\s　。！!？?]","");
  return s.matches("(?:今何時|いま何時|今は何時|現在時刻|時刻を教えて|時間を教えて|今日は何日|今日の日付|何曜日|日付を教えて|(?:バッテリー|電池残量|充電残量|接続状況|ネットワーク状況|wifi状況|wi-fi状況)(?:は|を教えて|を確認して)?)");
 }
 static String correction(String text){
  if(text==null)return "";
  String s=text.trim();if(s.length()>500)return s;
  // Only explicit naming/recognition corrections; never silently rewrite the original utterance.
  if(s.matches("(?s)^(?:名前|人名|地名|駅名|読み方|表記|聞き取り|音声認識|その名前|さっきの名前)(?:は|が|の訂正は).{1,120}(?:です|だよ|だ|と読む|って読む|と書く)[。！!]*$"))return "訂正："+s;
  return s;
 }
 private ConversationEntryPolicy(){}
}
