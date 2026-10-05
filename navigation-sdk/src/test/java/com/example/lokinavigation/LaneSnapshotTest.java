package com.example.lokinavigation;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LaneSnapshotTest {
    private List<List<LaneSnapshot.Direction>> lanes() {
        return Arrays.asList(Arrays.asList(new LaneSnapshot.Direction(1, true)),
                Arrays.asList(new LaneSnapshot.Direction(2, false)));
    }
    @Test public void freshAndExpiry() {
        LaneSnapshot s = new LaneSnapshot(LaneSnapshot.State.ENROUTE, 100, 200, lanes());
        assertTrue(s.displayable(100));
        assertTrue(s.displayable(5100));
        assertFalse(s.displayable(5101));
        assertFalse(s.displayable(99));
    }
    @Test public void nonNavigatingClearsLanes() {
        for (LaneSnapshot.State state : Arrays.asList(LaneSnapshot.State.REROUTING,
                LaneSnapshot.State.STOPPED, LaneSnapshot.State.UNKNOWN)) {
            LaneSnapshot s = new LaneSnapshot(state, 100, 200, lanes());
            assertTrue(s.lanes.isEmpty());
            assertNull(s.distanceMeters);
            assertFalse(s.displayable(101));
        }
    }
    @Test public void missingDataDoesNotInventLanes() {
        assertFalse(new LaneSnapshot(LaneSnapshot.State.ENROUTE, 0, null, null).displayable(0));
        List<List<LaneSnapshot.Direction>> unknown = Arrays.asList(
                Arrays.asList(new LaneSnapshot.Direction(1, false)));
        assertFalse(new LaneSnapshot(LaneSnapshot.State.ENROUTE, 0, -1, unknown).displayable(0));
    }
    @Test public void copiesLanePositions() {
        ArrayList<List<LaneSnapshot.Direction>> input = new ArrayList<>(lanes());
        input.add(0, null);
        LaneSnapshot s = new LaneSnapshot(LaneSnapshot.State.ENROUTE, 0, 0, input);
        input.clear();
        assertEquals(3, s.lanes.size());
        assertTrue(s.lanes.get(0).isEmpty());
        assertTrue(s.lanes.get(1).get(0).recommended);
        assertEquals(Integer.valueOf(0), s.distanceMeters);
    }
}
