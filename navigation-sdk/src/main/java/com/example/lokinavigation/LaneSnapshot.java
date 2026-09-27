package com.example.lokinavigation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Session-only data; never infer lanes from a turn instruction or persist road data. */
public final class LaneSnapshot {
    public enum State { ENROUTE, REROUTING, STOPPED, UNKNOWN }
    public static final long MAX_AGE_MS = 5000L;

    public static final class Direction {
        public final int sdkShape;
        public final boolean recommended;
        public Direction(int sdkShape, boolean recommended) {
            this.sdkShape = sdkShape;
            this.recommended = recommended;
        }
    }

    public final State state;
    public final long receivedElapsedMs;
    public final Integer distanceMeters;
    public final List<List<Direction>> lanes;

    public LaneSnapshot(State state, long elapsedMs, Integer distance, List<List<Direction>> lanes) {
        this.state = state == null ? State.UNKNOWN : state;
        this.receivedElapsedMs = elapsedMs;
        this.distanceMeters = this.state == State.ENROUTE && distance != null && distance >= 0 ? distance : null;
        ArrayList<List<Direction>> copy = new ArrayList<>();
        if (this.state == State.ENROUTE && lanes != null) {
            for (List<Direction> lane : lanes) {
                // Preserve lane positions, including lanes without directions.
                ArrayList<Direction> directions = new ArrayList<>();
                if (lane != null) for (Direction d : lane) if (d != null) directions.add(d);
                copy.add(Collections.unmodifiableList(directions));
            }
        }
        this.lanes = Collections.unmodifiableList(copy);
    }

    public boolean displayable(long elapsedMs) {
        long age = elapsedMs - receivedElapsedMs;
        if (state != State.ENROUTE || age < 0 || age > MAX_AGE_MS) return false;
        for (List<Direction> lane : lanes) for (Direction d : lane) if (d.recommended) return true;
        return false;
    }
}
