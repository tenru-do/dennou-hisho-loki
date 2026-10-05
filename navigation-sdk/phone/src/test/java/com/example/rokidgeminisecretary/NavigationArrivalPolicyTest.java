package com.example.rokidgeminisecretary;
import org.junit.Test;
import static org.junit.Assert.*;

public class NavigationArrivalPolicyTest {
    @Test public void onlyExplicitFinalArrivalCompletes() {
        assertTrue(NavigationArrivalPolicy.destinationArrivalMessage("目的地に到着しました"));
        assertTrue(NavigationArrivalPolicy.destinationArrivalMessage("You've arrived"));
        assertFalse(NavigationArrivalPolicy.destinationArrivalMessage("次、東京駅"));
        assertFalse(NavigationArrivalPolicy.destinationArrivalMessage("東京駅に到着しました"));
        assertFalse(NavigationArrivalPolicy.destinationArrivalMessage("目的地に到着まで5分"));
        assertFalse(NavigationArrivalPolicy.destinationArrivalMessage("到着 15:30"));
    }
    @Test public void requiresAccurateFreshSlowAndEstablishedRoute() {
        long now=100000;
        assertTrue(NavigationArrivalPolicy.nearFinalPoint(now,now-1000,10,1,now-60000,20));
        assertFalse(NavigationArrivalPolicy.nearFinalPoint(now,now-16000,10,1,now-60000,20));
        assertFalse(NavigationArrivalPolicy.nearFinalPoint(now,now-1000,50,1,now-60000,20));
        assertFalse(NavigationArrivalPolicy.nearFinalPoint(now,now-1000,10,5,now-60000,20));
        assertFalse(NavigationArrivalPolicy.nearFinalPoint(now,now-1000,10,1,now-1000,20));
        assertFalse(NavigationArrivalPolicy.nearFinalPoint(now,now-1000,10,1,now-60000,60));
    }
    @Test public void stationaryLocationWithoutSpeedCanArrive() {
        assertTrue(NavigationArrivalPolicy.nearFinalPoint(100000,99000,10,-1,40000,20));
        assertFalse(NavigationArrivalPolicy.nearFinalPoint(100000,99000,10,Float.NaN,40000,20));
        assertFalse(NavigationArrivalPolicy.nearFinalPoint(100000,99000,10,-1,40000,Float.NaN));
        assertFalse(NavigationArrivalPolicy.nearFinalPoint(100000,101000,10,-1,40000,20));
    }
}
