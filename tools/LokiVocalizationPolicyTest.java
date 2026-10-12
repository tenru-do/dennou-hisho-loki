package com.example.rokidkeyboardbridge;
public final class LokiVocalizationPolicyTest {
 static int n;static void equal(String before,String after){String v=LokiVocalizationPolicy.normalize(before);if(!v.equals(after))throw new AssertionError(before+" -> "+v);if(!LokiVocalizationPolicy.normalize(v).equals(v))throw new AssertionError("not idempotent");n++;}
 public static void main(String[]args){
  equal("あっ、あっ、はぁっ、っ、っ、落ち着きます。","ああ、ああ、はあ、落ち着きます。");
  equal("えっ！驚きました。","ええ！驚きました。");equal("いやぁっ、困ります。","いやあ、困ります。");equal("ああああ、ううううん、いいですね。","あああ、うううん、いいですね。");
  for(String x:new String[]{"待ってください。","しっかり確認します。","あった資料を読みます。","買った切符です。","いいえ、お断りします。","うーん、考えます。","いや、嫌です。","はあ、はあ。息を整えます。","１、２、３です。"})equal(x,x);
  String fallback=MascotTtsTextFilter.prepare("あった資料をしっかり確認します。あっ、驚きました。").fallback;
  if(!fallback.equals("あった資料をしっかり確認します。ああ、驚きました。"))throw new AssertionError("TTS fallback "+fallback);n++;
  if(MascotTtsTextFilter.prepare("<prosody>あっ</prosody>").ssml.contains("<prosody>あっ"))throw new AssertionError("untrusted markup");n++;
  if(!LokiPersonaPolicy.INSTRUCTION.contains("音声で読み上げる台詞"))throw new AssertionError("generation instruction missing");n++;
  System.out.println("PASS "+n+" vocal normalization, word preservation, meaning, idempotence and actual TTS integration checks");
 }
}
