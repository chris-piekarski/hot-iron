package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class GainPolicyTest {

    @Test
    void splitsTotalGainIntoLnaThenVga() {
        assertEquals(0, GainPolicy.lnaGain(0));
        assertEquals(0, GainPolicy.vgaGain(0));

        assertEquals(32, GainPolicy.lnaGain(32));
        assertEquals(0, GainPolicy.vgaGain(32));

        assertEquals(40, GainPolicy.lnaGain(40));
        assertEquals(0, GainPolicy.vgaGain(40));

        assertEquals(40, GainPolicy.lnaGain(41));
        assertEquals(0, GainPolicy.vgaGain(41));

        assertEquals(40, GainPolicy.lnaGain(42));
        assertEquals(2, GainPolicy.vgaGain(42));

        assertEquals(40, GainPolicy.lnaGain(100));
        assertEquals(60, GainPolicy.vgaGain(100));
        assertEquals(100, GainPolicy.clampTotal(200));
        assertEquals(32, GainPolicy.clampTotal(36));
    }

    @Test
    void negativeTotalGainClampsLna() {
        assertEquals(0, GainPolicy.lnaGain(-8));
        assertEquals(0, GainPolicy.vgaGain(-8));
    }
}
