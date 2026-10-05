package com.example.rokidkeyboardbridge;
import java.util.Random;
import java.util.regex.Pattern;

/** Monotonic-time visual policy, independent of Android and speech transport. */
public final class MascotSpeechVisualPolicy {
    public enum Style { NORMAL, INTIMATE, BREATHLESS }
    private static final Pattern BREATHLESS = Pattern.compile("はぁ|はあ|ふぁ|あぁ|ああん|あっ|あ…|んっ|んん|くっ|ひっ|いやっ|だめ|…っ");
    private static final Pattern INTIMATE = Pattern.compile("^(ふふ|えへ|んー|あの…|あのね|マスター…|好き|一緒|嬉しい)");
    private static final int[] SOFT_MOUTH = {0,0,1,2,3,2,1,0};
    private static final int[] QUICK_MOUTH = {2,3,2,3};
    private final Random random;
    private long nextBlinkAt = Long.MAX_VALUE, blinkUntil;
    private Style style = Style.NORMAL;
    public MascotSpeechVisualPolicy() { this(new Random()); }
    public MascotSpeechVisualPolicy(Random random) { this.random = random; }
    public static Style classify(String text) {
        String value = text == null ? "" : text.trim();
        if (BREATHLESS.matcher(value).find()) return Style.BREATHLESS;
        return INTIMATE.matcher(value).find() ? Style.INTIMATE : Style.NORMAL;
    }
    public void setText(String text) { style = classify(text); }
    public Style style() { return style; }
    public void begin(long now) { blinkUntil=0; nextBlinkAt=now+3000+random.nextInt(2001); }
    public void stop() { nextBlinkAt=Long.MAX_VALUE; blinkUntil=0; style=Style.NORMAL; }
    public boolean blinking(long now) {
        if (now >= nextBlinkAt) {
            blinkUntil=now+80+random.nextInt(81);
            nextBlinkAt=now+3000+random.nextInt(2001);
        }
        return now < blinkUntil;
    }
    public long nextBlinkAt() { return nextBlinkAt; }
    public long blinkUntil() { return blinkUntil; }
    public int talkBase(int fallback) { return style==Style.BREATHLESS?14:style==Style.INTIMATE?8:fallback; }
    public int mouthStage(long elapsed) {
        long safe=Math.max(0L,elapsed);
        if(style==Style.BREATHLESS) return QUICK_MOUTH[(int)(safe/65%QUICK_MOUTH.length)];
        if(style==Style.INTIMATE) return SOFT_MOUTH[(int)(safe/180%SOFT_MOUTH.length)];
        return MascotAnimationController.mouthStage(safe);
    }
    public float breathPulse(long elapsed) {
        long period=style==Style.BREATHLESS?800L:style==Style.INTIMATE?3200L:2400L;
        return (float)((1+Math.sin((Math.max(0L,elapsed)%period)*2*Math.PI/period))/2);
    }
    public float breathY(long elapsed) {
        return (breathPulse(elapsed)-.5f)*(style==Style.BREATHLESS?.08f:style==Style.INTIMATE?.02f:.012f);
    }
    public int cheekAlpha(long elapsed) { return style==Style.NORMAL?0:Math.round(36+100*breathPulse(elapsed)); }
}
