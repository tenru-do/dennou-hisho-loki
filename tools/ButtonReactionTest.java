package com.example.rokidkeyboardbridge;
import android.content.Context;
import android.graphics.*;
public final class ButtonReactionTest {
    private static int passed;
    private static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);passed++;}
    public static int run(Context context,String ignored){
        ButtonReactionMotion motion=new ButtonReactionMotion();
        check(!motion.active(0),"inactive initially");motion.begin(1000,false,false);
        check(motion.active(1000)&&motion.active(1899)&&!motion.active(1900),"bounded duration");
        check(!motion.active(999),"clock reversal safe");
        check(motion.face(0,1450)==1,"neutral acknowledgement");
        check(motion.face(6,1450)==6,"preserve worry");
        check(motion.face(21,1450)==21,"preserve refusal");
        check(motion.face(0,1900)==0,"face expires without restoring stale state");
        Canvas c=new Canvas(Bitmap.createBitmap(60,100,Bitmap.Config.ARGB_8888));Matrix m=new Matrix();
        motion.apply(c,60,100,1900);c.getMatrix(m);check(m.isIdentity(),"expired motion identity");
        motion.apply(c,60,100,1450);c.getMatrix(m);check(!m.isIdentity(),"visible acknowledgement");
        motion.begin(1500,true,false);check(motion.face(0,1600)==0,"contact preserves scene face");
        check(!motion.active(2400),"repeat replaces rather than queues");
        motion.clear();check(!motion.active(1550),"lifecycle clear");
        check(ButtonReactionContext.touching("肩をたたいてあげる"),"shoulder scene");
        check(ButtonReactionContext.touching("頭を撫でる"),"head scene");
        check(!ButtonReactionContext.touching("触らないで"),"negative contact cue");
        check(!ButtonReactionContext.touching("会話を終了、触れるのはやめて"),"end contact");
        check(!ButtonReactionContext.touching("明日の天気を教えて"),"ordinary request");
        LokiActionButton button=new LokiActionButton(context);final int[] clicks={0};
        button.setOnClickListener(v->clicks[0]++);button.performClick();check(clicks[0]==1,"single callback");
        button.setEnabled(false);button.performClick();check(clicks[0]==1,"disabled never activates");
        button.setEnabled(true);button.performClick();check(clicks[0]==2,"repeated activation preserved");
        String[] menu=NavigationEntryPolicy.menu(false),active=NavigationEntryPolicy.menu(true),quick=NavigationEntryPolicy.quickDisplayMenu();
        check(menu[1].equals("目的地を指定")&&menu[2].equals("予定から目的地"),"calendar under destination");
        check(active[4].equals("目的地を指定")&&active[5].equals("予定から目的地"),"calendar order during guidance");
        check(menu[3].equals("行きのルート")&&menu[4].equals("帰りのルート"),"saved route order preserved");
        check(quick.length==5&&quick[4].equals("ナビを閉じる"),"close last in display modes");
        check(ButtonReactionContext.touching("身体に触れている"),"contact vocabulary final");
        check(!ButtonReactionContext.touching("触っていない"),"negation final");
        check(!ButtonReactionContext.touching("話題を変えて触れる"),"topic change ends contact");
        check(LokiActionButton.class.isAssignableFrom(MusicToggleButton.class),"play button uses common feedback");
        return passed;
    }
}
