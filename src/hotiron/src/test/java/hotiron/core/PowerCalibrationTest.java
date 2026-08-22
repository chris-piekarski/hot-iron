package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class PowerCalibrationTest {

    @Test
    void testOffsetCalculation() {
        PowerCalibration cal = new PowerCalibration(-30.0, -35.0, 30.0);
        assertEquals(5.0, cal.offset_dB);
        assertEquals(30.0, cal.gain);

        // At same gain, offset should be +5 dB
        assertEquals(5.0, cal.getOffset_dB(30.0), 0.001);

        // At higher gain setting, the correction changes
        assertTrue(cal.getOffset_dB(40.0) < 5.0);
    }

    @Test
    void testCorrectPowerAppliesOffset() {
        PowerCalibration cal = new PowerCalibration(-30.0, -35.0, 30.0);
        double[] freqs = {2400};
        float[] powers = {-80f};
        FFTBins bins = new FFTBins(true, freqs, 100000f, powers);

        PowerCalibration.correctPower(cal, 30.0, bins);
        assertEquals(-75f, bins.sigPowdBm[0], 0.001f);  // -80 + 5
    }
}
