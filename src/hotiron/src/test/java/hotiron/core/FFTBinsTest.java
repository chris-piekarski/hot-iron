package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class FFTBinsTest {

    @Test
    void testHoldsData() {
        double[] freqs = {2400.0, 2400.1};
        float[] powers = {-80f, -75f};
        FFTBins bins = new FFTBins(true, freqs, 100000f, powers);

        assertTrue(bins.fullSweepDone);
        assertEquals(100000f, bins.fftBinWidthHz);
        assertEquals(2, bins.freqStart.length);
        assertEquals(2400.0, bins.freqStart[0]);
        assertEquals(-75f, bins.sigPowdBm[1], 0.001f);
    }
}
