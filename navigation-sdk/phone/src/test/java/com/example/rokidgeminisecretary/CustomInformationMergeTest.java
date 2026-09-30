package com.example.rokidgeminisecretary;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public final class CustomInformationMergeTest {
    @Test public void glassStaysFirstAndPhoneOnlyAddsMissingLines() {
        assertEquals("glass A\nglass B\nphone C",
                CustomInformationMerge.merge("glass A\nglass B", "glass B\nphone C", 100));
    }

    @Test public void identicalCopiesDoNotDuplicate() {
        assertEquals("same", CustomInformationMerge.merge("same", "same", 100));
    }

    @Test public void overflowDoesNotProduceTruncatedMaster() {
        assertNull(CustomInformationMerge.merge("glass", "phone", 10));
    }
}
