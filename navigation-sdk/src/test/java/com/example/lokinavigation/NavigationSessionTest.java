package com.example.lokinavigation;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

public class NavigationSessionTest {
    private boolean enabled = true, allowance = true, feedReady = true, clearThrows;
    private int reserved, requested, started, cleared;
    private NavigationSession.Completion reply;
    private final NavigationSession<String> session = new NavigationSession<>(() -> enabled,
            count -> { if (!allowance) return false; reserved += count; return true; },
            new NavigationSession.Engine<String>() {
                public void request(java.util.List<String> destinations, NavigationSession.Completion completion) {
                    assertEquals(destinations.size(), reserved);
                    requested++; reply = completion;
                }
                public boolean startGuidanceAndFeed() { started++; return feedReady; }
                public void stopAndClear() { cleared++; if (clearThrows) throw new IllegalStateException("stop failed"); }
            });

    @Test public void countsBeforeRequestAndStartsOnlyOnSuccess() {
        assertEquals(NavigationSession.StartResult.STARTED, session.start(Arrays.asList("a", "b")));
        assertEquals(2, reserved);
        assertEquals(0, started);
        reply.complete(true);
        assertEquals(NavigationSession.State.ACTIVE, session.state());
        assertEquals(1, started);
    }
    @Test public void disabledAndQuotaDeniedNeverCallSdk() {
        enabled = false;
        assertEquals(NavigationSession.StartResult.DISABLED, session.start(Arrays.asList("a")));
        enabled = true; allowance = false;
        assertEquals(NavigationSession.StartResult.LIMIT, session.start(Arrays.asList("a")));
        assertEquals(0, requested); assertEquals(0, reserved);
    }
    @Test public void duplicateTapDoesNotSpendAgain() {
        session.start(Arrays.asList("a"));
        assertEquals(NavigationSession.StartResult.BUSY, session.start(Arrays.asList("a")));
        reply.complete(true);
        assertEquals(NavigationSession.StartResult.BUSY, session.start(Arrays.asList("b")));
        assertEquals(1, reserved); assertEquals(1, requested);
    }
    @Test public void stopDuringRequestNeverRestartsGuidance() {
        session.start(Arrays.asList("a"));
        assertTrue(session.hasPendingRequest());
        session.stop();
        assertTrue(session.hasPendingRequest());
        assertEquals(NavigationSession.State.STOPPING, session.state());
        assertEquals(NavigationSession.StartResult.BUSY, session.start(Arrays.asList("b")));
        reply.complete(true);
        assertFalse(session.hasPendingRequest());
        assertEquals(0, started);
        assertEquals(NavigationSession.State.STOPPED, session.state());
    }
    @Test public void revokedGateStopsPendingResult() {
        session.start(Arrays.asList("a")); enabled = false;
        reply.complete(true);
        assertEquals(0, started);
        assertEquals(NavigationSession.State.STOPPED, session.state());
    }
    @Test public void routeFailureClearsAndDoesNotRefund() {
        session.start(Arrays.asList("a")); reply.complete(false);
        assertEquals(NavigationSession.State.FAILED, session.state());
        assertEquals(0, started); assertEquals(1, reserved); assertEquals(2, cleared);
    }
    @Test public void feedFailureClearsRoute() {
        feedReady = false;
        session.start(Arrays.asList("a")); reply.complete(true);
        assertEquals(NavigationSession.State.FAILED, session.state());
        assertEquals(2, cleared);
    }
    @Test public void duplicateCompletionIsIgnored() {
        session.start(Arrays.asList("a")); reply.complete(true); reply.complete(true);
        assertEquals(1, started);
        session.stop(); reply.complete(true);
        assertEquals(NavigationSession.State.STOPPED, session.state());
    }
    @Test public void stopFailureStillCancelsPendingStart() {
        session.start(Arrays.asList("a"));
        clearThrows = true;
        session.stop();
        assertTrue(session.hasPendingRequest());
        reply.complete(true);
        assertFalse(session.hasPendingRequest());
        assertEquals(0, started);
        assertEquals(NavigationSession.State.FAILED, session.state());
    }
}
