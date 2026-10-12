package com.example.rokidkeyboardbridge;
import java.util.regex.*;
/** Pronounceable short reactions; ordinary words and the meaning of refusals remain intact. */
public final class LokiVocalizationPolicy {
 private static final String EDGE="\\s、。！？!?…「『（）()」』";
 private static final Pattern CLIPPED=Pattern.compile("(^|["+EDGE+"])(あ|ぁ|ア|ァ|え|ぇ|エ|ェ|お|ぉ|オ|ォ|う|ぅ|ウ|ゥ|はぁ|はあ|は|ひ|ふ|いやぁ|いやあ|いや|きゃぁ|きゃあ|わ|ん)[っッ]+(?=$|["+EDGE+"])");
 private static final Pattern EMPTY=Pattern.compile("(^|["+EDGE+"])[っッ]+(?=$|["+EDGE+"])");
 public static final String INSTRUCTION="音声で読み上げる台詞では、小さい『っ』だけの発声や『っ、っ、』『あっ、あっ、』の連発、読点による細切れを避けてください。普通の単語の促音は正しく使ってください。感情がある場面では短い発声を一節程度にし、毎回答には入れません。驚きは『ああ、驚きました』『ええ、本当ですか』、困惑・思案は『うーん、少し考えます』、拒否は『いや、それは困ります』『いやあ、驚きましたが、お断りします』、息切れは『はあ、少し息を整えます』、安堵は『ふう、落ち着きました』『ああ、安心しました』、肯定は『いいですね』などを文脈に応じて使い分けてください。『ああああ』など長い母音の反復は短く自然な長音にし、息切れを性的な声にしないでください。肯定の声を拒否や苦痛の場面に混ぜないでください。直前と同じ出だし・同じ声を機械的に繰り返さず、発声だけで終わらせず丁寧な意味のある台詞につなげてください。";
 private LokiVocalizationPolicy(){}
 public static String normalize(String raw){
  if(raw==null||raw.isEmpty())return "";
  Matcher m=CLIPPED.matcher(raw);StringBuffer b=new StringBuffer();
  while(m.find()){String v=m.group(2);if(v.matches("[あぁアァ]"))v="ああ";else if(v.matches("[えぇエェ]"))v="ええ";else if(v.matches("[おぉオォ]"))v="おお";else if(v.matches("[うぅウゥ]"))v="うう";else if(v.equals("はぁ")||v.equals("はあ")||v.equals("は"))v="はあ";else if(v.equals("ひ"))v="ひい";else if(v.equals("ふ"))v="ふう";else if(v.startsWith("いや"))v=v.equals("いや")?"いや":"いやあ";else if(v.startsWith("きゃ"))v="きゃあ";else if(v.equals("わ"))v="わあ";m.appendReplacement(b,Matcher.quoteReplacement(m.group(1)+v));}m.appendTail(b);
  String text=EMPTY.matcher(b.toString()).replaceAll("$1");
  text=text.replaceAll("([あぁアァいぃイィうぅウゥえぇエェおぉオォ])\\1{3,}","$1$1$1");
  text=text.replaceAll("[、,](?:\\s*[、,])+","、").replaceAll("(^|[。！？!?])\\s*[、,]+","$1");
  return text.trim();
 }
}
