import com.example.rokidkeyboardbridge.*;
import java.util.*;
public final class ConversationPolicyTest {
 static int checks;
 static void check(boolean ok,String name){checks++;if(!ok)throw new AssertionError(name);}
 static byte[] pcm(int amplitude,int samples){byte[] b=new byte[samples*2];for(int i=0;i<samples;i++){int a=i%2==0?amplitude:-amplitude;b[2*i]=(byte)a;b[2*i+1]=(byte)(a>>8);}return b;}
 public static void main(String[] args){
  for(int turn=0;turn<40;turn++) {
   String response="マスター、まず必要な条件を確認いたします。背景を整理すると、次の判断に役立ちます。";
   check(LokiPersonaPolicy.accepts(response),"polite turn "+turn);
   check(!LokiPersonaPolicy.accepts("いいよ。次はこうしよう。"),"casual history drift");
   check(!LokiPersonaPolicy.accepts("これは便利だよ。試してね。"),"casual ending");
   check(TtsSentenceBuffer.split(response).length==1,"2 sentences one speech unit");
  }
  check(LokiPersonaPolicy.accepts("記事には「すごいだろ」とあります。内容は確認が必要です。"),"quotation");
  check(LokiPersonaPolicy.accepts(LokiPersonaPolicy.FALLBACK),"safe fallback");
  check(TtsSentenceBuffer.split("確認します、条件を整理します、\n背景も説明します。").length==1,"commas/newlines buffered");
  String longText=String.join("",Collections.nCopies(100,"内容を説明いたします。"));String[] chunks=TtsSentenceBuffer.split(longText);
  check(String.join("",chunks).equals(longText),"no dropped tail");
  for(String chunk:chunks){check(chunk.length()<=420,"hard limit");check(chunk.endsWith("。"),"sentence boundary");}
  String emoji=String.join("",Collections.nCopies(600,"🚀"));chunks=TtsSentenceBuffer.split(emoji);
  check(String.join("",chunks).equals(emoji),"emoji lossless");
  for(String chunk:chunks)check(!Character.isHighSurrogate(chunk.charAt(chunk.length()-1)),"safe unicode");
  for(int frame:new int[]{80,160,320,640}) {
   RadioPlaybackPolicy vad=new RadioPlaybackPolicy();byte[] voice=pcm(900,frame);
   int samples=0;
   while(samples+frame<11200){check(!vad.speechDetected(voice,voice.length),"wait 700ms");samples+=frame;}
   check(vad.speechDetected(pcm(900,11200-samples),(11200-samples)*2),"700ms boundary");
  }
  RadioPlaybackPolicy vad=new RadioPlaybackPolicy();
  for(int i=0;i<500;i++)check(!vad.speechDetected(pcm(350,320),640),"sustained ambient below threshold");
  for(int i=0;i<10;i++)check(!vad.speechDetected(pcm(900,320),640),"200ms acknowledgement");
  for(int i=0;i<5;i++)check(!vad.speechDetected(pcm(0,320),640),"silence resets short burst");
  for(int i=0;i<34;i++)check(!vad.speechDetected(pcm(900,320),640),"new utterance 680ms");
  check(vad.speechDetected(pcm(900,320),640),"new utterance 700ms");
  check(RadioPlaybackPolicy.calibratedThreshold(100)==520,"noise calibration");
  check(RadioPlaybackPolicy.calibratedThreshold(0)==360,"threshold floor");
  check(!vad.speechDetected(null,640),"null pcm");
  System.out.println("ConversationPolicyTest PASS: "+checks+" checks, 40 turns, speech units, sample-time VAD");
 }
}
