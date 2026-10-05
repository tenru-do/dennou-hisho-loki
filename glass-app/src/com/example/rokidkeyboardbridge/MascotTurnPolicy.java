package com.example.rokidkeyboardbridge;

/** Allocation-free timing for the 15-pose atlas. All inputs are eased Animator progress. */
final class MascotTurnPolicy {
    static final int FRAME_COUNT = 15;
    static final long DURATION_MS = 560L;
    static final float EYE_LEFT = .20f, EYE_TOP = .43f, EYE_RIGHT = .80f, EYE_BOTTOM = .59f;

    private MascotTurnPolicy() {}

    static float clamp(float value) { return Math.max(0f, Math.min(1f, value)); }

    static float position(float start, float target, float progress) {
        // The gaze leads the head. Hold the endpoint briefly to settle the turn.
        float head = clamp((progress - .15f) / .70f);
        return start + (target - start) * head;
    }

    static int frame(float position) {
        return Math.round(clamp(position) * (FRAME_COUNT - 1));
    }

    static boolean blinking(float progress) {
        return progress >= .40f && progress <= .60f;
    }

    static float gazeOffset(float progress, boolean toFront) {
        float anticipation = progress < .15f ? clamp(progress / .07f)
                : clamp((.30f - progress) / .15f);
        // Screen-right is the 3/4 pose's facing direction; front gaze comes left.
        return (toFront ? -1f : 1f) * .009f * anticipation;
    }

    static long duration(float start, float target) {
        // Keep the same maximum pose step even for a turn reversed halfway.
        return Math.max(160L, Math.round(DURATION_MS * Math.abs(target - start)));
    }
}
