package com.example.rokidkeyboardbridge;
import org.junit.Test;
import static org.junit.Assert.*;

public class NavigationDisplayPolicyTest {
    @Test public void holdsOnlyAnActiveFreshMissingStep() {
        assertTrue(NavigationDisplayPolicy.hold(true,true,false,true,false,1000,2000,5000));
        assertFalse(NavigationDisplayPolicy.hold(false,true,false,true,false,1000,2000,5000));
        assertFalse(NavigationDisplayPolicy.hold(true,false,false,true,false,1000,2000,5000));
        assertFalse(NavigationDisplayPolicy.hold(true,true,true,true,false,1000,2000,5000));
        assertFalse(NavigationDisplayPolicy.hold(true,true,false,false,false,1000,2000,5000));
        assertFalse(NavigationDisplayPolicy.hold(true,true,false,true,true,1000,2000,5000));
        assertFalse(NavigationDisplayPolicy.hold(true,true,false,true,false,1000,6001,5000));
        assertFalse(NavigationDisplayPolicy.hold(true,true,false,true,false,3000,2000,5000));
    }
}
