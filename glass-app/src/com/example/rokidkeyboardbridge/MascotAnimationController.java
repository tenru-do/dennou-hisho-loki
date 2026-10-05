package com.example.rokidkeyboardbridge;

/** Android-free sprite timing and finite gesture policy. All access is on the UI thread. */
public final class MascotAnimationController {
    public enum GestureState { NONE, NOD_ONCE, NOD_TWICE, SHAKE }
    public static final long MOUTH_STAGE_MS = 80L;
    private static final int[] MOUTH = {0, 1, 2, 3, 2, 1};
    private GestureState gesture = GestureState.NONE;
    private GestureState pending = GestureState.NONE;
    private long startedAt;

    public static int mouthStage(long elapsed) {
        return MOUTH[(int)(Math.max(0L, elapsed) / MOUTH_STAGE_MS % MOUTH.length)];
    }
    public static int sheetIndex(int talkBase) { return talkBase / 8; }
    public static int sourceCell(int talkBase, int stage) { return (talkBase / 2 % 4) * 4 + stage; }
    public static GestureState forSpeech(String text) {
        if (text == null) return GestureState.NONE;
        text = text.trim();
        while (!text.isEmpty() && "「『\"“（(".indexOf(text.charAt(0)) >= 0) text = text.substring(1).trim();
        String[][] prefixes = {
            {"了解", "承知", "かしこまり", "わかりました", "はい、", "任せて", "もちろん"},
            {"うん、", "そうだね", "そうですね", "なるほど", "確かに", "わかる", "ふむ"},
            {"いいえ", "ううん", "違います", "無理", "できません", "見つかりません"}
        };
        GestureState[] states = {GestureState.NOD_ONCE, GestureState.NOD_TWICE, GestureState.SHAKE};
        for (int i = 0; i < prefixes.length; i++)
            for (String prefix : prefixes[i]) if (text.startsWith(prefix)) return states[i];
        return GestureState.NONE;
    }
    public static long duration(GestureState state) {
        return state == GestureState.NOD_ONCE ? 600L : state == GestureState.NOD_TWICE ? 800L : state == GestureState.SHAKE ? 660L : 0L;
    }
    public void trigger(GestureState next, long now) {
        sample(now);
        if (next == GestureState.NONE) return;
        if (gesture == GestureState.SHAKE && next == GestureState.SHAKE) return;
        if (gesture == GestureState.NONE || next == GestureState.SHAKE) {
            gesture = next; startedAt = now; pending = GestureState.NONE;
        } else pending = next; // One bounded queued acknowledgement; never an unbounded backlog.
    }
    public void reset() { gesture = pending = GestureState.NONE; }
    public Frame sample(long now) {
        if (gesture != GestureState.NONE && now - startedAt >= duration(gesture)) {
            startedAt += duration(gesture);
            gesture = pending; pending = GestureState.NONE;
            if (gesture != GestureState.NONE && now - startedAt >= duration(gesture)) reset();
        }
        if (gesture == GestureState.NONE) return new Frame(GestureState.NONE, 0, 0, 0, 1, 0);
        double p = Math.max(0, Math.min(1, (now - startedAt) / (double)duration(gesture)));
        if (gesture == GestureState.SHAKE) {
            double[] values = {0, -1, 1, 0};
            double segment = p * 3;
            int at = Math.min(2, (int)segment);
            double ease = (1 - Math.cos(Math.PI * (segment - at))) / 2;
            float direction = (float)(values[at] + (values[at+1] - values[at]) * ease);
            int cell = p < 1.0/6 ? 8 : p < .5 ? 9 : p < 5.0/6 ? 10 : 11;
            return new Frame(gesture, direction * .025f, 0, direction * 8f, 1, cell);
        }
        float depth = (float)Math.pow(Math.sin(Math.PI * p * (gesture == GestureState.NOD_TWICE ? 2 : 1)), 2);
        boolean twice = gesture == GestureState.NOD_TWICE;
        int cell = twice ? (depth > .5 ? (p < .5 ? 5 : 7) : 4) : depth > .75 ? 2 : depth > .25 ? 1 : 0;
        return new Frame(gesture, 0, depth * (twice ? .05f : .10f), 0, 1-depth*(twice ? .02f : .06f), cell);
    }
    public static final class Frame {
        public final GestureState state;
        public final float x, y, rotation, scaleY;
        public final int atlasCell;
        Frame(GestureState state, float x, float y, float rotation, float scaleY, int cell) {
            this.state=state; this.x=x; this.y=y; this.rotation=rotation; this.scaleY=scaleY; this.atlasCell=cell;
        }
        public boolean active() { return state != GestureState.NONE; }
    }
}
