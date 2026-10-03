package com.example.rokidkeyboardbridge;
import java.io.StringReader;
import java.util.Random;
import javax.xml.parsers.DocumentBuilderFactory;
import org.xml.sax.InputSource;
import com.example.rokidkeyboardbridge.MascotSpeechVisualPolicy.Style;
public final class MascotSpeechEffectsTest {
    private static int checks;
    private static void check(boolean yes,String label) { checks++;if(!yes)throw new AssertionError(label); }
    public static void main(String[] args) throws Exception {
        String[] breaths={"はぁ","はあ","ふぁ","あぁ","ああん","あっ","あ…","んっ","んん","くっ","ひっ","いやっ","だめ","…っ"};
        for(String text:breaths) check(MascotSpeechVisualPolicy.classify("前\n"+text+"後")==Style.BREATHLESS,"all breathless patterns across newline");
        String[] soft={"ふふ","えへ","んー","あの…","あのね","マスター…","好き","一緒","嬉しい"};
        for(String text:soft) check(MascotSpeechVisualPolicy.classify(" "+text+"。 ")==Style.INTIMATE,"all intimate prefixes");
        check(MascotSpeechVisualPolicy.classify("今日は嬉しいですね")==Style.NORMAL,"intimate prefix only");
        check(MascotSpeechVisualPolicy.classify("ふふ、はぁ")==Style.BREATHLESS,"breathless priority");
        MascotSpeechVisualPolicy policy=new MascotSpeechVisualPolicy(new Random(73));
        policy.setText("はぁ"); check(policy.talkBase(0)==14,"base14");
        int[] quick={2,3,2,3};for(int i=0;i<32;i++) check(policy.mouthStage(i*65L)==quick[i%4],"quick half/open cycle");
        policy.setText("マスター…");check(policy.talkBase(0)==8,"base8");
        int[] slow={0,0,1,2,3,2,1,0};for(int i=0;i<32;i++) check(policy.mouthStage(i*180L)==slow[i%8],"slow cycle");
        for(String cue:new String[]{"はぁ","ふふ"}) {
            policy.setText(cue);long period=cue.equals("はぁ")?800:3200;
            check(Math.abs(policy.breathPulse(0)-.5)<.001,"breath center");
            check(policy.breathPulse(period/4)>.999,"breath peak");
            check(policy.breathPulse(period*3/4)<.001,"breath low");
            for(long t=0;t<period;t+=16) {
                check(policy.cheekAlpha(t)==Math.round(36+100*policy.breathPulse(t)),"same pulse alpha");
                check(Math.abs(policy.breathY(t)-(policy.breathPulse(t)-.5)*(cue.equals("はぁ")?.08:.02))<.00001,"same pulse displacement");
            }
        }
        policy.setText("通常です");check(policy.cheekAlpha(400)==0,"no ordinary blush");
        policy.begin(10000);
        long start=10000;
        for(int i=0;i<100;i++) {
            long at=policy.nextBlinkAt();check(at-start>=3000&&at-start<=5000,"3-5sec random interval");
            check(!policy.blinking(at-1),"open before blink");check(policy.blinking(at),"closed at start");
            long end=policy.blinkUntil();check(end-at>=80&&end-at<=160,"80-160ms duration");
            check(!policy.blinking(end),"open at end"); start=at;
        }
        policy.stop();check(!policy.blinking(1000000)&&policy.style()==Style.NORMAL,"idle resets effects");
        policy.begin(0);check(policy.blinking(100000),"delayed render starts finite blink");
        check(!policy.blinking(policy.blinkUntil()),"delayed render does not hold closed");
        String[] texts={"通常の案内です。","あっ、少し待って。あ…わかりました。","マスター…一緒に休みましょう。","はぁっ、だめです。", "<&\"' 😀>", "", "あっ\u0001だめ", "<speak>だめ</speak>"};
        DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
        for(String text:texts) {
            MascotTtsTextFilter.Prepared prepared=MascotTtsTextFilter.prepare(text);
            check(factory.newDocumentBuilder().parse(new InputSource(new StringReader(prepared.ssml))).getDocumentElement().getTagName().equals("speak"),"well-formed SSML and escaped source");
            check(!prepared.forTransport(false).contains("<")&&!prepared.forTransport(false).contains(">"),"no tags to unsupported engine");
            check(prepared.forTransport(true).equals(prepared.ssml),"explicit capability only");
        }
        MascotTtsTextFilter.Prepared pause=MascotTtsTextFilter.prepare("あっ、あ…はぁっ、だめ");
        check(pause.requestedPauseMs==900,"300+300+150+150ms metadata");
        check(pause.ssml.contains("time=\"300ms\"")&&pause.ssml.contains("time=\"150ms\""),"pause tags");
        check(MascotTtsTextFilter.prepare("マスター…").ssml.contains("rate=\"80%\" pitch=\"+1st\" volume=\"soft\""),"whisper attributes");
        check(pause.ssml.contains("rate=\"110%\" pitch=\"+2st\""),"urgent attributes");
        check(MascotTtsTextFilter.prepare(null).fallback.equals(""),"null safe");
        System.out.println("PASS MascotSpeechEffectsTest: "+checks+" checks");
    }
}
