package com.example.rokidkeyboardbridge;
import com.example.rokidkeyboardbridge.MascotAnimationController.GestureState;
public final class MascotAnimationControllerTest {
    private static int checks;
    private static void check(boolean value, String label) {
        checks++; if (!value) throw new AssertionError(label);
    }
    public static void main(String[] args) {
        int[] cycle = {0,1,2,3,2,1};
        for (int tick=0;tick<60;tick++) {
            check(MascotAnimationController.mouthStage(tick*80L)==cycle[tick%6], "ping-pong");
            check(MascotAnimationController.mouthStage(tick*80L+79)==cycle[tick%6], "stage hold");
        }
        check(MascotAnimationController.mouthStage(-1)==0, "negative elapsed");
        check(MascotAnimationController.mouthStage(Long.MAX_VALUE)>=0, "long elapsed");
        for (int base=0;base<=14;base+=2) for (int stage=0;stage<4;stage++) {
            check(MascotAnimationController.sheetIndex(base)==(base<8?0:1), "all eight expressions sheet");
            check(MascotAnimationController.sourceCell(base,stage)==(base/2%4)*4+stage, "all source cells");
        }
        String[][] starts = {{"了解","承知","かしこまり","わかりました","はい、","任せて","もちろん"},
          {"うん、","そうだね","そうですね","なるほど","確かに","わかる","ふむ"},
          {"いいえ","ううん","違います","無理","できません","見つかりません"}};
        GestureState[] states = {GestureState.NOD_ONCE,GestureState.NOD_TWICE,GestureState.SHAKE};
        for(int i=0;i<starts.length;i++) for(String text:starts[i]) {
            check(MascotAnimationController.forSpeech(text+"テスト")==states[i], "every requested prefix");
            check(MascotAnimationController.forSpeech("  「"+text)==states[i], "whitespace and quote");
        }
        check(MascotAnimationController.forSpeech(null)==GestureState.NONE,"null speech");
        check(MascotAnimationController.forSpeech("今日は、なるほどと言いました")==GestureState.NONE,"prefix only");
        for(GestureState state:states) {
            MascotAnimationController c=new MascotAnimationController(); c.trigger(state,1000);
            check(c.sample(1000).active(),"begin");
            check(c.sample(1000+MascotAnimationController.duration(state)-1).active(),"before end");
            MascotAnimationController.Frame f=c.sample(1000+MascotAnimationController.duration(state));
            check(!f.active() && f.x==0 && f.y==0 && f.rotation==0 && f.scaleY==1,"automatic neutral completion");
        }
        MascotAnimationController c=new MascotAnimationController();
        c.trigger(GestureState.NOD_ONCE,0); check(c.sample(300).y>.099,"one deep peak");
        c.reset(); c.trigger(GestureState.NOD_TWICE,0);
        check(c.sample(200).y>.049,"first shallow nod"); check(c.sample(400).y<.001,"return between nods");
        check(c.sample(600).y>.049,"second shallow nod");
        c.reset(); c.trigger(GestureState.SHAKE,0);
        check(c.sample(220).x<0 && c.sample(440).x>0,"left right shake");
        c.reset(); c.trigger(GestureState.NOD_ONCE,0); c.trigger(GestureState.NOD_TWICE,100);
        check(c.sample(600).state==GestureState.NOD_TWICE,"bounded acknowledgement queue");
        check(!c.sample(1400).active(),"queued completion");
        c.trigger(GestureState.NOD_ONCE,2000); c.trigger(GestureState.SHAKE,2100);
        check(c.sample(2200).state==GestureState.SHAKE,"error preempts nod");
        c.trigger(GestureState.SHAKE,2500); check(!c.sample(2760).active(),"duplicate error cannot prolong shake");
        c.trigger(GestureState.NOD_ONCE,3000); c.trigger(GestureState.NOD_TWICE,3100);
        check(!c.sample(10000).active(),"expired queue cannot revive after long pause");
        c.trigger(GestureState.SHAKE,11000); c.reset(); check(!c.sample(11001).active(),"detach reset");
        System.out.println("PASS MascotAnimationControllerTest: "+checks+" checks");
    }
}
