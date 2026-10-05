package com.example.rokidkeyboardbridge;

/** Six preallocated damped spring axes; positions are fractions of the face size. */
final class MascotSecondaryMotion {
    private final Axis fringeX = new Axis(24d, .50d);
    private final Axis fringeY = new Axis(26d, .54d);
    private final Axis sideX = new Axis(20d, .46d);
    private final Axis sideY = new Axis(22d, .50d);
    private final Axis cheekX = new Axis(32d, .58d);
    private final Axis cheekY = new Axis(34d, .62d);
    private long sampledAt;
    private float sampledAngle;
    private boolean initialized;

    void reset(long nowNanos, float angle) {
        sampledAt = nowNanos;
        sampledAngle = finiteAngle(angle);
        initialized = true;
        fringeX.reset(-sampledAngle * .055d);
        fringeY.reset(sampledAngle * .012d);
        sideX.reset(-sampledAngle * .060d);
        sideY.reset(sampledAngle * .016d);
        cheekX.reset(-sampledAngle * .025d);
        cheekY.reset(sampledAngle * .008d);
    }

    void advance(long nowNanos, float angle) {
        angle = finiteAngle(angle);
        if (!initialized) { reset(nowNanos, angle); return; }
        if (nowNanos <= sampledAt) return;
        double elapsed = (nowNanos - sampledAt) / 1_000_000_000d;
        // Resume from the current anchor after a background pause, without a large impulse.
        if (elapsed > .15d) { reset(nowNanos, angle); return; }
        int steps = Math.max(1, (int) Math.ceil(elapsed * 120d));
        double dt = elapsed / steps;
        for (int i = 1; i <= steps; i++) {
            double anchor = sampledAngle + (angle - sampledAngle) * (i / (double) steps);
            fringeX.advance(-anchor * .055d, dt);
            fringeY.advance(anchor * .012d, dt);
            sideX.advance(-anchor * .060d, dt);
            sideY.advance(anchor * .016d, dt);
            cheekX.advance(-anchor * .025d, dt);
            cheekY.advance(anchor * .008d, dt);
        }
        sampledAt = nowNanos;
        sampledAngle = angle;
        if (isAtRest()) reset(nowNanos, angle);
    }

    boolean isAtRest() {
        return fringeX.rest() && fringeY.rest() && sideX.rest()
                && sideY.rest() && cheekX.rest() && cheekY.rest();
    }

    float fringeX() { return fringeX.offset(.012d); }
    float fringeY() { return fringeY.offset(.004d); }
    float sideX() { return sideX.offset(.018d); }
    float sideY() { return sideY.offset(.006d); }
    float cheekX() { return cheekX.offset(.005d); }
    float cheekY() { return cheekY.offset(.0025d); }

    private static float finiteAngle(float value) {
        return Float.isNaN(value) || Float.isInfinite(value) ? 0f
                : Math.max(0f, Math.min(1f, value));
    }

    private static final class Axis {
        private final double decay, dampedFrequency, frequencySquared;
        private double position, velocity, anchor;

        Axis(double frequency, double damping) {
            decay = frequency * damping;
            dampedFrequency = frequency * Math.sqrt(1d - damping * damping);
            frequencySquared = frequency * frequency;
        }

        void reset(double target) { position = anchor = target; velocity = 0d; }

        void advance(double target, double dt) {
            // Exact underdamped solution for a held target; interpolate targets at <= 8.33ms.
            double offset = position - target;
            double envelope = Math.exp(-decay * dt);
            double sine = Math.sin(dampedFrequency * dt), cosine = Math.cos(dampedFrequency * dt);
            double nextPosition = target + envelope * (offset * cosine
                    + (velocity + decay * offset) / dampedFrequency * sine);
            velocity = envelope * (velocity * cosine
                    - (decay * velocity + frequencySquared * offset) / dampedFrequency * sine);
            position = nextPosition;
            anchor = target;
        }

        boolean rest() { return Math.abs(position - anchor) < .00015d && Math.abs(velocity) < .002d; }
        float offset(double limit) { return (float) Math.max(-limit, Math.min(limit, position - anchor)); }
    }
}
