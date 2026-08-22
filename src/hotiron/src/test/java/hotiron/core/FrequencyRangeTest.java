package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class FrequencyRangeTest {

    @Test
    void testBasicAccess() {
        FrequencyRange range = new FrequencyRange(2400, 2500);
        assertEquals(2400, range.getStartMHz());
        assertEquals(2500, range.getEndMHz());
    }

    @Test
    void testEquals() {
        FrequencyRange a = new FrequencyRange(100, 200);
        FrequencyRange b = new FrequencyRange(100, 200);
        FrequencyRange c = new FrequencyRange(100, 201);

        assertEquals(a, b);
        assertNotEquals(a, c);
        assertNotEquals(a, null);
        assertNotEquals(a, "string");
    }

    @Test
    void interleavedPadCoversFmHopHoles() {
        FrequencyRange nativeFm = new FrequencyRange(88, 108).forInterleavedNativeSweep();
        assertEquals(78, nativeFm.getStartMHz());
        assertEquals(118, nativeFm.getEndMHz());
        FrequencyRange floor = new FrequencyRange(1, 30).forInterleavedNativeSweep();
        assertEquals(1, floor.getStartMHz());
        assertEquals(40, floor.getEndMHz());
    }
}
