package com.example.rokidgeminisecretary;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
public class WalkingFallbackTest {
    @Test public void bicycleZeroRoutesRetriesWalkExactlyOnce() throws Exception {
        List<String> calls = new ArrayList<>();
        WalkingFallback.Result<String> result = WalkingFallback.fetch("bicycling", mode -> {
            calls.add(mode);
            if (mode.equals("bicycling")) throw new Exception("google_route_not_found");
            return "polyline";
        });
        assertEquals(Arrays.asList("bicycling","walking"), calls);
        assertEquals("walking", result.mode); assertEquals("polyline", result.value);
    }
    @Test public void successDoesNotFallback() throws Exception {
        assertEquals("bicycling", WalkingFallback.fetch("bicycling", mode -> "ok").mode);
    }
    @Test public void authNetworkAndQuotaDoNotFallback() throws Exception {
        for (String error : Arrays.asList("google_http_403","google_http_429","google_network_error")) {
            final int[] calls={0};
            try { WalkingFallback.fetch("bicycling", mode -> { calls[0]++; throw new Exception(error); }); fail(); }
            catch (Exception expected) { assertEquals(error,expected.getMessage()); }
            assertEquals(1,calls[0]);
        }
    }
    @Test public void walkingZeroRoutesDoesNotLoop() throws Exception {
        final int[] calls={0};
        try { WalkingFallback.fetch("walking", mode -> { calls[0]++; throw new Exception("google_route_not_found"); }); fail(); }
        catch (Exception expected) { assertEquals("google_route_not_found",expected.getMessage()); }
        assertEquals(1,calls[0]);
    }
}
