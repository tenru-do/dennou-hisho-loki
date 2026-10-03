package com.example.rokidkeyboardbridge;

public final class MascotSecondaryMotionTest {
    private static int checks;
    private static void check(boolean yes, String label) {
        checks++;
        if (!yes) throw new AssertionError(label);
    }
    private static long nanos(double seconds) { return Math.round(seconds * 1_000_000_000d); }

    private static void bounds(MascotSecondaryMotion motion) {
        check(Float.isFinite(motion.fringeX()) && Float.isFinite(motion.sideY()), "finite motion");
        check(Math.abs(motion.fringeX()) <= .01201f && Math.abs(motion.fringeY()) <= .00401f, "fringe clamp");
        check(Math.abs(motion.sideX()) <= .01801f && Math.abs(motion.sideY()) <= .00601f, "side clamp");
        check(Math.abs(motion.cheekX()) <= .00501f && Math.abs(motion.cheekY()) <= .00251f, "cheek clamp");
    }

    private static void stopAndSettle(int hz, boolean forward) {
        MascotSecondaryMotion motion = new MascotSecondaryMotion();
        motion.reset(0L, forward ? 0f : 1f);
        for (int frame = 1; frame <= hz / 2; frame++) {
            float angle = frame / (float) (hz / 2);
            motion.advance(nanos(frame / (double) hz), forward ? angle : 1f - angle);
            bounds(motion);
        }
        float lag = motion.sideX();
        check(forward ? lag > 0f : lag < 0f, "hair lags the turn");
        boolean overshot = false;
        float peak = 0f;
        for (int frame = 1; frame <= hz; frame++) {
            motion.advance(nanos(.5 + frame / (double) hz), forward ? 1f : 0f);
            bounds(motion);
            if (forward ? motion.sideX() < -.0002f : motion.sideX() > .0002f) overshot = true;
            peak = Math.max(peak, Math.abs(motion.sideX()));
        }
        check(overshot && peak > .0002f, "directional overshoot after head stops");
        check(motion.isAtRest() && motion.sideX() == 0f, "tail stops scheduling after settling");
    }

    public static void main(String[] args) {
        for (int hz : new int[]{30, 60, 90, 120}) {
            stopAndSettle(hz, true);
            stopAndSettle(hz, false);
        }
        // Exercise the real eased turn, including its gaze lead and final head hold.
        for (boolean forward : new boolean[]{true, false}) {
            MascotSecondaryMotion coupled = new MascotSecondaryMotion();
            coupled.reset(0L, forward ? 0f : 1f);
            boolean overshotAfterStop = false;
            for (int tick = 1; tick <= 90; tick++) {
                double time = tick / 60d;
                float eased = (float) ((1d - Math.cos(Math.PI * Math.min(1d,
                        time * 1000d / MascotTurnPolicy.DURATION_MS))) * .5d);
                float angle = MascotTurnPolicy.position(forward ? 0f : 1f,
                        forward ? 1f : 0f, eased);
                coupled.advance(nanos(time), angle);
                bounds(coupled);
                if (angle == (forward ? 1f : 0f)
                        && (forward ? coupled.sideX() < -.0002f : coupled.sideX() > .0002f))
                    overshotAfterStop = true;
            }
            check(overshotAfterStop, "real eased head stop retains directional inertia");
            check(coupled.isAtRest(), "real turn tail settles");
        }
        MascotSecondaryMotion motion = new MascotSecondaryMotion();
        motion.reset(0L, 0f);
        for (int frame = 1; frame <= 20; frame++) motion.advance(nanos(frame / 60d), frame / 30f);
        float before = motion.sideX();
        motion.advance(nanos(21 / 60d), 20 / 30f);
        check(Math.abs(motion.sideX() - before) < .01f, "reversal retains spring state");
        for (int frame = 22; frame <= 42; frame++) {
            motion.advance(nanos(frame / 60d), Math.max(0f, (42 - frame) / 30f));
            bounds(motion);
        }
        motion.advance(nanos(3d), 0f);
        check(motion.isAtRest(), "background pause cannot explode");
        motion.advance(nanos(3d), 1f);
        check(motion.isAtRest(), "same timestamp does not integrate twice");
        motion.advance(nanos(2d), 1f);
        check(motion.isAtRest(), "out-of-order callback ignored");
        motion.advance(nanos(4d), Float.NaN);
        bounds(motion);
        check(motion.isAtRest(), "invalid input safe");
        System.out.println("PASS MascotSecondaryMotionTest: " + checks + " checks");
    }
}
