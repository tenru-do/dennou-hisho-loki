package com.example.rokidgeminisecretary;
import org.junit.Test;
import static org.junit.Assert.*;

public class NavigationArrivalPolicyTest {
    @Test public void requiresAccurateFreshSlowAndEstablishedRoute() {
        long now=100000;
        assertTrue(NavigationArrivalPolicy.nearFinalPoint(now,now-1000,10,1,now-60000,20));
        assertFalse(NavigationArrivalPolicy.nearFinalPoint(now,now-16000,10,1,now-60000,20));
        assertFalse(NavigationArrivalPolicy.nearFinalPoint(now,now-1000,50,1,now-60000,20));
        assertFalse(NavigationArrivalPolicy.nearFinalPoint(now,now-1000,10,5,now-60000,20));
        assertFalse(NavigationArrivalPolicy.nearFinalPoint(now,now-1000,10,1,now-1000,20));
        assertFalse(NavigationArrivalPolicy.nearFinalPoint(now,now-1000,10,1,now-60000,60));
    }
}
