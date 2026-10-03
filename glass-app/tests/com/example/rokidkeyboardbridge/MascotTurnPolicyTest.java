package com.example.rokidkeyboardbridge;

public final class MascotTurnPolicyTest {
    private static int checks;
    private static void check(boolean yes, String label) {
        checks++;
        if (!yes) throw new AssertionError(label);
    }

    private static float eased(double time) {
        return (float) ((1.0 - Math.cos(Math.PI * time)) * .5);
    }

    private static void playback(float start, float target) {
        long duration = MascotTurnPolicy.duration(start, target);
        int previous = MascotTurnPolicy.frame(start);
        boolean[] seen = new boolean[MascotTurnPolicy.FRAME_COUNT];
        seen[previous] = true;
        int blinkFrames = 0;
        for (int tick = 0; tick * (1000.0 / 60.0) <= duration; tick++) {
            float progress = eased(tick * (1000.0 / 60.0) / duration);
            float angle = MascotTurnPolicy.position(start, target, progress);
            int cell = MascotTurnPolicy.frame(angle);
            check(Math.abs(cell - previous) <= 1, "60Hz cannot skip an intermediate pose");
            check(target > start ? cell >= previous : cell <= previous, "monotonic turn");
            seen[cell] = true;
            previous = cell;
            if (MascotTurnPolicy.blinking(progress)) blinkFrames++;
        }
        int endpoint = MascotTurnPolicy.frame(target);
        check(previous == endpoint, "endpoint settled before Animator end");
        for (int cell = Math.min(MascotTurnPolicy.frame(start), endpoint);
                cell <= Math.max(MascotTurnPolicy.frame(start), endpoint); cell++)
            check(seen[cell], "every traversed pose is actually shown");
        check(blinkFrames > 0, "mid-turn blink is visible at 60Hz");
    }

    public static void main(String[] args) {
        playback(0f, 1f);
        playback(1f, 0f);
        for (int cell = 1; cell < 14; cell++) {
            playback(cell / 14f, 0f);
            playback(cell / 14f, 1f);
        }
        check(MascotTurnPolicy.position(0f, 1f, .10f) == 0f, "head waits for gaze");
        check(MascotTurnPolicy.gazeOffset(.10f, true) < 0f, "front gaze leads left");
        check(MascotTurnPolicy.gazeOffset(.10f, false) > 0f, "3Q gaze leads right");
        check(MascotTurnPolicy.gazeOffset(.33f, true) == 0f, "gaze rejoins eyes");
        check(MascotTurnPolicy.blinking(.5f), "closed at midpoint in either direction");
        check(!MascotTurnPolicy.blinking(0f) && !MascotTurnPolicy.blinking(1f), "no stuck closed endpoints");
        check(MascotTurnPolicy.position(.43f, 0f, 0f) == .43f, "reversal preserves visible yaw");
        check(MascotTurnPolicy.frame(-1f) == 0 && MascotTurnPolicy.frame(2f) == 14, "atlas bounds");
        System.out.println("PASS MascotTurnPolicyTest: " + checks + " checks");
    }
}
